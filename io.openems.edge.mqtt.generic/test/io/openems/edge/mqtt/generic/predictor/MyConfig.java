package io.openems.edge.mqtt.generic.predictor;

import static io.openems.common.utils.ConfigUtils.generateReferenceTargetFilter;

import io.openems.common.test.AbstractComponentConfig;

@SuppressWarnings("all")
public class MyConfig extends AbstractComponentConfig implements Config {

	protected static class Builder {
		private String id;

		private Builder() {
		}

		public Builder setId(String id) {
			this.id = id;
			return this;
		}

		public MyConfig build() {
			return new MyConfig(this);
		}
	}

	public static Builder create() {
		return new Builder();
	}

	private MyConfig(Builder builder) {
		super(Config.class, builder.id);
	}

	@Override
	public String mqttBridge_id() {
		return "mqtt0";
	}

	@Override
	public String mqttBridge_target() {
		return generateReferenceTargetFilter(this.id(), this.mqttBridge_id());
	}

	@Override
	public String topicPrefix() {
		return "solcast/sum/";
	}

	@Override
	public String[] channelAddresses() {
		return new String[] { "_sum/ProductionActivePower" };
	}

	@Override
	public String timeZone() {
		return "Europe/Zurich";
	}

	@Override
	public double energyScale() {
		return 1000.0;
	}
}
