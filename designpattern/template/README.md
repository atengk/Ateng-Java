# 模板方法模式

模板方法模式用于在父类中定义一个算法流程骨架，把流程中固定不变的步骤写在父类，把可变步骤延迟到子类实现。
在 Spring Boot 项目中，模板方法模式常用于批量导入、文件解析、订单处理流程、支付处理流程、任务执行流程、报表生成、数据同步、消息发送等场景。

本文以“订单批量导入处理”为例。普通订单导入和退款订单导入都有固定流程：校验请求、解析行数据、逐行校验、逐行处理、统计结果、输出日志。不同导入类型只需要实现自己的业务校验和保存逻辑。

## 适用场景

模板方法模式适合处理“流程固定，但部分步骤有差异”的场景。

订单批量导入中，普通订单和退款订单都有类似处理流程：

| 步骤       | 普通订单导入                 | 退款订单导入                   |
| ---------- | ---------------------------- | ------------------------------ |
| 校验请求   | 校验导入类型、操作人、数据行 | 校验导入类型、操作人、数据行   |
| 解析行数据 | 解析用户、商品、数量、金额   | 解析订单号、退款金额、退款原因 |
| 业务校验   | 校验用户、商品、金额         | 校验订单号、退款金额、原因     |
| 处理数据   | 保存订单记录                 | 保存退款记录                   |
| 统计结果   | 统计成功和失败行             | 统计成功和失败行               |

如果每种导入类型都完整写一遍流程，会产生大量重复代码。模板方法模式可以把通用流程放在抽象类中，子类只实现差异化步骤。

## 基础配置

本示例基于 Spring Boot 3，使用 Hutool、Lombok 和 Validation。Hutool 用于集合判断、字符串判断、类型转换、金额判断和 ID 生成，Validation 用于接口参数基础校验。

文件位置：`pom.xml`

```xml
<dependencies>
    <!-- Spring Boot Web：提供 REST API 能力 -->
    <dependency>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-starter-web</artifactId>
    </dependency>

    <!-- Spring Boot Validation：用于接口参数基础校验 -->
    <dependency>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-starter-validation</artifactId>
    </dependency>

    <!-- Hutool：提供集合、字符串、类型转换、金额判断、ID生成等工具能力 -->
    <dependency>
        <groupId>cn.hutool</groupId>
        <artifactId>hutool-all</artifactId>
        <version>5.8.29</version>
    </dependency>

    <!-- Lombok：简化 Getter、Setter、构造器、日志对象等代码 -->
    <dependency>
        <groupId>org.projectlombok</groupId>
        <artifactId>lombok</artifactId>
        <optional>true</optional>
    </dependency>
</dependencies>
```

建议目录结构如下：

```text
src/main/java/io/github/atengk/pattern/template
├── TemplateApplication.java
├── common
│   ├── ApiResult.java
│   ├── BizException.java
│   └── GlobalExceptionHandler.java
└── order
    ├── controller
    │   └── OrderImportController.java
    ├── dto
    │   └── OrderImportRequest.java
    ├── model
    │   └── OrderImportRow.java
    ├── repository
    │   └── MockOrderImportRepository.java
    ├── service
    │   ├── OrderImportService.java
    │   └── impl
    │       └── OrderImportServiceImpl.java
    ├── template
    │   ├── AbstractOrderImportTemplate.java
    │   ├── OrderImportTemplateExecutor.java
    │   ├── OrderImportTypes.java
    │   └── impl
    │       ├── NormalOrderImportTemplate.java
    │       └── RefundOrderImportTemplate.java
    └── vo
        └── OrderImportResultVO.java
```

## 核心设计

本示例把模板方法模式拆成三个核心角色：

| 角色              | 项目中的类                                               | 说明                         |
| ----------------- | -------------------------------------------------------- | ---------------------------- |
| AbstractClass     | `AbstractOrderImportTemplate`                            | 定义订单导入流程骨架         |
| ConcreteClass     | `NormalOrderImportTemplate`、`RefundOrderImportTemplate` | 实现不同导入类型的差异步骤   |
| Client / Executor | `OrderImportTemplateExecutor`                            | 根据导入类型选择具体模板执行 |

执行流程如下：

```text
Controller
  -> OrderImportService
    -> OrderImportTemplateExecutor
      -> 根据 importType 获取具体导入模板
        -> AbstractOrderImportTemplate.importData()
          -> checkRequest()
          -> parseRows()
          -> beforeImport()
          -> validateRow()
          -> processRow()
          -> afterImport()
          -> buildResult()
```

模板方法模式的重点是：父类控制完整流程，子类只负责可变部分。这样既能复用通用流程，又能保留不同导入业务的扩展能力。

## 公共代码

公共响应对象、业务异常和全局异常处理用于统一接口返回。实际项目中可以复用已有基础包。

文件位置：`src/main/java/io/github/atengk/pattern/template/common/ApiResult.java`

