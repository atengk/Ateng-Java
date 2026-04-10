# ThreadLocal 

ThreadLocal 是 JDK 8 中用于实现线程隔离变量的工具类。它为每个线程提供独立的变量副本，不同线程之间互不干扰，从而避免共享变量带来的线程安全问题。底层通过 Thread 持有的 ThreadLocalMap 实现键值存储，键为 ThreadLocal 实例，值为线程私有数据。常用于保存用户会话、数据库连接等线程上下文信息。但需注意及时调用 remove()，防止内存泄漏。

------

## 用户上下文存储（登录用户信息缓存）

用户登录后，将用户信息存入 ThreadLocal，在整个请求链路中随时获取，避免重复传参

```java
import cn.hutool.core.util.ObjectUtil;

/**
 * 用户上下文工具类（基于 ThreadLocal 实现）
 *
 * @author Ateng
 * @since 2026-04-10
 */
public class UserContextHolder {

    /**
     * 使用 ThreadLocal 存储当前线程的用户信息
     */
    private static final ThreadLocal<User> USER_THREAD_LOCAL = new ThreadLocal<>();

    /**
     * 设置当前登录用户
     *
     * @param user 用户信息
     */
    public static void set(User user) {
        if (ObjectUtil.isNotNull(user)) {
            USER_THREAD_LOCAL.set(user);
        }
    }

    /**
     * 获取当前登录用户
     *
     * @return 用户信息
     */
    public static User get() {
        return USER_THREAD_LOCAL.get();
    }

    /**
     * 获取当前用户ID（快捷方法）
     *
     * @return 用户ID
     */
    public static Long getUserId() {
        User user = get();
        return ObjectUtil.isNotNull(user) ? user.getId() : null;
    }

    /**
     * 清除当前线程中的用户信息（必须调用，防止内存泄漏）
     */
    public static void clear() {
        USER_THREAD_LOCAL.remove();
    }
}
/**
 * 用户实体类（示例）
 *
 * @author Ateng
 * @since 2026-04-10
 */
public class User {

    private Long id;
    private String username;

    public User() {
    }

    public User(Long id, String username) {
        this.id = id;
        this.username = username;
    }

    public Long getId() {
        return id;
    }

    public String getUsername() {
        return username;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public void setUsername(String username) {
        this.username = username;
    }
}
```

在 Spring Boot 中通过拦截器统一设置和清除用户上下文

```java
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

/**
 * 用户上下文拦截器
 *
 * @author Ateng
 * @since 2026-04-10
 */
@Component
public class UserContextInterceptor implements HandlerInterceptor {

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {

        // 模拟从请求中解析用户信息（如 JWT / Token）
        Long userId = 1001L;
        String username = "testUser";

        User user = new User(userId, username);

        // 放入 ThreadLocal
        UserContextHolder.set(user);

        return true;
    }

    @Override
    public void afterCompletion(HttpServletRequest request, HttpServletResponse response, Object handler, Exception ex) {

        // 请求结束必须清理
        UserContextHolder.clear();
    }
}
```

注册拦截器

```java
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * Web 配置类
 *
 * @author Ateng
 * @since 2026-04-10
 */
@Configuration
public class WebConfig implements WebMvcConfigurer {

    @Autowired
    private UserContextInterceptor userContextInterceptor;

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(userContextInterceptor).addPathPatterns("/**");
    }
}
```

业务代码中直接获取用户信息，无需传参

```java
/**
 * 示例业务代码
 *
 * @author Ateng
 * @since 2026-04-10
 */
@Service
public class OrderService {

    public void createOrder() {
        Long userId = UserContextHolder.getUserId();
        System.out.println("当前用户ID：" + userId);
    }
}
```

用户上下文测试控制器

```java
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 用户上下文测试控制器
 *
 * @author Ateng
 * @since 2026-04-10
 */
@RestController
public class UserController {

    private final OrderService orderService;

    public UserController(OrderService orderService) {
        this.orderService = orderService;
    }

    /**
     * 测试接口：创建订单并打印当前用户ID
     *
     * 访问：http://localhost:8080/test/user
     *
     * @return 返回当前用户ID
     */
    @GetMapping("/test/user")
    public String testUser() {
        orderService.createOrder();
        return "当前用户ID：" + UserContextHolder.getUserId();
    }
}
```



## 请求级 TraceId（链路追踪）传递

为每个请求生成唯一 TraceId，并通过 ThreadLocal 在整个调用链路中传递，用于日志追踪与问题排查

