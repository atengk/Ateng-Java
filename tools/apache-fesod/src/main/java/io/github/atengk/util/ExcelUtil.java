package io.github.atengk.util;

import org.apache.fesod.sheet.ExcelWriter;
import org.apache.fesod.sheet.FesodSheet;
import org.apache.fesod.sheet.write.builder.ExcelWriterBuilder;
import org.apache.fesod.sheet.write.builder.ExcelWriterSheetBuilder;
import org.apache.fesod.sheet.write.handler.WriteHandler;
import org.apache.fesod.sheet.write.metadata.WriteSheet;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.multipart.MultipartFile;

import javax.servlet.http.HttpServletResponse;
import java.io.*;
import java.net.URL;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Excel 工具类
 *
 * <p>
 * 提供与 Excel 读写相关的常用操作方法，可按需扩展。
 * 支持基础导出、表头构建、多 Sheet 处理、输出流适配等能力。
 * 适用于服务端常见 Excel 处理场景。
 * </p>
 *
 * @author 孔余
 * @since 2026-01-27
 */
public class ExcelUtil {

    private static final Logger log = LoggerFactory.getLogger(ExcelUtil.class);

    private ExcelUtil() {
    }

    /*---------------------------------------------
     * 实体导出方法区域
     *---------------------------------------------*/

    /**
     * 实体导出（单 Sheet）
     *
     * <p>
     * 使用 Fesod 注解驱动模式，将实体列表直接导出为 Excel。
     * 表头、样式、格式全部由实体类上的注解控制。
     * </p>
     *
     * @param out        输出流（不可为空）
     * @param dataList   实体数据列表（可为空）
     * @param clazz      实体类型（不可为空）
     * @param sheetName  Sheet 名称（不可为空）
     * @param handlers   可选 WriteHandler 扩展
     * @param <T>        实体泛型类型
     */
    public static <T> void export(OutputStream out,
                                  List<T> dataList,
                                  Class<T> clazz,
                                  String sheetName,
                                  WriteHandler... handlers) {

        if (out == null) {
            log.error("[export] 输出流不能为空");
            throw new IllegalArgumentException("输出流不能为空");
        }
        if (clazz == null) {
            log.error("[export] 实体类型 clazz 不能为空");
            throw new IllegalArgumentException("实体类型不能为空");
        }

        List<T> rows = dataList == null ? Collections.emptyList() : dataList;

        log.info("[export] 开始实体单 Sheet 导出，entity={}，rows={}",
                clazz.getSimpleName(), rows.size());

        ExcelWriterSheetBuilder writer = FesodSheet.write(out, clazz)
                .sheet(sheetName);

        if (handlers != null && handlers.length > 0) {
            for (WriteHandler handler : handlers) {
                if (handler != null) {
                    writer.registerWriteHandler(handler);
                }
            }
        }

        writer.doWrite(rows);
        log.info("[export] 实体单 Sheet 导出完成");
    }

    /**
     * 实体导出（多 Sheet）
     *
     * <p>
     * 每个 Sheet 对应一个实体类型及其数据集合，
     * 适用于一个 Excel 中包含多个不同结构实体的场景。
     * </p>
     *
     * @param out           输出流（不可为空）
     * @param sheetDataList 多 Sheet 数据集合（不可为空/空集合）
     * @param handlers      可选全局 WriteHandler
     */
    public static void exportMultiSheet(OutputStream out,
                                        List<EntitySheetData<?>> sheetDataList,
                                        WriteHandler... handlers) {

        if (out == null) {
            log.error("[exportMultiSheet] 输出流不能为空");
            throw new IllegalArgumentException("输出流不能为空");
        }
        if (sheetDataList == null || sheetDataList.isEmpty()) {
            log.error("[exportMultiSheet] Sheet 数据不能为空");
            throw new IllegalArgumentException("Sheet 数据不能为空");
        }

        log.info("[exportMultiSheet] 开始实体多 Sheet 导出，共 {} 个 Sheet", sheetDataList.size());

        ExcelWriterBuilder builder = FesodSheet.write(out);

        if (handlers != null && handlers.length > 0) {
            for (WriteHandler handler : handlers) {
                if (handler != null) {
                    builder.registerWriteHandler(handler);
                }
            }
        }

        try (ExcelWriter excelWriter = builder.build()) {

            for (int i = 0; i < sheetDataList.size(); i++) {
                EntitySheetData<?> sd = sheetDataList.get(i);

                if (sd == null) {
                    log.warn("[exportMultiSheet] 第 {} 个 SheetData 为 null，已跳过", i);
                    continue;
                }
                if (sd.getClazz() == null) {
                    log.error("[exportMultiSheet] 第 {} 个 Sheet 的 clazz 为空", i);
                    throw new IllegalArgumentException("Sheet 实体类型不能为空");
                }

                List<?> rows = sd.getDataList() == null
                        ? Collections.emptyList()
                        : sd.getDataList();

                WriteSheet sheet = FesodSheet.writerSheet(i, sd.getSheetName())
                        .head(sd.getClazz())
                        .build();

                excelWriter.write(rows, sheet);

                log.info("[exportMultiSheet] Sheet[{}] 导出成功，名称='{}'，实体={}，行数={}",
                        i, sd.getSheetName(),
                        sd.getClazz().getSimpleName(),
                        rows.size());
            }
        }

        log.info("[exportMultiSheet] 实体多 Sheet 导出完成");
    }

