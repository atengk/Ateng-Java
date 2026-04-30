package io.github.atengk.id;

import io.github.atengk.utils.id.IdUtil;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class DistributedIdUtilTest {

    @Test
    void shouldGetWorkerAndDataCenterIds() {
        assertTrue(IdUtil.workerId() >= 0 && IdUtil.workerId() <= 31);
        assertTrue(IdUtil.dataCenterId() >= 0 && IdUtil.dataCenterId() <= 31);
        assertTrue(IdUtil.workerIdByIp() >= 0 && IdUtil.workerIdByIp() <= 31);
        assertTrue(IdUtil.workerIdByHostName() >= 0 && IdUtil.workerIdByHostName() <= 31);
    }

    @Test
    void shouldCheckWorkerAndDataCenterRange() {
        assertDoesNotThrow(() -> IdUtil.checkWorkerId(0));
        assertDoesNotThrow(() -> IdUtil.checkWorkerId(31));
        assertDoesNotThrow(() -> IdUtil.checkDataCenterId(0));
        assertDoesNotThrow(() -> IdUtil.checkDataCenterId(31));
        assertThrows(IllegalArgumentException.class, () -> IdUtil.checkWorkerId(-1));
        assertThrows(IllegalArgumentException.class, () -> IdUtil.checkWorkerId(32));
        assertThrows(IllegalArgumentException.class, () -> IdUtil.checkDataCenterId(-1));
        assertThrows(IllegalArgumentException.class, () -> IdUtil.checkDataCenterId(32));
    }

    @Test
    void shouldRejectInvalidEnvWorkerId() {
        assertThrows(IllegalArgumentException.class, () -> IdUtil.workerIdByEnv(""));
        assertThrows(IllegalArgumentException.class, () -> IdUtil.workerIdByEnv("__ID_UTIL_NOT_EXIST_ENV__"));
    }
}
