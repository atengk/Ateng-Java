# 设计模式：规格模式

规格模式用于把业务规则封装成可复用、可组合、可测试的规格对象。在 JDK21 和 Spring Boot 3 项目中，规格模式常用于优惠券领取规则、订单风控规则、商品上下架条件、会员权益判断、动态查询条件、领域对象校验、权限规则组合等场景。

需要注意：规格模式不是 GoF 23 种设计模式之一，属于这次设计模式文档里的“遗漏补充”。它在领域建模、DDD、复杂查询、复杂业务校验中非常常见，尤其适合规则较多、规则需要组合、规则不希望散落在大量 `if else` 中的项目。

## 基础配置

本示例基于 JDK21、Spring Boot 3、Maven、MyBatis-Plus、MySQL。示例包路径统一使用 `io.github.atengk`。

文件位置：`pom.xml`

```xml
<dependencies>
    <!-- Spring Boot Web，用于提供接口验证规格模式行为 -->
    <dependency>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-starter-web</artifactId>
    </dependency>

    <!-- MyBatis-Plus Spring Boot 3 Starter，用于动态查询规格示例 -->
    <dependency>
        <groupId>com.baomidou</groupId>
        <artifactId>mybatis-plus-spring-boot3-starter</artifactId>
        <version>3.5.8</version>
    </dependency>

    <!-- MySQL 驱动，用于连接 MySQL 数据库 -->
    <dependency>
        <groupId>com.mysql</groupId>
        <artifactId>mysql-connector-j</artifactId>
        <scope>runtime</scope>
    </dependency>

    <!-- Hutool 工具类，用于字符串、集合、金额等通用处理 -->
    <dependency>
        <groupId>cn.hutool</groupId>
        <artifactId>hutool-all</artifactId>
        <version>5.8.27</version>
    </dependency>

    <!-- Lombok，简化日志对象、Getter、Setter、构造方法等样板代码 -->
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

文件位置：`src/main/resources/application.yml`

```yaml
server:
  # 示例服务端口
  port: 8080

spring:
  datasource:
    # MySQL 连接地址，根据本地数据库调整
    url: jdbc:mysql://localhost:3306/design_demo?useUnicode=true&characterEncoding=utf8&serverTimezone=Asia/Shanghai&useSSL=false
    # 数据库用户名
    username: root
    # 数据库密码
    password: root
    # MySQL 驱动类
    driver-class-name: com.mysql.cj.jdbc.Driver

mybatis-plus:
  configuration:
    # 开发环境输出 SQL，便于观察动态查询规格生成的条件
    log-impl: org.apache.ibatis.logging.stdout.StdOutImpl
  global-config:
    db-config:
      # 示例使用雪花ID
      id-type: assign_id
```

文件位置：`sql/product.sql`

```sql
CREATE TABLE product (
    id BIGINT PRIMARY KEY COMMENT '商品ID',
    product_code VARCHAR(64) NOT NULL COMMENT '商品编码',
    product_name VARCHAR(100) NOT NULL COMMENT '商品名称',
    category_code VARCHAR(64) NOT NULL COMMENT '分类编码',
    price DECIMAL(18, 2) NOT NULL COMMENT '商品价格',
    stock INT NOT NULL DEFAULT 0 COMMENT '库存数量',
    status VARCHAR(32) NOT NULL COMMENT '商品状态：DRAFT 草稿，ON_SHELF 上架，OFF_SHELF 下架',
    create_time DATETIME NOT NULL COMMENT '创建时间',
    update_time DATETIME NOT NULL COMMENT '更新时间',
    UNIQUE KEY uk_product_code (product_code),
    KEY idx_category_code (category_code),
    KEY idx_status (status),
    KEY idx_price (price)
) COMMENT='商品表';
```

这张表用于演示“查询规格模式”。业务规则规格不依赖数据库，查询规格则通过 MyBatis-Plus 的 `LambdaQueryWrapper` 动态拼接查询条件。

## 核心概念

规格模式的核心目标是把一个判断条件封装成独立对象，并让规格之间可以通过 `and`、`or`、`not` 组合成更复杂的规则。

常见角色如下：

| 角色                   | 说明                                           |
| ---------------------- | ---------------------------------------------- |
| Specification          | 规格接口，定义对象是否满足规格                 |
| ConcreteSpecification  | 具体规格，实现某一个业务条件                   |
| CompositeSpecification | 组合规格，用于 `and`、`or`、`not` 组合         |
| Candidate              | 被判断的候选对象，例如订单、商品、用户、优惠券 |
| Client                 | 调用方，组合规格并执行判断                     |

典型结构如下：

```text
CouponSpecification
├── AmountReachedSpecification
├── UserLevelSpecification
├── CouponActiveSpecification
└── StockEnoughSpecification

finalSpec = activeSpec
    .and(stockEnoughSpec)
    .and(amountReachedSpec)
    .and(userLevelSpec)
```

规格模式最常见的两类用法：

```text
业务规则规格：判断某个对象是否满足规则
查询条件规格：把筛选条件封装成可组合查询条件
```

在 Spring Boot 项目中，常见优先级通常是：

```text
业务规则可组合：规格模式
动态查询条件可复用：查询规格模式
只有一个简单分支：普通 if 判断即可
算法互斥选择：策略模式更合适
```

规格模式不是为了消灭所有 `if`。它适合规则多、规则复用频繁、规则需要组合、规则需要单独测试的场景。

## 普通 Java 规格模式

普通 Java 规格模式适合先理解“规则对象化”和“规则组合”。下面以优惠券可用性为例，订单需要同时满足金额门槛、用户等级、首单要求等条件。

整体关系如下：

```text
OrderContext
    -> Specification<OrderContext>
        -> AmountReachedSpecification
        -> UserLevelSpecification
        -> FirstOrderSpecification
        -> AndSpecification / OrSpecification / NotSpecification
```

### 文件结构

```text
src/main/java/io/github/atengk/design/specification/simple/
├── Specification.java
├── AbstractSpecification.java
├── AndSpecification.java
├── OrSpecification.java
├── NotSpecification.java
├── OrderContext.java
├── AmountReachedSpecification.java
├── UserLevelSpecification.java
└── FirstOrderSpecification.java
```

文件位置：`src/main/java/io/github/atengk/design/specification/simple/Specification.java`

下面是通用规格接口，定义候选对象是否满足规则。

```java
package io.github.atengk.design.specification.simple;

/**
 * 通用规格接口
 *
 * @param <T> 候选对象类型
 * @author Ateng
 * @since 2026-05-01
 */
public interface Specification<T> {

    /**
     * 判断候选对象是否满足规格
     *
     * @param candidate 候选对象
     * @return true 表示满足，false 表示不满足
     */
    boolean isSatisfiedBy(T candidate);

