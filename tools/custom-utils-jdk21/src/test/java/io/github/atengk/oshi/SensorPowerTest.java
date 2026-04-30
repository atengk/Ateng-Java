package io.github.atengk.oshi;

import io.github.atengk.utils.oshi.OshiUtil;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class SensorPowerTest {

    @Test
    void normalSensorInfoShouldReturnData() {
        OshiUtil.SensorsInfo info = OshiUtil.getSensorsInfo();
        assertTrue(info.cpuTemperature() >= 0);
        assertNotNull(info.fanSpeeds());
        assertTrue(info.cpuVoltage() >= 0);
        assertNotNull(OshiUtil.snapshotSensors());
    }

    @Test
    void powerInfoShouldBeSafeWhenBatteryMissing() {
        assertNotNull(OshiUtil.listPowerSources());
        assertTrue(OshiUtil.getBatteryPercent() >= 0);
        assertDoesNotThrow(OshiUtil::isCharging);
    }

    @Test
    void fanSpeedsShouldNeverBeNull() {
        assertNotNull(OshiUtil.getFanSpeeds());
    }
}
