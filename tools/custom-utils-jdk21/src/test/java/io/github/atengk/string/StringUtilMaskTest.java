package io.github.atengk.string;

import io.github.atengk.utils.StringUtil;
import org.junit.jupiter.api.Test;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;

class StringUtilMaskTest {

    @Test
    void testMask() {
        assertEquals("a**d", StringUtil.mask("abcd", 1, 3));
        assertEquals("**cd", StringUtil.maskLeft("abcd", 2));
        assertEquals("ab**", StringUtil.maskRight("abcd", 2));
        assertEquals("ab**ef", StringUtil.maskMiddle("abcdef", 2, 2));
        assertEquals("138****8000", StringUtil.maskMobile("13800138000"));
        assertEquals("a***g@example.com", StringUtil.maskEmail("ateng@example.com"));
        assertEquals("110105********002X", StringUtil.maskIdCard("11010519491231002X"));
        assertEquals("4111********1111", StringUtil.maskBankCard("4111111111111111"));
        assertEquals("张*三", StringUtil.maskName("张小三"));
        assertEquals("浙江省杭州市****", StringUtil.maskAddress("浙江省杭州市西湖区文三路"));
    }

}
