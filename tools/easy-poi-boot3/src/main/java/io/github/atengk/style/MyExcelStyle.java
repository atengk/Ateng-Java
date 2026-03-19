package io.github.atengk.style;

import cn.afterturn.easypoi.excel.entity.params.ExcelExportEntity;
import cn.afterturn.easypoi.excel.entity.params.ExcelForEachParams;
import cn.afterturn.easypoi.excel.export.styler.AbstractExcelExportStyler;
import org.apache.poi.ss.usermodel.*;

/**
 * 自定义 Excel 样式处理器
 * 支持：
 * - 表头样式（加粗、居中、背景色）
 * - 普通单元格样式（左/中/右对齐）
 * - 数字/特殊字段样式
 * - 斑马纹行（可扩展）
 *
 * @author 孔余
 * @since 2026-01-22
 */
public class MyExcelStyle extends AbstractExcelExportStyler {

    public MyExcelStyle(Workbook workbook) {
        super.createStyles(workbook);
    }

    /**
     * 表头样式（默认居中、加粗、灰色背景）
     */
    @Override
    public CellStyle getTitleStyle(short colorIndex) {
        CellStyle style = workbook.createCellStyle();
        Font font = workbook.createFont();
        font.setFontName("宋体");
        font.setFontHeightInPoints((short) 11);
        font.setBold(true);
        style.setFont(font);

        style.setAlignment(HorizontalAlignment.CENTER);
        style.setVerticalAlignment(VerticalAlignment.CENTER);

        style.setFillForegroundColor(colorIndex);
        style.setFillPattern(FillPatternType.SOLID_FOREGROUND);

        setBorderThin(style);
        return style;
    }

    /**
     * 表头多级样式复用 getTitleStyle
     */
    @Override
    public CellStyle getHeaderStyle(short colorIndex) {
        return getTitleStyle(colorIndex);
    }

    /**
     * 普通单元格（左对齐）
     */
    @Override
    public CellStyle stringNoneStyle(Workbook workbook, boolean isWrap) {
        CellStyle style = workbook.createCellStyle();
        Font font = workbook.createFont();
        font.setFontName("宋体");
        font.setFontHeightInPoints((short) 11);
        style.setFont(font);

        style.setAlignment(HorizontalAlignment.LEFT);
        style.setVerticalAlignment(VerticalAlignment.CENTER);
        style.setWrapText(isWrap);

        setBorderThin(style);
        return style;
    }

    /**
     * 数字/特殊字段样式（右对齐）
     */
    @Override
    public CellStyle stringSeptailStyle(Workbook workbook, boolean isWrap) {
        CellStyle style = workbook.createCellStyle();
        Font font = workbook.createFont();
        font.setFontName("宋体");
        font.setFontHeightInPoints((short) 11);
        style.setFont(font);

        style.setAlignment(HorizontalAlignment.RIGHT);
        style.setVerticalAlignment(VerticalAlignment.CENTER);
        style.setWrapText(isWrap);

        setBorderThin(style);
        return style;
    }

    /**
     * 居中样式（可直接用于数字或文字列）
     */
    public CellStyle stringCenterStyle(Workbook workbook, boolean isWrap) {
        CellStyle style = workbook.createCellStyle();
        Font font = workbook.createFont();
        font.setFontName("宋体");
        font.setFontHeightInPoints((short) 11);
        style.setFont(font);

        style.setAlignment(HorizontalAlignment.CENTER);
        style.setVerticalAlignment(VerticalAlignment.CENTER);
        style.setWrapText(isWrap);

        setBorderThin(style);
        return style;
    }

    /**
     * 设置单元格细边框
     */
    private void setBorderThin(CellStyle style) {
        style.setBorderTop(BorderStyle.THIN);
        style.setBorderBottom(BorderStyle.THIN);
        style.setBorderLeft(BorderStyle.THIN);
        style.setBorderRight(BorderStyle.THIN);
    }

    /**
     * 模板 foreach 场景，直接复用普通样式
     */
    @Override
    public CellStyle getTemplateStyles(boolean isSingle, ExcelForEachParams excelForEachParams) {
        return this.stringNoneStyle;
    }

    /**
     * 覆盖默认奇偶行逻辑，全部使用普通样式
     */
    @Override
    public CellStyle getStyles(boolean noneStyler, ExcelExportEntity entity) {
        return this.stringNoneStyle;
    }

    /**
     * 根据单元格内容返回最终样式，默认全部左对齐
     */
    @Override
    public CellStyle getStyles(Cell cell, int dataRow, ExcelExportEntity entity, Object obj, Object data) {
        return this.stringNoneStyle;
    }
}
