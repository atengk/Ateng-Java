package io.github.atengk.controller;

import io.github.atengk.utils.SpringUtil;
import jakarta.servlet.ServletContext;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.beans.factory.config.ConfigurableBeanFactory;
import org.springframework.beans.factory.support.BeanDefinitionBuilder;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Profile;
import org.springframework.context.event.EventListener;
import org.springframework.context.support.DefaultMessageSourceResolvable;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.core.env.Environment;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.*;
import java.util.function.Supplier;

/**
 * SpringUtil 工具类使用示例接口。
 *
 * @author Ateng
 * @since 2026-04-27
 */
@Profile({"local", "dev", "test"})
@RestController("springUtilDemoController")
@RequestMapping("/demo/spring-util")
public class SpringUtilDemoController {

    private static final Logger log = LoggerFactory.getLogger(SpringUtilDemoController.class);

    private static final String CONTROLLER_BEAN_NAME = "springUtilDemoController";

    private static final String RUNTIME_ARG_BEAN_NAME = "springUtilRuntimeArgBean";

    /**
     * 演示 Spring 上下文相关方法。
     *
     * @return 上下文信息
     */
    @GetMapping("/context")
    public Map<String, Object> context() {
        Map<String, Object> data = new LinkedHashMap<>();

        ApplicationContext context = SpringUtil.getApplicationContext();

        data.put("getApplicationContext", context.getClass().getName());
        data.put("getBeanFactory", SpringUtil.getBeanFactory().getClass().getName());
        data.put("isInitialized", SpringUtil.isInitialized());
        data.put("isActive", SpringUtil.isActive());
        data.put("getApplicationContextId", SpringUtil.getApplicationContextId());
        data.put("getDisplayName", SpringUtil.getDisplayName());
        data.put("getStartupDate", SpringUtil.getStartupDate());
        data.put("getParentContext", SpringUtil.getParentContext()
                .map(parent -> parent.getClass().getName())
                .orElse("无父级上下文"));

        return data;
    }

    /**
     * 演示 Bean 获取相关方法。
     *
     * @return Bean 获取结果
     */
    @GetMapping("/beans/basic")
    public Map<String, Object> beansBasic() {
        ensureRuntimeArgBeanDefinition();

        Map<String, Object> data = new LinkedHashMap<>();

        data.put("getBean(Class)", SpringUtil.getBean(SpringUtilDemoController.class).getClass().getName());

        RuntimeArgBean classArgBean = SpringUtil.getBean(RuntimeArgBean.class, "from-getBean-Class-args");
        data.put("getBean(Class,Object...)", classArgBean.toMap());

        data.put("getBean(String)", SpringUtil.getBean(CONTROLLER_BEAN_NAME).getClass().getName());

        RuntimeArgBean nameArgBean = (RuntimeArgBean) SpringUtil.getBean(RUNTIME_ARG_BEAN_NAME, "from-getBean-String-args");
        data.put("getBean(String,Object...)", nameArgBean.toMap());

        data.put("getBean(String,Class)", SpringUtil.getBean(CONTROLLER_BEAN_NAME, SpringUtilDemoController.class).getClass().getName());

        data.put("getBeanIfPresent(Class)", SpringUtil.getBeanIfPresent(SpringUtilDemoController.class)
                .map(bean -> bean.getClass().getName())
                .orElse("不存在"));

        data.put("getBeanIfPresent(String)", SpringUtil.getBeanIfPresent(CONTROLLER_BEAN_NAME)
                .map(bean -> bean.getClass().getName())
                .orElse("不存在"));

        ObjectProvider<RuntimeArgBean> provider = SpringUtil.getBeanProvider(RuntimeArgBean.class);
        data.put("getBeanProvider", provider.getObject("from-provider").toMap());

        data.put("getBeansOfType", SpringUtil.getBeansOfType(SpringUtilDemoController.class).keySet());
        data.put("getBeansWithAnnotation", SpringUtil.getBeansWithAnnotation(RestController.class).keySet());
        data.put("getBeanNamesForType", Arrays.asList(SpringUtil.getBeanNamesForType(SpringUtilDemoController.class)));
        data.put("getBeanNamesForAnnotation", Arrays.asList(SpringUtil.getBeanNamesForAnnotation(RestController.class)));

        return data;
    }

