# 设计模式：迭代器模式

迭代器模式用于在不暴露集合内部结构的前提下，按统一方式顺序访问集合中的元素。在 JDK21 和 Spring Boot 3 项目中，迭代器模式常用于分页数据遍历、树节点遍历、批量任务扫描、文件记录读取、游标查询、大数据分批处理、聚合对象内部元素访问等场景。

需要注意：迭代器模式关注的是“如何遍历集合”。如果是处理树形整体和部分结构，更适合组合模式；如果是把操作封装成对象，更适合命令模式；如果是对大批量数据进行分批扫描且隐藏分页细节，迭代器模式更合适。

## 基础配置

本示例基于 JDK21、Spring Boot 3、Maven 项目。示例包路径统一使用 `io.github.atengk`。

文件位置：`pom.xml`

```xml
<dependencies>
    <!-- Spring Boot Web，用于提供接口验证迭代器模式行为 -->
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

    <!-- Lombok，简化日志对象、Getter、构造方法等样板代码 -->
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

迭代器模式的核心目标是把集合遍历逻辑从集合对象中拆出来，让调用方通过统一的迭代接口访问元素，而不用关心底层数据是数组、列表、树、分页接口还是游标查询。

常见角色如下：

| 角色              | 说明                                          |
| ----------------- | --------------------------------------------- |
| Iterator          | 迭代器接口，定义 `hasNext`、`next` 等访问方法 |
| ConcreteIterator  | 具体迭代器，实现具体遍历逻辑                  |
| Aggregate         | 聚合对象，表示可被遍历的数据集合              |
| ConcreteAggregate | 具体聚合对象，负责创建迭代器                  |
| Client            | 调用方，面向迭代器访问元素                    |

常见实现方式如下：

| 实现方式         | 是否推荐 | 适用场景                     |
| ---------------- | -------- | ---------------------------- |
| Java `Iterator`  | 推荐     | 本地集合遍历                 |
| 自定义迭代器     | 推荐     | 需要隐藏特殊遍历规则         |
| 分页迭代器       | 强烈推荐 | 批量处理数据库或远程接口数据 |
| 游标迭代器       | 强烈推荐 | 大数据量扫描                 |
| 直接暴露内部集合 | 不推荐   | 调用方容易依赖内部结构       |

在 Spring Boot 项目中，常见优先级通常是：

```text
分页迭代器 / 游标迭代器 > Java Iterator > 直接暴露内部 List
```

迭代器模式不是为了替代所有 `for` 循环，而是为了把“如何取下一批数据、如何判断是否结束、如何隐藏内部结构”这些遍历细节封装起来。

## 普通 Java 迭代器

普通 Java 迭代器适合不依赖 Spring 容器的集合遍历场景。下面以订单集合为例，调用方只通过迭代器遍历订单，不直接操作订单列表。

整体结构如下：

```text
OrderCollection
    -> 创建 OrderIterator
        -> hasNext()
        -> next()
```

### 文件结构

```text
src/main/java/io/github/atengk/design/iterator/simple/
├── OrderItem.java
├── OrderIterator.java
└── OrderCollection.java
```

文件位置：`src/main/java/io/github/atengk/design/iterator/simple/OrderItem.java`

下面是订单元素对象，用于表示集合中的单个订单。

```java
package io.github.atengk.design.iterator.simple;

import java.math.BigDecimal;

/**
 * 订单元素
 *
 * @param orderNo 订单号
 * @param userId  用户ID
 * @param amount  订单金额
 * @author Ateng
 * @since 2026-04-30
 */
public record OrderItem(String orderNo, Long userId, BigDecimal amount) {
}
```

文件位置：`src/main/java/io/github/atengk/design/iterator/simple/OrderIterator.java`

下面是订单迭代器接口，定义是否存在下一个元素以及获取下一个元素的方法。

```java
package io.github.atengk.design.iterator.simple;

/**
 * 订单迭代器
 *
 * @author Ateng
 * @since 2026-04-30
 */
public interface OrderIterator {

    /**
     * 是否存在下一个订单
     *
     * @return true 表示存在，false 表示不存在
     */
    boolean hasNext();

    /**
     * 获取下一个订单
     *
     * @return 订单元素
     */
    OrderItem next();
}
```

文件位置：`src/main/java/io/github/atengk/design/iterator/simple/OrderCollection.java`

下面是订单集合对象。它隐藏内部列表结构，只对外提供添加订单和创建迭代器能力。

```java
package io.github.atengk.design.iterator.simple;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.StrUtil;
import lombok.extern.slf4j.Slf4j;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.NoSuchElementException;

