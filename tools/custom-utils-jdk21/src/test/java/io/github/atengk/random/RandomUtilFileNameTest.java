package io.github.atengk.random;

import io.github.atengk.utils.random.RandomUtil;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class RandomUtilFileNameTest {

    @Test
    void shouldGenerateFileNamesAndObjectKeys() {
        assertTrue(RandomUtil.randomFileName().matches("[0-9a-f]{32}"));
        assertTrue(RandomUtil.randomFileName("txt").endsWith(".txt"));
        assertTrue(RandomUtil.randomFileName("avatar-", ".png").startsWith("avatar-"));
        assertEquals(8, RandomUtil.randomPathSegment(8).length());
        assertTrue(RandomUtil.randomObjectKey("upload", "jpg").startsWith("upload/"));
        assertTrue(RandomUtil.randomTempName().startsWith("tmp-"));
    }

    @Test
    void shouldHandleBoundaryAndException() {
        assertEquals("", RandomUtil.randomPathSegment(0));
        assertThrows(NullPointerException.class, () -> RandomUtil.randomFileName(null, "txt"));
        assertThrows(IllegalArgumentException.class, () -> RandomUtil.randomPathSegment(-1));
    }
}
