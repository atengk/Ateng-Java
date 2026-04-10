# Scoped Value 

**Scoped Value** 是在 JDK 21 中引入的一种轻量级上下文传递机制，用于在受控作用域内安全地共享不可变数据。它主要用于替代传统的 ThreadLocal，特别是在虚拟线程（Project Loom）场景下表现更优。Scoped Value 采用“只读 + 作用域绑定”的设计，避免了数据泄漏和线程复用带来的问题。其值仅在绑定的代码块中可见，超出作用域自动失效，从而提升并发安全性与可维护性。

------



## 请求级上下文透传（替代 ThreadLocal）

用于存储 userId、traceId、tenantId 等，在 Controller → Service → DAO 全链路安全传递

请求级上下文透传（基于 ScopedValue 替代 ThreadLocal，实现 userId / traceId 全链路传递）

定义请求上下文（存储 userId、traceId）

```java
package io.github.atengk.context;

import java.util.UUID;

/**
 * 请求上下文工具类（基于 ScopedValue 实现）
 *
 * 用于在整个调用链中传递用户信息和链路追踪ID
 *
 * @author Ateng
 * @since 2026-04-10
 */
public final class RequestContext {

    /**
     * 用户ID
     */
    public static final ScopedValue<String> USER_ID = ScopedValue.newInstance();

    /**
     * 链路追踪ID
     */
    public static final ScopedValue<String> TRACE_ID = ScopedValue.newInstance();

    private RequestContext() {
    }

    /**
     * 生成 TraceId
     *
     * @return TraceId
     */
    public static String generateTraceId() {
        return UUID.randomUUID().toString().replace("-", "");
    }
}
```

初始化上下文的过滤器（请求进入时绑定 ScopedValue）

```java
package io.github.atengk.filter;

import io.github.atengk.context.RequestContext;
import jakarta.servlet.*;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.stereotype.Component;

import java.io.IOException;

/**
 * 请求上下文初始化过滤器
 *
 * 在每个请求进入时初始化 ScopedValue 上下文
 *
 * @author Ateng
 * @since 2026-04-10
 */
@Component
public class ContextFilter implements Filter {

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
            throws IOException, ServletException {

        HttpServletRequest httpRequest = (HttpServletRequest) request;

        String userId = httpRequest.getHeader("X-USER-ID");
        String traceId = httpRequest.getHeader("X-TRACE-ID");

        if (traceId == null || traceId.isEmpty()) {
            traceId = RequestContext.generateTraceId();
        }

        String finalUserId = userId;
        String finalTraceId = traceId;

        ScopedValue.where(RequestContext.USER_ID, finalUserId)
                .where(RequestContext.TRACE_ID, finalTraceId)
                .run(() -> {
                    try {
                        chain.doFilter(request, response);
                    } catch (IOException | ServletException e) {
                        throw new RuntimeException(e);
                    }
                });
    }
}
```

业务层使用上下文（任意位置获取，无需传参）

```java
package io.github.atengk.service;

import io.github.atengk.context.RequestContext;
import org.springframework.stereotype.Service;

/**
 * 用户业务示例
 *
 * 演示如何获取 ScopedValue 中的数据
 *
 * @author Ateng
 * @since 2026-04-10
 */
@Service
public class UserService {

    /**
     * 示例方法
     */
    public void process() {

        String userId = RequestContext.USER_ID.get();
        String traceId = RequestContext.TRACE_ID.get();

        System.out.println("用户ID：" + userId);
        System.out.println("TraceId：" + traceId);
    }
}
```

控制层调用（触发完整链路）

```java
package io.github.atengk.basic.controller;

import io.github.atengk.basic.service.UserService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 控制层示例
 *
 * @author Ateng
 * @since 2026-04-10
 */
@RestController
public class UserController {

    private final UserService userService;

    public UserController(UserService userService) {
        this.userService = userService;
    }

    /**
     * 测试接口
     *
     * curl 示例：
     * curl -H "X-USER-ID: abc123" -H "X-TRACE-ID: abc123" http://localhost:10005/test
     *
     * @return 结果
     */
    @GetMapping("/test")
    public String test() {
        userService.process();
        return "ok";
    }
}
```

