package io.openems.edge.mqtt.generic.ess;

import io.openems.edge.bridge.mqtt.api.MqttComponent;
import io.openems.edge.common.component.OpenemsComponent;
import io.openems.edge.ess.api.SymmetricEss;

public interface EssMqtt extends SymmetricEss, MqttComponent, OpenemsComponent {

}
