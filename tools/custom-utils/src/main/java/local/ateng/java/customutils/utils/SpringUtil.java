package local.ateng.java.customutils.utils;

import org.springframework.beans.BeansException;
import org.springframework.beans.factory.NoSuchBeanDefinitionException;
import org.springframework.boot.context.properties.bind.Bindable;
import org.springframework.boot.context.properties.bind.Binder;
import org.springframework.context.*;
import org.springframework.core.ResolvableType;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Component;

import java.lang.annotation.Annotation;
import java.util.Map;

/**
 * Spring 工具类
 * 提供获取 Spring 上下文中的 Bean 的方法
 * <p>
 * 使用前请确保该类已被 Spring 扫描并注入（例如通过 @Component）
 *
 * @author Ateng
 * @since 2025-07-29
 */
@Component
public final class SpringUtil implements ApplicationContextAware, ApplicationEventPublisherAware {

    /**
     * Spring 上下文对象
     */
    private static ApplicationContext context;


    /**
     * Spring 事件发布器
     */
    private static ApplicationEventPublisher publisher;

    /**
     * 设置 Spring 上下文（由 Spring 自动调用）
     *
     * @param applicationContext Spring 上下文
     * @throws BeansException 设置异常
     */
    @Override
    public void setApplicationContext(ApplicationContext applicationContext) throws BeansException {
        if (SpringUtil.context == null) {
            SpringUtil.context = applicationContext;
        }
    }

    /**
     * 设置 Spring 事件发布器（由 Spring 自动调用）
     * <p>
     * 实现 {@link ApplicationEventPublisherAware} 接口后，Spring 启动时会自动注入事件发布器，
     * 可用于后续在静态方法中发布事件（如自定义事件、自定义通知等）。
     *
     * @param applicationEventPublisher Spring 提供的事件发布器
     */
    @Override
    public void setApplicationEventPublisher(ApplicationEventPublisher applicationEventPublisher) {
        if (SpringUtil.publisher == null) {
            SpringUtil.publisher = applicationEventPublisher;
        }
    }

    /**
     * 获取 Spring 上下文
     *
     * @return ApplicationContext
     */
    public static ApplicationContext getApplicationContext() {
        return context;
    }

    /**
     * 从 Spring 容器中获取指定类型的 Bean
     *
     * @param requiredType Bean 类型
     * @param <T>          泛型
     * @return Bean 实例
     */
    public static <T> T getBean(Class<T> requiredType) {
        return getApplicationContext().getBean(requiredType);
    }

    /**
     * 从 Spring 容器中获取指定名称的 Bean
     *
     * @param name Bean 名称
     * @return Bean 实例
     */
    public static Object getBean(String name) {
        return getApplicationContext().getBean(name);
    }

    /**
     * 从 Spring 容器中获取指定名称和类型的 Bean
     *
     * @param name         Bean 名称
     * @param requiredType Bean 类型
     * @param <T>          泛型
     * @return Bean 实例
     */
    public static <T> T getBean(String name, Class<T> requiredType) {
        return getApplicationContext().getBean(name, requiredType);
    }

    /**
     * 判断 Spring 容器中是否包含指定名称的 Bean
     *
     * @param name Bean 名称
     * @return 存在返回 true，否则返回 false
     */
    public static boolean containsBean(String name) {
        return getApplicationContext().containsBean(name);
    }

    /**
     * 判断指定名称的 Bean 是否为单例
     *
     * @param name Bean 名称
     * @return 是单例返回 true，否则返回 false
     */
    public static boolean isSingleton(String name) {
        return getApplicationContext().isSingleton(name);
    }

    /**
     * 获取指定名称 Bean 的类型
     *
     * @param name Bean 名称
     * @return Bean 类型
     */
    public static Class<?> getType(String name) {
        return getApplicationContext().getType(name);
    }

