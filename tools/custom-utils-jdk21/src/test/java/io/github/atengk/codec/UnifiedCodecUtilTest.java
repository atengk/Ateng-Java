package io.github.atengk.codec;

import io.github.atengk.utils.codec.CodecUtil;
import io.github.atengk.utils.codec.EncodeType;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class UnifiedCodecUtilTest {

    @Test
    void shouldEncodeAndDecodeByType() {
        assertEquals("中文", CodecUtil.decode(CodecUtil.encode("中文", EncodeType.URL), EncodeType.URL));
        assertEquals("中文", CodecUtil.decode(CodecUtil.encode("中文", EncodeType.BASE64), EncodeType.BASE64));
        assertEquals("中文", CodecUtil.decode(CodecUtil.encode("中文", EncodeType.BASE64_URL), EncodeType.BASE64_URL));
        assertEquals("中文", CodecUtil.decode(CodecUtil.encode("中文", EncodeType.BASE64_MIME), EncodeType.BASE64_MIME));
        assertEquals("中文", CodecUtil.decode(CodecUtil.encode("中文", EncodeType.HEX), EncodeType.HEX));
        assertEquals("中文", CodecUtil.decode(CodecUtil.encode("中文", EncodeType.UNICODE), EncodeType.UNICODE));
        assertEquals("<a>", CodecUtil.decode(CodecUtil.encode("<a>", EncodeType.HTML), EncodeType.HTML));
        assertEquals("<a>", CodecUtil.decode(CodecUtil.encode("<a>", EncodeType.XML), EncodeType.XML));
        assertEquals("a\nb", CodecUtil.decode(CodecUtil.encode("a\nb", EncodeType.JSON), EncodeType.JSON));
    }

    @Test
    void shouldConvertBetweenTypes() {
        String base64 = CodecUtil.encode("中文", EncodeType.BASE64);
        String hex = CodecUtil.convert(base64, EncodeType.BASE64, EncodeType.HEX);
        assertEquals("中文", CodecUtil.decode(hex, EncodeType.HEX));
    }

    @Test
    void shouldDetectAndValidateEncodedValues() {
        assertTrue(CodecUtil.isEncoded(CodecUtil.base64Encode("abc"), EncodeType.BASE64));
        assertTrue(CodecUtil.isEncoded(CodecUtil.stringToHex("abc"), EncodeType.HEX));
        assertTrue(CodecUtil.isEncoded(CodecUtil.unicodeEncode("中"), EncodeType.UNICODE));
        assertDoesNotThrow(() -> CodecUtil.validateEncoded(CodecUtil.base64Encode("abc"), EncodeType.BASE64));
        assertThrows(IllegalArgumentException.class, () -> CodecUtil.validateEncoded("abc", EncodeType.URL));
    }

    @Test
    void shouldRejectNullEncodeType() {
        assertThrows(NullPointerException.class, () -> CodecUtil.encode("abc", null));
        assertThrows(NullPointerException.class, () -> CodecUtil.decode("abc", null));
        assertThrows(NullPointerException.class, () -> CodecUtil.convert("abc", null, EncodeType.BASE64));
        assertThrows(NullPointerException.class, () -> CodecUtil.isEncoded("abc", null));
    }
}
