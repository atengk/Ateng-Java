# Spring Cloud Alibaba Seata

本文用于说明 Spring Boot 3 项目集成 Spring Cloud Alibaba Seata 的基础设计、适用边界、事务模式选择、版本规划、Maven 依赖以及 Nacos / Seata 本地开发环境准备。当前推荐按 Spring Boot 3.5.x 体系规划时，可选 Spring Cloud Alibaba 2025.0.0.0；该版本适配 Spring Boot 3.5.0、Spring Cloud 2025.0.0，并对应 Nacos 3.0.3、Seata 2.5.0。([Spring Cloud Alibaba](https://sca.aliyun.com/docs/2025.x/overview/version-explain/))

## 项目概述

本项目以订单、库存、账户三个典型微服务为示例，演示在 Spring Boot 3 与 Spring Cloud Alibaba 微服务体系下，通过 Seata 解决跨服务、跨数据库调用过程中的数据一致性问题。Seata 官方快速开始示例也采用了类似的业务链路：库存服务扣减库存、订单服务创建订单、账户服务扣减余额。([seata.apache.org](https://seata.apache.org/docs/user/quickstart/))

### 集成目标

本章节用于明确 Seata 在项目中的接入目标，避免将分布式事务误用为所有一致性问题的默认方案。

本项目集成 Seata 的核心目标是：在一次下单请求中，将订单创建、库存扣减、账户扣款纳入同一个全局事务。当任意一个服务执行失败时，已完成的一阶段本地数据库操作能够按事务上下文自动回滚，避免出现订单已创建但库存未扣减、账户已扣款但订单失败等异常数据状态。

集成后需要达到以下效果：

| 目标             | 说明                                                         |
| ---------------- | ------------------------------------------------------------ |
| 全局事务统一入口 | 在业务编排服务或订单服务入口方法上使用 `@GlobalTransactional` 开启全局事务。 |
| 本地事务独立提交 | 各微服务仍保留本地事务边界，数据库操作由本地事务提交。       |
| 异常自动回滚     | 当 Feign 调用、业务校验、数据库写入任一环节抛出异常时，Seata 负责驱动分支事务回滚。 |
| 事务上下文传递   | 服务间调用需要正确传递 `xid`，保证下游服务加入同一个全局事务。 |
| 低侵入接入       | 优先采用 AT 模式，减少业务代码中的 Try / Confirm / Cancel 补偿逻辑。 |
| 可观测与可定位   | 通过 Seata Server 日志、客户端日志、全局事务 ID 定位提交、回滚、锁冲突等问题。 |

在代码层面，项目后续章节应围绕以下调用链展开：

```text
order-service
  ├── 创建订单
  ├── 调用 storage-service 扣减库存
  └── 调用 account-service 扣减余额
```

其中 `order-service` 可以作为全局事务入口，也可以拆分出独立的 `business-service` 作为业务编排服务。示例项目为了降低理解成本，建议直接以 `order-service` 作为事务入口。

### 适用场景

本章节用于说明哪些业务适合使用 Seata，哪些业务不建议直接套用分布式事务。

Seata 适用于强一致性要求较高、调用链路相对明确、数据库写操作可以被事务代理管理的业务场景。对于典型的交易、库存、账户、订单、余额、积分、优惠券核销等场景，若多个微服务之间存在同步写入关系，并且任一环节失败都必须整体回滚，可以使用 Seata 进行全局事务控制。

适合场景如下：

| 场景         | 说明                                                   |
| ------------ | ------------------------------------------------------ |
| 下单链路     | 创建订单、扣减库存、扣减余额必须同时成功或同时失败。   |
| 支付前置校验 | 冻结账户金额、记录支付流水、锁定订单状态需要保持一致。 |
| 库存核销     | 扣减库存与写入库存流水必须保持一致。                   |
| 优惠券使用   | 优惠券状态更新、订单优惠金额写入需要统一回滚。         |
| 账户积分     | 账户余额、积分流水、业务单据需要同步写入。             |

不建议直接使用 Seata 的场景如下：

| 场景           | 原因                                       | 建议方案                                           |
| -------------- | ------------------------------------------ | -------------------------------------------------- |
| 超长业务流程   | 长时间持有全局事务会增加锁冲突与回滚成本。 | 使用 Saga、MQ 最终一致性或状态机。                 |
| 高并发热点扣减 | AT 模式依赖全局锁，热点行容易竞争。        | 使用库存预占、分段库存、Redis 原子扣减后异步落库。 |
| 外部三方接口   | 三方接口通常无法参与本地数据库事务。       | 使用 TCC、Saga 或可靠消息补偿。                    |
| 非 JDBC 数据源 | AT 模式主要面向 JDBC 关系型数据库。        | 根据资源类型选择 TCC、Saga 或最终一致性。          |
| 纯查询链路     | 无写操作时没有必要引入分布式事务。         | 保持普通服务调用即可。                             |

### 分布式事务模式选择

本章节用于确定项目采用哪种 Seata 事务模式，并说明选择依据。

Seata 提供 AT、TCC、Saga、XA 等事务模式；官方说明中，Seata 定位为分布式事务解决方案，并提供 AT、TCC、Saga、XA 模式。AT 模式要求使用支持本地 ACID 事务的关系型数据库，并通过 JDBC 访问数据库；一阶段会在同一个本地事务中提交业务数据和回滚日志，二阶段提交时异步清理回滚日志，二阶段回滚时根据一阶段生成的回滚日志执行补偿。([seata.apache.org](https://seata.apache.org/docs/overview/what-is-seata/))

本项目建议优先采用 AT 模式。

| 模式 | 适用场景                                                     | 优点                                 | 成本与限制                                                   |
| ---- | ------------------------------------------------------------ | ------------------------------------ | ------------------------------------------------------------ |
| AT   | Spring Boot 微服务 + MySQL / PostgreSQL 等关系型数据库写操作 | 业务侵入低，适合快速接入，开发成本低 | 依赖 undo_log、全局锁、SQL 支持范围；热点数据可能出现锁竞争  |
| TCC  | 支付、冻结、解冻、确认类业务                                 | 业务可控性强，适合核心交易链路       | 需要开发 Try / Confirm / Cancel 三套接口，幂等、空回滚、防悬挂复杂 |
| Saga | 长流程、多步骤、跨系统流程                                   | 适合长事务，锁占用少，吞吐较高       | 需要补偿逻辑，隔离性较弱                                     |
| XA   | 数据库原生 XA 能力较强的场景                                 | 标准化程度高，强一致性好             | 对数据库和驱动依赖强，性能开销较高                           |

当前示例项目建议如下：

| 业务模块     | 推荐模式  | 说明                                                    |
| ------------ | --------- | ------------------------------------------------------- |
| 订单创建     | AT        | 普通数据库写入，适合自动回滚。                          |
| 库存扣减     | AT        | 示例场景可使用 AT；高并发生产场景需关注热点库存锁冲突。 |
| 账户扣款     | AT / TCC  | 示例项目使用 AT；真实支付、冻结类场景优先考虑 TCC。     |
| 外部支付接口 | 不建议 AT | 外部接口无法自动回滚，应使用 TCC、Saga 或可靠消息。     |

项目初期采用 AT 模式可以降低接入复杂度，重点验证全局事务入口、Feign 调用链路、异常回滚、`undo_log` 生成与清理、Seata Server 注册发现等核心能力。后续如果账户扣款、支付确认、库存预占等业务对幂等和补偿要求更高，再单独演进为 TCC 或 Saga。

## 环境与依赖

本章节用于统一项目运行所需的 JDK、Spring Boot、Spring Cloud、Spring Cloud Alibaba、Nacos、Seata、数据库与构建工具版本，避免由于版本错配导致启动失败、依赖冲突或事务上下文无法传递。

### 技术版本规划

本项目建议优先使用 Spring Boot 3.5.x 技术线。如果团队已有 Spring Boot 3.2.x 存量项目，也可以使用 2023.0.x 技术线。Spring Cloud 官方兼容矩阵显示，Spring Cloud 2025.0.x 对应 Spring Boot 3.5.x，Spring Cloud 2024.0.x 对应 Spring Boot 3.4.x，Spring Cloud 2023.0.x 对应 Spring Boot 3.3.x / 3.2.x。([Home](https://spring.io/projects/spring-cloud/))

推荐版本规划如下：

| 类型                 | 推荐版本   | 说明                                                         |
| -------------------- | ---------- | ------------------------------------------------------------ |
| JDK                  | 17+        | Spring Boot 3 基线要求使用 Jakarta EE 体系，推荐 JDK 17 或更高版本。 |
| Spring Boot          | 3.5.0      | 与 Spring Cloud Alibaba 2025.0.0.0 对齐。                    |
| Spring Cloud         | 2025.0.0   | 与 Spring Boot 3.5.x 技术线对齐。                            |
| Spring Cloud Alibaba | 2025.0.0.0 | 适配 Spring Boot 3.5.0、Spring Cloud 2025.0.0。              |
| Nacos                | 3.0.3      | Spring Cloud Alibaba 2025.0.0.0 对应组件版本。               |
| Seata                | 2.5.0      | Spring Cloud Alibaba 2025.0.0.0 对应组件版本。               |
| MySQL                | 8.0.x      | 用于业务库、Seata Server 事务日志库。                        |
| Maven                | 3.9.x      | 用于多模块 Spring Boot 项目构建。                            |

兼容版本备选如下：

| 技术线       | Spring Boot | Spring Cloud | Spring Cloud Alibaba | Nacos | Seata |
| ------------ | ----------- | ------------ | -------------------- | ----- | ----- |
| 推荐新项目   | 3.5.0       | 2025.0.0     | 2025.0.0.0           | 3.0.3 | 2.5.0 |
| 稳定存量项目 | 3.2.4       | 2023.0.1     | 2023.0.1.0           | 2.3.2 | 2.0.0 |

如果项目已经固定 Spring Boot 3.2.x，建议采用第二套版本组合。Spring Cloud Alibaba 2023.0.1.0 官方声明适配 Spring Boot 3.2.4、Spring Cloud 2023.0.1，并对应 Nacos 2.3.2、Seata 2.0.0。([Spring Cloud Alibaba](https://sca.aliyun.com/docs/2023/overview/version-explain/))

### Maven 依赖配置

本章节给出父工程 `pom.xml` 的核心依赖管理方式。Spring 官方页面建议通过引入 Spring Cloud BOM，并将 `spring-cloud-alibaba-dependencies` 加入 `dependencyManagement` 来管理 Spring Cloud Alibaba 相关依赖版本。([Home](https://spring.io/projects/spring-cloud-alibaba))

父工程依赖管理建议放在根目录 `pom.xml` 中。

```xml
<properties>
    <!-- Java 编译版本，Spring Boot 3 推荐 JDK 17+ -->
    <java.version>17</java.version>

    <!-- Spring Boot 3.5.x 技术线 -->
    <spring-boot.version>3.5.0</spring-boot.version>

    <!-- Spring Cloud 2025.0.x 技术线 -->
    <spring-cloud.version>2025.0.0</spring-cloud.version>

    <!-- Spring Cloud Alibaba 2025.0.x 技术线 -->
    <spring-cloud-alibaba.version>2025.0.0.0</spring-cloud-alibaba.version>

    <!-- 常用工具库版本 -->
    <hutool.version>5.8.38</hutool.version>
    <mybatis-plus.version>3.5.12</mybatis-plus.version>
</properties>

<dependencyManagement>
    <dependencies>
        <!-- Spring Boot 依赖版本管理 -->
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-dependencies</artifactId>
            <version>${spring-boot.version}</version>
            <type>pom</type>
            <scope>import</scope>
        </dependency>

        <!-- Spring Cloud 依赖版本管理 -->
        <dependency>
            <groupId>org.springframework.cloud</groupId>
            <artifactId>spring-cloud-dependencies</artifactId>
            <version>${spring-cloud.version}</version>
            <type>pom</type>
            <scope>import</scope>
        </dependency>

        <!-- Spring Cloud Alibaba 依赖版本管理，统一管理 Nacos、Seata 等组件 -->
        <dependency>
            <groupId>com.alibaba.cloud</groupId>
            <artifactId>spring-cloud-alibaba-dependencies</artifactId>
            <version>${spring-cloud-alibaba.version}</version>
            <type>pom</type>
            <scope>import</scope>
        </dependency>

        <!-- MyBatis-Plus 依赖版本管理 -->
        <dependency>
            <groupId>com.baomidou</groupId>
            <artifactId>mybatis-plus-bom</artifactId>
            <version>${mybatis-plus.version}</version>
            <type>pom</type>
            <scope>import</scope>
        </dependency>
    </dependencies>
</dependencyManagement>
```

业务服务模块，例如 `order-service`、`storage-service`、`account-service`，建议引入以下依赖。

```xml
<dependencies>
    <!-- Web 接口支持 -->
    <dependency>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-starter-web</artifactId>
    </dependency>

    <!-- 参数校验，Spring Boot 3 使用 jakarta.validation 体系 -->
    <dependency>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-starter-validation</artifactId>
    </dependency>

    <!-- Nacos 服务注册与发现 -->
    <dependency>
        <groupId>com.alibaba.cloud</groupId>
        <artifactId>spring-cloud-starter-alibaba-nacos-discovery</artifactId>
    </dependency>

    <!-- Nacos 配置中心，按需启用 -->
    <dependency>
        <groupId>com.alibaba.cloud</groupId>
        <artifactId>spring-cloud-starter-alibaba-nacos-config</artifactId>
    </dependency>

    <!-- OpenFeign 服务调用 -->
    <dependency>
        <groupId>org.springframework.cloud</groupId>
        <artifactId>spring-cloud-starter-openfeign</artifactId>
    </dependency>

    <!-- Seata 分布式事务 starter，由 Spring Cloud Alibaba BOM 管理版本 -->
    <dependency>
        <groupId>com.alibaba.cloud</groupId>
        <artifactId>spring-cloud-starter-alibaba-seata</artifactId>
    </dependency>

    <!-- MyBatis-Plus 数据访问 -->
    <dependency>
        <groupId>com.baomidou</groupId>
        <artifactId>mybatis-plus-spring-boot3-starter</artifactId>
    </dependency>

    <!-- MySQL 8 驱动 -->
    <dependency>
        <groupId>com.mysql</groupId>
        <artifactId>mysql-connector-j</artifactId>
        <scope>runtime</scope>
    </dependency>

    <!-- Hutool 工具类，便于参数处理、集合处理、字符串处理等 -->
    <dependency>
        <groupId>cn.hutool</groupId>
        <artifactId>hutool-all</artifactId>
        <version>${hutool.version}</version>
    </dependency>

    <!-- Lombok，减少实体类、DTO、日志对象样板代码 -->
    <dependency>
        <groupId>org.projectlombok</groupId>
        <artifactId>lombok</artifactId>
        <optional>true</optional>
    </dependency>

    <!-- 测试依赖 -->
    <dependency>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-starter-test</artifactId>
        <scope>test</scope>
    </dependency>
</dependencies>
```

如果使用 Spring Boot 3.2.4 技术线，只需要调整版本属性：

```xml
<properties>
    <!-- Spring Boot 3.2.x 稳定技术线 -->
    <spring-boot.version>3.2.4</spring-boot.version>

    <!-- Spring Cloud 2023.0.x 技术线 -->
    <spring-cloud.version>2023.0.1</spring-cloud.version>

    <!-- Spring Cloud Alibaba 2023.0.x 技术线 -->
    <spring-cloud-alibaba.version>2023.0.1.0</spring-cloud-alibaba.version>
</properties>
```

### Nacos 与 Seata 环境准备

本章节用于准备本地开发环境中的注册中心、配置中心、Seata Server 和数据库。Nacos 官方 Docker 快速开始说明中，单机模式主要用于快速上手和测试；生产环境建议使用集群模式并开启鉴权，且不建议将 Nacos 暴露在公网环境。([Nacos 官网](https://nacos.io/docs/v3.1/quickstart/quick-start-docker/))

本地开发建议准备以下组件：

| 组件            | 用途                            | 默认端口           |
| --------------- | ------------------------------- | ------------------ |
| MySQL           | 业务库、Seata Server 事务日志库 | 3306               |
| Nacos           | 服务注册、配置管理              | 8080 / 8848 / 9848 |
| Seata Server    | 全局事务协调器 TC               | 8091 / 7091        |
| order-service   | 订单服务 / 全局事务入口         | 18081              |
| storage-service | 库存服务                        | 18082              |
| account-service | 账户服务                        | 18083              |

本地开发环境可使用 Docker Compose 统一启动。以下示例固定镜像版本，避免直接使用 `latest` 导致环境不可重复。

文件位置：`docker-compose.yml`

```yaml
services:
  mysql:
    image: mysql:8.0
    container_name: seata-mysql
    restart: always
    environment:
      # MySQL root 密码，仅用于本地开发
      MYSQL_ROOT_PASSWORD: root
      # 默认创建 Seata Server 事务日志库
      MYSQL_DATABASE: seata
      TZ: Asia/Shanghai
    ports:
      - "3306:3306"
    volumes:
      # 初始化脚本目录，后续放置业务库和 Seata Server 表结构
      - ./docker/mysql/init:/docker-entrypoint-initdb.d
      # MySQL 数据持久化
      - ./docker/mysql/data:/var/lib/mysql
    command:
      # 使用 utf8mb4，避免中文和表情字符写入异常
      - --character-set-server=utf8mb4
      - --collation-server=utf8mb4_unicode_ci

  nacos:
    image: nacos/nacos-server:v3.0.3
    container_name: seata-nacos
    restart: always
    depends_on:
      - mysql
    environment:
      # 单机模式，仅用于本地开发
      MODE: standalone
      # Nacos 3.x 鉴权相关配置，本地也建议显式配置
      NACOS_AUTH_ENABLE: "true"
      NACOS_AUTH_TOKEN: "VGhpcy1pcy1hLWxvY2FsLWRldi1uYWNvcy1hdXRoLXRva2VuLWZvci1zZWF0YQ=="
      NACOS_AUTH_IDENTITY_KEY: "serverIdentity"
      NACOS_AUTH_IDENTITY_VALUE: "security"
    ports:
      - "8080:8080"
      - "8848:8848"
      - "9848:9848"

  seata-server:
    image: apache/seata-server:2.5.0
    container_name: seata-server
    restart: always
    depends_on:
      - nacos
      - mysql
    environment:
      # Seata Server 对外暴露端口
      SEATA_PORT: 8091
      # 使用自定义配置文件，后续章节可扩展 registry / config / store 配置
      SEATA_CONFIG_NAME: file:/root/seata-config/application
    ports:
      - "8091:8091"
      - "7091:7091"
    volumes:
      # 挂载 Seata Server 自定义配置
      - ./docker/seata/config:/root/seata-config
```

启动本地基础环境：

```bash
docker compose up -d mysql nacos seata-server
docker compose ps
```

命令说明：

`docker compose up -d` 用于后台启动基础组件；`docker compose ps` 用于确认容器状态。Nacos 控制台默认可访问 `http://127.0.0.1:8080/index.html`，Seata Server 控制台端口通常为 `7091`，事务通信端口为 `8091`。Seata Docker 镜像说明中也列出了 `8091` 和 `7091` 端口，以及 `SEATA_PORT`、`STORE_MODE`、`SEATA_CONFIG_NAME` 等常用环境变量。([Docker Hub](https://hub.docker.com/r/apache/seata-server))

为了让 Seata AT 模式正常工作，每个参与分布式事务的业务库都需要创建 `undo_log` 表。Seata 官方快速开始说明中也明确指出，AT 模式需要 `UNDO_LOG` 表。([seata.apache.org](https://seata.apache.org/docs/user/quickstart/?utm_source=chatgpt.com))

文件位置：`docker/mysql/init/02-undo-log.sql`

```sql
-- 订单库
CREATE DATABASE IF NOT EXISTS seata_order DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;

-- 库存库
CREATE DATABASE IF NOT EXISTS seata_storage DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;

-- 账户库
CREATE DATABASE IF NOT EXISTS seata_account DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;

-- Seata Server 事务日志库
CREATE DATABASE IF NOT EXISTS seata DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;

USE seata_order;

CREATE TABLE IF NOT EXISTS undo_log
(
    id            BIGINT       NOT NULL AUTO_INCREMENT COMMENT '主键',
    branch_id     BIGINT       NOT NULL COMMENT '分支事务ID',
    xid           VARCHAR(100) NOT NULL COMMENT '全局事务ID',
    context       VARCHAR(128) NOT NULL COMMENT '上下文',
    rollback_info LONGBLOB     NOT NULL COMMENT '回滚信息',
    log_status    INT          NOT NULL COMMENT '状态，0正常，1防悬挂',
    log_created   DATETIME     NOT NULL COMMENT '创建时间',
    log_modified  DATETIME     NOT NULL COMMENT '修改时间',
    PRIMARY KEY (id),
    UNIQUE KEY ux_undo_log (xid, branch_id)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4 COMMENT ='Seata AT 模式回滚日志表';

USE seata_storage;

CREATE TABLE IF NOT EXISTS undo_log LIKE seata_order.undo_log;

USE seata_account;

CREATE TABLE IF NOT EXISTS undo_log LIKE seata_order.undo_log;
```

Seata 客户端与服务端后续需要重点对齐以下配置项：

| 配置项                                   | 说明                                                         |
| ---------------------------------------- | ------------------------------------------------------------ |
| `registry.type`                          | Seata Server 和客户端使用的注册中心类型，本项目使用 `nacos`。 |
| `config.type`                            | Seata 配置中心类型，本项目使用 `nacos` 或本地文件。          |
| `seata.tx-service-group`                 | 客户端事务分组名称，例如 `default_tx_group`。                |
| `service.vgroupMapping.default_tx_group` | 事务分组到 TC 集群名称的映射。                               |
| `store.mode`                             | Seata Server 事务会话存储模式，本地可用 `file`，生产建议 `db` 或高可用方案。 |
| `store.db.*`                             | 当 `store.mode=db` 时配置 Seata Server 事务日志库连接信息。  |

Seata 参数文档中说明，`registry.type`、`config.type` 是服务端和客户端都需要关注的配置；`store.mode=db` 时需要配置数据库驱动、URL、用户名、密码；`service.vgroupMapping.*` 用于把事务分组映射到 TC 集群。([seata.apache.org](https://seata.apache.org/docs/user/configurations/))

本地环境验证建议按以下顺序执行：

```bash
# 查看 Nacos 日志
docker logs -f seata-nacos

# 查看 Seata Server 日志
docker logs -f seata-server

# 查看 MySQL 是否可连接
docker exec -it seata-mysql mysql -uroot -proot -e "SHOW DATABASES;"
```

完成以上准备后，后续章节可以继续展开 `Seata 服务端配置`、`Spring Boot 3 项目接入`、`数据源代理配置`、`全局事务入口` 和 `回滚场景设计`。



## Seata 服务端配置

本章节用于配置 Seata Server，也就是 Seata 体系中的 TC 事务协调器。Seata 官方部署说明中，TC 是独立部署的服务端，业务系统集成的是 TM 和 RM 客户端角色；当使用 DB 存储模式时，Server 端需要创建 `global_table`、`branch_table`、`lock_table` 等表来保存全局事务、分支事务和全局锁信息。([seata.incubator.apache.org](https://seata.incubator.apache.org/zh-cn/docs/v2.0/ops/deploy-guide-beginner/?utm_source=chatgpt.com))

### Seata Server 部署方式

本项目本地开发环境建议使用 Docker Compose 部署 Seata Server，生产环境建议使用多节点 Seata Server + Nacos 注册中心 + DB 存储模式。Seata 官方 Docker Compose 部署文档明确建议不要直接拉取 `latest` 镜像，并且 Seata Server 1.5.0 之后配置文件已经调整为 `application.yml`。([seata.io](https://www.seata.io/docs/v2.5/ops/deploy-by-docker-compose?utm_source=chatgpt.com))

本地开发可以采用单节点部署，核心端口如下：

| 端口   | 用途                  |
| ------ | --------------------- |
| `7091` | Seata 控制台端口      |
| `8091` | Seata TC 事务通信端口 |

生产环境建议至少部署两个 Seata Server 实例，并保证这些实例使用相同的注册中心和相同的事务日志存储库。Seata 高可用部署依赖注册中心、配置中心和数据库；Server 端需要注册到 Nacos，并将事务数据保存到数据库中。([seata.apache.org](https://seata.apache.org/zh-cn/docs/v2.5/ops/deploy-ha/?utm_source=chatgpt.com))

文件位置：`docker-compose.yml`

```yaml
services:
  seata-server:
    image: apache/seata-server:2.5.0
    container_name: seata-server
    restart: always
    depends_on:
      - nacos
      - mysql
    environment:
      # Seata TC 事务通信端口
      SEATA_PORT: 8091

      # 指定自定义 application.yml 配置文件路径，不需要写 .yml 后缀
      SEATA_CONFIG_NAME: file:/root/seata-config/application
    ports:
      # Seata 控制台端口
      - "7091:7091"

      # Seata TC 通信端口
      - "8091:8091"
    volumes:
      # 挂载 Seata Server 配置文件
      - ./docker/seata/config/application.yml:/root/seata-config/application.yml
```

启动 Seata Server：

```bash
# 启动 Seata Server
docker compose up -d seata-server

# 查看容器状态
docker compose ps seata-server

# 查看 Seata Server 日志
docker logs -f seata-server
```

命令说明：

`docker compose up -d seata-server` 用于后台启动 Seata Server；`docker logs -f seata-server` 用于观察 TC 是否成功连接 Nacos、是否完成服务注册、是否成功连接事务日志库。

### 注册中心配置

本章节用于配置 Seata Server 注册到 Nacos，使业务服务可以通过注册中心发现 TC 节点。Seata 官方高可用配置示例中，`seata.registry.type` 可以设置为 `nacos`，并通过 `application`、`server-addr`、`group`、`cluster`、`username`、`password` 等配置完成服务注册。([seata.apache.org](https://seata.apache.org/zh-cn/docs/v2.5/ops/deploy-ha/?utm_source=chatgpt.com))

文件位置：`docker/seata/config/application.yml`

```yaml
server:
  # Seata 控制台端口
  port: 7091

spring:
  application:
    # Seata Server 注册到 Nacos 的服务名
    name: seata-server

console:
  user:
    # Seata 控制台登录账号
    username: seata

    # Seata 控制台登录密码，本地开发可使用默认值，生产环境必须修改
    password: seata

seata:
  registry:
    # 使用 Nacos 作为 Seata Server 注册中心
    type: nacos
    nacos:
      # Seata Server 注册到 Nacos 的应用名，客户端需要通过该名称发现 TC
      application: seata-server

      # Nacos 服务地址，Docker Compose 内部使用容器名访问
      server-addr: nacos:8848

      # Nacos 命名空间，本地开发可为空；生产建议按环境隔离
      namespace:

      # Nacos 分组
      group: SEATA_GROUP

      # Seata TC 集群名称，需要与客户端事务分组映射结果一致
      cluster: default

      # Nacos 鉴权账号
      username: nacos

      # Nacos 鉴权密码
      password: nacos
```

关键配置说明：

| 配置项                             | 说明                                                         |
| ---------------------------------- | ------------------------------------------------------------ |
| `seata.registry.type`              | 注册中心类型，本项目使用 `nacos`。                           |
| `seata.registry.nacos.application` | Seata Server 注册到 Nacos 的服务名，客户端通过该服务名发现 TC。 |
| `seata.registry.nacos.group`       | Nacos 注册分组，建议与配置中心分组保持一致。                 |
| `seata.registry.nacos.cluster`     | TC 集群名称，后续 `service.vgroupMapping.default_tx_group=default` 中的 `default` 就是该值。 |
| `seata.registry.nacos.namespace`   | 环境隔离字段，开发、测试、生产建议使用不同命名空间。         |

注册中心配置完成后，在 Nacos 控制台的服务列表中应能看到 `seata-server` 服务实例。如果看不到，需要优先检查 Nacos 地址、账号密码、命名空间、分组和 Docker 网络连通性。

### 配置中心配置

本章节用于配置 Seata Server 从 Nacos 读取远程配置。Seata 官方文档中，`seata.config.type` 支持 `nacos`，并且可以通过 `data-id` 指定远程配置文件，例如 `seataServer.properties`。([seata.apache.org](https://seata.apache.org/zh-cn/docs/v2.5/ops/deploy-ha/?utm_source=chatgpt.com))

继续在 Seata Server 的 `application.yml` 中追加配置中心配置。

文件位置：`docker/seata/config/application.yml`

```yaml
seata:
  config:
    # 使用 Nacos 作为 Seata 配置中心
    type: nacos
    nacos:
      # Nacos 配置中心地址
      server-addr: nacos:8848

      # Nacos 命名空间，本地开发可为空；生产建议按环境隔离
      namespace:

      # Seata 配置所在分组
      group: SEATA_GROUP

      # Seata Server 远程配置文件 Data ID
      data-id: seataServer.properties

      # Nacos 鉴权账号
      username: nacos

      # Nacos 鉴权密码
      password: nacos
```

需要在 Nacos 中新增配置：

| 配置项     | 值                       |
| ---------- | ------------------------ |
| `Data ID`  | `seataServer.properties` |
| `Group`    | `SEATA_GROUP`            |
| `配置格式` | `Properties`             |

文件位置：Nacos 配置中心 `seataServer.properties`

```properties
# 事务分组到 TC 集群的映射，default_tx_group 映射到 default 集群
service.vgroupMapping.default_tx_group=default

# Seata Server 存储模式，本地可用 file，生产建议使用 db
store.mode=db

# 数据源类型
store.db.datasource=druid

# 数据库类型
store.db.dbType=mysql

# MySQL 8 驱动
store.db.driverClassName=com.mysql.cj.jdbc.Driver

# Seata Server 事务日志库连接地址
store.db.url=jdbc:mysql://mysql:3306/seata?useUnicode=true&characterEncoding=utf8&useSSL=false&serverTimezone=Asia/Shanghai&rewriteBatchedStatements=true

# Seata Server 事务日志库账号
store.db.user=root

# Seata Server 事务日志库密码
store.db.password=root

# 初始连接数
store.db.minConn=1

# 最大连接数
store.db.maxConn=20

# 获取连接最大等待时间，单位毫秒
store.db.maxWait=5000

# 全局事务表名
store.db.globalTable=global_table

# 分支事务表名
store.db.branchTable=branch_table

# 全局锁表名
store.db.lockTable=lock_table

# Server 端分布式锁表名，多 Seata Server 节点下使用
store.db.distributedLockTable=distributed_lock

# 单次查询全局事务最大数量
store.db.queryLimit=100

# undo_log 保留天数
server.undo.logSaveDays=7

# undo_log 清理线程执行间隔，单位毫秒
server.undo.logDeletePeriod=86400000
```

Seata 参数文档中说明，`store.mode=db` 时需要配置 `store.db.driverClassName`、`store.db.url`、`store.db.user`、`store.db.password` 等属性；DB 模式默认使用 `global_table`、`branch_table`、`lock_table`，并且新版本包含 `distributed_lock` 表配置。([seata.apache.org](https://seata.apache.org/zh-cn/docs/user/configurations?utm_source=chatgpt.com))

### 事务日志存储配置

本章节用于创建 Seata Server 事务日志表。业务服务中的 `undo_log` 表用于 AT 模式回滚业务数据；Seata Server 的 `global_table`、`branch_table`、`lock_table`、`distributed_lock` 表用于 TC 保存全局事务会话、分支事务、全局锁和 Server 端分布式锁。

文件位置：`docker/mysql/init/01-seata-server.sql`

```sql
-- 创建 Seata Server 事务日志库
CREATE DATABASE IF NOT EXISTS seata
    DEFAULT CHARACTER SET utf8mb4
    COLLATE utf8mb4_unicode_ci;

USE seata;

-- 全局事务表，用于保存 GlobalSession
CREATE TABLE IF NOT EXISTS global_table
(
    xid                       VARCHAR(128) NOT NULL COMMENT '全局事务ID',
    transaction_id            BIGINT COMMENT '事务ID',
    status                    TINYINT      NOT NULL COMMENT '事务状态',
    application_id            VARCHAR(32) COMMENT '应用ID',
    transaction_service_group VARCHAR(32) COMMENT '事务分组',
    transaction_name          VARCHAR(128) COMMENT '事务名称',
    timeout                   INT COMMENT '超时时间',
    begin_time                BIGINT COMMENT '开始时间',
    application_data          VARCHAR(2000) COMMENT '应用扩展数据',
    gmt_create                DATETIME COMMENT '创建时间',
    gmt_modified              DATETIME COMMENT '修改时间',
    PRIMARY KEY (xid),
    KEY idx_status_gmt_modified (status, gmt_modified),
    KEY idx_transaction_id (transaction_id)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4 COMMENT ='Seata 全局事务表';

-- 分支事务表，用于保存 BranchSession
CREATE TABLE IF NOT EXISTS branch_table
(
    branch_id         BIGINT       NOT NULL COMMENT '分支事务ID',
    xid               VARCHAR(128) NOT NULL COMMENT '全局事务ID',
    transaction_id    BIGINT COMMENT '事务ID',
    resource_group_id VARCHAR(32) COMMENT '资源分组ID',
    resource_id       VARCHAR(256) COMMENT '资源ID',
    branch_type       VARCHAR(8) COMMENT '分支事务模式',
    status            TINYINT COMMENT '分支事务状态',
    client_id         VARCHAR(64) COMMENT '客户端ID',
    application_data  VARCHAR(2000) COMMENT '应用扩展数据',
    gmt_create        DATETIME(6) COMMENT '创建时间',
    gmt_modified      DATETIME(6) COMMENT '修改时间',
    PRIMARY KEY (branch_id),
    KEY idx_xid (xid)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4 COMMENT ='Seata 分支事务表';

-- 全局锁表，用于保存 AT 模式全局锁
CREATE TABLE IF NOT EXISTS lock_table
(
    row_key        VARCHAR(128) NOT NULL COMMENT '行锁唯一键',
    xid            VARCHAR(128) COMMENT '全局事务ID',
    transaction_id BIGINT COMMENT '事务ID',
    branch_id      BIGINT       NOT NULL COMMENT '分支事务ID',
    resource_id    VARCHAR(256) COMMENT '资源ID',
    table_name     VARCHAR(32) COMMENT '表名',
    pk             VARCHAR(36) COMMENT '主键值',
    status         TINYINT      NOT NULL DEFAULT 0 COMMENT '锁状态',
    gmt_create     DATETIME COMMENT '创建时间',
    gmt_modified   DATETIME COMMENT '修改时间',
    PRIMARY KEY (row_key),
    KEY idx_status (status),
    KEY idx_branch_id (branch_id),
    KEY idx_xid (xid)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4 COMMENT ='Seata 全局锁表';

-- Server 端分布式锁表，多 Seata Server 节点下用于协调提交和回滚处理
CREATE TABLE IF NOT EXISTS distributed_lock
(
    lock_key   CHAR(20) NOT NULL COMMENT '锁键',
    lock_value VARCHAR(20) NOT NULL COMMENT '锁值',
    expire     BIGINT COMMENT '过期时间',
    PRIMARY KEY (lock_key)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4 COMMENT ='Seata Server 分布式锁表';

-- 初始化异步提交锁
INSERT INTO distributed_lock (lock_key, lock_value, expire)
VALUES ('AsyncCommitting', ' ', 0)
ON DUPLICATE KEY UPDATE lock_value = VALUES(lock_value);

-- 初始化重试提交锁
INSERT INTO distributed_lock (lock_key, lock_value, expire)
VALUES ('RetryCommitting', ' ', 0)
ON DUPLICATE KEY UPDATE lock_value = VALUES(lock_value);

-- 初始化重试回滚锁
INSERT INTO distributed_lock (lock_key, lock_value, expire)
VALUES ('RetryRollbacking', ' ', 0)
ON DUPLICATE KEY UPDATE lock_value = VALUES(lock_value);

-- 初始化超时回滚锁
INSERT INTO distributed_lock (lock_key, lock_value, expire)
VALUES ('TxTimeoutCheck', ' ', 0)
ON DUPLICATE KEY UPDATE lock_value = VALUES(lock_value);
```

初始化完成后可以检查表结构：

```bash
# 查看 Seata Server 事务日志表
docker exec -it seata-mysql mysql -uroot -proot -e "USE seata; SHOW TABLES;"
```

预期至少包含以下表：

```text
branch_table
distributed_lock
global_table
lock_table
```

## Spring Boot 3 项目接入

本章节用于说明业务服务如何接入 Seata Client。Spring Cloud Alibaba 场景下，引入 `spring-cloud-starter-alibaba-seata` 后，可以完成 Seata 与 Spring Cloud 的集成；Seata 官方新手部署文档也说明，`spring-cloud-starter-alibaba-seata` 可用于自动完成 Spring Cloud 场景下的 xid 跨服务传递。([seata.apache.org](https://seata.apache.org/docs/v2.0/ops/deploy-guide-beginner/?utm_source=chatgpt.com))

### 应用基础配置

本章节先给出业务服务的基础配置，包括服务名、端口、Nacos 注册发现、数据库连接、MyBatis-Plus 配置。三个业务服务都遵循同一套结构，只需要调整服务名、端口和数据库名称。

订单服务配置如下。

文件位置：`order-service/src/main/resources/application.yml`

```yaml
server:
  # 订单服务端口
  port: 18081

spring:
  application:
    # 服务名，需要注册到 Nacos
    name: order-service

  cloud:
    nacos:
      discovery:
        # Nacos 注册中心地址
        server-addr: 127.0.0.1:8848

        # Nacos 命名空间，本地开发可为空
        namespace:

        # Nacos 服务分组
        group: DEFAULT_GROUP

        # Nacos 鉴权账号
        username: nacos

        # Nacos 鉴权密码
        password: nacos

  datasource:
    # MySQL 8 驱动
    driver-class-name: com.mysql.cj.jdbc.Driver

    # 订单库连接地址
    url: jdbc:mysql://127.0.0.1:3306/seata_order?useUnicode=true&characterEncoding=utf8&useSSL=false&serverTimezone=Asia/Shanghai&rewriteBatchedStatements=true

    # 数据库账号
    username: root

    # 数据库密码
    password: root

mybatis-plus:
  configuration:
    # SQL 日志，开发环境可开启，生产环境建议关闭或降低日志级别
    log-impl: org.apache.ibatis.logging.stdout.StdOutImpl

  global-config:
    db-config:
      # 主键策略，根据业务表设计调整
      id-type: assign_id

logging:
  level:
    # 业务包日志级别
    io.github.atengk: info

    # Seata 客户端日志级别，排查事务问题时可改为 debug
    io.seata: info
```

库存服务只需要调整服务名、端口和数据库：

文件位置：`storage-service/src/main/resources/application.yml`

```yaml
server:
  # 库存服务端口
  port: 18082

spring:
  application:
    # 服务名，需要注册到 Nacos
    name: storage-service

  cloud:
    nacos:
      discovery:
        # Nacos 注册中心地址
        server-addr: 127.0.0.1:8848
        namespace:
        group: DEFAULT_GROUP
        username: nacos
        password: nacos

  datasource:
    # MySQL 8 驱动
    driver-class-name: com.mysql.cj.jdbc.Driver

    # 库存库连接地址
    url: jdbc:mysql://127.0.0.1:3306/seata_storage?useUnicode=true&characterEncoding=utf8&useSSL=false&serverTimezone=Asia/Shanghai&rewriteBatchedStatements=true

    # 数据库账号
    username: root

    # 数据库密码
    password: root
```

账户服务只需要调整服务名、端口和数据库：

文件位置：`account-service/src/main/resources/application.yml`

```yaml
server:
  # 账户服务端口
  port: 18083

spring:
  application:
    # 服务名，需要注册到 Nacos
    name: account-service

  cloud:
    nacos:
      discovery:
        # Nacos 注册中心地址
        server-addr: 127.0.0.1:8848
        namespace:
        group: DEFAULT_GROUP
        username: nacos
        password: nacos

  datasource:
    # MySQL 8 驱动
    driver-class-name: com.mysql.cj.jdbc.Driver

    # 账户库连接地址
    url: jdbc:mysql://127.0.0.1:3306/seata_account?useUnicode=true&characterEncoding=utf8&useSSL=false&serverTimezone=Asia/Shanghai&rewriteBatchedStatements=true

    # 数据库账号
    username: root

    # 数据库密码
    password: root
```

### Seata 客户端配置

本章节用于配置业务服务连接 Seata Server。Seata 参数文档列出了客户端重点配置项，包括 `registry.type`、`config.type`、`service.vgroupMapping.*`、`service.disableGlobalTransaction` 等；事务分组文档说明，Spring Boot 应用通过 `seata.tx-service-group` 指定事务分组，再通过 `service.vgroupMapping.[事务分组]` 找到 TC 集群名称。([seata.apache.org](https://seata.apache.org/docs/user/configurations?utm_source=chatgpt.com))

建议三个业务服务使用同一个事务分组 `default_tx_group`，并统一映射到 Seata Server 的 `default` 集群。

文件位置：`order-service/src/main/resources/application.yml`

```yaml
seata:
  # 是否启用 Seata 自动装配
  enabled: true

  # Seata 应用 ID，建议与 spring.application.name 保持一致
  application-id: ${spring.application.name}

  # 事务分组名称，必须能在配置中心找到 service.vgroupMapping.default_tx_group
  tx-service-group: default_tx_group

  # 分布式事务模式，当前项目使用 AT 模式
  data-source-proxy-mode: AT

  registry:
    # Seata 客户端通过 Nacos 发现 Seata Server
    type: nacos
    nacos:
      # Seata Server 在 Nacos 中的服务名
      application: seata-server

      # Nacos 注册中心地址
      server-addr: 127.0.0.1:8848

      # Nacos 命名空间，需要与 Seata Server 注册配置保持一致
      namespace:

      # Nacos 分组，需要与 Seata Server 注册配置保持一致
      group: SEATA_GROUP

      # Seata TC 集群名称
      cluster: default

      # Nacos 鉴权账号
      username: nacos

      # Nacos 鉴权密码
      password: nacos

  config:
    # Seata 客户端从 Nacos 读取事务分组映射等配置
    type: nacos
    nacos:
      # Nacos 配置中心地址
      server-addr: 127.0.0.1:8848

      # Nacos 命名空间，需要与 Seata Server 配置中心保持一致
      namespace:

      # Seata 配置分组
      group: SEATA_GROUP

      # 远程配置 Data ID
      data-id: seataServer.properties

      # Nacos 鉴权账号
      username: nacos

      # Nacos 鉴权密码
      password: nacos
```

`storage-service` 和 `account-service` 也使用相同的 Seata 客户端配置，只需要保留各自不同的 `spring.application.name` 即可。

如果不希望客户端从 Nacos 配置中心读取 `service.vgroupMapping.default_tx_group`，也可以直接在每个业务服务的 `application.yml` 中写入映射配置：

```yaml
seata:
  service:
    vgroup-mapping:
      # default_tx_group 事务分组映射到 Seata Server 的 default 集群
      default_tx_group: default
```

两种写法二选一即可。推荐生产环境将 `service.vgroupMapping.default_tx_group=default` 放入 Nacos 配置中心，便于后续切换 TC 集群时不重新发布业务服务。

### 数据源代理配置

本章节用于说明 Seata AT 模式如何接管业务数据源。Seata 官方配置文档说明，使用 `seata-spring-boot-starter` 时默认开启数据源自动代理，可以通过 `seata.enableAutoDataSourceProxy=false` 关闭；若使用 `seata-all`，则需要显式使用 `@EnableAutoDataSourceProxy` 开启数据源代理。([seata.apache.org](https://seata.apache.org/docs/user/configurations?utm_source=chatgpt.com))

当前项目使用 `spring-cloud-starter-alibaba-seata`，建议保持自动代理开启。

文件位置：`order-service/src/main/resources/application.yml`

```yaml
seata:
  # 开启数据源自动代理，默认就是 true，这里显式声明便于阅读
  enable-auto-data-source-proxy: true

  # 使用 AT 模式代理数据源
  data-source-proxy-mode: AT
```

AT 模式下，每个参与分布式事务的业务库都必须创建 `undo_log` 表。订单库、库存库、账户库都需要各自拥有一张 `undo_log` 表，否则一阶段提交和二阶段回滚都会出现异常。

文件位置：`docker/mysql/init/02-undo-log.sql`

```sql
-- 订单库 undo_log
USE seata_order;

CREATE TABLE IF NOT EXISTS undo_log
(
    id            BIGINT       NOT NULL AUTO_INCREMENT COMMENT '主键',
    branch_id     BIGINT       NOT NULL COMMENT '分支事务ID',
    xid           VARCHAR(100) NOT NULL COMMENT '全局事务ID',
    context       VARCHAR(128) NOT NULL COMMENT '上下文',
    rollback_info LONGBLOB     NOT NULL COMMENT '回滚信息',
    log_status    INT          NOT NULL COMMENT '状态，0正常，1防悬挂',
    log_created   DATETIME     NOT NULL COMMENT '创建时间',
    log_modified  DATETIME     NOT NULL COMMENT '修改时间',
    PRIMARY KEY (id),
    UNIQUE KEY ux_undo_log (xid, branch_id)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4 COMMENT ='Seata AT 模式回滚日志表';

-- 库存库 undo_log
USE seata_storage;

CREATE TABLE IF NOT EXISTS undo_log LIKE seata_order.undo_log;

-- 账户库 undo_log
USE seata_account;

CREATE TABLE IF NOT EXISTS undo_log LIKE seata_order.undo_log;
```

使用自动数据源代理时需要注意以下几点：

| 注意项                      | 说明                                                         |
| --------------------------- | ------------------------------------------------------------ |
| 不要重复代理                | 使用 starter 自动代理后，不要再手动声明 `DataSourceProxy` Bean，避免数据源被重复代理。 |
| 每个业务库都要有 `undo_log` | 只在 Seata Server 库建表无效，业务库必须单独创建。           |
| 本地事务仍然需要保留        | 分支服务内部仍建议使用 `@Transactional` 控制本地事务边界。   |
| 只代理业务数据源            | 不要将 Seata Server 自身的事务日志库当成业务库代理。         |
| SQL 需符合 AT 模式限制      | 复杂 SQL、批量更新、无主键表、非确定性更新语句需要谨慎验证。 |

### 事务分组配置

本章节用于说明 `tx-service-group`、`service.vgroupMapping` 和 Seata Server `cluster` 的关系。Seata 事务分组文档说明，客户端会先读取 `seata.tx-service-group` 获取事务分组名，然后查找 `service.vgroupMapping.[事务分组名]` 获取 TC 集群名称，最后通过注册中心拉取该 TC 集群下的 Seata Server 节点。([seata.apache.org](https://seata.apache.org/zh-cn/docs/v2.4/user/txgroup/transaction-group?utm_source=chatgpt.com))

本项目推荐使用统一事务分组：

```text
default_tx_group -> default
```

对应关系如下：

| 配置位置                       | 配置项                                   | 示例值             | 说明                               |
| ------------------------------ | ---------------------------------------- | ------------------ | ---------------------------------- |
| 业务服务 `application.yml`     | `seata.tx-service-group`                 | `default_tx_group` | 客户端所属事务分组                 |
| Nacos `seataServer.properties` | `service.vgroupMapping.default_tx_group` | `default`          | 事务分组映射到 TC 集群             |
| Seata Server `application.yml` | `seata.registry.nacos.cluster`           | `default`          | Seata Server 注册到 Nacos 的集群名 |

推荐配置如下。

文件位置：业务服务 `application.yml`

```yaml
seata:
  # 当前服务加入 default_tx_group 事务分组
  tx-service-group: default_tx_group
```

文件位置：Nacos 配置中心 `seataServer.properties`

```properties
# default_tx_group 事务分组映射到 default TC 集群
service.vgroupMapping.default_tx_group=default
```

文件位置：Seata Server `application.yml`

```yaml
seata:
  registry:
    nacos:
      # Seata Server 注册时声明自身所在的 TC 集群
      cluster: default
```

事务分组排查时按以下顺序检查：

```bash
# 查看业务服务日志中是否出现 Seata 客户端初始化日志
docker logs -f order-service

# 查看 Seata Server 是否注册到 Nacos
docker logs -f seata-server

# 查看 Nacos 配置中是否存在事务分组映射
# 重点检查 Data ID、Group、namespace 是否与客户端配置一致
```

常见错误对应关系如下：

| 异常现象                                        | 常见原因                                        | 处理方式                                          |
| ----------------------------------------------- | ----------------------------------------------- | ------------------------------------------------- |
| `can not get cluster name in registry config`   | 找不到 `service.vgroupMapping.default_tx_group` | 检查 Nacos 配置中心 Data ID、Group、namespace。   |
| `no available service found in cluster default` | Seata Server 未注册成功或 cluster 不一致        | 检查 Seata Server 注册中心配置和 Nacos 服务列表。 |
| 全局事务未生效                                  | 未加 `@GlobalTransactional` 或未开启 Seata      | 检查入口方法注解、`seata.enabled`、启动日志。     |
| 下游服务未加入事务                              | xid 未传递或未引入 Seata starter                | 检查 Feign 依赖、Seata starter、请求链路日志。    |
| 回滚失败找不到 undo_log                         | 业务库未创建 `undo_log`                         | 在每个业务库执行 undo_log 初始化脚本。            |

完成以上配置后，Spring Boot 3 业务服务已经具备接入 Seata 的基础条件。后续章节可以继续展开 `业务服务设计`、`分布式事务实现` 和 `数据库设计`，重点补充 `@GlobalTransactional`、`@Transactional`、Feign 调用、订单 / 库存 / 账户业务表以及回滚测试接口。



## 业务服务设计

本章节用于定义参与分布式事务的业务服务边界。当前示例采用典型下单链路：订单服务作为全局事务入口，先创建订单，再调用库存服务扣减库存，最后调用账户服务扣减余额。Seata AT 模式适用于基于 JDBC 访问关系型数据库的场景，一阶段会在同一个本地事务中提交业务数据和回滚日志，二阶段提交时异步清理回滚日志，二阶段回滚时基于一阶段生成的回滚日志进行补偿。([seata.apache.org](https://seata.apache.org/docs/dev/mode/at-mode?utm_source=chatgpt.com))

### 订单服务设计

订单服务是当前示例项目的业务编排入口，负责接收下单请求、创建订单记录、调用库存服务、调用账户服务，并在任一环节失败时触发全局事务回滚。

订单服务主要职责如下：

| 职责         | 说明                                                    |
| ------------ | ------------------------------------------------------- |
| 接收下单请求 | 对外提供下单接口，例如 `POST /orders/create`。          |
| 创建订单记录 | 写入订单主表，初始状态可设置为 `CREATED` 或 `PENDING`。 |
| 调用库存服务 | 通过 Feign 调用 `storage-service` 扣减库存。            |
| 调用账户服务 | 通过 Feign 调用 `account-service` 扣减余额。            |
| 控制全局事务 | 在核心下单方法上使用 `@GlobalTransactional`。           |
| 记录事务日志 | 打印 `xid`、订单号、用户 ID、商品 ID 等关键日志。       |

订单服务建议目录结构如下：

```text
order-service
└── src/main/java/io/github/atengk/order
    ├── OrderApplication.java
    ├── controller
    │   └── OrderController.java
    ├── dto
    │   ├── OrderCreateRequest.java
    │   ├── StorageDeductRequest.java
    │   └── AccountDeductRequest.java
    ├── entity
    │   └── OrderInfo.java
    ├── feign
    │   ├── StorageFeignClient.java
    │   └── AccountFeignClient.java
    ├── mapper
    │   └── OrderInfoMapper.java
    └── service
        ├── OrderService.java
        └── impl
            └── OrderServiceImpl.java
```

订单请求对象用于承载下单接口参数，后续会被订单服务转换为库存扣减请求和账户扣款请求。

文件位置：`order-service/src/main/java/io/github/atengk/order/dto/OrderCreateRequest.java`

```java
package io.github.atengk.order.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.math.BigDecimal;

/**
 * 订单创建请求参数
 *
 * @author Ateng
 * @since 2026-05-05
 */
@Data
public class OrderCreateRequest {

    @NotNull(message = "用户ID不能为空")
    private Long userId;

    @NotNull(message = "商品ID不能为空")
    private Long productId;

    @NotNull(message = "购买数量不能为空")
    @Min(value = 1, message = "购买数量必须大于0")
    private Integer count;

    @NotNull(message = "订单金额不能为空")
    @DecimalMin(value = "0.01", message = "订单金额必须大于0")
    private BigDecimal amount;

    /**
     * 是否模拟异常，用于验证 Seata 全局回滚
     */
    private Boolean mockException;
}
```

订单服务接口只保留下单核心方法，控制器、测试接口、全局事务入口都可以围绕该方法展开。

文件位置：`order-service/src/main/java/io/github/atengk/order/service/OrderService.java`

```java
package io.github.atengk.order.service;

import io.github.atengk.order.dto.OrderCreateRequest;

public interface OrderService {

    Long createOrder(OrderCreateRequest request);

}
```

订单控制器用于对外暴露下单接口。这里保持控制器轻量，只做参数校验和服务调用，不在控制器中直接编排事务。

文件位置：`order-service/src/main/java/io/github/atengk/order/controller/OrderController.java`

```java
package io.github.atengk.order.controller;

import io.github.atengk.order.dto.OrderCreateRequest;
import io.github.atengk.order.service.OrderService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

/**
 * 订单接口
 *
 * @author Ateng
 * @since 2026-05-05
 */
@RestController
@RequestMapping("/orders")
@RequiredArgsConstructor
public class OrderController {

    private final OrderService orderService;

    @PostMapping("/create")
    public Long createOrder(@Valid @RequestBody OrderCreateRequest request) {
        return orderService.createOrder(request);
    }

}
```

### 库存服务设计

库存服务作为下游 RM 服务，负责扣减商品库存。它不负责开启全局事务，只需要在本地业务方法中使用 `@Transactional` 管理本地数据库写操作，并确保业务库存在 `undo_log` 表。

库存服务主要职责如下：

| 职责             | 说明                                   |
| ---------------- | -------------------------------------- |
| 接收扣减库存请求 | 对外提供 `POST /storage/deduct` 接口。 |
| 校验库存是否充足 | 根据商品 ID 查询库存记录。             |
| 扣减库存         | 执行库存扣减 SQL。                     |
| 记录库存流水     | 可选，生产业务中建议记录库存变更流水。 |
| 加入全局事务     | 通过上游传递的 XID 加入当前全局事务。  |

库存扣减请求对象如下：

文件位置：`storage-service/src/main/java/io/github/atengk/storage/dto/StorageDeductRequest.java`

```java
package io.github.atengk.storage.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

/**
 * 库存扣减请求参数
 *
 * @author Ateng
 * @since 2026-05-05
 */
@Data
public class StorageDeductRequest {

    @NotNull(message = "商品ID不能为空")
    private Long productId;

    @NotNull(message = "扣减数量不能为空")
    @Min(value = 1, message = "扣减数量必须大于0")
    private Integer count;

}
```

库存服务核心方法需要在本地事务中完成库存校验和扣减。AT 模式下一阶段会把业务数据更新和 `undo_log` 写入放在同一个本地事务中，因此库存扣减方法必须由 Seata 代理数据源执行。([seata.apache.org](https://seata.apache.org/docs/dev/mode/at-mode?utm_source=chatgpt.com))

文件位置：`storage-service/src/main/java/io/github/atengk/storage/service/impl/StorageServiceImpl.java`

```java
package io.github.atengk.storage.service.impl;

import cn.hutool.core.util.ObjectUtil;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import io.github.atengk.storage.dto.StorageDeductRequest;
import io.github.atengk.storage.entity.StorageInfo;
import io.github.atengk.storage.mapper.StorageInfoMapper;
import io.github.atengk.storage.service.StorageService;
import io.seata.core.context.RootContext;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 库存服务实现
 *
 * @author Ateng
 * @since 2026-05-05
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class StorageServiceImpl implements StorageService {

    private final StorageInfoMapper storageInfoMapper;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deduct(StorageDeductRequest request) {
        String xid = RootContext.getXID();
        log.info("开始扣减库存，xid={}，productId={}，count={}", xid, request.getProductId(), request.getCount());

        StorageInfo storageInfo = storageInfoMapper.selectById(request.getProductId());
        if (ObjectUtil.isNull(storageInfo)) {
            throw new IllegalArgumentException("商品库存不存在");
        }

        if (storageInfo.getResidue() < request.getCount()) {
            throw new IllegalArgumentException("商品库存不足");
        }

        LambdaUpdateWrapper<StorageInfo> updateWrapper = new LambdaUpdateWrapper<StorageInfo>()
                .eq(StorageInfo::getProductId, request.getProductId())
                .ge(StorageInfo::getResidue, request.getCount())
                .setSql("used = used + " + request.getCount())
                .setSql("residue = residue - " + request.getCount());

        int rows = storageInfoMapper.update(null, updateWrapper);
        if (rows <= 0) {
            throw new IllegalStateException("库存扣减失败，请稍后重试");
        }

        log.info("库存扣减完成，xid={}，productId={}，count={}", xid, request.getProductId(), request.getCount());
    }

}
```

### 账户服务设计

账户服务作为另一个下游 RM 服务，负责扣减用户余额。账户服务同样不主动开启全局事务，而是通过上游传递的 XID 加入订单服务开启的全局事务。

账户服务主要职责如下：

| 职责             | 说明                                           |
| ---------------- | ---------------------------------------------- |
| 接收账户扣款请求 | 对外提供 `POST /accounts/debit` 接口。         |
| 校验账户是否存在 | 根据用户 ID 查询账户记录。                     |
| 校验余额是否充足 | 判断可用余额是否大于等于订单金额。             |
| 扣减账户余额     | 执行余额扣减 SQL。                             |
| 模拟异常回滚     | 可通过请求参数触发异常，用于验证全局事务回滚。 |

账户扣款请求对象如下：

文件位置：`account-service/src/main/java/io/github/atengk/account/dto/AccountDebitRequest.java`

```java
package io.github.atengk.account.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.math.BigDecimal;

/**
 * 账户扣款请求参数
 *
 * @author Ateng
 * @since 2026-05-05
 */
@Data
public class AccountDebitRequest {

    @NotNull(message = "用户ID不能为空")
    private Long userId;

    @NotNull(message = "扣款金额不能为空")
    @DecimalMin(value = "0.01", message = "扣款金额必须大于0")
    private BigDecimal amount;

    /**
     * 是否模拟异常，用于验证全局事务回滚
     */
    private Boolean mockException;
}
```

账户服务实现中，建议先完成余额扣减，再根据 `mockException` 主动抛出异常。这样可以验证订单、库存、账户三个库的数据是否会同时回滚。

文件位置：`account-service/src/main/java/io/github/atengk/account/service/impl/AccountServiceImpl.java`

```java
package io.github.atengk.account.service.impl;

import cn.hutool.core.util.BooleanUtil;
import cn.hutool.core.util.ObjectUtil;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import io.github.atengk.account.dto.AccountDebitRequest;
import io.github.atengk.account.entity.AccountInfo;
import io.github.atengk.account.mapper.AccountInfoMapper;
import io.github.atengk.account.service.AccountService;
import io.seata.core.context.RootContext;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 账户服务实现
 *
 * @author Ateng
 * @since 2026-05-05
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AccountServiceImpl implements AccountService {

    private final AccountInfoMapper accountInfoMapper;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void debit(AccountDebitRequest request) {
        String xid = RootContext.getXID();
        log.info("开始账户扣款，xid={}，userId={}，amount={}", xid, request.getUserId(), request.getAmount());

        AccountInfo accountInfo = accountInfoMapper.selectById(request.getUserId());
        if (ObjectUtil.isNull(accountInfo)) {
            throw new IllegalArgumentException("账户不存在");
        }

        if (accountInfo.getResidue().compareTo(request.getAmount()) < 0) {
            throw new IllegalArgumentException("账户余额不足");
        }

        LambdaUpdateWrapper<AccountInfo> updateWrapper = new LambdaUpdateWrapper<AccountInfo>()
                .eq(AccountInfo::getUserId, request.getUserId())
                .ge(AccountInfo::getResidue, request.getAmount())
                .setSql("used = used + " + request.getAmount())
                .setSql("residue = residue - " + request.getAmount());

        int rows = accountInfoMapper.update(null, updateWrapper);
        if (rows <= 0) {
            throw new IllegalStateException("账户扣款失败，请稍后重试");
        }

        if (BooleanUtil.isTrue(request.getMockException())) {
            log.warn("触发账户扣款模拟异常，xid={}，userId={}", xid, request.getUserId());
            throw new IllegalStateException("模拟账户扣款异常，触发 Seata 全局回滚");
        }

        log.info("账户扣款完成，xid={}，userId={}，amount={}", xid, request.getUserId(), request.getAmount());
    }

}
```

### 服务调用链路设计

本章节用于说明一次下单请求在三个微服务之间的调用顺序，以及事务上下文如何贯穿整个链路。Seata 的事务上下文由 `RootContext` 管理，应用开启全局事务后会自动绑定 XID；事务结束后会自动解绑 XID。Seata 的事务传播本质上就是 XID 在服务内部和跨服务调用中的传播。([seata.apache.org](https://seata.apache.org/zh-cn/docs/v2.0/user/microservice?utm_source=chatgpt.com))

调用链路如下：

```text
客户端
  │
  ▼
order-service
  │  1. @GlobalTransactional 开启全局事务
  │  2. 创建订单
  │
  ├──► storage-service
  │       3. 接收 XID
  │       4. 加入全局事务
  │       5. 扣减库存
  │
  └──► account-service
          6. 接收 XID
          7. 加入全局事务
          8. 扣减余额
```

正常提交链路：

```text
create order -> deduct storage -> debit account -> global commit
```

异常回滚链路：

```text
create order -> deduct storage -> debit account -> throw exception -> global rollback
```

链路设计要点如下：

| 要点                               | 说明                                                         |
| ---------------------------------- | ------------------------------------------------------------ |
| 全局事务入口唯一                   | 一次业务请求只需要一个全局事务入口，一般放在业务编排层。     |
| 下游服务不重复开启全局事务         | 下游库存服务和账户服务只使用本地事务。                       |
| Feign 调用必须处于全局事务方法内部 | 如果 Feign 调用发生在 `@GlobalTransactional` 方法之外，下游无法加入当前全局事务。 |
| 异常必须向外抛出                   | 被捕获后吞掉的异常不会触发全局回滚。                         |
| 下游本地事务必须可回滚             | 下游方法使用 `@Transactional(rollbackFor = Exception.class)`。 |
| 每个业务库必须有 `undo_log`        | AT 模式回滚依赖业务库中的回滚日志。                          |

## 分布式事务实现

本章节用于落地全局事务入口、本地事务边界、Feign 调用和异常回滚测试。Seata AT 模式可以理解为增强版两阶段提交：一阶段提交业务数据和回滚日志，二阶段提交时异步清理回滚日志，二阶段回滚时根据回滚日志生成补偿操作。([seata.apache.org](https://seata.apache.org/docs/dev/mode/at-mode?utm_source=chatgpt.com))

### 全局事务入口

全局事务入口建议放在订单服务的核心业务编排方法上，而不是控制器上。这样可以保证事务边界贴近业务用例，并避免控制器中混入过多事务细节。

订单服务调用库存和账户服务需要使用 Feign。Spring Cloud OpenFeign 通过 `@FeignClient` 声明 HTTP 客户端，并且在负载均衡场景下，`@FeignClient` 的 `name` 对应服务注册中心中的服务名。([Home](https://docs.spring.io/spring-cloud-openfeign/reference/spring-cloud-openfeign.html?utm_source=chatgpt.com))

文件位置：`order-service/src/main/java/io/github/atengk/order/feign/StorageFeignClient.java`

```java
package io.github.atengk.order.feign;

import io.github.atengk.order.dto.StorageDeductRequest;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PostMapping;

/**
 * 库存服务 Feign 客户端
 *
 * @author Ateng
 * @since 2026-05-05
 */
@FeignClient(name = "storage-service")
public interface StorageFeignClient {

    @PostMapping("/storage/deduct")
    void deduct(StorageDeductRequest request);

}
```

文件位置：`order-service/src/main/java/io/github/atengk/order/feign/AccountFeignClient.java`

```java
package io.github.atengk.order.feign;

import io.github.atengk.order.dto.AccountDebitRequest;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PostMapping;

/**
 * 账户服务 Feign 客户端
 *
 * @author Ateng
 * @since 2026-05-05
 */
@FeignClient(name = "account-service")
public interface AccountFeignClient {

    @PostMapping("/accounts/debit")
    void debit(AccountDebitRequest request);

}
```

订单服务启动类需要开启 Feign 客户端扫描。

文件位置：`order-service/src/main/java/io/github/atengk/order/OrderApplication.java`

```java
package io.github.atengk.order;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.openfeign.EnableFeignClients;

/**
 * 订单服务启动类
 *
 * @author Ateng
 * @since 2026-05-05
 */
@EnableFeignClients
@MapperScan("io.github.atengk.order.mapper")
@SpringBootApplication
public class OrderApplication {

    public static void main(String[] args) {
        SpringApplication.run(OrderApplication.class, args);
    }

}
```

订单服务实现中，`@GlobalTransactional` 负责开启全局事务，`@Transactional` 负责订单服务自身数据库操作的本地事务。Seata 官方 Spring Cloud 示例也采用在入口方法上使用 `@GlobalTransactional`，当下游调用异常时触发全局回滚的方式。([seata.apache.org](https://seata.apache.org/zh-cn/blog/integrate-seata-with-spring-cloud?utm_source=chatgpt.com))

文件位置：`order-service/src/main/java/io/github/atengk/order/service/impl/OrderServiceImpl.java`

```java
package io.github.atengk.order.service.impl;

import cn.hutool.core.lang.Snowflake;
import cn.hutool.core.util.IdUtil;
import io.github.atengk.order.dto.AccountDebitRequest;
import io.github.atengk.order.dto.OrderCreateRequest;
import io.github.atengk.order.dto.StorageDeductRequest;
import io.github.atengk.order.entity.OrderInfo;
import io.github.atengk.order.feign.AccountFeignClient;
import io.github.atengk.order.feign.StorageFeignClient;
import io.github.atengk.order.mapper.OrderInfoMapper;
import io.github.atengk.order.service.OrderService;
import io.seata.core.context.RootContext;
import io.seata.spring.annotation.GlobalTransactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 订单服务实现
 *
 * @author Ateng
 * @since 2026-05-05
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class OrderServiceImpl implements OrderService {

    private final OrderInfoMapper orderInfoMapper;
    private final StorageFeignClient storageFeignClient;
    private final AccountFeignClient accountFeignClient;

    private static final Snowflake SNOWFLAKE = IdUtil.getSnowflake(1, 1);

    @Override
    @GlobalTransactional(
            name = "create-order-global-tx",
            rollbackFor = Exception.class,
            timeoutMills = 60000
    )
    @Transactional(rollbackFor = Exception.class)
    public Long createOrder(OrderCreateRequest request) {
        String xid = RootContext.getXID();
        Long orderId = SNOWFLAKE.nextId();

        log.info("开始创建订单全局事务，xid={}，orderId={}，userId={}，productId={}",
                xid, orderId, request.getUserId(), request.getProductId());

        OrderInfo orderInfo = new OrderInfo();
        orderInfo.setOrderId(orderId);
        orderInfo.setUserId(request.getUserId());
        orderInfo.setProductId(request.getProductId());
        orderInfo.setCount(request.getCount());
        orderInfo.setAmount(request.getAmount());
        orderInfo.setStatus("CREATED");

        int insertRows = orderInfoMapper.insert(orderInfo);
        if (insertRows <= 0) {
            throw new IllegalStateException("订单创建失败");
        }

        StorageDeductRequest storageRequest = new StorageDeductRequest();
        storageRequest.setProductId(request.getProductId());
        storageRequest.setCount(request.getCount());

        log.info("准备调用库存服务，xid={}，orderId={}，productId={}", xid, orderId, request.getProductId());
        storageFeignClient.deduct(storageRequest);

        AccountDebitRequest accountRequest = new AccountDebitRequest();
        accountRequest.setUserId(request.getUserId());
        accountRequest.setAmount(request.getAmount());
        accountRequest.setMockException(request.getMockException());

        log.info("准备调用账户服务，xid={}，orderId={}，userId={}", xid, orderId, request.getUserId());
        accountFeignClient.debit(accountRequest);

        orderInfo.setStatus("FINISHED");
        int updateRows = orderInfoMapper.updateById(orderInfo);
        if (updateRows <= 0) {
            throw new IllegalStateException("订单状态更新失败");
        }

        log.info("订单创建完成，xid={}，orderId={}", xid, orderId);
        return orderId;
    }

}
```

### 本地事务边界

本地事务边界用于控制每个服务内部的数据库操作。Seata 的全局事务并不替代本地事务；AT 模式要求业务数据和 `undo_log` 在同一个本地事务中提交，才能保证后续全局回滚时有可用的补偿依据。([seata.apache.org](https://seata.apache.org/docs/dev/mode/at-mode?utm_source=chatgpt.com))

建议按以下规则设置事务边界：

| 服务              | 方法          | 事务注解                                  | 说明                                   |
| ----------------- | ------------- | ----------------------------------------- | -------------------------------------- |
| `order-service`   | `createOrder` | `@GlobalTransactional` + `@Transactional` | 全局事务入口，同时管理订单库本地事务。 |
| `storage-service` | `deduct`      | `@Transactional`                          | 加入全局事务，只管理库存库本地事务。   |
| `account-service` | `debit`       | `@Transactional`                          | 加入全局事务，只管理账户库本地事务。   |

库存接口控制器如下：

文件位置：`storage-service/src/main/java/io/github/atengk/storage/controller/StorageController.java`

```java
package io.github.atengk.storage.controller;

import io.github.atengk.storage.dto.StorageDeductRequest;
import io.github.atengk.storage.service.StorageService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

/**
 * 库存接口
 *
 * @author Ateng
 * @since 2026-05-05
 */
@RestController
@RequestMapping("/storage")
@RequiredArgsConstructor
public class StorageController {

    private final StorageService storageService;

    @PostMapping("/deduct")
    public void deduct(@Valid @RequestBody StorageDeductRequest request) {
        storageService.deduct(request);
    }

}
```

账户接口控制器如下：

文件位置：`account-service/src/main/java/io/github/atengk/account/controller/AccountController.java`

```java
package io.github.atengk.account.controller;

import io.github.atengk.account.dto.AccountDebitRequest;
import io.github.atengk.account.service.AccountService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

/**
 * 账户接口
 *
 * @author Ateng
 * @since 2026-05-05
 */
@RestController
@RequestMapping("/accounts")
@RequiredArgsConstructor
public class AccountController {

    private final AccountService accountService;

    @PostMapping("/debit")
    public void debit(@Valid @RequestBody AccountDebitRequest request) {
        accountService.debit(request);
    }

}
```

本地事务使用注意事项：

| 注意项                               | 说明                                                         |
| ------------------------------------ | ------------------------------------------------------------ |
| 使用 `rollbackFor = Exception.class` | 避免受默认只回滚运行时异常的限制。                           |
| 不要吞掉异常                         | 如果捕获异常后不重新抛出，全局事务可能被提交。               |
| 不要异步执行核心数据库写操作         | `RootContext` 默认基于线程上下文，异步线程需要额外处理事务上下文。 |
| 不要绕过代理对象调用事务方法         | 同类内部直接调用可能导致 Spring 事务代理不生效。             |
| 保证表有主键                         | AT 模式生成前后镜像和回滚 SQL 时依赖主键定位数据。           |

### Feign 调用事务传递

Feign 调用事务传递的核心是 XID 传递。Seata 文档说明，Seata 的事务上下文由 `RootContext` 管理，应用可以通过 `RootContext.getXID()` 获取当前全局事务 XID；跨服务事务传播本质上就是把当前请求方的 XID 传递到被调用方，并在被调用方绑定到运行时上下文中。([seata.apache.org](https://seata.apache.org/zh-cn/docs/v2.0/user/microservice?utm_source=chatgpt.com))

在 Spring Cloud Alibaba Seata 场景中，通常只需要引入 `spring-cloud-starter-alibaba-seata` 和 `spring-cloud-starter-openfeign`，并保证 Feign 调用发生在 `@GlobalTransactional` 方法内部。为了方便定位问题，建议在上下游服务中打印当前 XID。

订单服务调用前打印 XID：

```java
String xid = RootContext.getXID();
log.info("调用库存服务前检查事务上下文，xid={}", xid);
storageFeignClient.deduct(storageRequest);
```

库存服务接收请求后打印 XID：

```java
String xid = RootContext.getXID();
log.info("库存服务接收到事务上下文，xid={}", xid);
```

账户服务接收请求后打印 XID：

```java
String xid = RootContext.getXID();
log.info("账户服务接收到事务上下文，xid={}", xid);
```

如果需要显式观察请求头，可以增加一个请求日志拦截器。该拦截器只用于开发排查，不建议在生产环境打印过多请求头。

文件位置：`storage-service/src/main/java/io/github/atengk/storage/config/SeataRequestLogFilter.java`

```java
package io.github.atengk.storage.config;

import io.seata.core.context.RootContext;
import jakarta.servlet.*;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.io.IOException;

/**
 * Seata 请求日志过滤器
 *
 * @author Ateng
 * @since 2026-05-05
 */
@Slf4j
@Component
public class SeataRequestLogFilter implements Filter {

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
            throws IOException, ServletException {
        HttpServletRequest httpServletRequest = (HttpServletRequest) request;
        String xid = RootContext.getXID();

        log.info("接收到服务请求，uri={}，xid={}", httpServletRequest.getRequestURI(), xid);
        chain.doFilter(request, response);
    }

}
```

Feign 调用排查重点如下：

| 现象                              | 可能原因                                  | 处理方式                                              |
| --------------------------------- | ----------------------------------------- | ----------------------------------------------------- |
| 下游 `RootContext.getXID()` 为空  | Feign 调用未发生在全局事务方法内部        | 检查 `@GlobalTransactional` 所在方法和调用顺序。      |
| 下游服务未加入全局事务            | 下游未引入 Seata starter 或数据源未被代理 | 检查依赖、启动日志、`seata.enabled`。                 |
| 上游捕获 Feign 异常后继续返回成功 | 异常被吞掉，全局事务提交                  | 捕获异常后必须重新抛出业务异常。                      |
| Feign fallback 返回默认成功       | 熔断降级掩盖真实失败                      | 分布式事务链路中谨慎使用 fallback，失败时应抛出异常。 |
| 异步调用丢失 XID                  | 事务上下文未跨线程传递                    | 避免在全局事务中异步执行核心写操作。                  |

### 回滚场景设计

回滚场景用于验证 Seata 是否真正接管了三个业务库的数据一致性。建议至少设计正常提交、库存失败、账户失败、订单后置失败四类场景。

| 场景         | 触发方式                              | 预期结果                                   |
| ------------ | ------------------------------------- | ------------------------------------------ |
| 正常提交     | `mockException=false`，库存和余额充足 | 订单创建成功，库存扣减成功，账户扣款成功。 |
| 库存不足     | 库存余额小于购买数量                  | 订单回滚，库存不变，账户不扣款。           |
| 账户余额不足 | 账户余额小于订单金额                  | 订单回滚，库存回滚，账户不扣款。           |
| 账户模拟异常 | `mockException=true`                  | 订单回滚，库存回滚，账户扣款回滚。         |
| 订单后置异常 | 账户扣款后抛出异常                    | 订单、库存、账户全部回滚。                 |

测试正常提交：

```bash
curl -X POST "http://127.0.0.1:18081/orders/create" \
  -H "Content-Type: application/json" \
  -d '{
    "userId": 1,
    "productId": 1001,
    "count": 1,
    "amount": 50.00,
    "mockException": false
  }'
```

测试账户服务异常导致全局回滚：

```bash
curl -X POST "http://127.0.0.1:18081/orders/create" \
  -H "Content-Type: application/json" \
  -d '{
    "userId": 1,
    "productId": 1001,
    "count": 1,
    "amount": 50.00,
    "mockException": true
  }'
```

验证 SQL 如下：

```sql
-- 查看订单是否回滚
SELECT * FROM seata_order.order_info ORDER BY id DESC LIMIT 10;

-- 查看库存是否回滚
SELECT * FROM seata_storage.storage_info WHERE product_id = 1001;

-- 查看账户余额是否回滚
SELECT * FROM seata_account.account_info WHERE user_id = 1;

-- 查看 undo_log 是否存在未清理记录
SELECT * FROM seata_order.undo_log;
SELECT * FROM seata_storage.undo_log;
SELECT * FROM seata_account.undo_log;
```

回滚验证时重点观察日志中的同一个 `xid`。一次请求中，订单服务、库存服务、账户服务打印出的 `xid` 应保持一致。如果三个服务的 `xid` 不一致，或下游服务 `xid` 为空，说明事务上下文没有正确传递。

建议日志关键字如下：

```text
开始创建订单全局事务
准备调用库存服务
开始扣减库存
准备调用账户服务
开始账户扣款
触发账户扣款模拟异常
Branch Rollbacked
Global Rollbacked
```

最终判断标准如下：

| 检查项            | 成功标准                                                     |
| ----------------- | ------------------------------------------------------------ |
| 订单库            | 异常场景下不应保留本次订单记录，或订单状态回滚到事务前状态。 |
| 库存库            | 异常场景下 `used`、`residue` 恢复到请求前。                  |
| 账户库            | 异常场景下 `used`、`residue` 恢复到请求前。                  |
| Seata Server 日志 | 能看到全局事务回滚记录。                                     |
| 三个服务日志      | 同一次请求的 `xid` 一致。                                    |
| `undo_log`        | 二阶段完成后通常会被清理；若残留，需要结合 Server 日志排查。 |

## 数据库设计

本章节用于定义订单、库存、账户三个业务库的表结构，以及 Seata AT 模式必须使用的 `undo_log` 表。Seata 官方说明中，AT 模式的一阶段会在同一个本地事务中提交业务数据和回滚日志；二阶段回滚时，Seata 会基于一阶段生成的回滚日志执行补偿操作。因此，每个参与 AT 分布式事务的业务库都必须创建 `undo_log` 表。([seata.io](https://www.seata.io/en-us/docs/overview/what-is-seata.html?utm_source=chatgpt.com))

### 业务表设计

业务表设计需要满足两个目标：一是能够支撑下单、扣库存、扣余额的基础业务流程；二是能够让 Seata AT 模式正确生成前镜像、后镜像和回滚 SQL。业务表必须使用 InnoDB 引擎，并且每张被 Seata 管理的业务表都应有明确主键。

当前示例使用三个独立业务库：

| 数据库          | 服务              | 说明                   |
| --------------- | ----------------- | ---------------------- |
| `seata_order`   | `order-service`   | 保存订单数据。         |
| `seata_storage` | `storage-service` | 保存商品库存数据。     |
| `seata_account` | `account-service` | 保存用户账户余额数据。 |

订单表用于记录用户下单数据，订单状态建议保留 `CREATED`、`FINISHED`、`CANCELED` 等枚举值，便于后续扩展订单状态流转。

文件位置：`docker/mysql/init/03-business-table.sql`

```sql
-- 创建订单库
CREATE DATABASE IF NOT EXISTS seata_order
    DEFAULT CHARACTER SET utf8mb4
    COLLATE utf8mb4_unicode_ci;

USE seata_order;

-- 订单表
CREATE TABLE IF NOT EXISTS order_info
(
    id          BIGINT         NOT NULL AUTO_INCREMENT COMMENT '主键ID',
    order_id    BIGINT         NOT NULL COMMENT '订单ID',
    user_id     BIGINT         NOT NULL COMMENT '用户ID',
    product_id  BIGINT         NOT NULL COMMENT '商品ID',
    count       INT            NOT NULL COMMENT '购买数量',
    amount      DECIMAL(18, 2) NOT NULL COMMENT '订单金额',
    status      VARCHAR(32)    NOT NULL COMMENT '订单状态：CREATED-已创建，FINISHED-已完成，CANCELED-已取消',
    create_time DATETIME       NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time DATETIME       NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '修改时间',
    PRIMARY KEY (id),
    UNIQUE KEY uk_order_id (order_id),
    KEY idx_user_id (user_id),
    KEY idx_product_id (product_id)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4 COMMENT ='订单表';
```

库存表用于记录商品总库存、已用库存和剩余库存。扣减库存时建议同时更新 `used` 和 `residue`，并在 SQL 条件中增加 `residue >= count`，避免并发场景下出现超卖。

```sql
-- 创建库存库
CREATE DATABASE IF NOT EXISTS seata_storage
    DEFAULT CHARACTER SET utf8mb4
    COLLATE utf8mb4_unicode_ci;

USE seata_storage;

-- 库存表
CREATE TABLE IF NOT EXISTS storage_info
(
    id          BIGINT   NOT NULL AUTO_INCREMENT COMMENT '主键ID',
    product_id  BIGINT   NOT NULL COMMENT '商品ID',
    total       INT      NOT NULL COMMENT '总库存',
    used        INT      NOT NULL DEFAULT 0 COMMENT '已用库存',
    residue     INT      NOT NULL COMMENT '剩余库存',
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '修改时间',
    PRIMARY KEY (id),
    UNIQUE KEY uk_product_id (product_id)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4 COMMENT ='库存表';
```

账户表用于记录用户账户总金额、已用金额和剩余金额。扣款时同样建议在 SQL 条件中增加 `residue >= amount`，避免并发扣款导致余额为负数。

```sql
-- 创建账户库
CREATE DATABASE IF NOT EXISTS seata_account
    DEFAULT CHARACTER SET utf8mb4
    COLLATE utf8mb4_unicode_ci;

USE seata_account;

-- 账户表
CREATE TABLE IF NOT EXISTS account_info
(
    id          BIGINT         NOT NULL AUTO_INCREMENT COMMENT '主键ID',
    user_id     BIGINT         NOT NULL COMMENT '用户ID',
    total       DECIMAL(18, 2) NOT NULL COMMENT '总金额',
    used        DECIMAL(18, 2) NOT NULL DEFAULT 0.00 COMMENT '已用金额',
    residue     DECIMAL(18, 2) NOT NULL COMMENT '剩余金额',
    create_time DATETIME       NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time DATETIME       NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '修改时间',
    PRIMARY KEY (id),
    UNIQUE KEY uk_user_id (user_id)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4 COMMENT ='账户表';
```

初始化测试数据用于验证正常提交和异常回滚。

```sql
-- 初始化库存数据
USE seata_storage;

INSERT INTO storage_info (product_id, total, used, residue)
VALUES (1001, 100, 0, 100)
ON DUPLICATE KEY UPDATE total   = VALUES(total),
                        used    = VALUES(used),
                        residue = VALUES(residue);

-- 初始化账户数据
USE seata_account;

INSERT INTO account_info (user_id, total, used, residue)
VALUES (1, 1000.00, 0.00, 1000.00)
ON DUPLICATE KEY UPDATE total   = VALUES(total),
                        used    = VALUES(used),
                        residue = VALUES(residue);
```

建议对应的实体类字段与表结构保持一致，避免 MyBatis-Plus 映射时出现字段缺失。

订单实体类用于映射 `order_info` 表。

文件位置：`order-service/src/main/java/io/github/atengk/order/entity/OrderInfo.java`

```java
package io.github.atengk.order.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 订单实体
 *
 * @author Ateng
 * @since 2026-05-05
 */
@Data
@TableName("order_info")
public class OrderInfo {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long orderId;

    private Long userId;

    private Long productId;

    private Integer count;

    private BigDecimal amount;

    private String status;

    private LocalDateTime createTime;

    private LocalDateTime updateTime;

}
```

库存实体类用于映射 `storage_info` 表。

文件位置：`storage-service/src/main/java/io/github/atengk/storage/entity/StorageInfo.java`

```java
package io.github.atengk.storage.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 库存实体
 *
 * @author Ateng
 * @since 2026-05-05
 */
@Data
@TableName("storage_info")
public class StorageInfo {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long productId;

    private Integer total;

    private Integer used;

    private Integer residue;

    private LocalDateTime createTime;

    private LocalDateTime updateTime;

}
```

账户实体类用于映射 `account_info` 表。

文件位置：`account-service/src/main/java/io/github/atengk/account/entity/AccountInfo.java`

```java
package io.github.atengk.account.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 账户实体
 *
 * @author Ateng
 * @since 2026-05-05
 */
@Data
@TableName("account_info")
public class AccountInfo {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long userId;

    private BigDecimal total;

    private BigDecimal used;

    private BigDecimal residue;

    private LocalDateTime createTime;

    private LocalDateTime updateTime;

}
```

### undo_log 表配置

`undo_log` 是 Seata AT 模式的客户端回滚日志表，不是 Seata Server 的事务日志表。Seata 新手部署文档也将资源脚本分为 `client` 和 `server` 两类：`client` 下包含客户端 `undo_log` 表，`server` 下包含服务端 `global_table`、`branch_table`、`lock_table` 等表。([seata.io](https://www.seata.io/zh-cn/docs/ops/deploy-guide-beginner.html?utm_source=chatgpt.com))

每个参与分布式事务的业务库都需要创建自己的 `undo_log` 表：

| 数据库          | 是否需要 `undo_log`     | 原因                                                   |
| --------------- | ----------------------- | ------------------------------------------------------ |
| `seata_order`   | 需要                    | 订单表参与全局事务。                                   |
| `seata_storage` | 需要                    | 库存表参与全局事务。                                   |
| `seata_account` | 需要                    | 账户表参与全局事务。                                   |
| `seata`         | 不需要业务库 `undo_log` | 该库用于 Seata Server 存储全局事务、分支事务和全局锁。 |

Seata 官方 AT 模式说明中给出了 MySQL `undo_log` 表结构，字段包括 `branch_id`、`xid`、`context`、`rollback_info`、`log_status`、`log_created`、`log_modified` 等。([seata.io](https://www.seata.io/en-us/docs/overview/what-is-seata.html?utm_source=chatgpt.com))

文件位置：`docker/mysql/init/04-undo-log.sql`

```sql
-- 订单库 undo_log
USE seata_order;

CREATE TABLE IF NOT EXISTS undo_log
(
    id            BIGINT       NOT NULL AUTO_INCREMENT COMMENT '主键ID',
    branch_id     BIGINT       NOT NULL COMMENT '分支事务ID',
    xid           VARCHAR(100) NOT NULL COMMENT '全局事务ID',
    context       VARCHAR(128) NOT NULL COMMENT '上下文',
    rollback_info LONGBLOB     NOT NULL COMMENT '回滚信息',
    log_status    INT          NOT NULL COMMENT '状态，0正常，1防悬挂',
    log_created   DATETIME     NOT NULL COMMENT '创建时间',
    log_modified  DATETIME     NOT NULL COMMENT '修改时间',
    PRIMARY KEY (id),
    UNIQUE KEY ux_undo_log (xid, branch_id)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4 COMMENT ='Seata AT 模式回滚日志表';

-- 库存库 undo_log
USE seata_storage;

CREATE TABLE IF NOT EXISTS undo_log
(
    id            BIGINT       NOT NULL AUTO_INCREMENT COMMENT '主键ID',
    branch_id     BIGINT       NOT NULL COMMENT '分支事务ID',
    xid           VARCHAR(100) NOT NULL COMMENT '全局事务ID',
    context       VARCHAR(128) NOT NULL COMMENT '上下文',
    rollback_info LONGBLOB     NOT NULL COMMENT '回滚信息',
    log_status    INT          NOT NULL COMMENT '状态，0正常，1防悬挂',
    log_created   DATETIME     NOT NULL COMMENT '创建时间',
    log_modified  DATETIME     NOT NULL COMMENT '修改时间',
    PRIMARY KEY (id),
    UNIQUE KEY ux_undo_log (xid, branch_id)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4 COMMENT ='Seata AT 模式回滚日志表';

-- 账户库 undo_log
USE seata_account;

CREATE TABLE IF NOT EXISTS undo_log
(
    id            BIGINT       NOT NULL AUTO_INCREMENT COMMENT '主键ID',
    branch_id     BIGINT       NOT NULL COMMENT '分支事务ID',
    xid           VARCHAR(100) NOT NULL COMMENT '全局事务ID',
    context       VARCHAR(128) NOT NULL COMMENT '上下文',
    rollback_info LONGBLOB     NOT NULL COMMENT '回滚信息',
    log_status    INT          NOT NULL COMMENT '状态，0正常，1防悬挂',
    log_created   DATETIME     NOT NULL COMMENT '创建时间',
    log_modified  DATETIME     NOT NULL COMMENT '修改时间',
    PRIMARY KEY (id),
    UNIQUE KEY ux_undo_log (xid, branch_id)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4 COMMENT ='Seata AT 模式回滚日志表';
```

`undo_log` 使用注意事项如下：

| 注意项                   | 说明                                                  |
| ------------------------ | ----------------------------------------------------- |
| 表名固定                 | 默认表名为 `undo_log`，不建议随意修改。               |
| 每个业务库都要创建       | 哪个库参与 AT 分布式事务，哪个库就必须有 `undo_log`。 |
| 不要手工清理活跃事务日志 | 事务未完成时删除 `undo_log` 可能导致回滚失败。        |
| 业务表必须有主键         | Seata 生成前后镜像和回滚 SQL 时依赖主键定位记录。     |
| 避免无条件批量更新       | 复杂批量 SQL 需要单独验证 AT 模式兼容性。             |

### 初始化脚本管理

初始化脚本建议按职责拆分，避免把 Seata Server 表、业务表、`undo_log` 表、测试数据混在同一个 SQL 文件中。这样在本地开发、测试环境、生产环境初始化时更容易按需执行。

推荐目录结构如下：

```text
docker
└── mysql
    └── init
        ├── 01-seata-server.sql
        ├── 02-database.sql
        ├── 03-business-table.sql
        ├── 04-undo-log.sql
        └── 05-test-data.sql
```

脚本职责说明：

| 脚本                    | 作用                                                         | 执行环境         |
| ----------------------- | ------------------------------------------------------------ | ---------------- |
| `01-seata-server.sql`   | 创建 Seata Server 使用的 `global_table`、`branch_table`、`lock_table`、`distributed_lock`。 | 开发、测试、生产 |
| `02-database.sql`       | 创建 `seata_order`、`seata_storage`、`seata_account`。       | 开发、测试、生产 |
| `03-business-table.sql` | 创建业务表。                                                 | 开发、测试、生产 |
| `04-undo-log.sql`       | 创建每个业务库的 `undo_log` 表。                             | 开发、测试、生产 |
| `05-test-data.sql`      | 初始化测试商品库存和测试账户余额。                           | 仅开发、测试     |

本地 Docker MySQL 首次启动时，会自动执行 `/docker-entrypoint-initdb.d` 目录下的初始化脚本。如果 MySQL 数据目录已经存在，初始化脚本不会重复执行。因此修改初始化脚本后，需要清理本地数据卷或手动进入 MySQL 执行脚本。

```bash
# 查看初始化脚本目录
ls -la docker/mysql/init

# 删除本地 MySQL 数据目录，重新初始化，仅限本地开发环境
rm -rf docker/mysql/data

# 重新启动 MySQL
docker compose up -d mysql

# 查看业务库是否创建成功
docker exec -it seata-mysql mysql -uroot -proot -e "SHOW DATABASES;"

# 查看订单库表结构
docker exec -it seata-mysql mysql -uroot -proot -e "USE seata_order; SHOW TABLES;"
```

命令说明：

`rm -rf docker/mysql/data` 会删除本地 MySQL 数据目录，只适合本地开发环境；测试和生产环境不允许用这种方式重置数据库。`SHOW DATABASES` 和 `SHOW TABLES` 用于确认数据库、业务表和 `undo_log` 是否创建成功。

## 接口设计

本章节用于定义下单、扣减库存、账户扣款和回滚测试接口。接口设计需要与前文服务职责保持一致：外部客户端只直接调用订单服务；库存服务和账户服务主要由订单服务通过 Feign 调用，不建议直接暴露给前端业务使用。

### 下单接口

下单接口由 `order-service` 提供，是全局事务入口。该接口负责创建订单、扣减库存、扣减余额。正常情况下返回订单 ID；异常情况下抛出错误，Seata 驱动全局事务回滚。

接口定义如下：

| 项目             | 内容                   |
| ---------------- | ---------------------- |
| 服务             | `order-service`        |
| 请求方式         | `POST`                 |
| 接口路径         | `/orders/create`       |
| Content-Type     | `application/json`     |
| 是否开启全局事务 | 是                     |
| 事务注解         | `@GlobalTransactional` |

请求参数：

| 字段            | 类型         | 必填 | 说明                         |
| --------------- | ------------ | ---- | ---------------------------- |
| `userId`        | `Long`       | 是   | 用户 ID。                    |
| `productId`     | `Long`       | 是   | 商品 ID。                    |
| `count`         | `Integer`    | 是   | 购买数量。                   |
| `amount`        | `BigDecimal` | 是   | 订单金额。                   |
| `mockException` | `Boolean`    | 否   | 是否模拟异常，用于验证回滚。 |

响应示例：

```json
{
  "code": 200,
  "message": "操作成功",
  "data": 1985164932201005056
}
```

下单接口控制器建议返回统一响应对象，便于前端和测试脚本判断执行结果。

文件位置：`order-service/src/main/java/io/github/atengk/order/controller/OrderController.java`

```java
package io.github.atengk.order.controller;

import io.github.atengk.order.dto.OrderCreateRequest;
import io.github.atengk.order.vo.ApiResult;
import io.github.atengk.order.service.OrderService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

/**
 * 订单接口
 *
 * @author Ateng
 * @since 2026-05-05
 */
@RestController
@RequestMapping("/orders")
@RequiredArgsConstructor
public class OrderController {

    private final OrderService orderService;

    /**
     * 创建订单
     *
     * @param request 订单创建请求
     * @return 订单ID
     */
    @PostMapping("/create")
    public ApiResult<Long> createOrder(@Valid @RequestBody OrderCreateRequest request) {
        Long orderId = orderService.createOrder(request);
        return ApiResult.success(orderId);
    }

}
```

统一响应对象用于包装接口返回值。

文件位置：`order-service/src/main/java/io/github/atengk/order/vo/ApiResult.java`

```java
package io.github.atengk.order.vo;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 接口统一响应对象
 *
 * @author Ateng
 * @since 2026-05-05
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
     * @param <T>  数据类型
     * @return 统一响应对象
     */
    public static <T> ApiResult<T> success(T data) {
        return new ApiResult<>(200, "操作成功", data);
    }

    /**
     * 返回失败结果
     *
     * @param message 错误信息
     * @param <T>     数据类型
     * @return 统一响应对象
     */
    public static <T> ApiResult<T> fail(String message) {
        return new ApiResult<>(500, message, null);
    }

}
```

调用示例：

```bash
curl -X POST "http://127.0.0.1:18081/orders/create" \
  -H "Content-Type: application/json" \
  -d '{
    "userId": 1,
    "productId": 1001,
    "count": 1,
    "amount": 50.00,
    "mockException": false
  }'
```

### 库存扣减接口

库存扣减接口由 `storage-service` 提供，主要供订单服务通过 Feign 调用。该接口自身不主动开启全局事务，但在接收到上游传递的 XID 后，会自动加入当前全局事务。

接口定义如下：

| 项目             | 内容                      |
| ---------------- | ------------------------- |
| 服务             | `storage-service`         |
| 请求方式         | `POST`                    |
| 接口路径         | `/storage/deduct`         |
| Content-Type     | `application/json`        |
| 是否开启全局事务 | 否                        |
| 本地事务         | 是，使用 `@Transactional` |

请求参数：

| 字段        | 类型      | 必填 | 说明       |
| ----------- | --------- | ---- | ---------- |
| `productId` | `Long`    | 是   | 商品 ID。  |
| `count`     | `Integer` | 是   | 扣减数量。 |

库存扣减接口控制器如下。

文件位置：`storage-service/src/main/java/io/github/atengk/storage/controller/StorageController.java`

```java
package io.github.atengk.storage.controller;

import io.github.atengk.storage.dto.StorageDeductRequest;
import io.github.atengk.storage.vo.ApiResult;
import io.github.atengk.storage.service.StorageService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

/**
 * 库存接口
 *
 * @author Ateng
 * @since 2026-05-05
 */
@RestController
@RequestMapping("/storage")
@RequiredArgsConstructor
public class StorageController {

    private final StorageService storageService;

    /**
     * 扣减库存
     *
     * @param request 库存扣减请求
     * @return 操作结果
     */
    @PostMapping("/deduct")
    public ApiResult<Void> deduct(@Valid @RequestBody StorageDeductRequest request) {
        storageService.deduct(request);
        return ApiResult.success(null);
    }

}
```

订单服务中的 Feign 客户端需要使用 `@RequestBody` 传递 JSON 请求体。

文件位置：`order-service/src/main/java/io/github/atengk/order/feign/StorageFeignClient.java`

```java
package io.github.atengk.order.feign;

import io.github.atengk.order.dto.StorageDeductRequest;
import io.github.atengk.order.vo.ApiResult;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

/**
 * 库存服务 Feign 客户端
 *
 * @author Ateng
 * @since 2026-05-05
 */
@FeignClient(name = "storage-service")
public interface StorageFeignClient {

    /**
     * 扣减库存
     *
     * @param request 库存扣减请求
     * @return 操作结果
     */
    @PostMapping("/storage/deduct")
    ApiResult<Void> deduct(@RequestBody StorageDeductRequest request);

}
```

单独调用库存扣减接口的示例：

```bash
curl -X POST "http://127.0.0.1:18082/storage/deduct" \
  -H "Content-Type: application/json" \
  -d '{
    "productId": 1001,
    "count": 1
  }'
```

该接口可以单独测试本地库存扣减能力，但单独调用时没有上游全局事务 XID，因此不会验证 Seata 全局事务传播效果。

### 账户扣款接口

账户扣款接口由 `account-service` 提供，主要供订单服务通过 Feign 调用。该接口负责校验账户余额、扣减账户余额，并可根据 `mockException` 参数主动抛出异常，用于验证全局事务回滚。

接口定义如下：

| 项目             | 内容                      |
| ---------------- | ------------------------- |
| 服务             | `account-service`         |
| 请求方式         | `POST`                    |
| 接口路径         | `/accounts/debit`         |
| Content-Type     | `application/json`        |
| 是否开启全局事务 | 否                        |
| 本地事务         | 是，使用 `@Transactional` |

请求参数：

| 字段            | 类型         | 必填 | 说明                   |
| --------------- | ------------ | ---- | ---------------------- |
| `userId`        | `Long`       | 是   | 用户 ID。              |
| `amount`        | `BigDecimal` | 是   | 扣款金额。             |
| `mockException` | `Boolean`    | 否   | 是否模拟账户扣款异常。 |

账户扣款接口控制器如下。

文件位置：`account-service/src/main/java/io/github/atengk/account/controller/AccountController.java`

```java
package io.github.atengk.account.controller;

import io.github.atengk.account.dto.AccountDebitRequest;
import io.github.atengk.account.vo.ApiResult;
import io.github.atengk.account.service.AccountService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

/**
 * 账户接口
 *
 * @author Ateng
 * @since 2026-05-05
 */
@RestController
@RequestMapping("/accounts")
@RequiredArgsConstructor
public class AccountController {

    private final AccountService accountService;

    /**
     * 账户扣款
     *
     * @param request 账户扣款请求
     * @return 操作结果
     */
    @PostMapping("/debit")
    public ApiResult<Void> debit(@Valid @RequestBody AccountDebitRequest request) {
        accountService.debit(request);
        return ApiResult.success(null);
    }

}
```

订单服务中的 Feign 客户端如下。

文件位置：`order-service/src/main/java/io/github/atengk/order/feign/AccountFeignClient.java`

```java
package io.github.atengk.order.feign;

import io.github.atengk.order.dto.AccountDebitRequest;
import io.github.atengk.order.vo.ApiResult;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

/**
 * 账户服务 Feign 客户端
 *
 * @author Ateng
 * @since 2026-05-05
 */
@FeignClient(name = "account-service")
public interface AccountFeignClient {

    /**
     * 账户扣款
     *
     * @param request 账户扣款请求
     * @return 操作结果
     */
    @PostMapping("/accounts/debit")
    ApiResult<Void> debit(@RequestBody AccountDebitRequest request);

}
```

单独调用账户扣款接口的示例：

```bash
curl -X POST "http://127.0.0.1:18083/accounts/debit" \
  -H "Content-Type: application/json" \
  -d '{
    "userId": 1,
    "amount": 50.00,
    "mockException": false
  }'
```

模拟账户扣款异常：

```bash
curl -X POST "http://127.0.0.1:18083/accounts/debit" \
  -H "Content-Type: application/json" \
  -d '{
    "userId": 1,
    "amount": 50.00,
    "mockException": true
  }'
```

单独调用账户服务时，只能验证账户服务本地事务回滚；要验证订单、库存、账户三个服务全部回滚，必须通过订单服务的下单接口发起请求。

### 测试回滚接口

测试回滚接口用于快速触发异常场景，验证 Seata 是否正确回滚订单库、库存库和账户库。建议测试入口仍放在 `order-service`，这样可以复用真实下单链路，而不是绕过业务流程单独构造异常。

接口定义如下：

| 项目         | 内容                                                         |
| ------------ | ------------------------------------------------------------ |
| 服务         | `order-service`                                              |
| 请求方式     | `POST`                                                       |
| 接口路径     | `/orders/test-rollback`                                      |
| Content-Type | `application/json`                                           |
| 作用         | 固定将 `mockException` 设置为 `true`，触发下游账户服务异常。 |

测试回滚接口控制器如下。

文件位置：`order-service/src/main/java/io/github/atengk/order/controller/OrderController.java`

```java
package io.github.atengk.order.controller;

import cn.hutool.core.bean.BeanUtil;
import io.github.atengk.order.dto.OrderCreateRequest;
import io.github.atengk.order.vo.ApiResult;
import io.github.atengk.order.service.OrderService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

/**
 * 订单接口
 *
 * @author Ateng
 * @since 2026-05-05
 */
@RestController
@RequestMapping("/orders")
@RequiredArgsConstructor
public class OrderController {

    private final OrderService orderService;

    /**
     * 创建订单
     *
     * @param request 订单创建请求
     * @return 订单ID
     */
    @PostMapping("/create")
    public ApiResult<Long> createOrder(@Valid @RequestBody OrderCreateRequest request) {
        Long orderId = orderService.createOrder(request);
        return ApiResult.success(orderId);
    }

    /**
     * 测试全局事务回滚
     *
     * @param request 订单创建请求
     * @return 订单ID
     */
    @PostMapping("/test-rollback")
    public ApiResult<Long> testRollback(@Valid @RequestBody OrderCreateRequest request) {
        OrderCreateRequest rollbackRequest = BeanUtil.copyProperties(request, OrderCreateRequest.class);
        rollbackRequest.setMockException(true);

        Long orderId = orderService.createOrder(rollbackRequest);
        return ApiResult.success(orderId);
    }

}
```

为了让异常响应更清晰，建议增加统一异常处理。这样在触发 Seata 回滚时，接口可以返回明确错误信息，日志中也能保留异常堆栈。

文件位置：`order-service/src/main/java/io/github/atengk/order/config/GlobalExceptionHandler.java`

```java
package io.github.atengk.order.config;

import io.github.atengk.order.vo.ApiResult;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.*;

/**
 * 全局异常处理器
 *
 * @author Ateng
 * @since 2026-05-05
 */
@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    /**
     * 处理参数校验异常
     *
     * @param ex 参数校验异常
     * @return 统一响应对象
     */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ApiResult<Void> handleMethodArgumentNotValidException(MethodArgumentNotValidException ex) {
        String message = ex.getBindingResult().getAllErrors().getFirst().getDefaultMessage();
        log.warn("接口参数校验失败，message={}", message);
        return ApiResult.fail(message);
    }

    /**
     * 处理业务异常
     *
     * @param ex 业务异常
     * @return 统一响应对象
     */
    @ExceptionHandler({
            IllegalArgumentException.class,
            IllegalStateException.class
    })
    public ApiResult<Void> handleBusinessException(RuntimeException ex) {
        log.warn("业务处理失败，message={}", ex.getMessage(), ex);
        return ApiResult.fail(ex.getMessage());
    }

    /**
     * 处理系统异常
     *
     * @param ex 系统异常
     * @return 统一响应对象
     */
    @ExceptionHandler(Exception.class)
    public ApiResult<Void> handleException(Exception ex) {
        log.error("系统异常", ex);
        return ApiResult.fail("系统异常，请稍后重试");
    }

}
```

调用回滚测试接口：

```bash
curl -X POST "http://127.0.0.1:18081/orders/test-rollback" \
  -H "Content-Type: application/json" \
  -d '{
    "userId": 1,
    "productId": 1001,
    "count": 1,
    "amount": 50.00
  }'
```

预期响应：

```json
{
  "code": 500,
  "message": "模拟账户扣款异常，触发 Seata 全局回滚",
  "data": null
}
```

回滚后执行以下 SQL 验证数据一致性：

```sql
-- 订单不应保留本次失败订单，或订单数据应回滚到事务前状态
SELECT *
FROM seata_order.order_info
ORDER BY id DESC
LIMIT 10;

-- 库存应恢复到请求前
SELECT product_id, total, used, residue
FROM seata_storage.storage_info
WHERE product_id = 1001;

-- 账户余额应恢复到请求前
SELECT user_id, total, used, residue
FROM seata_account.account_info
WHERE user_id = 1;

-- 正常完成二阶段后，undo_log 通常会被清理
SELECT *
FROM seata_order.undo_log;

SELECT *
FROM seata_storage.undo_log;

SELECT *
FROM seata_account.undo_log;
```

回滚测试判断标准如下：

| 检查项            | 正常结果                                                     |
| ----------------- | ------------------------------------------------------------ |
| `order_info`      | 测试回滚请求对应的订单记录不存在，或回到事务前状态。         |
| `storage_info`    | `used`、`residue` 与请求前一致。                             |
| `account_info`    | `used`、`residue` 与请求前一致。                             |
| `undo_log`        | 二阶段完成后通常无残留；若存在残留，需要结合 Seata Server 日志排查。 |
| 服务日志          | `order-service`、`storage-service`、`account-service` 中同一次请求的 `xid` 一致。 |
| Seata Server 日志 | 能看到全局事务回滚记录。                                     |



## 功能验证

本章节用于验证 Seata 是否真正接管了订单、库存、账户三个服务的分布式事务。验证时不要只看接口是否返回成功，还要同时检查三个业务库的数据、`undo_log` 状态、Seata Server 日志和各服务中的 `xid` 是否一致。Seata AT 模式一阶段会在同一个本地事务中提交业务数据和回滚日志，二阶段提交时异步清理回滚日志，二阶段回滚时基于回滚日志执行补偿。([seata.apache.org](https://seata.apache.org/docs/dev/mode/at-mode?utm_source=chatgpt.com))

### 正常提交验证

正常提交验证用于确认订单服务开启全局事务后，库存服务和账户服务能够正常加入同一个全局事务，并在所有分支执行成功后完成全局提交。

验证前需要保证基础数据存在：

```sql
-- 初始化库存数据
USE seata_storage;

INSERT INTO storage_info (product_id, total, used, residue)
VALUES (1001, 100, 0, 100)
ON DUPLICATE KEY UPDATE total   = VALUES(total),
                        used    = VALUES(used),
                        residue = VALUES(residue);

-- 初始化账户数据
USE seata_account;

INSERT INTO account_info (user_id, total, used, residue)
VALUES (1, 1000.00, 0.00, 1000.00)
ON DUPLICATE KEY UPDATE total   = VALUES(total),
                        used    = VALUES(used),
                        residue = VALUES(residue);
```

正常下单请求如下：

```bash
curl -X POST "http://127.0.0.1:18081/orders/create" \
  -H "Content-Type: application/json" \
  -d '{
    "userId": 1,
    "productId": 1001,
    "count": 1,
    "amount": 50.00,
    "mockException": false
  }'
```

预期响应如下：

```json
{
  "code": 200,
  "message": "操作成功",
  "data": 1985164932201005056
}
```

执行成功后检查订单、库存、账户数据：

```sql
-- 查看最新订单
SELECT order_id, user_id, product_id, count, amount, status, create_time
FROM seata_order.order_info
ORDER BY id DESC
LIMIT 5;

-- 查看库存扣减结果
SELECT product_id, total, used, residue
FROM seata_storage.storage_info
WHERE product_id = 1001;

-- 查看账户扣款结果
SELECT user_id, total, used, residue
FROM seata_account.account_info
WHERE user_id = 1;
```

正常提交后，数据应符合以下结果：

| 检查项            | 预期结果                                                     |
| ----------------- | ------------------------------------------------------------ |
| 订单表            | 新增一条订单记录，状态为 `FINISHED`。                        |
| 库存表            | `used` 增加 `1`，`residue` 减少 `1`。                        |
| 账户表            | `used` 增加 `50.00`，`residue` 减少 `50.00`。                |
| 服务日志          | `order-service`、`storage-service`、`account-service` 中同一次请求的 `xid` 一致。 |
| Seata Server 日志 | 可以看到全局事务提交完成。                                   |
| `undo_log`        | 二阶段提交完成后通常会被异步清理。                           |

验证 `undo_log`：

```sql
SELECT COUNT(*) AS order_undo_count
FROM seata_order.undo_log;

SELECT COUNT(*) AS storage_undo_count
FROM seata_storage.undo_log;

SELECT COUNT(*) AS account_undo_count
FROM seata_account.undo_log;
```

正常提交后，`undo_log` 不一定在接口返回瞬间立即为 `0`，因为 AT 模式二阶段提交会异步清理回滚日志。短暂等待后再次查询，通常应被清理。([seata.apache.org](https://seata.apache.org/docs/dev/mode/at-mode?utm_source=chatgpt.com))

### 异常回滚验证

异常回滚验证用于确认任一分支服务抛出异常时，Seata 能够驱动所有已完成的一阶段分支事务回滚。Seata AT 模式的回滚依赖一阶段生成的 `undo_log`，二阶段回滚时会根据回滚日志中的前后镜像生成补偿 SQL。([seata.apache.org](https://seata.apache.org/docs/dev/mode/at-mode?utm_source=chatgpt.com))

先记录测试前数据：

```sql
-- 测试前订单数量
SELECT COUNT(*) AS order_count
FROM seata_order.order_info;

-- 测试前库存状态
SELECT product_id, total, used, residue
FROM seata_storage.storage_info
WHERE product_id = 1001;

-- 测试前账户状态
SELECT user_id, total, used, residue
FROM seata_account.account_info
WHERE user_id = 1;
```

调用回滚测试接口：

```bash
curl -X POST "http://127.0.0.1:18081/orders/test-rollback" \
  -H "Content-Type: application/json" \
  -d '{
    "userId": 1,
    "productId": 1001,
    "count": 1,
    "amount": 50.00
  }'
```

预期响应如下：

```json
{
  "code": 500,
  "message": "模拟账户扣款异常，触发 Seata 全局回滚",
  "data": null
}
```

回滚后检查数据：

```sql
-- 回滚后订单数量应与测试前一致，或不存在本次失败订单
SELECT COUNT(*) AS order_count
FROM seata_order.order_info;

SELECT order_id, user_id, product_id, count, amount, status, create_time
FROM seata_order.order_info
ORDER BY id DESC
LIMIT 5;

-- 回滚后库存应恢复到测试前
SELECT product_id, total, used, residue
FROM seata_storage.storage_info
WHERE product_id = 1001;

-- 回滚后账户应恢复到测试前
SELECT user_id, total, used, residue
FROM seata_account.account_info
WHERE user_id = 1;
```

异常回滚验证结果如下：

| 检查项            | 预期结果                                       |
| ----------------- | ---------------------------------------------- |
| 订单表            | 本次失败订单不应最终保留，或回滚到事务前状态。 |
| 库存表            | `used`、`residue` 与测试前一致。               |
| 账户表            | `used`、`residue` 与测试前一致。               |
| 客户端日志        | 三个服务中同一次请求的 `xid` 一致。            |
| Seata Server 日志 | 能看到全局事务回滚完成。                       |
| `undo_log`        | 二阶段回滚完成后通常会被清理。                 |

建议再补充库存不足场景：

```bash
curl -X POST "http://127.0.0.1:18081/orders/create" \
  -H "Content-Type: application/json" \
  -d '{
    "userId": 1,
    "productId": 1001,
    "count": 99999,
    "amount": 50.00,
    "mockException": false
  }'
```

该场景应在库存服务抛出“商品库存不足”异常。订单服务已写入的订单数据应被回滚，账户服务不应发生扣款。

### 数据一致性验证

数据一致性验证用于从数据库最终状态判断全局事务是否正确提交或回滚。建议每次验证前记录基线数据，执行接口请求后再对比订单、库存、账户三类数据。

可以使用以下 SQL 快速查看当前业务状态：

```sql
-- 订单最新记录
SELECT id, order_id, user_id, product_id, count, amount, status, create_time
FROM seata_order.order_info
ORDER BY id DESC
LIMIT 10;

-- 商品库存
SELECT product_id, total, used, residue, update_time
FROM seata_storage.storage_info
WHERE product_id = 1001;

-- 用户账户
SELECT user_id, total, used, residue, update_time
FROM seata_account.account_info
WHERE user_id = 1;
```

一致性判断规则如下：

| 场景         | 订单               | 库存       | 账户       |
| ------------ | ------------------ | ---------- | ---------- |
| 正常提交     | 新增订单，状态完成 | 库存已扣减 | 余额已扣减 |
| 库存失败     | 订单回滚           | 库存不变   | 账户不变   |
| 账户失败     | 订单回滚           | 库存回滚   | 账户回滚   |
| 订单后置失败 | 订单回滚           | 库存回滚   | 账户回滚   |

可以增加一条汇总检查 SQL，用于快速观察三个库的关键数据：

```sql
-- 当前订单数量
SELECT 'order_count' AS item, COUNT(*) AS value
FROM seata_order.order_info;

-- 当前库存状态
SELECT 'storage_residue' AS item, residue AS value
FROM seata_storage.storage_info
WHERE product_id = 1001;

-- 当前账户余额
SELECT 'account_residue' AS item, residue AS value
FROM seata_account.account_info
WHERE user_id = 1;
```

同时检查 Seata Server 事务日志表：

```sql
-- 查看最近全局事务记录
SELECT xid, transaction_id, status, application_id, transaction_service_group, transaction_name, gmt_create, gmt_modified
FROM seata.global_table
ORDER BY gmt_modified DESC
LIMIT 10;

-- 查看最近分支事务记录
SELECT branch_id, xid, resource_id, branch_type, status, client_id, gmt_create, gmt_modified
FROM seata.branch_table
ORDER BY gmt_modified DESC
LIMIT 10;

-- 查看全局锁残留
SELECT row_key, xid, branch_id, resource_id, table_name, pk, status, gmt_create, gmt_modified
FROM seata.lock_table
ORDER BY gmt_modified DESC
LIMIT 10;
```

如果接口已经返回，但 `global_table`、`branch_table`、`lock_table` 中长期存在未完成记录，需要结合 Seata Server 日志判断是否存在 TC 异常、数据库连接异常、全局锁冲突或客户端分支回滚失败。

## 日志与问题定位

本章节用于说明如何通过客户端日志、Seata Server 日志和数据库状态定位分布式事务问题。Seata 的事务上下文由 `RootContext` 管理，应用开启全局事务后会自动绑定 XID，事务提交或回滚结束后会自动解绑 XID；跨服务传播的核心也是 XID 在运行时上下文和请求链路中的传递。([seata.apache.org](https://seata.apache.org/docs/user/microservice?utm_source=chatgpt.com))

### Seata 客户端日志

Seata 客户端日志主要用于观察业务服务是否成功连接 TC、是否开启全局事务、是否注册分支事务、是否收到提交或回滚通知。

建议在开发环境中将 Seata 客户端日志级别调整为 `debug`，生产环境保持 `info` 或按需临时调整。

文件位置：各业务服务 `src/main/resources/application.yml`

```yaml
logging:
  level:
    # 当前项目业务日志
    io.github.atengk: info

    # Seata 客户端核心日志，排查事务问题时可临时调整为 debug
    io.seata: debug

    # OpenFeign 调用日志，排查远程调用时可临时开启
    org.springframework.cloud.openfeign: debug
```

建议业务日志中固定打印以下字段：

| 字段        | 说明                                                        |
| ----------- | ----------------------------------------------------------- |
| `xid`       | 全局事务 ID，用于串联订单、库存、账户和 Seata Server 日志。 |
| `orderId`   | 订单 ID，用于定位本次业务请求。                             |
| `userId`    | 用户 ID，用于定位账户数据。                                 |
| `productId` | 商品 ID，用于定位库存数据。                                 |
| `branch`    | 当前业务分支，例如订单创建、库存扣减、账户扣款。            |

推荐日志格式如下：

```text
开始创建订单全局事务，xid=192.168.1.10:8091:1985164932201005056，orderId=1985164932201005056，userId=1，productId=1001
准备调用库存服务，xid=192.168.1.10:8091:1985164932201005056，orderId=1985164932201005056，productId=1001
开始扣减库存，xid=192.168.1.10:8091:1985164932201005056，productId=1001，count=1
准备调用账户服务，xid=192.168.1.10:8091:1985164932201005056，orderId=1985164932201005056，userId=1
开始账户扣款，xid=192.168.1.10:8091:1985164932201005056，userId=1，amount=50.00
```

如果需要统一日志格式，可以配置 `logback-spring.xml`。

文件位置：各业务服务 `src/main/resources/logback-spring.xml`

```xml
<?xml version="1.0" encoding="UTF-8"?>
<configuration>
    <!-- 控制台日志格式，包含时间、线程、级别、日志名和消息 -->
    <property name="CONSOLE_LOG_PATTERN"
              value="%d{yyyy-MM-dd HH:mm:ss.SSS} [%thread] %-5level %logger{50} - %msg%n"/>

    <appender name="CONSOLE" class="ch.qos.logback.core.ConsoleAppender">
        <encoder>
            <pattern>${CONSOLE_LOG_PATTERN}</pattern>
            <charset>UTF-8</charset>
        </encoder>
    </appender>

    <!-- 业务包日志 -->
    <logger name="io.github.atengk" level="INFO"/>

    <!-- Seata 日志，开发排查时可改为 DEBUG -->
    <logger name="io.seata" level="INFO"/>

    <root level="INFO">
        <appender-ref ref="CONSOLE"/>
    </root>
</configuration>
```

排查客户端问题时，优先搜索以下关键字：

```text
GlobalTransactional
Begin new global transaction
Register branch successfully
Branch committing
Branch rollbacking
Global commit
Global rollback
RootContext
xid
```

客户端定位顺序建议如下：

| 顺序 | 检查项                          | 说明                                               |
| ---- | ------------------------------- | -------------------------------------------------- |
| 1    | `@GlobalTransactional` 是否执行 | 查看订单服务是否打印全局事务开始日志。             |
| 2    | `RootContext.getXID()` 是否有值 | 入口服务和下游服务都应能看到同一个 XID。           |
| 3    | 分支服务是否注册成功            | 库存服务、账户服务应注册分支事务。                 |
| 4    | 本地事务是否回滚                | 下游服务异常时，本地 `@Transactional` 不能吞异常。 |
| 5    | 数据源是否被代理                | 未代理数据源时，AT 模式无法生成 `undo_log`。       |

Seata 配置文档中说明，`seata.enabled` 用于控制 Spring Boot 自动装配，`seata.enableAutoDataSourceProxy` 用于控制数据源自动代理；使用 starter 时默认会开启数据源自动代理，但仍建议在排查时显式检查配置和启动日志。([seata.apache.org](https://seata.apache.org/docs/user/configurations?utm_source=chatgpt.com))

### Seata Server 日志

Seata Server 日志主要用于定位 TC 是否正常启动、是否连接注册中心、是否连接事务日志库、是否接收到全局事务请求、是否成功提交或回滚分支事务。

查看 Seata Server 容器日志：

```bash
# 查看 Seata Server 实时日志
docker logs -f seata-server

# 查看最近 200 行日志
docker logs --tail=200 seata-server

# 按关键字过滤全局事务
docker logs seata-server 2>&1 | grep -i "global"

# 按关键字过滤回滚日志
docker logs seata-server 2>&1 | grep -i "rollback"
```

命令说明：

`docker logs -f` 用于实时观察 Seata Server 日志；`grep -i` 用于忽略大小写过滤关键字。排查回滚问题时，建议优先使用 `xid` 作为检索关键字，因为同一个 XID 可以贯穿订单服务、库存服务、账户服务和 Seata Server。

Seata Server 常见日志关键字如下：

```text
Server started
registry
config
global session
branch session
GlobalBegin
GlobalCommit
GlobalRollback
BranchRegister
BranchReport
lock retry
```

如果 Seata Server 使用 DB 存储模式，还需要检查 `seata` 库中的事务日志表：

```sql
-- 查看全局事务
SELECT xid, status, application_id, transaction_service_group, transaction_name, gmt_create, gmt_modified
FROM seata.global_table
ORDER BY gmt_modified DESC
LIMIT 20;

-- 查看分支事务
SELECT branch_id, xid, resource_id, branch_type, status, client_id, gmt_create, gmt_modified
FROM seata.branch_table
ORDER BY gmt_modified DESC
LIMIT 20;

-- 查看全局锁
SELECT row_key, xid, branch_id, resource_id, table_name, pk, status, gmt_create, gmt_modified
FROM seata.lock_table
ORDER BY gmt_modified DESC
LIMIT 20;
```

Seata Server 定位顺序如下：

| 顺序 | 检查项               | 说明                                                         |
| ---- | -------------------- | ------------------------------------------------------------ |
| 1    | Server 是否启动成功  | 查看 `7091` 控制台端口和 `8091` TC 端口。                    |
| 2    | 是否注册到 Nacos     | 在 Nacos 服务列表中查看 `seata-server`。                     |
| 3    | 是否读取到配置       | 检查 `seataServer.properties` 的 Data ID、Group、namespace。 |
| 4    | 是否连接 DB 成功     | 检查 `store.mode=db` 和 `store.db.*` 配置。                  |
| 5    | 是否收到全局事务请求 | 查看 `GlobalBegin`、`GlobalCommit`、`GlobalRollback` 日志。  |
| 6    | 是否存在锁冲突       | 查看 `lock_table` 和 Server 日志中的 lock retry。            |

### 常见异常定位

常见异常应优先从配置匹配、事务分组、数据源代理、`undo_log`、异常传播五个方向排查。

| 异常现象                                      | 常见原因                                 | 处理方式                                                     |
| --------------------------------------------- | ---------------------------------------- | ------------------------------------------------------------ |
| `can not get cluster name in registry config` | 找不到事务分组映射配置                   | 检查 `seata.tx-service-group` 与 `service.vgroupMapping.xxx` 是否匹配。 |
| `no available service found in cluster`       | Seata Server 未注册或集群名不一致        | 检查 Nacos 服务列表、`cluster`、`group`、`namespace`。       |
| 下游服务 `xid` 为空                           | XID 未跨服务传递                         | 确认 Feign 调用发生在 `@GlobalTransactional` 方法内部。      |
| 全局事务未回滚                                | 异常被捕获后未抛出                       | 捕获异常后重新抛出运行时异常或业务异常。                     |
| 找不到 `undo_log`                             | 业务库未创建 `undo_log`                  | 在每个参与事务的业务库创建 `undo_log`。                      |
| 回滚失败                                      | 业务数据被其他事务修改，前后镜像校验失败 | 排查是否存在绕过 Seata 的直接写库操作。                      |
| 全局锁等待超时                                | 多个全局事务更新同一热点行               | 降低热点竞争，缩短事务链路，优化 SQL。                       |
| 数据源未代理                                  | 自动代理未开启或手动配置冲突             | 检查 `seata.enable-auto-data-source-proxy` 和数据源 Bean。   |
| Feign fallback 后事务提交                     | 降级逻辑吞掉远程调用异常                 | 分布式事务链路中失败应抛出异常，不要返回伪成功。             |
| `Branch session rollback failed`              | 分支回滚失败                             | 检查业务库连接、`undo_log`、业务表主键和回滚 SQL。           |

事务分组异常排查重点：

```yaml
seata:
  # 客户端事务分组
  tx-service-group: default_tx_group

  service:
    vgroup-mapping:
      # default_tx_group 必须能映射到 Seata Server 注册时的 cluster
      default_tx_group: default
```

Seata Server 注册配置需要与之对应：

```yaml
seata:
  registry:
    nacos:
      # Seata Server 所属 TC 集群
      cluster: default
```

如果业务服务使用 Nacos 配置中心，`service.vgroupMapping.default_tx_group=default` 应放在客户端实际读取的 Data ID、Group 和 namespace 中。配置存在但仍报错时，通常是客户端读取的 namespace 或 group 与 Nacos 中实际配置不一致。

## 开发注意事项

本章节用于总结 Seata AT 模式在真实开发中的使用边界。Seata 可以降低分布式事务接入成本，但不应被当成所有一致性问题的默认解法。长事务、高并发热点行、外部三方接口、复杂 SQL、异步链路都需要单独设计。

### AT 模式使用限制

AT 模式适合关系型数据库、本地事务、JDBC 访问路径清晰的业务。官方文档说明，AT 模式的前提是支持本地 ACID 事务的关系型数据库，并且 Java 应用通过 JDBC 访问数据库。([seata.apache.org](https://seata.apache.org/docs/dev/mode/at-mode?utm_source=chatgpt.com))

AT 模式主要限制如下：

| 限制                 | 说明                                                 | 建议                                               |
| -------------------- | ---------------------------------------------------- | -------------------------------------------------- |
| 依赖关系型数据库     | AT 模式主要围绕 JDBC 数据源代理工作。                | Redis、MQ、ES、三方接口不要直接按 AT 理解。        |
| 依赖业务表主键       | 回滚镜像需要通过主键定位记录。                       | 所有参与事务的表必须有稳定主键。                   |
| 复杂 SQL 需要验证    | 批量更新、子查询更新、函数更新等可能存在兼容性风险。 | 核心 SQL 必须做回滚测试。                          |
| 热点行存在锁竞争     | AT 模式提交前需要申请全局锁。                        | 热点库存、余额类场景要控制事务粒度。               |
| 长事务成本高         | 全局事务时间越长，锁冲突和回滚成本越高。             | 不要在全局事务中执行慢接口、人工审批、长时间等待。 |
| 外部接口不可自动回滚 | 三方支付、短信、物流接口无法被 Seata 自动补偿。      | 使用 TCC、Saga、可靠消息或补偿任务。               |
| 读隔离需注意         | 普通查询不会像 `SELECT FOR UPDATE` 一样申请全局锁。  | 强一致读场景需要单独设计。                         |

AT 模式开发建议：

| 建议                | 说明                                                  |
| ------------------- | ----------------------------------------------------- |
| 控制全局事务范围    | 只把必须一起提交或回滚的数据库写操作放入全局事务。    |
| 缩短事务链路        | 全局事务中不要做大文件处理、远程慢接口、复杂计算。    |
| 避免热点大事务      | 库存、账户、优惠券等热点数据需要做并发压测。          |
| 失败必须抛异常      | 不要通过返回错误码但继续执行的方式表达失败。          |
| 不要绕过 Seata 写库 | 同一业务数据不要同时存在 Seata 代理写入和非代理写入。 |

### 事务传播注意事项

事务传播的核心是 XID 传播。Seata 文档说明，事务上下文由 `RootContext` 管理，应用开启全局事务后会自动绑定 XID；默认情况下，`RootContext` 基于 `ThreadLocal` 保存 XID，因此跨线程、异步任务、消息队列等场景需要额外处理事务上下文。([seata.apache.org](https://seata.apache.org/docs/user/microservice?utm_source=chatgpt.com))

事务传播注意事项如下：

| 场景           | 风险                                 | 建议                                                       |
| -------------- | ------------------------------------ | ---------------------------------------------------------- |
| Feign 同步调用 | 正常情况下可自动传递 XID             | 确保调用发生在 `@GlobalTransactional` 方法内部。           |
| 异步线程       | `ThreadLocal` 上下文可能丢失         | 不要在全局事务中异步执行核心写操作。                       |
| MQ 消息        | 消息消费者不是当前线程和当前请求链路 | 不要把 MQ 消费逻辑当作同一个全局事务分支。                 |
| 定时任务       | 没有上游全局事务上下文               | 需要重新设计事务边界。                                     |
| Feign fallback | fallback 可能吞掉异常                | 分布式事务链路中失败应抛出异常。                           |
| 捕获异常       | 捕获后不抛出会导致全局事务提交       | 捕获只用于记录日志，随后重新抛出异常。                     |
| 同类方法调用   | Spring AOP 代理可能失效              | 事务方法通过代理对象调用，避免 `this.xxx()` 调用事务方法。 |

推荐写法是让全局事务入口保持清晰：

```java
@GlobalTransactional(name = "create-order-global-tx", rollbackFor = Exception.class)
@Transactional(rollbackFor = Exception.class)
public Long createOrder(OrderCreateRequest request) {
    // 1. 创建订单
    // 2. 调用库存服务
    // 3. 调用账户服务
    // 4. 任一环节失败直接抛出异常
}
```

不推荐写法如下：

```java
@GlobalTransactional(name = "create-order-global-tx", rollbackFor = Exception.class)
public Long createOrder(OrderCreateRequest request) {
    try {
        // 创建订单
        // 调用库存服务
        // 调用账户服务
    } catch (Exception ex) {
        // 错误示例：吞掉异常会导致全局事务可能被提交
        log.warn("下单失败，但没有继续抛出异常", ex);
        return -1L;
    }
}
```

正确处理方式如下：

```java
@GlobalTransactional(name = "create-order-global-tx", rollbackFor = Exception.class)
public Long createOrder(OrderCreateRequest request) {
    try {
        // 创建订单
        // 调用库存服务
        // 调用账户服务
        return orderId;
    } catch (Exception ex) {
        log.error("下单失败，准备触发 Seata 全局回滚", ex);
        throw ex;
    }
}
```

### 数据库操作规范

数据库操作规范用于降低 AT 模式回滚失败、锁冲突、镜像不一致和并发异常的概率。AT 模式会解析业务 SQL，生成前镜像和后镜像，并在本地事务中写入 `undo_log`；回滚时会比较后镜像和当前数据，再根据前镜像生成补偿 SQL。([seata.apache.org](https://seata.apache.org/docs/dev/mode/at-mode?utm_source=chatgpt.com))

业务表设计规范如下：

| 规范                       | 说明                                           |
| -------------------------- | ---------------------------------------------- |
| 表必须有主键               | Seata 回滚需要通过主键定位数据。               |
| 使用 InnoDB                | 需要本地事务能力。                             |
| 字段类型保持稳定           | 频繁修改字段类型可能影响回滚日志兼容性。       |
| 关键金额字段使用 `DECIMAL` | 不要使用 `DOUBLE`、`FLOAT` 保存金额。          |
| 建议保留创建和更新时间     | 便于排查数据变化。                             |
| 不要人工修改 `undo_log`    | 未完成事务的 `undo_log` 被删除会导致回滚失败。 |

SQL 编写规范如下：

| 规范               | 推荐写法                   | 不推荐写法                |
| ------------------ | -------------------------- | ------------------------- |
| 更新必须带明确条件 | `WHERE product_id = ?`     | 无条件更新整表            |
| 扣减库存带余额判断 | `residue >= count`         | 先查后无条件更新          |
| 扣款带余额判断     | `residue >= amount`        | 余额不足也执行扣减        |
| 避免复杂批量更新   | 分批、按主键更新           | 一条 SQL 更新大量热点数据 |
| 避免非确定性更新   | 明确字段值和条件           | 依赖随机函数、复杂函数    |
| 避免绕过代理数据源 | 统一走 Spring 管理的数据源 | 手动创建 JDBC 连接写库    |

推荐库存扣减 SQL 逻辑：

```sql
UPDATE storage_info
SET used    = used + 1,
    residue = residue - 1
WHERE product_id = 1001
  AND residue >= 1;
```

推荐账户扣款 SQL 逻辑：

```sql
UPDATE account_info
SET used    = used + 50.00,
    residue = residue - 50.00
WHERE user_id = 1
  AND residue >= 50.00;
```

开发和测试环境可以通过以下 SQL 检查是否存在缺失主键的业务表：

```sql
SELECT table_schema, table_name
FROM information_schema.tables t
WHERE table_schema IN ('seata_order', 'seata_storage', 'seata_account')
  AND table_type = 'BASE TABLE'
  AND NOT EXISTS (
      SELECT 1
      FROM information_schema.table_constraints c
      WHERE c.table_schema = t.table_schema
        AND c.table_name = t.table_name
        AND c.constraint_type = 'PRIMARY KEY'
  );
```

检查每个业务库是否存在 `undo_log`：

```sql
SELECT table_schema, table_name
FROM information_schema.tables
WHERE table_schema IN ('seata_order', 'seata_storage', 'seata_account')
  AND table_name = 'undo_log';
```

最终开发约束建议如下：

| 类别     | 要求                                                  |
| -------- | ----------------------------------------------------- |
| 事务边界 | 一个业务入口只开启一个全局事务，下游服务只加入事务。  |
| 异常处理 | 失败必须抛异常，不要吞异常或返回伪成功。              |
| 数据源   | 使用 Seata 自动代理数据源，不要重复手动代理。         |
| 表结构   | 参与事务的业务表必须有主键，业务库必须有 `undo_log`。 |
| SQL      | 避免无条件更新、复杂批量更新和热点长事务。            |
| 调用链   | 全局事务中只放必要的同步数据库写操作。                |
| 外部系统 | 三方接口、MQ、异步任务不要直接纳入 AT 模式事务。      |