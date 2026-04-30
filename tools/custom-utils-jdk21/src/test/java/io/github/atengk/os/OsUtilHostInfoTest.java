package io.github.atengk.os;

import io.github.atengk.utils.OsUtil;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class OsUtilHostInfoTest {

    @Test
    void shouldGetUserAndHostInfo() {
        assertNotNull(OsUtil.getUserName());
        assertNotNull(OsUtil.getUserHome());
        assertFalse(OsUtil.getHostName().isBlank());
        assertFalse(OsUtil.getHostAddress().isBlank());
        assertFalse(OsUtil.getMachineId().isBlank());
        assertFalse(OsUtil.getInstanceId().isBlank());
    }

    @Test
    void shouldGetLocalAddresses() {
        assertFalse(OsUtil.getLocalAddressList().isEmpty());
        assertTrue(OsUtil.isLocalAddress("localhost"));
        assertTrue(OsUtil.isLoopbackAddress("127.0.0.1"));
    }

    @Test
    void shouldHandleNetworkIdentityBoundary() {
        assertFalse(OsUtil.isPrivateIp("not-an-ip"));
        assertTrue(OsUtil.isPrivateIp("192.168.1.1"));
        assertFalse(OsUtil.isLocalAddress(" "));
        assertNotNull(OsUtil.getMacAddress());
        assertDoesNotThrow(OsUtil::isRootUser);
        assertDoesNotThrow(OsUtil::isAdministrator);
    }
}
