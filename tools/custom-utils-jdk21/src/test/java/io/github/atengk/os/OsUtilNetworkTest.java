package io.github.atengk.os;

import io.github.atengk.utils.OsUtil;
import org.junit.jupiter.api.Test;

import java.net.ServerSocket;

import static org.junit.jupiter.api.Assertions.*;

class OsUtilNetworkTest {

    @Test
    void shouldFindAndCheckAvailablePort() {
        int port = OsUtil.findAvailablePort();
        assertTrue(port > 0);
        assertTrue(OsUtil.isPortAvailable(0));
        assertTrue(OsUtil.isPortAvailable(port));
    }

    @Test
    void shouldDetectOccupiedPort() throws Exception {
        try (ServerSocket serverSocket = new ServerSocket(0)) {
            int port = serverSocket.getLocalPort();
            assertFalse(OsUtil.isPortAvailable(port));
        }
    }

    @Test
    void shouldHandleNetworkAddressMethods() {
        assertFalse(OsUtil.getLocalIp().isBlank());
        assertFalse(OsUtil.getLocalIps().isEmpty());
        assertTrue(OsUtil.isLocalAddress("localhost"));
        assertTrue(OsUtil.isPrivateIp("10.0.0.1"));
        assertTrue(OsUtil.isLoopbackAddress("127.0.0.1"));
        assertFalse(OsUtil.isTcpPortOpen("127.0.0.1", OsUtil.findAvailablePort()));
    }

    @Test
    void shouldRejectInvalidPortArguments() {
        assertThrows(IllegalArgumentException.class, () -> OsUtil.isPortAvailable(-1));
        assertThrows(IllegalArgumentException.class, () -> OsUtil.isTcpPortOpen("localhost", 0));
        assertThrows(IllegalArgumentException.class, () -> OsUtil.findAvailablePort(0));
        assertThrows(IllegalArgumentException.class, () -> OsUtil.findAvailablePort(2000, 1000));
        assertFalse(OsUtil.isPrivateIp("invalid-ip"));
    }
}
