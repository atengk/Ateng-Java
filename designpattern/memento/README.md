# 备忘录模式

备忘录模式用于在不破坏对象封装性的前提下，保存对象某一时刻的内部状态，并在需要时恢复到该状态。
在 Spring Boot 项目中，备忘录模式常用于草稿编辑、配置回滚、审批撤回、表单恢复、版本快照、流程状态回退、规则发布回滚等场景。

本文以“订单草稿修改与回滚”为例。用户编辑订单草稿时，系统会在每次修改前自动保存一份快照。当用户发现修改错误时，可以选择某个历史快照进行恢复。

## 适用场景

备忘录模式适合处理“对象状态需要保存和恢复”的场景。

在订单草稿业务中，常见操作包括：

| 操作     | 说明                       |
| -------- | -------------------------- |
| 创建草稿 | 保存用户初始订单草稿       |
| 修改草稿 | 修改商品、数量、备注等字段 |
| 自动备份 | 每次修改前保存一份历史快照 |
| 查询历史 | 查看草稿历史版本           |
| 回滚草稿 | 把草稿恢复到某个历史快照   |

如果直接把历史版本逻辑写在 Service 中，Service 需要了解草稿对象的所有字段，后续字段增加时也要修改大量备份代码。备忘录模式可以把“如何保存自身状态”和“如何恢复自身状态”交给业务对象自己处理，历史管理器只负责保存快照。

## 基础配置

本示例基于 Spring Boot 3，使用 Hutool、Lombok 和 Validation。Hutool 用于对象判断、字符串判断、集合处理和 ID 生成，Validation 用于接口参数基础校验。

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

    <!-- Hutool：提供对象、字符串、集合、ID生成等工具能力 -->
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
src/main/java/io/github/atengk/pattern/memento
├── MementoApplication.java
├── common
│   ├── ApiResult.java
│   ├── BizException.java
│   └── GlobalExceptionHandler.java
└── order
    ├── controller
    │   └── OrderDraftController.java
    ├── dto
    │   ├── OrderDraftCreateRequest.java
    │   ├── OrderDraftUpdateRequest.java
    │   └── OrderDraftRollbackRequest.java
    ├── memento
    │   └── OrderDraftMemento.java
    ├── model
    │   └── OrderDraftAggregate.java
    ├── repository
    │   ├── OrderDraftHistoryRepository.java
    │   └── OrderDraftRepository.java
    ├── service
    │   ├── OrderDraftService.java
    │   └── impl
    │       └── OrderDraftServiceImpl.java
    └── vo
        ├── OrderDraftHistoryVO.java
        └── OrderDraftVO.java
```

## 核心设计

本示例把备忘录模式拆成三个核心角色：

| 角色       | 项目中的类                    | 说明                                         |
| ---------- | ----------------------------- | -------------------------------------------- |
| Originator | `OrderDraftAggregate`         | 原发器，负责创建快照和从快照恢复             |
| Memento    | `OrderDraftMemento`           | 备忘录，保存草稿某一时刻的状态               |
| Caretaker  | `OrderDraftHistoryRepository` | 管理者，只负责保存和查询快照，不修改快照内容 |

执行流程如下：

```text
创建草稿
  -> OrderDraftAggregate

修改草稿
  -> OrderDraftAggregate.createMemento()
  -> OrderDraftHistoryRepository.save()
  -> OrderDraftAggregate.update()

回滚草稿
  -> OrderDraftHistoryRepository.get()
  -> OrderDraftAggregate.restore()
```

备忘录模式的关键点是：历史管理器不直接读取和修改草稿对象内部状态，只保存 `OrderDraftMemento`。草稿对象如何生成快照、如何从快照恢复，由 `OrderDraftAggregate` 自己负责。

## 公共代码

公共响应对象、业务异常和全局异常处理用于统一接口返回。实际项目中可以复用已有基础包。

文件位置：`src/main/java/io/github/atengk/pattern/memento/common/ApiResult.java`

```java
package io.github.atengk.pattern.memento.common;

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

文件位置：`src/main/java/io/github/atengk/pattern/memento/common/BizException.java`

```java
package io.github.atengk.pattern.memento.common;

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

文件位置：`src/main/java/io/github/atengk/pattern/memento/common/GlobalExceptionHandler.java`

```java
package io.github.atengk.pattern.memento.common;

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

下面给出备忘录模式的核心代码。示例使用内存仓储模拟数据库，实际项目中可以把 `OrderDraftRepository` 和 `OrderDraftHistoryRepository` 替换为 MyBatis-Plus、JPA 或 Redis 存储。

文件位置：`src/main/java/io/github/atengk/pattern/memento/MementoApplication.java`

