# 设计模式：外观模式

外观模式也叫门面模式，用于为一组复杂子系统提供一个统一的高层入口，让调用方通过一个简单接口完成复杂流程。在 JDK21 和 Spring Boot 3 项目中，外观模式常用于下单流程、支付流程、报表导出、文件上传处理、用户注册、数据同步、第三方聚合调用、后台管理聚合接口等场景。

需要注意：外观模式关注的是“简化复杂子系统调用”。如果重点是接口不兼容转换，更适合适配器模式；如果重点是固定流程复用，更适合模板方法模式；如果重点是根据类型选择不同处理逻辑，更适合策略模式。

## 基础配置

本示例基于 JDK21、Spring Boot 3、Maven 项目。示例包路径统一使用 `io.github.atengk`。

文件位置：`pom.xml`

```xml
<dependencies>
    <!-- Spring Boot Web，用于提供接口验证外观模式行为 -->
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

外观模式的核心目标是隐藏子系统复杂性，对外提供一个更简单、更稳定、更符合业务语义的入口。

常见角色如下：

| 角色      | 说明                                               |
| --------- | -------------------------------------------------- |
| Facade    | 外观类，对外提供统一入口，内部编排多个子系统       |
| Subsystem | 子系统类，负责具体能力，例如库存、订单、支付、通知 |
| Client    | 调用方，只调用外观类，不直接编排多个子系统         |
| DTO       | 请求和响应对象，用于聚合多个子系统输入输出         |

常见实现方式如下：

| 实现方式                        | 是否推荐         | 适用场景                      |
| ------------------------------- | ---------------- | ----------------------------- |
| 普通 Java 外观类                | 推荐用于简单场景 | 本地工具、报表导出、文件处理  |
| Spring Facade Bean              | 强烈推荐         | Spring Boot 业务流程编排      |
| Controller 直接调用多个 Service | 不推荐           | Controller 过重，业务编排分散 |
| Service 之间互相深度调用        | 谨慎使用         | 容易形成复杂依赖链            |
| 外观类承担全部业务细节          | 不推荐           | 会变成上帝类，难维护          |

在 Spring Boot 项目中，常见优先级通常是：

```text
Spring Facade Bean > 普通 Java 外观类 > Controller 直接编排多个 Service
```

外观类不是为了替代 Service，而是用于组织多个子系统之间的协作，让调用方不用了解复杂流程。

## 普通 Java 外观

普通 Java 外观适合不依赖 Spring 容器的流程封装。下面以报表导出为例，导出一个报表需要查询数据、生成文件、上传文件三个步骤。调用方不应该关心这些子步骤，只需要调用一个外观入口。

整体流程如下：

```text
导出报表 -> 查询报表数据 -> 生成 Excel 文件 -> 上传文件 -> 返回下载地址
```

### 文件结构

```text
src/main/java/io/github/atengk/design/facade/simple/
├── ReportExportRequest.java
├── ReportExportResponse.java
├── ReportDataQueryService.java
├── ExcelGenerateService.java
├── FileStorageService.java
└── ReportExportFacade.java
```

文件位置：`src/main/java/io/github/atengk/design/facade/simple/ReportExportRequest.java`

下面是报表导出请求对象。

```java
package io.github.atengk.design.facade.simple;

/**
 * 报表导出请求
 *
 * @param reportType 报表类型
 * @param operatorId 操作人ID
 * @author Ateng
 * @since 2026-04-30
 */
public record ReportExportRequest(String reportType, Long operatorId) {
}
```

文件位置：`src/main/java/io/github/atengk/design/facade/simple/ReportExportResponse.java`

下面是报表导出响应对象。

```java
package io.github.atengk.design.facade.simple;

/**
 * 报表导出响应
 *
 * @param exportNo    导出编号
 * @param fileName    文件名
 * @param downloadUrl 下载地址
 * @param message     结果消息
 * @author Ateng
 * @since 2026-04-30
 */
public record ReportExportResponse(String exportNo, String fileName, String downloadUrl, String message) {
}
```

文件位置：`src/main/java/io/github/atengk/design/facade/simple/ReportDataQueryService.java`

下面是报表数据查询子系统，负责根据报表类型查询数据。

```java
package io.github.atengk.design.facade.simple;

import cn.hutool.core.map.MapUtil;
import cn.hutool.core.util.StrUtil;
import lombok.extern.slf4j.Slf4j;

import java.util.List;
import java.util.Map;

/**
 * 报表数据查询服务
 *
 * @author Ateng
 * @since 2026-04-30
 */
@Slf4j
public class ReportDataQueryService {