    /**
     * 实体导出到浏览器（单 Sheet）
     *
     * @param response  HttpServletResponse 对象（不可为空）
     * @param fileName  下载文件名，例如 "用户列表.xlsx"（不可为空）
     * @param dataList  实体数据列表（可为空）
     * @param clazz     实体类型（不可为空）
     * @param sheetName Sheet 名称（不可为空）
     * @param handlers  可选 WriteHandler
     * @param <T>       实体泛型类型
     */
    public static <T> void exportToResponse(HttpServletResponse response,
                                            String fileName,
                                            List<T> dataList,
                                            Class<T> clazz,
                                            String sheetName,
                                            WriteHandler... handlers) {
        Objects.requireNonNull(response, "HttpServletResponse 不能为空");
        try (OutputStream out = prepareResponseOutputStream(response, fileName)) {
            export(out, dataList, clazz, sheetName, handlers);
            out.flush();
            log.info("[exportToResponse] 浏览器实体下载成功，文件名={}", fileName);
        } catch (Exception e) {
            log.error("[exportToResponse] 实体 Excel 导出到浏览器失败，文件名={}", fileName, e);
            throw new IllegalStateException("实体 Excel 导出到浏览器失败: " + fileName, e);
        }
    }

    /**
     * 实体多 Sheet 导出到浏览器
     *
     * <p>
     * 使用场景：
     * - 一个 Excel 文件中包含多个 Sheet
     * - 每个 Sheet 可以是不同的实体类型
     * - 每个实体通过 Fesod 注解自动解析表头和样式
     * </p>
     *
     * @param response      HttpServletResponse 对象（不可为空）
     * @param fileName      下载文件名，例如 "多实体导出.xlsx"（不可为空）
     * @param sheetDataList 多 Sheet 实体数据集合（不可为空/空集合）
     * @param handlers      可选全局 WriteHandler 扩展参数
     */
    public static void exportMultiSheetToResponse(HttpServletResponse response,
                                                  String fileName,
                                                  List<EntitySheetData<?>> sheetDataList,
                                                  WriteHandler... handlers) {
        Objects.requireNonNull(response, "HttpServletResponse 不能为空");
        try (OutputStream out = prepareResponseOutputStream(response, fileName)) {
            exportMultiSheet(out, sheetDataList, handlers);
            out.flush();
            log.info("[exportMultiSheetToResponse] 浏览器实体多 Sheet 下载成功，文件名={}", fileName);
        } catch (Exception e) {
            log.error("[exportMultiSheetToResponse] 实体多 Sheet Excel 导出到浏览器失败，文件名={}", fileName, e);
            throw new IllegalStateException("实体多 Sheet Excel 导出到浏览器失败: " + fileName, e);
        }
    }

    /*---------------------------------------------
     * 转换输出流工具方法区域
     *---------------------------------------------*/

    /**
     * 根据文件路径创建 FileOutputStream
     *
     * @param filePath 文件完整路径（不可为空）
     * @return OutputStream 输出流对象
     * @throws IllegalStateException 创建失败时抛出
     */
    public static OutputStream toOutputStream(String filePath) {
        Objects.requireNonNull(filePath, "文件路径不能为空");
        try {
            File file = new File(filePath);
            ensureParentDirExists(file);
            return new FileOutputStream(file);
        } catch (IOException e) {
            log.error("[toOutputStream(String)] 创建文件输出流失败，path={}", filePath, e);
            throw new IllegalStateException("创建文件输出流失败，请检查路径是否合法: " + filePath, e);
        }
    }

