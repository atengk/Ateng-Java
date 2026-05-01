# 设计模式：仓储模式

仓储模式用于隔离领域对象和数据持久化细节，让业务层面向仓储接口编程，而不是直接依赖 Mapper、DAO、JPA Repository、RedisTemplate 或第三方存储 API。在 JDK21 和 Spring Boot 3 项目中，仓储模式常用于 DDD 聚合持久化、复杂查询封装、跨数据源读取、缓存与数据库组合、领域对象与数据库实体解耦等场景。

需要注意：仓储模式不是 GoF 23 种设计模式之一，属于这次设计模式文档里的“遗漏补充”。它在 Spring Boot 后端项目中非常常见，尤其适合业务复杂度较高、领域模型和数据库模型不希望强绑定的项目。

## 基础配置

本示例基于 JDK21、Spring Boot 3、Maven、MyBatis-Plus、MySQL。示例包路径统一使用 `io.github.atengk`。

文件位置：`pom.xml`

```xml
<dependencies>
    <!-- Spring Boot Web，用于提供接口验证仓储模式行为 -->
    <dependency>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-starter-web</artifactId>
    </dependency>

    <!-- MyBatis-Plus Spring Boot 3 Starter，用于数据库 CRUD 和分页查询 -->
    <dependency>
        <groupId>com.baomidou</groupId>
        <artifactId>mybatis-plus-spring-boot3-starter</artifactId>
        <version>3.5.8</version>
    </dependency>

    <!-- MySQL 驱动，用于连接 MySQL 数据库 -->
    <dependency>
        <groupId>com.mysql</groupId>
        <artifactId>mysql-connector-j</artifactId>
        <scope>runtime</scope>
    </dependency>

    <!-- Hutool 工具类，用于字符串、ID、集合、金额等通用处理 -->
    <dependency>
        <groupId>cn.hutool</groupId>
        <artifactId>hutool-all</artifactId>
        <version>5.8.27</version>
    </dependency>

    <!-- Lombok，简化日志对象、Getter、Setter、构造方法等样板代码 -->
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

文件位置：`src/main/resources/application.yml`

```yaml
server:
  # 示例服务端口
  port: 8080

spring:
  datasource:
    # MySQL 连接地址，根据本地数据库调整
    url: jdbc:mysql://localhost:3306/design_demo?useUnicode=true&characterEncoding=utf8&serverTimezone=Asia/Shanghai&useSSL=false
    # 数据库用户名
    username: root
    # 数据库密码
    password: root
    # MySQL 驱动类
    driver-class-name: com.mysql.cj.jdbc.Driver

mybatis-plus:
  configuration:
    # 控制台输出 SQL，开发环境便于调试，生产环境建议关闭
    log-impl: org.apache.ibatis.logging.stdout.StdOutImpl
  global-config:
    db-config:
      # 主键策略，示例中使用雪花ID
      id-type: assign_id
```

文件位置：`sql/product.sql`

```sql
CREATE TABLE product (
    id BIGINT PRIMARY KEY COMMENT '商品ID',
    product_code VARCHAR(64) NOT NULL COMMENT '商品编码',
    product_name VARCHAR(100) NOT NULL COMMENT '商品名称',
    price DECIMAL(18, 2) NOT NULL COMMENT '商品价格',
    stock INT NOT NULL DEFAULT 0 COMMENT '库存数量',
    status VARCHAR(32) NOT NULL COMMENT '商品状态：DRAFT 草稿，ON_SHELF 上架，OFF_SHELF 下架',
    create_time DATETIME NOT NULL COMMENT '创建时间',
    update_time DATETIME NOT NULL COMMENT '更新时间',
    UNIQUE KEY uk_product_code (product_code),
    KEY idx_status (status)
) COMMENT='商品表';
```

这张表用于模拟商品聚合的持久化。业务层不直接操作 `product` 表，也不直接依赖 MyBatis-Plus 的 `BaseMapper`，而是通过 `ProductRepository` 访问商品聚合。

## 核心概念

仓储模式的核心目标是把“业务对象的存取”包装成一个类似集合的接口。业务层只关心“保存商品、按 ID 获取商品、分页查询商品”，不关心底层是 MySQL、Redis、Elasticsearch、远程接口，还是多个存储组合。

常见角色如下：

| 角色                      | 说明                                              |
| ------------------------- | ------------------------------------------------- |
| Domain Model              | 领域对象，承载业务状态和业务行为                  |
| Repository Interface      | 仓储接口，定义领域对象的存取能力                  |
| Repository Implementation | 仓储实现，封装 Mapper、缓存、远程接口等持久化细节 |
| Persistence Entity        | 持久化实体，和数据库表结构对应                    |
| Mapper / DAO              | 数据访问组件，负责具体 SQL 或 ORM 操作            |
| Application Service       | 应用服务，调用仓储完成业务用例                    |

典型结构如下：

```text
Controller
    -> ProductApplicationService
        -> ProductRepository
            -> ProductMapper
                -> product 表
```

仓储模式和 DAO、Mapper 的区别在于：

| 对比项             | Repository         | Mapper / DAO       |
| ------------------ | ------------------ | ------------------ |
| 面向对象           | 面向领域对象       | 面向数据库表或 SQL |
| 所在层次           | 业务与基础设施之间 | 数据访问层         |
| 返回对象           | 领域对象、聚合对象 | Entity、PO、DO     |
| 关注点             | 业务对象存取语义   | SQL、CRUD、表字段  |
| 是否隔离持久化细节 | 是                 | 不一定             |

简单理解：

```text
Mapper 关心表怎么查。
Repository 关心业务对象怎么存取。
```

在简单 CRUD 项目中，Service 直接调用 Mapper 也可以接受。但在业务复杂、领域模型和数据库表结构不完全一致、需要缓存、需要跨表聚合、需要隔离基础设施时，仓储模式更合适。

## 普通 Java 仓储模式

普通 Java 仓储模式适合先理解仓储接口和领域对象之间的关系。下面用内存 Map 模拟商品仓储，业务层只依赖 `ProductRepository`，不关心数据存在哪里。

### 文件结构

```text
src/main/java/io/github/atengk/design/repository/simple/
├── ProductStatus.java
├── Product.java
├── ProductRepository.java
└── InMemoryProductRepository.java
```

文件位置：`src/main/java/io/github/atengk/design/repository/simple/ProductStatus.java`

下面是商品状态枚举。

```java
package io.github.atengk.design.repository.simple;