    /**
     * 查询报表数据
     *
     * @param reportType 报表类型
     * @return 报表数据
     */
    public List<Map<String, Object>> queryData(String reportType) {
        if (StrUtil.isBlank(reportType)) {
            log.warn("查询报表数据失败，报表类型为空");
            throw new IllegalArgumentException("报表类型不能为空");
        }

        log.info("查询报表数据，报表类型：{}", reportType);

        return List.of(
                MapUtil.<String, Object>builder()
                        .put("orderNo", "ORDER10001")
                        .put("amount", "99.90")
                        .put("status", "PAID")
                        .build(),
                MapUtil.<String, Object>builder()
                        .put("orderNo", "ORDER10002")
                        .put("amount", "66.60")
                        .put("status", "CREATED")
                        .build()
        );
    }
}
```

文件位置：`src/main/java/io/github/atengk/design/facade/simple/ExcelGenerateService.java`

下面是 Excel 文件生成子系统，示例中只模拟生成文件名。

```java
package io.github.atengk.design.facade.simple;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.date.DateUtil;
import cn.hutool.core.util.StrUtil;
import lombok.extern.slf4j.Slf4j;

import java.util.List;
import java.util.Map;

/**
 * Excel文件生成服务
 *
 * @author Ateng
 * @since 2026-04-30
 */
@Slf4j
public class ExcelGenerateService {

    /**
     * 生成Excel文件
     *
     * @param reportType 报表类型
     * @param rows       报表数据
     * @return 文件名
     */
    public String generateExcel(String reportType, List<Map<String, Object>> rows) {
        if (StrUtil.isBlank(reportType)) {
            log.warn("生成Excel失败，报表类型为空");
            throw new IllegalArgumentException("报表类型不能为空");
        }

        if (CollUtil.isEmpty(rows)) {
            log.warn("生成Excel失败，报表数据为空");
            throw new IllegalArgumentException("报表数据不能为空");
        }

        String fileName = StrUtil.format("{}-{}.xlsx", reportType, DateUtil.format(DateUtil.date(), "yyyyMMddHHmmss"));
        log.info("生成Excel文件成功，文件名：{}，数据量：{}", fileName, rows.size());
        return fileName;
    }
}
```

文件位置：`src/main/java/io/github/atengk/design/facade/simple/FileStorageService.java`

下面是文件存储子系统，示例中只模拟上传文件并返回下载地址。

```java
package io.github.atengk.design.facade.simple;

import cn.hutool.core.util.StrUtil;
import lombok.extern.slf4j.Slf4j;

/**
 * 文件存储服务
 *
 * @author Ateng
 * @since 2026-04-30
 */
@Slf4j
public class FileStorageService {

    /**
     * 上传文件
     *
     * @param fileName 文件名
     * @return 下载地址
     */
    public String upload(String fileName) {
        if (StrUtil.isBlank(fileName)) {
            log.warn("上传文件失败，文件名为空");
            throw new IllegalArgumentException("文件名不能为空");
        }

        String downloadUrl = "https://file.example.com/report/" + fileName;
        log.info("上传文件成功，文件名：{}，下载地址：{}", fileName, downloadUrl);
        return downloadUrl;
    }
}
```

文件位置：`src/main/java/io/github/atengk/design/facade/simple/ReportExportFacade.java`

下面是报表导出外观类。调用方只需要调用 `exportReport`，不用关心查询、生成、上传三个子步骤。

```java
package io.github.atengk.design.facade.simple;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.IdUtil;
import cn.hutool.core.util.StrUtil;
import lombok.extern.slf4j.Slf4j;

import java.util.List;
import java.util.Map;

/**
 * 报表导出外观
 *
 * @author Ateng
 * @since 2026-04-30
 */
@Slf4j
public class ReportExportFacade {

    private final ReportDataQueryService reportDataQueryService;
    private final ExcelGenerateService excelGenerateService;
    private final FileStorageService fileStorageService;

    /**
     * 创建报表导出外观
     *
     * @param reportDataQueryService 报表数据查询服务
     * @param excelGenerateService   Excel文件生成服务
     * @param fileStorageService     文件存储服务
     */
    public ReportExportFacade(ReportDataQueryService reportDataQueryService,
                              ExcelGenerateService excelGenerateService,
                              FileStorageService fileStorageService) {
        this.reportDataQueryService = reportDataQueryService;
        this.excelGenerateService = excelGenerateService;
        this.fileStorageService = fileStorageService;
    }

    /**
     * 导出报表
     *
     * @param request 报表导出请求
     * @return 报表导出响应
     */
    public ReportExportResponse exportReport(ReportExportRequest request) {
        validateRequest(request);

        String exportNo = "EXPORT" + IdUtil.getSnowflakeNextId();
        log.info("开始导出报表，导出编号：{}，报表类型：{}，操作人ID：{}",
                exportNo, request.reportType(), request.operatorId());

        List<Map<String, Object>> rows = reportDataQueryService.queryData(request.reportType());
        if (CollUtil.isEmpty(rows)) {
            log.warn("导出报表失败，查询结果为空，导出编号：{}", exportNo);
            throw new IllegalStateException("报表数据为空");
        }

        String fileName = excelGenerateService.generateExcel(request.reportType(), rows);
        String downloadUrl = fileStorageService.upload(fileName);

        log.info("导出报表完成，导出编号：{}，文件名：{}，下载地址：{}", exportNo, fileName, downloadUrl);

        return new ReportExportResponse(exportNo, fileName, downloadUrl, "导出成功");
    }

