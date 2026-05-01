# 设计模式：工厂模式

工厂模式用于封装对象创建逻辑，让调用方不直接依赖具体实现类。在 JDK21 和 Spring Boot 3 项目中，工厂模式常用于支付渠道、消息发送渠道、文件解析器、导入导出处理器、策略分发、第三方客户端构建等场景。

需要注意：在 Spring Boot 项目中，工厂模式通常不需要手动 `new` 对象，而是结合 Spring 容器、接口、多实现、`List<T>` 注入或 `Map<String, T>` 注入来完成对象分发。

## 基础配置

本示例基于 JDK21、Spring Boot 3、Maven 项目。示例包路径统一使用 `io.github.atengk`。

文件位置：`pom.xml`

```xml
<dependencies>
    <!-- Spring Boot Web，用于提供接口验证工厂模式行为 -->
    <dependency>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-starter-web</artifactId>
    </dependency>

    <!-- Hutool 工具类，用于字符串、集合、ID 等通用处理 -->
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

工厂模式的核心目标是把对象创建逻辑从业务流程中抽离出来，调用方只关心接口，不关心具体实现类。

常见实现方式如下：

| 实现方式          | 是否推荐         | 适用场景                       |
| ----------------- | ---------------- | ------------------------------ |
| 简单工厂          | 推荐用于简单场景 | 根据类型创建少量对象           |
| 工厂方法          | 推荐用于扩展场景 | 每个产品有独立创建逻辑         |
| 抽象工厂          | 适合复杂场景     | 创建一组有关联的产品族         |
| Spring 工厂分发   | 强烈推荐         | Spring Boot 项目中的多实现分发 |
| 直接 `new` 实现类 | 不推荐           | 调用方和具体类强耦合           |

在 Spring Boot 项目中，常见优先级通常是：

```text
Spring 工厂分发 > 工厂方法 > 简单工厂 > 抽象工厂
```

抽象工厂不是不好，而是业务系统中使用频率相对较低。大多数后端项目的渠道分发、类型分发、策略分发，使用 Spring 工厂分发会更直接。

## 简单工厂

简单工厂通过一个工厂类，根据参数返回不同的对象。它适合产品类型少、创建逻辑简单、变化不频繁的场景。

### 文件结构

```text
src/main/java/io/github/atengk/design/simplefactory/
├── Calculator.java
├── OperationType.java
├── Operation.java
├── AddOperation.java
├── MultiplyOperation.java
└── OperationFactory.java
```

文件位置：`src/main/java/io/github/atengk/design/simplefactory/OperationType.java`

下面的枚举用于定义支持的计算类型。

```java
package io.github.atengk.design.simplefactory;

/**
 * 计算类型
 *
 * @author Ateng
 * @since 2026-04-30
 */
public enum OperationType {

    /**
     * 加法
     */
    ADD,

    /**
     * 乘法
     */
    MULTIPLY
}
```

文件位置：`src/main/java/io/github/atengk/design/simplefactory/Operation.java`

下面的接口定义统一的计算行为。

```java
package io.github.atengk.design.simplefactory;

import java.math.BigDecimal;

/**
 * 计算操作
 *
 * @author Ateng
 * @since 2026-04-30
 */
public interface Operation {

    /**
     * 执行计算
     *
     * @param left  左操作数
     * @param right 右操作数
     * @return 计算结果
     */
    BigDecimal calculate(BigDecimal left, BigDecimal right);
}
```

文件位置：`src/main/java/io/github/atengk/design/simplefactory/AddOperation.java`

下面是加法操作实现类。

```java
package io.github.atengk.design.simplefactory;

import lombok.extern.slf4j.Slf4j;

import java.math.BigDecimal;

/**
 * 加法操作
 *
 * @author Ateng
 * @since 2026-04-30
 */
@Slf4j
public class AddOperation implements Operation {

    /**
     * 执行计算
     *
     * @param left  左操作数
     * @param right 右操作数
     * @return 计算结果
     */
    @Override
    public BigDecimal calculate(BigDecimal left, BigDecimal right) {
        BigDecimal result = left.add(right);
        log.info("执行加法计算，left={}，right={}，result={}", left, right, result);
        return result;
    }
}
```

文件位置：`src/main/java/io/github/atengk/design/simplefactory/MultiplyOperation.java`

下面是乘法操作实现类。

```java
package io.github.atengk.design.simplefactory;

