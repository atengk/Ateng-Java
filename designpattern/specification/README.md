# 规格模式

规格模式是 Spring Boot 项目中常用的业务规则组织方式，属于当前设计模式文档体系中的 **Spring Boot 实战补充模式**。它的核心作用是把业务规则封装成可组合、可复用的规格对象，尤其适合处理复杂判断条件、准入规则、优惠规则、风控规则和查询条件组合。

在实际项目中，规格模式常用于优惠券领取资格、订单风控校验、会员权益判断、商品上下架条件、审批准入条件、用户分群规则等场景。它的重点不是减少代码行数，而是把散落在 `if else` 中的规则拆成独立对象，让规则可以组合、复用、测试和扩展。

## 基础配置

本示例基于 **JDK 21 + Spring Boot 3**，使用 Spring Web 提供接口，使用 Validation 做请求参数校验，使用 Hutool 简化字符串、对象和集合处理。示例业务以“优惠券领取资格校验”为场景。

优惠券领取规则如下：

```text
1. 优惠券必须可用。
2. 用户必须是正常状态。
3. 用户会员等级必须满足优惠券要求。
4. 用户历史订单数必须满足要求。
5. 用户历史消费金额必须满足要求。
```

如果把这些规则全部写在 Service 中，代码会快速变成大量 `if else`。使用规格模式后，每个规则都是一个独立规格对象，Service 只负责组装上下文、调用规格并处理结果。

### 项目依赖

文件位置：`pom.xml`

下面配置 Web、Validation、Hutool 和 Lombok，满足接口、参数校验、工具类和日志对象使用需求。

```xml
<dependencies>
    <!-- Spring Web：提供 REST API 能力 -->
    <dependency>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-starter-web</artifactId>
    </dependency>

    <!-- Spring Validation：用于接口请求参数校验 -->
    <dependency>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-starter-validation</artifactId>
    </dependency>

    <!-- Hutool：常用工具类，简化字符串、对象、集合、日期等处理 -->
    <dependency>
        <groupId>cn.hutool</groupId>
        <artifactId>hutool-all</artifactId>
        <version>5.8.35</version>
    </dependency>

    <!-- Lombok：减少构造器、日志对象、Getter 等样板代码 -->
    <dependency>
        <groupId>org.projectlombok</groupId>
        <artifactId>lombok</artifactId>
        <optional>true</optional>
    </dependency>
</dependencies>
```

### 应用配置

文件位置：`src/main/resources/application.yml`

本示例先使用内存数据模拟用户和优惠券模板，暂不引入数据库。真实项目中可以把用户、优惠券、订单统计数据改为从 Repository、Mapper、Redis 或远程服务读取。

```yaml
spring:
  application:
    name: design-pattern-specification

server:
  port: 8080
```

### 文件结构

规格模式建议把通用规格接口、组合规格、业务规格和业务调用方分开。这样规则既可以独立测试，也可以被多个业务流程复用。

```text
src/main/java/io/github/atengk/specification
├── SpecificationApplication.java
├── controller
│   └── CouponController.java
├── domain
│   ├── model
│   │   ├── CouponReceiveContext.java
│   │   ├── CouponTemplate.java
│   │   └── UserProfile.java
│   └── specification
│       ├── AndSpecification.java
│       ├── NotSpecification.java
│       ├── OrSpecification.java
│       ├── Specification.java
│       └── SpecificationResult.java
├── dto
│   └── CouponReceiveRequest.java
├── service
│   ├── CouponReceiveService.java
│   └── impl
│       └── CouponReceiveServiceImpl.java
├── specification
│   ├── CouponAvailableSpecification.java
│   ├── CouponReceiveSpecificationFactory.java
│   ├── MemberLevelSpecification.java
│   ├── OrderCountSpecification.java
│   ├── TotalAmountSpecification.java
│   └── UserStatusSpecification.java
└── vo
    └── CouponReceiveResponse.java
```

## 模式说明

规格模式的核心思想是：把一个业务条件封装成一个规格对象，然后通过 `and`、`or`、`not` 等方式组合多个规格。

普通写法通常是：

```text
Service
 -> if 优惠券不可用，返回失败
 -> if 用户状态异常，返回失败
 -> if 会员等级不足，返回失败
 -> if 订单数不足，返回失败
 -> if 消费金额不足，返回失败
 -> 领取优惠券
```

规格模式写法是：

```text
Service
 -> 构建 CouponReceiveContext
 -> 调用 CouponReceiveSpecification
 -> 返回校验结果
 -> 校验通过后领取优惠券
```

规则本身拆成：

```text
CouponAvailableSpecification
UserStatusSpecification
MemberLevelSpecification
OrderCountSpecification
TotalAmountSpecification
```

每个规格只负责一个判断条件。新增规则时，通常只需要新增一个规格类，并在工厂类中组合进去，不需要在 Service 中继续堆叠复杂分支。

## 核心代码

这一节给出规格模式的完整关键代码。示例重点体现三点：

```text
1. 单个规则封装为独立 Specification。
2. 多个规则通过 AndSpecification、OrSpecification、NotSpecification 组合。
3. Service 只负责调用规格，不直接堆叠复杂 if else。
```

### 启动类

