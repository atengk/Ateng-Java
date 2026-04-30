package io.github.atengk.pdf.testsupport;

import io.github.atengk.utils.pdf.PDFUtil;

import javax.imageio.ImageIO;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

public final class PdfTestSupport {

    private PdfTestSupport() {
    }

    public static Path createPdf(Path dir, String name, String text) {
        Path pdf = dir.resolve(name);
        PDFUtil.writeToFile(pdf, document -> PDFUtil.addParagraph(document, text));
        return pdf;
    }

    public static Path createMultiPagePdf(Path dir, String name, int pages) {
        Path pdf = dir.resolve(name);
        PDFUtil.writeToFile(pdf, document -> {
            for (int i = 1; i <= pages; i++) {
                PDFUtil.addParagraph(document, "page-" + i);
                if (i < pages) {
                    document.newPage();
                }
            }
        });
        return pdf;
    }

    public static Path createImage(Path dir, String name) throws IOException {
        Path image = dir.resolve(name);
        BufferedImage bufferedImage = new BufferedImage(80, 80, BufferedImage.TYPE_INT_RGB);
        Graphics2D graphics = bufferedImage.createGraphics();
        graphics.setColor(Color.WHITE);
        graphics.fillRect(0, 0, 80, 80);
        graphics.setColor(Color.BLACK);
        graphics.fillOval(10, 10, 60, 60);
        graphics.dispose();
        ImageIO.write(bufferedImage, "png", image.toFile());
        return image;
    }

    public static Path createHtml(Path dir, String name) throws IOException {
        Path html = dir.resolve(name);
        Files.writeString(html, "<html><body><h1>标题</h1><p>内容</p></body></html>");
        return html;
    }

    public static void assertPdfExists(Path pdf) throws IOException {
        org.junit.jupiter.api.Assertions.assertTrue(Files.exists(pdf));
        org.junit.jupiter.api.Assertions.assertTrue(Files.size(pdf) > 0);
        org.junit.jupiter.api.Assertions.assertTrue(PDFUtil.isPdf(pdf));
    }

    public static List<List<String>> rows() {
        return List.of(List.of("1", "张三"), List.of("2", "李四"));
    }
}