```java
package io.github.atengk.pattern.template.common;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 统一接口响应对象
 *
 * @author Ateng
 * @since 2026-05-13
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ApiResult<T> {

    private Integer code;

    private String message;

    private T data;

    /**
     * 返回成功结果
     *
     * @param data 响应数据
     * @return 统一响应对象
     */
    public static <T> ApiResult<T> success(T data) {
        return new ApiResult<>(200, "操作成功", data);
    }

    /**
     * 返回失败结果
     *
     * @param message 错误信息
     * @return 统一响应对象
     */
    public static <T> ApiResult<T> fail(String message) {
        return new ApiResult<>(500, message, null);
    }

}
```

文件位置：`src/main/java/io/github/atengk/pattern/template/common/BizException.java`

```java
package io.github.atengk.pattern.template.common;

/**
 * 业务异常
 *
 * @author Ateng
 * @since 2026-05-13
 */
public class BizException extends RuntimeException {

    /**
     * 创建业务异常
     *
     * @param message 异常信息
     */
    public BizException(String message) {
        super(message);
    }

}
```

文件位置：`src/main/java/io/github/atengk/pattern/template/common/GlobalExceptionHandler.java`

```java
package io.github.atengk.pattern.template.common;

import lombok.extern.slf4j.Slf4j;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.validation.BindException;
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
     * 处理业务异常
     *
     * @param exception 业务异常
     * @return 统一响应对象
     */
    @ExceptionHandler(BizException.class)
    public ApiResult<Void> handleBizException(BizException exception) {
        log.warn("业务处理失败：{}", exception.getMessage());
        return ApiResult.fail(exception.getMessage());
    }

    /**
     * 处理参数校验异常
     *
     * @param exception 参数校验异常
     * @return 统一响应对象
     */
    @ExceptionHandler({MethodArgumentNotValidException.class, BindException.class})
    public ApiResult<Void> handleValidException(Exception exception) {
        log.warn("接口参数校验失败：{}", exception.getMessage());
        return ApiResult.fail("请求参数不合法");
    }

    /**
     * 处理请求体解析异常
     *
     * @param exception 请求体解析异常
     * @return 统一响应对象
     */
    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ApiResult<Void> handleMessageNotReadableException(HttpMessageNotReadableException exception) {
        log.warn("请求体解析失败：{}", exception.getMessage());
        return ApiResult.fail("请求体格式不正确");
    }

    /**
     * 处理系统异常
     *
     * @param exception 系统异常
     * @return 统一响应对象
     */
    @ExceptionHandler(Exception.class)
    public ApiResult<Void> handleException(Exception exception) {
        log.error("系统异常", exception);
        return ApiResult.fail("系统繁忙，请稍后重试");
    }

}
```

## 完整代码

下面给出模板方法模式的核心代码。示例使用接口请求中的 `rows` 模拟批量导入数据，实际项目中可以把 `rows` 替换为 Excel、CSV、对象存储文件或消息数据。

文件位置：`src/main/java/io/github/atengk/pattern/template/TemplateApplication.java`

```java
package io.github.atengk.pattern.template;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * 模板方法模式示例启动类
 *
 * @author Ateng
 * @since 2026-05-13
 */
@SpringBootApplication
public class TemplateApplication {

    public static void main(String[] args) {
        SpringApplication.run(TemplateApplication.class, args);
    }

}
```

## 请求对象和响应对象

请求对象用于承载导入类型、操作人和导入行数据。为了简化示例，行数据使用 `Map<String, Object>` 表示。实际项目中可以来自 Excel 行、CSV 行或中间 DTO。

文件位置：`src/main/java/io/github/atengk/pattern/template/order/dto/OrderImportRequest.java`

```java
package io.github.atengk.pattern.template.order.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.util.List;
import java.util.Map;

/**
 * 订单导入请求参数
 *
 * @author Ateng
 * @since 2026-05-13
 */
@Data
public class OrderImportRequest {

    @NotBlank(message = "导入类型不能为空")
    private String importType;

    @NotNull(message = "操作人ID不能为空")
    private Long operatorId;

    @NotEmpty(message = "导入数据不能为空")
    private List<Map<String, Object>> rows;

}
```

响应对象用于返回导入统计结果。

文件位置：`src/main/java/io/github/atengk/pattern/template/order/vo/OrderImportResultVO.java`

```java
package io.github.atengk.pattern.template.order.vo;

import lombok.Builder;
import lombok.Data;

import java.util.List;

/**
 * 订单导入结果
 *
 * @author Ateng
 * @since 2026-05-13
 */
@Data
@Builder
public class OrderImportResultVO {

    private String importType;

    private String importName;

    private Integer totalCount;

    private Integer successCount;

    private Integer failCount;

    private List<String> errorMessages;

}
```

## 导入行模型

导入行模型是模板流程内部使用的统一数据结构。普通订单导入和退款订单导入都可以从中取自己需要的字段。

文件位置：`src/main/java/io/github/atengk/pattern/template/order/model/OrderImportRow.java`

