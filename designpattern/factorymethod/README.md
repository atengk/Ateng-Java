# 设计模式：工厂方法模式

工厂方法模式用于把对象创建逻辑延迟到具体工厂类中，由不同工厂负责创建不同产品对象。在 JDK21 和 Spring Boot 3 项目中，工厂方法模式常用于支付处理器创建、文件解析器创建、消息发送器创建、导入处理器创建、导出处理器创建、通知渠道处理器创建等场景。

需要注意：工厂方法模式关注的是“创建单个产品对象”。如果需要创建同一产品族下的一组对象，更适合抽象工厂模式；如果只是字段很多的复杂对象组装，更适合构建者模式；如果只是根据类型选择一个已有 Bean 执行，也可以使用策略模式或 Spring Bean Map。

## 基础配置

本示例基于 JDK21、Spring Boot 3、Maven 项目。示例包路径统一使用 `io.github.atengk`。

文件位置：`pom.xml`

```xml
<dependencies>
    <!-- Spring Boot Web，用于提供接口验证工厂方法模式行为 -->
    <dependency>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-starter-web</artifactId>
    </dependency>

    <!-- Hutool 工具类，用于字符串、ID、集合、金额等通用处理 -->
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

工厂方法模式的核心目标是让调用方依赖抽象产品和抽象工厂，而不是直接 `new` 具体产品对象。每个具体工厂负责创建一种具体产品。

常见角色如下：

| 角色            | 说明                               |
| --------------- | ---------------------------------- |
| Product         | 抽象产品，定义产品统一行为         |
| ConcreteProduct | 具体产品，实现具体业务能力         |
| Factory         | 抽象工厂，定义创建产品的方法       |
| ConcreteFactory | 具体工厂，负责创建某一种具体产品   |
| Client          | 调用方，通过工厂创建产品并使用产品 |

典型结构如下：

```text
PaymentHandlerFactory
└── createHandler()

AlipayHandlerFactory
└── AlipayPaymentHandler

WechatHandlerFactory
└── WechatPaymentHandler
```

工厂方法模式和简单工厂的区别在于：简单工厂通常把所有创建分支写在一个工厂类中；工厂方法模式把不同产品的创建逻辑拆到不同具体工厂中。

在 Spring Boot 项目中，常见优先级通常是：

```text
Spring Bean 工厂方法 > 普通 Java 工厂方法 > 简单工厂 if else
```

如果产品数量少、创建逻辑简单，简单工厂也可以接受。如果产品创建逻辑复杂、产品类型经常扩展，工厂方法模式更适合。

## 普通 Java 工厂方法

普通 Java 工厂方法适合不依赖 Spring 容器的产品创建场景。下面以文件解析器为例，系统支持 CSV 和 JSON 两种文件解析器，不同工厂负责创建不同解析器。

整体关系如下：

```text
FileParserFactory
├── CsvFileParserFactory -> CsvFileParser
└── JsonFileParserFactory -> JsonFileParser
```

### 文件结构

```text
src/main/java/io/github/atengk/design/factorymethod/simple/
├── FileParser.java
├── FileParserFactory.java
├── CsvFileParser.java
├── CsvFileParserFactory.java
├── JsonFileParser.java
└── JsonFileParserFactory.java
```

文件位置：`src/main/java/io/github/atengk/design/factorymethod/simple/FileParser.java`

下面是文件解析器抽象产品接口。

```java
package io.github.atengk.design.factorymethod.simple;

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

    /**
     * 获取文件类型
     *
     * @return 文件类型
     */
    String fileType();
}
```

文件位置：`src/main/java/io/github/atengk/design/factorymethod/simple/FileParserFactory.java`

下面是文件解析器工厂接口。

```java
package io.github.atengk.design.factorymethod.simple;

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

    /**
     * 获取支持的文件类型
     *
     * @return 文件类型
     */
    String supportFileType();
}
```

文件位置：`src/main/java/io/github/atengk/design/factorymethod/simple/CsvFileParser.java`

下面是 CSV 文件解析器实现。

```java
package io.github.atengk.design.factorymethod.simple;

import cn.hutool.core.map.MapUtil;
import cn.hutool.core.util.StrUtil;
import lombok.extern.slf4j.Slf4j;

import java.util.ArrayList;
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
            log.warn("CSV文件解析失败，文件内容为空");
            throw new IllegalArgumentException("CSV文件内容不能为空");
        }

        List<Map<String, Object>> rows = new ArrayList<>();
        String[] lines = content.split("\\R");

        for (int index = 0; index < lines.length; index++) {
            String line = lines[index];
            if (StrUtil.isBlank(line)) {
                continue;
            }

            String[] columns = line.split(",");
            rows.add(MapUtil.<String, Object>builder()
                    .put("lineNo", index + 1)
                    .put("columnCount", columns.length)
                    .put("rawText", line)
                    .build());
        }

        log.info("CSV文件解析完成，行数：{}", rows.size());
        return rows;
    }

    /**
     * 获取文件类型
     *
     * @return 文件类型
     */
    @Override
    public String fileType() {
        return "csv";
    }
}
```

文件位置：`src/main/java/io/github/atengk/design/factorymethod/simple/CsvFileParserFactory.java`

下面是 CSV 文件解析器工厂。

```java
package io.github.atengk.design.factorymethod.simple;

