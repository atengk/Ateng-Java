# 迭代器模式

迭代器模式用于在不暴露集合内部结构的情况下，按统一方式顺序访问集合元素。
在 Spring Boot 项目中，迭代器模式常用于分页批处理、批量导出、批量发送通知、数据迁移、游标扫描、分批消费任务、批量补偿处理等场景。

本文以“分页批量处理订单数据”为例，通过迭代器按批读取订单，避免一次性把全部订单加载到内存中。

## 适用场景

迭代器模式适合处理“调用方需要遍历数据，但不应该关心数据如何读取”的场景。

在订单批处理业务中，常见需求包括：

| 场景         | 说明                                             |
| ------------ | ------------------------------------------------ |
| 批量导出订单 | 按批读取订单，逐批写入 Excel、CSV 或对象存储     |
| 批量补偿订单 | 查询待补偿订单，逐批执行补偿逻辑                 |
| 批量发送通知 | 查询满足条件的订单，逐批发送短信、站内信或 MQ    |
| 批量同步数据 | 按游标读取订单，同步到搜索引擎、数仓或第三方系统 |
| 批量巡检状态 | 分批扫描异常订单，输出巡检报告                   |

如果在 Service 中直接写分页循环，业务代码会混合“分页读取逻辑”和“订单处理逻辑”。迭代器模式可以把分页读取细节封装起来，调用方只需要不断判断是否还有下一批数据，然后处理当前批次。

## 基础配置

本示例基于 Spring Boot 3，使用 Hutool、Lombok 和 Validation。Hutool 用于集合判断、字符串判断、计时和 ID 生成，Validation 用于接口参数基础校验。

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

    <!-- Hutool：提供集合、字符串、计时、ID生成等工具能力 -->
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
src/main/java/io/github/atengk/pattern/iterator
├── IteratorApplication.java
├── common
│   ├── ApiResult.java
│   ├── BizException.java
│   └── GlobalExceptionHandler.java
└── order
    ├── controller
    │   └── OrderBatchController.java
    ├── dto
    │   └── OrderBatchProcessRequest.java
    ├── entity
    │   └── OrderInfo.java
    ├── iterator
    │   ├── OrderBatchIterator.java
    │   ├── OrderBatchIteratorFactory.java
    │   └── OrderCursorBatchIterator.java
    ├── repository
    │   └── MockOrderRepository.java
    ├── service
    │   ├── OrderBatchService.java
    │   └── impl
    │       └── OrderBatchServiceImpl.java
    └── vo
        └── OrderBatchProcessResultVO.java
```

## 核心设计

本示例使用“游标批量迭代器”实现订单分页读取。相比普通页码分页，游标分页在批处理场景中更稳定，尤其适合大数据量扫描。

角色关系如下：

| 角色                | 项目中的类                  | 说明                                    |
| ------------------- | --------------------------- | --------------------------------------- |
| Iterator            | `OrderBatchIterator`        | 定义是否存在下一批、获取下一批数据      |
| ConcreteIterator    | `OrderCursorBatchIterator`  | 基于游标按批读取订单                    |
| Aggregate / Factory | `OrderBatchIteratorFactory` | 创建具体订单迭代器                      |
| Client              | `OrderBatchServiceImpl`     | 使用迭代器处理订单，不关心读取细节      |
| Data Source         | `MockOrderRepository`       | 模拟订单数据源，实际项目可替换为 Mapper |

执行流程如下：

```text
Controller
  -> OrderBatchService
    -> OrderBatchIteratorFactory
      -> OrderCursorBatchIterator
        -> MockOrderRepository
    -> while(iterator.hasNext())
      -> iterator.next()
      -> 处理当前批次订单
```

迭代器只负责“如何读取下一批数据”，Service 只负责“拿到数据后如何处理”。

## 公共代码

公共响应对象、业务异常和全局异常处理用于统一接口返回。实际项目中可以复用已有基础包。

文件位置：`src/main/java/io/github/atengk/pattern/iterator/common/ApiResult.java`

```java
package io.github.atengk.pattern.iterator.common;

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

文件位置：`src/main/java/io/github/atengk/pattern/iterator/common/BizException.java`

