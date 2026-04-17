package local.ateng.java.customutils.utils;

import org.apache.tika.Tika;
import org.apache.tika.mime.MimeTypeException;
import org.apache.tika.mime.MimeTypes;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Locale;

/**
 * 文件类型工具类。
 * <p>
 * 提供文件 MIME 类型识别、扩展名映射、文件大类判断，以及常见文件类型判断。
 *
 * @author Ateng
 * @since 2025-07-21
 */
public final class FileTypeUtil {

    /**
     * MIME 识别器。
     */
    private static final Tika TIKA = new Tika();

    /**
     * MIME 类型映射器。
     */
    private static final MimeTypes MIME_TYPES = MimeTypes.getDefaultMimeTypes();

    /**
     * 未知类型。
     */
    public static final String MIME_UNKNOWN = "unknown";

    /**
     * 图片类型。
     */
    public static final String CATEGORY_IMAGE = "image";

    /**
     * 视频类型。
     */
    public static final String CATEGORY_VIDEO = "video";

    /**
     * 音频类型。
     */
    public static final String CATEGORY_AUDIO = "audio";

    /**
     * 文本类型。
     */
    public static final String CATEGORY_TEXT = "text";

    /**
     * 应用类型。
     */
    public static final String CATEGORY_APPLICATION = "application";

    /**
     * 禁止实例化工具类。
     */
    private FileTypeUtil() {
        throw new UnsupportedOperationException("工具类不可实例化");
    }

    /**
     * 获取文件 MIME 类型（优先按文件内容识别）。
     *
     * @param file 文件
     * @return MIME 类型，识别失败返回 null
     */
    public static String getMimeType(File file) {
        if (!isReadableFile(file)) {
            return null;
        }
        try {
            return TIKA.detect(file);
        } catch (IOException e) {
            return null;
        }
    }

    /**
     * 获取文件 MIME 类型。
     * <p>
     * 若路径对应真实文件，则按文件内容识别；否则按文件名/路径字符串推断。
     *
     * @param path 文件路径
     * @return MIME 类型，识别失败返回 null
     */
    public static String getMimeType(String path) {
        if (StringUtil.isBlank(path)) {
            return null;
        }
        File file = new File(path);
        if (isReadableFile(file)) {
            return getMimeType(file);
        }
        try {
            return TIKA.detect(path);
        } catch (Exception e) {
            return null;
        }
    }

    /**
     * 获取文件 MIME 类型。
     *
     * @param path 文件路径
     * @return MIME 类型，识别失败返回 null
     */
    public static String getMimeType(Path path) {
        if (path == null || Files.notExists(path) || Files.isDirectory(path)) {
            return null;
        }
        return getMimeType(path.toFile());
    }

    /**
     * 获取输入流的 MIME 类型。
     * <p>
     * 注意：该方法会消耗输入流中的数据。
     *
     * @param inputStream 输入流
     * @return MIME 类型，识别失败返回 null
     */
    public static String getMimeType(InputStream inputStream) {
        if (inputStream == null) {
            return null;
        }

        try {
            byte[] bytes = toByteArray(inputStream);
            return getMimeType(bytes);
        } catch (Exception e) {
            return null;
        }
    }

    /**
     * 将输入流读取为字节数组。
     *
     * @param inputStream 输入流
     * @return 字节数组
     * @throws IOException IO异常
     */
    private static byte[] toByteArray(InputStream inputStream) throws IOException {
        byte[] buffer = new byte[8192];
        int len;

        try (java.io.ByteArrayOutputStream output = new java.io.ByteArrayOutputStream()) {
            while ((len = inputStream.read(buffer)) != -1) {
                output.write(buffer, 0, len);
            }
            return output.toByteArray();
        }
    }

    /**
     * 获取字节数组的 MIME 类型。
     *
     * @param bytes 字节数组
     * @return MIME 类型，识别失败返回 null
     */
    public static String getMimeType(byte[] bytes) {
        if (ObjectUtil.isEmpty(bytes)) {
            return null;
        }
        try {
            return TIKA.detect(bytes);
        } catch (Exception e) {
            return null;
        }
    }

    /**
     * 获取文件扩展名（例如：.pdf、.png）。
     * <p>
     * 基于 MIME 类型映射得到标准扩展名。
     *
     * @param file 文件
     * @return 文件扩展名，包含点；识别失败返回 null
     */
    public static String getExtension(File file) {
        return getExtensionByMimeType(getMimeType(file));
    }

    /**
     * 获取文件扩展名（例如：.pdf、.png）。
     *
     * @param path 文件路径
     * @return 文件扩展名，包含点；识别失败返回 null
     */
    public static String getExtension(String path) {
        return getExtensionByMimeType(getMimeType(path));
    }

