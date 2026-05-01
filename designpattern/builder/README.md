# 设计模式：构建者模式

构建者模式用于把复杂对象的创建过程拆分出来，通过一步一步设置参数，最终构建出一个完整对象。在 JDK21 和 Spring Boot 3 项目中，构建者模式常用于复杂 DTO、查询条件、导出配置、第三方请求参数、聚合响应对象、订单创建命令、消息发送命令等场景。

需要注意：构建者模式关注的是“复杂对象如何创建”。如果只是创建简单对象，直接构造方法或 `record` 即可；如果对象字段较多、可选参数较多、构造过程需要校验或默认值，构建者模式更合适。

## 基础配置

本示例基于 JDK21、Spring Boot 3、Maven 项目。示例包路径统一使用 `io.github.atengk`。

文件位置：`pom.xml`

```xml
<dependencies>
    <!-- Spring Boot Web，用于提供接口验证构建者模式行为 -->
    <dependency>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-starter-web</artifactId>
    </dependency>

    <!-- Hutool 工具类，用于字符串、集合、ID、金额等通用处理 -->
    <dependency>
        <groupId>cn.hutool</groupId>
        <artifactId>hutool-all</artifactId>
        <version>5.8.27</version>
    </dependency>

    <!-- Lombok，提供 @Builder、@Getter、@Slf4j 等能力 -->
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

构建者模式的核心目标是把复杂对象的构造过程从业务代码中抽离出来，让调用方通过清晰的链式 API 构建对象。

常见角色如下：

| 角色            | 说明                                   |
| --------------- | -------------------------------------- |
| Product         | 被构建的复杂对象                       |
| Builder         | 抽象构建者，定义构建步骤               |
| ConcreteBuilder | 具体构建者，负责设置字段、默认值和校验 |
| Director        | 指挥者，负责按固定顺序组织构建过程     |
| Client          | 调用方，使用构建者创建对象             |

常见实现方式如下：

| 实现方式          | 是否推荐           | 适用场景                                 |
| ----------------- | ------------------ | ---------------------------------------- |
| 手写 Builder      | 推荐用于理解原理   | 需要自定义校验、默认值、不可变对象       |
| Lombok `@Builder` | 强烈推荐           | Spring Boot 项目中 DTO、Command、VO 构建 |
| 静态工厂方法      | 推荐用于简单对象   | 参数少、构造逻辑简单                     |
| 构造方法重载      | 不推荐用于复杂对象 | 字段多时可读性差                         |
| JavaBean Setter   | 谨慎使用           | 可变对象，可能出现半初始化状态           |

在 Spring Boot 项目中，常见优先级通常是：

```text
Lombok @Builder > 手写 Builder > 多构造方法重载
```

构建者模式尤其适合字段较多的对象。如果构造方法参数太长，调用方很难看出每个参数的含义。

不推荐写法：

```java
new ExportConfig("order", "订单报表", true, 1000, List.of("orderNo", "amount"));
```

推荐写法：

```java
ExportConfig.builder()
        .fileName("order")
        .sheetName("订单报表")
        .includeHeader(true)
        .pageSize(1000)
        .fields(List.of("orderNo", "amount"))
        .build();
```

## 普通 Java 构建者

普通 Java 构建者适合不依赖 Lombok 的对象创建场景。下面以报表导出配置为例，导出配置包含文件名、Sheet 名称、分页大小、导出字段、是否包含表头等多个参数。

如果使用长构造方法，参数顺序很容易写错。使用构建者模式后，每个字段含义更清晰，并且可以在 `build` 阶段统一做默认值和参数校验。

### 文件结构

```text
src/main/java/io/github/atengk/design/builder/simple/
└── ReportExportConfig.java
```

文件位置：`src/main/java/io/github/atengk/design/builder/simple/ReportExportConfig.java`

下面是手写 Builder 示例。该对象构建完成后不可变，适合在多线程环境中安全传递。

```java
package io.github.atengk.design.builder.simple;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.StrUtil;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 报表导出配置
 *
 * @author Ateng
 * @since 2026-04-30
 */
