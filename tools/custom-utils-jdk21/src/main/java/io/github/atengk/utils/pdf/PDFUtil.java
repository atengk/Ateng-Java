package io.github.atengk.utils.pdf;

import org.openpdf.text.Anchor;
import org.openpdf.text.Chunk;
import org.openpdf.text.Document;
import org.openpdf.text.DocumentException;
import org.openpdf.text.Element;
import org.openpdf.text.Font;
import org.openpdf.text.FontFactory;
import org.openpdf.text.Image;
import org.openpdf.text.PageSize;
import org.openpdf.text.Paragraph;
import org.openpdf.text.Phrase;
import org.openpdf.text.Rectangle;
import org.openpdf.text.pdf.AcroFields;
import org.openpdf.text.pdf.BaseFont;
import org.openpdf.text.pdf.ColumnText;
import org.openpdf.text.pdf.PdfContentByte;
import org.openpdf.text.pdf.PdfCopy;
import org.openpdf.text.pdf.PdfImportedPage;
import org.openpdf.text.pdf.PdfName;
import org.openpdf.text.pdf.PdfNumber;
import org.openpdf.text.pdf.PdfPCell;
import org.openpdf.text.pdf.PdfPTable;
import org.openpdf.text.pdf.PdfReader;
import org.openpdf.text.pdf.PdfStamper;
import org.openpdf.text.pdf.PdfWriter;
import org.openpdf.text.pdf.parser.PdfTextExtractor;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.Closeable;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.UncheckedIOException;
import java.lang.reflect.Field;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.Function;
import java.util.regex.Pattern;

/**
 * PDF 通用工具类，基于 OpenPDF 封装 PDF 创建、编辑、读取、加密、表单和常用业务能力。
 *
 * @author Ateng
 * @since 2026-04-30
 */
public final class PDFUtil {

    private static final byte[] PDF_HEADER = "%PDF".getBytes(StandardCharsets.US_ASCII);
    private static final SecureRandom SECURE_RANDOM = new SecureRandom();
    private static final Pattern HTML_TAG_PATTERN = Pattern.compile("<[^>]+>");
    private static final Pattern HTML_BREAK_PATTERN = Pattern.compile("(?i)<br\s*/?>|</p>|</div>|</h[1-6]>|</li>");

    private PDFUtil() {
        throw new UnsupportedOperationException("PDFUtil 是静态工具类，不允许实例化");
    }

    /**
     * PDF 内容构建回调。
     *
     * @author Ateng
     * @since 2026-04-30
     */
    @FunctionalInterface
    public interface PDFBuilder {
        /**
         * 构建 PDF 内容。
         *
         * @param document PDF 文档对象
         * @throws Exception 构建异常
         */
        void build(Document document) throws Exception;
    }

    /**
     * PDF 页面配置。
     *
     * @param pageSize 页面尺寸
     * @param marginLeft 左边距
     * @param marginRight 右边距
     * @param marginTop 上边距
     * @param marginBottom 下边距
     * @author Ateng
     * @since 2026-04-30
     */
    public record PageOptions(Rectangle pageSize, float marginLeft, float marginRight, float marginTop, float marginBottom) {
        /**
         * 创建页面配置。
         */
        public PageOptions {
            pageSize = pageSize == null ? PageSize.A4 : pageSize;
            requireNonNegative(marginLeft, "左边距不能为负数");
            requireNonNegative(marginRight, "右边距不能为负数");
            requireNonNegative(marginTop, "上边距不能为负数");
            requireNonNegative(marginBottom, "下边距不能为负数");
        }
    }

    /**
     * 字体配置。
     *
     * @param fontPath 字体文件路径，可以为空
     * @param size 字体大小
     * @param style 字体样式
     * @param color 字体颜色
     * @author Ateng
     * @since 2026-04-30
     */
    public record FontOptions(String fontPath, float size, int style, Color color) {
        /**
         * 创建字体配置。
         */
        public FontOptions {
            requirePositive(size, "字体大小必须大于 0");
            color = color == null ? Color.BLACK : color;
        }
    }

    /**
     * 文本样式配置。
     *
     * @param font 字体
     * @param alignment 对齐方式
     * @param spacingBefore 前间距
     * @param spacingAfter 后间距
     * @param firstLineIndent 首行缩进
     * @author Ateng
     * @since 2026-04-30
     */
    public record TextOptions(Font font, int alignment, float spacingBefore, float spacingAfter, float firstLineIndent) {
        /**
         * 创建文本样式配置。
         */
        public TextOptions {
            font = font == null ? createDefaultFont() : font;
            requireNonNegative(spacingBefore, "前间距不能为负数");
            requireNonNegative(spacingAfter, "后间距不能为负数");
            requireNonNegative(firstLineIndent, "首行缩进不能为负数");
        }
    }

    /**
     * 表格配置。
     *
     * @param widthPercentage 表格宽度百分比
     * @param columnWidths 列宽数组，可以为空
     * @param headerFont 表头字体
     * @param bodyFont 内容字体
     * @param cellPadding 单元格内边距
     * @author Ateng
     * @since 2026-04-30
     */
    public record TableOptions(float widthPercentage, float[] columnWidths, Font headerFont, Font bodyFont, float cellPadding) {
        /**
         * 创建表格配置。
         */
        public TableOptions {
            if (widthPercentage <= 0 || widthPercentage > 100) {
                throw new IllegalArgumentException("表格宽度百分比必须在 0 到 100 之间");
            }
            if (columnWidths != null) {
                for (float columnWidth : columnWidths) {
                    requirePositive(columnWidth, "列宽必须大于 0");
                }
                columnWidths = columnWidths.clone();
            }
            headerFont = headerFont == null ? createTableHeaderFont() : headerFont;
            bodyFont = bodyFont == null ? createTableBodyFont() : bodyFont;
            requireNonNegative(cellPadding, "单元格内边距不能为负数");
        }
    }

    /**
     * 图片配置。
     *
     * @param width 图片宽度，0 表示不指定
     * @param height 图片高度，0 表示不指定
     * @param x 绝对定位 X 坐标，可以为空
     * @param y 绝对定位 Y 坐标，可以为空
     * @param alignment 对齐方式
     * @author Ateng
     * @since 2026-04-30
     */
    public record ImageOptions(float width, float height, Float x, Float y, int alignment) {
        /**
         * 创建图片配置。
         */
        public ImageOptions {
            requireNonNegative(width, "图片宽度不能为负数");
            requireNonNegative(height, "图片高度不能为负数");
        }
    }

    /**
     * 水印配置。
     *
     * @param text 水印文本，可以为空
     * @param imagePath 水印图片路径，可以为空
     * @param font 水印字体
     * @param opacity 透明度，范围 0 到 1
     * @param rotation 旋转角度
     * @param x X 坐标，可以为空
     * @param y Y 坐标，可以为空
     * @param tiled 是否平铺
     * @author Ateng
     * @since 2026-04-30
     */
    public record WatermarkOptions(String text, Path imagePath, Font font, float opacity, float rotation, Float x, Float y, boolean tiled) {
        /**
         * 创建水印配置。
         */
        public WatermarkOptions {
            if (isBlank(text) && imagePath == null) {
                throw new IllegalArgumentException("水印文本和图片路径不能同时为空");
            }
            if (opacity < 0 || opacity > 1) {
                throw new IllegalArgumentException("透明度必须在 0 到 1 之间");
            }
            font = font == null ? new Font(Font.HELVETICA, 36, Font.BOLD, Color.LIGHT_GRAY) : font;
        }
    }

    /**
     * 页眉配置。
     *
     * @param text 页眉文本
     * @param font 字体
     * @param y Y 坐标
     * @author Ateng
     * @since 2026-04-30
     */
    public record HeaderOptions(String text, Font font, float y) {
        /**
         * 创建页眉配置。
         */
        public HeaderOptions {
            requireNotBlank(text, "页眉文本不能为空");
            font = font == null ? createDefaultFont(10) : font;
            requirePositive(y, "页眉 Y 坐标必须大于 0");
        }
    }

    /**
     * 页脚配置。
     *
     * @param text 页脚文本
     * @param font 字体
     * @param y Y 坐标
     * @author Ateng
     * @since 2026-04-30
     */
    public record FooterOptions(String text, Font font, float y) {
        /**
         * 创建页脚配置。
         */
        public FooterOptions {
            requireNotBlank(text, "页脚文本不能为空");
            font = font == null ? createDefaultFont(10) : font;
            requirePositive(y, "页脚 Y 坐标必须大于 0");
        }
    }

    /**
     * 页码配置。
     *
     * @param pattern 页码格式，支持 {page} 和 {total}
     * @param font 字体
     * @param y Y 坐标
     * @author Ateng
     * @since 2026-04-30
     */
    public record PageNumberOptions(String pattern, Font font, float y) {
        /**
         * 创建页码配置。
         */
        public PageNumberOptions {
            pattern = isBlank(pattern) ? "第 {page} / {total} 页" : pattern;
            font = font == null ? createDefaultFont(10) : font;
            requirePositive(y, "页码 Y 坐标必须大于 0");
        }
    }

    /**
     * 元数据配置。
     *
     * @param title 标题
     * @param author 作者
     * @param subject 主题
     * @param keywords 关键字
     * @param creator 创建者
     * @author Ateng
     * @since 2026-04-30
     */
    public record MetadataOptions(String title, String author, String subject, String keywords, String creator) {
        /**
         * 创建元数据配置。
         */
        public MetadataOptions {
        }
    }

    /**
     * 加密配置。
     *
     * @param userPassword 用户密码
     * @param ownerPassword 所有者密码
     * @param permissions 权限位
     * @param encryptionType 加密类型
     * @author Ateng
     * @since 2026-04-30
     */
    public record EncryptOptions(String userPassword, String ownerPassword, int permissions, int encryptionType) {
        /**
         * 创建加密配置。
         */
        public EncryptOptions {
            requireNotBlank(userPassword, "用户密码不能为空");
            if (isBlank(ownerPassword)) {
                ownerPassword = randomPassword();
            }
        }
    }

    /**
     * 权限保护配置。
     *
     * @param userPassword 用户密码
     * @param ownerPassword 所有者密码
     * @param allowPrint 是否允许打印
     * @param allowCopy 是否允许复制
     * @param allowModify 是否允许修改
     * @author Ateng
     * @since 2026-04-30
     */
    public record ProtectOptions(String userPassword, String ownerPassword, boolean allowPrint, boolean allowCopy, boolean allowModify) {
        /**
         * 创建权限保护配置。
         */
        public ProtectOptions {
            requireNotBlank(userPassword, "用户密码不能为空");
            if (isBlank(ownerPassword)) {
                ownerPassword = randomPassword();
            }
        }
    }

    /**
     * HTML 转 PDF 配置。
     *
     * @param pageOptions 页面配置
     * @param metadataOptions 元数据配置
     * @param font 字体
     * @author Ateng
     * @since 2026-04-30
     */
    public record HtmlPdfOptions(PageOptions pageOptions, MetadataOptions metadataOptions, Font font) {
        /**
         * 创建 HTML 转 PDF 配置。
         */
        public HtmlPdfOptions {
            pageOptions = pageOptions == null ? createA4PageOptions() : pageOptions;
            font = font == null ? createDefaultFont() : font;
        }
    }

    /**
     * 印章配置。
     *
     * @param imagePath 印章图片路径
     * @param pageNo 页码
     * @param x X 坐标
     * @param y Y 坐标
     * @param width 宽度
     * @param height 高度
     * @author Ateng
     * @since 2026-04-30
     */
    public record SealOptions(Path imagePath, int pageNo, float x, float y, float width, float height) {
        /**
         * 创建印章配置。
         */
        public SealOptions {
            requireReadableFile(imagePath, "印章图片不存在或不可读");
            requirePositive(pageNo, "页码必须大于 0");
            requireNonNegative(x, "X 坐标不能为负数");
            requireNonNegative(y, "Y 坐标不能为负数");
            requirePositive(width, "印章宽度必须大于 0");
            requirePositive(height, "印章高度必须大于 0");
        }
    }

    /**
     * 数字签名配置占位对象。
     *
     * @param reason 签名原因
     * @param location 签名地点
     * @author Ateng
     * @since 2026-04-30
     */
    public record SignOptions(String reason, String location) {
        /**
         * 创建签名配置。
         */
        public SignOptions {
        }
    }

    /**
     * 压缩配置。
     *
     * @param fullCompression 是否开启 PDF 全压缩
     * @param removeMetadata 是否移除元数据
     * @author Ateng
     * @since 2026-04-30
     */
    public record CompressOptions(boolean fullCompression, boolean removeMetadata) {
        /**
         * 创建压缩配置。
         */
        public CompressOptions {
        }
    }

    /**
     * 渲染配置占位对象。
     *
     * @param dpi 分辨率
     * @param imageFormat 图片格式
     * @author Ateng
     * @since 2026-04-30
     */
    public record RenderOptions(int dpi, String imageFormat) {
        /**
         * 创建渲染配置。
         */
        public RenderOptions {
            requirePositive(dpi, "DPI 必须大于 0");
            imageFormat = isBlank(imageFormat) ? "png" : imageFormat;
        }
    }

    /**
     * 表格列定义。
     *
     * @param title 列标题
     * @param valueGetter 值提取器
     * @param <T> 行数据类型
     * @author Ateng
     * @since 2026-04-30
     */
    public record TableColumn<T>(String title, Function<T, String> valueGetter) {
        /**
         * 创建表格列定义。
         */
        public TableColumn {
            requireNotBlank(title, "列标题不能为空");
            Objects.requireNonNull(valueGetter, "值提取器不能为空");
        }
    }

    /**
     * PDF 工具类运行时异常。
     *
     * @author Ateng
     * @since 2026-04-30
     */
    public static class PDFException extends RuntimeException {
        /**
         * 创建 PDF 异常。
         *
         * @param message 异常消息
         */
        public PDFException(String message) {
            super(message);
        }

        /**
         * 创建 PDF 异常。
         *
         * @param message 异常消息
         * @param cause 原始异常
         */
        public PDFException(String message, Throwable cause) {
            super(message, cause);
        }
    }

