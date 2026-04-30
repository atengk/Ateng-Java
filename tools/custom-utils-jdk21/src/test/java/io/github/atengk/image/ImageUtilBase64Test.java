package io.github.atengk.image;

import io.github.atengk.utils.ImageUtil;
import org.junit.jupiter.api.Test;

import java.awt.Color;
import java.awt.image.BufferedImage;
import java.util.Base64;

import static org.junit.jupiter.api.Assertions.*;

class ImageUtilBase64Test {

    @Test
    void shouldConvertBase64Image() {
        BufferedImage image = ImageTestSupport.image(10, 8, Color.RED);
        String base64 = ImageUtil.toBase64(image, "png");
        assertTrue(ImageUtil.isBase64Image(base64));
        assertEquals(10, ImageUtil.fromBase64(base64).getWidth());
    }

    @Test
    void shouldConvertDataUrl() {
        String dataUrl = ImageUtil.toDataUrl(ImageTestSupport.image(10, 8, Color.RED), "png");
        assertEquals("image/png", ImageUtil.getDataUrlMimeType(dataUrl));
        assertTrue(ImageUtil.removeDataUrlPrefix(dataUrl).length() > 0);
        assertEquals(8, ImageUtil.fromDataUrl(dataUrl).getHeight());
    }

    @Test
    void shouldRejectInvalidBase64Args() {
        assertFalse(ImageUtil.isBase64Image(Base64.getEncoder().encodeToString(new byte[]{1, 2, 3})));
        assertThrows(IllegalArgumentException.class, () -> ImageUtil.fromDataUrl("abc"));
        assertThrows(IllegalArgumentException.class, () -> ImageUtil.getDataUrlMimeType("abc"));
    }
}
