# Caffeine

## 模块概述

本模块用于说明 Spring Boot 3 项目中如何集成和使用 Caffeine 本地缓存。Caffeine 主要用于提升热点数据读取性能，减少重复数据库查询、远程接口调用和重复计算开销，适合对响应速度要求较高、数据规模可控、允许短时间本地缓存的业务场景。

在 Spring Boot 3 项目中，Caffeine 通常会配合 Spring Cache 使用。业务代码通过缓存注解声明缓存行为，底层由 Caffeine 负责在当前 JVM 进程内存储和读取缓存数据。

### Caffeine 简介

Caffeine 是一个高性能 Java 本地缓存库，运行在应用自身的 JVM 内部，不依赖 Redis、Memcached 等外部缓存中间件。由于缓存数据直接存放在本地内存中，因此访问速度快、接入成本低、部署简单。

Caffeine 常用于替代传统的 Guava Cache，适合在单体应用或微服务实例内部缓存热点数据。它支持最大容量限制、时间过期、自动淘汰、缓存统计等能力，可以有效降低数据库、远程服务和复杂计算逻辑的访问压力。

常见能力如下：

| 能力         | 说明                                                      |
| ------------ | --------------------------------------------------------- |
| 最大容量控制 | 通过 `maximumSize` 限制缓存条目数量，避免本地内存无限增长 |
| 时间过期     | 支持写入后过期、访问后过期等策略                          |
| 自动淘汰     | 缓存达到容量上限后，自动淘汰低价值缓存数据                |
| 本地内存访问 | 数据存储在当前 JVM 中，读取速度快                         |
| 缓存统计     | 可开启命中率、淘汰数量等统计信息                          |

需要注意的是，Caffeine 是本地缓存，不是分布式缓存。多个应用实例之间的缓存数据不会自动同步，因此它更适合缓存短期热点数据、字典数据、系统配置数据等允许短时间不一致的数据。

### 适用场景

Caffeine 适用于读多写少、数据变化频率较低、允许短时间缓存不一致的业务场景。由于缓存数据保存在本地 JVM 内存中，因此非常适合提升接口响应速度，降低后端资源访问压力。

常见适用场景如下：

| 场景             | 示例                       | 说明                     |
| ---------------- | -------------------------- | ------------------------ |
| 热点查询缓存     | 用户基础信息、商品基础信息 | 减少重复数据库查询       |
| 字典数据缓存     | 状态码、类型枚举、区域编码 | 数据变化少，适合本地缓存 |
| 系统配置缓存     | 业务开关、参数配置         | 提高配置读取速度         |
| 计算结果缓存     | 统计结果、规则匹配结果     | 避免重复计算             |
| 外部接口结果缓存 | 第三方接口基础信息         | 减少外部服务调用次数     |
| 短期临时数据     | 验证结果、短期上下文       | 适合生命周期较短的数据   |

不建议使用 Caffeine 的场景如下：

| 场景                         | 原因                                            |
| ---------------------------- | ----------------------------------------------- |
| 多实例强一致缓存             | Caffeine 是本地缓存，不会自动跨节点同步         |
| 超大容量缓存                 | 数据保存在 JVM 内存中，容量过大会影响应用稳定性 |
| 长周期核心业务数据           | 应用重启后本地缓存会丢失                        |
| 需要统一缓存管理的系统       | 更适合使用 Redis 等集中式缓存                   |
| 频繁写入且一致性要求高的数据 | 本地缓存容易出现短时间脏数据                    |

在实际项目中，如果系统同时要求本地高性能访问和多实例缓存共享，可以考虑 Redis + Caffeine 的多级缓存方案。Redis 负责集中式缓存，Caffeine 负责本地热点数据加速。

### 与 Spring Cache 的关系

Spring Cache 是 Spring 提供的缓存抽象，主要负责定义缓存注解、缓存接口和缓存管理器。它本身不直接负责数据存储，而是通过不同的缓存实现完成真正的缓存读写。

Caffeine 是 Spring Cache 支持的一种缓存实现。在 Spring Boot 3 项目中，引入 Caffeine 依赖后，可以通过 `CaffeineCacheManager` 将 Spring Cache 的缓存操作委托给 Caffeine 执行。

两者关系如下：

| 组件                   | 作用                                           |
| ---------------------- | ---------------------------------------------- |
| Spring Cache           | 提供缓存抽象、缓存注解和缓存管理接口           |
| Caffeine               | 提供高性能本地缓存存储能力                     |
| `CacheManager`         | 管理多个缓存空间                               |
| `CaffeineCacheManager` | Spring Cache 与 Caffeine 之间的适配器          |
| `@Cacheable`           | 查询时优先读取缓存，未命中时执行方法并写入缓存 |
| `@CachePut`            | 执行方法后更新缓存                             |
| `@CacheEvict`          | 删除指定缓存或清空缓存                         |

缓存调用流程如下：

```text
业务方法调用
    ↓
Spring Cache 拦截方法
    ↓
根据缓存名称和缓存 Key 查找缓存
    ↓
命中缓存：直接返回缓存结果
    ↓
未命中缓存：执行目标方法
    ↓
将方法返回值写入 Caffeine 本地缓存
    ↓
返回业务结果
```

使用 Spring Cache 的好处是业务代码与缓存实现解耦。业务层只需要关注缓存注解和缓存名称，不需要直接操作 Caffeine API。如果后续需要将 Caffeine 替换为 Redis 或其他缓存实现，通常只需要调整依赖和配置，业务代码可以保持相对稳定。

## 环境准备

本节用于说明 Spring Boot 3 集成 Caffeine 前需要准备的 JDK、Maven 依赖和基础配置文件。完成本节配置后，项目即可具备 Spring Cache + Caffeine 的基础缓存能力。

Spring Boot 3 要求使用 Java 17 或更高版本。因此，在集成 Caffeine 前，需要先确认项目 JDK、构建工具和 Spring Boot 版本满足基础要求。

### SpringBoot3 版本要求

推荐环境如下：

| 组件        | 推荐版本                    | 说明                                             |
| ----------- | --------------------------- | ------------------------------------------------ |
| JDK         | 17+                         | Spring Boot 3 最低要求 Java 17                   |
| Spring Boot | 3.x                         | 建议使用当前项目统一的 Spring Boot 3 版本        |
| Maven       | 3.6.3+                      | 用于项目依赖管理和构建                           |
| Caffeine    | 由 Spring Boot 依赖管理控制 | 使用 Spring Boot Parent 时通常不需要手动指定版本 |
| 编码        | UTF-8                       | 避免配置文件和日志中文乱码                       |

查看当前 Java 和 Maven 版本：

```bash
# 查看 Java 版本，确认版本为 17 或更高
java -version

# 查看 Maven 版本
mvn -version
```

如果项目使用 `spring-boot-starter-parent` 管理依赖版本，通常不需要手动指定 Caffeine 的版本。Spring Boot 会通过依赖管理自动选择兼容版本，减少版本冲突风险。

### Maven 依赖配置

在 Spring Boot 3 中使用 Caffeine，需要引入 `spring-boot-starter-cache` 和 `caffeine`。其中，`spring-boot-starter-cache` 提供 Spring Cache 的基础能力，`caffeine` 提供本地缓存实现。

文件位置：`pom.xml`

```xml
<dependencies>
    <!-- Spring Cache 基础依赖，提供缓存注解、CacheManager 等能力 -->
    <dependency>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-starter-cache</artifactId>
    </dependency>

    <!-- Caffeine 本地缓存实现，由 Spring Boot 依赖管理控制版本 -->
    <dependency>
        <groupId>com.github.ben-manes.caffeine</groupId>
        <artifactId>caffeine</artifactId>
    </dependency>

    <!-- Web 示例接口依赖，后续用于编写缓存验证接口 -->
    <dependency>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-starter-web</artifactId>
    </dependency>

    <!-- Lombok 简化实体类、DTO、日志对象声明 -->
    <dependency>
        <groupId>org.projectlombok</groupId>
        <artifactId>lombok</artifactId>
        <optional>true</optional>
    </dependency>

    <!-- Hutool 工具类，用于字符串、集合、对象判空等通用处理 -->
    <dependency>
        <groupId>cn.hutool</groupId>
        <artifactId>hutool-core</artifactId>
        <version>5.8.36</version>
    </dependency>

    <!-- 单元测试依赖，用于验证缓存命中、缓存更新和缓存删除逻辑 -->
    <dependency>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-starter-test</artifactId>
        <scope>test</scope>
    </dependency>
</dependencies>
```

如果项目已经使用如下 Parent 配置，则 Caffeine 版本通常由 Spring Boot 自动管理。

文件位置：`pom.xml`

```xml
<parent>
    <!-- Spring Boot Parent 用于统一插件版本、依赖版本和构建默认配置 -->
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-parent</artifactId>
    <version>3.3.5</version>
    <relativePath/>
</parent>

<properties>
    <!-- Spring Boot 3 最低要求 Java 17 -->
    <java.version>17</java.version>
</properties>
```

如果项目没有使用 `spring-boot-starter-parent`，可以通过 `dependencyManagement` 引入 Spring Boot BOM。

文件位置：`pom.xml`

```xml
<dependencyManagement>
    <dependencies>
        <!-- 使用 Spring Boot BOM 统一管理 Spring 与第三方组件版本 -->
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-dependencies</artifactId>
            <version>3.3.5</version>
            <type>pom</type>
            <scope>import</scope>
        </dependency>
    </dependencies>
</dependencyManagement>
```

实际项目中，`3.3.5` 需要替换为当前项目统一使用的 Spring Boot 版本。

### 缓存配置文件

Caffeine 可以通过 `application.yml` 配置缓存类型、缓存名称和缓存规则。常见配置包括最大缓存数量、写入后过期时间、访问后过期时间和缓存统计。

文件位置：`src/main/resources/application.yml`

```yaml
spring:
  application:
    # 应用名称，用于日志、监控和服务标识
    name: springboot3-caffeine-demo

  cache:
    # 指定缓存实现为 caffeine，避免存在多个缓存组件时自动推断不符合预期
    type: caffeine

    # 预先声明缓存名称，便于启动时创建和统一管理
    cache-names:
      - userCache
      - dictCache
      - configCache

    caffeine:
      # maximumSize：最大缓存条目数，超过后按 Caffeine 策略自动淘汰
      # expireAfterWrite：写入后过期时间，适合缓存固定生命周期数据
      # recordStats：开启统计信息，便于后续查看命中率、淘汰数等指标
      spec: maximumSize=10000,expireAfterWrite=10m,recordStats
```

常用 `spec` 参数如下：

| 参数                | 示例                    | 说明                           |
| ------------------- | ----------------------- | ------------------------------ |
| `maximumSize`       | `maximumSize=10000`     | 最大缓存条目数量               |
| `expireAfterWrite`  | `expireAfterWrite=10m`  | 写入后经过指定时间过期         |
| `expireAfterAccess` | `expireAfterAccess=30m` | 最后一次访问后经过指定时间过期 |
| `initialCapacity`   | `initialCapacity=100`   | 初始化容量                     |
| `recordStats`       | `recordStats`           | 开启缓存统计信息               |

用户信息缓存示例，适合写入后 10 分钟过期的业务数据。

