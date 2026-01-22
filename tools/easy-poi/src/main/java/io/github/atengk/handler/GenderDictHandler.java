package io.github.atengk.handler;

import cn.afterturn.easypoi.handler.inter.IExcelDictHandler;

/**
 * 性别字典处理器
 *
 * 统一维护性别字段的「值 ↔ 显示名称」映射关系：
 *
 * 数据库存值：
 *  1 → 男
 *  2 → 女
 *
 * 使用场景：
 * 1. 导出时：
 *    {{dict:genderDict;gender}}
 *    调用 toName，把 1 / 2 转换为 男 / 女
 *
 * 2. 导入时：
 *    Excel 中是 男 / 女
 *    调用 toValue，把 男 / 女 转换为 1 / 2
 *
 * 这样可以做到：
 * - Excel 对业务人员友好（看中文）
 * - 系统内部对数据库友好（存编码）
 *
 * @author 孔余
 * @since 2026-01-22
 */
public class GenderDictHandler implements IExcelDictHandler {

    /**
     * 导出时调用：将“字典值”转换为“显示名称”
     *
     * @param dict  字典标识，例如：gender
     * @param obj   当前行对象
     * @param name  当前字段名称
     * @param value 当前字段原始值，例如：1、2
     * @return 转换后的显示值，例如：男、女
     */
    @Override
    public String toName(String dict, Object obj, String name, Object value) {
        if (!"genderDict".equals(dict)) {
            return value == null ? "" : value.toString();
        }

        if (value == null) {
            return "";
        }

        switch (value.toString()) {
            case "1":
                return "男";
            case "2":
                return "女";
            default:
                return "未知";
        }
    }

    /**
     * 导入时调用：将“显示名称”反向转换为“字典值”
     *
     * Excel 中如果填写：
     *  男 → 返回 1
     *  女 → 返回 2
     *
     * @param dict  字典标识，例如：gender
     * @param obj   当前行对象
     * @param name  当前字段名称
     * @param value Excel 中读取到的值，例如：男、女
     * @return 转换后的字典值，例如：1、2
     */
    @Override
    public String toValue(String dict, Object obj, String name, Object value) {
        if (!"genderDict".equals(dict)) {
            return value == null ? "" : value.toString();
        }

        if (value == null) {
            return "";
        }

        switch (value.toString().trim()) {
            case "男":
                return "1";
            case "女":
                return "2";
            default:
                return "";
        }
    }
}
