package local.ateng.java.exception.handler;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.ConstraintViolationException;
import local.ateng.java.exception.constant.AppCodeEnum;
import local.ateng.java.exception.exception.ServiceException;
import local.ateng.java.exception.utils.Result;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.validation.FieldError;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.servlet.NoHandlerFoundException;
import org.springframework.web.servlet.resource.NoResourceFoundException;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

/**
 * 全局异常处理器
 *
 * <p>该类通过 {@link RestControllerAdvice} 注解实现 Spring Boot 统一异常处理，
 * 能够捕获控制层抛出的不同类型的异常，并统一转换为标准化的 {@link Result} 响应对象。</p>
 *
 * <p>主要功能包括：</p>
 * <ul>
 *   <li>处理 POST 请求参数校验异常（{@link MethodArgumentNotValidException}）</li>
 *   <li>处理 GET 请求参数校验异常（{@link ConstraintViolationException}）</li>
 *   <li>处理自定义业务异常（{@link ServiceException}）</li>
 *   <li>处理未捕获的系统异常，返回统一的错误响应</li>
 * </ul>
 *
 * <p>日志中会打印异常发生时的请求路径和详细堆栈，便于排查问题。</p>
 *
 * @author 孔余
 * @since 2025-01-09
 */
@RestControllerAdvice
@Slf4j
public class GlobalExceptionHandler {

    /**
     * 处理 POST 请求参数校验异常
     *
     * <p>当请求体参数校验失败（例如字段缺失、格式不正确等）时，
     * Spring 会抛出 {@link MethodArgumentNotValidException}，
     * 本方法捕获该异常并提取所有字段的错误信息。</p>
     *
     * @param request 当前 HTTP 请求对象
     * @param ex      参数校验异常
     * @return 标准化错误响应，包含首个错误提示及所有字段错误详情
     */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public Result handleValidationExceptions(HttpServletRequest request, HttpServletResponse response, MethodArgumentNotValidException ex) {
        // 获取所有参数校验失败的异常
        Map<String, String> errors = new HashMap<>();
        String firstFieldName = null;
        String firstErrorMessage = null;
        for (FieldError error : ex.getBindingResult().getFieldErrors()) {
            if (firstFieldName == null && firstErrorMessage == null) {
                firstFieldName = error.getField();
                firstErrorMessage = error.getDefaultMessage();
            }
            errors.put(error.getField(), error.getDefaultMessage());
        }
        // 打印异常日志
        log.error("请求 [{}] 参数校验失败: {}", request.getRequestURI(), ex.getMessage(), ex);
        // 设置状态码
        response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
        // 构建返回结果
        return Result.failure(firstErrorMessage).withData(errors);
    }

    /**
     * 处理 GET 请求参数校验异常
     *
     * <p>当 URL 查询参数校验失败时（例如 @RequestParam 或 @PathVariable 校验失败），
     * Spring 会抛出 {@link ConstraintViolationException}，
     * 本方法捕获该异常并提取所有字段的错误信息。</p>
     *
     * @param request 当前 HTTP 请求对象
     * @param ex      参数校验异常
     * @return 标准化错误响应，包含首个错误提示及所有字段错误详情
     */
    @ExceptionHandler(ConstraintViolationException.class)
    public Result handleConstraintViolationException(HttpServletRequest request, HttpServletResponse response, ConstraintViolationException ex) {
        // 获取所有参数校验失败的异常
        Map<String, String> errors = new HashMap<>();
        String firstFieldName = null;
        String firstErrorMessage = null;
        for (ConstraintViolation<?> violation : ex.getConstraintViolations()) {
            String propertyPath = violation.getPropertyPath().toString();
            // 只保留参数名称
            String fieldName = propertyPath.split("\\.")[1];
            String errorMessage = violation.getMessage();
            if (firstFieldName == null && firstErrorMessage == null) {
                firstFieldName = fieldName;
                firstErrorMessage = errorMessage;
            }
            errors.put(fieldName, errorMessage);
        }
        // 打印异常日志
        log.error("请求 [{}] 参数校验失败: {}", request.getRequestURI(), ex.getMessage(), ex);
        // 设置状态码
        response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
        // 构建返回结果
        return Result.failure(firstErrorMessage).withData(errors);
    }

