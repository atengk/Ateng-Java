# 设计模式：解释器模式

解释器模式用于为某种简单语言、规则表达式或 DSL 定义语法表示，并通过解释器解释执行。在 JDK21 和 Spring Boot 3 项目中，解释器模式常用于权限表达式、优惠规则表达式、搜索条件解析、告警规则、数据过滤规则、配置规则、流程条件判断、简单脚本规则等场景。

需要注意：解释器模式关注的是“定义一套语法，并解释这套语法”。如果只是根据类型选择不同算法，更适合策略模式；如果是多个处理器顺序处理请求，更适合责任链模式；如果规则非常复杂，建议使用成熟规则引擎或表达式引擎，而不是手写复杂解释器。

## 基础配置

本示例基于 JDK21、Spring Boot 3、Maven 项目。示例包路径统一使用 `io.github.atengk`。

文件位置：`pom.xml`

```xml
<dependencies>
    <!-- Spring Boot Web，用于提供接口验证解释器模式行为 -->
    <dependency>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-starter-web</artifactId>
    </dependency>

    <!-- Hutool 工具类，用于字符串、集合、数字等通用处理 -->
    <dependency>
        <groupId>cn.hutool</groupId>
        <artifactId>hutool-all</artifactId>
        <version>5.8.27</version>
    </dependency>

    <!-- Lombok，简化日志对象、Getter、构造方法等样板代码 -->
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

解释器模式的核心目标是把一个规则表达式拆解成语法树，每个语法节点都知道如何解释自己，最终由整棵语法树解释出结果。

常见角色如下：

| 角色                  | 说明                           |
| --------------------- | ------------------------------ |
| AbstractExpression    | 抽象表达式，定义解释方法       |
| TerminalExpression    | 终结符表达式，表示最小语法单元 |
| NonTerminalExpression | 非终结符表达式，组合多个表达式 |
| Context               | 上下文，保存解释时需要的数据   |
| Client                | 调用方，构建表达式并触发解释   |

典型结构如下：

```text
表达式：amount >= 100 AND userLevel == VIP

语法树：
AND
├── amount >= 100
└── userLevel == VIP
```

解释器模式通常适合语法简单、规则稳定、表达式数量较多的场景。如果语法越来越复杂，例如支持函数、变量、数组、嵌套对象、类型推导、短路求值、脚本执行等，就应该优先考虑成熟工具，例如 Spring Expression Language、Aviator、MVEL、Drools 等。

在 Spring Boot 项目中，常见优先级通常是：

```text
简单规则自定义解释器 > SpEL / Aviator 等成熟表达式引擎 > 手写复杂脚本解释器
```

解释器模式的实践重点不是“写一个万能脚本语言”，而是为业务中有限、稳定、可控的规则表达式提供清晰解释能力。

## 普通 Java 解释器模式

普通 Java 解释器适合不依赖 Spring 容器的简单规则判断。下面以订单优惠规则为例，系统需要判断订单是否满足优惠条件。

示例规则如下：

```text
订单金额 >= 100，并且用户等级是 VIP
```

对应表达式结构如下：

```text
AndExpression
├── AmountGreaterEqualExpression
└── UserLevelEqualsExpression
```

### 文件结构

```text
src/main/java/io/github/atengk/design/interpreter/simple/
├── DiscountContext.java
├── DiscountExpression.java
├── AmountGreaterEqualExpression.java
├── UserLevelEqualsExpression.java
├── ChannelEqualsExpression.java
├── AndExpression.java
├── OrExpression.java
└── NotExpression.java
```

文件位置：`src/main/java/io/github/atengk/design/interpreter/simple/DiscountContext.java`

下面是优惠规则解释上下文，保存订单金额、用户等级和下单渠道。

```java
package io.github.atengk.design.interpreter.simple;

import java.math.BigDecimal;

/**
 * 优惠规则上下文
 *
 * @param amount    订单金额
 * @param userLevel 用户等级
 * @param channel   下单渠道
 * @author Ateng
 * @since 2026-04-30
 */
public record DiscountContext(
        BigDecimal amount,
        String userLevel,
        String channel
) {
}
```

文件位置：`src/main/java/io/github/atengk/design/interpreter/simple/DiscountExpression.java`

下面是优惠表达式接口，所有规则表达式都实现该接口。

```java
package io.github.atengk.design.interpreter.simple;

/**
 * 优惠规则表达式
 *
 * @author Ateng
 * @since 2026-04-30
 */
public interface DiscountExpression {

    /**
     * 解释表达式
     *
     * @param context 优惠规则上下文
     * @return true 表示满足规则，false 表示不满足
     */
    boolean interpret(DiscountContext context);
}
```

文件位置：`src/main/java/io/github/atengk/design/interpreter/simple/AmountGreaterEqualExpression.java`

下面是订单金额大于等于指定金额的终结符表达式。

```java
package io.github.atengk.design.interpreter.simple;

import lombok.extern.slf4j.Slf4j;

import java.math.BigDecimal;

/**
 * 订单金额大于等于表达式
 *
 * @author Ateng
 * @since 2026-04-30
 */
@Slf4j
public class AmountGreaterEqualExpression implements DiscountExpression {

    private final BigDecimal thresholdAmount;

    /**
     * 创建订单金额大于等于表达式
     *
     * @param thresholdAmount 阈值金额
     */
    public AmountGreaterEqualExpression(BigDecimal thresholdAmount) {
        if (thresholdAmount == null || thresholdAmount.compareTo(BigDecimal.ZERO) < 0) {
            throw new IllegalArgumentException("阈值金额不能小于0");
        }

        this.thresholdAmount = thresholdAmount;
    }

    /**
     * 解释表达式
     *
     * @param context 优惠规则上下文
     * @return true 表示订单金额大于等于阈值
     */
    @Override
    public boolean interpret(DiscountContext context) {
        if (context == null || context.amount() == null) {
            log.warn("解释金额表达式失败，上下文或订单金额为空");
            return false;
        }

        boolean result = context.amount().compareTo(thresholdAmount) >= 0;
        log.info("解释金额表达式，订单金额：{}，阈值金额：{}，结果：{}", context.amount(), thresholdAmount, result);
        return result;
    }
}
```

文件位置：`src/main/java/io/github/atengk/design/interpreter/simple/UserLevelEqualsExpression.java`

下面是用户等级等于指定等级的终结符表达式。

```java
package io.github.atengk.design.interpreter.simple;

