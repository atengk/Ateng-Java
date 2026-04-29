package io.github.atengk.object;

import io.github.atengk.utils.ObjectUtil;
import org.junit.jupiter.api.Test;

import java.io.Serializable;

import static org.junit.jupiter.api.Assertions.*;

class ObjectUtilCloneTest {

    @Test
    void shouldCloneArray() {
        int[] source = {1, 2};
        int[] cloned = ObjectUtil.clone(source);
        assertArrayEquals(source, cloned);
        assertNotSame(source, cloned);
    }

    @Test
    void shouldCloneCloneableObject() {
        CloneableValue source = new CloneableValue("a");
        CloneableValue cloned = ObjectUtil.clone(source);
        assertEquals("a", cloned.name);
        assertNotSame(source, cloned);
        assertTrue(ObjectUtil.isCloneable(source));
    }

    @Test
    void shouldHandleUnsupportedClone() {
        PlainValue source = new PlainValue("a");
        assertFalse(ObjectUtil.isCloneable(source));
        assertSame(source, ObjectUtil.cloneIfPossible(source));
        assertThrows(IllegalArgumentException.class, () -> ObjectUtil.clone(source));
    }

    @Test
    void shouldCopySerializableObject() {
        SerializableValue source = new SerializableValue("a");
        SerializableValue copied = ObjectUtil.copyIfSerializable(source);
        assertEquals("a", copied.name);
        assertNotSame(source, copied);
        assertNull(ObjectUtil.copyIfSerializable(null));
        assertThrows(IllegalArgumentException.class, () -> ObjectUtil.copyIfSerializable(new PlainValue("a")));
        assertSame(source, ObjectUtil.identity(source));
    }

    static class CloneableValue implements Cloneable {
        private final String name;

        CloneableValue(String name) {
            this.name = name;
        }

        @Override
        protected CloneableValue clone() throws CloneNotSupportedException {
            return (CloneableValue) super.clone();
        }
    }

    static class SerializableValue implements Serializable {
        private final String name;

        SerializableValue(String name) {
            this.name = name;
        }
    }

    static class PlainValue {
        private final String name;

        PlainValue(String name) {
            this.name = name;
        }
    }
}
