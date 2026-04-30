package io.github.atengk.oshi;

import io.github.atengk.utils.oshi.OshiUtil;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class HardwareAssetTest {

    @Test
    void normalHardwareInfoShouldReturnData() {
        assertNotNull(OshiUtil.getComputerSystemInfo());
        assertNotNull(OshiUtil.getFirmwareInfo());
        assertNotNull(OshiUtil.getBaseboardInfo());
        assertNotNull(OshiUtil.snapshotHardware());
    }

    @Test
    void identifiersShouldBeSafe() {
        assertNotNull(OshiUtil.getManufacturer());
        assertNotNull(OshiUtil.getModel());
        assertNotNull(OshiUtil.getSerialNumber());
        assertNotNull(OshiUtil.getHardwareUuid());
        assertNotNull(OshiUtil.getMachineId());
        assertFalse(OshiUtil.getDeviceFingerprint().isBlank());
    }

    @Test
    void deviceFingerprintShouldBeStable() {
        assertEquals(OshiUtil.getDeviceFingerprint(), OshiUtil.getDeviceFingerprint());
    }
}
