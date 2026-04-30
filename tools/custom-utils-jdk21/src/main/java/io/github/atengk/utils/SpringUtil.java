package io.github.atengk.utils;

import jakarta.servlet.RequestDispatcher;
import jakarta.servlet.ServletContext;
import jakarta.servlet.ServletException;
import jakarta.servlet.ServletInputStream;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import org.springframework.aop.framework.Advised;
import org.springframework.aop.support.AopUtils;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.NoSuchBeanDefinitionException;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.config.AutowireCapableBeanFactory;
import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;
import org.springframework.beans.factory.support.BeanDefinitionRegistry;
import org.springframework.boot.context.properties.bind.Bindable;
import org.springframework.boot.context.properties.bind.Binder;
import org.springframework.boot.info.BuildProperties;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.context.*;
import org.springframework.context.i18n.LocaleContextHolder;
import org.springframework.core.annotation.AnnotatedElementUtils;
import org.springframework.core.annotation.AnnotationAwareOrderComparator;
import org.springframework.core.env.Environment;
import org.springframework.core.io.Resource;
import org.springframework.core.io.ResourceLoader;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;
import org.springframework.core.io.support.ResourcePatternResolver;
import org.springframework.core.task.AsyncTaskExecutor;
import org.springframework.core.task.TaskExecutor;
import org.springframework.scheduling.TaskScheduler;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.util.ClassUtils;
import org.springframework.web.context.WebApplicationContext;
import org.springframework.web.context.request.RequestAttributes;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.io.*;
import java.lang.annotation.Annotation;
import java.lang.reflect.Array;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;
import java.util.*;
import java.util.function.Supplier;

/**
 * Spring 通用工具类，提供 Spring 容器、Bean、环境配置、Servlet、事务、缓存、事件等常用静态访问能力。
 *
 * @author Ateng
 * @since 2026-04-30
 */
public final class SpringUtil {

    private static final String UNKNOWN = "unknown";
    private static final String APPLICATION_JSON_UTF8 = "application/json;charset=UTF-8";
    private static final String TEXT_PLAIN_UTF8 = "text/plain;charset=UTF-8";
    private static final List<String> CLIENT_IP_HEADERS = List.of(
            "X-Forwarded-For",
            "X-Real-IP",
            "Proxy-Client-IP",
            "WL-Proxy-Client-IP",
            "HTTP_CLIENT_IP",
            "HTTP_X_FORWARDED_FOR"
    );

    private static volatile ApplicationContext applicationContext;

    private SpringUtil() {
        throw new UnsupportedOperationException("SpringUtil 是静态工具类，不能实例化");
    }

    /**
     * 设置 Spring 应用上下文。
     *
     * @param context Spring 应用上下文，传入 null 表示清空上下文引用
     */
    public static void setApplicationContext(ApplicationContext context) {
        applicationContext = context;
    }

    /**
     * 获取当前 Spring 应用上下文。
     *
     * @return Spring 应用上下文，未初始化时返回 null
     */
    public static ApplicationContext getApplicationContext() {
        return applicationContext;
    }

    /**
     * 获取当前 Spring 应用上下文，未初始化时抛出异常。
     *
     * @return Spring 应用上下文
     */
    public static ApplicationContext requireApplicationContext() {
        ApplicationContext context = applicationContext;
        if (context == null) {
            throw new IllegalStateException("Spring ApplicationContext 尚未初始化");
        }
        return context;
    }

    /**
     * 判断 Spring 应用上下文是否已可用。
     *
     * @return 上下文存在且处于活动状态时返回 true
     */
    public static boolean isContextReady() {
        ApplicationContext context = applicationContext;
        if (context == null) {
            return false;
        }
        if (context instanceof ConfigurableApplicationContext configurableApplicationContext) {
            return configurableApplicationContext.isActive();
        }
        return true;
    }

    /**
     * 获取可配置 BeanFactory。
     *
     * @return 可配置 BeanFactory
     */
    public static ConfigurableListableBeanFactory getBeanFactory() {
        ApplicationContext context = requireApplicationContext();
        if (context instanceof ConfigurableApplicationContext configurableApplicationContext) {
            return configurableApplicationContext.getBeanFactory();
        }
        throw new IllegalStateException("当前 ApplicationContext 不支持 ConfigurableListableBeanFactory");
    }

    /**
     * 获取支持自动装配的 BeanFactory。
     *
     * @return 自动装配 BeanFactory
     */
    public static AutowireCapableBeanFactory getAutowireCapableBeanFactory() {
        return requireApplicationContext().getAutowireCapableBeanFactory();
    }

    /**
     * 获取 Spring 环境对象。
     *
     * @return Spring Environment
     */
    public static Environment getEnvironment() {
        return requireApplicationContext().getEnvironment();
    }

    /**
     * 根据类型获取 Bean。
     *
     * @param beanType Bean 类型
     * @param <T>      Bean 泛型
     * @return Bean 实例
     */
    public static <T> T getBean(Class<T> beanType) {
        requireNonNull(beanType, "beanType 不能为空");
        return requireApplicationContext().getBean(beanType);
    }

    /**
     * 根据名称获取 Bean。
     *
     * @param beanName Bean 名称
     * @return Bean 实例
     */
    public static Object getBean(String beanName) {
        requireText(beanName, "beanName 不能为空");
        return requireApplicationContext().getBean(beanName);
    }

    /**
     * 根据名称和类型获取 Bean。
     *
     * @param beanName Bean 名称
     * @param beanType Bean 类型
     * @param <T>      Bean 泛型
     * @return Bean 实例
     */
    public static <T> T getBean(String beanName, Class<T> beanType) {
        requireText(beanName, "beanName 不能为空");
        requireNonNull(beanType, "beanType 不能为空");
        return requireApplicationContext().getBean(beanName, beanType);
    }

    /**
     * 根据类型获取 Bean，获取失败时返回 null。
     *
     * @param beanType Bean 类型
     * @param <T>      Bean 泛型
     * @return Bean 实例，获取失败时返回 null
     */
    public static <T> T getBeanOrNull(Class<T> beanType) {
        requireNonNull(beanType, "beanType 不能为空");
        try {
            return requireApplicationContext().getBean(beanType);
        } catch (BeansException | IllegalStateException ex) {
            return null;
        }
    }

    /**
     * 根据名称获取 Bean，获取失败时返回 null。
     *
     * @param beanName Bean 名称
     * @return Bean 实例，获取失败时返回 null
     */
    public static Object getBeanOrNull(String beanName) {
        requireText(beanName, "beanName 不能为空");
        try {
            return requireApplicationContext().getBean(beanName);
        } catch (BeansException | IllegalStateException ex) {
            return null;
        }
    }

    /**
     * 根据类型获取 BeanProvider。
     *
     * @param beanType Bean 类型
     * @param <T>      Bean 泛型
     * @return ObjectProvider 实例
     */
    public static <T> ObjectProvider<T> getBeanProvider(Class<T> beanType) {
        requireNonNull(beanType, "beanType 不能为空");
        return requireApplicationContext().getBeanProvider(beanType);
    }

    /**
     * 根据类型获取所有 Bean。
     *
     * @param beanType Bean 类型
     * @param <T>      Bean 泛型
     * @return Bean 名称与实例映射
     */
    public static <T> Map<String, T> getBeansOfType(Class<T> beanType) {
        requireNonNull(beanType, "beanType 不能为空");
        return requireApplicationContext().getBeansOfType(beanType);
    }

    /**
     * 根据类型获取 Bean 映射。
     *
     * @param beanType Bean 类型
     * @param <T>      Bean 泛型
     * @return Bean 名称与实例映射
     */
    public static <T> Map<String, T> getBeanMap(Class<T> beanType) {
        return new LinkedHashMap<>(getBeansOfType(beanType));
    }

    /**
     * 根据类型获取 Bean 列表，并按照 Spring 排序规则排序。
     *
     * @param beanType Bean 类型
     * @param <T>      Bean 泛型
     * @return Bean 实例列表
     */
    public static <T> List<T> getBeanList(Class<T> beanType) {
        List<T> beans = new ArrayList<>(getBeansOfType(beanType).values());
        AnnotationAwareOrderComparator.sort(beans);
        return beans;
    }

    /**
     * 判断指定名称的 Bean 是否存在。
     *
     * @param beanName Bean 名称
     * @return 存在时返回 true
     */
    public static boolean containsBean(String beanName) {
        requireText(beanName, "beanName 不能为空");
        return requireApplicationContext().containsBean(beanName);
    }

    /**
     * 判断指定类型的 Bean 是否存在。
     *
     * @param beanType Bean 类型
     * @return 存在时返回 true
     */
    public static boolean containsBean(Class<?> beanType) {
        requireNonNull(beanType, "beanType 不能为空");
        return getBeanNamesForType(beanType).length > 0;
    }

    /**
     * 判断指定名称的 BeanDefinition 是否存在。
     *
     * @param beanName Bean 名称
     * @return 存在时返回 true
     */
    public static boolean containsBeanDefinition(String beanName) {
        requireText(beanName, "beanName 不能为空");
        return requireApplicationContext().containsBeanDefinition(beanName);
    }

    /**
     * 判断指定 Bean 是否为单例。
     *
     * @param beanName Bean 名称
     * @return 单例时返回 true
     */
    public static boolean isSingleton(String beanName) {
        requireText(beanName, "beanName 不能为空");
        return requireApplicationContext().isSingleton(beanName);
    }

    /**
     * 判断指定 Bean 是否为原型。
     *
     * @param beanName Bean 名称
     * @return 原型时返回 true
     */
    public static boolean isPrototype(String beanName) {
        requireText(beanName, "beanName 不能为空");
        return requireApplicationContext().isPrototype(beanName);
    }

    /**
     * 判断指定 Bean 是否匹配目标类型。
     *
     * @param beanName Bean 名称
     * @param beanType 目标类型
     * @return 匹配时返回 true
     */
    public static boolean isTypeMatch(String beanName, Class<?> beanType) {
        requireText(beanName, "beanName 不能为空");
        requireNonNull(beanType, "beanType 不能为空");
        return requireApplicationContext().isTypeMatch(beanName, beanType);
    }

    /**
     * 获取指定 Bean 的类型。
     *
     * @param beanName Bean 名称
     * @return Bean 类型，无法判断时返回 null
     */
    public static Class<?> getType(String beanName) {
        requireText(beanName, "beanName 不能为空");
        return requireApplicationContext().getType(beanName);
    }

