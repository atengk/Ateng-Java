package io.github.atengk.style;

import cn.afterturn.easypoi.excel.entity.params.ExcelExportEntity;
import cn.afterturn.easypoi.excel.entity.params.ExcelForEachParams;
import cn.afterturn.easypoi.excel.export.styler.AbstractExcelExportStyler;
import org.apache.poi.ss.usermodel.*;

/**
 * 全部内容居中对齐的 Excel 样式处理器
 * 特点：
 * 1. 表头居中 + 加粗 + 背景色
 * 2. 所有数据列（文本、数字、日期等）全部水平、垂直居中
 * 3. 不使用 EasyPOI 默认的奇偶行斑马纹逻辑
 * 4. 常用于报表型、展示型 Excel，视觉最规整
 *
 * @author 孔余
 * @since 2026-01-22
 */
public class CenterAlignExcelStyle extends AbstractExcelExportStyler {

    /**
     * 构造器中必须调用 createStyles
     */
    public CenterAlignExcelStyle(Workbook workbook) {
        super.createStyles(workbook);
    }

    /**
     * 表头样式（居中、加粗、背景色）
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
     * 多级表头样式，直接复用表头样式
     */
    @Override
    public CellStyle getHeaderStyle(short colorIndex) {
        return getTitleStyle(colorIndex);
    }

    /**
     * 普通字符串样式（全部居中）
     */
    @Override
    public CellStyle stringNoneStyle(Workbook workbook, boolean isWrap) {
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
     * 数字/特殊字段样式（同样全部居中）
     */
    @Override
    public CellStyle stringSeptailStyle(Workbook workbook, boolean isWrap) {
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
     * 模板 foreach 场景，复用普通居中样式
     */
    @Override
    public CellStyle getTemplateStyles(boolean isSingle, ExcelForEachParams excelForEachParams) {
        return this.stringNoneStyle;
    }

    /**
     * 覆盖 EasyPOI 默认的奇偶行样式逻辑，全部统一为居中样式
     */
    @Override
    public CellStyle getStyles(boolean noneStyler, ExcelExportEntity entity) {
        return this.stringNoneStyle;
    }

    /**
     * 根据单元格内容返回最终样式，强制所有内容居中
     */
    @Override
    public CellStyle getStyles(Cell cell,
                               int dataRow,
                               ExcelExportEntity entity,
                               Object obj,
                               Object data) {
        return this.stringNoneStyle;
    }

    /**
     * 统一设置细边框
     */
    private void setBorderThin(CellStyle style) {
        style.setBorderTop(BorderStyle.THIN);
        style.setBorderBottom(BorderStyle.THIN);
        style.setBorderLeft(BorderStyle.THIN);
        style.setBorderRight(BorderStyle.THIN);
    }
}
