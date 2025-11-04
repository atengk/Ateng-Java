package io.github.atengk.service;

/**
 * 枚举单例实现。
 *
 * <p>说明：
 * 1. 使用枚举实现单例是最简单、最安全的单例实现方式，能防止反射、序列化导致的多实例问题。
 * 2. 适用于独立工具类或在非 Spring 管理环境下需要单例的场景。
 * </p>
 */
public enum EnumSingleton {
    /**
     * 单例实例。
     */
    INSTANCE;

    /**
     * 示例状态或配置字段，演示可在单例中持有状态。
     */
    private String configValue = "default";

    /**
     * 获取配置值。
     *
     * @return 当前配置值
     */
    public String getConfigValue() {
        return configValue;
    }

    /**
     * 设置配置值。
     *
     * @param configValue 新的配置值
     */
    public void setConfigValue(String configValue) {
        this.configValue = configValue;
    }
}