    /**
     * 根据 File 对象创建 FileOutputStream
     *
     * @param file 文件对象（不可为空）
     * @return OutputStream 输出流对象
     * @throws IllegalStateException 创建失败时抛出
     */
    public static OutputStream toOutputStream(File file) {
        Objects.requireNonNull(file, "文件对象不能为空");
        try {
            ensureParentDirExists(file);
            return new FileOutputStream(file);
        } catch (IOException e) {
            log.error("[toOutputStream(File)] 创建文件输出流失败，file={}", file.getAbsolutePath(), e);
            throw new IllegalStateException("创建文件输出流失败，请检查文件是否可写: " + file.getAbsolutePath(), e);
        }
    }

    /**
     * 根据 Path 对象创建 FileOutputStream
     *
     * @param path 文件路径（不可为空）
     * @return OutputStream 输出流对象
     * @throws IllegalStateException 创建失败时抛出
     */
    public static OutputStream toOutputStream(Path path) {
        Objects.requireNonNull(path, "Path 不能为空");
        return toOutputStream(path.toFile());
    }

    /**
     * 将 byte[] 写入内存输出流（ByteArrayOutputStream）
     *
     * @param bytes 字节数组
     * @return ByteArrayOutputStream 内存输出流
     * @throws IllegalStateException 写入失败时抛出
     */
    public static ByteArrayOutputStream toOutputStream(byte[] bytes) {
        if (bytes == null) {
            throw new IllegalArgumentException("字节数组不能为空");
        }
        try {
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            out.write(bytes);
            return out;
        } catch (IOException e) {
            log.error("[toOutputStream(byte[])] 写入内存输出流失败", e);
            throw new IllegalStateException("写入内存输出流失败", e);
        }
    }

    /**
     * 将 MultipartFile 写入临时文件并返回输出流
     *
     * @param multipartFile 上传文件
     * @return FileOutputStream 输出流
     * @throws IllegalStateException 创建失败时抛出
     */
    public static OutputStream toOutputStream(MultipartFile multipartFile) {
        Objects.requireNonNull(multipartFile, "MultipartFile 不能为空");
        try {
            File tempFile = File.createTempFile("upload-", multipartFile.getOriginalFilename());
            multipartFile.transferTo(tempFile);
            tempFile.deleteOnExit();
            return new FileOutputStream(tempFile);
        } catch (IOException e) {
            log.error("[toOutputStream(MultipartFile)] 从 MultipartFile 创建输出流失败, name={}", multipartFile.getOriginalFilename(), e);
            throw new IllegalStateException("从 MultipartFile 创建输出流失败", e);
        }
    }

    /**
     * 将 MultipartFile 转为内存输出流（不会写入磁盘）
     *
     * @param multipartFile 上传文件
     * @return ByteArrayOutputStream 输出流
     * @throws IllegalStateException 创建失败时抛出
     */
    public static OutputStream toOutputStreamInMemory(MultipartFile multipartFile) {
        Objects.requireNonNull(multipartFile, "MultipartFile 不能为空");
        try {
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            out.write(multipartFile.getBytes());
            return out;
        } catch (IOException e) {
            log.error("[toOutputStreamInMemory] 从 MultipartFile 创建内存输出流失败, name={}", multipartFile.getOriginalFilename(), e);
            throw new IllegalStateException("从 MultipartFile 创建内存输出流失败", e);
        }
    }

    /*---------------------------------------------
     * 输入流构建方法区域（导入 / 模板读取）
     *---------------------------------------------*/

    /**
     * 根据文件路径创建 FileInputStream
     *
     * @param filePath 文件完整路径（不可为空）
     * @return InputStream 输入流对象
     * @throws IllegalStateException 创建失败时抛出
     */
    public static InputStream toInputStream(String filePath) {
        Objects.requireNonNull(filePath, "文件路径不能为空");
        try {
            File file = new File(filePath);
            if (!file.exists()) {
                log.error("[toInputStream(String)] 文件不存在，path={}", filePath);
                throw new IllegalArgumentException("文件不存在: " + filePath);
            }
            return new FileInputStream(file);
        } catch (Exception e) {
            log.error("[toInputStream(String)] 创建文件输入流失败，path={}", filePath, e);
            throw new IllegalStateException("创建文件输入流失败: " + filePath, e);
        }
    }