import lombok.extern.slf4j.Slf4j;

import java.math.BigDecimal;

/**
 * 乘法操作
 *
 * @author Ateng
 * @since 2026-04-30
 */
@Slf4j
public class MultiplyOperation implements Operation {

    /**
     * 执行计算
     *
     * @param left  左操作数
     * @param right 右操作数
     * @return 计算结果
     */
    @Override
    public BigDecimal calculate(BigDecimal left, BigDecimal right) {
        BigDecimal result = left.multiply(right);
        log.info("执行乘法计算，left={}，right={}，result={}", left, right, result);
        return result;
    }
}
```

文件位置：`src/main/java/io/github/atengk/design/simplefactory/OperationFactory.java`

下面是简单工厂类，根据计算类型创建不同的操作对象。

```java
package io.github.atengk.design.simplefactory;

import lombok.extern.slf4j.Slf4j;

/**
 * 计算操作工厂
 *
 * @author Ateng
 * @since 2026-04-30
 */
@Slf4j
public class OperationFactory {

    private OperationFactory() {
    }

    /**
     * 创建计算操作
     *
     * @param operationType 计算类型
     * @return 计算操作
     */
    public static Operation create(OperationType operationType) {
        if (operationType == null) {
            log.warn("创建计算操作失败，计算类型为空");
            throw new IllegalArgumentException("计算类型不能为空");
        }

        log.info("创建计算操作，计算类型：{}", operationType);

        return switch (operationType) {
            case ADD -> new AddOperation();
            case MULTIPLY -> new MultiplyOperation();
        };
    }
}
```

文件位置：`src/main/java/io/github/atengk/design/simplefactory/Calculator.java`

下面是调用方代码，调用方只依赖 `OperationFactory` 和 `Operation` 接口。

```java
package io.github.atengk.design.simplefactory;

import java.math.BigDecimal;

/**
 * 计算器
 *
 * @author Ateng
 * @since 2026-04-30
 */
public class Calculator {

    /**
     * 执行计算
     *
     * @param operationType 计算类型
     * @param left          左操作数
     * @param right         右操作数
     * @return 计算结果
     */
    public BigDecimal calculate(OperationType operationType, BigDecimal left, BigDecimal right) {
        Operation operation = OperationFactory.create(operationType);
        return operation.calculate(left, right);
    }
}
```

使用方式：

```java
Calculator calculator = new Calculator();

BigDecimal addResult = calculator.calculate(OperationType.ADD, BigDecimal.valueOf(10), BigDecimal.valueOf(5));
BigDecimal multiplyResult = calculator.calculate(OperationType.MULTIPLY, BigDecimal.valueOf(10), BigDecimal.valueOf(5));
```

简单工厂的优点是结构简单，调用方便。缺点是每增加一个产品类型，就需要修改工厂类，违反开闭原则。

## 工厂方法

工厂方法把对象创建逻辑下放到不同的工厂实现类中。相比简单工厂，它更适合扩展，因为新增产品时可以新增一个工厂类，而不是集中修改一个大工厂。

### 文件结构

```text
src/main/java/io/github/atengk/design/factorymethod/
├── FileParser.java
├── JsonFileParser.java
├── CsvFileParser.java
├── FileParserFactory.java
├── JsonFileParserFactory.java
└── CsvFileParserFactory.java
```

文件位置：`src/main/java/io/github/atengk/design/factorymethod/FileParser.java`

下面的接口定义文件解析器的统一行为。

```java
package io.github.atengk.design.factorymethod;

import java.util.List;
import java.util.Map;

/**
 * 文件解析器
 *
 * @author Ateng
 * @since 2026-04-30
 */
public interface FileParser {

    /**
     * 解析文件内容
     *
     * @param content 文件内容
     * @return 解析结果
     */
    List<Map<String, Object>> parse(String content);
}
```

文件位置：`src/main/java/io/github/atengk/design/factorymethod/JsonFileParser.java`

下面是 JSON 文件解析器示例。

```java
package io.github.atengk.design.factorymethod;

