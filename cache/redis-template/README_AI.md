# RedisTemplate

`RedisTemplate` 是 Spring Data Redis 提供的 Redis 操作模板，适用于在 Spring Boot 3 项目中以统一方式操作 Redis 的 String、Hash、List、Set、ZSet 等数据结构。它封装了 Redis 连接、序列化、异常转换和常用命令操作，开发时通常不直接操作底层 `RedisConnection`。Spring Data Redis 官方也明确说明，`RedisTemplate` 是 Redis 模块的核心高级抽象，并负责处理序列化和连接管理。([Home](https://docs.spring.io/spring-data/redis/reference/redis/template.html))

## RedisTemplate 概述

本节用于说明 `RedisTemplate` 在 Spring Boot 3 Redis 开发中的定位、能力边界和典型使用场景，帮助在项目选型时判断是否应该使用 `RedisTemplate`，还是使用 `StringRedisTemplate`、Spring Cache、Redisson 或响应式 `ReactiveRedisTemplate`。

### 功能定位

`RedisTemplate` 的核心定位是 Spring 应用访问 Redis 的通用操作入口。它不是 Redis 客户端本身，而是基于底层 Redis 驱动封装出的模板类。Spring Boot 3 默认使用 Lettuce 作为 Redis 客户端，`RedisTemplate` 通过 `RedisConnectionFactory` 获取连接，再通过不同的 `opsForXxx()` 方法完成数据读写。

常用操作入口如下：

| 操作入口        | 对应 Redis 数据结构 | 典型用途                           |
| --------------- | ------------------- | ---------------------------------- |
| `opsForValue()` | String              | 缓存对象、验证码、计数器、临时标记 |
| `opsForHash()`  | Hash                | 用户信息、配置项、对象字段级缓存   |
| `opsForList()`  | List                | 队列、最近记录、消息列表           |
| `opsForSet()`   | Set                 | 去重集合、标签集合、权限集合       |
| `opsForZSet()`  | ZSet                | 排行榜、权重排序、延迟队列基础结构 |
| `expire()`      | Key TTL             | 设置缓存过期时间                   |
| `delete()`      | Key 删除            | 删除缓存、清理临时数据             |
| `execute()`     | 底层回调            | 执行更底层的 Redis 操作            |

`RedisTemplate` 配置完成后是线程安全的，可以作为 Spring Bean 被多个业务类复用。官方文档也说明，`RedisTemplate` 提供了面向不同 Redis 数据结构的操作视图，例如 `ValueOperations`、`HashOperations`、`ListOperations`、`SetOperations`、`ZSetOperations` 等。([Home](https://docs.spring.io/spring-data/redis/reference/redis/template.html))

在实际项目中，推荐自定义 `RedisTemplate<String, Object>`，并显式配置 Key、Value、Hash Key、Hash Value 的序列化方式。不要直接依赖默认 JDK 序列化，因为默认序列化结果不可读，跨语言不友好，并且在不可信环境下存在反序列化安全风险。Spring Data Redis 官方也建议不要在不可信环境中使用 Java 原生序列化，通常应改用 JSON 等格式。([Home](https://docs.spring.io/spring-data/redis/reference/redis/template.html))

### 适用场景

`RedisTemplate` 适合需要精细控制 Redis 数据结构和序列化方式的业务场景。相比 `@Cacheable` 这类声明式缓存，`RedisTemplate` 更偏向命令级、业务级、结构化的 Redis 操作。

适合使用 `RedisTemplate` 的场景包括：

| 场景          | 说明                                           |
| ------------- | ---------------------------------------------- |
| 业务缓存      | 缓存用户信息、权限信息、字典配置、接口查询结果 |
| 临时数据      | 保存验证码、短信码、登录 token、一次性业务标识 |
| 计数器        | 浏览量、点赞数、接口限流计数、重试次数         |
| 分布式状态    | 保存任务状态、流程状态、幂等标记               |
| 排行榜        | 使用 ZSet 保存积分、热度、权重分值             |
| 集合去重      | 使用 Set 保存用户标签、已处理 ID、访问记录     |
| 简单队列      | 使用 List 实现轻量级先进先出或后进先出队列     |
| Hash 对象缓存 | 将对象字段拆分到 Redis Hash 中，便于局部更新   |

不建议仅为了普通方法缓存就直接使用 `RedisTemplate`。如果只是方法级缓存，可以优先考虑 Spring Cache。如果需要分布式锁、延迟队列、可重入锁、信号量等高级并发能力，可以优先考虑 Redisson。

## 环境准备

本节用于说明 RedisTemplate 开发前需要准备的 Redis 服务、Spring Boot 版本和 Maven 依赖。后续配置 `RedisTemplate`、编写工具类和业务代码时，都以本节环境为基础。

### Redis 服务准备

开发环境可以使用本地 Redis、Docker Redis 或远程 Redis。为了降低环境差异，开发文档中建议优先使用 Docker 启动 Redis，便于团队成员快速复现。

使用 Docker 启动一个带密码的 Redis 实例：

```bash
# 创建 Redis 数据目录，用于持久化数据
mkdir -p ./docker/redis/data

# 启动 Redis 容器
docker run -d \
  --name redis-dev \
  -p 6379:6379 \
  -v ./docker/redis/data:/data \
  redis:7.2 \
  redis-server --appendonly yes --requirepass 123456
```

参数说明：

| 参数                           | 说明                                 |
| ------------------------------ | ------------------------------------ |
| `--name redis-dev`             | 指定容器名称                         |
| `-p 6379:6379`                 | 将容器 Redis 端口映射到宿主机        |
| `-v ./docker/redis/data:/data` | 挂载数据目录，避免容器删除后数据丢失 |
| `--appendonly yes`             | 开启 AOF 持久化                      |
| `--requirepass 123456`         | 设置 Redis 访问密码                  |

验证 Redis 是否可用：

```bash
# 进入 Redis 容器
docker exec -it redis-dev redis-cli

# 认证密码
AUTH 123456

# 写入测试数据
SET boot3:redis:test "ok"

# 读取测试数据
GET boot3:redis:test
```

如果返回 `"ok"`，说明 Redis 服务可正常访问。

Spring Boot 项目中的 Redis 连接配置建议放在 `src/main/resources/application.yml`：

```yaml
spring:
  data:
    redis:
      # Redis 服务地址
      host: 127.0.0.1
      # Redis 服务端口
      port: 6379
      # Redis 密码；本地无密码时可以删除该配置
      password: 123456
      # Redis 数据库索引，默认 0
      database: 0
      # 连接超时时间
      timeout: 3s
      lettuce:
        pool:
          # 最大连接数
          max-active: 16
          # 最大空闲连接数
          max-idle: 8
          # 最小空闲连接数
          min-idle: 2
          # 获取连接最大等待时间，-1 表示无限等待
          max-wait: 3s
```

Spring Boot 3 中 Redis 配置前缀推荐使用 `spring.data.redis`。如果项目从 Spring Boot 2 迁移而来，需要检查旧配置中的 `spring.redis` 是否仍被使用，避免连接配置不生效。

### Spring Boot 版本说明

本文档面向 Spring Boot 3 项目。Spring Boot 3 基于 Spring Framework 6，最低要求 Java 17；Spring Boot 官方当前文档也列出 Spring Boot 4.0.6 为默认最新主线，同时仍提供 Spring Boot 3.5.x 稳定版本文档入口。([Home](https://docs.spring.io/spring-boot/system-requirements.html))

推荐版本组合如下：

| 组件         | 推荐版本   | 说明                                                         |
| ------------ | ---------- | ------------------------------------------------------------ |
| JDK          | 17 或 21   | Spring Boot 3 最低要求 Java 17，生产环境建议使用 LTS 版本    |
| Spring Boot  | 3.5.x      | Spring Boot 3 当前维护线，适合新建 Spring Boot 3 项目        |
| Maven        | 3.6.3+     | Spring Boot 3.2+ 官方文档明确要求 Maven 3.6.3 或更高版本     |
| Redis        | 6.2+ / 7.x | 开发和生产建议使用 Redis 7.x                                 |
| Redis Client | Lettuce    | Spring Boot Redis Starter 默认使用 Lettuce，支持连接池和响应式能力 |

版本选择建议：

| 项目类型                    | 建议                                                         |
| --------------------------- | ------------------------------------------------------------ |
| 新项目                      | 使用 Spring Boot 3.5.x + JDK 21 或 JDK 17                    |
| 存量 Spring Boot 2 项目升级 | 先升级到 Spring Boot 2.7.x，再迁移到 Spring Boot 3.x         |
| 需要 Jakarta EE 兼容        | 使用 Spring Boot 3.x，因为包名已从 `javax.*` 迁移到 `jakarta.*` |
| 只需要 RedisTemplate        | 引入 `spring-boot-starter-data-redis` 即可                   |
| 需要 Redis 连接池           | 额外引入 `commons-pool2`                                     |
| 需要 JSON 序列化            | 使用 Spring Boot 默认管理的 Jackson 依赖即可                 |

### Maven 依赖配置

本节给出 Spring Boot 3 使用 `RedisTemplate` 的 Maven 依赖配置。一般情况下，只需要引入 `spring-boot-starter-data-redis`，不需要手动指定 Spring Data Redis、Lettuce、Jackson 等依赖版本，因为 Spring Boot Parent 会统一管理依赖版本。Spring Boot 官方依赖版本说明也指出，使用 Maven dependency management 时，未显式声明版本的依赖会使用 Spring Boot 管理的版本。([Home](https://docs.spring.io/spring-boot/appendix/dependency-versions/coordinates.html))

文件位置：`pom.xml`

```xml
<project xmlns="http://maven.apache.org/POM/4.0.0"
         xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
         xsi:schemaLocation="http://maven.apache.org/POM/4.0.0 https://maven.apache.org/xsd/maven-4.0.0.xsd">
    <modelVersion>4.0.0</modelVersion>

    <!-- Spring Boot 3 项目父依赖，统一管理 Spring、Jackson、Lettuce 等依赖版本 -->
    <parent>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-starter-parent</artifactId>
        <version>3.5.14</version>
        <relativePath/>
    </parent>

    <groupId>io.github.atengk</groupId>
    <artifactId>redis-template-demo</artifactId>
    <version>1.0.0</version>
    <name>redis-template-demo</name>
    <description>Spring Boot 3 RedisTemplate 示例项目</description>

    <properties>
        <!-- Spring Boot 3 最低要求 Java 17 -->
        <java.version>17</java.version>
        <!-- Hutool 工具类版本，用于字符串、集合、JSON 辅助处理 -->
        <hutool.version>5.8.40</hutool.version>
    </properties>

    <dependencies>
        <!-- Web 基础依赖：用于编写 RedisTemplate 测试接口 -->
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-web</artifactId>
        </dependency>

        <!-- Redis 核心依赖：包含 Spring Data Redis、RedisTemplate、Lettuce 客户端 -->
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-data-redis</artifactId>
        </dependency>

        <!-- Lettuce 连接池依赖：启用 spring.data.redis.lettuce.pool 配置时需要 -->
        <dependency>
            <groupId>org.apache.commons</groupId>
            <artifactId>commons-pool2</artifactId>
        </dependency>

        <!-- Hutool 工具类：用于字符串、集合、日期、JSON 等常用处理 -->
        <dependency>
            <groupId>cn.hutool</groupId>
            <artifactId>hutool-all</artifactId>
            <version>${hutool.version}</version>
        </dependency>

        <!-- Lombok：简化实体类、日志对象和构造器代码 -->
        <dependency>
            <groupId>org.projectlombok</groupId>
            <artifactId>lombok</artifactId>
            <optional>true</optional>
        </dependency>

        <!-- 测试依赖：用于单元测试和 Spring Boot 集成测试 -->
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-test</artifactId>
            <scope>test</scope>
        </dependency>
    </dependencies>

    <build>
        <plugins>
            <!-- Spring Boot Maven 插件：用于打包可执行 Jar -->
            <plugin>
                <groupId>org.springframework.boot</groupId>
                <artifactId>spring-boot-maven-plugin</artifactId>
            </plugin>
        </plugins>
    </build>
</project>
```

依赖说明：

| 依赖                             | 是否必需 | 说明                                                        |
| -------------------------------- | -------- | ----------------------------------------------------------- |
| `spring-boot-starter-data-redis` | 必需     | 提供 RedisTemplate、StringRedisTemplate、连接工厂和自动配置 |
| `commons-pool2`                  | 推荐     | 使用 Lettuce 连接池配置时需要                               |
| `spring-boot-starter-web`        | 可选     | 如果只写 Service 或测试类，可以不引入                       |
| `hutool-all`                     | 推荐     | 辅助处理字符串、集合、JSON、日期等常见逻辑                  |
| `lombok`                         | 可选     | 简化日志、构造器、Getter、Setter                            |
| `spring-boot-starter-test`       | 推荐     | 编写 RedisTemplate 集成测试                                 |

完成依赖配置后，可以执行以下命令验证依赖是否正常解析：

```bash
# 查看 Maven 版本，建议 3.6.3 或更高版本
mvn -v

# 下载依赖并编译项目
mvn clean compile

# 启动 Spring Boot 项目
mvn spring-boot:run
```

启动成功后，项目应能根据 `application.yml` 中的 `spring.data.redis` 配置自动创建 `RedisConnectionFactory`、`RedisTemplate` 和 `StringRedisTemplate`。后续章节可以在此基础上继续补充 `RedisTemplate` 序列化配置、常用操作封装、业务示例和测试接口。

## Redis 连接配置

本节用于定义 Spring Boot 3 项目连接 Redis 的基础配置，包括 Redis 地址、密码、数据库索引、连接超时、Lettuce 连接池以及多环境配置方式。后续 `RedisTemplate` Bean 会基于这些连接配置创建 Redis 访问入口。

### application.yml 配置

`application.yml` 建议只放通用配置和当前激活环境，不直接写死开发、测试、生产环境的 Redis 地址。具体环境参数放到 `application-dev.yml`、`application-test.yml`、`application-prod.yml` 中。

文件位置：`src/main/resources/application.yml`

```yaml
spring:
  profiles:
    # 当前激活环境，可通过启动参数 --spring.profiles.active=prod 覆盖
    active: dev

  application:
    # 应用名称
    name: redis-template-demo
```

开发环境 Redis 配置如下。

文件位置：`src/main/resources/application-dev.yml`

```yaml
spring:
  data:
    redis:
      # Redis 服务地址
      host: 127.0.0.1
      # Redis 服务端口
      port: 6379
      # Redis 密码，本地无密码时可以删除
      password: 123456
      # Redis 数据库索引，默认使用 0
      database: 0
      # Redis 命令执行超时时间
      timeout: 3s
      # Lettuce 客户端配置
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

如果 Redis 未设置密码，可以删除 `password` 配置，或者保持为空：

```yaml
spring:
  data:
    redis:
      host: 127.0.0.1
      port: 6379
      password:
      database: 0
      timeout: 3s
```

生产环境不建议在配置文件中明文写入密码，应优先通过环境变量、配置中心或密钥管理系统注入。

文件位置：`src/main/resources/application-prod.yml`

```yaml
spring:
  data:
    redis:
      # 生产 Redis 地址，建议通过环境变量注入
      host: ${REDIS_HOST}
      # 生产 Redis 端口
      port: ${REDIS_PORT:6379}
      # 生产 Redis 密码，必须通过环境变量或密钥系统注入
      password: ${REDIS_PASSWORD}
      # 生产环境建议明确指定数据库索引
      database: ${REDIS_DATABASE:0}
      # 生产环境建议设置明确超时时间，避免调用长时间阻塞
      timeout: ${REDIS_TIMEOUT:3s}
      lettuce:
        pool:
          # 最大连接数，根据接口并发量和 Redis 承载能力调整
          max-active: ${REDIS_POOL_MAX_ACTIVE:64}
          # 最大空闲连接数
          max-idle: ${REDIS_POOL_MAX_IDLE:16}
          # 最小空闲连接数
          min-idle: ${REDIS_POOL_MIN_IDLE:4}
          # 获取连接最大等待时间
          max-wait: ${REDIS_POOL_MAX_WAIT:3s}
```

启动生产环境时可以通过参数指定环境：

```bash
java -jar redis-template-demo.jar \
  --spring.profiles.active=prod \
  --REDIS_HOST=192.168.10.20 \
  --REDIS_PORT=6379 \
  --REDIS_PASSWORD=your-password \
  --REDIS_DATABASE=0
```

### 连接池配置

Spring Boot 3 默认使用 Lettuce 作为 Redis 客户端。Lettuce 本身是线程安全的，但在高并发场景中仍建议启用连接池，避免单连接被大量 Redis 命令长期占用。

使用连接池时，`pom.xml` 中需要引入 `commons-pool2`：

```xml
<!-- Lettuce 连接池依赖：启用 spring.data.redis.lettuce.pool 配置时需要 -->
<dependency>
    <groupId>org.apache.commons</groupId>
    <artifactId>commons-pool2</artifactId>
</dependency>
```

连接池配置示例：

```yaml
spring:
  data:
    redis:
      host: 127.0.0.1
      port: 6379
      password: 123456
      database: 0
      timeout: 3s
      lettuce:
        pool:
          # 最大连接数。并发较高时可适当调大，但不能超过 Redis 服务承载能力
          max-active: 32
          # 最大空闲连接数。过大可能浪费连接资源，过小可能频繁创建连接
          max-idle: 16
          # 最小空闲连接数。用于保持一定数量的可用连接
          min-idle: 4
          # 从连接池获取连接的最大等待时间，超时后抛出异常
          max-wait: 3s
```

常用参数说明：

| 参数         | 说明                 | 建议                                     |
| ------------ | -------------------- | ---------------------------------------- |
| `timeout`    | Redis 命令超时时间   | 一般设置为 `2s` 到 `5s`                  |
| `max-active` | 最大连接数           | 根据接口并发量、Redis QPS 和实例规格调整 |
| `max-idle`   | 最大空闲连接数       | 通常小于或等于 `max-active`              |
| `min-idle`   | 最小空闲连接数       | 保持少量空闲连接，降低突发请求延迟       |
| `max-wait`   | 获取连接最大等待时间 | 不建议无限等待，避免线程堆积             |

连接池不是越大越好。连接数过大可能导致 Redis 服务端连接过多、上下文切换增加，甚至影响整体吞吐。建议先使用中等配置，再结合压测结果和 Redis 监控指标调整。

### 多环境配置建议

多环境配置的目标是让本地开发、测试环境和生产环境互相隔离，避免误连生产 Redis，或者因为缓存 Key、数据库索引、密码配置混乱导致数据污染。

推荐按照以下方式组织配置文件：

```text
src/main/resources/
├── application.yml
├── application-dev.yml
├── application-test.yml
└── application-prod.yml
```

开发环境建议使用本地 Redis 或独立开发 Redis，并使用独立数据库索引：

```yaml
spring:
  data:
    redis:
      host: 127.0.0.1
      port: 6379
      password: 123456
      database: 0
```

测试环境建议使用测试 Redis 实例，避免与开发环境共用数据：

```yaml
spring:
  data:
    redis:
      host: redis-test.internal
      port: 6379
      password: ${REDIS_PASSWORD}
      database: 1
```

生产环境建议使用生产专用 Redis 实例，密码和地址通过环境变量注入：

```yaml
spring:
  data:
    redis:
      host: ${REDIS_HOST}
      port: ${REDIS_PORT:6379}
      password: ${REDIS_PASSWORD}
      database: ${REDIS_DATABASE:0}
```

多环境配置建议如下：

| 项目       | 建议                                                         |
| ---------- | ------------------------------------------------------------ |
| Redis 地址 | 每个环境使用独立 Redis 实例或独立集群                        |
| Redis 密码 | 不写死在生产配置文件中，使用环境变量或配置中心               |
| 数据库索引 | 开发、测试可以用不同 `database` 隔离；生产建议使用独立实例隔离 |
| Key 前缀   | 建议增加应用名、环境名、业务模块前缀                         |
| 超时时间   | 所有环境都必须配置，避免默认值不符合预期                     |
| 连接池     | 测试和生产必须显式配置，开发环境可使用较小连接池             |
| 日志       | 连接异常、序列化异常和缓存关键操作需要有日志                 |

Redis Key 命名建议：

```text
应用名:环境:业务模块:业务标识
```

示例：

```text
redis-template-demo:dev:user:10001
redis-template-demo:prod:user:10001
redis-template-demo:prod:captcha:login:18800001111
redis-template-demo:prod:dict:system_config
```

这种命名方式可以降低不同环境、不同业务模块之间的 Key 冲突概率，也便于后续排查缓存问题。

## RedisTemplate 核心配置

本节用于配置项目中的核心 `RedisTemplate<String, Object>` Bean，包括 Key、Value、Hash Key、Hash Value 的序列化方式。实际项目中不建议直接使用 Spring Boot 默认的 `RedisTemplate<Object, Object>`，而应显式定义序列化规则。

### RedisTemplate Bean 配置

`RedisTemplate` 的核心配置目标是统一 Redis Key 和 Value 的序列化方式。Key 推荐使用字符串序列化，Value 推荐使用 JSON 序列化。这样 Redis 中的数据可读性更好，也更方便排查问题。

建议创建独立配置类 `RedisTemplateConfig`。

文件位置：`src/main/java/io/github/atengk/config/RedisTemplateConfig.java`

下面的配置类定义了 `RedisTemplate<String, Object>`，统一配置 String Key 序列化和 JSON Value 序列化。

```java
package io.github.atengk.config;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.fasterxml.jackson.annotation.PropertyAccessor;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.databind.jsontype.BasicPolymorphicTypeValidator;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.serializer.GenericJackson2JsonRedisSerializer;
import org.springframework.data.redis.serializer.RedisSerializer;
import org.springframework.data.redis.serializer.StringRedisSerializer;

/**
 * RedisTemplate 核心配置
 *
 * @author Ateng
 * @since 2026-05-06
 */
@Slf4j
@Configuration
public class RedisTemplateConfig {

    /**
     * 配置 RedisTemplate Bean
     *
     * @param redisConnectionFactory Redis 连接工厂
     * @return RedisTemplate 实例
     */
    @Bean
    public RedisTemplate<String, Object> redisTemplate(RedisConnectionFactory redisConnectionFactory) {
        RedisTemplate<String, Object> redisTemplate = new RedisTemplate<>();

        // 设置 Redis 连接工厂
        redisTemplate.setConnectionFactory(redisConnectionFactory);

        // Key 使用字符串序列化，便于 Redis 控制台直接查看
        StringRedisSerializer stringRedisSerializer = new StringRedisSerializer();

        // Value 使用 JSON 序列化，便于跨语言、可读性和排查问题
        RedisSerializer<Object> jsonRedisSerializer = this.jsonRedisSerializer();

        // 普通 Key 序列化
        redisTemplate.setKeySerializer(stringRedisSerializer);
        // 普通 Value 序列化
        redisTemplate.setValueSerializer(jsonRedisSerializer);

        // Hash Key 序列化
        redisTemplate.setHashKeySerializer(stringRedisSerializer);
        // Hash Value 序列化
        redisTemplate.setHashValueSerializer(jsonRedisSerializer);

        // 默认序列化器，兜底处理未显式指定的序列化场景
        redisTemplate.setDefaultSerializer(jsonRedisSerializer);

        // 初始化 RedisTemplate 配置
        redisTemplate.afterPropertiesSet();

        log.info("RedisTemplate 初始化完成，Key 使用 String 序列化，Value 使用 JSON 序列化");
        return redisTemplate;
    }

    /**
     * 构建 JSON Redis 序列化器
     *
     * @return JSON Redis 序列化器
     */
    private RedisSerializer<Object> jsonRedisSerializer() {
        ObjectMapper objectMapper = new ObjectMapper();

        // 支持 LocalDate、LocalDateTime 等 Java 8 时间类型
        objectMapper.registerModule(new JavaTimeModule());
        objectMapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);

        // 允许序列化非 public 字段
        objectMapper.setVisibility(PropertyAccessor.ALL, JsonAutoDetect.Visibility.ANY);

        // 限制反序列化类型范围，降低反序列化风险
        BasicPolymorphicTypeValidator typeValidator = BasicPolymorphicTypeValidator.builder()
                .allowIfSubType("io.github.atengk")
                .allowIfSubType("java.util")
                .allowIfSubType("java.time")
                .build();

        // 写入类型信息，便于 Object 类型反序列化恢复实际对象类型
        objectMapper.activateDefaultTyping(
                typeValidator,
                ObjectMapper.DefaultTyping.NON_FINAL,
                JsonTypeInfo.As.PROPERTY
        );

        return new GenericJackson2JsonRedisSerializer(objectMapper);
    }
}
```

该配置完成后，项目中可以直接注入：

```java
private final RedisTemplate<String, Object> redisTemplate;
```

如果项目同时需要操作纯字符串，可以继续使用 Spring Boot 自动配置的 `StringRedisTemplate`：

```java
private final StringRedisTemplate stringRedisTemplate;
```

`RedisTemplate<String, Object>` 适合保存对象、集合、Hash 数据等复杂结构；`StringRedisTemplate` 适合保存纯字符串、计数器、简单标记等数据。

### Key 序列化配置

Redis Key 推荐统一使用 `StringRedisSerializer`。这样 Redis 中的 Key 是普通字符串，便于使用 `redis-cli`、RedisInsight、运维平台直接查看和检索。

配置位置在 `RedisTemplateConfig` 中：

```java
StringRedisSerializer stringRedisSerializer = new StringRedisSerializer();

redisTemplate.setKeySerializer(stringRedisSerializer);
redisTemplate.setHashKeySerializer(stringRedisSerializer);
```

推荐的 Key 示例：

```text
redis-template-demo:dev:user:10001
redis-template-demo:dev:captcha:login:18800001111
redis-template-demo:dev:order:pay:lock:202605060001
```

不推荐使用 JDK 序列化作为 Key 序列化方式，因为序列化后的 Key 不是普通字符串，无法在 Redis 客户端中直观看到真实 Key 名称，也不利于排查问题。

错误示例：

```text
\xac\xed\x00\x05t\x00\x1dredis-template-demo:dev:user:10001
```

正确示例：

```text
redis-template-demo:dev:user:10001
```

实际项目中可以封装一个 Key 构建工具类，统一维护 Redis Key 规则，避免业务代码到处拼接字符串。

文件位置：`src/main/java/io/github/atengk/constant/RedisKeyConstants.java`

下面的常量类用于统一维护 Redis Key 前缀和常用 Key 构建方法。

```java
package io.github.atengk.constant;

import cn.hutool.core.text.CharSequenceUtil;

/**
 * Redis Key 常量
 *
 * @author Ateng
 * @since 2026-05-06
 */
public final class RedisKeyConstants {

    /**
     * 应用名称
     */
    public static final String APP_NAME = "redis-template-demo";

    /**
     * 默认环境
     */
    public static final String DEFAULT_ENV = "dev";

    /**
     * 用户缓存 Key 前缀
     */
    public static final String USER_CACHE_PREFIX = APP_NAME + ":" + DEFAULT_ENV + ":user:";

    /**
     * 登录验证码 Key 前缀
     */
    public static final String LOGIN_CAPTCHA_PREFIX = APP_NAME + ":" + DEFAULT_ENV + ":captcha:login:";

    private RedisKeyConstants() {
    }

    /**
     * 构建用户缓存 Key
     *
     * @param userId 用户 ID
     * @return 用户缓存 Key
     */
    public static String userCacheKey(Long userId) {
        return CharSequenceUtil.format("{}{}", USER_CACHE_PREFIX, userId);
    }

    /**
     * 构建登录验证码 Key
     *
     * @param phone 手机号
     * @return 登录验证码 Key
     */
    public static String loginCaptchaKey(String phone) {
        return CharSequenceUtil.format("{}{}", LOGIN_CAPTCHA_PREFIX, phone);
    }
}
```

### Value 序列化配置

Redis Value 推荐使用 JSON 序列化。相比 JDK 序列化，JSON 具有更好的可读性、跨语言能力和排查便利性。

配置位置在 `RedisTemplateConfig` 中：

```java
RedisSerializer<Object> jsonRedisSerializer = this.jsonRedisSerializer();

redisTemplate.setValueSerializer(jsonRedisSerializer);
redisTemplate.setDefaultSerializer(jsonRedisSerializer);
```

保存对象后的 Redis Value 示例：

```json
{
  "@class": "io.github.atengk.vo.UserCacheVO",
  "userId": 10001,
  "nickname": "Ateng",
  "status": 1,
  "createTime": "2026-05-06 10:30:00"
}
```

为了验证 Value 序列化效果，可以增加一个简单的缓存对象。

文件位置：`src/main/java/io/github/atengk/vo/UserCacheVO.java`

下面的 VO 类用于演示 RedisTemplate 保存和读取对象数据。

```java
package io.github.atengk.vo;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 用户缓存 VO
 *
 * @author Ateng
 * @since 2026-05-06
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class UserCacheVO {

    /**
     * 用户 ID
     */
    private Long userId;

    /**
     * 用户昵称
     */
    private String nickname;

    /**
     * 用户状态
     */
    private Integer status;

    /**
     * 创建时间
     */
    private LocalDateTime createTime;
}
```

文件位置：`src/main/java/io/github/atengk/service/RedisValueDemoService.java`

下面的 Service 演示使用 RedisTemplate 保存、读取和删除普通对象缓存。

```java
package io.github.atengk.service;

import cn.hutool.core.util.ObjectUtil;
import io.github.atengk.constant.RedisKeyConstants;
import io.github.atengk.vo.UserCacheVO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.LocalDateTime;

/**
 * Redis Value 操作示例服务
 *
 * @author Ateng
 * @since 2026-05-06
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class RedisValueDemoService {

    private final RedisTemplate<String, Object> redisTemplate;

    /**
     * 保存用户缓存
     *
     * @param userId 用户 ID
     */
    public void saveUserCache(Long userId) {
        String key = RedisKeyConstants.userCacheKey(userId);
        UserCacheVO userCache = new UserCacheVO(userId, "Ateng", 1, LocalDateTime.now());

        redisTemplate.opsForValue().set(key, userCache, Duration.ofMinutes(30));

        log.info("用户缓存写入成功，key={}", key);
    }

    /**
     * 获取用户缓存
     *
     * @param userId 用户 ID
     * @return 用户缓存
     */
    public UserCacheVO getUserCache(Long userId) {
        String key = RedisKeyConstants.userCacheKey(userId);
        Object value = redisTemplate.opsForValue().get(key);

        if (ObjectUtil.isNull(value)) {
            log.info("用户缓存不存在，key={}", key);
            return null;
        }

        if (value instanceof UserCacheVO userCache) {
            log.info("用户缓存读取成功，key={}", key);
            return userCache;
        }

        log.warn("用户缓存类型不匹配，key={}，valueType={}", key, value.getClass().getName());
        return null;
    }

    /**
     * 删除用户缓存
     *
     * @param userId 用户 ID
     * @return 是否删除成功
     */
    public Boolean deleteUserCache(Long userId) {
        String key = RedisKeyConstants.userCacheKey(userId);
        Boolean result = redisTemplate.delete(key);

        log.info("用户缓存删除完成，key={}，result={}", key, result);
        return result;
    }
}
```

### Hash 序列化配置

Redis Hash 适合保存对象的多个字段，例如用户信息、系统配置、统计指标等。Hash Key 推荐使用字符串序列化，Hash Value 推荐使用 JSON 序列化。

配置位置在 `RedisTemplateConfig` 中：

```java
redisTemplate.setHashKeySerializer(stringRedisSerializer);
redisTemplate.setHashValueSerializer(jsonRedisSerializer);
```

Hash 的 Redis 结构示例：

```text
Key: redis-template-demo:dev:user:hash:10001

HashKey: nickname
HashValue: "Ateng"

HashKey: status
HashValue: 1

HashKey: profile
HashValue: {"@class":"io.github.atengk.vo.UserCacheVO", ...}
```

如果 Hash Value 只保存字符串或数字，Redis 客户端中可读性最好。如果 Hash Value 保存复杂对象，则仍由 JSON 序列化器处理。

文件位置：`src/main/java/io/github/atengk/service/RedisHashDemoService.java`

下面的 Service 演示使用 RedisTemplate 操作 Redis Hash，包括写入字段、读取字段、批量读取和删除字段。

```java
package io.github.atengk.service;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.ObjectUtil;
import io.github.atengk.vo.UserCacheVO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.Map;

/**
 * Redis Hash 操作示例服务
 *
 * @author Ateng
 * @since 2026-05-06
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class RedisHashDemoService {

    private static final String USER_HASH_KEY_PREFIX = "redis-template-demo:dev:user:hash:";

    private final RedisTemplate<String, Object> redisTemplate;

    /**
     * 写入用户 Hash 缓存
     *
     * @param userId 用户 ID
     */
    public void saveUserHash(Long userId) {
        String key = USER_HASH_KEY_PREFIX + userId;

        redisTemplate.opsForHash().put(key, "userId", userId);
        redisTemplate.opsForHash().put(key, "nickname", "Ateng");
        redisTemplate.opsForHash().put(key, "status", 1);
        redisTemplate.opsForHash().put(key, "profile", new UserCacheVO(userId, "Ateng", 1, LocalDateTime.now()));

        log.info("用户 Hash 缓存写入成功，key={}", key);
    }

    /**
     * 读取用户昵称
     *
     * @param userId 用户 ID
     * @return 用户昵称
     */
    public String getNickname(Long userId) {
        String key = USER_HASH_KEY_PREFIX + userId;
        Object value = redisTemplate.opsForHash().get(key, "nickname");

        if (ObjectUtil.isNull(value)) {
            log.info("用户昵称缓存不存在，key={}", key);
            return null;
        }

        return value.toString();
    }

    /**
     * 批量读取用户 Hash
     *
     * @param userId 用户 ID
     * @return 用户 Hash 数据
     */
    public Map<Object, Object> getUserHash(Long userId) {
        String key = USER_HASH_KEY_PREFIX + userId;
        Map<Object, Object> entries = redisTemplate.opsForHash().entries(key);

        if (CollUtil.isEmpty(entries)) {
            log.info("用户 Hash 缓存为空，key={}", key);
            return Map.of();
        }

        log.info("用户 Hash 缓存读取成功，key={}，fieldCount={}", key, entries.size());
        return entries;
    }

    /**
     * 删除用户 Hash 字段
     *
     * @param userId 用户 ID
     * @param field  字段名称
     * @return 删除字段数量
     */
    public Long deleteHashField(Long userId, String field) {
        String key = USER_HASH_KEY_PREFIX + userId;
        Long count = redisTemplate.opsForHash().delete(key, field);

        log.info("用户 Hash 字段删除完成，key={}，field={}，count={}", key, field, count);
        return count;
    }
}
```

序列化配置总结：

| 配置项             | 推荐序列化器                         | 原因                     |
| ------------------ | ------------------------------------ | ------------------------ |
| Key                | `StringRedisSerializer`              | Key 可读，方便排查和运维 |
| Value              | `GenericJackson2JsonRedisSerializer` | 对象 JSON 化，可读性好   |
| Hash Key           | `StringRedisSerializer`              | Hash 字段名可读          |
| Hash Value         | `GenericJackson2JsonRedisSerializer` | 支持复杂对象             |
| Default Serializer | JSON 序列化器                        | 兜底处理未显式指定的场景 |

需要注意的是，JSON 序列化虽然可读性好，但反序列化时需要关注类型信息和安全边界。业务对象建议放在明确的业务包路径下，例如 `io.github.atengk`，不要开放过宽的反序列化类型范围。

## 常用数据类型操作

本节用于说明 `RedisTemplate` 对 Redis 常用数据结构的操作方式。以下示例默认已经完成前面章节中的 `RedisTemplate<String, Object>` Bean 配置，并且 Value、Hash Value 使用 JSON 序列化。

### String 类型操作

String 是 Redis 最常用的数据类型，适合保存验证码、登录状态、用户缓存、接口结果缓存、计数器等数据。通过 `redisTemplate.opsForValue()` 可以完成写入、读取、递增、递减和设置过期时间等操作。

常用方法如下：

| 方法                       | 说明                |
| -------------------------- | ------------------- |
| `set(key, value)`          | 写入字符串或对象    |
| `set(key, value, timeout)` | 写入并设置过期时间  |
| `get(key)`                 | 根据 Key 获取 Value |
| `increment(key)`           | 自增 1              |
| `increment(key, delta)`    | 按指定步长自增      |
| `decrement(key)`           | 自减 1              |
| `delete(key)`              | 删除 Key            |

文件位置：`src/main/java/io/github/atengk/service/RedisStringDemoService.java`

下面的 Service 演示 String 类型的对象缓存、验证码缓存和计数器操作。

```java
package io.github.atengk.service;

import cn.hutool.core.util.ObjectUtil;
import io.github.atengk.constant.RedisKeyConstants;
import io.github.atengk.vo.UserCacheVO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.LocalDateTime;

/**
 * Redis String 类型操作示例
 *
 * @author Ateng
 * @since 2026-05-06
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class RedisStringDemoService {

    private final RedisTemplate<String, Object> redisTemplate;

    /**
     * 保存用户对象缓存
     *
     * @param userId 用户 ID
     */
    public void saveUserCache(Long userId) {
        String key = RedisKeyConstants.userCacheKey(userId);
        UserCacheVO userCache = new UserCacheVO(userId, "Ateng", 1, LocalDateTime.now());

        redisTemplate.opsForValue().set(key, userCache, Duration.ofMinutes(30));

        log.info("String 用户缓存写入成功，key={}", key);
    }

    /**
     * 获取用户对象缓存
     *
     * @param userId 用户 ID
     * @return 用户缓存对象
     */
    public UserCacheVO getUserCache(Long userId) {
        String key = RedisKeyConstants.userCacheKey(userId);
        Object value = redisTemplate.opsForValue().get(key);

        if (ObjectUtil.isNull(value)) {
            log.info("String 用户缓存不存在，key={}", key);
            return null;
        }

        if (value instanceof UserCacheVO userCache) {
            return userCache;
        }

        log.warn("String 用户缓存类型不匹配，key={}，valueType={}", key, value.getClass().getName());
        return null;
    }

    /**
     * 保存登录验证码
     *
     * @param phone 手机号
     * @param code  验证码
     */
    public void saveLoginCode(String phone, String code) {
        String key = RedisKeyConstants.loginCaptchaKey(phone);

        redisTemplate.opsForValue().set(key, code, Duration.ofMinutes(5));

        log.info("登录验证码写入成功，key={}", key);
    }

    /**
     * 增加接口访问次数
     *
     * @param apiCode 接口编码
     * @return 当前访问次数
     */
    public Long incrementApiCount(String apiCode) {
        String key = "redis-template-demo:dev:api:count:" + apiCode;
        Long count = redisTemplate.opsForValue().increment(key, 1);

        redisTemplate.expire(key, Duration.ofDays(1));

        log.info("接口访问次数递增成功，key={}，count={}", key, count);
        return count;
    }
}
```

String 类型使用建议：

| 场景       | 建议                                       |
| ---------- | ------------------------------------------ |
| 验证码     | 必须设置较短 TTL，例如 5 分钟              |
| 登录 Token | 必须设置 TTL，并根据业务决定是否续期       |
| 计数器     | 使用 `increment`，不要先 `get` 再 `set`    |
| 对象缓存   | 使用 JSON 序列化，避免 JDK 序列化          |
| 热点缓存   | 建议增加随机过期偏移，避免同一时间大量失效 |

### Hash 类型操作

Hash 适合保存对象的多个字段，例如用户资料、系统配置、商品基础信息等。相比直接把整个对象作为 String 保存，Hash 可以单独更新某个字段，适合字段级缓存场景。

常用方法如下：

| 方法                       | 说明               |
| -------------------------- | ------------------ |
| `put(key, hashKey, value)` | 写入 Hash 字段     |
| `putAll(key, map)`         | 批量写入 Hash 字段 |
| `get(key, hashKey)`        | 获取单个 Hash 字段 |
| `entries(key)`             | 获取整个 Hash      |
| `delete(key, hashKey)`     | 删除指定 Hash 字段 |
| `hasKey(key, hashKey)`     | 判断字段是否存在   |

文件位置：`src/main/java/io/github/atengk/service/RedisHashDemoService.java`

下面的 Service 演示 Hash 类型的字段写入、批量写入、读取和删除。

```java
package io.github.atengk.service;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.map.MapUtil;
import cn.hutool.core.util.ObjectUtil;
import io.github.atengk.vo.UserCacheVO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.Map;

/**
 * Redis Hash 类型操作示例
 *
 * @author Ateng
 * @since 2026-05-06
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class RedisHashDemoService {

    private static final String USER_HASH_KEY_PREFIX = "redis-template-demo:dev:user:hash:";

    private final RedisTemplate<String, Object> redisTemplate;

    /**
     * 写入用户 Hash 缓存
     *
     * @param userId 用户 ID
     */
    public void saveUserHash(Long userId) {
        String key = USER_HASH_KEY_PREFIX + userId;

        Map<String, Object> userMap = MapUtil.<String, Object>builder()
                .put("userId", userId)
                .put("nickname", "Ateng")
                .put("status", 1)
                .put("profile", new UserCacheVO(userId, "Ateng", 1, LocalDateTime.now()))
                .build();

        redisTemplate.opsForHash().putAll(key, userMap);

        log.info("Hash 用户缓存写入成功，key={}，fieldCount={}", key, userMap.size());
    }

    /**
     * 获取用户昵称
     *
     * @param userId 用户 ID
     * @return 用户昵称
     */
    public String getNickname(Long userId) {
        String key = USER_HASH_KEY_PREFIX + userId;
        Object value = redisTemplate.opsForHash().get(key, "nickname");

        if (ObjectUtil.isNull(value)) {
            log.info("Hash 用户昵称不存在，key={}", key);
            return null;
        }

        return value.toString();
    }

    /**
     * 获取用户完整 Hash 数据
     *
     * @param userId 用户 ID
     * @return 用户 Hash 数据
     */
    public Map<Object, Object> getUserHash(Long userId) {
        String key = USER_HASH_KEY_PREFIX + userId;
        Map<Object, Object> entries = redisTemplate.opsForHash().entries(key);

        if (CollUtil.isEmpty(entries)) {
            log.info("Hash 用户缓存为空，key={}", key);
            return Map.of();
        }

        return entries;
    }

    /**
     * 删除 Hash 字段
     *
     * @param userId 用户 ID
     * @param field  字段名称
     * @return 删除数量
     */
    public Long deleteField(Long userId, String field) {
        String key = USER_HASH_KEY_PREFIX + userId;
        Long count = redisTemplate.opsForHash().delete(key, field);

        log.info("Hash 字段删除完成，key={}，field={}，count={}", key, field, count);
        return count;
    }
}
```

Hash 类型使用建议：

| 场景                 | 建议                                                       |
| -------------------- | ---------------------------------------------------------- |
| 对象字段频繁局部更新 | 优先使用 Hash                                              |
| 整个对象整体读写     | String + JSON 更简单                                       |
| Hash 字段名          | 使用字符串，例如 `nickname`、`status`                      |
| Hash Value           | 简单值和复杂对象都可以，但要统一序列化                     |
| 过期时间             | Redis Hash 只能对整个 Key 设置 TTL，不能对单个字段设置 TTL |

### List 类型操作

List 是有序列表，适合保存消息队列、最近访问记录、操作日志列表等数据。通过 `leftPush`、`rightPush`、`leftPop`、`rightPop` 可以实现队列或栈结构。

常用方法如下：

| 方法                     | 说明         |
| ------------------------ | ------------ |
| `leftPush(key, value)`   | 从左侧写入   |
| `rightPush(key, value)`  | 从右侧写入   |
| `leftPop(key)`           | 从左侧弹出   |
| `rightPop(key)`          | 从右侧弹出   |
| `range(key, start, end)` | 查询指定范围 |
| `trim(key, start, end)`  | 裁剪列表     |
| `size(key)`              | 获取列表长度 |

文件位置：`src/main/java/io/github/atengk/service/RedisListDemoService.java`

下面的 Service 演示 List 类型的队列写入、弹出、查询和保留最近记录。

```java
package io.github.atengk.service;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.ObjectUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.List;

/**
 * Redis List 类型操作示例
 *
 * @author Ateng
 * @since 2026-05-06
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class RedisListDemoService {

    private static final String NOTICE_QUEUE_KEY = "redis-template-demo:dev:queue:notice";

    private static final String RECENT_VIEW_KEY_PREFIX = "redis-template-demo:dev:user:recent:view:";

    private final RedisTemplate<String, Object> redisTemplate;

    /**
     * 写入通知队列
     *
     * @param message 通知消息
     */
    public void pushNotice(String message) {
        Long size = redisTemplate.opsForList().rightPush(NOTICE_QUEUE_KEY, message);

        log.info("通知消息写入队列成功，key={}，size={}", NOTICE_QUEUE_KEY, size);
    }

    /**
     * 弹出通知队列
     *
     * @return 通知消息
     */
    public String popNotice() {
        Object value = redisTemplate.opsForList().leftPop(NOTICE_QUEUE_KEY);

        if (ObjectUtil.isNull(value)) {
            log.info("通知队列为空，key={}", NOTICE_QUEUE_KEY);
            return null;
        }

        return value.toString();
    }

    /**
     * 保存用户最近访问商品
     *
     * @param userId    用户 ID
     * @param productId 商品 ID
     */
    public void saveRecentView(Long userId, Long productId) {
        String key = RECENT_VIEW_KEY_PREFIX + userId;

        redisTemplate.opsForList().leftPush(key, productId);
        redisTemplate.opsForList().trim(key, 0, 19);
        redisTemplate.expire(key, Duration.ofDays(7));

        log.info("用户最近访问商品写入成功，key={}，productId={}", key, productId);
    }

    /**
     * 查询用户最近访问商品
     *
     * @param userId 用户 ID
     * @return 商品 ID 列表
     */
    public List<Object> listRecentView(Long userId) {
        String key = RECENT_VIEW_KEY_PREFIX + userId;
        List<Object> values = redisTemplate.opsForList().range(key, 0, 19);

        if (CollUtil.isEmpty(values)) {
            log.info("用户最近访问商品为空，key={}", key);
            return List.of();
        }

        return values;
    }
}
```

List 类型使用建议：

| 场景         | 建议                                                        |
| ------------ | ----------------------------------------------------------- |
| 简单队列     | 使用 `rightPush` + `leftPop`                                |
| 最近记录     | 使用 `leftPush` + `trim`                                    |
| 列表长度控制 | 必须使用 `trim`，避免无限增长                               |
| 可靠消息     | 不建议只用 Redis List，需要考虑消息丢失、确认机制和重试机制 |
| 大列表查询   | 避免一次 `range(0, -1)` 拉取过多数据                        |

### Set 类型操作

Set 是无序去重集合，适合保存标签、权限编码、用户点赞记录、已处理 ID 等不需要排序但需要去重的数据。

常用方法如下：

| 方法                        | 说明             |
| --------------------------- | ---------------- |
| `add(key, values)`          | 添加元素         |
| `remove(key, values)`       | 删除元素         |
| `members(key)`              | 获取全部元素     |
| `isMember(key, value)`      | 判断元素是否存在 |
| `size(key)`                 | 获取集合大小     |
| `intersect(key, otherKey)`  | 求交集           |
| `union(key, otherKey)`      | 求并集           |
| `difference(key, otherKey)` | 求差集           |

文件位置：`src/main/java/io/github/atengk/service/RedisSetDemoService.java`

下面的 Service 演示 Set 类型的标签保存、权限判断和点赞去重。

```java
package io.github.atengk.service;

import cn.hutool.core.collection.CollUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.Set;

/**
 * Redis Set 类型操作示例
 *
 * @author Ateng
 * @since 2026-05-06
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class RedisSetDemoService {

    private static final String USER_TAG_KEY_PREFIX = "redis-template-demo:dev:user:tag:";

    private static final String ARTICLE_LIKE_KEY_PREFIX = "redis-template-demo:dev:article:like:";

    private final RedisTemplate<String, Object> redisTemplate;

    /**
     * 保存用户标签
     *
     * @param userId 用户 ID
     * @param tags   标签集合
     */
    public void saveUserTags(Long userId, Set<String> tags) {
        String key = USER_TAG_KEY_PREFIX + userId;

        if (CollUtil.isEmpty(tags)) {
            log.info("用户标签为空，跳过写入，key={}", key);
            return;
        }

        Long count = redisTemplate.opsForSet().add(key, tags.toArray());
        redisTemplate.expire(key, Duration.ofDays(30));

        log.info("用户标签写入成功，key={}，count={}", key, count);
    }

    /**
     * 查询用户标签
     *
     * @param userId 用户 ID
     * @return 标签集合
     */
    public Set<Object> listUserTags(Long userId) {
        String key = USER_TAG_KEY_PREFIX + userId;
        Set<Object> tags = redisTemplate.opsForSet().members(key);

        if (CollUtil.isEmpty(tags)) {
            return Set.of();
        }

        return tags;
    }

    /**
     * 点赞文章
     *
     * @param articleId 文章 ID
     * @param userId    用户 ID
     * @return 是否首次点赞
     */
    public Boolean likeArticle(Long articleId, Long userId) {
        String key = ARTICLE_LIKE_KEY_PREFIX + articleId;
        Boolean liked = redisTemplate.opsForSet().isMember(key, userId);

        if (Boolean.TRUE.equals(liked)) {
            log.info("用户已点赞，key={}，userId={}", key, userId);
            return false;
        }

        redisTemplate.opsForSet().add(key, userId);
        redisTemplate.expire(key, Duration.ofDays(90));

        log.info("文章点赞成功，key={}，userId={}", key, userId);
        return true;
    }

    /**
     * 判断用户是否已点赞文章
     *
     * @param articleId 文章 ID
     * @param userId    用户 ID
     * @return 是否已点赞
     */
    public Boolean hasLiked(Long articleId, Long userId) {
        String key = ARTICLE_LIKE_KEY_PREFIX + articleId;
        return redisTemplate.opsForSet().isMember(key, userId);
    }
}
```

Set 类型使用建议：

| 场景       | 建议                             |
| ---------- | -------------------------------- |
| 去重数据   | 优先使用 Set                     |
| 标签、权限 | 使用 Set 存储，判断效率较高      |
| 点赞记录   | 使用 `isMember` 判断是否重复点赞 |
| 大集合     | 避免频繁 `members` 全量读取      |
| 排序需求   | 如果需要按分值排序，应使用 ZSet  |

### ZSet 类型操作

ZSet 是有序集合，每个元素都有一个分值 Score，适合排行榜、热度榜、权重排序、延迟任务基础结构等场景。

常用方法如下：

| 方法                                      | 说明               |
| ----------------------------------------- | ------------------ |
| `add(key, value, score)`                  | 添加元素和分值     |
| `score(key, value)`                       | 获取元素分值       |
| `incrementScore(key, value, delta)`       | 增加元素分值       |
| `reverseRange(key, start, end)`           | 按分值从高到低查询 |
| `range(key, start, end)`                  | 按分值从低到高查询 |
| `reverseRangeWithScores(key, start, end)` | 查询元素和分值     |
| `remove(key, values)`                     | 删除元素           |
| `rank(key, value)`                        | 获取升序排名       |
| `reverseRank(key, value)`                 | 获取降序排名       |

文件位置：`src/main/java/io/github/atengk/service/RedisZSetDemoService.java`

下面的 Service 演示 ZSet 类型的文章热度排行榜操作。

```java
package io.github.atengk.service;

import cn.hutool.core.collection.CollUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ZSetOperations;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.Set;

/**
 * Redis ZSet 类型操作示例
 *
 * @author Ateng
 * @since 2026-05-06
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class RedisZSetDemoService {

    private static final String ARTICLE_HOT_RANK_KEY = "redis-template-demo:dev:rank:article:hot";

    private final RedisTemplate<String, Object> redisTemplate;

    /**
     * 增加文章热度分
     *
     * @param articleId 文章 ID
     * @param score     增加分值
     * @return 增加后的分值
     */
    public Double incrementArticleScore(Long articleId, double score) {
        Double currentScore = redisTemplate.opsForZSet().incrementScore(ARTICLE_HOT_RANK_KEY, articleId, score);
        redisTemplate.expire(ARTICLE_HOT_RANK_KEY, Duration.ofDays(7));

        log.info("文章热度分增加成功，key={}，articleId={}，score={}", ARTICLE_HOT_RANK_KEY, articleId, currentScore);
        return currentScore;
    }

    /**
     * 查询文章热度 Top N
     *
     * @param size 查询数量
     * @return 文章 ID 集合
     */
    public Set<Object> listTopArticles(long size) {
        Set<Object> values = redisTemplate.opsForZSet().reverseRange(ARTICLE_HOT_RANK_KEY, 0, size - 1);

        if (CollUtil.isEmpty(values)) {
            log.info("文章热度榜为空，key={}", ARTICLE_HOT_RANK_KEY);
            return Set.of();
        }

        return values;
    }

    /**
     * 查询文章热度 Top N，并返回分值
     *
     * @param size 查询数量
     * @return 文章和分值集合
     */
    public Set<ZSetOperations.TypedTuple<Object>> listTopArticlesWithScore(long size) {
        Set<ZSetOperations.TypedTuple<Object>> tuples =
                redisTemplate.opsForZSet().reverseRangeWithScores(ARTICLE_HOT_RANK_KEY, 0, size - 1);

        if (CollUtil.isEmpty(tuples)) {
            return Set.of();
        }

        return tuples;
    }

    /**
     * 查询文章排名
     *
     * @param articleId 文章 ID
     * @return 排名，从 1 开始
     */
    public Long getArticleRank(Long articleId) {
        Long rank = redisTemplate.opsForZSet().reverseRank(ARTICLE_HOT_RANK_KEY, articleId);

        if (rank == null) {
            log.info("文章未进入热度榜，key={}，articleId={}", ARTICLE_HOT_RANK_KEY, articleId);
            return null;
        }

        return rank + 1;
    }
}
```

ZSet 类型使用建议：

| 场景       | 建议                                             |
| ---------- | ------------------------------------------------ |
| 排行榜     | 使用 ZSet，Score 保存排序分值                    |
| 热度排序   | 使用 `incrementScore` 动态增加分值               |
| Top N 查询 | 使用 `reverseRange` 或 `reverseRangeWithScores`  |
| 排名查询   | 使用 `reverseRank`，返回值从 0 开始              |
| 分值设计   | 建议统一分值规则，例如浏览 +1、点赞 +5、评论 +10 |

## 业务封装设计

本节用于说明如何在实际项目中封装 Redis 操作。业务代码不建议到处直接注入 `RedisTemplate` 并手写 Key、TTL 和序列化处理，而应通过统一工具类、Key 命名规范和过期时间管理降低维护成本。

### Redis 工具类封装

Redis 工具类用于集中封装常用操作，避免业务层重复调用 `redisTemplate.opsForXxx()`。工具类不应该封装所有 Redis 命令，而应该覆盖项目中最常用、最稳定的操作。

推荐文件结构如下：

```text
src/main/java/io/github/atengk/
├── common/redis/RedisCacheUtil.java
├── common/redis/RedisKeyBuilder.java
└── common/redis/RedisTtlConstants.java
```

文件位置：`src/main/java/io/github/atengk/common/redis/RedisCacheUtil.java`

下面的工具类封装 String、Hash、List、Set、ZSet 的常用操作，并对空 Key、空集合、类型转换和日志进行统一处理。

```java
package io.github.atengk.common.redis;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.convert.Convert;
import cn.hutool.core.util.ObjectUtil;
import cn.hutool.core.util.RandomUtil;
import cn.hutool.core.util.StrUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ZSetOperations;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Redis 缓存工具类
 *
 * @author Ateng
 * @since 2026-05-06
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class RedisCacheUtil {

    private final RedisTemplate<String, Object> redisTemplate;

    /**
     * 写入缓存
     *
     * @param key   缓存 Key
     * @param value 缓存值
     */
    public void set(String key, Object value) {
        checkKey(key);
        redisTemplate.opsForValue().set(key, value);
    }

    /**
     * 写入缓存并设置过期时间
     *
     * @param key     缓存 Key
     * @param value   缓存值
     * @param timeout 过期时间
     */
    public void set(String key, Object value, Duration timeout) {
        checkKey(key);

        if (ObjectUtil.isNull(timeout) || timeout.isZero() || timeout.isNegative()) {
            redisTemplate.opsForValue().set(key, value);
            return;
        }

        redisTemplate.opsForValue().set(key, value, timeout);
    }

    /**
     * 写入缓存并设置随机过期偏移
     *
     * @param key           缓存 Key
     * @param value         缓存值
     * @param baseTimeout   基础过期时间
     * @param jitterSeconds 随机偏移秒数
     */
    public void setWithJitter(String key, Object value, Duration baseTimeout, long jitterSeconds) {
        checkKey(key);

        long randomSeconds = jitterSeconds > 0 ? RandomUtil.randomLong(0, jitterSeconds) : 0;
        Duration timeout = baseTimeout.plusSeconds(randomSeconds);

        redisTemplate.opsForValue().set(key, value, timeout);
    }

    /**
     * 读取缓存
     *
     * @param key   缓存 Key
     * @param clazz 目标类型
     * @param <T>   目标泛型
     * @return 缓存值
     */
    public <T> T get(String key, Class<T> clazz) {
        checkKey(key);
        Object value = redisTemplate.opsForValue().get(key);

        if (ObjectUtil.isNull(value)) {
            return null;
        }

        if (clazz.isInstance(value)) {
            return clazz.cast(value);
        }

        try {
            return Convert.convert(clazz, value);
        } catch (Exception e) {
            log.warn("Redis 缓存类型转换失败，key={}，sourceType={}，targetType={}",
                    key, value.getClass().getName(), clazz.getName());
            return null;
        }
    }

    /**
     * 删除缓存
     *
     * @param key 缓存 Key
     * @return 是否删除成功
     */
    public Boolean delete(String key) {
        checkKey(key);
        return redisTemplate.delete(key);
    }

    /**
     * 批量删除缓存
     *
     * @param keys 缓存 Key 集合
     * @return 删除数量
     */
    public Long delete(Collection<String> keys) {
        if (CollUtil.isEmpty(keys)) {
            return 0L;
        }

        return redisTemplate.delete(keys);
    }

    /**
     * 设置过期时间
     *
     * @param key     缓存 Key
     * @param timeout 过期时间
     * @return 是否设置成功
     */
    public Boolean expire(String key, Duration timeout) {
        checkKey(key);

        if (ObjectUtil.isNull(timeout) || timeout.isZero() || timeout.isNegative()) {
            log.warn("Redis 过期时间非法，key={}，timeout={}", key, timeout);
            return false;
        }

        return redisTemplate.expire(key, timeout);
    }

    /**
     * 获取剩余过期时间，单位秒
     *
     * @param key 缓存 Key
     * @return 剩余秒数
     */
    public Long getExpire(String key) {
        checkKey(key);
        return redisTemplate.getExpire(key);
    }

    /**
     * 自增
     *
     * @param key   缓存 Key
     * @param delta 自增步长
     * @return 自增后的值
     */
    public Long increment(String key, long delta) {
        checkKey(key);
        return redisTemplate.opsForValue().increment(key, delta);
    }

    /**
     * 写入 Hash 字段
     *
     * @param key     缓存 Key
     * @param hashKey Hash 字段
     * @param value   字段值
     */
    public void hPut(String key, String hashKey, Object value) {
        checkKey(key);
        checkKey(hashKey);
        redisTemplate.opsForHash().put(key, hashKey, value);
    }

    /**
     * 读取 Hash 字段
     *
     * @param key     缓存 Key
     * @param hashKey Hash 字段
     * @param clazz   目标类型
     * @param <T>     目标泛型
     * @return 字段值
     */
    public <T> T hGet(String key, String hashKey, Class<T> clazz) {
        checkKey(key);
        checkKey(hashKey);

        Object value = redisTemplate.opsForHash().get(key, hashKey);
        if (ObjectUtil.isNull(value)) {
            return null;
        }

        if (clazz.isInstance(value)) {
            return clazz.cast(value);
        }

        try {
            return Convert.convert(clazz, value);
        } catch (Exception e) {
            log.warn("Redis Hash 字段类型转换失败，key={}，hashKey={}，targetType={}", key, hashKey, clazz.getName());
            return null;
        }
    }

    /**
     * 获取 Hash 全部字段
     *
     * @param key 缓存 Key
     * @return Hash 字段 Map
     */
    public Map<Object, Object> hEntries(String key) {
        checkKey(key);
        return redisTemplate.opsForHash().entries(key);
    }

    /**
     * 删除 Hash 字段
     *
     * @param key      缓存 Key
     * @param hashKeys Hash 字段
     * @return 删除数量
     */
    public Long hDelete(String key, Object... hashKeys) {
        checkKey(key);
        return redisTemplate.opsForHash().delete(key, hashKeys);
    }

    /**
     * 从右侧写入 List
     *
     * @param key   缓存 Key
     * @param value 元素值
     * @return 写入后的列表长度
     */
    public Long lRightPush(String key, Object value) {
        checkKey(key);
        return redisTemplate.opsForList().rightPush(key, value);
    }

    /**
     * 查询 List 范围
     *
     * @param key   缓存 Key
     * @param start 开始索引
     * @param end   结束索引
     * @return 元素列表
     */
    public List<Object> lRange(String key, long start, long end) {
        checkKey(key);
        List<Object> values = redisTemplate.opsForList().range(key, start, end);

        if (CollUtil.isEmpty(values)) {
            return List.of();
        }

        return values;
    }

    /**
     * 从左侧弹出 List
     *
     * @param key 缓存 Key
     * @return 元素值
     */
    public Object lLeftPop(String key) {
        checkKey(key);
        return redisTemplate.opsForList().leftPop(key);
    }

    /**
     * 添加 Set 元素
     *
     * @param key    缓存 Key
     * @param values 元素值
     * @return 添加数量
     */
    public Long sAdd(String key, Object... values) {
        checkKey(key);
        return redisTemplate.opsForSet().add(key, values);
    }

    /**
     * 查询 Set 全部元素
     *
     * @param key 缓存 Key
     * @return 元素集合
     */
    public Set<Object> sMembers(String key) {
        checkKey(key);
        Set<Object> values = redisTemplate.opsForSet().members(key);

        if (CollUtil.isEmpty(values)) {
            return Set.of();
        }

        return values;
    }

    /**
     * 判断 Set 元素是否存在
     *
     * @param key   缓存 Key
     * @param value 元素值
     * @return 是否存在
     */
    public Boolean sIsMember(String key, Object value) {
        checkKey(key);
        return redisTemplate.opsForSet().isMember(key, value);
    }

    /**
     * 添加 ZSet 元素
     *
     * @param key   缓存 Key
     * @param value 元素值
     * @param score 分值
     * @return 是否添加成功
     */
    public Boolean zAdd(String key, Object value, double score) {
        checkKey(key);
        return redisTemplate.opsForZSet().add(key, value, score);
    }

    /**
     * 增加 ZSet 分值
     *
     * @param key   缓存 Key
     * @param value 元素值
     * @param delta 分值增量
     * @return 增加后的分值
     */
    public Double zIncrementScore(String key, Object value, double delta) {
        checkKey(key);
        return redisTemplate.opsForZSet().incrementScore(key, value, delta);
    }

    /**
     * 查询 ZSet 降序范围
     *
     * @param key   缓存 Key
     * @param start 开始索引
     * @param end   结束索引
     * @return 元素集合
     */
    public Set<Object> zReverseRange(String key, long start, long end) {
        checkKey(key);
        Set<Object> values = redisTemplate.opsForZSet().reverseRange(key, start, end);

        if (CollUtil.isEmpty(values)) {
            return Set.of();
        }

        return values;
    }

    /**
     * 查询 ZSet 降序范围和分值
     *
     * @param key   缓存 Key
     * @param start 开始索引
     * @param end   结束索引
     * @return 元素和分值集合
     */
    public Set<ZSetOperations.TypedTuple<Object>> zReverseRangeWithScores(String key, long start, long end) {
        checkKey(key);
        Set<ZSetOperations.TypedTuple<Object>> values = redisTemplate.opsForZSet().reverseRangeWithScores(key, start, end);

        if (CollUtil.isEmpty(values)) {
            return Set.of();
        }

        return values;
    }

    /**
     * 校验 Redis Key
     *
     * @param key Redis Key
     */
    private void checkKey(String key) {
        if (StrUtil.isBlank(key)) {
            throw new IllegalArgumentException("Redis Key 不能为空");
        }
    }
}
```

业务代码中使用工具类时，只关心 Key、Value 和业务动作，不需要重复处理底层操作。

文件位置：`src/main/java/io/github/atengk/service/UserCacheService.java`

下面的 Service 演示业务层通过 `RedisCacheUtil` 操作用户缓存。

```java
package io.github.atengk.service;

import io.github.atengk.common.redis.RedisCacheUtil;
import io.github.atengk.common.redis.RedisKeyBuilder;
import io.github.atengk.common.redis.RedisTtlConstants;
import io.github.atengk.vo.UserCacheVO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

/**
 * 用户缓存服务
 *
 * @author Ateng
 * @since 2026-05-06
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class UserCacheService {

    private final RedisCacheUtil redisCacheUtil;

    private final RedisKeyBuilder redisKeyBuilder;

    /**
     * 保存用户缓存
     *
     * @param userId 用户 ID
     */
    public void saveUserCache(Long userId) {
        String key = redisKeyBuilder.build("user", "info", userId);
        UserCacheVO userCache = new UserCacheVO(userId, "Ateng", 1, LocalDateTime.now());

        redisCacheUtil.setWithJitter(key, userCache, RedisTtlConstants.USER_CACHE_TTL, 300);

        log.info("用户缓存保存成功，key={}", key);
    }

    /**
     * 查询用户缓存
     *
     * @param userId 用户 ID
     * @return 用户缓存
     */
    public UserCacheVO getUserCache(Long userId) {
        String key = redisKeyBuilder.build("user", "info", userId);
        return redisCacheUtil.get(key, UserCacheVO.class);
    }

    /**
     * 删除用户缓存
     *
     * @param userId 用户 ID
     * @return 是否删除成功
     */
    public Boolean deleteUserCache(Long userId) {
        String key = redisKeyBuilder.build("user", "info", userId);
        Boolean result = redisCacheUtil.delete(key);

        log.info("用户缓存删除完成，key={}，result={}", key, result);
        return result;
    }
}
```

### Key 命名规范

Redis Key 命名规范用于解决 Key 冲突、环境隔离、业务定位和运维排查问题。项目中不要随意拼接 Key，建议通过统一构建器生成。

推荐格式如下：

```text
应用名:环境:业务模块:业务类型:业务标识
```

示例：

```text
redis-template-demo:dev:user:info:10001
redis-template-demo:test:captcha:login:18800001111
redis-template-demo:prod:article:like:90001
redis-template-demo:prod:rank:article:hot
redis-template-demo:prod:queue:notice
```

命名规则说明：

| 片段     | 说明                 | 示例                              |
| -------- | -------------------- | --------------------------------- |
| 应用名   | 区分不同应用         | `redis-template-demo`             |
| 环境     | 区分开发、测试、生产 | `dev`、`test`、`prod`             |
| 业务模块 | 区分业务域           | `user`、`order`、`article`        |
| 业务类型 | 区分缓存用途         | `info`、`lock`、`rank`、`captcha` |
| 业务标识 | 具体业务 ID          | `10001`、手机号、订单号           |

文件位置：`src/main/java/io/github/atengk/common/redis/RedisKeyBuilder.java`

下面的构建器统一生成 Redis Key，避免业务代码直接手写 Key。

```java
package io.github.atengk.common.redis;

import cn.hutool.core.util.StrUtil;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.Arrays;
import java.util.Objects;
import java.util.stream.Collectors;

/**
 * Redis Key 构建器
 *
 * @author Ateng
 * @since 2026-05-06
 */
@Component
public class RedisKeyBuilder {

    @Value("${spring.application.name:redis-template-demo}")
    private String applicationName;

    @Value("${spring.profiles.active:dev}")
    private String activeProfile;

    /**
     * 构建 Redis Key
     *
     * @param module   业务模块
     * @param type     业务类型
     * @param segments 业务标识片段
     * @return Redis Key
     */
    public String build(String module, String type, Object... segments) {
        if (StrUtil.isBlank(module)) {
            throw new IllegalArgumentException("Redis Key 业务模块不能为空");
        }
        if (StrUtil.isBlank(type)) {
            throw new IllegalArgumentException("Redis Key 业务类型不能为空");
        }

        String prefix = StrUtil.join(":", applicationName, activeProfile, module, type);
        String suffix = Arrays.stream(segments)
                .filter(Objects::nonNull)
                .map(Object::toString)
                .filter(StrUtil::isNotBlank)
                .collect(Collectors.joining(":"));

        if (StrUtil.isBlank(suffix)) {
            return prefix;
        }

        return StrUtil.join(":", prefix, suffix);
    }
}
```

业务中使用示例：

```java
String userKey = redisKeyBuilder.build("user", "info", 10001);
String captchaKey = redisKeyBuilder.build("captcha", "login", "18800001111");
String rankKey = redisKeyBuilder.build("rank", "article", "hot");
String lockKey = redisKeyBuilder.build("order", "pay:lock", "202605060001");
```

Key 命名注意事项：

| 规则                   | 说明                                   |
| ---------------------- | -------------------------------------- |
| 不使用空格             | 避免客户端和脚本处理异常               |
| 不使用中文             | 便于跨系统、跨语言和运维工具处理       |
| 不使用过长 Key         | Key 过长会增加内存消耗                 |
| 不直接拼接生产环境 Key | 必须通过环境标识隔离                   |
| 不使用模糊前缀         | 例如 `data:1`、`cache:1` 可读性差      |
| 不在生产使用 `KEYS *`  | 大量 Key 时会阻塞 Redis，应使用 `SCAN` |

### 过期时间管理

过期时间管理用于控制缓存生命周期，避免缓存长期占用 Redis 内存，也避免大量 Key 同一时间失效造成缓存雪崩。业务缓存默认应设置 TTL，只有明确需要长期保存的数据才不设置过期时间。

推荐按照业务类型统一定义 TTL 常量。

文件位置：`src/main/java/io/github/atengk/common/redis/RedisTtlConstants.java`

下面的常量类统一维护常见缓存过期时间。

```java
package io.github.atengk.common.redis;

import java.time.Duration;

/**
 * Redis 过期时间常量
 *
 * @author Ateng
 * @since 2026-05-06
 */
public final class RedisTtlConstants {

    /**
     * 登录验证码过期时间
     */
    public static final Duration LOGIN_CAPTCHA_TTL = Duration.ofMinutes(5);

    /**
     * 用户缓存过期时间
     */
    public static final Duration USER_CACHE_TTL = Duration.ofMinutes(30);

    /**
     * 字典缓存过期时间
     */
    public static final Duration DICT_CACHE_TTL = Duration.ofHours(6);

    /**
     * 接口限流计数过期时间
     */
    public static final Duration RATE_LIMIT_TTL = Duration.ofMinutes(1);

    /**
     * 排行榜缓存过期时间
     */
    public static final Duration RANK_CACHE_TTL = Duration.ofDays(7);

    /**
     * 幂等标记过期时间
     */
    public static final Duration IDEMPOTENT_TTL = Duration.ofMinutes(10);

    private RedisTtlConstants() {
    }
}
```

不同业务的 TTL 建议如下：

| 业务类型     | 建议 TTL        | 说明                       |
| ------------ | --------------- | -------------------------- |
| 验证码       | 3 到 5 分钟     | 时间过长会增加安全风险     |
| 登录状态     | 30 分钟到 7 天  | 根据登录策略决定是否续期   |
| 用户信息缓存 | 10 到 60 分钟   | 数据变更时主动删除或更新   |
| 字典配置     | 1 到 24 小时    | 可配合后台刷新             |
| 接口结果缓存 | 30 秒到 10 分钟 | 根据数据实时性决定         |
| 排行榜       | 1 小时到 7 天   | 根据统计周期决定           |
| 限流计数     | 1 秒到 1 分钟   | 与限流窗口保持一致         |
| 分布式锁     | 数秒到数分钟    | 必须设置过期时间，避免死锁 |
| 幂等标记     | 5 到 30 分钟    | 覆盖业务重试窗口即可       |

TTL 管理建议：

| 建议                   | 说明                                 |
| ---------------------- | ------------------------------------ |
| 缓存必须有默认 TTL     | 防止无用 Key 长期占用内存            |
| 热点缓存增加随机偏移   | 避免大量 Key 同时过期                |
| 不同业务使用不同 TTL   | 不要所有缓存统一 30 分钟             |
| 数据变更时主动删除缓存 | 避免只依赖 TTL 导致脏读时间过长      |
| 空值缓存设置短 TTL     | 防止缓存穿透，同时避免长期缓存空结果 |
| 锁 Key 必须设置 TTL    | 防止业务异常导致锁无法释放           |

保存热点缓存时建议使用随机过期偏移：

```java
redisCacheUtil.setWithJitter(
        key,
        userCache,
        RedisTtlConstants.USER_CACHE_TTL,
        300
);
```

这里的 `300` 表示在基础 TTL 上增加 `0` 到 `300` 秒的随机偏移。例如基础过期时间是 30 分钟，最终过期时间会分布在 30 到 35 分钟之间，从而降低同一批缓存同时过期的风险。

空值缓存示例：

```java
redisCacheUtil.set(
        redisKeyBuilder.build("user", "info", userId),
        "NULL",
        Duration.ofMinutes(2)
);
```

空值缓存只适合短时间保护数据库，不能设置过长 TTL。读取时需要判断 `"NULL"` 标记，避免把空值当成正常业务数据使用。

## 典型业务场景

本节用于说明 `RedisTemplate` 在实际业务中的常见落地方式，包括缓存数据读写、验证码存储、基础分布式锁和计数器。以下示例默认已经完成前面章节中的 `RedisTemplate<String, Object>`、`RedisCacheUtil`、`RedisKeyBuilder` 和 `RedisTtlConstants` 配置。

### 缓存数据读写

缓存数据读写通常采用 Cache Aside 模式。业务读取数据时先查 Redis，缓存命中则直接返回；缓存未命中时查询数据库或远程接口，然后写入 Redis。数据更新时先更新数据库，再删除或更新 Redis 缓存。

典型读取流程如下：

```text
查询缓存 -> 缓存命中 -> 返回数据
查询缓存 -> 缓存未命中 -> 查询数据库 -> 写入缓存 -> 返回数据
```

典型更新流程如下：

```text
更新数据库 -> 删除缓存
```

不建议在更新数据库后立即写缓存，因为并发场景下容易出现旧数据覆盖新缓存的问题。更常见的做法是更新数据库后删除缓存，让下一次查询重新加载。

文件位置：`src/main/java/io/github/atengk/service/UserCacheBizService.java`

下面的 Service 演示用户信息缓存的读取、写入、删除和空值缓存处理。

```java
package io.github.atengk.service;

import cn.hutool.core.util.ObjectUtil;
import cn.hutool.core.util.StrUtil;
import io.github.atengk.common.redis.RedisCacheUtil;
import io.github.atengk.common.redis.RedisKeyBuilder;
import io.github.atengk.common.redis.RedisTtlConstants;
import io.github.atengk.vo.UserCacheVO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.LocalDateTime;

/**
 * 用户缓存业务服务
 *
 * @author Ateng
 * @since 2026-05-06
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class UserCacheBizService {

    private static final String NULL_VALUE = "NULL";

    private final RedisCacheUtil redisCacheUtil;

    private final RedisKeyBuilder redisKeyBuilder;

    /**
     * 查询用户信息，优先读取 Redis 缓存
     *
     * @param userId 用户 ID
     * @return 用户缓存信息
     */
    public UserCacheVO getUserInfo(Long userId) {
        String key = redisKeyBuilder.build("user", "info", userId);

        Object cacheValue = redisCacheUtil.get(key, Object.class);
        if (ObjectUtil.isNotNull(cacheValue)) {
            if (StrUtil.equals(NULL_VALUE, cacheValue.toString())) {
                log.info("用户空值缓存命中，key={}", key);
                return null;
            }

            if (cacheValue instanceof UserCacheVO userCacheVO) {
                log.info("用户缓存命中，key={}", key);
                return userCacheVO;
            }

            log.warn("用户缓存类型异常，key={}，valueType={}", key, cacheValue.getClass().getName());
            redisCacheUtil.delete(key);
        }

        UserCacheVO dbUser = this.getUserFromDb(userId);
        if (ObjectUtil.isNull(dbUser)) {
            redisCacheUtil.set(key, NULL_VALUE, Duration.ofMinutes(2));
            log.info("用户不存在，写入空值缓存，key={}", key);
            return null;
        }

        redisCacheUtil.setWithJitter(key, dbUser, RedisTtlConstants.USER_CACHE_TTL, 300);
        log.info("用户缓存重建完成，key={}", key);
        return dbUser;
    }

    /**
     * 更新用户信息后删除缓存
     *
     * @param userId   用户 ID
     * @param nickname 用户昵称
     */
    public void updateUserInfo(Long userId, String nickname) {
        this.updateUserToDb(userId, nickname);

        String key = redisKeyBuilder.build("user", "info", userId);
        redisCacheUtil.delete(key);

        log.info("用户信息更新成功，缓存已删除，key={}", key);
    }

    /**
     * 删除用户缓存
     *
     * @param userId 用户 ID
     */
    public void deleteUserCache(Long userId) {
        String key = redisKeyBuilder.build("user", "info", userId);
        redisCacheUtil.delete(key);

        log.info("用户缓存删除成功，key={}", key);
    }

    /**
     * 模拟从数据库查询用户
     *
     * @param userId 用户 ID
     * @return 用户信息
     */
    private UserCacheVO getUserFromDb(Long userId) {
        if (userId == null || userId <= 0) {
            return null;
        }

        return new UserCacheVO(userId, "Ateng", 1, LocalDateTime.now());
    }

    /**
     * 模拟更新数据库用户信息
     *
     * @param userId   用户 ID
     * @param nickname 用户昵称
     */
    private void updateUserToDb(Long userId, String nickname) {
        log.info("模拟更新数据库用户信息，userId={}，nickname={}", userId, nickname);
    }
}
```

缓存读写注意事项：

| 问题       | 处理方式                                 |
| ---------- | ---------------------------------------- |
| 缓存穿透   | 对不存在的数据写入短 TTL 空值缓存        |
| 缓存雪崩   | TTL 增加随机偏移，避免大量 Key 同时失效  |
| 缓存击穿   | 热点 Key 可加锁重建，或提前预热          |
| 数据一致性 | 更新数据库后删除缓存                     |
| 脏数据     | 关键业务不要只依赖缓存，缓存应作为加速层 |

### 验证码存储

验证码适合存储在 Redis String 中，Key 使用手机号、邮箱或业务流水号拼接，Value 保存验证码内容，TTL 通常设置为 3 到 5 分钟。验证码校验成功后应立即删除，避免重复使用。

验证码流程如下：

```text
生成验证码 -> 写入 Redis 并设置 TTL -> 发送验证码
用户提交验证码 -> 读取 Redis -> 比对成功 -> 删除验证码
```

文件位置：`src/main/java/io/github/atengk/service/CaptchaService.java`

下面的 Service 演示登录验证码的生成、保存、校验和删除。

```java
package io.github.atengk.service;