/**
 * 订单集合
 *
 * @author Ateng
 * @since 2026-04-30
 */
@Slf4j
public class OrderCollection {

    private final List<OrderItem> orders = new ArrayList<>();

    /**
     * 添加订单
     *
     * @param order 订单元素
     */
    public void add(OrderItem order) {
        validateOrder(order);

        orders.add(order);
        log.info("添加订单到集合成功，订单号：{}，当前数量：{}", order.orderNo(), orders.size());
    }

    /**
     * 创建订单迭代器
     *
     * @return 订单迭代器
     */
    public OrderIterator iterator() {
        return new ListOrderIterator(List.copyOf(orders));
    }

    /**
     * 获取订单数量
     *
     * @return 订单数量
     */
    public int size() {
        return orders.size();
    }

    /**
     * 校验订单元素
     *
     * @param order 订单元素
     */
    private void validateOrder(OrderItem order) {
        if (order == null) {
            log.warn("添加订单失败，订单为空");
            throw new IllegalArgumentException("订单不能为空");
        }

        if (StrUtil.isBlank(order.orderNo())) {
            log.warn("添加订单失败，订单号为空");
            throw new IllegalArgumentException("订单号不能为空");
        }

        if (order.userId() == null || order.userId() <= 0) {
            log.warn("添加订单失败，用户ID不合法，用户ID：{}", order.userId());
            throw new IllegalArgumentException("用户ID必须大于0");
        }

        if (order.amount() == null || order.amount().compareTo(BigDecimal.ZERO) <= 0) {
            log.warn("添加订单失败，订单金额不合法，金额：{}", order.amount());
            throw new IllegalArgumentException("订单金额必须大于0");
        }
    }

    /**
     * 基于列表的订单迭代器
     *
     * @author Ateng
     * @since 2026-04-30
     */
    private static class ListOrderIterator implements OrderIterator {

        private final List<OrderItem> orders;
        private int cursor = 0;

        /**
         * 创建基于列表的订单迭代器
         *
         * @param orders 订单列表
         */
        private ListOrderIterator(List<OrderItem> orders) {
            this.orders = CollUtil.emptyIfNull(orders);
        }

        /**
         * 是否存在下一个订单
         *
         * @return true 表示存在，false 表示不存在
         */
        @Override
        public boolean hasNext() {
            return cursor < orders.size();
        }

        /**
         * 获取下一个订单
         *
         * @return 订单元素
         */
        @Override
        public OrderItem next() {
            if (!hasNext()) {
                throw new NoSuchElementException("没有更多订单");
            }

            return orders.get(cursor++);
        }
    }
}
```

使用方式：

```java
OrderCollection collection = new OrderCollection();
collection.add(new OrderItem("ORDER10001", 10001L, BigDecimal.valueOf(99.90)));
collection.add(new OrderItem("ORDER10002", 10002L, BigDecimal.valueOf(199.00)));

OrderIterator iterator = collection.iterator();
while (iterator.hasNext()) {
    OrderItem order = iterator.next();
    System.out.println(order.orderNo());
}
```

调用方只通过 `OrderIterator` 访问订单元素，不需要知道 `OrderCollection` 内部使用的是 `ArrayList`、数组还是其他结构。

## Spring Boot 分页迭代器

Spring Boot 项目中，迭代器模式更常见的价值是隐藏分页查询细节。调用方只需要不断调用 `hasNext` 和 `next`，不用关心当前是第几页、每页多少条、什么时候加载下一页。

下面以订单批量处理为例，系统需要分页扫描订单，并逐个处理订单。

整体流程如下：

```text
Controller 触发处理
    -> OrderBatchProcessService
        -> OrderPageIterator
            -> OrderQueryService 分页查询
                -> 逐个返回订单
```

### 文件结构

```text
src/main/java/io/github/atengk/design/
├── IteratorApplication.java
├── controller/
│   └── OrderBatchController.java
├── dto/
│   ├── OrderData.java
│   ├── PageQuery.java
│   ├── PageResult.java
│   └── BatchProcessResponse.java
├── iterator/
│   └── OrderPageIterator.java
└── service/
    ├── OrderQueryService.java
    ├── OrderBatchProcessService.java
    └── impl/
        ├── OrderQueryServiceImpl.java
        └── OrderBatchProcessServiceImpl.java