import cn.hutool.core.collection.ListUtil;
import cn.hutool.core.map.MapUtil;
import cn.hutool.core.util.StrUtil;
import lombok.extern.slf4j.Slf4j;

import java.util.List;
import java.util.Map;

/**
 * JSON文件解析器
 *
 * @author Ateng
 * @since 2026-04-30
 */
@Slf4j
public class JsonFileParser implements FileParser {

    /**
     * 解析文件内容
     *
     * @param content 文件内容
     * @return 解析结果
     */
    @Override
    public List<Map<String, Object>> parse(String content) {
        if (StrUtil.isBlank(content)) {
            log.warn("解析JSON文件失败，文件内容为空");
            return ListUtil.empty();
        }

        log.info("解析JSON文件内容，内容长度：{}", content.length());
        return ListUtil.of(MapUtil.<String, Object>builder()
                .put("type", "json")
                .put("content", content)
                .build());
    }
}
```

文件位置：`src/main/java/io/github/atengk/design/factorymethod/CsvFileParser.java`

下面是 CSV 文件解析器示例。

```java
package io.github.atengk.design.factorymethod;

import cn.hutool.core.collection.ListUtil;
import cn.hutool.core.map.MapUtil;
import cn.hutool.core.text.csv.CsvData;
import cn.hutool.core.text.csv.CsvUtil;
import cn.hutool.core.text.csv.CsvReader;
import cn.hutool.core.util.StrUtil;
import lombok.extern.slf4j.Slf4j;

import java.io.StringReader;
import java.util.List;
import java.util.Map;

/**
 * CSV文件解析器
 *
 * @author Ateng
 * @since 2026-04-30
 */
@Slf4j
public class CsvFileParser implements FileParser {

    /**
     * 解析文件内容
     *
     * @param content 文件内容
     * @return 解析结果
     */
    @Override
    public List<Map<String, Object>> parse(String content) {
        if (StrUtil.isBlank(content)) {
            log.warn("解析CSV文件失败，文件内容为空");
            return ListUtil.empty();
        }

        CsvReader reader = CsvUtil.getReader();
        CsvData csvData = reader.read(new StringReader(content));

        log.info("解析CSV文件内容，行数：{}", csvData.getRowCount());
        return ListUtil.of(MapUtil.<String, Object>builder()
                .put("type", "csv")
                .put("rowCount", csvData.getRowCount())
                .build());
    }
}
```

文件位置：`src/main/java/io/github/atengk/design/factorymethod/FileParserFactory.java`

下面是文件解析器工厂接口。

```java
package io.github.atengk.design.factorymethod;

/**
 * 文件解析器工厂
 *
 * @author Ateng
 * @since 2026-04-30
 */
public interface FileParserFactory {

    /**
     * 创建文件解析器
     *
     * @return 文件解析器
     */
    FileParser createParser();
}
```

文件位置：`src/main/java/io/github/atengk/design/factorymethod/JsonFileParserFactory.java`

下面是 JSON 文件解析器工厂。

```java
package io.github.atengk.design.factorymethod;

import lombok.extern.slf4j.Slf4j;

/**
 * JSON文件解析器工厂
 *
 * @author Ateng
 * @since 2026-04-30
 */
@Slf4j
public class JsonFileParserFactory implements FileParserFactory {

    /**
     * 创建文件解析器
     *
     * @return 文件解析器
     */
    @Override
    public FileParser createParser() {
        log.info("创建JSON文件解析器");
        return new JsonFileParser();
    }
}
```

文件位置：`src/main/java/io/github/atengk/design/factorymethod/CsvFileParserFactory.java`

下面是 CSV 文件解析器工厂。

```java
package io.github.atengk.design.factorymethod;

import lombok.extern.slf4j.Slf4j;

/**
 * CSV文件解析器工厂
 *
 * @author Ateng
 * @since 2026-04-30
 */
@Slf4j
public class CsvFileParserFactory implements FileParserFactory {

