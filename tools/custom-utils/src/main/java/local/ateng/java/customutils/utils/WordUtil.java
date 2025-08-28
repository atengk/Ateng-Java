package local.ateng.java.customutils.utils;

import org.apache.poi.util.Units;
import org.apache.poi.xwpf.model.XWPFHeaderFooterPolicy;
import org.apache.poi.xwpf.usermodel.*;

import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Map;

/**
 * Word 文档导出工具类
 * 基于 Apache POI 的 XWPFDocument 实现
 * 支持常见的标题、段落、表格、图片、分页、页眉页脚操作
 * <p>
 * 使用步骤：
 * 1. 创建文档 {@link #createDocument()}
 * 2. 添加内容（标题、段落、表格等）
 * 3. 保存文档 {@link #writeDocument(XWPFDocument, String)}
 *
 * @author Ateng
 * @since 2025-08-27
 */
public final class WordUtil {

    private WordUtil() {
        throw new UnsupportedOperationException("工具类不可实例化");
    }

    /**
     * 创建一个新的 Word 文档
     *
     * @return XWPFDocument 文档对象
     */
    public static XWPFDocument createDocument() {
        return new XWPFDocument();
    }

    /**
     * 添加标题
     *
     * @param document 文档对象
     * @param text     标题内容
     * @param fontSize 字体大小
     */
    public static void addTitle(XWPFDocument document, String text, int fontSize) {
        XWPFParagraph title = document.createParagraph();
        title.setAlignment(ParagraphAlignment.CENTER);
        XWPFRun run = title.createRun();
        run.setText(text);
        run.setBold(true);
        run.setFontSize(fontSize);
    }

    /**
     * 添加段落
     *
     * @param document   文档对象
     * @param text       段落内容
     * @param fontFamily 字体
     * @param fontSize   字体大小
     * @param align      对齐方式
     */
    public static void addParagraph(XWPFDocument document, String text, String fontFamily, int fontSize, ParagraphAlignment align) {
        XWPFParagraph paragraph = document.createParagraph();
        paragraph.setAlignment(align);
        XWPFRun run = paragraph.createRun();
        run.setText(text);
        run.setFontFamily(fontFamily);
        run.setFontSize(fontSize);
    }

    /**
     * 添加段落（全面版）
     * 支持字体、大小、颜色、加粗、斜体、下划线、删除线、对齐、缩进、行距、背景色等属性
     *
     * @param document        文档对象
     * @param text            段落内容
     * @param fontFamily      字体名称
     * @param fontSize        字体大小
     * @param bold            是否加粗
     * @param italic          是否斜体
     * @param underline       下划线样式（可用 UnderlinePatterns 枚举）
     * @param strike          是否添加删除线
     * @param color           字体颜色（RGB 十六进制，如 "000000"）
     * @param highlight       高亮颜色（可用 HighlightColor 枚举）
     * @param align           对齐方式（ParagraphAlignment 枚举）
     * @param indentation     首行缩进字符数（0 表示无缩进）
     * @param spacingBefore   段前间距（磅值）
     * @param spacingAfter    段后间距（磅值）
     * @param lineSpacing     行距（1.0 表示单倍，2.0 表示两倍）
     * @param backgroundColor 背景色（RGB 十六进制，如 "DDDDDD"，null 表示无）
     */
    public static void addParagraphFull(XWPFDocument document,
                                        String text,
                                        String fontFamily,
                                        int fontSize,
                                        boolean bold,
                                        boolean italic,
                                        UnderlinePatterns underline,
                                        boolean strike,
                                        String color,
                                        HighlightColor highlight,
                                        ParagraphAlignment align,
                                        int indentation,
                                        int spacingBefore,
                                        int spacingAfter,
                                        double lineSpacing,
                                        String backgroundColor) {
        XWPFParagraph paragraph = document.createParagraph();

        // 段落对齐
        paragraph.setAlignment(align);

        // 首行缩进（乘以 200 表示一个 Tab 的宽度，POI 默认单位是 TWIPS = 1/20 point）
        if (indentation > 0) {
            paragraph.setFirstLineIndent(indentation * 200);
        }

        // 段前/段后间距
        paragraph.setSpacingBefore(spacingBefore);
        paragraph.setSpacingAfter(spacingAfter);

        // 行距（POI 默认是 240 = 单倍）
        paragraph.setSpacingBetween(lineSpacing);

        // 创建 Run（段落文字样式）
        XWPFRun run = paragraph.createRun();
        run.setText(text);
        run.setFontFamily(fontFamily);
        run.setFontSize(fontSize);
        run.setBold(bold);
        run.setItalic(italic);
        run.setStrikeThrough(strike);

        if (underline != null) {
            run.setUnderline(underline);
        }
        if (color != null) {
            run.setColor(color);
        }
        if (highlight != null) {
            run.setTextHighlightColor(highlight.name());
        }

        // 背景色（通过段落 shading 设置）
        if (backgroundColor != null) {
            paragraph.getCTP().addNewPPr().addNewShd().setFill(backgroundColor);
        }
    }

    /**
     * 高亮颜色枚举
     * 对应 Word 内置的 highlight 颜色
     */
    public enum HighlightColor {
        YELLOW, GREEN, CYAN, MAGENTA, BLUE, RED, DARK_BLUE, DARK_CYAN,
        DARK_GREEN, DARK_MAGENTA, DARK_RED, DARK_YELLOW, BLACK, GRAY,
        LIGHT_GRAY, WHITE
    }