```java
import cn.hutool.core.util.StrUtil;
import cn.hutool.core.util.IdUtil;

/**
 * TraceId 上下文工具类（基于 ThreadLocal 实现）
 *
 * @author Ateng
 * @since 2026-04-10
 */
public class TraceIdContextHolder {

    /**
     * 存储 TraceId
     */
    private static final ThreadLocal<String> TRACE_ID_THREAD_LOCAL = new ThreadLocal<>();

    /**
     * 设置 TraceId
     *
     * @param traceId 链路ID
     */
    public static void set(String traceId) {
        if (StrUtil.isNotBlank(traceId)) {
            TRACE_ID_THREAD_LOCAL.set(traceId);
        }
    }

    /**
     * 获取 TraceId
     *
     * @return TraceId
     */
    public static String get() {
        return TRACE_ID_THREAD_LOCAL.get();
    }

    /**
     * 获取或生成 TraceId（推荐方法）
     *
     * @return TraceId
     */
    public static String getOrCreate() {
        String traceId = get();
        if (StrUtil.isBlank(traceId)) {
            traceId = IdUtil.fastSimpleUUID();
            set(traceId);
        }
        return traceId;
    }

    /**
     * 清理（必须调用）
     */
    public static void clear() {
        TRACE_ID_THREAD_LOCAL.remove();
    }
}
```

通过拦截器统一生成或透传 TraceId（支持从请求头获取）

```java
import cn.hutool.core.util.StrUtil;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

/**
 * TraceId 拦截器
 *
 * @author Ateng
 * @since 2026-04-10
 */
@Component
public class TraceIdInterceptor implements HandlerInterceptor {

    private static final String HEADER_TRACE_ID = "X-Trace-Id";

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {

        // 1. 优先从请求头获取（网关透传）
        String traceId = request.getHeader(HEADER_TRACE_ID);

        // 2. 如果没有则生成
        if (StrUtil.isBlank(traceId)) {
            traceId = TraceIdContextHolder.getOrCreate();
        } else {
            TraceIdContextHolder.set(traceId);
        }

        // 3. 回写响应头（方便前端/调用方获取）
        response.setHeader(HEADER_TRACE_ID, traceId);

        return true;
    }

    @Override
    public void afterCompletion(HttpServletRequest request, HttpServletResponse response, Object handler, Exception ex) {
        TraceIdContextHolder.clear();
    }
}
```

注册拦截器

```java
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * Web 配置类（注册 TraceId 拦截器）
 *
 * @author Ateng
 * @since 2026-04-10
 */
@Configuration
public class TraceWebConfig implements WebMvcConfigurer {

    @Autowired
    private TraceIdInterceptor traceIdInterceptor;

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(traceIdInterceptor).addPathPatterns("/**");
    }
}
```

业务中直接使用 TraceId（用于日志打印）

```java
/**
 * 示例业务类（打印 TraceId）
 *
 * @author Ateng
 * @since 2026-04-10
 */
@Service
public class TraceService {

    public String process() {
        String traceId = TraceIdContextHolder.get();
        System.out.println("当前 TraceId：" + traceId);
        return traceId;
    }
}
```

提供 Controller 方便测试（可手动传递或自动生成 TraceId）

```java
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * TraceId 测试控制器
 *
 * @author Ateng
 * @since 2026-04-10
 */
@RestController
public class TraceController {

    private final TraceService traceService;

    public TraceController(TraceService traceService) {
        this.traceService = traceService;
    }

    /**
     * 测试接口
     *
     * 1. 不传请求头：自动生成 TraceId
     * 2. 传请求头 X-Trace-Id：使用传入值
     *
     * curl 示例：
     * curl http://localhost:8080/test/trace
     * curl -H "X-Trace-Id: abc123" http://localhost:8080/test/trace
     *
     * @return TraceId
     */
    @GetMapping("/test/trace")
    public String testTrace() {
        return "TraceId：" + traceService.process();
    }
}
```

## 数据库连接上下文（多数据源切换）

通过 ThreadLocal 存储当前数据源标识，在执行数据库操作前动态切换数据源（典型多数据源实现核心）

```java
import cn.hutool.core.util.StrUtil;

/**
 * 数据源上下文工具类（基于 ThreadLocal 实现）
 *
 * @author Ateng
 * @since 2026-04-10
 */
public class DataSourceContextHolder {

    /**
     * 存储当前线程的数据源 key
     */
    private static final ThreadLocal<String> DATASOURCE_THREAD_LOCAL = new ThreadLocal<>();

    /**
     * 默认数据源
     */
    public static final String DEFAULT_DS = "master";

    /**
     * 设置数据源
     *
     * @param ds 数据源标识
     */
    public static void set(String ds) {
        if (StrUtil.isNotBlank(ds)) {
            DATASOURCE_THREAD_LOCAL.set(ds);
        }
    }

    /**
     * 获取当前数据源
     *
     * @return 数据源标识
     */
    public static String get() {
        String ds = DATASOURCE_THREAD_LOCAL.get();
        return StrUtil.isNotBlank(ds) ? ds : DEFAULT_DS;
    }

    /**
     * 清理
     */
    public static void clear() {
        DATASOURCE_THREAD_LOCAL.remove();
    }
}
```

动态数据源路由（核心类，继承 AbstractRoutingDataSource）

```java
import org.springframework.jdbc.datasource.lookup.AbstractRoutingDataSource;

/**
 * 动态数据源路由
 *
 * @author Ateng
 * @since 2026-04-10
 */
public class DynamicDataSource extends AbstractRoutingDataSource {

    /**
     * 决定当前使用哪个数据源
     */
    @Override
    protected Object determineCurrentLookupKey() {
        return DataSourceContextHolder.get();
    }
}
```