```java
package io.github.atengk.pattern.iterator.common;

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

文件位置：`src/main/java/io/github/atengk/pattern/iterator/common/GlobalExceptionHandler.java`

```java
package io.github.atengk.pattern.iterator.common;

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

下面给出迭代器模式的核心实现。示例使用内存集合模拟订单表，实际项目中可以把 `MockOrderRepository` 替换成 MyBatis-Plus Mapper，并使用 `id > lastId limit batchSize` 的方式查询。

文件位置：`src/main/java/io/github/atengk/pattern/iterator/IteratorApplication.java`

```java
package io.github.atengk.pattern.iterator;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * 迭代器模式示例启动类
 *
 * @author Ateng
 * @since 2026-05-13
 */
@SpringBootApplication
public class IteratorApplication {

    public static void main(String[] args) {
        SpringApplication.run(IteratorApplication.class, args);
    }

}
```

## 订单模型

订单实体用于模拟数据库中的订单记录。这里保留 `id` 字段作为游标分页依据。

文件位置：`src/main/java/io/github/atengk/pattern/iterator/order/entity/OrderInfo.java`

```java
package io.github.atengk.pattern.iterator.order.entity;

import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;

/**
 * 订单信息实体
 *
 * @author Ateng
 * @since 2026-05-13
 */
@Data
@Builder
public class OrderInfo {

    private Long id;

    private String orderNo;

    private Long userId;

    private Long productId;

    private BigDecimal amount;

    private String status;

}
```

请求对象用于接收批处理参数，包括订单状态和每批处理数量。

文件位置：`src/main/java/io/github/atengk/pattern/iterator/order/dto/OrderBatchProcessRequest.java`

```java
package io.github.atengk.pattern.iterator.order.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

/**
 * 订单批处理请求参数
 *
 * @author Ateng
 * @since 2026-05-13
 */
@Data
public class OrderBatchProcessRequest {

    @NotBlank(message = "订单状态不能为空")
    private String status;

    @Min(value = 1, message = "每批处理数量不能小于1")
    @Max(value = 500, message = "每批处理数量不能大于500")
    private Integer batchSize = 20;

}
```

响应对象用于返回批处理结果，包括总处理数量、批次数和处理状态。

文件位置：`src/main/java/io/github/atengk/pattern/iterator/order/vo/OrderBatchProcessResultVO.java`

```java
package io.github.atengk.pattern.iterator.order.vo;

import lombok.Builder;
import lombok.Data;

/**
 * 订单批处理结果
 *
 * @author Ateng
 * @since 2026-05-13
 */
@Data
@Builder
public class OrderBatchProcessResultVO {

    private String status;

    private Integer batchSize;

    private Integer batchCount;

    private Integer totalCount;

    private Long costMillis;

}
```

## 模拟数据源

这里使用 `MockOrderRepository` 模拟订单表。核心方法是 `queryByStatusAfterId`，它根据状态和游标 ID 查询下一批订单。

文件位置：`src/main/java/io/github/atengk/pattern/iterator/order/repository/MockOrderRepository.java`

```java
package io.github.atengk.pattern.iterator.order.repository;

import cn.hutool.core.util.IdUtil;
import io.github.atengk.pattern.iterator.order.entity.OrderInfo;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/**
 * 模拟订单数据仓储
 *
 * @author Ateng
 * @since 2026-05-13
 */
@Slf4j
@Repository
public class MockOrderRepository {

    private final List<OrderInfo> orderStorage = new ArrayList<>();

    /**
     * 初始化模拟订单数据
     */
    @PostConstruct
    public void initData() {
        for (long index = 1; index <= 105; index++) {
            String status = index % 3 == 0 ? "CANCELED" : "PAID";

            OrderInfo orderInfo = OrderInfo.builder()
                    .id(index)
                    .orderNo("OD" + IdUtil.getSnowflakeNextIdStr())
                    .userId(10000L + index)
                    .productId(20000L + index)
                    .amount(BigDecimal.valueOf(50 + index))
                    .status(status)
                    .build();

            orderStorage.add(orderInfo);
        }

        log.info("模拟订单数据初始化完成，订单数量：{}", orderStorage.size());
    }

    /**
     * 根据订单状态和游标ID查询下一批订单
     *
     * @param status 订单状态
     * @param lastId 上一次读取到的最大ID
     * @param limit 查询数量
     * @return 订单列表
     */
    public List<OrderInfo> queryByStatusAfterId(String status, Long lastId, Integer limit) {
        return orderStorage.stream()
                .filter(orderInfo -> orderInfo.getStatus().equalsIgnoreCase(status))
                .filter(orderInfo -> orderInfo.getId() > lastId)
                .sorted(Comparator.comparingLong(OrderInfo::getId))
                .limit(limit)
                .toList();
    }

}
```