    /**
     * 根据 File 对象创建 FileInputStream
     *
     * @param file 文件对象（不可为空）
     * @return InputStream 输入流对象
     * @throws IllegalStateException 创建失败时抛出
     */
    public static InputStream toInputStream(File file) {
        Objects.requireNonNull(file, "文件对象不能为空");
        try {
            if (!file.exists()) {
                log.error("[toInputStream(File)] 文件不存在，file={}", file.getAbsolutePath());
                throw new IllegalArgumentException("文件不存在: " + file.getAbsolutePath());
            }
            return new FileInputStream(file);
        } catch (Exception e) {
            log.error("[toInputStream(File)] 创建文件输入流失败，file={}", file.getAbsolutePath(), e);
            throw new IllegalStateException("创建文件输入流失败: " + file.getAbsolutePath(), e);
        }
    }

    /**
     * 根据 Path 对象创建 FileInputStream
     *
     * @param path 文件路径（不可为空）
     * @return InputStream 输入流对象
     */
    public static InputStream toInputStream(Path path) {
        Objects.requireNonNull(path, "Path 不能为空");
        return toInputStream(path.toFile());
    }

    /**
     * 将 byte[] 转为内存输入流
     *
     * @param bytes 字节数组
     * @return ByteArrayInputStream 输入流
     */
    public static InputStream toInputStream(byte[] bytes) {
        if (bytes == null) {
            throw new IllegalArgumentException("字节数组不能为空");
        }
        return new ByteArrayInputStream(bytes);
    }

    /**
     * 从 MultipartFile 创建输入流（不落盘）
     *
     * @param multipartFile 上传文件
     * @return InputStream 输入流
     */
    public static InputStream toInputStream(MultipartFile multipartFile) {
        Objects.requireNonNull(multipartFile, "MultipartFile 不能为空");
        try {
            return multipartFile.getInputStream();
        } catch (IOException e) {
            log.error("[toInputStream(MultipartFile)] 创建输入流失败, name={}", multipartFile.getOriginalFilename(), e);
            throw new IllegalStateException("从 MultipartFile 创建输入流失败", e);
        }
    }

    /**
     * 从 MultipartFile 创建临时文件并返回输入流
     *
     * <p>
     * 适用于部分第三方 API 必须接收 FileInputStream 的场景
     * </p>
     *
     * @param multipartFile 上传文件
     * @return InputStream 输入流
     */
    public static InputStream toInputStreamByTempFile(MultipartFile multipartFile) {
        Objects.requireNonNull(multipartFile, "MultipartFile 不能为空");
        try {
            File tempFile = File.createTempFile("upload-", multipartFile.getOriginalFilename());
            multipartFile.transferTo(tempFile);
            tempFile.deleteOnExit();
            return new FileInputStream(tempFile);
        } catch (IOException e) {
            log.error("[toInputStreamByTempFile] 从 MultipartFile 创建临时文件输入流失败, name={}", multipartFile.getOriginalFilename(), e);
            throw new IllegalStateException("从 MultipartFile 创建临时文件输入流失败", e);
        }
    }

    /**
     * 从 classpath 读取资源文件并创建输入流
     *
     * <p>
     * 常用于模板导出，例如：
     * resources/templates/user_template.xlsx
     * </p>
     *
     * @param classpathLocation 资源路径，例如 "templates/user_template.xlsx"
     * @return InputStream 输入流
     */
    public static InputStream toInputStreamFromClasspath(String classpathLocation) {
        Objects.requireNonNull(classpathLocation, "classpath 资源路径不能为空");
        InputStream in = Thread.currentThread()
                .getContextClassLoader()
                .getResourceAsStream(classpathLocation);
        if (in == null) {
            log.error("[toInputStreamFromClasspath] 未找到 classpath 资源: {}", classpathLocation);
            throw new IllegalArgumentException("未找到 classpath 资源: " + classpathLocation);
        }
        return in;
    }

    /**
     * 从 URL 资源创建输入流
     *
     * <p>
     * 适用于读取远程模板文件
     * </p>
     *
     * @param url 资源地址
     * @return InputStream 输入流
     */
    public static InputStream toInputStream(URL url) {
        Objects.requireNonNull(url, "URL 不能为空");
        try {
            return url.openStream();
        } catch (IOException e) {
            log.error("[toInputStream(URL)] 创建输入流失败, url={}", url, e);
            throw new IllegalStateException("从 URL 创建输入流失败: " + url, e);
        }
    }