自定义注解，用于标记方法使用哪个数据源

```java
import java.lang.annotation.*;

/**
 * 数据源切换注解
 *
 * @author Ateng
 * @since 2026-04-10
 */
@Target({ElementType.METHOD, ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface DS {

    /**
     * 数据源名称
     */
    String value() default "master";
}
```

AOP 切面，在方法执行前后切换数据源

```java
import cn.hutool.core.util.StrUtil;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.*;
import org.springframework.stereotype.Component;

/**
 * 数据源切面
 *
 * @author Ateng
 * @since 2026-04-10
 */
@Aspect
@Component
public class DataSourceAspect {

    @Around("@annotation(ds)")
    public Object around(ProceedingJoinPoint point, DS ds) throws Throwable {

        String dsKey = ds.value();

        try {
            // 设置数据源
            if (StrUtil.isNotBlank(dsKey)) {
                DataSourceContextHolder.set(dsKey);
            }

            return point.proceed();
        } finally {
            // 清理，避免线程复用污染
            DataSourceContextHolder.clear();
        }
    }
}
```

简单模拟 Service（不同数据源）

```java
/**
 * 示例业务类（模拟多数据源）
 *
 * @author Ateng
 * @since 2026-04-10
 */
@Service
public class UserService {

    /**
     * 使用主库
     */
    @DS("master")
    public String getFromMaster() {
        return "当前数据源：" + DataSourceContextHolder.get();
    }

    /**
     * 使用从库
     */
    @DS("slave")
    public String getFromSlave() {
        return "当前数据源：" + DataSourceContextHolder.get();
    }
}
```

提供 Controller 方便测试切换效果

```java
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 多数据源测试控制器
 *
 * @author Ateng
 * @since 2026-04-10
 */
@RestController
public class DataSourceController {

    private final UserService userService;

    public DataSourceController(UserService userService) {
        this.userService = userService;
    }

    /**
     * 测试主库
     *
     * http://localhost:8080/test/ds/master
     */
    @GetMapping("/test/ds/master")
    public String master() {
        return userService.getFromMaster();
    }

    /**
     * 测试从库
     *
     * http://localhost:8080/test/ds/slave
     */
    @GetMapping("/test/ds/slave")
    public String slave() {
        return userService.getFromSlave();
    }
}
```

## 事务上下文绑定（手动事务控制辅助）

通过 ThreadLocal 绑定当前线程的事务资源（Connection），实现手动事务控制（开启 / 提交 / 回滚）

```java
import java.sql.Connection;

/**
 * 事务上下文工具类（基于 ThreadLocal 实现）
 *
 * @author Ateng
 * @since 2026-04-10
 */
public class TransactionContextHolder {

    /**
     * 存储当前线程的数据库连接
     */
    private static final ThreadLocal<Connection> CONNECTION_THREAD_LOCAL = new ThreadLocal<>();

    /**
     * 绑定连接
     *
     * @param connection 数据库连接
     */
    public static void bind(Connection connection) {
        if (connection != null) {
            CONNECTION_THREAD_LOCAL.set(connection);
        }
    }

    /**
     * 获取当前连接
     *
     * @return Connection
     */
    public static Connection get() {
        return CONNECTION_THREAD_LOCAL.get();
    }

    /**
     * 移除连接
     */
    public static void clear() {
        CONNECTION_THREAD_LOCAL.remove();
    }
}
```

手动事务管理工具类（开启 / 提交 / 回滚）

```java
import cn.hutool.core.exceptions.ExceptionUtil;

import javax.sql.DataSource;
import java.sql.Connection;

/**
 * 手动事务管理工具
 *
 * @author Ateng
 * @since 2026-04-10
 */
public class TransactionManager {

    private final DataSource dataSource;

    public TransactionManager(DataSource dataSource) {
        this.dataSource = dataSource;
    }

    /**
     * 开启事务
     */
    public void begin() {
        try {
            Connection connection = dataSource.getConnection();
            connection.setAutoCommit(false);
            TransactionContextHolder.bind(connection);
        } catch (Exception e) {
            throw new RuntimeException("开启事务失败：" + ExceptionUtil.getMessage(e), e);
        }
    }

    /**
     * 提交事务
     */
    public void commit() {
        Connection connection = TransactionContextHolder.get();
        if (connection == null) {
            return;
        }
        try {
            connection.commit();
        } catch (Exception e) {
            throw new RuntimeException("提交事务失败：" + ExceptionUtil.getMessage(e), e);
        } finally {
            close(connection);
        }
    }

    /**
     * 回滚事务
     */
    public void rollback() {
        Connection connection = TransactionContextHolder.get();
        if (connection == null) {
            return;
        }
        try {
            connection.rollback();
        } catch (Exception e) {
            throw new RuntimeException("回滚事务失败：" + ExceptionUtil.getMessage(e), e);
        } finally {
            close(connection);
        }
    }

    /**
     * 关闭连接并清理
     */
    private void close(Connection connection) {
        try {
            connection.close();
        } catch (Exception ignored) {
        }
        TransactionContextHolder.clear();
    }
}
```

业务层使用 ThreadLocal 中的连接（避免重复获取连接）