```java
package io.github.atengk.pattern.memento;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * 备忘录模式示例启动类
 *
 * @author Ateng
 * @since 2026-05-13
 */
@SpringBootApplication
public class MementoApplication {

    public static void main(String[] args) {
        SpringApplication.run(MementoApplication.class, args);
    }

}
```

## 请求对象和响应对象

请求对象用于接收草稿创建、修改和回滚参数。响应对象用于返回当前草稿状态和历史快照信息。

文件位置：`src/main/java/io/github/atengk/pattern/memento/order/dto/OrderDraftCreateRequest.java`

```java
package io.github.atengk.pattern.memento.order.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

/**
 * 订单草稿创建请求参数
 *
 * @author Ateng
 * @since 2026-05-13
 */
@Data
public class OrderDraftCreateRequest {

    @NotNull(message = "用户ID不能为空")
    private Long userId;

    @NotNull(message = "商品ID不能为空")
    private Long productId;

    @Min(value = 1, message = "购买数量必须大于0")
    private Integer quantity;

    private String remark;

}
```

文件位置：`src/main/java/io/github/atengk/pattern/memento/order/dto/OrderDraftUpdateRequest.java`

```java
package io.github.atengk.pattern.memento.order.dto;

import jakarta.validation.constraints.Min;
import lombok.Data;

/**
 * 订单草稿修改请求参数
 *
 * @author Ateng
 * @since 2026-05-13
 */
@Data
public class OrderDraftUpdateRequest {

    private Long productId;

    @Min(value = 1, message = "购买数量必须大于0")
    private Integer quantity;

    private String remark;

    private String status;

}
```

文件位置：`src/main/java/io/github/atengk/pattern/memento/order/dto/OrderDraftRollbackRequest.java`

```java
package io.github.atengk.pattern.memento.order.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

/**
 * 订单草稿回滚请求参数
 *
 * @author Ateng
 * @since 2026-05-13
 */
@Data
public class OrderDraftRollbackRequest {

    @NotBlank(message = "快照ID不能为空")
    private String snapshotId;

}
```

文件位置：`src/main/java/io/github/atengk/pattern/memento/order/vo/OrderDraftVO.java`

```java
package io.github.atengk.pattern.memento.order.vo;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 订单草稿展示对象
 *
 * @author Ateng
 * @since 2026-05-13
 */
@Data
@Builder
public class OrderDraftVO {

    private String draftId;

    private Long userId;

    private Long productId;

    private Integer quantity;

    private String remark;

    private String status;

    private Integer version;

    private LocalDateTime updatedAt;

}
```

文件位置：`src/main/java/io/github/atengk/pattern/memento/order/vo/OrderDraftHistoryVO.java`

```java
package io.github.atengk.pattern.memento.order.vo;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 订单草稿历史快照展示对象
 *
 * @author Ateng
 * @since 2026-05-13
 */
@Data
@Builder
public class OrderDraftHistoryVO {

    private String snapshotId;

    private String draftId;

    private Integer snapshotVersion;

    private String reason;

    private Long productId;

    private Integer quantity;

    private String remark;

    private String status;

    private LocalDateTime createdAt;

}
```

## 备忘录对象

备忘录对象保存订单草稿某一时刻的状态。这里使用 `@Getter` 和 `@Builder`，不提供 setter，避免外部随意修改历史快照。

文件位置：`src/main/java/io/github/atengk/pattern/memento/order/memento/OrderDraftMemento.java`

```java
package io.github.atengk.pattern.memento.order.memento;

import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;

/**
 * 订单草稿备忘录
 *
 * @author Ateng
 * @since 2026-05-13
 */
@Getter
@Builder
public class OrderDraftMemento {

    private final String snapshotId;

    private final String draftId;

    private final Long userId;

    private final Long productId;

    private final Integer quantity;

    private final String remark;

    private final String status;

    private final Integer snapshotVersion;

    private final String reason;

    private final LocalDateTime createdAt;

}
```

## 原发器对象

`OrderDraftAggregate` 是备忘录模式中的原发器。它负责保存自身状态、创建快照、从快照恢复状态。外部对象不需要知道草稿内部字段如何复制。

文件位置：`src/main/java/io/github/atengk/pattern/memento/order/model/OrderDraftAggregate.java`

