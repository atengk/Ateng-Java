package io.github.atengk.redisson.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

/**
 * 访问控制示例
 *
 * @author Ateng
 * @since 2026-04-11
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AccessService {

    private final BlackWhiteListService blackWhiteListService;

    /**
     * 校验访问
     *
     * @param ip IP地址
     * @return 是否允许
     */
    public String check(String ip) {

        if (blackWhiteListService.isBlack(ip)) {
            log.warn("黑名单拦截，ip={}", ip);
            return "访问被拒绝（黑名单）";
        }

        if (blackWhiteListService.isWhite(ip)) {
            log.info("白名单放行，ip={}", ip);
            return "访问通过（白名单）";
        }

        log.info("普通访问，ip={}", ip);

        return "访问通过";
    }

}