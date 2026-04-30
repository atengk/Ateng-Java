package io.github.atengk.system;

import io.github.atengk.utils.SystemUtil;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class SystemUtilContainerTest {

    @Test
    void shouldDetectContainerEnvironmentWithoutException() {
        assertDoesNotThrow(SystemUtil::isDocker);
        assertDoesNotThrow(SystemUtil::isKubernetes);
        assertDoesNotThrow(SystemUtil::isContainer);
    }

    @Test
    void shouldReadContainerMetadataSafely() {
        assertNotNull(SystemUtil.getContainerId());
        assertNotNull(SystemUtil.getPodName());
        assertNotNull(SystemUtil.getNamespace());
        assertNotNull(SystemUtil.getNodeName());
    }

    @Test
    void shouldDetectCiEnvironmentSafely() {
        assertDoesNotThrow(SystemUtil::isCiEnv);
        assertNotNull(SystemUtil.getCiName());
    }
}
