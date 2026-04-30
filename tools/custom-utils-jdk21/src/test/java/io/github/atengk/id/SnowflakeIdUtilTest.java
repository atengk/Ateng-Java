package io.github.atengk.id;

import io.github.atengk.utils.id.IdUtil;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class SnowflakeIdUtilTest {

    @Test
    void shouldGenerateSnowflakeId() {
        long id = IdUtil.snowflakeId();
        assertTrue(id > 0);
        assertEquals(Long.toString(id), Long.toString(IdUtil.parseLongId(Long.toString(id))));
        assertTrue(IdUtil.isSnowflakeId(Long.toString(id)));
    }

    @Test
    void shouldGenerateSnowflakeWithWorkerAndDataCenter() {
        long id = IdUtil.snowflakeId(3, 7);
        assertEquals(3, IdUtil.getSnowflakeWorkerId(id));
        assertEquals(7, IdUtil.getSnowflakeDataCenterId(id));
        assertNotNull(IdUtil.getSnowflakeTime(id));
    }

    @Test
    void shouldGenerateIncreasingSnowflakeIds() {
        long first = IdUtil.nextSnowflakeId();
        long second = IdUtil.nextSnowflakeId();
        assertTrue(second > first);
        assertTrue(IdUtil.nextSnowflakeIdStr().matches("\\d+"));
    }

    @Test
    void shouldRejectInvalidSnowflakeInput() {
        assertFalse(IdUtil.isSnowflakeId("0"));
        assertFalse(IdUtil.isSnowflakeId("abc"));
        assertThrows(IllegalArgumentException.class, () -> IdUtil.snowflakeId(-1, 0));
        assertThrows(IllegalArgumentException.class, () -> IdUtil.snowflakeId(0, 32));
        assertThrows(IllegalArgumentException.class, () -> IdUtil.getSnowflakeTime(0));
    }
}