    /**
     * 获取指定 Bean 的别名。
     *
     * @param beanName Bean 名称
     * @return Bean 别名数组
     */
    public static String[] getAliases(String beanName) {
        requireText(beanName, "beanName 不能为空");
        return requireApplicationContext().getAliases(beanName);
    }

    /**
     * 获取所有 BeanDefinition 名称。
     *
     * @return Bean 名称数组
     */
    public static String[] getBeanNames() {
        return requireApplicationContext().getBeanDefinitionNames();
    }

    /**
     * 根据类型获取 Bean 名称数组。
     *
     * @param beanType Bean 类型
     * @return Bean 名称数组
     */
    public static String[] getBeanNamesForType(Class<?> beanType) {
        requireNonNull(beanType, "beanType 不能为空");
        return requireApplicationContext().getBeanNamesForType(beanType);
    }

    /**
     * 获取配置属性值。
     *
     * @param key 属性键
     * @return 属性值，不存在时返回 null
     */
    public static String getProperty(String key) {
        requireText(key, "key 不能为空");
        return getEnvironment().getProperty(key);
    }

    /**
     * 获取配置属性值，不存在时返回默认值。
     *
     * @param key          属性键
     * @param defaultValue 默认值
     * @return 属性值
     */
    public static String getProperty(String key, String defaultValue) {
        requireText(key, "key 不能为空");
        return getEnvironment().getProperty(key, defaultValue);
    }

    /**
     * 获取指定类型的配置属性值。
     *
     * @param key        属性键
     * @param targetType 目标类型
     * @param <T>        属性泛型
     * @return 属性值，不存在时返回 null
     */
    public static <T> T getProperty(String key, Class<T> targetType) {
        requireText(key, "key 不能为空");
        requireNonNull(targetType, "targetType 不能为空");
        try {
            return getEnvironment().getProperty(key, targetType);
        } catch (RuntimeException ex) {
            throw new IllegalArgumentException("配置属性格式错误：" + key, ex);
        }
    }

    /**
     * 获取必填配置属性值。
     *
     * @param key 属性键
     * @return 属性值
     */
    public static String getRequiredProperty(String key) {
        requireText(key, "key 不能为空");
        return getEnvironment().getRequiredProperty(key);
    }

    /**
     * 获取指定类型的必填配置属性值。
     *
     * @param key        属性键
     * @param targetType 目标类型
     * @param <T>        属性泛型
     * @return 属性值
     */
    public static <T> T getRequiredProperty(String key, Class<T> targetType) {
        requireText(key, "key 不能为空");
        requireNonNull(targetType, "targetType 不能为空");
        try {
            return getEnvironment().getRequiredProperty(key, targetType);
        } catch (RuntimeException ex) {
            throw new IllegalArgumentException("必填配置属性不存在或格式错误：" + key, ex);
        }
    }

    /**
     * 判断配置属性是否存在。
     *
     * @param key 属性键
     * @return 存在时返回 true
     */
    public static boolean containsProperty(String key) {
        requireText(key, "key 不能为空");
        return getEnvironment().containsProperty(key);
    }

    /**
     * 获取 Integer 类型配置属性。
     *
     * @param key          属性键
     * @param defaultValue 默认值
     * @return Integer 属性值
     */
    public static Integer getPropertyAsInt(String key, Integer defaultValue) {
        Integer value = getProperty(key, Integer.class);
        return value == null ? defaultValue : value;
    }

    /**
     * 获取 Long 类型配置属性。
     *
     * @param key          属性键
     * @param defaultValue 默认值
     * @return Long 属性值
     */
    public static Long getPropertyAsLong(String key, Long defaultValue) {
        Long value = getProperty(key, Long.class);
        return value == null ? defaultValue : value;
    }

    /**
     * 获取 Boolean 类型配置属性。
     *
     * @param key          属性键
     * @param defaultValue 默认值
     * @return Boolean 属性值
     */
    public static Boolean getPropertyAsBoolean(String key, Boolean defaultValue) {
        Boolean value = getProperty(key, Boolean.class);
        return value == null ? defaultValue : value;
    }

    /**
     * 获取逗号分隔的字符串列表配置属性。
     *
     * @param key 属性键
     * @return 字符串列表，不存在时返回空列表
     */
    public static List<String> getPropertyAsList(String key) {
        requireText(key, "key 不能为空");
        String value = getProperty(key);
        if (value == null || value.isBlank()) {
            return List.of();
        }
        return Arrays.stream(value.split(","))
                .map(String::trim)
                .filter(item -> !item.isEmpty())
                .toList();
    }

    /**
     * 获取当前激活的 Profile。
     *
     * @return 激活 Profile 列表
     */
    public static List<String> getActiveProfiles() {
        return List.of(getEnvironment().getActiveProfiles());
    }

    /**
     * 获取默认 Profile。
     *
     * @return 默认 Profile 列表
     */
    public static List<String> getDefaultProfiles() {
        return List.of(getEnvironment().getDefaultProfiles());
    }

    /**
     * 判断指定 Profile 是否激活。
     *
     * @param profile Profile 名称
     * @return 激活时返回 true
     */
    public static boolean isProfileActive(String profile) {
        requireText(profile, "profile 不能为空");
        return getActiveProfiles().contains(profile);
    }

    /**
     * 判断任意一个 Profile 是否激活。
     *
     * @param profiles Profile 名称数组
     * @return 任意一个激活时返回 true
     */
    public static boolean isAnyProfileActive(String... profiles) {
        requireNonEmptyArray(profiles, "profiles 不能为空");
        return Arrays.stream(profiles).anyMatch(SpringUtil::isProfileActive);
    }

    /**
     * 判断所有 Profile 是否均已激活。
     *
     * @param profiles Profile 名称数组
     * @return 全部激活时返回 true
     */
    public static boolean isAllProfileActive(String... profiles) {
        requireNonEmptyArray(profiles, "profiles 不能为空");
        return Arrays.stream(profiles).allMatch(SpringUtil::isProfileActive);
    }

    /**
     * 判断是否为 local 环境。
     *
     * @return local Profile 激活时返回 true
     */
    public static boolean isLocal() {
        return isProfileActiveSafely("local");
    }

    /**
     * 判断是否为 dev 环境。
     *
     * @return dev Profile 激活时返回 true
     */
    public static boolean isDev() {
        return isProfileActiveSafely("dev");
    }

    /**
     * 判断是否为 test 环境。
     *
     * @return test Profile 激活时返回 true
     */
    public static boolean isTest() {
        return isProfileActiveSafely("test");
    }

    /**
     * 判断是否为 prod 环境。
     *
     * @return prod Profile 激活时返回 true
     */
    public static boolean isProd() {
        return isProfileActiveSafely("prod");
    }

    /**
     * 对手动创建的对象执行依赖注入。
     *
     * @param target 目标对象
     */
    public static void autowireBean(Object target) {
        requireNonNull(target, "target 不能为空");
        getAutowireCapableBeanFactory().autowireBean(target);
    }

    /**
     * 初始化手动创建的 Bean。
     *
     * @param target   目标对象
     * @param beanName Bean 名称
     * @return 初始化后的 Bean
     */
    public static Object initializeBean(Object target, String beanName) {
        requireNonNull(target, "target 不能为空");
        requireText(beanName, "beanName 不能为空");
        return getAutowireCapableBeanFactory().initializeBean(target, beanName);
    }

    /**
     * 销毁手动创建的 Bean。
     *
     * @param target 目标对象
     */
    public static void destroyBean(Object target) {
        requireNonNull(target, "target 不能为空");
        getAutowireCapableBeanFactory().destroyBean(target);
    }

    /**
     * 创建由 Spring 管理生命周期的 Bean。
     *
     * @param beanType Bean 类型
     * @param <T>      Bean 泛型
     * @return Bean 实例
     */
    public static <T> T createBean(Class<T> beanType) {
        requireNonNull(beanType, "beanType 不能为空");
        return getAutowireCapableBeanFactory().createBean(beanType);
    }

    /**
     * 注册单例 Bean。
     *
     * @param beanName        Bean 名称
     * @param singletonObject 单例对象
     */
    public static void registerSingleton(String beanName, Object singletonObject) {
        requireText(beanName, "beanName 不能为空");
        requireNonNull(singletonObject, "singletonObject 不能为空");
        getBeanFactory().registerSingleton(beanName, singletonObject);
    }

    /**
     * 移除 BeanDefinition。
     *
     * @param beanName Bean 名称
     */
    public static void removeBeanDefinition(String beanName) {
        requireText(beanName, "beanName 不能为空");
        ConfigurableListableBeanFactory beanFactory = getBeanFactory();
        if (beanFactory instanceof BeanDefinitionRegistry registry) {
            if (registry.containsBeanDefinition(beanName)) {
                registry.removeBeanDefinition(beanName);
            }
            return;
        }
        throw new IllegalStateException("当前 BeanFactory 不支持移除 BeanDefinition");
    }

    /**
     * 发布 Spring 应用事件。
     *
     * @param event 应用事件
     */
    public static void publishEvent(Object event) {
        requireNonNull(event, "event 不能为空");
        requireApplicationContext().publishEvent(event);
    }

    /**
     * 发布 Spring ApplicationEvent 事件。
     *
     * @param event 应用事件
     */
    public static void publishEvent(ApplicationEvent event) {
        requireNonNull(event, "event 不能为空");
        requireApplicationContext().publishEvent(event);
    }

    /**
     * 安全发布 Spring 应用事件，发布失败时返回 false。
     *
     * @param event 应用事件
     * @return 发布成功时返回 true
     */
    public static boolean publishEventSafely(Object event) {
        try {
            publishEvent(event);
            return true;
        } catch (RuntimeException ex) {
            return false;
        }
    }

    /**
     * 根据消息编码获取国际化消息。
     *
     * @param code 消息编码
     * @return 国际化消息
     */
    public static String getMessage(String code) {
        return getMessage(code, null, null, LocaleContextHolder.getLocale());
    }

    /**
     * 根据消息编码和参数获取国际化消息。
     *
     * @param code 消息编码
     * @param args 消息参数
     * @return 国际化消息
     */
    public static String getMessage(String code, Object... args) {
        return getMessage(code, args, null, LocaleContextHolder.getLocale());
    }