import cn.hutool.core.util.StrUtil;
import lombok.extern.slf4j.Slf4j;

/**
 * 用户等级等于表达式
 *
 * @author Ateng
 * @since 2026-04-30
 */
@Slf4j
public class UserLevelEqualsExpression implements DiscountExpression {

    private final String expectedUserLevel;

    /**
     * 创建用户等级等于表达式
     *
     * @param expectedUserLevel 期望用户等级
     */
    public UserLevelEqualsExpression(String expectedUserLevel) {
        if (StrUtil.isBlank(expectedUserLevel)) {
            throw new IllegalArgumentException("期望用户等级不能为空");
        }

        this.expectedUserLevel = expectedUserLevel;
    }

    /**
     * 解释表达式
     *
     * @param context 优惠规则上下文
     * @return true 表示用户等级匹配
     */
    @Override
    public boolean interpret(DiscountContext context) {
        if (context == null || StrUtil.isBlank(context.userLevel())) {
            log.warn("解释用户等级表达式失败，上下文或用户等级为空");
            return false;
        }

        boolean result = StrUtil.equalsIgnoreCase(context.userLevel(), expectedUserLevel);
        log.info("解释用户等级表达式，当前等级：{}，期望等级：{}，结果：{}",
                context.userLevel(), expectedUserLevel, result);
        return result;
    }
}
```

文件位置：`src/main/java/io/github/atengk/design/interpreter/simple/ChannelEqualsExpression.java`

下面是下单渠道等于指定渠道的终结符表达式。

```java
package io.github.atengk.design.interpreter.simple;

import cn.hutool.core.util.StrUtil;
import lombok.extern.slf4j.Slf4j;

/**
 * 下单渠道等于表达式
 *
 * @author Ateng
 * @since 2026-04-30
 */
@Slf4j
public class ChannelEqualsExpression implements DiscountExpression {

    private final String expectedChannel;

    /**
     * 创建下单渠道等于表达式
     *
     * @param expectedChannel 期望下单渠道
     */
    public ChannelEqualsExpression(String expectedChannel) {
        if (StrUtil.isBlank(expectedChannel)) {
            throw new IllegalArgumentException("期望下单渠道不能为空");
        }

        this.expectedChannel = expectedChannel;
    }

    /**
     * 解释表达式
     *
     * @param context 优惠规则上下文
     * @return true 表示渠道匹配
     */
    @Override
    public boolean interpret(DiscountContext context) {
        if (context == null || StrUtil.isBlank(context.channel())) {
            log.warn("解释渠道表达式失败，上下文或渠道为空");
            return false;
        }

        boolean result = StrUtil.equalsIgnoreCase(context.channel(), expectedChannel);
        log.info("解释渠道表达式，当前渠道：{}，期望渠道：{}，结果：{}",
                context.channel(), expectedChannel, result);
        return result;
    }
}
```

文件位置：`src/main/java/io/github/atengk/design/interpreter/simple/AndExpression.java`

下面是逻辑与表达式，用于组合两个子表达式。

```java
package io.github.atengk.design.interpreter.simple;

import lombok.extern.slf4j.Slf4j;

/**
 * 逻辑与表达式
 *
 * @author Ateng
 * @since 2026-04-30
 */
@Slf4j
public class AndExpression implements DiscountExpression {

    private final DiscountExpression leftExpression;
    private final DiscountExpression rightExpression;

    /**
     * 创建逻辑与表达式
     *
     * @param leftExpression  左表达式
     * @param rightExpression 右表达式
     */
    public AndExpression(DiscountExpression leftExpression, DiscountExpression rightExpression) {
        if (leftExpression == null || rightExpression == null) {
            throw new IllegalArgumentException("左右表达式不能为空");
        }

        this.leftExpression = leftExpression;
        this.rightExpression = rightExpression;
    }

    /**
     * 解释表达式
     *
     * @param context 优惠规则上下文
     * @return true 表示两个表达式都满足
     */
    @Override
    public boolean interpret(DiscountContext context) {
        boolean leftResult = leftExpression.interpret(context);
        if (!leftResult) {
            log.info("解释逻辑与表达式，左表达式不满足，短路返回 false");
            return false;
        }

        boolean rightResult = rightExpression.interpret(context);
        boolean result = leftResult && rightResult;

        log.info("解释逻辑与表达式，左结果：{}，右结果：{}，最终结果：{}", leftResult, rightResult, result);
        return result;
    }
}
```

文件位置：`src/main/java/io/github/atengk/design/interpreter/simple/OrExpression.java`

下面是逻辑或表达式，用于组合两个子表达式。

```java
package io.github.atengk.design.interpreter.simple;

import lombok.extern.slf4j.Slf4j;

/**
 * 逻辑或表达式
 *
 * @author Ateng
 * @since 2026-04-30
 */
@Slf4j
public class OrExpression implements DiscountExpression {

    private final DiscountExpression leftExpression;
    private final DiscountExpression rightExpression;

    /**
     * 创建逻辑或表达式
     *
     * @param leftExpression  左表达式
     * @param rightExpression 右表达式
     */
    public OrExpression(DiscountExpression leftExpression, DiscountExpression rightExpression) {
        if (leftExpression == null || rightExpression == null) {
            throw new IllegalArgumentException("左右表达式不能为空");
        }

        this.leftExpression = leftExpression;
        this.rightExpression = rightExpression;
    }

    /**
     * 解释表达式
     *
     * @param context 优惠规则上下文
     * @return true 表示至少一个表达式满足
     */
    @Override
    public boolean interpret(DiscountContext context) {
        boolean leftResult = leftExpression.interpret(context);
        if (leftResult) {
            log.info("解释逻辑或表达式，左表达式满足，短路返回 true");
            return true;
        }

        boolean rightResult = rightExpression.interpret(context);
        boolean result = leftResult || rightResult;

        log.info("解释逻辑或表达式，左结果：{}，右结果：{}，最终结果：{}", leftResult, rightResult, result);
        return result;
    }
}
```

文件位置：`src/main/java/io/github/atengk/design/interpreter/simple/NotExpression.java`

下面是逻辑非表达式，用于对子表达式结果取反。

```java
package io.github.atengk.design.interpreter.simple;

