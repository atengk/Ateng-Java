package io.github.atengk.handler;

import org.apache.fesod.sheet.write.handler.SheetWriteHandler;
import org.apache.fesod.sheet.write.handler.context.SheetWriteHandlerContext;
import org.apache.fesod.sheet.write.metadata.holder.WriteSheetHolder;
import org.apache.fesod.sheet.write.metadata.holder.WriteWorkbookHolder;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;

import java.util.Collections;
import java.util.Map;

/**
 * Sheet 级别行高、列宽统一处理器。
 * <p>
 * 功能：
 * <ul>
 *     <li>自动识别表头行数（支持多级表头）</li>
 *     <li>设置表头行高</li>
 *     <li>设置内容行高</li>
 *     <li>设置指定列的列宽</li>
 * </ul>
 * <p>
 * 设计原则：
 * <ul>
 *     <li>列宽在 Sheet 创建时设置</li>
 *     <li>行高在 Sheet 写入完成后设置</li>
 *     <li>列宽单位使用“字符宽度”，内部自动 *256</li>
 *     <li>使用者不需要关心表头是几级</li>
 * </ul>
 *
 * @author 孔余
 * @since 2026-01-26
 */
public class RowColumnDimensionHandler implements SheetWriteHandler {

    /**
     * 表头行高（单位：磅）
     */
    private final short headRowHeight;

    /**
     * 内容行高（单位：磅）
     */
    private final short contentRowHeight;

    /**
     * 指定列宽配置
     * key：列索引（从 0 开始）
     * value：列宽（字符宽度，不需要乘 256）
     */
    private final Map<Integer, Integer> columnWidthMap;

    public RowColumnDimensionHandler(short headRowHeight,
                                     short contentRowHeight,
                                     Map<Integer, Integer> columnWidthMap) {
        this.headRowHeight = headRowHeight;
        this.contentRowHeight = contentRowHeight;
        this.columnWidthMap = columnWidthMap == null ? Collections.emptyMap() : columnWidthMap;
    }

    /**
     * Sheet 创建完成后回调。
     * <p>
     * 此时 Row 还未创建，只能做 Sheet 结构类操作：
     * <ul>
     *     <li>列宽</li>
     *     <li>冻结窗格</li>
     *     <li>打印设置</li>
     * </ul>
     */
    @Override
    public void afterSheetCreate(WriteWorkbookHolder writeWorkbookHolder,
                                 WriteSheetHolder writeSheetHolder) {

        Sheet sheet = writeSheetHolder.getSheet();
        setColumnWidth(sheet);
    }

    /**
     * Sheet 写入完成后的回调。
     * <p>
     * 此时：
     * <ul>
     *     <li>所有 Row 已存在</li>
     *     <li>可以安全设置行高</li>
     * </ul>
     */
    @Override
    public void afterSheetDispose(SheetWriteHandlerContext context) {
        Sheet sheet = context.getWriteSheetHolder().getSheet();
        setRowHeight(sheet, context.getWriteSheetHolder());
    }

    /**
     * 设置列宽（字符宽度 → Excel 单位 *256）
     */
    private void setColumnWidth(Sheet sheet) {
        if (columnWidthMap.isEmpty()) {
            return;
        }

        for (Map.Entry<Integer, Integer> entry : columnWidthMap.entrySet()) {
            Integer columnIndex = entry.getKey();
            Integer columnWidth = entry.getValue();

            if (columnIndex == null || columnWidth == null || columnWidth <= 0) {
                continue;
            }

            sheet.setColumnWidth(columnIndex, columnWidth * 256);
        }
    }

    /**
     * 设置行高，自动区分表头行和内容行。
     */
    private void setRowHeight(Sheet sheet, WriteSheetHolder writeSheetHolder) {

        int headRowCount = writeSheetHolder
                .getExcelWriteHeadProperty()
                .getHeadRowNumber();

        int lastRowNum = sheet.getLastRowNum();

        for (int rowIndex = 0; rowIndex <= lastRowNum; rowIndex++) {
            Row row = sheet.getRow(rowIndex);
            if (row == null) {
                continue;
            }

            if (rowIndex < headRowCount) {
                row.setHeightInPoints(headRowHeight);
            } else {
                row.setHeightInPoints(contentRowHeight);
            }
        }
    }
}