日志输出示例（效果）

```text
用户ID：abc123
TraceId：abc123
```

## 统一日志链路追踪（TraceId/MDC 替代方案）

结合 Scoped Value 实现无污染的日志上下文（避免 ThreadLocal 泄漏问题）

统一日志链路追踪（基于 ScopedValue 替代 MDC，实现 TraceId 自动注入日志）

定义日志上下文工具（封装获取 TraceId）

```java
package io.github.atengk.context;

/**
 * 日志上下文工具类
 *
 * 提供统一获取 TraceId 的能力
 *
 * @author Ateng
 * @since 2026-04-10
 */
public final class LogContext {

    private LogContext() {
    }

    /**
     * 获取当前 TraceId
     *
     * @return TraceId
     */
    public static String getTraceId() {
        return RequestContext.TRACE_ID.isBound()
                ? RequestContext.TRACE_ID.get()
                : "N/A";
    }
}
```

自定义日志工具（统一输出日志并自动带上 TraceId）

```java
package io.github.atengk.util;

import io.github.atengk.context.LogContext;
import cn.hutool.core.util.StrUtil;

/**
 * 自定义日志工具类
 *
 * 统一日志格式，自动追加 TraceId
 *
 * @author Ateng
 * @since 2026-04-10
 */
public final class LogUtil {

    private LogUtil() {
    }

    /**
     * info日志
     *
     * @param msg 日志内容
     */
    public static void info(String msg) {
        String traceId = LogContext.getTraceId();
        System.out.println(StrUtil.format("[INFO] [traceId={}] {}", traceId, msg));
    }

    /**
     * error日志
     *
     * @param msg 日志内容
     * @param e   异常
     */
    public static void error(String msg, Throwable e) {
        String traceId = LogContext.getTraceId();
        System.out.println(StrUtil.format("[ERROR] [traceId={}] {}", traceId, msg));
        e.printStackTrace();
    }
}
```

业务层使用日志（自动携带 TraceId）

```java
package io.github.atengk.service;

import io.github.atengk.util.LogUtil;
import org.springframework.stereotype.Service;

/**
 * 用户业务示例
 *
 * 演示日志自动携带 TraceId
 *
 * @author Ateng
 * @since 2026-04-10
 */
@Service
public class UserService {

    /**
     * 示例方法
     */
    public void process() {
        LogUtil.info("开始处理用户业务");

        try {
            int a = 1 / 0;
        } catch (Exception e) {
            LogUtil.error("业务异常", e);
        }

        LogUtil.info("结束处理用户业务");
    }
}
```

控制层调用（提供 curl 示例）

```java
package io.github.atengk.controller;

import io.github.atengk.service.UserService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 控制层示例
 *
 * @author Ateng
 * @since 2026-04-10
 */
@RestController
public class UserController {

    private final UserService userService;

    public UserController(UserService userService) {
        this.userService = userService;
    }

    /**
     * 测试接口
     *
     * curl -H "X-USER-ID: abc123" -H "X-TRACE-ID: abc123" http://localhost:10005/test
     *
     * @return 结果
     */
    @GetMapping("/test")
    public String test() {
        userService.process();
        return "ok";
    }
}
```

日志输出示例（效果）

```text
[INFO] [traceId=abc123] 开始处理用户业务
[ERROR] [traceId=abc123] 业务异常
[INFO] [traceId=abc123] 结束处理用户业务
```

## 多租户上下文隔离（Tenant 上下文）

在多租户系统中优雅传递 tenantId，避免手动参数传递

多租户上下文隔离（基于 ScopedValue 实现 tenantId 贯穿全链路，避免参数污染）

定义租户上下文（tenantId + 统一获取能力）

