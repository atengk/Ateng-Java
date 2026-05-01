# 设计模式：代理模式

代理模式用于为目标对象提供一个代理对象，由代理对象控制对目标对象的访问，并在访问前后增加额外逻辑。在 JDK21 和 Spring Boot 3 项目中，代理模式常用于权限校验、日志记录、事务控制、远程调用、缓存控制、延迟加载、接口限流、方法耗时统计、异常包装等场景。

需要注意：代理模式关注的是“控制访问”。如果重点是给对象动态叠加多个增强能力，装饰器模式更合适；如果重点是根据业务类型切换不同实现，策略模式更合适；如果重点是通过代理对象统一拦截调用，代理模式更合适。

## 基础配置

本示例基于 JDK21、Spring Boot 3、Maven 项目。示例包路径统一使用 `io.github.atengk`。

文件位置：`pom.xml`

```xml
<dependencies>
    <!-- Spring Boot Web，用于提供接口验证代理模式行为 -->
    <dependency>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-starter-web</artifactId>
    </dependency>

    <!-- Spring Boot AOP，用于演示 Spring 代理机制 -->
    <dependency>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-starter-aop</artifactId>
    </dependency>

    <!-- Hutool 工具类，用于字符串、ID、计时器、日期等通用处理 -->
    <dependency>
        <groupId>cn.hutool</groupId>
        <artifactId>hutool-all</artifactId>
        <version>5.8.27</version>
    </dependency>

    <!-- Lombok，简化日志对象、构造方法等样板代码 -->
    <dependency>
        <groupId>org.projectlombok</groupId>
        <artifactId>lombok</artifactId>
        <optional>true</optional>
    </dependency>

    <!-- Spring Boot 测试依赖，用于单元测试验证 -->
    <dependency>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-starter-test</artifactId>
        <scope>test</scope>
    </dependency>
</dependencies>
```

如果项目使用 Spring Boot 3，建议使用 JDK17 及以上版本。当前文档以 JDK21 为基准，示例代码可以直接用于 Spring Boot 3 项目。

## 核心概念

代理模式的核心目标是让调用方不直接访问目标对象，而是通过代理对象访问目标对象。代理对象可以在调用前后加入控制逻辑。

常见角色如下：

| 角色     | 说明                                   |
| -------- | -------------------------------------- |
| 抽象主题 | 定义目标对象和代理对象共同实现的接口   |
| 真实主题 | 目标对象，负责核心业务逻辑             |
| 代理对象 | 持有真实主题引用，控制对真实主题的访问 |
| 调用方   | 面向抽象主题调用，不直接依赖真实主题   |

常见代理方式如下：

| 实现方式        | 是否推荐         | 适用场景                                             |
| --------------- | ---------------- | ---------------------------------------------------- |
| 静态代理        | 推荐用于理解原理 | 代理类手写，结构直观                                 |
| JDK 动态代理    | 推荐用于接口代理 | 目标对象实现了接口                                   |
| CGLIB 代理      | Spring 常用      | 目标对象没有接口时通过子类代理                       |
| Spring AOP 代理 | 强烈推荐         | Spring Boot 项目中的日志、权限、事务、监控等横切逻辑 |
| 直接修改目标类  | 不推荐           | 容易污染核心业务逻辑                                 |

在 Spring Boot 项目中，常见优先级通常是：

```text
Spring AOP 代理 > JDK 动态代理 > 静态代理
```

静态代理适合理解模式结构；JDK 动态代理适合框架级接口增强；Spring AOP 代理适合真实业务项目中的横切增强。

## 静态代理

静态代理是最直观的代理实现方式。代理类和目标类实现同一个接口，代理类持有目标类对象，并在调用前后增加控制逻辑。

下面以文档下载为例，真实服务只负责下载文档，代理服务负责权限校验和访问日志。

### 文件结构

```text
src/main/java/io/github/atengk/design/proxy/staticproxy/
├── DocumentDownloadService.java
├── RealDocumentDownloadService.java
└── DocumentDownloadServiceProxy.java
```

文件位置：`src/main/java/io/github/atengk/design/proxy/staticproxy/DocumentDownloadService.java`

下面是文档下载服务接口，真实对象和代理对象都实现该接口。

