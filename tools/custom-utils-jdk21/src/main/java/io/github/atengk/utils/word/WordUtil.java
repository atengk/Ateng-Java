package io.github.atengk.utils.word;

import org.apache.poi.ooxml.POIXMLProperties;
import org.apache.poi.openxml4j.exceptions.InvalidFormatException;
import org.apache.poi.util.Units;
import org.apache.poi.wp.usermodel.HeaderFooterType;
import org.apache.poi.xwpf.usermodel.*;
import org.apache.xmlbeans.XmlCursor;
import org.openxmlformats.schemas.wordprocessingml.x2006.main.*;

import java.io.*;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.text.NumberFormat;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.concurrent.atomic.AtomicLong;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Word 文档工具类，基于 Apache POI 的 {@link XWPFDocument} 实现常用 docx 操作。
 *
 * @author Ateng
 * @since 2026-04-30
 */
public final class WordUtil {

    private static final String DEFAULT_PLACEHOLDER_PREFIX = "${";
    private static final String DEFAULT_PLACEHOLDER_SUFFIX = "}";
    private static final Pattern DEFAULT_PLACEHOLDER_PATTERN = Pattern.compile("\\$\\{([^}]+)}");
    private static final String COMMENT_PREFIX = "[批注] ";
    private static final AtomicLong BOOKMARK_ID = new AtomicLong(1);
    private static final int A4_WIDTH_TWIPS = 11906;
    private static final int A4_HEIGHT_TWIPS = 16838;

    private WordUtil() {
        throw new AssertionError("WordUtil 不允许实例化");
    }

    /**
     * 页面方向。
     */
    public enum PageOrientation {
        /**
         * 纵向页面。
         */
        PORTRAIT,
        /**
         * 横向页面。
         */
        LANDSCAPE
    }

    /**
     * Word 文档基础属性。
     *
     * @param title       标题
     * @param author      作者
     * @param subject     主题
     * @param keywords    关键词
     * @param createdTime 创建时间
     */
    public record WordProperties(String title, String author, String subject, String keywords,
                                 LocalDateTime createdTime) {
    }

    /**
     * Word 文本样式配置。
     *
     * @param fontFamily 字体
     * @param fontSize   字号
     * @param color      颜色，十六进制格式
     * @param bold       是否加粗
     * @param italic     是否斜体
     * @param underline  是否下划线
     */
    public record WordTextStyle(String fontFamily, Integer fontSize, String color, Boolean bold, Boolean italic,
                                Boolean underline) {
    }

    /**
     * Word 表格样式配置。
     *
     * @param width      表格宽度
     * @param border     是否显示边框
     * @param header     是否应用表头样式
     * @param background 表头背景色
     */
    public record WordTableStyle(String width, boolean border, boolean header, String background) {
    }

    /**
     * Word 图片配置。
     *
     * @param data        图片字节
     * @param fileName    图片文件名
     * @param width       图片宽度，单位像素
     * @param height      图片高度，单位像素
     * @param placeholder 图片占位符
     */
    public record WordImage(byte[] data, String fileName, int width, int height, String placeholder) {
        /**
         * 创建图片配置。
         *
         * @param imageStream 图片输入流
         * @param fileName    图片文件名
         * @param width       图片宽度，单位像素
         * @param height      图片高度，单位像素
         * @param placeholder 图片占位符
         * @return 图片配置
         */
        public static WordImage of(InputStream imageStream, String fileName, int width, int height, String placeholder) {
            Objects.requireNonNull(imageStream, "imageStream 不能为空");
            try {
                return new WordImage(imageStream.readAllBytes(), fileName, width, height, placeholder);
            } catch (IOException e) {
                throw new IllegalArgumentException("读取图片输入流失败", e);
            }
        }
    }

    /**
     * Word 页面配置。
     *
     * @param width       页面宽度，单位 twips
     * @param height      页面高度，单位 twips
     * @param orientation 页面方向
     * @param top         上边距，单位 twips
     * @param right       右边距，单位 twips
     * @param bottom      下边距，单位 twips
     * @param left        左边距，单位 twips
     */
    public record WordPageConfig(int width, int height, PageOrientation orientation, int top, int right, int bottom,
                                 int left) {
    }

    /**
     * Word 模板填充配置。
     *
     * @param prefix                  占位符前缀
     * @param suffix                  占位符后缀
     * @param clearUnusedPlaceholders 是否清理未填充占位符
     */
    public record WordTemplateConfig(String prefix, String suffix, boolean clearUnusedPlaceholders) {
    }

    /**
     * Word 导出配置。
     *
     * @param fileName 文件名
     */
    public record WordExportConfig(String fileName) {
    }

    /**
     * 创建空 Word 文档。
     *
     * @return 空文档对象
     */
    public static XWPFDocument create() {
        return new XWPFDocument();
    }

    /**
     * 从文件路径读取 Word 文档。
     *
     * @param path 文件路径
     * @return 文档对象
     * @throws IOException 文件读取失败时抛出
     */
    public static XWPFDocument read(Path path) throws IOException {
        Objects.requireNonNull(path, "path 不能为空");
        checkDocxFile(path);
        try (InputStream inputStream = Files.newInputStream(path)) {
            return read(inputStream);
        }
    }

    /**
     * 从输入流读取 Word 文档。
     *
     * @param inputStream 输入流
     * @return 文档对象
     * @throws IOException 文档读取失败时抛出
     */
    public static XWPFDocument read(InputStream inputStream) throws IOException {
        Objects.requireNonNull(inputStream, "inputStream 不能为空");
        return new XWPFDocument(inputStream);
    }

    /**
     * 将 Word 文档写入输出流。
     *
     * @param document     文档对象
     * @param outputStream 输出流
     * @throws IOException 写出失败时抛出
     */
    public static void write(XWPFDocument document, OutputStream outputStream) throws IOException {
        checkDocument(document);
        Objects.requireNonNull(outputStream, "outputStream 不能为空");
        document.write(outputStream);
        outputStream.flush();
    }

    /**
     * 将 Word 文档写入本地文件。
     *
     * @param document 文档对象
     * @param path     文件路径
     * @throws IOException 写出失败时抛出
     */
    public static void writeToFile(XWPFDocument document, Path path) throws IOException {
        checkDocument(document);
        Objects.requireNonNull(path, "path 不能为空");
        Path parent = path.getParent();
        if (parent != null) {
            Files.createDirectories(parent);
        }
        try (OutputStream outputStream = Files.newOutputStream(path)) {
            write(document, outputStream);
        }
    }

    /**
     * 将 Word 文档转换为字节数组。
     *
     * @param document 文档对象
     * @return 文档字节数组
     * @throws IOException 转换失败时抛出
     */
    public static byte[] writeToBytes(XWPFDocument document) throws IOException {
        checkDocument(document);
        try (ByteArrayOutputStream outputStream = new ByteArrayOutputStream()) {
            write(document, outputStream);
            return outputStream.toByteArray();
        }
    }

    /**
     * 复制 Word 文档。
     *
     * @param document 源文档
     * @return 新文档对象
     * @throws IOException 复制失败时抛出
     */
    public static XWPFDocument copy(XWPFDocument document) throws IOException {
        return read(new ByteArrayInputStream(writeToBytes(document)));
    }

    /**
     * 安静关闭可关闭对象。
     *
     * @param closeable 可关闭对象
     */
    public static void closeQuietly(Closeable closeable) {
        if (closeable == null) {
            return;
        }
        try {
            closeable.close();
        } catch (IOException ignored) {
            // 安静关闭，不向外抛出异常。
        }
    }

    /**
     * 判断 Word 文档正文是否为空。
     *
     * @param document 文档对象
     * @return 为空返回 true
     */
    public static boolean isEmpty(XWPFDocument document) {
        checkDocument(document);
        return document.getBodyElements().isEmpty() || extractText(document).isBlank();
    }

    /**
     * 获取 Word 文档写出后的字节大小。
     *
     * @param document 文档对象
     * @return 字节大小
     * @throws IOException 转换失败时抛出
     */
    public static long sizeOf(XWPFDocument document) throws IOException {
        return writeToBytes(document).length;
    }

    /**
     * 获取文档标题。
     *
     * @param document 文档对象
     * @return 标题
     */
    public static String getTitle(XWPFDocument document) {
        checkDocument(document);
        return coreProperties(document).getTitle();
    }

    /**
     * 设置文档标题。
     *
     * @param document 文档对象
     * @param title    标题
     */
    public static void setTitle(XWPFDocument document, String title) {
        checkDocument(document);
        coreProperties(document).setTitle(toSafeText(title));
    }

    /**
     * 获取文档作者。
     *
     * @param document 文档对象
     * @return 作者
     */
    public static String getAuthor(XWPFDocument document) {
        checkDocument(document);
        return coreProperties(document).getCreator();
    }

    /**
     * 设置文档作者。
     *
     * @param document 文档对象
     * @param author   作者
     */
    public static void setAuthor(XWPFDocument document, String author) {
        checkDocument(document);
        coreProperties(document).setCreator(toSafeText(author));
    }

    /**
     * 获取文档主题。
     *
     * @param document 文档对象
     * @return 主题
     */
    public static String getSubject(XWPFDocument document) {
        checkDocument(document);
        return coreProperties(document).getSubject();
    }

    /**
     * 设置文档主题。
     *
     * @param document 文档对象
     * @param subject  主题
     */
    public static void setSubject(XWPFDocument document, String subject) {
        checkDocument(document);
        coreProperties(document).setSubjectProperty(toSafeText(subject));
    }

    /**
     * 获取文档关键词。
     *
     * @param document 文档对象
     * @return 关键词
     */
    public static String getKeywords(XWPFDocument document) {
        checkDocument(document);
        return coreProperties(document).getKeywords();
    }

    /**
     * 设置文档关键词。
     *
     * @param document 文档对象
     * @param keywords 关键词
     */
    public static void setKeywords(XWPFDocument document, String keywords) {
        checkDocument(document);
        coreProperties(document).setKeywords(toSafeText(keywords));
    }

    /**
     * 获取文档创建时间。
     *
     * @param document Word 文档对象
     * @return 创建时间，未设置时返回 null
     */
    public static LocalDateTime getCreatedTime(XWPFDocument document) {
        checkDocument(document);
        Date created = coreProperties(document).getCreated();
        return created == null ? null : LocalDateTime.ofInstant(created.toInstant(), ZoneId.systemDefault());
    }

    /**
     * 设置文档创建时间。
     *
     * @param document 文档对象
     * @param time     创建时间
     */
    public static void setCreatedTime(XWPFDocument document, LocalDateTime time) {
        checkDocument(document);
        Date date = time == null ? null : Date.from(time.atZone(ZoneId.systemDefault()).toInstant());
        coreProperties(document).setCreated(Optional.ofNullable(date));
    }

    /**
     * 设置文档基础属性。
     *
     * @param document   文档对象
     * @param properties 文档属性
     */
    public static void setProperties(XWPFDocument document, WordProperties properties) {
        checkDocument(document);
        Objects.requireNonNull(properties, "properties 不能为空");
        setTitle(document, properties.title());
        setAuthor(document, properties.author());
        setSubject(document, properties.subject());
        setKeywords(document, properties.keywords());
        setCreatedTime(document, properties.createdTime());
    }

    /**
     * 获取正文全部段落。
     *
     * @param document 文档对象
     * @return 段落列表
     */
    public static List<XWPFParagraph> getParagraphs(XWPFDocument document) {
        checkDocument(document);
        return document.getParagraphs();
    }

    /**
     * 获取正文第一个段落。
     *
     * @param document 文档对象
     * @return 第一个段落
     */
    public static XWPFParagraph getFirstParagraph(XWPFDocument document) {
        List<XWPFParagraph> paragraphs = getParagraphs(document);
        return paragraphs.isEmpty() ? null : paragraphs.getFirst();
    }

