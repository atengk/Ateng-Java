package local.ateng.java.log.filter;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.MDC;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.UUID;

/**
 * 使用MDC生成TRACE_ID
 * 在每个请求的开始处（例如，Filter中），你可以使用MDC.put方法生成并设置TRACE_ID。通常TRACE_ID可以从请求头中获取，或者如果没有可以自己生成一个。
 *
 * @author 孔余
 * @email 2385569970@qq.com
 * @since 2025-02-16
 */
public class TraceIdFilter extends OncePerRequestFilter {

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain) throws ServletException, IOException {
        // 从请求头中获取TRACE_ID，如果没有则生成一个
        String traceId = request.getHeader("TRACE_ID");
        if (traceId == null) {
            traceId = UUID.randomUUID().toString();
        }

        // 将TRACE_ID放入MDC
        MDC.put("traceId", traceId);

        try {
            // 继续处理请求
            filterChain.doFilter(request, response);
        } finally {
            // 请求结束后清理MDC，避免内存泄漏
            MDC.remove("traceId");
        }
    }
}
