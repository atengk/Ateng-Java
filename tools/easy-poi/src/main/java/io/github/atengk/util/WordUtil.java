package io.github.atengk.util;

import cn.afterturn.easypoi.word.entity.MyXWPFDocument;
import cn.afterturn.easypoi.word.parse.ParseWord07;
import org.apache.poi.xwpf.usermodel.XWPFDocument;

import javax.servlet.http.HttpServletResponse;
import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Map;

/**
 * Word 模板填充工具
 *
 * @author 孔余
 * @since 2026-01-22
 */
public final class WordUtil {

    private static final String DOCX_CONTENT_TYPE =
            "application/vnd.openxmlformats-officedocument.wordprocessingml.document";

    private WordUtil() {
        // 工具类不允许实例化
    }

    /**
     * 将 InputStream 转为 MyXWPFDocument，并填充 Map 数据。
     *
     * @param inputStream Word 模板输入流（必须为 docx）
     * @param map         数据模型
     */
    public static XWPFDocument export(InputStream inputStream, Map<String, Object> map) {
        if (inputStream == null) {
            throw new IllegalArgumentException("模板输入流不能为空");
        }
        if (map == null) {
            throw new IllegalArgumentException("数据模型 Map 不能为空");
        }

        try {
            // 按你要求这里必须是 MyXWPFDocument
            XWPFDocument document = new MyXWPFDocument(inputStream);

            // 调用 EasyPoi 填充
            new ParseWord07().parseWord(document, map);

            return document;
        } catch (Exception e) {
            throw new RuntimeException("Word 模板填充失败：" + e.getMessage(), e);
        }
    }

    /**
     * 将文档写入到指定文件路径。
     *
     * <p>内部委托给 {@link #write(XWPFDocument, Path)} 保证逻辑统一。</p>
     *
     * @param document Word 文档对象（不能为空）
     * @param target   文件路径字符串（不能为空、非空白）
     * @throws IllegalArgumentException 参数为空或路径非法时抛出
     * @throws RuntimeException         写入失败时抛出
     */
    public static void write(XWPFDocument document, String target) {
        validateDocument(document);
        if (target == null || target.trim().isEmpty()) {
            throw new IllegalArgumentException("输出路径 String target 不能为空或空白");
        }

        try {
            write(document, Paths.get(target));
        } catch (Exception e) {
            throw new RuntimeException("Word 文档写入 String 路径失败：" + target, e);
        }
    }

    /**
     * 将文档写入到指定文件。
     *
     * @param document  Word 文档对象（不能为空）
     * @param target    输出目标文件对象（不能为空）
     * @throws IllegalArgumentException 参数为空时抛出
     * @throws RuntimeException         写入失败时抛出
     */
    public static void write(XWPFDocument document, File target) {
        validateDocument(document);
        if (target == null) {
            throw new IllegalArgumentException("输出目标 File 不能为空");
        }

        try (OutputStream os = new FileOutputStream(target)) {
            document.write(os);
        } catch (IOException e) {
            throw new RuntimeException("Word 文档写入文件失败：" + target.getAbsolutePath(), e);
        }
    }

    /**
     * 将文档写入到指定路径。
     *
     * @param document  Word 文档对象（不能为空）
     * @param path      输出路径（不能为空）
     * @throws IllegalArgumentException 参数为空时抛出
     * @throws RuntimeException         写入失败时抛出
     */
    public static void write(XWPFDocument document, Path path) {
        validateDocument(document);
        if (path == null) {
            throw new IllegalArgumentException("输出目标 Path 不能为空");
        }

        try (OutputStream os = Files.newOutputStream(path)) {
            document.write(os);
        } catch (IOException e) {
            throw new RuntimeException("Word 文档写入路径失败：" + path.toAbsolutePath(), e);
        }
    }

    /**
     * 将文档写入到指定输出流。
     *
     * <p>该方法不负责关闭传入的 {@link OutputStream}，
     * 调用者需自行管理输出流生命周期。</p>
     *
     * @param document  Word 文档对象（不能为空）
     * @param os        输出流（不能为空）
     * @throws IllegalArgumentException 参数为空时抛出
     * @throws RuntimeException         写入失败时抛出
     */
    public static void write(XWPFDocument document, OutputStream os) {
        validateDocument(document);
        if (os == null) {
            throw new IllegalArgumentException("输出流 OutputStream 不能为空");
        }

        try {
            document.write(os);
        } catch (IOException e) {
            throw new RuntimeException("Word 文档写入 OutputStream 失败", e);
        }
    }

    /**
     * 将文档写入到 HTTP 响应中，实现浏览器下载。
     *
     * @param document  Word 文档对象（不能为空）
     * @param response  HTTP 响应对象（不能为空）
     * @param filename  下载文件名（不能为空，将自动附加 .docx）
     * @throws IllegalArgumentException 参数为空时抛出
     * @throws RuntimeException         写入失败时抛出
     */
    public static void write(XWPFDocument document, HttpServletResponse response, String filename) {
        validateDocument(document);
        if (response == null) {
            throw new IllegalArgumentException("HttpServletResponse 不能为空");
        }
        if (filename == null || filename.trim().isEmpty()) {
            throw new IllegalArgumentException("下载文件名不能为空");
        }

        try {
            response.setContentType(DOCX_CONTENT_TYPE);
            response.setHeader("Content-Disposition",
                    "attachment; filename=\"" + encodeFilename(filename) + "\"");

            try (OutputStream os = response.getOutputStream()) {
                document.write(os);
            }
        } catch (IOException e) {
            throw new RuntimeException("Word 文档写入 HTTP 响应失败", e);
        }
    }

    /**
     * 校验文档是否为空。
     */
    private static void validateDocument(XWPFDocument document) {
        if (document == null) {
            throw new IllegalArgumentException("XWPFDocument 文档不能为空");
        }
    }

    /**
     * 处理文件名的基本兼容（仅处理空格与基础字符，复杂场景可自行扩展）。
     */
    private static String encodeFilename(String filename) {
        return filename.trim().replace(" ", "_") + ".docx";
    }

}