```java
package io.github.atengk.pattern.template.order.model;

import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;

/**
 * 订单导入行数据
 *
 * @author Ateng
 * @since 2026-05-13
 */
@Data
@Builder
public class OrderImportRow {

    private Integer rowNo;

    private Long userId;

    private Long productId;

    private Integer quantity;

    private BigDecimal amount;

    private String orderNo;

    private BigDecimal refundAmount;

    private String refundReason;

}
```

## 模拟仓储

仓储类用于模拟订单导入后的持久化操作。实际项目中可以替换为 MyBatis-Plus Mapper、JPA Repository 或批量写入服务。

文件位置：`src/main/java/io/github/atengk/pattern/template/order/repository/MockOrderImportRepository.java`

```java
package io.github.atengk.pattern.template.order.repository;

import cn.hutool.core.util.IdUtil;
import io.github.atengk.pattern.template.order.model.OrderImportRow;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * 模拟订单导入仓储
 *
 * @author Ateng
 * @since 2026-05-13
 */
@Slf4j
@Repository
public class MockOrderImportRepository {

    private final List<String> normalOrderStorage = new CopyOnWriteArrayList<>();

    private final List<String> refundOrderStorage = new CopyOnWriteArrayList<>();

    /**
     * 保存普通订单导入记录
     *
     * @param row 导入行数据
     * @return 订单编号
     */
    public String saveNormalOrder(OrderImportRow row) {
        String orderNo = "OD" + IdUtil.getSnowflakeNextIdStr();
        normalOrderStorage.add(orderNo);

        log.info("普通订单导入保存成功，rowNo：{}，orderNo：{}，userId：{}，productId：{}",
                row.getRowNo(), orderNo, row.getUserId(), row.getProductId());

        return orderNo;
    }

    /**
     * 保存退款订单导入记录
     *
     * @param row 导入行数据
     * @return 退款单号
     */
    public String saveRefundOrder(OrderImportRow row) {
        String refundNo = "RF" + IdUtil.getSnowflakeNextIdStr();
        refundOrderStorage.add(refundNo);

        log.info("退款订单导入保存成功，rowNo：{}，refundNo：{}，orderNo：{}，refundAmount：{}",
                row.getRowNo(), refundNo, row.getOrderNo(), row.getRefundAmount());

        return refundNo;
    }

}
```

## 导入类型常量

导入类型用于选择具体模板类。导入类型可能被前端、运营配置或任务调度依赖，应保持稳定。

文件位置：`src/main/java/io/github/atengk/pattern/template/order/template/OrderImportTypes.java`

```java
package io.github.atengk.pattern.template.order.template;

/**
 * 订单导入类型常量
 *
 * @author Ateng
 * @since 2026-05-13
 */
public final class OrderImportTypes {

    public static final String NORMAL_ORDER = "NORMAL_ORDER";

    public static final String REFUND_ORDER = "REFUND_ORDER";

    private OrderImportTypes() {
    }

}
```

## 抽象模板类

抽象模板类是模板方法模式的核心。`importData` 方法定义完整导入流程，并使用 `final` 修饰，避免子类破坏流程顺序。
子类只能实现 `validateRow` 和 `processRow` 等可变步骤。

文件位置：`src/main/java/io/github/atengk/pattern/template/order/template/AbstractOrderImportTemplate.java`