    /**
     * 演示 Bean 元数据、定义、别名、作用域相关方法。
     *
     * @return Bean 元数据
     */
    @GetMapping("/beans/metadata")
    public Map<String, Object> beansMetadata() {
        Map<String, Object> data = new LinkedHashMap<>();

        data.put("getBeanDefinitionNames.size", SpringUtil.getBeanDefinitionNames().length);
        data.put("getBeanDefinitionCount", SpringUtil.getBeanDefinitionCount());

        data.put("containsBean(String)", SpringUtil.containsBean(CONTROLLER_BEAN_NAME));
        data.put("containsLocalBean", SpringUtil.containsLocalBean(CONTROLLER_BEAN_NAME));
        data.put("containsBean(Class)", SpringUtil.containsBean(SpringUtilDemoController.class));
        data.put("containsBeanDefinition", SpringUtil.containsBeanDefinition(CONTROLLER_BEAN_NAME));
        data.put("containsSingleton", SpringUtil.containsSingleton(CONTROLLER_BEAN_NAME));

        data.put("isSingleton", SpringUtil.isSingleton(CONTROLLER_BEAN_NAME));
        data.put("isPrototype", SpringUtil.isPrototype(RUNTIME_ARG_BEAN_NAME));
        data.put("isTypeMatch", SpringUtil.isTypeMatch(CONTROLLER_BEAN_NAME, SpringUtilDemoController.class));

        Class<?> type = SpringUtil.getType(CONTROLLER_BEAN_NAME);
        data.put("getType", type == null ? null : type.getName());

        data.put("getAliases", Arrays.asList(SpringUtil.getAliases(CONTROLLER_BEAN_NAME)));

        BeanDefinition beanDefinition = SpringUtil.getBeanDefinition(CONTROLLER_BEAN_NAME);
        data.put("getBeanDefinition.beanClassName", beanDefinition.getBeanClassName());
        data.put("getBeanDefinition.scope", beanDefinition.getScope());
        data.put("getBeanDefinition.lazyInit", beanDefinition.isLazyInit());

        data.put("findAnnotationOnBean", SpringUtil.findAnnotationOnBean(CONTROLLER_BEAN_NAME, RestController.class)
                .map(annotation -> annotation.annotationType().getName())
                .orElse("未找到注解"));

        return data;
    }