    /**
     * 校验导出请求
     *
     * @param request 报表导出请求
     */
    private void validateRequest(ReportExportRequest request) {
        if (request == null) {
            log.warn("导出报表失败，请求参数为空");
            throw new IllegalArgumentException("请求参数不能为空");
        }

        if (StrUtil.isBlank(request.reportType())) {
            log.warn("导出报表失败，报表类型为空");
            throw new IllegalArgumentException("报表类型不能为空");
        }

        if (request.operatorId() == null || request.operatorId() <= 0) {
            log.warn("导出报表失败，操作人ID不合法，操作人ID：{}", request.operatorId());
            throw new IllegalArgumentException("操作人ID必须大于0");
        }
    }
}
```

使用方式：

```java
ReportExportFacade facade = new ReportExportFacade(
        new ReportDataQueryService(),
        new ExcelGenerateService(),
        new FileStorageService()
);

ReportExportResponse response = facade.exportReport(new ReportExportRequest("order", 10001L));
```

调用方只依赖 `ReportExportFacade`，不需要直接调用 `ReportDataQueryService`、`ExcelGenerateService` 和 `FileStorageService`。

## Spring Boot 外观

Spring Boot 项目中更常见的写法，是把外观类注册为 Spring Bean，由外观类统一编排多个 Service。下面以下单结算为例，一个下单流程通常涉及用户校验、库存扣减、订单创建、支付预处理、通知发送等多个子系统。

整体流程如下：

```text
提交订单 -> 校验用户 -> 校验并扣减库存 -> 创建订单 -> 创建支付单 -> 发送通知 -> 返回下单结果
```

示例中的子系统职责如下：

```text
UserService       用户校验
InventoryService  库存校验和扣减
OrderService      创建订单
PaymentService    创建支付单
NoticeService     发送通知
OrderCheckoutFacade 统一编排下单流程
```

### 文件结构

```text
src/main/java/io/github/atengk/design/
├── FacadeApplication.java
├── controller/
│   └── OrderCheckoutController.java
├── dto/
│   ├── OrderCheckoutRequest.java
│   ├── OrderCheckoutResponse.java
│   ├── OrderCreateCommand.java
│   └── PaymentCreateCommand.java
├── facade/
│   └── OrderCheckoutFacade.java
└── service/
    ├── UserService.java
    ├── InventoryService.java
    ├── OrderService.java
    ├── PaymentService.java
    ├── NoticeService.java
    └── impl/
        ├── UserServiceImpl.java
        ├── InventoryServiceImpl.java
        ├── OrderServiceImpl.java
        ├── PaymentServiceImpl.java
        └── NoticeServiceImpl.java
```

文件位置：`src/main/java/io/github/atengk/design/FacadeApplication.java`

下面是 Spring Boot 启动类。

```java
package io.github.atengk.design;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * 外观模式示例启动类
 *
 * @author Ateng
 * @since 2026-04-30
 */
@SpringBootApplication
public class FacadeApplication {

    /**
     * 应用启动入口
     *
     * @param args 启动参数
     */
    public static void main(String[] args) {
        SpringApplication.run(FacadeApplication.class, args);
    }
}
```

文件位置：`src/main/java/io/github/atengk/design/dto/OrderCheckoutRequest.java`

下面是下单结算请求对象。

```java
package io.github.atengk.design.dto;

import java.math.BigDecimal;

/**
 * 下单结算请求
 *
 * @param userId      用户ID
 * @param productId   商品ID
 * @param productName 商品名称
 * @param quantity    购买数量
 * @param unitPrice   商品单价
 * @author Ateng
 * @since 2026-04-30
 */
public record OrderCheckoutRequest(
        Long userId,
        Long productId,
        String productName,
        Integer quantity,
        BigDecimal unitPrice
) {
}
```

文件位置：`src/main/java/io/github/atengk/design/dto/OrderCheckoutResponse.java`

下面是下单结算响应对象。

```java
package io.github.atengk.design.dto;

import java.math.BigDecimal;

/**
 * 下单结算响应
 *
 * @param orderNo     订单号
 * @param payNo       支付单号
 * @param userId      用户ID
 * @param productName 商品名称
 * @param quantity    购买数量
 * @param totalAmount 订单总金额
 * @param message     结果消息
 * @author Ateng
 * @since 2026-04-30
 */
public record OrderCheckoutResponse(
        String orderNo,
        String payNo,
        Long userId,
        String productName,
        Integer quantity,
        BigDecimal totalAmount,
        String message
) {
}
```

文件位置：`src/main/java/io/github/atengk/design/dto/OrderCreateCommand.java`

下面是创建订单命令对象，用于外观类调用订单子系统。

```java
package io.github.atengk.design.dto;