```java
package io.github.atengk.pattern.template.order.template;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.convert.Convert;
import cn.hutool.core.map.MapUtil;
import cn.hutool.core.util.NumberUtil;
import cn.hutool.core.util.StrUtil;
import io.github.atengk.pattern.template.common.BizException;
import io.github.atengk.pattern.template.order.dto.OrderImportRequest;
import io.github.atengk.pattern.template.order.model.OrderImportRow;
import io.github.atengk.pattern.template.order.vo.OrderImportResultVO;
import lombok.extern.slf4j.Slf4j;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * 订单导入抽象模板
 *
 * @author Ateng
 * @since 2026-05-13
 */
@Slf4j
public abstract class AbstractOrderImportTemplate {

    /**
     * 返回导入类型
     *
     * @return 导入类型
     */
    public abstract String importType();

    /**
     * 返回导入名称
     *
     * @return 导入名称
     */
    public abstract String importName();

    /**
     * 导入数据模板方法
     *
     * @param request 导入请求
     * @return 导入结果
     */
    public final OrderImportResultVO importData(OrderImportRequest request) {
        checkRequest(request);

        log.info("开始执行订单导入任务，importType：{}，importName：{}，operatorId：{}",
                importType(), importName(), request.getOperatorId());

        List<OrderImportRow> rows = parseRows(request);
        beforeImport(request, rows);

        int successCount = 0;
        List<String> errorMessages = new ArrayList<>();

        for (OrderImportRow row : rows) {
            try {
                validateRow(row);
                processRow(row);
                successCount++;
            } catch (Exception exception) {
                String errorMessage = StrUtil.format("第{}行导入失败：{}", row.getRowNo(), exception.getMessage());
                errorMessages.add(errorMessage);
                log.warn(errorMessage);
            }
        }

        afterImport(request, rows, successCount, errorMessages);

        OrderImportResultVO result = OrderImportResultVO.builder()
                .importType(importType())
                .importName(importName())
                .totalCount(rows.size())
                .successCount(successCount)
                .failCount(errorMessages.size())
                .errorMessages(errorMessages)
                .build();

        log.info("订单导入任务执行完成，importType：{}，totalCount：{}，successCount：{}，failCount：{}",
                importType(), result.getTotalCount(), result.getSuccessCount(), result.getFailCount());

        return result;
    }

    /**
     * 校验导入请求
     *
     * @param request 导入请求
     */
    protected void checkRequest(OrderImportRequest request) {
        if (request == null) {
            throw new BizException("导入请求不能为空");
        }

        if (StrUtil.isBlank(request.getImportType())) {
            throw new BizException("导入类型不能为空");
        }

        if (request.getOperatorId() == null || request.getOperatorId() <= 0) {
            throw new BizException("操作人ID不合法");
        }

        if (CollUtil.isEmpty(request.getRows())) {
            throw new BizException("导入数据不能为空");
        }
    }

    /**
     * 解析导入行数据
     *
     * @param request 导入请求
     * @return 导入行数据集合
     */
    protected List<OrderImportRow> parseRows(OrderImportRequest request) {
        List<OrderImportRow> result = new ArrayList<>();

        for (int index = 0; index < request.getRows().size(); index++) {
            Map<String, Object> rowMap = request.getRows().get(index);

            OrderImportRow row = OrderImportRow.builder()
                    .rowNo(index + 1)
                    .userId(Convert.toLong(MapUtil.get(rowMap, "userId", Object.class), null))
                    .productId(Convert.toLong(MapUtil.get(rowMap, "productId", Object.class), null))
                    .quantity(Convert.toInt(MapUtil.get(rowMap, "quantity", Object.class), null))
                    .amount(toBigDecimal(MapUtil.get(rowMap, "amount", Object.class)))
                    .orderNo(Convert.toStr(MapUtil.get(rowMap, "orderNo", Object.class), null))
                    .refundAmount(toBigDecimal(MapUtil.get(rowMap, "refundAmount", Object.class)))
                    .refundReason(Convert.toStr(MapUtil.get(rowMap, "refundReason", Object.class), null))
                    .build();

            result.add(row);
        }

        log.info("导入行数据解析完成，importType：{}，rowCount：{}", importType(), result.size());
        return result;
    }

    /**
     * 导入前置钩子
     *
     * @param request 导入请求
     * @param rows 导入行数据集合
     */
    protected void beforeImport(OrderImportRequest request, List<OrderImportRow> rows) {
        log.info("执行导入前置处理，importType：{}，rowCount：{}", importType(), rows.size());
    }

    /**
     * 校验单行导入数据
     *
     * @param row 导入行数据
     */
    protected abstract void validateRow(OrderImportRow row);

    /**
     * 处理单行导入数据
     *
     * @param row 导入行数据
     */
    protected abstract void processRow(OrderImportRow row);

    /**
     * 导入后置钩子
     *
     * @param request 导入请求
     * @param rows 导入行数据集合
     * @param successCount 成功数量
     * @param errorMessages 错误信息集合
     */
    protected void afterImport(OrderImportRequest request,
                               List<OrderImportRow> rows,
                               Integer successCount,
                               List<String> errorMessages) {
        log.info("执行导入后置处理，importType：{}，successCount：{}，failCount：{}",
                importType(), successCount, errorMessages.size());
    }

    /**
     * 转换金额
     *
     * @param value 原始值
     * @return 金额
     */
    private BigDecimal toBigDecimal(Object value) {
        if (value == null) {
            return null;
        }

        BigDecimal amount = Convert.toBigDecimal(value, null);
        if (amount == null || NumberUtil.isLess(amount, BigDecimal.ZERO)) {
            return amount;
        }

        return amount;
    }

}
```

## 具体模板类

普通订单导入模板只实现普通订单的校验和保存逻辑，不需要重复编写导入流程。

文件位置：`src/main/java/io/github/atengk/pattern/template/order/template/impl/NormalOrderImportTemplate.java`

