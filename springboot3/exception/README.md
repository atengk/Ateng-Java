# 全局异常处理

全局异常处理可以通过使用 **`@ControllerAdvice`** 或 **`@RestControllerAdvice`** 结合 **`@ExceptionHandler`** 实现。它是一个优雅的方式，用来集中处理应用程序中的异常，避免在每个控制器中重复处理异常逻辑。

------

**核心注解介绍**

1. **`@ControllerAdvice`**：
    - 作用：定义全局范围的异常处理器，适用于所有的 `Controller`。
    - 处理异常后可以返回视图或 JSON，取决于控制器的返回类型。
2. **`@RestControllerAdvice`**：
    - 是 `@ControllerAdvice` 的扩展，默认将异常处理结果返回为 JSON 或其他对象格式（基于 `@ResponseBody`）。
3. **`@ExceptionHandler`**：
    - 作用：指定要处理的异常类型，可以用于单个控制器或全局异常处理器中。



## 统一返回实体

```java
package local.ateng.java.exception.utils;

import org.slf4j.MDC;

import java.io.Serial;
import java.io.Serializable;
import java.time.Instant;
import java.time.format.DateTimeFormatter;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.StringJoiner;

/**
 * REST 接口统一响应结果。
 * <p>
 * 用于 Spring Boot Controller 接口统一返回业务状态、业务状态码、提示信息、响应数据、
 * 响应时间、链路追踪标识、请求路径和扩展元数据。
 * </p>
 * <p>
 * 当前类型采用不可变设计，所有修改操作都会返回新的 Result 实例，避免响应对象在异步处理、
 * 日志采集、异常处理或拦截器扩展过程中出现状态漂移。
 * </p>
 *
 * @param <T> 响应数据类型
 * @author Ateng
 * @since 2026-05-01
 */
public final class Result<T> implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * 默认成功业务码。
     */
    public static final String DEFAULT_SUCCESS_CODE = "0";

    /**
     * 默认失败业务码。
     */
    public static final String DEFAULT_FAILURE_CODE = "-1";

    /**
     * 默认成功提示。
     */
    public static final String DEFAULT_SUCCESS_MESSAGE = "请求成功";

    /**
     * 默认失败提示。
     */
    public static final String DEFAULT_FAILURE_MESSAGE = "服务器异常，请稍后再试";

    /**
     * 参数校验失败业务码。
     */
    public static final String PARAM_ERROR_CODE = "400";

    /**
     * 认证失败业务码。
     */
    public static final String UNAUTHORIZED_CODE = "401";

    /**
     * 权限不足业务码。
     */
    public static final String FORBIDDEN_CODE = "403";

    /**
     * 资源不存在业务码。
     */
    public static final String NOT_FOUND_CODE = "404";

    /**
     * 业务冲突业务码。
     */
    public static final String CONFLICT_CODE = "409";

    /**
     * 请求限流业务码。
     */
    public static final String TOO_MANY_REQUESTS_CODE = "429";

    /**
     * MDC 标准链路追踪字段。
     */
    public static final String MDC_TRACE_ID_KEY = "traceId";

    /**
     * MDC 请求标识字段。
     */
    public static final String MDC_REQUEST_ID_KEY = "requestId";

    /**
     * MDC 兼容链路字段集合。
     */
    private static final String[] MDC_TRACE_ID_KEYS = {
            MDC_TRACE_ID_KEY,
            "trace_id",
            "trace-id",
            "X-B3-TraceId",
            "x-b3-traceid",
            "traceparent",
            MDC_REQUEST_ID_KEY,
            "request_id",
            "request-id"
    };

    /**
     * 业务执行是否成功。
     */
    private final boolean success;

    /**
     * 业务状态码。
     */
    private final String code;

    /**
     * 响应提示信息。
     */
    private final String msg;

    /**
     * 响应数据。
     */
    private final T data;

    /**
     * 响应构建时间，ISO-8601 UTC 格式。
     */
    private final String timestamp;

    /**
     * 链路追踪标识。
     */
    private final String traceId;

    /**
     * 当前请求路径。
     */
    private final String path;

    /**
     * 扩展响应元数据。
     */
    private final Map<String, Object> extra;

    /**
     * 构建统一响应结果。
     *
     * @param success   业务执行状态
     * @param code      业务状态码
     * @param msg       响应提示信息
     * @param data      响应数据
     * @param timestamp 响应构建时间
     * @param traceId   链路追踪标识
     * @param path      当前请求路径
     * @param extra     扩展响应元数据
     */
    private Result(boolean success,
                   String code,
                   String msg,
                   T data,
                   String timestamp,
                   String traceId,
                   String path,
                   Map<String, Object> extra) {
        this.success = success;
        this.code = normalizeText(code, success ? DEFAULT_SUCCESS_CODE : DEFAULT_FAILURE_CODE);
        this.msg = normalizeText(msg, success ? DEFAULT_SUCCESS_MESSAGE : DEFAULT_FAILURE_MESSAGE);
        this.data = data;
        this.timestamp = normalizeText(timestamp, nowTimestamp());
        this.traceId = firstText(traceId, currentTraceId());
        this.path = normalizeNullableText(path);
        this.extra = immutableExtra(extra);
    }

    /**
     * 构建成功响应。
     *
     * @param <T> 响应数据类型
     * @return 成功响应
     */
    public static <T> Result<T> success() {
        return success(null);
    }

    /**
     * 构建成功响应并携带响应数据。
     *
     * @param data 响应数据
     * @param <T>  响应数据类型
     * @return 成功响应
     */
    public static <T> Result<T> success(T data) {
        return new Result<>(true, DEFAULT_SUCCESS_CODE, DEFAULT_SUCCESS_MESSAGE, data, null, null, null, null);
    }

    /**
     * 构建成功响应并覆盖提示信息。
     *
     * @param msg 响应提示信息
     * @param <T> 响应数据类型
     * @return 成功响应
     */
    public static <T> Result<T> successMessage(String msg) {
        return new Result<>(true, DEFAULT_SUCCESS_CODE, msg, null, null, null, null, null);
    }

    /**
     * 构建成功响应并携带提示信息和响应数据。
     *
     * @param msg  响应提示信息
     * @param data 响应数据
     * @param <T>  响应数据类型
     * @return 成功响应
     */
    public static <T> Result<T> success(String msg, T data) {
        return new Result<>(true, DEFAULT_SUCCESS_CODE, msg, data, null, null, null, null);
    }

    /**
     * 构建成功响应并覆盖业务码、提示信息和响应数据。
     *
     * @param code 业务状态码
     * @param msg  响应提示信息
     * @param data 响应数据
     * @param <T>  响应数据类型
     * @return 成功响应
     */
    public static <T> Result<T> success(String code, String msg, T data) {
        return new Result<>(true, code, msg, data, null, null, null, null);
    }

    /**
     * 构建失败响应。
     *
     * @param <T> 响应数据类型
     * @return 失败响应
     */
    public static <T> Result<T> failure() {
        return failure(DEFAULT_FAILURE_MESSAGE);
    }

    /**
     * 构建失败响应并覆盖提示信息。
     *
     * @param msg 响应提示信息
     * @param <T> 响应数据类型
     * @return 失败响应
     */
    public static <T> Result<T> failure(String msg) {
        return new Result<>(false, DEFAULT_FAILURE_CODE, msg, null, null, null, null, null);
    }

    /**
     * 构建失败响应并覆盖业务码和提示信息。
     *
     * @param code 业务状态码
     * @param msg  响应提示信息
     * @param <T>  响应数据类型
     * @return 失败响应
     */
    public static <T> Result<T> failure(String code, String msg) {
        return new Result<>(false, code, msg, null, null, null, null, null);
    }

    /**
     * 构建失败响应并携带错误上下文数据。
     *
     * @param code 业务状态码
     * @param msg  响应提示信息
     * @param data 错误上下文数据
     * @param <T>  响应数据类型
     * @return 失败响应
     */
    public static <T> Result<T> failure(String code, String msg, T data) {
        return new Result<>(false, code, msg, data, null, null, null, null);
    }

    /**
     * 构建参数校验失败响应。
     *
     * @param msg 响应提示信息
     * @param <T> 响应数据类型
     * @return 参数校验失败响应
     */
    public static <T> Result<T> paramError(String msg) {
        return failure(PARAM_ERROR_CODE, msg);
    }

    /**
     * 构建认证失败响应。
     *
     * @param msg 响应提示信息
     * @param <T> 响应数据类型
     * @return 认证失败响应
     */
    public static <T> Result<T> unauthorized(String msg) {
        return failure(UNAUTHORIZED_CODE, msg);
    }

    /**
     * 构建权限不足响应。
     *
     * @param msg 响应提示信息
     * @param <T> 响应数据类型
     * @return 权限不足响应
     */
    public static <T> Result<T> forbidden(String msg) {
        return failure(FORBIDDEN_CODE, msg);
    }

    /**
     * 构建资源不存在响应。
     *
     * @param msg 响应提示信息
     * @param <T> 响应数据类型
     * @return 资源不存在响应
     */
    public static <T> Result<T> notFound(String msg) {
        return failure(NOT_FOUND_CODE, msg);
    }

    /**
     * 构建业务冲突响应。
     *
     * @param msg 响应提示信息
     * @param <T> 响应数据类型
     * @return 业务冲突响应
     */
    public static <T> Result<T> conflict(String msg) {
        return failure(CONFLICT_CODE, msg);
    }

    /**
     * 构建请求限流响应。
     *
     * @param msg 响应提示信息
     * @param <T> 响应数据类型
     * @return 请求限流响应
     */
    public static <T> Result<T> tooManyRequests(String msg) {
        return failure(TOO_MANY_REQUESTS_CODE, msg);
    }

    /**
     * 基于 Optional 构建存在即成功、不存在即失败的响应。
     *
     * @param optional    可选响应数据
     * @param notFoundMsg 资源不存在提示信息
     * @param <T>         响应数据类型
     * @return 统一响应
     */
    public static <T> Result<T> fromOptional(Optional<T> optional, String notFoundMsg) {
        if (optional != null && optional.isPresent()) {
            return success(optional.get());
        }
        return notFound(notFoundMsg);
    }

    /**
     * 基于可空数据构建存在即成功、不存在即失败的响应。
     *
     * @param data        可空响应数据
     * @param notFoundMsg 资源不存在提示信息
     * @param <T>         响应数据类型
     * @return 统一响应
     */
    public static <T> Result<T> fromNullable(T data, String notFoundMsg) {
        return data == null ? notFound(notFoundMsg) : success(data);
    }

    /**
     * 获取当前线程上下文中的链路追踪标识。
     *
     * @return MDC 中的链路追踪标识，不存在时返回 null
     */
    public static String currentTraceId() {
        for (String key : MDC_TRACE_ID_KEYS) {
            String value = MDC.get(key);
            if (!hasText(value)) {
                continue;
            }

            if ("traceparent".equals(key)) {
                String traceId = parseTraceParent(value);
                if (hasText(traceId)) {
                    return traceId;
                }
            }

            return value.strip();
        }
        return null;
    }

    /**
     * 获取业务执行状态。
     *
     * @return 成功返回 true，否则返回 false
     */
    public boolean isSuccess() {
        return success;
    }

    /**
     * 获取业务状态码。
     *
     * @return 业务状态码
     */
    public String getCode() {
        return code;
    }

    /**
     * 获取响应提示信息。
     *
     * @return 响应提示信息
     */
    public String getMsg() {
        return msg;
    }

    /**
     * 获取响应数据。
     *
     * @return 响应数据
     */
    public T getData() {
        return data;
    }

    /**
     * 获取响应构建时间。
     *
     * @return ISO-8601 UTC 时间字符串
     */
    public String getTimestamp() {
        return timestamp;
    }

    /**
     * 获取链路追踪标识。
     *
     * @return 链路追踪标识
     */
    public String getTraceId() {
        return traceId;
    }

    /**
     * 获取当前请求路径。
     *
     * @return 当前请求路径
     */
    public String getPath() {
        return path;
    }

    /**
     * 获取扩展响应元数据。
     *
     * @return 不可变扩展响应元数据
     */
    public Map<String, Object> getExtra() {
        return extra;
    }

    /**
     * 判断响应是否处于成功状态。
     * <p>
     * 方法命名不采用 JavaBean getter 规范，避免被识别为额外响应字段。
     * </p>
     *
     * @return 成功返回 true，否则返回 false
     */
    public boolean successful() {
        return this.success;
    }

    /**
     * 判断响应是否处于失败状态。
     * <p>
     * 方法命名不采用 JavaBean getter 规范，避免被识别为额外响应字段。
     * </p>
     *
     * @return 失败返回 true，否则返回 false
     */
    public boolean failed() {
        return !this.success;
    }

    /**
     * 复制当前响应并覆盖业务状态码。
     *
     * @param code 业务状态码
     * @return 新响应对象
     */
    public Result<T> withCode(String code) {
        return new Result<>(this.success, code, this.msg, this.data, this.timestamp, this.traceId, this.path, this.extra);
    }

    /**
     * 复制当前响应并覆盖提示信息。
     *
     * @param msg 响应提示信息
     * @return 新响应对象
     */
    public Result<T> withMsg(String msg) {
        return new Result<>(this.success, this.code, msg, this.data, this.timestamp, this.traceId, this.path, this.extra);
    }

    /**
     * 复制当前响应并覆盖响应数据。
     *
     * @param data 新响应数据
     * @param <U>  新响应数据类型
     * @return 新响应对象
     */
    public <U> Result<U> withData(U data) {
        return new Result<>(this.success, this.code, this.msg, data, this.timestamp, this.traceId, this.path, this.extra);
    }

    /**
     * 复制当前响应并覆盖链路追踪标识。
     *
     * @param traceId 链路追踪标识
     * @return 新响应对象
     */
    public Result<T> withTraceId(String traceId) {
        return new Result<>(this.success, this.code, this.msg, this.data, this.timestamp, traceId, this.path, this.extra);
    }

    /**
     * 复制当前响应并从 MDC 刷新链路追踪标识。
     *
     * @return 新响应对象
     */
    public Result<T> withCurrentTraceId() {
        return withTraceId(currentTraceId());
    }

    /**
     * 复制当前响应并覆盖请求路径。
     *
     * @param path 当前请求路径
     * @return 新响应对象
     */
    public Result<T> withPath(String path) {
        return new Result<>(this.success, this.code, this.msg, this.data, this.timestamp, this.traceId, path, this.extra);
    }

    /**
     * 复制当前响应并补充请求上下文。
     *
     * @param traceId 链路追踪标识
     * @param path    当前请求路径
     * @return 新响应对象
     */
    public Result<T> withRequestContext(String traceId, String path) {
        return new Result<>(this.success, this.code, this.msg, this.data, this.timestamp, traceId, path, this.extra);
    }

    /**
     * 复制当前响应并追加单个扩展元数据。
     *
     * @param key   扩展字段名
     * @param value 扩展字段值
     * @return 新响应对象
     */
    public Result<T> withExtra(String key, Object value) {
        if (!hasText(key)) {
            throw new IllegalArgumentException("扩展字段名不能为空");
        }

        Map<String, Object> newExtra = new LinkedHashMap<>(this.extra);
        newExtra.put(key.strip(), value);
        return new Result<>(this.success, this.code, this.msg, this.data, this.timestamp, this.traceId, this.path, newExtra);
    }

    /**
     * 复制当前响应并合并扩展元数据。
     *
     * @param extra 扩展响应元数据
     * @return 新响应对象
     */
    public Result<T> withExtra(Map<String, Object> extra) {
        if (extra == null || extra.isEmpty()) {
            return this;
        }

        Map<String, Object> newExtra = new LinkedHashMap<>(this.extra);
        for (Map.Entry<String, Object> entry : extra.entrySet()) {
            String key = entry.getKey();
            if (hasText(key)) {
                newExtra.put(key.strip(), entry.getValue());
            }
        }

        return new Result<>(this.success, this.code, this.msg, this.data, this.timestamp, this.traceId, this.path, newExtra);
    }

    /**
     * 复制当前响应并清空扩展元数据。
     *
     * @return 新响应对象
     */
    public Result<T> withoutExtra() {
        if (this.extra.isEmpty()) {
            return this;
        }
        return new Result<>(this.success, this.code, this.msg, this.data, this.timestamp, this.traceId, this.path, null);
    }

    /**
     * 复制当前响应并覆盖响应构建时间。
     *
     * @param timestamp 响应构建时间
     * @return 新响应对象
     */
    public Result<T> withTimestamp(Instant timestamp) {
        String formattedTimestamp = timestamp == null ? null : DateTimeFormatter.ISO_INSTANT.format(timestamp);
        return new Result<>(this.success, this.code, this.msg, this.data, formattedTimestamp, this.traceId, this.path, this.extra);
    }

    /**
     * 复制当前响应并覆盖响应构建时间。
     *
     * @param timestamp ISO-8601 时间字符串
     * @return 新响应对象
     */
    public Result<T> withTimestamp(String timestamp) {
        return new Result<>(this.success, this.code, this.msg, this.data, timestamp, this.traceId, this.path, this.extra);
    }

    /**
     * 判断两个对象是否相等。
     *
     * @param object 待比较对象
     * @return 相等返回 true，否则返回 false
     */
    @Override
    public boolean equals(Object object) {
        if (this == object) {
            return true;
        }
        if (!(object instanceof Result<?> result)) {
            return false;
        }
        return success == result.success
                && Objects.equals(code, result.code)
                && Objects.equals(msg, result.msg)
                && Objects.equals(data, result.data)
                && Objects.equals(timestamp, result.timestamp)
                && Objects.equals(traceId, result.traceId)
                && Objects.equals(path, result.path)
                && Objects.equals(extra, result.extra);
    }

    /**
     * 生成对象哈希值。
     *
     * @return 哈希值
     */
    @Override
    public int hashCode() {
        return Objects.hash(success, code, msg, data, timestamp, traceId, path, extra);
    }

    /**
     * 生成对象字符串。
     *
     * @return 对象字符串
     */
    @Override
    public String toString() {
        return new StringJoiner(", ", Result.class.getSimpleName() + "[", "]")
                .add("success=" + success)
                .add("code='" + code + "'")
                .add("msg='" + msg + "'")
                .add("data=" + data)
                .add("timestamp='" + timestamp + "'")
                .add("traceId='" + traceId + "'")
                .add("path='" + path + "'")
                .add("extra=" + extra)
                .toString();
    }

    /**
     * 获取首个有效文本。
     *
     * @param primary  优先文本
     * @param fallback 兜底文本
     * @return 规范化后的有效文本
     */
    private static String firstText(String primary, String fallback) {
        if (hasText(primary)) {
            return primary.strip();
        }
        return normalizeNullableText(fallback);
    }

    /**
     * 规范化必填文本字段。
     *
     * @param value        原始文本
     * @param defaultValue 默认文本
     * @return 规范化后的文本
     */
    private static String normalizeText(String value, String defaultValue) {
        return hasText(value) ? value.strip() : defaultValue;
    }

    /**
     * 规范化可选文本字段。
     *
     * @param value 原始文本
     * @return 规范化后的文本
     */
    private static String normalizeNullableText(String value) {
        return hasText(value) ? value.strip() : null;
    }

    /**
     * 判断文本是否包含有效字符。
     *
     * @param value 待判断文本
     * @return 包含有效字符返回 true，否则返回 false
     */
    private static boolean hasText(String value) {
        return value != null && !value.isBlank();
    }

    /**
     * 获取当前 UTC 时间字符串。
     *
     * @return ISO-8601 UTC 时间字符串
     */
    private static String nowTimestamp() {
        return DateTimeFormatter.ISO_INSTANT.format(Instant.now());
    }

    /**
     * 解析 W3C Trace Context 中的 traceId。
     *
     * @param traceparent W3C traceparent 字段值
     * @return traceId，不符合格式时返回 null
     */
    private static String parseTraceParent(String traceparent) {
        if (!hasText(traceparent)) {
            return null;
        }

        String value = traceparent.strip();
        String[] parts = value.split("-");
        if (parts.length < 4) {
            return null;
        }

        String traceId = parts[1];
        if (traceId.length() != 32 || isAllZero(traceId) || !isLowerHex(traceId)) {
            return null;
        }

        return traceId;
    }

    /**
     * 判断字符串是否为小写十六进制。
     *
     * @param value 待判断字符串
     * @return 是小写十六进制返回 true，否则返回 false
     */
    private static boolean isLowerHex(String value) {
        for (int i = 0; i < value.length(); i++) {
            char ch = value.charAt(i);
            boolean digit = ch >= '0' && ch <= '9';
            boolean lowerHex = ch >= 'a' && ch <= 'f';
            if (!digit && !lowerHex) {
                return false;
            }
        }
        return true;
    }

    /**
     * 判断字符串是否全为 0。
     *
     * @param value 待判断字符串
     * @return 全为 0 返回 true，否则返回 false
     */
    private static boolean isAllZero(String value) {
        for (int i = 0; i < value.length(); i++) {
            if (value.charAt(i) != '0') {
                return false;
            }
        }
        return true;
    }

    /**
     * 构建不可变扩展元数据。
     *
     * @param extra 原始扩展元数据
     * @return 不可变扩展元数据
     */
    private static Map<String, Object> immutableExtra(Map<String, Object> extra) {
        if (extra == null || extra.isEmpty()) {
            return Collections.emptyMap();
        }

        Map<String, Object> copy = new LinkedHashMap<>();
        for (Map.Entry<String, Object> entry : extra.entrySet()) {
            String key = entry.getKey();
            if (hasText(key)) {
                copy.put(key.strip(), entry.getValue());
            }
        }

        if (copy.isEmpty()) {
            return Collections.emptyMap();
        }

        return Collections.unmodifiableMap(copy);
    }

}
```

