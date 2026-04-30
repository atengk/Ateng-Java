package io.github.atengk.image;

import io.github.atengk.utils.ImageUtil;
import org.junit.jupiter.api.Test;

import java.awt.Color;
import java.awt.image.BufferedImage;

import static org.junit.jupiter.api.Assertions.*;

class ImageUtilQualityDetectTest {

    @Test
    void shouldDetectBlankTransparentAndSolidColor() {
        BufferedImage white = ImageTestSupport.image(5, 5, Color.WHITE);
        BufferedImage transparent = ImageUtil.createTransparentCanvas(5, 5);
        assertTrue(ImageUtil.isBlank(white));
        assertTrue(ImageUtil.isTransparent(transparent));
        assertTrue(ImageUtil.isSolidColor(white));
        assertFalse(ImageUtil.isSolidColor(ImageTestSupport.mixedImage()));
    }

    @Test
    void shouldCalculateColorBrightnessSimilarityAndHash() {
        BufferedImage black = ImageTestSupport.image(5, 5, Color.BLACK);
        BufferedImage white = ImageTestSupport.image(5, 5, Color.WHITE);
        assertEquals(Color.BLACK.getRed(), ImageUtil.getAverageColor(black).getRed());
        assertTrue(ImageUtil.isDark(black));
        assertTrue(ImageUtil.isBright(white));
        assertEquals(1.0, ImageUtil.similarity(black, black), 0.0001);
        assertEquals(64, ImageUtil.perceptualHash(black).length());
    }

    @Test
    void shouldRejectInvalidQualityDetectArgs() {
        assertThrows(NullPointerException.class, () -> ImageUtil.isBlank(null));
        assertThrows(NullPointerException.class, () -> ImageUtil.similarity(null, ImageTestSupport.image(1, 1, Color.WHITE)));
    }
}
