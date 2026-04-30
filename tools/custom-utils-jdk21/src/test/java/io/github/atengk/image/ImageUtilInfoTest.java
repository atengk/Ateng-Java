package io.github.atengk.image;

import io.github.atengk.utils.ImageUtil;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.awt.Color;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.*;

class ImageUtilInfoTest {

    @TempDir
    Path tempDir;

    @Test
    void shouldGetBasicInfo() {
        Path path = ImageTestSupport.write(tempDir, "info.png", ImageTestSupport.image(40, 20, Color.RED), "png");
        ImageUtil.ImageInfo info = ImageUtil.getInfo(path);
        assertEquals(40, info.width());
        assertEquals(20, info.height());
        assertEquals("png", info.format());
        assertEquals("image/png", info.mimeType());
        assertTrue(info.fileSize() > 0);
    }

    @Test
    void shouldCheckImageDirection() {
        assertTrue(ImageUtil.isLandscape(ImageTestSupport.image(40, 20, Color.RED)));
        assertTrue(ImageUtil.isPortrait(ImageTestSupport.image(20, 40, Color.RED)));
        assertTrue(ImageUtil.isSquare(ImageTestSupport.image(20, 20, Color.RED)));
    }

    @Test
    void shouldRejectInvalidInfoInput() {
        assertThrows(NullPointerException.class, () -> ImageUtil.getWidth(null));
        assertThrows(IllegalArgumentException.class, () -> new ImageUtil.ImageSize(0, 1));
    }
}
