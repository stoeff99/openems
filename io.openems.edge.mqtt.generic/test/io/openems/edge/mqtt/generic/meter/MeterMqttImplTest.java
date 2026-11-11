package io.openems.edge.mqtt.generic.meter;

import static io.openems.common.test.TestUtils.createDummyClock;
import static java.time.temporal.ChronoUnit.SECONDS;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

import io.openems.common.test.DummyConfigurationAdmin;
import io.openems.common.types.MeterType;
import io.openems.edge.bridge.mqtt.api.MqttComponent;
import io.openems.edge.bridge.mqtt.test.DummyBridgeMqtt;
import io.openems.edge.common.test.AbstractComponentTest.TestCase;
import io.openems.edge.common.test.ComponentTest;
import io.openems.edge.common.test.DummyComponentManager;
import io.openems.edge.meter.api.ElectricityMeter;

public class MeterMqttImplTest {

	private static final String PV = "Cerbo/N/c0619ab7a15e/pvinverter/20/Ac/Power";
	private static final String PV_ENERGY = "Cerbo/N/c0619ab7a15e/pvinverter/20/Ac/Energy/Forward";

	@Test
	public void testProductionMeter() throws Exception {
		var clock = createDummyClock();
		var bridge = new DummyBridgeMqtt("mqtt0");
		new ComponentTest(new MeterMqttImpl()) //
				.addReference("cm", new DummyConfigurationAdmin()) //
				.addReference("componentManager", new DummyComponentManager(clock)) //
				.addReference("mqttBridge", bridge) //
				.activate(MyConfig.create() //
						.setId("meter1") //
						.setType(MeterType.PRODUCTION) //
						.setActivePowerTopic(PV) //
						.setProductionEnergyTopic(PV_ENERGY) //
						.setTimeoutSeconds(60) //
						.build()) //
				.next(new TestCase() //
						.onBeforeProcessImage(() -> {
							assertTrue(bridge.isSubscribed(PV));
							bridge.simulateMessage(PV, "1234.6");
							bridge.simulateMessage(PV_ENERGY, "{\"value\": 12.5}");
						}) //
						.output(ElectricityMeter.ChannelId.ACTIVE_POWER, 1235) //
						.output(ElectricityMeter.ChannelId.ACTIVE_PRODUCTION_ENERGY, 12500L) //
						.output(MqttComponent.ChannelId.MQTT_COMMUNICATION_FAILED, false)) //
				.next(new TestCase() //
						.timeleap(clock, 61, SECONDS) //
						.output(ElectricityMeter.ChannelId.ACTIVE_POWER, null) //
						.output(MqttComponent.ChannelId.MQTT_COMMUNICATION_FAILED, true)) //
				.deactivate();
	}

	@Test
	public void testGridMeterFromPhasesInverted() throws Exception {
		var bridge = new DummyBridgeMqtt("mqtt0");
		new ComponentTest(new MeterMqttImpl()) //
				.addReference("cm", new DummyConfigurationAdmin()) //
				.addReference("componentManager", new DummyComponentManager(createDummyClock())) //
				.addReference("mqttBridge", bridge) //
				.activate(MyConfig.create() //
						.setId("meter0") //
						.setType(MeterType.GRID) //
						.setActivePowerL1Topic("grid/l1") //
						.setActivePowerL2Topic("grid/l2") //
						.setActivePowerL3Topic("grid/l3") //
						.setInvert(true) //
						.build()) //
				.next(new TestCase() //
						.onBeforeProcessImage(() -> {
							bridge.simulateMessage("grid/l1", "100");
							bridge.simulateMessage("grid/l2", "200");
						}) //
						.output(ElectricityMeter.ChannelId.ACTIVE_POWER_L1, -100) //
						.output(ElectricityMeter.ChannelId.ACTIVE_POWER, null)) //
				.next(new TestCase() //
						.onBeforeProcessImage(() -> bridge.simulateMessage("grid/l3", "-50")) //
						.output(ElectricityMeter.ChannelId.ACTIVE_POWER_L3, 50) //
						.output(ElectricityMeter.ChannelId.ACTIVE_POWER, -250)) //
				.deactivate();
	}

	@Test
	public void testJsonPathAndScale() throws Exception {
		var bridge = new DummyBridgeMqtt("mqtt0");
		new ComponentTest(new MeterMqttImpl()) //
				.addReference("cm", new DummyConfigurationAdmin()) //
				.addReference("componentManager", new DummyComponentManager(createDummyClock())) //
				.addReference("mqttBridge", bridge) //
				.activate(MyConfig.create() //
						.setId("meter2") //
						.setType(MeterType.CONSUMPTION_METERED) //
						.setActivePowerTopic("house/power") //
						.setJsonPath("data.kw") //
						.setPowerScale(1000) //
						.build()) //
				.next(new TestCase() //
						.onBeforeProcessImage(() -> bridge.simulateMessage("house/power", "{\"data\":{\"kw\":1.5}}")) //
						.output(ElectricityMeter.ChannelId.ACTIVE_POWER, 1500)) //
				.deactivate();
	}
}