public class ReportExportConfig {

    private final String fileName;
    private final String sheetName;
    private final Boolean includeHeader;
    private final Integer pageSize;
    private final List<String> fields;
    private final LocalDateTime createTime;

    private ReportExportConfig(Builder builder) {
        this.fileName = builder.fileName;
        this.sheetName = builder.sheetName;
        this.includeHeader = builder.includeHeader;
        this.pageSize = builder.pageSize;
        this.fields = List.copyOf(builder.fields);
        this.createTime = builder.createTime;
    }

    /**
     * 创建构建器
     *
     * @return 报表导出配置构建器
     */
    public static Builder builder() {
        return new Builder();
    }

    /**
     * 获取文件名
     *
     * @return 文件名
     */
    public String getFileName() {
        return fileName;
    }

    /**
     * 获取Sheet名称
     *
     * @return Sheet名称
     */
    public String getSheetName() {
        return sheetName;
    }

    /**
     * 是否包含表头
     *
     * @return true 表示包含表头，false 表示不包含
     */
    public Boolean getIncludeHeader() {
        return includeHeader;
    }

    /**
     * 获取分页大小
     *
     * @return 分页大小
     */
    public Integer getPageSize() {
        return pageSize;
    }

    /**
     * 获取导出字段
     *
     * @return 导出字段
     */
    public List<String> getFields() {
        return fields;
    }

    /**
     * 获取创建时间
     *
     * @return 创建时间
     */
    public LocalDateTime getCreateTime() {
        return createTime;
    }

    /**
     * 报表导出配置构建器
     *
     * @author Ateng
     * @since 2026-04-30
     */
    public static class Builder {

        private String fileName;
        private String sheetName;
        private Boolean includeHeader = true;
        private Integer pageSize = 1000;
        private List<String> fields = List.of();
        private LocalDateTime createTime = LocalDateTime.now();

        /**
         * 设置文件名
         *
         * @param fileName 文件名
         * @return 构建器
         */
        public Builder fileName(String fileName) {
            this.fileName = fileName;
            return this;
        }

        /**
         * 设置Sheet名称
         *
         * @param sheetName Sheet名称
         * @return 构建器
         */
        public Builder sheetName(String sheetName) {
            this.sheetName = sheetName;
            return this;
        }

        /**
         * 设置是否包含表头
         *
         * @param includeHeader true 表示包含表头，false 表示不包含
         * @return 构建器
         */
        public Builder includeHeader(Boolean includeHeader) {
            this.includeHeader = includeHeader;
            return this;
        }

        /**
         * 设置分页大小
         *
         * @param pageSize 分页大小
         * @return 构建器
         */
        public Builder pageSize(Integer pageSize) {
            this.pageSize = pageSize;
            return this;
        }

        /**
         * 设置导出字段
         *
         * @param fields 导出字段
         * @return 构建器
         */
        public Builder fields(List<String> fields) {
            this.fields = fields;
            return this;
        }

        /**
         * 设置创建时间
         *
         * @param createTime 创建时间
         * @return 构建器
         */
        public Builder createTime(LocalDateTime createTime) {
            this.createTime = createTime;
            return this;
        }

        /**
         * 构建报表导出配置
         *
         * @return 报表导出配置
         */
        public ReportExportConfig build() {
            validate();
            return new ReportExportConfig(this);
        }

        /**
         * 校验构建参数
         */
        private void validate() {
            if (StrUtil.isBlank(fileName)) {
                throw new IllegalArgumentException("文件名不能为空");
            }

            if (StrUtil.isBlank(sheetName)) {
                throw new IllegalArgumentException("Sheet名称不能为空");
            }

            if (pageSize == null || pageSize <= 0) {
                throw new IllegalArgumentException("分页大小必须大于0");
            }

            if (CollUtil.isEmpty(fields)) {
                throw new IllegalArgumentException("导出字段不能为空");
            }

            if (includeHeader == null) {
                includeHeader = true;
            }

            if (createTime == null) {
                createTime = LocalDateTime.now();
            }
        }
    }
}
```

使用方式：

```java
ReportExportConfig config = ReportExportConfig.builder()
        .fileName("order-report")
        .sheetName("订单报表")
        .includeHeader(true)
        .pageSize(1000)
        .fields(List.of("orderNo", "amount", "status", "createTime"))
        .build();
