package local.ateng.java.validator.utils;

import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 统一 API 响应结果封装类，支持链式调用和额外数据存储。
 * <p>
 * 该类是 **不可变对象（Immutable Object）**，以保证线程安全。
 * 每次修改操作都会创建一个新的 `Result<T>` 实例，避免并发环境下的数据竞争问题。
 * </p>
 *
 * @param <T> 业务数据的类型
 * @author 孔余
 * @email 2385569970@qq.com
 * @since 2025-03-05
 */
public class Result<T> implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 默认成功状态码
     */
    private static final String DEFAULT_SUCCESS_CODE = "0";
    /**
     * 默认成功消息
     */
    private static final String DEFAULT_SUCCESS_MESSAGE = "请求成功";
    /**
     * 默认失败状态码
     */
    private static final String DEFAULT_ERROR_CODE = "-1";
    /**
     * 默认失败消息
     */
    private static final String DEFAULT_ERROR_MESSAGE = "服务器异常，请稍后再试";

    /**
     * 状态码
     */
    private final String code;
    /**
     * 提示信息
     */
    private final String msg;
    /**
     * 业务数据
     */
    private final T data;
    /**
     * 响应时间戳（不可变）
     */
    private final LocalDateTime timestamp;
    /**
     * 额外信息存储（使用不可变 Map，保证线程安全）
     */
    private final Map<String, Object> extra;

    /**
     * 私有构造方法，默认初始化成功状态，extra 为空 Map。
     */
    private Result() {
        this.code = DEFAULT_SUCCESS_CODE;
        this.msg = DEFAULT_SUCCESS_MESSAGE;
        this.data = null;
        this.timestamp = LocalDateTime.now();
        this.extra = Collections.emptyMap();
    }

    /**
     * 统一构造方法，确保 extra 是不可变的，防止并发修改问题。
     *
     * @param code  状态码
     * @param msg   提示信息
     * @param data  业务数据
     * @param extra 额外信息 Map
     */
    private Result(String code, String msg, T data, Map<String, Object> extra) {
        this.code = code;
        this.msg = msg;
        this.data = data;
        this.timestamp = LocalDateTime.now();
        // 使用不可变 Map 防止外部修改
        this.extra = (extra == null) ? Collections.emptyMap() : Collections.unmodifiableMap(extra);
    }

    // ==============================
    // 静态工厂方法（标准 API 响应）
    // ==============================

    /**
     * 生成一个成功的响应（无数据）。
     *
     * @param <T> 泛型类型
     * @return 成功的 Result 实例
     */
    public static <T> Result<T> success() {
        return new Result<>(DEFAULT_SUCCESS_CODE, DEFAULT_SUCCESS_MESSAGE, null, null);
    }

    /**
     * 生成一个成功的响应，包含数据。
     *
     * @param <T>  业务数据类型
     * @param data 业务数据
     * @return 成功的 Result 实例
     */
    public static <T> Result<T> success(T data) {
        return new Result<>(DEFAULT_SUCCESS_CODE, DEFAULT_SUCCESS_MESSAGE, data, null);
    }

    /**
     * 生成一个成功的响应，包含自定义消息和数据。
     *
     * @param <T>  业务数据类型
     * @param msg  自定义成功消息
     * @param data 业务数据
     * @return 成功的 Result 实例
     */
    public static <T> Result<T> success(String msg, T data) {
        return new Result<>(DEFAULT_SUCCESS_CODE, msg, data, null);
    }

    /**
     * 生成一个成功的响应，包含自定义状态码、消息和数据。
     *
     * @param <T>  业务数据类型
     * @param code 自定义状态码
     * @param msg  自定义成功消息
     * @param data 业务数据
     * @return 返回标准成功响应
     */
    public static <T> Result<T> success(String code, String msg, T data) {
        return new Result<>(code, msg, data, null);
    }

    /**
     * 生成一个失败的响应（无数据）。
     *
     * @param <T> 业务数据类型
     * @return 失败的 Result 实例
     */
    public static <T> Result<T> failure() {
        return new Result<>(DEFAULT_ERROR_CODE, DEFAULT_ERROR_MESSAGE, null, null);
    }

    /**
     * 生成一个失败的响应，包含自定义错误消息。
     *
     * @param <T> 业务数据类型
     * @param msg 自定义失败消息
     * @return 失败的 Result 实例
     */
    public static <T> Result<T> failure(String msg) {
        return new Result<>(DEFAULT_ERROR_CODE, msg, null, null);
    }

    /**
     * 生成一个失败的响应，包含自定义状态码和消息（无业务数据）。
     *
     * @param <T>  业务数据类型
     * @param code 自定义状态码
     * @param msg  自定义失败消息
     * @return 返回标准失败响应
     */
    public static <T> Result<T> failure(String code, String msg) {
        return new Result<>(code, msg, null, null);
    }

    /**
     * 生成一个失败的响应，包含自定义状态码、消息和数据。
     *
     * @param <T>  业务数据类型
     * @param code 自定义状态码
     * @param msg  自定义失败消息
     * @param data 业务数据
     * @return 返回标准失败响应
     */
    public static <T> Result<T> failure(String code, String msg, T data) {
        return new Result<>(code, msg, data, null);
    }

    // ==============================
    // 生成新实例的方法（不可变对象）
    // ==============================

    /**
     * 生成新的 `Result<T>`，修改状态码。
     *
     * @param code 新的状态码
     * @return 新的 `Result<T>` 实例
     */
    public Result<T> withCode(String code) {
        return new Result<>(code, this.msg, this.data, this.extra);
    }

    /**
     * 生成新的 `Result<T>`，修改提示信息。
     *
     * @param msg 新的提示信息
     * @return 新的 `Result<T>` 实例
     */
    public Result<T> withMsg(String msg) {
        return new Result<>(this.code, msg, this.data, this.extra);
    }

    /**
     * 生成新的 `Result<U>`，修改业务数据类型。
     *
     * @param <U>  新的泛型类型
     * @param data 业务数据
     * @return 新的 `Result<U>` 实例
     */
    public <U> Result<U> withData(U data) {
        return new Result<>(this.code, this.msg, data, this.extra);
    }

    /**
     * 生成新的 `Result<T>`，添加单个额外信息。
     *
     * @param key   额外信息的键
     * @param value 额外信息的值
     * @return 新的 `Result<T>` 实例
     */
    public Result<T> withExtra(String key, Object value) {
        Map<String, Object> newExtra = new ConcurrentHashMap<>(this.extra);
        newExtra.put(key, value);
        return new Result<>(this.code, this.msg, this.data, newExtra);
    }

    /**
     * 生成新的 `Result<T>`，添加多个额外信息。
     *
     * @param extraData 额外信息的 Map
     * @return 新的 `Result<T>` 实例
     */
    public Result<T> withExtra(Map<String, Object> extraData) {
        if (extraData == null || extraData.isEmpty()) {
            return this;
        }
        Map<String, Object> newExtra = new ConcurrentHashMap<>(this.extra);
        newExtra.putAll(extraData);
        return new Result<>(this.code, this.msg, this.data, newExtra);
    }

    // ==============================
    // Getter 方法（保持不可变性）
    // ==============================

    public String getCode() {
        return code;
    }

    public String getMsg() {
        return msg;
    }

    public T getData() {
        return data;
    }

    public LocalDateTime getTimestamp() {
        return timestamp;
    }

    public Map<String, Object> getExtra() {
        return extra;
    }

    // ==============================
    // toString 方法
    // ==============================

    @Override
    public String toString() {
        return "Result{" +
                "code='" + code + '\'' +
                ", msg='" + msg + '\'' +
                ", data=" + data +
                ", timestamp=" + timestamp +
                ", extra=" + extra +
                '}';
    }
}
