package io.github.atengk.thread;

import io.github.atengk.utils.thread.VirtualThreadUtil;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.Semaphore;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.*;

class VirtualThreadConcurrencyLimitTest {

    @Test
    void callWithLimitShouldLimitConcurrency() throws Exception {
        AtomicInteger running = new AtomicInteger();
        AtomicInteger maxRunning = new AtomicInteger();
        List<Callable<Integer>> tasks = java.util.stream.IntStream.range(0, 8)
                .mapToObj(i -> (Callable<Integer>) () -> {
                    int current = running.incrementAndGet();
                    maxRunning.accumulateAndGet(current, Math::max);
                    Thread.sleep(20);
                    running.decrementAndGet();
                    return i;
                })
                .toList();
        List<Integer> results = VirtualThreadUtil.callWithLimit(tasks, 2);
        assertEquals(8, results.size());
        assertTrue(maxRunning.get() <= 2);
    }

    @Test
    void mapWithLimitShouldReturnMappedValues() throws Exception {
        List<Integer> result = VirtualThreadUtil.mapWithLimit(List.of(1, 2, 3), value -> value + 1, 2);
        assertEquals(List.of(2, 3, 4), result);
    }

    @Test
    void withSemaphoreShouldReleasePermit() throws Exception {
        Semaphore semaphore = new Semaphore(1);
        String value = VirtualThreadUtil.withSemaphore(semaphore, () -> "ok");
        assertEquals("ok", value);
        assertEquals(1, semaphore.availablePermits());
    }

    @Test
    void limitInvalidArgumentsShouldThrowException() {
        assertThrows(IllegalArgumentException.class, () -> VirtualThreadUtil.callWithLimit(List.of(() -> "ok"), 0));
        assertThrows(NullPointerException.class, () -> VirtualThreadUtil.withSemaphore(null, () -> "ok"));
    }
}
