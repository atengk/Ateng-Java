# 构建者模式

构建者模式属于创建型模式，用于分步骤构建复杂对象。它适合对象字段较多、部分字段可选、创建过程需要校验、对象构建过程不希望散落在业务代码中的场景。在当前 29 个设计模式文档体系中，构建者模式属于 GoF 创建型模式，模块名为 `builder`。

## 基础配置

本示例基于 JDK 21、Spring Boot 3、Maven、Hutool、Lombok 编写。示例场景是“订单提交上下文构建”。订单提交时需要组合用户、商品明细、收货地址、优惠券、请求来源、备注、幂等请求号等信息，这类对象字段较多，并且存在必填校验和默认值处理，适合使用构建者模式。

文件位置：`pom.xml`

```xml
<dependencies>
    <!-- Spring Boot Web，用于提供订单提交接口 -->
    <dependency>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-starter-web</artifactId>
    </dependency>

    <!-- Spring Boot Validation，用于请求参数校验 -->
    <dependency>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-starter-validation</artifactId>
    </dependency>

    <!-- Hutool 工具类，用于字符串、集合、ID、日期、JSON 等常用处理 -->
    <dependency>
        <groupId>cn.hutool</groupId>
        <artifactId>hutool-all</artifactId>
        <version>5.8.36</version>
    </dependency>

    <!-- Lombok，用于减少 getter、setter、构造方法和日志样板代码 -->
    <dependency>
        <groupId>org.projectlombok</groupId>
        <artifactId>lombok</artifactId>
        <optional>true</optional>
    </dependency>

    <!-- Spring Boot 测试依赖，用于验证构建者模式 -->
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
  port: 8080

demo:
  builder:
    # 默认订单来源
    default-source: WEB
    # 默认超时时间，单位分钟
    default-timeout-minutes: 30
    # 是否允许提交空备注
    allow-empty-remark: true
```

## 模式说明

构建者模式解决的是“复杂对象如何清晰创建”的问题。它把对象创建过程从业务代码中抽离出来，通过链式方法一步一步设置字段，最后通过 `build()` 方法生成目标对象。

在 Spring Boot 项目中，构建者模式常见于：

```text
创建复杂请求上下文
创建第三方接口请求对象
创建导出任务参数
创建消息发送对象
创建订单提交命令
创建查询条件对象
创建测试数据对象
创建不可变业务对象
```

如果不用构建者模式，复杂对象通常会出现以下问题：

```text
构造方法参数过多，可读性差
多个构造方法重载，维护困难
setter 到处调用，创建过程散落
必填字段和默认值缺少统一校验
对象创建完成后仍可被任意修改
```

构建者模式适合字段较多且创建过程有规则的对象。对于只有两三个字段的简单 DTO，没有必要强行使用。

## 项目结构

本示例使用手写 Builder 来体现模式结构，同时补充 Lombok `@Builder` 的轻量写法。核心类是 `OrderSubmitContext`，它表示订单提交过程中的不可变业务上下文。

```text
src/main/java/io/github/atengk/pattern/builder
├── BuilderApplication.java
├── config
│   └── BuilderDemoProperties.java
├── controller
│   └── OrderSubmitController.java
├── domain
│   └── OrderSubmitContext.java
├── dto
│   ├── OrderAddressDTO.java
│   ├── OrderItemDTO.java
│   └── OrderSubmitDTO.java
├── enums
│   └── OrderSourceEnum.java
├── service
│   ├── OrderSubmitService.java
│   └── OrderSubmitServiceImpl.java
└── vo
    └── OrderSubmitVO.java
```

## 核心代码

这一部分给出构建者模式的完整核心代码。重点是 `OrderSubmitContext`，它通过内部静态类 `Builder` 分步骤构建订单提交上下文，并在 `build()` 方法中完成统一校验、默认值处理和不可变对象创建。

文件位置：`src/main/java/io/github/atengk/pattern/builder/BuilderApplication.java`

