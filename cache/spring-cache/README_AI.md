# Spring Cache

## 模块概述

本模块用于在 Spring Boot 3 项目中统一接入 Spring Cache 缓存抽象，屏蔽底层缓存组件差异，使业务代码通过注解完成查询缓存、缓存更新和缓存删除。Spring Cache 的核心能力是基于方法调用进行缓存处理：命中缓存时直接返回缓存结果，未命中时执行目标方法并写入缓存。Spring Boot 在启用缓存能力后会自动配置缓存基础设施。([Home](https://docs.spring.io/spring-boot/3.5/reference/io/caching.html))

### Spring Cache 功能定位

Spring Cache 是 Spring Framework 提供的缓存抽象层，不直接限定缓存数据必须存储在本地内存、Redis、Caffeine、JCache 或其他组件中。它通过 `Cache` 和 `CacheManager` 抽象统一管理缓存读写，由具体缓存实现负责数据存储、过期策略、容量限制和分布式访问能力。Spring Boot 在未显式定义 `CacheManager` 或 `CacheResolver` 时，会按顺序自动检测可用缓存提供者，也可以通过 `spring.cache.type` 强制指定缓存类型。([Home](https://docs.spring.io/spring-boot/3.5/reference/io/caching.html))

在业务开发中，Spring Cache 的主要定位如下：

| 定位             | 说明                                                         |
| ---------------- | ------------------------------------------------------------ |
| 方法级缓存抽象   | 通过 `@Cacheable`、`@CachePut`、`@CacheEvict` 等注解对 Service 方法进行缓存控制 |
| 统一缓存入口     | 业务代码不直接依赖 Redis、Caffeine 等具体实现，降低缓存组件替换成本 |
| 降低重复查询     | 对高频、低变化、可复用的数据进行缓存，减少数据库、远程接口或复杂计算压力 |
| 支持缓存治理     | 通过缓存名称、Key 规则、过期时间、序列化策略等方式规范缓存使用 |
| 适配多种缓存实现 | 可接入 Redis、Caffeine、JCache、Cache2k、Simple 等缓存提供者 |

Spring Cache 不负责解决所有缓存一致性问题，也不替代业务层的数据更新流程。它更适合用于“读多写少、允许短时间弱一致、结果可复用”的场景。

### 适用业务场景

Spring Cache 适合用于缓存访问频率高、查询成本高、数据变化频率低或对实时一致性要求不强的数据。常见场景包括：

| 场景         | 示例                                 | 推荐缓存实现      |
| ------------ | ------------------------------------ | ----------------- |
| 字典数据     | 性别、状态、类型、地区、业务枚举     | Redis / Caffeine  |
| 系统配置     | 参数配置、开关配置、规则配置         | Redis             |
| 权限数据     | 用户菜单、角色权限、接口权限         | Redis             |
| 热点详情     | 商品详情、文章详情、机构详情         | Redis             |
| 远程接口结果 | 第三方接口、内部 RPC 查询结果        | Redis / Caffeine  |
| 复杂计算结果 | 统计汇总、报表中间结果、规则计算结果 | Redis / Caffeine  |
| 单机临时缓存 | 本地开发、单体应用短生命周期缓存     | Caffeine / Simple |

优先将 Spring Cache 应用于 Service 层查询方法，而不是 Controller 层。Controller 层通常包含协议转换、参数校验和响应包装；Service 层更接近业务查询边界，缓存 Key 也更稳定。

### 缓存使用边界

缓存不是数据源，不应作为业务数据的唯一可信存储。缓存数据必须能够从数据库、远程服务或其他权威数据源重新构建。

以下场景不建议直接使用 Spring Cache，或需要谨慎设计：

| 边界               | 说明                                                         |
| ------------------ | ------------------------------------------------------------ |
| 强一致核心交易     | 订单支付、库存扣减、账户余额、资金流水等强一致场景不应仅依赖注解缓存 |
| 高频写入数据       | 数据频繁变更会导致缓存频繁失效，收益较低且容易引入一致性问题 |
| 大对象缓存         | 大列表、大报表、大文件内容会增加 Redis 或 JVM 内存压力       |
| 用户高度个性化数据 | Key 维度过多会导致缓存膨胀，需要控制 TTL 和容量              |
| 不可预测 Key       | 使用复杂对象、无规范拼接或包含随机值的 Key，会降低命中率     |
| 跨方法内部调用     | 同一个 Bean 内部方法调用可能绕过 Spring 代理，导致缓存注解不生效 |
| 批量数据更新       | 批量导入、批量修改后需要明确缓存清理策略，避免旧数据长期存在 |

建议遵循以下原则：

1. 缓存只加在查询稳定、耗时明显、命中率可预期的方法上。
2. 所有缓存都必须设置合理的过期策略，生产环境不建议无限期缓存。
3. 更新数据时优先删除缓存，而不是尝试手动同步多个缓存副本。
4. 缓存 Key 必须具备可读性、唯一性和可维护性。
5. 对空值缓存、缓存穿透、缓存击穿、缓存雪崩需要单独设计防护策略。

## 环境准备

本章节说明 Spring Boot 3 项目接入 Spring Cache 前需要确认的版本、缓存组件和依赖配置。本文以 Spring Boot 3.5.x 为基线；官方 Spring Boot 3.5 文档当前显示 3.5.14，要求至少 Java 17，并依赖 Spring Framework 6.2.18 或更高版本。([Home](https://docs.spring.io/spring-boot/3.5/system-requirements.html))

### Spring Boot 版本要求

Spring Boot 3 对基础运行环境有明确要求，项目接入 Spring Cache 前应先统一 JDK、构建工具和 Spring Boot 版本，避免依赖冲突。

| 项目             | 要求                                                         |
| ---------------- | ------------------------------------------------------------ |
| JDK              | Java 17 或更高版本                                           |
| Spring Boot      | 建议使用 Spring Boot 3.3.x、3.4.x 或 3.5.x 的稳定补丁版本    |
| Spring Framework | 由 Spring Boot BOM 管理，不建议手动覆盖                      |
| Maven            | 3.6.3 或更高版本                                             |
| Gradle           | Gradle 7.x 或 8.x，按当前 Spring Boot 小版本要求选择         |
| Servlet 容器     | Spring Boot 3.5 默认支持 Tomcat 10.1、Jetty 12、Undertow 2.3 |

版本选择建议：

| 项目类型                | 推荐版本策略                                                 |
| ----------------------- | ------------------------------------------------------------ |
| 新项目                  | 优先选择当前 Spring Boot 3 稳定补丁版本，例如 3.5.x          |
| 存量 Spring Boot 3 项目 | 保持当前小版本线，只升级补丁版本，例如 3.3.x -> 3.3 最新补丁 |
| 从 Spring Boot 2 升级   | 先完成 Jakarta 包名迁移，再接入 Spring Cache                 |
| 企业长期维护项目        | 固定 Spring Boot BOM 版本，避免业务模块单独覆盖 Spring 相关依赖 |

Maven 项目建议使用 Spring Boot Parent 管理依赖版本：

```xml
<!-- Spring Boot 3 项目父工程，统一管理 Spring 生态依赖版本 -->
<parent>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-parent</artifactId>
    <version>3.5.14</version>
    <relativePath/>
</parent>

<properties>
    <!-- Spring Boot 3 最低要求 Java 17 -->
    <java.version>17</java.version>

    <!-- Hutool 用于业务工具类、Key 处理、对象判空等辅助场景 -->
    <hutool.version>5.8.44</hutool.version>
</properties>
```

### 缓存组件选择

Spring Cache 只是缓存抽象，不负责实际存储。Spring Boot 会根据项目依赖自动检测缓存提供者，支持 Generic、JCache、Hazelcast、Infinispan、Couchbase、Redis、Caffeine、Cache2k、Simple 等实现；未添加具体缓存库时，会使用基于 `ConcurrentHashMap` 的 Simple 本地缓存，但该实现不推荐用于生产环境。([Home](https://docs.spring.io/spring-boot/3.5/reference/io/caching.html))

常见缓存组件选择如下：

| 缓存组件           | 类型       | 优点                                           | 局限                                 | 推荐场景                               |
| ------------------ | ---------- | ---------------------------------------------- | ------------------------------------ | -------------------------------------- |
| Redis              | 分布式缓存 | 多实例共享、支持 TTL、便于运维观测、适合微服务 | 需要独立 Redis 服务，存在网络开销    | 生产环境、集群部署、权限/字典/热点数据 |
| Caffeine           | 本地缓存   | 性能高、无网络开销、支持容量和过期策略         | 多实例数据不共享，重启丢失           | 单体应用、本地热点、短生命周期缓存     |
| Simple             | 本地内存   | 零配置、适合快速验证                           | 无容量治理、无分布式能力，不推荐生产 | 本地开发、Demo、单元测试               |
| JCache / Ehcache 3 | 标准缓存   | 标准化 API，生态成熟                           | 配置复杂度相对更高                   | 需要 JSR-107 标准兼容的项目            |
| Cache2k            | 本地缓存   | 高性能本地缓存                                 | 团队使用成本较高                     | 对本地缓存性能有明确要求的场景         |

推荐选择策略：

1. 微服务、集群部署、需要多实例共享缓存时，优先选择 Redis。
2. 单体应用或对极致读取性能敏感、且允许每个实例各自缓存时，选择 Caffeine。
3. 本地开发、功能验证、测试环境可以使用 Simple 或 `spring.cache.type=none`。
4. 不建议在同一业务方法上混用 Spring Cache 注解和 JCache 注解，官方文档也建议不要混用两套注解体系。([Home](https://docs.spring.io/spring-boot/3.5/reference/io/caching.html))

生产环境建议 Redis 作为默认方案，Caffeine 作为本地热点优化方案。若后续需要实现多级缓存，可在独立章节中通过自定义 `CacheManager` 或组合缓存方案实现，不建议在初始接入阶段直接复杂化。

### 项目依赖配置

项目需要先引入 `spring-boot-starter-cache`。该 Starter 会提供 Spring Cache 基础能力，并引入 `spring-context-support`。如果选择 Redis，还需要引入 `spring-boot-starter-data-redis`；如果选择 Caffeine，则额外引入 `com.github.ben-manes.caffeine:caffeine`。([Home](https://docs.spring.io/spring-boot/3.5/reference/io/caching.html))

Redis 方案适合生产环境和分布式部署，依赖配置如下。

```xml
<dependencies>
    <!-- Web 接口开发基础依赖，根据项目是否提供 HTTP API 决定是否需要 -->
    <dependency>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-starter-web</artifactId>
    </dependency>

    <!-- Spring Cache 缓存抽象基础依赖，提供 @Cacheable、@CachePut、@CacheEvict 等注解能力 -->
    <dependency>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-starter-cache</artifactId>
    </dependency>

    <!-- Redis 缓存实现依赖，用于生产环境分布式缓存 -->
    <dependency>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-starter-data-redis</artifactId>
    </dependency>

    <!-- Hutool 工具类，可用于缓存 Key 处理、对象判断、集合处理等业务辅助逻辑 -->
    <dependency>
        <groupId>cn.hutool</groupId>
        <artifactId>hutool-all</artifactId>
        <version>${hutool.version}</version>
    </dependency>

    <!-- Lombok 简化实体、DTO、日志对象等样板代码，编译期生效 -->
    <dependency>
        <groupId>org.projectlombok</groupId>
        <artifactId>lombok</artifactId>
        <optional>true</optional>
    </dependency>

    <!-- Spring Boot 测试依赖，用于缓存逻辑单元测试和接口测试 -->
    <dependency>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-starter-test</artifactId>
        <scope>test</scope>
    </dependency>
</dependencies>
```

Caffeine 方案适合单体应用或本地热点缓存，依赖配置如下。

```xml
<dependencies>
    <!-- Spring Cache 缓存抽象基础依赖 -->
    <dependency>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-starter-cache</artifactId>
    </dependency>

    <!-- Caffeine 本地缓存实现，适合高性能本地缓存场景 -->
    <dependency>
        <groupId>com.github.ben-manes.caffeine</groupId>
        <artifactId>caffeine</artifactId>
    </dependency>

    <!-- Hutool 工具类，可用于缓存 Key 拼接、参数规范化、集合处理等辅助逻辑 -->
    <dependency>
        <groupId>cn.hutool</groupId>
        <artifactId>hutool-all</artifactId>
        <version>${hutool.version}</version>
    </dependency>
</dependencies>
```

如果项目使用 Gradle，可以按如下方式配置。

```gradle
dependencies {
    // Spring Cache 缓存抽象基础依赖
    implementation 'org.springframework.boot:spring-boot-starter-cache'

    // Redis 缓存实现，生产环境分布式缓存推荐使用
    implementation 'org.springframework.boot:spring-boot-starter-data-redis'

    // Caffeine 本地缓存实现，如只使用 Redis 可移除该依赖
    implementation 'com.github.ben-manes.caffeine:caffeine'

    // Hutool 工具类，用于业务辅助处理
    implementation 'cn.hutool:hutool-all:5.8.44'

    // Spring Boot 测试依赖
    testImplementation 'org.springframework.boot:spring-boot-starter-test'
}
```

依赖引入后，需要在配置文件中明确缓存类型。Redis 方案示例：

```yaml
spring:
  cache:
    # 明确指定缓存实现，避免类路径存在多个缓存组件时产生歧义
    type: redis
    # 声明缓存空间名称，便于启动时初始化和统一治理
    cache-names:
      - userCache
      - dictCache
      - configCache
    redis:
      # 缓存默认过期时间，生产环境不建议永久缓存
      time-to-live: 30m
      # 保留缓存名前缀，避免不同缓存空间出现 Key 冲突
      use-key-prefix: true
      # 不缓存 null 值，降低缓存穿透和脏数据风险
      cache-null-values: false

  data:
    redis:
      # Redis 服务地址
      host: localhost
      # Redis 服务端口
      port: 6379
      # Redis 数据库索引，根据环境规划选择
      database: 0
      # Redis 连接超时时间
      timeout: 3s
```

Caffeine 方案示例：

```yaml
spring:
  cache:
    # 明确指定 Caffeine 本地缓存
    type: caffeine
    # 声明允许使用的缓存空间
    cache-names:
      - userCache
      - dictCache
      - configCache
    caffeine:
      # maximumSize 控制最大缓存条目数，expireAfterWrite 控制写入后的过期时间
      spec: maximumSize=10000,expireAfterWrite=30m
```

本地测试或不希望启用缓存时，可以使用空实现：

```yaml
spring:
  cache:
    # 禁用真实缓存实现，适合测试环境或排查缓存影响
    type: none
```

依赖配置完成后，后续章节即可继续编写 `@EnableCaching` 启用缓存、`CacheManager` 配置、核心注解使用、缓存 Key 设计、Redis 序列化和缓存一致性处理等内容。



## 基础配置

本章节用于完成 Spring Cache 的启用、配置文件编写和 `CacheManager` 定制。Spring Cache 本身只提供缓存抽象，真正的缓存读写由 `CacheManager` 管理；Spring Boot 会在项目引入缓存依赖后根据类路径自动检测缓存实现，也可以通过 `spring.cache.type` 明确指定 Redis、Caffeine、Simple 等缓存类型。([Home](https://docs.spring.io/spring-boot/reference/io/caching.html?utm_source=chatgpt.com))

### 启用缓存支持

Spring Cache 的注解不会自动生效，需要在 Spring Boot 启动类或配置类上添加 `@EnableCaching`。该注解会启用基于 Spring AOP 的缓存拦截能力，使 `@Cacheable`、`@CachePut`、`@CacheEvict`、`@Caching` 等注解在 Spring Bean 方法调用时生效。([Home](https://docs.spring.io/spring-framework/reference/integration/cache/annotations.html?utm_source=chatgpt.com))

文件位置：`src/main/java/io/github/atengk/CacheApplication.java`

下面的启动类用于启用 Spring Boot 应用和 Spring Cache 注解支持。

```java
package io.github.atengk;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cache.annotation.EnableCaching;

/**
 * Spring Cache 示例应用启动类
 *
 * @author Ateng
 * @since 2026-05-06
 */
@EnableCaching
@SpringBootApplication
public class CacheApplication {

    /**
     * 启动应用
     *
     * @param args 启动参数
     */
    public static void main(String[] args) {
        SpringApplication.run(CacheApplication.class, args);
    }

}
```

也可以将 `@EnableCaching` 放到独立配置类中，适合缓存配置较多、需要统一管理缓存 Bean 的项目。

文件位置：`src/main/java/io/github/atengk/config/CacheEnableConfig.java`

下面的配置类只负责启用缓存能力，适合与启动类解耦。

```java
package io.github.atengk.config;

import org.springframework.cache.annotation.EnableCaching;
import org.springframework.context.annotation.Configuration;

/**
 * Spring Cache 启用配置
 *
 * @author Ateng
 * @since 2026-05-06
 */
@EnableCaching
@Configuration
public class CacheEnableConfig {
}
```

注意：同一个项目中只需要启用一次缓存支持，不需要在多个配置类上重复添加 `@EnableCaching`。

### 缓存配置文件

缓存配置文件用于声明缓存类型、缓存空间、默认过期时间和底层缓存连接参数。生产环境通常推荐使用 Redis，因为 Redis 支持多实例共享缓存、TTL 过期控制和集中化运维；本地热点缓存或单体应用可以使用 Caffeine。Spring Boot 支持通过 `spring.cache.type` 强制指定缓存实现，Redis 和 Caffeine 都可以通过 `spring.cache.*` 配置进行初始化。([Home](https://docs.spring.io/spring-boot/reference/io/caching.html?utm_source=chatgpt.com))

Redis 方案配置如下。

文件位置：`src/main/resources/application.yml`

```yaml
server:
  port: 8080

spring:
  application:
    name: spring-cache-demo

  cache:
    # 指定 Spring Cache 使用 Redis 作为缓存实现
    type: redis
    # 预声明缓存空间，便于统一管理缓存名称
    cache-names:
      - productCache
      - productListCache
      - dictCache
      - configCache
    redis:
      # 默认缓存过期时间，未单独配置的缓存空间使用该 TTL
      time-to-live: 30m
      # 保留缓存名前缀，避免不同缓存空间出现 Key 冲突
      use-key-prefix: true
      # 缓存名前缀，可根据项目名称或模块名称设置
      key-prefix: "ateng:"
      # 不缓存 null 值，避免空结果长期占用缓存
      cache-null-values: false

  data:
    redis:
      # Redis 服务地址
      host: localhost
      # Redis 服务端口
      port: 6379
      # Redis 数据库索引
      database: 0
      # Redis 连接超时时间
      timeout: 3s
      lettuce:
        pool:
          # 最大连接数
          max-active: 16
          # 最大空闲连接数
          max-idle: 8
          # 最小空闲连接数
          min-idle: 2
          # 获取连接最大等待时间
          max-wait: 3s

logging:
  level:
    io.github.atengk: info
    org.springframework.cache: info
```

Caffeine 方案配置如下。

文件位置：`src/main/resources/application-caffeine.yml`

```yaml
spring:
  cache:
    # 指定 Spring Cache 使用 Caffeine 本地缓存
    type: caffeine
    # 声明本地缓存空间
    cache-names:
      - productCache
      - productListCache
      - dictCache
      - configCache
    caffeine:
      # maximumSize 限制最大缓存数量，expireAfterWrite 表示写入后过期
      spec: maximumSize=10000,expireAfterWrite=30m

logging:
  level:
    io.github.atengk: info
    org.springframework.cache: info
```

测试环境如果需要排除缓存影响，可以禁用真实缓存实现。

文件位置：`src/main/resources/application-test.yml`

```yaml
spring:
  cache:
    # 使用 none 禁用缓存实现，便于测试业务逻辑本身
    type: none
```

配置建议如下：

| 配置项                                 | 建议                                    |
| -------------------------------------- | --------------------------------------- |
| `spring.cache.type`                    | 生产环境明确指定，不依赖自动推断        |
| `spring.cache.cache-names`             | 建议预声明核心缓存空间，避免名称散落    |
| `spring.cache.redis.time-to-live`      | 必须设置默认 TTL，不建议永久缓存        |
| `spring.cache.redis.cache-null-values` | 默认建议关闭，空值缓存需要单独设计      |
| `spring.cache.redis.use-key-prefix`    | 建议保持开启，避免不同缓存空间 Key 冲突 |
| `spring.cache.caffeine.spec`           | 必须配置最大容量，避免 JVM 内存无限增长 |

### CacheManager 配置

`CacheManager` 是 Spring Cache 的核心管理入口，负责根据缓存名称获取具体的 `Cache` 实例。Spring Boot 可以自动配置 `RedisCacheManager` 或 `CaffeineCacheManager`；如果需要设置不同缓存空间的 TTL、序列化策略、事务感知能力或本地缓存规格，可以自定义 `CacheManager` Bean。Spring Boot 官方文档也说明，Redis 场景可以通过 `RedisCacheConfiguration` Bean 或自定义 `RedisCacheManager` 完全控制缓存默认配置。([Home](https://docs.spring.io/spring-boot/reference/io/caching.html?utm_source=chatgpt.com))

Redis 自定义 `CacheManager` 示例：

文件位置：`src/main/java/io/github/atengk/config/RedisCacheManagerConfig.java`

下面的配置类用于统一配置 Redis 缓存序列化方式、默认 TTL 和不同缓存空间的独立过期时间。

```java
package io.github.atengk.config;

import cn.hutool.core.map.MapUtil;
import org.springframework.cache.CacheManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.data.redis.cache.RedisCacheConfiguration;
import org.springframework.data.redis.cache.RedisCacheManager;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.serializer.GenericJackson2JsonRedisSerializer;
import org.springframework.data.redis.serializer.RedisSerializationContext;
import org.springframework.data.redis.serializer.StringRedisSerializer;

import java.time.Duration;
import java.util.Map;

/**
 * Redis CacheManager 配置
 *
 * @author Ateng
 * @since 2026-05-06
 */
@Configuration
public class RedisCacheManagerConfig {

    /**
     * 配置 RedisCacheManager
     *
     * @param redisConnectionFactory Redis 连接工厂
     * @return CacheManager
     */
    @Bean
    @Primary
    public CacheManager redisCacheManager(RedisConnectionFactory redisConnectionFactory) {
        RedisCacheConfiguration defaultConfiguration = this.defaultRedisCacheConfiguration(Duration.ofMinutes(30));

        Map<String, RedisCacheConfiguration> cacheConfigurationMap = MapUtil.newHashMap();
        cacheConfigurationMap.put("productCache", this.defaultRedisCacheConfiguration(Duration.ofMinutes(20)));
        cacheConfigurationMap.put("productListCache", this.defaultRedisCacheConfiguration(Duration.ofMinutes(5)));
        cacheConfigurationMap.put("dictCache", this.defaultRedisCacheConfiguration(Duration.ofHours(6)));
        cacheConfigurationMap.put("configCache", this.defaultRedisCacheConfiguration(Duration.ofHours(1)));

        return RedisCacheManager.builder(redisConnectionFactory)
                // 默认缓存配置
                .cacheDefaults(defaultConfiguration)
                // 不同缓存空间独立 TTL
                .withInitialCacheConfigurations(cacheConfigurationMap)
                // 事务提交后再执行缓存写入或删除，降低事务回滚导致的缓存脏数据风险
                .transactionAware()
                .build();
    }

    /**
     * 创建 Redis 缓存配置
     *
     * @param ttl 缓存过期时间
     * @return RedisCacheConfiguration
     */
    private RedisCacheConfiguration defaultRedisCacheConfiguration(Duration ttl) {
        return RedisCacheConfiguration.defaultCacheConfig()
                // 设置缓存过期时间
                .entryTtl(ttl)
                // 禁止缓存 null 值
                .disableCachingNullValues()
                // Redis Key 使用字符串序列化，便于排查
                .serializeKeysWith(RedisSerializationContext.SerializationPair.fromSerializer(new StringRedisSerializer()))
                // Redis Value 使用 JSON 序列化，便于跨版本和跨语言排查
                .serializeValuesWith(RedisSerializationContext.SerializationPair.fromSerializer(new GenericJackson2JsonRedisSerializer()));
    }

}
```

Caffeine 自定义 `CacheManager` 示例：

文件位置：`src/main/java/io/github/atengk/config/CaffeineCacheManagerConfig.java`

下面的配置类用于配置 Caffeine 本地缓存的缓存空间、最大容量和写入后过期时间。

```java
package io.github.atengk.config;

import cn.hutool.core.collection.CollUtil;
import com.github.benmanes.caffeine.cache.Caffeine;
import org.springframework.cache.CacheManager;
import org.springframework.cache.caffeine.CaffeineCacheManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Duration;

/**
 * Caffeine CacheManager 配置
 *
 * @author Ateng
 * @since 2026-05-06
 */
@Configuration
public class CaffeineCacheManagerConfig {

    /**
     * 配置 CaffeineCacheManager
     *
     * @return CacheManager
     */
    @Bean
    public CacheManager caffeineCacheManager() {
        CaffeineCacheManager cacheManager = new CaffeineCacheManager();
        cacheManager.setCacheNames(CollUtil.newArrayList(
                "productCache",
                "productListCache",
                "dictCache",
                "configCache"
        ));
        cacheManager.setCaffeine(Caffeine.newBuilder()
                // 写入 30 分钟后过期
                .expireAfterWrite(Duration.ofMinutes(30))
                // 最大缓存数量，防止 JVM 内存无限增长
                .maximumSize(10000));
        return cacheManager;
    }

}
```

使用建议：

| 场景                     | 建议                                    |
| ------------------------ | --------------------------------------- |
| 只需要统一 TTL           | 可以直接使用 `application.yml` 配置     |
| 需要不同缓存空间不同 TTL | 自定义 `RedisCacheManager`              |
| 需要统一 JSON 序列化     | 自定义 `RedisCacheConfiguration`        |
| 需要本地缓存容量控制     | 自定义 `CaffeineCacheManager`           |
| 存在事务更新数据         | Redis 缓存建议开启 `transactionAware()` |

## 核心注解使用

Spring Cache 提供一组声明式注解来完成缓存读写操作。`@Cacheable` 用于查询缓存，`@CachePut` 用于执行方法并更新缓存，`@CacheEvict` 用于删除缓存，`@Caching` 用于组合多个缓存操作。Spring Framework 官方文档明确说明了这几类注解的职责差异。([Home](https://docs.spring.io/spring-framework/reference/integration/cache/annotations.html?utm_source=chatgpt.com))

本章节示例以商品数据为例，缓存空间约定如下：

| 缓存空间           | 用途         |
| ------------------ | ------------ |
| `productCache`     | 商品详情缓存 |
| `productListCache` | 商品列表缓存 |
| `dictCache`        | 字典数据缓存 |
| `configCache`      | 系统配置缓存 |

示例 DTO 和 VO 如下。

文件位置：`src/main/java/io/github/atengk/model/dto/ProductUpdateDTO.java`

下面的 DTO 用于接收商品更新参数。

```java
package io.github.atengk.model.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;

/**
 * 商品更新参数
 *
 * @author Ateng
 * @since 2026-05-06
 */
public record ProductUpdateDTO(

        /**
         * 商品 ID
         */
        @NotNull(message = "商品ID不能为空")
        Long id,

        /**
         * 商品名称
         */
        @NotBlank(message = "商品名称不能为空")
        String name,

        /**
         * 商品价格
         */
        @NotNull(message = "商品价格不能为空")
        BigDecimal price
) {
}
```

文件位置：`src/main/java/io/github/atengk/model/vo/ProductVO.java`

下面的 VO 用于返回商品详情数据。

```java
package io.github.atengk.model.vo;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 商品展示对象
 *
 * @author Ateng
 * @since 2026-05-06
 */
public record ProductVO(

        /**
         * 商品 ID
         */
        Long id,

        /**
         * 商品名称
         */
        String name,

        /**
         * 商品价格
         */
        BigDecimal price,

        /**
         * 更新时间
         */
        LocalDateTime updateTime
) {
}
```

### @Cacheable 查询缓存

`@Cacheable` 用于查询缓存。方法执行前，Spring 会先根据缓存名称和 Key 查询缓存；如果命中缓存，则直接返回缓存值，不再执行目标方法；如果未命中缓存，则执行目标方法，并将返回结果写入缓存。官方文档对 `@Cacheable` 的描述也是“方法结果可被缓存，相同参数后续调用可直接返回缓存结果”。([Home](https://docs.spring.io/spring-framework/reference/integration/cache/annotations.html?utm_source=chatgpt.com))

适用场景：

| 场景         | 说明                                   |
| ------------ | -------------------------------------- |
| 详情查询     | 根据 ID 查询商品、用户、机构、文章详情 |
| 字典查询     | 查询业务字典、状态枚举、地区树         |
| 配置查询     | 查询系统参数、开关配置                 |
| 远程结果缓存 | 缓存第三方接口或内部 RPC 查询结果      |

文件位置：`src/main/java/io/github/atengk/service/ProductCacheService.java`

下面的 Service 示例演示 `@Cacheable` 查询缓存、`@CachePut` 更新缓存、`@CacheEvict` 删除缓存和 `@Caching` 组合缓存操作。

```java
package io.github.atengk.service;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.ObjectUtil;
import cn.hutool.core.util.StrUtil;
import io.github.atengk.model.dto.ProductUpdateDTO;
import io.github.atengk.model.vo.ProductVO;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.CachePut;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.cache.annotation.Caching;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

/**
 * 商品缓存业务服务
 *
 * @author Ateng
 * @since 2026-05-06
 */
@Slf4j
@Service
public class ProductCacheService {

    private final ConcurrentMap<Long, ProductVO> productStorage = new ConcurrentHashMap<>();

    /**
     * 初始化模拟数据
     */
    @PostConstruct
    public void initData() {
        productStorage.put(1L, new ProductVO(1L, "机械键盘", new BigDecimal("299.00"), LocalDateTime.now()));
        productStorage.put(2L, new ProductVO(2L, "无线鼠标", new BigDecimal("129.00"), LocalDateTime.now()));
        productStorage.put(3L, new ProductVO(3L, "显示器", new BigDecimal("999.00"), LocalDateTime.now()));
        log.info("商品模拟数据初始化完成，数量：{}", productStorage.size());
    }

    /**
     * 查询商品详情
     *
     * @param id 商品 ID
     * @return 商品详情
     */
    @Cacheable(
            cacheNames = "productCache",
            key = "'detail:' + #id",
            unless = "#result == null"
    )
    public ProductVO getDetail(Long id) {
        log.info("未命中商品详情缓存，查询数据源，商品ID：{}", id);
        return productStorage.get(id);
    }

    /**
     * 根据关键字查询商品列表
     *
     * @param keyword 商品关键字
     * @return 商品列表
     */
    @Cacheable(
            cacheNames = "productListCache",
            key = "'keyword:' + #keyword",
            condition = "#keyword != null && #keyword.length() >= 2",
            unless = "#result == null || #result.isEmpty()"
    )
    public List<ProductVO> listByKeyword(String keyword) {
        log.info("未命中商品列表缓存，查询数据源，关键字：{}", keyword);
        return productStorage.values()
                .stream()
                .filter(item -> StrUtil.containsIgnoreCase(item.name(), keyword))
                .toList();
    }

    /**
     * 更新商品并刷新详情缓存
     *
     * @param dto 商品更新参数
     * @return 更新后的商品详情
     */
    @CachePut(
            cacheNames = "productCache",
            key = "'detail:' + #dto.id"
    )
    public ProductVO update(ProductUpdateDTO dto) {
        ProductVO oldProduct = productStorage.get(dto.id());
        if (ObjectUtil.isNull(oldProduct)) {
            log.warn("商品更新失败，商品不存在，商品ID：{}", dto.id());
            return null;
        }

        ProductVO newProduct = new ProductVO(dto.id(), dto.name(), dto.price(), LocalDateTime.now());
        productStorage.put(dto.id(), newProduct);
        log.info("商品更新成功，并刷新商品详情缓存，商品ID：{}", dto.id());
        return newProduct;
    }

    /**
     * 删除商品并清理详情缓存
     *
     * @param id 商品 ID
     */
    @CacheEvict(
            cacheNames = "productCache",
            key = "'detail:' + #id"
    )
    public void delete(Long id) {
        productStorage.remove(id);
        log.info("商品删除成功，并清理商品详情缓存，商品ID：{}", id);
    }

    /**
     * 更新商品并清理关联缓存
     *
     * @param dto 商品更新参数
     * @return 更新后的商品详情
     */
    @Caching(
            put = {
                    @CachePut(cacheNames = "productCache", key = "'detail:' + #dto.id")
            },
            evict = {
                    @CacheEvict(cacheNames = "productListCache", allEntries = true)
            }
    )
    public ProductVO updateAndClearList(ProductUpdateDTO dto) {
        ProductVO oldProduct = productStorage.get(dto.id());
        if (ObjectUtil.isNull(oldProduct)) {
            log.warn("商品组合缓存更新失败，商品不存在，商品ID：{}", dto.id());
            return null;
        }

        ProductVO newProduct = new ProductVO(dto.id(), dto.name(), dto.price(), LocalDateTime.now());
        productStorage.put(dto.id(), newProduct);
        log.info("商品更新成功，刷新详情缓存并清理商品列表缓存，商品ID：{}", dto.id());
        return newProduct;
    }

    /**
     * 查询所有商品
     *
     * @return 商品列表
     */
    public List<ProductVO> listAll() {
        return CollUtil.newArrayList(productStorage.values());
    }

}
```

`@Cacheable` 常用属性如下：

| 属性                   | 说明                             | 示例                          |
| ---------------------- | -------------------------------- | ----------------------------- |
| `cacheNames` / `value` | 缓存空间名称                     | `cacheNames = "productCache"` |
| `key`                  | 缓存 Key，支持 SpEL              | `key = "'detail:' + #id"`     |
| `condition`            | 满足条件才缓存，方法执行前判断   | `condition = "#id != null"`   |
| `unless`               | 满足条件则不缓存，方法执行后判断 | `unless = "#result == null"`  |
| `sync`                 | 同一个 Key 并发加载时同步执行    | `sync = true`                 |

使用建议：

1. 查询详情时使用 `@Cacheable`。
2. Key 建议包含业务语义，例如 `detail:1`、`keyword:手机`。
3. 不建议缓存 `null`，除非已经设计好缓存穿透策略。
4. `condition` 适合判断入参，`unless` 适合判断返回值。
5. 不建议将分页大结果集长期缓存，容易造成内存膨胀。

### @CachePut 更新缓存

`@CachePut` 用于强制执行目标方法，并将方法返回值写入缓存。它不会像 `@Cacheable` 一样因为缓存命中而跳过方法执行，因此适合“数据已经更新成功，需要刷新对应缓存”的场景。官方文档也明确说明，`@CachePut` 会始终执行方法，并把结果放入缓存；同时不建议将 `@CachePut` 和 `@Cacheable` 混在同一个方法上，因为两者一个可能跳过执行，一个强制执行，行为容易冲突。([Home](https://docs.spring.io/spring-framework/reference/integration/cache/annotations.html?utm_source=chatgpt.com))

适用场景：

| 场景         | 说明                           |
| ------------ | ------------------------------ |
| 修改详情     | 更新商品后刷新商品详情缓存     |
| 保存配置     | 修改系统配置后刷新配置缓存     |
| 更新用户资料 | 修改用户资料后刷新用户详情缓存 |

典型写法如下：

```java
@CachePut(
        cacheNames = "productCache",
        key = "'detail:' + #dto.id"
)
public ProductVO update(ProductUpdateDTO dto) {
    // 执行业务更新逻辑
    return updatedProduct;
}
```

使用建议：

1. `@CachePut` 方法必须返回需要写入缓存的数据。
2. 如果更新方法返回 `void`，不适合使用 `@CachePut`，应改用 `@CacheEvict` 删除缓存。
3. 数据库更新失败时不要刷新缓存。
4. 不建议与 `@Cacheable` 放在同一个方法上。
5. 对于复杂更新场景，优先“更新数据库后删除缓存”，而不是强行同步多个缓存副本。

### @CacheEvict 删除缓存

`@CacheEvict` 用于删除缓存，常见于新增、修改、删除数据后清理旧缓存。它既可以删除指定 Key，也可以通过 `allEntries = true` 清空整个缓存空间。官方文档说明，`@CacheEvict` 用于触发缓存驱逐，并支持 `allEntries` 执行缓存级别清理。([Home](https://docs.spring.io/spring-framework/reference/integration/cache/annotations.html?utm_source=chatgpt.com))

删除指定商品详情缓存：

```java
@CacheEvict(
        cacheNames = "productCache",
        key = "'detail:' + #id"
)
public void delete(Long id) {
    // 删除数据库中的商品数据
}
```

清空商品列表缓存：

```java
@CacheEvict(
        cacheNames = "productListCache",
        allEntries = true
)
public void refreshProductListCache() {
    // 执行会影响列表结果的业务操作
}
```

`@CacheEvict` 常用属性如下：

| 属性               | 说明                     | 示例                          |
| ------------------ | ------------------------ | ----------------------------- |
| `cacheNames`       | 要清理的缓存空间         | `cacheNames = "productCache"` |
| `key`              | 要清理的缓存 Key         | `key = "'detail:' + #id"`     |
| `allEntries`       | 是否清空整个缓存空间     | `allEntries = true`           |
| `beforeInvocation` | 是否在方法执行前删除缓存 | `beforeInvocation = true`     |
| `condition`        | 满足条件才删除缓存       | `condition = "#id != null"`   |

使用建议：

1. 删除详情数据时，清理对应详情缓存。
2. 修改会影响列表查询结果的数据时，清理列表缓存。
3. `allEntries = true` 要谨慎使用，适合字典、配置、列表等小范围缓存。
4. 默认情况下，缓存清理发生在方法成功执行后。
5. 如果希望无论业务方法是否异常都先清理缓存，可以使用 `beforeInvocation = true`，但需要评估数据一致性风险。

### @Caching 组合操作

`@Caching` 用于在一个方法上组合多个缓存操作，可以同时使用多个 `@Cacheable`、`@CachePut` 和 `@CacheEvict`。官方文档说明，当一个方法需要多个相同或不同类型的缓存操作时，可以使用 `@Caching` 进行组合。([Home](https://docs.spring.io/spring-framework/reference/integration/cache/annotations.html?utm_source=chatgpt.com))

典型场景是：更新商品详情后，刷新商品详情缓存，同时清空商品列表缓存。

```java
@Caching(
        put = {
                @CachePut(cacheNames = "productCache", key = "'detail:' + #dto.id")
        },
        evict = {
                @CacheEvict(cacheNames = "productListCache", allEntries = true)
        }
)
public ProductVO updateAndClearList(ProductUpdateDTO dto) {
    // 更新数据库，并返回更新后的商品详情
    return updatedProduct;
}
```

也可以同时删除多个缓存空间：

```java
@Caching(
        evict = {
                @CacheEvict(cacheNames = "productCache", key = "'detail:' + #id"),
                @CacheEvict(cacheNames = "productListCache", allEntries = true),
                @CacheEvict(cacheNames = "configCache", key = "'product:recommend'")
        }
)
public void deleteAndClearRelatedCache(Long id) {
    // 删除商品，并清理相关缓存
}
```

使用建议：

1. 一个业务方法影响多个缓存空间时，使用 `@Caching`。
2. 更新详情缓存和清理列表缓存可以组合处理。
3. 多个缓存操作的 Key 必须保持清晰，不要依赖默认 Key。
4. 组合操作不要过度复杂，缓存关系复杂时建议拆分为专门的缓存清理服务。
5. 方法内部抛出异常时，默认不会执行方法完成后的缓存写入或删除操作。

核心注解选择建议如下：

| 操作目标                         | 推荐注解       |
| -------------------------------- | -------------- |
| 查询时优先读缓存                 | `@Cacheable`   |
| 方法必须执行，并用返回值刷新缓存 | `@CachePut`    |
| 数据变更后删除缓存               | `@CacheEvict`  |
| 一个方法同时处理多个缓存         | `@Caching`     |
| 类级别统一缓存名称               | `@CacheConfig` |

整体建议是：查询方法使用 `@Cacheable`，更新方法优先使用 `@CacheEvict` 删除旧缓存；只有在返回值就是最新缓存值、且缓存关系简单时，才使用 `@CachePut` 直接刷新缓存。



## 缓存 Key 设计

缓存 Key 是 Spring Cache 落地时最容易失控的部分。Key 设计不规范会导致缓存命中率低、缓存覆盖、缓存膨胀和排查困难。Spring Cache 默认会根据方法参数生成 Key，也支持通过 SpEL 表达式和自定义 `KeyGenerator` 控制 Key 生成逻辑；`@Cacheable` 的 `key` 属性用于动态计算 Key，`keyGenerator` 属性用于指定自定义 Key 生成器 Bean。([Home](https://docs.spring.io/spring-framework/docs/current/javadoc-api/org/springframework/cache/annotation/Cacheable.html?utm_source=chatgpt.com))

### 默认 Key 生成规则

当没有显式指定 `key` 或 `keyGenerator` 时，Spring Cache 使用默认 `KeyGenerator` 生成缓存 Key。默认规则是：无参数时返回 `SimpleKey.EMPTY`，单个参数时直接使用该参数，多个参数时使用包含所有参数的 `SimpleKey`。([Home](https://docs.spring.io/spring-framework/reference/integration/cache/annotations.html?utm_source=chatgpt.com))

默认规则如下：

| 方法参数 | 默认 Key          | 示例                                                    |
| -------- | ----------------- | ------------------------------------------------------- |
| 无参数   | `SimpleKey.EMPTY` | `listAll()`                                             |
| 单个参数 | 参数本身          | `getById(1L)` 的 Key 为 `1`                             |
| 多个参数 | `SimpleKey`       | `list("手机", 1, 10)` 的 Key 为 `SimpleKey [手机,1,10]` |

默认 Key 适合简单方法，但生产环境通常建议显式定义 Key。原因是默认 Key 缺少业务语义，不便于 Redis 中排查，也容易在方法参数复杂、对象字段变化、分页查询和多条件查询场景下产生维护问题。

不推荐的写法如下：

```java
@Cacheable(cacheNames = "productCache")
public ProductVO getDetail(Long id) {
    return productRepository.getById(id);
}
```

推荐的写法如下：

```java
@Cacheable(cacheNames = "productCache", key = "'detail:' + #id")
public ProductVO getDetail(Long id) {
    return productRepository.getById(id);
}
```

Key 设计建议如下：

| 规则         | 说明                                           | 示例                        |
| ------------ | ---------------------------------------------- | --------------------------- |
| 包含业务语义 | 便于定位缓存用途                               | `detail:1001`               |
| 包含查询维度 | 避免不同查询条件互相覆盖                       | `page:keyword:phone:1:20`   |
| 避免随机值   | 随机值会导致缓存无法命中                       | 不使用 `UUID`、当前时间戳   |
| 避免超长对象 | 大对象直接作为 Key 会增加 Redis 内存和排查成本 | 对复杂参数做摘要            |
| 区分缓存空间 | 通过 `cacheNames` 区分业务区域                 | `productCache`、`dictCache` |
| 保持稳定     | 同一业务场景 Key 规则长期稳定                  | `detail:{id}`               |

### SpEL 表达式使用

Spring Cache 注解支持通过 SpEL 表达式生成 Key、判断缓存条件和控制是否缓存结果。`@Cacheable` 的 SpEL 上下文支持访问方法、目标对象、缓存集合和方法参数，例如 `#root.method`、`#root.target`、`#root.caches`、`#root.methodName`、`#root.targetClass`、`#root.args[0]`、`#p0`、`#a0` 等。([Home](https://docs.spring.io/spring-framework/docs/current/javadoc-api/org/springframework/cache/annotation/Cacheable.html?utm_source=chatgpt.com))

常用 SpEL 表达式如下：

| 表达式                 | 说明                                                 |
| ---------------------- | ---------------------------------------------------- |
| `#id`                  | 通过参数名访问方法参数                               |
| `#p0` / `#a0`          | 访问第一个方法参数                                   |
| `#root.methodName`     | 当前方法名                                           |
| `#root.targetClass`    | 当前目标类                                           |
| `#root.args[0]`        | 参数数组中的第一个参数                               |
| `#result`              | 方法返回结果，常用于 `unless` 或方法执行后的缓存判断 |
| `T(类全名).静态方法()` | 调用静态方法处理 Key                                 |

常见写法如下：

```java
@Cacheable(
        cacheNames = "productCache",
        key = "'detail:' + #id",
        condition = "#id != null",
        unless = "#result == null"
)
public ProductVO getDetail(Long id) {
    return productRepository.getById(id);
}
```

分页查询缓存可以把查询条件、页码和页大小都纳入 Key：

```java
@Cacheable(
        cacheNames = "productListCache",
        key = "'page:' + T(cn.hutool.core.util.StrUtil).blankToDefault(#keyword, 'all') + ':' + #page + ':' + #size",
        condition = "#page != null && #page > 0 && #size != null && #size > 0",
        unless = "#result == null || #result.isEmpty()"
)
public List<ProductVO> list(String keyword, Integer page, Integer size) {
    return productRepository.list(keyword, page, size);
}
```

SpEL 使用建议如下：

| 场景             | 推荐写法                                                 |
| ---------------- | -------------------------------------------------------- |
| 单 ID 查询       | `key = "'detail:' + #id"`                                |
| 用户维度缓存     | `key = "'user:' + #userId"`                              |
| 分页缓存         | `key = "'page:' + #keyword + ':' + #page + ':' + #size"` |
| 参数可能为空     | 使用 `condition` 或 `T(StrUtil).blankToDefault()`        |
| 返回值为空不缓存 | `unless = "#result == null"`                             |
| 列表为空不缓存   | `unless = "#result == null                               |

注意：`key` 和 `keyGenerator` 用于同一目的，不建议在同一个缓存注解中同时使用。复杂 Key 规则应优先封装到自定义 `KeyGenerator` 中，避免大量 SpEL 表达式散落在业务代码里。

### 自定义 KeyGenerator

当 Key 规则需要在多个业务模块复用，或者方法参数是复杂对象时，可以自定义 `KeyGenerator`。自定义生成器适合统一增加类名、方法名、参数摘要、租户标识、系统前缀等信息。Spring Cache 的 `keyGenerator` 属性用于指定自定义 `KeyGenerator` Bean 名称。([Home](https://docs.spring.io/spring-framework/docs/current/javadoc-api/org/springframework/cache/annotation/Cacheable.html?utm_source=chatgpt.com))

文件位置：`src/main/java/io/github/atengk/common/cache/BusinessCacheKeyGenerator.java`

下面的 Key 生成器会使用类名、方法名和参数 MD5 摘要生成稳定 Key，避免复杂参数直接进入 Redis Key。

```java
package io.github.atengk.common.cache;

import cn.hutool.core.util.ArrayUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.crypto.SecureUtil;
import cn.hutool.json.JSONUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.interceptor.KeyGenerator;
import org.springframework.stereotype.Component;

import java.lang.reflect.Method;
import java.util.Arrays;

/**
 * 业务缓存 Key 生成器
 *
 * @author Ateng
 * @since 2026-05-06
 */
@Slf4j
@Component("businessKeyGenerator")
public class BusinessCacheKeyGenerator implements KeyGenerator {

    /**
     * 生成缓存 Key
     *
     * @param target 目标对象
     * @param method 目标方法
     * @param params 方法参数
     * @return 缓存 Key
     */
    @Override
    public Object generate(Object target, Method method, Object... params) {
        String className = target.getClass().getSimpleName();
        String methodName = method.getName();

        if (ArrayUtil.isEmpty(params)) {
            String key = StrUtil.format("{}:{}:empty", className, methodName);
            log.debug("生成无参缓存Key：{}", key);
            return key;
        }

        String paramJson = JSONUtil.toJsonStr(Arrays.asList(params));
        String paramDigest = SecureUtil.md5(paramJson);
        String key = StrUtil.format("{}:{}:{}", className, methodName, paramDigest);

        log.debug("生成业务缓存Key，方法：{}.{}, Key：{}", className, methodName, key);
        return key;
    }

}
```

使用自定义 Key 生成器：

```java
@Cacheable(
        cacheNames = "productListCache",
        keyGenerator = "businessKeyGenerator",
        condition = "#page != null && #page > 0 && #size != null && #size > 0",
        unless = "#result == null || #result.isEmpty()"
)
public List<ProductVO> list(String keyword, Integer page, Integer size) {
    return productRepository.list(keyword, page, size);
}
```

自定义 KeyGenerator 使用建议：

| 场景                   | 建议                                  |
| ---------------------- | ------------------------------------- |
| 简单 ID 查询           | 直接使用 SpEL                         |
| 多参数查询             | 可使用 SpEL，也可使用 KeyGenerator    |
| 复杂对象参数           | 推荐使用 KeyGenerator                 |
| 多模块统一 Key 规则    | 推荐使用 KeyGenerator                 |
| 需要人工排查 Redis Key | Key 中保留业务语义，参数部分可摘要    |
| 需要租户隔离           | KeyGenerator 中加入租户 ID 或系统编码 |

## 业务代码开发

本章节给出一个可直接运行的 Spring Boot 3 缓存业务示例。示例使用内存 `ConcurrentHashMap` 模拟数据源，实际项目中可以替换为 MyBatis-Plus Mapper、JPA Repository 或远程服务调用。缓存接入位置建议放在 Service 层，因为 Service 层更接近业务查询边界，Key 规则更稳定，也更容易统一处理数据更新后的缓存清理。

### Service 层缓存接入

Service 层缓存接入的核心原则是：查询方法使用 `@Cacheable`，更新方法使用 `@CachePut` 或 `@CacheEvict`，删除方法使用 `@CacheEvict`，影响列表结果的操作需要清理列表缓存。Spring Framework 官方文档中，`@Cacheable` 用于缓存方法结果，`@CachePut` 会始终执行方法并更新缓存，`@CacheEvict` 用于缓存驱逐，`@Caching` 用于组合多个缓存操作。([Home](https://docs.spring.io/spring-framework/reference/integration/cache/annotations.html?utm_source=chatgpt.com))

本示例文件结构如下：

```text
src/main/java/io/github/atengk
├── common/cache/CacheNames.java
├── product/controller/ProductController.java
├── product/model/dto/ProductCreateRequest.java
├── product/model/dto/ProductUpdateRequest.java
├── product/model/vo/ProductVO.java
├── product/service/ProductService.java
└── product/service/impl/ProductServiceImpl.java
```

文件位置：`src/main/java/io/github/atengk/common/cache/CacheNames.java`

下面的常量类用于统一维护缓存空间名称，避免缓存名称散落在业务代码中。

```java
package io.github.atengk.common.cache;

/**
 * 缓存名称常量
 *
 * @author Ateng
 * @since 2026-05-06
 */
public final class CacheNames {

    /**
     * 商品详情缓存
     */
    public static final String PRODUCT = "productCache";

    /**
     * 商品列表缓存
     */
    public static final String PRODUCT_LIST = "productListCache";

    private CacheNames() {
        throw new IllegalStateException("缓存常量类不能实例化");
    }

}
```

文件位置：`src/main/java/io/github/atengk/product/model/dto/ProductCreateRequest.java`

下面的 DTO 用于接收新增商品请求参数。

```java
package io.github.atengk.product.model.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;

/**
 * 商品新增请求参数
 *
 * @author Ateng
 * @since 2026-05-06
 */
public record ProductCreateRequest(

        /**
         * 商品名称
         */
        @NotBlank(message = "商品名称不能为空")
        String name,

        /**
         * 商品价格
         */
        @NotNull(message = "商品价格不能为空")
        @DecimalMin(value = "0.01", message = "商品价格必须大于0")
        BigDecimal price
) {
}
```

文件位置：`src/main/java/io/github/atengk/product/model/dto/ProductUpdateRequest.java`

下面的 DTO 用于接收修改商品请求参数。

```java
package io.github.atengk.product.model.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;

/**
 * 商品修改请求参数
 *
 * @author Ateng
 * @since 2026-05-06
 */
public record ProductUpdateRequest(

        /**
         * 商品名称
         */
        @NotBlank(message = "商品名称不能为空")
        String name,

        /**
         * 商品价格
         */
        @NotNull(message = "商品价格不能为空")
        @DecimalMin(value = "0.01", message = "商品价格必须大于0")
        BigDecimal price
) {
}
```

文件位置：`src/main/java/io/github/atengk/product/model/vo/ProductVO.java`

下面的 VO 用于返回商品数据。

```java
package io.github.atengk.product.model.vo;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 商品展示对象
 *
 * @author Ateng
 * @since 2026-05-06
 */
public record ProductVO(

        /**
         * 商品 ID
         */
        Long id,

        /**
         * 商品名称
         */
        String name,

        /**
         * 商品价格
         */
        BigDecimal price,

        /**
         * 更新时间
         */
        LocalDateTime updateTime
) {
}
```

文件位置：`src/main/java/io/github/atengk/product/service/ProductService.java`

下面的 Service 接口定义商品查询、新增、修改和删除能力。

```java
package io.github.atengk.product.service;

import io.github.atengk.product.model.dto.ProductCreateRequest;
import io.github.atengk.product.model.dto.ProductUpdateRequest;
import io.github.atengk.product.model.vo.ProductVO;

import java.util.List;

/**
 * 商品业务服务
 *
 * @author Ateng
 * @since 2026-05-06
 */
public interface ProductService {

    /**
     * 查询商品详情
     *
     * @param id 商品 ID
     * @return 商品详情
     */
    ProductVO getDetail(Long id);

    /**
     * 查询商品列表
     *
     * @param keyword 商品关键字
     * @param page 页码
     * @param size 每页数量
     * @return 商品列表
     */
    List<ProductVO> list(String keyword, Integer page, Integer size);

    /**
     * 新增商品
     *
     * @param request 新增请求
     * @return 商品详情
     */
    ProductVO create(ProductCreateRequest request);

    /**
     * 修改商品
     *
     * @param id 商品 ID
     * @param request 修改请求
     * @return 商品详情
     */
    ProductVO update(Long id, ProductUpdateRequest request);

    /**
     * 删除商品
     *
     * @param id 商品 ID
     */
    void delete(Long id);

}
```

文件位置：`src/main/java/io/github/atengk/product/service/impl/ProductServiceImpl.java`

下面的 Service 实现类在查询、更新和删除方法上接入 Spring Cache 注解，并使用 Hutool 处理参数和对象判断。

```java
package io.github.atengk.product.service.impl;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.ObjectUtil;
import cn.hutool.core.util.StrUtil;
import io.github.atengk.common.cache.CacheNames;
import io.github.atengk.product.model.dto.ProductCreateRequest;
import io.github.atengk.product.model.dto.ProductUpdateRequest;
import io.github.atengk.product.model.vo.ProductVO;
import io.github.atengk.product.service.ProductService;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.CachePut;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.cache.annotation.Caching;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.atomic.AtomicLong;

/**
 * 商品业务服务实现
 *
 * @author Ateng
 * @since 2026-05-06
 */
@Slf4j
@Service
public class ProductServiceImpl implements ProductService {

    private final AtomicLong idGenerator = new AtomicLong(100L);

    private final ConcurrentMap<Long, ProductVO> productStorage = new ConcurrentHashMap<>();

    /**
     * 初始化模拟商品数据
     */
    @PostConstruct
    public void initData() {
        productStorage.put(1L, new ProductVO(1L, "机械键盘", new BigDecimal("299.00"), LocalDateTime.now()));
        productStorage.put(2L, new ProductVO(2L, "无线鼠标", new BigDecimal("129.00"), LocalDateTime.now()));
        productStorage.put(3L, new ProductVO(3L, "显示器", new BigDecimal("999.00"), LocalDateTime.now()));
        log.info("商品模拟数据初始化完成，数量：{}", productStorage.size());
    }

    /**
     * 查询商品详情
     *
     * @param id 商品 ID
     * @return 商品详情
     */
    @Override
    @Cacheable(
            cacheNames = CacheNames.PRODUCT,
            key = "'detail:' + #id",
            condition = "#id != null",
            unless = "#result == null"
    )
    public ProductVO getDetail(Long id) {
        log.info("未命中商品详情缓存，查询数据源，商品ID：{}", id);
        return productStorage.get(id);
    }

    /**
     * 查询商品列表
     *
     * @param keyword 商品关键字
     * @param page 页码
     * @param size 每页数量
     * @return 商品列表
     */
    @Override
    @Cacheable(
            cacheNames = CacheNames.PRODUCT_LIST,
            keyGenerator = "businessKeyGenerator",
            condition = "#page != null && #page > 0 && #size != null && #size > 0",
            unless = "#result == null || #result.isEmpty()"
    )
    public List<ProductVO> list(String keyword, Integer page, Integer size) {
        String safeKeyword = StrUtil.trimToEmpty(keyword);
        int safePage = Math.max(ObjectUtil.defaultIfNull(page, 1), 1);
        int safeSize = Math.min(Math.max(ObjectUtil.defaultIfNull(size, 10), 1), 100);
        long offset = (long) (safePage - 1) * safeSize;

        log.info("未命中商品列表缓存，查询数据源，关键字：{}，页码：{}，每页数量：{}", safeKeyword, safePage, safeSize);

        return productStorage.values()
                .stream()
                .filter(product -> StrUtil.isBlank(safeKeyword) || StrUtil.containsIgnoreCase(product.name(), safeKeyword))
                .sorted(Comparator.comparing(ProductVO::id))
                .skip(offset)
                .limit(safeSize)
                .toList();
    }

    /**
     * 新增商品
     *
     * @param request 新增请求
     * @return 商品详情
     */
    @Override
    @CacheEvict(
            cacheNames = CacheNames.PRODUCT_LIST,
            allEntries = true
    )
    public ProductVO create(ProductCreateRequest request) {
        Long id = idGenerator.incrementAndGet();
        ProductVO product = new ProductVO(id, request.name(), request.price(), LocalDateTime.now());
        productStorage.put(id, product);

        log.info("商品新增成功，清理商品列表缓存，商品ID：{}", id);
        return product;
    }

    /**
     * 修改商品
     *
     * @param id 商品 ID
     * @param request 修改请求
     * @return 商品详情
     */
    @Override
    @Caching(
            put = {
                    @CachePut(
                            cacheNames = CacheNames.PRODUCT,
                            key = "'detail:' + #id",
                            unless = "#result == null"
                    )
            },
            evict = {
                    @CacheEvict(
                            cacheNames = CacheNames.PRODUCT_LIST,
                            allEntries = true
                    )
            }
    )
    public ProductVO update(Long id, ProductUpdateRequest request) {
        ProductVO oldProduct = productStorage.get(id);
        if (ObjectUtil.isNull(oldProduct)) {
            log.warn("商品修改失败，商品不存在，商品ID：{}", id);
            return null;
        }

        ProductVO newProduct = new ProductVO(id, request.name(), request.price(), LocalDateTime.now());
        productStorage.put(id, newProduct);

        log.info("商品修改成功，刷新商品详情缓存并清理商品列表缓存，商品ID：{}", id);
        return newProduct;
    }

    /**
     * 删除商品
     *
     * @param id 商品 ID
     */
    @Override
    @Caching(
            evict = {
                    @CacheEvict(cacheNames = CacheNames.PRODUCT, key = "'detail:' + #id"),
                    @CacheEvict(cacheNames = CacheNames.PRODUCT_LIST, allEntries = true)
            }
    )
    public void delete(Long id) {
        ProductVO removedProduct = productStorage.remove(id);
        if (ObjectUtil.isNull(removedProduct)) {
            log.warn("商品删除跳过，商品不存在，商品ID：{}", id);
            return;
        }

        log.info("商品删除成功，清理商品详情缓存和商品列表缓存，商品ID：{}", id);
    }

    /**
     * 查询全部商品，仅用于内部调试
     *
     * @return 商品列表
     */
    public List<ProductVO> listAll() {
        return CollUtil.newArrayList(productStorage.values());
    }

}
```

Service 层缓存接入说明：

| 方法        | 缓存处理                         | 说明                                 |
| ----------- | -------------------------------- | ------------------------------------ |
| `getDetail` | `@Cacheable`                     | 商品详情查询，命中缓存后不执行方法   |
| `list`      | `@Cacheable` + `keyGenerator`    | 商品列表查询，使用自定义 Key 生成器  |
| `create`    | `@CacheEvict(allEntries = true)` | 新增商品会影响列表结果，清理列表缓存 |
| `update`    | `@CachePut` + `@CacheEvict`      | 刷新详情缓存，同时清理列表缓存       |
| `delete`    | `@Caching` + 多个 `@CacheEvict`  | 删除详情缓存，并清理列表缓存         |

### 查询接口缓存示例

查询接口用于验证 `@Cacheable` 是否生效。第一次请求会执行 Service 方法并写入缓存，第二次使用相同参数请求时应直接命中缓存，不再打印“未命中缓存”的业务日志。

文件位置：`src/main/java/io/github/atengk/product/controller/ProductController.java`

下面的 Controller 提供商品详情查询、列表查询、新增、修改和删除接口。

```java
package io.github.atengk.product.controller;

import io.github.atengk.product.model.dto.ProductCreateRequest;
import io.github.atengk.product.model.dto.ProductUpdateRequest;
import io.github.atengk.product.model.vo.ProductVO;
import io.github.atengk.product.service.ProductService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Positive;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 商品接口控制器
 *
 * @author Ateng
 * @since 2026-05-06
 */
@Validated
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/products")
public class ProductController {

    private final ProductService productService;

    /**
     * 查询商品详情
     *
     * @param id 商品 ID
     * @return 商品详情
     */
    @GetMapping("/{id}")
    public ProductVO getDetail(@PathVariable @Positive(message = "商品ID必须大于0") Long id) {
        return productService.getDetail(id);
    }

    /**
     * 查询商品列表
     *
     * @param keyword 商品关键字
     * @param page 页码
     * @param size 每页数量
     * @return 商品列表
     */
    @GetMapping
    public List<ProductVO> list(@RequestParam(required = false) String keyword,
                                @RequestParam(defaultValue = "1") Integer page,
                                @RequestParam(defaultValue = "10") Integer size) {
        return productService.list(keyword, page, size);
    }

    /**
     * 新增商品
     *
     * @param request 新增请求
     * @return 商品详情
     */
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ProductVO create(@Valid @RequestBody ProductCreateRequest request) {
        return productService.create(request);
    }

    /**
     * 修改商品
     *
     * @param id 商品 ID
     * @param request 修改请求
     * @return 商品详情
     */
    @PutMapping("/{id}")
    public ProductVO update(@PathVariable @Positive(message = "商品ID必须大于0") Long id,
                            @Valid @RequestBody ProductUpdateRequest request) {
        return productService.update(id, request);
    }

    /**
     * 删除商品
     *
     * @param id 商品 ID
     */
    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable @Positive(message = "商品ID必须大于0") Long id) {
        productService.delete(id);
    }

}
```

启动项目后，调用详情查询接口：

```bash
curl -s http://localhost:8080/api/products/1
```

响应示例：

```json
{
  "id": 1,
  "name": "机械键盘",
  "price": 299.00,
  "updateTime": "2026-05-06T10:20:30"
}
```

连续调用两次相同接口：

```bash
curl -s http://localhost:8080/api/products/1
curl -s http://localhost:8080/api/products/1
```

预期现象：

1. 第一次调用打印日志：`未命中商品详情缓存，查询数据源，商品ID：1`。
2. 第二次调用不再打印该日志，说明已命中 `productCache`。
3. Redis 中可以看到类似 `productCache::detail:1` 的缓存 Key。实际 Key 前缀取决于 `spring.cache.redis.key-prefix` 或自定义 `RedisCacheManager` 配置。

查询列表接口：

```bash
curl -s "http://localhost:8080/api/products?keyword=鼠&page=1&size=10"
```

响应示例：

```json
[
  {
    "id": 2,
    "name": "无线鼠标",
    "price": 129.00,
    "updateTime": "2026-05-06T10:20:30"
  }
]
```

使用 Redis 验证缓存 Key：

```bash
redis-cli --scan --pattern "*productCache*"
redis-cli --scan --pattern "*productListCache*"
```

`--scan` 用于渐进式扫描 Key，比直接使用 `KEYS` 更适合生产环境排查。`--pattern` 用于按缓存空间名称过滤结果，实际环境应根据项目缓存前缀调整匹配规则。

### 更新与删除缓存示例

更新和删除接口用于验证缓存一致性处理。更新商品详情后，需要刷新详情缓存并清理列表缓存；删除商品后，需要删除详情缓存并清理列表缓存。`@CachePut` 会执行方法并将返回结果写入缓存，`@CacheEvict` 用于删除缓存，`@Caching` 可以组合多个缓存操作。([Home](https://docs.spring.io/spring-framework/reference/integration/cache/annotations.html?utm_source=chatgpt.com))

新增商品：

```bash
curl -s -X POST "http://localhost:8080/api/products" \
  -H "Content-Type: application/json" \
  -d '{
    "name": "蓝牙耳机",
    "price": 199.00
  }'
```

新增商品后会清理 `productListCache`，因为列表查询结果已经发生变化。新增接口没有直接写入详情缓存，主要原因是新增商品通常不一定马上被详情接口访问，直接清理列表缓存即可。

修改商品：

```bash
curl -s -X PUT "http://localhost:8080/api/products/1" \
  -H "Content-Type: application/json" \
  -d '{
    "name": "机械键盘 Pro",
    "price": 399.00
  }'
```

修改接口预期效果：

1. 执行业务更新逻辑。
2. 使用 `@CachePut` 刷新 `productCache::detail:1`。
3. 使用 `@CacheEvict(allEntries = true)` 清理 `productListCache`。
4. 再次查询 `/api/products/1` 时返回更新后的商品详情。

删除商品：

```bash
curl -i -X DELETE "http://localhost:8080/api/products/1"
```

删除接口预期效果：

1. 删除内存数据源中的商品。
2. 删除 `productCache::detail:1`。
3. 清理 `productListCache`。
4. 再次查询 `/api/products/1` 时返回空结果或由项目统一异常处理逻辑返回不存在提示。

缓存验证命令：

```bash
# 查询商品详情缓存
redis-cli --scan --pattern "*productCache*"

# 查询商品列表缓存
redis-cli --scan --pattern "*productListCache*"

# 查看指定 Key 的剩余过期时间，Key 名称按实际扫描结果替换
redis-cli TTL "productCache::detail:1"
```

`TTL` 返回值说明：

| 返回值 | 说明                                         |
| ------ | -------------------------------------------- |
| 正整数 | Key 剩余过期秒数                             |
| `-1`   | Key 存在但没有过期时间，不推荐用于业务缓存   |
| `-2`   | Key 不存在，通常表示未写入、已过期或已被删除 |

业务开发注意事项：

| 问题                 | 说明                                                         |
| -------------------- | ------------------------------------------------------------ |
| Service 内部方法互调 | 同一个 Bean 内部直接调用缓存方法可能绕过 Spring 代理，导致缓存注解不生效 |
| 更新返回 `null`      | `@CachePut` 配合 `unless = "#result == null"`，避免把空结果写入缓存 |
| 列表缓存清理         | 新增、修改、删除通常都会影响列表，需要清理列表缓存           |
| Key 不稳定           | 不要把当前时间、随机数、未规范化对象直接作为 Key             |
| 缓存和事务           | 数据库事务回滚时要避免缓存提前更新，Redis CacheManager 可结合 `transactionAware()` 使用 |
| 空值缓存             | 默认不建议缓存空值；需要处理缓存穿透时，应单独设计短 TTL 空值缓存策略 |

整体接入原则是：详情缓存精确删除或刷新，列表缓存按空间清理，复杂查询使用自定义 Key 生成器，更新链路优先保证数据库成功后再处理缓存。



## 缓存存储实现

Spring Cache 只定义缓存抽象，不限定底层存储实现。Spring Boot 会根据类路径和配置自动选择缓存提供者；Redis 可用于分布式缓存，Caffeine 可用于本地缓存，二者都可以通过 `CacheManager` 接入 Spring Cache。Spring Boot 官方文档说明，当 Redis 可用并完成配置时，会自动配置 `RedisCacheManager`；当 Caffeine 依赖存在时，会自动配置 `CaffeineCacheManager`，并支持通过 `spring.cache.cache-names` 和对应配置项创建缓存空间。([Home](https://docs.spring.io/spring-boot/reference/io/caching.html?utm_source=chatgpt.com))

### 本地缓存实现

本地缓存是指缓存数据存储在当前应用 JVM 内存中，常用实现是 Caffeine。它的优势是访问速度快、没有网络开销、配置简单；局限是多实例之间数据不共享，应用重启后缓存丢失，不适合存储需要跨节点一致访问的数据。

适用场景如下：

| 场景           | 说明                                       |
| -------------- | ------------------------------------------ |
| 单体应用       | 应用只有一个实例，缓存数据不需要跨节点共享 |
| 本地热点数据   | 少量高频访问数据，允许每个节点独立缓存     |
| 临时计算结果   | 计算成本较高，但结果生命周期较短           |
| 字典或配置副本 | 允许短时间弱一致，可以通过 TTL 自动刷新    |

文件位置：`src/main/resources/application-caffeine.yml`

下面的配置使用 Caffeine 作为 Spring Cache 的本地缓存实现。

```yaml
spring:
  cache:
    # 使用 Caffeine 作为本地缓存实现
    type: caffeine
    # 预声明缓存空间，避免缓存名称散落且不可控
    cache-names:
      - productCache
      - productListCache
      - dictCache
      - configCache
    caffeine:
      # maximumSize 限制最大缓存条目数，expireAfterWrite 表示写入后 30 分钟过期
      spec: maximumSize=10000,expireAfterWrite=30m
```

如果需要通过 Java 配置控制 Caffeine，可以定义 `CaffeineCacheManager`。

文件位置：`src/main/java/io/github/atengk/config/CaffeineCacheConfig.java`

下面的配置类用于声明本地缓存空间、容量上限和过期策略。

```java
package io.github.atengk.config;

import cn.hutool.core.collection.CollUtil;
import com.github.benmanes.caffeine.cache.Caffeine;
import org.springframework.cache.CacheManager;
import org.springframework.cache.caffeine.CaffeineCacheManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Duration;

/**
 * Caffeine 本地缓存配置
 *
 * @author Ateng
 * @since 2026-05-06
 */
@Configuration
public class CaffeineCacheConfig {

    /**
     * 配置 Caffeine 缓存管理器
     *
     * @return 缓存管理器
     */
    @Bean
    public CacheManager caffeineCacheManager() {
        CaffeineCacheManager cacheManager = new CaffeineCacheManager();
        cacheManager.setCacheNames(CollUtil.newArrayList(
                "productCache",
                "productListCache",
                "dictCache",
                "configCache"
        ));
        cacheManager.setCaffeine(Caffeine.newBuilder()
                // 写入 30 分钟后过期，适合本地热点缓存
                .expireAfterWrite(Duration.ofMinutes(30))
                // 最大缓存数量，防止 JVM 内存无限增长
                .maximumSize(10000)
                // 记录缓存命中率等统计信息，便于后续观测
                .recordStats());
        return cacheManager;
    }

}
```

本地缓存使用建议：

| 建议                      | 说明                                       |
| ------------------------- | ------------------------------------------ |
| 必须设置容量上限          | 避免 JVM 内存被缓存无限占用                |
| 必须设置过期策略          | 避免旧数据长期滞留                         |
| 不缓存大对象              | 大列表、大报表、大 JSON 会增加堆内存压力   |
| 不用于强一致数据          | 多实例之间无法自动同步                     |
| 适合配合 Redis 做热点优化 | 多级缓存需要单独设计，不建议初期直接复杂化 |

### Redis 缓存实现

Redis 缓存是生产环境中更常用的 Spring Cache 存储方案。它支持多实例共享缓存、TTL 过期、集中化管理和 Redis 命令排查，适合微服务、集群部署和热点数据缓存。Spring Boot 官方文档说明，Redis 缓存可以通过 `spring.cache.redis.*` 配置默认 TTL、缓存名称等，也可以通过自定义 `RedisCacheConfiguration` 或 `RedisCacheManager` 完全控制配置。([Home](https://docs.spring.io/spring-boot/reference/io/caching.html?utm_source=chatgpt.com))

文件位置：`src/main/resources/application-redis.yml`

下面的配置使用 Redis 作为 Spring Cache 的缓存存储。

```yaml
spring:
  cache:
    # 使用 Redis 作为分布式缓存实现
    type: redis
    # 预声明缓存空间
    cache-names:
      - productCache
      - productListCache
      - dictCache
      - configCache
      - nullValueCache
    redis:
      # 默认缓存过期时间
      time-to-live: 30m
      # 开启缓存名前缀，防止不同缓存空间 Key 冲突
      use-key-prefix: true
      # 项目前缀，便于 Redis 中区分业务系统
      key-prefix: "ateng:"
      # 默认不缓存 null，缓存穿透场景使用独立策略处理
      cache-null-values: false

  data:
    redis:
      # Redis 服务地址
      host: localhost
      # Redis 服务端口
      port: 6379
      # Redis 数据库索引
      database: 0
      # Redis 命令超时时间
      timeout: 3s
      lettuce:
        pool:
          # 最大连接数
          max-active: 16
          # 最大空闲连接数
          max-idle: 8
          # 最小空闲连接数
          min-idle: 2
          # 获取连接最大等待时间
          max-wait: 3s
```

生产环境通常需要自定义 `RedisCacheManager`，统一处理序列化、TTL 和不同缓存空间的过期策略。

文件位置：`src/main/java/io/github/atengk/config/RedisCacheConfig.java`

下面的配置类用于创建 Redis 缓存管理器，并为不同缓存空间设置不同 TTL。

```java
package io.github.atengk.config;

import cn.hutool.core.map.MapUtil;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.springframework.cache.CacheManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.data.redis.cache.RedisCacheConfiguration;
import org.springframework.data.redis.cache.RedisCacheManager;
import org.springframework.data.redis.cache.RedisCacheWriter;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.serializer.GenericJackson2JsonRedisSerializer;
import org.springframework.data.redis.serializer.RedisSerializationContext;
import org.springframework.data.redis.serializer.StringRedisSerializer;

import java.time.Duration;
import java.util.Map;

/**
 * Redis 缓存配置
 *
 * @author Ateng
 * @since 2026-05-06
 */
@Configuration
public class RedisCacheConfig {

    /**
     * 配置 Redis 缓存管理器
     *
     * @param redisConnectionFactory Redis 连接工厂
     * @return 缓存管理器
     */
    @Bean
    @Primary
    public CacheManager redisCacheManager(RedisConnectionFactory redisConnectionFactory) {
        RedisCacheConfiguration defaultConfig = this.createRedisCacheConfiguration(Duration.ofMinutes(30));

        Map<String, RedisCacheConfiguration> cacheConfigMap = MapUtil.newHashMap();
        cacheConfigMap.put("productCache", this.createRedisCacheConfiguration(Duration.ofMinutes(20)));
        cacheConfigMap.put("productListCache", this.createRedisCacheConfiguration(Duration.ofMinutes(5)));
        cacheConfigMap.put("dictCache", this.createRedisCacheConfiguration(Duration.ofHours(6)));
        cacheConfigMap.put("configCache", this.createRedisCacheConfiguration(Duration.ofHours(1)));
        cacheConfigMap.put("nullValueCache", this.createRedisCacheConfiguration(Duration.ofMinutes(2)));

        return RedisCacheManager.builder(RedisCacheWriter.nonLockingRedisCacheWriter(redisConnectionFactory))
                // 默认缓存配置
                .cacheDefaults(defaultConfig)
                // 初始化不同缓存空间的独立配置
                .withInitialCacheConfigurations(cacheConfigMap)
                // 事务提交后再执行缓存写入或删除，降低事务回滚导致的缓存脏数据风险
                .transactionAware()
                .build();
    }

    /**
     * 创建 Redis 缓存配置
     *
     * @param ttl 缓存过期时间
     * @return Redis 缓存配置
     */
    private RedisCacheConfiguration createRedisCacheConfiguration(Duration ttl) {
        ObjectMapper objectMapper = this.createCacheObjectMapper();
        GenericJackson2JsonRedisSerializer valueSerializer = new GenericJackson2JsonRedisSerializer(objectMapper);

        return RedisCacheConfiguration.defaultCacheConfig()
                // 设置缓存过期时间
                .entryTtl(ttl)
                // 禁止缓存 null，空值缓存使用业务包装对象处理
                .disableCachingNullValues()
                // Key 使用字符串序列化，便于 Redis 中直接排查
                .serializeKeysWith(RedisSerializationContext.SerializationPair.fromSerializer(new StringRedisSerializer()))
                // Value 使用 JSON 序列化，兼顾可读性和复杂对象支持
                .serializeValuesWith(RedisSerializationContext.SerializationPair.fromSerializer(valueSerializer));
    }

    /**
     * 创建缓存专用 ObjectMapper
     *
     * @return ObjectMapper
     */
    private ObjectMapper createCacheObjectMapper() {
        ObjectMapper objectMapper = new ObjectMapper();
        // 支持 LocalDateTime、LocalDate、LocalTime 等 Java 8 时间类型
        objectMapper.registerModule(new JavaTimeModule());
        // 写入类型信息，便于反序列化为原始对象类型
        objectMapper.activateDefaultTyping(
                objectMapper.getPolymorphicTypeValidator(),
                ObjectMapper.DefaultTyping.NON_FINAL,
                JsonTypeInfo.As.PROPERTY
        );
        return objectMapper;
    }

}
```

Redis 缓存使用建议：

| 建议             | 说明                                                         |
| ---------------- | ------------------------------------------------------------ |
| 开启 Key 前缀    | Spring Boot 官方也建议保留 Redis 缓存名前缀，避免不同缓存空间 Key 冲突。([Home](https://docs.spring.io/spring-boot/reference/io/caching.html?utm_source=chatgpt.com)) |
| 设置默认 TTL     | 不建议业务缓存永久有效                                       |
| 不直接缓存大对象 | 大对象会增加 Redis 内存和网络传输成本                        |
| 区分缓存空间     | 详情、列表、字典、配置应使用不同缓存空间                     |
| 保持序列化一致   | 避免升级后出现反序列化失败                                   |
| 配合监控         | 关注 Redis 内存、Key 数量、命中率和慢查询                    |

### 多缓存空间配置

多缓存空间用于按业务类型隔离缓存数据，使不同类型的数据拥有独立 TTL、Key 规则和清理策略。例如商品详情缓存可以保留 20 分钟，商品列表缓存只保留 5 分钟，字典缓存可以保留 6 小时。

推荐缓存空间规划如下：

| 缓存空间           | 用途         | 推荐 TTL | 清理策略                 |
| ------------------ | ------------ | -------- | ------------------------ |
| `productCache`     | 商品详情     | 20 分钟  | 按商品 ID 精确删除或刷新 |
| `productListCache` | 商品列表     | 5 分钟   | 新增、修改、删除后清空   |
| `dictCache`        | 字典数据     | 6 小时   | 字典变更后清空           |
| `configCache`      | 系统配置     | 1 小时   | 配置变更后精确删除       |
| `nullValueCache`   | 空值占位缓存 | 1-2 分钟 | 短 TTL 自动过期          |

文件位置：`src/main/java/io/github/atengk/common/cache/CacheNames.java`

下面的常量类用于统一维护缓存空间名称。

```java
package io.github.atengk.common.cache;

/**
 * 缓存名称常量
 *
 * @author Ateng
 * @since 2026-05-06
 */
public final class CacheNames {

    /**
     * 商品详情缓存
     */
    public static final String PRODUCT = "productCache";

    /**
     * 商品列表缓存
     */
    public static final String PRODUCT_LIST = "productListCache";

    /**
     * 字典缓存
     */
    public static final String DICT = "dictCache";

    /**
     * 系统配置缓存
     */
    public static final String CONFIG = "configCache";

    /**
     * 空值占位缓存
     */
    public static final String NULL_VALUE = "nullValueCache";

    private CacheNames() {
        throw new IllegalStateException("缓存常量类不能实例化");
    }

}
```

多缓存空间使用示例：

```java
@Cacheable(cacheNames = CacheNames.PRODUCT, key = "'detail:' + #id", unless = "#result == null")
public ProductVO getDetail(Long id) {
    return productRepository.getById(id);
}

@Cacheable(cacheNames = CacheNames.PRODUCT_LIST, keyGenerator = "businessKeyGenerator", unless = "#result == null || #result.isEmpty()")
public List<ProductVO> list(String keyword, Integer page, Integer size) {
    return productRepository.list(keyword, page, size);
}

@Cacheable(cacheNames = CacheNames.DICT, key = "'type:' + #dictType", unless = "#result == null || #result.isEmpty()")
public List<DictVO> listDict(String dictType) {
    return dictRepository.listByType(dictType);
}
```

多缓存空间配置建议：

| 问题         | 建议                                              |
| ------------ | ------------------------------------------------- |
| 缓存名称散落 | 使用常量类或枚举统一管理                          |
| TTL 一刀切   | 使用自定义 `RedisCacheManager` 按缓存空间设置 TTL |
| 列表缓存太多 | 限制分页和查询条件，必要时只缓存首页或热点查询    |
| 字典缓存更新 | 字典变更后清空对应字典缓存                        |
| 配置缓存更新 | 配置变更后精确删除配置 Key                        |

## 缓存序列化

Redis 缓存涉及 Key 和 Value 的序列化。Key 建议使用字符串序列化，便于人工排查；Value 建议使用 JSON 序列化，便于阅读和跨版本兼容。Spring Data Redis 提供 `StringRedisSerializer` 和 `GenericJackson2JsonRedisSerializer` 等序列化器，其中 `GenericJackson2JsonRedisSerializer` 是基于 Jackson 2 的 JSON 序列化器，支持将对象映射为带动态类型信息的 JSON。([Home](https://docs.spring.io/spring-data-redis/docs/3.0.9/api/org/springframework/data/redis/serializer/GenericJackson2JsonRedisSerializer.html?utm_source=chatgpt.com))

### Redis Key 序列化

Redis Key 应保持可读、稳定、短小和可定位。Spring Cache 的 Redis Key 通常由缓存名前缀、缓存空间名称和业务 Key 组成。Spring Boot 官方文档建议保留 Redis 缓存名前缀，以避免不同缓存空间使用相同 Key 时产生冲突。([Home](https://docs.spring.io/spring-boot/reference/io/caching.html?utm_source=chatgpt.com))

推荐 Key 形态如下：

```text
ateng:productCache::detail:1
ateng:productListCache::ProductServiceImpl:list:86f7e437faa5a7fce15d1ddcb9eaeaea
ateng:dictCache::type:order_status
ateng:configCache::key:system.logo
```

Key 序列化配置如下：

```java
.serializeKeysWith(
        RedisSerializationContext.SerializationPair.fromSerializer(new StringRedisSerializer())
)
```

Key 设计建议：

| 建议             | 说明                      |
| ---------------- | ------------------------- |
| 使用字符串序列化 | Redis 中可直接查看 Key    |
| 加业务前缀       | 区分项目、环境或模块      |
| 保留缓存空间     | 避免不同缓存空间 Key 冲突 |
| 避免超长 Key     | 复杂参数建议做 MD5 摘要   |
| 避免随机 Key     | 随机值会导致缓存无法命中  |
| 规范分隔符       | 推荐使用 `:` 分隔业务层级 |

### Redis Value 序列化

Redis Value 存储的是方法返回值。默认 JDK 序列化可用，但可读性差、跨语言不友好、排查困难。生产环境通常建议使用 JSON 序列化。`GenericJackson2JsonRedisSerializer` 可以将对象序列化为 JSON，并通过类型信息支持复杂对象反序列化。([Home](https://docs.spring.io/spring-data-redis/docs/3.0.9/api/org/springframework/data/redis/serializer/GenericJackson2JsonRedisSerializer.html?utm_source=chatgpt.com))

Value 序列化配置如下：

```java
GenericJackson2JsonRedisSerializer valueSerializer = new GenericJackson2JsonRedisSerializer(objectMapper);

RedisCacheConfiguration.defaultCacheConfig()
        .serializeValuesWith(
                RedisSerializationContext.SerializationPair.fromSerializer(valueSerializer)
        );
```

常见序列化方案对比如下：

| 方案                                 | 优点                    | 局限                         | 推荐程度     |
| ------------------------------------ | ----------------------- | ---------------------------- | ------------ |
| JDK 序列化                           | 无需额外配置            | 不可读、体积较大、类变更敏感 | 不推荐       |
| `GenericJackson2JsonRedisSerializer` | JSON 可读，支持动态类型 | 会写入类型信息               | 推荐         |
| `Jackson2JsonRedisSerializer<T>`     | 类型明确，JSON 可读     | 通用缓存场景类型处理较麻烦   | 可选         |
| String 序列化                        | 简单直接                | 只适合字符串值               | 特定场景可用 |

### JSON 序列化配置

JSON 序列化需要重点处理 Java 8 时间类型、类型信息和反序列化兼容性。如果缓存对象中包含 `LocalDateTime`、`LocalDate`、`LocalTime`，需要注册 `JavaTimeModule`。如果缓存返回值类型较多，建议使用 `GenericJackson2JsonRedisSerializer` 并配置类型信息。

文件位置：`src/main/java/io/github/atengk/config/RedisSerializationConfig.java`

下面的配置类单独封装 Redis 缓存序列化配置，便于在 `RedisCacheManager` 中复用。

```java
package io.github.atengk.config;

import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.serializer.GenericJackson2JsonRedisSerializer;

/**
 * Redis JSON 序列化配置
 *
 * @author Ateng
 * @since 2026-05-06
 */
@Configuration
public class RedisSerializationConfig {

    /**
     * 创建缓存专用 JSON 序列化器
     *
     * @return GenericJackson2JsonRedisSerializer
     */
    @Bean
    public GenericJackson2JsonRedisSerializer redisJsonSerializer() {
        ObjectMapper objectMapper = new ObjectMapper();
        // 支持 Java 8 时间类型
        objectMapper.registerModule(new JavaTimeModule());
        // 写入类型信息，避免 Object 类型反序列化为 LinkedHashMap
        objectMapper.activateDefaultTyping(
                objectMapper.getPolymorphicTypeValidator(),
                ObjectMapper.DefaultTyping.NON_FINAL,
                JsonTypeInfo.As.PROPERTY
        );
        return new GenericJackson2JsonRedisSerializer(objectMapper);
    }

}
```

在 `RedisCacheManager` 中使用该序列化器：

```java
@Bean
@Primary
public CacheManager redisCacheManager(RedisConnectionFactory redisConnectionFactory,
                                      GenericJackson2JsonRedisSerializer redisJsonSerializer) {
    RedisCacheConfiguration defaultConfig = RedisCacheConfiguration.defaultCacheConfig()
            .entryTtl(Duration.ofMinutes(30))
            .disableCachingNullValues()
            .serializeKeysWith(RedisSerializationContext.SerializationPair.fromSerializer(new StringRedisSerializer()))
            .serializeValuesWith(RedisSerializationContext.SerializationPair.fromSerializer(redisJsonSerializer));

    return RedisCacheManager.builder(redisConnectionFactory)
            .cacheDefaults(defaultConfig)
            .transactionAware()
            .build();
}
```

序列化注意事项：

| 问题                           | 建议                                       |
| ------------------------------ | ------------------------------------------ |
| `LocalDateTime` 反序列化失败   | 注册 `JavaTimeModule`                      |
| 反序列化后变成 `LinkedHashMap` | 使用带类型信息的 JSON 序列化               |
| 类字段频繁变更                 | 控制缓存 TTL，避免旧结构数据长期存在       |
| 跨服务共享缓存                 | 保持对象结构兼容，或缓存 DTO 而不是 Entity |
| 缓存值过大                     | 精简 VO 字段，避免缓存完整聚合对象         |
| 安全风险                       | 不缓存不可信来源构造的任意类型对象         |

## 缓存一致性处理

缓存一致性处理的目标是降低数据库与缓存之间的数据不一致时间窗口。Spring Cache 的注解式缓存更适合“读多写少、允许短时间弱一致”的场景；强一致交易链路不应仅依赖注解缓存。Spring Framework 官方文档中，`@CacheEvict` 用于驱逐缓存，`@CachePut` 用于在方法执行后更新缓存，`@Caching` 可组合多个缓存操作。([Home](https://docs.spring.io/spring-framework/reference/integration/cache/annotations.html?utm_source=chatgpt.com))

### 数据更新后的缓存清理

数据更新后的缓存处理通常有两种方式：删除缓存和更新缓存。生产环境更推荐“先更新数据库，后删除缓存”，因为它简单、稳定、对复杂缓存关系更安全。只有当方法返回值就是缓存中的最新值，并且缓存关系简单时，才使用 `@CachePut` 更新缓存。

常见策略如下：

| 数据操作 | 推荐缓存动作                     | 说明                   |
| -------- | -------------------------------- | ---------------------- |
| 新增数据 | 清理列表缓存                     | 新增数据会影响列表结果 |
| 修改详情 | 删除或刷新详情缓存，清理列表缓存 | 详情和列表都可能变化   |
| 删除数据 | 删除详情缓存，清理列表缓存       | 避免已删除数据仍被命中 |
| 字典变更 | 清理对应字典缓存                 | 字典通常读多写少       |
| 配置变更 | 删除配置 Key                     | 避免旧配置继续生效     |

文件位置：`src/main/java/io/github/atengk/product/service/impl/ProductWriteServiceImpl.java`

下面的示例展示数据更新后如何清理详情缓存和列表缓存。

```java
package io.github.atengk.product.service.impl;

import cn.hutool.core.util.ObjectUtil;
import io.github.atengk.common.cache.CacheNames;
import io.github.atengk.product.model.dto.ProductUpdateRequest;
import io.github.atengk.product.model.vo.ProductVO;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Caching;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

/**
 * 商品写操作缓存一致性示例服务
 *
 * @author Ateng
 * @since 2026-05-06
 */
@Slf4j
@Service
public class ProductWriteServiceImpl {

    private final ConcurrentMap<Long, ProductVO> productStorage = new ConcurrentHashMap<>();

    /**
     * 修改商品并清理相关缓存
     *
     * @param id 商品 ID
     * @param request 修改请求
     * @return 商品详情
     */
    @Transactional(rollbackFor = Exception.class)
    @Caching(
            evict = {
                    @CacheEvict(cacheNames = CacheNames.PRODUCT, key = "'detail:' + #id"),
                    @CacheEvict(cacheNames = CacheNames.PRODUCT_LIST, allEntries = true)
            }
    )
    public ProductVO updateAndEvictCache(Long id, ProductUpdateRequest request) {
        ProductVO oldProduct = productStorage.get(id);
        if (ObjectUtil.isNull(oldProduct)) {
            log.warn("商品修改失败，商品不存在，商品ID：{}", id);
            return null;
        }

        ProductVO newProduct = new ProductVO(
                id,
                request.name(),
                request.price() == null ? BigDecimal.ZERO : request.price(),
                LocalDateTime.now()
        );
        productStorage.put(id, newProduct);

        log.info("商品修改成功，事务提交后清理商品详情缓存和商品列表缓存，商品ID：{}", id);
        return newProduct;
    }

    /**
     * 删除商品并清理相关缓存
     *
     * @param id 商品 ID
     */
    @Transactional(rollbackFor = Exception.class)
    @Caching(
            evict = {
                    @CacheEvict(cacheNames = CacheNames.PRODUCT, key = "'detail:' + #id"),
                    @CacheEvict(cacheNames = CacheNames.PRODUCT_LIST, allEntries = true)
            }
    )
    public void deleteAndEvictCache(Long id) {
        ProductVO removedProduct = productStorage.remove(id);
        if (ObjectUtil.isNull(removedProduct)) {
            log.warn("商品删除跳过，商品不存在，商品ID：{}", id);
            return;
        }

        log.info("商品删除成功，事务提交后清理商品详情缓存和商品列表缓存，商品ID：{}", id);
    }

}
```

一致性处理建议：

| 建议                            | 说明                                         |
| ------------------------------- | -------------------------------------------- |
| 写操作优先删除缓存              | 避免手动同步多个缓存副本                     |
| 列表缓存按空间清理              | 列表 Key 维度多，精确清理成本高              |
| 详情缓存精确清理                | 根据业务 ID 删除指定 Key                     |
| 配置 Redis `transactionAware()` | 事务提交后再执行缓存操作                     |
| 避免缓存提前更新                | 数据库事务失败时不应留下新缓存               |
| 高并发强一致场景单独设计        | 需要结合锁、消息队列、延迟双删或 binlog 订阅 |

延迟双删适合并发读写较高的场景，但它不能保证绝对强一致，只能进一步缩小旧缓存回填窗口。可以在业务更新后立即删除一次缓存，再延迟删除一次缓存。简单项目不建议一开始就引入该策略。

```java
@Caching(
        evict = {
                @CacheEvict(cacheNames = CacheNames.PRODUCT, key = "'detail:' + #id"),
                @CacheEvict(cacheNames = CacheNames.PRODUCT_LIST, allEntries = true)
        }
)
public void updateProduct(Long id, ProductUpdateRequest request) {
    // 先更新数据库，再删除缓存
    // 如果存在高并发旧值回填风险，可通过消息队列或定时任务做延迟二次删除
}
```

### 缓存穿透处理

缓存穿透是指请求查询一个缓存和数据库都不存在的数据，导致每次请求都绕过缓存并访问数据源。常见原因包括恶意请求不存在的 ID、参数校验缺失、接口被扫描、业务数据确实不存在但访问频率很高。

常见处理方案如下：

| 方案            | 说明                                | 适用场景       |
| --------------- | ----------------------------------- | -------------- |
| 参数校验        | 非法 ID、空关键字、异常分页直接拒绝 | 所有查询接口   |
| 不缓存 null     | 避免脏空值长期存在                  | 普通查询       |
| 短 TTL 空值缓存 | 对热点不存在数据做短时间占位        | 高频不存在 ID  |
| 布隆过滤器      | 判断 ID 是否可能存在                | 大规模 ID 查询 |
| 接口限流        | 降低恶意穿透流量                    | 外部公开接口   |

基础防护可以通过 `condition` 和 `unless` 实现：

```java
@Cacheable(
        cacheNames = CacheNames.PRODUCT,
        key = "'detail:' + #id",
        condition = "#id != null && #id > 0",
        unless = "#result == null"
)
public ProductVO getDetail(Long id) {
    return productRepository.getById(id);
}
```

如果某些不存在的 ID 被高频访问，可以使用短 TTL 空值占位缓存。由于前面的 Redis 配置禁用了 null 缓存，推荐使用业务包装对象表示“存在 / 不存在”，避免直接缓存 Java `null`。

文件位置：`src/main/java/io/github/atengk/product/model/vo/ProductCacheValue.java`

下面的包装对象用于区分真实商品数据和空值占位。

```java
package io.github.atengk.product.model.vo;

/**
 * 商品缓存包装值
 *
 * @author Ateng
 * @since 2026-05-06
 */
public record ProductCacheValue(

        /**
         * 数据是否存在
         */
        Boolean exists,

        /**
         * 商品数据
         */
        ProductVO data
) {

    /**
     * 创建存在数据的缓存值
     *
     * @param data 商品数据
     * @return 缓存包装值
     */
    public static ProductCacheValue exist(ProductVO data) {
        return new ProductCacheValue(Boolean.TRUE, data);
    }

    /**
     * 创建空值占位缓存
     *
     * @return 缓存包装值
     */
    public static ProductCacheValue empty() {
        return new ProductCacheValue(Boolean.FALSE, null);
    }

}
```

文件位置：`src/main/java/io/github/atengk/product/service/impl/ProductPenetrationServiceImpl.java`

下面的服务使用短 TTL 空值缓存降低不存在 ID 对数据源的重复访问。

```java
package io.github.atengk.product.service.impl;

import cn.hutool.core.lang.BooleanUtil;
import cn.hutool.core.util.ObjectUtil;
import io.github.atengk.common.cache.CacheNames;
import io.github.atengk.product.model.vo.ProductCacheValue;
import io.github.atengk.product.model.vo.ProductVO;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

/**
 * 商品缓存穿透处理示例服务
 *
 * @author Ateng
 * @since 2026-05-06
 */
@Slf4j
@Service
public class ProductPenetrationServiceImpl {

    private final ConcurrentMap<Long, ProductVO> productStorage = new ConcurrentHashMap<>();

    /**
     * 查询商品详情缓存包装值
     *
     * @param id 商品 ID
     * @return 商品缓存包装值
     */
    @Cacheable(
            cacheNames = CacheNames.NULL_VALUE,
            key = "'product:detail:' + #id",
            condition = "#id != null && #id > 0"
    )
    public ProductCacheValue getDetailCacheValue(Long id) {
        log.info("未命中商品空值保护缓存，查询数据源，商品ID：{}", id);

        ProductVO product = productStorage.get(id);
        if (ObjectUtil.isNull(product)) {
            log.info("商品不存在，写入短TTL空值占位缓存，商品ID：{}", id);
            return ProductCacheValue.empty();
        }

        return ProductCacheValue.exist(product);
    }

    /**
     * 查询商品详情
     *
     * @param id 商品 ID
     * @return 商品详情
     */
    public ProductVO getDetail(Long id) {
        ProductCacheValue cacheValue = this.getDetailCacheValue(id);
        if (ObjectUtil.isNull(cacheValue) || BooleanUtil.isFalse(cacheValue.exists())) {
            return null;
        }
        return cacheValue.data();
    }

}
```

对应的 `nullValueCache` 建议设置短 TTL，例如 1 到 2 分钟：

```java
cacheConfigMap.put("nullValueCache", this.createRedisCacheConfiguration(Duration.ofMinutes(2)));
```

缓存穿透处理建议：

| 建议                     | 说明                                               |
| ------------------------ | -------------------------------------------------- |
| 优先做参数校验           | 非法参数不进入缓存和数据库                         |
| 默认不缓存 null          | 避免空值长期污染缓存                               |
| 热点不存在数据用短 TTL   | 降低重复打到数据库                                 |
| 空值缓存时间要短         | 防止数据刚创建后仍返回空值                         |
| 高风险接口加限流         | 降低恶意扫描风险                                   |
| 大规模 ID 可用布隆过滤器 | 布隆过滤器需要额外维护，不是 Spring Cache 默认能力 |

### 缓存过期策略

缓存过期策略用于控制缓存生命周期。过期时间过短会降低命中率，过长会增加旧数据风险和内存压力。不同业务数据应设置不同 TTL，而不是所有缓存使用同一个过期时间。

推荐 TTL 策略如下：

| 缓存类型     | 推荐 TTL     | 说明                         |
| ------------ | ------------ | ---------------------------- |
| 商品详情     | 10-30 分钟   | 热点数据，允许短时间弱一致   |
| 商品列表     | 1-5 分钟     | 查询条件多，变化影响面大     |
| 字典数据     | 1-12 小时    | 变化少，可通过后台变更时清理 |
| 系统配置     | 5-60 分钟    | 视配置实时性要求调整         |
| 空值占位     | 30 秒-2 分钟 | 防穿透，必须短 TTL           |
| 远程接口结果 | 1-10 分钟    | 降低第三方接口压力           |

Redis 多缓存空间 TTL 配置示例：

```java
Map<String, RedisCacheConfiguration> cacheConfigMap = MapUtil.newHashMap();
cacheConfigMap.put("productCache", this.createRedisCacheConfiguration(Duration.ofMinutes(20)));
cacheConfigMap.put("productListCache", this.createRedisCacheConfiguration(Duration.ofMinutes(5)));
cacheConfigMap.put("dictCache", this.createRedisCacheConfiguration(Duration.ofHours(6)));
cacheConfigMap.put("configCache", this.createRedisCacheConfiguration(Duration.ofHours(1)));
cacheConfigMap.put("nullValueCache", this.createRedisCacheConfiguration(Duration.ofMinutes(2)));
```

为了避免大量 Key 同时过期造成缓存雪崩，可以在业务层对不同缓存空间设置不同 TTL。Spring Cache 标准注解不直接支持每个 Key 动态随机 TTL；如果必须对每个 Key 增加随机过期时间，可以使用 `RedisTemplate` 手动写缓存，或扩展缓存实现。

文件位置：`src/main/java/io/github/atengk/common/cache/CacheTtlHelper.java`

下面的工具类用于生成带轻微随机偏移的 TTL，适合手动缓存场景。

```java
package io.github.atengk.common.cache;

import cn.hutool.core.util.RandomUtil;

import java.time.Duration;

/**
 * 缓存过期时间辅助工具
 *
 * @author Ateng
 * @since 2026-05-06
 */
public final class CacheTtlHelper {

    private CacheTtlHelper() {
        throw new IllegalStateException("缓存工具类不能实例化");
    }

    /**
     * 生成带随机偏移的 TTL
     *
     * @param baseSeconds 基础秒数
     * @param randomSeconds 随机偏移秒数
     * @return 过期时间
     */
    public static Duration randomTtl(long baseSeconds, int randomSeconds) {
        int offset = RandomUtil.randomInt(0, Math.max(randomSeconds, 1));
        return Duration.ofSeconds(baseSeconds + offset);
    }

}
```

缓存过期策略建议：

| 策略                    | 说明                                         |
| ----------------------- | -------------------------------------------- |
| 所有缓存必须有 TTL      | 避免缓存永久占用内存                         |
| 按业务类型设置 TTL      | 详情、列表、字典、配置分开配置               |
| 热点 Key 可适当延长 TTL | 降低高频数据回源压力                         |
| 列表缓存 TTL 应较短     | 列表受新增、修改、删除影响更大               |
| 空值缓存 TTL 必须短     | 避免真实数据创建后仍命中空值                 |
| 避免同一时间大批量过期  | 通过不同缓存空间 TTL 或随机 TTL 降低雪崩风险 |
| 缓存不是权威数据源      | 过期后必须能从数据库或远程服务重新构建       |

验证 Redis 缓存过期时间：

```bash
# 扫描商品详情缓存 Key
redis-cli --scan --pattern "*productCache*"

# 查看指定 Key 的剩余过期时间，Key 按实际扫描结果替换
redis-cli TTL "ateng:productCache::detail:1"

# 查看 Redis 当前 Key 数量
redis-cli DBSIZE
```

`TTL` 返回正整数表示剩余秒数，返回 `-1` 表示 Key 存在但没有过期时间，返回 `-2` 表示 Key 不存在。生产环境中业务缓存出现大量 `TTL = -1` 的 Key，通常说明缓存配置存在风险，需要检查 `RedisCacheConfiguration.entryTtl()` 是否生效。



## 测试与验证

本章节用于验证 Spring Cache 是否正确启用、缓存是否命中、缓存是否按预期清理，以及 Redis 中的数据结构、Key 和 TTL 是否符合设计。Spring Boot 提供 `spring-boot-starter-test`，其中包含 Spring Boot 测试模块、JUnit Jupiter、AssertJ、Hamcrest 等常用测试能力；接口测试可以使用 MockMvc，在不启动真实服务器的情况下完成 Spring MVC 请求处理验证。([Home](https://docs.spring.io/spring-boot/reference/testing/index.html?utm_source=chatgpt.com))

### 单元测试

单元测试主要验证 Service 层缓存逻辑是否生效。测试重点不是 Redis 本身，而是确认 `@Cacheable`、`@CachePut`、`@CacheEvict` 等注解在 Spring 容器中能够按预期执行。由于 Spring Cache 基于代理机制，单元测试建议使用 `@SpringBootTest` 启动 Spring 容器，而不是直接 `new` Service 实例。Spring Boot 官方文档说明，`@SpringBootTest` 会通过 `SpringApplication` 创建测试用 `ApplicationContext`，适合需要 Spring Boot 特性的测试场景。([Home](https://docs.spring.io/spring-boot/reference/testing/spring-boot-applications.html?utm_source=chatgpt.com))

测试环境可以使用 Simple 本地缓存，减少 Redis 依赖。

文件位置：`src/test/resources/application-test.yml`

```yaml
spring:
  cache:
    # 测试环境使用 Simple 本地缓存，避免单元测试依赖 Redis 服务
    type: simple

logging:
  level:
    io.github.atengk: info
    org.springframework.cache: debug
```

文件位置：`src/test/java/io/github/atengk/product/service/ProductServiceCacheTest.java`

下面的测试类用于验证商品详情查询缓存、商品更新后的缓存刷新，以及商品删除后的缓存清理。

```java
package io.github.atengk.product.service;

import io.github.atengk.common.cache.CacheNames;
import io.github.atengk.product.model.dto.ProductUpdateRequest;
import io.github.atengk.product.model.vo.ProductVO;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.test.context.ActiveProfiles;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 商品 Service 缓存测试
 *
 * @author Ateng
 * @since 2026-05-06
 */
@SpringBootTest
@ActiveProfiles("test")
class ProductServiceCacheTest {

    @Autowired
    private ProductService productService;

    @Autowired
    private CacheManager cacheManager;

    /**
     * 每次测试前清理缓存，避免测试之间互相影响
     */
    @BeforeEach
    void clearCache() {
        this.clearCache(CacheNames.PRODUCT);
        this.clearCache(CacheNames.PRODUCT_LIST);
    }

    /**
     * 验证商品详情查询缓存
     */
    @Test
    void shouldCacheProductDetail() {
        ProductVO firstResult = productService.getDetail(1L);
        ProductVO secondResult = productService.getDetail(1L);

        assertThat(firstResult).isNotNull();
        assertThat(secondResult).isNotNull();
        assertThat(secondResult.id()).isEqualTo(firstResult.id());

        Cache productCache = cacheManager.getCache(CacheNames.PRODUCT);
        assertThat(productCache).isNotNull();

        Cache.ValueWrapper valueWrapper = productCache.get("detail:1");
        assertThat(valueWrapper).isNotNull();
        assertThat(valueWrapper.get()).isNotNull();
    }

    /**
     * 验证修改商品后刷新详情缓存
     */
    @Test
    void shouldRefreshProductDetailCacheAfterUpdate() {
        productService.getDetail(1L);

        ProductUpdateRequest request = new ProductUpdateRequest("机械键盘 Max", new BigDecimal("499.00"));
        ProductVO updatedProduct = productService.update(1L, request);

        assertThat(updatedProduct).isNotNull();
        assertThat(updatedProduct.name()).isEqualTo("机械键盘 Max");

        Cache productCache = cacheManager.getCache(CacheNames.PRODUCT);
        assertThat(productCache).isNotNull();

        Cache.ValueWrapper valueWrapper = productCache.get("detail:1");
        assertThat(valueWrapper).isNotNull();

        ProductVO cachedProduct = (ProductVO) valueWrapper.get();
        assertThat(cachedProduct).isNotNull();
        assertThat(cachedProduct.name()).isEqualTo("机械键盘 Max");
    }

    /**
     * 验证删除商品后清理详情缓存
     */
    @Test
    void shouldEvictProductDetailCacheAfterDelete() {
        productService.getDetail(2L);

        Cache productCache = cacheManager.getCache(CacheNames.PRODUCT);
        assertThat(productCache).isNotNull();
        assertThat(productCache.get("detail:2")).isNotNull();

        productService.delete(2L);

        assertThat(productCache.get("detail:2")).isNull();
    }

    /**
     * 清理指定缓存空间
     *
     * @param cacheName 缓存名称
     */
    private void clearCache(String cacheName) {
        Cache cache = cacheManager.getCache(cacheName);
        if (cache != null) {
            cache.clear();
        }
    }

}
```

运行测试：

```bash
mvn test -Dtest=ProductServiceCacheTest
```

该命令只运行 `ProductServiceCacheTest` 测试类。测试通过后，说明 Spring Cache 注解已经在 Spring 容器代理下生效，并且 Service 层的查询、更新、删除缓存逻辑符合预期。

如果需要验证方法是否只执行一次，可以在 Service 中保留前面示例的中文日志，例如 `未命中商品详情缓存，查询数据源`。首次调用会打印该日志，第二次相同 Key 调用不再打印，说明查询结果来自缓存。

### 接口测试

接口测试用于验证 Controller 层请求能否正确触发 Service 层缓存逻辑。MockMvc 可以在不启动真实 HTTP 服务的情况下执行完整 Spring MVC 请求处理流程，包括请求映射、参数绑定、校验、消息转换和响应断言。Spring Framework 官方文档说明，MockMvc 使用模拟请求和响应对象完成 Spring MVC 请求处理，适合控制器和接口层测试。([Home](https://docs.spring.io/spring-framework/reference/testing/mockmvc.html?utm_source=chatgpt.com))

文件位置：`src/test/java/io/github/atengk/product/controller/ProductControllerCacheTest.java`

下面的接口测试类用于验证商品详情查询、商品列表查询、商品更新和删除接口。

```java
package io.github.atengk.product.controller;

import io.github.atengk.common.cache.CacheNames;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * 商品接口缓存测试
 *
 * @author Ateng
 * @since 2026-05-06
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class ProductControllerCacheTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private CacheManager cacheManager;

    /**
     * 每次测试前清理缓存，避免历史缓存影响断言
     */
    @BeforeEach
    void clearCache() {
        this.clearCache(CacheNames.PRODUCT);
        this.clearCache(CacheNames.PRODUCT_LIST);
    }

    /**
     * 验证查询商品详情接口会写入缓存
     *
     * @throws Exception 请求异常
     */
    @Test
    void shouldCacheProductDetailWhenCallQueryApi() throws Exception {
        mockMvc.perform(get("/api/products/{id}", 1L))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.name").exists());

        Cache productCache = cacheManager.getCache(CacheNames.PRODUCT);
        assertThat(productCache).isNotNull();
        assertThat(productCache.get("detail:1")).isNotNull();
    }

    /**
     * 验证修改商品接口会刷新详情缓存
     *
     * @throws Exception 请求异常
     */
    @Test
    void shouldRefreshCacheWhenUpdateProduct() throws Exception {
        mockMvc.perform(get("/api/products/{id}", 1L))
                .andExpect(status().isOk());

        String requestBody = """
                {
                  "name": "机械键盘 Ultra",
                  "price": 599.00
                }
                """;

        mockMvc.perform(put("/api/products/{id}", 1L)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("机械键盘 Ultra"))
                .andExpect(jsonPath("$.price").value(599.00));

        Cache productCache = cacheManager.getCache(CacheNames.PRODUCT);
        assertThat(productCache).isNotNull();
        assertThat(productCache.get("detail:1")).isNotNull();
    }

    /**
     * 验证删除商品接口会清理详情缓存
     *
     * @throws Exception 请求异常
     */
    @Test
    void shouldEvictCacheWhenDeleteProduct() throws Exception {
        mockMvc.perform(get("/api/products/{id}", 2L))
                .andExpect(status().isOk());

        Cache productCache = cacheManager.getCache(CacheNames.PRODUCT);
        assertThat(productCache).isNotNull();
        assertThat(productCache.get("detail:2")).isNotNull();

        mockMvc.perform(delete("/api/products/{id}", 2L))
                .andExpect(status().isNoContent());

        assertThat(productCache.get("detail:2")).isNull();
    }

    /**
     * 清理指定缓存空间
     *
     * @param cacheName 缓存名称
     */
    private void clearCache(String cacheName) {
        Cache cache = cacheManager.getCache(cacheName);
        if (cache != null) {
            cache.clear();
        }
    }

}
```

运行接口测试：

```bash
mvn test -Dtest=ProductControllerCacheTest
```

如果项目使用 Gradle，可以执行：

```bash
./gradlew test --tests "io.github.atengk.product.controller.ProductControllerCacheTest"
```

接口测试通过后，说明 Controller 请求能够正常进入 Service 层，并触发 Spring Cache 注解逻辑。需要注意，测试方法中通过 `CacheManager` 校验缓存是为了确认缓存状态；实际业务接口不应暴露缓存内部细节。

### Redis 数据验证

Redis 数据验证用于确认生产环境或联调环境中的缓存 Key、Value、TTL 和清理行为是否符合设计。Spring Boot 的 Redis 缓存支持通过 `spring.cache.cache-names` 创建缓存空间，通过 `spring.cache.redis.*` 设置默认配置；官方文档也建议保留 Redis 缓存 Key 前缀，以避免不同缓存空间使用相同 Key 时产生冲突。([Home](https://docs.spring.io/spring-boot/reference/io/caching.html?utm_source=chatgpt.com))

启动 Redis 后，使用 Redis 配置运行应用：

```bash
mvn spring-boot:run -Dspring-boot.run.profiles=redis
```

执行查询接口，触发缓存写入：

```bash
# 第一次请求，未命中缓存，会查询数据源并写入 Redis
curl -s http://localhost:8080/api/products/1

# 第二次请求，命中缓存，不再执行 Service 查询逻辑
curl -s http://localhost:8080/api/products/1
```

扫描 Redis 中的缓存 Key：

```bash
# 扫描商品详情缓存
redis-cli --scan --pattern "*productCache*"

# 扫描商品列表缓存
redis-cli --scan --pattern "*productListCache*"

# 扫描当前项目缓存前缀
redis-cli --scan --pattern "ateng:*"
```

查看指定 Key 的 Value 和 TTL：

```bash
# 查看缓存值，Key 需要替换为实际扫描结果
redis-cli GET "ateng:productCache::detail:1"

# 查看剩余过期时间，单位秒
redis-cli TTL "ateng:productCache::detail:1"
```

验证修改后的缓存刷新：

```bash
curl -s -X PUT "http://localhost:8080/api/products/1" \
  -H "Content-Type: application/json" \
  -d '{
    "name": "机械键盘 Pro",
    "price": 399.00
  }'

redis-cli GET "ateng:productCache::detail:1"
redis-cli --scan --pattern "*productListCache*"
```

验证删除后的缓存清理：

```bash
curl -i -X DELETE "http://localhost:8080/api/products/1"

redis-cli GET "ateng:productCache::detail:1"
redis-cli --scan --pattern "*productListCache*"
```

`TTL` 返回值说明如下：

| 返回值 | 说明                         | 处理建议                                               |
| ------ | ---------------------------- | ------------------------------------------------------ |
| 正整数 | Key 存在，并且有剩余过期时间 | 正常                                                   |
| `-1`   | Key 存在，但没有过期时间     | 检查 `entryTtl()` 或 `spring.cache.redis.time-to-live` |
| `-2`   | Key 不存在                   | 可能未写入、已过期或已被删除                           |

Redis 验证重点如下：

| 验证项             | 预期结果                                           |
| ------------------ | -------------------------------------------------- |
| Key 是否带缓存空间 | 应包含 `productCache`、`productListCache` 等缓存名 |
| Key 是否带项目前缀 | 应包含 `ateng:` 或项目自定义前缀                   |
| Value 是否可读     | 使用 JSON 序列化后应具备基本可读性                 |
| TTL 是否存在       | 业务缓存不应长期 `TTL = -1`                        |
| 更新后详情缓存     | 应刷新为最新数据或被删除后重新加载                 |
| 更新后列表缓存     | 应被清理，避免列表旧数据                           |
| 删除后详情缓存     | 对应详情 Key 应不存在                              |

## 开发注意事项

本章节汇总 Spring Cache 开发中的常见问题。Spring Cache 注解基于 Spring AOP 代理执行，缓存注解声明本身不会自动生效，必须启用缓存支持并通过 Spring 容器代理对象调用；Spring Framework 官方文档也说明，声明缓存注解并不会自动触发行为，需要通过 `@EnableCaching` 或 XML 方式启用缓存注解能力。([Home](https://docs.spring.io/spring-framework/reference/integration/cache/annotations.html?utm_source=chatgpt.com))

### 注解失效场景

注解失效通常不是缓存组件问题，而是代理调用链、Bean 管理方式、方法可见性、Key 条件或配置问题导致的。`@Cacheable` 会在调用时检查缓存中是否已有对应 Key；命中时直接返回缓存值，未命中时执行目标方法并存储返回值，因此一旦代理没有生效，方法会每次都执行。([Home](https://docs.spring.io/spring-framework/docs/current/javadoc-api/org/springframework/cache/annotation/Cacheable.html?utm_source=chatgpt.com))

常见失效场景如下：

| 场景                    | 现象                   | 原因                          | 处理方式                                |
| ----------------------- | ---------------------- | ----------------------------- | --------------------------------------- |
| 未添加 `@EnableCaching` | 注解完全不生效         | 未启用缓存注解能力            | 在启动类或配置类添加 `@EnableCaching`   |
| 同类内部方法调用        | 每次都执行目标方法     | `this.xxx()` 绕过 Spring 代理 | 拆到另一个 Service，或通过代理对象调用  |
| 对象不是 Spring Bean    | 注解不生效             | 未被 Spring 容器管理          | 使用 `@Service`、`@Component` 注册 Bean |
| 方法不是 `public`       | 注解可能不生效         | 代理拦截不到目标方法          | 缓存方法使用 `public`                   |
| `key` 表达式错误        | 抛 SpEL 异常或缓存错乱 | 参数名、字段名写错            | 使用 `#p0` 或确保参数名可用             |
| `condition` 为 false    | 不写入缓存             | 条件表达式阻止缓存            | 检查条件表达式                          |
| `unless` 为 true        | 方法执行但不缓存       | 返回值满足排除条件            | 检查返回值和 `unless`                   |
| `cacheNames` 不存在     | 缓存异常或未创建       | 缓存空间未配置                | 配置 `cache-names` 或允许动态创建       |
| 多个 `CacheManager`     | 使用了非预期缓存管理器 | 未指定主缓存管理器            | 添加 `@Primary` 或指定 `cacheManager`   |
| 返回对象不可序列化      | Redis 写入失败         | Value 序列化失败              | 检查 JSON 序列化配置                    |

错误示例：同类内部调用导致缓存注解不生效。

```java
@Service
public class ProductInternalCallService {

    public ProductVO wrapper(Long id) {
        // 该调用不会经过 Spring 代理，getDetail 上的缓存注解可能不生效
        return this.getDetail(id);
    }

    @Cacheable(cacheNames = "productCache", key = "'detail:' + #id")
    public ProductVO getDetail(Long id) {
        return queryFromDatabase(id);
    }

}
```

推荐写法：将缓存方法放到独立 Service 中，由另一个 Spring Bean 调用。

文件位置：`src/main/java/io/github/atengk/product/service/ProductQueryService.java`

下面的 Service 专门负责可缓存的查询方法。

```java
package io.github.atengk.product.service;

import io.github.atengk.common.cache.CacheNames;
import io.github.atengk.product.model.vo.ProductVO;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

/**
 * 商品查询服务
 *
 * @author Ateng
 * @since 2026-05-06
 */
@Slf4j
@Service
public class ProductQueryService {

    /**
     * 查询商品详情
     *
     * @param id 商品 ID
     * @return 商品详情
     */
    @Cacheable(
            cacheNames = CacheNames.PRODUCT,
            key = "'detail:' + #id",
            condition = "#id != null && #id > 0",
            unless = "#result == null"
    )
    public ProductVO getDetail(Long id) {
        log.info("未命中商品详情缓存，查询数据源，商品ID：{}", id);
        return this.queryFromDatabase(id);
    }

    /**
     * 模拟查询数据源
     *
     * @param id 商品 ID
     * @return 商品详情
     */
    private ProductVO queryFromDatabase(Long id) {
        return null;
    }

}
```

文件位置：`src/main/java/io/github/atengk/product/service/ProductFacadeService.java`

下面的 Facade Service 通过注入 Spring Bean 调用缓存查询方法，避免同类内部调用失效。

```java
package io.github.atengk.product.service;

import io.github.atengk.product.model.vo.ProductVO;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

/**
 * 商品门面服务
 *
 * @author Ateng
 * @since 2026-05-06
 */
@Service
@RequiredArgsConstructor
public class ProductFacadeService {

    private final ProductQueryService productQueryService;

    /**
     * 查询商品详情
     *
     * @param id 商品 ID
     * @return 商品详情
     */
    public ProductVO getDetail(Long id) {
        return productQueryService.getDetail(id);
    }

}
```

排查注解是否生效，可以临时开启缓存日志：

```yaml
logging:
  level:
    org.springframework.cache: trace
    io.github.atengk: debug
```

### 事务与缓存顺序

事务和缓存顺序是生产环境中最容易引入脏数据的问题。默认情况下，缓存注解和事务注解都通过 Spring AOP 执行；如果数据库事务回滚，但缓存已经更新或删除，就可能出现缓存与数据库不一致。Redis 缓存场景建议在 `RedisCacheManager` 中启用 `transactionAware()`，使缓存操作尽量在事务提交后执行。

Spring Cache 注解职责需要区分清楚：`@CachePut` 会执行方法并更新缓存，`@CacheEvict` 用于驱逐缓存，`@Caching` 用于组合多个缓存操作；当一个写操作同时影响详情缓存和列表缓存时，通常使用 `@Caching` 组合处理。([Home](https://docs.spring.io/spring-framework/reference/integration/cache/annotations.html?utm_source=chatgpt.com))

推荐的写操作顺序如下：

```text
1. 校验请求参数
2. 查询或锁定业务数据
3. 执行数据库新增、修改或删除
4. 数据库事务提交
5. 删除或刷新缓存
6. 后续查询重新构建缓存
```

文件位置：`src/main/java/io/github/atengk/config/RedisCacheTransactionConfig.java`

下面的配置类通过 `transactionAware()` 配置事务感知的 Redis 缓存管理器。

```java
package io.github.atengk.config;

import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.springframework.cache.CacheManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.data.redis.cache.RedisCacheConfiguration;
import org.springframework.data.redis.cache.RedisCacheManager;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.serializer.GenericJackson2JsonRedisSerializer;
import org.springframework.data.redis.serializer.RedisSerializationContext;
import org.springframework.data.redis.serializer.StringRedisSerializer;

import java.time.Duration;

/**
 * Redis 缓存事务顺序配置
 *
 * @author Ateng
 * @since 2026-05-06
 */
@Configuration
public class RedisCacheTransactionConfig {

    /**
     * 配置事务感知的 Redis 缓存管理器
     *
     * @param redisConnectionFactory Redis 连接工厂
     * @return 缓存管理器
     */
    @Bean
    @Primary
    public CacheManager transactionAwareRedisCacheManager(RedisConnectionFactory redisConnectionFactory) {
        ObjectMapper objectMapper = new ObjectMapper();
        objectMapper.registerModule(new JavaTimeModule());
        objectMapper.activateDefaultTyping(
                objectMapper.getPolymorphicTypeValidator(),
                ObjectMapper.DefaultTyping.NON_FINAL,
                JsonTypeInfo.As.PROPERTY
        );

        RedisCacheConfiguration cacheConfiguration = RedisCacheConfiguration.defaultCacheConfig()
                // 设置默认 TTL，避免缓存永久有效
                .entryTtl(Duration.ofMinutes(30))
                // 禁止缓存 null 值
                .disableCachingNullValues()
                // Key 使用字符串序列化
                .serializeKeysWith(RedisSerializationContext.SerializationPair.fromSerializer(new StringRedisSerializer()))
                // Value 使用 JSON 序列化
                .serializeValuesWith(RedisSerializationContext.SerializationPair.fromSerializer(new GenericJackson2JsonRedisSerializer(objectMapper)));

        return RedisCacheManager.builder(redisConnectionFactory)
                .cacheDefaults(cacheConfiguration)
                // 事务提交后执行缓存操作，降低事务回滚导致的脏缓存风险
                .transactionAware()
                .build();
    }

}
```

文件位置：`src/main/java/io/github/atengk/product/service/ProductTransactionalService.java`

下面的 Service 展示事务方法中更新数据库后清理缓存的推荐写法。

```java
package io.github.atengk.product.service;

import cn.hutool.core.util.ObjectUtil;
import io.github.atengk.common.cache.CacheNames;
import io.github.atengk.product.model.dto.ProductUpdateRequest;
import io.github.atengk.product.model.vo.ProductVO;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Caching;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

/**
 * 商品事务缓存服务
 *
 * @author Ateng
 * @since 2026-05-06
 */
@Slf4j
@Service
public class ProductTransactionalService {

    private final ConcurrentMap<Long, ProductVO> productStorage = new ConcurrentHashMap<>();

    /**
     * 修改商品并在事务成功后清理缓存
     *
     * @param id 商品 ID
     * @param request 修改请求
     * @return 商品详情
     */
    @Transactional(rollbackFor = Exception.class)
    @Caching(
            evict = {
                    @CacheEvict(cacheNames = CacheNames.PRODUCT, key = "'detail:' + #id"),
                    @CacheEvict(cacheNames = CacheNames.PRODUCT_LIST, allEntries = true)
            }
    )
    public ProductVO updateProduct(Long id, ProductUpdateRequest request) {
        ProductVO oldProduct = productStorage.get(id);
        if (ObjectUtil.isNull(oldProduct)) {
            log.warn("商品修改失败，商品不存在，商品ID：{}", id);
            return null;
        }

        ProductVO newProduct = new ProductVO(id, request.name(), request.price(), LocalDateTime.now());
        productStorage.put(id, newProduct);

        log.info("商品修改成功，等待事务提交后清理缓存，商品ID：{}", id);
        return newProduct;
    }

}
```

事务与缓存建议如下：

| 场景                 | 建议                                           |
| -------------------- | ---------------------------------------------- |
| 普通更新             | 更新数据库后删除缓存                           |
| 更新方法返回最新详情 | 可以使用 `@CachePut` 刷新详情缓存              |
| 更新影响列表         | 同时清理列表缓存                               |
| 存在事务             | 使用 `transactionAware()` 降低回滚脏缓存风险   |
| 高并发写入           | 考虑数据库锁、消息队列、延迟双删或 binlog 订阅 |
| 强一致交易链路       | 不建议仅依赖 Spring Cache 注解                 |

### 常见问题排查

常见问题排查应从四个方向开始：缓存是否启用、代理是否生效、Key 是否正确、底层缓存是否写入。Spring Boot Redis 缓存可以通过 `spring.cache.cache-names` 创建缓存空间，通过 `spring.cache.redis.time-to-live` 设置 TTL，并且默认会添加 Key 前缀来避免不同缓存空间冲突。([Home](https://docs.spring.io/spring-boot/reference/io/caching.html?utm_source=chatgpt.com))

常见问题清单如下：

| 问题                   | 可能原因                                        | 排查方式                                      | 处理方式                                               |
| ---------------------- | ----------------------------------------------- | --------------------------------------------- | ------------------------------------------------------ |
| 每次查询都执行方法     | 缓存未启用、内部调用、Key 每次不同              | 看业务日志和 `org.springframework.cache` 日志 | 添加 `@EnableCaching`，避免 `this` 调用，固定 Key      |
| Redis 没有 Key         | 未命中写入条件、`unless` 排除、Redis 配置未生效 | 查看日志、检查 `spring.cache.type`            | 修正条件表达式和配置                                   |
| Key 看不懂             | 使用默认 Key 或 JDK 序列化                      | `redis-cli --scan` 查看 Key                   | 使用 `StringRedisSerializer` 和业务 Key                |
| Value 是乱码           | 使用 JDK 序列化                                 | `redis-cli GET` 查看 Value                    | 改为 JSON 序列化                                       |
| TTL 为 `-1`            | 未设置过期时间                                  | `redis-cli TTL key`                           | 配置 `entryTtl()` 或 `spring.cache.redis.time-to-live` |
| 更新后仍返回旧数据     | 未清理缓存、清理 Key 不一致                     | 对比 `@Cacheable` 和 `@CacheEvict` 的 Key     | 统一 Key 常量和 SpEL                                   |
| 列表数据不更新         | 修改后只清理详情缓存                            | 扫描 `productListCache`                       | 新增、修改、删除后清理列表缓存                         |
| 缓存报序列化异常       | 返回对象类型不兼容                              | 查看异常堆栈                                  | 调整 JSON 序列化、缓存 VO 而非 Entity                  |
| 多个 CacheManager 冲突 | 存在 Redis 和 Caffeine 多个管理器               | 查看 Bean 注入情况                            | 使用 `@Primary` 或指定 `cacheManager`                  |
| 测试缓存不稳定         | 测试之间共享缓存                                | 每个测试前清理缓存                            | `@BeforeEach` 调用 `cache.clear()`                     |

排查命令如下：

```bash
# 查看 Redis 是否连通
redis-cli PING

# 扫描项目缓存 Key
redis-cli --scan --pattern "ateng:*"

# 查看商品详情缓存
redis-cli --scan --pattern "*productCache*"

# 查看商品列表缓存
redis-cli --scan --pattern "*productListCache*"

# 查看指定 Key 的剩余过期时间
redis-cli TTL "ateng:productCache::detail:1"

# 查看指定 Key 的缓存值
redis-cli GET "ateng:productCache::detail:1"

# 删除指定 Key，手动验证缓存重建
redis-cli DEL "ateng:productCache::detail:1"
```

这些命令分别用于检查 Redis 连接、扫描缓存 Key、查看 TTL、查看缓存值和手动删除缓存。生产环境排查时优先使用 `--scan`，避免使用 `KEYS *` 扫描全量 Key 造成 Redis 阻塞风险。

应用侧排查可以临时提高日志级别：

```yaml
logging:
  level:
    # 查看 Spring Cache 注解解析和缓存操作日志
    org.springframework.cache: trace
    # 查看项目业务日志
    io.github.atengk: debug
    # 查看 Redis 命令相关日志，必要时临时开启
    org.springframework.data.redis: debug
```

推荐排查顺序如下：

```text
1. 确认依赖中存在 spring-boot-starter-cache
2. 确认启动类或配置类存在 @EnableCaching
3. 确认缓存方法所在类是 Spring Bean
4. 确认调用路径经过 Spring 代理，而不是 this 内部调用
5. 确认 cacheNames 和 key 与清理逻辑一致
6. 确认 condition 和 unless 没有阻止缓存写入
7. 确认 CacheManager 是预期实现
8. 确认 Redis 中存在 Key 且 TTL 正常
9. 确认 Value 序列化方式可读且可反序列化
10. 确认数据更新后执行了对应缓存清理
```

最终验收标准如下：

| 验收项      | 标准                                         |
| ----------- | -------------------------------------------- |
| 查询缓存    | 相同 Key 第二次调用不再执行数据源查询        |
| 更新缓存    | 修改后详情缓存刷新或被删除                   |
| 删除缓存    | 删除后详情缓存不存在                         |
| 列表缓存    | 新增、修改、删除后列表缓存被清理             |
| Redis Key   | Key 可读，包含项目、缓存空间和业务标识       |
| Redis Value | Value 可读或可稳定反序列化                   |
| TTL         | 业务缓存存在合理过期时间                     |
| 测试        | Service 和 Controller 缓存测试通过           |
| 日志        | 缓存命中与未命中可通过业务日志或缓存日志定位 |