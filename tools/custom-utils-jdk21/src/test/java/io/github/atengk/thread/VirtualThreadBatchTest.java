package io.github.atengk.thread;

import io.github.atengk.utils.thread.VirtualThreadUtil;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.Future;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.*;

class VirtualThreadBatchTest {

    @Test
    void invokeAllShouldReturnAllResults() throws Exception {
        List<Callable<Integer>> tasks = List.of(() -> 1, () -> 2, () -> 3);
        List<Future<Integer>> futures = VirtualThreadUtil.invokeAll(tasks);
        assertEquals(List.of(1, 2, 3), futures.stream().map(VirtualThreadUtil::getOrNull).toList());
    }

    @Test
    void mapAndForEachShouldProcessCollection() throws Exception {
        List<Integer> mapped = VirtualThreadUtil.map(List.of(1, 2, 3), value -> value * 2);
        assertEquals(List.of(2, 4, 6), mapped);
        AtomicInteger sum = new AtomicInteger();
        VirtualThreadUtil.forEach(List.of(1, 2, 3), sum::addAndGet);
        assertEquals(6, sum.get());
    }

    @Test
    void runAllWithTimeoutShouldThrowWhenTaskTimeout() {
        List<Runnable> tasks = List.of(() -> {
            try {
                Thread.sleep(200);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        });
        assertThrows(TimeoutException.class, () -> VirtualThreadUtil.runAll(tasks, Duration.ofMillis(20)));
    }

    @Test
    void batchInvalidArgumentsShouldThrowException() {
        assertThrows(NullPointerException.class, () -> VirtualThreadUtil.invokeAll(null));
        assertThrows(NullPointerException.class, () -> VirtualThreadUtil.map(List.of(1), null));
        assertThrows(IllegalArgumentException.class, () -> VirtualThreadUtil.invokeAll(List.of(() -> 1), Duration.ZERO));
    }
}