文件位置：`src/main/java/io/github/atengk/specification/SpecificationApplication.java`

启动类用于启动 Spring Boot 应用。

```java
package io.github.atengk.specification;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * 规格模式示例启动类
 *
 * @author Ateng
 * @since 2026-05-13
 */
@SpringBootApplication
public class SpecificationApplication {

    public static void main(String[] args) {
        SpringApplication.run(SpecificationApplication.class, args);
    }

}
```

### 规格结果对象

文件位置：`src/main/java/io/github/atengk/specification/domain/specification/SpecificationResult.java`

该对象用于表达规格校验结果。相比只返回 `boolean`，它可以携带失败原因，更适合接口返回、日志记录和问题排查。

```java
package io.github.atengk.specification.domain.specification;

import cn.hutool.core.util.StrUtil;

/**
 * 规格校验结果
 *
 * @author Ateng
 * @since 2026-05-13
 */
public record SpecificationResult(

        boolean passed,

        String message

) {

    public static SpecificationResult pass() {
        return new SpecificationResult(true, "校验通过");
    }

    public static SpecificationResult fail(String message) {
        return new SpecificationResult(false, StrUtil.blankToDefault(message, "校验未通过"));
    }

    public static SpecificationResult fail(String messageTemplate, Object... params) {
        return fail(StrUtil.format(messageTemplate, params));
    }

}
```

### 规格接口

文件位置：`src/main/java/io/github/atengk/specification/domain/specification/Specification.java`

该接口是规格模式的核心抽象。所有具体规则都实现它，组合规格也实现它。

```java
package io.github.atengk.specification.domain.specification;

import java.util.Arrays;

/**
 * 规格接口
 *
 * @author Ateng
 * @since 2026-05-13
 */
public interface Specification<T> {

    SpecificationResult check(T context);

    default Specification<T> and(Specification<T> other) {
        return Specification.allOf(this, other);
    }

    default Specification<T> or(Specification<T> other) {
        return Specification.anyOf(this, other);
    }

    default Specification<T> not(String message) {
        return new NotSpecification<>(this, message);
    }

    @SafeVarargs
    static <T> Specification<T> allOf(Specification<T>... specifications) {
        return new AndSpecification<>(Arrays.asList(specifications));
    }

    @SafeVarargs
    static <T> Specification<T> anyOf(Specification<T>... specifications) {
        return new OrSpecification<>(Arrays.asList(specifications));
    }

}
```

### AND 组合规格

文件位置：`src/main/java/io/github/atengk/specification/domain/specification/AndSpecification.java`

该规格用于表达多个规则必须全部满足的场景。优惠券领取资格通常就是 AND 组合。

```java
package io.github.atengk.specification.domain.specification;

import cn.hutool.core.collection.CollUtil;

import java.util.List;

/**
 * AND 组合规格
 *
 * @author Ateng
 * @since 2026-05-13
 */
public class AndSpecification<T> implements Specification<T> {

    private final List<Specification<T>> specifications;

    public AndSpecification(List<Specification<T>> specifications) {
        this.specifications = CollUtil.emptyIfNull(specifications);
    }

    @Override
    public SpecificationResult check(T context) {
        if (CollUtil.isEmpty(specifications)) {
            return SpecificationResult.pass();
        }

        for (Specification<T> specification : specifications) {
            if (specification == null) {
                continue;
            }

            SpecificationResult result = specification.check(context);
            if (!result.passed()) {
                return result;
            }
        }

        return SpecificationResult.pass();
    }

}
```

### OR 组合规格

文件位置：`src/main/java/io/github/atengk/specification/domain/specification/OrSpecification.java`

该规格用于表达多个规则满足任意一个即可通过的场景，例如“会员等级满足或白名单用户满足”。

```java
package io.github.atengk.specification.domain.specification;

import cn.hutool.core.collection.CollUtil;

import java.util.ArrayList;
import java.util.List;

/**
 * OR 组合规格
 *
 * @author Ateng
 * @since 2026-05-13
 */
public class OrSpecification<T> implements Specification<T> {

    private final List<Specification<T>> specifications;

    public OrSpecification(List<Specification<T>> specifications) {
        this.specifications = CollUtil.emptyIfNull(specifications);
    }

    @Override
    public SpecificationResult check(T context) {
        if (CollUtil.isEmpty(specifications)) {
            return SpecificationResult.fail("没有可用的 OR 规格");
        }

        List<String> failMessages = new ArrayList<>();
        for (Specification<T> specification : specifications) {
            if (specification == null) {
                continue;
            }

            SpecificationResult result = specification.check(context);
            if (result.passed()) {
                return SpecificationResult.pass();
            }
            failMessages.add(result.message());
        }

        return SpecificationResult.fail("所有可选规格均未满足：{}", CollUtil.join(failMessages, "；"));
    }

}
```

### NOT 组合规格

文件位置：`src/main/java/io/github/atengk/specification/domain/specification/NotSpecification.java`

该规格用于表达反向规则，例如“不能是黑名单用户”“不能是已领取用户”。

