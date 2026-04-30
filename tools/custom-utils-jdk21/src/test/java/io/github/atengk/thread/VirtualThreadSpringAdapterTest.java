package io.github.atengk.thread;

import io.github.atengk.utils.thread.VirtualThreadUtil;
import org.junit.jupiter.api.Test;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.*;

class VirtualThreadSpringAdapterTest {

    @Test
    void taskExecutorShouldCreateUsableExecutor() throws Exception {
        try (ExecutorService executor = VirtualThreadUtil.taskExecutor("spring-task")) {
            Future<Boolean> future = executor.submit(() -> Thread.currentThread().isVirtual());
            assertTrue(future.get());
        }
    }

    @Test
    void asyncAndApplicationExecutorShouldCreateExecutors() throws Exception {
        try (ExecutorService async = VirtualThreadUtil.asyncExecutor();
             ExecutorService app = VirtualThreadUtil.applicationExecutor()) {
            assertTrue(async.submit(() -> Thread.currentThread().getName().startsWith("vt-async")).get());
            assertTrue(app.submit(() -> Thread.currentThread().getName().startsWith("vt-app")).get());
        }
    }

    @Test
    void scheduledExecutorShouldRunDelayedTask() throws Exception {
        try (ScheduledExecutorService scheduler = VirtualThreadUtil.scheduledExecutor()) {
            Future<String> future = scheduler.schedule(() -> "ok", 10, TimeUnit.MILLISECONDS);
            assertEquals("ok", future.get(1, TimeUnit.SECONDS));
        }
    }

    @Test
    void decorateShouldRejectNullTask() {
        assertThrows(NullPointerException.class, () -> VirtualThreadUtil.decorate((Runnable) null));
        assertThrows(NullPointerException.class, () -> VirtualThreadUtil.decorate((java.util.concurrent.Callable<Object>) null));
    }
}