## 响应增强处理器

```java
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
```



## 全局异常拦截器

这里只给出关键的ExceptionHandler类，其他涉及到的代码自行去源码中查找

```java
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

```



## 接口测试

**创建接口**

创建一个测试接口，查看全局异常处理的结果

```java
package local.ateng.java.exception.controller;

import local.ateng.java.exception.constant.AppCodeEnum;
import local.ateng.java.exception.utils.Result;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;

/**
 * 测试接口
 *
 * @author 孔余
 * @email 2385569970@qq.com
 * @since 2025-01-09
 */
@RestController
@RequestMapping("/test")
public class TestController {

    @GetMapping("/exception")
    public Result exception(@RequestParam(name = "id", required = false, defaultValue = "0") Long id) {
        long result = 1 / id;
        HashMap<String, Long> map = new HashMap<>() {{
            put("result", result);
            put("null", null);
        }};
        return Result.success(AppCodeEnum.SUCCESS.getCode(), AppCodeEnum.SUCCESS.getDescription()).withData(map);
    }

}
```

**正确使用**

```
C:\Users\admin>curl http://localhost:12006/test/exception?id=1
{"code":"0","msg":"请求成功","data":{"result":1,"null":null}}
```

**异常使用**

由此可以看到全局异常生效

```
C:\Users\admin>curl http://localhost:12006/test/exception?id=0
{"code":"-1","msg":"数据计算异常","data":null}
C:\Users\admin>curl http://localhost:12006/test/exception
{"code":"-1","msg":"数据计算异常","data":null}
C:\Users\admin>curl http://localhost:12006/test/exception?id=null
{"code":"-1","msg":"数据类型不匹配异常","data":null}
```