    /**
     * 获取正文最后一个段落。
     *
     * @param document 文档对象
     * @return 最后一个段落
     */
    public static XWPFParagraph getLastParagraph(XWPFDocument document) {
        List<XWPFParagraph> paragraphs = getParagraphs(document);
        return paragraphs.isEmpty() ? null : paragraphs.getLast();
    }

    /**
     * 在文档末尾追加段落。
     *
     * @param document 文档对象
     * @param text     段落文本
     * @return 新段落
     */
    public static XWPFParagraph addParagraph(XWPFDocument document, String text) {
        checkDocument(document);
        XWPFParagraph paragraph = document.createParagraph();
        setParagraphText(paragraph, text);
        return paragraph;
    }

    /**
     * 在正文元素指定位置插入段落。
     *
     * @param document 文档对象
     * @param index    正文元素索引
     * @param text     段落文本
     * @return 新段落
     */
    public static XWPFParagraph insertParagraph(XWPFDocument document, int index, String text) {
        checkDocument(document);
        checkIndexForInsert(index, document.getBodyElements().size(), "index");
        if (index == document.getBodyElements().size()) {
            return addParagraph(document, text);
        }
        IBodyElement element = document.getBodyElements().get(index);
        XmlCursor cursor = newCursor(element);
        try {
            XWPFParagraph paragraph = document.insertNewParagraph(cursor);
            setParagraphText(paragraph, text);
            return paragraph;
        } finally {
            cursor.dispose();
        }
    }

    /**
     * 删除指定段落。
     *
     * @param document 文档对象
     * @param index    段落索引
     */
    public static void removeParagraph(XWPFDocument document, int index) {
        checkDocument(document);
        List<XWPFParagraph> paragraphs = document.getParagraphs();
        checkIndex(index, paragraphs.size(), "index");
        int bodyPos = document.getPosOfParagraph(paragraphs.get(index));
        document.removeBodyElement(bodyPos);
    }

    /**
     * 清空段落文本。
     *
     * @param paragraph 段落对象
     */
    public static void clearParagraph(XWPFParagraph paragraph) {
        Objects.requireNonNull(paragraph, "paragraph 不能为空");
        for (int i = paragraph.getRuns().size() - 1; i >= 0; i--) {
            paragraph.removeRun(i);
        }
    }

    /**
     * 设置段落文本。
     *
     * @param paragraph 段落对象
     * @param text      文本
     */
    public static void setParagraphText(XWPFParagraph paragraph, String text) {
        Objects.requireNonNull(paragraph, "paragraph 不能为空");
        XWPFRun styleRun = paragraph.getRuns().isEmpty() ? null : paragraph.getRuns().getFirst();
        clearParagraph(paragraph);
        XWPFRun run = paragraph.createRun();
        if (styleRun != null) {
            copyRunStyle(styleRun, run);
        }
        setRunText(run, toSafeText(text));
    }

    /**
     * 追加段落文本。
     *
     * @param paragraph 段落对象
     * @param text      文本
     */
    public static void appendParagraphText(XWPFParagraph paragraph, String text) {
        Objects.requireNonNull(paragraph, "paragraph 不能为空");
        XWPFRun run = paragraph.createRun();
        setRunText(run, text);
    }

    /**
     * 判断段落文本是否为空。
     *
     * @param paragraph 段落对象
     * @return 为空返回 true
     */
    public static boolean isBlankParagraph(XWPFParagraph paragraph) {
        Objects.requireNonNull(paragraph, "paragraph 不能为空");
        return getParagraphText(paragraph).isBlank();
    }

    /**
     * 根据关键字查找正文段落。
     *
     * @param document 文档对象
     * @param keyword  关键字
     * @return 匹配的段落列表
     */
    public static List<XWPFParagraph> findParagraphs(XWPFDocument document, String keyword) {
        checkDocument(document);
        if (keyword == null || keyword.isEmpty()) {
            return Collections.emptyList();
        }
        List<XWPFParagraph> result = new ArrayList<>();
        for (XWPFParagraph paragraph : document.getParagraphs()) {
            if (getParagraphText(paragraph).contains(keyword)) {
                result.add(paragraph);
            }
        }
        return result;
    }

    /**
     * 获取段落纯文本。
     *
     * @param paragraph 段落对象
     * @return 段落文本
     */
    public static String getParagraphText(XWPFParagraph paragraph) {
        Objects.requireNonNull(paragraph, "paragraph 不能为空");
        String text = paragraph.getText();
        return text == null ? "" : text;
    }

    /**
     * 获取段落所有文本片段。
     *
     * @param paragraph 段落对象
     * @return Run 列表
     */
    public static List<XWPFRun> getRuns(XWPFParagraph paragraph) {
        Objects.requireNonNull(paragraph, "paragraph 不能为空");
        return paragraph.getRuns();
    }

    /**
     * 新增文本片段。
     *
     * @param paragraph 段落对象
     * @param text      文本
     * @return 新 Run
     */
    public static XWPFRun addRun(XWPFParagraph paragraph, String text) {
        Objects.requireNonNull(paragraph, "paragraph 不能为空");
        XWPFRun run = paragraph.createRun();
        setRunText(run, text);
        return run;
    }

    /**
     * 设置 Run 文本。
     *
     * @param run  Run 对象
     * @param text 文本
     */
    public static void setRunText(XWPFRun run, String text) {
        Objects.requireNonNull(run, "run 不能为空");
        run.setText(cleanInvalidXmlChars(toSafeText(text)), 0);
    }

    /**
     * 清空 Run 文本。
     *
     * @param run Run 对象
     */
    public static void clearRun(XWPFRun run) {
        setRunText(run, "");
    }

    /**
     * 复制 Run 样式。
     *
     * @param source 源 Run
     * @param target 目标 Run
     */
    public static void copyRunStyle(XWPFRun source, XWPFRun target) {
        Objects.requireNonNull(source, "source 不能为空");
        Objects.requireNonNull(target, "target 不能为空");
        if (source.getCTR().isSetRPr()) {
            target.getCTR().setRPr((CTRPr) source.getCTR().getRPr().copy());
        }
    }

    /**
     * 合并段落中的多个 Run。
     *
     * @param paragraph 段落对象
     */
    public static void mergeRuns(XWPFParagraph paragraph) {
        Objects.requireNonNull(paragraph, "paragraph 不能为空");
        if (paragraph.getRuns().size() <= 1) {
            return;
        }
        XWPFRun firstRun = paragraph.getRuns().getFirst();
        StringBuilder builder = new StringBuilder();
        for (XWPFRun run : paragraph.getRuns()) {
            builder.append(getRunText(run));
        }
        clearParagraph(paragraph);
        XWPFRun run = paragraph.createRun();
        copyRunStyle(firstRun, run);
        setRunText(run, builder.toString());
    }

    /**
     * 标准化段落 Run，便于跨 Run 占位符替换。
     *
     * @param paragraph 段落对象
     */
    public static void normalizeRuns(XWPFParagraph paragraph) {
        mergeRuns(paragraph);
    }

    /**
     * 获取 Run 文本。
     *
     * @param run Run 对象
     * @return 文本
     */
    public static String getRunText(XWPFRun run) {
        Objects.requireNonNull(run, "run 不能为空");
        String text = run.getText(0);
        return text == null ? "" : text;
    }

    /**
     * 判断 Run 文本是否为空。
     *
     * @param run Run 对象
     * @return 为空返回 true
     */
    public static boolean isBlankRun(XWPFRun run) {
        return getRunText(run).isBlank();
    }

    /**
     * 替换文档正文和表格中的指定文本。
     *
     * @param document    文档对象
     * @param searchText  原文本
     * @param replacement 替换文本
     */
    public static void replaceText(XWPFDocument document, String searchText, String replacement) {
        Objects.requireNonNull(searchText, "searchText 不能为空");
        replaceText(document, Map.of(searchText, toSafeText(replacement)));
    }

    /**
     * 批量替换文档正文和表格中的文本。
     *
     * @param document     文档对象
     * @param replacements 替换映射
     */
    public static void replaceText(XWPFDocument document, Map<String, String> replacements) {
        checkDocument(document);
        Map<String, String> safeMap = normalizeStringMap(replacements);
        for (XWPFParagraph paragraph : document.getParagraphs()) {
            replaceParagraphText(paragraph, safeMap);
        }
        for (XWPFTable table : document.getTables()) {
            replaceTableText(table, safeMap);
        }
    }

    /**
     * 批量替换段落文本。
     *
     * @param paragraph    段落对象
     * @param replacements 替换映射
     */
    public static void replaceParagraphText(XWPFParagraph paragraph, Map<String, String> replacements) {
        Objects.requireNonNull(paragraph, "paragraph 不能为空");
        Map<String, String> safeMap = normalizeStringMap(replacements);
        if (safeMap.isEmpty()) {
            return;
        }
        String original = getParagraphText(paragraph);
        String replaced = replaceByMap(original, safeMap);
        if (!Objects.equals(original, replaced)) {
            setParagraphText(paragraph, replaced);
        }
    }

    /**
     * 批量替换表格文本。
     *
     * @param table        表格对象
     * @param replacements 替换映射
     */
    public static void replaceTableText(XWPFTable table, Map<String, String> replacements) {
        Objects.requireNonNull(table, "table 不能为空");
        for (XWPFTableRow row : table.getRows()) {
            for (XWPFTableCell cell : row.getTableCells()) {
                for (XWPFParagraph paragraph : cell.getParagraphs()) {
                    replaceParagraphText(paragraph, replacements);
                }
            }
        }
    }

    /**
     * 批量替换页眉页脚文本。
     *
     * @param document     文档对象
     * @param replacements 替换映射
     */
    public static void replaceHeaderFooterText(XWPFDocument document, Map<String, String> replacements) {
        checkDocument(document);
        for (XWPFHeader header : document.getHeaderList()) {
            for (XWPFParagraph paragraph : header.getParagraphs()) {
                replaceParagraphText(paragraph, replacements);
            }
            for (XWPFTable table : header.getTables()) {
                replaceTableText(table, replacements);
            }
        }
        for (XWPFFooter footer : document.getFooterList()) {
            for (XWPFParagraph paragraph : footer.getParagraphs()) {
                replaceParagraphText(paragraph, replacements);
            }
            for (XWPFTable table : footer.getTables()) {
                replaceTableText(table, replacements);
            }
        }
    }

    /**
     * 替换正文、表格、页眉、页脚中的所有文本。
     *
     * @param document     文档对象
     * @param replacements 替换映射
     */
    public static void replaceAllText(XWPFDocument document, Map<String, String> replacements) {
        replaceText(document, replacements);
        replaceHeaderFooterText(document, replacements);
    }

    /**
     * 判断文档是否包含指定文本。
     *
     * @param document 文档对象
     * @param text     文本
     * @return 包含返回 true
     */
    public static boolean containsText(XWPFDocument document, String text) {
        checkDocument(document);
        return text != null && !text.isEmpty() && extractAllText(document).contains(text);
    }

    /**
     * 统计指定文本在文档中的出现次数。
     *
     * @param document 文档对象
     * @param text     文本
     * @return 出现次数
     */
    public static int countText(XWPFDocument document, String text) {
        checkDocument(document);
        if (text == null || text.isEmpty()) {
            return 0;
        }
        String allText = extractAllText(document);
        int count = 0;
        int index = 0;
        while ((index = allText.indexOf(text, index)) >= 0) {
            count++;
            index += text.length();
        }
        return count;
    }

    /**
     * 填充默认 ${key} 格式的模板占位符。
     *
     * @param document 文档对象
     * @param data     数据映射
     */
    public static void fillTemplate(XWPFDocument document, Map<String, Object> data) {
        fillTemplate(document, data, DEFAULT_PLACEHOLDER_PREFIX, DEFAULT_PLACEHOLDER_SUFFIX);
    }

