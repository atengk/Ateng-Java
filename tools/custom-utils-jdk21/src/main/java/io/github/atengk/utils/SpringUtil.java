package io.github.atengk.utils;

import jakarta.servlet.ServletContext;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.beans.factory.NoSuchBeanDefinitionException;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.beans.factory.config.BeanFactoryPostProcessor;
import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;
import org.springframework.beans.factory.support.BeanDefinitionBuilder;
import org.springframework.beans.factory.support.BeanDefinitionRegistry;
import org.springframework.beans.factory.support.DefaultSingletonBeanRegistry;
import org.springframework.beans.factory.support.GenericBeanDefinition;
import org.springframework.boot.web.context.WebServerApplicationContext;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.ApplicationEventPublisherAware;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.MessageSourceResolvable;
import org.springframework.context.annotation.Lazy;
import org.springframework.context.i18n.LocaleContextHolder;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.core.convert.ConversionService;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.core.env.Environment;
import org.springframework.core.io.Resource;
import org.springframework.lang.NonNull;
import org.springframework.lang.Nullable;
import org.springframework.stereotype.Component;
import org.springframework.web.context.WebApplicationContext;
import org.springframework.web.context.request.RequestAttributes;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.lang.annotation.Annotation;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Arrays;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Supplier;

/**
 * Spring 上下文工具类。
 *
 * @author Ateng
 * @since 2026-04-27
 */
@Lazy(false)
@Component
@Order(Ordered.HIGHEST_PRECEDENCE)
public final class SpringUtil implements ApplicationContextAware, ApplicationEventPublisherAware, BeanFactoryPostProcessor, DisposableBean {

    private static final Logger log = LoggerFactory.getLogger(SpringUtil.class);

    private static volatile ApplicationContext applicationContext;

    private static volatile ApplicationEventPublisher applicationEventPublisher;

    private static volatile ConfigurableListableBeanFactory beanFactory;

    /**
     * 设置 Spring 应用上下文。
     *
     * @param context Spring 应用上下文
     * @throws BeansException Spring Bean 异常
     */
    @Override
    public void setApplicationContext(@NonNull ApplicationContext context) throws BeansException {
        ApplicationContext oldContext = SpringUtil.applicationContext;
        if (oldContext != null && oldContext != context) {
            log.warn("检测到 Spring 应用上下文被重新设置，将使用新的上下文引用");
        }
        SpringUtil.applicationContext = context;
    }

    /**
     * 设置 Spring 事件发布器。
     *
     * @param publisher Spring 事件发布器
     */
    @Override
    public void setApplicationEventPublisher(@NonNull ApplicationEventPublisher publisher) {
        SpringUtil.applicationEventPublisher = publisher;
    }

    /**
     * 设置 Spring BeanFactory。
     *
     * @param factory Spring BeanFactory
     * @throws BeansException Spring Bean 异常
     */
    @Override
    public void postProcessBeanFactory(@NonNull ConfigurableListableBeanFactory factory) throws BeansException {
        ConfigurableListableBeanFactory oldFactory = SpringUtil.beanFactory;
        if (oldFactory != null && oldFactory != factory) {
            log.warn("检测到 Spring BeanFactory 被重新设置，将使用新的 BeanFactory 引用");
        }
        SpringUtil.beanFactory = factory;
    }

    /**
     * Spring 容器销毁时清理静态引用。
     */
    @Override
    public void destroy() {
        log.info("清理 SpringUtil 静态上下文引用");
        applicationContext = null;
        applicationEventPublisher = null;
        beanFactory = null;
    }

    /**
     * 获取 Spring 应用上下文。
     *
     * @return Spring 应用上下文
     */
    public static ApplicationContext getApplicationContext() {
        ApplicationContext context = applicationContext;
        if (context == null) {
            throw new IllegalStateException("Spring 应用上下文尚未初始化");
        }
        return context;
    }

    /**
     * 获取 Spring BeanFactory。
     *
     * @return Spring BeanFactory
     */
    public static ConfigurableListableBeanFactory getBeanFactory() {
        ConfigurableListableBeanFactory factory = beanFactory;
        if (factory == null) {
            throw new IllegalStateException("Spring BeanFactory 尚未初始化");
        }
        return factory;
    }

    /**
     * 判断 Spring 上下文是否已经初始化。
     *
     * @return 是否已经初始化
     */
    public static boolean isInitialized() {
        return applicationContext != null;
    }

