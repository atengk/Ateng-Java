package io.github.atengk.image;

import io.github.atengk.utils.ImageUtil;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.awt.Color;
import java.awt.image.BufferedImage;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.*;

class ImageUtilTransformTest {

    @TempDir
    Path tempDir;

    @Test
    void shouldRotateAndFlipImage() {
        BufferedImage image = ImageTestSupport.image(30, 10, Color.CYAN);
        assertEquals(10, ImageUtil.rotateRight(image).getWidth());
        assertEquals(10, ImageUtil.rotateLeft(image).getWidth());
        assertEquals(30, ImageUtil.rotate180(image).getWidth());
        assertEquals(30, ImageUtil.flipHorizontal(image).getWidth());
        assertEquals(10, ImageUtil.flipVertical(image).getHeight());
    }

    @Test
    void shouldFixOrientation() {
        BufferedImage image = ImageTestSupport.image(30, 10, Color.CYAN);
        assertEquals(10, ImageUtil.fixOrientation(image, 6).getWidth());
        assertEquals(30, ImageUtil.fixOrientation(image, 1).getWidth());
        Path path = ImageTestSupport.write(tempDir, "orientation.png", image, "png");
        assertEquals(1, ImageUtil.getOrientation(path));
    }

    @Test
    void shouldRejectInvalidTransformArgs() {
        BufferedImage image = ImageTestSupport.image(10, 10, Color.CYAN);
        assertThrows(IllegalArgumentException.class, () -> ImageUtil.rotate(image, Double.NaN));
        assertThrows(NullPointerException.class, () -> ImageUtil.flipHorizontal(null));
        assertThrows(IllegalArgumentException.class, () -> ImageUtil.getOrientation(new byte[]{1, 2, 3}));
    }
}