文件位置：`src/main/resources/application.yml`

```yaml
spring:
  cache:
    type: caffeine
    cache-names:
      - userCache
    caffeine:
      # 用户信息缓存：限制容量，写入后 10 分钟过期
      spec: maximumSize=5000,expireAfterWrite=10m,recordStats
```

字典数据缓存示例，适合访问后自动续期的热点数据。

文件位置：`src/main/resources/application.yml`

```yaml
spring:
  cache:
    type: caffeine
    cache-names:
      - dictCache
    caffeine:
      # 字典缓存：访问后续期，适合热点字典长期保留
      spec: maximumSize=2000,expireAfterAccess=60m,recordStats
```

需要注意的是，`spring.cache.caffeine.spec` 是全局默认配置，通常会作用于当前 `CaffeineCacheManager` 管理的缓存。如果不同缓存需要设置不同的过期时间、容量或淘汰策略，建议后续通过 Java 配置自定义 `CaffeineCacheManager`，而不是只依赖全局配置。



## 缓存基础配置

本节用于说明 Spring Boot 3 项目中启用 Spring Cache、配置 Caffeine 缓存管理器，以及设置缓存容量和过期时间的基础方式。完成本节配置后，后续即可通过注解式或编程式方式操作缓存。

### 启用缓存能力

Spring Cache 默认不会自动启用，需要在启动类或配置类上添加 `@EnableCaching` 注解。该注解会开启 Spring 的缓存代理能力，使 `@Cacheable`、`@CachePut`、`@CacheEvict` 等缓存注解生效。

推荐将 `@EnableCaching` 放在 Spring Boot 启动类上，便于统一识别当前应用已经启用缓存能力。

文件位置：`src/main/java/io/github/atengk/CaffeineApplication.java`

这段代码用于在 Spring Boot 启动类中启用缓存能力。

```java
package io.github.atengk;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cache.annotation.EnableCaching;

/**
 * Caffeine 缓存示例应用启动类
 *
 * @author Ateng
 * @since 2026-05-06
 */
@EnableCaching
@SpringBootApplication
public class CaffeineApplication {

    public static void main(String[] args) {
        SpringApplication.run(CaffeineApplication.class, args);
    }

}
```

需要注意的是，`@EnableCaching` 只负责启用 Spring Cache 的代理能力，不负责指定具体缓存实现。具体使用 Caffeine、Redis 还是其他缓存组件，由依赖和缓存管理器配置决定。

### 配置 Caffeine 缓存管理器

在 Spring Boot 3 项目中，如果已经引入 `spring-boot-starter-cache` 和 `caffeine` 依赖，并且在配置文件中指定了 `spring.cache.type=caffeine`，Spring Boot 可以自动配置 `CaffeineCacheManager`。

对于简单项目，可以直接使用 `application.yml` 中的全局配置。对于实际业务项目，如果不同缓存空间需要不同的容量和过期时间，建议通过 Java 配置显式声明 `CacheManager`。

下面示例使用 `SimpleCacheManager` + `CaffeineCache` 为不同缓存名称配置不同的容量和过期时间。

文件位置：`src/main/java/io/github/atengk/cache/config/CaffeineCacheConfig.java`

这段代码用于自定义 Caffeine 缓存管理器，并为不同缓存空间设置不同缓存策略。

```java
package io.github.atengk.cache.config;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.ArrayUtil;
import cn.hutool.core.util.StrUtil;
import com.github.benmanes.caffeine.cache.Caffeine;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.CacheManager;
import org.springframework.cache.caffeine.CaffeineCache;
import org.springframework.cache.interceptor.KeyGenerator;
import org.springframework.cache.support.SimpleCacheManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.lang.reflect.Method;
import java.time.Duration;
import java.util.Arrays;
import java.util.stream.Collectors;

/**
 * Caffeine 缓存配置
 *
 * @author Ateng
 * @since 2026-05-06
 */
@Slf4j
@Configuration
public class CaffeineCacheConfig {

    /**
     * 配置缓存管理器
     *
     * @return 缓存管理器
     */
    @Bean
    public CacheManager cacheManager() {
        SimpleCacheManager cacheManager = new SimpleCacheManager();

        cacheManager.setCaches(CollUtil.newArrayList(
                buildCache("userCache", 5000, 10),
                buildCache("dictCache", 2000, 60),
                buildCache("configCache", 1000, 30)
        ));

        log.info("初始化 Caffeine 缓存管理器完成，缓存空间：userCache、dictCache、configCache");
        return cacheManager;
    }

    /**
     * 配置缓存 Key 生成器
     *
     * @return Key 生成器
     */
    @Bean("cacheKeyGenerator")
    public KeyGenerator cacheKeyGenerator() {
        return (Object target, Method method, Object... params) -> {
            Object[] safeParams = ArrayUtil.isEmpty(params) ? new Object[]{"none"} : params;
            String paramText = Arrays.stream(safeParams)
                    .map(String::valueOf)
                    .collect(Collectors.joining(":"));

            return StrUtil.format("{}:{}:{}", target.getClass().getSimpleName(), method.getName(), paramText);
        };
    }

    /**
     * 构建 Caffeine 缓存对象
     *
     * @param cacheName 缓存名称
     * @param maximumSize 最大缓存数量
     * @param expireAfterWriteMinutes 写入后过期分钟数
     * @return Caffeine 缓存对象
     */
    private CaffeineCache buildCache(String cacheName, long maximumSize, long expireAfterWriteMinutes) {
        return new CaffeineCache(
                cacheName,
                Caffeine.newBuilder()
                        // 初始化容量，减少缓存扩容成本
                        .initialCapacity(100)
                        // 最大缓存条目数，超过后自动淘汰
                        .maximumSize(maximumSize)
                        // 写入后过期时间
                        .expireAfterWrite(Duration.ofMinutes(expireAfterWriteMinutes))
                        // 开启缓存统计，便于后续观察命中率
                        .recordStats()
                        .build()
        );
    }

}
```

如果使用上述 Java 配置，`application.yml` 中可以保留基础缓存类型配置，也可以移除 `spring.cache.caffeine.spec`，避免全局配置和 Java 配置混淆。

文件位置：`src/main/resources/application.yml`

```yaml
spring:
  cache:
    # 指定缓存类型为 caffeine
    type: caffeine
```

如果项目只需要所有缓存使用同一套规则，则使用配置文件方式即可，不需要额外声明 `CacheManager` Bean。配置文件方式更简单，Java 配置方式更灵活。

### 设置缓存容量与过期时间

Caffeine 的容量和过期时间可以通过 `application.yml` 的 `spec` 配置，也可以通过 Java 代码中的 `Caffeine.newBuilder()` 配置。实际项目中，两种方式选择一种即可。

配置文件方式适合简单场景，所有缓存空间共用同一套规则。

文件位置：`src/main/resources/application.yml`

```yaml
spring:
  cache:
    type: caffeine
    cache-names:
      - userCache
      - dictCache
      - configCache
    caffeine:
      # maximumSize：最大缓存条目数
      # expireAfterWrite：写入后 10 分钟过期
      # recordStats：开启缓存统计
      spec: maximumSize=10000,expireAfterWrite=10m,recordStats
```

Java 配置方式适合不同缓存空间需要不同规则的场景。

| 缓存名称      | 建议容量 | 建议过期时间   | 适用数据             |
| ------------- | -------- | -------------- | -------------------- |
| `userCache`   | 5000     | 写入后 10 分钟 | 用户基础信息         |
| `dictCache`   | 2000     | 写入后 60 分钟 | 字典、枚举、区域编码 |
| `configCache` | 1000     | 写入后 30 分钟 | 系统配置、业务开关   |

常用容量和过期参数如下：

| 参数                | 示例                    | 说明                           |
| ------------------- | ----------------------- | ------------------------------ |
| `initialCapacity`   | `initialCapacity=100`   | 初始容量                       |
| `maximumSize`       | `maximumSize=10000`     | 最大缓存条目数                 |
| `expireAfterWrite`  | `expireAfterWrite=10m`  | 写入后经过指定时间过期         |
| `expireAfterAccess` | `expireAfterAccess=30m` | 最后一次访问后经过指定时间过期 |
| `recordStats`       | `recordStats`           | 开启缓存统计信息               |

容量不建议设置过大。Caffeine 是 JVM 本地缓存，缓存对象会占用应用堆内存。如果缓存条目数量过大，可能导致 Full GC 频率升高，甚至引发内存溢出。通常建议先按业务访问量设置一个保守值，再根据接口 QPS、缓存命中率、堆内存占用情况逐步调整。

## 注解式缓存开发

注解式缓存是 Spring Cache 最常用的开发方式。业务方法只需要通过注解声明缓存行为，Spring 会在方法调用前后自动完成缓存读取、写入和删除。

下面示例使用用户信息作为演示对象，模拟数据库查询、更新和删除流程。

示例文件结构如下：

```text
src/main/java/io/github/atengk/cache/model/UserCacheVO.java
src/main/java/io/github/atengk/cache/service/UserCacheService.java
src/main/java/io/github/atengk/cache/service/impl/UserCacheServiceImpl.java
src/main/java/io/github/atengk/cache/controller/UserCacheController.java
```

文件位置：`src/main/java/io/github/atengk/cache/model/UserCacheVO.java`

这段代码用于定义用户缓存示例对象。

```java
package io.github.atengk.cache.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 用户缓存视图对象
 *
 * @author Ateng
 * @since 2026-05-06
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserCacheVO {

    /**
     * 用户ID
     */
    private Long id;

    /**
     * 用户账号
     */
    private String username;

    /**
     * 用户昵称
     */
    private String nickname;

}
```

文件位置：`src/main/java/io/github/atengk/cache/service/UserCacheService.java`

这段代码用于定义用户缓存业务接口。

```java
package io.github.atengk.cache.service;

import io.github.atengk.cache.model.UserCacheVO;

/**
 * 用户缓存业务接口
 *
 * @author Ateng
 * @since 2026-05-06
 */
public interface UserCacheService {

    /**
     * 根据用户ID查询用户信息
     *
     * @param id 用户ID
     * @return 用户信息
     */
    UserCacheVO findById(Long id);

    /**
     * 保存或更新用户信息
     *
     * @param user 用户信息
     * @return 用户信息
     */
    UserCacheVO saveOrUpdate(UserCacheVO user);

    /**
     * 根据用户ID删除用户信息
     *
     * @param id 用户ID
     */
    void deleteById(Long id);

    /**
     * 清空所有用户缓存
     */
    void clearAll();

}
```

文件位置：`src/main/java/io/github/atengk/cache/service/impl/UserCacheServiceImpl.java`

这段代码用于演示 `@Cacheable`、`@CachePut` 和 `@CacheEvict` 的基本用法。

