package io.github.atengk.thread;

import io.github.atengk.utils.thread.TimedResult;
import io.github.atengk.utils.thread.VirtualTaskResult;
import io.github.atengk.utils.thread.VirtualTaskStats;
import io.github.atengk.utils.thread.VirtualThreadUtil;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.*;

class VirtualThreadMonitorStatsTest {

    @Test
    void measureShouldReturnDuration() throws Exception {
        Duration duration = VirtualThreadUtil.measure(() -> { });
        TimedResult<String> result = VirtualThreadUtil.measure(() -> "ok");
        assertFalse(duration.isNegative());
        assertEquals("ok", result.getValue());
        assertFalse(result.getDuration().isNegative());
    }

    @Test
    void timedShouldCallbackDuration() throws Exception {
        AtomicReference<Duration> duration = new AtomicReference<>();
        VirtualThreadUtil.timed(() -> { }, duration::set);
        assertNotNull(duration.get());
        String value = VirtualThreadUtil.timed(() -> "ok", (result, cost) -> assertEquals("ok", result));
        assertEquals("ok", value);
    }

    @Test
    void statsAndSummaryShouldCountResults() {
        List<Callable<Integer>> tasks = List.of(() -> 1, () -> {
            throw new IllegalStateException("失败");
        });
        VirtualTaskStats stats = VirtualThreadUtil.stats(tasks);
        assertEquals(2, stats.getTotalCount());
        assertEquals(1, stats.getSuccessCount());
        assertEquals(1, stats.getFailureCount());

        VirtualTaskStats summary = VirtualThreadUtil.summary(List.of(
                VirtualTaskResult.success(0, 1, Duration.ofMillis(1)),
                VirtualTaskResult.failure(1, new IllegalStateException("失败"), Duration.ofMillis(1))
        ));
        assertEquals(1, summary.getSuccessCount());
        assertEquals(1, summary.getFailureCount());
    }

    @Test
    void countMethodsShouldReturnExpectedCounts() {
        List<VirtualTaskResult<Integer>> results = List.of(
                VirtualTaskResult.success(0, 1, Duration.ZERO),
                VirtualTaskResult.failure(1, new IllegalStateException("失败"), Duration.ZERO),
                VirtualTaskResult.timeout(2, new java.util.concurrent.TimeoutException("超时"), Duration.ZERO)
        );
        assertEquals(1, VirtualThreadUtil.countSuccess(results));
        assertEquals(1, VirtualThreadUtil.countFailure(results));
        assertEquals(1, VirtualThreadUtil.countTimeout(results));
    }

    @Test
    void monitorInvalidArgumentsShouldThrowException() {
        assertThrows(NullPointerException.class, () -> VirtualThreadUtil.measure((Runnable) null));
        assertThrows(NullPointerException.class, () -> VirtualThreadUtil.timed(() -> { }, null));
        assertThrows(NullPointerException.class, () -> VirtualThreadUtil.summary(null));
    }
}