    /**
     * 与规格组合
     *
     * @param other 其他规格
     * @return 组合后的规格
     */
    default Specification<T> and(Specification<T> other) {
        return new AndSpecification<>(this, other);
    }

    /**
     * 或规格组合
     *
     * @param other 其他规格
     * @return 组合后的规格
     */
    default Specification<T> or(Specification<T> other) {
        return new OrSpecification<>(this, other);
    }

    /**
     * 非规格组合
     *
     * @return 组合后的规格
     */
    default Specification<T> not() {
        return new NotSpecification<>(this);
    }
}
```

文件位置：`src/main/java/io/github/atengk/design/specification/simple/AndSpecification.java`

下面是逻辑与规格，两个规格都满足时才返回 true。

```java
package io.github.atengk.design.specification.simple;

import lombok.extern.slf4j.Slf4j;

/**
 * 逻辑与规格
 *
 * @param <T> 候选对象类型
 * @author Ateng
 * @since 2026-05-01
 */
@Slf4j
public class AndSpecification<T> implements Specification<T> {

    private final Specification<T> left;
    private final Specification<T> right;

    /**
     * 创建逻辑与规格
     *
     * @param left  左规格
     * @param right 右规格
     */
    public AndSpecification(Specification<T> left, Specification<T> right) {
        if (left == null || right == null) {
            throw new IllegalArgumentException("左右规格不能为空");
        }

        this.left = left;
        this.right = right;
    }

    /**
     * 判断候选对象是否满足规格
     *
     * @param candidate 候选对象
     * @return true 表示满足
     */
    @Override
    public boolean isSatisfiedBy(T candidate) {
        boolean leftResult = left.isSatisfiedBy(candidate);
        if (!leftResult) {
            log.info("逻辑与规格短路返回 false");
            return false;
        }

        boolean rightResult = right.isSatisfiedBy(candidate);
        return rightResult;
    }
}
```

文件位置：`src/main/java/io/github/atengk/design/specification/simple/OrSpecification.java`

下面是逻辑或规格，任意一个规格满足就返回 true。

```java
package io.github.atengk.design.specification.simple;

import lombok.extern.slf4j.Slf4j;

/**
 * 逻辑或规格
 *
 * @param <T> 候选对象类型
 * @author Ateng
 * @since 2026-05-01
 */
@Slf4j
public class OrSpecification<T> implements Specification<T> {

    private final Specification<T> left;
    private final Specification<T> right;

    /**
     * 创建逻辑或规格
     *
     * @param left  左规格
     * @param right 右规格
     */
    public OrSpecification(Specification<T> left, Specification<T> right) {
        if (left == null || right == null) {
            throw new IllegalArgumentException("左右规格不能为空");
        }

        this.left = left;
        this.right = right;
    }

    /**
     * 判断候选对象是否满足规格
     *
     * @param candidate 候选对象
     * @return true 表示满足
     */
    @Override
    public boolean isSatisfiedBy(T candidate) {
        boolean leftResult = left.isSatisfiedBy(candidate);
        if (leftResult) {
            log.info("逻辑或规格短路返回 true");
            return true;
        }

        return right.isSatisfiedBy(candidate);
    }
}
```

文件位置：`src/main/java/io/github/atengk/design/specification/simple/NotSpecification.java`

下面是逻辑非规格，用于对原规格结果取反。

```java
package io.github.atengk.design.specification.simple;

/**
 * 逻辑非规格
 *
 * @param <T> 候选对象类型
 * @author Ateng
 * @since 2026-05-01
 */
public class NotSpecification<T> implements Specification<T> {

    private final Specification<T> specification;

    /**
     * 创建逻辑非规格
     *
     * @param specification 原规格
     */
    public NotSpecification(Specification<T> specification) {
        if (specification == null) {
            throw new IllegalArgumentException("规格不能为空");
        }

        this.specification = specification;
    }

    /**
     * 判断候选对象是否满足规格
     *
     * @param candidate 候选对象
     * @return true 表示满足取反后的规格
     */
    @Override
    public boolean isSatisfiedBy(T candidate) {
        return !specification.isSatisfiedBy(candidate);
    }
}
```

文件位置：`src/main/java/io/github/atengk/design/specification/simple/OrderContext.java`

下面是订单上下文，作为规格判断的候选对象。

```java
package io.github.atengk.design.specification.simple;

import java.math.BigDecimal;

/**
 * 订单上下文
 *
 * @param orderNo    订单号
 * @param userId     用户ID
 * @param userLevel  用户等级
 * @param amount     订单金额
 * @param firstOrder 是否首单
 * @author Ateng
 * @since 2026-05-01
 */
public record OrderContext(
        String orderNo,
        Long userId,
        String userLevel,
        BigDecimal amount,
        Boolean firstOrder
) {
}
```

文件位置：`src/main/java/io/github/atengk/design/specification/simple/AmountReachedSpecification.java`

下面是订单金额达标规格。

```java
package io.github.atengk.design.specification.simple;

import lombok.extern.slf4j.Slf4j;

import java.math.BigDecimal;

/**
 * 订单金额达标规格
 *
 * @author Ateng
 * @since 2026-05-01
 */
@Slf4j
public class AmountReachedSpecification implements Specification<OrderContext> {

    private final BigDecimal thresholdAmount;

    /**
     * 创建订单金额达标规格
     *
     * @param thresholdAmount 门槛金额
     */
    public AmountReachedSpecification(BigDecimal thresholdAmount) {
        if (thresholdAmount == null || thresholdAmount.compareTo(BigDecimal.ZERO) < 0) {
            throw new IllegalArgumentException("门槛金额不能小于0");
        }

        this.thresholdAmount = thresholdAmount;
    }

    /**
     * 判断订单是否满足金额门槛
     *
     * @param candidate 订单上下文
     * @return true 表示满足
     */
    @Override
    public boolean isSatisfiedBy(OrderContext candidate) {
        if (candidate == null || candidate.amount() == null) {
            log.warn("金额规格校验失败，订单上下文或金额为空");
            return false;
        }

        boolean result = candidate.amount().compareTo(thresholdAmount) >= 0;
        log.info("金额规格校验完成，订单号：{}，订单金额：{}，门槛金额：{}，结果：{}",
                candidate.orderNo(), candidate.amount(), thresholdAmount, result);
        return result;
    }
}
```

文件位置：`src/main/java/io/github/atengk/design/specification/simple/UserLevelSpecification.java`

下面是用户等级规格。

```java
package io.github.atengk.design.specification.simple;

import cn.hutool.core.util.StrUtil;
import lombok.extern.slf4j.Slf4j;

/**
 * 用户等级规格
 *
 * @author Ateng
 * @since 2026-05-01
 */
@Slf4j
public class UserLevelSpecification implements Specification<OrderContext> {

    private final String requiredUserLevel;

