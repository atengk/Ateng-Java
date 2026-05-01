# 设计模式：模板方法模式

模板方法模式用于在父类中定义一套固定执行流程，把流程中可变的步骤延迟到子类实现。在 JDK21 和 Spring Boot 3 项目中，模板方法模式常用于订单处理、文件导入、数据同步、支付回调、审批流、定时任务、接口调用封装、消息消费等场景。

需要注意：模板方法模式适合“流程固定、步骤可变”的业务。如果只是简单的单个算法切换，策略模式会更轻；如果既有固定流程，又有部分步骤需要差异化实现，模板方法模式更合适。

## 基础配置

本示例基于 JDK21、Spring Boot 3、Maven 项目。示例包路径统一使用 `io.github.atengk`。

文件位置：`pom.xml`

```xml
<dependencies>
    <!-- Spring Boot Web，用于提供接口验证模板方法模式行为 -->
    <dependency>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-starter-web</artifactId>
    </dependency>

    <!-- Hutool 工具类，用于字符串、集合、金额、ID 等通用处理 -->
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

模板方法模式的核心目标是让父类控制流程，让子类负责变化点。

常见角色如下：

| 角色       | 说明                                                  |
| ---------- | ----------------------------------------------------- |
| 抽象模板类 | 定义固定流程，提供模板方法                            |
| 模板方法   | 串联多个步骤，通常使用 `final` 修饰，避免子类破坏流程 |
| 抽象步骤   | 父类只定义方法，具体实现交给子类                      |
| 默认步骤   | 父类提供默认实现，子类可选择覆盖                      |
| 钩子方法   | 父类提供空实现或默认判断，用于控制流程分支            |
| 具体模板类 | 实现差异化步骤                                        |

模板方法模式的典型结构如下：

```text
固定流程：
参数校验 -> 前置处理 -> 核心处理 -> 后置处理 -> 返回结果

变化点：
不同业务类型的参数校验、核心处理、后置处理可以不同。
```

在 Spring Boot 项目中，常见优先级通常是：

```text
Spring Bean 模板方法 > 普通 Java 模板方法 > 大量重复流程代码
```

模板方法模式和策略模式的区别是：策略模式强调“替换算法”，模板方法模式强调“复用流程”。

## 普通 Java 模板方法

普通 Java 模板方法适合不依赖 Spring 容器的流程封装。下面以文件导入为例，不同文件类型的解析逻辑不同，但整体导入流程相同。

整体流程如下：

```text
校验文件内容 -> 解析文件 -> 校验数据 -> 保存数据 -> 返回导入结果
```

### 文件结构

```text
src/main/java/io/github/atengk/design/template/simple/
├── ImportRequest.java
├── ImportResult.java
├── AbstractFileImportTemplate.java
├── CsvFileImportTemplate.java
└── JsonFileImportTemplate.java
```

文件位置：`src/main/java/io/github/atengk/design/template/simple/ImportRequest.java`

下面是文件导入请求参数对象。

```java
package io.github.atengk.design.template.simple;

/**
 * 文件导入请求
 *
 * @param fileName 文件名
 * @param content  文件内容
 * @author Ateng
 * @since 2026-04-30
 */
public record ImportRequest(String fileName, String content) {
}
```

文件位置：`src/main/java/io/github/atengk/design/template/simple/ImportResult.java`

下面是文件导入结果对象。

```java
package io.github.atengk.design.template.simple;

/**
 * 文件导入结果
 *
 * @param fileName    文件名
 * @param success     是否成功
 * @param totalCount  总数量
 * @param message     结果消息
 * @author Ateng
 * @since 2026-04-30
 */
public record ImportResult(String fileName, Boolean success, Integer totalCount, String message) {
}
```

文件位置：`src/main/java/io/github/atengk/design/template/simple/AbstractFileImportTemplate.java`

下面是文件导入抽象模板类。`importFile` 是模板方法，用于固定整体执行流程。

```java
package io.github.atengk.design.template.simple;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.StrUtil;
import lombok.extern.slf4j.Slf4j;

import java.util.List;
import java.util.Map;

/**
 * 文件导入抽象模板
 *
 * @author Ateng
 * @since 2026-04-30
 */
@Slf4j
public abstract class AbstractFileImportTemplate {