```java
package io.github.atengk.pattern.builder;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * 构建者模式示例启动类
 *
 * @author Ateng
 * @since 2026-05-13
 */
@SpringBootApplication
public class BuilderApplication {

    /**
     * 启动构建者模式示例应用
     *
     * @param args 启动参数
     */
    public static void main(String[] args) {
        SpringApplication.run(BuilderApplication.class, args);
    }

}
```

文件位置：`src/main/java/io/github/atengk/pattern/builder/config/BuilderDemoProperties.java`

```java
package io.github.atengk.pattern.builder.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * 构建者模式示例配置
 *
 * @author Ateng
 * @since 2026-05-13
 */
@Data
@Component
@ConfigurationProperties(prefix = "demo.builder")
public class BuilderDemoProperties {

    /**
     * 默认订单来源
     */
    private String defaultSource = "WEB";

    /**
     * 默认超时时间，单位分钟
     */
    private Integer defaultTimeoutMinutes = 30;

    /**
     * 是否允许空备注
     */
    private Boolean allowEmptyRemark = true;

}
```

下面的枚举用于定义订单来源，避免在业务代码中散落魔法字符串。

文件位置：`src/main/java/io/github/atengk/pattern/builder/enums/OrderSourceEnum.java`

```java
package io.github.atengk.pattern.builder.enums;

import cn.hutool.core.util.StrUtil;
import lombok.Getter;

import java.util.Arrays;

/**
 * 订单来源枚举
 *
 * @author Ateng
 * @since 2026-05-13
 */
@Getter
public enum OrderSourceEnum {

    WEB("WEB", "网页端"),

    APP("APP", "移动端"),

    MINI_PROGRAM("MINI_PROGRAM", "小程序"),

    ADMIN("ADMIN", "管理后台");

    private final String code;

    private final String description;

    OrderSourceEnum(String code, String description) {
        this.code = code;
        this.description = description;
    }

    /**
     * 根据编码解析订单来源
     *
     * @param code 来源编码
     * @return 订单来源
     */
    public static OrderSourceEnum parse(String code) {
        return Arrays.stream(values())
                .filter(item -> StrUtil.equalsIgnoreCase(item.getCode(), code))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException(StrUtil.format("不支持的订单来源：{}", code)));
    }

}
```

## DTO 和 VO

这一部分定义接口请求对象和响应对象。DTO 用于接收前端请求，真正进入业务流程前，会被转换成不可变的 `OrderSubmitContext`。

文件位置：`src/main/java/io/github/atengk/pattern/builder/dto/OrderItemDTO.java`

```java
package io.github.atengk.pattern.builder.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.math.BigDecimal;

/**
 * 订单商品明细请求对象
 *
 * @author Ateng
 * @since 2026-05-13
 */
@Data
public class OrderItemDTO {

    /**
     * 商品编号
     */
    @NotBlank(message = "商品编号不能为空")
    private String skuId;

    /**
     * 商品名称
     */
    @NotBlank(message = "商品名称不能为空")
    private String skuName;

    /**
     * 购买数量
     */
    @NotNull(message = "购买数量不能为空")
    @Min(value = 1, message = "购买数量必须大于0")
    private Integer quantity;

    /**
     * 商品单价
     */
    @NotNull(message = "商品单价不能为空")
    private BigDecimal price;

}
```

文件位置：`src/main/java/io/github/atengk/pattern/builder/dto/OrderAddressDTO.java`

```java
package io.github.atengk.pattern.builder.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

/**
 * 订单收货地址请求对象
 *
 * @author Ateng
 * @since 2026-05-13
 */
@Data
public class OrderAddressDTO {

    /**
     * 收货人姓名
     */
    @NotBlank(message = "收货人姓名不能为空")
    private String receiverName;

    /**
     * 收货人手机号
     */
    @NotBlank(message = "收货人手机号不能为空")
    private String receiverPhone;

    /**
     * 省份
     */
    @NotBlank(message = "省份不能为空")
    private String province;

    /**
     * 城市
     */
    @NotBlank(message = "城市不能为空")
    private String city;

    /**
     * 详细地址
     */
    @NotBlank(message = "详细地址不能为空")
    private String detailAddress;

}
```