/**
 * 商品状态
 *
 * @author Ateng
 * @since 2026-05-01
 */
public enum ProductStatus {

    /**
     * 草稿
     */
    DRAFT,

    /**
     * 上架
     */
    ON_SHELF,

    /**
     * 下架
     */
    OFF_SHELF
}
```

文件位置：`src/main/java/io/github/atengk/design/repository/simple/Product.java`

下面是商品领域对象。它不仅保存字段，也包含上架、下架、改价等业务行为。

```java
package io.github.atengk.design.repository.simple;

import cn.hutool.core.util.StrUtil;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

import java.math.BigDecimal;

/**
 * 商品领域对象
 *
 * @author Ateng
 * @since 2026-05-01
 */
@Slf4j
@Getter
public class Product {

    private final Long id;
    private final String productCode;
    private String productName;
    private BigDecimal price;
    private Integer stock;
    private ProductStatus status;

    /**
     * 创建商品领域对象
     *
     * @param id          商品ID
     * @param productCode 商品编码
     * @param productName 商品名称
     * @param price       商品价格
     * @param stock       库存数量
     */
    public Product(Long id, String productCode, String productName, BigDecimal price, Integer stock) {
        if (id == null || id <= 0) {
            throw new IllegalArgumentException("商品ID必须大于0");
        }
        if (StrUtil.hasBlank(productCode, productName)) {
            throw new IllegalArgumentException("商品编码和名称不能为空");
        }
        if (price == null || price.compareTo(BigDecimal.ZERO) < 0) {
            throw new IllegalArgumentException("商品价格不能小于0");
        }
        if (stock == null || stock < 0) {
            throw new IllegalArgumentException("库存数量不能小于0");
        }

        this.id = id;
        this.productCode = productCode;
        this.productName = productName;
        this.price = price;
        this.stock = stock;
        this.status = ProductStatus.DRAFT;
    }

    /**
     * 修改价格
     *
     * @param newPrice 新价格
     */
    public void changePrice(BigDecimal newPrice) {
        if (newPrice == null || newPrice.compareTo(BigDecimal.ZERO) < 0) {
            log.warn("修改商品价格失败，价格不合法，商品编码：{}，价格：{}", productCode, newPrice);
            throw new IllegalArgumentException("商品价格不能小于0");
        }

        this.price = newPrice;
        log.info("修改商品价格成功，商品编码：{}，新价格：{}", productCode, newPrice);
    }

    /**
     * 上架商品
     */
    public void putOnShelf() {
        if (stock <= 0) {
            log.warn("商品上架失败，库存不足，商品编码：{}，库存：{}", productCode, stock);
            throw new IllegalStateException("库存不足，不能上架");
        }

        this.status = ProductStatus.ON_SHELF;
        log.info("商品上架成功，商品编码：{}", productCode);
    }

    /**
     * 下架商品
     */
    public void takeOffShelf() {
        this.status = ProductStatus.OFF_SHELF;
        log.info("商品下架成功，商品编码：{}", productCode);
    }
}
```

文件位置：`src/main/java/io/github/atengk/design/repository/simple/ProductRepository.java`

下面是商品仓储接口。业务层只依赖这个接口。

```java
package io.github.atengk.design.repository.simple;

import java.util.List;
import java.util.Optional;

/**
 * 商品仓储接口
 *
 * @author Ateng
 * @since 2026-05-01
 */
public interface ProductRepository {

    /**
     * 保存商品
     *
     * @param product 商品领域对象
     */
    void save(Product product);

    /**
     * 根据商品ID查询商品
     *
     * @param productId 商品ID
     * @return 商品领域对象
     */
    Optional<Product> findById(Long productId);

    /**
     * 根据商品编码查询商品
     *
     * @param productCode 商品编码
     * @return 商品领域对象
     */
    Optional<Product> findByProductCode(String productCode);

    /**
     * 查询全部商品
     *
     * @return 商品列表
     */
    List<Product> findAll();

    /**
     * 删除商品
     *
     * @param productId 商品ID
     */
    void deleteById(Long productId);
}
```

文件位置：`src/main/java/io/github/atengk/design/repository/simple/InMemoryProductRepository.java`

下面是内存商品仓储实现。它隐藏了 `Map` 存储细节。

```java
package io.github.atengk.design.repository.simple;

import cn.hutool.core.util.StrUtil;
import lombok.extern.slf4j.Slf4j;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 内存商品仓储实现
 *
 * @author Ateng
 * @since 2026-05-01
 */
@Slf4j
public class InMemoryProductRepository implements ProductRepository {

    private final Map<Long, Product> productMap = new ConcurrentHashMap<>();

    /**
     * 保存商品
     *
     * @param product 商品领域对象
     */
    @Override
    public void save(Product product) {
        if (product == null) {
            log.warn("保存商品失败，商品为空");
            throw new IllegalArgumentException("商品不能为空");
        }

        productMap.put(product.getId(), product);
        log.info("保存商品成功，商品ID：{}，商品编码：{}", product.getId(), product.getProductCode());
    }

    /**
     * 根据商品ID查询商品
     *
     * @param productId 商品ID
     * @return 商品领域对象
     */
    @Override
    public Optional<Product> findById(Long productId) {
        if (productId == null || productId <= 0) {
            return Optional.empty();
        }

        return Optional.ofNullable(productMap.get(productId));
    }

    /**
     * 根据商品编码查询商品
     *
     * @param productCode 商品编码
     * @return 商品领域对象
     */
    @Override
    public Optional<Product> findByProductCode(String productCode) {
        if (StrUtil.isBlank(productCode)) {
            return Optional.empty();
        }

        return productMap.values().stream()
                .filter(product -> StrUtil.equals(product.getProductCode(), productCode))
                .findFirst();
    }

    /**
     * 查询全部商品
     *
     * @return 商品列表
     */
    @Override
    public List<Product> findAll() {
        return List.copyOf(productMap.values());
    }

    /**
     * 删除商品
     *
     * @param productId 商品ID
     */
    @Override
    public void deleteById(Long productId) {
        if (productId == null || productId <= 0) {
            return;
        }

        productMap.remove(productId);
        log.info("删除商品成功，商品ID：{}", productId);
    }
}
```

使用方式：

```java
ProductRepository productRepository = new InMemoryProductRepository();

