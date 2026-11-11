package io.openems.edge.mqtt.generic.ess;

import static io.openems.common.utils.ConfigUtils.generateReferenceTargetFilter;

import io.openems.common.test.AbstractComponentConfig;

@SuppressWarnings("all")
public class MyConfig extends AbstractComponentConfig implements Config {

	protected static class Builder {
		private String id;
		private String mqttBridgeId = "mqtt0";
		private String socTopic = "";
		private String activePowerTopic = "";
		private boolean invertPower = true;
		private int capacity = 10000;
		private String chargeEnergyTopic = "";
		private String dischargeEnergyTopic = "";
		private String minCellVoltageTopic = "";
		private String maxCellVoltageTopic = "";
		private String temperatureTopic = "";

		private Builder() {
		}

		public Builder setId(String id) {
			this.id = id;
			return this;
		}

		public Builder setSocTopic(String topic) {
			this.socTopic = topic;
			return this;
		}

		public Builder setActivePowerTopic(String topic) {
			this.activePowerTopic = topic;
			return this;
		}

		public Builder setInvertPower(boolean invertPower) {
			this.invertPower = invertPower;
			return this;
		}

		public Builder setCapacity(int capacity) {
			this.capacity = capacity;
			return this;
		}

		public Builder setChargeEnergyTopic(String topic) {
			this.chargeEnergyTopic = topic;
			return this;
		}

		public Builder setDischargeEnergyTopic(String topic) {
			this.dischargeEnergyTopic = topic;
			return this;
		}

		public Builder setMinCellVoltageTopic(String topic) {
			this.minCellVoltageTopic = topic;
			return this;
		}

		public Builder setMaxCellVoltageTopic(String topic) {
			this.maxCellVoltageTopic = topic;
			return this;
		}

		public Builder setTemperatureTopic(String topic) {
			this.temperatureTopic = topic;
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
	public String mqttBridge_id() {
		return this.builder.mqttBridgeId;
	}

	@Override
	public String mqttBridge_target() {
		return generateReferenceTargetFilter(this.id(), this.mqttBridge_id());
	}

	@Override
	public String socTopic() {
		return this.builder.socTopic;
	}

	@Override
	public String activePowerTopic() {
		return this.builder.activePowerTopic;
	}

	@Override
	public boolean invertPower() {
		return this.builder.invertPower;
	}

	@Override
	public double powerScale() {
		return 1.0;
	}

	@Override
	public int capacity() {
		return this.builder.capacity;
	}

	@Override
	public int maxApparentPower() {
		return 5000;
	}

	@Override
	public String chargeEnergyTopic() {
		return this.builder.chargeEnergyTopic;
	}

	@Override
	public String dischargeEnergyTopic() {
		return this.builder.dischargeEnergyTopic;
	}

	@Override
	public double energyScale() {
		return 1000.0;
	}

	@Override
	public String minCellVoltageTopic() {
		return this.builder.minCellVoltageTopic;
	}

	@Override
	public String maxCellVoltageTopic() {
		return this.builder.maxCellVoltageTopic;
	}

	@Override
	public String temperatureTopic() {
		return this.builder.temperatureTopic;
	}

	@Override
	public String jsonPath() {
		return "";
	}

	@Override
	public int timeoutSeconds() {
		return 120;
	}
}
