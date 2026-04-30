package io.github.atengk.spring;

import io.github.atengk.utils.SpringUtil;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.util.concurrent.atomic.AtomicBoolean;

import static org.junit.jupiter.api.Assertions.*;

class TransactionSupportTest {
    private AnnotationConfigApplicationContext context;

    @BeforeEach
    void setUp() {
        context = SpringUtilTestSupport.createContext();
    }

    @AfterEach
    void tearDown() {
        if (TransactionSynchronizationManager.isSynchronizationActive()) {
            TransactionSynchronizationManager.clearSynchronization();
        }
        SpringUtilTestSupport.clear(context);
    }

    @Test
    void shouldExecuteInTransaction() {
        String result = SpringUtil.executeInTransaction(() -> {
            assertTrue(SpringUtil.isTransactionActive());
            return SpringUtil.getCurrentTransactionName();
        });
        assertNull(result);

        AtomicBoolean ran = new AtomicBoolean();
        SpringUtil.executeInTransaction(() -> ran.set(true));
        assertTrue(ran.get());
    }

    @Test
    void shouldRunAfterCommitWithoutTransaction() {
        AtomicBoolean ran = new AtomicBoolean(false);
        SpringUtil.registerAfterCommit(() -> ran.set(true));
        assertTrue(ran.get());
    }

    @Test
    void shouldRegisterCallbacksWithSynchronization() {
        AtomicBoolean complete = new AtomicBoolean(false);
        AtomicBoolean rollback = new AtomicBoolean(false);
        TransactionSynchronizationManager.initSynchronization();
        SpringUtil.registerAfterCompletion(() -> complete.set(true));
        SpringUtil.registerAfterRollback(() -> rollback.set(true));
        TransactionSynchronizationManager.getSynchronizations().forEach(item -> item.afterCompletion(1));
        assertTrue(complete.get());
        assertTrue(rollback.get());
    }

    @Test
    void shouldRejectNullTransactionTasks() {
        assertThrows(NullPointerException.class, () -> SpringUtil.registerAfterCommit(null));
        assertThrows(NullPointerException.class, () -> SpringUtil.executeInTransaction((Runnable) null));
    }
}