```

手写 Builder 的优点是可控性强，可以在 `build` 方法中处理默认值、参数校验、不可变拷贝等逻辑。缺点是样板代码较多，字段多时维护成本较高。

## Lombok 构建者

Spring Boot 项目中更常见的写法是使用 Lombok `@Builder`。它可以自动生成构建器，减少大量样板代码。

下面以订单创建命令为例。订单创建命令通常由 Controller 请求参数、用户上下文、商品信息、优惠计算结果等多个来源组装而来，非常适合使用构建者模式。

### 文件结构

```text
src/main/java/io/github/atengk/design/builder/lombok/
├── OrderCreateCommand.java
└── OrderCreateResult.java
```

文件位置：`src/main/java/io/github/atengk/design/builder/lombok/OrderCreateCommand.java`

下面是使用 Lombok `@Builder` 的订单创建命令对象。

```java
package io.github.atengk.design.builder.lombok;

import lombok.Builder;
import lombok.Getter;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 订单创建命令
 *
 * @author Ateng
 * @since 2026-04-30
 */
@Getter
@Builder
public class OrderCreateCommand {

    private final Long userId;
    private final Long productId;
    private final String productName;
    private final Integer quantity;
    private final BigDecimal unitPrice;
    private final BigDecimal discountAmount;
    private final BigDecimal totalAmount;
    private final String source;
    private final LocalDateTime createTime;
}
```

文件位置：`src/main/java/io/github/atengk/design/builder/lombok/OrderCreateResult.java`

下面是使用 Lombok `@Builder` 的订单创建结果对象。

```java
package io.github.atengk.design.builder.lombok;

import lombok.Builder;
import lombok.Getter;

import java.math.BigDecimal;

/**
 * 订单创建结果
 *
 * @author Ateng
 * @since 2026-04-30
 */
@Getter
@Builder
public class OrderCreateResult {

    private final String orderNo;
    private final Long userId;
    private final String productName;
    private final Integer quantity;
    private final BigDecimal totalAmount;
    private final String message;
}
```

使用方式：

```java
OrderCreateCommand command = OrderCreateCommand.builder()
        .userId(10001L)
        .productId(20001L)
        .productName("键盘")
        .quantity(2)
        .unitPrice(BigDecimal.valueOf(199))
        .discountAmount(BigDecimal.valueOf(20))
        .totalAmount(BigDecimal.valueOf(378))
        .source("APP")
        .createTime(LocalDateTime.now())
        .build();

OrderCreateResult result = OrderCreateResult.builder()
        .orderNo("ORDER10001")
        .userId(command.getUserId())
        .productName(command.getProductName())
        .quantity(command.getQuantity())
        .totalAmount(command.getTotalAmount())
        .message("创建成功")
        .build();
```

Lombok `@Builder` 的优点是代码简洁，适合 DTO、Command、VO、复杂查询条件等对象。缺点是默认不会自动做业务校验，如果需要强校验，可以在外层 Assembler、Factory 或 Service 中处理。

## Spring Boot 构建者

Spring Boot 项目中，构建者模式通常不会单独存在，而是和 DTO、Command、Assembler、Service 配合使用。下面以订单创建为例，Controller 接收简单请求，Assembler 负责构建复杂命令对象，Service 负责执行业务。

整体流程如下：

```text
Controller 接收请求 -> Assembler 构建命令对象 -> Service 创建订单 -> Builder 构建响应对象
```

这种写法可以避免 Controller 和 Service 中堆积大量对象组装代码。

### 文件结构

```text
src/main/java/io/github/atengk/design/
├── BuilderApplication.java
├── assembler/
│   └── OrderCreateAssembler.java
├── controller/
│   └── OrderCreateController.java
├── dto/
│   ├── OrderCreateRequest.java
│   ├── OrderCreateCommand.java
│   └── OrderCreateResponse.java
└── service/
    ├── OrderCreateService.java
    └── impl/
        └── OrderCreateServiceImpl.java