Product product = new Product(1L, "P10001", "机械键盘", BigDecimal.valueOf(199.00), 100);
product.putOnShelf();

productRepository.save(product);

Product savedProduct = productRepository.findByProductCode("P10001")
        .orElseThrow(() -> new IllegalArgumentException("商品不存在"));
```

这里的业务代码不关心商品是存到 Map、MySQL、Redis 还是远程服务。只要仓储接口不变，底层实现可以替换。

## Spring Boot 仓储模式

Spring Boot 项目中，仓储模式更常见的写法是：领域对象和数据库实体分离，Service 依赖 Repository 接口，Repository 实现内部再使用 MyBatis-Plus Mapper。

整体结构如下：

```text
Controller
    -> ProductApplicationService
        -> ProductRepository
            -> ProductMapper
                -> product 表
```

### 文件结构

```text
src/main/java/io/github/atengk/design/
├── RepositoryApplication.java
├── controller/
│   └── ProductController.java
├── domain/
│   ├── Product.java
│   └── ProductStatus.java
├── dto/
│   ├── ProductCreateRequest.java
│   ├── ProductChangePriceRequest.java
│   ├── ProductPageQuery.java
│   ├── ProductResponse.java
│   └── PageResult.java
├── entity/
│   └── ProductEntity.java
├── mapper/
│   └── ProductMapper.java
├── repository/
│   ├── ProductRepository.java
│   └── impl/
│       └── MybatisProductRepository.java
└── service/
    ├── ProductApplicationService.java
    └── impl/
        └── ProductApplicationServiceImpl.java
```

文件位置：`src/main/java/io/github/atengk/design/RepositoryApplication.java`

下面是 Spring Boot 启动类，配置 MyBatis Mapper 扫描路径。

```java
package io.github.atengk.design;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * 仓储模式示例启动类
 *
 * @author Ateng
 * @since 2026-05-01
 */
@MapperScan("io.github.atengk.design.mapper")
@SpringBootApplication
public class RepositoryApplication {

    /**
     * 应用启动入口
     *
     * @param args 启动参数
     */
    public static void main(String[] args) {
        SpringApplication.run(RepositoryApplication.class, args);
    }
}
```

文件位置：`src/main/java/io/github/atengk/design/domain/ProductStatus.java`

下面是商品状态枚举。

```java
package io.github.atengk.design.domain;

/**
 * 商品状态
 *
 * @author Ateng
 * @since 2026-05-01
 */
public enum ProductStatus {

    /**
     * 草稿
     */
    DRAFT,

    /**
     * 上架
     */
    ON_SHELF,

    /**
     * 下架
     */
    OFF_SHELF
}
```

文件位置：`src/main/java/io/github/atengk/design/domain/Product.java`

下面是商品领域对象。它包含业务行为，不直接绑定数据库表注解。

```java
package io.github.atengk.design.domain;

import cn.hutool.core.util.StrUtil;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

import java.math.BigDecimal;

/**
 * 商品领域对象
 *
 * @author Ateng
 * @since 2026-05-01
 */
@Slf4j
@Getter
public class Product {

    private final Long id;
    private final String productCode;
    private String productName;
    private BigDecimal price;
    private Integer stock;
    private ProductStatus status;

    /**
     * 创建商品领域对象
     *
     * @param id          商品ID
     * @param productCode 商品编码
     * @param productName 商品名称
     * @param price       商品价格
     * @param stock       库存数量
     * @param status      商品状态
     */
    public Product(Long id,
                   String productCode,
                   String productName,
                   BigDecimal price,
                   Integer stock,
                   ProductStatus status) {
        if (id == null || id <= 0) {
            throw new IllegalArgumentException("商品ID必须大于0");
        }
        if (StrUtil.hasBlank(productCode, productName)) {
            throw new IllegalArgumentException("商品编码和商品名称不能为空");
        }
        if (price == null || price.compareTo(BigDecimal.ZERO) < 0) {
            throw new IllegalArgumentException("商品价格不能小于0");
        }
        if (stock == null || stock < 0) {
            throw new IllegalArgumentException("库存数量不能小于0");
        }

        this.id = id;
        this.productCode = productCode;
        this.productName = productName;
        this.price = price;
        this.stock = stock;
        this.status = status == null ? ProductStatus.DRAFT : status;
    }

    /**
     * 创建草稿商品
     *
     * @param id          商品ID
     * @param productCode 商品编码
     * @param productName 商品名称
     * @param price       商品价格
     * @param stock       库存数量
     * @return 商品领域对象
     */
    public static Product createDraft(Long id,
                                      String productCode,
                                      String productName,
                                      BigDecimal price,
                                      Integer stock) {
        return new Product(id, productCode, productName, price, stock, ProductStatus.DRAFT);
    }

    /**
     * 修改商品价格
     *
     * @param newPrice 新价格
     */
    public void changePrice(BigDecimal newPrice) {
        if (newPrice == null || newPrice.compareTo(BigDecimal.ZERO) < 0) {
            log.warn("修改商品价格失败，价格不合法，商品编码：{}，价格：{}", productCode, newPrice);
            throw new IllegalArgumentException("商品价格不能小于0");
        }

        this.price = newPrice;
        log.info("修改商品价格成功，商品编码：{}，新价格：{}", productCode, newPrice);
    }

    /**
     * 上架商品
     */
    public void putOnShelf() {
        if (stock <= 0) {
            log.warn("商品上架失败，库存不足，商品编码：{}，库存：{}", productCode, stock);
            throw new IllegalStateException("库存不足，不能上架");
        }

        this.status = ProductStatus.ON_SHELF;
        log.info("商品上架成功，商品编码：{}", productCode);
    }

    /**
     * 下架商品
     */
    public void takeOffShelf() {
        this.status = ProductStatus.OFF_SHELF;
        log.info("商品下架成功，商品编码：{}", productCode);
    }
}
```

文件位置：`src/main/java/io/github/atengk/design/entity/ProductEntity.java`

下面是商品持久化实体。它只和数据库表结构对应，不承载复杂业务行为。

```java
package io.github.atengk.design.entity;

import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 商品持久化实体
 *
 * @author Ateng
 * @since 2026-05-01
 */
@Data
@TableName("product")
public class ProductEntity {

    /**
     * 商品ID
     */
    @TableId
    private Long id;

    /**
     * 商品编码
     */
    private String productCode;

