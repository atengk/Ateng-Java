package io.github.atengk.filetype;

import io.github.atengk.utils.filetype.FileTypeUtil;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class FileTypeUtilMimeTypeTest {

    @Test
    void shouldParseMimeTypeParts() {
        assertEquals("image", FileTypeUtil.getMajorType("image/png"));
        assertEquals("png", FileTypeUtil.getSubType("image/png"));
        assertEquals("text/plain", FileTypeUtil.normalizeMimeType(" Text/Plain; charset=UTF-8 "));
    }

    @Test
    void shouldMatchMimeTypePatterns() {
        assertTrue(FileTypeUtil.isMimeType("application/pdf", "APPLICATION/PDF"));
        assertTrue(FileTypeUtil.isMimeTypeMatched("image/png", "image/*"));
        assertTrue(FileTypeUtil.isMimeTypeMatched("image/png", "*/*"));
        assertTrue(FileTypeUtil.isAnyMimeTypeMatched("video/mp4", List.of("image/*", "video/*")));
        assertFalse(FileTypeUtil.isMimeTypeMatched("application/pdf", "image/*"));
    }

    @Test
    void shouldIdentifyMimeTypeStatus() {
        assertTrue(FileTypeUtil.isKnownMimeType("application/pdf"));
        assertTrue(FileTypeUtil.isUnknownMimeType("application/octet-stream"));
        assertTrue(FileTypeUtil.isApplicationType("application/pdf"));
        assertTrue(FileTypeUtil.isTextType("text/plain"));
        assertTrue(FileTypeUtil.isBinaryType("application/pdf"));
        assertFalse(FileTypeUtil.isBinaryType("text/plain"));
    }

    @Test
    void shouldHandleInvalidMimeValues() {
        assertEquals("", FileTypeUtil.getMajorType(null));
        assertEquals("", FileTypeUtil.getSubType("invalid"));
        assertFalse(FileTypeUtil.isMimeType(null, "application/pdf"));
        assertFalse(FileTypeUtil.isAnyMimeTypeMatched("image/png", null));
    }
}