    /**
     * 获取指定类型的所有 Bean（包括泛型集合）
     *
     * @param type 类型
     * @param <T>  泛型
     * @return 类型对应的所有 Bean Map（BeanName -> Bean）
     */
    public static <T> Map<String, T> getBeansOfType(Class<T> type) {
        return context.getBeansOfType(type);
    }

    /**
     * 延迟获取 Bean（适合在非 Spring 管理类中使用）
     *
     * @param type Bean 类型
     * @param <T>  泛型
     * @return Bean 实例
     */
    public static <T> T lazyGetBean(Class<T> type) {
        return context.getAutowireCapableBeanFactory().createBean(type);
    }

    /**
     * 发布 Spring 应用事件
     *
     * @param event 事件对象
     */
    public static void publishEvent(ApplicationEvent event) {
        if (publisher != null) {
            publisher.publishEvent(event);
        }
    }

    /**
     * 发布任意对象作为事件（推荐 Spring 4.2+）
     *
     * @param event 任意对象
     */
    public static void publishEvent(Object event) {
        if (publisher != null) {
            publisher.publishEvent(event);
        }
    }

    /**
     * 获取当前激活的 Profile 数组（如 dev、test、prod）
     *
     * @return 激活的 profiles，若未设置则返回空数组
     */
    public static String[] getActiveProfiles() {
        return context.getEnvironment().getActiveProfiles();
    }

    /**
     * 判断指定 profile 是否被激活
     *
     * @param profile 配置 profile 名称
     * @return 被激活返回 true
     */
    public static boolean isProfileActive(String profile) {
        for (String activeProfile : getActiveProfiles()) {
            if (activeProfile.equals(profile)) {
                return true;
            }
        }
        return false;
    }

    /**
     * 获取 Spring 的 Environment 对象
     * 可用于访问配置、环境变量、激活的 profiles 等信息
     *
     * @return Environment 实例
     */
    public static Environment getEnvironment() {
        return context.getEnvironment();
    }

    /**
     * 获取指定环境变量（系统环境变量）
     *
     * @param name 环境变量名称（如 JAVA_HOME、PATH 等）
     * @return 对应值，若不存在返回 null
     */
    public static String getEnvironment(String name) {
        return System.getenv(name);
    }

    /**
     * 获取指定环境变量（带默认值）
     *
     * @param name         环境变量名称
     * @param defaultValue 默认值
     * @return 环境变量值或默认值
     */
    public static String getEnvironment(String name, String defaultValue) {
        String value = System.getenv(name);
        return value != null ? value : defaultValue;
    }

    /**
     * 获取指定 key 对应的配置值（application.yml 或 .properties 中）
     *
     * @param key 配置 key
     * @return 对应配置值，若不存在则返回 null
     */
    public static String getProperty(String key) {
        return getEnvironment().getProperty(key);
    }

    /**
     * 获取指定 key 对应的配置值，若不存在则返回默认值
     *
     * @param key          配置 key
     * @param defaultValue 默认值
     * @return 配置值或默认值
     */
    public static String getProperty(String key, String defaultValue) {
        return getEnvironment().getProperty(key, defaultValue);
    }

    /**
     * 获取指定配置项并转换为指定类型
     *
     * @param key        配置 key
     * @param targetType 目标类型
     * @param <T>        泛型类型
     * @return 类型转换后的值，若未找到或转换失败返回 null
     */
    public static <T> T getProperty(String key, Class<T> targetType) {
        return getEnvironment().getProperty(key, targetType);
    }

    /**
     * 获取指定配置项并转换为指定类型（支持默认值）
     *
     * @param key          配置 key
     * @param targetType   目标类型
     * @param defaultValue 默认值
     * @param <T>          泛型类型
     * @return 转换后的值或默认值
     */
    public static <T> T getProperty(String key, Class<T> targetType, T defaultValue) {
        return getEnvironment().getProperty(key, targetType, defaultValue);
    }

