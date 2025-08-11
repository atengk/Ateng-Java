package local.ateng.java.excel.utils;

import cn.idev.excel.EasyExcel;
import cn.idev.excel.ExcelReader;
import cn.idev.excel.ExcelWriter;
import cn.idev.excel.FastExcel;
import cn.idev.excel.enums.WriteDirectionEnum;
import cn.idev.excel.read.builder.ExcelReaderBuilder;
import cn.idev.excel.read.listener.PageReadListener;
import cn.idev.excel.read.listener.ReadListener;
import cn.idev.excel.read.metadata.ReadSheet;
import cn.idev.excel.util.StringUtils;
import cn.idev.excel.write.builder.ExcelWriterBuilder;
import cn.idev.excel.write.builder.ExcelWriterSheetBuilder;
import cn.idev.excel.write.handler.WriteHandler;
import cn.idev.excel.write.merge.LoopMergeStrategy;
import cn.idev.excel.write.metadata.WriteSheet;
import cn.idev.excel.write.metadata.fill.FillConfig;
import cn.idev.excel.write.metadata.fill.FillWrapper;
import cn.idev.excel.write.style.HorizontalCellStyleStrategy;
import org.springframework.core.io.ClassPathResource;
import org.springframework.util.CollectionUtils;
import org.springframework.web.multipart.MultipartFile;

import javax.servlet.http.HttpServletResponse;
import java.io.*;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

/**
 * Excel 读取工具类，基于 FastExcel 实现。
 * 支持多种读取模式，包括单 Sheet、指定 Sheet、无表头读取、自定义监听器等。
 * <p>
 * 更多说明见：
 * <a href="https://github.com/fast-excel/fastexcel">StringUtil 使用文档</a>
 * </p>
 *
 * @author 孔余
 * @since 2025-08-06
 */
public final class ExcelUtil {

    /**
     * 禁止实例化工具类
     */
    private ExcelUtil() {
        throw new UnsupportedOperationException("工具类不可实例化");
    }

    /**
     * Excel 2003 及更早版本的文件扩展名（后缀名：.xls）
     */
    public static final String EXCEL_XLS_SUFFIX = ".xls";

    /**
     * Excel 2007 及更高版本的文件扩展名（后缀名：.xlsx）
     */
    public static final String EXCEL_XLSX_SUFFIX = ".xlsx";

    /**
     * 默认的表头起始行号（从第 1 行开始）
     */
    public static final int DEFAULT_HEAD_ROW_NUM = 1;

    /**
     * 表示无表头的起始行号（从第 0 行开始读取数据）
     */
    public static final int NO_HEAD_ROW_NUM = 0;

    /**
     * 默认 Sheet 名称
     */
    public static final String DEFAULT_SHEET_NAME = "Sheet1";

    /**
     * 响应头名称：Content-Disposition
     */
    public static final String CONTENT_DISPOSITION_HEADER = "Content-Disposition";

    /**
     * 默认 Excel 文件扩展名（用于浏览器下载）
     */
    public static final String EXCEL_CONTENT_TYPE =
            "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet";

    /**
     * Content-Disposition 响应头前缀
     */
    public static final String CONTENT_DISPOSITION_PREFIX = "attachment; filename=";

    /**
     * 空格替换字符（用于 URLEncoder 编码）
     */
    public static final String SPACE_ENCODED = "%20";

    /**
     * UTF-8 编码格式
     */
    public static final String CHARACTER_ENCODING_UTF8 = "UTF-8";

    /**
     * 模板基础路径（classpath 资源路径）
     */
    public static final String TEMPLATE_BASE_PATH = "templates/";


    // -------------------------------------------- Excel 读取功能 --------------------------------------------

    /**
     * 默认读取第一个 Sheet，含表头，读取为指定 Java Bean 类型的数据列表。
     *
     * @param file  Excel 文件
     * @param clazz 数据模型类型
     * @param <T>   泛型类型
     * @return Java Bean 数据列表
     * @throws IOException 文件读取异常
     */
    public static <T> List<T> readFirstSheetWithHeader(MultipartFile file, Class<T> clazz) throws IOException {
        try (InputStream inputStream = file.getInputStream()) {
            return FastExcel.read(inputStream, clazz, null)
                    .sheet()
                    .headRowNumber(DEFAULT_HEAD_ROW_NUM)
                    .doReadSync();
        }
    }


    /**
     * 读取 Excel，支持配置是否忽略空行
     *
     * @param inputStream    Excel 输入流
     * @param ignoreEmptyRow 是否忽略空行，true 忽略，false 不忽略
     * @return 读取的数据列表，List<List<String>>
     */
    public static List<List<String>> readFirstSheetWithIgnoreEmptyRows(InputStream inputStream, boolean ignoreEmptyRow) {
        List<List<String>> resultList = new ArrayList<>();
        ExcelReaderBuilder builder = FastExcel.read(inputStream, new PageReadListener<List<String>>(dataList -> {
            for (List<String> row : dataList) {
                if (ignoreEmptyRow) {
                    boolean allEmpty = row.stream().allMatch(cell -> cell == null || cell.trim().isEmpty());
                    if (allEmpty) {
                        continue;
                    }
                }
                resultList.add(row);
            }
        }));
        builder.sheet().doRead();
        return resultList;
    }

