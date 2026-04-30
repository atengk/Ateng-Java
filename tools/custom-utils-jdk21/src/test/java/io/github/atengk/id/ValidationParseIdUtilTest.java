package io.github.atengk.id;

import io.github.atengk.utils.id.IdUtil;
import org.junit.jupiter.api.Test;

import java.util.OptionalLong;

import static org.junit.jupiter.api.Assertions.*;

class ValidationParseIdUtilTest {

    @Test
    void shouldValidateCommonIds() {
        assertTrue(IdUtil.isId(IdUtil.uuid()));
        assertTrue(IdUtil.isId(IdUtil.uuidSimple()));
        assertTrue(IdUtil.isId(IdUtil.ulid()));
        assertTrue(IdUtil.isId(Long.toString(IdUtil.snowflakeId())));
        assertFalse(IdUtil.isId(null));
        assertFalse(IdUtil.isId("   "));
        assertFalse(IdUtil.isId("!@#"));
    }

    @Test
    void shouldValidateNumericAndLongId() {
        assertTrue(IdUtil.isNumericId("000"));
        assertFalse(IdUtil.isNumericId("12a"));
        assertTrue(IdUtil.isLongId("1"));
        assertFalse(IdUtil.isLongId("0"));
        assertFalse(IdUtil.isLongId("9223372036854775808"));
    }

    @Test
    void shouldParseLongId() {
        assertEquals(123L, IdUtil.parseLongId("123"));
        assertEquals(OptionalLong.of(123L), IdUtil.tryParseLongId("123"));
        assertTrue(IdUtil.tryParseLongId("bad").isEmpty());
        assertThrows(IllegalArgumentException.class, () -> IdUtil.parseLongId("0"));
        assertEquals(123L, IdUtil.requireLongId("123"));
    }

    @Test
    void shouldGetIdType() {
        assertEquals(IdUtil.IdType.UUID_V7, IdUtil.getIdType(IdUtil.uuidV7()));
        assertEquals(IdUtil.IdType.UUID, IdUtil.getIdType(IdUtil.uuid()));
        assertEquals(IdUtil.IdType.ULID, IdUtil.getIdType(IdUtil.ulid()));
        assertEquals(IdUtil.IdType.SNOWFLAKE, IdUtil.getIdType(Long.toString(IdUtil.snowflakeId())));
        assertEquals(IdUtil.IdType.NUMERIC, IdUtil.getIdType("123"));
        assertEquals(IdUtil.IdType.SHORT_ID, IdUtil.getIdType("AbcD"));
        assertEquals(IdUtil.IdType.UNKNOWN, IdUtil.getIdType(""));
    }

    @Test
    void shouldRequireId() {
        String id = IdUtil.uuidSimple();
        assertEquals(id, IdUtil.requireId(id));
        assertThrows(IllegalArgumentException.class, () -> IdUtil.requireId("!"));
    }
}
