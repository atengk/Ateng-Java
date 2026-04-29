package io.github.atengk.string;

import io.github.atengk.utils.StringUtil;
import org.junit.jupiter.api.Test;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;

class StringUtilSafeConvertTest {

    @Test
    void testSafeConvert() {
        assertEquals("", StringUtil.safeToString(null));
        assertNull(StringUtil.toStringOrNull(null));
        assertEquals("", StringUtil.toStringOrEmpty(null));
        assertEquals("123", StringUtil.safeToString(123));
        assertEquals(1, StringUtil.parseInt("1"));
        assertEquals(9, StringUtil.parseInt("x", 9));
        assertEquals(1L, StringUtil.parseLong("1"));
        assertEquals(9L, StringUtil.parseLong("x", 9L));
        assertEquals(1.5D, StringUtil.parseDouble("1.5"));
        assertEquals(9.5D, StringUtil.parseDouble("x", 9.5D));
        assertEquals(Boolean.TRUE, StringUtil.parseBoolean("yes"));
        assertEquals(Boolean.FALSE, StringUtil.parseBoolean("off"));
        assertEquals(new BigDecimal("1.23"), StringUtil.parseBigDecimal("1.23"));
        assertEquals(new BigDecimal("9"), StringUtil.parseBigDecimal("x", new BigDecimal("9")));
    }

}