    /**
     * 创建用户等级规格
     *
     * @param requiredUserLevel 要求用户等级
     */
    public UserLevelSpecification(String requiredUserLevel) {
        if (StrUtil.isBlank(requiredUserLevel)) {
            throw new IllegalArgumentException("要求用户等级不能为空");
        }

        this.requiredUserLevel = requiredUserLevel;
    }

    /**
     * 判断订单用户等级是否满足要求
     *
     * @param candidate 订单上下文
     * @return true 表示满足
     */
    @Override
    public boolean isSatisfiedBy(OrderContext candidate) {
        if (candidate == null || StrUtil.isBlank(candidate.userLevel())) {
            log.warn("用户等级规格校验失败，订单上下文或用户等级为空");
            return false;
        }

        boolean result = StrUtil.equalsIgnoreCase(candidate.userLevel(), requiredUserLevel);
        log.info("用户等级规格校验完成，订单号：{}，当前等级：{}，要求等级：{}，结果：{}",
                candidate.orderNo(), candidate.userLevel(), requiredUserLevel, result);
        return result;
    }
}
```

文件位置：`src/main/java/io/github/atengk/design/specification/simple/FirstOrderSpecification.java`

下面是首单规格。

```java
package io.github.atengk.design.specification.simple;

import lombok.extern.slf4j.Slf4j;

/**
 * 首单规格
 *
 * @author Ateng
 * @since 2026-05-01
 */
@Slf4j
public class FirstOrderSpecification implements Specification<OrderContext> {

    /**
     * 判断订单是否为首单
     *
     * @param candidate 订单上下文
     * @return true 表示满足
     */
    @Override
    public boolean isSatisfiedBy(OrderContext candidate) {
        boolean result = candidate != null && Boolean.TRUE.equals(candidate.firstOrder());
        log.info("首单规格校验完成，订单号：{}，结果：{}",
                candidate == null ? null : candidate.orderNo(), result);
        return result;
    }
}
```

使用方式：

```java
Specification<OrderContext> specification = new AmountReachedSpecification(BigDecimal.valueOf(100))
        .and(new UserLevelSpecification("VIP"))
        .or(new FirstOrderSpecification());

OrderContext context = new OrderContext(
        "ORDER10001",
        10001L,
        "NORMAL",
        BigDecimal.valueOf(120),
        true
);

boolean satisfied = specification.isSatisfiedBy(context);
```

这条规则表示：

```text
订单金额 >= 100 且用户等级是 VIP，或者订单是首单
```

规格模式的价值在于：金额规则、用户等级规则、首单规则都可以单独复用和单独测试。

## Spring Boot 业务规则规格

Spring Boot 项目中，规格模式常用于复杂业务规则校验。下面以优惠券领取为例，领取优惠券需要满足：优惠券有效、库存充足、用户等级符合、订单金额达到门槛。

整体流程如下：

```text
Controller
    -> CouponReceiveService
        -> 构建 CouponReceiveContext
        -> CouponReceiveSpecificationFactory
        -> specification.isSatisfiedBy(context)
        -> 通过后执行领取
```

### 文件结构

```text
src/main/java/io/github/atengk/design/
├── SpecificationApplication.java
├── controller/
│   └── CouponReceiveController.java
├── dto/
│   ├── CouponReceiveRequest.java
│   └── CouponReceiveResponse.java
├── spec/
│   ├── Specification.java
│   ├── AndSpecification.java
│   ├── CouponReceiveContext.java
│   ├── CouponActiveSpecification.java
│   ├── CouponStockSpecification.java
│   ├── CouponUserLevelSpecification.java
│   ├── CouponAmountSpecification.java
│   └── CouponReceiveSpecificationFactory.java
└── service/
    ├── CouponReceiveService.java
    └── impl/
        └── CouponReceiveServiceImpl.java
```

文件位置：`src/main/java/io/github/atengk/design/SpecificationApplication.java`

下面是 Spring Boot 启动类。

```java
package io.github.atengk.design;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * 规格模式示例启动类
 *
 * @author Ateng
 * @since 2026-05-01
 */
@SpringBootApplication
public class SpecificationApplication {

    /**
     * 应用启动入口
     *
     * @param args 启动参数
     */
    public static void main(String[] args) {
        SpringApplication.run(SpecificationApplication.class, args);
    }
}
```

文件位置：`src/main/java/io/github/atengk/design/spec/Specification.java`

下面是 Spring Boot 示例中的通用规格接口。

```java
package io.github.atengk.design.spec;

/**
 * 通用规格接口
 *
 * @param <T> 候选对象类型
 * @author Ateng
 * @since 2026-05-01
 */
public interface Specification<T> {

    /**
     * 判断候选对象是否满足规格
     *
     * @param candidate 候选对象
     * @return true 表示满足，false 表示不满足
     */
    boolean isSatisfiedBy(T candidate);

    /**
     * 与规格组合
     *
     * @param other 其他规格
     * @return 组合规格
     */
    default Specification<T> and(Specification<T> other) {
        return new AndSpecification<>(this, other);
    }
}
```

文件位置：`src/main/java/io/github/atengk/design/spec/AndSpecification.java`

下面是逻辑与规格，用于组合优惠券多个领取条件。

```java
package io.github.atengk.design.spec;

/**
 * 逻辑与规格
 *
 * @param <T> 候选对象类型
 * @author Ateng
 * @since 2026-05-01
 */
public class AndSpecification<T> implements Specification<T> {

    private final Specification<T> left;
    private final Specification<T> right;

    /**
     * 创建逻辑与规格
     *
     * @param left  左规格
     * @param right 右规格
     */
    public AndSpecification(Specification<T> left, Specification<T> right) {
        if (left == null || right == null) {
            throw new IllegalArgumentException("左右规格不能为空");
        }

        this.left = left;
        this.right = right;
    }

    /**
     * 判断候选对象是否满足规格
     *
     * @param candidate 候选对象
     * @return true 表示满足
     */
    @Override
    public boolean isSatisfiedBy(T candidate) {
        return left.isSatisfiedBy(candidate) && right.isSatisfiedBy(candidate);
    }
}
```

文件位置：`src/main/java/io/github/atengk/design/spec/CouponReceiveContext.java`

下面是优惠券领取上下文，承载规则判断需要的全部数据。

```java
package io.github.atengk.design.spec;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 优惠券领取上下文
 *
 * @param couponNo          优惠券编号
 * @param userId            用户ID
 * @param userLevel         用户等级
 * @param orderAmount       订单金额
 * @param couponStock       优惠券库存
 * @param requiredUserLevel 要求用户等级
 * @param thresholdAmount   使用门槛金额
 * @param beginTime         生效时间
 * @param endTime           失效时间
 * @param currentTime       当前时间
 * @author Ateng
 * @since 2026-05-01
 */