```java
package io.github.atengk.cache.service.impl;

import cn.hutool.core.util.ObjUtil;
import cn.hutool.core.util.StrUtil;
import io.github.atengk.cache.model.UserCacheVO;
import io.github.atengk.cache.service.UserCacheService;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.CachePut;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 用户缓存业务实现
 *
 * @author Ateng
 * @since 2026-05-06
 */
@Slf4j
@Service
public class UserCacheServiceImpl implements UserCacheService {

    private final Map<Long, UserCacheVO> userStore = new ConcurrentHashMap<>();

    /**
     * 初始化模拟数据
     */
    @PostConstruct
    public void initData() {
        userStore.put(1L, UserCacheVO.builder()
                .id(1L)
                .username("ateng")
                .nickname("阿腾")
                .build());

        userStore.put(2L, UserCacheVO.builder()
                .id(2L)
                .username("spring")
                .nickname("Spring用户")
                .build());

        log.info("初始化用户模拟数据完成，数量：{}", userStore.size());
    }

    /**
     * 根据用户ID查询用户信息
     *
     * @param id 用户ID
     * @return 用户信息
     */
    @Override
    @Cacheable(cacheNames = "userCache", key = "#id", condition = "#id != null", unless = "#result == null")
    public UserCacheVO findById(Long id) {
        if (ObjUtil.isNull(id)) {
            log.warn("查询用户信息失败，用户ID为空");
            return null;
        }

        log.info("缓存未命中，查询用户信息，用户ID：{}", id);
        return userStore.get(id);
    }

    /**
     * 保存或更新用户信息
     *
     * @param user 用户信息
     * @return 用户信息
     */
    @Override
    @CachePut(cacheNames = "userCache", key = "#user.id", condition = "#user != null && #user.id != null")
    public UserCacheVO saveOrUpdate(UserCacheVO user) {
        if (ObjUtil.isNull(user) || ObjUtil.isNull(user.getId())) {
            throw new IllegalArgumentException("用户ID不能为空");
        }

        if (StrUtil.isBlank(user.getUsername())) {
            user.setUsername(StrUtil.format("user_{}", user.getId()));
        }

        userStore.put(user.getId(), user);
        log.info("保存或更新用户信息完成，用户ID：{}", user.getId());
        return user;
    }

    /**
     * 根据用户ID删除用户信息
     *
     * @param id 用户ID
     */
    @Override
    @CacheEvict(cacheNames = "userCache", key = "#id", condition = "#id != null")
    public void deleteById(Long id) {
        if (ObjUtil.isNull(id)) {
            log.warn("删除用户信息失败，用户ID为空");
            return;
        }

        userStore.remove(id);
        log.info("删除用户信息完成，用户ID：{}", id);
    }

    /**
     * 清空所有用户缓存
     */
    @Override
    @CacheEvict(cacheNames = "userCache", allEntries = true)
    public void clearAll() {
        log.info("清空用户缓存完成");
    }

}
```

文件位置：`src/main/java/io/github/atengk/cache/controller/UserCacheController.java`

这段代码用于提供用户缓存测试接口。

```java
package io.github.atengk.cache.controller;

import io.github.atengk.cache.model.UserCacheVO;
import io.github.atengk.cache.service.UserCacheService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

/**
 * 用户缓存测试接口
 *
 * @author Ateng
 * @since 2026-05-06
 */
@RestController
@RequiredArgsConstructor
@RequestMapping("/cache/users")
public class UserCacheController {

    private final UserCacheService userCacheService;

    /**
     * 根据用户ID查询用户信息
     *
     * @param id 用户ID
     * @return 用户信息
     */
    @GetMapping("/{id}")
    public UserCacheVO findById(@PathVariable Long id) {
        return userCacheService.findById(id);
    }

    /**
     * 保存或更新用户信息
     *
     * @param user 用户信息
     * @return 用户信息
     */
    @PostMapping
    public UserCacheVO saveOrUpdate(@RequestBody UserCacheVO user) {
        return userCacheService.saveOrUpdate(user);
    }

    /**
     * 根据用户ID删除用户信息
     *
     * @param id 用户ID
     */
    @DeleteMapping("/{id}")
    public void deleteById(@PathVariable Long id) {
        userCacheService.deleteById(id);
    }

    /**
     * 清空所有用户缓存
     */
    @DeleteMapping
    public void clearAll() {
        userCacheService.clearAll();
    }

}
```

### 使用 @Cacheable 查询缓存

`@Cacheable` 用于查询缓存。方法执行前，Spring Cache 会先根据缓存名称和缓存 Key 查询缓存。如果缓存命中，则直接返回缓存中的数据；如果缓存未命中，则执行目标方法，并将返回结果写入缓存。

常用配置如下：

```java
@Cacheable(cacheNames = "userCache", key = "#id", condition = "#id != null", unless = "#result == null")
```

参数说明如下：

| 参数         | 说明                                       |
| ------------ | ------------------------------------------ |
| `cacheNames` | 缓存名称，对应 `CacheManager` 中的缓存空间 |
| `key`        | 缓存 Key，支持 SpEL 表达式                 |
| `condition`  | 方法执行前判断，满足条件才进行缓存处理     |
| `unless`     | 方法执行后判断，满足条件则不写入缓存       |

示例逻辑如下：

```java
@Cacheable(cacheNames = "userCache", key = "#id", condition = "#id != null", unless = "#result == null")
public UserCacheVO findById(Long id) {
    log.info("缓存未命中，查询用户信息，用户ID：{}", id);
    return userStore.get(id);
}
```

第一次请求 `/cache/users/1` 时，控制台会打印查询日志，表示方法真实执行。再次请求相同接口时，如果缓存未过期，方法不会再次执行，而是直接返回缓存数据。

### 使用 @CachePut 更新缓存

`@CachePut` 用于更新缓存。与 `@Cacheable` 不同，`@CachePut` 每次都会执行目标方法，然后将方法返回值写入缓存。

它适合用于新增或更新业务数据后同步刷新缓存，避免下次查询时读到旧缓存。

常用配置如下：

```java
@CachePut(cacheNames = "userCache", key = "#user.id", condition = "#user != null && #user.id != null")
```

示例逻辑如下：

```java
@CachePut(cacheNames = "userCache", key = "#user.id", condition = "#user != null && #user.id != null")
public UserCacheVO saveOrUpdate(UserCacheVO user) {
    userStore.put(user.getId(), user);
    log.info("保存或更新用户信息完成，用户ID：{}", user.getId());
    return user;
}
```

使用 `@CachePut` 时需要注意，方法返回值会作为新的缓存值写入缓存。如果方法返回 `null`，则可能导致缓存中写入空值或不符合预期的数据。因此，实际业务中建议返回更新后的完整对象。

### 使用 @CacheEvict 删除缓存

`@CacheEvict` 用于删除缓存。它常用于删除数据、更新数据后主动清理缓存、批量刷新缓存等场景。

删除指定 Key 的缓存：

```java
@CacheEvict(cacheNames = "userCache", key = "#id", condition = "#id != null")
public void deleteById(Long id) {
    userStore.remove(id);
    log.info("删除用户信息完成，用户ID：{}", id);
}
```

清空指定缓存空间下的所有缓存：

```java
@CacheEvict(cacheNames = "userCache", allEntries = true)
public void clearAll() {
    log.info("清空用户缓存完成");
}
```

常用参数如下：

| 参数               | 说明                                               |
| ------------------ | -------------------------------------------------- |
| `key`              | 删除指定缓存 Key                                   |
| `allEntries`       | 是否清空当前缓存名称下的所有缓存                   |
| `beforeInvocation` | 是否在方法执行前清理缓存，默认是方法成功执行后清理 |

如果删除数据库数据和删除缓存需要保持一致，通常建议在业务方法成功执行后再删除缓存，也就是使用默认的 `beforeInvocation = false`。

### 缓存 Key 设计

缓存 Key 是缓存能否正确命中的关键。Key 设计不合理会导致缓存冲突、缓存污染或无法命中。

常见 Key 写法如下：

| 写法          | 示例                                                         | 说明                     |
| ------------- | ------------------------------------------------------------ | ------------------------ |
| 固定参数      | `key = "#id"`                                                | 适合单参数查询           |
| 对象属性      | `key = "#user.id"`                                           | 适合对象参数             |
| 字符串拼接    | `key = "'user:' + #id"`                                      | 增加业务前缀，避免冲突   |
| 方法参数索引  | `key = "#p0"`                                                | 使用第一个参数           |
| Hutool 格式化 | `key = "T(cn.hutool.core.util.StrUtil).format('user:{}', #id)"` | 使用 Hutool 生成可读 Key |
| 自定义生成器  | `keyGenerator = "cacheKeyGenerator"`                         | 统一生成复杂 Key         |

推荐业务 Key 带上明确前缀，例如：

```java
@Cacheable(cacheNames = "userCache", key = "'user:' + #id")
public UserCacheVO findById(Long id) {
    return userStore.get(id);
}
```

如果系统存在租户、语言、渠道、区域等上下文信息，Key 中也应该包含这些维度。

```java
@Cacheable(cacheNames = "userCache", key = "'tenant:' + #tenantId + ':user:' + #id")
public UserCacheVO findByTenantAndId(Long tenantId, Long id) {
    return userStore.get(id);
}
```

使用自定义 Key 生成器时，可以直接引用前面 `CaffeineCacheConfig` 中定义的 `cacheKeyGenerator`。

```java
@Cacheable(cacheNames = "userCache", keyGenerator = "cacheKeyGenerator")
public UserCacheVO findById(Long id) {
    return userStore.get(id);
}
```

需要注意的是，`key` 和 `keyGenerator` 不能同时使用。简单场景建议直接使用 `key`，复杂场景再统一封装 `KeyGenerator`。

## 编程式缓存开发

编程式缓存是指在业务代码中直接注入 `CacheManager`，然后手动读取、写入、删除缓存。相比注解式缓存，编程式缓存更灵活，适合缓存逻辑复杂、Key 动态生成、需要手动控制缓存生命周期的场景。

常见适用场景如下：

| 场景         | 说明                                   |
| ------------ | -------------------------------------- |
| 动态缓存名称 | 缓存名称需要根据业务类型动态决定       |
| 复杂缓存 Key | Key 需要根据多个字段、上下文或规则生成 |
| 条件写入缓存 | 需要根据业务结果决定是否写入           |
| 批量删除缓存 | 需要循环删除多个 Key                   |
| 非方法级缓存 | 缓存逻辑不适合直接绑定到某个方法上     |

### 注入 CacheManager

`CacheManager` 是 Spring Cache 的核心接口，用于根据缓存名称获取具体的 `Cache` 对象。拿到 `Cache` 后，即可手动进行读取、写入、删除和清空操作。

文件位置：`src/main/java/io/github/atengk/cache/service/ManualUserCacheService.java`

这段代码用于演示通过 `CacheManager` 手动操作 Caffeine 缓存。