```java
package io.github.atengk.pattern.memento.order.model;

import cn.hutool.core.util.IdUtil;
import cn.hutool.core.util.ObjectUtil;
import cn.hutool.core.util.StrUtil;
import io.github.atengk.pattern.memento.common.BizException;
import io.github.atengk.pattern.memento.order.dto.OrderDraftCreateRequest;
import io.github.atengk.pattern.memento.order.dto.OrderDraftUpdateRequest;
import io.github.atengk.pattern.memento.order.memento.OrderDraftMemento;
import io.github.atengk.pattern.memento.order.vo.OrderDraftVO;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

import java.time.LocalDateTime;

/**
 * 订单草稿聚合对象
 *
 * @author Ateng
 * @since 2026-05-13
 */
@Slf4j
@Getter
public class OrderDraftAggregate {

    private static final String DEFAULT_STATUS = "EDITING";

    private String draftId;

    private Long userId;

    private Long productId;

    private Integer quantity;

    private String remark;

    private String status;

    private Integer version;

    private LocalDateTime updatedAt;

    /**
     * 根据创建请求构建订单草稿
     *
     * @param request 订单草稿创建请求
     * @return 订单草稿聚合对象
     */
    public static OrderDraftAggregate create(OrderDraftCreateRequest request) {
        OrderDraftAggregate aggregate = new OrderDraftAggregate();
        aggregate.draftId = "DFT" + IdUtil.getSnowflakeNextIdStr();
        aggregate.userId = request.getUserId();
        aggregate.productId = request.getProductId();
        aggregate.quantity = request.getQuantity();
        aggregate.remark = StrUtil.blankToDefault(request.getRemark(), "");
        aggregate.status = DEFAULT_STATUS;
        aggregate.version = 1;
        aggregate.updatedAt = LocalDateTime.now();

        log.info("订单草稿创建完成，draftId：{}，userId：{}", aggregate.draftId, aggregate.userId);
        return aggregate;
    }

    /**
     * 修改订单草稿
     *
     * @param request 订单草稿修改请求
     */
    public void update(OrderDraftUpdateRequest request) {
        if (ObjectUtil.isNotNull(request.getProductId())) {
            this.productId = request.getProductId();
        }

        if (ObjectUtil.isNotNull(request.getQuantity())) {
            this.quantity = request.getQuantity();
        }

        if (ObjectUtil.isNotNull(request.getRemark())) {
            this.remark = request.getRemark();
        }

        if (StrUtil.isNotBlank(request.getStatus())) {
            this.status = StrUtil.upperCase(request.getStatus());
        }

        this.version++;
        this.updatedAt = LocalDateTime.now();

        log.info("订单草稿修改完成，draftId：{}，version：{}", this.draftId, this.version);
    }

    /**
     * 创建当前状态的备忘录快照
     *
     * @param reason 快照原因
     * @return 订单草稿备忘录
     */
    public OrderDraftMemento createMemento(String reason) {
        return OrderDraftMemento.builder()
                .snapshotId("SNP" + IdUtil.getSnowflakeNextIdStr())
                .draftId(this.draftId)
                .userId(this.userId)
                .productId(this.productId)
                .quantity(this.quantity)
                .remark(this.remark)
                .status(this.status)
                .snapshotVersion(this.version)
                .reason(StrUtil.blankToDefault(reason, "系统自动备份"))
                .createdAt(LocalDateTime.now())
                .build();
    }

    /**
     * 从备忘录快照恢复状态
     *
     * @param memento 订单草稿备忘录
     */
    public void restore(OrderDraftMemento memento) {
        if (!StrUtil.equals(this.draftId, memento.getDraftId())) {
            throw new BizException("快照不属于当前草稿，不能回滚");
        }

        this.userId = memento.getUserId();
        this.productId = memento.getProductId();
        this.quantity = memento.getQuantity();
        this.remark = memento.getRemark();
        this.status = memento.getStatus();
        this.version++;
        this.updatedAt = LocalDateTime.now();

        log.info("订单草稿回滚完成，draftId：{}，snapshotId：{}，currentVersion：{}",
                this.draftId, memento.getSnapshotId(), this.version);
    }

    /**
     * 转换为展示对象
     *
     * @return 订单草稿展示对象
     */
    public OrderDraftVO toVO() {
        return OrderDraftVO.builder()
                .draftId(this.draftId)
                .userId(this.userId)
                .productId(this.productId)
                .quantity(this.quantity)
                .remark(this.remark)
                .status(this.status)
                .version(this.version)
                .updatedAt(this.updatedAt)
                .build();
    }

}
```

## 仓储对象

草稿仓储负责保存当前草稿状态，历史仓储负责保存备忘录快照。二者职责分离，避免当前状态和历史状态混在一起。

文件位置：`src/main/java/io/github/atengk/pattern/memento/order/repository/OrderDraftRepository.java`