## 迭代器接口

迭代器接口定义批量读取的统一行为。调用方不需要知道数据来自数据库、缓存、文件还是远程接口。

文件位置：`src/main/java/io/github/atengk/pattern/iterator/order/iterator/OrderBatchIterator.java`

```java
package io.github.atengk.pattern.iterator.order.iterator;

import io.github.atengk.pattern.iterator.order.entity.OrderInfo;

import java.util.List;

/**
 * 订单批量迭代器
 *
 * @author Ateng
 * @since 2026-05-13
 */
public interface OrderBatchIterator {

    /**
     * 判断是否存在下一批订单
     *
     * @return 是否存在下一批
     */
    boolean hasNext();

    /**
     * 获取下一批订单
     *
     * @return 下一批订单列表
     */
    List<OrderInfo> next();

}
```

## 游标批量迭代器

具体迭代器封装游标分页逻辑。调用方只需要调用 `hasNext()` 和 `next()`，不用关心 `lastId` 如何维护。

文件位置：`src/main/java/io/github/atengk/pattern/iterator/order/iterator/OrderCursorBatchIterator.java`

```java
package io.github.atengk.pattern.iterator.order.iterator;

import cn.hutool.core.collection.CollUtil;
import io.github.atengk.pattern.iterator.common.BizException;
import io.github.atengk.pattern.iterator.order.entity.OrderInfo;
import io.github.atengk.pattern.iterator.order.repository.MockOrderRepository;
import lombok.extern.slf4j.Slf4j;

import java.util.Collections;
import java.util.List;

/**
 * 基于游标的订单批量迭代器
 *
 * @author Ateng
 * @since 2026-05-13
 */
@Slf4j
public class OrderCursorBatchIterator implements OrderBatchIterator {

    private final MockOrderRepository orderRepository;

    private final String status;

    private final Integer batchSize;

    private Long lastId = 0L;

    private Boolean loaded = false;

    private Boolean finished = false;

    private List<OrderInfo> currentBatch = Collections.emptyList();

    /**
     * 创建订单游标批量迭代器
     *
     * @param orderRepository 订单仓储
     * @param status 订单状态
     * @param batchSize 每批数量
     */
    public OrderCursorBatchIterator(MockOrderRepository orderRepository, String status, Integer batchSize) {
        this.orderRepository = orderRepository;
        this.status = status;
        this.batchSize = batchSize;
    }

    /**
     * 判断是否存在下一批订单
     *
     * @return 是否存在下一批
     */
    @Override
    public boolean hasNext() {
        if (finished) {
            return false;
        }

        if (!loaded) {
            loadNextBatch();
        }

        return CollUtil.isNotEmpty(currentBatch);
    }

    /**
     * 获取下一批订单
     *
     * @return 下一批订单列表
     */
    @Override
    public List<OrderInfo> next() {
        if (!hasNext()) {
            throw new BizException("没有可读取的下一批订单");
        }

        List<OrderInfo> result = currentBatch;
        OrderInfo lastOrder = result.get(result.size() - 1);
        lastId = lastOrder.getId();

        loaded = false;
        currentBatch = Collections.emptyList();

        log.info("读取下一批订单完成，status：{}，lastId：{}，batchCount：{}",
                status, lastId, result.size());

        return result;
    }

    /**
     * 加载下一批订单
     */
    private void loadNextBatch() {
        currentBatch = orderRepository.queryByStatusAfterId(status, lastId, batchSize);
        loaded = true;

        if (CollUtil.isEmpty(currentBatch)) {
            finished = true;
            log.info("订单迭代器读取结束，status：{}，lastId：{}", status, lastId);
        }
    }

}
```

## 迭代器工厂

迭代器工厂负责创建具体迭代器。这样 Service 不直接依赖具体迭代器构造细节，后续可以扩展为数据库迭代器、Redis 迭代器或远程接口迭代器。

