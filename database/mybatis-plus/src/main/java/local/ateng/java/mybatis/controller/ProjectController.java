package local.ateng.java.mybatis.controller;

import local.ateng.java.mybatis.service.IProjectService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * <p>
 * 项目表，包含常用字段类型 前端控制器
 * </p>
 *
 * @author Ateng
 * @since 2026-04-13
 */
@RestController
@RequestMapping("/project")
@RequiredArgsConstructor
public class ProjectController {

    private final IProjectService projectService;

    @GetMapping("/count")
    public Long count() {
        return projectService.count();
    }

}