```

文件位置：`src/main/java/io/github/atengk/design/BuilderApplication.java`

下面是 Spring Boot 启动类。

```java
package io.github.atengk.design;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * 构建者模式示例启动类
 *
 * @author Ateng
 * @since 2026-04-30
 */
@SpringBootApplication
public class BuilderApplication {

    /**
     * 应用启动入口
     *
     * @param args 启动参数
     */
    public static void main(String[] args) {
        SpringApplication.run(BuilderApplication.class, args);
    }
}
```

文件位置：`src/main/java/io/github/atengk/design/dto/OrderCreateRequest.java`

下面是订单创建请求对象。请求对象字段尽量保持和接口入参一致，不承载复杂业务组装逻辑。

```java
package io.github.atengk.design.dto;

import java.math.BigDecimal;

/**
 * 订单创建请求
 *
 * @param userId      用户ID
 * @param productId   商品ID
 * @param productName 商品名称
 * @param quantity    购买数量
 * @param unitPrice   商品单价
 * @param source      来源渠道
 * @author Ateng
 * @since 2026-04-30
 */
public record OrderCreateRequest(
        Long userId,
        Long productId,
        String productName,
        Integer quantity,
        BigDecimal unitPrice,
        String source
) {
}
```

文件位置：`src/main/java/io/github/atengk/design/dto/OrderCreateCommand.java`

下面是订单创建命令对象。它由 Assembler 统一构建，供 Service 使用。

```java
package io.github.atengk.design.dto;

import lombok.Builder;
import lombok.Getter;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 订单创建命令
 *
 * @author Ateng
 * @since 2026-04-30
 */
@Getter
@Builder
public class OrderCreateCommand {

    private final Long userId;
    private final Long productId;
    private final String productName;
    private final Integer quantity;
    private final BigDecimal unitPrice;
    private final BigDecimal originalAmount;
    private final BigDecimal discountAmount;
    private final BigDecimal totalAmount;
    private final String source;
    private final String requestNo;
    private final LocalDateTime createTime;
}
```

文件位置：`src/main/java/io/github/atengk/design/dto/OrderCreateResponse.java`

下面是订单创建响应对象，通过 Builder 构建统一响应。

```java
package io.github.atengk.design.dto;

import lombok.Builder;
import lombok.Getter;

import java.math.BigDecimal;

/**
 * 订单创建响应
 *
 * @author Ateng
 * @since 2026-04-30
 */
@Getter
@Builder
public class OrderCreateResponse {

    private final String orderNo;
    private final String requestNo;
    private final Long userId;
    private final String productName;
    private final Integer quantity;
    private final BigDecimal originalAmount;
    private final BigDecimal discountAmount;
    private final BigDecimal totalAmount;
    private final String message;
}
```

文件位置：`src/main/java/io/github/atengk/design/assembler/OrderCreateAssembler.java`

下面是订单创建装配器。它负责把接口请求转换成业务命令对象，并统一处理金额计算、默认值、请求流水号等组装逻辑。

```java
package io.github.atengk.design.assembler;

import cn.hutool.core.util.IdUtil;
import cn.hutool.core.util.NumberUtil;
import cn.hutool.core.util.StrUtil;
import io.github.atengk.design.dto.OrderCreateCommand;
import io.github.atengk.design.dto.OrderCreateRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;

/**
 * 订单创建装配器
 *
 * @author Ateng
 * @since 2026-04-30
 */
@Slf4j
@Component
public class OrderCreateAssembler {

    private static final BigDecimal DISCOUNT_THRESHOLD_AMOUNT = BigDecimal.valueOf(100);
    private static final BigDecimal DISCOUNT_AMOUNT = BigDecimal.valueOf(20);
    private static final String DEFAULT_SOURCE = "UNKNOWN";

