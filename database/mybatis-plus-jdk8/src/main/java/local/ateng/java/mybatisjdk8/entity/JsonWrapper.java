package local.ateng.java.mybatisjdk8.entity;

import java.io.Serializable;

/**
 * JSON 字段包装基类
 * <p>
 * 用于承载数据库 JSON 字段的泛型结构，避免直接使用裸泛型导致的类型擦除问题。
 *
 * @author Ateng
 * @since 2026-04-12
 */
public abstract class JsonWrapper<T> implements Serializable {

    /**
     * 实际数据
     */
    private T value;

    /**
     * 无参构造方法
     */
    public JsonWrapper() {
    }

    /**
     * 带参构造方法
     *
     * @param value 实际数据
     */
    public JsonWrapper(T value) {
        this.value = value;
    }

    /**
     * 获取实际数据
     *
     * @return value
     */
    public T getValue() {
        return value;
    }

    /**
     * 设置实际数据
     *
     * @param value 数据
     */
    public void setValue(T value) {
        this.value = value;
    }
}