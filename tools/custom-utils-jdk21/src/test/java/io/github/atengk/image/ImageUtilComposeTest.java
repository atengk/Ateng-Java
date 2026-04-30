package io.github.atengk.image;

import io.github.atengk.utils.ImageUtil;
import org.junit.jupiter.api.Test;

import java.awt.Color;
import java.awt.image.BufferedImage;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class ImageUtilComposeTest {

    @Test
    void shouldCreateCanvasAndOverlay() {
        BufferedImage canvas = ImageUtil.createCanvas(30, 20, Color.WHITE);
        BufferedImage transparent = ImageUtil.createTransparentCanvas(10, 10);
        assertEquals(30, canvas.getWidth());
        assertTrue(ImageUtil.hasAlpha(transparent));
        assertEquals(30, ImageUtil.overlay(canvas, ImageTestSupport.image(5, 5, Color.RED), 1, 1).getWidth());
        assertEquals(30, ImageUtil.overlayCenter(canvas, ImageTestSupport.image(5, 5, Color.RED)).getWidth());
    }

    @Test
    void shouldConcatPadAndBorder() {
        BufferedImage a = ImageTestSupport.image(10, 10, Color.RED);
        BufferedImage b = ImageTestSupport.image(20, 10, Color.BLUE);
        assertEquals(30, ImageUtil.concatHorizontal(List.of(a, b)).getWidth());
        assertEquals(20, ImageUtil.concatVertical(List.of(a, b)).getHeight());
        assertEquals(40, ImageUtil.concatGrid(List.of(a, b, a), 2).getWidth());
        assertEquals(14, ImageUtil.padding(a, 2, Color.WHITE).getWidth());
        assertEquals(14, ImageUtil.border(a, 2, Color.BLACK).getWidth());
    }

    @Test
    void shouldRejectInvalidComposeArgs() {
        assertThrows(IllegalArgumentException.class, () -> ImageUtil.createCanvas(0, 10, Color.WHITE));
        assertThrows(IllegalArgumentException.class, () -> ImageUtil.concatHorizontal(List.of()));
        assertThrows(IllegalArgumentException.class, () -> ImageUtil.concatGrid(List.of(ImageTestSupport.image(1, 1, Color.RED)), 0));
    }
}