    /**
     * 构建订单创建命令
     *
     * @param request 订单创建请求
     * @return 订单创建命令
     */
    public OrderCreateCommand buildCommand(OrderCreateRequest request) {
        validateRequest(request);

        BigDecimal originalAmount = NumberUtil.mul(request.unitPrice(), BigDecimal.valueOf(request.quantity()))
                .setScale(2, RoundingMode.HALF_UP);
        BigDecimal discountAmount = calculateDiscountAmount(originalAmount);
        BigDecimal totalAmount = NumberUtil.sub(originalAmount, discountAmount)
                .setScale(2, RoundingMode.HALF_UP);

        String requestNo = "REQ" + IdUtil.getSnowflakeNextId();
        String source = StrUtil.blankToDefault(request.source(), DEFAULT_SOURCE);

        log.info("构建订单创建命令，用户ID：{}，商品ID：{}，原始金额：{}，优惠金额：{}，应付金额：{}",
                request.userId(), request.productId(), originalAmount, discountAmount, totalAmount);

        return OrderCreateCommand.builder()
                .userId(request.userId())
                .productId(request.productId())
                .productName(request.productName())
                .quantity(request.quantity())
                .unitPrice(request.unitPrice())
                .originalAmount(originalAmount)
                .discountAmount(discountAmount)
                .totalAmount(totalAmount)
                .source(source)
                .requestNo(requestNo)
                .createTime(LocalDateTime.now())
                .build();
    }

    /**
     * 计算优惠金额
     *
     * @param originalAmount 原始金额
     * @return 优惠金额
     */
    private BigDecimal calculateDiscountAmount(BigDecimal originalAmount) {
        if (originalAmount.compareTo(DISCOUNT_THRESHOLD_AMOUNT) >= 0) {
            return DISCOUNT_AMOUNT;
        }

        return BigDecimal.ZERO;
    }

    /**
     * 校验订单创建请求
     *
     * @param request 订单创建请求
     */
    private void validateRequest(OrderCreateRequest request) {
        if (request == null) {
            log.warn("构建订单创建命令失败，请求参数为空");
            throw new IllegalArgumentException("请求参数不能为空");
        }

        if (request.userId() == null || request.userId() <= 0) {
            log.warn("构建订单创建命令失败，用户ID不合法，用户ID：{}", request.userId());
            throw new IllegalArgumentException("用户ID必须大于0");
        }

        if (request.productId() == null || request.productId() <= 0) {
            log.warn("构建订单创建命令失败，商品ID不合法，商品ID：{}", request.productId());
            throw new IllegalArgumentException("商品ID必须大于0");
        }

        if (StrUtil.isBlank(request.productName())) {
            log.warn("构建订单创建命令失败，商品名称为空");
            throw new IllegalArgumentException("商品名称不能为空");
        }

        if (request.quantity() == null || request.quantity() <= 0) {
            log.warn("构建订单创建命令失败，购买数量不合法，购买数量：{}", request.quantity());
            throw new IllegalArgumentException("购买数量必须大于0");
        }

        if (request.unitPrice() == null || request.unitPrice().compareTo(BigDecimal.ZERO) <= 0) {
            log.warn("构建订单创建命令失败，商品单价不合法，商品单价：{}", request.unitPrice());
            throw new IllegalArgumentException("商品单价必须大于0");
        }
    }
}
```

文件位置：`src/main/java/io/github/atengk/design/service/OrderCreateService.java`

下面是订单创建服务接口。

```java
package io.github.atengk.design.service;

import io.github.atengk.design.dto.OrderCreateCommand;
import io.github.atengk.design.dto.OrderCreateResponse;

/**
 * 订单创建服务
 *
 * @author Ateng
 * @since 2026-04-30
 */
public interface OrderCreateService {

    /**
     * 创建订单
     *
     * @param command 订单创建命令
     * @return 订单创建响应
     */
    OrderCreateResponse createOrder(OrderCreateCommand command);
}
```

文件位置：`src/main/java/io/github/atengk/design/service/impl/OrderCreateServiceImpl.java`

下面是订单创建服务实现。它接收构建好的命令对象，执行业务逻辑，并通过 Builder 构建响应对象。

```java
package io.github.atengk.design.service.impl;