文件位置：`src/main/java/io/github/atengk/pattern/iterator/order/iterator/OrderBatchIteratorFactory.java`

```java
package io.github.atengk.pattern.iterator.order.iterator;

import cn.hutool.core.util.StrUtil;
import io.github.atengk.pattern.iterator.common.BizException;
import io.github.atengk.pattern.iterator.order.repository.MockOrderRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

/**
 * 订单批量迭代器工厂
 *
 * @author Ateng
 * @since 2026-05-13
 */
@Component
@RequiredArgsConstructor
public class OrderBatchIteratorFactory {

    private final MockOrderRepository orderRepository;

    /**
     * 创建按订单状态读取的批量迭代器
     *
     * @param status 订单状态
     * @param batchSize 每批数量
     * @return 订单批量迭代器
     */
    public OrderBatchIterator createByStatus(String status, Integer batchSize) {
        if (StrUtil.isBlank(status)) {
            throw new BizException("订单状态不能为空");
        }

        if (batchSize == null || batchSize <= 0) {
            throw new BizException("每批处理数量必须大于0");
        }

        return new OrderCursorBatchIterator(orderRepository, StrUtil.upperCase(status), batchSize);
    }

}
```

## 业务服务

业务服务是迭代器的使用方。它只关心“是否还有下一批”和“如何处理当前批次”，不关心数据源如何分页读取。

文件位置：`src/main/java/io/github/atengk/pattern/iterator/order/service/OrderBatchService.java`

```java
package io.github.atengk.pattern.iterator.order.service;

import io.github.atengk.pattern.iterator.order.dto.OrderBatchProcessRequest;
import io.github.atengk.pattern.iterator.order.vo.OrderBatchProcessResultVO;

/**
 * 订单批处理服务接口
 *
 * @author Ateng
 * @since 2026-05-13
 */
public interface OrderBatchService {

    /**
     * 批量处理订单
     *
     * @param request 订单批处理请求
     * @return 批处理结果
     */
    OrderBatchProcessResultVO processOrders(OrderBatchProcessRequest request);

}
```

Service 实现类使用迭代器循环读取订单，并逐批执行处理逻辑。

文件位置：`src/main/java/io/github/atengk/pattern/iterator/order/service/impl/OrderBatchServiceImpl.java`

```java
package io.github.atengk.pattern.iterator.order.service.impl;

import cn.hutool.core.date.DateUtil;
import cn.hutool.core.date.TimeInterval;
import cn.hutool.core.util.StrUtil;
import io.github.atengk.pattern.iterator.order.dto.OrderBatchProcessRequest;
import io.github.atengk.pattern.iterator.order.entity.OrderInfo;
import io.github.atengk.pattern.iterator.order.iterator.OrderBatchIterator;
import io.github.atengk.pattern.iterator.order.iterator.OrderBatchIteratorFactory;
import io.github.atengk.pattern.iterator.order.service.OrderBatchService;
import io.github.atengk.pattern.iterator.order.vo.OrderBatchProcessResultVO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * 订单批处理服务实现类
 *
 * @author Ateng
 * @since 2026-05-13
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class OrderBatchServiceImpl implements OrderBatchService {

    private final OrderBatchIteratorFactory orderBatchIteratorFactory;

    /**
     * 批量处理订单
     *
     * @param request 订单批处理请求
     * @return 批处理结果
     */
    @Override
    public OrderBatchProcessResultVO processOrders(OrderBatchProcessRequest request) {
        TimeInterval timer = DateUtil.timer();

        String status = StrUtil.upperCase(request.getStatus());
        Integer batchSize = request.getBatchSize();

        OrderBatchIterator iterator = orderBatchIteratorFactory.createByStatus(status, batchSize);

        int batchCount = 0;
        int totalCount = 0;

        while (iterator.hasNext()) {
            List<OrderInfo> orderBatch = iterator.next();

            batchCount++;
            totalCount += orderBatch.size();

            handleOrderBatch(orderBatch, batchCount);
        }

        long costMillis = timer.interval();
        log.info("订单批处理完成，status：{}，batchSize：{}，batchCount：{}，totalCount：{}，costMillis：{}",
                status, batchSize, batchCount, totalCount, costMillis);

        return OrderBatchProcessResultVO.builder()
                .status(status)
                .batchSize(batchSize)
                .batchCount(batchCount)
                .totalCount(totalCount)
                .costMillis(costMillis)
                .build();
    }

    /**
     * 处理当前批次订单
     *
     * @param orderBatch 订单批次
     * @param batchIndex 批次序号
     */
    private void handleOrderBatch(List<OrderInfo> orderBatch, Integer batchIndex) {
        log.info("开始处理第 {} 批订单，数量：{}", batchIndex, orderBatch.size());

        for (OrderInfo orderInfo : orderBatch) {
            log.info("处理订单，id：{}，orderNo：{}，userId：{}，amount：{}，status：{}",
                    orderInfo.getId(),
                    orderInfo.getOrderNo(),
                    orderInfo.getUserId(),
                    orderInfo.getAmount(),
                    orderInfo.getStatus());
        }

        log.info("第 {} 批订单处理完成", batchIndex);
    }

}
```

