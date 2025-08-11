package local.ateng.java.excel.handler;

import cn.idev.excel.metadata.Head;
import cn.idev.excel.write.merge.AbstractMergeStrategy;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.ss.util.CellRangeAddress;

import java.util.List;

/**
 * AutoMergeStrategy
 *
 * <p>按列自动合并连续相同内容的单元格。合并在写入尾行时一次性执行，避免重复合并导致 POI 异常。</p>
 *
 * 使用说明：
 * - mergeCols: 要合并的列索引数组（如 new int[]{0, 2}）
 * - headerRows: 表头占用的行数（例如多级表头时为 headerList.get(0).size()）
 * - dataList: 实际要写入的数据（不包含表头行）
 *
 * 注册示例：
 * EasyExcel.write(outputStream)
 *     .head(headerList)
 *     .registerWriteHandler(new AutoMergeStrategy(new int[]{0}, headerRows, dataList))
 *     .sheet("Sheet1")
 *     .doWrite(dataList);
 */
public class AutoMergeStrategy extends AbstractMergeStrategy {

    private final int[] mergeCols;
    private final int headerRows;
    private final List<List<String>> dataList;

    /**
     * @param mergeCols 合并列索引数组
     * @param headerRows 表头行数（表头占用的行数）
     * @param dataList 写入的数据（不包含表头）
     */
    public AutoMergeStrategy(int[] mergeCols, int headerRows, List<List<String>> dataList) {
        if (mergeCols == null || mergeCols.length == 0) {
            throw new IllegalArgumentException("mergeCols 不能为空");
        }
        if (headerRows < 0) {
            throw new IllegalArgumentException("headerRows 必须 >= 0");
        }
        if (dataList == null) {
            throw new IllegalArgumentException("dataList 不能为空");
        }
        this.mergeCols = mergeCols;
        this.headerRows = headerRows;
        this.dataList = dataList;
    }

    /**
     * 对单列进行连续值合并
     *
     * @param sheet 列所在的工作表
     * @param colIndex 列索引
     */
    private void mergeColumn(Sheet sheet, int colIndex) {
        if (dataList.isEmpty()) {
            return;
        }

        // 数据在 sheet 中起始行（包含 headerRows）
        int startRow = headerRows;
        String prev = null;
        int groupStart = startRow;

        for (int i = 0; i < dataList.size(); i++) {
            int sheetRowIndex = headerRows + i;
            List<String> row = dataList.get(i);
            String current = null;
            if (colIndex < row.size()) {
                current = row.get(colIndex);
            } else {
                current = "";
            }
            if (prev == null) {
                prev = current;
                groupStart = sheetRowIndex;
                continue;
            }

            boolean atLastRow = (i == dataList.size() - 1);

            if (atLastRow) {
                // 处理最后一行：如果与前一行相同，则合并到当前行；否则合并到上一行
                if (current != null && current.equals(prev)) {
                    // groupStart..sheetRowIndex
                    applyMergeIfNeeded(sheet, groupStart, sheetRowIndex, colIndex);
                } else {
                    // groupStart..(sheetRowIndex - 1)
                    applyMergeIfNeeded(sheet, groupStart, sheetRowIndex - 1, colIndex);
                    // 不需要合并当前单元格与下一行（没有下一行）
                }
            } else {
                // 非最后一行：如果值变化则合并前一段
                if (!stringsEqual(prev, current)) {
                    int endRow = sheetRowIndex - 1;
                    applyMergeIfNeeded(sheet, groupStart, endRow, colIndex);
                    // 重置分组
                    groupStart = sheetRowIndex;
                    prev = current;
                } else {
                    // 值相同，继续
                    prev = current;
                }
            }
        }
    }

    /**
     * 判断两个字符串是否相等（对 null 做空字符串处理）
     */
    private boolean stringsEqual(String a, String b) {
        if (a == null) {
            a = "";
        }
        if (b == null) {
            b = "";
        }
        return a.equals(b);
    }

    /**
     * 在合并前检查是否已存在相同区域，若不存在则执行合并并给合并单元格设置样式
     */
    private void applyMergeIfNeeded(Sheet sheet, int firstRow, int lastRow, int colIndex) {
        if (lastRow <= firstRow) {
            return;
        }
        if (isRegionMerged(sheet, firstRow, lastRow, colIndex, colIndex)) {
            return;
        }

        CellRangeAddress region = new CellRangeAddress(firstRow, lastRow, colIndex, colIndex);
        sheet.addMergedRegion(region);

        // 设置合并后首单元格样式（只创建一次基础样式）
        Workbook workbook = sheet.getWorkbook();
        CellStyle style = workbook.createCellStyle();
        style.setVerticalAlignment(VerticalAlignment.CENTER);
        style.setAlignment(HorizontalAlignment.CENTER);

        Row row = sheet.getRow(firstRow);
        if (row == null) {
            row = sheet.createRow(firstRow);
        }
        Cell cell = row.getCell(colIndex, Row.MissingCellPolicy.CREATE_NULL_AS_BLANK);
        cell.setCellStyle(style);
    }

    /**
     * 检查给定区域是否已经被合并（完全匹配）
     */
    private boolean isRegionMerged(Sheet sheet, int firstRow, int lastRow, int firstCol, int lastCol) {
        int n = sheet.getNumMergedRegions();
        for (int i = 0; i < n; i++) {
            CellRangeAddress r = sheet.getMergedRegion(i);
            if (r.getFirstRow() == firstRow && r.getLastRow() == lastRow
                    && r.getFirstColumn() == firstCol && r.getLastColumn() == lastCol) {
                return true;
            }
        }
        return false;
    }

    /**
     * merge
     *
     * @param sheet
     * @param cell
     * @param head
     * @param relativeRowIndex
     */
    @Override
    protected void merge(Sheet sheet, Cell cell, Head head, Integer relativeRowIndex) {
        if (cell == null) {
            return;
        }

        // 计算 Excel 中最后一行的索引（包含表头），lastDataRowIndex 代表数据最后一行在 sheet 中的真实行索引
        int lastDataRowIndex = headerRows + dataList.size() - 1;

        // 仅在写到数据最后一行并且当前列为 mergeCols 的最后一个元素时触发一次合并操作
        // 这样可以保证合并操作只执行一次，避免重复合并造成的 overlaps 异常
        int currentRowIndex = cell.getRowIndex();
        int currentColIndex = cell.getColumnIndex();
        int lastMergeCol = mergeCols[mergeCols.length - 1];

        if (currentRowIndex != lastDataRowIndex) {
            return;
        }
        if (currentColIndex != lastMergeCol) {
            return;
        }

        // 执行每个需要合并的列的合并逻辑
        for (int col : mergeCols) {
            mergeColumn(sheet, col);
        }
    }
}
