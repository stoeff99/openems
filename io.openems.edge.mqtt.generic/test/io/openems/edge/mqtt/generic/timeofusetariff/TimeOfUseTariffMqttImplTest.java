package io.openems.edge.mqtt.generic.timeofusetariff;

import static io.openems.common.test.TestUtils.createDummyClock;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.time.Instant;
import java.time.ZonedDateTime;

import org.junit.Test;

import io.openems.common.test.DummyConfigurationAdmin;
import io.openems.common.test.TimeLeapClock;
import io.openems.edge.bridge.mqtt.test.DummyBridgeMqtt;
import io.openems.edge.common.test.ComponentTest;
import io.openems.edge.common.test.DummyComponentManager;

public class TimeOfUseTariffMqttImplTest {

	private static final String PAYLOAD = """
			{"interval_count":3,"intervals":[
			 {"chf_m_per_hour":0.0111,"chf_per_kwh_sum":0.19876,"end_timestamp":"2026-11-11T00:15:00+01:00",
			  "month_hours_used":720,"start_timestamp":"2026-11-11T00:00:00+01:00","total_chf":0.20987},
			 {"chf_m_per_hour":0.0111,"chf_per_kwh_sum":0.19733,"end_timestamp":"2026-11-11T00:30:00+01:00",
			  "month_hours_used":720,"start_timestamp":"2026-11-11T00:15:00+01:00","total_chf":0.20844},
			 {"chf_m_per_hour":0.0111,"chf_per_kwh_sum":0.1871,"end_timestamp":"2026-11-11T00:45:00+01:00",
			  "month_hours_used":720,"start_timestamp":"2026-11-11T00:30:00+01:00","total_chf":0.19821}
			]}""";

	@Test
	public void testParsePrices() {
		var prices = TimeOfUseTariffMqttImpl.parsePrices(PAYLOAD, "intervals", "start_timestamp", "chf_per_kwh_sum",
				1000);
		var start = ZonedDateTime.parse("2026-11-11T00:00:00+01:00").toInstant();
		assertEquals(198.76, prices.getFirst(), 0.001);
		assertEquals(197.33, prices.getAt(start.plusSeconds(900)), 0.001);
		assertEquals(187.1, prices.getAt(start.plusSeconds(1800)), 0.001);
	}

	@Test
	public void testComponent() throws Exception {
		var clock = new TimeLeapClock(Instant.parse("2026-11-10T23:15:00Z"));
		var bridge = new DummyBridgeMqtt("mqtt0");
		var sut = new TimeOfUseTariffMqttImpl();
		new ComponentTest(sut) //
				.addReference("cm", new DummyConfigurationAdmin()) //
				.addReference("componentManager", new DummyComponentManager(clock)) //
				.addReference("mqttBridge", bridge) //
				.activate(MyConfig.create().setId("timeOfUseTariff0").build());
		assertTrue(sut.getPrices().isEmpty());
		bridge.simulateMessage("ekz/ems/tariffs/intervals", PAYLOAD);
		// 23:15Z == 00:15+01:00 -> second interval is first
		assertEquals(197.33, sut.getPrices().getFirst(), 0.001);
	}
}
