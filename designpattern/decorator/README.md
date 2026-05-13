# 装饰器模式

装饰器模式属于结构型模式，核心作用是在不修改原对象代码的前提下，动态给对象增加额外能力。在当前设计模式文档体系中，装饰器模式位于结构型模式分类下，适合日志增强、数据脱敏、缓存增强、压缩处理、结果包装、权限校验等 Spring Boot 项目场景。

本文以 **JDK21 + Spring Boot 3** 后端项目为背景，通过“订单导出文件增强”的示例，说明装饰器模式在真实项目中的落地方式。

## 基础配置

本示例模拟一个订单导出模块。系统原本只有一个基础导出能力：把订单数据导出成文本文件。后续业务提出了多个增强需求：

```text
导出结果需要追加汇总信息
导出内容需要对手机号脱敏
导出文件内容需要压缩
导出操作需要记录审计日志
```

如果把这些能力全部写进基础导出类，会导致导出类职责膨胀。后续每新增一种增强能力，都要修改原类，容易破坏已有逻辑。

装饰器模式的处理方式是：基础导出类只负责导出本身，每个增强能力单独定义一个装饰器。装饰器和原对象实现相同接口，可以一层一层包裹原对象。

本示例需要以下依赖。

```xml
<dependencies>
    <!-- Spring Web：提供 REST API 能力 -->
    <dependency>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-starter-web</artifactId>
    </dependency>

    <!-- 参数校验：用于校验导出请求参数 -->
    <dependency>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-starter-validation</artifactId>
    </dependency>

    <!-- Hutool：用于字符串、集合、压缩、日期、ID 等工具处理 -->
    <dependency>
        <groupId>cn.hutool</groupId>
        <artifactId>hutool-all</artifactId>
        <version>${hutool.version}</version>
    </dependency>

    <!-- Lombok：减少 DTO、VO、构造器等样板代码 -->
    <dependency>
        <groupId>org.projectlombok</groupId>
        <artifactId>lombok</artifactId>
        <optional>true</optional>
    </dependency>

    <!-- 测试依赖：用于单元测试和接口测试 -->
    <dependency>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-starter-test</artifactId>
        <scope>test</scope>
    </dependency>
</dependencies>
```

示例项目配置如下。

```yaml
server:
  port: 8080 # 示例服务端口

export:
  audit:
    enabled: true # 是否启用导出审计日志
```

本示例的核心文件结构如下。

```text
src/main/java/io/github/atengk/designpattern/decorator
├── DecoratorApplication.java
├── config
│   └── ExportProperties.java
├── controller
│   └── OrderExportController.java
├── decorator
│   ├── OrderExportComponent.java
│   ├── BasicOrderExportComponent.java
│   ├── AbstractOrderExportDecorator.java
│   ├── SummaryOrderExportDecorator.java
│   ├── DesensitizeOrderExportDecorator.java
│   ├── CompressOrderExportDecorator.java
│   ├── AuditOrderExportDecorator.java
│   └── OrderExportDecoratorFactory.java
├── dto
│   └── OrderExportRequest.java
├── repository
│   └── OrderMemoryRepository.java
├── service
│   ├── OrderExportService.java
│   └── OrderExportServiceImpl.java
├── vo
│   ├── ApiResult.java
│   ├── OrderExportResultVO.java
│   └── OrderRecordVO.java
└── web
    └── GlobalExceptionHandler.java
```

## 模式设计

装饰器模式要求装饰器和被装饰对象实现相同接口。这样调用方只依赖统一接口，不关心当前对象是原始对象，还是被一层或多层装饰后的对象。

本示例中的角色分工如下。

| 角色       | 示例类                            | 说明                                     |
| ---------- | --------------------------------- | ---------------------------------------- |
| 抽象组件   | `OrderExportComponent`            | 定义订单导出的统一接口                   |
| 具体组件   | `BasicOrderExportComponent`       | 基础导出能力，只负责生成原始导出内容     |
| 抽象装饰器 | `AbstractOrderExportDecorator`    | 持有一个 `OrderExportComponent` 委托对象 |
| 具体装饰器 | `SummaryOrderExportDecorator`     | 给导出结果追加汇总信息                   |
| 具体装饰器 | `DesensitizeOrderExportDecorator` | 对导出内容进行手机号脱敏                 |
| 具体装饰器 | `CompressOrderExportDecorator`    | 对导出内容进行压缩                       |
| 具体装饰器 | `AuditOrderExportDecorator`       | 记录导出审计日志                         |
| 装饰器工厂 | `OrderExportDecoratorFactory`     | 根据请求参数动态组装装饰链               |

核心流程如下。

```text
Controller
    ↓
OrderExportService
    ↓
OrderExportDecoratorFactory 构建装饰链
    ↓
AuditOrderExportDecorator
    ↓
CompressOrderExportDecorator
    ↓
DesensitizeOrderExportDecorator
    ↓
SummaryOrderExportDecorator
    ↓
BasicOrderExportComponent
    ↓
返回增强后的导出结果
```

