package io.github.atengk.handler;

import cn.afterturn.easypoi.handler.inter.IExcelDictHandler;

public class NumberDictHandler implements IExcelDictHandler {

    @Override
    public String toName(String dict, Object obj, String name, Object value) {
        if ("ageDict".equals(dict)) {
            if (value == null) {
                return "";
            }
            switch (value.toString()) {
                case "1": return "青年";
                case "2": return "中年";
                case "3": return "老年";
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
                case "青年": return "1";
                case "中年": return "2";
                case "老年": return "3";
            }
        }
        return null;
    }
}