```

文件位置：`src/main/java/io/github/atengk/design/IteratorApplication.java`

下面是 Spring Boot 启动类。

```java
package io.github.atengk.design;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * 迭代器模式示例启动类
 *
 * @author Ateng
 * @since 2026-04-30
 */
@SpringBootApplication
public class IteratorApplication {

    /**
     * 应用启动入口
     *
     * @param args 启动参数
     */
    public static void main(String[] args) {
        SpringApplication.run(IteratorApplication.class, args);
    }
}
```

文件位置：`src/main/java/io/github/atengk/design/dto/OrderData.java`

下面是订单数据对象，用于模拟数据库查询结果。

```java
package io.github.atengk.design.dto;

import java.math.BigDecimal;

/**
 * 订单数据
 *
 * @param orderNo 订单号
 * @param userId  用户ID
 * @param amount  订单金额
 * @param status  订单状态
 * @author Ateng
 * @since 2026-04-30
 */
public record OrderData(
        String orderNo,
        Long userId,
        BigDecimal amount,
        String status
) {
}
```

文件位置：`src/main/java/io/github/atengk/design/dto/PageQuery.java`

下面是分页查询对象。

```java
package io.github.atengk.design.dto;

/**
 * 分页查询
 *
 * @param pageNum  页码，从 1 开始
 * @param pageSize 每页大小
 * @author Ateng
 * @since 2026-04-30
 */
public record PageQuery(Integer pageNum, Integer pageSize) {
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
 * @param records  当前页数据
 * @param pageNum  当前页码
 * @param pageSize 每页大小
 * @param total    总数量
 * @param hasNext  是否存在下一页
 * @param <T>      数据类型
 * @author Ateng
 * @since 2026-04-30
 */
public record PageResult<T>(
        List<T> records,
        Integer pageNum,
        Integer pageSize,
        Long total,
        Boolean hasNext
) {
}
```

文件位置：`src/main/java/io/github/atengk/design/dto/BatchProcessResponse.java`

下面是批量处理响应对象。

```java
package io.github.atengk.design.dto;

import java.math.BigDecimal;

/**
 * 批量处理响应
 *
 * @param totalCount  处理数量
 * @param totalAmount 处理总金额
 * @param message     响应消息
 * @author Ateng
 * @since 2026-04-30
 */
public record BatchProcessResponse(
        Integer totalCount,
        BigDecimal totalAmount,
        String message
) {
}
```

文件位置：`src/main/java/io/github/atengk/design/service/OrderQueryService.java`

下面是订单查询服务接口，负责按分页条件查询订单。

```java
package io.github.atengk.design.service;

import io.github.atengk.design.dto.OrderData;
import io.github.atengk.design.dto.PageQuery;
import io.github.atengk.design.dto.PageResult;

/**
 * 订单查询服务
 *
 * @author Ateng
 * @since 2026-04-30
 */
public interface OrderQueryService {

    /**
     * 分页查询订单
     *
     * @param query 分页查询
     * @return 订单分页结果
     */
    PageResult<OrderData> queryPage(PageQuery query);
}
```

文件位置：`src/main/java/io/github/atengk/design/service/impl/OrderQueryServiceImpl.java`

下面是订单查询服务实现。示例使用内存数据模拟数据库分页查询，实际项目中可以替换为 MyBatis-Plus 分页查询。

```java
package io.github.atengk.design.service.impl;

import cn.hutool.core.collection.CollUtil;
import io.github.atengk.design.dto.OrderData;
import io.github.atengk.design.dto.PageQuery;
import io.github.atengk.design.dto.PageResult;
import io.github.atengk.design.service.OrderQueryService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.List;

/**
 * 订单查询服务实现
 *
 * @author Ateng
 * @since 2026-04-30
 */
@Slf4j
@Service
public class OrderQueryServiceImpl implements OrderQueryService {

    private static final List<OrderData> MOCK_ORDERS = List.of(
            new OrderData("ORDER10001", 10001L, BigDecimal.valueOf(99.90), "PAID"),
            new OrderData("ORDER10002", 10002L, BigDecimal.valueOf(199.00), "PAID"),
            new OrderData("ORDER10003", 10003L, BigDecimal.valueOf(49.90), "CREATED"),
            new OrderData("ORDER10004", 10004L, BigDecimal.valueOf(299.00), "PAID"),
            new OrderData("ORDER10005", 10005L, BigDecimal.valueOf(18.80), "CANCELED"),
            new OrderData("ORDER10006", 10006L, BigDecimal.valueOf(66.60), "PAID")
    );