```java
package io.github.atengk.cache.service;

import cn.hutool.core.util.ObjUtil;
import cn.hutool.core.util.StrUtil;
import io.github.atengk.cache.model.UserCacheVO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.stereotype.Service;

/**
 * 用户缓存编程式操作服务
 *
 * @author Ateng
 * @since 2026-05-06
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ManualUserCacheService {

    private static final String USER_CACHE = "userCache";

    private final CacheManager cacheManager;

    /**
     * 手动读取用户缓存
     *
     * @param id 用户ID
     * @return 用户信息
     */
    public UserCacheVO getUser(Long id) {
        if (ObjUtil.isNull(id)) {
            log.warn("读取用户缓存失败，用户ID为空");
            return null;
        }

        Cache cache = getCache(USER_CACHE);
        Cache.ValueWrapper valueWrapper = cache.get(id);

        if (ObjUtil.isNull(valueWrapper)) {
            log.info("用户缓存未命中，用户ID：{}", id);
            return null;
        }

        Object value = valueWrapper.get();
        if (value instanceof UserCacheVO user) {
            log.info("用户缓存命中，用户ID：{}", id);
            return user;
        }

        log.warn("用户缓存类型不匹配，用户ID：{}", id);
        return null;
    }

    /**
     * 手动读取缓存，未命中时加载数据
     *
     * @param id 用户ID
     * @return 用户信息
     */
    public UserCacheVO getUserWithLoader(Long id) {
        if (ObjUtil.isNull(id)) {
            log.warn("加载用户缓存失败，用户ID为空");
            return null;
        }

        Cache cache = getCache(USER_CACHE);
        return cache.get(id, () -> {
            log.info("用户缓存未命中，执行数据加载，用户ID：{}", id);
            return UserCacheVO.builder()
                    .id(id)
                    .username(StrUtil.format("manual_{}", id))
                    .nickname("编程式加载用户")
                    .build();
        });
    }

    /**
     * 手动写入用户缓存
     *
     * @param user 用户信息
     */
    public void putUser(UserCacheVO user) {
        if (ObjUtil.isNull(user) || ObjUtil.isNull(user.getId())) {
            throw new IllegalArgumentException("用户ID不能为空");
        }

        Cache cache = getCache(USER_CACHE);
        cache.put(user.getId(), user);
        log.info("写入用户缓存完成，用户ID：{}", user.getId());
    }

    /**
     * 手动删除用户缓存
     *
     * @param id 用户ID
     */
    public void evictUser(Long id) {
        if (ObjUtil.isNull(id)) {
            log.warn("删除用户缓存失败，用户ID为空");
            return;
        }

        Cache cache = getCache(USER_CACHE);
        cache.evict(id);
        log.info("删除用户缓存完成，用户ID：{}", id);
    }

    /**
     * 手动清空用户缓存
     */
    public void clearUserCache() {
        Cache cache = getCache(USER_CACHE);
        cache.clear();
        log.info("清空用户缓存完成");
    }

    /**
     * 根据缓存名称获取缓存对象
     *
     * @param cacheName 缓存名称
     * @return 缓存对象
     */
    private Cache getCache(String cacheName) {
        Cache cache = cacheManager.getCache(cacheName);
        if (ObjUtil.isNull(cache)) {
            throw new IllegalArgumentException(StrUtil.format("缓存不存在：{}", cacheName));
        }
        return cache;
    }

}
```

### 手动读取缓存

手动读取缓存时，先通过 `cacheManager.getCache(cacheName)` 获取缓存对象，然后通过 `cache.get(key)` 获取缓存值。

基础读取方式如下：

```java
Cache cache = cacheManager.getCache("userCache");
Cache.ValueWrapper valueWrapper = cache.get(1L);
```

如果 `valueWrapper` 为 `null`，表示缓存未命中。如果不为 `null`，可以通过 `valueWrapper.get()` 获取真实缓存对象。

也可以使用 `cache.get(key, Callable)` 实现“缓存未命中时自动加载数据”的逻辑：

```java
UserCacheVO user = cache.get(1L, () -> {
    log.info("缓存未命中，执行数据加载");
    return queryUserFromDatabase(1L);
});
```

这种方式适合将查询数据库、查询远程接口或执行复杂计算的逻辑放入加载函数中。需要注意，加载函数不应包含耗时过长或不可控的逻辑，否则会影响接口响应时间。

### 手动写入与删除缓存

手动写入缓存使用 `cache.put(key, value)`。

```java
Cache cache = cacheManager.getCache("userCache");
cache.put(user.getId(), user);
```

手动删除指定缓存使用 `cache.evict(key)`。

```java
Cache cache = cacheManager.getCache("userCache");
cache.evict(userId);
```

手动清空整个缓存空间使用 `cache.clear()`。

```java
Cache cache = cacheManager.getCache("userCache");
cache.clear();
```

编程式缓存的优势是控制粒度更细，但业务代码会直接感知缓存操作，耦合度高于注解式缓存。一般建议优先使用注解式缓存；当注解无法满足复杂业务逻辑时，再使用编程式缓存补充。



## 业务示例实现

本节通过用户信息、字典数据和热点配置三个常见业务场景说明 Caffeine 在 Spring Boot 3 项目中的使用方式。三个示例分别对应查询型缓存、相对稳定数据缓存和热点配置缓存，基本覆盖日常业务开发中的本地缓存使用场景。

示例缓存空间规划如下：

| 缓存名称      | 业务含义         | 推荐过期策略       |
| ------------- | ---------------- | ------------------ |
| `userCache`   | 用户基础信息缓存 | 写入后 10 分钟过期 |
| `dictCache`   | 字典数据缓存     | 写入后 60 分钟过期 |
| `configCache` | 热点配置缓存     | 写入后 30 分钟过期 |

### 用户信息查询缓存

用户信息查询缓存适合用于缓存用户昵称、头像、账号状态、组织信息等读取频率较高但变更不频繁的数据。业务查询时优先读取本地缓存，缓存未命中时再查询数据库或其他数据源。

用户信息缓存的核心处理流程如下：

```text
请求用户信息
    ↓
根据用户ID生成缓存Key
    ↓
查询 userCache
    ↓
缓存命中：直接返回用户信息
    ↓
缓存未命中：查询数据库或数据源
    ↓
写入 userCache
    ↓
返回用户信息
```

用户信息查询建议使用 `@Cacheable`，更新用户信息时使用 `@CachePut`，删除用户信息时使用 `@CacheEvict`。

核心示例：

```java
@Cacheable(cacheNames = "userCache", key = "'user:' + #id", condition = "#id != null", unless = "#result == null")
public UserCacheVO findById(Long id) {
    log.info("缓存未命中，查询用户信息，用户ID：{}", id);
    return userStore.get(id);
}
```

接口验证方式：

```bash
# 第一次查询，缓存未命中，会进入业务方法
curl http://localhost:8080/cache/users/1

# 第二次查询，缓存命中，不会再次进入业务方法
curl http://localhost:8080/cache/users/1

# 更新用户信息，并同步刷新缓存
curl -X POST http://localhost:8080/cache/users \
  -H "Content-Type: application/json" \
  -d '{"id":1,"username":"ateng","nickname":"阿腾-已更新"}'

# 删除用户信息，并同步删除缓存
curl -X DELETE http://localhost:8080/cache/users/1
```

用户信息缓存需要注意缓存 Key 的业务前缀。相比直接使用 `key = "#id"`，推荐使用 `key = "'user:' + #id"`，这样可以降低不同业务共用缓存空间时的 Key 冲突风险。

### 字典数据缓存

字典数据通常包括状态码、业务类型、区域编码、枚举映射等数据。这类数据变化频率低、读取频率高，非常适合放入 Caffeine 本地缓存。

字典缓存通常按字典类型进行缓存，例如 `user_status`、`order_status`、`pay_type`。每个字典类型对应一组字典项。

示例文件结构如下：

```text
src/main/java/io/github/atengk/cache/model/DictItemVO.java
src/main/java/io/github/atengk/cache/service/DictCacheService.java
src/main/java/io/github/atengk/cache/service/impl/DictCacheServiceImpl.java
src/main/java/io/github/atengk/cache/controller/DictCacheController.java
```

文件位置：`src/main/java/io/github/atengk/cache/model/DictItemVO.java`

这段代码用于定义字典项缓存对象。

```java
package io.github.atengk.cache.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 字典项缓存对象
 *
 * @author Ateng
 * @since 2026-05-06
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DictItemVO {

    /**
     * 字典类型
     */
    private String dictType;

    /**
     * 字典编码
     */
    private String dictCode;

    /**
     * 字典名称
     */
    private String dictName;

    /**
     * 排序值
     */
    private Integer sort;

}
```

文件位置：`src/main/java/io/github/atengk/cache/service/DictCacheService.java`

这段代码用于定义字典缓存业务接口。

```java
package io.github.atengk.cache.service;

import io.github.atengk.cache.model.DictItemVO;

import java.util.List;

/**
 * 字典缓存业务接口
 *
 * @author Ateng
 * @since 2026-05-06
 */
public interface DictCacheService {

    /**
     * 根据字典类型查询字典项
     *
     * @param dictType 字典类型
     * @return 字典项列表
     */
    List<DictItemVO> listByType(String dictType);

    /**
     * 重新加载指定字典缓存
     *
     * @param dictType 字典类型
     * @return 字典项列表
     */
    List<DictItemVO> reloadByType(String dictType);

    /**
     * 删除指定字典缓存
     *
     * @param dictType 字典类型
     */
    void evictByType(String dictType);

}
```

文件位置：`src/main/java/io/github/atengk/cache/service/impl/DictCacheServiceImpl.java`

这段代码用于实现字典数据的查询缓存、缓存刷新和缓存删除。

```java
package io.github.atengk.cache.service.impl;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.StrUtil;
import io.github.atengk.cache.model.DictItemVO;
import io.github.atengk.cache.service.DictCacheService;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.CachePut;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 字典缓存业务实现
 *
 * @author Ateng
 * @since 2026-05-06
 */
@Slf4j
@Service
public class DictCacheServiceImpl implements DictCacheService {

    private final Map<String, List<DictItemVO>> dictStore = new ConcurrentHashMap<>();

    /**
     * 初始化模拟字典数据
     */
    @PostConstruct
    public void initData() {
        dictStore.put("user_status", CollUtil.newArrayList(
                DictItemVO.builder().dictType("user_status").dictCode("0").dictName("禁用").sort(1).build(),
                DictItemVO.builder().dictType("user_status").dictCode("1").dictName("启用").sort(2).build()
        ));

        dictStore.put("order_status", CollUtil.newArrayList(
                DictItemVO.builder().dictType("order_status").dictCode("created").dictName("已创建").sort(1).build(),
                DictItemVO.builder().dictType("order_status").dictCode("paid").dictName("已支付").sort(2).build(),
                DictItemVO.builder().dictType("order_status").dictCode("finished").dictName("已完成").sort(3).build()
        ));

        log.info("初始化字典模拟数据完成，字典类型数量：{}", dictStore.size());
    }

    /**
     * 根据字典类型查询字典项
     *
     * @param dictType 字典类型
     * @return 字典项列表
     */
    @Override
    @Cacheable(
            cacheNames = "dictCache",
            key = "'dict:' + #dictType",
            condition = "T(cn.hutool.core.util.StrUtil).isNotBlank(#dictType)",
            unless = "#result == null || #result.isEmpty()"
    )
    public List<DictItemVO> listByType(String dictType) {
        log.info("字典缓存未命中，查询字典数据，字典类型：{}", dictType);

        List<DictItemVO> dictItems = dictStore.get(dictType);
        if (CollUtil.isEmpty(dictItems)) {
            log.warn("未查询到字典数据，字典类型：{}", dictType);
            return CollUtil.newArrayList();
        }

        return dictItems.stream()
                .sorted(Comparator.comparing(DictItemVO::getSort))
                .toList();
    }

    /**
     * 重新加载指定字典缓存
     *
     * @param dictType 字典类型
     * @return 字典项列表
     */
    @Override
    @CachePut(
            cacheNames = "dictCache",
            key = "'dict:' + #dictType",
            condition = "T(cn.hutool.core.util.StrUtil).isNotBlank(#dictType)"
    )
    public List<DictItemVO> reloadByType(String dictType) {
        if (StrUtil.isBlank(dictType)) {
            throw new IllegalArgumentException("字典类型不能为空");
        }

        log.info("重新加载字典缓存，字典类型：{}", dictType);
        List<DictItemVO> dictItems = dictStore.get(dictType);
        return CollUtil.isEmpty(dictItems) ? CollUtil.newArrayList() : dictItems;
    }

    /**
     * 删除指定字典缓存
     *
     * @param dictType 字典类型
     */
    @Override
    @CacheEvict(
            cacheNames = "dictCache",
            key = "'dict:' + #dictType",
            condition = "T(cn.hutool.core.util.StrUtil).isNotBlank(#dictType)"
    )
    public void evictByType(String dictType) {
        log.info("删除字典缓存完成，字典类型：{}", dictType);
    }

}
```