/**
 * CSV文件解析器工厂
 *
 * @author Ateng
 * @since 2026-04-30
 */
public class CsvFileParserFactory implements FileParserFactory {

    /**
     * 创建文件解析器
     *
     * @return CSV文件解析器
     */
    @Override
    public FileParser createParser() {
        return new CsvFileParser();
    }

    /**
     * 获取支持的文件类型
     *
     * @return 文件类型
     */
    @Override
    public String supportFileType() {
        return "csv";
    }
}
```

文件位置：`src/main/java/io/github/atengk/design/factorymethod/simple/JsonFileParser.java`

下面是 JSON 文件解析器实现。示例中用 Hutool JSON 工具将 JSON 数组解析为列表。

```java
package io.github.atengk.design.factorymethod.simple;

import cn.hutool.core.map.MapUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.json.JSONArray;
import cn.hutool.json.JSONUtil;
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
            log.warn("JSON文件解析失败，文件内容为空");
            throw new IllegalArgumentException("JSON文件内容不能为空");
        }

        if (!JSONUtil.isTypeJSONArray(content)) {
            log.warn("JSON文件解析失败，内容不是JSON数组");
            throw new IllegalArgumentException("JSON文件内容必须是数组格式");
        }

        JSONArray jsonArray = JSONUtil.parseArray(content);
        List<Map<String, Object>> rows = jsonArray.stream()
                .map(item -> MapUtil.<String, Object>builder()
                        .put("value", item)
                        .put("type", item == null ? "null" : item.getClass().getSimpleName())
                        .build())
                .toList();

        log.info("JSON文件解析完成，数据量：{}", rows.size());
        return rows;
    }

    /**
     * 获取文件类型
     *
     * @return 文件类型
     */
    @Override
    public String fileType() {
        return "json";
    }
}
```

文件位置：`src/main/java/io/github/atengk/design/factorymethod/simple/JsonFileParserFactory.java`

下面是 JSON 文件解析器工厂。

```java
package io.github.atengk.design.factorymethod.simple;

/**
 * JSON文件解析器工厂
 *
 * @author Ateng
 * @since 2026-04-30
 */
public class JsonFileParserFactory implements FileParserFactory {

    /**
     * 创建文件解析器
     *
     * @return JSON文件解析器
     */
    @Override
    public FileParser createParser() {
        return new JsonFileParser();
    }

    /**
     * 获取支持的文件类型
     *
     * @return 文件类型
     */
    @Override
    public String supportFileType() {
        return "json";
    }
}
```

使用方式：

```java
FileParserFactory factory = new CsvFileParserFactory();
FileParser parser = factory.createParser();

List<Map<String, Object>> rows = parser.parse("""
        orderNo,amount,status
        ORDER10001,99.90,PAID
        ORDER10002,199.00,CREATED
        """);
```

如果需要切换为 JSON 解析，只需要替换工厂：

```java
FileParserFactory factory = new JsonFileParserFactory();
```

调用方依赖的是 `FileParserFactory` 和 `FileParser` 抽象，不直接依赖 `CsvFileParser` 或 `JsonFileParser`。

## Spring Boot 工厂方法

Spring Boot 项目中更常见的写法，是把具体产品和具体工厂都注册为 Bean，调用方通过工厂上下文选择合适工厂，再由工厂创建或返回产品对象。

下面以支付处理器为例，系统支持支付宝、微信、银联三种支付渠道。每个支付渠道对应一个支付处理器，每个支付处理器由自己的工厂创建。

整体流程如下：

```text
Controller
    -> PaymentFactoryContext
        -> PaymentHandlerFactory
            -> PaymentHandler
```

示例支持三个渠道：

```text
alipay    支付宝
wechat    微信支付
unionpay  银联支付
```

### 文件结构

```text
src/main/java/io/github/atengk/design/
├── FactoryMethodApplication.java
├── controller/
│   └── PaymentController.java
├── context/
│   └── PaymentFactoryContext.java
├── dto/
│   ├── PaymentRequest.java
│   └── PaymentResponse.java
├── factory/
│   ├── PaymentHandlerFactory.java
│   ├── AlipayPaymentHandlerFactory.java
│   ├── WechatPaymentHandlerFactory.java
│   └── UnionPayPaymentHandlerFactory.java
└── handler/
    ├── PaymentHandler.java
    ├── AlipayPaymentHandler.java
    ├── WechatPaymentHandler.java
    └── UnionPayPaymentHandler.java
