package io.openems.edge.mqtt.generic.common;

import com.google.gson.JsonElement;
import com.google.gson.JsonParser;

/**
 * Helpers to parse numeric MQTT payloads.
 *
 * <p>
 * Supported payload formats:
 * <ul>
 * <li>Plain numbers, e.g. {@code -1333.01} (LoxBerry, Node-RED, ...)
 * <li>JSON numbers or strings, e.g. {@code "38"}
 * <li>JSON objects with a {@code value} field, e.g. {@code {"value": 38}}
 * (native Victron Venus OS / Cerbo GX format)
 * <li>JSON objects with an explicit dotted path, e.g. {@code data.power}
 * </ul>
 */
public final class MqttValues {

	private MqttValues() {
	}

	/**
	 * Parses a numeric value from an MQTT payload.
	 *
	 * @param payload  the payload as string
	 * @param jsonPath optional dotted JSON path (e.g. "data.power"); if empty, a
	 *                 "value" field is used for JSON objects
	 * @return the value, or null if the payload is empty, null or not numeric
	 */
	public static Double parseDouble(String payload, String jsonPath) {
		if (payload == null) {
			return null;
		}
		var trimmed = payload.trim();
		if (trimmed.isEmpty() || trimmed.equalsIgnoreCase("null")) {
			return null;
		}
		try {
			return Double.parseDouble(trimmed);
		} catch (NumberFormatException e) {
			// not a plain number -> try JSON
		}
		try {
			var element = JsonParser.parseString(trimmed);
			element = navigate(element, jsonPath);
			return toDouble(element);
		} catch (RuntimeException e) {
			return null;
		}
	}

	private static JsonElement navigate(JsonElement element, String jsonPath) {
		if (element == null || element.isJsonNull()) {
			return null;
		}
		if (jsonPath != null && !jsonPath.isBlank()) {
			for (var key : jsonPath.trim().split("\\.")) {
				if (element == null || !element.isJsonObject()) {
					return null;
				}
				element = element.getAsJsonObject().get(key);
			}
			return element;
		}
		if (element.isJsonObject() && element.getAsJsonObject().has("value")) {
			return element.getAsJsonObject().get("value");
		}
		return element;
	}

	private static Double toDouble(JsonElement element) {
		if (element == null || element.isJsonNull() || !element.isJsonPrimitive()) {
			return null;
		}
		var primitive = element.getAsJsonPrimitive();
		if (primitive.isNumber()) {
			return primitive.getAsDouble();
		}
		if (primitive.isString()) {
			try {
				return Double.parseDouble(primitive.getAsString().trim());
			} catch (NumberFormatException e) {
				return null;
			}
		}
		return null;
	}

	/**
	 * Rounds a nullable double to a nullable Integer.
	 *
	 * @param value the value
	 * @return the rounded value or null
	 */
	public static Integer toInt(Double value) {
		return value == null ? null : (int) Math.round(value);
	}

	/**
	 * Rounds a nullable double to a nullable Long.
	 *
	 * @param value the value
	 * @return the rounded value or null
	 */
	public static Long toLong(Double value) {
		return value == null ? null : Math.round(value);
	}
}