```java
package io.github.atengk.design.proxy.staticproxy;

/**
 * 文档下载服务
 *
 * @author Ateng
 * @since 2026-04-30
 */
public interface DocumentDownloadService {

    /**
     * 下载文档
     *
     * @param userId     用户ID
     * @param documentId 文档ID
     * @return 下载结果
     */
    String download(Long userId, Long documentId);
}
```

文件位置：`src/main/java/io/github/atengk/design/proxy/staticproxy/RealDocumentDownloadService.java`

下面是真实文档下载服务，只负责核心下载逻辑。

```java
package io.github.atengk.design.proxy.staticproxy;

import lombok.extern.slf4j.Slf4j;

/**
 * 真实文档下载服务
 *
 * @author Ateng
 * @since 2026-04-30
 */
@Slf4j
public class RealDocumentDownloadService implements DocumentDownloadService {

    /**
     * 下载文档
     *
     * @param userId     用户ID
     * @param documentId 文档ID
     * @return 下载结果
     */
    @Override
    public String download(Long userId, Long documentId) {
        log.info("执行文档下载，用户ID：{}，文档ID：{}", userId, documentId);
        return "文档下载成功，documentId=" + documentId;
    }
}
```

文件位置：`src/main/java/io/github/atengk/design/proxy/staticproxy/DocumentDownloadServiceProxy.java`

下面是静态代理类，用于在下载前做权限校验，并记录访问日志。

```java
package io.github.atengk.design.proxy.staticproxy;

import cn.hutool.core.util.ObjectUtil;
import lombok.extern.slf4j.Slf4j;

/**
 * 文档下载服务静态代理
 *
 * @author Ateng
 * @since 2026-04-30
 */
@Slf4j
public class DocumentDownloadServiceProxy implements DocumentDownloadService {

    private final DocumentDownloadService target;

    /**
     * 创建文档下载服务代理
     *
     * @param target 目标文档下载服务
     */
    public DocumentDownloadServiceProxy(DocumentDownloadService target) {
        if (target == null) {
            throw new IllegalArgumentException("目标文档下载服务不能为空");
        }
        this.target = target;
    }

    /**
     * 下载文档
     *
     * @param userId     用户ID
     * @param documentId 文档ID
     * @return 下载结果
     */
    @Override
    public String download(Long userId, Long documentId) {
        validateRequest(userId, documentId);
        checkPermission(userId, documentId);

        log.info("文档下载代理开始执行，用户ID：{}，文档ID：{}", userId, documentId);
        String result = target.download(userId, documentId);
        log.info("文档下载代理执行完成，用户ID：{}，文档ID：{}，结果：{}", userId, documentId, result);

        return result;
    }

    /**
     * 校验请求参数
     *
     * @param userId     用户ID
     * @param documentId 文档ID
     */
    private void validateRequest(Long userId, Long documentId) {
        if (ObjectUtil.hasNull(userId, documentId) || userId <= 0 || documentId <= 0) {
            log.warn("文档下载参数校验失败，用户ID：{}，文档ID：{}", userId, documentId);
            throw new IllegalArgumentException("用户ID和文档ID必须大于0");
        }
    }

    /**
     * 校验访问权限
     *
     * @param userId     用户ID
     * @param documentId 文档ID
     */
    private void checkPermission(Long userId, Long documentId) {
        log.info("校验文档下载权限，用户ID：{}，文档ID：{}", userId, documentId);
    }
}
```

使用方式：

```java
DocumentDownloadService downloadService = new DocumentDownloadServiceProxy(
        new RealDocumentDownloadService()
);

String result = downloadService.download(10001L, 20001L);
```

静态代理的优点是结构清晰，容易理解。缺点是每个被代理接口都需要手写代理类，接口方法较多时维护成本高。

## JDK 动态代理

JDK 动态代理通过 `Proxy` 和 `InvocationHandler` 在运行时生成代理对象。它要求目标对象至少实现一个接口，适合对接口方法做统一增强。

下面以用户查询服务为例，通过动态代理统一记录方法耗时和异常日志。

### 文件结构

```text
src/main/java/io/github/atengk/design/proxy/jdk/
├── UserQueryService.java
├── UserQueryServiceImpl.java
├── LogInvocationHandler.java
└── JdkProxyFactory.java
```