```

文件位置：`src/main/java/io/github/atengk/design/FactoryMethodApplication.java`

下面是 Spring Boot 启动类。

```java
package io.github.atengk.design;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * 工厂方法模式示例启动类
 *
 * @author Ateng
 * @since 2026-04-30
 */
@SpringBootApplication
public class FactoryMethodApplication {

    /**
     * 应用启动入口
     *
     * @param args 启动参数
     */
    public static void main(String[] args) {
        SpringApplication.run(FactoryMethodApplication.class, args);
    }
}
```

文件位置：`src/main/java/io/github/atengk/design/dto/PaymentRequest.java`

下面是支付请求对象。

```java
package io.github.atengk.design.dto;

import java.math.BigDecimal;

/**
 * 支付请求
 *
 * @param channel    支付渠道
 * @param orderNo    订单号
 * @param userId     用户ID
 * @param amount     支付金额
 * @param subject    支付标题
 * @author Ateng
 * @since 2026-04-30
 */
public record PaymentRequest(
        String channel,
        String orderNo,
        Long userId,
        BigDecimal amount,
        String subject
) {
}
```

文件位置：`src/main/java/io/github/atengk/design/dto/PaymentResponse.java`

下面是支付响应对象。

```java
package io.github.atengk.design.dto;

/**
 * 支付响应
 *
 * @param channel 支付渠道
 * @param orderNo 订单号
 * @param tradeNo 第三方交易号
 * @param success 是否成功
 * @param message 响应消息
 * @author Ateng
 * @since 2026-04-30
 */
public record PaymentResponse(
        String channel,
        String orderNo,
        String tradeNo,
        Boolean success,
        String message
) {
}
```

文件位置：`src/main/java/io/github/atengk/design/handler/PaymentHandler.java`

下面是支付处理器抽象产品接口。

```java
package io.github.atengk.design.handler;

import io.github.atengk.design.dto.PaymentRequest;
import io.github.atengk.design.dto.PaymentResponse;

/**
 * 支付处理器
 *
 * @author Ateng
 * @since 2026-04-30
 */
public interface PaymentHandler {

    /**
     * 执行支付
     *
     * @param request 支付请求
     * @return 支付响应
     */
    PaymentResponse pay(PaymentRequest request);

    /**
     * 获取支付渠道
     *
     * @return 支付渠道
     */
    String channel();
}
```

文件位置：`src/main/java/io/github/atengk/design/handler/AlipayPaymentHandler.java`

下面是支付宝支付处理器。

```java
package io.github.atengk.design.handler;

import cn.hutool.core.util.IdUtil;
import cn.hutool.core.util.StrUtil;
import io.github.atengk.design.dto.PaymentRequest;
import io.github.atengk.design.dto.PaymentResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;

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
     * 执行支付
     *
     * @param request 支付请求
     * @return 支付响应
     */
    @Override
    public PaymentResponse pay(PaymentRequest request) {
        validateRequest(request);

        String tradeNo = "ALI" + IdUtil.getSnowflakeNextId();
        log.info("支付宝支付成功，订单号：{}，用户ID：{}，金额：{}，交易号：{}",
                request.orderNo(), request.userId(), request.amount(), tradeNo);

        return new PaymentResponse(channel(), request.orderNo(), tradeNo, true, "支付宝支付成功");
    }

    /**
     * 获取支付渠道
     *
     * @return 支付渠道
     */
    @Override
    public String channel() {
        return "alipay";
    }

    /**
     * 校验支付请求
     *
     * @param request 支付请求
     */
    private void validateRequest(PaymentRequest request) {
        if (request == null) {
            log.warn("支付宝支付失败，请求参数为空");
            throw new IllegalArgumentException("请求参数不能为空");
        }

        if (StrUtil.isBlank(request.orderNo())) {
            log.warn("支付宝支付失败，订单号为空");
            throw new IllegalArgumentException("订单号不能为空");
        }

        if (request.amount() == null || request.amount().compareTo(BigDecimal.ZERO) <= 0) {
            log.warn("支付宝支付失败，金额不合法，金额：{}", request.amount());
            throw new IllegalArgumentException("支付金额必须大于0");
        }
    }
}
```

文件位置：`src/main/java/io/github/atengk/design/handler/WechatPaymentHandler.java`

下面是微信支付处理器。

```java
package io.github.atengk.design.handler;

