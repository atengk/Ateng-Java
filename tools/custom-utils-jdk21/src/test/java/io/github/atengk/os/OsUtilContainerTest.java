package io.github.atengk.os;

import io.github.atengk.utils.OsUtil;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class OsUtilContainerTest {

    @Test
    void shouldDetectContainerEnvironmentWithoutException() {
        assertDoesNotThrow(OsUtil::isDocker);
        assertDoesNotThrow(OsUtil::isKubernetes);
        assertDoesNotThrow(OsUtil::isContainer);
    }

    @Test
    void shouldReadContainerMetadataSafely() {
        assertNotNull(OsUtil.getContainerId());
        assertNotNull(OsUtil.getPodName());
        assertNotNull(OsUtil.getNamespace());
        assertNotNull(OsUtil.getNodeName());
    }

    @Test
    void shouldDetectCiEnvironmentSafely() {
        assertDoesNotThrow(OsUtil::isCiEnv);
        assertNotNull(OsUtil.getCiName());
    }
}