    /**
     * 导入文件
     *
     * @param request 文件导入请求
     * @return 文件导入结果
     */
    public final ImportResult importFile(ImportRequest request) {
        validateRequest(request);

        log.info("开始导入文件，文件名：{}", request.fileName());

        List<Map<String, Object>> rows = parseContent(request.content());
        validateRows(rows);
        saveRows(rows);

        ImportResult result = new ImportResult(request.fileName(), true, rows.size(), "导入成功");
        log.info("文件导入完成，文件名：{}，数据量：{}", request.fileName(), rows.size());
        return result;
    }

    /**
     * 校验导入请求
     *
     * @param request 文件导入请求
     */
    protected void validateRequest(ImportRequest request) {
        if (request == null) {
            log.warn("文件导入失败，请求参数为空");
            throw new IllegalArgumentException("请求参数不能为空");
        }

        if (StrUtil.isBlank(request.fileName())) {
            log.warn("文件导入失败，文件名为空");
            throw new IllegalArgumentException("文件名不能为空");
        }

        if (StrUtil.isBlank(request.content())) {
            log.warn("文件导入失败，文件内容为空，文件名：{}", request.fileName());
            throw new IllegalArgumentException("文件内容不能为空");
        }
    }

    /**
     * 解析文件内容
     *
     * @param content 文件内容
     * @return 解析后的数据行
     */
    protected abstract List<Map<String, Object>> parseContent(String content);

    /**
     * 校验数据行
     *
     * @param rows 数据行
     */
    protected void validateRows(List<Map<String, Object>> rows) {
        if (CollUtil.isEmpty(rows)) {
            log.warn("文件导入失败，解析结果为空");
            throw new IllegalArgumentException("解析结果不能为空");
        }
    }

    /**
     * 保存数据行
     *
     * @param rows 数据行
     */
    protected void saveRows(List<Map<String, Object>> rows) {
        log.info("保存导入数据，数据量：{}", rows.size());
    }
}
```

文件位置：`src/main/java/io/github/atengk/design/template/simple/CsvFileImportTemplate.java`

下面是 CSV 文件导入模板，只实现 CSV 解析这一变化点。

```java
package io.github.atengk.design.template.simple;

import cn.hutool.core.map.MapUtil;
import cn.hutool.core.text.csv.CsvData;
import cn.hutool.core.text.csv.CsvReader;
import cn.hutool.core.text.csv.CsvUtil;
import lombok.extern.slf4j.Slf4j;

import java.io.StringReader;
import java.util.List;
import java.util.Map;

/**
 * CSV文件导入模板
 *
 * @author Ateng
 * @since 2026-04-30
 */
@Slf4j
public class CsvFileImportTemplate extends AbstractFileImportTemplate {

    /**
     * 解析文件内容
     *
     * @param content 文件内容
     * @return 解析后的数据行
     */
    @Override
    protected List<Map<String, Object>> parseContent(String content) {
        CsvReader reader = CsvUtil.getReader();
        CsvData csvData = reader.read(new StringReader(content));

        log.info("解析CSV文件完成，行数：{}", csvData.getRowCount());

        return List.of(MapUtil.<String, Object>builder()
                .put("fileType", "csv")
                .put("rowCount", csvData.getRowCount())
                .build());
    }
}
```

文件位置：`src/main/java/io/github/atengk/design/template/simple/JsonFileImportTemplate.java`

下面是 JSON 文件导入模板，只实现 JSON 解析这一变化点。

```java
package io.github.atengk.design.template.simple;

import cn.hutool.core.map.MapUtil;
import cn.hutool.json.JSONUtil;
import lombok.extern.slf4j.Slf4j;

import java.util.List;
import java.util.Map;

/**
 * JSON文件导入模板
 *
 * @author Ateng
 * @since 2026-04-30
 */
@Slf4j
public class JsonFileImportTemplate extends AbstractFileImportTemplate {

    /**
     * 解析文件内容
     *
     * @param content 文件内容
     * @return 解析后的数据行
     */
    @Override
    protected List<Map<String, Object>> parseContent(String content) {
        Object json = JSONUtil.parse(content);

        log.info("解析JSON文件完成，JSON类型：{}", json.getClass().getSimpleName());

        return List.of(MapUtil.<String, Object>builder()
                .put("fileType", "json")
                .put("content", json)
                .build());
    }
}
```

使用方式：

```java
AbstractFileImportTemplate csvTemplate = new CsvFileImportTemplate();
ImportResult csvResult = csvTemplate.importFile(new ImportRequest(
        "user.csv",
        "id,name\n1,Ateng"
));