```java
package io.github.atengk.pattern.memento.order.repository;

import cn.hutool.core.util.StrUtil;
import io.github.atengk.pattern.memento.common.BizException;
import io.github.atengk.pattern.memento.order.model.OrderDraftAggregate;
import org.springframework.stereotype.Repository;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 订单草稿仓储
 *
 * @author Ateng
 * @since 2026-05-13
 */
@Repository
public class OrderDraftRepository {

    private final Map<String, OrderDraftAggregate> draftStorage = new ConcurrentHashMap<>();

    /**
     * 保存订单草稿
     *
     * @param aggregate 订单草稿聚合对象
     */
    public void save(OrderDraftAggregate aggregate) {
        draftStorage.put(aggregate.getDraftId(), aggregate);
    }

    /**
     * 查询必须存在的订单草稿
     *
     * @param draftId 草稿ID
     * @return 订单草稿聚合对象
     */
    public OrderDraftAggregate getRequired(String draftId) {
        if (StrUtil.isBlank(draftId)) {
            throw new BizException("草稿ID不能为空");
        }

        OrderDraftAggregate aggregate = draftStorage.get(draftId);
        if (aggregate == null) {
            throw new BizException("订单草稿不存在");
        }

        return aggregate;
    }

}
```

文件位置：`src/main/java/io/github/atengk/pattern/memento/order/repository/OrderDraftHistoryRepository.java`

```java
package io.github.atengk.pattern.memento.order.repository;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.StrUtil;
import io.github.atengk.pattern.memento.common.BizException;
import io.github.atengk.pattern.memento.order.memento.OrderDraftMemento;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Repository;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 订单草稿历史快照仓储
 *
 * @author Ateng
 * @since 2026-05-13
 */
@Slf4j
@Repository
public class OrderDraftHistoryRepository {

    private final Map<String, List<OrderDraftMemento>> historyStorage = new ConcurrentHashMap<>();

    /**
     * 保存草稿历史快照
     *
     * @param memento 订单草稿备忘录
     */
    public void save(OrderDraftMemento memento) {
        historyStorage.computeIfAbsent(memento.getDraftId(), key -> new ArrayList<>()).add(memento);
        log.info("订单草稿快照保存完成，draftId：{}，snapshotId：{}，snapshotVersion：{}",
                memento.getDraftId(), memento.getSnapshotId(), memento.getSnapshotVersion());
    }

    /**
     * 查询草稿历史快照列表
     *
     * @param draftId 草稿ID
     * @return 历史快照列表
     */
    public List<OrderDraftMemento> listByDraftId(String draftId) {
        if (StrUtil.isBlank(draftId)) {
            throw new BizException("草稿ID不能为空");
        }

        List<OrderDraftMemento> mementos = historyStorage.get(draftId);
        if (CollUtil.isEmpty(mementos)) {
            return new ArrayList<>();
        }

        return mementos.stream()
                .sorted(Comparator.comparing(OrderDraftMemento::getCreatedAt).reversed())
                .toList();
    }

    /**
     * 查询必须存在的草稿快照
     *
     * @param draftId 草稿ID
     * @param snapshotId 快照ID
     * @return 草稿快照
     */
    public OrderDraftMemento getRequired(String draftId, String snapshotId) {
        return listByDraftId(draftId)
                .stream()
                .filter(memento -> StrUtil.equals(memento.getSnapshotId(), snapshotId))
                .findFirst()
                .orElseThrow(() -> new BizException("订单草稿快照不存在"));
    }

}
```

## 业务服务

业务服务负责组织用例流程：创建草稿、修改草稿、查询草稿、查询历史、回滚草稿。
需要注意的是，Service 不直接复制草稿字段，而是调用 `createMemento()` 和 `restore()` 完成状态保存与恢复。

文件位置：`src/main/java/io/github/atengk/pattern/memento/order/service/OrderDraftService.java`

```java
package io.github.atengk.pattern.memento.order.service;

import io.github.atengk.pattern.memento.order.dto.OrderDraftCreateRequest;
import io.github.atengk.pattern.memento.order.dto.OrderDraftRollbackRequest;
import io.github.atengk.pattern.memento.order.dto.OrderDraftUpdateRequest;
import io.github.atengk.pattern.memento.order.vo.OrderDraftHistoryVO;
import io.github.atengk.pattern.memento.order.vo.OrderDraftVO;

import java.util.List;

/**
 * 订单草稿服务
 *
 * @author Ateng
 * @since 2026-05-13
 */
public interface OrderDraftService {

    /**
     * 创建订单草稿
     *
     * @param request 创建请求
     * @return 订单草稿
     */
    OrderDraftVO createDraft(OrderDraftCreateRequest request);

    /**
     * 修改订单草稿
     *
     * @param draftId 草稿ID
     * @param request 修改请求
     * @return 订单草稿
     */
    OrderDraftVO updateDraft(String draftId, OrderDraftUpdateRequest request);

    /**
     * 查询订单草稿
     *
     * @param draftId 草稿ID
     * @return 订单草稿
     */
    OrderDraftVO getDraft(String draftId);

    /**
     * 查询订单草稿历史快照
     *
     * @param draftId 草稿ID
     * @return 历史快照列表
     */
    List<OrderDraftHistoryVO> listHistory(String draftId);

    /**
     * 回滚订单草稿
     *
     * @param draftId 草稿ID
     * @param request 回滚请求
     * @return 回滚后的订单草稿
     */
    OrderDraftVO rollbackDraft(String draftId, OrderDraftRollbackRequest request);

}
```