    /**
     * 判断 Spring 上下文是否处于活跃状态。
     *
     * @return 是否活跃
     */
    public static boolean isActive() {
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
     * 获取上下文 ID。
     *
     * @return 上下文 ID
     */
    public static String getApplicationContextId() {
        return getApplicationContext().getId();
    }

    /**
     * 获取上下文展示名称。
     *
     * @return 展示名称
     */
    public static String getDisplayName() {
        return getApplicationContext().getDisplayName();
    }

    /**
     * 获取应用启动时间。
     *
     * @return 应用启动时间
     */
    public static Instant getStartupDate() {
        return Instant.ofEpochMilli(getApplicationContext().getStartupDate());
    }

    /**
     * 获取父级 Spring 上下文。
     *
     * @return 父级上下文 Optional
     */
    public static Optional<ApplicationContext> getParentContext() {
        return Optional.ofNullable(getApplicationContext().getParent());
    }

    /**
     * 根据 Bean 类型获取 Bean。
     *
     * @param requiredType Bean 类型
     * @param <T> Bean 泛型
     * @return Bean 实例
     */
    public static <T> T getBean(Class<T> requiredType) {
        Objects.requireNonNull(requiredType, "requiredType 不能为空");
        return getApplicationContext().getBean(requiredType);
    }

    /**
     * 根据 Bean 类型和构造参数获取 Bean。
     *
     * @param requiredType Bean 类型
     * @param args 参数
     * @param <T> Bean 泛型
     * @return Bean 实例
     */
    public static <T> T getBean(Class<T> requiredType, Object... args) {
        Objects.requireNonNull(requiredType, "requiredType 不能为空");
        return getApplicationContext().getBean(requiredType, args);
    }

    /**
     * 根据 Bean 名称获取 Bean。
     *
     * @param name Bean 名称
     * @return Bean 实例
     */
    public static Object getBean(String name) {
        assertHasText(name, "name 不能为空");
        return getApplicationContext().getBean(name);
    }

    /**
     * 根据 Bean 名称和参数获取 Bean。
     *
     * @param name Bean 名称
     * @param args 参数
     * @return Bean 实例
     */
    public static Object getBean(String name, Object... args) {
        assertHasText(name, "name 不能为空");
        return getApplicationContext().getBean(name, args);
    }

    /**
     * 根据 Bean 名称和类型获取 Bean。
     *
     * @param name Bean 名称
     * @param requiredType Bean 类型
     * @param <T> Bean 泛型
     * @return Bean 实例
     */
    public static <T> T getBean(String name, Class<T> requiredType) {
        assertHasText(name, "name 不能为空");
        Objects.requireNonNull(requiredType, "requiredType 不能为空");
        return getApplicationContext().getBean(name, requiredType);
    }

    /**
     * 根据 Bean 类型安全获取 Bean。
     *
     * @param requiredType Bean 类型
     * @param <T> Bean 泛型
     * @return Bean Optional
     */
    public static <T> Optional<T> getBeanIfPresent(Class<T> requiredType) {
        Objects.requireNonNull(requiredType, "requiredType 不能为空");
        try {
            return Optional.of(getApplicationContext().getBean(requiredType));
        } catch (NoSuchBeanDefinitionException ex) {
            return Optional.empty();
        }
    }

    /**
     * 根据 Bean 名称安全获取 Bean。
     *
     * @param name Bean 名称
     * @return Bean Optional
     */
    public static Optional<Object> getBeanIfPresent(String name) {
        assertHasText(name, "name 不能为空");
        if (!containsBean(name)) {
            return Optional.empty();
        }
        return Optional.of(getApplicationContext().getBean(name));
    }

    /**
     * 获取 Bean 延迟提供器。
     *
     * @param requiredType Bean 类型
     * @param <T> Bean 泛型
     * @return ObjectProvider
     */
    public static <T> ObjectProvider<T> getBeanProvider(Class<T> requiredType) {
        Objects.requireNonNull(requiredType, "requiredType 不能为空");
        return getApplicationContext().getBeanProvider(requiredType);
    }

    /**
     * 根据 Bean 类型获取所有匹配 Bean。
     *
     * @param type Bean 类型
     * @param <T> Bean 泛型
     * @return Bean Map
     */
    public static <T> Map<String, T> getBeansOfType(Class<T> type) {
        Objects.requireNonNull(type, "type 不能为空");
        return getApplicationContext().getBeansOfType(type);
    }

    /**
     * 根据注解获取 Bean。
     *
     * @param annotationType 注解类型
     * @return Bean Map
     */
    public static Map<String, Object> getBeansWithAnnotation(Class<? extends Annotation> annotationType) {
        Objects.requireNonNull(annotationType, "annotationType 不能为空");
        return getApplicationContext().getBeansWithAnnotation(annotationType);
    }

    /**
     * 根据 Bean 类型获取所有 Bean 名称。
     *
     * @param type Bean 类型
     * @return Bean 名称数组
     */
    public static String[] getBeanNamesForType(Class<?> type) {
        Objects.requireNonNull(type, "type 不能为空");
        return getApplicationContext().getBeanNamesForType(type);
    }

    /**
     * 根据注解获取所有 Bean 名称。
     *
     * @param annotationType 注解类型
     * @return Bean 名称数组
     */
    public static String[] getBeanNamesForAnnotation(Class<? extends Annotation> annotationType) {
        Objects.requireNonNull(annotationType, "annotationType 不能为空");
        return getApplicationContext().getBeanNamesForAnnotation(annotationType);
    }

    /**
     * 获取全部 Bean 定义名称。
     *
     * @return Bean 定义名称数组
     */
    public static String[] getBeanDefinitionNames() {
        return getApplicationContext().getBeanDefinitionNames();
    }

    /**
     * 获取 Bean 定义数量。
     *
     * @return Bean 定义数量
     */
    public static int getBeanDefinitionCount() {
        return getApplicationContext().getBeanDefinitionCount();
    }

    /**
     * 判断是否包含指定名称的 Bean。
     *
     * @param name Bean 名称
     * @return 是否存在
     */
    public static boolean containsBean(String name) {
        assertHasText(name, "name 不能为空");
        return getApplicationContext().containsBean(name);
    }

    /**
     * 判断当前 BeanFactory 是否包含本地 Bean。
     *
     * @param name Bean 名称
     * @return 是否存在
     */
    public static boolean containsLocalBean(String name) {
        assertHasText(name, "name 不能为空");
        return getBeanFactory().containsLocalBean(name);
    }

    /**
     * 判断是否包含指定类型的 Bean。
     *
     * @param type Bean 类型
     * @return 是否存在
     */
    public static boolean containsBean(Class<?> type) {
        Objects.requireNonNull(type, "type 不能为空");
        return getApplicationContext().getBeanNamesForType(type).length > 0;
    }

    /**
     * 判断是否包含指定 Bean 定义。
     *
     * @param beanName Bean 名称
     * @return 是否存在
     */
    public static boolean containsBeanDefinition(String beanName) {
        assertHasText(beanName, "beanName 不能为空");
        return getBeanFactory().containsBeanDefinition(beanName);
    }

    /**
     * 判断是否包含指定单例。
     *
     * @param beanName Bean 名称
     * @return 是否存在
     */
    public static boolean containsSingleton(String beanName) {
        assertHasText(beanName, "beanName 不能为空");
        return getBeanFactory().containsSingleton(beanName);
    }

    /**
     * 判断指定 Bean 是否单例。
     *
     * @param name Bean 名称
     * @return 是否单例
     */
    public static boolean isSingleton(String name) {
        assertHasText(name, "name 不能为空");
        return getApplicationContext().isSingleton(name);
    }

    /**
     * 判断指定 Bean 是否原型。
     *
     * @param name Bean 名称
     * @return 是否原型
     */
    public static boolean isPrototype(String name) {
        assertHasText(name, "name 不能为空");
        return getApplicationContext().isPrototype(name);
    }

    /**
     * 判断指定 Bean 是否匹配目标类型。
     *
     * @param name Bean 名称
     * @param targetType 目标类型
     * @return 是否匹配
     */
    public static boolean isTypeMatch(String name, Class<?> targetType) {
        assertHasText(name, "name 不能为空");
        Objects.requireNonNull(targetType, "targetType 不能为空");
        return getApplicationContext().isTypeMatch(name, targetType);
    }

    /**
     * 获取指定 Bean 的类型。
     *
     * @param name Bean 名称
     * @return Bean 类型
     */
    @Nullable
    public static Class<?> getType(String name) {
        assertHasText(name, "name 不能为空");
        return getApplicationContext().getType(name);
    }

    /**
     * 获取指定 Bean 的别名。
     *
     * @param name Bean 名称
     * @return 别名数组
     */
    public static String[] getAliases(String name) {
        assertHasText(name, "name 不能为空");
        return getApplicationContext().getAliases(name);
    }

    /**
     * 获取 Bean 定义。
     *
     * @param beanName Bean 名称
     * @return Bean 定义
     */
    public static BeanDefinition getBeanDefinition(String beanName) {
        assertHasText(beanName, "beanName 不能为空");
        return getBeanFactory().getBeanDefinition(beanName);
    }

    /**
     * 查找 Bean 上的注解。
     *
     * @param beanName Bean 名称
     * @param annotationType 注解类型
     * @param <A> 注解泛型
     * @return 注解 Optional
     */
    public static <A extends Annotation> Optional<A> findAnnotationOnBean(String beanName, Class<A> annotationType) {
        assertHasText(beanName, "beanName 不能为空");
        Objects.requireNonNull(annotationType, "annotationType 不能为空");
        return Optional.ofNullable(getApplicationContext().findAnnotationOnBean(beanName, annotationType));
    }

    /**
     * 对已有对象执行依赖注入。
     *
     * @param existingBean 已有对象
     */
    public static void autowireBean(Object existingBean) {
        Objects.requireNonNull(existingBean, "existingBean 不能为空");
        getBeanFactory().autowireBean(existingBean);
    }

    /**
     * 对已有对象执行依赖注入和 Bean 初始化流程。
     *
     * @param existingBean 已有对象
     * @param beanName Bean 名称
     * @return 初始化后的 Bean
     */
    public static Object configureBean(Object existingBean, String beanName) {
        Objects.requireNonNull(existingBean, "existingBean 不能为空");
        assertHasText(beanName, "beanName 不能为空");
        return getBeanFactory().configureBean(existingBean, beanName);
    }

    /**
     * 初始化 Bean。
     *
     * @param existingBean 已有对象
     * @param beanName Bean 名称
     * @return 初始化后的 Bean
     */
    public static Object initializeBean(Object existingBean, String beanName) {
        Objects.requireNonNull(existingBean, "existingBean 不能为空");
        assertHasText(beanName, "beanName 不能为空");
        return getBeanFactory().initializeBean(existingBean, beanName);
    }

    /**
     * 销毁 Bean。
     *
     * @param existingBean Bean 对象
     */
    public static void destroyBean(Object existingBean) {
        Objects.requireNonNull(existingBean, "existingBean 不能为空");
        getBeanFactory().destroyBean(existingBean);
    }

    /**
     * 动态注册单例 Bean。
     *
     * @param beanName Bean 名称
     * @param singletonObject 单例对象
     */
    public static void registerSingleton(String beanName, Object singletonObject) {
        assertHasText(beanName, "beanName 不能为空");
        Objects.requireNonNull(singletonObject, "singletonObject 不能为空");

        ConfigurableListableBeanFactory factory = getBeanFactory();
        if (factory.containsBean(beanName) || factory.containsSingleton(beanName)) {
            throw new IllegalStateException("Bean 已存在，禁止重复注册：" + beanName);
        }

        factory.registerSingleton(beanName, singletonObject);
        log.info("动态注册单例 Bean：{}", beanName);
    }

    /**
     * 销毁动态注册的单例 Bean。
     *
     * @param beanName Bean 名称
     */
    public static void destroySingleton(String beanName) {
        assertHasText(beanName, "beanName 不能为空");

        ConfigurableListableBeanFactory factory = getBeanFactory();
        if (!factory.containsSingleton(beanName)) {
            return;
        }

        if (factory instanceof DefaultSingletonBeanRegistry registry) {
            registry.destroySingleton(beanName);
            log.info("销毁动态单例 Bean：{}", beanName);
            return;
        }

        throw new IllegalStateException("当前 BeanFactory 不支持销毁单例 Bean：" + factory.getClass().getName());
    }

    /**
     * 动态注册 BeanDefinition。
     *
     * @param beanName Bean 名称
     * @param beanClass Bean 类型
     */
    public static void registerBeanDefinition(String beanName, Class<?> beanClass) {
        assertHasText(beanName, "beanName 不能为空");
        Objects.requireNonNull(beanClass, "beanClass 不能为空");

        BeanDefinition beanDefinition = BeanDefinitionBuilder
                .genericBeanDefinition(beanClass)
                .getBeanDefinition();

        registerBeanDefinition(beanName, beanDefinition);
    }

    /**
     * 动态注册 BeanDefinition。
     *
     * @param beanName Bean 名称
     * @param beanDefinition Bean 定义
     */
    public static void registerBeanDefinition(String beanName, BeanDefinition beanDefinition) {
        assertHasText(beanName, "beanName 不能为空");
        Objects.requireNonNull(beanDefinition, "beanDefinition 不能为空");

        ConfigurableListableBeanFactory factory = getBeanFactory();
        if (factory.containsBean(beanName) || factory.containsBeanDefinition(beanName)) {
            throw new IllegalStateException("BeanDefinition 已存在，禁止重复注册：" + beanName);
        }

        if (factory instanceof BeanDefinitionRegistry registry) {
            registry.registerBeanDefinition(beanName, beanDefinition);
            log.info("动态注册 BeanDefinition：{}", beanName);
            return;
        }

        throw new IllegalStateException("当前 BeanFactory 不支持注册 BeanDefinition：" + factory.getClass().getName());
    }

    /**
     * 通过 Supplier 动态注册 BeanDefinition。
     *
     * @param beanName Bean 名称
     * @param beanClass Bean 类型
     * @param supplier Bean 供应器
     * @param <T> Bean 泛型
     */
    public static <T> void registerBeanDefinition(String beanName, Class<T> beanClass, Supplier<T> supplier) {
        assertHasText(beanName, "beanName 不能为空");
        Objects.requireNonNull(beanClass, "beanClass 不能为空");
        Objects.requireNonNull(supplier, "supplier 不能为空");

        GenericBeanDefinition beanDefinition = new GenericBeanDefinition();
        beanDefinition.setBeanClass(beanClass);
        beanDefinition.setInstanceSupplier(supplier);

        registerBeanDefinition(beanName, beanDefinition);
    }

    /**
     * 移除 BeanDefinition。
     *
     * @param beanName Bean 名称
     */
    public static void removeBeanDefinition(String beanName) {
        assertHasText(beanName, "beanName 不能为空");

        ConfigurableListableBeanFactory factory = getBeanFactory();
        if (!factory.containsBeanDefinition(beanName)) {
            return;
        }

        if (factory instanceof BeanDefinitionRegistry registry) {
            registry.removeBeanDefinition(beanName);
            log.info("移除 BeanDefinition：{}", beanName);
            return;
        }

        throw new IllegalStateException("当前 BeanFactory 不支持移除 BeanDefinition：" + factory.getClass().getName());
    }

    /**
     * 获取环境对象。
     *
     * @return Environment
     */
    public static Environment getEnvironment() {
        return getApplicationContext().getEnvironment();
    }

    /**
     * 获取可配置环境对象。
     *
     * @return ConfigurableEnvironment
     */
    public static ConfigurableEnvironment getConfigurableEnvironment() {
        Environment environment = getEnvironment();
        if (environment instanceof ConfigurableEnvironment configurableEnvironment) {
            return configurableEnvironment;
        }
        throw new IllegalStateException("当前 Environment 不支持 ConfigurableEnvironment：" + environment.getClass().getName());
    }

    /**
     * 获取类型转换服务。
     *
     * @return ConversionService
     */
    public static ConversionService getConversionService() {
        return getConfigurableEnvironment().getConversionService();
    }

    /**
     * 转换数据类型。
     *
     * @param source 原始值
     * @param targetType 目标类型
     * @param <T> 目标泛型
     * @return 转换后的值
     */
    @Nullable
    public static <T> T convert(@Nullable Object source, Class<T> targetType) {
        Objects.requireNonNull(targetType, "targetType 不能为空");
        if (source == null) {
            return null;
        }
        return getConversionService().convert(source, targetType);
    }

    /**
     * 获取配置属性。
     *
     * @param key 配置键
     * @return 配置值
     */
    @Nullable
    public static String getProperty(String key) {
        assertHasText(key, "key 不能为空");
        return getEnvironment().getProperty(key);
    }

    /**
     * 获取配置属性，支持默认值。
     *
     * @param key 配置键
     * @param defaultValue 默认值
     * @return 配置值
     */
    public static String getProperty(String key, String defaultValue) {
        assertHasText(key, "key 不能为空");
        return getEnvironment().getProperty(key, defaultValue);
    }

    /**
     * 获取指定类型的配置属性。
     *
     * @param key 配置键
     * @param targetType 目标类型
     * @param <T> 目标泛型
     * @return 配置值
     */
    @Nullable
    public static <T> T getProperty(String key, Class<T> targetType) {
        assertHasText(key, "key 不能为空");
        Objects.requireNonNull(targetType, "targetType 不能为空");
        return getEnvironment().getProperty(key, targetType);
    }

    /**
     * 获取指定类型的配置属性，支持默认值。
     *
     * @param key 配置键
     * @param targetType 目标类型
     * @param defaultValue 默认值
     * @param <T> 目标泛型
     * @return 配置值
     */
    public static <T> T getProperty(String key, Class<T> targetType, T defaultValue) {
        assertHasText(key, "key 不能为空");
        Objects.requireNonNull(targetType, "targetType 不能为空");
        return getEnvironment().getProperty(key, targetType, defaultValue);
    }

    /**
     * 获取必填配置属性。
     *
     * @param key 配置键
     * @return 配置值
     */
    public static String getRequiredProperty(String key) {
        assertHasText(key, "key 不能为空");
        return getEnvironment().getRequiredProperty(key);
    }

    /**
     * 获取必填配置属性并转换为指定类型。
     *
     * @param key 配置键
     * @param targetType 目标类型
     * @param <T> 目标泛型
     * @return 配置值
     */
    public static <T> T getRequiredProperty(String key, Class<T> targetType) {
        assertHasText(key, "key 不能为空");
        Objects.requireNonNull(targetType, "targetType 不能为空");
        return getEnvironment().getRequiredProperty(key, targetType);
    }

    /**
     * 判断配置属性是否存在。
     *
     * @param key 配置键
     * @return 是否存在
     */
    public static boolean containsProperty(String key) {
        assertHasText(key, "key 不能为空");
        return getEnvironment().containsProperty(key);
    }

    /**
     * 解析占位符，不存在的占位符会原样保留。
     *
     * @param text 文本
     * @return 解析后的文本
     */
    public static String resolvePlaceholders(String text) {
        assertHasText(text, "text 不能为空");
        return getEnvironment().resolvePlaceholders(text);
    }

    /**
     * 解析必填占位符，不存在的占位符会抛出异常。
     *
     * @param text 文本
     * @return 解析后的文本
     */
    public static String resolveRequiredPlaceholders(String text) {
        assertHasText(text, "text 不能为空");
        return getEnvironment().resolveRequiredPlaceholders(text);
    }

    /**
     * 解析嵌入值。
     *
     * @param value 嵌入表达式
     * @return 解析后的值
     */
    @Nullable
    public static String resolveEmbeddedValue(String value) {
        assertHasText(value, "value 不能为空");
        return getBeanFactory().resolveEmbeddedValue(value);
    }

    /**
     * 获取当前激活 Profile。
     *
     * @return Profile 数组
     */
    public static String[] getActiveProfiles() {
        return getEnvironment().getActiveProfiles();
    }

    /**
     * 获取默认 Profile。
     *
     * @return Profile 数组
     */
    public static String[] getDefaultProfiles() {
        return getEnvironment().getDefaultProfiles();
    }

    /**
     * 判断当前环境是否匹配 Profile 表达式。
     *
     * @param profileExpression Profile 表达式，例如 prod、!dev
     * @return 是否匹配
     */
    public static boolean matchesProfile(String profileExpression) {
        assertHasText(profileExpression, "profileExpression 不能为空");
        return getEnvironment().matchesProfiles(profileExpression);
    }

    /**
     * 判断当前环境是否匹配任意 Profile。
     *
     * @param profiles Profile 数组
     * @return 是否匹配
     */
    public static boolean matchesAnyProfile(String... profiles) {
        Objects.requireNonNull(profiles, "profiles 不能为空");
        return Arrays.stream(profiles)
                .filter(Objects::nonNull)
                .map(String::trim)
                .filter(profile -> !profile.isEmpty())
                .anyMatch(SpringUtil::matchesProfile);
    }

    /**
     * 判断当前环境是否匹配全部 Profile。
     *
     * @param profiles Profile 数组
     * @return 是否全部匹配
     */
    public static boolean matchesAllProfiles(String... profiles) {
        Objects.requireNonNull(profiles, "profiles 不能为空");
        return Arrays.stream(profiles)
                .filter(Objects::nonNull)
                .map(String::trim)
                .filter(profile -> !profile.isEmpty())
                .allMatch(SpringUtil::matchesProfile);
    }

    /**
     * 获取应用名称。
     *
     * @return 应用名称
     */
    public static String getApplicationName() {
        return getProperty("spring.application.name", "");
    }

    /**
     * 获取配置端口。
     *
     * @return 配置端口
     */
    public static int getServerPort() {
        return getProperty("server.port", Integer.class, 8080);
    }

    /**
     * 获取实际运行端口。
     *
     * @return 实际运行端口
     */
    public static int getLocalServerPort() {
        Integer localServerPort = getProperty("local.server.port", Integer.class);
        if (localServerPort != null) {
            return localServerPort;
        }

        ApplicationContext context = getApplicationContext();
        if (context instanceof WebServerApplicationContext webServerApplicationContext
                && webServerApplicationContext.getWebServer() != null) {
            return webServerApplicationContext.getWebServer().getPort();
        }

        return getServerPort();
    }

    /**
     * 发布 Spring 应用事件。
     *
     * @param event 事件对象
     */
    public static void publishEvent(Object event) {
        Objects.requireNonNull(event, "event 不能为空");

        ApplicationEventPublisher publisher = applicationEventPublisher;
        if (publisher != null) {
            publisher.publishEvent(event);
            return;
        }

        getApplicationContext().publishEvent(event);
    }

    /**
     * 获取国际化消息。
     *
     * @param code 消息编码
     * @return 国际化消息
     */
    public static String getMessage(String code) {
        assertHasText(code, "code 不能为空");
        return getApplicationContext().getMessage(code, null, LocaleContextHolder.getLocale());
    }

    /**
     * 获取国际化消息。
     *
     * @param code 消息编码
     * @param args 参数
     * @return 国际化消息
     */
    public static String getMessage(String code, Object[] args) {
        assertHasText(code, "code 不能为空");
        return getApplicationContext().getMessage(code, args, LocaleContextHolder.getLocale());
    }

    /**
     * 获取国际化消息。
     *
     * @param code 消息编码
     * @param args 参数
     * @param defaultMessage 默认消息
     * @return 国际化消息
     */
    public static String getMessage(String code, @Nullable Object[] args, String defaultMessage) {
        assertHasText(code, "code 不能为空");
        return getApplicationContext().getMessage(code, args, defaultMessage, LocaleContextHolder.getLocale());
    }

    /**
     * 获取国际化消息。
     *
     * @param code 消息编码
     * @param args 参数
     * @param defaultMessage 默认消息
     * @param locale 语言环境
     * @return 国际化消息
     */
    public static String getMessage(String code, @Nullable Object[] args, String defaultMessage, Locale locale) {
        assertHasText(code, "code 不能为空");
        Objects.requireNonNull(locale, "locale 不能为空");
        return getApplicationContext().getMessage(code, args, defaultMessage, locale);
    }

    /**
     * 获取国际化消息。
     *
     * @param resolvable 消息解析对象
     * @return 国际化消息
     */
    public static String getMessage(MessageSourceResolvable resolvable) {
        Objects.requireNonNull(resolvable, "resolvable 不能为空");
        return getApplicationContext().getMessage(resolvable, LocaleContextHolder.getLocale());
    }

    /**
     * 根据位置获取资源。
     *
     * @param location 资源位置
     * @return Resource
     */
    public static Resource getResource(String location) {
        assertHasText(location, "location 不能为空");
        return getApplicationContext().getResource(location);
    }

    /**
     * 根据位置表达式获取资源数组。
     *
     * @param locationPattern 资源位置表达式
     * @return Resource 数组
     */
    public static Resource[] getResources(String locationPattern) {
        assertHasText(locationPattern, "locationPattern 不能为空");
        try {
            return getApplicationContext().getResources(locationPattern);
        } catch (IOException ex) {
            throw new UncheckedIOException("获取 Spring 资源失败：" + locationPattern, ex);
        }
    }

    /**
     * 读取资源输入流。
     *
     * @param location 资源位置
     * @return 输入流
     */
    public static InputStream getResourceAsStream(String location) {
        assertHasText(location, "location 不能为空");
        try {
            Resource resource = getResource(location);
            if (!resource.exists()) {
                throw new IllegalStateException("资源不存在：" + location);
            }
            return resource.getInputStream();
        } catch (IOException ex) {
            throw new UncheckedIOException("读取 Spring 资源失败：" + location, ex);
        }
    }

    /**
     * 读取资源字节数组。
     *
     * @param location 资源位置
     * @return 字节数组
     */
    public static byte[] readResourceBytes(String location) {
        try (InputStream inputStream = getResourceAsStream(location)) {
            return inputStream.readAllBytes();
        } catch (IOException ex) {
            throw new UncheckedIOException("读取 Spring 资源字节失败：" + location, ex);
        }
    }

    /**
     * 读取 UTF-8 资源文本。
     *
     * @param location 资源位置
     * @return 资源文本
     */
    public static String readResourceText(String location) {
        return readResourceText(location, StandardCharsets.UTF_8);
    }

    /**
     * 读取指定编码资源文本。
     *
     * @param location 资源位置
     * @param charset 字符集
     * @return 资源文本
     */
    public static String readResourceText(String location, Charset charset) {
        assertHasText(location, "location 不能为空");
        Objects.requireNonNull(charset, "charset 不能为空");
        return new String(readResourceBytes(location), charset);
    }

    /**
     * 获取当前请求属性。
     *
     * @return 请求属性 Optional
     */
    public static Optional<ServletRequestAttributes> getCurrentRequestAttributes() {
        RequestAttributes requestAttributes = RequestContextHolder.getRequestAttributes();
        if (requestAttributes instanceof ServletRequestAttributes servletRequestAttributes) {
            return Optional.of(servletRequestAttributes);
        }
        return Optional.empty();
    }

    /**
     * 获取当前 HTTP 请求。
     *
     * @return HTTP 请求 Optional
     */
    public static Optional<HttpServletRequest> getCurrentRequest() {
        return getCurrentRequestAttributes().map(ServletRequestAttributes::getRequest);
    }

    /**
     * 获取当前 HTTP 响应。
     *
     * @return HTTP 响应 Optional
     */
    public static Optional<HttpServletResponse> getCurrentResponse() {
        return getCurrentRequestAttributes().map(ServletRequestAttributes::getResponse);
    }

    /**
     * 获取当前 HTTP 请求，不存在时抛出异常。
     *
     * @return HTTP 请求
     */
    public static HttpServletRequest getRequiredCurrentRequest() {
        return getCurrentRequest()
                .orElseThrow(() -> new IllegalStateException("当前线程不存在 HTTP 请求上下文"));
    }

    /**
     * 获取当前 HTTP 响应，不存在时抛出异常。
     *
     * @return HTTP 响应
     */
    public static HttpServletResponse getRequiredCurrentResponse() {
        return getCurrentResponse()
                .orElseThrow(() -> new IllegalStateException("当前线程不存在 HTTP 响应上下文"));
    }

    /**
     * 获取 ServletContext。
     *
     * @return ServletContext Optional
     */
    public static Optional<ServletContext> getServletContext() {
        ApplicationContext context = getApplicationContext();
        if (context instanceof WebApplicationContext webApplicationContext) {
            return Optional.ofNullable(webApplicationContext.getServletContext());
        }
        return getCurrentRequest().map(HttpServletRequest::getServletContext);
    }

    /**
     * 获取当前请求 URI。
     *
     * @return 请求 URI Optional
     */
    public static Optional<String> getCurrentRequestUri() {
        return getCurrentRequest().map(HttpServletRequest::getRequestURI);
    }

    /**
     * 获取当前请求 URL。
     *
     * @return 请求 URL Optional
     */
    public static Optional<String> getCurrentRequestUrl() {
        return getCurrentRequest().map(request -> request.getRequestURL().toString());
    }

    /**
     * 获取当前请求方法。
     *
     * @return 请求方法 Optional
     */
    public static Optional<String> getCurrentRequestMethod() {
        return getCurrentRequest().map(HttpServletRequest::getMethod);
    }

    /**
     * 获取当前请求客户端 IP。
     *
     * @return 客户端 IP Optional
     */
    public static Optional<String> getCurrentClientIp() {
        return getCurrentRequest().map(SpringUtil::resolveClientIp);
    }

    /**
     * 关闭 Spring 上下文。
     */
    public static void closeApplicationContext() {
        ApplicationContext context = applicationContext;
        if (context instanceof ConfigurableApplicationContext configurableApplicationContext) {
            configurableApplicationContext.close();
            log.info("关闭 Spring 应用上下文");
        }
    }

    /**
     * 解析客户端 IP。
     *
     * @param request HTTP 请求
     * @return 客户端 IP
     */
    private static String resolveClientIp(HttpServletRequest request) {
        String[] headerNames = {
                "X-Forwarded-For",
                "X-Real-IP",
                "Proxy-Client-IP",
                "WL-Proxy-Client-IP",
                "HTTP_CLIENT_IP",
                "HTTP_X_FORWARDED_FOR"
        };

        for (String headerName : headerNames) {
            String value = request.getHeader(headerName);
            if (hasText(value) && !"unknown".equalsIgnoreCase(value)) {
                int commaIndex = value.indexOf(',');
                return commaIndex >= 0 ? value.substring(0, commaIndex).trim() : value.trim();
            }
        }

        return request.getRemoteAddr();
    }

    /**
     * 校验字符串是否有文本内容。
     *
     * @param text 字符串
     * @param message 异常消息
     */
    private static void assertHasText(String text, String message) {
        if (!hasText(text)) {
            throw new IllegalArgumentException(message);
        }
    }

    /**
     * 判断字符串是否有文本内容。
     *
     * @param text 字符串
     * @return 是否有文本
     */
    private static boolean hasText(String text) {
        return text != null && !text.trim().isEmpty();
    }
}