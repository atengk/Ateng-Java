package io.github.atengk.interceptor.interceptor;

import io.github.atengk.interceptor.wrapper.RequestWrapper;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.web.servlet.HandlerInterceptor;

import java.util.HashMap;
import java.util.Map;

/**
 * 接口日志拦截器
 */
public class LogInterceptor implements HandlerInterceptor {

    /**
     * 线程变量：记录开始时间
     */
    private static final ThreadLocal<Long> START_TIME = new ThreadLocal<>();

    /**
     * 请求进入时
     */
    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {

        // 记录开始时间
        START_TIME.set(System.currentTimeMillis());

        return true;
    }

    /**
     * Controller执行后（但未返回视图）
     */
    @Override
    public void afterCompletion(HttpServletRequest request, HttpServletResponse response, Object handler, Exception ex) throws Exception {

        // 计算耗时
        long cost = System.currentTimeMillis() - START_TIME.get();

        // 获取请求信息
        String uri = request.getRequestURI();
        String method = request.getMethod();
        String ip = getClientIp(request);

        // 获取参数
        Map<String, Object> params = new HashMap<>();

        // GET参数
        request.getParameterMap().forEach((k, v) -> params.put(k, v));

        // POST JSON参数（需使用包装类）
        String body = "";
        if (request instanceof RequestWrapper) {
            body = ((RequestWrapper) request).getBody();
        }

        // 打印日志（实际项目建议使用 log 框架）
        System.out.println("==== 接口访问日志 ====");
        System.out.println("URI: " + uri);
        System.out.println("Method: " + method);
        System.out.println("IP: " + ip);
        System.out.println("Params: " + params);
        System.out.println("Body: " + body);
        System.out.println("耗时: " + cost + " ms");
        System.out.println("=====================");

        // 清理 ThreadLocal
        START_TIME.remove();
    }

    /**
     * 获取客户端IP
     */
    private String getClientIp(HttpServletRequest request) {

        String ip = request.getHeader("X-Forwarded-For");

        if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getHeader("X-Real-IP");
        }

        if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getRemoteAddr();
        }

        return ip;
    }
}
