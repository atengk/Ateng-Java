package io.github.atengk.system;

import io.github.atengk.utils.SystemUtil;
import org.junit.jupiter.api.Test;

import java.net.ServerSocket;

import static org.junit.jupiter.api.Assertions.*;

class SystemUtilNetworkTest {

    @Test
    void shouldFindAndCheckAvailablePort() {
        int port = SystemUtil.findAvailablePort();
        assertTrue(port > 0);
        assertTrue(SystemUtil.isPortAvailable(0));
        assertTrue(SystemUtil.isPortAvailable(port));
    }

    @Test
    void shouldDetectOccupiedPort() throws Exception {
        try (ServerSocket serverSocket = new ServerSocket(0)) {
            int port = serverSocket.getLocalPort();
            assertFalse(SystemUtil.isPortAvailable(port));
        }
    }

    @Test
    void shouldHandleNetworkAddressMethods() {
        assertFalse(SystemUtil.getLocalIp().isBlank());
        assertFalse(SystemUtil.getLocalIps().isEmpty());
        assertTrue(SystemUtil.isLocalAddress("localhost"));
        assertTrue(SystemUtil.isPrivateIp("10.0.0.1"));
        assertTrue(SystemUtil.isLoopbackAddress("127.0.0.1"));
        assertFalse(SystemUtil.isTcpPortOpen("127.0.0.1", SystemUtil.findAvailablePort()));
    }

    @Test
    void shouldRejectInvalidPortArguments() {
        assertThrows(IllegalArgumentException.class, () -> SystemUtil.isPortAvailable(-1));
        assertThrows(IllegalArgumentException.class, () -> SystemUtil.isTcpPortOpen("localhost", 0));
        assertThrows(IllegalArgumentException.class, () -> SystemUtil.findAvailablePort(0));
        assertThrows(IllegalArgumentException.class, () -> SystemUtil.findAvailablePort(2000, 1000));
        assertFalse(SystemUtil.isPrivateIp("invalid-ip"));
    }
}
