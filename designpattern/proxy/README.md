# 代理模式

代理模式属于结构型模式，核心作用是通过代理对象控制对真实对象的访问。在当前设计模式文档体系中，代理模式位于结构型模式分类下，常见于权限控制、缓存代理、审计日志、远程调用、事务、AOP、接口限流等 Spring Boot 项目场景。

本文以 **JDK21 + Spring Boot 3** 后端项目为背景，通过“商品详情查询代理”的示例，说明代理模式在真实项目中的落地方式。

## 基础配置

本示例模拟一个商品详情查询接口。系统中真实的商品查询服务只负责查询商品数据，但接口调用前后还需要处理以下横切逻辑：

```text
查询前校验访问权限
查询前优先读取缓存
查询后写入缓存
查询后记录审计日志
异常时记录失败审计
```

如果把这些逻辑全部写进真实商品查询服务，会导致查询服务职责膨胀。后续如果缓存策略、权限规则、审计格式发生变化，也会影响核心查询逻辑。

代理模式的处理方式是：真实对象只负责核心业务，代理对象和真实对象实现相同接口。调用方注入接口时拿到的是代理对象，由代理对象决定是否访问真实对象，以及访问前后做哪些控制。

本示例需要以下依赖。

```xml
<dependencies>
    <!-- Spring Web：提供 REST API 能力 -->
    <dependency>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-starter-web</artifactId>
    </dependency>

    <!-- 参数校验：用于校验商品查询请求参数 -->
    <dependency>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-starter-validation</artifactId>
    </dependency>

    <!-- Hutool：用于字符串、集合、日期、ID、金额等常用工具处理 -->
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

    <!-- 配置元数据提示：让 IDE 能识别 @ConfigurationProperties -->
    <dependency>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-configuration-processor</artifactId>
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

product:
  proxy:
    cache-enabled: true # 是否启用商品详情缓存代理
    cache-expire-seconds: 60 # 本地缓存过期时间，单位秒
    audit-enabled: true # 是否启用查询审计日志
```

本示例的核心文件结构如下。

```text
src/main/java/io/github/atengk/designpattern/proxy
├── ProxyApplication.java
├── config
│   └── ProductProxyProperties.java
├── controller
│   └── ProductController.java
├── dto
│   └── ProductQueryRequest.java
├── proxy
│   ├── ProductQueryService.java
│   ├── RealProductQueryService.java
│   └── ProductQueryProxyService.java
├── repository
│   └── ProductMemoryRepository.java
├── service
│   ├── ProductAuditService.java
│   ├── ProductCacheService.java
│   └── ProductPermissionService.java
├── vo
│   ├── ApiResult.java
│   ├── ProductCacheStatsVO.java
│   └── ProductDetailVO.java
└── web
    └── GlobalExceptionHandler.java
```

## 模式设计

代理模式的关键是：调用方不直接访问真实对象，而是访问代理对象。代理对象可以在真实对象访问前后增加控制逻辑，也可以在某些情况下不访问真实对象。

本示例中的角色分工如下。

| 角色     | 示例类                     | 说明                              |
| -------- | -------------------------- | --------------------------------- |
| 抽象主题 | `ProductQueryService`      | 定义商品详情查询接口              |
| 真实主题 | `RealProductQueryService`  | 真正查询商品数据                  |
| 代理主题 | `ProductQueryProxyService` | 控制真实查询服务的访问            |
| 权限服务 | `ProductPermissionService` | 判断当前用户是否允许访问商品      |
| 缓存服务 | `ProductCacheService`      | 读取和写入商品详情缓存            |
| 审计服务 | `ProductAuditService`      | 记录查询成功或失败日志            |
| 调用方   | `ProductController`        | 只依赖 `ProductQueryService` 接口 |

核心流程如下。

```text
Controller
    ↓
ProductQueryService 接口
    ↓
ProductQueryProxyService 代理对象
    ↓
检查缓存
    ↓
缓存命中：校验权限后直接返回
    ↓
缓存未命中：调用 RealProductQueryService
    ↓
真实查询商品数据
    ↓
校验权限
    ↓
写入缓存
    ↓
记录审计日志
    ↓
返回商品详情
```

代理模式的重点不是“增强功能”本身，而是“控制访问”。缓存命中时，代理对象甚至可以不调用真实对象；权限不通过时，代理对象会直接拒绝访问真实对象的结果。

## 核心代码