public record CouponReceiveContext(
        String couponNo,
        Long userId,
        String userLevel,
        BigDecimal orderAmount,
        Integer couponStock,
        String requiredUserLevel,
        BigDecimal thresholdAmount,
        LocalDateTime beginTime,
        LocalDateTime endTime,
        LocalDateTime currentTime
) {
}
```

文件位置：`src/main/java/io/github/atengk/design/spec/CouponActiveSpecification.java`

下面是优惠券有效期规格。

```java
package io.github.atengk.design.spec;

import lombok.extern.slf4j.Slf4j;

import java.time.LocalDateTime;

/**
 * 优惠券有效期规格
 *
 * @author Ateng
 * @since 2026-05-01
 */
@Slf4j
public class CouponActiveSpecification implements Specification<CouponReceiveContext> {

    /**
     * 判断优惠券是否处于有效期内
     *
     * @param candidate 优惠券领取上下文
     * @return true 表示有效
     */
    @Override
    public boolean isSatisfiedBy(CouponReceiveContext candidate) {
        if (candidate == null || candidate.beginTime() == null || candidate.endTime() == null) {
            log.warn("优惠券有效期规格校验失败，上下文或时间为空");
            return false;
        }

        LocalDateTime currentTime = candidate.currentTime() == null ? LocalDateTime.now() : candidate.currentTime();
        boolean result = !currentTime.isBefore(candidate.beginTime()) && !currentTime.isAfter(candidate.endTime());

        log.info("优惠券有效期规格校验完成，优惠券编号：{}，结果：{}", candidate.couponNo(), result);
        return result;
    }
}
```

文件位置：`src/main/java/io/github/atengk/design/spec/CouponStockSpecification.java`

下面是优惠券库存规格。

```java
package io.github.atengk.design.spec;

import lombok.extern.slf4j.Slf4j;

/**
 * 优惠券库存规格
 *
 * @author Ateng
 * @since 2026-05-01
 */
@Slf4j
public class CouponStockSpecification implements Specification<CouponReceiveContext> {

    /**
     * 判断优惠券库存是否充足
     *
     * @param candidate 优惠券领取上下文
     * @return true 表示库存充足
     */
    @Override
    public boolean isSatisfiedBy(CouponReceiveContext candidate) {
        boolean result = candidate != null && candidate.couponStock() != null && candidate.couponStock() > 0;
        log.info("优惠券库存规格校验完成，优惠券编号：{}，库存：{}，结果：{}",
                candidate == null ? null : candidate.couponNo(),
                candidate == null ? null : candidate.couponStock(),
                result);
        return result;
    }
}
```

文件位置：`src/main/java/io/github/atengk/design/spec/CouponUserLevelSpecification.java`

下面是用户等级规格。

```java
package io.github.atengk.design.spec;

import cn.hutool.core.util.StrUtil;
import lombok.extern.slf4j.Slf4j;

/**
 * 优惠券用户等级规格
 *
 * @author Ateng
 * @since 2026-05-01
 */
@Slf4j
public class CouponUserLevelSpecification implements Specification<CouponReceiveContext> {

    /**
     * 判断用户等级是否符合优惠券要求
     *
     * @param candidate 优惠券领取上下文
     * @return true 表示符合
     */
    @Override
    public boolean isSatisfiedBy(CouponReceiveContext candidate) {
        if (candidate == null || StrUtil.hasBlank(candidate.userLevel(), candidate.requiredUserLevel())) {
            log.warn("优惠券用户等级规格校验失败，上下文、用户等级或要求等级为空");
            return false;
        }

        boolean result = StrUtil.equalsIgnoreCase(candidate.userLevel(), candidate.requiredUserLevel());
        log.info("优惠券用户等级规格校验完成，用户ID：{}，当前等级：{}，要求等级：{}，结果：{}",
                candidate.userId(), candidate.userLevel(), candidate.requiredUserLevel(), result);
        return result;
    }
}
```

文件位置：`src/main/java/io/github/atengk/design/spec/CouponAmountSpecification.java`

下面是订单金额门槛规格。

```java
package io.github.atengk.design.spec;

import lombok.extern.slf4j.Slf4j;

import java.math.BigDecimal;

/**
 * 优惠券金额门槛规格
 *
 * @author Ateng
 * @since 2026-05-01
 */
@Slf4j
public class CouponAmountSpecification implements Specification<CouponReceiveContext> {

    /**
     * 判断订单金额是否达到优惠券门槛
     *
     * @param candidate 优惠券领取上下文
     * @return true 表示达到门槛
     */
    @Override
    public boolean isSatisfiedBy(CouponReceiveContext candidate) {
        if (candidate == null || candidate.orderAmount() == null || candidate.thresholdAmount() == null) {
            log.warn("优惠券金额门槛规格校验失败，上下文、订单金额或门槛金额为空");
            return false;
        }

        boolean result = candidate.orderAmount().compareTo(candidate.thresholdAmount()) >= 0;
        log.info("优惠券金额门槛规格校验完成，订单金额：{}，门槛金额：{}，结果：{}",
                candidate.orderAmount(), candidate.thresholdAmount(), result);
        return result;
    }
}
```

文件位置：`src/main/java/io/github/atengk/design/spec/CouponReceiveSpecificationFactory.java`

下面是优惠券领取规格工厂，用于组合完整领取规则。

```java
package io.github.atengk.design.spec;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * 优惠券领取规格工厂
 *
 * @author Ateng
 * @since 2026-05-01
 */
@Slf4j
@Component
public class CouponReceiveSpecificationFactory {

    /**
     * 创建优惠券领取完整规格
     *
     * @return 优惠券领取规格
     */
    public Specification<CouponReceiveContext> createReceiveSpecification() {
        Specification<CouponReceiveContext> specification = new CouponActiveSpecification()
                .and(new CouponStockSpecification())
                .and(new CouponUserLevelSpecification())
                .and(new CouponAmountSpecification());

        log.info("创建优惠券领取组合规格完成");
        return specification;
    }
}
```

文件位置：`src/main/java/io/github/atengk/design/dto/CouponReceiveRequest.java`

下面是优惠券领取请求对象。

```java
package io.github.atengk.design.dto;

import java.math.BigDecimal;

/**
 * 优惠券领取请求
 *
 * @param couponNo    优惠券编号
 * @param userId      用户ID
 * @param userLevel   用户等级
 * @param orderAmount 订单金额
 * @author Ateng
 * @since 2026-05-01
 */
public record CouponReceiveRequest(
        String couponNo,
        Long userId,
        String userLevel,
        BigDecimal orderAmount
) {
}
```

文件位置：`src/main/java/io/github/atengk/design/dto/CouponReceiveResponse.java`

下面是优惠券领取响应对象。

```java
package io.github.atengk.design.dto;