装饰器模式的关键点是：增强能力不是写死在基础类中，而是通过包装对象的方式叠加上去。

## 核心代码

下面给出装饰器模式在 Spring Boot 项目中的关键实现。示例使用内存仓储模拟订单数据，真实项目中可以替换为 MyBatis-Plus、JPA 或远程订单服务。

项目启动类负责启动 Spring Boot 应用，并开启配置属性扫描。

文件位置：`src/main/java/io/github/atengk/designpattern/decorator/DecoratorApplication.java`

```java
package io.github.atengk.designpattern.decorator;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;

/**
 * 装饰器模式示例应用启动类
 *
 * @author Ateng
 * @since 2026-05-13
 */
@ConfigurationPropertiesScan
@SpringBootApplication
public class DecoratorApplication {

    /**
     * 应用启动入口
     *
     * @param args 启动参数
     */
    public static void main(String[] args) {
        SpringApplication.run(DecoratorApplication.class, args);
    }
}
```

导出配置类用于控制审计日志等开关。

文件位置：`src/main/java/io/github/atengk/designpattern/decorator/config/ExportProperties.java`

```java
package io.github.atengk.designpattern.decorator.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * 导出功能配置
 *
 * @author Ateng
 * @since 2026-05-13
 */
@Data
@ConfigurationProperties(prefix = "export")
public class ExportProperties {

    /**
     * 审计配置
     */
    private Audit audit = new Audit();

    /**
     * 审计配置项
     *
     * @author Ateng
     * @since 2026-05-13
     */
    @Data
    public static class Audit {

        /**
         * 是否启用审计日志
         */
        private Boolean enabled = Boolean.TRUE;
    }
}
```

导出请求 DTO 用于控制本次导出需要叠加哪些增强能力。

文件位置：`src/main/java/io/github/atengk/designpattern/decorator/dto/OrderExportRequest.java`

```java
package io.github.atengk.designpattern.decorator.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

/**
 * 订单导出请求
 *
 * @author Ateng
 * @since 2026-05-13
 */
@Data
public class OrderExportRequest {

    /**
     * 操作人
     */
    @NotBlank(message = "操作人不能为空")
    private String operator;

    /**
     * 是否追加汇总信息
     */
    private Boolean summary = Boolean.FALSE;

    /**
     * 是否对敏感信息脱敏
     */
    private Boolean desensitize = Boolean.FALSE;

    /**
     * 是否压缩导出内容
     */
    private Boolean compress = Boolean.FALSE;
}
```

订单记录 VO 用于模拟订单数据。

文件位置：`src/main/java/io/github/atengk/designpattern/decorator/vo/OrderRecordVO.java`

```java
package io.github.atengk.designpattern.decorator.vo;

import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;

/**
 * 订单记录返回对象
 *
 * @author Ateng
 * @since 2026-05-13
 */
@Data
@Builder
public class OrderRecordVO {

    /**
     * 订单号
     */
    private String orderNo;

    /**
     * 用户名称
     */
    private String userName;

    /**
     * 手机号
     */
    private String phone;

    /**
     * 商品名称
     */
    private String productName;

    /**
     * 订单金额
     */
    private BigDecimal amount;
}
```

订单导出结果 VO 是所有装饰器共同操作的结果对象。

文件位置：`src/main/java/io/github/atengk/designpattern/decorator/vo/OrderExportResultVO.java`

```java
package io.github.atengk.designpattern.decorator.vo;

import lombok.Builder;
import lombok.Data;

import java.util.List;

/**
 * 订单导出结果返回对象
 *
 * @author Ateng
 * @since 2026-05-13
 */
@Data
@Builder
public class OrderExportResultVO {

    /**
     * 导出文件名
     */
    private String fileName;

    /**
     * 文件内容
     */
    private String content;

    /**
     * 是否已压缩
     */
    private Boolean compressed;

    /**
     * 文件大小，单位字节
     */
    private Integer size;

    /**
     * 应用过的增强能力
     */
    private List<String> decorators;
}
```

统一 API 返回对象用于包装接口响应。

文件位置：`src/main/java/io/github/atengk/designpattern/decorator/vo/ApiResult.java`

```java
package io.github.atengk.designpattern.decorator.vo;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * API 统一返回对象
 *
 * @author Ateng
 * @since 2026-05-13
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ApiResult<T> {

    /**
     * 业务状态码
     */
    private Integer code;

    /**
     * 返回消息
     */
    private String message;

    /**
     * 返回数据
     */
    private T data;

    /**
     * 成功返回
     *
     * @param data 返回数据
     * @return API 返回对象
     */
    public static <T> ApiResult<T> success(T data) {
        return ApiResult.<T>builder()
                .code(200)
                .message("操作成功")
                .data(data)
                .build();
    }

    /**
     * 失败返回
     *
     * @param message 失败消息
     * @return API 返回对象
     */
    public static ApiResult<Void> fail(String message) {
        return ApiResult.<Void>builder()
                .code(500)
                .message(message)
                .build();
    }
}
```

