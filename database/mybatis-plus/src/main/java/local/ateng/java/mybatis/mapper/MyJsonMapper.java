package local.ateng.java.mybatis.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import local.ateng.java.mybatis.entity.MyJson;

import java.util.List;

/**
 * <p>
 * Json表 Mapper 接口
 * </p>
 *
 * @author Ateng
 * @since 2025-06-13
 */
public interface MyJsonMapper extends BaseMapper<MyJson> {
    List<MyJson> selectMyJson();
}