    /**
     * 商品名称
     */
    private String productName;

    /**
     * 商品价格
     */
    private BigDecimal price;

    /**
     * 库存数量
     */
    private Integer stock;

    /**
     * 商品状态
     */
    private String status;

    /**
     * 创建时间
     */
    private LocalDateTime createTime;

    /**
     * 更新时间
     */
    private LocalDateTime updateTime;
}
```

文件位置：`src/main/java/io/github/atengk/design/mapper/ProductMapper.java`

下面是 MyBatis-Plus Mapper。它只负责数据库访问，不暴露给业务服务直接使用。

```java
package io.github.atengk.design.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import io.github.atengk.design.entity.ProductEntity;

/**
 * 商品Mapper
 *
 * @author Ateng
 * @since 2026-05-01
 */
public interface ProductMapper extends BaseMapper<ProductEntity> {
}
```

文件位置：`src/main/java/io/github/atengk/design/dto/PageResult.java`

下面是通用分页结果对象。

```java
package io.github.atengk.design.dto;

import java.util.List;

/**
 * 分页结果
 *
 * @param records  数据列表
 * @param pageNum  当前页码
 * @param pageSize 每页大小
 * @param total    总数量
 * @param <T>      数据类型
 * @author Ateng
 * @since 2026-05-01
 */
public record PageResult<T>(
        List<T> records,
        Long pageNum,
        Long pageSize,
        Long total
) {
}
```

文件位置：`src/main/java/io/github/atengk/design/dto/ProductPageQuery.java`

下面是商品分页查询对象。

```java
package io.github.atengk.design.dto;

/**
 * 商品分页查询
 *
 * @param pageNum     页码
 * @param pageSize    每页大小
 * @param productName 商品名称
 * @param status      商品状态
 * @author Ateng
 * @since 2026-05-01
 */
public record ProductPageQuery(
        Long pageNum,
        Long pageSize,
        String productName,
        String status
) {
}
```

文件位置：`src/main/java/io/github/atengk/design/repository/ProductRepository.java`

下面是商品仓储接口。应用服务依赖该接口，而不是直接依赖 `ProductMapper`。

```java
package io.github.atengk.design.repository;

import io.github.atengk.design.domain.Product;
import io.github.atengk.design.dto.PageResult;
import io.github.atengk.design.dto.ProductPageQuery;

import java.util.Optional;

/**
 * 商品仓储接口
 *
 * @author Ateng
 * @since 2026-05-01
 */
public interface ProductRepository {

    /**
     * 保存商品
     *
     * @param product 商品领域对象
     */
    void save(Product product);

    /**
     * 根据商品ID查询商品
     *
     * @param productId 商品ID
     * @return 商品领域对象
     */
    Optional<Product> findById(Long productId);

    /**
     * 根据商品编码查询商品
     *
     * @param productCode 商品编码
     * @return 商品领域对象
     */
    Optional<Product> findByProductCode(String productCode);

    /**
     * 判断商品编码是否存在
     *
     * @param productCode 商品编码
     * @return true 表示存在，false 表示不存在
     */
    boolean existsByProductCode(String productCode);

    /**
     * 分页查询商品
     *
     * @param query 分页查询条件
     * @return 商品分页结果
     */
    PageResult<Product> page(ProductPageQuery query);

    /**
     * 删除商品
     *
     * @param productId 商品ID
     */
    void deleteById(Long productId);
}
```

文件位置：`src/main/java/io/github/atengk/design/repository/impl/MybatisProductRepository.java`

下面是基于 MyBatis-Plus 的商品仓储实现。它负责领域对象和持久化实体之间的转换，并隐藏查询细节。

```java
package io.github.atengk.design.repository.impl;

import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import io.github.atengk.design.domain.Product;
import io.github.atengk.design.domain.ProductStatus;
import io.github.atengk.design.dto.PageResult;
import io.github.atengk.design.dto.ProductPageQuery;
import io.github.atengk.design.entity.ProductEntity;
import io.github.atengk.design.mapper.ProductMapper;
import io.github.atengk.design.repository.ProductRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * MyBatis商品仓储实现
 *
 * @author Ateng
 * @since 2026-05-01
 */
@Slf4j
@Repository
@RequiredArgsConstructor
public class MybatisProductRepository implements ProductRepository {

    private final ProductMapper productMapper;

    /**
     * 保存商品
     *
     * @param product 商品领域对象
     */
    @Override
    public void save(Product product) {
        if (product == null) {
            log.warn("保存商品失败，商品为空");
            throw new IllegalArgumentException("商品不能为空");
        }

        ProductEntity oldEntity = productMapper.selectById(product.getId());
        ProductEntity entity = toEntity(product);

        if (oldEntity == null) {
            entity.setCreateTime(LocalDateTime.now());
            entity.setUpdateTime(LocalDateTime.now());
            productMapper.insert(entity);
            log.info("新增商品成功，商品ID：{}，商品编码：{}", product.getId(), product.getProductCode());
            return;
        }

        entity.setCreateTime(oldEntity.getCreateTime());
        entity.setUpdateTime(LocalDateTime.now());
        productMapper.updateById(entity);
        log.info("更新商品成功，商品ID：{}，商品编码：{}", product.getId(), product.getProductCode());
    }

    /**
     * 根据商品ID查询商品
     *
     * @param productId 商品ID
     * @return 商品领域对象
     */
    @Override
    public Optional<Product> findById(Long productId) {
        if (productId == null || productId <= 0) {
            return Optional.empty();
        }

        return Optional.ofNullable(productMapper.selectById(productId))
                .map(this::toDomain);
    }

    /**
     * 根据商品编码查询商品
     *
     * @param productCode 商品编码
     * @return 商品领域对象
     */
    @Override
    public Optional<Product> findByProductCode(String productCode) {
        if (StrUtil.isBlank(productCode)) {
            return Optional.empty();
        }

        LambdaQueryWrapper<ProductEntity> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(ProductEntity::getProductCode, productCode);

        return Optional.ofNullable(productMapper.selectOne(wrapper))
                .map(this::toDomain);
    }

    /**
     * 判断商品编码是否存在
     *
     * @param productCode 商品编码
     * @return true 表示存在，false 表示不存在
     */
    @Override
    public boolean existsByProductCode(String productCode) {
        if (StrUtil.isBlank(productCode)) {
            return false;
        }

        LambdaQueryWrapper<ProductEntity> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(ProductEntity::getProductCode, productCode);

        boolean exists = productMapper.selectCount(wrapper) > 0;
        log.info("校验商品编码是否存在，商品编码：{}，结果：{}", productCode, exists);
        return exists;
    }

