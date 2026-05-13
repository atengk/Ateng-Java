# 享元模式

享元模式属于结构型模式，核心作用是共享大量重复对象中的公共部分，减少内存开销和对象创建成本。在当前设计模式文档体系中，享元模式位于结构型模式分类下，适合缓存共享对象、字典项、模板对象、配置对象、规则对象等 Spring Boot 项目场景。

本文以 **JDK21 + Spring Boot 3** 后端项目为背景，通过“优惠券模板共享”的示例，说明享元模式在真实项目中的落地方式。

## 基础配置

本示例模拟一个优惠券模块。系统中会给大量用户发放优惠券，但很多用户券实际上都来自同一个优惠券模板。

如果每一张用户券都重复保存模板名称、优惠类型、满减门槛、优惠金额、使用说明等字段，内存对象会出现大量重复数据：

```text
用户券 1：用户 A + 模板 T1 + 满 100 减 10 + 新人优惠券
用户券 2：用户 B + 模板 T1 + 满 100 减 10 + 新人优惠券
用户券 3：用户 C + 模板 T1 + 满 100 减 10 + 新人优惠券
```

这些对象中，“用户 ID、领取时间、使用状态”是每张券不同的外部状态，而“模板名称、优惠类型、门槛金额、优惠金额”是大量券共享的内部状态。

享元模式的处理方式是：把可共享的优惠券模板抽成享元对象，由工厂统一缓存和复用；用户券对象只保存自己的外部状态，并引用共享模板。

本示例需要以下依赖。

```xml
<dependencies>
    <!-- Spring Web：提供 REST API 能力 -->
    <dependency>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-starter-web</artifactId>
    </dependency>

    <!-- 参数校验：用于校验优惠券发放请求参数 -->
    <dependency>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-starter-validation</artifactId>
    </dependency>

    <!-- Hutool：用于金额、集合、字符串、ID、日期等常用工具处理 -->
    <dependency>
        <groupId>cn.hutool</groupId>
        <artifactId>hutool-all</artifactId>
        <version>${hutool.version}</version>
    </dependency>

    <!-- Lombok：减少 DTO、VO、构造器等样板代码 -->
    <dependency>
        <groupId>org.projectlombok</groupId>
        <artifactId>lombok</artifactId>
        <optional>true</optional>
    </dependency>

    <!-- 测试依赖：用于单元测试和接口测试 -->
    <dependency>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-starter-test</artifactId>
        <scope>test</scope>
    </dependency>
</dependencies>
```

示例项目配置如下。

```yaml
server:
  port: 8080 # 示例服务端口

coupon:
  cache:
    preload: true # 是否启动时预加载优惠券模板
```

本示例的核心文件结构如下。

```text
src/main/java/io/github/atengk/designpattern/flyweight
├── FlyweightApplication.java
├── config
│   └── CouponProperties.java
├── controller
│   └── UserCouponController.java
├── dto
│   ├── UserCouponIssueRequest.java
│   └── UserCouponUseRequest.java
├── enums
│   ├── CouponDiscountType.java
│   └── UserCouponStatus.java
├── flyweight
│   ├── CouponTemplateFlyweight.java
│   ├── CouponTemplateFlyweightFactory.java
│   └── UserCouponContext.java
├── repository
│   ├── CouponTemplateMemoryRepository.java
│   └── UserCouponMemoryRepository.java
├── service
│   ├── UserCouponService.java
│   └── UserCouponServiceImpl.java
├── vo
│   ├── ApiResult.java
│   ├── CouponTemplateCacheStatsVO.java
│   └── UserCouponVO.java
└── web
    └── GlobalExceptionHandler.java
```

## 模式设计

享元模式的重点是区分内部状态和外部状态。

内部状态是可以共享、相对稳定、与具体使用场景无关的数据；外部状态是不能共享、由调用方或上下文传入的数据。

在本示例中，优惠券模板是内部状态，用户券上下文是外部状态。

| 类型     | 示例字段          | 是否共享 | 说明           |
| -------- | ----------------- | -------- | -------------- |
| 内部状态 | `templateId`      | 是       | 优惠券模板 ID  |
| 内部状态 | `templateName`    | 是       | 优惠券模板名称 |
| 内部状态 | `discountType`    | 是       | 优惠类型       |
| 内部状态 | `thresholdAmount` | 是       | 使用门槛       |
| 内部状态 | `discountAmount`  | 是       | 优惠金额       |
| 外部状态 | `userCouponId`    | 否       | 用户券 ID      |
| 外部状态 | `userId`          | 否       | 用户 ID        |
| 外部状态 | `receiveTime`     | 否       | 领取时间       |
| 外部状态 | `status`          | 否       | 用户券状态     |

本示例中的角色分工如下。

| 角色       | 示例类                           | 说明                                 |
| ---------- | -------------------------------- | ------------------------------------ |
| 享元对象   | `CouponTemplateFlyweight`        | 可共享的优惠券模板对象               |
| 享元工厂   | `CouponTemplateFlyweightFactory` | 负责创建、缓存、获取享元对象         |
| 外部状态   | `UserCouponContext`              | 每张用户券独有的上下文数据           |
| 模板仓储   | `CouponTemplateMemoryRepository` | 模拟模板数据来源                     |
| 用户券仓储 | `UserCouponMemoryRepository`     | 模拟用户券数据存储                   |
| 业务服务   | `UserCouponServiceImpl`          | 组合享元对象和外部状态，完成业务操作 |

核心流程如下。

```text
Controller
    ↓
UserCouponService
    ↓
UserCouponMemoryRepository 查询用户券外部状态
    ↓
CouponTemplateFlyweightFactory 获取共享模板对象
    ↓
CouponTemplateFlyweight 计算优惠与转换展示
    ↓
返回用户券详情
```