    /**
     * 分页查询订单
     *
     * @param query 分页查询
     * @return 订单分页结果
     */
    @Override
    public PageResult<OrderData> queryPage(PageQuery query) {
        validateQuery(query);

        int fromIndex = (query.pageNum() - 1) * query.pageSize();
        if (fromIndex >= MOCK_ORDERS.size()) {
            log.info("分页查询订单为空，页码：{}，每页大小：{}", query.pageNum(), query.pageSize());
            return new PageResult<>(List.of(), query.pageNum(), query.pageSize(), (long) MOCK_ORDERS.size(), false);
        }

        int toIndex = Math.min(fromIndex + query.pageSize(), MOCK_ORDERS.size());
        List<OrderData> records = MOCK_ORDERS.subList(fromIndex, toIndex);
        boolean hasNext = toIndex < MOCK_ORDERS.size();

        log.info("分页查询订单成功，页码：{}，每页大小：{}，当前数量：{}，是否有下一页：{}",
                query.pageNum(), query.pageSize(), CollUtil.size(records), hasNext);

        return new PageResult<>(
                records,
                query.pageNum(),
                query.pageSize(),
                (long) MOCK_ORDERS.size(),
                hasNext
        );
    }

    /**
     * 校验分页查询
     *
     * @param query 分页查询
     */
    private void validateQuery(PageQuery query) {
        if (query == null) {
            log.warn("分页查询订单失败，查询参数为空");
            throw new IllegalArgumentException("分页查询参数不能为空");
        }

        if (query.pageNum() == null || query.pageNum() <= 0) {
            log.warn("分页查询订单失败，页码不合法，页码：{}", query.pageNum());
            throw new IllegalArgumentException("页码必须大于0");
        }

        if (query.pageSize() == null || query.pageSize() <= 0) {
            log.warn("分页查询订单失败，每页大小不合法，每页大小：{}", query.pageSize());
            throw new IllegalArgumentException("每页大小必须大于0");
        }
    }
}
```

文件位置：`src/main/java/io/github/atengk/design/iterator/OrderPageIterator.java`

下面是订单分页迭代器。它内部维护分页状态，调用方只需要逐个取订单。

```java
package io.github.atengk.design.iterator;

import cn.hutool.core.collection.CollUtil;
import io.github.atengk.design.dto.OrderData;
import io.github.atengk.design.dto.PageQuery;
import io.github.atengk.design.dto.PageResult;
import io.github.atengk.design.service.OrderQueryService;
import lombok.extern.slf4j.Slf4j;

import java.util.Iterator;
import java.util.List;
import java.util.NoSuchElementException;

/**
 * 订单分页迭代器
 *
 * @author Ateng
 * @since 2026-04-30
 */
@Slf4j
public class OrderPageIterator implements Iterator<OrderData> {

    private final OrderQueryService orderQueryService;
    private final int pageSize;

    private int currentPageNum = 1;
    private boolean hasMorePage = true;
    private List<OrderData> currentRecords = List.of();
    private int currentIndex = 0;

    /**
     * 创建订单分页迭代器
     *
     * @param orderQueryService 订单查询服务
     * @param pageSize          每页大小
     */
    public OrderPageIterator(OrderQueryService orderQueryService, int pageSize) {
        if (orderQueryService == null) {
            throw new IllegalArgumentException("订单查询服务不能为空");
        }

        if (pageSize <= 0) {
            throw new IllegalArgumentException("每页大小必须大于0");
        }

        this.orderQueryService = orderQueryService;
        this.pageSize = pageSize;
    }

    /**
     * 是否存在下一个订单
     *
     * @return true 表示存在，false 表示不存在
     */
    @Override
    public boolean hasNext() {
        if (currentIndex < currentRecords.size()) {
            return true;
        }

        if (!hasMorePage) {
            return false;
        }

        loadNextPage();
        return currentIndex < currentRecords.size();
    }

    /**
     * 获取下一个订单
     *
     * @return 订单数据
     */
    @Override
    public OrderData next() {
        if (!hasNext()) {
            throw new NoSuchElementException("没有更多订单数据");
        }

        return currentRecords.get(currentIndex++);
    }

