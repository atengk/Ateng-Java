package io.github.atengk.thread;

import io.github.atengk.utils.thread.VirtualThreadUtil;
import org.junit.jupiter.api.Test;

import java.util.concurrent.Future;

import static org.junit.jupiter.api.Assertions.*;

class VirtualThreadDiagnosticTest {

    @Test
    void isVirtualShouldDetectVirtualThread() throws Exception {
        Future<Boolean> future = VirtualThreadUtil.future((java.util.concurrent.Callable<Boolean>) VirtualThreadUtil::isVirtual);
        assertTrue(future.get());
        assertTrue(VirtualThreadUtil.isPlatform());
    }

    @Test
    void currentThreadInfoShouldContainThreadText() {
        String info = VirtualThreadUtil.currentThreadInfo();
        assertTrue(info.contains("Thread"));
        assertTrue(VirtualThreadUtil.currentThreadId() > 0);
        assertNotNull(VirtualThreadUtil.currentThreadName());
    }

    @Test
    void dumpShouldReturnThreadInfo() {
        String dump = VirtualThreadUtil.dump(Thread.currentThread());
        assertTrue(dump.contains("virtual="));
        assertTrue(dump.contains("state="));
    }

    @Test
    void diagnosticInvalidArgumentsShouldThrowException() {
        assertThrows(NullPointerException.class, () -> VirtualThreadUtil.isVirtual(null));
        assertThrows(NullPointerException.class, () -> VirtualThreadUtil.dump(null));
    }
}