    /**
     * 根据消息编码获取国际化消息，不存在时返回默认消息。
     *
     * @param code           消息编码
     * @param defaultMessage 默认消息
     * @return 国际化消息
     */
    public static String getMessage(String code, String defaultMessage) {
        return getMessage(code, null, defaultMessage, LocaleContextHolder.getLocale());
    }

    /**
     * 根据消息编码、参数和默认消息获取国际化消息。
     *
     * @param code           消息编码
     * @param args           消息参数
     * @param defaultMessage 默认消息
     * @return 国际化消息
     */
    public static String getMessage(String code, Object[] args, String defaultMessage) {
        return getMessage(code, args, defaultMessage, LocaleContextHolder.getLocale());
    }

    /**
     * 根据消息编码和语言区域获取国际化消息。
     *
     * @param code   消息编码
     * @param locale 语言区域
     * @return 国际化消息
     */
    public static String getMessage(String code, Locale locale) {
        return getMessage(code, null, null, locale);
    }

    /**
     * 根据消息编码、参数和语言区域获取国际化消息。
     *
     * @param code   消息编码
     * @param args   消息参数
     * @param locale 语言区域
     * @return 国际化消息
     */
    public static String getMessage(String code, Object[] args, Locale locale) {
        return getMessage(code, args, null, locale);
    }

    /**
     * 获取 Spring Resource。
     *
     * @param location 资源位置
     * @return Resource 对象
     */
    public static Resource getResource(String location) {
        requireText(location, "location 不能为空");
        ApplicationContext context = requireApplicationContext();
        if (context instanceof ResourceLoader resourceLoader) {
            return resourceLoader.getResource(location);
        }
        return new PathMatchingResourcePatternResolver().getResource(location);
    }

    /**
     * 根据资源路径模式获取多个 Resource。
     *
     * @param locationPattern 资源路径模式
     * @return Resource 数组
     */
    public static Resource[] getResources(String locationPattern) {
        requireText(locationPattern, "locationPattern 不能为空");
        try {
            ApplicationContext context = requireApplicationContext();
            ResourcePatternResolver resolver = context instanceof ResourcePatternResolver resourcePatternResolver
                    ? resourcePatternResolver
                    : new PathMatchingResourcePatternResolver(context);
            return resolver.getResources(locationPattern);
        } catch (IOException ex) {
            throw new UncheckedIOException("读取资源失败：" + locationPattern, ex);
        }
    }

    /**
     * 判断资源是否存在。
     *
     * @param location 资源位置
     * @return 存在时返回 true
     */
    public static boolean resourceExists(String location) {
        return getResource(location).exists();
    }

    /**
     * 获取资源输入流。
     *
     * @param location 资源位置
     * @return 输入流
     */
    public static InputStream getResourceAsStream(String location) {
        try {
            return getResource(location).getInputStream();
        } catch (IOException ex) {
            throw new UncheckedIOException("打开资源输入流失败：" + location, ex);
        }
    }

    /**
     * 按 UTF-8 读取资源文本。
     *
     * @param location 资源位置
     * @return 资源文本
     */
    public static String readResourceAsString(String location) {
        return new String(readResourceAsBytes(location), StandardCharsets.UTF_8);
    }

    /**
     * 读取资源字节数组。
     *
     * @param location 资源位置
     * @return 资源字节数组
     */
    public static byte[] readResourceAsBytes(String location) {
        try (InputStream inputStream = getResourceAsStream(location)) {
            return inputStream.readAllBytes();
        } catch (IOException ex) {
            throw new UncheckedIOException("读取资源失败：" + location, ex);
        }
    }

    /**
     * 判断对象是否为 Spring AOP 代理对象。
     *
     * @param bean Bean 对象
     * @return 是代理对象时返回 true
     */
    public static boolean isAopProxy(Object bean) {
        return bean != null && AopUtils.isAopProxy(bean);
    }

    /**
     * 判断对象是否为 JDK 动态代理。
     *
     * @param bean Bean 对象
     * @return 是 JDK 动态代理时返回 true
     */
    public static boolean isJdkDynamicProxy(Object bean) {
        return bean != null && AopUtils.isJdkDynamicProxy(bean);
    }

    /**
     * 判断对象是否为 CGLIB 代理。
     *
     * @param bean Bean 对象
     * @return 是 CGLIB 代理时返回 true
     */
    public static boolean isCglibProxy(Object bean) {
        return bean != null && AopUtils.isCglibProxy(bean);
    }

    /**
     * 获取对象的目标类型。
     *
     * @param bean Bean 对象
     * @return 目标类型
     */
    public static Class<?> getTargetClass(Object bean) {
        requireNonNull(bean, "bean 不能为空");
        return AopUtils.getTargetClass(bean);
    }

    /**
     * 获取代理对象的最终目标对象。
     *
     * @param proxy 代理对象
     * @return 目标对象
     */
    public static Object getTargetObject(Object proxy) {
        requireNonNull(proxy, "proxy 不能为空");
        Object current = proxy;
        Set<Object> visited = Collections.newSetFromMap(new IdentityHashMap<>());
        while (current instanceof Advised advised && visited.add(current)) {
            try {
                Object target = advised.getTargetSource().getTarget();
                if (target == null) {
                    return current;
                }
                current = target;
            } catch (Exception ex) {
                throw new IllegalStateException("获取 AOP 目标对象失败", ex);
            }
        }
        return current;
    }

    /**
     * 获取代理对象的最终目标类型。
     *
     * @param bean Bean 对象
     * @return 最终目标类型
     */
    public static Class<?> getUltimateTargetClass(Object bean) {
        requireNonNull(bean, "bean 不能为空");
        return ClassUtils.getUserClass(AopUtils.getTargetClass(getTargetObject(bean)));
    }

    /**
     * 查找类上的合并注解。
     *
     * @param targetClass    目标类
     * @param annotationType 注解类型
     * @param <A>            注解泛型
     * @return 注解实例，不存在时返回 null
     */
    public static <A extends Annotation> A findAnnotation(Class<?> targetClass, Class<A> annotationType) {
        return findMergedAnnotation(targetClass, annotationType);
    }

    /**
     * 查找方法上的合并注解。
     *
     * @param method         目标方法
     * @param annotationType 注解类型
     * @param <A>            注解泛型
     * @return 注解实例，不存在时返回 null
     */
    public static <A extends Annotation> A findAnnotation(Method method, Class<A> annotationType) {
        return findMergedAnnotation(method, annotationType);
    }

    /**
     * 获取类上的直接注解。
     *
     * @param targetClass    目标类
     * @param annotationType 注解类型
     * @param <A>            注解泛型
     * @return 注解实例，不存在时返回 null
     */
    public static <A extends Annotation> A getAnnotation(Class<?> targetClass, Class<A> annotationType) {
        requireNonNull(targetClass, "targetClass 不能为空");
        requireNonNull(annotationType, "annotationType 不能为空");
        return targetClass.getAnnotation(annotationType);
    }

    /**
     * 判断类上是否存在指定注解。
     *
     * @param targetClass    目标类
     * @param annotationType 注解类型
     * @return 存在时返回 true
     */
    public static boolean hasAnnotation(Class<?> targetClass, Class<? extends Annotation> annotationType) {
        return findMergedAnnotation(targetClass, annotationType) != null;
    }

    /**
     * 判断方法上是否存在指定注解。
     *
     * @param method         目标方法
     * @param annotationType 注解类型
     * @return 存在时返回 true
     */
    public static boolean hasMethodAnnotation(Method method, Class<? extends Annotation> annotationType) {
        return findMergedAnnotation(method, annotationType) != null;
    }

    /**
     * 查找类上的合并注解，支持组合注解和元注解。
     *
     * @param targetClass    目标类
     * @param annotationType 注解类型
     * @param <A>            注解泛型
     * @return 注解实例，不存在时返回 null
     */
    public static <A extends Annotation> A findMergedAnnotation(Class<?> targetClass, Class<A> annotationType) {
        requireNonNull(targetClass, "targetClass 不能为空");
        requireNonNull(annotationType, "annotationType 不能为空");
        return AnnotatedElementUtils.findMergedAnnotation(targetClass, annotationType);
    }

    /**
     * 查找方法上的合并注解，支持组合注解和元注解。
     *
     * @param method         目标方法
     * @param annotationType 注解类型
     * @param <A>            注解泛型
     * @return 注解实例，不存在时返回 null
     */
    public static <A extends Annotation> A findMergedAnnotation(Method method, Class<A> annotationType) {
        requireNonNull(method, "method 不能为空");
        requireNonNull(annotationType, "annotationType 不能为空");
        return AnnotatedElementUtils.findMergedAnnotation(method, annotationType);
    }

    /**
     * 获取当前请求属性。
     *
     * @return 请求属性，不存在时返回 null
     */
    public static RequestAttributes getRequestAttributes() {
        return RequestContextHolder.getRequestAttributes();
    }

    /**
     * 获取当前 Servlet 请求属性。
     *
     * @return Servlet 请求属性，不存在时返回 null
     */
    public static ServletRequestAttributes getServletRequestAttributes() {
        RequestAttributes attributes = getRequestAttributes();
        return attributes instanceof ServletRequestAttributes servletRequestAttributes ? servletRequestAttributes : null;
    }

    /**
     * 获取当前 HttpServletRequest。
     *
     * @return 当前请求对象，不存在时返回 null
     */
    public static HttpServletRequest getRequest() {
        ServletRequestAttributes attributes = getServletRequestAttributes();
        return attributes == null ? null : attributes.getRequest();
    }

    /**
     * 获取当前 HttpServletRequest，不存在时抛出异常。
     *
     * @return 当前请求对象
     */
    public static HttpServletRequest requireRequest() {
        HttpServletRequest request = getRequest();
        if (request == null) {
            throw new IllegalStateException("当前线程不存在 HttpServletRequest");
        }
        return request;
    }

    /**
     * 获取当前 HttpServletResponse。
     *
     * @return 当前响应对象，不存在时返回 null
     */
    public static HttpServletResponse getResponse() {
        ServletRequestAttributes attributes = getServletRequestAttributes();
        return attributes == null ? null : attributes.getResponse();
    }

    /**
     * 获取当前 HttpServletResponse，不存在时抛出异常。
     *
     * @return 当前响应对象
     */
    public static HttpServletResponse requireResponse() {
        HttpServletResponse response = getResponse();
        if (response == null) {
            throw new IllegalStateException("当前线程不存在 HttpServletResponse");
        }
        return response;
    }

