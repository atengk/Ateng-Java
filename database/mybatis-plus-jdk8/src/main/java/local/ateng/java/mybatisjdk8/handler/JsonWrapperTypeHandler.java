package local.ateng.java.mybatisjdk8.handler;

import cn.hutool.core.util.ObjectUtil;
import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.JSONReader;
import com.alibaba.fastjson2.JSONWriter;
import com.baomidou.mybatisplus.extension.handlers.AbstractJsonTypeHandler;
import local.ateng.java.mybatisjdk8.entity.JsonWrapper;
import local.ateng.java.mybatisjdk8.entity.MyDataList;
import org.apache.ibatis.type.JdbcType;
import org.apache.ibatis.type.MappedJdbcTypes;
import org.apache.ibatis.type.MappedTypes;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;

/**
 * JsonWrapper 通用 TypeHandler
 *
 * @author Ateng
 * @since 2026-04-12
 */
@MappedJdbcTypes({JdbcType.VARCHAR, JdbcType.LONGVARCHAR, JdbcType.OTHER}) // 数据库字段类型
@MappedTypes({MyDataList.class})     // Java 类型
public class JsonWrapperTypeHandler<T extends JsonWrapper>
        extends AbstractJsonTypeHandler<T> {

    private static final Logger log = LoggerFactory.getLogger(JsonWrapperTypeHandler.class);

    private final Class<T> clazz;

    public JsonWrapperTypeHandler(Class<T> clazz) {
        this.clazz = clazz;
    }

    /**
     * 解析 JSON → 包装类
     */
    @Override
    protected T parse(String json) {
        if (ObjectUtil.isEmpty(json)) {
            return null;
        }

        try {
            T wrapper = clazz.newInstance();

            // 获取泛型 T
            Type superType = clazz.getGenericSuperclass();

            if (superType instanceof ParameterizedType) {
                Type actualType = ((ParameterizedType) superType)
                        .getActualTypeArguments()[0];

                Object value = JSON.parseObject(
                        json, actualType,
                        // 默认下是camel case精确匹配，打开这个后，能够智能识别camel/upper/pascal/snake/Kebab五中case
                        JSONReader.Feature.SupportSmartMatch,
                        // 允许字段名不带引号
                        JSONReader.Feature.AllowUnQuotedFieldNames,
                        // 忽略无法序列化的字段
                        JSONReader.Feature.IgnoreNoneSerializable
                );

                wrapper.setValue(value);
            }

            return wrapper;

        } catch (Exception e) {
            log.error("JsonWrapper 反序列化失败", e);
            return null;
        }
    }

    /**
     * 包装类 → JSON
     */
    @Override
    protected String toJson(T obj) {
        if (ObjectUtil.isEmpty(obj)) {
            return null;
        }

        try {
            return JSON.toJSONString(
                    obj.getValue(),
                    // 序列化输出空值字段
                    JSONWriter.Feature.WriteNulls,
                    // 基于字段反序列化
                    JSONWriter.Feature.FieldBased
            );
        } catch (Exception e) {
            log.error("JsonWrapper 序列化失败", e);
            return null;
        }
    }
}
