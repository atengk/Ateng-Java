package local.ateng.java.mybatisjdk8.handler;

import com.alibaba.fastjson.TypeReference;
import local.ateng.java.mybatisjdk8.entity.MyData;
import org.apache.ibatis.type.JdbcType;
import org.apache.ibatis.type.MappedJdbcTypes;
import org.apache.ibatis.type.MappedTypes;

import java.util.List;

/**
 * Fastjson2 的自定义类型处理器，用于处理 {@code List<MyData>} 类型与 JSON 字段之间的转换。
 * <p>
 * 继承自 {@link FastjsonGenericTypeReferenceHandler}，通过传入 {@link TypeReference} 保留泛型类型信息，
 * 实现对嵌套集合类型的正确序列化与反序列化。
 *
 * <p>通常在 MyBatis Plus 中用于如下字段：</p>
 *
 * <pre>{@code
 * @TableField(typeHandler = FastjsonListMyDataTypeHandler.class)
 * private List<MyData> dataList;
 * }</pre>
 *
 * @author 孔余
 * @since 2025-07-28
 */
@MappedJdbcTypes({JdbcType.VARCHAR, JdbcType.LONGVARCHAR, JdbcType.OTHER}) // 数据库字段类型
@MappedTypes({List.class})     // Java 类型
public class FastjsonListMyDataTypeHandler extends FastjsonGenericTypeReferenceHandler<List<MyData>> {

    /**
     * 默认构造函数，传入 List<MyData> 的类型引用以保留泛型信息。
     */
    public FastjsonListMyDataTypeHandler() {
        super(new TypeReference<List<MyData>>() {
        });
    }

}