    /**
     * 获取当前 Session，不存在时不创建。
     *
     * @return 当前 Session，不存在时返回 null
     */
    public static HttpSession getSession() {
        return getSession(false);
    }

    /**
     * 获取当前 Session。
     *
     * @param create 不存在时是否创建
     * @return 当前 Session，不存在且不创建时返回 null
     */
    public static HttpSession getSession(boolean create) {
        HttpServletRequest request = getRequest();
        return request == null ? null : request.getSession(create);
    }

    /**
     * 获取当前 Session，不存在时抛出异常。
     *
     * @return 当前 Session
     */
    public static HttpSession requireSession() {
        HttpSession session = getSession(false);
        if (session == null) {
            throw new IllegalStateException("当前线程不存在 HttpSession");
        }
        return session;
    }

    /**
     * 获取 ServletContext。
     *
     * @return ServletContext，不存在时返回 null
     */
    public static ServletContext getServletContext() {
        HttpServletRequest request = getRequest();
        if (request != null) {
            return request.getServletContext();
        }
        ApplicationContext context = applicationContext;
        if (context instanceof WebApplicationContext webApplicationContext) {
            return webApplicationContext.getServletContext();
        }
        return null;
    }

    /**
     * 获取请求参数。
     *
     * @param name 参数名
     * @return 参数值，不存在时返回 null
     */
    public static String getParameter(String name) {
        requireText(name, "name 不能为空");
        HttpServletRequest request = getRequest();
        return request == null ? null : request.getParameter(name);
    }

    /**
     * 获取请求参数，不存在时返回默认值。
     *
     * @param name         参数名
     * @param defaultValue 默认值
     * @return 参数值
     */
    public static String getParameter(String name, String defaultValue) {
        String value = getParameter(name);
        return value == null ? defaultValue : value;
    }

    /**
     * 获取请求参数映射。
     *
     * @return 参数映射，不存在请求时返回空映射
     */
    public static Map<String, String[]> getParameterMap() {
        HttpServletRequest request = getRequest();
        if (request == null) {
            return Map.of();
        }
        Map<String, String[]> result = new LinkedHashMap<>();
        request.getParameterMap().forEach((key, value) -> result.put(key, value == null ? new String[0] : value.clone()));
        return Collections.unmodifiableMap(result);
    }

    /**
     * 获取请求参数的多个值。
     *
     * @param name 参数名
     * @return 参数值列表，不存在时返回空列表
     */
    public static List<String> getParameterValues(String name) {
        requireText(name, "name 不能为空");
        HttpServletRequest request = getRequest();
        if (request == null) {
            return List.of();
        }
        String[] values = request.getParameterValues(name);
        return values == null ? List.of() : List.of(values);
    }

    /**
     * 获取 Integer 类型请求参数。
     *
     * @param name 参数名
     * @return Integer 参数值，不存在时返回 null
     */
    public static Integer getParameterAsInt(String name) {
        String value = getParameter(name);
        if (value == null || value.isBlank()) {
            return null;
        }
        try {
            return Integer.valueOf(value.trim());
        } catch (NumberFormatException ex) {
            throw new IllegalArgumentException("请求参数不是有效 Integer：" + name, ex);
        }
    }

    /**
     * 获取 Long 类型请求参数。
     *
     * @param name 参数名
     * @return Long 参数值，不存在时返回 null
     */
    public static Long getParameterAsLong(String name) {
        String value = getParameter(name);
        if (value == null || value.isBlank()) {
            return null;
        }
        try {
            return Long.valueOf(value.trim());
        } catch (NumberFormatException ex) {
            throw new IllegalArgumentException("请求参数不是有效 Long：" + name, ex);
        }
    }

    /**
     * 获取 Boolean 类型请求参数。
     *
     * @param name 参数名
     * @return Boolean 参数值，不存在时返回 null
     */
    public static Boolean getParameterAsBoolean(String name) {
        String value = getParameter(name);
        return value == null || value.isBlank() ? null : parseBooleanValue(value, "请求参数不是有效 Boolean：" + name);
    }

    /**
     * 获取列表类型请求参数，支持重复参数和逗号分隔参数。
     *
     * @param name 参数名
     * @return 参数值列表
     */
    public static List<String> getParameterAsList(String name) {
        List<String> values = getParameterValues(name);
        if (values.isEmpty()) {
            return List.of();
        }
        List<String> result = new ArrayList<>();
        for (String value : values) {
            if (value == null || value.isBlank()) {
                continue;
            }
            Arrays.stream(value.split(","))
                    .map(String::trim)
                    .filter(item -> !item.isEmpty())
                    .forEach(result::add);
        }
        return List.copyOf(result);
    }

    /**
     * 获取请求头。
     *
     * @param name 请求头名称
     * @return 请求头值，不存在时返回 null
     */
    public static String getHeader(String name) {
        requireText(name, "name 不能为空");
        HttpServletRequest request = getRequest();
        return request == null ? null : request.getHeader(name);
    }

    /**
     * 获取请求头，不存在时返回默认值。
     *
     * @param name         请求头名称
     * @param defaultValue 默认值
     * @return 请求头值
     */
    public static String getHeader(String name, String defaultValue) {
        String value = getHeader(name);
        return value == null ? defaultValue : value;
    }

    /**
     * 获取同名请求头的全部值。
     *
     * @param name 请求头名称
     * @return 请求头值列表
     */
    public static List<String> getHeaders(String name) {
        requireText(name, "name 不能为空");
        HttpServletRequest request = getRequest();
        if (request == null) {
            return List.of();
        }
        return enumerationToList(request.getHeaders(name));
    }

    /**
     * 获取全部请求头名称。
     *
     * @return 请求头名称列表
     */
    public static List<String> getHeaderNames() {
        HttpServletRequest request = getRequest();
        if (request == null) {
            return List.of();
        }
        return enumerationToList(request.getHeaderNames());
    }

    /**
     * 获取 Authorization 请求头。
     *
     * @return Authorization 请求头，不存在时返回 null
     */
    public static String getAuthorization() {
        return getHeader("Authorization");
    }

    /**
     * 从 Authorization 请求头中获取 Bearer Token。
     *
     * @return Bearer Token，不存在或格式不正确时返回 null
     */
    public static String getBearerToken() {
        String authorization = getAuthorization();
        if (authorization == null) {
            return null;
        }
        String prefix = "Bearer ";
        return authorization.regionMatches(true, 0, prefix, 0, prefix.length())
                ? authorization.substring(prefix.length()).trim()
                : null;
    }

    /**
     * 获取请求 Content-Type。
     *
     * @return Content-Type，不存在时返回 null
     */
    public static String getContentType() {
        HttpServletRequest request = getRequest();
        return request == null ? null : request.getContentType();
    }

    /**
     * 获取 User-Agent 请求头。
     *
     * @return User-Agent，不存在时返回 null
     */
    public static String getUserAgent() {
        return getHeader("User-Agent");
    }

    /**
     * 获取 Referer 请求头。
     *
     * @return Referer，不存在时返回 null
     */
    public static String getReferer() {
        return getHeader("Referer");
    }

    /**
     * 获取 Origin 请求头。
     *
     * @return Origin，不存在时返回 null
     */
    public static String getOrigin() {
        return getHeader("Origin");
    }

    /**
     * 获取 Host 请求头。
     *
     * @return Host，不存在时返回 null
     */
    public static String getHost() {
        return getHeader("Host");
    }

    /**
     * 获取客户端真实 IP。
     *
     * @return 客户端 IP，不存在请求时返回 null
     */
    public static String getClientIp() {
        HttpServletRequest request = getRequest();
        if (request == null) {
            return null;
        }
        for (String header : CLIENT_IP_HEADERS) {
            String value = request.getHeader(header);
            String ip = firstValidIp(value);
            if (ip != null) {
                return normalizeIp(ip);
            }
        }
        return normalizeIp(request.getRemoteAddr());
    }

    /**
     * 获取客户端端口。
     *
     * @return 客户端端口，不存在请求时返回 null
     */
    public static Integer getClientPort() {
        HttpServletRequest request = getRequest();
        return request == null ? null : request.getRemotePort();
    }

    /**
     * 获取远程地址。
     *
     * @return 远程地址，不存在请求时返回 null
     */
    public static String getRemoteAddr() {
        HttpServletRequest request = getRequest();
        return request == null ? null : request.getRemoteAddr();
    }

    /**
     * 获取远程主机。
     *
     * @return 远程主机，不存在请求时返回 null
     */
    public static String getRemoteHost() {
        HttpServletRequest request = getRequest();
        return request == null ? null : request.getRemoteHost();
    }

    /**
     * 获取请求协议方案。
     *
     * @return scheme，不存在请求时返回 null
     */
    public static String getScheme() {
        HttpServletRequest request = getRequest();
        return request == null ? null : request.getScheme();
    }

    /**
     * 获取服务端名称。
     *
     * @return 服务端名称，不存在请求时返回 null
     */
    public static String getServerName() {
        HttpServletRequest request = getRequest();
        return request == null ? null : request.getServerName();
    }

    /**
     * 获取服务端端口。
     *
     * @return 服务端端口，不存在请求时返回 null
     */
    public static Integer getServerPort() {
        HttpServletRequest request = getRequest();
        if (request != null) {
            return request.getServerPort();
        }
        return getPropertyAsInt("server.port", null);
    }

    /**
     * 获取请求协议。
     *
     * @return 请求协议，不存在请求时返回 null
     */
    public static String getProtocol() {
        HttpServletRequest request = getRequest();
        return request == null ? null : request.getProtocol();
    }

    /**
     * 判断当前请求是否为 Ajax 请求。
     *
     * @return Ajax 请求时返回 true
     */
    public static boolean isAjaxRequest() {
        return "XMLHttpRequest".equalsIgnoreCase(getHeader("X-Requested-With"));
    }

    /**
     * 判断当前请求是否为 HTTPS 安全请求。
     *
     * @return 安全请求时返回 true
     */
    public static boolean isSecureRequest() {
        HttpServletRequest request = getRequest();
        return request != null && request.isSecure();
    }

    /**
     * 获取请求 URI。
     *
     * @return 请求 URI，不存在请求时返回 null
     */
    public static String getRequestUri() {
        HttpServletRequest request = getRequest();
        return request == null ? null : request.getRequestURI();
    }