AbstractFileImportTemplate jsonTemplate = new JsonFileImportTemplate();
ImportResult jsonResult = jsonTemplate.importFile(new ImportRequest(
        "user.json",
        "{\"id\":1,\"name\":\"Ateng\"}"
));
```

普通 Java 模板方法的优点是结构清晰，不依赖 Spring。缺点是模板对象需要手动创建，不适合需要注入数据库、Redis、第三方客户端等 Spring Bean 的业务场景。

## Spring Boot 模板方法

Spring Boot 项目中更常见的写法，是把抽象模板类和具体模板类都交给 Spring 管理。下面以订单提交为例，不同订单类型的处理细节不同，但整体提交流程相同。

整体流程如下：

```text
校验请求 -> 创建订单号 -> 计算金额 -> 扣减库存 -> 保存订单 -> 发送通知 -> 返回结果
```

示例支持两种订单类型：

```text
normal      普通订单
flash_sale  秒杀订单
```

### 文件结构

```text
src/main/java/io/github/atengk/design/
├── TemplateMethodApplication.java
├── controller/
│   └── OrderSubmitController.java
├── dto/
│   ├── OrderSubmitRequest.java
│   └── OrderSubmitResponse.java
├── template/
│   ├── AbstractOrderSubmitTemplate.java
│   ├── NormalOrderSubmitTemplate.java
│   └── FlashSaleOrderSubmitTemplate.java
└── context/
    └── OrderSubmitContext.java
```

文件位置：`src/main/java/io/github/atengk/design/TemplateMethodApplication.java`

下面是 Spring Boot 启动类。

```java
package io.github.atengk.design;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * 模板方法模式示例启动类
 *
 * @author Ateng
 * @since 2026-04-30
 */
@SpringBootApplication
public class TemplateMethodApplication {

    /**
     * 应用启动入口
     *
     * @param args 启动参数
     */
    public static void main(String[] args) {
        SpringApplication.run(TemplateMethodApplication.class, args);
    }
}
```

文件位置：`src/main/java/io/github/atengk/design/dto/OrderSubmitRequest.java`

下面是订单提交请求参数对象。

```java
package io.github.atengk.design.dto;

import java.math.BigDecimal;

/**
 * 订单提交请求
 *
 * @param orderType   订单类型
 * @param userId      用户ID
 * @param productId   商品ID
 * @param productName 商品名称
 * @param quantity    购买数量
 * @param unitPrice   商品单价
 * @author Ateng
 * @since 2026-04-30
 */
public record OrderSubmitRequest(
        String orderType,
        Long userId,
        Long productId,
        String productName,
        Integer quantity,
        BigDecimal unitPrice
) {
}
```

文件位置：`src/main/java/io/github/atengk/design/dto/OrderSubmitResponse.java`

下面是订单提交响应结果。

```java
package io.github.atengk.design.dto;

import java.math.BigDecimal;

/**
 * 订单提交响应
 *
 * @param orderNo     订单号
 * @param orderType   订单类型
 * @param productName 商品名称
 * @param quantity    购买数量
 * @param payAmount   应付金额
 * @param message     结果消息
 * @author Ateng
 * @since 2026-04-30
 */
public record OrderSubmitResponse(
        String orderNo,
        String orderType,
        String productName,
        Integer quantity,
        BigDecimal payAmount,
        String message
) {
}
```

文件位置：`src/main/java/io/github/atengk/design/template/AbstractOrderSubmitTemplate.java`

下面是订单提交抽象模板类。`submit` 是模板方法，使用 `final` 固定订单提交流程，子类只能扩展具体步骤。

```java
package io.github.atengk.design.template;

import cn.hutool.core.util.IdUtil;
import cn.hutool.core.util.NumberUtil;
import cn.hutool.core.util.StrUtil;
import io.github.atengk.design.dto.OrderSubmitRequest;
import io.github.atengk.design.dto.OrderSubmitResponse;
import lombok.extern.slf4j.Slf4j;

import java.math.BigDecimal;
import java.math.RoundingMode;

/**
 * 订单提交抽象模板
 *
 * @author Ateng
 * @since 2026-04-30
 */
@Slf4j
public abstract class AbstractOrderSubmitTemplate {

