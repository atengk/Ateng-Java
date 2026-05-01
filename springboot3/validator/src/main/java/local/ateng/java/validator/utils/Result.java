package local.ateng.java.validator.utils;

import org.slf4j.MDC;

import java.io.Serial;
import java.io.Serializable;
import java.time.Instant;
import java.time.format.DateTimeFormatter;
import java.util.*;

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