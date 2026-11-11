package io.openems.edge.mqtt.generic.common;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

import io.openems.edge.bridge.mqtt.api.BridgeMqtt;
import io.openems.edge.bridge.mqtt.api.BridgeMqtt.MqttSubscription;
import io.openems.edge.bridge.mqtt.api.MqttMessage;
import io.openems.edge.bridge.mqtt.api.QoS;

/**
 * Keeps track of the MQTT subscriptions of one component.
 */
public class MqttSubscriptions {

	private final List<MqttSubscription> subscriptions = new ArrayList<>();

	/**
	 * Subscribes to a topic if the topic is not blank.
	 *
	 * @param bridge   the {@link BridgeMqtt}
	 * @param topic    the topic (filter); ignored if null or blank
	 * @param callback the callback receiving the {@link MqttMessage}
	 */
	public synchronized void subscribe(BridgeMqtt bridge, String topic, Consumer<MqttMessage> callback) {
		if (bridge == null || topic == null || topic.isBlank()) {
			return;
		}
		this.subscriptions.add(bridge.subscribe(topic.trim(), QoS.AT_LEAST_ONCE, callback));
	}

	/**
	 * Unsubscribes from all topics.
	 */
	public synchronized void unsubscribeAll() {
		for (var subscription : this.subscriptions) {
			try {
				subscription.unsubscribe();
			} catch (RuntimeException e) {
				// ignore errors during cleanup
			}
		}
		this.subscriptions.clear();
	}

	/**
	 * Checks whether a topic is configured.
	 *
	 * @param topic the topic
	 * @return true if not null and not blank
	 */
	public static boolean isSet(String topic) {
		return topic != null && !topic.isBlank();
	}
}