文件位置：`src/main/java/io/github/atengk/design/proxy/jdk/UserQueryService.java`

下面是用户查询服务接口。

```java
package io.github.atengk.design.proxy.jdk;

/**
 * 用户查询服务
 *
 * @author Ateng
 * @since 2026-04-30
 */
public interface UserQueryService {

    /**
     * 根据用户ID查询用户名
     *
     * @param userId 用户ID
     * @return 用户名
     */
    String getUsername(Long userId);
}
```

文件位置：`src/main/java/io/github/atengk/design/proxy/jdk/UserQueryServiceImpl.java`

下面是用户查询服务实现类。

```java
package io.github.atengk.design.proxy.jdk;

import cn.hutool.core.util.ObjectUtil;
import lombok.extern.slf4j.Slf4j;

/**
 * 用户查询服务实现
 *
 * @author Ateng
 * @since 2026-04-30
 */
@Slf4j
public class UserQueryServiceImpl implements UserQueryService {

    /**
     * 根据用户ID查询用户名
     *
     * @param userId 用户ID
     * @return 用户名
     */
    @Override
    public String getUsername(Long userId) {
        if (ObjectUtil.isNull(userId) || userId <= 0) {
            log.warn("查询用户名失败，用户ID不合法，用户ID：{}", userId);
            throw new IllegalArgumentException("用户ID必须大于0");
        }

        log.info("查询用户名，用户ID：{}", userId);
        return "Ateng-" + userId;
    }
}
```

文件位置：`src/main/java/io/github/atengk/design/proxy/jdk/LogInvocationHandler.java`

下面是动态代理调用处理器，统一处理方法调用前后的日志和耗时统计。

```java
package io.github.atengk.design.proxy.jdk;

import cn.hutool.core.date.TimeInterval;
import cn.hutool.core.util.StrUtil;
import lombok.extern.slf4j.Slf4j;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

/**
 * 日志调用处理器
 *
 * @author Ateng
 * @since 2026-04-30
 */
@Slf4j
public class LogInvocationHandler implements InvocationHandler {

    private final Object target;

    /**
     * 创建日志调用处理器
     *
     * @param target 目标对象
     */
    public LogInvocationHandler(Object target) {
        if (target == null) {
            throw new IllegalArgumentException("目标对象不能为空");
        }
        this.target = target;
    }

    /**
     * 执行代理调用
     *
     * @param proxy  代理对象
     * @param method 被调用方法
     * @param args   方法参数
     * @return 方法返回值
     * @throws Throwable 调用异常
     */
    @Override
    public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
        TimeInterval timer = new TimeInterval();
        String methodName = StrUtil.format("{}.{}", target.getClass().getSimpleName(), method.getName());

        try {
            log.info("开始执行代理方法，方法：{}", methodName);
            Object result = method.invoke(target, args);
            log.info("代理方法执行成功，方法：{}，耗时：{}ms", methodName, timer.interval());
            return result;
        } catch (InvocationTargetException exception) {
            Throwable targetException = exception.getTargetException();
            log.warn("代理方法执行失败，方法：{}，异常：{}", methodName, targetException.getMessage());
            throw targetException;
        }
    }
}
```

文件位置：`src/main/java/io/github/atengk/design/proxy/jdk/JdkProxyFactory.java`

下面是 JDK 动态代理工厂，用于创建接口代理对象。

```java
package io.github.atengk.design.proxy.jdk;

import java.lang.reflect.Proxy;

/**
 * JDK动态代理工厂
 *
 * @author Ateng
 * @since 2026-04-30
 */
public class JdkProxyFactory {

    private JdkProxyFactory() {
    }

    /**
     * 创建代理对象
     *
     * @param target        目标对象
     * @param interfaceType 接口类型
     * @param <T>           接口泛型
     * @return 代理对象
     */
    public static <T> T createProxy(Object target, Class<T> interfaceType) {
        Object proxy = Proxy.newProxyInstance(
                interfaceType.getClassLoader(),
                new Class[]{interfaceType},
                new LogInvocationHandler(target)
        );

        return interfaceType.cast(proxy);
    }
}
```

使用方式：

```java
UserQueryService userQueryService = JdkProxyFactory.createProxy(
        new UserQueryServiceImpl(),
        UserQueryService.class
);

String username = userQueryService.getUsername(10001L);
```

