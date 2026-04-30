package io.github.atengk.image;

import io.github.atengk.utils.ImageUtil;
import org.junit.jupiter.api.Test;

import java.awt.Color;
import java.awt.image.BufferedImage;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class ImageUtilThumbnailTest {

    @Test
    void shouldCreateThumbnailAvatarAndCover() {
        BufferedImage image = ImageTestSupport.image(100, 50, Color.PINK);
        assertEquals(20, ImageUtil.thumbnail(image, 20, 20).getWidth());
        assertEquals(40, ImageUtil.avatar(image, 40).getHeight());
        assertTrue(ImageUtil.hasAlpha(ImageUtil.circleAvatar(image, 40)));
        assertEquals(16, ImageUtil.cover(image, 16, 9).getWidth());
    }

    @Test
    void shouldCreatePreviewAndBatchThumbnail() {
        BufferedImage image = ImageTestSupport.image(100, 50, Color.PINK);
        assertEquals(50, ImageUtil.preview(image, 50).getWidth());
        assertEquals(2, ImageUtil.thumbnailBatch(List.of(image, image), 10, 10).size());
        assertEquals(50, ImageUtil.thumbnailFit(image, 50, 50).getWidth());
    }

    @Test
    void shouldRejectInvalidThumbnailArgs() {
        BufferedImage image = ImageTestSupport.image(10, 10, Color.PINK);
        assertThrows(IllegalArgumentException.class, () -> ImageUtil.avatar(image, 0));
        assertThrows(IllegalArgumentException.class, () -> ImageUtil.thumbnailBatch(List.of(), 10, 10));
        assertThrows(NullPointerException.class, () -> ImageUtil.preview(null, 10));
    }
}