    /**
     * 演示 Bean 生命周期、自动装配、动态注册和动态移除相关方法。
     *
     * @return Bean 生命周期操作结果
     */
    @PostMapping("/beans/lifecycle")
    public Map<String, Object> beansLifecycle() {
        Map<String, Object> data = new LinkedHashMap<>();
        String suffix = String.valueOf(System.nanoTime());

        DemoInjectTarget injectTarget = new DemoInjectTarget();
        SpringUtil.autowireBean(injectTarget);
        data.put("autowireBean", injectTarget.toMap());

        String configureBeanName = "springUtilConfigureBean_" + suffix;
        try {
            SpringUtil.registerBeanDefinition(configureBeanName, DemoLifecycleBean.class);
            DemoLifecycleBean configureBean = new DemoLifecycleBean("configureBean");
            Object configured = SpringUtil.configureBean(configureBean, configureBeanName);
            data.put("configureBean", ((DemoLifecycleBean) configured).toMap());
            SpringUtil.destroyBean(configured);
            data.put("destroyBean.afterConfigureBean", ((DemoLifecycleBean) configured).toMap());
        } finally {
            cleanupDynamicBean(configureBeanName);
        }

        DemoLifecycleBean initializeBean = new DemoLifecycleBean("initializeBean");
        Object initialized = SpringUtil.initializeBean(initializeBean, "manualInitializeBean");
        data.put("initializeBean", ((DemoLifecycleBean) initialized).toMap());

        SpringUtil.destroyBean(initialized);
        data.put("destroyBean.afterInitializeBean", ((DemoLifecycleBean) initialized).toMap());

        String singletonName = "springUtilSingleton_" + suffix;
        SpringUtil.registerSingleton(singletonName, new DemoRuntimeObject(singletonName, "registerSingleton"));
        data.put("registerSingleton", SpringUtil.getBean(singletonName).getClass().getName());
        data.put("containsSingleton.afterRegisterSingleton", SpringUtil.containsSingleton(singletonName));
        SpringUtil.destroySingleton(singletonName);
        data.put("destroySingleton", !SpringUtil.containsSingleton(singletonName));

        String classDefinitionName = "springUtilClassDefinition_" + suffix;
        try {
            SpringUtil.registerBeanDefinition(classDefinitionName, DemoRegisteredBean.class);
            DemoRegisteredBean bean = SpringUtil.getBean(classDefinitionName, DemoRegisteredBean.class);
            data.put("registerBeanDefinition(String,Class)", bean.toMap());
        } finally {
            cleanupDynamicBean(classDefinitionName);
        }

        String definitionName = "springUtilBeanDefinition_" + suffix;
        try {
            BeanDefinition beanDefinition = BeanDefinitionBuilder
                    .genericBeanDefinition(DemoRegisteredBean.class)
                    .addPropertyValue("source", "registerBeanDefinition(String,BeanDefinition)")
                    .getBeanDefinition();

            SpringUtil.registerBeanDefinition(definitionName, beanDefinition);
            DemoRegisteredBean bean = SpringUtil.getBean(definitionName, DemoRegisteredBean.class);
            data.put("registerBeanDefinition(String,BeanDefinition)", bean.toMap());
        } finally {
            cleanupDynamicBean(definitionName);
        }

        String supplierDefinitionName = "springUtilSupplierDefinition_" + suffix;
        try {
            SpringUtil.registerBeanDefinition(
                    supplierDefinitionName,
                    DemoRegisteredBean.class,
                    () -> new DemoRegisteredBean("registerBeanDefinition(String,Class,Supplier)")
            );
            DemoRegisteredBean bean = SpringUtil.getBean(supplierDefinitionName, DemoRegisteredBean.class);
            data.put("registerBeanDefinition(String,Class,Supplier)", bean.toMap());
        } finally {
            cleanupDynamicBean(supplierDefinitionName);
        }

        String removeDefinitionName = "springUtilRemoveDefinition_" + suffix;
        SpringUtil.registerBeanDefinition(removeDefinitionName, DemoRegisteredBean.class);
        data.put("containsBeanDefinition.beforeRemove", SpringUtil.containsBeanDefinition(removeDefinitionName));
        SpringUtil.removeBeanDefinition(removeDefinitionName);
        data.put("removeBeanDefinition", !SpringUtil.containsBeanDefinition(removeDefinitionName));

        return data;
    }

    /**
     * 演示环境变量、配置属性、类型转换、Profile、占位符解析相关方法。
     *
     * @return 环境配置信息
     */
    @GetMapping("/environment")
    public Map<String, Object> environment() {
        Map<String, Object> data = new LinkedHashMap<>();

        Environment environment = SpringUtil.getEnvironment();
        ConfigurableEnvironment configurableEnvironment = SpringUtil.getConfigurableEnvironment();

        data.put("getEnvironment", environment.getClass().getName());
        data.put("getConfigurableEnvironment", configurableEnvironment.getClass().getName());
        data.put("getConversionService", SpringUtil.getConversionService().getClass().getName());

        data.put("convert", SpringUtil.convert("123", Integer.class));

        data.put("getProperty(String)", SpringUtil.getProperty("spring.application.name"));
        data.put("getProperty(String,String)", SpringUtil.getProperty("spring.application.name", "unknown-app"));
        data.put("getProperty(String,Class)", SpringUtil.getProperty("server.port", Integer.class));
        data.put("getProperty(String,Class,T)", SpringUtil.getProperty("server.port", Integer.class, 8080));

        data.put("getRequiredProperty(String)", safe(() -> SpringUtil.getRequiredProperty("java.version")));
        data.put("getRequiredProperty(String,Class)", safe(() -> SpringUtil.getRequiredProperty("java.version", String.class)));

        data.put("containsProperty", SpringUtil.containsProperty("java.version"));

        data.put("resolvePlaceholders", SpringUtil.resolvePlaceholders("应用名称：${spring.application.name:unknown-app}"));
        data.put("resolveRequiredPlaceholders", SpringUtil.resolveRequiredPlaceholders("Java版本：${java.version}"));
        data.put("resolveEmbeddedValue", SpringUtil.resolveEmbeddedValue("当前应用：${spring.application.name:unknown-app}"));

        data.put("getActiveProfiles", Arrays.asList(SpringUtil.getActiveProfiles()));
        data.put("getDefaultProfiles", Arrays.asList(SpringUtil.getDefaultProfiles()));
        data.put("matchesProfile", SpringUtil.matchesProfile("local | dev | test"));
        data.put("matchesAnyProfile", SpringUtil.matchesAnyProfile("local", "dev", "test"));
        data.put("matchesAllProfiles", SpringUtil.matchesAllProfiles("!prod"));

        data.put("getApplicationName", SpringUtil.getApplicationName());
        data.put("getServerPort", SpringUtil.getServerPort());
        data.put("getLocalServerPort", SpringUtil.getLocalServerPort());

        return data;
    }