    /**
     * 创建文件解析器
     *
     * @return 文件解析器
     */
    @Override
    public FileParser createParser() {
        log.info("创建CSV文件解析器");
        return new CsvFileParser();
    }
}
```

使用方式：

```java
FileParserFactory factory = new CsvFileParserFactory();
FileParser parser = factory.createParser();
List<Map<String, Object>> result = parser.parse("id,name\n1,Ateng");
```

工厂方法适合产品创建逻辑较复杂的场景。缺点是类数量会增加，如果只是简单类型分发，使用 Spring 工厂分发会更轻量。

## Spring Boot 工厂分发

Spring Boot 项目中最常用的工厂模式，是将多个实现类注册为 Spring Bean，然后通过工厂类统一分发。

这种方式适合支付渠道、登录方式、消息渠道、订单处理器、导入解析器等业务场景。

### 文件结构

```text
src/main/java/io/github/atengk/design/
├── FactoryApplication.java
├── controller/
│   └── PaymentController.java
├── dto/
│   └── PaymentRequest.java
├── factory/
│   └── PaymentHandlerFactory.java
└── handler/
    ├── PaymentHandler.java
    ├── AlipayPaymentHandler.java
    └── WechatPaymentHandler.java
```

文件位置：`src/main/java/io/github/atengk/design/FactoryApplication.java`

下面是 Spring Boot 启动类。

```java
package io.github.atengk.design;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * 工厂模式示例启动类
 *
 * @author Ateng
 * @since 2026-04-30
 */
@SpringBootApplication
public class FactoryApplication {

    /**
     * 应用启动入口
     *
     * @param args 启动参数
     */
    public static void main(String[] args) {
        SpringApplication.run(FactoryApplication.class, args);
    }
}
```

文件位置：`src/main/java/io/github/atengk/design/dto/PaymentRequest.java`

下面是支付请求参数对象，使用 JDK21 的 `record` 简化不可变 DTO 定义。

```java
package io.github.atengk.design.dto;

import java.math.BigDecimal;

/**
 * 支付请求参数
 *
 * @param payType 支付类型
 * @param orderNo 订单号
 * @param amount  支付金额
 * @author Ateng
 * @since 2026-04-30
 */
public record PaymentRequest(String payType, String orderNo, BigDecimal amount) {
}
```

文件位置：`src/main/java/io/github/atengk/design/handler/PaymentHandler.java`

下面是支付处理器接口，不同支付渠道实现该接口。

```java
package io.github.atengk.design.handler;

import io.github.atengk.design.dto.PaymentRequest;

/**
 * 支付处理器
 *
 * @author Ateng
 * @since 2026-04-30
 */
public interface PaymentHandler {

    /**
     * 获取支持的支付类型
     *
     * @return 支付类型
     */
    String supportType();

    /**
     * 执行支付
     *
     * @param request 支付请求参数
     * @return 支付结果
     */
    String pay(PaymentRequest request);
}
```

文件位置：`src/main/java/io/github/atengk/design/handler/AlipayPaymentHandler.java`

下面是支付宝支付处理器。

```java
package io.github.atengk.design.handler;

import cn.hutool.core.util.IdUtil;
import io.github.atengk.design.dto.PaymentRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * 支付宝支付处理器
 *
 * @author Ateng
 * @since 2026-04-30
 */
@Slf4j
@Component
public class AlipayPaymentHandler implements PaymentHandler {

    /**
     * 获取支持的支付类型
     *
     * @return 支付类型
     */
    @Override
    public String supportType() {
        return "alipay";
    }

    /**
     * 执行支付
     *
     * @param request 支付请求参数
     * @return 支付结果
     */
    @Override
    public String pay(PaymentRequest request) {
        String tradeNo = IdUtil.fastSimpleUUID();
        log.info("执行支付宝支付，订单号：{}，金额：{}，交易号：{}", request.orderNo(), request.amount(), tradeNo);
        return "支付宝支付成功，tradeNo=" + tradeNo;
    }
}
```

文件位置：`src/main/java/io/github/atengk/design/handler/WechatPaymentHandler.java`

下面是微信支付处理器。

```java
package io.github.atengk.design.handler;

import cn.hutool.core.util.IdUtil;
import io.github.atengk.design.dto.PaymentRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * 微信支付处理器
 *
 * @author Ateng
 * @since 2026-04-30
 */
@Slf4j
@Component
public class WechatPaymentHandler implements PaymentHandler {

