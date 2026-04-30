package io.github.atengk.desensitized;

import io.github.atengk.utils.desensitized.DesensitizedUtil;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class GeneralMaskingTest {

    @Test
    void shouldMaskCommonString() {
        assertEquals("138****5678", DesensitizedUtil.mask("13812345678", 3, 4));
        assertEquals("ab**ef", DesensitizedUtil.maskRange("abcdef", 2, 4));
        assertEquals("***def", DesensitizedUtil.maskLeft("abcdef", 3));
        assertEquals("abc***", DesensitizedUtil.maskRight("abcdef", 3));
        assertEquals("abc***", DesensitizedUtil.keepLeft("abcdef", 3));
        assertEquals("***def", DesensitizedUtil.keepRight("abcdef", 3));
        assertEquals("ab##ef", DesensitizedUtil.customMask("abcdef", 2, 2, '#'));
    }

    @Test
    void shouldHandleBoundaryValue() {
        assertNull(DesensitizedUtil.mask(null, 1, 1));
        assertEquals("", DesensitizedUtil.mask("", 1, 1));
        assertEquals("abc", DesensitizedUtil.mask("abc", 2, 1));
        assertEquals("abcdef", DesensitizedUtil.maskRange("abcdef", 6, 6));
        assertEquals("*****", DesensitizedUtil.fixedMask("abc", 5));
        assertNull(DesensitizedUtil.fixedMask(null, 5));
        assertEquals("***", DesensitizedUtil.fullMask("abc"));
        assertEquals("--", DesensitizedUtil.emptyToDefaultMask(" ", "--"));
    }

    @Test
    void shouldThrowWhenArgumentInvalid() {
        assertThrows(IllegalArgumentException.class, () -> DesensitizedUtil.mask("abc", -1, 1));
        assertThrows(IllegalArgumentException.class, () -> DesensitizedUtil.maskRange("abc", 2, 1));
        assertThrows(IllegalArgumentException.class, () -> DesensitizedUtil.fixedMask("abc", -1));
    }
}