下面给出代理模式在 Spring Boot 项目中的关键实现。示例使用内存仓储和本地缓存，真实项目中可以替换为 MyBatis-Plus、Redis、Caffeine、远程商品中心或 Elasticsearch。

项目启动类负责启动 Spring Boot 应用，并开启配置属性扫描。

文件位置：`src/main/java/io/github/atengk/designpattern/proxy/ProxyApplication.java`

```java
package io.github.atengk.designpattern.proxy;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;

/**
 * 代理模式示例应用启动类
 *
 * @author Ateng
 * @since 2026-05-13
 */
@ConfigurationPropertiesScan
@SpringBootApplication
public class ProxyApplication {

    /**
     * 应用启动入口
     *
     * @param args 启动参数
     */
    public static void main(String[] args) {
        SpringApplication.run(ProxyApplication.class, args);
    }
}
```

代理配置类用于控制缓存代理和审计代理是否启用。

文件位置：`src/main/java/io/github/atengk/designpattern/proxy/config/ProductProxyProperties.java`

```java
package io.github.atengk.designpattern.proxy.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * 商品查询代理配置
 *
 * @author Ateng
 * @since 2026-05-13
 */
@Data
@ConfigurationProperties(prefix = "product.proxy")
public class ProductProxyProperties {

    /**
     * 是否启用缓存代理
     */
    private Boolean cacheEnabled = Boolean.TRUE;

    /**
     * 缓存过期时间，单位秒
     */
    private Integer cacheExpireSeconds = 60;

    /**
     * 是否启用审计日志
     */
    private Boolean auditEnabled = Boolean.TRUE;
}
```

商品查询请求 DTO 用于承接接口查询参数。

文件位置：`src/main/java/io/github/atengk/designpattern/proxy/dto/ProductQueryRequest.java`

```java
package io.github.atengk.designpattern.proxy.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

/**
 * 商品详情查询请求
 *
 * @author Ateng
 * @since 2026-05-13
 */
@Data
public class ProductQueryRequest {

    /**
     * 当前用户 ID
     */
    @NotBlank(message = "用户 ID 不能为空")
    private String userId;

    /**
     * 商品 ID
     */
    @NotBlank(message = "商品 ID 不能为空")
    private String productId;

    /**
     * 是否强制刷新缓存
     */
    private Boolean forceRefresh = Boolean.FALSE;
}
```

商品详情 VO 表示最终返回给调用方的商品数据。

文件位置：`src/main/java/io/github/atengk/designpattern/proxy/vo/ProductDetailVO.java`

```java
package io.github.atengk.designpattern.proxy.vo;

import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;

/**
 * 商品详情返回对象
 *
 * @author Ateng
 * @since 2026-05-13
 */
@Data
@Builder
public class ProductDetailVO {

    /**
     * 商品 ID
     */
    private String productId;

    /**
     * 商品名称
     */
    private String productName;

    /**
     * 商品价格
     */
    private BigDecimal price;

    /**
     * 商品可见范围：PUBLIC、INTERNAL
     */
    private String visibleScope;

    /**
     * 销售状态
     */
    private String saleStatus;

    /**
     * 商品描述
     */
    private String description;

    /**
     * 数据来源：DB、CACHE
     */
    private String source;

    /**
     * 复制商品详情并替换来源
     *
     * @param source 数据来源
     * @return 商品详情
     */
    public ProductDetailVO copyWithSource(String source) {
        return ProductDetailVO.builder()
                .productId(productId)
                .productName(productName)
                .price(price)
                .visibleScope(visibleScope)
                .saleStatus(saleStatus)
                .description(description)
                .source(source)
                .build();
    }
}
```

缓存统计 VO 用于验证代理缓存是否生效。

文件位置：`src/main/java/io/github/atengk/designpattern/proxy/vo/ProductCacheStatsVO.java`

```java
package io.github.atengk.designpattern.proxy.vo;

import lombok.Builder;
import lombok.Data;

/**
 * 商品缓存统计返回对象
 *
 * @author Ateng
 * @since 2026-05-13
 */
@Data
@Builder
public class ProductCacheStatsVO {

    /**
     * 缓存数量
     */
    private Integer cacheSize;

    /**
     * 缓存是否启用
     */
    private Boolean cacheEnabled;

    /**
     * 缓存过期时间，单位秒
     */
    private Integer cacheExpireSeconds;
}
```

统一 API 返回对象用于包装接口响应。

