package io.github.atengk.redisson.controller;

import cn.hutool.core.util.ObjectUtil;
import io.github.atengk.redisson.service.AuthService;
import io.github.atengk.redisson.service.SessionService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

/**
 * 会话测试控制器
 *
 * @author Ateng
 * @since 2026-04-11
 */
@Slf4j
@RestController
@RequestMapping("/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;

    /**
     * 登录
     * <p>
     * curl -X POST "http://localhost:8080/auth/login?userId=1"
     */
    @PostMapping("/login")
    public Object login(@RequestParam Long userId) {

        if (ObjectUtil.isEmpty(userId)) {
            return "userId不能为空";
        }

        return authService.login(userId);
    }

    /**
     * 获取当前用户
     * <p>
     * curl "http://localhost:8080/auth/me?token=xxx"
     */
    @GetMapping("/me")
    public Object me(@RequestParam String token) {

        if (ObjectUtil.isEmpty(token)) {
            return "token不能为空";
        }

        SessionService.UserSession session = authService.getCurrentUser(token);

        if (ObjectUtil.isEmpty(session)) {
            return "未登录或已过期";
        }

        return session;
    }

    /**
     * 登出
     * <p>
     * curl -X POST "http://localhost:8080/auth/logout?token=xxx"
     */
    @PostMapping("/logout")
    public Object logout(@RequestParam String token) {

        if (ObjectUtil.isEmpty(token)) {
            return "token不能为空";
        }

        authService.logout(token);

        return "登出成功";
    }

}