import cn.hutool.core.util.RandomUtil;
import cn.hutool.core.util.StrUtil;
import io.github.atengk.common.redis.RedisCacheUtil;
import io.github.atengk.common.redis.RedisKeyBuilder;
import io.github.atengk.common.redis.RedisTtlConstants;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

/**
 * 验证码服务
 *
 * @author Ateng
 * @since 2026-05-06
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class CaptchaService {

    private final RedisCacheUtil redisCacheUtil;

    private final RedisKeyBuilder redisKeyBuilder;

    /**
     * 生成登录验证码
     *
     * @param phone 手机号
     * @return 验证码
     */
    public String generateLoginCaptcha(String phone) {
        if (StrUtil.isBlank(phone)) {
            throw new IllegalArgumentException("手机号不能为空");
        }

        String code = RandomUtil.randomNumbers(6);
        String key = redisKeyBuilder.build("captcha", "login", phone);

        redisCacheUtil.set(key, code, RedisTtlConstants.LOGIN_CAPTCHA_TTL);

        log.info("登录验证码生成成功，key={}", key);
        return code;
    }

    /**
     * 校验登录验证码
     *
     * @param phone 手机号
     * @param code  用户输入验证码
     * @return 是否校验通过
     */
    public Boolean verifyLoginCaptcha(String phone, String code) {
        if (StrUtil.hasBlank(phone, code)) {
            return false;
        }

        String key = redisKeyBuilder.build("captcha", "login", phone);
        String cacheCode = redisCacheUtil.get(key, String.class);

        if (StrUtil.isBlank(cacheCode)) {
            log.info("登录验证码不存在或已过期，key={}", key);
            return false;
        }

        if (!StrUtil.equals(cacheCode, code)) {
            log.info("登录验证码校验失败，key={}", key);
            return false;
        }

        redisCacheUtil.delete(key);
        log.info("登录验证码校验成功，key={}", key);
        return true;
    }
}
```

验证码接口示例可以放在 Controller 中，便于本地验证。

文件位置：`src/main/java/io/github/atengk/controller/CaptchaController.java`

下面的 Controller 提供验证码生成和校验接口。

```java
package io.github.atengk.controller;

