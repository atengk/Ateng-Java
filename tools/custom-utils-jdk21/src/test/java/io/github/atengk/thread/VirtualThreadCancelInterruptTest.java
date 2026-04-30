package io.github.atengk.thread;

import io.github.atengk.utils.thread.VirtualThreadUtil;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.concurrent.Future;
import java.util.concurrent.CountDownLatch;

import static org.junit.jupiter.api.Assertions.*;

class VirtualThreadCancelInterruptTest {

    @Test
    void cancelShouldCancelFuture() {
        Future<String> future = VirtualThreadUtil.future(() -> {
            Thread.sleep(500);
            return "slow";
        });
        assertTrue(VirtualThreadUtil.cancel(future));
        assertTrue(VirtualThreadUtil.isCancelled(future));
    }

    @Test
    void cancelAllShouldCancelFutures() {
        Future<String> first = VirtualThreadUtil.future(() -> {
            Thread.sleep(500);
            return "first";
        });
        Future<String> second = VirtualThreadUtil.future(() -> {
            Thread.sleep(500);
            return "second";
        });
        assertEquals(2, VirtualThreadUtil.cancelAll(List.of(first, second)));
    }

    @Test
    void interruptShouldInterruptThread() throws Exception {
        CountDownLatch started = new CountDownLatch(1);
        Thread thread = VirtualThreadUtil.start(() -> {
            started.countDown();
            try {
                Thread.sleep(500);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        });
        started.await();
        VirtualThreadUtil.interrupt(thread);
        VirtualThreadUtil.join(thread);
        assertFalse(thread.isAlive());
    }

    @Test
    void cancelAndInterruptInvalidCasesShouldBeSafe() {
        assertFalse(VirtualThreadUtil.cancel(null));
        assertFalse(VirtualThreadUtil.cancelQuietly(null));
        assertThrows(NullPointerException.class, () -> VirtualThreadUtil.interrupt(null));
    }
}