import java.math.BigDecimal;

/**
 * 创建订单命令
 *
 * @param userId      用户ID
 * @param productId   商品ID
 * @param productName 商品名称
 * @param quantity    购买数量
 * @param totalAmount 订单总金额
 * @author Ateng
 * @since 2026-04-30
 */
public record OrderCreateCommand(
        Long userId,
        Long productId,
        String productName,
        Integer quantity,
        BigDecimal totalAmount
) {
}
```

文件位置：`src/main/java/io/github/atengk/design/dto/PaymentCreateCommand.java`

下面是创建支付单命令对象，用于外观类调用支付子系统。

```java
package io.github.atengk.design.dto;

import java.math.BigDecimal;

/**
 * 创建支付单命令
 *
 * @param orderNo 订单号
 * @param userId  用户ID
 * @param amount  支付金额
 * @author Ateng
 * @since 2026-04-30
 */
public record PaymentCreateCommand(
        String orderNo,
        Long userId,
        BigDecimal amount
) {
}
```

文件位置：`src/main/java/io/github/atengk/design/service/UserService.java`

下面是用户服务接口。

```java
package io.github.atengk.design.service;

/**
 * 用户服务
 *
 * @author Ateng
 * @since 2026-04-30
 */
public interface UserService {

    /**
     * 校验用户是否可下单
     *
     * @param userId 用户ID
     */
    void checkUserCanOrder(Long userId);
}
```

文件位置：`src/main/java/io/github/atengk/design/service/InventoryService.java`

下面是库存服务接口。

```java
package io.github.atengk.design.service;

/**
 * 库存服务
 *
 * @author Ateng
 * @since 2026-04-30
 */
public interface InventoryService {

    /**
     * 扣减库存
     *
     * @param productId 商品ID
     * @param quantity  扣减数量
     */
    void deductStock(Long productId, Integer quantity);
}
```

文件位置：`src/main/java/io/github/atengk/design/service/OrderService.java`

下面是订单服务接口。

```java
package io.github.atengk.design.service;

import io.github.atengk.design.dto.OrderCreateCommand;

/**
 * 订单服务
 *
 * @author Ateng
 * @since 2026-04-30
 */
public interface OrderService {

    /**
     * 创建订单
     *
     * @param command 创建订单命令
     * @return 订单号
     */
    String createOrder(OrderCreateCommand command);
}
```

文件位置：`src/main/java/io/github/atengk/design/service/PaymentService.java`

下面是支付服务接口。

```java
package io.github.atengk.design.service;

import io.github.atengk.design.dto.PaymentCreateCommand;

/**
 * 支付服务
 *
 * @author Ateng
 * @since 2026-04-30
 */
public interface PaymentService {

    /**
     * 创建支付单
     *
     * @param command 创建支付单命令
     * @return 支付单号
     */
    String createPayment(PaymentCreateCommand command);
}
```

文件位置：`src/main/java/io/github/atengk/design/service/NoticeService.java`

下面是通知服务接口。

```java
package io.github.atengk.design.service;

/**
 * 通知服务
 *
 * @author Ateng
 * @since 2026-04-30
 */
public interface NoticeService {

    /**
     * 发送下单通知
     *
     * @param userId  用户ID
     * @param orderNo 订单号
     */
    void sendOrderCreatedNotice(Long userId, String orderNo);
}
```

文件位置：`src/main/java/io/github/atengk/design/service/impl/UserServiceImpl.java`

下面是用户服务实现，负责用户下单资格校验。

```java
package io.github.atengk.design.service.impl;

import io.github.atengk.design.service.UserService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

/**
 * 用户服务实现
 *
 * @author Ateng
 * @since 2026-04-30
 */
@Slf4j
@Service
public class UserServiceImpl implements UserService {

    /**
     * 校验用户是否可下单
     *
     * @param userId 用户ID
     */
    @Override
    public void checkUserCanOrder(Long userId) {
        if (userId == null || userId <= 0) {
            log.warn("用户下单校验失败，用户ID不合法，用户ID：{}", userId);
            throw new IllegalArgumentException("用户ID必须大于0");
        }

        log.info("用户下单校验通过，用户ID：{}", userId);
    }
}
```

文件位置：`src/main/java/io/github/atengk/design/service/impl/InventoryServiceImpl.java`

下面是库存服务实现，负责库存扣减。示例中只打印日志，实际项目中应操作库存表或库存服务。

```java
package io.github.atengk.design.service.impl;

import io.github.atengk.design.service.InventoryService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

/**
 * 库存服务实现
 *
 * @author Ateng
 * @since 2026-04-30
 */
@Slf4j
@Service
public class InventoryServiceImpl implements InventoryService {

