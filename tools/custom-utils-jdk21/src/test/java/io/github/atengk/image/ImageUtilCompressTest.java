package io.github.atengk.image;

import io.github.atengk.utils.ImageUtil;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.awt.Color;
import java.awt.image.BufferedImage;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.*;

class ImageUtilCompressTest {

    @TempDir
    Path tempDir;

    @Test
    void shouldCompressImageWithQuality() {
        BufferedImage image = ImageTestSupport.image(200, 100, Color.ORANGE);
        byte[] bytes = ImageUtil.compress(image, 0.8f);
        assertTrue(bytes.length > 0);
        assertTrue(ImageUtil.isImage(bytes));
    }

    @Test
    void shouldCompressFileAndSize() throws Exception {
        Path source = ImageTestSupport.write(tempDir, "source.png", ImageTestSupport.image(100, 100, Color.ORANGE), "png");
        Path target = tempDir.resolve("target.jpg");
        ImageUtil.compress(source, target, 0.7f);
        assertTrue(Files.size(target) > 0);
        assertTrue(ImageUtil.needCompress(target, 1));
        assertEquals(0.9f, ImageUtil.getRecommendedQuality(1000), 0.001f);
    }

    @Test
    void shouldRejectInvalidCompressArgs() {
        BufferedImage image = ImageTestSupport.image(20, 20, Color.ORANGE);
        assertThrows(IllegalArgumentException.class, () -> ImageUtil.compress(image, 0.01f));
        assertThrows(IllegalArgumentException.class, () -> ImageUtil.compressToSize(new byte[]{1, 2}, 0));
        assertThrows(IllegalArgumentException.class, () -> ImageUtil.getRecommendedQuality(0));
    }
}
