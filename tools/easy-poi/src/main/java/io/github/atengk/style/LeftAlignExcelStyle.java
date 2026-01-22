package io.github.atengk.style;

import cn.afterturn.easypoi.excel.entity.params.ExcelExportEntity;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.Workbook;

/**
 * 所有列统一左对齐，适合文字内容居多的场景
 */
public class LeftAlignExcelStyle extends MyExcelStyle {

    public LeftAlignExcelStyle(Workbook workbook) {
        super(workbook);
    }

    @Override
    public CellStyle getStyles(Cell cell, int dataRow, ExcelExportEntity entity, Object obj, Object data) {
        return this.stringNoneStyle;
    }
}