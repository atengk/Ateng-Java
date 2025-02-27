package local.ateng.java.auth.utils;

import cn.hutool.core.convert.Convert;
import cn.hutool.core.convert.ConvertException;
import cn.hutool.core.date.DateTime;
import cn.hutool.core.date.DateUtil;
import cn.hutool.extra.spring.SpringUtil;
import cn.hutool.jwt.JWT;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import local.ateng.java.auth.vo.SysUserVo;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.MediaType;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

import java.io.IOException;
import java.io.PrintWriter;
import java.util.Arrays;
import java.util.Optional;

/**
 * SpringSecurity 工具类
 * 提供对认证信息获取、JWT 创建与验证的操作
 *
 * @author 孔余
 * @since 2025-02-26
 */
public class SecurityUtils {

    private static final Logger log = LoggerFactory.getLogger(SecurityUtils.class);
    // token 名称
    private static String TOKEN_NAME = SpringUtil.getProperty("jwt.token-name", "ateng-token");
    // 从配置文件中注入密钥和过期时间（单位：小时）
    // 默认密钥为 "Admin@123"
    private static String SECRET_KEY = SpringUtil.getProperty("jwt.secret-key", "Admin@123");
    // 默认过期时间为 24 小时
    private static int EXPIRATION_TIME = Integer.parseInt(SpringUtil.getProperty("jwt.expiration", "24"));