```java
import java.sql.Connection;

/**
 * 示例业务类（手动事务）
 *
 * @author Ateng
 * @since 2026-04-10
 */
@Service
public class AccountService {

    /**
     * 模拟转账操作
     */
    public void transfer() throws Exception {

        // 从 ThreadLocal 获取连接
        Connection connection = TransactionContextHolder.get();

        if (connection == null) {
            throw new RuntimeException("当前无事务连接");
        }

        // 模拟 SQL 执行
        System.out.println("执行扣款操作...");
        System.out.println("执行加款操作...");

        // 模拟异常（测试回滚）
        if (true) {
            throw new RuntimeException("模拟异常");
        }
    }
}
```

提供 Controller 方便测试事务提交和回滚

```java
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.sql.DataSource;

/**
 * 事务测试控制器
 *
 * @author Ateng
 * @since 2026-04-10
 */
@RestController
public class TransactionController {

    private final DataSource dataSource;

    private final AccountService accountService;

    public TransactionController(DataSource dataSource, AccountService accountService) {
        this.dataSource = dataSource;
        this.accountService = accountService;
    }
    
    /**
     * 测试事务提交
     *
     * http://localhost:8080/test/tx/commit
     */
    @GetMapping("/test/tx/commit")
    public String commit() {

        TransactionManager txManager = new TransactionManager(dataSource);

        try {
            txManager.begin();
            // 这里不抛异常，模拟成功
            System.out.println("执行正常逻辑...");
            txManager.commit();
            return "事务提交成功";
        } catch (Exception e) {
            txManager.rollback();
            return "事务回滚：" + e.getMessage();
        }
    }

    /**
     * 测试事务回滚
     *
     * http://localhost:8080/test/tx/rollback
     */
    @GetMapping("/test/tx/rollback")
    public String rollback() {

        TransactionManager txManager = new TransactionManager(dataSource);

        try {
            txManager.begin();
            accountService.transfer(); // 内部抛异常
            txManager.commit();
            return "事务提交成功";
        } catch (Exception e) {
            txManager.rollback();
            return "事务回滚：" + e.getMessage();
        }
    }
}
```

## 日期格式化工具（解决 SimpleDateFormat 线程安全问题）

使用 ThreadLocal 为每个线程提供独立的 SimpleDateFormat 实例，解决线程安全问题

```java
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;

/**
 * 线程安全的日期工具类（基于 ThreadLocal 实现）
 *
 * @author Ateng
 * @since 2026-04-10
 */
public class DateFormatUtil {

    /**
     * 默认日期格式
     */
    private static final String DEFAULT_PATTERN = "yyyy-MM-dd HH:mm:ss";

    /**
     * ThreadLocal 缓存 SimpleDateFormat
     */
    private static final ThreadLocal<SimpleDateFormat> SDF_THREAD_LOCAL =
            ThreadLocal.withInitial(() -> new SimpleDateFormat(DEFAULT_PATTERN));

    /**
     * 格式化日期
     *
     * @param date 日期
     * @return 字符串
     */
    public static String format(Date date) {
        return SDF_THREAD_LOCAL.get().format(date);
    }

    /**
     * 解析日期
     *
     * @param dateStr 日期字符串
     * @return Date
     */
    public static Date parse(String dateStr) {
        try {
            return SDF_THREAD_LOCAL.get().parse(dateStr);
        } catch (ParseException e) {
            throw new RuntimeException("日期解析失败：" + dateStr, e);
        }
    }

    /**
     * 获取当前线程的 SimpleDateFormat（扩展用）
     */
    public static SimpleDateFormat get() {
        return SDF_THREAD_LOCAL.get();
    }

    /**
     * 清理 ThreadLocal（一般可不手动调用，线程结束自动释放）
     */
    public static void clear() {
        SDF_THREAD_LOCAL.remove();
    }
}
```

模拟高并发场景测试（验证线程安全）

```java
import java.util.Date;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * 日期工具测试类
 *
 * @author Ateng
 * @since 2026-04-10
 */
public class DateFormatTest {

    public static void main(String[] args) {

        ExecutorService executor = Executors.newFixedThreadPool(10);

        for (int i = 0; i < 20; i++) {
            executor.execute(() -> {
                String dateStr = DateFormatUtil.format(new Date());
                System.out.println(Thread.currentThread().getName() + " -> " + dateStr);

                // 再解析回去
                Date date = DateFormatUtil.parse(dateStr);
                System.out.println(Thread.currentThread().getName() + " parse -> " + date);
            });
        }

        executor.shutdown();
    }
}
```

提供 Controller 方便测试

```java
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Date;

/**
 * 日期格式化测试控制器
 *
 * @author Ateng
 * @since 2026-04-10
 */
@RestController
public class DateController {

    /**
     * 测试格式化
     *
     * http://localhost:8080/test/date/format
     */
    @GetMapping("/test/date/format")
    public String format() {
        return DateFormatUtil.format(new Date());
    }

    /**
     * 测试解析
     *
     * http://localhost:8080/test/date/parse?date=2026-04-10 12:00:00
     */
    @GetMapping("/test/date/parse")
    public String parse(String date) {
        return DateFormatUtil.parse(date).toString();
    }
}
```