享元模式的关键不是简单缓存，而是把大量对象中的重复部分抽离出来共享，把变化部分作为外部状态传入。

## 核心代码

下面给出享元模式在 Spring Boot 项目中的关键实现。示例使用内存仓储模拟数据库，真实项目中可以替换为 MyBatis-Plus、Redis 或本地缓存组件。

项目启动类负责启动 Spring Boot 应用，并开启配置属性扫描。

文件位置：`src/main/java/io/github/atengk/designpattern/flyweight/FlyweightApplication.java`

```java
package io.github.atengk.designpattern.flyweight;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;

/**
 * 享元模式示例应用启动类
 *
 * @author Ateng
 * @since 2026-05-13
 */
@ConfigurationPropertiesScan
@SpringBootApplication
public class FlyweightApplication {

    /**
     * 应用启动入口
     *
     * @param args 启动参数
     */
    public static void main(String[] args) {
        SpringApplication.run(FlyweightApplication.class, args);
    }
}
```

优惠券配置类用于控制模板缓存行为。

文件位置：`src/main/java/io/github/atengk/designpattern/flyweight/config/CouponProperties.java`

```java
package io.github.atengk.designpattern.flyweight.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * 优惠券配置
 *
 * @author Ateng
 * @since 2026-05-13
 */
@Data
@ConfigurationProperties(prefix = "coupon")
public class CouponProperties {

    /**
     * 缓存配置
     */
    private Cache cache = new Cache();

    /**
     * 缓存配置项
     *
     * @author Ateng
     * @since 2026-05-13
     */
    @Data
    public static class Cache {

        /**
         * 是否启动时预加载模板
         */
        private Boolean preload = Boolean.TRUE;
    }
}
```

优惠类型枚举用于表达不同优惠券模板的计算方式。

文件位置：`src/main/java/io/github/atengk/designpattern/flyweight/enums/CouponDiscountType.java`

```java
package io.github.atengk.designpattern.flyweight.enums;

import cn.hutool.core.util.StrUtil;

import java.util.Arrays;

/**
 * 优惠券优惠类型
 *
 * @author Ateng
 * @since 2026-05-13
 */
public enum CouponDiscountType {

    /**
     * 满减券
     */
    FULL_REDUCTION,

    /**
     * 立减券
     */
    DIRECT_REDUCTION;

    /**
     * 根据类型编码解析优惠类型
     *
     * @param type 类型编码
     * @return 优惠类型
     */
    public static CouponDiscountType parse(String type) {
        if (StrUtil.isBlank(type)) {
            throw new IllegalArgumentException("优惠类型不能为空");
        }

        return Arrays.stream(values())
                .filter(item -> StrUtil.equalsIgnoreCase(item.name(), type))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException(StrUtil.format("不支持的优惠类型：{}", type)));
    }
}
```

用户券状态枚举用于标识用户券是否可用、已使用或已过期。

文件位置：`src/main/java/io/github/atengk/designpattern/flyweight/enums/UserCouponStatus.java`

```java
package io.github.atengk.designpattern.flyweight.enums;

import cn.hutool.core.util.StrUtil;

import java.util.Arrays;

/**
 * 用户优惠券状态
 *
 * @author Ateng
 * @since 2026-05-13
 */
public enum UserCouponStatus {

    /**
     * 未使用
     */
    UNUSED,

    /**
     * 已使用
     */
    USED,

    /**
     * 已过期
     */
    EXPIRED;

    /**
     * 根据状态编码解析用户券状态
     *
     * @param status 状态编码
     * @return 用户券状态
     */
    public static UserCouponStatus parse(String status) {
        if (StrUtil.isBlank(status)) {
            throw new IllegalArgumentException("用户券状态不能为空");
        }

        return Arrays.stream(values())
                .filter(item -> StrUtil.equalsIgnoreCase(item.name(), status))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException(StrUtil.format("不支持的用户券状态：{}", status)));
    }
}
```

发放用户券请求 DTO 用于模拟批量发券。

文件位置：`src/main/java/io/github/atengk/designpattern/flyweight/dto/UserCouponIssueRequest.java`

```java
package io.github.atengk.designpattern.flyweight.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

/**
 * 用户优惠券发放请求
 *
 * @author Ateng
 * @since 2026-05-13
 */
@Data
public class UserCouponIssueRequest {

    /**
     * 用户 ID
     */
    @NotBlank(message = "用户 ID 不能为空")
    private String userId;

    /**
     * 优惠券模板 ID
     */
    @NotBlank(message = "优惠券模板 ID 不能为空")
    private String templateId;

    /**
     * 发放数量
     */
    @Min(value = 1, message = "发放数量不能小于 1")
    @Max(value = 100, message = "单次最多发放 100 张")
    private Integer quantity = 1;
}
```

使用用户券请求 DTO 用于模拟核销优惠券。

文件位置：`src/main/java/io/github/atengk/designpattern/flyweight/dto/UserCouponUseRequest.java`

```java
package io.github.atengk.designpattern.flyweight.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.math.BigDecimal;

/**
 * 用户优惠券使用请求
 *
 * @author Ateng
 * @since 2026-05-13
 */
@Data
public class UserCouponUseRequest {

    /**
     * 用户券 ID
     */
    @NotBlank(message = "用户券 ID 不能为空")
    private String userCouponId;

    /**
     * 订单金额
     */
    @NotNull(message = "订单金额不能为空")
    @DecimalMin(value = "0.01", message = "订单金额必须大于 0")
    private BigDecimal orderAmount;
}
```

用户券返回 VO 聚合用户券外部状态和模板享元对象的共享信息。

文件位置：`src/main/java/io/github/atengk/designpattern/flyweight/vo/UserCouponVO.java`