```java
package io.github.atengk.pattern.template.order.template.impl;

import cn.hutool.core.util.NumberUtil;
import cn.hutool.core.util.ObjectUtil;
import io.github.atengk.pattern.template.common.BizException;
import io.github.atengk.pattern.template.order.model.OrderImportRow;
import io.github.atengk.pattern.template.order.repository.MockOrderImportRepository;
import io.github.atengk.pattern.template.order.template.AbstractOrderImportTemplate;
import io.github.atengk.pattern.template.order.template.OrderImportTypes;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;

/**
 * 普通订单导入模板
 *
 * @author Ateng
 * @since 2026-05-13
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class NormalOrderImportTemplate extends AbstractOrderImportTemplate {

    private final MockOrderImportRepository orderImportRepository;

    /**
     * 返回导入类型
     *
     * @return 导入类型
     */
    @Override
    public String importType() {
        return OrderImportTypes.NORMAL_ORDER;
    }

    /**
     * 返回导入名称
     *
     * @return 导入名称
     */
    @Override
    public String importName() {
        return "普通订单导入";
    }

    /**
     * 校验普通订单导入行
     *
     * @param row 导入行数据
     */
    @Override
    protected void validateRow(OrderImportRow row) {
        if (ObjectUtil.isNull(row.getUserId()) || row.getUserId() <= 0) {
            throw new BizException("用户ID不合法");
        }

        if (ObjectUtil.isNull(row.getProductId()) || row.getProductId() <= 0) {
            throw new BizException("商品ID不合法");
        }

        if (ObjectUtil.isNull(row.getQuantity()) || row.getQuantity() <= 0) {
            throw new BizException("购买数量必须大于0");
        }

        if (ObjectUtil.isNull(row.getAmount()) || NumberUtil.isLessOrEqual(row.getAmount(), BigDecimal.ZERO)) {
            throw new BizException("订单金额必须大于0");
        }
    }

    /**
     * 处理普通订单导入行
     *
     * @param row 导入行数据
     */
    @Override
    protected void processRow(OrderImportRow row) {
        String orderNo = orderImportRepository.saveNormalOrder(row);
        log.info("普通订单导入行处理完成，rowNo：{}，orderNo：{}", row.getRowNo(), orderNo);
    }

}
```

退款订单导入模板只实现退款订单的校验和保存逻辑。

文件位置：`src/main/java/io/github/atengk/pattern/template/order/template/impl/RefundOrderImportTemplate.java`

```java
package io.github.atengk.pattern.template.order.template.impl;

import cn.hutool.core.util.NumberUtil;
import cn.hutool.core.util.ObjectUtil;
import cn.hutool.core.util.StrUtil;
import io.github.atengk.pattern.template.common.BizException;
import io.github.atengk.pattern.template.order.model.OrderImportRow;
import io.github.atengk.pattern.template.order.repository.MockOrderImportRepository;
import io.github.atengk.pattern.template.order.template.AbstractOrderImportTemplate;
import io.github.atengk.pattern.template.order.template.OrderImportTypes;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;

/**
 * 退款订单导入模板
 *
 * @author Ateng
 * @since 2026-05-13
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class RefundOrderImportTemplate extends AbstractOrderImportTemplate {

    private final MockOrderImportRepository orderImportRepository;

    /**
     * 返回导入类型
     *
     * @return 导入类型
     */
    @Override
    public String importType() {
        return OrderImportTypes.REFUND_ORDER;
    }

    /**
     * 返回导入名称
     *
     * @return 导入名称
     */
    @Override
    public String importName() {
        return "退款订单导入";
    }

    /**
     * 校验退款订单导入行
     *
     * @param row 导入行数据
     */
    @Override
    protected void validateRow(OrderImportRow row) {
        if (StrUtil.isBlank(row.getOrderNo())) {
            throw new BizException("订单编号不能为空");
        }

        if (ObjectUtil.isNull(row.getRefundAmount())
                || NumberUtil.isLessOrEqual(row.getRefundAmount(), BigDecimal.ZERO)) {
            throw new BizException("退款金额必须大于0");
        }

        if (StrUtil.isBlank(row.getRefundReason())) {
            throw new BizException("退款原因不能为空");
        }
    }

    /**
     * 处理退款订单导入行
     *
     * @param row 导入行数据
     */
    @Override
    protected void processRow(OrderImportRow row) {
        String refundNo = orderImportRepository.saveRefundOrder(row);
        log.info("退款订单导入行处理完成，rowNo：{}，refundNo：{}", row.getRowNo(), refundNo);
    }

}
```

## 模板执行器

模板执行器负责从 Spring 容器中收集所有导入模板，并根据 `importType` 选择具体模板。
这部分不是模板方法模式本身的核心，但在 Spring Boot 项目中很常用。

文件位置：`src/main/java/io/github/atengk/pattern/template/order/template/OrderImportTemplateExecutor.java`

