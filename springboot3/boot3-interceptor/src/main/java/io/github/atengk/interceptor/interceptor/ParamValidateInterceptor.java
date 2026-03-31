package io.github.atengk.interceptor.interceptor;

import io.github.atengk.interceptor.annotation.ValidateParam;
import io.github.atengk.interceptor.util.ValidateUtil;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.core.MethodParameter;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.servlet.HandlerInterceptor;

/**
 * 参数校验拦截器
 */
public class ParamValidateInterceptor implements HandlerInterceptor {

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {

        if (!(handler instanceof HandlerMethod)) {
            return true;
        }

        HandlerMethod method = (HandlerMethod) handler;

        // 是否开启校验
        ValidateParam validateParam = method.getMethodAnnotation(ValidateParam.class);
        if (validateParam == null) {
            return true;
        }

        // 获取方法参数
        MethodParameter[] parameters = method.getMethodParameters();

        for (MethodParameter parameter : parameters) {

            // 从 request attribute 中取参数（Spring 会放入）
            Object arg = request.getAttribute(parameter.getParameterName());

            if (arg != null) {
                ValidateUtil.validate(arg);
            }
        }

        return true;
    }
}
