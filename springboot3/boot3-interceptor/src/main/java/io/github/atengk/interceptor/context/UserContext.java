package io.github.atengk.interceptor.context;


/**
 * 用户上下文（ThreadLocal实现）
 */
public class UserContext {

    /**
     * 用户ID
     */
    private static final ThreadLocal<Long> USER_ID = new ThreadLocal<>();

    /**
     * 用户名
     */
    private static final ThreadLocal<String> USERNAME = new ThreadLocal<>();
    public static void set(Long userId) {
        USER_ID.set(userId);
    }
    /**
     * 设置用户信息
     */
    public static void set(Long userId, String username) {
        USER_ID.set(userId);
        USERNAME.set(username);
    }

    /**
     * 获取用户ID
     */
    public static Long getUserId() {
        return USER_ID.get();
    }

    /**
     * 获取用户名
     */
    public static String getUsername() {
        return USERNAME.get();
    }

    public static String getRole() {
        return USERNAME.get();
    }

    /**
     * 清理（必须）
     */
    public static void clear() {
        USER_ID.remove();
        USERNAME.remove();
    }
}