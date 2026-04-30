package io.github.atengk.pdf;

import io.github.atengk.utils.pdf.PDFUtil;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class PDFUtilFontStyleTest {

    @Test
    void defaultFontsShouldBeCreated() {
        assertNotNull(PDFUtil.createDefaultFont());
        assertNotNull(PDFUtil.createDefaultFont(10));
        assertNotNull(PDFUtil.createChineseFont());
        assertNotNull(PDFUtil.createBoldFont(12));
        assertNotNull(PDFUtil.createTitleFont());
        assertNotNull(PDFUtil.createTableHeaderFont());
        assertNotNull(PDFUtil.createTableBodyFont());
        assertNotNull(PDFUtil.createTextStyle());
        assertNotNull(PDFUtil.createCellStyle());
    }

    @Test
    void invalidFontArgumentsShouldThrow() {
        assertThrows(IllegalArgumentException.class, () -> PDFUtil.createDefaultFont(0));
        assertThrows(IllegalArgumentException.class, () -> PDFUtil.createBoldFont(-1));
        assertThrows(IllegalArgumentException.class, () -> PDFUtil.createFont("", 12));
        assertThrows(IllegalArgumentException.class, () -> PDFUtil.registerFont(" "));
        assertThrows(IllegalArgumentException.class, () -> PDFUtil.registerFonts(" "));
    }
}