    /**
     * 处理业务异常
     *
     * <p>当业务逻辑中主动抛出 {@link ServiceException} 时，
     * 本方法捕获异常并返回自定义的错误码和错误信息。</p>
     *
     * @param request 当前 HTTP 请求对象
     * @param ex      自定义业务异常
     * @return 标准化错误响应，包含业务定义的错误码、错误信息及详细描述
     */
    @ExceptionHandler(ServiceException.class)
    public final Result handleServiceException(HttpServletRequest request, HttpServletResponse response, ServiceException ex) {
        String message = ex.getMessage();
        String code = ex.getCode();
        HashMap<String, String> data = new HashMap<>();
        data.put("detailMessage", ex.getDetailMessage());
        // 打印异常日志
        log.error("请求 [{}] 业务异常: {}", request.getRequestURI(), ex.getMessage(), ex);
        // 设置状态码
        response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
        // 构建返回结果
        return Result.failure(code, message).withData(data);
    }

    /**
     * 处理系统级异常
     *
     * <p>兜底异常处理方法，捕获所有未显式声明的异常类型，
     * 并根据异常类型返回不同的提示信息和 HTTP 状态码。</p>
     *
     * <p>常见异常映射：</p>
     * <ul>
     *   <li>{@link HttpRequestMethodNotSupportedException} → 请求方式不支持 (405)</li>
     *   <li>{@link NoHandlerFoundException}, {@link NoResourceFoundException} → 资源未找到 (404)</li>
     *   <li>{@link HttpMessageNotReadableException}, {@link MissingServletRequestParameterException},
     *       {@link MethodArgumentTypeMismatchException}, {@link NumberFormatException} → 请求参数错误 (400)</li>
     *   <li>{@link IllegalArgumentException} → 非法参数 (400)</li>
     *   <li>{@link IOException} → 文件读写异常 (500)</li>
     *   <li>其他未知异常 → 系统内部错误 (500)</li>
     * </ul>
     *
     * @param request 当前 HTTP 请求对象
     * @param ex      未捕获的系统异常
     * @return 标准化错误响应，包含异常对应的提示信息
     */
    @ExceptionHandler(Exception.class)
    public final Result handleAllExceptions(HttpServletRequest request, HttpServletResponse response, Exception ex) {
        // 定义异常码和消息
        int httpStatus = HttpServletResponse.SC_INTERNAL_SERVER_ERROR;
        String message;
        // 分批处理异常类型
        if (ex instanceof HttpRequestMethodNotSupportedException) {
            message = "请求方式不支持";
            httpStatus = HttpServletResponse.SC_METHOD_NOT_ALLOWED;
        } else if (ex instanceof NoHandlerFoundException || ex instanceof NoResourceFoundException || ex instanceof HttpMessageNotReadableException) {
            message = "资源未找到";
            httpStatus = HttpServletResponse.SC_NOT_FOUND;
        } else if (ex instanceof MissingServletRequestParameterException) {
            message = "请求参数缺失";
        } else if (ex instanceof IllegalArgumentException) {
            message = "请求参数错误";
            httpStatus = HttpServletResponse.SC_BAD_REQUEST;
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
            httpStatus = HttpServletResponse.SC_INTERNAL_SERVER_ERROR;
        }
        // 打印异常日志
        log.error("请求 [{}] 系统异常: {}", request.getRequestURI(), ex.getMessage(), ex);
        // 设置状态码
        response.setStatus(httpStatus);
        // 构建返回结果
        return Result.failure(message);
    }


}
