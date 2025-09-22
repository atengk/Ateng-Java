package local.ateng.java.exception.handler;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.ConstraintViolationException;
import local.ateng.java.exception.enums.AppCodeEnum;
import local.ateng.java.exception.exception.ServiceException;
import local.ateng.java.exception.utils.Result;
import lombok.extern.slf4j.Slf4j;
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
        response.setStatus(AppCodeEnum.PARAM_DATA_VALIDATION_FAILED.getHttpStatus());
        // 构建返回结果
        return Result.failure(AppCodeEnum.PARAM_DATA_VALIDATION_FAILED.getCode(), firstErrorMessage)
                .withData(errors);
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
        response.setStatus(AppCodeEnum.PARAM_DATA_VALIDATION_FAILED.getHttpStatus());
        // 构建返回结果
        return Result.failure(AppCodeEnum.PARAM_DATA_VALIDATION_FAILED.getCode(), firstErrorMessage)
                .withData(errors);
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
     * 兜底系统异常处理
     *
     * <p>
     * 捕获所有未显式声明的异常类型，根据异常类别返回对应的枚举错误码和提示信息。
     * 确保系统不会因未捕获异常而暴露堆栈或返回非标准响应。
     * </p>
     *
     * @param request  当前 HTTP 请求对象
     * @param response 当前 HTTP 响应对象
     * @param ex       未捕获的异常
     * @return {@link Result} 标准化失败结果，包含错误码与提示信息
     */
    @ExceptionHandler(Exception.class)
    public Result handleAllExceptions(HttpServletRequest request,
                                      HttpServletResponse response,
                                      Exception ex) {
        AppCodeEnum errorCode;
        // 分批处理异常类型
        if (ex instanceof HttpRequestMethodNotSupportedException) {
            errorCode = AppCodeEnum.REQUEST_METHOD_NOT_SUPPORTED;
        } else if (ex instanceof NoHandlerFoundException || ex instanceof NoResourceFoundException) {
            errorCode = AppCodeEnum.RESOURCE_NOT_FOUND;
        } else if (ex instanceof MissingServletRequestParameterException) {
            errorCode = AppCodeEnum.PARAM_MISSING_REQUIRED;
        } else if (ex instanceof IllegalArgumentException
                || ex instanceof MethodArgumentTypeMismatchException
                || ex instanceof NumberFormatException) {
            errorCode = AppCodeEnum.PARAM_REQUEST_PARAMETER_TYPE_ERROR;
        } else if (ex instanceof FileNotFoundException || ex instanceof IOException) {
            errorCode = AppCodeEnum.FILE_NOT_FOUND;
        } else if (ex instanceof NullPointerException) {
            errorCode = AppCodeEnum.NULL_POINTER_ERROR;
        } else if (ex instanceof UnsupportedOperationException) {
            errorCode = AppCodeEnum.INVALID_OPERATION_TYPE;
        } else {
            errorCode = AppCodeEnum.ERROR;
        }
        // 打印异常日志
        log.error("请求 [{}] 系统异常: {}", request.getRequestURI(), ex.getMessage(), ex);
        // 设置状态码
        response.setStatus(errorCode.getHttpStatus());
        // 构建返回结果
        return Result.failure(errorCode.getCode(), errorCode.getMessage());
    }

}
