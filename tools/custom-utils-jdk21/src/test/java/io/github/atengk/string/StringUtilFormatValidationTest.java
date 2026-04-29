package io.github.atengk.string;

import io.github.atengk.utils.StringUtil;
import org.junit.jupiter.api.Test;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;

class StringUtilFormatValidationTest {

    @Test
    void testFormatValidation() {
        assertTrue(StringUtil.isNumeric("123.45"));
        assertTrue(StringUtil.isInteger("-123"));
        assertTrue(StringUtil.isLong("9223372036854775807"));
        assertTrue(StringUtil.isDecimal(".5"));
        assertTrue(StringUtil.isPositiveNumber("1"));
        assertTrue(StringUtil.isNegativeNumber("-1"));
        assertTrue(StringUtil.isAlpha("abcXYZ"));
        assertTrue(StringUtil.isAlphaNumeric("abc123"));
        assertTrue(StringUtil.isAscii("abc123"));
        assertTrue(StringUtil.isLowerCase("abc"));
        assertTrue(StringUtil.isUpperCase("ABC"));
        assertFalse(StringUtil.isAlpha("abc123"));
    }

}