文件位置：`src/main/java/io/github/atengk/pattern/builder/dto/OrderSubmitDTO.java`

```java
package io.github.atengk.pattern.builder.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import lombok.Data;

import java.util.List;

/**
 * 订单提交请求对象
 *
 * @author Ateng
 * @since 2026-05-13
 */
@Data
public class OrderSubmitDTO {

    /**
     * 用户编号
     */
    @NotBlank(message = "用户编号不能为空")
    private String userId;

    /**
     * 订单来源：WEB、APP、MINI_PROGRAM、ADMIN
     */
    private String source;

    /**
     * 幂等请求号
     */
    private String requestNo;

    /**
     * 优惠券编号
     */
    private String couponId;

    /**
     * 订单备注
     */
    private String remark;

    /**
     * 收货地址
     */
    @Valid
    private OrderAddressDTO address;

    /**
     * 商品明细
     */
    @Valid
    @NotEmpty(message = "商品明细不能为空")
    private List<OrderItemDTO> items;

}
```

文件位置：`src/main/java/io/github/atengk/pattern/builder/vo/OrderSubmitVO.java`

```java
package io.github.atengk.pattern.builder.vo;

import java.math.BigDecimal;

/**
 * 订单提交响应对象
 *
 * @author Ateng
 * @since 2026-05-13
 */
public record OrderSubmitVO(
        String orderNo,
        String userId,
        String source,
        String requestNo,
        BigDecimal totalAmount,
        Integer totalQuantity,
        Boolean success,
        String message,
        String submitTime
) {
}
```

## 手写构建者实现

这一部分是构建者模式的核心。`OrderSubmitContext` 的构造方法私有化，外部不能直接 `new`，只能通过 `OrderSubmitContext.builder()` 获取构建器并逐步设置参数。

`build()` 方法负责完成以下工作：

```text
校验必填字段
处理默认值
计算订单总金额
计算商品总数量
复制集合防止外部修改
创建不可变业务上下文对象
```

文件位置：`src/main/java/io/github/atengk/pattern/builder/domain/OrderSubmitContext.java`