Service 实现类在修改前保存快照，在回滚前也保存一次当前状态，避免回滚操作本身不可撤销。

文件位置：`src/main/java/io/github/atengk/pattern/memento/order/service/impl/OrderDraftServiceImpl.java`

```java
package io.github.atengk.pattern.memento.order.service.impl;

import io.github.atengk.pattern.memento.order.dto.OrderDraftCreateRequest;
import io.github.atengk.pattern.memento.order.dto.OrderDraftRollbackRequest;
import io.github.atengk.pattern.memento.order.dto.OrderDraftUpdateRequest;
import io.github.atengk.pattern.memento.order.memento.OrderDraftMemento;
import io.github.atengk.pattern.memento.order.model.OrderDraftAggregate;
import io.github.atengk.pattern.memento.order.repository.OrderDraftHistoryRepository;
import io.github.atengk.pattern.memento.order.repository.OrderDraftRepository;
import io.github.atengk.pattern.memento.order.service.OrderDraftService;
import io.github.atengk.pattern.memento.order.vo.OrderDraftHistoryVO;
import io.github.atengk.pattern.memento.order.vo.OrderDraftVO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * 订单草稿服务实现类
 *
 * @author Ateng
 * @since 2026-05-13
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class OrderDraftServiceImpl implements OrderDraftService {

    private final OrderDraftRepository orderDraftRepository;

    private final OrderDraftHistoryRepository orderDraftHistoryRepository;

    /**
     * 创建订单草稿
     *
     * @param request 创建请求
     * @return 订单草稿
     */
    @Override
    public OrderDraftVO createDraft(OrderDraftCreateRequest request) {
        OrderDraftAggregate aggregate = OrderDraftAggregate.create(request);
        orderDraftRepository.save(aggregate);

        log.info("订单草稿保存成功，draftId：{}", aggregate.getDraftId());
        return aggregate.toVO();
    }

    /**
     * 修改订单草稿
     *
     * @param draftId 草稿ID
     * @param request 修改请求
     * @return 订单草稿
     */
    @Override
    public OrderDraftVO updateDraft(String draftId, OrderDraftUpdateRequest request) {
        OrderDraftAggregate aggregate = orderDraftRepository.getRequired(draftId);

        OrderDraftMemento beforeUpdateMemento = aggregate.createMemento("修改前自动备份");
        orderDraftHistoryRepository.save(beforeUpdateMemento);

        aggregate.update(request);
        orderDraftRepository.save(aggregate);

        log.info("订单草稿修改并备份完成，draftId：{}，snapshotId：{}",
                draftId, beforeUpdateMemento.getSnapshotId());

        return aggregate.toVO();
    }

    /**
     * 查询订单草稿
     *
     * @param draftId 草稿ID
     * @return 订单草稿
     */
    @Override
    public OrderDraftVO getDraft(String draftId) {
        return orderDraftRepository.getRequired(draftId).toVO();
    }

    /**
     * 查询订单草稿历史快照
     *
     * @param draftId 草稿ID
     * @return 历史快照列表
     */
    @Override
    public List<OrderDraftHistoryVO> listHistory(String draftId) {
        return orderDraftHistoryRepository.listByDraftId(draftId)
                .stream()
                .map(this::toHistoryVO)
                .toList();
    }

    /**
     * 回滚订单草稿
     *
     * @param draftId 草稿ID
     * @param request 回滚请求
     * @return 回滚后的订单草稿
     */
    @Override
    public OrderDraftVO rollbackDraft(String draftId, OrderDraftRollbackRequest request) {
        OrderDraftAggregate aggregate = orderDraftRepository.getRequired(draftId);

        OrderDraftMemento beforeRollbackMemento = aggregate.createMemento("回滚前自动备份");
        orderDraftHistoryRepository.save(beforeRollbackMemento);

        OrderDraftMemento targetMemento = orderDraftHistoryRepository.getRequired(draftId, request.getSnapshotId());
        aggregate.restore(targetMemento);
        orderDraftRepository.save(aggregate);

        log.info("订单草稿回滚成功，draftId：{}，targetSnapshotId：{}，backupSnapshotId：{}",
                draftId, targetMemento.getSnapshotId(), beforeRollbackMemento.getSnapshotId());

        return aggregate.toVO();
    }

    /**
     * 转换历史快照展示对象
     *
     * @param memento 订单草稿备忘录
     * @return 历史快照展示对象
     */
    private OrderDraftHistoryVO toHistoryVO(OrderDraftMemento memento) {
        return OrderDraftHistoryVO.builder()
                .snapshotId(memento.getSnapshotId())
                .draftId(memento.getDraftId())
                .snapshotVersion(memento.getSnapshotVersion())
                .reason(memento.getReason())
                .productId(memento.getProductId())
                .quantity(memento.getQuantity())
                .remark(memento.getRemark())
                .status(memento.getStatus())
                .createdAt(memento.getCreatedAt())
                .build();
    }

}
```

