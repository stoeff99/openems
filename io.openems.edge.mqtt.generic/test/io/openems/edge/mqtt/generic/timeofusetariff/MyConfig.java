package io.openems.edge.mqtt.generic.timeofusetariff;

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
	public String topic() {
		return "ekz/ems/tariffs/intervals";
	}

	@Override
	public String arrayField() {
		return "intervals";
	}

	@Override
	public String startField() {
		return "start_timestamp";
	}

	@Override
	public String priceField() {
		return "chf_per_kwh_sum";
	}

	@Override
	public double priceScale() {
		return 1000.0;
	}
}
