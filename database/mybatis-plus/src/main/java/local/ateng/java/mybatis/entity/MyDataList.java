package local.ateng.java.mybatis.entity;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serial;
import java.io.Serializable;
import java.util.List;

/**
 * MyData 列表包装对象
 *
 * 用于承载 JSON 数组结构的数据，在持久化层通过 TypeHandler
 * 实现与数据库 JSON 字段的映射，避免直接使用泛型（如 List<MyData>）
 * 在 MyBatis 中因类型擦除导致的反序列化不准确问题。
 *
 * 设计说明：
 * 1. 使用具体类型替代泛型声明，提升 TypeHandler 解析稳定性
 * 2. 作为 JSON 字段的领域模型载体，便于扩展（如后续增加元数据）
 * 3. 适用于需要强类型约束的 JSON 数组场景
 *
 * @author Ateng
 * @since 2026-04-12
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
public class MyDataList implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * 实际数据集合
     *
     * 对应数据库 JSON 数组内容
     */
    private List<MyData> dataList;

}