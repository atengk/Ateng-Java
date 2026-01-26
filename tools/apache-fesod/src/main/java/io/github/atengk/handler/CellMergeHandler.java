package io.github.atengk.handler;

import org.apache.fesod.sheet.write.handler.CellWriteHandler;
import org.apache.fesod.sheet.write.handler.context.CellWriteHandlerContext;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.ss.util.CellRangeAddress;

import java.text.SimpleDateFormat;
import java.util.*;

/**
 * Excel 单元格自动纵向合并策略（支持指定列或所有列）。
 * <p>
 * 功能特点：
 * <ul>
 *     <li>连续相邻行相同值自动合并（支持多列选择）</li>
 *     <li>合并后保留原单元格样式，仅增加水平垂直居中</li>
 *     <li>支持公式计算后的显示值比较，避免类型差异</li>
 *     <li>使用缓存减少重复样式创建</li>
 * </ul>
 * 使用场景：
 * <pre>
 * EasyExcel.write(file, Data.class)
 *     .registerWriteHandler(new CellMergeHandler(0, 1))
 *     .sheet().doWrite(dataList);
 * </pre>
 *
 * @author 孔余
 * @since 2026-01-26
 */
public class CellMergeHandler implements CellWriteHandler {

    private static final SimpleDateFormat DATE_TIME_FORMAT =
            new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");

    /**
     * 需要合并的列索引（从 0 开始）。
     * 数组为空表示处理所有列。
     */
    private final int[] mergeColumns;

    /**
     * 是否合并所有列（根据 mergeColumns 判断）。
     */
    private final boolean mergeAllColumns;

    /**
     * 单元格值格式化工具，用于比较时统一格式。
     */
    private DataFormatter formatter;

    /**
     * 公式计算器（懒加载，按需创建）。
     */
    private FormulaEvaluator evaluator;

    /**
     * 样式缓存，key 为原样式，value 为添加了居中属性的新样式。
     * 避免频繁创建重复的 CellStyle 对象。
     */
    private final Map<CellStyle, CellStyle> styleCache = new HashMap<>();

    /**
     * 列合并区域缓存。
     * key 为列索引，value 为该列当前的最后一个合并区域。
     */
    private final Map<Integer, CellRangeAddress> lastMergedRegionByCol = new HashMap<>();

    /**
     * 构造方法，合并所有列。
     */
    public CellMergeHandler() {
        this.mergeColumns = new int[0];
        this.mergeAllColumns = true;
    }

    /**
     * 构造方法，仅合并指定列。
     *
     * @param mergeColumns 列索引（从 0 开始）；null 或空表示合并所有列
     */
    public CellMergeHandler(int... mergeColumns) {
        this.mergeColumns = mergeColumns == null ? new int[0] : Arrays.copyOf(mergeColumns, mergeColumns.length);
        this.mergeAllColumns = this.mergeColumns.length == 0;
    }

    @Override
    public void afterCellDispose(CellWriteHandlerContext context) {
        // 跳过表头行
        if (Boolean.TRUE.equals(context.getHead())) {
            return;
        }

        // 获取当前单元格与 Sheet
        final Cell cell = context.getCell();
        if (cell == null) {
            return;
        }
        final Sheet sheet = context.getWriteSheetHolder().getSheet();
        if (sheet == null) {
            return;
        }

        // 初始化工具类（懒加载）
        if (formatter == null) {
            formatter = new DataFormatter(Locale.getDefault());
        }
        if (evaluator == null) {
            evaluator = sheet.getWorkbook().getCreationHelper().createFormulaEvaluator();
        }

        final int rowIndex = cell.getRowIndex();
        final int colIndex = cell.getColumnIndex();

        // 检查是否需要处理该列
        if (!shouldMergeColumn(colIndex)) {
            return;
        }

        // 第一行不合并
        if (rowIndex <= 0) {
            return;
        }

        // 获取当前值与上一行值
        final String current = getCellText(cell);
        if (isBlank(current)) {
            return;
        }
        final String prev = getCellText(getCell(sheet, rowIndex - 1, colIndex));

        // 值不同则不合并
        if (!Objects.equals(current, prev)) {
            return;
        }

        // 查找或创建合并区域
        CellRangeAddress region = lastMergedRegionByCol.get(colIndex);
        if (region == null || region.getLastRow() != rowIndex - 1) {
            Integer idx = findMergedRegionIndex(sheet, rowIndex - 1, colIndex);
            if (idx != null) {
                region = sheet.getMergedRegion(idx);
                region = new CellRangeAddress(region.getFirstRow(), region.getLastRow(),
                        region.getFirstColumn(), region.getLastColumn());
            } else {
                CellRangeAddress newRegion = new CellRangeAddress(rowIndex - 1, rowIndex, colIndex, colIndex);
                sheet.addMergedRegion(newRegion);
                setRegionCenterStyle(sheet, newRegion);
                lastMergedRegionByCol.put(colIndex, newRegion);
                return;
            }
        }

        // 扩展已有合并区域
        removeRegionIfPresent(sheet, region);
        CellRangeAddress extended = new CellRangeAddress(region.getFirstRow(), rowIndex,
                colIndex, colIndex);
        sheet.addMergedRegion(extended);
        setRegionCenterStyle(sheet, extended);
        lastMergedRegionByCol.put(colIndex, extended);
    }

