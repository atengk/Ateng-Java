package io.github.atengk.thread;

import io.github.atengk.utils.thread.VirtualThreadUtil;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.util.Optional;
import java.util.concurrent.Future;
import java.util.concurrent.TimeoutException;

import static org.junit.jupiter.api.Assertions.*;

class VirtualThreadTimeoutTest {

    @Test
    void callWithTimeoutShouldReturnValue() throws Exception {
        String value = VirtualThreadUtil.callWithTimeout(() -> "ok", Duration.ofSeconds(1));
        assertEquals("ok", value);
    }

    @Test
    void callWithTimeoutShouldCancelSlowTask() {
        assertThrows(TimeoutException.class, () -> VirtualThreadUtil.callWithTimeout(() -> {
            Thread.sleep(200);
            return "slow";
        }, Duration.ofMillis(20)));
    }

    @Test
    void completeOnTimeoutShouldReturnDefaultValue() {
        String value = VirtualThreadUtil.completeOnTimeout(() -> {
            try {
                Thread.sleep(200);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
            return "slow";
        }, "default", Duration.ofMillis(20));
        assertEquals("default", value);
    }

    @Test
    void awaitQuietlyShouldReturnEmptyOnTimeout() {
        Future<String> future = VirtualThreadUtil.future(() -> {
            Thread.sleep(200);
            return "slow";
        });
        Optional<String> value = VirtualThreadUtil.awaitQuietly(future, Duration.ofMillis(20));
        assertTrue(value.isEmpty());
    }

    @Test
    void timeoutInvalidArgumentsShouldThrowException() {
        assertThrows(IllegalArgumentException.class, () -> VirtualThreadUtil.callWithTimeout(() -> "ok", Duration.ZERO));
        assertThrows(NullPointerException.class, () -> VirtualThreadUtil.callWithTimeout(null, Duration.ofSeconds(1)));
    }
}