订单内存仓储用于模拟数据库查询。真实项目中可以替换为 Mapper 或 Repository。

文件位置：`src/main/java/io/github/atengk/designpattern/decorator/repository/OrderMemoryRepository.java`

```java
package io.github.atengk.designpattern.decorator.repository;

import io.github.atengk.designpattern.decorator.vo.OrderRecordVO;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.util.List;

/**
 * 订单内存仓储
 *
 * @author Ateng
 * @since 2026-05-13
 */
@Repository
public class OrderMemoryRepository {

    /**
     * 查询订单记录
     *
     * @return 订单记录列表
     */
    public List<OrderRecordVO> listOrders() {
        return List.of(
                OrderRecordVO.builder()
                        .orderNo("ORDER202605130001")
                        .userName("张三")
                        .phone("13800000001")
                        .productName("JDK21 实战课程")
                        .amount(new BigDecimal("99.90"))
                        .build(),
                OrderRecordVO.builder()
                        .orderNo("ORDER202605130002")
                        .userName("李四")
                        .phone("13800000002")
                        .productName("Spring Boot 3 项目课程")
                        .amount(new BigDecimal("199.90"))
                        .build(),
                OrderRecordVO.builder()
                        .orderNo("ORDER202605130003")
                        .userName("王五")
                        .phone("13800000003")
                        .productName("设计模式专题课程")
                        .amount(new BigDecimal("66.60"))
                        .build()
        );
    }
}
```

`OrderExportComponent` 是装饰器模式中的抽象组件。基础导出类和所有装饰器都实现这个接口。

文件位置：`src/main/java/io/github/atengk/designpattern/decorator/decorator/OrderExportComponent.java`

```java
package io.github.atengk.designpattern.decorator.decorator;

import io.github.atengk.designpattern.decorator.dto.OrderExportRequest;
import io.github.atengk.designpattern.decorator.vo.OrderExportResultVO;

/**
 * 订单导出组件接口
 *
 * @author Ateng
 * @since 2026-05-13
 */
public interface OrderExportComponent {

    /**
     * 导出订单
     *
     * @param request 订单导出请求
     * @return 订单导出结果
     */
    OrderExportResultVO export(OrderExportRequest request);
}
```

基础导出组件只负责生成原始订单导出内容，不包含脱敏、压缩、汇总、审计等增强逻辑。

文件位置：`src/main/java/io/github/atengk/designpattern/decorator/decorator/BasicOrderExportComponent.java`

```java
package io.github.atengk.designpattern.decorator.decorator;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.date.DateUtil;
import io.github.atengk.designpattern.decorator.dto.OrderExportRequest;
import io.github.atengk.designpattern.decorator.repository.OrderMemoryRepository;
import io.github.atengk.designpattern.decorator.vo.OrderExportResultVO;
import io.github.atengk.designpattern.decorator.vo.OrderRecordVO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

/**
 * 基础订单导出组件
 *
 * @author Ateng
 * @since 2026-05-13
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class BasicOrderExportComponent implements OrderExportComponent {

    private final OrderMemoryRepository orderMemoryRepository;

    /**
     * 导出订单基础内容
     *
     * @param request 订单导出请求
     * @return 订单导出结果
     */
    @Override
    public OrderExportResultVO export(OrderExportRequest request) {
        List<OrderRecordVO> orders = orderMemoryRepository.listOrders();
        List<String> lines = new ArrayList<>();

        lines.add("订单号,用户名称,手机号,商品名称,订单金额");
        for (OrderRecordVO order : orders) {
            lines.add(String.join(",",
                    order.getOrderNo(),
                    order.getUserName(),
                    order.getPhone(),
                    order.getProductName(),
                    order.getAmount().toPlainString()
            ));
        }

        String content = CollUtil.join(lines, "\n");
        String fileName = "order-export-" + DateUtil.format(DateUtil.date(), "yyyyMMddHHmmss") + ".csv";

        log.info("生成基础订单导出文件成功，operator={}，fileName={}，rows={}",
                request.getOperator(), fileName, orders.size());

        return OrderExportResultVO.builder()
                .fileName(fileName)
                .content(content)
                .compressed(Boolean.FALSE)
                .size(content.getBytes(StandardCharsets.UTF_8).length)
                .decorators(new ArrayList<>())
                .build();
    }
}
```

抽象装饰器持有一个 `OrderExportComponent`，并默认把请求委托给被装饰对象处理。

文件位置：`src/main/java/io/github/atengk/designpattern/decorator/decorator/AbstractOrderExportDecorator.java`

