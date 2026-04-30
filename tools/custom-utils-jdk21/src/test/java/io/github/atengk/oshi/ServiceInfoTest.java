package io.github.atengk.oshi;

import io.github.atengk.utils.oshi.OshiUtil;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class ServiceInfoTest {

    @Test
    void normalServiceInfoShouldReturnData() {
        assertNotNull(OshiUtil.listServices());
        assertNotNull(OshiUtil.listRunningServices());
        assertNotNull(OshiUtil.listStoppedServices());
    }

    @Test
    void unknownServiceShouldBeSafe() {
        String name = "service-not-exists-for-oshi-util-test";
        assertNull(OshiUtil.findService(name));
        assertFalse(OshiUtil.isServiceRunning(name));
        assertEquals("UNKNOWN", OshiUtil.getServiceStatus(name));
    }

    @Test
    void invalidServiceNameShouldThrow() {
        assertThrows(IllegalArgumentException.class, () -> OshiUtil.findService(null));
        assertThrows(IllegalArgumentException.class, () -> OshiUtil.isServiceRunning(""));
        assertThrows(IllegalArgumentException.class, () -> OshiUtil.getServiceStatus(" "));
    }
}
