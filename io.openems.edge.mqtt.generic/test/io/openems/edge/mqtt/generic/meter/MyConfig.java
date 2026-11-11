package io.openems.edge.mqtt.generic.meter;

import static io.openems.common.utils.ConfigUtils.generateReferenceTargetFilter;

import io.openems.common.test.AbstractComponentConfig;
import io.openems.common.types.MeterType;

@SuppressWarnings("all")
public class MyConfig extends AbstractComponentConfig implements Config {

	protected static class Builder {
		private String id;
		private MeterType type = MeterType.GRID;
		private String mqttBridgeId = "mqtt0";
		private String activePowerTopic = "";
		private String activePowerL1Topic = "";
		private String activePowerL2Topic = "";
		private String activePowerL3Topic = "";
		private String productionEnergyTopic = "";
		private String consumptionEnergyTopic = "";
		private String jsonPath = "";
		private boolean invert = false;
		private double powerScale = 1.0;
		private double energyScale = 1000.0;
		private int timeoutSeconds = 120;

		private Builder() {
		}

		public Builder setId(String id) {
			this.id = id;
			return this;
		}

		public Builder setType(MeterType type) {
			this.type = type;
			return this;
		}

		public Builder setMqttBridgeId(String mqttBridgeId) {
			this.mqttBridgeId = mqttBridgeId;
			return this;
		}

		public Builder setActivePowerTopic(String topic) {
			this.activePowerTopic = topic;
			return this;
		}

		public Builder setActivePowerL1Topic(String topic) {
			this.activePowerL1Topic = topic;
			return this;
		}

		public Builder setActivePowerL2Topic(String topic) {
			this.activePowerL2Topic = topic;
			return this;
		}

		public Builder setActivePowerL3Topic(String topic) {
			this.activePowerL3Topic = topic;
			return this;
		}

		public Builder setProductionEnergyTopic(String topic) {
			this.productionEnergyTopic = topic;
			return this;
		}

		public Builder setConsumptionEnergyTopic(String topic) {
			this.consumptionEnergyTopic = topic;
			return this;
		}

		public Builder setJsonPath(String jsonPath) {
			this.jsonPath = jsonPath;
			return this;
		}

		public Builder setInvert(boolean invert) {
			this.invert = invert;
			return this;
		}

		public Builder setPowerScale(double powerScale) {
			this.powerScale = powerScale;
			return this;
		}

		public Builder setTimeoutSeconds(int timeoutSeconds) {
			this.timeoutSeconds = timeoutSeconds;
			return this;
		}

		public MyConfig build() {
			return new MyConfig(this);
		}
	}

	public static Builder create() {
		return new Builder();
	}

	private final Builder builder;

	private MyConfig(Builder builder) {
		super(Config.class, builder.id);
		this.builder = builder;
	}

	@Override
	public MeterType type() {
		return this.builder.type;
	}

	@Override
	public String mqttBridge_id() {
		return this.builder.mqttBridgeId;
	}

	@Override
	public String mqttBridge_target() {
		return generateReferenceTargetFilter(this.id(), this.mqttBridge_id());
	}

	@Override
	public String activePowerTopic() {
		return this.builder.activePowerTopic;
	}

	@Override
	public String activePowerL1Topic() {
		return this.builder.activePowerL1Topic;
	}

	@Override
	public String activePowerL2Topic() {
		return this.builder.activePowerL2Topic;
	}

	@Override
	public String activePowerL3Topic() {
		return this.builder.activePowerL3Topic;
	}

	@Override
	public String productionEnergyTopic() {
		return this.builder.productionEnergyTopic;
	}

	@Override
	public String consumptionEnergyTopic() {
		return this.builder.consumptionEnergyTopic;
	}

	@Override
	public String jsonPath() {
		return this.builder.jsonPath;
	}

	@Override
	public boolean invert() {
		return this.builder.invert;
	}

	@Override
	public double powerScale() {
		return this.builder.powerScale;
	}

	@Override
	public double energyScale() {
		return this.builder.energyScale;
	}

	@Override
	public int timeoutSeconds() {
		return this.builder.timeoutSeconds;
	}
}