    /**
     * 获取请求 URL。
     *
     * @return 请求 URL，不存在请求时返回 null
     */
    public static String getRequestUrl() {
        HttpServletRequest request = getRequest();
        return request == null ? null : request.getRequestURL().toString();
    }

    /**
     * 获取查询字符串。
     *
     * @return 查询字符串，不存在时返回 null
     */
    public static String getQueryString() {
        HttpServletRequest request = getRequest();
        return request == null ? null : request.getQueryString();
    }

    /**
     * 获取完整请求 URL。
     *
     * @return 完整请求 URL，不存在请求时返回 null
     */
    public static String getFullUrl() {
        String url = getRequestUrl();
        if (url == null) {
            return null;
        }
        String queryString = getQueryString();
        return queryString == null || queryString.isBlank() ? url : url + "?" + queryString;
    }

    /**
     * 获取上下文路径。
     *
     * @return 上下文路径，不存在请求时尝试读取 server.servlet.context-path
     */
    public static String getContextPath() {
        HttpServletRequest request = getRequest();
        if (request != null) {
            return request.getContextPath();
        }
        return getProperty("server.servlet.context-path", "");
    }

    /**
     * 获取 Servlet 路径。
     *
     * @return Servlet 路径，不存在请求时返回 null
     */
    public static String getServletPath() {
        HttpServletRequest request = getRequest();
        return request == null ? null : request.getServletPath();
    }

    /**
     * 获取路径信息。
     *
     * @return 路径信息，不存在时返回 null
     */
    public static String getPathInfo() {
        HttpServletRequest request = getRequest();
        return request == null ? null : request.getPathInfo();
    }

    /**
     * 获取请求方法。
     *
     * @return 请求方法，不存在请求时返回 null
     */
    public static String getMethod() {
        HttpServletRequest request = getRequest();
        return request == null ? null : request.getMethod();
    }

    /**
     * 获取请求方法。
     *
     * @return 请求方法，不存在请求时返回 null
     */
    public static String getRequestMethod() {
        return getMethod();
    }

    /**
     * 判断是否为 GET 请求。
     *
     * @return GET 请求时返回 true
     */
    public static boolean isGet() {
        return "GET".equalsIgnoreCase(getMethod());
    }

    /**
     * 判断是否为 POST 请求。
     *
     * @return POST 请求时返回 true
     */
    public static boolean isPost() {
        return "POST".equalsIgnoreCase(getMethod());
    }

    /**
     * 判断是否为 PUT 请求。
     *
     * @return PUT 请求时返回 true
     */
    public static boolean isPut() {
        return "PUT".equalsIgnoreCase(getMethod());
    }

    /**
     * 判断是否为 DELETE 请求。
     *
     * @return DELETE 请求时返回 true
     */
    public static boolean isDelete() {
        return "DELETE".equalsIgnoreCase(getMethod());
    }

    /**
     * 获取所有 Cookie。
     *
     * @return Cookie 数组，不存在时返回空数组
     */
    public static Cookie[] getCookies() {
        HttpServletRequest request = getRequest();
        Cookie[] cookies = request == null ? null : request.getCookies();
        return cookies == null ? new Cookie[0] : cookies.clone();
    }

    /**
     * 根据名称获取 Cookie。
     *
     * @param name Cookie 名称
     * @return Cookie，不存在时返回 null
     */
    public static Cookie getCookie(String name) {
        requireText(name, "name 不能为空");
        for (Cookie cookie : getCookies()) {
            if (name.equals(cookie.getName())) {
                return cookie;
            }
        }
        return null;
    }

    /**
     * 根据名称获取 Cookie 值。
     *
     * @param name Cookie 名称
     * @return Cookie 值，不存在时返回 null
     */
    public static String getCookieValue(String name) {
        Cookie cookie = getCookie(name);
        return cookie == null ? null : cookie.getValue();
    }

    /**
     * 添加 Cookie。
     *
     * @param name  Cookie 名称
     * @param value Cookie 值
     */
    public static void addCookie(String name, String value) {
        addCookie(name, value, -1);
    }

    /**
     * 添加 Cookie。
     *
     * @param name   Cookie 名称
     * @param value  Cookie 值
     * @param maxAge 存活秒数
     */
    public static void addCookie(String name, String value, int maxAge) {
        addCookie(name, value, "/", maxAge);
    }

    /**
     * 添加 Cookie。
     *
     * @param name   Cookie 名称
     * @param value  Cookie 值
     * @param path   Cookie 路径
     * @param maxAge 存活秒数
     */
    public static void addCookie(String name, String value, String path, int maxAge) {
        requireText(name, "name 不能为空");
        Cookie cookie = new Cookie(name, value == null ? "" : value);
        cookie.setPath(path == null || path.isBlank() ? "/" : path);
        cookie.setMaxAge(maxAge);
        requireResponse().addCookie(cookie);
    }

    /**
     * 添加带安全属性的 Cookie。
     *
     * @param name     Cookie 名称
     * @param value    Cookie 值
     * @param path     Cookie 路径
     * @param domain   Cookie 域名
     * @param maxAge   存活秒数
     * @param httpOnly 是否 HttpOnly
     * @param secure   是否 Secure
     * @param sameSite SameSite 策略
     */
    public static void addCookie(String name, String value, String path, String domain, int maxAge,
                                 boolean httpOnly, boolean secure, String sameSite) {
        requireText(name, "name 不能为空");
        Cookie cookie = new Cookie(name, value == null ? "" : value);
        cookie.setPath(path == null || path.isBlank() ? "/" : path);
        if (domain != null && !domain.isBlank()) {
            cookie.setDomain(domain);
        }
        cookie.setMaxAge(maxAge);
        cookie.setHttpOnly(httpOnly);
        cookie.setSecure(secure);
        requireResponse().addCookie(cookie);
        if (sameSite != null && !sameSite.isBlank()) {
            requireResponse().addHeader("Set-Cookie", buildSameSiteCookieHeader(cookie, sameSite));
        }
    }

    /**
     * 删除指定名称的 Cookie。
     *
     * @param name Cookie 名称
     */
    public static void removeCookie(String name) {
        removeCookie(name, "/");
    }

    /**
     * 删除指定名称和路径的 Cookie。
     *
     * @param name Cookie 名称
     * @param path Cookie 路径
     */
    public static void removeCookie(String name, String path) {
        addCookie(name, "", path, 0);
    }

    /**
     * 设置响应状态码。
     *
     * @param status HTTP 状态码
     */
    public static void setStatus(int status) {
        requireResponse().setStatus(status);
    }

    /**
     * 设置响应头。
     *
     * @param name  响应头名称
     * @param value 响应头值
     */
    public static void setHeader(String name, String value) {
        requireText(name, "name 不能为空");
        requireResponse().setHeader(name, value);
    }

    /**
     * 添加响应头。
     *
     * @param name  响应头名称
     * @param value 响应头值
     */
    public static void addHeader(String name, String value) {
        requireText(name, "name 不能为空");
        requireResponse().addHeader(name, value);
    }

    /**
     * 设置响应 Content-Type。
     *
     * @param contentType Content-Type
     */
    public static void setContentType(String contentType) {
        requireText(contentType, "contentType 不能为空");
        requireResponse().setContentType(contentType);
    }

    /**
     * 设置 JSON UTF-8 响应类型。
     */
    public static void setJsonContentType() {
        requireResponse().setContentType(APPLICATION_JSON_UTF8);
        setUtf8Encoding();
    }

    /**
     * 设置 UTF-8 响应编码。
     */
    public static void setUtf8Encoding() {
        requireResponse().setCharacterEncoding(StandardCharsets.UTF_8.name());
    }

    /**
     * 写出文本响应。
     *
     * @param text 文本内容
     */
    public static void writeText(String text) {
        HttpServletResponse response = requireResponse();
        if (response.getContentType() == null) {
            response.setContentType(TEXT_PLAIN_UTF8);
        }
        try {
            PrintWriter writer = response.getWriter();
            writer.write(text == null ? "" : text);
            writer.flush();
        } catch (IOException ex) {
            throw new UncheckedIOException("写出文本响应失败", ex);
        }
    }

    /**
     * 写出 JSON 响应。
     *
     * @param data 响应数据
     */
    public static void writeJson(Object data) {
        setJsonContentType();
        writeText(toJson(data));
    }

    /**
     * 写出字节响应。
     *
     * @param bytes 字节数组
     */
    public static void writeBytes(byte[] bytes) {
        HttpServletResponse response = requireResponse();
        try {
            OutputStream outputStream = response.getOutputStream();
            outputStream.write(bytes == null ? new byte[0] : bytes);
            outputStream.flush();
        } catch (IOException ex) {
            throw new UncheckedIOException("写出字节响应失败", ex);
        }
    }

    /**
     * 刷新响应缓冲区。
     */
    public static void flush() {
        try {
            requireResponse().flushBuffer();
        } catch (IOException ex) {
            throw new UncheckedIOException("刷新响应失败", ex);
        }
    }

    /**
     * 设置文件下载响应头。
     *
     * @param fileName 文件名
     */
    public static void setDownloadHeader(String fileName) {
        setDownloadHeader(fileName, "application/octet-stream");
    }

    /**
     * 设置文件下载响应头。
     *
     * @param fileName    文件名
     * @param contentType Content-Type
     */
    public static void setDownloadHeader(String fileName, String contentType) {
        requireText(fileName, "fileName 不能为空");
        HttpServletResponse response = requireResponse();
        response.setContentType(contentType == null || contentType.isBlank() ? "application/octet-stream" : contentType);
        String encoded = URLEncoder.encode(fileName, StandardCharsets.UTF_8).replace("+", "%20");
        response.setHeader("Content-Disposition", "attachment; filename*=UTF-8''" + encoded);
    }

    /**
     * 写出下载文件字节。
     *
     * @param fileName 文件名
     * @param bytes    文件字节数组
     */
    public static void writeDownload(String fileName, byte[] bytes) {
        setDownloadHeader(fileName);
        writeBytes(bytes);
    }

    /**
     * 写出下载文件输入流。
     *
     * @param fileName    文件名
     * @param inputStream 文件输入流
     */
    public static void writeDownload(String fileName, InputStream inputStream) {
        requireNonNull(inputStream, "inputStream 不能为空");
        setDownloadHeader(fileName);
        try (inputStream) {
            inputStream.transferTo(requireResponse().getOutputStream());
            flush();
        } catch (IOException ex) {
            throw new UncheckedIOException("写出下载文件失败：" + fileName, ex);
        }
    }