    /**
     * 扣减库存
     *
     * @param productId 商品ID
     * @param quantity  扣减数量
     */
    @Override
    public void deductStock(Long productId, Integer quantity) {
        if (productId == null || productId <= 0) {
            log.warn("扣减库存失败，商品ID不合法，商品ID：{}", productId);
            throw new IllegalArgumentException("商品ID必须大于0");
        }

        if (quantity == null || quantity <= 0) {
            log.warn("扣减库存失败，扣减数量不合法，数量：{}", quantity);
            throw new IllegalArgumentException("扣减数量必须大于0");
        }

        log.info("扣减库存成功，商品ID：{}，扣减数量：{}", productId, quantity);
    }
}
```

文件位置：`src/main/java/io/github/atengk/design/service/impl/OrderServiceImpl.java`

下面是订单服务实现，负责创建订单并返回订单号。

```java
package io.github.atengk.design.service.impl;

import cn.hutool.core.util.IdUtil;
import io.github.atengk.design.dto.OrderCreateCommand;
import io.github.atengk.design.service.OrderService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

/**
 * 订单服务实现
 *
 * @author Ateng
 * @since 2026-04-30
 */
@Slf4j
@Service
public class OrderServiceImpl implements OrderService {

    /**
     * 创建订单
     *
     * @param command 创建订单命令
     * @return 订单号
     */
    @Override
    public String createOrder(OrderCreateCommand command) {
        String orderNo = "ORDER" + IdUtil.getSnowflakeNextId();

        log.info("创建订单成功，订单号：{}，用户ID：{}，商品ID：{}，金额：{}",
                orderNo, command.userId(), command.productId(), command.totalAmount());

        return orderNo;
    }
}
```

文件位置：`src/main/java/io/github/atengk/design/service/impl/PaymentServiceImpl.java`

下面是支付服务实现，负责创建支付单并返回支付单号。

```java
package io.github.atengk.design.service.impl;

import cn.hutool.core.util.IdUtil;
import io.github.atengk.design.dto.PaymentCreateCommand;
import io.github.atengk.design.service.PaymentService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

/**
 * 支付服务实现
 *
 * @author Ateng
 * @since 2026-04-30
 */
@Slf4j
@Service
public class PaymentServiceImpl implements PaymentService {

    /**
     * 创建支付单
     *
     * @param command 创建支付单命令
     * @return 支付单号
     */
    @Override
    public String createPayment(PaymentCreateCommand command) {
        String payNo = "PAY" + IdUtil.getSnowflakeNextId();

        log.info("创建支付单成功，支付单号：{}，订单号：{}，用户ID：{}，金额：{}",
                payNo, command.orderNo(), command.userId(), command.amount());

        return payNo;
    }
}
```

文件位置：`src/main/java/io/github/atengk/design/service/impl/NoticeServiceImpl.java`

下面是通知服务实现，负责发送下单通知。

```java
package io.github.atengk.design.service.impl;

import io.github.atengk.design.service.NoticeService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

/**
 * 通知服务实现
 *
 * @author Ateng
 * @since 2026-04-30
 */
@Slf4j
@Service
public class NoticeServiceImpl implements NoticeService {

    /**
     * 发送下单通知
     *
     * @param userId  用户ID
     * @param orderNo 订单号
     */
    @Override
    public void sendOrderCreatedNotice(Long userId, String orderNo) {
        log.info("发送下单通知，用户ID：{}，订单号：{}", userId, orderNo);
    }
}
```

文件位置：`src/main/java/io/github/atengk/design/facade/OrderCheckoutFacade.java`

下面是下单结算外观类。它负责组织多个子系统调用，并对 Controller 暴露一个简洁入口。

```java
package io.github.atengk.design.facade;

import cn.hutool.core.util.NumberUtil;
import cn.hutool.core.util.StrUtil;
import io.github.atengk.design.dto.OrderCheckoutRequest;
import io.github.atengk.design.dto.OrderCheckoutResponse;
import io.github.atengk.design.dto.OrderCreateCommand;
import io.github.atengk.design.dto.PaymentCreateCommand;
import io.github.atengk.design.service.InventoryService;
import io.github.atengk.design.service.NoticeService;
import io.github.atengk.design.service.OrderService;
import io.github.atengk.design.service.PaymentService;
import io.github.atengk.design.service.UserService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;