import cn.hutool.core.util.IdUtil;
import io.github.atengk.design.dto.OrderCreateCommand;
import io.github.atengk.design.dto.OrderCreateResponse;
import io.github.atengk.design.service.OrderCreateService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

/**
 * 订单创建服务实现
 *
 * @author Ateng
 * @since 2026-04-30
 */
@Slf4j
@Service
public class OrderCreateServiceImpl implements OrderCreateService {

    /**
     * 创建订单
     *
     * @param command 订单创建命令
     * @return 订单创建响应
     */
    @Override
    public OrderCreateResponse createOrder(OrderCreateCommand command) {
        String orderNo = "ORDER" + IdUtil.getSnowflakeNextId();

        log.info("创建订单成功，订单号：{}，请求流水号：{}，用户ID：{}，商品ID：{}，应付金额：{}",
                orderNo, command.getRequestNo(), command.getUserId(), command.getProductId(), command.getTotalAmount());

        return OrderCreateResponse.builder()
                .orderNo(orderNo)
                .requestNo(command.getRequestNo())
                .userId(command.getUserId())
                .productName(command.getProductName())
                .quantity(command.getQuantity())
                .originalAmount(command.getOriginalAmount())
                .discountAmount(command.getDiscountAmount())
                .totalAmount(command.getTotalAmount())
                .message("创建成功")
                .build();
    }
}
```

文件位置：`src/main/java/io/github/atengk/design/controller/OrderCreateController.java`

下面是订单创建接口。Controller 只负责接收参数和调用装配器，不直接拼装复杂命令对象。

```java
package io.github.atengk.design.controller;

import io.github.atengk.design.assembler.OrderCreateAssembler;
import io.github.atengk.design.dto.OrderCreateCommand;
import io.github.atengk.design.dto.OrderCreateRequest;
import io.github.atengk.design.dto.OrderCreateResponse;
import io.github.atengk.design.service.OrderCreateService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;

/**
 * 订单创建控制器
 *
 * @author Ateng
 * @since 2026-04-30
 */
@RestController
@RequiredArgsConstructor
@RequestMapping("/builder/order")
public class OrderCreateController {

    private final OrderCreateAssembler orderCreateAssembler;
    private final OrderCreateService orderCreateService;

    /**
     * 创建订单
     *
     * @param userId      用户ID
     * @param productId   商品ID
     * @param productName 商品名称
     * @param quantity    购买数量
     * @param unitPrice   商品单价
     * @param source      来源渠道
     * @return 订单创建响应
     */
    @PostMapping("/create")
    public OrderCreateResponse createOrder(@RequestParam Long userId,
                                           @RequestParam Long productId,
                                           @RequestParam String productName,
                                           @RequestParam Integer quantity,
                                           @RequestParam BigDecimal unitPrice,
                                           @RequestParam(required = false) String source) {
        OrderCreateRequest request = new OrderCreateRequest(
                userId,
                productId,
                productName,
                quantity,
                unitPrice,
                source
        );

        OrderCreateCommand command = orderCreateAssembler.buildCommand(request);
        return orderCreateService.createOrder(command);
    }
}
```

接口调用示例：

```bash
curl -X POST "http://localhost:8080/builder/order/create?userId=10001&productId=20001&productName=键盘&quantity=2&unitPrice=199.00&source=APP"
```

可能返回：

```json
{
  "orderNo": "ORDER2019776866538487808",
  "requestNo": "REQ2019776866538487807",
  "userId": 10001,
  "productName": "键盘",
  "quantity": 2,
  "originalAmount": 398.00,
  "discountAmount": 20,
  "totalAmount": 378.00,
  "message": "创建成功"
}
```

这种方式的优点是对象构建逻辑集中在 `OrderCreateAssembler` 中，Controller 和 Service 都更加干净。后续如果新增优惠字段、渠道字段、链路追踪字段，也可以集中调整构建过程。

## 扩展复杂查询条件

构建者模式也很适合复杂查询条件。下面以订单查询条件为例，查询条件通常有很多可选字段，如果使用构造方法会非常混乱。

文件位置：`src/main/java/io/github/atengk/design/dto/OrderQueryCondition.java`

下面是订单查询条件对象，使用 Lombok `@Builder` 简化构建。

```java
package io.github.atengk.design.dto;

