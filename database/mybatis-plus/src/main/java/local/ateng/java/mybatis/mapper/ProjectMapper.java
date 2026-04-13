package local.ateng.java.mybatis.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import local.ateng.java.mybatis.entity.Project;
import org.apache.ibatis.annotations.Select;

import java.util.List;

/**
 * <p>
 * 项目表，包含常用字段类型 Mapper 接口
 * </p>
 *
 * @author Ateng
 * @since 2026-04-13
 */
public interface ProjectMapper extends BaseMapper<Project> {

    @Select("select * from project_mini")
    List<Project> list();

}