    /**
     * 演示 Spring 事件发布。
     *
     * @param message 事件消息
     * @return 发布结果
     */
    @PostMapping("/events")
    public Map<String, Object> events(@RequestParam(defaultValue = "SpringUtil 事件演示") String message) {
        Map<String, Object> data = new LinkedHashMap<>();

        SpringUtilDemoEvent event = new SpringUtilDemoEvent(message, Instant.now());
        SpringUtil.publishEvent(event);

        data.put("publishEvent", "已发布");
        data.put("event", event.toMap());

        return data;
    }

    /**
     * 监听 SpringUtil 演示事件。
     *
     * @param event 演示事件
     */
    @EventListener
    public void onSpringUtilDemoEvent(SpringUtilDemoEvent event) {
        log.info("收到 SpringUtil 演示事件：message={}，time={}", event.message(), event.time());
    }

    /**
     * 演示国际化消息相关方法。
     *
     * @param code 消息编码
     * @return 国际化消息
     */
    @GetMapping("/messages")
    public Map<String, Object> messages(@RequestParam(defaultValue = "springUtil.demo.message") String code) {
        Map<String, Object> data = new LinkedHashMap<>();

        data.put("getMessage(String)", safe(() -> SpringUtil.getMessage(code)));
        data.put("getMessage(String,Object[])", safe(() -> SpringUtil.getMessage(code, new Object[]{"Ateng"})));
        data.put("getMessage(String,Object[],String)", SpringUtil.getMessage(code, new Object[]{"Ateng"}, "默认消息：{0}"));
        data.put("getMessage(String,Object[],String,Locale)", SpringUtil.getMessage(code, new Object[]{"Ateng"}, "默认中文消息：{0}", Locale.SIMPLIFIED_CHINESE));

        DefaultMessageSourceResolvable resolvable = new DefaultMessageSourceResolvable(
                new String[]{code},
                new Object[]{"Ateng"},
                "Resolvable 默认消息：{0}"
        );
        data.put("getMessage(MessageSourceResolvable)", SpringUtil.getMessage(resolvable));

        return data;
    }

