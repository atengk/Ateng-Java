package io.github.atengk.redisson.service;

import cn.hutool.core.lang.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

/**
 * 登录业务示例
 *
 * @author Ateng
 * @since 2026-04-11
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AuthService {

    private final SessionService sessionService;

    /**
     * 登录
     *
     * @param userId 用户ID
     * @return token
     */
    public String login(Long userId) {

        String token = UUID.fastUUID().toString(true);

        SessionService.UserSession session = new SessionService.UserSession();
        session.setUserId(userId);
        session.setUsername("user_" + userId);

        sessionService.createSession(token, session, 1800);

        log.info("用户登录成功，userId={}，token={}", userId, token);

        return token;
    }

    /**
     * 获取当前用户
     *
     * @param token token
     * @return 用户信息
     */
    public SessionService.UserSession getCurrentUser(String token) {

        return sessionService.getSession(token);
    }

    /**
     * 登出
     *
     * @param token token
     */
    public void logout(String token) {

        sessionService.deleteSession(token);

        log.info("用户登出成功，token={}", token);
    }

}
