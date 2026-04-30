package io.github.atengk.system;

import io.github.atengk.utils.SystemUtil;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class SystemUtilHostInfoTest {

    @Test
    void shouldGetUserAndHostInfo() {
        assertNotNull(SystemUtil.getUserName());
        assertNotNull(SystemUtil.getUserHome());
        assertFalse(SystemUtil.getHostName().isBlank());
        assertFalse(SystemUtil.getHostAddress().isBlank());
        assertFalse(SystemUtil.getMachineId().isBlank());
        assertFalse(SystemUtil.getInstanceId().isBlank());
    }

    @Test
    void shouldGetLocalAddresses() {
        assertFalse(SystemUtil.getLocalAddressList().isEmpty());
        assertTrue(SystemUtil.isLocalAddress("localhost"));
        assertTrue(SystemUtil.isLoopbackAddress("127.0.0.1"));
    }

    @Test
    void shouldHandleNetworkIdentityBoundary() {
        assertFalse(SystemUtil.isPrivateIp("not-an-ip"));
        assertTrue(SystemUtil.isPrivateIp("192.168.1.1"));
        assertFalse(SystemUtil.isLocalAddress(" "));
        assertNotNull(SystemUtil.getMacAddress());
        assertDoesNotThrow(SystemUtil::isRootUser);
        assertDoesNotThrow(SystemUtil::isAdministrator);
    }
}
