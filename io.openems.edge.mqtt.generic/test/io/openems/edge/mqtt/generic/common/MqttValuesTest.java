package io.openems.edge.mqtt.generic.common;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

import org.junit.Test;

public class MqttValuesTest {

	@Test
	public void testParseDouble() {
		assertEquals(-1333.01, MqttValues.parseDouble("-1333.01", ""), 0.0001);
		assertEquals(38.0, MqttValues.parseDouble(" 38 ", ""), 0.0001);
		assertEquals(38.0, MqttValues.parseDouble("{\"value\": 38}", ""), 0.0001);
		assertEquals(38.0, MqttValues.parseDouble("\"38\"", ""), 0.0001);
		assertEquals(2.5, MqttValues.parseDouble("{\"a\":{\"b\":2.5}}", "a.b"), 0.0001);
		assertNull(MqttValues.parseDouble("", ""));
		assertNull(MqttValues.parseDouble("{\"value\": null}", ""));
		assertNull(MqttValues.parseDouble("sum", ""));
		assertNull(MqttValues.parseDouble("{\"a\":1}", "x.y"));
		assertNull(MqttValues.parseDouble(null, ""));
	}
}