    /*---------------------------------------------
     * 内部辅助工具方法区域
     *---------------------------------------------*/

    /**
     * 确保父级目录存在
     */
    private static void ensureParentDirExists(File file) {
        File parent = file.getParentFile();
        if (parent != null && !parent.exists()) {
            boolean created = parent.mkdirs();
            if (!created) {
                log.warn("[ensureParentDirExists] 父目录创建失败: {}", parent.getAbsolutePath());
            }
        }
    }

    /*---------------------------------------------
     * 表头/数据构建区域
     *---------------------------------------------*/

    /**
     * 构建 Fesod 所需的表头结构（多级表头）
     *
     * @param headers 表头描述集合（不可为空）
     * @return 多层 List 结构的表头
     * @throws IllegalArgumentException 当 headers 非法时抛出
     */
    public static List<List<String>> buildHead(List<HeaderItem> headers) {
        if (headers == null || headers.isEmpty()) {
            log.error("[buildHead] 表头为空，无法构建 Head");
            throw new IllegalArgumentException("表头不能为空");
        }

        List<List<String>> result = new ArrayList<>(headers.size());

        for (HeaderItem item : headers) {
            if (item == null) {
                log.error("[buildHead] HeaderItem 为空");
                throw new IllegalArgumentException("表头项不能为空");
            }
            if (item.getPath() == null || item.getPath().isEmpty()) {
                log.error("[buildHead] HeaderItem.path 非法，field={}", item.getField());
                throw new IllegalArgumentException("表头路径不能为空，且必须至少包含一级名称");
            }

            // 深复制避免外部修改影响
            result.add(new ArrayList<>(item.getPath()));
        }

        return result;
    }

    /**
     * 根据表头字段映射构建数据行
     *
     * @param headers  表头描述集合（至少包含 field，不可为空）
     * @param dataList 数据源列表（可为空）
     * @return 按字段顺序生成的二维数据行
     * @throws IllegalArgumentException 当 headers 非法时抛出
     */
    public static List<List<Object>> buildRows(List<HeaderItem> headers, List<Map<String, Object>> dataList) {
        if (headers == null || headers.isEmpty()) {
            log.error("[buildRows] 表头为空，无法构建数据行");
            throw new IllegalArgumentException("表头不能为空，无法构建数据行");
        }

        // 提取字段列表（headers -> field）
        List<String> fields = headers.stream()
                .peek(h -> {
                    if (h == null) {
                        log.error("[buildRows] HeaderItem 为空");
                        throw new IllegalArgumentException("表头项不能为空");
                    }
                    if (h.getField() == null) {
                        log.error("[buildRows] HeaderItem.field 为空，path={}", h.getPath());
                        throw new IllegalArgumentException("HeaderItem.field 不能为空");
                    }
                })
                .map(HeaderItem::getField)
                .collect(Collectors.toList());

        // dataList 为空时直接返回空集合，不抛异常
        if (dataList == null || dataList.isEmpty()) {
            log.warn("[buildRows] 数据列表为空，返回空行集");
            return Collections.emptyList();
        }

        List<List<Object>> rows = new ArrayList<>(dataList.size());

        for (Map<String, Object> data : dataList) {
            if (data == null) {
                log.warn("[buildRows] 检测到空数据行，已跳过");
                continue;
            }

            List<Object> row = new ArrayList<>(fields.size());
            for (String field : fields) {
                // 若 field 不存在，返回 null（Excel 可接受）
                row.add(data.getOrDefault(field, null));
            }
            rows.add(row);
        }

        return rows;
    }

    /*---------------------------------------------
     * 导出方法区域（支持单Sheet、多Sheet）
     *---------------------------------------------*/

    /**
     * 简单导出（单 Sheet，无分组表头）
     *
     * @param out       输出流（不可为空）
     * @param headers   表头字符串列表，每个字符串为一级表头
     * @param dataList  数据列表，可为空
     * @param sheetName Sheet 名称（不可为空）
     * @param handlers  可选 WriteHandler
     */
    public static void exportDynamicSimple(OutputStream out,
                                           List<String> headers,
                                           List<Map<String, Object>> dataList,
                                           String sheetName,
                                           WriteHandler... handlers) {

        if (headers == null || headers.isEmpty()) {
            log.error("[exportDynamicSimple] 表头为空");
            throw new IllegalArgumentException("表头不能为空");
        }

        List<HeaderItem> headerItems = headers.stream()
                .peek(h -> {
                    if (h == null || h.trim().isEmpty()) {
                        log.error("[exportDynamicSimple] 检测到空表头项");
                        throw new IllegalArgumentException("表头项不能为空");
                    }
                })
                .map(h -> new HeaderItem(Collections.singletonList(h), h))
                .collect(Collectors.toList());

        exportDynamic(out, headerItems, dataList, sheetName, handlers);
    }


