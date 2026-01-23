package io.github.atengk.style;

import cn.afterturn.easypoi.entity.BaseTypeConstants;
import cn.afterturn.easypoi.excel.entity.params.ExcelExportEntity;
import cn.afterturn.easypoi.excel.entity.params.ExcelForEachParams;
import cn.afterturn.easypoi.excel.export.styler.AbstractExcelExportStyler;
import org.apache.poi.ss.usermodel.*;

/**
 * Excel 导出样式策略实现类。
 *
 * <p>样式适用范围：</p>
 * <ul>
 *     <li>普通导出场景</li>
 *     <li>模板渲染场景（Foreach）</li>
 * </ul>
 *
 * <p>样式策略说明：</p>
 * <ul>
 *     <li>表头类单元格：加粗、灰底、水平垂直居中</li>
 *     <li>文本类单元格：左对齐</li>
 *     <li>数字类单元格：右对齐</li>
 *     <li>日期类单元格：居中</li>
 *     <li>图片类型单元格：居中</li>
 *     <li>所有单元格统一配置细边框</li>
 * </ul>
 *
 * <p>基于 EasyPOI 类型常量 {@link BaseTypeConstants} 映射样式。</p>
 *
 * @author
 * @since 2026-01-22
 */
public class CustomExcelExportStyler extends AbstractExcelExportStyler {

    /** 表头单元格样式（灰底、加粗、居中对齐） */
    private final CellStyle headerCenterStyle;

    /** 文本单元格样式（左对齐） */
    private final CellStyle textLeftStyle;

    /** 数字单元格样式（右对齐） */
    private final CellStyle numberRightStyle;

    /** 日期单元格样式（居中对齐） */
    private final CellStyle dateCenterStyle;


    /**
     * 构造函数。
     *
     * @param workbook Excel 工作簿实例
     */
    public CustomExcelExportStyler(Workbook workbook) {
        super.createStyles(workbook);
        this.headerCenterStyle = createHeaderStyle();
        this.textLeftStyle = createTextStyle();
        this.numberRightStyle = createNumberStyle();
        this.dateCenterStyle = createDateStyle();
    }


    /**
     * 创建表头样式。
     * <p>配置内容：</p>
     * <ul>
     *     <li>字体加粗</li>
     *     <li>灰色背景填充</li>
     *     <li>水平垂直居中</li>
     *     <li>细边框</li>
     * </ul>
     *
     * @return 表头样式
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

        style.setFillForegroundColor(IndexedColors.GREY_25_PERCENT.getIndex());
        style.setFillPattern(FillPatternType.SOLID_FOREGROUND);

        setThinBorder(style);

        return style;
    }


    /**
     * 创建文本类型样式（左对齐）。
     *
     * @return 文本样式
     */
    private CellStyle createTextStyle() {
        CellStyle style = workbook.createCellStyle();

        Font font = workbook.createFont();
        font.setFontName("宋体");
        font.setFontHeightInPoints((short) 11);
        style.setFont(font);

        style.setAlignment(HorizontalAlignment.LEFT);
        style.setVerticalAlignment(VerticalAlignment.CENTER);

        setThinBorder(style);

        return style;
    }


    /**
     * 创建数字类型样式（右对齐）。
     *
     * @return 数字样式
     */
    private CellStyle createNumberStyle() {
        CellStyle style = workbook.createCellStyle();

        Font font = workbook.createFont();
        font.setFontName("宋体");
        font.setFontHeightInPoints((short) 11);
        style.setFont(font);

        style.setAlignment(HorizontalAlignment.RIGHT);
        style.setVerticalAlignment(VerticalAlignment.CENTER);

        setThinBorder(style);

        return style;
    }


