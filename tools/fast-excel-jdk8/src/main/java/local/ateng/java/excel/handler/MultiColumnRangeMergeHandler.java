package local.ateng.java.excel.handler;

import cn.idev.excel.metadata.Head;
import cn.idev.excel.write.handler.CellWriteHandler;
import cn.idev.excel.write.metadata.holder.WriteSheetHolder;
import cn.idev.excel.write.metadata.holder.WriteTableHolder;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.ss.util.CellRangeAddress;

import java.util.*;

public class MultiColumnRangeMergeHandler implements CellWriteHandler {


    private final List<int[]> columnRanges; // 每个 int[]{startCol,endCol}
    private final Map<String, CellRangeAddress> activeRegion = new HashMap<>();
    private DataFormatter formatter;
    private FormulaEvaluator evaluator;

    public MultiColumnRangeMergeHandler(List<int[]> columnRanges) {
        this.columnRanges = columnRanges == null ? Collections.emptyList() : columnRanges;
    }

    @Override
    public void afterCellDispose(WriteSheetHolder writeSheetHolder,
                                 WriteTableHolder writeTableHolder,
                                 List cellDataList,
                                 Cell cell,
                                 Head head,
                                 Integer relativeRowIndex,
                                 Boolean isHead) {
        if (Boolean.TRUE.equals(isHead) || cell == null) {
            return;
        }
        Sheet sheet = writeSheetHolder.getSheet();
        if (formatter == null) {
            formatter = new DataFormatter();
        }
        if (evaluator == null) {
            evaluator = sheet.getWorkbook().getCreationHelper().createFormulaEvaluator();
        }

        int rowIndex = cell.getRowIndex();

        for (int[] range : columnRanges) {
            int startCol = range[0];
            int endCol = range[1];
            if (cell.getColumnIndex() < startCol || cell.getColumnIndex() > endCol) {
                continue;
            }
            if (rowIndex == 0) {
                continue;
            }

            boolean allSame = true;
            for (int col = startCol; col <= endCol; col++) {
                String cur = getCellText(sheet, rowIndex, col);
                String prev = getCellText(sheet, rowIndex - 1, col);
                if (!Objects.equals(cur, prev)) {
                    allSame = false;
                    break;
                }
            }

            String key = startCol + "-" + endCol;
            if (allSame) {
                CellRangeAddress region = activeRegion.get(key);
                if (region == null) {
                    region = new CellRangeAddress(rowIndex - 1, rowIndex, startCol, endCol);
                    sheet.addMergedRegion(region);
                    activeRegion.put(key, region);
                } else {
                    removeRegionIfPresent(sheet, region);
                    region = new CellRangeAddress(region.getFirstRow(), rowIndex, startCol, endCol);
                    sheet.addMergedRegion(region);
                    activeRegion.put(key, region);
                }
            } else {
                activeRegion.remove(key);
            }
        }
    }

    private String getCellText(Sheet sheet, int row, int col) {
        Row r = sheet.getRow(row);
        if (r == null) return "";
        Cell c = r.getCell(col);
        if (c == null) return "";
        try {
            return formatter.formatCellValue(c, evaluator).trim();
        } catch (Exception e) {
            return "";
        }
    }

    private void removeRegionIfPresent(Sheet sheet, CellRangeAddress target) {
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
}
