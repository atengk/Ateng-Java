package io.github.atengk.xss.config;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.core.util.URLUtil;
import jakarta.servlet.*;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.util.AntPathMatcher;

import java.io.IOException;

/**
 * XSS 过滤器（企业级）
 * <p>
 * 特性：
 * 1. 支持 * 和 **
 * 2. 支持 context-path
 * 3. URI 标准化（防绕过）
 * 4. Pattern 预编译（提升性能）
 *
 * @author 孔余
 * @since 2026-04-05
 */
@Component
@Order(Ordered.HIGHEST_PRECEDENCE)
public class XssFilter implements Filter {

    private final XssProperties properties;

    private final AntPathMatcher pathMatcher = new AntPathMatcher();

    public XssFilter(XssProperties properties) {
        this.properties = properties;
    }

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
            throws IOException, ServletException {

        if (!properties.isEnabled()) {
            chain.doFilter(request, response);
            return;
        }

        HttpServletRequest req = (HttpServletRequest) request;

        /*
         * 获取标准化 URI（去除 context-path + 解码）
         */
        String uri = normalizeUri(req);

        /*
         * 忽略路径
         */
        if (isExclude(uri)) {
            chain.doFilter(request, response);
            return;
        }

        /*
         * 包装请求
         */
        XssHttpServletRequestWrapper wrapper =
                new XssHttpServletRequestWrapper(req);

        chain.doFilter(wrapper, response);
    }

    /**
     * URI 标准化（企业级关键点）
     */
    private String normalizeUri(HttpServletRequest request) {

        String uri = request.getRequestURI();

        /*
         * 去掉 context-path
         */
        String contextPath = request.getContextPath();
        if (StrUtil.isNotBlank(contextPath) && uri.startsWith(contextPath)) {
            uri = uri.substring(contextPath.length());
        }

        /*
         * URL 解码（防止 %2F 绕过）
         */
        uri = URLUtil.decode(uri, "UTF-8");

        /*
         * 统一格式
         */
        uri = StrUtil.removeSuffix(uri, "/");

        return uri;
    }

    /**
     * 是否忽略
     */
    private boolean isExclude(String uri) {

        if (CollUtil.isEmpty(properties.getExcludePaths())) {
            return false;
        }

        for (String pattern : properties.getExcludePaths()) {
            if (pathMatcher.match(pattern, uri)) {
                return true;
            }
        }

        return false;
    }
}