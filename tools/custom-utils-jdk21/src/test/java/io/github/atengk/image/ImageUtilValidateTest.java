package io.github.atengk.image;

import io.github.atengk.utils.ImageUtil;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.awt.Color;
import java.awt.image.BufferedImage;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.*;

class ImageUtilValidateTest {

    @TempDir
    Path tempDir;

    @Test
    void shouldValidateImageFileAndBytes() {
        BufferedImage image = ImageTestSupport.image(20, 10, Color.RED);
        Path path = ImageTestSupport.write(tempDir, "v.png", image, "png");
        assertTrue(ImageUtil.isImage(path));
        assertTrue(ImageUtil.isImage(ImageTestSupport.pngBytes(image)));
        ImageUtil.checkFormat(path, "png");
        ImageUtil.checkFormat(ImageTestSupport.pngBytes(image), "png");
    }

    @Test
    void shouldCheckSizeDimensionAndPixels() throws Exception {
        Path path = ImageTestSupport.write(tempDir, "v.png", ImageTestSupport.image(20, 10, Color.RED), "png");
        ImageUtil.checkFileSize(path, Files.size(path) + 1);
        ImageUtil.checkDimension(ImageUtil.read(path), 20, 10);
        ImageUtil.checkMaxPixels(ImageUtil.read(path), 200);
        assertFalse(ImageUtil.isOverSize(path, Files.size(path) + 1));
        assertFalse(ImageUtil.isOverPixels(ImageUtil.read(path), 200));
    }

    @Test
    void shouldRejectInvalidValidationCases() throws Exception {
        Path text = tempDir.resolve("a.txt");
        Files.writeString(text, "not image");
        assertFalse(ImageUtil.isImage(text));
        assertThrows(IllegalArgumentException.class, () -> ImageUtil.checkReadable(text));
        assertThrows(IllegalArgumentException.class, () -> ImageUtil.checkDimension(ImageTestSupport.image(20, 10, Color.RED), 19, 10));
        assertThrows(IllegalArgumentException.class, () -> ImageUtil.checkFormat(ImageTestSupport.pngBytes(ImageTestSupport.image(1, 1, Color.RED)), "jpg"));
    }
}