    /**
     * 创建默认 PDF 文档对象。
     *
     * @return PDF 文档对象
     */
    public static Document createDocument() {
        return createDocument(createA4PageOptions());
    }

    /**
     * 按页面配置创建 PDF 文档对象。
     *
     * @param options 页面配置
     * @return PDF 文档对象
     */
    public static Document createDocument(PageOptions options) {
        PageOptions actual = options == null ? createA4PageOptions() : options;
        return new Document(actual.pageSize(), actual.marginLeft(), actual.marginRight(), actual.marginTop(), actual.marginBottom());
    }

    /**
     * 创建 PDF 写出器。
     *
     * @param document PDF 文档对象
     * @param out 输出流
     * @return PDF 写出器
     */
    public static PdfWriter createWriter(Document document, OutputStream out) {
        Objects.requireNonNull(document, "PDF 文档对象不能为空");
        Objects.requireNonNull(out, "输出流不能为空");
        try {
            return PdfWriter.getInstance(document, out);
        } catch (DocumentException e) {
            throw wrapPdfException("创建 PDF 写出器失败", e);
        }
    }

    /**
     * 使用默认页面配置写出 PDF。
     *
     * @param out 输出流
     * @param builder 内容构建器
     */
    public static void write(OutputStream out, PDFBuilder builder) {
        writeToStream(out, createA4PageOptions(), null, builder);
    }

    /**
     * 写出 PDF 到文件。
     *
     * @param outputPath 输出文件路径
     * @param builder 内容构建器
     */
    public static void writeToFile(Path outputPath, PDFBuilder builder) {
        writeToFile(outputPath, createA4PageOptions(), null, builder);
    }

    /**
     * 按页面和元数据配置写出 PDF 到文件。
     *
     * @param outputPath 输出文件路径
     * @param pageOptions 页面配置
     * @param metadataOptions 元数据配置
     * @param builder 内容构建器
     */
    public static void writeToFile(Path outputPath, PageOptions pageOptions, MetadataOptions metadataOptions, PDFBuilder builder) {
        requireOutputPath(outputPath);
        try (OutputStream out = Files.newOutputStream(outputPath)) {
            writeToStream(out, pageOptions, metadataOptions, builder);
        } catch (IOException e) {
            throw wrapPdfException("写出 PDF 文件失败", e);
        }
    }

    /**
     * 生成 PDF 字节数组。
     *
     * @param builder 内容构建器
     * @return PDF 字节数组
     */
    public static byte[] writeToBytes(PDFBuilder builder) {
        return writeToBytes(createA4PageOptions(), null, builder);
    }

    /**
     * 按页面和元数据配置生成 PDF 字节数组。
     *
     * @param pageOptions 页面配置
     * @param metadataOptions 元数据配置
     * @param builder 内容构建器
     * @return PDF 字节数组
     */
    public static byte[] writeToBytes(PageOptions pageOptions, MetadataOptions metadataOptions, PDFBuilder builder) {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        writeToStream(out, pageOptions, metadataOptions, builder);
        return out.toByteArray();
    }

    /**
     * 写出 PDF 到输出流。
     *
     * @param out 输出流
     * @param builder 内容构建器
     */
    public static void writeToStream(OutputStream out, PDFBuilder builder) {
        writeToStream(out, createA4PageOptions(), null, builder);
    }

    /**
     * 按页面和元数据配置写出 PDF 到输出流。
     *
     * @param out 输出流
     * @param pageOptions 页面配置
     * @param metadataOptions 元数据配置
     * @param builder 内容构建器
     */
    public static void writeToStream(OutputStream out, PageOptions pageOptions, MetadataOptions metadataOptions, PDFBuilder builder) {
        Objects.requireNonNull(out, "输出流不能为空");
        Objects.requireNonNull(builder, "PDF 内容构建器不能为空");
        Document document = createDocument(pageOptions);
        try {
            createWriter(document, out);
            applyMetadata(document, metadataOptions);
            document.open();
            builder.build(document);
        } catch (Exception e) {
            throw wrapPdfException("生成 PDF 失败", e);
        } finally {
            if (document.isOpen()) {
                document.close();
            }
        }
    }

    /**
     * 创建空白 PDF 文件。
     *
     * @param outputPath 输出文件路径
     */
    public static void createBlankPdf(Path outputPath) {
        createBlankPdf(outputPath, 1);
    }

    /**
     * 创建指定页数的空白 PDF 文件。
     *
     * @param outputPath 输出文件路径
     * @param pageCount 页数
     */
    public static void createBlankPdf(Path outputPath, int pageCount) {
        requirePositive(pageCount, "页数必须大于 0");
        writeToFile(outputPath, document -> {
            for (int i = 0; i < pageCount; i++) {
                document.add(new Paragraph(" "));
                if (i < pageCount - 1) {
                    document.newPage();
                }
            }
        });
    }

    /**
     * 判断字节数组是否为 PDF。
     *
     * @param bytes 文件字节
     * @return 是否为 PDF
     */
    public static boolean isPdf(byte[] bytes) {
        if (bytes == null || bytes.length < PDF_HEADER.length) {
            return false;
        }
        for (int i = 0; i < PDF_HEADER.length; i++) {
            if (bytes[i] != PDF_HEADER[i]) {
                return false;
            }
        }
        return true;
    }

    /**
     * 判断路径是否为 PDF 文件。
     *
     * @param path 文件路径
     * @return 是否为 PDF
     */
    public static boolean isPdf(Path path) {
        if (path == null || !Files.isRegularFile(path) || !Files.isReadable(path)) {
            return false;
        }
        try (InputStream in = Files.newInputStream(path)) {
            byte[] header = in.readNBytes(PDF_HEADER.length);
            return isPdf(header);
        } catch (IOException e) {
            return false;
        }
    }

    /**
     * 创建默认页面配置。
     *
     * @return 页面配置
     */
    public static PageOptions createPageOptions() {
        return createA4PageOptions();
    }

    /**
     * 创建 A4 页面配置。
     *
     * @return A4 页面配置
     */
    public static PageOptions createA4PageOptions() {
        return new PageOptions(PageSize.A4, 36, 36, 36, 36);
    }

    /**
     * 创建横向 A4 页面配置。
     *
     * @return 横向页面配置
     */
    public static PageOptions createLandscapePageOptions() {
        return new PageOptions(PageSize.A4.rotate(), 36, 36, 36, 36);
    }

    /**
     * 设置文档页面尺寸。
     *
     * @param document PDF 文档对象
     * @param pageSize 页面尺寸
     */
    public static void setPageSize(Document document, Rectangle pageSize) {
        Objects.requireNonNull(document, "PDF 文档对象不能为空");
        Objects.requireNonNull(pageSize, "页面尺寸不能为空");
        document.setPageSize(pageSize);
    }

    /**
     * 设置文档页边距。
     *
     * @param document PDF 文档对象
     * @param left 左边距
     * @param right 右边距
     * @param top 上边距
     * @param bottom 下边距
     */
    public static void setMargins(Document document, float left, float right, float top, float bottom) {
        Objects.requireNonNull(document, "PDF 文档对象不能为空");
        requireNonNegative(left, "左边距不能为负数");
        requireNonNegative(right, "右边距不能为负数");
        requireNonNegative(top, "上边距不能为负数");
        requireNonNegative(bottom, "下边距不能为负数");
        document.setMargins(left, right, top, bottom);
    }

    /**
     * 新增页面。
     *
     * @param document PDF 文档对象
     */
    public static void newPage(Document document) {
        Objects.requireNonNull(document, "PDF 文档对象不能为空");
        document.newPage();
    }

    /**
     * 添加分页。
     *
     * @param document PDF 文档对象
     */
    public static void addPageBreak(Document document) {
        newPage(document);
    }

    /**
     * 获取 PDF 页数。
     *
     * @param pdfPath PDF 文件路径
     * @return 页数
     */
    public static int getPageCount(Path pdfPath) {
        return readPageCount(pdfPath);
    }

    /**
     * 从输入流读取 PDF 页数。
     *
     * @param in 输入流
     * @return 页数
     */
    public static int getPageCount(InputStream in) {
        Objects.requireNonNull(in, "输入流不能为空");
        try {
            return new PdfReader(in).getNumberOfPages();
        } catch (IOException e) {
            throw wrapPdfException("读取 PDF 页数失败", e);
        }
    }

    /**
     * 复制指定页码范围到新 PDF。
     *
     * @param source 源 PDF
     * @param target 目标 PDF
     * @param fromPage 起始页码，从 1 开始
     * @param toPage 结束页码，从 1 开始
     */
    public static void copyPages(Path source, Path target, int fromPage, int toPage) {
        splitByRange(source, target, fromPage, toPage);
    }

    /**
     * 删除指定页面并生成新 PDF。
     *
     * @param source 源 PDF
     * @param target 目标 PDF
     * @param pages 待删除页码集合
     */
    public static void removePages(Path source, Path target, Collection<Integer> pages) {
        requireReadablePdf(source);
        requireOutputPath(target);
        Objects.requireNonNull(pages, "待删除页码集合不能为空");
        try (PdfReader reader = new PdfReader(source.toString()); OutputStream out = Files.newOutputStream(target)) {
            Set<Integer> removeSet = new HashSet<>(pages);
            Document document = new Document(reader.getPageSizeWithRotation(1));
            PdfCopy copy = new PdfCopy(document, out);
            document.open();
            for (int i = 1; i <= reader.getNumberOfPages(); i++) {
                if (!removeSet.contains(i)) {
                    copy.addPage(copy.getImportedPage(reader, i));
                }
            }
            document.close();
        } catch (Exception e) {
            throw wrapPdfException("删除 PDF 页面失败", e);
        }
    }

    /**
     * 旋转指定页面。
     *
     * @param source 源 PDF
     * @param target 目标 PDF
     * @param pageNo 页码，从 1 开始
     * @param degree 旋转角度，通常为 90、180、270
     */
    public static void rotatePage(Path source, Path target, int pageNo, int degree) {
        requireReadablePdf(source);
        requireOutputPath(target);
        requirePositive(pageNo, "页码必须大于 0");
        if (degree % 90 != 0) {
            throw new IllegalArgumentException("旋转角度必须是 90 的倍数");
        }
        try (PdfReader reader = new PdfReader(source.toString()); OutputStream out = Files.newOutputStream(target)) {
            validatePageNo(reader, pageNo);
            int rotation = (reader.getPageRotation(pageNo) + degree) % 360;
            reader.getPageN(pageNo).put(PdfName.ROTATE, new PdfNumber(rotation));
            try (PdfStamper stamper = new PdfStamper(reader, out)) {
                // 通过 PdfStamper 写回页面字典修改。
            }
        } catch (Exception e) {
            throw wrapPdfException("旋转 PDF 页面失败", e);
        }
    }

    /**
     * 调整页面尺寸。该方法采用复制方式重建 PDF，不对原内容做复杂重排。
     *
     * @param source 源 PDF
     * @param target 目标 PDF
     * @param options 页面配置
     */
    public static void resizePage(Path source, Path target, PageOptions options) {
        requireReadablePdf(source);
        requireOutputPath(target);
        PageOptions actual = options == null ? createA4PageOptions() : options;
        try (PdfReader reader = new PdfReader(source.toString()); OutputStream out = Files.newOutputStream(target)) {
            Document document = createDocument(actual);
            PdfWriter writer = PdfWriter.getInstance(document, out);
            document.open();
            PdfContentByte canvas = writer.getDirectContent();
            for (int i = 1; i <= reader.getNumberOfPages(); i++) {
                document.newPage();
                PdfImportedPage page = writer.getImportedPage(reader, i);
                Rectangle original = reader.getPageSizeWithRotation(i);
                float scale = Math.min(actual.pageSize().getWidth() / original.getWidth(), actual.pageSize().getHeight() / original.getHeight());
                canvas.addTemplate(page, scale, 0, 0, scale, 0, 0);
            }
            document.close();
        } catch (Exception e) {
            throw wrapPdfException("调整 PDF 页面尺寸失败", e);
        }
    }

    /**
     * 添加标题。
     *
     * @param document PDF 文档对象
     * @param text 标题文本
     */
    public static void addTitle(Document document, String text) {
        addParagraph(document, text, new TextOptions(createTitleFont(), Element.ALIGN_CENTER, 0, 12, 0));
    }

    /**
     * 添加副标题。
     *
     * @param document PDF 文档对象
     * @param text 副标题文本
     */
    public static void addSubTitle(Document document, String text) {
        addParagraph(document, text, new TextOptions(createBoldFont(14), Element.ALIGN_LEFT, 8, 8, 0));
    }

    /**
     * 添加段落。
     *
     * @param document PDF 文档对象
     * @param text 段落文本
     */
    public static void addParagraph(Document document, String text) {
        addParagraph(document, text, new TextOptions(createDefaultFont(), Element.ALIGN_LEFT, 0, 6, 0));
    }

    /**
     * 使用指定字体添加段落。
     *
     * @param document PDF 文档对象
     * @param text 段落文本
     * @param font 字体
     */
    public static void addParagraph(Document document, String text, Font font) {
        addParagraph(document, text, new TextOptions(font, Element.ALIGN_LEFT, 0, 6, 0));
    }

    /**
     * 按文本配置添加段落。
     *
     * @param document PDF 文档对象
     * @param text 段落文本
     * @param options 文本配置
     */
    public static void addParagraph(Document document, String text, TextOptions options) {
        Objects.requireNonNull(document, "PDF 文档对象不能为空");
        requireNotNull(text, "段落文本不能为空");
        TextOptions actual = options == null ? new TextOptions(createDefaultFont(), Element.ALIGN_LEFT, 0, 6, 0) : options;
        try {
            Paragraph paragraph = createParagraph(text, actual.font());
            paragraph.setAlignment(actual.alignment());
            paragraph.setSpacingBefore(actual.spacingBefore());
            paragraph.setSpacingAfter(actual.spacingAfter());
            paragraph.setFirstLineIndent(actual.firstLineIndent());
            document.add(paragraph);
        } catch (DocumentException e) {
            throw wrapPdfException("添加段落失败", e);
        }
    }

    /**
     * 添加普通文本。
     *
     * @param document PDF 文档对象
     * @param text 文本
     */
    public static void addText(Document document, String text) {
        addParagraph(document, text);
    }