    /**
     * 加载下一页数据
     */
    private void loadNextPage() {
        PageResult<OrderData> pageResult = orderQueryService.queryPage(new PageQuery(currentPageNum, pageSize));

        currentRecords = CollUtil.emptyIfNull(pageResult.records());
        currentIndex = 0;
        hasMorePage = Boolean.TRUE.equals(pageResult.hasNext());
        currentPageNum++;

        log.info("订单分页迭代器加载数据，页码：{}，数量：{}，是否有下一页：{}",
                pageResult.pageNum(), currentRecords.size(), hasMorePage);
    }
}
```

文件位置：`src/main/java/io/github/atengk/design/service/OrderBatchProcessService.java`

下面是订单批量处理服务接口。

```java
package io.github.atengk.design.service;

import io.github.atengk.design.dto.BatchProcessResponse;

/**
 * 订单批量处理服务
 *
 * @author Ateng
 * @since 2026-04-30
 */
public interface OrderBatchProcessService {

    /**
     * 批量处理订单
     *
     * @param pageSize 每页大小
     * @return 批量处理响应
     */
    BatchProcessResponse process(Integer pageSize);
}
```

文件位置：`src/main/java/io/github/atengk/design/service/impl/OrderBatchProcessServiceImpl.java`

下面是订单批量处理服务实现。它使用分页迭代器逐个处理订单，不直接关心分页查询细节。

```java
package io.github.atengk.design.service.impl;

import cn.hutool.core.util.NumberUtil;
import io.github.atengk.design.dto.BatchProcessResponse;
import io.github.atengk.design.dto.OrderData;
import io.github.atengk.design.iterator.OrderPageIterator;
import io.github.atengk.design.service.OrderBatchProcessService;
import io.github.atengk.design.service.OrderQueryService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;

/**
 * 订单批量处理服务实现
 *
 * @author Ateng
 * @since 2026-04-30
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class OrderBatchProcessServiceImpl implements OrderBatchProcessService {

    private final OrderQueryService orderQueryService;

    /**
     * 批量处理订单
     *
     * @param pageSize 每页大小
     * @return 批量处理响应
     */
    @Override
    public BatchProcessResponse process(Integer pageSize) {
        int actualPageSize = pageSize == null || pageSize <= 0 ? 2 : pageSize;
        OrderPageIterator iterator = new OrderPageIterator(orderQueryService, actualPageSize);

        int totalCount = 0;
        BigDecimal totalAmount = BigDecimal.ZERO;

        while (iterator.hasNext()) {
            OrderData order = iterator.next();

            if (!"PAID".equals(order.status())) {
                log.info("跳过非已支付订单，订单号：{}，状态：{}", order.orderNo(), order.status());
                continue;
            }

            totalCount++;
            totalAmount = NumberUtil.add(totalAmount, order.amount());

            log.info("处理已支付订单，订单号：{}，用户ID：{}，金额：{}", order.orderNo(), order.userId(), order.amount());
        }

        log.info("批量处理订单完成，处理数量：{}，处理总金额：{}", totalCount, totalAmount);

        return new BatchProcessResponse(
                totalCount,
                totalAmount,
                "批量处理完成"
        );
    }
}
```

文件位置：`src/main/java/io/github/atengk/design/controller/OrderBatchController.java`

下面是订单批量处理接口，用于验证分页迭代器效果。

```java
package io.github.atengk.design.controller;

import io.github.atengk.design.dto.BatchProcessResponse;
import io.github.atengk.design.service.OrderBatchProcessService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

/**
 * 订单批量处理控制器
 *
 * @author Ateng
 * @since 2026-04-30
 */
@RestController
@RequiredArgsConstructor
@RequestMapping("/iterator/order")
public class OrderBatchController {

    private final OrderBatchProcessService orderBatchProcessService;

