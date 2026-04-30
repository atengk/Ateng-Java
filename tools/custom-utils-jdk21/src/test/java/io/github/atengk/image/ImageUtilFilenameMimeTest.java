package io.github.atengk.image;

import io.github.atengk.utils.ImageUtil;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class ImageUtilFilenameMimeTest {

    @Test
    void shouldHandleExtensionAndMimeType() {
        assertEquals("png", ImageUtil.getExtension("a.b.png"));
        assertEquals("", ImageUtil.getExtension("filename"));
        assertEquals("jpg", ImageUtil.getExtensionByFormat("jpeg"));
        assertEquals("image/jpeg", ImageUtil.getMimeType("jpg"));
        assertEquals("png", ImageUtil.getFormatByMimeType("image/png"));
    }

    @Test
    void shouldGenerateFilenameAndCheckSupport() {
        String filename = ImageUtil.generateFilename("png");
        assertTrue(filename.endsWith(".png"));
        assertTrue(ImageUtil.isSupportedExtension(".jpg"));
        assertTrue(ImageUtil.isSupportedMimeType("image/jpeg"));
        assertFalse(ImageUtil.isSupportedMimeType("text/plain"));
    }

    @Test
    void shouldRejectInvalidFilenameMimeArgs() {
        assertThrows(IllegalArgumentException.class, () -> ImageUtil.getExtension(" "));
        assertThrows(IllegalArgumentException.class, () -> ImageUtil.getFormatByMimeType("text/plain"));
        assertThrows(IllegalArgumentException.class, () -> ImageUtil.generateFilename("bad@format"));
    }
}