## 控制器接口

控制器提供订单草稿创建、修改、查询历史和回滚接口。调用方不需要知道备忘录对象如何创建和恢复。

文件位置：`src/main/java/io/github/atengk/pattern/memento/order/controller/OrderDraftController.java`

```java
package io.github.atengk.pattern.memento.order.controller;

import io.github.atengk.pattern.memento.common.ApiResult;
import io.github.atengk.pattern.memento.order.dto.OrderDraftCreateRequest;
import io.github.atengk.pattern.memento.order.dto.OrderDraftRollbackRequest;
import io.github.atengk.pattern.memento.order.dto.OrderDraftUpdateRequest;
import io.github.atengk.pattern.memento.order.service.OrderDraftService;
import io.github.atengk.pattern.memento.order.vo.OrderDraftHistoryVO;
import io.github.atengk.pattern.memento.order.vo.OrderDraftVO;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 订单草稿接口
 *
 * @author Ateng
 * @since 2026-05-13
 */
@RestController
@RequestMapping("/order-drafts")
@RequiredArgsConstructor
public class OrderDraftController {

    private final OrderDraftService orderDraftService;

    /**
     * 创建订单草稿
     *
     * @param request 创建请求
     * @return 订单草稿
     */
    @PostMapping
    public ApiResult<OrderDraftVO> createDraft(@Valid @RequestBody OrderDraftCreateRequest request) {
        return ApiResult.success(orderDraftService.createDraft(request));
    }

    /**
     * 修改订单草稿
     *
     * @param draftId 草稿ID
     * @param request 修改请求
     * @return 订单草稿
     */
    @PutMapping("/{draftId}")
    public ApiResult<OrderDraftVO> updateDraft(@PathVariable String draftId,
                                               @Valid @RequestBody OrderDraftUpdateRequest request) {
        return ApiResult.success(orderDraftService.updateDraft(draftId, request));
    }

    /**
     * 查询订单草稿
     *
     * @param draftId 草稿ID
     * @return 订单草稿
     */
    @GetMapping("/{draftId}")
    public ApiResult<OrderDraftVO> getDraft(@PathVariable String draftId) {
        return ApiResult.success(orderDraftService.getDraft(draftId));
    }

    /**
     * 查询订单草稿历史快照
     *
     * @param draftId 草稿ID
     * @return 历史快照列表
     */
    @GetMapping("/{draftId}/histories")
    public ApiResult<List<OrderDraftHistoryVO>> listHistory(@PathVariable String draftId) {
        return ApiResult.success(orderDraftService.listHistory(draftId));
    }

    /**
     * 回滚订单草稿
     *
     * @param draftId 草稿ID
     * @param request 回滚请求
     * @return 回滚后的订单草稿
     */
    @PostMapping("/{draftId}/rollback")
    public ApiResult<OrderDraftVO> rollbackDraft(@PathVariable String draftId,
                                                 @Valid @RequestBody OrderDraftRollbackRequest request) {
        return ApiResult.success(orderDraftService.rollbackDraft(draftId, request));
    }

}
```

## 使用方式

启动项目后，先创建订单草稿，再修改草稿，系统会在每次修改前保存快照。之后可以查询历史快照并回滚到指定快照。

创建订单草稿：

```bash
curl -X POST "http://localhost:8080/order-drafts" \
  -H "Content-Type: application/json" \
  -d '{
    "userId": 10001,
    "productId": 20001,
    "quantity": 1,
    "remark": "初始草稿"
  }'
```

返回示例：

```json
{
  "code": 200,
  "message": "操作成功",
  "data": {
    "draftId": "DFT1998948715737427968",
    "userId": 10001,
    "productId": 20001,
    "quantity": 1,
    "remark": "初始草稿",
    "status": "EDITING",
    "version": 1,
    "updatedAt": "2026-05-13T10:00:00"
  }
}
```

第一次修改草稿：