    /**
     * 判断当前列是否需要合并。
     *
     * @param colIndex 列索引
     * @return 是否需要处理
     */
    private boolean shouldMergeColumn(int colIndex) {
        if (mergeAllColumns) {
            return true;
        }
        for (int c : mergeColumns) {
            if (c == colIndex) {
                return true;
            }
        }
        return false;
    }

    /**
     * 获取单元格文本值。
     * 统一使用 DataFormatter 格式化，支持公式计算。
     *
     * @param cell 单元格
     * @return 格式化后的文本
     */
    private String getCellText(Cell cell) {
        if (cell == null) {
            return "";
        }

        CellType type = cell.getCellType();
        if (type == CellType.FORMULA) {
            type = cell.getCachedFormulaResultType();
        }

        switch (type) {
            case STRING:
                return trimToEmpty(cell.getStringCellValue());

            case NUMERIC:
                try {
                    Date date = cell.getDateCellValue();
                    return formatDate(date);
                } catch (Exception e) {
                    // 不是日期，才当普通数字处理
                    return trimToEmpty(formatter.formatCellValue(cell));
                }

            case BOOLEAN:
                return String.valueOf(cell.getBooleanCellValue());

            case BLANK:
                return "";

            default:
                return trimToEmpty(formatter.formatCellValue(cell));
        }
    }

    /**
     * 时间格式化
     *
     * @param date 时间
     * @return 格式化时间字符串
     */
    private String formatDate(Date date) {
        if (date == null) {
            return "";
        }
        return DATE_TIME_FORMAT.format(date);
    }

    /**
     * 获取指定位置的单元格。
     *
     * @param sheet Sheet
     * @param row   行号
     * @param col   列号
     * @return 单元格
     */
    private Cell getCell(Sheet sheet, int row, int col) {
        if (sheet == null || row < 0 || col < 0) {
            return null;
        }
        Row r = sheet.getRow(row);
        return (r == null) ? null : r.getCell(col);
    }

    /**
     * 查找包含指定行列的单列合并区域索引。
     *
     * @param sheet Sheet
     * @param row   行号
     * @param col   列号
     * @return 区域索引，找不到返回 null
     */
    private Integer findMergedRegionIndex(Sheet sheet, int row, int col) {
        int num = sheet.getNumMergedRegions();
        for (int i = 0; i < num; i++) {
            CellRangeAddress region = sheet.getMergedRegion(i);
            if (region.getFirstColumn() == col && region.getLastColumn() == col
                    && region.getFirstRow() <= row && row <= region.getLastRow()) {
                return i;
            }
        }
        return null;
    }

    /**
     * 删除指定的合并区域（按范围精确匹配）。
     *
     * @param sheet  Sheet
     * @param target 合并区域
     */
    private void removeRegionIfPresent(Sheet sheet, CellRangeAddress target) {
        if (sheet == null || target == null) {
            return;
        }
        for (int i = sheet.getNumMergedRegions() - 1; i >= 0; i--) {
            CellRangeAddress r = sheet.getMergedRegion(i);
            if (r.getFirstRow() == target.getFirstRow()
                    && r.getLastRow() == target.getLastRow()
                    && r.getFirstColumn() == target.getFirstColumn()
                    && r.getLastColumn() == target.getLastColumn()) {
                sheet.removeMergedRegion(i);
                return;
            }
        }
    }

    /**
     * 将合并区域设置为居中（保留原样式，使用缓存减少重复创建）。
     *
     * @param sheet  Sheet
     * @param region 合并区域
     */
    private void setRegionCenterStyle(Sheet sheet, CellRangeAddress region) {
        if (sheet == null || region == null) {
            return;
        }

        Workbook wb = sheet.getWorkbook();

        for (int row = region.getFirstRow(); row <= region.getLastRow(); row++) {
            Row r = sheet.getRow(row);
            if (r == null) {
                continue;
            }

            for (int col = region.getFirstColumn(); col <= region.getLastColumn(); col++) {
                Cell cell = r.getCell(col);
                if (cell == null) {
                    continue;
                }

                CellStyle oldStyle = cell.getCellStyle();
                CellStyle newStyle = styleCache.get(oldStyle);
                if (newStyle == null) {
                    newStyle = wb.createCellStyle();
                    if (oldStyle != null) {
                        newStyle.cloneStyleFrom(oldStyle);
                    }
                    newStyle.setAlignment(HorizontalAlignment.CENTER);
                    newStyle.setVerticalAlignment(VerticalAlignment.CENTER);
                    styleCache.put(oldStyle, newStyle);
                }
                cell.setCellStyle(newStyle);
            }
        }
    }

    /**
     * 判断字符串是否为空白。
     *
     * @param s 字符串
     * @return 是否为空白
     */
    private static boolean isBlank(String s) {
        return s == null || s.trim().isEmpty();
    }

    /**
     * 去除首尾空格，null 转为空串。
     *
     * @param s 字符串
     * @return 去空格后的字符串
     */
    private static String trimToEmpty(String s) {
        return s == null ? "" : s.trim();
    }
}