```java
package io.github.atengk.specification.domain.specification;

import cn.hutool.core.util.StrUtil;

/**
 * NOT 组合规格
 *
 * @author Ateng
 * @since 2026-05-13
 */
public class NotSpecification<T> implements Specification<T> {

    private final Specification<T> specification;

    private final String message;

    public NotSpecification(Specification<T> specification, String message) {
        this.specification = specification;
        this.message = StrUtil.blankToDefault(message, "不满足取反规格");
    }

    @Override
    public SpecificationResult check(T context) {
        if (specification == null) {
            return SpecificationResult.pass();
        }

        SpecificationResult result = specification.check(context);
        if (result.passed()) {
            return SpecificationResult.fail(message);
        }

        return SpecificationResult.pass();
    }

}
```

## 领域对象

本示例中的规格不是直接判断接口 DTO，而是判断业务上下文 `CouponReceiveContext`。这样可以避免规则对象依赖外部接口参数结构。

### 用户画像对象

文件位置：`src/main/java/io/github/atengk/specification/domain/model/UserProfile.java`

该对象表示用户参与优惠券领取校验时需要的关键画像数据。

```java
package io.github.atengk.specification.domain.model;

import java.math.BigDecimal;

/**
 * 用户画像对象
 *
 * @author Ateng
 * @since 2026-05-13
 */
public record UserProfile(

        String userId,

        String username,

        String status,

        Integer memberLevel,

        Integer orderCount,

        BigDecimal totalAmount

) {
}
```

### 优惠券模板对象

文件位置：`src/main/java/io/github/atengk/specification/domain/model/CouponTemplate.java`

该对象表示优惠券模板及其领取门槛。

```java
package io.github.atengk.specification.domain.model;

import java.math.BigDecimal;

/**
 * 优惠券模板对象
 *
 * @author Ateng
 * @since 2026-05-13
 */
public record CouponTemplate(

        String couponId,

        String couponName,

        Boolean available,

        Integer requiredMemberLevel,

        Integer minOrderCount,

        BigDecimal minTotalAmount

) {
}
```

### 领取上下文对象

文件位置：`src/main/java/io/github/atengk/specification/domain/model/CouponReceiveContext.java`

该对象是规格校验的输入上下文，聚合了用户画像和优惠券模板。

```java
package io.github.atengk.specification.domain.model;

/**
 * 优惠券领取上下文
 *
 * @author Ateng
 * @since 2026-05-13
 */
public record CouponReceiveContext(

        UserProfile userProfile,

        CouponTemplate couponTemplate

) {
}
```

## 业务规格实现

这一节给出优惠券领取资格中的具体规则。每个规格只处理一个判断条件，保持单一职责。

### 优惠券可用规格

文件位置：`src/main/java/io/github/atengk/specification/specification/CouponAvailableSpecification.java`

该规格判断优惠券模板是否存在并且处于可用状态。

```java
package io.github.atengk.specification.specification;

import cn.hutool.core.util.ObjectUtil;
import io.github.atengk.specification.domain.model.CouponReceiveContext;
import io.github.atengk.specification.domain.model.CouponTemplate;
import io.github.atengk.specification.domain.specification.Specification;
import io.github.atengk.specification.domain.specification.SpecificationResult;

/**
 * 优惠券可用规格
 *
 * @author Ateng
 * @since 2026-05-13
 */
public class CouponAvailableSpecification implements Specification<CouponReceiveContext> {

    @Override
    public SpecificationResult check(CouponReceiveContext context) {
        if (ObjectUtil.isNull(context) || ObjectUtil.isNull(context.couponTemplate())) {
            return SpecificationResult.fail("优惠券不存在");
        }

        CouponTemplate couponTemplate = context.couponTemplate();
        if (!Boolean.TRUE.equals(couponTemplate.available())) {
            return SpecificationResult.fail("优惠券不可用，couponId={}", couponTemplate.couponId());
        }

        return SpecificationResult.pass();
    }

}
```

### 用户状态规格

文件位置：`src/main/java/io/github/atengk/specification/specification/UserStatusSpecification.java`

该规格判断用户是否存在并且处于正常状态。

```java
package io.github.atengk.specification.specification;

import cn.hutool.core.util.ObjectUtil;
import cn.hutool.core.util.StrUtil;
import io.github.atengk.specification.domain.model.CouponReceiveContext;
import io.github.atengk.specification.domain.model.UserProfile;
import io.github.atengk.specification.domain.specification.Specification;
import io.github.atengk.specification.domain.specification.SpecificationResult;

/**
 * 用户状态规格
 *
 * @author Ateng
 * @since 2026-05-13
 */
public class UserStatusSpecification implements Specification<CouponReceiveContext> {

    @Override
    public SpecificationResult check(CouponReceiveContext context) {
        if (ObjectUtil.isNull(context) || ObjectUtil.isNull(context.userProfile())) {
            return SpecificationResult.fail("用户不存在");
        }

        UserProfile userProfile = context.userProfile();
        if (!StrUtil.equals("NORMAL", userProfile.status())) {
            return SpecificationResult.fail("用户状态异常，userId={}，status={}", userProfile.userId(), userProfile.status());
        }

        return SpecificationResult.pass();
    }

}
```

### 会员等级规格

文件位置：`src/main/java/io/github/atengk/specification/specification/MemberLevelSpecification.java`

该规格判断用户会员等级是否达到优惠券要求。