文件位置：`src/main/java/io/github/atengk/cache/controller/DictCacheController.java`

这段代码用于提供字典缓存验证接口。

```java
package io.github.atengk.cache.controller;

import io.github.atengk.cache.model.DictItemVO;
import io.github.atengk.cache.service.DictCacheService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 字典缓存测试接口
 *
 * @author Ateng
 * @since 2026-05-06
 */
@RestController
@RequiredArgsConstructor
@RequestMapping("/cache/dicts")
public class DictCacheController {

    private final DictCacheService dictCacheService;

    /**
     * 根据字典类型查询字典项
     *
     * @param dictType 字典类型
     * @return 字典项列表
     */
    @GetMapping("/{dictType}")
    public List<DictItemVO> listByType(@PathVariable String dictType) {
        return dictCacheService.listByType(dictType);
    }

    /**
     * 重新加载指定字典缓存
     *
     * @param dictType 字典类型
     * @return 字典项列表
     */
    @PostMapping("/{dictType}/reload")
    public List<DictItemVO> reloadByType(@PathVariable String dictType) {
        return dictCacheService.reloadByType(dictType);
    }

    /**
     * 删除指定字典缓存
     *
     * @param dictType 字典类型
     */
    @DeleteMapping("/{dictType}")
    public void evictByType(@PathVariable String dictType) {
        dictCacheService.evictByType(dictType);
    }

}
```

接口验证方式：

```bash
# 查询用户状态字典，第一次请求会进入业务方法
curl http://localhost:8080/cache/dicts/user_status

# 再次查询用户状态字典，如果缓存未过期，则直接命中缓存
curl http://localhost:8080/cache/dicts/user_status

# 主动刷新指定字典缓存
curl -X POST http://localhost:8080/cache/dicts/user_status/reload

# 删除指定字典缓存
curl -X DELETE http://localhost:8080/cache/dicts/user_status
```

字典数据通常可以设置较长的过期时间。如果字典数据支持后台维护，建议在字典新增、修改、删除后主动清理或刷新对应字典缓存。

### 热点配置缓存

热点配置通常包括业务开关、阈值参数、功能开关、默认值配置等。这类配置经常被业务方法读取，但变更频率较低，适合使用 Caffeine 缓存。

热点配置缓存建议按配置 Key 进行缓存，例如 `order.timeout.minutes`、`feature.register.enabled`、`risk.max.retry.count`。

示例文件结构如下：

```text
src/main/java/io/github/atengk/cache/model/HotConfigVO.java
src/main/java/io/github/atengk/cache/service/HotConfigService.java
src/main/java/io/github/atengk/cache/service/impl/HotConfigServiceImpl.java
src/main/java/io/github/atengk/cache/controller/HotConfigController.java
```

文件位置：`src/main/java/io/github/atengk/cache/model/HotConfigVO.java`

这段代码用于定义热点配置缓存对象。

```java
package io.github.atengk.cache.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 热点配置缓存对象
 *
 * @author Ateng
 * @since 2026-05-06
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class HotConfigVO {

    /**
     * 配置键
     */
    private String configKey;

    /**
     * 配置值
     */
    private String configValue;

    /**
     * 配置说明
     */
    private String remark;

    /**
     * 更新时间
     */
    private LocalDateTime updateTime;

}
```

文件位置：`src/main/java/io/github/atengk/cache/service/HotConfigService.java`

这段代码用于定义热点配置缓存业务接口。

```java
package io.github.atengk.cache.service;

import io.github.atengk.cache.model.HotConfigVO;

/**
 * 热点配置缓存业务接口
 *
 * @author Ateng
 * @since 2026-05-06
 */
public interface HotConfigService {

    /**
     * 根据配置键查询配置
     *
     * @param configKey 配置键
     * @return 热点配置
     */
    HotConfigVO getByKey(String configKey);

    /**
     * 保存或刷新配置
     *
     * @param config 配置信息
     * @return 热点配置
     */
    HotConfigVO saveOrRefresh(HotConfigVO config);

    /**
     * 删除指定配置缓存
     *
     * @param configKey 配置键
     */
    void evictByKey(String configKey);

    /**
     * 清空热点配置缓存
     */
    void clearAll();

}
```

文件位置：`src/main/java/io/github/atengk/cache/service/impl/HotConfigServiceImpl.java`

这段代码用于实现热点配置的查询缓存、缓存刷新和缓存清理。

```java
package io.github.atengk.cache.service.impl;

import cn.hutool.core.util.ObjUtil;
import cn.hutool.core.util.StrUtil;
import io.github.atengk.cache.model.HotConfigVO;
import io.github.atengk.cache.service.HotConfigService;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.CachePut;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 热点配置缓存业务实现
 *
 * @author Ateng
 * @since 2026-05-06
 */
@Slf4j
@Service
public class HotConfigServiceImpl implements HotConfigService {

    private final Map<String, HotConfigVO> configStore = new ConcurrentHashMap<>();

    /**
     * 初始化模拟配置数据
     */
    @PostConstruct
    public void initData() {
        configStore.put("feature.register.enabled", HotConfigVO.builder()
                .configKey("feature.register.enabled")
                .configValue("true")
                .remark("是否开启用户注册")
                .updateTime(LocalDateTime.now())
                .build());

        configStore.put("order.timeout.minutes", HotConfigVO.builder()
                .configKey("order.timeout.minutes")
                .configValue("30")
                .remark("订单超时时间，单位分钟")
                .updateTime(LocalDateTime.now())
                .build());

        log.info("初始化热点配置模拟数据完成，配置数量：{}", configStore.size());
    }

    /**
     * 根据配置键查询配置
     *
     * @param configKey 配置键
     * @return 热点配置
     */
    @Override
    @Cacheable(
            cacheNames = "configCache",
            key = "'config:' + #configKey",
            condition = "T(cn.hutool.core.util.StrUtil).isNotBlank(#configKey)",
            unless = "#result == null"
    )
    public HotConfigVO getByKey(String configKey) {
        log.info("热点配置缓存未命中，查询配置数据，配置键：{}", configKey);
        return configStore.get(configKey);
    }

    /**
     * 保存或刷新配置
     *
     * @param config 配置信息
     * @return 热点配置
     */
    @Override
    @CachePut(
            cacheNames = "configCache",
            key = "'config:' + #config.configKey",
            condition = "#config != null && T(cn.hutool.core.util.StrUtil).isNotBlank(#config.configKey)"
    )
    public HotConfigVO saveOrRefresh(HotConfigVO config) {
        if (ObjUtil.isNull(config) || StrUtil.isBlank(config.getConfigKey())) {
            throw new IllegalArgumentException("配置键不能为空");
        }

        config.setUpdateTime(LocalDateTime.now());
        configStore.put(config.getConfigKey(), config);

        log.info("保存并刷新热点配置缓存完成，配置键：{}", config.getConfigKey());
        return config;
    }

    /**
     * 删除指定配置缓存
     *
     * @param configKey 配置键
     */
    @Override
    @CacheEvict(
            cacheNames = "configCache",
            key = "'config:' + #configKey",
            condition = "T(cn.hutool.core.util.StrUtil).isNotBlank(#configKey)"
    )
    public void evictByKey(String configKey) {
        if (StrUtil.isBlank(configKey)) {
            log.warn("删除热点配置缓存失败，配置键为空");
            return;
        }

        configStore.remove(configKey);
        log.info("删除热点配置和缓存完成，配置键：{}", configKey);
    }

    /**
     * 清空热点配置缓存
     */
    @Override
    @CacheEvict(cacheNames = "configCache", allEntries = true)
    public void clearAll() {
        log.info("清空热点配置缓存完成");
    }

}
```

文件位置：`src/main/java/io/github/atengk/cache/controller/HotConfigController.java`

这段代码用于提供热点配置缓存验证接口。

```java
package io.github.atengk.cache.controller;

import io.github.atengk.cache.model.HotConfigVO;
import io.github.atengk.cache.service.HotConfigService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

/**
 * 热点配置缓存测试接口
 *
 * @author Ateng
 * @since 2026-05-06
 */
@RestController
@RequiredArgsConstructor
@RequestMapping("/cache/configs")
public class HotConfigController {

    private final HotConfigService hotConfigService;

    /**
     * 根据配置键查询配置
     *
     * @param configKey 配置键
     * @return 热点配置
     */
    @GetMapping("/{configKey}")
    public HotConfigVO getByKey(@PathVariable String configKey) {
        return hotConfigService.getByKey(configKey);
    }

    /**
     * 保存或刷新配置
     *
     * @param config 配置信息
     * @return 热点配置
     */
    @PostMapping
    public HotConfigVO saveOrRefresh(@RequestBody HotConfigVO config) {
        return hotConfigService.saveOrRefresh(config);
    }

    /**
     * 删除指定配置缓存
     *
     * @param configKey 配置键
     */
    @DeleteMapping("/{configKey}")
    public void evictByKey(@PathVariable String configKey) {
        hotConfigService.evictByKey(configKey);
    }

    /**
     * 清空热点配置缓存
     */
    @DeleteMapping
    public void clearAll() {
        hotConfigService.clearAll();
    }

}
```

接口验证方式：

```bash
# 查询热点配置，第一次请求会进入业务方法
curl http://localhost:8080/cache/configs/feature.register.enabled

# 再次查询相同配置，如果缓存未过期，则直接命中缓存
curl http://localhost:8080/cache/configs/feature.register.enabled

# 保存配置，并刷新缓存
curl -X POST http://localhost:8080/cache/configs \
  -H "Content-Type: application/json" \
  -d '{"configKey":"feature.register.enabled","configValue":"false","remark":"是否开启用户注册"}'

# 删除指定配置，并删除对应缓存
curl -X DELETE http://localhost:8080/cache/configs/feature.register.enabled

# 清空全部热点配置缓存
curl -X DELETE http://localhost:8080/cache/configs
```

热点配置通常会影响业务判断，建议在配置变更后主动刷新缓存，不建议完全依赖自然过期。对于非常关键的开关配置，如果系统是多实例部署，还需要结合 Redis、消息队列或配置中心实现跨节点通知。

## 缓存失效策略

