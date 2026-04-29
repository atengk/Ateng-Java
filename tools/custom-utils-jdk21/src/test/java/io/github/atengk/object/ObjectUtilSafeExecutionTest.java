package io.github.atengk.object;

import io.github.atengk.utils.ObjectUtil;
import org.junit.jupiter.api.Test;

import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.*;

class ObjectUtilSafeExecutionTest {

    @Test
    void shouldExecuteActionsByCondition() {
        AtomicInteger count = new AtomicInteger();
        ObjectUtil.ifNull(null, count::incrementAndGet);
        ObjectUtil.ifEmpty("", count::incrementAndGet);
        ObjectUtil.ifBlank(" ", count::incrementAndGet);
        assertEquals(3, count.get());
    }

    @Test
    void shouldExecuteConsumerByCondition() {
        AtomicReference<String> ref = new AtomicReference<>();
        ObjectUtil.ifNotNull("a", ref::set);
        assertEquals("a", ref.get());
        ObjectUtil.ifNotEmpty("b", ref::set);
        assertEquals("b", ref.get());
        ObjectUtil.ifNotBlank("c", ref::set);
        assertEquals("c", ref.get());
    }

    @Test
    void shouldMapValues() {
        assertEquals(Integer.valueOf(1), ObjectUtil.mapIfNotNull("a", String::length));
        assertNull(ObjectUtil.mapIfNotNull(null, Object::toString));
        assertEquals(Integer.valueOf(1), ObjectUtil.mapIfNotEmpty("a", String::length));
        assertNull(ObjectUtil.mapIfNotEmpty("", String::length));
        assertEquals(Integer.valueOf(0), ObjectUtil.mapOrDefault("", String::length, 0));
    }

    @Test
    void shouldThrowWhenActionMissing() {
        assertThrows(NullPointerException.class, () -> ObjectUtil.ifNull(null, null));
        assertThrows(NullPointerException.class, () -> ObjectUtil.ifNotNull("a", null));
        assertThrows(NullPointerException.class, () -> ObjectUtil.mapIfNotNull("a", null));
    }
}