    /**
     * 添加一个空行。
     *
     * @param document PDF 文档对象
     */
    public static void addBlankLine(Document document) {
        addBlankLines(document, 1);
    }

    /**
     * 添加多个空行。
     *
     * @param document PDF 文档对象
     * @param count 空行数量
     */
    public static void addBlankLines(Document document, int count) {
        requirePositive(count, "空行数量必须大于 0");
        for (int i = 0; i < count; i++) {
            addParagraph(document, " ");
        }
    }

    /**
     * 添加无序列表。
     *
     * @param document PDF 文档对象
     * @param items 列表项
     */
    public static void addList(Document document, java.util.List<String> items) {
        addPdfList(document, items, false);
    }

    /**
     * 添加有序列表。
     *
     * @param document PDF 文档对象
     * @param items 列表项
     */
    public static void addOrderedList(Document document, java.util.List<String> items) {
        addPdfList(document, items, true);
    }

    /**
     * 添加超链接。
     *
     * @param document PDF 文档对象
     * @param text 链接文本
     * @param url 链接地址
     */
    public static void addAnchor(Document document, String text, String url) {
        Objects.requireNonNull(document, "PDF 文档对象不能为空");
        requireNotBlank(text, "链接文本不能为空");
        requireNotBlank(url, "链接地址不能为空");
        try {
            Anchor anchor = new Anchor(text, createDefaultFont());
            anchor.setReference(url);
            document.add(anchor);
        } catch (DocumentException e) {
            throw wrapPdfException("添加超链接失败", e);
        }
    }

    /**
     * 添加分隔线。
     *
     * @param document PDF 文档对象
     */
    public static void addSeparator(Document document) {
        addParagraph(document, "────────────────────────", createDefaultFont(10));
    }

    /**
     * 创建默认段落对象。
     *
     * @param text 段落文本
     * @return 段落对象
     */
    public static Paragraph createParagraph(String text) {
        return createParagraph(text, createDefaultFont());
    }

    /**
     * 创建指定字体段落对象。
     *
     * @param text 段落文本
     * @param font 字体
     * @return 段落对象
     */
    public static Paragraph createParagraph(String text, Font font) {
        requireNotNull(text, "段落文本不能为空");
        return new Paragraph(text, font == null ? createDefaultFont() : font);
    }

    /**
     * 创建短语对象。
     *
     * @param text 文本
     * @return 短语对象
     */
    public static Phrase createPhrase(String text) {
        requireNotNull(text, "短语文本不能为空");
        return new Phrase(text, createDefaultFont());
    }

    /**
     * 创建文本块对象。
     *
     * @param text 文本
     * @return 文本块对象
     */
    public static Chunk createChunk(String text) {
        requireNotNull(text, "文本块内容不能为空");
        return new Chunk(text, createDefaultFont());
    }

    /**
     * 从字体文件创建字体。
     *
     * @param fontPath 字体路径
     * @param size 字体大小
     * @return 字体对象
     */
    public static Font createFont(String fontPath, float size) {
        return createFont(fontPath, size, Font.NORMAL);
    }

    /**
     * 从字体文件创建指定样式字体。
     *
     * @param fontPath 字体路径
     * @param size 字体大小
     * @param style 字体样式
     * @return 字体对象
     */
    public static Font createFont(String fontPath, float size, int style) {
        requireNotBlank(fontPath, "字体路径不能为空");
        requirePositive(size, "字体大小必须大于 0");
        try {
            BaseFont baseFont = BaseFont.createFont(fontPath, BaseFont.IDENTITY_H, BaseFont.EMBEDDED);
            return new Font(baseFont, size, style);
        } catch (Exception e) {
            throw wrapPdfException("创建字体失败", e);
        }
    }

    /**
     * 创建默认字体。
     *
     * @return 默认字体
     */
    public static Font createDefaultFont() {
        return createDefaultFont(12);
    }

    /**
     * 创建指定大小的默认字体。
     *
     * @param size 字体大小
     * @return 默认字体
     */
    public static Font createDefaultFont(float size) {
        requirePositive(size, "字体大小必须大于 0");
        return FontFactory.getFont(FontFactory.HELVETICA, BaseFont.WINANSI, size, Font.NORMAL, Color.BLACK);
    }

    /**
     * 创建中文字体。未传入字体文件时会尝试使用 OpenPDF 内置字体名，运行环境不支持时回退默认字体。
     *
     * @return 中文字体
     */
    public static Font createChineseFont() {
        try {
            BaseFont baseFont = BaseFont.createFont("STSong-Light", "UniGB-UCS2-H", BaseFont.NOT_EMBEDDED);
            return new Font(baseFont, 12, Font.NORMAL);
        } catch (Exception e) {
            return createDefaultFont();
        }
    }

    /**
     * 创建加粗字体。
     *
     * @param size 字体大小
     * @return 加粗字体
     */
    public static Font createBoldFont(float size) {
        requirePositive(size, "字体大小必须大于 0");
        return FontFactory.getFont(FontFactory.HELVETICA, BaseFont.WINANSI, size, Font.BOLD, Color.BLACK);
    }

    /**
     * 创建标题字体。
     *
     * @return 标题字体
     */
    public static Font createTitleFont() {
        return createBoldFont(18);
    }

    /**
     * 创建表头字体。
     *
     * @return 表头字体
     */
    public static Font createTableHeaderFont() {
        return createBoldFont(11);
    }

    /**
     * 创建表格内容字体。
     *
     * @return 表格内容字体
     */
    public static Font createTableBodyFont() {
        return createDefaultFont(10);
    }

    /**
     * 注册单个字体。
     *
     * @param fontPath 字体路径
     */
    public static void registerFont(String fontPath) {
        requireNotBlank(fontPath, "字体路径不能为空");
        FontFactory.register(fontPath);
    }

    /**
     * 批量注册目录中的字体文件。
     *
     * @param fontDir 字体目录
     */
    public static void registerFonts(String fontDir) {
        requireNotBlank(fontDir, "字体目录不能为空");
        FontFactory.registerDirectory(fontDir);
    }

    /**
     * 创建默认文本样式。
     *
     * @return 文本样式
     */
    public static TextOptions createTextStyle() {
        return new TextOptions(createDefaultFont(), Element.ALIGN_LEFT, 0, 6, 0);
    }

    /**
     * 创建默认表格样式。
     *
     * @return 表格样式
     */
    public static TableOptions createCellStyle() {
        return new TableOptions(100, null, createTableHeaderFont(), createTableBodyFont(), 5);
    }

    /**
     * 创建指定列数表格。
     *
     * @param columnCount 列数
     * @return 表格对象
     */
    public static PdfPTable createTable(int columnCount) {
        requirePositive(columnCount, "列数必须大于 0");
        PdfPTable table = new PdfPTable(columnCount);
        table.setWidthPercentage(100);
        return table;
    }

    /**
     * 按列宽创建表格。
     *
     * @param columnWidths 列宽数组
     * @return 表格对象
     */
    public static PdfPTable createTable(float[] columnWidths) {
        Objects.requireNonNull(columnWidths, "列宽数组不能为空");
        if (columnWidths.length == 0) {
            throw new IllegalArgumentException("列宽数组不能为空");
        }
        PdfPTable table = new PdfPTable(columnWidths.length);
        setColumnWidths(table, columnWidths);
        table.setWidthPercentage(100);
        return table;
    }

    /**
     * 添加表格对象。
     *
     * @param document PDF 文档对象
     * @param table 表格对象
     */
    public static void addTable(Document document, PdfPTable table) {
        Objects.requireNonNull(document, "PDF 文档对象不能为空");
        Objects.requireNonNull(table, "表格对象不能为空");
        try {
            document.add(table);
        } catch (DocumentException e) {
            throw wrapPdfException("添加表格失败", e);
        }
    }

    /**
     * 快速添加简单表格。
     *
     * @param document PDF 文档对象
     * @param headers 表头
     * @param rows 数据行
     */
    public static void addTable(Document document, java.util.List<String> headers, java.util.List<java.util.List<String>> rows) {
        addSimpleTable(document, headers, rows, createCellStyle());
    }

    /**
     * 添加表头行。
     *
     * @param table 表格对象
     * @param headers 表头
     */
    public static void addHeaderRow(PdfPTable table, java.util.List<String> headers) {
        Objects.requireNonNull(table, "表格对象不能为空");
        requireNotEmpty(headers, "表头不能为空");
        for (String header : headers) {
            table.addCell(createHeaderCell(header));
        }
        table.setHeaderRows(1);
    }

    /**
     * 添加普通行。
     *
     * @param table 表格对象
     * @param row 行数据
     */
    public static void addRow(PdfPTable table, java.util.List<String> row) {
        Objects.requireNonNull(table, "表格对象不能为空");
        requireNotEmpty(row, "行数据不能为空");
        for (String value : row) {
            table.addCell(createCell(value));
        }
    }

    /**
     * 批量添加普通行。
     *
     * @param table 表格对象
     * @param rows 行数据集合
     */
    public static void addRows(PdfPTable table, java.util.List<java.util.List<String>> rows) {
        Objects.requireNonNull(table, "表格对象不能为空");
        requireNotNull(rows, "行数据集合不能为空");
        for (java.util.List<String> row : rows) {
            addRow(table, row);
        }
    }

    /**
     * 创建普通单元格。
     *
     * @param text 文本
     * @return 单元格对象
     */
    public static PdfPCell createCell(String text) {
        return createCell(text, createTableBodyFont(), 5);
    }

    /**
     * 创建指定字体和内边距的单元格。
     *
     * @param text 文本
     * @param font 字体
     * @param padding 内边距
     * @return 单元格对象
     */
    public static PdfPCell createCell(String text, Font font, float padding) {
        requireNotNull(text, "单元格文本不能为空");
        requireNonNegative(padding, "单元格内边距不能为负数");
        PdfPCell cell = new PdfPCell(new Phrase(text, font == null ? createTableBodyFont() : font));
        cell.setPadding(padding);
        cell.setVerticalAlignment(Element.ALIGN_MIDDLE);
        return cell;
    }

    /**
     * 创建表头单元格。
     *
     * @param text 文本
     * @return 表头单元格
     */
    public static PdfPCell createHeaderCell(String text) {
        PdfPCell cell = createCell(text, createTableHeaderFont(), 5);
        cell.setHorizontalAlignment(Element.ALIGN_CENTER);
        cell.setBackgroundColor(new Color(235, 235, 235));
        return cell;
    }

    /**
     * 创建空单元格。
     *
     * @return 空单元格
     */
    public static PdfPCell createEmptyCell() {
        return createCell("");
    }

    /**
     * 创建横向合并单元格。
     *
     * @param text 文本
     * @param colspan 横向合并列数
     * @return 单元格对象
     */
    public static PdfPCell createMergedCell(String text, int colspan) {
        requirePositive(colspan, "横向合并列数必须大于 0");
        PdfPCell cell = createCell(text);
        cell.setColspan(colspan);
        return cell;
    }

    /**
     * 创建纵向合并单元格。
     *
     * @param text 文本
     * @param rowspan 纵向合并行数
     * @return 单元格对象
     */
    public static PdfPCell createRowspanCell(String text, int rowspan) {
        requirePositive(rowspan, "纵向合并行数必须大于 0");
        PdfPCell cell = createCell(text);
        cell.setRowspan(rowspan);
        return cell;
    }

    /**
     * 设置表格宽度百分比。
     *
     * @param table 表格对象
     * @param widthPercentage 宽度百分比
     */
    public static void setTableWidth(PdfPTable table, float widthPercentage) {
        Objects.requireNonNull(table, "表格对象不能为空");
        if (widthPercentage <= 0 || widthPercentage > 100) {
            throw new IllegalArgumentException("表格宽度百分比必须在 0 到 100 之间");
        }
        table.setWidthPercentage(widthPercentage);
    }

    /**
     * 设置表格列宽。
     *
     * @param table 表格对象
     * @param widths 列宽数组
     */
    public static void setColumnWidths(PdfPTable table, float[] widths) {
        Objects.requireNonNull(table, "表格对象不能为空");
        Objects.requireNonNull(widths, "列宽数组不能为空");
        try {
            table.setWidths(widths.clone());
        } catch (DocumentException e) {
            throw wrapPdfException("设置表格列宽失败", e);
        }
    }

    /**
     * 设置单元格内边距。
     *
     * @param cell 单元格对象
     * @param padding 内边距
     */
    public static void setCellPadding(PdfPCell cell, float padding) {
        Objects.requireNonNull(cell, "单元格对象不能为空");
        requireNonNegative(padding, "单元格内边距不能为负数");
        cell.setPadding(padding);
    }

    /**
     * 设置单元格水平和垂直对齐方式。
     *
     * @param cell 单元格对象
     * @param horizontalAlign 水平对齐方式
     * @param verticalAlign 垂直对齐方式
     */
    public static void setCellAlign(PdfPCell cell, int horizontalAlign, int verticalAlign) {
        Objects.requireNonNull(cell, "单元格对象不能为空");
        cell.setHorizontalAlignment(horizontalAlign);
        cell.setVerticalAlignment(verticalAlign);
    }

    /**
     * 添加简单表格。
     *
     * @param document PDF 文档对象
     * @param headers 表头
     * @param rows 数据行
     * @param options 表格配置
     */
    public static void addSimpleTable(Document document, java.util.List<String> headers, java.util.List<java.util.List<String>> rows, TableOptions options) {
        requireNotEmpty(headers, "表头不能为空");
        requireNotNull(rows, "数据行不能为空");
        TableOptions actual = options == null ? createCellStyle() : options;
        PdfPTable table = actual.columnWidths() == null ? createTable(headers.size()) : createTable(actual.columnWidths());
        setTableWidth(table, actual.widthPercentage());
        for (String header : headers) {
            table.addCell(createCell(header, actual.headerFont(), actual.cellPadding()));
        }
        table.setHeaderRows(1);
        for (java.util.List<String> row : rows) {
            for (int i = 0; i < headers.size(); i++) {
                String value = row != null && i < row.size() && row.get(i) != null ? row.get(i) : "";
                table.addCell(createCell(value, actual.bodyFont(), actual.cellPadding()));
            }
        }
        addTable(document, table);
    }

