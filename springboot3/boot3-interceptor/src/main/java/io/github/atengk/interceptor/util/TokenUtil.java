package io.github.atengk.interceptor.util;

/**
 * Token 工具类（示例用，实际项目建议使用 JWT）
 */
public class TokenUtil {

    /**
     * 模拟解析 Token
     * 规则：token = userId（简单演示）
     */
    public static Long parseToken(String token) {
        try {
            return Long.parseLong(token);
        } catch (Exception e) {
            return null;
        }
    }
    public static Long getUserId(String token) {
        try {
            return Long.parseLong(token.split(":")[0]);
        } catch (Exception e) {
            return null;
        }
    }


    public static String getUsername(String token) {
        try {
            return token.split(":")[1];
        } catch (Exception e) {
            return null;
        }
    }

    public static String getRole(String token) {
        try {
            return token.split(":")[1];
        } catch (Exception e) {
            return null;
        }
    }
}