    /**
     * 批量处理订单
     *
     * @param pageSize 每页大小
     * @return 批量处理响应
     */
    @PostMapping("/process")
    public BatchProcessResponse process(@RequestParam(required = false) Integer pageSize) {
        return orderBatchProcessService.process(pageSize);
    }
}
```

接口调用示例：

```bash
curl -X POST "http://localhost:8080/iterator/order/process?pageSize=2"
```

可能返回：

```json
{
  "totalCount": 4,
  "totalAmount": 664.50,
  "message": "批量处理完成"
}
```

这种方式的优点是批量处理逻辑不用直接写分页循环。分页状态、下一页加载、结束判断都被封装在 `OrderPageIterator` 中。

## 游标迭代器

当数据量较大时，页码分页可能存在性能问题，尤其是数据库 `OFFSET` 很大时查询会变慢。此时可以使用游标迭代器，通过上一批数据的最大 ID 或时间作为下一批查询条件。

整体思路如下：

```text
lastId = 0
查询 id > lastId LIMIT pageSize
处理当前批次
lastId = 当前批次最大 id
继续查询下一批
```

### 文件结构

```text
src/main/java/io/github/atengk/design/iterator/cursor/
├── UserData.java
├── UserCursorQueryService.java
└── UserCursorIterator.java
```

文件位置：`src/main/java/io/github/atengk/design/iterator/cursor/UserData.java`

下面是用户数据对象。

```java
package io.github.atengk.design.iterator.cursor;

/**
 * 用户数据
 *
 * @param id       用户ID
 * @param username 用户名
 * @author Ateng
 * @since 2026-04-30
 */
public record UserData(Long id, String username) {
}
```

文件位置：`src/main/java/io/github/atengk/design/iterator/cursor/UserCursorQueryService.java`

下面是用户游标查询服务接口。

```java
package io.github.atengk.design.iterator.cursor;

import java.util.List;

/**
 * 用户游标查询服务
 *
 * @author Ateng
 * @since 2026-04-30
 */
public interface UserCursorQueryService {

    /**
     * 按游标查询用户
     *
     * @param lastId   上一次最大用户ID
     * @param pageSize 每批大小
     * @return 用户列表
     */
    List<UserData> queryAfterId(Long lastId, Integer pageSize);
}
```

文件位置：`src/main/java/io/github/atengk/design/iterator/cursor/UserCursorIterator.java`

下面是用户游标迭代器。它通过 `lastId` 控制下一批数据的位置。

```java
package io.github.atengk.design.iterator.cursor;

import cn.hutool.core.collection.CollUtil;
import lombok.extern.slf4j.Slf4j;

import java.util.Iterator;
import java.util.List;
import java.util.NoSuchElementException;

/**
 * 用户游标迭代器
 *
 * @author Ateng
 * @since 2026-04-30
 */
@Slf4j
public class UserCursorIterator implements Iterator<UserData> {

    private final UserCursorQueryService queryService;
    private final int pageSize;

    private Long lastId = 0L;
    private boolean hasMore = true;
    private List<UserData> currentRecords = List.of();
    private int currentIndex = 0;

    /**
     * 创建用户游标迭代器
     *
     * @param queryService 用户游标查询服务
     * @param pageSize     每批大小
     */
    public UserCursorIterator(UserCursorQueryService queryService, int pageSize) {
        if (queryService == null) {
            throw new IllegalArgumentException("用户游标查询服务不能为空");
        }

        if (pageSize <= 0) {
            throw new IllegalArgumentException("每批大小必须大于0");
        }

        this.queryService = queryService;
        this.pageSize = pageSize;
    }

    /**
     * 是否存在下一个用户
     *
     * @return true 表示存在，false 表示不存在
     */
    @Override
    public boolean hasNext() {
        if (currentIndex < currentRecords.size()) {
            return true;
        }

        if (!hasMore) {
            return false;
        }

        loadNextBatch();
        return currentIndex < currentRecords.size();
    }

    /**
     * 获取下一个用户
     *
     * @return 用户数据
     */
    @Override
    public UserData next() {
        if (!hasNext()) {
            throw new NoSuchElementException("没有更多用户数据");
        }

        return currentRecords.get(currentIndex++);
    }

