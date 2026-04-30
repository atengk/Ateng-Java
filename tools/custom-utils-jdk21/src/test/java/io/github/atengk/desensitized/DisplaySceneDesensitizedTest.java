package io.github.atengk.desensitized;

import io.github.atengk.utils.desensitized.DesensitizedUtil;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class DisplaySceneDesensitizedTest {

    @Test
    void shouldMaskDisplayScene() {
        assertEquals("138****5678", DesensitizedUtil.display("13812345678"));
        assertEquals("138****5678", DesensitizedUtil.export("13812345678"));
        assertEquals("138****5678", DesensitizedUtil.print("13812345678"));
        assertEquals("138****5678", DesensitizedUtil.report("13812345678"));
        assertEquals("138****5678", DesensitizedUtil.listView("13812345678"));
        assertEquals("138****5678", DesensitizedUtil.detailView("13812345678"));
        assertEquals("138****5678", DesensitizedUtil.preview("13812345678"));
    }

    @Test
    void shouldHandleBoundaryDisplayScene() {
        assertNull(DesensitizedUtil.display(null));
        assertEquals("", DesensitizedUtil.export(""));
        assertEquals("plain", DesensitizedUtil.preview("plain"));
    }
}
