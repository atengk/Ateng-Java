package io.github.atengk.thread;

import io.github.atengk.utils.thread.VirtualBatchResult;
import io.github.atengk.utils.thread.VirtualThreadUtil;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.concurrent.Callable;

import static org.junit.jupiter.api.Assertions.*;

class VirtualThreadResultCollectionTest {

    @Test
    void collectAllShouldSeparateSuccessAndFailure() {
        List<Callable<Integer>> tasks = List.of(() -> 1, () -> {
            throw new IllegalStateException("失败");
        });
        VirtualBatchResult<Integer> result = VirtualThreadUtil.collectAll(tasks);
        assertEquals(2, result.getResults().size());
        assertEquals(1, result.getSuccessResults().size());
        assertEquals(1, result.getFailureResults().size());
        assertTrue(result.hasFailure());
    }

    @Test
    void collectWithTimeoutShouldMarkTimeout() {
        List<Callable<String>> tasks = List.of(() -> {
            Thread.sleep(200);
            return "slow";
        });
        VirtualBatchResult<String> result = VirtualThreadUtil.collectWithTimeout(tasks, Duration.ofMillis(20));
        assertEquals(1, result.stats().getTimeoutCount());
    }

    @Test
    void firstSuccessShouldReturnFirstSuccessfulValue() {
        List<Callable<String>> tasks = List.of(() -> {
            throw new IllegalStateException("失败");
        }, () -> "ok");
        assertEquals("ok", VirtualThreadUtil.firstSuccess(tasks));
        assertEquals("default", VirtualThreadUtil.firstSuccessOrDefault(List.of(() -> {
            throw new IllegalStateException("失败");
        }), "default"));
    }

    @Test
    void firstSuccessShouldThrowWhenNoSuccess() {
        assertThrows(NoSuchElementException.class, () -> VirtualThreadUtil.firstSuccess(List.of(() -> {
            throw new IllegalStateException("失败");
        })));
        assertThrows(NullPointerException.class, () -> VirtualThreadUtil.collectAll(null));
    }
}