文件位置：`src/main/java/io/github/atengk/designpattern/proxy/vo/ApiResult.java`

```java
package io.github.atengk.designpattern.proxy.vo;

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

商品内存仓储模拟商品数据库。真实项目中可以替换为 Mapper、Repository 或远程商品服务。

文件位置：`src/main/java/io/github/atengk/designpattern/proxy/repository/ProductMemoryRepository.java`

```java
package io.github.atengk.designpattern.proxy.repository;

import cn.hutool.core.map.MapUtil;
import io.github.atengk.designpattern.proxy.vo.ProductDetailVO;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.Map;

/**
 * 商品内存仓储
 *
 * @author Ateng
 * @since 2026-05-13
 */
@Repository
public class ProductMemoryRepository {

    private final Map<String, ProductDetailVO> productMap = MapUtil.<String, ProductDetailVO>builder(new HashMap<>())
            .put("product-1001", ProductDetailVO.builder()
                    .productId("product-1001")
                    .productName("JDK21 实战课程")
                    .price(new BigDecimal("99.90"))
                    .visibleScope("PUBLIC")
                    .saleStatus("ON_SALE")
                    .description("面向 Java 后端开发的 JDK21 实战课程")
                    .source("DB")
                    .build())
            .put("product-1002", ProductDetailVO.builder()
                    .productId("product-1002")
                    .productName("Spring Boot 3 内部项目课")
                    .price(new BigDecimal("199.90"))
                    .visibleScope("INTERNAL")
                    .saleStatus("ON_SALE")
                    .description("仅内部用户可见的 Spring Boot 3 项目课程")
                    .source("DB")
                    .build())
            .build();

    /**
     * 根据商品 ID 查询商品详情
     *
     * @param productId 商品 ID
     * @return 商品详情
     */
    public ProductDetailVO getByProductId(String productId) {
        ProductDetailVO product = productMap.get(productId);
        if (product == null) {
            throw new IllegalArgumentException("商品不存在：" + productId);
        }
        return product.copyWithSource("DB");
    }
}
```

抽象主题接口定义商品查询能力，代理对象和真实对象都实现这个接口。

文件位置：`src/main/java/io/github/atengk/designpattern/proxy/proxy/ProductQueryService.java`

```java
package io.github.atengk.designpattern.proxy.proxy;

import io.github.atengk.designpattern.proxy.dto.ProductQueryRequest;
import io.github.atengk.designpattern.proxy.vo.ProductDetailVO;

/**
 * 商品查询服务接口
 *
 * @author Ateng
 * @since 2026-05-13
 */
public interface ProductQueryService {

    /**
     * 查询商品详情
     *
     * @param request 商品查询请求
     * @return 商品详情
     */
    ProductDetailVO queryDetail(ProductQueryRequest request);
}
```

真实商品查询服务只负责核心查询，不处理缓存、权限、审计等代理逻辑。

文件位置：`src/main/java/io/github/atengk/designpattern/proxy/proxy/RealProductQueryService.java`

```java
package io.github.atengk.designpattern.proxy.proxy;

import io.github.atengk.designpattern.proxy.dto.ProductQueryRequest;
import io.github.atengk.designpattern.proxy.repository.ProductMemoryRepository;
import io.github.atengk.designpattern.proxy.vo.ProductDetailVO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

/**
 * 真实商品查询服务
 *
 * @author Ateng
 * @since 2026-05-13
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class RealProductQueryService implements ProductQueryService {

    private final ProductMemoryRepository productMemoryRepository;

    /**
     * 查询商品详情
     *
     * @param request 商品查询请求
     * @return 商品详情
     */
    @Override
    public ProductDetailVO queryDetail(ProductQueryRequest request) {
        log.info("访问真实商品查询服务，productId={}", request.getProductId());
        return productMemoryRepository.getByProductId(request.getProductId());
    }
}
```

权限服务用于控制当前用户是否可以查看目标商品。这里用简单规则模拟：`INTERNAL` 商品只有 `admin` 用户可以查看。

文件位置：`src/main/java/io/github/atengk/designpattern/proxy/service/ProductPermissionService.java`

```java
package io.github.atengk.designpattern.proxy.service;

import cn.hutool.core.util.StrUtil;
import io.github.atengk.designpattern.proxy.vo.ProductDetailVO;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

/**
 * 商品访问权限服务
 *
 * @author Ateng
 * @since 2026-05-13
 */
@Slf4j
@Service
public class ProductPermissionService {

