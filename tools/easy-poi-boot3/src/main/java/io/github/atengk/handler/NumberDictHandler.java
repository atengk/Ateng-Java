package io.github.atengk.handler;

import cn.afterturn.easypoi.handler.inter.IExcelDictHandler;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class NumberDictHandler implements IExcelDictHandler {
    @Override
    public List<Map> getList(String dict) {
        List<Map> list = new ArrayList<>();
        Map<String, String> dictMap = new HashMap<>(2);
        dictMap.put("dictKey", "1");
        dictMap.put("dictValue", "青年");
        list.add(dictMap);
        dictMap = new HashMap<>(2);
        dictMap.put("dictKey", "2");
        dictMap.put("dictValue", "中年");
        list.add(dictMap);
        dictMap = new HashMap<>(2);
        dictMap.put("dictKey", "3");
        dictMap.put("dictValue", "老年");
        list.add(dictMap);
        return list;
    }

    @Override
    public String toName(String dict, Object obj, String name, Object value) {
        if ("ageDict".equals(dict)) {
            if (value == null) {
                return "";
            }
            switch (value.toString()) {
                case "1":
                    return "青年";
                case "2":
                    return "中年";
                case "3":
                    return "老年";
            }
        }
        return null;
    }

    @Override
    public String toValue(String dict, Object obj, String name, Object value) {
        if ("ageDict".equals(dict)) {
            if (value == null) {
                return null;
            }
            switch (value.toString()) {
                case "青年":
                    return "1";
                case "中年":
                    return "2";
                case "老年":
                    return "3";
            }
        }
        return null;
    }
}