import lombok.extern.slf4j.Slf4j;

/**
 * 逻辑非表达式
 *
 * @author Ateng
 * @since 2026-04-30
 */
@Slf4j
public class NotExpression implements DiscountExpression {

    private final DiscountExpression expression;

    /**
     * 创建逻辑非表达式
     *
     * @param expression 子表达式
     */
    public NotExpression(DiscountExpression expression) {
        if (expression == null) {
            throw new IllegalArgumentException("子表达式不能为空");
        }

        this.expression = expression;
    }

    /**
     * 解释表达式
     *
     * @param context 优惠规则上下文
     * @return true 表示子表达式不满足
     */
    @Override
    public boolean interpret(DiscountContext context) {
        boolean result = !expression.interpret(context);
        log.info("解释逻辑非表达式，最终结果：{}", result);
        return result;
    }
}
```

使用方式：

```java
DiscountExpression expression = new AndExpression(
        new AmountGreaterEqualExpression(BigDecimal.valueOf(100)),
        new OrExpression(
                new UserLevelEqualsExpression("VIP"),
                new ChannelEqualsExpression("APP")
        )
);

DiscountContext context = new DiscountContext(
        BigDecimal.valueOf(199),
        "NORMAL",
        "APP"
);

boolean matched = expression.interpret(context);
```

这段规则表示：

```text
订单金额 >= 100，并且用户等级是 VIP 或下单渠道是 APP
```

最终 `matched` 会返回 `true`。

## Spring Boot 权限表达式解释器

Spring Boot 项目中，解释器模式更常见的用法是解释权限、规则、条件表达式。下面以权限表达式为例，系统支持角色、权限码、部门三种判断，并支持 `&&`、`||`、`!` 和括号。

支持的表达式示例：

```text
role:ADMIN
perm:order:create
dept:100
role:ADMIN && perm:order:create
role:ADMIN && (perm:order:create || perm:order:audit)
!role:GUEST && perm:order:create
```

整体流程如下：

```text
Controller
    -> PermissionExpressionService
        -> PermissionExpressionParser
            -> Expression 语法树
                -> interpret(PermissionContext)
```

### 文件结构

```text
src/main/java/io/github/atengk/design/
├── InterpreterApplication.java
├── controller/
│   └── PermissionExpressionController.java
├── dto/
│   ├── PermissionCheckRequest.java
│   ├── PermissionCheckResponse.java
│   └── PermissionContext.java
├── expression/
│   ├── PermissionExpression.java
│   ├── RoleExpression.java
│   ├── PermissionCodeExpression.java
│   ├── DepartmentExpression.java
│   ├── AndPermissionExpression.java
│   ├── OrPermissionExpression.java
│   └── NotPermissionExpression.java
├── parser/
│   └── PermissionExpressionParser.java
└── service/
    ├── PermissionExpressionService.java
    └── impl/
        └── PermissionExpressionServiceImpl.java
```

文件位置：`src/main/java/io/github/atengk/design/InterpreterApplication.java`

下面是 Spring Boot 启动类。

```java
package io.github.atengk.design;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * 解释器模式示例启动类
 *
 * @author Ateng
 * @since 2026-04-30
 */
@SpringBootApplication
public class InterpreterApplication {

    /**
     * 应用启动入口
     *
     * @param args 启动参数
     */
    public static void main(String[] args) {
        SpringApplication.run(InterpreterApplication.class, args);
    }
}
```

文件位置：`src/main/java/io/github/atengk/design/dto/PermissionContext.java`

下面是权限解释上下文，用于保存当前用户拥有的角色、权限码和部门ID。

```java
package io.github.atengk.design.dto;

import java.util.Set;

/**
 * 权限解释上下文
 *
 * @param userId          用户ID
 * @param roles           用户角色集合
 * @param permissions     用户权限码集合
 * @param departmentIds   用户部门ID集合
 * @author Ateng
 * @since 2026-04-30
 */
public record PermissionContext(
        Long userId,
        Set<String> roles,
        Set<String> permissions,
        Set<Long> departmentIds
) {
}
```

文件位置：`src/main/java/io/github/atengk/design/dto/PermissionCheckRequest.java`

下面是权限表达式校验请求对象。

```java
package io.github.atengk.design.dto;

import java.util.Set;

/**
 * 权限表达式校验请求
 *
 * @param userId        用户ID
 * @param expression    权限表达式
 * @param roles         用户角色集合
 * @param permissions   用户权限码集合
 * @param departmentIds 用户部门ID集合
 * @author Ateng
 * @since 2026-04-30
 */
public record PermissionCheckRequest(
        Long userId,
        String expression,
        Set<String> roles,
        Set<String> permissions,
        Set<Long> departmentIds
) {
}
```

文件位置：`src/main/java/io/github/atengk/design/dto/PermissionCheckResponse.java`

下面是权限表达式校验响应对象。

```java
package io.github.atengk.design.dto;

/**
 * 权限表达式校验响应
 *
 * @param userId     用户ID
 * @param expression 权限表达式
 * @param passed     是否通过
 * @param message    响应消息
 * @author Ateng
 * @since 2026-04-30
 */
public record PermissionCheckResponse(
        Long userId,
        String expression,
        Boolean passed,
        String message
) {
}
```

文件位置：`src/main/java/io/github/atengk/design/expression/PermissionExpression.java`

下面是权限表达式接口，所有权限表达式节点都实现该接口。

```java
package io.github.atengk.design.expression;

import io.github.atengk.design.dto.PermissionContext;

/**
 * 权限表达式
 *
 * @author Ateng
 * @since 2026-04-30
 */
public interface PermissionExpression {

    /**
     * 解释权限表达式
     *
     * @param context 权限解释上下文
     * @return true 表示通过，false 表示不通过
     */
    boolean interpret(PermissionContext context);
}
```

文件位置：`src/main/java/io/github/atengk/design/expression/RoleExpression.java`

下面是角色表达式，用于解释 `role:ADMIN` 这类语法。

```java
package io.github.atengk.design.expression;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.StrUtil;
import io.github.atengk.design.dto.PermissionContext;
import lombok.extern.slf4j.Slf4j;

/**
 * 角色权限表达式
 *
 * @author Ateng
 * @since 2026-04-30
 */