```java
package io.github.atengk.designpattern.flyweight.vo;

import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 用户优惠券返回对象
 *
 * @author Ateng
 * @since 2026-05-13
 */
@Data
@Builder
public class UserCouponVO {

    /**
     * 用户券 ID
     */
    private String userCouponId;

    /**
     * 用户 ID
     */
    private String userId;

    /**
     * 优惠券模板 ID
     */
    private String templateId;

    /**
     * 优惠券模板名称
     */
    private String templateName;

    /**
     * 优惠类型
     */
    private String discountType;

    /**
     * 使用门槛
     */
    private BigDecimal thresholdAmount;

    /**
     * 优惠金额
     */
    private BigDecimal discountAmount;

    /**
     * 用户券状态
     */
    private String status;

    /**
     * 领取时间
     */
    private LocalDateTime receiveTime;

    /**
     * 展示文案
     */
    private String displayText;
}
```

缓存统计 VO 用于验证享元对象是否被复用。

文件位置：`src/main/java/io/github/atengk/designpattern/flyweight/vo/CouponTemplateCacheStatsVO.java`

```java
package io.github.atengk.designpattern.flyweight.vo;

import lombok.Builder;
import lombok.Data;

/**
 * 优惠券模板缓存统计返回对象
 *
 * @author Ateng
 * @since 2026-05-13
 */
@Data
@Builder
public class CouponTemplateCacheStatsVO {

    /**
     * 缓存模板数量
     */
    private Integer cacheSize;

    /**
     * 模板仓储数量
     */
    private Integer repositorySize;

    /**
     * 是否已预加载
     */
    private Boolean preloaded;
}
```

统一 API 返回对象用于包装接口响应。

文件位置：`src/main/java/io/github/atengk/designpattern/flyweight/vo/ApiResult.java`

```java
package io.github.atengk.designpattern.flyweight.vo;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * API 统一返回对象
 *
 * @author Ateng
 * @since 2026-05-13
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ApiResult<T> {

    /**
     * 业务状态码
     */
    private Integer code;

    /**
     * 返回消息
     */
    private String message;

    /**
     * 返回数据
     */
    private T data;

    /**
     * 成功返回
     *
     * @param data 返回数据
     * @return API 返回对象
     */
    public static <T> ApiResult<T> success(T data) {
        return ApiResult.<T>builder()
                .code(200)
                .message("操作成功")
                .data(data)
                .build();
    }

    /**
     * 失败返回
     *
     * @param message 失败消息
     * @return API 返回对象
     */
    public static ApiResult<Void> fail(String message) {
        return ApiResult.<Void>builder()
                .code(500)
                .message(message)
                .build();
    }
}
```

用户券上下文表示外部状态。它不保存模板名称、优惠金额等重复字段，只保存每张用户券独有的信息。

文件位置：`src/main/java/io/github/atengk/designpattern/flyweight/flyweight/UserCouponContext.java`

```java
package io.github.atengk.designpattern.flyweight.flyweight;

import io.github.atengk.designpattern.flyweight.enums.UserCouponStatus;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 用户优惠券上下文
 *
 * @author Ateng
 * @since 2026-05-13
 */
@Data
@Builder
public class UserCouponContext {

    /**
     * 用户券 ID
     */
    private String userCouponId;

    /**
     * 用户 ID
     */
    private String userId;

    /**
     * 优惠券模板 ID
     */
    private String templateId;

    /**
     * 用户券状态
     */
    private UserCouponStatus status;

    /**
     * 领取时间
     */
    private LocalDateTime receiveTime;
}
```

优惠券模板享元对象保存可共享的模板信息，并提供优惠计算和展示文案生成能力。

文件位置：`src/main/java/io/github/atengk/designpattern/flyweight/flyweight/CouponTemplateFlyweight.java`

```java
package io.github.atengk.designpattern.flyweight.flyweight;

import cn.hutool.core.util.NumberUtil;
import cn.hutool.core.util.StrUtil;
import io.github.atengk.designpattern.flyweight.enums.CouponDiscountType;
import io.github.atengk.designpattern.flyweight.vo.UserCouponVO;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

import java.math.BigDecimal;

/**
 * 优惠券模板享元对象
 *
 * @author Ateng
 * @since 2026-05-13
 */
@Getter
@RequiredArgsConstructor
public class CouponTemplateFlyweight {

    /**
     * 优惠券模板 ID
     */
    private final String templateId;

    /**
     * 优惠券模板名称
     */
    private final String templateName;

    /**
     * 优惠类型
     */
    private final CouponDiscountType discountType;

    /**
     * 使用门槛
     */
    private final BigDecimal thresholdAmount;

    /**
     * 优惠金额
     */
    private final BigDecimal discountAmount;

    /**
     * 计算优惠金额
     *
     * @param orderAmount 订单金额
     * @return 实际优惠金额
     */
    public BigDecimal calculateDiscount(BigDecimal orderAmount) {
        if (orderAmount == null || NumberUtil.isLess(orderAmount, BigDecimal.ZERO)) {
            throw new IllegalArgumentException("订单金额不合法");
        }

        if (discountType == CouponDiscountType.FULL_REDUCTION
                && NumberUtil.isLess(orderAmount, thresholdAmount)) {
            return BigDecimal.ZERO;
        }

        return NumberUtil.min(discountAmount, orderAmount);
    }

    /**
     * 转换用户券展示对象
     *
     * @param context 用户券上下文
     * @return 用户券展示对象
     */
    public UserCouponVO toUserCouponVO(UserCouponContext context) {
        return UserCouponVO.builder()
                .userCouponId(context.getUserCouponId())
                .userId(context.getUserId())
                .templateId(templateId)
                .templateName(templateName)
                .discountType(discountType.name())
                .thresholdAmount(thresholdAmount)
                .discountAmount(discountAmount)
                .status(context.getStatus().name())
                .receiveTime(context.getReceiveTime())
                .displayText(buildDisplayText())
                .build();
    }

    /**
     * 构建优惠券展示文案
     *
     * @return 展示文案
     */
    public String buildDisplayText() {
        if (discountType == CouponDiscountType.FULL_REDUCTION) {
            return StrUtil.format("满 {} 减 {}", thresholdAmount.toPlainString(), discountAmount.toPlainString());
        }

        return StrUtil.format("立减 {}", discountAmount.toPlainString());
    }
}
```

