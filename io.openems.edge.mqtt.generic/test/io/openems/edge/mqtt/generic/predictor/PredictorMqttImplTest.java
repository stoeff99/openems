package io.openems.edge.mqtt.generic.predictor;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.time.Instant;

import org.junit.Test;

import io.openems.common.test.DummyConfigurationAdmin;
import io.openems.common.test.TimeLeapClock;
import io.openems.common.types.ChannelAddress;
import io.openems.edge.bridge.mqtt.test.DummyBridgeMqtt;
import io.openems.edge.common.test.ComponentTest;
import io.openems.edge.common.test.DummyComponentManager;

public class PredictorMqttImplTest {

	private static final ChannelAddress PRODUCTION = new ChannelAddress("_sum", "ProductionActivePower");

	@Test
	public void test() throws Exception {
		// 2026-11-11 09:20 local time (UTC+1)
		var clock = new TimeLeapClock(Instant.parse("2026-11-11T08:20:00Z"));
		var bridge = new DummyBridgeMqtt("mqtt0");
		var sut = new PredictorMqttImpl();
		new ComponentTest(sut) //
				.addReference("cm", new DummyConfigurationAdmin()) //
				.addReference("componentManager", new DummyComponentManager(clock)) //
				.addReference("mqttBridge", bridge) //
				.activate(MyConfig.create().setId("predictor0").build());

		assertTrue(bridge.isSubscribed("solcast/sum/#"));
		assertTrue(sut.getPrediction(PRODUCTION).isEmpty());

		bridge.simulateMessage("solcast/sum/today_hour_09_kwh", "0.2");
		bridge.simulateMessage("solcast/sum/today_hour_10_kwh", "0.275");
		bridge.simulateMessage("solcast/sum/today_hour_11_kwh", "0.801");
		bridge.simulateMessage("solcast/sum/tomorrow_hour_00_kwh", "0");
		bridge.simulateMessage("solcast/sum/today_peak_1_kwh", "0.856"); // ignored

		var prediction = sut.getPrediction(PRODUCTION);
		var start = Instant.parse("2026-11-11T08:15:00Z"); // 09:15 local
		assertEquals(start, prediction.getFirstTime());
		assertEquals(Integer.valueOf(200), prediction.getAt(start));
		assertEquals(Integer.valueOf(200), prediction.getAt(start.plusSeconds(30 * 60)));
		assertEquals(Integer.valueOf(275), prediction.getAt(start.plusSeconds(45 * 60))); // 10:00
		assertEquals(Integer.valueOf(801), prediction.getAt(start.plusSeconds(105 * 60))); // 11:00
		assertNull(prediction.getAt(start.plusSeconds(165 * 60))); // 12:00 unknown

		assertTrue(sut.getPrediction(new ChannelAddress("_sum", "ConsumptionActivePower")).isEmpty());
	}
}
