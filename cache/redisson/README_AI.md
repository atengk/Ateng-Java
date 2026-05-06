# Redisson

Redisson 是 Redis 官方协议之上的 Java 客户端增强框架，面向 Spring Boot 项目提供分布式对象、分布式锁、分布式集合、限流器、延迟队列、布隆过滤器、发布订阅等能力。相比直接使用 Redis 命令或 `RedisTemplate` 操作 Key-Value，Redisson 更偏向于提供可直接用于业务开发的高层分布式组件。

## 功能概述

本节用于说明 Redisson 在 Spring Boot 3 项目中的定位、适用场景、与 `RedisTemplate` 的区别，以及本模块在实际开发中的目标与边界。

### Redisson 适用场景

Redisson 适合用于需要分布式协调能力的 Spring Boot 业务系统，尤其是单体服务向多实例部署、微服务部署、容器化部署演进后，需要跨 JVM、跨节点共享状态或控制并发的场景。

常见适用场景如下：

| 场景       | Redisson 能力                          | 典型业务示例                                 |
| ---------- | -------------------------------------- | -------------------------------------------- |
| 分布式锁   | `RLock`、`RFairLock`、`RReadWriteLock` | 防止重复提交、库存扣减、定时任务防重入       |
| 分布式限流 | `RRateLimiter`                         | 接口访问频率控制、短信发送限流、登录失败限制 |
| 分布式集合 | `RMap`、`RList`、`RSet`、`RQueue`      | 多实例共享集合、任务队列、临时状态管理       |
| 延迟任务   | `RDelayedQueue`                        | 订单超时取消、消息延迟处理、重试任务调度     |
| 分布式缓存 | `RMapCache`、本地缓存 Map              | 热点数据缓存、带过期时间的业务数据缓存       |
| 原子计数   | `RAtomicLong`、`RLongAdder`            | 浏览量、点赞数、并发计数器                   |
| 发布订阅   | `RTopic`                               | 服务间轻量通知、缓存刷新通知                 |