```java
package io.github.atengk.pattern.builder.domain;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.IdUtil;
import cn.hutool.core.util.NumberUtil;
import cn.hutool.core.util.StrUtil;
import io.github.atengk.pattern.builder.dto.OrderAddressDTO;
import io.github.atengk.pattern.builder.dto.OrderItemDTO;
import io.github.atengk.pattern.builder.enums.OrderSourceEnum;
import lombok.Getter;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * 订单提交上下文
 *
 * @author Ateng
 * @since 2026-05-13
 */
@Getter
public class OrderSubmitContext {

    private final String orderNo;

    private final String userId;

    private final OrderSourceEnum source;

    private final String requestNo;

    private final String couponId;

    private final String remark;

    private final OrderAddressDTO address;

    private final List<OrderItemDTO> items;

    private final BigDecimal totalAmount;

    private final Integer totalQuantity;

    private final Integer timeoutMinutes;

    private OrderSubmitContext(Builder builder) {
        this.orderNo = builder.orderNo;
        this.userId = builder.userId;
        this.source = builder.source;
        this.requestNo = builder.requestNo;
        this.couponId = builder.couponId;
        this.remark = builder.remark;
        this.address = builder.address;
        this.items = Collections.unmodifiableList(new ArrayList<>(builder.items));
        this.totalAmount = builder.totalAmount;
        this.totalQuantity = builder.totalQuantity;
        this.timeoutMinutes = builder.timeoutMinutes;
    }

    /**
     * 创建订单提交上下文构建器
     *
     * @return 构建器
     */
    public static Builder builder() {
        return new Builder();
    }

    /**
     * 订单提交上下文构建器
     *
     * @author Ateng
     * @since 2026-05-13
     */
    public static class Builder {

        private String orderNo;

        private String userId;

        private OrderSourceEnum source;

        private String requestNo;

        private String couponId;

        private String remark;

        private OrderAddressDTO address;

        private List<OrderItemDTO> items = new ArrayList<>();

        private BigDecimal totalAmount = BigDecimal.ZERO;

        private Integer totalQuantity = 0;

        private Integer timeoutMinutes = 30;

        private Boolean allowEmptyRemark = true;

        /**
         * 设置订单号
         *
         * @param orderNo 订单号
         * @return 构建器
         */
        public Builder orderNo(String orderNo) {
            this.orderNo = orderNo;
            return this;
        }

        /**
         * 设置用户编号
         *
         * @param userId 用户编号
         * @return 构建器
         */
        public Builder userId(String userId) {
            this.userId = userId;
            return this;
        }

        /**
         * 设置订单来源
         *
         * @param source 订单来源
         * @return 构建器
         */
        public Builder source(OrderSourceEnum source) {
            this.source = source;
            return this;
        }

        /**
         * 设置幂等请求号
         *
         * @param requestNo 幂等请求号
         * @return 构建器
         */
        public Builder requestNo(String requestNo) {
            this.requestNo = requestNo;
            return this;
        }

        /**
         * 设置优惠券编号
         *
         * @param couponId 优惠券编号
         * @return 构建器
         */
        public Builder couponId(String couponId) {
            this.couponId = couponId;
            return this;
        }

        /**
         * 设置订单备注
         *
         * @param remark 订单备注
         * @return 构建器
         */
        public Builder remark(String remark) {
            this.remark = remark;
            return this;
        }

        /**
         * 设置收货地址
         *
         * @param address 收货地址
         * @return 构建器
         */
        public Builder address(OrderAddressDTO address) {
            this.address = address;
            return this;
        }

        /**
         * 设置商品明细
         *
         * @param items 商品明细
         * @return 构建器
         */
        public Builder items(List<OrderItemDTO> items) {
            this.items = CollUtil.isEmpty(items) ? new ArrayList<>() : new ArrayList<>(items);
            return this;
        }

        /**
         * 设置订单超时时间
         *
         * @param timeoutMinutes 超时时间，单位分钟
         * @return 构建器
         */
        public Builder timeoutMinutes(Integer timeoutMinutes) {
            if (timeoutMinutes != null) {
                this.timeoutMinutes = timeoutMinutes;
            }
            return this;
        }

        /**
         * 设置是否允许空备注
         *
         * @param allowEmptyRemark 是否允许空备注
         * @return 构建器
         */
        public Builder allowEmptyRemark(Boolean allowEmptyRemark) {
            if (allowEmptyRemark != null) {
                this.allowEmptyRemark = allowEmptyRemark;
            }
            return this;
        }

        /**
         * 构建订单提交上下文
         *
         * @return 订单提交上下文
         */
        public OrderSubmitContext build() {
            fillDefaultValue();
            checkRequiredValue();
            calculateOrderAmount();

            return new OrderSubmitContext(this);
        }

        /**
         * 填充默认值
         */
        private void fillDefaultValue() {
            if (StrUtil.isBlank(orderNo)) {
                this.orderNo = StrUtil.format("ORDER_{}", IdUtil.fastSimpleUUID());
            }

            if (StrUtil.isBlank(requestNo)) {
                this.requestNo = StrUtil.format("REQ_{}", IdUtil.fastSimpleUUID());
            }

            if (source == null) {
                this.source = OrderSourceEnum.WEB;
            }

            if (remark == null) {
                this.remark = "";
            }
        }

        /**
         * 校验必填字段
         */
        private void checkRequiredValue() {
            if (StrUtil.isBlank(userId)) {
                throw new IllegalArgumentException("用户编号不能为空");
            }

            if (address == null) {
                throw new IllegalArgumentException("收货地址不能为空");
            }

            if (CollUtil.isEmpty(items)) {
                throw new IllegalArgumentException("商品明细不能为空");
            }

            if (!Boolean.TRUE.equals(allowEmptyRemark) && StrUtil.isBlank(remark)) {
                throw new IllegalArgumentException("订单备注不能为空");
            }
        }

        /**
         * 计算订单金额和商品数量
         */
        private void calculateOrderAmount() {
            BigDecimal amount = BigDecimal.ZERO;
            int quantity = 0;

            for (OrderItemDTO item : items) {
                BigDecimal itemAmount = NumberUtil.mul(item.getPrice(), item.getQuantity());
                amount = NumberUtil.add(amount, itemAmount);
                quantity += item.getQuantity();
            }

            this.totalAmount = amount;
            this.totalQuantity = quantity;
        }

    }

}
```