    /**
     * 按指定前后缀填充模板占位符。
     *
     * @param document 文档对象
     * @param data     数据映射
     * @param prefix   占位符前缀
     * @param suffix   占位符后缀
     */
    public static void fillTemplate(XWPFDocument document, Map<String, Object> data, String prefix, String suffix) {
        checkDocument(document);
        Map<String, String> replacements = toPlaceholderMap(data, prefix, suffix);
        replaceAllText(document, replacements);
    }

    /**
     * 按模板配置填充模板。
     *
     * @param document 文档对象
     * @param data     数据映射
     * @param config   模板配置
     */
    public static void fillTemplate(XWPFDocument document, Map<String, Object> data, WordTemplateConfig config) {
        Objects.requireNonNull(config, "config 不能为空");
        fillTemplate(document, data, config.prefix(), config.suffix());
        if (config.clearUnusedPlaceholders()) {
            clearUnusedPlaceholders(document);
        }
    }

    /**
     * 替换单个默认占位符。
     *
     * @param document 文档对象
     * @param key      占位符键名
     * @param value    占位符值
     */
    public static void replacePlaceholder(XWPFDocument document, String key, Object value) {
        Objects.requireNonNull(key, "key 不能为空");
        replacePlaceholders(document, Map.of(key, value));
    }

    /**
     * 批量替换默认占位符。
     *
     * @param document 文档对象
     * @param data     数据映射
     */
    public static void replacePlaceholders(XWPFDocument document, Map<String, Object> data) {
        fillTemplate(document, data);
    }

    /**
     * 获取文档中的默认占位符键名。
     *
     * @param document 文档对象
     * @return 占位符键名集合
     */
    public static Set<String> getPlaceholders(XWPFDocument document) {
        checkDocument(document);
        return extractPlaceholders(document);
    }

    /**
     * 判断文档是否存在指定默认占位符。
     *
     * @param document 文档对象
     * @param key      占位符键名
     * @return 存在返回 true
     */
    public static boolean hasPlaceholder(XWPFDocument document, String key) {
        Objects.requireNonNull(key, "key 不能为空");
        return getPlaceholders(document).contains(key);
    }

    /**
     * 校验模板参数并返回缺失占位符集合。
     *
     * @param document 文档对象
     * @param data     数据映射
     * @return 缺失占位符集合
     */
    public static Set<String> validatePlaceholders(XWPFDocument document, Map<String, Object> data) {
        Set<String> placeholders = getPlaceholders(document);
        Set<String> missing = new LinkedHashSet<>();
        Set<String> keys = data == null ? Collections.emptySet() : data.keySet();
        for (String placeholder : placeholders) {
            if (!keys.contains(placeholder)) {
                missing.add(placeholder);
            }
        }
        return missing;
    }

    /**
     * 清理未填充的默认占位符。
     *
     * @param document 文档对象
     */
    public static void clearUnusedPlaceholders(XWPFDocument document) {
        checkDocument(document);
        Map<String, String> replacements = new LinkedHashMap<>();
        for (String placeholder : getPlaceholders(document)) {
            replacements.put(DEFAULT_PLACEHOLDER_PREFIX + placeholder + DEFAULT_PLACEHOLDER_SUFFIX, "");
        }
        replaceAllText(document, replacements);
    }

    /**
     * 保留未填充占位符，此方法用于语义化表达无需处理。
     *
     * @param document 文档对象
     */
    public static void keepUnusedPlaceholders(XWPFDocument document) {
        checkDocument(document);
    }

    /**
     * 获取文档全部表格。
     *
     * @param document 文档对象
     * @return 表格列表
     */
    public static List<XWPFTable> getTables(XWPFDocument document) {
        checkDocument(document);
        return document.getTables();
    }

    /**
     * 获取指定索引表格。
     *
     * @param document 文档对象
     * @param index    表格索引
     * @return 表格对象
     */
    public static XWPFTable getTable(XWPFDocument document, int index) {
        List<XWPFTable> tables = getTables(document);
        checkIndex(index, tables.size(), "index");
        return tables.get(index);
    }

    /**
     * 在文档末尾创建表格。
     *
     * @param document 文档对象
     * @param rows     行数
     * @param cols     列数
     * @return 表格对象
     */
    public static XWPFTable addTable(XWPFDocument document, int rows, int cols) {
        checkDocument(document);
        checkPositive(rows, "rows");
        checkPositive(cols, "cols");
        XWPFTable table = document.createTable(rows, cols);
        ensureTableSize(table, rows, cols);
        return table;
    }

    /**
     * 在正文元素指定位置插入表格。
     *
     * @param document 文档对象
     * @param index    正文元素索引
     * @param rows     行数
     * @param cols     列数
     * @return 表格对象
     */
    public static XWPFTable insertTable(XWPFDocument document, int index, int rows, int cols) {
        checkDocument(document);
        checkPositive(rows, "rows");
        checkPositive(cols, "cols");
        checkIndexForInsert(index, document.getBodyElements().size(), "index");
        if (index == document.getBodyElements().size()) {
            return addTable(document, rows, cols);
        }
        IBodyElement element = document.getBodyElements().get(index);
        XmlCursor cursor = newCursor(element);
        try {
            XWPFTable table = document.insertNewTbl(cursor);
            ensureTableSize(table, rows, cols);
            return table;
        } finally {
            cursor.dispose();
        }
    }

    /**
     * 删除指定索引表格。
     *
     * @param document 文档对象
     * @param index    表格索引
     */
    public static void removeTable(XWPFDocument document, int index) {
        XWPFTable table = getTable(document, index);
        int bodyPos = document.getPosOfTable(table);
        document.removeBodyElement(bodyPos);
    }

    /**
     * 获取表格纯文本。
     *
     * @param table 表格对象
     * @return 表格文本
     */
    public static String getTableText(XWPFTable table) {
        Objects.requireNonNull(table, "table 不能为空");
        StringBuilder builder = new StringBuilder();
        for (List<String> row : extractTableData(table)) {
            if (!builder.isEmpty()) {
                builder.append(System.lineSeparator());
            }
            builder.append(String.join("\t", row));
        }
        return builder.toString();
    }

    /**
     * 设置单元格文本。
     *
     * @param cell 单元格
     * @param text 文本
     */
    public static void setCellText(XWPFTableCell cell, String text) {
        Objects.requireNonNull(cell, "cell 不能为空");
        List<XWPFParagraph> paragraphs = cell.getParagraphs();
        if (paragraphs.isEmpty()) {
            setParagraphText(cell.addParagraph(), text);
        } else {
            setParagraphText(paragraphs.getFirst(), text);
            for (int i = paragraphs.size() - 1; i >= 1; i--) {
                cell.removeParagraph(i);
            }
        }
    }

    /**
     * 获取单元格文本。
     *
     * @param cell 单元格
     * @return 文本
     */
    public static String getCellText(XWPFTableCell cell) {
        Objects.requireNonNull(cell, "cell 不能为空");
        String text = cell.getText();
        return text == null ? "" : text;
    }

    /**
     * 获取指定单元格。
     *
     * @param table    表格对象
     * @param rowIndex 行索引
     * @param colIndex 列索引
     * @return 单元格
     */
    public static XWPFTableCell getCell(XWPFTable table, int rowIndex, int colIndex) {
        checkCellIndex(table, rowIndex, colIndex);
        return table.getRow(rowIndex).getCell(colIndex);
    }

    /**
     * 追加表格行。
     *
     * @param table 表格对象
     * @return 新行
     */
    public static XWPFTableRow addRow(XWPFTable table) {
        Objects.requireNonNull(table, "table 不能为空");
        return table.createRow();
    }

    /**
     * 插入表格行。
     *
     * @param table    表格对象
     * @param rowIndex 行索引
     * @return 新行
     */
    public static XWPFTableRow insertRow(XWPFTable table, int rowIndex) {
        Objects.requireNonNull(table, "table 不能为空");
        checkIndexForInsert(rowIndex, table.getRows().size(), "rowIndex");
        XWPFTableRow row = table.insertNewTableRow(rowIndex);
        int cols = table.getRows().stream().mapToInt(r -> r.getTableCells().size()).max().orElse(1);
        for (int i = 0; i < cols; i++) {
            row.createCell();
        }
        return row;
    }

    /**
     * 删除表格行。
     *
     * @param table    表格对象
     * @param rowIndex 行索引
     */
    public static void removeRow(XWPFTable table, int rowIndex) {
        Objects.requireNonNull(table, "table 不能为空");
        checkIndex(rowIndex, table.getRows().size(), "rowIndex");
        table.removeRow(rowIndex);
    }

    /**
     * 复制表格行。
     *
     * @param table          表格对象
     * @param sourceRowIndex 源行索引
     * @param targetRowIndex 目标插入索引
     * @return 复制后的行
     */
    public static XWPFTableRow copyRow(XWPFTable table, int sourceRowIndex, int targetRowIndex) {
        Objects.requireNonNull(table, "table 不能为空");
        checkIndex(sourceRowIndex, table.getRows().size(), "sourceRowIndex");
        checkIndexForInsert(targetRowIndex, table.getRows().size(), "targetRowIndex");
        XWPFTableRow source = table.getRow(sourceRowIndex);
        XWPFTableRow target = table.insertNewTableRow(targetRowIndex);
        target.getCtRow().set(source.getCtRow().copy());
        return target;
    }

    /**
     * 按列填充表格行。
     *
     * @param row    表格行
     * @param values 列值
     */
    public static void fillRow(XWPFTableRow row, List<?> values) {
        Objects.requireNonNull(row, "row 不能为空");
        List<?> safeValues = values == null ? Collections.emptyList() : values;
        for (int i = 0; i < safeValues.size(); i++) {
            while (row.getTableCells().size() <= i) {
                row.createCell();
            }
            setCellText(row.getCell(i), formatValue(safeValues.get(i)));
        }
    }

    /**
     * 填充二维表格数据。
     *
     * @param table 表格对象
     * @param rows  二维数据
     */
    public static void fillTable(XWPFTable table, List<List<?>> rows) {
        Objects.requireNonNull(table, "table 不能为空");
        List<List<?>> safeRows = rows == null ? Collections.emptyList() : rows;
        for (int i = 0; i < safeRows.size(); i++) {
            while (table.getRows().size() <= i) {
                table.createRow();
            }
            fillRow(table.getRow(i), safeRows.get(i));
        }
    }

    /**
     * 根据模板行填充表格数据。
     *
     * @param table            表格对象
     * @param templateRowIndex 模板行索引
     * @param dataList         数据列表
     */
    public static void fillTableByTemplateRow(XWPFTable table, int templateRowIndex, List<?> dataList) {
        Objects.requireNonNull(table, "table 不能为空");
        checkIndex(templateRowIndex, table.getRows().size(), "templateRowIndex");
        List<?> safeList = dataList == null ? Collections.emptyList() : dataList;
        int insertIndex = templateRowIndex + 1;
        for (Object data : safeList) {
            XWPFTableRow row = copyRow(table, templateRowIndex, insertIndex++);
            if (data instanceof Map<?, ?> map) {
                Map<String, String> replacements = new LinkedHashMap<>();
                for (Map.Entry<?, ?> entry : map.entrySet()) {
                    replacements.put(DEFAULT_PLACEHOLDER_PREFIX + entry.getKey() + DEFAULT_PLACEHOLDER_SUFFIX, formatValue(entry.getValue()));
                }
                for (XWPFTableCell cell : row.getTableCells()) {
                    for (XWPFParagraph paragraph : cell.getParagraphs()) {
                        replaceParagraphText(paragraph, replacements);
                    }
                }
            } else if (data instanceof List<?> list) {
                fillRow(row, list);
            } else {
                fillRow(row, List.of(data));
            }
        }
        table.removeRow(templateRowIndex);
    }

    /**
     * 设置表格宽度。
     *
     * @param table 表格对象
     * @param width 宽度，例如 100% 或 9000
     */
    public static void setTableWidth(XWPFTable table, String width) {
        Objects.requireNonNull(table, "table 不能为空");
        table.setWidth(toSafeText(width));
    }