    /**
     * 创建日期类型样式（居中）。
     *
     * @return 日期样式
     */
    private CellStyle createDateStyle() {
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
     * 表头样式适配（调用自定义样式）。
     *
     * @param color 表头颜色（框架传入）
     * @return 表头样式
     */
    @Override
    public CellStyle getTitleStyle(short color) {
        return headerCenterStyle;
    }


    /**
     * 多级表头样式适配。
     *
     * @param color 表头颜色
     * @return 表头样式
     */
    @Override
    public CellStyle getHeaderStyle(short color) {
        return headerCenterStyle;
    }

    /**
     * 样式选择入口（无 Cell 上下文版本）。
     *
     * <p>该方法一般用于注解模式下的导出场景，框架仅提供字段元数据（entity），
     * 不提供当前单元格信息，因此样式决策完全依赖字段类型。</p>
     *
     * @param noneStyler 是否忽略框架默认样式（一般无需关注）
     * @param entity     字段元数据对象
     * @return CellStyle 样式实例
     */
    @Override
    public CellStyle getStyles(boolean noneStyler, ExcelExportEntity entity) {
        return resolveStyleByType(entity != null ? entity.getType() : null);
    }

    /**
     * 样式选择入口（带 Cell 上下文版本）。
     *
     * <p>该方法在动态渲染、Foreach 模板或模板填充模式下触发，
     * 框架会提供 Cell、行号、值等上下文信息。
     * 样式决策仍基于字段类型进行。</p>
     *
     * @param cell   当前 POI Cell 对象
     * @param row    数据行行号（不含表头）
     * @param entity 字段元数据对象
     * @param obj    当前整行数据对象
     * @param value  字段对应的原始值
     * @return CellStyle 样式实例
     */
    @Override
    public CellStyle getStyles(Cell cell, int row, ExcelExportEntity entity, Object obj, Object value) {
        return resolveStyleByType(entity != null ? entity.getType() : null);
    }

    /**
     * Foreach 模板渲染使用的样式选择。
     *
     * <p>该方法在处理 `{{$fe:list t.name}}` 等模板语法时触发，
     * 字段元数据需从 ExcelForEachParams 中提取，因此与注解模式入口分离。</p>
     *
     * @param isSingle 是否为单列渲染（框架内部字段）
     * @param params   Foreach 参数对象，包含字段元数据
     * @return CellStyle 样式实例
     */
    @Override
    public CellStyle getTemplateStyles(boolean isSingle, ExcelForEachParams params) {
        return textLeftStyle;
    }


    /**
     * 基于字段类型决定样式（公共逻辑）。
     *
     * <p>该方法用于将 BaseTypeConstants 类型映射为对应的单元格样式。
     * 由于 BaseTypeConstants 为包装类型 Integer，其类型值在运行期才能确定，
     * 因此采用 if/else 判断而非 switch 常量表达式。</p>
     *
     * <p>样式映射规则：</p>
     * <ul>
     *     <li>DOUBLE_TYPE → 数字样式（右对齐）</li>
     *     <li>DATE_TYPE、IMAGE_TYPE → 日期/图片类样式（居中）</li>
     *     <li>其他或空类型 → 文本样式（左对齐）</li>
     * </ul>
     *
     * @param type BaseTypeConstants 字段类型
     * @return CellStyle 样式实例
     */
    private CellStyle resolveStyleByType(Integer type) {
        if (type == null) {
            return textLeftStyle;
        }
        if (BaseTypeConstants.DOUBLE_TYPE.equals(type)) {
            return numberRightStyle;
        }
        if (BaseTypeConstants.DATE_TYPE.equals(type) || BaseTypeConstants.IMAGE_TYPE.equals(type)) {
            return dateCenterStyle;
        }
        return textLeftStyle;
    }

    /**
     * 设置细边框，增强单元格视觉边界。
     *
     * @param style 单元格样式对象
     */
    private void setThinBorder(CellStyle style) {
        style.setBorderTop(BorderStyle.THIN);
        style.setBorderBottom(BorderStyle.THIN);
        style.setBorderLeft(BorderStyle.THIN);
        style.setBorderRight(BorderStyle.THIN);
    }
}