/**
 * 下单结算外观
 *
 * @author Ateng
 * @since 2026-04-30
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class OrderCheckoutFacade {

    private final UserService userService;
    private final InventoryService inventoryService;
    private final OrderService orderService;
    private final PaymentService paymentService;
    private final NoticeService noticeService;

    /**
     * 提交订单
     *
     * @param request 下单结算请求
     * @return 下单结算响应
     */
    public OrderCheckoutResponse checkout(OrderCheckoutRequest request) {
        validateRequest(request);

        BigDecimal totalAmount = NumberUtil.mul(request.unitPrice(), BigDecimal.valueOf(request.quantity()))
                .setScale(2, RoundingMode.HALF_UP);

        log.info("开始执行下单结算，用户ID：{}，商品ID：{}，数量：{}，金额：{}",
                request.userId(), request.productId(), request.quantity(), totalAmount);

        userService.checkUserCanOrder(request.userId());
        inventoryService.deductStock(request.productId(), request.quantity());

        OrderCreateCommand orderCreateCommand = new OrderCreateCommand(
                request.userId(),
                request.productId(),
                request.productName(),
                request.quantity(),
                totalAmount
        );
        String orderNo = orderService.createOrder(orderCreateCommand);

        PaymentCreateCommand paymentCreateCommand = new PaymentCreateCommand(
                orderNo,
                request.userId(),
                totalAmount
        );
        String payNo = paymentService.createPayment(paymentCreateCommand);

        noticeService.sendOrderCreatedNotice(request.userId(), orderNo);

        log.info("下单结算完成，订单号：{}，支付单号：{}，用户ID：{}", orderNo, payNo, request.userId());

        return new OrderCheckoutResponse(
                orderNo,
                payNo,
                request.userId(),
                request.productName(),
                request.quantity(),
                totalAmount,
                "下单成功"
        );
    }

    /**
     * 校验下单结算请求
     *
     * @param request 下单结算请求
     */
    private void validateRequest(OrderCheckoutRequest request) {
        if (request == null) {
            log.warn("下单失败，请求参数为空");
            throw new IllegalArgumentException("请求参数不能为空");
        }

        if (request.userId() == null || request.userId() <= 0) {
            log.warn("下单失败，用户ID不合法，用户ID：{}", request.userId());
            throw new IllegalArgumentException("用户ID必须大于0");
        }

        if (request.productId() == null || request.productId() <= 0) {
            log.warn("下单失败，商品ID不合法，商品ID：{}", request.productId());
            throw new IllegalArgumentException("商品ID必须大于0");
        }

        if (StrUtil.isBlank(request.productName())) {
            log.warn("下单失败，商品名称为空");
            throw new IllegalArgumentException("商品名称不能为空");
        }

        if (request.quantity() == null || request.quantity() <= 0) {
            log.warn("下单失败，购买数量不合法，购买数量：{}", request.quantity());
            throw new IllegalArgumentException("购买数量必须大于0");
        }

        if (request.unitPrice() == null || request.unitPrice().compareTo(BigDecimal.ZERO) <= 0) {
            log.warn("下单失败，商品单价不合法，商品单价：{}", request.unitPrice());
            throw new IllegalArgumentException("商品单价必须大于0");
        }
    }
}
```

文件位置：`src/main/java/io/github/atengk/design/controller/OrderCheckoutController.java`

下面是下单结算接口。Controller 只调用外观类，不直接编排多个子系统。

```java
package io.github.atengk.design.controller;

import io.github.atengk.design.dto.OrderCheckoutRequest;
import io.github.atengk.design.dto.OrderCheckoutResponse;
import io.github.atengk.design.facade.OrderCheckoutFacade;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;

/**
 * 下单结算控制器
 *
 * @author Ateng
 * @since 2026-04-30
 */
@RestController
@RequiredArgsConstructor
@RequestMapping("/facade/order")
public class OrderCheckoutController {

    private final OrderCheckoutFacade orderCheckoutFacade;

    /**
     * 提交订单
     *
     * @param userId      用户ID
     * @param productId   商品ID
     * @param productName 商品名称
     * @param quantity    购买数量
     * @param unitPrice   商品单价
     * @return 下单结算响应
     */
    @PostMapping("/checkout")
    public OrderCheckoutResponse checkout(@RequestParam Long userId,
                                          @RequestParam Long productId,
                                          @RequestParam String productName,
                                          @RequestParam Integer quantity,
                                          @RequestParam BigDecimal unitPrice) {
        OrderCheckoutRequest request = new OrderCheckoutRequest(
                userId,
                productId,
                productName,
                quantity,
                unitPrice
        );
        return orderCheckoutFacade.checkout(request);
    }
}
```

接口调用示例：

```bash
curl -X POST "http://localhost:8080/facade/order/checkout?userId=10001&productId=20001&productName=键盘&quantity=2&unitPrice=199.00"
```

可能返回：

```json
{
  "orderNo": "ORDER2019776866538487808",
  "payNo": "PAY2019776866538487809",
  "userId": 10001,
  "productName": "键盘",
  "quantity": 2,
  "totalAmount": 398.00,
  "message": "下单成功"
}
```

这种方式的优点是 Controller 保持轻量，多个子系统的调用顺序和数据转换集中在 `OrderCheckoutFacade` 中，后续如果下单流程增加风控、优惠、积分、发票等步骤，也可以在外观层统一编排。

## 扩展外观流程

在 Spring Boot 外观模式中，扩展流程通常是在外观类中新增对子系统的编排，而不是让 Controller 直接调用更多 Service。下面以新增优惠计算为例。

### 文件结构

```text
src/main/java/io/github/atengk/design/
└── service/
    ├── DiscountService.java
    └── impl/
        └── DiscountServiceImpl.java