    /**
     * 获取当前认证用户的详细信息
     *
     * @return 认证信息中的用户详细数据
     */
    public static SysUserVo getAuthenticatedUserDetails() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        return (SysUserVo) authentication.getDetails();
    }

    /**
     * 设置当前认证信息
     *
     * @param authentication 当前认证信息
     */
    public static void setAuthenticatedUser(Authentication authentication) {
        SecurityContextHolder.getContext().setAuthentication(authentication);
    }

    /**
     * 创建 JWT Token
     *
     * @param user 用户信息对象
     * @return 生成的 JWT Token
     */
    public static String generateJwtToken(SysUserVo user) {
        // 当前时间
        DateTime currentTime = DateUtil.date();
        // 计算 Token 的过期时间
        DateTime expirationTime = DateUtil.offsetHour(currentTime, SecurityUtils.EXPIRATION_TIME);

        return JWT.create()
                .setKey(SECRET_KEY.getBytes())  // 设置密钥
                .setPayload("user", user)      // 设置用户信息作为载荷
                .setNotBefore(currentTime)     // 设置 Token 可用时间
                .setExpiresAt(expirationTime) // 设置过期时间
                .sign();                      // 签名生成 Token
    }

    /**
     * 解析 JWT Token
     *
     * @param token JWT Token
     * @return 解析出的用户信息
     */
    public static SysUserVo parseJwtToken(String token) {
        SysUserVo userVo = null;
        try {
            JWT jwt = JWT.of(token);
            userVo = Convert.convert(SysUserVo.class, jwt.getPayload("user"));
        } catch (ConvertException e) {
            log.error("token={}，解析错误：{}", token, e.getMessage());
            throw new RuntimeException("token解析错误");
        }
        return userVo;
    }

    /**
     * 验证 JWT Token 的签名是否有效
     *
     * @param token JWT Token
     * @return 是否有效
     */
    public static Boolean verifyJwtToken(String token) {
        Boolean result = false;
        try {
            result = JWT.of(token).setKey(SECRET_KEY.getBytes()).verify();
        } catch (Exception e) {
            log.error("token={}，效验错误：{}", token, e.getMessage());
            throw new RuntimeException("token效验错误");
        }
        return result;
    }

    /**
     * 从请求中获取指定名称的 token。优先级为：
     * 1. 从 "Authorization" 头部获取 Bearer token；
     * 2. 从指定的 header 中获取；
     * 3. 如果指定的 header 中没有，从请求参数中获取；
     * 4. 如果以上都没有，则从 cookies 中获取指定名称的 token。
     *
     * @param request HttpServletRequest 对象，包含请求信息
     * @return 返回 token 的值，如果没有找到对应的 token 则返回 null
     */
    public static String getToken(HttpServletRequest request) {
        // Step 1: 尝试从 "Authorization" 头部获取 Bearer token
        String token = request.getHeader("Authorization");
        if (token != null && !token.isBlank() && token.startsWith("Bearer ")) {
            // 提取 Bearer 后的 token
            token = token.substring(7);
        }

        // Step 2: 如果 token 为空，从指定的 header 中获取 token
        if (token == null || token.isBlank()) {
            token = request.getHeader(TOKEN_NAME);
        }

        // Step 3: 如果 header 中没有，尝试从请求参数中获取 token
        if (token == null || token.isBlank()) {
            token = request.getParameter(TOKEN_NAME);
        }

        // Step 4: 如果请求参数中没有，从 cookies 中获取指定名称的 token
        if (token == null || token.isBlank()) {
            Cookie[] cookies = request.getCookies();
            if (cookies != null) {
                // 使用流式操作从 cookies 中查找指定名称的 token
                token = Optional.ofNullable(cookies)
                        .stream()
                        .flatMap(Arrays::stream) // 将 Cookie[] 转换成流
                        .filter(cookie -> TOKEN_NAME.equals(cookie.getName())) // 筛选出符合名称的 cookie
                        .map(Cookie::getValue) // 提取 cookie 的值
                        .findFirst() // 返回第一个匹配的 token 值
                        .orElse(null); // 如果未找到，返回 null
            }
        }

        // 返回最终获取到的 token 或 null
        return token;
    }


    /**
     * 发送统一的 JSON 格式响应。
     *
     * @param response   HttpServletResponse 对象，用于向客户端发送响应
     * @param statusCode 响应的 HTTP 状态码
     * @param code       错误或状态码
     * @param msg        错误或状态信息
     * @throws IOException 如果写入响应时发生 I/O 错误
     */
    public static void sendResponse(HttpServletResponse response, int statusCode, String code, String msg) {
        // 设置响应头
        response.setStatus(statusCode);
        response.setCharacterEncoding("UTF-8");
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);

        // 使用 try-with-resources 自动关闭 PrintWriter
        try (PrintWriter printWriter = response.getWriter()) {
            // 构造返回的结果并写入响应
            printWriter.write(Result.error(code, msg).toString());
            printWriter.flush();  // 确保内容已发送到客户端
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * 将 token 写入到响应的 Cookie 中，自动判断是否使用 Secure 属性，并设置 HttpOnly 为 false。
     *
     * @param request  HttpServletRequest 对象，用于检查是否通过 HTTPS
     * @param response HttpServletResponse 对象
     * @param name     Cookie的名称
     * @param value    Cookie的值
     * @param expiry   Cookie的有效期
     * @param path     Cookie 的路径
     * @param httpOnly 设置 HttpOnly
     */
    public static void addTokenCookie(HttpServletRequest request, HttpServletResponse response, String name, String value, int expiry, String path, boolean httpOnly) {
        // 创建Cookie，并将其值设置为传入的 token 值
        Cookie cookie = new Cookie(name, value);

        // 设置 Cookie 的有效期
        cookie.setMaxAge(expiry);  // 单位：秒。如果为负值，表示会话期间有效，默认为-1
        cookie.setPath(path);  // 设置 Cookie 的路径，"/" 表示对所有路径有效

        // 自动判断是否使用 HTTPS 协议
        boolean isSecure = request.isSecure();  // 判断请求是否使用 HTTPS
        cookie.setSecure(isSecure);  // 如果是 HTTPS，则设置 Secure 为 true

        // 明确设置 HttpOnly 为 false，允许 JavaScript 访问
        cookie.setHttpOnly(httpOnly);  // 允许 JavaScript 访问 Cookie

        // 设置 domain 属性
        cookie.setDomain(request.getServerName());

        // 将 Cookie 添加到响应中
        response.addCookie(cookie);
    }

}
