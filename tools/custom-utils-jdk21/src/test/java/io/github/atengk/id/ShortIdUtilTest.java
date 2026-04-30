package io.github.atengk.id;

import io.github.atengk.utils.id.IdUtil;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class ShortIdUtilTest {

    @Test
    void shouldGenerateShortIds() {
        assertEquals(8, IdUtil.shortId().length());
        assertEquals(12, IdUtil.shortId(12).length());
        assertEquals(21, IdUtil.nanoId().length());
        assertEquals(10, IdUtil.nanoId(10).length());
    }

    @Test
    void shouldGenerateShortIdByAlphabet() {
        assertTrue(IdUtil.shortIdUpper(6).matches("^[A-Z]{6}$"));
        assertTrue(IdUtil.shortIdLower(6).matches("^[a-z]{6}$"));
        assertTrue(IdUtil.shortIdWithNumber(6).matches("^\\d{6}$"));
        assertTrue(IdUtil.shortIdWithLetter(6).matches("^[A-Za-z]{6}$"));
        assertTrue(IdUtil.shortIdWithMix(6).matches("^[A-Za-z0-9]{6}$"));
    }

    @Test
    void shouldGenerateShortUuid() {
        String value = IdUtil.shortUuid();
        assertEquals(22, value.length());
        assertTrue(IdUtil.isShortId(value, 22));
        assertTrue(IdUtil.isUuid(IdUtil.base64UrlToUuid(value)));
    }

    @Test
    void shouldCheckShortIdAndRejectInvalidLength() {
        assertTrue(IdUtil.isShortId("Abc123", 6));
        assertFalse(IdUtil.isShortId("Abc123", 5));
        assertFalse(IdUtil.isShortId("Abc 123", 7));
        assertThrows(IllegalArgumentException.class, () -> IdUtil.shortId(0));
        assertThrows(IllegalArgumentException.class, () -> IdUtil.isShortId("abc", 0));
    }
}
