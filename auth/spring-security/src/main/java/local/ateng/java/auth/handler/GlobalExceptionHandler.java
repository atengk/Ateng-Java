package local.ateng.java.auth.handler;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import local.ateng.java.auth.constant.AppCodeEnum;
import local.ateng.java.auth.exception.ServiceException;
import local.ateng.java.auth.utils.Result;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.security.authorization.AuthorizationDeniedException;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.servlet.NoHandlerFoundException;
import org.springframework.web.servlet.resource.NoResourceFoundException;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.HashMap;

/**
 * 全局异常处理器
 *
 * @author 孔余
 * @email 2385569970@qq.com
 * @since 2025-01-09
 */
@RestControllerAdvice
@Slf4j
public class GlobalExceptionHandler {

    // 处理业务的异常
    @ExceptionHandler(ServiceException.class)
    public final Result handleServiceException(HttpServletRequest request, HttpServletResponse response, ServiceException ex) {
        String message = ex.getMessage();
        String code = ex.getCode();
        HashMap<String, String> data = new HashMap<>();
        data.put("detailMessage", ex.getDetailMessage());
        // 打印异常日志
        log.error("处理业务的异常 ==> {}", message);
        // 设置状态码
        ex.printStackTrace();
        response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
        // 构建返回结果
        return Result.error(code, message).setData(data);
    }

    // 处理其他的异常
    @ExceptionHandler(Exception.class)
    public final Result handleAllExceptions(HttpServletRequest request, HttpServletResponse response, Exception ex) {
        // 定义异常码和消息
        String message;
        // 分批处理异常类型
        if (ex instanceof HttpRequestMethodNotSupportedException) {
            message = "请求方式错误";
        } else if (ex instanceof NoHandlerFoundException || ex instanceof NoResourceFoundException || ex instanceof HttpMessageNotReadableException) {
            message = "资源未找到";
        } else if (ex instanceof MissingServletRequestParameterException) {
            message = "请求参数缺失";
        } else if (ex instanceof IllegalArgumentException) {
            message = "非法参数异常";
        } else if (ex instanceof ClassCastException) {
            message = "类型转换错误";
        } else if (ex instanceof ArithmeticException) {
            message = "数据计算异常";
        } else if (ex instanceof IndexOutOfBoundsException) {
            message = "数组越界异常";
        } else if (ex instanceof FileNotFoundException || ex instanceof IOException) {
            message = "文件操作异常";
        } else if (ex instanceof NullPointerException) {
            message = "空指针异常";
        } else if (ex instanceof MethodArgumentTypeMismatchException || ex instanceof NumberFormatException) {
            message = "数据类型不匹配异常";
        } else if (ex instanceof UnsupportedOperationException) {
            message = "不支持的操作异常";
        } else {
            // 默认处理
            message = AppCodeEnum.ERROR.getDescription();
        }
        // 打印异常日志
        log.error("处理其他的异常 ==> {}", message);
        ex.printStackTrace();
        // 设置状态码
        response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
        // 构建返回结果
        return Result.error(message);
    }

    // 将 Spring Security 异常处理
    @ExceptionHandler({AuthorizationDeniedException.class})
    public void handleException(Exception e) throws Exception{
        // 将 Spring Security 异常继续抛出，以便交给自定义处理器处理
        if (e instanceof AuthorizationDeniedException) {
            throw e;
        }
    }

}