```java
package io.github.atengk.designpattern.decorator.decorator;

import io.github.atengk.designpattern.decorator.dto.OrderExportRequest;
import io.github.atengk.designpattern.decorator.vo.OrderExportResultVO;
import lombok.RequiredArgsConstructor;

/**
 * 订单导出抽象装饰器
 *
 * @author Ateng
 * @since 2026-05-13
 */
@RequiredArgsConstructor
public abstract class AbstractOrderExportDecorator implements OrderExportComponent {

    /**
     * 被装饰的订单导出组件
     */
    protected final OrderExportComponent delegate;

    /**
     * 导出订单
     *
     * @param request 订单导出请求
     * @return 订单导出结果
     */
    @Override
    public OrderExportResultVO export(OrderExportRequest request) {
        return delegate.export(request);
    }

    /**
     * 记录已应用的装饰器名称
     *
     * @param result        导出结果
     * @param decoratorName 装饰器名称
     */
    protected void addDecorator(OrderExportResultVO result, String decoratorName) {
        result.getDecorators().add(decoratorName);
    }
}
```

汇总信息装饰器在基础导出内容后追加统计信息。

文件位置：`src/main/java/io/github/atengk/designpattern/decorator/decorator/SummaryOrderExportDecorator.java`

```java
package io.github.atengk.designpattern.decorator.decorator;

import cn.hutool.core.util.StrUtil;
import io.github.atengk.designpattern.decorator.dto.OrderExportRequest;
import io.github.atengk.designpattern.decorator.vo.OrderExportResultVO;
import lombok.extern.slf4j.Slf4j;

import java.nio.charset.StandardCharsets;

/**
 * 订单导出汇总信息装饰器
 *
 * @author Ateng
 * @since 2026-05-13
 */
@Slf4j
public class SummaryOrderExportDecorator extends AbstractOrderExportDecorator {

    /**
     * 创建汇总信息装饰器
     *
     * @param delegate 被装饰的订单导出组件
     */
    public SummaryOrderExportDecorator(OrderExportComponent delegate) {
        super(delegate);
    }

    /**
     * 导出订单并追加汇总信息
     *
     * @param request 订单导出请求
     * @return 订单导出结果
     */
    @Override
    public OrderExportResultVO export(OrderExportRequest request) {
        OrderExportResultVO result = super.export(request);
        int dataRows = Math.max(StrUtil.split(result.getContent(), "\n").size() - 1, 0);

        String newContent = result.getContent()
                + "\n"
                + StrUtil.format("汇总信息,共导出 {} 条订单,操作人 {}", dataRows, request.getOperator());

        result.setContent(newContent);
        result.setSize(newContent.getBytes(StandardCharsets.UTF_8).length);
        addDecorator(result, "SUMMARY");

        log.info("订单导出追加汇总信息成功，operator={}，rows={}", request.getOperator(), dataRows);
        return result;
    }
}
```

脱敏装饰器对导出内容中的手机号进行脱敏处理。它不关心基础导出内容如何生成，只处理最终内容。

文件位置：`src/main/java/io/github/atengk/designpattern/decorator/decorator/DesensitizeOrderExportDecorator.java`

```java
package io.github.atengk.designpattern.decorator.decorator;

import cn.hutool.core.util.DesensitizedUtil;
import io.github.atengk.designpattern.decorator.dto.OrderExportRequest;
import io.github.atengk.designpattern.decorator.vo.OrderExportResultVO;
import lombok.extern.slf4j.Slf4j;

import java.nio.charset.StandardCharsets;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 订单导出数据脱敏装饰器
 *
 * @author Ateng
 * @since 2026-05-13
 */
@Slf4j
public class DesensitizeOrderExportDecorator extends AbstractOrderExportDecorator {

    private static final Pattern PHONE_PATTERN = Pattern.compile("1[3-9]\\d{9}");

    /**
     * 创建数据脱敏装饰器
     *
     * @param delegate 被装饰的订单导出组件
     */
    public DesensitizeOrderExportDecorator(OrderExportComponent delegate) {
        super(delegate);
    }

    /**
     * 导出订单并对敏感字段脱敏
     *
     * @param request 订单导出请求
     * @return 订单导出结果
     */
    @Override
    public OrderExportResultVO export(OrderExportRequest request) {
        OrderExportResultVO result = super.export(request);
        String content = result.getContent();

        Matcher matcher = PHONE_PATTERN.matcher(content);
        StringBuilder builder = new StringBuilder();
        while (matcher.find()) {
            String phone = matcher.group();
            matcher.appendReplacement(builder, DesensitizedUtil.mobilePhone(phone));
        }
        matcher.appendTail(builder);

        String newContent = builder.toString();
        result.setContent(newContent);
        result.setSize(newContent.getBytes(StandardCharsets.UTF_8).length);
        addDecorator(result, "DESENSITIZE");

        log.info("订单导出数据脱敏成功，operator={}", request.getOperator());
        return result;
    }
}
```

压缩装饰器对导出内容进行 Gzip 压缩，并用 Base64 字符串表示压缩后的内容，便于接口示例直接返回。

文件位置：`src/main/java/io/github/atengk/designpattern/decorator/decorator/CompressOrderExportDecorator.java`

