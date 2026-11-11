package io.openems.edge.mqtt.generic.ess;

import static io.openems.common.test.TestUtils.createDummyClock;

import org.junit.Test;

import io.openems.common.test.DummyConfigurationAdmin;
import io.openems.edge.bridge.mqtt.test.DummyBridgeMqtt;
import io.openems.edge.common.sum.GridMode;
import io.openems.edge.common.test.AbstractComponentTest.TestCase;
import io.openems.edge.common.test.ComponentTest;
import io.openems.edge.common.test.DummyComponentManager;
import io.openems.edge.ess.api.SymmetricEss;

public class EssMqttImplTest {

	private static final String PREFIX = "Cerbo/N/c0619ab7a15e/";
	private static final String SOC = PREFIX + "system/0/Dc/Battery/Soc";
	private static final String POWER = PREFIX + "system/0/Dc/Battery/Power";
	private static final String CHARGED = PREFIX + "battery/512/History/ChargedEnergy";
	private static final String DISCHARGED = PREFIX + "battery/512/History/DischargedEnergy";
	private static final String MIN_CELL = PREFIX + "battery/512/System/MinCellVoltage";
	private static final String MAX_CELL = PREFIX + "battery/512/System/MaxCellVoltage";
	private static final String TEMP = PREFIX + "system/0/Dc/Battery/Temperature";

	@Test
	public void testCerbo() throws Exception {
		var bridge = new DummyBridgeMqtt("mqtt0");
		new ComponentTest(new EssMqttImpl()) //
				.addReference("cm", new DummyConfigurationAdmin()) //
				.addReference("componentManager", new DummyComponentManager(createDummyClock())) //
				.addReference("mqttBridge", bridge) //
				.activate(MyConfig.create() //
						.setId("ess0") //
						.setSocTopic(SOC) //
						.setActivePowerTopic(POWER) //
						.setCapacity(6000) //
						.setChargeEnergyTopic(CHARGED) //
						.setDischargeEnergyTopic(DISCHARGED) //
						.setMinCellVoltageTopic(MIN_CELL) //
						.setMaxCellVoltageTopic(MAX_CELL) //
						.setTemperatureTopic(TEMP) //
						.build()) //
				.next(new TestCase() //
						.onBeforeProcessImage(() -> {
							bridge.simulateMessage(SOC, "38");
							bridge.simulateMessage(POWER, "-1333.01"); // Victron: discharging
							bridge.simulateMessage(CHARGED, "918.279");
							bridge.simulateMessage(DISCHARGED, "889.781");
							bridge.simulateMessage(MIN_CELL, "3.261");
							bridge.simulateMessage(MAX_CELL, "3.263");
							bridge.simulateMessage(TEMP, "20");
						}) //
						.output(SymmetricEss.ChannelId.SOC, 38) //
						.output(SymmetricEss.ChannelId.ACTIVE_POWER, 1333) // OpenEMS: + = discharge
						.output(SymmetricEss.ChannelId.CAPACITY, 6000) //
						.output(SymmetricEss.ChannelId.GRID_MODE, GridMode.ON_GRID) //
						.output(SymmetricEss.ChannelId.ACTIVE_CHARGE_ENERGY, 918279L) //
						.output(SymmetricEss.ChannelId.ACTIVE_DISCHARGE_ENERGY, 889781L) //
						.output(SymmetricEss.ChannelId.MIN_CELL_VOLTAGE, 3261) //
						.output(SymmetricEss.ChannelId.MAX_CELL_VOLTAGE, 3263) //
						.output(SymmetricEss.ChannelId.MIN_CELL_TEMPERATURE, 20) //
						.output(SymmetricEss.ChannelId.MAX_CELL_TEMPERATURE, 20)) //
				.deactivate();
	}
}