缓存失效策略用于控制缓存数据在什么时候被移除或重新加载。合理的失效策略可以避免缓存长期占用内存，也可以降低业务读取旧数据的概率。

Caffeine 常见失效方式包括基于时间的过期、基于容量的淘汰和业务主动清理。实际项目中通常会组合使用这些策略。

### 基于时间的过期策略

基于时间的过期策略是最常见的缓存失效方式。Caffeine 支持写入后过期和访问后过期，两者适用场景不同。

| 策略       | 配置                | 说明                           | 适用场景           |
| ---------- | ------------------- | ------------------------------ | ------------------ |
| 写入后过期 | `expireAfterWrite`  | 缓存写入后经过固定时间失效     | 用户信息、配置快照 |
| 访问后过期 | `expireAfterAccess` | 最后一次访问后经过固定时间失效 | 字典数据、热点数据 |
| 自定义过期 | `expireAfter`       | 根据业务规则设置不同过期时间   | 高级场景，配置复杂 |

配置文件方式：

文件位置：`src/main/resources/application.yml`

```yaml
spring:
  cache:
    type: caffeine
    caffeine:
      # 写入后 10 分钟过期，适合固定生命周期缓存
      spec: maximumSize=10000,expireAfterWrite=10m,recordStats
```

访问后过期配置：

```yaml
spring:
  cache:
    type: caffeine
    caffeine:
      # 最后一次访问后 30 分钟过期，适合访问频率高的热点缓存
      spec: maximumSize=10000,expireAfterAccess=30m,recordStats
```

Java 配置方式：

```java
Caffeine.newBuilder()
        // 写入后 10 分钟过期
        .expireAfterWrite(Duration.ofMinutes(10))
        // 最大缓存条目数
        .maximumSize(10000)
        // 开启缓存统计
        .recordStats()
        .build();
```

选择建议如下：

| 数据类型     | 推荐策略                                      | 原因                                 |
| ------------ | --------------------------------------------- | ------------------------------------ |
| 用户基础信息 | `expireAfterWrite`                            | 用户数据变更后应在固定时间内自然过期 |
| 字典数据     | `expireAfterAccess` 或较长 `expireAfterWrite` | 字典变化少，热点字典可以保留更久     |
| 热点配置     | `expireAfterWrite` + 主动刷新                 | 配置影响业务逻辑，变更后应主动刷新   |
| 计算结果     | `expireAfterWrite`                            | 计算结果通常具有明确有效期           |

如果业务对一致性要求较高，不建议只依赖时间过期，应在数据变更后主动清理或刷新缓存。

### 基于容量的淘汰策略

基于容量的淘汰策略用于限制缓存占用的内存空间。Caffeine 是本地缓存，数据存储在 JVM 堆内存中，如果不限制容量，缓存数据可能持续增长，最终影响应用稳定性。

配置文件方式：

文件位置：`src/main/resources/application.yml`

```yaml
spring:
  cache:
    type: caffeine
    caffeine:
      # 最多缓存 10000 个条目，超过后自动淘汰
      spec: maximumSize=10000,expireAfterWrite=10m,recordStats
```

Java 配置方式：

```java
Caffeine.newBuilder()
        // 初始化容量，适当降低扩容成本
        .initialCapacity(100)
        // 最大缓存数量，超过后由 Caffeine 自动淘汰
        .maximumSize(10000)
        // 写入后 10 分钟过期
        .expireAfterWrite(Duration.ofMinutes(10))
        .build();
```

容量设置需要结合数据量、对象大小、接口访问量和 JVM 堆内存综合评估。一般建议先使用较保守的容量配置，再根据缓存命中率和内存监控逐步调整。

常见容量建议如下：

| 缓存类型     | 建议容量     | 说明                         |
| ------------ | ------------ | ---------------------------- |
| 用户信息缓存 | 5000 - 50000 | 根据活跃用户量设置           |
| 字典缓存     | 1000 - 5000  | 字典数据通常较少             |
| 配置缓存     | 500 - 2000   | 配置项数量通常有限           |
| 查询结果缓存 | 1000 - 10000 | 需要避免缓存过多组合查询结果 |

不建议把分页列表、大对象、文件内容、超大 JSON 等直接放入本地缓存。此类数据容易造成内存占用不可控，必要时应改用 Redis、对象存储或数据库查询优化方案。

### 主动清理缓存

主动清理缓存用于在业务数据发生变化时立即删除或刷新缓存，避免用户继续读取旧数据。常见方式包括使用 `@CacheEvict` 注解和使用 `CacheManager` 编程式清理。

使用 `@CacheEvict` 删除单个缓存：

```java
@CacheEvict(cacheNames = "userCache", key = "'user:' + #id", condition = "#id != null")
public void deleteById(Long id) {
    userStore.remove(id);
    log.info("删除用户信息并清理缓存完成，用户ID：{}", id);
}
```

使用 `@CacheEvict` 清空整个缓存空间：

```java
@CacheEvict(cacheNames = "userCache", allEntries = true)
public void clearAllUserCache() {
    log.info("清空用户缓存完成");
}
```

使用 `CacheManager` 手动清理缓存：

```java
Cache cache = cacheManager.getCache("userCache");
if (cache != null) {
    cache.evict("user:1");
    log.info("手动删除用户缓存完成，缓存Key：user:1");
}
```

批量清理多个缓存空间时，可以封装一个统一的缓存管理服务。

文件位置：`src/main/java/io/github/atengk/cache/service/CacheAdminService.java`

这段代码用于统一管理多个缓存空间的主动清理操作。

```java
package io.github.atengk.cache.service;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.StrUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.stereotype.Service;

import java.util.Collection;

/**
 * 缓存管理服务
 *
 * @author Ateng
 * @since 2026-05-06
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class CacheAdminService {

    private final CacheManager cacheManager;

    /**
     * 清理指定缓存名称下的指定Key
     *
     * @param cacheName 缓存名称
     * @param key 缓存Key
     */
    public void evict(String cacheName, Object key) {
        if (StrUtil.isBlank(cacheName) || key == null) {
            log.warn("清理缓存失败，缓存名称或缓存Key为空");
            return;
        }

        Cache cache = cacheManager.getCache(cacheName);
        if (cache == null) {
            log.warn("清理缓存失败，缓存不存在，缓存名称：{}", cacheName);
            return;
        }

        cache.evict(key);
        log.info("清理缓存完成，缓存名称：{}，缓存Key：{}", cacheName, key);
    }

    /**
     * 清空指定缓存空间
     *
     * @param cacheName 缓存名称
     */
    public void clear(String cacheName) {
        if (StrUtil.isBlank(cacheName)) {
            log.warn("清空缓存失败，缓存名称为空");
            return;
        }

        Cache cache = cacheManager.getCache(cacheName);
        if (cache == null) {
            log.warn("清空缓存失败，缓存不存在，缓存名称：{}", cacheName);
            return;
        }

        cache.clear();
        log.info("清空缓存完成，缓存名称：{}", cacheName);
    }

    /**
     * 清空所有缓存空间
     */
    public void clearAll() {
        Collection<String> cacheNames = cacheManager.getCacheNames();
        if (CollUtil.isEmpty(cacheNames)) {
            log.warn("清空全部缓存结束，未发现缓存空间");
            return;
        }

        cacheNames.forEach(this::clear);
        log.info("清空全部缓存完成，缓存数量：{}", cacheNames.size());
    }

}
```

主动清理缓存适合后台管理操作、数据同步任务、配置发布任务等场景。对核心业务数据而言，推荐在数据变更成功后立即清理或刷新缓存，而不是等待缓存自然过期。

## 缓存一致性处理

缓存一致性处理用于降低缓存数据和真实数据源之间的不一致风险。Caffeine 是本地缓存，在单实例应用中一致性问题相对简单；但在多实例部署中，每个实例都有自己的本地缓存，必须额外考虑跨节点缓存同步问题。

本节主要说明单实例或单节点内的常见一致性处理方式。多实例场景可以结合 Redis、MQ、配置中心事件或服务广播机制扩展。

### 数据更新后的缓存刷新

数据更新后常见处理方式有两种：一种是更新数据库后删除缓存，下一次查询时重新加载；另一种是更新数据库后直接刷新缓存。

对于用户信息、配置项这类可以直接拿到更新后完整对象的数据，可以使用 `@CachePut` 刷新缓存。

```java
@CachePut(cacheNames = "userCache", key = "'user:' + #user.id", condition = "#user != null && #user.id != null")
public UserCacheVO saveOrUpdate(UserCacheVO user) {
    userStore.put(user.getId(), user);
    log.info("更新用户信息并刷新缓存完成，用户ID：{}", user.getId());
    return user;
}
```

处理流程如下：

```text
更新业务数据
    ↓
更新数据库或数据源成功
    ↓
方法返回最新对象
    ↓
@CachePut 将返回值写入缓存
    ↓
后续查询直接读取最新缓存
```

适用场景：

| 场景                   | 推荐方式                         |
| ---------------------- | -------------------------------- |
| 更新后可以获取完整对象 | 使用 `@CachePut` 刷新缓存        |
| 更新后只能获取部分字段 | 使用 `@CacheEvict` 删除缓存      |
| 更新逻辑复杂且涉及多表 | 优先删除缓存，避免写入不完整数据 |
| 批量更新大量数据       | 清理相关缓存或清空缓存空间       |

如果更新方法返回的不是完整业务对象，不建议直接使用 `@CachePut`。例如只返回 `Boolean`、影响行数或简单状态时，写入缓存的数据可能不是后续查询期望的数据。

不推荐示例：

```java
@CachePut(cacheNames = "userCache", key = "'user:' + #id")
public Boolean updateNickname(Long id, String nickname) {
    return Boolean.TRUE;
}
```

上面示例会把 `Boolean.TRUE` 写入 `userCache`，导致后续查询用户信息时出现类型不匹配问题。此类场景建议使用 `@CacheEvict` 删除缓存。

推荐示例：

```java
@CacheEvict(cacheNames = "userCache", key = "'user:' + #id", condition = "#id != null")
public Boolean updateNickname(Long id, String nickname) {
    log.info("更新用户昵称并删除缓存，用户ID：{}", id);
    return Boolean.TRUE;
}
```

### 数据删除后的缓存清理

数据删除后必须同步删除缓存，否则后续查询可能继续返回已删除的数据。删除缓存建议使用 `@CacheEvict`。

删除单条数据缓存：

```java
@CacheEvict(cacheNames = "userCache", key = "'user:' + #id", condition = "#id != null")
public void deleteById(Long id) {
    userStore.remove(id);
    log.info("删除用户数据并清理缓存完成，用户ID：{}", id);
}
```

批量删除时，如果可以明确计算出所有缓存 Key，可以逐个删除；如果无法准确计算 Key，建议清空对应缓存空间。

逐个删除示例：

```java
public void deleteBatch(List<Long> ids) {
    if (CollUtil.isEmpty(ids)) {
        log.warn("批量删除用户缓存失败，用户ID列表为空");
        return;
    }

    Cache cache = cacheManager.getCache("userCache");
    if (cache == null) {
        log.warn("批量删除用户缓存失败，缓存不存在：userCache");
        return;
    }

    ids.forEach(id -> cache.evict(StrUtil.format("user:{}", id)));
    log.info("批量删除用户缓存完成，数量：{}", ids.size());
}
```