    /**
     * 分页查询商品
     *
     * @param query 分页查询条件
     * @return 商品分页结果
     */
    @Override
    public PageResult<Product> page(ProductPageQuery query) {
        long pageNum = query == null || query.pageNum() == null || query.pageNum() <= 0 ? 1L : query.pageNum();
        long pageSize = query == null || query.pageSize() == null || query.pageSize() <= 0 ? 10L : query.pageSize();

        LambdaQueryWrapper<ProductEntity> wrapper = new LambdaQueryWrapper<>();
        if (query != null && StrUtil.isNotBlank(query.productName())) {
            wrapper.like(ProductEntity::getProductName, query.productName());
        }
        if (query != null && StrUtil.isNotBlank(query.status())) {
            wrapper.eq(ProductEntity::getStatus, query.status());
        }

        wrapper.orderByDesc(ProductEntity::getCreateTime);

        Page<ProductEntity> entityPage = productMapper.selectPage(new Page<>(pageNum, pageSize), wrapper);
        List<Product> records = entityPage.getRecords().stream()
                .map(this::toDomain)
                .toList();

        log.info("分页查询商品完成，页码：{}，每页大小：{}，总数：{}", pageNum, pageSize, entityPage.getTotal());

        return new PageResult<>(
                records,
                entityPage.getCurrent(),
                entityPage.getSize(),
                entityPage.getTotal()
        );
    }

    /**
     * 删除商品
     *
     * @param productId 商品ID
     */
    @Override
    public void deleteById(Long productId) {
        if (productId == null || productId <= 0) {
            log.warn("删除商品失败，商品ID不合法，商品ID：{}", productId);
            throw new IllegalArgumentException("商品ID必须大于0");
        }

        productMapper.deleteById(productId);
        log.info("删除商品成功，商品ID：{}", productId);
    }

    /**
     * 转换为持久化实体
     *
     * @param product 商品领域对象
     * @return 商品持久化实体
     */
    private ProductEntity toEntity(Product product) {
        ProductEntity entity = new ProductEntity();
        entity.setId(product.getId());
        entity.setProductCode(product.getProductCode());
        entity.setProductName(product.getProductName());
        entity.setPrice(product.getPrice());
        entity.setStock(product.getStock());
        entity.setStatus(product.getStatus().name());
        return entity;
    }

    /**
     * 转换为领域对象
     *
     * @param entity 商品持久化实体
     * @return 商品领域对象
     */
    private Product toDomain(ProductEntity entity) {
        if (entity == null) {
            throw new IllegalArgumentException("商品持久化实体不能为空");
        }

        return new Product(
                entity.getId(),
                entity.getProductCode(),
                entity.getProductName(),
                entity.getPrice(),
                entity.getStock(),
                ProductStatus.valueOf(entity.getStatus())
        );
    }
}
```

## 应用服务和接口

应用服务负责业务用例编排。它调用仓储接口获取领域对象，再调用领域对象执行业务行为，最后通过仓储保存。

文件位置：`src/main/java/io/github/atengk/design/dto/ProductCreateRequest.java`

下面是商品创建请求对象。

```java
package io.github.atengk.design.dto;

import java.math.BigDecimal;

/**
 * 商品创建请求
 *
 * @param productCode 商品编码
 * @param productName 商品名称
 * @param price       商品价格
 * @param stock       库存数量
 * @author Ateng
 * @since 2026-05-01
 */
public record ProductCreateRequest(
        String productCode,
        String productName,
        BigDecimal price,
        Integer stock
) {
}
```

文件位置：`src/main/java/io/github/atengk/design/dto/ProductChangePriceRequest.java`

下面是商品改价请求对象。

```java
package io.github.atengk.design.dto;

import java.math.BigDecimal;

/**
 * 商品改价请求
 *
 * @param productId 商品ID
 * @param newPrice  新价格
 * @author Ateng
 * @since 2026-05-01
 */
public record ProductChangePriceRequest(
        Long productId,
        BigDecimal newPrice
) {
}
```

文件位置：`src/main/java/io/github/atengk/design/dto/ProductResponse.java`

下面是商品响应对象。

```java
package io.github.atengk.design.dto;

import java.math.BigDecimal;

/**
 * 商品响应
 *
 * @param id          商品ID
 * @param productCode 商品编码
 * @param productName 商品名称
 * @param price       商品价格
 * @param stock       库存数量
 * @param status      商品状态
 * @author Ateng
 * @since 2026-05-01
 */
public record ProductResponse(
        Long id,
        String productCode,
        String productName,
        BigDecimal price,
        Integer stock,
        String status
) {
}
```

文件位置：`src/main/java/io/github/atengk/design/service/ProductApplicationService.java`

下面是商品应用服务接口。

```java
package io.github.atengk.design.service;

import io.github.atengk.design.dto.PageResult;
import io.github.atengk.design.dto.ProductChangePriceRequest;
import io.github.atengk.design.dto.ProductCreateRequest;
import io.github.atengk.design.dto.ProductPageQuery;
import io.github.atengk.design.dto.ProductResponse;

/**
 * 商品应用服务
 *
 * @author Ateng
 * @since 2026-05-01
 */
public interface ProductApplicationService {

    /**
     * 创建商品
     *
     * @param request 商品创建请求
     * @return 商品响应
     */
    ProductResponse create(ProductCreateRequest request);

    /**
     * 修改商品价格
     *
     * @param request 商品改价请求
     * @return 商品响应
     */
    ProductResponse changePrice(ProductChangePriceRequest request);

    /**
     * 上架商品
     *
     * @param productId 商品ID
     * @return 商品响应
     */
    ProductResponse putOnShelf(Long productId);

    /**
     * 根据商品ID查询商品
     *
     * @param productId 商品ID
     * @return 商品响应
     */
    ProductResponse getById(Long productId);

    /**
     * 分页查询商品
     *
     * @param query 商品分页查询
     * @return 商品分页结果
     */
    PageResult<ProductResponse> page(ProductPageQuery query);
}
```

文件位置：`src/main/java/io/github/atengk/design/service/impl/ProductApplicationServiceImpl.java`

下面是商品应用服务实现。它只依赖 `ProductRepository`，不直接操作 Mapper。

```java
package io.github.atengk.design.service.impl;