## 接口幂等 Token 上下文缓存

通过 ThreadLocal 缓存当前请求的幂等 Token，在业务链路中统一获取，避免重复解析请求头或参数

```java
import cn.hutool.core.util.StrUtil;

/**
 * 幂等 Token 上下文工具类（基于 ThreadLocal 实现）
 *
 * @author Ateng
 * @since 2026-04-10
 */
public class IdempotentTokenContextHolder {

    /**
     * 存储 Token
     */
    private static final ThreadLocal<String> TOKEN_THREAD_LOCAL = new ThreadLocal<>();

    /**
     * 设置 Token
     *
     * @param token 幂等 Token
     */
    public static void set(String token) {
        if (StrUtil.isNotBlank(token)) {
            TOKEN_THREAD_LOCAL.set(token);
        }
    }

    /**
     * 获取 Token
     *
     * @return Token
     */
    public static String get() {
        return TOKEN_THREAD_LOCAL.get();
    }

    /**
     * 清理
     */
    public static void clear() {
        TOKEN_THREAD_LOCAL.remove();
    }
}
```

简单幂等校验工具（基于内存模拟，可替换为 Redis）

```java
import cn.hutool.core.util.StrUtil;

import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 幂等校验工具类（示例：本地缓存实现）
 *
 * @author Ateng
 * @since 2026-04-10
 */
public class IdempotentUtil {

    /**
     * 已使用 Token 缓存（生产建议使用 Redis）
     */
    private static final Set<String> TOKEN_CACHE = ConcurrentHashMap.newKeySet();

    /**
     * 校验并标记 Token
     *
     * @param token 幂等 Token
     */
    public static void checkAndSave(String token) {

        if (StrUtil.isBlank(token)) {
            throw new RuntimeException("幂等 Token 不能为空");
        }

        // 如果已存在，说明重复请求
        if (!TOKEN_CACHE.add(token)) {
            throw new RuntimeException("重复请求");
        }
    }
}
```

拦截器统一获取 Token 并做幂等校验

```java
import cn.hutool.core.util.StrUtil;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

/**
 * 幂等拦截器
 *
 * @author Ateng
 * @since 2026-04-10
 */
@Component
public class IdempotentInterceptor implements HandlerInterceptor {

    private static final String HEADER_TOKEN = "X-Idempotent-Token";

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {

        String token = request.getHeader(HEADER_TOKEN);

        if (StrUtil.isBlank(token)) {
            throw new RuntimeException("缺少幂等 Token");
        }

        // 放入 ThreadLocal
        IdempotentTokenContextHolder.set(token);

        // 幂等校验
        IdempotentUtil.checkAndSave(token);

        return true;
    }

    @Override
    public void afterCompletion(HttpServletRequest request, HttpServletResponse response, Object handler, Exception ex) {
        IdempotentTokenContextHolder.clear();
    }
}
```

注册拦截器

```java
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * Web 配置类（注册幂等拦截器）
 *
 * @author Ateng
 * @since 2026-04-10
 */
@Configuration
public class WebConfig implements WebMvcConfigurer {

    @Autowired
    private IdempotentInterceptor idempotentInterceptor;

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(idempotentInterceptor).addPathPatterns("/**");
    }
}
```

业务中直接使用 Token（无需重复获取）

```java
/**
 * 示例业务类（使用幂等 Token）
 *
 * @author Ateng
 * @since 2026-04-10
 */
@Service
public class OrderIdempotentService {

    public String createOrder() {
        String token = IdempotentTokenContextHolder.get();
        return "订单创建成功，Token：" + token;
    }
}
```

提供 Controller 方便测试幂等效果

```java
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 幂等测试控制器
 *
 * @author Ateng
 * @since 2026-04-10
 */
@RestController
public class IdempotentController {

    private final OrderIdempotentService service;

    public IdempotentController(OrderIdempotentService service) {
        this.service = service;
    }

    /**
     * 测试接口
     *
     * curl 示例：
     * curl -H "X-Idempotent-Token: abc123" http://localhost:8080/test/idempotent
     *
     * 同一个 Token 重复请求会报错
     */
    @GetMapping("/test/idempotent")
    public String test() {
        return service.createOrder();
    }
}
```

## 灰度/租户上下文（多租户隔离）

通过 ThreadLocal 存储当前请求的租户标识（tenantId）或灰度标识，实现多租户数据隔离与灰度控制

```java
import cn.hutool.core.util.StrUtil;

/**
 * 租户上下文工具类（基于 ThreadLocal 实现）
 *
 * @author Ateng
 * @since 2026-04-10
 */
public class TenantContextHolder {

    /**
     * 存储租户ID
     */
    private static final ThreadLocal<String> TENANT_THREAD_LOCAL = new ThreadLocal<>();

    /**
     * 默认租户
     */
    private static final String DEFAULT_TENANT = "default";

    /**
     * 设置租户ID
     *
     * @param tenantId 租户ID
     */
    public static void set(String tenantId) {
        if (StrUtil.isNotBlank(tenantId)) {
            TENANT_THREAD_LOCAL.set(tenantId);
        }
    }

    /**
     * 获取租户ID
     *
     * @return tenantId
     */
    public static String get() {
        String tenantId = TENANT_THREAD_LOCAL.get();
        return StrUtil.isNotBlank(tenantId) ? tenantId : DEFAULT_TENANT;
    }

    /**
     * 清理
     */
    public static void clear() {
        TENANT_THREAD_LOCAL.remove();
    }
}
```