    /**
     * 提交订单
     *
     * @param request 订单提交请求
     * @return 订单提交响应
     */
    public final OrderSubmitResponse submit(OrderSubmitRequest request) {
        validateRequest(request);

        String orderNo = createOrderNo(request);
        beforeCalculateAmount(request, orderNo);

        BigDecimal payAmount = calculateAmount(request);
        deductStock(request, orderNo);
        saveOrder(request, orderNo, payAmount);

        if (needSendNotice()) {
            sendNotice(request, orderNo);
        }

        log.info("订单提交完成，订单号：{}，订单类型：{}，应付金额：{}", orderNo, supportType(), payAmount);

        return new OrderSubmitResponse(
                orderNo,
                supportType(),
                request.productName(),
                request.quantity(),
                payAmount,
                "提交成功"
        );
    }

    /**
     * 获取支持的订单类型
     *
     * @return 订单类型
     */
    public abstract String supportType();

    /**
     * 校验订单提交请求
     *
     * @param request 订单提交请求
     */
    protected void validateRequest(OrderSubmitRequest request) {
        if (request == null) {
            log.warn("订单提交失败，请求参数为空");
            throw new IllegalArgumentException("请求参数不能为空");
        }

        if (StrUtil.isBlank(request.orderType())) {
            log.warn("订单提交失败，订单类型为空");
            throw new IllegalArgumentException("订单类型不能为空");
        }

        if (request.userId() == null || request.userId() <= 0) {
            log.warn("订单提交失败，用户ID不合法，用户ID：{}", request.userId());
            throw new IllegalArgumentException("用户ID必须大于0");
        }

        if (request.productId() == null || request.productId() <= 0) {
            log.warn("订单提交失败，商品ID不合法，商品ID：{}", request.productId());
            throw new IllegalArgumentException("商品ID必须大于0");
        }

        if (StrUtil.isBlank(request.productName())) {
            log.warn("订单提交失败，商品名称为空");
            throw new IllegalArgumentException("商品名称不能为空");
        }

        if (request.quantity() == null || request.quantity() <= 0) {
            log.warn("订单提交失败，购买数量不合法，购买数量：{}", request.quantity());
            throw new IllegalArgumentException("购买数量必须大于0");
        }

        if (request.unitPrice() == null || request.unitPrice().compareTo(BigDecimal.ZERO) <= 0) {
            log.warn("订单提交失败，商品单价不合法，商品单价：{}", request.unitPrice());
            throw new IllegalArgumentException("商品单价必须大于0");
        }
    }

    /**
     * 创建订单号
     *
     * @param request 订单提交请求
     * @return 订单号
     */
    protected String createOrderNo(OrderSubmitRequest request) {
        String orderNo = StrUtil.format("{}{}", supportType().toUpperCase().replace("-", "_"), IdUtil.getSnowflakeNextId());
        log.info("创建订单号，订单类型：{}，订单号：{}", supportType(), orderNo);
        return orderNo;
    }

    /**
     * 计算金额前置处理
     *
     * @param request 订单提交请求
     * @param orderNo 订单号
     */
    protected void beforeCalculateAmount(OrderSubmitRequest request, String orderNo) {
        log.debug("执行金额计算前置处理，订单号：{}", orderNo);
    }

    /**
     * 计算应付金额
     *
     * @param request 订单提交请求
     * @return 应付金额
     */
    protected BigDecimal calculateAmount(OrderSubmitRequest request) {
        BigDecimal amount = NumberUtil.mul(request.unitPrice(), BigDecimal.valueOf(request.quantity()));
        return amount.setScale(2, RoundingMode.HALF_UP);
    }

    /**
     * 扣减库存
     *
     * @param request 订单提交请求
     * @param orderNo 订单号
     */
    protected abstract void deductStock(OrderSubmitRequest request, String orderNo);

    /**
     * 保存订单
     *
     * @param request   订单提交请求
     * @param orderNo   订单号
     * @param payAmount 应付金额
     */
    protected void saveOrder(OrderSubmitRequest request, String orderNo, BigDecimal payAmount) {
        log.info("保存订单，订单号：{}，用户ID：{}，商品ID：{}，金额：{}",
                orderNo, request.userId(), request.productId(), payAmount);
    }

    /**
     * 是否需要发送通知
     *
     * @return true 表示发送通知，false 表示不发送
     */
    protected boolean needSendNotice() {
        return true;
    }

    /**
     * 发送通知
     *
     * @param request 订单提交请求
     * @param orderNo 订单号
     */
    protected void sendNotice(OrderSubmitRequest request, String orderNo) {
        log.info("发送订单通知，订单号：{}，用户ID：{}", orderNo, request.userId());
    }
}
```

文件位置：`src/main/java/io/github/atengk/design/template/NormalOrderSubmitTemplate.java`

下面是普通订单提交模板。普通订单使用默认金额计算逻辑，只实现普通库存扣减逻辑。

```java
package io.github.atengk.design.template;

