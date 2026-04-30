package io.github.atengk.image;

import io.github.atengk.utils.ImageUtil;
import org.junit.jupiter.api.Test;

import java.awt.Color;
import java.awt.image.BufferedImage;

import static org.junit.jupiter.api.Assertions.*;

class ImageUtilResizeTest {

    @Test
    void shouldResizeWithDifferentModes() {
        BufferedImage image = ImageTestSupport.image(100, 50, Color.RED);
        assertEquals(20, ImageUtil.resize(image, 20, 10).getWidth());
        assertEquals(20, ImageUtil.resizeByWidth(image, 20).getWidth());
        assertEquals(10, ImageUtil.resizeByHeight(image, 10).getHeight());
    }

    @Test
    void shouldFitFillAndScale() {
        BufferedImage image = ImageTestSupport.image(100, 50, Color.RED);
        assertEquals(50, ImageUtil.fit(image, 50, 50).getWidth());
        assertEquals(30, ImageUtil.fill(image, 30, 30).getWidth());
        assertEquals(50, ImageUtil.scale(image, 0.5).getWidth());
        assertTrue(ImageUtil.needResize(image, 80, 80));
    }

    @Test
    void shouldRejectInvalidResizeArgs() {
        BufferedImage image = ImageTestSupport.image(10, 10, Color.RED);
        assertThrows(IllegalArgumentException.class, () -> ImageUtil.resize(image, 0, 10));
        assertThrows(IllegalArgumentException.class, () -> ImageUtil.scale(image, 0));
        assertThrows(NullPointerException.class, () -> ImageUtil.fit(null, 10, 10));
    }
}