    /**
     * 添加表格
     *
     * @param document 文档对象
     * @param data     二维数组，表示表格数据
     */
    public static void addTable(XWPFDocument document, String[][] data) {
        if (data == null || data.length == 0) {
            return;
        }
        int rowCount = data.length;
        int colCount = data[0].length;
        XWPFTable table = document.createTable(rowCount, colCount);
        table.setWidth("100%");
        for (int i = 0; i < rowCount; i++) {
            for (int j = 0; j < colCount; j++) {
                table.getRow(i).getCell(j).setText(data[i][j]);
            }
        }
    }

    /**
     * 插入图片
     *
     * @param document    文档对象
     * @param imageStream 图片输入流
     * @param format      图片格式（XWPFDocument.PICTURE_TYPE_JPEG 等）
     * @param width       图片宽度（px）
     * @param height      图片高度（px）
     * @throws IOException 图片流异常
     */
    public static void addPicture(XWPFDocument document, InputStream imageStream, int format, int width, int height) throws IOException {
        XWPFParagraph paragraph = document.createParagraph();
        XWPFRun run = paragraph.createRun();
        try {
            run.addPicture(imageStream, format, "picture", Units.toEMU(width), Units.toEMU(height));
        } catch (Exception e) {
            throw new IOException("添加图片失败", e);
        }
    }

    /**
     * 添加分页符
     *
     * @param document 文档对象
     */
    public static void addPageBreak(XWPFDocument document) {
        XWPFParagraph paragraph = document.createParagraph();
        XWPFRun run = paragraph.createRun();
        run.addBreak(BreakType.PAGE);
    }

    /**
     * 添加页眉
     *
     * @param document 文档对象
     * @param text     页眉内容
     */
    public static void addHeader(XWPFDocument document, String text) {
        XWPFHeaderFooterPolicy policy = document.createHeaderFooterPolicy();
        XWPFHeader header = policy.createHeader(XWPFHeaderFooterPolicy.DEFAULT);
        XWPFParagraph paragraph = header.createParagraph();
        paragraph.setAlignment(ParagraphAlignment.CENTER);
        XWPFRun run = paragraph.createRun();
        run.setText(text);
    }

    /**
     * 添加页脚
     *
     * @param document 文档对象
     * @param text     页脚内容
     */
    public static void addFooter(XWPFDocument document, String text) {
        XWPFHeaderFooterPolicy policy = document.createHeaderFooterPolicy();
        XWPFFooter footer = policy.createFooter(XWPFHeaderFooterPolicy.DEFAULT);
        XWPFParagraph paragraph = footer.createParagraph();
        paragraph.setAlignment(ParagraphAlignment.CENTER);
        XWPFRun run = paragraph.createRun();
        run.setText(text);
    }

    /**
     * 模板文本替换
     * 将 Word 中的占位符 {{key}} 替换为实际的值
     *
     * @param document 文档对象
     * @param params   替换参数，key 为占位符名称（不带 {{}}），value 为替换值
     */
    public static void replaceTemplate(XWPFDocument document, Map<String, String> params) {
        if (params == null || params.isEmpty()) {
            return;
        }
        for (XWPFParagraph paragraph : document.getParagraphs()) {
            for (XWPFRun run : paragraph.getRuns()) {
                String text = run.getText(0);
                if (text != null) {
                    for (Map.Entry<String, String> entry : params.entrySet()) {
                        String key = "{{" + entry.getKey() + "}}";
                        if (text.contains(key)) {
                            text = text.replace(key, entry.getValue());
                        }
                    }
                    run.setText(text, 0);
                }
            }
        }
        for (XWPFTable table : document.getTables()) {
            for (XWPFTableRow row : table.getRows()) {
                for (XWPFTableCell cell : row.getTableCells()) {
                    for (XWPFParagraph paragraph : cell.getParagraphs()) {
                        for (XWPFRun run : paragraph.getRuns()) {
                            String text = run.getText(0);
                            if (text != null) {
                                for (Map.Entry<String, String> entry : params.entrySet()) {
                                    String key = "{{" + entry.getKey() + "}}";
                                    if (text.contains(key)) {
                                        text = text.replace(key, entry.getValue());
                                    }
                                }
                                run.setText(text, 0);
                            }
                        }
                    }
                }
            }
        }
    }

    /**
     * 在指定表格后动态追加行
     *
     * @param table Word 表格对象
     * @param data  二维数组，表示多行多列数据
     */
    public static void appendTableRows(XWPFTable table, String[][] data) {
        if (data == null || data.length == 0) {
            return;
        }
        for (String[] rowData : data) {
            XWPFTableRow row = table.createRow();
            for (int i = 0; i < rowData.length; i++) {
                row.getCell(i).setText(rowData[i]);
            }
        }
    }

    /**
     * 添加超链接
     *
     * @param document 文档对象
     * @param url      链接地址
     * @param text     显示文字
     */
    public static void addHyperlink(XWPFDocument document, String url, String text) {
        XWPFParagraph paragraph = document.createParagraph();
        XWPFHyperlinkRun hyperlink = paragraph.createHyperlinkRun(url);
        hyperlink.setText(text);
        hyperlink.setColor("0000FF");
        hyperlink.setUnderline(UnderlinePatterns.SINGLE);
    }

    /**
     * 添加文字水印
     *
     * @param document 文档对象
     * @param text     水印文字
     */
    public static void addWatermark(XWPFDocument document, String text) {
        XWPFHeaderFooterPolicy policy = document.createHeaderFooterPolicy();
        policy.createWatermark(text);
    }

    /**
     * 将文档写入文件
     *
     * @param document 文档对象
     * @param filePath 保存路径
     * @throws IOException 文件写入异常
     */
    public static void writeDocument(XWPFDocument document, String filePath) throws IOException {
        try (OutputStream out = new FileOutputStream(filePath)) {
            document.write(out);
        }
    }
}
