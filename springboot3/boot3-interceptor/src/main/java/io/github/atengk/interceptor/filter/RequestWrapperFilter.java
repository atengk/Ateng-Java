package io.github.atengk.interceptor.filter;

import io.github.atengk.interceptor.wrapper.RequestWrapper;
import jakarta.servlet.*;
import jakarta.servlet.http.HttpServletRequest;

import java.io.IOException;

/**
 * 请求包装过滤器
 */
public class RequestWrapperFilter implements Filter {

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
            throws IOException, ServletException {

        if (request instanceof HttpServletRequest) {
            RequestWrapper wrapper = new RequestWrapper((HttpServletRequest) request);

            // 使用包装后的 request
            chain.doFilter(wrapper, response);
        } else {
            chain.doFilter(request, response);
        }
    }
}