    /**
     * 根据对象列表添加表格。
     *
     * @param document PDF 文档对象
     * @param data 数据列表
     * @param columns 列定义
     * @param <T> 数据类型
     */
    public static <T> void addBeanTable(Document document, java.util.List<T> data, java.util.List<TableColumn<T>> columns) {
        requireNotNull(data, "数据列表不能为空");
        requireNotEmpty(columns, "列定义不能为空");
        java.util.List<String> headers = columns.stream().map(TableColumn::title).toList();
        java.util.List<java.util.List<String>> rows = new ArrayList<>();
        for (T item : data) {
            java.util.List<String> row = new ArrayList<>();
            for (TableColumn<T> column : columns) {
                row.add(column.valueGetter().apply(item));
            }
            rows.add(row);
        }
        addTable(document, headers, rows);
    }

    /**
     * 创建表格列定义。
     *
     * @param title 列标题
     * @param valueGetter 值提取器
     * @param <T> 行数据类型
     * @return 表格列定义
     */
    public static <T> TableColumn<T> createTableColumn(String title, Function<T, String> valueGetter) {
        return new TableColumn<>(title, valueGetter);
    }

    /**
     * 从文件添加图片。
     *
     * @param document PDF 文档对象
     * @param imagePath 图片路径
     */
    public static void addImage(Document document, Path imagePath) {
        requireReadableFile(imagePath, "图片文件不存在或不可读");
        try {
            addImage(document, Image.getInstance(imagePath.toString()));
        } catch (Exception e) {
            throw wrapPdfException("添加图片失败", e);
        }
    }

    /**
     * 从字节数组添加图片。
     *
     * @param document PDF 文档对象
     * @param imageBytes 图片字节
     */
    public static void addImage(Document document, byte[] imageBytes) {
        Objects.requireNonNull(imageBytes, "图片字节不能为空");
        if (imageBytes.length == 0) {
            throw new IllegalArgumentException("图片字节不能为空");
        }
        try {
            addImage(document, Image.getInstance(imageBytes));
        } catch (Exception e) {
            throw wrapPdfException("添加图片失败", e);
        }
    }

    /**
     * 添加 OpenPDF 图片对象。
     *
     * @param document PDF 文档对象
     * @param image 图片对象
     */
    public static void addImage(Document document, Image image) {
        Objects.requireNonNull(document, "PDF 文档对象不能为空");
        Objects.requireNonNull(image, "图片对象不能为空");
        try {
            document.add(image);
        } catch (DocumentException e) {
            throw wrapPdfException("添加图片失败", e);
        }
    }

    /**
     * 按宽度等比添加图片。
     *
     * @param document PDF 文档对象
     * @param imagePath 图片路径
     * @param width 目标宽度
     */
    public static void addImageFitWidth(Document document, Path imagePath, float width) {
        requirePositive(width, "图片宽度必须大于 0");
        requireReadableFile(imagePath, "图片文件不存在或不可读");
        try {
            Image image = Image.getInstance(imagePath.toString());
            float ratio = width / image.getWidth();
            image.scaleAbsolute(width, image.getHeight() * ratio);
            addImage(document, image);
        } catch (Exception e) {
            throw wrapPdfException("按宽度添加图片失败", e);
        }
    }

    /**
     * 添加适配页面的图片。
     *
     * @param document PDF 文档对象
     * @param imagePath 图片路径
     */
    public static void addImageFitPage(Document document, Path imagePath) {
        requireReadableFile(imagePath, "图片文件不存在或不可读");
        try {
            Image image = Image.getInstance(imagePath.toString());
            Rectangle pageSize = document.getPageSize();
            image.scaleToFit(pageSize.getWidth() - document.leftMargin() - document.rightMargin(), pageSize.getHeight() - document.topMargin() - document.bottomMargin());
            addImage(document, image);
        } catch (Exception e) {
            throw wrapPdfException("添加适配页面图片失败", e);
        }
    }

    /**
     * 按绝对坐标添加图片。
     *
     * @param canvas PDF 画布
     * @param imagePath 图片路径
     * @param x X 坐标
     * @param y Y 坐标
     */
    public static void addAbsoluteImage(PdfContentByte canvas, Path imagePath, float x, float y) {
        Objects.requireNonNull(canvas, "PDF 画布不能为空");
        requireReadableFile(imagePath, "图片文件不存在或不可读");
        try {
            Image image = Image.getInstance(imagePath.toString());
            image.setAbsolutePosition(x, y);
            canvas.addImage(image);
        } catch (Exception e) {
            throw wrapPdfException("按绝对坐标添加图片失败", e);
        }
    }

    /**
     * 添加 Logo 图片。
     *
     * @param document PDF 文档对象
     * @param logoPath Logo 图片路径
     */
    public static void addLogo(Document document, Path logoPath) {
        addImageFitWidth(document, logoPath, 120);
    }

    /**
     * 添加印章图片。
     *
     * @param document PDF 文档对象
     * @param sealPath 印章图片路径
     */
    public static void addSeal(Document document, Path sealPath) {
        addImageFitWidth(document, sealPath, 100);
    }

    /**
     * 绘制线条。
     *
     * @param canvas PDF 画布
     * @param x1 起点 X
     * @param y1 起点 Y
     * @param x2 终点 X
     * @param y2 终点 Y
     */
    public static void addLine(PdfContentByte canvas, float x1, float y1, float x2, float y2) {
        Objects.requireNonNull(canvas, "PDF 画布不能为空");
        canvas.moveTo(x1, y1);
        canvas.lineTo(x2, y2);
        canvas.stroke();
    }

    /**
     * 绘制矩形。
     *
     * @param canvas PDF 画布
     * @param x X 坐标
     * @param y Y 坐标
     * @param width 宽度
     * @param height 高度
     */
    public static void addRectangle(PdfContentByte canvas, float x, float y, float width, float height) {
        Objects.requireNonNull(canvas, "PDF 画布不能为空");
        requirePositive(width, "矩形宽度必须大于 0");
        requirePositive(height, "矩形高度必须大于 0");
        canvas.rectangle(x, y, width, height);
        canvas.stroke();
    }

    /**
     * 绘制圆形。
     *
     * @param canvas PDF 画布
     * @param x 圆心 X
     * @param y 圆心 Y
     * @param radius 半径
     */
    public static void addCircle(PdfContentByte canvas, float x, float y, float radius) {
        Objects.requireNonNull(canvas, "PDF 画布不能为空");
        requirePositive(radius, "半径必须大于 0");
        canvas.circle(x, y, radius);
        canvas.stroke();
    }

    /**
     * 添加二维码图片。该方法使用 JDK 图形 API 生成简化占位二维码图，不依赖第三方二维码库。
     *
     * @param document PDF 文档对象
     * @param content 二维码内容
     */
    public static void addQrCodeImage(Document document, String content) {
        addImage(document, createQrCodeImage(content));
    }

    /**
     * 根据文本生成简化二维码占位图片。
     *
     * @param content 二维码内容
     * @return 图片字节
     */
    public static byte[] createQrCodeImage(String content) {
        requireNotBlank(content, "二维码内容不能为空");
        try {
            int size = 160;
            BufferedImage image = new BufferedImage(size, size, BufferedImage.TYPE_INT_RGB);
            Graphics2D graphics = image.createGraphics();
            graphics.setColor(Color.WHITE);
            graphics.fillRect(0, 0, size, size);
            graphics.setColor(Color.BLACK);
            int hash = content.hashCode();
            int cell = 8;
            for (int y = 0; y < size / cell; y++) {
                for (int x = 0; x < size / cell; x++) {
                    int bit = (hash >>> ((x + y) % Integer.SIZE)) & 1;
                    if (bit == 1 || x < 2 && y < 2 || x > 17 && y < 2 || x < 2 && y > 17) {
                        graphics.fillRect(x * cell, y * cell, cell, cell);
                    }
                }
            }
            graphics.dispose();
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            javax.imageio.ImageIO.write(image, "png", out);
            return out.toByteArray();
        } catch (IOException e) {
            throw wrapPdfException("生成二维码占位图片失败", e);
        }
    }

    /**
     * 给已有 PDF 添加页眉。
     *
     * @param source 源 PDF
     * @param target 目标 PDF
     * @param options 页眉配置
     */
    public static void addHeader(Path source, Path target, HeaderOptions options) {
        requireNotNull(options, "页眉配置不能为空");
        stampEachPage(source, target, (reader, stamper, pageNo) -> {
            Rectangle pageSize = reader.getPageSizeWithRotation(pageNo);
            ColumnText.showTextAligned(stamper.getOverContent(pageNo), Element.ALIGN_CENTER, new Phrase(options.text(), options.font()), pageSize.getWidth() / 2, options.y(), 0);
        });
    }

    /**
     * 给已有 PDF 添加页脚。
     *
     * @param source 源 PDF
     * @param target 目标 PDF
     * @param options 页脚配置
     */
    public static void addFooter(Path source, Path target, FooterOptions options) {
        requireNotNull(options, "页脚配置不能为空");
        stampEachPage(source, target, (reader, stamper, pageNo) -> {
            Rectangle pageSize = reader.getPageSizeWithRotation(pageNo);
            ColumnText.showTextAligned(stamper.getOverContent(pageNo), Element.ALIGN_CENTER, new Phrase(options.text(), options.font()), pageSize.getWidth() / 2, options.y(), 0);
        });
    }

    /**
     * 添加默认页码。
     *
     * @param source 源 PDF
     * @param target 目标 PDF
     */
    public static void addPageNumber(Path source, Path target) {
        addPageNumber(source, target, new PageNumberOptions(null, null, 20));
    }

    /**
     * 按配置添加页码。
     *
     * @param source 源 PDF
     * @param target 目标 PDF
     * @param options 页码配置
     */
    public static void addPageNumber(Path source, Path target, PageNumberOptions options) {
        requireNotNull(options, "页码配置不能为空");
        stampEachPage(source, target, (reader, stamper, pageNo) -> {
            Rectangle pageSize = reader.getPageSizeWithRotation(pageNo);
            String text = createPageNumberText(pageNo, reader.getNumberOfPages(), options.pattern());
            ColumnText.showTextAligned(stamper.getOverContent(pageNo), Element.ALIGN_CENTER, new Phrase(text, options.font()), pageSize.getWidth() / 2, options.y(), 0);
        });
    }

    /**
     * 同时添加页眉和页脚。
     *
     * @param source 源 PDF
     * @param target 目标 PDF
     * @param headerOptions 页眉配置
     * @param footerOptions 页脚配置
     */
    public static void addHeaderFooter(Path source, Path target, HeaderOptions headerOptions, FooterOptions footerOptions) {
        requireNotNull(headerOptions, "页眉配置不能为空");
        requireNotNull(footerOptions, "页脚配置不能为空");
        stampEachPage(source, target, (reader, stamper, pageNo) -> {
            Rectangle pageSize = reader.getPageSizeWithRotation(pageNo);
            ColumnText.showTextAligned(stamper.getOverContent(pageNo), Element.ALIGN_CENTER, new Phrase(headerOptions.text(), headerOptions.font()), pageSize.getWidth() / 2, headerOptions.y(), 0);
            ColumnText.showTextAligned(stamper.getOverContent(pageNo), Element.ALIGN_CENTER, new Phrase(footerOptions.text(), footerOptions.font()), pageSize.getWidth() / 2, footerOptions.y(), 0);
        });
    }

    /**
     * 创建页码文本。
     *
     * @param currentPage 当前页
     * @param totalPage 总页数
     * @return 页码文本
     */
    public static String createPageNumberText(int currentPage, int totalPage) {
        return createPageNumberText(currentPage, totalPage, "第 {page} / {total} 页");
    }

    /**
     * 根据格式创建页码文本。
     *
     * @param currentPage 当前页
     * @param totalPage 总页数
     * @param pattern 页码格式
     * @return 页码文本
     */
    public static String createPageNumberText(int currentPage, int totalPage, String pattern) {
        requirePositive(currentPage, "当前页必须大于 0");
        requirePositive(totalPage, "总页数必须大于 0");
        if (currentPage > totalPage) {
            throw new IllegalArgumentException("当前页不能大于总页数");
        }
        String actual = isBlank(pattern) ? "第 {page} / {total} 页" : pattern;
        return actual.replace("{page}", String.valueOf(currentPage)).replace("{total}", String.valueOf(totalPage));
    }

    /**
     * 使用盖章方式添加页码。
     *
     * @param stamper PDF 盖章器
     * @param options 页码配置
     */
    public static void stampPageNumber(PdfStamper stamper, PageNumberOptions options) {
        Objects.requireNonNull(stamper, "PDF 盖章器不能为空");
        throw new UnsupportedOperationException("独立 PdfStamper 无法安全获取总页数，请使用 addPageNumber(Path, Path, PageNumberOptions)");
    }

    /**
     * 添加文字水印。
     *
     * @param source 源 PDF
     * @param target 目标 PDF
     * @param text 水印文本
     */
    public static void addTextWatermark(Path source, Path target, String text) {
        addTextWatermark(source, target, new WatermarkOptions(text, null, null, 0.25f, 45, null, null, false));
    }

    /**
     * 按配置添加文字水印。
     *
     * @param source 源 PDF
     * @param target 目标 PDF
     * @param options 水印配置
     */
    public static void addTextWatermark(Path source, Path target, WatermarkOptions options) {
        requireNotNull(options, "水印配置不能为空");
        if (isBlank(options.text())) {
            throw new IllegalArgumentException("文字水印内容不能为空");
        }
        stampEachPage(source, target, (reader, stamper, pageNo) -> drawTextWatermark(reader, stamper.getOverContent(pageNo), pageNo, options));
    }

    /**
     * 添加图片水印。
     *
     * @param source 源 PDF
     * @param target 目标 PDF
     * @param imagePath 图片路径
     */
    public static void addImageWatermark(Path source, Path target, Path imagePath) {
        addImageWatermark(source, target, new WatermarkOptions(null, imagePath, null, 0.25f, 0, null, null, false));
    }

