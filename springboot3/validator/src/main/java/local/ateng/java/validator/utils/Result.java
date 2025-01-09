package local.ateng.java.validator.utils;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import local.ateng.java.validator.constant.AppCodeEnum;

import java.io.Serial;
import java.io.Serializable;

/**
 * 统一结果返回处理，支持链式调用
 *
 * @author 孔余
 * @email 2385569970@qq.com
 * @since 2025-01-09
 */
public class Result<T> implements Serializable {
    @Serial
    private static final long serialVersionUID = 1L;
    // 提供 ObjectMapper 的实例
    private static final ObjectMapper objectMapper = new ObjectMapper();
    private String code; // 状态码
    private String msg; // 提示信息
    private T data; // 返回的数据

    /**
     * 默认构造方法，设置初始的状态码和提示信息
     */
    public Result() {
        this.code = AppCodeEnum.SUCCESS.getCode();
        this.msg = AppCodeEnum.SUCCESS.getDescription();
    }

    /**
     * 静态方法，返回一个错误结果
     */
    public static Result error() {
        return error(AppCodeEnum.ERROR.getCode(), AppCodeEnum.ERROR.getDescription());
    }

    /**
     * 静态方法，返回一个带自定义状态码和提示信息的错误结果
     */
    public static Result error(String code, String msg) {
        Result r = new Result();
        r.setCode(code);
        r.setMsg(msg);
        return r;
    }

    /**
     * 静态方法，返回一个带自定义状态码和自定义提示信息的错误结果
     */
    public static Result error(String msg) {
        return error(AppCodeEnum.ERROR.getCode(), msg);
    }

    /**
     * 静态方法，返回一个默认的成功结果
     */
    public static Result success() {
        return new Result();
    }

    /**
     * 静态方法，返回一个带自定义状态码和自定义提示信息的成功结果
     */
    public static Result success(String msg) {
        return success(AppCodeEnum.SUCCESS.getCode(), msg);
    }

    /**
     * 静态方法，返回一个带自定义状态码和提示信息的成功结果
     */
    public static Result success(String code, String msg) {
        Result r = new Result();
        r.setCode(code);
        r.setMsg(msg);
        return r;
    }

    /**
     * 静态方法，返回一个包含指定数据的成功结果
     */
    public static <T> Result<T> success(T data) {
        Result<T> r = new Result<>();
        r.setData(data);
        return r;
    }

    /**
     * 获取返回的数据
     */
    public Object getData() {
        return data;
    }

    /**
     * 设置返回的数据
     */
    public Result<T> setData(T data) {
        this.data = data;
        return this;
    }

    /**
     * 获取data里面的数据，并且设置泛型可以直接转换出对应的类型
     */
    public <T> T getData(Class<T> clazz) {
        try {
            // 将 data 转换为 JSON 字符串，再转换为指定类型
            String json = objectMapper.writeValueAsString(this.getData());
            return objectMapper.readValue(json, clazz);
        } catch (JsonProcessingException e) {
            throw new RuntimeException("JSON 解析失败", e);
        }
    }

    /**
     * 获取状态码
     */
    public String getCode() {
        return code;
    }

    /**
     * 设置状态码
     */
    public void setCode(String code) {
        this.code = code;
    }

    /**
     * 获取提示信息
     */
    public String getMsg() {
        return msg;
    }

    /**
     * 设置提示信息
     */
    public void setMsg(String msg) {
        this.msg = msg;
    }

    /**
     * 转换为JSON字符串
     */
    @Override
    public String toString() {
        try {
            return objectMapper.writeValueAsString(this);
        } catch (JsonProcessingException e) {
            throw new RuntimeException("JSON 序列化失败", e);
        }
    }
}