优惠券模板内存仓储模拟数据库中的模板表。

文件位置：`src/main/java/io/github/atengk/designpattern/flyweight/repository/CouponTemplateMemoryRepository.java`

```java
package io.github.atengk.designpattern.flyweight.repository;

import io.github.atengk.designpattern.flyweight.enums.CouponDiscountType;
import lombok.Builder;
import lombok.Data;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 优惠券模板内存仓储
 *
 * @author Ateng
 * @since 2026-05-13
 */
@Repository
public class CouponTemplateMemoryRepository {

    private final Map<String, CouponTemplateRecord> templateMap = new ConcurrentHashMap<>();

    /**
     * 初始化示例优惠券模板
     */
    public CouponTemplateMemoryRepository() {
        templateMap.put("template-10", CouponTemplateRecord.builder()
                .templateId("template-10")
                .templateName("新人满减券")
                .discountType(CouponDiscountType.FULL_REDUCTION)
                .thresholdAmount(new BigDecimal("100.00"))
                .discountAmount(new BigDecimal("10.00"))
                .build());

        templateMap.put("template-30", CouponTemplateRecord.builder()
                .templateId("template-30")
                .templateName("大额订单满减券")
                .discountType(CouponDiscountType.FULL_REDUCTION)
                .thresholdAmount(new BigDecimal("300.00"))
                .discountAmount(new BigDecimal("30.00"))
                .build());

        templateMap.put("template-direct-5", CouponTemplateRecord.builder()
                .templateId("template-direct-5")
                .templateName("无门槛立减券")
                .discountType(CouponDiscountType.DIRECT_REDUCTION)
                .thresholdAmount(BigDecimal.ZERO)
                .discountAmount(new BigDecimal("5.00"))
                .build());
    }

    /**
     * 根据模板 ID 查询模板
     *
     * @param templateId 模板 ID
     * @return 模板记录
     */
    public CouponTemplateRecord getByTemplateId(String templateId) {
        CouponTemplateRecord record = templateMap.get(templateId);
        if (record == null) {
            throw new IllegalArgumentException("优惠券模板不存在：" + templateId);
        }
        return record;
    }

    /**
     * 查询全部模板
     *
     * @return 模板记录列表
     */
    public List<CouponTemplateRecord> listAll() {
        return List.copyOf(templateMap.values());
    }

    /**
     * 查询模板数量
     *
     * @return 模板数量
     */
    public int count() {
        return templateMap.size();
    }

    /**
     * 优惠券模板记录
     *
     * @author Ateng
     * @since 2026-05-13
     */
    @Data
    @Builder
    public static class CouponTemplateRecord {

        /**
         * 优惠券模板 ID
         */
        private String templateId;

        /**
         * 优惠券模板名称
         */
        private String templateName;

        /**
         * 优惠类型
         */
        private CouponDiscountType discountType;

        /**
         * 使用门槛
         */
        private BigDecimal thresholdAmount;

        /**
         * 优惠金额
         */
        private BigDecimal discountAmount;
    }
}
```

享元工厂负责统一创建、缓存和获取优惠券模板享元对象。相同模板 ID 永远复用同一个享元对象。

文件位置：`src/main/java/io/github/atengk/designpattern/flyweight/flyweight/CouponTemplateFlyweightFactory.java`

```java
package io.github.atengk.designpattern.flyweight.flyweight;

import cn.hutool.core.util.BooleanUtil;
import io.github.atengk.designpattern.flyweight.config.CouponProperties;
import io.github.atengk.designpattern.flyweight.repository.CouponTemplateMemoryRepository;
import io.github.atengk.designpattern.flyweight.repository.CouponTemplateMemoryRepository.CouponTemplateRecord;
import io.github.atengk.designpattern.flyweight.vo.CouponTemplateCacheStatsVO;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 优惠券模板享元工厂
 *
 * @author Ateng
 * @since 2026-05-13
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class CouponTemplateFlyweightFactory {

    private final CouponTemplateMemoryRepository couponTemplateMemoryRepository;
    private final CouponProperties couponProperties;
    private final Map<String, CouponTemplateFlyweight> cache = new ConcurrentHashMap<>();
    private volatile boolean preloaded = false;

    /**
     * 初始化模板缓存
     */
    @PostConstruct
    public void init() {
        if (BooleanUtil.isTrue(couponProperties.getCache().getPreload())) {
            couponTemplateMemoryRepository.listAll()
                    .forEach(record -> cache.put(record.getTemplateId(), createFlyweight(record)));
            preloaded = true;
            log.info("优惠券模板享元预加载完成，cacheSize={}", cache.size());
        }
    }

    /**
     * 根据模板 ID 获取享元对象
     *
     * @param templateId 模板 ID
     * @return 优惠券模板享元对象
     */
    public CouponTemplateFlyweight getFlyweight(String templateId) {
        CouponTemplateFlyweight flyweight = cache.computeIfAbsent(templateId, key -> {
            CouponTemplateRecord record = couponTemplateMemoryRepository.getByTemplateId(key);
            log.info("创建优惠券模板享元对象，templateId={}，templateName={}", record.getTemplateId(), record.getTemplateName());
            return createFlyweight(record);
        });

        log.info("获取优惠券模板享元对象，templateId={}，identityHash={}",
                templateId, System.identityHashCode(flyweight));

        return flyweight;
    }

    /**
     * 查询缓存统计信息
     *
     * @return 缓存统计
     */
    public CouponTemplateCacheStatsVO stats() {
        return CouponTemplateCacheStatsVO.builder()
                .cacheSize(cache.size())
                .repositorySize(couponTemplateMemoryRepository.count())
                .preloaded(preloaded)
                .build();
    }

    /**
     * 根据模板记录创建享元对象
     *
     * @param record 模板记录
     * @return 优惠券模板享元对象
     */
    private CouponTemplateFlyweight createFlyweight(CouponTemplateRecord record) {
        return new CouponTemplateFlyweight(
                record.getTemplateId(),
                record.getTemplateName(),
                record.getDiscountType(),
                record.getThresholdAmount(),
                record.getDiscountAmount()
        );
    }
}
```

