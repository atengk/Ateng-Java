package io.github.atengk.service;

/**
 * 双重检查锁（Double-Checked Locking，DCL）单例实现。
 *
 * <p>说明：
 * 1. 这是经典的懒加载单例实现，适合不使用 Spring 管理、但想在第一次使用时才初始化的场景。
 * 2. 该实现使用 volatile 修饰实例变量以保证可见性，避免指令重排问题。
 * 3. 在 Spring 管理环境中通常不推荐使用手写单例，除非有特殊理由。
 * </p>
 */
public final class DoubleCheckedLockingSingleton {

    /**
     * 单例实例，使用 volatile 防止指令重排导致的安全问题。
     */
    private static volatile DoubleCheckedLockingSingleton instance;

    /**
     * 私有构造函数，防止外部直接实例化。
     */
    private DoubleCheckedLockingSingleton() {
        // Prevent instantiation
    }

    /**
     * 获取单例实例。使用双重检查锁以降低同步开销。
     *
     * @return 单例实例
     */
    public static DoubleCheckedLockingSingleton getInstance() {
        if (instance == null) {
            synchronized (DoubleCheckedLockingSingleton.class) {
                if (instance == null) {
                    instance = new DoubleCheckedLockingSingleton();
                }
            }
        }
        return instance;
    }

    /**
     * 示例方法，返回一个字符串以示作用。
     *
     * @return 示例字符串
     */
    public String hello() {
        return "hello from DCL singleton";
    }
}