import cn.hutool.core.util.IdUtil;
import cn.hutool.core.util.StrUtil;
import io.github.atengk.design.dto.PaymentRequest;
import io.github.atengk.design.dto.PaymentResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;

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
     * 执行支付
     *
     * @param request 支付请求
     * @return 支付响应
     */
    @Override
    public PaymentResponse pay(PaymentRequest request) {
        validateRequest(request);

        String tradeNo = "WX" + IdUtil.getSnowflakeNextId();
        log.info("微信支付成功，订单号：{}，用户ID：{}，金额：{}，交易号：{}",
                request.orderNo(), request.userId(), request.amount(), tradeNo);

        return new PaymentResponse(channel(), request.orderNo(), tradeNo, true, "微信支付成功");
    }

    /**
     * 获取支付渠道
     *
     * @return 支付渠道
     */
    @Override
    public String channel() {
        return "wechat";
    }

    /**
     * 校验支付请求
     *
     * @param request 支付请求
     */
    private void validateRequest(PaymentRequest request) {
        if (request == null) {
            log.warn("微信支付失败，请求参数为空");
            throw new IllegalArgumentException("请求参数不能为空");
        }

        if (StrUtil.isBlank(request.orderNo())) {
            log.warn("微信支付失败，订单号为空");
            throw new IllegalArgumentException("订单号不能为空");
        }

        if (request.amount() == null || request.amount().compareTo(BigDecimal.ZERO) <= 0) {
            log.warn("微信支付失败，金额不合法，金额：{}", request.amount());
            throw new IllegalArgumentException("支付金额必须大于0");
        }
    }
}
```

文件位置：`src/main/java/io/github/atengk/design/handler/UnionPayPaymentHandler.java`

下面是银联支付处理器。

```java
package io.github.atengk.design.handler;

import cn.hutool.core.util.IdUtil;
import cn.hutool.core.util.StrUtil;
import io.github.atengk.design.dto.PaymentRequest;
import io.github.atengk.design.dto.PaymentResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;

/**
 * 银联支付处理器
 *
 * @author Ateng
 * @since 2026-04-30
 */
@Slf4j
@Component
public class UnionPayPaymentHandler implements PaymentHandler {

    /**
     * 执行支付
     *
     * @param request 支付请求
     * @return 支付响应
     */
    @Override
    public PaymentResponse pay(PaymentRequest request) {
        validateRequest(request);

        String tradeNo = "UP" + IdUtil.getSnowflakeNextId();
        log.info("银联支付成功，订单号：{}，用户ID：{}，金额：{}，交易号：{}",
                request.orderNo(), request.userId(), request.amount(), tradeNo);

        return new PaymentResponse(channel(), request.orderNo(), tradeNo, true, "银联支付成功");
    }

    /**
     * 获取支付渠道
     *
     * @return 支付渠道
     */
    @Override
    public String channel() {
        return "unionpay";
    }

    /**
     * 校验支付请求
     *
     * @param request 支付请求
     */
    private void validateRequest(PaymentRequest request) {
        if (request == null) {
            log.warn("银联支付失败，请求参数为空");
            throw new IllegalArgumentException("请求参数不能为空");
        }

        if (StrUtil.isBlank(request.orderNo())) {
            log.warn("银联支付失败，订单号为空");
            throw new IllegalArgumentException("订单号不能为空");
        }

        if (request.amount() == null || request.amount().compareTo(BigDecimal.ZERO) <= 0) {
            log.warn("银联支付失败，金额不合法，金额：{}", request.amount());
            throw new IllegalArgumentException("支付金额必须大于0");
        }
    }
}
```

文件位置：`src/main/java/io/github/atengk/design/factory/PaymentHandlerFactory.java`

下面是支付处理器工厂接口。

```java
package io.github.atengk.design.factory;

import io.github.atengk.design.handler.PaymentHandler;

/**
 * 支付处理器工厂
 *
 * @author Ateng
 * @since 2026-04-30
 */
public interface PaymentHandlerFactory {

    /**
     * 创建支付处理器
     *
     * @return 支付处理器
     */
    PaymentHandler createHandler();

    /**
     * 获取支持的支付渠道
     *
     * @return 支付渠道
     */
    String supportChannel();
}
```

文件位置：`src/main/java/io/github/atengk/design/factory/AlipayPaymentHandlerFactory.java`

下面是支付宝支付处理器工厂。

```java
package io.github.atengk.design.factory;

import io.github.atengk.design.handler.AlipayPaymentHandler;
import io.github.atengk.design.handler.PaymentHandler;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

/**
 * 支付宝支付处理器工厂
 *
 * @author Ateng
 * @since 2026-04-30
 */
@Component
@RequiredArgsConstructor
public class AlipayPaymentHandlerFactory implements PaymentHandlerFactory {

    private final AlipayPaymentHandler alipayPaymentHandler;

    /**
     * 创建支付处理器
     *
     * @return 支付处理器
     */
    @Override
    public PaymentHandler createHandler() {
        return alipayPaymentHandler;
    }

    /**
     * 获取支持的支付渠道
     *
     * @return 支付渠道
     */
    @Override
    public String supportChannel() {
        return "alipay";
    }
}
```

文件位置：`src/main/java/io/github/atengk/design/factory/WechatPaymentHandlerFactory.java`

下面是微信支付处理器工厂。

```java
package io.github.atengk.design.factory;