    /**
     * 获取文件扩展名（例如：.pdf、.png）。
     *
     * @param path 文件路径
     * @return 文件扩展名，包含点；识别失败返回 null
     */
    public static String getExtension(Path path) {
        return getExtensionByMimeType(getMimeType(path));
    }

    /**
     * 获取输入流对应的文件扩展名（例如：.pdf、.png）。
     *
     * @param inputStream 输入流
     * @return 文件扩展名，包含点；识别失败返回 null
     */
    public static String getExtension(InputStream inputStream) {
        return getExtensionByMimeType(getMimeType(inputStream));
    }

    /**
     * 获取字节数组对应的文件扩展名（例如：.pdf、.png）。
     *
     * @param bytes 字节数组
     * @return 文件扩展名，包含点；识别失败返回 null
     */
    public static String getExtension(byte[] bytes) {
        return getExtensionByMimeType(getMimeType(bytes));
    }

    /**
     * 获取文件扩展名。
     *
     * @param mimeType MIME 类型
     * @return 文件扩展名，包含点；识别失败返回 null
     */
    private static String getExtensionByMimeType(String mimeType) {
        if (StringUtil.isBlank(mimeType)) {
            return null;
        }
        try {
            return MIME_TYPES.forName(mimeType).getExtension();
        } catch (MimeTypeException e) {
            return null;
        } catch (Exception e) {
            return null;
        }
    }

    /**
     * 获取文件大类：image、video、audio、text、application、unknown。
     *
     * @param file 文件
     * @return 文件大类
     */
    public static String getFileCategory(File file) {
        return getFileCategoryByMimeType(getMimeType(file));
    }

    /**
     * 获取文件大类：image、video、audio、text、application、unknown。
     *
     * @param path 文件路径
     * @return 文件大类
     */
    public static String getFileCategory(String path) {
        return getFileCategoryByMimeType(getMimeType(path));
    }

    /**
     * 获取文件大类：image、video、audio、text、application、unknown。
     *
     * @param path 文件路径
     * @return 文件大类
     */
    public static String getFileCategory(Path path) {
        return getFileCategoryByMimeType(getMimeType(path));
    }

    /**
     * 获取文件大类：image、video、audio、text、application、unknown。
     *
     * @param inputStream 输入流
     * @return 文件大类
     */
    public static String getFileCategory(InputStream inputStream) {
        return getFileCategoryByMimeType(getMimeType(inputStream));
    }

    /**
     * 获取文件大类：image、video、audio、text、application、unknown。
     *
     * @param bytes 字节数组
     * @return 文件大类
     */
    public static String getFileCategory(byte[] bytes) {
        return getFileCategoryByMimeType(getMimeType(bytes));
    }

    /**
     * 根据 MIME 类型获取文件大类。
     *
     * @param mimeType MIME 类型
     * @return 文件大类
     */
    private static String getFileCategoryByMimeType(String mimeType) {
        if (StringUtil.isBlank(mimeType)) {
            return MIME_UNKNOWN;
        }
        int index = mimeType.indexOf('/');
        if (index <= 0) {
            return MIME_UNKNOWN;
        }
        return mimeType.substring(0, index).toLowerCase(Locale.ROOT);
    }

    /**
     * 判断是否为图片文件。
     *
     * @param file 文件
     * @return true-是；false-否
     */
    public static boolean isImage(File file) {
        return CATEGORY_IMAGE.equals(getFileCategory(file));
    }

    /**
     * 判断是否为图片文件。
     *
     * @param path 文件路径
     * @return true-是；false-否
     */
    public static boolean isImage(String path) {
        return CATEGORY_IMAGE.equals(getFileCategory(path));
    }

    /**
     * 判断是否为图片文件。
     *
     * @param path 文件路径
     * @return true-是；false-否
     */
    public static boolean isImage(Path path) {
        return CATEGORY_IMAGE.equals(getFileCategory(path));
    }

    /**
     * 判断是否为图片文件。
     *
     * @param inputStream 输入流
     * @return true-是；false-否
     */
    public static boolean isImage(InputStream inputStream) {
        return CATEGORY_IMAGE.equals(getFileCategory(inputStream));
    }

    /**
     * 判断是否为图片文件。
     *
     * @param bytes 字节数组
     * @return true-是；false-否
     */
    public static boolean isImage(byte[] bytes) {
        return CATEGORY_IMAGE.equals(getFileCategory(bytes));
    }

    /**
     * 判断是否为视频文件。
     *
     * @param file 文件
     * @return true-是；false-否
     */
    public static boolean isVideo(File file) {
        return CATEGORY_VIDEO.equals(getFileCategory(file));
    }