```java
package io.github.atengk.context;

/**
 * 多租户上下文
 *
 * 使用 ScopedValue 实现 tenantId 线程隔离与链路透传
 *
 * @author Ateng
 * @since 2026-04-10
 */
public final class TenantContext {

    /**
     * 租户ID
     */
    public static final ScopedValue<String> TENANT_ID = ScopedValue.newInstance();

    private TenantContext() {
    }

    /**
     * 获取当前租户ID
     *
     * @return tenantId
     */
    public static String getTenantId() {
        return TENANT_ID.isBound() ? TENANT_ID.get() : null;
    }
}
```

请求过滤器（初始化 tenantId + 绑定 ScopedValue）

```java
package io.github.atengk.filter;

import io.github.atengk.context.RequestContext;
import io.github.atengk.context.TenantContext;
import jakarta.servlet.*;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.stereotype.Component;

import java.io.IOException;

/**
 * 多租户上下文过滤器
 *
 * 在请求入口绑定 tenantId / userId / traceId
 *
 * @author Ateng
 * @since 2026-04-10
 */
@Component
public class TenantContextFilter implements Filter {

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
            throws IOException, ServletException {

        HttpServletRequest httpRequest = (HttpServletRequest) request;

        String tenantId = httpRequest.getHeader("X-TENANT-ID");
        String userId = httpRequest.getHeader("X-USER-ID");
        String traceId = httpRequest.getHeader("X-TRACE-ID");

        if (traceId == null || traceId.isEmpty()) {
            traceId = RequestContext.generateTraceId();
        }

        String finalTenantId = tenantId;
        String finalUserId = userId;
        String finalTraceId = traceId;

        ScopedValue.where(TenantContext.TENANT_ID, finalTenantId)
                .where(RequestContext.USER_ID, finalUserId)
                .where(RequestContext.TRACE_ID, finalTraceId)
                .run(() -> {
                    try {
                        chain.doFilter(request, response);
                    } catch (IOException | ServletException e) {
                        throw new RuntimeException(e);
                    }
                });
    }
}
```

业务层（自动隔离租户数据查询）

```java
package io.github.atengk.service;

import io.github.atengk.context.TenantContext;
import io.github.atengk.context.RequestContext;
import io.github.atengk.util.LogUtil;
import org.springframework.stereotype.Service;

/**
 * 租户业务示例
 *
 * 演示 tenantId 在业务链路中的自动隔离能力
 *
 * @author Ateng
 * @since 2026-04-10
 */
@Service
public class TenantUserService {

    /**
     * 模拟查询用户数据（按租户隔离）
     */
    public void queryUser() {

        String tenantId = TenantContext.getTenantId();
        String userId = RequestContext.USER_ID.get();

        LogUtil.info("查询用户数据，tenantId=" + tenantId + ", userId=" + userId);

        // 模拟 SQL：select * from user where tenant_id = ?
    }
}
```

控制层（curl 调用示例）

```java
package io.github.atengk.controller;

import io.github.atengk.service.TenantUserService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 多租户控制器示例
 *
 * @author Ateng
 * @since 2026-04-10
 */
@RestController
public class TenantController {

    private final TenantUserService tenantUserService;

    public TenantController(TenantUserService tenantUserService) {
        this.tenantUserService = tenantUserService;
    }

    /**
     * 租户查询测试接口
     *
     * curl -H "X-TENANT-ID: t001" -H "X-USER-ID: u100" -H "X-TRACE-ID: abc123" http://localhost:10005/tenant/query
     *
     * @return 结果
     */
    @GetMapping("/tenant/query")
    public String query() {
        tenantUserService.queryUser();
        return "ok";
    }
}
```

日志输出示例（效果）

```text
[INFO] [traceId=abc123] 查询用户数据，tenantId=t001, userId=u100
```

## 异步任务上下文传递（StructuredTaskScope）

解决线程池/并发场景下 ThreadLocal 丢失问题，实现上下文自动继承

异步任务上下文传递（基于 JDK21 StructuredTaskScope，实现 ScopedValue 在并发子任务中的自动继承）

定义异步执行工具（统一管理 StructuredTaskScope + 上下文透传）