    /**
     * 读取 Excel 所有 Sheet 页，含表头，读取为指定 Java Bean 类型的数据列表。
     * 每个 Sheet 的数据单独一份 List，返回整体为 List<List<T>>。
     *
     * @param file  Excel 文件
     * @param clazz 数据模型类型
     * @param <T>   泛型
     * @return 所有 Sheet 页的数据列表
     * @throws IOException 文件读取异常
     */
    public static <T> List<List<T>> readAllSheetsWithHeader(MultipartFile file, Class<T> clazz) throws IOException {
        try (InputStream inputStream = file.getInputStream()) {
            ExcelReader excelReader = FastExcel.read(inputStream).build();
            List<ReadSheet> readSheets = excelReader.excelExecutor().sheetList();
            List<List<T>> allData = new java.util.ArrayList<>();
            for (ReadSheet sheet : readSheets) {
                List<T> data = FastExcel.read(file.getInputStream(), clazz, null)
                        .sheet(sheet.getSheetNo())
                        .headRowNumber(DEFAULT_HEAD_ROW_NUM)
                        .doReadSync();
                allData.add(data);
            }
            excelReader.finish();
            return allData;
        }
    }

    /**
     * 读取指定 Sheet 页（通过 Sheet 索引），含表头。
     *
     * @param file    Excel 文件
     * @param clazz   数据模型类型
     * @param sheetNo Sheet 页索引，从 0 开始
     * @param <T>     泛型类型
     * @return 指定 Sheet 的数据列表
     * @throws IOException 文件读取异常
     */
    public static <T> List<T> readSheetWithHeader(MultipartFile file, Class<T> clazz, int sheetNo) throws IOException {
        try (InputStream inputStream = file.getInputStream()) {
            return FastExcel.read(inputStream, clazz, null)
                    .sheet(sheetNo)
                    .headRowNumber(DEFAULT_HEAD_ROW_NUM)
                    .doReadSync();
        }
    }

    /**
     * 读取 Excel 文件，不包含表头（即从第一行开始作为数据行）。
     *
     * @param file    Excel 文件
     * @param clazz   数据模型类型
     * @param sheetNo Sheet 页索引
     * @param <T>     泛型类型
     * @return 数据列表
     * @throws IOException 文件读取异常
     */
    public static <T> List<T> readSheetWithoutHeader(MultipartFile file, Class<T> clazz, int sheetNo) throws IOException {
        try (InputStream inputStream = file.getInputStream()) {
            return FastExcel.read(inputStream, clazz, null)
                    .sheet(sheetNo)
                    .headRowNumber(NO_HEAD_ROW_NUM)
                    .doReadSync();
        }
    }

    /**
     * 使用自定义监听器读取指定 Sheet 页，适用于需要数据校验、动态处理等高级需求。
     *
     * @param file         Excel 文件
     * @param clazz        数据模型类型
     * @param sheetNo      Sheet 页索引
     * @param readListener 自定义监听器
     * @param <T>          泛型类型
     * @throws IOException 文件读取异常
     */
    public static <T> void readWithCustomListener(MultipartFile file, Class<T> clazz, int sheetNo, ReadListener<T> readListener) throws IOException {
        try (InputStream inputStream = file.getInputStream()) {
            FastExcel.read(inputStream, clazz, readListener)
                    .sheet(sheetNo)
                    .doRead();
        }
    }