import io.github.atengk.service.CaptchaService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

/**
 * 验证码接口
 *
 * @author Ateng
 * @since 2026-05-06
 */
@RestController
@RequiredArgsConstructor
@RequestMapping("/captcha")
public class CaptchaController {

    private final CaptchaService captchaService;

    /**
     * 生成登录验证码
     *
     * @param phone 手机号
     * @return 验证码
     */
    @GetMapping("/login")
    public String generateLoginCaptcha(@RequestParam String phone) {
        return captchaService.generateLoginCaptcha(phone);
    }

    /**
     * 校验登录验证码
     *
     * @param phone 手机号
     * @param code  验证码
     * @return 是否通过
     */
    @PostMapping("/login/verify")
    public Boolean verifyLoginCaptcha(@RequestParam String phone, @RequestParam String code) {
        return captchaService.verifyLoginCaptcha(phone, code);
    }
}
```

接口验证命令：

```bash
# 生成验证码
curl "http://localhost:8080/captcha/login?phone=18800001111"

# 校验验证码，将 code 替换为上一步返回的验证码
curl -X POST "http://localhost:8080/captcha/login/verify?phone=18800001111&code=123456"
```

验证码存储注意事项：

| 项目     | 建议                                                         |
| -------- | ------------------------------------------------------------ |
| TTL      | 一般设置为 3 到 5 分钟                                       |
| 校验成功 | 必须删除验证码                                               |
| 校验失败 | 不建议删除验证码，可配合失败次数限制                         |
| Key 设计 | 应包含业务类型，例如 `captcha:login`、`captcha:reset-password` |
| 安全控制 | 生产环境需要增加发送频率限制和校验失败次数限制               |

### 分布式锁基础实现

分布式锁用于控制多个服务实例对同一资源的并发访问。基于 RedisTemplate 可以使用 `SET key value NX EX` 语义实现基础锁能力，对应到 Spring Data Redis 中就是 `setIfAbsent(key, value, timeout)`。

该实现适合演示基础原理和轻量场景。生产环境如果涉及锁续期、可重入锁、公平锁、读写锁、红锁或复杂并发控制，建议使用 Redisson。

基础加锁流程如下：

```text
生成唯一锁值 -> setIfAbsent 写入锁 Key 和 TTL -> 成功表示获得锁
```

基础释放锁流程如下：

```text
执行 Lua 脚本 -> 判断锁值是否一致 -> 一致则删除 -> 不一致则不删除
```

释放锁时不能直接 `delete(key)`，否则可能误删其他线程或其他实例持有的锁。必须先比较锁值，确认是当前持有者后再删除。

文件位置：`src/main/java/io/github/atengk/service/RedisLockService.java`

下面的 Service 使用 RedisTemplate 实现基础分布式锁，包括加锁、解锁和带锁执行业务逻辑。

```java
package io.github.atengk.service;