```java
package io.github.atengk.specification.specification;

import cn.hutool.core.util.ObjectUtil;
import io.github.atengk.specification.domain.model.CouponReceiveContext;
import io.github.atengk.specification.domain.model.CouponTemplate;
import io.github.atengk.specification.domain.model.UserProfile;
import io.github.atengk.specification.domain.specification.Specification;
import io.github.atengk.specification.domain.specification.SpecificationResult;

/**
 * 会员等级规格
 *
 * @author Ateng
 * @since 2026-05-13
 */
public class MemberLevelSpecification implements Specification<CouponReceiveContext> {

    @Override
    public SpecificationResult check(CouponReceiveContext context) {
        if (ObjectUtil.isNull(context) || ObjectUtil.isNull(context.userProfile()) || ObjectUtil.isNull(context.couponTemplate())) {
            return SpecificationResult.fail("会员等级校验数据不完整");
        }

        UserProfile userProfile = context.userProfile();
        CouponTemplate couponTemplate = context.couponTemplate();

        int memberLevel = ObjectUtil.defaultIfNull(userProfile.memberLevel(), 0);
        int requiredMemberLevel = ObjectUtil.defaultIfNull(couponTemplate.requiredMemberLevel(), 0);

        if (memberLevel < requiredMemberLevel) {
            return SpecificationResult.fail(
                    "会员等级不足，当前等级={}，要求等级={}",
                    memberLevel,
                    requiredMemberLevel
            );
        }

        return SpecificationResult.pass();
    }

}
```

### 历史订单数规格

文件位置：`src/main/java/io/github/atengk/specification/specification/OrderCountSpecification.java`

该规格判断用户历史订单数是否达到优惠券要求。

```java
package io.github.atengk.specification.specification;

import cn.hutool.core.util.ObjectUtil;
import io.github.atengk.specification.domain.model.CouponReceiveContext;
import io.github.atengk.specification.domain.model.CouponTemplate;
import io.github.atengk.specification.domain.model.UserProfile;
import io.github.atengk.specification.domain.specification.Specification;
import io.github.atengk.specification.domain.specification.SpecificationResult;

/**
 * 历史订单数规格
 *
 * @author Ateng
 * @since 2026-05-13
 */
public class OrderCountSpecification implements Specification<CouponReceiveContext> {

    @Override
    public SpecificationResult check(CouponReceiveContext context) {
        if (ObjectUtil.isNull(context) || ObjectUtil.isNull(context.userProfile()) || ObjectUtil.isNull(context.couponTemplate())) {
            return SpecificationResult.fail("历史订单数校验数据不完整");
        }

        UserProfile userProfile = context.userProfile();
        CouponTemplate couponTemplate = context.couponTemplate();

        int orderCount = ObjectUtil.defaultIfNull(userProfile.orderCount(), 0);
        int minOrderCount = ObjectUtil.defaultIfNull(couponTemplate.minOrderCount(), 0);

        if (orderCount < minOrderCount) {
            return SpecificationResult.fail(
                    "历史订单数不足，当前订单数={}，要求订单数={}",
                    orderCount,
                    minOrderCount
            );
        }

        return SpecificationResult.pass();
    }

}
```

### 历史消费金额规格

文件位置：`src/main/java/io/github/atengk/specification/specification/TotalAmountSpecification.java`

该规格判断用户历史消费金额是否达到优惠券要求。

```java
package io.github.atengk.specification.specification;

import cn.hutool.core.util.ObjectUtil;
import io.github.atengk.specification.domain.model.CouponReceiveContext;
import io.github.atengk.specification.domain.model.CouponTemplate;
import io.github.atengk.specification.domain.model.UserProfile;
import io.github.atengk.specification.domain.specification.Specification;
import io.github.atengk.specification.domain.specification.SpecificationResult;

import java.math.BigDecimal;

/**
 * 历史消费金额规格
 *
 * @author Ateng
 * @since 2026-05-13
 */
public class TotalAmountSpecification implements Specification<CouponReceiveContext> {

    @Override
    public SpecificationResult check(CouponReceiveContext context) {
        if (ObjectUtil.isNull(context) || ObjectUtil.isNull(context.userProfile()) || ObjectUtil.isNull(context.couponTemplate())) {
            return SpecificationResult.fail("历史消费金额校验数据不完整");
        }

        UserProfile userProfile = context.userProfile();
        CouponTemplate couponTemplate = context.couponTemplate();

        BigDecimal totalAmount = ObjectUtil.defaultIfNull(userProfile.totalAmount(), BigDecimal.ZERO);
        BigDecimal minTotalAmount = ObjectUtil.defaultIfNull(couponTemplate.minTotalAmount(), BigDecimal.ZERO);

        if (totalAmount.compareTo(minTotalAmount) < 0) {
            return SpecificationResult.fail(
                    "历史消费金额不足，当前金额={}，要求金额={}",
                    totalAmount,
                    minTotalAmount
            );
        }

        return SpecificationResult.pass();
    }

}
```

### 规格工厂

文件位置：`src/main/java/io/github/atengk/specification/specification/CouponReceiveSpecificationFactory.java`

该工厂类负责组装业务规格。Service 不直接感知具体规则列表，只调用工厂获取组合后的规格。

