package local.ateng.java.mybatisjdk8.handler;

import com.alibaba.fastjson2.TypeReference;
import local.ateng.java.mybatisjdk8.entity.MyData;

import java.util.List;

/**
 * Fastjson2 的自定义类型处理器，用于处理 {@code List<MyData>} 类型与 JSON 字段之间的转换。
 * <p>
 * 继承自 {@link Fastjson2GenericTypeReferenceHandler}，通过传入 {@link TypeReference} 保留泛型类型信息，
 * 实现对嵌套集合类型的正确序列化与反序列化。
 *
 * <p>通常在 MyBatis Plus 中用于如下字段：</p>
 *
 * <pre>{@code
 * @TableField(typeHandler = Fastjson2ListMyDataTypeHandler.class)
 * private List<MyData> dataList;
 * }</pre>
 *
 * @author 孔余
 * @since 2025-07-28
 */
public class Fastjson2ListMyDataTypeHandler extends Fastjson2GenericTypeReferenceHandler<List<MyData>> {

    /**
     * 默认构造函数，传入 List<MyData> 的类型引用以保留泛型信息。
     */
    public Fastjson2ListMyDataTypeHandler() {
        super(new TypeReference<List<MyData>>() {
        });
    }

}
