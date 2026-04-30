package io.github.atengk.thread;

import io.github.atengk.utils.thread.VirtualThreadUtil;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Future;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Supplier;

import static org.junit.jupiter.api.Assertions.*;

class VirtualThreadAdapterTest {

    @Test
    void callableRunnableSupplierAdaptersShouldWork() throws Exception {
        Runnable runnable = VirtualThreadUtil.toRunnable(() -> "ok");
        assertDoesNotThrow(runnable::run);
        assertNull(VirtualThreadUtil.toCallable(() -> { }).call());
        Supplier<String> supplier = VirtualThreadUtil.toSupplier(() -> "value");
        assertEquals("value", supplier.get());
    }

    @Test
    void futureShouldConvertToCompletableFuture() throws Exception {
        Future<String> future = VirtualThreadUtil.future(() -> "ok");
        CompletableFuture<String> completableFuture = VirtualThreadUtil.toCompletableFuture(future);
        assertEquals("ok", completableFuture.get());
    }

    @Test
    void toFuturesShouldSubmitAllTasks() {
        List<Future<Integer>> futures = VirtualThreadUtil.toFutures(List.of(() -> 1, () -> 2));
        assertEquals(List.of(1, 2), futures.stream().map(VirtualThreadUtil::getOrNull).toList());
    }

    @Test
    void wrapFunctionAndConsumerShouldWork() {
        Function<Integer, Integer> function = VirtualThreadUtil.wrapFunction(value -> value + 1);
        Consumer<String> consumer = VirtualThreadUtil.wrapConsumer(value -> assertEquals("ok", value));
        assertEquals(Integer.valueOf(2), function.apply(1));
        assertDoesNotThrow(() -> consumer.accept("ok"));
    }

    @Test
    void adapterInvalidArgumentsShouldThrowException() {
        assertThrows(NullPointerException.class, () -> VirtualThreadUtil.toRunnable(null));
        assertThrows(NullPointerException.class, () -> VirtualThreadUtil.toCompletableFuture(null));
        assertThrows(NullPointerException.class, () -> VirtualThreadUtil.wrapFunction(null));
    }
}
