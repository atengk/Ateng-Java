package io.github.atengk.desensitized;

import io.github.atengk.utils.desensitized.DesensitizedUtil;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class ContactDesensitizedTest {

    @Test
    void shouldMaskContactInfo() {
        assertEquals("138****5678", DesensitizedUtil.mobile("13812345678"));
        assertEquals("400*****67", DesensitizedUtil.phone("4001234567"));
        assertEquals("010-******78", DesensitizedUtil.fixedPhone("010-12345678"));
        assertEquals("t**t@example.com", DesensitizedUtil.email("test@example.com"));
        assertEquals("1***5", DesensitizedUtil.qq("12345"));
        assertEquals("w************t", DesensitizedUtil.wechat("wechat-account"));
        assertEquals("t******m", DesensitizedUtil.telegram("telegram"));
        assertEquals("1********8", DesensitizedUtil.whatsapp("1234567898"));
    }

    @Test
    void shouldMaskContactAutomatically() {
        assertEquals("138****5678", DesensitizedUtil.contact("13812345678"));
        assertEquals("a*c@example.com", DesensitizedUtil.contact("abc@example.com"));
        assertEquals("138****5678", DesensitizedUtil.emergencyContact("13812345678"));
    }

    @Test
    void shouldHandleBoundaryContactValue() {
        assertNull(DesensitizedUtil.mobile(null));
        assertEquals("", DesensitizedUtil.email(""));
        assertEquals("a*c", DesensitizedUtil.email("abc"));
    }
}
