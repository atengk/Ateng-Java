package io.github.atengk.basic.context;

import java.util.List;

/**
 * 用户登录上下文
 *
 * 用于承载当前登录用户的基础信息、角色、权限
 *
 * @author Ateng
 * @since 2026-04-10
 */
public final class UserContext {

    /**
     * 当前登录用户信息
     */
    public static final ScopedValue<UserInfo> USER = ScopedValue.newInstance();

    private UserContext() {
    }

    /**
     * 获取当前用户
     *
     * @return UserInfo
     */
    public static UserInfo getUser() {
        return USER.isBound() ? USER.get() : null;
    }

    /**
     * 用户信息对象
     */
    public record UserInfo(
            String userId,
            String username,
            List<String> roles,
            List<String> permissions
    ) {
    }
}