import io.github.atengk.design.handler.PaymentHandler;
import io.github.atengk.design.handler.WechatPaymentHandler;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

/**
 * 微信支付处理器工厂
 *
 * @author Ateng
 * @since 2026-04-30
 */
@Component
@RequiredArgsConstructor
public class WechatPaymentHandlerFactory implements PaymentHandlerFactory {

    private final WechatPaymentHandler wechatPaymentHandler;

    /**
     * 创建支付处理器
     *
     * @return 支付处理器
     */
    @Override
    public PaymentHandler createHandler() {
        return wechatPaymentHandler;
    }

    /**
     * 获取支持的支付渠道
     *
     * @return 支付渠道
     */
    @Override
    public String supportChannel() {
        return "wechat";
    }
}
```

文件位置：`src/main/java/io/github/atengk/design/factory/UnionPayPaymentHandlerFactory.java`

下面是银联支付处理器工厂。

```java
package io.github.atengk.design.factory;

import io.github.atengk.design.handler.PaymentHandler;
import io.github.atengk.design.handler.UnionPayPaymentHandler;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

/**
 * 银联支付处理器工厂
 *
 * @author Ateng
 * @since 2026-04-30
 */
@Component
@RequiredArgsConstructor
public class UnionPayPaymentHandlerFactory implements PaymentHandlerFactory {

    private final UnionPayPaymentHandler unionPayPaymentHandler;

    /**
     * 创建支付处理器
     *
     * @return 支付处理器
     */
    @Override
    public PaymentHandler createHandler() {
        return unionPayPaymentHandler;
    }

    /**
     * 获取支持的支付渠道
     *
     * @return 支付渠道
     */
    @Override
    public String supportChannel() {
        return "unionpay";
    }
}
```

文件位置：`src/main/java/io/github/atengk/design/context/PaymentFactoryContext.java`

下面是支付工厂上下文。它负责根据支付渠道找到对应具体工厂。

```java
package io.github.atengk.design.context;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.StrUtil;
import io.github.atengk.design.factory.PaymentHandlerFactory;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * 支付工厂上下文
 *
 * @author Ateng
 * @since 2026-04-30
 */
@Slf4j
@Component
public class PaymentFactoryContext {

    private final Map<String, PaymentHandlerFactory> factoryMap;

    /**
     * 创建支付工厂上下文
     *
     * @param factories 支付处理器工厂列表
     */
    public PaymentFactoryContext(List<PaymentHandlerFactory> factories) {
        if (CollUtil.isEmpty(factories)) {
            log.warn("支付处理器工厂列表为空");
            this.factoryMap = Map.of();
            return;
        }

        this.factoryMap = factories.stream()
                .collect(Collectors.toUnmodifiableMap(
                        factory -> StrUtil.trim(factory.supportChannel()).toLowerCase(),
                        Function.identity()
                ));

        log.info("初始化支付工厂上下文，支持渠道：{}", factoryMap.keySet());
    }

    /**
     * 获取支付处理器工厂
     *
     * @param channel 支付渠道
     * @return 支付处理器工厂
     */
    public PaymentHandlerFactory getFactory(String channel) {
        if (StrUtil.isBlank(channel)) {
            log.warn("获取支付处理器工厂失败，支付渠道为空");
            throw new IllegalArgumentException("支付渠道不能为空");
        }

        String key = StrUtil.trim(channel).toLowerCase();
        PaymentHandlerFactory factory = factoryMap.get(key);

        if (factory == null) {
            log.warn("获取支付处理器工厂失败，不支持的支付渠道：{}", channel);
            throw new IllegalArgumentException("不支持的支付渠道：" + channel);
        }

        return factory;
    }
}
```

文件位置：`src/main/java/io/github/atengk/design/controller/PaymentController.java`

下面是支付接口，用于验证工厂方法模式效果。

```java
package io.github.atengk.design.controller;

import cn.hutool.core.util.StrUtil;
import io.github.atengk.design.context.PaymentFactoryContext;
import io.github.atengk.design.dto.PaymentRequest;
import io.github.atengk.design.dto.PaymentResponse;
import io.github.atengk.design.factory.PaymentHandlerFactory;
import io.github.atengk.design.handler.PaymentHandler;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;

/**
 * 支付控制器
 *
 * @author Ateng
 * @since 2026-04-30
 */
@Slf4j
@RestController
@RequiredArgsConstructor
@RequestMapping("/factory-method/payment")
public class PaymentController {

    private final PaymentFactoryContext paymentFactoryContext;

