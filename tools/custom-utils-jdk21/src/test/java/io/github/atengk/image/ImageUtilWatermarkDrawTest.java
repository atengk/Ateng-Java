package io.github.atengk.image;

import io.github.atengk.utils.ImageUtil;
import org.junit.jupiter.api.Test;

import java.awt.Color;
import java.awt.Font;
import java.awt.image.BufferedImage;

import static org.junit.jupiter.api.Assertions.*;

class ImageUtilWatermarkDrawTest {

    @Test
    void shouldAddImageAndTextWatermark() {
        BufferedImage base = ImageTestSupport.image(100, 60, Color.GRAY);
        BufferedImage watermark = ImageTestSupport.image(20, 10, Color.RED);
        assertEquals(100, ImageUtil.addImageWatermark(base, watermark, 5, 5).getWidth());
        assertEquals(100, ImageUtil.addCenterWatermark(base, watermark, 0.5f).getWidth());
        assertEquals(100, ImageUtil.addTextWatermark(base, "测试", 10, 20).getWidth());
    }

    @Test
    void shouldDrawShapesAndTileWatermark() {
        BufferedImage base = ImageTestSupport.image(100, 60, Color.GRAY);
        assertEquals(100, ImageUtil.addTileWatermark(base, "A", new Font(Font.SANS_SERIF, Font.PLAIN, 12), Color.WHITE, 0.3f, 10).getWidth());
        assertEquals(100, ImageUtil.drawRect(base, 1, 1, 20, 20, Color.RED).getWidth());
        assertEquals(100, ImageUtil.drawRoundRect(base, 1, 1, 20, 20, 4, 4, Color.RED).getWidth());
        assertEquals(100, ImageUtil.drawImageWithAlpha(base, ImageTestSupport.image(5, 5, Color.BLUE), 0, 0, 0.5f).getWidth());
    }

    @Test
    void shouldRejectInvalidWatermarkArgs() {
        BufferedImage base = ImageTestSupport.image(20, 20, Color.GRAY);
        assertThrows(IllegalArgumentException.class, () -> ImageUtil.addImageWatermark(base, base, 0, 0, -0.1f));
        assertThrows(IllegalArgumentException.class, () -> ImageUtil.addTileWatermark(base, " ", new Font(Font.SANS_SERIF, Font.PLAIN, 12), Color.WHITE, 0.5f, 10));
        assertThrows(IllegalArgumentException.class, () -> ImageUtil.drawRect(base, 0, 0, 0, 1, Color.RED));
    }
}