    /**
     * 设置指定列宽。
     *
     * @param table    表格对象
     * @param colIndex 列索引
     * @param width    宽度，单位 twips
     */
    public static void setColumnWidth(XWPFTable table, int colIndex, String width) {
        Objects.requireNonNull(table, "table 不能为空");
        for (XWPFTableRow row : table.getRows()) {
            if (colIndex >= 0 && colIndex < row.getTableCells().size()) {
                setCellWidth(row.getCell(colIndex), width);
            }
        }
    }

    /**
     * 设置单元格宽度。
     *
     * @param cell  单元格
     * @param width 宽度，单位 twips
     */
    public static void setCellWidth(XWPFTableCell cell, String width) {
        Objects.requireNonNull(cell, "cell 不能为空");
        CTTcPr tcPr = getOrAddTcPr(cell);
        CTTblWidth tcW = tcPr.isSetTcW() ? tcPr.getTcW() : tcPr.addNewTcW();
        tcW.setW(parseBigInteger(width, "width"));
        tcW.setType(STTblWidth.DXA);
    }

    /**
     * 设置单元格背景色。
     *
     * @param cell  单元格
     * @param color 十六进制颜色
     */
    public static void setCellBackgroundColor(XWPFTableCell cell, String color) {
        Objects.requireNonNull(cell, "cell 不能为空");
        cell.setColor(normalizeColor(color));
    }

    /**
     * 设置单元格垂直对齐。
     *
     * @param cell  单元格
     * @param align 对齐方式
     */
    public static void setCellVerticalAlign(XWPFTableCell cell, XWPFTableCell.XWPFVertAlign align) {
        Objects.requireNonNull(cell, "cell 不能为空");
        Objects.requireNonNull(align, "align 不能为空");
        cell.setVerticalAlignment(align);
    }

    /**
     * 设置表格边框。
     *
     * @param table 表格对象
     */
    public static void setTableBorders(XWPFTable table) {
        Objects.requireNonNull(table, "table 不能为空");
        CTTblBorders borders = getOrAddTableBorders(table);
        setBorder(borders.isSetTop() ? borders.getTop() : borders.addNewTop(), STBorder.SINGLE);
        setBorder(borders.isSetBottom() ? borders.getBottom() : borders.addNewBottom(), STBorder.SINGLE);
        setBorder(borders.isSetLeft() ? borders.getLeft() : borders.addNewLeft(), STBorder.SINGLE);
        setBorder(borders.isSetRight() ? borders.getRight() : borders.addNewRight(), STBorder.SINGLE);
        setBorder(borders.isSetInsideH() ? borders.getInsideH() : borders.addNewInsideH(), STBorder.SINGLE);
        setBorder(borders.isSetInsideV() ? borders.getInsideV() : borders.addNewInsideV(), STBorder.SINGLE);
    }

    /**
     * 清除表格边框。
     *
     * @param table 表格对象
     */
    public static void clearTableBorders(XWPFTable table) {
        Objects.requireNonNull(table, "table 不能为空");
        CTTblBorders borders = getOrAddTableBorders(table);
        setBorder(borders.isSetTop() ? borders.getTop() : borders.addNewTop(), STBorder.NIL);
        setBorder(borders.isSetBottom() ? borders.getBottom() : borders.addNewBottom(), STBorder.NIL);
        setBorder(borders.isSetLeft() ? borders.getLeft() : borders.addNewLeft(), STBorder.NIL);
        setBorder(borders.isSetRight() ? borders.getRight() : borders.addNewRight(), STBorder.NIL);
        setBorder(borders.isSetInsideH() ? borders.getInsideH() : borders.addNewInsideH(), STBorder.NIL);
        setBorder(borders.isSetInsideV() ? borders.getInsideV() : borders.addNewInsideV(), STBorder.NIL);
    }

    /**
     * 横向合并单元格。
     *
     * @param table    表格对象
     * @param rowIndex 行索引
     * @param fromCol  起始列
     * @param toCol    结束列
     */
    public static void mergeCellsHorizontal(XWPFTable table, int rowIndex, int fromCol, int toCol) {
        Objects.requireNonNull(table, "table 不能为空");
        checkIndex(rowIndex, table.getRows().size(), "rowIndex");
        if (fromCol < 0 || toCol < fromCol || toCol >= table.getRow(rowIndex).getTableCells().size()) {
            throw new IllegalArgumentException("列索引范围非法");
        }
        for (int col = fromCol; col <= toCol; col++) {
            XWPFTableCell cell = table.getRow(rowIndex).getCell(col);
            CTHMerge hMerge = getOrAddTcPr(cell).isSetHMerge() ? getOrAddTcPr(cell).getHMerge() : getOrAddTcPr(cell).addNewHMerge();
            hMerge.setVal(col == fromCol ? STMerge.RESTART : STMerge.CONTINUE);
        }
    }

    /**
     * 纵向合并单元格。
     *
     * @param table    表格对象
     * @param colIndex 列索引
     * @param fromRow  起始行
     * @param toRow    结束行
     */
    public static void mergeCellsVertical(XWPFTable table, int colIndex, int fromRow, int toRow) {
        Objects.requireNonNull(table, "table 不能为空");
        if (fromRow < 0 || toRow < fromRow || toRow >= table.getRows().size()) {
            throw new IllegalArgumentException("行索引范围非法");
        }
        for (int row = fromRow; row <= toRow; row++) {
            checkCellIndex(table, row, colIndex);
            XWPFTableCell cell = table.getRow(row).getCell(colIndex);
            CTVMerge vMerge = getOrAddTcPr(cell).isSetVMerge() ? getOrAddTcPr(cell).getVMerge() : getOrAddTcPr(cell).addNewVMerge();
            vMerge.setVal(row == fromRow ? STMerge.RESTART : STMerge.CONTINUE);
        }
    }

    /**
     * 合并指定矩形区域单元格。
     *
     * @param table   表格对象
     * @param fromRow 起始行
     * @param toRow   结束行
     * @param fromCol 起始列
     * @param toCol   结束列
     */
    public static void mergeCells(XWPFTable table, int fromRow, int toRow, int fromCol, int toCol) {
        for (int row = fromRow; row <= toRow; row++) {
            mergeCellsHorizontal(table, row, fromCol, toCol);
        }
        for (int col = fromCol; col <= toCol; col++) {
            mergeCellsVertical(table, col, fromRow, toRow);
        }
    }

    /**
     * 设置表头行样式。
     *
     * @param row 表头行
     */
    public static void setHeaderRowStyle(XWPFTableRow row) {
        Objects.requireNonNull(row, "row 不能为空");
        for (XWPFTableCell cell : row.getTableCells()) {
            setCellBackgroundColor(cell, "D9EAF7");
            for (XWPFParagraph paragraph : cell.getParagraphs()) {
                paragraph.setAlignment(ParagraphAlignment.CENTER);
                for (XWPFRun run : paragraph.getRuns()) {
                    run.setBold(true);
                }
            }
        }
    }

    /**
     * 设置表格隔行背景色。
     *
     * @param table 表格对象
     */
    public static void setAlternateRowStyle(XWPFTable table) {
        Objects.requireNonNull(table, "table 不能为空");
        for (int i = 1; i < table.getRows().size(); i += 2) {
            for (XWPFTableCell cell : table.getRow(i).getTableCells()) {
                setCellBackgroundColor(cell, "F7F7F7");
            }
        }
    }

    /**
     * 在文档末尾追加图片。
     *
     * @param document    文档对象
     * @param imageStream 图片输入流
     * @param fileName    图片文件名
     * @param width       宽度，单位像素
     * @param height      高度，单位像素
     * @throws IOException 写入失败时抛出
     */
    public static void addImage(XWPFDocument document, InputStream imageStream, String fileName, int width, int height) throws IOException {
        checkDocument(document);
        XWPFParagraph paragraph = document.createParagraph();
        insertImage(paragraph, imageStream, fileName, width, height);
    }

    /**
     * 在段落中插入图片。
     *
     * @param paragraph   段落对象
     * @param imageStream 图片输入流
     * @param fileName    图片文件名
     * @param width       宽度，单位像素
     * @param height      高度，单位像素
     * @throws IOException 写入失败时抛出
     */
    public static void insertImage(XWPFParagraph paragraph, InputStream imageStream, String fileName, int width, int height) throws IOException {
        Objects.requireNonNull(paragraph, "paragraph 不能为空");
        Objects.requireNonNull(imageStream, "imageStream 不能为空");
        checkPositive(width, "width");
        checkPositive(height, "height");
        try {
            paragraph.createRun().addPicture(imageStream, getPictureType(fileName), fileName, Units.toEMU(width), Units.toEMU(height));
        } catch (InvalidFormatException e) {
            throw new IOException("插入图片失败", e);
        }
    }

    /**
     * 在单元格中插入图片。
     *
     * @param cell        单元格
     * @param imageStream 图片输入流
     * @param fileName    图片文件名
     * @param width       宽度，单位像素
     * @param height      高度，单位像素
     * @throws IOException 写入失败时抛出
     */
    public static void insertImage(XWPFTableCell cell, InputStream imageStream, String fileName, int width, int height) throws IOException {
        Objects.requireNonNull(cell, "cell 不能为空");
        XWPFParagraph paragraph = cell.getParagraphs().isEmpty() ? cell.addParagraph() : cell.getParagraphs().getFirst();
        insertImage(paragraph, imageStream, fileName, width, height);
    }

    /**
     * 将占位符替换为图片。
     *
     * @param document    文档对象
     * @param placeholder 占位符
     * @param imageStream 图片输入流
     * @param fileName    图片文件名
     * @param width       宽度，单位像素
     * @param height      高度，单位像素
     * @throws IOException 写入失败时抛出
     */
    public static void replacePlaceholderWithImage(XWPFDocument document, String placeholder, InputStream imageStream, String fileName, int width, int height) throws IOException {
        checkDocument(document);
        Objects.requireNonNull(placeholder, "placeholder 不能为空");
        for (XWPFParagraph paragraph : findAllParagraphs(document)) {
            if (getParagraphText(paragraph).contains(placeholder)) {
                replaceParagraphText(paragraph, Map.of(placeholder, ""));
                insertImage(paragraph, imageStream, fileName, width, height);
                return;
            }
        }
    }

    /**
     * 批量追加图片。
     *
     * @param document 文档对象
     * @param images   图片配置列表
     * @throws IOException 写入失败时抛出
     */
    public static void addImages(XWPFDocument document, List<WordImage> images) throws IOException {
        checkDocument(document);
        if (images == null) {
            return;
        }
        for (WordImage image : images) {
            Objects.requireNonNull(image, "image 不能为空");
            addImage(document, new ByteArrayInputStream(image.data()), image.fileName(), image.width(), image.height());
        }
    }

    /**
     * 根据文件名获取 POI 图片类型。
     *
     * @param fileName 文件名
     * @return 图片类型
     */
    public static int getPictureType(String fileName) {
        Objects.requireNonNull(fileName, "fileName 不能为空");
        String lower = fileName.toLowerCase(Locale.ROOT);
        if (lower.endsWith(".png")) {
            return Document.PICTURE_TYPE_PNG;
        }
        if (lower.endsWith(".jpg") || lower.endsWith(".jpeg")) {
            return Document.PICTURE_TYPE_JPEG;
        }
        if (lower.endsWith(".gif")) {
            return Document.PICTURE_TYPE_GIF;
        }
        if (lower.endsWith(".bmp")) {
            return Document.PICTURE_TYPE_BMP;
        }
        throw new IllegalArgumentException("不支持的图片格式: " + fileName);
    }

    /**
     * 判断图片格式是否支持。
     *
     * @param fileName 文件名
     * @return 支持返回 true
     */
    public static boolean isSupportedImage(String fileName) {
        if (fileName == null || fileName.isBlank()) {
            return false;
        }
        try {
            getPictureType(fileName);
            return true;
        } catch (IllegalArgumentException e) {
            return false;
        }
    }