import io.github.atengk.design.dto.OrderSubmitRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * 普通订单提交模板
 *
 * @author Ateng
 * @since 2026-04-30
 */
@Slf4j
@Component
public class NormalOrderSubmitTemplate extends AbstractOrderSubmitTemplate {

    /**
     * 获取支持的订单类型
     *
     * @return 订单类型
     */
    @Override
    public String supportType() {
        return "normal";
    }

    /**
     * 扣减库存
     *
     * @param request 订单提交请求
     * @param orderNo 订单号
     */
    @Override
    protected void deductStock(OrderSubmitRequest request, String orderNo) {
        log.info("扣减普通商品库存，订单号：{}，商品ID：{}，数量：{}",
                orderNo, request.productId(), request.quantity());
    }
}
```

文件位置：`src/main/java/io/github/atengk/design/template/FlashSaleOrderSubmitTemplate.java`

下面是秒杀订单提交模板。秒杀订单覆盖了前置处理、金额计算、库存扣减和通知控制逻辑。

```java
package io.github.atengk.design.template;

import cn.hutool.core.util.NumberUtil;
import io.github.atengk.design.dto.OrderSubmitRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.RoundingMode;

/**
 * 秒杀订单提交模板
 *
 * @author Ateng
 * @since 2026-04-30
 */
@Slf4j
@Component
public class FlashSaleOrderSubmitTemplate extends AbstractOrderSubmitTemplate {

    private static final BigDecimal FLASH_SALE_DISCOUNT_RATE = BigDecimal.valueOf(0.8);

    /**
     * 获取支持的订单类型
     *
     * @return 订单类型
     */
    @Override
    public String supportType() {
        return "flash_sale";
    }

    /**
     * 计算金额前置处理
     *
     * @param request 订单提交请求
     * @param orderNo 订单号
     */
    @Override
    protected void beforeCalculateAmount(OrderSubmitRequest request, String orderNo) {
        log.info("校验秒杀活动资格，订单号：{}，用户ID：{}，商品ID：{}",
                orderNo, request.userId(), request.productId());
    }

    /**
     * 计算应付金额
     *
     * @param request 订单提交请求
     * @return 应付金额
     */
    @Override
    protected BigDecimal calculateAmount(OrderSubmitRequest request) {
        BigDecimal originalAmount = NumberUtil.mul(request.unitPrice(), BigDecimal.valueOf(request.quantity()));
        BigDecimal payAmount = NumberUtil.mul(originalAmount, FLASH_SALE_DISCOUNT_RATE).setScale(2, RoundingMode.HALF_UP);

        log.info("计算秒杀订单金额，原始金额：{}，折扣后金额：{}", originalAmount, payAmount);
        return payAmount;
    }

    /**
     * 扣减库存
     *
     * @param request 订单提交请求
     * @param orderNo 订单号
     */
    @Override
    protected void deductStock(OrderSubmitRequest request, String orderNo) {
        log.info("扣减秒杀库存，订单号：{}，商品ID：{}，数量：{}",
                orderNo, request.productId(), request.quantity());
    }

    /**
     * 是否需要发送通知
     *
     * @return true 表示发送通知，false 表示不发送
     */
    @Override
    protected boolean needSendNotice() {
        return false;
    }
}
```

文件位置：`src/main/java/io/github/atengk/design/context/OrderSubmitContext.java`

下面是订单提交上下文。它负责根据订单类型选择对应模板，然后执行固定提交流程。

```java
package io.github.atengk.design.context;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.StrUtil;
import io.github.atengk.design.dto.OrderSubmitRequest;
import io.github.atengk.design.dto.OrderSubmitResponse;
import io.github.atengk.design.template.AbstractOrderSubmitTemplate;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * 订单提交上下文
 *
 * @author Ateng
 * @since 2026-04-30
 */
@Slf4j
@Component
public class OrderSubmitContext {

    private final Map<String, AbstractOrderSubmitTemplate> templateMap;

