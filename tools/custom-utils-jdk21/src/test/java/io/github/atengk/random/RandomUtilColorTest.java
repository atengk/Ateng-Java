package io.github.atengk.random;

import io.github.atengk.utils.random.RandomUtil;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class RandomUtilColorTest {

    @Test
    void shouldGenerateColors() {
        assertTrue(RandomUtil.randomHexColor().matches("#[0-9A-F]{6}"));
        assertTrue(RandomUtil.randomRgbColor().matches("rgb\\(\\d{1,3},\\d{1,3},\\d{1,3}\\)"));
        assertTrue(RandomUtil.randomRgbaColor(0.5).matches("rgba\\(\\d{1,3},\\d{1,3},\\d{1,3},0.5\\)"));
        assertFalse(RandomUtil.randomColorName().isBlank());
        assertTrue(RandomUtil.randomLightColor().matches("#[0-9A-F]{6}"));
        assertTrue(RandomUtil.randomDarkColor().matches("#[0-9A-F]{6}"));
    }

    @Test
    void shouldRejectInvalidAlpha() {
        assertThrows(IllegalArgumentException.class, () -> RandomUtil.randomRgbaColor(-0.1));
        assertThrows(IllegalArgumentException.class, () -> RandomUtil.randomRgbaColor(1.1));
    }
}
