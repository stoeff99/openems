package io.openems.edge.mqtt.generic.predictor;

import static org.osgi.service.component.annotations.ConfigurationPolicy.REQUIRE;
import static org.osgi.service.component.annotations.ReferenceCardinality.MANDATORY;
import static org.osgi.service.component.annotations.ReferencePolicy.STATIC;
import static org.osgi.service.component.annotations.ReferencePolicyOption.GREEDY;

import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.concurrent.ConcurrentSkipListMap;
import java.util.regex.Pattern;

import org.osgi.service.cm.ConfigurationAdmin;
import org.osgi.service.component.ComponentContext;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Deactivate;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.metatype.annotations.Designate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.openems.common.exceptions.OpenemsError.OpenemsNamedException;
import io.openems.common.types.ChannelAddress;
import io.openems.common.utils.DateUtils;
import io.openems.edge.bridge.mqtt.api.BridgeMqtt;
import io.openems.edge.bridge.mqtt.api.MqttComponent;
import io.openems.edge.bridge.mqtt.api.MqttMessage;
import io.openems.edge.common.component.AbstractOpenemsComponent;
import io.openems.edge.common.component.ComponentManager;
import io.openems.edge.common.component.OpenemsComponent;
import io.openems.edge.mqtt.generic.common.MqttSubscriptions;
import io.openems.edge.mqtt.generic.common.MqttValues;
import io.openems.edge.predictor.api.prediction.Prediction;
import io.openems.edge.predictor.api.prediction.Predictor;

@Designate(ocd = Config.class, factory = true)
@Component(//
		name = "Predictor.Mqtt.HourlyForecast", //
		immediate = true, //
		configurationPolicy = REQUIRE)
public class PredictorMqttImpl extends AbstractOpenemsComponent
		implements PredictorMqtt, Predictor, MqttComponent, OpenemsComponent {

	private static final Pattern HOUR_TOPIC = Pattern.compile("(today|tomorrow)_hour_(\\d{1,2})_kwh$");

	private final Logger log = LoggerFactory.getLogger(PredictorMqttImpl.class);
	private final MqttSubscriptions subscriptions = new MqttSubscriptions();

	/** Hour start (local time) -> average power [W]. */
	private final ConcurrentSkipListMap<LocalDateTime, Double> hourlyPower = new ConcurrentSkipListMap<>();

	@Reference
	private ConfigurationAdmin cm;

	@Reference
	private ComponentManager componentManager;

	@Reference(policy = STATIC, policyOption = GREEDY, cardinality = MANDATORY)
	private BridgeMqtt mqttBridge;

	private Config config;
	private ZoneId zone;
	private ChannelAddress[] channelAddresses = new ChannelAddress[0];

	public PredictorMqttImpl() {
		super(//
				OpenemsComponent.ChannelId.values(), //
				MqttComponent.ChannelId.values() //
		);
	}

	@Activate
	private void activate(ComponentContext context, Config config) throws OpenemsNamedException {
		super.activate(context, config.id(), config.alias(), config.enabled());
		this.config = config;
		if (OpenemsComponent.updateReferenceFilter(this.cm, this.servicePid(), "mqttBridge", config.mqttBridge_id())) {
			return;
		}
		this.zone = ZoneId.of(config.timeZone());
		var addresses = new ArrayList<ChannelAddress>();
		for (var address : config.channelAddresses()) {
			addresses.add(ChannelAddress.fromString(address));
		}
		this.channelAddresses = addresses.toArray(ChannelAddress[]::new);
		if (config.enabled()) {
			this.subscribe();
		}
	}

	@Override
	@Deactivate
	protected void deactivate() {
		this.subscriptions.unsubscribeAll();
		super.deactivate();
	}

	private void subscribe() {
		try {
			var prefix = this.config.topicPrefix();
			var filter = prefix.endsWith("/") ? prefix + "#" : prefix + "/#";
			this.subscriptions.subscribe(this.mqttBridge, filter, this::onMessage);
			this._setMqttCommunicationFailed(false);
		} catch (RuntimeException e) {
			this.logError(this.log, "Failed to subscribe to MQTT topics: " + e.getMessage());
			this._setMqttCommunicationFailed(true);
		}
	}

	@Override
	public void retryMqttCommunication() {
		this.subscriptions.unsubscribeAll();
		this.subscribe();
	}

	private void onMessage(MqttMessage message) {
		var matcher = HOUR_TOPIC.matcher(message.topic());
		if (!matcher.find()) {
			return;
		}
		var hour = Integer.parseInt(matcher.group(2));
		if (hour < 0 || hour > 23) {
			return;
		}
		var today = LocalDate.now(this.componentManager.getClock().withZone(this.zone));
		var date = matcher.group(1).equals("today") ? today : today.plusDays(1);
		var key = date.atTime(hour, 0);
		var kwh = MqttValues.parseDouble(message.payloadAsString(), "");
		if (kwh == null) {
			this.hourlyPower.remove(key);
		} else {
			this.hourlyPower.put(key, kwh * this.config.energyScale());
		}
		// Forget values older than two days
		this.hourlyPower.headMap(today.minusDays(2).atStartOfDay()).clear();
		this._setMqttCommunicationFailed(false);
	}

	@Override
	public ChannelAddress[] getChannelAddresses() {
		return this.channelAddresses;
	}

	@Override
	public Prediction getPrediction(ChannelAddress channelAddress) {
		if (!Arrays.asList(this.channelAddresses).contains(channelAddress) || this.hourlyPower.isEmpty()) {
			return Prediction.EMPTY_PREDICTION;
		}
		var now = DateUtils.roundDownToQuarter(Instant.now(this.componentManager.getClock()));
		var lastHour = this.hourlyPower.lastKey().atZone(this.zone).toInstant();
		var end = lastHour.plus(1, ChronoUnit.HOURS);
		var values = new ArrayList<Integer>();
		for (var t = now; t.isBefore(end); t = t.plus(15, ChronoUnit.MINUTES)) {
			var hourStart = LocalDateTime.ofInstant(t, this.zone).truncatedTo(ChronoUnit.HOURS);
			values.add(MqttValues.toInt(this.hourlyPower.get(hourStart)));
		}
		if (values.isEmpty()) {
			return Prediction.EMPTY_PREDICTION;
		}
		return Prediction.from(now, values.toArray(Integer[]::new));
	}

	private void _setMqttCommunicationFailed(boolean value) {
		this.channel(MqttComponent.ChannelId.MQTT_COMMUNICATION_FAILED).setNextValue(value);
	}
}