模拟租户数据隔离（实际可用于 SQL 拼接 / 数据源切换 / MyBatis 拦截器）

```java
/**
 * 示例业务类（多租户）
 *
 * @author Ateng
 * @since 2026-04-10
 */
@Service
public class TenantService {

    /**
     * 查询数据（根据租户隔离）
     */
    public String queryData() {
        String tenantId = TenantContextHolder.get();
        return "当前租户：" + tenantId + "，返回对应数据";
    }
}
```

拦截器统一解析租户信息（Header / Token / 参数）

```java
import cn.hutool.core.util.StrUtil;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

/**
 * 租户拦截器
 *
 * @author Ateng
 * @since 2026-04-10
 */
@Component
public class TenantInterceptor implements HandlerInterceptor {

    private static final String HEADER_TENANT = "X-Tenant-Id";

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {

        // 1. 从请求头获取
        String tenantId = request.getHeader(HEADER_TENANT);

        // 2. 兜底默认租户
        if (StrUtil.isBlank(tenantId)) {
            tenantId = "default";
        }

        // 3. 放入 ThreadLocal
        TenantContextHolder.set(tenantId);

        return true;
    }

    @Override
    public void afterCompletion(HttpServletRequest request, HttpServletResponse response, Object handler, Exception ex) {
        TenantContextHolder.clear();
    }
}
```

注册拦截器

```java
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * Web 配置类（注册租户拦截器）
 *
 * @author Ateng
 * @since 2026-04-10
 */
@Configuration
public class WebConfig implements WebMvcConfigurer {

    @Autowired
    private TenantInterceptor tenantInterceptor;

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(tenantInterceptor).addPathPatterns("/**");
    }
}
```

提供 Controller 方便测试租户隔离效果

```java
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 多租户测试控制器
 *
 * @author Ateng
 * @since 2026-04-10
 */
@RestController
public class TenantController {

    private final TenantService tenantService;

    public TenantController(TenantService tenantService) {
        this.tenantService = tenantService;
    }

    /**
     * 测试接口
     *
     * curl 示例：
     * curl http://localhost:8080/test/tenant
     * curl -H "X-Tenant-Id: tenantA" http://localhost:8080/test/tenant
     *
     * @return 租户数据
     */
    @GetMapping("/test/tenant")
    public String test() {
        return tenantService.queryData();
    }
}
```

## 权限上下文缓存（减少重复查询）

通过 ThreadLocal 缓存当前用户的权限信息（角色/权限码），避免在一次请求中重复查询数据库或远程服务

```java
import cn.hutool.core.collection.CollUtil;

import java.util.Set;

/**
 * 权限上下文工具类（基于 ThreadLocal 实现）
 *
 * @author Ateng
 * @since 2026-04-10
 */
public class PermissionContextHolder {

    /**
     * 存储当前用户权限集合
     */
    private static final ThreadLocal<Set<String>> PERMISSION_THREAD_LOCAL = new ThreadLocal<>();

    /**
     * 设置权限集合
     *
     * @param permissions 权限集合
     */
    public static void set(Set<String> permissions) {
        if (CollUtil.isNotEmpty(permissions)) {
            PERMISSION_THREAD_LOCAL.set(permissions);
        }
    }

    /**
     * 获取权限集合
     *
     * @return 权限集合
     */
    public static Set<String> get() {
        return PERMISSION_THREAD_LOCAL.get();
    }

    /**
     * 判断是否拥有某权限
     *
     * @param permission 权限码
     * @return 是否拥有
     */
    public static boolean hasPermission(String permission) {
        Set<String> permissions = get();
        return CollUtil.isNotEmpty(permissions) && permissions.contains(permission);
    }

    /**
     * 清理
     */
    public static void clear() {
        PERMISSION_THREAD_LOCAL.remove();
    }
}
```

模拟权限查询（实际项目中应查询数据库或远程服务）

```java
import java.util.HashSet;
import java.util.Set;

/**
 * 权限服务（模拟查询）
 *
 * @author Ateng
 * @since 2026-04-10
 */
@Service
public class PermissionService {

    /**
     * 根据用户ID获取权限集合
     */
    public Set<String> getPermissions(Long userId) {

        // 模拟不同用户权限
        Set<String> permissions = new HashSet<>();

        if (userId == 1001L) {
            permissions.add("order:create");
            permissions.add("order:view");
        } else {
            permissions.add("order:view");
        }

        return permissions;
    }
}
```

拦截器中一次性加载权限并缓存到 ThreadLocal