```java
package io.github.atengk.pattern.template.order.template;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.StrUtil;
import io.github.atengk.pattern.template.common.BizException;
import io.github.atengk.pattern.template.order.dto.OrderImportRequest;
import io.github.atengk.pattern.template.order.vo.OrderImportResultVO;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 订单导入模板执行器
 *
 * @author Ateng
 * @since 2026-05-13
 */
@Slf4j
@Component
public class OrderImportTemplateExecutor {

    private final Map<String, AbstractOrderImportTemplate> templateMap;

    /**
     * 初始化订单导入模板执行器
     *
     * @param templates Spring 容器中的订单导入模板集合
     */
    public OrderImportTemplateExecutor(List<AbstractOrderImportTemplate> templates) {
        if (CollUtil.isEmpty(templates)) {
            this.templateMap = Collections.emptyMap();
            log.warn("订单导入模板执行器未加载到任何模板");
            return;
        }

        Map<String, AbstractOrderImportTemplate> registerMap = new HashMap<>(templates.size());
        for (AbstractOrderImportTemplate template : templates) {
            String importType = StrUtil.upperCase(template.importType());
            if (registerMap.containsKey(importType)) {
                throw new IllegalStateException("订单导入类型重复注册：" + importType);
            }

            registerMap.put(importType, template);
            log.info("注册订单导入模板，importType：{}，importName：{}", importType, template.importName());
        }

        this.templateMap = Collections.unmodifiableMap(registerMap);
    }

    /**
     * 执行订单导入
     *
     * @param request 导入请求
     * @return 导入结果
     */
    public OrderImportResultVO execute(OrderImportRequest request) {
        String importType = StrUtil.upperCase(request.getImportType());
        AbstractOrderImportTemplate template = templateMap.get(importType);

        if (template == null) {
            throw new BizException(StrUtil.format("不支持的订单导入类型：{}", request.getImportType()));
        }

        return template.importData(request);
    }

}
```

## 业务服务

业务服务负责接收导入请求并调用模板执行器。业务服务不直接关心每种导入类型的具体流程。

文件位置：`src/main/java/io/github/atengk/pattern/template/order/service/OrderImportService.java`

```java
package io.github.atengk.pattern.template.order.service;

import io.github.atengk.pattern.template.order.dto.OrderImportRequest;
import io.github.atengk.pattern.template.order.vo.OrderImportResultVO;

/**
 * 订单导入服务
 *
 * @author Ateng
 * @since 2026-05-13
 */
public interface OrderImportService {

    /**
     * 导入订单数据
     *
     * @param request 导入请求
     * @return 导入结果
     */
    OrderImportResultVO importOrders(OrderImportRequest request);

}
```

文件位置：`src/main/java/io/github/atengk/pattern/template/order/service/impl/OrderImportServiceImpl.java`

```java
package io.github.atengk.pattern.template.order.service.impl;

import io.github.atengk.pattern.template.order.dto.OrderImportRequest;
import io.github.atengk.pattern.template.order.service.OrderImportService;
import io.github.atengk.pattern.template.order.template.OrderImportTemplateExecutor;
import io.github.atengk.pattern.template.order.vo.OrderImportResultVO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

/**
 * 订单导入服务实现类
 *
 * @author Ateng
 * @since 2026-05-13
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class OrderImportServiceImpl implements OrderImportService {

    private final OrderImportTemplateExecutor orderImportTemplateExecutor;

    /**
     * 导入订单数据
     *
     * @param request 导入请求
     * @return 导入结果
     */
    @Override
    public OrderImportResultVO importOrders(OrderImportRequest request) {
        log.info("开始订单导入服务，importType：{}，operatorId：{}",
                request.getImportType(), request.getOperatorId());

        OrderImportResultVO result = orderImportTemplateExecutor.execute(request);

        log.info("订单导入服务完成，importType：{}，successCount：{}，failCount：{}",
                result.getImportType(), result.getSuccessCount(), result.getFailCount());

        return result;
    }

}
```

## 控制器接口

控制器提供统一导入入口。调用方通过 `importType` 指定导入类型，系统自动选择对应模板。

文件位置：`src/main/java/io/github/atengk/pattern/template/order/controller/OrderImportController.java`

```java
package io.github.atengk.pattern.template.order.controller;

import io.github.atengk.pattern.template.common.ApiResult;
import io.github.atengk.pattern.template.order.dto.OrderImportRequest;
import io.github.atengk.pattern.template.order.service.OrderImportService;
import io.github.atengk.pattern.template.order.vo.OrderImportResultVO;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

/**
 * 订单导入接口
 *
 * @author Ateng
 * @since 2026-05-13
 */
@RestController
@RequestMapping("/order-imports")
@RequiredArgsConstructor
public class OrderImportController {

    private final OrderImportService orderImportService;

    /**
     * 导入订单数据
     *
     * @param request 导入请求
     * @return 导入结果
     */
    @PostMapping
    public ApiResult<OrderImportResultVO> importOrders(@Valid @RequestBody OrderImportRequest request) {
        return ApiResult.success(orderImportService.importOrders(request));
    }

}
```

## 使用方式

启动项目后，调用统一导入接口即可触发模板方法模式。

接口信息：

| 项目         | 内容                 |
| ------------ | -------------------- |
| 请求路径     | `/order-imports`     |
| 请求方法     | `POST`               |
| Content-Type | `application/json`   |
| 核心字段     | `importType`、`rows` |

普通订单导入请求：

