package io.github.atengk.thread;

import io.github.atengk.utils.thread.VirtualThreadUtil;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;

import static org.junit.jupiter.api.Assertions.*;

class VirtualThreadExecutorTest {

    @Test
    void newExecutorShouldExecuteVirtualTask() throws Exception {
        try (ExecutorService executor = VirtualThreadUtil.newExecutor("exec")) {
            Future<Boolean> future = executor.submit(() -> Thread.currentThread().isVirtual());
            assertTrue(future.get());
        }
    }

    @Test
    void shutdownAndAwaitShouldTerminateExecutor() throws Exception {
        ExecutorService executor = VirtualThreadUtil.newExecutor();
        executor.submit(() -> { });
        assertTrue(VirtualThreadUtil.shutdownAndAwait(executor, Duration.ofSeconds(1)));
        assertTrue(executor.isShutdown());
    }

    @Test
    void executorInvalidArgumentsShouldThrowException() {
        assertThrows(NullPointerException.class, () -> VirtualThreadUtil.newExecutor((java.util.concurrent.ThreadFactory) null));
        assertThrows(NullPointerException.class, () -> VirtualThreadUtil.shutdown(null));
        assertThrows(IllegalArgumentException.class, () -> VirtualThreadUtil.awaitTermination(VirtualThreadUtil.newExecutor(), Duration.ZERO));
    }
}
