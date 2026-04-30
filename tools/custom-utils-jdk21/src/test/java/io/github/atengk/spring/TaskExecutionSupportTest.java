package io.github.atengk.spring;

import io.github.atengk.utils.spring.SpringUtil;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;

import java.time.Duration;
import java.time.Instant;
import java.util.concurrent.atomic.AtomicBoolean;

import static org.junit.jupiter.api.Assertions.*;

class TaskExecutionSupportTest {
    private AnnotationConfigApplicationContext context;

    @BeforeEach
    void setUp() {
        context = SpringUtilTestSupport.createContext();
    }

    @AfterEach
    void tearDown() {
        SpringUtilTestSupport.clear(context);
    }

    @Test
    void shouldAccessExecutors() {
        assertNotNull(SpringUtil.getTaskExecutor());
        assertNotNull(SpringUtil.getAsyncTaskExecutor());
        assertNotNull(SpringUtil.getTaskScheduler());
    }

    @Test
    void shouldExecuteTask() {
        AtomicBoolean ran = new AtomicBoolean(false);
        SpringUtil.executeAsync(() -> ran.set(true));
        assertTrue(ran.get());
    }

    @Test
    void shouldValidateTaskScheduling() {
        assertThrows(NullPointerException.class, () -> SpringUtil.executeAsync(null));
        assertThrows(IllegalArgumentException.class, () -> SpringUtil.scheduleAtFixedRate(() -> {}, Duration.ZERO));
        assertThrows(NullPointerException.class, () -> SpringUtil.schedule(() -> {}, null));
        assertDoesNotThrow(() -> SpringUtil.schedule(() -> {}, Instant.now().plusMillis(50)));
    }
}