    /**
     * 演示资源读取相关方法。
     *
     * @param location 资源路径
     * @return 资源读取结果
     */
    @GetMapping("/resources")
    public Map<String, Object> resources(@RequestParam(defaultValue = "classpath:application.yml") String location) {
        Map<String, Object> data = new LinkedHashMap<>();

        Resource resource = SpringUtil.getResource(location);
        data.put("getResource.exists", resource.exists());
        data.put("getResource.filename", resource.getFilename());
        data.put("getResource.description", resource.getDescription());

        Resource[] resources = SpringUtil.getResources("classpath*:META-INF/spring/*.imports");
        data.put("getResources.count", resources.length);
        data.put("getResources.first", resources.length > 0 ? resources[0].getDescription() : null);

        data.put("getResourceAsStream", safe(() -> {
            try (InputStream inputStream = SpringUtil.getResourceAsStream(location)) {
                return "读取成功，available=" + inputStream.available();
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }));

        data.put("readResourceBytes", safe(() -> SpringUtil.readResourceBytes(location).length));

        data.put("readResourceText", safe(() -> {
            String text = SpringUtil.readResourceText(location);
            return text.length() > 300 ? text.substring(0, 300) : text;
        }));

        data.put("readResourceText(location,charset)", safe(() -> {
            String text = SpringUtil.readResourceText(location, StandardCharsets.UTF_8);
            return text.length() > 300 ? text.substring(0, 300) : text;
        }));

        return data;
    }

    /**
     * 演示当前 Web 请求上下文相关方法。
     *
     * @return Web 请求上下文信息
     */
    @GetMapping("/web")
    public Map<String, Object> web() {
        Map<String, Object> data = new LinkedHashMap<>();

        data.put("getCurrentRequestAttributes", SpringUtil.getCurrentRequestAttributes()
                .map(attributes -> attributes.getClass().getName())
                .orElse("不存在"));

        data.put("getCurrentRequest", SpringUtil.getCurrentRequest()
                .map(HttpServletRequest::getRequestURI)
                .orElse("不存在"));

        data.put("getCurrentResponse", SpringUtil.getCurrentResponse()
                .map(HttpServletResponse::getStatus)
                .orElse(null));

        HttpServletRequest requiredRequest = SpringUtil.getRequiredCurrentRequest();
        HttpServletResponse requiredResponse = SpringUtil.getRequiredCurrentResponse();

        data.put("getRequiredCurrentRequest", requiredRequest.getRequestURI());
        data.put("getRequiredCurrentResponse", requiredResponse.getStatus());

        Optional<ServletContext> servletContext = SpringUtil.getServletContext();
        data.put("getServletContext", servletContext.map(ServletContext::getContextPath).orElse("不存在"));

        data.put("getCurrentRequestUri", SpringUtil.getCurrentRequestUri().orElse(""));
        data.put("getCurrentRequestUrl", SpringUtil.getCurrentRequestUrl().orElse(""));
        data.put("getCurrentRequestMethod", SpringUtil.getCurrentRequestMethod().orElse(""));
        data.put("getCurrentClientIp", SpringUtil.getCurrentClientIp().orElse(""));

        return data;
    }

    /**
     * 演示关闭 Spring 上下文。该接口风险较高，只允许明确传入 confirm=close 时执行。
     *
     * @param confirm 确认参数
     * @return 操作结果
     */
    @PostMapping("/context/close")
    public ResponseEntity<Map<String, Object>> closeApplicationContext(@RequestParam(required = false) String confirm) {
        Map<String, Object> data = new LinkedHashMap<>();

        if (!"close".equals(confirm)) {
            data.put("closeApplicationContext", "未执行");
            data.put("reason", "该操作会关闭 Spring 上下文，如需执行请传入 confirm=close");
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(data);
        }

        data.put("closeApplicationContext", "即将关闭 Spring 上下文");
        log.warn("收到关闭 Spring 应用上下文请求");
        SpringUtil.closeApplicationContext();

        return ResponseEntity.ok(data);
    }

    /**
     * 清理动态注册的 Bean。
     *
     * @param beanName Bean 名称
     */
    private void cleanupDynamicBean(String beanName) {
        if (SpringUtil.containsSingleton(beanName)) {
            SpringUtil.destroySingleton(beanName);
        }
        if (SpringUtil.containsBeanDefinition(beanName)) {
            SpringUtil.removeBeanDefinition(beanName);
        }
    }

    /**
     * 安全执行演示逻辑，避免部分演示方法异常导致整个接口失败。
     *
     * @param supplier 数据提供器
     * @return 执行结果或异常信息
     */
    private Object safe(Supplier<?> supplier) {
        try {
            return supplier.get();
        } catch (Exception ex) {
            log.warn("SpringUtil 演示方法执行失败：{}", ex.getMessage());
            return ex.getClass().getSimpleName() + "：" + ex.getMessage();
        }
    }

    /**
     * 支持运行时参数创建的演示 Bean。
     *
     * @author Ateng
     * @since 2026-04-27
     */
    public static class RuntimeArgBean {

        private final String value;

        public RuntimeArgBean(String value) {
            this.value = value;
        }

        public Map<String, Object> toMap() {
            Map<String, Object> data = new LinkedHashMap<>();
            data.put("className", getClass().getName());
            data.put("value", value);
            return data;
        }
    }

    /**
     * 动态注册 BeanDefinition 的演示 Bean。
     *
     * @author Ateng
     * @since 2026-04-27
     */
    public static class DemoRegisteredBean {

        private String source = "default";

        public DemoRegisteredBean() {
        }

        public DemoRegisteredBean(String source) {
            this.source = source;
        }

        public String getSource() {
            return source;
        }

        public void setSource(String source) {
            this.source = source;
        }

        public Map<String, Object> toMap() {
            Map<String, Object> data = new LinkedHashMap<>();
            data.put("className", getClass().getName());
            data.put("source", source);
            return data;
        }
    }

    /**
     * 动态注册单例对象的演示对象。
     *
     * @author Ateng
     * @since 2026-04-27
     */
    public static class DemoRuntimeObject {

        private final String name;

        private final String source;

        public DemoRuntimeObject(String name, String source) {
            this.name = name;
            this.source = source;
        }

        public String getName() {
            return name;
        }

        public String getSource() {
            return source;
        }
    }

    /**
     * 自动注入演示对象。
     *
     * @author Ateng
     * @since 2026-04-27
     */
    @Profile({"local", "dev", "test"})
    @Component
    public static class DemoInjectTarget {

        @Autowired
        private Environment environment;

        public Map<String, Object> toMap() {
            Map<String, Object> data = new LinkedHashMap<>();
            data.put("environmentInjected", environment != null);
            data.put("activeProfiles", environment == null ? null : Arrays.asList(environment.getActiveProfiles()));
            return data;
        }
    }

    /**
     * Bean 初始化和销毁流程演示对象。
     *
     * @author Ateng
     * @since 2026-04-27
     */
    public static class DemoLifecycleBean implements InitializingBean, DisposableBean {

        private String name = "default";

        private boolean initialized;

        private boolean destroyed;

        public DemoLifecycleBean() {
        }

        public DemoLifecycleBean(String name) {
            this.name = name;
        }

        @Override
        public void afterPropertiesSet() {
            this.initialized = true;
        }

        @Override
        public void destroy() {
            this.destroyed = true;
        }

        public Map<String, Object> toMap() {
            Map<String, Object> data = new LinkedHashMap<>();
            data.put("name", name);
            data.put("initialized", initialized);
            data.put("destroyed", destroyed);
            return data;
        }
    }

    /**
     * SpringUtil 事件演示对象。
     *
     * @author Ateng
     * @since 2026-04-27
     */
    public record SpringUtilDemoEvent(String message, Instant time) {

        public Map<String, Object> toMap() {
            Map<String, Object> data = new LinkedHashMap<>();
            data.put("message", message);
            data.put("time", time);
            return data;
        }
    }

    /**
     * 确保运行时参数 BeanDefinition 已注册。
     */
    private void ensureRuntimeArgBeanDefinition() {
        if (SpringUtil.containsBeanDefinition(RUNTIME_ARG_BEAN_NAME)) {
            return;
        }

        synchronized (SpringUtilDemoController.class) {
            if (SpringUtil.containsBeanDefinition(RUNTIME_ARG_BEAN_NAME)) {
                return;
            }

            BeanDefinitionBuilder builder = BeanDefinitionBuilder.genericBeanDefinition(RuntimeArgBean.class);
            builder.setScope(ConfigurableBeanFactory.SCOPE_PROTOTYPE);

            SpringUtil.registerBeanDefinition(RUNTIME_ARG_BEAN_NAME, builder.getBeanDefinition());
            log.info("注册运行时参数演示 BeanDefinition：{}", RUNTIME_ARG_BEAN_NAME);
        }
    }
}