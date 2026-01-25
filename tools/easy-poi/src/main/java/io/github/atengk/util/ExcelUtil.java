package io.github.atengk.util;

import cn.afterturn.easypoi.excel.ExcelExportUtil;
import cn.afterturn.easypoi.excel.ExcelImportUtil;
import cn.afterturn.easypoi.excel.entity.ExportParams;
import cn.afterturn.easypoi.excel.entity.ImportParams;
import cn.afterturn.easypoi.excel.entity.TemplateExportParams;
import cn.afterturn.easypoi.excel.entity.result.ExcelImportResult;
import org.apache.poi.ss.usermodel.Workbook;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.web.multipart.MultipartFile;

import javax.servlet.http.HttpServletResponse;
import java.io.*;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.Map;

/**
 * Excel 工具类（基于 EasyPOI + Apache POI 封装）
 *
 * <p>
 * 提供企业级 Excel 处理能力的统一入口，主要用于：
 * </p>
 *
 * <ul>
 *     <li>基于模板（.xlsx）填充数据并生成 Workbook</li>
 *     <li>支持从 classpath、本地文件、对象存储、网络流等多种来源读取模板</li>
 *     <li>支持 Workbook 导出到本地文件、HTTP 响应流、文件流等多种场景</li>
 *     <li>支持对导出完成后的 Workbook 进行二次样式加工（指定列、条件样式、斑马纹、表头高亮等）</li>
 * </ul>
 *
 * <p>
 * 设计目标：
 * </p>
 *
 * <ul>
 *     <li>屏蔽 EasyPOI 与 POI 的底层复杂度，对外提供简单、稳定的 API</li>
 *     <li>所有方法均为静态方法，符合工具类的使用语义</li>
 *     <li>适用于报表系统、数据导出、运营数据分析、模板化 Excel 生成等企业级场景</li>
 * </ul>
 *
 * <p>
 * 典型使用流程：
 * </p>
 *
 * <pre>
 * Workbook workbook = ExcelUtil.exportExcelByTemplate("doc/user_template.xlsx", data);
 *
 * ExcelUtil.applyByTitle(workbook, 0, "分数", 1, (wb, cell) -> {
 *     // 自定义样式处理
 * });
 *
 * ExcelUtil.exportToResponse(workbook, "用户数据.xlsx", response);
 * </pre>
 *
 * <p>
 * 该类为纯工具类：
 * </p>
 * <ul>
 *     <li>禁止实例化（私有构造方法）</li>
 *     <li>不保存任何状态，线程安全</li>
 * </ul>
 *
 * @author 孔余
 * @since 2026-01-22
 */
public final class ExcelUtil {

    private ExcelUtil() {
    }

    /**
     * 基于实体注解导出数据（默认参数），返回 Workbook。
     *
     * <p>该方法适用于使用 @Excel 注解映射的实体类，将对象列表转换为 Excel Workbook，
     * 调用方可选择自行保存文件或进一步加工。</p>
     *
     * <p>注意：返回的 Workbook 由调用方负责关闭，或使用 {@link #write(Workbook, File)} /
     * {@link #write(Workbook, Path)} / {@link #write(Workbook, String, HttpServletResponse)}
     * 等方法统一输出并关闭。</p>
     *
     * @param clazz 实体类型（需使用 @Excel 注解）
     * @param data  数据集合，不能为空
     * @param <T>   实体类型泛型
     * @return 填充后的 Workbook 对象（未关闭）
     */
    public static <T> Workbook exportExcel(Class<T> clazz, List<T> data) {
        return exportExcel(clazz, data, null);
    }

    /**
     * 基于实体注解导出数据（支持函数式配置 ExportParams），返回 Workbook。
     *
     * <p>该方法允许通过 Lambda 对 {@link ExportParams} 进行个性化配置，例如：</p>
     *
     * <pre>{@code
     * Workbook wb = ExcelUtil.exportExcel(User.class, list, p -> {
     *     p.setTitle("用户报表");
     *     p.setSheetName("用户列表");
     * });
     * }</pre>
     *
     * <p>注意：返回的 Workbook 由调用方负责关闭或通过统一 write 方法输出并关闭。</p>
     *
     * @param clazz      实体类型（需使用 @Excel 注解）
     * @param data       数据集合，不能为空
     * @param configurer 导出参数配置回调，允许为 null
     * @param <T>        实体类型泛型
     * @return 填充后的 Workbook 对象（未关闭）
     */
    public static <T> Workbook exportExcel(Class<T> clazz,
                                           List<T> data,
                                           ExportParamsConfigurer configurer) {

        if (clazz == null) {
            throw new IllegalArgumentException("clazz 不能为空");
        }
        if (data == null) {
            throw new IllegalArgumentException("data 不能为空");
        }

        ExportParams params = new ExportParams();
        if (configurer != null) {
            configurer.configure(params);
        }

        try {
            return ExcelExportUtil.exportExcel(params, clazz, data);
        } catch (Exception e) {
            throw new IllegalStateException("Excel 导出失败（对象注解模式）", e);
        }
    }

