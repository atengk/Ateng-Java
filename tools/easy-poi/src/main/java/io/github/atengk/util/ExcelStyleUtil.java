package io.github.atengk.util;

import org.apache.poi.ss.usermodel.*;

/**
 * Excel 样式后处理工具类。
 * <p>
 * 适用于 EasyPOI 导出完成后的 Workbook 二次加工场景，
 * 通过“表头名称”定位指定列，对该列下所有数据单元格统一应用自定义样式规则。
 * </p>
 * <p>
 * 设计思想：
 * <ul>
 *     <li>不依赖 EasyPOI 内部样式回调机制，避免样式不生效问题</li>
 *     <li>直接基于 Apache POI 对 Workbook 进行后处理，稳定可控</li>
 *     <li>以“表头名称”为唯一定位依据，避免列顺序变动导致样式失效</li>
 * </ul>
 * <p>
 * 主要能力：
 * <ul>
 *     <li>支持通过 Sheet 下标或 Sheet 名称定位工作表</li>
 *     <li>支持自动扫描多行表头（1 行 / 2 行 / 3 行…）</li>
 *     <li>自动从表头下一行作为数据起始行</li>
 *     <li>支持为指定列批量应用任意样式策略</li>
 * </ul>
 * <p>
 * 典型使用示例：
 * <pre>
 * ExcelStyleUtil.applyByTitle(workbook, 0, "分数", 5, (wb, cell) -> {
 *     int score = (int) cell.getNumericCellValue();
 *     if (score < 60) {
 *         CellStyle style = wb.createCellStyle();
 *         style.setFillForegroundColor(IndexedColors.ROSE.getIndex());
 *         style.setFillPattern(FillPatternType.SOLID_FOREGROUND);
 *         style.setAlignment(HorizontalAlignment.CENTER);
 *         cell.setCellStyle(style);
 *     }
 * });
 * </pre>
 * <p>
 * 该类是一个纯工具类，不允许被实例化。
 *
 * @author 孔余
 * @since 2026-01-22
 */
public final class ExcelStyleUtil {

    private ExcelStyleUtil() {
    }

    /**
     * 通过表头名称，对指定列应用样式处理。
     * 默认 Sheet 第 1 个，最大扫描表头行数为 3
     *
     * @param workbook  工作簿对象
     * @param titleName 表头名称，例如：分数、年龄、状态
     * @param handler   单元格样式处理器
     */
    public static void applyByTitle(
            Workbook workbook,
            String titleName,
            CellStyleHandler handler) {

        Sheet sheet = workbook.getSheetAt(0);
        applyInternal(workbook, sheet, titleName, 3, handler);
    }

    /**
     * 通过 Sheet 下标 + 表头名称，对指定列应用样式处理。
     *
     * @param workbook         工作簿对象
     * @param sheetIndex       Sheet 下标，从 0 开始
     * @param titleName        表头名称，例如：分数、年龄、状态
     * @param maxHeaderRowScan 最大扫描表头行数，用于适配多行表头结构
     *                         通常取值 3~5 即可
     * @param handler          单元格样式处理器
     */
    public static void applyByTitle(
            Workbook workbook,
            int sheetIndex,
            String titleName,
            int maxHeaderRowScan,
            CellStyleHandler handler) {

        Sheet sheet = workbook.getSheetAt(sheetIndex);
        applyInternal(workbook, sheet, titleName, maxHeaderRowScan, handler);
    }

    /**
     * 通过 Sheet 名称 + 表头名称，对指定列应用样式处理。
     *
     * @param workbook         工作簿对象
     * @param sheetName        Sheet 名称
     * @param titleName        表头名称，例如：分数、年龄、状态
     * @param maxHeaderRowScan 最大扫描表头行数，用于适配多行表头结构
     * @param handler          单元格样式处理器
     */
    public static void applyByTitle(
            Workbook workbook,
            String sheetName,
            String titleName,
            int maxHeaderRowScan,
            CellStyleHandler handler) {

        Sheet sheet = workbook.getSheet(sheetName);
        if (sheet == null) {
            return;
        }
        applyInternal(workbook, sheet, titleName, maxHeaderRowScan, handler);
    }

    /**
     * 内部统一处理逻辑。
     * <p>
     * 主要流程：
     * <ol>
     *     <li>在前 N 行中定位表头所在行与列</li>
     *     <li>以表头行的下一行作为数据起始行</li>
     *     <li>对目标列的所有单元格逐个执行样式处理器</li>
     * </ol>
     */
    private static void applyInternal(
            Workbook workbook,
            Sheet sheet,
            String titleName,
            int maxHeaderRowScan,
            CellStyleHandler handler) {

        HeaderLocation location = findHeader(sheet, titleName, maxHeaderRowScan);
        if (location == null) {
            return;
        }

        int headerRowIndex = location.headerRowIndex;
        int colIndex = location.colIndex;

        int firstDataRowIndex = headerRowIndex + 1;

        for (int rowIndex = firstDataRowIndex; rowIndex <= sheet.getLastRowNum(); rowIndex++) {
            Row row = sheet.getRow(rowIndex);
            if (row == null) {
                continue;
            }

            Cell cell = row.getCell(colIndex);
            if (cell == null) {
                continue;
            }

            handler.handle(workbook, cell);
        }
    }

    /**
     * 在指定的前 N 行中扫描表头名称，返回表头所在的行号与列号。
     *
     * @param sheet            当前 Sheet
     * @param titleName        表头名称
     * @param maxHeaderRowScan 最大扫描行数
     * @return 表头位置信息，未找到返回 null
     */
    private static HeaderLocation findHeader(
            Sheet sheet,
            String titleName,
            int maxHeaderRowScan) {

        int scanLimit = Math.min(maxHeaderRowScan, sheet.getLastRowNum() + 1);

        for (int rowIndex = 0; rowIndex < scanLimit; rowIndex++) {
            Row row = sheet.getRow(rowIndex);
            if (row == null) {
                continue;
            }

            for (int colIndex = 0; colIndex < row.getLastCellNum(); colIndex++) {
                Cell cell = row.getCell(colIndex);
                if (cell == null) {
                    continue;
                }

                if (cell.getCellType() == CellType.STRING
                        && titleName.equals(cell.getStringCellValue().trim())) {
                    return new HeaderLocation(rowIndex, colIndex);
                }
            }
        }
        return null;
    }

    /**
     * 表头定位结果封装对象。
     * <p>
     * 用于同时返回：
     * <ul>
     *     <li>表头所在行号</li>
     *     <li>表头所在列号</li>
     * </ul>
     */
    private static final class HeaderLocation {

        private final int headerRowIndex;
        private final int colIndex;

        private HeaderLocation(int headerRowIndex, int colIndex) {
            this.headerRowIndex = headerRowIndex;
            this.colIndex = colIndex;
        }
    }
}
