package local.ateng.java.customutils.utils;

import org.springframework.beans.BeansException;
import org.springframework.beans.factory.NoSuchBeanDefinitionException;
import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;
import org.springframework.beans.factory.support.DefaultSingletonBeanRegistry;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.bind.Bindable;
import org.springframework.boot.context.properties.bind.Binder;
import org.springframework.context.*;
import org.springframework.core.ResolvableType;
import org.springframework.core.annotation.AnnotatedElementUtils;
import org.springframework.core.env.Environment;
import org.springframework.core.io.Resource;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Component;
import org.springframework.util.FileCopyUtils;
import org.springframework.web.context.WebApplicationContext;
import org.springframework.web.context.request.RequestAttributes;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;
import org.springframework.web.util.UriComponents;
import org.springframework.web.util.UriComponentsBuilder;

import javax.servlet.ServletContext;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.lang.annotation.Annotation;
import java.lang.management.ManagementFactory;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

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
     * 未知标识常量
     */
    private static final String UNKNOWN = "unknown";

    /**
     * 资源路径前缀：classpath
     */
    private static final String CLASSPATH_PREFIX = "classpath:";

    /**
     * 设置 Spring 上下文（由 Spring 自动调用）
     *
     * @param applicationContext Spring 上下文
     * @throws BeansException 设置异常
     */
    @Override
    public void setApplicationContext(@NonNull ApplicationContext applicationContext) throws BeansException {
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
    public void setApplicationEventPublisher(@NonNull ApplicationEventPublisher applicationEventPublisher) {
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
     * 获取指定类型的第一个 Bean（按 Spring 容器注册顺序）
     *
     * <p>
     * 如果指定类型在容器中存在多个实现类，将返回第一个注册的 Bean。
     * 如果不存在该类型的 Bean，则返回 null。
     * </p>
     *
     * <p><b>示例：</b></p>
     * <pre>{@code
     * MyService service = SpringUtil.getFirstBean(MyService.class);
     * if (service != null) {
     *     service.doSomething();
     * }
     * }</pre>
     *
     * @param type Bean 类型
     * @param <T>  泛型
     * @return 指定类型的第一个 Bean 实例，如果不存在返回 null
     */
    public static <T> T getFirstBean(Class<T> type) {
        Map<String, T> beans = getAllBeans(type);
        return beans.isEmpty() ? null : beans.values().iterator().next();
    }

    /**
     * 获取指定类型的所有 Bean
     *
     * @param type Bean 类型
     * @param <T>  泛型
     * @return Bean 名称与实例映射，如果不存在返回空 Map
     */
    public static <T> Map<String, T> getAllBeans(Class<T> type) {
        return getApplicationContext().getBeansOfType(type);
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
     * 获取当前服务端口号（server.port）
     *
     * @return 服务端口号，默认为 8080
     */
    public static int getServerPort() {
        return getEnvironment().getProperty("server.port", Integer.class, 8080);
    }

    /**
     * 获取上下文路径（server.servlet.context-path）
     *
     * @return 上下文路径，未设置则返回空串
     */
    public static String getContextPath() {
        return getEnvironment().getProperty("server.servlet.context-path", "");
    }

    /**
     * 获取指定 logger 的日志级别（如 logging.level.com.example=DEBUG）
     *
     * @param loggerName 日志名称（如 com.example）
     * @return 对应日志级别（如 DEBUG、INFO），找不到返回 null
     */
    public static String getLogLevel(String loggerName) {
        return getEnvironment().getProperty("logging.level." + loggerName);
    }

    /**
     * 获取当前 Spring Boot 应用的名称（spring.application.name）
     *
     * @return 应用名称，若未配置则返回 null
     */
    public static String getAppName() {
        return getEnvironment().getProperty("spring.application.name");
    }

    /**
     * 获取 Spring 应用启动时间（时间戳）
     *
     * @return 启动时间（毫秒值），若上下文未初始化则返回 -1
     */
    public static long getApplicationStartupTime() {
        ApplicationContext context = getApplicationContext();
        return context != null ? context.getStartupDate() : -1;
    }

    /**
     * 获取当前 Java 进程的 PID（适配 JDK 8）
     *
     * @return 当前进程的 PID，失败返回 -1
     */
    public static long getPid() {
        // 分隔符，用于提取 PID
        final String atSymbol = "@";
        try {
            String jvmName = ManagementFactory.getRuntimeMXBean().getName();
            if (jvmName != null && jvmName.contains(atSymbol)) {
                return Long.parseLong(jvmName.split(atSymbol)[0]);
            }
        } catch (Exception ignored) {
        }
        return -1;
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
    public static String getSystemEnv(String name) {
        return System.getenv(name);
    }

    /**
     * 获取指定环境变量（带默认值）
     *
     * @param name         环境变量名称
     * @param defaultValue 默认值
     * @return 环境变量值或默认值
     */
    public static String getSystemEnv(String name, String defaultValue) {
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

    /**
     * 获取当前 ServletContext 对象
     *
     * @return ServletContext 对象，若无法获取则返回 null
     */
    public static ServletContext getServletContext() {
        ApplicationContext ctx = getApplicationContext();
        if (ctx instanceof WebApplicationContext) {
            return ((WebApplicationContext) ctx).getServletContext();
        }
        return null;
    }

    /**
     * 获取 ServletRequestAttributes（请求上下文属性对象）
     *
     * @return ServletRequestAttributes，若不存在则返回 null
     */
    public static ServletRequestAttributes getServletRequestAttributes() {
        RequestAttributes attributes = RequestContextHolder.getRequestAttributes();
        return attributes instanceof ServletRequestAttributes ? (ServletRequestAttributes) attributes : null;
    }

    /**
     * 获取当前请求对象（HttpServletRequest）
     *
     * @return 当前请求对象，若不存在则返回 null
     */
    public static HttpServletRequest getHttpServletRequest() {
        ServletRequestAttributes attributes = getServletRequestAttributes();
        return attributes != null ? attributes.getRequest() : null;
    }

    /**
     * 获取当前响应对象（HttpServletResponse）
     *
     * @return 当前响应对象，若不存在则返回 null
     */
    public static HttpServletResponse getHttpServletResponse() {
        ServletRequestAttributes attributes = getServletRequestAttributes();
        return attributes != null ? attributes.getResponse() : null;
    }

    /**
     * 获取当前会话对象（HttpSession），若无请求上下文或会话返回 null
     *
     * @return 当前 HttpSession
     */
    public static HttpSession getHttpSession() {
        HttpServletRequest request = getHttpServletRequest();
        return request != null ? request.getSession(false) : null;
    }

    /**
     * 获取客户端真实 IP 地址，兼容多层代理场景。
     *
     * @return 客户端 IP；如果请求不存在则返回 null
     */
    public static String getClientIpAddress() {
        HttpServletRequest request = getHttpServletRequest();
        if (request == null) {
            return null;
        }
        return getClientIpAddress(request);
    }

    /**
     * 获取客户端 IP，支持自定义默认值兜底
     *
     * @param defaultIp 默认 IP（如 unknown / 0.0.0.0）
     * @return 客户端 IP
     */
    public static String getClientIpAddressOrDefault(String defaultIp) {
        String ip = getClientIpAddress();
        return StringUtil.isBlank(ip) ? defaultIp : ip;
    }

    /**
     * 获取客户端真实 IP 地址，兼容多层代理场景。
     *
     * @param request HTTP 请求
     * @return 客户端 IP
     */
    public static String getClientIpAddress(HttpServletRequest request) {
        if (request == null) {
            return null;
        }

        String ip = resolveClientIp(request);
        if (StringUtil.isBlank(ip)) {
            ip = request.getRemoteAddr();
        }

        return normalizeIp(ip);
    }

    /**
     * 解析客户端 IP，按常见代理头部顺序依次获取。
     *
     * @param request HTTP 请求
     * @return 解析到的 IP；未获取到则返回 null
     */
    private static String resolveClientIp(HttpServletRequest request) {
        String[] headerNames = {
                "X-Forwarded-For",
                "X-Real-IP",
                "Proxy-Client-IP",
                "WL-Proxy-Client-IP",
                "HTTP_CLIENT_IP",
                "HTTP_X_FORWARDED_FOR",
                "CF-Connecting-IP"
        };

        for (String headerName : headerNames) {
            String ip = request.getHeader(headerName);
            String resolvedIp = extractFirstIp(ip);
            if (StringUtil.isNotBlank(resolvedIp)) {
                return resolvedIp;
            }
        }

        return null;
    }

    /**
     * 从代理头中提取第一个有效 IP。
     * <p>
     * 例如：X-Forwarded-For: 10.0.0.1, 192.168.1.1
     * 会返回 10.0.0.1。
     *
     * @param ipHeader 代理头值
     * @return 第一个有效 IP；无效时返回 null
     */
    private static String extractFirstIp(String ipHeader) {
        if (StringUtil.isBlank(ipHeader) || UNKNOWN.equalsIgnoreCase(ipHeader)) {
            return null;
        }

        String[] ipArray = StringUtil.split(ipHeader, ",");
        if (ipArray == null || ipArray.length == 0) {
            return null;
        }

        for (String ip : ipArray) {
            if (StringUtil.isBlank(ip) || UNKNOWN.equalsIgnoreCase(ip)) {
                continue;
            }
            return ip;
        }

        return null;
    }

    /**
     * 标准化 IP 地址，处理常见本地回环写法。
     *
     * @param ip 原始 IP
     * @return 标准化后的 IP
     */
    private static String normalizeIp(String ip) {
        if (StringUtil.isBlank(ip)) {
            return ip;
        }

        String trimmedIp = ip.trim();
        if ("0:0:0:0:0:0:0:1".equals(trimmedIp)) {
            return "127.0.0.1";
        }
        if ("::1".equals(trimmedIp)) {
            return "127.0.0.1";
        }

        return trimmedIp;
    }

    /**
     * 获取 resources 目录下指定路径的资源
     *
     * <p>
     * 示例：获取 resources/config/app.yml
     * <pre>{@code
     * Resource resource = SpringUtil.getResource("config/app.yml");
     * }</pre>
     * </p>
     *
     * @param path 相对于 resources 的路径
     * @return Resource 对象
     */
    public static Resource getResource(String path) {
        return getApplicationContext().getResource(CLASSPATH_PREFIX + path);
    }

    /**
     * 获取 classpath 文件的输入流
     *
     * @param path 文件路径
     * @return 输入流，未找到时返回 null
     */
    public static InputStream getResourceInputStream(String path) {
        try {
            return getResource(path).getInputStream();
        } catch (Exception e) {
            return null;
        }
    }

    /**
     * 读取 classpath 文件内容为字符串
     *
     * @param path 文件路径
     * @return 文件内容字符串，异常时返回 null
     */
    public static String getResourceReadString(String path) {
        try (InputStream in = getResourceInputStream(path)) {
            if (in == null) {
                return null;
            }
            byte[] bytes = FileCopyUtils.copyToByteArray(in);
            return new String(bytes, StandardCharsets.UTF_8);
        } catch (Exception e) {
            return null;
        }
    }

    /**
     * 按行读取 classpath 文件内容
     *
     * @param path 文件路径
     * @return 文件内容行列表，异常时返回 null
     */
    public static List<String> getResourceReadLines(String path) {
        try (BufferedReader reader = new BufferedReader(
                new InputStreamReader(getResourceInputStream(path), StandardCharsets.UTF_8))) {
            return reader.lines().collect(Collectors.toList());
        } catch (Exception e) {
            return null;
        }
    }

    /**
     * 读取 classpath 文件为字节数组
     *
     * @param path 文件路径
     * @return 字节数组，异常时返回 null
     */
    public static byte[] getResourceReadBytes(String path) {
        try (InputStream in = getResourceInputStream(path)) {
            if (in == null) {
                return null;
            }
            return FileCopyUtils.copyToByteArray(in);
        } catch (Exception e) {
            return null;
        }
    }

    /**
     * 获取 Spring Boot 启动类所在的基础包名。
     *
     * <p>
     * 该方法通过扫描 Spring 容器中所有 Bean，查找带有
     * {@link SpringBootApplication} 注解的类，并返回该类所在的包名。
     * </p>
     *
     * <p><b>示例：</b></p>
     * <pre>{@code
     * String mainPackage = SpringUtil.getMainApplicationPackage();
     * System.out.println("主包路径：" + mainPackage);
     * }</pre>
     *
     * <p><b>注意事项：</b></p>
     * <ul>
     *     <li>如果启动类使用了自定义组合注解（例如 @MySpringCloudApplication 包含 @SpringBootApplication），
     *     该方法可能返回 null，因为启动类未注册为普通 Bean。</li>
     *     <li>对于代理类或工厂生成的 Bean，{@code beanClass.getPackage()} 可能为 null。</li>
     *     <li>因此该方法仅在启动类本身被 Spring 容器扫描为 Bean 的场景下可靠。</li>
     * </ul>
     *
     * @return 启动类所在的根包名，例如 "com.example.project"，获取失败返回 null
     */
    public static String getMainApplicationPackage() {
        try {
            ApplicationContext context = getApplicationContext();
            if (context != null) {
                String[] beanNames = context.getBeanDefinitionNames();
                for (String beanName : beanNames) {
                    Class<?> beanClass = context.getType(beanName);
                    if (beanClass != null &&
                            AnnotatedElementUtils.hasAnnotation(beanClass, SpringBootApplication.class)) {
                        return beanClass.getPackage().getName();
                    }
                }
            }
        } catch (Exception ignored) {
        }
        return null;
    }

    /**
     * 获取指定启动类所在的基础包名。
     *
     * <p>
     * 该方法直接使用启动类 Class 对象获取包名，更加可靠，适用于以下场景：
     * </p>
     *
     * <ul>
     *     <li>启动类未注册为 Spring Bean</li>
     *     <li>使用自定义组合注解（@MySpringCloudApplication）</li>
     *     <li>存在代理类或工厂 Bean，避免 getPackage() 返回 null</li>
     * </ul>
     *
     * <p><b>示例：</b></p>
     * <pre>{@code
     * String mainPackage = SpringUtil.getMainApplicationPackage(MyCaApplication.class);
     * System.out.println("主包路径：" + mainPackage);
     * }</pre>
     *
     * @param mainClazz 启动类 Class 对象
     * @return 启动类所在包名，如果参数为 null 则返回 null
     */
    public static String getMainApplicationPackage(Class<?> mainClazz) {
        return mainClazz != null ? mainClazz.getPackage().getName() : null;
    }

    /**
     * 构建完整的 URL，支持基础链接、查询参数、路径参数以及是否进行编码。
     *
     * <p><b>示例：</b></p>
     * <pre>{@code
     * Map<String, Object> queryParams = new HashMap<>();
     * queryParams.put("page", 1);
     * queryParams.put("size", 10);
     *
     * Map<String, Object> pathVars = new HashMap<>();
     * pathVars.put("id", 1001);
     *
     * String url = SpringUtil.buildUrl(
     *     "http://localhost:8080/api/user/{id}",
     *     queryParams,
     *     pathVars,
     *     true
     * );
     * System.out.println(url); // http://localhost:8080/api/user/1001?page=1&size=10
     * }</pre>
     *
     * @param baseUrl    基础 URL，例如 "http://localhost:8080/api/user/{id}"
     * @param queryParams 查询参数 Map，可为 null
     * @param uriVariables 路径参数 Map，可为 null
     * @param encode     是否进行 URL 编码
     * @return 构建后的完整 URL
     */
    public static String buildUrl(String baseUrl,
                                  Map<String, ?> queryParams,
                                  Map<String, ?> uriVariables,
                                  boolean encode) {
        UriComponentsBuilder builder = UriComponentsBuilder.fromUriString(baseUrl);

        if (queryParams != null && !queryParams.isEmpty()) {
            queryParams.forEach(builder::queryParam);
        }

        UriComponents uriComponents;
        if (uriVariables != null && !uriVariables.isEmpty()) {
            uriComponents = builder.buildAndExpand(uriVariables);
        } else {
            uriComponents = builder.build();
        }

        return encode ? uriComponents.encode().toUriString() : uriComponents.toUriString();
    }

    /**
     * 构建带查询参数的 URL。
     * <p>
     * 使用给定的基础地址 {@code baseUrl} 和查询参数 {@code queryParams} 构建完整 URL。
     * 该方法不涉及路径变量替换，适合纯 GET 请求参数场景。
     *
     * @param baseUrl     基础 URL，例如 "http://localhost:8080/api/user"
     * @param queryParams 查询参数键值对，例如 {"name":"Tom","age":18}
     * @return 构建完成的 URL 字符串
     */
    public static String buildUrl(String baseUrl, Map<String, ?> queryParams) {
        return buildUrl(baseUrl, queryParams, null, true);
    }

    /**
     * 构建带路径参数的 URL。
     * <p>
     * 使用给定的基础地址 {@code baseUrl} 和路径变量 {@code uriVariables} 构建完整 URL。
     * 该方法不包含查询参数，常用于 RESTful 风格的接口地址拼接。
     *
     * @param baseUrl      基础 URL，支持占位符，例如 "http://localhost:8080/api/user/{id}"
     * @param uriVariables 路径参数键值对，例如 {"id":1001}
     * @return 构建完成的 URL 字符串
     */
    public static String buildUrlWithPath(String baseUrl, Map<String, ?> uriVariables) {
        return buildUrl(baseUrl, null, uriVariables, true);
    }

    /**
     * 构建不带参数的 URL。
     * <p>
     * 该方法仅对基础地址进行 encode 处理（可选），不附加路径参数或查询参数。
     *
     * @param baseUrl 基础 URL，例如 "http://localhost:8080/api/user"
     * @param encode  是否对 URL 进行编码处理
     * @return 构建完成的 URL 字符串
     */
    public static String buildUrl(String baseUrl, boolean encode) {
        return buildUrl(baseUrl, null, null, encode);
    }

    /**
     * 构建带路径参数与查询参数的 URL。
     * <p>
     * 综合处理路径参数与查询参数，支持 RESTful 占位符替换与 GET 请求参数拼接。
     *
     * @param baseUrl      基础 URL，例如 "http://localhost:8080/api/user/{id}"
     * @param queryParams  查询参数键值对，例如 {"page":1,"size":20}
     * @param uriVariables 路径参数键值对，例如 {"id":1001}
     * @return 构建完成的 URL 字符串
     */
    public static String buildUrl(String baseUrl, Map<String, ?> queryParams, Map<String, ?> uriVariables) {
        return buildUrl(baseUrl, queryParams, uriVariables, true);
    }

    /**
     * 判断 Spring 上下文是否已初始化
     *
     * @return 已初始化返回 true，否则返回 false
     */
    public static boolean isApplicationContextReady() {
        return context != null;
    }

    /**
     * 获取 Spring 上下文 ID
     *
     * @return 上下文 ID，未初始化返回 null
     */
    public static String getApplicationContextId() {
        return context != null ? context.getId() : null;
    }

    /**
     * 获取 Spring 上下文显示名称
     *
     * @return 上下文显示名称，未初始化返回 null
     */
    public static String getApplicationContextDisplayName() {
        return context != null ? context.getDisplayName() : null;
    }

    /**
     * 获取父级 Spring 上下文
     *
     * @return 父级 ApplicationContext，未初始化或无父级返回 null
     */
    public static ApplicationContext getParentApplicationContext() {
        return context != null ? context.getParent() : null;
    }

    /**
     * 判断当前上下文是否存在父级上下文
     *
     * @return 存在父级上下文返回 true，否则返回 false
     */
    public static boolean hasParentApplicationContext() {
        return getParentApplicationContext() != null;
    }

    /**
     * 获取 Bean 的别名数组
     *
     * @param beanName Bean 名称
     * @return Bean 别名数组，若不存在或上下文未初始化返回空数组
     */
    public static String[] getAliases(String beanName) {
        if (context == null || beanName == null) {
            return new String[0];
        }
        return context.getAliases(beanName);
    }

    /**
     * 判断指定名称的 Bean 是否为原型
     *
     * @param name Bean 名称
     * @return 是原型返回 true，否则返回 false
     */
    public static boolean isPrototype(String name) {
        if (context == null || name == null) {
            return false;
        }
        try {
            return context.isPrototype(name);
        } catch (NoSuchBeanDefinitionException e) {
            return false;
        }
    }

    /**
     * 判断当前上下文本地是否包含指定 Bean，不检查父级上下文
     *
     * @param name Bean 名称
     * @return 本地上下文存在返回 true，否则返回 false
     */
    public static boolean containsLocalBean(String name) {
        if (context == null || name == null) {
            return false;
        }
        return context.containsLocalBean(name);
    }

    /**
     * 根据类型安全获取 Bean，不存在时返回 null
     *
     * @param requiredType Bean 类型
     * @param <T>          泛型
     * @return Bean 实例，不存在返回 null
     */
    public static <T> T getBeanOrNull(Class<T> requiredType) {
        if (context == null || requiredType == null) {
            return null;
        }
        try {
            return context.getBean(requiredType);
        } catch (BeansException e) {
            return null;
        }
    }

    /**
     * 根据名称安全获取 Bean，不存在时返回 null
     *
     * @param name Bean 名称
     * @return Bean 实例，不存在返回 null
     */
    public static Object getBeanOrNull(String name) {
        if (context == null || name == null) {
            return null;
        }
        try {
            return context.getBean(name);
        } catch (BeansException e) {
            return null;
        }
    }

    /**
     * 根据名称和类型安全获取 Bean，不存在时返回 null
     *
     * @param name         Bean 名称
     * @param requiredType Bean 类型
     * @param <T>          泛型
     * @return Bean 实例，不存在返回 null
     */
    public static <T> T getBeanOrNull(String name, Class<T> requiredType) {
        if (context == null || name == null || requiredType == null) {
            return null;
        }
        try {
            return context.getBean(name, requiredType);
        } catch (BeansException e) {
            return null;
        }
    }

    /**
     * 根据类型获取 Bean，不存在时返回默认值
     *
     * @param requiredType Bean 类型
     * @param defaultValue 默认值
     * @param <T>          泛型
     * @return Bean 实例或默认值
     */
    public static <T> T getBeanOrDefault(Class<T> requiredType, T defaultValue) {
        T bean = getBeanOrNull(requiredType);
        return bean != null ? bean : defaultValue;
    }

    /**
     * 根据名称获取 Bean，不存在时返回默认值
     *
     * @param name         Bean 名称
     * @param defaultValue 默认值
     * @return Bean 实例或默认值
     */
    public static Object getBeanOrDefault(String name, Object defaultValue) {
        Object bean = getBeanOrNull(name);
        return bean != null ? bean : defaultValue;
    }

    /**
     * 获取指定类型的 ObjectProvider
     *
     * @param requiredType Bean 类型
     * @param <T>          泛型
     * @return ObjectProvider，未初始化返回 null
     */
    public static <T> org.springframework.beans.factory.ObjectProvider<T> getBeanProvider(Class<T> requiredType) {
        if (context == null || requiredType == null) {
            return null;
        }
        return context.getBeanProvider(requiredType);
    }

    /**
     * 获取指定类型可用的 Bean，不存在返回 null，存在多个时按 Spring 规则处理
     *
     * @param requiredType Bean 类型
     * @param <T>          泛型
     * @return Bean 实例，不存在返回 null
     */
    public static <T> T getBeanIfAvailable(Class<T> requiredType) {
        org.springframework.beans.factory.ObjectProvider<T> provider = getBeanProvider(requiredType);
        return provider != null ? provider.getIfAvailable() : null;
    }

    /**
     * 获取指定类型唯一 Bean，若不存在或不唯一返回 null
     *
     * @param requiredType Bean 类型
     * @param <T>          泛型
     * @return 唯一 Bean 实例，否则返回 null
     */
    public static <T> T getBeanIfUnique(Class<T> requiredType) {
        org.springframework.beans.factory.ObjectProvider<T> provider = getBeanProvider(requiredType);
        return provider != null ? provider.getIfUnique() : null;
    }

    /**
     * 获取指定类型的所有 Bean，支持控制是否包含非单例 Bean 以及是否允许提前初始化
     *
     * @param type                Bean 类型
     * @param includeNonSingletons 是否包含非单例 Bean
     * @param allowEagerInit       是否允许提前初始化懒加载 Bean
     * @param <T>                 泛型
     * @return Bean 名称与实例映射
     */
    public static <T> Map<String, T> getAllBeans(Class<T> type, boolean includeNonSingletons, boolean allowEagerInit) {
        if (context == null || type == null) {
            return java.util.Collections.emptyMap();
        }
        return context.getBeansOfType(type, includeNonSingletons, allowEagerInit);
    }

    /**
     * 根据注解获取 Bean 名称数组
     *
     * @param annotationType 注解类型
     * @param <A>            注解泛型
     * @return Bean 名称数组，若无匹配返回空数组
     */
    public static <A extends Annotation> String[] getBeanNamesForAnnotation(Class<A> annotationType) {
        if (context == null || annotationType == null) {
            return new String[0];
        }
        return context.getBeanNamesForAnnotation(annotationType);
    }

    /**
     * 获取指定 Bean 上的注解
     *
     * @param beanName       Bean 名称
     * @param annotationType 注解类型
     * @param <A>            注解泛型
     * @return 注解实例，不存在返回 null
     */
    public static <A extends Annotation> A findAnnotationOnBean(String beanName, Class<A> annotationType) {
        if (context == null || beanName == null || annotationType == null) {
            return null;
        }
        try {
            return context.findAnnotationOnBean(beanName, annotationType);
        } catch (NoSuchBeanDefinitionException e) {
            return null;
        }
    }

    /**
     * 对已有对象执行 Spring 依赖注入
     *
     * @param existingBean 已存在的对象
     */
    public static void autowireBean(Object existingBean) {
        if (context != null && existingBean != null) {
            context.getAutowireCapableBeanFactory().autowireBean(existingBean);
        }
    }

    /**
     * 初始化已有 Bean，触发 BeanPostProcessor 等 Spring 生命周期逻辑
     *
     * @param existingBean 已存在的对象
     * @param beanName     Bean 名称
     * @return 初始化后的 Bean
     */
    public static Object initializeBean(Object existingBean, String beanName) {
        if (context == null || existingBean == null) {
            return existingBean;
        }
        return context.getAutowireCapableBeanFactory().initializeBean(existingBean, beanName);
    }

    /**
     * 销毁已有 Bean，触发 Spring 销毁生命周期逻辑
     *
     * @param existingBean 已存在的对象
     */
    public static void destroyBean(Object existingBean) {
        if (context != null && existingBean != null) {
            context.getAutowireCapableBeanFactory().destroyBean(existingBean);
        }
    }

    /**
     * 获取 AutowireCapableBeanFactory
     *
     * @return AutowireCapableBeanFactory，未初始化返回 null
     */
    public static org.springframework.beans.factory.config.AutowireCapableBeanFactory getAutowireCapableBeanFactory() {
        return context != null ? context.getAutowireCapableBeanFactory() : null;
    }

    /**
     * 获取当前上下文中的 BeanFactory
     *
     * @return ConfigurableListableBeanFactory，非 ConfigurableApplicationContext 时返回 null
     */
    public static org.springframework.beans.factory.config.ConfigurableListableBeanFactory getBeanFactory() {
        if (context instanceof org.springframework.context.ConfigurableApplicationContext) {
            return ((org.springframework.context.ConfigurableApplicationContext) context).getBeanFactory();
        }
        return null;
    }

    /**
     * 注册单例 Bean 到 Spring 容器
     *
     * @param beanName Bean 名称
     * @param bean     Bean 实例
     * @return 注册成功返回 true，否则返回 false
     */
    public static boolean registerSingleton(String beanName, Object bean) {
        org.springframework.beans.factory.config.ConfigurableListableBeanFactory beanFactory = getBeanFactory();
        if (beanFactory == null || beanName == null || bean == null) {
            return false;
        }
        beanFactory.registerSingleton(beanName, bean);
        return true;
    }

    /**
     * 销毁已注册的单例 Bean
     *
     * @param beanName Bean 名称
     * @return 销毁成功返回 true，否则返回 false
     */
    public static boolean destroySingleton(String beanName) {
        ConfigurableListableBeanFactory beanFactory = getBeanFactory();
        if (beanFactory == null || beanName == null || beanName.trim().isEmpty()) {
            return false;
        }

        if (!(beanFactory instanceof DefaultSingletonBeanRegistry)) {
            return false;
        }

        DefaultSingletonBeanRegistry registry =
                (DefaultSingletonBeanRegistry) beanFactory;

        if (!registry.containsSingleton(beanName)) {
            return false;
        }

        registry.destroySingleton(beanName);
        return true;
    }

    /**
     * 判断配置项是否存在
     *
     * @param key 配置 key
     * @return 存在返回 true，否则返回 false
     */
    public static boolean containsProperty(String key) {
        return getEnvironment() != null && getEnvironment().containsProperty(key);
    }

    /**
     * 获取必填配置项，不存在时抛出异常
     *
     * @param key 配置 key
     * @return 配置值
     */
    public static String getRequiredProperty(String key) {
        return getEnvironment().getRequiredProperty(key);
    }

    /**
     * 获取必填配置项并转换为指定类型，不存在时抛出异常
     *
     * @param key        配置 key
     * @param targetType 目标类型
     * @param <T>        泛型
     * @return 配置值
     */
    public static <T> T getRequiredProperty(String key, Class<T> targetType) {
        return getEnvironment().getRequiredProperty(key, targetType);
    }

    /**
     * 获取默认 Profile 数组
     *
     * @return 默认 profiles，未初始化返回空数组
     */
    public static String[] getDefaultProfiles() {
        return context != null ? context.getEnvironment().getDefaultProfiles() : new String[0];
    }

    /**
     * 判断是否接受指定 profiles 表达式
     *
     * @param profiles profiles 表达式
     * @return 匹配返回 true，否则返回 false
     */
    public static boolean acceptsProfiles(String... profiles) {
        if (context == null || profiles == null || profiles.length == 0) {
            return false;
        }
        return context.getEnvironment().acceptsProfiles(org.springframework.core.env.Profiles.of(profiles));
    }

    /**
     * 解析字符串中的占位符，无法解析时抛出异常
     *
     * @param value 带占位符的字符串
     * @return 解析后的字符串
     */
    public static String resolveRequiredPlaceholders(String value) {
        if (context == null || value == null) {
            return value;
        }
        return context.getEnvironment().resolveRequiredPlaceholders(value);
    }

    /**
     * 使用 Binder 绑定配置到指定类型
     *
     * @param key        配置 key
     * @param targetType 目标类型
     * @param <T>        泛型
     * @return 绑定结果，未配置返回 null
     */
    public static <T> T bindProperty(String key, Class<T> targetType) {
        if (context == null || key == null || targetType == null) {
            return null;
        }
        return Binder.get(getEnvironment()).bind(key, targetType).orElse(null);
    }

    /**
     * 使用 Binder 绑定配置到指定类型，未配置返回默认值
     *
     * @param key          配置 key
     * @param targetType   目标类型
     * @param defaultValue 默认值
     * @param <T>          泛型
     * @return 绑定结果或默认值
     */
    public static <T> T bindProperty(String key, Class<T> targetType, T defaultValue) {
        T value = bindProperty(key, targetType);
        return value != null ? value : defaultValue;
    }

    /**
     * 获取指定路径资源是否存在
     *
     * @param path resources 下的相对路径
     * @return 存在返回 true，否则返回 false
     */
    public static boolean resourceExists(String path) {
        Resource resource = getResource(path);
        return resource != null && resource.exists();
    }

    /**
     * 获取指定路径资源文件名
     *
     * @param path resources 下的相对路径
     * @return 文件名，异常时返回 null
     */
    public static String getResourceFilename(String path) {
        try {
            Resource resource = getResource(path);
            return resource != null ? resource.getFilename() : null;
        } catch (Exception e) {
            return null;
        }
    }

    /**
     * 获取指定路径资源 URL
     *
     * @param path resources 下的相对路径
     * @return 资源 URL，异常时返回 null
     */
    public static java.net.URL getResourceUrl(String path) {
        try {
            Resource resource = getResource(path);
            return resource != null ? resource.getURL() : null;
        } catch (Exception e) {
            return null;
        }
    }

    /**
     * 根据 locationPattern 获取多个资源
     *
     * @param locationPattern 资源匹配表达式，例如 {@code classpath*:mapper/**&#47;*.xml}
     * @return Resource 数组，异常时返回空数组
     */
    public static Resource[] getResources(String locationPattern) {
        if (context == null || locationPattern == null) {
            return new Resource[0];
        }
        try {
            return context.getResources(locationPattern);
        } catch (Exception e) {
            return new Resource[0];
        }
    }

    /**
     * 获取当前请求 Header
     *
     * @param name Header 名称
     * @return Header 值，不存在返回 null
     */
    public static String getRequestHeader(String name) {
        HttpServletRequest request = getHttpServletRequest();
        return request != null ? request.getHeader(name) : null;
    }

    /**
     * 获取当前请求参数
     *
     * @param name 参数名称
     * @return 参数值，不存在返回 null
     */
    public static String getRequestParam(String name) {
        HttpServletRequest request = getHttpServletRequest();
        return request != null ? request.getParameter(name) : null;
    }

    /**
     * 获取当前请求参数，支持默认值
     *
     * @param name         参数名称
     * @param defaultValue 默认值
     * @return 参数值或默认值
     */
    public static String getRequestParam(String name, String defaultValue) {
        String value = getRequestParam(name);
        return value != null ? value : defaultValue;
    }

    /**
     * 获取当前请求参数 Map
     *
     * @return 请求参数 Map，若无请求上下文返回空 Map
     */
    public static Map<String, String[]> getRequestParamMap() {
        HttpServletRequest request = getHttpServletRequest();
        return request != null ? request.getParameterMap() : java.util.Collections.emptyMap();
    }

    /**
     * 获取当前请求属性
     *
     * @param name 属性名称
     * @return 属性值，不存在返回 null
     */
    public static Object getRequestAttribute(String name) {
        HttpServletRequest request = getHttpServletRequest();
        return request != null ? request.getAttribute(name) : null;
    }

    /**
     * 设置当前请求属性
     *
     * @param name  属性名称
     * @param value 属性值
     */
    public static void setRequestAttribute(String name, Object value) {
        HttpServletRequest request = getHttpServletRequest();
        if (request != null) {
            request.setAttribute(name, value);
        }
    }

    /**
     * 移除当前请求属性
     *
     * @param name 属性名称
     */
    public static void removeRequestAttribute(String name) {
        HttpServletRequest request = getHttpServletRequest();
        if (request != null) {
            request.removeAttribute(name);
        }
    }

    /**
     * 获取当前 Session 属性
     *
     * @param name 属性名称
     * @return 属性值，不存在返回 null
     */
    public static Object getSessionAttribute(String name) {
        HttpSession session = getHttpSession();
        return session != null ? session.getAttribute(name) : null;
    }

    /**
     * 设置当前 Session 属性
     *
     * @param name  属性名称
     * @param value 属性值
     */
    public static void setSessionAttribute(String name, Object value) {
        HttpSession session = getHttpSession();
        if (session != null) {
            session.setAttribute(name, value);
        }
    }

    /**
     * 移除当前 Session 属性
     *
     * @param name 属性名称
     */
    public static void removeSessionAttribute(String name) {
        HttpSession session = getHttpSession();
        if (session != null) {
            session.removeAttribute(name);
        }
    }

    /**
     * 获取当前请求 Cookie 值
     *
     * @param name Cookie 名称
     * @return Cookie 值，不存在返回 null
     */
    public static String getCookieValue(String name) {
        HttpServletRequest request = getHttpServletRequest();
        if (request == null || name == null || request.getCookies() == null) {
            return null;
        }

        for (javax.servlet.http.Cookie cookie : request.getCookies()) {
            if (name.equals(cookie.getName())) {
                return cookie.getValue();
            }
        }
        return null;
    }

    /**
     * 获取当前请求 URI
     *
     * @return 请求 URI，若无请求上下文返回 null
     */
    public static String getRequestUri() {
        HttpServletRequest request = getHttpServletRequest();
        return request != null ? request.getRequestURI() : null;
    }

    /**
     * 获取当前请求 URL
     *
     * @return 请求 URL，若无请求上下文返回 null
     */
    public static String getRequestUrl() {
        HttpServletRequest request = getHttpServletRequest();
        return request != null ? request.getRequestURL().toString() : null;
    }

    /**
     * 获取当前请求完整 URL，包含查询参数
     *
     * @return 完整请求 URL，若无请求上下文返回 null
     */
    public static String getFullRequestUrl() {
        HttpServletRequest request = getHttpServletRequest();
        if (request == null) {
            return null;
        }

        String url = request.getRequestURL().toString();
        String queryString = request.getQueryString();
        return queryString == null || queryString.length() == 0 ? url : url + "?" + queryString;
    }

    /**
     * 获取当前请求方法
     *
     * @return 请求方法，若无请求上下文返回 null
     */
    public static String getRequestMethod() {
        HttpServletRequest request = getHttpServletRequest();
        return request != null ? request.getMethod() : null;
    }

    /**
     * 获取当前请求 User-Agent
     *
     * @return User-Agent，若无请求上下文返回 null
     */
    public static String getUserAgent() {
        return getRequestHeader("User-Agent");
    }

    /**
     * 获取当前请求 Referer
     *
     * @return Referer，若无请求上下文返回 null
     */
    public static String getReferer() {
        return getRequestHeader("Referer");
    }

    /**
     * 判断当前请求是否为 Ajax 请求
     *
     * @return Ajax 请求返回 true，否则返回 false
     */
    public static boolean isAjaxRequest() {
        String requestedWith = getRequestHeader("X-Requested-With");
        return "XMLHttpRequest".equalsIgnoreCase(requestedWith);
    }

    /**
     * 获取当前请求基础地址
     *
     * @return 基础地址，例如 http://localhost:8080/context-path，若无请求上下文返回 null
     */
    public static String getBaseUrl() {
        HttpServletRequest request = getHttpServletRequest();
        if (request == null) {
            return null;
        }

        StringBuilder builder = new StringBuilder();
        builder.append(request.getScheme())
                .append("://")
                .append(request.getServerName());

        int port = request.getServerPort();
        boolean defaultHttpPort = "http".equalsIgnoreCase(request.getScheme()) && port == 80;
        boolean defaultHttpsPort = "https".equalsIgnoreCase(request.getScheme()) && port == 443;
        if (!defaultHttpPort && !defaultHttpsPort) {
            builder.append(":").append(port);
        }

        builder.append(request.getContextPath());
        return builder.toString();
    }

    /**
     * 获取 ServletContext 真实路径
     *
     * @param path Web 应用内路径
     * @return 真实路径，无法获取返回 null
     */
    public static String getRealPath(String path) {
        ServletContext servletContext = getServletContext();
        return servletContext != null ? servletContext.getRealPath(path) : null;
    }

    /**
     * 判断当前应用上下文是否可关闭
     *
     * @return 可关闭返回 true，否则返回 false
     */
    public static boolean isCloseableApplicationContext() {
        return context instanceof org.springframework.context.ConfigurableApplicationContext;
    }

    /**
     * 关闭当前 Spring 应用上下文
     *
     * @return 关闭成功返回 true，否则返回 false
     */
    public static boolean closeApplicationContext() {
        if (context instanceof org.springframework.context.ConfigurableApplicationContext) {
            ((org.springframework.context.ConfigurableApplicationContext) context).close();
            context = null;
            publisher = null;
            return true;
        }
        return false;
    }

    /**
     * 获取 Spring Boot 应用所在目录
     *
     * @return 应用所在目录，获取失败返回 null
     */
    public static java.io.File getApplicationHomeDir() {
        try {
            return new org.springframework.boot.system.ApplicationHome().getDir();
        } catch (Exception e) {
            return null;
        }
    }

    /**
     * 获取 Spring Boot 应用所在目录路径
     *
     * @return 应用所在目录绝对路径，获取失败返回 null
     */
    public static String getApplicationHomePath() {
        java.io.File dir = getApplicationHomeDir();
        return dir != null ? dir.getAbsolutePath() : null;
    }
}
