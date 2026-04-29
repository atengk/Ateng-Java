package io.github.atengk.string;

import io.github.atengk.utils.StringUtil;
import org.junit.jupiter.api.Test;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;

class StringUtilBusinessValidationTest {

    @Test
    void testBusinessValidation() {
        assertTrue(StringUtil.isEmail("test@example.com"));
        assertTrue(StringUtil.isMobile("13800138000"));
        assertTrue(StringUtil.isPhone("010-12345678"));
        assertTrue(StringUtil.isUrl("https://example.com/path"));
        assertTrue(StringUtil.isHttpUrl("https://example.com"));
        assertTrue(StringUtil.isIpv4("192.168.1.1"));
        assertTrue(StringUtil.isIpv6("0:0:0:0:0:0:0:1"));
        assertFalse(StringUtil.isIdCard("110105194912310021"));
        assertTrue(StringUtil.isBankCard("4111111111111111"));
        assertTrue(StringUtil.isPostCode("100000"));
        assertTrue(StringUtil.isUuid("550e8400-e29b-41d4-a716-446655440000"));
    }

}
