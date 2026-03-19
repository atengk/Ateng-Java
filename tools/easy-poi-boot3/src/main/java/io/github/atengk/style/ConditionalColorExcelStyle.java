package io.github.atengk.style;

import cn.afterturn.easypoi.excel.entity.params.ExcelExportEntity;
import cn.afterturn.easypoi.excel.entity.params.ExcelForEachParams;
import cn.afterturn.easypoi.excel.export.styler.AbstractExcelExportStyler;
import org.apache.poi.ss.usermodel.*;

public class ConditionalColorExcelStyle extends AbstractExcelExportStyler {
    // 缓存基础样式，避免重复创建
    private CellStyle stringLeftStyle;
    private CellStyle stringCenterStyle;
    private CellStyle stringRightStyle;

    // 缓存动态样式示例（红、橙）
    private CellStyle failedStyle;
    private CellStyle warnStyle;

    public ConditionalColorExcelStyle(Workbook workbook) {
        super.createStyles(workbook);
        initBaseStyles(workbook);
        initDynamicStyles(workbook);
    }

    /**
     * 初始化普通基础样式
     */
    private void initBaseStyles(Workbook workbook) {
        this.stringLeftStyle = workbook.createCellStyle();
        Font font = workbook.createFont();
        font.setFontName("宋体");
        font.setFontHeightInPoints((short) 11);
        stringLeftStyle.setFont(font);
        stringLeftStyle.setAlignment(HorizontalAlignment.LEFT);
        stringLeftStyle.setVerticalAlignment(VerticalAlignment.CENTER);
        stringLeftStyle.setWrapText(false);
        setBorderThin(stringLeftStyle);

        this.stringCenterStyle = workbook.createCellStyle();
        stringCenterStyle.cloneStyleFrom(stringLeftStyle);
        stringCenterStyle.setAlignment(HorizontalAlignment.CENTER);

        this.stringRightStyle = workbook.createCellStyle();
        stringRightStyle.cloneStyleFrom(stringLeftStyle);
        stringRightStyle.setAlignment(HorizontalAlignment.RIGHT);
    }

    /**
     * 初始化动态样式（支持背景色）
     */
    private void initDynamicStyles(Workbook workbook) {
        // 红底 + 白字（用于失败状态）
        this.failedStyle = workbook.createCellStyle();
        failedStyle.cloneStyleFrom(stringCenterStyle);
        failedStyle.setFillForegroundColor(IndexedColors.RED.getIndex());
        failedStyle.setFillPattern(FillPatternType.SOLID_FOREGROUND);

        Font white = workbook.createFont();
        white.setColor(IndexedColors.WHITE.getIndex());
        white.setBold(true);
        failedStyle.setFont(white);

        // 橙底（用于金额警告）
        this.warnStyle = workbook.createCellStyle();
        warnStyle.cloneStyleFrom(stringRightStyle);
        warnStyle.setFillForegroundColor(IndexedColors.LIGHT_ORANGE.getIndex());
        warnStyle.setFillPattern(FillPatternType.SOLID_FOREGROUND);
    }

    /**
     * 单元格动态样式核心方法
     * data 对应字段真实值，entity 对应列配置信息
     *
     * 👉 重点：你可以在这里手写各种条件
     */
    @Override
    public CellStyle getStyles(Cell cell, int dataRow, ExcelExportEntity entity, Object obj, Object data) {

        // === 示例1：status 字段失败时高亮 ===
        if ("省份".equalsIgnoreCase(entity.getName()) && data instanceof String) {
            String status = (String) data;
            if ("重庆".equalsIgnoreCase(status)) {
                return failedStyle;
            }
        }

        // === 示例2：amount 字段金额超过阈值 ===
        if ("年龄".equalsIgnoreCase(entity.getName()) && data instanceof Number) {
            double value = ((Number) data).doubleValue();
            if (value > 10) {
                return warnStyle;
            }
            return stringRightStyle;
        }

        // === 默认样式 ===
        return stringLeftStyle;
    }

    /**
     * 必须覆盖，否则 EasyPoi 会走父类逻辑导致覆盖
     */
    @Override
    public CellStyle getStyles(boolean noneStyler, ExcelExportEntity entity) {
        return stringLeftStyle;
    }

    @Override
    public CellStyle getTemplateStyles(boolean isSingle, ExcelForEachParams excelForEachParams) {
        return stringLeftStyle;
    }

    @Override
    public CellStyle getHeaderStyle(short headerColor) {
        return null;
    }

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

    private void setBorderThin(CellStyle style) {
        style.setBorderTop(BorderStyle.THIN);
        style.setBorderBottom(BorderStyle.THIN);
        style.setBorderLeft(BorderStyle.THIN);
        style.setBorderRight(BorderStyle.THIN);
    }
}