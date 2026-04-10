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
