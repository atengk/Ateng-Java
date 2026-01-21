package io.github.atengk.handler;

import cn.afterturn.easypoi.handler.inter.IExcelDataHandler;
import org.apache.poi.ss.usermodel.CreationHelper;
import org.apache.poi.ss.usermodel.Hyperlink;

import java.util.HashMap;
import java.util.Map;

/**
 * number 字段导入导出的自定义处理器
 *
 * 功能：
 * - 导出：1 -> 一号、2 -> 二号、3 -> 三号
 * - 导入：一号 -> 1、二号 -> 2、三号 -> 3
 *
 * 注意点：
 * - 实现 IExcelDataHandler 全部方法
 */
public class NumberDataHandler implements IExcelDataHandler<Object> {

    private String[] needHandlerFields;

    /**
     * 字典映射（可改）
     */
    private static final Map<String, String> EXPORT_MAP = new HashMap<>();
    private static final Map<String, String> IMPORT_MAP = new HashMap<>();

    static {
        EXPORT_MAP.put("1", "一号");
        EXPORT_MAP.put("2", "二号");
        EXPORT_MAP.put("3", "三号");

        IMPORT_MAP.put("一号", "1");
        IMPORT_MAP.put("二号", "2");
        IMPORT_MAP.put("三号", "3");
    }

    @Override
    public Object exportHandler(Object obj, String name, Object value) {
        if (!match(name)) {
            return value;
        }
        if (value == null) {
            return null;
        }
        String raw = String.valueOf(value);
        return EXPORT_MAP.getOrDefault(raw, raw);
    }

    @Override
    public Object importHandler(Object obj, String name, Object value) {
        if (!match(name)) {
            return value;
        }
        if (value == null) {
            return null;
        }
        String raw = String.valueOf(value);
        return IMPORT_MAP.getOrDefault(raw, raw);
    }

    @Override
    public String[] getNeedHandlerFields() {
        return needHandlerFields;
    }

    @Override
    public void setNeedHandlerFields(String[] fields) {
        this.needHandlerFields = fields;
    }

    @Override
    public void setMapValue(Map<String, Object> map, String originKey, Object value) {
        if (!match(originKey)) {
            map.put(originKey, value);
            return;
        }

        if (value != null) {
            String raw = String.valueOf(value);
            map.put(originKey, IMPORT_MAP.getOrDefault(raw, raw));
        } else {
            map.put(originKey, null);
        }
    }

    @Override
    public Hyperlink getHyperlink(CreationHelper creationHelper, Object obj, String name, Object value) {
        // 这里通常不用超链接，返回 null 即可
        return null;
    }

    /**
     * 判断字段是否在处理范围
     */
    private boolean match(String name) {
        if (needHandlerFields == null) {
            return false;
        }
        for (String field : needHandlerFields) {
            if (field.equals(name)) {
                return true;
            }
        }
        return false;
    }
}