## 业务层调用

业务层通过构建者把接口请求 DTO 转换成订单提交上下文。这样订单号生成、来源默认值、幂等请求号生成、订单金额计算等逻辑不会散落在 Service 各处。

文件位置：`src/main/java/io/github/atengk/pattern/builder/service/OrderSubmitService.java`

```java
package io.github.atengk.pattern.builder.service;

import io.github.atengk.pattern.builder.dto.OrderSubmitDTO;
import io.github.atengk.pattern.builder.vo.OrderSubmitVO;

/**
 * 订单提交服务接口
 *
 * @author Ateng
 * @since 2026-05-13
 */
public interface OrderSubmitService {

    /**
     * 提交订单
     *
     * @param submitDTO 订单提交请求
     * @return 订单提交结果
     */
    OrderSubmitVO submit(OrderSubmitDTO submitDTO);

}
```

文件位置：`src/main/java/io/github/atengk/pattern/builder/service/OrderSubmitServiceImpl.java`

```java
package io.github.atengk.pattern.builder.service;

import cn.hutool.core.date.DateUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.json.JSONUtil;
import io.github.atengk.pattern.builder.config.BuilderDemoProperties;
import io.github.atengk.pattern.builder.domain.OrderSubmitContext;
import io.github.atengk.pattern.builder.dto.OrderSubmitDTO;
import io.github.atengk.pattern.builder.enums.OrderSourceEnum;
import io.github.atengk.pattern.builder.vo.OrderSubmitVO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

/**
 * 订单提交服务实现
 *
 * @author Ateng
 * @since 2026-05-13
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class OrderSubmitServiceImpl implements OrderSubmitService {

    private final BuilderDemoProperties properties;

    /**
     * 提交订单
     *
     * @param submitDTO 订单提交请求
     * @return 订单提交结果
     */
    @Override
    public OrderSubmitVO submit(OrderSubmitDTO submitDTO) {
        log.info("准备提交订单，请求参数：{}", JSONUtil.toJsonStr(submitDTO));

        OrderSourceEnum source = StrUtil.isBlank(submitDTO.getSource())
                ? OrderSourceEnum.parse(properties.getDefaultSource())
                : OrderSourceEnum.parse(submitDTO.getSource());

        OrderSubmitContext context = OrderSubmitContext.builder()
                .userId(submitDTO.getUserId())
                .source(source)
                .requestNo(submitDTO.getRequestNo())
                .couponId(submitDTO.getCouponId())
                .remark(submitDTO.getRemark())
                .address(submitDTO.getAddress())
                .items(submitDTO.getItems())
                .timeoutMinutes(properties.getDefaultTimeoutMinutes())
                .allowEmptyRemark(properties.getAllowEmptyRemark())
                .build();

        log.info("订单上下文构建完成，orderNo：{}，requestNo：{}，totalAmount：{}",
                context.getOrderNo(), context.getRequestNo(), context.getTotalAmount());

        return new OrderSubmitVO(
                context.getOrderNo(),
                context.getUserId(),
                context.getSource().getCode(),
                context.getRequestNo(),
                context.getTotalAmount(),
                context.getTotalQuantity(),
                true,
                "订单提交成功",
                DateUtil.now()
        );
    }

}
```

