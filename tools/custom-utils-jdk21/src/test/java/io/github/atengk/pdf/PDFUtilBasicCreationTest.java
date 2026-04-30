package io.github.atengk.pdf;

import io.github.atengk.pdf.testsupport.PdfTestSupport;
import io.github.atengk.utils.pdf.PDFUtil;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.ByteArrayOutputStream;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.*;

class PDFUtilBasicCreationTest {

    @TempDir
    Path dir;

    @Test
    void writeToFileShouldCreatePdf() throws Exception {
        Path pdf = dir.resolve("basic.pdf");
        PDFUtil.writeToFile(pdf, document -> PDFUtil.addParagraph(document, "hello"));
        PdfTestSupport.assertPdfExists(pdf);
        assertEquals(1, PDFUtil.getPageCount(pdf));
    }

    @Test
    void writeToBytesShouldReturnPdfBytes() {
        byte[] bytes = PDFUtil.writeToBytes(document -> PDFUtil.addParagraph(document, "bytes"));
        assertTrue(PDFUtil.isPdf(bytes));
    }

    @Test
    void writeToStreamShouldRejectNullBuilder() {
        assertThrows(NullPointerException.class, () -> PDFUtil.writeToStream(new ByteArrayOutputStream(), null));
    }

    @Test
    void constructorShouldRejectReflectionInstantiation() throws Exception {
        Constructor<PDFUtil> constructor = PDFUtil.class.getDeclaredConstructor();
        constructor.setAccessible(true);
        InvocationTargetException exception = assertThrows(InvocationTargetException.class, constructor::newInstance);
        assertInstanceOf(UnsupportedOperationException.class, exception.getCause());
    }
}