JDK 动态代理的优点是不需要为每个接口手写代理类。缺点是只能代理接口方法，不能直接代理没有接口的普通类。

## Spring Boot AOP 代理

Spring Boot 项目中最常见的代理模式实践是 Spring AOP。Spring AOP 会为符合切点规则的 Bean 创建代理对象，调用方注入的通常不是原始对象，而是代理对象。

下面以订单支付为例，业务服务只负责支付逻辑，AOP 代理统一做访问日志、耗时统计和异常记录。

### 文件结构

```text
src/main/java/io/github/atengk/design/
├── ProxyApplication.java
├── annotation/
│   └── AccessLog.java
├── aspect/
│   └── AccessLogAspect.java
├── controller/
│   └── OrderPayController.java
├── dto/
│   ├── OrderPayRequest.java
│   └── OrderPayResponse.java
└── service/
    ├── OrderPayService.java
    └── impl/
        └── OrderPayServiceImpl.java
```

文件位置：`src/main/java/io/github/atengk/design/ProxyApplication.java`

下面是 Spring Boot 启动类。

```java
package io.github.atengk.design;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * 代理模式示例启动类
 *
 * @author Ateng
 * @since 2026-04-30
 */
@SpringBootApplication
public class ProxyApplication {

    /**
     * 应用启动入口
     *
     * @param args 启动参数
     */
    public static void main(String[] args) {
        SpringApplication.run(ProxyApplication.class, args);
    }
}
```

文件位置：`src/main/java/io/github/atengk/design/annotation/AccessLog.java`

下面是访问日志注解，用于标记需要被 AOP 代理增强的方法。

```java
package io.github.atengk.design.annotation;

import java.lang.annotation.*;

/**
 * 访问日志注解
 *
 * @author Ateng
 * @since 2026-04-30
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface AccessLog {

    /**
     * 业务名称
     *
     * @return 业务名称
     */
    String value();
}
```

文件位置：`src/main/java/io/github/atengk/design/aspect/AccessLogAspect.java`

下面是访问日志切面。它通过代理方式拦截带有 `@AccessLog` 的方法，并在方法执行前后记录日志。

```java
package io.github.atengk.design.aspect;

import cn.hutool.core.date.TimeInterval;
import cn.hutool.core.util.ArrayUtil;
import io.github.atengk.design.annotation.AccessLog;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.stereotype.Component;

import java.lang.reflect.Method;

/**
 * 访问日志切面
 *
 * @author Ateng
 * @since 2026-04-30
 */
@Slf4j
@Aspect
@Component
public class AccessLogAspect {

    /**
     * 环绕增强访问日志
     *
     * @param joinPoint 连接点
     * @return 方法返回值
     * @throws Throwable 执行异常
     */
    @Around("@annotation(io.github.atengk.design.annotation.AccessLog)")
    public Object around(ProceedingJoinPoint joinPoint) throws Throwable {
        TimeInterval timer = new TimeInterval();
        Method method = ((MethodSignature) joinPoint.getSignature()).getMethod();
        AccessLog accessLog = method.getAnnotation(AccessLog.class);

        String methodName = method.getDeclaringClass().getSimpleName() + "." + method.getName();
        Object[] args = joinPoint.getArgs();

        try {
            log.info("接口访问开始，业务：{}，方法：{}，参数数量：{}",
                    accessLog.value(), methodName, ArrayUtil.length(args));

            Object result = joinPoint.proceed();

            log.info("接口访问成功，业务：{}，方法：{}，耗时：{}ms",
                    accessLog.value(), methodName, timer.interval());

            return result;
        } catch (Throwable throwable) {
            log.warn("接口访问失败，业务：{}，方法：{}，耗时：{}ms，异常：{}",
                    accessLog.value(), methodName, timer.interval(), throwable.getMessage());
            throw throwable;
        }
    }
}
```

文件位置：`src/main/java/io/github/atengk/design/dto/OrderPayRequest.java`

下面是订单支付请求参数对象。

```java
package io.github.atengk.design.dto;

import java.math.BigDecimal;

/**
 * 订单支付请求
 *
 * @param orderNo 订单号
 * @param userId  用户ID
 * @param amount  支付金额
 * @author Ateng
 * @since 2026-04-30
 */
public record OrderPayRequest(String orderNo, Long userId, BigDecimal amount) {
}
```