    /**
     * 判断是否为视频文件。
     *
     * @param path 文件路径
     * @return true-是；false-否
     */
    public static boolean isVideo(String path) {
        return CATEGORY_VIDEO.equals(getFileCategory(path));
    }

    /**
     * 判断是否为视频文件。
     *
     * @param path 文件路径
     * @return true-是；false-否
     */
    public static boolean isVideo(Path path) {
        return CATEGORY_VIDEO.equals(getFileCategory(path));
    }

    /**
     * 判断是否为视频文件。
     *
     * @param inputStream 输入流
     * @return true-是；false-否
     */
    public static boolean isVideo(InputStream inputStream) {
        return CATEGORY_VIDEO.equals(getFileCategory(inputStream));
    }

    /**
     * 判断是否为视频文件。
     *
     * @param bytes 字节数组
     * @return true-是；false-否
     */
    public static boolean isVideo(byte[] bytes) {
        return CATEGORY_VIDEO.equals(getFileCategory(bytes));
    }

    /**
     * 判断是否为音频文件。
     *
     * @param file 文件
     * @return true-是；false-否
     */
    public static boolean isAudio(File file) {
        return CATEGORY_AUDIO.equals(getFileCategory(file));
    }

    /**
     * 判断是否为音频文件。
     *
     * @param path 文件路径
     * @return true-是；false-否
     */
    public static boolean isAudio(String path) {
        return CATEGORY_AUDIO.equals(getFileCategory(path));
    }

    /**
     * 判断是否为音频文件。
     *
     * @param path 文件路径
     * @return true-是；false-否
     */
    public static boolean isAudio(Path path) {
        return CATEGORY_AUDIO.equals(getFileCategory(path));
    }

    /**
     * 判断是否为音频文件。
     *
     * @param inputStream 输入流
     * @return true-是；false-否
     */
    public static boolean isAudio(InputStream inputStream) {
        return CATEGORY_AUDIO.equals(getFileCategory(inputStream));
    }

    /**
     * 判断是否为音频文件。
     *
     * @param bytes 字节数组
     * @return true-是；false-否
     */
    public static boolean isAudio(byte[] bytes) {
        return CATEGORY_AUDIO.equals(getFileCategory(bytes));
    }

    /**
     * 判断是否为文本文件。
     *
     * @param file 文件
     * @return true-是；false-否
     */
    public static boolean isText(File file) {
        return CATEGORY_TEXT.equals(getFileCategory(file));
    }

    /**
     * 判断是否为文本文件。
     *
     * @param path 文件路径
     * @return true-是；false-否
     */
    public static boolean isText(String path) {
        return CATEGORY_TEXT.equals(getFileCategory(path));
    }

    /**
     * 判断是否为文本文件。
     *
     * @param path 文件路径
     * @return true-是；false-否
     */
    public static boolean isText(Path path) {
        return CATEGORY_TEXT.equals(getFileCategory(path));
    }

    /**
     * 判断是否为文本文件。
     *
     * @param inputStream 输入流
     * @return true-是；false-否
     */
    public static boolean isText(InputStream inputStream) {
        return CATEGORY_TEXT.equals(getFileCategory(inputStream));
    }

    /**
     * 判断是否为文本文件。
     *
     * @param bytes 字节数组
     * @return true-是；false-否
     */
    public static boolean isText(byte[] bytes) {
        return CATEGORY_TEXT.equals(getFileCategory(bytes));
    }

    /**
     * 判断是否为 PDF 文件。
     *
     * @param file 文件
     * @return true-是；false-否
     */
    public static boolean isPdf(File file) {
        return "application/pdf".equalsIgnoreCase(getMimeType(file));
    }

    /**
     * 判断是否为 PDF 文件。
     *
     * @param path 文件路径
     * @return true-是；false-否
     */
    public static boolean isPdf(String path) {
        return "application/pdf".equalsIgnoreCase(getMimeType(path));
    }

    /**
     * 判断是否为 PDF 文件。
     *
     * @param path 文件路径
     * @return true-是；false-否
     */
    public static boolean isPdf(Path path) {
        return "application/pdf".equalsIgnoreCase(getMimeType(path));
    }

    /**
     * 判断是否为 PDF 文件。
     *
     * @param inputStream 输入流
     * @return true-是；false-否
     */
    public static boolean isPdf(InputStream inputStream) {
        return "application/pdf".equalsIgnoreCase(getMimeType(inputStream));
    }

