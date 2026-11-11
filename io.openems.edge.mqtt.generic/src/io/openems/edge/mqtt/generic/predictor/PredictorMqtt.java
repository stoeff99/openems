package io.openems.edge.mqtt.generic.predictor;

import io.openems.edge.bridge.mqtt.api.MqttComponent;
import io.openems.edge.common.component.OpenemsComponent;
import io.openems.edge.predictor.api.prediction.Predictor;

public interface PredictorMqtt extends Predictor, MqttComponent, OpenemsComponent {

}
