package io.github.atengk.image;

import io.github.atengk.utils.ImageUtil;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.awt.Color;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.*;

class ImageUtilReadWriteTest {

    @TempDir
    Path tempDir;

    @Test
    void shouldReadAndWriteImageFile() {
        Path path = ImageTestSupport.write(tempDir, "demo.png", ImageTestSupport.image(20, 10, Color.RED), "png");
        BufferedImage image = ImageUtil.read(path);
        assertEquals(20, image.getWidth());
        assertEquals(10, image.getHeight());
    }

    @Test
    void shouldConvertImageToBytesAndInputStream() {
        BufferedImage image = ImageTestSupport.image(5, 5, Color.BLUE);
        byte[] bytes = ImageUtil.toBytes(image, "png");
        assertTrue(bytes.length > 0);
        assertEquals(5, ImageUtil.read(new ByteArrayInputStream(bytes)).getWidth());
        assertNotNull(ImageUtil.toInputStream(image, "png"));
    }

    @Test
    void shouldHandleInvalidReadCases() {
        assertNull(ImageUtil.readQuietly(tempDir.resolve("missing.png")));
        assertThrows(IllegalArgumentException.class, () -> ImageUtil.read(new byte[]{1, 2, 3}));
        assertThrows(NullPointerException.class, () -> ImageUtil.write(null, "png", tempDir.resolve("x.png")));
    }
}