```java
package io.github.atengk.specification.specification;

import io.github.atengk.specification.domain.model.CouponReceiveContext;
import io.github.atengk.specification.domain.specification.Specification;
import org.springframework.stereotype.Component;

/**
 * 优惠券领取规格工厂
 *
 * @author Ateng
 * @since 2026-05-13
 */
@Component
public class CouponReceiveSpecificationFactory {

    public Specification<CouponReceiveContext> createReceiveSpecification() {
        return Specification.allOf(
                new CouponAvailableSpecification(),
                new UserStatusSpecification(),
                new MemberLevelSpecification(),
                new OrderCountSpecification(),
                new TotalAmountSpecification()
        );
    }

    public Specification<CouponReceiveContext> createBasicSpecification() {
        return Specification.allOf(
                new CouponAvailableSpecification(),
                new UserStatusSpecification()
        );
    }

}
```

## 接口对象

这一节定义接口入参和出参。规格对象不直接依赖这些 DTO、VO，避免业务规则和接口协议强绑定。

### 请求 DTO

文件位置：`src/main/java/io/github/atengk/specification/dto/CouponReceiveRequest.java`

该 DTO 用于接收优惠券领取请求。

```java
package io.github.atengk.specification.dto;

import jakarta.validation.constraints.NotBlank;

/**
 * 优惠券领取请求
 *
 * @author Ateng
 * @since 2026-05-13
 */
public record CouponReceiveRequest(

        @NotBlank(message = "用户ID不能为空")
        String userId,

        @NotBlank(message = "优惠券ID不能为空")
        String couponId

) {
}
```

### 响应 VO

文件位置：`src/main/java/io/github/atengk/specification/vo/CouponReceiveResponse.java`

该 VO 用于返回领取结果或资格校验结果。

```java
package io.github.atengk.specification.vo;

import java.time.LocalDateTime;

/**
 * 优惠券领取响应
 *
 * @author Ateng
 * @since 2026-05-13
 */
public record CouponReceiveResponse(

        Boolean received,

        String userId,

        String couponId,

        String message,

        LocalDateTime operateTime

) {
}
```

## 业务代码

这一节给出 Service 和 Controller。示例中使用内存数据模拟查询，真实项目中可以替换为 Repository、Mapper、Redis 或远程服务。

### Service 接口

文件位置：`src/main/java/io/github/atengk/specification/service/CouponReceiveService.java`

该接口定义优惠券领取和资格校验能力。

```java
package io.github.atengk.specification.service;

import io.github.atengk.specification.dto.CouponReceiveRequest;
import io.github.atengk.specification.vo.CouponReceiveResponse;

/**
 * 优惠券领取业务接口
 *
 * @author Ateng
 * @since 2026-05-13
 */
public interface CouponReceiveService {

    CouponReceiveResponse receiveCoupon(CouponReceiveRequest request);

    CouponReceiveResponse checkEligibility(String userId, String couponId);

}
```

### Service 实现

文件位置：`src/main/java/io/github/atengk/specification/service/impl/CouponReceiveServiceImpl.java`

该实现类负责组装业务上下文并调用规格。注意这里没有把所有规则都写成 Service 内部的 `if else`。