    /**
     * 加载下一批数据
     */
    private void loadNextBatch() {
        currentRecords = CollUtil.emptyIfNull(queryService.queryAfterId(lastId, pageSize));
        currentIndex = 0;

        if (CollUtil.isEmpty(currentRecords)) {
            hasMore = false;
            log.info("用户游标迭代器加载结束，lastId：{}", lastId);
            return;
        }

        lastId = currentRecords.get(currentRecords.size() - 1).id();
        hasMore = currentRecords.size() == pageSize;

        log.info("用户游标迭代器加载数据，数量：{}，lastId：{}，是否继续：{}",
                currentRecords.size(), lastId, hasMore);
    }
}
```

游标迭代器比页码分页更适合大批量扫描，但要求查询字段有稳定顺序，例如自增 ID、创建时间加 ID 等。生产环境中常见 SQL 如下：

```sql
SELECT id, username
FROM sys_user
WHERE id > #{lastId}
ORDER BY id ASC
LIMIT #{pageSize};
```

这种查询不会随着扫描页数增大而出现明显的 `OFFSET` 性能问题。

## 迭代器模式和组合模式的关系

组合模式常用于构建树，迭代器模式常用于遍历树。二者可以组合使用。

例如权限树使用组合模式建模后，可以再提供一个深度优先迭代器，统一遍历所有节点。

示例遍历方式：

```text
系统管理
用户管理
新增用户
删除用户
角色管理
新增角色
分配权限
```

组合模式解决“树怎么组织”，迭代器模式解决“树怎么遍历”。

在业务项目中，如果树结构只需要返回给前端展示，直接递归转换 DTO 即可。如果树结构需要多种遍历方式，例如深度优先、广度优先、只遍历叶子节点、只遍历菜单节点，就可以引入专门的迭代器。

## 迭代器模式和责任链模式的区别

迭代器模式和责任链模式都可能出现“一个接一个处理”的结构，但含义不同。

| 对比项           | 迭代器模式                 | 责任链模式                    |
| ---------------- | -------------------------- | ----------------------------- |
| 核心目的         | 顺序访问集合元素           | 多个处理器依次处理请求        |
| 被遍历对象       | 数据元素                   | 处理器节点                    |
| 是否修改处理流程 | 不强调                     | 强调处理、中断、放行          |
| 典型方法         | `hasNext`、`next`          | `handle`、`check`、`doFilter` |
| 典型场景         | 扫描订单、读取文件、遍历树 | 参数校验、风控链、过滤链      |

简单理解：

```text
迭代器模式：一个一个取数据。
责任链模式：一个请求过多个关卡。
```

批量扫描订单适合迭代器模式。订单提交前经过参数校验、库存校验、风控校验，更适合责任链模式。

## 迭代器模式和 Stream 的关系

Java Stream 也可以完成集合遍历、过滤、映射和聚合，但它和迭代器模式关注点不同。

| 对比项   | 迭代器模式                       | Java Stream                  |
| -------- | -------------------------------- | ---------------------------- |
| 核心目的 | 封装遍历过程                     | 声明式数据处理               |
| 数据来源 | 可以是集合、分页、游标、远程接口 | 通常基于已有集合或可流式来源 |
| 控制方式 | 手动 `hasNext` / `next`          | 链式操作                     |
| 适合场景 | 隐藏分页或游标细节               | 集合过滤、映射、聚合         |
| 可中断性 | 调用方容易控制                   | 通过短路操作控制             |

如果数据已经在内存中，使用 Stream 通常更简洁：

```java
BigDecimal totalAmount = orders.stream()
        .filter(order -> "PAID".equals(order.status()))
        .map(OrderData::amount)
        .reduce(BigDecimal.ZERO, BigDecimal::add);
