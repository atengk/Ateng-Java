package io.github.atengk.style;

import cn.afterturn.easypoi.excel.entity.params.ExcelExportEntity;
import cn.afterturn.easypoi.excel.entity.params.ExcelForEachParams;
import cn.afterturn.easypoi.excel.export.styler.AbstractExcelExportStyler;
import org.apache.poi.ss.usermodel.*;

public class BusinessGrayExcelStyle extends AbstractExcelExportStyler {

    public BusinessGrayExcelStyle(Workbook workbook) {
        super.createStyles(workbook);
    }

    @Override
    public CellStyle getTitleStyle(short colorIndex) {
        CellStyle style = workbook.createCellStyle();
        Font font = workbook.createFont();

        font.setFontName("微软雅黑");
        font.setBold(true);
        font.setColor(IndexedColors.WHITE.getIndex());
        font.setFontHeightInPoints((short) 11);
        style.setFont(font);

        style.setAlignment(HorizontalAlignment.CENTER);
        style.setVerticalAlignment(VerticalAlignment.CENTER);

        // 深灰背景
        style.setFillForegroundColor(IndexedColors.GREY_80_PERCENT.getIndex());
        style.setFillPattern(FillPatternType.SOLID_FOREGROUND);

        setBorderThin(style);
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
        font.setFontHeightInPoints((short) 10);
        style.setFont(font);

        style.setAlignment(HorizontalAlignment.LEFT);
        style.setVerticalAlignment(VerticalAlignment.CENTER);
        style.setWrapText(isWrap);

        setBorderThin(style);
        return style;
    }

    @Override
    public CellStyle stringSeptailStyle(Workbook workbook, boolean isWrap) {
        CellStyle style = workbook.createCellStyle();
        Font font = workbook.createFont();

        font.setFontName("微软雅黑");
        font.setFontHeightInPoints((short) 10);
        style.setFont(font);

        style.setAlignment(HorizontalAlignment.RIGHT);
        style.setVerticalAlignment(VerticalAlignment.CENTER);
        style.setWrapText(isWrap);

        setBorderThin(style);
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

    private void setBorderThin(CellStyle style) {
        style.setBorderTop(BorderStyle.THIN);
        style.setBorderBottom(BorderStyle.THIN);
        style.setBorderLeft(BorderStyle.THIN);
        style.setBorderRight(BorderStyle.THIN);
    }
}