文件位置：`src/main/java/io/github/atengk/pattern/builder/controller/OrderSubmitController.java`

```java
package io.github.atengk.pattern.builder.controller;

import io.github.atengk.pattern.builder.dto.OrderSubmitDTO;
import io.github.atengk.pattern.builder.service.OrderSubmitService;
import io.github.atengk.pattern.builder.vo.OrderSubmitVO;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

/**
 * 订单提交接口
 *
 * @author Ateng
 * @since 2026-05-13
 */
@RestController
@RequestMapping("/api/patterns/builder/orders")
@RequiredArgsConstructor
public class OrderSubmitController {

    private final OrderSubmitService orderSubmitService;

    /**
     * 提交订单
     *
     * @param submitDTO 订单提交请求
     * @return 订单提交结果
     */
    @PostMapping("/submit")
    public OrderSubmitVO submit(@Valid @RequestBody OrderSubmitDTO submitDTO) {
        return orderSubmitService.submit(submitDTO);
    }

}
```

## 使用方式

启动项目后，通过订单提交接口传入复杂订单参数。Service 内部会使用构建者创建订单提交上下文，并返回计算后的订单金额、商品数量和订单号。

接口地址：

```text
POST /api/patterns/builder/orders/submit
```

完整请求示例：

```bash
curl -X POST "http://localhost:8080/api/patterns/builder/orders/submit" \
  -H "Content-Type: application/json" \
  -d '{
    "userId": "USER_001",
    "source": "APP",
    "requestNo": "REQ_202605130001",
    "couponId": "COUPON_10001",
    "remark": "工作日白天配送",
    "address": {
      "receiverName": "张三",
      "receiverPhone": "13800000000",
      "province": "广东省",
      "city": "深圳市",
      "detailAddress": "南山区科技园 1 号"
    },
    "items": [
      {
        "skuId": "SKU_10001",
        "skuName": "机械键盘",
        "quantity": 2,
        "price": 299.00
      },
      {
        "skuId": "SKU_10002",
        "skuName": "无线鼠标",
        "quantity": 1,
        "price": 129.00
      }
    ]
  }'
```

响应示例：

```json
{
  "orderNo": "ORDER_f2cf372f78494c34a12d7c56db3b9a33",
  "userId": "USER_001",
  "source": "APP",
  "requestNo": "REQ_202605130001",
  "totalAmount": 727.00,
  "totalQuantity": 3,
  "success": true,
  "message": "订单提交成功",
  "submitTime": "2026-05-13 14:20:30"
}
```

最小请求示例：

```bash
curl -X POST "http://localhost:8080/api/patterns/builder/orders/submit" \
  -H "Content-Type: application/json" \
  -d '{
    "userId": "USER_002",
    "address": {
      "receiverName": "李四",
      "receiverPhone": "13900000000",
      "province": "浙江省",
      "city": "杭州市",
      "detailAddress": "西湖区文三路 2 号"
    },
    "items": [
      {
        "skuId": "SKU_20001",
        "skuName": "显示器",
        "quantity": 1,
        "price": 899.00
      }
    ]
  }'
```

这个请求没有传 `source`、`requestNo`、`couponId`、`remark`。构建器会自动处理默认来源、生成幂等请求号、忽略空优惠券，并把备注处理为空字符串。

## Lombok Builder 写法

在 Spring Boot 项目中，如果对象只是字段较多，但没有复杂校验、默认值处理、金额计算等构建过程，可以使用 Lombok 的 `@Builder` 简化代码。

