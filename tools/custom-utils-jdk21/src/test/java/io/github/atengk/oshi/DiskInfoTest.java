package io.github.atengk.oshi;

import io.github.atengk.utils.oshi.OshiUtil;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class DiskInfoTest {

    @Test
    void normalDiskInfoShouldReturnData() {
        assertNotNull(OshiUtil.listDisks());
        assertNotNull(OshiUtil.listPartitions());
        assertNotNull(OshiUtil.listFileStores());
        assertTrue(OshiUtil.getDiskUsage() >= 0);
        assertNotNull(OshiUtil.snapshotDisk());
    }

    @Test
    void diskSpaceShouldBeNonNegative() {
        assertTrue(OshiUtil.getTotalDiskSpace() >= 0);
        assertTrue(OshiUtil.getUsableDiskSpace() >= 0);
        assertTrue(OshiUtil.getUsedDiskSpace() >= 0);
    }

    @Test
    void invalidDiskParametersShouldThrow() {
        assertThrows(IllegalArgumentException.class, () -> OshiUtil.getDiskInfo(" "));
        assertThrows(IllegalArgumentException.class, () -> OshiUtil.getFileStore(null));
        assertThrows(IllegalArgumentException.class, () -> OshiUtil.getDiskUsage("/path/not-exists-for-test"));
    }
}