import cn.hutool.core.lang.UUID;
import cn.hutool.core.util.StrUtil;
import io.github.atengk.common.redis.RedisKeyBuilder;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.List;
import java.util.function.Supplier;

/**
 * Redis 分布式锁服务
 *
 * @author Ateng
 * @since 2026-05-06
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class RedisLockService {

    private static final Long RELEASE_SUCCESS = 1L;

    private final RedisTemplate<String, Object> redisTemplate;

    private final RedisKeyBuilder redisKeyBuilder;

    /**
     * 尝试加锁
     *
     * @param lockKey    锁 Key
     * @param lockValue  锁 Value
     * @param expireTime 锁过期时间
     * @return 是否加锁成功
     */
    public Boolean tryLock(String lockKey, String lockValue, Duration expireTime) {
        if (StrUtil.hasBlank(lockKey, lockValue)) {
            throw new IllegalArgumentException("锁 Key 和锁 Value 不能为空");
        }
        if (expireTime == null || expireTime.isZero() || expireTime.isNegative()) {
            throw new IllegalArgumentException("锁过期时间必须大于 0");
        }

        Boolean result = redisTemplate.opsForValue().setIfAbsent(lockKey, lockValue, expireTime);
        boolean locked = Boolean.TRUE.equals(result);

        log.info("Redis 分布式锁加锁结果，key={}，locked={}", lockKey, locked);
        return locked;
    }

    /**
     * 释放锁
     *
     * @param lockKey   锁 Key
     * @param lockValue 锁 Value
     * @return 是否释放成功
     */
    public Boolean unlock(String lockKey, String lockValue) {
        if (StrUtil.hasBlank(lockKey, lockValue)) {
            return false;
        }

        String luaScript = """
                if redis.call('get', KEYS[1]) == ARGV[1] then
                    return redis.call('del', KEYS[1])
                else
                    return 0
                end
                """;

        DefaultRedisScript<Long> redisScript = new DefaultRedisScript<>();
        redisScript.setScriptText(luaScript);
        redisScript.setResultType(Long.class);

        Long result = redisTemplate.execute(redisScript, List.of(lockKey), lockValue);
        boolean released = RELEASE_SUCCESS.equals(result);

        log.info("Redis 分布式锁释放结果，key={}，released={}", lockKey, released);
        return released;
    }

    /**
     * 带锁执行业务逻辑
     *
     * @param module     业务模块
     * @param bizId      业务 ID
     * @param expireTime 锁过期时间
     * @param supplier   业务逻辑
     * @param <T>        返回类型
     * @return 业务执行结果
     */
    public <T> T executeWithLock(String module, Object bizId, Duration expireTime, Supplier<T> supplier) {
        String lockKey = redisKeyBuilder.build(module, "lock", bizId);
        String lockValue = UUID.fastUUID().toString(true);

        Boolean locked = this.tryLock(lockKey, lockValue, expireTime);
        if (!Boolean.TRUE.equals(locked)) {
            throw new IllegalStateException("业务处理中，请稍后重试");
        }

        try {
            return supplier.get();
        } finally {
            this.unlock(lockKey, lockValue);
        }
    }
}
```

文件位置：`src/main/java/io/github/atengk/service/OrderPayService.java`

下面的 Service 演示在订单支付场景中使用 Redis 分布式锁防止重复处理。

```java
package io.github.atengk.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.Duration;

