package io.github.atengk.redisson.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

/**
 * 签到业务示例
 *
 * @author Ateng
 * @since 2026-04-11
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class UserSignService {

    private final SignService signService;

    /**
     * 用户签到
     */
    public void sign(Long userId) {
        signService.sign(userId);
    }

    /**
     * 查询是否签到
     */
    public boolean isSigned(Long userId) {
        return signService.isSigned(userId);
    }

    /**
     * 查询签到总天数
     */
    public long count(Long userId) {
        return signService.getSignCount(userId);
    }

    /**
     * 查询连续签到
     */
    public int continuous(Long userId) {
        return signService.getContinuousSignCount(userId);
    }

}
