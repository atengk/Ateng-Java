package io.github.atengk.string;

import io.github.atengk.utils.StringUtil;
import org.junit.jupiter.api.Test;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;

class StringUtilEmptyBlankTest {

    @Test
    void testEmptyBlank() {
        assertTrue(StringUtil.isEmpty(null));
        assertTrue(StringUtil.isEmpty(""));
        assertFalse(StringUtil.isEmpty(" "));
        assertTrue(StringUtil.isNotEmpty("a"));
        assertTrue(StringUtil.isBlank(" \t\n"));
        assertFalse(StringUtil.isBlank("a"));
        assertTrue(StringUtil.isNotBlank("a"));
        assertTrue(StringUtil.hasText("a"));
        assertTrue(StringUtil.isAllBlank(null, "", " "));
        assertTrue(StringUtil.isAnyBlank("a", " "));
        assertTrue(StringUtil.isAllEmpty(null, ""));
        assertTrue(StringUtil.isAnyEmpty("a", ""));
    }

}