```java
package io.github.atengk.async;

import io.github.atengk.context.RequestContext;
import io.github.atengk.context.TenantContext;

import java.util.concurrent.Callable;
import java.util.concurrent.StructuredTaskScope;

/**
 * 异步任务执行工具（StructuredTaskScope + ScopedValue）
 *
 * 用于解决线程池场景下上下文丢失问题，实现请求级上下文自动传播
 *
 * @author Ateng
 * @since 2026-04-10
 */
public final class AsyncExecutor {

    private AsyncExecutor() {
    }

    /**
     * 执行两个并行任务，并返回组合结果
     *
     * @param taskA 任务A
     * @param taskB 任务B
     * @return 组合结果
     */
    public static <A, B> Result<A, B> run(Callable<A> taskA, Callable<B> taskB) {

        try (var scope = new StructuredTaskScope.ShutdownOnFailure()) {

            StructuredTaskScope.Subtask<A> a = scope.fork(taskA);
            StructuredTaskScope.Subtask<B> b = scope.fork(taskB);

            scope.join();
            scope.throwIfFailed();

            return new Result<>(a.get(), b.get());
        } catch (Exception e) {
            throw new RuntimeException("异步任务执行失败", e);
        }
    }

    /**
     * 组合结果
     */
    public record Result<A, B>(A a, B b) {
    }
}
```

异步业务服务（在子任务中自动获取 ScopedValue 上下文）

```java
package io.github.atengk.service;

import io.github.atengk.async.AsyncExecutor;
import io.github.atengk.context.RequestContext;
import io.github.atengk.context.TenantContext;
import io.github.atengk.util.LogUtil;
import org.springframework.stereotype.Service;

/**
 * 异步业务示例
 *
 * 演示 StructuredTaskScope 下 ScopedValue 自动传播能力
 *
 * @author Ateng
 * @since 2026-04-10
 */
@Service
public class AsyncUserService {

    /**
     * 并行执行两个任务
     */
    public void processAsync() {

        AsyncExecutor.run(
                () -> {

                    LogUtil.info("任务A执行，userId=" + RequestContext.USER_ID.get()
                            + ", tenantId=" + TenantContext.getTenantId());

                    return "A完成";
                },
                () -> {

                    LogUtil.info("任务B执行，userId=" + RequestContext.USER_ID.get()
                            + ", tenantId=" + TenantContext.getTenantId());

                    return "B完成";
                }
        );
    }
}
```

控制层（包含 curl 调用示例）

```java
package io.github.atengk.controller;

import io.github.atengk.service.AsyncUserService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 异步任务控制器
 *
 * @author Ateng
 * @since 2026-04-10
 */
@RestController
public class AsyncController {

    private final AsyncUserService asyncUserService;

    public AsyncController(AsyncUserService asyncUserService) {
        this.asyncUserService = asyncUserService;
    }

    /**
     * 异步任务测试接口
     *
     * curl -H "X-USER-ID: u100" -H "X-TENANT-ID: t001" -H "X-TRACE-ID: abc123" http://localhost:10005/async/test
     *
     * @return 结果
     */
    @GetMapping("/async/test")
    public String test() {
        asyncUserService.processAsync();
        return "ok";
    }
}
```

日志输出示例（效果）

```text
[INFO] [traceId=abc123] 任务A执行，userId=u100, tenantId=t001
[INFO] [traceId=abc123] 任务B执行，userId=u100, tenantId=t001
```

## 权限与用户信息上下文（登录态共享）

在业务代码中随处获取当前登录用户，无需显式传参（替代 SecurityContextHolder 部分场景）

权限与用户信息上下文（基于 ScopedValue 构建登录态共享上下文，用于统一获取当前用户、角色、权限信息）

定义用户登录上下文（UserContext）