    /**
     * 基于实体注解导出数据到本地文件（基于字符串文件路径，支持函数式配置 ExportParams）。
     *
     * <p>适用于以下场景：</p>
     * <ul>
     *     <li>从配置文件或运行参数中传入文件路径</li>
     *     <li>无需手动构建 File / Path 对象的快速落盘场景</li>
     *     <li>单元测试、定时任务、数据归档等业务逻辑</li>
     * </ul>
     *
     * <p>内部会自动创建父目录，并调用 {@link #write(Workbook, Path)} 写入并关闭 Workbook。</p>
     *
     * @param clazz      实体类型（需使用 @Excel 注解）
     * @param data       数据集合
     * @param filePath   本地文件路径（相对或绝对），例如："target/users.xlsx"
     * @param configurer 导出参数配置回调，可为 null
     * @param <T>        泛型类型
     */
    public static <T> void exportExcel(Class<T> clazz,
                                       List<T> data,
                                       String filePath,
                                       ExportParamsConfigurer configurer) {

        if (filePath == null || filePath.trim().isEmpty()) {
            throw new IllegalArgumentException("filePath 不能为空");
        }

        Workbook workbook = exportExcel(clazz, data, configurer);
        write(workbook, Paths.get(filePath));
    }

    /**
     * 基于实体注解导出数据到本地文件（支持函数式配置 ExportParams）。
     *
     * <p>适用于以下场景：</p>
     * <ul>
     *     <li>单元测试导出验证</li>
     *     <li>服务器本地报表生成</li>
     *     <li>定时任务落盘归档</li>
     * </ul>
     *
     * <p>内部会调用 {@link #write(Workbook, File)} 写入并关闭 Workbook。</p>
     *
     * @param clazz      实体类型（需使用 @Excel 注解）
     * @param data       数据集合
     * @param file       导出目标文件对象，例如：new File("user.xlsx")
     * @param configurer 导出参数配置回调，允许为 null
     * @param <T>        泛型类型
     */
    public static <T> void exportExcel(Class<T> clazz,
                                       List<T> data,
                                       File file,
                                       ExportParamsConfigurer configurer) {

        Workbook workbook = exportExcel(clazz, data, configurer);
        write(workbook, file);
    }

    /**
     * 基于实体注解导出数据到本地路径（支持函数式配置 ExportParams）。
     *
     * <p>适用于基于 NIO 的本地磁盘操作，与本工具类的 File 写法保持一致。</p>
     *
     * <p>内部会调用 {@link #write(Workbook, Path)} 写入并关闭 Workbook。</p>
     *
     * @param clazz      实体类型（需使用 @Excel 注解）
     * @param data       数据集合
     * @param filePath   导出文件路径，例如：Paths.get("target/user.xlsx")
     * @param configurer 导出参数配置回调，允许为 null
     * @param <T>        泛型类型
     */
    public static <T> void exportExcel(Class<T> clazz,
                                       List<T> data,
                                       Path filePath,
                                       ExportParamsConfigurer configurer) {

        Workbook workbook = exportExcel(clazz, data, configurer);
        write(workbook, filePath);
    }

    /**
     * 基于实体注解导出数据到浏览器（支持函数式配置 ExportParams）。
     *
     * <p>适用于以下场景：</p>
     * <ul>
     *     <li>前端点击“导出 Excel”按钮</li>
     *     <li>SaaS 系统在线数据下载</li>
     *     <li>报表服务 HTTP 文件输出</li>
     * </ul>
     *
     * <p>内部会调用 {@link #write(Workbook, String, HttpServletResponse)} 写入并关闭 Workbook。</p>
     *
     * @param clazz      实体类型（需使用 @Excel 注解）
     * @param data       数据集合
     * @param fileName   下载文件名，例如：“用户列表.xlsx”
     * @param response   HttpServletResponse
     * @param configurer 导出参数配置回调，允许为 null
     * @param <T>        泛型类型
     */
    public static <T> void exportExcel(Class<T> clazz,
                                       List<T> data,
                                       String fileName,
                                       HttpServletResponse response,
                                       ExportParamsConfigurer configurer) {

        Workbook workbook = exportExcel(clazz, data, configurer);
        write(workbook, fileName, response);
    }

    /**
     * 基于实体注解导出数据（支持函数式配置 ExportParams），输出为 byte[] 数组。
     *
     * <p>适用于以下场景：</p>
     * <ul>
     *     <li>微服务之间通过接口返回 Excel 二进制</li>
     *     <li>Redis / 缓存存储 Excel 数据</li>
     *     <li>上传 OSS / MinIO / COS 对象存储</li>
     *     <li>消息队列（MQ）通过二进制传输 Excel 文件</li>
     * </ul>
     *
     * <p>内部使用内存缓冲，不会产生磁盘 IO，性能高且适用于云原生环境。</p>
     *
     * @param clazz      实体类型（需使用 @Excel 注解）
     * @param data       数据集合
     * @param configurer 导出参数配置回调，可为 null
     * @param <T>        泛型类型
     * @return Excel 文件的二进制数据 byte[]
     */
    public static <T> byte[] exportExcelToBytes(Class<T> clazz,
                                                List<T> data,
                                                ExportParamsConfigurer configurer) {
        Workbook workbook = exportExcel(clazz, data, configurer);
        try (ByteArrayOutputStream bos = new ByteArrayOutputStream()) {
            workbook.write(bos);
            return bos.toByteArray();
        } catch (IOException e) {
            throw new IllegalStateException("Excel 导出为 byte[] 失败", e);
        } finally {
            try {
                workbook.close();
            } catch (IOException ignored) {
            }
        }
    }

