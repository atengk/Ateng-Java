package io.github.atengk.image;

import io.github.atengk.utils.ImageUtil;
import org.junit.jupiter.api.Test;

import java.awt.Color;

import static org.junit.jupiter.api.Assertions.*;

class ImageUtilWebTest {

    @Test
    void shouldGetWebResponseValues() {
        assertEquals("image/png", ImageUtil.getContentType("png"));
        assertEquals("demo.jpg", ImageUtil.getDownloadFilename("demo.png", "jpg"));
        assertTrue(ImageUtil.toResponseBytes(ImageTestSupport.image(5, 5, Color.RED), "png").length > 0);
    }

    @Test
    void shouldCheckWebSafeFormatAndCacheKey() {
        assertTrue(ImageUtil.isWebSafeFormat("png"));
        assertFalse(ImageUtil.isWebSafeFormat("bad@format"));
        assertEquals("img1:resize:100:200", ImageUtil.getCacheKey("img1", "resize", 100, 200));
    }

    @Test
    void shouldRejectInvalidWebArgs() {
        assertThrows(IllegalArgumentException.class, () -> ImageUtil.getContentType(" "));
        assertThrows(IllegalArgumentException.class, () -> ImageUtil.getDownloadFilename(" ", "png"));
        assertThrows(IllegalArgumentException.class, () -> ImageUtil.getCacheKey(" ", "resize"));
    }
}
