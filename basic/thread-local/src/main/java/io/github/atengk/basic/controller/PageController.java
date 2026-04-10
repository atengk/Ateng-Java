package io.github.atengk.basic.controller;

import io.github.atengk.basic.holder.PageContextHolder;
import io.github.atengk.basic.service.UserPageService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 分页测试控制器
 *
 * @author Ateng
 * @since 2026-04-10
 */
@RestController
public class PageController {

    private final UserPageService service;

    public PageController(UserPageService service) {
        this.service = service;
    }

    /**
     * 测试分页
     *
     * http://localhost:8080/test/page?page=1&size=5
     */
    @GetMapping("/test/page")
    public List<String> page(int page, int size) {

        try {
            // 设置分页上下文
            PageContextHolder.set(page, size);

            return service.listUsers();
        } finally {
            // 必须清理
            PageContextHolder.clear();
        }
    }
}