Redisson 官方文档也将其定位为 Redis/Valkey Java 客户端，提供同步、异步、响应式 API，以及 50 多种基于 Redis 的 Java 对象和服务，包括 Lock、Queue、Map、List、Semaphore、RateLimiter 等能力。([Maven Central](https://central.sonatype.com/artifact/org.redisson/redisson-spring-boot-starter?utm_source=chatgpt.com))

### 与 RedisTemplate 的区别

`RedisTemplate` 和 Redisson 都可以操作 Redis，但两者的抽象层次不同。`RedisTemplate` 更接近 Redis 数据结构操作工具，适合直接读写 String、Hash、List、Set、ZSet 等原生结构；Redisson 更接近分布式组件库，适合直接使用锁、限流器、延迟队列、分布式集合等高级能力。

| 对比项     | RedisTemplate                                    | Redisson                                               |
| ---------- | ------------------------------------------------ | ------------------------------------------------------ |
| 主要定位   | Redis 数据访问模板                               | Redis 分布式对象与分布式服务框架                       |
| 抽象层级   | 偏底层，围绕 Redis 原生命令封装                  | 偏高层，围绕 Java 对象和分布式组件封装                 |
| 常用能力   | String、Hash、List、Set、ZSet 操作               | Lock、Map、Queue、RateLimiter、Semaphore、DelayedQueue |
| 分布式锁   | 需要自行设计锁 Key、过期时间、Lua 脚本、释放逻辑 | 内置 `RLock`，支持可重入、自动续期、超时释放           |
| 延迟队列   | 通常需要 ZSet + 轮询 + Lua 自行实现              | 可使用 `RDelayedQueue`                                 |
| 限流       | 通常需要 Lua 或令牌桶逻辑自行实现                | 可使用 `RRateLimiter`                                  |
| 使用复杂度 | 简单操作直观，高级能力需要自行封装               | 高级分布式能力开箱即用                                 |
| 适合场景   | 普通缓存、简单 Redis 数据读写                    | 分布式锁、限流、队列、复杂并发控制                     |

在实际项目中，两者不是绝对替代关系。普通缓存读写、简单 Key-Value 操作可以继续使用 `RedisTemplate`；涉及分布式锁、限流、延迟队列、分布式对象时，优先使用 Redisson，可以减少重复造轮子和并发控制错误。

### 模块目标与边界

本模块的目标是为 Spring Boot 3 项目提供一套可复用的 Redisson 集成方案，使业务代码能够稳定使用分布式锁、分布式限流、延迟队列、分布式集合和缓存能力。

模块目标如下：

| 目标           | 说明                                                         |
| -------------- | ------------------------------------------------------------ |
| 统一依赖配置   | 在项目中统一引入 Redisson Starter，避免业务模块重复声明依赖  |
| 统一连接配置   | 支持单机、哨兵、集群等 Redis 部署模式                        |
| 统一客户端管理 | 由 Spring 容器管理 `RedissonClient` 生命周期                 |
| 统一业务封装   | 对锁、限流、延迟队列等常用能力进行 Service 层封装            |
| 统一异常处理   | 对 Redis 连接异常、锁获取失败、限流失败等情况提供清晰处理方式 |
| 统一验证方式   | 提供接口测试、并发测试、锁释放验证等验证路径                 |

模块边界如下：

| 范围              | 是否包含   | 说明                                             |
| ----------------- | ---------- | ------------------------------------------------ |
| Redisson 依赖引入 | 是         | 包含 Maven/Gradle 依赖配置                       |
| Redis 单机连接    | 是         | 适合本地开发和测试环境                           |
| Redis 哨兵连接    | 是         | 适合主从高可用场景                               |
| Redis 集群连接    | 是         | 适合生产集群场景                                 |
| 分布式锁封装      | 是         | 包含可重入锁、公平锁、读写锁等                   |
| 限流器封装        | 是         | 包含固定速率限流和接口限流示例                   |
| 延迟队列封装      | 是         | 包含任务投递、消费和异常处理                     |
| Redis 服务部署    | 部分包含   | 仅提供本地和 Docker 示例，不展开完整生产运维体系 |
| Redis 数据建模    | 不重点包含 | 不作为 Redis 数据库设计文档                      |
| Redis 内核原理    | 不重点包含 | 只说明开发所需的关键概念                         |
| 高级性能压测      | 不重点包含 | 仅提供基础并发验证方式                           |

## 环境准备

本节用于说明 Spring Boot 3 集成 Redisson 前需要准备的基础环境，包括 JDK、构建工具、Redis 服务和 Redisson 依赖。Redisson Spring Boot Starter 官方支持 Spring Boot 1.3.x 到 4.0.x，并依赖 Spring Data Redis 模块；Spring Boot 3.x 项目需要使用 `spring.data.redis` 配置前缀。([Redisson](https://redisson.pro/docs/integration-with-spring/))

### Spring Boot 3 版本要求

Spring Boot 3 项目要求使用 Java 17 或更高版本。以 Spring Boot 3.3 系列官方要求为例，Spring Boot 3.3.16 至少需要 Java 17，并要求 Maven 3.6.3 或更高版本；Gradle 支持 7.5+ 或 8.x。([Spring 文档](https://docs.enterprise.spring.io/spring-boot/system-requirements.html?utm_source=chatgpt.com))

建议开发环境如下：

| 工具             | 建议版本    | 说明                                          |
| ---------------- | ----------- | --------------------------------------------- |
| JDK              | 17+         | Spring Boot 3 基础要求                        |
| Spring Boot      | 3.x         | 本文档以 Spring Boot 3 项目为前提             |
| Maven            | 3.6.3+      | 官方明确支持的 Maven 最低版本                 |
| Gradle           | 7.5+ 或 8.x | 使用 Gradle 项目时选择                        |
| Redis            | 6.x / 7.x   | 本地开发建议使用 Redis 7.x                    |
| Redisson Starter | 4.3.1       | 当前官方文档与 Maven Central 均可查询到该版本 |

检查本地 Java 与 Maven 版本：

```bash
# 查看 JDK 版本，确认主版本不低于 17
java -version

# 查看 Maven 版本，确认 Maven 不低于 3.6.3
mvn -version
```

如果项目使用 Spring Boot 3，需要重点确认以下事项：

| 检查项                          | 要求                                    |
| ------------------------------- | --------------------------------------- |
| `pom.xml` 的 Spring Boot Parent | 使用 `3.x.x`                            |
| JDK 编译版本                    | `17` 或更高                             |
| Redis 配置前缀                  | 使用 `spring.data.redis`                |
| Jakarta 包名                    | Spring Boot 3 已切换到 `jakarta.*` 体系 |
| Redisson Spring Data 模块       | 使用与 Spring Data Redis 3.x 匹配的模块 |

Redisson 官方说明中，`redisson-spring-data-3x` 对应 Spring Boot 3.x，`redisson-spring-data-4x` 对应 Spring Boot 4.x；如果 Starter 默认依赖的 Spring Data Redis 模块与项目 Spring Boot 主版本不一致，需要显式排除并指定兼容模块。([Redisson](https://redisson.pro/docs/integration-with-spring/))

### Redis 服务准备

Redisson 依赖 Redis 服务运行，开发环境可以使用本机 Redis、Docker Redis、远程 Redis、哨兵模式 Redis 或 Redis Cluster。初期开发建议先使用单机 Redis，待功能验证完成后再切换到哨兵或集群配置。

本地开发推荐使用 Docker 启动 Redis，便于快速创建、销毁和隔离环境。

```bash
# 拉取 Redis 7 镜像
docker pull redis:7

# 启动 Redis 容器
docker run -d \
  --name redis-redisson \
  -p 6379:6379 \
  redis:7 \
  redis-server --appendonly yes
```

参数说明：

| 参数                            | 说明                                        |
| ------------------------------- | ------------------------------------------- |
| `--name redis-redisson`         | 指定容器名称，便于后续启动、停止和查看日志  |
| `-p 6379:6379`                  | 将容器内 Redis 端口映射到宿主机             |
| `redis:7`                       | 使用 Redis 7 官方镜像                       |
| `redis-server --appendonly yes` | 开启 AOF 持久化，避免容器重启后数据全部丢失 |

验证 Redis 是否可用：

```bash
# 进入 Redis 容器执行 redis-cli
docker exec -it redis-redisson redis-cli

# 在 redis-cli 中执行
ping
```

如果返回结果为：

```text
PONG
```

说明 Redis 服务已启动并可正常响应请求。

也可以直接在宿主机执行：

```bash
# 查看 Redis 容器运行状态
docker ps | grep redis-redisson

# 查看 Redis 容器日志
docker logs redis-redisson
```

生产环境 Redis 建议至少关注以下配置：

| 配置项   | 建议                               |
| -------- | ---------------------------------- |
| 密码认证 | 必须启用 Redis 密码                |
| 网络访问 | 不建议直接暴露公网端口             |
| 持久化   | 根据业务场景启用 AOF 或 RDB        |
| 高可用   | 生产建议使用哨兵或集群             |
| 连接数   | 根据服务实例数和线程模型评估       |
| Key 命名 | 统一业务前缀，避免跨模块冲突       |
| 监控     | 关注内存、连接数、慢查询、命令 QPS |

### Redisson 依赖配置

Spring Boot 3 项目推荐使用 `redisson-spring-boot-starter` 集成 Redisson。官方文档中的 Community Edition Maven 坐标为 `org.redisson:redisson-spring-boot-starter`，当前官方文档示例版本为 `4.3.1`，Maven Central 也显示 `4.3.1` 为可用版本。([Redisson](https://redisson.pro/docs/integration-with-spring/))

文件位置：`pom.xml`

```xml
<dependencies>
    <!-- Spring Boot Web 基础依赖，用于提供 Controller、JSON 序列化、嵌入式 Web 容器等能力 -->
    <dependency>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-starter-web</artifactId>
    </dependency>

    <!-- Redisson Spring Boot Starter，用于自动装配 RedissonClient 和 Redis 相关集成能力 -->
    <dependency>
        <groupId>org.redisson</groupId>
        <artifactId>redisson-spring-boot-starter</artifactId>
        <version>4.3.1</version>
    </dependency>

    <!-- Lombok，用于减少 DTO、配置类、日志对象等样板代码 -->
    <dependency>
        <groupId>org.projectlombok</groupId>
        <artifactId>lombok</artifactId>
        <optional>true</optional>
    </dependency>

    <!-- Hutool 工具类库，业务开发中可用于字符串、集合、JSON、日期等常用处理 -->
    <dependency>
        <groupId>cn.hutool</groupId>
        <artifactId>hutool-all</artifactId>
        <version>5.8.36</version>
    </dependency>

    <!-- Spring Boot Test，用于单元测试、并发测试和集成测试 -->
    <dependency>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-starter-test</artifactId>
        <scope>test</scope>
    </dependency>
</dependencies>
```

如果项目使用 Gradle，可以使用以下配置。

文件位置：`build.gradle`

```groovy
dependencies {
    // Spring Boot Web 基础依赖
    implementation 'org.springframework.boot:spring-boot-starter-web'

    // Redisson Spring Boot Starter
    implementation 'org.redisson:redisson-spring-boot-starter:4.3.1'

    // Hutool 工具类库
    implementation 'cn.hutool:hutool-all:5.8.36'

    // Lombok 编译期注解处理
    compileOnly 'org.projectlombok:lombok'
    annotationProcessor 'org.projectlombok:lombok'

    // Spring Boot 测试依赖
    testImplementation 'org.springframework.boot:spring-boot-starter-test'
}
```

Spring Boot 3 的 Redis 基础配置建议使用 `spring.data.redis` 前缀。Redisson 官方文档也明确给出了 Spring Boot 3.x+ 的 `spring.data.redis` 配置结构。([Redisson](https://redisson.pro/docs/integration-with-spring/))

文件位置：`src/main/resources/application.yml`

```yaml
spring:
  application:
    # 应用名称，用于日志、监控和 Redis 客户端标识
    name: springboot3-redisson-demo

  data:
    redis:
      # Redis 单机地址
      host: 127.0.0.1

      # Redis 单机端口
      port: 6379

      # Redis 数据库索引，默认 0
      database: 0

      # Redis 密码；本地无密码时可留空，生产环境必须配置
      password:

      # Redis 命令超时时间
      timeout: 3000ms

      # Redis 连接超时时间
      connect-timeout: 3000ms

      # Redis 客户端名称，便于在 Redis 连接列表中识别来源
      client-name: ${spring.application.name}
```

如果需要使用 Redisson 原生配置文件，可以通过 `spring.redis.redisson.file` 或配置片段方式引入 Redisson 配置；官方文档提供了 `classpath:redisson.yaml` 和内联 `config` 两种形式。([Redisson](https://redisson.pro/docs/integration-with-spring/))

文件位置：`src/main/resources/application.yml`

```yaml
spring:
  redis:
    redisson:
      # 指定 Redisson 原生配置文件位置
      file: classpath:redisson.yaml
```

文件位置：`src/main/resources/redisson.yaml`

```yaml
# 单机 Redis 配置
singleServerConfig:
  # Redis 地址，Redisson 使用 redis:// 或 rediss:// 协议前缀
  address: "redis://127.0.0.1:6379"

  # Redis 数据库索引
  database: 0

  # Redis 密码；无密码时配置为 null
  password: null

  # 最小空闲连接数
  connectionMinimumIdleSize: 8

  # 连接池大小
  connectionPoolSize: 32

  # 命令等待超时时间，单位毫秒
  timeout: 3000

  # 连接超时时间，单位毫秒
  connectTimeout: 3000

  # 命令重试次数
  retryAttempts: 3

  # 命令重试间隔，单位毫秒
  retryInterval: 1500

# Netty 线程数，0 表示由 Redisson 自动计算
threads: 0

# Netty 事件循环线程数，0 表示由 Redisson 自动计算
nettyThreads: 0
```

依赖配置完成后，可以执行以下命令验证依赖是否能正常解析：

```bash
# 清理并编译项目，验证依赖是否下载成功
mvn clean compile

# 查看 Redisson 相关依赖树
mvn dependency:tree | grep redisson
```

需要注意，`redisson-spring-boot-starter` 会跟随 Spring Boot/Spring Data Redis 版本存在兼容关系。Spring Boot 3.x 项目通常应匹配 Spring Data Redis 3.x 相关模块；如果项目升级到 Spring Boot 4，则需要重新检查 Redisson Starter 及 `redisson-spring-data-*` 模块版本，避免出现自动配置类、Spring Data Redis API 或依赖冲突问题。



## 基础配置

本节用于定义 Redisson 在 Spring Boot 3 项目中的基础连接方式。Redisson 支持通过 Spring Boot 通用 Redis 配置、`spring.redis.redisson.file` 指定原生配置文件，或通过代码中的 `Config` 对象进行编程式配置；原生配置支持单机、哨兵、集群等多种模式。([Redisson](https://redisson.pro/docs/integration-with-spring/?utm_source=chatgpt.com))

### 单机模式配置

单机模式适合本地开发、测试环境或 Redis 单节点部署场景。该模式连接一个 Redis 实例，配置简单，适合先验证 Redisson 客户端初始化、数据结构操作、分布式锁和限流器等基础能力。

文件位置：`src/main/resources/application-single.yml`

```yaml
spring:
  profiles:
    active: single

  redis:
    redisson:
      # 使用 Redisson 原生 YAML 配置文件
      file: classpath:redisson-single.yaml
```

文件位置：`src/main/resources/redisson-single.yaml`

```yaml
# Redisson 单机模式配置
singleServerConfig:
  # Redis 单机地址，普通连接使用 redis://，SSL 连接使用 rediss://
  address: "redis://127.0.0.1:6379"

  # Redis 账号；未启用 ACL 时可不配置
  username: null

  # Redis 密码；本地无密码时可配置为 null
  password: null

  # Redis 数据库索引
  database: 0

  # Redis 命令响应超时时间，单位毫秒
  timeout: 3000

  # Redis 建立连接超时时间，单位毫秒
  connectTimeout: 3000

  # 命令发送失败后的最大重试次数
  retryAttempts: 4

  # 重试延迟策略，避免固定间隔重试造成瞬时压力
  retryDelay: !<org.redisson.config.EqualJitterDelay>
    baseDelay: PT1S
    maxDelay: PT2S

  # 断线重连延迟策略
  reconnectionDelay: !<org.redisson.config.EqualJitterDelay>
    baseDelay: PT0.1S
    maxDelay: PT10S

  # 客户端名称，便于 Redis CLIENT LIST 排查连接来源
  clientName: "springboot3-redisson-single"

  # 最小空闲连接数
  connectionMinimumIdleSize: 8

  # 连接池大小
  connectionPoolSize: 32

  # 订阅连接最小空闲数，用于锁、发布订阅等场景
  subscriptionConnectionMinimumIdleSize: 1

  # 订阅连接池大小
  subscriptionConnectionPoolSize: 16

# Redisson 业务线程数；未明确压测前建议使用默认值
threads: 0

# Netty 线程数；未明确压测前建议使用默认值
nettyThreads: 0

# 传输模式，通用环境使用 NIO
transportMode: "NIO"
```

启动时使用单机配置：

```bash
# 使用 single profile 启动项目
mvn spring-boot:run -Dspring-boot.run.profiles=single
```

Redisson 单机模式通过 `singleServerConfig` 启用，核心配置是 `address`，地址需要使用 `redis://` 或 `rediss://` 协议前缀。Redisson 官方配置文档也明确说明，单机模式对应 `SingleServerConfig`，YAML 属性名与 Redisson `Config` 对象属性名保持一致。([Redisson](https://redisson.pro/docs/configuration/?utm_source=chatgpt.com))

### 哨兵模式配置

哨兵模式适合 Redis 主从高可用部署场景。业务服务连接 Sentinel 节点，由 Sentinel 负责发现当前 Master 节点；当 Master 故障转移后，Redisson 可以通过 Sentinel 获取新的 Master 信息。

文件位置：`src/main/resources/application-sentinel.yml`

```yaml
spring:
  profiles:
    active: sentinel

  redis:
    redisson:
      # 使用 Redisson 哨兵模式原生配置文件
      file: classpath:redisson-sentinel.yaml
```

文件位置：`src/main/resources/redisson-sentinel.yaml`

```yaml
# Redisson 哨兵模式配置
sentinelServersConfig:
  # Sentinel 中配置的 Master 名称，需要与 sentinel.conf 中 monitor 的名称一致
  masterName: "mymaster"

  # Sentinel 节点地址，建议至少配置 3 个 Sentinel 节点
  sentinelAddresses:
    - "redis://127.0.0.1:26379"
    - "redis://127.0.0.1:26380"
    - "redis://127.0.0.1:26381"

  # Redis 账号；未启用 ACL 时可不配置
  username: null

  # Redis 密码；如果 Redis 主从节点启用了密码，需要配置
  password: null

  # Redis 数据库索引
  database: 0

  # Redis 命令响应超时时间，单位毫秒
  timeout: 3000

  # Redis 建立连接超时时间，单位毫秒
  connectTimeout: 3000

  # 命令发送失败后的最大重试次数
  retryAttempts: 4

  # 重试延迟策略
  retryDelay: !<org.redisson.config.EqualJitterDelay>
    baseDelay: PT1S
    maxDelay: PT2S

  # 断线重连延迟策略
  reconnectionDelay: !<org.redisson.config.EqualJitterDelay>
    baseDelay: PT0.1S
    maxDelay: PT10S

  # 读模式：SLAVE 表示优先从从节点读取，MASTER 表示只从主节点读取，MASTER_SLAVE 表示主从都可读
  readMode: "SLAVE"

  # 订阅模式：通常使用 MASTER，避免从节点切换带来的订阅不稳定
  subscriptionMode: "MASTER"

  # Master 节点最小空闲连接数
  masterConnectionMinimumIdleSize: 8

  # Master 节点连接池大小
  masterConnectionPoolSize: 32

  # Slave 节点最小空闲连接数
  slaveConnectionMinimumIdleSize: 8

  # Slave 节点连接池大小
  slaveConnectionPoolSize: 32

  # 订阅连接最小空闲数
  subscriptionConnectionMinimumIdleSize: 1

  # 订阅连接池大小
  subscriptionConnectionPoolSize: 16

  # 客户端名称
  clientName: "springboot3-redisson-sentinel"

# Redisson 业务线程数
threads: 0

# Netty 线程数
nettyThreads: 0

# 传输模式
transportMode: "NIO"
```

启动时使用哨兵配置：

```bash
# 使用 sentinel profile 启动项目
mvn spring-boot:run -Dspring-boot.run.profiles=sentinel
```

哨兵模式通过 `sentinelServersConfig` 启用，核心配置是 `masterName` 和 `sentinelAddresses`。`masterName` 必须与 Sentinel 监控的主节点名称一致，否则 Redisson 无法通过 Sentinel 正确发现 Master。Redisson 官方文档中，哨兵模式对应 `SentinelServersConfig`，支持 `masterName`、`sentinelAddresses`、`readMode`、`subscriptionMode` 等参数。([Redisson](https://redisson.pro/docs/configuration/?utm_source=chatgpt.com))

### 集群模式配置

集群模式适合 Redis Cluster 部署场景。业务服务只需要配置部分集群节点地址，Redisson 会根据 Redis Cluster 拓扑发现其他节点，并根据槽位分布访问对应节点。

文件位置：`src/main/resources/application-cluster.yml`

```yaml
spring:
  profiles:
    active: cluster

  redis:
    redisson:
      # 使用 Redisson 集群模式原生配置文件
      file: classpath:redisson-cluster.yaml
```

文件位置：`src/main/resources/redisson-cluster.yaml`

```yaml
# Redisson 集群模式配置
clusterServersConfig:
  # Redis Cluster 节点地址，不要求写全所有节点，但建议至少配置多个可用节点
  nodeAddresses:
    - "redis://127.0.0.1:7000"
    - "redis://127.0.0.1:7001"
    - "redis://127.0.0.1:7002"
    - "redis://127.0.0.1:7003"
    - "redis://127.0.0.1:7004"
    - "redis://127.0.0.1:7005"

  # Redis 账号；未启用 ACL 时可不配置
  username: null

  # Redis 密码；集群所有节点密码应保持一致
  password: null

  # 集群拓扑扫描间隔，单位毫秒
  scanInterval: 2000

  # 是否检查所有槽位可用，生产环境建议保持 true
  checkSlotsCoverage: true

  # Redis 命令响应超时时间，单位毫秒
  timeout: 3000

  # Redis 建立连接超时时间，单位毫秒
  connectTimeout: 3000

  # 命令发送失败后的最大重试次数
  retryAttempts: 4

  # 重试延迟策略
  retryDelay: !<org.redisson.config.EqualJitterDelay>
    baseDelay: PT1S
    maxDelay: PT2S

  # 断线重连延迟策略
  reconnectionDelay: !<org.redisson.config.EqualJitterDelay>
    baseDelay: PT0.1S
    maxDelay: PT10S

  # 读模式：SLAVE、MASTER、MASTER_SLAVE
  readMode: "SLAVE"

  # 订阅模式：MASTER 或 SLAVE
  subscriptionMode: "MASTER"

  # Master 节点最小空闲连接数
  masterConnectionMinimumIdleSize: 8

  # Master 节点连接池大小
  masterConnectionPoolSize: 32

  # Slave 节点最小空闲连接数
  slaveConnectionMinimumIdleSize: 8

  # Slave 节点连接池大小
  slaveConnectionPoolSize: 32

  # 订阅连接最小空闲数
  subscriptionConnectionMinimumIdleSize: 1

  # 订阅连接池大小
  subscriptionConnectionPoolSize: 16

  # 客户端名称
  clientName: "springboot3-redisson-cluster"

# Redisson 业务线程数
threads: 0

# Netty 线程数
nettyThreads: 0

# 传输模式
transportMode: "NIO"
```

启动时使用集群配置：

```bash
# 使用 cluster profile 启动项目
mvn spring-boot:run -Dspring-boot.run.profiles=cluster
```

集群模式通过 `clusterServersConfig` 启用，核心配置是 `nodeAddresses` 和 `scanInterval`。Redisson 官方文档说明，Redisson 会基于 Redis Cluster 节点地址发现集群拓扑，`scanInterval` 用于控制集群拓扑扫描间隔，`checkSlotsCoverage` 用于启动时检查槽位覆盖情况。([Redisson](https://redisson.pro/docs/configuration/?utm_source=chatgpt.com))

### 配置参数说明

本节汇总 Redisson 常用连接参数，便于在不同环境中统一调整。生产环境不建议直接复制本地开发配置，应结合 Redis 部署方式、服务实例数、并发量和连接数限制进行压测后确认。

| 参数                             | 所属模式   | 说明                        | 建议                                  |
| -------------------------------- | ---------- | --------------------------- | ------------------------------------- |
| `address`                        | 单机       | Redis 单机地址              | 必须带 `redis://` 或 `rediss://` 前缀 |
| `sentinelAddresses`              | 哨兵       | Sentinel 节点地址列表       | 至少配置 3 个 Sentinel 节点           |
| `masterName`                     | 哨兵       | Sentinel 监控的 Master 名称 | 必须与 Sentinel 配置一致              |
| `nodeAddresses`                  | 集群       | Redis Cluster 节点地址列表  | 建议配置多个节点，提升启动可用性      |
| `database`                       | 单机、哨兵 | Redis 数据库索引            | 集群模式不支持按数据库切换            |
| `username`                       | 通用       | Redis ACL 用户名            | Redis 6+ 启用 ACL 时配置              |
| `password`                       | 通用       | Redis 密码                  | 生产环境必须配置                      |
| `timeout`                        | 通用       | Redis 命令响应超时时间      | 常用 `3000ms`，慢网络可适当增加       |
| `connectTimeout`                 | 通用       | 建立连接超时时间            | 常用 `3000ms` 到 `10000ms`            |
| `retryAttempts`                  | 通用       | 命令发送失败后的重试次数    | 常用 `3` 到 `4`                       |
| `retryDelay`                     | 通用       | 重试延迟策略                | 建议使用抖动延迟，避免集中重试        |
| `reconnectionDelay`              | 通用       | 断线重连延迟策略            | 建议使用抖动延迟                      |
| `connectionPoolSize`             | 单机       | 普通连接池大小              | 根据并发和实例数调整                  |
| `masterConnectionPoolSize`       | 哨兵、集群 | Master 连接池大小           | 写多场景适当调大                      |
| `slaveConnectionPoolSize`        | 哨兵、集群 | Slave 连接池大小            | 读多场景适当调大                      |
| `subscriptionConnectionPoolSize` | 通用       | 订阅连接池大小              | 使用锁、Topic、Semaphore 时需关注     |
| `readMode`                       | 哨兵、集群 | 读取节点策略                | 读一致性要求高时使用 `MASTER`         |
| `subscriptionMode`               | 哨兵、集群 | 订阅节点策略                | 通常使用 `MASTER`                     |
| `scanInterval`                   | 集群       | 集群拓扑扫描间隔            | 常用 `1000ms` 到 `5000ms`             |
| `checkSlotsCoverage`             | 集群       | 启动时检查槽位覆盖          | 生产建议保持 `true`                   |
| `clientName`                     | 通用       | Redis 客户端名称            | 建议配置为应用名或实例名              |
| `threads`                        | 通用       | Redisson 业务线程数         | 无明确压测结论时使用默认值            |
| `nettyThreads`                   | 通用       | Netty IO 线程数             | 无明确压测结论时使用默认值            |

Redisson 当前配置文档中，命令超时、重试次数和重试延迟是容错配置的核心参数；`retryDelay` 支持固定延迟、等抖动、全抖动、去相关抖动等策略，生产环境建议优先使用带抖动的延迟策略，避免 Redis 故障恢复时所有客户端同时重试。([Redisson](https://redisson.pro/docs/configuration/?utm_source=chatgpt.com))

## Redisson 客户端初始化

本节用于说明 Spring Boot 3 项目中如何创建和管理 `RedissonClient`。实际开发中优先使用 Redisson Starter 自动配置；只有在需要动态切换模式、统一封装配置属性、强控制连接参数或接入内部配置中心时，才建议自定义 `RedissonClient` Bean。

### 自动配置方式

自动配置方式适合大多数 Spring Boot 项目。引入 `redisson-spring-boot-starter` 后，可以通过 Spring Boot Redis 通用配置或 Redisson 原生配置文件初始化客户端。Redisson Starter 官方文档说明，它会集成 Spring Boot，并提供 `RedissonClient`、`RedissonRxClient`、`RedissonReactiveClient`、`RedisTemplate` 等 Spring Bean。([Redisson](https://redisson.pro/docs/integration-with-spring/?utm_source=chatgpt.com))

文件位置：`src/main/resources/application.yml`

```yaml
spring:
  application:
    # 应用名称，用于日志、监控和 Redis 客户端识别
    name: springboot3-redisson-demo

  data:
    redis:
      # Spring Boot 3 推荐使用 spring.data.redis 前缀
      host: 127.0.0.1
      port: 6379
      database: 0
      password:
      timeout: 3000ms
      connect-timeout: 3000ms
      client-name: ${spring.application.name}
```

在业务代码中直接注入 `RedissonClient` 即可。

文件位置：`src/main/java/io/github/atengk/redisson/service/RedissonHealthService.java`

```java
package io.github.atengk.redisson.service;

import cn.hutool.core.date.DateUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.redisson.api.RBucket;
import org.redisson.api.RedissonClient;
import org.springframework.stereotype.Service;

import java.util.Date;
import java.util.concurrent.TimeUnit;

/**
 * Redisson 客户端健康检查服务。
 *
 * @author Ateng
 * @since 2026-05-06
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class RedissonHealthService {

    private final RedissonClient redissonClient;

    /**
     * 写入并读取测试 Key，用于验证 RedissonClient 是否可用。
     *
     * @return Redis 中读取到的测试值
     */
    public String check() {
        String key = "redisson:health:check";
        String value = "Redisson 正常：" + DateUtil.formatDateTime(new Date());

        RBucket<String> bucket = redissonClient.getBucket(key);
        bucket.set(value, 30, TimeUnit.SECONDS);

        String result = bucket.get();
        log.info("Redisson客户端健康检查完成，key：{}，value：{}", key, result);
        return result;
    }
}
```

文件位置：`src/main/java/io/github/atengk/redisson/controller/RedissonHealthController.java`

```java
package io.github.atengk.redisson.controller;

import io.github.atengk.redisson.service.RedissonHealthService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Redisson 客户端健康检查接口。
 *
 * @author Ateng
 * @since 2026-05-06
 */
@RestController
@RequiredArgsConstructor
public class RedissonHealthController {

    private final RedissonHealthService redissonHealthService;

    /**
     * 检查 Redisson 客户端是否可正常读写 Redis。
     *
     * @return 健康检查结果
     */
    @GetMapping("/redisson/health")
    public String health() {
        return redissonHealthService.check();
    }
}
```

验证接口：

```bash
# 启动项目后访问健康检查接口
curl http://localhost:8080/redisson/health
```

返回示例：

```text
Redisson 正常：2026-05-06 10:30:00
```

### 自定义配置方式

自定义配置方式适合需要用统一业务配置控制 Redisson 的项目。例如根据 `app.redisson.mode` 在单机、哨兵、集群之间切换，或者需要对连接池、读写模式、客户端名称等参数做二次封装。

文件结构如下：

```text
src/main/java/io/github/atengk/redisson/config/RedissonClientProperties.java
src/main/java/io/github/atengk/redisson/config/RedissonClientConfiguration.java
src/main/resources/application.yml
```

文件位置：`src/main/resources/application.yml`

```yaml
app:
  redisson:
    # 可选值：SINGLE、SENTINEL、CLUSTER
    mode: SINGLE

    # Redis ACL 用户名；未启用 ACL 时可留空
    username:

    # Redis 密码；生产环境建议通过环境变量注入
    password: ${REDIS_PASSWORD:}

    # 客户端名称，便于 Redis 侧排查连接来源
    client-name: ${spring.application.name:springboot3-redisson-demo}

    # 命令响应超时时间
    timeout: 3s

    # 建立连接超时时间
    connect-timeout: 3s

    # 命令发送失败后的重试次数
    retry-attempts: 4

    # 最小重试延迟
    retry-min-delay: 1s

    # 最大重试延迟
    retry-max-delay: 2s

    # 业务线程数；0 表示不显式指定，使用 Redisson 默认策略
    threads: 0

    # Netty 线程数；0 表示不显式指定，使用 Redisson 默认策略
    netty-threads: 0

    single:
      # 单机 Redis 地址
      address: redis://127.0.0.1:6379
      database: 0
      connection-minimum-idle-size: 8
      connection-pool-size: 32

    sentinel:
      # Sentinel Master 名称
      master-name: mymaster
      sentinel-addresses:
        - redis://127.0.0.1:26379
        - redis://127.0.0.1:26380
        - redis://127.0.0.1:26381
      database: 0
      read-mode: SLAVE
      subscription-mode: MASTER
      master-connection-minimum-idle-size: 8
      master-connection-pool-size: 32
      slave-connection-minimum-idle-size: 8
      slave-connection-pool-size: 32

    cluster:
      # Redis Cluster 节点地址
      node-addresses:
        - redis://127.0.0.1:7000
        - redis://127.0.0.1:7001
        - redis://127.0.0.1:7002
      scan-interval: 2000
      read-mode: SLAVE
      subscription-mode: MASTER
      master-connection-minimum-idle-size: 8
      master-connection-pool-size: 32
      slave-connection-minimum-idle-size: 8
      slave-connection-pool-size: 32
```

下面的属性类用于承载 `app.redisson` 下的自定义配置。

文件位置：`src/main/java/io/github/atengk/redisson/config/RedissonClientProperties.java`

```java
package io.github.atengk.redisson.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;

/**
 * Redisson 客户端配置属性。
 *
 * @author Ateng
 * @since 2026-05-06
 */
@Data
@ConfigurationProperties(prefix = "app.redisson")
public class RedissonClientProperties {

    /**
     * 客户端连接模式。
     */
    private Mode mode = Mode.SINGLE;

    /**
     * Redis ACL 用户名。
     */
    private String username;

    /**
     * Redis 密码。
     */
    private String password;

    /**
     * Redis 客户端名称。
     */
    private String clientName = "springboot3-redisson-demo";

    /**
     * 命令响应超时时间。
     */
    private Duration timeout = Duration.ofSeconds(3);

    /**
     * 建立连接超时时间。
     */
    private Duration connectTimeout = Duration.ofSeconds(3);

    /**
     * 命令发送失败后的重试次数。
     */
    private int retryAttempts = 4;

    /**
     * 最小重试延迟。
     */
    private Duration retryMinDelay = Duration.ofSeconds(1);

    /**
     * 最大重试延迟。
     */
    private Duration retryMaxDelay = Duration.ofSeconds(2);

    /**
     * Redisson 业务线程数。
     */
    private int threads = 0;

    /**
     * Netty 线程数。
     */
    private int nettyThreads = 0;

    /**
     * 单机配置。
     */
    private Single single = new Single();

    /**
     * 哨兵配置。
     */
    private Sentinel sentinel = new Sentinel();

    /**
     * 集群配置。
     */
    private Cluster cluster = new Cluster();

    /**
     * Redisson 连接模式。
     */
    public enum Mode {
        SINGLE,
        SENTINEL,
        CLUSTER
    }

    /**
     * 单机模式配置。
     *
     * @author Ateng
     * @since 2026-05-06
     */
    @Data
    public static class Single {
        private String address = "redis://127.0.0.1:6379";
        private int database = 0;
        private int connectionMinimumIdleSize = 8;
        private int connectionPoolSize = 32;
    }

    /**
     * 哨兵模式配置。
     *
     * @author Ateng
     * @since 2026-05-06
     */
    @Data
    public static class Sentinel {
        private String masterName = "mymaster";
        private List<String> sentinelAddresses = new ArrayList<>();
        private int database = 0;
        private String readMode = "SLAVE";
        private String subscriptionMode = "MASTER";
        private int masterConnectionMinimumIdleSize = 8;
        private int masterConnectionPoolSize = 32;
        private int slaveConnectionMinimumIdleSize = 8;
        private int slaveConnectionPoolSize = 32;
    }

    /**
     * 集群模式配置。
     *
     * @author Ateng
     * @since 2026-05-06
     */
    @Data
    public static class Cluster {
        private List<String> nodeAddresses = new ArrayList<>();
        private int scanInterval = 2000;
        private String readMode = "SLAVE";
        private String subscriptionMode = "MASTER";
        private int masterConnectionMinimumIdleSize = 8;
        private int masterConnectionPoolSize = 32;
        private int slaveConnectionMinimumIdleSize = 8;
        private int slaveConnectionPoolSize = 32;
    }
}
```

下面的配置类根据 `app.redisson.mode` 创建对应模式的 `RedissonClient`。

文件位置：`src/main/java/io/github/atengk/redisson/config/RedissonClientConfiguration.java`

```java
package io.github.atengk.redisson.config;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.StrUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.redisson.Redisson;
import org.redisson.api.RedissonClient;
import org.redisson.config.BaseConfig;
import org.redisson.config.ClusterServersConfig;
import org.redisson.config.Config;
import org.redisson.config.EqualJitterDelay;
import org.redisson.config.ReadMode;
import org.redisson.config.SentinelServersConfig;
import org.redisson.config.SingleServerConfig;
import org.redisson.config.SubscriptionMode;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Duration;

/**
 * Redisson 客户端初始化配置。
 *
 * @author Ateng
 * @since 2026-05-06
 */
@Slf4j
@Configuration
@RequiredArgsConstructor
@EnableConfigurationProperties(RedissonClientProperties.class)
public class RedissonClientConfiguration {

    private final RedissonClientProperties properties;

    /**
     * 创建 RedissonClient 客户端。
     *
     * @return Redisson 客户端
     */
    @Bean(destroyMethod = "shutdown")
    @ConditionalOnMissingBean(RedissonClient.class)
    public RedissonClient redissonClient() {
        Config config = new Config();
        applyGlobalConfig(config);

        switch (properties.getMode()) {
            case SINGLE -> applySingleServerConfig(config);
            case SENTINEL -> applySentinelServersConfig(config);
            case CLUSTER -> applyClusterServersConfig(config);
            default -> throw new IllegalArgumentException("不支持的Redisson模式：" + properties.getMode());
        }

        RedissonClient redissonClient = Redisson.create(config);
        log.info("Redisson客户端初始化完成，连接模式：{}", properties.getMode());
        return redissonClient;
    }

    /**
     * 应用全局配置。
     *
     * @param config Redisson 配置对象
     */
    private void applyGlobalConfig(Config config) {
        if (properties.getThreads() > 0) {
            config.setThreads(properties.getThreads());
        }
        if (properties.getNettyThreads() > 0) {
            config.setNettyThreads(properties.getNettyThreads());
        }
    }

    /**
     * 应用单机模式配置。
     *
     * @param config Redisson 配置对象
     */
    private void applySingleServerConfig(Config config) {
        RedissonClientProperties.Single single = properties.getSingle();
        if (StrUtil.isBlank(single.getAddress())) {
            throw new IllegalArgumentException("Redisson单机模式Redis地址不能为空");
        }

        SingleServerConfig singleServerConfig = config.useSingleServer()
                .setAddress(single.getAddress())
                .setDatabase(single.getDatabase())
                .setConnectionMinimumIdleSize(single.getConnectionMinimumIdleSize())
                .setConnectionPoolSize(single.getConnectionPoolSize());

        applyBaseConfig(singleServerConfig);
    }

    /**
     * 应用哨兵模式配置。
     *
     * @param config Redisson 配置对象
     */
    private void applySentinelServersConfig(Config config) {
        RedissonClientProperties.Sentinel sentinel = properties.getSentinel();
        if (StrUtil.isBlank(sentinel.getMasterName())) {
            throw new IllegalArgumentException("Redisson哨兵模式masterName不能为空");
        }
        if (CollUtil.isEmpty(sentinel.getSentinelAddresses())) {
            throw new IllegalArgumentException("Redisson哨兵模式sentinelAddresses不能为空");
        }

        SentinelServersConfig sentinelServersConfig = config.useSentinelServers()
                .setMasterName(sentinel.getMasterName())
                .addSentinelAddress(sentinel.getSentinelAddresses().toArray(String[]::new))
                .setDatabase(sentinel.getDatabase())
                .setReadMode(ReadMode.valueOf(sentinel.getReadMode()))
                .setSubscriptionMode(SubscriptionMode.valueOf(sentinel.getSubscriptionMode()))
                .setMasterConnectionMinimumIdleSize(sentinel.getMasterConnectionMinimumIdleSize())
                .setMasterConnectionPoolSize(sentinel.getMasterConnectionPoolSize())
                .setSlaveConnectionMinimumIdleSize(sentinel.getSlaveConnectionMinimumIdleSize())
                .setSlaveConnectionPoolSize(sentinel.getSlaveConnectionPoolSize());

        applyBaseConfig(sentinelServersConfig);
    }

    /**
     * 应用集群模式配置。
     *
     * @param config Redisson 配置对象
     */
    private void applyClusterServersConfig(Config config) {
        RedissonClientProperties.Cluster cluster = properties.getCluster();
        if (CollUtil.isEmpty(cluster.getNodeAddresses())) {
            throw new IllegalArgumentException("Redisson集群模式nodeAddresses不能为空");
        }

        ClusterServersConfig clusterServersConfig = config.useClusterServers()
                .addNodeAddress(cluster.getNodeAddresses().toArray(String[]::new))
                .setScanInterval(cluster.getScanInterval())
                .setReadMode(ReadMode.valueOf(cluster.getReadMode()))
                .setSubscriptionMode(SubscriptionMode.valueOf(cluster.getSubscriptionMode()))
                .setMasterConnectionMinimumIdleSize(cluster.getMasterConnectionMinimumIdleSize())
                .setMasterConnectionPoolSize(cluster.getMasterConnectionPoolSize())
                .setSlaveConnectionMinimumIdleSize(cluster.getSlaveConnectionMinimumIdleSize())
                .setSlaveConnectionPoolSize(cluster.getSlaveConnectionPoolSize());

        applyBaseConfig(clusterServersConfig);
    }

    /**
     * 应用 Redisson 基础连接配置。
     *
     * @param baseConfig 基础配置对象
     */
    private void applyBaseConfig(BaseConfig<?> baseConfig) {
        baseConfig.setTimeout(toMillis(properties.getTimeout()))
                .setConnectTimeout(toMillis(properties.getConnectTimeout()))
                .setRetryAttempts(properties.getRetryAttempts())
                .setRetryDelay(new EqualJitterDelay(properties.getRetryMinDelay(), properties.getRetryMaxDelay()));

        if (StrUtil.isNotBlank(properties.getUsername())) {
            baseConfig.setUsername(properties.getUsername());
        }
        if (StrUtil.isNotBlank(properties.getPassword())) {
            baseConfig.setPassword(properties.getPassword());
        }
        if (StrUtil.isNotBlank(properties.getClientName())) {
            baseConfig.setClientName(properties.getClientName());
        }
    }

    /**
     * 将 Duration 转换为毫秒整数。
     *
     * @param duration 时间配置
     * @return 毫秒数
     */
    private int toMillis(Duration duration) {
        return Math.toIntExact(duration.toMillis());
    }
}
```

自定义配置方式需要注意两点。第一，不建议同时配置 `spring.redis.redisson.file` 和自定义 `RedissonClient` Bean，否则容易出现配置来源混乱。第二，当前示例使用 `@ConditionalOnMissingBean(RedissonClient.class)`，如果项目中已经存在其他 `RedissonClient` Bean，本配置不会重复创建客户端。

### 客户端生命周期管理

`RedissonClient` 内部维护 Redis 连接池、订阅连接、Netty 线程等资源，必须由 Spring 容器统一管理。使用 Starter 自动配置时，客户端生命周期通常由自动配置负责；自定义 Bean 时，应通过 `@Bean(destroyMethod = "shutdown")` 在应用关闭时释放资源。

推荐生命周期管理方式如下：

| 场景               | 推荐方式                            | 说明                      |
| ------------------ | ----------------------------------- | ------------------------- |
| Starter 自动配置   | 直接注入 `RedissonClient`           | 不手动 `shutdown()`       |
| 自定义 Bean        | `@Bean(destroyMethod = "shutdown")` | Spring 容器关闭时自动释放 |
| 临时代码或测试代码 | `try-finally` 调用 `shutdown()`     | 避免测试进程残留连接      |
| 多客户端场景       | 明确 Bean 名称和关闭策略            | 避免连接泄漏和重复初始化  |

测试代码中如果手动创建客户端，需要显式关闭。

文件位置：`src/test/java/io/github/atengk/redisson/RedissonManualClientTest.java`

```java
package io.github.atengk.redisson;

import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Test;
import org.redisson.Redisson;
import org.redisson.api.RBucket;
import org.redisson.api.RedissonClient;
import org.redisson.config.Config;

import java.util.concurrent.TimeUnit;

/**
 * Redisson 手动客户端生命周期测试。
 *
 * @author Ateng
 * @since 2026-05-06
 */
@Slf4j
class RedissonManualClientTest {

    /**
     * 验证手动创建 RedissonClient 后可以正常关闭。
     */
    @Test
    void testManualClientShutdown() {
        RedissonClient redissonClient = null;
        try {
            Config config = new Config();
            config.useSingleServer()
                    .setAddress("redis://127.0.0.1:6379")
                    .setDatabase(0)
                    .setTimeout(3000)
                    .setConnectTimeout(3000);

            redissonClient = Redisson.create(config);

            RBucket<String> bucket = redissonClient.getBucket("redisson:test:manual");
            bucket.set("manual-client-ok", 30, TimeUnit.SECONDS);

            log.info("Redisson手动客户端测试完成，value：{}", bucket.get());
        } finally {
            if (redissonClient != null) {
                redissonClient.shutdown();
                log.info("Redisson手动客户端已关闭");
            }
        }
    }
}
```

可以通过 Redis 客户端连接列表验证客户端是否正常建立连接：

```bash
# 查看 Redis 客户端连接
redis-cli CLIENT LIST

# 如果配置了 clientName，可以按应用名称过滤
redis-cli CLIENT LIST | grep springboot3-redisson
```

生产环境中不建议在业务方法内频繁创建和关闭 `RedissonClient`。正确做法是将 `RedissonClient` 作为单例 Bean 交给 Spring 管理，业务层只负责注入和使用。频繁创建客户端会导致连接池重复初始化、Netty 线程重复创建，并增加 Redis 连接数压力。



## 常用数据结构开发

本节用于说明 Redisson 常用分布式数据结构的开发方式。Redisson 提供的 `RBucket`、`RMap`、`RList`、`RSet`、`RQueue` 等对象都基于 Redis 存储，但使用方式更接近 Java 原生对象或集合。`RBucket` 是 Redis/Valkey 上的对象持有器，单个对象大小限制为 512MB，并且是线程安全对象；`RMap`、`RSet`、`RList`、`RQueue` 也分别对齐 Java 中的 `ConcurrentMap`、`Set`、`List`、`Queue` 使用模型。([Redisson](https://redisson.pro/docs/data-and-services/objects/?utm_source=chatgpt.com))

示例文件结构如下：

```text
src/main/java/io/github/atengk/redisson/service/DataStructureDemoService.java
src/main/java/io/github/atengk/redisson/controller/DataStructureDemoController.java
```

下面的服务类集中演示 Bucket、Map、List、Set、Queue 的基础操作。

文件位置：`src/main/java/io/github/atengk/redisson/service/DataStructureDemoService.java`

```java
package io.github.atengk.redisson.service;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.IdUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.json.JSONObject;
import cn.hutool.json.JSONUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.redisson.api.RBucket;
import org.redisson.api.RList;
import org.redisson.api.RMap;
import org.redisson.api.RQueue;
import org.redisson.api.RSet;
import org.redisson.api.RedissonClient;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

/**
 * Redisson 常用数据结构示例服务。
 *
 * @author Ateng
 * @since 2026-05-06
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class DataStructureDemoService {

    private final RedissonClient redissonClient;

    /**
     * 使用 RBucket 保存用户资料。
     *
     * @param userId 用户ID
     * @param profile 用户资料
     * @return 保存后的 JSON 字符串
     */
    public String saveBucketProfile(String userId, Map<String, Object> profile) {
        if (StrUtil.isBlank(userId)) {
            throw new IllegalArgumentException("用户ID不能为空");
        }
        if (CollUtil.isEmpty(profile)) {
            throw new IllegalArgumentException("用户资料不能为空");
        }

        String key = "redisson:bucket:user:profile:" + userId;
        String value = JSONUtil.toJsonStr(profile);

        RBucket<String> bucket = redissonClient.getBucket(key);
        bucket.set(value, 30, TimeUnit.MINUTES);

        log.info("Bucket用户资料保存成功，key：{}", key);
        return value;
    }

    /**
     * 使用 RBucket 查询用户资料。
     *
     * @param userId 用户ID
     * @return 用户资料
     */
    public JSONObject getBucketProfile(String userId) {
        if (StrUtil.isBlank(userId)) {
            throw new IllegalArgumentException("用户ID不能为空");
        }

        String key = "redisson:bucket:user:profile:" + userId;
        RBucket<String> bucket = redissonClient.getBucket(key);
        String value = bucket.get();

        if (StrUtil.isBlank(value)) {
            log.info("Bucket用户资料不存在，key：{}", key);
            return new JSONObject();
        }

        return JSONUtil.parseObj(value);
    }

    /**
     * 使用 RMap 保存用户资料。
     *
     * @param userId 用户ID
     * @param profile 用户资料
     * @return 是否新增或覆盖成功
     */
    public Boolean putUserProfileToMap(String userId, Map<String, Object> profile) {
        if (StrUtil.isBlank(userId)) {
            throw new IllegalArgumentException("用户ID不能为空");
        }
        if (CollUtil.isEmpty(profile)) {
            throw new IllegalArgumentException("用户资料不能为空");
        }

        RMap<String, String> profileMap = redissonClient.getMap("redisson:map:user:profile");
        boolean result = profileMap.fastPut(userId, JSONUtil.toJsonStr(profile));

        log.info("Map用户资料写入完成，userId：{}，result：{}", userId, result);
        return result;
    }

    /**
     * 使用 RMap 查询用户资料。
     *
     * @param userId 用户ID
     * @return 用户资料
     */
    public JSONObject getUserProfileFromMap(String userId) {
        if (StrUtil.isBlank(userId)) {
            throw new IllegalArgumentException("用户ID不能为空");
        }

        RMap<String, String> profileMap = redissonClient.getMap("redisson:map:user:profile");
        String value = profileMap.get(userId);

        if (StrUtil.isBlank(value)) {
            log.info("Map用户资料不存在，userId：{}", userId);
            return new JSONObject();
        }

        return JSONUtil.parseObj(value);
    }

    /**
     * 使用 RList 追加用户访问日志。
     *
     * @param userId 用户ID
     * @param logs 访问日志
     * @return 当前全部日志
     */
    public List<String> appendAccessLogs(String userId, List<String> logs) {
        if (StrUtil.isBlank(userId)) {
            throw new IllegalArgumentException("用户ID不能为空");
        }
        if (CollUtil.isEmpty(logs)) {
            return List.of();
        }

        List<String> normalizedLogs = logs.stream()
                .filter(StrUtil::isNotBlank)
                .toList();

        if (CollUtil.isEmpty(normalizedLogs)) {
            return List.of();
        }

        RList<String> accessLogList = redissonClient.getList("redisson:list:user:access:" + userId);
        accessLogList.addAll(normalizedLogs);

        log.info("List用户访问日志追加完成，userId：{}，count：{}", userId, normalizedLogs.size());
        return accessLogList.readAll();
    }

    /**
     * 使用 RSet 保存用户标签。
     *
     * @param userId 用户ID
     * @param tags 用户标签
     * @return 去重后的标签列表
     */
    public List<String> addUserTags(String userId, List<String> tags) {
        if (StrUtil.isBlank(userId)) {
            throw new IllegalArgumentException("用户ID不能为空");
        }
        if (CollUtil.isEmpty(tags)) {
            return List.of();
        }

        List<String> normalizedTags = tags.stream()
                .filter(StrUtil::isNotBlank)
                .distinct()
                .toList();

        RSet<String> tagSet = redissonClient.getSet("redisson:set:user:tags:" + userId);
        tagSet.addAll(normalizedTags);

        log.info("Set用户标签写入完成，userId：{}，count：{}", userId, normalizedTags.size());
        return new ArrayList<>(tagSet);
    }

    /**
     * 使用 RQueue 投递订单任务。
     *
     * @param orderNo 订单号
     * @return 任务ID
     */
    public String offerOrderTask(String orderNo) {
        if (StrUtil.isBlank(orderNo)) {
            throw new IllegalArgumentException("订单号不能为空");
        }

        String taskId = IdUtil.fastSimpleUUID();
        JSONObject payload = JSONUtil.createObj()
                .set("taskId", taskId)
                .set("orderNo", orderNo)
                .set("type", "ORDER_PENDING_PROCESS");

        RQueue<String> queue = redissonClient.getQueue("redisson:queue:order:pending");
        boolean result = queue.offer(payload.toString());

        log.info("Queue订单任务投递完成，orderNo：{}，taskId：{}，result：{}", orderNo, taskId, result);
        return taskId;
    }

    /**
     * 使用 RQueue 拉取订单任务。
     *
     * @return 订单任务
     */
    public JSONObject pollOrderTask() {
        RQueue<String> queue = redissonClient.getQueue("redisson:queue:order:pending");
        String payload = queue.poll();

        if (StrUtil.isBlank(payload)) {
            log.info("Queue订单任务为空");
            return new JSONObject();
        }

        log.info("Queue订单任务拉取完成，payload：{}", payload);
        return JSONUtil.parseObj(payload);
    }
}
```

下面的控制器用于通过 HTTP 接口验证常用数据结构操作。

文件位置：`src/main/java/io/github/atengk/redisson/controller/DataStructureDemoController.java`

```java
package io.github.atengk.redisson.controller;

import cn.hutool.json.JSONObject;
import io.github.atengk.redisson.service.DataStructureDemoService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * Redisson 常用数据结构示例接口。
 *
 * @author Ateng
 * @since 2026-05-06
 */
@RestController
@RequiredArgsConstructor
@RequestMapping("/redisson/structure")
public class DataStructureDemoController {

    private final DataStructureDemoService dataStructureDemoService;

    /**
     * 保存 Bucket 用户资料。
     *
     * @param userId 用户ID
     * @param profile 用户资料
     * @return 保存结果
     */
    @PostMapping("/bucket/{userId}")
    public String saveBucketProfile(@PathVariable String userId, @RequestBody Map<String, Object> profile) {
        return dataStructureDemoService.saveBucketProfile(userId, profile);
    }

    /**
     * 查询 Bucket 用户资料。
     *
     * @param userId 用户ID
     * @return 用户资料
     */
    @GetMapping("/bucket/{userId}")
    public JSONObject getBucketProfile(@PathVariable String userId) {
        return dataStructureDemoService.getBucketProfile(userId);
    }

    /**
     * 保存 Map 用户资料。
     *
     * @param userId 用户ID
     * @param profile 用户资料
     * @return 是否成功
     */
    @PostMapping("/map/{userId}")
    public Boolean putUserProfileToMap(@PathVariable String userId, @RequestBody Map<String, Object> profile) {
        return dataStructureDemoService.putUserProfileToMap(userId, profile);
    }

    /**
     * 查询 Map 用户资料。
     *
     * @param userId 用户ID
     * @return 用户资料
     */
    @GetMapping("/map/{userId}")
    public JSONObject getUserProfileFromMap(@PathVariable String userId) {
        return dataStructureDemoService.getUserProfileFromMap(userId);
    }

    /**
     * 追加 List 访问日志。
     *
     * @param userId 用户ID
     * @param logs 日志列表
     * @return 当前日志列表
     */
    @PostMapping("/list/{userId}")
    public List<String> appendAccessLogs(@PathVariable String userId, @RequestBody List<String> logs) {
        return dataStructureDemoService.appendAccessLogs(userId, logs);
    }

    /**
     * 保存 Set 用户标签。
     *
     * @param userId 用户ID
     * @param tags 标签列表
     * @return 去重后的标签列表
     */
    @PostMapping("/set/{userId}")
    public List<String> addUserTags(@PathVariable String userId, @RequestBody List<String> tags) {
        return dataStructureDemoService.addUserTags(userId, tags);
    }

    /**
     * 投递 Queue 订单任务。
     *
     * @param orderNo 订单号
     * @return 任务ID
     */
    @PostMapping("/queue/{orderNo}")
    public String offerOrderTask(@PathVariable String orderNo) {
        return dataStructureDemoService.offerOrderTask(orderNo);
    }

    /**
     * 拉取 Queue 订单任务。
     *
     * @return 订单任务
     */
    @GetMapping("/queue")
    public JSONObject pollOrderTask() {
        return dataStructureDemoService.pollOrderTask();
    }
}
```

### Bucket 对象操作

`RBucket` 适合保存单个对象，例如验证码、登录会话、临时配置、用户快照、接口调用结果等。它更接近 Redis String 的对象化封装，适合通过一个 Key 保存一个完整值。Redisson 官方文档将 `RBucket` 定义为 Redis/Valkey 上任意对象的 holder，支持 `set`、`get`、`trySet`、`compareAndSet`、`getAndSet` 等操作。([Redisson](https://redisson.pro/docs/data-and-services/objects/?utm_source=chatgpt.com))

验证 Bucket 写入：

```bash
curl -X POST "http://localhost:8080/redisson/structure/bucket/1001" \
  -H "Content-Type: application/json" \
  -d '{"name":"Ateng","level":"VIP","source":"bucket"}'
```

验证 Bucket 读取：

```bash
curl "http://localhost:8080/redisson/structure/bucket/1001"
```

适用建议：

| 场景                      | 建议                                                   |
| ------------------------- | ------------------------------------------------------ |
| 一个 Key 对应一个完整对象 | 使用 `RBucket`                                         |
| 需要设置整体过期时间      | 使用 `bucket.set(value, timeout, unit)`                |
| 需要局部字段更新          | 不建议使用 `RBucket`，优先考虑 `RMap`                  |
| 大对象存储                | 谨慎使用，避免超过 Redis 单 Key 过大导致网络和内存压力 |

### Map 集合操作

`RMap` 适合保存按业务主键索引的数据，例如用户资料、商品快照、配置项、库存缓存等。Redisson 的 `RMap` 实现了 `ConcurrentMap` 接口，是线程安全对象；对于不关心旧值的写入、删除操作，可以优先使用 `fastPut`、`fastRemove`，减少返回旧值带来的额外开销。([Redisson](https://redisson.pro/docs/data-and-services/collections/?utm_source=chatgpt.com))

验证 Map 写入：

```bash
curl -X POST "http://localhost:8080/redisson/structure/map/1001" \
  -H "Content-Type: application/json" \
  -d '{"name":"Ateng","level":"VIP","source":"map"}'
```

验证 Map 读取：

```bash
curl "http://localhost:8080/redisson/structure/map/1001"
```

适用建议：

| 场景                       | 建议                                                   |
| -------------------------- | ------------------------------------------------------ |
| 一个集合中保存多个业务对象 | 使用 `RMap`                                            |
| 只需要判断写入是否成功     | 使用 `fastPut`                                         |
| 需要获取旧值               | 使用 `put`                                             |
| 需要按字段维度组织缓存     | 使用 `RMap` 比多个散列 Key 更易管理                    |
| 高频读取                   | 可进一步考虑 `RLocalCachedMap`，但要关注本地缓存一致性 |

### List 集合操作

`RList` 适合保存有序数据，例如访问日志、操作记录、最近浏览列表、步骤流转记录等。Redisson 的 `RList` 实现了 Java `List` 接口，支持按索引访问、范围读取、追加、插入、删除等操作。([Redisson](https://redisson.pro/docs/data-and-services/collections/?utm_source=chatgpt.com))

验证 List 追加：

```bash
curl -X POST "http://localhost:8080/redisson/structure/list/1001" \
  -H "Content-Type: application/json" \
  -d '["login","view-product","submit-order"]'
```

适用建议：

| 场景               | 建议                                    |
| ------------------ | --------------------------------------- |
| 需要保持插入顺序   | 使用 `RList`                            |
| 需要按索引读取     | 使用 `RList`                            |
| 需要去重           | 不建议使用 `RList`，优先使用 `RSet`     |
| 只需要队列先进先出 | 优先使用 `RQueue`                       |
| 列表可能无限增长   | 需要配合 `trim`、过期时间或业务清理策略 |

### Set 集合操作

`RSet` 适合保存无序且去重的数据，例如用户标签、权限标识、去重 ID、黑名单、白名单等。Redisson 的 `RSet` 实现了 Java `Set` 接口，元素唯一性基于序列化后的值状态，而不是对象本身的 `hashCode()` 或 `equals()`。([Redisson](https://redisson.pro/docs/data-and-services/collections/?utm_source=chatgpt.com))

验证 Set 写入：

```bash
curl -X POST "http://localhost:8080/redisson/structure/set/1001" \
  -H "Content-Type: application/json" \
  -d '["JAVA","REDIS","REDIS","SPRING_BOOT"]'
```

返回结果中 `REDIS` 只会保留一份。

适用建议：

| 场景                       | 建议                                    |
| -------------------------- | --------------------------------------- |
| 需要天然去重               | 使用 `RSet`                             |
| 需要判断元素是否存在       | 使用 `contains`                         |
| 需要保存用户标签或权限编码 | 使用 `RSet`                             |
| 需要排序                   | 使用 `RSortedSet` 或 `RScoredSortedSet` |
| 需要统计交并差             | 可结合 Redisson Set 相关集合操作封装    |

### Queue 队列操作

`RQueue` 适合简单的 FIFO 队列场景，例如轻量任务投递、临时任务缓冲、简单异步处理等。Redisson 官方文档说明，`RQueue` 实现了 `java.util.Queue` 接口，是线程安全对象，但它不具备可靠队列中的确认、可见性超时、投递保证等能力；需要强可靠消息处理时，应使用 MQ 或 Redisson PRO 的 Reliable Queue。([Redisson](https://redisson.pro/docs/data-and-services/queues/index.html?utm_source=chatgpt.com))

验证 Queue 投递：

```bash
curl -X POST "http://localhost:8080/redisson/structure/queue/ORDER202605060001"
```

验证 Queue 拉取：

```bash
curl "http://localhost:8080/redisson/structure/queue"
```

适用建议：

| 场景             | 建议                                     |
| ---------------- | ---------------------------------------- |
| 简单先进先出任务 | 使用 `RQueue`                            |
| 消费失败必须重试 | 不建议只用 `RQueue`                      |
| 需要延迟消费     | 后续章节使用延迟队列方案                 |
| 需要消息确认机制 | 优先使用 RabbitMQ、Kafka 或可靠队列      |
| 任务不能丢失     | 不建议使用普通 `RQueue` 单独承载核心链路 |

## 分布式锁开发

本节用于说明 Redisson 分布式锁的常用开发方式。Redisson 的 `RLock` 是基于 Redis/Valkey 的分布式可重入锁，并实现了 Java `Lock` 接口；它通过发布订阅通知其他 Redisson 实例中等待获取锁的线程。Redisson 还提供公平锁、读写锁、联锁等锁类型。([Redisson](https://redisson.pro/docs/data-and-services/locks-and-synchronizers/index.html?utm_source=chatgpt.com))

示例文件结构如下：

```text
src/main/java/io/github/atengk/redisson/service/DistributedLockDemoService.java
src/main/java/io/github/atengk/redisson/controller/DistributedLockDemoController.java
```

下面的服务类集中演示可重入锁、公平锁、读写锁、联锁，以及带超时时间的锁使用方式。

文件位置：`src/main/java/io/github/atengk/redisson/service/DistributedLockDemoService.java`

```java
package io.github.atengk.redisson.service;

import cn.hutool.core.util.StrUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.redisson.api.RLock;
import org.redisson.api.RReadWriteLock;
import org.redisson.api.RedissonClient;
import org.springframework.stereotype.Service;

import java.util.concurrent.TimeUnit;
import java.util.function.Supplier;

/**
 * Redisson 分布式锁示例服务。
 *
 * @author Ateng
 * @since 2026-05-06
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class DistributedLockDemoService {

    private final RedissonClient redissonClient;

    /**
     * 使用可重入锁处理订单提交。
     *
     * @param orderNo 订单号
     * @return 处理结果
     */
    public String submitOrderWithReentrantLock(String orderNo) {
        if (StrUtil.isBlank(orderNo)) {
            throw new IllegalArgumentException("订单号不能为空");
        }

        String lockKey = "redisson:lock:order:submit:" + orderNo;
        RLock lock = redissonClient.getLock(lockKey);
        boolean locked = false;

        try {
            locked = lock.tryLock(3, 10, TimeUnit.SECONDS);
            if (!locked) {
                log.info("订单提交锁获取失败，orderNo：{}", orderNo);
                return "订单正在处理中，请勿重复提交";
            }

            log.info("订单提交锁获取成功，orderNo：{}", orderNo);
            return "订单提交成功：" + orderNo;
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            log.warn("订单提交锁等待被中断，orderNo：{}", orderNo, e);
            return "订单提交被中断";
        } finally {
            unlockSafely(lock, locked, lockKey);
        }
    }

    /**
     * 使用公平锁处理排队业务。
     *
     * @param bizNo 业务编号
     * @return 处理结果
     */
    public String processWithFairLock(String bizNo) {
        if (StrUtil.isBlank(bizNo)) {
            throw new IllegalArgumentException("业务编号不能为空");
        }

        String lockKey = "redisson:lock:fair:biz:" + bizNo;
        RLock lock = redissonClient.getFairLock(lockKey);
        boolean locked = false;

        try {
            locked = lock.tryLock(5, 10, TimeUnit.SECONDS);
            if (!locked) {
                log.info("公平锁获取失败，bizNo：{}", bizNo);
                return "业务繁忙，请稍后重试";
            }

            log.info("公平锁获取成功，bizNo：{}", bizNo);
            return "公平锁业务处理成功：" + bizNo;
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            log.warn("公平锁等待被中断，bizNo：{}", bizNo, e);
            return "业务处理被中断";
        } finally {
            unlockSafely(lock, locked, lockKey);
        }
    }

    /**
     * 使用读锁读取库存。
     *
     * @param skuCode 商品编码
     * @return 读取结果
     */
    public String readInventory(String skuCode) {
        if (StrUtil.isBlank(skuCode)) {
            throw new IllegalArgumentException("商品编码不能为空");
        }

        RReadWriteLock readWriteLock = redissonClient.getReadWriteLock("redisson:lock:inventory:" + skuCode);
        RLock readLock = readWriteLock.readLock();
        boolean locked = false;

        try {
            locked = readLock.tryLock(2, 5, TimeUnit.SECONDS);
            if (!locked) {
                return "库存读取繁忙，请稍后重试";
            }

            log.info("库存读锁获取成功，skuCode：{}", skuCode);
            return "库存读取成功：" + skuCode;
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            log.warn("库存读锁等待被中断，skuCode：{}", skuCode, e);
            return "库存读取被中断";
        } finally {
            unlockSafely(readLock, locked, "redisson:lock:inventory:" + skuCode + ":read");
        }
    }

    /**
     * 使用写锁更新库存。
     *
     * @param skuCode 商品编码
     * @param delta 库存变更数量
     * @return 更新结果
     */
    public String updateInventory(String skuCode, Integer delta) {
        if (StrUtil.isBlank(skuCode)) {
            throw new IllegalArgumentException("商品编码不能为空");
        }
        if (delta == null || delta == 0) {
            throw new IllegalArgumentException("库存变更数量不能为空或0");
        }

        RReadWriteLock readWriteLock = redissonClient.getReadWriteLock("redisson:lock:inventory:" + skuCode);
        RLock writeLock = readWriteLock.writeLock();
        boolean locked = false;

        try {
            locked = writeLock.tryLock(3, 10, TimeUnit.SECONDS);
            if (!locked) {
                return "库存更新繁忙，请稍后重试";
            }

            log.info("库存写锁获取成功，skuCode：{}，delta：{}", skuCode, delta);
            return "库存更新成功：" + skuCode + "，变更数量：" + delta;
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            log.warn("库存写锁等待被中断，skuCode：{}", skuCode, e);
            return "库存更新被中断";
        } finally {
            unlockSafely(writeLock, locked, "redisson:lock:inventory:" + skuCode + ":write");
        }
    }

    /**
     * 使用联锁同时锁定订单和库存。
     *
     * @param orderNo 订单号
     * @param skuCode 商品编码
     * @return 处理结果
     */
    public String processWithMultiLock(String orderNo, String skuCode) {
        if (StrUtil.isBlank(orderNo)) {
            throw new IllegalArgumentException("订单号不能为空");
        }
        if (StrUtil.isBlank(skuCode)) {
            throw new IllegalArgumentException("商品编码不能为空");
        }

        RLock orderLock = redissonClient.getLock("redisson:lock:order:" + orderNo);
        RLock inventoryLock = redissonClient.getLock("redisson:lock:inventory:" + skuCode);
        RLock multiLock = redissonClient.getMultiLock(orderLock, inventoryLock);
        boolean locked = false;

        try {
            locked = multiLock.tryLock(5, 15, TimeUnit.SECONDS);
            if (!locked) {
                log.info("联锁获取失败，orderNo：{}，skuCode：{}", orderNo, skuCode);
                return "订单或库存正在处理中，请稍后重试";
            }

            log.info("联锁获取成功，orderNo：{}，skuCode：{}", orderNo, skuCode);
            return "订单库存联动处理成功：" + orderNo + "，" + skuCode;
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            log.warn("联锁等待被中断，orderNo：{}，skuCode：{}", orderNo, skuCode, e);
            return "联锁业务处理被中断";
        } finally {
            unlockSafely(multiLock, locked, "redisson:lock:multi:" + orderNo + ":" + skuCode);
        }
    }

    /**
     * 使用固定租约时间执行受保护逻辑。
     *
     * @param bizKey 业务Key
     * @param supplier 业务逻辑
     * @param <T> 返回类型
     * @return 业务执行结果
     */
    public <T> T executeWithLeaseTime(String bizKey, Supplier<T> supplier) {
        if (StrUtil.isBlank(bizKey)) {
            throw new IllegalArgumentException("业务Key不能为空");
        }
        if (supplier == null) {
            throw new IllegalArgumentException("业务逻辑不能为空");
        }

        String lockKey = "redisson:lock:lease:" + bizKey;
        RLock lock = redissonClient.getLock(lockKey);
        boolean locked = false;

        try {
            locked = lock.tryLock(2, 15, TimeUnit.SECONDS);
            if (!locked) {
                throw new IllegalStateException("业务正在处理中，请稍后重试");
            }

            log.info("固定租约锁获取成功，bizKey：{}", bizKey);
            return supplier.get();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("固定租约锁等待被中断", e);
        } finally {
            unlockSafely(lock, locked, lockKey);
        }
    }

    /**
     * 安全释放锁。
     *
     * @param lock 锁对象
     * @param locked 是否成功获取锁
     * @param lockKey 锁Key
     */
    private void unlockSafely(RLock lock, boolean locked, String lockKey) {
        if (locked && lock.isHeldByCurrentThread()) {
            lock.unlock();
            log.info("分布式锁释放成功，lockKey：{}", lockKey);
        }
    }
}
```

下面的控制器用于验证分布式锁接口。

文件位置：`src/main/java/io/github/atengk/redisson/controller/DistributedLockDemoController.java`

```java
package io.github.atengk.redisson.controller;

import io.github.atengk.redisson.service.DistributedLockDemoService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

/**
 * Redisson 分布式锁示例接口。
 *
 * @author Ateng
 * @since 2026-05-06
 */
@RestController
@RequiredArgsConstructor
@RequestMapping("/redisson/lock")
public class DistributedLockDemoController {

    private final DistributedLockDemoService distributedLockDemoService;

    /**
     * 使用可重入锁提交订单。
     *
     * @param orderNo 订单号
     * @return 处理结果
     */
    @PostMapping("/order/{orderNo}")
    public String submitOrderWithReentrantLock(@PathVariable String orderNo) {
        return distributedLockDemoService.submitOrderWithReentrantLock(orderNo);
    }

    /**
     * 使用公平锁处理业务。
     *
     * @param bizNo 业务编号
     * @return 处理结果
     */
    @PostMapping("/fair/{bizNo}")
    public String processWithFairLock(@PathVariable String bizNo) {
        return distributedLockDemoService.processWithFairLock(bizNo);
    }

    /**
     * 使用读锁读取库存。
     *
     * @param skuCode 商品编码
     * @return 读取结果
     */
    @GetMapping("/inventory/{skuCode}")
    public String readInventory(@PathVariable String skuCode) {
        return distributedLockDemoService.readInventory(skuCode);
    }

    /**
     * 使用写锁更新库存。
     *
     * @param skuCode 商品编码
     * @param delta 变更数量
     * @return 更新结果
     */
    @PostMapping("/inventory/{skuCode}")
    public String updateInventory(@PathVariable String skuCode, @RequestParam Integer delta) {
        return distributedLockDemoService.updateInventory(skuCode, delta);
    }

    /**
     * 使用联锁处理订单和库存。
     *
     * @param orderNo 订单号
     * @param skuCode 商品编码
     * @return 处理结果
     */
    @PostMapping("/multi/{orderNo}/{skuCode}")
    public String processWithMultiLock(@PathVariable String orderNo, @PathVariable String skuCode) {
        return distributedLockDemoService.processWithMultiLock(orderNo, skuCode);
    }

    /**
     * 使用固定租约时间执行受保护逻辑。
     *
     * @param bizKey 业务Key
     * @return 处理结果
     */
    @PostMapping("/lease/{bizKey}")
    public String executeWithLeaseTime(@PathVariable String bizKey) {
        return distributedLockDemoService.executeWithLeaseTime(bizKey, () -> "固定租约锁业务处理成功：" + bizKey);
    }
}
```

### 可重入锁

可重入锁是 Redisson 最常用的分布式锁类型，通过 `redissonClient.getLock(lockKey)` 获取。它适合防重复提交、定时任务防重入、库存扣减、支付回调幂等等场景。Redisson 官方文档说明，`RLock` 遵循 Java `Lock` 规范，只有持有锁的线程才能释放锁，否则会抛出 `IllegalMonitorStateException`。([Redisson](https://redisson.pro/docs/data-and-services/locks-and-synchronizers/index.html?utm_source=chatgpt.com))

验证接口：

```bash
curl -X POST "http://localhost:8080/redisson/lock/order/ORDER202605060001"
```

开发要点：

| 要点       | 说明                                                     |
| ---------- | -------------------------------------------------------- |
| 锁 Key     | 必须包含明确业务维度，例如订单号、用户ID、商品编码       |
| 获取方式   | 推荐 `tryLock(waitTime, leaseTime, TimeUnit.SECONDS)`    |
| 释放方式   | 必须在 `finally` 中释放                                  |
| 释放前判断 | 使用 `lock.isHeldByCurrentThread()` 避免释放其他线程的锁 |
| 异常处理   | 捕获 `InterruptedException` 后必须恢复中断标记           |

### 公平锁

公平锁通过 `redissonClient.getFairLock(lockKey)` 获取，适合需要按请求顺序处理的业务，例如排队抢号、排队领取资源、排队执行单一业务操作等。Redisson 官方文档说明，公平锁会保证线程按请求顺序获取锁；如果等待队列中的线程死亡，Redisson 会等待该线程返回，每个死亡线程会带来 5 秒等待。([Redisson](https://redisson.pro/docs/data-and-services/locks-and-synchronizers/index.html?utm_source=chatgpt.com))

验证接口：

```bash
curl -X POST "http://localhost:8080/redisson/lock/fair/BIZ202605060001"
```

适用建议：

| 场景                         | 是否建议             |
| ---------------------------- | -------------------- |
| 必须严格按请求顺序处理       | 建议                 |
| 高并发低延迟接口             | 谨慎使用             |
| 普通防重复提交               | 优先使用普通 `RLock` |
| 队列中线程可能频繁超时或死亡 | 谨慎使用             |

### 读写锁

读写锁通过 `redissonClient.getReadWriteLock(lockKey)` 获取，适合读多写少的业务。读锁之间可以并发执行，写锁会与其他读锁、写锁互斥。Redisson 官方文档说明，`RReadWriteLock` 是基于 Redis/Valkey 的分布式可重入读写锁，读锁和写锁都实现了 `RLock` 接口，并允许多个读锁持有者或一个写锁持有者。([Redisson](https://redisson.pro/docs/data-and-services/locks-and-synchronizers/index.html?utm_source=chatgpt.com))

验证读锁：

```bash
curl "http://localhost:8080/redisson/lock/inventory/SKU1001"
```

验证写锁：

```bash
curl -X POST "http://localhost:8080/redisson/lock/inventory/SKU1001?delta=-1"
```

适用建议：

| 场景                           | 建议                     |
| ------------------------------ | ------------------------ |
| 读多写少                       | 使用读写锁               |
| 读操作可以并发，写操作必须独占 | 使用读写锁               |
| 写操作频率很高                 | 普通 `RLock` 可能更简单  |
| 业务逻辑很短                   | 避免为了锁类型复杂化代码 |

### 联锁与红锁

联锁通过 `redissonClient.getMultiLock(lock1, lock2, ...)` 获取，适合一个业务操作必须同时锁定多个资源的场景，例如订单与库存联动、账户转账同时锁定转出账户和转入账户等。Redisson 官方文档说明，`MultiLock` 可以将多个 `RLock` 组合成一个锁对象统一处理，每个 `RLock` 甚至可以来自不同的 Redisson 实例。([Redisson](https://redisson.pro/docs/data-and-services/locks-and-synchronizers/index.html?utm_source=chatgpt.com))

验证联锁：

```bash
curl -X POST "http://localhost:8080/redisson/lock/multi/ORDER202605060001/SKU1001"
```

红锁需要谨慎处理。Redisson 当前官方文档已经明确标记 `RedLock` 为 deprecated，并说明它已被 `RLock` 和 `RFencedLock` 取代。因此新项目不建议继续设计基于 RedLock 的核心链路；如果是历史项目迁移，可以保留兼容性说明，但新增业务应优先使用普通 `RLock`、`MultiLock`，或者在需要防止过期锁持有者继续写入外部系统时使用 `RFencedLock` 的 fencing token 机制。([Redisson](https://redisson.pro/docs/data-and-services/locks-and-synchronizers/index.html?utm_source=chatgpt.com))

适用建议：

| 锁类型         | 建议                                  |
| -------------- | ------------------------------------- |
| `RLock`        | 默认优先使用                          |
| `MultiLock`    | 多资源必须同时锁定时使用              |
| `RedLock`      | 不建议新项目使用                      |
| `RFencedLock`  | 外部系统需要 fencing token 校验时考虑 |
| 数据库唯一约束 | 涉及最终一致性时仍应保留数据库兜底    |

### 锁超时与续期机制

Redisson 锁的超时行为需要区分 `watchdog` 自动续期和显式 `leaseTime`。Redisson 官方文档说明，如果获取锁的 Redisson 实例崩溃，锁可能一直处于持有状态；为了避免这种情况，Redisson 维护了 lock watchdog，在持锁实例存活期间延长锁过期时间，默认 watchdog 超时时间是 30 秒，可通过 `Config.lockWatchdogTimeout` 调整。同时，也可以在加锁时指定 `leaseTime`，到期后锁会自动释放。([Redisson](https://redisson.pro/docs/data-and-services/locks-and-synchronizers/index.html?utm_source=chatgpt.com))

验证固定租约锁：

```bash
curl -X POST "http://localhost:8080/redisson/lock/lease/BIZ202605060001"
```

使用建议如下：

| 加锁方式                             | 行为                               | 适用场景                           |
| ------------------------------------ | ---------------------------------- | ---------------------------------- |
| `lock.lock()`                        | 使用 watchdog 自动续期             | 业务耗时不确定，但必须确保最终释放 |
| `lock.lock(leaseTime, unit)`         | 固定租约，到期自动释放             | 业务耗时可控                       |
| `tryLock(waitTime, leaseTime, unit)` | 等待指定时间获取锁，成功后固定租约 | 推荐用于大多数接口业务             |
| `tryLock()`                          | 立即尝试获取锁                     | 快速失败场景                       |

生产代码建议优先使用：

```java
boolean locked = lock.tryLock(3, 10, TimeUnit.SECONDS);
```

含义如下：

| 参数               | 说明                   |
| ------------------ | ---------------------- |
| `3`                | 最多等待 3 秒获取锁    |
| `10`               | 获取锁后最多持有 10 秒 |
| `TimeUnit.SECONDS` | 时间单位               |

注意事项：

| 问题                         | 建议                                                  |
| ---------------------------- | ----------------------------------------------------- |
| 业务耗时可能超过 `leaseTime` | 调大 `leaseTime`，或不指定 `leaseTime` 使用 watchdog  |
| 业务方法异常退出             | 必须在 `finally` 中释放锁                             |
| 当前线程未持有锁             | 不要直接 `unlock()`，先判断 `isHeldByCurrentThread()` |
| 锁 Key 过粗                  | 会降低并发度，例如所有订单共用一个锁                  |
| 锁 Key 过细                  | 可能无法保护真实临界区                                |
| 锁只保证互斥                 | 不能替代数据库唯一约束、状态机校验和幂等设计          |



## 分布式限流开发

本节用于说明 Redisson 分布式限流器的开发方式。`RRateLimiter` 是基于 Redis 的限流对象，可以限制所有 Redisson 实例整体调用速率，也可以限制同一个 Redisson 实例内的调用速率；该对象线程安全，但不保证公平性。Redisson 官方示例中通过 `redisson.getRateLimiter(name)` 获取限流器，并使用 `trySetRate` 初始化限流规则，再通过 `acquire` 或 `tryAcquire` 获取令牌。([Redisson](https://redisson.pro/docs/data-and-services/objects/?utm_source=chatgpt.com))

示例文件结构如下：

```text
src/main/java/io/github/atengk/redisson/service/RateLimiterDemoService.java
src/main/java/io/github/atengk/redisson/controller/RateLimiterDemoController.java
```

下面的服务类用于封装限流器初始化、令牌获取和接口限流判断。

文件位置：`src/main/java/io/github/atengk/redisson/service/RateLimiterDemoService.java`

```java
package io.github.atengk.redisson.service;

import cn.hutool.core.util.StrUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.redisson.api.RRateLimiter;
import org.redisson.api.RateType;
import org.redisson.api.RedissonClient;
import org.springframework.stereotype.Service;

import java.time.Duration;

/**
 * Redisson 分布式限流示例服务。
 *
 * @author Ateng
 * @since 2026-05-06
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class RateLimiterDemoService {

    private final RedissonClient redissonClient;

    public Boolean initFixedRateLimiter(String limiterName, long rate, long intervalSeconds) {
        if (StrUtil.isBlank(limiterName)) {
            throw new IllegalArgumentException("限流器名称不能为空");
        }
        if (rate <= 0) {
            throw new IllegalArgumentException("限流速率必须大于0");
        }
        if (intervalSeconds <= 0) {
            throw new IllegalArgumentException("限流周期必须大于0秒");
        }

        RRateLimiter limiter = redissonClient.getRateLimiter(limiterName);
        boolean initialized = limiter.trySetRate(RateType.OVERALL, rate, Duration.ofSeconds(intervalSeconds));

        log.info("限流器初始化完成，limiterName：{}，rate：{}，intervalSeconds：{}，initialized：{}",
                limiterName, rate, intervalSeconds, initialized);
        return initialized;
    }

    public Boolean resetFixedRateLimiter(String limiterName, long rate, long intervalSeconds) {
        if (StrUtil.isBlank(limiterName)) {
            throw new IllegalArgumentException("限流器名称不能为空");
        }
        if (rate <= 0) {
            throw new IllegalArgumentException("限流速率必须大于0");
        }
        if (intervalSeconds <= 0) {
            throw new IllegalArgumentException("限流周期必须大于0秒");
        }

        RRateLimiter limiter = redissonClient.getRateLimiter(limiterName);
        limiter.setRate(RateType.OVERALL, rate, Duration.ofSeconds(intervalSeconds));

        log.info("限流器规则重置完成，limiterName：{}，rate：{}，intervalSeconds：{}",
                limiterName, rate, intervalSeconds);
        return true;
    }

    public Boolean tryAcquire(String limiterName, long permits, long timeoutMillis) {
        if (StrUtil.isBlank(limiterName)) {
            throw new IllegalArgumentException("限流器名称不能为空");
        }
        if (permits <= 0) {
            throw new IllegalArgumentException("令牌数量必须大于0");
        }
        if (timeoutMillis < 0) {
            throw new IllegalArgumentException("等待时间不能小于0毫秒");
        }

        RRateLimiter limiter = redissonClient.getRateLimiter(limiterName);
        boolean acquired = timeoutMillis == 0
                ? limiter.tryAcquire(permits)
                : limiter.tryAcquire(permits, Duration.ofMillis(timeoutMillis));

        log.info("限流器令牌获取完成，limiterName：{}，permits：{}，timeoutMillis：{}，acquired：{}",
                limiterName, permits, timeoutMillis, acquired);
        return acquired;
    }

    public String limitApiAccess(String subject) {
        String normalizedSubject = StrUtil.blankToDefault(subject, "anonymous");
        String limiterName = "redisson:rate:api:" + normalizedSubject;

        RRateLimiter limiter = redissonClient.getRateLimiter(limiterName);
        limiter.trySetRate(RateType.OVERALL, 10, Duration.ofMinutes(1));

        boolean acquired = limiter.tryAcquire();
        if (!acquired) {
            log.info("接口访问被限流，subject：{}", normalizedSubject);
            return "访问过于频繁，请稍后重试";
        }

        log.info("接口访问通过限流校验，subject：{}", normalizedSubject);
        return "接口访问成功，subject：" + normalizedSubject;
    }

    public Long availablePermits(String limiterName) {
        if (StrUtil.isBlank(limiterName)) {
            throw new IllegalArgumentException("限流器名称不能为空");
        }

        RRateLimiter limiter = redissonClient.getRateLimiter(limiterName);
        return limiter.availablePermits();
    }
}
```

下面的控制器用于通过 HTTP 接口验证限流器初始化、令牌获取和接口限流效果。

文件位置：`src/main/java/io/github/atengk/redisson/controller/RateLimiterDemoController.java`

```java
package io.github.atengk.redisson.controller;

import io.github.atengk.redisson.service.RateLimiterDemoService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

/**
 * Redisson 分布式限流示例接口。
 *
 * @author Ateng
 * @since 2026-05-06
 */
@RestController
@RequiredArgsConstructor
@RequestMapping("/redisson/rate")
public class RateLimiterDemoController {

    private final RateLimiterDemoService rateLimiterDemoService;

    @PostMapping("/init")
    public Boolean initFixedRateLimiter(@RequestParam String limiterName,
                                        @RequestParam(defaultValue = "5") long rate,
                                        @RequestParam(defaultValue = "10") long intervalSeconds) {
        return rateLimiterDemoService.initFixedRateLimiter(limiterName, rate, intervalSeconds);
    }

    @PostMapping("/reset")
    public Boolean resetFixedRateLimiter(@RequestParam String limiterName,
                                         @RequestParam(defaultValue = "5") long rate,
                                         @RequestParam(defaultValue = "10") long intervalSeconds) {
        return rateLimiterDemoService.resetFixedRateLimiter(limiterName, rate, intervalSeconds);
    }

    @GetMapping("/acquire")
    public Boolean tryAcquire(@RequestParam String limiterName,
                              @RequestParam(defaultValue = "1") long permits,
                              @RequestParam(defaultValue = "0") long timeoutMillis) {
        return rateLimiterDemoService.tryAcquire(limiterName, permits, timeoutMillis);
    }

    @GetMapping("/available")
    public Long availablePermits(@RequestParam String limiterName) {
        return rateLimiterDemoService.availablePermits(limiterName);
    }

    @GetMapping("/api")
    public String limitApiAccess(@RequestParam(defaultValue = "anonymous") String subject) {
        return rateLimiterDemoService.limitApiAccess(subject);
    }
}
```

### RRateLimiter 使用

`RRateLimiter` 的核心流程是：获取限流器对象、初始化限流规则、尝试获取令牌、根据令牌获取结果决定是否执行业务。`trySetRate` 只会在限流器未初始化时设置规则；如果需要覆盖已有规则，应使用 `setRate`。在较新的 Redisson API 中，基于 `Duration` 的 `trySetRate`、`setRate`、`tryAcquire` 方法是推荐方式，旧的 `RateIntervalUnit` 重载已经被标记为过时。([javadoc.io](https://www.javadoc.io/static/org.redisson/redisson/3.40.1/org/redisson/api/RRateLimiter.html?utm_source=chatgpt.com))

初始化限流器：

```bash
curl -X POST "http://localhost:8080/redisson/rate/init?limiterName=redisson:rate:demo&rate=5&intervalSeconds=10"
```

获取令牌：

```bash
curl "http://localhost:8080/redisson/rate/acquire?limiterName=redisson:rate:demo&permits=1&timeoutMillis=0"
```

查看剩余令牌：

```bash
curl "http://localhost:8080/redisson/rate/available?limiterName=redisson:rate:demo"
```

### 固定速率限流

固定速率限流适合控制接口、用户、租户、业务动作在固定时间窗口内的最大访问次数。例如每个用户每分钟最多访问 10 次、每个手机号每分钟最多发送 1 次短信、每个接口每秒最多通过 100 次请求。

常用限流维度如下：

| 维度       | 限流 Key 示例                     | 说明                   |
| ---------- | --------------------------------- | ---------------------- |
| 全局接口   | `redisson:rate:api:/order/submit` | 控制某个接口整体访问量 |
| 用户维度   | `redisson:rate:user:1001`         | 控制单个用户访问频率   |
| IP 维度    | `redisson:rate:ip:127.0.0.1`      | 控制单个 IP 访问频率   |
| 手机号维度 | `redisson:rate:sms:13800138000`   | 控制短信发送频率       |
| 租户维度   | `redisson:rate:tenant:10001`      | 控制租户级调用量       |

重置固定速率规则：

```bash
curl -X POST "http://localhost:8080/redisson/rate/reset?limiterName=redisson:rate:demo&rate=20&intervalSeconds=60"
```

开发建议：

| 场景               | 建议                                 |
| ------------------ | ------------------------------------ |
| 初始化限流规则     | 使用 `trySetRate`                    |
| 管理后台修改规则   | 使用 `setRate`                       |
| 接口快速失败       | 使用 `tryAcquire()`                  |
| 允许短暂等待       | 使用 `tryAcquire(permits, Duration)` |
| 不建议阻塞接口线程 | 谨慎使用 `acquire()`                 |

### 接口限流示例

接口限流示例中，每个 `subject` 每分钟最多允许访问 10 次。`subject` 可以是用户 ID、租户 ID、IP 地址或业务标识。实际项目中建议在网关、拦截器、AOP 或注解中统一处理，而不是散落在每个 Controller 方法中。

验证接口限流：

```bash
for i in $(seq 1 15); do
  curl "http://localhost:8080/redisson/rate/api?subject=user1001"
  echo
done
```

命令说明：

| 命令               | 说明               |
| ------------------ | ------------------ |
| `seq 1 15`         | 连续发起 15 次请求 |
| `subject=user1001` | 使用同一个限流主体 |
| 前 10 次左右       | 正常返回成功       |
| 后续请求           | 返回访问频繁提示   |

接口限流注意事项：

| 问题           | 建议                                              |
| -------------- | ------------------------------------------------- |
| 限流 Key 过粗  | 会误伤其他用户或业务                              |
| 限流 Key 过细  | Redis Key 数量会增长较快                          |
| 规则频繁变化   | 使用管理接口调用 `setRate`                        |
| 限流失败响应   | 建议统一返回业务错误码，例如 `429` 或自定义错误码 |
| 高并发核心接口 | 限流只是一层保护，仍需配合熔断、降级和队列削峰    |

## 分布式延迟队列开发

本节用于说明 Redisson 延迟队列的开发方式。`RDelayedQueue` 是 Redisson 提供的分布式延迟队列接口，`offer(value, delay, timeUnit)` 会将元素按指定延迟时间转移到目标队列中；目标队列通常使用 `RBlockingQueue` 或 `RQueue`。需要注意，Redisson 当前参考文档已经标记 Delayed Queue 对象为 deprecated，并提示使用带 delay 能力的 `RReliableQueue` 替代；本文仍保留 `RDelayedQueue` 示例，用于兼容常见 Spring Boot 项目中的传统实现方式。([javadoc.io](https://www.javadoc.io/static/org.redisson/redisson/3.25.1/org/redisson/api/RDelayedQueue.html?utm_source=chatgpt.com))

示例文件结构如下：

```text
src/main/java/io/github/atengk/redisson/service/DelayQueueDemoService.java
src/main/java/io/github/atengk/redisson/controller/DelayQueueDemoController.java
```

下面的服务类用于实现延迟任务投递、到期任务拉取、任务消费、失败重试和死信队列。

文件位置：`src/main/java/io/github/atengk/redisson/service/DelayQueueDemoService.java`

```java
package io.github.atengk.redisson.service;

import cn.hutool.core.date.DateUtil;
import cn.hutool.core.util.IdUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.json.JSONObject;
import cn.hutool.json.JSONUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.redisson.api.RBlockingQueue;
import org.redisson.api.RDelayedQueue;
import org.redisson.api.RQueue;
import org.redisson.api.RedissonClient;
import org.springframework.stereotype.Service;

import java.util.concurrent.TimeUnit;

/**
 * Redisson 分布式延迟队列示例服务。
 *
 * @author Ateng
 * @since 2026-05-06
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class DelayQueueDemoService {

    private static final String READY_QUEUE_NAME = "redisson:delay:order:ready";
    private static final String DEAD_QUEUE_NAME = "redisson:delay:order:dead";
    private static final int MAX_RETRY_COUNT = 3;

    private final RedissonClient redissonClient;

    public String offerOrderCloseTask(String orderNo, long delaySeconds) {
        if (StrUtil.isBlank(orderNo)) {
            throw new IllegalArgumentException("订单号不能为空");
        }
        if (delaySeconds <= 0) {
            throw new IllegalArgumentException("延迟时间必须大于0秒");
        }

        String taskId = IdUtil.fastSimpleUUID();
        JSONObject payload = JSONUtil.createObj()
                .set("taskId", taskId)
                .set("taskType", "ORDER_CLOSE")
                .set("orderNo", orderNo)
                .set("retryCount", 0)
                .set("createdAt", DateUtil.now());

        RBlockingQueue<String> readyQueue = redissonClient.getBlockingQueue(READY_QUEUE_NAME);
        RDelayedQueue<String> delayedQueue = redissonClient.getDelayedQueue(readyQueue);
        delayedQueue.offer(payload.toString(), delaySeconds, TimeUnit.SECONDS);

        log.info("延迟任务投递成功，taskId：{}，orderNo：{}，delaySeconds：{}", taskId, orderNo, delaySeconds);
        return taskId;
    }

    public JSONObject pollOrderCloseTask(long timeoutSeconds) {
        if (timeoutSeconds < 0) {
            throw new IllegalArgumentException("等待时间不能小于0秒");
        }

        RBlockingQueue<String> readyQueue = redissonClient.getBlockingQueue(READY_QUEUE_NAME);

        try {
            String payload = timeoutSeconds == 0
                    ? readyQueue.poll()
                    : readyQueue.poll(timeoutSeconds, TimeUnit.SECONDS);

            if (StrUtil.isBlank(payload)) {
                log.info("延迟队列暂无到期任务");
                return new JSONObject();
            }

            log.info("延迟队列到期任务拉取成功，payload：{}", payload);
            return JSONUtil.parseObj(payload);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            log.warn("延迟队列任务拉取被中断", e);
            return new JSONObject();
        }
    }

    public String consumeOnce(long timeoutSeconds) {
        JSONObject task = pollOrderCloseTask(timeoutSeconds);
        if (task.isEmpty()) {
            return "暂无到期任务";
        }

        try {
            processOrderCloseTask(task);
            log.info("延迟任务消费成功，taskId：{}，orderNo：{}", task.getStr("taskId"), task.getStr("orderNo"));
            return "延迟任务消费成功：" + task.getStr("taskId");
        } catch (Exception e) {
            log.warn("延迟任务消费失败，taskId：{}，原因：{}", task.getStr("taskId"), e.getMessage(), e);
            handleConsumeFailure(task);
            return "延迟任务消费失败，已进入重试或死信处理：" + task.getStr("taskId");
        }
    }

    private void processOrderCloseTask(JSONObject task) {
        String orderNo = task.getStr("orderNo");
        if (StrUtil.isBlank(orderNo)) {
            throw new IllegalArgumentException("任务订单号不能为空");
        }

        if (StrUtil.startWith(orderNo, "FAIL")) {
            throw new IllegalStateException("模拟订单关闭失败");
        }

        log.info("执行订单关闭业务，orderNo：{}", orderNo);
    }

    private void handleConsumeFailure(JSONObject task) {
        Integer retryCountValue = task.getInt("retryCount");
        int retryCount = retryCountValue == null ? 0 : retryCountValue;

        if (retryCount < MAX_RETRY_COUNT) {
            int nextRetryCount = retryCount + 1;
            task.set("retryCount", nextRetryCount);
            task.set("lastFailedAt", DateUtil.now());

            long retryDelaySeconds = nextRetryCount * 30L;
            RBlockingQueue<String> readyQueue = redissonClient.getBlockingQueue(READY_QUEUE_NAME);
            RDelayedQueue<String> delayedQueue = redissonClient.getDelayedQueue(readyQueue);
            delayedQueue.offer(task.toString(), retryDelaySeconds, TimeUnit.SECONDS);

            log.info("延迟任务已重新投递，taskId：{}，retryCount：{}，retryDelaySeconds：{}",
                    task.getStr("taskId"), nextRetryCount, retryDelaySeconds);
            return;
        }

        RQueue<String> deadQueue = redissonClient.getQueue(DEAD_QUEUE_NAME);
        deadQueue.offer(task.toString());
        log.warn("延迟任务进入死信队列，taskId：{}，retryCount：{}", task.getStr("taskId"), retryCount);
    }
}
```

下面的控制器用于验证延迟任务投递、拉取和消费。

文件位置：`src/main/java/io/github/atengk/redisson/controller/DelayQueueDemoController.java`

```java
package io.github.atengk.redisson.controller;

import cn.hutool.json.JSONObject;
import io.github.atengk.redisson.service.DelayQueueDemoService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

/**
 * Redisson 分布式延迟队列示例接口。
 *
 * @author Ateng
 * @since 2026-05-06
 */
@RestController
@RequiredArgsConstructor
@RequestMapping("/redisson/delay")
public class DelayQueueDemoController {

    private final DelayQueueDemoService delayQueueDemoService;

    @PostMapping("/order/{orderNo}")
    public String offerOrderCloseTask(@PathVariable String orderNo,
                                      @RequestParam(defaultValue = "30") long delaySeconds) {
        return delayQueueDemoService.offerOrderCloseTask(orderNo, delaySeconds);
    }

    @GetMapping("/poll")
    public JSONObject pollOrderCloseTask(@RequestParam(defaultValue = "1") long timeoutSeconds) {
        return delayQueueDemoService.pollOrderCloseTask(timeoutSeconds);
    }

    @PostMapping("/consume")
    public String consumeOnce(@RequestParam(defaultValue = "1") long timeoutSeconds) {
        return delayQueueDemoService.consumeOnce(timeoutSeconds);
    }
}
```

### 延迟队列设计

延迟队列建议拆成两个队列：延迟队列和就绪队列。业务投递任务时写入 `RDelayedQueue`；任务到期后，Redisson 将任务转移到目标 `RBlockingQueue`；消费者只需要从目标队列中拉取到期任务。

推荐设计如下：

| 组件     | 示例名称                     | 作用                         |
| -------- | ---------------------------- | ---------------------------- |
| 延迟队列 | `RDelayedQueue`              | 接收带延迟时间的任务         |
| 就绪队列 | `redisson:delay:order:ready` | 保存已经到期、可消费的任务   |
| 死信队列 | `redisson:delay:order:dead`  | 保存多次失败后不再重试的任务 |
| 任务 ID  | `taskId`                     | 唯一标识一次延迟任务         |
| 业务编号 | `orderNo`                    | 关联具体业务数据             |
| 重试次数 | `retryCount`                 | 控制最大重试次数             |

任务 JSON 示例：

```json
{
  "taskId": "5c2d9f8a1d7b4f2a9c0f3d6b8e1a1234",
  "taskType": "ORDER_CLOSE",
  "orderNo": "ORDER202605060001",
  "retryCount": 0,
  "createdAt": "2026-05-06 10:30:00"
}
```

### 任务投递

任务投递用于将业务任务写入延迟队列。常见业务包括订单超时取消、支付超时关闭、优惠券到期处理、延迟重试、异步状态检查等。

投递一个 10 秒后到期的订单关闭任务：

```bash
curl -X POST "http://localhost:8080/redisson/delay/order/ORDER202605060001?delaySeconds=10"
```

投递失败模拟任务：

```bash
curl -X POST "http://localhost:8080/redisson/delay/order/FAIL_ORDER202605060001?delaySeconds=10"
```

任务投递注意事项：

| 问题         | 建议                                           |
| ------------ | ---------------------------------------------- |
| 任务体过大   | 只保存任务 ID 和必要字段，详细数据从数据库查询 |
| 重复投递     | 业务侧需要幂等控制                             |
| 任务丢失风险 | 核心链路建议配合数据库任务表兜底               |
| 延迟精度     | 不适合要求毫秒级精确调度的场景                 |

### 任务消费

任务消费负责从就绪队列中拉取到期任务并执行业务逻辑。示例中使用接口触发消费，生产环境中可以改为定时任务、独立消费者线程、XXL-JOB 或部署独立 Worker 服务。

手动拉取到期任务：

```bash
curl "http://localhost:8080/redisson/delay/poll?timeoutSeconds=1"
```

手动消费一条到期任务：

```bash
curl -X POST "http://localhost:8080/redisson/delay/consume?timeoutSeconds=1"
```

生产环境建议：

| 场景         | 建议                               |
| ------------ | ---------------------------------- |
| 低频任务     | 使用定时任务轮询消费               |
| 中高频任务   | 使用独立消费者服务                 |
| 任务不能丢失 | 配合数据库任务表、状态机和补偿任务 |
| 多实例消费   | 使用多个消费者实例并发消费就绪队列 |
| 消费幂等     | 按 `taskId` 或业务单号做幂等控制   |

### 异常处理

延迟任务消费失败时，不建议直接丢弃任务。示例代码使用最大重试次数和死信队列处理异常：失败后按 `retryCount * 30` 秒重新投递；超过最大重试次数后进入死信队列，后续由人工或补偿任务处理。

异常处理建议如下：

| 异常类型         | 处理方式                       |
| ---------------- | ------------------------------ |
| 参数异常         | 直接进入死信队列或记录异常任务 |
| 临时网络异常     | 延迟重试                       |
| 下游服务异常     | 延迟重试并限制最大次数         |
| 业务状态不满足   | 查询数据库确认是否需要忽略     |
| 超过最大重试次数 | 写入死信队列并告警             |

需要强调的是，普通 `RDelayedQueue` 不等价于完整可靠消息队列。对于资金、订单、库存等强可靠场景，应保留数据库状态机、幂等表、补偿任务或使用具备确认机制的消息队列。

## 分布式缓存开发

本节用于说明 Redisson 在缓存场景中的常用开发方式。Redisson 提供 `RLocalCachedMap` 用于本地缓存 Map，每个 Redisson 实例维护本地缓存以减少 Redis 网络往返，适合读多写少场景；也提供 `RMapCache`，支持为每个条目设置 TTL 和最大空闲时间。([javadoc.io](https://javadoc.io/static/org.redisson/redisson/3.34.0/org/redisson/api/RLocalCachedMap.html?utm_source=chatgpt.com))

示例文件结构如下：

```text
src/main/java/io/github/atengk/redisson/service/CacheDemoService.java
src/main/java/io/github/atengk/redisson/controller/CacheDemoController.java
```

下面的服务类用于演示本地缓存 Map、带过期时间的 MapCache，以及缓存刷新和删除。

文件位置：`src/main/java/io/github/atengk/redisson/service/CacheDemoService.java`

```java
package io.github.atengk.redisson.service;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.json.JSONObject;
import cn.hutool.json.JSONUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.redisson.api.LocalCachedMapOptions;
import org.redisson.api.RLocalCachedMap;
import org.redisson.api.RMapCache;
import org.redisson.api.RedissonClient;
import org.springframework.stereotype.Service;

import java.util.Map;
import java.util.concurrent.TimeUnit;

/**
 * Redisson 分布式缓存示例服务。
 *
 * @author Ateng
 * @since 2026-05-06
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class CacheDemoService {

    private static final String LOCAL_PRODUCT_CACHE_NAME = "redisson:local-cache:product";
    private static final String EXPIRE_PRODUCT_CACHE_NAME = "redisson:map-cache:product";

    private final RedissonClient redissonClient;

    public Boolean putProductToLocalCache(String productId, Map<String, Object> product) {
        if (StrUtil.isBlank(productId)) {
            throw new IllegalArgumentException("商品ID不能为空");
        }
        if (CollUtil.isEmpty(product)) {
            throw new IllegalArgumentException("商品信息不能为空");
        }

        RLocalCachedMap<String, String> localCachedMap = getProductLocalCachedMap();
        boolean result = localCachedMap.fastPut(productId, JSONUtil.toJsonStr(product));

        log.info("商品本地缓存写入完成，productId：{}，result：{}", productId, result);
        return result;
    }

    public JSONObject getProductFromLocalCache(String productId) {
        if (StrUtil.isBlank(productId)) {
            throw new IllegalArgumentException("商品ID不能为空");
        }

        RLocalCachedMap<String, String> localCachedMap = getProductLocalCachedMap();
        String value = localCachedMap.get(productId);

        if (StrUtil.isBlank(value)) {
            log.info("商品本地缓存未命中，productId：{}", productId);
            return new JSONObject();
        }

        log.info("商品本地缓存命中，productId：{}", productId);
        return JSONUtil.parseObj(value);
    }

    public Boolean putProductWithTtl(String productId, Map<String, Object> product, long ttlSeconds) {
        if (StrUtil.isBlank(productId)) {
            throw new IllegalArgumentException("商品ID不能为空");
        }
        if (CollUtil.isEmpty(product)) {
            throw new IllegalArgumentException("商品信息不能为空");
        }
        if (ttlSeconds <= 0) {
            throw new IllegalArgumentException("缓存过期时间必须大于0秒");
        }

        RMapCache<String, String> mapCache = redissonClient.getMapCache(EXPIRE_PRODUCT_CACHE_NAME);
        boolean result = mapCache.fastPut(productId, JSONUtil.toJsonStr(product), ttlSeconds, TimeUnit.SECONDS);

        log.info("商品过期缓存写入完成，productId：{}，ttlSeconds：{}，result：{}", productId, ttlSeconds, result);
        return result;
    }

    public JSONObject getProductFromTtlCache(String productId) {
        if (StrUtil.isBlank(productId)) {
            throw new IllegalArgumentException("商品ID不能为空");
        }

        RMapCache<String, String> mapCache = redissonClient.getMapCache(EXPIRE_PRODUCT_CACHE_NAME);
        String value = mapCache.get(productId);

        if (StrUtil.isBlank(value)) {
            log.info("商品过期缓存未命中，productId：{}", productId);
            return new JSONObject();
        }

        log.info("商品过期缓存命中，productId：{}", productId);
        return JSONUtil.parseObj(value);
    }

    public Boolean refreshProductCache(String productId, Map<String, Object> product, long ttlSeconds) {
        if (StrUtil.isBlank(productId)) {
            throw new IllegalArgumentException("商品ID不能为空");
        }
        if (CollUtil.isEmpty(product)) {
            throw new IllegalArgumentException("商品信息不能为空");
        }

        String value = JSONUtil.toJsonStr(product);

        RLocalCachedMap<String, String> localCachedMap = getProductLocalCachedMap();
        localCachedMap.fastPut(productId, value);

        RMapCache<String, String> mapCache = redissonClient.getMapCache(EXPIRE_PRODUCT_CACHE_NAME);
        mapCache.fastPut(productId, value, ttlSeconds, TimeUnit.SECONDS);

        log.info("商品缓存刷新完成，productId：{}，ttlSeconds：{}", productId, ttlSeconds);
        return true;
    }

    public Boolean removeProductCache(String productId) {
        if (StrUtil.isBlank(productId)) {
            throw new IllegalArgumentException("商品ID不能为空");
        }

        RLocalCachedMap<String, String> localCachedMap = getProductLocalCachedMap();
        localCachedMap.fastRemove(productId);

        RMapCache<String, String> mapCache = redissonClient.getMapCache(EXPIRE_PRODUCT_CACHE_NAME);
        mapCache.fastRemove(productId);

        log.info("商品缓存删除完成，productId：{}", productId);
        return true;
    }

    @SuppressWarnings("deprecation")
    private RLocalCachedMap<String, String> getProductLocalCachedMap() {
        LocalCachedMapOptions<String, String> options = LocalCachedMapOptions.<String, String>defaults()
                .cacheSize(1000)
                .evictionPolicy(LocalCachedMapOptions.EvictionPolicy.LRU)
                .syncStrategy(LocalCachedMapOptions.SyncStrategy.INVALIDATE)
                .reconnectionStrategy(LocalCachedMapOptions.ReconnectionStrategy.CLEAR);

        return redissonClient.getLocalCachedMap(LOCAL_PRODUCT_CACHE_NAME, options);
    }
}
```

下面的控制器用于验证缓存写入、读取、刷新和删除。

文件位置：`src/main/java/io/github/atengk/redisson/controller/CacheDemoController.java`

```java
package io.github.atengk.redisson.controller;

import cn.hutool.json.JSONObject;
import io.github.atengk.redisson.service.CacheDemoService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * Redisson 分布式缓存示例接口。
 *
 * @author Ateng
 * @since 2026-05-06
 */
@RestController
@RequiredArgsConstructor
@RequestMapping("/redisson/cache")
public class CacheDemoController {

    private final CacheDemoService cacheDemoService;

    @PostMapping("/local/{productId}")
    public Boolean putProductToLocalCache(@PathVariable String productId,
                                          @RequestBody Map<String, Object> product) {
        return cacheDemoService.putProductToLocalCache(productId, product);
    }

    @GetMapping("/local/{productId}")
    public JSONObject getProductFromLocalCache(@PathVariable String productId) {
        return cacheDemoService.getProductFromLocalCache(productId);
    }

    @PostMapping("/ttl/{productId}")
    public Boolean putProductWithTtl(@PathVariable String productId,
                                     @RequestParam(defaultValue = "300") long ttlSeconds,
                                     @RequestBody Map<String, Object> product) {
        return cacheDemoService.putProductWithTtl(productId, product, ttlSeconds);
    }

    @GetMapping("/ttl/{productId}")
    public JSONObject getProductFromTtlCache(@PathVariable String productId) {
        return cacheDemoService.getProductFromTtlCache(productId);
    }

    @PutMapping("/{productId}")
    public Boolean refreshProductCache(@PathVariable String productId,
                                       @RequestParam(defaultValue = "300") long ttlSeconds,
                                       @RequestBody Map<String, Object> product) {
        return cacheDemoService.refreshProductCache(productId, product, ttlSeconds);
    }

    @DeleteMapping("/{productId}")
    public Boolean removeProductCache(@PathVariable String productId) {
        return cacheDemoService.removeProductCache(productId);
    }
}
```

### 本地缓存 Map

`RLocalCachedMap` 适合读多写少、热点数据访问频繁、网络往返开销明显的场景。每个应用实例都会维护一份本地缓存；当其他实例修改同名 Map 时，可以通过同步策略让本地缓存失效或更新。官方 Javadoc 中说明，`RLocalCachedMap` 每个实例维护本地缓存，以实现快速读取，适合以读为主且不希望频繁产生网络往返的 Map。([javadoc.io](https://javadoc.io/static/org.redisson/redisson/3.34.0/org/redisson/api/RLocalCachedMap.html?utm_source=chatgpt.com))

写入本地缓存：

```bash
curl -X POST "http://localhost:8080/redisson/cache/local/P1001" \
  -H "Content-Type: application/json" \
  -d '{"name":"机械键盘","price":299,"stock":100}'
```

读取本地缓存：

```bash
curl "http://localhost:8080/redisson/cache/local/P1001"
```

常用配置说明：

| 参数                   | 说明               | 建议                           |
| ---------------------- | ------------------ | ------------------------------ |
| `cacheSize`            | 本地缓存最大条目数 | 设置上限，避免 JVM 内存不可控  |
| `evictionPolicy`       | 本地缓存淘汰策略   | 常用 `LRU`                     |
| `syncStrategy`         | 多实例同步策略     | 常用 `INVALIDATE`              |
| `reconnectionStrategy` | 断线重连后处理策略 | 常用 `CLEAR`                   |
| `storeCacheMiss`       | 是否缓存空结果     | 谨慎开启，避免缓存穿透与脏空值 |

在较新的 Redisson API 中，本地缓存配置也提供了 `org.redisson.api.options.LocalCachedMapOptions`，包含 `cacheSize`、`syncStrategy`、`evictionPolicy`、`timeToLive`、`maxIdle`、`storeMode` 等选项；旧的 `org.redisson.api.LocalCachedMapOptions` 仍可见但已标记为过时。实际项目应根据当前 Redisson 版本选择对应 API。([javadoc.io](https://javadoc.io/static/org.redisson/redisson/3.35.0/org/redisson/api/options/LocalCachedMapOptions.html?utm_source=chatgpt.com))

### 缓存过期策略

缓存过期策略用于控制数据在 Redis 中的生命周期。`RMapCache` 支持对每个 Entry 设置 TTL 和最大空闲时间；官方文档示例中，`map.put(key, value, ttl, TimeUnit.MINUTES)` 用于设置条目 TTL，也支持同时设置 `ttl` 与 `maxIdleTime`。([Redisson](https://redisson.pro/docs/data-and-services/collections/?utm_source=chatgpt.com))

写入 60 秒过期缓存：

```bash
curl -X POST "http://localhost:8080/redisson/cache/ttl/P1001?ttlSeconds=60" \
  -H "Content-Type: application/json" \
  -d '{"name":"机械键盘","price":299,"stock":100}'
```

读取过期缓存：

```bash
curl "http://localhost:8080/redisson/cache/ttl/P1001"
```

等待 60 秒后再次读取，如果返回空对象，说明缓存已过期。

常见过期策略：

| 策略         | 说明                   | 适用场景                     |
| ------------ | ---------------------- | ---------------------------- |
| 固定 TTL     | 写入时设置固定过期时间 | 商品详情、配置快照、列表缓存 |
| 随机 TTL     | 固定 TTL 加随机偏移    | 防止大量 Key 同时过期        |
| 最大空闲时间 | 长时间未访问则过期     | 会话、临时状态               |
| 主动删除     | 数据变更时删除缓存     | 更新频率不高的数据           |
| 主动刷新     | 数据变更后重新写入缓存 | 读多写少且要求低延迟读取     |

### 缓存更新策略

缓存更新策略决定数据库和缓存之间的一致性方式。常见方案有 Cache Aside、先更新数据库再删除缓存、先删除缓存再更新数据库、异步刷新缓存等。Spring Boot 业务中更常用的是 Cache Aside：读取时先查缓存，未命中再查数据库并回写缓存；更新时先更新数据库，再删除或刷新缓存。

刷新商品缓存：

```bash
curl -X PUT "http://localhost:8080/redisson/cache/P1001?ttlSeconds=300" \
  -H "Content-Type: application/json" \
  -d '{"name":"机械键盘Pro","price":399,"stock":80}'
```

删除商品缓存：

```bash
curl -X DELETE "http://localhost:8080/redisson/cache/P1001"
```

缓存更新建议：

| 场景               | 建议                                 |
| ------------------ | ------------------------------------ |
| 数据一致性要求较高 | 更新数据库后删除缓存                 |
| 读性能要求较高     | 更新数据库后刷新缓存                 |
| 热点 Key           | 可使用本地缓存 Map，但要关注一致性   |
| 写入频繁           | 不建议使用本地缓存，避免频繁失效广播 |
| 缓存穿透           | 缓存空值或使用布隆过滤器             |
| 缓存击穿           | 热点 Key 加锁回源                    |
| 缓存雪崩           | TTL 加随机偏移，避免集中失效         |

开发中需要区分 `RLocalCachedMap` 和 `RMapCache` 的职责：`RLocalCachedMap` 解决本地读取性能问题，重点在减少网络往返；`RMapCache` 解决 Redis 侧条目过期问题，重点在 TTL 和空闲过期控制。对于既要求本地缓存又要求 Redis Entry 级过期的复杂场景，需要确认当前 Redisson 版本及功能授权范围，避免误以为本地缓存 TTL 等同于 Redis 数据 TTL。



## Spring Boot 业务集成

本节用于说明 Redisson 在真实 Spring Boot 业务代码中的封装方式。前面章节已经分别演示了锁、限流、缓存、队列等能力，业务集成阶段不建议在 Controller 中直接操作 `RedissonClient`，而应通过配置属性、Service 封装、统一异常处理和统一响应结构降低业务侵入。

Redisson Spring Boot Starter 会集成 Spring Boot，并提供 `RedissonClient`、`RedissonRxClient`、`RedissonReactiveClient`、`RedisTemplate` 等 Spring Bean；Spring Boot 3.x 推荐使用 `spring.data.redis` 配置结构。([Redisson](https://redisson.pro/docs/integration-with-spring/?utm_source=chatgpt.com))

示例文件结构如下：

```text
src/main/java/io/github/atengk/redisson/common/ApiResult.java
src/main/java/io/github/atengk/redisson/exception/BizException.java
src/main/java/io/github/atengk/redisson/exception/GlobalExceptionHandler.java
src/main/java/io/github/atengk/redisson/config/RedissonBusinessProperties.java
src/main/java/io/github/atengk/redisson/service/RedissonBusinessService.java
src/main/java/io/github/atengk/redisson/controller/OrderBusinessController.java
```

### Service 层封装

Service 层封装的目标是把 Redisson 的底层 API 包装成业务语义明确的方法，例如“带锁执行订单提交”“限流访问接口”“写入业务缓存”“删除业务缓存”。这样 Controller 和业务服务不需要关心锁 Key 拼接、释放锁、限流失败、异常处理等细节。

下面的配置属性用于统一维护业务锁、限流和缓存默认参数。

文件位置：`src/main/java/io/github/atengk/redisson/config/RedissonBusinessProperties.java`

```java
package io.github.atengk.redisson.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

import java.time.Duration;

/**
 * Redisson 业务封装配置属性。
 *
 * @author Ateng
 * @since 2026-05-06
 */
@Data
@ConfigurationProperties(prefix = "app.redisson.business")
public class RedissonBusinessProperties {

    /**
     * 业务锁配置。
     */
    private Lock lock = new Lock();

    /**
     * 限流配置。
     */
    private Rate rate = new Rate();

    /**
     * 缓存配置。
     */
    private Cache cache = new Cache();

    /**
     * 业务锁配置。
     *
     * @author Ateng
     * @since 2026-05-06
     */
    @Data
    public static class Lock {
        private Duration waitTime = Duration.ofSeconds(3);
        private Duration leaseTime = Duration.ofSeconds(10);
        private String keyPrefix = "redisson:business:lock:";
    }

    /**
     * 业务限流配置。
     *
     * @author Ateng
     * @since 2026-05-06
     */
    @Data
    public static class Rate {
        private String keyPrefix = "redisson:business:rate:";
        private long permits = 10;
        private Duration interval = Duration.ofMinutes(1);
    }

    /**
     * 业务缓存配置。
     *
     * @author Ateng
     * @since 2026-05-06
     */
    @Data
    public static class Cache {
        private String keyPrefix = "redisson:business:cache:";
        private Duration ttl = Duration.ofMinutes(10);
    }
}
```

文件位置：`src/main/resources/application.yml`

```yaml
app:
  redisson:
    business:
      lock:
        # 默认等待锁时间
        wait-time: 3s
        # 默认锁租约时间
        lease-time: 10s
        # 业务锁 Key 前缀
        key-prefix: redisson:business:lock:
      rate:
        # 业务限流 Key 前缀
        key-prefix: redisson:business:rate:
        # 默认限流令牌数
        permits: 10
        # 默认限流周期
        interval: 1m
      cache:
        # 业务缓存 Key 前缀
        key-prefix: redisson:business:cache:
        # 默认缓存过期时间
        ttl: 10m
```

下面的业务封装类提供锁执行、限流判断、缓存读写和缓存删除能力。

文件位置：`src/main/java/io/github/atengk/redisson/service/RedissonBusinessService.java`

```java
package io.github.atengk.redisson.service;

import cn.hutool.core.util.StrUtil;
import cn.hutool.json.JSONUtil;
import io.github.atengk.redisson.config.RedissonBusinessProperties;
import io.github.atengk.redisson.exception.BizException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.redisson.api.RBucket;
import org.redisson.api.RLock;
import org.redisson.api.RRateLimiter;
import org.redisson.api.RateType;
import org.redisson.api.RedissonClient;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.concurrent.TimeUnit;
import java.util.function.Supplier;

/**
 * Redisson 业务能力封装服务。
 *
 * @author Ateng
 * @since 2026-05-06
 */
@Slf4j
@Service
@RequiredArgsConstructor
@EnableConfigurationProperties(RedissonBusinessProperties.class)
public class RedissonBusinessService {

    private final RedissonClient redissonClient;
    private final RedissonBusinessProperties properties;

    /**
     * 使用分布式锁执行业务逻辑。
     *
     * @param bizKey 业务 Key
     * @param supplier 业务逻辑
     * @param <T> 返回类型
     * @return 业务执行结果
     */
    public <T> T executeWithLock(String bizKey, Supplier<T> supplier) {
        if (StrUtil.isBlank(bizKey)) {
            throw new BizException("业务Key不能为空");
        }
        if (supplier == null) {
            throw new BizException("业务逻辑不能为空");
        }

        String lockKey = properties.getLock().getKeyPrefix() + bizKey;
        RLock lock = redissonClient.getLock(lockKey);
        boolean locked = false;

        try {
            locked = lock.tryLock(
                    properties.getLock().getWaitTime().toMillis(),
                    properties.getLock().getLeaseTime().toMillis(),
                    TimeUnit.MILLISECONDS
            );

            if (!locked) {
                log.info("业务锁获取失败，bizKey：{}", bizKey);
                throw new BizException("业务处理中，请勿重复操作");
            }

            log.info("业务锁获取成功，bizKey：{}", bizKey);
            return supplier.get();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            log.warn("业务锁等待被中断，bizKey：{}", bizKey, e);
            throw new BizException("业务处理被中断");
        } finally {
            unlockSafely(lock, locked, lockKey);
        }
    }

    /**
     * 判断是否通过限流。
     *
     * @param rateKey 限流 Key
     * @return 是否通过限流
     */
    public boolean tryPassRateLimit(String rateKey) {
        if (StrUtil.isBlank(rateKey)) {
            throw new BizException("限流Key不能为空");
        }

        String limiterName = properties.getRate().getKeyPrefix() + rateKey;
        RRateLimiter limiter = redissonClient.getRateLimiter(limiterName);
        Duration interval = properties.getRate().getInterval();

        limiter.trySetRate(RateType.OVERALL, properties.getRate().getPermits(), interval);
        boolean passed = limiter.tryAcquire();

        log.info("业务限流校验完成，rateKey：{}，passed：{}", rateKey, passed);
        return passed;
    }

    /**
     * 写入业务缓存。
     *
     * @param cacheKey 缓存 Key
     * @param value 缓存值
     * @return 是否写入成功
     */
    public boolean putCache(String cacheKey, Object value) {
        if (StrUtil.isBlank(cacheKey)) {
            throw new BizException("缓存Key不能为空");
        }
        if (value == null) {
            throw new BizException("缓存值不能为空");
        }

        String key = properties.getCache().getKeyPrefix() + cacheKey;
        RBucket<String> bucket = redissonClient.getBucket(key);
        bucket.set(JSONUtil.toJsonStr(value), properties.getCache().getTtl().toSeconds(), TimeUnit.SECONDS);

        log.info("业务缓存写入成功，cacheKey：{}", cacheKey);
        return true;
    }

    /**
     * 读取业务缓存。
     *
     * @param cacheKey 缓存 Key
     * @return 缓存字符串
     */
    public String getCache(String cacheKey) {
        if (StrUtil.isBlank(cacheKey)) {
            throw new BizException("缓存Key不能为空");
        }

        String key = properties.getCache().getKeyPrefix() + cacheKey;
        RBucket<String> bucket = redissonClient.getBucket(key);
        String value = bucket.get();

        log.info("业务缓存读取完成，cacheKey：{}，hit：{}", cacheKey, StrUtil.isNotBlank(value));
        return value;
    }

    /**
     * 删除业务缓存。
     *
     * @param cacheKey 缓存 Key
     * @return 是否删除成功
     */
    public boolean removeCache(String cacheKey) {
        if (StrUtil.isBlank(cacheKey)) {
            throw new BizException("缓存Key不能为空");
        }

        String key = properties.getCache().getKeyPrefix() + cacheKey;
        RBucket<String> bucket = redissonClient.getBucket(key);
        boolean deleted = bucket.delete();

        log.info("业务缓存删除完成，cacheKey：{}，deleted：{}", cacheKey, deleted);
        return deleted;
    }

    /**
     * 安全释放锁。
     *
     * @param lock 锁对象
     * @param locked 是否成功获取锁
     * @param lockKey 锁 Key
     */
    private void unlockSafely(RLock lock, boolean locked, String lockKey) {
        if (locked && lock.isHeldByCurrentThread()) {
            lock.unlock();
            log.info("业务锁释放成功，lockKey：{}", lockKey);
        }
    }
}
```

锁封装中使用 `tryLock(waitTime, leaseTime, unit)`，含义是最多等待指定时间获取锁，成功后在租约时间到期自动释放；Redisson 的 `RLock` 遵循 Java `Lock` 规范，只允许持锁线程释放锁，否则会抛出 `IllegalMonitorStateException`。([Redisson](https://redisson.pro/docs/data-and-services/locks-and-synchronizers/index.html?utm_source=chatgpt.com))

### Controller 接口示例

Controller 层只保留参数接收、基础入口定义和业务服务调用，不直接拼接 Redis Key，也不直接处理 Redisson API。下面示例演示订单提交、接口限流、订单缓存写入、订单缓存读取和缓存删除。

文件位置：`src/main/java/io/github/atengk/redisson/controller/OrderBusinessController.java`

```java
package io.github.atengk.redisson.controller;

import cn.hutool.core.map.MapUtil;
import io.github.atengk.redisson.common.ApiResult;
import io.github.atengk.redisson.exception.BizException;
import io.github.atengk.redisson.service.RedissonBusinessService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * 订单业务 Redisson 集成示例接口。
 *
 * @author Ateng
 * @since 2026-05-06
 */
@RestController
@RequiredArgsConstructor
@RequestMapping("/business/order")
public class OrderBusinessController {

    private final RedissonBusinessService redissonBusinessService;

    /**
     * 提交订单，使用分布式锁防止重复提交。
     *
     * @param orderNo 订单号
     * @return 提交结果
     */
    @PostMapping("/submit/{orderNo}")
    public ApiResult<String> submitOrder(@PathVariable String orderNo) {
        String result = redissonBusinessService.executeWithLock("order:submit:" + orderNo, () -> {
            Map<String, Object> order = MapUtil.<String, Object>builder()
                    .put("orderNo", orderNo)
                    .put("status", "SUBMITTED")
                    .put("message", "订单提交成功")
                    .build();

            redissonBusinessService.putCache("order:" + orderNo, order);
            return "订单提交成功：" + orderNo;
        });

        return ApiResult.ok(result);
    }

    /**
     * 查询订单详情，使用限流保护查询接口。
     *
     * @param orderNo 订单号
     * @param userId 用户ID
     * @return 查询结果
     */
    @GetMapping("/{orderNo}")
    public ApiResult<String> getOrder(@PathVariable String orderNo,
                                      @RequestParam(defaultValue = "anonymous") String userId) {
        boolean passed = redissonBusinessService.tryPassRateLimit("order:query:" + userId);
        if (!passed) {
            throw new BizException("查询过于频繁，请稍后重试");
        }

        String value = redissonBusinessService.getCache("order:" + orderNo);
        return ApiResult.ok(value);
    }

    /**
     * 删除订单缓存。
     *
     * @param orderNo 订单号
     * @return 删除结果
     */
    @DeleteMapping("/{orderNo}/cache")
    public ApiResult<Boolean> removeOrderCache(@PathVariable String orderNo) {
        boolean deleted = redissonBusinessService.removeCache("order:" + orderNo);
        return ApiResult.ok(deleted);
    }
}
```

接口说明：

| 接口                               | 方法     | 说明                             |
| ---------------------------------- | -------- | -------------------------------- |
| `/business/order/submit/{orderNo}` | `POST`   | 提交订单，使用分布式锁防重复提交 |
| `/business/order/{orderNo}`        | `GET`    | 查询订单，使用限流保护           |
| `/business/order/{orderNo}/cache`  | `DELETE` | 删除订单缓存                     |

### 配置属性封装

配置属性封装用于把业务参数从代码中移到 `application.yml`。实际项目中，不建议把锁等待时间、锁租约时间、限流速率、缓存 TTL 写死在业务代码中，否则环境切换、压测调参和线上应急都不方便。

配置建议如下：

| 配置项      | 建议                                           |
| ----------- | ---------------------------------------------- |
| 锁 Key 前缀 | 按业务模块统一，例如 `redisson:business:lock:` |
| 锁等待时间  | 接口类业务常用 1 到 3 秒                       |
| 锁租约时间  | 应大于正常业务耗时                             |
| 限流周期    | 根据接口特性配置，例如 1 秒、1 分钟、1 小时    |
| 限流令牌数  | 按用户、IP、租户或接口维度配置                 |
| 缓存 TTL    | 避免永久缓存，除非业务明确要求                 |
| 密码类配置  | 使用环境变量或配置中心注入                     |

如果业务锁执行时间不确定，可以使用 Redisson watchdog 自动续期机制；Redisson 默认 lock watchdog 超时时间为 30 秒，可通过 `Config.lockWatchdogTimeout` 调整。显式设置 `leaseTime` 后，锁会在租约时间到期后自动释放。([Redisson](https://redisson.pro/docs/data-and-services/locks-and-synchronizers/index.html?utm_source=chatgpt.com))

### 异常处理封装

统一异常处理用于规范接口返回结构，避免 Controller 中大量重复 `try-catch`。下面示例使用 `BizException` 表示业务异常，使用 `GlobalExceptionHandler` 统一返回 JSON 结果。

文件位置：`src/main/java/io/github/atengk/redisson/common/ApiResult.java`

```java
package io.github.atengk.redisson.common;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 接口统一响应对象。
 *
 * @author Ateng
 * @since 2026-05-06
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ApiResult<T> {

    private Integer code;
    private String message;
    private T data;

    /**
     * 返回成功结果。
     *
     * @param data 响应数据
     * @param <T> 数据类型
     * @return 成功响应
     */
    public static <T> ApiResult<T> ok(T data) {
        return new ApiResult<>(200, "操作成功", data);
    }

    /**
     * 返回失败结果。
     *
     * @param code 错误码
     * @param message 错误信息
     * @param <T> 数据类型
     * @return 失败响应
     */
    public static <T> ApiResult<T> fail(Integer code, String message) {
        return new ApiResult<>(code, message, null);
    }
}
```

文件位置：`src/main/java/io/github/atengk/redisson/exception/BizException.java`

```java
package io.github.atengk.redisson.exception;

/**
 * 业务异常。
 *
 * @author Ateng
 * @since 2026-05-06
 */
public class BizException extends RuntimeException {

    /**
     * 创建业务异常。
     *
     * @param message 异常信息
     */
    public BizException(String message) {
        super(message);
    }

    /**
     * 创建业务异常。
     *
     * @param message 异常信息
     * @param cause 原始异常
     */
    public BizException(String message, Throwable cause) {
        super(message, cause);
    }
}
```

文件位置：`src/main/java/io/github/atengk/redisson/exception/GlobalExceptionHandler.java`

```java
package io.github.atengk.redisson.exception;

import io.github.atengk.redisson.common.ApiResult;
import lombok.extern.slf4j.Slf4j;
import org.redisson.client.RedisConnectionException;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

/**
 * 全局异常处理器。
 *
 * @author Ateng
 * @since 2026-05-06
 */
@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    /**
     * 处理业务异常。
     *
     * @param e 业务异常
     * @return 统一响应
     */
    @ExceptionHandler(BizException.class)
    public ApiResult<Void> handleBizException(BizException e) {
        log.info("业务异常：{}", e.getMessage());
        return ApiResult.fail(400, e.getMessage());
    }

    /**
     * 处理 Redis 连接异常。
     *
     * @param e Redis 连接异常
     * @return 统一响应
     */
    @ExceptionHandler(RedisConnectionException.class)
    public ApiResult<Void> handleRedisConnectionException(RedisConnectionException e) {
        log.error("Redis连接异常：{}", e.getMessage(), e);
        return ApiResult.fail(HttpStatus.SERVICE_UNAVAILABLE.value(), "Redis服务不可用，请稍后重试");
    }

    /**
     * 处理参数异常。
     *
     * @param e 参数异常
     * @return 统一响应
     */
    @ExceptionHandler(IllegalArgumentException.class)
    public ApiResult<Void> handleIllegalArgumentException(IllegalArgumentException e) {
        log.info("参数异常：{}", e.getMessage());
        return ApiResult.fail(400, e.getMessage());
    }

    /**
     * 处理系统异常。
     *
     * @param e 系统异常
     * @return 统一响应
     */
    @ExceptionHandler(Exception.class)
    public ApiResult<Void> handleException(Exception e) {
        log.error("系统异常：{}", e.getMessage(), e);
        return ApiResult.fail(500, "系统繁忙，请稍后重试");
    }
}
```

## 测试与验证

本节用于说明 Redisson 功能集成后的验证方式，包括单元测试、接口测试、并发测试和锁释放验证。Redisson 相关代码需要重点验证 Redis 连通性、锁释放、限流效果、缓存过期、并发互斥和异常兜底。

Spring Boot 3.3 系列要求至少 Java 17，Maven 需要 3.6.3 或更高版本；因此测试环境也应保持同样的 JDK 和构建工具版本，避免本地可运行而 CI/CD 失败。([docs.enterprise.spring.io](https://docs.enterprise.spring.io/spring-boot/system-requirements.html?utm_source=chatgpt.com))

### 单元测试

单元测试用于验证 Redisson 封装类是否能正常获取锁、写入缓存、读取缓存和删除缓存。以下示例使用 `@SpringBootTest` 启动 Spring 容器，依赖本地 Redis 或 Docker Redis。

文件位置：`src/test/java/io/github/atengk/redisson/RedissonBusinessServiceTest.java`

```java
package io.github.atengk.redisson;

import io.github.atengk.redisson.service.RedissonBusinessService;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

/**
 * Redisson 业务封装单元测试。
 *
 * @author Ateng
 * @since 2026-05-06
 */
@SpringBootTest
class RedissonBusinessServiceTest {

    @Autowired
    private RedissonBusinessService redissonBusinessService;

    /**
     * 验证分布式锁业务执行。
     */
    @Test
    void testExecuteWithLock() {
        String result = redissonBusinessService.executeWithLock("test:lock:1001", () -> "success");
        Assertions.assertEquals("success", result);
    }

    /**
     * 验证业务缓存写入和读取。
     */
    @Test
    void testCachePutAndGet() {
        boolean saved = redissonBusinessService.putCache("test:cache:1001", "hello-redisson");
        Assertions.assertTrue(saved);

        String value = redissonBusinessService.getCache("test:cache:1001");
        Assertions.assertNotNull(value);
        Assertions.assertTrue(value.contains("hello-redisson"));
    }

    /**
     * 验证业务缓存删除。
     */
    @Test
    void testCacheRemove() {
        redissonBusinessService.putCache("test:cache:remove:1001", "remove-value");
        boolean deleted = redissonBusinessService.removeCache("test:cache:remove:1001");

        Assertions.assertTrue(deleted);
    }

    /**
     * 验证限流器可正常获取令牌。
     */
    @Test
    void testRateLimit() {
        boolean passed = redissonBusinessService.tryPassRateLimit("test:rate:1001");
        Assertions.assertTrue(passed);
    }
}
```

测试执行命令：

```bash
# 执行全部测试
mvn test

# 只执行 Redisson 业务封装测试
mvn -Dtest=RedissonBusinessServiceTest test
```

命令说明：

| 命令                                 | 说明                 |
| ------------------------------------ | -------------------- |
| `mvn test`                           | 执行项目中全部测试   |
| `-Dtest=RedissonBusinessServiceTest` | 只执行指定测试类     |
| 测试前提                             | Redis 服务必须可连接 |

### 接口测试

接口测试用于验证 Controller、Service、Redisson 和 Redis 的完整链路。测试前需要启动 Redis 服务和 Spring Boot 应用。

启动应用：

```bash
# 使用默认配置启动项目
mvn spring-boot:run
```

提交订单：

```bash
curl -X POST "http://localhost:8080/business/order/submit/ORDER202605060001"
```

查询订单：

```bash
curl "http://localhost:8080/business/order/ORDER202605060001?userId=1001"
```

删除订单缓存：

```bash
curl -X DELETE "http://localhost:8080/business/order/ORDER202605060001/cache"
```

预期响应结构：

```json
{
  "code": 200,
  "message": "操作成功",
  "data": "订单提交成功：ORDER202605060001"
}
```

如果 Redis 未启动，接口应返回类似以下结构：

```json
{
  "code": 503,
  "message": "Redis服务不可用，请稍后重试",
  "data": null
}
```

### 并发测试

并发测试用于验证分布式锁是否能正确保护临界区。下面示例通过多个线程同时执行同一个业务 Key，统计实际进入临界区的次数。由于锁内业务执行较短，多个线程可能按顺序获取锁；如果要验证“只允许一个请求成功，其他请求快速失败”，需要把锁等待时间设置得更短，并让临界区停留足够长时间。

文件位置：`src/test/java/io/github/atengk/redisson/RedissonLockConcurrencyTest.java`

```java
package io.github.atengk.redisson;

import io.github.atengk.redisson.exception.BizException;
import io.github.atengk.redisson.service.RedissonBusinessService;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Redisson 分布式锁并发测试。
 *
 * @author Ateng
 * @since 2026-05-06
 */
@Slf4j
@SpringBootTest
class RedissonLockConcurrencyTest {

    @Autowired
    private RedissonBusinessService redissonBusinessService;

    /**
     * 验证多个线程并发获取同一把锁时，临界区可以被互斥保护。
     *
     * @throws InterruptedException 线程等待异常
     */
    @Test
    void testConcurrentLock() throws InterruptedException {
        int threadCount = 20;
        CountDownLatch startLatch = new CountDownLatch(1);
        CountDownLatch finishLatch = new CountDownLatch(threadCount);
        AtomicInteger successCount = new AtomicInteger();
        AtomicInteger failCount = new AtomicInteger();

        for (int i = 0; i < threadCount; i++) {
            Thread.startVirtualThread(() -> {
                try {
                    startLatch.await();
                    redissonBusinessService.executeWithLock("concurrent:order:1001", () -> {
                        successCount.incrementAndGet();
                        sleep(300);
                        return true;
                    });
                } catch (BizException e) {
                    failCount.incrementAndGet();
                    log.info("并发获取锁失败：{}", e.getMessage());
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    failCount.incrementAndGet();
                } finally {
                    finishLatch.countDown();
                }
            });
        }

        startLatch.countDown();
        finishLatch.await();

        log.info("并发测试完成，successCount：{}，failCount：{}", successCount.get(), failCount.get());

        Assertions.assertTrue(successCount.get() > 0);
        Assertions.assertEquals(threadCount, successCount.get() + failCount.get());
    }

    /**
     * 线程休眠。
     *
     * @param millis 毫秒数
     */
    private void sleep(long millis) {
        try {
            Thread.sleep(millis);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new BizException("线程休眠被中断", e);
        }
    }
}
```

执行并发测试：

```bash
mvn -Dtest=RedissonLockConcurrencyTest test
```

如果使用 JDK 17，而不是支持虚拟线程的更高版本，需要将 `Thread.startVirtualThread` 改为普通线程池，例如 `Executors.newFixedThreadPool(threadCount)`。

### 锁释放验证

锁释放验证用于确认业务正常结束、业务异常结束、线程中断、锁租约到期等场景下锁不会长期残留。Redisson 的 watchdog 会在未显式设置 `leaseTime` 时自动续期；如果设置了 `leaseTime`，到期后锁会自动释放。([Redisson](https://redisson.pro/docs/data-and-services/locks-and-synchronizers/index.html?utm_source=chatgpt.com))

可以通过 Redis 命令查看锁 Key 是否存在：

```bash
# 查看业务锁 Key
redis-cli --scan --pattern "redisson:business:lock:*"

# 查看指定锁 Key 的剩余过期时间，返回 -2 表示 Key 不存在
redis-cli ttl "redisson:business:lock:order:submit:ORDER202605060001"
```

验证步骤：

| 步骤 | 操作                 | 预期                |
| ---- | -------------------- | ------------------- |
| 1    | 调用提交订单接口     | 接口正常返回        |
| 2    | 立即扫描锁 Key       | 可能短暂存在        |
| 3    | 等待租约时间结束     | 锁 Key 消失         |
| 4    | 业务异常后再次扫描   | 锁 Key 不应长期存在 |
| 5    | 重复调用同一订单提交 | 不应出现死锁        |

如果发现锁 Key 长时间不释放，需要重点检查以下问题：

| 问题                            | 检查点                                |
| ------------------------------- | ------------------------------------- |
| 未在 `finally` 释放锁           | 所有加锁逻辑必须有 `finally`          |
| 释放了非当前线程锁              | 释放前检查 `isHeldByCurrentThread()`  |
| 锁租约时间过长                  | 调整 `leaseTime`                      |
| 业务线程阻塞                    | 检查外部调用、数据库慢 SQL、死循环    |
| 使用 `lock.lock()` 后进程仍存活 | watchdog 会持续续期，需要业务主动释放 |

## 部署与运行

本节用于说明本地运行、Docker Redis 运行和生产环境配置建议。部署时需要重点关注 Redis 地址、密码、连接池大小、应用实例数量、锁租约时间、限流规则和缓存过期策略。

### 本地运行

本地运行适合开发和调试。建议使用单机 Redis，配置简单，便于验证 Redisson 客户端初始化和业务功能。

本地配置示例：

文件位置：`src/main/resources/application-local.yml`

```yaml
spring:
  application:
    name: springboot3-redisson-demo

  data:
    redis:
      # 本地 Redis 地址
      host: 127.0.0.1
      # 本地 Redis 端口
      port: 6379
      # 本地开发默认使用 0 号库
      database: 0
      # 本地无密码时留空
      password:
      # 命令超时时间
      timeout: 3000ms
      # 连接超时时间
      connect-timeout: 3000ms
      # 客户端名称
      client-name: ${spring.application.name}

app:
  redisson:
    business:
      lock:
        wait-time: 3s
        lease-time: 10s
      rate:
        permits: 10
        interval: 1m
      cache:
        ttl: 10m
```

启动命令：

```bash
# 使用 local 配置启动
mvn spring-boot:run -Dspring-boot.run.profiles=local
```

本地运行检查：

```bash
# 检查 Redis 是否响应
redis-cli ping

# 检查应用接口
curl -X POST "http://localhost:8080/business/order/submit/ORDER202605060001"

# 检查 Redis 中是否存在业务缓存
redis-cli --scan --pattern "redisson:business:cache:*"
```

### Docker Redis 运行

Docker Redis 适合本地开发、集成测试和临时演示环境。以下方式启动一个 Redis 7 容器并映射到本机 `6379` 端口。

```bash
# 拉取 Redis 镜像
docker pull redis:7

# 启动 Redis 容器
docker run -d \
  --name redis-redisson \
  -p 6379:6379 \
  redis:7 \
  redis-server --appendonly yes

# 检查容器状态
docker ps | grep redis-redisson

# 验证 Redis 可用性
docker exec -it redis-redisson redis-cli ping
```

也可以使用 Docker Compose 管理 Redis。

文件位置：`docker-compose.yml`

```yaml
services:
  redis:
    # Redis 7 官方镜像
    image: redis:7
    container_name: redis-redisson
    restart: unless-stopped
    ports:
      # 本机 6379 映射到容器 6379
      - "6379:6379"
    command:
      # 开启 AOF 持久化
      - redis-server
      - --appendonly
      - "yes"
    volumes:
      # Redis 数据持久化目录
      - ./data/redis:/data
```

启动和停止命令：

```bash
# 启动 Redis
docker compose up -d

# 查看 Redis 日志
docker compose logs -f redis

# 验证 Redis
docker exec -it redis-redisson redis-cli ping

# 停止 Redis
docker compose down
```

命令说明：

| 命令                           | 说明                              |
| ------------------------------ | --------------------------------- |
| `docker compose up -d`         | 后台启动 Redis                    |
| `docker compose logs -f redis` | 查看 Redis 日志                   |
| `redis-cli ping`               | 验证 Redis 是否正常响应           |
| `docker compose down`          | 停止并移除 Compose 创建的容器网络 |

### 生产环境配置建议

生产环境配置需要以稳定性、隔离性和可观测性为核心。Redisson 不应只按本地开发配置直接上线，需要根据 Redis 部署方式、服务实例数、业务并发量和故障恢复策略调整参数。

生产建议如下：

| 方向       | 建议                                               |
| ---------- | -------------------------------------------------- |
| Redis 部署 | 使用哨兵或集群，不建议核心业务只依赖单机 Redis     |
| Redis 密码 | 必须启用密码或 ACL                                 |
| 网络访问   | Redis 不应直接暴露公网                             |
| 配置管理   | 密码、地址、连接池参数通过配置中心或环境变量管理   |
| Key 命名   | 使用统一前缀，例如 `业务:模块:类型:标识`           |
| 连接池     | 按应用实例数和 Redis 最大连接数统一评估            |
| 锁租约     | 设置为大于正常业务耗时，并保留一定冗余             |
| 限流规则   | 区分全局、用户、IP、租户等维度                     |
| 缓存 TTL   | 避免大量 Key 同时过期，可增加随机偏移              |
| 监控       | 监控 Redis 内存、连接数、慢查询、QPS、阻塞命令     |
| 日志       | 记录锁失败、限流拒绝、Redis 异常、队列消费失败     |
| 降级       | Redis 不可用时，核心接口应有明确失败策略或降级策略 |

生产配置示例：

文件位置：`src/main/resources/application-prod.yml`

```yaml
spring:
  application:
    name: springboot3-redisson-demo

  data:
    redis:
      # 生产 Redis 地址，建议由环境变量或配置中心注入
      host: ${REDIS_HOST}
      port: ${REDIS_PORT:6379}
      database: ${REDIS_DATABASE:0}
      password: ${REDIS_PASSWORD}
      timeout: 5000ms
      connect-timeout: 5000ms
      client-name: ${spring.application.name}-${HOSTNAME:default}

app:
  redisson:
    business:
      lock:
        # 生产环境根据接口耗时调整
        wait-time: 3s
        lease-time: 30s
        key-prefix: redisson:prod:business:lock:
      rate:
        # 示例：单个限流主体每分钟 60 次
        permits: 60
        interval: 1m
        key-prefix: redisson:prod:business:rate:
      cache:
        # 示例：业务缓存默认 10 分钟
        ttl: 10m
        key-prefix: redisson:prod:business:cache:
```

如果生产环境使用 Redisson 原生 YAML 配置，可以通过 `spring.redis.redisson.file` 指定配置文件；Redisson Spring Boot Starter 文档支持使用通用 Spring Boot Redis 配置，也支持指定 Redisson YAML 配置文件。([Redisson](https://redisson.pro/docs/integration-with-spring/?utm_source=chatgpt.com))

生产上线前检查清单：

| 检查项       | 要求                                     |
| ------------ | ---------------------------------------- |
| Redis 连通性 | 应用容器能访问 Redis                     |
| Redis 密码   | 密码不写死在代码仓库                     |
| 超时配置     | `timeout`、`connect-timeout` 合理        |
| 锁 Key       | 包含明确业务维度                         |
| 锁释放       | 所有锁都有 `finally` 释放                |
| 限流策略     | 限流 Key 不误伤其他用户                  |
| 缓存策略     | 有 TTL，有删除或刷新机制                 |
| 异常处理     | Redis 异常有统一响应                     |
| 日志         | 关键路径有业务日志                       |
| 监控         | Redis 和应用都接入监控                   |
| 压测         | 验证连接池、锁等待、限流效果和缓存命中率 |

生产环境中，Redisson 能解决分布式协调问题，但不能替代数据库约束、业务状态机和幂等设计。订单提交、库存扣减、支付回调等核心链路仍应保留数据库唯一索引、状态校验、幂等表或消息补偿机制。