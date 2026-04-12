package io.github.atengk.redisson.controller;

import cn.hutool.core.util.ObjectUtil;
import io.github.atengk.redisson.service.VisitService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

/**
 * UV统计测试控制器
 *
 * @author Ateng
 * @since 2026-04-11
 */
@RestController
@RequestMapping("/uv")
@RequiredArgsConstructor
public class UvController {

    private final VisitService visitService;

    /**
     * 记录访问
     *
     * curl -X POST "http://localhost:8080/uv/visit?page=home&userFlag=ip_127.0.0.1"
     */
    @PostMapping("/visit")
    public Object visit(@RequestParam String page,
                        @RequestParam String userFlag) {

        if (ObjectUtil.hasEmpty(page, userFlag)) {
            return "参数不能为空";
        }

        visitService.visit(page, userFlag);

        return "记录成功";
    }

    /**
     * 获取今日UV
     *
     * curl "http://localhost:8080/uv/count?page=home"
     */
    @GetMapping("/count")
    public Object count(@RequestParam String page) {

        if (ObjectUtil.isEmpty(page)) {
            return "page不能为空";
        }

        return visitService.getUv(page);
    }

}