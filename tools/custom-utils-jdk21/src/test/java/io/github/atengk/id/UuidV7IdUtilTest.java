package io.github.atengk.id;

import io.github.atengk.utils.id.IdUtil;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class UuidV7IdUtilTest {

    @Test
    void shouldGenerateUuidV7() {
        String value = IdUtil.uuidV7();
        assertTrue(IdUtil.isUuidV7(value));
        assertTrue(IdUtil.isUuidV7(IdUtil.removeUuidHyphen(value)));
        assertEquals(36, value.length());
    }

    @Test
    void shouldParseUuidV7Time() {
        long before = System.currentTimeMillis() - 1000;
        String value = IdUtil.uuidV7();
        long timestamp = IdUtil.getUuidV7Timestamp(value);
        long after = System.currentTimeMillis() + 1000;
        assertTrue(timestamp >= before && timestamp <= after);
        assertNotNull(IdUtil.getUuidV7Time(value));
    }

    @Test
    void shouldCompareUuidV7() throws InterruptedException {
        String left = IdUtil.uuidV7();
        Thread.sleep(2);
        String right = IdUtil.uuidV7();
        assertTrue(IdUtil.compareUuidV7(left, right) < 0);
    }

    @Test
    void shouldRejectInvalidUuidV7() {
        assertFalse(IdUtil.isUuidV7(IdUtil.uuidV4()));
        assertThrows(IllegalArgumentException.class, () -> IdUtil.getUuidV7Timestamp("abc"));
        assertThrows(IllegalArgumentException.class, () -> IdUtil.compareUuidV7(IdUtil.uuidV7(), "abc"));
    }
}
