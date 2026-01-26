package io.github.atengk.util;

import org.apache.fesod.sheet.converters.Converter;
import org.apache.fesod.sheet.enums.CellDataTypeEnum;
import org.apache.fesod.sheet.metadata.GlobalConfiguration;
import org.apache.fesod.sheet.metadata.data.ReadCellData;
import org.apache.fesod.sheet.metadata.data.WriteCellData;
import org.apache.fesod.sheet.metadata.property.ExcelContentProperty;

/**
 * 性别字段 Excel 映射转换器
 * <p>
 * 功能说明：
 * 1. 导出时：将 Java 中的性别编码转换为 Excel 可读文本
 * 2. 导入时：将 Excel 中的性别文本转换为 Java 性别编码
 * <p>
 * 映射规则：
 * Java -> Excel
 * 1  -> 男
 * 2  -> 女
 * 0  -> 未知
 * <p>
 * Excel -> Java
 * 男   -> 1
 * 女   -> 2
 * 未知 -> 0
 * <p>
 * 使用方式：
 * 在实体字段上配置：
 *
 * @author 孔余
 * @ExcelProperty(value = "性别", converter = GenderConverter.class)
 * <p>
 * 适用场景：
 * - 枚举字段导入导出
 * - 字典字段导入导出
 * - 固定映射规则字段
 * @since 2026-01-26
 */
public class GenderConverter implements Converter<Integer> {

    /**
     * 返回当前转换器支持的 Java 类型
     *
     * @return Java 字段类型
     */
    @Override
    public Class<?> supportJavaTypeKey() {
        return Integer.class;
    }

    /**
     * 返回当前转换器支持的 Excel 单元格类型
     *
     * @return Excel 单元格类型枚举
     */
    @Override
    public CellDataTypeEnum supportExcelTypeKey() {
        return CellDataTypeEnum.STRING;
    }

    /**
     * Excel -> Java 数据转换
     * <p>
     * 在 Excel 导入时执行：
     * 将单元格中的文本转换为 Java 字段值
     *
     * @param cellData            Excel 单元格数据
     * @param contentProperty     字段内容属性
     * @param globalConfiguration 全局配置
     * @return Java 字段值
     */
    @Override
    public Integer convertToJavaData(ReadCellData<?> cellData,
                                     ExcelContentProperty contentProperty,
                                     GlobalConfiguration globalConfiguration) {

        String value = cellData.getStringValue();

        if ("男".equals(value)) {
            return 1;
        }

        if ("女".equals(value)) {
            return 2;
        }

        if ("未知".equals(value)) {
            return 0;
        }

        return null;
    }

    /**
     * Java -> Excel 数据转换
     * <p>
     * 在 Excel 导出时执行：
     * 将 Java 字段值转换为 Excel 单元格显示文本
     *
     * @param value               Java 字段值
     * @param contentProperty     字段内容属性
     * @param globalConfiguration 全局配置
     * @return Excel 单元格数据对象
     */
    @Override
    public WriteCellData<?> convertToExcelData(Integer value,
                                               ExcelContentProperty contentProperty,
                                               GlobalConfiguration globalConfiguration) {

        String text;

        if (value == null) {
            text = "";
        } else if (value == 1) {
            text = "男";
        } else if (value == 2) {
            text = "女";
        } else if (value == 0) {
            text = "未知";
        } else {
            text = "未知";
        }

        return new WriteCellData<>(text);
    }
}