文件位置：`src/main/java/io/github/atengk/design/dto/OrderPayResponse.java`

下面是订单支付响应结果。

```java
package io.github.atengk.design.dto;

/**
 * 订单支付响应
 *
 * @param orderNo 订单号
 * @param payNo   支付流水号
 * @param message 结果消息
 * @author Ateng
 * @since 2026-04-30
 */
public record OrderPayResponse(String orderNo, String payNo, String message) {
}
```

文件位置：`src/main/java/io/github/atengk/design/service/OrderPayService.java`

下面是订单支付服务接口。

```java
package io.github.atengk.design.service;

import io.github.atengk.design.dto.OrderPayRequest;
import io.github.atengk.design.dto.OrderPayResponse;

/**
 * 订单支付服务
 *
 * @author Ateng
 * @since 2026-04-30
 */
public interface OrderPayService {

    /**
     * 支付订单
     *
     * @param request 订单支付请求
     * @return 订单支付响应
     */
    OrderPayResponse pay(OrderPayRequest request);
}
```

文件位置：`src/main/java/io/github/atengk/design/service/impl/OrderPayServiceImpl.java`

下面是订单支付服务实现类。业务方法只关注核心支付逻辑，访问日志由代理切面处理。

```java
package io.github.atengk.design.service.impl;

import cn.hutool.core.util.IdUtil;
import cn.hutool.core.util.StrUtil;
import io.github.atengk.design.annotation.AccessLog;
import io.github.atengk.design.dto.OrderPayRequest;
import io.github.atengk.design.dto.OrderPayResponse;
import io.github.atengk.design.service.OrderPayService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;

/**
 * 订单支付服务实现
 *
 * @author Ateng
 * @since 2026-04-30
 */
@Slf4j
@Service
public class OrderPayServiceImpl implements OrderPayService {

    /**
     * 支付订单
     *
     * @param request 订单支付请求
     * @return 订单支付响应
     */
    @Override
    @AccessLog("订单支付")
    public OrderPayResponse pay(OrderPayRequest request) {
        validateRequest(request);

        String payNo = "PAY" + IdUtil.getSnowflakeNextId();
        log.info("执行订单支付，订单号：{}，用户ID：{}，金额：{}，支付流水号：{}",
                request.orderNo(), request.userId(), request.amount(), payNo);

        return new OrderPayResponse(request.orderNo(), payNo, "支付成功");
    }

    /**
     * 校验订单支付请求
     *
     * @param request 订单支付请求
     */
    private void validateRequest(OrderPayRequest request) {
        if (request == null) {
            log.warn("订单支付失败，请求参数为空");
            throw new IllegalArgumentException("请求参数不能为空");
        }

        if (StrUtil.isBlank(request.orderNo())) {
            log.warn("订单支付失败，订单号为空");
            throw new IllegalArgumentException("订单号不能为空");
        }

        if (request.userId() == null || request.userId() <= 0) {
            log.warn("订单支付失败，用户ID不合法，用户ID：{}", request.userId());
            throw new IllegalArgumentException("用户ID必须大于0");
        }

        if (request.amount() == null || request.amount().compareTo(BigDecimal.ZERO) <= 0) {
            log.warn("订单支付失败，支付金额不合法，金额：{}", request.amount());
            throw new IllegalArgumentException("支付金额必须大于0");
        }
    }
}
```

文件位置：`src/main/java/io/github/atengk/design/controller/OrderPayController.java`

下面是订单支付接口，用于验证 Spring AOP 代理效果。

```java
package io.github.atengk.design.controller;

import io.github.atengk.design.dto.OrderPayRequest;
import io.github.atengk.design.dto.OrderPayResponse;
import io.github.atengk.design.service.OrderPayService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;

/**
 * 订单支付控制器
 *
 * @author Ateng
 * @since 2026-04-30
 */
@RestController
@RequiredArgsConstructor
@RequestMapping("/proxy/order")
public class OrderPayController {

    private final OrderPayService orderPayService;

    /**
     * 支付订单
     *
     * @param orderNo 订单号
     * @param userId  用户ID
     * @param amount  支付金额
     * @return 订单支付响应
     */
    @PostMapping("/pay")
    public OrderPayResponse pay(@RequestParam String orderNo,
                                @RequestParam Long userId,
                                @RequestParam BigDecimal amount) {
        OrderPayRequest request = new OrderPayRequest(orderNo, userId, amount);
        return orderPayService.pay(request);
    }
}
```