下面示例适合普通查询条件对象。它没有复杂构建逻辑，只是为了让创建过程更清晰。

文件位置：`src/main/java/io/github/atengk/pattern/builder/domain/OrderQueryCondition.java`

```java
package io.github.atengk.pattern.builder.domain;

import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;

/**
 * 订单查询条件
 *
 * @author Ateng
 * @since 2026-05-13
 */
@Getter
@Builder
public class OrderQueryCondition {

    /**
     * 用户编号
     */
    private final String userId;

    /**
     * 订单来源
     */
    private final String source;

    /**
     * 开始时间
     */
    private final LocalDateTime startTime;

    /**
     * 结束时间
     */
    private final LocalDateTime endTime;

    /**
     * 最小金额
     */
    private final Integer minAmount;

    /**
     * 最大金额
     */
    private final Integer maxAmount;

}
```

调用示例：

```java
OrderQueryCondition condition = OrderQueryCondition.builder()
        .userId("USER_001")
        .source("APP")
        .minAmount(100)
        .maxAmount(1000)
        .build();
```

Lombok `@Builder` 更适合简单对象构建。对于需要复杂校验、默认值填充、字段联动计算、不可变集合复制的对象，建议手写 Builder，逻辑更可控。

## 验证方式

可以通过单元测试验证构建器是否能正确生成默认值、计算金额，以及在必填字段缺失时抛出异常。

文件位置：`src/test/java/io/github/atengk/pattern/builder/OrderSubmitContextTest.java`

```java
package io.github.atengk.pattern.builder;

import io.github.atengk.pattern.builder.domain.OrderSubmitContext;
import io.github.atengk.pattern.builder.dto.OrderAddressDTO;
import io.github.atengk.pattern.builder.dto.OrderItemDTO;
import io.github.atengk.pattern.builder.enums.OrderSourceEnum;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.List;

/**
 * 订单提交上下文构建器测试
 *
 * @author Ateng
 * @since 2026-05-13
 */
@Slf4j
class OrderSubmitContextTest {

    /**
     * 验证构建器能够正确构建订单提交上下文
     */
    @Test
    void shouldBuildOrderSubmitContext() {
        OrderSubmitContext context = OrderSubmitContext.builder()
                .userId("USER_001")
                .source(OrderSourceEnum.APP)
                .address(buildAddress())
                .items(buildItems())
                .build();

        log.info("订单号：{}，请求号：{}，总金额：{}，总数量：{}",
                context.getOrderNo(), context.getRequestNo(), context.getTotalAmount(), context.getTotalQuantity());

        Assertions.assertNotNull(context.getOrderNo());
        Assertions.assertNotNull(context.getRequestNo());
        Assertions.assertEquals(OrderSourceEnum.APP, context.getSource());
        Assertions.assertEquals(new BigDecimal("727.00"), context.getTotalAmount());
        Assertions.assertEquals(3, context.getTotalQuantity());
    }

    /**
     * 验证缺少用户编号时构建失败
     */
    @Test
    void shouldThrowExceptionWhenUserIdBlank() {
        IllegalArgumentException exception = Assertions.assertThrows(
                IllegalArgumentException.class,
                () -> OrderSubmitContext.builder()
                        .address(buildAddress())
                        .items(buildItems())
                        .build()
        );

        log.info("构建失败异常信息：{}", exception.getMessage());
        Assertions.assertTrue(exception.getMessage().contains("用户编号不能为空"));
    }

    /**
     * 构建测试收货地址
     *
     * @return 收货地址
     */
    private OrderAddressDTO buildAddress() {
        OrderAddressDTO address = new OrderAddressDTO();
        address.setReceiverName("张三");
        address.setReceiverPhone("13800000000");
        address.setProvince("广东省");
        address.setCity("深圳市");
        address.setDetailAddress("南山区科技园 1 号");
        return address;
    }

    /**
     * 构建测试商品明细
     *
     * @return 商品明细
     */
    private List<OrderItemDTO> buildItems() {
        OrderItemDTO keyboard = new OrderItemDTO();
        keyboard.setSkuId("SKU_10001");
        keyboard.setSkuName("机械键盘");
        keyboard.setQuantity(2);
        keyboard.setPrice(new BigDecimal("299.00"));

        OrderItemDTO mouse = new OrderItemDTO();
        mouse.setSkuId("SKU_10002");
        mouse.setSkuName("无线鼠标");
        mouse.setQuantity(1);
        mouse.setPrice(new BigDecimal("129.00"));

        return List.of(keyboard, mouse);
    }

}
```

