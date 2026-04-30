package io.github.atengk.thread;

import io.github.atengk.utils.thread.VirtualThreadContextProvider;
import io.github.atengk.utils.thread.VirtualThreadUtil;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import java.util.concurrent.Callable;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.*;

class VirtualThreadContextTest {

    private static final ThreadLocal<String> LOCAL = new ThreadLocal<>();

    @AfterEach
    void clear() {
        LOCAL.remove();
        VirtualThreadUtil.clearContextProviders();
    }

    @Test
    void wrapShouldPropagateRegisteredContext() throws Exception {
        VirtualThreadUtil.registerContextProvider(new VirtualThreadContextProvider() {
            @Override
            public Object capture() {
                return LOCAL.get();
            }

            @Override
            public void restore(Object context) {
                LOCAL.set((String) context);
            }

            @Override
            public void clear() {
                LOCAL.remove();
            }
        });
        LOCAL.set("trace-1");
        Callable<String> task = VirtualThreadUtil.wrap(LOCAL::get);
        assertEquals("trace-1", task.call());
        assertNull(LOCAL.get());
    }

    @Test
    void runWithContextShouldRestoreContext() {
        AtomicReference<String> value = new AtomicReference<>();
        VirtualThreadUtil.registerContextProvider(new SimpleProvider());
        LOCAL.set("tenant-1");
        VirtualThreadUtil.runWithContext(() -> value.set(LOCAL.get()));
        assertEquals("tenant-1", value.get());
    }

    @Test
    void restoreContextShouldRejectInvalidSnapshot() {
        assertThrows(IllegalArgumentException.class, () -> VirtualThreadUtil.restoreContext("bad"));
        assertThrows(NullPointerException.class, () -> VirtualThreadUtil.registerContextProvider(null));
    }

    private static final class SimpleProvider implements VirtualThreadContextProvider {
        @Override
        public Object capture() {
            return LOCAL.get();
        }

        @Override
        public void restore(Object context) {
            LOCAL.set((String) context);
        }

        @Override
        public void clear() {
            LOCAL.remove();
        }
    }
}