## 关于统一返回的使用

### **使用 `Result<T>`**

```java
package local.ateng.java.controller;

import local.ateng.java.serialize.utils.Result;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.Map;

/**
 * 示例 Controller，展示如何在 Spring Boot 中使用 Result<T> 封装统一的 API 响应。
 */
@RestController
@RequestMapping("/api")
public class ExampleController {

    /**
     * 获取成功响应的示例，无业务数据。
     *
     * @return 返回标准成功响应
     */
    @GetMapping("/success/no-data")
    public Result<String> successNoData() {
        return Result.success();
    }

    /**
     * 获取成功响应的示例，包含业务数据。
     *
     * @return 返回成功响应，包含业务数据
     */
    @GetMapping("/success/with-data")
    public Result<Map<String, String>> successWithData() {
        Map<String, String> data = new HashMap<>();
        data.put("message", "Hello, World!");
        data.put("status", "success");

        return Result.success(data);
    }

    /**
     * 获取成功响应的示例，包含自定义消息和数据。
     *
     * @return 返回成功响应，包含自定义消息和数据
     */
    @GetMapping("/success/custom-msg")
    public Result<String> successCustomMsg() {
        return Result.success("Custom success message", "This is some business data");
    }

    /**
     * 获取失败响应的示例，包含自定义错误消息。
     *
     * @return 返回失败响应，包含自定义错误消息
     */
    @GetMapping("/failure/custom-msg")
    public Result<String> failureCustomMsg() {
        return Result.failure("Custom failure message");
    }

    /**
     * 获取失败响应的示例，包含自定义错误码和消息。
     *
     * @return 返回失败响应，包含自定义错误码和消息
     */
    @GetMapping("/failure/custom-code-msg")
    public Result<String> failureCustomCodeMsg() {
        return Result.failure("1001", "Custom error code and message");
    }

    /**
     * 获取包含额外信息的成功响应示例。
     *
     * @return 返回成功响应，包含额外信息
     */
    @GetMapping("/success/with-extra")
    public Result<Map<String, String>> successWithExtra() {
        Map<String, String> data = new HashMap<>();
        data.put("message", "Operation was successful");

        return Result.success(data)
                .extra("timestamp", System.currentTimeMillis())  // 添加额外信息
                .extra("source", "example-controller");
    }

    /**
     * 获取失败响应的示例，包含自定义状态码、消息和数据。
     *
     * @return 返回失败响应，包含自定义状态码、消息和数据
     */
    @GetMapping("/failure/with-data")
    public Result<String> failureWithData() {
        return Result.failure("500", "Server error", "Failed to process the request");
    }
}
```

