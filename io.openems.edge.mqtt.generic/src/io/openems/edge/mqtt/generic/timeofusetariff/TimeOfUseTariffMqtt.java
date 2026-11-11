package io.openems.edge.mqtt.generic.timeofusetariff;

import io.openems.edge.bridge.mqtt.api.MqttComponent;
import io.openems.edge.common.component.OpenemsComponent;
import io.openems.edge.timeofusetariff.api.TimeOfUseTariff;

public interface TimeOfUseTariffMqtt extends TimeOfUseTariff, MqttComponent, OpenemsComponent {

}
