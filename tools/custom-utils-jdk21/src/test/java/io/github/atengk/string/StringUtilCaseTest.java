package io.github.atengk.string;

import io.github.atengk.utils.StringUtil;
import org.junit.jupiter.api.Test;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;

class StringUtilCaseTest {

    @Test
    void testCase() {
        assertEquals("ABC", StringUtil.upperCase("abc"));
        assertEquals("abc", StringUtil.lowerCase("ABC"));
        assertEquals("Ateng", StringUtil.capitalize("ateng"));
        assertEquals("ateng", StringUtil.uncapitalize("Ateng"));
        assertEquals("aB1", StringUtil.swapCase("Ab1"));
        assertEquals("Ateng", StringUtil.firstCharUpper("ateng"));
        assertEquals("ateng", StringUtil.firstCharLower("Ateng"));
        assertEquals("Hello World", StringUtil.toTitleCase("hello world"));
    }

}
