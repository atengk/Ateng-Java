package local.ateng.java.mybatisjdk8.controller;

import local.ateng.java.mybatisjdk8.entity.Project;
import local.ateng.java.mybatisjdk8.service.IProjectService;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

/**
 * <p>
 * 项目表，包含常用字段类型 前端控制器
 * </p>
 *
 * @author Ateng
 * @since 2025-07-17
 */
@RestController
@RequestMapping("/project")
@RequiredArgsConstructor(onConstructor = @__(@Autowired))
public class ProjectController {
    private final IProjectService projectService;

    @GetMapping("/one")
    public Project getOne() {
        Project project = projectService.lambdaQuery()
                .eq(Project::getId, 1)
                .orderByDesc(Project::getId)
                .last("limit 1")
                .one();
        return project;
    }

    @PostMapping("/add")
    public Boolean add(@RequestBody Project project) {
        return projectService.save(project);
    }

}
