package local.ateng.java.mybatisjdk8.handler;

import com.alibaba.fastjson2.TypeReference;
import local.ateng.java.mybatisjdk8.entity.MyData;

/**
 * Fastjson2 类型处理器，用于将 {@link MyData} 类型与 JSON 字段之间进行序列化与反序列化。
 * <p>
 * 继承自 {@link Fastjson2GenericTypeReferenceHandler}，通过 {@link TypeReference} 保留泛型类型信息，
 * 实现对 MyBatis Plus 字段的自动 JSON 映射。
 *
 * <p>典型用法如下：</p>
 * <pre>{@code
 * @TableField(typeHandler = Fastjson2MyDataTypeHandler.class)
 * private MyData data;
 * }</pre>
 *
 * @author 孔余
 * @since 2025-07-28
 */
public class Fastjson2MyDataTypeHandler extends Fastjson2GenericTypeReferenceHandler<MyData> {

    /**
     * 默认构造方法，传入 MyData 的类型引用，用于保留类型信息以支持反序列化。
     */
    public Fastjson2MyDataTypeHandler() {
        super(new TypeReference<MyData>() {
        });
    }
}
