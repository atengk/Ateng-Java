package local.ateng.java.excel.handler;

import cn.idev.excel.write.handler.CellWriteHandler;
import cn.idev.excel.write.handler.context.CellWriteHandlerContext;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.ss.util.CellRangeAddress;

import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

/**
 * Excel 单元格自动合并策略（支持指定行列范围，连续相邻行和列相同值自动合并）
 *
 * <p>功能特点：
 * <ul>
 *     <li>连续相邻行、列相同值自动合并，可形成矩形区域</li>
 *     <li>支持指定合并的行范围、列范围</li>
 *     <li>合并后保留原单元格样式，并设置水平垂直居中</li>
 *     <li>支持公式计算后的显示值比较</li>
 *     <li>使用缓存减少重复样式创建</li>
 * </ul>
 *
 * 使用示例：
 * <pre>
 * EasyExcel.write(file, Data.class)
 *     .registerWriteHandler(new RowColMergeStrategy(1, 10, 0, 3))
 *     .sheet().doWrite(dataList);
 * </pre>
 *
 * @author
 * @since 2025-08-15
 */
public class RowColMergeStrategy implements CellWriteHandler {

    private final int startCol;
    private final int endCol;

    private DataFormatter formatter;
    private FormulaEvaluator evaluator;
    private final Map<CellStyle, CellStyle> styleCache = new HashMap<>();

    public RowColMergeStrategy(int startCol, int endCol) {
        if (startCol <= endCol) {
            this.startCol = startCol;
            this.endCol = endCol;
        } else {
            this.startCol = endCol;
            this.endCol = startCol;
        }
    }

    @Override
    public void afterCellDispose(CellWriteHandlerContext context) {
        if (Boolean.TRUE.equals(context.getHead())) {
            return;
        }
        Cell cell = context.getCell();
        if (cell == null) {
            return;
        }
        Sheet sheet = context.getWriteSheetHolder().getSheet();
        if (sheet == null) {
            return;
        }

        if (formatter == null) {
            formatter = new DataFormatter(Locale.getDefault());
        }
        if (evaluator == null) {
            evaluator = sheet.getWorkbook().getCreationHelper().createFormulaEvaluator();
        }

        int rowIndex = cell.getRowIndex();
        int colIndex = cell.getColumnIndex();

        // 只限制列范围（适用于所有行）
        if (colIndex < startCol || colIndex > endCol) {
            return;
        }

        String currentVal = getCellText(cell);
        if (isBlank(currentVal)) {
            return;
        }

        // 横向合并（与左侧单元格相同）
        if (colIndex > startCol) {
            Cell leftCell = getCell(sheet, rowIndex, colIndex - 1);
            if (leftCell != null && currentVal.equals(getCellText(leftCell))) {
                mergeRegion(sheet, rowIndex, rowIndex, colIndex - 1, colIndex);
            }
        }

        // 纵向合并（与上一行同列单元格相同） —— 现在以第0行为边界（即适用于“所有行”）
        if (rowIndex > 0) {
            Cell aboveCell = getCell(sheet, rowIndex - 1, colIndex);
            if (aboveCell != null && currentVal.equals(getCellText(aboveCell))) {
                mergeRegion(sheet, rowIndex - 1, rowIndex, colIndex, colIndex);
            }
        }
    }

    /**
     * 合并单元格区域（如果已存在则扩展）
     */
    private void mergeRegion(Sheet sheet, int firstRow, int lastRow, int firstCol, int lastCol) {
        // 检查是否已有合并区域包含当前范围
        CellRangeAddress exist = findMergedRegion(sheet, firstRow, lastRow, firstCol, lastCol);
        if (exist != null) {
            // 已存在则先移除
            removeRegion(sheet, exist);
            firstRow = Math.min(firstRow, exist.getFirstRow());
            lastRow = Math.max(lastRow, exist.getLastRow());
            firstCol = Math.min(firstCol, exist.getFirstColumn());
            lastCol = Math.max(lastCol, exist.getLastColumn());
        }
        CellRangeAddress newRegion = new CellRangeAddress(firstRow, lastRow, firstCol, lastCol);
        sheet.addMergedRegion(newRegion);
        setRegionCenterStyle(sheet, newRegion);
    }

    /**
     * 查找已存在的合并区域（任意重叠则返回该区域）
     */
    private CellRangeAddress findMergedRegion(Sheet sheet, int firstRow, int lastRow, int firstCol, int lastCol) {
        for (int i = 0; i < sheet.getNumMergedRegions(); i++) {
            CellRangeAddress r = sheet.getMergedRegion(i);
            if (r.getFirstRow() <= lastRow && r.getLastRow() >= firstRow &&
                    r.getFirstColumn() <= lastCol && r.getLastColumn() >= firstCol) {
                return r;
            }
        }
        return null;
    }

    /**
     * 删除指定合并区域（精确 match）
     */
    private void removeRegion(Sheet sheet, CellRangeAddress target) {
        for (int i = sheet.getNumMergedRegions() - 1; i >= 0; i--) {
            CellRangeAddress r = sheet.getMergedRegion(i);
            if (r.equals(target)) {
                sheet.removeMergedRegion(i);
                return;
            }
        }
    }

    /**
     * 设置区域样式为居中（保留原样式并缓存）
     */
    private void setRegionCenterStyle(Sheet sheet, CellRangeAddress region) {
        Workbook wb = sheet.getWorkbook();
        for (int r = region.getFirstRow(); r <= region.getLastRow(); r++) {
            Row row = sheet.getRow(r);
            if (row == null) row = sheet.createRow(r);
            for (int c = region.getFirstColumn(); c <= region.getLastColumn(); c++) {
                Cell cell = row.getCell(c);
                if (cell == null) cell = row.createCell(c);
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
     * 获取单元格文本（支持公式显示值）
     */
    private String getCellText(Cell cell) {
        if (cell == null) {
            return "";
        }
        try {
            return trimToEmpty(formatter.formatCellValue(cell, evaluator));
        } catch (Exception e) {
            switch (cell.getCellType()) {
                case STRING:
                    return trimToEmpty(cell.getStringCellValue());
                case NUMERIC:
                    if (DateUtil.isCellDateFormatted(cell)) {
                        return trimToEmpty(formatter.formatCellValue(cell));
                    }
                    return trimToEmpty(String.valueOf(cell.getNumericCellValue()));
                case BOOLEAN:
                    return String.valueOf(cell.getBooleanCellValue());
                default:
                    return "";
            }
        }
    }

    private Cell getCell(Sheet sheet, int row, int col) {
        Row r = sheet.getRow(row);
        return r == null ? null : r.getCell(col);
    }

    private static boolean isBlank(String s) {
        return s == null || s.trim().isEmpty();
    }

    private static String trimToEmpty(String s) {
        return s == null ? "" : s.trim();
    }
}