@Slf4j
public class RoleExpression implements PermissionExpression {

    private final String requiredRole;

    /**
     * 创建角色权限表达式
     *
     * @param requiredRole 要求角色
     */
    public RoleExpression(String requiredRole) {
        if (StrUtil.isBlank(requiredRole)) {
            throw new IllegalArgumentException("要求角色不能为空");
        }

        this.requiredRole = requiredRole;
    }

    /**
     * 解释权限表达式
     *
     * @param context 权限解释上下文
     * @return true 表示拥有指定角色
     */
    @Override
    public boolean interpret(PermissionContext context) {
        if (context == null || CollUtil.isEmpty(context.roles())) {
            log.info("解释角色表达式失败，用户角色为空，要求角色：{}", requiredRole);
            return false;
        }

        boolean result = context.roles().stream()
                .anyMatch(role -> StrUtil.equalsIgnoreCase(role, requiredRole));

        log.info("解释角色表达式，用户ID：{}，要求角色：{}，结果：{}", context.userId(), requiredRole, result);
        return result;
    }
}
```

文件位置：`src/main/java/io/github/atengk/design/expression/PermissionCodeExpression.java`

下面是权限码表达式，用于解释 `perm:order:create` 这类语法。

```java
package io.github.atengk.design.expression;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.StrUtil;
import io.github.atengk.design.dto.PermissionContext;
import lombok.extern.slf4j.Slf4j;

/**
 * 权限码表达式
 *
 * @author Ateng
 * @since 2026-04-30
 */
@Slf4j
public class PermissionCodeExpression implements PermissionExpression {

    private final String requiredPermission;

    /**
     * 创建权限码表达式
     *
     * @param requiredPermission 要求权限码
     */
    public PermissionCodeExpression(String requiredPermission) {
        if (StrUtil.isBlank(requiredPermission)) {
            throw new IllegalArgumentException("要求权限码不能为空");
        }

        this.requiredPermission = requiredPermission;
    }

    /**
     * 解释权限表达式
     *
     * @param context 权限解释上下文
     * @return true 表示拥有指定权限码
     */
    @Override
    public boolean interpret(PermissionContext context) {
        if (context == null || CollUtil.isEmpty(context.permissions())) {
            log.info("解释权限码表达式失败，用户权限码为空，要求权限码：{}", requiredPermission);
            return false;
        }

        boolean result = context.permissions().stream()
                .anyMatch(permission -> StrUtil.equalsIgnoreCase(permission, requiredPermission));

        log.info("解释权限码表达式，用户ID：{}，要求权限码：{}，结果：{}",
                context.userId(), requiredPermission, result);
        return result;
    }
}
```

文件位置：`src/main/java/io/github/atengk/design/expression/DepartmentExpression.java`

下面是部门表达式，用于解释 `dept:100` 这类语法。

```java
package io.github.atengk.design.expression;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.NumberUtil;
import cn.hutool.core.util.StrUtil;
import io.github.atengk.design.dto.PermissionContext;
import lombok.extern.slf4j.Slf4j;

/**
 * 部门权限表达式
 *
 * @author Ateng
 * @since 2026-04-30
 */
@Slf4j
public class DepartmentExpression implements PermissionExpression {

    private final Long requiredDepartmentId;

    /**
     * 创建部门权限表达式
     *
     * @param departmentIdText 部门ID文本
     */
    public DepartmentExpression(String departmentIdText) {
        if (StrUtil.isBlank(departmentIdText) || !NumberUtil.isLong(departmentIdText)) {
            throw new IllegalArgumentException("部门ID必须是数字");
        }

        this.requiredDepartmentId = Long.valueOf(departmentIdText);
    }

    /**
     * 解释权限表达式
     *
     * @param context 权限解释上下文
     * @return true 表示属于指定部门
     */
    @Override
    public boolean interpret(PermissionContext context) {
        if (context == null || CollUtil.isEmpty(context.departmentIds())) {
            log.info("解释部门表达式失败，用户部门为空，要求部门ID：{}", requiredDepartmentId);
            return false;
        }

        boolean result = context.departmentIds().contains(requiredDepartmentId);
        log.info("解释部门表达式，用户ID：{}，要求部门ID：{}，结果：{}",
                context.userId(), requiredDepartmentId, result);
        return result;
    }
}
```

文件位置：`src/main/java/io/github/atengk/design/expression/AndPermissionExpression.java`

下面是权限逻辑与表达式。

```java
package io.github.atengk.design.expression;

import io.github.atengk.design.dto.PermissionContext;
import lombok.extern.slf4j.Slf4j;

/**
 * 权限逻辑与表达式
 *
 * @author Ateng
 * @since 2026-04-30
 */
@Slf4j
public class AndPermissionExpression implements PermissionExpression {

    private final PermissionExpression leftExpression;
    private final PermissionExpression rightExpression;

    /**
     * 创建权限逻辑与表达式
     *
     * @param leftExpression  左表达式
     * @param rightExpression 右表达式
     */
    public AndPermissionExpression(PermissionExpression leftExpression, PermissionExpression rightExpression) {
        if (leftExpression == null || rightExpression == null) {
            throw new IllegalArgumentException("左右表达式不能为空");
        }

        this.leftExpression = leftExpression;
        this.rightExpression = rightExpression;
    }

    /**
     * 解释权限表达式
     *
     * @param context 权限解释上下文
     * @return true 表示左右表达式都通过
     */
    @Override
    public boolean interpret(PermissionContext context) {
        boolean leftResult = leftExpression.interpret(context);
        if (!leftResult) {
            log.info("解释权限逻辑与表达式，左表达式不通过，短路返回 false");
            return false;
        }

        boolean rightResult = rightExpression.interpret(context);
        boolean result = leftResult && rightResult;

        log.info("解释权限逻辑与表达式，左结果：{}，右结果：{}，最终结果：{}",
                leftResult, rightResult, result);
        return result;
    }
}
```

文件位置：`src/main/java/io/github/atengk/design/expression/OrPermissionExpression.java`

下面是权限逻辑或表达式。

```java
package io.github.atengk.design.expression;

import io.github.atengk.design.dto.PermissionContext;
import lombok.extern.slf4j.Slf4j;