```

如果数据需要从数据库或远程接口分批加载，迭代器更适合隐藏分页细节：

```java
OrderPageIterator iterator = new OrderPageIterator(orderQueryService, 100);
while (iterator.hasNext()) {
    OrderData order = iterator.next();
    // 逐个处理订单
}
```

两者也可以结合。例如自定义 `Spliterator` 后把分页迭代器包装成 Stream，但普通业务项目中没有必要过度封装。

## 验证方式

启动 Spring Boot 项目：

```bash
mvn spring-boot:run
```

执行订单批量处理接口：

```bash
curl -X POST "http://localhost:8080/iterator/order/process?pageSize=2"
```

如果分页迭代器正常，可以看到类似日志：

```text
分页查询订单成功，页码：1，每页大小：2，当前数量：2，是否有下一页：true
订单分页迭代器加载数据，页码：1，数量：2，是否有下一页：true
处理已支付订单，订单号：ORDER10001，用户ID：10001，金额：99.90
处理已支付订单，订单号：ORDER10002，用户ID：10002，金额：199.00
分页查询订单成功，页码：2，每页大小：2，当前数量：2，是否有下一页：true
订单分页迭代器加载数据，页码：2，数量：2，是否有下一页：true
跳过非已支付订单，订单号：ORDER10003，状态：CREATED
处理已支付订单，订单号：ORDER10004，用户ID：10004，金额：299.00
分页查询订单成功，页码：3，每页大小：2，当前数量：2，是否有下一页：false
订单分页迭代器加载数据，页码：3，数量：2，是否有下一页：false
跳过非已支付订单，订单号：ORDER10005，状态：CANCELED
处理已支付订单，订单号：ORDER10006，用户ID：10006，金额：66.60
批量处理订单完成，处理数量：4，处理总金额：664.50
```

执行不传分页大小的请求：

```bash
curl -X POST "http://localhost:8080/iterator/order/process"
```

服务会使用默认分页大小 `2`，用于避免一次性加载过多数据。

执行非法分页大小：

```bash
curl -X POST "http://localhost:8080/iterator/order/process?pageSize=-1"
```

示例代码中会自动使用默认值。如果项目要求严格校验，也可以直接抛出异常。

## 注意事项

迭代器模式适合隐藏遍历细节，但不要为了普通集合遍历强行封装迭代器。如果只是简单遍历一个 `List`，直接使用增强 `for` 或 Stream 即可。

适合使用迭代器模式的场景：

```text
分页扫描数据
游标扫描数据
读取大文件记录
遍历复杂树结构
隐藏集合内部结构
需要多种遍历方式
需要统一遍历本地和远程数据
```

不太适合使用迭代器模式的场景：

```text
简单 List 遍历
简单 Map 遍历
只处理几个固定元素
遍历逻辑没有复用价值
```

分页迭代器中要避免一次性加载全部数据。下面这种写法不适合大数据量：

```java
List<OrderData> allOrders = orderQueryService.queryAll();
for (OrderData order : allOrders) {
    // 处理订单
}
```

推荐分批加载：

```java
OrderPageIterator iterator = new OrderPageIterator(orderQueryService, 100);
while (iterator.hasNext()) {
    OrderData order = iterator.next();
    // 处理订单
}
```

迭代器通常不是线程安全对象。不要把同一个迭代器实例放到 Spring 单例 Bean 的成员变量中共享。

错误示例：

```java
private OrderPageIterator currentIterator;
```

推荐在方法内部创建迭代器：

```java
public BatchProcessResponse process(Integer pageSize) {
    OrderPageIterator iterator = new OrderPageIterator(orderQueryService, pageSize);
    while (iterator.hasNext()) {
        OrderData order = iterator.next();
    }
}
```

分页迭代器要注意数据变化问题。如果遍历过程中数据被新增、删除或修改，页码分页可能出现重复数据或漏数据。对一致性要求较高的场景，建议使用游标分页、快照表、任务表或固定查询条件。

常见生产方案：

```text
按 ID 游标扫描
按 create_time + id 组合游标扫描
先生成待处理任务快照
使用状态字段标记处理进度
使用数据库锁或分布式锁控制并发
```

如果迭代器处理的是远程接口分页，需要考虑超时、限流、重试和幂等。不要在迭代器内部无限重试，否则可能导致线程长时间阻塞。

生产环境中，批量处理通常还需要记录进度：

```text
任务ID
当前游标
已处理数量
失败数量
最近错误
任务状态
开始时间
结束时间
```

迭代器只负责遍历，不应该承担复杂业务处理。业务处理逻辑应放在 Service、Handler 或 Command 中。

不推荐：

```java
public OrderData next() {
    OrderData order = currentRecords.get(currentIndex++);
    // 更新订单状态
    // 发送消息
    // 写审计日志
    return order;
}
```

推荐：

```java
while (iterator.hasNext()) {
    OrderData order = iterator.next();
    orderProcessor.process(order);
}
```

## 总结

在 JDK21 和 Spring Boot 3 项目中，迭代器模式的实践重点是隐藏集合、分页、游标或树结构的遍历细节，让调用方用统一方式访问元素。

普通 Java 迭代器适合理解原理和本地集合封装。Spring Boot 项目中更常见的是分页迭代器和游标迭代器，适合订单批处理、用户扫描、数据同步、文件导入、远程接口分页拉取等场景。推荐使用“查询服务 + 迭代器 + 批处理服务”的结构，让分页状态和业务处理逻辑解耦。

迭代器模式不是为了替代所有 `for` 循环，而是为了处理“遍历过程复杂、数据来源复杂、需要隐藏内部结构或分批加载”的场景。实际落地时，需要重点关注分页一致性、游标选择、内存占用、异常重试、处理进度和线程安全。