    /**
     * 复杂导出（单 Sheet，支持多级表头）
     *
     * @param out       输出流（不可为空）
     * @param headers   表头定义（不可为空）
     * @param dataList  数据源，可为空
     * @param sheetName Sheet 名称（不可为空）
     * @param handlers  可选 WriteHandler
     */
    public static void exportDynamicComplex(OutputStream out,
                                            List<HeaderItem> headers,
                                            List<Map<String, Object>> dataList,
                                            String sheetName,
                                            WriteHandler... handlers) {
        exportDynamic(out, headers, dataList, sheetName, handlers);
    }


    /**
     * 核心单 Sheet 导出方法（带默认列宽策略）
     *
     * @param out       输出流（不可为空）
     * @param headers   表头定义（不可为空）
     * @param dataList  数据源，可为空
     * @param sheetName Sheet 名称（不可为空）
     * @param handlers  可选 WriteHandler
     */
    public static void exportDynamic(OutputStream out,
                                     List<HeaderItem> headers,
                                     List<Map<String, Object>> dataList,
                                     String sheetName,
                                     WriteHandler... handlers) {

        if (out == null) {
            log.error("[exportDynamic] 输出流不能为空");
            throw new IllegalArgumentException("输出流不能为空");
        }
        if (headers == null || headers.isEmpty()) {
            log.error("[exportDynamic] 表头为空，无法导出");
            throw new IllegalArgumentException("表头不能为空");
        }

        log.info("[exportDynamic] 开始单 Sheet 导出，headers={}，rows={}",
                headers.size(), dataList == null ? 0 : dataList.size());

        ExcelWriterSheetBuilder writer = FesodSheet.write(out)
                .head(buildHead(headers))
                .sheet(sheetName);

        // 注册用户扩展 handler
        if (handlers != null && handlers.length > 0) {
            for (WriteHandler handler : handlers) {
                if (handler != null) {
                    writer.registerWriteHandler(handler);
                }
            }
        }

        writer.doWrite(buildRows(headers, dataList));
        log.info("[exportDynamic] 单 Sheet 导出完成");
    }


    /**
     * 多 Sheet 导出
     *
     * @param out           输出流（不可为空）
     * @param sheetDataList 多 Sheet 数据（不可为空/空列表）
     * @param handlers      可选全局 handler
     */
    public static void exportDynamicMultiSheet(OutputStream out,
                                               List<SheetData> sheetDataList,
                                               WriteHandler... handlers) {

        if (out == null) {
            log.error("[exportDynamicMultiSheet] 输出流不能为空");
            throw new IllegalArgumentException("输出流不能为空");
        }
        if (sheetDataList == null || sheetDataList.isEmpty()) {
            log.error("[exportDynamicMultiSheet] 多 Sheet 数据不能为空");
            throw new IllegalArgumentException("Sheet 数据不能为空");
        }

        log.info("[exportDynamicMultiSheet] 开始多 Sheet 导出，共 {} 个 Sheet", sheetDataList.size());

        ExcelWriterBuilder builder = FesodSheet.write(out);

        // 注册用户扩展 handler（全局）
        if (handlers != null && handlers.length > 0) {
            for (WriteHandler handler : handlers) {
                if (handler != null) {
                    builder.registerWriteHandler(handler);
                }
            }
        }

        try (ExcelWriter excelWriter = builder.build()) {

            for (int i = 0; i < sheetDataList.size(); i++) {
                SheetData sd = sheetDataList.get(i);

                if (sd == null) {
                    log.warn("[exportDynamicMultiSheet] 第 {} 个 SheetData 为 null，已跳过", i);
                    continue;
                }

                if (sd.getHeaders() == null || sd.getHeaders().isEmpty()) {
                    log.error("[exportDynamicMultiSheet] 第 {} 个 Sheet 表头为空", i);
                    throw new IllegalArgumentException("多 Sheet 导出时，Sheet 表头不能为空");
                }

                List<Map<String, Object>> rows =
                        sd.getDataList() == null ? Collections.emptyList() : sd.getDataList();

                WriteSheet sheet = FesodSheet.writerSheet(i, sd.getSheetName())
                        .head(buildHead(sd.getHeaders()))
                        .build();

                excelWriter.write(buildRows(sd.getHeaders(), rows), sheet);
                log.info("[exportDynamicMultiSheet] Sheet[{}] 导出成功，名称='{}'，行数={}", i, sd.getSheetName(), rows.size());
            }
        }

        log.info("[exportDynamicMultiSheet] 全部 Sheet 导出完成");
    }

