package io.github.atengk.id;

import io.github.atengk.utils.id.IdUtil;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class UuidIdUtilTest {

    @Test
    void shouldGenerateStandardUuid() {
        String value = IdUtil.uuid();
        assertTrue(IdUtil.isUuid(value));
        assertEquals(36, value.length());
    }

    @Test
    void shouldGenerateSimpleAndUpperUuid() {
        String simple = IdUtil.uuidSimple();
        String upper = IdUtil.uuidSimpleUpper();
        assertTrue(IdUtil.isUuidSimple(simple));
        assertTrue(IdUtil.isUuidSimple(upper));
        assertEquals(32, simple.length());
        assertEquals(upper, upper.toUpperCase());
    }

    @Test
    void shouldNormalizeAndRemoveHyphen() {
        String simple = "550e8400e29b41d4a716446655440000";
        String standard = IdUtil.normalizeUuid(simple);
        assertEquals("550e8400-e29b-41d4-a716-446655440000", standard);
        assertEquals(simple, IdUtil.removeUuidHyphen(standard));
    }

    @Test
    void shouldRejectInvalidUuid() {
        assertFalse(IdUtil.isUuid(null));
        assertFalse(IdUtil.isUuid("abc"));
        assertFalse(IdUtil.isUuidSimple("abc"));
        assertThrows(IllegalArgumentException.class, () -> IdUtil.normalizeUuid("abc"));
    }
}