```java
package io.github.atengk.designpattern.decorator.decorator;

import cn.hutool.core.codec.Base64;
import cn.hutool.core.util.ZipUtil;
import io.github.atengk.designpattern.decorator.dto.OrderExportRequest;
import io.github.atengk.designpattern.decorator.vo.OrderExportResultVO;
import lombok.extern.slf4j.Slf4j;

import java.nio.charset.StandardCharsets;

/**
 * 订单导出压缩装饰器
 *
 * @author Ateng
 * @since 2026-05-13
 */
@Slf4j
public class CompressOrderExportDecorator extends AbstractOrderExportDecorator {

    /**
     * 创建压缩装饰器
     *
     * @param delegate 被装饰的订单导出组件
     */
    public CompressOrderExportDecorator(OrderExportComponent delegate) {
        super(delegate);
    }

    /**
     * 导出订单并压缩内容
     *
     * @param request 订单导出请求
     * @return 订单导出结果
     */
    @Override
    public OrderExportResultVO export(OrderExportRequest request) {
        OrderExportResultVO result = super.export(request);

        byte[] rawBytes = result.getContent().getBytes(StandardCharsets.UTF_8);
        byte[] gzipBytes = ZipUtil.gzip(rawBytes);
        String base64Content = Base64.encode(gzipBytes);

        result.setFileName(result.getFileName() + ".gz");
        result.setContent(base64Content);
        result.setCompressed(Boolean.TRUE);
        result.setSize(gzipBytes.length);
        addDecorator(result, "COMPRESS");

        log.info("订单导出内容压缩成功，operator={}，rawSize={}，gzipSize={}",
                request.getOperator(), rawBytes.length, gzipBytes.length);

        return result;
    }
}
```

审计装饰器记录导出操作日志。它可以包裹整个装饰链，从而记录最终文件名、压缩状态和增强能力。

文件位置：`src/main/java/io/github/atengk/designpattern/decorator/decorator/AuditOrderExportDecorator.java`

```java
package io.github.atengk.designpattern.decorator.decorator;

import io.github.atengk.designpattern.decorator.dto.OrderExportRequest;
import io.github.atengk.designpattern.decorator.vo.OrderExportResultVO;
import lombok.extern.slf4j.Slf4j;

/**
 * 订单导出审计日志装饰器
 *
 * @author Ateng
 * @since 2026-05-13
 */
@Slf4j
public class AuditOrderExportDecorator extends AbstractOrderExportDecorator {

    /**
     * 创建审计日志装饰器
     *
     * @param delegate 被装饰的订单导出组件
     */
    public AuditOrderExportDecorator(OrderExportComponent delegate) {
        super(delegate);
    }

    /**
     * 导出订单并记录审计日志
     *
     * @param request 订单导出请求
     * @return 订单导出结果
     */
    @Override
    public OrderExportResultVO export(OrderExportRequest request) {
        log.info("订单导出审计开始，operator={}", request.getOperator());

        OrderExportResultVO result = super.export(request);
        addDecorator(result, "AUDIT");

        log.info("订单导出审计完成，operator={}，fileName={}，compressed={}，decorators={}",
                request.getOperator(), result.getFileName(), result.getCompressed(), result.getDecorators());

        return result;
    }
}
```

装饰器工厂根据请求参数动态组装装饰链。这里可以清楚看到每个增强能力都是独立叠加的。

文件位置：`src/main/java/io/github/atengk/designpattern/decorator/decorator/OrderExportDecoratorFactory.java`

```java
package io.github.atengk.designpattern.decorator.decorator;

import cn.hutool.core.util.BooleanUtil;
import io.github.atengk.designpattern.decorator.config.ExportProperties;
import io.github.atengk.designpattern.decorator.dto.OrderExportRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * 订单导出装饰器工厂
 *
 * @author Ateng
 * @since 2026-05-13
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class OrderExportDecoratorFactory {

    private final BasicOrderExportComponent basicOrderExportComponent;
    private final ExportProperties exportProperties;

    /**
     * 根据请求参数构建订单导出组件
     *
     * @param request 订单导出请求
     * @return 订单导出组件
     */
    public OrderExportComponent build(OrderExportRequest request) {
        OrderExportComponent component = basicOrderExportComponent;

        if (BooleanUtil.isTrue(request.getSummary())) {
            component = new SummaryOrderExportDecorator(component);
            log.info("订单导出装饰器已启用：SUMMARY");
        }

        if (BooleanUtil.isTrue(request.getDesensitize())) {
            component = new DesensitizeOrderExportDecorator(component);
            log.info("订单导出装饰器已启用：DESENSITIZE");
        }

        if (BooleanUtil.isTrue(request.getCompress())) {
            component = new CompressOrderExportDecorator(component);
            log.info("订单导出装饰器已启用：COMPRESS");
        }

        if (BooleanUtil.isTrue(exportProperties.getAudit().getEnabled())) {
            component = new AuditOrderExportDecorator(component);
            log.info("订单导出装饰器已启用：AUDIT");
        }

        return component;
    }
}
```

订单导出服务接口定义业务入口。

文件位置：`src/main/java/io/github/atengk/designpattern/decorator/service/OrderExportService.java`

