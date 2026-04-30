package io.github.atengk.image;

import io.github.atengk.utils.ImageUtil;
import org.junit.jupiter.api.Test;

import java.awt.Color;
import java.awt.image.BufferedImage;

import static org.junit.jupiter.api.Assertions.*;

class ImageUtilColorAlphaTest {

    @Test
    void shouldHandleColorConversions() {
        BufferedImage image = ImageTestSupport.image(10, 10, Color.RED);
        assertEquals(BufferedImage.TYPE_BYTE_GRAY, ImageUtil.toGray(image).getType());
        assertEquals(BufferedImage.TYPE_BYTE_BINARY, ImageUtil.toBinary(image).getType());
        assertNotEquals(image.getRGB(0, 0), ImageUtil.invertColor(image).getRGB(0, 0));
    }

    @Test
    void shouldHandleAlphaAndBackground() {
        BufferedImage argb = ImageTestSupport.argbImage(10, 10, new Color(255, 0, 0, 128));
        assertTrue(ImageUtil.hasAlpha(argb));
        assertTrue(ImageUtil.hasAlpha(ImageUtil.changeAlpha(argb, 0.5f)));
        assertFalse(ImageUtil.hasAlpha(ImageUtil.removeAlpha(argb, Color.WHITE)));
        assertEquals(10, ImageUtil.replaceBackground(argb, new Color(255, 0, 0, 128), Color.BLUE, 255).getWidth());
    }

    @Test
    void shouldRejectInvalidColorArgs() {
        BufferedImage image = ImageTestSupport.image(10, 10, Color.RED);
        assertThrows(IllegalArgumentException.class, () -> ImageUtil.changeAlpha(image, 2f));
        assertThrows(NullPointerException.class, () -> ImageUtil.removeAlpha(image, null));
        assertThrows(IllegalArgumentException.class, () -> ImageUtil.replaceBackground(image, Color.RED, Color.BLUE, 256));
    }
}
