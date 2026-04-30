package io.github.atengk.thread;

import io.github.atengk.utils.thread.VirtualThreadUtil;
import org.junit.jupiter.api.Test;

import java.util.Optional;
import java.util.concurrent.CompletionException;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.*;

class VirtualThreadExceptionHandlingTest {

    @Test
    void runSafeShouldHandleException() {
        AtomicReference<String> message = new AtomicReference<>();
        boolean success = VirtualThreadUtil.runSafe(() -> {
            throw new IllegalArgumentException("参数错误");
        }, throwable -> message.set(throwable.getMessage()));
        assertFalse(success);
        assertEquals("参数错误", message.get());
    }

    @Test
    void callSafeShouldReturnDefaultValue() {
        String value = VirtualThreadUtil.callSafe(() -> {
            throw new IllegalStateException("失败");
        }, "default");
        Optional<String> optional = VirtualThreadUtil.callSafe(() -> "ok");
        assertEquals("default", value);
        assertEquals(Optional.of("ok"), optional);
    }

    @Test
    void unwrapShouldRemoveWrapperException() {
        IllegalStateException cause = new IllegalStateException("root");
        Throwable throwable = VirtualThreadUtil.unwrap(new CompletionException(new ExecutionException(cause)));
        assertSame(cause, throwable);
    }

    @Test
    void typeJudgementShouldWork() {
        assertTrue(VirtualThreadUtil.isTimeout(new TimeoutException("timeout")));
        assertTrue(VirtualThreadUtil.isInterrupted(new InterruptedException("interrupt")));
        assertFalse(VirtualThreadUtil.isTimeout(null));
    }
}