```java
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.util.Set;

/**
 * 权限拦截器
 *
 * @author Ateng
 * @since 2026-04-10
 */
@Component
public class PermissionInterceptor implements HandlerInterceptor {

    private final PermissionService permissionService;

    public PermissionInterceptor(PermissionService permissionService) {
        this.permissionService = permissionService;
    }

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {

        // 从用户上下文获取用户ID（依赖前面 UserContextHolder）
        Long userId = UserContextHolder.getUserId();

        // 查询权限
        Set<String> permissions = permissionService.getPermissions(userId);

        // 放入 ThreadLocal
        PermissionContextHolder.set(permissions);

        return true;
    }

    @Override
    public void afterCompletion(HttpServletRequest request, HttpServletResponse response, Object handler, Exception ex) {
        PermissionContextHolder.clear();
    }
}
```

注册拦截器

```java
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * Web 配置类（注册权限拦截器）
 *
 * @author Ateng
 * @since 2026-04-10
 */
@Configuration
public class WebConfig implements WebMvcConfigurer {

    @Autowired
    private PermissionInterceptor permissionInterceptor;

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(permissionInterceptor).addPathPatterns("/**");
    }
}
```

业务中直接使用权限上下文进行校验

```java
/**
 * 示例业务类（权限校验）
 *
 * @author Ateng
 * @since 2026-04-10
 */
@Service
public class OrderPermissionService {

    /**
     * 创建订单（需要权限）
     */
    public String createOrder() {
        if (!PermissionContextHolder.hasPermission("order:create")) {
            throw new RuntimeException("无权限创建订单");
        }
        return "订单创建成功";
    }

    /**
     * 查看订单
     */
    public String viewOrder() {
        if (!PermissionContextHolder.hasPermission("order:view")) {
            throw new RuntimeException("无权限查看订单");
        }
        return "订单查看成功";
    }
}
```

提供 Controller 方便测试权限效果

```java
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 权限测试控制器
 *
 * @author Ateng
 * @since 2026-04-10
 */
@RestController
public class PermissionController {

    private final OrderPermissionService service;

    public PermissionController(OrderPermissionService service) {
        this.service = service;
    }

    /**
     * 测试创建订单权限
     *
     * http://localhost:8080/test/permission/create
     */
    @GetMapping("/test/permission/create")
    public String create() {
        return service.createOrder();
    }

    /**
     * 测试查看订单权限
     *
     * http://localhost:8080/test/permission/view
     */
    @GetMapping("/test/permission/view")
    public String view() {
        return service.viewOrder();
    }
}
```

## 分页上下文（如 PageHelper 原理实现）

通过 ThreadLocal 存储分页参数，在 DAO 层统一获取，实现类似 PageHelper 的“无侵入分页”

```java
import cn.hutool.core.util.ObjectUtil;

/**
 * 分页上下文工具类（基于 ThreadLocal 实现）
 *
 * @author Ateng
 * @since 2026-04-10
 */
public class PageContextHolder {

    /**
     * 存储分页对象
     */
    private static final ThreadLocal<Page> PAGE_THREAD_LOCAL = new ThreadLocal<>();

    /**
     * 设置分页参数
     *
     * @param page 页码（从1开始）
     * @param size 每页条数
     */
    public static void set(int page, int size) {
        PAGE_THREAD_LOCAL.set(new Page(page, size));
    }

    /**
     * 获取分页参数
     *
     * @return Page
     */
    public static Page get() {
        return PAGE_THREAD_LOCAL.get();
    }

    /**
     * 获取 offset（SQL 偏移量）
     */
    public static int getOffset() {
        Page page = get();
        if (ObjectUtil.isNull(page)) {
            return 0;
        }
        return (page.getPage() - 1) * page.getSize();
    }

    /**
     * 获取 limit
     */
    public static int getLimit() {
        Page page = get();
        return ObjectUtil.isNotNull(page) ? page.getSize() : 0;
    }

    /**
     * 清理
     */
    public static void clear() {
        PAGE_THREAD_LOCAL.remove();
    }
}
```

分页对象定义

```java
/**
 * 分页对象
 *
 * @author Ateng
 * @since 2026-04-10
 */
public class Page {

    private int page;
    private int size;

    public Page(int page, int size) {
        this.page = page;
        this.size = size;
    }

    public int getPage() {
        return page;
    }

    public int getSize() {
        return size;
    }
}
```

DAO 层统一使用分页上下文拼接 SQL（模拟）

```java
import java.util.ArrayList;
import java.util.List;

/**
 * 示例 DAO（模拟分页查询）
 *
 * @author Ateng
 * @since 2026-04-10
 */
@Repository
public class UserDao {

    /**
     * 查询用户列表（自动分页）
     */
    public List<String> queryUsers() {

        int offset = PageContextHolder.getOffset();
        int limit = PageContextHolder.getLimit();

        // 模拟 SQL
        String sql = "SELECT * FROM user LIMIT " + offset + ", " + limit;
        System.out.println("执行SQL：" + sql);

        // 模拟返回数据
        List<String> list = new ArrayList<>();
        for (int i = 1; i <= limit; i++) {
            list.add("User_" + (offset + i));
        }

        return list;
    }
}
```

Service 层调用（无侵入）