    /**
     * 判断是否为 PDF 文件。
     *
     * @param bytes 字节数组
     * @return true-是；false-否
     */
    public static boolean isPdf(byte[] bytes) {
        return "application/pdf".equalsIgnoreCase(getMimeType(bytes));
    }

    /**
     * 判断是否为 Office 文档。
     * <p>
     * 包括 doc/docx/xls/xlsx/ppt/pptx 等常见格式。
     *
     * @param file 文件
     * @return true-是；false-否
     */
    public static boolean isOfficeDocument(File file) {
        return isOfficeMimeType(getMimeType(file));
    }

    /**
     * 判断是否为 Office 文档。
     *
     * @param path 文件路径
     * @return true-是；false-否
     */
    public static boolean isOfficeDocument(String path) {
        return isOfficeMimeType(getMimeType(path));
    }

    /**
     * 判断是否为 Office 文档。
     *
     * @param path 文件路径
     * @return true-是；false-否
     */
    public static boolean isOfficeDocument(Path path) {
        return isOfficeMimeType(getMimeType(path));
    }

    /**
     * 判断是否为 Office 文档。
     *
     * @param inputStream 输入流
     * @return true-是；false-否
     */
    public static boolean isOfficeDocument(InputStream inputStream) {
        return isOfficeMimeType(getMimeType(inputStream));
    }

    /**
     * 判断是否为 Office 文档。
     *
     * @param bytes 字节数组
     * @return true-是；false-否
     */
    public static boolean isOfficeDocument(byte[] bytes) {
        return isOfficeMimeType(getMimeType(bytes));
    }

    /**
     * 判断是否为 Office 文档 MIME 类型。
     *
     * @param mimeType MIME 类型
     * @return true-是；false-否
     */
    private static boolean isOfficeMimeType(String mimeType) {
        if (StringUtil.isBlank(mimeType)) {
            return false;
        }

        String lowerMimeType = mimeType.toLowerCase(Locale.ROOT);

        return lowerMimeType.contains("msword")
                || lowerMimeType.contains("ms-excel")
                || lowerMimeType.contains("ms-powerpoint")
                || lowerMimeType.contains("officedocument")
                || lowerMimeType.contains("wordprocessingml")
                || lowerMimeType.contains("spreadsheetml")
                || lowerMimeType.contains("presentationml");
    }

    /**
     * 判断文件是否可读取。
     *
     * @param file 文件
     * @return true-可读取；false-不可读取
     */
    private static boolean isReadableFile(File file) {
        return file != null && file.exists() && file.isFile() && file.canRead();
    }

    /**
     * 判断是否为 ZIP 压缩包。
     *
     * @param file 文件
     * @return true-是；false-否
     */
    public static boolean isZip(File file) {
        return isZipMimeType(getMimeType(file));
    }

    public static boolean isZip(String path) {
        return isZipMimeType(getMimeType(path));
    }

    public static boolean isZip(Path path) {
        return isZipMimeType(getMimeType(path));
    }

    public static boolean isZip(InputStream inputStream) {
        return isZipMimeType(getMimeType(inputStream));
    }

    public static boolean isZip(byte[] bytes) {
        return isZipMimeType(getMimeType(bytes));
    }

    private static boolean isZipMimeType(String mimeType) {
        if (StringUtil.isBlank(mimeType)) {
            return false;
        }
        return "application/zip".equalsIgnoreCase(mimeType)
                || mimeType.contains("compressed")
                || mimeType.contains("zip");
    }

    /**
     * 判断是否为 Excel 文件（xls/xlsx）。
     */
    public static boolean isExcel(File file) {
        return isExcelMimeType(getMimeType(file));
    }

    public static boolean isExcel(String path) {
        return isExcelMimeType(getMimeType(path));
    }

    public static boolean isExcel(Path path) {
        return isExcelMimeType(getMimeType(path));
    }

    public static boolean isExcel(InputStream inputStream) {
        return isExcelMimeType(getMimeType(inputStream));
    }

    public static boolean isExcel(byte[] bytes) {
        return isExcelMimeType(getMimeType(bytes));
    }

    private static boolean isExcelMimeType(String mimeType) {
        if (StringUtil.isBlank(mimeType)) {
            return false;
        }
        String m = mimeType.toLowerCase(Locale.ROOT);
        return m.contains("excel") || m.contains("spreadsheetml");
    }

    /**
     * 判断是否为 Word 文件（doc/docx）。
     */
    public static boolean isWord(File file) {
        return isWordMimeType(getMimeType(file));
    }

    public static boolean isWord(String path) {
        return isWordMimeType(getMimeType(path));
    }

    public static boolean isWord(Path path) {
        return isWordMimeType(getMimeType(path));
    }