/**
 * 订单支付服务
 *
 * @author Ateng
 * @since 2026-05-06
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class OrderPayService {

    private final RedisLockService redisLockService;

    /**
     * 支付订单
     *
     * @param orderNo 订单编号
     * @return 支付结果
     */
    public String payOrder(String orderNo) {
        return redisLockService.executeWithLock("order:pay", orderNo, Duration.ofSeconds(10), () -> {
            log.info("开始处理订单支付，orderNo={}", orderNo);

            // 这里应执行真实业务逻辑：查询订单、校验状态、扣减余额、更新订单状态、发送消息等
            this.doPay(orderNo);

            log.info("订单支付处理完成，orderNo={}", orderNo);
            return "支付成功";
        });
    }

    /**
     * 模拟支付处理
     *
     * @param orderNo 订单编号
     */
    private void doPay(String orderNo) {
        log.info("模拟执行支付业务，orderNo={}", orderNo);
    }
}
```

分布式锁注意事项：

| 项目         | 建议                                 |
| ------------ | ------------------------------------ |
| 锁 Value     | 必须使用唯一值，例如 UUID            |
| 锁 TTL       | 必须设置，避免业务异常导致死锁       |
| 释放锁       | 必须比较 Value 后删除                |
| 业务耗时     | 锁 TTL 应大于正常业务耗时            |
| 自动续期     | RedisTemplate 基础实现不支持自动续期 |
| 生产复杂场景 | 建议使用 Redisson                    |

### 计数器实现

计数器适合统计接口访问次数、短信发送次数、登录失败次数、文章浏览量、点赞数等数据。Redis String 的 `increment` 操作是原子操作，适合高并发计数场景。

常见计数器类型如下：

| 类型         | 示例                                  |
| ------------ | ------------------------------------- |
| 日访问计数   | `api:count:daily:20260506:/user/list` |
| 用户失败次数 | `login:fail:18800001111`              |
| 文章浏览量   | `article:view:10001`                  |
| 短信发送次数 | `sms:send:18800001111`                |
| 限流窗口计数 | `rate-limit:login:18800001111`        |

文件位置：`src/main/java/io/github/atengk/service/RedisCounterService.java`

下面的 Service 演示普通计数器、带 TTL 计数器、登录失败次数和接口限流计数。

```java
package io.github.atengk.service;

