package io.github.atengk.thread;

import io.github.atengk.utils.thread.VirtualThreadUtil;
import org.junit.jupiter.api.Test;

import java.util.concurrent.ThreadFactory;
import java.util.concurrent.atomic.AtomicBoolean;

import static org.junit.jupiter.api.Assertions.*;

class VirtualThreadCreationTest {

    @Test
    void startShouldCreateVirtualThread() throws Exception {
        AtomicBoolean executed = new AtomicBoolean(false);
        Thread thread = VirtualThreadUtil.start(() -> executed.set(VirtualThreadUtil.isVirtual()));
        VirtualThreadUtil.join(thread);
        assertTrue(executed.get());
        assertTrue(thread.isVirtual());
    }

    @Test
    void unstartedShouldNotRunBeforeStart() throws Exception {
        AtomicBoolean executed = new AtomicBoolean(false);
        Thread thread = VirtualThreadUtil.unstarted("create-test", () -> executed.set(true));
        assertFalse(executed.get());
        thread.start();
        VirtualThreadUtil.join(thread);
        assertTrue(executed.get());
    }

    @Test
    void factoryShouldCreateNamedVirtualThread() {
        ThreadFactory factory = VirtualThreadUtil.factory("case", 10);
        Thread thread = factory.newThread(() -> { });
        assertTrue(thread.isVirtual());
        assertEquals("case-10", thread.getName());
    }

    @Test
    void invalidCreationArgumentsShouldThrowException() {
        assertThrows(NullPointerException.class, () -> VirtualThreadUtil.start(null));
        assertThrows(IllegalArgumentException.class, () -> VirtualThreadUtil.start("bad", -1, () -> { }));
        assertThrows(IllegalArgumentException.class, () -> VirtualThreadUtil.unstarted(" ", () -> { }));
    }
}