## 控制器接口

控制器提供批处理入口。调用方传入订单状态和每批数量后，服务层通过迭代器完成批量处理。

文件位置：`src/main/java/io/github/atengk/pattern/iterator/order/controller/OrderBatchController.java`

```java
package io.github.atengk.pattern.iterator.order.controller;

import io.github.atengk.pattern.iterator.common.ApiResult;
import io.github.atengk.pattern.iterator.order.dto.OrderBatchProcessRequest;
import io.github.atengk.pattern.iterator.order.service.OrderBatchService;
import io.github.atengk.pattern.iterator.order.vo.OrderBatchProcessResultVO;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

/**
 * 订单批处理接口
 *
 * @author Ateng
 * @since 2026-05-13
 */
@RestController
@RequestMapping("/order-batches")
@RequiredArgsConstructor
public class OrderBatchController {

    private final OrderBatchService orderBatchService;

    /**
     * 批量处理订单
     *
     * @param request 订单批处理请求
     * @return 批处理结果
     */
    @PostMapping("/process")
    public ApiResult<OrderBatchProcessResultVO> processOrders(@Valid @RequestBody OrderBatchProcessRequest request) {
        return ApiResult.success(orderBatchService.processOrders(request));
    }

}
```

## 使用方式

启动项目后，调用批处理接口即可触发迭代器按批读取订单。

接口信息：

| 项目         | 内容                     |
| ------------ | ------------------------ |
| 请求路径     | `/order-batches/process` |
| 请求方法     | `POST`                   |
| Content-Type | `application/json`       |
| 主要参数     | `status`、`batchSize`    |

处理已支付订单：

```bash
curl -X POST "http://localhost:8080/order-batches/process" \
  -H "Content-Type: application/json" \
  -d '{
    "status": "PAID",
    "batchSize": 20
  }'
```

返回示例：

```json
{
  "code": 200,
  "message": "操作成功",
  "data": {
    "status": "PAID",
    "batchSize": 20,
    "batchCount": 4,
    "totalCount": 70,
    "costMillis": 35
  }
}
```

处理已取消订单：

```bash
curl -X POST "http://localhost:8080/order-batches/process" \
  -H "Content-Type: application/json" \
  -d '{
    "status": "CANCELED",
    "batchSize": 10
  }'
```

返回示例：

```json
{
  "code": 200,
  "message": "操作成功",
  "data": {
    "status": "CANCELED",
    "batchSize": 10,
    "batchCount": 4,
    "totalCount": 35,
    "costMillis": 20
  }
}
```

## 验证方式

正常请求后，可以通过日志观察迭代器读取和业务处理过程。

```text
模拟订单数据初始化完成，订单数量：105
读取下一批订单完成，status：PAID，lastId：29，batchCount：20
开始处理第 1 批订单，数量：20
第 1 批订单处理完成
读取下一批订单完成，status：PAID，lastId：59，batchCount：20
开始处理第 2 批订单，数量：20
第 2 批订单处理完成
读取下一批订单完成，status：PAID，lastId：89，batchCount：20
开始处理第 3 批订单，数量：20
第 3 批订单处理完成
读取下一批订单完成，status：PAID，lastId：105，batchCount：10
开始处理第 4 批订单，数量：10
第 4 批订单处理完成
订单迭代器读取结束，status：PAID，lastId：105
订单批处理完成，status：PAID，batchSize：20，batchCount：4，totalCount：70，costMillis：35
```