    /**
     * 获取支持的支付类型
     *
     * @return 支付类型
     */
    @Override
    public String supportType() {
        return "wechat";
    }

    /**
     * 执行支付
     *
     * @param request 支付请求参数
     * @return 支付结果
     */
    @Override
    public String pay(PaymentRequest request) {
        String tradeNo = IdUtil.fastSimpleUUID();
        log.info("执行微信支付，订单号：{}，金额：{}，交易号：{}", request.orderNo(), request.amount(), tradeNo);
        return "微信支付成功，tradeNo=" + tradeNo;
    }
}
```

文件位置：`src/main/java/io/github/atengk/design/factory/PaymentHandlerFactory.java`

下面是 Spring 工厂分发类。它在构造方法中接收所有 `PaymentHandler` 实现，并按支付类型缓存到 Map 中。

```java
package io.github.atengk.design.factory;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.StrUtil;
import io.github.atengk.design.handler.PaymentHandler;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * 支付处理器工厂
 *
 * @author Ateng
 * @since 2026-04-30
 */
@Slf4j
@Component
public class PaymentHandlerFactory {

    private final Map<String, PaymentHandler> handlerMap;

    /**
     * 创建支付处理器工厂
     *
     * @param paymentHandlers 支付处理器列表
     */
    public PaymentHandlerFactory(List<PaymentHandler> paymentHandlers) {
        if (CollUtil.isEmpty(paymentHandlers)) {
            log.warn("支付处理器列表为空");
            this.handlerMap = Map.of();
            return;
        }

        this.handlerMap = paymentHandlers.stream()
                .collect(Collectors.toUnmodifiableMap(
                        handler -> StrUtil.lowerFirst(handler.supportType()),
                        Function.identity()
                ));

        log.info("初始化支付处理器工厂，支持支付类型：{}", handlerMap.keySet());
    }

    /**
     * 获取支付处理器
     *
     * @param payType 支付类型
     * @return 支付处理器
     */
    public PaymentHandler getHandler(String payType) {
        if (StrUtil.isBlank(payType)) {
            log.warn("获取支付处理器失败，支付类型为空");
            throw new IllegalArgumentException("支付类型不能为空");
        }

        String normalizedPayType = StrUtil.trim(payType).toLowerCase();
        PaymentHandler handler = handlerMap.get(normalizedPayType);

        if (handler == null) {
            log.warn("获取支付处理器失败，不支持的支付类型：{}", payType);
            throw new IllegalArgumentException("不支持的支付类型：" + payType);
        }

        log.debug("获取支付处理器成功，支付类型：{}", normalizedPayType);
        return handler;
    }
}
```

文件位置：`src/main/java/io/github/atengk/design/controller/PaymentController.java`

下面是支付接口，用于验证工厂分发效果。

```java
package io.github.atengk.design.controller;

import io.github.atengk.design.dto.PaymentRequest;
import io.github.atengk.design.factory.PaymentHandlerFactory;
import io.github.atengk.design.handler.PaymentHandler;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;

/**
 * 支付控制器
 *
 * @author Ateng
 * @since 2026-04-30
 */
@RestController
@RequiredArgsConstructor
@RequestMapping("/factory/payment")
public class PaymentController {

    private final PaymentHandlerFactory paymentHandlerFactory;

    /**
     * 执行支付
     *
     * @param payType 支付类型
     * @param orderNo 订单号
     * @param amount  支付金额
     * @return 支付结果
     */
    @PostMapping("/pay")
    public String pay(@RequestParam String payType,
                      @RequestParam String orderNo,
                      @RequestParam BigDecimal amount) {
        PaymentRequest request = new PaymentRequest(payType, orderNo, amount);
        PaymentHandler handler = paymentHandlerFactory.getHandler(payType);
        return handler.pay(request);
    }
}
```

接口调用示例：

```bash
curl -X POST "http://localhost:8080/factory/payment/pay?payType=alipay&orderNo=ORDER10001&amount=99.90"