执行测试：

```bash
mvn test -Dtest=OrderSubmitContextTest
```

命令说明：`-Dtest=OrderSubmitContextTest` 表示只运行当前构建者模式测试类，用于快速验证对象构建、默认值处理和异常校验逻辑。

## 和工厂模式的区别

构建者模式、简单工厂模式、工厂方法模式、抽象工厂模式都属于创建型模式，但关注点不同。

```text
简单工厂模式：根据类型获取一个对象
工厂方法模式：由具体工厂创建一个对象
抽象工厂模式：由具体工厂创建一组相关对象
构建者模式：分步骤构建一个复杂对象
```

如果问题是“根据类型选择哪个实现类”，通常是工厂相关模式。如果问题是“一个对象字段很多，创建步骤复杂”，通常是构建者模式。

在订单提交场景中：

```text
选择支付宝还是微信支付：适合工厂方法或抽象工厂
构建订单提交上下文：适合构建者模式
根据订单类型选择处理器：适合简单工厂或策略模式
```

## 开发建议

在 Spring Boot 项目中使用构建者模式时，建议遵循以下原则：

```text
字段较多且部分字段可选时，可以考虑构建者模式
创建过程包含默认值、校验、计算、转换时，优先手写 Builder
简单对象可以使用 Lombok @Builder
构建后的对象尽量设计为不可变对象
集合字段在 build 时要复制，避免外部修改影响对象状态
不要把复杂业务流程塞进 Builder，Builder 只负责构建对象
Controller DTO 不一定需要 Builder，业务上下文对象更适合 Builder
```

需要注意的是，构建者模式不是为了减少代码量。手写 Builder 的代码量通常会增加，但它能换来更清晰的创建过程、更集中的校验逻辑和更稳定的不可变对象。

## 常见问题

构建者模式和 Lombok `@Builder` 不是一回事。Lombok `@Builder` 是一种生成构建器代码的工具，构建者模式是一种对象创建思想。对于简单对象，`@Builder` 足够；对于复杂对象，手写 Builder 更适合。

构建者模式不应该替代所有构造方法。对于简单值对象，例如只有 `id` 和 `name` 两个字段的对象，直接构造方法或 record 更清晰。

构建者模式也不应该承载过多业务逻辑。例如库存校验、优惠券核销、订单落库、发消息等操作不应该写进 `build()` 方法，这些应该放在 Service、领域服务或应用服务中。`build()` 只适合做对象构建相关的校验、默认值、字段计算和格式转换。

## 总结

构建者模式用于分步骤构建复杂对象。它适合字段较多、参数可选、默认值较多、创建过程需要统一校验或计算的对象。

本示例的核心流程可以概括为：

```text
Controller 接收订单提交 DTO
Service 解析订单来源和配置
通过 OrderSubmitContext.builder() 创建构建器
链式设置用户、地址、商品、优惠券、备注等信息
build() 内部处理默认值、校验和金额计算
返回不可变订单提交上下文
Service 使用上下文完成后续业务处理
```

在真实 Spring Boot 项目中，构建者模式常用于订单上下文、支付请求、导出任务、消息发送请求、复杂查询条件、第三方 API 请求对象等场景。它的核心价值是让复杂对象创建过程集中、清晰、可校验，并减少业务代码中的参数混乱。