```bash
curl -X POST "http://localhost:8080/order-imports" \
  -H "Content-Type: application/json" \
  -d '{
    "importType": "NORMAL_ORDER",
    "operatorId": 10001,
    "rows": [
      {
        "userId": 10001,
        "productId": 20001,
        "quantity": 2,
        "amount": 199.90
      },
      {
        "userId": 10002,
        "productId": 20002,
        "quantity": 1,
        "amount": 59.90
      },
      {
        "userId": null,
        "productId": 20003,
        "quantity": 1,
        "amount": 89.90
      }
    ]
  }'
```

返回示例：

```json
{
  "code": 200,
  "message": "操作成功",
  "data": {
    "importType": "NORMAL_ORDER",
    "importName": "普通订单导入",
    "totalCount": 3,
    "successCount": 2,
    "failCount": 1,
    "errorMessages": [
      "第3行导入失败：用户ID不合法"
    ]
  }
}
```

退款订单导入请求：

```bash
curl -X POST "http://localhost:8080/order-imports" \
  -H "Content-Type: application/json" \
  -d '{
    "importType": "REFUND_ORDER",
    "operatorId": 10001,
    "rows": [
      {
        "orderNo": "OD1998948715737427968",
        "refundAmount": 99.90,
        "refundReason": "商品质量问题"
      },
      {
        "orderNo": "OD1998948715737427969",
        "refundAmount": 59.90,
        "refundReason": "用户申请退款"
      },
      {
        "orderNo": "OD1998948715737427970",
        "refundAmount": 0,
        "refundReason": "金额错误示例"
      }
    ]
  }'
```

返回示例：

```json
{
  "code": 200,
  "message": "操作成功",
  "data": {
    "importType": "REFUND_ORDER",
    "importName": "退款订单导入",
    "totalCount": 3,
    "successCount": 2,
    "failCount": 1,
    "errorMessages": [
      "第3行导入失败：退款金额必须大于0"
    ]
  }
}
```

不支持的导入类型：

```bash
curl -X POST "http://localhost:8080/order-imports" \
  -H "Content-Type: application/json" \
  -d '{
    "importType": "UNKNOWN_ORDER",
    "operatorId": 10001,
    "rows": [
      {
        "userId": 10001
      }
    ]
  }'
```

返回示例：

```json
{
  "code": 500,
  "message": "不支持的订单导入类型：UNKNOWN_ORDER",
  "data": null
}
```

## 验证方式

项目启动后，可以看到模板注册日志：

```text
注册订单导入模板，importType：NORMAL_ORDER，importName：普通订单导入
注册订单导入模板，importType：REFUND_ORDER，importName：退款订单导入
```

执行普通订单导入后，可以看到模板流程日志：

```text
开始订单导入服务，importType：NORMAL_ORDER，operatorId：10001
开始执行订单导入任务，importType：NORMAL_ORDER，importName：普通订单导入，operatorId：10001
导入行数据解析完成，importType：NORMAL_ORDER，rowCount：3
执行导入前置处理，importType：NORMAL_ORDER，rowCount：3
普通订单导入保存成功，rowNo：1，orderNo：OD1998948715737427968，userId：10001，productId：20001
普通订单导入行处理完成，rowNo：1，orderNo：OD1998948715737427968
普通订单导入保存成功，rowNo：2，orderNo：OD1998948715737427969，userId：10002，productId：20002
普通订单导入行处理完成，rowNo：2，orderNo：OD1998948715737427969
第3行导入失败：用户ID不合法
执行导入后置处理，importType：NORMAL_ORDER，successCount：2，failCount：1
订单导入任务执行完成，importType：NORMAL_ORDER，totalCount：3，successCount：2，failCount：1
订单导入服务完成，importType：NORMAL_ORDER，successCount：2，failCount：1
```

从日志可以看出，完整流程由 `AbstractOrderImportTemplate#importData` 控制，普通订单模板只负责自己的行校验和行处理。

## 扩展方式

如果后续新增“售后订单导入”，只需要新增导入类型和模板类。

第一，新增导入类型：

```java
public static final String AFTER_SALE_ORDER = "AFTER_SALE_ORDER";
```

第二，新增具体模板类。

文件位置：`src/main/java/io/github/atengk/pattern/template/order/template/impl/AfterSaleOrderImportTemplate.java`

```java
package io.github.atengk.pattern.template.order.template.impl;

import cn.hutool.core.util.StrUtil;
import io.github.atengk.pattern.template.common.BizException;
import io.github.atengk.pattern.template.order.model.OrderImportRow;
import io.github.atengk.pattern.template.order.template.AbstractOrderImportTemplate;
import io.github.atengk.pattern.template.order.template.OrderImportTypes;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * 售后订单导入模板
 *
 * @author Ateng
 * @since 2026-05-13
 */
@Slf4j
@Component
public class AfterSaleOrderImportTemplate extends AbstractOrderImportTemplate {

    /**
     * 返回导入类型
     *
     * @return 导入类型
     */
    @Override
    public String importType() {
        return OrderImportTypes.AFTER_SALE_ORDER;
    }

    /**
     * 返回导入名称
     *
     * @return 导入名称
     */
    @Override
    public String importName() {
        return "售后订单导入";
    }

    /**
     * 校验售后订单导入行
     *
     * @param row 导入行数据
     */
    @Override
    protected void validateRow(OrderImportRow row) {
        if (StrUtil.isBlank(row.getOrderNo())) {
            throw new BizException("订单编号不能为空");
        }

        if (StrUtil.isBlank(row.getRefundReason())) {
            throw new BizException("售后原因不能为空");
        }
    }

    /**
     * 处理售后订单导入行
     *
     * @param row 导入行数据
     */
    @Override
    protected void processRow(OrderImportRow row) {
        log.info("售后订单导入行处理完成，rowNo：{}，orderNo：{}，reason：{}",
                row.getRowNo(), row.getOrderNo(), row.getRefundReason());
    }

}
```

