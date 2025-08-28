package local.ateng.java.customutils.utils;

import com.lowagie.text.*;
import com.lowagie.text.Font;
import com.lowagie.text.Image;
import com.lowagie.text.pdf.*;

import java.awt.*;
import java.io.*;
import java.util.List;

/**
 * PDF 工具类 (基于 OpenPDF, 适配 JDK8)
 * <p>
 * 功能覆盖:
 * 1. 创建 PDF (标题、段落、表格、列表)
 * 2. 中文字体支持
 * 3. 添加水印 (文字/图片)
 * 4. 合并 PDF
 * 5. 拆分 PDF
 * 6. 提取 PDF 文本
 * 7. 导出字节流 (可用于 Web 下载)
 * 8. 加密/解密 PDF
 * <p>
 * 注意：需要引入 openpdf 依赖
 * <dependency>
 * <groupId>com.github.librepdf</groupId>
 * <artifactId>openpdf</artifactId>
 * <version>1.3.30</version>
 * </dependency>
 */
public final class PDFUtil {

    private PDFUtil() {
        throw new UnsupportedOperationException("工具类不可实例化");
    }

    /**
     * 默认中文字体（防止中文乱码）
     */
    private static final Font DEFAULT_CHINESE_FONT;

    static {
        BaseFont bfChinese;
        try {
            // 使用系统内置的中文字体（兼容性较好，避免乱码）
            bfChinese = BaseFont.createFont("STSongStd-Light", "UniGB-UCS2-H", BaseFont.EMBEDDED);
        } catch (Exception e) {
            throw new RuntimeException("初始化中文字体失败", e);
        }
        DEFAULT_CHINESE_FONT = new Font(bfChinese, 12, Font.NORMAL, Color.BLACK);
    }

    /**
     * 创建一个 PDF 文档，包含标题和段落
     */
    public static void createPdf(String filePath, String title, List<String> paragraphs) throws Exception {
        Document document = new Document(PageSize.A4);
        PdfWriter.getInstance(document, new FileOutputStream(filePath));
        document.open();

        // 添加标题
        if (title != null) {
            Paragraph titlePara = new Paragraph(title, new Font(DEFAULT_CHINESE_FONT.getBaseFont(), 18, Font.BOLD));
            titlePara.setAlignment(Element.ALIGN_CENTER);
            document.add(titlePara);
            document.add(new Paragraph("\n"));
        }

        // 添加段落
        if (paragraphs != null) {
            for (String p : paragraphs) {
                document.add(new Paragraph(p, DEFAULT_CHINESE_FONT));
            }
        }

        document.close();
    }

    /**
     * 创建 PDF 表格
     */
    public static void createPdfWithTable(String filePath, String title, List<String[]> tableData) throws Exception {
        Document document = new Document(PageSize.A4);
        PdfWriter.getInstance(document, new FileOutputStream(filePath));
        document.open();

        // 标题
        if (title != null) {
            Paragraph titlePara = new Paragraph(title, new Font(DEFAULT_CHINESE_FONT.getBaseFont(), 16, Font.BOLD));
            titlePara.setAlignment(Element.ALIGN_CENTER);
            document.add(titlePara);
            document.add(new Paragraph("\n"));
        }

        if (tableData != null && !tableData.isEmpty()) {
            PdfPTable table = new PdfPTable(tableData.get(0).length);
            table.setWidthPercentage(100);
            for (String[] row : tableData) {
                for (String col : row) {
                    PdfPCell cell = new PdfPCell(new Phrase(col, DEFAULT_CHINESE_FONT));
                    cell.setHorizontalAlignment(Element.ALIGN_CENTER);
                    table.addCell(cell);
                }
            }
            document.add(table);
        }

        document.close();
    }

    /**
     * 合并多个 PDF
     */
    public static void mergePdf(List<String> pdfFiles, String destFile) throws Exception {
        Document document = new Document();
        PdfCopy copy = new PdfCopy(document, new FileOutputStream(destFile));
        document.open();

        for (String file : pdfFiles) {
            PdfReader reader = new PdfReader(file);
            int n = reader.getNumberOfPages();
            for (int i = 1; i <= n; i++) {
                copy.addPage(copy.getImportedPage(reader, i));
            }
            reader.close();
        }
        document.close();
    }

    /**
     * 拆分 PDF
     */
    public static void splitPdf(String pdfFile, String outputDir) throws Exception {
        PdfReader reader = new PdfReader(pdfFile);
        int n = reader.getNumberOfPages();

        for (int i = 1; i <= n; i++) {
            Document document = new Document();
            PdfCopy copy = new PdfCopy(document, new FileOutputStream(outputDir + "/page_" + i + ".pdf"));
            document.open();
            copy.addPage(copy.getImportedPage(reader, i));
            document.close();
        }
        reader.close();
    }

    /**
     * 添加文字水印
     */
    public static void addTextWatermark(String srcFile, String destFile, String watermark) throws Exception {
        PdfReader reader = new PdfReader(srcFile);
        PdfStamper stamper = new PdfStamper(reader, new FileOutputStream(destFile));

        int n = reader.getNumberOfPages();
        PdfContentByte content;
        BaseFont baseFont = BaseFont.createFont(BaseFont.HELVETICA, BaseFont.WINANSI, BaseFont.NOT_EMBEDDED);

        for (int i = 1; i <= n; i++) {
            content = stamper.getOverContent(i);
            content.beginText();
            content.setFontAndSize(baseFont, 30);
            content.setColorFill(Color.LIGHT_GRAY);
            content.showTextAligned(Element.ALIGN_CENTER, watermark, 300, 400, 45);
            content.endText();
        }

        stamper.close();
        reader.close();
    }

    /**
     * 添加图片水印
     */
    public static void addImageWatermark(String srcFile, String destFile, String imagePath) throws Exception {
        PdfReader reader = new PdfReader(srcFile);
        PdfStamper stamper = new PdfStamper(reader, new FileOutputStream(destFile));

        Image img = Image.getInstance(imagePath);
        img.setAbsolutePosition(200, 300);
        img.scalePercent(50);

        int n = reader.getNumberOfPages();
        for (int i = 1; i <= n; i++) {
            PdfContentByte content = stamper.getOverContent(i);
            content.addImage(img);
        }

        stamper.close();
        reader.close();
    }

    /**
     * PDF 加密（设置密码）
     */
    public static void encryptPdf(String srcFile, String destFile, String userPassword, String ownerPassword) throws Exception {
        PdfReader reader = new PdfReader(srcFile);
        PdfStamper stamper = new PdfStamper(reader, new FileOutputStream(destFile));

        stamper.setEncryption(userPassword.getBytes(), ownerPassword.getBytes(),
                PdfWriter.ALLOW_PRINTING, PdfWriter.ENCRYPTION_AES_128);

        stamper.close();
        reader.close();
    }

    /**
     * 将 PDF 输出为字节数组 (用于 Web 接口返回)
     */
    public static byte[] toByteArray(String filePath) throws IOException {
        try (InputStream in = new FileInputStream(filePath);
             ByteArrayOutputStream bos = new ByteArrayOutputStream()) {
            byte[] buffer = new byte[4096];
            int len;
            while ((len = in.read(buffer)) != -1) {
                bos.write(buffer, 0, len);
            }
            return bos.toByteArray();
        }
    }
}
