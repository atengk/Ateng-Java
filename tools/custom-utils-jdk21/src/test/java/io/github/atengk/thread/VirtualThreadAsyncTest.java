package io.github.atengk.thread;

import io.github.atengk.utils.thread.VirtualThreadUtil;
import org.junit.jupiter.api.Test;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;

import static org.junit.jupiter.api.Assertions.*;

class VirtualThreadAsyncTest {

    @Test
    void runAsyncShouldComplete() throws Exception {
        CompletableFuture<Void> future = VirtualThreadUtil.runAsync(() -> assertTrue(Thread.currentThread().isVirtual()));
        assertNull(future.get());
    }

    @Test
    void supplyAsyncShouldReturnValue() throws Exception {
        CompletableFuture<String> future = VirtualThreadUtil.supplyAsync("async-case", () -> Thread.currentThread().getName());
        assertEquals("async-case", future.get());
    }

    @Test
    void futureShouldPropagateException() {
        Future<String> future = VirtualThreadUtil.future(() -> {
            throw new IllegalStateException("失败");
        });
        assertThrows(ExecutionException.class, future::get);
    }

    @Test
    void completableShouldRejectNull() {
        assertThrows(NullPointerException.class, () -> VirtualThreadUtil.completable((Runnable) null));
        assertThrows(NullPointerException.class, () -> VirtualThreadUtil.completable((java.util.function.Supplier<Object>) null));
    }
}