用户券内存仓储只保存外部状态，不重复保存模板内部状态。

文件位置：`src/main/java/io/github/atengk/designpattern/flyweight/repository/UserCouponMemoryRepository.java`

```java
package io.github.atengk.designpattern.flyweight.repository;

import cn.hutool.core.util.IdUtil;
import cn.hutool.core.util.StrUtil;
import io.github.atengk.designpattern.flyweight.enums.UserCouponStatus;
import io.github.atengk.designpattern.flyweight.flyweight.UserCouponContext;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * 用户优惠券内存仓储
 *
 * @author Ateng
 * @since 2026-05-13
 */
@Slf4j
@Repository
public class UserCouponMemoryRepository {

    private final List<UserCouponContext> userCoupons = new CopyOnWriteArrayList<>();

    /**
     * 发放用户优惠券
     *
     * @param userId     用户 ID
     * @param templateId 模板 ID
     * @return 用户券上下文
     */
    public UserCouponContext issue(String userId, String templateId) {
        UserCouponContext context = UserCouponContext.builder()
                .userCouponId("UC_" + IdUtil.fastSimpleUUID())
                .userId(userId)
                .templateId(templateId)
                .status(UserCouponStatus.UNUSED)
                .receiveTime(LocalDateTime.now())
                .build();

        userCoupons.add(context);
        log.info("用户优惠券发放成功，userCouponId={}，userId={}，templateId={}",
                context.getUserCouponId(), userId, templateId);

        return context;
    }

    /**
     * 查询用户优惠券列表
     *
     * @param userId 用户 ID
     * @return 用户券上下文列表
     */
    public List<UserCouponContext> listByUserId(String userId) {
        return userCoupons.stream()
                .filter(context -> StrUtil.equals(context.getUserId(), userId))
                .toList();
    }

    /**
     * 根据用户券 ID 查询用户券
     *
     * @param userCouponId 用户券 ID
     * @return 用户券上下文
     */
    public UserCouponContext getByUserCouponId(String userCouponId) {
        return userCoupons.stream()
                .filter(context -> StrUtil.equals(context.getUserCouponId(), userCouponId))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("用户优惠券不存在：" + userCouponId));
    }

    /**
     * 标记用户券已使用
     *
     * @param userCouponId 用户券 ID
     */
    public void markUsed(String userCouponId) {
        UserCouponContext context = getByUserCouponId(userCouponId);
        context.setStatus(UserCouponStatus.USED);
        log.info("用户优惠券已使用，userCouponId={}", userCouponId);
    }

    /**
     * 查询用户券数量
     *
     * @return 用户券数量
     */
    public int count() {
        return userCoupons.size();
    }
}
```

用户券服务接口定义发券、查询、使用和缓存统计能力。

文件位置：`src/main/java/io/github/atengk/designpattern/flyweight/service/UserCouponService.java`

```java
package io.github.atengk.designpattern.flyweight.service;

import io.github.atengk.designpattern.flyweight.dto.UserCouponIssueRequest;
import io.github.atengk.designpattern.flyweight.dto.UserCouponUseRequest;
import io.github.atengk.designpattern.flyweight.vo.CouponTemplateCacheStatsVO;
import io.github.atengk.designpattern.flyweight.vo.UserCouponVO;

import java.math.BigDecimal;
import java.util.List;

/**
 * 用户优惠券服务接口
 *
 * @author Ateng
 * @since 2026-05-13
 */
public interface UserCouponService {

    /**
     * 发放用户优惠券
     *
     * @param request 发放请求
     * @return 用户券列表
     */
    List<UserCouponVO> issue(UserCouponIssueRequest request);

    /**
     * 查询用户优惠券列表
     *
     * @param userId 用户 ID
     * @return 用户券列表
     */
    List<UserCouponVO> listByUserId(String userId);

    /**
     * 使用用户优惠券
     *
     * @param request 使用请求
     * @return 实际优惠金额
     */
    BigDecimal use(UserCouponUseRequest request);

    /**
     * 查询模板缓存统计
     *
     * @return 缓存统计
     */
    CouponTemplateCacheStatsVO stats();
}
```

服务实现类把用户券外部状态和模板享元对象组合起来完成业务操作。

文件位置：`src/main/java/io/github/atengk/designpattern/flyweight/service/UserCouponServiceImpl.java`