    /**
     * 基于实体注解导出数据（支持函数式配置 ExportParams），输出为 InputStream。
     *
     * <p>适用于以下场景：</p>
     * <ul>
     *     <li>上传 OSS / MinIO / COS（大多要求 InputStream）</li>
     *     <li>第三方 SDK 接收流式数据处理</li>
     *     <li>HTTP 响应中作为 Streaming 输出</li>
     *     <li>云原生无磁盘环境</li>
     * </ul>
     *
     * <p>输出为字节流包装的 {@link ByteArrayInputStream}，
     * 调用方负责关闭 InputStream。</p>
     *
     * @param clazz      实体类型（需使用 @Excel 注解）
     * @param data       数据集合
     * @param configurer 导出参数配置回调，可为 null
     * @param <T>        泛型类型
     * @return Excel 内容的输入流对象（调用方负责关闭）
     */
    public static <T> InputStream exportExcelToStream(Class<T> clazz,
                                                      List<T> data,
                                                      ExportParamsConfigurer configurer) {

        byte[] bytes = exportExcelToBytes(clazz, data, configurer);
        return new ByteArrayInputStream(bytes);
    }

    /**
     * 将 Workbook 导出为本地 Excel 文件（基于字符串路径）
     *
     * <p>
     * 适用于：
     * <ul>
     *     <li>从配置文件或运行参数中获取文件路径</li>
     *     <li>无需显式构建 Path 对象的快速导出场景</li>
     *     <li>单元测试、本地调试、定时任务等落盘需求</li>
     * </ul>
     *
     * <p>
     * 注意：
     * <ul>
     *     <li>内部会自动创建父目录</li>
     *     <li>Workbook 会在写入完成后自动关闭</li>
     * </ul>
     * </p>
     *
     * @param workbook 已生成的 Workbook 对象
     * @param filePath 目标文件路径（相对或绝对），例如："target/user.xlsx"
     */
    public static void write(Workbook workbook, String filePath) {
        if (filePath == null || filePath.trim().isEmpty()) {
            throw new IllegalArgumentException("filePath 不能为空");
        }
        write(workbook, Paths.get(filePath));
    }

    /**
     * 将 Workbook 导出为本地 Excel 文件
     *
     * <p>
     * 适用于：
     * - 单元测试
     * - 本地调试
     * - 定时任务批量生成文件
     * - 数据归档
     * </p>
     *
     * @param workbook 已生成的 Workbook 对象
     * @param filePath 目标文件完整路径，例如：target/user.xlsx
     */
    public static void write(Workbook workbook, Path filePath) {
        if (workbook == null) {
            throw new IllegalArgumentException("Workbook 不能为空");
        }
        if (filePath == null) {
            throw new IllegalArgumentException("filePath 不能为空");
        }

        try {
            // 确保父目录存在
            Files.createDirectories(filePath.getParent());

            try (OutputStream outputStream = Files.newOutputStream(filePath)) {
                workbook.write(outputStream);
            } finally {
                workbook.close();
            }
        } catch (IOException e) {
            throw new IllegalStateException("导出 Excel 文件失败: " + filePath, e);
        }
    }

    /**
     * 将 Workbook 导出为本地 Excel 文件（基于 File）
     *
     * <p>
     * 适用于：
     * </p>
     *
     * <ul>
     *     <li>单元测试直接落盘验证</li>
     *     <li>本地调试生成中间文件</li>
     *     <li>定时任务批量生成 Excel 报表</li>
     *     <li>历史数据归档</li>
     * </ul>
     *
     * <p>
     * 如果目标文件所在目录不存在，会自动创建父目录。
     * </p>
     *
     * @param workbook 已生成的 Workbook 对象
     * @param file     目标文件对象，例如：new File("target/user.xlsx")
     */
    public static void write(Workbook workbook, File file) {
        if (workbook == null) {
            throw new IllegalArgumentException("Workbook 不能为空");
        }
        if (file == null) {
            throw new IllegalArgumentException("file 不能为空");
        }

        try {
            File parentDir = file.getParentFile();
            if (parentDir != null && !parentDir.exists()) {
                boolean created = parentDir.mkdirs();
                if (!created) {
                    throw new IllegalStateException("创建目录失败: " + parentDir.getAbsolutePath());
                }
            }

            try (OutputStream outputStream = new FileOutputStream(file)) {
                workbook.write(outputStream);
            } finally {
                workbook.close();
            }
        } catch (IOException e) {
            throw new IllegalStateException("导出 Excel 文件失败: " + file.getAbsolutePath(), e);
        }
    }

