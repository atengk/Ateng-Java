package io.github.atengk.redisson.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

/**
 * 用户查询示例
 *
 * @author Ateng
 * @since 2026-04-11
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class UserQueryService {

    private final BloomFilterService bloomFilterService;

    /**
     * 查询用户
     *
     * @param userId 用户ID
     * @return 用户信息
     */
    public String query(Long userId) {

        if (!bloomFilterService.contains(userId)) {
            log.warn("布隆过滤器拦截，userId={}", userId);
            return "用户不存在";
        }

        log.info("查询数据库，userId={}", userId);

        return "用户信息：" + userId;
    }

}