import cn.hutool.core.date.DateUtil;
import cn.hutool.core.util.StrUtil;
import io.github.atengk.common.redis.RedisCacheUtil;
import io.github.atengk.common.redis.RedisKeyBuilder;
import io.github.atengk.common.redis.RedisTtlConstants;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.Date;

/**
 * Redis 计数器服务
 *
 * @author Ateng
 * @since 2026-05-06
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class RedisCounterService {

    private final RedisCacheUtil redisCacheUtil;

    private final RedisKeyBuilder redisKeyBuilder;

    /**
     * 增加文章浏览量
     *
     * @param articleId 文章 ID
     * @return 当前浏览量
     */
    public Long incrementArticleView(Long articleId) {
        String key = redisKeyBuilder.build("article", "view", articleId);
        Long count = redisCacheUtil.increment(key, 1);

        log.info("文章浏览量递增成功，key={}，count={}", key, count);
        return count;
    }

    /**
     * 增加每日接口访问次数
     *
     * @param apiCode 接口编码
     * @return 当前访问次数
     */
    public Long incrementDailyApiCount(String apiCode) {
        if (StrUtil.isBlank(apiCode)) {
            throw new IllegalArgumentException("接口编码不能为空");
        }

        String date = DateUtil.format(new Date(), "yyyyMMdd");
        String key = redisKeyBuilder.build("api", "daily-count", date, apiCode);

        Long count = redisCacheUtil.increment(key, 1);
        redisCacheUtil.expire(key, Duration.ofDays(2));

        log.info("每日接口访问次数递增成功，key={}，count={}", key, count);
        return count;
    }

    /**
     * 增加登录失败次数
     *
     * @param account 登录账号
     * @return 当前失败次数
     */
    public Long incrementLoginFailCount(String account) {
        String key = redisKeyBuilder.build("login", "fail", account);

        Long count = redisCacheUtil.increment(key, 1);
        redisCacheUtil.expire(key, Duration.ofMinutes(15));

        log.info("登录失败次数递增成功，key={}，count={}", key, count);
        return count;
    }

    /**
     * 判断登录失败次数是否超限
     *
     * @param account 登录账号
     * @return 是否超限
     */
    public Boolean isLoginFailLimited(String account) {
        String key = redisKeyBuilder.build("login", "fail", account);
        Long count = redisCacheUtil.get(key, Long.class);

        return count != null && count >= 5;
    }

    /**
     * 接口限流计数
     *
     * @param bizCode 业务编码
     * @param bizId   业务标识
     * @param limit   限制次数
     * @return 是否允许访问
     */
    public Boolean tryAccess(String bizCode, Object bizId, long limit) {
        String key = redisKeyBuilder.build("rate-limit", bizCode, bizId);

        Long count = redisCacheUtil.increment(key, 1);
        if (count != null && count == 1L) {
            redisCacheUtil.expire(key, RedisTtlConstants.RATE_LIMIT_TTL);
        }

        boolean allowed = count != null && count <= limit;
        log.info("接口限流计数完成，key={}，count={}，limit={}，allowed={}", key, count, limit, allowed);

        return allowed;
    }

    /**
     * 清理登录失败次数
     *
     * @param account 登录账号
     */
    public void clearLoginFailCount(String account) {
        String key = redisKeyBuilder.build("login", "fail", account);
        redisCacheUtil.delete(key);

        log.info("登录失败次数已清理，key={}", key);
    }
}
```

计数器接口可以用于快速验证计数效果。

文件位置：`src/main/java/io/github/atengk/controller/RedisCounterController.java`

下面的 Controller 提供文章浏览量和接口限流测试接口。

```java
package io.github.atengk.controller;

