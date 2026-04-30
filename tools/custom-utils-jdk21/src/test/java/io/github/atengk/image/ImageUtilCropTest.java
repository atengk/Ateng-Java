package io.github.atengk.image;

import io.github.atengk.utils.ImageUtil;
import org.junit.jupiter.api.Test;

import java.awt.Color;
import java.awt.image.BufferedImage;

import static org.junit.jupiter.api.Assertions.*;

class ImageUtilCropTest {

    @Test
    void shouldCropImages() {
        BufferedImage image = ImageTestSupport.image(100, 60, Color.RED);
        assertEquals(20, ImageUtil.crop(image, 0, 0, 20, 10).getWidth());
        assertEquals(30, ImageUtil.cropCenter(image, 30, 30).getHeight());
        assertTrue(ImageUtil.isCropAreaValid(image, 1, 1, 10, 10));
    }

    @Test
    void shouldCropSafeSquareCircleAndResize() {
        BufferedImage image = ImageTestSupport.image(100, 60, Color.RED);
        assertEquals(10, ImageUtil.cropSafe(image, 95, 55, 20, 20).getWidth());
        assertEquals(60, ImageUtil.cropSquare(image).getWidth());
        assertTrue(ImageUtil.hasAlpha(ImageUtil.cropCircle(image)));
        assertEquals(40, ImageUtil.cropAndResize(image, 40, 40).getWidth());
    }

    @Test
    void shouldRejectInvalidCropArgs() {
        BufferedImage image = ImageTestSupport.image(20, 20, Color.RED);
        assertThrows(IllegalArgumentException.class, () -> ImageUtil.crop(image, -1, 0, 10, 10));
        assertThrows(IllegalArgumentException.class, () -> ImageUtil.cropCenter(image, 30, 30));
        assertThrows(IllegalArgumentException.class, () -> ImageUtil.cropCenterByRatio(image, 0));
    }
}