/**
 * 优惠券领取响应
 *
 * @param couponNo 优惠券编号
 * @param userId   用户ID
 * @param received 是否领取成功
 * @param message  响应消息
 * @author Ateng
 * @since 2026-05-01
 */
public record CouponReceiveResponse(
        String couponNo,
        Long userId,
        Boolean received,
        String message
) {
}
```

文件位置：`src/main/java/io/github/atengk/design/service/CouponReceiveService.java`

下面是优惠券领取服务接口。

```java
package io.github.atengk.design.service;

import io.github.atengk.design.dto.CouponReceiveRequest;
import io.github.atengk.design.dto.CouponReceiveResponse;

/**
 * 优惠券领取服务
 *
 * @author Ateng
 * @since 2026-05-01
 */
public interface CouponReceiveService {

    /**
     * 领取优惠券
     *
     * @param request 优惠券领取请求
     * @return 优惠券领取响应
     */
    CouponReceiveResponse receive(CouponReceiveRequest request);
}
```

文件位置：`src/main/java/io/github/atengk/design/service/impl/CouponReceiveServiceImpl.java`

下面是优惠券领取服务实现。示例中用固定数据模拟优惠券配置，实际项目中可以从数据库读取。

```java
package io.github.atengk.design.service.impl;

import cn.hutool.core.util.StrUtil;
import io.github.atengk.design.dto.CouponReceiveRequest;
import io.github.atengk.design.dto.CouponReceiveResponse;
import io.github.atengk.design.service.CouponReceiveService;
import io.github.atengk.design.spec.CouponReceiveContext;
import io.github.atengk.design.spec.CouponReceiveSpecificationFactory;
import io.github.atengk.design.spec.Specification;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 优惠券领取服务实现
 *
 * @author Ateng
 * @since 2026-05-01
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class CouponReceiveServiceImpl implements CouponReceiveService {

    private final CouponReceiveSpecificationFactory specificationFactory;

    /**
     * 领取优惠券
     *
     * @param request 优惠券领取请求
     * @return 优惠券领取响应
     */
    @Override
    public CouponReceiveResponse receive(CouponReceiveRequest request) {
        validateRequest(request);

        CouponReceiveContext context = new CouponReceiveContext(
                request.couponNo(),
                request.userId(),
                request.userLevel(),
                request.orderAmount(),
                10,
                "VIP",
                BigDecimal.valueOf(100),
                LocalDateTime.now().minusDays(1),
                LocalDateTime.now().plusDays(7),
                LocalDateTime.now()
        );

        Specification<CouponReceiveContext> specification = specificationFactory.createReceiveSpecification();
        boolean satisfied = specification.isSatisfiedBy(context);

        if (!satisfied) {
            log.warn("领取优惠券失败，不满足领取规格，优惠券编号：{}，用户ID：{}",
                    request.couponNo(), request.userId());
            return new CouponReceiveResponse(request.couponNo(), request.userId(), false, "不满足优惠券领取条件");
        }

        log.info("领取优惠券成功，优惠券编号：{}，用户ID：{}", request.couponNo(), request.userId());
        return new CouponReceiveResponse(request.couponNo(), request.userId(), true, "领取成功");
    }

    /**
     * 校验优惠券领取请求
     *
     * @param request 优惠券领取请求
     */
    private void validateRequest(CouponReceiveRequest request) {
        if (request == null) {
            log.warn("领取优惠券失败，请求参数为空");
            throw new IllegalArgumentException("请求参数不能为空");
        }

        if (StrUtil.hasBlank(request.couponNo(), request.userLevel())) {
            log.warn("领取优惠券失败，优惠券编号或用户等级为空");
            throw new IllegalArgumentException("优惠券编号和用户等级不能为空");
        }

        if (request.userId() == null || request.userId() <= 0) {
            log.warn("领取优惠券失败，用户ID不合法，用户ID：{}", request.userId());
            throw new IllegalArgumentException("用户ID必须大于0");
        }

        if (request.orderAmount() == null || request.orderAmount().compareTo(BigDecimal.ZERO) < 0) {
            log.warn("领取优惠券失败，订单金额不合法，金额：{}", request.orderAmount());
            throw new IllegalArgumentException("订单金额不能小于0");
        }
    }
}
```

文件位置：`src/main/java/io/github/atengk/design/controller/CouponReceiveController.java`

下面是优惠券领取接口，用于验证业务规则规格。

```java
package io.github.atengk.design.controller;

import io.github.atengk.design.dto.CouponReceiveRequest;
import io.github.atengk.design.dto.CouponReceiveResponse;
import io.github.atengk.design.service.CouponReceiveService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;

/**
 * 优惠券领取控制器
 *
 * @author Ateng
 * @since 2026-05-01
 */
@RestController
@RequiredArgsConstructor
@RequestMapping("/specification/coupon")
public class CouponReceiveController {

    private final CouponReceiveService couponReceiveService;

    /**
     * 领取优惠券
     *
     * @param couponNo    优惠券编号
     * @param userId      用户ID
     * @param userLevel   用户等级
     * @param orderAmount 订单金额
     * @return 优惠券领取响应
     */
    @PostMapping("/receive")
    public CouponReceiveResponse receive(@RequestParam String couponNo,
                                         @RequestParam Long userId,
                                         @RequestParam String userLevel,
                                         @RequestParam BigDecimal orderAmount) {
        CouponReceiveRequest request = new CouponReceiveRequest(couponNo, userId, userLevel, orderAmount);
        return couponReceiveService.receive(request);
    }
}
```

## 查询规格模式

规格模式也常用于动态查询。查询规格的目标不是返回 true 或 false，而是把查询条件封装起来，统一应用到查询构造器中。

整体关系如下：

```text
ProductQueryRequest
    -> ProductQuerySpecification
        -> apply(LambdaQueryWrapper<ProductEntity>)
            -> ProductMapper.selectPage(...)
```

### 文件结构

```text
src/main/java/io/github/atengk/design/
├── dto/
│   ├── PageResult.java
│   ├── ProductQueryRequest.java
│   └── ProductResponse.java
├── entity/
│   └── ProductEntity.java
├── mapper/
│   └── ProductMapper.java
├── queryspec/
│   ├── QuerySpecification.java
│   └── ProductQuerySpecification.java
├── service/
│   ├── ProductQueryService.java
│   └── impl/
│       └── ProductQueryServiceImpl.java
└── controller/
    └── ProductQueryController.java
```

文件位置：`src/main/java/io/github/atengk/design/entity/ProductEntity.java`

下面是商品持久化实体，对应 `product` 表。

```java
package io.github.atengk.design.entity;

import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 商品持久化实体
 *
 * @author Ateng
 * @since 2026-05-01
 */
@Data
@TableName("product")
public class ProductEntity {

    /**
     * 商品ID
     */
    @TableId
    private Long id;

