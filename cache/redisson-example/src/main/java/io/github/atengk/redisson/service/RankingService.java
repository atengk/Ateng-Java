package io.github.atengk.redisson.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.redisson.api.RScoredSortedSet;
import org.redisson.api.RedissonClient;
import org.springframework.stereotype.Service;

import java.util.Collection;

/**
 * 排行榜服务
 *
 * @author Ateng
 * @since 2026-04-11
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class RankingService {

    private final RedissonClient redissonClient;

    private static final String RANK_KEY = "rank:score";

    /**
     * 增加分数
     *
     * @param userId 用户ID
     * @param score 分数
     */
    public void addScore(Long userId, double score) {

        RScoredSortedSet<Long> zSet = redissonClient.getScoredSortedSet(RANK_KEY);

        zSet.addScore(userId, score);

        log.info("增加分数成功，userId={}，score={}", userId, score);
    }

    /**
     * 获取用户排名（从1开始）
     *
     * @param userId 用户ID
     * @return 排名
     */
    public Integer getRank(Long userId) {

        RScoredSortedSet<Long> zSet = redissonClient.getScoredSortedSet(RANK_KEY);

        Integer rank = zSet.revRank(userId);

        if (rank == null) {
            return null;
        }

        return rank + 1;
    }

    /**
     * 获取 TopN 用户
     *
     * @param n 数量
     * @return 用户列表
     */
    public Collection<Long> topN(int n) {

        RScoredSortedSet<Long> zSet = redissonClient.getScoredSortedSet(RANK_KEY);

        return zSet.valueRangeReversed(0, n - 1);
    }

}
