package io.github.atengk.os;

import io.github.atengk.utils.OsUtil;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class OsUtilShutdownHookTest {

    @Test
    void shouldAddAndRemoveShutdownHook() {
        Thread hook = OsUtil.addShutdownHook(() -> { });
        assertNotNull(hook);
        assertTrue(OsUtil.removeShutdownHook(hook));
    }

    @Test
    void shouldHandleShutdownHookBoundary() {
        assertFalse(OsUtil.removeShutdownHook(null));
        assertThrows(NullPointerException.class, () -> OsUtil.addShutdownHook(null));
    }

    @Test
    void shouldRunGcAndFinalizationWithoutException() {
        assertDoesNotThrow(OsUtil::gc);
        assertDoesNotThrow(OsUtil::runFinalization);
    }
}