    /**
     * 商品编码
     */
    private String productCode;

    /**
     * 商品名称
     */
    private String productName;

    /**
     * 分类编码
     */
    private String categoryCode;

    /**
     * 商品价格
     */
    private BigDecimal price;

    /**
     * 库存数量
     */
    private Integer stock;

    /**
     * 商品状态
     */
    private String status;

    /**
     * 创建时间
     */
    private LocalDateTime createTime;

    /**
     * 更新时间
     */
    private LocalDateTime updateTime;
}
```

文件位置：`src/main/java/io/github/atengk/design/mapper/ProductMapper.java`

下面是商品 Mapper。

```java
package io.github.atengk.design.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import io.github.atengk.design.entity.ProductEntity;

/**
 * 商品Mapper
 *
 * @author Ateng
 * @since 2026-05-01
 */
public interface ProductMapper extends BaseMapper<ProductEntity> {
}
```

文件位置：`src/main/java/io/github/atengk/design/dto/ProductQueryRequest.java`

下面是商品查询请求对象。

```java
package io.github.atengk.design.dto;

import java.math.BigDecimal;

/**
 * 商品查询请求
 *
 * @param pageNum      页码
 * @param pageSize     每页大小
 * @param productName  商品名称
 * @param categoryCode 分类编码
 * @param minPrice     最低价格
 * @param maxPrice     最高价格
 * @param onlyOnShelf  是否只查询上架商品
 * @author Ateng
 * @since 2026-05-01
 */
public record ProductQueryRequest(
        Long pageNum,
        Long pageSize,
        String productName,
        String categoryCode,
        BigDecimal minPrice,
        BigDecimal maxPrice,
        Boolean onlyOnShelf
) {
}
```

文件位置：`src/main/java/io/github/atengk/design/dto/ProductResponse.java`

下面是商品响应对象。

```java
package io.github.atengk.design.dto;

import java.math.BigDecimal;

/**
 * 商品响应
 *
 * @param id           商品ID
 * @param productCode  商品编码
 * @param productName  商品名称
 * @param categoryCode 分类编码
 * @param price        商品价格
 * @param stock        库存数量
 * @param status       商品状态
 * @author Ateng
 * @since 2026-05-01
 */
public record ProductResponse(
        Long id,
        String productCode,
        String productName,
        String categoryCode,
        BigDecimal price,
        Integer stock,
        String status
) {
}
```

文件位置：`src/main/java/io/github/atengk/design/dto/PageResult.java`

下面是分页结果对象。

```java
package io.github.atengk.design.dto;

import java.util.List;

/**
 * 分页结果
 *
 * @param records  数据列表
 * @param pageNum  当前页码
 * @param pageSize 每页大小
 * @param total    总数量
 * @param <T>      数据类型
 * @author Ateng
 * @since 2026-05-01
 */
public record PageResult<T>(
        List<T> records,
        Long pageNum,
        Long pageSize,
        Long total
) {
}
```

文件位置：`src/main/java/io/github/atengk/design/queryspec/QuerySpecification.java`

下面是查询规格接口，用于把规格应用到查询构造器。

```java
package io.github.atengk.design.queryspec;

/**
 * 查询规格接口
 *
 * @param <W> 查询构造器类型
 * @author Ateng
 * @since 2026-05-01
 */
public interface QuerySpecification<W> {

    /**
     * 应用查询规格
     *
     * @param wrapper 查询构造器
     */
    void apply(W wrapper);
}
```

文件位置：`src/main/java/io/github/atengk/design/queryspec/ProductQuerySpecification.java`

下面是商品查询规格。它把商品名称、分类、价格区间、状态条件封装到一个对象中。

```java
package io.github.atengk.design.queryspec;

import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import io.github.atengk.design.dto.ProductQueryRequest;
import io.github.atengk.design.entity.ProductEntity;
import lombok.extern.slf4j.Slf4j;

import java.math.BigDecimal;

/**
 * 商品查询规格
 *
 * @author Ateng
 * @since 2026-05-01
 */
@Slf4j
public class ProductQuerySpecification implements QuerySpecification<LambdaQueryWrapper<ProductEntity>> {

    private final ProductQueryRequest request;

    /**
     * 创建商品查询规格
     *
     * @param request 商品查询请求
     */
    public ProductQuerySpecification(ProductQueryRequest request) {
        this.request = request;
    }

    /**
     * 应用查询规格
     *
     * @param wrapper 查询构造器
     */
    @Override
    public void apply(LambdaQueryWrapper<ProductEntity> wrapper) {
        if (wrapper == null) {
            throw new IllegalArgumentException("查询构造器不能为空");
        }

        if (request == null) {
            log.info("商品查询规格为空，使用默认查询条件");
            return;
        }

        if (StrUtil.isNotBlank(request.productName())) {
            wrapper.like(ProductEntity::getProductName, request.productName());
        }

        if (StrUtil.isNotBlank(request.categoryCode())) {
            wrapper.eq(ProductEntity::getCategoryCode, request.categoryCode());
        }

        if (request.minPrice() != null) {
            validatePrice(request.minPrice(), "最低价格");
            wrapper.ge(ProductEntity::getPrice, request.minPrice());
        }

        if (request.maxPrice() != null) {
            validatePrice(request.maxPrice(), "最高价格");
            wrapper.le(ProductEntity::getPrice, request.maxPrice());
        }

        if (Boolean.TRUE.equals(request.onlyOnShelf())) {
            wrapper.eq(ProductEntity::getStatus, "ON_SHELF");
        }

        wrapper.orderByDesc(ProductEntity::getCreateTime);
        log.info("应用商品查询规格完成，商品名称：{}，分类：{}，最低价：{}，最高价：{}，只看上架：{}",
                request.productName(), request.categoryCode(), request.minPrice(), request.maxPrice(), request.onlyOnShelf());
    }

    /**
     * 校验价格
     *
     * @param price 价格
     * @param name  字段名称
     */
    private void validatePrice(BigDecimal price, String name) {
        if (price.compareTo(BigDecimal.ZERO) < 0) {
            log.warn("商品查询规格校验失败，{}不能小于0，值：{}", name, price);
            throw new IllegalArgumentException(name + "不能小于0");
        }
    }
}
```

文件位置：`src/main/java/io/github/atengk/design/service/ProductQueryService.java`

下面是商品查询服务接口。

```java
package io.github.atengk.design.service;

import io.github.atengk.design.dto.PageResult;
import io.github.atengk.design.dto.ProductQueryRequest;
import io.github.atengk.design.dto.ProductResponse;

/**
 * 商品查询服务
 *
 * @author Ateng
 * @since 2026-05-01
 */
public interface ProductQueryService {

