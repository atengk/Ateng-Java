package local.ateng.java.mybatisjdk8.handler;

import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.JSONReader;
import com.alibaba.fastjson2.JSONWriter;
import com.alibaba.fastjson2.TypeReference;
import com.baomidou.mybatisplus.extension.handlers.AbstractJsonTypeHandler;

import java.lang.reflect.Type;

public class Fastjson2GenericTypeHandler<T> extends AbstractJsonTypeHandler<T> {

    private final Type type;

    public Fastjson2GenericTypeHandler(TypeReference<T> typeReference) {
        this.type = typeReference.getType();
    }

    @Override
    protected T parse(String json) {
        if (json == null || json.trim().isEmpty()) {
            return null;
        }
        try {
            return JSON.parseObject(
                    json, type,
                    // 支持将 JSON 数组映射为 Java Bean（按顺序赋值）
                    JSONReader.Feature.SupportArrayToBean,
                    // 支持智能匹配字段名称（camelCase, PascalCase, snake_case等）
                    JSONReader.Feature.SupportSmartMatch
            );
        } catch (Exception e) {
            return null;
        }
    }

    @Override
    protected String toJson(T obj) {
        try {
            if (obj == null) {
                return null;
            }
            return JSON.toJSONString(
                    obj,
                    // 序列化输出空值字段
                    JSONWriter.Feature.WriteNulls,
                    // 在大范围超过JavaScript支持的整数，输出为字符串格式
                    JSONWriter.Feature.BrowserCompatible,
                    // 序列化BigDecimal使用toPlainString，避免科学计数法
                    JSONWriter.Feature.WriteBigDecimalAsPlain
            );
        } catch (Exception e) {
            return null;
        }
    }
}