    /**
     * 使用 Binder 获取泛型配置（如 List、Map 等）
     *
     * <p>该方法适用于 Spring Boot 2.0 及以上版本，可读取复杂泛型类型配置。</p>
     *
     * <p><b>使用示例：</b></p>
     * <pre>{@code
     * // 读取 List<String>
     * ResolvableType listType = ResolvableType.forClassWithGenerics(List.class, String.class);
     * List<String> tags = SpringUtil.getGenericProperty("app.tags", listType);
     *
     * // 读取 Map<String, Integer>
     * ResolvableType mapType = ResolvableType.forClassWithGenerics(Map.class, String.class, Integer.class);
     * Map<String, Integer> scoreMap = SpringUtil.getGenericProperty("app.scores", mapType);
     * }</pre>
     *
     * @param key  配置项 key，例如 "app.tags"
     * @param type 泛型类型，可以通过 ResolvableType.forClassWithGenerics 创建
     * @param <T>  返回类型（如 List<String>、Map<String, Integer>）
     * @return 绑定的配置值，若未配置返回 null
     */
    public static <T> T getGenericProperty(String key, ResolvableType type) {
        Environment environment = getEnvironment();
        Binder binder = Binder.get(environment);

        Bindable<T> bindable = Bindable.of(type);
        return binder.bind(key, bindable).orElse(null);
    }

    /**
     * 根据类型获取所有符合的 Bean 名称数组
     *
     * @param type Bean 类型
     * @return Bean 名称数组，若无匹配返回空数组
     */
    public static String[] getBeanNamesForType(Class<?> type) {
        if (context == null) {
            return new String[0];
        }
        return context.getBeanNamesForType(type);
    }

    /**
     * 获取容器中所有带某个注解的 Bean 实例（Map，key 为 Bean 名称）
     *
     * @param annotationType 注解类型
     * @param <A>            注解泛型
     * @return Bean 名称-实例映射，若无匹配返回空 Map
     */
    public static <A extends Annotation> Map<String, Object> getBeansWithAnnotation(Class<A> annotationType) {
        if (context == null) {
            return java.util.Collections.emptyMap();
        }
        return context.getBeansWithAnnotation(annotationType);
    }

    /**
     * 判断指定名称的 Bean 是否存在于 Spring 容器中
     *
     * @param name Bean 名称
     * @return 存在返回 true，否则 false
     */
    public static boolean isBeanPresent(String name) {
        if (context == null) {
            return false;
        }
        return context.containsBean(name);
    }

    /**
     * 清空 Spring 静态上下文引用（仅用于测试或特殊场景）
     */
    public static void clearApplicationContext() {
        context = null;
    }

    /**
     * 获取当前上下文中 Bean 定义的数量
     *
     * @return Bean 定义数量，若 context 为空返回 0
     */
    public static int getBeanDefinitionCount() {
        if (context == null) {
            return 0;
        }
        return context.getBeanDefinitionCount();
    }

    /**
     * 获取所有 Bean 的名称数组
     *
     * @return Bean 名称数组，若 context 为空返回空数组
     */
    public static String[] getBeanDefinitionNames() {
        if (context == null) {
            return new String[0];
        }
        return context.getBeanDefinitionNames();
    }

    /**
     * 判断指定 Bean 是否匹配给定类型（包括继承或接口实现）
     *
     * @param beanName Bean 名称
     * @param type     类型 Class
     * @return 匹配返回 true，否则 false；Bean 不存在也返回 false
     */
    public static boolean isTypeMatch(String beanName, Class<?> type) {
        if (context == null) {
            return false;
        }
        try {
            return context.isTypeMatch(beanName, type);
        } catch (NoSuchBeanDefinitionException e) {
            return false;
        }
    }

    /**
     * 解析带有 ${...} 占位符的字符串，解析 Spring 配置的占位符
     *
     * @param value 带有占位符的字符串，如 "${app.name}"
     * @return 解析后的字符串，无法解析返回原字符串
     */
    public static String resolveEmbeddedValue(String value) {
        if (context == null) {
            return value;
        }
        return context.getEnvironment().resolvePlaceholders(value);
    }

}