```bash
curl -X PUT "http://localhost:8080/order-drafts/DFT1998948715737427968" \
  -H "Content-Type: application/json" \
  -d '{
    "productId": 20002,
    "quantity": 2,
    "remark": "第一次修改商品和数量"
  }'
```

第二次修改草稿：

```bash
curl -X PUT "http://localhost:8080/order-drafts/DFT1998948715737427968" \
  -H "Content-Type: application/json" \
  -d '{
    "quantity": 5,
    "remark": "第二次修改数量"
  }'
```

查询历史快照：

```bash
curl -X GET "http://localhost:8080/order-drafts/DFT1998948715737427968/histories"
```

返回示例：

```json
{
  "code": 200,
  "message": "操作成功",
  "data": [
    {
      "snapshotId": "SNP1998948991737427970",
      "draftId": "DFT1998948715737427968",
      "snapshotVersion": 2,
      "reason": "修改前自动备份",
      "productId": 20002,
      "quantity": 2,
      "remark": "第一次修改商品和数量",
      "status": "EDITING",
      "createdAt": "2026-05-13T10:02:00"
    },
    {
      "snapshotId": "SNP1998948881737427969",
      "draftId": "DFT1998948715737427968",
      "snapshotVersion": 1,
      "reason": "修改前自动备份",
      "productId": 20001,
      "quantity": 1,
      "remark": "初始草稿",
      "status": "EDITING",
      "createdAt": "2026-05-13T10:01:00"
    }
  ]
}
```

回滚到第一次快照：

```bash
curl -X POST "http://localhost:8080/order-drafts/DFT1998948715737427968/rollback" \
  -H "Content-Type: application/json" \
  -d '{
    "snapshotId": "SNP1998948881737427969"
  }'
```

返回示例：

```json
{
  "code": 200,
  "message": "操作成功",
  "data": {
    "draftId": "DFT1998948715737427968",
    "userId": 10001,
    "productId": 20001,
    "quantity": 1,
    "remark": "初始草稿",
    "status": "EDITING",
    "version": 4,
    "updatedAt": "2026-05-13T10:03:00"
  }
}
```

## 验证方式

正常修改草稿时，可以通过日志看到快照保存和草稿修改过程：

```text
订单草稿创建完成，draftId：DFT1998948715737427968，userId：10001
订单草稿保存成功，draftId：DFT1998948715737427968
订单草稿快照保存完成，draftId：DFT1998948715737427968，snapshotId：SNP1998948881737427969，snapshotVersion：1
订单草稿修改完成，draftId：DFT1998948715737427968，version：2
订单草稿修改并备份完成，draftId：DFT1998948715737427968，snapshotId：SNP1998948881737427969
```

正常回滚草稿时，可以看到回滚前自动备份和目标快照恢复过程：

```text
订单草稿快照保存完成，draftId：DFT1998948715737427968，snapshotId：SNP1998949101737427971，snapshotVersion：3
订单草稿回滚完成，draftId：DFT1998948715737427968，snapshotId：SNP1998948881737427969，currentVersion：4
订单草稿回滚成功，draftId：DFT1998948715737427968，targetSnapshotId：SNP1998948881737427969，backupSnapshotId：SNP1998949101737427971
```

如果回滚不存在的快照：

```bash
curl -X POST "http://localhost:8080/order-drafts/DFT1998948715737427968/rollback" \
  -H "Content-Type: application/json" \
  -d '{
    "snapshotId": "SNP_NOT_EXIST"
  }'
```

返回示例：

```json
{
  "code": 500,
  "message": "订单草稿快照不存在",
  "data": null
}
```

## 替换为数据库表

实际项目中，当前草稿和历史快照通常使用两张表保存。

订单草稿表：

```sql
-- 订单草稿当前状态表
CREATE TABLE t_order_draft (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '主键ID',
    draft_id VARCHAR(64) NOT NULL COMMENT '草稿ID',
    user_id BIGINT NOT NULL COMMENT '用户ID',
    product_id BIGINT NOT NULL COMMENT '商品ID',
    quantity INT NOT NULL COMMENT '购买数量',
    remark VARCHAR(500) DEFAULT '' COMMENT '备注',
    status VARCHAR(32) NOT NULL COMMENT '草稿状态',
    version INT NOT NULL COMMENT '版本号',
    updated_at DATETIME NOT NULL COMMENT '更新时间',
    UNIQUE KEY uk_draft_id (draft_id)
) COMMENT='订单草稿当前状态表';
```

订单草稿历史快照表：