import lombok.Builder;
import lombok.Getter;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 订单查询条件
 *
 * @author Ateng
 * @since 2026-04-30
 */
@Getter
@Builder
public class OrderQueryCondition {

    private final Long userId;
    private final Long productId;
    private final String orderNo;
    private final String orderStatus;
    private final BigDecimal minAmount;
    private final BigDecimal maxAmount;
    private final LocalDateTime startTime;
    private final LocalDateTime endTime;
    private final Integer pageNum;
    private final Integer pageSize;
}
```

使用方式：

```java
OrderQueryCondition condition = OrderQueryCondition.builder()
        .userId(10001L)
        .orderStatus("PAID")
        .minAmount(BigDecimal.valueOf(100))
        .maxAmount(BigDecimal.valueOf(1000))
        .pageNum(1)
        .pageSize(20)
        .build();
```

对于复杂查询条件，Builder 的优势非常明显：调用方只设置自己关心的字段，不需要传入大量 `null`。

不推荐写法：

```java
new OrderQueryCondition(10001L, null, null, "PAID", BigDecimal.valueOf(100), BigDecimal.valueOf(1000), null, null, 1, 20);
```

这种构造方法参数过长，字段含义不清晰，也容易传错顺序。

## 构建者模式和工厂模式的区别

构建者模式和工厂模式都属于创建型设计模式，但关注点不同。

| 对比项   | 构建者模式                 | 工厂模式                           |
| -------- | -------------------------- | ---------------------------------- |
| 核心目的 | 一步一步构建复杂对象       | 根据类型创建不同对象               |
| 关注点   | 对象内部字段和构建过程     | 创建哪个具体实现类                 |
| 适合对象 | 字段多、参数多、可选项多   | 多个子类、多种产品类型             |
| 调用方式 | 链式设置字段后 `build`     | 传入类型后返回对象                 |
| 典型场景 | DTO、Command、VO、查询条件 | 支付处理器、文件解析器、消息发送器 |

简单理解：

```text
构建者模式：我知道要创建哪个对象，但这个对象参数很多。
工厂模式：我不确定要创建哪个对象，需要根据类型决定。
```

创建订单命令、导出配置、查询条件，更适合构建者模式。根据支付渠道创建支付宝处理器、微信处理器，更适合工厂模式。

## 构建者模式和 JavaBean Setter 的区别

构建者模式和 JavaBean Setter 都能设置多个字段，但对象状态管理不同。

| 对比项   | 构建者模式                   | JavaBean Setter                  |
| -------- | ---------------------------- | -------------------------------- |
| 对象状态 | 构建完成后可设计为不可变     | 通常是可变对象                   |
| 参数校验 | 可集中在 `build` 阶段        | 分散在各个 Setter 或业务代码中   |
| 可读性   | 链式调用，字段含义清晰       | 字段含义清晰，但对象可能半初始化 |
| 线程安全 | 不可变对象更容易线程安全     | 可变对象需要额外控制             |
| 适合场景 | 复杂 DTO、命令对象、配置对象 | 框架绑定对象、简单表单对象       |

JavaBean Setter 的问题是对象可能处于半初始化状态。

```java
OrderCreateCommand command = new OrderCreateCommand();
command.setUserId(10001L);
// 此时 productId、quantity、amount 等字段可能还没设置，对象状态不完整
```

构建者模式则可以在最后统一校验：

```java
OrderCreateCommand command = OrderCreateCommand.builder()
        .userId(10001L)
        .productId(20001L)
        .quantity(2)
        .totalAmount(BigDecimal.valueOf(398))
        .build();
