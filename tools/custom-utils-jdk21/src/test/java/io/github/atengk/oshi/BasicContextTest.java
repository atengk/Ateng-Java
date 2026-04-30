package io.github.atengk.oshi;

import io.github.atengk.utils.oshi.OshiUtil;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Constructor;

import static org.junit.jupiter.api.Assertions.*;

class BasicContextTest {

    @Test
    void normalContextShouldNotBeNull() {
        assertNotNull(OshiUtil.getSystemInfo());
        assertNotNull(OshiUtil.getHardware());
        assertNotNull(OshiUtil.getOperatingSystem());
        assertNotNull(OshiUtil.getOshiVersion());
    }

    @Test
    void refreshAndClearCacheShouldWork() {
        OshiUtil.refresh();
        assertNotNull(OshiUtil.getSystemInfo());
        OshiUtil.clearCache();
        assertNotNull(OshiUtil.getHardware());
    }

    @Test
    void constructorShouldThrowException() throws Exception {
        Constructor<OshiUtil> constructor = OshiUtil.class.getDeclaredConstructor();
        constructor.setAccessible(true);
        Exception exception = assertThrows(Exception.class, constructor::newInstance);
        assertNotNull(exception.getCause());
    }
}