```java
import java.util.List;

/**
 * 示例业务类
 *
 * @author Ateng
 * @since 2026-04-10
 */
@Service
public class UserPageService {

    private final UserDao userDao;

    public UserPageService(UserDao userDao) {
        this.userDao = userDao;
    }

    public List<String> listUsers() {
        return userDao.queryUsers();
    }
}
```

Controller 中设置分页参数（类似 PageHelper.startPage）

```java
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 分页测试控制器
 *
 * @author Ateng
 * @since 2026-04-10
 */
@RestController
public class PageController {

    private final UserPageService service;

    public PageController(UserPageService service) {
        this.service = service;
    }

    /**
     * 测试分页
     *
     * http://localhost:8080/test/page?page=1&size=5
     */
    @GetMapping("/test/page")
    public List<String> page(int page, int size) {

        try {
            // 设置分页上下文
            PageContextHolder.set(page, size);

            return service.listUsers();
        } finally {
            // 必须清理
            PageContextHolder.clear();
        }
    }
}
```

## 日志增强上下文（MDC 简化版实现）

通过 ThreadLocal 存储日志上下文（如 traceId、userId），在日志打印时自动携带，实现类似 MDC 的效果

```java
import java.util.HashMap;
import java.util.Map;

/**
 * 日志上下文工具类（MDC 简化版，基于 ThreadLocal 实现）
 *
 * @author Ateng
 * @since 2026-04-10
 */
public class LogContextHolder {

    /**
     * 存储日志上下文（key-value）
     */
    private static final ThreadLocal<Map<String, String>> CONTEXT_THREAD_LOCAL =
            ThreadLocal.withInitial(HashMap::new);

    /**
     * 设置上下文
     *
     * @param key   键
     * @param value 值
     */
    public static void put(String key, String value) {
        CONTEXT_THREAD_LOCAL.get().put(key, value);
    }

    /**
     * 获取上下文值
     *
     * @param key 键
     * @return 值
     */
    public static String get(String key) {
        return CONTEXT_THREAD_LOCAL.get().get(key);
    }

    /**
     * 获取全部上下文
     */
    public static Map<String, String> getAll() {
        return CONTEXT_THREAD_LOCAL.get();
    }

    /**
     * 移除某个 key
     */
    public static void remove(String key) {
        CONTEXT_THREAD_LOCAL.get().remove(key);
    }

    /**
     * 清空
     */
    public static void clear() {
        CONTEXT_THREAD_LOCAL.remove();
    }
}
```

简单日志工具类（自动拼接上下文）

```java
import cn.hutool.core.map.MapUtil;

/**
 * 日志工具类（增强日志输出）
 *
 * @author Ateng
 * @since 2026-04-10
 */
public class LogUtil {

    /**
     * 打印日志（自动带上下文）
     *
     * @param message 日志内容
     */
    public static void info(String message) {

        StringBuilder sb = new StringBuilder();

        // 拼接上下文
        if (MapUtil.isNotEmpty(LogContextHolder.getAll())) {
            sb.append("[");
            LogContextHolder.getAll().forEach((k, v) -> sb.append(k).append("=").append(v).append(" "));
            sb.append("] ");
        }

        sb.append(message);

        System.out.println(sb.toString());
    }
}
```

拦截器中统一注入日志上下文（如 traceId、userId）

```java
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

/**
 * 日志上下文拦截器
 *
 * @author Ateng
 * @since 2026-04-10
 */
@Component
public class LogContextInterceptor implements HandlerInterceptor {

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {

        // 可结合前面示例（TraceId + UserContext）
        String traceId = TraceIdContextHolder.getOrCreate();
        Long userId = UserContextHolder.getUserId();

        LogContextHolder.put("traceId", traceId);
        LogContextHolder.put("userId", String.valueOf(userId));

        return true;
    }

    @Override
    public void afterCompletion(HttpServletRequest request, HttpServletResponse response, Object handler, Exception ex) {
        LogContextHolder.clear();
    }
}
```

注册拦截器

```java
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * Web 配置类（注册日志上下文拦截器）
 *
 * @author Ateng
 * @since 2026-04-10
 */
@Configuration
public class LogWebConfig implements WebMvcConfigurer {

    @Autowired
    private LogContextInterceptor logContextInterceptor;

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(logContextInterceptor).addPathPatterns("/**");
    }
}
```

业务中打印日志（自动带上下文）

```java
/**
 * 示例业务类（日志增强）
 *
 * @author Ateng
 * @since 2026-04-10
 */
@Service
public class LogService {

    public String process() {

        LogUtil.info("开始处理业务");

        // 模拟业务逻辑
        LogUtil.info("处理中...");

        LogUtil.info("处理完成");

        return "ok";
    }
}
```

提供 Controller 方便测试日志上下文效果

```java
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 日志上下文测试控制器
 *
 * @author Ateng
 * @since 2026-04-10
 */
@RestController
public class LogController {

    private final LogService logService;

    public LogController(LogService logService) {
        this.logService = logService;
    }

    /**
     * 测试日志
     *
     * http://localhost:8080/test/log
     */
    @GetMapping("/test/log")
    public String test() {
        return logService.process();
    }
}
```