curl -X POST "http://localhost:8080/factory/payment/pay?payType=wechat&orderNo=ORDER10002&amount=66.60"
```

可能返回：

```text
支付宝支付成功，tradeNo=7ad9a53f967645218dcba5e63e7d971b
微信支付成功，tradeNo=42bb8ac0c53e45e7997a8c6b8d30a620
```

这种方式的优点是扩展成本低。后续新增银联支付，只需要新增一个 `UnionPayPaymentHandler` 实现类，不需要修改控制器，也不需要修改原有支付处理器。

## 扩展一个新产品

在 Spring 工厂分发模式中，新增产品通常只需要新增一个实现类。下面以新增银行卡支付为例。

文件位置：`src/main/java/io/github/atengk/design/handler/BankCardPaymentHandler.java`

下面的实现类会被 Spring 自动扫描，并自动加入 `PaymentHandlerFactory` 的处理器列表。

```java
package io.github.atengk.design.handler;

import cn.hutool.core.util.IdUtil;
import io.github.atengk.design.dto.PaymentRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * 银行卡支付处理器
 *
 * @author Ateng
 * @since 2026-04-30
 */
@Slf4j
@Component
public class BankCardPaymentHandler implements PaymentHandler {

    /**
     * 获取支持的支付类型
     *
     * @return 支付类型
     */
    @Override
    public String supportType() {
        return "bankCard";
    }

    /**
     * 执行支付
     *
     * @param request 支付请求参数
     * @return 支付结果
     */
    @Override
    public String pay(PaymentRequest request) {
        String tradeNo = IdUtil.fastSimpleUUID();
        log.info("执行银行卡支付，订单号：{}，金额：{}，交易号：{}", request.orderNo(), request.amount(), tradeNo);
        return "银行卡支付成功，tradeNo=" + tradeNo;
    }
}
```

调用示例：

```bash
curl -X POST "http://localhost:8080/factory/payment/pay?payType=bankCard&orderNo=ORDER10003&amount=188.00"
```

这里需要注意一个细节：`PaymentHandlerFactory` 中使用了 `toLowerCase()` 统一转换支付类型，所以请求参数可以传：

```text
bankcard
BANKCARD
BankCard
```

如果业务要求严格区分大小写，可以去掉统一小写逻辑。

## 抽象工厂

抽象工厂用于创建一组有关联的对象，通常称为产品族。例如同一个消息渠道下，既需要创建发送器，也需要创建模板渲染器。

它比工厂方法更复杂，适合产品之间存在强关联的场景。

### 文件结构

```text
src/main/java/io/github/atengk/design/abstractfactory/
├── MessageSender.java
├── TemplateRenderer.java
├── MessageFactory.java
├── EmailMessageFactory.java
├── SmsMessageFactory.java
├── EmailMessageSender.java
├── SmsMessageSender.java
├── EmailTemplateRenderer.java
└── SmsTemplateRenderer.java
```

文件位置：`src/main/java/io/github/atengk/design/abstractfactory/MessageSender.java`

下面是消息发送器接口。

```java
package io.github.atengk.design.abstractfactory;

/**
 * 消息发送器
 *
 * @author Ateng
 * @since 2026-04-30
 */
public interface MessageSender {

    /**
     * 发送消息
     *
     * @param receiver 接收人
     * @param content  消息内容
     * @return 发送结果
     */
    String send(String receiver, String content);
}
```

文件位置：`src/main/java/io/github/atengk/design/abstractfactory/TemplateRenderer.java`

下面是模板渲染器接口。

```java
package io.github.atengk.design.abstractfactory;

import java.util.Map;

/**
 * 模板渲染器
 *
 * @author Ateng
 * @since 2026-04-30
 */
public interface TemplateRenderer {

    /**
     * 渲染模板
     *
     * @param template 模板内容
     * @param params   模板参数
     * @return 渲染结果
     */
    String render(String template, Map<String, Object> params);
}
```

文件位置：`src/main/java/io/github/atengk/design/abstractfactory/MessageFactory.java`

下面是抽象工厂接口，用于创建同一消息渠道下的一组产品。

```java
package io.github.atengk.design.abstractfactory;

/**
 * 消息工厂
 *
 * @author Ateng
 * @since 2026-04-30
 */
public interface MessageFactory {

    /**
     * 创建消息发送器
     *
     * @return 消息发送器
     */
    MessageSender createSender();