```

如果使用手写 Builder，可以在 `build` 阶段确保必要字段完整。

## 验证方式

启动 Spring Boot 项目：

```bash
mvn spring-boot:run
```

执行订单创建接口：

```bash
curl -X POST "http://localhost:8080/builder/order/create?userId=10001&productId=20001&productName=键盘&quantity=2&unitPrice=199.00&source=APP"
```

如果构建者模式流程正常，可以看到类似日志：

```text
构建订单创建命令，用户ID：10001，商品ID：20001，原始金额：398.00，优惠金额：20，应付金额：378.00
创建订单成功，订单号：ORDER2019776866538487808，请求流水号：REQ2019776866538487807，用户ID：10001，商品ID：20001，应付金额：378.00
```

执行不传来源渠道的请求：

```bash
curl -X POST "http://localhost:8080/builder/order/create?userId=10001&productId=20001&productName=键盘&quantity=1&unitPrice=99.00"
```

此时 `OrderCreateAssembler` 会使用默认来源：

```text
UNKNOWN
```

执行异常请求：

```bash
curl -X POST "http://localhost:8080/builder/order/create?userId=10001&productId=20001&productName=键盘&quantity=0&unitPrice=199.00&source=APP"
```

异常日志示例：

```text
构建订单创建命令失败，购买数量不合法，购买数量：0
```

实际项目中建议结合全局异常处理器，将业务异常转换成统一响应结构。

## 注意事项

构建者模式适合复杂对象，但不建议所有对象都使用 Builder。字段很少、语义清晰的对象，直接使用 `record` 或构造方法即可。

适合使用 Builder 的对象：

```text
字段数量较多
可选参数较多
构建过程需要默认值
构建过程需要参数校验
对象希望设计为不可变
对象由多个来源的数据组装而成
```

不太适合使用 Builder 的对象：

```text
只有一两个字段的简单对象
纯数据库实体 Entity
需要被框架频繁反射赋值的表单对象
生命周期很短且构造简单的临时对象
```

不推荐滥用 Builder：

```java
UserId userId = UserId.builder()
        .value(10001L)
        .build();
```

这种对象只有一个字段，使用 Builder 反而增加复杂度。

对于 Lombok `@Builder`，需要注意默认值问题。字段直接赋默认值不会自动进入 Builder，应该使用 `@Builder.Default`。

示例：

```java
@Getter
@Builder
public class ExportTask {

    @Builder.Default
    private final Integer pageSize = 1000;
}
```

如果不加 `@Builder.Default`，通过 Builder 构建时 `pageSize` 可能是 `null`。

对于集合字段，建议避免外部集合被修改后影响对象内部状态。手写 Builder 时可以使用不可变拷贝：

```java
this.fields = List.copyOf(builder.fields);
```

如果 Builder 构建的是业务命令对象，建议在 Assembler 或 Factory 中集中构建，不要让 Controller 直接写大量 Builder 链式代码。

不推荐写法：

```java
@PostMapping("/create")
public OrderCreateResponse create(...) {
    OrderCreateCommand command = OrderCreateCommand.builder()
            .userId(userId)
            .productId(productId)
            .productName(productName)
            .quantity(quantity)
            .unitPrice(unitPrice)
            .originalAmount(...)
            .discountAmount(...)
            .totalAmount(...)
            .build();

    return orderCreateService.createOrder(command);
}
```

推荐写法：

```java
OrderCreateRequest request = new OrderCreateRequest(userId, productId, productName, quantity, unitPrice, source);
OrderCreateCommand command = orderCreateAssembler.buildCommand(request);
return orderCreateService.createOrder(command);
```

这样可以让 Controller 保持轻量，对象构建规则也更容易复用。

## 总结

在 JDK21 和 Spring Boot 3 项目中，构建者模式的实践重点是让复杂对象创建过程更清晰、更安全、更易维护。

普通 Java 手写 Builder 适合需要严格控制默认值、校验和不可变性的场景。Lombok `@Builder` 适合 Spring Boot 项目中的 DTO、Command、VO、查询条件等对象。对于复杂业务对象，推荐使用“Request 接收参数，Assembler 使用 Builder 构建 Command，Service 执行业务并使用 Builder 构建 Response”的结构。

构建者模式不是为了替代所有构造方法，而是为了处理字段多、参数多、可选项多、构建逻辑复杂的对象。合理使用可以显著提升代码可读性，减少长参数构造方法和对象半初始化问题。