    /**
     * 创建订单提交上下文
     *
     * @param templates 订单提交模板列表
     */
    public OrderSubmitContext(List<AbstractOrderSubmitTemplate> templates) {
        if (CollUtil.isEmpty(templates)) {
            log.warn("订单提交模板列表为空");
            this.templateMap = Map.of();
            return;
        }

        this.templateMap = templates.stream()
                .collect(Collectors.toUnmodifiableMap(
                        template -> StrUtil.trim(template.supportType()).toLowerCase(),
                        Function.identity()
                ));

        log.info("初始化订单提交上下文，支持订单类型：{}", templateMap.keySet());
    }

    /**
     * 提交订单
     *
     * @param request 订单提交请求
     * @return 订单提交响应
     */
    public OrderSubmitResponse submit(OrderSubmitRequest request) {
        if (request == null) {
            log.warn("提交订单失败，请求参数为空");
            throw new IllegalArgumentException("请求参数不能为空");
        }

        if (StrUtil.isBlank(request.orderType())) {
            log.warn("提交订单失败，订单类型为空");
            throw new IllegalArgumentException("订单类型不能为空");
        }

        String orderType = StrUtil.trim(request.orderType()).toLowerCase();
        AbstractOrderSubmitTemplate template = templateMap.get(orderType);

        if (template == null) {
            log.warn("提交订单失败，不支持的订单类型：{}", request.orderType());
            throw new IllegalArgumentException("不支持的订单类型：" + request.orderType());
        }

        log.debug("匹配订单提交模板成功，订单类型：{}", orderType);
        return template.submit(request);
    }
}
```

文件位置：`src/main/java/io/github/atengk/design/controller/OrderSubmitController.java`

下面是订单提交接口，用于验证模板方法模式效果。

```java
package io.github.atengk.design.controller;

import io.github.atengk.design.context.OrderSubmitContext;
import io.github.atengk.design.dto.OrderSubmitRequest;
import io.github.atengk.design.dto.OrderSubmitResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;

/**
 * 订单提交控制器
 *
 * @author Ateng
 * @since 2026-04-30
 */
@RestController
@RequiredArgsConstructor
@RequestMapping("/template/order")
public class OrderSubmitController {

    private final OrderSubmitContext orderSubmitContext;

    /**
     * 提交订单
     *
     * @param orderType   订单类型
     * @param userId      用户ID
     * @param productId   商品ID
     * @param productName 商品名称
     * @param quantity    购买数量
     * @param unitPrice   商品单价
     * @return 订单提交响应
     */
    @PostMapping("/submit")
    public OrderSubmitResponse submit(@RequestParam String orderType,
                                      @RequestParam Long userId,
                                      @RequestParam Long productId,
                                      @RequestParam String productName,
                                      @RequestParam Integer quantity,
                                      @RequestParam BigDecimal unitPrice) {
        OrderSubmitRequest request = new OrderSubmitRequest(
                orderType,
                userId,
                productId,
                productName,
                quantity,
                unitPrice
        );
        return orderSubmitContext.submit(request);
    }
}
```

接口调用示例：

```bash
curl -X POST "http://localhost:8080/template/order/submit?orderType=normal&userId=10001&productId=20001&productName=键盘&quantity=2&unitPrice=199.00"

curl -X POST "http://localhost:8080/template/order/submit?orderType=flash_sale&userId=10002&productId=20002&productName=鼠标&quantity=1&unitPrice=99.00"
```

普通订单可能返回：

```json
{
  "orderNo": "NORMAL2019776866538487808",
  "orderType": "normal",
  "productName": "键盘",
  "quantity": 2,
  "payAmount": 398.00,
  "message": "提交成功"
}
```

秒杀订单可能返回：

```json
{
  "orderNo": "FLASH_SALE2019776866538487809",
  "orderType": "flash_sale",
  "productName": "鼠标",
  "quantity": 1,
  "payAmount": 79.20,
  "message": "提交成功"
}
```

这种方式的优点是主流程稳定，子类只负责差异化步骤。后续新增预售订单、拼团订单、积分订单时，只需要新增一个模板子类。

## 扩展一个新模板

在 Spring Boot 模板方法模式中，新增业务类型通常只需要新增一个模板子类。下面以预售订单为例，预售订单只收取 20% 定金，并使用预售库存扣减逻辑。

文件位置：`src/main/java/io/github/atengk/design/template/PresaleOrderSubmitTemplate.java`

下面的实现类会被 Spring 自动扫描，并自动加入 `OrderSubmitContext` 的模板列表。

```java
package io.github.atengk.design.template;

