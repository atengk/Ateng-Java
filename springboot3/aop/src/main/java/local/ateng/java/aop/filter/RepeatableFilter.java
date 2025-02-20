package local.ateng.java.aop.filter;

import cn.hutool.core.io.IoUtil;
import jakarta.servlet.*;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletRequestWrapper;

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStreamReader;

/**
 * 配置过滤器，可以重复读取请求体
 *
 * @author 孔余
 * @email 2385569970@qq.com
 * @since 2025-02-20
 */
public class RepeatableFilter implements Filter {

    @Override
    public void init(FilterConfig filterConfig) throws ServletException {
        // 过滤器初始化，通常可以做一些初始化操作，比如设置日志记录器等
    }

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
            throws IOException, ServletException {

        // 将缓存请求体存储到 request 属性中，后续可以访问
        ServletRequest requestWrapper = null;
        if (request instanceof HttpServletRequest) {
            requestWrapper = new RepeatedlyRequestWrapper((HttpServletRequest) request, response);
        }

        // 继续执行过滤链
        if (null == requestWrapper) {
            chain.doFilter(request, response);
        } else {
            chain.doFilter(requestWrapper, response);
        }

    }

    @Override
    public void destroy() {
        // 过滤器销毁时执行清理工作，这里没有需要销毁的资源
    }

    /**
     * 自定义 HttpServletRequestWrapper，用于存储缓存的请求体
     */
    public static class RepeatedlyRequestWrapper extends HttpServletRequestWrapper {
        private final byte[] body;

        public RepeatedlyRequestWrapper(HttpServletRequest request, ServletResponse response) throws IOException {
            super(request);
            request.setCharacterEncoding("UTF-8");
            response.setCharacterEncoding("UTF-8");

            body = IoUtil.readBytes(request.getInputStream(), false);
        }

        @Override
        public BufferedReader getReader() throws IOException {
            return new BufferedReader(new InputStreamReader(getInputStream()));
        }

        @Override
        public ServletInputStream getInputStream() throws IOException {
            final ByteArrayInputStream bais = new ByteArrayInputStream(body);
            return new ServletInputStream() {
                @Override
                public int read() throws IOException {
                    return bais.read();
                }

                @Override
                public int available() throws IOException {
                    return body.length;
                }

                @Override
                public boolean isFinished() {
                    return false;
                }

                @Override
                public boolean isReady() {
                    return false;
                }

                @Override
                public void setReadListener(ReadListener readListener) {

                }
            };
        }
    }
}