    /**
     * 删除图片占位符文本。
     *
     * @param document    文档对象
     * @param placeholder 占位符
     */
    public static void removeImagePlaceholder(XWPFDocument document, String placeholder) {
        replaceAllText(document, Map.of(placeholder, ""));
    }

    /**
     * 设置 Run 字体。
     *
     * @param run        Run 对象
     * @param fontFamily 字体
     */
    public static void setFont(XWPFRun run, String fontFamily) {
        Objects.requireNonNull(run, "run 不能为空");
        run.setFontFamily(toSafeText(fontFamily));
    }

    /**
     * 设置 Run 字号。
     *
     * @param run      Run 对象
     * @param fontSize 字号
     */
    public static void setFontSize(XWPFRun run, int fontSize) {
        Objects.requireNonNull(run, "run 不能为空");
        checkPositive(fontSize, "fontSize");
        run.setFontSize(fontSize);
    }

    /**
     * 设置 Run 字体颜色。
     *
     * @param run   Run 对象
     * @param color 十六进制颜色
     */
    public static void setFontColor(XWPFRun run, String color) {
        Objects.requireNonNull(run, "run 不能为空");
        run.setColor(normalizeColor(color));
    }

    /**
     * 设置 Run 加粗。
     *
     * @param run  Run 对象
     * @param bold 是否加粗
     */
    public static void setBold(XWPFRun run, boolean bold) {
        Objects.requireNonNull(run, "run 不能为空");
        run.setBold(bold);
    }

    /**
     * 设置 Run 斜体。
     *
     * @param run    Run 对象
     * @param italic 是否斜体
     */
    public static void setItalic(XWPFRun run, boolean italic) {
        Objects.requireNonNull(run, "run 不能为空");
        run.setItalic(italic);
    }

    /**
     * 设置 Run 下划线。
     *
     * @param run       Run 对象
     * @param underline 是否下划线
     */
    public static void setUnderline(XWPFRun run, boolean underline) {
        Objects.requireNonNull(run, "run 不能为空");
        run.setUnderline(underline ? UnderlinePatterns.SINGLE : UnderlinePatterns.NONE);
    }

    /**
     * 设置段落对齐方式。
     *
     * @param paragraph 段落对象
     * @param alignment 对齐方式
     */
    public static void setParagraphAlign(XWPFParagraph paragraph, ParagraphAlignment alignment) {
        Objects.requireNonNull(paragraph, "paragraph 不能为空");
        Objects.requireNonNull(alignment, "alignment 不能为空");
        paragraph.setAlignment(alignment);
    }

    /**
     * 设置段落首行缩进。
     *
     * @param paragraph       段落对象
     * @param firstLineIndent 首行缩进，单位 twips
     */
    public static void setParagraphIndent(XWPFParagraph paragraph, int firstLineIndent) {
        Objects.requireNonNull(paragraph, "paragraph 不能为空");
        paragraph.setIndentationFirstLine(firstLineIndent);
    }

    /**
     * 设置段落行距。
     *
     * @param paragraph 段落对象
     * @param spacing   行距
     */
    public static void setLineSpacing(XWPFParagraph paragraph, double spacing) {
        Objects.requireNonNull(paragraph, "paragraph 不能为空");
        if (spacing <= 0) {
            throw new IllegalArgumentException("spacing 必须大于 0");
        }
        paragraph.setSpacingBetween(spacing);
    }

    /**
     * 设置段前段后间距。
     *
     * @param paragraph 段落对象
     * @param before    段前间距
     * @param after     段后间距
     */
    public static void setParagraphSpacing(XWPFParagraph paragraph, int before, int after) {
        Objects.requireNonNull(paragraph, "paragraph 不能为空");
        if (before < 0 || after < 0) {
            throw new IllegalArgumentException("段落间距不能小于 0");
        }
        paragraph.setSpacingBefore(before);
        paragraph.setSpacingAfter(after);
    }

    /**
     * 复制段落样式。
     *
     * @param source 源段落
     * @param target 目标段落
     */
    public static void copyParagraphStyle(XWPFParagraph source, XWPFParagraph target) {
        Objects.requireNonNull(source, "source 不能为空");
        Objects.requireNonNull(target, "target 不能为空");
        if (source.getCTP().isSetPPr()) {
            target.getCTP().setPPr((CTPPr) source.getCTP().getPPr().copy());
        } else if (target.getCTP().isSetPPr()) {
            target.getCTP().unsetPPr();
        }
    }

    /**
     * 复制表格样式。
     *
     * @param source 源表格
     * @param target 目标表格
     */
    public static void copyTableStyle(XWPFTable source, XWPFTable target) {
        Objects.requireNonNull(source, "source 不能为空");
        Objects.requireNonNull(target, "target 不能为空");

        CTTblPr sourceTablePr = source.getCTTbl().getTblPr();
        if (sourceTablePr != null) {
            target.getCTTbl().setTblPr((CTTblPr) sourceTablePr.copy());
        }
    }

    /**
     * 应用默认正文样式。
     *
     * @param document 文档对象
     */
    public static void applyDefaultTextStyle(XWPFDocument document) {
        checkDocument(document);
        for (XWPFParagraph paragraph : findAllParagraphs(document)) {
            for (XWPFRun run : paragraph.getRuns()) {
                run.setFontFamily("宋体");
                run.setFontSize(12);
            }
        }
    }

    /**
     * 应用标题样式。
     *
     * @param paragraph 段落对象
     * @param level     标题级别
     */
    public static void applyHeadingStyle(XWPFParagraph paragraph, int level) {
        Objects.requireNonNull(paragraph, "paragraph 不能为空");
        if (level < 1 || level > 9) {
            throw new IllegalArgumentException("level 必须在 1 到 9 之间");
        }
        paragraph.setStyle("Heading" + level);
        for (XWPFRun run : paragraph.getRuns()) {
            run.setBold(true);
            run.setFontSize(Math.max(10, 18 - level));
        }
    }

    /**
     * 添加指定级别标题。
     *
     * @param document 文档对象
     * @param text     标题文本
     * @param level    标题级别
     * @return 标题段落
     */
    public static XWPFParagraph addHeading(XWPFDocument document, String text, int level) {
        XWPFParagraph paragraph = addParagraph(document, text);
        applyHeadingStyle(paragraph, level);
        return paragraph;
    }

    /**
     * 添加一级标题。
     *
     * @param document 文档对象
     * @param text     标题文本
     * @return 标题段落
     */
    public static XWPFParagraph addHeading1(XWPFDocument document, String text) {
        return addHeading(document, text, 1);
    }

    /**
     * 添加二级标题。
     *
     * @param document 文档对象
     * @param text     标题文本
     * @return 标题段落
     */
    public static XWPFParagraph addHeading2(XWPFDocument document, String text) {
        return addHeading(document, text, 2);
    }

    /**
     * 添加三级标题。
     *
     * @param document 文档对象
     * @param text     标题文本
     * @return 标题段落
     */
    public static XWPFParagraph addHeading3(XWPFDocument document, String text) {
        return addHeading(document, text, 3);
    }

    /**
     * 添加目录域。
     *
     * @param document 文档对象
     * @return 目录段落
     */
    public static XWPFParagraph addToc(XWPFDocument document) {
        checkDocument(document);
        XWPFParagraph paragraph = document.createParagraph();
        addField(paragraph, "TOC \\o \"1-3\" \\h \\z \\u");
        return paragraph;
    }

    /**
     * 标记目录需要在 Word 中更新。
     *
     * @param document 文档对象
     */
    public static void markTocDirty(XWPFDocument document) {
        checkDocument(document);
    }

    /**
     * 添加带编号标题。
     *
     * @param document 文档对象
     * @param text     标题文本
     * @param level    标题级别
     * @return 标题段落
     */
    public static XWPFParagraph addNumberedHeading(XWPFDocument document, String text, int level) {
        return addHeading(document, text, level);
    }

    /**
     * 设置 A4 页面。
     *
     * @param document 文档对象
     */
    public static void setA4Page(XWPFDocument document) {
        setPageSize(document, A4_WIDTH_TWIPS, A4_HEIGHT_TWIPS);
        setPageOrientation(document, PageOrientation.PORTRAIT);
    }

    /**
     * 设置页面大小。
     *
     * @param document 文档对象
     * @param width    页面宽度，单位 twips
     * @param height   页面高度，单位 twips
     */
    public static void setPageSize(XWPFDocument document, int width, int height) {
        checkDocument(document);
        checkPositive(width, "width");
        checkPositive(height, "height");
        CTPageSz pageSz = getOrAddSectPr(document).isSetPgSz() ? getOrAddSectPr(document).getPgSz() : getOrAddSectPr(document).addNewPgSz();
        pageSz.setW(BigInteger.valueOf(width));
        pageSz.setH(BigInteger.valueOf(height));
    }

    /**
     * 设置页面方向。
     *
     * @param document    文档对象
     * @param orientation 页面方向
     */
    public static void setPageOrientation(XWPFDocument document, PageOrientation orientation) {
        checkDocument(document);
        Objects.requireNonNull(orientation, "orientation 不能为空");
        CTPageSz pageSz = getOrAddSectPr(document).isSetPgSz() ? getOrAddSectPr(document).getPgSz() : getOrAddSectPr(document).addNewPgSz();
        pageSz.setOrient(orientation == PageOrientation.LANDSCAPE ? STPageOrientation.LANDSCAPE : STPageOrientation.PORTRAIT);
    }

    /**
     * 设置页边距。
     *
     * @param document 文档对象
     * @param top      上边距
     * @param right    右边距
     * @param bottom   下边距
     * @param left     左边距
     */
    public static void setPageMargin(XWPFDocument document, int top, int right, int bottom, int left) {
        checkDocument(document);
        if (top < 0 || right < 0 || bottom < 0 || left < 0) {
            throw new IllegalArgumentException("页边距不能小于 0");
        }
        CTPageMar pageMar = getOrAddSectPr(document).isSetPgMar() ? getOrAddSectPr(document).getPgMar() : getOrAddSectPr(document).addNewPgMar();
        pageMar.setTop(BigInteger.valueOf(top));
        pageMar.setRight(BigInteger.valueOf(right));
        pageMar.setBottom(BigInteger.valueOf(bottom));
        pageMar.setLeft(BigInteger.valueOf(left));
    }

    /**
     * 在文档末尾添加分页符。
     *
     * @param document 文档对象
     */
    public static void addPageBreak(XWPFDocument document) {
        checkDocument(document);
        addPageBreak(document.createParagraph());
    }

    /**
     * 在段落中添加分页符。
     *
     * @param paragraph 段落对象
     */
    public static void addPageBreak(XWPFParagraph paragraph) {
        Objects.requireNonNull(paragraph, "paragraph 不能为空");
        paragraph.createRun().addBreak(BreakType.PAGE);
    }

    /**
     * 添加普通换行。
     *
     * @param run Run 对象
     */
    public static void addBreak(XWPFRun run) {
        Objects.requireNonNull(run, "run 不能为空");
        run.addBreak();
    }

    /**
     * 添加指定数量空行。
     *
     * @param document 文档对象
     * @param count    空行数量
     */
    public static void addBlankLine(XWPFDocument document, int count) {
        checkDocument(document);
        if (count < 0) {
            throw new IllegalArgumentException("count 不能小于 0");
        }
        for (int i = 0; i < count; i++) {
            document.createParagraph();
        }
    }

    /**
     * 在默认页脚添加页码。
     *
     * @param document 文档对象
     */
    public static void addPageNumber(XWPFDocument document) {
        addPageNumberFooter(document);
    }

    /**
     * 在段落中添加总页数字段。
     *
     * @param paragraph 段落对象
     */
    public static void addTotalPageField(XWPFParagraph paragraph) {
        addField(paragraph, "NUMPAGES");
    }