    /**
     * 按配置添加图片水印。
     *
     * @param source 源 PDF
     * @param target 目标 PDF
     * @param options 水印配置
     */
    public static void addImageWatermark(Path source, Path target, WatermarkOptions options) {
        requireNotNull(options, "水印配置不能为空");
        requireReadableFile(options.imagePath(), "水印图片不存在或不可读");
        stampEachPage(source, target, (reader, stamper, pageNo) -> drawImageWatermark(reader, stamper.getOverContent(pageNo), pageNo, options));
    }

    /**
     * 添加平铺文字水印。
     *
     * @param source 源 PDF
     * @param target 目标 PDF
     * @param text 水印文本
     */
    public static void addTiledWatermark(Path source, Path target, String text) {
        addTextWatermark(source, target, new WatermarkOptions(text, null, null, 0.2f, 45, null, null, true));
    }

    /**
     * 给指定页面添加水印。
     *
     * @param source 源 PDF
     * @param target 目标 PDF
     * @param pageNo 页码
     * @param options 水印配置
     */
    public static void addWatermarkToPage(Path source, Path target, int pageNo, WatermarkOptions options) {
        requirePositive(pageNo, "页码必须大于 0");
        requireNotNull(options, "水印配置不能为空");
        stampPages(source, target, Set.of(pageNo), (reader, stamper, current) -> {
            if (!isBlank(options.text())) {
                drawTextWatermark(reader, stamper.getOverContent(current), current, options);
            } else {
                drawImageWatermark(reader, stamper.getOverContent(current), current, options);
            }
        });
    }

    /**
     * 尝试移除水印。由于 PDF 水印实现形式不统一，该方法执行内容复制，不保证可移除已合并到内容流中的水印。
     *
     * @param source 源 PDF
     * @param target 目标 PDF
     */
    public static void removeWatermark(Path source, Path target) {
        merge(java.util.List.of(source), target);
    }

    /**
     * 创建默认水印配置。
     *
     * @return 水印配置
     */
    public static WatermarkOptions createWatermarkOptions() {
        return new WatermarkOptions("CONFIDENTIAL", null, null, 0.25f, 45, null, null, true);
    }

    /**
     * 合并多个 PDF 文件。
     *
     * @param sources 源 PDF 集合
     * @param target 目标 PDF
     */
    public static void merge(java.util.List<Path> sources, Path target) {
        requireNotEmpty(sources, "源 PDF 集合不能为空");
        requireOutputPath(target);
        try (OutputStream out = Files.newOutputStream(target)) {
            mergeStreams(sources.stream().map(path -> {
                requireReadablePdf(path);
                try {
                    return Files.newInputStream(path);
                } catch (IOException e) {
                    throw new UncheckedIOException(e);
                }
            }).toList(), out);
        } catch (UncheckedIOException | IOException e) {
            throw wrapPdfException("合并 PDF 失败", e);
        }
    }

    /**
     * 合并多个 PDF 并返回字节数组。
     *
     * @param sources 源 PDF 集合
     * @return 合并后的 PDF 字节数组
     */
    public static byte[] mergeToBytes(java.util.List<Path> sources) {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        java.util.List<InputStream> streams = new ArrayList<>();
        try {
            for (Path source : sources) {
                requireReadablePdf(source);
                streams.add(Files.newInputStream(source));
            }
            mergeStreams(streams, out);
            return out.toByteArray();
        } catch (IOException e) {
            throw wrapPdfException("合并 PDF 字节失败", e);
        } finally {
            streams.forEach(PDFUtil::safeClose);
        }
    }

    /**
     * 合并多个 PDF 输入流。
     *
     * @param sources 源输入流集合
     * @param out 输出流
     */
    public static void mergeStreams(java.util.List<InputStream> sources, OutputStream out) {
        requireNotEmpty(sources, "源 PDF 输入流集合不能为空");
        Objects.requireNonNull(out, "输出流不能为空");
        Document document = null;
        PdfCopy copy = null;
        java.util.List<PdfReader> readers = new ArrayList<>();
        try {
            for (InputStream source : sources) {
                Objects.requireNonNull(source, "源 PDF 输入流不能为空");
                PdfReader reader = new PdfReader(source);
                readers.add(reader);
                if (document == null) {
                    document = new Document(reader.getPageSizeWithRotation(1));
                    copy = new PdfCopy(document, out);
                    document.open();
                }
                for (int page = 1; page <= reader.getNumberOfPages(); page++) {
                    copy.addPage(copy.getImportedPage(reader, page));
                }
            }
        } catch (Exception e) {
            throw wrapPdfException("合并 PDF 输入流失败", e);
        } finally {
            if (document != null && document.isOpen()) {
                document.close();
            }
            readers.forEach(PdfReader::close);
            sources.forEach(PDFUtil::safeClose);
        }
    }

    /**
     * 将一个 PDF 追加到另一个 PDF 后面。
     *
     * @param source 源 PDF
     * @param appendFile 追加 PDF
     * @param target 目标 PDF
     */
    public static void append(Path source, Path appendFile, Path target) {
        merge(java.util.List.of(source, appendFile), target);
    }

    /**
     * 在指定页后插入 PDF。
     *
     * @param source 源 PDF
     * @param insertFile 插入 PDF
     * @param pageNo 插入位置页码，从 1 开始
     * @param target 目标 PDF
     */
    public static void insert(Path source, Path insertFile, int pageNo, Path target) {
        requireReadablePdf(source);
        requireReadablePdf(insertFile);
        requireOutputPath(target);
        requirePositive(pageNo, "插入页码必须大于 0");
        try (PdfReader sourceReader = new PdfReader(source.toString()); PdfReader insertReader = new PdfReader(insertFile.toString()); OutputStream out = Files.newOutputStream(target)) {
            validatePageNo(sourceReader, pageNo);
            Document document = new Document(sourceReader.getPageSizeWithRotation(1));
            PdfCopy copy = new PdfCopy(document, out);
            document.open();
            for (int i = 1; i <= sourceReader.getNumberOfPages(); i++) {
                copy.addPage(copy.getImportedPage(sourceReader, i));
                if (i == pageNo) {
                    for (int j = 1; j <= insertReader.getNumberOfPages(); j++) {
                        copy.addPage(copy.getImportedPage(insertReader, j));
                    }
                }
            }
            document.close();
        } catch (Exception e) {
            throw wrapPdfException("插入 PDF 失败", e);
        }
    }

    /**
     * 按单页拆分 PDF。
     *
     * @param source 源 PDF
     * @param outputDir 输出目录
     */
    public static void split(Path source, Path outputDir) {
        split(source, outputDir, 1);
    }

    /**
     * 按固定页数拆分 PDF。
     *
     * @param source 源 PDF
     * @param outputDir 输出目录
     * @param pageSize 每个文件包含页数
     */
    public static void split(Path source, Path outputDir, int pageSize) {
        requireReadablePdf(source);
        requireDirectory(outputDir);
        requirePositive(pageSize, "每个文件页数必须大于 0");
        int total = readPageCount(source);
        int part = 1;
        for (int from = 1; from <= total; from += pageSize) {
            int to = Math.min(total, from + pageSize - 1);
            splitByRange(source, outputDir.resolve("part-" + part + ".pdf"), from, to);
            part++;
        }
    }

    /**
     * 按页码范围拆分 PDF。
     *
     * @param source 源 PDF
     * @param target 目标 PDF
     * @param fromPage 起始页
     * @param toPage 结束页
     */
    public static void splitByRange(Path source, Path target, int fromPage, int toPage) {
        requireReadablePdf(source);
        requireOutputPath(target);
        try (PdfReader reader = new PdfReader(source.toString()); OutputStream out = Files.newOutputStream(target)) {
            validatePageRange(reader, fromPage, toPage);
            Document document = new Document(reader.getPageSizeWithRotation(fromPage));
            PdfCopy copy = new PdfCopy(document, out);
            document.open();
            for (int i = fromPage; i <= toPage; i++) {
                copy.addPage(copy.getImportedPage(reader, i));
            }
            document.close();
        } catch (Exception e) {
            throw wrapPdfException("按范围拆分 PDF 失败", e);
        }
    }

    /**
     * 提取指定页面。
     *
     * @param source 源 PDF
     * @param target 目标 PDF
     * @param pages 页码集合
     */
    public static void extractPages(Path source, Path target, java.util.List<Integer> pages) {
        requireReadablePdf(source);
        requireOutputPath(target);
        requireNotEmpty(pages, "页码集合不能为空");
        try (PdfReader reader = new PdfReader(source.toString()); OutputStream out = Files.newOutputStream(target)) {
            Document document = new Document(reader.getPageSizeWithRotation(pages.getFirst()));
            PdfCopy copy = new PdfCopy(document, out);
            document.open();
            for (Integer page : pages) {
                requireNotNull(page, "页码不能为空");
                validatePageNo(reader, page);
                copy.addPage(copy.getImportedPage(reader, page));
            }
            document.close();
        } catch (Exception e) {
            throw wrapPdfException("提取 PDF 页面失败", e);
        }
    }

    /**
     * 提取首页。
     *
     * @param source 源 PDF
     * @param target 目标 PDF
     */
    public static void extractFirstPage(Path source, Path target) {
        extractPages(source, target, java.util.List.of(1));
    }

    /**
     * 提取末页。
     *
     * @param source 源 PDF
     * @param target 目标 PDF
     */
    public static void extractLastPage(Path source, Path target) {
        extractPages(source, target, java.util.List.of(readPageCount(source)));
    }

    /**
     * 读取 PDF 页数。
     *
     * @param pdfPath PDF 文件路径
     * @return 页数
     */
    public static int readPageCount(Path pdfPath) {
        requireReadablePdf(pdfPath);
        try (PdfReader reader = new PdfReader(pdfPath.toString())) {
            return reader.getNumberOfPages();
        } catch (IOException e) {
            throw wrapPdfException("读取 PDF 页数失败", e);
        }
    }

    /**
     * 读取 PDF 元数据。
     *
     * @param pdfPath PDF 文件路径
     * @return 元数据 Map
     */
    public static Map<String, String> readMetadata(Path pdfPath) {
        requireReadablePdf(pdfPath);
        try (PdfReader reader = new PdfReader(pdfPath.toString())) {
            return new LinkedHashMap<>(reader.getInfo());
        } catch (IOException e) {
            throw wrapPdfException("读取 PDF 元数据失败", e);
        }
    }

    /**
     * 读取 PDF 标题。
     *
     * @param pdfPath PDF 文件路径
     * @return 标题
     */
    public static String readTitle(Path pdfPath) {
        return readMetadata(pdfPath).get("Title");
    }

    /**
     * 读取 PDF 作者。
     *
     * @param pdfPath PDF 文件路径
     * @return 作者
     */
    public static String readAuthor(Path pdfPath) {
        return readMetadata(pdfPath).get("Author");
    }

    /**
     * 读取 PDF 主题。
     *
     * @param pdfPath PDF 文件路径
     * @return 主题
     */
    public static String readSubject(Path pdfPath) {
        return readMetadata(pdfPath).get("Subject");
    }

    /**
     * 读取 PDF 关键字。
     *
     * @param pdfPath PDF 文件路径
     * @return 关键字
     */
    public static String readKeywords(Path pdfPath) {
        return readMetadata(pdfPath).get("Keywords");
    }

    /**
     * 提取 PDF 全文文本。
     *
     * @param pdfPath PDF 文件路径
     * @return 文本内容
     */
    public static String extractText(Path pdfPath) {
        int total = readPageCount(pdfPath);
        return extractText(pdfPath, 1, total);
    }

    /**
     * 提取指定页文本。
     *
     * @param pdfPath PDF 文件路径
     * @param pageNo 页码
     * @return 文本内容
     */
    public static String extractText(Path pdfPath, int pageNo) {
        return extractText(pdfPath, pageNo, pageNo);
    }

    /**
     * 提取指定页码范围文本。
     *
     * @param pdfPath PDF 文件路径
     * @param fromPage 起始页
     * @param toPage 结束页
     * @return 文本内容
     */
    public static String extractText(Path pdfPath, int fromPage, int toPage) {
        requireReadablePdf(pdfPath);
        try (PdfReader reader = new PdfReader(pdfPath.toString())) {
            validatePageRange(reader, fromPage, toPage);

            PdfTextExtractor extractor = new PdfTextExtractor(reader);
            StringBuilder builder = new StringBuilder();

            for (int page = fromPage; page <= toPage; page++) {
                if (!builder.isEmpty()) {
                    builder.append(System.lineSeparator());
                }
                builder.append(extractor.getTextFromPage(page, false));
            }

            return builder.toString();
        } catch (IOException e) {
            throw wrapPdfException("提取 PDF 文本失败", e);
        }
    }

    /**
     * 提取 PDF 图片。当前仅创建输出目录并返回空列表，复杂图片解析建议使用专门图像解析实现。
     *
     * @param pdfPath PDF 文件路径
     * @param outputDir 输出目录
     * @return 提取出的图片路径集合
     */
    public static java.util.List<Path> extractImages(Path pdfPath, Path outputDir) {
        requireReadablePdf(pdfPath);
        requireDirectory(outputDir);
        return java.util.List.of();
    }

    /**
     * 读取指定页尺寸。
     *
     * @param pdfPath PDF 文件路径
     * @param pageNo 页码
     * @return 页面尺寸
     */
    public static Rectangle readPageSize(Path pdfPath, int pageNo) {
        requireReadablePdf(pdfPath);
        try (PdfReader reader = new PdfReader(pdfPath.toString())) {
            validatePageNo(reader, pageNo);
            return reader.getPageSizeWithRotation(pageNo);
        } catch (IOException e) {
            throw wrapPdfException("读取 PDF 页面尺寸失败", e);
        }
    }

    /**
     * 判断 PDF 是否包含可提取文本。
     *
     * @param pdfPath PDF 文件路径
     * @return 是否包含文本
     */
    public static boolean hasText(Path pdfPath) {
        return !extractText(pdfPath).trim().isEmpty();
    }

    /**
     * 粗略判断是否为扫描件 PDF。
     *
     * @param pdfPath PDF 文件路径
     * @return 是否可能为扫描件
     */
    public static boolean isScannedPdf(Path pdfPath) {
        return !hasText(pdfPath);
    }