    /*---------------------------------------------
     * 浏览器下载方法（Spring Boot Response）区域
     *---------------------------------------------*/

    /**
     * 获取浏览器输出流，并统一设置 ContentType、编码、文件名
     *
     * @param response HttpServletResponse 对象
     * @param fileName 下载文件名
     * @return 输出流
     */
    private static OutputStream prepareResponseOutputStream(HttpServletResponse response, String fileName) {
        try {
            response.setContentType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
            response.setCharacterEncoding("UTF-8");
            String encodedFileName = URLEncoder.encode(fileName, StandardCharsets.UTF_8.name());
            response.setHeader("Content-Disposition", "attachment;filename=\"" + encodedFileName + "\"");
            return response.getOutputStream();
        } catch (Exception e) {
            log.error("获取浏览器输出流失败，文件名：{}", fileName, e);
            throw new IllegalStateException("获取浏览器输出流失败: " + fileName, e);
        }
    }

    /**
     * 将单 Sheet Excel 导出到浏览器（一级表头）
     *
     * <p>使用场景：
     * - 前端点击下载按钮时，将 Excel 文件直接推送给浏览器
     * - 表头为简单一级列表，每个 header 对应一列
     *
     * @param response  HttpServletResponse 对象（不可为空）
     * @param fileName  下载文件名，例如 "用户列表.xlsx"（不可为空）
     * @param headers   一级表头列表，每个字符串为一列名称（不可为空，且不可包含空字符串）
     * @param dataList  数据列表，每个 Map 对应一行，可为空
     * @param sheetName Sheet 名称（不可为空）
     * @param handlers  可选的 WriteHandler 扩展参数，可为空
     * @throws IllegalStateException 导出或流操作失败时抛出
     */
    public static void exportDynamicSimpleToResponse(HttpServletResponse response,
                                                     String fileName,
                                                     List<String> headers,
                                                     List<Map<String, Object>> dataList,
                                                     String sheetName,
                                                     WriteHandler... handlers) {
        Objects.requireNonNull(response, "HttpServletResponse 不能为空");
        try (OutputStream out = prepareResponseOutputStream(response, fileName)) {
            exportDynamicSimple(out, headers, dataList, sheetName, handlers);
            out.flush();
            log.info("[exportDynamicSimpleToResponse] 浏览器下载成功，文件名={}", fileName);
        } catch (Exception e) {
            log.error("[exportDynamicSimpleToResponse] Excel 导出到浏览器失败，文件名={}", fileName, e);
            throw new IllegalStateException("Excel 导出到浏览器失败: " + fileName, e);
        }
    }

    /**
     * 将单 Sheet Excel 导出到浏览器（多级表头）
     *
     * <p>使用场景：
     * - 表头为多级结构（支持分组表头）
     * - 数据为 Map 列表，键对应 HeaderItem.field
     *
     * @param response  HttpServletResponse 对象（不可为空）
     * @param fileName  下载文件名，例如 "用户数据.xlsx"（不可为空）
     * @param headers   表头定义列表，每个 HeaderItem 可包含多级 path（不可为空，且 path 不为空）
     * @param dataList  数据列表，每个 Map 对应一行，可为空
     * @param sheetName Sheet 名称（不可为空）
     * @param handlers  可选的 WriteHandler 扩展参数，可为空
     * @throws IllegalStateException 导出或流操作失败时抛出
     */
    public static void exportDynamicToResponse(HttpServletResponse response,
                                                      String fileName,
                                                      List<HeaderItem> headers,
                                                      List<Map<String, Object>> dataList,
                                                      String sheetName,
                                                      WriteHandler... handlers) {
        Objects.requireNonNull(response, "HttpServletResponse 不能为空");
        try (OutputStream out = prepareResponseOutputStream(response, fileName)) {
            exportDynamic(out, headers, dataList, sheetName, handlers);
            out.flush();
            log.info("[exportDynamicToResponse] 浏览器下载成功，文件名={}", fileName);
        } catch (Exception e) {
            log.error("[exportDynamicToResponse] Excel 导出到浏览器失败，文件名={}", fileName, e);
            throw new IllegalStateException("Excel 导出到浏览器失败: " + fileName, e);
        }
    }

