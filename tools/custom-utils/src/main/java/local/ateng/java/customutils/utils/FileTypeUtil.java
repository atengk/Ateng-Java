package local.ateng.java.customutils.utils;

import org.apache.tika.Tika;
import org.apache.tika.mime.MimeTypes;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.Locale;

/**
 * 文件类型工具类
 *
 * @author Ateng
 * @since 2025-07-21
 */
public final class FileTypeUtil {

    private static final Tika tika = new Tika();

    /**
     * 禁止实例化工具类
     */
    private FileTypeUtil() {
        throw new UnsupportedOperationException("工具类不可实例化");
    }

    /**
     * 获取文件 MIME 类型（如：image/png、application/pdf）
     *
     * @param file 文件
     * @return MIME 类型
     */
    public static String getMimeType(File file) {
        if (file == null || !file.exists()) {
            return null;
        }
        try {
            return tika.detect(file);
        } catch (IOException e) {
            return null;
        }
    }

    /**
     * 获取文件 MIME 类型（如：image/png、application/pdf）
     *
     * @param path 文件路径
     * @return MIME 类型
     */
    public static String getMimeType(String path) {
        if (path == null || path.trim().isEmpty()) {
            return null;
        }
        return tika.detect(path);
    }

    /**
     * 获取输入流的 MIME 类型
     *
     * @param inputStream 输入流
     * @return MIME 类型
     */
    public static String getMimeType(InputStream inputStream) {
        if (inputStream == null) {
            return null;
        }
        try {
            return tika.detect(inputStream);
        } catch (IOException e) {
            return null;
        }
    }

    /**
     * 获取文件扩展名（如：.pdf、.png）
     *
     * @param file 文件
     * @return 文件扩展名，包含点
     */
    public static String getExtension(File file) {
        String mimeType = getMimeType(file);
        if (mimeType == null) {
            return null;
        }
        try {
            return MimeTypes.getDefaultMimeTypes().forName(mimeType).getExtension();
        } catch (Exception e) {
            return null;
        }
    }

    /**
     * 获取文件扩展名（如：.pdf、.png）
     *
     * @param path 文件路径
     * @return 文件扩展名，包含点
     */
    public static String getExtension(String path) {
        String mimeType = getMimeType(path);
        if (mimeType == null) {
            return null;
        }
        try {
            return MimeTypes.getDefaultMimeTypes().forName(mimeType).getExtension();
        } catch (Exception e) {
            return null;
        }
    }

    /**
     * 获取通用文件类别：image、video、audio、text、application
     *
     * @param file 文件
     * @return 文件大类，出错返回 unknown
     */
    public static String getFileCategory(File file) {
        String mimeType = getMimeType(file);
        if (mimeType == null) {
            return "unknown";
        }
        return mimeType.split("/")[0].toLowerCase(Locale.ROOT);
    }

    public static boolean isImage(File file) {
        return "image".equals(getFileCategory(file));
    }

    public static boolean isVideo(File file) {
        return "video".equals(getFileCategory(file));
    }

    public static boolean isAudio(File file) {
        return "audio".equals(getFileCategory(file));
    }

    public static boolean isText(File file) {
        return "text".equals(getFileCategory(file));
    }

    public static boolean isPdf(File file) {
        return "application/pdf".equalsIgnoreCase(getMimeType(file));
    }

    public static boolean isOfficeDocument(File file) {
        String mime = getMimeType(file);
        if (mime == null) {
            return false;
        }
        return mime.contains("msword") || mime.contains("officedocument");
    }

}