新增模板类后，Spring 会自动注入到 `List<AbstractOrderImportTemplate>` 中，执行器会自动完成注册，不需要修改原有导入流程。

## 结合 Excel 文件导入

实际项目中，导入数据通常来自 Excel 文件。此时可以保留模板方法结构，只调整 `parseRows` 的数据来源。

常见做法是：

| 层级             | 职责                           |
| ---------------- | ------------------------------ |
| Controller       | 接收 `MultipartFile`           |
| Service          | 保存文件、创建导入任务         |
| Template         | 定义导入流程                   |
| Parser           | 解析 Excel 为 `OrderImportRow` |
| ConcreteTemplate | 校验并处理具体业务行           |

如果使用 EasyExcel，可以把抽象模板中的 `parseRows` 改成调用独立解析器：

```java
protected List<OrderImportRow> parseRows(OrderImportRequest request) {
    return orderImportExcelParser.parse(request.getFileUrl());
}
```

模板方法模式关注的是“流程骨架”。数据来源可以是接口 JSON、Excel、CSV、MQ 消息或数据库临时表，不影响模式结构。

## 优点和注意事项

模板方法模式的核心价值是复用固定流程，并把差异步骤交给子类实现。

| 注意事项                 | 说明                                             |
| ------------------------ | ------------------------------------------------ |
| 模板方法建议使用 `final` | 防止子类重写主流程，破坏流程顺序                 |
| 抽象类不要过重           | 父类只放通用流程和通用逻辑，不要塞入所有业务细节 |
| 钩子方法要谨慎设计       | `beforeImport`、`afterImport` 适合扩展非核心步骤 |
| 子类只实现差异点         | 不要在子类中重复模板类已有流程                   |
| 流程变化大时不适合       | 如果不同业务流程差异很大，模板方法会变得牵强     |
| 可以结合策略或工厂       | 多模板选择时，常结合注册器、工厂或策略执行器使用 |

## 和策略模式的区别

模板方法模式和策略模式都能减少重复代码，但关注点不同。

| 模式         | 关注点                             | 典型场景                                       |
| ------------ | ---------------------------------- | ---------------------------------------------- |
| 模板方法模式 | 固定流程骨架，子类实现部分步骤     | 导入流程、导出流程、任务执行流程、支付处理流程 |
| 策略模式     | 多种算法可替换，运行时选择一种执行 | 优惠计算、支付渠道、运费计算、导出格式         |

模板方法模式强调“流程固定，步骤可变”。
策略模式强调“算法可替换，流程通常由调用方控制”。

## 和责任链模式的区别

模板方法模式和责任链模式都可能包含多个步骤，但组织方式不同。

| 模式         | 关注点                       | 典型场景                               |
| ------------ | ---------------------------- | -------------------------------------- |
| 模板方法模式 | 父类定义固定步骤顺序         | 导入、导出、任务执行、流程处理         |
| 责任链模式   | 多个处理器按链路顺序处理请求 | 参数校验、风控过滤、审批链、业务规则链 |

模板方法的步骤通常写在一个抽象类中，顺序固定。
责任链的节点通常是多个独立处理器，可以动态增减和排序。

## 和命令模式的区别

模板方法模式和命令模式也经常一起出现，但意图不同。

| 模式         | 关注点                   | 典型场景                              |
| ------------ | ------------------------ | ------------------------------------- |
| 模板方法模式 | 复用一套固定处理流程     | 批处理、导入、导出、任务执行          |
| 命令模式     | 把一次业务动作封装成对象 | 操作中心、任务调度、MQ 消费、撤销重做 |

如果核心问题是“多个业务动作有相同流程”，优先考虑模板方法模式。
如果核心问题是“把一个动作封装起来执行、排队、记录”，优先考虑命令模式。

## 小结

模板方法模式在 Spring Boot 项目中的常见落地方式是：使用抽象类定义固定业务流程，通过抽象方法和钩子方法开放差异步骤，再由具体子类实现不同业务逻辑。
在批量导入、文件导出、支付处理、任务执行、数据同步、报表生成等流程稳定但步骤有差异的场景中，模板方法模式可以减少重复代码，使流程结构更清晰、更容易扩展。