/**
 * 权限逻辑或表达式
 *
 * @author Ateng
 * @since 2026-04-30
 */
@Slf4j
public class OrPermissionExpression implements PermissionExpression {

    private final PermissionExpression leftExpression;
    private final PermissionExpression rightExpression;

    /**
     * 创建权限逻辑或表达式
     *
     * @param leftExpression  左表达式
     * @param rightExpression 右表达式
     */
    public OrPermissionExpression(PermissionExpression leftExpression, PermissionExpression rightExpression) {
        if (leftExpression == null || rightExpression == null) {
            throw new IllegalArgumentException("左右表达式不能为空");
        }

        this.leftExpression = leftExpression;
        this.rightExpression = rightExpression;
    }

    /**
     * 解释权限表达式
     *
     * @param context 权限解释上下文
     * @return true 表示任意表达式通过
     */
    @Override
    public boolean interpret(PermissionContext context) {
        boolean leftResult = leftExpression.interpret(context);
        if (leftResult) {
            log.info("解释权限逻辑或表达式，左表达式通过，短路返回 true");
            return true;
        }

        boolean rightResult = rightExpression.interpret(context);
        boolean result = leftResult || rightResult;

        log.info("解释权限逻辑或表达式，左结果：{}，右结果：{}，最终结果：{}",
                leftResult, rightResult, result);
        return result;
    }
}
```

文件位置：`src/main/java/io/github/atengk/design/expression/NotPermissionExpression.java`

下面是权限逻辑非表达式。

```java
package io.github.atengk.design.expression;

import io.github.atengk.design.dto.PermissionContext;
import lombok.extern.slf4j.Slf4j;

/**
 * 权限逻辑非表达式
 *
 * @author Ateng
 * @since 2026-04-30
 */
@Slf4j
public class NotPermissionExpression implements PermissionExpression {

    private final PermissionExpression expression;

    /**
     * 创建权限逻辑非表达式
     *
     * @param expression 子表达式
     */
    public NotPermissionExpression(PermissionExpression expression) {
        if (expression == null) {
            throw new IllegalArgumentException("子表达式不能为空");
        }

        this.expression = expression;
    }

    /**
     * 解释权限表达式
     *
     * @param context 权限解释上下文
     * @return true 表示子表达式不通过
     */
    @Override
    public boolean interpret(PermissionContext context) {
        boolean result = !expression.interpret(context);
        log.info("解释权限逻辑非表达式，最终结果：{}", result);
        return result;
    }
}
```

文件位置：`src/main/java/io/github/atengk/design/parser/PermissionExpressionParser.java`

下面是权限表达式解析器。它把字符串表达式解析成表达式语法树，支持 `&&`、`||`、`!` 和括号。

```java
package io.github.atengk.design.parser;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.StrUtil;
import io.github.atengk.design.expression.AndPermissionExpression;
import io.github.atengk.design.expression.DepartmentExpression;
import io.github.atengk.design.expression.NotPermissionExpression;
import io.github.atengk.design.expression.OrPermissionExpression;
import io.github.atengk.design.expression.PermissionCodeExpression;
import io.github.atengk.design.expression.PermissionExpression;
import io.github.atengk.design.expression.RoleExpression;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

/**
 * 权限表达式解析器
 *
 * @author Ateng
 * @since 2026-04-30
 */
@Slf4j
@Component
public class PermissionExpressionParser {

    private List<String> tokens = List.of();
    private int position = 0;

    /**
     * 解析权限表达式
     *
     * @param expression 权限表达式
     * @return 权限表达式语法树
     */
    public PermissionExpression parse(String expression) {
        if (StrUtil.isBlank(expression)) {
            log.warn("解析权限表达式失败，表达式为空");
            throw new IllegalArgumentException("权限表达式不能为空");
        }

        this.tokens = tokenize(expression);
        this.position = 0;

        PermissionExpression parsedExpression = parseOr();

        if (position < tokens.size()) {
            String token = tokens.get(position);
            log.warn("解析权限表达式失败，存在无法识别的剩余Token：{}", token);
            throw new IllegalArgumentException("表达式语法错误，无法识别：" + token);
        }

        log.info("解析权限表达式成功，表达式：{}，Token数量：{}", expression, tokens.size());
        return parsedExpression;
    }

    /**
     * 解析逻辑或表达式
     *
     * @return 权限表达式
     */
    private PermissionExpression parseOr() {
        PermissionExpression expression = parseAnd();

        while (match("||")) {
            PermissionExpression rightExpression = parseAnd();
            expression = new OrPermissionExpression(expression, rightExpression);
        }

        return expression;
    }

    /**
     * 解析逻辑与表达式
     *
     * @return 权限表达式
     */
    private PermissionExpression parseAnd() {
        PermissionExpression expression = parseUnary();

        while (match("&&")) {
            PermissionExpression rightExpression = parseUnary();
            expression = new AndPermissionExpression(expression, rightExpression);
        }

        return expression;
    }

    /**
     * 解析一元表达式
     *
     * @return 权限表达式
     */
    private PermissionExpression parseUnary() {
        if (match("!")) {
            return new NotPermissionExpression(parseUnary());
        }

        return parsePrimary();
    }

    /**
     * 解析基础表达式
     *
     * @return 权限表达式
     */
    private PermissionExpression parsePrimary() {
        if (match("(")) {
            PermissionExpression expression = parseOr();
            if (!match(")")) {
                log.warn("解析权限表达式失败，缺少右括号");
                throw new IllegalArgumentException("表达式语法错误，缺少右括号");
            }
            return expression;
        }

        if (position >= tokens.size()) {
            log.warn("解析权限表达式失败，表达式意外结束");
            throw new IllegalArgumentException("表达式语法错误，表达式意外结束");
        }

        String token = tokens.get(position++);
        return parseTerminal(token);
    }

    /**
     * 解析终结符表达式
     *
     * @param token Token
     * @return 权限表达式
     */
    private PermissionExpression parseTerminal(String token) {
        if (StrUtil.startWithIgnoreCase(token, "role:")) {
            return new RoleExpression(StrUtil.subAfter(token, "role:", false));
        }

        if (StrUtil.startWithIgnoreCase(token, "perm:")) {
            return new PermissionCodeExpression(StrUtil.subAfter(token, "perm:", false));
        }

        if (StrUtil.startWithIgnoreCase(token, "dept:")) {
            return new DepartmentExpression(StrUtil.subAfter(token, "dept:", false));
        }

        log.warn("解析权限表达式失败，不支持的Token：{}", token);
        throw new IllegalArgumentException("不支持的表达式Token：" + token);
    }