    /**
     * 将 Workbook 通过 Spring Boot 接口直接输出给前端下载
     *
     * <p>
     * Content-Type: application/vnd.openxmlformats-officedocument.spreadsheetml.sheet
     * Content-Disposition: attachment; filename="xxx.xlsx"
     * </p>
     * <p>
     * 适用于：
     * - 浏览器下载 Excel
     * - 前端点击“导出”按钮
     * - SaaS 系统在线报表导出
     *
     * @param workbook 已生成的 Workbook
     * @param fileName 下载文件名，例如：用户数据.xlsx
     * @param response HttpServletResponse
     */
    public static void write(
            Workbook workbook,
            String fileName,
            HttpServletResponse response) {

        if (workbook == null) {
            throw new IllegalArgumentException("Workbook 不能为空");
        }
        if (fileName == null || fileName.isEmpty()) {
            throw new IllegalArgumentException("fileName 不能为空");
        }
        if (response == null) {
            throw new IllegalArgumentException("HttpServletResponse 不能为空");
        }

        try {
            String encodedFileName = URLEncoder.encode(fileName, StandardCharsets.UTF_8.name())
                    .replaceAll("\\+", "%20");

            response.setCharacterEncoding(StandardCharsets.UTF_8.name());
            response.setContentType(
                    "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
            response.setHeader(
                    HttpHeaders.CONTENT_DISPOSITION,
                    "attachment; filename=\"" + encodedFileName + "\"");

            try (OutputStream outputStream = response.getOutputStream()) {
                workbook.write(outputStream);
                outputStream.flush();
            } finally {
                workbook.close();
            }
        } catch (IOException e) {
            throw new IllegalStateException("通过接口导出 Excel 失败", e);
        }
    }

    /**
     * 使用 classpath 模板导出（默认配置）
     *
     * @param templatePath 模板路径，例如：doc/user_template.xlsx
     * @param data         模板参数数据
     * @return 填充完成的 Workbook
     */
    public static Workbook exportExcelByTemplate(String templatePath, Map<String, Object> data) {
        return exportExcelByTemplate(templatePath, data, null);
    }

    /**
     * 使用 classpath 模板导出（开放 TemplateExportParams 配置）
     *
     * @param templatePath 模板路径，相对 classpath
     * @param data         模板数据
     * @param configurer   参数配置回调，可为 null
     * @return 填充完成的 Workbook
     */
    public static Workbook exportExcelByTemplate(String templatePath,
                                            Map<String, Object> data,
                                            TemplateParamsConfigurer configurer) {

        if (templatePath == null || templatePath.trim().isEmpty()) {
            throw new IllegalArgumentException("模板路径不能为空");
        }
        if (data == null) {
            throw new IllegalArgumentException("模板数据 data 不能为空");
        }

        Resource resource = new ClassPathResource(templatePath);

        if (!resource.exists()) {
            throw new IllegalStateException("Excel 模板不存在 (请检查路径或资源是否已打包)：路径=" + templatePath);
        }

        try (InputStream inputStream = resource.getInputStream()) {
            return doExportExcelByTemplate(inputStream, data, configurer);
        } catch (IOException e) {
            throw new IllegalStateException("Excel 模板读取失败(文件 IO 异常)：路径=" + templatePath, e);
        }
    }

    /**
     * 使用模板流导出（默认配置）
     * <p>
     * 场景示例：
     * - OSS/MinIO 下载输入流
     * - 远程 HTTP 下载流
     * - 数据库存储模板
     * <p>
     * 注意：不会关闭传入流，由调用方管理。
     *
     * @param templateInputStream 模板输入流
     * @param data                模板数据
     */
    public static Workbook exportExcelByTemplate(InputStream templateInputStream, Map<String, Object> data) {
        return exportExcelByTemplate(templateInputStream, data, null);
    }

    /**
     * 使用模板流导出（开放 TemplateExportParams 配置）
     *
     * @param templateInputStream 模板输入流（不会被关闭）
     * @param data                模板数据
     * @param configurer          配置回调，可为 null
     */
    public static Workbook exportExcelByTemplate(InputStream templateInputStream,
                                            Map<String, Object> data,
                                            TemplateParamsConfigurer configurer) {

        if (templateInputStream == null) {
            throw new IllegalArgumentException("templateInputStream 不能为空");
        }
        if (data == null) {
            throw new IllegalArgumentException("模板数据 data 不能为空");
        }

        try {
            return doExportExcelByTemplate(templateInputStream, data, configurer);
        } catch (Exception e) {
            throw new IllegalStateException("Excel 模板导出失败(模板流处理异常)", e);
        }
    }

    /**
     * 核心执行逻辑（统一出口）
     *
     * @param templateInputStream 模板流
     * @param data                模板数据
     * @param configurer          可选参数配置器
     */
    private static Workbook doExportExcelByTemplate(InputStream templateInputStream,
                                                    Map<String, Object> data,
                                                    TemplateParamsConfigurer configurer) throws IOException {

        TemplateExportParams params = new TemplateExportParams(templateInputStream);

        if (configurer != null) {
            configurer.configure(params);
        }

        return ExcelExportUtil.exportExcel(params, data);
    }

    /**
     * 从本地文件读取为 InputStream
     *
     * <p>
     * 适用于：
     * </p>
     *
     * <ul>
     *     <li>磁盘模板文件读取</li>
     *     <li>历史文件二次处理</li>
     *     <li>定时任务读取已生成 Excel</li>
     * </ul>
     *
     * <p>
     * 返回的 InputStream 需要由调用方负责关闭。
     * </p>
     *
     * @param file 本地文件对象
     * @return 文件输入流
     */
    public static InputStream getInputStream(File file) {
        if (file == null) {
            throw new IllegalArgumentException("file 不能为空");
        }
        if (!file.exists()) {
            throw new IllegalStateException("文件不存在: " + file.getAbsolutePath());
        }
        if (!file.isFile()) {
            throw new IllegalStateException("不是有效的文件: " + file.getAbsolutePath());
        }

        try {
            return new FileInputStream(file);
        } catch (IOException e) {
            throw new IllegalStateException("读取本地文件失败: " + file.getAbsolutePath(), e);
        }
    }

    /**
     * 从本地 Path 读取为 InputStream
     *
     * <p>
     * 适用于：
     * </p>
     *
     * <ul>
     *     <li>基于 NIO 的文件系统操作</li>
     *     <li>统一 Path 与 File 风格的文件读取方式</li>
     *     <li>与 exportToFile(Workbook, Path) 形成完整闭环</li>
     * </ul>
     *
     * <p>
     * 返回的 InputStream 需要由调用方负责关闭。
     * </p>
     *
     * @param path 本地文件路径，例如：Paths.get("target/user.xlsx")
     * @return 文件输入流
     */
    public static InputStream getInputStream(Path path) {
        if (path == null) {
            throw new IllegalArgumentException("path 不能为空");
        }
        if (!Files.exists(path)) {
            throw new IllegalStateException("文件不存在: " + path.toAbsolutePath());
        }
        if (!Files.isRegularFile(path)) {
            throw new IllegalStateException("不是有效的文件: " + path.toAbsolutePath());
        }

        try {
            return Files.newInputStream(path);
        } catch (IOException e) {
            throw new IllegalStateException("读取本地 Path 文件失败: " + path.toAbsolutePath(), e);
        }
    }

    /**
     * 从 classpath 读取资源文件为 InputStream
     *
     * <p>
     * 适用于：
     * </p>
     *
     * <ul>
     *     <li>resources 目录下 Excel 模板</li>
     *     <li>打包到 jar 内的模板文件</li>
     *     <li>固定模板文件读取</li>
     * </ul>
     *
     * <p>
     * 返回的 InputStream 需要由调用方负责关闭。
     * </p>
     *
     * @param classPath 资源路径，例如：doc/user_template.xlsx
     * @return 资源输入流
     */
    public static InputStream getInputStreamFromClasspath(String classPath) {
        if (classPath == null || classPath.trim().isEmpty()) {
            throw new IllegalArgumentException("classPath 不能为空");
        }

        Resource resource = new ClassPathResource(classPath);
        if (!resource.exists()) {
            throw new IllegalStateException("classpath 资源不存在: " + classPath);
        }

        try {
            return resource.getInputStream();
        } catch (IOException e) {
            throw new IllegalStateException("读取 classpath 资源失败: " + classPath, e);
        }
    }

    /**
     * 从 MultipartFile 读取为 InputStream
     *
     * <p>
     * 适用于：
     * </p>
     *
     * <ul>
     *     <li>前端上传 Excel 文件</li>
     *     <li>HTTP 接口导入场景</li>
     *     <li>Excel 导入统一入口</li>
     * </ul>
     *
     * <p>
     * 返回的 InputStream 需要由调用方负责关闭。
     * </p>
     *
     * @param multipartFile 前端上传文件对象
     * @return 文件输入流
     */
    public static InputStream getInputStream(MultipartFile multipartFile) {
        if (multipartFile == null) {
            throw new IllegalArgumentException("multipartFile 不能为空");
        }
        if (multipartFile.isEmpty()) {
            throw new IllegalStateException("上传文件为空");
        }

        try {
            return multipartFile.getInputStream();
        } catch (IOException e) {
            throw new IllegalStateException("读取 MultipartFile 输入流失败", e);
        }
    }

    /**
     * 将 Excel 二进制数据转换为 {@link InputStream}
     *
     * <p>
     * 适用于以下场景：
     * </p>
     *
     * <ul>
     *     <li>Excel 文件内容已提前读取为 byte[]</li>
     *     <li>从数据库、Redis、对象存储（OSS / MinIO）中直接获取文件字节数据</li>
     *     <li>微服务之间通过 RPC 或 MQ 传递 Excel 二进制内容</li>
     * </ul>
     *
     * <p>
     * 内部基于 {@link java.io.ByteArrayInputStream} 实现，
     * 不涉及磁盘 IO，完全在内存中操作，性能高且适合云原生环境。
     * </p>
     *
     * <p>
     * 返回的 {@link InputStream} 由调用方负责关闭。
     * </p>
     *
     * @param bytes Excel 文件二进制数据
     * @return 对应的输入流对象
     */
    public static InputStream getInputStream(byte[] bytes) {
        if (bytes == null || bytes.length == 0) {
            throw new IllegalArgumentException("bytes 不能为空");
        }
        return new ByteArrayInputStream(bytes);
    }

    /**
     * 使用 Excel 文件导入为对象列表（默认参数）
     *
     * @param file  Excel 文件
     * @param clazz 目标实体类型
     * @param <T>   泛型类型
     * @return 导入后的数据集合
     */
    public static <T> List<T> importExcel(File file, Class<T> clazz) {
        return importExcel(file, clazz, null);
    }

    /**
     * 使用 Excel 文件导入为对象列表（支持参数函数式配置）
     *
     * @param file       Excel 文件
     * @param clazz      目标实体类型
     * @param configurer 参数配置回调，可为 null
     * @param <T>        泛型类型
     * @return 导入后的数据集合
     */
    public static <T> List<T> importExcel(File file,
                                          Class<T> clazz,
                                          ImportParamsConfigurer configurer) {

        if (file == null) {
            throw new IllegalArgumentException("file 不能为空");
        }
        if (clazz == null) {
            throw new IllegalArgumentException("clazz 不能为空");
        }

        ImportParams params = new ImportParams();
        if (configurer != null) {
            configurer.configure(params);
        }

        try {
            return ExcelImportUtil.importExcel(file, clazz, params);
        } catch (Exception e) {
            throw new IllegalStateException("Excel 文件导入失败: " + file.getAbsolutePath(), e);
        }
    }

    /**
     * 从本地 Path 文件导入 Excel 为对象列表（默认参数）
     *
     * <p>
     * 适用于：
     * </p>
     *
     * <ul>
     *     <li>本地磁盘 Excel 文件导入</li>
     *     <li>定时任务批量导入数据</li>
     *     <li>测试环境快速验证 Excel 数据结构</li>
     * </ul>
     *
     * <p>
     * 该方法使用 EasyPOI 默认 {@link ImportParams} 配置，
     * 适合结构简单、无需复杂控制的 Excel 文件。
     * </p>
     *
     * @param path  本地 Excel 文件路径，例如：Paths.get("target/users.xlsx")
     * @param clazz 目标实体类型
     * @param <T>   泛型类型
     * @return 导入后的对象集合
     */
    public static <T> List<T> importExcel(Path path, Class<T> clazz) {
        return importExcel(path, clazz, null);
    }

    /**
     * 从本地 Path 文件导入 Excel 为对象列表（支持函数式配置 ImportParams）
     *
     * <p>
     * 适用于：
     * </p>
     *
     * <ul>
     *     <li>需要设置标题行、表头行、数据起始行的复杂 Excel</li>
     *     <li>需要开启校验、并行解析等高级功能的导入场景</li>
     *     <li>企业级 Excel 导入统一入口</li>
     * </ul>
     *
     * <p>
     * 通过 {@link ImportParamsConfigurer} 可以灵活配置：
     * </p>
     *
     * <ul>
     *     <li>titleRows / headRows / startRows</li>
     *     <li>needVerify / verifyHandler</li>
     *     <li>sheetIndex / sheetNum / sheetName</li>
     *     <li>concurrentTask / critical</li>
     *     <li>importFields / needCheckOrder</li>
     * </ul>
     *
     * @param path       本地 Excel 文件路径
     * @param clazz      目标实体类型
     * @param configurer ImportParams 参数配置回调，可为 null
     * @param <T>        泛型类型
     * @return 导入后的对象集合
     */
    public static <T> List<T> importExcel(Path path,
                                          Class<T> clazz,
                                          ImportParamsConfigurer configurer) {

        if (path == null) {
            throw new IllegalArgumentException("path 不能为空");
        }
        if (clazz == null) {
            throw new IllegalArgumentException("clazz 不能为空");
        }

        ImportParams params = new ImportParams();
        if (configurer != null) {
            configurer.configure(params);
        }

        try {
            return ExcelImportUtil.importExcel(path.toFile(), clazz, params);
        } catch (Exception e) {
            throw new IllegalStateException("Excel Path 导入失败: " + path.toAbsolutePath(), e);
        }
    }

    /**
     * 从 MultipartFile 导入 Excel 为对象列表（默认参数）
     *
     * <p>
     * 适用于：
     * </p>
     *
     * <ul>
     *     <li>前端上传 Excel 文件导入</li>
     *     <li>Spring Boot 接口文件上传解析</li>
     *     <li>最基础的 Excel 数据接收场景</li>
     * </ul>
     *
     * <p>
     * 该方法使用 EasyPOI 默认 {@link ImportParams} 配置，
     * 适合不需要复杂校验和特殊处理的快速导入。
     * </p>
     *
     * @param file  前端上传的 Excel 文件
     * @param clazz 目标实体类型
     * @param <T>   泛型类型
     * @return 导入后的对象集合
     */
    public static <T> List<T> importExcel(MultipartFile file,
                                          Class<T> clazz) {
        return importExcel(file, clazz, null);
    }

    /**
     * 从 MultipartFile 导入 Excel 为对象列表（支持函数式配置 ImportParams）
     *
     * <p>
     * 适用于：
     * </p>
     *
     * <ul>
     *     <li>接口上传 Excel 并进行业务校验的场景</li>
     *     <li>需要校验模板合法性、字段完整性的企业系统</li>
     *     <li>需要并行解析、大数据量导入的高性能场景</li>
     * </ul>
     *
     * <p>
     * 通过 {@link ImportParamsConfigurer} 可以灵活配置：
     * </p>
     *
     * <ul>
     *     <li>Excel 结构参数（titleRows、headRows、startRows）</li>
     *     <li>校验参数（needVerify、verifyHandler、verifyGroup）</li>
     *     <li>模板校验（importFields、needCheckOrder）</li>
     *     <li>多 Sheet 导入（startSheetIndex、sheetNum、sheetName）</li>
     *     <li>性能优化（concurrentTask、critical）</li>
     * </ul>
     *
     * @param file       前端上传的 Excel 文件
     * @param clazz      目标实体类型
     * @param configurer ImportParams 参数配置回调，可为 null
     * @param <T>        泛型类型
     * @return 导入后的对象集合
     */
    public static <T> List<T> importExcel(MultipartFile file,
                                          Class<T> clazz,
                                          ImportParamsConfigurer configurer) {

        if (file == null) {
            throw new IllegalArgumentException("multipartFile 不能为空");
        }
        if (file.isEmpty()) {
            throw new IllegalStateException("上传文件为空");
        }
        if (clazz == null) {
            throw new IllegalArgumentException("clazz 不能为空");
        }

        ImportParams params = new ImportParams();
        if (configurer != null) {
            configurer.configure(params);
        }

        try {
            return ExcelImportUtil.importExcel(file.getInputStream(), clazz, params);
        } catch (Exception e) {
            throw new IllegalStateException("Excel MultipartFile 导入失败", e);
        }
    }

    /**
     * 基于二进制数组导入 Excel 为对象列表（默认参数）
     *
     * <p>
     * 适用于：
     * </p>
     *
     * <ul>
     *     <li>文件内容已提前读取为 byte[] 的场景</li>
     *     <li>从数据库、缓存、对象存储中直接获取 Excel 二进制数据</li>
     *     <li>远程服务通过 RPC 传递 Excel 文件字节流</li>
     * </ul>
     *
     * <p>
     * 该方法使用 EasyPOI 默认 {@link ImportParams} 配置，
     * 适合结构简单、无需复杂控制的 Excel 文件。
     * </p>
     *
     * @param bytes Excel 文件二进制数据
     * @param clazz 目标实体类型
     * @param <T>   泛型类型
     * @return 导入后的对象集合
     */
    public static <T> List<T> importExcel(byte[] bytes, Class<T> clazz) {
        return importExcel(bytes, clazz, null);
    }

    /**
     * 基于二进制数组导入 Excel 为对象列表（支持函数式配置 ImportParams）
     *
     * <p>
     * 适用于：
     * </p>
     *
     * <ul>
     *     <li>从对象存储（OSS / MinIO / COS）直接下载为 byte[] 后导入</li>
     *     <li>无需落盘即可完成 Excel 导入的高性能场景</li>
     *     <li>微服务间通过消息或接口传输 Excel 数据</li>
     * </ul>
     *
     * <p>
     * 通过 {@link ImportParamsConfigurer} 可以灵活配置：
     * </p>
     *
     * <ul>
     *     <li>Excel 结构参数（titleRows、headRows、startRows）</li>
     *     <li>校验参数（needVerify、verifyHandler、verifyGroup）</li>
     *     <li>模板校验（importFields、needCheckOrder）</li>
     *     <li>多 Sheet 导入（startSheetIndex、sheetNum、sheetName）</li>
     *     <li>性能优化（concurrentTask、critical）</li>
     * </ul>
     *
     * <p>
     * 内部会将 byte[] 包装为 {@link java.io.ByteArrayInputStream}，
     * 不涉及任何本地文件读写，适合无磁盘依赖的云原生环境。
     * </p>
     *
     * @param bytes      Excel 文件二进制数据
     * @param clazz      目标实体类型
     * @param configurer ImportParams 参数配置回调，可为 null
     * @param <T>        泛型类型
     * @return 导入后的对象集合
     */
    public static <T> List<T> importExcel(byte[] bytes,
                                          Class<T> clazz,
                                          ImportParamsConfigurer configurer) {

        if (bytes == null || bytes.length == 0) {
            throw new IllegalArgumentException("bytes 不能为空");
        }
        if (clazz == null) {
            throw new IllegalArgumentException("clazz 不能为空");
        }

        ImportParams params = new ImportParams();
        if (configurer != null) {
            configurer.configure(params);
        }

        try (InputStream inputStream = new ByteArrayInputStream(bytes)) {
            return ExcelImportUtil.importExcel(inputStream, clazz, params);
        } catch (Exception e) {
            throw new IllegalStateException("Excel 二进制数据导入失败", e);
        }
    }

    /**
     * 基于 InputStream 导入 Excel 数据
     *
     * <p>
     * 这是最底层、最通用的一种导入方式，所有 File / Path / MultipartFile / ClassPath / byte[]
     * 最终都可以统一转换为 InputStream 后调用该方法。
     * </p>
     *
     * <p>
     * 适用场景：
     * </p>
     * <ul>
     *     <li>文件来源不确定（网络流、OSS、MinIO、FTP 等）</li>
     *     <li>统一封装导入入口，降低调用方复杂度</li>
     *     <li>微服务、云原生、无本地文件系统环境</li>
     * </ul>
     *
     * @param inputStream Excel 文件输入流
     * @param clazz       Excel 映射的实体类类型
     * @param <T>         泛型类型
     * @return 导入后的数据列表
     */
    public static <T> List<T> importExcel(InputStream inputStream,
                                          Class<T> clazz) {
        return importExcel(inputStream, clazz, null);
    }

    /**
     * 基于 InputStream 导入 Excel 数据，并支持自定义导入参数配置
     *
     * <p>
     * 推荐所有导入最终都走这个方法，是整个 Excel 导入体系的“核心入口”。
     * </p>
     *
     * <p>
     * 通过 {@link ImportParamsConfigurer} 可以灵活配置 {@link ImportParams}，例如：
     * </p>
     *
     * <ul>
     *     <li>设置标题行数：{@code params.setTitleRows(1)}</li>
     *     <li>设置表头行数：{@code params.setHeadRows(1)}</li>
     *     <li>开启校验：{@code params.setNeedVerify(true)}</li>
     *     <li>配置图片保存路径：{@code params.setSaveUrl("/excel/upload/excelUpload")}</li>
     *     <li>设置读取 Sheet 范围：{@code params.setStartSheetIndex(0)}</li>
     *     <li>设置读取 Sheet 数量：{@code params.setSheetNum(2)}</li>
     * </ul>
     *
     * <p>
     * 注意：
     * </p>
     * <ul>
     *     <li>InputStream 由调用方负责关闭，或在外层使用 try-with-resources 管理</li>
     *     <li>该方法不会主动关闭流，保证流的可控性</li>
     * </ul>
     *
     * @param inputStream Excel 文件输入流
     * @param clazz       Excel 映射的实体类类型
     * @param configurer  导入参数配置回调，可为 null
     * @param <T>         泛型类型
     * @return 导入后的数据列表
     */
    public static <T> List<T> importExcel(InputStream inputStream,
                                          Class<T> clazz,
                                          ImportParamsConfigurer configurer) {

        if (inputStream == null) {
            throw new IllegalArgumentException("inputStream 不能为空");
        }
        if (clazz == null) {
            throw new IllegalArgumentException("clazz 不能为空");
        }

        ImportParams params = new ImportParams();
        if (configurer != null) {
            configurer.configure(params);
        }

        try {
            return ExcelImportUtil.importExcel(inputStream, clazz, params);
        } catch (Exception e) {
            throw new IllegalStateException("Excel InputStream 导入失败", e);
        }
    }

    /**
     * 从 ClassPath 路径导入 Excel 数据
     *
     * <p>
     * 适用于以下场景：
     * </p>
     *
     * <ul>
     *     <li>Excel 模板或测试数据文件位于 resources 目录下</li>
     *     <li>单元测试、集成测试环境下读取内置 Excel 文件</li>
     *     <li>随应用一起打包发布的固定 Excel 资源文件</li>
     * </ul>
     *
     * <p>
     * 例如：
     * </p>
     *
     * <pre>
     * importFromClasspath("excel/import_users.xlsx", MyUser.class);
     * </pre>
     *
     * @param classpathLocation classpath 下的文件路径，例如：excel/import_users.xlsx
     * @param clazz             Excel 映射的实体类类型
     * @param <T>               泛型类型
     * @return 导入后的数据列表
     */
    public static <T> List<T> importExcelFromClasspath(String classpathLocation,
                                                       Class<T> clazz) {
        return importExcelFromClasspath(classpathLocation, clazz, null);
    }

    /**
     * 从 ClassPath 路径导入 Excel 数据，并支持自定义导入参数配置
     *
     * <p>
     * 通过 {@link ImportParamsConfigurer} 可对 {@link ImportParams} 进行灵活配置，例如：
     * </p>
     *
     * <ul>
     *     <li>是否需要校验：setNeedVerify(true)</li>
     *     <li>是否保存图片：setSaveUrl(...)</li>
     *     <li>标题行数：setTitleRows(...)</li>
     *     <li>表头行数：setHeadRows(...)</li>
     *     <li>开始导入行号：setStartRows(...)</li>
     *     <li>是否开启多 Sheet 导入：setNeedAllSheets(true)</li>
     * </ul>
     *
     * <p>
     * 该方法内部基于 InputStream 读取，不依赖真实文件路径，
     * 非常适合云原生与容器化环境。
     * </p>
     *
     * @param classpathLocation classpath 下的文件路径，例如：excel/import_users.xlsx
     * @param clazz             Excel 映射的实体类类型
     * @param configurer        导入参数配置回调，可为 null
     * @param <T>               泛型类型
     * @return 导入后的数据列表
     */
    public static <T> List<T> importExcelFromClasspath(String classpathLocation,
                                                       Class<T> clazz,
                                                       ImportParamsConfigurer configurer) {

        if (classpathLocation == null || classpathLocation.trim().isEmpty()) {
            throw new IllegalArgumentException("classpathLocation 不能为空");
        }
        if (clazz == null) {
            throw new IllegalArgumentException("clazz 不能为空");
        }

        ImportParams params = new ImportParams();
        if (configurer != null) {
            configurer.configure(params);
        }

        try (InputStream inputStream = getInputStreamFromClasspath(classpathLocation)) {
            return ExcelImportUtil.importExcel(inputStream, clazz, params);
        } catch (Exception e) {
            throw new IllegalStateException("Excel ClassPath 导入失败: " + classpathLocation, e);
        }
    }

    /**
     * 基于 InputStream 导入 Excel 数据（高级模式）
     *
     * <p>
     * 返回 {@link ExcelImportResult}，可获取：
     * </p>
     *
     * <ul>
     *     <li>成功数据列表：{@link ExcelImportResult#getList()}</li>
     *     <li>失败数据列表：{@link ExcelImportResult#getFailList()}</li>
     *     <li>是否存在校验失败：{@link ExcelImportResult#isVerifyFail()}</li>
     *     <li>失败数据 Excel：{@link ExcelImportResult#getFailWorkbook()}</li>
     * </ul>
     *
     * <p>
     * 适用于需要错误收集、失败行导出、失败原因回溯等完整导入场景。
     * </p>
     *
     * @param inputStream Excel 文件输入流
     * @param clazz       Excel 映射的实体类类型
     * @param <T>         泛型类型
     * @return Excel 导入完整结果对象
     */
    public static <T> ExcelImportResult<T> importExcelMore(InputStream inputStream,
                                                           Class<T> clazz) {
        return importExcelMore(inputStream, clazz, null);
    }

    /**
     * 基于 InputStream 导入 Excel 数据（高级模式），并支持完整导入结果返回
     *
     * <p>
     * 推荐所有需要错误收集、失败行导出、失败原因定位的导入统一走该方法，
     * 是整个 Excel 高级导入体系的“核心入口”。
     * </p>
     *
     * <p>
     * 通过 {@link ImportParamsConfigurer} 可以灵活配置 {@link ImportParams}，例如：
     * </p>
     *
     * <ul>
     *     <li>开启校验：{@code params.setNeedVerify(true)}</li>
     *     <li>设置校验处理器：{@code params.setVerifyHandler(new MyUserVerifyHandler())}</li>
     *     <li>是否忽略空行：{@code params.setIgnoreEmptyRow(true)}</li>
     *     <li>是否生成失败 Excel：{@code params.setNeedSave(true)}</li>
     *     <li>设置图片保存路径：{@code params.setSaveUrl("/excel/upload/excelUpload")}</li>
     * </ul>
     *
     * <p>
     * 注意：
     * </p>
     *
     * <ul>
     *     <li>InputStream 由调用方负责关闭</li>
     *     <li>该方法不会主动关闭流，保证流生命周期可控</li>
     * </ul>
     *
     * @param inputStream Excel 文件输入流
     * @param clazz       Excel 映射的实体类类型
     * @param configurer  导入参数配置回调，可为 null
     * @param <T>         泛型类型
     * @return Excel 导入完整结果对象
     */
    public static <T> ExcelImportResult<T> importExcelMore(InputStream inputStream,
                                                           Class<T> clazz,
                                                           ImportParamsConfigurer configurer) {

        if (inputStream == null) {
            throw new IllegalArgumentException("inputStream 不能为空");
        }
        if (clazz == null) {
            throw new IllegalArgumentException("clazz 不能为空");
        }

        ImportParams params = new ImportParams();
        if (configurer != null) {
            configurer.configure(params);
        }

        try {
            return ExcelImportUtil.importExcelMore(inputStream, clazz, params);
        } catch (Exception e) {
            throw new IllegalStateException("Excel InputStream 高级导入失败", e);
        }
    }

}