    /**
     * 设置 PDF 元数据。
     *
     * @param source 源 PDF
     * @param target 目标 PDF
     * @param options 元数据配置
     */
    public static void setMetadata(Path source, Path target, MetadataOptions options) {
        requireReadablePdf(source);
        requireOutputPath(target);
        requireNotNull(options, "元数据配置不能为空");

        try (PdfReader reader = new PdfReader(source.toString());
             OutputStream out = Files.newOutputStream(target)) {

            Map<String, String> info = new HashMap<>(reader.getInfo());
            putIfNotBlank(info, "Title", options.title());
            putIfNotBlank(info, "Author", options.author());
            putIfNotBlank(info, "Subject", options.subject());
            putIfNotBlank(info, "Keywords", options.keywords());
            putIfNotBlank(info, "Creator", options.creator());

            Rectangle pageSize = reader.getNumberOfPages() > 0
                    ? reader.getPageSizeWithRotation(1)
                    : PageSize.A4;

            Document document = new Document(pageSize);
            try {
                applyMetadata(document, info);

                PdfCopy copy = new PdfCopy(document, out);
                document.open();

                for (int page = 1; page <= reader.getNumberOfPages(); page++) {
                    copy.addPage(copy.getImportedPage(reader, page));
                }

                copy.freeReader(reader);
            } finally {
                if (document.isOpen()) {
                    document.close();
                }
            }
        } catch (Exception e) {
            throw wrapPdfException("设置 PDF 元数据失败", e);
        }
    }

    private static void applyMetadata(Document document, Map<String, String> info) {
        if (info == null || info.isEmpty()) {
            return;
        }

        addMetadataIfPresent(info, "Title", document::addTitle);
        addMetadataIfPresent(info, "Author", document::addAuthor);
        addMetadataIfPresent(info, "Subject", document::addSubject);
        addMetadataIfPresent(info, "Keywords", document::addKeywords);
        addMetadataIfPresent(info, "Creator", document::addCreator);
    }

    private static void addMetadataIfPresent(Map<String, String> info, String key, java.util.function.Consumer<String> consumer) {
        String value = info.get(key);
        if (value != null && !value.isBlank()) {
            consumer.accept(value);
        }
    }

    /**
     * 设置文档标题。
     *
     * @param document PDF 文档对象
     * @param title 标题
     */
    public static void setTitle(Document document, String title) {
        Objects.requireNonNull(document, "PDF 文档对象不能为空");
        requireNotBlank(title, "标题不能为空");
        document.addTitle(title);
    }

    /**
     * 设置文档作者。
     *
     * @param document PDF 文档对象
     * @param author 作者
     */
    public static void setAuthor(Document document, String author) {
        Objects.requireNonNull(document, "PDF 文档对象不能为空");
        requireNotBlank(author, "作者不能为空");
        document.addAuthor(author);
    }

    /**
     * 设置文档主题。
     *
     * @param document PDF 文档对象
     * @param subject 主题
     */
    public static void setSubject(Document document, String subject) {
        Objects.requireNonNull(document, "PDF 文档对象不能为空");
        requireNotBlank(subject, "主题不能为空");
        document.addSubject(subject);
    }

    /**
     * 设置文档关键字。
     *
     * @param document PDF 文档对象
     * @param keywords 关键字
     */
    public static void setKeywords(Document document, String keywords) {
        Objects.requireNonNull(document, "PDF 文档对象不能为空");
        requireNotBlank(keywords, "关键字不能为空");
        document.addKeywords(keywords);
    }

    /**
     * 设置文档创建者。
     *
     * @param document PDF 文档对象
     * @param creator 创建者
     */
    public static void setCreator(Document document, String creator) {
        Objects.requireNonNull(document, "PDF 文档对象不能为空");
        requireNotBlank(creator, "创建者不能为空");
        document.addCreator(creator);
    }

    /**
     * 清理 PDF 元数据。
     *
     * @param source 源 PDF
     * @param target 目标 PDF
     */
    public static void clearMetadata(Path source, Path target) {
        setMetadata(source, target, new MetadataOptions("", "", "", "", ""));
    }

    /**
     * 复制 PDF 元数据到新文件。
     *
     * @param source 源 PDF
     * @param target 目标 PDF
     */
    public static void copyMetadata(Path source, Path target) {
        requireReadablePdf(source);
        MetadataOptions options = new MetadataOptions(readTitle(source), readAuthor(source), readSubject(source), readKeywords(source), readMetadata(source).get("Creator"));
        merge(java.util.List.of(source), target);
        Path temp = target.resolveSibling(target.getFileName() + ".metadata.tmp.pdf");
        setMetadata(target, temp, options);
        moveReplace(temp, target);
    }

    /**
     * 使用用户密码加密 PDF。
     *
     * @param source 源 PDF
     * @param target 目标 PDF
     * @param userPassword 用户密码
     */
    public static void encrypt(Path source, Path target, String userPassword) {
        encrypt(source, target, new EncryptOptions(userPassword, userPassword, PdfWriter.ALLOW_PRINTING, PdfWriter.STANDARD_ENCRYPTION_128));
    }

    /**
     * 按配置加密 PDF。
     *
     * @param source 源 PDF
     * @param target 目标 PDF
     * @param options 加密配置
     */
    public static void encrypt(Path source, Path target, EncryptOptions options) {
        requireReadablePdf(source);
        requireOutputPath(target);
        requireNotNull(options, "加密配置不能为空");
        try (PdfReader reader = new PdfReader(source.toString()); OutputStream out = Files.newOutputStream(target)) {
            try (PdfStamper stamper = new PdfStamper(reader, out)) {
                stamper.setEncryption(options.userPassword().getBytes(StandardCharsets.UTF_8), options.ownerPassword().getBytes(StandardCharsets.UTF_8), options.permissions(), options.encryptionType());
            }
        } catch (Exception e) {
            throw wrapPdfException("加密 PDF 失败", e);
        }
    }

    /**
     * 解密 PDF。
     *
     * @param source 源 PDF
     * @param target 目标 PDF
     * @param password 密码
     */
    public static void decrypt(Path source, Path target, String password) {
        requireReadableFile(source, "源 PDF 不存在或不可读");
        requireOutputPath(target);
        requireNotBlank(password, "密码不能为空");
        try (PdfReader reader = new PdfReader(source.toString(), password.getBytes(StandardCharsets.UTF_8)); OutputStream out = Files.newOutputStream(target)) {
            try (PdfStamper stamper = new PdfStamper(reader, out)) {
                // 打开后重新写出即生成未加密副本。
            }
        } catch (Exception e) {
            throw wrapPdfException("解密 PDF 失败", e);
        }
    }

    /**
     * 设置 PDF 权限保护。
     *
     * @param source 源 PDF
     * @param target 目标 PDF
     * @param options 权限配置
     */
    public static void protect(Path source, Path target, ProtectOptions options) {
        requireNotNull(options, "权限配置不能为空");
        int permissions = 0;
        if (options.allowPrint()) {
            permissions |= PdfWriter.ALLOW_PRINTING;
        }
        if (options.allowCopy()) {
            permissions |= PdfWriter.ALLOW_COPY;
        }
        if (options.allowModify()) {
            permissions |= PdfWriter.ALLOW_MODIFY_CONTENTS;
        }
        encrypt(source, target, new EncryptOptions(options.userPassword(), options.ownerPassword(), permissions, PdfWriter.STANDARD_ENCRYPTION_128));
    }

    /**
     * 设置是否允许打印。
     *
     * @param source 源 PDF
     * @param target 目标 PDF
     * @param allowed 是否允许
     */
    public static void setPrintAllowed(Path source, Path target, boolean allowed) {
        protect(source, target, new ProtectOptions(randomPassword(), randomPassword(), allowed, true, true));
    }

    /**
     * 设置是否允许复制。
     *
     * @param source 源 PDF
     * @param target 目标 PDF
     * @param allowed 是否允许
     */
    public static void setCopyAllowed(Path source, Path target, boolean allowed) {
        protect(source, target, new ProtectOptions(randomPassword(), randomPassword(), true, allowed, true));
    }

    /**
     * 设置是否允许修改。
     *
     * @param source 源 PDF
     * @param target 目标 PDF
     * @param allowed 是否允许
     */
    public static void setModifyAllowed(Path source, Path target, boolean allowed) {
        protect(source, target, new ProtectOptions(randomPassword(), randomPassword(), true, true, allowed));
    }

    /**
     * 判断 PDF 是否加密。
     *
     * @param pdfPath PDF 文件路径
     * @return 是否加密
     */
    public static boolean isEncrypted(Path pdfPath) {
        requireReadableFile(pdfPath, "PDF 文件不存在或不可读");
        try (PdfReader reader = new PdfReader(pdfPath.toString())) {
            return reader.isEncrypted();
        } catch (IOException e) {
            return true;
        }
    }

    /**
     * 校验 PDF 密码。
     *
     * @param pdfPath PDF 文件路径
     * @param password 密码
     * @return 密码是否可用
     */
    public static boolean checkPassword(Path pdfPath, String password) {
        requireReadableFile(pdfPath, "PDF 文件不存在或不可读");
        requireNotBlank(password, "密码不能为空");
        try (PdfReader ignored = new PdfReader(pdfPath.toString(), password.getBytes(StandardCharsets.UTF_8))) {
            return true;
        } catch (IOException e) {
            return false;
        }
    }

    /**
     * 移除密码保护。
     *
     * @param source 源 PDF
     * @param target 目标 PDF
     * @param ownerPassword 所有者密码
     */
    public static void removePassword(Path source, Path target, String ownerPassword) {
        decrypt(source, target, ownerPassword);
    }

    /**
     * 填充 PDF 表单并默认扁平化。
     *
     * @param template PDF 表单模板
     * @param target 目标 PDF
     * @param data 表单数据
     */
    public static void fillForm(Path template, Path target, Map<String, Object> data) {
        fillForm(template, target, data, true);
    }

    /**
     * 填充 PDF 表单。
     *
     * @param template PDF 表单模板
     * @param target 目标 PDF
     * @param data 表单数据
     * @param flatten 是否扁平化
     */
    public static void fillForm(Path template, Path target, Map<String, Object> data, boolean flatten) {
        requireReadablePdf(template);
        requireOutputPath(target);
        requireNotNull(data, "表单数据不能为空");
        try (PdfReader reader = new PdfReader(template.toString()); OutputStream out = Files.newOutputStream(target)) {
            try (PdfStamper stamper = new PdfStamper(reader, out)) {
                AcroFields form = stamper.getAcroFields();
                for (Map.Entry<String, Object> entry : data.entrySet()) {
                    if (!isBlank(entry.getKey())) {
                        form.setField(entry.getKey(), entry.getValue() == null ? "" : String.valueOf(entry.getValue()));
                    }
                }
                stamper.setFormFlattening(flatten);
            }
        } catch (Exception e) {
            throw wrapPdfException("填充 PDF 表单失败", e);
        }
    }

    /**
     * 读取 PDF 表单字段。
     *
     * @param pdfPath PDF 文件路径
     * @return 字段 Map
     */
    public static Map<String, String> readFormFields(Path pdfPath) {
        requireReadablePdf(pdfPath);

        try (PdfReader reader = new PdfReader(pdfPath.toString())) {
            AcroFields fields = reader.getAcroFields();
            Map<String, String> result = new LinkedHashMap<>();

            if (fields == null) {
                return result;
            }

            Map<String, AcroFields.Item> allFields = fields.getAllFields();
            if (allFields == null || allFields.isEmpty()) {
                return result;
            }

            for (String name : allFields.keySet()) {
                String value = fields.getField(name);
                result.put(name, value == null ? "" : value);
            }

            return result;
        } catch (IOException e) {
            throw wrapPdfException("读取 PDF 表单字段失败", e);
        }
    }

    /**
     * 获取 PDF 表单字段名称。
     *
     * @param pdfPath PDF 文件路径
     * @return 字段名称集合
     */
    public static Set<String> getFormFieldNames(Path pdfPath) {
        return readFormFields(pdfPath).keySet();
    }

    /**
     * 判断 PDF 是否包含表单。
     *
     * @param pdfPath PDF 文件路径
     * @return 是否包含表单
     */
    public static boolean hasForm(Path pdfPath) {
        return !readFormFields(pdfPath).isEmpty();
    }

    /**
     * 扁平化 PDF 表单。
     *
     * @param source 源 PDF
     * @param target 目标 PDF
     */
    public static void flattenForm(Path source, Path target) {
        fillForm(source, target, Map.of(), true);
    }

    /**
     * 重命名表单字段。OpenPDF 对字段重命名支持有限，该方法当前不修改文件并抛出不支持异常。
     *
     * @param source 源 PDF
     * @param target 目标 PDF
     * @param oldName 原字段名
     * @param newName 新字段名
     */
    public static void renameField(Path source, Path target, String oldName, String newName) {
        requireReadablePdf(source);
        requireOutputPath(target);
        requireNotBlank(oldName, "原字段名不能为空");
        requireNotBlank(newName, "新字段名不能为空");
        throw new UnsupportedOperationException("字段重命名依赖具体表单结构，建议在模板阶段维护字段名称");
    }

    /**
     * 删除表单字段。OpenPDF 对字段删除支持有限，该方法当前不修改文件并抛出不支持异常。
     *
     * @param source 源 PDF
     * @param target 目标 PDF
     * @param fieldName 字段名
     */
    public static void removeField(Path source, Path target, String fieldName) {
        requireReadablePdf(source);
        requireOutputPath(target);
        requireNotBlank(fieldName, "字段名不能为空");
        throw new UnsupportedOperationException("字段删除依赖具体表单结构，建议通过扁平化或模板重建处理");
    }

    /**
     * 根据简单模板回调渲染 PDF。
     *
     * @param template 模板构建器
     * @param data 模板数据
     * @param target 目标 PDF
     */
    public static void renderTemplate(PDFBuilder template, Map<String, Object> data, Path target) {
        requireNotNull(template, "模板构建器不能为空");
        requireNotNull(data, "模板数据不能为空");
        writeToFile(target, template);
    }

    /**
     * 根据简单模板回调渲染 PDF 字节数组。
     *
     * @param template 模板构建器
     * @param data 模板数据
     * @return PDF 字节数组
     */
    public static byte[] renderTemplateToBytes(PDFBuilder template, Map<String, Object> data) {
        requireNotNull(template, "模板构建器不能为空");
        requireNotNull(data, "模板数据不能为空");
        return writeToBytes(template);
    }

