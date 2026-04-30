package io.github.atengk.id;

import io.github.atengk.utils.id.IdUtil;
import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;

import static org.junit.jupiter.api.Assertions.*;

class EncodingIdUtilTest {

    @Test
    void shouldEncodeAndDecodeBase36() {
        assertEquals("0", IdUtil.toBase36(0));
        assertEquals(123456789L, IdUtil.fromBase36(IdUtil.toBase36(123456789L)));
        assertThrows(IllegalArgumentException.class, () -> IdUtil.toBase36(-1));
        assertThrows(IllegalArgumentException.class, () -> IdUtil.fromBase36("!"));
    }

    @Test
    void shouldEncodeAndDecodeBase62() {
        assertEquals("0", IdUtil.toBase62(0));
        long value = 9_876_543_210L;
        assertEquals(value, IdUtil.fromBase62(IdUtil.toBase62(value)));
        assertThrows(IllegalArgumentException.class, () -> IdUtil.toBase62(-1));
        assertThrows(IllegalArgumentException.class, () -> IdUtil.fromBase62("中文"));
    }

    @Test
    void shouldEncodeAndDecodeBase64Url() {
        byte[] bytes = "Ateng".getBytes(StandardCharsets.UTF_8);
        String encoded = IdUtil.toBase64Url(bytes);
        assertArrayEquals(bytes, IdUtil.fromBase64Url(encoded));
        assertThrows(NullPointerException.class, () -> IdUtil.toBase64Url(null));
        assertThrows(IllegalArgumentException.class, () -> IdUtil.fromBase64Url("!"));
    }

    @Test
    void shouldConvertUuidAndSnowflake() {
        String uuid = IdUtil.uuid();
        String compressed = IdUtil.uuidToBase64Url(uuid);
        assertEquals(22, compressed.length());
        assertEquals(uuid, IdUtil.base64UrlToUuid(compressed));

        long snowflake = IdUtil.snowflakeId();
        String base62 = IdUtil.snowflakeToBase62(snowflake);
        assertEquals(snowflake, IdUtil.base62ToSnowflake(base62));
        assertThrows(IllegalArgumentException.class, () -> IdUtil.base64UrlToUuid("abc"));
    }
}
