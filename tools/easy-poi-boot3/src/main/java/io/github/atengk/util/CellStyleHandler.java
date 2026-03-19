package io.github.atengk.util;

import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.Workbook;

/**
 * 单元格样式处理器函数接口
 *
 * @author 孔余
 * @since 2026-01-22
 */
@FunctionalInterface
public interface CellStyleHandler {

    /**
     * 对单个单元格执行样式处理逻辑。
     *
     * @param workbook 当前工作簿对象，用于创建和复用 CellStyle
     * @param cell     当前需要处理的单元格
     */
    void handle(Workbook workbook, Cell cell);
}
