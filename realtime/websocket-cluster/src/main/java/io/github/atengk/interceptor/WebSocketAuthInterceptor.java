package io.github.atengk.interceptor;

import cn.hutool.core.net.url.UrlBuilder;
import cn.hutool.core.net.url.UrlQuery;
import cn.hutool.core.util.CharsetUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.server.ServerHttpRequest;
import org.springframework.http.server.ServerHttpResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.WebSocketHandler;
import org.springframework.web.socket.server.HandshakeInterceptor;

import java.net.URI;
import java.util.Map;

/**
 * WebSocket 握手阶段鉴权拦截器
 *
 * <p>
 * 在 WebSocket 握手建立之前执行，用于从请求参数中解析 token，
 * 并完成用户身份校验，将用户信息存入 WebSocket Session attributes。
 * </p>
 *
 * @author 孔余
 * @since 2026-01-30
 */
@Slf4j
@Component
public class WebSocketAuthInterceptor implements HandshakeInterceptor {

    /**
     * WebSocket Session 中存储用户ID的属性名
     */
    public static final String ATTR_USER_ID = "USER_ID";

    /**
     * WebSocket 握手前置处理
     *
     * <p>
     * 用于从请求 URI 中解析 token，并校验 token 的合法性。
     * 校验通过后，将用户ID写入 attributes，供后续 WebSocketHandler 使用。
     * </p>
     *
     * @param request    当前 HTTP 请求
     * @param response   当前 HTTP 响应
     * @param handler    WebSocket 处理器
     * @param attributes WebSocket Session 属性集合
     * @return true 表示允许握手，false 表示拒绝握手
     */
    @Override
    public boolean beforeHandshake(ServerHttpRequest request,
                                   ServerHttpResponse response,
                                   WebSocketHandler handler,
                                   Map<String, Object> attributes) {

        // 获取请求 URI
        URI uri = request.getURI();

        // 使用 Hutool 解析并自动解码查询参数
        UrlQuery query = UrlBuilder.of(uri, CharsetUtil.CHARSET_UTF_8).getQuery();

        // 获取 token 参数
        String token = null;
        if (query != null) {
            CharSequence value = query.get("token");
            if (value != null) {
                token = value.toString();
            }
        }

        // token 为空，直接拒绝握手
        if (token == null || token.isBlank()) {
            log.warn("WebSocket 握手失败，token 缺失");
            return false;
        }

        // 根据 token 解析用户ID
        String userId = parseUserIdFromToken(token);
        if (userId == null) {
            log.warn("WebSocket 握手失败，token 无效，token：{}", token);
            return false;
        }

        // 将用户ID存入 WebSocket Session 属性
        attributes.put(ATTR_USER_ID, userId);

        log.info("WebSocket 握手鉴权成功，用户ID：{}", userId);
        return true;
    }

    /**
     * WebSocket 握手完成后的回调
     *
     * <p>
     * 当前未做额外处理，预留扩展。
     * </p>
     *
     * @param request  当前 HTTP 请求
     * @param response 当前 HTTP 响应
     * @param handler  WebSocket 处理器
     * @param ex       握手异常（如果有）
     */
    @Override
    public void afterHandshake(ServerHttpRequest request,
                               ServerHttpResponse response,
                               WebSocketHandler handler,
                               Exception ex) {
    }

    /**
     * 根据 token 解析用户ID
     *
     * <p>
     * 示例实现：通过固定 token 映射用户ID，
     * 实际项目中可替换为 JWT 校验或统一认证中心校验。
     * </p>
     *
     * @param token 客户端传入的 token
     * @return 用户ID，解析失败返回 null
     */
    private String parseUserIdFromToken(String token) {
        if ("Admin@123".equals(token)) {
            return "10001";
        }
        return null;
    }
}