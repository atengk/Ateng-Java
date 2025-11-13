package local.ateng.java.customutils.utils;

import org.springframework.beans.BeansException;
import org.springframework.beans.factory.NoSuchBeanDefinitionException;
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
     * 获取客户端真实 IP 地址（考虑多层代理）
     *
     * @return IP 地址
     */
    public static String getClientIpAddress() {
        HttpServletRequest request = getHttpServletRequest();
        if (request == null) {
            return null;
        }

        String ip = request.getHeader("X-Forwarded-For");
        if (ip != null && !ip.isEmpty() && !UNKNOWN.equalsIgnoreCase(ip)) {
            return ip.split(",")[0];
        }

        ip = request.getHeader("Proxy-Client-IP");
        if (ip != null && !ip.isEmpty() && !UNKNOWN.equalsIgnoreCase(ip)) {
            return ip;
        }

        ip = request.getHeader("WL-Proxy-Client-IP");
        if (ip != null && !ip.isEmpty() && !UNKNOWN.equalsIgnoreCase(ip)) {
            return ip;
        }

        return request.getRemoteAddr();
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

}
