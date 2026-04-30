package io.github.atengk.desensitized;

import io.github.atengk.utils.desensitized.DesensitizedUtil;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class NetworkDeviceDesensitizedTest {

    @Test
    void shouldMaskNetworkAndDeviceInfo() {
        assertEquals("192.168.1.*", DesensitizedUtil.ip("192.168.1.100"));
        assertEquals("192.168.1.*", DesensitizedUtil.ipv4("192.168.1.100"));
        assertEquals("2001:****:7334", DesensitizedUtil.ipv6("2001:0db8:85a3:0000:0000:8a2e:0370:7334"));
        assertEquals("AA:BB:CC:**:**:**", DesensitizedUtil.mac("AA:BB:CC:DD:EE:FF"));
        assertEquals("8608*******1234", DesensitizedUtil.imei("860812345671234"));
        assertEquals("4600*******1234", DesensitizedUtil.imsi("460001234561234"));
        assertEquals("DEV2*******0001", DesensitizedUtil.deviceId("DEV202604300001"));
        assertEquals("ANDR*******0001", DesensitizedUtil.androidId("ANDROID20260001"));
        assertEquals("550e8400************************0000", DesensitizedUtil.idfa("550e8400-e29b-41d4-a716-446655440000"));
        assertEquals("550e8400************************0000", DesensitizedUtil.oaid("550e8400-e29b-41d4-a716-446655440000"));
        assertEquals("550e8400************************0000", DesensitizedUtil.uuid("550e8400-e29b-41d4-a716-446655440000"));
        assertEquals("Mozilla/5.***************afari", DesensitizedUtil.userAgent("Mozilla/5.0 AppleWebKit Safari"));
        assertEquals("a*i.example.com", DesensitizedUtil.domain("api.example.com"));
        assertEquals("https://a.com/api?token=***&name=tom&mobile=***#x", DesensitizedUtil.url("https://a.com/api?token=abc&name=tom&mobile=13812345678#x"));
        assertEquals("/api?token=***&name=tom", DesensitizedUtil.uri("/api?token=abc&name=tom"));
        assertEquals("token=***&name=tom", DesensitizedUtil.queryString("token=abc&name=tom"));
    }

    @Test
    void shouldHandleBoundaryNetworkValue() {
        assertNull(DesensitizedUtil.ip(null));
        assertEquals("bad", DesensitizedUtil.ipv4("bad"));
        assertEquals("AA-BB-CC-**-**-**", DesensitizedUtil.mac("AA-BB-CC-DD-EE-FF"));
        assertEquals("plain-url", DesensitizedUtil.url("plain-url"));
    }
}