    /**
     * 匹配指定Token
     *
     * @param expected 期望Token
     * @return true 表示匹配成功
     */
    private boolean match(String expected) {
        if (position >= tokens.size()) {
            return false;
        }

        if (StrUtil.equals(tokens.get(position), expected)) {
            position++;
            return true;
        }

        return false;
    }

    /**
     * 将表达式切分为Token列表
     *
     * @param expression 权限表达式
     * @return Token列表
     */
    private List<String> tokenize(String expression) {
        List<String> result = new ArrayList<>();
        int index = 0;

        while (index < expression.length()) {
            char currentChar = expression.charAt(index);

            if (Character.isWhitespace(currentChar)) {
                index++;
                continue;
            }

            if (currentChar == '(' || currentChar == ')' || currentChar == '!') {
                result.add(String.valueOf(currentChar));
                index++;
                continue;
            }

            if (currentChar == '&' && index + 1 < expression.length() && expression.charAt(index + 1) == '&') {
                result.add("&&");
                index += 2;
                continue;
            }

            if (currentChar == '|' && index + 1 < expression.length() && expression.charAt(index + 1) == '|') {
                result.add("||");
                index += 2;
                continue;
            }

            int startIndex = index;
            while (index < expression.length()) {
                char nextChar = expression.charAt(index);
                if (Character.isWhitespace(nextChar) || nextChar == '(' || nextChar == ')' || nextChar == '!' || nextChar == '&' || nextChar == '|') {
                    break;
                }
                index++;
            }

            result.add(expression.substring(startIndex, index));
        }

        if (CollUtil.isEmpty(result)) {
            log.warn("解析权限表达式失败，Token列表为空");
            throw new IllegalArgumentException("权限表达式不能为空");
        }

        return result;
    }
}
```

文件位置：`src/main/java/io/github/atengk/design/service/PermissionExpressionService.java`

下面是权限表达式服务接口。

```java
package io.github.atengk.design.service;

import io.github.atengk.design.dto.PermissionCheckRequest;
import io.github.atengk.design.dto.PermissionCheckResponse;

/**
 * 权限表达式服务
 *
 * @author Ateng
 * @since 2026-04-30
 */
public interface PermissionExpressionService {

    /**
     * 校验权限表达式
     *
     * @param request 权限表达式校验请求
     * @return 权限表达式校验响应
     */
    PermissionCheckResponse check(PermissionCheckRequest request);
}
```

文件位置：`src/main/java/io/github/atengk/design/service/impl/PermissionExpressionServiceImpl.java`

下面是权限表达式服务实现。它负责构建上下文、解析表达式并解释执行。

```java
package io.github.atengk.design.service.impl;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.StrUtil;
import io.github.atengk.design.dto.PermissionCheckRequest;
import io.github.atengk.design.dto.PermissionCheckResponse;
import io.github.atengk.design.dto.PermissionContext;
import io.github.atengk.design.expression.PermissionExpression;
import io.github.atengk.design.parser.PermissionExpressionParser;
import io.github.atengk.design.service.PermissionExpressionService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.Set;

/**
 * 权限表达式服务实现
 *
 * @author Ateng
 * @since 2026-04-30
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class PermissionExpressionServiceImpl implements PermissionExpressionService {

    private final PermissionExpressionParser permissionExpressionParser;

    /**
     * 校验权限表达式
     *
     * @param request 权限表达式校验请求
     * @return 权限表达式校验响应
     */
    @Override
    public PermissionCheckResponse check(PermissionCheckRequest request) {
        validateRequest(request);

        PermissionContext context = new PermissionContext(
                request.userId(),
                CollUtil.emptyIfNull(request.roles()),
                CollUtil.emptyIfNull(request.permissions()),
                CollUtil.emptyIfNull(request.departmentIds())
        );

        PermissionExpression expression = permissionExpressionParser.parse(request.expression());
        boolean passed = expression.interpret(context);

        log.info("权限表达式校验完成，用户ID：{}，表达式：{}，结果：{}",
                request.userId(), request.expression(), passed);

        return new PermissionCheckResponse(
                request.userId(),
                request.expression(),
                passed,
                passed ? "权限校验通过" : "权限校验不通过"
        );
    }

    /**
     * 校验请求参数
     *
     * @param request 权限表达式校验请求
     */
    private void validateRequest(PermissionCheckRequest request) {
        if (request == null) {
            log.warn("权限表达式校验失败，请求参数为空");
            throw new IllegalArgumentException("请求参数不能为空");
        }

        if (request.userId() == null || request.userId() <= 0) {
            log.warn("权限表达式校验失败，用户ID不合法，用户ID：{}", request.userId());
            throw new IllegalArgumentException("用户ID必须大于0");
        }

        if (StrUtil.isBlank(request.expression())) {
            log.warn("权限表达式校验失败，表达式为空，用户ID：{}", request.userId());
            throw new IllegalArgumentException("权限表达式不能为空");
        }
    }
}
```

文件位置：`src/main/java/io/github/atengk/design/controller/PermissionExpressionController.java`

下面是权限表达式接口，用于验证解释器模式效果。

```java
package io.github.atengk.design.controller;

import cn.hutool.core.collection.CollUtil;
import io.github.atengk.design.dto.PermissionCheckRequest;
import io.github.atengk.design.dto.PermissionCheckResponse;
import io.github.atengk.design.service.PermissionExpressionService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.Set;

/**
 * 权限表达式控制器
 *
 * @author Ateng
 * @since 2026-04-30
 */
@RestController
@RequiredArgsConstructor
@RequestMapping("/interpreter/permission")
public class PermissionExpressionController {

    private final PermissionExpressionService permissionExpressionService;

