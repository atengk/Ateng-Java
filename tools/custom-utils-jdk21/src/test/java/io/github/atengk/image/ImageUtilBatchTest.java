package io.github.atengk.image;

import io.github.atengk.utils.ImageUtil;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.awt.Color;
import java.awt.image.BufferedImage;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class ImageUtilBatchTest {

    @TempDir
    Path tempDir;

    @Test
    void shouldProcessBatchImagesInMemory() {
        BufferedImage image = ImageTestSupport.image(20, 10, Color.BLUE);
        assertEquals(2, ImageUtil.resizeBatch(List.of(image, image), 10, 5).size());
        assertEquals(2, ImageUtil.convertBatch(List.of(image, image), "png").size());
        assertEquals(2, ImageUtil.compressBatch(List.of(ImageTestSupport.pngBytes(image), ImageTestSupport.pngBytes(image)), 10_000).size());
    }

    @Test
    void shouldWalkCheckAndProcessBatchFiles() throws Exception {
        Path a = ImageTestSupport.write(tempDir, "a.png", ImageTestSupport.image(20, 10, Color.BLUE), "png");
        Files.writeString(tempDir.resolve("b.txt"), "not image");
        assertEquals(1, ImageUtil.walkImages(tempDir).size());
        assertTrue(ImageUtil.checkBatch(List.of(a)).getFirst().success());
        Path out = tempDir.resolve("out");
        assertTrue(ImageUtil.processBatch(List.of(a), out, "jpg", 10, 10).getFirst().success());
        assertTrue(Files.exists(out.resolve("a.jpg")));
    }

    @Test
    void shouldRejectInvalidBatchArgs() {
        assertThrows(IllegalArgumentException.class, () -> ImageUtil.resizeBatch(List.of(), 10, 10));
        assertThrows(IllegalArgumentException.class, () -> ImageUtil.compressBatch(List.of(new byte[0]), 10));
        assertThrows(IllegalArgumentException.class, () -> ImageUtil.walkImages(tempDir.resolve("missing")));
    }
}