清空缓存空间示例：

```java
@CacheEvict(cacheNames = "userCache", allEntries = true)
public void deleteByOrganizationId(Long organizationId) {
    log.info("按组织删除用户数据后清空用户缓存，组织ID：{}", organizationId);
}
```

删除缓存时需要注意事务边界。如果数据库删除失败，但缓存已经提前删除，通常只会导致下次重新加载旧数据；如果数据库删除成功，但缓存删除失败，则可能继续读取脏数据。因此实际业务中通常建议在数据变更成功后再清理缓存。

### 缓存穿透处理

缓存穿透是指查询一个不存在的数据，由于缓存中没有记录，每次请求都会直接打到数据库或底层数据源。如果恶意请求大量不存在的 Key，可能造成数据库压力异常升高。

常见处理方式包括参数校验、空值缓存、布隆过滤器和限流保护。

| 处理方式   | 说明                             | 适用场景                    |
| ---------- | -------------------------------- | --------------------------- |
| 参数校验   | 非法参数直接拒绝，不查询数据源   | ID 为空、格式错误、范围非法 |
| 空值缓存   | 不存在的数据也缓存一个短期空结果 | 查询不存在但请求频繁的数据  |
| 布隆过滤器 | 提前判断数据是否可能存在         | 数据量大、穿透风险高        |
| 限流保护   | 对异常高频访问进行限制           | 外部接口或公开接口          |

参数校验示例：

```java
@Cacheable(cacheNames = "userCache", key = "'user:' + #id", condition = "#id != null && #id > 0", unless = "#result == null")
public UserCacheVO findById(Long id) {
    log.info("查询用户信息，用户ID：{}", id);
    return userStore.get(id);
}
```

上面示例通过 `condition = "#id != null && #id > 0"` 避免空 ID 和非法 ID 进入缓存逻辑。

如果希望对不存在的数据进行短时间缓存，可以返回一个空对象标记，避免每次都查询底层数据源。

文件位置：`src/main/java/io/github/atengk/cache/model/UserCacheVO.java`

这段代码用于给用户缓存对象增加空数据标记字段。

```java
package io.github.atengk.cache.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 用户缓存视图对象
 *
 * @author Ateng
 * @since 2026-05-06
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserCacheVO {

    /**
     * 用户ID
     */
    private Long id;

    /**
     * 用户账号
     */
    private String username;

    /**
     * 用户昵称
     */
    private String nickname;

    /**
     * 是否为空数据标记
     */
    private Boolean emptyFlag;

}
```

空值缓存示例：

```java
@Cacheable(cacheNames = "userCache", key = "'user:' + #id", condition = "#id != null && #id > 0")
public UserCacheVO findByIdWithEmptyCache(Long id) {
    log.info("缓存未命中，查询用户信息，用户ID：{}", id);

    UserCacheVO user = userStore.get(id);
    if (user == null) {
        log.warn("用户不存在，写入空值缓存标记，用户ID：{}", id);
        return UserCacheVO.builder()
                .id(id)
                .emptyFlag(Boolean.TRUE)
                .build();
    }

    user.setEmptyFlag(Boolean.FALSE);
    return user;
}
```

调用方处理空值标记：

```java
UserCacheVO user = userCacheService.findByIdWithEmptyCache(id);
if (user != null && Boolean.TRUE.equals(user.getEmptyFlag())) {
    log.warn("用户不存在，用户ID：{}", id);
    return null;
}
return user;
```

使用空值缓存时需要注意过期时间不宜过长。不存在的数据未来可能被创建，如果空值缓存时间过长，可能导致新数据创建后仍然读取到空标记。实际项目中建议为空值缓存设置更短的过期时间；如果不同缓存 Key 需要不同过期时间，可以通过自定义 Caffeine 配置或拆分缓存空间实现。

对于公开接口、搜索接口、详情接口等容易被恶意构造参数访问的场景，仅靠本地缓存不够，还应结合参数校验、接口限流、访问鉴权、黑名单或网关防护共同处理。



## 接口验证

本节用于验证 Caffeine 缓存是否已经在 Spring Boot 3 项目中正常生效。验证重点包括查询缓存是否命中、更新数据后缓存是否刷新、删除数据后缓存是否清理。

验证前需要确保项目已经完成以下配置：

| 检查项         | 说明                                    |
| -------------- | --------------------------------------- |
| 已引入依赖     | `spring-boot-starter-cache`、`caffeine` |
| 已启用缓存     | 启动类或配置类上添加 `@EnableCaching`   |
| 已配置缓存空间 | `userCache`、`dictCache`、`configCache` |
| 已启动应用     | 默认端口为 `8080`                       |
| 已实现接口     | 用户、字典、配置缓存测试接口已创建      |

启动项目：

```bash
# 在项目根目录执行
mvn spring-boot:run
```

启动成功后，观察控制台是否出现类似日志：

```text
初始化 Caffeine 缓存管理器完成，缓存空间：userCache、dictCache、configCache
初始化用户模拟数据完成，数量：2
初始化字典模拟数据完成，字典类型数量：2
初始化热点配置模拟数据完成，配置数量：2
```

如果没有看到缓存相关日志，需要检查 `@EnableCaching`、`CacheManager` 配置类和缓存名称是否正确。

### 查询缓存验证

查询缓存验证用于确认 `@Cacheable` 是否生效。第一次请求时缓存不存在，会执行目标方法；第二次请求相同 Key 时，如果缓存未过期，应直接返回缓存结果，不再执行目标方法。

以用户信息查询为例。

```bash
# 第一次查询用户信息，缓存未命中，会进入业务方法
curl http://localhost:8080/cache/users/1

# 第二次查询相同用户信息，缓存命中，不应再次进入业务方法
curl http://localhost:8080/cache/users/1
```

第一次请求预期返回：

```json
{
  "id": 1,
  "username": "ateng",
  "nickname": "阿腾"
}
```

第一次请求时，控制台应出现类似日志：

```text
缓存未命中，查询用户信息，用户ID：1
```

第二次请求相同接口时，如果缓存命中，控制台不应再次打印上面的业务查询日志。这说明 `@Cacheable` 已经生效，请求结果来自 Caffeine 本地缓存。

字典缓存验证：

```bash
# 第一次查询用户状态字典，缓存未命中
curl http://localhost:8080/cache/dicts/user_status

# 第二次查询用户状态字典，缓存命中
curl http://localhost:8080/cache/dicts/user_status
```

热点配置缓存验证：

```bash
# 第一次查询热点配置，缓存未命中
curl http://localhost:8080/cache/configs/feature.register.enabled

# 第二次查询热点配置，缓存命中
curl http://localhost:8080/cache/configs/feature.register.enabled
```

如果每次请求都会打印“缓存未命中”日志，通常说明缓存没有生效。常见原因如下：

| 问题                     | 说明                                                 |
| ------------------------ | ---------------------------------------------------- |
| 未添加 `@EnableCaching`  | Spring Cache 代理未启用                              |
| 缓存名称不存在           | `cacheNames` 与配置的缓存名称不一致                  |
| 方法内部自调用           | 同一个类内部直接调用缓存方法，Spring 代理不会生效    |
| Key 不一致               | 查询、更新、删除使用的缓存 Key 规则不一致            |
| 返回结果被 `unless` 排除 | 例如 `unless = "#result == null"` 导致空结果不写缓存 |

为了更直观观察缓存状态，可以增加一个缓存检查接口。

文件位置：`src/main/java/io/github/atengk/cache/controller/CacheInspectController.java`

这段代码用于查看当前缓存名称、缓存大小和 Caffeine 统计信息。

```java
package io.github.atengk.cache.controller;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.map.MapUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.CacheManager;
import org.springframework.cache.caffeine.CaffeineCache;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Collection;
import java.util.Map;

/**
 * 缓存检查接口
 *
 * @author Ateng
 * @since 2026-05-06
 */
@RestController
@RequiredArgsConstructor
public class CacheInspectController {

    private final CacheManager cacheManager;

    /**
     * 查询缓存状态
     *
     * @return 缓存状态信息
     */
    @GetMapping("/cache/inspect")
    public Object inspect() {
        Collection<String> cacheNames = cacheManager.getCacheNames();
        if (CollUtil.isEmpty(cacheNames)) {
            return CollUtil.newArrayList();
        }

        return cacheNames.stream()
                .map(this::buildCacheInfo)
                .toList();
    }

    /**
     * 构建缓存状态信息
     *
     * @param cacheName 缓存名称
     * @return 缓存状态信息
     */
    private Map<String, Object> buildCacheInfo(String cacheName) {
        org.springframework.cache.Cache springCache = cacheManager.getCache(cacheName);
        Map<String, Object> cacheInfo = MapUtil.newHashMap();
        cacheInfo.put("cacheName", cacheName);

        if (springCache instanceof CaffeineCache caffeineCache) {
            com.github.benmanes.caffeine.cache.Cache<Object, Object> nativeCache = caffeineCache.getNativeCache();
            cacheInfo.put("estimatedSize", nativeCache.estimatedSize());
            cacheInfo.put("stats", nativeCache.stats().toString());
        }

        return cacheInfo;
    }

}
```

调用缓存检查接口：

```bash
curl http://localhost:8080/cache/inspect
```

如果前面已经多次查询用户、字典或配置接口，返回结果中应能看到对应缓存空间的 `estimatedSize` 发生变化。

### 更新缓存验证

更新缓存验证用于确认 `@CachePut` 是否生效。`@CachePut` 每次都会执行目标方法，并将方法返回值写入缓存。适合用于保存或更新数据后同步刷新缓存。

以用户信息更新为例。

先查询用户信息：

```bash
curl http://localhost:8080/cache/users/1
```

更新用户信息：

```bash
curl -X POST http://localhost:8080/cache/users \
  -H "Content-Type: application/json" \
  -d '{"id":1,"username":"ateng","nickname":"阿腾-已更新"}'
```

再次查询用户信息：

```bash
curl http://localhost:8080/cache/users/1
```

预期返回：

```json
{
  "id": 1,
  "username": "ateng",
  "nickname": "阿腾-已更新"
}
```

控制台应出现类似日志：

```text
保存或更新用户信息完成，用户ID：1
```

再次查询时，如果缓存已刷新，通常不会再打印“缓存未命中，查询用户信息”日志，而是直接从缓存中返回更新后的用户信息。

热点配置刷新验证：

```bash
# 保存配置，并刷新 configCache
curl -X POST http://localhost:8080/cache/configs \
  -H "Content-Type: application/json" \
  -d '{"configKey":"feature.register.enabled","configValue":"false","remark":"是否开启用户注册"}'

# 查询配置，确认返回的是更新后的值
curl http://localhost:8080/cache/configs/feature.register.enabled
```

预期返回：

```json
{
  "configKey": "feature.register.enabled",
  "configValue": "false",
  "remark": "是否开启用户注册"
}
```

更新缓存验证需要重点检查返回值。`@CachePut` 会将方法返回值写入缓存，如果方法只返回 `Boolean`、影响行数或状态码，就会把这些值写入缓存，导致后续查询出现类型不匹配或数据异常。

错误示例：

```java
@CachePut(cacheNames = "userCache", key = "'user:' + #id")
public Boolean updateNickname(Long id, String nickname) {
    return Boolean.TRUE;
}
```