```java
package io.github.atengk.designpattern.decorator.service;

import io.github.atengk.designpattern.decorator.dto.OrderExportRequest;
import io.github.atengk.designpattern.decorator.vo.OrderExportResultVO;

/**
 * 订单导出服务接口
 *
 * @author Ateng
 * @since 2026-05-13
 */
public interface OrderExportService {

    /**
     * 导出订单
     *
     * @param request 订单导出请求
     * @return 订单导出结果
     */
    OrderExportResultVO export(OrderExportRequest request);
}
```

服务实现类只负责获取装饰后的导出组件并调用统一接口，不关心具体装饰器的执行细节。

文件位置：`src/main/java/io/github/atengk/designpattern/decorator/service/OrderExportServiceImpl.java`

```java
package io.github.atengk.designpattern.decorator.service;

import io.github.atengk.designpattern.decorator.decorator.OrderExportComponent;
import io.github.atengk.designpattern.decorator.decorator.OrderExportDecoratorFactory;
import io.github.atengk.designpattern.decorator.dto.OrderExportRequest;
import io.github.atengk.designpattern.decorator.vo.OrderExportResultVO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

/**
 * 订单导出服务实现类
 *
 * @author Ateng
 * @since 2026-05-13
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class OrderExportServiceImpl implements OrderExportService {

    private final OrderExportDecoratorFactory orderExportDecoratorFactory;

    /**
     * 导出订单
     *
     * @param request 订单导出请求
     * @return 订单导出结果
     */
    @Override
    public OrderExportResultVO export(OrderExportRequest request) {
        log.info("开始执行订单导出，operator={}，summary={}，desensitize={}，compress={}",
                request.getOperator(), request.getSummary(), request.getDesensitize(), request.getCompress());

        OrderExportComponent component = orderExportDecoratorFactory.build(request);
        OrderExportResultVO result = component.export(request);

        log.info("订单导出完成，operator={}，fileName={}，size={}，decorators={}",
                request.getOperator(), result.getFileName(), result.getSize(), result.getDecorators());

        return result;
    }
}
```

Controller 对外提供订单导出接口。

文件位置：`src/main/java/io/github/atengk/designpattern/decorator/controller/OrderExportController.java`

```java
package io.github.atengk.designpattern.decorator.controller;

import io.github.atengk.designpattern.decorator.dto.OrderExportRequest;
import io.github.atengk.designpattern.decorator.service.OrderExportService;
import io.github.atengk.designpattern.decorator.vo.ApiResult;
import io.github.atengk.designpattern.decorator.vo.OrderExportResultVO;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

/**
 * 订单导出接口控制器
 *
 * @author Ateng
 * @since 2026-05-13
 */
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/orders/export")
public class OrderExportController {

    private final OrderExportService orderExportService;

    /**
     * 导出订单
     *
     * @param request 订单导出请求
     * @return 订单导出结果
     */
    @PostMapping
    public ApiResult<OrderExportResultVO> export(@Valid @RequestBody OrderExportRequest request) {
        return ApiResult.success(orderExportService.export(request));
    }
}
```

全局异常处理器用于统一处理参数校验异常和业务异常。

文件位置：`src/main/java/io/github/atengk/designpattern/decorator/web/GlobalExceptionHandler.java`

```java
package io.github.atengk.designpattern.decorator.web;

import cn.hutool.core.util.StrUtil;
import io.github.atengk.designpattern.decorator.vo.ApiResult;
import lombok.extern.slf4j.Slf4j;
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
     * 处理参数校验异常
     *
     * @param exception 参数校验异常
     * @return API 返回对象
     */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ApiResult<Void> handleValidException(MethodArgumentNotValidException exception) {
        String message = exception.getBindingResult()
                .getFieldErrors()
                .stream()
                .findFirst()
                .map(error -> StrUtil.format("{} {}", error.getField(), error.getDefaultMessage()))
                .orElse("请求参数不合法");

        log.warn("请求参数校验失败，message={}", message);
        return ApiResult.fail(message);
    }

    /**
     * 处理非法参数异常
     *
     * @param exception 非法参数异常
     * @return API 返回对象
     */
    @ExceptionHandler(IllegalArgumentException.class)
    public ApiResult<Void> handleIllegalArgumentException(IllegalArgumentException exception) {
        log.warn("请求参数错误，message={}", exception.getMessage());
        return ApiResult.fail(exception.getMessage());
    }

    /**
     * 处理系统异常
     *
     * @param exception 系统异常
     * @return API 返回对象
     */
    @ExceptionHandler(Exception.class)
    public ApiResult<Void> handleException(Exception exception) {
        log.error("系统处理异常", exception);
        return ApiResult.fail("系统处理异常");
    }
}
```

## 使用方式

启动项目后，可以通过统一接口测试不同增强能力的组合。

只执行基础导出：

```bash
curl -X POST 'http://localhost:8080/api/orders/export' \
  -H 'Content-Type: application/json' \
  -d '{
    "operator": "admin",
    "summary": false,
    "desensitize": false,
    "compress": false
  }'
```

