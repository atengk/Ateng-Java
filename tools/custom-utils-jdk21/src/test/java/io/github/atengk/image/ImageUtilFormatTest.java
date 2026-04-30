package io.github.atengk.image;

import io.github.atengk.utils.ImageUtil;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.awt.Color;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.*;

class ImageUtilFormatTest {

    @TempDir
    Path tempDir;

    @Test
    void shouldDetectAndConvertFormat() {
        Path source = ImageTestSupport.write(tempDir, "source.png", ImageTestSupport.image(12, 8, Color.GREEN), "png");
        Path target = tempDir.resolve("target.jpg");
        assertEquals("png", ImageUtil.getFormat(source));
        ImageUtil.convert(source, "jpg", target);
        assertEquals("jpg", ImageUtil.getFormat(target));
    }

    @Test
    void shouldNormalizeAndCheckSupportedFormats() {
        assertEquals("jpg", ImageUtil.normalizeFormat("JPEG"));
        assertTrue(ImageUtil.isSupportedFormat("png"));
        assertFalse(ImageUtil.isSupportedFormat("bad@format"));
    }

    @Test
    void shouldRejectInvalidFormat() {
        assertThrows(IllegalArgumentException.class, () -> ImageUtil.normalizeFormat(" "));
        assertThrows(IllegalArgumentException.class, () -> ImageUtil.convert(ImageTestSupport.image(1, 1, Color.WHITE), "bad@format"));
    }
}