    public static boolean isWord(InputStream inputStream) {
        return isWordMimeType(getMimeType(inputStream));
    }

    public static boolean isWord(byte[] bytes) {
        return isWordMimeType(getMimeType(bytes));
    }

    private static boolean isWordMimeType(String mimeType) {
        if (StringUtil.isBlank(mimeType)) {
            return false;
        }
        String m = mimeType.toLowerCase(Locale.ROOT);
        return m.contains("msword") || m.contains("wordprocessingml");
    }

    /**
     * 判断是否为 PPT 文件（ppt/pptx）。
     */
    public static boolean isPpt(File file) {
        return isPptMimeType(getMimeType(file));
    }

    public static boolean isPpt(String path) {
        return isPptMimeType(getMimeType(path));
    }

    public static boolean isPpt(Path path) {
        return isPptMimeType(getMimeType(path));
    }

    public static boolean isPpt(InputStream inputStream) {
        return isPptMimeType(getMimeType(inputStream));
    }

    public static boolean isPpt(byte[] bytes) {
        return isPptMimeType(getMimeType(bytes));
    }

    private static boolean isPptMimeType(String mimeType) {
        if (StringUtil.isBlank(mimeType)) {
            return false;
        }
        String m = mimeType.toLowerCase(Locale.ROOT);
        return m.contains("powerpoint") || m.contains("presentationml");
    }

    /**
     * 判断是否为 CSV 文件。
     */
    public static boolean isCsv(File file) {
        return isCsvMimeType(getMimeType(file));
    }

    public static boolean isCsv(String path) {
        return isCsvMimeType(getMimeType(path));
    }

    public static boolean isCsv(Path path) {
        return isCsvMimeType(getMimeType(path));
    }

    public static boolean isCsv(InputStream inputStream) {
        return isCsvMimeType(getMimeType(inputStream));
    }

    public static boolean isCsv(byte[] bytes) {
        return isCsvMimeType(getMimeType(bytes));
    }

    private static boolean isCsvMimeType(String mimeType) {
        if (StringUtil.isBlank(mimeType)) {
            return false;
        }
        return mimeType.toLowerCase(Locale.ROOT).contains("csv");
    }

    /**
     * 判断是否为 JSON 文件。
     */
    public static boolean isJson(File file) {
        return isJsonMimeType(getMimeType(file));
    }

    public static boolean isJson(String path) {
        return isJsonMimeType(getMimeType(path));
    }

    public static boolean isJson(Path path) {
        return isJsonMimeType(getMimeType(path));
    }

    public static boolean isJson(InputStream inputStream) {
        return isJsonMimeType(getMimeType(inputStream));
    }

    public static boolean isJson(byte[] bytes) {
        return isJsonMimeType(getMimeType(bytes));
    }

    private static boolean isJsonMimeType(String mimeType) {
        if (StringUtil.isBlank(mimeType)) {
            return false;
        }
        String m = mimeType.toLowerCase(Locale.ROOT);
        return m.contains("json");
    }

    /**
     * 判断是否为 XML 文件。
     */
    public static boolean isXml(File file) {
        return isXmlMimeType(getMimeType(file));
    }

    public static boolean isXml(String path) {
        return isXmlMimeType(getMimeType(path));
    }

    public static boolean isXml(Path path) {
        return isXmlMimeType(getMimeType(path));
    }

    public static boolean isXml(InputStream inputStream) {
        return isXmlMimeType(getMimeType(inputStream));
    }

    public static boolean isXml(byte[] bytes) {
        return isXmlMimeType(getMimeType(bytes));
    }

    private static boolean isXmlMimeType(String mimeType) {
        if (StringUtil.isBlank(mimeType)) {
            return false;
        }
        String m = mimeType.toLowerCase(Locale.ROOT);
        return m.contains("xml");
    }

    /**
     * 判断是否为 HTML 文件。
     */
    public static boolean isHtml(File file) {
        return isHtmlMimeType(getMimeType(file));
    }

    public static boolean isHtml(String path) {
        return isHtmlMimeType(getMimeType(path));
    }

    public static boolean isHtml(Path path) {
        return isHtmlMimeType(getMimeType(path));
    }

    public static boolean isHtml(InputStream inputStream) {
        return isHtmlMimeType(getMimeType(inputStream));
    }

    public static boolean isHtml(byte[] bytes) {
        return isHtmlMimeType(getMimeType(bytes));
    }

    private static boolean isHtmlMimeType(String mimeType) {
        if (StringUtil.isBlank(mimeType)) {
            return false;
        }
        String m = mimeType.toLowerCase(Locale.ROOT);
        return m.contains("html");
    }

}