返回示例：

```json
{
  "code": 200,
  "message": "操作成功",
  "data": {
    "fileName": "order-export-20260513103000.csv",
    "content": "订单号,用户名称,手机号,商品名称,订单金额\nORDER202605130001,张三,13800000001,JDK21 实战课程,99.90\nORDER202605130002,李四,13800000002,Spring Boot 3 项目课程,199.90\nORDER202605130003,王五,13800000003,设计模式专题课程,66.60",
    "compressed": false,
    "size": 287,
    "decorators": [
      "AUDIT"
    ]
  }
}
```

执行汇总和脱敏增强：

```bash
curl -X POST 'http://localhost:8080/api/orders/export' \
  -H 'Content-Type: application/json' \
  -d '{
    "operator": "admin",
    "summary": true,
    "desensitize": true,
    "compress": false
  }'
```

返回示例：

```json
{
  "code": 200,
  "message": "操作成功",
  "data": {
    "fileName": "order-export-20260513103100.csv",
    "content": "订单号,用户名称,手机号,商品名称,订单金额\nORDER202605130001,张三,138****0001,JDK21 实战课程,99.90\nORDER202605130002,李四,138****0002,Spring Boot 3 项目课程,199.90\nORDER202605130003,王五,138****0003,设计模式专题课程,66.60\n汇总信息,共导出 3 条订单,操作人 admin",
    "compressed": false,
    "size": 322,
    "decorators": [
      "SUMMARY",
      "DESENSITIZE",
      "AUDIT"
    ]
  }
}
```

执行汇总、脱敏、压缩增强：

```bash
curl -X POST 'http://localhost:8080/api/orders/export' \
  -H 'Content-Type: application/json' \
  -d '{
    "operator": "admin",
    "summary": true,
    "desensitize": true,
    "compress": true
  }'
```

返回示例：

```json
{
  "code": 200,
  "message": "操作成功",
  "data": {
    "fileName": "order-export-20260513103200.csv.gz",
    "content": "H4sIAAAAAAAA...",
    "compressed": true,
    "size": 180,
    "decorators": [
      "SUMMARY",
      "DESENSITIZE",
      "COMPRESS",
      "AUDIT"
    ]
  }
}
```

## 验证方式

可以从下面几个角度验证装饰器模式是否落地成功。

第一，基础导出类没有被增强逻辑污染。`BasicOrderExportComponent` 只负责生成原始订单导出内容，不处理脱敏、压缩、汇总和审计。

第二，增强能力可以自由组合。请求中只启用 `summary` 时，只追加汇总信息；同时启用 `summary`、`desensitize`、`compress` 时，会依次叠加三个增强能力。

第三，调用方只依赖统一接口。`OrderExportServiceImpl` 调用的是 `OrderExportComponent.export()`，不需要判断当前组件到底是基础组件还是装饰后的组件。

第四，新增增强能力不需要修改基础导出类。例如新增“加密导出”时，只需要新增 `EncryptOrderExportDecorator`，再在工厂中按配置包裹即可。

可以重点查看日志：

```text
开始执行订单导出，operator=admin，summary=true，desensitize=true，compress=true
订单导出装饰器已启用：SUMMARY
订单导出装饰器已启用：DESENSITIZE
订单导出装饰器已启用：COMPRESS
订单导出装饰器已启用：AUDIT
生成基础订单导出文件成功，operator=admin，fileName=order-export-20260513103200.csv，rows=3
订单导出追加汇总信息成功，operator=admin，rows=3
订单导出数据脱敏成功，operator=admin
订单导出内容压缩成功，operator=admin，rawSize=322，gzipSize=180
订单导出审计完成，operator=admin，fileName=order-export-20260513103200.csv.gz，compressed=true，decorators=[SUMMARY, DESENSITIZE, COMPRESS, AUDIT]
```

## 扩展新装饰器

如果要新增“导出内容加密”能力，可以新增一个加密装饰器。

文件位置：`src/main/java/io/github/atengk/designpattern/decorator/decorator/EncryptOrderExportDecorator.java`

```java
package io.github.atengk.designpattern.decorator.decorator;

import cn.hutool.crypto.SecureUtil;
import io.github.atengk.designpattern.decorator.dto.OrderExportRequest;
import io.github.atengk.designpattern.decorator.vo.OrderExportResultVO;
import lombok.extern.slf4j.Slf4j;

import java.nio.charset.StandardCharsets;

/**
 * 订单导出加密装饰器
 *
 * @author Ateng
 * @since 2026-05-13
 */
@Slf4j
public class EncryptOrderExportDecorator extends AbstractOrderExportDecorator {

    /**
     * 创建加密装饰器
     *
     * @param delegate 被装饰的订单导出组件
     */
    public EncryptOrderExportDecorator(OrderExportComponent delegate) {
        super(delegate);
    }

    /**
     * 导出订单并加密内容
     *
     * @param request 订单导出请求
     * @return 订单导出结果
     */
    @Override
    public OrderExportResultVO export(OrderExportRequest request) {
        OrderExportResultVO result = super.export(request);
        String encryptedContent = SecureUtil.md5(result.getContent());

        result.setContent(encryptedContent);
        result.setSize(encryptedContent.getBytes(StandardCharsets.UTF_8).length);
        addDecorator(result, "ENCRYPT");

        log.info("订单导出内容加密成功，operator={}", request.getOperator());
        return result;
    }
}
```