    /**
     * 执行支付
     *
     * @param channel 支付渠道
     * @param orderNo 订单号
     * @param userId  用户ID
     * @param amount  支付金额
     * @param subject 支付标题
     * @return 支付响应
     */
    @PostMapping("/pay")
    public PaymentResponse pay(@RequestParam String channel,
                               @RequestParam String orderNo,
                               @RequestParam Long userId,
                               @RequestParam BigDecimal amount,
                               @RequestParam String subject) {
        PaymentRequest request = new PaymentRequest(
                channel,
                orderNo,
                userId,
                amount,
                subject
        );

        validateRequest(request);

        PaymentHandlerFactory factory = paymentFactoryContext.getFactory(request.channel());
        PaymentHandler handler = factory.createHandler();

        log.info("匹配支付处理器成功，支付渠道：{}，订单号：{}", handler.channel(), request.orderNo());
        return handler.pay(request);
    }

    /**
     * 校验支付请求
     *
     * @param request 支付请求
     */
    private void validateRequest(PaymentRequest request) {
        if (request == null) {
            log.warn("支付失败，请求参数为空");
            throw new IllegalArgumentException("请求参数不能为空");
        }

        if (StrUtil.hasBlank(request.channel(), request.orderNo(), request.subject())) {
            log.warn("支付失败，支付渠道、订单号或标题为空");
            throw new IllegalArgumentException("支付渠道、订单号和标题不能为空");
        }

        if (request.userId() == null || request.userId() <= 0) {
            log.warn("支付失败，用户ID不合法，用户ID：{}", request.userId());
            throw new IllegalArgumentException("用户ID必须大于0");
        }

        if (request.amount() == null || request.amount().compareTo(BigDecimal.ZERO) <= 0) {
            log.warn("支付失败，金额不合法，金额：{}", request.amount());
            throw new IllegalArgumentException("支付金额必须大于0");
        }
    }
}
```

接口调用示例：

```bash
curl -X POST "http://localhost:8080/factory-method/payment/pay?channel=alipay&orderNo=ORDER10001&userId=10001&amount=99.90&subject=键盘订单"

curl -X POST "http://localhost:8080/factory-method/payment/pay?channel=wechat&orderNo=ORDER10002&userId=10002&amount=66.60&subject=鼠标订单"

curl -X POST "http://localhost:8080/factory-method/payment/pay?channel=unionpay&orderNo=ORDER10003&userId=10003&amount=188.00&subject=显示器订单"
```

支付宝可能返回：

```json
{
  "channel": "alipay",
  "orderNo": "ORDER10001",
  "tradeNo": "ALI2019776866538487808",
  "success": true,
  "message": "支付宝支付成功"
}
```

这种方式的优点是 Controller 不需要直接 `new` 支付处理器，也不需要写大量 `if else`。新增支付渠道时，新增处理器和工厂即可。

## 扩展一个新支付工厂

在 Spring Boot 工厂方法模式中，新增产品通常需要新增具体产品和具体工厂。下面以 PayPal 支付为例。

### 文件结构

```text
src/main/java/io/github/atengk/design/
├── handler/
│   └── PaypalPaymentHandler.java
└── factory/
    └── PaypalPaymentHandlerFactory.java
```

文件位置：`src/main/java/io/github/atengk/design/handler/PaypalPaymentHandler.java`

下面是 PayPal 支付处理器。

```java
package io.github.atengk.design.handler;

import cn.hutool.core.util.IdUtil;
import cn.hutool.core.util.StrUtil;
import io.github.atengk.design.dto.PaymentRequest;
import io.github.atengk.design.dto.PaymentResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;

/**
 * PayPal支付处理器
 *
 * @author Ateng
 * @since 2026-04-30
 */
@Slf4j
@Component
public class PaypalPaymentHandler implements PaymentHandler {

    /**
     * 执行支付
     *
     * @param request 支付请求
     * @return 支付响应
     */
    @Override
    public PaymentResponse pay(PaymentRequest request) {
        if (request == null || StrUtil.isBlank(request.orderNo())) {
            log.warn("PayPal支付失败，请求参数或订单号为空");
            throw new IllegalArgumentException("请求参数和订单号不能为空");
        }

        if (request.amount() == null || request.amount().compareTo(BigDecimal.ZERO) <= 0) {
            log.warn("PayPal支付失败，金额不合法，订单号：{}，金额：{}", request.orderNo(), request.amount());
            throw new IllegalArgumentException("支付金额必须大于0");
        }

        String tradeNo = "PAYPAL" + IdUtil.getSnowflakeNextId();
        log.info("PayPal支付成功，订单号：{}，金额：{}，交易号：{}",
                request.orderNo(), request.amount(), tradeNo);

        return new PaymentResponse(channel(), request.orderNo(), tradeNo, true, "PayPal支付成功");
    }

    /**
     * 获取支付渠道
     *
     * @return 支付渠道
     */
    @Override
    public String channel() {
        return "paypal";
    }
}
```

文件位置：`src/main/java/io/github/atengk/design/factory/PaypalPaymentHandlerFactory.java`

下面是 PayPal 支付处理器工厂。

```java
package io.github.atengk.design.factory;

