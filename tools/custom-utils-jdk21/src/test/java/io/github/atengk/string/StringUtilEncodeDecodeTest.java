package io.github.atengk.string;

import io.github.atengk.utils.StringUtil;
import org.junit.jupiter.api.Test;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;

class StringUtilEncodeDecodeTest {

    @Test
    void testEncodeDecode() {
        assertArrayEquals("中文".getBytes(StandardCharsets.UTF_8), StringUtil.getBytes("中文"));
        assertEquals("中文", StringUtil.newString("中文".getBytes(StandardCharsets.UTF_8)));
        assertEquals("5Lit5paH", StringUtil.base64Encode("中文"));
        assertEquals("中文", StringUtil.base64Decode("5Lit5paH"));
        assertArrayEquals("abc".getBytes(StandardCharsets.UTF_8), StringUtil.base64DecodeToBytes(StringUtil.base64Encode("abc")));
        assertEquals("a+b", StringUtil.urlEncode("a b"));
        assertEquals("a b", StringUtil.urlDecode("a+b"));
        assertEquals("&lt;a&gt;&amp;", StringUtil.htmlEscape("<a>&"));
        assertEquals("<a>&", StringUtil.htmlUnescape("&lt;a&gt;&amp;"));
        assertEquals("A\\u4E2D", StringUtil.unicodeEncode("A中"));
        assertEquals("A中", StringUtil.unicodeDecode("A\\u4E2D"));
    }

}
