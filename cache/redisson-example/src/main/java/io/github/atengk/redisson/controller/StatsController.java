package io.github.atengk.redisson.controller;

import cn.hutool.core.util.ObjectUtil;
import io.github.atengk.redisson.service.StatsService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

/**
 * 计数器测试控制器
 *
 * @author Ateng
 * @since 2026-04-11
 */
@Slf4j
@RestController
@RequestMapping("/stats")
@RequiredArgsConstructor
public class StatsController {

    private final StatsService statsService;

    /**
     * 点赞
     *
     * curl -X POST "http://localhost:8080/stats/like?postId=1"
     */
    @PostMapping("/like")
    public Object like(@RequestParam Long postId) {

        if (ObjectUtil.isEmpty(postId)) {
            return "postId不能为空";
        }

        return statsService.like(postId);
    }

    /**
     * 浏览
     *
     * curl -X POST "http://localhost:8080/stats/view?postId=1"
     */
    @PostMapping("/view")
    public Object view(@RequestParam Long postId) {

        if (ObjectUtil.isEmpty(postId)) {
            return "postId不能为空";
        }

        return statsService.view(postId);
    }

    /**
     * 获取点赞数
     *
     * curl "http://localhost:8080/stats/like/count?postId=1"
     */
    @GetMapping("/like/count")
    public Object likeCount(@RequestParam Long postId) {

        if (ObjectUtil.isEmpty(postId)) {
            return "postId不能为空";
        }

        return statsService.getLikeCount(postId);
    }

}