import io.github.atengk.design.handler.PaymentHandler;
import io.github.atengk.design.handler.PaypalPaymentHandler;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

/**
 * PayPal支付处理器工厂
 *
 * @author Ateng
 * @since 2026-04-30
 */
@Component
@RequiredArgsConstructor
public class PaypalPaymentHandlerFactory implements PaymentHandlerFactory {

    private final PaypalPaymentHandler paypalPaymentHandler;

    /**
     * 创建支付处理器
     *
     * @return 支付处理器
     */
    @Override
    public PaymentHandler createHandler() {
        return paypalPaymentHandler;
    }

    /**
     * 获取支持的支付渠道
     *
     * @return 支付渠道
     */
    @Override
    public String supportChannel() {
        return "paypal";
    }
}
```

调用示例：

```bash
curl -X POST "http://localhost:8080/factory-method/payment/pay?channel=paypal&orderNo=ORDER10004&userId=10004&amount=20.00&subject=测试订单"
```

新增 PayPal 后，`PaymentController` 和 `PaymentFactoryContext` 不需要修改。Spring 会自动把新的工厂 Bean 注入到工厂列表中。

## 工厂方法模式和简单工厂

简单工厂通常把创建逻辑集中在一个类中，根据类型判断创建哪个产品。

简单工厂示例：

```java
public PaymentHandler createHandler(String channel) {
    if ("alipay".equals(channel)) {
        return new AlipayPaymentHandler();
    }
    if ("wechat".equals(channel)) {
        return new WechatPaymentHandler();
    }
    throw new IllegalArgumentException("不支持的支付渠道：" + channel);
}
```

这种方式适合产品数量少、创建逻辑简单的场景。但随着产品类型增加，简单工厂容易变成巨大 `if else` 或 `switch`。

对比如下：

| 对比项   | 简单工厂         | 工厂方法模式         |
| -------- | ---------------- | -------------------- |
| 工厂数量 | 一个工厂         | 多个具体工厂         |
| 创建逻辑 | 集中在一个类中   | 分散到具体工厂中     |
| 扩展产品 | 通常要修改原工厂 | 新增具体工厂         |
| 复杂度   | 低               | 中等                 |
| 适合场景 | 产品少、变化少   | 产品多、创建逻辑复杂 |

简单理解：

```text
简单工厂：一个工厂根据类型创建不同产品。
工厂方法：每个具体工厂负责创建一种产品。
```

如果项目中只有两三个简单对象，简单工厂更直接。如果产品类型持续增加，工厂方法更容易扩展。

## 工厂方法模式和抽象工厂模式

工厂方法模式和抽象工厂模式都属于创建型模式，但创建粒度不同。

| 对比项       | 工厂方法模式           | 抽象工厂模式                       |
| ------------ | ---------------------- | ---------------------------------- |
| 创建对象     | 一个产品对象           | 一组相关产品对象                   |
| 关注点       | 单个产品扩展           | 产品族一致性                       |
| 工厂方法数量 | 通常一个主要创建方法   | 多个创建方法                       |
| 典型场景     | 支付处理器、文件解析器 | 多云厂商组件族、多数据库方言组件族 |
| 扩展产品     | 新增具体工厂           | 新增产品族容易，新增产品等级麻烦   |

简单理解：

```text
工厂方法模式：创建一个对象。
抽象工厂模式：创建一整套对象。
```

如果只创建一个支付处理器，使用工厂方法模式即可。如果一个云厂商下要同时创建对象存储、短信、MQ 等一组组件，则更适合抽象工厂模式。

## 工厂方法模式和策略模式

工厂方法模式和策略模式经常在 Spring Boot 项目中写得很像，都是接口加多个实现。但二者关注点不同。

| 对比项   | 工厂方法模式               | 策略模式                            |
| -------- | -------------------------- | ----------------------------------- |
| 核心目的 | 创建对象                   | 执行业务算法                        |
| 关注点   | 对象创建过程               | 行为选择和执行                      |
| 典型方法 | `createHandler()`          | `execute()`、`calculate()`、`pay()` |
| 调用时机 | 先创建产品，再使用产品     | 直接选择策略执行                    |
| 典型场景 | 创建解析器、处理器、客户端 | 优惠计算、支付规则、物流计费        |

简单理解：

```text
工厂方法模式：重点是“怎么创建处理器”。
策略模式：重点是“选哪个处理逻辑执行”。
```

在实际项目中，两者可以组合使用。工厂负责创建处理器，处理器本身也可以是某种策略。

如果处理器都是 Spring 单例 Bean，且创建逻辑几乎没有差异，那么直接使用策略模式的 Bean Map 也可以，不一定必须显式建工厂类。

## 验证方式

启动 Spring Boot 项目：

```bash
mvn spring-boot:run
```

执行支付宝支付：

```bash
curl -X POST "http://localhost:8080/factory-method/payment/pay?channel=alipay&orderNo=ORDER10001&userId=10001&amount=99.90&subject=键盘订单"
```

执行微信支付：

```bash
curl -X POST "http://localhost:8080/factory-method/payment/pay?channel=wechat&orderNo=ORDER10002&userId=10002&amount=66.60&subject=鼠标订单"
```

执行银联支付：

```bash
curl -X POST "http://localhost:8080/factory-method/payment/pay?channel=unionpay&orderNo=ORDER10003&userId=10003&amount=188.00&subject=显示器订单"
```

如果工厂方法模式正常，可以看到类似日志：

```text
初始化支付工厂上下文，支持渠道：[alipay, wechat, unionpay]
匹配支付处理器成功，支付渠道：alipay，订单号：ORDER10001
支付宝支付成功，订单号：ORDER10001，用户ID：10001，金额：99.90，交易号：ALI2019776866538487808
```

执行不支持的支付渠道：

```bash
curl -X POST "http://localhost:8080/factory-method/payment/pay?channel=unknown&orderNo=ORDER10004&userId=10004&amount=20.00&subject=测试订单"
```

异常日志示例：

```text
获取支付处理器工厂失败，不支持的支付渠道：unknown
```

实际项目中建议结合全局异常处理器，将业务异常转换成统一响应结构。

## 注意事项

工厂方法模式适合隔离对象创建逻辑，但不要为了创建一个非常简单的对象而引入大量工厂类。对象创建本身简单、类型数量很少时，简单工厂或直接 Spring 注入更合适。

适合使用工厂方法模式的场景：

```text
产品类型较多
产品创建逻辑复杂
产品扩展比较频繁
调用方不应该依赖具体产品类
希望消除分散的 new 操作
希望每种产品创建逻辑独立维护
```

不太适合使用工厂方法模式的场景：

```text
只创建一个固定对象
对象创建没有变化
产品类型很少且不会扩展
直接 Spring 注入即可满足需求
为了模式而增加空壳工厂
```

不要把工厂方法写成新的上帝工厂。

不推荐写法：

```java
public class PaymentFactory {

