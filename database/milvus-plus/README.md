# Dromara MilvusPlus：向量数据库增强操作库

MilvusPlus（简称 MP）是一个 Milvus 的操作工具，旨在简化与 Milvus 向量数据库的交互，为开发者提供类似 MyBatis-Plus 注解和方法调用风格的直观 API,提高效率而生。

- [Gitee](https://gitee.com/dromara/MilvusPlus)



## 基础配置

**添加依赖**

```xml
<!-- Dromara MilvusPlus：向量数据库增强操作库 -->
<dependency>
    <groupId>org.dromara.milvus-plus</groupId>
    <artifactId>milvus-plus-boot-starter</artifactId>
    <version>2.2.5</version>
</dependency>
```

**编辑配置文件**

`application.yml`

```yaml
---
# MilvusPlus 配置
milvus:
  uri: http://192.168.1.12:40140
  # token: root:Milvus
  enable: true
  open-log: true
  db-name: kongyu
  username: root
  password: Milvus
  packages:
    - io.github.atengk.milvus.entity
```



## 快速使用

### 创建实体类

```java
package io.github.atengk.milvus.entity;

import io.milvus.v2.common.DataType;
import io.milvus.v2.common.IndexParam;
import lombok.Data;
import org.dromara.milvus.plus.annotation.ExtraParam;
import org.dromara.milvus.plus.annotation.MilvusCollection;
import org.dromara.milvus.plus.annotation.MilvusField;
import org.dromara.milvus.plus.annotation.MilvusIndex;

import java.util.List;

/**
 * 人脸向量实体
 *
 * @author Ateng
 * @since 2026-04-10
 */
@Data
@MilvusCollection(name = "face_collection") // 指定Milvus集合的名称
public class Face {
    @MilvusField(
            name = "person_id", // 字段名称
            dataType = DataType.Int64, // 数据类型为64位整数
            isPrimaryKey = true // 标记为主键
    )
    private Long personId; // 人员的唯一标识符

    @MilvusField(
            name = "face_vector", // 字段名称
            dataType = DataType.FloatVector, // 数据类型为浮点型向量
            dimension = 128 // 向量维度，假设人脸特征向量的维度是128
    )
    @MilvusIndex(
            indexType = IndexParam.IndexType.IVF_FLAT, // 使用IVF_FLAT索引类型
            metricType = IndexParam.MetricType.L2, // 使用L2距离度量类型
            indexName = "face_index", // 索引名称
            extraParams = { // 指定额外的索引参数
                    @ExtraParam(key = "nlist", value = "100") // 例如，IVF的nlist参数
            }
    )
    private List<Float> faceVector; // 存储人脸特征的向量
}
```

### 创建 Mapper

```
package io.github.atengk.milvus.mapper;

import io.github.atengk.milvus.entity.Face;
import org.dromara.milvus.plus.mapper.MilvusMapper;
import org.springframework.stereotype.Component;

/**
 * 人脸向量 Mapper
 *
 * @author Ateng
 * @since 2026-04-10
 */
@Component
public class FaceMilvusMapper extends MilvusMapper<Face> {

}
```

### 创建接口

```java
package io.github.atengk.milvus.controller;

import io.github.atengk.milvus.entity.Face;
import io.github.atengk.milvus.mapper.FaceMilvusMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

/**
 * Milvus 向量操作接口
 *
 * @author Ateng
 * @since 2026-04-10
 */
@RestController
@RequestMapping("/milvus")
@RequiredArgsConstructor
public class MilvusController {

    private final FaceMilvusMapper faceMilvusMapper;

    /**
     * 插入测试数据
     */
    @PostMapping("/insert")
    public Object insert(@RequestParam(defaultValue = "10") int count) {

        List<Face> list = new ArrayList<>();

        for (long i = 1; i <= count; i++) {
            Face face = new Face();
            face.setPersonId(i);
            face.setFaceVector(randomVector(128));
            list.add(face);
        }

        return faceMilvusMapper.insert(list.toArray(new Face[0]));
    }

    /**
     * 向量检索
     */
    @GetMapping("/search")
    public Object search(@RequestParam(defaultValue = "3") int topK) {

        List<Float> queryVector = randomVector(128);

        return faceMilvusMapper.queryWrapper()
                .vector(Face::getFaceVector, queryVector)
                .topK(topK)
                .query();
    }

    /**
     * 生成随机向量
     */
    private List<Float> randomVector(int dimension) {
        List<Float> vector = new ArrayList<>(dimension);
        for (int i = 0; i < dimension; i++) {
            vector.add(ThreadLocalRandom.current().nextFloat());
        }
        return vector;
    }
}
```

![image-20260410175301154](./assets/image-20260410175301154.png)



## 更多使用示例

```java
package io.github.atengk.milvus.controller;

import io.github.atengk.milvus.entity.Face;
import io.github.atengk.milvus.mapper.FaceMilvusMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ThreadLocalRandom;

/**
 * Milvus 向量操作接口
 *
 * @author Ateng
 * @since 2026-04-10
 */
@RestController
@RequestMapping("/milvus")
@RequiredArgsConstructor
public class MilvusController {

    private final FaceMilvusMapper faceMilvusMapper;

    /**
     * 插入测试数据
     */
    @PostMapping("/insert")
    public Object insert(@RequestParam(defaultValue = "10") int count) {

        List<Face> list = new ArrayList<>();

        for (long i = 1; i <= count; i++) {
            Face face = new Face();
            face.setPersonId(i);
            face.setFaceVector(randomVector(128));
            list.add(face);
        }

        return faceMilvusMapper.insert(list.toArray(new Face[0]));
    }

    /**
     * 向量检索
     */
    @GetMapping("/search")
    public Object search(@RequestParam(defaultValue = "3") int topK) {

        List<Float> queryVector = randomVector(128);

        return faceMilvusMapper.queryWrapper()
                .vector(Face::getFaceVector, queryVector)
                .topK(topK)
                .query();
    }

    /**
     * 生成随机向量
     */
    private List<Float> randomVector(int dimension) {
        List<Float> vector = new ArrayList<>(dimension);
        for (int i = 0; i < dimension; i++) {
            vector.add(ThreadLocalRandom.current().nextFloat());
        }
        return vector;
    }

    /**
     * 根据ID查询
     */
    @GetMapping("/getById")
    public Object getById(@RequestParam Long id) {
        return faceMilvusMapper.getById(id);
    }

    /**
     * 根据ID删除
     */
    @DeleteMapping("/delete")
    public Object delete(@RequestParam Long id) {
        return faceMilvusMapper.removeById(id);
    }

    /**
     * 更新向量
     */
    @PostMapping("/update")
    public Object update(@RequestParam Long id) {

        Face face = new Face();
        face.setPersonId(id);
        face.setFaceVector(randomVector(128));

        return faceMilvusMapper.updateById(face);
    }

    /**
     * 标量查询（等值）
     */
    @GetMapping("/query/eq")
    public Object queryEq(@RequestParam Long id) {

        return faceMilvusMapper.queryWrapper()
                .eq(Face::getPersonId, id)
                .query();
    }

    /**
     * 范围查询
     */
    @GetMapping("/query/range")
    public Object queryRange() {

        return faceMilvusMapper.queryWrapper()
                .between(Face::getPersonId, 1L, 5L)
                .query();
    }

    /**
     * IN 查询
     */
    @GetMapping("/query/in")
    public Object queryIn() {

        return faceMilvusMapper.queryWrapper()
                .in(Face::getPersonId, List.of(1L, 2L, 3L))
                .query();
    }

    /**
     * 向量 + 条件过滤
     */
    @GetMapping("/search/filter")
    public Object searchWithFilter() {

        return faceMilvusMapper.queryWrapper()
                .vector(Face::getFaceVector, randomVector(128))
                .ne(Face::getPersonId, 1L)
                .topK(5)
                .query();
    }

    /**
     * 指定返回字段
     */
    @GetMapping("/search/fields")
    public Object searchFields() {

        return faceMilvusMapper.queryWrapper()
                .vector(Face::getFaceVector, randomVector(128))
                .topK(3)
                .query(Face::getPersonId);
    }

    /**
     * 自定义搜索参数
     */
    @GetMapping("/search/params")
    public Object searchParams() {

        Map<String, Object> params = new HashMap<>();
        params.put("metric_type", "L2");
        params.put("radius", 0.8f);
        params.put("range_filter", 0.2f);

        return faceMilvusMapper.queryWrapper()
                .vector(Face::getFaceVector, randomVector(128))
                .searchParams(params)
                .topK(5)
                .query();
    }

    /**
     * 限制返回数量
     */
    @GetMapping("/query/limit")
    public Object queryLimit() {

        return faceMilvusMapper.queryWrapper()
                .limit(5L)
                .query();
    }

    /**
     * 使用 Wrapper 插入
     */
    @PostMapping("/insert/wrapper")
    public Object insertWrapper() {

        return faceMilvusMapper.lambda(faceMilvusMapper.insertWrapper())
                .put("person_id", 999L)
                .put("face_vector", randomVector(128))
                .insert();
    }

    /**
     * 条件删除
     */
    @DeleteMapping("/delete/condition")
    public Object deleteCondition() {

        return faceMilvusMapper.deleteWrapper()
                .eq("person_id", 10L)
                .remove();
    }

    /**
     * 条件更新
     */
    @PostMapping("/update/condition")
    public Object updateCondition() {

        Face face = new Face();
        face.setFaceVector(randomVector(128));

        return faceMilvusMapper.updateWrapper()
                .eq("person_id", 2L)
                .update(face);
    }


}
```