```java
package io.github.atengk.designpattern.flyweight.service;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.NumberUtil;
import io.github.atengk.designpattern.flyweight.dto.UserCouponIssueRequest;
import io.github.atengk.designpattern.flyweight.dto.UserCouponUseRequest;
import io.github.atengk.designpattern.flyweight.enums.UserCouponStatus;
import io.github.atengk.designpattern.flyweight.flyweight.CouponTemplateFlyweight;
import io.github.atengk.designpattern.flyweight.flyweight.CouponTemplateFlyweightFactory;
import io.github.atengk.designpattern.flyweight.flyweight.UserCouponContext;
import io.github.atengk.designpattern.flyweight.repository.CouponTemplateMemoryRepository;
import io.github.atengk.designpattern.flyweight.repository.UserCouponMemoryRepository;
import io.github.atengk.designpattern.flyweight.vo.CouponTemplateCacheStatsVO;
import io.github.atengk.designpattern.flyweight.vo.UserCouponVO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

/**
 * 用户优惠券服务实现类
 *
 * @author Ateng
 * @since 2026-05-13
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class UserCouponServiceImpl implements UserCouponService {

    private final CouponTemplateMemoryRepository couponTemplateMemoryRepository;
    private final UserCouponMemoryRepository userCouponMemoryRepository;
    private final CouponTemplateFlyweightFactory couponTemplateFlyweightFactory;

    /**
     * 发放用户优惠券
     *
     * @param request 发放请求
     * @return 用户券列表
     */
    @Override
    public List<UserCouponVO> issue(UserCouponIssueRequest request) {
        couponTemplateMemoryRepository.getByTemplateId(request.getTemplateId());

        List<UserCouponVO> resultList = new ArrayList<>();
        for (int i = 0; i < request.getQuantity(); i++) {
            UserCouponContext context = userCouponMemoryRepository.issue(request.getUserId(), request.getTemplateId());
            CouponTemplateFlyweight flyweight = couponTemplateFlyweightFactory.getFlyweight(context.getTemplateId());
            resultList.add(flyweight.toUserCouponVO(context));
        }

        log.info("用户优惠券批量发放完成，userId={}，templateId={}，quantity={}",
                request.getUserId(), request.getTemplateId(), resultList.size());

        return resultList;
    }

    /**
     * 查询用户优惠券列表
     *
     * @param userId 用户 ID
     * @return 用户券列表
     */
    @Override
    public List<UserCouponVO> listByUserId(String userId) {
        List<UserCouponContext> contexts = userCouponMemoryRepository.listByUserId(userId);
        if (CollUtil.isEmpty(contexts)) {
            return List.of();
        }

        List<UserCouponVO> resultList = contexts.stream()
                .map(context -> {
                    CouponTemplateFlyweight flyweight = couponTemplateFlyweightFactory.getFlyweight(context.getTemplateId());
                    return flyweight.toUserCouponVO(context);
                })
                .toList();

        log.info("查询用户优惠券列表成功，userId={}，size={}", userId, resultList.size());
        return resultList;
    }

    /**
     * 使用用户优惠券
     *
     * @param request 使用请求
     * @return 实际优惠金额
     */
    @Override
    public BigDecimal use(UserCouponUseRequest request) {
        UserCouponContext context = userCouponMemoryRepository.getByUserCouponId(request.getUserCouponId());
        if (context.getStatus() != UserCouponStatus.UNUSED) {
            throw new IllegalStateException("当前优惠券不可使用，status=" + context.getStatus());
        }

        CouponTemplateFlyweight flyweight = couponTemplateFlyweightFactory.getFlyweight(context.getTemplateId());
        BigDecimal discountAmount = flyweight.calculateDiscount(request.getOrderAmount());

        if (NumberUtil.equals(discountAmount, BigDecimal.ZERO)) {
            throw new IllegalStateException("订单金额未满足优惠券使用门槛");
        }

        userCouponMemoryRepository.markUsed(request.getUserCouponId());
        log.info("用户优惠券使用成功，userCouponId={}，templateId={}，orderAmount={}，discountAmount={}",
                request.getUserCouponId(), context.getTemplateId(), request.getOrderAmount(), discountAmount);

        return discountAmount;
    }

    /**
     * 查询模板缓存统计
     *
     * @return 缓存统计
     */
    @Override
    public CouponTemplateCacheStatsVO stats() {
        CouponTemplateCacheStatsVO stats = couponTemplateFlyweightFactory.stats();
        log.info("查询优惠券模板享元缓存统计，cacheSize={}，repositorySize={}",
                stats.getCacheSize(), stats.getRepositorySize());
        return stats;
    }
}
```

Controller 对外提供发券、查询用户券、使用优惠券和查看缓存统计接口。

文件位置：`src/main/java/io/github/atengk/designpattern/flyweight/controller/UserCouponController.java`

```java
package io.github.atengk.designpattern.flyweight.controller;

import cn.hutool.core.util.StrUtil;
import io.github.atengk.designpattern.flyweight.dto.UserCouponIssueRequest;
import io.github.atengk.designpattern.flyweight.dto.UserCouponUseRequest;
import io.github.atengk.designpattern.flyweight.service.UserCouponService;
import io.github.atengk.designpattern.flyweight.vo.ApiResult;
import io.github.atengk.designpattern.flyweight.vo.CouponTemplateCacheStatsVO;
import io.github.atengk.designpattern.flyweight.vo.UserCouponVO;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.List;

/**
 * 用户优惠券接口控制器
 *
 * @author Ateng
 * @since 2026-05-13
 */
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/user-coupons")
public class UserCouponController {

    private final UserCouponService userCouponService;

    /**
     * 发放用户优惠券
     *
     * @param request 发放请求
     * @return 用户券列表
     */
    @PostMapping("/issue")
    public ApiResult<List<UserCouponVO>> issue(@Valid @RequestBody UserCouponIssueRequest request) {
        return ApiResult.success(userCouponService.issue(request));
    }

    /**
     * 查询用户优惠券列表
     *
     * @param userId 用户 ID
     * @return 用户券列表
     */
    @GetMapping
    public ApiResult<List<UserCouponVO>> listByUserId(@RequestParam String userId) {
        if (StrUtil.isBlank(userId)) {
            throw new IllegalArgumentException("用户 ID 不能为空");
        }
        return ApiResult.success(userCouponService.listByUserId(userId));
    }

    /**
     * 使用用户优惠券
     *
     * @param request 使用请求
     * @return 实际优惠金额
     */
    @PostMapping("/use")
    public ApiResult<BigDecimal> use(@Valid @RequestBody UserCouponUseRequest request) {
        return ApiResult.success(userCouponService.use(request));
    }

    /**
     * 查询模板享元缓存统计
     *
     * @return 缓存统计
     */
    @GetMapping("/template-cache/stats")
    public ApiResult<CouponTemplateCacheStatsVO> stats() {
        return ApiResult.success(userCouponService.stats());
    }
}
```