import cn.hutool.core.util.NumberUtil;
import io.github.atengk.design.dto.OrderSubmitRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.RoundingMode;

/**
 * 预售订单提交模板
 *
 * @author Ateng
 * @since 2026-04-30
 */
@Slf4j
@Component
public class PresaleOrderSubmitTemplate extends AbstractOrderSubmitTemplate {

    private static final BigDecimal DEPOSIT_RATE = BigDecimal.valueOf(0.2);

    /**
     * 获取支持的订单类型
     *
     * @return 订单类型
     */
    @Override
    public String supportType() {
        return "presale";
    }

    /**
     * 计算金额前置处理
     *
     * @param request 订单提交请求
     * @param orderNo 订单号
     */
    @Override
    protected void beforeCalculateAmount(OrderSubmitRequest request, String orderNo) {
        log.info("校验预售活动状态，订单号：{}，商品ID：{}", orderNo, request.productId());
    }

    /**
     * 计算应付金额
     *
     * @param request 订单提交请求
     * @return 应付金额
     */
    @Override
    protected BigDecimal calculateAmount(OrderSubmitRequest request) {
        BigDecimal originalAmount = NumberUtil.mul(request.unitPrice(), BigDecimal.valueOf(request.quantity()));
        BigDecimal depositAmount = NumberUtil.mul(originalAmount, DEPOSIT_RATE).setScale(2, RoundingMode.HALF_UP);

        log.info("计算预售订单定金，原始金额：{}，定金金额：{}", originalAmount, depositAmount);
        return depositAmount;
    }