    /**
     * 写出下载文件。
     *
     * @param fileName 文件名
     * @param file     文件对象
     */
    public static void writeDownload(String fileName, File file) {
        requireNonNull(file, "file 不能为空");
        if (!file.isFile()) {
            throw new IllegalArgumentException("下载文件不存在或不是普通文件：" + file);
        }
        try {
            writeDownload(fileName, java.nio.file.Files.newInputStream(file.toPath()));
        } catch (IOException ex) {
            throw new UncheckedIOException("打开下载文件失败：" + file, ex);
        }
    }

    /**
     * 读取请求体字符串。
     *
     * @return 请求体字符串，不存在请求时返回 null
     */
    public static String getRequestBody() {
        return getRequestBodyAsString();
    }

    /**
     * 读取请求体字符串。
     *
     * @return 请求体字符串，不存在请求时返回 null
     */
    public static String getRequestBodyAsString() {
        byte[] bytes = getRequestBodyAsBytes();
        return bytes == null ? null : new String(bytes, StandardCharsets.UTF_8);
    }

    /**
     * 读取请求体字节数组。
     *
     * @return 请求体字节数组，不存在请求时返回 null
     */
    public static byte[] getRequestBodyAsBytes() {
        HttpServletRequest request = getRequest();
        if (request == null) {
            return null;
        }
        try {
            return request.getInputStream().readAllBytes();
        } catch (IOException ex) {
            throw new UncheckedIOException("读取请求体失败", ex);
        }
    }

    /**
     * 获取请求输入流。
     *
     * @return 请求输入流，不存在请求时返回 null
     */
    public static ServletInputStream getInputStream() {
        HttpServletRequest request = getRequest();
        if (request == null) {
            return null;
        }
        try {
            return request.getInputStream();
        } catch (IOException ex) {
            throw new UncheckedIOException("获取请求输入流失败", ex);
        }
    }

    /**
     * 获取请求字符读取器。
     *
     * @return 请求字符读取器，不存在请求时返回 null
     */
    public static java.io.BufferedReader getReader() {
        HttpServletRequest request = getRequest();
        if (request == null) {
            return null;
        }
        try {
            return request.getReader();
        } catch (IOException ex) {
            throw new UncheckedIOException("获取请求 Reader 失败", ex);
        }
    }

    /**
     * 获取请求属性。
     *
     * @param name 属性名
     * @return 属性值，不存在时返回 null
     */
    public static Object getAttribute(String name) {
        requireText(name, "name 不能为空");
        HttpServletRequest request = getRequest();
        return request == null ? null : request.getAttribute(name);
    }

    /**
     * 设置请求属性。
     *
     * @param name  属性名
     * @param value 属性值
     */
    public static void setAttribute(String name, Object value) {
        requireText(name, "name 不能为空");
        requireRequest().setAttribute(name, value);
    }

    /**
     * 移除请求属性。
     *
     * @param name 属性名
     */
    public static void removeAttribute(String name) {
        requireText(name, "name 不能为空");
        requireRequest().removeAttribute(name);
    }

    /**
     * 获取请求属性名称列表。
     *
     * @return 属性名称列表
     */
    public static List<String> getAttributeNames() {
        HttpServletRequest request = getRequest();
        return request == null ? List.of() : enumerationToList(request.getAttributeNames());
    }

    /**
     * 获取字符串请求属性。
     *
     * @param name 属性名
     * @return 字符串属性值，不存在时返回 null
     */
    public static String getAttributeAsString(String name) {
        Object value = getAttribute(name);
        return value == null ? null : String.valueOf(value);
    }

    /**
     * 获取 Long 类型请求属性。
     *
     * @param name 属性名
     * @return Long 属性值，不存在时返回 null
     */
    public static Long getAttributeAsLong(String name) {
        Object value = getAttribute(name);
        if (value == null) {
            return null;
        }
        if (value instanceof Number number) {
            return number.longValue();
        }
        try {
            return Long.valueOf(String.valueOf(value));
        } catch (NumberFormatException ex) {
            throw new IllegalArgumentException("请求属性不是有效 Long：" + name, ex);
        }
    }

    /**
     * 获取 Session 属性。
     *
     * @param name 属性名
     * @return 属性值，不存在时返回 null
     */
    public static Object getSessionAttribute(String name) {
        requireText(name, "name 不能为空");
        HttpSession session = getSession(false);
        return session == null ? null : session.getAttribute(name);
    }

    /**
     * 设置 Session 属性。
     *
     * @param name  属性名
     * @param value 属性值
     */
    public static void setSessionAttribute(String name, Object value) {
        requireText(name, "name 不能为空");
        HttpSession session = getSession(true);
        if (session == null) {
            throw new IllegalStateException("当前线程不存在 HttpSession");
        }
        session.setAttribute(name, value);
    }

    /**
     * 移除 Session 属性。
     *
     * @param name 属性名
     */
    public static void removeSessionAttribute(String name) {
        requireText(name, "name 不能为空");
        HttpSession session = getSession(false);
        if (session != null) {
            session.removeAttribute(name);
        }
    }

    /**
     * 使当前 Session 失效。
     */
    public static void invalidateSession() {
        HttpSession session = getSession(false);
        if (session != null) {
            session.invalidate();
        }
    }

    /**
     * 获取当前 Session ID。
     *
     * @return Session ID，不存在时返回 null
     */
    public static String getSessionId() {
        HttpSession session = getSession(false);
        return session == null ? null : session.getId();
    }

    /**
     * 判断当前 Session 是否存在。
     *
     * @return 存在时返回 true
     */
    public static boolean isSessionExists() {
        return getSession(false) != null;
    }

    /**
     * 转发当前请求。
     *
     * @param path 转发路径
     */
    public static void forward(String path) {
        requireText(path, "path 不能为空");
        try {
            RequestDispatcher dispatcher = requireRequest().getRequestDispatcher(path);
            if (dispatcher == null) {
                throw new IllegalArgumentException("无法获取 RequestDispatcher：" + path);
            }
            dispatcher.forward(requireRequest(), requireResponse());
        } catch (IOException ex) {
            throw new UncheckedIOException("请求转发失败：" + path, ex);
        } catch (ServletException ex) {
            throw new IllegalStateException("请求转发失败：" + path, ex);
        }
    }

    /**
     * 重定向当前响应。
     *
     * @param url 重定向 URL
     */
    public static void redirect(String url) {
        requireText(url, "url 不能为空");
        try {
            requireResponse().sendRedirect(url);
        } catch (IOException ex) {
            throw new UncheckedIOException("响应重定向失败：" + url, ex);
        }
    }

    /**
     * 发送错误状态码。
     *
     * @param status HTTP 状态码
     */
    public static void sendError(int status) {
        try {
            requireResponse().sendError(status);
        } catch (IOException ex) {
            throw new UncheckedIOException("发送错误状态失败：" + status, ex);
        }
    }

    /**
     * 发送错误状态码和错误消息。
     *
     * @param status  HTTP 状态码
     * @param message 错误消息
     */
    public static void sendError(int status, String message) {
        try {
            requireResponse().sendError(status, message);
        } catch (IOException ex) {
            throw new UncheckedIOException("发送错误状态失败：" + status, ex);
        }
    }

    /**
     * 判断当前线程是否存在活动事务。
     *
     * @return 存在活动事务时返回 true
     */
    public static boolean isTransactionActive() {
        return TransactionSynchronizationManager.isActualTransactionActive();
    }

    /**
     * 判断当前事务是否只读。
     *
     * @return 只读事务时返回 true
     */
    public static boolean isCurrentTransactionReadOnly() {
        return TransactionSynchronizationManager.isCurrentTransactionReadOnly();
    }

    /**
     * 获取当前事务名称。
     *
     * @return 事务名称，不存在时返回 null
     */
    public static String getCurrentTransactionName() {
        return TransactionSynchronizationManager.getCurrentTransactionName();
    }

