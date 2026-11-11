package io.openems.edge.mqtt.generic.timeofusetariff;

import static java.time.format.DateTimeFormatter.ISO_OFFSET_DATE_TIME;
import static org.osgi.service.component.annotations.ConfigurationPolicy.REQUIRE;
import static org.osgi.service.component.annotations.ReferenceCardinality.MANDATORY;
import static org.osgi.service.component.annotations.ReferencePolicy.STATIC;
import static org.osgi.service.component.annotations.ReferencePolicyOption.GREEDY;

import java.time.Instant;
import java.time.ZonedDateTime;
import java.util.TreeMap;
import java.util.concurrent.atomic.AtomicReference;

import org.osgi.service.cm.ConfigurationAdmin;
import org.osgi.service.component.ComponentContext;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Deactivate;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.metatype.annotations.Designate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.collect.ImmutableSortedMap;
import com.google.gson.JsonArray;
import com.google.gson.JsonParser;

import io.openems.edge.bridge.mqtt.api.BridgeMqtt;
import io.openems.edge.bridge.mqtt.api.MqttComponent;
import io.openems.edge.bridge.mqtt.api.MqttMessage;
import io.openems.edge.common.component.AbstractOpenemsComponent;
import io.openems.edge.common.component.ComponentManager;
import io.openems.edge.common.component.OpenemsComponent;
import io.openems.edge.mqtt.generic.common.MqttSubscriptions;
import io.openems.edge.timeofusetariff.api.TimeOfUsePrices;
import io.openems.edge.timeofusetariff.api.TimeOfUseTariff;

@Designate(ocd = Config.class, factory = true)
@Component(//
		name = "TimeOfUseTariff.Mqtt", //
		immediate = true, //
		configurationPolicy = REQUIRE)
public class TimeOfUseTariffMqttImpl extends AbstractOpenemsComponent
		implements TimeOfUseTariffMqtt, TimeOfUseTariff, MqttComponent, OpenemsComponent {

	private final Logger log = LoggerFactory.getLogger(TimeOfUseTariffMqttImpl.class);
	private final MqttSubscriptions subscriptions = new MqttSubscriptions();
	private final AtomicReference<TimeOfUsePrices> prices = new AtomicReference<>(TimeOfUsePrices.EMPTY_PRICES);

	@Reference
	private ConfigurationAdmin cm;

	@Reference
	private ComponentManager componentManager;

	@Reference(policy = STATIC, policyOption = GREEDY, cardinality = MANDATORY)
	private BridgeMqtt mqttBridge;

	private Config config;

	public TimeOfUseTariffMqttImpl() {
		super(//
				OpenemsComponent.ChannelId.values(), //
				MqttComponent.ChannelId.values() //
		);
	}

	@Activate
	private void activate(ComponentContext context, Config config) {
		super.activate(context, config.id(), config.alias(), config.enabled());
		this.config = config;
		if (OpenemsComponent.updateReferenceFilter(this.cm, this.servicePid(), "mqttBridge", config.mqttBridge_id())) {
			return;
		}
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
			this.subscriptions.subscribe(this.mqttBridge, this.config.topic(), this::onMessage);
			this._setMqttCommunicationFailed(false);
		} catch (RuntimeException e) {
			this.logError(this.log, "Failed to subscribe to MQTT topic: " + e.getMessage());
			this._setMqttCommunicationFailed(true);
		}
	}

	@Override
	public void retryMqttCommunication() {
		this.subscriptions.unsubscribeAll();
		this.subscribe();
	}

	private void onMessage(MqttMessage message) {
		try {
			var parsed = parsePrices(message.payloadAsString(), this.config.arrayField(), this.config.startField(),
					this.config.priceField(), this.config.priceScale());
			this.prices.set(parsed);
			this._setMqttCommunicationFailed(false);
		} catch (RuntimeException e) {
			this.logWarn(this.log, "Unable to parse prices from [" + message.topic() + "]: " + e.getMessage());
		}
	}

	/**
	 * Parses a JSON price list.
	 *
	 * @param payload    the JSON payload
	 * @param arrayField the name of the array field; empty if the payload is an
	 *                   array
	 * @param startField the name of the start timestamp field
	 * @param priceField the name of the price field
	 * @param priceScale the factor to convert to price per MWh
	 * @return the {@link TimeOfUsePrices}
	 */
	protected static TimeOfUsePrices parsePrices(String payload, String arrayField, String startField,
			String priceField, double priceScale) {
		var json = JsonParser.parseString(payload);
		final JsonArray array;
		if (arrayField == null || arrayField.isBlank()) {
			array = json.getAsJsonArray();
		} else {
			array = json.getAsJsonObject().getAsJsonArray(arrayField);
		}
		var map = new TreeMap<Instant, Double>();
		for (var element : array) {
			var obj = element.getAsJsonObject();
			var start = obj.get(startField);
			var price = obj.get(priceField);
			if (start == null || start.isJsonNull() || price == null || price.isJsonNull()) {
				continue;
			}
			var time = ZonedDateTime.parse(start.getAsString(), ISO_OFFSET_DATE_TIME).toInstant();
			map.put(time, price.getAsDouble() * priceScale);
		}
		return TimeOfUsePrices.from(ImmutableSortedMap.copyOf(map));
	}

	@Override
	public TimeOfUsePrices getPrices() {
		return TimeOfUsePrices.from(Instant.now(this.componentManager.getClock()), this.prices.get());
	}

	private void _setMqttCommunicationFailed(boolean value) {
		this.channel(MqttComponent.ChannelId.MQTT_COMMUNICATION_FAILED).setNextValue(value);
	}
}