    /**
     * 将 HTML 字符串转换为基础文本 PDF。该方法不解析复杂 CSS，仅适合无第三方 HTML 渲染依赖的轻量场景。
     *
     * @param html HTML 字符串
     * @param target 目标 PDF
     */
    public static void renderHtmlTemplate(String html, Path target) {
        htmlToPdf(html, target);
    }

    /**
     * 将 HTML 字符串转换为基础文本 PDF 字节数组。
     *
     * @param html HTML 字符串
     * @return PDF 字节数组
     */
    public static byte[] renderHtmlTemplateToBytes(String html) {
        return htmlToPdfBytes(html);
    }

    /**
     * 生成合同类 PDF。
     *
     * @param data 合同数据
     * @param target 目标 PDF
     */
    public static void renderContract(Map<String, Object> data, Path target) {
        renderKeyValuePdf("合同", data, target);
    }

    /**
     * 生成报表类 PDF。
     *
     * @param data 报表数据
     * @param target 目标 PDF
     */
    public static void renderReport(Map<String, Object> data, Path target) {
        renderKeyValuePdf("报表", data, target);
    }

    /**
     * 生成票据类 PDF。
     *
     * @param data 票据数据
     * @param target 目标 PDF
     */
    public static void renderInvoice(Map<String, Object> data, Path target) {
        renderKeyValuePdf("票据", data, target);
    }

    /**
     * 生成证书类 PDF。
     *
     * @param data 证书数据
     * @param target 目标 PDF
     */
    public static void renderCertificate(Map<String, Object> data, Path target) {
        renderKeyValuePdf("证书", data, target);
    }

    /**
     * HTML 字符串转 PDF。
     *
     * @param html HTML 字符串
     * @param target 目标 PDF
     */
    public static void htmlToPdf(String html, Path target) {
        htmlToPdf(html, target, new HtmlPdfOptions(null, null, null));
    }

    /**
     * HTML 字符串按配置转 PDF。
     *
     * @param html HTML 字符串
     * @param target 目标 PDF
     * @param options 转换配置
     */
    public static void htmlToPdf(String html, Path target, HtmlPdfOptions options) {
        requireNotNull(html, "HTML 内容不能为空");
        HtmlPdfOptions actual = options == null ? new HtmlPdfOptions(null, null, null) : options;
        writeToFile(target, actual.pageOptions(), actual.metadataOptions(), document -> addParagraph(document, htmlToPlainText(html), actual.font()));
    }

    /**
     * HTML 字符串写入 PDF 输出流。
     *
     * @param html HTML 字符串
     * @param out 输出流
     */
    public static void htmlToPdf(String html, OutputStream out) {
        requireNotNull(html, "HTML 内容不能为空");
        writeToStream(out, document -> addParagraph(document, htmlToPlainText(html)));
    }

    /**
     * HTML 字符串转 PDF 字节数组。
     *
     * @param html HTML 字符串
     * @return PDF 字节数组
     */
    public static byte[] htmlToPdfBytes(String html) {
        requireNotNull(html, "HTML 内容不能为空");
        return writeToBytes(document -> addParagraph(document, htmlToPlainText(html)));
    }

    /**
     * HTML 文件转 PDF。
     *
     * @param htmlPath HTML 文件路径
     * @param target 目标 PDF
     */
    public static void htmlFileToPdf(Path htmlPath, Path target) {
        requireReadableFile(htmlPath, "HTML 文件不存在或不可读");
        try {
            htmlToPdf(Files.readString(htmlPath, StandardCharsets.UTF_8), target);
        } catch (IOException e) {
            throw wrapPdfException("读取 HTML 文件失败", e);
        }
    }

    /**
     * URL 页面转 PDF。仅支持 file:// URL，本方法不发起网络请求。
     *
     * @param url URL 地址
     * @param target 目标 PDF
     */
    public static void urlToPdf(String url, Path target) {
        requireNotBlank(url, "URL 不能为空");
        if (!url.startsWith("file://")) {
            throw new UnsupportedOperationException("仅使用 JDK 和 OpenPDF 时不建议工具类直接请求外部 URL，请先获取 HTML 后调用 htmlToPdf");
        }
        htmlFileToPdf(Path.of(java.net.URI.create(url)), target);
    }

    /**
     * 模板数据转 PDF。
     *
     * @param templateName 模板名称
     * @param data 模板数据
     * @param target 目标 PDF
     */
    public static void templateToPdf(String templateName, Map<String, Object> data, Path target) {
        requireNotBlank(templateName, "模板名称不能为空");
        renderKeyValuePdf(templateName, data, target);
    }

    /**
     * 模板数据转 PDF 字节数组。
     *
     * @param templateName 模板名称
     * @param data 模板数据
     * @return PDF 字节数组
     */
    public static byte[] templateToPdfBytes(String templateName, Map<String, Object> data) {
        requireNotBlank(templateName, "模板名称不能为空");
        requireNotNull(data, "模板数据不能为空");
        return writeToBytes(document -> writeKeyValueContent(document, templateName, data));
    }

    /**
     * 添加图片印章到已有 PDF。
     *
     * @param source 源 PDF
     * @param target 目标 PDF
     * @param options 印章配置
     */
    public static void addSealImage(Path source, Path target, SealOptions options) {
        requireNotNull(options, "印章配置不能为空");
        stampPages(source, target, Set.of(options.pageNo()), (reader, stamper, pageNo) -> {
            Image image = Image.getInstance(options.imagePath().toString());
            image.scaleAbsolute(options.width(), options.height());
            image.setAbsolutePosition(options.x(), options.y());
            stamper.getOverContent(pageNo).addImage(image);
        });
    }

    /**
     * 添加签名图片到已有 PDF。
     *
     * @param source 源 PDF
     * @param target 目标 PDF
     * @param options 图片配置
     */
    public static void addSignatureImage(Path source, Path target, SealOptions options) {
        addSealImage(source, target, options);
    }

    /**
     * 对 PDF 进行数字签名。当前项目未引入证书和 BouncyCastle 等签名依赖，因此不提供真实签名实现。
     *
     * @param source 源 PDF
     * @param target 目标 PDF
     * @param options 签名配置
     */
    public static void sign(Path source, Path target, SignOptions options) {
        requireReadablePdf(source);
        requireOutputPath(target);
        requireNotNull(options, "签名配置不能为空");
        throw new UnsupportedOperationException("真实数字签名需要证书、摘要算法和签名 Provider，当前纯 JDK + OpenPDF 核心项目不实现");
    }

    /**
     * 验证 PDF 数字签名。当前项目未引入签名验证依赖，因此不提供真实验证实现。
     *
     * @param pdfPath PDF 文件路径
     * @return 是否验证通过
     */
    public static boolean verifySignature(Path pdfPath) {
        requireReadablePdf(pdfPath);
        throw new UnsupportedOperationException("真实数字签名验证需要证书链和签名 Provider，当前纯 JDK + OpenPDF 核心项目不实现");
    }

    /**
     * 获取 PDF 签名信息。当前返回空列表。
     *
     * @param pdfPath PDF 文件路径
     * @return 签名信息列表
     */
    public static java.util.List<String> getSignatures(Path pdfPath) {
        requireReadablePdf(pdfPath);
        return java.util.List.of();
    }

    /**
     * 判断 PDF 是否包含签名。
     *
     * @param pdfPath PDF 文件路径
     * @return 是否包含签名
     */
    public static boolean hasSignature(Path pdfPath) {
        return !getSignatures(pdfPath).isEmpty();
    }

    /**
     * 默认压缩 PDF。
     *
     * @param source 源 PDF
     * @param target 目标 PDF
     */
    public static void compress(Path source, Path target) {
        compress(source, target, new CompressOptions(true, false));
    }

    /**
     * 按配置压缩 PDF。
     *
     * @param source 源 PDF
     * @param target 目标 PDF
     * @param options 压缩配置
     */
    public static void compress(Path source, Path target, CompressOptions options) {
        requireReadablePdf(source);
        requireOutputPath(target);
        requireNotNull(options, "压缩配置不能为空");

        try (PdfReader reader = new PdfReader(source.toString());
             OutputStream out = Files.newOutputStream(target)) {

            Rectangle pageSize = reader.getNumberOfPages() > 0
                    ? reader.getPageSizeWithRotation(1)
                    : PageSize.A4;

            Document document = new Document(pageSize);

            try {
                if (!options.removeMetadata()) {
                    applyMetadata(document, reader.getInfo());
                }

                PdfCopy copy = new PdfCopy(document, out);

                if (options.fullCompression()) {
                    copy.setFullCompression();
                }

                document.open();

                for (int page = 1; page <= reader.getNumberOfPages(); page++) {
                    copy.addPage(copy.getImportedPage(reader, page));
                }

                copy.freeReader(reader);
            } finally {
                if (document.isOpen()) {
                    document.close();
                }
            }
        } catch (Exception e) {
            throw wrapPdfException("压缩 PDF 失败", e);
        }
    }

    /**
     * 压缩 PDF 图片。当前不解析并重采样图片，仅执行 PDF 全压缩。
     *
     * @param source 源 PDF
     * @param target 目标 PDF
     * @param quality 图片质量，范围 0 到 1
     */
    public static void compressImages(Path source, Path target, float quality) {
        if (quality <= 0 || quality > 1) {
            throw new IllegalArgumentException("图片质量必须在 0 到 1 之间");
        }
        compress(source, target);
    }

    /**
     * 移除未使用对象。当前通过重写 PDF 间接处理。
     *
     * @param source 源 PDF
     * @param target 目标 PDF
     */
    public static void removeUnusedObjects(Path source, Path target) {
        compress(source, target);
    }

    /**
     * 优化 PDF。
     *
     * @param source 源 PDF
     * @param target 目标 PDF
     */
    public static void optimize(Path source, Path target) {
        compress(source, target, new CompressOptions(true, true));
    }

    /**
     * 线性化 PDF。OpenPDF 核心不提供可靠线性化能力。
     *
     * @param source 源 PDF
     * @param target 目标 PDF
     */
    public static void linearize(Path source, Path target) {
        requireReadablePdf(source);
        requireOutputPath(target);
        throw new UnsupportedOperationException("PDF 线性化建议使用 qpdf 等专业工具处理，OpenPDF 核心不提供可靠实现");
    }

    /**
     * 获取文件大小。
     *
     * @param pdfPath PDF 文件路径
     * @return 文件大小字节数
     */
    public static long getFileSize(Path pdfPath) {
        requireReadableFile(pdfPath, "文件不存在或不可读");
        try {
            return Files.size(pdfPath);
        } catch (IOException e) {
            throw wrapPdfException("获取文件大小失败", e);
        }
    }

    /**
     * 估算压缩比例。该方法不实际压缩，仅返回 0 作为保守值。
     *
     * @param source 源 PDF
     * @param options 压缩配置
     * @return 估算压缩比例
     */
    public static double estimateCompressRatio(Path source, CompressOptions options) {
        requireReadablePdf(source);
        requireNotNull(options, "压缩配置不能为空");
        return 0D;
    }

    /**
     * 校验 PDF 是否可读。
     *
     * @param pdfPath PDF 文件路径
     */
    public static void validatePdf(Path pdfPath) {
        requireReadablePdf(pdfPath);
        try (PdfReader ignored = new PdfReader(pdfPath.toString())) {
            // 可成功打开即认为基础校验通过。
        } catch (IOException e) {
            throw wrapPdfException("PDF 文件不可读或格式错误", e);
        }
    }

    /**
     * 校验页码范围。
     *
     * @param pdfPath PDF 文件路径
     * @param fromPage 起始页
     * @param toPage 结束页
     */
    public static void validatePageRange(Path pdfPath, int fromPage, int toPage) {
        requireReadablePdf(pdfPath);
        try (PdfReader reader = new PdfReader(pdfPath.toString())) {
            validatePageRange(reader, fromPage, toPage);
        } catch (IOException e) {
            throw wrapPdfException("校验页码范围失败", e);
        }
    }

    /**
     * 校验 PDF 密码，不通过时抛出异常。
     *
     * @param pdfPath PDF 文件路径
     * @param password 密码
     */
    public static void validatePassword(Path pdfPath, String password) {
        if (!checkPassword(pdfPath, password)) {
            throw new IllegalArgumentException("PDF 密码错误或文件不可打开");
        }
    }

    /**
     * 校验输出路径。
     *
     * @param outputPath 输出路径
     */
    public static void validateOutputPath(Path outputPath) {
        requireOutputPath(outputPath);
    }

    /**
     * 要求路径必须是 PDF。
     *
     * @param pdfPath PDF 文件路径
     */
    public static void requirePdf(Path pdfPath) {
        requireReadablePdf(pdfPath);
    }

    /**
     * 要求路径存在。
     *
     * @param path 路径
     */
    public static void requireExists(Path path) {
        if (path == null || !Files.exists(path)) {
            throw new IllegalArgumentException("路径不存在");
        }
    }

    /**
     * 要求路径可读。
     *
     * @param path 路径
     */
    public static void requireReadable(Path path) {
        requireReadableFile(path, "文件不存在或不可读");
    }

    /**
     * 要求路径可写。
     *
     * @param path 路径
     */
    public static void requireWritable(Path path) {
        requireOutputPath(path);
    }

    /**
     * 安全关闭资源。
     *
     * @param closeable 可关闭资源
     */
    public static void safeClose(Closeable closeable) {
        if (closeable != null) {
            try {
                closeable.close();
            } catch (IOException ignored) {
                // 工具类静默关闭资源，避免覆盖主流程异常。
            }
        }
    }

    /**
     * 包装 PDF 异常。
     *
     * @param e 原始异常
     * @return PDF 运行时异常
     */
    public static PDFException wrapPdfException(Exception e) {
        return wrapPdfException("PDF 处理失败", e);
    }

    /**
     * 包装 PDF 异常。
     *
     * @param message 异常消息
     * @param e 原始异常
     * @return PDF 运行时异常
     */
    public static PDFException wrapPdfException(String message, Exception e) {
        if (e instanceof PDFException pdfException) {
            return pdfException;
        }
        return new PDFException(message, e);
    }

