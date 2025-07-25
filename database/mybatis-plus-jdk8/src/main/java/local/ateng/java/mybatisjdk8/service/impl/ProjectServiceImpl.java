package local.ateng.java.mybatisjdk8.service.impl;

import local.ateng.java.mybatisjdk8.entity.Project;
import local.ateng.java.mybatisjdk8.mapper.ProjectMapper;
import local.ateng.java.mybatisjdk8.service.IProjectService;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.springframework.stereotype.Service;

/**
 * <p>
 * 项目表，包含常用字段类型 服务实现类
 * </p>
 *
 * @author Ateng
 * @since 2025-07-17
 */
@Service
public class ProjectServiceImpl extends ServiceImpl<ProjectMapper, Project> implements IProjectService {

}