    /**
     * 创建模板渲染器
     *
     * @return 模板渲染器
     */
    TemplateRenderer createRenderer();
}
```

文件位置：`src/main/java/io/github/atengk/design/abstractfactory/EmailMessageSender.java`

下面是邮件消息发送器。

```java
package io.github.atengk.design.abstractfactory;

import lombok.extern.slf4j.Slf4j;

/**
 * 邮件消息发送器
 *
 * @author Ateng
 * @since 2026-04-30
 */
@Slf4j
public class EmailMessageSender implements MessageSender {

    /**
     * 发送消息
     *
     * @param receiver 接收人
     * @param content  消息内容
     * @return 发送结果
     */
    @Override
    public String send(String receiver, String content) {
        log.info("发送邮件消息，接收人：{}，内容：{}", receiver, content);
        return "邮件发送成功";
    }
}
```

文件位置：`src/main/java/io/github/atengk/design/abstractfactory/SmsMessageSender.java`

下面是短信消息发送器。

```java
package io.github.atengk.design.abstractfactory;

import lombok.extern.slf4j.Slf4j;

/**
 * 短信消息发送器
 *
 * @author Ateng
 * @since 2026-04-30
 */
@Slf4j
public class SmsMessageSender implements MessageSender {

    /**
     * 发送消息
     *
     * @param receiver 接收人
     * @param content  消息内容
     * @return 发送结果
     */
    @Override
    public String send(String receiver, String content) {
        log.info("发送短信消息，接收人：{}，内容：{}", receiver, content);
        return "短信发送成功";
    }
}
```

文件位置：`src/main/java/io/github/atengk/design/abstractfactory/EmailTemplateRenderer.java`

下面是邮件模板渲染器。

```java
package io.github.atengk.design.abstractfactory;

import cn.hutool.core.util.StrUtil;

import java.util.Map;

/**
 * 邮件模板渲染器
 *
 * @author Ateng
 * @since 2026-04-30
 */
public class EmailTemplateRenderer implements TemplateRenderer {

    /**
     * 渲染模板
     *
     * @param template 模板内容
     * @param params   模板参数
     * @return 渲染结果
     */
    @Override
    public String render(String template, Map<String, Object> params) {
        String username = String.valueOf(params.getOrDefault("username", "用户"));
        return StrUtil.format("[邮件通知] " + template, username);
    }
}
```

文件位置：`src/main/java/io/github/atengk/design/abstractfactory/SmsTemplateRenderer.java`

下面是短信模板渲染器。

```java
package io.github.atengk.design.abstractfactory;

import cn.hutool.core.util.StrUtil;

import java.util.Map;

/**
 * 短信模板渲染器
 *
 * @author Ateng
 * @since 2026-04-30
 */
public class SmsTemplateRenderer implements TemplateRenderer {

    /**
     * 渲染模板
     *
     * @param template 模板内容
     * @param params   模板参数
     * @return 渲染结果
     */
    @Override
    public String render(String template, Map<String, Object> params) {
        String username = String.valueOf(params.getOrDefault("username", "用户"));
        return StrUtil.format("[短信通知] " + template, username);
    }
}
```

文件位置：`src/main/java/io/github/atengk/design/abstractfactory/EmailMessageFactory.java`

下面是邮件消息工厂。

```java
package io.github.atengk.design.abstractfactory;

/**
 * 邮件消息工厂
 *
 * @author Ateng
 * @since 2026-04-30
 */
public class EmailMessageFactory implements MessageFactory {

    /**
     * 创建消息发送器
     *
     * @return 消息发送器
     */
    @Override
    public MessageSender createSender() {
        return new EmailMessageSender();
    }

    /**
     * 创建模板渲染器
     *
     * @return 模板渲染器
     */
    @Override
    public TemplateRenderer createRenderer() {
        return new EmailTemplateRenderer();
    }
}
```

文件位置：`src/main/java/io/github/atengk/design/abstractfactory/SmsMessageFactory.java`

下面是短信消息工厂。

```java
package io.github.atengk.design.abstractfactory;

/**
 * 短信消息工厂
 *
 * @author Ateng
 * @since 2026-04-30
 */
public class SmsMessageFactory implements MessageFactory {

