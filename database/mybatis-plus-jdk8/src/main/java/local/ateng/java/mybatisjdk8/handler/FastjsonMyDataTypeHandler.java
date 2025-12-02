package local.ateng.java.mybatisjdk8.handler;

import com.alibaba.fastjson.TypeReference;
import local.ateng.java.mybatisjdk8.entity.MyData;
import org.apache.ibatis.type.JdbcType;
import org.apache.ibatis.type.MappedJdbcTypes;
import org.apache.ibatis.type.MappedTypes;

/**
 * Fastjson 类型处理器，用于将 {@link MyData} 类型与 JSON 字段之间进行序列化与反序列化。
 * <p>
 * 继承自 {@link FastjsonGenericTypeReferenceHandler}，通过 {@link TypeReference} 保留泛型类型信息，
 * 实现对 MyBatis Plus 字段的自动 JSON 映射。
 *
 * <p>典型用法如下：</p>
 * <pre>{@code
 * @TableField(typeHandler = FastjsonMyDataTypeHandler.class)
 * private MyData data;
 * }</pre>
 *
 * @author 孔余
 * @since 2025-07-28
 */
@MappedJdbcTypes({JdbcType.VARCHAR, JdbcType.LONGVARCHAR, JdbcType.OTHER}) // 数据库字段类型
@MappedTypes({MyData.class})     // Java 类型
public class FastjsonMyDataTypeHandler extends FastjsonGenericTypeReferenceHandler<MyData> {

    /**
     * 默认构造方法，传入 MyData 的类型引用，用于保留类型信息以支持反序列化。
     */
    public FastjsonMyDataTypeHandler() {
        super(new TypeReference<MyData>() {
        });
    }
}
