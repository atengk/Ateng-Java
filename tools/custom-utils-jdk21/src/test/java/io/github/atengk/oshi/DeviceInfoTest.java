package io.github.atengk.oshi;

import io.github.atengk.utils.oshi.OshiUtil;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class DeviceInfoTest {

    @Test
    void normalDeviceInfoShouldReturnData() {
        assertNotNull(OshiUtil.listGraphicsCards());
        assertNotNull(OshiUtil.listDisplays());
        assertNotNull(OshiUtil.listUsbDevices());
        assertNotNull(OshiUtil.listSoundCards());
        assertNotNull(OshiUtil.snapshotDevices());
    }

    @Test
    void usbTreeModeShouldReturnList() {
        assertNotNull(OshiUtil.listUsbDevices(true));
        assertNotNull(OshiUtil.listUsbDevices(false));
    }

    @Test
    void primaryGraphicsCardShouldBeSafe() {
        assertDoesNotThrow(OshiUtil::getPrimaryGraphicsCard);
    }
}