### **解释：**

1. **`successNoData()`**：示范返回一个**成功**的响应，且没有业务数据。
2. **`successWithData()`**：示范返回一个**成功**的响应，包含一些业务数据（如 `Map<String, String>`）。
3. **`successCustomMsg()`**：示范返回一个**成功**的响应，包含自定义消息和数据。
4. **`failureCustomMsg()`**：示范返回一个**失败**的响应，包含自定义失败消息（没有业务数据）。
5. **`failureCustomCodeMsg()`**：示范返回一个**失败**的响应，包含自定义状态码和失败消息。
6. **`successWithExtra()`**：示范返回一个**成功**的响应，并且在返回中包含**额外信息**（通过 `extra()` 方法添加的）。
7. **`failureWithData()`**：示范返回一个**失败**的响应，包含自定义状态码、消息以及业务数据。

------

### **API 响应示例**

#### **成功响应：**

- `/api/success/no-data`

    ```json
    {
      "code": "0",
      "msg": "请求成功",
      "data": null,
      "timestamp": "2025-03-05T10:20:30",
      "extra": {}
    }
    ```

- `/api/success/with-data`

    ```json
    {
      "code": "0",
      "msg": "请求成功",
      "data": {
        "message": "Hello, World!",
        "status": "success"
      },
      "timestamp": "2025-03-05T10:20:30",
      "extra": {}
    }
    ```

#### **失败响应：**

- `/api/failure/custom-msg`

    ```json
    {
      "code": "-1",
      "msg": "Custom failure message",
      "data": null,
      "timestamp": "2025-03-05T10:20:30",
      "extra": {}
    }
    ```

- `/api/failure/custom-code-msg`

    ```json
    {
      "code": "1001",
      "msg": "Custom error code and message",
      "data": null,
      "timestamp": "2025-03-05T10:20:30",
      "extra": {}
    }
    ```

#### **带额外信息的成功响应：**

- ```
    /api/success/with-extra
    ```

    ```json
    {
      "code": "0",
      "msg": "请求成功",
      "data": {
        "message": "Operation was successful"
      },
      "timestamp": "2025-03-05T10:20:30",
      "extra": {
        "timestamp": 1678013430000,
        "source": "example-controller"
      }
    }
    ```