全局异常处理器用于统一处理参数校验异常和业务异常。

文件位置：`src/main/java/io/github/atengk/designpattern/flyweight/web/GlobalExceptionHandler.java`

```java
package io.github.atengk.designpattern.flyweight.web;

import cn.hutool.core.util.StrUtil;
import io.github.atengk.designpattern.flyweight.vo.ApiResult;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

/**
 * 全局异常处理器
 *
 * @author Ateng
 * @since 2026-05-13
 */
@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    /**
     * 处理参数校验异常
     *
     * @param exception 参数校验异常
     * @return API 返回对象
     */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ApiResult<Void> handleValidException(MethodArgumentNotValidException exception) {
        String message = exception.getBindingResult()
                .getFieldErrors()
                .stream()
                .findFirst()
                .map(error -> StrUtil.format("{} {}", error.getField(), error.getDefaultMessage()))
                .orElse("请求参数不合法");

        log.warn("请求参数校验失败，message={}", message);
        return ApiResult.fail(message);
    }

    /**
     * 处理非法参数异常
     *
     * @param exception 非法参数异常
     * @return API 返回对象
     */
    @ExceptionHandler(IllegalArgumentException.class)
    public ApiResult<Void> handleIllegalArgumentException(IllegalArgumentException exception) {
        log.warn("请求参数错误，message={}", exception.getMessage());
        return ApiResult.fail(exception.getMessage());
    }

    /**
     * 处理业务状态异常
     *
     * @param exception 业务状态异常
     * @return API 返回对象
     */
    @ExceptionHandler(IllegalStateException.class)
    public ApiResult<Void> handleIllegalStateException(IllegalStateException exception) {
        log.warn("业务处理失败，message={}", exception.getMessage());
        return ApiResult.fail(exception.getMessage());
    }

    /**
     * 处理系统异常
     *
     * @param exception 系统异常
     * @return API 返回对象
     */
    @ExceptionHandler(Exception.class)
    public ApiResult<Void> handleException(Exception exception) {
        log.error("系统处理异常", exception);
        return ApiResult.fail("系统处理异常");
    }
}
```

## 使用方式

启动项目后，可以先查看模板享元缓存统计。

```bash
curl -X GET 'http://localhost:8080/api/user-coupons/template-cache/stats'
```

返回示例：

```json
{
  "code": 200,
  "message": "操作成功",
  "data": {
    "cacheSize": 3,
    "repositorySize": 3,
    "preloaded": true
  }
}
```

给同一个用户批量发放 3 张相同模板的优惠券：

```bash
curl -X POST 'http://localhost:8080/api/user-coupons/issue' \
  -H 'Content-Type: application/json' \
  -d '{
    "userId": "user-1001",
    "templateId": "template-10",
    "quantity": 3
  }'
```

返回示例：

```json
{
  "code": 200,
  "message": "操作成功",
  "data": [
    {
      "userCouponId": "UC_xxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxx",
      "userId": "user-1001",
      "templateId": "template-10",
      "templateName": "新人满减券",
      "discountType": "FULL_REDUCTION",
      "thresholdAmount": 100.00,
      "discountAmount": 10.00,
      "status": "UNUSED",
      "receiveTime": "2026-05-13T10:30:00",
      "displayText": "满 100.00 减 10.00"
    }
  ]
}
```

查询用户优惠券列表：

```bash
curl -X GET 'http://localhost:8080/api/user-coupons?userId=user-1001'
```

使用优惠券：

```bash
curl -X POST 'http://localhost:8080/api/user-coupons/use' \
  -H 'Content-Type: application/json' \
  -d '{
    "userCouponId": "UC_xxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxx",
    "orderAmount": 199.90
  }'
```

返回示例：

```json
{
  "code": 200,
  "message": "操作成功",
  "data": 10.00
}
```

订单金额不满足满减门槛时：

```bash
curl -X POST 'http://localhost:8080/api/user-coupons/use' \
  -H 'Content-Type: application/json' \
  -d '{
    "userCouponId": "UC_xxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxx",
    "orderAmount": 50.00
  }'
```

返回示例：

```json
{
  "code": 500,
  "message": "订单金额未满足优惠券使用门槛",
  "data": null
}
```

## 验证方式

可以从下面几个角度验证享元模式是否落地成功。

第一，大量用户券没有重复保存模板信息。`UserCouponContext` 只保存 `userCouponId`、`userId`、`templateId`、`status`、`receiveTime`，模板名称、优惠类型、门槛金额、优惠金额都来自 `CouponTemplateFlyweight`。

第二，相同模板 ID 复用同一个享元对象。日志中的 `identityHash` 可以帮助观察同一个 `templateId` 获取到的对象是否一致。

```text
获取优惠券模板享元对象，templateId=template-10，identityHash=123456789
获取优惠券模板享元对象，templateId=template-10，identityHash=123456789
获取优惠券模板享元对象，templateId=template-10，identityHash=123456789
```

第三，缓存数量和用户券数量不会线性增长。即使给 10000 个用户发放 `template-10`，模板享元缓存中仍然只需要一个 `template-10` 对象。

第四，业务操作通过“享元对象 + 外部状态”完成。展示用户券时，`CouponTemplateFlyweight.toUserCouponVO(context)` 会把共享模板信息和用户券上下文组合成返回对象。

可以重点查看日志：

```text
优惠券模板享元预加载完成，cacheSize=3
用户优惠券发放成功，userCouponId=UC_xxx，userId=user-1001，templateId=template-10
获取优惠券模板享元对象，templateId=template-10，identityHash=123456789
用户优惠券使用成功，userCouponId=UC_xxx，templateId=template-10，orderAmount=199.90，discountAmount=10.00
```