    /**
     * 校验用户是否可以查看商品
     *
     * @param userId  用户 ID
     * @param product 商品详情
     */
    public void checkCanView(String userId, ProductDetailVO product) {
        if (StrUtil.equals("INTERNAL", product.getVisibleScope()) && !StrUtil.equals("admin", userId)) {
            throw new IllegalStateException("当前用户无权访问内部商品：" + product.getProductId());
        }

        log.info("商品访问权限校验通过，userId={}，productId={}", userId, product.getProductId());
    }
}
```

缓存服务用于模拟商品详情本地缓存。代理对象会优先访问缓存，缓存命中时可以避免访问真实对象。

文件位置：`src/main/java/io/github/atengk/designpattern/proxy/service/ProductCacheService.java`

```java
package io.github.atengk.designpattern.proxy.service;

import cn.hutool.core.util.BooleanUtil;
import io.github.atengk.designpattern.proxy.config.ProductProxyProperties;
import io.github.atengk.designpattern.proxy.vo.ProductCacheStatsVO;
import io.github.atengk.designpattern.proxy.vo.ProductDetailVO;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 商品详情缓存服务
 *
 * @author Ateng
 * @since 2026-05-13
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ProductCacheService {

    private final ProductProxyProperties productProxyProperties;
    private final Map<String, CacheValue> cache = new ConcurrentHashMap<>();

    /**
     * 从缓存中获取商品详情
     *
     * @param cacheKey 缓存 key
     * @return 商品详情
     */
    public Optional<ProductDetailVO> get(String cacheKey) {
        if (!BooleanUtil.isTrue(productProxyProperties.getCacheEnabled())) {
            return Optional.empty();
        }

        CacheValue cacheValue = cache.get(cacheKey);
        if (cacheValue == null) {
            log.info("商品详情缓存未命中，cacheKey={}", cacheKey);
            return Optional.empty();
        }

        if (cacheValue.getExpireTime().isBefore(LocalDateTime.now())) {
            cache.remove(cacheKey);
            log.info("商品详情缓存已过期，cacheKey={}", cacheKey);
            return Optional.empty();
        }

        log.info("商品详情缓存命中，cacheKey={}", cacheKey);
        return Optional.of(cacheValue.getValue().copyWithSource("CACHE"));
    }

    /**
     * 写入商品详情缓存
     *
     * @param cacheKey 缓存 key
     * @param value    商品详情
     */
    public void put(String cacheKey, ProductDetailVO value) {
        if (!BooleanUtil.isTrue(productProxyProperties.getCacheEnabled())) {
            return;
        }

        LocalDateTime expireTime = LocalDateTime.now()
                .plusSeconds(productProxyProperties.getCacheExpireSeconds());

        cache.put(cacheKey, new CacheValue(value.copyWithSource("DB"), expireTime));
        log.info("商品详情缓存写入成功，cacheKey={}，expireTime={}", cacheKey, expireTime);
    }

    /**
     * 查询缓存统计
     *
     * @return 缓存统计
     */
    public ProductCacheStatsVO stats() {
        return ProductCacheStatsVO.builder()
                .cacheSize(cache.size())
                .cacheEnabled(productProxyProperties.getCacheEnabled())
                .cacheExpireSeconds(productProxyProperties.getCacheExpireSeconds())
                .build();
    }

    /**
     * 缓存值
     *
     * @author Ateng
     * @since 2026-05-13
     */
    @Data
    @AllArgsConstructor
    private static class CacheValue {

        /**
         * 商品详情
         */
        private ProductDetailVO value;

        /**
         * 过期时间
         */
        private LocalDateTime expireTime;
    }
}
```

审计服务用于记录商品查询行为。代理对象会在查询成功、失败、缓存命中等场景中调用它。

文件位置：`src/main/java/io/github/atengk/designpattern/proxy/service/ProductAuditService.java`

```java
package io.github.atengk.designpattern.proxy.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

/**
 * 商品查询审计服务
 *
 * @author Ateng
 * @since 2026-05-13
 */
@Slf4j
@Service
public class ProductAuditService {

