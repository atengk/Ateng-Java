package io.github.atengk.oshi;

import io.github.atengk.utils.oshi.OshiUtil;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class NetworkInfoTest {

    @Test
    void normalNetworkInfoShouldReturnData() {
        assertNotNull(OshiUtil.listNetworkInterfaces());
        assertNotNull(OshiUtil.listIpAddresses());
        assertNotNull(OshiUtil.listMacAddresses());
        assertNotNull(OshiUtil.snapshotNetwork());
    }

    @Test
    void networkParamsShouldBeSafe() {
        assertNotNull(OshiUtil.getHostname());
        assertNotNull(OshiUtil.getDomainName());
        assertNotNull(OshiUtil.getDnsServers());
        assertNotNull(OshiUtil.getGateway());
        assertNotNull(OshiUtil.getTcpStats());
        assertNotNull(OshiUtil.getUdpStats());
    }

    @Test
    void invalidNetworkInterfaceNameShouldThrow() {
        assertThrows(IllegalArgumentException.class, () -> OshiUtil.getNetworkInterface(""));
    }
}
