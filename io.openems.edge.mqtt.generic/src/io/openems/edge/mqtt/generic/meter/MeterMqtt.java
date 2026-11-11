package io.openems.edge.mqtt.generic.meter;

import io.openems.edge.bridge.mqtt.api.MqttComponent;
import io.openems.edge.common.component.OpenemsComponent;
import io.openems.edge.meter.api.ElectricityMeter;

public interface MeterMqtt extends ElectricityMeter, MqttComponent, OpenemsComponent {

}