    /**
     * 校验权限表达式
     *
     * @param userId        用户ID
     * @param expression    权限表达式
     * @param roles         角色集合，逗号分隔
     * @param permissions   权限码集合，逗号分隔
     * @param departmentIds 部门ID集合，逗号分隔
     * @return 权限表达式校验响应
     */
    @GetMapping("/check")
    public PermissionCheckResponse check(@RequestParam Long userId,
                                         @RequestParam String expression,
                                         @RequestParam(required = false) Set<String> roles,
                                         @RequestParam(required = false) Set<String> permissions,
                                         @RequestParam(required = false) Set<Long> departmentIds) {
        PermissionCheckRequest request = new PermissionCheckRequest(
                userId,
                expression,
                CollUtil.emptyIfNull(roles),
                CollUtil.emptyIfNull(permissions),
                CollUtil.emptyIfNull(departmentIds)
        );

        return permissionExpressionService.check(request);
    }
}
```

接口调用示例：

```bash
curl -G "http://localhost:8080/interpreter/permission/check" \
  --data-urlencode "userId=10001" \
  --data-urlencode "expression=role:ADMIN && perm:order:create" \
  --data-urlencode "roles=ADMIN" \
  --data-urlencode "permissions=order:create,order:update" \
  --data-urlencode "departmentIds=100,200"
```

可能返回：

```json
{
  "userId": 10001,
  "expression": "role:ADMIN && perm:order:create",
  "passed": true,
  "message": "权限校验通过"
}
```

带括号和逻辑非的调用示例：

```bash
curl -G "http://localhost:8080/interpreter/permission/check" \
  --data-urlencode "userId=10001" \
  --data-urlencode "expression=!role:GUEST && (perm:order:create || perm:order:audit)" \
  --data-urlencode "roles=ADMIN" \
  --data-urlencode "permissions=order:audit" \
  --data-urlencode "departmentIds=100"
```

这种方式的优点是表达式可以配置化。比如接口权限、按钮权限、审批条件都可以用相同解释器判断，不需要为每条规则写硬编码分支。

## 扩展新的终结符表达式

解释器模式的扩展点通常是新增终结符表达式。比如现在支持 `role:`、`perm:`、`dept:`，如果需要支持用户ID表达式 `user:10001`，可以新增一个表达式类。

文件位置：`src/main/java/io/github/atengk/design/expression/UserIdExpression.java`

下面是用户ID表达式，用于解释 `user:10001` 这类语法。

```java
package io.github.atengk.design.expression;

import cn.hutool.core.util.NumberUtil;
import cn.hutool.core.util.StrUtil;
import io.github.atengk.design.dto.PermissionContext;
import lombok.extern.slf4j.Slf4j;

/**
 * 用户ID表达式
 *
 * @author Ateng
 * @since 2026-04-30
 */
@Slf4j
public class UserIdExpression implements PermissionExpression {

    private final Long requiredUserId;

    /**
     * 创建用户ID表达式
     *
     * @param userIdText 用户ID文本
     */
    public UserIdExpression(String userIdText) {
        if (StrUtil.isBlank(userIdText) || !NumberUtil.isLong(userIdText)) {
            throw new IllegalArgumentException("用户ID必须是数字");
        }

        this.requiredUserId = Long.valueOf(userIdText);
    }

    /**
     * 解释权限表达式
     *
     * @param context 权限解释上下文
     * @return true 表示当前用户ID匹配
     */
    @Override
    public boolean interpret(PermissionContext context) {
        boolean result = context != null && requiredUserId.equals(context.userId());
        log.info("解释用户ID表达式，当前用户ID：{}，要求用户ID：{}，结果：{}",
                context == null ? null : context.userId(), requiredUserId, result);
        return result;
    }
}
```

然后在 `PermissionExpressionParser` 的 `parseTerminal` 方法中新增分支：

```java
if (StrUtil.startWithIgnoreCase(token, "user:")) {
    return new UserIdExpression(StrUtil.subAfter(token, "user:", false));
}
```

使用示例：

```text
user:10001 || role:ADMIN
```

如果终结符类型持续增加，可以将终结符解析能力抽成独立工厂，避免 `parseTerminal` 中分支过多。

## 解释器模式和策略模式的区别

解释器模式和策略模式都能把规则从主流程中拆出去，但二者关注点不同。

| 对比项   | 解释器模式                     | 策略模式                     |
| -------- | ------------------------------ | ---------------------------- |
| 核心目的 | 解释一套表达式语法             | 从多个算法中选择一个执行     |
| 输入内容 | 通常是表达式字符串或语法树     | 通常是策略类型和业务参数     |
| 结构特点 | 有终结符、非终结符、语法组合   | 一个接口多个实现             |
| 典型场景 | 权限表达式、规则 DSL、搜索语法 | 优惠算法、计费算法、支付渠道 |
| 扩展方式 | 扩展语法节点或解析器           | 新增策略实现                 |

简单理解：

```text
解释器模式：用户写一条规则表达式，系统解释这条表达式。
策略模式：系统根据类型选择一种算法执行。
```

如果表达式是 `role:ADMIN && perm:order:create`，适合解释器模式。如果只是根据 `discountType=FULL_REDUCTION` 选择满减算法，适合策略模式。

## 解释器模式和责任链模式的区别

解释器模式和责任链模式都可以处理规则，但规则组织方式不同。

| 对比项             | 解释器模式             | 责任链模式                 |
| ------------------ | ---------------------- | -------------------------- |
| 核心目的           | 解释表达式语法         | 多个处理器按顺序处理请求   |
| 规则结构           | 语法树结构             | 链式结构                   |
| 是否支持括号和组合 | 支持，适合组合表达式   | 不适合复杂括号组合         |
| 是否强调顺序       | 按语法优先级解释       | 按链路顺序执行             |
| 典型场景           | 权限表达式、条件表达式 | 参数校验链、风控链、过滤链 |

简单理解：

```text
解释器模式：解释一条规则公式。
责任链模式：请求依次过多个关卡。
```

表达式 `role:ADMIN && (perm:a || perm:b)` 更适合解释器模式。订单提交前依次校验参数、库存、风控，更适合责任链模式。

## 解释器模式和规则引擎

解释器模式适合简单 DSL，但不适合复杂规则系统。如果规则涉及大量事实对象、规则优先级、冲突解决、规则分组、规则热更新、规则命中解释、规则版本管理等，建议使用成熟规则引擎或表达式引擎。

常见选择如下：

| 工具       | 适用场景                    |
| ---------- | --------------------------- |
| SpEL       | Spring 项目中简单表达式求值 |
| Aviator    | 高性能表达式求值            |
| MVEL       | 对象属性访问和表达式求值    |
| Drools     | 复杂规则引擎                |
| Easy Rules | 简单规则引擎                |

如果只是下面这种表达式，手写解释器可以接受：

```text
role:ADMIN && perm:order:create
amount >= 100 && userLevel == VIP
dept:100 || role:SUPER_ADMIN
```

如果规则变成下面这种复杂形式，就不建议继续手写：

```text
支持函数调用
支持数组和集合操作
支持嵌套对象属性
支持规则优先级
支持规则分组
支持动态脚本
支持规则版本发布
支持命中路径解释
```

解释器模式适合“语法简单、边界可控”的业务规则，不适合把业务系统变成脚本执行平台。

## 验证方式

启动 Spring Boot 项目：

```bash
mvn spring-boot:run
```

执行角色和权限码表达式：

```bash
curl -G "http://localhost:8080/interpreter/permission/check" \
  --data-urlencode "userId=10001" \
  --data-urlencode "expression=role:ADMIN && perm:order:create" \
  --data-urlencode "roles=ADMIN" \
  --data-urlencode "permissions=order:create,order:update" \
  --data-urlencode "departmentIds=100,200"
