package io.github.atengk.string;

import io.github.atengk.utils.StringUtil;
import org.junit.jupiter.api.Test;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;

class StringUtilFilterTest {

    @Test
    void testFilter() {
        assertEquals("123", StringUtil.retainDigits("a1b2c3"));
        assertEquals("abc", StringUtil.retainLetters("a1b2c3"));
        assertEquals("a1b2", StringUtil.retainAlphaNumeric("a-1_b@2"));
        assertEquals("abc", StringUtil.removeDigits("a1b2c3"));
        assertEquals("123", StringUtil.removeLetters("a1b2c3"));
        assertEquals("a1b2", StringUtil.removeSpecialChars("a-1_b@2"));
        assertEquals("ACE", StringUtil.filterByPredicate("AbCdE", Character::isUpperCase));
        assertEquals("ok", StringUtil.removeEmoji("ok😊"));
    }

}