## 扩展 Redis 缓存

示例中使用 `ConcurrentHashMap` 保存享元对象，适合单体应用或本地缓存场景。生产环境中，如果模板数据需要跨实例共享，可以使用 Redis 缓存模板数据。

但要注意：享元对象本身通常是 JVM 内对象，Redis 保存的是可重建享元对象的数据。常见做法是：

```text
Redis 保存模板 JSON
本地 JVM 缓存 CouponTemplateFlyweight
缓存未命中时从 Redis 或数据库加载模板数据
再创建本地享元对象
```

扩展时可以把 `CouponTemplateFlyweightFactory` 中的加载逻辑改成：

```java
CouponTemplateRecord record = loadFromLocalCacheOrRedisOrDatabase(templateId);
return createFlyweight(record);
```

如果模板变化频率较低，可以本地缓存加过期时间。如果模板需要实时生效，可以结合版本号、消息通知或缓存失效事件刷新本地享元对象。

## 扩展新优惠类型

如果要新增折扣券，例如 `DISCOUNT_RATE`，可以先扩展枚举：

```java
/**
 * 折扣券
 */
DISCOUNT_RATE
```

然后在 `CouponTemplateFlyweight` 中扩展计算逻辑：

```java
if (discountType == CouponDiscountType.DISCOUNT_RATE) {
    return orderAmount.subtract(NumberUtil.mul(orderAmount, discountAmount));
}
```

生产项目中，如果优惠类型越来越多，不建议把所有计算逻辑都写在享元对象中。可以把优惠计算进一步拆成策略模式：

```text
CouponTemplateFlyweight 保存共享模板状态
CouponDiscountStrategy 负责不同优惠类型的计算
```

这时享元模式负责共享模板对象，策略模式负责扩展优惠算法，两者可以组合使用。

## 适用场景

享元模式适合存在大量重复对象，并且这些对象可以拆分为共享内部状态和独立外部状态的场景。

常见 Spring Boot 项目场景如下。

| 场景       | 内部状态           | 外部状态                     |
| ---------- | ------------------ | ---------------------------- |
| 优惠券系统 | 优惠券模板         | 用户券 ID、用户 ID、领取状态 |
| 权限系统   | 权限定义、菜单定义 | 用户角色、授权状态           |
| 字典系统   | 字典项定义         | 当前表单字段、当前业务对象   |
| 消息系统   | 消息模板           | 接收人、变量参数、发送状态   |
| 报表系统   | 报表模板           | 查询条件、导出人、导出时间   |
| 工作流系统 | 流程定义、节点定义 | 流程实例、当前审批人、状态   |
| 商品系统   | 商品规格定义       | SKU 库存、价格、销售状态     |

享元模式尤其适合以下特征明显的模块：

```text
对象数量非常多
对象中有大量重复字段
重复字段相对稳定
对象可以拆成共享状态和外部状态
共享对象可以被缓存和复用
```

## 和其他模式的区别

享元模式容易和单例模式、缓存、原型模式、对象池混淆。区分时重点看模式解决的问题。

| 模式     | 关注点                 | 和享元模式的区别                                             |
| -------- | ---------------------- | ------------------------------------------------------------ |
| 享元模式 | 共享大量细粒度重复对象 | 重点是拆分内部状态和外部状态                                 |
| 单例模式 | 保证一个类只有一个实例 | 通常只有一个对象，不强调大量对象共享                         |
| 缓存     | 避免重复查询或计算     | 缓存是一种技术手段，享元强调对象结构设计                     |
| 原型模式 | 通过复制创建新对象     | 重点是复制对象，不是共享对象                                 |
| 对象池   | 复用可变对象资源       | 通常用于连接、线程等昂贵资源，享元对象通常更偏不可变共享数据 |

本示例中，优惠券模板对象被多个用户券共享，而且用户券外部状态独立变化，所以更适合使用享元模式。

## 注意事项

享元对象应尽量设计成不可变对象。示例中的 `CouponTemplateFlyweight` 使用 `final` 字段和只读 getter，避免共享对象被某个业务流程修改后影响其他用户券。

不要把外部状态放进享元对象。比如 `userId`、`userCouponId`、`status` 不应该保存到 `CouponTemplateFlyweight` 中，否则对象就无法安全共享。

享元工厂要考虑线程安全。Spring Boot 服务通常是多线程处理请求，示例中使用 `ConcurrentHashMap` 和 `computeIfAbsent()` 保证并发场景下缓存创建安全。

不要为了少量对象强行使用享元模式。如果对象数量很少，或者重复字段不多，使用享元模式可能只会增加复杂度。

享元模式不能代替数据库设计。用户券表中仍然应该保存 `templateId` 作为外键或关联字段，模板信息应该放在模板表中。享元模式主要解决运行时对象复用和代码结构问题。

模板变更要考虑缓存一致性。如果模板对象已经被缓存，后台修改了模板配置，需要明确是立即刷新、延迟刷新、版本化生效，还是禁止修改已发放模板。

## 总结

享元模式的核心价值是把大量重复对象中的共享部分抽离出来复用，从而降低内存占用和对象创建成本。

在本示例中：

```text
CouponTemplateFlyweight 保存可共享的优惠券模板信息
UserCouponContext 保存每张用户券独有的外部状态
CouponTemplateFlyweightFactory 负责缓存和复用模板享元对象
UserCouponServiceImpl 通过模板享元对象和用户券上下文组合完成业务操作
```

最终效果是：

```text
用户券对象不再重复保存模板字段
相同模板 ID 复用同一个享元对象
共享状态和外部状态边界清晰
大量用户券场景下对象结构更轻量
模板展示和优惠计算逻辑更容易集中维护
```