import cn.hutool.core.util.IdUtil;
import cn.hutool.core.util.StrUtil;
import io.github.atengk.design.domain.Product;
import io.github.atengk.design.dto.PageResult;
import io.github.atengk.design.dto.ProductChangePriceRequest;
import io.github.atengk.design.dto.ProductCreateRequest;
import io.github.atengk.design.dto.ProductPageQuery;
import io.github.atengk.design.dto.ProductResponse;
import io.github.atengk.design.repository.ProductRepository;
import io.github.atengk.design.service.ProductApplicationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;

/**
 * 商品应用服务实现
 *
 * @author Ateng
 * @since 2026-05-01
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ProductApplicationServiceImpl implements ProductApplicationService {

    private final ProductRepository productRepository;

    /**
     * 创建商品
     *
     * @param request 商品创建请求
     * @return 商品响应
     */
    @Override
    public ProductResponse create(ProductCreateRequest request) {
        validateCreateRequest(request);

        if (productRepository.existsByProductCode(request.productCode())) {
            log.warn("创建商品失败，商品编码已存在，商品编码：{}", request.productCode());
            throw new IllegalArgumentException("商品编码已存在：" + request.productCode());
        }

        Product product = Product.createDraft(
                IdUtil.getSnowflakeNextId(),
                request.productCode(),
                request.productName(),
                request.price(),
                request.stock()
        );

        productRepository.save(product);
        log.info("创建商品完成，商品ID：{}，商品编码：{}", product.getId(), product.getProductCode());

        return toResponse(product);
    }

    /**
     * 修改商品价格
     *
     * @param request 商品改价请求
     * @return 商品响应
     */
    @Override
    public ProductResponse changePrice(ProductChangePriceRequest request) {
        validateChangePriceRequest(request);

        Product product = productRepository.findById(request.productId())
                .orElseThrow(() -> {
                    log.warn("修改商品价格失败，商品不存在，商品ID：{}", request.productId());
                    return new IllegalArgumentException("商品不存在：" + request.productId());
                });

        product.changePrice(request.newPrice());
        productRepository.save(product);

        log.info("修改商品价格完成，商品ID：{}，新价格：{}", product.getId(), product.getPrice());
        return toResponse(product);
    }

    /**
     * 上架商品
     *
     * @param productId 商品ID
     * @return 商品响应
     */
    @Override
    public ProductResponse putOnShelf(Long productId) {
        Product product = productRepository.findById(productId)
                .orElseThrow(() -> {
                    log.warn("上架商品失败，商品不存在，商品ID：{}", productId);
                    return new IllegalArgumentException("商品不存在：" + productId);
                });

        product.putOnShelf();
        productRepository.save(product);

        log.info("上架商品完成，商品ID：{}，商品编码：{}", product.getId(), product.getProductCode());
        return toResponse(product);
    }

    /**
     * 根据商品ID查询商品
     *
     * @param productId 商品ID
     * @return 商品响应
     */
    @Override
    public ProductResponse getById(Long productId) {
        Product product = productRepository.findById(productId)
                .orElseThrow(() -> {
                    log.warn("查询商品失败，商品不存在，商品ID：{}", productId);
                    return new IllegalArgumentException("商品不存在：" + productId);
                });

        return toResponse(product);
    }

    /**
     * 分页查询商品
     *
     * @param query 商品分页查询
     * @return 商品分页结果
     */
    @Override
    public PageResult<ProductResponse> page(ProductPageQuery query) {
        PageResult<Product> pageResult = productRepository.page(query);

        return new PageResult<>(
                pageResult.records().stream().map(this::toResponse).toList(),
                pageResult.pageNum(),
                pageResult.pageSize(),
                pageResult.total()
        );
    }

    /**
     * 转换为商品响应
     *
     * @param product 商品领域对象
     * @return 商品响应
     */
    private ProductResponse toResponse(Product product) {
        return new ProductResponse(
                product.getId(),
                product.getProductCode(),
                product.getProductName(),
                product.getPrice(),
                product.getStock(),
                product.getStatus().name()
        );
    }

    /**
     * 校验商品创建请求
     *
     * @param request 商品创建请求
     */
    private void validateCreateRequest(ProductCreateRequest request) {
        if (request == null) {
            log.warn("创建商品失败，请求参数为空");
            throw new IllegalArgumentException("请求参数不能为空");
        }

        if (StrUtil.hasBlank(request.productCode(), request.productName())) {
            log.warn("创建商品失败，商品编码或商品名称为空");
            throw new IllegalArgumentException("商品编码和商品名称不能为空");
        }

        if (request.price() == null || request.price().compareTo(BigDecimal.ZERO) < 0) {
            log.warn("创建商品失败，商品价格不合法，价格：{}", request.price());
            throw new IllegalArgumentException("商品价格不能小于0");
        }

        if (request.stock() == null || request.stock() < 0) {
            log.warn("创建商品失败，库存数量不合法，库存：{}", request.stock());
            throw new IllegalArgumentException("库存数量不能小于0");
        }
    }

    /**
     * 校验商品改价请求
     *
     * @param request 商品改价请求
     */
    private void validateChangePriceRequest(ProductChangePriceRequest request) {
        if (request == null) {
            log.warn("修改商品价格失败，请求参数为空");
            throw new IllegalArgumentException("请求参数不能为空");
        }

        if (request.productId() == null || request.productId() <= 0) {
            log.warn("修改商品价格失败，商品ID不合法，商品ID：{}", request.productId());
            throw new IllegalArgumentException("商品ID必须大于0");
        }

        if (request.newPrice() == null || request.newPrice().compareTo(BigDecimal.ZERO) < 0) {
            log.warn("修改商品价格失败，新价格不合法，价格：{}", request.newPrice());
            throw new IllegalArgumentException("商品价格不能小于0");
        }
    }
}
```

文件位置：`src/main/java/io/github/atengk/design/controller/ProductController.java`

下面是商品接口，用于验证仓储模式的创建、改价、上架、查询和分页能力。

```java
package io.github.atengk.design.controller;

import io.github.atengk.design.dto.PageResult;
import io.github.atengk.design.dto.ProductChangePriceRequest;
import io.github.atengk.design.dto.ProductCreateRequest;
import io.github.atengk.design.dto.ProductPageQuery;
import io.github.atengk.design.dto.ProductResponse;
import io.github.atengk.design.service.ProductApplicationService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;