然后在 `OrderExportRequest` 中增加开关：

```java
/**
 * 是否加密导出内容
 */
private Boolean encrypt = Boolean.FALSE;
```

再在 `OrderExportDecoratorFactory` 中追加装饰逻辑：

```java
if (BooleanUtil.isTrue(request.getEncrypt())) {
    component = new EncryptOrderExportDecorator(component);
    log.info("订单导出装饰器已启用：ENCRYPT");
}
```

生产环境中不建议用 MD5 表示可逆加密，这里只是示例。真实导出文件加密应使用 AES、SM4 或文件级加密方案，并妥善管理密钥。

## 适用场景

装饰器模式适合“主体能力稳定，但附加能力经常变化或需要灵活组合”的场景。

常见 Spring Boot 项目场景如下。

| 场景     | 基础能力     | 装饰增强                                 |
| -------- | ------------ | ---------------------------------------- |
| 文件导出 | 生成文件     | 脱敏、压缩、加密、水印、审计             |
| 接口返回 | 返回业务数据 | 字段脱敏、结果包装、缓存、日志           |
| 消息发送 | 发送消息     | 重试、限流、签名、审计、失败告警         |
| 文件上传 | 保存文件     | 病毒扫描、格式校验、图片压缩、元数据记录 |
| 查询服务 | 查询数据     | 缓存、权限过滤、脱敏、监控打点           |
| 支付请求 | 发起支付     | 幂等校验、签名、风控、审计               |

装饰器模式尤其适合这些特征明显的模块：

```text
基础对象职责比较稳定
增强能力可能多个同时启用
增强能力有先后顺序
不希望修改原始类
不希望通过继承制造大量子类
```

## 和其他模式的区别

装饰器模式容易和代理模式、适配器模式、责任链模式混淆。区分时重点看模式解决的问题。

| 模式       | 关注点                 | 和装饰器模式的区别                           |
| ---------- | ---------------------- | -------------------------------------------- |
| 装饰器模式 | 动态增强对象能力       | 重点是给原对象叠加新功能，通常保持同一接口   |
| 代理模式   | 控制对象访问           | 重点是访问控制、远程代理、缓存代理、事务代理 |
| 适配器模式 | 转换不兼容接口         | 重点是接口转换，不是功能增强                 |
| 责任链模式 | 多个处理器顺序处理请求 | 重点是请求沿链传递，某个节点可中断或转交     |
| 策略模式   | 替换不同算法或处理逻辑 | 重点是选择一个实现，不是层层叠加             |

本示例中，脱敏、压缩、汇总、审计都是围绕“订单导出”这个基础能力做增强，而且可以组合叠加，因此更适合使用装饰器模式。

## 注意事项

装饰器的顺序非常重要。例如本示例中，通常应该先追加汇总，再脱敏，最后压缩。如果先压缩，再脱敏，脱敏装饰器面对的就是 Base64 压缩内容，无法再识别手机号。

装饰器不要承担过多业务主流程。基础导出组件负责核心导出，装饰器负责附加能力。如果装饰器里开始处理订单状态、库存扣减、支付逻辑，说明职责边界已经失控。

装饰器层级不要过深。过多装饰器会增加调试成本。生产项目中可以通过日志、调用链追踪或在返回对象中记录已启用装饰器，帮助排查问题。

Spring 项目中要避免 Bean 循环依赖。本文示例中装饰器对象由工厂手动 `new` 出来，基础组件由 Spring 管理，结构比较清晰。如果所有装饰器都交给 Spring 注入，需要特别注意 `@Primary`、`@Qualifier` 和装饰顺序。

不要把装饰器模式误用成万能扩展点。如果多个增强能力之间存在明显的条件流转、中断处理或责任传递，更适合使用责任链模式。如果只是对方法调用做统一拦截，AOP 或代理模式可能更直接。

## 总结

装饰器模式的核心价值是：在不修改原对象的前提下，通过包装对象动态增加功能。

在本示例中：

```text
OrderExportComponent 定义统一导出接口
BasicOrderExportComponent 负责基础订单导出
SummaryOrderExportDecorator 负责追加汇总信息
DesensitizeOrderExportDecorator 负责数据脱敏
CompressOrderExportDecorator 负责压缩内容
AuditOrderExportDecorator 负责审计日志
OrderExportDecoratorFactory 负责动态组装装饰链
```

最终效果是：

```text
基础导出逻辑保持稳定
增强能力独立拆分
多个增强能力可以自由组合
新增增强能力不需要修改基础类
调用方仍然面向统一接口编程
代码扩展性和可维护性更好
```
