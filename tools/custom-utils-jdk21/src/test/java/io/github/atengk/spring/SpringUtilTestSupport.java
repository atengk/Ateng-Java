package io.github.atengk.spring;

import io.github.atengk.utils.SpringUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.config.ConfigurableBeanFactory;
import org.springframework.boot.info.BuildProperties;
import org.springframework.cache.CacheManager;
import org.springframework.cache.concurrent.ConcurrentMapCacheManager;
import org.springframework.context.MessageSource;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.context.annotation.Scope;
import org.springframework.context.event.EventListener;
import org.springframework.context.support.StaticMessageSource;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.core.env.MapPropertySource;
import org.springframework.core.task.SyncTaskExecutor;
import org.springframework.scheduling.TaskScheduler;
import org.springframework.scheduling.concurrent.ThreadPoolTaskScheduler;
import org.springframework.stereotype.Component;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.support.AbstractPlatformTransactionManager;
import org.springframework.transaction.support.DefaultTransactionStatus;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.web.context.request.RequestContextHolder;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.util.Locale;
import java.util.Map;
import java.util.Properties;
import java.util.concurrent.atomic.AtomicInteger;

final class SpringUtilTestSupport {

    private SpringUtilTestSupport() {
    }

    static AnnotationConfigApplicationContext createContext() {
        AnnotationConfigApplicationContext context = new AnnotationConfigApplicationContext();
        context.getEnvironment().setActiveProfiles("dev", "test");
        context.getEnvironment().getPropertySources().addFirst(new MapPropertySource("testProps", Map.ofEntries(
                Map.entry("spring.application.name", "spring-util-test"),
                Map.entry("spring.application.id", "spring-util-id"),
                Map.entry("server.port", "8080"),
                Map.entry("server.servlet.context-path", "/demo"),
                Map.entry("application.version", "1.0.0"),
                Map.entry("sample.int", "12"),
                Map.entry("sample.long", "99"),
                Map.entry("sample.bool", "true"),
                Map.entry("sample.list", "a,b, ,c"),
                Map.entry("invalid.int", "abc"),
                Map.entry("demo.name", "demo-name"),
                Map.entry("demo.port", "9000"),
                Map.entry("demo.list[0]", "x"),
                Map.entry("demo.list[1]", "y"),
                Map.entry("demo.map.a", "1"),
                Map.entry("demo.map.b", "2")
        )));
        context.register(TestConfig.class);
        context.refresh();
        SpringUtil.setApplicationContext(context);
        return context;
    }

    static void clear(AnnotationConfigApplicationContext context) {
        RequestContextHolder.resetRequestAttributes();
        if (context != null) {
            context.close();
        }
        SpringUtil.setApplicationContext(null);
    }

    @Configuration
    static class TestConfig {
        @Bean
        TestService testService() {
            return new TestService("main");
        }

        @Bean("namedText")
        String namedText() {
            return "hello";
        }

        @Bean
        @Scope(ConfigurableBeanFactory.SCOPE_PROTOTYPE)
        PrototypeBean prototypeBean() {
            return new PrototypeBean();
        }

        @Bean
        NeedInject needInject() {
            return new NeedInject();
        }

        @Bean
        MessageSource messageSource() {
            StaticMessageSource source = new StaticMessageSource();
            source.addMessage("hello", Locale.CHINA, "你好 {0}");
            source.addMessage("simple", Locale.CHINA, "简单消息");
            return source;
        }

        @Bean
        AtomicInteger eventCounter() {
            return new AtomicInteger();
        }

        @Bean
        EventListenerBean eventListenerBean(AtomicInteger eventCounter) {
            return new EventListenerBean(eventCounter);
        }

        @Bean
        CacheManager cacheManager() {
            return new ConcurrentMapCacheManager("users");
        }

        @Bean
        TransactionTemplate transactionTemplate() {
            return new TransactionTemplate(new SimpleTransactionManager());
        }

        @Bean
        SyncTaskExecutor taskExecutor() {
            return new SyncTaskExecutor();
        }

        @Bean
        TaskScheduler taskScheduler() {
            ThreadPoolTaskScheduler scheduler = new ThreadPoolTaskScheduler();
            scheduler.setPoolSize(1);
            scheduler.setThreadNamePrefix("spring-util-test-");
            scheduler.initialize();
            return scheduler;
        }

        @Bean
        BuildProperties buildProperties() {
            Properties properties = new Properties();
            properties.setProperty("version", "2.0.0-build");
            properties.setProperty("name", "spring-util");
            return new BuildProperties(properties);
        }

        @Bean
        @Primary
        Strategy primaryStrategy() {
            return new PrimaryStrategy();
        }

        @Bean
        Strategy highStrategy() {
            return new HighStrategy();
        }

        @Bean
        Strategy lowStrategy() {
            return new LowStrategy();
        }

        @Bean
        MarkedBean markedBean() {
            return new MarkedBean();
        }
    }

    static class TestService {
        private final String name;

        TestService(String name) {
            this.name = name;
        }

        String getName() {
            return name;
        }
    }

    static class PrototypeBean {
    }

    static class NeedInject {
        @Autowired
        TestService testService;
    }

    static class EventListenerBean {
        private final AtomicInteger counter;

        EventListenerBean(AtomicInteger counter) {
            this.counter = counter;
        }

        @EventListener
        void handle(DemoEvent event) {
            counter.incrementAndGet();
        }
    }

    record DemoEvent(String name) {
    }

    interface Strategy {
        String name();
    }

    static class PrimaryStrategy implements Strategy {
        @Override
        public String name() {
            return "primary";
        }
    }

    @Order(Ordered.HIGHEST_PRECEDENCE)
    static class HighStrategy implements Strategy {
        @Override
        public String name() {
            return "high";
        }
    }

    @Order(Ordered.LOWEST_PRECEDENCE)
    static class LowStrategy implements Strategy {
        @Override
        public String name() {
            return "low";
        }
    }

    @Component
    @DemoMarker
    static class MarkedBean {
    }

    @Retention(RetentionPolicy.RUNTIME)
    @interface DemoMarker {
    }

    static class SimpleTransactionManager extends AbstractPlatformTransactionManager {
        @Override
        protected Object doGetTransaction() {
            return new Object();
        }

        @Override
        protected void doBegin(Object transaction, TransactionDefinition definition) {
        }

        @Override
        protected void doCommit(DefaultTransactionStatus status) {
        }

        @Override
        protected void doRollback(DefaultTransactionStatus status) {
        }
    }

    static class DemoProperties {
        private String name;
        private int port;

        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }

        public int getPort() {
            return port;
        }

        public void setPort(int port) {
            this.port = port;
        }
    }
}