    /**
     * 记录商品查询审计日志
     *
     * @param userId    用户 ID
     * @param productId 商品 ID
     * @param success   是否成功
     * @param source    数据来源
     * @param message   审计消息
     * @param costMs    耗时，单位毫秒
     */
    public void record(String userId, String productId, Boolean success, String source, String message, Long costMs) {
        log.info("商品查询审计，userId={}，productId={}，success={}，source={}，message={}，costMs={}",
                userId, productId, success, source, message, costMs);
    }
}
```

代理商品查询服务是真正暴露给 Controller 的实现。它和真实服务实现相同接口，并通过 `@Primary` 让 Spring 在注入 `ProductQueryService` 时优先注入代理对象。

文件位置：`src/main/java/io/github/atengk/designpattern/proxy/proxy/ProductQueryProxyService.java`

```java
package io.github.atengk.designpattern.proxy.proxy;

import cn.hutool.core.util.BooleanUtil;
import cn.hutool.core.util.StrUtil;
import io.github.atengk.designpattern.proxy.config.ProductProxyProperties;
import io.github.atengk.designpattern.proxy.dto.ProductQueryRequest;
import io.github.atengk.designpattern.proxy.service.ProductAuditService;
import io.github.atengk.designpattern.proxy.service.ProductCacheService;
import io.github.atengk.designpattern.proxy.service.ProductPermissionService;
import io.github.atengk.designpattern.proxy.vo.ProductDetailVO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Service;

import java.util.Optional;

/**
 * 商品查询代理服务
 *
 * @author Ateng
 * @since 2026-05-13
 */
@Slf4j
@Primary
@Service
@RequiredArgsConstructor
public class ProductQueryProxyService implements ProductQueryService {

    private final RealProductQueryService realProductQueryService;
    private final ProductPermissionService productPermissionService;
    private final ProductCacheService productCacheService;
    private final ProductAuditService productAuditService;
    private final ProductProxyProperties productProxyProperties;

    /**
     * 查询商品详情
     *
     * @param request 商品查询请求
     * @return 商品详情
     */
    @Override
    public ProductDetailVO queryDetail(ProductQueryRequest request) {
        long start = System.currentTimeMillis();
        String cacheKey = buildCacheKey(request.getProductId());
        String source = "UNKNOWN";

        try {
            if (canReadCache(request)) {
                Optional<ProductDetailVO> cachedProduct = productCacheService.get(cacheKey);
                if (cachedProduct.isPresent()) {
                    ProductDetailVO product = cachedProduct.get();
                    productPermissionService.checkCanView(request.getUserId(), product);
                    source = product.getSource();

                    log.info("代理服务返回缓存商品详情，userId={}，productId={}",
                            request.getUserId(), request.getProductId());

                    audit(request, true, source, "缓存命中", start);
                    return product;
                }
            }

            ProductDetailVO product = realProductQueryService.queryDetail(request);
            productPermissionService.checkCanView(request.getUserId(), product);

            productCacheService.put(cacheKey, product);
            source = product.getSource();

            log.info("代理服务返回真实商品详情，userId={}，productId={}",
                    request.getUserId(), request.getProductId());

            audit(request, true, source, "真实服务查询成功", start);
            return product;
        } catch (RuntimeException exception) {
            audit(request, false, source, exception.getMessage(), start);
            log.warn("代理服务拦截到商品查询异常，userId={}，productId={}，message={}",
                    request.getUserId(), request.getProductId(), exception.getMessage());
            throw exception;
        }
    }

    /**
     * 判断是否可以读取缓存
     *
     * @param request 商品查询请求
     * @return 是否可以读取缓存
     */
    private boolean canReadCache(ProductQueryRequest request) {
        return BooleanUtil.isTrue(productProxyProperties.getCacheEnabled())
                && !BooleanUtil.isTrue(request.getForceRefresh());
    }

    /**
     * 构建缓存 key
     *
     * @param productId 商品 ID
     * @return 缓存 key
     */
    private String buildCacheKey(String productId) {
        return StrUtil.format("product:detail:{}", productId);
    }

    /**
     * 记录审计日志
     *
     * @param request 商品查询请求
     * @param success 是否成功
     * @param source  数据来源
     * @param message 审计消息
     * @param start   开始时间
     */
    private void audit(ProductQueryRequest request, Boolean success, String source, String message, long start) {
        if (!BooleanUtil.isTrue(productProxyProperties.getAuditEnabled())) {
            return;
        }

        productAuditService.record(
                request.getUserId(),
                request.getProductId(),
                success,
                source,
                message,
                System.currentTimeMillis() - start
        );
    }
}
```

Controller 只依赖 `ProductQueryService` 接口。由于代理服务使用了 `@Primary`，这里实际注入的是 `ProductQueryProxyService`。

文件位置：`src/main/java/io/github/atengk/designpattern/proxy/controller/ProductController.java`

```java
package io.github.atengk.designpattern.proxy.controller;

