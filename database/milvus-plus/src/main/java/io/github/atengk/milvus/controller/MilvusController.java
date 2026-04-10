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