```java
package io.github.atengk.context;

import java.util.List;

/**
 * 用户登录上下文
 *
 * 用于承载当前登录用户的基础信息、角色、权限
 *
 * @author Ateng
 * @since 2026-04-10
 */
public final class UserContext {

    /**
     * 当前登录用户信息
     */
    public static final ScopedValue<UserInfo> USER = ScopedValue.newInstance();

    private UserContext() {
    }

    /**
     * 获取当前用户
     *
     * @return UserInfo
     */
    public static UserInfo getUser() {
        return USER.isBound() ? USER.get() : null;
    }

    /**
     * 用户信息对象
     */
    public record UserInfo(
            String userId,
            String username,
            List<String> roles,
            List<String> permissions
    ) {
    }
}
```

登录态解析过滤器（模拟 Token -> 用户信息 + ScopedValue 绑定）

```java
package io.github.atengk.filter;

import io.github.atengk.context.RequestContext;
import io.github.atengk.context.TenantContext;
import io.github.atengk.context.UserContext;
import jakarta.servlet.*;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.List;

/**
 * 用户登录态过滤器
 *
 * 模拟从请求头解析 token 并构建用户上下文
 *
 * @author Ateng
 * @since 2026-04-10
 */
@Component
public class UserContextFilter implements Filter {

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
            throws IOException, ServletException {

        HttpServletRequest httpRequest = (HttpServletRequest) request;

        String userId = httpRequest.getHeader("X-USER-ID");
        String username = httpRequest.getHeader("X-USERNAME");
        String tenantId = httpRequest.getHeader("X-TENANT-ID");
        String traceId = httpRequest.getHeader("X-TRACE-ID");

        if (traceId == null || traceId.isEmpty()) {
            traceId = RequestContext.generateTraceId();
        }

        UserContext.UserInfo userInfo = new UserContext.UserInfo(
                userId,
                username,
                List.of("admin", "user"),
                List.of("user:read", "user:write")
        );

        String finalTraceId = traceId;
        String finalTenantId = tenantId;
        String finalUserId = userId;

        ScopedValue.where(UserContext.USER, userInfo)
                .where(RequestContext.USER_ID, finalUserId)
                .where(TenantContext.TENANT_ID, finalTenantId)
                .where(RequestContext.TRACE_ID, finalTraceId)
                .run(() -> {
                    try {
                        chain.doFilter(request, response);
                    } catch (IOException | ServletException e) {
                        throw new RuntimeException(e);
                    }
                });
    }
}
```

权限业务服务（统一获取登录用户信息）

```java
package io.github.atengk.service;

import io.github.atengk.context.UserContext;
import io.github.atengk.util.LogUtil;
import org.springframework.stereotype.Service;

/**
 * 权限与用户业务示例
 *
 * 演示登录态信息统一获取（角色 / 权限 / 用户）
 *
 * @author Ateng
 * @since 2026-04-10
 */
@Service
public class PermissionService {

    /**
     * 权限校验示例
     */
    public void checkPermission() {

        UserContext.UserInfo user = UserContext.getUser();

        LogUtil.info("当前用户：" + user.username()
                + ", roles=" + user.roles()
                + ", permissions=" + user.permissions());

        // 示例：权限判断
        if (!user.permissions().contains("user:write")) {
            throw new RuntimeException("无权限访问");
        }

        LogUtil.info("权限校验通过");
    }
}
```

控制层（curl 调用示例）

```java
package io.github.atengk.controller;

import io.github.atengk.service.PermissionService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 权限控制器示例
 *
 * @author Ateng
 * @since 2026-04-10
 */
@RestController
public class PermissionController {

    private final PermissionService permissionService;

    public PermissionController(PermissionService permissionService) {
        this.permissionService = permissionService;
    }

    /**
     * 登录态与权限测试接口
     *
     * curl -H "X-USER-ID: u100" -H "X-USERNAME: tom" -H "X-TENANT-ID: t001" -H "X-TRACE-ID: abc123" http://localhost:10005/permission/check
     *
     * @return 结果
     */
    @GetMapping("/permission/check")
    public String check() {
        permissionService.checkPermission();
        return "ok";
    }
}
```

日志输出示例（效果）

```text
[INFO] [traceId=abc123] 当前用户：tom, roles=[admin, user], permissions=[user:read, user:write]
[INFO] [traceId=abc123] 权限校验通过
```

