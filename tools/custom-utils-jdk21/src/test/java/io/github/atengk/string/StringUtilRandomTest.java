package io.github.atengk.string;

import io.github.atengk.utils.StringUtil;
import org.junit.jupiter.api.Test;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;

class StringUtilRandomTest {

    @Test
    void testRandom() {
        assertTrue(StringUtil.randomNumeric(6).matches("\\d{6}"));
        assertTrue(StringUtil.randomAlphabetic(6).matches("[A-Za-z]{6}"));
        assertTrue(StringUtil.randomAlphaNumeric(6).matches("[A-Za-z0-9]{6}"));
        assertEquals("AAAAAA", StringUtil.randomString(6, "A"));
        assertTrue(StringUtil.randomUpperCase(6).matches("[A-Z]{6}"));
        assertTrue(StringUtil.randomLowerCase(6).matches("[a-z]{6}"));
        assertTrue(StringUtil.randomCode(6).matches("\\d{6}"));
        assertTrue(StringUtil.isUuid(StringUtil.randomUuid()));
    }

}