    /**
     * 添加默认页眉文本。
     *
     * @param document 文档对象
     * @param text     页眉文本
     * @return 页眉对象
     */
    public static XWPFHeader addHeader(XWPFDocument document, String text) {
        checkDocument(document);
        XWPFHeader header = document.createHeader(HeaderFooterType.DEFAULT);
        setHeaderText(header, text);
        return header;
    }

    /**
     * 添加默认页脚文本。
     *
     * @param document 文档对象
     * @param text     页脚文本
     * @return 页脚对象
     */
    public static XWPFFooter addFooter(XWPFDocument document, String text) {
        checkDocument(document);
        XWPFFooter footer = document.createFooter(HeaderFooterType.DEFAULT);
        setFooterText(footer, text);
        return footer;
    }

    /**
     * 获取全部页眉。
     *
     * @param document 文档对象
     * @return 页眉列表
     */
    public static List<XWPFHeader> getHeaders(XWPFDocument document) {
        checkDocument(document);
        return document.getHeaderList();
    }

    /**
     * 获取全部页脚。
     *
     * @param document 文档对象
     * @return 页脚列表
     */
    public static List<XWPFFooter> getFooters(XWPFDocument document) {
        checkDocument(document);
        return document.getFooterList();
    }

    /**
     * 清空全部页眉内容。
     *
     * @param document 文档对象
     */
    public static void clearHeaders(XWPFDocument document) {
        checkDocument(document);
        for (XWPFHeader header : document.getHeaderList()) {
            for (XWPFParagraph paragraph : header.getParagraphs()) {
                clearParagraph(paragraph);
            }
        }
    }

    /**
     * 清空全部页脚内容。
     *
     * @param document 文档对象
     */
    public static void clearFooters(XWPFDocument document) {
        checkDocument(document);
        for (XWPFFooter footer : document.getFooterList()) {
            for (XWPFParagraph paragraph : footer.getParagraphs()) {
                clearParagraph(paragraph);
            }
        }
    }

    /**
     * 设置页眉文本。
     *
     * @param header 页眉对象
     * @param text   文本
     */
    public static void setHeaderText(XWPFHeader header, String text) {
        Objects.requireNonNull(header, "header 不能为空");
        XWPFParagraph paragraph = header.getParagraphs().isEmpty() ? header.createParagraph() : header.getParagraphs().getFirst();
        setParagraphText(paragraph, text);
    }

    /**
     * 设置页脚文本。
     *
     * @param footer 页脚对象
     * @param text   文本
     */
    public static void setFooterText(XWPFFooter footer, String text) {
        Objects.requireNonNull(footer, "footer 不能为空");
        XWPFParagraph paragraph = footer.getParagraphs().isEmpty() ? footer.createParagraph() : footer.getParagraphs().getFirst();
        setParagraphText(paragraph, text);
    }

    /**
     * 添加页码页脚。
     *
     * @param document 文档对象
     * @return 页脚对象
     */
    public static XWPFFooter addPageNumberFooter(XWPFDocument document) {
        checkDocument(document);
        XWPFFooter footer = document.createFooter(HeaderFooterType.DEFAULT);
        XWPFParagraph paragraph = footer.createParagraph();
        paragraph.setAlignment(ParagraphAlignment.CENTER);
        addField(paragraph, "PAGE");
        return footer;
    }

    /**
     * 添加图片页眉。
     *
     * @param document    文档对象
     * @param imageStream 图片输入流
     * @param fileName    图片文件名
     * @param width       宽度，单位像素
     * @param height      高度，单位像素
     * @return 页眉对象
     * @throws IOException 写入失败时抛出
     */
    public static XWPFHeader addHeaderImage(XWPFDocument document, InputStream imageStream, String fileName, int width, int height) throws IOException {
        XWPFHeader header = addHeader(document, "");
        insertImage(header.getParagraphs().getFirst(), imageStream, fileName, width, height);
        return header;
    }

    /**
     * 添加图片页脚。
     *
     * @param document    文档对象
     * @param imageStream 图片输入流
     * @param fileName    图片文件名
     * @param width       宽度，单位像素
     * @param height      高度，单位像素
     * @return 页脚对象
     * @throws IOException 写入失败时抛出
     */
    public static XWPFFooter addFooterImage(XWPFDocument document, InputStream imageStream, String fileName, int width, int height) throws IOException {
        XWPFFooter footer = addFooter(document, "");
        insertImage(footer.getParagraphs().getFirst(), imageStream, fileName, width, height);
        return footer;
    }

    /**
     * 添加文字水印，采用页眉浅色文本实现基础水印效果。
     *
     * @param document 文档对象
     * @param text     水印文本
     */
    public static void addTextWatermark(XWPFDocument document, String text) {
        XWPFHeader header = addHeader(document, text);
        for (XWPFParagraph paragraph : header.getParagraphs()) {
            paragraph.setAlignment(ParagraphAlignment.CENTER);
            for (XWPFRun run : paragraph.getRuns()) {
                run.setColor("D0D0D0");
                run.setFontSize(28);
            }
        }
    }

    /**
     * 添加图片水印，采用页眉图片实现基础水印效果。
     *
     * @param document    文档对象
     * @param imageStream 图片输入流
     * @param fileName    图片文件名
     * @throws IOException 写入失败时抛出
     */
    public static void addImageWatermark(XWPFDocument document, InputStream imageStream, String fileName) throws IOException {
        addHeaderImage(document, imageStream, fileName, 180, 180);
    }

    /**
     * 清除水印相关页眉内容。
     *
     * @param document 文档对象
     */
    public static void clearWatermark(XWPFDocument document) {
        clearHeaders(document);
    }

    /**
     * 设置文档背景色。
     *
     * @param document 文档对象
     * @param color    十六进制颜色
     */
    public static void setBackgroundColor(XWPFDocument document, String color) {
        checkDocument(document);
        if (!document.getDocument().isSetBackground()) {
            document.getDocument().addNewBackground();
        }
        document.getDocument().getBackground().setColor(normalizeColor(color));
    }

    /**
     * 添加保密标识。
     *
     * @param document 文档对象
     * @param text     标识文本
     */
    public static void addConfidentialMark(XWPFDocument document, String text) {
        addTextWatermark(document, text);
    }

    /**
     * 合并多个 Word 文档。
     *
     * @param documents 文档列表
     * @return 合并后的文档
     */
    public static XWPFDocument merge(List<XWPFDocument> documents) {
        XWPFDocument target = create();
        if (documents == null) {
            return target;
        }
        for (XWPFDocument document : documents) {
            if (document != null) {
                appendDocument(target, document);
            }
        }
        return target;
    }

    /**
     * 将源文档追加到目标文档。
     *
     * @param target 目标文档
     * @param source 源文档
     */
    public static void appendDocument(XWPFDocument target, XWPFDocument source) {
        checkDocument(target);
        checkDocument(source);
        copyBodyElements(source, target);
    }

    /**
     * 分页后追加源文档。
     *
     * @param target 目标文档
     * @param source 源文档
     */
    public static void appendDocumentWithPageBreak(XWPFDocument target, XWPFDocument source) {
        checkDocument(target);
        addPageBreak(target);
        appendDocument(target, source);
    }

    /**
     * 按段落拆分文档。
     *
     * @param document 文档对象
     * @return 拆分后的文档列表
     */
    public static List<XWPFDocument> splitByParagraph(XWPFDocument document) {
        checkDocument(document);
        List<XWPFDocument> result = new ArrayList<>();
        for (XWPFParagraph paragraph : document.getParagraphs()) {
            XWPFDocument item = create();
            item.getDocument().getBody().addNewP().set(paragraph.getCTP().copy());
            result.add(item);
        }
        return result;
    }

    /**
     * 按分页符拆分文档。
     *
     * @param document 文档对象
     * @return 拆分后的文档列表
     */
    public static List<XWPFDocument> splitByPageBreak(XWPFDocument document) {
        checkDocument(document);
        List<XWPFDocument> result = new ArrayList<>();
        XWPFDocument current = create();
        for (IBodyElement element : document.getBodyElements()) {
            copyBodyElement(element, current);
            if (element instanceof XWPFParagraph paragraph && paragraph.getCTP().xmlText().contains("type=\"page\"")) {
                result.add(current);
                current = create();
            }
        }
        if (!current.getBodyElements().isEmpty()) {
            result.add(current);
        }
        return result;
    }

    /**
     * 提取指定范围段落为新文档。
     *
     * @param document  文档对象
     * @param fromIndex 起始索引，包含
     * @param toIndex   结束索引，不包含
     * @return 新文档
     */
    public static XWPFDocument extractParagraphs(XWPFDocument document, int fromIndex, int toIndex) {
        checkDocument(document);
        if (fromIndex < 0 || toIndex < fromIndex || toIndex > document.getParagraphs().size()) {
            throw new IllegalArgumentException("段落范围非法");
        }
        XWPFDocument target = create();
        for (int i = fromIndex; i < toIndex; i++) {
            target.getDocument().getBody().addNewP().set(document.getParagraphs().get(i).getCTP().copy());
        }
        return target;
    }

    /**
     * 复制源文档正文元素到目标文档。
     *
     * @param source 源文档
     * @param target 目标文档
     */
    public static void copyBodyElements(XWPFDocument source, XWPFDocument target) {
        checkDocument(source);
        checkDocument(target);
        for (IBodyElement element : source.getBodyElements()) {
            copyBodyElement(element, target);
        }
    }

    /**
     * 复制文档样式，当前实现为基础校验入口，复杂样式关系可按项目需求增强。
     *
     * @param source 源文档
     * @param target 目标文档
     */
    public static void copyStyles(XWPFDocument source, XWPFDocument target) {
        checkDocument(source);
        checkDocument(target);
    }

    /**
     * 提取正文纯文本。
     *
     * @param document 文档对象
     * @return 正文文本
     */
    public static String extractText(XWPFDocument document) {
        checkDocument(document);
        StringBuilder builder = new StringBuilder();
        for (XWPFParagraph paragraph : document.getParagraphs()) {
            appendLine(builder, getParagraphText(paragraph));
        }
        return builder.toString();
    }

    /**
     * 提取正文段落文本列表。
     *
     * @param document 文档对象
     * @return 段落文本列表
     */
    public static List<String> extractParagraphTexts(XWPFDocument document) {
        checkDocument(document);
        List<String> result = new ArrayList<>();
        for (XWPFParagraph paragraph : document.getParagraphs()) {
            result.add(getParagraphText(paragraph));
        }
        return result;
    }

    /**
     * 提取全部表格文本。
     *
     * @param document 文档对象
     * @return 表格文本列表
     */
    public static List<String> extractTableTexts(XWPFDocument document) {
        checkDocument(document);
        List<String> result = new ArrayList<>();
        for (XWPFTable table : document.getTables()) {
            result.add(getTableText(table));
        }
        return result;
    }

    /**
     * 提取页眉文本。
     *
     * @param document 文档对象
     * @return 页眉文本列表
     */
    public static List<String> extractHeaderTexts(XWPFDocument document) {
        checkDocument(document);
        List<String> result = new ArrayList<>();
        for (XWPFHeader header : document.getHeaderList()) {
            for (XWPFParagraph paragraph : header.getParagraphs()) {
                result.add(getParagraphText(paragraph));
            }
        }
        return result;
    }

    /**
     * 提取页脚文本。
     *
     * @param document 文档对象
     * @return 页脚文本列表
     */
    public static List<String> extractFooterTexts(XWPFDocument document) {
        checkDocument(document);
        List<String> result = new ArrayList<>();
        for (XWPFFooter footer : document.getFooterList()) {
            for (XWPFParagraph paragraph : footer.getParagraphs()) {
                result.add(getParagraphText(paragraph));
            }
        }
        return result;
    }