/**
 * 商品控制器
 *
 * @author Ateng
 * @since 2026-05-01
 */
@RestController
@RequiredArgsConstructor
@RequestMapping("/repository/product")
public class ProductController {

    private final ProductApplicationService productApplicationService;

    /**
     * 创建商品
     *
     * @param productCode 商品编码
     * @param productName 商品名称
     * @param price       商品价格
     * @param stock       库存数量
     * @return 商品响应
     */
    @PostMapping("/create")
    public ProductResponse create(@RequestParam String productCode,
                                  @RequestParam String productName,
                                  @RequestParam BigDecimal price,
                                  @RequestParam Integer stock) {
        ProductCreateRequest request = new ProductCreateRequest(productCode, productName, price, stock);
        return productApplicationService.create(request);
    }

    /**
     * 修改商品价格
     *
     * @param productId 商品ID
     * @param newPrice  新价格
     * @return 商品响应
     */
    @PostMapping("/change-price")
    public ProductResponse changePrice(@RequestParam Long productId,
                                       @RequestParam BigDecimal newPrice) {
        ProductChangePriceRequest request = new ProductChangePriceRequest(productId, newPrice);
        return productApplicationService.changePrice(request);
    }

    /**
     * 上架商品
     *
     * @param productId 商品ID
     * @return 商品响应
     */
    @PostMapping("/put-on-shelf")
    public ProductResponse putOnShelf(@RequestParam Long productId) {
        return productApplicationService.putOnShelf(productId);
    }

    /**
     * 根据商品ID查询商品
     *
     * @param productId 商品ID
     * @return 商品响应
     */
    @GetMapping("/detail")
    public ProductResponse getById(@RequestParam Long productId) {
        return productApplicationService.getById(productId);
    }

    /**
     * 分页查询商品
     *
     * @param pageNum     页码
     * @param pageSize    每页大小
     * @param productName 商品名称
     * @param status      商品状态
     * @return 商品分页结果
     */
    @GetMapping("/page")
    public PageResult<ProductResponse> page(@RequestParam(defaultValue = "1") Long pageNum,
                                            @RequestParam(defaultValue = "10") Long pageSize,
                                            @RequestParam(required = false) String productName,
                                            @RequestParam(required = false) String status) {
        ProductPageQuery query = new ProductPageQuery(pageNum, pageSize, productName, status);
        return productApplicationService.page(query);
    }
}
```

## 使用方式

启动项目之前，先创建数据库和表结构。

```bash
mysql -uroot -proot -e "CREATE DATABASE IF NOT EXISTS design_demo DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;"
mysql -uroot -proot design_demo < sql/product.sql
```

这里 `-uroot -proot` 根据本地 MySQL 账号密码调整。`design_demo` 是示例数据库名，和 `application.yml` 中的连接地址保持一致。

启动 Spring Boot 项目：

```bash
mvn spring-boot:run
```

创建商品：

```bash
curl -X POST "http://localhost:8080/repository/product/create?productCode=P10001&productName=机械键盘&price=199.00&stock=100"
```

可能返回：

```json
{
  "id": 2020123456789017600,
  "productCode": "P10001",
  "productName": "机械键盘",
  "price": 199.00,
  "stock": 100,
  "status": "DRAFT"
}
```

修改价格：

```bash
curl -X POST "http://localhost:8080/repository/product/change-price?productId=2020123456789017600&newPrice=189.00"
```

上架商品：

```bash
curl -X POST "http://localhost:8080/repository/product/put-on-shelf?productId=2020123456789017600"
```

查询商品详情：

```bash
curl "http://localhost:8080/repository/product/detail?productId=2020123456789017600"
```

分页查询商品：

```bash
curl "http://localhost:8080/repository/product/page?pageNum=1&pageSize=10&productName=键盘&status=ON_SHELF"
```

如果仓储模式正常，可以看到类似日志：

```text
校验商品编码是否存在，商品编码：P10001，结果：false
新增商品成功，商品ID：2020123456789017600，商品编码：P10001
创建商品完成，商品ID：2020123456789017600，商品编码：P10001
修改商品价格成功，商品编码：P10001，新价格：189.00
更新商品成功，商品ID：2020123456789017600，商品编码：P10001
商品上架成功，商品编码：P10001
更新商品成功，商品ID：2020123456789017600，商品编码：P10001
分页查询商品完成，页码：1，每页大小：10，总数：1
```

## 仓储模式和 Mapper 的区别

仓储模式经常被误解为“给 Mapper 套一层壳”。如果仓储层只是机械转发 Mapper 方法，没有封装领域语义，价值会很低。

不推荐：

```java
public interface ProductRepository {

    int insert(ProductEntity entity);

    int updateById(ProductEntity entity);

    ProductEntity selectById(Long id);
}
```

这只是换了一个名字的 Mapper。推荐让仓储接口表达业务对象存取语义：

```java
public interface ProductRepository {

    void save(Product product);

    Optional<Product> findById(Long productId);

    Optional<Product> findByProductCode(String productCode);

    PageResult<Product> page(ProductPageQuery query);
}
```

Mapper 负责表，Repository 负责业务对象。Mapper 可以返回 `ProductEntity`，Repository 应该尽量返回 `Product` 这类领域对象。

## 仓储模式和 DAO 模式的区别

DAO 模式通常更靠近数据库访问，强调封装数据访问逻辑。仓储模式更靠近领域层，强调用集合语义管理聚合对象。

| 对比项     | DAO 模式                     | 仓储模式                         |
| ---------- | ---------------------------- | -------------------------------- |
| 关注点     | 数据访问                     | 领域对象存取                     |
| 常见对象   | Entity、PO、DO               | Aggregate、Domain Model          |
| 接口语义   | `insert`、`update`、`select` | `save`、`findById`、`findByCode` |
| 业务表达   | 较弱                         | 较强                             |
| DDD 中位置 | 基础设施层                   | 领域层接口，基础设施层实现       |

简单理解：

```text
DAO：我帮你访问数据库。
Repository：我帮你存取领域对象。
```

在简单 CRUD 项目中，DAO 或 Mapper 已经足够。在复杂领域模型中，Repository 更能保护业务层，避免业务代码被数据库表结构牵着走。

## 仓储模式和 Service 的边界

仓储层不应该承载业务流程。仓储只负责对象存取，应用服务负责业务用例编排，领域对象负责核心业务规则。

不推荐把业务流程塞进仓储：

```java
public void createProductAndSendNotice(Product product) {
    // 保存商品
    // 发送通知
    // 写审计日志
}
```

推荐职责拆分：

```text
ProductApplicationService
    -> ProductRepository.save(product)
    -> EventPublisher.publish(ProductCreatedEvent)
