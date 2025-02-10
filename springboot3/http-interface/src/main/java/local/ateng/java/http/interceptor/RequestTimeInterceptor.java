package local.ateng.java.http.interceptor;


import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;
import org.springframework.web.servlet.ModelAndView;

@Component
public class RequestTimeInterceptor implements HandlerInterceptor {

    // 在请求处理之前调用，记录开始时间
    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        // 记录请求开始时间
        request.setAttribute("startTime", System.currentTimeMillis());
        return true;  // 继续执行下一个拦截器或者处理器
    }

    // 在请求处理之后调用，用于记录耗时
    @Override
    public void postHandle(HttpServletRequest request, HttpServletResponse response, Object handler,
                           ModelAndView modelAndView) throws Exception {
        // 计算请求耗时
        long startTime = (Long) request.getAttribute("startTime");
        long endTime = System.currentTimeMillis();
        long duration = endTime - startTime;

        // 打印日志
        System.out.println("Request URL: " + request.getRequestURL() + " | Duration: " + duration + " ms");
    }

    // 在请求处理完成之后调用
    @Override
    public void afterCompletion(HttpServletRequest request, HttpServletResponse response, Object handler,
                                Exception ex) throws Exception {
        // 可以做一些资源清理等工作
        request.removeAttribute("startTime");
    }
}