```

文件位置：`src/main/java/io/github/atengk/design/service/DiscountService.java`

下面是优惠服务接口。

```java
package io.github.atengk.design.service;

import java.math.BigDecimal;

/**
 * 优惠服务
 *
 * @author Ateng
 * @since 2026-04-30
 */
public interface DiscountService {

    /**
     * 计算优惠后金额
     *
     * @param userId         用户ID
     * @param originalAmount 原始金额
     * @return 优惠后金额
     */
    BigDecimal calculateDiscountAmount(Long userId, BigDecimal originalAmount);
}
```

文件位置：`src/main/java/io/github/atengk/design/service/impl/DiscountServiceImpl.java`

下面是优惠服务实现，示例中订单满 100 减 20。

```java
package io.github.atengk.design.service.impl;

import cn.hutool.core.util.NumberUtil;
import io.github.atengk.design.service.DiscountService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;

/**
 * 优惠服务实现
 *
 * @author Ateng
 * @since 2026-04-30
 */
@Slf4j
@Service
public class DiscountServiceImpl implements DiscountService {

    private static final BigDecimal THRESHOLD_AMOUNT = BigDecimal.valueOf(100);
    private static final BigDecimal REDUCTION_AMOUNT = BigDecimal.valueOf(20);

    /**
     * 计算优惠后金额
     *
     * @param userId         用户ID
     * @param originalAmount 原始金额
     * @return 优惠后金额
     */
    @Override
    public BigDecimal calculateDiscountAmount(Long userId, BigDecimal originalAmount) {
        BigDecimal payableAmount = originalAmount;

        if (originalAmount.compareTo(THRESHOLD_AMOUNT) >= 0) {
            payableAmount = NumberUtil.sub(originalAmount, REDUCTION_AMOUNT);
        }

        payableAmount = payableAmount.setScale(2, RoundingMode.HALF_UP);
        log.info("计算优惠金额，用户ID：{}，原始金额：{}，优惠后金额：{}", userId, originalAmount, payableAmount);
        return payableAmount;
    }
}
```

在 `OrderCheckoutFacade` 中新增依赖：

```java
private final DiscountService discountService;
```

然后将原始金额计算后，增加优惠计算：

```java
BigDecimal originalAmount = NumberUtil.mul(request.unitPrice(), BigDecimal.valueOf(request.quantity()))
        .setScale(2, RoundingMode.HALF_UP);

