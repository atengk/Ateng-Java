package io.github.atengk.style;

import cn.afterturn.easypoi.excel.entity.params.ExcelExportEntity;
import cn.afterturn.easypoi.excel.entity.params.ExcelForEachParams;
import cn.afterturn.easypoi.excel.export.styler.AbstractExcelExportStyler;
import org.apache.poi.ss.usermodel.*;

/**
 * 通用审美 Excel 样式
 *
 * 特点：
 * - 字体：微软雅黑（更现代商务）
 * - 表头：深蓝背景、白字、加粗、居中
 * - 数据：
 *    文本左对齐、数字右对齐、日期居中
 *    默认白底，可开启淡灰背景增强视觉分区
 * - 边框：统一细线，打印 & 视觉都干净
 * - 易用于企业管理后台、金融报表、BI 输出等
 *
 * @author ChatGPT
 * @since 2026-01-23
 */
public class ElegantBusinessExcelStyle extends AbstractExcelExportStyler {

    public ElegantBusinessExcelStyle(Workbook workbook) {
        super.createStyles(workbook);
    }

    /**
     * 表头样式
     */
    @Override
    public CellStyle getTitleStyle(short colorIndex) {
        CellStyle style = workbook.createCellStyle();

        Font font = workbook.createFont();
        font.setFontName("微软雅黑");
        font.setFontHeightInPoints((short) 11);
        font.setBold(true);
        font.setColor(IndexedColors.WHITE.getIndex()); // 表头白字
        style.setFont(font);

        style.setAlignment(HorizontalAlignment.CENTER);
        style.setVerticalAlignment(VerticalAlignment.CENTER);

        // 深蓝背景（偏金融、偏商用）
        style.setFillForegroundColor(IndexedColors.DARK_BLUE.getIndex());
        style.setFillPattern(FillPatternType.SOLID_FOREGROUND);

        setBorderThin(style);
        return style;
    }

    /**
     * 多级表头复用同一表头样式
     */
    @Override
    public CellStyle getHeaderStyle(short colorIndex) {
        return getTitleStyle(colorIndex);
    }

    /**
     * 文本字段（左对齐，默认白背景）
     */
    @Override
    public CellStyle stringNoneStyle(Workbook workbook, boolean isWrap) {
        CellStyle style = workbook.createCellStyle();

        Font font = workbook.createFont();
        font.setFontName("微软雅黑");
        font.setFontHeightInPoints((short) 10);
        font.setColor(IndexedColors.BLACK.getIndex());
        style.setFont(font);

        style.setAlignment(HorizontalAlignment.LEFT);
        style.setVerticalAlignment(VerticalAlignment.CENTER);
        style.setWrapText(isWrap);

        style.setFillForegroundColor(IndexedColors.WHITE.getIndex());
        style.setFillPattern(FillPatternType.SOLID_FOREGROUND);

        setBorderThin(style);
        return style;
    }

    /**
     * 数字字段（右对齐，白背景）
     */
    @Override
    public CellStyle stringSeptailStyle(Workbook workbook, boolean isWrap) {
        CellStyle style = workbook.createCellStyle();

        Font font = workbook.createFont();
        font.setFontName("微软雅黑");
        font.setFontHeightInPoints((short) 10);
        font.setColor(IndexedColors.BLACK.getIndex());
        style.setFont(font);

        style.setAlignment(HorizontalAlignment.RIGHT);
        style.setVerticalAlignment(VerticalAlignment.CENTER);
        style.setWrapText(isWrap);

        style.setFillForegroundColor(IndexedColors.WHITE.getIndex());
        style.setFillPattern(FillPatternType.SOLID_FOREGROUND);

        setBorderThin(style);
        return style;
    }

    /**
     * 日期等字段居中（如需）
     */
    public CellStyle dateCenterStyle(Workbook workbook, boolean isWrap) {
        CellStyle style = workbook.createCellStyle();

        Font font = workbook.createFont();
        font.setFontName("微软雅黑");
        font.setFontHeightInPoints((short) 10);
        font.setColor(IndexedColors.BLACK.getIndex());
        style.setFont(font);

        style.setAlignment(HorizontalAlignment.CENTER);
        style.setVerticalAlignment(VerticalAlignment.CENTER);
        style.setWrapText(isWrap);

        setBorderThin(style);
        return style;
    }

    /**
     * 模板 foreach 统一白底左对齐
     */
    @Override
    public CellStyle getTemplateStyles(boolean isSingle, ExcelForEachParams excelForEachParams) {
        return this.stringNoneStyle;
    }

    /**
     * 不使用默认斑马纹，统一样式
     */
    @Override
    public CellStyle getStyles(boolean noneStyler, ExcelExportEntity entity) {
        return this.stringNoneStyle;
    }

    /**
     * 根据字段类型动态处理
     */
    @Override
    public CellStyle getStyles(Cell cell,
                               int dataRow,
                               ExcelExportEntity entity,
                               Object obj,
                               Object data) {

        if (data instanceof Number) {
            return this.stringSeptailStyle;
        }

        // 可拓展日期识别逻辑
        if (data instanceof java.util.Date) {
            return dateCenterStyle(workbook, false);
        }

        return this.stringNoneStyle;
    }

    /**
     * 边框统一细线
     */
    private void setBorderThin(CellStyle style) {
        style.setBorderTop(BorderStyle.THIN);
        style.setBorderBottom(BorderStyle.THIN);
        style.setBorderLeft(BorderStyle.THIN);
        style.setBorderRight(BorderStyle.THIN);
    }
}
