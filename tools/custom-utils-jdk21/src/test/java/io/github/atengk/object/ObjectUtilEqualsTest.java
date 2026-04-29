package io.github.atengk.object;

import io.github.atengk.utils.ObjectUtil;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class ObjectUtilEqualsTest {

    @Test
    void shouldCheckEquals() {
        assertTrue(ObjectUtil.equals("a", "a"));
        assertFalse(ObjectUtil.equals("a", "b"));
        assertTrue(ObjectUtil.notEquals("a", "b"));
        assertTrue(ObjectUtil.deepEquals(new int[]{1, 2}, new int[]{1, 2}));
        assertTrue(ObjectUtil.deepEquals(new Object[]{new int[]{1}}, new Object[]{new int[]{1}}));
    }

    @Test
    void shouldCheckEqualsAnyAndAll() {
        assertTrue(ObjectUtil.equalsAny("a", "b", "a"));
        assertFalse(ObjectUtil.equalsAny("a"));
        assertTrue(ObjectUtil.equalsAll("a", "a", "a"));
        assertFalse(ObjectUtil.equalsAll("a", "a", "b"));
        assertTrue(ObjectUtil.notEqualsAny("a", "b", "c"));
    }

    @Test
    void shouldCheckSameReference() {
        Object value = new Object();
        assertTrue(ObjectUtil.same(value, value));
        assertFalse(ObjectUtil.same(value, new Object()));
        assertTrue(ObjectUtil.notSame(value, new Object()));
    }
}
