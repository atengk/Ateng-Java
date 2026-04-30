package io.github.atengk.desensitized;

import io.github.atengk.utils.desensitized.DesensitizedUtil;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class AddressLocationDesensitizedTest {

    @Test
    void shouldMaskAddressAndLocation() {
        assertEquals("浙江省杭州市***", DesensitizedUtil.address("浙江省杭州市西湖区文三路100号"));
        assertEquals("浙江省杭州市***", DesensitizedUtil.detailAddress("浙江省杭州市西湖区文三路100号"));
        assertEquals("浙江省杭州市西湖区***", DesensitizedUtil.provinceCityAddress("浙江省杭州市西湖区文三路100号"));
        assertEquals("116.39***,39.91***", DesensitizedUtil.geoLocation("116.397128,39.916527"));
        assertEquals("116.39***", DesensitizedUtil.longitude("116.397128"));
        assertEquals("39.91***", DesensitizedUtil.latitude("39.916527"));
        assertEquals("31****", DesensitizedUtil.postcode("310000"));
        assertEquals("北京市朝阳区***", DesensitizedUtil.ipLocation("北京市朝阳区望京街道"));
    }

    @Test
    void shouldHandleBoundaryAddressValue() {
        assertNull(DesensitizedUtil.address(null));
        assertEquals("", DesensitizedUtil.address(""));
        assertEquals("ab*", DesensitizedUtil.longitude("abc"));
        assertEquals("杭*区", DesensitizedUtil.address("杭州区"));
    }
}