接口调用示例：

```bash
curl -X POST "http://localhost:8080/proxy/order/pay?orderNo=ORDER10001&userId=10001&amount=99.90"
```

可能返回：

```json
{
  "orderNo": "ORDER10001",
  "payNo": "PAY2019776866538487808",
  "message": "支付成功"
}
```

如果 AOP 代理正常，可以看到类似日志：

```text
接口访问开始，业务：订单支付，方法：OrderPayServiceImpl.pay，参数数量：1
执行订单支付，订单号：ORDER10001，用户ID：10001，金额：99.90，支付流水号：PAY2019776866538487808
接口访问成功，业务：订单支付，方法：OrderPayServiceImpl.pay，耗时：8ms
```

这种方式的优点是业务代码和横切逻辑分离。订单支付服务不需要关心日志增强逻辑，Spring 代理会在运行时完成拦截。

## CGLIB 代理

CGLIB 代理通过生成目标类的子类来实现代理，因此不要求目标对象实现接口。Spring AOP 在某些场景下会使用 CGLIB 代理，例如目标类没有接口，或者显式配置使用类代理。

在 Spring Boot 项目中可以通过配置强制使用 CGLIB 代理。

文件位置：`src/main/resources/application.yml`

```yaml
spring:
  aop:
    # true 表示优先使用 CGLIB 基于类创建代理
    proxy-target-class: true
```

需要注意：CGLIB 通过继承生成代理子类，所以不能代理 `final` 类，也不能代理 `final` 方法。

示例：

```java
public final class FinalOrderService {
    public String pay() {
        return "success";
    }
}
```

这种类无法被 CGLIB 正常代理，因为 `final` 类不能被继承。

在 Spring Boot 业务项目中，一般不需要手动使用 CGLIB API。多数情况下只需要使用 Spring AOP，由 Spring 根据目标对象情况选择代理方式。

## 代理模式和装饰器模式的区别

代理模式和装饰器模式结构相似，都是持有一个同接口对象，然后转发调用。但二者的意图不同。

| 对比项   | 代理模式                                      | 装饰器模式                               |
| -------- | --------------------------------------------- | ---------------------------------------- |
| 核心目的 | 控制访问目标对象                              | 增强目标对象能力                         |
| 关注点   | 访问控制、远程调用、延迟加载、权限、事务      | 日志、审计、缓存、重试、脱敏、加密、限流 |
| 调用关系 | 通常代理一个目标对象                          | 可以多层叠加多个装饰器                   |
| 典型应用 | Spring AOP、RPC 代理、Mapper 代理、懒加载代理 | 服务增强链、IO 流增强、业务能力叠加      |
| 目标对象 | 可能被隐藏或延迟创建                          | 通常明确存在并被增强                     |

简单理解：

```text
代理模式：你不能直接访问目标对象，需要通过代理对象访问。
装饰器模式：目标对象可以直接用，但我给它额外加一些能力。
```

在 Spring Boot 中，AOP、事务、MyBatis Mapper 接口、Feign 客户端都体现了代理模式思想。手写服务增强链更接近装饰器模式。

## Spring AOP 代理的常见失效场景

Spring AOP 是基于代理对象实现的，因此调用是否经过代理对象非常关键。

最常见的失效场景是类内部方法自调用。

错误示例：

```java
@Service
public class OrderService {

    public void createOrder() {
        // 内部直接调用，不经过 Spring 代理对象
        this.payOrder();
    }

    @AccessLog("支付订单")
    public void payOrder() {
        // 这里的 AOP 可能不会生效
    }
}
```

原因是 `this.payOrder()` 调用的是当前对象方法，不是 Spring 容器中的代理对象。

推荐做法之一是拆分到另一个 Spring Bean 中：

```java
@Service
@RequiredArgsConstructor
public class OrderService {

    private final OrderPayService orderPayService;

    public void createOrder() {
        orderPayService.payOrder();
    }
}
```

被调用服务：