import io.github.atengk.designpattern.proxy.dto.ProductQueryRequest;
import io.github.atengk.designpattern.proxy.proxy.ProductQueryService;
import io.github.atengk.designpattern.proxy.service.ProductCacheService;
import io.github.atengk.designpattern.proxy.vo.ApiResult;
import io.github.atengk.designpattern.proxy.vo.ProductCacheStatsVO;
import io.github.atengk.designpattern.proxy.vo.ProductDetailVO;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

/**
 * 商品接口控制器
 *
 * @author Ateng
 * @since 2026-05-13
 */
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/products")
public class ProductController {

    private final ProductQueryService productQueryService;
    private final ProductCacheService productCacheService;

    /**
     * 查询商品详情
     *
     * @param request 商品查询请求
     * @return 商品详情
     */
    @PostMapping("/detail")
    public ApiResult<ProductDetailVO> queryDetail(@Valid @RequestBody ProductQueryRequest request) {
        return ApiResult.success(productQueryService.queryDetail(request));
    }

    /**
     * 查询商品缓存统计
     *
     * @return 商品缓存统计
     */
    @GetMapping("/cache/stats")
    public ApiResult<ProductCacheStatsVO> cacheStats() {
        return ApiResult.success(productCacheService.stats());
    }
}
```

全局异常处理器用于统一处理参数校验异常和业务异常。

文件位置：`src/main/java/io/github/atengk/designpattern/proxy/web/GlobalExceptionHandler.java`

```java
package io.github.atengk.designpattern.proxy.web;

