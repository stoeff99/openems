package io.openems.edge.mqtt.generic.meter;

import static io.openems.edge.mqtt.generic.common.MqttSubscriptions.isSet;
import static io.openems.edge.mqtt.generic.common.MqttValues.toInt;
import static io.openems.edge.mqtt.generic.common.MqttValues.toLong;
import static org.osgi.service.component.annotations.ConfigurationPolicy.REQUIRE;
import static org.osgi.service.component.annotations.ReferenceCardinality.MANDATORY;
import static org.osgi.service.component.annotations.ReferenceCardinality.OPTIONAL;
import static org.osgi.service.component.annotations.ReferencePolicy.DYNAMIC;
import static org.osgi.service.component.annotations.ReferencePolicy.STATIC;
import static org.osgi.service.component.annotations.ReferencePolicyOption.GREEDY;

import java.time.Duration;
import java.time.Instant;

import org.osgi.service.cm.ConfigurationAdmin;
import org.osgi.service.component.ComponentContext;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Deactivate;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.event.Event;
import org.osgi.service.event.EventHandler;
import org.osgi.service.event.propertytypes.EventTopics;
import org.osgi.service.metatype.annotations.Designate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.openems.common.channel.AccessMode;
import io.openems.common.types.MeterType;
import io.openems.edge.bridge.mqtt.api.BridgeMqtt;
import io.openems.edge.bridge.mqtt.api.MqttComponent;
import io.openems.edge.bridge.mqtt.api.MqttMessage;
import io.openems.edge.common.component.AbstractOpenemsComponent;
import io.openems.edge.common.component.ComponentManager;
import io.openems.edge.common.component.OpenemsComponent;
import io.openems.edge.common.event.EdgeEventConstants;
import io.openems.edge.common.modbusslave.ModbusSlave;
import io.openems.edge.common.modbusslave.ModbusSlaveTable;
import io.openems.edge.meter.api.ElectricityMeter;
import io.openems.edge.mqtt.generic.common.MqttSubscriptions;
import io.openems.edge.mqtt.generic.common.MqttValues;
import io.openems.edge.timedata.api.Timedata;
import io.openems.edge.timedata.api.TimedataProvider;
import io.openems.edge.timedata.api.utils.CalculateEnergyFromPower;

@Designate(ocd = Config.class, factory = true)
@Component(//
		name = "Meter.Mqtt", //
		immediate = true, //
		configurationPolicy = REQUIRE)