如果每批数量超过限制，例如：

```bash
curl -X POST "http://localhost:8080/order-batches/process" \
  -H "Content-Type: application/json" \
  -d '{
    "status": "PAID",
    "batchSize": 1000
  }'
```

返回示例：

```json
{
  "code": 500,
  "message": "请求参数不合法",
  "data": null
}
```

## 替换为 MyBatis-Plus 查询

实际项目中，`MockOrderRepository` 通常会被 Mapper 替换。核心 SQL 思路是按主键游标读取，避免深分页。

示例 SQL：

```sql
-- 按状态和游标ID查询下一批订单
SELECT
    id,
    order_no,
    user_id,
    product_id,
    amount,
    status
FROM t_order
WHERE status = #{status}
  AND id > #{lastId}
ORDER BY id ASC
LIMIT #{limit};
```

如果使用 MyBatis-Plus，可以把仓储方法改成类似形式：

```java
public List<OrderInfo> queryByStatusAfterId(String status, Long lastId, Integer limit) {
    return lambdaQuery()
            .eq(OrderInfo::getStatus, status)
            .gt(OrderInfo::getId, lastId)
            .orderByAsc(OrderInfo::getId)
            .last("LIMIT " + limit)
            .list();
}
```

在生产项目中，`limit` 应来自后端校验后的整数，不要直接拼接用户输入的原始字符串。

## 扩展方式

如果后续不仅要遍历订单，还要遍历用户、商品、账单、库存记录，可以抽象出通用批量迭代器。

例如定义泛型批量迭代器：

```java
package io.github.atengk.pattern.iterator.common;

import java.util.List;

/**
 * 通用批量迭代器
 *
 * @author Ateng
 * @since 2026-05-13
 */
public interface BatchIterator<T> {

    /**
     * 判断是否存在下一批数据
     *
     * @return 是否存在下一批
     */
    boolean hasNext();

    /**
     * 获取下一批数据
     *
     * @return 下一批数据
     */
    List<T> next();

}
```

然后让订单迭代器实现 `BatchIterator<OrderInfo>`，用户迭代器实现 `BatchIterator<UserInfo>`。这样批量处理框架可以复用，只需要替换具体数据读取逻辑。

## 优点和注意事项

迭代器模式的核心价值是隔离遍历逻辑和业务处理逻辑。Service 不需要直接维护分页参数、游标、是否结束等细节，只需要按统一方式获取下一批数据。

| 注意事项                     | 说明                                                         |
| ---------------------------- | ------------------------------------------------------------ |
| 优先使用游标分页             | 批处理场景中，`id > lastId` 通常比 `pageNo + pageSize` 更稳定 |
| 避免一次性加载全部数据       | 大批量导出、补偿、同步时不要直接 `list()` 全量数据           |
| 处理逻辑要考虑幂等           | 批处理失败重试时，订单处理动作应具备幂等能力                 |
| 迭代期间避免修改查询条件字段 | 如果一边查询 `status = PAID`，一边把状态改掉，普通页码分页可能跳数据 |
| 注意批次大小                 | 批次过小会增加查询次数，批次过大会增加内存和接口压力         |
| 可以结合任务调度             | 迭代器模式常和 XXL-JOB、Spring Task、MQ 消费补偿任务结合使用 |

## 和责任链模式的区别

迭代器模式和责任链模式都可能出现“顺序执行”，但语义不同。

| 模式       | 关注点                       | 典型场景                               |
| ---------- | ---------------------------- | -------------------------------------- |
| 迭代器模式 | 顺序访问一组数据             | 分页读取、批量导出、批量处理、游标扫描 |
| 责任链模式 | 一个请求依次经过多个处理节点 | 参数校验、风控校验、审批链、过滤链     |

简单来说，迭代器模式遍历的是“数据集合”，责任链模式遍历的是“处理节点”。

## 小结

迭代器模式在 Spring Boot 项目中的常见落地方式是：定义统一迭代器接口，把分页读取、游标维护、结束判断封装到具体迭代器中，业务服务只负责处理每批数据。
在批量导出、批量补偿、数据同步、消息重试、巡检任务等场景中，迭代器模式可以有效降低批处理代码复杂度，并避免一次性加载大量数据。