```java
package io.github.atengk.specification.service.impl;

import cn.hutool.core.util.ObjectUtil;
import io.github.atengk.specification.domain.model.CouponReceiveContext;
import io.github.atengk.specification.domain.model.CouponTemplate;
import io.github.atengk.specification.domain.model.UserProfile;
import io.github.atengk.specification.domain.specification.Specification;
import io.github.atengk.specification.domain.specification.SpecificationResult;
import io.github.atengk.specification.dto.CouponReceiveRequest;
import io.github.atengk.specification.service.CouponReceiveService;
import io.github.atengk.specification.specification.CouponReceiveSpecificationFactory;
import io.github.atengk.specification.vo.CouponReceiveResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

/**
 * 优惠券领取业务实现
 *
 * @author Ateng
 * @since 2026-05-13
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class CouponReceiveServiceImpl implements CouponReceiveService {

    private static final Map<String, UserProfile> USER_PROFILE_MAP = new HashMap<>();

    private static final Map<String, CouponTemplate> COUPON_TEMPLATE_MAP = new HashMap<>();

    private final CouponReceiveSpecificationFactory specificationFactory;

    static {
        USER_PROFILE_MAP.put("10001", new UserProfile(
                "10001",
                "张三",
                "NORMAL",
                3,
                8,
                new BigDecimal("1299.00")
        ));
        USER_PROFILE_MAP.put("10002", new UserProfile(
                "10002",
                "李四",
                "FROZEN",
                5,
                20,
                new BigDecimal("6000.00")
        ));
        USER_PROFILE_MAP.put("10003", new UserProfile(
                "10003",
                "王五",
                "NORMAL",
                1,
                1,
                new BigDecimal("99.00")
        ));

        COUPON_TEMPLATE_MAP.put("C001", new CouponTemplate(
                "C001",
                "满减优惠券",
                true,
                2,
                3,
                new BigDecimal("500.00")
        ));
        COUPON_TEMPLATE_MAP.put("C002", new CouponTemplate(
                "C002",
                "高等级会员专享券",
                true,
                5,
                10,
                new BigDecimal("3000.00")
        ));
        COUPON_TEMPLATE_MAP.put("C003", new CouponTemplate(
                "C003",
                "已下架优惠券",
                false,
                1,
                0,
                BigDecimal.ZERO
        ));
    }

    @Override
    public CouponReceiveResponse receiveCoupon(CouponReceiveRequest request) {
        CouponReceiveContext context = buildContext(request.userId(), request.couponId());

        Specification<CouponReceiveContext> receiveSpecification = specificationFactory.createReceiveSpecification();
        SpecificationResult result = receiveSpecification.check(context);

        if (!result.passed()) {
            log.warn("优惠券领取失败，userId={}，couponId={}，reason={}",
                    request.userId(), request.couponId(), result.message());
            return new CouponReceiveResponse(false, request.userId(), request.couponId(), result.message(), LocalDateTime.now());
        }

        log.info("优惠券领取成功，userId={}，couponId={}", request.userId(), request.couponId());
        return new CouponReceiveResponse(true, request.userId(), request.couponId(), "领取成功", LocalDateTime.now());
    }

    @Override
    public CouponReceiveResponse checkEligibility(String userId, String couponId) {
        CouponReceiveContext context = buildContext(userId, couponId);

        Specification<CouponReceiveContext> receiveSpecification = specificationFactory.createReceiveSpecification();
        SpecificationResult result = receiveSpecification.check(context);

        String message = result.passed() ? "具备领取资格" : result.message();
        log.info("优惠券资格校验完成，userId={}，couponId={}，passed={}，message={}",
                userId, couponId, result.passed(), message);

        return new CouponReceiveResponse(result.passed(), userId, couponId, message, LocalDateTime.now());
    }

    private CouponReceiveContext buildContext(String userId, String couponId) {
        UserProfile userProfile = USER_PROFILE_MAP.get(userId);
        CouponTemplate couponTemplate = COUPON_TEMPLATE_MAP.get(couponId);

        if (ObjectUtil.isNull(userProfile)) {
            log.warn("用户画像不存在，userId={}", userId);
        }
        if (ObjectUtil.isNull(couponTemplate)) {
            log.warn("优惠券模板不存在，couponId={}", couponId);
        }

        return new CouponReceiveContext(userProfile, couponTemplate);
    }

}
```

### Controller 接口

文件位置：`src/main/java/io/github/atengk/specification/controller/CouponController.java`

该 Controller 提供领取优惠券和校验领取资格两个接口。

```java
package io.github.atengk.specification.controller;

import io.github.atengk.specification.dto.CouponReceiveRequest;
import io.github.atengk.specification.service.CouponReceiveService;
import io.github.atengk.specification.vo.CouponReceiveResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

/**
 * 优惠券接口
 *
 * @author Ateng
 * @since 2026-05-13
 */
@RestController
@RequestMapping("/coupons")
@RequiredArgsConstructor
public class CouponController {

    private final CouponReceiveService couponReceiveService;

    @PostMapping("/receive")
    public CouponReceiveResponse receiveCoupon(@Valid @RequestBody CouponReceiveRequest request) {
        return couponReceiveService.receiveCoupon(request);
    }

    @GetMapping("/eligibility")
    public CouponReceiveResponse checkEligibility(@RequestParam String userId,
                                                  @RequestParam String couponId) {
        return couponReceiveService.checkEligibility(userId, couponId);
    }

}
```

## 使用方式

本示例提供两个接口：领取优惠券和校验领取资格。两个接口都复用同一组规格对象。

### 校验领取资格

接口信息：

| 项目     | 内容                           |
| -------- | ------------------------------ |
| 请求路径 | `/coupons/eligibility`         |
| 请求方法 | `GET`                          |
| 主要作用 | 判断用户是否具备优惠券领取资格 |

用户 `10001` 满足 `C001` 的领取条件。

```bash
curl -X GET 'http://localhost:8080/coupons/eligibility?userId=10001&couponId=C001'
```

响应示例：

```json
{
  "received": true,
  "userId": "10001",
  "couponId": "C001",
  "message": "具备领取资格",
  "operateTime": "2026-05-13T10:30:00"
}
```

用户 `10003` 不满足 `C001` 的领取条件。

```bash
curl -X GET 'http://localhost:8080/coupons/eligibility?userId=10003&couponId=C001'
```

响应示例：

```json
{
  "received": false,
  "userId": "10003",
  "couponId": "C001",
  "message": "会员等级不足，当前等级=1，要求等级=2",
  "operateTime": "2026-05-13T10:31:00"
}
```

### 领取优惠券

接口信息：

| 项目         | 内容                 |
| ------------ | -------------------- |
| 请求路径     | `/coupons/receive`   |
| 请求方法     | `POST`               |
| Content-Type | `application/json`   |
| 主要作用     | 校验资格并领取优惠券 |

请求示例：

```bash
curl -X POST 'http://localhost:8080/coupons/receive' \
  -H 'Content-Type: application/json' \
  -d '{
    "userId": "10001",
    "couponId": "C001"
  }'
```

响应示例：

```json
{
  "received": true,
  "userId": "10001",
  "couponId": "C001",
  "message": "领取成功",
  "operateTime": "2026-05-13T10:32:00"
}
```