    /**
     * 注册事务提交后执行的任务，无事务时立即执行。
     *
     * @param task 回调任务
     */
    public static void registerAfterCommit(Runnable task) {
        requireNonNull(task, "task 不能为空");
        if (TransactionSynchronizationManager.isSynchronizationActive()) {
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                @Override
                public void afterCommit() {
                    task.run();
                }
            });
        } else {
            task.run();
        }
    }

    /**
     * 注册事务回滚后执行的任务，无事务时不执行。
     *
     * @param task 回调任务
     */
    public static void registerAfterRollback(Runnable task) {
        requireNonNull(task, "task 不能为空");
        if (TransactionSynchronizationManager.isSynchronizationActive()) {
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                @Override
                public void afterCompletion(int status) {
                    if (status == STATUS_ROLLED_BACK) {
                        task.run();
                    }
                }
            });
        }
    }

    /**
     * 注册事务完成后执行的任务，无事务时立即执行。
     *
     * @param task 回调任务
     */
    public static void registerAfterCompletion(Runnable task) {
        requireNonNull(task, "task 不能为空");
        if (TransactionSynchronizationManager.isSynchronizationActive()) {
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                @Override
                public void afterCompletion(int status) {
                    task.run();
                }
            });
        } else {
            task.run();
        }
    }

    /**
     * 在事务中执行任务。
     *
     * @param task 任务
     */
    public static void executeInTransaction(Runnable task) {
        requireNonNull(task, "task 不能为空");
        executeInTransaction(() -> {
            task.run();
            return null;
        });
    }

    /**
     * 在事务中执行任务并返回结果。
     *
     * @param supplier 任务供应器
     * @param <T>      返回值泛型
     * @return 任务结果
     */
    public static <T> T executeInTransaction(Supplier<T> supplier) {
        requireNonNull(supplier, "supplier 不能为空");
        TransactionTemplate transactionTemplate = getBean(TransactionTemplate.class);
        return transactionTemplate.execute(status -> supplier.get());
    }

    /**
     * 在事务提交后执行任务，无事务时立即执行。
     *
     * @param task 回调任务
     */
    public static void executeAfterCommit(Runnable task) {
        registerAfterCommit(task);
    }

    /**
     * 获取缓存管理器。
     *
     * @return CacheManager，不存在时返回 null
     */
    public static CacheManager getCacheManager() {
        return getBeanOrNull(CacheManager.class);
    }

    /**
     * 根据名称获取缓存。
     *
     * @param cacheName 缓存名称
     * @return Cache，不存在时返回 null
     */
    public static Cache getCache(String cacheName) {
        requireText(cacheName, "cacheName 不能为空");
        CacheManager cacheManager = getCacheManager();
        return cacheManager == null ? null : cacheManager.getCache(cacheName);
    }

    /**
     * 获取缓存值。
     *
     * @param cacheName 缓存名称
     * @param key       缓存键
     * @return 缓存值，不存在时返回 null
     */
    public static Object getCacheValue(String cacheName, Object key) {
        requireNonNull(key, "key 不能为空");
        Cache cache = getCache(cacheName);
        Cache.ValueWrapper wrapper = cache == null ? null : cache.get(key);
        return wrapper == null ? null : wrapper.get();
    }

    /**
     * 写入缓存值。
     *
     * @param cacheName 缓存名称
     * @param key       缓存键
     * @param value     缓存值
     */
    public static void putCacheValue(String cacheName, Object key, Object value) {
        requireNonNull(key, "key 不能为空");
        Cache cache = requireCache(cacheName);
        cache.put(key, value);
    }

    /**
     * 清除指定缓存键。
     *
     * @param cacheName 缓存名称
     * @param key       缓存键
     */
    public static void evictCacheValue(String cacheName, Object key) {
        requireNonNull(key, "key 不能为空");
        Cache cache = requireCache(cacheName);
        cache.evict(key);
    }

    /**
     * 清空指定缓存。
     *
     * @param cacheName 缓存名称
     */
    public static void clearCache(String cacheName) {
        Cache cache = requireCache(cacheName);
        cache.clear();
    }

    /**
     * 获取任务执行器。
     *
     * @return TaskExecutor，不存在时返回 null
     */
    public static TaskExecutor getTaskExecutor() {
        TaskExecutor executor = getBeanOrNull(TaskExecutor.class);
        if (executor != null) {
            return executor;
        }
        Object bean = getBeanOrNull("taskExecutor");
        return bean instanceof TaskExecutor taskExecutor ? taskExecutor : null;
    }

    /**
     * 获取异步任务执行器。
     *
     * @return AsyncTaskExecutor，不存在时返回 null
     */
    public static AsyncTaskExecutor getAsyncTaskExecutor() {
        return getBeanOrNull(AsyncTaskExecutor.class);
    }

    /**
     * 获取任务调度器。
     *
     * @return TaskScheduler，不存在时返回 null
     */
    public static TaskScheduler getTaskScheduler() {
        return getBeanOrNull(TaskScheduler.class);
    }

    /**
     * 异步执行任务。
     *
     * @param task 任务
     */
    public static void executeAsync(Runnable task) {
        requireNonNull(task, "task 不能为空");
        TaskExecutor executor = getTaskExecutor();
        if (executor == null) {
            java.util.concurrent.CompletableFuture.runAsync(task);
            return;
        }
        executor.execute(task);
    }

    /**
     * 在指定时间调度执行任务。
     *
     * @param task      任务
     * @param startTime 开始时间
     */
    public static void schedule(Runnable task, Instant startTime) {
        requireNonNull(task, "task 不能为空");
        requireNonNull(startTime, "startTime 不能为空");
        TaskScheduler scheduler = requireTaskScheduler();
        scheduler.schedule(task, startTime);
    }

    /**
     * 按固定频率调度执行任务。
     *
     * @param task   任务
     * @param period 周期
     */
    public static void scheduleAtFixedRate(Runnable task, Duration period) {
        requireNonNull(task, "task 不能为空");
        requireNonNull(period, "period 不能为空");
        if (period.isZero() || period.isNegative()) {
            throw new IllegalArgumentException("period 必须大于 0");
        }
        requireTaskScheduler().scheduleAtFixedRate(task, period);
    }

    /**
     * 获取应用名称。
     *
     * @return 应用名称，不存在时返回 null
     */
    public static String getApplicationName() {
        return getProperty("spring.application.name");
    }

    /**
     * 获取应用主类名称。
     *
     * @return 主类名称，无法判断时返回 null
     */
    public static String getMainClass() {
        String command = System.getProperty("sun.java.command");
        if (command == null || command.isBlank()) {
            return null;
        }
        return command.split("\\s+")[0];
    }

    /**
     * 获取应用启动时间。
     *
     * @return 应用启动时间
     */
    public static Date getStartupDate() {
        return new Date(requireApplicationContext().getStartupDate());
    }

    /**
     * 获取应用 ID。
     *
     * @return 应用 ID，不存在时返回应用名称
     */
    public static String getApplicationId() {
        return getProperty("spring.application.id", getApplicationName());
    }

    /**
     * 获取应用版本。
     *
     * @return 应用版本，不存在时返回 null
     */
    public static String getApplicationVersion() {
        BuildProperties buildProperties = getBeanOrNull(BuildProperties.class);
        if (buildProperties != null && buildProperties.getVersion() != null) {
            return buildProperties.getVersion();
        }
        String version = getProperty("application.version");
        if (version != null) {
            return version;
        }
        version = getProperty("app.version");
        if (version != null) {
            return version;
        }
        Package pkg = SpringUtil.class.getPackage();
        return pkg == null ? null : pkg.getImplementationVersion();
    }

    /**
     * 将配置前缀绑定为目标类型。
     *
     * @param prefix     配置前缀
     * @param targetType 目标类型
     * @param <T>        目标泛型
     * @return 绑定对象，不存在时返回 null
     */
    public static <T> T bind(String prefix, Class<T> targetType) {
        requireText(prefix, "prefix 不能为空");
        requireNonNull(targetType, "targetType 不能为空");
        return Binder.get(getEnvironment()).bind(prefix, targetType).orElse(null);
    }

    /**
     * 将配置前缀绑定为目标类型，不存在时返回默认值。
     *
     * @param prefix       配置前缀
     * @param targetType   目标类型
     * @param defaultValue 默认值
     * @param <T>          目标泛型
     * @return 绑定对象
     */
    public static <T> T bindOrDefault(String prefix, Class<T> targetType, T defaultValue) {
        T value = bind(prefix, targetType);
        return value == null ? defaultValue : value;
    }

    /**
     * 将配置前缀绑定为列表。
     *
     * @param prefix      配置前缀
     * @param elementType 元素类型
     * @param <T>         元素泛型
     * @return 列表，不存在时返回空列表
     */
    public static <T> List<T> bindList(String prefix, Class<T> elementType) {
        requireText(prefix, "prefix 不能为空");
        requireNonNull(elementType, "elementType 不能为空");
        List<T> result = Binder.get(getEnvironment()).bind(prefix, Bindable.listOf(elementType)).orElse(List.of());
        return List.copyOf(result);
    }

    /**
     * 将配置前缀绑定为 Map。
     *
     * @param prefix    配置前缀
     * @param keyType   键类型
     * @param valueType 值类型
     * @param <K>       键泛型
     * @param <V>       值泛型
     * @return Map，不存在时返回空 Map
     */
    public static <K, V> Map<K, V> bindMap(String prefix, Class<K> keyType, Class<V> valueType) {
        requireText(prefix, "prefix 不能为空");
        requireNonNull(keyType, "keyType 不能为空");
        requireNonNull(valueType, "valueType 不能为空");
        Map<K, V> result = Binder.get(getEnvironment()).bind(prefix, Bindable.mapOf(keyType, valueType)).orElse(Map.of());
        return Map.copyOf(result);
    }

    /**
     * 判断应用上下文是否正在运行。
     *
     * @return 正在运行时返回 true
     */
    public static boolean isRunning() {
        ApplicationContext context = applicationContext;
        return context instanceof Lifecycle lifecycle && lifecycle.isRunning();
    }

    /**
     * 判断应用上下文是否处于活动状态。
     *
     * @return 活动状态时返回 true
     */
    public static boolean isActive() {
        ApplicationContext context = applicationContext;
        return context instanceof ConfigurableApplicationContext configurableApplicationContext && configurableApplicationContext.isActive();
    }

    /**
     * 判断应用上下文是否已关闭。
     *
     * @return 已关闭或未设置上下文时返回 true
     */
    public static boolean isClosed() {
        return !isActive();
    }

    /**
     * 关闭当前应用上下文。
     */
    public static void closeContext() {
        ApplicationContext context = applicationContext;
        if (context instanceof ConfigurableApplicationContext configurableApplicationContext) {
            configurableApplicationContext.close();
        }
    }

    /**
     * 获取应用启动关闭监视器，无法反射获取时返回 ApplicationContext 本身。
     *
     * @return 启动关闭监视器对象
     */
    public static Object getStartupShutdownMonitor() {
        ApplicationContext context = requireApplicationContext();
        try {
            Method method = context.getClass().getMethod("getStartupShutdownMonitor");
            return method.invoke(context);
        } catch (NoSuchMethodException ex) {
            return context;
        } catch (IllegalAccessException | InvocationTargetException ex) {
            throw new IllegalStateException("获取启动关闭监视器失败", ex);
        }
    }

    /**
     * 根据类型获取排序后的 Bean 列表。
     *
     * @param beanType Bean 类型
     * @param <T>      Bean 泛型
     * @return 排序后的 Bean 列表
     */
    public static <T> List<T> getOrderedBeans(Class<T> beanType) {
        return getOrderedBeanList(beanType);
    }

    /**
     * 根据类型获取排序后的 Bean 列表。
     *
     * @param beanType Bean 类型
     * @param <T>      Bean 泛型
     * @return 排序后的 Bean 列表
     */
    public static <T> List<T> getOrderedBeanList(Class<T> beanType) {
        return getBeanList(beanType);
    }

    /**
     * 根据类型获取主 Bean。
     *
     * @param beanType Bean 类型
     * @param <T>      Bean 泛型
     * @return 主 Bean 实例，不存在时返回 null
     */
    public static <T> T getPrimaryBean(Class<T> beanType) {
        requireNonNull(beanType, "beanType 不能为空");
        try {
            return requireApplicationContext().getBean(beanType);
        } catch (BeansException ex) {
            return null;
        }
    }

    /**
     * 获取带有指定注解的 Bean 映射。
     *
     * @param annotationType 注解类型
     * @return Bean 名称与实例映射
     */
    public static Map<String, Object> getBeanByAnnotation(Class<? extends Annotation> annotationType) {
        return getBeansWithAnnotation(annotationType);
    }

    /**
     * 获取带有指定注解的 Bean 映射。
     *
     * @param annotationType 注解类型
     * @return Bean 名称与实例映射
     */
    public static Map<String, Object> getBeansWithAnnotation(Class<? extends Annotation> annotationType) {
        requireNonNull(annotationType, "annotationType 不能为空");
        return new LinkedHashMap<>(requireApplicationContext().getBeansWithAnnotation(annotationType));
    }

    /**
     * 尝试根据类型获取 Bean。
     *
     * @param beanType Bean 类型
     * @param <T>      Bean 泛型
     * @return Bean Optional
     */
    public static <T> Optional<T> tryGetBean(Class<T> beanType) {
        return Optional.ofNullable(getBeanOrNull(beanType));
    }

    /**
     * 尝试根据名称和类型获取 Bean。
     *
     * @param beanName Bean 名称
     * @param beanType Bean 类型
     * @param <T>      Bean 泛型
     * @return Bean Optional
     */
    public static <T> Optional<T> tryGetBean(String beanName, Class<T> beanType) {
        requireText(beanName, "beanName 不能为空");
        requireNonNull(beanType, "beanType 不能为空");
        try {
            return Optional.of(requireApplicationContext().getBean(beanName, beanType));
        } catch (BeansException | IllegalStateException ex) {
            return Optional.empty();
        }
    }

    /**
     * 尝试获取配置属性。
     *
     * @param key 属性键
     * @return 属性 Optional
     */
    public static Optional<String> tryGetProperty(String key) {
        try {
            return Optional.ofNullable(getProperty(key));
        } catch (RuntimeException ex) {
            return Optional.empty();
        }
    }

    /**
     * 尝试发布事件。
     *
     * @param event 事件对象
     * @return 发布成功时返回 true
     */
    public static boolean tryPublishEvent(Object event) {
        return publishEventSafely(event);
    }

    private static String getMessage(String code, Object[] args, String defaultMessage, Locale locale) {
        requireText(code, "code 不能为空");
        Locale targetLocale = locale == null ? LocaleContextHolder.getLocale() : locale;
        MessageSource messageSource = requireApplicationContext();
        return messageSource.getMessage(code, args, defaultMessage, targetLocale);
    }

    private static Cache requireCache(String cacheName) {
        Cache cache = getCache(cacheName);
        if (cache == null) {
            throw new NoSuchBeanDefinitionException(Cache.class, "缓存不存在：" + cacheName);
        }
        return cache;
    }

    private static TaskScheduler requireTaskScheduler() {
        TaskScheduler scheduler = getTaskScheduler();
        if (scheduler == null) {
            throw new NoSuchBeanDefinitionException(TaskScheduler.class, "未找到 TaskScheduler Bean");
        }
        return scheduler;
    }

    private static boolean isProfileActiveSafely(String profile) {
        try {
            return isProfileActive(profile);
        } catch (RuntimeException ex) {
            return false;
        }
    }

    private static <T> List<T> enumerationToList(Enumeration<T> enumeration) {
        if (enumeration == null) {
            return List.of();
        }
        List<T> result = new ArrayList<>();
        while (enumeration.hasMoreElements()) {
            result.add(enumeration.nextElement());
        }
        return List.copyOf(result);
    }

    private static String firstValidIp(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        for (String part : value.split(",")) {
            String ip = part.trim();
            if (!ip.isEmpty() && !UNKNOWN.equalsIgnoreCase(ip)) {
                return ip;
            }
        }
        return null;
    }

    private static String normalizeIp(String ip) {
        if (ip == null || ip.isBlank()) {
            return null;
        }
        String trimmed = ip.trim();
        if ("0:0:0:0:0:0:0:1".equals(trimmed)) {
            return "127.0.0.1";
        }
        int colonIndex = trimmed.indexOf(':');
        if (colonIndex > 0 && trimmed.indexOf(':', colonIndex + 1) < 0 && trimmed.indexOf('.') > 0) {
            return trimmed.substring(0, colonIndex);
        }
        return trimmed;
    }

    private static Boolean parseBooleanValue(String value, String errorMessage) {
        String text = value.trim().toLowerCase(Locale.ROOT);
        return switch (text) {
            case "true", "1", "yes", "y", "on" -> Boolean.TRUE;
            case "false", "0", "no", "n", "off" -> Boolean.FALSE;
            default -> throw new IllegalArgumentException(errorMessage);
        };
    }

    private static String buildSameSiteCookieHeader(Cookie cookie, String sameSite) {
        StringBuilder builder = new StringBuilder();
        builder.append(cookie.getName()).append('=').append(cookie.getValue() == null ? "" : cookie.getValue());
        if (cookie.getPath() != null) {
            builder.append("; Path=").append(cookie.getPath());
        }
        if (cookie.getDomain() != null) {
            builder.append("; Domain=").append(cookie.getDomain());
        }
        if (cookie.getMaxAge() >= 0) {
            builder.append("; Max-Age=").append(cookie.getMaxAge());
        }
        if (cookie.isHttpOnly()) {
            builder.append("; HttpOnly");
        }
        if (cookie.getSecure()) {
            builder.append("; Secure");
        }
        builder.append("; SameSite=").append(sameSite);
        return builder.toString();
    }

    private static void requireNonNull(Object value, String message) {
        Objects.requireNonNull(value, message);
    }

    private static void requireText(String value, String message) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(message);
        }
    }

    private static void requireNonEmptyArray(String[] values, String message) {
        if (values == null || values.length == 0) {
            throw new IllegalArgumentException(message);
        }
        for (String value : values) {
            requireText(value, message);
        }
    }

    private static String toJson(Object value) {
        StringBuilder builder = new StringBuilder();
        writeJsonValue(builder, value, Collections.newSetFromMap(new IdentityHashMap<>()));
        return builder.toString();
    }

    private static void writeJsonValue(StringBuilder builder, Object value, Set<Object> visiting) {
        if (value == null) {
            builder.append("null");
            return;
        }
        if (value instanceof String text) {
            writeJsonString(builder, text);
            return;
        }
        if (value instanceof Character character) {
            writeJsonString(builder, String.valueOf(character));
            return;
        }
        if (value instanceof Number || value instanceof Boolean) {
            builder.append(value);
            return;
        }
        if (value instanceof Enum<?> enumValue) {
            writeJsonString(builder, enumValue.name());
            return;
        }
        if (value instanceof Date date) {
            writeJsonString(builder, date.toInstant().toString());
            return;
        }
        if (value instanceof Instant instant) {
            writeJsonString(builder, instant.toString());
            return;
        }
        if (value instanceof Map<?, ?> map) {
            if (!visiting.add(value)) {
                throw new IllegalArgumentException("JSON 序列化检测到循环引用");
            }
            builder.append('{');
            boolean first = true;
            for (Map.Entry<?, ?> entry : map.entrySet()) {
                if (!first) {
                    builder.append(',');
                }
                writeJsonString(builder, String.valueOf(entry.getKey()));
                builder.append(':');
                writeJsonValue(builder, entry.getValue(), visiting);
                first = false;
            }
            builder.append('}');
            visiting.remove(value);
            return;
        }
        if (value instanceof Iterable<?> iterable) {
            if (!visiting.add(value)) {
                throw new IllegalArgumentException("JSON 序列化检测到循环引用");
            }
            builder.append('[');
            boolean first = true;
            for (Object item : iterable) {
                if (!first) {
                    builder.append(',');
                }
                writeJsonValue(builder, item, visiting);
                first = false;
            }
            builder.append(']');
            visiting.remove(value);
            return;
        }
        Class<?> valueClass = value.getClass();
        if (valueClass.isArray()) {
            if (!visiting.add(value)) {
                throw new IllegalArgumentException("JSON 序列化检测到循环引用");
            }
            builder.append('[');
            int length = Array.getLength(value);
            for (int i = 0; i < length; i++) {
                if (i > 0) {
                    builder.append(',');
                }
                writeJsonValue(builder, Array.get(value, i), visiting);
            }
            builder.append(']');
            visiting.remove(value);
            return;
        }
        writeBeanAsJson(builder, value, visiting);
    }

    private static void writeBeanAsJson(StringBuilder builder, Object value, Set<Object> visiting) {
        if (!visiting.add(value)) {
            throw new IllegalArgumentException("JSON 序列化检测到循环引用");
        }
        Map<String, Object> properties = new LinkedHashMap<>();
        for (Method method : value.getClass().getMethods()) {
            if (!Modifier.isPublic(method.getModifiers()) || method.getParameterCount() != 0 || method.getReturnType() == Void.TYPE) {
                continue;
            }
            String name = method.getName();
            if ("getClass".equals(name)) {
                continue;
            }
            String propertyName = null;
            if (name.startsWith("get") && name.length() > 3) {
                propertyName = Character.toLowerCase(name.charAt(3)) + name.substring(4);
            } else if (name.startsWith("is") && name.length() > 2 && method.getReturnType() == Boolean.TYPE) {
                propertyName = Character.toLowerCase(name.charAt(2)) + name.substring(3);
            }
            if (propertyName == null) {
                continue;
            }
            try {
                properties.put(propertyName, method.invoke(value));
            } catch (IllegalAccessException | InvocationTargetException ex) {
                throw new IllegalArgumentException("JSON 序列化读取属性失败：" + propertyName, ex);
            }
        }
        builder.append('{');
        boolean first = true;
        for (Map.Entry<String, Object> entry : properties.entrySet()) {
            if (!first) {
                builder.append(',');
            }
            writeJsonString(builder, entry.getKey());
            builder.append(':');
            writeJsonValue(builder, entry.getValue(), visiting);
            first = false;
        }
        builder.append('}');
        visiting.remove(value);
    }

    private static void writeJsonString(StringBuilder builder, String text) {
        builder.append('"');
        for (int i = 0; i < text.length(); i++) {
            char ch = text.charAt(i);
            switch (ch) {
                case '"' -> builder.append("\\\"");
                case '\\' -> builder.append("\\\\");
                case '\b' -> builder.append("\\b");
                case '\f' -> builder.append("\\f");
                case '\n' -> builder.append("\\n");
                case '\r' -> builder.append("\\r");
                case '\t' -> builder.append("\\t");
                default -> {
                    if (ch < 0x20) {
                        builder.append(String.format("\\u%04x", (int) ch));
                    } else {
                        builder.append(ch);
                    }
                }
            }
        }
        builder.append('"');
    }
}