    /**
     * 将多 Sheet Excel 导出到浏览器
     *
     * <p>使用场景：
     * - 同一个 Excel 包含多个 Sheet，每个 Sheet 可有独立表头和数据
     * - SheetData 列表不可为空，每个 SheetData 需有 sheetName 和 headers
     *
     * @param response      HttpServletResponse 对象（不可为空）
     * @param fileName      下载文件名，例如 "多Sheet导出.xlsx"（不可为空）
     * @param sheetDataList 多 Sheet 数据列表，每个 SheetData 包含 sheetName、headers、dataList（不可为空/空列表）
     * @param handlers      可选的全局 WriteHandler 扩展参数，可为空
     * @throws IllegalStateException 导出或流操作失败时抛出
     */
    public static void exportDynamicMultiSheetToResponse(HttpServletResponse response,
                                                         String fileName,
                                                         List<SheetData> sheetDataList,
                                                         WriteHandler... handlers) {
        Objects.requireNonNull(response, "HttpServletResponse 不能为空");
        try (OutputStream out = prepareResponseOutputStream(response, fileName)) {
            exportDynamicMultiSheet(out, sheetDataList, handlers);
            out.flush();
            log.info("[exportDynamicMultiSheetToResponse] 浏览器多 Sheet 下载成功，文件名={}", fileName);
        } catch (Exception e) {
            log.error("[exportDynamicMultiSheetToResponse] Excel 多 Sheet 导出到浏览器失败，文件名={}", fileName, e);
            throw new IllegalStateException("Excel 多 Sheet 导出到浏览器失败: " + fileName, e);
        }
    }

    /*---------------------------------------------
     * 内部数据结构
     *---------------------------------------------*/

    /**
     * 表头项，用于动态构建多级表头
     *
     * <p>
     * 示例：
     * path = ["用户信息","姓名"]
     * field = "name"
     * </p>
     */
    public static class HeaderItem {

        /**
         * 多级表头路径，例如：["一级","二级","三级"]
         * 最后一级为实际列名
         */
        private final List<String> path;

        /**
         * 对应数据 Map 中的字段 key
         */
        private final String field;

        public HeaderItem(List<String> path, String field) {
            if (path == null || path.isEmpty()) {
                throw new IllegalArgumentException("path 不能为空");
            }
            if (field == null || field.isEmpty()) {
                throw new IllegalArgumentException("field 不能为空");
            }
            this.path = path;
            this.field = field;
        }

        public List<String> getPath() {
            return path;
        }

        public String getField() {
            return field;
        }
    }

    /**
     * Sheet 数据载体，用于多 Sheet 导出场景
     */
    public static class SheetData {

        /**
         * Sheet 名称
         */
        private final String sheetName;

        /**
         * 表头定义列表
         */
        private final List<HeaderItem> headers;

        /**
         * 实际数据行列表，每行 Map 代表一个 Entity
         */
        private final List<Map<String, Object>> dataList;

        public SheetData(String sheetName,
                         List<HeaderItem> headers,
                         List<Map<String, Object>> dataList) {
            this.sheetName = sheetName;
            this.headers = headers;
            this.dataList = dataList;
        }

        public String getSheetName() {
            return sheetName;
        }

        public List<HeaderItem> getHeaders() {
            return headers;
        }

        public List<Map<String, Object>> getDataList() {
            return dataList;
        }
    }

    /**
     * 实体 Sheet 数据载体，用于多 Sheet 实体导出场景
     *
     * @param <T> 实体类型
     */
    public static class EntitySheetData<T> {

        /**
         * Sheet 名称
         */
        private final String sheetName;

        /**
         * 实体类型
         */
        private final Class<T> clazz;

        /**
         * 实体数据列表
         */
        private final List<T> dataList;

        public EntitySheetData(String sheetName,
                               Class<T> clazz,
                               List<T> dataList) {
            this.sheetName = sheetName;
            this.clazz = clazz;
            this.dataList = dataList;
        }

        public String getSheetName() {
            return sheetName;
        }

        public Class<T> getClazz() {
            return clazz;
        }

        public List<T> getDataList() {
            return dataList;
        }
    }

}