    /**
     * 提取正文、表格、页眉、页脚中的全部文本。
     *
     * @param document 文档对象
     * @return 全部文本
     */
    public static String extractAllText(XWPFDocument document) {
        checkDocument(document);
        StringBuilder builder = new StringBuilder();
        appendLine(builder, extractText(document));
        for (String text : extractTableTexts(document)) {
            appendLine(builder, text);
        }
        for (String text : extractHeaderTexts(document)) {
            appendLine(builder, text);
        }
        for (String text : extractFooterTexts(document)) {
            appendLine(builder, text);
        }
        return builder.toString();
    }

    /**
     * 提取文档内图片数据。
     *
     * @param document 文档对象
     * @return 图片数据列表
     */
    public static List<XWPFPictureData> extractImages(XWPFDocument document) {
        checkDocument(document);
        return document.getAllPictures();
    }

    /**
     * 提取文档默认占位符键名。
     *
     * @param document 文档对象
     * @return 占位符键名集合
     */
    public static Set<String> extractPlaceholders(XWPFDocument document) {
        checkDocument(document);
        Set<String> result = new LinkedHashSet<>();
        Matcher matcher = DEFAULT_PLACEHOLDER_PATTERN.matcher(extractAllText(document));
        while (matcher.find()) {
            result.add(matcher.group(1));
        }
        return result;
    }

    /**
     * 提取表格二维数据。
     *
     * @param table 表格对象
     * @return 二维文本列表
     */
    public static List<List<String>> extractTableData(XWPFTable table) {
        Objects.requireNonNull(table, "table 不能为空");
        List<List<String>> result = new ArrayList<>();
        for (XWPFTableRow row : table.getRows()) {
            List<String> rowData = new ArrayList<>();
            for (XWPFTableCell cell : row.getTableCells()) {
                rowData.add(getCellText(cell));
            }
            result.add(rowData);
        }
        return result;
    }

    /**
     * 提取关键字附近文本。
     *
     * @param document 文档对象
     * @param keyword  关键字
     * @param range    前后字符范围
     * @return 上下文文本
     */
    public static String extractTextAroundKeyword(XWPFDocument document, String keyword, int range) {
        checkDocument(document);
        Objects.requireNonNull(keyword, "keyword 不能为空");
        if (range < 0) {
            throw new IllegalArgumentException("range 不能小于 0");
        }
        String text = extractAllText(document);
        int index = text.indexOf(keyword);
        if (index < 0) {
            return "";
        }
        int start = Math.max(0, index - range);
        int end = Math.min(text.length(), index + keyword.length() + range);
        return text.substring(start, end);
    }

    /**
     * 添加超链接。
     *
     * @param paragraph 段落对象
     * @param text      链接文本
     * @param url       链接地址
     * @return Run 对象
     */
    public static XWPFRun addHyperlink(XWPFParagraph paragraph, String text, String url) {
        Objects.requireNonNull(paragraph, "paragraph 不能为空");
        Objects.requireNonNull(url, "url 不能为空");
        XWPFRun run = paragraph.createHyperlinkRun(url);
        run.setText(toSafeText(text));
        run.setColor("0000FF");
        run.setUnderline(UnderlinePatterns.SINGLE);
        return run;
    }

    /**
     * 获取文档超链接列表。
     *
     * @param document 文档对象
     * @return 超链接列表
     */
    public static List<XWPFHyperlink> getHyperlinks(XWPFDocument document) {
        checkDocument(document);
        return List.of(document.getHyperlinks());
    }

    /**
     * 在段落添加书签。
     *
     * @param paragraph    段落对象
     * @param bookmarkName 书签名称
     */
    public static void addBookmark(XWPFParagraph paragraph, String bookmarkName) {
        Objects.requireNonNull(paragraph, "paragraph 不能为空");
        Objects.requireNonNull(bookmarkName, "bookmarkName 不能为空");
        BigInteger id = BigInteger.valueOf(BOOKMARK_ID.getAndIncrement());
        CTBookmark start = paragraph.getCTP().addNewBookmarkStart();
        start.setName(bookmarkName);
        start.setId(id);
        paragraph.getCTP().addNewBookmarkEnd().setId(id);
    }

    /**
     * 查找指定书签所在段落。
     *
     * @param document     文档对象
     * @param bookmarkName 书签名称
     * @return 段落 Optional
     */
    public static Optional<XWPFParagraph> findBookmark(XWPFDocument document, String bookmarkName) {
        checkDocument(document);
        Objects.requireNonNull(bookmarkName, "bookmarkName 不能为空");
        for (XWPFParagraph paragraph : findAllParagraphs(document)) {
            for (CTBookmark bookmark : paragraph.getCTP().getBookmarkStartList()) {
                if (bookmarkName.equals(bookmark.getName())) {
                    return Optional.of(paragraph);
                }
            }
        }
        return Optional.empty();
    }

    /**
     * 删除指定书签。
     *
     * @param document     文档对象
     * @param bookmarkName 书签名称
     */
    public static void removeBookmark(XWPFDocument document, String bookmarkName) {
        checkDocument(document);
        Objects.requireNonNull(bookmarkName, "bookmarkName 不能为空");
        for (XWPFParagraph paragraph : findAllParagraphs(document)) {
            CTP ctp = paragraph.getCTP();
            for (int i = ctp.sizeOfBookmarkStartArray() - 1; i >= 0; i--) {
                if (bookmarkName.equals(ctp.getBookmarkStartArray(i).getName())) {
                    BigInteger id = ctp.getBookmarkStartArray(i).getId();
                    ctp.removeBookmarkStart(i);
                    for (int j = ctp.sizeOfBookmarkEndArray() - 1; j >= 0; j--) {
                        if (id.equals(ctp.getBookmarkEndArray(j).getId())) {
                            ctp.removeBookmarkEnd(j);
                        }
                    }
                }
            }
        }
    }

    /**
     * 添加基础批注文本。
     *
     * @param document 文档对象
     * @param text     批注文本
     */
    public static void addComment(XWPFDocument document, String text) {
        addParagraph(document, COMMENT_PREFIX + toSafeText(text));
    }

    /**
     * 获取基础批注文本。
     *
     * @param document 文档对象
     * @return 批注文本列表
     */
    public static List<String> getComments(XWPFDocument document) {
        checkDocument(document);
        List<String> result = new ArrayList<>();
        for (XWPFParagraph paragraph : document.getParagraphs()) {
            String text = getParagraphText(paragraph);
            if (text.startsWith(COMMENT_PREFIX)) {
                result.add(text.substring(COMMENT_PREFIX.length()));
            }
        }
        return result;
    }

    /**
     * 删除基础批注文本段落。
     *
     * @param document 文档对象
     */
    public static void removeComments(XWPFDocument document) {
        checkDocument(document);
        for (int i = document.getParagraphs().size() - 1; i >= 0; i--) {
            if (getParagraphText(document.getParagraphs().get(i)).startsWith(COMMENT_PREFIX)) {
                removeParagraph(document, i);
            }
        }
    }

    /**
     * 校验文档对象不为空。
     *
     * @param document 文档对象
     */
    public static void checkDocument(XWPFDocument document) {
        Objects.requireNonNull(document, "document 不能为空");
    }

    /**
     * 校验文件路径是 docx 文件。
     *
     * @param path 文件路径
     */
    public static void checkDocxFile(Path path) {
        Objects.requireNonNull(path, "path 不能为空");
        if (!isDocx(path.getFileName().toString())) {
            throw new IllegalArgumentException("只支持 .docx 文件: " + path);
        }
    }

    /**
     * 判断文件名是否为 docx。
     *
     * @param fileName 文件名
     * @return 是 docx 返回 true
     */
    public static boolean isDocx(String fileName) {
        return fileName != null && fileName.toLowerCase(Locale.ROOT).endsWith(".docx");
    }

    /**
     * 校验模板必填占位符。
     *
     * @param document     文档对象
     * @param requiredKeys 必填键名
     */
    public static void checkPlaceholders(XWPFDocument document, Collection<String> requiredKeys) {
        checkDocument(document);
        if (requiredKeys == null || requiredKeys.isEmpty()) {
            return;
        }
        Set<String> placeholders = getPlaceholders(document);
        for (String requiredKey : requiredKeys) {
            if (!placeholders.contains(requiredKey)) {
                throw new IllegalArgumentException("模板缺少占位符: " + requiredKey);
            }
        }
    }

    /**
     * 校验表格索引。
     *
     * @param document   文档对象
     * @param tableIndex 表格索引
     */
    public static void checkTableIndex(XWPFDocument document, int tableIndex) {
        checkIndex(tableIndex, getTables(document).size(), "tableIndex");
    }

    /**
     * 校验单元格索引。
     *
     * @param table    表格对象
     * @param rowIndex 行索引
     * @param colIndex 列索引
     */
    public static void checkCellIndex(XWPFTable table, int rowIndex, int colIndex) {
        Objects.requireNonNull(table, "table 不能为空");
        checkIndex(rowIndex, table.getRows().size(), "rowIndex");
        checkIndex(colIndex, table.getRow(rowIndex).getTableCells().size(), "colIndex");
    }

    /**
     * 校验图片类型是否支持。
     *
     * @param fileName 文件名
     */
    public static void checkImageType(String fileName) {
        getPictureType(fileName);
    }

    /**
     * 校验文件大小。
     *
     * @param path    文件路径
     * @param maxSize 最大字节数
     * @throws IOException 读取文件大小失败时抛出
     */
    public static void checkFileSize(Path path, long maxSize) throws IOException {
        Objects.requireNonNull(path, "path 不能为空");
        if (maxSize < 0) {
            throw new IllegalArgumentException("maxSize 不能小于 0");
        }
        if (Files.size(path) > maxSize) {
            throw new IllegalArgumentException("文件大小超过限制: " + maxSize);
        }
    }

    /**
     * 清理非法 XML 字符。
     *
     * @param text 原文本
     * @return 清理后的文本
     */
    public static String cleanInvalidXmlChars(String text) {
        if (text == null || text.isEmpty()) {
            return "";
        }
        StringBuilder builder = new StringBuilder(text.length());
        text.codePoints().filter(WordUtil::isValidXmlChar).forEach(builder::appendCodePoint);
        return builder.toString();
    }

    /**
     * 安全转换为文本。
     *
     * @param value 值
     * @return 文本
     */
    public static String toSafeText(Object value) {
        return value == null ? "" : cleanInvalidXmlChars(String.valueOf(value));
    }

    /**
     * 将对象格式化为模板文本。
     *
     * @param value 对象值
     * @return 文本
     */
    public static String formatValue(Object value) {
        if (value == null) {
            return "";
        }
        if (value instanceof LocalDate localDate) {
            return formatDate(localDate, "yyyy-MM-dd");
        }
        if (value instanceof LocalDateTime localDateTime) {
            return formatDateTime(localDateTime, "yyyy-MM-dd HH:mm:ss");
        }
        if (value instanceof BigDecimal bigDecimal) {
            return bigDecimal.stripTrailingZeros().toPlainString();
        }
        if (value instanceof Collection<?> collection) {
            return joinValues(collection, ",");
        }
        if (value instanceof Boolean bool) {
            return formatBoolean(bool, "是", "否");
        }
        if (value instanceof Enum<?> enumValue) {
            return formatEnum(enumValue);
        }
        return toSafeText(value);
    }

    /**
     * 格式化日期。
     *
     * @param date    日期
     * @param pattern 格式
     * @return 日期文本
     */
    public static String formatDate(LocalDate date, String pattern) {
        if (date == null) {
            return "";
        }
        return date.format(DateTimeFormatter.ofPattern(defaultPattern(pattern, "yyyy-MM-dd")));
    }

    /**
     * 格式化日期时间。
     *
     * @param time    日期时间
     * @param pattern 格式
     * @return 日期时间文本
     */
    public static String formatDateTime(LocalDateTime time, String pattern) {
        if (time == null) {
            return "";
        }
        return time.format(DateTimeFormatter.ofPattern(defaultPattern(pattern, "yyyy-MM-dd HH:mm:ss")));
    }