    /**
     * 创建消息发送器
     *
     * @return 消息发送器
     */
    @Override
    public MessageSender createSender() {
        return new SmsMessageSender();
    }

    /**
     * 创建模板渲染器
     *
     * @return 模板渲染器
     */
    @Override
    public TemplateRenderer createRenderer() {
        return new SmsTemplateRenderer();
    }
}
```

使用方式：

```java
MessageFactory factory = new EmailMessageFactory();

TemplateRenderer renderer = factory.createRenderer();
MessageSender sender = factory.createSender();

String content = renderer.render("你好，{}，你的订单已发货", Map.of("username", "Ateng"));
String result = sender.send("ateng@example.com", content);
```

抽象工厂的优点是可以保证同一产品族的对象配套使用。缺点是结构较重，新增产品等级时需要修改多个工厂接口和实现类。

## 验证方式

启动 Spring Boot 项目：

```bash
mvn spring-boot:run
```

执行支付宝支付：

```bash
curl -X POST "http://localhost:8080/factory/payment/pay?payType=alipay&orderNo=ORDER10001&amount=99.90"
```

执行微信支付：

```bash
curl -X POST "http://localhost:8080/factory/payment/pay?payType=wechat&orderNo=ORDER10002&amount=66.60"
```

执行银行卡支付：

```bash
curl -X POST "http://localhost:8080/factory/payment/pay?payType=bankCard&orderNo=ORDER10003&amount=188.00"
```

如果工厂分发正常，可以看到类似日志：

```text
初始化支付处理器工厂，支持支付类型：[alipay, wechat, bankcard]
执行支付宝支付，订单号：ORDER10001，金额：99.90，交易号：7ad9a53f967645218dcba5e63e7d971b
执行微信支付，订单号：ORDER10002，金额：66.60，交易号：42bb8ac0c53e45e7997a8c6b8d30a620
执行银行卡支付，订单号：ORDER10003，金额：188.00，交易号：11bc3ac4e9f94b00bd78f7771d26426c
```

如果传入不支持的支付类型：

```bash
curl -X POST "http://localhost:8080/factory/payment/pay?payType=paypal&orderNo=ORDER10004&amount=20.00"
```

会抛出异常：

```text
不支持的支付类型：paypal
```

实际项目中建议结合全局异常处理器，将该异常转换成统一响应结构。

## 注意事项

工厂模式适合解决对象创建和实现分发问题，不适合替代所有 `if else`。如果分支逻辑非常简单，强行拆成多个工厂和实现类，反而会增加维护成本。

在 Spring Boot 项目中，推荐将具体实现类交给 Spring 管理，而不是在工厂中频繁使用 `new`。这样可以保留依赖注入、AOP、事务、配置绑定、生命周期管理等能力。

不推荐在业务代码中这样写：

```java
if ("alipay".equals(payType)) {
    return new AlipayPaymentHandler().pay(request);
}
if ("wechat".equals(payType)) {
    return new WechatPaymentHandler().pay(request);
}
```

推荐使用工厂分发：

```java
PaymentHandler handler = paymentHandlerFactory.getHandler(payType);
return handler.pay(request);
```

如果工厂管理的是有状态对象，需要特别注意线程安全。Spring Bean 默认是单例，多线程请求会共享同一个对象实例，不要在处理器成员变量中保存请求级数据。

错误示例：

```java
private String currentOrderNo;
private BigDecimal currentAmount;
```

推荐将请求数据放在方法参数、局部变量、DTO 或上下文对象中。

```java
public String pay(PaymentRequest request) {
    String tradeNo = IdUtil.fastSimpleUUID();
    return "支付成功，tradeNo=" + tradeNo;
}
```

## 总结

在 JDK21 和 Spring Boot 3 项目中，工厂模式的实践重点是降低调用方和具体实现类之间的耦合。

简单工厂适合少量类型分发，工厂方法适合创建逻辑独立扩展，抽象工厂适合创建一组相关产品。对于大多数 Spring Boot 后端项目，更推荐使用 Spring 工厂分发：让所有实现类注册为 Bean，再通过统一工厂按业务类型选择对应实现。

这种方式扩展成本低，结构清晰，也能充分利用 Spring 的依赖注入、生命周期管理、配置管理和 AOP 能力。
