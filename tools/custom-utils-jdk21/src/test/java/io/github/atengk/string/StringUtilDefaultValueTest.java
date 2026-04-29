package io.github.atengk.string;

import io.github.atengk.utils.StringUtil;
import org.junit.jupiter.api.Test;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;

class StringUtilDefaultValueTest {

    @Test
    void testDefaultValue() {
        assertEquals("d", StringUtil.defaultIfNull(null, "d"));
        assertEquals("v", StringUtil.defaultIfNull("v", "d"));
        assertEquals("d", StringUtil.defaultIfEmpty("", "d"));
        assertEquals(" ", StringUtil.defaultIfEmpty(" ", "d"));
        assertEquals("d", StringUtil.defaultIfBlank(" ", "d"));
        assertNull(StringUtil.emptyToNull(""));
        assertNull(StringUtil.blankToNull(" "));
        assertEquals("", StringUtil.nullToEmpty(null));
        assertEquals("d", StringUtil.nullToDefault(null, "d"));
    }

}