    public PaymentHandler create(String channel) {
        if ("alipay".equals(channel)) {
            // 大量支付宝创建逻辑
        } else if ("wechat".equals(channel)) {
            // 大量微信创建逻辑
        } else if ("unionpay".equals(channel)) {
            // 大量银联创建逻辑
        }
        return null;
    }
}
```

推荐将创建逻辑拆分给具体工厂：

```java
public class AlipayPaymentHandlerFactory implements PaymentHandlerFactory {

    public PaymentHandler createHandler() {
        return alipayPaymentHandler;
    }
}
```

如果具体产品是 Spring 单例 Bean，工厂方法通常返回已有 Bean，而不是每次 `new` 一个对象。

推荐：

```java
@Override
public PaymentHandler createHandler() {
    return alipayPaymentHandler;
}
```

如果产品对象需要保存请求级状态，不要把状态放在 Spring 单例 Bean 中。可以把请求状态放在方法参数中，或者在工厂中创建新的命令对象、上下文对象。

错误示例：

```java
private String currentOrderNo;
private BigDecimal currentAmount;
```

推荐：

```java
public PaymentResponse pay(PaymentRequest request) {
    return doPay(request.orderNo(), request.amount());
}
```

如果产品创建涉及外部配置，例如支付密钥、回调地址、超时时间，建议使用 `@ConfigurationProperties` 或配置中心统一管理，不要硬编码在工厂或产品类中。

常见配置项：

```text
merchantId
appId
privateKey
publicKey
notifyUrl
gatewayUrl
connectTimeout
readTimeout
retryTimes
```

工厂方法模式只解决对象创建问题，不自动解决业务幂等、状态流转、事务一致性和外部调用可靠性。支付、发货、退款等核心业务仍然需要结合状态模式、命令模式、事务、幂等表、消息队列和补偿机制。

## 总结

在 JDK21 和 Spring Boot 3 项目中，工厂方法模式的实践重点是把具体产品创建逻辑放到具体工厂中，让调用方依赖抽象工厂和抽象产品。

普通 Java 工厂方法适合理解原理和本地对象创建。Spring Boot 项目中更推荐使用“产品接口 + 具体产品 Bean + 工厂接口 + 具体工厂 Bean + 工厂上下文”的结构。对于支付处理器、文件解析器、导入处理器、消息发送器等场景，工厂方法模式可以减少调用方对具体类的依赖，并让新增产品更加清晰。

工厂方法模式不是为了替代所有 `new`，也不是为了替代 Spring 的依赖注入。它最适合处理“产品类型会扩展、创建逻辑需要隔离、调用方不应该感知具体产品类”的场景。实际落地时，需要控制工厂数量和职责边界，避免为了模式引入过度设计。