    /**
     * 判断文件是否为空或扩展名不合法。
     *
     * @param file Excel 文件
     * @return 是否有效
     */
    public static boolean isValidExcelFile(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            return false;
        }
        String fileName = file.getOriginalFilename();
        if (StringUtils.isBlank(fileName)) {
            return false;
        }
        String lowerCase = fileName.toLowerCase();
        return lowerCase.endsWith(EXCEL_XLS_SUFFIX) || lowerCase.endsWith(EXCEL_XLSX_SUFFIX);
    }

    // -------------------------------------------- Excel 写入功能 --------------------------------------------

    /**
     * 写入 Excel 到指定的输出流，适用于写入结构化的对象列表。
     * 适合输出到文件、网络响应流等。
     *
     * @param <T>          泛型对象类型
     * @param dataList     待写入的数据列表，不得为 null 或空
     * @param clazz        数据对应的类类型，不能为空
     * @param outputStream 目标输出流，不能为空
     * @throws IllegalArgumentException 如果参数不合法
     */
    public static <T> void writeToOutputStream(List<T> dataList, Class<T> clazz, OutputStream outputStream) {
        if (outputStream == null) {
            throw new IllegalArgumentException("输出流(outputStream)不能为空");
        }
        if (clazz == null) {
            throw new IllegalArgumentException("数据类型(clazz)不能为空");
        }
        if (dataList == null || dataList.isEmpty()) {
            throw new IllegalArgumentException("数据列表(dataList)不能为空或空列表");
        }

        EasyExcel.write(outputStream, clazz)
                .sheet(DEFAULT_SHEET_NAME)
                .doWrite(dataList);
    }

    /**
     * 写入 Excel 到指定的输出流，适用于写入结构化的对象列表，支持样式设置。
     * 适合输出到文件、网络响应流等。
     *
     * @param <T>          泛型对象类型
     * @param dataList     待写入的数据列表，不得为 null 或空
     * @param clazz        数据对应的类类型，不能为空
     * @param outputStream 目标输出流，不能为空
     * @param writeHandler 样式策略，支持表头和内容样式，允许为 null（则使用默认样式）
     * @throws IllegalArgumentException 如果参数不合法
     */
    public static <T> void writeToOutputStream(List<T> dataList,
                                               Class<T> clazz,
                                               OutputStream outputStream,
                                               WriteHandler writeHandler) {
        if (outputStream == null) {
            throw new IllegalArgumentException("输出流(outputStream)不能为空");
        }
        if (clazz == null) {
            throw new IllegalArgumentException("数据类型(clazz)不能为空");
        }
        if (dataList == null || dataList.isEmpty()) {
            throw new IllegalArgumentException("数据列表(dataList)不能为空或空列表");
        }

        ExcelWriterSheetBuilder builder = EasyExcel.write(outputStream, clazz)
                .sheet(DEFAULT_SHEET_NAME);

        if (writeHandler != null) {
            builder.registerWriteHandler(writeHandler);
        } else {
            // 如果没有传入样式，使用默认样式（可根据需求调整）
            builder.registerWriteHandler(ExcelStyleUtil.getDefaultStyleStrategy());
        }

        builder.doWrite(dataList);
    }

    /**
     * 写入 Excel 到指定的输出流，适用于写入结构化的对象列表，支持多样式策略设置。
     * 适合输出到文件、网络响应流等。
     *
     * <p>使用说明：
     * <pre>
     * // 例如传入多个样式处理器
     * List<WriteHandler> handlers = Arrays.asList(
     *     ExcelStyleUtil.getDefaultStyleStrategy(),
     *     ExcelStyleUtil.autoMergeStrategy(Arrays.asList(0, 1)),
     *     new CustomCellStyleHandler()
     * );
     * ExcelUtil.writeToOutputStream(dataList, MyData.class, outputStream, handlers);
     * </pre>
     *
     * @param <T>           泛型对象类型
     * @param dataList      待写入的数据列表，不得为 null 或空
     * @param clazz         数据对应的类类型，不能为空
     * @param outputStream  目标输出流，不能为空
     * @param writeHandlers 多个样式策略处理器，可为 null 或空，若为空则使用默认样式策略
     * @throws IllegalArgumentException 如果参数不合法
     */
    public static <T> void writeToOutputStream(List<T> dataList,
                                               Class<T> clazz,
                                               OutputStream outputStream,
                                               List<WriteHandler> writeHandlers) {
        if (outputStream == null) {
            throw new IllegalArgumentException("输出流(outputStream)不能为空");
        }
        if (clazz == null) {
            throw new IllegalArgumentException("数据类型(clazz)不能为空");
        }
        if (dataList == null || dataList.isEmpty()) {
            throw new IllegalArgumentException("数据列表(dataList)不能为空或空列表");
        }

        ExcelWriterSheetBuilder builder = EasyExcel.write(outputStream, clazz)
                .sheet(DEFAULT_SHEET_NAME);

        if (writeHandlers != null && !writeHandlers.isEmpty()) {
            // 依次注册所有样式处理器
            for (WriteHandler handler : writeHandlers) {
                if (handler != null) {
                    builder.registerWriteHandler(handler);
                }
            }
        } else {
            // 若无传入样式处理器，注册默认样式策略
            builder.registerWriteHandler(ExcelStyleUtil.getDefaultStyleStrategy());
        }

        builder.doWrite(dataList);
    }

    /**
     * 写入 Excel 到指定的输出流，适用于写入结构化的对象列表，支持多样式策略设置。
     * 适合输出到文件、网络响应流等。
     *
     * <p>使用说明：
     * <pre>
     * // 传入多个样式处理器
     * ExcelUtil.writeToOutputStream(dataList, MyData.class, outputStream,
     *     ExcelStyleUtil.getDefaultStyleStrategy(),
     *     ExcelStyleUtil.autoMergeStrategy(Arrays.asList(0, 1)),
     *     new CustomCellStyleHandler());
     * </pre>
     *
     * @param <T>           泛型对象类型
     * @param dataList      待写入的数据列表，不得为 null 或空
     * @param clazz         数据对应的类类型，不能为空
     * @param outputStream  目标输出流，不能为空
     * @param writeHandlers 可变参数样式策略处理器，允许不传或传空，默认使用默认样式策略
     * @throws IllegalArgumentException 如果参数不合法
     */
    public static <T> void writeToOutputStream(List<T> dataList,
                                               Class<T> clazz,
                                               OutputStream outputStream,
                                               WriteHandler... writeHandlers) {
        if (outputStream == null) {
            throw new IllegalArgumentException("输出流(outputStream)不能为空");
        }
        if (clazz == null) {
            throw new IllegalArgumentException("数据类型(clazz)不能为空");
        }
        if (dataList == null || dataList.isEmpty()) {
            throw new IllegalArgumentException("数据列表(dataList)不能为空或空列表");
        }

        ExcelWriterSheetBuilder builder = EasyExcel.write(outputStream, clazz)
                .sheet(DEFAULT_SHEET_NAME);

        if (writeHandlers != null && writeHandlers.length > 0) {
            for (WriteHandler handler : writeHandlers) {
                if (handler != null) {
                    builder.registerWriteHandler(handler);
                }
            }
        } else {
            builder.registerWriteHandler(ExcelStyleUtil.getDefaultStyleStrategy());
        }

        builder.doWrite(dataList);
    }

    /**
     * 写入多个 Sheet 到 Excel（每个 Sheet 一组数据）。
     * 用于多个同类或异类对象写入不同 Sheet。
     *
     * @param <T>          数据泛型
     * @param dataListList 每个 Sheet 的数据列表（List<List<T>>），不能为空
     * @param clazz        数据类类型（所有 sheet 通用）
     * @param sheetNames   对应 Sheet 名称列表，不能为空，长度必须一致
     * @param outputStream 输出流，不能为空
     * @throws IllegalArgumentException 参数为空或不匹配时抛出
     * @throws IOException              输出流异常
     */
    public static <T> void writeToOutputStream(List<List<T>> dataListList,
                                               Class<T> clazz,
                                               List<String> sheetNames,
                                               OutputStream outputStream) throws IOException {
        if (CollectionUtils.isEmpty(dataListList) || CollectionUtils.isEmpty(sheetNames)) {
            throw new IllegalArgumentException("数据列表或 Sheet 名称列表不能为空");
        }
        if (dataListList.size() != sheetNames.size()) {
            throw new IllegalArgumentException("数据列表和 Sheet 名称列表大小不一致");
        }
        if (outputStream == null || clazz == null) {
            throw new IllegalArgumentException("输出流和数据类型不能为空");
        }

        try (ExcelWriter excelWriter = EasyExcel.write(outputStream, clazz).build()) {
            for (int i = 0; i < dataListList.size(); i++) {
                List<T> dataList = dataListList.get(i);
                if (CollectionUtils.isEmpty(dataList)) {
                    continue;
                }
                String sheetName = sheetNames.get(i);
                WriteSheet writeSheet = EasyExcel.writerSheet(i, sheetName).build();
                excelWriter.write(dataList, writeSheet);
            }
        }
    }

    /**
     * 写入 Excel 到指定的文件路径，适用于写入结构化的对象列表。
     *
     * @param <T>      泛型对象类型
     * @param dataList 待写入的数据列表，不得为 null 或空
     * @param clazz    数据对应的类类型，不能为空
     * @param filePath 输出文件完整路径，不能为空或空字符串
     * @throws IOException              如果写入文件时发生 IO 异常
     * @throws IllegalArgumentException 如果参数不合法
     */
    public static <T> void writeToFile(List<T> dataList, Class<T> clazz, Path filePath) throws IOException {
        if (filePath == null) {
            throw new IllegalArgumentException("输出文件路径(filePath)不能为空");
        }
        if (clazz == null) {
            throw new IllegalArgumentException("数据类型(clazz)不能为空");
        }
        if (CollectionUtils.isEmpty(dataList)) {
            throw new IllegalArgumentException("数据列表(dataList)不能为空或空列表");
        }

        try (OutputStream outputStream = Files.newOutputStream(filePath)) {
            writeToOutputStream(dataList, clazz, outputStream);
        }
    }

    /**
     * 写入 Excel 到指定的文件路径，适用于写入结构化的对象列表。
     *
     * @param <T>      泛型对象类型
     * @param dataList 待写入的数据列表，不得为 null 或空
     * @param clazz    数据对应的类类型，不能为空
     * @param filePath 输出文件完整路径，不能为空或空字符串
     * @throws IOException              如果写入文件时发生 IO 异常
     * @throws IllegalArgumentException 如果参数不合法
     */
    public static <T> void writeToFile(List<T> dataList, Class<T> clazz, String filePath) throws IOException {
        if (StringUtils.isBlank(filePath)) {
            throw new IllegalArgumentException("文件路径(filePath)不能为空");
        }
        writeToFile(dataList, clazz, Paths.get(filePath));
    }

    /**
     * 写入多个 Sheet 到本地 Excel 文件。
     * 每个 Sheet 一组数据，Sheet 名称一一对应。
     *
     * @param <T>          数据泛型
     * @param dataListList 每个 Sheet 的数据列表（List<List<T>>），不能为空
     * @param clazz        数据类类型
     * @param sheetNames   对应 Sheet 名称列表，不能为空，长度必须一致
     * @param filePath     Excel 文件路径，不能为空
     * @throws IllegalArgumentException 参数为空或不匹配时抛出
     * @throws IOException              文件写入异常
     */
    public static <T> void writeToFile(List<List<T>> dataListList,
                                       Class<T> clazz,
                                       List<String> sheetNames,
                                       Path filePath) throws IOException {
        try (OutputStream outputStream = Files.newOutputStream(filePath)) {
            writeToOutputStream(dataListList, clazz, sheetNames, outputStream);
        }
    }

    /**
     * 写入多个 Sheet 到本地 Excel 文件。
     * 每个 Sheet 一组数据，Sheet 名称一一对应。
     *
     * @param <T>          数据泛型
     * @param dataListList 每个 Sheet 的数据列表（List<List<T>>），不能为空
     * @param clazz        数据类类型
     * @param sheetNames   对应 Sheet 名称列表，不能为空，长度必须一致
     * @param filePath     Excel 文件路径，不能为空
     * @throws IllegalArgumentException 参数为空或不匹配时抛出
     * @throws IOException              文件写入异常
     */
    public static <T> void writeToFile(List<List<T>> dataListList,
                                       Class<T> clazz,
                                       List<String> sheetNames,
                                       String filePath) throws IOException {
        if (StringUtils.isBlank(filePath)) {
            throw new IllegalArgumentException("文件路径(filePath)不能为空");
        }
        writeToFile(dataListList, clazz, sheetNames, Paths.get(filePath));
    }

    /**
     * 写入 Excel 文件到浏览器响应（实现文件下载）。
     * 支持结构化数据导出为 Excel，自动设置响应头、编码等。
     *
     * @param <T>      泛型数据类型
     * @param dataList 待写入的数据列表，不能为空或空列表
     * @param clazz    数据类的 Class 类型，不能为空
     * @param response HttpServletResponse 对象，不能为空
     * @param fileName 下载文件名（不含扩展名），不能为空
     * @throws IOException              写入响应流时发生的 IO 异常
     * @throws IllegalArgumentException 如果参数为空或不合法
     */
    public static <T> void writeToResponse(List<T> dataList, Class<T> clazz, HttpServletResponse response, String fileName) throws IOException {
        if (response == null) {
            throw new IllegalArgumentException("响应对象(response)不能为空");
        }
        if (clazz == null) {
            throw new IllegalArgumentException("数据类(clazz)不能为空");
        }
        if (CollectionUtils.isEmpty(dataList)) {
            throw new IllegalArgumentException("数据列表(dataList)不能为空或空列表");
        }
        if (StringUtils.isBlank(fileName)) {
            throw new IllegalArgumentException("文件名(fileName)不能为空");
        }

        // 编码文件名，避免中文乱码
        String encodedFileName = URLEncoder.encode(fileName, StandardCharsets.UTF_8.name())
                .replaceAll("\\+", SPACE_ENCODED);

        // 设置响应头，设置下载 Excel 文件
        response.setContentType(EXCEL_CONTENT_TYPE);
        response.setCharacterEncoding(StandardCharsets.UTF_8.name());
        response.setHeader(CONTENT_DISPOSITION_HEADER,
                CONTENT_DISPOSITION_PREFIX + encodedFileName + EXCEL_XLSX_SUFFIX);

        // 写入数据到响应流
        try (OutputStream outputStream = response.getOutputStream()) {
            writeToOutputStream(dataList, clazz, outputStream);
        }
    }

    /**
     * 写入多个 Sheet 到浏览器响应，触发 Excel 下载。
     * 每个 Sheet 一组数据，Sheet 名称一一对应。
     *
     * @param <T>          数据泛型
     * @param dataListList 每个 Sheet 的数据列表（List<List<T>>），不能为空
     * @param clazz        数据类类型
     * @param sheetNames   对应 Sheet 名称列表，不能为空，长度必须一致
     * @param response     HTTP 响应对象，不能为空
     * @param fileName     下载文件名（不含扩展名），不能为空
     * @throws IllegalArgumentException 参数为空或不匹配时抛出
     * @throws IOException              写出异常
     */
    public static <T> void writeToResponse(List<List<T>> dataListList,
                                           Class<T> clazz,
                                           List<String> sheetNames,
                                           HttpServletResponse response,
                                           String fileName) throws IOException {
        if (response == null) {
            throw new IllegalArgumentException("响应对象(response)不能为空");
        }
        if (clazz == null) {
            throw new IllegalArgumentException("数据类(clazz)不能为空");
        }
        if (CollectionUtils.isEmpty(dataListList)) {
            throw new IllegalArgumentException("数据列表(dataList)不能为空或空列表");
        }
        if (StringUtils.isBlank(fileName)) {
            throw new IllegalArgumentException("文件名(fileName)不能为空");
        }

        // 编码文件名，避免中文乱码
        String encodedFileName = URLEncoder.encode(fileName, StandardCharsets.UTF_8.name())
                .replaceAll("\\+", SPACE_ENCODED);

        // 设置响应头，设置下载 Excel 文件
        response.setContentType(EXCEL_CONTENT_TYPE);
        response.setCharacterEncoding(StandardCharsets.UTF_8.name());
        response.setHeader(CONTENT_DISPOSITION_HEADER,
                CONTENT_DISPOSITION_PREFIX + encodedFileName + EXCEL_XLSX_SUFFIX);

        try (OutputStream outputStream = response.getOutputStream()) {
            writeToOutputStream(dataListList, clazz, sheetNames, outputStream);
        }
    }

    /**
     * 写入 Excel：单级表头 + 多行数据
     *
     * <p>使用场景：数据源为 List<List<String>>，表头为 List<String>，适用于动态列导出</p>
     *
     * @param outputStream 输出流（如响应流或文件流）
     * @param headerList   单级表头，如 ["姓名", "年龄", "部门"]
     * @param dataList     数据内容，如 [["张三", "25", "技术部"], ["李四", "30", "市场部"]]
     */
    public static void writeWithSimpleHeader(OutputStream outputStream,
                                             List<String> headerList,
                                             List<List<String>> dataList) {
        // 参数校验
        validateHeader(headerList);

        // 构造 EasyExcel 支持的表头格式（每列封装为一个 List）
        List<List<String>> excelHead = buildSingleLevelHeader(headerList);

        // 执行写入
        writeWithHeader(outputStream, excelHead, dataList);
    }

    /**
     * 写入无表头的 Excel 文件。
     * 适用于导出无结构化列表数据，如 List<List<String>>。
     *
     * @param dataList     待写入的数据列表，不得为 null 或空
     * @param outputStream 输出目标流（文件流或响应流）
     * @throws IOException              如果写入过程中出现 IO 异常
     * @throws IllegalArgumentException 如果输出流或数据为空
     */
    public static void writeWithoutHead(List<List<String>> dataList, OutputStream outputStream) throws IOException {
        if (outputStream == null) {
            throw new IllegalArgumentException("输出流(outputStream)不能为空");
        }
        if (dataList == null || dataList.isEmpty()) {
            throw new IllegalArgumentException("数据列表(dataList)不能为空或空列表");
        }

        EasyExcel.write(outputStream)
                .needHead(false)
                .sheet(DEFAULT_SHEET_NAME)
                .doWrite(dataList);
    }

    /**
     * 写入 Excel：多级表头 + 多行数据
     * <p>
     * 表头横纵重复的会自动合并
     *
     * <p>使用场景：表头结构为多级 List，如 [["人员信息", "姓名"], ["人员信息", "年龄"]]</p>
     *
     * @param outputStream 输出流（如响应流或文件流）
     * @param headerList   多级表头结构
     * @param dataList     数据内容，如 [["张三", "25"], ["李四", "30"]]
     */
    public static void writeWithMultiLevelHeader(OutputStream outputStream,
                                                 List<List<String>> headerList,
                                                 List<List<String>> dataList) {
        // 参数校验
        validateHeader(headerList);

        // 执行写入
        writeWithHeader(outputStream, headerList, dataList);
    }

    /**
     * 写入 Excel：多级表头 + 多行数据
     * <p>
     * 表头横纵重复的会自动合并
     *
     * <p>使用场景：表头结构为多级 List，如 [["人员信息", "姓名"], ["人员信息", "年龄"]]</p>
     *
     * @param outputStream  输出流（如响应流或文件流）
     * @param headerList    多级表头结构
     * @param dataList      数据内容，如 [["张三", "25"], ["李四", "30"]]
     * @param writeHandlers 可变参数样式策略处理器，允许不传或传空，默认使用默认样式策略
     */
    public static void writeWithMultiLevelHeader(OutputStream outputStream,
                                                 List<List<String>> headerList,
                                                 List<List<String>> dataList,
                                                 WriteHandler... writeHandlers) {
        // 参数校验
        validateHeader(headerList);

        ExcelWriterSheetBuilder builder = EasyExcel.write(outputStream)
                .head(headerList)
                .autoCloseStream(false)
                .sheet(DEFAULT_SHEET_NAME);

        if (writeHandlers != null && writeHandlers.length > 0) {
            for (WriteHandler handler : writeHandlers) {
                if (handler != null) {
                    builder.registerWriteHandler(handler);
                }
            }
        }

        builder.doWrite(dataList);
    }


    /**
     * 通用写入方法（供单级/多级表头方法复用）
     *
     * @param outputStream 输出流
     * @param header       表头结构（单级或多级）
     * @param data         数据列表
     */
    private static void writeWithHeader(OutputStream outputStream,
                                        List<List<String>> header,
                                        List<List<String>> data) {
        EasyExcel.write(outputStream)
                .head(header)
                .sheet(DEFAULT_SHEET_NAME)
                .doWrite(data);
    }

    // -------------------------------------------- 模板导出功能（填充功能） --------------------------------------------

    /**
     * 使用输入流填充普通字段，输出到 OutputStream。
     *
     * @param inputStream  输入流，使用后不会关闭
     * @param outputStream 输出流，调用方负责关闭
     * @param data         普通字段数据对象（实体或 Map）
     */
    public static void fillTemplate(InputStream inputStream, OutputStream outputStream, Object data) {
        FastExcel.write(outputStream)
                .withTemplate(inputStream)
                .sheet()
                .doFill(data);
    }

    /**
     * 使用输入流填充 Map + 列表，输出到 OutputStream。
     *
     * @param inputStream  输入流，使用后不会关闭
     * @param outputStream 输出流，调用方负责关闭
     * @param mapData      普通字段数据 Map
     * @param list         列表数据
     * @param listName     模板中列表变量名
     */
    public static void fillMapAndList(InputStream inputStream, OutputStream outputStream,
                                      Map<String, Object> mapData, List<?> list, String listName) {
        ExcelWriter writer = FastExcel.write(outputStream)
                .withTemplate(inputStream)
                .build();
        WriteSheet sheet = FastExcel.writerSheet().build();
        writer.fill(mapData, sheet);
        writer.fill(new FillWrapper(listName, list), sheet);
        writer.finish();
    }

    /**
     * 使用输入流填充列表数据，支持横向或纵向填充，输出到 OutputStream。
     *
     * @param inputStream  输入流，使用后不会关闭
     * @param outputStream 输出流，调用方负责关闭
     * @param list         列表数据
     * @param listName     模板中列表变量名
     * @param horizontal   是否横向填充，false 为纵向
     */
    public static void fillListWithDirection(InputStream inputStream, OutputStream outputStream,
                                             List<?> list, String listName, boolean horizontal) {
        FillConfig fillConfig = FillConfig.builder()
                .direction(horizontal ? WriteDirectionEnum.HORIZONTAL : WriteDirectionEnum.VERTICAL)
                .build();
        ExcelWriter writer = FastExcel.write(outputStream)
                .withTemplate(inputStream)
                .build();
        WriteSheet sheet = FastExcel.writerSheet().build();
        writer.fill(new FillWrapper(listName, list), fillConfig, sheet);
        writer.finish();
    }

    /**
     * 使用输入流填充 Map + 列表，导出到浏览器响应。
     *
     * @param inputStream 输入流，使用后不会关闭
     * @param fileName    下载文件名（无扩展名）
     * @param mapData     普通字段 Map
     * @param list        列表数据
     * @param listName    模板中列表变量名
     * @param response    HTTP 响应对象
     * @throws IOException IO 异常
     */
    public static void fillTemplateToHttpResponse(InputStream inputStream, String fileName,
                                                  Map<String, Object> mapData, List<?> list, String listName,
                                                  HttpServletResponse response) throws IOException {
        if (StringUtils.isBlank(fileName)) {
            throw new IllegalArgumentException("导出文件名不能为空");
        }

        String encodedFileName = URLEncoder.encode(fileName, CHARACTER_ENCODING_UTF8)
                .replaceAll("\\+", SPACE_ENCODED);

        response.setContentType(EXCEL_CONTENT_TYPE);
        response.setCharacterEncoding(CHARACTER_ENCODING_UTF8);
        response.setHeader(CONTENT_DISPOSITION_HEADER, CONTENT_DISPOSITION_PREFIX + encodedFileName + EXCEL_XLSX_SUFFIX);

        try (OutputStream outputStream = response.getOutputStream()) {
            ExcelWriter writer = FastExcel.write(outputStream)
                    .withTemplate(inputStream)
                    .build();
            WriteSheet sheet = FastExcel.writerSheet().build();
            writer.fill(mapData, sheet);
            writer.fill(new FillWrapper(listName, list), sheet);
            writer.finish();
        }
    }

    // -------------------------------------------- 高级功能 --------------------------------------------

    /**
     * 自动适配列宽写入 Excel（注意：性能较差，不推荐大量数据使用）
     *
     * @param outputStream 输出流
     * @param dataList     数据列表
     * @param clazz        数据类类型
     */
    public static void writeWithAutoColumnWidth(OutputStream outputStream, List<?> dataList, Class<?> clazz) {
        FastExcel.write(outputStream, clazz)
                .registerWriteHandler(ExcelStyleUtil.autoColumnWidthStrategy())
                .sheet(DEFAULT_SHEET_NAME)
                .doWrite(dataList);
    }

    /**
     * 写入 Excel 并设置表头和内容样式
     *
     * @param outputStream 输出流
     * @param dataList     数据列表
     * @param clazz        数据类类型
     */
    public static void writeWithStyle(OutputStream outputStream, List<?> dataList, Class<?> clazz) {
        HorizontalCellStyleStrategy styleStrategy = ExcelStyleUtil.getDefaultStyleStrategy();
        FastExcel.write(outputStream, clazz)
                .registerWriteHandler(styleStrategy)
                .sheet(DEFAULT_SHEET_NAME)
                .doWrite(dataList);
    }

    /**
     * 写入 Excel 时自动合并列
     *
     * @param outputStream 输出流
     * @param dataList     数据列表
     * @param clazz        数据类类型
     * @param eachRow      每几行合并一次
     * @param columnIndex  要合并的列索引（从 0 开始）
     */
    public static void writeWithMerge(OutputStream outputStream, List<?> dataList, Class<?> clazz, int eachRow, int columnIndex) {
        WriteHandler mergeStrategy = new LoopMergeStrategy(eachRow, columnIndex);
        FastExcel.write(outputStream, clazz)
                .registerWriteHandler(mergeStrategy)
                .sheet(DEFAULT_SHEET_NAME)
                .doWrite(dataList);
    }

    /**
     * 写入 Excel 并设置隐藏列和冻结窗口
     *
     * @param outputStream   输出流
     * @param dataList       数据列表
     * @param clazz          数据类类型
     * @param freezeColCount 冻结的列数（从第 0 列开始）
     * @param hiddenCols     需要隐藏的列索引数组（允许为 null 或空数组）
     */
    public static void writeWithHiddenAndFreeze(OutputStream outputStream, List<?> dataList, Class<?> clazz,
                                                int freezeColCount, int[] hiddenCols) {

        ExcelWriterBuilder writerBuilder = FastExcel.write(outputStream, clazz)
                .registerWriteHandler(ExcelStyleUtil.freezeAndHiddenStrategy(freezeColCount, hiddenCols));

        writerBuilder.sheet(DEFAULT_SHEET_NAME)
                .doWrite(dataList);
    }

    /**
     * 分批写入（用于大数据导出优化）
     *
     * @param outputStream 输出流
     * @param batchData    分批数据列表
     * @param clazz        数据类类型
     */
    public static void writeBatch(OutputStream outputStream, List<List<?>> batchData, Class<?> clazz) {
        try (ExcelWriter writer = FastExcel.write(outputStream, clazz).build()) {
            WriteSheet writeSheet = FastExcel.writerSheet(DEFAULT_SHEET_NAME).build();
            for (List<?> dataChunk : batchData) {
                writer.write(dataChunk, writeSheet);
            }
        }
    }

    // -------------------------------------------- 辅助功能 --------------------------------------------

    /**
     * 校验文件是否为合法的 Excel 文件格式（.xls 或 .xlsx）
     *
     * @param file 文件对象
     * @return 合法返回 true，非法返回 false
     */
    public static boolean isValidExcelFile(File file) {
        if (file == null || !file.exists() || !file.isFile()) {
            return false;
        }
        String fileName = file.getName().toLowerCase();
        return fileName.endsWith(EXCEL_XLSX_SUFFIX)
                || fileName.endsWith(EXCEL_XLS_SUFFIX);
    }

    /**
     * 校验 Excel 是否为空模板（无内容行）
     *
     * @param inputStream Excel 输入流
     * @return 如果至少有一条数据则返回 false，否则返回 true
     */
    public static boolean isEmptyTemplate(InputStream inputStream) {
        if (inputStream == null) {
            return true;
        }
        final boolean[] isEmpty = {true};
        FastExcel.read(inputStream, new PageReadListener<>(dataList -> {
            if (dataList != null && !dataList.isEmpty()) {
                isEmpty[0] = false;
            }
        })).sheet().doRead();
        return isEmpty[0];
    }


    /**
     * 将 File 转为 InputStream
     *
     * @param file 文件对象
     * @return 输入流
     * @throws IOException IO 异常
     */
    public static InputStream toInputStream(File file) throws IOException {
        if (file == null) {
            throw new IllegalArgumentException("File 不能为 null");
        }
        return Files.newInputStream(file.toPath());
    }

    /**
     * 将 MultipartFile 转为 InputStream
     *
     * @param multipartFile Spring MultipartFile
     * @return 输入流
     * @throws IOException IO 异常
     */
    public static InputStream toInputStream(MultipartFile multipartFile) throws IOException {
        if (multipartFile == null || multipartFile.isEmpty()) {
            throw new IllegalArgumentException("MultipartFile 不能为空或空文件");
        }
        return multipartFile.getInputStream();
    }

    /**
     * 从 classpath 加载 Excel 转为 InputStream
     *
     * @param templatePath 模板在 resources 下的路径（例如 templates/user_template.xlsx）
     * @return 模板输入流
     * @throws FileNotFoundException 模板不存在时抛出
     */
    public static InputStream toInputStream(ClassPathResource templatePath) throws IOException {
        if (!templatePath.exists()) {
            throw new FileNotFoundException("模板文件不存在: " + templatePath);
        }
        return templatePath.getInputStream();
    }

    /**
     * 获取 Excel 文件中所有 sheet 的名称列表
     *
     * @param inputStream Excel 输入流
     * @return sheet 名称列表
     */
    public static List<String> getSheetNames(InputStream inputStream) {
        List<String> sheetNames = new ArrayList<>();
        List<ReadSheet> sheets = FastExcel.read(inputStream).build().excelExecutor().sheetList();
        for (ReadSheet sheet : sheets) {
            sheetNames.add(sheet.getSheetName());
        }
        return sheetNames;
    }

    /**
     * 读取 Excel 的第一行作为表头，动态映射后续数据
     *
     * @param inputStream Excel 输入流
     * @return 表头列表
     */
    public static List<String> readExcelHeader(InputStream inputStream) {
        List<String> headerList = new ArrayList<>();
        FastExcel.read(inputStream, new PageReadListener<List<String>>(dataList -> {
            if (!dataList.isEmpty()) {
                headerList.addAll(dataList.get(0));
            }
        })).sheet().doRead();
        return headerList;
    }

    /**
     * 构建 EasyExcel 所需格式的单级表头（每个字段作为一列）
     *
     * @param headerList 原始单级表头
     * @return 多级封装形式的单级表头
     */
    public static List<List<String>> buildSingleLevelHeader(List<String> headerList) {
        List<List<String>> result = new ArrayList<>();
        for (String head : headerList) {
            result.add(Collections.singletonList(head));
        }
        return result;
    }

    /**
     * 表头参数校验（单级或多级）
     *
     * @param header 表头
     */
    public static void validateHeader(List<?> header) {
        if (header == null || header.isEmpty()) {
            throw new IllegalArgumentException("表头(headerList)不能为空");
        }
    }

}