    /**
     * 分页查询商品
     *
     * @param request 商品查询请求
     * @return 商品分页结果
     */
    PageResult<ProductResponse> page(ProductQueryRequest request);
}
```

文件位置：`src/main/java/io/github/atengk/design/service/impl/ProductQueryServiceImpl.java`

下面是商品查询服务实现。它通过查询规格构建动态 SQL 条件。

```java
package io.github.atengk.design.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import io.github.atengk.design.dto.PageResult;
import io.github.atengk.design.dto.ProductQueryRequest;
import io.github.atengk.design.dto.ProductResponse;
import io.github.atengk.design.entity.ProductEntity;
import io.github.atengk.design.mapper.ProductMapper;
import io.github.atengk.design.queryspec.ProductQuerySpecification;
import io.github.atengk.design.service.ProductQueryService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

/**
 * 商品查询服务实现
 *
 * @author Ateng
 * @since 2026-05-01
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ProductQueryServiceImpl implements ProductQueryService {

    private final ProductMapper productMapper;

    /**
     * 分页查询商品
     *
     * @param request 商品查询请求
     * @return 商品分页结果
     */
    @Override
    public PageResult<ProductResponse> page(ProductQueryRequest request) {
        long pageNum = request == null || request.pageNum() == null || request.pageNum() <= 0 ? 1L : request.pageNum();
        long pageSize = request == null || request.pageSize() == null || request.pageSize() <= 0 ? 10L : request.pageSize();

        LambdaQueryWrapper<ProductEntity> wrapper = new LambdaQueryWrapper<>();
        new ProductQuerySpecification(request).apply(wrapper);

        Page<ProductEntity> entityPage = productMapper.selectPage(new Page<>(pageNum, pageSize), wrapper);

        log.info("商品规格分页查询完成，页码：{}，每页大小：{}，总数：{}",
                entityPage.getCurrent(), entityPage.getSize(), entityPage.getTotal());

        return new PageResult<>(
                entityPage.getRecords().stream().map(this::toResponse).toList(),
                entityPage.getCurrent(),
                entityPage.getSize(),
                entityPage.getTotal()
        );
    }

    /**
     * 转换为商品响应
     *
     * @param entity 商品实体
     * @return 商品响应
     */
    private ProductResponse toResponse(ProductEntity entity) {
        return new ProductResponse(
                entity.getId(),
                entity.getProductCode(),
                entity.getProductName(),
                entity.getCategoryCode(),
                entity.getPrice(),
                entity.getStock(),
                entity.getStatus()
        );
    }
}
```

文件位置：`src/main/java/io/github/atengk/design/controller/ProductQueryController.java`

下面是商品查询接口，用于验证查询规格模式。

```java
package io.github.atengk.design.controller;

import io.github.atengk.design.dto.PageResult;
import io.github.atengk.design.dto.ProductQueryRequest;
import io.github.atengk.design.dto.ProductResponse;
import io.github.atengk.design.service.ProductQueryService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;

/**
 * 商品查询控制器
 *
 * @author Ateng
 * @since 2026-05-01
 */
@RestController
@RequiredArgsConstructor
@RequestMapping("/specification/product")
public class ProductQueryController {

    private final ProductQueryService productQueryService;

