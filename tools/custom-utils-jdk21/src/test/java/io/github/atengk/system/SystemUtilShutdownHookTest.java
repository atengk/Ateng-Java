package io.github.atengk.system;

import io.github.atengk.utils.SystemUtil;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class SystemUtilShutdownHookTest {

    @Test
    void shouldAddAndRemoveShutdownHook() {
        Thread hook = SystemUtil.addShutdownHook(() -> { });
        assertNotNull(hook);
        assertTrue(SystemUtil.removeShutdownHook(hook));
    }

    @Test
    void shouldHandleShutdownHookBoundary() {
        assertFalse(SystemUtil.removeShutdownHook(null));
        assertThrows(NullPointerException.class, () -> SystemUtil.addShutdownHook(null));
    }

    @Test
    void shouldRunGcAndFinalizationWithoutException() {
        assertDoesNotThrow(SystemUtil::gc);
        assertDoesNotThrow(SystemUtil::runFinalization);
    }
}