正确做法是返回更新后的完整缓存对象，或者改用 `@CacheEvict` 删除缓存，让下一次查询重新加载。

### 删除缓存验证

删除缓存验证用于确认 `@CacheEvict` 是否生效。删除缓存后，再次查询相同数据时，应该重新进入业务方法。

以用户信息删除为例。

先查询用户信息，使缓存写入：

```bash
curl http://localhost:8080/cache/users/2
```

再次查询确认缓存命中：

```bash
curl http://localhost:8080/cache/users/2
```

删除用户信息和缓存：

```bash
curl -X DELETE http://localhost:8080/cache/users/2
```

再次查询用户信息：

```bash
curl http://localhost:8080/cache/users/2
```

由于示例中的 `deleteById` 同时删除了模拟数据和缓存，因此再次查询可能返回空结果。控制台应重新出现查询业务方法的日志，说明缓存已经被删除。

清空用户缓存：

```bash
curl -X DELETE http://localhost:8080/cache/users
```

清空字典缓存：

```bash
curl -X DELETE http://localhost:8080/cache/dicts/user_status
```

清空热点配置缓存：

```bash
curl -X DELETE http://localhost:8080/cache/configs
```

删除缓存时重点检查以下内容：

| 检查项                  | 说明                                           |
| ----------------------- | ---------------------------------------------- |
| `cacheNames` 是否一致   | 查询和删除必须操作同一个缓存空间               |
| `key` 是否一致          | 查询写入的 Key 和删除使用的 Key 必须完全一致   |
| 是否使用 `allEntries`   | `allEntries = true` 会清空整个缓存空间         |
| 是否受 `condition` 影响 | 条件不满足时不会执行缓存删除                   |
| 是否发生异常            | 默认情况下，方法异常时不会执行方法后的缓存删除 |

如果删除缓存后仍然返回旧数据，优先检查 Key 是否一致。例如查询使用的是 `key = "'user:' + #id"`，删除也必须使用同样规则：

```java
@CacheEvict(cacheNames = "userCache", key = "'user:' + #id")
public void deleteById(Long id) {
    // 删除业务数据
}
```

不能写成：

```java
@CacheEvict(cacheNames = "userCache", key = "#id")
public void deleteById(Long id) {
    // 删除业务数据
}
```

这两个 Key 分别是 `user:1` 和 `1`，不是同一个缓存 Key。

## 开发注意事项

本节用于说明 Caffeine 在 Spring Boot 3 项目中的常见开发风险。重点包括缓存对象序列化、缓存 Key 冲突和本地缓存适用边界。

Caffeine 使用简单，但它是本地进程缓存，和 Redis 这类集中式缓存有明显差异。实际项目中需要结合业务一致性要求、应用部署模式、数据规模和内存情况综合判断是否适合使用。

### 缓存对象序列化

Caffeine 默认将对象直接缓存在当前 JVM 内存中，不需要像 Redis 那样进行网络传输和序列化存储。因此在单独使用 Caffeine 时，缓存对象通常不需要实现 `Serializable` 接口。

但这并不表示缓存对象可以随意设计。Caffeine 缓存的是对象引用，如果缓存对象被外部代码修改，缓存中的对象也可能随之改变。

示例问题：

```java
UserCacheVO user = userCacheService.findById(1L);
user.setNickname("临时修改");
```

如果 `user` 是从 Caffeine 缓存中直接取出的对象引用，那么这次修改可能会影响缓存中的对象状态，导致后续读取到被修改后的数据。

推荐做法如下：

| 做法             | 说明                                       |
| ---------------- | ------------------------------------------ |
| 缓存不可变对象   | 使用不可变 DTO 或避免外部修改              |
| 返回副本对象     | 查询缓存后返回对象拷贝，减少引用污染       |
| 避免缓存复杂对象 | 不缓存包含连接、流、线程上下文等对象       |
| 避免缓存超大对象 | 大对象会增加 JVM 堆内存压力                |
| 控制字段范围     | 只缓存业务需要的字段，不缓存完整数据库实体 |

如果需要返回副本，可以使用 Hutool 的 `BeanUtil.copyProperties`。

文件位置：`src/main/java/io/github/atengk/cache/util/CacheObjectUtil.java`

这段代码用于复制缓存对象，避免调用方直接修改缓存中的对象引用。

```java
package io.github.atengk.cache.util;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.util.ObjUtil;

/**
 * 缓存对象工具类
 *
 * @author Ateng
 * @since 2026-05-06
 */
public final class CacheObjectUtil {

    private CacheObjectUtil() {
    }

    /**
     * 复制缓存对象
     *
     * @param source 源对象
     * @param targetClass 目标类型
     * @return 复制后的对象
     * @param <T> 目标类型
     */
    public static <T> T copy(Object source, Class<T> targetClass) {
        if (ObjUtil.isNull(source)) {
            return null;
        }
        return BeanUtil.copyProperties(source, targetClass);
    }

}
```

使用示例：

```java
UserCacheVO cachedUser = userCacheService.findById(1L);
UserCacheVO responseUser = CacheObjectUtil.copy(cachedUser, UserCacheVO.class);
```

如果后续计划将缓存实现从 Caffeine 切换到 Redis，缓存对象建议提前满足序列化要求，例如字段类型清晰、避免循环引用、避免缓存不可序列化资源对象。这样可以降低后续迁移成本。

### 缓存 Key 冲突

缓存 Key 冲突是缓存开发中最常见的问题之一。不同业务方法如果使用相同缓存名称和相同 Key，可能会互相覆盖缓存数据，导致返回错误结果。

错误示例：

```java
@Cacheable(cacheNames = "userCache", key = "#id")
public UserCacheVO findUserById(Long id) {
    return null;
}

@Cacheable(cacheNames = "userCache", key = "#id")
public Object findUserPermissionById(Long id) {
    return null;
}
```

上面两个方法都使用 `userCache` 和 `#id` 作为缓存 Key。如果 `id` 相同，就可能发生缓存覆盖或类型不匹配。

推荐使用业务前缀区分不同缓存数据：

```java
@Cacheable(cacheNames = "userCache", key = "'user:info:' + #id")
public UserCacheVO findUserById(Long id) {
    return null;
}

@Cacheable(cacheNames = "userCache", key = "'user:permission:' + #id")
public Object findUserPermissionById(Long id) {
    return null;
}
```

常见 Key 设计建议如下：

| 场景     | 推荐 Key                      |
| -------- | ----------------------------- |
| 用户详情 | `user:info:{id}`              |
| 用户权限 | `user:permission:{id}`        |
| 字典类型 | `dict:{dictType}`             |
| 配置项   | `config:{configKey}`          |
| 租户用户 | `tenant:{tenantId}:user:{id}` |
| 分页查询 | `query:{hash}`                |

如果 Key 由多个动态参数组成，建议保持参数顺序固定，避免同一业务含义生成多个不同 Key。

示例：

```java
@Cacheable(cacheNames = "userCache", key = "'tenant:' + #tenantId + ':user:' + #userId")
public UserCacheVO findTenantUser(Long tenantId, Long userId) {
    return null;
}
```

复杂查询条件不建议直接拼接完整对象字符串，因为对象默认 `toString()` 可能不稳定，也可能包含过多字段。可以提取关键字段，或者对查询条件生成摘要。

使用 Hutool 生成摘要 Key 示例：

```java
String keySource = StrUtil.format("{}:{}:{}", tenantId, status, keyword);
String cacheKey = SecureUtil.md5(keySource);
```

需要引入 Hutool crypto 模块时，可以添加依赖。

文件位置：`pom.xml`

```xml
<!-- Hutool 加密摘要工具，用于复杂缓存Key生成 -->
<dependency>
    <groupId>cn.hutool</groupId>
    <artifactId>hutool-crypto</artifactId>
    <version>5.8.36</version>
</dependency>
```

缓存 Key 设计原则如下：

| 原则     | 说明                                         |
| -------- | -------------------------------------------- |
| 可读性   | 简单业务优先使用可读 Key                     |
| 唯一性   | 同一缓存空间内不同业务不能共用相同 Key       |
| 稳定性   | 同一业务参数必须生成相同 Key                 |
| 简洁性   | 避免 Key 过长                                |
| 维度完整 | 多租户、多语言、多渠道场景必须包含上下文维度 |

### 本地缓存适用边界

Caffeine 是本地缓存，数据只存在当前应用实例的 JVM 内存中。应用重启后缓存会丢失，多实例之间缓存也不会自动同步。

本地缓存适合以下场景：

| 场景             | 说明                       |
| ---------------- | -------------------------- |
| 单实例应用       | 缓存只需要在当前进程内生效 |
| 读多写少数据     | 例如字典、配置、基础信息   |
| 短期热点数据     | 允许短时间不一致           |
| 对延迟敏感的接口 | 本地内存读取速度快         |
| 降低数据库压力   | 缓存热点查询结果           |

本地缓存不适合以下场景：

| 场景             | 原因                             |
| ---------------- | -------------------------------- |
| 强一致业务数据   | 多实例缓存无法自动同步           |
| 大容量数据缓存   | 会占用 JVM 堆内存                |
| 跨服务共享缓存   | 其他服务无法直接读取本地缓存     |
| 长期持久化数据   | 应用重启后缓存丢失               |
| 高频变更数据     | 容易出现短时间脏数据             |
| 分布式锁、计数器 | 应使用 Redis、数据库或专用中间件 |

在多实例部署中，每个实例都有自己的 Caffeine 缓存。例如部署了三个应用实例：

```text
用户请求
    ↓
负载均衡
    ↓
实例 A：userCache
实例 B：userCache
实例 C：userCache
```

如果实例 A 更新了用户信息并刷新了本地缓存，实例 B 和实例 C 的本地缓存不会自动变化。因此，多实例场景下需要额外处理缓存同步。

常见处理方式如下：

| 方案                      | 说明                                             |
| ------------------------- | ------------------------------------------------ |
| 缩短过期时间              | 降低脏数据存在时间                               |
| 数据变更后广播事件        | 通过 MQ、Redis Pub/Sub、配置中心事件通知其他实例 |
| 使用 Redis 作为集中式缓存 | 多实例共享同一份缓存                             |
| Redis + Caffeine 多级缓存 | Redis 保证共享，Caffeine 加速热点访问            |
| 对关键数据不使用本地缓存  | 强一致数据直接查数据库或集中式缓存               |

推荐选择方式：

| 业务要求               | 推荐方案                         |
| ---------------------- | -------------------------------- |
| 单实例、读多写少       | 直接使用 Caffeine                |
| 多实例、允许短暂不一致 | Caffeine + 较短过期时间          |
| 多实例、需要共享缓存   | Redis                            |
| 高并发热点读取         | Redis + Caffeine 多级缓存        |
| 强一致核心数据         | 谨慎使用缓存，优先保证数据正确性 |

最终建议是：Caffeine 适合作为应用内部的性能优化手段，不应作为系统级共享数据源。对于字典、配置、热点查询等读多写少场景，它能显著降低访问延迟；对于订单状态、账户余额、库存扣减、支付状态等强一致数据，应优先保证数据库或集中式缓存的一致性，再谨慎评估是否引入本地缓存。