    /**
     * 扣减库存
     *
     * @param request 订单提交请求
     * @param orderNo 订单号
     */
    @Override
    protected void deductStock(OrderSubmitRequest request, String orderNo) {
        log.info("锁定预售库存，订单号：{}，商品ID：{}，数量：{}",
                orderNo, request.productId(), request.quantity());
    }
}
```

调用示例：

```bash
curl -X POST "http://localhost:8080/template/order/submit?orderType=presale&userId=10003&productId=20003&productName=显示器&quantity=1&unitPrice=1299.00"
```

可能返回：

```json
{
  "orderNo": "PRESALE2019776866538487810",
  "orderType": "presale",
  "productName": "显示器",
  "quantity": 1,
  "payAmount": 259.80,
  "message": "提交成功"
}
```

新增预售订单模板后，原有的 `OrderSubmitController`、`OrderSubmitContext`、普通订单模板和秒杀订单模板都不需要修改。

## 钩子方法

钩子方法是模板方法模式中非常常见的扩展点。它允许父类在固定流程中预留可选分支，让子类决定是否执行某个步骤。

在上面的订单提交模板中，`needSendNotice` 就是钩子方法：

```java
protected boolean needSendNotice() {
    return true;
}
```

父类在模板方法中根据钩子方法决定是否发送通知：

```java
if (needSendNotice()) {
    sendNotice(request, orderNo);
}
```

普通订单不覆盖该方法，因此默认发送通知。秒杀订单覆盖该方法并返回 `false`，因此不发送通知。

```java
@Override
protected boolean needSendNotice() {
    return false;
}
```

钩子方法适合处理“流程整体固定，但某些步骤是否执行由子类决定”的场景。例如是否发送通知、是否记录审计日志、是否异步处理、是否执行补偿逻辑等。

## 模板方法模式和策略模式的区别

模板方法模式和策略模式都能减少重复代码，但二者关注点不同。

| 对比项     | 模板方法模式           | 策略模式               |
| ---------- | ---------------------- | ---------------------- |
| 关注点     | 固定流程，变化步骤     | 替换算法，选择行为     |
| 实现方式   | 继承抽象类             | 实现接口               |
| 主流程位置 | 父类中                 | 调用方或上下文中       |
| 变化点     | 子类覆盖步骤方法       | 不同策略类实现接口     |
| 适合场景   | 流程稳定、局部变化     | 算法可替换、流程不固定 |
| 常见组合   | 模板方法 + Spring Bean | 策略 + 工厂            |

简单理解：

```text
模板方法模式：先做 A，再做 B，再做 C，其中 B 的细节不同。
策略模式：我要做某件事，但具体用哪种算法不确定。
```

订单提交、文件导入、支付回调这类“流程固定”的场景更适合模板方法模式。优惠计算、物流计费、规则判断这类“算法可替换”的场景更适合策略模式。

## 验证方式

启动 Spring Boot 项目：

```bash
mvn spring-boot:run
```

执行普通订单提交：

```bash
curl -X POST "http://localhost:8080/template/order/submit?orderType=normal&userId=10001&productId=20001&productName=键盘&quantity=2&unitPrice=199.00"
```

执行秒杀订单提交：

```bash
curl -X POST "http://localhost:8080/template/order/submit?orderType=flash_sale&userId=10002&productId=20002&productName=鼠标&quantity=1&unitPrice=99.00"
```

执行预售订单提交：

```bash
curl -X POST "http://localhost:8080/template/order/submit?orderType=presale&userId=10003&productId=20003&productName=显示器&quantity=1&unitPrice=1299.00"
```

如果模板分发正常，可以看到类似日志：

```text
初始化订单提交上下文，支持订单类型：[normal, flash_sale, presale]
创建订单号，订单类型：normal，订单号：NORMAL2019776866538487808
扣减普通商品库存，订单号：NORMAL2019776866538487808，商品ID：20001，数量：2
保存订单，订单号：NORMAL2019776866538487808，用户ID：10001，商品ID：20001，金额：398.00
发送订单通知，订单号：NORMAL2019776866538487808，用户ID：10001
订单提交完成，订单号：NORMAL2019776866538487808，订单类型：normal，应付金额：398.00
```

如果传入不支持的订单类型：

```bash
curl -X POST "http://localhost:8080/template/order/submit?orderType=group_buy&userId=10004&productId=20004&productName=耳机&quantity=1&unitPrice=299.00"
```

会抛出异常：

```text
不支持的订单类型：group_buy
```

实际项目中建议结合全局异常处理器，将该异常转换成统一响应结构。

## 注意事项

模板方法模式依赖继承，因此不适合层级过深的复杂继承结构。一个抽象模板类最好只表达一类稳定流程，不要把多个不相关流程强行塞进同一个父类。

模板方法建议使用 `final` 修饰，避免子类覆盖主流程导致执行顺序被破坏。

推荐写法：

```java
public final OrderSubmitResponse submit(OrderSubmitRequest request) {
    validateRequest(request);
    String orderNo = createOrderNo(request);
    BigDecimal payAmount = calculateAmount(request);
    deductStock(request, orderNo);
    saveOrder(request, orderNo, payAmount);
    return buildResponse(request, orderNo, payAmount);
}
```

不推荐让子类直接覆盖主流程：

```java
public OrderSubmitResponse submit(OrderSubmitRequest request) {
    // 子类随意覆盖后，流程顺序不可控
    return null;
}
```

Spring Bean 默认是单例，模板类中不要保存请求级状态。

错误示例：

```java
private String currentOrderNo;
private Long currentUserId;
private BigDecimal currentPayAmount;
```

这些字段在并发请求下会互相污染，导致线程安全问题。

推荐将请求数据放在方法参数、局部变量、DTO 或上下文对象中。

```java
public final OrderSubmitResponse submit(OrderSubmitRequest request) {
    String orderNo = createOrderNo(request);
    BigDecimal payAmount = calculateAmount(request);
    return new OrderSubmitResponse(orderNo, supportType(), request.productName(), request.quantity(), payAmount, "提交成功");
}
```

如果模板步骤需要依赖数据库、Redis、MQ、第三方客户端，可以直接在具体模板类中注入对应的 Spring Bean。

示例：

```java
@RequiredArgsConstructor
@Component
public class NormalOrderSubmitTemplate extends AbstractOrderSubmitTemplate {

    private final StockService stockService;

    @Override
    public String supportType() {
        return "normal";
    }

    @Override
    protected void deductStock(OrderSubmitRequest request, String orderNo) {
        stockService.deduct(request.productId(), request.quantity());
    }
}
```

实际项目中需要结合事务控制、幂等校验、库存一致性、异常补偿等机制，不能只依赖本地模板流程保证业务完整性。

## 总结

在 JDK21 和 Spring Boot 3 项目中，模板方法模式的实践重点是复用稳定流程，把变化步骤交给子类实现。

普通 Java 模板方法适合无依赖的流程封装。Spring Boot 模板方法适合订单提交、文件导入、支付回调、数据同步、消息消费等业务流程。对于这些场景，推荐使用“抽象模板类 + 多个具体模板类 + Spring 上下文分发”的结构。

模板方法模式不是为了替代所有分支判断，而是为了让固定业务流程有统一入口、统一顺序和统一校验，同时允许局部步骤按业务类型灵活扩展。
