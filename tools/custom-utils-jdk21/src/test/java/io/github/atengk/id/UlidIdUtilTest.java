package io.github.atengk.id;

import io.github.atengk.utils.id.IdUtil;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class UlidIdUtilTest {

    @Test
    void shouldGenerateUlid() {
        String value = IdUtil.ulid();
        assertTrue(IdUtil.isUlid(value));
        assertEquals(26, value.length());
    }

    @Test
    void shouldGenerateLowerUlidAndParseTime() {
        String value = IdUtil.ulidLower();
        assertEquals(value, value.toLowerCase());
        assertTrue(IdUtil.isUlid(value));
        long timestamp = IdUtil.getUlidTimestamp(value);
        assertTrue(timestamp <= System.currentTimeMillis() + 1000);
        assertNotNull(IdUtil.getUlidTime(value));
    }

    @Test
    void shouldGenerateMonotonicUlid() {
        String first = IdUtil.monotonicUlid();
        String second = IdUtil.monotonicUlid();
        assertTrue(IdUtil.isUlid(first));
        assertTrue(IdUtil.compareUlid(first, second) < 0 || !first.equals(second));
    }

    @Test
    void shouldRejectInvalidUlid() {
        assertFalse(IdUtil.isUlid(null));
        assertFalse(IdUtil.isUlid("abc"));
        assertFalse(IdUtil.isUlid("8ZZZZZZZZZZZZZZZZZZZZZZZZZ"));
        assertThrows(IllegalArgumentException.class, () -> IdUtil.getUlidTimestamp("abc"));
        assertThrows(IllegalArgumentException.class, () -> IdUtil.compareUlid(IdUtil.ulid(), "abc"));
    }
}
