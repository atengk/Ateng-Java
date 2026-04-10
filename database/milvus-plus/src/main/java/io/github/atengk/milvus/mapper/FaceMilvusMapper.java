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