@EventTopics({ //
		EdgeEventConstants.TOPIC_CYCLE_BEFORE_PROCESS_IMAGE, //
		EdgeEventConstants.TOPIC_CYCLE_AFTER_PROCESS_IMAGE //
})
public class MeterMqttImpl extends AbstractOpenemsComponent
		implements MeterMqtt, ElectricityMeter, MqttComponent, OpenemsComponent, TimedataProvider, EventHandler,
		ModbusSlave {

	private final Logger log = LoggerFactory.getLogger(MeterMqttImpl.class);

	private final CalculateEnergyFromPower calculateProductionEnergy = new CalculateEnergyFromPower(this,
			ElectricityMeter.ChannelId.ACTIVE_PRODUCTION_ENERGY);
	private final CalculateEnergyFromPower calculateConsumptionEnergy = new CalculateEnergyFromPower(this,
			ElectricityMeter.ChannelId.ACTIVE_CONSUMPTION_ENERGY);
	private final MqttSubscriptions subscriptions = new MqttSubscriptions();
	private final Double[] phasePowers = new Double[3];

	@Reference
	private ConfigurationAdmin cm;

	@Reference
	private ComponentManager componentManager;

	@Reference(policy = DYNAMIC, policyOption = GREEDY, cardinality = OPTIONAL)
	private volatile Timedata timedata;

	@Reference(policy = STATIC, policyOption = GREEDY, cardinality = MANDATORY)
	private BridgeMqtt mqttBridge;

	private Config config;
	private volatile Instant lastPowerMessage = null;

	public MeterMqttImpl() {
		super(//
				OpenemsComponent.ChannelId.values(), //
				ElectricityMeter.ChannelId.values(), //
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
		if (!isSet(config.activePowerTopic()) && !isSet(config.activePowerL1Topic())
				&& !isSet(config.activePowerL2Topic()) && !isSet(config.activePowerL3Topic())) {
			this.logWarn(this.log, "No Active Power topic configured");
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
			var c = this.config;
			this.subscriptions.subscribe(this.mqttBridge, c.activePowerTopic(), this::onActivePower);
			this.subscriptions.subscribe(this.mqttBridge, c.activePowerL1Topic(), m -> this.onPhasePower(0, m));
			this.subscriptions.subscribe(this.mqttBridge, c.activePowerL2Topic(), m -> this.onPhasePower(1, m));
			this.subscriptions.subscribe(this.mqttBridge, c.activePowerL3Topic(), m -> this.onPhasePower(2, m));
			this.subscriptions.subscribe(this.mqttBridge, c.productionEnergyTopic(), m -> this
					._setActiveProductionEnergy(toLong(this.scaleEnergy(this.parse(m)))));
			this.subscriptions.subscribe(this.mqttBridge, c.consumptionEnergyTopic(), m -> this
					._setActiveConsumptionEnergy(toLong(this.scaleEnergy(this.parse(m)))));
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

	private Double parse(MqttMessage message) {
		return MqttValues.parseDouble(message.payloadAsString(), this.config.jsonPath());
	}

	private Double scalePower(Double value) {
		if (value == null) {
			return null;
		}
		return value * this.config.powerScale() * (this.config.invert() ? -1 : 1);
	}

	private Double scaleEnergy(Double value) {
		return value == null ? null : value * this.config.energyScale();
	}

	private void onActivePower(MqttMessage message) {
		var value = this.scalePower(this.parse(message));
		this._setActivePower(toInt(value));
		this.messageReceived();
	}

	private void onPhasePower(int phase, MqttMessage message) {
		var value = this.scalePower(this.parse(message));
		switch (phase) {
		case 0 -> this._setActivePowerL1(toInt(value));
		case 1 -> this._setActivePowerL2(toInt(value));
		case 2 -> this._setActivePowerL3(toInt(value));
		}
		if (isSet(this.config.activePowerTopic())) {
			return;
		}
		// No total topic -> calculate total from the configured phases
		synchronized (this.phasePowers) {
			this.phasePowers[phase] = value;
			var configured = new String[] { this.config.activePowerL1Topic(), this.config.activePowerL2Topic(),
					this.config.activePowerL3Topic() };
			double sum = 0;
			for (var i = 0; i < 3; i++) {
				if (!isSet(configured[i])) {
					continue;
				}
				if (this.phasePowers[i] == null) {
					return; // wait until all configured phases are known
				}
				sum += this.phasePowers[i];
			}
			this._setActivePower(toInt(sum));
		}
		this.messageReceived();
	}

	private void messageReceived() {
		this.lastPowerMessage = Instant.now(this.componentManager.getClock());
		this._setMqttCommunicationFailed(false);
	}

	@Override
	public void handleEvent(Event event) {
		if (!this.isEnabled()) {
			return;
		}
		switch (event.getTopic()) {
		case EdgeEventConstants.TOPIC_CYCLE_BEFORE_PROCESS_IMAGE -> this.checkTimeout();
		case EdgeEventConstants.TOPIC_CYCLE_AFTER_PROCESS_IMAGE -> this.calculateEnergy();
		}
	}

	private void checkTimeout() {
		if (this.config.timeoutSeconds() <= 0) {
			return;
		}
		var last = this.lastPowerMessage;
		var now = Instant.now(this.componentManager.getClock());
		if (last != null && Duration.between(last, now).getSeconds() > this.config.timeoutSeconds()) {
			this._setActivePower(null);
			this._setActivePowerL1(null);
			this._setActivePowerL2(null);
			this._setActivePowerL3(null);
			this._setMqttCommunicationFailed(true);
		}
	}

	private void calculateEnergy() {
		var activePower = this.getActivePower().get();
		if (!isSet(this.config.productionEnergyTopic())) {
			this.calculateProductionEnergy.update(activePower == null ? null : Math.max(0, activePower));
		}
		if (!isSet(this.config.consumptionEnergyTopic())) {
			this.calculateConsumptionEnergy.update(activePower == null ? null : Math.max(0, -activePower));
		}
	}

	private void _setMqttCommunicationFailed(boolean value) {
		this.channel(MqttComponent.ChannelId.MQTT_COMMUNICATION_FAILED).setNextValue(value);
	}

	@Override
	public MeterType getMeterType() {
		return this.config.type();
	}

	@Override
	public Timedata getTimedata() {
		return this.timedata;
	}

	@Override
	public String debugLog() {
		return "L:" + this.getActivePower().asString();
	}

	@Override
	public ModbusSlaveTable getModbusSlaveTable(AccessMode accessMode) {
		return new ModbusSlaveTable(//
				OpenemsComponent.getModbusSlaveNatureTable(accessMode), //
				ElectricityMeter.getModbusSlaveNatureTable(accessMode));
	}
}