    /**
     * 渲染指定页为图片。当前未引入 openpdf-renderer 模块，因此不提供实现。
     *
     * @param pdfPath PDF 文件路径
     * @param pageNo 页码
     * @param outputPath 输出图片路径
     */
    public static void renderPageToImage(Path pdfPath, int pageNo, Path outputPath) {
        requireReadablePdf(pdfPath);
        requirePositive(pageNo, "页码必须大于 0");
        requireOutputPath(outputPath);
        throw new UnsupportedOperationException("PDF 渲染为图片需要 openpdf-renderer 或其他渲染库，当前核心项目不实现");
    }

    /**
     * 渲染首页为图片。
     *
     * @param pdfPath PDF 文件路径
     * @param outputPath 输出图片路径
     */
    public static void renderFirstPageToImage(Path pdfPath, Path outputPath) {
        renderPageToImage(pdfPath, 1, outputPath);
    }

    /**
     * 批量渲染 PDF 页面为图片。
     *
     * @param pdfPath PDF 文件路径
     * @param outputDir 输出目录
     */
    public static void renderPagesToImages(Path pdfPath, Path outputDir) {
        requireReadablePdf(pdfPath);
        requireDirectory(outputDir);
        throw new UnsupportedOperationException("PDF 批量渲染为图片需要 openpdf-renderer 或其他渲染库，当前核心项目不实现");
    }

    /**
     * 创建 PDF 缩略图。
     *
     * @param pdfPath PDF 文件路径
     * @param outputPath 输出图片路径
     */
    public static void createThumbnail(Path pdfPath, Path outputPath) {
        renderFirstPageToImage(pdfPath, outputPath);
    }

    /**
     * 创建 PDF 预览图。
     *
     * @param pdfPath PDF 文件路径
     * @param outputDir 输出目录
     * @param maxPages 最大页数
     */
    public static void createPreviewImages(Path pdfPath, Path outputDir, int maxPages) {
        requirePositive(maxPages, "最大页数必须大于 0");
        renderPagesToImages(pdfPath, outputDir);
    }

    /**
     * 快速创建简单 PDF。
     *
     * @param title 标题
     * @param content 内容
     * @param target 目标 PDF
     */
    public static void createSimplePdf(String title, String content, Path target) {
        requireNotBlank(title, "标题不能为空");
        requireNotNull(content, "内容不能为空");
        writeToFile(target, document -> {
            addTitle(document, title);
            addParagraph(document, content);
        });
    }

    /**
     * 根据段落创建文本 PDF。
     *
     * @param paragraphs 段落集合
     * @param target 目标 PDF
     */
    public static void createTextPdf(java.util.List<String> paragraphs, Path target) {
        requireNotEmpty(paragraphs, "段落集合不能为空");
        writeToFile(target, document -> {
            for (String paragraph : paragraphs) {
                addParagraph(document, paragraph == null ? "" : paragraph);
            }
        });
    }

    /**
     * 创建表格 PDF。
     *
     * @param title 标题
     * @param headers 表头
     * @param rows 数据行
     * @param target 目标 PDF
     */
    public static void createTablePdf(String title, java.util.List<String> headers, java.util.List<java.util.List<String>> rows, Path target) {
        requireNotBlank(title, "标题不能为空");
        writeToFile(target, document -> {
            addTitle(document, title);
            addTable(document, headers, rows);
        });
    }

    /**
     * 根据图片列表创建 PDF。
     *
     * @param images 图片路径集合
     * @param target 目标 PDF
     */
    public static void createImagePdf(java.util.List<Path> images, Path target) {
        requireNotEmpty(images, "图片路径集合不能为空");
        writeToFile(target, document -> {
            for (int i = 0; i < images.size(); i++) {
                addImageFitPage(document, images.get(i));
                if (i < images.size() - 1) {
                    document.newPage();
                }
            }
        });
    }

    /**
     * 创建带水印 PDF。
     *
     * @param source 源 PDF
     * @param target 目标 PDF
     * @param watermark 水印文本
     */
    public static void createWatermarkedPdf(Path source, Path target, String watermark) {
        addTextWatermark(source, target, watermark);
    }

    /**
     * 创建加密 PDF。
     *
     * @param source 源 PDF
     * @param target 目标 PDF
     * @param password 密码
     */
    public static void createProtectedPdf(Path source, Path target, String password) {
        encrypt(source, target, password);
    }

    /**
     * 创建合并 PDF。
     *
     * @param files PDF 文件集合
     * @param target 目标 PDF
     */
    public static void createMergedPdf(java.util.List<Path> files, Path target) {
        merge(files, target);
    }

    /**
     * 创建盖章 PDF。
     *
     * @param source 源 PDF
     * @param target 目标 PDF
     * @param options 印章配置
     */
    public static void createStampedPdf(Path source, Path target, SealOptions options) {
        addSealImage(source, target, options);
    }

    /**
     * 创建预览资源。当前返回空列表，实际图片预览需引入渲染模块。
     *
     * @param source 源 PDF
     * @param outputDir 输出目录
     * @return 预览资源路径集合
     */
    public static java.util.List<Path> createPreview(Path source, Path outputDir) {
        requireReadablePdf(source);
        requireDirectory(outputDir);
        return java.util.List.of();
    }

    private interface PageStampHandler {
        void handle(PdfReader reader, PdfStamper stamper, int pageNo) throws Exception;
    }

    private static void addPdfList(Document document, java.util.List<String> items, boolean ordered) {
        Objects.requireNonNull(document, "PDF 文档对象不能为空");
        requireNotEmpty(items, "列表项不能为空");
        try {
            org.openpdf.text.List pdfList = new org.openpdf.text.List(ordered);
            for (String item : items) {
                pdfList.add(item == null ? "" : item);
            }
            document.add(pdfList);
        } catch (DocumentException e) {
            throw wrapPdfException("添加列表失败", e);
        }
    }

    private static void stampEachPage(Path source, Path target, PageStampHandler handler) {
        stampPages(source, target, null, handler);
    }

    private static void stampPages(Path source, Path target, Set<Integer> pages, PageStampHandler handler) {
        requireReadablePdf(source);
        requireOutputPath(target);
        Objects.requireNonNull(handler, "页面处理器不能为空");
        try (PdfReader reader = new PdfReader(source.toString()); OutputStream out = Files.newOutputStream(target)) {
            if (pages != null) {
                for (Integer page : pages) {
                    validatePageNo(reader, page);
                }
            }
            try (PdfStamper stamper = new PdfStamper(reader, out)) {
                for (int pageNo = 1; pageNo <= reader.getNumberOfPages(); pageNo++) {
                    if (pages == null || pages.contains(pageNo)) {
                        handler.handle(reader, stamper, pageNo);
                    }
                }
            }
        } catch (Exception e) {
            throw wrapPdfException("处理 PDF 页面失败", e);
        }
    }

    private static void drawTextWatermark(PdfReader reader, PdfContentByte canvas, int pageNo, WatermarkOptions options) {
        Rectangle pageSize = reader.getPageSizeWithRotation(pageNo);
        canvas.saveState();
        try {
            canvas.setGState(createOpacityState(options.opacity()));
            if (options.tiled()) {
                for (float x = 80; x < pageSize.getWidth(); x += 180) {
                    for (float y = 80; y < pageSize.getHeight(); y += 140) {
                        ColumnText.showTextAligned(canvas, Element.ALIGN_CENTER, new Phrase(options.text(), options.font()), x, y, options.rotation());
                    }
                }
            } else {
                float x = options.x() == null ? pageSize.getWidth() / 2 : options.x();
                float y = options.y() == null ? pageSize.getHeight() / 2 : options.y();
                ColumnText.showTextAligned(canvas, Element.ALIGN_CENTER, new Phrase(options.text(), options.font()), x, y, options.rotation());
            }
        } finally {
            canvas.restoreState();
        }
    }

    private static void drawImageWatermark(PdfReader reader, PdfContentByte canvas, int pageNo, WatermarkOptions options) throws Exception {
        Rectangle pageSize = reader.getPageSizeWithRotation(pageNo);
        Image image = Image.getInstance(options.imagePath().toString());
        image.scaleToFit(160, 160);
        float x = options.x() == null ? (pageSize.getWidth() - image.getScaledWidth()) / 2 : options.x();
        float y = options.y() == null ? (pageSize.getHeight() - image.getScaledHeight()) / 2 : options.y();
        image.setAbsolutePosition(x, y);
        canvas.saveState();
        try {
            canvas.setGState(createOpacityState(options.opacity()));
            canvas.addImage(image);
        } finally {
            canvas.restoreState();
        }
    }

    private static org.openpdf.text.pdf.PdfGState createOpacityState(float opacity) {
        org.openpdf.text.pdf.PdfGState state = new org.openpdf.text.pdf.PdfGState();
        state.setFillOpacity(opacity);
        state.setStrokeOpacity(opacity);
        return state;
    }

    private static void renderKeyValuePdf(String title, Map<String, Object> data, Path target) {
        requireNotBlank(title, "标题不能为空");
        requireNotNull(data, "数据不能为空");
        writeToFile(target, document -> writeKeyValueContent(document, title, data));
    }

    private static void writeKeyValueContent(Document document, String title, Map<String, Object> data) {
        addTitle(document, title);
        java.util.List<String> headers = java.util.List.of("字段", "值");
        java.util.List<java.util.List<String>> rows = new ArrayList<>();
        for (Map.Entry<String, Object> entry : data.entrySet()) {
            rows.add(java.util.List.of(entry.getKey(), entry.getValue() == null ? "" : String.valueOf(entry.getValue())));
        }
        addTable(document, headers, rows);
    }

    private static String htmlToPlainText(String html) {
        String withBreaks = HTML_BREAK_PATTERN.matcher(html).replaceAll(System.lineSeparator());
        String noTags = HTML_TAG_PATTERN.matcher(withBreaks).replaceAll("");
        return noTags.replace("&nbsp;", " ").replace("&lt;", "<").replace("&gt;", ">").replace("&amp;", "&").trim();
    }

    private static void applyMetadata(Document document, MetadataOptions options) {
        if (options == null) {
            return;
        }
        if (!isBlank(options.title())) {
            document.addTitle(options.title());
        }
        if (!isBlank(options.author())) {
            document.addAuthor(options.author());
        }
        if (!isBlank(options.subject())) {
            document.addSubject(options.subject());
        }
        if (!isBlank(options.keywords())) {
            document.addKeywords(options.keywords());
        }
        if (!isBlank(options.creator())) {
            document.addCreator(options.creator());
        }
    }

    private static void putIfNotBlank(Map<String, String> map, String key, String value) {
        if (value == null) {
            return;
        }
        if (value.isBlank()) {
            map.remove(key);
        } else {
            map.put(key, value);
        }
    }

    private static void validatePageRange(PdfReader reader, int fromPage, int toPage) {
        requirePositive(fromPage, "起始页必须大于 0");
        requirePositive(toPage, "结束页必须大于 0");
        if (fromPage > toPage) {
            throw new IllegalArgumentException("起始页不能大于结束页");
        }
        validatePageNo(reader, fromPage);
        validatePageNo(reader, toPage);
    }

    private static void validatePageNo(PdfReader reader, int pageNo) {
        requirePositive(pageNo, "页码必须大于 0");
        if (pageNo > reader.getNumberOfPages()) {
            throw new IllegalArgumentException("页码超过 PDF 总页数");
        }
    }

    private static void requireReadablePdf(Path path) {
        requireReadableFile(path, "PDF 文件不存在或不可读");
        if (!isPdf(path)) {
            throw new IllegalArgumentException("文件不是有效 PDF");
        }
    }

    private static void requireReadableFile(Path path, String message) {
        if (path == null || !Files.isRegularFile(path) || !Files.isReadable(path)) {
            throw new IllegalArgumentException(message);
        }
    }

    private static void requireOutputPath(Path outputPath) {
        Objects.requireNonNull(outputPath, "输出路径不能为空");
        Path parent = outputPath.toAbsolutePath().getParent();
        try {
            if (parent != null) {
                Files.createDirectories(parent);
            }
            if (Files.exists(outputPath) && !Files.isWritable(outputPath)) {
                throw new IllegalArgumentException("输出路径不可写");
            }
        } catch (IOException e) {
            throw wrapPdfException("创建输出目录失败", e);
        }
    }

    private static void requireDirectory(Path dir) {
        Objects.requireNonNull(dir, "目录路径不能为空");
        try {
            Files.createDirectories(dir);
        } catch (IOException e) {
            throw wrapPdfException("创建目录失败", e);
        }
        if (!Files.isDirectory(dir) || !Files.isWritable(dir)) {
            throw new IllegalArgumentException("目录不存在或不可写");
        }
    }

    private static void requireNotNull(Object value, String message) {
        if (value == null) {
            throw new IllegalArgumentException(message);
        }
    }

    private static void requireNotBlank(String value, String message) {
        if (isBlank(value)) {
            throw new IllegalArgumentException(message);
        }
    }

    private static void requireNotEmpty(Collection<?> values, String message) {
        if (values == null || values.isEmpty()) {
            throw new IllegalArgumentException(message);
        }
    }

    private static void requirePositive(int value, String message) {
        if (value <= 0) {
            throw new IllegalArgumentException(message);
        }
    }

    private static void requirePositive(float value, String message) {
        if (Float.isNaN(value) || Float.isInfinite(value) || value <= 0) {
            throw new IllegalArgumentException(message);
        }
    }

    private static void requireNonNegative(float value, String message) {
        if (Float.isNaN(value) || Float.isInfinite(value) || value < 0) {
            throw new IllegalArgumentException(message);
        }
    }

    private static boolean isBlank(String value) {
        return value == null || value.isBlank();
    }

    private static String randomPassword() {
        byte[] bytes = new byte[24];
        SECURE_RANDOM.nextBytes(bytes);
        StringBuilder builder = new StringBuilder(bytes.length * 2);
        for (byte b : bytes) {
            builder.append(String.format("%02x", b));
        }
        return builder.toString();
    }

    private static void moveReplace(Path source, Path target) {
        try {
            Files.move(source, target, java.nio.file.StandardCopyOption.REPLACE_EXISTING);
        } catch (IOException e) {
            throw wrapPdfException("移动文件失败", e);
        }
    }
}
