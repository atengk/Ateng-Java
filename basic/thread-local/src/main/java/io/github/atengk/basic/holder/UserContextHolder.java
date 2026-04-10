package io.github.atengk.basic.holder;

import cn.hutool.core.util.ObjectUtil;
import io.github.atengk.basic.model.User;

/**
 * 用户上下文工具类（基于 ThreadLocal 实现）
 *
 * @author Ateng
 * @since 2026-04-10
 */
public class UserContextHolder {

    /**
     * 使用 ThreadLocal 存储当前线程的用户信息
     */
    private static final ThreadLocal<User> USER_THREAD_LOCAL = new ThreadLocal<>();

    /**
     * 设置当前登录用户
     *
     * @param user 用户信息
     */
    public static void set(User user) {
        if (ObjectUtil.isNotNull(user)) {
            USER_THREAD_LOCAL.set(user);
        }
    }

    /**
     * 获取当前登录用户
     *
     * @return 用户信息
     */
    public static User get() {
        return USER_THREAD_LOCAL.get();
    }

    /**
     * 获取当前用户ID（快捷方法）
     *
     * @return 用户ID
     */
    public static Long getUserId() {
        User user = get();
        return ObjectUtil.isNotNull(user) ? user.getId() : null;
    }

    /**
     * 清除当前线程中的用户信息（必须调用，防止内存泄漏）
     */
    public static void clear() {
        USER_THREAD_LOCAL.remove();
    }
}
