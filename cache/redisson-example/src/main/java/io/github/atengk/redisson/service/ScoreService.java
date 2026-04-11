package io.github.atengk.redisson.service;


import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

/**
 * 积分业务示例
 *
 * @author Ateng
 * @since 2026-04-11
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ScoreService {

    private final RankingService rankingService;

    /**
     * 增加积分
     *
     * @param userId 用户ID
     * @param score 分数
     */
    public void addScore(Long userId, double score) {

        rankingService.addScore(userId, score);

    }

    /**
     * 查询排名
     *
     * @param userId 用户ID
     * @return 排名
     */
    public Integer getRank(Long userId) {

        return rankingService.getRank(userId);

    }

}