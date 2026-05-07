# Spring Boot 集成 XXL-JOB 开发

本文档用于说明在 Spring Boot 3 项目中接入 XXL-JOB 的基础开发流程，重点覆盖调度中心准备、执行器服务准备、任务开发前置条件和本地验证方式。XXL-JOB 官方定位为分布式任务调度平台，核心目标是开发迅速、学习简单、轻量级、易扩展，并支持调度中心 HA、执行器 HA、任务注册、路由策略、阻塞策略、失败重试和 Rolling 日志等能力。([GitHub](https://github.com/xuxueli/xxl-job/blob/master/doc/XXL-JOB官方文档.md))

## 模块概述

本模块用于将业务系统中的定时任务、异步补偿任务、批处理任务统一接入 XXL-JOB 调度平台。接入后，任务的触发时间、执行状态、执行日志、失败重试和分片执行等能力由调度中心统一管理，业务系统只需要作为执行器提供具体任务逻辑。

### XXL-JOB 简介

XXL-JOB 是一个中心化调度、分布式执行的任务调度平台。整体架构主要由两部分组成：调度中心和执行器。调度中心负责任务配置、任务触发、执行日志查看、失败告警、调度报表等管理能力；执行器部署在业务服务中，负责接收调度请求并执行具体任务逻辑。

在 Spring Boot 3 项目中，业务服务通常通过引入 `xxl-job-core` 依赖成为一个 XXL-JOB 执行器。执行器启动后会按照配置向调度中心注册，调度中心根据执行器的 `appname`、注册地址、路由策略和任务配置，向对应的执行器节点发起任务调度请求。

XXL-JOB 的常见核心能力包括：

| 能力         | 说明                                                         |
| ------------ | ------------------------------------------------------------ |
| 任务统一管理 | 通过调度中心 Web 页面创建、修改、启动、停止任务              |
| 动态调度     | 支持 Cron、固定间隔、固定延时、手动触发、API 触发等方式      |
| 执行器注册   | 执行器周期性向调度中心注册，调度中心自动发现可用节点         |
| 集群执行     | 多个执行器实例可组成执行器集群，支持轮询、随机、一致性 Hash、故障转移等路由策略 |
| 分片广播     | 多实例同时执行同一个任务，可用于大批量数据分片处理           |
| 失败重试     | 任务失败后可按配置自动重试                                   |
| 执行日志     | 支持在线查看任务执行日志，便于排查生产问题                   |
| 阻塞处理     | 同一任务执行过慢时，可配置单机串行、丢弃后续调度、覆盖之前调度等策略 |

### 接入目标

本模块的接入目标是将 Spring Boot 3 服务改造为 XXL-JOB 执行器，使业务系统具备可配置、可观察、可重试的分布式任务执行能力。

接入完成后，应达到以下效果：

| 目标             | 说明                                                         |
| ---------------- | ------------------------------------------------------------ |
| 执行器自动注册   | Spring Boot 服务启动后，自动向 XXL-JOB 调度中心注册执行器地址 |
| 任务统一触发     | 定时任务不再依赖本地 `@Scheduled` 固定配置，而是由调度中心统一触发 |
| 任务参数动态调整 | 任务参数可在调度中心配置，无需重新发布业务服务               |
| 任务日志可追踪   | 每次任务执行都能在调度中心查看执行结果和关键日志             |
| 支持多实例部署   | 执行器服务可横向扩容，调度中心按路由策略选择节点执行         |
| 支持异常重试     | 对失败任务配置重试次数，降低短暂异常导致的任务失败风险       |
| 支持分片任务     | 对大数据量任务按执行器实例进行分片，提高处理效率             |

接入过程中需要重点保证以下配置一致：

| 配置项                     | 要求                                               |
| -------------------------- | -------------------------------------------------- |
| `xxl.job.admin.addresses`  | 必须指向可访问的调度中心地址                       |
| `xxl.job.executor.appname` | 必须与调度中心“执行器管理”中的 AppName 保持一致    |
| `xxl.job.accessToken`      | 如果调度中心配置了 Token，执行器必须配置相同 Token |
| `xxl.job.executor.ip`      | 多网卡、容器、Kubernetes 环境建议显式配置          |
| `xxl.job.executor.port`    | 同一台机器部署多个执行器实例时不能冲突             |
| `xxl.job.executor.logpath` | 需要保证目录存在且应用进程具备写入权限             |

### 使用场景

XXL-JOB 适合用于需要统一调度、统一监控、统一日志和分布式执行的任务场景。对于简单单体项目，本地 `@Scheduled` 可以满足基础定时需求；但当任务需要动态配置、失败重试、分布式执行、执行日志查看或手动补偿时，更适合接入 XXL-JOB。

常见使用场景如下：

| 场景           | 示例                                                   |
| -------------- | ------------------------------------------------------ |
| 数据同步任务   | 定时同步第三方系统订单、用户、库存、账单数据           |
| 状态补偿任务   | 扫描长时间未完成的订单、支付、退款、审批记录并进行补偿 |
| 报表统计任务   | 每日、每小时生成业务统计数据、运营报表、财务汇总数据   |
| 消息补偿任务   | 扫描发送失败的短信、邮件、站内信、MQ 消息并重试        |
| 文件处理任务   | 定时处理上传文件、生成导出文件、清理临时文件           |
| 缓存刷新任务   | 定时刷新热点缓存、字典缓存、配置缓存                   |
| 数据清理任务   | 定时清理过期日志、临时表、历史数据、无效附件           |
| 分片批处理任务 | 多实例并行处理大批量用户、订单、账单或日志数据         |
| 运维巡检任务   | 定时检查接口可用性、磁盘空间、业务指标异常             |

不建议使用 XXL-JOB 处理极高频、强实时、毫秒级触发的任务。此类场景更适合使用消息队列、流式计算、事件驱动机制或服务内部异步线程池。

## 环境准备

本节用于说明接入 XXL-JOB 前需要准备的基础环境。Spring Boot 3 项目通常要求 JDK 17 或更高版本；XXL-JOB 3.x 官方也要求 JDK 17，且 v3.0.0 Release Notes 明确说明调度中心升级至 Spring Boot 3 + JDK 17。([GitHub](https://github.com/xuxueli/xxl-job/blob/master/doc/XXL-JOB官方文档.md))

### 版本说明

Spring Boot 3 与 XXL-JOB 的版本选择需要同时考虑 JDK、调度中心版本、执行器依赖版本和部署环境。建议调度中心和执行器客户端使用同一大版本，避免协议、认证或日志接口差异导致兼容性问题。

推荐版本基线如下：

| 组件           | 推荐版本 | 说明                                                        |
| -------------- | -------- | ----------------------------------------------------------- |
| JDK            | 17+      | Spring Boot 3 和 XXL-JOB 3.x 均建议使用 JDK 17 作为基础版本 |
| Spring Boot    | 3.x      | 本文档以 Spring Boot 3 项目作为执行器服务                   |
| XXL-JOB        | 3.x      | Spring Boot 3 项目建议优先选择 XXL-JOB 3.x 版本             |
| Maven          | 3.8+     | 用于构建 Spring Boot 项目                                   |
| MySQL          | 8.x      | 调度中心默认需要数据库保存任务、执行器、日志等数据          |
| Docker         | 20.x+    | 本地或测试环境可使用 Docker 启动调度中心和 MySQL            |
| Docker Compose | 2.x      | 可选，用于编排调度中心、数据库等组件                        |

版本选择建议：

1. 新项目优先使用 `JDK 17 + Spring Boot 3.x + XXL-JOB 3.x`。
2. 如果企业内部已有 XXL-JOB 调度中心，应先确认调度中心版本，再选择执行器依赖版本。
3. 如果调度中心仍为 XXL-JOB 2.x，Spring Boot 3 执行器接入前需要重点验证客户端兼容性。
4. 如果使用较新的 XXL-JOB 3.x 版本，需要关注 Release Notes 中的数据库升级脚本、认证配置、OpenAPI 返回结构和日志字段变化。
5. 官方文档说明 XXL-JOB 3.x 开始要求 JDK 17，2.x 及以下支持 JDK 1.8，因此混用版本时要重点检查 JDK 与依赖兼容性。([GitHub](https://github.com/xuxueli/xxl-job/blob/master/doc/XXL-JOB官方文档.md))

Spring Boot 执行器服务的 Maven 依赖版本示例：

```xml
<!-- XXL-JOB 执行器核心依赖，用于将 Spring Boot 服务注册为 XXL-JOB 执行器 -->
<dependency>
    <groupId>com.xuxueli</groupId>
    <artifactId>xxl-job-core</artifactId>
    <version>3.0.0</version>
</dependency>
```

如果项目使用公司统一依赖管理，建议将版本放到父工程或 `dependencyManagement` 中统一维护：

```xml
<properties>
    <!-- Spring Boot 3 与 XXL-JOB 3.x 建议使用 JDK 17 -->
    <java.version>17</java.version>
    <xxl-job.version>3.0.0</xxl-job.version>
</properties>

<dependencyManagement>
    <dependencies>
        <!-- 统一 XXL-JOB 版本，避免多个业务模块依赖版本不一致 -->
        <dependency>
            <groupId>com.xuxueli</groupId>
            <artifactId>xxl-job-core</artifactId>
            <version>${xxl-job.version}</version>
        </dependency>
    </dependencies>
</dependencyManagement>
```

### 调度中心准备

调度中心是 XXL-JOB 的管理端，负责任务配置、任务触发、执行器管理、执行日志查看和调度报表展示。执行器服务接入前，必须先准备可访问的调度中心地址。

调度中心准备流程如下：

1. 准备 MySQL 数据库。
2. 初始化 XXL-JOB 调度中心数据库脚本。
3. 启动 `xxl-job-admin` 服务。
4. 登录调度中心 Web 页面。
5. 在“执行器管理”中创建执行器。
6. 确认调度中心网络地址可被执行器服务访问。

调度中心数据库建议单独创建，例如：

```sql
-- 创建 XXL-JOB 调度中心数据库
CREATE DATABASE IF NOT EXISTS xxl_job
    DEFAULT CHARACTER SET utf8mb4
    DEFAULT COLLATE utf8mb4_unicode_ci;
```

本地开发环境可以使用 Docker 启动 MySQL：

```bash
docker run -d \
  --name xxl-job-mysql \
  -p 3306:3306 \
  -e MYSQL_ROOT_PASSWORD=123456 \
  -e MYSQL_DATABASE=xxl_job \
  -v /data/docker/xxl-job/mysql:/var/lib/mysql \
  mysql:8.0
```

以上命令会启动一个 MySQL 8 容器，并创建 `xxl_job` 数据库。`/data/docker/xxl-job/mysql` 是宿主机持久化目录，生产环境需要替换为规范的数据盘路径，并设置更强的数据库密码。

调度中心启动后，通常通过如下地址访问：

```text
http://localhost:8080/xxl-job-admin
```

默认登录账号需要以实际部署版本为准。首次部署后，应立即修改默认管理员密码，并限制调度中心后台访问来源。

执行器管理需要提前创建一条执行器配置：

| 字段     | 示例值              | 说明                                        |
| -------- | ------------------- | ------------------------------------------- |
| AppName  | `demo-job-executor` | 执行器唯一标识，需要与 Spring Boot 配置一致 |
| 名称     | `示例任务执行器`    | 调度中心页面展示名称                        |
| 注册方式 | 自动注册            | 推荐使用，执行器启动后自动上报地址          |
| 机器地址 | 空                  | 自动注册模式下无需手动维护地址              |

调度中心准备完成后，需要验证以下内容：

| 检查项       | 验证方式                                  |
| ------------ | ----------------------------------------- |
| 页面可访问   | 浏览器访问调度中心地址                    |
| 数据库正常   | 调度中心启动日志无数据库连接异常          |
| 执行器已创建 | “执行器管理”中存在对应 AppName            |
| 网络可达     | 执行器服务所在机器可以访问调度中心地址    |
| Token 一致   | 调度中心和执行器的 `accessToken` 配置一致 |

### 执行器服务准备

执行器服务是具体执行业务任务的 Spring Boot 3 应用。接入 XXL-JOB 前，需要先确认服务具备基础运行环境、配置文件、日志目录和网络端口。

执行器服务准备内容如下：

| 准备项       | 说明                                                         |
| ------------ | ------------------------------------------------------------ |
| JDK          | 使用 JDK 17 或更高版本启动应用                               |
| 依赖         | 引入 `xxl-job-core`                                          |
| 配置文件     | 在 `application.yml` 中配置调度中心地址、执行器 AppName、端口、日志目录等 |
| 执行器配置类 | 创建 `XxlJobSpringExecutor` Bean                             |
| 任务 Handler | 使用 `@XxlJob` 标注具体任务方法                              |
| 日志目录     | 确保执行器日志目录存在且可写                                 |
| 网络端口     | 确保执行器端口未被占用，并且调度中心可访问                   |

执行器基础配置示例：

```yaml
server:
  port: 18080

xxl:
  job:
    # 调度中心地址，多个地址使用英文逗号分隔
    admin:
      addresses: http://localhost:8080/xxl-job-admin

    # 调度中心与执行器通讯 Token；为空表示不启用，生产环境建议配置
    accessToken: default_token

    executor:
      # 执行器 AppName，必须与调度中心“执行器管理”中的 AppName 一致
      appname: demo-job-executor

      # 执行器注册地址 IP；为空时自动获取，容器或多网卡环境建议显式指定
      ip:

      # 执行器通讯端口，不能与 server.port 冲突
      port: 9999

      # 执行器日志目录，应用进程必须具备写入权限
      logpath: /data/logs/xxl-job/jobhandler

      # 执行器日志保留天数，设置为 7 表示保留 7 天
      logretentiondays: 7
```

执行器服务启动前，需要确认日志目录存在：

```bash
mkdir -p /data/logs/xxl-job/jobhandler
chmod 755 /data/logs/xxl-job/jobhandler
```

`mkdir -p` 用于递归创建日志目录，`chmod 755` 用于保证应用进程具备基础访问权限。生产环境如果使用独立运行用户，需要将目录属主调整为应用运行用户，例如 `chown -R app:app /data/logs/xxl-job`。

本地启动执行器服务后，需要观察控制台日志或应用日志中是否存在执行器注册成功、调度中心连接成功等信息。随后进入调度中心的“执行器管理”页面，查看对应 AppName 下是否出现在线机器地址。

执行器准备完成后，建议做一次最小化验证：

| 验证项     | 预期结果                                           |
| ---------- | -------------------------------------------------- |
| 应用启动   | Spring Boot 服务正常启动，无 Bean 初始化异常       |
| 执行器端口 | `9999` 端口正常监听                                |
| 自动注册   | 调度中心能看到执行器在线地址                       |
| 手动触发   | 调度中心手动触发任务后，执行器能收到请求           |
| 日志查看   | 调度中心能查看本次任务执行日志                     |
| 异常处理   | 任务抛出异常时，调度中心显示失败状态并记录错误日志 |



## 项目依赖配置

本节用于说明 Spring Boot 3 执行器服务需要引入的 Maven 依赖、配置文件结构和多环境配置方式。XXL-JOB 执行器并不是普通的 Spring Boot Starter 自动装配模型，业务服务通常需要显式引入 `xxl-job-core`，并自行创建 `XxlJobSpringExecutor` Bean 完成接入。官方文档中执行器示例也采用引入 `xxl-job-core` 并配置执行器组件的方式。([GitHub](https://github.com/xuxueli/xxl-job/blob/master/doc/XXL-JOB官方文档.md?utm_source=chatgpt.com))

### Maven 依赖

Spring Boot 项目作为 XXL-JOB 执行器时，只需要引入 `xxl-job-core`。如果项目中需要使用配置绑定、参数校验、日志注解和常用工具类，可以同时引入 `spring-boot-starter-validation`、`lombok` 和 `hutool-all`。

以下配置放在业务服务的 `pom.xml` 中。

```xml
<properties>
    <!-- Spring Boot 3 推荐使用 JDK 17 或更高版本 -->
    <java.version>17</java.version>

    <!-- XXL-JOB 执行器版本，建议与调度中心保持同一大版本 -->
    <xxl-job.version>3.4.0</xxl-job.version>

    <!-- Hutool 工具类版本，可按公司 BOM 统一管理 -->
    <hutool.version>5.8.39</hutool.version>
</properties>

<dependencies>
    <!-- Web 服务基础依赖；如果当前服务已存在，可不重复添加 -->
    <dependency>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-starter-web</artifactId>
    </dependency>

    <!-- 参数校验依赖，用于配置属性校验和业务参数校验 -->
    <dependency>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-starter-validation</artifactId>
    </dependency>

    <!-- XXL-JOB 执行器核心依赖，用于注册执行器、接收调度请求、执行任务 Handler -->
    <dependency>
        <groupId>com.xuxueli</groupId>
        <artifactId>xxl-job-core</artifactId>
        <version>${xxl-job.version}</version>
    </dependency>

    <!-- Hutool 工具类，示例中用于字符串、集合、日期等常用处理 -->
    <dependency>
        <groupId>cn.hutool</groupId>
        <artifactId>hutool-all</artifactId>
        <version>${hutool.version}</version>
    </dependency>

    <!-- Lombok，用于减少 Getter、Setter、构造器和日志样板代码 -->
    <dependency>
        <groupId>org.projectlombok</groupId>
        <artifactId>lombok</artifactId>
        <optional>true</optional>
    </dependency>

    <!-- 配置元数据生成器，用于 IDE 提示 application.yml 中的自定义配置项 -->
    <dependency>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-configuration-processor</artifactId>
        <optional>true</optional>
    </dependency>
</dependencies>
```

如果项目中使用父工程统一管理版本，可以将 XXL-JOB 版本放到父工程的 `dependencyManagement` 中，子模块只保留依赖声明：

```xml
<dependencyManagement>
    <dependencies>
        <!-- 统一 XXL-JOB 执行器版本，避免多模块版本不一致 -->
        <dependency>
            <groupId>com.xuxueli</groupId>
            <artifactId>xxl-job-core</artifactId>
            <version>${xxl-job.version}</version>
        </dependency>
    </dependencies>
</dependencyManagement>
```

### XXL-JOB 配置项

XXL-JOB 执行器配置主要分为调度中心配置和执行器配置。调度中心配置用于告诉执行器注册到哪里、通讯 Token 是什么、请求超时时间是多少；执行器配置用于声明当前服务的 AppName、注册地址、通讯端口和日志目录。

官方执行器配置项包括 `xxl.job.admin.addresses`、`xxl.job.admin.accessToken`、`xxl.job.admin.timeout`、`xxl.job.executor.enabled`、`xxl.job.executor.appname`、`xxl.job.executor.address`、`xxl.job.executor.ip`、`xxl.job.executor.port`、`xxl.job.executor.logpath`、`xxl.job.executor.logretentiondays` 等。([GitHub](https://github.com/xuxueli/xxl-job/blob/master/xxl-job-executor-samples/xxl-job-executor-sample-frameless/src/main/resources/xxl-job-executor.properties?utm_source=chatgpt.com))

基础配置放在 `src/main/resources/application.yml` 中：

```yaml
server:
  # 业务服务端口，与 XXL-JOB 执行器通讯端口不是同一个端口
  port: 18080

spring:
  application:
    # 应用名称，可作为执行器 appname 的组成部分
    name: order-service

xxl:
  job:
    admin:
      # 调度中心地址，多个地址使用英文逗号分隔
      addresses: http://127.0.0.1:8080/xxl-job-admin

      # 调度中心与执行器通讯 Token，生产环境必须修改默认值
      accessToken: ${XXL_JOB_ACCESS_TOKEN:default_token}

      # 调度中心通讯超时时间，单位秒
      timeout: 3

    executor:
      # 是否启用 XXL-JOB 执行器，默认开启
      enabled: true

      # 执行器 AppName，必须与调度中心“执行器管理”中的 AppName 一致
      appname: ${spring.application.name}-executor

      # 执行器注册地址；为空时使用 ip + port 自动生成注册地址
      address:

      # 执行器 IP；为空时自动获取，多网卡、容器、Kubernetes 环境建议显式配置
      ip:

      # 执行器通讯端口，调度中心会通过该端口触发任务
      port: 9999

      # 执行器任务日志目录，应用进程需要具备读写权限
      logpath: /data/applogs/xxl-job/jobhandler

      # 日志保留天数；大于等于 3 时自动清理，-1 表示关闭自动清理
      logretentiondays: 30

      # 任务扫描排除包路径，多个包使用英文逗号分隔
      excludedpackage: org.springframework,spring
```

配置项说明如下：

| 配置项                              | 是否必填 | 示例值                                | 说明                                                 |
| ----------------------------------- | -------- | ------------------------------------- | ---------------------------------------------------- |
| `xxl.job.admin.addresses`           | 是       | `http://127.0.0.1:8080/xxl-job-admin` | 调度中心地址，多个地址用英文逗号分隔                 |
| `xxl.job.admin.accessToken`         | 否       | `default_token`                       | 执行器与调度中心通讯 Token，生产环境必须使用非默认值 |
| `xxl.job.admin.timeout`             | 否       | `3`                                   | 调度中心通讯超时时间，单位秒                         |
| `xxl.job.executor.enabled`          | 否       | `true`                                | 是否启用执行器，适合在本地或特殊环境关闭任务执行     |
| `xxl.job.executor.appname`          | 是       | `order-service-executor`              | 执行器 AppName，需要与调度中心配置一致               |
| `xxl.job.executor.address`          | 否       | `http://10.0.0.10:9999`               | 显式注册地址，容器端口映射场景常用                   |
| `xxl.job.executor.ip`               | 否       | `10.0.0.10`                           | 执行器通讯 IP，多网卡时建议指定                      |
| `xxl.job.executor.port`             | 是       | `9999`                                | 执行器内嵌通讯端口，不能与同机其他执行器冲突         |
| `xxl.job.executor.logpath`          | 否       | `/data/applogs/xxl-job/jobhandler`    | 任务执行日志目录                                     |
| `xxl.job.executor.logretentiondays` | 否       | `30`                                  | 日志保留天数                                         |
| `xxl.job.executor.excludedpackage`  | 否       | `org.springframework,spring`          | 排除不需要扫描任务 Handler 的包路径                  |

需要注意的是，部分旧版本文档或历史项目可能使用 `xxl.job.accessToken`，而较新的执行器示例使用 `xxl.job.admin.accessToken`。新项目建议统一使用 `xxl.job.admin.accessToken`，旧项目迁移时需要检查配置类读取的是哪个键，并保证调度中心与执行器 Token 保持一致。

### 多环境配置

多环境配置用于区分本地开发、测试环境和生产环境的调度中心地址、执行器端口、日志目录和 Token。建议公共配置放在 `application.yml`，环境差异放在 `application-dev.yml`、`application-test.yml`、`application-prod.yml` 中。

公共配置保留默认结构：

```yaml
spring:
  profiles:
    # 默认使用 dev 环境，生产部署时通过启动参数覆盖
    active: dev

xxl:
  job:
    admin:
      timeout: 3
      accessToken: ${XXL_JOB_ACCESS_TOKEN:}
    executor:
      enabled: true
      appname: ${spring.application.name}-executor
      port: 9999
      logretentiondays: 30
      excludedpackage: org.springframework,spring
```

本地开发环境通常连接本机调度中心，并使用相对日志目录，避免污染系统目录：

```yaml
# 文件位置：src/main/resources/application-dev.yml

xxl:
  job:
    admin:
      # 本地调度中心地址
      addresses: http://127.0.0.1:8080/xxl-job-admin

      # 本地开发可使用弱 Token，但需要与调度中心保持一致
      accessToken: default_token

    executor:
      # 本地可使用固定端口，便于调试
      port: 9999

      # 本地日志目录
      logpath: ./logs/xxl-job/jobhandler

      # 本地日志保留时间较短
      logretentiondays: 7
```

测试环境通常连接测试调度中心，并显式配置执行器 IP 或地址：

```yaml
# 文件位置：src/main/resources/application-test.yml

xxl:
  job:
    admin:
      # 测试环境调度中心地址
      addresses: http://xxl-job-admin-test:8080/xxl-job-admin

      # 从环境变量读取 Token，避免提交到代码仓库
      accessToken: ${XXL_JOB_ACCESS_TOKEN}

    executor:
      # 测试环境执行器名称
      appname: order-service-test-executor

      # 测试环境执行器通讯端口
      port: 9999

      # 测试环境日志目录
      logpath: /data/logs/order-service/xxl-job/jobhandler

      # 容器环境如果自动识别 IP 不准确，可通过环境变量指定
      ip: ${POD_IP:}
```

生产环境建议连接高可用调度中心，并通过环境变量注入敏感配置：

```yaml
# 文件位置：src/main/resources/application-prod.yml

xxl:
  job:
    admin:
      # 生产调度中心集群地址，多个地址使用英文逗号分隔
      addresses: http://xxl-job-admin-01:8080/xxl-job-admin,http://xxl-job-admin-02:8080/xxl-job-admin

      # 生产 Token 必须通过环境变量、配置中心或密钥系统注入
      accessToken: ${XXL_JOB_ACCESS_TOKEN}

      # 生产环境可适当提高超时时间
      timeout: 5

    executor:
      # 生产执行器名称，必须提前在调度中心创建
      appname: order-service-prod-executor

      # 生产执行器端口
      port: 9999

      # 生产日志目录
      logpath: /data/logs/order-service/xxl-job/jobhandler

      # 生产日志保留时间
      logretentiondays: 30

      # Kubernetes 或容器端口映射场景可显式指定注册地址
      address: ${XXL_JOB_EXECUTOR_ADDRESS:}
```

生产启动时建议通过命令行参数或环境变量指定 Profile：

```bash
java -jar order-service.jar \
  --spring.profiles.active=prod \
  --XXL_JOB_ACCESS_TOKEN='替换为生产调度中心Token'
```

`--spring.profiles.active=prod` 用于启用生产配置，`XXL_JOB_ACCESS_TOKEN` 用于注入通讯 Token。生产环境不建议将 Token 明文写入 Git 仓库。

## 执行器接入

本节用于说明如何在 Spring Boot 3 项目中创建 XXL-JOB 执行器配置类。接入后，服务启动时会初始化 `XxlJobSpringExecutor`，扫描 Spring 容器中的 `@XxlJob` 任务方法，并向调度中心注册当前执行器实例。

### 执行器配置类

建议使用 `@ConfigurationProperties` 管理 XXL-JOB 配置，避免在配置类中散落大量 `@Value`。本示例包含两个关键类：`XxlJobProperties` 负责绑定配置文件，`XxlJobConfig` 负责创建 `XxlJobSpringExecutor` Bean。

文件结构如下：

```text
src/main/java/io/github/atengk/config/
├── XxlJobProperties.java
└── XxlJobConfig.java
```

下面代码用于绑定 `application.yml` 中的 `xxl.job` 配置。

文件位置：`src/main/java/io/github/atengk/config/XxlJobProperties.java`

```java
package io.github.atengk.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

/**
 * XXL-JOB 配置属性
 *
 * @author Ateng
 * @since 2026-05-07
 */
@Data
@Validated
@ConfigurationProperties(prefix = "xxl.job")
public class XxlJobProperties {

    /**
     * 调度中心配置
     */
    private Admin admin = new Admin();

    /**
     * 执行器配置
     */
    private Executor executor = new Executor();

    /**
     * 调度中心配置
     *
     * @author Ateng
     * @since 2026-05-07
     */
    @Data
    public static class Admin {

        /**
         * 调度中心地址，多个地址使用英文逗号分隔
         */
        private String addresses;

        /**
         * 调度中心通讯 Token
         */
        private String accessToken;

        /**
         * 通讯超时时间，单位秒
         */
        private int timeout = 3;
    }

    /**
     * 执行器配置
     *
     * @author Ateng
     * @since 2026-05-07
     */
    @Data
    public static class Executor {

        /**
         * 是否启用执行器
         */
        private boolean enabled = true;

        /**
         * 执行器 AppName
         */
        private String appname;

        /**
         * 执行器注册地址
         */
        private String address;

        /**
         * 执行器 IP
         */
        private String ip;

        /**
         * 执行器端口
         */
        private int port = 9999;

        /**
         * 执行器日志目录
         */
        private String logpath = "/data/applogs/xxl-job/jobhandler";

        /**
         * 执行器日志保留天数
         */
        private int logretentiondays = 30;

        /**
         * 任务扫描排除包路径
         */
        private String excludedpackage;
    }
}
```

下面代码用于创建 XXL-JOB 执行器 Bean，并在初始化前检查核心配置。

文件位置：`src/main/java/io/github/atengk/config/XxlJobConfig.java`

```java
package io.github.atengk.config;

import cn.hutool.core.util.StrUtil;
import com.xxl.job.core.executor.impl.XxlJobSpringExecutor;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * XXL-JOB 执行器配置
 *
 * @author Ateng
 * @since 2026-05-07
 */
@Slf4j
@Configuration
@RequiredArgsConstructor
@EnableConfigurationProperties(XxlJobProperties.class)
@ConditionalOnProperty(prefix = "xxl.job.executor", name = "enabled", havingValue = "true", matchIfMissing = true)
public class XxlJobConfig {

    private final XxlJobProperties xxlJobProperties;

    /**
     * 创建 XXL-JOB Spring 执行器
     *
     * @return XXL-JOB Spring 执行器
     */
    @Bean
    public XxlJobSpringExecutor xxlJobExecutor() {
        XxlJobProperties.Admin admin = xxlJobProperties.getAdmin();
        XxlJobProperties.Executor executor = xxlJobProperties.getExecutor();

        checkConfig(admin, executor);

        XxlJobSpringExecutor xxlJobSpringExecutor = new XxlJobSpringExecutor();
        xxlJobSpringExecutor.setAdminAddresses(admin.getAddresses());
        xxlJobSpringExecutor.setAppname(executor.getAppname());
        xxlJobSpringExecutor.setAddress(executor.getAddress());
        xxlJobSpringExecutor.setIp(executor.getIp());
        xxlJobSpringExecutor.setPort(executor.getPort());
        xxlJobSpringExecutor.setAccessToken(admin.getAccessToken());
        xxlJobSpringExecutor.setTimeout(admin.getTimeout());
        xxlJobSpringExecutor.setLogPath(StrUtil.blankToDefault(
                executor.getLogpath(),
                "/data/applogs/xxl-job/jobhandler"
        ));
        xxlJobSpringExecutor.setLogRetentionDays(executor.getLogretentiondays());

        log.info("初始化 XXL-JOB 执行器完成，appname={}，admin={}，port={}",
                executor.getAppname(), admin.getAddresses(), executor.getPort());
        return xxlJobSpringExecutor;
    }

    /**
     * 检查核心配置
     *
     * @param admin 调度中心配置
     * @param executor 执行器配置
     */
    private void checkConfig(XxlJobProperties.Admin admin, XxlJobProperties.Executor executor) {
        if (StrUtil.isBlank(admin.getAddresses())) {
            throw new IllegalArgumentException("XXL-JOB 调度中心地址不能为空：xxl.job.admin.addresses");
        }
        if (StrUtil.isBlank(executor.getAppname())) {
            throw new IllegalArgumentException("XXL-JOB 执行器 AppName 不能为空：xxl.job.executor.appname");
        }
        if (executor.getPort() < 0) {
            throw new IllegalArgumentException("XXL-JOB 执行器端口不能小于 0：xxl.job.executor.port");
        }
        if (executor.getLogretentiondays() != -1 && executor.getLogretentiondays() < 3) {
            log.warn("XXL-JOB 日志保留天数小于 3，自动清理可能不会生效，当前配置={}", executor.getLogretentiondays());
        }
    }
}
```

如果某些环境不希望启动执行器，例如本地只启动接口服务、不接收调度任务，可以设置：

```yaml
xxl:
  job:
    executor:
      # 关闭执行器初始化，不注册到调度中心
      enabled: false
```

### 执行器注册机制

执行器注册是调度中心发现业务服务节点的过程。执行器服务启动后，`XxlJobSpringExecutor` 会根据配置的 `admin.addresses`、`appname`、`address`、`ip`、`port` 等信息初始化执行器，并向调度中心注册。调度中心根据 AppName 将多个执行器实例归为同一个执行器集群，任务触发时再按照路由策略选择具体节点。官方文档说明，`AppName` 是执行器集群的唯一标识，执行器会根据 AppName 自动、周期性注册。([GitHub](https://github.com/xuxueli/xxl-job/blob/master/doc/XXL-JOB-English-Documentation.md?utm_source=chatgpt.com))

自动注册模式是推荐方式，调度中心“执行器管理”中配置如下：

| 字段     | 推荐值                        | 说明                                   |
| -------- | ----------------------------- | -------------------------------------- |
| AppName  | `order-service-prod-executor` | 必须与 `xxl.job.executor.appname` 一致 |
| 名称     | `订单服务执行器`              | 页面展示名称                           |
| 注册方式 | 自动注册                      | 由执行器启动后主动注册                 |
| 机器地址 | 留空                          | 自动注册模式无需手动填写               |
| 排序     | `1`                           | 多执行器展示顺序                       |

自动注册时，注册地址生成逻辑通常遵循以下原则：

| 配置情况                                | 注册地址来源                   | 适用场景                             |
| --------------------------------------- | ------------------------------ | ------------------------------------ |
| 配置了 `xxl.job.executor.address`       | 直接使用 `address`             | 容器端口映射、网关转发、固定公网地址 |
| 未配置 `address`，配置了 `ip` 和 `port` | 使用 `ip:port`                 | 多网卡服务器、Kubernetes Pod IP      |
| 未配置 `address` 和 `ip`                | 自动获取本机 IP，再拼接 `port` | 本地开发、单网卡服务器               |
| `port` 小于等于 0                       | 自动选择端口                   | 本地临时调试，不建议生产使用         |

容器和 Kubernetes 环境中，如果调度中心无法访问执行器自动识别出来的 IP，应显式配置 `address` 或 `ip`：

```yaml
xxl:
  job:
    executor:
      # Kubernetes 场景可注入 Pod IP
      ip: ${POD_IP}

      # 如果通过 Service 或固定域名暴露执行器端口，也可以直接配置完整注册地址
      address: ${XXL_JOB_EXECUTOR_ADDRESS:}
```

启动后可以通过以下方式验证注册是否成功：

```bash
# 查看执行器端口是否监听
ss -lntp | grep 9999

# 查看应用日志中 XXL-JOB 初始化信息
grep -i "XXL-JOB" /data/logs/order-service/application.log
```

`ss -lntp` 用于确认执行器通讯端口是否已经监听，`grep` 用于从应用日志中检索执行器初始化、注册或通讯异常信息。如果执行器端口未监听，需要检查 `xxl.job.executor.enabled`、端口冲突和配置类是否被 Spring 扫描。

### 日志目录配置

XXL-JOB 执行器日志目录用于保存任务运行日志。调度中心查看任务日志时，会通过执行器读取该目录下的日志文件，因此该路径必须存在，并且应用运行用户需要具备读写权限。官方配置说明中也明确指出，执行器运行日志文件存储路径需要具备读写权限，日志保留天数大于等于 3 时自动清理，`-1` 表示关闭自动清理。([GitHub](https://github.com/xuxueli/xxl-job/blob/master/doc/XXL-JOB官方文档.md?utm_source=chatgpt.com))

推荐生产目录如下：

```yaml
xxl:
  job:
    executor:
      # 推荐为每个服务单独分配 XXL-JOB 日志目录
      logpath: /data/logs/order-service/xxl-job/jobhandler

      # 生产环境根据磁盘容量设置保留天数
      logretentiondays: 30
```

Linux 环境需要提前创建目录并授权：

```bash
mkdir -p /data/logs/order-service/xxl-job/jobhandler
chown -R app:app /data/logs/order-service/xxl-job
chmod 755 /data/logs/order-service/xxl-job/jobhandler
```

`mkdir -p` 用于递归创建日志目录，`chown -R app:app` 用于将目录归属到应用运行用户，`chmod 755` 用于保证应用进程可以访问该目录。实际用户和用户组需要替换为生产环境中的应用运行账号。

如果使用 Docker 部署执行器，建议将日志目录挂载到宿主机，避免容器重建后任务日志丢失：

```bash
docker run -d \
  --name order-service \
  -p 18080:18080 \
  -p 9999:9999 \
  -e SPRING_PROFILES_ACTIVE=prod \
  -e XXL_JOB_ACCESS_TOKEN='替换为生产调度中心Token' \
  -v /data/logs/order-service:/data/logs/order-service \
  order-service:latest
```

其中 `-p 9999:9999` 用于暴露 XXL-JOB 执行器通讯端口，`-v /data/logs/order-service:/data/logs/order-service` 用于持久化任务执行日志。生产环境如果使用 Kubernetes，需要将日志目录挂载到持久卷或由日志采集系统统一采集。

日志配置完成后，可以通过以下步骤验证：

| 验证项   | 操作                                                 | 预期结果               |
| -------- | ---------------------------------------------------- | ---------------------- |
| 目录权限 | `ls -ld /data/logs/order-service/xxl-job/jobhandler` | 应用用户具备写入权限   |
| 任务执行 | 在调度中心手动触发任务                               | 任务可以正常执行       |
| 日志生成 | 查看 `logpath` 目录                                  | 生成任务日志文件       |
| 页面查看 | 调度中心点击“执行日志”                               | 可以查看执行器输出日志 |
| 自动清理 | 等待超过保留天数                                     | 过期日志被自动清理     |

生产环境需要重点避免以下问题：

1. 不要将多个业务系统共用同一个 `logpath`，否则日志排查困难。
2. 不要将日志目录放在临时目录，例如 `/tmp`，否则系统清理后日志可能丢失。
3. 不要在容器内只写本地文件系统而不挂载卷，否则容器重建后日志会丢失。
4. 不要使用默认 Token 或空 Token 暴露在不可信网络中。
5. 不要让执行器端口只监听在容器内部却未对调度中心可达。



## 任务开发

本节用于说明 XXL-JOB Bean 模式任务的开发方式。Spring Boot 执行器接入完成后，业务任务通常通过 `@XxlJob` 注解声明为 JobHandler，调度中心创建任务时通过 JobHandler 名称定位并触发对应方法。XXL-JOB 支持自定义任务参数、任务日志、失败重试、Rolling 实时日志和在线查看调度结果等能力。([GitHub](https://github.com/xuxueli/xxl-job?utm_source=chatgpt.com))

### Bean 模式任务

Bean 模式是 Spring Boot 项目中最常用的任务开发方式。任务方法定义在 Spring 容器 Bean 中，并使用 `@XxlJob("jobHandlerName")` 标注。调度中心创建任务时，“运行模式”选择 `BEAN`，“JobHandler” 填写注解中的名称。

建议任务类统一放在 `job` 包下，避免和 Controller、Service 等业务入口混在一起。

文件结构如下：

```text
src/main/java/io/github/atengk/job/
├── OrderJobHandler.java
└── dto/
    └── OrderTimeoutJobParam.java
```

下面代码提供两个常用 Bean 模式任务：一个简单任务，一个订单超时关闭任务。任务方法使用 `void` 返回值，执行结果通过默认成功、抛出异常、`XxlJobHelper.handleSuccess` 或 `XxlJobHelper.handleFail` 控制。

文件位置：`src/main/java/io/github/atengk/job/OrderJobHandler.java`

```java
package io.github.atengk.job;

import cn.hutool.core.date.DateUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.json.JSONUtil;
import com.xxl.job.core.context.XxlJobHelper;
import com.xxl.job.core.handler.annotation.XxlJob;
import io.github.atengk.job.dto.OrderTimeoutJobParam;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.concurrent.TimeUnit;

/**
 * 订单任务处理器
 *
 * @author Ateng
 * @since 2026-05-07
 */
@Slf4j
@Component
public class OrderJobHandler {

    /**
     * 简单任务示例
     */
    @XxlJob("orderSimpleJobHandler")
    public void orderSimpleJobHandler() throws Exception {
        XxlJobHelper.log("订单简单任务开始执行，当前时间：{}", DateUtil.now());
        log.info("订单简单任务开始执行");

        for (int i = 1; i <= 3; i++) {
            XxlJobHelper.log("订单简单任务执行进度：第 {} 次处理", i);
            TimeUnit.SECONDS.sleep(1);
        }

        XxlJobHelper.log("订单简单任务执行完成");
        log.info("订单简单任务执行完成");
    }

    /**
     * 订单超时关闭任务
     */
    @XxlJob("orderTimeoutCloseJobHandler")
    public void orderTimeoutCloseJobHandler() {
        String jobParam = XxlJobHelper.getJobParam();
        XxlJobHelper.log("订单超时关闭任务开始执行，任务参数：{}", jobParam);
        log.info("订单超时关闭任务开始执行");

        OrderTimeoutJobParam param = parseOrderTimeoutParam(jobParam);
        if (param.getTimeoutMinutes() <= 0) {
            XxlJobHelper.handleFail("订单超时时间必须大于 0 分钟");
            return;
        }

        try {
            // 示例：这里应替换为真实业务 Service 调用
            int affectedRows = closeTimeoutOrders(param);

            XxlJobHelper.log("订单超时关闭任务执行完成，超时时间={}分钟，批次大小={}，处理数量={}",
                    param.getTimeoutMinutes(), param.getBatchSize(), affectedRows);
            log.info("订单超时关闭任务执行完成，处理数量={}", affectedRows);

            XxlJobHelper.handleSuccess(StrUtil.format("订单超时关闭成功，处理数量：{}", affectedRows));
        } catch (Exception e) {
            XxlJobHelper.log(e);
            log.error("订单超时关闭任务执行失败", e);
            XxlJobHelper.handleFail("订单超时关闭任务执行失败：" + e.getMessage());
        }
    }

    /**
     * 解析订单超时任务参数
     *
     * @param jobParam 调度中心任务参数
     * @return 订单超时任务参数
     */
    private OrderTimeoutJobParam parseOrderTimeoutParam(String jobParam) {
        if (StrUtil.isBlank(jobParam)) {
            OrderTimeoutJobParam defaultParam = new OrderTimeoutJobParam();
            defaultParam.setTimeoutMinutes(30);
            defaultParam.setBatchSize(500);
            return defaultParam;
        }

        if (!JSONUtil.isTypeJSON(jobParam)) {
            throw new IllegalArgumentException("任务参数必须是 JSON 格式");
        }

        return JSONUtil.toBean(jobParam, OrderTimeoutJobParam.class);
    }

    /**
     * 关闭超时订单
     *
     * @param param 订单超时任务参数
     * @return 处理数量
     */
    private int closeTimeoutOrders(OrderTimeoutJobParam param) {
        XxlJobHelper.log("开始扫描超时订单，timeoutMinutes={}，batchSize={}",
                param.getTimeoutMinutes(), param.getBatchSize());

        // 示例代码：实际项目中应调用 OrderService 或 Mapper 执行业务逻辑
        return Math.min(param.getBatchSize(), 128);
    }
}
```

下面 DTO 用于接收调度中心传入的 JSON 参数。

文件位置：`src/main/java/io/github/atengk/job/dto/OrderTimeoutJobParam.java`

```java
package io.github.atengk.job.dto;

import lombok.Data;

/**
 * 订单超时任务参数
 *
 * @author Ateng
 * @since 2026-05-07
 */
@Data
public class OrderTimeoutJobParam {

    /**
     * 超时时间，单位分钟
     */
    private int timeoutMinutes = 30;

    /**
     * 单批处理数量
     */
    private int batchSize = 500;
}
```

调度中心任务配置示例：

| 配置项       | 示例值                                  | 说明                                   |
| ------------ | --------------------------------------- | -------------------------------------- |
| 执行器       | `order-service-prod-executor`           | 选择已注册的订单服务执行器             |
| 运行模式     | `BEAN`                                  | Spring Bean 任务模式                   |
| JobHandler   | `orderTimeoutCloseJobHandler`           | 对应 `@XxlJob` 注解值                  |
| 任务参数     | `{"timeoutMinutes":30,"batchSize":500}` | 通过 `XxlJobHelper.getJobParam()` 获取 |
| Cron         | `0 */5 * * * ?`                         | 每 5 分钟执行一次                      |
| 失败重试次数 | `2`                                     | 任务失败后最多重试 2 次                |
| 负责人       | `order-team`                            | 便于失败告警和责任归属                 |

### 参数接收方式

XXL-JOB 任务参数由调度中心配置，并在任务运行时传递给执行器。Bean 模式任务中可以通过 `XxlJobHelper.getJobParam()` 获取任务参数，参数本质是字符串，因此可以传普通文本、数字、JSON 或逗号分隔值。官方特性中也明确支持自定义任务参数在线配置并即时生效。([GitHub](https://github.com/xuxueli/xxl-job?utm_source=chatgpt.com))

常见参数格式如下：

| 参数格式 | 示例                                    | 适用场景                 |
| -------- | --------------------------------------- | ------------------------ |
| 空参数   | 空                                      | 简单巡检、固定逻辑任务   |
| 普通文本 | `2026-05-07`                            | 指定日期、状态、业务类型 |
| 数字     | `500`                                   | 指定批次大小、重试阈值   |
| JSON     | `{"timeoutMinutes":30,"batchSize":500}` | 多字段任务参数           |
| 逗号分隔 | `WAIT_PAY,WAIT_CONFIRM`                 | 简单列表参数             |

如果任务参数较多，建议统一使用 JSON。JSON 参数便于扩展字段，也便于后续在调度中心页面维护。

下面示例展示三种常用参数接收方式。

文件位置：`src/main/java/io/github/atengk/job/ParamJobHandler.java`

```java
package io.github.atengk.job;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.json.JSONUtil;
import com.xxl.job.core.context.XxlJobHelper;
import com.xxl.job.core.handler.annotation.XxlJob;
import io.github.atengk.job.dto.OrderTimeoutJobParam;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * 任务参数处理器
 *
 * @author Ateng
 * @since 2026-05-07
 */
@Slf4j
@Component
public class ParamJobHandler {

    /**
     * 文本参数任务
     */
    @XxlJob("textParamJobHandler")
    public void textParamJobHandler() {
        String jobParam = XxlJobHelper.getJobParam();
        String bizDate = StrUtil.blankToDefault(jobParam, "CURRENT_DATE");

        XxlJobHelper.log("文本参数任务开始执行，bizDate={}", bizDate);
        log.info("文本参数任务开始执行，bizDate={}", bizDate);

        // 示例：实际项目中可按 bizDate 统计日报、同步数据或执行补偿
        XxlJobHelper.handleSuccess("文本参数任务执行成功");
    }

    /**
     * 列表参数任务
     */
    @XxlJob("listParamJobHandler")
    public void listParamJobHandler() {
        String jobParam = XxlJobHelper.getJobParam();
        if (StrUtil.isBlank(jobParam)) {
            XxlJobHelper.handleFail("列表参数不能为空，例如：WAIT_PAY,WAIT_CONFIRM");
            return;
        }

        List<String> statusList = StrUtil.splitTrim(jobParam, StrUtil.COMMA);
        if (CollUtil.isEmpty(statusList)) {
            XxlJobHelper.handleFail("列表参数解析结果为空");
            return;
        }

        XxlJobHelper.log("列表参数任务开始执行，statusList={}", statusList);
        log.info("列表参数任务开始执行，statusList={}", statusList);

        // 示例：实际项目中可按状态列表批量扫描业务数据
        XxlJobHelper.handleSuccess("列表参数任务执行成功");
    }

    /**
     * JSON 参数任务
     */
    @XxlJob("jsonParamJobHandler")
    public void jsonParamJobHandler() {
        String jobParam = XxlJobHelper.getJobParam();
        if (StrUtil.isBlank(jobParam) || !JSONUtil.isTypeJSON(jobParam)) {
            XxlJobHelper.handleFail("JSON 参数格式错误，示例：{\"timeoutMinutes\":30,\"batchSize\":500}");
            return;
        }

        OrderTimeoutJobParam param = JSONUtil.toBean(jobParam, OrderTimeoutJobParam.class);
        XxlJobHelper.log("JSON 参数任务开始执行，timeoutMinutes={}，batchSize={}",
                param.getTimeoutMinutes(), param.getBatchSize());
        log.info("JSON 参数任务开始执行，timeoutMinutes={}，batchSize={}",
                param.getTimeoutMinutes(), param.getBatchSize());

        // 示例：实际项目中可根据参数控制扫描范围和批量大小
        XxlJobHelper.handleSuccess("JSON 参数任务执行成功");
    }
}
```

参数设计建议如下：

| 建议                | 说明                                                 |
| ------------------- | ---------------------------------------------------- |
| 参数较多时使用 JSON | 避免逗号分隔参数顺序混乱                             |
| 任务参数设置默认值  | 避免调度中心漏填参数导致任务不可用                   |
| 对参数做格式校验    | 参数非法时尽早失败，并输出明确原因                   |
| 不传敏感信息        | 密码、Token、密钥应放在配置中心或环境变量            |
| 控制批次大小        | 批处理任务必须支持 `batchSize`，避免单次处理过多数据 |
| 保持参数向后兼容    | 新增字段时设置默认值，不破坏历史任务配置             |

### 任务日志输出

XXL-JOB 的任务日志需要使用 `XxlJobHelper.log` 输出，这类日志会写入执行器任务日志目录，并可以在调度中心“调度日志”中查看。普通业务日志仍然使用 `log.info`、`log.warn`、`log.error` 输出到应用日志。两者定位不同：`XxlJobHelper.log` 面向调度中心排查任务执行过程，应用日志面向服务整体运行排查。XXL-JOB 官方特性中也包含 Rolling 实时日志和在线查看执行器完整日志能力。([GitHub](https://github.com/xuxueli/xxl-job?utm_source=chatgpt.com))

推荐日志输出方式如下：

```java
XxlJobHelper.log("任务开始执行，param={}", jobParam);
log.info("任务开始执行，param={}", jobParam);

try {
    // 执行业务逻辑
    XxlJobHelper.log("任务处理完成，处理数量={}", affectedRows);
    log.info("任务处理完成，处理数量={}", affectedRows);
} catch (Exception e) {
    XxlJobHelper.log(e);
    log.error("任务执行异常", e);
    XxlJobHelper.handleFail("任务执行异常：" + e.getMessage());
}
```

任务日志建议包含以下内容：

| 日志内容 | 说明                                                  |
| -------- | ----------------------------------------------------- |
| 任务开始 | 输出任务名称、参数、开始时间                          |
| 参数解析 | 输出解析后的关键参数，不输出敏感信息                  |
| 数据范围 | 输出本次处理的数据范围、时间范围、状态范围            |
| 批次进度 | 大任务按批次输出进度，例如当前页、批次大小、累计数量  |
| 分支决策 | 输出跳过、终止、降级、重试等关键判断                  |
| 任务结果 | 输出成功数量、失败数量、耗时、返回信息                |
| 异常堆栈 | 使用 `XxlJobHelper.log(e)` 保留调度中心可见的异常堆栈 |

较长任务建议按批次输出进度，避免只看到开始和结束：

```java
for (int pageNo = 1; pageNo <= totalPage; pageNo++) {
    XxlJobHelper.log("开始处理第 {} 页数据，pageSize={}", pageNo, pageSize);

    // 示例：这里替换为真实分页处理逻辑
    int currentCount = pageSize;

    XxlJobHelper.log("第 {} 页数据处理完成，当前处理数量={}", pageNo, currentCount);
}
```

日志输出注意事项：

1. 不要在高频循环中逐条输出日志，避免任务日志过大。
2. 不要输出身份证、手机号、Token、密码等敏感信息。
3. 异常场景同时输出 `XxlJobHelper.log(e)` 和 `log.error`，便于调度中心和应用日志双向排查。
4. 大批量任务建议每处理一个批次输出一次日志，不建议每条数据输出一次日志。
5. 任务结束时输出汇总结果，包括成功数、失败数、跳过数和耗时。

### 任务执行结果返回

XXL-JOB Bean 模式任务默认正常执行完成即视为成功。如果业务逻辑需要显式返回失败或成功信息，可以使用 `XxlJobHelper.handleSuccess` 和 `XxlJobHelper.handleFail` 设置任务结果。任务抛出未捕获异常时，调度中心会将任务标记为失败。

常见执行结果处理方式如下：

| 处理方式                                 | 调度结果 | 适用场景                               |
| ---------------------------------------- | -------- | -------------------------------------- |
| 方法正常结束                             | 成功     | 简单任务，无需额外返回信息             |
| `XxlJobHelper.handleSuccess("成功信息")` | 成功     | 希望调度日志显示业务结果               |
| `XxlJobHelper.handleFail("失败原因")`    | 失败     | 业务校验失败、依赖不可用、处理结果异常 |
| 抛出异常                                 | 失败     | 非预期异常，需要失败重试或告警         |
| 捕获异常但不处理                         | 可能成功 | 不推荐，容易掩盖真实失败               |

推荐写法是：可预期的业务失败使用 `handleFail`，不可预期的系统异常记录日志后使用 `handleFail` 或直接抛出异常。

```java
try {
    int affectedRows = 128;
    if (affectedRows <= 0) {
        XxlJobHelper.handleFail("未处理到任何数据，请检查任务参数或数据状态");
        return;
    }

    XxlJobHelper.handleSuccess("任务执行成功，处理数量：" + affectedRows);
} catch (Exception e) {
    XxlJobHelper.log(e);
    log.error("任务执行失败", e);
    XxlJobHelper.handleFail("任务执行失败：" + e.getMessage());
}
```

对于需要失败重试的任务，应保证任务逻辑具备幂等性。失败重试可能导致同一批数据被再次处理，如果任务没有幂等控制，可能出现重复关闭订单、重复发消息、重复生成账单等问题。XXL-JOB 支持配置任务失败重试次数，失败后会按照预设次数主动重试。([GitHub](https://github.com/xuxueli/xxl-job?utm_source=chatgpt.com))

幂等处理建议如下：

| 场景         | 建议                                               |
| ------------ | -------------------------------------------------- |
| 修改订单状态 | 使用状态条件更新，例如只更新 `WAIT_PAY` 状态的数据 |
| 发送消息     | 记录消息发送流水，按业务唯一键防重复               |
| 生成账单     | 使用账单唯一索引，例如 `biz_date + merchant_id`    |
| 清理数据     | 使用明确时间范围和状态条件                         |
| 第三方调用   | 传递业务幂等号，并记录请求结果                     |

## 调度中心配置

本节用于说明任务开发完成后，如何在 XXL-JOB 调度中心完成执行器管理、任务管理、路由策略和阻塞处理策略配置。调度中心配置是否正确，直接决定任务能否被调度、调度到哪个执行器实例，以及任务堆积时如何处理。XXL-JOB 支持执行器自动注册、丰富路由策略、阻塞处理策略、失败重试、任务参数和调度日志等能力。([GitHub](https://github.com/xuxueli/xxl-job?utm_source=chatgpt.com))

### 执行器管理

执行器管理用于维护业务服务对应的执行器集群。一个 Spring Boot 服务通常对应一个执行器 AppName；同一个服务部署多个实例时，这些实例使用相同 AppName 自动注册到同一个执行器集群下。

在调度中心进入“执行器管理”，新增执行器配置：

| 字段     | 示例值                        | 说明                                                   |
| -------- | ----------------------------- | ------------------------------------------------------ |
| AppName  | `order-service-prod-executor` | 执行器唯一标识，必须与 `xxl.job.executor.appname` 一致 |
| 名称     | `订单服务生产执行器`          | 页面展示名称                                           |
| 排序     | `1`                           | 页面排序                                               |
| 注册方式 | 自动注册                      | 推荐方式，执行器启动后自动上报地址                     |
| 机器地址 | 空                            | 自动注册模式下不需要手动维护                           |

执行器自动注册依赖以下条件：

| 条件             | 检查方式                           |
| ---------------- | ---------------------------------- |
| 执行器服务已启动 | 查看 Spring Boot 启动日志          |
| 执行器端口已监听 | `ss -lntp                          |
| 调度中心地址正确 | 检查 `xxl.job.admin.addresses`     |
| Token 一致       | 检查调度中心和执行器 `accessToken` |
| AppName 一致     | 检查调度中心 AppName 和配置文件    |
| 网络互通         | 调度中心能访问执行器 `ip:port`     |

如果执行器列表没有在线地址，优先排查以下配置：

```yaml
xxl:
  job:
    admin:
      # 调度中心地址必须从执行器服务所在网络可访问
      addresses: http://xxl-job-admin:8080/xxl-job-admin

      # Token 必须与调度中心配置一致
      accessToken: ${XXL_JOB_ACCESS_TOKEN}

    executor:
      # 必须与调度中心执行器 AppName 完全一致
      appname: order-service-prod-executor

      # 多网卡、容器、Kubernetes 环境建议显式指定
      ip: ${POD_IP:}

      # 调度中心需要能访问该端口
      port: 9999
```

### 任务管理

任务管理用于创建具体调度任务。一个执行器可以配置多个任务，每个任务通过 JobHandler 绑定到执行器服务中的一个 `@XxlJob` 方法。

新增任务时，核心配置如下：

| 配置项       | 示例值                                  | 说明                  |
| ------------ | --------------------------------------- | --------------------- |
| 执行器       | `订单服务生产执行器`                    | 选择前面创建的执行器  |
| 任务描述     | `订单超时关闭任务`                      | 描述任务用途          |
| 负责人       | `order-team`                            | 用于告警和责任归属    |
| 报警邮件     | `order@example.com`                     | 任务失败时通知        |
| 调度类型     | `CRON`                                  | 常用 Cron 调度        |
| Cron         | `0 */5 * * * ?`                         | 每 5 分钟执行         |
| 运行模式     | `BEAN`                                  | Spring Bean 任务      |
| JobHandler   | `orderTimeoutCloseJobHandler`           | 对应 `@XxlJob` 注解值 |
| 任务参数     | `{"timeoutMinutes":30,"batchSize":500}` | 任务运行参数          |
| 路由策略     | `轮询`                                  | 多实例执行器选择策略  |
| 阻塞处理策略 | `单机串行`                              | 任务堆积时的处理策略  |
| 任务超时时间 | `300`                                   | 单位秒，0 表示不限制  |
| 失败重试次数 | `2`                                     | 失败后重试次数        |

任务创建后，建议先不要直接启动自动调度，而是按以下步骤验证：

1. 点击“执行一次”，确认任务可以被手动触发。
2. 查看“调度日志”，确认任务结果为成功。
3. 点击日志详情，确认能看到 `XxlJobHelper.log` 输出。
4. 修改任务参数，再次手动触发，确认参数实时生效。
5. 确认无误后再启动任务自动调度。

任务参数示例：

```json
{
  "timeoutMinutes": 30,
  "batchSize": 500
}
```

对应代码中的 JobHandler 名称：

```java
@XxlJob("orderTimeoutCloseJobHandler")
public void orderTimeoutCloseJobHandler() {
    // 任务逻辑
}
```

调度中心的 `JobHandler` 必须与 `@XxlJob` 注解值完全一致，包括大小写。常见错误是调度中心填写了方法名，但代码中注解值不同，导致任务触发失败。

### 路由策略配置

路由策略用于决定调度中心在多个执行器实例中选择哪一个节点执行任务。XXL-JOB 在执行器集群部署时提供多种路由策略，包括第一个、最后一个、轮询、随机、一致性 HASH、最不经常使用、最近最久未使用、故障转移、忙碌转移和分片广播等。([GitHub](https://github.com/xuxueli/xxl-job?utm_source=chatgpt.com))

常用路由策略说明如下：

| 路由策略       | 说明                               | 适用场景                 |
| -------------- | ---------------------------------- | ------------------------ |
| 第一个         | 固定选择地址列表中的第一个执行器   | 临时指定主节点执行       |
| 最后一个       | 固定选择地址列表中的最后一个执行器 | 临时指定尾部节点执行     |
| 轮询           | 多个执行器实例轮流执行             | 普通无状态任务，推荐常用 |
| 随机           | 随机选择一个执行器实例             | 简单负载分散场景         |
| 一致性 HASH    | 同一任务尽量路由到稳定节点         | 有本地缓存或节点亲和需求 |
| 最不经常使用   | 优先选择使用频率低的节点           | 希望长期均衡分摊任务     |
| 最近最久未使用 | 优先选择较久未使用的节点           | 希望节点使用更均衡       |
| 故障转移       | 先检测可用性，再选择可用节点       | 稳定性要求较高的任务     |
| 忙碌转移       | 优先选择当前不忙的节点             | 长任务、容易阻塞的任务   |
| 分片广播       | 所有执行器实例都执行一次           | 大批量数据分片处理       |

一般任务推荐配置如下：

| 任务类型       | 推荐路由策略 | 原因                         |
| -------------- | ------------ | ---------------------------- |
| 普通定时任务   | 轮询         | 多实例均衡执行               |
| 轻量巡检任务   | 随机或轮询   | 无明显节点依赖               |
| 重要补偿任务   | 故障转移     | 优先保证任务能被可用节点执行 |
| 长耗时任务     | 忙碌转移     | 避免继续压到繁忙节点         |
| 大批量处理任务 | 分片广播     | 多实例并行处理               |
| 有节点缓存依赖 | 一致性 HASH  | 降低节点切换带来的缓存失效   |

对于“订单超时关闭”这类普通补偿任务，如果任务本身做了数据库状态条件控制，推荐使用 `轮询` 或 `故障转移`。如果任务需要所有实例同时参与处理大量数据，应使用 `分片广播`，并在代码中通过分片参数控制每个实例处理不同数据范围。

分片广播任务代码示例：

文件位置：`src/main/java/io/github/atengk/job/ShardJobHandler.java`

```java
package io.github.atengk.job;

import com.xxl.job.core.context.XxlJobHelper;
import com.xxl.job.core.handler.annotation.XxlJob;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * 分片任务处理器
 *
 * @author Ateng
 * @since 2026-05-07
 */
@Slf4j
@Component
public class ShardJobHandler {

    /**
     * 订单分片处理任务
     */
    @XxlJob("orderShardJobHandler")
    public void orderShardJobHandler() {
        int shardIndex = XxlJobHelper.getShardIndex();
        int shardTotal = XxlJobHelper.getShardTotal();

        XxlJobHelper.log("订单分片任务开始执行，shardIndex={}，shardTotal={}", shardIndex, shardTotal);
        log.info("订单分片任务开始执行，shardIndex={}，shardTotal={}", shardIndex, shardTotal);

        // 示例：实际项目中可使用 id % shardTotal = shardIndex 控制每个分片处理的数据
        int affectedRows = handleShardOrders(shardIndex, shardTotal);

        XxlJobHelper.handleSuccess("订单分片任务执行成功，处理数量：" + affectedRows);
    }

    /**
     * 处理当前分片订单
     *
     * @param shardIndex 当前分片序号
     * @param shardTotal 总分片数
     * @return 处理数量
     */
    private int handleShardOrders(int shardIndex, int shardTotal) {
        XxlJobHelper.log("开始处理当前分片订单，分片条件：id % {} = {}", shardTotal, shardIndex);

        // 示例代码：实际项目中应替换为真实数据库分页处理逻辑
        return 100;
    }
}
```

分片广播任务的 SQL 条件通常类似：

```sql
-- 示例：按订单 ID 取模分片，避免多个执行器处理同一批数据
SELECT *
FROM t_order
WHERE status = 'WAIT_PAY'
  AND MOD(id, #{shardTotal}) = #{shardIndex}
LIMIT #{batchSize};
```

### 阻塞处理策略配置

阻塞处理策略用于处理“上一次任务还没有执行完成，下一次调度又来了”的情况。XXL-JOB 支持单机串行、丢弃后续调度、覆盖之前调度等阻塞处理策略。([GitHub](https://github.com/xuxueli/xxl-job?utm_source=chatgpt.com))

常见阻塞处理策略如下：

| 阻塞处理策略 | 说明                                   | 适用场景                         |
| ------------ | -------------------------------------- | -------------------------------- |
| 单机串行     | 同一执行器节点上，同一任务排队串行执行 | 默认安全策略，适合大多数任务     |
| 丢弃后续调度 | 上一次未执行完时，新的调度直接丢弃     | 只关心最新周期，不允许堆积       |
| 覆盖之前调度 | 终止之前任务，执行新的调度             | 只关心最新结果，且任务可安全中断 |

推荐配置如下：

| 任务类型         | 推荐策略               | 原因                           |
| ---------------- | ---------------------- | ------------------------------ |
| 订单补偿任务     | 单机串行               | 避免同一任务并发处理同一批数据 |
| 报表统计任务     | 丢弃后续调度           | 报表任务过慢时避免调度堆积     |
| 缓存刷新任务     | 覆盖之前调度           | 只关心最新缓存结果             |
| 文件清理任务     | 单机串行               | 避免多个任务同时清理同一目录   |
| 第三方同步任务   | 单机串行               | 避免频繁调用第三方接口         |
| 长耗时批处理任务 | 丢弃后续调度或单机串行 | 根据是否允许堆积决定           |

配置建议：

1. 默认使用 `单机串行`，这是最稳妥的策略。
2. 如果任务执行时间可能超过调度间隔，并且不希望任务排队堆积，使用 `丢弃后续调度`。
3. 如果任务只需要保留最新执行结果，并且业务逻辑可以安全中断，可以使用 `覆盖之前调度`。
4. 对涉及资金、订单、库存、账单等强一致业务的任务，不建议使用 `覆盖之前调度`。
5. 对长耗时任务应同时配置合理的任务超时时间，避免任务长期占用执行线程。

订单超时关闭任务推荐配置：

| 配置项       | 推荐值               |
| ------------ | -------------------- |
| 路由策略     | `故障转移` 或 `轮询` |
| 阻塞处理策略 | `单机串行`           |
| 任务超时时间 | `300`                |
| 失败重试次数 | `2`                  |
| 调度类型     | `CRON`               |
| Cron         | `0 */5 * * * ?`      |

缓存刷新任务推荐配置：

| 配置项       | 推荐值           |
| ------------ | ---------------- |
| 路由策略     | `轮询`           |
| 阻塞处理策略 | `覆盖之前调度`   |
| 任务超时时间 | `120`            |
| 失败重试次数 | `1`              |
| 调度类型     | `CRON`           |
| Cron         | `0 */10 * * * ?` |

报表统计任务推荐配置：

| 配置项       | 推荐值         |
| ------------ | -------------- |
| 路由策略     | `故障转移`     |
| 阻塞处理策略 | `丢弃后续调度` |
| 任务超时时间 | `1800`         |
| 失败重试次数 | `1`            |
| 调度类型     | `CRON`         |
| Cron         | `0 10 1 * * ?` |

阻塞策略和路由策略需要一起考虑。路由策略决定任务调度到哪个执行器节点，阻塞策略决定同一个节点上任务来不及执行时如何处理。对于生产任务，建议先在测试环境模拟“任务执行时间大于调度间隔”的场景，确认阻塞策略符合业务预期后再上线。



## 典型任务示例

本节给出几类生产项目中常见的 XXL-JOB 任务示例，包括简单定时任务、带参数任务、分片广播任务和失败重试任务。XXL-JOB 支持 Cron 触发、人工触发、API 触发、执行器集群路由、分片广播、失败重试和 Rolling 实时日志等能力，典型任务开发时应重点关注参数校验、任务幂等、日志输出和异常返回。([GitHub](https://github.com/xuxueli/xxl-job?utm_source=chatgpt.com))

### 简单定时任务

简单定时任务适合处理固定逻辑、无外部参数或只依赖系统配置的场景，例如刷新缓存、检查服务状态、清理临时文件、生成固定周期统计数据等。此类任务一般不需要复杂参数，但仍然建议输出开始、过程和结束日志，便于在调度中心查看执行过程。

文件位置：`src/main/java/io/github/atengk/job/TypicalJobHandler.java`

```java
package io.github.atengk.job;

import cn.hutool.core.date.DateUtil;
import cn.hutool.core.util.RandomUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.json.JSONUtil;
import com.xxl.job.core.context.XxlJobHelper;
import com.xxl.job.core.handler.annotation.XxlJob;
import io.github.atengk.job.dto.InvoiceSyncJobParam;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.concurrent.TimeUnit;

/**
 * XXL-JOB 典型任务处理器
 *
 * @author Ateng
 * @since 2026-05-07
 */
@Slf4j
@Component
public class TypicalJobHandler {

    /**
     * 简单定时任务
     */
    @XxlJob("simpleTimerJobHandler")
    public void simpleTimerJobHandler() throws InterruptedException {
        String startTime = DateUtil.now();
        XxlJobHelper.log("简单定时任务开始执行，startTime={}", startTime);
        log.info("简单定时任务开始执行，startTime={}", startTime);

        for (int index = 1; index <= 3; index++) {
            XxlJobHelper.log("简单定时任务处理中，当前步骤={}", index);
            TimeUnit.SECONDS.sleep(1);
        }

        String endTime = DateUtil.now();
        XxlJobHelper.log("简单定时任务执行完成，endTime={}", endTime);
        log.info("简单定时任务执行完成，endTime={}", endTime);
        XxlJobHelper.handleSuccess("简单定时任务执行成功");
    }

    /**
     * 带参数任务
     */
    @XxlJob("invoiceSyncJobHandler")
    public void invoiceSyncJobHandler() {
        String jobParam = XxlJobHelper.getJobParam();
        XxlJobHelper.log("发票同步任务开始执行，原始参数={}", jobParam);
        log.info("发票同步任务开始执行");

        InvoiceSyncJobParam param = parseInvoiceSyncParam(jobParam);
        if (StrUtil.isBlank(param.getBizDate())) {
            XxlJobHelper.handleFail("业务日期不能为空");
            return;
        }
        if (param.getBatchSize() <= 0) {
            XxlJobHelper.handleFail("批次大小必须大于 0");
            return;
        }

        int affectedRows = syncInvoiceData(param);
        XxlJobHelper.log("发票同步任务执行完成，bizDate={}，batchSize={}，处理数量={}",
                param.getBizDate(), param.getBatchSize(), affectedRows);
        log.info("发票同步任务执行完成，处理数量={}", affectedRows);
        XxlJobHelper.handleSuccess(StrUtil.format("发票同步成功，处理数量：{}", affectedRows));
    }

    /**
     * 分片广播任务
     */
    @XxlJob("customerShardJobHandler")
    public void customerShardJobHandler() {
        int shardIndex = XxlJobHelper.getShardIndex();
        int shardTotal = XxlJobHelper.getShardTotal();

        XxlJobHelper.log("客户分片任务开始执行，shardIndex={}，shardTotal={}", shardIndex, shardTotal);
        log.info("客户分片任务开始执行，shardIndex={}，shardTotal={}", shardIndex, shardTotal);

        int affectedRows = handleCustomerShard(shardIndex, shardTotal);

        XxlJobHelper.log("客户分片任务执行完成，shardIndex={}，shardTotal={}，处理数量={}",
                shardIndex, shardTotal, affectedRows);
        log.info("客户分片任务执行完成，处理数量={}", affectedRows);
        XxlJobHelper.handleSuccess(StrUtil.format("客户分片处理成功，处理数量：{}", affectedRows));
    }

    /**
     * 失败重试任务
     */
    @XxlJob("retryDemoJobHandler")
    public void retryDemoJobHandler() {
        String requestNo = StrUtil.format("JOB-{}", DateUtil.format(DateUtil.date(), "yyyyMMddHHmmss"));
        XxlJobHelper.log("失败重试任务开始执行，requestNo={}", requestNo);
        log.info("失败重试任务开始执行，requestNo={}", requestNo);

        try {
            callUnstableRemoteService(requestNo);
            XxlJobHelper.handleSuccess("失败重试任务执行成功");
        } catch (Exception e) {
            XxlJobHelper.log(e);
            log.error("失败重试任务执行失败，requestNo={}", requestNo, e);
            XxlJobHelper.handleFail("失败重试任务执行失败：" + e.getMessage());
        }
    }

    /**
     * 解析发票同步任务参数
     *
     * @param jobParam 任务参数
     * @return 发票同步任务参数
     */
    private InvoiceSyncJobParam parseInvoiceSyncParam(String jobParam) {
        if (StrUtil.isBlank(jobParam)) {
            InvoiceSyncJobParam defaultParam = new InvoiceSyncJobParam();
            defaultParam.setBizDate(DateUtil.today());
            defaultParam.setBatchSize(500);
            return defaultParam;
        }

        if (!JSONUtil.isTypeJSON(jobParam)) {
            throw new IllegalArgumentException("任务参数必须是 JSON 格式");
        }

        return JSONUtil.toBean(jobParam, InvoiceSyncJobParam.class);
    }

    /**
     * 同步发票数据
     *
     * @param param 发票同步任务参数
     * @return 处理数量
     */
    private int syncInvoiceData(InvoiceSyncJobParam param) {
        XxlJobHelper.log("开始同步发票数据，bizDate={}，batchSize={}",
                param.getBizDate(), param.getBatchSize());

        // 示例：实际项目中应替换为 InvoiceService 或 Mapper 调用
        return Math.min(param.getBatchSize(), 256);
    }

    /**
     * 处理当前客户分片数据
     *
     * @param shardIndex 当前分片序号
     * @param shardTotal 总分片数
     * @return 处理数量
     */
    private int handleCustomerShard(int shardIndex, int shardTotal) {
        XxlJobHelper.log("开始处理当前客户分片，分片条件：id % {} = {}", shardTotal, shardIndex);

        // 示例：实际项目中建议使用 id 取模分片，避免多个执行器重复处理同一批数据
        return 100 + shardIndex;
    }

    /**
     * 调用不稳定的外部服务
     *
     * @param requestNo 请求编号
     */
    private void callUnstableRemoteService(String requestNo) {
        int randomValue = RandomUtil.randomInt(1, 10);
        XxlJobHelper.log("调用外部服务，requestNo={}，randomValue={}", requestNo, randomValue);

        if (randomValue <= 6) {
            throw new IllegalStateException("模拟外部服务短暂不可用");
        }

        XxlJobHelper.log("外部服务调用成功，requestNo={}", requestNo);
    }
}
```

带参数任务 DTO 放在 `job/dto` 包下，便于任务参数统一维护。

文件位置：`src/main/java/io/github/atengk/job/dto/InvoiceSyncJobParam.java`

```java
package io.github.atengk.job.dto;

import lombok.Data;

/**
 * 发票同步任务参数
 *
 * @author Ateng
 * @since 2026-05-07
 */
@Data
public class InvoiceSyncJobParam {

    /**
     * 业务日期，格式 yyyy-MM-dd
     */
    private String bizDate;

    /**
     * 单批处理数量
     */
    private int batchSize = 500;
}
```

### 带参数任务

带参数任务适合处理需要动态指定业务日期、批次大小、业务状态、租户标识或数据范围的场景。调度中心配置的“任务参数”会在执行时传给执行器，Bean 模式任务中通过 `XxlJobHelper.getJobParam()` 获取。

推荐使用 JSON 作为任务参数格式：

```json
{
  "bizDate": "2026-05-07",
  "batchSize": 500
}
```

调度中心配置示例：

| 配置项       | 示例值                                     |
| ------------ | ------------------------------------------ |
| 运行模式     | `BEAN`                                     |
| JobHandler   | `invoiceSyncJobHandler`                    |
| 任务参数     | `{"bizDate":"2026-05-07","batchSize":500}` |
| 路由策略     | `轮询`                                     |
| 阻塞处理策略 | `单机串行`                                 |
| 任务超时时间 | `300`                                      |
| 失败重试次数 | `1`                                        |

带参数任务开发建议如下：

| 建议           | 说明                                                     |
| -------------- | -------------------------------------------------------- |
| 使用 JSON 参数 | 便于增加字段，不依赖参数顺序                             |
| 参数设置默认值 | 避免调度中心漏填参数导致任务不可用                       |
| 参数必须校验   | 日期、批次大小、状态值都需要校验                         |
| 不传敏感字段   | Token、密码、密钥不应放入任务参数                        |
| 记录关键参数   | 使用 `XxlJobHelper.log` 输出业务日期、批次大小等关键字段 |
| 控制批处理规模 | 使用 `batchSize` 限制单次处理量，避免任务运行过久        |

### 分片广播任务

分片广播任务适合大批量数据处理场景。任务路由策略选择“分片广播”后，每个在线执行器实例都会执行一次同一个 JobHandler，代码中通过 `XxlJobHelper.getShardIndex()` 获取当前分片序号，通过 `XxlJobHelper.getShardTotal()` 获取总分片数。XXL-JOB 官方说明中，分片广播会广播触发集群中所有执行器执行一次，并支持根据分片参数开发分片任务。([GitHub](https://github.com/xuxueli/xxl-job?utm_source=chatgpt.com))

常见分片条件如下：

```sql
-- 示例：按主键 ID 取模分片，每个执行器只处理自己的分片数据
SELECT id, customer_name, status
FROM t_customer
WHERE status = 'WAIT_PROCESS'
  AND MOD(id, #{shardTotal}) = #{shardIndex}
LIMIT #{batchSize};
```

调度中心配置示例：

| 配置项       | 示例值                    |
| ------------ | ------------------------- |
| 运行模式     | `BEAN`                    |
| JobHandler   | `customerShardJobHandler` |
| 路由策略     | `分片广播`                |
| 阻塞处理策略 | `单机串行`                |
| 任务参数     | `{"batchSize":1000}`      |
| 任务超时时间 | `900`                     |
| 失败重试次数 | `1`                       |

分片任务需要注意以下几点：

1. 分片条件必须稳定，常用做法是 `id % shardTotal = shardIndex`。
2. 每个分片只能处理自己的数据，避免重复处理。
3. 分片总数会随在线执行器数量变化，任务逻辑不能依赖固定分片数量。
4. 分片广播任务重试时应保证幂等，避免部分分片重复执行导致数据重复处理。
5. 如果数据量很大，应在每个分片内部继续分页处理，不要一次性加载全部数据。

### 失败重试任务

失败重试任务适合处理第三方接口短暂不可用、网络抖动、数据库短暂异常、消息发送失败等临时性故障。XXL-JOB 支持配置任务失败重试次数，任务失败后会按配置主动重试；分片任务还支持分片粒度失败重试。([GitHub](https://github.com/xuxueli/xxl-job?utm_source=chatgpt.com))

调度中心配置示例：

| 配置项       | 示例值                |
| ------------ | --------------------- |
| 运行模式     | `BEAN`                |
| JobHandler   | `retryDemoJobHandler` |
| 路由策略     | `故障转移`            |
| 阻塞处理策略 | `单机串行`            |
| 任务超时时间 | `120`                 |
| 失败重试次数 | `3`                   |

失败重试任务的关键不是“能重试”，而是“重试后不会产生重复副作用”。生产任务必须具备幂等控制。

| 场景           | 幂等建议                             |
| -------------- | ------------------------------------ |
| 第三方接口调用 | 传递业务唯一请求号，例如 `requestNo` |
| 消息发送       | 使用消息流水表记录发送状态           |
| 订单状态变更   | SQL 更新时增加原状态条件             |
| 账单生成       | 使用唯一索引控制重复生成             |
| 文件处理       | 记录文件处理状态和文件哈希           |

订单状态更新类任务可以采用如下 SQL 思路：

```sql
-- 只更新待支付状态的数据，避免重试时重复关闭已处理订单
UPDATE t_order
SET status = 'CLOSED',
    close_time = NOW(),
    update_time = NOW()
WHERE id = #{orderId}
  AND status = 'WAIT_PAY';
```

如果更新影响行数为 `0`，通常表示该订单已经被其他流程处理，不应直接判定为系统异常。可以记录日志后跳过，保证任务重试安全。

## 本地开发与调试

本节用于说明如何在本地启动调度中心和执行器，并通过调度中心手动触发任务、查看执行日志。XXL-JOB 官方提供 `xuxueli/xxl-job-admin` Docker 镜像，可用于快速启动调度中心；调度中心依赖数据库保存任务、执行器和执行日志等数据。([Docker Hub](https://hub.docker.com/r/xuxueli/xxl-job-admin?utm_source=chatgpt.com))

### 本地启动调度中心

本地开发建议使用 Docker Compose 启动 MySQL 和 XXL-JOB Admin。需要注意的是，调度中心数据库初始化 SQL 应与所使用的 XXL-JOB 版本保持一致；如果使用源码方式启动，可以从对应版本源码中的 `tables_xxl_job.sql` 初始化数据库。

文件位置：`docker/xxl-job/docker-compose.yml`

```yaml
services:
  xxl-job-mysql:
    image: mysql:8.0
    container_name: xxl-job-mysql
    restart: unless-stopped
    environment:
      # 本地开发密码，生产环境必须修改
      MYSQL_ROOT_PASSWORD: 123456
      MYSQL_DATABASE: xxl_job
    ports:
      - "3306:3306"
    volumes:
      # MySQL 数据持久化目录
      - ./mysql/data:/var/lib/mysql
      # 初始化 SQL 目录，将 tables_xxl_job.sql 放到该目录
      - ./mysql/init:/docker-entrypoint-initdb.d
    command:
      # 设置字符集，避免中文乱码
      - --character-set-server=utf8mb4
      - --collation-server=utf8mb4_unicode_ci
    networks:
      - xxl-job-net

  xxl-job-admin:
    image: xuxueli/xxl-job-admin:3.4.0
    container_name: xxl-job-admin
    restart: unless-stopped
    depends_on:
      - xxl-job-mysql
    ports:
      - "8080:8080"
    environment:
      # 自定义 Spring Boot 启动参数，连接本地 MySQL 容器
      PARAMS: >-
        --spring.datasource.url=jdbc:mysql://xxl-job-mysql:3306/xxl_job?useUnicode=true&characterEncoding=UTF-8&autoReconnect=true&serverTimezone=Asia/Shanghai
        --spring.datasource.username=root
        --spring.datasource.password=123456
        --xxl.job.accessToken=default_token
    volumes:
      # 调度中心日志目录
      - ./admin/logs:/data/applogs
    networks:
      - xxl-job-net

networks:
  xxl-job-net:
    driver: bridge
```

如果本地镜像仓库没有 `3.4.0` 标签，需要将 `xuxueli/xxl-job-admin:3.4.0` 替换为当前调度中心实际使用的版本。执行器客户端版本建议与调度中心保持同一大版本。

启动命令如下：

```bash
cd docker/xxl-job

# 启动 MySQL 和 XXL-JOB Admin
docker compose up -d

# 查看容器状态
docker compose ps

# 查看调度中心启动日志
docker logs -f xxl-job-admin
```

`docker compose up -d` 用于后台启动服务，`docker compose ps` 用于查看容器运行状态，`docker logs -f xxl-job-admin` 用于观察调度中心是否启动成功。如果启动失败，优先检查 MySQL 初始化脚本、数据库连接地址、数据库账号密码和镜像版本。

调度中心启动后，本地访问地址通常为：

```text
http://127.0.0.1:8080/xxl-job-admin
```

首次进入后，需要在“执行器管理”中新增执行器：

| 字段     | 示例值                       |
| -------- | ---------------------------- |
| AppName  | `order-service-dev-executor` |
| 名称     | `订单服务本地执行器`         |
| 注册方式 | `自动注册`                   |
| 机器地址 | 留空                         |

### 本地启动执行器

本地执行器就是当前 Spring Boot 3 业务服务。启动前需要确认 `application-dev.yml` 中的调度中心地址、执行器 AppName、通讯端口和 Token 与调度中心一致。

文件位置：`src/main/resources/application-dev.yml`

```yaml
server:
  port: 18080

spring:
  application:
    name: order-service

xxl:
  job:
    admin:
      # 本地调度中心地址
      addresses: http://127.0.0.1:8080/xxl-job-admin

      # 本地 Token，需要与调度中心保持一致
      accessToken: default_token

      # 调度中心通讯超时时间，单位秒
      timeout: 3

    executor:
      # 本地执行器名称，必须与调度中心执行器 AppName 一致
      appname: order-service-dev-executor

      # 本地开发建议使用固定端口
      port: 9999

      # 本地任务日志目录
      logpath: ./logs/xxl-job/jobhandler

      # 本地日志保留 7 天
      logretentiondays: 7
```

通过 Maven 启动执行器：

```bash
# 在项目根目录执行
mvn spring-boot:run -Dspring-boot.run.profiles=dev
```

或者通过 Jar 包启动：

```bash
# 先构建应用
mvn clean package -DskipTests

# 启动 dev 环境
java -jar target/order-service.jar --spring.profiles.active=dev
```

启动后检查执行器端口：

```bash
# Linux 或 macOS
ss -lntp | grep 9999 || netstat -an | grep 9999

# 查看本地任务日志目录
ls -al ./logs/xxl-job/jobhandler
```

如果执行器启动成功，调度中心“执行器管理”页面中对应 AppName 下会出现在线机器地址，例如：

```text
http://192.168.1.10:9999
```

如果本地调度中心运行在 Docker 中，而执行器运行在宿主机，通常 `127.0.0.1` 对执行器访问调度中心没有问题；但调度中心回调执行器时，需要能访问执行器真实 IP 和 `9999` 端口。如果自动识别的 IP 不可达，可以在本地配置中显式指定：

```yaml
xxl:
  job:
    executor:
      # 替换为宿主机局域网 IP
      ip: 192.168.1.10
      port: 9999
```

### 手动触发任务

手动触发任务用于在不等待 Cron 时间到达的情况下验证任务是否可执行。开发阶段建议所有任务先通过“执行一次”验证通过，再启动自动调度。

调度中心配置简单任务：

| 配置项       | 示例值                  |
| ------------ | ----------------------- |
| 执行器       | `订单服务本地执行器`    |
| 任务描述     | `简单定时任务`          |
| 调度类型     | `CRON`                  |
| Cron         | `0 */5 * * * ?`         |
| 运行模式     | `BEAN`                  |
| JobHandler   | `simpleTimerJobHandler` |
| 路由策略     | `轮询`                  |
| 阻塞处理策略 | `单机串行`              |
| 任务超时时间 | `120`                   |
| 失败重试次数 | `0`                     |

调度中心配置带参数任务：

| 配置项       | 示例值                                     |
| ------------ | ------------------------------------------ |
| 执行器       | `订单服务本地执行器`                       |
| 任务描述     | `发票同步任务`                             |
| 调度类型     | `CRON`                                     |
| Cron         | `0 0/10 * * * ?`                           |
| 运行模式     | `BEAN`                                     |
| JobHandler   | `invoiceSyncJobHandler`                    |
| 任务参数     | `{"bizDate":"2026-05-07","batchSize":500}` |
| 路由策略     | `轮询`                                     |
| 阻塞处理策略 | `单机串行`                                 |
| 任务超时时间 | `300`                                      |
| 失败重试次数 | `1`                                        |

手动触发步骤：

1. 进入调度中心“任务管理”。
2. 找到目标任务。
3. 点击“操作”中的“执行一次”。
4. 如果任务有参数，确认任务参数是否符合预期。
5. 进入“调度日志”查看本次执行结果。
6. 点击日志详情，查看 `XxlJobHelper.log` 输出。

手动触发成功后，应同时验证以下内容：

| 验证项       | 预期结果                         |
| ------------ | -------------------------------- |
| 调度结果     | 显示成功                         |
| 执行器地址   | 指向本地执行器地址               |
| Handler 匹配 | 没有出现 JobHandler 不存在异常   |
| 参数解析     | 日志中能看到解析后的参数         |
| 任务日志     | 能看到开始、过程、结束日志       |
| 应用日志     | 控制台或应用日志中能看到业务日志 |

### 查看执行日志

XXL-JOB 执行日志分为调度中心日志和执行器任务日志。调度中心日志用于查看任务是否触发、触发到哪个执行器、返回结果是什么；执行器任务日志用于查看任务代码内部通过 `XxlJobHelper.log` 输出的过程日志。XXL-JOB 支持 Rolling 实时日志，便于在线查看任务执行过程。([GitHub](https://github.com/xuxueli/xxl-job?utm_source=chatgpt.com))

调度中心查看方式：

1. 进入“调度日志”。
2. 按任务、时间、执行状态过滤日志。
3. 点击“执行日志”或“日志详情”。
4. 查看调度结果、执行结果、执行地址和任务输出。

本地文件查看方式：

```bash
# 查看本地执行器任务日志目录
find ./logs/xxl-job/jobhandler -type f | sort | tail -20

# 实时查看最近一个任务日志文件
tail -f "$(find ./logs/xxl-job/jobhandler -type f | sort | tail -1)"
```

`find` 用于检索执行器任务日志文件，`sort | tail -20` 用于查看最近生成的日志文件，`tail -f` 用于实时跟踪最新日志内容。

如果调度中心无法查看任务日志，按以下顺序排查：

| 问题               | 排查方向                                       |
| ------------------ | ---------------------------------------------- |
| 日志详情为空       | 检查任务中是否使用 `XxlJobHelper.log` 输出日志 |
| 提示执行器连接失败 | 检查调度中心是否能访问执行器 `ip:port`         |
| 日志文件不存在     | 检查 `xxl.job.executor.logpath` 是否正确       |
| 权限不足           | 检查应用进程是否具备日志目录写入权限           |
| 任务未执行         | 检查 JobHandler 名称、执行器在线状态和调度日志 |
| 日志过早清理       | 检查 `xxl.job.executor.logretentiondays` 配置  |

本地调试阶段建议保留以下日志输出：

```java
XxlJobHelper.log("任务开始执行，param={}", XxlJobHelper.getJobParam());
log.info("任务开始执行，param={}", XxlJobHelper.getJobParam());

try {
    // 执行业务逻辑
    XxlJobHelper.log("任务执行完成");
    log.info("任务执行完成");
    XxlJobHelper.handleSuccess("任务执行成功");
} catch (Exception e) {
    XxlJobHelper.log(e);
    log.error("任务执行失败", e);
    XxlJobHelper.handleFail("任务执行失败：" + e.getMessage());
}
```

本地调试完成后，再根据任务类型配置正式 Cron、路由策略、阻塞处理策略、超时时间和失败重试次数。对于会修改业务数据的任务，建议先在测试环境使用小批次参数验证，例如 `batchSize=10`，确认日志和数据结果符合预期后再放大批次。



## 部署说明

本节用于说明 XXL-JOB 执行器服务和调度中心在测试、预发、生产环境中的部署方式。生产部署时需要重点关注服务可用性、数据库持久化、执行器端口暴露、Token 安全、日志目录持久化和调度中心到执行器的网络连通性。

### 执行器部署

执行器部署指的是将已经接入 XXL-JOB 的 Spring Boot 3 业务服务部署到服务器、容器或 Kubernetes 环境中。执行器服务除了需要暴露业务端口 `server.port`，还需要暴露 XXL-JOB 执行器通讯端口 `xxl.job.executor.port`，调度中心会通过该端口触发任务。

生产环境推荐配置如下：

```yaml
# 文件位置：src/main/resources/application-prod.yml

server:
  # 业务服务端口
  port: 18080

spring:
  application:
    name: order-service

xxl:
  job:
    admin:
      # 生产调度中心地址，多个地址使用英文逗号分隔
      addresses: http://xxl-job-admin-01:8080/xxl-job-admin,http://xxl-job-admin-02:8080/xxl-job-admin

      # 生产 Token 通过环境变量注入，不建议明文写入配置文件
      accessToken: ${XXL_JOB_ACCESS_TOKEN}

      # 调度中心通讯超时时间，单位秒
      timeout: 5

    executor:
      # 生产执行器 AppName，必须与调度中心执行器管理中的 AppName 一致
      appname: order-service-prod-executor

      # 执行器通讯端口，不能与业务端口和同机其他执行器端口冲突
      port: 9999

      # 容器或多网卡环境建议通过环境变量显式指定
      ip: ${POD_IP:}

      # 如需固定注册地址，可配置完整地址
      address: ${XXL_JOB_EXECUTOR_ADDRESS:}

      # 任务日志目录，必须持久化并保证应用用户可写
      logpath: /data/logs/order-service/xxl-job/jobhandler

      # 任务日志保留天数
      logretentiondays: 30
```

如果使用普通 Jar 包部署，可以通过 systemd 管理执行器服务。

文件位置：`/etc/systemd/system/order-service.service`

```ini
[Unit]
Description=Order Service
After=network.target

[Service]
Type=simple
User=app
Group=app
WorkingDirectory=/data/apps/order-service

# 生产环境变量，Token 不写入 application-prod.yml
Environment="SPRING_PROFILES_ACTIVE=prod"
Environment="XXL_JOB_ACCESS_TOKEN=替换为生产调度中心Token"

# 启动 Spring Boot 业务服务
ExecStart=/usr/bin/java -Xms512m -Xmx1024m -jar /data/apps/order-service/order-service.jar

# 异常退出后自动重启
Restart=always
RestartSec=10

# 标准输出和错误输出写入 journald
StandardOutput=journal
StandardError=journal

[Install]
WantedBy=multi-user.target
```

部署和启动命令如下：

```bash
# 创建应用目录和日志目录
mkdir -p /data/apps/order-service
mkdir -p /data/logs/order-service/xxl-job/jobhandler

# 授权给应用运行用户
chown -R app:app /data/apps/order-service
chown -R app:app /data/logs/order-service

# 复制构建后的 Jar 包
cp target/order-service.jar /data/apps/order-service/order-service.jar

# 加载 systemd 配置
systemctl daemon-reload

# 启动服务
systemctl start order-service

# 设置开机自启
systemctl enable order-service

# 查看服务状态
systemctl status order-service
```

`mkdir -p` 用于创建部署目录和任务日志目录，`chown -R` 用于保证应用运行用户具备目录读写权限，`systemctl daemon-reload` 用于重新加载 systemd 配置，`systemctl start` 用于启动服务，`systemctl status` 用于查看服务运行状态。

如果使用 Docker 部署执行器，需要同时暴露业务端口和执行器通讯端口，并挂载日志目录：

```bash
docker run -d \
  --name order-service \
  --restart unless-stopped \
  -p 18080:18080 \
  -p 9999:9999 \
  -e SPRING_PROFILES_ACTIVE=prod \
  -e XXL_JOB_ACCESS_TOKEN='替换为生产调度中心Token' \
  -e XXL_JOB_EXECUTOR_ADDRESS='http://宿主机IP:9999' \
  -v /data/logs/order-service:/data/logs/order-service \
  order-service:latest
```

`-p 18080:18080` 暴露业务接口端口，`-p 9999:9999` 暴露 XXL-JOB 执行器通讯端口，`XXL_JOB_EXECUTOR_ADDRESS` 用于在容器网络复杂时指定调度中心可访问的执行器地址，`-v` 用于持久化任务日志目录。

### 调度中心部署

调度中心部署指的是部署 `xxl-job-admin` 管理端。调度中心依赖数据库保存执行器、任务、调度日志和执行日志索引等数据，因此生产环境必须使用稳定的 MySQL 实例，并做好数据库备份、账号权限和高可用配置。

生产环境调度中心建议采用独立服务部署，数据库单独部署：

| 组件            | 部署建议                           |
| --------------- | ---------------------------------- |
| `xxl-job-admin` | 至少部署 2 个实例，前面接负载均衡  |
| MySQL           | 使用独立数据库实例或数据库集群     |
| 日志目录        | 挂载到持久化磁盘                   |
| 访问入口        | 通过 Nginx、Ingress 或内网域名访问 |
| 账号安全        | 修改默认密码，限制后台访问来源     |
| Token           | 配置非默认 `accessToken`           |

Docker Compose 生产化示例可作为测试或小规模环境参考：

```yaml
# 文件位置：deploy/xxl-job-admin/docker-compose.yml

services:
  xxl-job-admin:
    image: xuxueli/xxl-job-admin:3.4.0
    container_name: xxl-job-admin
    restart: unless-stopped
    ports:
      - "8080:8080"
    environment:
      PARAMS: >-
        --spring.datasource.url=jdbc:mysql://mysql-prod:3306/xxl_job?useUnicode=true&characterEncoding=UTF-8&autoReconnect=true&serverTimezone=Asia/Shanghai
        --spring.datasource.username=xxl_job
        --spring.datasource.password=替换为数据库密码
        --xxl.job.accessToken=替换为生产通讯Token
    volumes:
      # 调度中心日志目录
      - /data/logs/xxl-job-admin:/data/applogs
    networks:
      - xxl-job-net

networks:
  xxl-job-net:
    driver: bridge
```

启动命令如下：

```bash
cd deploy/xxl-job-admin

# 启动调度中心
docker compose up -d

# 查看容器状态
docker compose ps

# 查看启动日志
docker logs -f xxl-job-admin
```

如果调度中心使用 Nginx 统一入口，可以增加反向代理配置。

文件位置：`/etc/nginx/conf.d/xxl-job-admin.conf`

```nginx
server {
    listen 80;
    server_name xxl-job-admin.example.com;

    # 调度中心后台访问入口
    location /xxl-job-admin/ {
        proxy_pass http://127.0.0.1:8080/xxl-job-admin/;
        proxy_set_header Host $host;
        proxy_set_header X-Real-IP $remote_addr;
        proxy_set_header X-Forwarded-For $proxy_add_x_forwarded_for;

        # 调度中心接口请求可能需要较长等待时间
        proxy_connect_timeout 10s;
        proxy_read_timeout 60s;
        proxy_send_timeout 60s;
    }
}
```

应用 Nginx 配置：

```bash
# 检查 Nginx 配置
nginx -t

# 重新加载 Nginx
nginx -s reload
```

`nginx -t` 用于检查配置语法，`nginx -s reload` 用于平滑加载新配置。生产环境建议进一步增加访问控制，只允许办公网、VPN 或运维网段访问调度中心后台。

### 网络与端口配置

XXL-JOB 网络模型中存在两个方向的访问：执行器访问调度中心，用于注册和回调；调度中心访问执行器，用于触发任务和拉取任务日志。很多生产问题都不是代码问题，而是这两个方向的网络不通。

端口说明如下：

| 端口    | 方向                            | 说明                     |
| ------- | ------------------------------- | ------------------------ |
| `8080`  | 浏览器或执行器 → 调度中心       | 调度中心 Web 和接口端口  |
| `18080` | 用户或内部系统 → 执行器业务服务 | Spring Boot 业务服务端口 |
| `9999`  | 调度中心 → 执行器               | XXL-JOB 执行器通讯端口   |
| `3306`  | 调度中心 → MySQL                | 调度中心数据库端口       |

网络连通性验证命令如下：

```bash
# 在执行器服务器上验证能否访问调度中心
curl -I http://xxl-job-admin.example.com/xxl-job-admin/

# 在调度中心服务器上验证能否访问执行器端口
nc -vz 10.0.0.21 9999

# 查看执行器端口是否监听
ss -lntp | grep 9999

# 查看业务端口是否监听
ss -lntp | grep 18080
```

`curl -I` 用于验证调度中心 HTTP 地址是否可访问，`nc -vz` 用于验证调度中心到执行器端口是否连通，`ss -lntp` 用于确认本机端口监听状态。如果 `nc` 不存在，可以使用 `telnet` 或临时安装网络工具包。

生产环境网络配置建议如下：

| 配置项         | 建议                                                         |
| -------------- | ------------------------------------------------------------ |
| 调度中心后台   | 只允许办公网、VPN 或堡垒机访问                               |
| 执行器通讯端口 | 只允许调度中心访问                                           |
| `accessToken`  | 所有环境必须配置非空 Token                                   |
| 容器网络       | 显式配置调度中心可访问的执行器地址                           |
| 多网卡机器     | 显式配置 `xxl.job.executor.ip`                               |
| Kubernetes     | 使用 Pod IP、HostNetwork 或固定 Service 方案，按实际网络模型选择 |

如果执行器部署在 Kubernetes 中，常见配置如下：

```yaml
env:
  # 注入 Pod IP，供执行器向调度中心注册
  - name: POD_IP
    valueFrom:
      fieldRef:
        fieldPath: status.podIP

  # 注入生产 Token
  - name: XXL_JOB_ACCESS_TOKEN
    valueFrom:
      secretKeyRef:
        name: xxl-job-secret
        key: access-token

ports:
  # 业务服务端口
  - name: http
    containerPort: 18080

  # XXL-JOB 执行器通讯端口
  - name: xxl-job
    containerPort: 9999
```

Kubernetes 环境要重点确认调度中心能否直接访问 Pod IP。如果调度中心部署在集群外部，Pod IP 通常不可达，此时应通过 NodePort、HostNetwork、固定网关或专用网络方案暴露执行器端口。

## 测试验证

本节用于说明 XXL-JOB 接入后的验证流程。测试验证应覆盖执行器注册、任务调度、异常场景和日志查看，确保任务不仅能执行成功，也能在失败、超时、网络异常和参数错误时正确暴露问题。

### 注册验证

注册验证用于确认执行器服务启动后是否成功注册到调度中心。只有执行器在线，调度中心才能触发任务。

验证步骤如下：

1. 启动调度中心。
2. 在调度中心“执行器管理”中创建对应 AppName。
3. 启动 Spring Boot 执行器服务。
4. 进入“执行器管理”查看在线机器地址。
5. 确认在线地址中的 IP 和端口能被调度中心访问。

执行器本机检查命令：

```bash
# 查看应用进程
ps -ef | grep order-service | grep -v grep

# 查看执行器端口是否监听
ss -lntp | grep 9999

# 查看应用启动日志
journalctl -u order-service -n 200 --no-pager
```

调度中心服务器检查命令：

```bash
# 从调度中心服务器访问执行器端口
nc -vz 10.0.0.21 9999

# 如果执行器通过域名或代理暴露，检查代理地址
curl -I http://order-service-job.example.com
```

注册成功的判断标准如下：

| 验证项     | 预期结果                            |
| ---------- | ----------------------------------- |
| 执行器进程 | Spring Boot 应用正常运行            |
| 执行器端口 | `9999` 端口处于监听状态             |
| 执行器管理 | 调度中心显示在线机器地址            |
| AppName    | 调度中心 AppName 与配置文件完全一致 |
| Token      | 调度中心与执行器 Token 一致         |
| 网络       | 调度中心可以访问执行器通讯端口      |

### 调度验证

调度验证用于确认调度中心能否正确触发执行器中的 JobHandler。建议先验证简单任务，再验证带参数任务、分片任务和长耗时任务。

简单任务验证配置如下：

| 配置项       | 示例值                        |
| ------------ | ----------------------------- |
| 执行器       | `order-service-prod-executor` |
| 运行模式     | `BEAN`                        |
| JobHandler   | `simpleTimerJobHandler`       |
| 路由策略     | `轮询`                        |
| 阻塞处理策略 | `单机串行`                    |
| 任务超时时间 | `120`                         |
| 失败重试次数 | `0`                           |

验证步骤如下：

1. 在调度中心“任务管理”中新建任务。
2. 点击“执行一次”。
3. 进入“调度日志”查看执行结果。
4. 点击“执行日志”查看任务内部日志。
5. 确认应用日志中也有对应业务日志。
6. 启动任务自动调度，等待 Cron 到达后再次检查结果。

应用日志检查命令：

```bash
# systemd 部署方式查看应用日志
journalctl -u order-service -f

# Docker 部署方式查看应用日志
docker logs -f order-service

# 文件日志方式查看应用日志
tail -f /data/logs/order-service/application.log
```

调度成功的判断标准如下：

| 验证项     | 预期结果                         |
| ---------- | -------------------------------- |
| 调度日志   | 调度结果为成功                   |
| 执行日志   | 任务日志能正常打开               |
| 执行地址   | 指向正确的执行器实例             |
| JobHandler | 未出现 Handler 不存在异常        |
| 任务参数   | 参数可以被正确解析               |
| 业务结果   | 数据状态、日志或输出结果符合预期 |

### 异常场景验证

异常场景验证用于确认任务失败时能正确返回失败状态、记录日志、触发重试，并且不会造成重复数据或脏数据。生产任务上线前必须至少验证参数错误、业务异常、超时、重复触发和执行器不可用几类场景。

推荐异常验证项如下：

| 场景           | 验证方式                     | 预期结果                      |
| -------------- | ---------------------------- | ----------------------------- |
| 参数为空       | 清空任务参数后手动触发       | 任务按默认值执行或明确失败    |
| 参数格式错误   | 传入非 JSON 参数             | 任务失败并输出参数错误原因    |
| Handler 不存在 | 故意填写错误 JobHandler      | 调度失败并提示 Handler 不存在 |
| 执行器离线     | 停止执行器后手动触发         | 调度失败或路由到其他可用节点  |
| Token 不一致   | 修改执行器 Token 后重启      | 注册或触发失败                |
| 任务超时       | 设置较短超时时间并执行长任务 | 调度中心显示超时失败          |
| 失败重试       | 任务主动抛出异常             | 按失败重试次数重试            |
| 重复调度       | 调度间隔小于任务执行时长     | 按阻塞处理策略处理            |

用于验证异常和重试的示例 Handler 如下。

文件位置：`src/main/java/io/github/atengk/job/VerifyJobHandler.java`

```java
package io.github.atengk.job;

import cn.hutool.core.util.StrUtil;
import com.xxl.job.core.context.XxlJobHelper;
import com.xxl.job.core.handler.annotation.XxlJob;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.concurrent.TimeUnit;

/**
 * XXL-JOB 验证任务处理器
 *
 * @author Ateng
 * @since 2026-05-07
 */
@Slf4j
@Component
public class VerifyJobHandler {

    /**
     * 参数异常验证任务
     */
    @XxlJob("paramErrorVerifyJobHandler")
    public void paramErrorVerifyJobHandler() {
        String jobParam = XxlJobHelper.getJobParam();
        XxlJobHelper.log("参数异常验证任务开始执行，param={}", jobParam);
        log.info("参数异常验证任务开始执行，param={}", jobParam);

        if (StrUtil.isBlank(jobParam)) {
            XxlJobHelper.handleFail("任务参数不能为空");
            return;
        }

        XxlJobHelper.handleSuccess("参数异常验证任务执行成功");
    }

    /**
     * 超时验证任务
     *
     * @throws InterruptedException 线程中断异常
     */
    @XxlJob("timeoutVerifyJobHandler")
    public void timeoutVerifyJobHandler() throws InterruptedException {
        XxlJobHelper.log("超时验证任务开始执行，预计休眠 120 秒");
        log.info("超时验证任务开始执行");

        TimeUnit.SECONDS.sleep(120);

        XxlJobHelper.log("超时验证任务执行完成");
        log.info("超时验证任务执行完成");
        XxlJobHelper.handleSuccess("超时验证任务执行成功");
    }

    /**
     * 失败重试验证任务
     */
    @XxlJob("retryVerifyJobHandler")
    public void retryVerifyJobHandler() {
        XxlJobHelper.log("失败重试验证任务开始执行");
        log.info("失败重试验证任务开始执行");

        throw new IllegalStateException("模拟任务执行失败，用于验证失败重试");
    }
}
```

对应调度中心配置建议：

| JobHandler                   | 验证点       | 推荐配置         |
| ---------------------------- | ------------ | ---------------- |
| `paramErrorVerifyJobHandler` | 参数错误处理 | 失败重试次数 `0` |
| `timeoutVerifyJobHandler`    | 任务超时     | 超时时间 `10` 秒 |
| `retryVerifyJobHandler`      | 失败重试     | 失败重试次数 `2` |

异常验证完成后，应删除或停用验证任务，避免误触发影响生产环境。

### 日志验证

日志验证用于确认调度中心可以查看任务执行过程，执行器本地也能生成任务日志。XXL-JOB 的任务过程日志需要通过 `XxlJobHelper.log` 输出，普通 `log.info` 只会进入应用日志，不一定展示在调度中心任务日志中。

验证步骤如下：

1. 确认 `xxl.job.executor.logpath` 配置正确。
2. 确认日志目录存在并且应用用户可写。
3. 手动触发一个包含 `XxlJobHelper.log` 的任务。
4. 在调度中心“调度日志”中查看执行日志。
5. 在执行器服务器上查看日志文件是否生成。
6. 验证异常任务中是否能看到完整异常堆栈。

本地文件检查命令如下：

```bash
# 查看日志目录权限
ls -ld /data/logs/order-service/xxl-job/jobhandler

# 查看最近生成的任务日志文件
find /data/logs/order-service/xxl-job/jobhandler -type f | sort | tail -20

# 实时查看最新任务日志
tail -f "$(find /data/logs/order-service/xxl-job/jobhandler -type f | sort | tail -1)"
```

日志验证通过标准如下：

| 验证项   | 预期结果                   |
| -------- | -------------------------- |
| 日志目录 | 目录存在且应用用户可写     |
| 日志文件 | 手动触发任务后生成新日志   |
| 调度中心 | 可以打开执行日志页面       |
| 正常任务 | 能看到开始、过程、结束日志 |
| 异常任务 | 能看到失败原因和异常堆栈   |
| 日志清理 | 超过保留天数后可自动清理   |

## 常见问题

本节整理 Spring Boot 3 集成 XXL-JOB 时常见问题及处理方式。排障时建议按照“配置是否正确、服务是否启动、端口是否监听、网络是否互通、Token 是否一致、JobHandler 是否存在、日志目录是否可写”的顺序逐项检查。

### 执行器注册失败

执行器注册失败通常表现为调度中心“执行器管理”中没有在线机器地址，或者执行器服务启动日志中出现注册失败、认证失败、连接调度中心失败等信息。

常见原因如下：

| 原因                   | 现象                       | 处理方式                                      |
| ---------------------- | -------------------------- | --------------------------------------------- |
| 调度中心地址错误       | 执行器日志连接失败         | 检查 `xxl.job.admin.addresses`                |
| AppName 不一致         | 调度中心看不到对应执行器   | 保证配置文件和执行器管理中的 AppName 完全一致 |
| Token 不一致           | 注册或调用失败             | 保证调度中心和执行器 Token 一致               |
| 执行器端口被占用       | 应用启动失败或端口未监听   | 修改 `xxl.job.executor.port`                  |
| 调度中心无法访问执行器 | 执行器在线不稳定或触发失败 | 检查防火墙、安全组、容器端口映射              |
| 多网卡 IP 识别错误     | 注册了不可达 IP            | 显式配置 `xxl.job.executor.ip`                |
| 容器注册地址错误       | 注册为容器内部 IP          | 显式配置 `xxl.job.executor.address`           |
| 配置类未加载           | 没有初始化执行器           | 检查配置类包路径和 `@Configuration`           |

执行器本机排查命令：

```bash
# 查看执行器端口是否监听
ss -lntp | grep 9999

# 查看应用启动日志
journalctl -u order-service -n 300 --no-pager

# 检查配置文件是否包含 XXL-JOB 配置
grep -R "xxl:" -n /data/apps/order-service/config /data/apps/order-service 2>/dev/null
```

调度中心服务器排查命令：

```bash
# 验证调度中心到执行器端口的连通性
nc -vz 10.0.0.21 9999

# 验证调度中心后台是否可访问
curl -I http://127.0.0.1:8080/xxl-job-admin/
```

如果是 Kubernetes 环境，优先确认执行器注册到调度中心的地址是否为调度中心可达地址。Pod IP、Service IP、NodePort、Ingress 地址的可达性取决于调度中心所在网络，不能只看执行器容器内部是否正常。

### 任务触发失败

任务触发失败通常表现为“调度日志”中调度失败、执行失败、Handler 不存在、执行器无可用地址、请求超时或返回失败结果。

常见原因如下：

| 原因                | 现象                 | 处理方式                            |
| ------------------- | -------------------- | ----------------------------------- |
| 执行器离线          | 提示无可用执行器     | 检查执行器是否在线                  |
| JobHandler 填写错误 | 提示 Handler 不存在  | 检查 `@XxlJob` 注解值和调度中心配置 |
| 路由策略不合适      | 固定路由到异常节点   | 改为轮询、故障转移或忙碌转移        |
| 任务参数错误        | 业务代码返回失败     | 检查任务参数格式和字段              |
| 任务超时            | 调度日志显示超时     | 调整超时时间或优化任务耗时          |
| 执行器端口不可达    | 请求执行器失败       | 检查防火墙、安全组、端口映射        |
| 业务代码异常        | 执行日志出现异常堆栈 | 根据异常日志修复代码                |
| 阻塞策略影响        | 后续调度被丢弃或覆盖 | 检查阻塞处理策略                    |

排查 JobHandler 是否一致：

```java
@XxlJob("invoiceSyncJobHandler")
public void invoiceSyncJobHandler() {
    // 任务逻辑
}
```

调度中心中必须填写：

```text
invoiceSyncJobHandler
```

如果任务参数是 JSON，调度中心参数应保持合法 JSON 格式：

```json
{
  "bizDate": "2026-05-07",
  "batchSize": 500
}
```

任务触发失败排查顺序：

1. 查看调度中心“调度日志”的调度结果和执行结果。
2. 查看执行器是否在线。
3. 检查 JobHandler 是否与 `@XxlJob` 注解值一致。
4. 检查任务参数是否符合代码解析逻辑。
5. 检查执行器应用日志是否存在业务异常。
6. 检查调度中心是否能访问执行器端口。
7. 检查路由策略和阻塞处理策略是否符合预期。

### 日志无法查看

日志无法查看通常表现为调度中心点击“执行日志”后为空、报错、连接执行器失败，或者执行器服务器上没有生成任务日志文件。

常见原因如下：

| 原因                      | 现象                   | 处理方式                                  |
| ------------------------- | ---------------------- | ----------------------------------------- |
| 未使用 `XxlJobHelper.log` | 调度中心日志内容为空   | 在任务中增加 `XxlJobHelper.log`           |
| 日志目录不存在            | 执行器无法写日志       | 创建 `xxl.job.executor.logpath` 目录      |
| 目录权限不足              | 任务日志写入失败       | 授权给应用运行用户                        |
| 容器未挂载日志目录        | 容器重建后日志丢失     | 使用 `-v` 或持久卷挂载                    |
| 调度中心访问不到执行器    | 日志拉取失败           | 检查执行器注册地址和端口                  |
| 日志保留天数过短          | 历史日志被清理         | 调整 `logretentiondays`                   |
| 只写应用日志              | 调度中心看不到业务日志 | 同时使用 `XxlJobHelper.log` 和 `log.info` |

日志目录修复命令：

```bash
# 创建任务日志目录
mkdir -p /data/logs/order-service/xxl-job/jobhandler

# 设置目录属主
chown -R app:app /data/logs/order-service/xxl-job

# 设置基础权限
chmod 755 /data/logs/order-service/xxl-job/jobhandler

# 检查目录权限
ls -ld /data/logs/order-service/xxl-job/jobhandler
```

任务代码中建议同时输出调度日志和应用日志：

```java
XxlJobHelper.log("任务开始执行，param={}", XxlJobHelper.getJobParam());
log.info("任务开始执行，param={}", XxlJobHelper.getJobParam());

try {
    // 执行业务逻辑
    XxlJobHelper.log("任务执行完成");
    log.info("任务执行完成");
    XxlJobHelper.handleSuccess("任务执行成功");
} catch (Exception e) {
    XxlJobHelper.log(e);
    log.error("任务执行失败", e);
    XxlJobHelper.handleFail("任务执行失败：" + e.getMessage());
}
```

如果调度中心能看到调度记录，但打不开执行日志，应重点检查调度中心到执行器注册地址的网络连通性。调度中心查看日志时依赖执行器地址，如果执行器注册的是容器内网 IP、Pod IP 或错误网卡 IP，就可能出现任务能注册但日志拉取失败的情况。

### Spring Boot 3 兼容问题

Spring Boot 3 兼容问题主要集中在 JDK 版本、依赖版本、`javax.*` 到 `jakarta.*` 包迁移、老版本 XXL-JOB 客户端兼容性以及第三方依赖冲突。新项目建议使用 JDK 17、Spring Boot 3.x 和 XXL-JOB 3.x 作为同一套技术基线。

常见兼容问题如下：

| 问题                     | 现象                       | 处理方式                                    |
| ------------------------ | -------------------------- | ------------------------------------------- |
| JDK 版本过低             | 应用启动失败               | 使用 JDK 17 或更高版本                      |
| XXL-JOB 客户端版本过旧   | 编译或运行异常             | 升级到与调度中心兼容的 3.x 客户端           |
| `javax.*` 依赖冲突       | 类找不到或 Bean 初始化失败 | 替换为 `jakarta.*` 兼容依赖                 |
| Spring Boot 2 老配置迁移 | 配置项不生效               | 检查配置绑定类和 YAML 层级                  |
| 日志依赖冲突             | 启动时出现日志实现冲突     | 统一使用 Spring Boot 默认日志体系           |
| 配置类未扫描             | 执行器未初始化             | 保证配置类在启动类扫描路径下                |
| AOT 或原生镜像问题       | 运行期反射异常             | 先使用普通 JVM 部署验证，必要时补充反射配置 |

Maven 中建议统一 Java 版本：

```xml
<properties>
    <!-- Spring Boot 3 推荐使用 JDK 17 或更高版本 -->
    <java.version>17</java.version>
    <maven.compiler.release>17</maven.compiler.release>
</properties>
```

编译插件建议明确 release 版本：

```xml
<build>
    <plugins>
        <!-- 指定 Java 编译版本，避免本地和流水线 JDK 不一致 -->
        <plugin>
            <groupId>org.apache.maven.plugins</groupId>
            <artifactId>maven-compiler-plugin</artifactId>
            <configuration>
                <release>17</release>
            </configuration>
        </plugin>
    </plugins>
</build>
```

Spring Boot 3 项目中如果出现 `javax.servlet`、`javax.validation`、`javax.annotation` 相关错误，应检查依赖是否仍然使用旧包名。常见替换方向如下：

| Spring Boot 2 常见包  | Spring Boot 3 对应方向  |
| --------------------- | ----------------------- |
| `javax.servlet.*`     | `jakarta.servlet.*`     |
| `javax.validation.*`  | `jakarta.validation.*`  |
| `javax.annotation.*`  | `jakarta.annotation.*`  |
| `javax.persistence.*` | `jakarta.persistence.*` |

排查依赖冲突可以使用 Maven 命令：

```bash
# 查看依赖树
mvn dependency:tree

# 检查是否存在 javax 相关旧依赖
mvn dependency:tree | grep "javax"

# 检查 XXL-JOB 依赖版本
mvn dependency:tree | grep "xxl-job"
```

`mvn dependency:tree` 用于查看完整依赖树，`grep "javax"` 用于筛查旧 Java EE 包依赖，`grep "xxl-job"` 用于确认当前项目实际引入的 XXL-JOB 客户端版本。

Spring Boot 3 接入 XXL-JOB 的最终检查清单如下：

| 检查项         | 要求                                   |
| -------------- | -------------------------------------- |
| JDK            | 使用 JDK 17 或更高版本                 |
| Spring Boot    | 使用 3.x 版本                          |
| XXL-JOB 客户端 | 与调度中心保持兼容                     |
| 配置类         | `XxlJobSpringExecutor` Bean 正常初始化 |
| AppName        | 与调度中心执行器管理一致               |
| Token          | 调度中心和执行器一致                   |
| 执行器端口     | 已监听且调度中心可访问                 |
| 日志目录       | 存在且应用用户可写                     |
| JobHandler     | 与调度中心任务配置完全一致             |
| 任务幂等       | 失败重试不会导致重复副作用             |