```

仓储中可以做的事情：

```text
领域对象和 Entity 转换
查询条件封装
分页查询封装
缓存读取和回写
跨表组装领域对象
隐藏 Mapper 或远程接口
```

仓储中不建议做的事情：

```text
发送短信
发布 MQ
处理审批流
执行支付
编排多个业务服务
处理 Controller 参数
```

## 扩展缓存仓储

仓储模式很适合扩展缓存，因为业务层依赖的是仓储接口，不关心数据来自缓存还是数据库。可以在仓储实现中组合 Redis 和 MyBatis。

示例结构：

```text
CachedProductRepository
    -> RedisTemplate
    -> ProductMapper
```

简化示例：

```java
@Repository
@RequiredArgsConstructor
public class CachedProductRepository implements ProductRepository {

    private final ProductMapper productMapper;
    private final RedisTemplate<String, Product> redisTemplate;

    public Optional<Product> findById(Long productId) {
        String cacheKey = "product:" + productId;
        Product cachedProduct = redisTemplate.opsForValue().get(cacheKey);
        if (cachedProduct != null) {
            return Optional.of(cachedProduct);
        }

        Product product = Optional.ofNullable(productMapper.selectById(productId))
                .map(this::toDomain)
                .orElse(null);

        if (product != null) {
            redisTemplate.opsForValue().set(cacheKey, product, Duration.ofMinutes(10));
        }

        return Optional.ofNullable(product);
    }
}
```

如果要在同一个项目中同时存在 `MybatisProductRepository` 和 `CachedProductRepository`，需要用 `@Primary` 或 `@Qualifier` 指定注入哪个实现。

```java
@Primary
@Repository
public class CachedProductRepository implements ProductRepository {
}
```

缓存仓储要重点处理一致性问题。保存商品后，需要更新或删除缓存：

```text
save(product)
    -> update database
    -> delete cache
```

通常推荐写库后删除缓存，而不是直接更新缓存，降低并发不一致风险。

## 验证方式

仓储模式是否落地正确，可以从依赖方向、业务行为、数据库结果三个角度验证。

检查依赖方向：

```text
Controller 只能依赖 Service
Service 只能依赖 Repository
Repository 实现依赖 Mapper
Mapper 不暴露给 Controller 和 Service
```

检查代码中是否出现以下不推荐情况：

```text
Controller 直接注入 Mapper
Service 直接调用 ProductMapper
业务层直接操作 ProductEntity
领域对象中出现 @TableName
Repository 接口暴露 insert、selectById 这类数据库语义
```

执行接口后，可以查询数据库确认结果：

```sql
SELECT id, product_code, product_name, price, stock, status, create_time, update_time
FROM product
ORDER BY create_time DESC;
```

如果创建、改价、上架都正常，数据库中应该能看到：

```text
product_code = P10001
price = 189.00
status = ON_SHELF
```

## 注意事项

仓储模式适合业务复杂度较高的项目，不适合所有简单 CRUD 都强行套一层。简单后台管理系统如果只是表单增删改查，Service 直接使用 MyBatis-Plus 的 ServiceImpl 也可以。

适合使用仓储模式的场景：

```text
DDD 聚合持久化
领域模型和数据库模型需要解耦
需要隐藏复杂查询
需要组合数据库和缓存
需要跨表组装业务对象
需要替换底层存储实现
需要隔离 Mapper 对业务层的污染
```

不太适合使用仓储模式的场景：

```text
纯 CRUD 后台
没有领域模型
表结构和接口模型完全一致
项目规模很小
Repository 只是机械转发 Mapper
```

仓储接口不要过度泛化。下面这种通用仓储看似复用，实际容易丢失业务语义：

```java
public interface BaseRepository<T, ID> {

    void save(T entity);

    T findById(ID id);

    void deleteById(ID id);
}
```

更推荐按聚合设计具体仓储：

```java
public interface ProductRepository {

    void save(Product product);

    Optional<Product> findByProductCode(String productCode);
}
```

仓储实现中要控制对象转换复杂度。如果转换逻辑变多，可以单独抽出转换器：

```text
ProductConverter
    -> toEntity(Product)
    -> toDomain(ProductEntity)
```

如果仓储返回的是领域对象，就不要让业务层再接触 `ProductEntity`。否则仓储模式的隔离价值会被破坏。

不推荐：

```java
ProductEntity entity = productMapper.selectById(productId);
```

推荐：

```java
Product product = productRepository.findById(productId)
        .orElseThrow();
```

如果一个聚合需要多张表组装，仓储可以在内部完成，但要注意查询性能和事务边界。例如订单聚合可能涉及：

```text
order_main
order_item
order_payment
order_delivery
```

这种情况下，`OrderRepository.findById(orderId)` 可以组装完整订单聚合，但不建议在 Controller 或 Service 中散落多次 Mapper 查询。

仓储层可以处理数据访问异常包装，但不要吞掉异常。生产环境中可以把数据库异常转换为业务可理解的异常，并记录日志。

## 总结

在 JDK21 和 Spring Boot 3 项目中，仓储模式的实践重点是隔离业务层和持久化层，让业务层面向领域对象和仓储接口编程，而不是直接依赖 Mapper、Entity 或数据库表结构。

普通 Java 仓储模式适合理解“像集合一样存取领域对象”的思想。Spring Boot 项目中更推荐使用“领域对象 + 持久化实体 + Mapper + Repository 接口 + Repository 实现 + 应用服务”的结构。对于 DDD 聚合、复杂查询、缓存组合、跨表组装、领域模型和数据库模型解耦等场景，仓储模式可以显著提升代码可维护性。

仓储模式不是给 Mapper 换名字，也不是所有 CRUD 的必选项。它最适合处理“业务层不应该知道数据怎么存、表怎么查、缓存怎么用”的场景。实际落地时，需要重点控制 Repository 和 Mapper 的边界、领域对象和 Entity 的转换、缓存一致性、查询性能和事务边界。