BigDecimal totalAmount = discountService.calculateDiscountAmount(request.userId(), originalAmount);
```

扩展后，Controller 仍然只调用：

```java
return orderCheckoutFacade.checkout(request);
```

这体现了外观模式的价值：调用方入口稳定，内部流程可以持续演进。

## 外观模式和适配器模式的区别

外观模式和适配器模式都能隐藏复杂性，但关注点不同。

| 对比项           | 外观模式                         | 适配器模式                           |
| ---------------- | -------------------------------- | ------------------------------------ |
| 核心目的         | 简化复杂子系统调用               | 转换不兼容接口                       |
| 关注点           | 流程编排、统一入口               | 参数转换、接口兼容、结果转换         |
| 面向对象数量     | 通常封装多个子系统               | 通常适配一个外部对象或接口           |
| 是否改变接口语义 | 通常不强调转换                   | 强调转换成目标接口                   |
| 典型场景         | 下单结算、报表导出、文件处理流程 | 第三方支付、旧短信接口、多云存储接口 |

简单理解：

```text
外观模式：接口太多、流程太复杂，我给你一个简单入口。
适配器模式：接口不兼容，我帮你转换成能用的接口。
```

下单流程中调用用户、库存、订单、支付、通知多个子系统，更适合外观模式。把支付宝、微信、银联不同支付接口统一成内部支付接口，更适合适配器模式。

## 外观模式和模板方法模式的区别

外观模式和模板方法模式都可能组织一个业务流程，但二者结构不同。

| 对比项       | 外观模式               | 模板方法模式                 |
| ------------ | ---------------------- | ---------------------------- |
| 核心目的     | 简化外部调用           | 固定流程，延迟变化步骤到子类 |
| 实现方式     | 组合多个子系统         | 继承抽象父类                 |
| 扩展方式     | 增加或调整子系统编排   | 子类覆盖抽象步骤或钩子方法   |
| 流程控制位置 | 外观类                 | 抽象模板父类                 |
| 典型场景     | 下单门面、报表导出门面 | 文件导入模板、订单处理模板   |

简单理解：

```text
外观模式：我把多个服务调用封装成一个入口。
模板方法模式：我定义固定步骤，具体步骤由子类实现。
```

如果业务重点是降低调用方复杂度，优先考虑外观模式。如果业务重点是多个子类复用同一套固定流程，优先考虑模板方法模式。

## 验证方式

启动 Spring Boot 项目：

```bash
mvn spring-boot:run
```

执行下单结算接口：

```bash
curl -X POST "http://localhost:8080/facade/order/checkout?userId=10001&productId=20001&productName=键盘&quantity=2&unitPrice=199.00"
```

如果外观模式流程正常，可以看到类似日志：

```text
开始执行下单结算，用户ID：10001，商品ID：20001，数量：2，金额：398.00
用户下单校验通过，用户ID：10001
扣减库存成功，商品ID：20001，扣减数量：2
创建订单成功，订单号：ORDER2019776866538487808，用户ID：10001，商品ID：20001，金额：398.00
创建支付单成功，支付单号：PAY2019776866538487809，订单号：ORDER2019776866538487808，用户ID：10001，金额：398.00
发送下单通知，用户ID：10001，订单号：ORDER2019776866538487808
下单结算完成，订单号：ORDER2019776866538487808，支付单号：PAY2019776866538487809，用户ID：10001
```

执行异常请求：

```bash
curl -X POST "http://localhost:8080/facade/order/checkout?userId=10001&productId=20001&productName=键盘&quantity=0&unitPrice=199.00"
```

异常日志示例：

```text
下单失败，购买数量不合法，购买数量：0
```

实际项目中建议结合全局异常处理器，将业务异常转换成统一响应结构。

## 注意事项

外观模式适合简化复杂流程，但不要让外观类变成“上帝类”。外观类应该负责流程编排和数据组装，不应该承担所有子系统的业务细节。

推荐外观类承担这些职责：

```text
统一入口
请求参数校验
子系统调用顺序编排
子系统输入输出转换
聚合响应结果
记录关键流程日志
```

不推荐外观类承担这些职责：

```text
直接写复杂库存扣减算法
直接写支付渠道适配逻辑
直接写大量数据库 CRUD
直接写复杂营销规则
直接处理所有异常补偿细节
```

错误示例：

```java
public OrderCheckoutResponse checkout(OrderCheckoutRequest request) {
    // 校验用户
    // 查询用户表
    // 查询商品表
    // 扣库存SQL
    // 创建订单SQL
    // 创建支付SQL
    // 调第三方支付
    // 发短信
    // 发MQ
    // 写审计
    return null;
}
```

推荐将具体能力拆给子系统，外观类只做编排：

```java
userService.checkUserCanOrder(request.userId());
inventoryService.deductStock(request.productId(), request.quantity());
String orderNo = orderService.createOrder(command);
String payNo = paymentService.createPayment(paymentCommand);
noticeService.sendOrderCreatedNotice(request.userId(), orderNo);
```

Controller 不建议直接编排多个子系统。

不推荐写法：

```java
@PostMapping("/checkout")
public OrderCheckoutResponse checkout(...) {
    userService.checkUserCanOrder(userId);
    inventoryService.deductStock(productId, quantity);
    String orderNo = orderService.createOrder(command);
    String payNo = paymentService.createPayment(paymentCommand);
    noticeService.sendOrderCreatedNotice(userId, orderNo);
    return response;
}
```

推荐写法：

```java
@PostMapping("/checkout")
public OrderCheckoutResponse checkout(...) {
    OrderCheckoutRequest request = new OrderCheckoutRequest(userId, productId, productName, quantity, unitPrice);
    return orderCheckoutFacade.checkout(request);
}
```

Spring Bean 默认是单例，外观类中不要保存请求级状态。

错误示例：

```java
private String currentOrderNo;
private Long currentUserId;
private BigDecimal currentAmount;
```

推荐使用方法参数和局部变量：

```java
public OrderCheckoutResponse checkout(OrderCheckoutRequest request) {
    BigDecimal totalAmount = NumberUtil.mul(request.unitPrice(), BigDecimal.valueOf(request.quantity()));
    String orderNo = orderService.createOrder(command);
    return buildResponse(request, orderNo, totalAmount);
}
```

如果外观类编排的是强一致业务流程，例如下单、支付、扣库存，需要结合事务、幂等、分布式锁、消息队列、补偿任务等机制。外观模式只解决调用复杂度问题，不自动保证事务一致性。

生产环境中，下单流程通常需要额外考虑：

```text
接口幂等
库存并发扣减
订单事务提交
支付单唯一性
消息可靠投递
异常补偿
重复通知控制
日志链路追踪
```

## 总结

在 JDK21 和 Spring Boot 3 项目中，外观模式的实践重点是为复杂子系统提供一个简单、稳定、业务语义清晰的入口。

普通 Java 外观适合本地工具和简单流程封装。Spring Boot 外观适合下单结算、报表导出、文件处理、用户注册、支付聚合、后台管理聚合接口等场景。对于这些场景，推荐使用“Controller 调用 Facade，Facade 编排多个 Service，Service 负责具体能力”的结构。

外观模式不是为了把所有代码集中到一个类中，而是为了降低调用方复杂度，让业务入口清晰，让子系统职责独立，让复杂流程更容易维护和演进。
