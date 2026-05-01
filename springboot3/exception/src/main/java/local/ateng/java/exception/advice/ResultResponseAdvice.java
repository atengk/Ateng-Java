package local.ateng.java.exception.advice;

import jakarta.servlet.http.HttpServletRequest;
import local.ateng.java.exception.utils.Result;
import org.springframework.core.MethodParameter;
import org.springframework.http.MediaType;
import org.springframework.http.converter.HttpMessageConverter;
import org.springframework.http.server.ServerHttpRequest;
import org.springframework.http.server.ServerHttpResponse;
import org.springframework.http.server.ServletServerHttpRequest;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.servlet.mvc.method.annotation.ResponseBodyAdvice;

/**
 * Result 响应增强处理器。
 * <p>
 * 用于在 Controller 响应写出前，统一补充当前请求路径，避免业务接口手动调用 withPath。
 * </p>
 *
 * @author Ateng
 * @since 2026-05-01
 */
@RestControllerAdvice
public class ResultResponseAdvice implements ResponseBodyAdvice<Object> {

    /**
     * 判断是否需要处理响应体。
     *
     * @param returnType    控制器方法返回类型
     * @param converterType 消息转换器类型
     * @return 返回 true 表示进入 beforeBodyWrite
     */
    @Override
    public boolean supports(MethodParameter returnType,
                            Class<? extends HttpMessageConverter<?>> converterType) {
        return true;
    }

    /**
     * 在响应写出前补充 Result 的请求路径。
     *
     * @param body                  原始响应体
     * @param returnType            控制器方法返回类型
     * @param selectedContentType   响应内容类型
     * @param selectedConverterType 消息转换器类型
     * @param request               当前请求
     * @param response              当前响应
     * @return 处理后的响应体
     */
    @Override
    public Object beforeBodyWrite(Object body,
                                  MethodParameter returnType,
                                  MediaType selectedContentType,
                                  Class<? extends HttpMessageConverter<?>> selectedConverterType,
                                  ServerHttpRequest request,
                                  ServerHttpResponse response) {
        if (!(body instanceof Result<?> result)) {
            return body;
        }

        if (result.getPath() != null && !result.getPath().isBlank()) {
            return body;
        }

        String path = resolvePath(request);
        return result.withPath(path);
    }

    /**
     * 解析当前请求路径。
     *
     * @param request 当前请求
     * @return 请求路径
     */
    private String resolvePath(ServerHttpRequest request) {
        if (request instanceof ServletServerHttpRequest servletRequest) {
            HttpServletRequest httpServletRequest = servletRequest.getServletRequest();
            return httpServletRequest.getRequestURI();
        }
        return request.getURI().getPath();
    }

}