```sql
-- 订单草稿历史快照表
CREATE TABLE t_order_draft_snapshot (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '主键ID',
    snapshot_id VARCHAR(64) NOT NULL COMMENT '快照ID',
    draft_id VARCHAR(64) NOT NULL COMMENT '草稿ID',
    user_id BIGINT NOT NULL COMMENT '用户ID',
    product_id BIGINT NOT NULL COMMENT '商品ID',
    quantity INT NOT NULL COMMENT '购买数量',
    remark VARCHAR(500) DEFAULT '' COMMENT '备注',
    status VARCHAR(32) NOT NULL COMMENT '草稿状态',
    snapshot_version INT NOT NULL COMMENT '快照版本号',
    reason VARCHAR(128) NOT NULL COMMENT '快照原因',
    created_at DATETIME NOT NULL COMMENT '创建时间',
    UNIQUE KEY uk_snapshot_id (snapshot_id),
    KEY idx_draft_id_created_at (draft_id, created_at)
) COMMENT='订单草稿历史快照表';
```

落库时需要注意：修改草稿和保存修改前快照应放在同一个事务中。回滚时也应先保存回滚前快照，再恢复目标快照，避免回滚操作本身无法撤销。

## 扩展方式

如果需要支持“最多保留最近 20 个快照”，可以在保存快照后清理旧版本。核心逻辑可以放在历史仓储中。

示例逻辑：

```java
private static final int MAX_HISTORY_SIZE = 20;

/**
 * 清理超过限制的历史快照
 *
 * @param draftId 草稿ID
 */
private void cleanExpiredSnapshots(String draftId) {
    List<OrderDraftMemento> mementos = historyStorage.get(draftId);
    if (CollUtil.isEmpty(mementos) || mementos.size() <= MAX_HISTORY_SIZE) {
        return;
    }

    List<OrderDraftMemento> retained = mementos.stream()
            .sorted(Comparator.comparing(OrderDraftMemento::getCreatedAt).reversed())
            .limit(MAX_HISTORY_SIZE)
            .toList();

    historyStorage.put(draftId, new ArrayList<>(retained));
    log.info("订单草稿历史快照清理完成，draftId：{}，retainedSize：{}", draftId, retained.size());
}
```

如果需要支持“对比两个快照差异”，可以查询两个 `OrderDraftMemento`，逐字段比较并返回差异项。这个能力不应放在备忘录对象里，建议放在独立的 `OrderDraftSnapshotCompareService` 中。

## 优点和注意事项

备忘录模式的核心价值是保存和恢复对象状态，同时尽量不破坏对象封装性。

| 注意事项             | 说明                                           |
| -------------------- | ---------------------------------------------- |
| 快照对象尽量不可变   | 备忘录应避免提供 setter，防止历史状态被修改    |
| 不要暴露过多内部细节 | 外部管理器只保存快照，不直接修改原发器内部字段 |
| 注意快照体积         | 如果对象字段很多，频繁保存快照会带来存储压力   |
| 注意事务一致性       | 保存快照和修改当前对象应处于同一事务边界       |
| 注意历史清理         | 高频编辑场景需要限制快照数量或设置过期时间     |
| 回滚前建议再备份     | 回滚操作本身也可能误操作，应保留回滚前状态     |

## 和命令模式的区别

备忘录模式和命令模式都可能用于撤销操作，但关注点不同。

| 模式       | 关注点                                         | 典型场景                              |
| ---------- | ---------------------------------------------- | ------------------------------------- |
| 备忘录模式 | 保存对象状态，并恢复到某个历史状态             | 草稿回滚、配置版本恢复、表单恢复      |
| 命令模式   | 把操作封装成命令对象，可执行、排队、记录或撤销 | 操作中心、任务调度、MQ 消费、撤销重做 |

如果撤销逻辑主要是“恢复历史状态”，优先考虑备忘录模式。
如果撤销逻辑主要是“执行一个反向操作”，例如扣库存对应补库存、支付对应退款，优先考虑命令模式或命令模式结合补偿事务。

## 和原型模式的区别

备忘录模式和原型模式都可能涉及对象复制，但目的不同。

| 模式       | 关注点                 | 典型场景                             |
| ---------- | ---------------------- | ------------------------------------ |
| 备忘录模式 | 保存历史状态并支持恢复 | 配置回滚、草稿恢复、审批撤回         |
| 原型模式   | 通过复制快速创建新对象 | 模板复制、对象克隆、批量创建相似对象 |

备忘录模式强调“历史状态管理”，原型模式强调“对象创建”。

## 小结

备忘录模式在 Spring Boot 项目中的常见落地方式是：由业务对象自己创建快照和恢复快照，历史管理器只负责保存和查询快照。
在订单草稿、营销配置、审批流程、规则发布、表单编辑等需要历史版本和回滚能力的场景中，备忘录模式可以使状态恢复逻辑更清晰，并减少外部代码对对象内部状态的直接依赖。