```

执行带括号的表达式：

```bash
curl -G "http://localhost:8080/interpreter/permission/check" \
  --data-urlencode "userId=10001" \
  --data-urlencode "expression=role:ADMIN && (perm:order:create || perm:order:audit)" \
  --data-urlencode "roles=ADMIN" \
  --data-urlencode "permissions=order:audit" \
  --data-urlencode "departmentIds=100"
```

执行逻辑非表达式：

```bash
curl -G "http://localhost:8080/interpreter/permission/check" \
  --data-urlencode "userId=10001" \
  --data-urlencode "expression=!role:GUEST && perm:order:create" \
  --data-urlencode "roles=ADMIN" \
  --data-urlencode "permissions=order:create" \
  --data-urlencode "departmentIds=100"
```

如果解释器模式正常，可以看到类似日志：

```text
解析权限表达式成功，表达式：role:ADMIN && perm:order:create，Token数量：3
解释角色表达式，用户ID：10001，要求角色：ADMIN，结果：true
解释权限码表达式，用户ID：10001，要求权限码：order:create，结果：true
解释权限逻辑与表达式，左结果：true，右结果：true，最终结果：true
权限表达式校验完成，用户ID：10001，表达式：role:ADMIN && perm:order:create，结果：true
```

执行语法错误表达式：

```bash
curl -G "http://localhost:8080/interpreter/permission/check" \
  --data-urlencode "userId=10001" \
  --data-urlencode "expression=role:ADMIN && (perm:order:create" \
  --data-urlencode "roles=ADMIN" \
  --data-urlencode "permissions=order:create"
```

异常日志示例：

```text
解析权限表达式失败，缺少右括号
```

实际项目中建议结合全局异常处理器，将语法错误转换成统一响应结构。

## 注意事项

解释器模式适合简单、稳定、可控的表达式，不适合无限扩展成复杂脚本语言。语法一旦复杂，解析器、错误提示、性能优化和安全控制都会变得困难。

适合使用解释器模式的场景：

```text
权限表达式
简单优惠规则
告警判断条件
搜索过滤条件
流程流转条件
配置化开关条件
简单业务 DSL
```

不太适合使用解释器模式的场景：

```text
复杂脚本执行
复杂数学表达式
复杂对象属性访问
规则数量巨大且需要冲突解决
需要规则版本发布和灰度
需要业务人员在线编辑复杂规则
```

不要把用户输入的任意字符串直接作为脚本执行。本文示例只支持固定前缀和固定逻辑符号，不执行 Java 代码，因此风险较低。如果使用 SpEL 或其他表达式引擎，需要限制可访问对象、方法和类型。

风险示例：

```text
允许表达式调用任意 Java 方法
允许表达式访问系统类
允许表达式执行反射
允许表达式访问文件或网络
```

表达式解析器需要有明确语法边界。本文示例仅支持：

```text
role:xxx
perm:xxx
dept:xxx
&&
||
!
(
)
```

不支持的语法应该明确抛出异常，而不是尝试模糊解析。

表达式缓存可以提升性能。如果同一表达式会被频繁解释，可以缓存解析后的语法树。

示例思路：

```text
Map<String, PermissionExpression> expressionCache
key = 表达式字符串
value = 解析后的表达式语法树
```

但要注意缓存大小控制，避免用户传入大量不同表达式导致内存增长。可以使用 Caffeine 设置最大数量和过期时间。

表达式解释过程应该尽量无副作用。解释器通常只判断条件，不应该在 `interpret` 方法中修改数据库、发送消息或改变上下文状态。

不推荐：

```java
public boolean interpret(PermissionContext context) {
    // 修改用户权限
    // 写数据库
    // 发送通知
    return true;
}
```

推荐：

```java
public boolean interpret(PermissionContext context) {
    return context.permissions().contains(requiredPermission);
}
```

如果表达式用于权限控制，需要配合权限数据来源、缓存刷新、审计日志和接口安全策略。解释器只负责判断表达式本身，不负责用户登录态、权限加载、权限变更同步等完整安全体系。

生产环境中常见补充点包括：

```text
表达式合法性校验
表达式解析缓存
权限上下文缓存
表达式变更审计
权限变更刷新
接口级别兜底鉴权
异常表达式降级策略
```

## 总结

在 JDK21 和 Spring Boot 3 项目中，解释器模式的实践重点是为简单业务规则定义一套清晰语法，并把表达式解析成可解释的语法树。

普通 Java 解释器适合理解终结符表达式、非终结符表达式和上下文。Spring Boot 项目中更常见的是权限表达式、优惠规则、告警规则、流程条件等配置化规则解释。推荐使用“表达式接口 + 终结符表达式 + 逻辑组合表达式 + 解析器 + 业务服务”的结构，把规则判断从硬编码分支中抽离出来。

解释器模式不是为了实现通用脚本语言。它最适合处理“语法简单、规则稳定、表达式可配置、解释结果明确”的场景。实际落地时，需要重点关注语法边界、表达式缓存、安全限制、错误提示和复杂规则是否应该交给成熟规则引擎。
