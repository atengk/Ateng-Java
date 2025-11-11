package io.github.atengk.restclient.interceptor;

import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpRequest;
import org.springframework.http.client.ClientHttpRequestExecution;
import org.springframework.http.client.ClientHttpRequestInterceptor;
import org.springframework.http.client.ClientHttpResponse;
import org.springframework.stereotype.Component;

import java.io.IOException;

/**
 * RestClient 拦截器：用于添加统一的认证 Token（如 JWT）到请求头中。
 * <p>
 * 注意：此实现为模拟 Token 获取逻辑，实际生产中应接入真实的缓存（如 Redis）或配置中心。
 *
 * @author 孔余
 * @since 2025-07-30
 */
@Component
@Slf4j
public class AuthInterceptor implements ClientHttpRequestInterceptor {

    /**
     * 拦截请求，添加 Authorization 头。
     *
     * @param request   原始请求
     * @param body      请求体字节数组
     * @param execution 请求执行器（用于继续调用链）
     * @return 响应结果
     * @throws IOException IO 异常
     */
    @Override
    public ClientHttpResponse intercept(HttpRequest request, byte[] body, ClientHttpRequestExecution execution)
            throws IOException {

        // 获取 Token（可替换为从 Redis、配置中心或上下文中获取）
        String token = getToken();

        // 日志记录请求 URI 和添加 token 的行为（仅开发调试阶段启用）
        log.debug("Adding Authorization token to request: {}", request.getURI());

        // 若 token 为空，可选择记录日志或抛出异常（视业务场景而定）
        if (token == null || token.isEmpty()) {
            log.warn("Authorization token is missing.");
            // 你也可以选择抛出自定义异常
            // throw new IllegalStateException("Missing authorization token");
        }

        // 设置请求头
        HttpHeaders headers = request.getHeaders();
        headers.set(HttpHeaders.AUTHORIZATION, "Bearer " + token);

        // 执行请求
        return execution.execute(request, body);
    }

    /**
     * 获取 Token 的方法（可扩展为从缓存或配置服务获取）
     *
     * @return JWT Token 字符串
     */
    private String getToken() {
        // 模拟：返回一个硬编码 Token（请替换为实际获取逻辑）
        return "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...";
    }
}