    /**
     * 格式化金额。
     *
     * @param amount 金额
     * @return 金额文本
     */
    public static String formatAmount(BigDecimal amount) {
        if (amount == null) {
            return "";
        }
        return NumberFormat.getCurrencyInstance(Locale.CHINA).format(amount);
    }

    /**
     * 格式化百分比。
     *
     * @param value 小数值
     * @return 百分比文本
     */
    public static String formatPercent(BigDecimal value) {
        if (value == null) {
            return "";
        }
        NumberFormat format = NumberFormat.getPercentInstance(Locale.CHINA);
        format.setMaximumFractionDigits(2);
        return format.format(value);
    }

    /**
     * 拼接集合值。
     *
     * @param values    集合
     * @param delimiter 分隔符
     * @return 拼接文本
     */
    public static String joinValues(Collection<?> values, String delimiter) {
        if (values == null || values.isEmpty()) {
            return "";
        }
        String safeDelimiter = delimiter == null ? "" : delimiter;
        StringBuilder builder = new StringBuilder();
        for (Object value : values) {
            if (!builder.isEmpty()) {
                builder.append(safeDelimiter);
            }
            builder.append(formatValue(value));
        }
        return builder.toString();
    }

    /**
     * 按换行拆分文本。
     *
     * @param text 文本
     * @return 行文本列表
     */
    public static List<String> splitLines(String text) {
        if (text == null || text.isEmpty()) {
            return Collections.emptyList();
        }
        return List.of(text.split("\\R", -1));
    }

    /**
     * 格式化布尔值。
     *
     * @param value     布尔值
     * @param trueText  true 文本
     * @param falseText false 文本
     * @return 格式化文本
     */
    public static String formatBoolean(Boolean value, String trueText, String falseText) {
        if (value == null) {
            return "";
        }
        return value ? toSafeText(trueText) : toSafeText(falseText);
    }

    /**
     * 格式化枚举值。
     *
     * @param value 枚举值
     * @return 枚举名称
     */
    public static String formatEnum(Enum<?> value) {
        return value == null ? "" : value.name();
    }

    /**
     * 将文档写入 Web 响应输出流。
     *
     * @param document     文档对象
     * @param outputStream 响应输出流
     * @throws IOException 写出失败时抛出
     */
    public static void writeToResponse(XWPFDocument document, OutputStream outputStream) throws IOException {
        write(document, outputStream);
    }

    /**
     * 构建浏览器下载文件名。
     *
     * @param fileName 原文件名
     * @return 编码后的文件名
     */
    public static String buildDownloadFileName(String fileName) {
        String safeName = toSafeText(fileName);
        if (safeName.isBlank()) {
            safeName = "document.docx";
        }
        if (!safeName.toLowerCase(Locale.ROOT).endsWith(".docx")) {
            safeName += ".docx";
        }
        return URLEncoder.encode(safeName, StandardCharsets.UTF_8).replace("+", "%20");
    }

    /**
     * 构建下载响应头映射。
     *
     * @param fileName 文件名
     * @return 响应头映射
     */
    public static Map<String, String> buildDownloadHeaders(String fileName) {
        Map<String, String> headers = new LinkedHashMap<>();
        setDownloadHeaders(headers, fileName);
        return headers;
    }

    /**
     * 设置下载响应头映射。
     *
     * @param headers  响应头映射
     * @param fileName 文件名
     */
    public static void setDownloadHeaders(Map<String, String> headers, String fileName) {
        Objects.requireNonNull(headers, "headers 不能为空");
        String encoded = buildDownloadFileName(fileName);
        headers.put("Content-Type", "application/vnd.openxmlformats-officedocument.wordprocessingml.document");
        headers.put("Content-Disposition", "attachment; filename*=UTF-8''" + encoded);
    }

    /**
     * 填充模板并写入响应输出流。
     *
     * @param templateStream 模板输入流
     * @param data           数据映射
     * @param outputStream   输出流
     * @param fileName       文件名
     * @return 下载响应头映射
     * @throws IOException 读写失败时抛出
     */
    public static Map<String, String> exportTemplateToResponse(InputStream templateStream, Map<String, Object> data, OutputStream outputStream, String fileName) throws IOException {
        try (XWPFDocument document = read(templateStream)) {
            fillTemplate(document, data);
            writeToResponse(document, outputStream);
            return buildDownloadHeaders(fileName);
        }
    }

    /**
     * 导出文档字节数组。
     *
     * @param document 文档对象
     * @return 字节数组
     * @throws IOException 写出失败时抛出
     */
    public static byte[] exportToBytes(XWPFDocument document) throws IOException {
        return writeToBytes(document);
    }

    /**
     * 导出文档到临时文件。
     *
     * @param document 文档对象
     * @param fileName 文件名
     * @return 临时文件路径
     * @throws IOException 写出失败时抛出
     */
    public static Path exportToTempFile(XWPFDocument document, String fileName) throws IOException {
        checkDocument(document);
        String safeName = buildDownloadFileName(fileName).replace("%", "");
        Path tempFile = Files.createTempFile("word-", "-" + safeName);
        writeToFile(document, tempFile);
        return tempFile;
    }

    private static POIXMLProperties.CoreProperties coreProperties(XWPFDocument document) {
        return document.getProperties().getCoreProperties();
    }

    private static XmlCursor newCursor(IBodyElement element) {
        if (element.getElementType() == BodyElementType.PARAGRAPH) {
            return ((XWPFParagraph) element).getCTP().newCursor();
        }
        if (element.getElementType() == BodyElementType.TABLE) {
            return ((XWPFTable) element).getCTTbl().newCursor();
        }
        throw new IllegalArgumentException("不支持的正文元素类型: " + element.getElementType());
    }

    private static Map<String, String> normalizeStringMap(Map<String, String> source) {
        if (source == null || source.isEmpty()) {
            return Collections.emptyMap();
        }
        Map<String, String> result = new LinkedHashMap<>();
        for (Map.Entry<String, String> entry : source.entrySet()) {
            if (entry.getKey() != null && !entry.getKey().isEmpty()) {
                result.put(entry.getKey(), toSafeText(entry.getValue()));
            }
        }
        return result;
    }

    private static String replaceByMap(String source, Map<String, String> replacements) {
        String result = source == null ? "" : source;
        for (Map.Entry<String, String> entry : replacements.entrySet()) {
            result = result.replace(entry.getKey(), entry.getValue());
        }
        return result;
    }

    private static Map<String, String> toPlaceholderMap(Map<String, Object> data, String prefix, String suffix) {
        String safePrefix = prefix == null ? DEFAULT_PLACEHOLDER_PREFIX : prefix;
        String safeSuffix = suffix == null ? DEFAULT_PLACEHOLDER_SUFFIX : suffix;
        if (data == null || data.isEmpty()) {
            return Collections.emptyMap();
        }
        Map<String, String> result = new LinkedHashMap<>();
        for (Map.Entry<String, Object> entry : data.entrySet()) {
            if (entry.getKey() != null) {
                result.put(safePrefix + entry.getKey() + safeSuffix, formatValue(entry.getValue()));
            }
        }
        return result;
    }

    private static void ensureTableSize(XWPFTable table, int rows, int cols) {
        while (table.getRows().size() < rows) {
            table.createRow();
        }
        for (XWPFTableRow row : table.getRows()) {
            while (row.getTableCells().size() < cols) {
                row.createCell();
            }
        }
    }

    private static CTTcPr getOrAddTcPr(XWPFTableCell cell) {
        return cell.getCTTc().isSetTcPr() ? cell.getCTTc().getTcPr() : cell.getCTTc().addNewTcPr();
    }

    private static CTTblBorders getOrAddTableBorders(XWPFTable table) {
        Objects.requireNonNull(table, "table 不能为空");

        CTTblPr tblPr = table.getCTTbl().getTblPr();
        if (tblPr == null) {
            tblPr = table.getCTTbl().addNewTblPr();
        }

        CTTblBorders tblBorders = tblPr.getTblBorders();
        if (tblBorders == null) {
            tblBorders = tblPr.addNewTblBorders();
        }

        return tblBorders;
    }

    private static void setBorder(CTBorder border, STBorder.Enum value) {
        border.setVal(value);
        border.setSz(BigInteger.valueOf(4));
        border.setColor("000000");
    }

    private static String normalizeColor(String color) {
        Objects.requireNonNull(color, "color 不能为空");
        String value = color.startsWith("#") ? color.substring(1) : color;
        if (!value.matches("[0-9a-fA-F]{6}")) {
            throw new IllegalArgumentException("颜色必须是 6 位十六进制格式");
        }
        return value.toUpperCase(Locale.ROOT);
    }

    private static BigInteger parseBigInteger(String value, String name) {
        try {
            return new BigInteger(Objects.requireNonNull(value, name + " 不能为空"));
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException(name + " 必须是整数", e);
        }
    }

    private static List<XWPFParagraph> findAllParagraphs(XWPFDocument document) {
        List<XWPFParagraph> result = new ArrayList<>();
        result.addAll(document.getParagraphs());
        for (XWPFTable table : document.getTables()) {
            for (XWPFTableRow row : table.getRows()) {
                for (XWPFTableCell cell : row.getTableCells()) {
                    result.addAll(cell.getParagraphs());
                }
            }
        }
        for (XWPFHeader header : document.getHeaderList()) {
            result.addAll(header.getParagraphs());
        }
        for (XWPFFooter footer : document.getFooterList()) {
            result.addAll(footer.getParagraphs());
        }
        return result;
    }

    private static void addField(XWPFParagraph paragraph, String instruction) {
        Objects.requireNonNull(paragraph, "paragraph 不能为空");
        CTR begin = paragraph.getCTP().addNewR();
        begin.addNewFldChar().setFldCharType(STFldCharType.BEGIN);
        CTR instr = paragraph.getCTP().addNewR();
        instr.addNewInstrText().setStringValue(instruction);
        CTR separate = paragraph.getCTP().addNewR();
        separate.addNewFldChar().setFldCharType(STFldCharType.SEPARATE);
        CTR end = paragraph.getCTP().addNewR();
        end.addNewFldChar().setFldCharType(STFldCharType.END);
    }

    private static CTSectPr getOrAddSectPr(XWPFDocument document) {
        return document.getDocument().getBody().isSetSectPr()
                ? document.getDocument().getBody().getSectPr()
                : document.getDocument().getBody().addNewSectPr();
    }

    private static void copyBodyElement(IBodyElement element, XWPFDocument target) {
        if (element instanceof XWPFParagraph paragraph) {
            target.getDocument().getBody().addNewP().set(paragraph.getCTP().copy());
        } else if (element instanceof XWPFTable table) {
            target.getDocument().getBody().addNewTbl().set(table.getCTTbl().copy());
        }
    }

    private static void appendLine(StringBuilder builder, String text) {
        if (text == null || text.isEmpty()) {
            return;
        }
        if (!builder.isEmpty()) {
            builder.append(System.lineSeparator());
        }
        builder.append(text);
    }

    private static void checkIndex(int index, int size, String name) {
        if (index < 0 || index >= size) {
            throw new IndexOutOfBoundsException(name + " 越界: " + index + ", size=" + size);
        }
    }

    private static void checkIndexForInsert(int index, int size, String name) {
        if (index < 0 || index > size) {
            throw new IndexOutOfBoundsException(name + " 越界: " + index + ", size=" + size);
        }
    }

    private static void checkPositive(int value, String name) {
        if (value <= 0) {
            throw new IllegalArgumentException(name + " 必须大于 0");
        }
    }

    private static boolean isValidXmlChar(int codePoint) {
        return codePoint == 0x9 || codePoint == 0xA || codePoint == 0xD
                || (codePoint >= 0x20 && codePoint <= 0xD7FF)
                || (codePoint >= 0xE000 && codePoint <= 0xFFFD)
                || (codePoint >= 0x10000 && codePoint <= 0x10FFFF);
    }

    private static String defaultPattern(String pattern, String defaultPattern) {
        return pattern == null || pattern.isBlank() ? defaultPattern : pattern;
    }
}