import cn.hutool.core.util.StrUtil;
import io.github.atengk.designpattern.proxy.vo.ApiResult;
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
     * 处理业务状态异常
     *
     * @param exception 业务状态异常
     * @return API 返回对象
     */
    @ExceptionHandler(IllegalStateException.class)
    public ApiResult<Void> handleIllegalStateException(IllegalStateException exception) {
        log.warn("业务处理失败，message={}", exception.getMessage());
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

启动项目后，可以通过统一商品详情接口测试代理模式的访问控制、缓存和审计。

第一次查询公开商品，缓存未命中，会访问真实服务。

```bash
curl -X POST 'http://localhost:8080/api/products/detail' \
  -H 'Content-Type: application/json' \
  -d '{
    "userId": "user-1001",
    "productId": "product-1001",
    "forceRefresh": false
  }'
```

返回示例：

```json
{
  "code": 200,
  "message": "操作成功",
  "data": {
    "productId": "product-1001",
    "productName": "JDK21 实战课程",
    "price": 99.90,
    "visibleScope": "PUBLIC",
    "saleStatus": "ON_SALE",
    "description": "面向 Java 后端开发的 JDK21 实战课程",
    "source": "DB"
  }
}
```

第二次查询同一个商品，缓存命中，代理对象不会再访问真实商品查询服务。

```bash
curl -X POST 'http://localhost:8080/api/products/detail' \
  -H 'Content-Type: application/json' \
  -d '{
    "userId": "user-1001",
    "productId": "product-1001",
    "forceRefresh": false
  }'
```

返回示例：

```json
{
  "code": 200,
  "message": "操作成功",
  "data": {
    "productId": "product-1001",
    "productName": "JDK21 实战课程",
    "price": 99.90,
    "visibleScope": "PUBLIC",
    "saleStatus": "ON_SALE",
    "description": "面向 Java 后端开发的 JDK21 实战课程",
    "source": "CACHE"
  }
}
```

普通用户查询内部商品时，代理对象会拦截访问。

```bash
curl -X POST 'http://localhost:8080/api/products/detail' \
  -H 'Content-Type: application/json' \
  -d '{
    "userId": "user-1001",
    "productId": "product-1002",
    "forceRefresh": false
  }'
```

返回示例：

```json
{
  "code": 500,
  "message": "当前用户无权访问内部商品：product-1002",
  "data": null
}
```

管理员查询内部商品时可以正常访问。

```bash
curl -X POST 'http://localhost:8080/api/products/detail' \
  -H 'Content-Type: application/json' \
  -d '{
    "userId": "admin",
    "productId": "product-1002",
    "forceRefresh": false
  }'
```

查看缓存统计：

```bash
curl -X GET 'http://localhost:8080/api/products/cache/stats'
```

返回示例：

```json
{
  "code": 200,
  "message": "操作成功",
  "data": {
    "cacheSize": 2,
    "cacheEnabled": true,
    "cacheExpireSeconds": 60
  }
}
```

## 验证方式

可以从下面几个角度验证代理模式是否落地成功。

第一，Controller 只依赖接口，不直接依赖真实服务。`ProductController` 注入的是 `ProductQueryService`，实际运行时由 Spring 注入 `@Primary` 标记的 `ProductQueryProxyService`。

第二，真实服务只负责核心查询。`RealProductQueryService` 只从仓储查询商品详情，不处理权限、缓存和审计。

第三，代理对象可以控制是否访问真实对象。缓存命中时，`ProductQueryProxyService` 直接返回缓存数据，不调用 `RealProductQueryService`。

第四，代理对象可以拒绝访问。普通用户访问内部商品时，代理对象会抛出异常并记录审计日志。

可以重点查看日志：

```text
商品详情缓存未命中，cacheKey=product:detail:product-1001
访问真实商品查询服务，productId=product-1001
商品访问权限校验通过，userId=user-1001，productId=product-1001
商品详情缓存写入成功，cacheKey=product:detail:product-1001，expireTime=2026-05-13T10:31:00
商品查询审计，userId=user-1001，productId=product-1001，success=true，source=DB，message=真实服务查询成功，costMs=12

商品详情缓存命中，cacheKey=product:detail:product-1001
商品访问权限校验通过，userId=user-1001，productId=product-1001
商品查询审计，userId=user-1001，productId=product-1001，success=true，source=CACHE，message=缓存命中，costMs=2
```

## 使用 Spring AOP 动态代理

上面的示例属于静态代理：手动编写 `ProductQueryProxyService`。在 Spring Boot 项目中，事务、缓存、权限注解、日志切面通常使用 Spring AOP 动态代理完成。

如果希望通过注解方式代理商品查询，可以新增一个审计注解。

文件位置：`src/main/java/io/github/atengk/designpattern/proxy/aop/ProductQueryAudit.java`

```java
package io.github.atengk.designpattern.proxy.aop;

import java.lang.annotation.*;

/**
 * 商品查询审计注解
 *
 * @author Ateng
 * @since 2026-05-13
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface ProductQueryAudit {

    /**
     * 业务名称
     *
     * @return 业务名称
     */
    String value() default "商品查询";
}
```

然后新增 AOP 切面。

文件位置：`src/main/java/io/github/atengk/designpattern/proxy/aop/ProductQueryAuditAspect.java`

```java
package io.github.atengk.designpattern.proxy.aop;

import io.github.atengk.designpattern.proxy.dto.ProductQueryRequest;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.*;
import org.springframework.stereotype.Component;

/**
 * 商品查询审计切面
 *
 * @author Ateng
 * @since 2026-05-13
 */
@Slf4j
@Aspect
@Component
public class ProductQueryAuditAspect {

    /**
     * 环绕记录商品查询审计日志
     *
     * @param joinPoint 切点
     * @param audit     审计注解
     * @return 方法返回值
     * @throws Throwable 目标方法异常
     */
    @Around("@annotation(audit)")
    public Object around(ProceedingJoinPoint joinPoint, ProductQueryAudit audit) throws Throwable {
        long start = System.currentTimeMillis();
        ProductQueryRequest request = findRequest(joinPoint.getArgs());

        try {
            Object result = joinPoint.proceed();
            log.info("AOP 商品查询审计成功，business={}，userId={}，productId={}，costMs={}",
                    audit.value(),
                    request == null ? null : request.getUserId(),
                    request == null ? null : request.getProductId(),
                    System.currentTimeMillis() - start);
            return result;
        } catch (Throwable throwable) {
            log.warn("AOP 商品查询审计失败，business={}，userId={}，productId={}，message={}，costMs={}",
                    audit.value(),
                    request == null ? null : request.getUserId(),
                    request == null ? null : request.getProductId(),
                    throwable.getMessage(),
                    System.currentTimeMillis() - start);
            throw throwable;
        }
    }

    /**
     * 从参数列表中查找商品查询请求
     *
     * @param args 方法参数
     * @return 商品查询请求
     */
    private ProductQueryRequest findRequest(Object[] args) {
        if (args == null) {
            return null;
        }

        for (Object arg : args) {
            if (arg instanceof ProductQueryRequest request) {
                return request;
            }
        }

        return null;
    }
}
```

使用方式是在目标方法上添加注解：

```java
@ProductQueryAudit("商品详情查询")
@Override
public ProductDetailVO queryDetail(ProductQueryRequest request) {
    return productMemoryRepository.getByProductId(request.getProductId());
}
```

Spring AOP 动态代理更适合通用横切逻辑，例如日志、事务、权限注解、监控埋点。静态代理更适合业务语义明确、代理流程需要手动编排的场景。

## 适用场景

代理模式适合“调用方不应该直接访问真实对象，访问过程需要被控制”的场景。

常见 Spring Boot 项目场景如下。

| 场景         | 真实对象             | 代理职责               |
| ------------ | -------------------- | ---------------------- |
| 商品详情查询 | 商品查询服务         | 缓存、权限、审计       |
| 文件下载     | 文件存储服务         | 权限校验、防盗链、限流 |
| 远程接口调用 | 第三方 API 客户端    | 超时、重试、熔断、签名 |
| 支付接口     | 支付渠道客户端       | 幂等、验签、风控、日志 |
| 数据访问     | Mapper 或 Repository | 事务、读写分离、缓存   |
| 后台操作     | 业务管理服务         | 操作权限、审计日志     |
| 敏感数据查询 | 用户资料服务         | 脱敏、授权、访问记录   |

代理模式尤其适合以下特征明显的模块：

```text
真实对象职责需要保持纯粹
调用真实对象前需要校验或控制
调用真实对象后需要审计或转换
部分场景可以不访问真实对象
调用方不应该感知代理细节
```

## 和其他模式的区别

代理模式容易和装饰器模式、适配器模式、外观模式、责任链模式混淆。区分时重点看模式解决的问题。

| 模式       | 关注点                 | 和代理模式的区别                           |
| ---------- | ---------------------- | ------------------------------------------ |
| 代理模式   | 控制真实对象访问       | 重点是访问控制、缓存、远程代理、权限、事务 |
| 装饰器模式 | 动态增强对象能力       | 重点是功能叠加，通常不强调控制访问         |
| 适配器模式 | 转换不兼容接口         | 重点是接口转换，不是控制访问               |
| 外观模式   | 简化复杂子系统调用     | 重点是统一入口，不一定和真实对象同接口     |
| 责任链模式 | 多个处理器顺序处理请求 | 重点是链式传递，节点之间通常是同级处理器   |

本示例中，`ProductQueryProxyService` 代理 `RealProductQueryService`，并决定是否访问真实查询服务，所以更符合代理模式。虽然它也做了缓存和审计增强，但核心目的仍然是控制对真实商品查询服务的访问。

## 注意事项

代理对象和真实对象通常应该实现相同接口。这样调用方只依赖抽象接口，不需要知道自己拿到的是代理对象还是真实对象。

代理逻辑不要侵入真实对象。真实对象应该保持核心职责，例如本示例中真实服务只负责商品查询。权限、缓存、审计交给代理处理。

缓存代理要注意权限边界。即使缓存命中，也不能绕过权限校验。示例中缓存命中后仍然调用 `ProductPermissionService.checkCanView()`，避免普通用户通过缓存访问内部商品。

代理类不要过度膨胀。如果代理中同时堆积权限、缓存、限流、熔断、审计、脱敏、加密等大量逻辑，可以进一步拆分为多个协作服务，或改用 AOP、责任链、过滤器等结构。

Spring AOP 代理有边界。默认情况下，同一个类内部方法自调用不会触发代理；`final` 类、`final` 方法也可能影响代理增强。涉及事务、缓存、权限注解时要特别注意代理是否真正生效。

不要把代理模式和缓存工具混为一谈。缓存只是代理对象可以做的一种访问控制手段，代理模式的核心是代理对象代替调用方访问真实对象。

## 总结

代理模式的核心价值是通过代理对象控制真实对象的访问，让真实对象保持核心职责，访问控制逻辑集中在代理层。

在本示例中：

```text
ProductQueryService 定义统一查询接口
RealProductQueryService 负责真实商品查询
ProductQueryProxyService 负责缓存、权限校验和审计日志
ProductController 只依赖接口，不感知代理细节
```

最终效果是：

```text
真实查询服务职责更纯粹
调用方不直接访问真实对象
缓存命中时可以避免真实查询
权限不通过时可以拒绝访问
查询审计逻辑集中在代理层
代码结构更清晰，也更容易扩展和维护
```