领取已下架优惠券：

```bash
curl -X POST 'http://localhost:8080/coupons/receive' \
  -H 'Content-Type: application/json' \
  -d '{
    "userId": "10001",
    "couponId": "C003"
  }'
```

响应示例：

```json
{
  "received": false,
  "userId": "10001",
  "couponId": "C003",
  "message": "优惠券不可用，couponId=C003",
  "operateTime": "2026-05-13T10:33:00"
}
```

## 验证方式

可以通过接口响应和日志验证规格模式是否生效。

启动项目：

```bash
mvn spring-boot:run
```

执行通过场景：

```bash
curl -X GET 'http://localhost:8080/coupons/eligibility?userId=10001&couponId=C001'
```

执行用户状态异常场景：

```bash
curl -X GET 'http://localhost:8080/coupons/eligibility?userId=10002&couponId=C001'
```

执行会员等级不足场景：

```bash
curl -X GET 'http://localhost:8080/coupons/eligibility?userId=10003&couponId=C001'
```

执行优惠券不可用场景：

```bash
curl -X GET 'http://localhost:8080/coupons/eligibility?userId=10001&couponId=C003'
```

验证点：

```text
1. Service 中没有堆叠大量规则 if else。
2. 每个业务规则都有独立 Specification 类。
3. 组合规则由 CouponReceiveSpecificationFactory 统一组装。
4. 失败时能返回具体失败原因。
5. 新增规则时，只需要新增规格类并调整规格组合。
```

## 扩展示例

规格模式的优势在于组合。除了全部满足的 `AND` 组合，也可以构建更复杂的业务规则。

### 任意规则满足即可通过

例如某个优惠券允许“会员等级达到 5 或历史消费金额达到 3000”即可领取，可以这样组合：

```java
Specification<CouponReceiveContext> vipOrHighAmountSpec = Specification.anyOf(
        new MemberLevelSpecification(),
        new TotalAmountSpecification()
);
```

如果业务要求基础条件必须满足，同时满足会员等级或消费金额之一，可以这样组合：

```java
Specification<CouponReceiveContext> complexSpec = Specification.allOf(
        new CouponAvailableSpecification(),
        new UserStatusSpecification(),
        Specification.anyOf(
                new MemberLevelSpecification(),
                new TotalAmountSpecification()
        )
);
```

### 反向规则

例如存在一个 `BlacklistUserSpecification`，用于判断用户是否在黑名单中。业务上需要表达“用户不能在黑名单中”，可以这样写：

```java
Specification<CouponReceiveContext> notBlacklistSpec =
        new BlacklistUserSpecification().not("黑名单用户不能领取优惠券");
```

这里的 `not` 不是简单的代码炫技，它能让规则表达更接近业务语言：

```text
优惠券可用
AND 用户状态正常
AND 用户不在黑名单
AND 会员等级满足
```

## 适用场景

规格模式适合规则较多、规则经常变化、规则需要组合复用的场景。

常见适用场景：

```text
优惠券领取：
- 用户状态校验
- 会员等级校验
- 历史订单数校验
- 历史消费金额校验
- 活动时间校验

订单风控：
- 收货地址风险校验
- 下单频率校验
- 支付账号校验
- 黑名单校验
- 大额订单校验

会员权益：
- 会员等级校验
- 权益有效期校验
- 使用次数校验
- 适用场景校验

商品上下架：
- 库存校验
- 价格校验
- 类目状态校验
- 商家状态校验
- 审核状态校验
```

## 不适用场景

规格模式会增加类数量。如果业务规则非常简单，直接判断更清晰。

不建议使用的场景：

```text
1. 只有一两个简单条件。
2. 规则不会复用，也不会扩展。
3. 判断逻辑和主流程高度绑定，拆出来反而不清晰。
4. 团队不熟悉规格模式，短期维护成本高于收益。
5. 只是为了消灭 if else 而机械拆类。
```

例如下面这种简单判断没有必要引入规格模式：

```java
if (!Boolean.TRUE.equals(couponTemplate.available())) {
    return "优惠券不可用";
}
```

当规则开始变成下面这样时，再考虑规格模式更合理：

```text
优惠券可用
AND 用户状态正常
AND 用户等级满足
AND 历史订单数满足
AND 历史消费金额满足
AND 当前时间在活动周期内
AND 用户不在黑名单
AND 用户未超过领取次数
```

## 和责任链模式的区别

规格模式和责任链模式都可以处理多个规则，但关注点不同。当前设计模式文档中也强调，责任链模式按顺序处理请求，规格模式组合判断条件。

| 对比项   | 规格模式                     | 责任链模式                       |
| -------- | ---------------------------- | -------------------------------- |
| 核心关注 | 条件判断是否满足             | 请求按链路逐个处理               |
| 组合方式 | `and`、`or`、`not`           | next 节点顺序流转                |
| 返回结果 | 通常是通过或不通过           | 可以修改请求、终止流程、继续传递 |
| 适合场景 | 资格校验、规则组合、查询条件 | 过滤器、审批流、风控处理链       |
| 规则关系 | 更强调逻辑组合               | 更强调处理顺序                   |