import io.github.atengk.service.RedisCounterService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

/**
 * Redis 计数器接口
 *
 * @author Ateng
 * @since 2026-05-06
 */
@RestController
@RequiredArgsConstructor
@RequestMapping("/redis/counter")
public class RedisCounterController {

    private final RedisCounterService redisCounterService;

    /**
     * 增加文章浏览量
     *
     * @param articleId 文章 ID
     * @return 当前浏览量
     */
    @PostMapping("/article/{articleId}/view")
    public Long incrementArticleView(@PathVariable Long articleId) {
        return redisCounterService.incrementArticleView(articleId);
    }

    /**
     * 测试接口限流
     *
     * @param account 账号
     * @return 是否允许访问
     */
    @PostMapping("/rate-limit/login")
    public Boolean tryLoginAccess(@RequestParam String account) {
        return redisCounterService.tryAccess("login", account, 5);
    }
}
```

接口验证命令：

```bash
# 文章浏览量递增
curl -X POST "http://localhost:8080/redis/counter/article/10001/view"

# 登录限流测试，连续请求超过 5 次后返回 false
curl -X POST "http://localhost:8080/redis/counter/rate-limit/login?account=18800001111"
```

计数器使用注意事项：

| 项目         | 建议                                    |
| ------------ | --------------------------------------- |
| 自增操作     | 使用 `increment`，不要 `get` 后再 `set` |
| 限流计数     | 第一次自增后设置 TTL                    |
| 浏览量统计   | 可先写 Redis，再异步同步数据库          |
| 登录失败次数 | 登录成功后应清理失败计数                |
| 短窗口限流   | 使用较短 TTL，例如 1 分钟               |
| 高精度限流   | 可使用 Lua、滑动窗口或令牌桶算法实现    |

## 测试与验证

本节用于验证 `RedisTemplate` 配置、序列化规则、业务封装和接口调用是否正常。建议按三个层次验证：先跑测试类确认 Spring Boot 能连接 Redis，再通过 HTTP 接口验证业务流程，最后使用 Redis 客户端查看真实 Key 和 Value。

### 单元测试

Redis 相关测试通常依赖真实 Redis 服务，因此更准确地说是 Spring Boot 集成测试。测试前需要确保本地 Redis 已启动，并且 `application-dev.yml` 中的 Redis 地址、端口、密码与实际环境一致。

测试依赖如果前面已经引入 `spring-boot-starter-test`，这里不需要重复添加：

```xml
<!-- 测试依赖：包含 JUnit 5、Spring Test、AssertJ 等测试组件 -->
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-test</artifactId>
    <scope>test</scope>
</dependency>
```

推荐测试文件结构如下：

```text
src/test/java/io/github/atengk/
├── RedisTemplateConnectionTest.java
├── RedisTemplateOperationTest.java
└── RedisBusinessScenarioTest.java
```

文件位置：`src/test/java/io/github/atengk/RedisTemplateConnectionTest.java`

下面的测试类用于验证 Spring Boot 是否可以正常连接 Redis，并完成基础读写。

```java
package io.github.atengk;

import cn.hutool.core.util.StrUtil;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.redis.connection.RedisConnection;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.test.context.ActiveProfiles;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Redis 连接测试
 *
 * @author Ateng
 * @since 2026-05-06
 */
@Slf4j
@SpringBootTest
@ActiveProfiles("dev")
class RedisTemplateConnectionTest {

    @Autowired
    private RedisConnectionFactory redisConnectionFactory;

    /**
     * 测试 Redis 连接是否正常
     */
    @Test
    void testRedisConnection() {
        try (RedisConnection connection = redisConnectionFactory.getConnection()) {
            String ping = connection.ping();

            log.info("Redis 连接测试完成，ping={}", ping);

            assertThat(StrUtil.equalsIgnoreCase("PONG", ping)).isTrue();
        }
    }
}
```

文件位置：`src/test/java/io/github/atengk/RedisTemplateOperationTest.java`

下面的测试类用于验证 String、Hash、List、Set、ZSet 的基础操作是否正常。

```java
package io.github.atengk;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.RandomUtil;
import io.github.atengk.vo.UserCacheVO;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.test.context.ActiveProfiles;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.Map;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * RedisTemplate 常用数据类型测试
 *
 * @author Ateng
 * @since 2026-05-06
 */
@Slf4j
@SpringBootTest
@ActiveProfiles("dev")
class RedisTemplateOperationTest {

    private static final String KEY_PREFIX = "redis-template-demo:test:operation:";

    @Autowired
    private RedisTemplate<String, Object> redisTemplate;

    /**
     * 清理测试数据
     */
    @AfterEach
    void cleanTestData() {
        Set<String> keys = redisTemplate.keys(KEY_PREFIX + "*");
        if (CollUtil.isNotEmpty(keys)) {
            redisTemplate.delete(keys);
            log.info("Redis 测试数据清理完成，count={}", keys.size());
        }
    }

    /**
     * 测试 String 对象缓存
     */
    @Test
    void testStringOperation() {
        String key = KEY_PREFIX + "string:user:" + RandomUtil.randomNumbers(6);
        UserCacheVO userCache = new UserCacheVO(10001L, "Ateng", 1, LocalDateTime.now());

        redisTemplate.opsForValue().set(key, userCache, Duration.ofMinutes(5));
        Object value = redisTemplate.opsForValue().get(key);

        log.info("String 类型测试完成，key={}，value={}", key, value);

        assertThat(value).isInstanceOf(UserCacheVO.class);
        assertThat(((UserCacheVO) value).getUserId()).isEqualTo(10001L);
    }

    /**
     * 测试 Hash 操作
     */
    @Test
    void testHashOperation() {
        String key = KEY_PREFIX + "hash:user:" + RandomUtil.randomNumbers(6);

        redisTemplate.opsForHash().putAll(key, Map.of(
                "userId", 10001L,
                "nickname", "Ateng",
                "status", 1
        ));

        Object nickname = redisTemplate.opsForHash().get(key, "nickname");
        Map<Object, Object> entries = redisTemplate.opsForHash().entries(key);

        log.info("Hash 类型测试完成，key={}，entries={}", key, entries);

        assertThat(nickname).isEqualTo("Ateng");
        assertThat(entries).containsKey("userId");
    }

    /**
     * 测试 List 操作
     */
    @Test
    void testListOperation() {
        String key = KEY_PREFIX + "list:notice:" + RandomUtil.randomNumbers(6);

        redisTemplate.opsForList().rightPush(key, "消息1");
        redisTemplate.opsForList().rightPush(key, "消息2");

        Object firstMessage = redisTemplate.opsForList().leftPop(key);
        Long size = redisTemplate.opsForList().size(key);

        log.info("List 类型测试完成，key={}，firstMessage={}，size={}", key, firstMessage, size);

        assertThat(firstMessage).isEqualTo("消息1");
        assertThat(size).isEqualTo(1L);
    }

    /**
     * 测试 Set 操作
     */
    @Test
    void testSetOperation() {
        String key = KEY_PREFIX + "set:tag:" + RandomUtil.randomNumbers(6);

        redisTemplate.opsForSet().add(key, "Java", "SpringBoot", "Redis");
        Boolean exists = redisTemplate.opsForSet().isMember(key, "Redis");
        Set<Object> members = redisTemplate.opsForSet().members(key);

        log.info("Set 类型测试完成，key={}，members={}", key, members);

        assertThat(exists).isTrue();
        assertThat(members).contains("Redis");
    }

    /**
     * 测试 ZSet 操作
     */
    @Test
    void testZSetOperation() {
        String key = KEY_PREFIX + "zset:rank:" + RandomUtil.randomNumbers(6);

        redisTemplate.opsForZSet().add(key, "article-10001", 10D);
        redisTemplate.opsForZSet().incrementScore(key, "article-10001", 5D);

        Double score = redisTemplate.opsForZSet().score(key, "article-10001");
        Long rank = redisTemplate.opsForZSet().reverseRank(key, "article-10001");

        log.info("ZSet 类型测试完成，key={}，score={}，rank={}", key, score, rank);

        assertThat(score).isEqualTo(15D);
        assertThat(rank).isEqualTo(0L);
    }
}
```

文件位置：`src/test/java/io/github/atengk/RedisBusinessScenarioTest.java`

下面的测试类用于验证业务封装后的缓存读写、验证码、分布式锁和计数器场景。

```java
package io.github.atengk;

import io.github.atengk.service.CaptchaService;
import io.github.atengk.service.RedisCounterService;
import io.github.atengk.service.RedisLockService;
import io.github.atengk.service.UserCacheBizService;
import io.github.atengk.vo.UserCacheVO;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import java.time.Duration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Redis 业务场景测试
 *
 * @author Ateng
 * @since 2026-05-06
 */
@Slf4j
@SpringBootTest
@ActiveProfiles("dev")
class RedisBusinessScenarioTest {

    @Autowired
    private UserCacheBizService userCacheBizService;

    @Autowired
    private CaptchaService captchaService;

    @Autowired
    private RedisLockService redisLockService;

    @Autowired
    private RedisCounterService redisCounterService;

    /**
     * 测试缓存数据读写
     */
    @Test
    void testUserCache() {
        Long userId = 10001L;

        UserCacheVO userInfo = userCacheBizService.getUserInfo(userId);
        UserCacheVO cachedUserInfo = userCacheBizService.getUserInfo(userId);

        log.info("用户缓存业务测试完成，userInfo={}，cachedUserInfo={}", userInfo, cachedUserInfo);

        assertThat(userInfo).isNotNull();
        assertThat(cachedUserInfo).isNotNull();
        assertThat(cachedUserInfo.getUserId()).isEqualTo(userId);
    }

    /**
     * 测试验证码生成和校验
     */
    @Test
    void testCaptcha() {
        String phone = "18800001111";

        String code = captchaService.generateLoginCaptcha(phone);
        Boolean verified = captchaService.verifyLoginCaptcha(phone, code);
        Boolean repeatVerified = captchaService.verifyLoginCaptcha(phone, code);

        log.info("验证码业务测试完成，phone={}，verified={}，repeatVerified={}", phone, verified, repeatVerified);

        assertThat(code).hasSize(6);
        assertThat(verified).isTrue();
        assertThat(repeatVerified).isFalse();
    }

    /**
     * 测试分布式锁
     */
    @Test
    void testRedisLock() {
        String lockKey = "redis-template-demo:test:lock:order:10001";
        String lockValue = "test-lock-value";

        Boolean firstLock = redisLockService.tryLock(lockKey, lockValue, Duration.ofSeconds(10));
        Boolean secondLock = redisLockService.tryLock(lockKey, "other-lock-value", Duration.ofSeconds(10));
        Boolean unlock = redisLockService.unlock(lockKey, lockValue);

        log.info("分布式锁业务测试完成，firstLock={}，secondLock={}，unlock={}", firstLock, secondLock, unlock);

        assertThat(firstLock).isTrue();
        assertThat(secondLock).isFalse();
        assertThat(unlock).isTrue();
    }

    /**
     * 测试分布式锁业务执行失败场景
     */
    @Test
    void testRedisLockConflict() {
        String lockKey = "redis-template-demo:test:lock:conflict";
        String lockValue = "test-lock-value";

        redisLockService.tryLock(lockKey, lockValue, Duration.ofSeconds(10));

        assertThatThrownBy(() -> redisLockService.executeWithLock("test", "conflict", Duration.ofSeconds(10), () -> "success"))
                .isInstanceOf(IllegalStateException.class);

        redisLockService.unlock(lockKey, lockValue);
    }

    /**
     * 测试计数器
     */
    @Test
    void testCounter() {
        Long count = redisCounterService.incrementArticleView(10001L);
        Boolean allowed = redisCounterService.tryAccess("test-login", "18800001111", 5);

        log.info("计数器业务测试完成，count={}，allowed={}", count, allowed);

        assertThat(count).isGreaterThanOrEqualTo(1L);
        assertThat(allowed).isTrue();
    }
}
```

执行测试命令：

```bash
# 确认 Redis 容器正在运行
docker ps | grep redis-dev

# 执行全部测试
mvn test

# 只执行 RedisTemplate 操作测试
mvn -Dtest=RedisTemplateOperationTest test

# 只执行业务场景测试
mvn -Dtest=RedisBusinessScenarioTest test
```

如果测试失败，优先检查 Redis 是否启动、密码是否正确、`spring.profiles.active` 是否使用了正确环境。

### 接口验证

接口验证用于确认 Controller、Service、RedisTemplate 和 Redis 服务之间的完整链路是否正常。建议先验证简单接口，再验证带 TTL、计数器、分布式锁等业务接口。

如果前面章节已经创建了 `CaptchaController` 和 `RedisCounterController`，可以直接使用 `curl` 验证。

启动项目：

```bash
# 使用 dev 环境启动项目
mvn spring-boot:run -Dspring-boot.run.profiles=dev
```

验证码接口验证：

```bash
# 生成登录验证码
curl "http://localhost:8080/captcha/login?phone=18800001111"

# 校验登录验证码，将 code 替换成上一步返回值
curl -X POST "http://localhost:8080/captcha/login/verify?phone=18800001111&code=123456"
```

计数器接口验证：

```bash
# 文章浏览量递增
curl -X POST "http://localhost:8080/redis/counter/article/10001/view"

# 登录限流测试，连续请求超过 5 次后应返回 false
curl -X POST "http://localhost:8080/redis/counter/rate-limit/login?account=18800001111"
```

为了验证缓存数据读写和分布式锁，可以增加一个统一测试接口。

文件位置：`src/main/java/io/github/atengk/controller/RedisScenarioController.java`

下面的 Controller 提供缓存查询、缓存更新和订单支付加锁测试接口。

```java
package io.github.atengk.controller;

import io.github.atengk.service.OrderPayService;
import io.github.atengk.service.UserCacheBizService;
import io.github.atengk.vo.UserCacheVO;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

/**
 * Redis 业务场景验证接口
 *
 * @author Ateng
 * @since 2026-05-06
 */
