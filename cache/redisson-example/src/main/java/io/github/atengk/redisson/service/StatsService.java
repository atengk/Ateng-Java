package io.github.atengk.redisson.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

/**
 * 统计业务示例
 *
 * @author Ateng
 * @since 2026-04-11
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class StatsService {

    private final CounterService counterService;

    /**
     * 点赞
     *
     * @param postId 帖子ID
     * @return 当前点赞数
     */
    public long like(Long postId) {

        String key = "like:post:" + postId;

        return counterService.increment(key);
    }

    /**
     * 浏览
     *
     * @param postId 帖子ID
     * @return 当前浏览量
     */
    public long view(Long postId) {

        String key = "view:post:" + postId;

        return counterService.increment(key);
    }

    /**
     * 获取点赞数
     */
    public long getLikeCount(Long postId) {
        return counterService.get("like:post:" + postId);
    }

}
