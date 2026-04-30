package io.github.atengk.thread;

import io.github.atengk.utils.thread.VirtualThreadUtil;
import org.junit.jupiter.api.Test;

import java.util.function.Supplier;

import static org.junit.jupiter.api.Assertions.*;

class VirtualThreadNamingTest {

    @Test
    void nameShouldGenerateThreadName() {
        assertEquals("order-1", VirtualThreadUtil.name("order", 1));
        assertTrue(VirtualThreadUtil.name("order").startsWith("order-"));
    }

    @Test
    void nameOfAndPrefixShouldNormalizeBusinessName() {
        assertEquals("vt-order-1001", VirtualThreadUtil.nameOf("order", "1001"));
        assertEquals("vt-user-sync", VirtualThreadUtil.prefix("user sync"));
    }

    @Test
    void newNameFactoryShouldIncreaseIndex() {
        Supplier<String> factory = VirtualThreadUtil.newNameFactory("case");
        assertEquals("case-1", factory.get());
        assertEquals("case-2", factory.get());
        assertEquals("case-9", VirtualThreadUtil.newIndexedNameFactory("case").apply(9));
    }

    @Test
    void namingInvalidArgumentsShouldThrowException() {
        assertThrows(NullPointerException.class, () -> VirtualThreadUtil.normalizeName(null));
        assertThrows(IllegalArgumentException.class, () -> VirtualThreadUtil.name("case", -1));
        assertThrows(IllegalArgumentException.class, () -> VirtualThreadUtil.prefix(" "));
    }
}
