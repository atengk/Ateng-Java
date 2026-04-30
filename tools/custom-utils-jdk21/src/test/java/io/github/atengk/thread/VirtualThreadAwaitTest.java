package io.github.atengk.thread;

import io.github.atengk.utils.thread.VirtualThreadUtil;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.util.List;
import java.util.concurrent.Future;
import java.util.concurrent.TimeoutException;

import static org.junit.jupiter.api.Assertions.*;

class VirtualThreadAwaitTest {

    @Test
    void joinAndJoinAllShouldWaitThreads() throws Exception {
        Thread first = VirtualThreadUtil.start(() -> { });
        Thread second = VirtualThreadUtil.start(() -> { });
        VirtualThreadUtil.join(first);
        assertTrue(VirtualThreadUtil.joinAll(List.of(first, second), Duration.ofSeconds(1)));
    }

    @Test
    void getAndAwaitAllShouldReturnResults() throws Exception {
        Future<String> first = VirtualThreadUtil.future(() -> "a");
        Future<String> second = VirtualThreadUtil.future(() -> "b");
        assertEquals("a", VirtualThreadUtil.get(first));
        VirtualThreadUtil.awaitAll(List.of(second));
        assertEquals("b", VirtualThreadUtil.getOrNull(second));
    }

    @Test
    void awaitAnyShouldReturnCompletedFuture() throws Exception {
        Future<String> first = VirtualThreadUtil.future(() -> {
            Thread.sleep(50);
            return "a";
        });
        Future<String> second = VirtualThreadUtil.future(() -> "b");
        Future<?> completed = VirtualThreadUtil.awaitAny(List.of(first, second));
        assertTrue(completed.isDone());
    }

    @Test
    void awaitShouldThrowTimeout() {
        Future<String> future = VirtualThreadUtil.future(() -> {
            Thread.sleep(200);
            return "slow";
        });
        assertThrows(TimeoutException.class, () -> VirtualThreadUtil.await(future, Duration.ofMillis(20)));
    }

    @Test
    void awaitInvalidArgumentsShouldThrowException() {
        assertThrows(NullPointerException.class, () -> VirtualThreadUtil.join(null));
        assertThrows(IllegalArgumentException.class, () -> VirtualThreadUtil.awaitAny(List.of()));
        assertThrows(IllegalArgumentException.class, () -> VirtualThreadUtil.get(VirtualThreadUtil.future(() -> "ok"), Duration.ZERO));
    }
}
