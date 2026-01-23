package io.github.atengk.style;

import cn.afterturn.easypoi.excel.entity.params.ExcelExportEntity;
import cn.afterturn.easypoi.excel.entity.params.ExcelForEachParams;
import cn.afterturn.easypoi.excel.export.styler.AbstractExcelExportStyler;
import org.apache.poi.ss.usermodel.*;

/**
 * Excel 导出样式策略实现类。
 *
 * <p>样式适配场景：</p>
 * <ul>
 *     <li>注解模式导出（基于 {@link ExcelExportEntity} 元数据）</li>
 *     <li>模板渲染模式（Foreach 模板 {@link ExcelForEachParams}）</li>
 *     <li>普通 Java 数据模式（基于单元格值进行样式映射）</li>
 * </ul>
 *
 * <p>样式策略说明：</p>
 * <ul>
 *     <li>表头单元格：白色背景、加粗黑体、居中、细边框</li>
 *     <li>普通内容单元格：宋体、居中、细边框</li>
 *     <li>无类型区分逻辑，所有内容样式一律居中处理</li>
 * </ul>
 *
 * <p>效果特性：</p>
 * <ul>
 *     <li>提高视觉统一性</li>
 *     <li>适合列表类型导出展示</li>
 *     <li>适用于无复杂类型差异的场景</li>
 * </ul>
 *
 * @author 孔余
 * @since 2026-01-23
 */
public class CustomConciseExcelExportStyler extends AbstractExcelExportStyler {

    /**
     * 表头单元格样式（白色背景 + 加粗 + 居中）
     */
    private final CellStyle headerCenterStyle;

    /**
     * 内容单元格样式（居中）
     */
    private final CellStyle centerStyle;

    /**
     * 构造函数。
     *
     * <p>框架在创建工作簿时注入 Workbook 实例，
     * 并要求调用者在该类构造中进行样式初始化。</p>
     *
     * @param workbook Excel 工作簿实例
     */
    public CustomConciseExcelExportStyler(Workbook workbook) {
        super.createStyles(workbook);
        this.headerCenterStyle = createHeaderStyle();
        this.centerStyle = createCenterStyle();
    }

    /**
     * 创建表头样式。
     *
     * <p>配置内容：</p>
     * <ul>
     *     <li>字体加粗（黑体）</li>
     *     <li>白色背景填充</li>
     *     <li>水平 / 垂直居中</li>
     *     <li>细边框增强视觉结构</li>
     * </ul>
     *
     * @return 表头样式实例
     */
    private CellStyle createHeaderStyle() {
        CellStyle style = workbook.createCellStyle();

        Font font = workbook.createFont();
        font.setFontName("宋体");
        font.setFontHeightInPoints((short) 11);
        font.setBold(true);
        style.setFont(font);

        style.setAlignment(HorizontalAlignment.CENTER);
        style.setVerticalAlignment(VerticalAlignment.CENTER);

        style.setFillForegroundColor(IndexedColors.WHITE.getIndex());
        style.setFillPattern(FillPatternType.SOLID_FOREGROUND);

        setThinBorder(style);
        return style;
    }

    /**
     * 创建内容样式（全局居中）。
     *
     * <p>配置内容：</p>
     * <ul>
     *     <li>宋体字体</li>
     *     <li>水平 / 垂直居中</li>
     *     <li>细边框增强表格结构</li>
     * </ul>
     *
     * @return 内容单元格样式实例
     */
    private CellStyle createCenterStyle() {
        CellStyle style = workbook.createCellStyle();

        Font font = workbook.createFont();
        font.setFontName("宋体");
        font.setFontHeightInPoints((short) 11);
        style.setFont(font);

        style.setAlignment(HorizontalAlignment.CENTER);
        style.setVerticalAlignment(VerticalAlignment.CENTER);

        setThinBorder(style);
        return style;
    }

    /**
     * 表头样式适配（一级表头）。
     *
     * <p>框架会在导出表头时调用此方法。</p>
     *
     * @param color 框架内部传递的表头色值（忽略）
     * @return 表头样式实例
     */
    @Override
    public CellStyle getTitleStyle(short color) {
        return headerCenterStyle;
    }

    /**
     * 表头样式适配（多级表头）。
     *
     * <p>作用与 {@link #getTitleStyle(short)} 基本一致。</p>
     *
     * @param color 框架内部传递颜色（忽略）
     * @return 表头样式实例
     */
    @Override
    public CellStyle getHeaderStyle(short color) {
        return headerCenterStyle;
    }

    /**
     * 样式选择入口（注解模式）。
     *
     * <p>框架仅提供 {@link ExcelExportEntity} 元数据，
     * 无具体 Cell 和数据上下文。</p>
     *
     * @param noneStyler 是否忽略默认样式（忽略）
     * @param entity     字段元数据对象
     * @return 内容单元格样式实例
     */
    @Override
    public CellStyle getStyles(boolean noneStyler, ExcelExportEntity entity) {
        return centerStyle;
    }

    /**
     * 样式选择入口（带 Cell 上下文版本）。
     *
     * <p>框架在模板渲染 / Foreach / 普通导出时调用，
     * 提供了当前 cell、行号、字段、对象与原始值信息。</p>
     *
     * @param cell   当前 POI 单元格实例
     * @param row    数据行行号（不包含表头）
     * @param entity 字段元数据对象
     * @param obj    当前整行对象
     * @param value  字段值
     * @return 样式实例
     */
    @Override
    public CellStyle getStyles(Cell cell, int row, ExcelExportEntity entity, Object obj, Object value) {
        if (row < 0) {
            return headerCenterStyle;
        }
        return centerStyle;
    }

    /**
     * Foreach 模板渲染样式适配。
     *
     * <p>模板模式中不会传实际数据对象，因此优先使用模板内定义样式。</p>
     *
     * @param isSingle 是否为单列渲染
     * @param params   Foreach 参数对象（包含模板样式信息）
     * @return 样式实例
     */
    @Override
    public CellStyle getTemplateStyles(boolean isSingle, ExcelForEachParams params) {
        if (params.getCellStyle() != null) {
            return params.getCellStyle();
        }
        return centerStyle;
    }

    /**
     * 设置统一细边框。
     *
     * @param style POI 单元格样式对象
     */
    private void setThinBorder(CellStyle style) {
        style.setBorderTop(BorderStyle.THIN);
        style.setBorderBottom(BorderStyle.THIN);
        style.setBorderLeft(BorderStyle.THIN);
        style.setBorderRight(BorderStyle.THIN);
    }
}