    /**
     * 分页查询商品
     *
     * @param pageNum      页码
     * @param pageSize     每页大小
     * @param productName  商品名称
     * @param categoryCode 分类编码
     * @param minPrice     最低价格
     * @param maxPrice     最高价格
     * @param onlyOnShelf  是否只看上架商品
     * @return 商品分页结果
     */
    @GetMapping("/page")
    public PageResult<ProductResponse> page(@RequestParam(defaultValue = "1") Long pageNum,
                                            @RequestParam(defaultValue = "10") Long pageSize,
                                            @RequestParam(required = false) String productName,
                                            @RequestParam(required = false) String categoryCode,
                                            @RequestParam(required = false) BigDecimal minPrice,
                                            @RequestParam(required = false) BigDecimal maxPrice,
                                            @RequestParam(defaultValue = "false") Boolean onlyOnShelf) {
        ProductQueryRequest request = new ProductQueryRequest(
                pageNum,
                pageSize,
                productName,
                categoryCode,
                minPrice,
                maxPrice,
                onlyOnShelf
        );

        return productQueryService.page(request);
    }
}
```

## 使用方式

启动项目之前，先创建数据库和商品表。

```bash
mysql -uroot -proot -e "CREATE DATABASE IF NOT EXISTS design_demo DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;"
mysql -uroot -proot design_demo < sql/product.sql
```

插入测试商品数据：

```sql
INSERT INTO product (
    id, product_code, product_name, category_code, price, stock, status, create_time, update_time
) VALUES
(10001, 'P10001', '机械键盘', 'digital', 199.00, 100, 'ON_SHELF', NOW(), NOW()),
(10002, 'P10002', '无线鼠标', 'digital', 99.00, 200, 'ON_SHELF', NOW(), NOW()),
(10003, 'P10003', '办公椅', 'office', 399.00, 50, 'DRAFT', NOW(), NOW()),
(10004, 'P10004', '显示器', 'digital', 1299.00, 20, 'OFF_SHELF', NOW(), NOW());
```

启动 Spring Boot 项目：

```bash
mvn spring-boot:run
```

验证优惠券业务规则规格：

```bash
curl -X POST "http://localhost:8080/specification/coupon/receive?couponNo=C10001&userId=10001&userLevel=VIP&orderAmount=199.00"
```

可能返回：

```json
{
  "couponNo": "C10001",
  "userId": 10001,
  "received": true,
  "message": "领取成功"
}
```

验证不满足用户等级的情况：

```bash
curl -X POST "http://localhost:8080/specification/coupon/receive?couponNo=C10001&userId=10002&userLevel=NORMAL&orderAmount=199.00"
```

可能返回：

```json
{
  "couponNo": "C10001",
  "userId": 10002,
  "received": false,
  "message": "不满足优惠券领取条件"
}
```

验证商品查询规格：

```bash
curl "http://localhost:8080/specification/product/page?pageNum=1&pageSize=10&productName=键盘&categoryCode=digital&minPrice=100&maxPrice=500&onlyOnShelf=true"
```

可能返回：

```json
{
  "records": [
    {
      "id": 10001,
      "productCode": "P10001",
      "productName": "机械键盘",
      "categoryCode": "digital",
      "price": 199.00,
      "stock": 100,
      "status": "ON_SHELF"
    }
  ],
  "pageNum": 1,
  "pageSize": 10,
  "total": 1
}
```

如果规格模式正常，可以看到类似日志：

```text
创建优惠券领取组合规格完成
优惠券有效期规格校验完成，优惠券编号：C10001，结果：true
优惠券库存规格校验完成，优惠券编号：C10001，库存：10，结果：true
优惠券用户等级规格校验完成，用户ID：10001，当前等级：VIP，要求等级：VIP，结果：true
优惠券金额门槛规格校验完成，订单金额：199.00，门槛金额：100，结果：true
领取优惠券成功，优惠券编号：C10001，用户ID：10001
应用商品查询规格完成，商品名称：键盘，分类：digital，最低价：100，最高价：500，只看上架：true
商品规格分页查询完成，页码：1，每页大小：10，总数：1
```

## 规格模式和策略模式的区别

规格模式和策略模式都可以减少 `if else`，但它们解决的问题不同。

| 对比项       | 规格模式                       | 策略模式                     |
| ------------ | ------------------------------ | ---------------------------- |
| 核心目的     | 判断对象是否满足某个规则       | 选择某种算法执行             |
| 结果类型     | 通常是 boolean 或查询条件      | 任意业务结果                 |
| 是否强调组合 | 强调 `and`、`or`、`not`        | 不强调                       |
| 典型场景     | 优惠券条件、风控条件、查询条件 | 支付渠道、优惠计算、物流计费 |
| 规则关系     | 多个规则可以叠加               | 多个策略通常互斥选择         |

简单理解：

```text
规格模式：这个对象是否满足这些条件。
策略模式：这个场景应该用哪种算法处理。
```

例如“订单金额是否达到 100、用户是否是 VIP、是否首单”适合规格模式。
“满减、折扣、立减选择哪一种优惠算法”更适合策略模式。

## 规格模式和解释器模式的区别

规格模式和解释器模式都可以处理规则，但规则来源不同。

| 对比项       | 规格模式                        | 解释器模式                          |
| ------------ | ------------------------------- | ----------------------------------- |
| 规则表达方式 | Java 对象组合                   | 字符串表达式或语法树                |
| 使用方式     | `specA.and(specB)`              | 解析 `a && b`                       |
| 适用场景     | 规则由开发定义                  | 规则需要配置化表达                  |
| 复杂度       | 中低                            | 中高                                |
| 典型例子     | `AmountSpec.and(UserLevelSpec)` | `amount >= 100 && userLevel == VIP` |

简单理解：

```text
规格模式：用对象表达规则。
解释器模式：用表达式字符串表达规则。
```

如果规则由开发人员维护，规格模式更安全、更容易测试。
如果规则需要配置到数据库或后台页面中，由业务人员编辑，就可能需要解释器模式或成熟规则引擎。

## 规格模式和责任链模式的区别

规格模式和责任链模式都可以处理多个条件，但组织方式不同。

| 对比项       | 规格模式               | 责任链模式                     |
| ------------ | ---------------------- | ------------------------------ |
| 核心目的     | 组合判断条件           | 按顺序处理请求                 |
| 执行结构     | 规则组合树             | 处理器链                       |
| 是否强调顺序 | 不强                   | 强                             |
| 是否强调中断 | 不强，但可短路         | 强                             |
| 典型场景     | 是否满足优惠券领取条件 | 参数校验、风控校验、审批流节点 |

简单理解：

```text
规格模式：规则是否满足。
责任链模式：请求经过哪些处理节点。
```

优惠券“有效期、库存、等级、金额”组合判断适合规格模式。
订单提交前“参数校验、库存校验、金额校验、风控校验”按顺序处理，更适合责任链模式。

## 注意事项

规格模式适合规则复用和规则组合，但不要为了一个简单判断强行拆类。如果规则只有一两个，并且不会复用，普通 `if` 更直接。

适合使用规格模式的场景：

```text
优惠券领取条件
订单风控条件
商品上下架条件
会员权益可用条件
动态查询条件
权限规则组合
领域对象复杂校验
多个规则需要 and / or / not 组合
```

不太适合使用规格模式的场景：

```text
只有一个简单判断
规则不会复用
规则没有组合需求
使用规格后类数量明显膨胀
规则本质是算法选择而不是条件判断
```

不要把规格类写成上帝规则类。

不推荐：

```java
public class CouponReceiveSpecification {

    public boolean check(CouponReceiveContext context) {
        // 校验有效期
        // 校验库存
        // 校验用户等级
        // 校验金额
        // 校验黑名单
        // 校验地区
        // 校验渠道
        return true;
    }
}
```

推荐拆成多个独立规格：

```text
CouponActiveSpecification
CouponStockSpecification
CouponUserLevelSpecification
CouponAmountSpecification
CouponChannelSpecification
CouponBlacklistSpecification
```

规格对象最好保持无状态或不可变。规格可以持有固定配置，例如门槛金额、要求等级，但不要保存请求级状态。

错误示例：

```java
private Long currentUserId;
private BigDecimal currentOrderAmount;
```

推荐通过候选对象传入：

```java
public boolean isSatisfiedBy(CouponReceiveContext candidate) {
    return candidate.orderAmount().compareTo(candidate.thresholdAmount()) >= 0;
}
```

如果规格校验失败需要返回具体原因，单纯 boolean 可能不够。可以扩展为规格结果对象：

```java
public record SpecificationResult(
        Boolean satisfied,
        String reason
) {
}
```

这样可以返回更明确的失败原因：

```text
优惠券库存不足
订单金额未达到门槛
用户等级不符合要求
优惠券不在有效期内
```

但如果只是简单判断，boolean 更轻量。

查询规格要注意 SQL 性能。规格只是组织查询条件，不会自动解决索引问题。常见关注点包括：

```text
like 查询是否命中索引
范围查询是否有合适索引
组合条件是否过多
分页深度是否过大
排序字段是否有索引
```

业务规则规格不要访问数据库。规格最好只判断传入对象。如果规格内部频繁查库，会导致规则难测试、性能不可控。

不推荐：

```java
public boolean isSatisfiedBy(CouponReceiveContext context) {
    User user = userMapper.selectById(context.userId());
    return "VIP".equals(user.getLevel());
}
```

推荐在 Service 中准备好上下文数据，再交给规格判断：

```java
CouponReceiveContext context = new CouponReceiveContext(...);
boolean satisfied = specification.isSatisfiedBy(context);
```

## 总结

在 JDK21 和 Spring Boot 3 项目中，规格模式的实践重点是把业务规则对象化，并通过组合方式表达复杂规则，避免规则散落在大量 `if else` 中。

普通 Java 规格模式适合理解规则封装、规则复用和 `and`、`or`、`not` 组合。Spring Boot 项目中，规格模式常用于优惠券、风控、商品状态、会员权益、权限判断等业务规则，也可以用于封装 MyBatis-Plus 动态查询条件。推荐使用“规格接口 + 具体规格 + 组合规格 + 上下文对象 + 应用服务”的结构。

规格模式不是策略模式，也不是解释器模式。它最适合处理“对象是否满足某些可组合条件”的场景。实际落地时，需要重点控制规格粒度、失败原因表达、规则是否访问外部资源、查询性能和类数量膨胀问题。
