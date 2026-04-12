package io.github.atengk.redisson.controller;

import cn.hutool.core.util.ObjectUtil;
import io.github.atengk.redisson.service.BloomFilterService;
import io.github.atengk.redisson.service.UserQueryService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

/**
 * 布隆过滤器测试控制器
 *
 * @author Ateng
 * @since 2026-04-11
 */
@RestController
@RequestMapping("/bf")
@RequiredArgsConstructor
public class BloomController {

    private final BloomFilterService bloomFilterService;
    private final UserQueryService userQueryService;

    /**
     * 初始化布隆过滤器
     *
     * curl -X POST "http://localhost:8080/bf/init?size=1000000&fpp=0.01"
     */
    @PostMapping("/init")
    public Object init(@RequestParam Long size,
                       @RequestParam Double fpp) {

        if (ObjectUtil.hasEmpty(size, fpp)) {
            return "参数不能为空";
        }

        bloomFilterService.init(size, fpp);

        return "初始化成功";
    }

    /**
     * 添加用户
     *
     * curl -X POST "http://localhost:8080/bf/add?userId=1"
     */
    @PostMapping("/add")
    public Object add(@RequestParam Long userId) {

        if (ObjectUtil.isEmpty(userId)) {
            return "userId不能为空";
        }

        bloomFilterService.add(userId);

        return "添加成功";
    }

    /**
     * 查询用户
     *
     * curl "http://localhost:8080/bf/query?userId=1"
     */
    @GetMapping("/query")
    public Object query(@RequestParam Long userId) {

        if (ObjectUtil.isEmpty(userId)) {
            return "userId不能为空";
        }

        return userQueryService.query(userId);
    }

}