简单理解：

```text
规格模式关注“这个对象是否满足某组规则”。
责任链模式关注“这个请求要经过哪些处理节点”。
```

## 和解释器模式的区别

规格模式和解释器模式都可能用于规则表达，但抽象层次不同。解释器模式偏向解析表达式或 DSL，规格模式偏向用对象表达规则。

| 对比项   | 规格模式           | 解释器模式                   |
| -------- | ------------------ | ---------------------------- |
| 表达方式 | Java 对象组合      | 表达式、脚本、DSL            |
| 典型形式 | `specA.and(specB)` | `level >= 3 && amount > 500` |
| 适合人群 | 开发人员维护       | 可面向运营、配置人员         |
| 扩展方式 | 新增规格类         | 新增语法、解析器、函数       |
| 复杂度   | 中等               | 较高                         |

如果规则主要由开发人员维护，使用规格模式通常更简单。如果规则需要由运营后台配置，并且表达式很多，可以考虑解释器模式、规则引擎或 DSL。

## 和策略模式的区别

策略模式解决的是“选择哪一种算法或处理方式”，规格模式解决的是“对象是否满足某个条件”。

| 对比项       | 规格模式                     | 策略模式                     |
| ------------ | ---------------------------- | ---------------------------- |
| 核心问题     | 判断条件是否满足             | 选择并执行某种业务算法       |
| 典型方法     | `check(context)`             | `execute(request)`           |
| 返回结果     | 通过、不通过、失败原因       | 业务处理结果                 |
| 常见场景     | 优惠资格、风控条件、查询条件 | 支付方式、计价方式、通知渠道 |
| 是否强调组合 | 强调组合                     | 不一定强调组合               |

简单理解：

```text
规格模式回答：能不能做？
策略模式回答：怎么做？
```

## 项目落地建议

在 Spring Boot 项目中使用规格模式时，应优先保证规则对象小而清晰，不要把一个规格写成新的“大 Service”。

建议：

```text
1. 一个规格类只表达一个明确业务条件。
2. 规格命名使用业务语义，例如 MemberLevelSpecification。
3. 规格输入建议使用业务上下文对象，而不是直接使用 Controller DTO。
4. 规格结果不要只返回 boolean，建议携带失败原因。
5. 组合规则放到 Factory 或 Assembler 中，不要散落在 Controller。
6. 规则需要复用时再抽成规格，不要为简单判断过度设计。
7. 涉及数据库查询时，优先在 Service 或 Repository 中准备上下文数据，再交给规格判断。
8. 规格类应尽量无状态，便于复用和单元测试。
```

推荐命名：

```text
UserStatusSpecification
MemberLevelSpecification
CouponAvailableSpecification
OrderAmountSpecification
BlacklistUserSpecification
ActivityTimeSpecification
```

不推荐命名：

```text
CheckSpecification
CommonSpecification
RuleSpecification
CouponSpecification
ValidateSpecification
```

这些名字过于宽泛，后续很容易变成规则垃圾桶。

## 常见问题

### 规格模式能不能访问数据库

可以，但不建议让每个规格对象随意访问数据库。更推荐在 Service 或 Repository 中提前准备好上下文数据，然后让规格对象做纯规则判断。

推荐方式：

```text
Service 查询用户画像、优惠券模板、订单统计
 -> 构建 CouponReceiveContext
 -> Specification 判断
```

不推荐方式：

```text
Specification 内部到处注入 Mapper、Redis、远程 Feign
 -> 每个规则都查一次数据
 -> 性能和依赖关系失控
```

如果某些规则必须访问外部数据，可以把数据访问封装到专门的领域服务或查询服务中，并明确控制调用次数和缓存策略。

### 规格对象是否应该交给 Spring 管理

如果规格对象无状态，可以直接 `new`，也可以由工厂统一创建。如果规格对象需要依赖配置、服务或外部组件，可以交给 Spring 管理。

一般建议：

```text
无状态简单规格：可以直接 new，由 Factory 统一组装。
有依赖规格：使用 @Component 交给 Spring 管理。
复杂规则集合：使用 Factory、Builder 或配置驱动方式组装。
```

### 规格模式是否只能用于校验

不是。规格模式除了用于业务校验，也可以用于查询条件组合。

例如在 JPA 中，`Specification<T>` 常用于动态查询条件；在 MyBatis-Plus 中，也可以借鉴这种思想，把查询条件封装为独立对象，再组合成 `LambdaQueryWrapper`。

不过本文示例重点是业务规则规格，不是数据库查询规格。二者思想一致，但落地代码不同。

## 总结

规格模式适合把复杂业务规则拆成可组合、可复用、可测试的规则对象。它在 Spring Boot 项目中常用于优惠券资格、订单风控、会员权益、商品规则、审批准入等场景。

推荐落地方式：

```text
Controller
 -> Service
   -> 构建业务上下文
   -> SpecificationFactory 创建组合规格
   -> Specification 校验
   -> 返回结果或继续执行业务
```

当业务规则只有一两个简单判断时，不需要引入规格模式。当规则数量增加、规则需要复用、规则组合关系复杂时，规格模式可以明显降低 Service 中的条件分支复杂度，让业务规则更加清晰、稳定和易扩展。