@RestController
@RequiredArgsConstructor
@RequestMapping("/redis/scenario")
public class RedisScenarioController {

    private final UserCacheBizService userCacheBizService;

    private final OrderPayService orderPayService;

    /**
     * 查询用户缓存
     *
     * @param userId 用户 ID
     * @return 用户信息
     */
    @GetMapping("/user/{userId}")
    public UserCacheVO getUserInfo(@PathVariable Long userId) {
        return userCacheBizService.getUserInfo(userId);
    }

    /**
     * 更新用户并删除缓存
     *
     * @param userId   用户 ID
     * @param nickname 用户昵称
     * @return 处理结果
     */
    @PostMapping("/user/{userId}")
    public String updateUserInfo(@PathVariable Long userId, @RequestParam String nickname) {
        userCacheBizService.updateUserInfo(userId, nickname);
        return "用户更新成功";
    }

    /**
     * 支付订单，内部使用 Redis 分布式锁
     *
     * @param orderNo 订单编号
     * @return 支付结果
     */
    @PostMapping("/order/pay")
    public String payOrder(@RequestParam String orderNo) {
        return orderPayService.payOrder(orderNo);
    }
}
```

缓存接口验证命令：

```bash
# 第一次查询，通常触发缓存重建
curl "http://localhost:8080/redis/scenario/user/10001"

# 第二次查询，通常命中 Redis 缓存
curl "http://localhost:8080/redis/scenario/user/10001"

# 更新用户，更新后删除缓存
curl -X POST "http://localhost:8080/redis/scenario/user/10001?nickname=AtengNew"

# 再次查询，重新加载缓存
curl "http://localhost:8080/redis/scenario/user/10001"
```

分布式锁接口验证命令：

```bash
# 正常支付请求
curl -X POST "http://localhost:8080/redis/scenario/order/pay?orderNo=PAY202605060001"

# 可以并发多次请求观察日志，确认同一订单不会重复进入核心处理逻辑
curl -X POST "http://localhost:8080/redis/scenario/order/pay?orderNo=PAY202605060001" &
curl -X POST "http://localhost:8080/redis/scenario/order/pay?orderNo=PAY202605060001" &
```

接口验证时重点观察以下内容：

| 验证项         | 预期结果                               |
| -------------- | -------------------------------------- |
| 项目启动       | 无 Redis 连接异常                      |
| 第一次缓存查询 | 日志出现缓存重建                       |
| 第二次缓存查询 | 日志出现缓存命中                       |
| 验证码校验成功 | 返回 `true`，并删除 Redis 中验证码 Key |
| 验证码重复校验 | 返回 `false`                           |
| 计数器接口     | 每次请求数值递增                       |
| 限流接口       | 超过限制次数后返回 `false`             |
| 分布式锁接口   | 并发时只有一个请求获得锁               |

### Redis 客户端查看数据

接口和测试通过后，应使用 Redis 客户端查看真实数据，确认 Key 命名、TTL、序列化格式和数据结构符合预期。可以使用 `redis-cli`、RedisInsight 或其他 Redis 管理工具。

进入 Redis 容器：

```bash
# 进入 Redis 容器的 redis-cli
docker exec -it redis-dev redis-cli

# 如果设置了密码，需要先认证
AUTH 123456
```

查看 Key：

```bash
# 不建议生产环境使用 KEYS，这里仅用于本地开发验证
KEYS redis-template-demo:dev:*

# 推荐使用 SCAN 分批扫描
SCAN 0 MATCH redis-template-demo:dev:* COUNT 100
```

查看 String 类型数据：

```bash
# 查看用户缓存
GET redis-template-demo:dev:user:info:10001

# 查看验证码
GET redis-template-demo:dev:captcha:login:18800001111

# 查看剩余过期时间，单位秒
TTL redis-template-demo:dev:captcha:login:18800001111
```

查看 Hash 类型数据：

```bash
# 查看 Hash 全部字段
HGETALL redis-template-demo:dev:user:hash:10001

# 查看指定 Hash 字段
HGET redis-template-demo:dev:user:hash:10001 nickname
```

查看 List 类型数据：

```bash
# 查看 List 长度
LLEN redis-template-demo:dev:queue:notice

# 查看 List 范围数据
LRANGE redis-template-demo:dev:queue:notice 0 -1
```

查看 Set 类型数据：

```bash
# 查看 Set 成员
SMEMBERS redis-template-demo:dev:article:like:10001

# 判断成员是否存在，返回 1 表示存在，0 表示不存在
SISMEMBER redis-template-demo:dev:article:like:10001 10086
```

查看 ZSet 类型数据：

```bash
# 查看排行榜，按分值从高到低
ZREVRANGE redis-template-demo:dev:rank:article:hot 0 9 WITHSCORES

# 查看指定元素分值
ZSCORE redis-template-demo:dev:rank:article:hot 10001
```

如果使用 `GenericJackson2JsonRedisSerializer`，Value 中通常会包含 `@class` 类型信息，这是为了让 `Object` 类型反序列化时能恢复为具体 Java 类型。如果不希望保存类型信息，需要改用明确类型的 Jackson 序列化策略，但这会增加泛型对象反序列化处理成本。

在终端查看中文或 JSON 时，可以使用 `--raw` 模式提升可读性：

```bash
# 使用 raw 模式进入 redis-cli，减少转义字符干扰
docker exec -it redis-dev redis-cli --raw

AUTH 123456

GET redis-template-demo:dev:user:info:10001
```

## 常见问题

本节整理 `RedisTemplate` 开发中最常见的问题，包括序列化异常、数据乱码和连接超时。排查顺序建议从配置开始，再看序列化器，最后检查 Redis 服务和网络环境。

### 序列化异常

序列化异常通常发生在写入对象、读取对象或项目升级后读取旧缓存时。常见表现包括类型转换失败、Jackson 反序列化失败、类路径变化后无法恢复对象等。

常见异常示例：

```text
org.springframework.data.redis.serializer.SerializationException: Could not read JSON
com.fasterxml.jackson.databind.exc.InvalidTypeIdException
com.fasterxml.jackson.databind.exc.MismatchedInputException
java.lang.ClassCastException
```

常见原因如下：

| 原因                     | 说明                                                     |
| ------------------------ | -------------------------------------------------------- |
| Value 序列化器前后不一致 | 之前用 JDK 序列化，后来改成 JSON 序列化                  |
| 缓存中存在旧数据         | 老数据格式和新序列化器不兼容                             |
| 对象包名或类名变化       | JSON 中的 `@class` 找不到对应类                          |
| 泛型对象反序列化不明确   | 使用 `Object` 接收复杂泛型集合时类型信息不足             |
| Jackson 时间类型未配置   | `LocalDateTime` 等类型未注册 `JavaTimeModule`            |
| 类型白名单过窄           | `BasicPolymorphicTypeValidator` 不允许当前业务类反序列化 |

推荐配置中应确保注册 Java 时间模块，并限制可反序列化的业务包路径：

```java
ObjectMapper objectMapper = new ObjectMapper();

// 支持 LocalDate、LocalDateTime 等 Java 8 时间类型
objectMapper.registerModule(new JavaTimeModule());
objectMapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);

// 限制反序列化类型范围，避免开放过宽
BasicPolymorphicTypeValidator typeValidator = BasicPolymorphicTypeValidator.builder()
        .allowIfSubType("io.github.atengk")
        .allowIfSubType("java.util")
        .allowIfSubType("java.time")
        .build();

objectMapper.activateDefaultTyping(
        typeValidator,
        ObjectMapper.DefaultTyping.NON_FINAL,
        JsonTypeInfo.As.PROPERTY
);
```

排查步骤：

```bash
# 查看问题 Key 的原始内容
docker exec -it redis-dev redis-cli --raw

AUTH 123456

GET redis-template-demo:dev:user:info:10001
```

如果看到的是 JSON，说明当前数据可读；如果看到的是大量不可读字符，可能是旧 JDK 序列化数据。

处理建议：

| 场景                 | 处理方式                                 |
| -------------------- | ---------------------------------------- |
| 开发环境旧缓存不兼容 | 直接删除旧 Key                           |
| 测试环境旧缓存不兼容 | 按前缀批量清理                           |
| 生产环境旧缓存不兼容 | 做灰度兼容或版本化 Key，不要直接全量删除 |
| 类名或包名变更       | 新旧缓存隔离，使用新 Key 前缀            |
| 泛型反序列化失败     | 使用明确类型接收，或调整序列化策略       |

开发环境批量清理命令：

```bash
# 进入 redis-cli 后执行，开发环境可使用
SCAN 0 MATCH redis-template-demo:dev:* COUNT 100

# 删除指定 Key
DEL redis-template-demo:dev:user:info:10001
```

如果需要在本地快速清空当前数据库：

```bash
# 仅允许开发环境使用，会清空当前 Redis database
FLUSHDB
```

### 数据乱码问题

数据乱码通常不是 Redis 本身问题，而是序列化器不统一导致的。最常见情况是 Key 或 Value 使用了 JDK 序列化，导致 Redis 客户端中看到 `\xac\xed\x00\x05` 之类的内容。

乱码示例：

```text
\xac\xed\x00\x05t\x00 redis-template-demo:dev:user:info:10001
```

常见原因如下：

| 问题          | 原因                                       |
| ------------- | ------------------------------------------ |
| Key 乱码      | Key 使用了 JDK 序列化器                    |
| Value 乱码    | Value 使用了 JDK 序列化器                  |
| Hash 字段乱码 | Hash Key 使用了 JDK 序列化器               |
| Hash 值不可读 | Hash Value 使用了 JDK 序列化或二进制序列化 |
| 中文显示转义  | redis-cli 未使用 `--raw` 模式              |

正确的 RedisTemplate 序列化配置应保证 Key 和 Hash Key 使用 `StringRedisSerializer`：

```java
StringRedisSerializer stringRedisSerializer = new StringRedisSerializer();

redisTemplate.setKeySerializer(stringRedisSerializer);
redisTemplate.setHashKeySerializer(stringRedisSerializer);
```

Value 和 Hash Value 推荐使用 JSON 序列化：

```java
RedisSerializer<Object> jsonRedisSerializer = new GenericJackson2JsonRedisSerializer(objectMapper);

redisTemplate.setValueSerializer(jsonRedisSerializer);
redisTemplate.setHashValueSerializer(jsonRedisSerializer);
redisTemplate.setDefaultSerializer(jsonRedisSerializer);
```

如果 Redis 中已经存在乱码 Key，需要注意：乱码 Key 的实际二进制内容与肉眼看到的字符串不同，不一定能直接复制删除。开发环境可以直接清空数据库；测试和生产环境建议按明确前缀迁移到新 Key，不要盲目批量删除。

查看数据时建议使用：

```bash
# raw 模式显示数据，中文和 JSON 更容易阅读
redis-cli --raw

# 查看当前数据库 Key 数量
DBSIZE

# 分批扫描 Key
SCAN 0 MATCH redis-template-demo:dev:* COUNT 100
```

数据乱码处理建议：

| 场景       | 建议                                                  |
| ---------- | ----------------------------------------------------- |
| 新项目     | 一开始就显式配置 RedisTemplate 序列化器               |
| 旧项目改造 | 使用新 Key 前缀，避免新旧序列化数据混用               |
| 开发环境   | 可以清理旧 Key 后重新测试                             |
| 生产环境   | 先确认 Key 归属和业务影响，再灰度清理                 |
| Hash 乱码  | 同时检查 `hashKeySerializer` 和 `hashValueSerializer` |

### 连接超时问题

连接超时通常发生在项目启动、首次访问 Redis、Redis 命令执行或高并发获取连接时。常见异常包括连接失败、认证失败、连接池耗尽、命令超时等。

常见异常示例：

```text
org.springframework.data.redis.RedisConnectionFailureException
io.lettuce.core.RedisConnectionException
io.lettuce.core.RedisCommandTimeoutException
org.apache.commons.pool2.impl.NoSuchElementException: Timeout waiting for idle object
NOAUTH Authentication required
WRONGPASS invalid username-password pair
```

常见原因如下：

| 原因                 | 说明                                    |
| -------------------- | --------------------------------------- |
| Redis 未启动         | 本地或远程 Redis 服务不可用             |
| 地址或端口错误       | `host`、`port` 配置不正确               |
| 密码错误             | `password` 与 Redis 实际密码不一致      |
| 网络不通             | 防火墙、安全组、容器网络或内网 DNS 问题 |
| 超时时间过短         | Redis 响应慢时触发命令超时              |
| 连接池过小           | 并发请求下连接池耗尽                    |
| Redis 服务端压力过高 | CPU、内存、慢查询、连接数过高           |

先检查 Redis 服务是否正常：

```bash
# 查看 Redis 容器是否运行
docker ps | grep redis-dev

# 进入 Redis CLI
docker exec -it redis-dev redis-cli

# 认证
AUTH 123456

# 测试服务响应
PING
```

如果是远程 Redis，可以从应用服务器上测试端口连通性：

```bash
# 测试 Redis 端口是否可访问
nc -vz 192.168.10.20 6379

# 或使用 telnet 测试
telnet 192.168.10.20 6379
```

检查 Spring Boot Redis 配置：

```yaml
spring:
  data:
    redis:
      # Redis 地址必须是应用服务器可访问的地址
      host: 127.0.0.1
      # Redis 端口
      port: 6379
      # Redis 密码，必须与服务端 requirepass 一致
      password: 123456
      # 命令超时时间，不建议过短
      timeout: 3s
      lettuce:
        pool:
          # 最大连接数，根据并发和 Redis 规格调整
          max-active: 32
          # 最大空闲连接数
          max-idle: 16
          # 最小空闲连接数
          min-idle: 4
          # 获取连接最大等待时间
          max-wait: 3s
```

连接池耗尽时，可以适当增大连接池，但应同时排查是否存在慢命令或业务长时间占用 Redis 连接。

排查 Redis 服务状态：

```bash
# 查看 Redis 服务基础信息
INFO server

# 查看客户端连接信息
INFO clients

# 查看内存使用情况
INFO memory

# 查看慢查询，默认返回最近 10 条
SLOWLOG GET 10
```

连接超时处理建议：

| 问题                      | 处理方式                                          |
| ------------------------- | ------------------------------------------------- |
| Redis 未启动              | 启动 Redis 服务或容器                             |
| 密码错误                  | 修正 `spring.data.redis.password`                 |
| 容器中访问本机 Redis 失败 | 不要使用 `127.0.0.1` 指向宿主机，改用容器网络地址 |
| 远程连接失败              | 检查防火墙、安全组、Redis bind 配置               |
| 命令超时                  | 调整 `timeout`，并排查慢查询                      |
| 连接池耗尽                | 增大连接池，优化高频 Redis 调用                   |
| Redis 压力过高            | 拆分热点 Key、减少大 Key 操作、增加监控和容量     |

生产环境还应避免以下操作：

| 操作                         | 风险                                  |
| ---------------------------- | ------------------------------------- |
| `KEYS *`                     | 大量 Key 时会阻塞 Redis               |
| 一次性 `HGETALL` 大 Hash     | 可能造成网络和内存压力                |
| 一次性 `LRANGE 0 -1` 大 List | 可能拉取大量数据                      |
| 高频无 TTL 写入              | 容易造成内存持续增长                  |
| 大对象直接缓存               | 增加序列化、网络传输和 Redis 内存压力 |

如果连接超时问题只在高峰期出现，通常不是单纯配置问题，需要结合应用日志、Redis `SLOWLOG`、连接池监控、Redis CPU 和网络指标一起分析。