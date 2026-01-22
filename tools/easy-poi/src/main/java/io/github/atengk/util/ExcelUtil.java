package io.github.atengk.util;

import cn.afterturn.easypoi.excel.ExcelExportUtil;
import cn.afterturn.easypoi.excel.entity.TemplateExportParams;
import org.apache.poi.ss.usermodel.Workbook;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;

import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
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
 * Workbook workbook = ExcelUtil.exportByTemplate("doc/user_template.xlsx", data);
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
    public static void exportToFile(Workbook workbook, Path filePath) {
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
            }
        } catch (IOException e) {
            throw new IllegalStateException("导出 Excel 文件失败: " + filePath, e);
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
    public static void exportToResponse(
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
            }
        } catch (IOException e) {
            throw new IllegalStateException("通过接口导出 Excel 失败", e);
        }
    }

    /**
     * 读取 Excel 模板并导出
     *
     * @param templatePath 模板路径（相对于 resources），如：doc/user_template.xlsx
     * @param data         模板参数数据
     * @return 填充完成后的 Workbook
     */
    public static Workbook exportByTemplate(String templatePath, Map<String, Object> data) {
        Resource resource = new ClassPathResource(templatePath);

        if (!resource.exists()) {
            throw new IllegalArgumentException("模板文件不存在: " + templatePath);
        }

        try (InputStream inputStream = resource.getInputStream()) {
            TemplateExportParams params = new TemplateExportParams(inputStream);
            return ExcelExportUtil.exportExcel(params, data);
        } catch (IOException e) {
            throw new IllegalStateException("读取模板文件失败: " + templatePath, e);
        }
    }

    /**
     * 通过模板文件流导出 Excel
     *
     * <p>
     * 适用于模板来源不固定的场景，例如：
     * - 文件服务器
     * - 对象存储（OSS、MinIO）
     * - 远程下载
     * - 数据库存储模板
     * </p>
     *
     * <p>
     * 注意：
     * - 该方法不会关闭传入的 InputStream，调用方自行管理生命周期
     * - 适合对流进行复用或统一关闭管理的场景
     * </p>
     *
     * @param templateInputStream 模板文件输入流
     * @param data                模板参数数据
     * @return 填充完成后的 Workbook
     */
    public static Workbook exportByTemplate(InputStream templateInputStream,
                                            Map<String, Object> data) {

        if (templateInputStream == null) {
            throw new IllegalArgumentException("templateInputStream 不能为空");
        }
        if (data == null) {
            throw new IllegalArgumentException("data 不能为空");
        }

        try {
            TemplateExportParams params = new TemplateExportParams(templateInputStream);
            return ExcelExportUtil.exportExcel(params, data);
        } catch (Exception e) {
            throw new IllegalStateException("通过模板流导出 Excel 失败", e);
        }
    }

}
