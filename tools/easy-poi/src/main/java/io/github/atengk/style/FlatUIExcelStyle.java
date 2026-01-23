package io.github.atengk.style;

import cn.afterturn.easypoi.excel.entity.params.ExcelExportEntity;
import cn.afterturn.easypoi.excel.entity.params.ExcelForEachParams;
import cn.afterturn.easypoi.excel.export.styler.AbstractExcelExportStyler;
import org.apache.poi.ss.usermodel.*;

public class FlatUIExcelStyle extends AbstractExcelExportStyler {

    public FlatUIExcelStyle(Workbook workbook) {
        super.createStyles(workbook);
    }

    @Override
    public CellStyle getTitleStyle(short colorIndex) {
        CellStyle style = workbook.createCellStyle();
        Font font = workbook.createFont();

        font.setFontName("微软雅黑");
        font.setBold(true);
        font.setColor(IndexedColors.DARK_BLUE.getIndex());
        font.setFontHeightInPoints((short) 10);
        style.setFont(font);

        style.setAlignment(HorizontalAlignment.CENTER);
        style.setVerticalAlignment(VerticalAlignment.CENTER);

        // 扁平淡蓝背景（不刺眼）
        style.setFillForegroundColor(IndexedColors.PALE_BLUE.getIndex());
        style.setFillPattern(FillPatternType.SOLID_FOREGROUND);

        setBorderHair(style);
        return style;
    }

    @Override
    public CellStyle getHeaderStyle(short colorIndex) {
        return getTitleStyle(colorIndex);
    }

    @Override
    public CellStyle stringNoneStyle(Workbook workbook, boolean isWrap) {
        CellStyle style = workbook.createCellStyle();
        Font font = workbook.createFont();

        font.setFontName("微软雅黑");
        font.setFontHeightInPoints((short) 9);
        style.setFont(font);

        style.setAlignment(HorizontalAlignment.LEFT);
        style.setVerticalAlignment(VerticalAlignment.CENTER);
        style.setWrapText(isWrap);

        setBorderHair(style);
        return style;
    }

    @Override
    public CellStyle stringSeptailStyle(Workbook workbook, boolean isWrap) {
        CellStyle style = workbook.createCellStyle();
        Font font = workbook.createFont();

        font.setFontName("微软雅黑");
        font.setFontHeightInPoints((short) 9);
        style.setFont(font);

        style.setAlignment(HorizontalAlignment.RIGHT);
        style.setVerticalAlignment(VerticalAlignment.CENTER);
        style.setWrapText(isWrap);

        setBorderHair(style);
        return style;
    }

    @Override
    public CellStyle getTemplateStyles(boolean isSingle, ExcelForEachParams params) {
        return this.stringNoneStyle;
    }

    @Override
    public CellStyle getStyles(Cell cell, int dataRow, ExcelExportEntity entity, Object obj, Object data) {
        if (data instanceof Number) {
            return this.stringSeptailStyle;
        }
        return this.stringNoneStyle;
    }

    /**
     * UI 风格 → 超细边框（更接近 Web UI）
     */
    private void setBorderHair(CellStyle style) {
        style.setBorderTop(BorderStyle.HAIR);
        style.setBorderBottom(BorderStyle.HAIR);
        style.setBorderLeft(BorderStyle.HAIR);
        style.setBorderRight(BorderStyle.HAIR);
    }
}
