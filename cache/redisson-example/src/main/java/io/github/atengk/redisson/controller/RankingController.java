package io.github.atengk.redisson.controller;

import cn.hutool.core.util.ObjectUtil;
import io.github.atengk.redisson.service.RankingService;
import io.github.atengk.redisson.service.ScoreService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import java.util.Collection;

/**
 * 排行榜测试控制器
 *
 * @author Ateng
 * @since 2026-04-11
 */
@Slf4j
@RestController
@RequestMapping("/rank")
@RequiredArgsConstructor
public class RankingController {

    private final ScoreService scoreService;
    private final RankingService rankingService;

    /**
     * 增加积分
     *
     * curl -X POST "http://localhost:8080/rank/add?userId=1&score=10"
     */
    @PostMapping("/add")
    public String add(@RequestParam Long userId,
                      @RequestParam Double score) {

        if (ObjectUtil.hasEmpty(userId, score)) {
            return "参数不能为空";
        }

        scoreService.addScore(userId, score);

        return "操作成功";
    }

    /**
     * 查询排名
     *
     * curl "http://localhost:8080/rank/get?userId=1"
     */
    @GetMapping("/get")
    public Object get(@RequestParam Long userId) {

        if (ObjectUtil.isEmpty(userId)) {
            return "userId不能为空";
        }

        return scoreService.getRank(userId);
    }

    /**
     * 获取TopN
     *
     * curl "http://localhost:8080/rank/top?n=5"
     */
    @GetMapping("/top")
    public Object top(@RequestParam Integer n) {

        if (ObjectUtil.isEmpty(n)) {
            return "n不能为空";
        }

        Collection<Long> result = rankingService.topN(n);

        return result;
    }

}