```java
@Service
public class OrderPayService {

    @AccessLog("支付订单")
    public void payOrder() {
        // 通过 Spring Bean 调用时，AOP 可以生效
    }
}
```

实际项目中，事务注解 `@Transactional` 的自调用失效也是同一个原因。

## 验证方式

启动 Spring Boot 项目：

```bash
mvn spring-boot:run
```

执行订单支付：

```bash
curl -X POST "http://localhost:8080/proxy/order/pay?orderNo=ORDER10001&userId=10001&amount=99.90"
```

如果代理模式生效，可以看到访问日志切面先执行，业务方法再执行，最后切面记录执行成功日志。

正常日志示例：

```text
接口访问开始，业务：订单支付，方法：OrderPayServiceImpl.pay，参数数量：1
执行订单支付，订单号：ORDER10001，用户ID：10001，金额：99.90，支付流水号：PAY2019776866538487808
接口访问成功，业务：订单支付，方法：OrderPayServiceImpl.pay，耗时：8ms
```

异常请求示例：

```bash
curl -X POST "http://localhost:8080/proxy/order/pay?orderNo=ORDER10001&userId=10001&amount=0"
```

异常日志示例：

```text
接口访问开始，业务：订单支付，方法：OrderPayServiceImpl.pay，参数数量：1
订单支付失败，支付金额不合法，金额：0
接口访问失败，业务：订单支付，方法：OrderPayServiceImpl.pay，耗时：3ms，异常：支付金额必须大于0
```

实际项目中建议结合全局异常处理器，将业务异常转换成统一响应结构。

## 注意事项

代理模式适合控制访问，但不要把所有业务逻辑都放进代理层。代理层应该处理权限、日志、事务、缓存、远程调用、懒加载、限流等访问控制或横切逻辑，核心业务仍然应该放在真实主题对象中。

不推荐把代理类写成这样：

```java
@Override
public String download(Long userId, Long documentId) {
    // 权限校验
    // 业务查询
    // 数据计算
    // 文件生成
    // 审计日志
    // 消息发送
    return "success";
}
```

推荐将核心逻辑放在真实对象中，代理对象只负责访问控制：

```java
@Override
public String download(Long userId, Long documentId) {
    checkPermission(userId, documentId);
    return target.download(userId, documentId);
}
```

使用 JDK 动态代理时，目标类必须实现接口，否则无法创建接口代理。

错误示例：

```java
public class UserQueryServiceImpl {
    public String getUsername(Long userId) {
        return "Ateng";
    }
}
```

推荐抽象接口：

```java
public interface UserQueryService {
    String getUsername(Long userId);
}
```

Spring AOP 中，不要依赖 `final` 类或 `final` 方法做增强。尤其在强制使用 CGLIB 代理时，`final` 会阻止子类代理。

错误示例：

```java
@Service
public final class OrderPayServiceImpl {

    @AccessLog("订单支付")
    public final void pay() {
    }
}
```

Spring AOP 只对 Spring 容器管理的 Bean 生效。手动 `new` 出来的对象不会被 Spring 创建代理。

错误示例：

```java
OrderPayService orderPayService = new OrderPayServiceImpl();
orderPayService.pay(request);
```

推荐通过 Spring 注入：

```java
@RequiredArgsConstructor
@RestController
public class OrderPayController {

    private final OrderPayService orderPayService;
}
```

如果代理逻辑涉及分布式缓存、分布式锁、远程调用或数据库事务，需要结合 Redis、数据库唯一约束、事务传播行为、超时控制和异常补偿机制，不能只依赖本地代理逻辑保证业务一致性。

## 总结

在 JDK21 和 Spring Boot 3 项目中，代理模式的实践重点是通过代理对象控制目标对象访问，并把横切逻辑从核心业务中拆出来。

静态代理适合理解代理模式结构。JDK 动态代理适合接口级统一增强。Spring AOP 代理适合业务项目中的日志、权限、事务、监控、异常记录等横切能力。对于 Spring Boot 项目，推荐优先使用 Spring AOP 或框架内置代理机制，而不是手写大量代理类。

代理模式不是为了替代业务分层，也不是为了把所有逻辑都放到代理对象中。它更适合在访问目标对象之前或之后，统一增加控制逻辑，让真实业务对象保持清晰、稳定和可维护。
