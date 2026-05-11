# PostgreSQL 17

PostgreSQL 17 是一个面向生产环境的开源对象关系型数据库版本，适合承担业务系统中的核心数据存储、事务处理、复杂查询、权限控制、备份恢复和数据一致性保障职责。本文以 PostgreSQL 17 为基准，后续安装、初始化、连接和项目集成示例均围绕该版本展开。

## PostgreSQL 17 概述

本章用于说明 PostgreSQL 17 在项目中的定位、版本能力、适用场景以及开发团队与数据库之间的职责边界。理解这些内容后，再进入安装、建库、建表、SQL 开发和运维管理会更加清晰。

### 数据库定位

PostgreSQL 是一个功能完整的对象关系型数据库管理系统，既支持传统关系模型，也支持事务、复杂 SQL、JSON、数组、全文搜索、扩展插件、自定义类型和多种索引类型。它在项目中通常不是简单的“存表工具”，而是核心业务数据平台的一部分。

在 Java 或 Spring Boot 项目中，PostgreSQL 通常承担以下职责：

| 定位             | 说明                                                |
| ---------------- | --------------------------------------------------- |
| 核心关系型数据库 | 存储用户、订单、权限、配置、日志、审计等结构化数据  |
| 事务型数据引擎   | 支持事务提交、回滚、隔离级别和锁机制                |
| 复杂查询引擎     | 支持 JOIN、子查询、窗口函数、聚合统计和执行计划分析 |
| 半结构化数据存储 | 通过 `jsonb` 存储动态属性、扩展字段和接口报文       |
| 可扩展数据库平台 | 通过插件扩展 UUID、加密、模糊搜索、地理信息等能力   |
| 运维友好型数据库 | 提供备份恢复、日志、统计视图、复制和高可用能力      |

数据库层负责保证数据可靠性、一致性和可恢复性；应用层负责业务流程、接口编排、参数校验、权限上下文和外部系统调用。两者边界清晰，后期维护成本会更低。

### 版本特性

PostgreSQL 17 于 2024-09-26 发布，主要增强集中在 VACUUM 内存管理、SQL/JSON、查询性能、逻辑复制、增量备份和 COPY 导入等方向。([PostgreSQL](https://www.postgresql.org/docs/current/release-17.html?utm_source=chatgpt.com))

| 特性方向      | PostgreSQL 17 改进                                           | 项目开发影响                                   |
| ------------- | ------------------------------------------------------------ | ---------------------------------------------- |
| VACUUM 优化   | 引入新的 VACUUM 内存管理机制，降低内存消耗并提升清理效率     | 对大表、高频更新表、软删除表更友好             |
| SQL/JSON 增强 | 增加 SQL/JSON 构造函数、查询函数和 `JSON_TABLE()`            | JSON 数据可以更方便地转换成关系型结果参与查询  |
| 查询性能增强  | 改进顺序读取、高并发写入、B-Tree 多值查询等场景              | 常规 OLTP 查询和批量查询有更好的优化空间       |
| 逻辑复制增强  | 增加故障切换控制，并提供 `pg_createsubscriber` 工具          | 更适合在线迁移、版本升级、数据同步和高可用场景 |
| 增量备份      | `pg_basebackup` 支持增量备份，并提供 `pg_combinebackup` 工具 | 大库备份的时间和存储成本可以进一步降低         |
| COPY 容错导入 | `COPY` 支持在导入时忽略错误行                                | CSV 或外部数据导入时更容易处理脏数据           |

从项目角度看，PostgreSQL 17 的重点不是单纯增加语法，而是继续强化生产环境中常见的大表维护、JSON 数据处理、批量导入、复制升级、备份恢复和性能分析能力。

### 常用使用场景

PostgreSQL 17 适合用于对数据一致性、查询能力和可维护性要求较高的业务系统。它可以覆盖从本地开发环境到生产级高并发服务的多类场景。

| 场景            | 说明                                              |
| --------------- | ------------------------------------------------- |
| 后台管理系统    | 用户、角色、菜单、权限、字典、配置等基础数据      |
| 订单交易系统    | 订单、支付流水、库存扣减、状态流转等强事务场景    |
| SaaS 多租户系统 | 通过租户字段、独立 Schema 或独立数据库隔离数据    |
| 内容与配置平台  | 使用普通字段配合 `jsonb` 存储动态配置和扩展属性   |
| 数据统计报表    | 使用聚合函数、窗口函数、物化视图完成统计分析      |
| 日志审计系统    | 存储操作日志、数据变更记录、接口请求记录          |
| 地理信息系统    | 结合 PostGIS 处理空间数据、距离计算和地图查询     |
| 搜索辅助场景    | 结合全文搜索、`pg_trgm`、GIN 索引实现基础搜索能力 |

不建议将 PostgreSQL 直接替代所有中间件。高频缓存优先使用 Redis；复杂全文检索优先考虑 Elasticsearch 或 OpenSearch；大文件存储优先使用对象存储；异步削峰优先使用 MQ；超大规模离线分析优先考虑数仓或湖仓系统。

### 项目开发中的职责边界

在项目开发中，PostgreSQL 负责保证数据的正确性、完整性、查询能力和可恢复性。应用程序负责业务流程编排、接口输入输出、权限上下文、外部系统调用和用户交互。

数据库侧建议承担以下职责：

| 职责         | 说明                                                   |
| ------------ | ------------------------------------------------------ |
| 表结构设计   | 设计字段类型、主键、外键、唯一约束、非空约束和检查约束 |
| 数据一致性   | 通过事务、约束、锁和隔离级别保证数据状态正确           |
| 查询性能     | 通过索引、执行计划和 SQL 优化提升查询效率              |
| 权限隔离     | 控制用户、角色、Schema、表、字段和函数权限             |
| 数据生命周期 | 配合分区、归档、备份、恢复和 VACUUM 管理数据           |
| 基础审计能力 | 通过审计字段、触发器或日志表记录关键变更               |
| 数据迁移     | 使用 Flyway、Liquibase 或脚本管理 DDL、DML 变更        |

应用侧建议承担以下职责：

| 职责       | 说明                                                        |
| ---------- | ----------------------------------------------------------- |
| 业务流程   | 处理订单流程、审批流、状态机和外部系统调用                  |
| 参数校验   | 在 Controller、DTO、Service 层完成格式和业务规则校验        |
| 事务边界   | 在 Service 层定义清晰事务范围，避免长事务                   |
| 缓存策略   | 决定哪些数据进入 Redis、本地缓存或其他缓存组件              |
| 异步处理   | 使用 MQ、任务调度或事件机制处理异步任务                     |
| SQL 管理   | 使用参数绑定、Mapper、Repository 或查询构造器，避免拼接 SQL |
| 连接池管理 | 使用 HikariCP 等连接池控制连接数、超时和泄漏检测            |

基本原则是：数据库负责“数据必须正确”，应用负责“业务如何流转”。强一致性的底线规则应尽量通过数据库约束兜底，复杂业务流程不要过度写入触发器或存储过程，避免后期排查困难。

## 环境安装与初始化

本章用于说明 PostgreSQL 17 在本地环境和 Docker 环境中的安装、启动、初始化和连接方式。开发环境建议固定主版本，避免使用 `latest` 或系统默认仓库导致版本不一致。

### 本地开发环境安装

本地安装适合需要长期调试数据库、使用本机服务、连接 IDE 或执行备份恢复命令的开发场景。Linux 环境建议使用 PostgreSQL 官方软件源安装指定主版本；macOS 可以使用 Homebrew、Postgres.app 或安装器；Windows 开发环境建议优先使用 Docker Desktop 或官方安装器。

Ubuntu 或 Debian 环境可以使用 PostgreSQL 官方 APT 仓库安装 PostgreSQL 17。

```bash
# 更新本地软件包索引
sudo apt update

# 安装 PostgreSQL 仓库管理工具
sudo apt install -y postgresql-common

# 添加 PostgreSQL 官方 APT 仓库
sudo /usr/share/postgresql-common/pgdg/apt.postgresql.org.sh

# 重新更新软件包索引
sudo apt update

# 安装 PostgreSQL 17 服务端和客户端
sudo apt install -y postgresql-17 postgresql-client-17

# 查看客户端版本
psql --version

# 查看 PostgreSQL 集群状态
pg_lsclusters
```

`postgresql-17` 是服务端包，`postgresql-client-17` 是客户端工具包，`pg_lsclusters` 用于查看 Debian 或 Ubuntu 系统中的 PostgreSQL 集群、端口、状态和数据目录。

macOS 使用 Homebrew 时，可以安装 PostgreSQL 17 版本包。

```bash
# 安装 PostgreSQL 17
brew install postgresql@17

# 启动 PostgreSQL 17 服务
brew services start postgresql@17

# 查看 psql 版本
psql --version
```

如果本机同时存在多个 PostgreSQL 主版本，需要明确 `psql` 的实际路径，避免客户端版本和服务端版本混淆。

### Docker 部署方式

Docker 部署适合本地开发、临时测试、CI 环境和学习实验。PostgreSQL 官方镜像支持通过环境变量初始化超级用户、密码和默认数据库，其中 `POSTGRES_PASSWORD` 是使用官方镜像时的重要初始化变量；初始化脚本可以放入 `/docker-entrypoint-initdb.d/`，并且只会在数据目录为空时执行。([Docker Documentation](https://docs.docker.com/guides/postgresql/advanced-configuration-and-initialization/?utm_source=chatgpt.com))

文件位置：`docker-compose.yml`

```yaml
services:
  postgres:
    image: postgres:17
    container_name: postgres17
    restart: unless-stopped
    ports:
      # 本地开发端口映射，生产环境不要直接暴露到公网
      - "5432:5432"
    environment:
      # 初始化超级用户
      POSTGRES_USER: postgres
      # 初始化超级用户密码，生产环境建议使用环境变量或密钥管理系统注入
      POSTGRES_PASSWORD: postgres123456
      # 初始化默认业务数据库
      POSTGRES_DB: app_db
      # 初始化参数，仅在首次创建空数据目录时生效
      POSTGRES_INITDB_ARGS: "--encoding=UTF8 --locale=C.UTF-8"
      # PostgreSQL 数据目录
      PGDATA: /var/lib/postgresql/data
    volumes:
      # 数据持久化目录
      - ./data/postgresql:/var/lib/postgresql/data
      # 初始化脚本目录，仅首次初始化空数据目录时执行
      - ./initdb:/docker-entrypoint-initdb.d
    healthcheck:
      test: ["CMD-SHELL", "pg_isready -U postgres -d app_db"]
      interval: 10s
      timeout: 5s
      retries: 5
```

启动、查看日志和连接数据库：

```bash
# 启动 PostgreSQL 17
docker compose up -d

# 查看容器状态
docker compose ps

# 查看启动日志
docker compose logs -f postgres

# 进入容器内 psql
docker exec -it postgres17 psql -U postgres -d app_db

# 从宿主机连接，前提是本机已安装 psql 客户端
psql -h 127.0.0.1 -p 5432 -U postgres -d app_db
```

`./data/postgresql` 是宿主机持久化目录，删除该目录会导致数据库数据丢失。`./initdb` 下的 `.sql`、`.sql.gz` 或 `.sh` 初始化脚本只会在首次创建空数据目录时执行，后续重启容器不会重复执行。

### 服务启动与停止

PostgreSQL 可以通过系统服务、PostgreSQL 工具或 Docker 命令管理。`pg_ctl` 是 PostgreSQL 官方提供的服务控制工具，可用于初始化、启动、停止、重启、重载配置和查看服务状态；停止时可以使用 `smart`、`fast`、`immediate` 等关闭模式。([PostgreSQL](https://www.postgresql.org/docs/17/app-pg-ctl.html?utm_source=chatgpt.com))

Ubuntu 或 Debian 使用包安装后，常见管理命令如下：

```bash
# 查看 PostgreSQL 集群列表
pg_lsclusters

# 启动 PostgreSQL 17 main 集群
sudo pg_ctlcluster 17 main start

# 停止 PostgreSQL 17 main 集群
sudo pg_ctlcluster 17 main stop

# 重启 PostgreSQL 17 main 集群
sudo pg_ctlcluster 17 main restart

# 重载配置，不中断数据库进程
sudo pg_ctlcluster 17 main reload

# 查看系统服务状态
sudo systemctl status postgresql
```

使用 `pg_ctl` 管理指定数据目录：

```bash
# 查看服务状态
pg_ctl -D /data/postgresql/17/main status

# 启动服务，并将日志写入指定文件
pg_ctl -D /data/postgresql/17/main -l /data/postgresql/17/server.log start

# 重载配置
pg_ctl -D /data/postgresql/17/main reload

# 快速停止服务
pg_ctl -D /data/postgresql/17/main stop -m fast

# 重启服务
pg_ctl -D /data/postgresql/17/main restart
```

Docker 环境中使用 Compose 管理服务：

```bash
# 停止容器，不删除数据
docker compose stop postgres

# 启动已存在容器
docker compose start postgres

# 重启容器
docker compose restart postgres

# 停止并删除容器，不删除宿主机挂载目录
docker compose down

# 停止并删除容器、网络和匿名卷，谨慎使用
docker compose down -v
```

生产环境不要随意使用 `docker compose down -v`，该命令可能删除匿名卷。对于使用宿主机目录挂载的数据，也需要确认实际数据目录不会被误删。

### 数据目录初始化

PostgreSQL 的数据目录也称为数据库集群目录，一个数据库集群可以包含多个数据库。`initdb` 会创建数据库集群所需目录、共享系统表，并创建 `postgres`、`template1` 和 `template0` 数据库。([PostgreSQL](https://www.postgresql.org/docs/17/app-initdb.html?utm_source=chatgpt.com))

手动初始化自定义数据目录：

```bash
# 创建数据目录，权限只允许 postgres 用户访问
sudo install -o postgres -g postgres -m 700 -d /data/postgresql/17/main

# 切换到 postgres 系统用户执行初始化
sudo -iu postgres /usr/lib/postgresql/17/bin/initdb \
  -D /data/postgresql/17/main \
  --encoding=UTF8 \
  --locale=C.UTF-8 \
  --data-checksums
```

`-D` 指定数据目录，`--encoding=UTF8` 指定默认编码，`--locale=C.UTF-8` 指定默认区域设置，`--data-checksums` 启用数据页校验。数据页校验需要在初始化时开启，不建议在不了解影响的情况下对生产环境随意变更。

Docker 环境中，可以通过初始化脚本创建 Schema、账号和基础权限。

文件位置：`initdb/01-init-schema.sql`

```sql
-- 创建业务 Schema
CREATE SCHEMA IF NOT EXISTS app;

-- 创建应用账号，生产环境应使用更强密码并通过密钥管理系统注入
CREATE USER app_user WITH PASSWORD 'app_user_123456';

-- 授权连接数据库
GRANT CONNECT ON DATABASE app_db TO app_user;

-- 授权使用 Schema
GRANT USAGE, CREATE ON SCHEMA app TO app_user;

-- 设置默认搜索路径
ALTER ROLE app_user IN DATABASE app_db SET search_path TO app, public;
```

初始化脚本适合放置开发环境所需的 Schema、测试账号、扩展插件和少量基础数据。生产环境更建议通过 Flyway 或 Liquibase 管理脚本版本，不要依赖容器初始化目录重复执行变更。

### 客户端工具选择

PostgreSQL 客户端工具用于连接数据库、执行 SQL、导入导出数据、查看对象结构和排查问题。开发人员至少应掌握 `psql` 的基础使用，同时可以根据习惯选择图形化工具提升日常开发效率。

| 工具         | 适用场景                   | 说明                                                |
| ------------ | -------------------------- | --------------------------------------------------- |
| `psql`       | 命令行开发、排查、脚本执行 | 官方命令行客户端，适合执行 SQL 和自动化脚本         |
| pgAdmin 4    | 图形化管理                 | 适合查看库表结构、执行 SQL、管理对象                |
| DBeaver      | 通用数据库客户端           | 适合同时管理 PostgreSQL、MySQL、Oracle 等多种数据库 |
| DataGrip     | IDE 型数据库工具           | 适合重度 SQL 开发、结构对比和代码提示               |
| `pg_dump`    | 逻辑备份                   | 导出数据库、Schema 或表数据                         |
| `pg_restore` | 逻辑恢复                   | 恢复自定义格式备份文件                              |
| JDBC         | Java 应用连接              | Spring Boot、MyBatis、JPA 等后端项目连接数据库      |

`psql` 常用连接和元命令如下：

```bash
# 使用 TCP 连接本地 PostgreSQL
psql -h 127.0.0.1 -p 5432 -U app_user -d app_db
```

连接后常用命令：

```sql
\l              -- 查看数据库列表
\c app_db       -- 切换数据库
\dn             -- 查看 Schema
\dt app.*       -- 查看 app Schema 下的表
\d+ app.user    -- 查看表结构详情
\du             -- 查看角色
\x              -- 切换扩展显示
\timing         -- 显示 SQL 执行耗时
\q              -- 退出 psql
```

`psql` 适合保留为所有环境的基础工具。图形化工具可以提升开发效率，但生产问题排查、自动化脚本和容器环境操作仍建议掌握命令行方式。

### 连接参数说明

连接参数决定客户端如何访问 PostgreSQL 服务。服务端常见连接配置包括监听地址、端口、最大连接数、认证方式和 SSL 设置。客户端常见连接信息包括主机、端口、数据库、用户名、密码、Schema、超时时间和应用名称。

| 参数              | 示例                                | 说明                               |
| ----------------- | ----------------------------------- | ---------------------------------- |
| `host`            | `127.0.0.1`                         | 数据库服务器地址                   |
| `port`            | `5432`                              | 数据库监听端口                     |
| `database`        | `app_db`                            | 要连接的数据库                     |
| `user`            | `app_user`                          | 数据库用户名                       |
| `password`        | `******`                            | 数据库密码                         |
| `sslmode`         | `disable`、`require`、`verify-full` | SSL 连接模式                       |
| `currentSchema`   | `app`                               | JDBC 常用参数，用于指定默认 Schema |
| `connectTimeout`  | `10`                                | 连接超时时间                       |
| `ApplicationName` | `order-service`                     | 应用名称，便于排查连接来源         |

`psql` 连接示例：

```bash
# 基础连接
psql -h 127.0.0.1 -p 5432 -U app_user -d app_db

# 使用环境变量传递密码，避免交互输入
PGPASSWORD='app_user_123456' psql \
  -h 127.0.0.1 \
  -p 5432 \
  -U app_user \
  -d app_db

# 查看当前 search_path
PGPASSWORD='app_user_123456' psql \
  -h 127.0.0.1 \
  -p 5432 \
  -U app_user \
  -d app_db \
  -c "SHOW search_path;"
```

Spring Boot 连接配置示例：

文件位置：`src/main/resources/application.yml`

```yaml
spring:
  datasource:
    # PostgreSQL JDBC 连接地址，currentSchema 指定默认 Schema
    url: jdbc:postgresql://127.0.0.1:5432/app_db?currentSchema=app&sslmode=disable&ApplicationName=demo-service
    # 数据库账号
    username: app_user
    # 数据库密码，生产环境建议使用环境变量或配置中心注入
    password: app_user_123456
    driver-class-name: org.postgresql.Driver
    hikari:
      # 连接池名称，便于日志和监控识别
      pool-name: demo-postgresql-pool
      # 最大连接数，应结合 PostgreSQL max_connections 和服务实例数设置
      maximum-pool-size: 10
      # 最小空闲连接数
      minimum-idle: 2
      # 获取连接超时时间
      connection-timeout: 30000
      # 空闲连接最大存活时间
      idle-timeout: 600000
      # 连接最大生命周期
      max-lifetime: 1800000
```

连接失败时优先检查以下内容：

| 检查项                 | 说明                                                         |
| ---------------------- | ------------------------------------------------------------ |
| 服务是否启动           | 使用 `pg_isready`、`systemctl status postgresql` 或 `docker compose ps` |
| 地址是否监听           | 检查 `listen_addresses` 是否允许远程连接                     |
| 端口是否正确           | 默认端口是 `5432`，Docker 环境还要检查端口映射               |
| 防火墙是否放行         | 云服务器安全组、本机防火墙都可能阻断连接                     |
| 账号密码是否正确       | 检查用户是否存在、密码是否正确                               |
| 数据库是否存在         | PostgreSQL 连接必须指定已存在的数据库                        |
| `pg_hba.conf` 是否允许 | 远程连接需要匹配正确的认证规则                               |
| Schema 是否正确        | 查询表不存在时，检查 `search_path` 或 `currentSchema`        |


## 数据库与用户管理

本章用于说明 PostgreSQL 中数据库、用户、角色和权限的基础管理方式。PostgreSQL 的权限体系以角色为核心，用户本质上也是一种带有 `LOGIN` 属性的角色，因此在项目中应优先按“角色分组、用户继承角色、权限授予角色”的方式组织权限。

### 数据库创建

数据库是 PostgreSQL 中的顶层数据容器。一个 PostgreSQL 实例可以包含多个数据库，不同数据库之间默认相互隔离，普通 SQL 查询不能直接跨数据库访问数据。

创建数据库前，建议先创建数据库所有者账号，再由该账号拥有业务数据库。

先创建数据库所有者，再创建业务数据库。

```sql
-- 创建数据库所有者账号
CREATE ROLE app_owner WITH LOGIN PASSWORD 'app_owner_123456';

-- 创建业务数据库
CREATE DATABASE app_db
    OWNER app_owner
    ENCODING 'UTF8'
    TEMPLATE template0
    LC_COLLATE 'C.UTF-8'
    LC_CTYPE 'C.UTF-8'
    CONNECTION LIMIT -1;
```

参数说明如下：

| 参数               | 说明                                                         |
| ------------------ | ------------------------------------------------------------ |
| `OWNER`            | 数据库所有者，通常设置为业务库管理员账号                     |
| `ENCODING`         | 数据库编码，项目中通常使用 `UTF8`                            |
| `TEMPLATE`         | 创建数据库使用的模板，指定字符集和排序规则时常用 `template0` |
| `LC_COLLATE`       | 字符串排序规则                                               |
| `LC_CTYPE`         | 字符分类规则                                                 |
| `CONNECTION LIMIT` | 数据库连接数限制，`-1` 表示不单独限制                        |

创建完成后可以使用 `psql` 查看数据库列表。

```sql
-- 查看数据库列表
\l
```

也可以通过 SQL 查询数据库信息。

```sql
-- 查询当前实例中的数据库
SELECT
    datname,
    pg_catalog.pg_get_userbyid(datdba) AS owner,
    encoding,
    datcollate,
    datctype,
    datconnlimit
FROM pg_database
ORDER BY datname;
```

开发环境可以直接创建业务库；生产环境建议通过初始化脚本、发布脚本或数据库变更平台执行，避免手工操作不可追溯。

### 数据库删除

删除数据库会永久移除该数据库中的所有 Schema、表、索引、函数、视图、数据和权限配置。执行删除前必须确认备份已经完成，并且当前连接不能连接在待删除数据库上。

删除数据库前，建议先连接到 `postgres` 数据库。

```bash
# 连接到默认 postgres 数据库，避免占用待删除数据库
psql -h 127.0.0.1 -p 5432 -U postgres -d postgres
```

删除数据库的常用 SQL 如下：

```sql
-- 删除没有活动连接的数据库
DROP DATABASE IF EXISTS app_db;
```

如果数据库存在活动连接，可以先查看连接，再按需终止连接。

```sql
-- 查看连接到 app_db 的会话
SELECT
    pid,
    usename,
    application_name,
    client_addr,
    state,
    query
FROM pg_stat_activity
WHERE datname = 'app_db';

-- 终止 app_db 的其他连接
SELECT pg_terminate_backend(pid)
FROM pg_stat_activity
WHERE datname = 'app_db'
  AND pid <> pg_backend_pid();

-- 删除数据库
DROP DATABASE IF EXISTS app_db;
```

PostgreSQL 也支持使用 `WITH (FORCE)` 强制断开连接并删除数据库。

```sql
-- 强制删除数据库，谨慎使用
DROP DATABASE IF EXISTS app_db WITH (FORCE);
```

生产环境删除数据库前至少确认三件事：备份文件可用、无业务流量访问、删除对象名称正确。不要在未确认当前连接环境的情况下直接执行 `DROP DATABASE`。

### 数据库切换

PostgreSQL 不能像 MySQL 一样通过 `USE database_name;` 在同一个连接中切换数据库。切换数据库本质上是重新建立连接。

在 `psql` 中可以使用 `\c` 切换数据库。

```sql
-- 切换到 app_db 数据库
\c app_db

-- 使用指定用户切换到 app_db
\c app_db app_user

-- 查看当前连接信息
\conninfo
```

从命令行连接指定数据库。

```bash
# 连接指定数据库
psql -h 127.0.0.1 -p 5432 -U app_user -d app_db
```

在应用程序中，数据库由 JDBC URL 决定。例如 Spring Boot 中连接 `app_db`：

```yaml
spring:
  datasource:
    # app_db 表示连接的目标数据库
    url: jdbc:postgresql://127.0.0.1:5432/app_db?currentSchema=app
    username: app_user
    password: app_user_123456
```

如果项目需要访问多个数据库，建议在应用层配置多数据源，而不是尝试在单个连接中动态切换数据库。对于同一业务系统内的模块隔离，优先考虑多 Schema，而不是拆成多个数据库。

### 用户创建

PostgreSQL 中用户是可以登录的角色。`CREATE USER` 是 `CREATE ROLE ... LOGIN` 的简化写法。实际项目中建议使用 `CREATE ROLE`，语义更统一。

创建普通应用账号。

```sql
-- 创建可登录的应用账号
CREATE ROLE app_user WITH
    LOGIN
    PASSWORD 'app_user_123456'
    NOSUPERUSER
    NOCREATEDB
    NOCREATEROLE
    NOREPLICATION;
```

常用属性说明如下：

| 属性            | 说明                               |
| --------------- | ---------------------------------- |
| `LOGIN`         | 允许该角色登录数据库               |
| `PASSWORD`      | 设置登录密码                       |
| `NOSUPERUSER`   | 非超级用户，应用账号必须使用该配置 |
| `NOCREATEDB`    | 不允许创建数据库                   |
| `NOCREATEROLE`  | 不允许创建其他角色                 |
| `NOREPLICATION` | 不允许作为复制账号使用             |

查看用户和角色。

```sql
-- psql 元命令，查看角色列表
\du

-- SQL 查询角色信息
SELECT
    rolname,
    rolcanlogin,
    rolsuper,
    rolcreatedb,
    rolcreaterole,
    rolreplication
FROM pg_roles
ORDER BY rolname;
```

项目中不建议应用程序使用 `postgres` 超级用户连接数据库。推荐为每个系统、每个环境创建独立账号，例如 `order_dev_user`、`order_test_user`、`order_prod_user`。

### 用户密码管理

用户密码可以在创建用户时指定，也可以通过 `ALTER ROLE` 修改。生产环境应避免在脚本中明文硬编码密码，优先通过环境变量、配置中心、密钥管理系统或部署平台注入。

修改用户密码。

```sql
-- 修改用户密码
ALTER ROLE app_user WITH PASSWORD 'new_app_user_123456';
```

设置密码有效期。

```sql
-- 设置密码有效期
ALTER ROLE app_user VALID UNTIL '2026-12-31 23:59:59';

-- 设置密码永不过期
ALTER ROLE app_user VALID UNTIL 'infinity';
```

锁定或解锁用户登录能力。

```sql
-- 禁止用户登录
ALTER ROLE app_user NOLOGIN;

-- 恢复用户登录
ALTER ROLE app_user LOGIN;
```

查看密码有效期和登录能力。

```sql
SELECT
    rolname,
    rolcanlogin,
    rolvaliduntil
FROM pg_roles
WHERE rolname = 'app_user';
```

密码管理建议如下：

| 建议           | 说明                                   |
| -------------- | -------------------------------------- |
| 不使用超级用户 | 应用程序不要使用 `postgres` 账号       |
| 不共用账号     | 不同系统、不同环境使用独立账号         |
| 不明文提交     | 密码不要提交到 Git 仓库                |
| 定期轮换       | 生产账号应具备密码轮换机制             |
| 最小权限       | 密码泄露时，账号权限越小，影响范围越小 |

### 用户权限管理

PostgreSQL 权限通常分为数据库权限、Schema 权限、对象权限和默认权限。项目中不要直接给用户授权，建议给角色授权，再把用户加入角色。

创建读写角色和只读角色。

```sql
-- 读写角色，不允许直接登录
CREATE ROLE app_rw NOLOGIN;

-- 只读角色，不允许直接登录
CREATE ROLE app_ro NOLOGIN;
```

授予数据库连接权限。

```sql
-- 允许角色连接 app_db 数据库
GRANT CONNECT ON DATABASE app_db TO app_rw;
GRANT CONNECT ON DATABASE app_db TO app_ro;
```

授予 Schema 使用权限。

```sql
-- 允许角色使用 app Schema
GRANT USAGE ON SCHEMA app TO app_rw;
GRANT USAGE ON SCHEMA app TO app_ro;

-- 允许读写角色在 app Schema 下创建对象
GRANT CREATE ON SCHEMA app TO app_rw;
```

授予表权限。

```sql
-- 读写角色拥有表的查询、写入、更新、删除权限
GRANT SELECT, INSERT, UPDATE, DELETE
ON ALL TABLES IN SCHEMA app
TO app_rw;

-- 只读角色只允许查询
GRANT SELECT
ON ALL TABLES IN SCHEMA app
TO app_ro;
```

授予序列权限。

```sql
-- 读写角色允许使用自增序列
GRANT USAGE, SELECT, UPDATE
ON ALL SEQUENCES IN SCHEMA app
TO app_rw;

-- 只读角色通常只需要查看序列
GRANT SELECT
ON ALL SEQUENCES IN SCHEMA app
TO app_ro;
```

将用户加入角色。

```sql
-- 应用写账号继承读写角色权限
GRANT app_rw TO app_user;

-- 报表账号继承只读角色权限
GRANT app_ro TO report_user;
```

回收权限使用 `REVOKE`。

```sql
-- 回收 app_user 的读写角色
REVOKE app_rw FROM app_user;

-- 回收角色对表的删除权限
REVOKE DELETE ON ALL TABLES IN SCHEMA app FROM app_rw;
```

权限设计建议如下：

| 账号类型     | 推荐权限                                   |
| ------------ | ------------------------------------------ |
| 应用读写账号 | `CONNECT`、`USAGE`、表 DML、序列使用权限   |
| 应用只读账号 | `CONNECT`、`USAGE`、表 `SELECT`            |
| 迁移脚本账号 | DDL 权限、对象创建权限、必要的数据变更权限 |
| 运维账号     | 按需授权，不建议长期使用超级用户           |
| 报表账号     | 只读权限，必要时只开放指定视图             |

### 角色管理

角色用于组织权限。PostgreSQL 中用户、用户组、权限组都属于角色，只是是否具有 `LOGIN` 属性不同。项目中推荐使用“权限角色 + 登录用户”的结构。

典型角色设计如下：

| 角色          | 是否允许登录 | 用途                                    |
| ------------- | ------------ | --------------------------------------- |
| `app_owner`   | 是           | 数据库和对象所有者，执行 DDL 或迁移脚本 |
| `app_rw`      | 否           | 应用读写权限组                          |
| `app_ro`      | 否           | 报表或查询只读权限组                    |
| `app_user`    | 是           | 应用程序连接账号                        |
| `report_user` | 是           | 报表或 BI 查询账号                      |

创建角色结构。

```sql
-- 数据库对象所有者
CREATE ROLE app_owner WITH LOGIN PASSWORD 'app_owner_123456';

-- 读写权限组
CREATE ROLE app_rw NOLOGIN;

-- 只读权限组
CREATE ROLE app_ro NOLOGIN;

-- 应用登录账号
CREATE ROLE app_user WITH LOGIN PASSWORD 'app_user_123456';

-- 报表登录账号
CREATE ROLE report_user WITH LOGIN PASSWORD 'report_user_123456';
```

建立角色继承关系。

```sql
-- 应用账号继承读写权限
GRANT app_rw TO app_user;

-- 报表账号继承只读权限
GRANT app_ro TO report_user;
```

查看角色继承关系。

```sql
SELECT
    member_role.rolname AS member,
    parent_role.rolname AS granted_role
FROM pg_auth_members m
JOIN pg_roles member_role ON member_role.oid = m.member
JOIN pg_roles parent_role ON parent_role.oid = m.roleid
ORDER BY member_role.rolname, parent_role.rolname;
```

删除角色前，需要先回收权限和对象依赖。

```sql
-- 回收角色继承关系
REVOKE app_rw FROM app_user;

-- 删除登录用户
DROP ROLE IF EXISTS app_user;

-- 删除权限角色
DROP ROLE IF EXISTS app_rw;
```

如果角色拥有数据库对象，需要先转移对象所有权或删除对象，否则无法直接删除角色。

```sql
-- 将 app_user 拥有的对象转移给 app_owner
REASSIGN OWNED BY app_user TO app_owner;

-- 删除 app_user 相关权限
DROP OWNED BY app_user;

-- 删除角色
DROP ROLE app_user;
```

角色管理的核心原则是：权限授予角色，用户继承角色。这样当人员、系统账号或环境变化时，只需要调整角色继承关系，不需要重复维护大量对象权限。

### 默认权限配置

`GRANT ON ALL TABLES IN SCHEMA` 只会影响当前已经存在的对象，不会自动影响未来新建的表、序列和函数。要让未来创建的对象自动获得权限，需要配置默认权限。

默认权限通常由对象创建者执行。例如表由 `app_owner` 创建，就应对 `app_owner` 配置默认权限。

为未来新建表配置默认权限。

```sql
-- app_owner 未来在 app Schema 下创建的表，自动授予 app_rw 读写权限
ALTER DEFAULT PRIVILEGES FOR ROLE app_owner IN SCHEMA app
GRANT SELECT, INSERT, UPDATE, DELETE ON TABLES TO app_rw;

-- app_owner 未来在 app Schema 下创建的表，自动授予 app_ro 查询权限
ALTER DEFAULT PRIVILEGES FOR ROLE app_owner IN SCHEMA app
GRANT SELECT ON TABLES TO app_ro;
```

为未来新建序列配置默认权限。

```sql
-- app_rw 自动获得未来序列的使用权限
ALTER DEFAULT PRIVILEGES FOR ROLE app_owner IN SCHEMA app
GRANT USAGE, SELECT, UPDATE ON SEQUENCES TO app_rw;

-- app_ro 自动获得未来序列的查询权限
ALTER DEFAULT PRIVILEGES FOR ROLE app_owner IN SCHEMA app
GRANT SELECT ON SEQUENCES TO app_ro;
```

为未来新建函数配置默认权限。

```sql
-- app_rw 自动获得未来函数执行权限
ALTER DEFAULT PRIVILEGES FOR ROLE app_owner IN SCHEMA app
GRANT EXECUTE ON FUNCTIONS TO app_rw;

-- app_ro 自动获得未来函数执行权限
ALTER DEFAULT PRIVILEGES FOR ROLE app_owner IN SCHEMA app
GRANT EXECUTE ON FUNCTIONS TO app_ro;
```

查看默认权限。

```sql
SELECT
    n.nspname AS schema_name,
    r.rolname AS owner_role,
    d.defaclobjtype AS object_type,
    d.defaclacl AS default_acl
FROM pg_default_acl d
JOIN pg_roles r ON r.oid = d.defaclrole
LEFT JOIN pg_namespace n ON n.oid = d.defaclnamespace
ORDER BY schema_name, owner_role, object_type;
```

默认权限配置时需要注意：

| 注意点             | 说明                                                       |
| ------------------ | ---------------------------------------------------------- |
| 只影响未来对象     | 已存在对象仍需要单独执行 `GRANT ON ALL ...`                |
| 和创建者有关       | 谁创建对象，就要针对谁配置默认权限                         |
| 和 Schema 有关     | 推荐使用 `IN SCHEMA app` 限定范围                          |
| 不替代 Schema 权限 | 表有权限不代表 Schema 可访问，仍需 `GRANT USAGE ON SCHEMA` |
| 迁移账号要固定     | Flyway、Liquibase 等迁移账号应作为统一对象创建者           |

## Schema 管理

Schema 是数据库内部的命名空间，用于组织表、视图、函数、序列和类型等对象。同一个数据库中可以有多个 Schema，不同 Schema 中可以存在同名表。项目中合理使用 Schema，可以提升对象组织能力、权限隔离能力和多模块管理能力。

### Schema 创建

Schema 用于在同一个数据库内划分对象空间。常见做法是为业务系统创建独立 Schema，例如 `app`、`auth`、`order_service`、`report` 等。

创建业务 Schema。

```sql
-- 创建 app Schema，并指定所有者
CREATE SCHEMA IF NOT EXISTS app AUTHORIZATION app_owner;
```

也可以先创建 Schema，再修改所有者。

```sql
-- 创建 Schema
CREATE SCHEMA IF NOT EXISTS app;

-- 修改 Schema 所有者
ALTER SCHEMA app OWNER TO app_owner;
```

查看 Schema 列表。

```sql
-- psql 元命令
\dn

-- SQL 查询 Schema
SELECT
    schema_name,
    schema_owner
FROM information_schema.schemata
ORDER BY schema_name;
```

创建表时可以显式指定 Schema。

```sql
-- 在 app Schema 下创建用户表
CREATE TABLE app.sys_user (
    id BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    username VARCHAR(64) NOT NULL,
    nickname VARCHAR(64),
    enabled BOOLEAN NOT NULL DEFAULT TRUE,
    create_time TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);
```

如果不显式指定 Schema，PostgreSQL 会根据当前 `search_path` 决定对象创建位置。开发规范中建议 DDL 脚本显式带上 Schema，避免因连接参数不同导致对象创建到错误位置。

### Schema 删除

删除 Schema 会影响该 Schema 下的表、视图、序列、函数、类型等对象。删除前应确认对象依赖和备份情况。

删除空 Schema。

```sql
-- 只删除空 Schema
DROP SCHEMA IF EXISTS app;
```

如果 Schema 下存在对象，直接删除会失败。可以使用 `CASCADE` 级联删除，但生产环境需要非常谨慎。

```sql
-- 删除 Schema 及其下所有对象，谨慎使用
DROP SCHEMA IF EXISTS app CASCADE;
```

查看 Schema 下的对象。

```sql
SELECT
    n.nspname AS schema_name,
    c.relname AS object_name,
    c.relkind AS object_type
FROM pg_class c
JOIN pg_namespace n ON n.oid = c.relnamespace
WHERE n.nspname = 'app'
ORDER BY c.relkind, c.relname;
```

`relkind` 常见值如下：

| 值   | 说明     |
| ---- | -------- |
| `r`  | 普通表   |
| `i`  | 索引     |
| `S`  | 序列     |
| `v`  | 视图     |
| `m`  | 物化视图 |
| `p`  | 分区表   |

生产环境删除 Schema 前，建议先导出对象清单，再确认没有业务程序连接、定时任务、视图、函数或外部报表依赖这些对象。

### Schema 搜索路径

`search_path` 决定 PostgreSQL 在未指定 Schema 时如何查找对象。例如查询 `SELECT * FROM sys_user;` 时，数据库会按 `search_path` 中的顺序查找 `sys_user` 表。

查看当前搜索路径。

```sql
-- 查看当前会话的 Schema 搜索路径
SHOW search_path;
```

临时设置当前会话的搜索路径。

```sql
-- 仅当前会话生效
SET search_path TO app, public;
```

为指定用户在指定数据库下设置默认搜索路径。

```sql
-- app_user 连接 app_db 时默认使用 app Schema
ALTER ROLE app_user IN DATABASE app_db SET search_path TO app, public;
```

为当前数据库设置默认搜索路径。

```sql
-- 当前数据库默认搜索路径
ALTER DATABASE app_db SET search_path TO app, public;
```

JDBC 中也可以通过 `currentSchema` 指定默认 Schema。

```yaml
spring:
  datasource:
    # currentSchema 指定应用默认访问的 Schema
    url: jdbc:postgresql://127.0.0.1:5432/app_db?currentSchema=app
    username: app_user
    password: app_user_123456
```

项目中建议遵循以下规则：

| 规则                 | 说明                                   |
| -------------------- | -------------------------------------- |
| DDL 显式写 Schema    | 例如 `CREATE TABLE app.sys_user`       |
| 应用连接指定 Schema  | JDBC 使用 `currentSchema=app`          |
| 查询尽量明确对象来源 | 复杂 SQL 建议写 `app.table_name`       |
| 不滥用 `public`      | 不建议把业务表全部放入 `public`        |
| 避免搜索路径过长     | 搜索路径越复杂，越容易出现对象命名冲突 |

### Schema 权限控制

Schema 权限主要包括 `USAGE` 和 `CREATE`。`USAGE` 表示可以访问 Schema 中的对象名称，`CREATE` 表示可以在 Schema 下创建对象。即使用户拥有表权限，如果没有 Schema 的 `USAGE` 权限，也无法正常访问该 Schema 下的表。

授予 Schema 使用权限。

```sql
-- 允许读写角色使用 app Schema
GRANT USAGE ON SCHEMA app TO app_rw;

-- 允许只读角色使用 app Schema
GRANT USAGE ON SCHEMA app TO app_ro;
```

授予 Schema 建对象权限。

```sql
-- 允许 app_rw 在 app Schema 下创建对象
GRANT CREATE ON SCHEMA app TO app_rw;
```

回收 Schema 权限。

```sql
-- 回收只读角色对 app Schema 的使用权限
REVOKE USAGE ON SCHEMA app FROM app_ro;

-- 回收读写角色在 app Schema 下创建对象的权限
REVOKE CREATE ON SCHEMA app FROM app_rw;
```

限制 `public` Schema 的默认权限。

```sql
-- 回收所有普通用户在 public Schema 下创建对象的权限
REVOKE CREATE ON SCHEMA public FROM PUBLIC;

-- 可选：回收 public Schema 的使用权限，需确认不会影响现有对象访问
REVOKE USAGE ON SCHEMA public FROM PUBLIC;
```

为应用角色配置完整 Schema 和对象权限。

```sql
-- Schema 使用权限
GRANT USAGE ON SCHEMA app TO app_rw;
GRANT USAGE ON SCHEMA app TO app_ro;

-- 表权限
GRANT SELECT, INSERT, UPDATE, DELETE ON ALL TABLES IN SCHEMA app TO app_rw;
GRANT SELECT ON ALL TABLES IN SCHEMA app TO app_ro;

-- 序列权限
GRANT USAGE, SELECT, UPDATE ON ALL SEQUENCES IN SCHEMA app TO app_rw;
GRANT SELECT ON ALL SEQUENCES IN SCHEMA app TO app_ro;

-- 函数权限
GRANT EXECUTE ON ALL FUNCTIONS IN SCHEMA app TO app_rw;
GRANT EXECUTE ON ALL FUNCTIONS IN SCHEMA app TO app_ro;
```

Schema 权限和对象权限是两层控制。常见问题是已经给了表的 `SELECT` 权限，但查询仍然提示权限不足，这时需要检查是否缺少 `GRANT USAGE ON SCHEMA`。

### 多 Schema 项目组织方式

多 Schema 适合在同一个数据库中按业务域、模块、租户或权限边界组织对象。它比多数据库更轻量，也更方便在同一个连接中进行事务和关联查询。

常见组织方式如下：

| 组织方式       | 示例                                 | 适用场景                   |
| -------------- | ------------------------------------ | -------------------------- |
| 按业务域划分   | `auth`、`order`、`payment`、`report` | 中大型单体系统或模块化系统 |
| 按应用划分     | `admin_app`、`open_api`、`job`       | 多个应用共享同一数据库实例 |
| 按权限边界划分 | `core`、`audit`、`readonly`          | 数据权限隔离要求较高的系统 |
| 按租户划分     | `tenant_1001`、`tenant_1002`         | 租户数量有限且隔离要求较高 |
| 按数据层次划分 | `ods`、`dwd`、`ads`                  | 数据处理、报表、分析类系统 |

按业务域划分 Schema 的示例。

```sql
-- 认证权限模块
CREATE SCHEMA IF NOT EXISTS auth AUTHORIZATION app_owner;

-- 订单模块
CREATE SCHEMA IF NOT EXISTS orders AUTHORIZATION app_owner;

-- 支付模块
CREATE SCHEMA IF NOT EXISTS payment AUTHORIZATION app_owner;

-- 报表模块
CREATE SCHEMA IF NOT EXISTS report AUTHORIZATION app_owner;
```

为不同 Schema 授权。

```sql
-- 应用读写角色访问核心业务 Schema
GRANT USAGE ON SCHEMA auth, orders, payment TO app_rw;
GRANT SELECT, INSERT, UPDATE, DELETE ON ALL TABLES IN SCHEMA auth, orders, payment TO app_rw;

-- 报表只读角色访问报表 Schema
GRANT USAGE ON SCHEMA report TO app_ro;
GRANT SELECT ON ALL TABLES IN SCHEMA report TO app_ro;
```

按模块访问时，建议 SQL 显式带上 Schema。

```sql
-- 查询订单及其支付记录
SELECT
    o.id,
    o.order_no,
    o.total_amount,
    p.pay_status,
    p.pay_time
FROM orders.order_info o
LEFT JOIN payment.pay_record p ON p.order_id = o.id
WHERE o.order_no = 'PO202605090001';
```

多 Schema 设计建议如下：

| 建议                    | 说明                                                         |
| ----------------------- | ------------------------------------------------------------ |
| 不要过度拆分            | 小型系统使用一个业务 Schema 即可                             |
| 避免使用保留字          | 不建议使用 `user`、`order` 等容易冲突的名称，可用 `sys_user`、`orders` |
| DDL 显式指定 Schema     | 避免对象被创建到 `public`                                    |
| 权限按 Schema 分组      | 不同模块或账号只授予必要 Schema 权限                         |
| 统一迁移账号            | 避免默认权限因对象创建者不同而失效                           |
| 跨 Schema 查询要清晰    | SQL 中显式写 `schema.table`，提升可读性                      |
| 多租户谨慎使用多 Schema | 租户数量很多时，多 Schema 会增加迁移和运维复杂度             |

对于普通 Spring Boot 单体项目，推荐使用一个业务 Schema，例如 `app`。对于模块边界清晰、报表隔离明显或权限要求较高的系统，可以使用 `auth`、`business`、`report`、`audit` 等多个 Schema。对于租户数量较多的 SaaS 系统，通常优先考虑租户字段隔离或独立数据库方案，多 Schema 租户隔离需要谨慎评估迁移、备份和权限维护成本。


## 数据类型

本章用于说明 PostgreSQL 常用数据类型的选择方式。字段类型设计会直接影响数据准确性、索引效率、存储空间、SQL 可读性和后续扩展能力。项目开发中不建议随意使用“大而全”的类型，应根据业务含义选择合适类型。

### 数值类型

数值类型用于存储整数、小数、金额、数量、比例、排序号等数据。设计时要优先考虑取值范围、是否需要精确计算、是否参与索引和是否涉及金额。

常用数值类型如下：

| 类型               | 存储         | 适用场景                  | 说明                                          |
| ------------------ | ------------ | ------------------------- | --------------------------------------------- |
| `smallint`         | 2 字节       | 状态值、小范围数字        | 范围较小，较少作为业务主字段                  |
| `integer` / `int`  | 4 字节       | 普通计数、排序、数量      | 常用整数类型                                  |
| `bigint`           | 8 字节       | 主键、雪花 ID、大数量统计 | 推荐用于业务主键和大数据量计数                |
| `numeric(p, s)`    | 可变         | 金额、费率、精确小数      | 精确计算，适合财务类字段                      |
| `decimal(p, s)`    | 可变         | 同 `numeric`              | `decimal` 是标准 SQL 写法，本质接近 `numeric` |
| `real`             | 4 字节       | 浮点近似值                | 不适合金额                                    |
| `double precision` | 8 字节       | 科学计算、地理坐标近似值  | 不适合金额                                    |
| `smallserial`      | 2 字节       | 小范围自增                | 旧式自增类型                                  |
| `serial`           | 4 字节       | 普通自增                  | 旧式自增类型                                  |
| `bigserial`        | 8 字节       | 大范围自增                | 旧式自增类型                                  |
| `identity`         | 依赖字段类型 | 标准自增主键              | PostgreSQL 10+ 推荐方式                       |

数值字段示例：

```sql
CREATE TABLE app.product_price (
    id BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    product_name VARCHAR(128) NOT NULL,
    stock_count INTEGER NOT NULL DEFAULT 0,
    sale_count BIGINT NOT NULL DEFAULT 0,
    sale_price NUMERIC(12, 2) NOT NULL,
    discount_rate NUMERIC(5, 4) NOT NULL DEFAULT 1.0000,
    sort_order INTEGER NOT NULL DEFAULT 0
);
```

金额字段建议使用 `numeric(精度, 小数位)`，例如 `numeric(12, 2)` 表示最多 12 位数字，其中 2 位小数。不要使用 `real` 或 `double precision` 存储金额，因为浮点数是近似值，可能产生精度误差。

主键字段如果使用数据库自增，推荐使用 `BIGINT GENERATED ALWAYS AS IDENTITY`。如果项目使用分布式 ID，可以使用 `BIGINT` 存储雪花 ID，或者使用 `UUID` 类型存储 UUID。

### 字符串类型

字符串类型用于存储名称、编码、手机号、邮箱、描述、备注、JSON 文本、地址等字符数据。PostgreSQL 中常用字符串类型主要是 `varchar(n)`、`text` 和 `char(n)`。

常用字符串类型如下：

| 类型                   | 适用场景                                     | 说明                                    |
| ---------------------- | -------------------------------------------- | --------------------------------------- |
| `varchar(n)`           | 用户名、手机号、编码、标题等有长度限制的字段 | 推荐用于大多数业务字符串                |
| `character varying(n)` | 同 `varchar(n)`                              | 完整写法，实际项目中一般写 `varchar(n)` |
| `text`                 | 文章内容、备注、日志内容、大段文本           | 不限制长度，适合长文本                  |
| `char(n)`              | 固定长度编码                                 | 不常用，容易产生空格填充问题            |

字符串字段示例：

```sql
CREATE TABLE app.sys_user (
    id BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    username VARCHAR(64) NOT NULL,
    nickname VARCHAR(64),
    phone VARCHAR(20),
    email VARCHAR(128),
    avatar_url VARCHAR(512),
    remark TEXT
);
```

常见字段长度建议如下：

| 字段     | 推荐类型                       |
| -------- | ------------------------------ |
| 用户名   | `varchar(64)`                  |
| 昵称     | `varchar(64)`                  |
| 手机号   | `varchar(20)`                  |
| 邮箱     | `varchar(128)`                 |
| URL      | `varchar(512)` 或 `text`       |
| 业务编码 | `varchar(32)`、`varchar(64)`   |
| 标题     | `varchar(128)`、`varchar(255)` |
| 备注     | `text`                         |
| 请求报文 | `text` 或 `jsonb`              |

项目中不建议无脑使用 `varchar(255)`。如果字段有明确业务长度，应按业务含义设置，例如手机号使用 `varchar(20)`，订单号使用 `varchar(64)`，名称使用 `varchar(128)`。

### 日期时间类型

日期时间类型用于存储创建时间、更新时间、业务日期、过期时间、统计周期、时间范围等数据。时间字段设计时要明确是否需要时区、是否只存日期、是否用于排序和区间查询。

常用日期时间类型如下：

| 类型                                       | 适用场景               | 说明                            |
| ------------------------------------------ | ---------------------- | ------------------------------- |
| `date`                                     | 生日、账单日、业务日期 | 只包含年月日                    |
| `time`                                     | 每日固定时间           | 只包含时分秒                    |
| `timestamp`                                | 本地时间、业务时间     | 不包含时区                      |
| `timestamp with time zone` / `timestamptz` | 绝对时间点、跨时区系统 | 包含时区语义，推荐用于全局系统  |
| `interval`                                 | 时间间隔               | 表示持续时间，例如 3 天、2 小时 |

审计字段示例：

```sql
CREATE TABLE app.audit_demo (
    id BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    business_date DATE NOT NULL,
    start_time TIMESTAMP NOT NULL,
    expire_time TIMESTAMP,
    create_time TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_time TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);
```

如果系统涉及跨时区用户、国际化业务、全球部署或多地域服务，建议使用 `TIMESTAMPTZ` 存储绝对时间点：

```sql
CREATE TABLE app.login_record (
    id BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    user_id BIGINT NOT NULL,
    login_ip VARCHAR(64),
    login_time TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP
);
```

常用时间函数如下：

```sql
-- 当前事务开始时间
SELECT CURRENT_TIMESTAMP;

-- 当前日期
SELECT CURRENT_DATE;

-- 当前时间
SELECT CURRENT_TIME;

-- 当前真实时钟时间
SELECT clock_timestamp();

-- 日期加 7 天
SELECT CURRENT_DATE + INTERVAL '7 days';

-- 按天截断时间
SELECT date_trunc('day', CURRENT_TIMESTAMP);
```

项目中建议统一约定时间类型。常见选择是：业务日期使用 `date`，创建时间和更新时间使用 `timestamp` 或 `timestamptz`，跨时区系统优先使用 `timestamptz`。

### 布尔类型

布尔类型用于存储真假值，例如是否启用、是否删除、是否默认、是否成功等。PostgreSQL 使用 `boolean` 类型，值可以是 `true`、`false` 或 `null`。

布尔字段示例：

```sql
CREATE TABLE app.sys_config (
    id BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    config_key VARCHAR(128) NOT NULL,
    config_value TEXT,
    enabled BOOLEAN NOT NULL DEFAULT TRUE,
    deleted BOOLEAN NOT NULL DEFAULT FALSE
);
```

查询布尔字段时，建议直接使用布尔表达式：

```sql
-- 查询启用且未删除的数据
SELECT *
FROM app.sys_config
WHERE enabled
  AND NOT deleted;
```

不建议使用 `0`、`1` 代替布尔值。如果字段只有真假两种状态，应优先使用 `boolean`，而不是 `smallint` 或 `varchar`。

### JSON 与 JSONB 类型

PostgreSQL 支持 `json` 和 `jsonb` 两种 JSON 类型。`json` 保存原始文本格式，`jsonb` 保存二进制解析后的结构。项目中大多数场景推荐使用 `jsonb`，因为它更适合查询、索引和字段提取。

`json` 与 `jsonb` 区别如下：

| 类型    | 特点                                   | 适用场景                               |
| ------- | -------------------------------------- | -------------------------------------- |
| `json`  | 保留原始文本格式，写入时校验 JSON 格式 | 需要保留原始 JSON 文本格式             |
| `jsonb` | 二进制存储，支持索引和高效查询         | 动态属性、扩展字段、接口报文、事件内容 |

JSONB 字段示例：

```sql
CREATE TABLE app.event_log (
    id BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    event_type VARCHAR(64) NOT NULL,
    event_data JSONB NOT NULL,
    create_time TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);
```

插入 JSONB 数据：

```sql
INSERT INTO app.event_log (event_type, event_data)
VALUES (
    'USER_LOGIN',
    '{
        "userId": 10001,
        "username": "admin",
        "clientIp": "127.0.0.1",
        "success": true
    }'::jsonb
);
```

查询 JSONB 字段：

```sql
-- 提取 JSON 字段文本
SELECT
    event_data ->> 'username' AS username,
    event_data ->> 'clientIp' AS client_ip
FROM app.event_log;

-- 按 JSON 字段过滤
SELECT *
FROM app.event_log
WHERE event_data ->> 'username' = 'admin';

-- 判断 JSON 是否包含指定结构
SELECT *
FROM app.event_log
WHERE event_data @> '{"success": true}'::jsonb;
```

如果 JSONB 字段需要频繁查询，应考虑创建 GIN 索引或表达式索引：

```sql
-- JSONB 通用 GIN 索引
CREATE INDEX idx_event_log_event_data_gin
ON app.event_log
USING GIN (event_data);

-- 针对 username 字段创建表达式索引
CREATE INDEX idx_event_log_username
ON app.event_log ((event_data ->> 'username'));
```

JSONB 适合存储扩展属性，但不应滥用。核心查询字段、关联字段、统计字段应优先设计为普通列，否则会降低数据约束能力和查询可维护性。

### UUID 类型

UUID 类型用于存储全局唯一标识。PostgreSQL 原生支持 `uuid` 类型，适合用于对外暴露 ID、分布式系统标识、接口幂等键、文件标识、事件标识等场景。

UUID 字段示例：

```sql
CREATE TABLE app.file_info (
    id BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    file_uuid UUID NOT NULL,
    file_name VARCHAR(255) NOT NULL,
    file_size BIGINT NOT NULL,
    create_time TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uk_file_info_file_uuid UNIQUE (file_uuid)
);
```

使用 `gen_random_uuid()` 生成 UUID 需要启用 `pgcrypto` 扩展：

```sql
-- 启用 pgcrypto 扩展
CREATE EXTENSION IF NOT EXISTS pgcrypto;

-- 使用 UUID 默认值
CREATE TABLE app.api_token (
    id BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    token_id UUID NOT NULL DEFAULT gen_random_uuid(),
    token_name VARCHAR(128) NOT NULL,
    create_time TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uk_api_token_token_id UNIQUE (token_id)
);
```

UUID 与 BIGINT 的选择建议如下：

| 类型            | 优点                           | 注意事项                         |
| --------------- | ------------------------------ | -------------------------------- |
| `BIGINT`        | 索引更小，排序友好，性能稳定   | 分布式场景需要额外 ID 生成策略   |
| `UUID`          | 全局唯一，不暴露递增趋势       | 索引更大，随机写入可能影响局部性 |
| `BIGINT + UUID` | 内部使用 BIGINT，对外使用 UUID | 字段更多，但兼顾性能和安全性     |

常见项目中推荐内部主键使用 `BIGINT`，对外暴露标识使用 `UUID` 或业务编码。

### 数组类型

PostgreSQL 支持数组类型，可以存储多个同类型值，例如标签 ID、权限码、评分列表等。但数组字段不适合承载复杂关系，尤其不适合替代多对多关联表。

数组字段示例：

```sql
CREATE TABLE app.article (
    id BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    title VARCHAR(255) NOT NULL,
    tag_codes VARCHAR(64)[] NOT NULL DEFAULT '{}',
    category_ids BIGINT[] NOT NULL DEFAULT '{}',
    create_time TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);
```

插入数组数据：

```sql
INSERT INTO app.article (title, tag_codes, category_ids)
VALUES (
    'PostgreSQL 数组使用',
    ARRAY['database', 'postgresql', 'sql'],
    ARRAY[1, 2, 3]
);
```

查询数组数据：

```sql
-- 判断数组是否包含某个元素
SELECT *
FROM app.article
WHERE 'postgresql' = ANY(tag_codes);

-- 判断数组是否包含指定数组
SELECT *
FROM app.article
WHERE tag_codes @> ARRAY['postgresql'];

-- 展开数组
SELECT
    id,
    title,
    unnest(tag_codes) AS tag_code
FROM app.article;
```

数组适合存储轻量、弱关系、变化不频繁的数据。对于需要单独维护、关联查询、权限控制、排序、统计的多值数据，建议使用关联表。

### 枚举类型

枚举类型用于限制字段只能取预定义值，例如订单状态、支付状态、任务状态等。枚举类型能增强数据库约束，但变更枚举值需要执行 DDL，因此不适合频繁变化的业务字典。

创建枚举类型：

```sql
CREATE TYPE app.order_status AS ENUM (
    'PENDING',
    'PAID',
    'SHIPPED',
    'FINISHED',
    'CANCELLED'
);
```

使用枚举类型建表：

```sql
CREATE TABLE app.order_info (
    id BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    order_no VARCHAR(64) NOT NULL,
    order_status app.order_status NOT NULL DEFAULT 'PENDING',
    create_time TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uk_order_info_order_no UNIQUE (order_no)
);
```

查询枚举字段：

```sql
SELECT *
FROM app.order_info
WHERE order_status = 'PAID';
```

新增枚举值：

```sql
ALTER TYPE app.order_status ADD VALUE 'REFUNDED';
```

枚举类型适合状态值稳定、枚举项较少、数据库层需要强约束的场景。如果枚举项经常由后台配置维护，建议使用字典表，而不是数据库枚举类型。

### 网络地址类型

PostgreSQL 提供网络地址类型，用于存储 IP 地址、网段和 MAC 地址。相比字符串类型，网络地址类型可以提供格式校验和专用操作符。

常用网络地址类型如下：

| 类型       | 说明                          |
| ---------- | ----------------------------- |
| `inet`     | IPv4 或 IPv6 地址，可包含子网 |
| `cidr`     | IPv4 或 IPv6 网络地址         |
| `macaddr`  | MAC 地址                      |
| `macaddr8` | EUI-64 格式 MAC 地址          |

网络地址字段示例：

```sql
CREATE TABLE app.login_security_log (
    id BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    user_id BIGINT NOT NULL,
    login_ip INET NOT NULL,
    client_mac MACADDR,
    success BOOLEAN NOT NULL,
    create_time TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);
```

查询指定网段内的 IP：

```sql
SELECT *
FROM app.login_security_log
WHERE login_ip << '192.168.1.0/24';
```

如果只是记录普通客户端 IP，`inet` 比 `varchar(64)` 更合适。如果需要兼容非标准内容，例如代理链路、多个 IP、特殊文本，可以额外使用字符串字段存储原始值。

### 几何类型

PostgreSQL 内置几何类型可以存储点、线、矩形、圆等简单几何数据。它适合轻量几何计算，但如果项目涉及严肃地理信息、坐标系、地图检索、距离计算和空间索引，建议使用 PostGIS 扩展。

常用几何类型如下：

| 类型      | 说明     |
| --------- | -------- |
| `point`   | 点       |
| `line`    | 无限直线 |
| `lseg`    | 线段     |
| `box`     | 矩形     |
| `path`    | 路径     |
| `polygon` | 多边形   |
| `circle`  | 圆       |

几何字段示例：

```sql
CREATE TABLE app.store_location (
    id BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    store_name VARCHAR(128) NOT NULL,
    location POINT NOT NULL,
    service_area CIRCLE,
    create_time TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);
```

插入几何数据：

```sql
INSERT INTO app.store_location (store_name, location, service_area)
VALUES (
    '中心门店',
    POINT(120.123456, 30.123456),
    CIRCLE(POINT(120.123456, 30.123456), 10)
);
```

几何类型适合简单场景。生产级地理信息系统建议使用 PostGIS 的 `geometry` 或 `geography` 类型，并配合 GiST、SP-GiST 等索引能力。

### 自定义类型

自定义类型用于扩展 PostgreSQL 的类型系统，常见形式包括枚举类型、复合类型、域类型等。项目中最常见的是枚举类型和域类型。

域类型可以基于已有类型增加约束，例如统一手机号、邮箱、金额等字段规则。

创建手机号域类型：

```sql
CREATE DOMAIN app.phone_number AS VARCHAR(20)
CHECK (VALUE ~ '^[0-9+\\-]{6,20}$');
```

使用域类型建表：

```sql
CREATE TABLE app.user_contact (
    id BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    user_id BIGINT NOT NULL,
    phone app.phone_number NOT NULL,
    create_time TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);
```

创建金额域类型：

```sql
CREATE DOMAIN app.money_amount AS NUMERIC(12, 2)
CHECK (VALUE >= 0);
```

使用金额域类型：

```sql
CREATE TABLE app.account_balance (
    id BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    user_id BIGINT NOT NULL,
    balance app.money_amount NOT NULL DEFAULT 0.00,
    freeze_amount app.money_amount NOT NULL DEFAULT 0.00
);
```

自定义类型可以提升一致性，但也会增加迁移和维护成本。普通项目中优先使用基础类型加表约束，只有当多个表反复使用同一约束规则时，再考虑域类型。

## 表结构设计

本章用于说明 PostgreSQL 表结构的基础设计方式。表结构设计是数据库建模的核心，应同时考虑业务含义、数据约束、查询模式、索引策略、后续变更和版本管理。

### 表创建

创建表时应明确 Schema、字段类型、主键、非空约束、默认值、唯一约束和必要的检查约束。生产项目中不建议把所有表创建在 `public` Schema 下。

基础业务表示例：

```sql
CREATE TABLE app.sys_user (
    id BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    username VARCHAR(64) NOT NULL,
    nickname VARCHAR(64),
    phone VARCHAR(20),
    email VARCHAR(128),
    enabled BOOLEAN NOT NULL DEFAULT TRUE,
    deleted BOOLEAN NOT NULL DEFAULT FALSE,
    version INTEGER NOT NULL DEFAULT 0,
    create_time TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_time TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uk_sys_user_username UNIQUE (username),
    CONSTRAINT ck_sys_user_username_not_blank CHECK (length(trim(username)) > 0)
);
```

表设计建议如下：

| 设计项   | 建议                                                |
| -------- | --------------------------------------------------- |
| 表名     | 使用小写字母和下划线，例如 `sys_user`               |
| 主键     | 推荐 `BIGINT` 或 `UUID`                             |
| 必填字段 | 使用 `NOT NULL`                                     |
| 默认值   | 对布尔、状态、计数、时间字段设置合理默认值          |
| 唯一性   | 使用唯一约束保证业务唯一                            |
| 软删除   | 常见字段为 `deleted BOOLEAN NOT NULL DEFAULT FALSE` |
| 乐观锁   | 常见字段为 `version INTEGER NOT NULL DEFAULT 0`     |
| 审计字段 | 常见字段为 `create_time`、`update_time`             |

复杂业务表应优先保证结构清晰，而不是把所有扩展信息都塞进 `jsonb`。核心查询字段和关联字段必须设计成普通列。

### 表删除

删除表会永久删除表结构和表数据。生产环境执行前应确认备份、依赖关系和业务流量。

删除普通表：

```sql
DROP TABLE IF EXISTS app.sys_user;
```

如果表被其他对象依赖，例如外键、视图、函数等，直接删除可能失败。可以使用 `CASCADE` 级联删除依赖对象，但需要谨慎。

```sql
-- 级联删除表及依赖对象，生产环境谨慎使用
DROP TABLE IF EXISTS app.sys_user CASCADE;
```

如果只想清空数据而保留表结构，应使用 `TRUNCATE` 或 `DELETE`，不要删除表。

```sql
-- 快速清空表数据
TRUNCATE TABLE app.sys_user;

-- 清空表并重置自增序列
TRUNCATE TABLE app.sys_user RESTART IDENTITY;

-- 有外键依赖时级联清空，谨慎使用
TRUNCATE TABLE app.sys_user RESTART IDENTITY CASCADE;
```

`DROP TABLE` 是结构删除，`TRUNCATE` 是数据清空，`DELETE` 是逐行删除。生产环境应根据目标选择正确命令。

### 表重命名

表重命名用于调整表名、修正命名错误或配合数据迁移。重命名表不会删除数据，但会影响应用 SQL、视图、函数、外键、权限脚本和迁移脚本。

重命名表：

```sql
ALTER TABLE app.sys_user RENAME TO sys_user_old;
```

重命名后查看表：

```sql
SELECT
    schemaname,
    tablename
FROM pg_tables
WHERE schemaname = 'app'
ORDER BY tablename;
```

如果表上存在索引、约束、序列，重命名表不会自动把这些对象名称全部改成新规范名称。为了保持对象命名一致，可以继续重命名相关约束或索引。

```sql
-- 重命名约束
ALTER TABLE app.sys_user_old
RENAME CONSTRAINT uk_sys_user_username TO uk_sys_user_old_username;

-- 重命名索引
ALTER INDEX app.idx_sys_user_phone RENAME TO idx_sys_user_old_phone;
```

生产环境重命名前，应先全局搜索应用 SQL、Mapper、报表、视图、函数和定时任务，确认没有遗漏引用。

### 字段新增

新增字段是常见的表结构变更。新增字段时需要考虑是否允许为空、是否有默认值、是否需要回填历史数据、是否需要索引以及是否影响大表锁等待。

新增可空字段：

```sql
ALTER TABLE app.sys_user
ADD COLUMN avatar_url VARCHAR(512);
```

新增带默认值字段：

```sql
ALTER TABLE app.sys_user
ADD COLUMN enabled BOOLEAN NOT NULL DEFAULT TRUE;
```

新增字段后回填历史数据：

```sql
-- 新增字段
ALTER TABLE app.sys_user
ADD COLUMN user_level INTEGER;

-- 回填历史数据
UPDATE app.sys_user
SET user_level = 1
WHERE user_level IS NULL;

-- 设置非空约束
ALTER TABLE app.sys_user
ALTER COLUMN user_level SET NOT NULL;

-- 设置默认值
ALTER TABLE app.sys_user
ALTER COLUMN user_level SET DEFAULT 1;
```

大表新增字段建议分步骤执行，先新增可空字段，再分批回填数据，最后设置默认值和非空约束，避免一次性变更造成长时间锁表。

### 字段修改

字段修改包括修改字段类型、默认值、字段名、非空约束等。修改字段类型可能触发表重写，生产环境需要提前评估数据量和锁影响。

修改字段类型：

```sql
ALTER TABLE app.sys_user
ALTER COLUMN phone TYPE VARCHAR(32);
```

使用表达式转换字段类型：

```sql
ALTER TABLE app.sys_user
ALTER COLUMN user_level TYPE INTEGER
USING user_level::INTEGER;
```

修改字段默认值：

```sql
ALTER TABLE app.sys_user
ALTER COLUMN enabled SET DEFAULT TRUE;
```

修改字段名：

```sql
ALTER TABLE app.sys_user
RENAME COLUMN nickname TO display_name;
```

删除字段默认值：

```sql
ALTER TABLE app.sys_user
ALTER COLUMN enabled DROP DEFAULT;
```

字段修改建议如下：

| 场景           | 建议                                 |
| -------------- | ------------------------------------ |
| 扩大字符串长度 | 通常风险较低                         |
| 缩小字符串长度 | 先检查是否存在超长数据               |
| 文本转数字     | 必须先清洗非法数据                   |
| 可空改非空     | 先回填历史空值                       |
| 字段重命名     | 确认应用 SQL、Mapper、报表和视图引用 |
| 类型大幅调整   | 建议新建字段、回填、切换、删除旧字段 |

对于高频访问的大表，字段类型变更应优先采用兼容式发布策略，而不是一次性破坏性修改。

### 字段删除

字段删除会永久移除字段及其数据。生产环境删除字段前，应确认应用代码、SQL、报表、视图、函数和导出任务已经不再使用该字段。

删除字段：

```sql
ALTER TABLE app.sys_user
DROP COLUMN IF EXISTS avatar_url;
```

如果字段被视图、索引、约束等对象依赖，直接删除可能失败。可以使用 `CASCADE` 级联删除依赖对象，但需要谨慎。

```sql
-- 级联删除字段及依赖对象，生产环境谨慎使用
ALTER TABLE app.sys_user
DROP COLUMN IF EXISTS avatar_url CASCADE;
```

生产环境推荐采用安全删除流程：

```sql
-- 第一步：应用代码停止写入和读取旧字段
-- 第二步：观察一个发布周期，确认无依赖
-- 第三步：备份数据
-- 第四步：删除字段
ALTER TABLE app.sys_user
DROP COLUMN IF EXISTS old_field;
```

不建议在同一个版本中同时完成“代码删除字段引用”和“数据库删除字段”，否则回滚时容易失败。

### 默认值设置

默认值用于在插入数据时自动填充字段，适合状态、布尔值、计数、创建时间、版本号等字段。默认值可以减少应用层重复赋值，也可以提高数据一致性。

创建表时设置默认值：

```sql
CREATE TABLE app.task_info (
    id BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    task_name VARCHAR(128) NOT NULL,
    task_status VARCHAR(32) NOT NULL DEFAULT 'PENDING',
    retry_count INTEGER NOT NULL DEFAULT 0,
    enabled BOOLEAN NOT NULL DEFAULT TRUE,
    create_time TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);
```

给已有字段设置默认值：

```sql
ALTER TABLE app.task_info
ALTER COLUMN task_status SET DEFAULT 'PENDING';

ALTER TABLE app.task_info
ALTER COLUMN retry_count SET DEFAULT 0;
```

删除默认值：

```sql
ALTER TABLE app.task_info
ALTER COLUMN task_status DROP DEFAULT;
```

常见默认值建议如下：

| 字段          | 默认值                       |
| ------------- | ---------------------------- |
| `enabled`     | `TRUE`                       |
| `deleted`     | `FALSE`                      |
| `version`     | `0`                          |
| `sort_order`  | `0`                          |
| `retry_count` | `0`                          |
| `create_time` | `CURRENT_TIMESTAMP`          |
| `status`      | 按业务定义，例如 `'PENDING'` |

默认值只在插入时生效，不会自动修改历史数据。如果要修复历史空值，需要额外执行 `UPDATE`。

### 非空约束

非空约束用于保证字段必须有值。对业务必填字段、状态字段、布尔字段、审计字段、主键字段等，应优先使用 `NOT NULL`。

创建表时设置非空：

```sql
CREATE TABLE app.department (
    id BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    dept_name VARCHAR(128) NOT NULL,
    dept_code VARCHAR(64) NOT NULL,
    enabled BOOLEAN NOT NULL DEFAULT TRUE,
    create_time TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);
```

给已有字段增加非空约束：

```sql
-- 先检查是否存在空值
SELECT COUNT(*)
FROM app.department
WHERE dept_name IS NULL;

-- 回填空值
UPDATE app.department
SET dept_name = '未命名部门'
WHERE dept_name IS NULL;

-- 设置非空约束
ALTER TABLE app.department
ALTER COLUMN dept_name SET NOT NULL;
```

删除非空约束：

```sql
ALTER TABLE app.department
ALTER COLUMN dept_name DROP NOT NULL;
```

非空约束建议由数据库兜底，不要只依赖应用层校验。应用层校验可以提升用户体验，数据库约束负责保证最终数据正确。

### 主键设计

主键用于唯一标识表中的每一行数据。主键必须唯一且非空，并且通常会自动创建唯一索引。主键设计会影响关联查询、索引大小、写入性能和数据迁移。

常见主键方案如下：

| 方案            | 示例                                  | 适用场景                   |
| --------------- | ------------------------------------- | -------------------------- |
| 自增 `BIGINT`   | `BIGINT GENERATED ALWAYS AS IDENTITY` | 单库业务系统、内部表       |
| 分布式 `BIGINT` | 雪花 ID                               | 分布式系统、分库分表预留   |
| `UUID`          | `UUID DEFAULT gen_random_uuid()`      | 对外标识、全局唯一场景     |
| 复合主键        | `(tenant_id, business_id)`            | 关联表、中间表、特殊业务表 |

推荐的自增主键：

```sql
CREATE TABLE app.sys_role (
    id BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    role_code VARCHAR(64) NOT NULL,
    role_name VARCHAR(128) NOT NULL,
    create_time TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);
```

关联表复合主键示例：

```sql
CREATE TABLE app.sys_user_role (
    user_id BIGINT NOT NULL,
    role_id BIGINT NOT NULL,
    create_time TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT pk_sys_user_role PRIMARY KEY (user_id, role_id)
);
```

主键设计建议如下：

| 建议                   | 说明                                 |
| ---------------------- | ------------------------------------ |
| 优先使用无业务含义主键 | 不要用手机号、邮箱、订单号直接做主键 |
| 主键保持稳定           | 主键不应随业务字段变化而变化         |
| 大表优先用 `BIGINT`    | 索引体积和查询性能更稳定             |
| 对外暴露可使用 UUID    | 避免暴露自增 ID 趋势                 |
| 关联表可使用复合主键   | 防止重复关系数据                     |

业务唯一性应通过唯一约束保证，不应直接依赖主键表达业务唯一含义。

### 外键设计

外键用于保证表与表之间的引用完整性。例如订单必须属于已存在的用户，订单明细必须属于已存在的订单。外键可以增强数据一致性，但也会影响写入、删除和迁移操作。

外键示例：

```sql
CREATE TABLE app.order_info (
    id BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    order_no VARCHAR(64) NOT NULL,
    user_id BIGINT NOT NULL,
    total_amount NUMERIC(12, 2) NOT NULL,
    create_time TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uk_order_info_order_no UNIQUE (order_no),
    CONSTRAINT fk_order_info_user_id FOREIGN KEY (user_id)
        REFERENCES app.sys_user (id)
);
```

带删除策略的外键：

```sql
CREATE TABLE app.order_item (
    id BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    order_id BIGINT NOT NULL,
    product_name VARCHAR(128) NOT NULL,
    quantity INTEGER NOT NULL,
    sale_price NUMERIC(12, 2) NOT NULL,
    CONSTRAINT fk_order_item_order_id FOREIGN KEY (order_id)
        REFERENCES app.order_info (id)
        ON DELETE CASCADE
);
```

常见外键动作如下：

| 动作                 | 说明                       |
| -------------------- | -------------------------- |
| `ON DELETE RESTRICT` | 存在子数据时禁止删除父数据 |
| `ON DELETE CASCADE`  | 删除父数据时自动删除子数据 |
| `ON DELETE SET NULL` | 删除父数据时子表字段置空   |
| `ON UPDATE CASCADE`  | 父表主键更新时同步更新子表 |

外键设计建议如下：

| 场景             | 建议                                       |
| ---------------- | ------------------------------------------ |
| 核心强一致关系   | 可以使用外键                               |
| 高并发写入大表   | 谨慎使用外键，评估性能和锁影响             |
| 跨库关系         | 无法使用普通外键，需应用层保证             |
| 数据仓库或日志表 | 通常不使用外键                             |
| 软删除系统       | 外键不能直接表达软删除约束，需要应用层配合 |

是否使用外键应根据系统类型决定。中后台管理系统和强一致业务适合使用外键；高吞吐互联网业务中，可能更多依赖应用层约束和异步校验。

### 唯一约束

唯一约束用于保证字段或字段组合不重复。常见场景包括用户名、手机号、邮箱、订单号、租户内编码等。

单字段唯一约束：

```sql
CREATE TABLE app.sys_user_unique_demo (
    id BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    username VARCHAR(64) NOT NULL,
    email VARCHAR(128),
    CONSTRAINT uk_sys_user_unique_demo_username UNIQUE (username)
);
```

联合唯一约束：

```sql
CREATE TABLE app.sys_dict_item (
    id BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    dict_type VARCHAR(64) NOT NULL,
    item_code VARCHAR(64) NOT NULL,
    item_name VARCHAR(128) NOT NULL,
    sort_order INTEGER NOT NULL DEFAULT 0,
    CONSTRAINT uk_sys_dict_item_type_code UNIQUE (dict_type, item_code)
);
```

为已有表新增唯一约束：

```sql
ALTER TABLE app.sys_dict_item
ADD CONSTRAINT uk_sys_dict_item_type_code UNIQUE (dict_type, item_code);
```

如果存在软删除字段，需要注意唯一约束与软删除的冲突。例如同一个用户名删除后是否允许重新注册。如果允许，则可以使用部分唯一索引：

```sql
-- 仅对未删除数据保证用户名唯一
CREATE UNIQUE INDEX uk_sys_user_username_not_deleted
ON app.sys_user (username)
WHERE deleted = FALSE;
```

唯一约束适合表达全表唯一规则；部分唯一索引适合表达有条件的唯一规则，例如“同一租户下未删除数据唯一”。

### 检查约束

检查约束用于限制字段值必须满足指定条件，例如金额不能小于 0、结束时间必须大于开始时间、状态只能在指定范围内等。

金额检查约束：

```sql
CREATE TABLE app.account_record (
    id BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    user_id BIGINT NOT NULL,
    amount NUMERIC(12, 2) NOT NULL,
    create_time TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT ck_account_record_amount_non_negative CHECK (amount >= 0)
);
```

时间范围检查约束：

```sql
CREATE TABLE app.activity_info (
    id BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    activity_name VARCHAR(128) NOT NULL,
    start_time TIMESTAMP NOT NULL,
    end_time TIMESTAMP NOT NULL,
    CONSTRAINT ck_activity_info_time_range CHECK (end_time > start_time)
);
```

状态检查约束：

```sql
CREATE TABLE app.message_info (
    id BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    message_title VARCHAR(128) NOT NULL,
    message_status VARCHAR(32) NOT NULL,
    CONSTRAINT ck_message_info_status CHECK (
        message_status IN ('DRAFT', 'PUBLISHED', 'OFFLINE')
    )
);
```

为已有表新增检查约束：

```sql
ALTER TABLE app.activity_info
ADD CONSTRAINT ck_activity_info_time_range CHECK (end_time > start_time);
```

检查约束适合稳定规则。如果规则经常变化，例如后台可配置的业务状态，建议使用字典表或配置表，不要频繁修改检查约束。

### 表注释与字段注释

表注释和字段注释用于描述数据对象的业务含义，便于开发、联调、排查、生成文档和后续维护。生产项目中建议所有核心业务表和核心字段都添加注释。

表注释示例：

```sql
COMMENT ON TABLE app.sys_user IS '系统用户表';
```

字段注释示例：

```sql
COMMENT ON COLUMN app.sys_user.id IS '主键ID';
COMMENT ON COLUMN app.sys_user.username IS '用户名';
COMMENT ON COLUMN app.sys_user.nickname IS '用户昵称';
COMMENT ON COLUMN app.sys_user.phone IS '手机号';
COMMENT ON COLUMN app.sys_user.email IS '邮箱';
COMMENT ON COLUMN app.sys_user.enabled IS '是否启用';
COMMENT ON COLUMN app.sys_user.deleted IS '是否删除';
COMMENT ON COLUMN app.sys_user.version IS '乐观锁版本号';
COMMENT ON COLUMN app.sys_user.create_time IS '创建时间';
COMMENT ON COLUMN app.sys_user.update_time IS '更新时间';
```

完整建表和注释示例：

```sql
CREATE TABLE app.sys_menu (
    id BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    parent_id BIGINT,
    menu_name VARCHAR(128) NOT NULL,
    menu_code VARCHAR(128) NOT NULL,
    menu_type VARCHAR(32) NOT NULL,
    path VARCHAR(255),
    component VARCHAR(255),
    permission_code VARCHAR(255),
    sort_order INTEGER NOT NULL DEFAULT 0,
    enabled BOOLEAN NOT NULL DEFAULT TRUE,
    create_time TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_time TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uk_sys_menu_menu_code UNIQUE (menu_code),
    CONSTRAINT ck_sys_menu_menu_type CHECK (menu_type IN ('DIR', 'MENU', 'BUTTON'))
);

COMMENT ON TABLE app.sys_menu IS '系统菜单表';
COMMENT ON COLUMN app.sys_menu.id IS '主键ID';
COMMENT ON COLUMN app.sys_menu.parent_id IS '父级菜单ID';
COMMENT ON COLUMN app.sys_menu.menu_name IS '菜单名称';
COMMENT ON COLUMN app.sys_menu.menu_code IS '菜单编码';
COMMENT ON COLUMN app.sys_menu.menu_type IS '菜单类型：DIR目录，MENU菜单，BUTTON按钮';
COMMENT ON COLUMN app.sys_menu.path IS '路由路径';
COMMENT ON COLUMN app.sys_menu.component IS '前端组件路径';
COMMENT ON COLUMN app.sys_menu.permission_code IS '权限标识';
COMMENT ON COLUMN app.sys_menu.sort_order IS '排序号';
COMMENT ON COLUMN app.sys_menu.enabled IS '是否启用';
COMMENT ON COLUMN app.sys_menu.create_time IS '创建时间';
COMMENT ON COLUMN app.sys_menu.update_time IS '更新时间';
```

查询表和字段注释：

```sql
SELECT
    c.relname AS table_name,
    obj_description(c.oid) AS table_comment
FROM pg_class c
JOIN pg_namespace n ON n.oid = c.relnamespace
WHERE n.nspname = 'app'
  AND c.relkind = 'r'
ORDER BY c.relname;
```

查询字段注释：

```sql
SELECT
    c.relname AS table_name,
    a.attname AS column_name,
    col_description(a.attrelid, a.attnum) AS column_comment
FROM pg_class c
JOIN pg_namespace n ON n.oid = c.relnamespace
JOIN pg_attribute a ON a.attrelid = c.oid
WHERE n.nspname = 'app'
  AND c.relkind = 'r'
  AND a.attnum > 0
  AND NOT a.attisdropped
ORDER BY c.relname, a.attnum;
```

注释应描述业务含义，而不是简单重复字段名。例如 `enabled` 的注释写“是否启用”是有价值的，`user_name` 的注释只写“user_name”则没有意义。


## 数据操作

本章用于说明 PostgreSQL 中常见的数据增删改查操作。业务开发中应优先使用参数化 SQL、Mapper、Repository 或 ORM 框架生成 SQL，避免字符串拼接，防止 SQL 注入和类型转换错误。

### 数据插入

数据插入使用 `INSERT INTO`。插入时建议明确指定字段列表，不建议依赖表字段顺序。这样即使后续新增字段，也不容易影响已有 SQL。

基础插入示例：

```sql
INSERT INTO app.sys_user (
    username,
    nickname,
    phone,
    email,
    enabled,
    deleted
) VALUES (
    'admin',
    '系统管理员',
    '13800000000',
    'admin@example.com',
    TRUE,
    FALSE
);
```

如果字段有默认值，可以省略该字段。例如 `id`、`enabled`、`deleted`、`create_time`、`update_time` 等字段已经有默认值时，可以只插入业务必填字段。

```sql
INSERT INTO app.sys_user (
    username,
    nickname,
    phone,
    email
) VALUES (
    'zhangsan',
    '张三',
    '13800000001',
    'zhangsan@example.com'
);
```

插入时也可以使用 `DEFAULT` 关键字显式使用字段默认值。

```sql
INSERT INTO app.sys_user (
    username,
    nickname,
    enabled,
    deleted
) VALUES (
    'lisi',
    '李四',
    DEFAULT,
    DEFAULT
);
```

插入数据时建议遵循以下规则：

| 规则                        | 说明                                    |
| --------------------------- | --------------------------------------- |
| 明确字段列表                | 避免字段顺序变化导致插入错误            |
| 不手动插入自增主键          | 除非是数据迁移或修复场景                |
| 必填字段必须赋值            | 对应 `NOT NULL` 字段必须有值或默认值    |
| 字符串使用单引号            | PostgreSQL 中字符串字面量使用 `'value'` |
| 布尔值使用 `TRUE` / `FALSE` | 不建议使用 `0` / `1` 表示布尔值         |

### 批量插入

批量插入适合一次性写入多条数据。相比逐条执行 `INSERT`，批量插入可以减少网络往返和 SQL 执行开销。

使用多行 `VALUES` 批量插入：

```sql
INSERT INTO app.sys_user (
    username,
    nickname,
    phone,
    email
) VALUES
    ('user001', '用户001', '13800001001', 'user001@example.com'),
    ('user002', '用户002', '13800001002', 'user002@example.com'),
    ('user003', '用户003', '13800001003', 'user003@example.com');
```

使用 `INSERT INTO ... SELECT` 从查询结果插入：

```sql
INSERT INTO app.sys_user_archive (
    user_id,
    username,
    nickname,
    archive_time
)
SELECT
    id,
    username,
    nickname,
    CURRENT_TIMESTAMP
FROM app.sys_user
WHERE deleted = TRUE;
```

使用 `COPY` 导入大量数据：

```sql
COPY app.sys_user (
    username,
    nickname,
    phone,
    email
)
FROM '/tmp/sys_user.csv'
WITH (
    FORMAT csv,
    HEADER true,
    ENCODING 'UTF8'
);
```

如果从客户端本机导入文件，可以在 `psql` 中使用 `\copy`：

```sql
\copy app.sys_user(username, nickname, phone, email)
FROM './sys_user.csv'
WITH (FORMAT csv, HEADER true, ENCODING 'UTF8');
```

批量插入建议如下：

| 场景             | 推荐方式                                                 |
| ---------------- | -------------------------------------------------------- |
| 几条到几百条数据 | 多行 `VALUES`                                            |
| 从已有表生成数据 | `INSERT INTO ... SELECT`                                 |
| CSV 大批量导入   | `COPY` 或 `\copy`                                        |
| Java 批量写入    | JDBC batch、MyBatis 批处理、MyBatis-Plus 批量保存        |
| 超大数据量导入   | 分批导入、关闭不必要索引、导入后重建索引或执行 `ANALYZE` |

### 数据查询

数据查询使用 `SELECT`。实际项目中查询时应明确字段列表，不建议在业务代码中长期使用 `SELECT *`，因为字段增加会影响网络传输、对象映射和查询稳定性。

查询指定字段：

```sql
SELECT
    id,
    username,
    nickname,
    phone,
    email,
    enabled,
    create_time
FROM app.sys_user;
```

查询单条数据：

```sql
SELECT
    id,
    username,
    nickname,
    phone,
    email
FROM app.sys_user
WHERE id = 1;
```

查询并使用字段别名：

```sql
SELECT
    id AS user_id,
    username AS user_name,
    nickname AS display_name,
    create_time AS register_time
FROM app.sys_user
WHERE deleted = FALSE;
```

查询常量和表达式：

```sql
SELECT
    id,
    username,
    nickname,
    CASE WHEN enabled THEN '启用' ELSE '禁用' END AS enabled_text,
    CURRENT_TIMESTAMP AS query_time
FROM app.sys_user
WHERE deleted = FALSE;
```

业务查询建议如下：

| 建议               | 说明                                      |
| ------------------ | ----------------------------------------- |
| 明确字段列表       | 减少无用字段传输，避免字段变化影响映射    |
| 条件字段建索引     | 高频过滤字段应结合索引设计                |
| 避免大结果集       | 查询列表必须分页或限制范围                |
| 避免函数包裹索引列 | 例如 `date(create_time)` 可能影响索引使用 |
| 复杂查询先分析计划 | 使用 `EXPLAIN` 或 `EXPLAIN ANALYZE`       |

### 条件查询

条件查询使用 `WHERE`。常见条件包括等值匹配、范围查询、模糊匹配、空值判断、集合判断和布尔条件。

等值查询：

```sql
SELECT
    id,
    username,
    nickname
FROM app.sys_user
WHERE username = 'admin';
```

范围查询：

```sql
SELECT
    id,
    username,
    create_time
FROM app.sys_user
WHERE create_time >= TIMESTAMP '2026-01-01 00:00:00'
  AND create_time < TIMESTAMP '2026-02-01 00:00:00';
```

集合查询：

```sql
SELECT
    id,
    username,
    enabled
FROM app.sys_user
WHERE id IN (1, 2, 3);
```

空值查询：

```sql
SELECT
    id,
    username,
    email
FROM app.sys_user
WHERE email IS NULL;
```

布尔条件查询：

```sql
SELECT
    id,
    username,
    nickname
FROM app.sys_user
WHERE enabled
  AND NOT deleted;
```

模糊查询：

```sql
SELECT
    id,
    username,
    nickname
FROM app.sys_user
WHERE nickname LIKE '%张%';
```

大小写不敏感模糊查询可以使用 `ILIKE`：

```sql
SELECT
    id,
    username,
    email
FROM app.sys_user
WHERE email ILIKE '%EXAMPLE.COM';
```

条件查询中需要特别注意 `NULL`。不能使用 `= NULL` 判断空值，必须使用 `IS NULL` 或 `IS NOT NULL`。

### 数据更新

数据更新使用 `UPDATE`。更新数据时必须带上明确的 `WHERE` 条件，避免误更新整张表。

按主键更新：

```sql
UPDATE app.sys_user
SET
    nickname = '系统管理员',
    phone = '13800009999',
    update_time = CURRENT_TIMESTAMP
WHERE id = 1;
```

按业务唯一字段更新：

```sql
UPDATE app.sys_user
SET
    enabled = FALSE,
    update_time = CURRENT_TIMESTAMP
WHERE username = 'admin';
```

基于原值更新：

```sql
UPDATE app.sys_user
SET
    version = version + 1,
    update_time = CURRENT_TIMESTAMP
WHERE id = 1;
```

带乐观锁的更新：

```sql
UPDATE app.sys_user
SET
    nickname = '管理员',
    version = version + 1,
    update_time = CURRENT_TIMESTAMP
WHERE id = 1
  AND version = 0;
```

如果更新结果影响行数为 `0`，通常表示数据不存在、条件不满足或乐观锁版本冲突。

更新数据建议如下：

| 建议                   | 说明                                      |
| ---------------------- | ----------------------------------------- |
| 必须写 `WHERE`         | 防止误更新全表                            |
| 更新时间同步维护       | 修改业务字段时同步修改 `update_time`      |
| 状态流转加条件         | 例如只允许 `PENDING` 更新为 `PAID`        |
| 高并发场景使用乐观锁   | 使用 `version` 或业务状态作为并发控制条件 |
| 批量更新先查询影响范围 | 避免条件过宽                              |

### 批量更新

批量更新用于一次性修改多条数据。批量更新前应先用相同条件执行 `SELECT COUNT(*)` 或查询样本数据，确认影响范围正确。

按条件批量更新：

```sql
UPDATE app.sys_user
SET
    enabled = FALSE,
    update_time = CURRENT_TIMESTAMP
WHERE deleted = TRUE;
```

按集合批量更新：

```sql
UPDATE app.sys_user
SET
    enabled = FALSE,
    update_time = CURRENT_TIMESTAMP
WHERE id IN (1, 2, 3);
```

使用 `CASE WHEN` 批量更新不同值：

```sql
UPDATE app.sys_user
SET
    nickname = CASE id
        WHEN 1 THEN '管理员'
        WHEN 2 THEN '测试用户'
        WHEN 3 THEN '运营用户'
        ELSE nickname
    END,
    update_time = CURRENT_TIMESTAMP
WHERE id IN (1, 2, 3);
```

使用临时数据表批量更新：

```sql
CREATE TEMP TABLE tmp_user_nickname (
    id BIGINT PRIMARY KEY,
    nickname VARCHAR(64) NOT NULL
);

INSERT INTO tmp_user_nickname (id, nickname)
VALUES
    (1, '管理员'),
    (2, '测试用户'),
    (3, '运营用户');

UPDATE app.sys_user u
SET
    nickname = t.nickname,
    update_time = CURRENT_TIMESTAMP
FROM tmp_user_nickname t
WHERE u.id = t.id;
```

批量更新建议如下：

| 场景             | 建议                               |
| ---------------- | ---------------------------------- |
| 小批量固定数据   | 使用 `CASE WHEN`                   |
| 中大批量外部数据 | 使用临时表或中间表                 |
| 大表批量更新     | 分批执行，避免长事务和大量锁等待   |
| 生产修复数据     | 先备份，先查询影响范围，再执行更新 |
| 有索引字段更新   | 注意索引维护成本和表膨胀           |

### 数据删除

物理删除使用 `DELETE`。删除数据会移除表中的行，但不会立即释放磁盘空间，后续需要依赖 VACUUM 回收可复用空间。

按主键删除：

```sql
DELETE FROM app.sys_user
WHERE id = 1;
```

按条件删除：

```sql
DELETE FROM app.sys_user
WHERE deleted = TRUE
  AND update_time < CURRENT_TIMESTAMP - INTERVAL '180 days';
```

删除前查询影响范围：

```sql
SELECT COUNT(*)
FROM app.sys_user
WHERE deleted = TRUE
  AND update_time < CURRENT_TIMESTAMP - INTERVAL '180 days';
```

删除并返回被删除数据：

```sql
DELETE FROM app.sys_user
WHERE id = 1
RETURNING id, username, nickname;
```

物理删除建议如下：

| 场景         | 建议                      |
| ------------ | ------------------------- |
| 普通业务数据 | 优先逻辑删除              |
| 临时表数据   | 可以物理删除或 `TRUNCATE` |
| 历史归档数据 | 归档后分批物理删除        |
| 大批量删除   | 分批执行，避免长事务      |
| 有外键依赖   | 先确认子表数据和外键策略  |

不要在生产环境直接执行无条件 `DELETE FROM table_name;`。如果确实要清空整表，应明确使用 `TRUNCATE`，并确认是否需要重置序列和处理外键依赖。

### 逻辑删除

逻辑删除是通过字段标记数据已删除，而不是立即物理删除。常见字段为 `deleted BOOLEAN NOT NULL DEFAULT FALSE`，也可以使用 `delete_time`、`delete_user_id` 等字段记录删除信息。

逻辑删除字段设计：

```sql
CREATE TABLE app.article (
    id BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    title VARCHAR(255) NOT NULL,
    content TEXT,
    deleted BOOLEAN NOT NULL DEFAULT FALSE,
    delete_time TIMESTAMP,
    create_time TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_time TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);
```

执行逻辑删除：

```sql
UPDATE app.article
SET
    deleted = TRUE,
    delete_time = CURRENT_TIMESTAMP,
    update_time = CURRENT_TIMESTAMP
WHERE id = 1
  AND deleted = FALSE;
```

查询未删除数据：

```sql
SELECT
    id,
    title,
    create_time
FROM app.article
WHERE deleted = FALSE
ORDER BY create_time DESC;
```

恢复逻辑删除数据：

```sql
UPDATE app.article
SET
    deleted = FALSE,
    delete_time = NULL,
    update_time = CURRENT_TIMESTAMP
WHERE id = 1
  AND deleted = TRUE;
```

如果逻辑删除表需要唯一约束，建议使用部分唯一索引。例如用户名在未删除数据中唯一：

```sql
CREATE UNIQUE INDEX uk_sys_user_username_not_deleted
ON app.sys_user (username)
WHERE deleted = FALSE;
```

逻辑删除建议如下：

| 建议                   | 说明                                          |
| ---------------------- | --------------------------------------------- |
| 查询默认过滤删除数据   | 所有业务查询都应带 `deleted = FALSE`          |
| 唯一约束考虑软删除     | 使用部分唯一索引处理重复注册等场景            |
| 定期归档历史数据       | 长期逻辑删除会增加表体积                      |
| 敏感数据按规则物理清理 | 涉及合规时，逻辑删除不等于真正删除            |
| 索引考虑删除字段       | 高频查询可组合 `(deleted, business_key)` 索引 |

### UPSERT 操作

UPSERT 表示“存在则更新，不存在则插入”。PostgreSQL 使用 `INSERT ... ON CONFLICT` 实现 UPSERT。冲突目标必须是唯一约束、主键或唯一索引。

准备示例表：

```sql
CREATE TABLE app.sys_config (
    id BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    config_key VARCHAR(128) NOT NULL,
    config_value TEXT,
    remark VARCHAR(255),
    update_time TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    create_time TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uk_sys_config_key UNIQUE (config_key)
);
```

存在冲突时什么都不做：

```sql
INSERT INTO app.sys_config (
    config_key,
    config_value,
    remark
) VALUES (
    'system.name',
    '后台管理系统',
    '系统名称'
)
ON CONFLICT (config_key) DO NOTHING;
```

存在冲突时更新：

```sql
INSERT INTO app.sys_config (
    config_key,
    config_value,
    remark
) VALUES (
    'system.name',
    'PostgreSQL 管理系统',
    '系统名称'
)
ON CONFLICT (config_key) DO UPDATE
SET
    config_value = EXCLUDED.config_value,
    remark = EXCLUDED.remark,
    update_time = CURRENT_TIMESTAMP;
```

`EXCLUDED` 表示本次尝试插入的新数据。可以理解为“冲突时被排除插入的那一行”。

带条件的 UPSERT：

```sql
INSERT INTO app.sys_config (
    config_key,
    config_value,
    remark
) VALUES (
    'system.name',
    '新系统名称',
    '系统名称'
)
ON CONFLICT (config_key) DO UPDATE
SET
    config_value = EXCLUDED.config_value,
    remark = EXCLUDED.remark,
    update_time = CURRENT_TIMESTAMP
WHERE app.sys_config.config_value IS DISTINCT FROM EXCLUDED.config_value;
```

UPSERT 建议如下：

| 建议             | 说明                                   |
| ---------------- | -------------------------------------- |
| 冲突字段必须唯一 | 需要主键、唯一约束或唯一索引           |
| 明确更新字段     | 不要无脑覆盖所有字段                   |
| 维护更新时间     | 冲突更新时同步修改 `update_time`       |
| 谨慎覆盖审计字段 | `create_time` 通常不应在冲突更新时修改 |
| 可加条件更新     | 避免相同数据重复更新造成无意义写入     |

### RETURNING 返回数据

`RETURNING` 可以在 `INSERT`、`UPDATE`、`DELETE` 后直接返回受影响的数据，常用于获取自增主键、更新后的字段值或删除前的数据快照。

插入后返回主键：

```sql
INSERT INTO app.sys_user (
    username,
    nickname,
    phone
) VALUES (
    'wangwu',
    '王五',
    '13800000005'
)
RETURNING id;
```

插入后返回完整数据：

```sql
INSERT INTO app.sys_user (
    username,
    nickname,
    phone
) VALUES (
    'zhaoliu',
    '赵六',
    '13800000006'
)
RETURNING
    id,
    username,
    nickname,
    phone,
    enabled,
    create_time;
```

更新后返回新值：

```sql
UPDATE app.sys_user
SET
    nickname = '王五-更新',
    update_time = CURRENT_TIMESTAMP
WHERE username = 'wangwu'
RETURNING
    id,
    username,
    nickname,
    update_time;
```

删除后返回被删除数据：

```sql
DELETE FROM app.sys_user
WHERE username = 'zhaoliu'
RETURNING
    id,
    username,
    nickname;
```

UPSERT 后返回数据：

```sql
INSERT INTO app.sys_config (
    config_key,
    config_value
) VALUES (
    'system.theme',
    'dark'
)
ON CONFLICT (config_key) DO UPDATE
SET
    config_value = EXCLUDED.config_value,
    update_time = CURRENT_TIMESTAMP
RETURNING
    id,
    config_key,
    config_value,
    update_time;
```

在 Java 项目中，`RETURNING` 可以减少一次额外查询，尤其适合插入后立即需要主键、默认值或数据库生成字段的场景。

## 查询语法

本章用于说明 PostgreSQL 常用查询语法。查询语法是日常开发中使用频率最高的部分，应重点掌握过滤、排序、分页、分组、子查询、空值处理和条件表达式。

### 基础 SELECT 查询

`SELECT` 用于从表、视图、子查询或函数结果中读取数据。基础结构通常包括字段列表、数据来源、过滤条件、排序和分页。

基础查询：

```sql
SELECT
    id,
    username,
    nickname,
    phone,
    email
FROM app.sys_user;
```

字段别名：

```sql
SELECT
    id AS user_id,
    username AS user_name,
    nickname AS display_name
FROM app.sys_user;
```

表达式查询：

```sql
SELECT
    id,
    username,
    nickname,
    username || ' - ' || COALESCE(nickname, '') AS user_label
FROM app.sys_user;
```

查询当前时间和常量：

```sql
SELECT
    CURRENT_DATE AS current_date,
    CURRENT_TIMESTAMP AS current_time,
    'PostgreSQL 17' AS database_version_note;
```

业务开发中建议避免直接 `SELECT *`，除非是临时排查或交互式查询。接口返回、Mapper 映射和报表 SQL 应明确字段列表。

### WHERE 条件过滤

`WHERE` 用于过滤行数据。多个条件可以使用 `AND`、`OR`、`NOT` 组合。复杂条件建议使用括号明确优先级。

基础过滤：

```sql
SELECT
    id,
    username,
    nickname
FROM app.sys_user
WHERE deleted = FALSE;
```

多条件过滤：

```sql
SELECT
    id,
    username,
    nickname,
    enabled
FROM app.sys_user
WHERE deleted = FALSE
  AND enabled = TRUE;
```

组合条件过滤：

```sql
SELECT
    id,
    username,
    nickname,
    phone,
    email
FROM app.sys_user
WHERE deleted = FALSE
  AND (
      phone = '13800000000'
      OR email = 'admin@example.com'
  );
```

范围过滤：

```sql
SELECT
    id,
    username,
    create_time
FROM app.sys_user
WHERE create_time >= TIMESTAMP '2026-01-01 00:00:00'
  AND create_time < TIMESTAMP '2026-02-01 00:00:00';
```

集合过滤：

```sql
SELECT
    id,
    username,
    nickname
FROM app.sys_user
WHERE id IN (1, 2, 3);
```

空值过滤：

```sql
SELECT
    id,
    username,
    email
FROM app.sys_user
WHERE email IS NOT NULL;
```

`WHERE` 条件中最容易出错的是 `NULL` 判断和 `OR` 优先级。涉及 `OR` 时建议显式加括号，避免查询范围被意外扩大。

### ORDER BY 排序

`ORDER BY` 用于对查询结果排序。可以按一个字段排序，也可以按多个字段排序。默认是升序 `ASC`，降序使用 `DESC`。

按创建时间倒序：

```sql
SELECT
    id,
    username,
    create_time
FROM app.sys_user
WHERE deleted = FALSE
ORDER BY create_time DESC;
```

多字段排序：

```sql
SELECT
    id,
    username,
    enabled,
    create_time
FROM app.sys_user
WHERE deleted = FALSE
ORDER BY enabled DESC, create_time DESC, id DESC;
```

空值排序控制：

```sql
SELECT
    id,
    username,
    email
FROM app.sys_user
ORDER BY email ASC NULLS LAST;
```

按表达式排序：

```sql
SELECT
    id,
    username,
    nickname
FROM app.sys_user
ORDER BY length(username) DESC;
```

分页查询必须有稳定排序字段。不要只按非唯一字段排序，例如只按 `create_time DESC`，当多条数据创建时间相同时，分页可能出现重复或遗漏。建议追加主键作为稳定排序条件。

```sql
SELECT
    id,
    username,
    create_time
FROM app.sys_user
WHERE deleted = FALSE
ORDER BY create_time DESC, id DESC;
```

### LIMIT 与 OFFSET 分页

`LIMIT` 用于限制返回行数，`OFFSET` 用于跳过指定行数。常用于后台列表分页。

第一页，每页 10 条：

```sql
SELECT
    id,
    username,
    nickname,
    create_time
FROM app.sys_user
WHERE deleted = FALSE
ORDER BY create_time DESC, id DESC
LIMIT 10 OFFSET 0;
```

第二页，每页 10 条：

```sql
SELECT
    id,
    username,
    nickname,
    create_time
FROM app.sys_user
WHERE deleted = FALSE
ORDER BY create_time DESC, id DESC
LIMIT 10 OFFSET 10;
```

第 `pageNum` 页的偏移量计算方式：

```text
OFFSET = (pageNum - 1) * pageSize
```

普通后台分页可以使用 `LIMIT + OFFSET`。但当页码很深时，数据库仍然需要扫描并跳过大量数据，性能会下降。大数据量场景建议使用游标分页或基于主键的延续分页。

基于主键的下一页查询：

```sql
SELECT
    id,
    username,
    nickname,
    create_time
FROM app.sys_user
WHERE deleted = FALSE
  AND id < 10000
ORDER BY id DESC
LIMIT 10;
```

分页建议如下：

| 场景         | 推荐方式                            |
| ------------ | ----------------------------------- |
| 后台普通列表 | `LIMIT + OFFSET`                    |
| 深分页       | 基于主键或时间游标分页              |
| 导出大数据   | 游标、分批查询或流式处理            |
| 分页排序     | 必须有稳定排序字段                  |
| 统计总数     | 单独执行 `COUNT(*)`，并注意大表成本 |

### DISTINCT 去重

`DISTINCT` 用于去除重复结果。它会基于查询字段整体去重，而不是只按某个字段去重。

查询不同状态：

```sql
SELECT DISTINCT
    enabled
FROM app.sys_user;
```

查询不同手机号和邮箱组合：

```sql
SELECT DISTINCT
    phone,
    email
FROM app.sys_user
WHERE deleted = FALSE;
```

PostgreSQL 支持 `DISTINCT ON`，可以按指定字段保留每组中的第一条数据。使用时必须配合 `ORDER BY` 明确保留哪一条。

按用户名保留最新一条记录：

```sql
SELECT DISTINCT ON (username)
    id,
    username,
    nickname,
    create_time
FROM app.sys_user
ORDER BY username, create_time DESC, id DESC;
```

`DISTINCT` 会增加排序或哈希去重成本，不应为了掩盖 JOIN 错误而随意添加。如果查询结果重复，优先检查关联条件是否正确。

### GROUP BY 分组

`GROUP BY` 用于按字段分组，并结合聚合函数进行统计。常见聚合函数包括 `COUNT`、`SUM`、`AVG`、`MIN`、`MAX`。

按启用状态统计用户数量：

```sql
SELECT
    enabled,
    COUNT(*) AS user_count
FROM app.sys_user
GROUP BY enabled;
```

按日期统计新增用户数：

```sql
SELECT
    create_time::date AS create_date,
    COUNT(*) AS user_count
FROM app.sys_user
WHERE deleted = FALSE
GROUP BY create_time::date
ORDER BY create_date DESC;
```

按多个字段分组：

```sql
SELECT
    enabled,
    deleted,
    COUNT(*) AS user_count
FROM app.sys_user
GROUP BY enabled, deleted
ORDER BY enabled DESC, deleted ASC;
```

聚合查询中，`SELECT` 出现的非聚合字段通常必须出现在 `GROUP BY` 中。

错误示例：

```sql
SELECT
    enabled,
    username,
    COUNT(*)
FROM app.sys_user
GROUP BY enabled;
```

上面 SQL 中 `username` 既不是聚合函数，也没有出现在 `GROUP BY` 中，因此不符合分组查询规则。

### HAVING 分组过滤

`HAVING` 用于过滤分组后的结果。`WHERE` 是分组前过滤行数据，`HAVING` 是分组后过滤聚合结果。

统计用户数量大于 10 的启用状态分组：

```sql
SELECT
    enabled,
    COUNT(*) AS user_count
FROM app.sys_user
GROUP BY enabled
HAVING COUNT(*) > 10;
```

按日期统计新增用户数，并只返回大于 100 的日期：

```sql
SELECT
    create_time::date AS create_date,
    COUNT(*) AS user_count
FROM app.sys_user
WHERE deleted = FALSE
GROUP BY create_time::date
HAVING COUNT(*) > 100
ORDER BY create_date DESC;
```

`WHERE` 和 `HAVING` 可以同时使用：

```sql
SELECT
    enabled,
    COUNT(*) AS user_count
FROM app.sys_user
WHERE deleted = FALSE
GROUP BY enabled
HAVING COUNT(*) >= 1;
```

使用建议如下：

| 子句       | 执行位置 | 适用场景     |
| ---------- | -------- | ------------ |
| `WHERE`    | 分组前   | 过滤原始行   |
| `GROUP BY` | 聚合阶段 | 按字段分组   |
| `HAVING`   | 分组后   | 过滤聚合结果 |
| `ORDER BY` | 结果阶段 | 排序最终结果 |

能在 `WHERE` 中过滤的条件，不要放到 `HAVING` 中。提前过滤可以减少参与分组的数据量。

### 子查询

子查询是嵌套在 SQL 内部的查询，可以出现在 `WHERE`、`FROM`、`SELECT` 等位置。子查询适合表达分步逻辑，但复杂子查询应注意性能和可读性。

`WHERE IN` 子查询：

```sql
SELECT
    id,
    username,
    nickname
FROM app.sys_user
WHERE id IN (
    SELECT user_id
    FROM app.sys_user_role
    WHERE role_id = 1
);
```

`FROM` 子查询：

```sql
SELECT
    t.create_date,
    t.user_count
FROM (
    SELECT
        create_time::date AS create_date,
        COUNT(*) AS user_count
    FROM app.sys_user
    WHERE deleted = FALSE
    GROUP BY create_time::date
) t
WHERE t.user_count > 10
ORDER BY t.create_date DESC;
```

`SELECT` 子查询：

```sql
SELECT
    u.id,
    u.username,
    (
        SELECT COUNT(*)
        FROM app.sys_user_role ur
        WHERE ur.user_id = u.id
    ) AS role_count
FROM app.sys_user u
WHERE u.deleted = FALSE;
```

相关子查询会对外层查询的每一行产生依赖，数据量大时需要重点分析执行计划。很多相关子查询可以改写为 JOIN 或聚合查询。

使用 JOIN 改写角色数量统计：

```sql
SELECT
    u.id,
    u.username,
    COUNT(ur.role_id) AS role_count
FROM app.sys_user u
LEFT JOIN app.sys_user_role ur ON ur.user_id = u.id
WHERE u.deleted = FALSE
GROUP BY u.id, u.username;
```

### EXISTS 查询

`EXISTS` 用于判断子查询是否存在结果。它通常适合表达“是否存在关联数据”的场景。与 `IN` 相比，`EXISTS` 更关注是否存在匹配行，而不是返回具体值列表。

查询拥有角色的用户：

```sql
SELECT
    u.id,
    u.username,
    u.nickname
FROM app.sys_user u
WHERE EXISTS (
    SELECT 1
    FROM app.sys_user_role ur
    WHERE ur.user_id = u.id
);
```

查询没有角色的用户：

```sql
SELECT
    u.id,
    u.username,
    u.nickname
FROM app.sys_user u
WHERE NOT EXISTS (
    SELECT 1
    FROM app.sys_user_role ur
    WHERE ur.user_id = u.id
);
```

查询拥有管理员角色的用户：

```sql
SELECT
    u.id,
    u.username,
    u.nickname
FROM app.sys_user u
WHERE EXISTS (
    SELECT 1
    FROM app.sys_user_role ur
    JOIN app.sys_role r ON r.id = ur.role_id
    WHERE ur.user_id = u.id
      AND r.role_code = 'ADMIN'
);
```

`EXISTS` 子查询中通常写 `SELECT 1`，因为只关心是否存在记录，不关心返回字段。实际执行时，优化器会根据语义优化查询计划。

### CASE WHEN 条件表达式

`CASE WHEN` 用于在 SQL 中进行条件判断，常用于状态翻译、分类统计、条件排序和条件聚合。

状态翻译：

```sql
SELECT
    id,
    username,
    enabled,
    CASE
        WHEN enabled THEN '启用'
        ELSE '禁用'
    END AS enabled_text
FROM app.sys_user;
```

多条件判断：

```sql
SELECT
    id,
    username,
    create_time,
    CASE
        WHEN create_time >= CURRENT_DATE THEN '今日新增'
        WHEN create_time >= CURRENT_DATE - INTERVAL '7 days' THEN '最近一周'
        ELSE '更早'
    END AS create_range
FROM app.sys_user;
```

条件聚合：

```sql
SELECT
    COUNT(*) AS total_count,
    SUM(CASE WHEN enabled THEN 1 ELSE 0 END) AS enabled_count,
    SUM(CASE WHEN NOT enabled THEN 1 ELSE 0 END) AS disabled_count
FROM app.sys_user
WHERE deleted = FALSE;
```

条件排序：

```sql
SELECT
    id,
    username,
    enabled,
    create_time
FROM app.sys_user
WHERE deleted = FALSE
ORDER BY
    CASE WHEN enabled THEN 0 ELSE 1 END,
    create_time DESC;
```

`CASE WHEN` 可以提升查询表达能力，但复杂业务规则不建议全部堆在 SQL 中。规则变化频繁时，应考虑在应用层或配置表中处理。

### COALESCE 空值处理

`COALESCE` 用于返回参数列表中第一个非空值。它常用于为空字段提供默认展示值、参与字符串拼接或处理聚合结果。

基础用法：

```sql
SELECT
    id,
    username,
    COALESCE(nickname, '未设置昵称') AS nickname
FROM app.sys_user;
```

字符串拼接时处理空值：

```sql
SELECT
    id,
    username,
    username || ' - ' || COALESCE(nickname, '') AS user_label
FROM app.sys_user;
```

聚合结果为空时返回 0：

```sql
SELECT
    COALESCE(SUM(amount), 0) AS total_amount
FROM app.account_record
WHERE user_id = 1;
```

多个候选值取第一个非空：

```sql
SELECT
    id,
    username,
    COALESCE(phone, email, '无联系方式') AS contact
FROM app.sys_user;
```

`COALESCE` 不会修改数据库中的真实值，只是改变查询结果的展示或计算结果。如果字段业务上不允许为空，应使用 `NOT NULL` 和默认值从数据层约束。

### NULLIF 值转换

`NULLIF` 用于比较两个值，如果相等则返回 `NULL`，否则返回第一个值。常用于把特殊值转换为空值，或避免除零错误。

基础用法：

```sql
SELECT NULLIF('A', 'A') AS result_equal;
```

结果为 `NULL`。

```sql
SELECT NULLIF('A', 'B') AS result_not_equal;
```

结果为 `'A'`。

将空字符串转换为 `NULL`：

```sql
SELECT
    id,
    username,
    NULLIF(trim(email), '') AS email
FROM app.sys_user;
```

避免除零错误：

```sql
SELECT
    total_count,
    success_count,
    success_count::numeric / NULLIF(total_count, 0) AS success_rate
FROM app.task_stat;
```

结合 `COALESCE` 使用：

```sql
SELECT
    total_count,
    success_count,
    COALESCE(success_count::numeric / NULLIF(total_count, 0), 0) AS success_rate
FROM app.task_stat;
```

`NULLIF` 适合处理“特殊值等价于空”的场景，例如空字符串、零分母、默认占位值等。对于入库数据，建议优先在写入时清洗；对于历史数据或报表查询，可以在查询中使用 `NULLIF` 做兼容处理。


## 表连接

表连接用于把多张表中的数据按关联条件组合成一个查询结果。PostgreSQL 支持 `INNER JOIN`、`LEFT JOIN`、`RIGHT JOIN`、`FULL JOIN`、`CROSS JOIN`、`LATERAL JOIN` 等连接方式。实际项目中最常用的是 `INNER JOIN` 和 `LEFT JOIN`。

为了便于说明，以下示例假设存在用户表、角色表、用户角色关联表、订单表和订单明细表。

```sql
-- 用户表
CREATE TABLE app.sys_user (
    id BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    username VARCHAR(64) NOT NULL,
    nickname VARCHAR(64),
    enabled BOOLEAN NOT NULL DEFAULT TRUE,
    deleted BOOLEAN NOT NULL DEFAULT FALSE,
    create_time TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- 角色表
CREATE TABLE app.sys_role (
    id BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    role_code VARCHAR(64) NOT NULL,
    role_name VARCHAR(128) NOT NULL,
    enabled BOOLEAN NOT NULL DEFAULT TRUE,
    CONSTRAINT uk_sys_role_role_code UNIQUE (role_code)
);

-- 用户角色关联表
CREATE TABLE app.sys_user_role (
    user_id BIGINT NOT NULL,
    role_id BIGINT NOT NULL,
    create_time TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT pk_sys_user_role PRIMARY KEY (user_id, role_id)
);

-- 订单表
CREATE TABLE app.order_info (
    id BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    order_no VARCHAR(64) NOT NULL,
    user_id BIGINT NOT NULL,
    order_status VARCHAR(32) NOT NULL,
    total_amount NUMERIC(12, 2) NOT NULL DEFAULT 0,
    create_time TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uk_order_info_order_no UNIQUE (order_no)
);

-- 订单明细表
CREATE TABLE app.order_item (
    id BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    order_id BIGINT NOT NULL,
    product_name VARCHAR(128) NOT NULL,
    quantity INTEGER NOT NULL,
    sale_price NUMERIC(12, 2) NOT NULL
);
```

### INNER JOIN

`INNER JOIN` 表示内连接，只返回两张表中能够匹配上的数据。如果左表或右表没有匹配记录，则该行不会出现在结果中。

查询用户及其角色：

```sql
SELECT
    u.id AS user_id,
    u.username,
    u.nickname,
    r.role_code,
    r.role_name
FROM app.sys_user u
INNER JOIN app.sys_user_role ur ON ur.user_id = u.id
INNER JOIN app.sys_role r ON r.id = ur.role_id
WHERE u.deleted = FALSE
  AND u.enabled = TRUE
  AND r.enabled = TRUE;
```

上面 SQL 只会返回已经分配角色的用户。如果某个用户没有角色，则不会出现在查询结果中。

`INNER JOIN` 适合以下场景：

| 场景                 | 说明                                   |
| -------------------- | -------------------------------------- |
| 必须存在关联数据     | 例如查询有角色的用户、有明细的订单     |
| 过滤无效关联         | 例如只查询有启用角色的用户             |
| 多表数据必须同时存在 | 例如订单、用户、支付记录都存在时才展示 |

`INNER JOIN` 的连接条件应写在 `ON` 后面，普通过滤条件通常写在 `WHERE` 后面。这样 SQL 结构更清晰，也便于后续维护。

### LEFT JOIN

`LEFT JOIN` 表示左连接，会保留左表全部数据，右表没有匹配时，右表字段返回 `NULL`。它常用于查询主表列表，并附带可选的关联信息。

查询所有用户及其角色信息：

```sql
SELECT
    u.id AS user_id,
    u.username,
    u.nickname,
    r.role_code,
    r.role_name
FROM app.sys_user u
LEFT JOIN app.sys_user_role ur ON ur.user_id = u.id
LEFT JOIN app.sys_role r ON r.id = ur.role_id
WHERE u.deleted = FALSE;
```

如果用户没有角色，`role_code` 和 `role_name` 会返回 `NULL`。

查询没有分配角色的用户：

```sql
SELECT
    u.id,
    u.username,
    u.nickname
FROM app.sys_user u
LEFT JOIN app.sys_user_role ur ON ur.user_id = u.id
WHERE u.deleted = FALSE
  AND ur.user_id IS NULL;
```

查询订单及其明细数量：

```sql
SELECT
    o.id,
    o.order_no,
    o.order_status,
    o.total_amount,
    COUNT(oi.id) AS item_count
FROM app.order_info o
LEFT JOIN app.order_item oi ON oi.order_id = o.id
GROUP BY
    o.id,
    o.order_no,
    o.order_status,
    o.total_amount
ORDER BY o.id DESC;
```

`LEFT JOIN` 使用时要特别注意过滤条件的位置。如果右表过滤条件写在 `WHERE` 中，可能会把左连接变成类似内连接的效果。

错误示例：

```sql
SELECT
    u.id,
    u.username,
    r.role_name
FROM app.sys_user u
LEFT JOIN app.sys_user_role ur ON ur.user_id = u.id
LEFT JOIN app.sys_role r ON r.id = ur.role_id
WHERE r.enabled = TRUE;
```

上面 SQL 会过滤掉没有角色的用户，因为没有角色时 `r.enabled` 为 `NULL`，不满足 `r.enabled = TRUE`。

如果需要保留没有角色的用户，应把右表条件放到 `ON` 中：

```sql
SELECT
    u.id,
    u.username,
    r.role_name
FROM app.sys_user u
LEFT JOIN app.sys_user_role ur ON ur.user_id = u.id
LEFT JOIN app.sys_role r ON r.id = ur.role_id AND r.enabled = TRUE
WHERE u.deleted = FALSE;
```

### RIGHT JOIN

`RIGHT JOIN` 表示右连接，会保留右表全部数据，左表没有匹配时，左表字段返回 `NULL`。它和 `LEFT JOIN` 语义相反，但实际项目中使用频率较低。

查询所有角色及其关联用户：

```sql
SELECT
    u.id AS user_id,
    u.username,
    r.id AS role_id,
    r.role_code,
    r.role_name
FROM app.sys_user u
RIGHT JOIN app.sys_user_role ur ON ur.user_id = u.id
RIGHT JOIN app.sys_role r ON r.id = ur.role_id
WHERE r.enabled = TRUE;
```

从可读性角度看，大多数 `RIGHT JOIN` 都可以改写成 `LEFT JOIN`。上面的 SQL 更推荐写成：

```sql
SELECT
    u.id AS user_id,
    u.username,
    r.id AS role_id,
    r.role_code,
    r.role_name
FROM app.sys_role r
LEFT JOIN app.sys_user_role ur ON ur.role_id = r.id
LEFT JOIN app.sys_user u ON u.id = ur.user_id
WHERE r.enabled = TRUE;
```

项目中建议优先使用 `LEFT JOIN`，减少团队成员在阅读 SQL 时的理解成本。

### FULL JOIN

`FULL JOIN` 表示全连接，会同时保留左右两张表的数据。能匹配上的行合并显示，不能匹配的行对应另一侧字段为 `NULL`。

`FULL JOIN` 常用于数据对账、差异比对、同步校验等场景。

示例：对比业务订单表和第三方支付流水表。

```sql
SELECT
    o.order_no AS order_no_in_order,
    p.order_no AS order_no_in_payment,
    o.total_amount AS order_amount,
    p.pay_amount,
    o.order_status,
    p.pay_status
FROM app.order_info o
FULL JOIN app.payment_record p ON p.order_no = o.order_no
WHERE o.order_no IS NULL
   OR p.order_no IS NULL
   OR o.total_amount <> p.pay_amount;
```

上面 SQL 可以找出三类问题：

| 问题                     | 表现                             |
| ------------------------ | -------------------------------- |
| 订单存在但支付流水不存在 | `p.order_no IS NULL`             |
| 支付流水存在但订单不存在 | `o.order_no IS NULL`             |
| 两边金额不一致           | `o.total_amount <> p.pay_amount` |

`FULL JOIN` 在普通业务查询中较少使用，更多用于核对、审计、数据迁移和离线校验。

### CROSS JOIN

`CROSS JOIN` 表示交叉连接，会返回两张表的笛卡尔积。左表每一行都会和右表每一行组合。

示例：生成颜色和尺码组合。

```sql
CREATE TEMP TABLE tmp_color (
    color_name VARCHAR(32) NOT NULL
);

CREATE TEMP TABLE tmp_size (
    size_name VARCHAR(32) NOT NULL
);

INSERT INTO tmp_color (color_name)
VALUES ('黑色'), ('白色'), ('蓝色');

INSERT INTO tmp_size (size_name)
VALUES ('S'), ('M'), ('L');

SELECT
    c.color_name,
    s.size_name
FROM tmp_color c
CROSS JOIN tmp_size s
ORDER BY c.color_name, s.size_name;
```

结果会生成 `3 * 3 = 9` 条组合数据。

`CROSS JOIN` 适合以下场景：

| 场景         | 说明                         |
| ------------ | ---------------------------- |
| 生成组合数据 | 例如颜色和尺码、日期和门店   |
| 构造测试数据 | 快速生成多维组合样本         |
| 维度补全     | 报表中生成完整日期和分类组合 |

使用 `CROSS JOIN` 要谨慎。如果两张表数据量较大，结果集会急剧膨胀。例如 10 万行和 10 万行交叉连接会产生 100 亿行结果。

### LATERAL JOIN

`LATERAL JOIN` 允许右侧子查询引用左侧表的字段。它适合处理“每一行主表都需要执行一个相关子查询”的场景，例如查询每个用户最新一笔订单、每个订单前几条明细、展开 JSON 或数组数据等。

查询每个用户最近一笔订单：

```sql
SELECT
    u.id AS user_id,
    u.username,
    latest_order.order_no,
    latest_order.order_status,
    latest_order.total_amount,
    latest_order.create_time AS order_create_time
FROM app.sys_user u
LEFT JOIN LATERAL (
    SELECT
        o.order_no,
        o.order_status,
        o.total_amount,
        o.create_time
    FROM app.order_info o
    WHERE o.user_id = u.id
    ORDER BY o.create_time DESC, o.id DESC
    LIMIT 1
) latest_order ON TRUE
WHERE u.deleted = FALSE;
```

查询每个订单金额最高的前 2 条明细：

```sql
SELECT
    o.order_no,
    item.product_name,
    item.quantity,
    item.sale_price
FROM app.order_info o
LEFT JOIN LATERAL (
    SELECT
        oi.product_name,
        oi.quantity,
        oi.sale_price
    FROM app.order_item oi
    WHERE oi.order_id = o.id
    ORDER BY oi.sale_price DESC, oi.id DESC
    LIMIT 2
) item ON TRUE
ORDER BY o.id DESC, item.sale_price DESC;
```

展开 JSONB 数组：

```sql
SELECT
    e.id,
    e.event_type,
    item.value ->> 'name' AS item_name,
    item.value ->> 'value' AS item_value
FROM app.event_log e
CROSS JOIN LATERAL jsonb_array_elements(e.event_data -> 'items') AS item(value);
```

`LATERAL JOIN` 的优势是可读性强，能自然表达“每一行对应一个子查询”的逻辑。需要注意的是，复杂 `LATERAL` 子查询可能对每一行执行关联计算，数据量大时必须结合索引和执行计划分析。

### 多表关联查询

多表关联查询用于在一个 SQL 中组合多个业务对象。常见场景包括订单列表带用户信息、订单明细、支付状态、角色权限等。

查询订单列表，附带用户信息和订单明细数量：

```sql
SELECT
    o.id,
    o.order_no,
    o.order_status,
    o.total_amount,
    o.create_time,
    u.username,
    u.nickname,
    COUNT(oi.id) AS item_count
FROM app.order_info o
INNER JOIN app.sys_user u ON u.id = o.user_id
LEFT JOIN app.order_item oi ON oi.order_id = o.id
WHERE u.deleted = FALSE
GROUP BY
    o.id,
    o.order_no,
    o.order_status,
    o.total_amount,
    o.create_time,
    u.username,
    u.nickname
ORDER BY o.create_time DESC, o.id DESC;
```

查询用户及其角色列表，使用字符串聚合展示角色名称：

```sql
SELECT
    u.id,
    u.username,
    u.nickname,
    STRING_AGG(r.role_name, ',' ORDER BY r.role_name) AS role_names
FROM app.sys_user u
LEFT JOIN app.sys_user_role ur ON ur.user_id = u.id
LEFT JOIN app.sys_role r ON r.id = ur.role_id
WHERE u.deleted = FALSE
GROUP BY
    u.id,
    u.username,
    u.nickname
ORDER BY u.id DESC;
```

查询订单明细，同时展示订单和用户信息：

```sql
SELECT
    o.order_no,
    o.order_status,
    u.username,
    u.nickname,
    oi.product_name,
    oi.quantity,
    oi.sale_price,
    oi.quantity * oi.sale_price AS item_amount
FROM app.order_item oi
INNER JOIN app.order_info o ON o.id = oi.order_id
INNER JOIN app.sys_user u ON u.id = o.user_id
WHERE o.order_status IN ('PAID', 'FINISHED')
ORDER BY o.create_time DESC, oi.id ASC;
```

多表关联查询建议如下：

| 建议             | 说明                                             |
| ---------------- | ------------------------------------------------ |
| 表别名要清晰     | 例如用户表 `u`、订单表 `o`、明细表 `oi`          |
| 连接条件必须完整 | 避免漏写条件导致笛卡尔积                         |
| 过滤条件分清位置 | 主表过滤放 `WHERE`，右表保留条件可放 `ON`        |
| 字段加表别名     | 避免同名字段歧义                                 |
| 先确认主表       | 以列表主体作为驱动思路，例如订单列表以订单表为主 |
| 聚合前避免重复行 | 多个一对多连接容易放大数据量                     |

### 关联查询优化

关联查询优化的核心是减少参与连接的数据量、使用正确的连接条件、让连接字段命中索引，并避免无意义的重复行放大。

常见索引设计：

```sql
-- 用户角色关联表：按用户查角色
CREATE INDEX idx_sys_user_role_user_id
ON app.sys_user_role (user_id);

-- 用户角色关联表：按角色查用户
CREATE INDEX idx_sys_user_role_role_id
ON app.sys_user_role (role_id);

-- 订单表：按用户查订单
CREATE INDEX idx_order_info_user_id
ON app.order_info (user_id);

-- 订单表：按创建时间排序
CREATE INDEX idx_order_info_create_time_id
ON app.order_info (create_time DESC, id DESC);

-- 订单明细表：按订单查明细
CREATE INDEX idx_order_item_order_id
ON app.order_item (order_id);
```

使用 `EXPLAIN` 查看执行计划：

```sql
EXPLAIN
SELECT
    o.id,
    o.order_no,
    u.username
FROM app.order_info o
INNER JOIN app.sys_user u ON u.id = o.user_id
WHERE o.create_time >= TIMESTAMP '2026-01-01 00:00:00'
ORDER BY o.create_time DESC, o.id DESC
LIMIT 20;
```

使用 `EXPLAIN ANALYZE` 查看真实执行情况：

```sql
EXPLAIN ANALYZE
SELECT
    o.id,
    o.order_no,
    u.username
FROM app.order_info o
INNER JOIN app.sys_user u ON u.id = o.user_id
WHERE o.create_time >= TIMESTAMP '2026-01-01 00:00:00'
ORDER BY o.create_time DESC, o.id DESC
LIMIT 20;
```

常见优化方向如下：

| 问题                        | 优化方式                                     |
| --------------------------- | -------------------------------------------- |
| 连接字段没有索引            | 给外键字段、关联字段创建索引                 |
| 结果行异常变多              | 检查是否多个一对多表同时 JOIN                |
| 条件写在函数上              | 避免 `date(create_time) = ...`，改成范围查询 |
| LEFT JOIN 被 WHERE 条件破坏 | 右表过滤条件按需要放入 `ON`                  |
| 分页前 JOIN 数据太大        | 先分页主表，再 JOIN 明细                     |
| 聚合后再过滤太慢            | 能放 `WHERE` 的条件不要放 `HAVING`           |
| 明细表重复导致统计错误      | 先对子表预聚合，再连接主表                   |

先分页主表，再关联用户信息：

```sql
WITH page_order AS (
    SELECT
        id,
        order_no,
        user_id,
        order_status,
        total_amount,
        create_time
    FROM app.order_info
    WHERE create_time >= TIMESTAMP '2026-01-01 00:00:00'
    ORDER BY create_time DESC, id DESC
    LIMIT 20 OFFSET 0
)
SELECT
    o.id,
    o.order_no,
    o.order_status,
    o.total_amount,
    o.create_time,
    u.username,
    u.nickname
FROM page_order o
INNER JOIN app.sys_user u ON u.id = o.user_id
ORDER BY o.create_time DESC, o.id DESC;
```

先预聚合明细，再关联订单：

```sql
WITH item_stat AS (
    SELECT
        order_id,
        COUNT(*) AS item_count,
        SUM(quantity * sale_price) AS item_total_amount
    FROM app.order_item
    GROUP BY order_id
)
SELECT
    o.order_no,
    o.total_amount,
    COALESCE(s.item_count, 0) AS item_count,
    COALESCE(s.item_total_amount, 0) AS item_total_amount
FROM app.order_info o
LEFT JOIN item_stat s ON s.order_id = o.id;
```

关联查询优化不要只靠“加索引”。应先确认 SQL 语义是否正确，再分析数据量、连接顺序、过滤条件、索引命中和结果集大小。

## 聚合与统计

聚合与统计用于对多行数据进行汇总计算，例如数量、金额、平均值、最大值、最小值、分组统计、字符串聚合、数组聚合、JSON 聚合和多维统计。PostgreSQL 的聚合能力较强，适合支撑后台报表、运营统计和数据校验。

### COUNT 统计

`COUNT` 用于统计数量。常见写法包括 `COUNT(*)`、`COUNT(字段)` 和 `COUNT(DISTINCT 字段)`。

统计总行数：

```sql
SELECT COUNT(*) AS total_count
FROM app.sys_user;
```

统计未删除用户数量：

```sql
SELECT COUNT(*) AS user_count
FROM app.sys_user
WHERE deleted = FALSE;
```

统计有邮箱的用户数量：

```sql
SELECT COUNT(email) AS email_count
FROM app.sys_user
WHERE deleted = FALSE;
```

`COUNT(email)` 只统计 `email IS NOT NULL` 的行，`COUNT(*)` 统计所有满足条件的行。

统计去重手机号数量：

```sql
SELECT COUNT(DISTINCT phone) AS phone_count
FROM app.sys_user
WHERE deleted = FALSE
  AND phone IS NOT NULL;
```

按状态统计数量：

```sql
SELECT
    enabled,
    COUNT(*) AS user_count
FROM app.sys_user
WHERE deleted = FALSE
GROUP BY enabled
ORDER BY enabled DESC;
```

常见区别如下：

| 写法                     | 说明                                 |
| ------------------------ | ------------------------------------ |
| `COUNT(*)`               | 统计所有行，推荐用于总数统计         |
| `COUNT(1)`               | 统计所有行，语义上与 `COUNT(*)` 类似 |
| `COUNT(column)`          | 统计该字段非空行                     |
| `COUNT(DISTINCT column)` | 统计去重后的非空值数量               |

业务统计总数时优先使用 `COUNT(*)`。如果要统计某个字段有值的数据，再使用 `COUNT(column)`。

### SUM 求和

`SUM` 用于求和，常用于金额、数量、积分、库存、访问次数等统计。

统计订单总金额：

```sql
SELECT
    SUM(total_amount) AS total_amount
FROM app.order_info
WHERE order_status IN ('PAID', 'FINISHED');
```

按用户统计订单金额：

```sql
SELECT
    user_id,
    SUM(total_amount) AS total_amount
FROM app.order_info
WHERE order_status IN ('PAID', 'FINISHED')
GROUP BY user_id
ORDER BY total_amount DESC;
```

统计订单明细金额：

```sql
SELECT
    order_id,
    SUM(quantity * sale_price) AS item_total_amount
FROM app.order_item
GROUP BY order_id;
```

处理空结果：

```sql
SELECT
    COALESCE(SUM(total_amount), 0) AS total_amount
FROM app.order_info
WHERE user_id = -1;
```

如果没有满足条件的行，`SUM` 会返回 `NULL`，不是 `0`。业务接口中通常需要使用 `COALESCE(SUM(...), 0)` 转成 `0`。

### AVG 平均值

`AVG` 用于计算平均值，常用于平均订单金额、平均评分、平均耗时等。

计算平均订单金额：

```sql
SELECT
    AVG(total_amount) AS avg_order_amount
FROM app.order_info
WHERE order_status IN ('PAID', 'FINISHED');
```

按用户统计平均订单金额：

```sql
SELECT
    user_id,
    COUNT(*) AS order_count,
    SUM(total_amount) AS total_amount,
    AVG(total_amount) AS avg_order_amount
FROM app.order_info
WHERE order_status IN ('PAID', 'FINISHED')
GROUP BY user_id
ORDER BY avg_order_amount DESC;
```

保留两位小数：

```sql
SELECT
    user_id,
    ROUND(AVG(total_amount), 2) AS avg_order_amount
FROM app.order_info
WHERE order_status IN ('PAID', 'FINISHED')
GROUP BY user_id;
```

计算平均值时要注意分母范围。例如只统计已支付订单，条件中必须排除未支付、已取消、退款等不应参与计算的数据。

### MIN 与 MAX

`MIN` 和 `MAX` 用于获取最小值和最大值。常用于最大金额、最小金额、最早时间、最新时间等统计。

统计订单金额范围：

```sql
SELECT
    MIN(total_amount) AS min_amount,
    MAX(total_amount) AS max_amount
FROM app.order_info
WHERE order_status IN ('PAID', 'FINISHED');
```

统计每个用户的首单时间和最近下单时间：

```sql
SELECT
    user_id,
    MIN(create_time) AS first_order_time,
    MAX(create_time) AS latest_order_time
FROM app.order_info
GROUP BY user_id;
```

查找最新订单时间后再查询订单：

```sql
SELECT
    o.id,
    o.order_no,
    o.user_id,
    o.total_amount,
    o.create_time
FROM app.order_info o
WHERE o.create_time = (
    SELECT MAX(create_time)
    FROM app.order_info
);
```

如果需要“每个用户最新一笔订单的完整记录”，不要只用 `MAX(create_time)` 后简单关联，因为可能存在同一时间多条记录。可以使用窗口函数或 `LATERAL JOIN` 处理。

### STRING_AGG 字符串聚合

`STRING_AGG` 用于把多行字符串聚合成一个字符串，常用于展示用户角色、标签、分类名称等。

按用户聚合角色名称：

```sql
SELECT
    u.id,
    u.username,
    STRING_AGG(r.role_name, ',' ORDER BY r.role_name) AS role_names
FROM app.sys_user u
LEFT JOIN app.sys_user_role ur ON ur.user_id = u.id
LEFT JOIN app.sys_role r ON r.id = ur.role_id
WHERE u.deleted = FALSE
GROUP BY
    u.id,
    u.username
ORDER BY u.id DESC;
```

去重后聚合角色编码：

```sql
SELECT
    u.id,
    u.username,
    STRING_AGG(DISTINCT r.role_code, ',' ORDER BY r.role_code) AS role_codes
FROM app.sys_user u
LEFT JOIN app.sys_user_role ur ON ur.user_id = u.id
LEFT JOIN app.sys_role r ON r.id = ur.role_id
WHERE u.deleted = FALSE
GROUP BY
    u.id,
    u.username;
```

按订单聚合商品名称：

```sql
SELECT
    o.order_no,
    STRING_AGG(oi.product_name, '、' ORDER BY oi.id) AS product_names
FROM app.order_info o
LEFT JOIN app.order_item oi ON oi.order_id = o.id
GROUP BY o.order_no;
```

`STRING_AGG` 适合展示层聚合，不适合替代关联表存储。不要把多个业务值长期拼接存到一个字段中，否则后续查询和维护会变复杂。

### ARRAY_AGG 数组聚合

`ARRAY_AGG` 用于把多行数据聚合成数组。它适合在 SQL 查询中返回数组结果，或为后续数组函数处理提供数据。

按用户聚合角色编码数组：

```sql
SELECT
    u.id,
    u.username,
    ARRAY_AGG(r.role_code ORDER BY r.role_code) AS role_codes
FROM app.sys_user u
LEFT JOIN app.sys_user_role ur ON ur.user_id = u.id
LEFT JOIN app.sys_role r ON r.id = ur.role_id
WHERE u.deleted = FALSE
GROUP BY
    u.id,
    u.username;
```

去重聚合数组：

```sql
SELECT
    u.id,
    u.username,
    ARRAY_AGG(DISTINCT r.role_code ORDER BY r.role_code) AS role_codes
FROM app.sys_user u
LEFT JOIN app.sys_user_role ur ON ur.user_id = u.id
LEFT JOIN app.sys_role r ON r.id = ur.role_id
WHERE u.deleted = FALSE
GROUP BY
    u.id,
    u.username;
```

过滤空值：

```sql
SELECT
    u.id,
    u.username,
    ARRAY_AGG(r.role_code ORDER BY r.role_code)
        FILTER (WHERE r.role_code IS NOT NULL) AS role_codes
FROM app.sys_user u
LEFT JOIN app.sys_user_role ur ON ur.user_id = u.id
LEFT JOIN app.sys_role r ON r.id = ur.role_id
WHERE u.deleted = FALSE
GROUP BY
    u.id,
    u.username;
```

如果没有匹配数据，聚合结果可能为 `NULL`。接口返回时可以转成空数组：

```sql
SELECT
    u.id,
    u.username,
    COALESCE(
        ARRAY_AGG(r.role_code ORDER BY r.role_code)
            FILTER (WHERE r.role_code IS NOT NULL),
        ARRAY[]::VARCHAR[]
    ) AS role_codes
FROM app.sys_user u
LEFT JOIN app.sys_user_role ur ON ur.user_id = u.id
LEFT JOIN app.sys_role r ON r.id = ur.role_id
WHERE u.deleted = FALSE
GROUP BY
    u.id,
    u.username;
```

### JSON_AGG JSON 聚合

`JSON_AGG` 和 `JSONB_AGG` 用于把多行数据聚合成 JSON 数组。它们适合在 SQL 中直接构造层级数据，例如订单及明细、用户及角色、分类及子项。

按用户聚合角色 JSON：

```sql
SELECT
    u.id,
    u.username,
    COALESCE(
        JSON_AGG(
            JSON_BUILD_OBJECT(
                'roleId', r.id,
                'roleCode', r.role_code,
                'roleName', r.role_name
            )
            ORDER BY r.role_code
        ) FILTER (WHERE r.id IS NOT NULL),
        '[]'::json
    ) AS roles
FROM app.sys_user u
LEFT JOIN app.sys_user_role ur ON ur.user_id = u.id
LEFT JOIN app.sys_role r ON r.id = ur.role_id
WHERE u.deleted = FALSE
GROUP BY
    u.id,
    u.username;
```

按订单聚合明细 JSON：

```sql
SELECT
    o.id,
    o.order_no,
    o.total_amount,
    COALESCE(
        JSON_AGG(
            JSON_BUILD_OBJECT(
                'productName', oi.product_name,
                'quantity', oi.quantity,
                'salePrice', oi.sale_price,
                'itemAmount', oi.quantity * oi.sale_price
            )
            ORDER BY oi.id
        ) FILTER (WHERE oi.id IS NOT NULL),
        '[]'::json
    ) AS items
FROM app.order_info o
LEFT JOIN app.order_item oi ON oi.order_id = o.id
GROUP BY
    o.id,
    o.order_no,
    o.total_amount;
```

使用 `JSONB_AGG` 构造 JSONB 数组：

```sql
SELECT
    o.id,
    o.order_no,
    JSONB_AGG(
        JSONB_BUILD_OBJECT(
            'productName', oi.product_name,
            'quantity', oi.quantity
        )
        ORDER BY oi.id
    ) FILTER (WHERE oi.id IS NOT NULL) AS items
FROM app.order_info o
LEFT JOIN app.order_item oi ON oi.order_id = o.id
GROUP BY
    o.id,
    o.order_no;
```

`JSON_AGG` 很适合报表、接口聚合和一次性读取层级数据。但如果聚合层级过深或数据量过大，建议在应用层组装，避免 SQL 过重、结果过大和内存消耗过高。

### FILTER 聚合过滤

`FILTER` 用于对聚合函数单独添加过滤条件。它比在 `SUM(CASE WHEN ...)` 中写条件更清晰，适合做多状态统计。

统计用户启用和禁用数量：

```sql
SELECT
    COUNT(*) AS total_count,
    COUNT(*) FILTER (WHERE enabled = TRUE) AS enabled_count,
    COUNT(*) FILTER (WHERE enabled = FALSE) AS disabled_count
FROM app.sys_user
WHERE deleted = FALSE;
```

统计不同订单状态的数量和金额：

```sql
SELECT
    COUNT(*) AS total_count,
    COUNT(*) FILTER (WHERE order_status = 'PENDING') AS pending_count,
    COUNT(*) FILTER (WHERE order_status = 'PAID') AS paid_count,
    COUNT(*) FILTER (WHERE order_status = 'FINISHED') AS finished_count,
    COALESCE(SUM(total_amount) FILTER (WHERE order_status = 'PAID'), 0) AS paid_amount,
    COALESCE(SUM(total_amount) FILTER (WHERE order_status = 'FINISHED'), 0) AS finished_amount
FROM app.order_info;
```

按日期统计订单状态：

```sql
SELECT
    create_time::date AS order_date,
    COUNT(*) AS total_count,
    COUNT(*) FILTER (WHERE order_status = 'PAID') AS paid_count,
    COUNT(*) FILTER (WHERE order_status = 'CANCELLED') AS cancelled_count
FROM app.order_info
GROUP BY create_time::date
ORDER BY order_date DESC;
```

`FILTER` 常用于统计看板、运营报表、状态分布和条件聚合。相比多个子查询，它可以在一次扫描中完成多项统计。

### GROUPING SETS

`GROUPING SETS` 用于在一个 SQL 中指定多个分组维度，返回多个聚合层级的结果。它适合同时查询明细维度统计和汇总统计。

按订单日期、订单状态分别统计，并增加总计：

```sql
SELECT
    create_time::date AS order_date,
    order_status,
    COUNT(*) AS order_count,
    SUM(total_amount) AS total_amount
FROM app.order_info
GROUP BY GROUPING SETS (
    (create_time::date, order_status),
    (create_time::date),
    (order_status),
    ()
)
ORDER BY order_date NULLS LAST, order_status NULLS LAST;
```

上面 SQL 会返回四类统计结果：

| 分组                                | 说明             |
| ----------------------------------- | ---------------- |
| `(create_time::date, order_status)` | 按日期和状态统计 |
| `(create_time::date)`               | 按日期统计       |
| `(order_status)`                    | 按状态统计       |
| `()`                                | 总计             |

使用 `GROUPING()` 区分汇总行：

```sql
SELECT
    create_time::date AS order_date,
    order_status,
    GROUPING(create_time::date) AS is_order_date_total,
    GROUPING(order_status) AS is_order_status_total,
    COUNT(*) AS order_count,
    SUM(total_amount) AS total_amount
FROM app.order_info
GROUP BY GROUPING SETS (
    (create_time::date, order_status),
    (create_time::date),
    (order_status),
    ()
)
ORDER BY order_date NULLS LAST, order_status NULLS LAST;
```

`GROUPING()` 返回 `1` 表示该列在当前结果行中被聚合掉，返回 `0` 表示该列参与当前分组。

### ROLLUP

`ROLLUP` 用于按层级生成小计和总计。它适合有层级关系的统计，例如日期 -> 状态、区域 -> 门店、一级分类 -> 二级分类。

按日期和订单状态生成小计：

```sql
SELECT
    create_time::date AS order_date,
    order_status,
    COUNT(*) AS order_count,
    SUM(total_amount) AS total_amount
FROM app.order_info
GROUP BY ROLLUP (create_time::date, order_status)
ORDER BY order_date NULLS LAST, order_status NULLS LAST;
```

等价于生成以下分组：

| 分组                                | 说明             |
| ----------------------------------- | ---------------- |
| `(create_time::date, order_status)` | 每天每种状态统计 |
| `(create_time::date)`               | 每天小计         |
| `()`                                | 总计             |

使用 `GROUPING()` 标识小计和总计：

```sql
SELECT
    CASE
        WHEN GROUPING(create_time::date) = 1 THEN '总计'
        ELSE create_time::date::text
    END AS order_date_label,
    CASE
        WHEN GROUPING(order_status) = 1 THEN '小计'
        ELSE order_status
    END AS order_status_label,
    COUNT(*) AS order_count,
    SUM(total_amount) AS total_amount
FROM app.order_info
GROUP BY ROLLUP (create_time::date, order_status)
ORDER BY
    GROUPING(create_time::date),
    create_time::date,
    GROUPING(order_status),
    order_status;
```

`ROLLUP` 的字段顺序很重要。`ROLLUP(a, b, c)` 会按 `a -> b -> c` 的层级生成汇总，不是任意组合统计。

### CUBE

`CUBE` 用于生成多个维度的所有组合统计。它适合多维分析，例如按日期、状态、渠道分别统计以及组合统计。

按日期和订单状态生成所有组合统计：

```sql
SELECT
    create_time::date AS order_date,
    order_status,
    COUNT(*) AS order_count,
    SUM(total_amount) AS total_amount
FROM app.order_info
GROUP BY CUBE (create_time::date, order_status)
ORDER BY order_date NULLS LAST, order_status NULLS LAST;
```

`CUBE(create_time::date, order_status)` 会生成以下分组：

| 分组                                | 说明        |
| ----------------------------------- | ----------- |
| `(create_time::date, order_status)` | 日期 + 状态 |
| `(create_time::date)`               | 日期        |
| `(order_status)`                    | 状态        |
| `()`                                | 总计        |

三维统计示例：

```sql
SELECT
    create_time::date AS order_date,
    order_status,
    user_id,
    COUNT(*) AS order_count,
    SUM(total_amount) AS total_amount
FROM app.order_info
GROUP BY CUBE (create_time::date, order_status, user_id)
ORDER BY
    order_date NULLS LAST,
    order_status NULLS LAST,
    user_id NULLS LAST;
```

`CUBE` 会生成所有维度组合，维度越多，结果集越大。`CUBE(a, b, c)` 会生成 8 类分组，`CUBE(a, b, c, d)` 会生成 16 类分组。因此生产报表中应控制维度数量，并尽量提前过滤数据范围。

`GROUPING SETS`、`ROLLUP` 和 `CUBE` 的选择建议如下：

| 语法            | 适用场景             |
| --------------- | -------------------- |
| `GROUPING SETS` | 明确指定需要哪些分组 |
| `ROLLUP`        | 按层级生成小计和总计 |
| `CUBE`          | 生成所有维度组合统计 |

普通业务统计优先使用 `GROUP BY` 和 `FILTER`。只有在报表需要多层小计、多维组合或一次 SQL 返回多种统计粒度时，再使用 `GROUPING SETS`、`ROLLUP` 或 `CUBE`。


## 窗口函数

窗口函数用于在不压缩结果行的情况下，对当前行所在的一组数据进行排序、排名、累计、前后行比较和分区统计。它和 `GROUP BY` 的区别是：`GROUP BY` 会把多行聚合成一行，而窗口函数会保留原始行，同时额外计算分析结果。

窗口函数的基本语法如下：

```sql
窗口函数() OVER (
    PARTITION BY 分区字段
    ORDER BY 排序字段
    ROWS BETWEEN 起始边界 AND 结束边界
)
```

常见组成部分说明如下：

| 组成部分         | 说明                                          |
| ---------------- | --------------------------------------------- |
| `OVER`           | 声明这是一个窗口函数                          |
| `PARTITION BY`   | 按字段分区，类似分组，但不会合并行            |
| `ORDER BY`       | 指定窗口内排序方式                            |
| `ROWS` / `RANGE` | 指定窗口计算范围                              |
| 排名函数         | 如 `ROW_NUMBER()`、`RANK()`、`DENSE_RANK()`   |
| 偏移函数         | 如 `LAG()`、`LEAD()`                          |
| 首尾函数         | 如 `FIRST_VALUE()`、`LAST_VALUE()`            |
| 聚合窗口函数     | 如 `SUM() OVER`、`AVG() OVER`、`COUNT() OVER` |

以下示例主要基于订单表 `app.order_info`：

```sql
CREATE TABLE app.order_info (
    id BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    order_no VARCHAR(64) NOT NULL,
    user_id BIGINT NOT NULL,
    order_status VARCHAR(32) NOT NULL,
    total_amount NUMERIC(12, 2) NOT NULL DEFAULT 0,
    create_time TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uk_order_info_order_no UNIQUE (order_no)
);
```

### ROW_NUMBER

`ROW_NUMBER()` 用于为窗口内的每一行生成连续序号。即使排序字段的值相同，也会分配不同的行号。

查询订单列表并生成全局序号：

```sql
SELECT
    ROW_NUMBER() OVER (ORDER BY create_time DESC, id DESC) AS row_no,
    id,
    order_no,
    user_id,
    order_status,
    total_amount,
    create_time
FROM app.order_info
ORDER BY create_time DESC, id DESC;
```

按用户分区，为每个用户的订单生成序号：

```sql
SELECT
    ROW_NUMBER() OVER (
        PARTITION BY user_id
        ORDER BY create_time DESC, id DESC
    ) AS user_order_no,
    id,
    order_no,
    user_id,
    order_status,
    total_amount,
    create_time
FROM app.order_info
ORDER BY user_id, user_order_no;
```

查询每个用户最新一笔订单：

```sql
WITH ranked_order AS (
    SELECT
        ROW_NUMBER() OVER (
            PARTITION BY user_id
            ORDER BY create_time DESC, id DESC
        ) AS rn,
        id,
        order_no,
        user_id,
        order_status,
        total_amount,
        create_time
    FROM app.order_info
)
SELECT
    id,
    order_no,
    user_id,
    order_status,
    total_amount,
    create_time
FROM ranked_order
WHERE rn = 1;
```

`ROW_NUMBER()` 常用于分页、去重、取每组第一条、取每组最新记录等场景。它不关心排序字段是否相同，只保证结果中每行都有唯一序号。

### RANK

`RANK()` 用于排名。排序值相同时排名相同，但后续排名会跳号。

按订单金额进行全局排名：

```sql
SELECT
    RANK() OVER (ORDER BY total_amount DESC) AS amount_rank,
    id,
    order_no,
    user_id,
    total_amount,
    create_time
FROM app.order_info
ORDER BY amount_rank, id;
```

如果金额排名结果是：

| 金额 | `RANK()` |
| ---- | -------- |
| 1000 | 1        |
| 1000 | 1        |
| 800  | 3        |
| 500  | 4        |

可以看到，两个订单并列第 1 后，下一个排名是第 3。

按用户分区，对每个用户的订单金额排名：

```sql
SELECT
    RANK() OVER (
        PARTITION BY user_id
        ORDER BY total_amount DESC
    ) AS user_amount_rank,
    id,
    order_no,
    user_id,
    total_amount,
    create_time
FROM app.order_info
ORDER BY user_id, user_amount_rank, id;
```

查询每个用户订单金额排名前三的数据：

```sql
WITH ranked_order AS (
    SELECT
        RANK() OVER (
            PARTITION BY user_id
            ORDER BY total_amount DESC
        ) AS amount_rank,
        id,
        order_no,
        user_id,
        total_amount,
        create_time
    FROM app.order_info
)
SELECT
    id,
    order_no,
    user_id,
    total_amount,
    amount_rank
FROM ranked_order
WHERE amount_rank <= 3
ORDER BY user_id, amount_rank, id;
```

`RANK()` 适合需要体现并列名次，并允许排名跳号的业务场景，例如竞赛排名、销售榜单、金额排行榜。

### DENSE_RANK

`DENSE_RANK()` 也是排名函数。与 `RANK()` 不同的是，排序值相同时排名相同，但后续排名不会跳号。

按订单金额进行密集排名：

```sql
SELECT
    DENSE_RANK() OVER (ORDER BY total_amount DESC) AS amount_rank,
    id,
    order_no,
    user_id,
    total_amount,
    create_time
FROM app.order_info
ORDER BY amount_rank, id;
```

如果金额排名结果是：

| 金额 | `DENSE_RANK()` |
| ---- | -------------- |
| 1000 | 1              |
| 1000 | 1              |
| 800  | 2              |
| 500  | 3              |

可以看到，两个订单并列第 1 后，下一个排名是第 2。

对比 `RANK()` 和 `DENSE_RANK()`：

```sql
SELECT
    id,
    order_no,
    total_amount,
    RANK() OVER (ORDER BY total_amount DESC) AS rank_no,
    DENSE_RANK() OVER (ORDER BY total_amount DESC) AS dense_rank_no
FROM app.order_info
ORDER BY total_amount DESC, id;
```

按用户统计订单金额密集排名：

```sql
SELECT
    DENSE_RANK() OVER (
        PARTITION BY user_id
        ORDER BY total_amount DESC
    ) AS user_amount_rank,
    id,
    order_no,
    user_id,
    total_amount
FROM app.order_info
ORDER BY user_id, user_amount_rank, id;
```

`DENSE_RANK()` 适合需要连续排名的业务场景，例如会员等级排行、商品销量排行、部门业绩排行。

### LAG 与 LEAD

`LAG()` 用于获取当前行之前的某一行数据，`LEAD()` 用于获取当前行之后的某一行数据。它们常用于环比、同比、状态流转、前后记录差值计算等场景。

查询每个用户订单与上一笔订单的金额差：

```sql
SELECT
    id,
    order_no,
    user_id,
    total_amount,
    create_time,
    LAG(total_amount) OVER (
        PARTITION BY user_id
        ORDER BY create_time ASC, id ASC
    ) AS prev_amount,
    total_amount - LAG(total_amount) OVER (
        PARTITION BY user_id
        ORDER BY create_time ASC, id ASC
    ) AS amount_diff
FROM app.order_info
ORDER BY user_id, create_time ASC, id ASC;
```

查询每个用户下一笔订单时间：

```sql
SELECT
    id,
    order_no,
    user_id,
    create_time,
    LEAD(create_time) OVER (
        PARTITION BY user_id
        ORDER BY create_time ASC, id ASC
    ) AS next_order_time
FROM app.order_info
ORDER BY user_id, create_time ASC, id ASC;
```

`LAG()` 和 `LEAD()` 可以指定偏移量和默认值：

```sql
SELECT
    id,
    order_no,
    user_id,
    total_amount,
    LAG(total_amount, 1, 0) OVER (
        PARTITION BY user_id
        ORDER BY create_time ASC, id ASC
    ) AS prev_amount,
    LEAD(total_amount, 1, 0) OVER (
        PARTITION BY user_id
        ORDER BY create_time ASC, id ASC
    ) AS next_amount
FROM app.order_info
ORDER BY user_id, create_time ASC, id ASC;
```

参数说明如下：

| 写法                | 说明                                     |
| ------------------- | ---------------------------------------- |
| `LAG(column)`       | 获取上一行该字段值                       |
| `LAG(column, 2)`    | 获取前 2 行该字段值                      |
| `LAG(column, 1, 0)` | 获取上一行该字段值，如果不存在则返回 `0` |
| `LEAD(column)`      | 获取下一行该字段值                       |
| `LEAD(column, 2)`   | 获取后 2 行该字段值                      |

统计每日订单金额与前一天的差值：

```sql
WITH daily_order AS (
    SELECT
        create_time::date AS order_date,
        SUM(total_amount) AS total_amount
    FROM app.order_info
    WHERE order_status IN ('PAID', 'FINISHED')
    GROUP BY create_time::date
)
SELECT
    order_date,
    total_amount,
    LAG(total_amount) OVER (ORDER BY order_date) AS prev_total_amount,
    total_amount - LAG(total_amount) OVER (ORDER BY order_date) AS amount_diff
FROM daily_order
ORDER BY order_date;
```

`LAG()` 和 `LEAD()` 不会改变行数，只是在当前结果行上增加前后行信息。用于报表环比时，应先聚合到目标粒度，再使用窗口函数计算差值。

### FIRST_VALUE 与 LAST_VALUE

`FIRST_VALUE()` 用于获取窗口内第一行的值，`LAST_VALUE()` 用于获取窗口内最后一行的值。它们常用于获取首单、末单、初始状态、最新状态等数据。

查询每个用户第一笔订单金额：

```sql
SELECT
    id,
    order_no,
    user_id,
    total_amount,
    create_time,
    FIRST_VALUE(total_amount) OVER (
        PARTITION BY user_id
        ORDER BY create_time ASC, id ASC
    ) AS first_order_amount
FROM app.order_info
ORDER BY user_id, create_time ASC, id ASC;
```

查询每个用户第一笔订单号：

```sql
SELECT
    id,
    order_no,
    user_id,
    create_time,
    FIRST_VALUE(order_no) OVER (
        PARTITION BY user_id
        ORDER BY create_time ASC, id ASC
    ) AS first_order_no
FROM app.order_info
ORDER BY user_id, create_time ASC, id ASC;
```

使用 `LAST_VALUE()` 时要特别注意窗口范围。默认窗口范围通常截止到当前行，因此直接使用 `LAST_VALUE()` 可能得到当前行的值，而不是分区内真正最后一行的值。

错误或容易误解的写法：

```sql
SELECT
    id,
    order_no,
    user_id,
    total_amount,
    LAST_VALUE(total_amount) OVER (
        PARTITION BY user_id
        ORDER BY create_time ASC, id ASC
    ) AS last_order_amount
FROM app.order_info;
```

推荐显式指定窗口范围：

```sql
SELECT
    id,
    order_no,
    user_id,
    total_amount,
    create_time,
    LAST_VALUE(total_amount) OVER (
        PARTITION BY user_id
        ORDER BY create_time ASC, id ASC
        ROWS BETWEEN UNBOUNDED PRECEDING AND UNBOUNDED FOLLOWING
    ) AS last_order_amount
FROM app.order_info
ORDER BY user_id, create_time ASC, id ASC;
```

窗口范围说明如下：

| 写法                                                       | 说明             |
| ---------------------------------------------------------- | ---------------- |
| `UNBOUNDED PRECEDING`                                      | 从分区第一行开始 |
| `CURRENT ROW`                                              | 到当前行         |
| `UNBOUNDED FOLLOWING`                                      | 到分区最后一行   |
| `ROWS BETWEEN UNBOUNDED PRECEDING AND UNBOUNDED FOLLOWING` | 当前分区全部行   |

如果只是查询每组第一条或最后一条完整记录，`ROW_NUMBER()` 通常更直观。如果是在每一行上展示首值或末值，则适合使用 `FIRST_VALUE()` 和 `LAST_VALUE()`。

### SUM OVER

`SUM() OVER` 用于在保留明细行的同时计算累计值、分区总和或移动窗口求和。

查询每笔订单，并展示该用户订单总金额：

```sql
SELECT
    id,
    order_no,
    user_id,
    total_amount,
    SUM(total_amount) OVER (
        PARTITION BY user_id
    ) AS user_total_amount
FROM app.order_info
WHERE order_status IN ('PAID', 'FINISHED')
ORDER BY user_id, id;
```

查询每个用户的订单累计金额：

```sql
SELECT
    id,
    order_no,
    user_id,
    total_amount,
    create_time,
    SUM(total_amount) OVER (
        PARTITION BY user_id
        ORDER BY create_time ASC, id ASC
        ROWS BETWEEN UNBOUNDED PRECEDING AND CURRENT ROW
    ) AS running_total_amount
FROM app.order_info
WHERE order_status IN ('PAID', 'FINISHED')
ORDER BY user_id, create_time ASC, id ASC;
```

查询每日订单金额和累计金额：

```sql
WITH daily_order AS (
    SELECT
        create_time::date AS order_date,
        SUM(total_amount) AS daily_amount
    FROM app.order_info
    WHERE order_status IN ('PAID', 'FINISHED')
    GROUP BY create_time::date
)
SELECT
    order_date,
    daily_amount,
    SUM(daily_amount) OVER (
        ORDER BY order_date
        ROWS BETWEEN UNBOUNDED PRECEDING AND CURRENT ROW
    ) AS running_amount
FROM daily_order
ORDER BY order_date;
```

计算最近 7 天滚动订单金额：

```sql
WITH daily_order AS (
    SELECT
        create_time::date AS order_date,
        SUM(total_amount) AS daily_amount
    FROM app.order_info
    WHERE order_status IN ('PAID', 'FINISHED')
    GROUP BY create_time::date
)
SELECT
    order_date,
    daily_amount,
    SUM(daily_amount) OVER (
        ORDER BY order_date
        ROWS BETWEEN 6 PRECEDING AND CURRENT ROW
    ) AS last_7_rows_amount
FROM daily_order
ORDER BY order_date;
```

`SUM() OVER` 和普通 `SUM()` 的区别是，普通 `SUM()` 通常配合 `GROUP BY` 压缩结果行，而 `SUM() OVER` 会保留每一行，同时计算窗口内的合计值。

### AVG OVER

`AVG() OVER` 用于在保留明细行的同时计算平均值，常用于用户平均订单金额、移动平均、部门平均值对比等场景。

查询每笔订单，并展示该用户平均订单金额：

```sql
SELECT
    id,
    order_no,
    user_id,
    total_amount,
    AVG(total_amount) OVER (
        PARTITION BY user_id
    ) AS user_avg_amount
FROM app.order_info
WHERE order_status IN ('PAID', 'FINISHED')
ORDER BY user_id, id;
```

查询每笔订单金额与用户平均金额的差值：

```sql
SELECT
    id,
    order_no,
    user_id,
    total_amount,
    ROUND(AVG(total_amount) OVER (PARTITION BY user_id), 2) AS user_avg_amount,
    total_amount - ROUND(AVG(total_amount) OVER (PARTITION BY user_id), 2) AS diff_from_user_avg
FROM app.order_info
WHERE order_status IN ('PAID', 'FINISHED')
ORDER BY user_id, total_amount DESC;
```

计算每日订单金额的 7 行移动平均：

```sql
WITH daily_order AS (
    SELECT
        create_time::date AS order_date,
        SUM(total_amount) AS daily_amount
    FROM app.order_info
    WHERE order_status IN ('PAID', 'FINISHED')
    GROUP BY create_time::date
)
SELECT
    order_date,
    daily_amount,
    ROUND(
        AVG(daily_amount) OVER (
            ORDER BY order_date
            ROWS BETWEEN 6 PRECEDING AND CURRENT ROW
        ),
        2
    ) AS last_7_rows_avg_amount
FROM daily_order
ORDER BY order_date;
```

`AVG() OVER` 适合对当前行与整体均值、分组均值、移动均值进行对比。报表中如果需要展示“当前值、平均值、与平均值差异”，窗口函数通常比多个子查询更清晰。

### PARTITION BY

`PARTITION BY` 用于定义窗口分区。窗口函数会在每个分区内部独立计算。它类似 `GROUP BY` 的分组概念，但不会把多行压缩成一行。

不使用 `PARTITION BY` 时，整个查询结果是一个窗口：

```sql
SELECT
    id,
    order_no,
    user_id,
    total_amount,
    SUM(total_amount) OVER () AS all_total_amount
FROM app.order_info
WHERE order_status IN ('PAID', 'FINISHED');
```

使用 `PARTITION BY user_id` 后，每个用户是一个独立窗口：

```sql
SELECT
    id,
    order_no,
    user_id,
    total_amount,
    SUM(total_amount) OVER (
        PARTITION BY user_id
    ) AS user_total_amount
FROM app.order_info
WHERE order_status IN ('PAID', 'FINISHED')
ORDER BY user_id, id;
```

按多个字段分区：

```sql
SELECT
    id,
    order_no,
    user_id,
    order_status,
    total_amount,
    COUNT(*) OVER (
        PARTITION BY user_id, order_status
    ) AS user_status_order_count
FROM app.order_info
ORDER BY user_id, order_status, id;
```

`PARTITION BY` 和 `GROUP BY` 的区别如下：

| 对比项               | `PARTITION BY`                 | `GROUP BY`       |
| -------------------- | ------------------------------ | ---------------- |
| 是否保留明细行       | 保留                           | 不保留           |
| 是否改变结果行数     | 通常不改变                     | 会合并行         |
| 常见用途             | 排名、累计、前后对比、分区统计 | 汇总统计         |
| 是否必须配合聚合函数 | 不一定                         | 通常配合聚合函数 |
| 是否可返回非分组字段 | 可以                           | 通常不可以       |

如果需要“每行数据 + 分组统计结果”，优先考虑窗口函数。如果只需要“每组一行统计结果”，使用 `GROUP BY` 更合适。

### 窗口函数分页

窗口函数可以用于分页，特别是需要先对复杂查询结果编号，再按编号截取数据的场景。常用方式是使用 `ROW_NUMBER()` 生成行号，然后在外层查询过滤行号范围。

使用窗口函数实现分页：

```sql
WITH user_page AS (
    SELECT
        ROW_NUMBER() OVER (
            ORDER BY create_time DESC, id DESC
        ) AS row_no,
        id,
        username,
        nickname,
        enabled,
        create_time
    FROM app.sys_user
    WHERE deleted = FALSE
)
SELECT
    row_no,
    id,
    username,
    nickname,
    enabled,
    create_time
FROM user_page
WHERE row_no BETWEEN 1 AND 10
ORDER BY row_no;
```

查询第 2 页，每页 10 条：

```sql
WITH user_page AS (
    SELECT
        ROW_NUMBER() OVER (
            ORDER BY create_time DESC, id DESC
        ) AS row_no,
        id,
        username,
        nickname,
        enabled,
        create_time
    FROM app.sys_user
    WHERE deleted = FALSE
)
SELECT
    row_no,
    id,
    username,
    nickname,
    enabled,
    create_time
FROM user_page
WHERE row_no BETWEEN 11 AND 20
ORDER BY row_no;
```

分页时同时返回总数：

```sql
WITH user_page AS (
    SELECT
        ROW_NUMBER() OVER (
            ORDER BY create_time DESC, id DESC
        ) AS row_no,
        COUNT(*) OVER () AS total_count,
        id,
        username,
        nickname,
        enabled,
        create_time
    FROM app.sys_user
    WHERE deleted = FALSE
)
SELECT
    row_no,
    total_count,
    id,
    username,
    nickname,
    enabled,
    create_time
FROM user_page
WHERE row_no BETWEEN 1 AND 10
ORDER BY row_no;
```

窗口函数分页和 `LIMIT OFFSET` 对比：

| 分页方式            | 优点                                       | 注意事项                   |
| ------------------- | ------------------------------------------ | -------------------------- |
| `LIMIT OFFSET`      | 简单直观，适合普通分页                     | 深分页性能可能下降         |
| `ROW_NUMBER()` 分页 | 适合复杂查询编号、返回总数、按计算结果分页 | 仍需要对完整候选集排序编号 |
| 游标分页            | 适合大数据量滚动加载                       | 只能按固定游标方向翻页     |

普通后台列表优先使用 `LIMIT OFFSET`。当需要对复杂结果集编号、同时返回总数、或按窗口计算结果筛选时，可以使用窗口函数分页。

### 窗口函数排名

窗口函数排名常用于榜单、每组 Top N、最新记录、最高金额记录、销售排行和数据去重。常用函数包括 `ROW_NUMBER()`、`RANK()` 和 `DENSE_RANK()`。

三种排名函数对比如下：

| 函数           | 并列时是否相同排名 | 后续排名是否跳号 | 适用场景                   |
| -------------- | ------------------ | ---------------- | -------------------------- |
| `ROW_NUMBER()` | 否                 | 否               | 唯一序号、分页、每组取一条 |
| `RANK()`       | 是                 | 是               | 竞赛排名、允许跳号         |
| `DENSE_RANK()` | 是                 | 否               | 连续排名、等级排名         |

对订单金额进行三种排名对比：

```sql
SELECT
    id,
    order_no,
    user_id,
    total_amount,
    ROW_NUMBER() OVER (
        ORDER BY total_amount DESC, id DESC
    ) AS row_number_no,
    RANK() OVER (
        ORDER BY total_amount DESC
    ) AS rank_no,
    DENSE_RANK() OVER (
        ORDER BY total_amount DESC
    ) AS dense_rank_no
FROM app.order_info
WHERE order_status IN ('PAID', 'FINISHED')
ORDER BY total_amount DESC, id DESC;
```

查询每个用户订单金额最高的一笔订单：

```sql
WITH ranked_order AS (
    SELECT
        ROW_NUMBER() OVER (
            PARTITION BY user_id
            ORDER BY total_amount DESC, id DESC
        ) AS rn,
        id,
        order_no,
        user_id,
        total_amount,
        create_time
    FROM app.order_info
    WHERE order_status IN ('PAID', 'FINISHED')
)
SELECT
    id,
    order_no,
    user_id,
    total_amount,
    create_time
FROM ranked_order
WHERE rn = 1
ORDER BY total_amount DESC;
```

查询每个用户订单金额前三名，允许并列：

```sql
WITH ranked_order AS (
    SELECT
        RANK() OVER (
            PARTITION BY user_id
            ORDER BY total_amount DESC
        ) AS amount_rank,
        id,
        order_no,
        user_id,
        total_amount,
        create_time
    FROM app.order_info
    WHERE order_status IN ('PAID', 'FINISHED')
)
SELECT
    id,
    order_no,
    user_id,
    total_amount,
    amount_rank,
    create_time
FROM ranked_order
WHERE amount_rank <= 3
ORDER BY user_id, amount_rank, total_amount DESC;
```

查询每个用户订单金额前三个等级，连续排名：

```sql
WITH ranked_order AS (
    SELECT
        DENSE_RANK() OVER (
            PARTITION BY user_id
            ORDER BY total_amount DESC
        ) AS amount_rank,
        id,
        order_no,
        user_id,
        total_amount,
        create_time
    FROM app.order_info
    WHERE order_status IN ('PAID', 'FINISHED')
)
SELECT
    id,
    order_no,
    user_id,
    total_amount,
    amount_rank,
    create_time
FROM ranked_order
WHERE amount_rank <= 3
ORDER BY user_id, amount_rank, total_amount DESC;
```

窗口函数排名建议如下：

| 场景                 | 推荐函数                   |
| -------------------- | -------------------------- |
| 每组取最新一条       | `ROW_NUMBER()`             |
| 每组取金额最高一条   | `ROW_NUMBER()`             |
| 榜单允许并列并跳号   | `RANK()`                   |
| 榜单允许并列但不跳号 | `DENSE_RANK()`             |
| 分页编号             | `ROW_NUMBER()`             |
| Top N 且包含并列     | `RANK()` 或 `DENSE_RANK()` |

排名查询必须有明确排序条件。如果排序字段可能相同，建议追加主键作为稳定排序字段，例如 `ORDER BY total_amount DESC, id DESC`。这样可以避免相同金额时结果顺序不稳定。


## JSON 与 JSONB 使用

PostgreSQL 同时支持 `json` 和 `jsonb` 类型，适合存储半结构化数据，例如扩展属性、事件内容、接口报文、配置快照、动态表单数据等。项目中更常用的是 `jsonb`，因为它支持更丰富的查询、索引和操作能力。

### JSON 与 JSONB 区别

`json` 和 `jsonb` 都可以存储 JSON 数据，但存储方式和适用场景不同。选择时应根据是否需要查询、索引和结构化处理来判断。

| 类型    | 存储方式         | 特点                               | 适用场景                     |
| ------- | ---------------- | ---------------------------------- | ---------------------------- |
| `json`  | 原始文本格式     | 保留输入文本格式，包括空格和键顺序 | 需要保留原始 JSON 文本的场景 |
| `jsonb` | 二进制结构化格式 | 写入时解析，查询和索引能力更强     | 大多数业务场景推荐使用       |
| `json`  | 写入相对轻       | 查询处理能力弱                     | 只存储、不频繁查询           |
| `jsonb` | 写入时有解析成本 | 支持 GIN 索引、包含判断、路径查询  | 动态属性、搜索、过滤、统计   |

创建对比表：

```sql
CREATE TABLE app.json_demo (
    id BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    json_data JSON,
    jsonb_data JSONB,
    create_time TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);
```

写入相同数据：

```sql
INSERT INTO app.json_demo (
    json_data,
    jsonb_data
) VALUES (
    '{"name": "PostgreSQL", "version": 17, "tags": ["database", "sql"]}',
    '{"name": "PostgreSQL", "version": 17, "tags": ["database", "sql"]}'
);
```

查询 JSON 字段：

```sql
SELECT
    json_data ->> 'name' AS json_name,
    jsonb_data ->> 'name' AS jsonb_name
FROM app.json_demo;
```

项目选择建议如下：

| 场景                   | 推荐类型                    |
| ---------------------- | --------------------------- |
| 只保存第三方原始报文   | `json` 或 `text`            |
| 保存并查询动态属性     | `jsonb`                     |
| 需要按 JSON 字段过滤   | `jsonb`                     |
| 需要为 JSON 字段建索引 | `jsonb`                     |
| 需要 JSON 包含判断     | `jsonb`                     |
| 日志类数据只写少查     | `jsonb` 或按成本选择 `text` |

普通业务系统中，如果没有保留原始 JSON 文本格式的特殊要求，优先使用 `jsonb`。

### JSONB 字段设计

`jsonb` 字段适合存储结构不固定、字段变化频繁、扩展性要求高的数据。但它不应替代正常的关系型建模。核心业务字段、查询字段、关联字段和统计字段应优先设计为普通列。

适合使用 `jsonb` 的字段：

| 字段            | 说明         |
| --------------- | ------------ |
| `extra_data`    | 扩展属性     |
| `request_body`  | 接口请求报文 |
| `response_body` | 接口响应报文 |
| `event_data`    | 事件内容     |
| `form_data`     | 动态表单数据 |
| `config_data`   | 配置快照     |
| `snapshot_data` | 业务快照     |

不建议只用 `jsonb` 存储所有业务数据。例如订单表中，订单号、用户 ID、状态、金额、创建时间等字段应作为普通列，扩展信息可以放入 `jsonb`。

推荐设计示例：

```sql
CREATE TABLE app.order_info (
    id BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    order_no VARCHAR(64) NOT NULL,
    user_id BIGINT NOT NULL,
    order_status VARCHAR(32) NOT NULL,
    total_amount NUMERIC(12, 2) NOT NULL DEFAULT 0,
    extra_data JSONB NOT NULL DEFAULT '{}'::jsonb,
    create_time TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_time TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uk_order_info_order_no UNIQUE (order_no)
);

COMMENT ON TABLE app.order_info IS '订单信息表';
COMMENT ON COLUMN app.order_info.id IS '主键ID';
COMMENT ON COLUMN app.order_info.order_no IS '订单编号';
COMMENT ON COLUMN app.order_info.user_id IS '用户ID';
COMMENT ON COLUMN app.order_info.order_status IS '订单状态';
COMMENT ON COLUMN app.order_info.total_amount IS '订单总金额';
COMMENT ON COLUMN app.order_info.extra_data IS '订单扩展数据';
COMMENT ON COLUMN app.order_info.create_time IS '创建时间';
COMMENT ON COLUMN app.order_info.update_time IS '更新时间';
```

事件日志表设计示例：

```sql
CREATE TABLE app.event_log (
    id BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    event_type VARCHAR(64) NOT NULL,
    business_id VARCHAR(128),
    event_data JSONB NOT NULL DEFAULT '{}'::jsonb,
    create_time TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

COMMENT ON TABLE app.event_log IS '事件日志表';
COMMENT ON COLUMN app.event_log.id IS '主键ID';
COMMENT ON COLUMN app.event_log.event_type IS '事件类型';
COMMENT ON COLUMN app.event_log.business_id IS '业务ID';
COMMENT ON COLUMN app.event_log.event_data IS '事件数据';
COMMENT ON COLUMN app.event_log.create_time IS '创建时间';
```

字段设计建议如下：

| 建议                     | 说明                                                |
| ------------------------ | --------------------------------------------------- |
| 核心字段普通列化         | 订单号、状态、金额、用户 ID 不要放入 JSONB          |
| JSONB 字段设置默认值     | 推荐 `DEFAULT '{}'::jsonb` 或 `DEFAULT '[]'::jsonb` |
| 明确 JSON 结构约定       | 在文档中说明字段含义，避免随意写入                  |
| 高频查询字段建表达式索引 | 例如 `(extra_data ->> 'channel')`                   |
| 大报文谨慎入库           | 大 JSON 会增加存储、备份和查询成本                  |
| 不滥用嵌套层级           | 嵌套过深会降低可读性和查询维护性                    |

### JSON 数据写入

JSON 数据写入时，可以直接使用字符串字面量并转换为 `jsonb`，也可以使用 PostgreSQL 的 JSON 构造函数生成 JSON 对象或数组。

直接写入 JSONB：

```sql
INSERT INTO app.event_log (
    event_type,
    business_id,
    event_data
) VALUES (
    'USER_LOGIN',
    '10001',
    '{
        "userId": 10001,
        "username": "admin",
        "clientIp": "127.0.0.1",
        "success": true,
        "loginTime": "2026-05-09 10:00:00"
    }'::jsonb
);
```

写入订单扩展数据：

```sql
INSERT INTO app.order_info (
    order_no,
    user_id,
    order_status,
    total_amount,
    extra_data
) VALUES (
    'PO202605090001',
    10001,
    'PAID',
    199.90,
    '{
        "channel": "APP",
        "couponId": 30001,
        "remark": "首单优惠",
        "delivery": {
            "receiver": "张三",
            "phone": "13800000000",
            "address": "杭州市西湖区"
        }
    }'::jsonb
);
```

使用 `jsonb_build_object` 构造 JSONB 对象：

```sql
INSERT INTO app.event_log (
    event_type,
    business_id,
    event_data
)
SELECT
    'ORDER_CREATED',
    'PO202605090001',
    jsonb_build_object(
        'orderNo', 'PO202605090001',
        'userId', 10001,
        'amount', 199.90,
        'status', 'CREATED',
        'createTime', CURRENT_TIMESTAMP
    );
```

使用 `jsonb_build_array` 构造 JSONB 数组：

```sql
INSERT INTO app.event_log (
    event_type,
    business_id,
    event_data
)
VALUES (
    'TAG_SELECTED',
    '10001',
    jsonb_build_object(
        'tags',
        jsonb_build_array('database', 'postgresql', 'jsonb')
    )
);
```

批量写入 JSONB：

```sql
INSERT INTO app.event_log (
    event_type,
    business_id,
    event_data
) VALUES
    (
        'USER_LOGIN',
        '10001',
        '{"username": "admin", "success": true}'::jsonb
    ),
    (
        'USER_LOGOUT',
        '10001',
        '{"username": "admin", "reason": "manual"}'::jsonb
    ),
    (
        'USER_LOGIN',
        '10002',
        '{"username": "test", "success": false, "message": "password error"}'::jsonb
    );
```

写入建议如下：

| 建议                     | 说明                                                   |
| ------------------------ | ------------------------------------------------------ |
| 显式转换类型             | 使用 `'{}'::jsonb`，避免类型推断不清晰                 |
| 默认值使用 JSONB 字面量  | 对象默认值用 `'{}'::jsonb`，数组默认值用 `'[]'::jsonb` |
| 应用层保证结构           | 数据库只校验 JSON 格式，不自动校验业务结构             |
| 避免写入超大 JSON        | 大字段会影响查询、备份和网络传输                       |
| 需要结构约束时加检查约束 | 可用 `jsonb_typeof` 检查对象或数组类型                 |

使用检查约束限制 JSONB 必须是对象：

```sql
ALTER TABLE app.event_log
ADD CONSTRAINT ck_event_log_event_data_object
CHECK (jsonb_typeof(event_data) = 'object');
```

### JSON 字段查询

JSON 字段查询常用操作符包括 `->`、`->>`、`#>`、`#>>`、`@>` 等。实际项目中最常用的是 `->>` 提取文本值，以及 `@>` 判断 JSONB 是否包含指定结构。

常用操作符如下：

| 操作符 | 说明                              | 示例                                        |
| ------ | --------------------------------- | ------------------------------------------- |
| `->`   | 获取 JSON 对象字段，返回 JSON     | `event_data -> 'username'`                  |
| `->>`  | 获取 JSON 对象字段，返回文本      | `event_data ->> 'username'`                 |
| `#>`   | 按路径获取 JSON，返回 JSON        | `event_data #> '{delivery,address}'`        |
| `#>>`  | 按路径获取 JSON，返回文本         | `event_data #>> '{delivery,address}'`       |
| `@>`   | 判断左侧 JSONB 是否包含右侧 JSONB | `event_data @> '{"success": true}'`         |
| `?`    | 判断是否存在顶层 key              | `event_data ? 'username'`                   |
| `?     | `                                 | 判断是否存在任意 key                        |
| `?&`   | 判断是否存在全部 key              | `event_data ?& array['username','success']` |

查询 JSON 字段文本：

```sql
SELECT
    id,
    event_type,
    event_data ->> 'username' AS username,
    event_data ->> 'clientIp' AS client_ip,
    event_data ->> 'success' AS success
FROM app.event_log
WHERE event_type = 'USER_LOGIN';
```

查询嵌套字段：

```sql
SELECT
    id,
    order_no,
    extra_data #>> '{delivery,receiver}' AS receiver,
    extra_data #>> '{delivery,phone}' AS receiver_phone,
    extra_data #>> '{delivery,address}' AS receiver_address
FROM app.order_info;
```

按 JSON 字段过滤：

```sql
SELECT
    id,
    event_type,
    business_id,
    event_data
FROM app.event_log
WHERE event_data ->> 'username' = 'admin';
```

按 JSON 数值字段过滤时，需要显式类型转换：

```sql
SELECT
    id,
    order_no,
    total_amount,
    extra_data
FROM app.order_info
WHERE (extra_data ->> 'couponId')::BIGINT = 30001;
```

使用包含查询：

```sql
SELECT
    id,
    event_type,
    business_id,
    event_data
FROM app.event_log
WHERE event_data @> '{"success": true}'::jsonb;
```

判断顶层 key 是否存在：

```sql
SELECT
    id,
    event_type,
    event_data
FROM app.event_log
WHERE event_data ? 'username';
```

查询建议如下：

| 场景          | 推荐写法                                     |
| ------------- | -------------------------------------------- |
| 取文本值      | `jsonb_column ->> 'key'`                     |
| 取 JSON 对象  | `jsonb_column -> 'key'`                      |
| 取嵌套文本    | `jsonb_column #>> '{a,b,c}'`                 |
| 判断包含结构  | `jsonb_column @> '{"key": "value"}'::jsonb`  |
| 判断 key 存在 | `jsonb_column ? 'key'`                       |
| 数值比较      | `(jsonb_column ->> 'amount')::numeric > 100` |

如果 JSON 字段要频繁用于过滤，应评估是否应该提升为普通列，或者创建表达式索引。

### JSON 字段更新

JSONB 字段更新可以整体替换，也可以使用 `jsonb_set` 更新指定路径。更新 JSONB 时需要注意，JSONB 是一个整体字段，局部更新在逻辑上是修改局部路径，但底层仍可能产生行版本更新。

整体替换 JSONB 字段：

```sql
UPDATE app.order_info
SET
    extra_data = '{
        "channel": "WEB",
        "remark": "后台修改",
        "delivery": {
            "receiver": "李四",
            "phone": "13800000001",
            "address": "上海市浦东新区"
        }
    }'::jsonb,
    update_time = CURRENT_TIMESTAMP
WHERE order_no = 'PO202605090001';
```

更新顶层字段：

```sql
UPDATE app.order_info
SET
    extra_data = jsonb_set(
        extra_data,
        '{channel}',
        '"MINI_PROGRAM"'::jsonb,
        true
    ),
    update_time = CURRENT_TIMESTAMP
WHERE order_no = 'PO202605090001';
```

更新嵌套字段：

```sql
UPDATE app.order_info
SET
    extra_data = jsonb_set(
        extra_data,
        '{delivery,address}',
        '"北京市朝阳区"'::jsonb,
        true
    ),
    update_time = CURRENT_TIMESTAMP
WHERE order_no = 'PO202605090001';
```

增加或覆盖多个字段：

```sql
UPDATE app.order_info
SET
    extra_data = extra_data || '{
        "source": "promotion",
        "operator": "admin"
    }'::jsonb,
    update_time = CURRENT_TIMESTAMP
WHERE order_no = 'PO202605090001';
```

删除顶层字段：

```sql
UPDATE app.order_info
SET
    extra_data = extra_data - 'operator',
    update_time = CURRENT_TIMESTAMP
WHERE order_no = 'PO202605090001';
```

删除嵌套字段：

```sql
UPDATE app.order_info
SET
    extra_data = extra_data #- '{delivery,address}',
    update_time = CURRENT_TIMESTAMP
WHERE order_no = 'PO202605090001';
```

向 JSONB 数组追加元素：

```sql
UPDATE app.event_log
SET event_data = jsonb_set(
    event_data,
    '{tags}',
    COALESCE(event_data -> 'tags', '[]'::jsonb) || '"postgresql"'::jsonb,
    true
)
WHERE id = 1;
```

更新建议如下：

| 建议                       | 说明                              |
| -------------------------- | --------------------------------- |
| 小字段频繁更新不要放 JSONB | 高频更新字段建议普通列化          |
| 使用 `jsonb_set` 更新路径  | 避免应用层读取整个 JSON 后再覆盖  |
| 更新值注意 JSON 格式       | 字符串值要写成 `'"value"'::jsonb` |
| 修改后同步更新时间         | 业务表应维护 `update_time`        |
| 大 JSON 更新要谨慎         | 会增加 WAL、表膨胀和 VACUUM 压力  |

### JSON 路径查询

JSON 路径查询用于按路径表达式查询 JSONB 内容，适合结构较复杂、嵌套层级较深或需要条件匹配的 JSON 数据。

准备示例数据：

```sql
INSERT INTO app.event_log (
    event_type,
    business_id,
    event_data
) VALUES (
    'ORDER_PAY',
    'PO202605090001',
    '{
        "orderNo": "PO202605090001",
        "pay": {
            "amount": 199.90,
            "method": "ALIPAY",
            "success": true
        },
        "items": [
            {"name": "键盘", "price": 99.90, "quantity": 1},
            {"name": "鼠标", "price": 100.00, "quantity": 1}
        ]
    }'::jsonb
);
```

使用 `jsonb_path_exists` 判断路径是否存在或条件是否满足：

```sql
SELECT
    id,
    event_type,
    business_id
FROM app.event_log
WHERE jsonb_path_exists(
    event_data,
    '$.pay ? (@.success == true)'
);
```

查询订单金额大于 100 的数据：

```sql
SELECT
    id,
    event_type,
    business_id,
    event_data #>> '{pay,amount}' AS pay_amount
FROM app.event_log
WHERE jsonb_path_exists(
    event_data,
    '$.pay ? (@.amount > 100)'
);
```

查询数组中存在指定条件的元素：

```sql
SELECT
    id,
    event_type,
    business_id
FROM app.event_log
WHERE jsonb_path_exists(
    event_data,
    '$.items[*] ? (@.price >= 100)'
);
```

使用 `jsonb_path_query` 返回匹配内容：

```sql
SELECT
    id,
    jsonb_path_query(event_data, '$.items[*] ? (@.price >= 100)') AS matched_item
FROM app.event_log
WHERE event_type = 'ORDER_PAY';
```

使用 `jsonb_path_query_array` 返回匹配数组：

```sql
SELECT
    id,
    jsonb_path_query_array(event_data, '$.items[*] ? (@.price >= 100)') AS matched_items
FROM app.event_log
WHERE event_type = 'ORDER_PAY';
```

JSON 路径查询建议如下：

| 场景             | 建议                                                |
| ---------------- | --------------------------------------------------- |
| 简单字段提取     | 优先使用 `->>` 或 `#>>`                             |
| 判断包含固定结构 | 优先使用 `@>`                                       |
| 嵌套数组条件查询 | 可使用 JSON Path                                    |
| 复杂 JSON 条件   | 使用 `jsonb_path_exists`                            |
| 需要返回匹配片段 | 使用 `jsonb_path_query` 或 `jsonb_path_query_array` |

JSON Path 表达能力强，但可读性和索引利用需要重点关注。普通业务查询不要为了追求高级语法而放弃更简单的字段设计。

### JSON 数组处理

JSON 数组常见于标签、商品明细、动态表单项、事件明细等场景。PostgreSQL 可以展开 JSON 数组，再像普通行一样查询和统计。

准备示例数据：

```sql
CREATE TABLE app.order_snapshot (
    id BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    order_no VARCHAR(64) NOT NULL,
    snapshot_data JSONB NOT NULL DEFAULT '{}'::jsonb,
    create_time TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

INSERT INTO app.order_snapshot (
    order_no,
    snapshot_data
) VALUES (
    'PO202605090001',
    '{
        "items": [
            {"skuId": 1001, "name": "键盘", "price": 99.90, "quantity": 1},
            {"skuId": 1002, "name": "鼠标", "price": 100.00, "quantity": 2}
        ]
    }'::jsonb
);
```

展开 JSON 数组：

```sql
SELECT
    s.order_no,
    item.value ->> 'skuId' AS sku_id,
    item.value ->> 'name' AS product_name,
    (item.value ->> 'price')::numeric AS price,
    (item.value ->> 'quantity')::integer AS quantity
FROM app.order_snapshot s
CROSS JOIN LATERAL jsonb_array_elements(s.snapshot_data -> 'items') AS item(value);
```

统计 JSON 数组中的商品总金额：

```sql
SELECT
    s.order_no,
    SUM(
        (item.value ->> 'price')::numeric
        * (item.value ->> 'quantity')::integer
    ) AS snapshot_total_amount
FROM app.order_snapshot s
CROSS JOIN LATERAL jsonb_array_elements(s.snapshot_data -> 'items') AS item(value)
GROUP BY s.order_no;
```

查询数组中包含指定商品的数据：

```sql
SELECT
    s.id,
    s.order_no,
    s.snapshot_data
FROM app.order_snapshot s
WHERE EXISTS (
    SELECT 1
    FROM jsonb_array_elements(s.snapshot_data -> 'items') AS item(value)
    WHERE (item.value ->> 'skuId')::BIGINT = 1001
);
```

获取 JSON 数组长度：

```sql
SELECT
    id,
    order_no,
    jsonb_array_length(snapshot_data -> 'items') AS item_count
FROM app.order_snapshot;
```

按数组下标获取元素：

```sql
SELECT
    id,
    order_no,
    snapshot_data -> 'items' -> 0 AS first_item,
    snapshot_data -> 'items' -> 0 ->> 'name' AS first_item_name
FROM app.order_snapshot;
```

JSON 数组使用建议如下：

| 建议               | 说明                                               |
| ------------------ | -------------------------------------------------- |
| 适合存快照         | 订单快照、接口报文、事件明细适合 JSON 数组         |
| 不适合强关系明细   | 订单明细、库存明细等核心业务仍建议单独建表         |
| 展开时使用 LATERAL | `jsonb_array_elements` 常配合 `CROSS JOIN LATERAL` |
| 类型转换要显式     | JSON 中数字提取为文本后再转数值                    |
| 大数组谨慎查询     | 数组过大会增加展开和统计成本                       |

### JSON 聚合

JSON 聚合用于把关系型数据组装成 JSON 对象或 JSON 数组，常用于接口返回、报表数据、嵌套结构查询等场景。

按用户聚合角色 JSON：

```sql
SELECT
    u.id,
    u.username,
    u.nickname,
    COALESCE(
        jsonb_agg(
            jsonb_build_object(
                'roleId', r.id,
                'roleCode', r.role_code,
                'roleName', r.role_name
            )
            ORDER BY r.role_code
        ) FILTER (WHERE r.id IS NOT NULL),
        '[]'::jsonb
    ) AS roles
FROM app.sys_user u
LEFT JOIN app.sys_user_role ur ON ur.user_id = u.id
LEFT JOIN app.sys_role r ON r.id = ur.role_id
WHERE u.deleted = FALSE
GROUP BY
    u.id,
    u.username,
    u.nickname;
```

按订单聚合订单明细 JSON：

```sql
SELECT
    o.id,
    o.order_no,
    o.order_status,
    o.total_amount,
    COALESCE(
        jsonb_agg(
            jsonb_build_object(
                'itemId', oi.id,
                'productName', oi.product_name,
                'quantity', oi.quantity,
                'salePrice', oi.sale_price,
                'itemAmount', oi.quantity * oi.sale_price
            )
            ORDER BY oi.id
        ) FILTER (WHERE oi.id IS NOT NULL),
        '[]'::jsonb
    ) AS items
FROM app.order_info o
LEFT JOIN app.order_item oi ON oi.order_id = o.id
GROUP BY
    o.id,
    o.order_no,
    o.order_status,
    o.total_amount;
```

构造单个 JSON 对象：

```sql
SELECT
    jsonb_build_object(
        'orderNo', o.order_no,
        'status', o.order_status,
        'amount', o.total_amount,
        'createTime', o.create_time
    ) AS order_json
FROM app.order_info o
WHERE o.order_no = 'PO202605090001';
```

构造接口风格的嵌套结果：

```sql
SELECT
    jsonb_build_object(
        'orderId', o.id,
        'orderNo', o.order_no,
        'status', o.order_status,
        'amount', o.total_amount,
        'items',
        COALESCE(
            jsonb_agg(
                jsonb_build_object(
                    'productName', oi.product_name,
                    'quantity', oi.quantity,
                    'salePrice', oi.sale_price
                )
                ORDER BY oi.id
            ) FILTER (WHERE oi.id IS NOT NULL),
            '[]'::jsonb
        )
    ) AS order_detail
FROM app.order_info o
LEFT JOIN app.order_item oi ON oi.order_id = o.id
WHERE o.order_no = 'PO202605090001'
GROUP BY
    o.id,
    o.order_no,
    o.order_status,
    o.total_amount;
```

JSON 聚合建议如下：

| 场景         | 建议                         |
| ------------ | ---------------------------- |
| 简单嵌套结果 | 可以在 SQL 中使用 JSON 聚合  |
| 复杂业务组装 | 建议在应用层组装             |
| 大结果集     | 避免一次聚合过大 JSON        |
| 接口返回     | 注意字段命名和空数组处理     |
| 报表导出     | 可以用 JSON 聚合减少多次查询 |

SQL 中构造 JSON 能减少应用层拼装成本，但不应把复杂业务逻辑全部塞进 SQL。数据量大、逻辑复杂时，应用层分步查询和组装通常更可控。

### JSONB 索引

JSONB 查询如果没有索引，通常需要扫描大量数据。PostgreSQL 支持对 JSONB 字段创建 GIN 索引，也支持对 JSONB 提取表达式创建 B-Tree 索引。

创建 JSONB GIN 索引：

```sql
CREATE INDEX idx_event_log_event_data_gin
ON app.event_log
USING GIN (event_data);
```

该索引适合 `@>`、`?`、`?|`、`?&` 等操作符查询。

示例查询：

```sql
SELECT
    id,
    event_type,
    business_id
FROM app.event_log
WHERE event_data @> '{"success": true}'::jsonb;
```

使用 `jsonb_path_ops` 创建更紧凑的 GIN 索引：

```sql
CREATE INDEX idx_event_log_event_data_path_gin
ON app.event_log
USING GIN (event_data jsonb_path_ops);
```

`jsonb_path_ops` 对包含查询较友好，索引通常更小，但支持的操作符范围比默认 GIN 操作符类少。需要根据查询类型选择。

对 JSONB 中的字段创建表达式索引：

```sql
CREATE INDEX idx_event_log_username
ON app.event_log ((event_data ->> 'username'));
```

适合以下查询：

```sql
SELECT
    id,
    event_type,
    event_data
FROM app.event_log
WHERE event_data ->> 'username' = 'admin';
```

对 JSONB 中的数值字段创建表达式索引：

```sql
CREATE INDEX idx_order_info_coupon_id
ON app.order_info (((extra_data ->> 'couponId')::BIGINT));
```

适合以下查询：

```sql
SELECT
    id,
    order_no,
    extra_data
FROM app.order_info
WHERE (extra_data ->> 'couponId')::BIGINT = 30001;
```

创建部分索引：

```sql
CREATE INDEX idx_event_log_login_success
ON app.event_log ((event_data ->> 'username'))
WHERE event_type = 'USER_LOGIN'
  AND event_data @> '{"success": true}'::jsonb;
```

JSONB 索引选择建议如下：

| 查询场景                                   | 推荐索引                             |
| ------------------------------------------ | ------------------------------------ |
| `event_data @> '{"a": 1}'`                 | GIN 索引                             |
| `event_data ? 'key'`                       | GIN 索引                             |
| `event_data ->> 'username' = 'admin'`      | 表达式 B-Tree 索引                   |
| `(event_data ->> 'amount')::numeric > 100` | 表达式索引，按实际查询评估           |
| 只查询某类事件                             | 部分索引                             |
| 多个普通字段和 JSON 字段组合过滤           | 普通列索引 + JSON 表达式索引综合评估 |

索引不是越多越好。JSONB 索引会增加写入成本和存储空间，应根据真实 SQL 和执行计划创建。

### JSONB 查询优化

JSONB 查询优化的核心是控制 JSONB 使用范围、减少大字段扫描、为高频过滤条件建立合适索引，并避免把关系型查询强行写成 JSON 查询。

优先把高频查询字段普通列化：

```sql
-- 不推荐：高频查询字段都放在 JSONB 中
CREATE TABLE app.bad_order_design (
    id BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    order_data JSONB NOT NULL
);

-- 推荐：核心字段普通列化，扩展字段放 JSONB
CREATE TABLE app.good_order_design (
    id BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    order_no VARCHAR(64) NOT NULL,
    user_id BIGINT NOT NULL,
    order_status VARCHAR(32) NOT NULL,
    total_amount NUMERIC(12, 2) NOT NULL,
    extra_data JSONB NOT NULL DEFAULT '{}'::jsonb
);
```

避免在 JSONB 字段上做无索引大范围过滤：

```sql
-- 如果 channel 高频查询，应考虑表达式索引或普通列化
SELECT
    id,
    order_no
FROM app.order_info
WHERE extra_data ->> 'channel' = 'APP';
```

为高频查询创建表达式索引：

```sql
CREATE INDEX idx_order_info_channel
ON app.order_info ((extra_data ->> 'channel'));
```

查看执行计划：

```sql
EXPLAIN ANALYZE
SELECT
    id,
    order_no
FROM app.order_info
WHERE extra_data ->> 'channel' = 'APP';
```

避免对 JSONB 提取结果反复转换：

```sql
-- 查询时需要转换，适合低频查询
SELECT
    id,
    order_no
FROM app.order_info
WHERE (extra_data ->> 'couponId')::BIGINT = 30001;
```

如果 `couponId` 是核心业务过滤字段，建议改为普通列：

```sql
ALTER TABLE app.order_info
ADD COLUMN coupon_id BIGINT;
```

JSONB 查询优化建议如下：

| 优化方向              | 说明                                           |
| --------------------- | ---------------------------------------------- |
| 核心字段普通列化      | 高频查询、排序、关联、统计字段不要长期放 JSONB |
| 创建合适索引          | `@>` 用 GIN，`->>` 等值过滤用表达式索引        |
| 控制 JSON 大小        | 大 JSON 会影响 IO、缓存、备份和更新            |
| 避免深层嵌套          | 深层路径查询可读性差，维护成本高               |
| 减少频繁更新          | 频繁更新 JSONB 大字段容易产生表膨胀            |
| 使用执行计划验证      | 索引是否命中必须通过 `EXPLAIN ANALYZE` 确认    |
| 使用部分索引          | 针对特定事件类型或状态创建更小索引             |
| 不用 JSONB 替代关联表 | 强关系、多对多、明细数据应优先建表             |

JSONB 的定位应是关系模型的补充，而不是替代。项目建模时先确定核心关系和查询模式，再决定哪些扩展字段适合放入 JSONB。

## 数组使用

PostgreSQL 支持数组类型，可以在一个字段中存储多个同类型值。数组适合轻量、多值、弱关系的场景，例如标签编码、权限标识、简单分类、临时参数集合等。对于需要维护独立属性、排序、统计、权限控制的多值关系，应优先使用关联表。

### 数组字段定义

数组字段通过 `类型[]` 定义，例如 `VARCHAR[]`、`BIGINT[]`、`INTEGER[]`。数组字段建议设置默认值，避免出现 `NULL` 和空数组混用的问题。

定义数组字段：

```sql
CREATE TABLE app.article (
    id BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    title VARCHAR(255) NOT NULL,
    tag_codes VARCHAR(64)[] NOT NULL DEFAULT '{}',
    category_ids BIGINT[] NOT NULL DEFAULT '{}',
    create_time TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_time TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

COMMENT ON TABLE app.article IS '文章表';
COMMENT ON COLUMN app.article.id IS '主键ID';
COMMENT ON COLUMN app.article.title IS '文章标题';
COMMENT ON COLUMN app.article.tag_codes IS '标签编码数组';
COMMENT ON COLUMN app.article.category_ids IS '分类ID数组';
COMMENT ON COLUMN app.article.create_time IS '创建时间';
COMMENT ON COLUMN app.article.update_time IS '更新时间';
```

定义整型数组：

```sql
CREATE TABLE app.score_record (
    id BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    user_id BIGINT NOT NULL,
    scores INTEGER[] NOT NULL DEFAULT '{}',
    create_time TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);
```

定义文本数组：

```sql
CREATE TABLE app.user_permission_snapshot (
    id BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    user_id BIGINT NOT NULL,
    permission_codes TEXT[] NOT NULL DEFAULT '{}',
    create_time TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);
```

数组字段设计建议如下：

| 建议               | 说明                                         |
| ------------------ | -------------------------------------------- |
| 设置非空默认值     | 推荐 `NOT NULL DEFAULT '{}'`                 |
| 数组元素类型要明确 | 不要用 `TEXT[]` 承载所有类型                 |
| 弱关系可以使用数组 | 标签编码、权限快照、临时分类可以考虑         |
| 强关系不要用数组   | 用户角色、订单明细、商品分类关系建议建关联表 |
| 高频查询要考虑索引 | 包含判断可使用 GIN 索引                      |
| 数组长度不要过大   | 大数组会影响查询、更新和网络传输             |

### 数组数据插入

数组数据可以使用 `ARRAY[...]` 语法插入，也可以使用数组字面量 `'{}'`。推荐在 SQL 中使用 `ARRAY[...]`，可读性更好。

插入文章标签和分类：

```sql
INSERT INTO app.article (
    title,
    tag_codes,
    category_ids
) VALUES (
    'PostgreSQL 数组使用',
    ARRAY['database', 'postgresql', 'sql'],
    ARRAY[1, 2, 3]
);
```

插入空数组：

```sql
INSERT INTO app.article (
    title,
    tag_codes,
    category_ids
) VALUES (
    '无标签文章',
    ARRAY[]::VARCHAR[],
    ARRAY[]::BIGINT[]
);
```

使用默认空数组：

```sql
INSERT INTO app.article (
    title
) VALUES (
    '默认空数组文章'
);
```

批量插入数组数据：

```sql
INSERT INTO app.article (
    title,
    tag_codes,
    category_ids
) VALUES
    (
        'PostgreSQL JSONB 使用',
        ARRAY['database', 'jsonb'],
        ARRAY[1, 2]
    ),
    (
        'PostgreSQL 索引设计',
        ARRAY['database', 'index'],
        ARRAY[1, 3]
    ),
    (
        'Spring Boot 集成 PostgreSQL',
        ARRAY['java', 'spring-boot', 'postgresql'],
        ARRAY[2, 4]
    );
```

向已有数组追加元素：

```sql
UPDATE app.article
SET
    tag_codes = array_append(tag_codes, 'tutorial'),
    update_time = CURRENT_TIMESTAMP
WHERE id = 1;
```

向数组前面插入元素：

```sql
UPDATE app.article
SET
    tag_codes = array_prepend('featured', tag_codes),
    update_time = CURRENT_TIMESTAMP
WHERE id = 1;
```

拼接两个数组：

```sql
UPDATE app.article
SET
    tag_codes = tag_codes || ARRAY['backend', 'sql'],
    update_time = CURRENT_TIMESTAMP
WHERE id = 1;
```

删除数组元素：

```sql
UPDATE app.article
SET
    tag_codes = array_remove(tag_codes, 'tutorial'),
    update_time = CURRENT_TIMESTAMP
WHERE id = 1;
```

写入建议如下：

| 场景         | 推荐写法                            |
| ------------ | ----------------------------------- |
| 插入固定数组 | `ARRAY['a', 'b']`                   |
| 插入空数组   | `ARRAY[]::VARCHAR[]`                |
| 追加单个元素 | `array_append(array_column, value)` |
| 删除指定元素 | `array_remove(array_column, value)` |
| 拼接数组     | `array_column                       |
| 避免空值混乱 | 字段设置 `NOT NULL DEFAULT '{}'`    |

### 数组元素查询

数组元素查询可以通过下标、`ANY`、数组函数等方式完成。PostgreSQL 数组下标从 `1` 开始，不是从 `0` 开始。

查询数组第一个元素：

```sql
SELECT
    id,
    title,
    tag_codes[1] AS first_tag_code
FROM app.article;
```

查询数组长度：

```sql
SELECT
    id,
    title,
    cardinality(tag_codes) AS tag_count,
    cardinality(category_ids) AS category_count
FROM app.article;
```

也可以使用 `array_length` 查询指定维度长度：

```sql
SELECT
    id,
    title,
    array_length(tag_codes, 1) AS tag_count
FROM app.article;
```

查询包含某个标签的文章：

```sql
SELECT
    id,
    title,
    tag_codes
FROM app.article
WHERE 'postgresql' = ANY(tag_codes);
```

查询分类 ID 包含指定值的文章：

```sql
SELECT
    id,
    title,
    category_ids
FROM app.article
WHERE 2 = ANY(category_ids);
```

查询数组为空的记录：

```sql
SELECT
    id,
    title
FROM app.article
WHERE cardinality(tag_codes) = 0;
```

查询数组不为空的记录：

```sql
SELECT
    id,
    title,
    tag_codes
FROM app.article
WHERE cardinality(tag_codes) > 0;
```

数组查询注意事项如下：

| 注意点                     | 说明                               |
| -------------------------- | ---------------------------------- |
| 数组下标从 1 开始          | `tag_codes[1]` 表示第一个元素      |
| 空数组不是 `NULL`          | 推荐默认值使用空数组，减少空值判断 |
| `ANY` 适合单元素匹配       | 判断某个值是否存在于数组中         |
| 长度判断推荐 `cardinality` | 可读性较好                         |
| 查询频繁时考虑 GIN 索引    | 包含判断适合索引优化               |

### 数组包含判断

数组包含判断常用于标签过滤、权限匹配、分类筛选等场景。常见操作符包括 `@>`、`<@` 和 `&&`。

数组操作符说明：

| 操作符 | 说明                   | 示例                                |
| ------ | ---------------------- | ----------------------------------- |
| `@>`   | 左侧数组包含右侧数组   | `tag_codes @> ARRAY['postgresql']`  |
| `<@`   | 左侧数组被右侧数组包含 | `ARRAY['postgresql'] <@ tag_codes`  |
| `&&`   | 两个数组是否有交集     | `tag_codes && ARRAY['java', 'sql']` |

查询同时包含 `database` 和 `postgresql` 标签的文章：

```sql
SELECT
    id,
    title,
    tag_codes
FROM app.article
WHERE tag_codes @> ARRAY['database', 'postgresql'];
```

查询包含任意一个指定标签的文章：

```sql
SELECT
    id,
    title,
    tag_codes
FROM app.article
WHERE tag_codes && ARRAY['java', 'spring-boot'];
```

查询文章标签是否全部属于指定标签集合：

```sql
SELECT
    id,
    title,
    tag_codes
FROM app.article
WHERE tag_codes <@ ARRAY['database', 'postgresql', 'sql', 'index', 'jsonb'];
```

查询分类包含指定分类 ID 的文章：

```sql
SELECT
    id,
    title,
    category_ids
FROM app.article
WHERE category_ids @> ARRAY[2]::BIGINT[];
```

使用数组条件做权限匹配示例：

```sql
SELECT
    id,
    user_id,
    permission_codes
FROM app.user_permission_snapshot
WHERE permission_codes @> ARRAY['system:user:list'];
```

包含判断建议如下：

| 需求             | 推荐写法                                                     |
| ---------------- | ------------------------------------------------------------ |
| 包含单个元素     | `'postgresql' = ANY(tag_codes)` 或 `tag_codes @> ARRAY['postgresql']` |
| 同时包含多个元素 | `tag_codes @> ARRAY['a', 'b']`                               |
| 包含任意元素     | `tag_codes && ARRAY['a', 'b']`                               |
| 判断数组范围     | `tag_codes <@ ARRAY[...]`                                    |
| 需要索引优化     | 优先使用 GIN 索引支持的操作符                                |

如果数组字段承载的是强业务关系，例如用户角色、文章标签关系，并且需要维护标签名称、排序、状态、权限等属性，建议改为关联表。

### 数组展开

数组展开用于把数组中的每个元素转换为独立行。常用函数是 `unnest`，复杂场景可以配合 `WITH ORDINALITY` 保留元素顺序。

展开文章标签：

```sql
SELECT
    id,
    title,
    unnest(tag_codes) AS tag_code
FROM app.article;
```

使用 `CROSS JOIN LATERAL` 展开数组：

```sql
SELECT
    a.id,
    a.title,
    tag.tag_code
FROM app.article a
CROSS JOIN LATERAL unnest(a.tag_codes) AS tag(tag_code);
```

展开数组并保留顺序：

```sql
SELECT
    a.id,
    a.title,
    tag.tag_code,
    tag.ordinality AS tag_index
FROM app.article a
CROSS JOIN LATERAL unnest(a.tag_codes) WITH ORDINALITY AS tag(tag_code, ordinality)
ORDER BY a.id, tag.ordinality;
```

展开多个数组时需要谨慎。如果多个数组长度不同，结果可能不符合预期。建议先明确业务语义。

展开分类 ID：

```sql
SELECT
    a.id,
    a.title,
    category.category_id
FROM app.article a
CROSS JOIN LATERAL unnest(a.category_ids) AS category(category_id);
```

统计每个标签出现次数：

```sql
SELECT
    tag.tag_code,
    COUNT(*) AS article_count
FROM app.article a
CROSS JOIN LATERAL unnest(a.tag_codes) AS tag(tag_code)
GROUP BY tag.tag_code
ORDER BY article_count DESC, tag.tag_code;
```

数组展开建议如下：

| 场景             | 建议                      |
| ---------------- | ------------------------- |
| 一行转多行       | 使用 `unnest`             |
| 需要引用主表字段 | 使用 `CROSS JOIN LATERAL` |
| 需要保留数组顺序 | 使用 `WITH ORDINALITY`    |
| 数组很大         | 注意展开后结果集膨胀      |
| 需要频繁统计     | 考虑改为明细表或关联表    |

### 数组聚合

数组聚合用于把多行数据聚合成数组，常用函数是 `array_agg`。它适合在查询结果中返回集合字段，例如用户角色编码数组、订单商品名称数组、文章标签数组等。

按用户聚合角色编码：

```sql
SELECT
    u.id,
    u.username,
    COALESCE(
        array_agg(r.role_code ORDER BY r.role_code)
            FILTER (WHERE r.role_code IS NOT NULL),
        ARRAY[]::VARCHAR[]
    ) AS role_codes
FROM app.sys_user u
LEFT JOIN app.sys_user_role ur ON ur.user_id = u.id
LEFT JOIN app.sys_role r ON r.id = ur.role_id
WHERE u.deleted = FALSE
GROUP BY
    u.id,
    u.username;
```

按订单聚合商品名称：

```sql
SELECT
    o.id,
    o.order_no,
    COALESCE(
        array_agg(oi.product_name ORDER BY oi.id)
            FILTER (WHERE oi.product_name IS NOT NULL),
        ARRAY[]::VARCHAR[]
    ) AS product_names
FROM app.order_info o
LEFT JOIN app.order_item oi ON oi.order_id = o.id
GROUP BY
    o.id,
    o.order_no;
```

去重聚合：

```sql
SELECT
    u.id,
    u.username,
    COALESCE(
        array_agg(DISTINCT r.role_code ORDER BY r.role_code)
            FILTER (WHERE r.role_code IS NOT NULL),
        ARRAY[]::VARCHAR[]
    ) AS role_codes
FROM app.sys_user u
LEFT JOIN app.sys_user_role ur ON ur.user_id = u.id
LEFT JOIN app.sys_role r ON r.id = ur.role_id
WHERE u.deleted = FALSE
GROUP BY
    u.id,
    u.username;
```

将数组聚合结果转换为字符串：

```sql
SELECT
    u.id,
    u.username,
    array_to_string(
        COALESCE(
            array_agg(r.role_name ORDER BY r.role_name)
                FILTER (WHERE r.role_name IS NOT NULL),
            ARRAY[]::VARCHAR[]
        ),
        ','
    ) AS role_names
FROM app.sys_user u
LEFT JOIN app.sys_user_role ur ON ur.user_id = u.id
LEFT JOIN app.sys_role r ON r.id = ur.role_id
WHERE u.deleted = FALSE
GROUP BY
    u.id,
    u.username;
```

数组聚合建议如下：

| 建议                  | 说明                                     |
| --------------------- | ---------------------------------------- |
| 聚合时指定排序        | 使用 `array_agg(column ORDER BY column)` |
| 过滤空值              | 使用 `FILTER (WHERE column IS NOT NULL)` |
| 空结果转空数组        | 使用 `COALESCE(..., ARRAY[]::类型[])`    |
| 去重时使用 `DISTINCT` | 避免重复关联导致数组重复                 |
| 大数组谨慎返回        | 结果过大时会增加内存和网络成本           |

### 数组索引

数组字段如果经常用于包含、交集、被包含等查询，应考虑创建 GIN 索引。GIN 索引适合数组的 `@>`、`<@`、`&&` 等操作。

为标签数组创建 GIN 索引：

```sql
CREATE INDEX idx_article_tag_codes_gin
ON app.article
USING GIN (tag_codes);
```

为分类数组创建 GIN 索引：

```sql
CREATE INDEX idx_article_category_ids_gin
ON app.article
USING GIN (category_ids);
```

索引适合以下查询：

```sql
-- 查询包含指定标签的文章
SELECT
    id,
    title,
    tag_codes
FROM app.article
WHERE tag_codes @> ARRAY['postgresql'];
-- 查询包含任意指定标签的文章
SELECT
    id,
    title,
    tag_codes
FROM app.article
WHERE tag_codes && ARRAY['java', 'spring-boot'];
-- 查询包含指定分类 ID 的文章
SELECT
    id,
    title,
    category_ids
FROM app.article
WHERE category_ids @> ARRAY[2]::BIGINT[];
```

使用执行计划验证索引效果：

```sql
EXPLAIN ANALYZE
SELECT
    id,
    title,
    tag_codes
FROM app.article
WHERE tag_codes @> ARRAY['postgresql'];
```

数组索引建议如下：

| 场景               | 是否建议索引             |
| ------------------ | ------------------------ |
| 偶尔查询数组字段   | 不一定需要索引           |
| 高频包含判断       | 建议 GIN 索引            |
| 高频交集判断       | 建议 GIN 索引            |
| 数组字段频繁更新   | 谨慎建索引，评估写入成本 |
| 数组很长且数据量大 | 需要结合执行计划评估     |
| 需要按元素统计     | 优先考虑拆分成关联表     |

数组类型能简化部分多值字段设计，但不应滥用。对于“值本身还需要属性、状态、排序、权限、创建时间”的场景，关联表通常比数组字段更清晰、更规范，也更利于长期维护。


## 日期时间处理

日期时间处理用于存储、查询、计算和统计业务时间。常见场景包括创建时间、更新时间、登录时间、订单时间、过期时间、账单日期、统计周期和时间区间筛选。项目中应统一时间类型、时区策略和查询边界，避免出现跨天错误、时区偏差和索引失效。

### 当前时间获取

PostgreSQL 提供多个当前时间函数，不同函数的语义略有差异。业务开发中最常用的是 `CURRENT_TIMESTAMP`、`NOW()`、`CURRENT_DATE` 和 `clock_timestamp()`。

常用当前时间函数如下：

| 函数                      | 返回内容              | 说明                            |
| ------------------------- | --------------------- | ------------------------------- |
| `CURRENT_DATE`            | 当前日期              | 只包含年月日                    |
| `CURRENT_TIME`            | 当前时间              | 只包含时分秒和时区              |
| `CURRENT_TIMESTAMP`       | 当前事务时间          | 等价于当前事务开始时的时间      |
| `NOW()`                   | 当前事务时间          | 与 `CURRENT_TIMESTAMP` 语义接近 |
| `clock_timestamp()`       | 当前真实时钟时间      | 每次调用都返回真实当前时间      |
| `statement_timestamp()`   | 当前 SQL 语句开始时间 | 同一条 SQL 中通常保持一致       |
| `transaction_timestamp()` | 当前事务开始时间      | 与 `CURRENT_TIMESTAMP` 类似     |

查询当前时间：

```sql
SELECT
    CURRENT_DATE AS current_date,
    CURRENT_TIME AS current_time,
    CURRENT_TIMESTAMP AS current_timestamp,
    NOW() AS now_time,
    clock_timestamp() AS clock_time;
```

在同一个事务中，`NOW()` 和 `CURRENT_TIMESTAMP` 通常保持事务开始时的时间不变，而 `clock_timestamp()` 会返回实际调用时的时间。

```sql
BEGIN;

SELECT
    NOW() AS now_time,
    clock_timestamp() AS clock_time;

-- 模拟中间经过一段时间后再次查询
SELECT
    NOW() AS now_time,
    clock_timestamp() AS clock_time;

COMMIT;
```

业务表中常用默认时间字段：

```sql
CREATE TABLE app.operation_log (
    id BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    operation_type VARCHAR(64) NOT NULL,
    operation_content TEXT,
    create_time TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);
```

如果用于审计字段，`CURRENT_TIMESTAMP` 通常足够。如果需要记录函数执行过程中每个步骤的真实时间差，可以使用 `clock_timestamp()`。

### 时间类型选择

时间类型选择会影响时区处理、查询结果、数据一致性和跨地区系统行为。项目中不要混用多种时间类型，应先统一约定。

常用时间类型如下：

| 类型          | 说明                 | 适用场景                             |
| ------------- | -------------------- | ------------------------------------ |
| `date`        | 日期，不包含时分秒   | 生日、账单日、业务日期、统计日期     |
| `time`        | 时间，不包含日期     | 每日固定执行时间、营业时间           |
| `timestamp`   | 日期时间，不包含时区 | 单地区系统、本地业务时间             |
| `timestamptz` | 带时区语义的时间戳   | 跨时区系统、国际化系统、统一绝对时间 |
| `interval`    | 时间间隔             | 有效期、持续时长、时间差             |

常见业务字段类型建议：

| 字段            | 推荐类型                     | 说明     |
| --------------- | ---------------------------- | -------- |
| `create_time`   | `timestamp` 或 `timestamptz` | 创建时间 |
| `update_time`   | `timestamp` 或 `timestamptz` | 更新时间 |
| `delete_time`   | `timestamp` 或 `timestamptz` | 删除时间 |
| `business_date` | `date`                       | 业务日期 |
| `birthday`      | `date`                       | 生日     |
| `expire_time`   | `timestamp` 或 `timestamptz` | 过期时间 |
| `duration`      | `interval`                   | 持续时长 |

普通单地区后台系统可以使用 `timestamp`：

```sql
CREATE TABLE app.task_info (
    id BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    task_name VARCHAR(128) NOT NULL,
    start_time TIMESTAMP NOT NULL,
    end_time TIMESTAMP,
    create_time TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);
```

跨时区系统建议使用 `timestamptz`：

```sql
CREATE TABLE app.login_record (
    id BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    user_id BIGINT NOT NULL,
    login_ip VARCHAR(64),
    login_time TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP
);
```

日期型业务字段使用 `date`：

```sql
CREATE TABLE app.daily_report (
    id BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    report_date DATE NOT NULL,
    total_count INTEGER NOT NULL DEFAULT 0,
    total_amount NUMERIC(12, 2) NOT NULL DEFAULT 0,
    create_time TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uk_daily_report_date UNIQUE (report_date)
);
```

类型选择建议如下：

| 建议                     | 说明                                               |
| ------------------------ | -------------------------------------------------- |
| 业务日期用 `date`        | 不要用字符串保存日期                               |
| 创建更新时间统一类型     | 不要同一项目中一部分用 `timestamp`，一部分用字符串 |
| 跨时区优先 `timestamptz` | 存储绝对时间点更可靠                               |
| 持续时长用 `interval`    | 不要用字符串描述“3天2小时”                         |
| 不用 `varchar` 存时间    | 会影响排序、比较和索引                             |

### 时间格式化

时间格式化用于把日期时间转换成指定格式的字符串。PostgreSQL 使用 `to_char` 格式化时间，常用于报表展示、导出和拼接统计周期。

格式化当前时间：

```sql
SELECT
    to_char(CURRENT_TIMESTAMP, 'YYYY-MM-DD HH24:MI:SS') AS current_time_text;
```

格式化日期：

```sql
SELECT
    to_char(CURRENT_DATE, 'YYYY-MM-DD') AS current_date_text;
```

格式化订单创建时间：

```sql
SELECT
    id,
    order_no,
    to_char(create_time, 'YYYY-MM-DD HH24:MI:SS') AS create_time_text
FROM app.order_info
ORDER BY create_time DESC;
```

常用格式模板如下：

| 模板   | 说明          | 示例       |
| ------ | ------------- | ---------- |
| `YYYY` | 四位年份      | `2026`     |
| `MM`   | 两位月份      | `05`       |
| `DD`   | 两位日期      | `09`       |
| `HH24` | 24 小时制小时 | `23`       |
| `MI`   | 分钟          | `30`       |
| `SS`   | 秒            | `59`       |
| `MS`   | 毫秒          | `123`      |
| `Day`  | 星期名称      | `Saturday` |

按年月格式化统计周期：

```sql
SELECT
    to_char(create_time, 'YYYY-MM') AS month_text,
    COUNT(*) AS order_count,
    SUM(total_amount) AS total_amount
FROM app.order_info
GROUP BY to_char(create_time, 'YYYY-MM')
ORDER BY month_text;
```

格式化建议如下：

| 场景           | 建议                              |
| -------------- | --------------------------------- |
| SQL 报表导出   | 可以使用 `to_char`                |
| 接口返回       | 通常由应用层统一格式化            |
| WHERE 条件过滤 | 不建议对时间列使用 `to_char` 过滤 |
| 分组统计       | 可以使用 `date_trunc` 后再格式化  |
| 排序           | 优先按原始时间字段排序            |

不建议这样写时间范围查询：

```sql
SELECT *
FROM app.order_info
WHERE to_char(create_time, 'YYYY-MM-DD') = '2026-05-09';
```

上面写法会对字段做函数处理，可能影响索引使用。推荐使用范围查询：

```sql
SELECT *
FROM app.order_info
WHERE create_time >= TIMESTAMP '2026-05-09 00:00:00'
  AND create_time < TIMESTAMP '2026-05-10 00:00:00';
```

### 时间计算

时间计算用于处理过期时间、有效期、时间差、账期、周期统计等场景。PostgreSQL 使用 `interval` 表示时间间隔。

当前时间加减：

```sql
SELECT
    CURRENT_TIMESTAMP AS current_time,
    CURRENT_TIMESTAMP + INTERVAL '1 day' AS after_one_day,
    CURRENT_TIMESTAMP - INTERVAL '7 days' AS before_seven_days,
    CURRENT_TIMESTAMP + INTERVAL '1 month' AS after_one_month;
```

计算过期时间：

```sql
SELECT
    id,
    task_name,
    create_time,
    create_time + INTERVAL '30 minutes' AS expire_time
FROM app.task_info;
```

查询最近 7 天数据：

```sql
SELECT
    id,
    order_no,
    total_amount,
    create_time
FROM app.order_info
WHERE create_time >= CURRENT_TIMESTAMP - INTERVAL '7 days'
ORDER BY create_time DESC;
```

计算两个时间之间的差值：

```sql
SELECT
    id,
    task_name,
    start_time,
    end_time,
    end_time - start_time AS cost_time
FROM app.task_info
WHERE end_time IS NOT NULL;
```

提取时间差中的部分字段：

```sql
SELECT
    id,
    task_name,
    EXTRACT(EPOCH FROM end_time - start_time) AS cost_seconds
FROM app.task_info
WHERE end_time IS NOT NULL;
```

常用 `interval` 写法：

| 写法                    | 说明    |
| ----------------------- | ------- |
| `INTERVAL '1 day'`      | 1 天    |
| `INTERVAL '7 days'`     | 7 天    |
| `INTERVAL '1 hour'`     | 1 小时  |
| `INTERVAL '30 minutes'` | 30 分钟 |
| `INTERVAL '10 seconds'` | 10 秒   |
| `INTERVAL '1 month'`    | 1 个月  |
| `INTERVAL '1 year'`     | 1 年    |

时间计算建议如下：

| 建议                                   | 说明                                   |
| -------------------------------------- | -------------------------------------- |
| 使用 `interval`                        | 不要手动拼接字符串计算时间             |
| 时间差转秒用 `EXTRACT(EPOCH FROM ...)` | 适合耗时统计                           |
| 月份计算要谨慎                         | 不同月份天数不同                       |
| 过期判断用范围条件                     | 例如 `expire_time < CURRENT_TIMESTAMP` |
| 大量计算字段可考虑落库                 | 高频统计可使用冗余字段或汇总表         |

### 时间区间查询

时间区间查询是业务系统中最常见的查询之一，例如按创建时间筛选订单、按登录时间筛选日志、按账单日期统计数据。推荐使用左闭右开区间，即 `>= start_time AND < end_time`。

查询某一天的数据：

```sql
SELECT
    id,
    order_no,
    total_amount,
    create_time
FROM app.order_info
WHERE create_time >= TIMESTAMP '2026-05-09 00:00:00'
  AND create_time < TIMESTAMP '2026-05-10 00:00:00'
ORDER BY create_time DESC;
```

查询某个月的数据：

```sql
SELECT
    id,
    order_no,
    total_amount,
    create_time
FROM app.order_info
WHERE create_time >= TIMESTAMP '2026-05-01 00:00:00'
  AND create_time < TIMESTAMP '2026-06-01 00:00:00'
ORDER BY create_time DESC;
```

查询最近 30 天的数据：

```sql
SELECT
    id,
    order_no,
    total_amount,
    create_time
FROM app.order_info
WHERE create_time >= CURRENT_TIMESTAMP - INTERVAL '30 days'
ORDER BY create_time DESC;
```

使用 `BETWEEN` 查询时间要谨慎：

```sql
SELECT
    id,
    order_no,
    create_time
FROM app.order_info
WHERE create_time BETWEEN TIMESTAMP '2026-05-09 00:00:00'
                      AND TIMESTAMP '2026-05-09 23:59:59';
```

上面写法可能遗漏 `23:59:59.001` 之后的数据。更推荐写成：

```sql
SELECT
    id,
    order_no,
    create_time
FROM app.order_info
WHERE create_time >= TIMESTAMP '2026-05-09 00:00:00'
  AND create_time < TIMESTAMP '2026-05-10 00:00:00';
```

时间字段索引示例：

```sql
CREATE INDEX idx_order_info_create_time_id
ON app.order_info (create_time DESC, id DESC);
```

时间区间查询建议如下：

| 建议               | 说明                                            |
| ------------------ | ----------------------------------------------- |
| 使用左闭右开       | `>= start_time AND < end_time`                  |
| 不用字符串截取过滤 | 避免 `to_char(create_time, ...)` 出现在 `WHERE` |
| 时间字段建索引     | 高频时间范围查询应建立索引                      |
| 分页追加主键排序   | `ORDER BY create_time DESC, id DESC`            |
| 明确前端传入时区   | 跨时区系统必须统一转换策略                      |

### 时区处理

时区处理用于解决不同地区用户、服务器和数据库之间的时间显示与存储问题。PostgreSQL 中 `timestamp` 不带时区语义，`timestamptz` 表示带时区语义的时间点。

查看当前时区：

```sql
SHOW timezone;
```

设置当前会话时区：

```sql
SET timezone TO 'Asia/Shanghai';

SELECT CURRENT_TIMESTAMP;
```

查看同一个时间点在不同时区下的展示：

```sql
SELECT
    CURRENT_TIMESTAMP AS current_time,
    CURRENT_TIMESTAMP AT TIME ZONE 'UTC' AS utc_time,
    CURRENT_TIMESTAMP AT TIME ZONE 'Asia/Shanghai' AS shanghai_time,
    CURRENT_TIMESTAMP AT TIME ZONE 'Asia/Tokyo' AS tokyo_time;
```

`timestamptz` 字段示例：

```sql
CREATE TABLE app.global_event_log (
    id BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    event_type VARCHAR(64) NOT NULL,
    event_time TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP
);
```

写入带时区时间：

```sql
INSERT INTO app.global_event_log (
    event_type,
    event_time
) VALUES (
    'USER_LOGIN',
    TIMESTAMPTZ '2026-05-09 10:00:00+09'
);
```

按指定时区展示：

```sql
SELECT
    id,
    event_type,
    event_time,
    event_time AT TIME ZONE 'Asia/Shanghai' AS event_time_shanghai,
    event_time AT TIME ZONE 'Asia/Tokyo' AS event_time_tokyo
FROM app.global_event_log;
```

时区处理建议如下：

| 场景           | 建议                                   |
| -------------- | -------------------------------------- |
| 单地区内部系统 | 可以统一使用 `timestamp`               |
| 跨时区系统     | 推荐使用 `timestamptz`                 |
| 接口传输       | 建议使用 ISO 8601 格式并带时区         |
| 数据库存储     | 统一存储绝对时间点或统一本地时间策略   |
| 前端展示       | 根据用户时区转换展示                   |
| 定时报表       | 明确统计时区，例如按北京时间自然日统计 |

项目中最常见的问题是：数据库、后端 JVM、容器和前端使用的时区不一致。建议在部署规范中统一设置数据库时区、JVM 时区、容器时区和应用序列化格式。

### 日期截断

日期截断用于把时间按指定粒度归一化，例如按天、按月、按小时统计。PostgreSQL 使用 `date_trunc` 完成日期截断。

按天截断：

```sql
SELECT
    date_trunc('day', create_time) AS day_time,
    COUNT(*) AS order_count
FROM app.order_info
GROUP BY date_trunc('day', create_time)
ORDER BY day_time;
```

按月截断：

```sql
SELECT
    date_trunc('month', create_time) AS month_time,
    COUNT(*) AS order_count,
    SUM(total_amount) AS total_amount
FROM app.order_info
GROUP BY date_trunc('month', create_time)
ORDER BY month_time;
```

按小时截断：

```sql
SELECT
    date_trunc('hour', create_time) AS hour_time,
    COUNT(*) AS order_count
FROM app.order_info
WHERE create_time >= CURRENT_DATE
GROUP BY date_trunc('hour', create_time)
ORDER BY hour_time;
```

常用截断粒度如下：

| 粒度 | 示例                                |
| ---- | ----------------------------------- |
| 年   | `date_trunc('year', create_time)`   |
| 月   | `date_trunc('month', create_time)`  |
| 周   | `date_trunc('week', create_time)`   |
| 日   | `date_trunc('day', create_time)`    |
| 小时 | `date_trunc('hour', create_time)`   |
| 分钟 | `date_trunc('minute', create_time)` |

截断后格式化展示：

```sql
SELECT
    to_char(date_trunc('month', create_time), 'YYYY-MM') AS month_text,
    COUNT(*) AS order_count,
    SUM(total_amount) AS total_amount
FROM app.order_info
GROUP BY date_trunc('month', create_time)
ORDER BY month_text;
```

日期截断建议如下：

| 建议                      | 说明                           |
| ------------------------- | ------------------------------ |
| 分组可以用 `date_trunc`   | 适合按天、月、小时统计         |
| 过滤不要滥用 `date_trunc` | 时间范围过滤优先使用原始字段   |
| 周统计注意起始日          | `week` 的含义应和业务约定一致  |
| 展示格式化放最后          | 先按时间分组，再格式化成字符串 |
| 高频统计可落汇总表        | 大表按时间聚合可能成本较高     |

### 日期分组统计

日期分组统计常用于日报、月报、小时趋势、订单趋势、登录趋势等报表。通常先按时间范围过滤，再使用 `date_trunc` 或 `create_time::date` 分组。

按天统计订单数量和金额：

```sql
SELECT
    create_time::date AS order_date,
    COUNT(*) AS order_count,
    SUM(total_amount) AS total_amount
FROM app.order_info
WHERE order_status IN ('PAID', 'FINISHED')
GROUP BY create_time::date
ORDER BY order_date;
```

按月统计订单：

```sql
SELECT
    date_trunc('month', create_time)::date AS order_month,
    COUNT(*) AS order_count,
    SUM(total_amount) AS total_amount
FROM app.order_info
WHERE order_status IN ('PAID', 'FINISHED')
GROUP BY date_trunc('month', create_time)
ORDER BY order_month;
```

按小时统计当天订单：

```sql
SELECT
    date_trunc('hour', create_time) AS order_hour,
    COUNT(*) AS order_count,
    SUM(total_amount) AS total_amount
FROM app.order_info
WHERE create_time >= CURRENT_DATE
  AND create_time < CURRENT_DATE + INTERVAL '1 day'
GROUP BY date_trunc('hour', create_time)
ORDER BY order_hour;
```

补齐没有数据的日期，可以使用 `generate_series`：

```sql
WITH date_range AS (
    SELECT generate_series(
        DATE '2026-05-01',
        DATE '2026-05-07',
        INTERVAL '1 day'
    )::date AS stat_date
),
order_stat AS (
    SELECT
        create_time::date AS order_date,
        COUNT(*) AS order_count,
        SUM(total_amount) AS total_amount
    FROM app.order_info
    WHERE create_time >= DATE '2026-05-01'
      AND create_time < DATE '2026-05-08'
      AND order_status IN ('PAID', 'FINISHED')
    GROUP BY create_time::date
)
SELECT
    d.stat_date,
    COALESCE(s.order_count, 0) AS order_count,
    COALESCE(s.total_amount, 0) AS total_amount
FROM date_range d
LEFT JOIN order_stat s ON s.order_date = d.stat_date
ORDER BY d.stat_date;
```

按日期和状态统计：

```sql
SELECT
    create_time::date AS order_date,
    order_status,
    COUNT(*) AS order_count,
    SUM(total_amount) AS total_amount
FROM app.order_info
GROUP BY
    create_time::date,
    order_status
ORDER BY
    order_date,
    order_status;
```

日期分组统计建议如下：

| 建议                           | 说明                                     |
| ------------------------------ | ---------------------------------------- |
| 先过滤时间范围                 | 减少参与分组的数据量                     |
| 再按日期分组                   | 使用 `create_time::date` 或 `date_trunc` |
| 空日期补齐用 `generate_series` | 适合趋势图                               |
| 金额聚合处理空值               | 使用 `COALESCE(SUM(...), 0)`             |
| 大表统计建汇总表               | 报表高频访问时不要每次扫明细表           |

## 字符串处理

字符串处理用于拼接、截取、替换、大小写转换、模糊搜索、正则匹配、聚合展示和全文搜索。PostgreSQL 的字符串函数较丰富，适合处理常见文本转换和查询需求。

### 字符串拼接

PostgreSQL 可以使用 `||` 操作符或 `concat` 函数拼接字符串。需要注意的是，`||` 遇到 `NULL` 时结果可能为 `NULL`，而 `concat` 会把 `NULL` 当作空字符串处理。

使用 `||` 拼接：

```sql
SELECT
    id,
    username,
    username || ' - ' || nickname AS user_label
FROM app.sys_user;
```

处理空值：

```sql
SELECT
    id,
    username,
    username || ' - ' || COALESCE(nickname, '') AS user_label
FROM app.sys_user;
```

使用 `concat` 拼接：

```sql
SELECT
    id,
    username,
    concat(username, ' - ', nickname) AS user_label
FROM app.sys_user;
```

使用 `concat_ws` 按分隔符拼接：

```sql
SELECT
    id,
    username,
    concat_ws(' / ', username, nickname, phone, email) AS user_info
FROM app.sys_user;
```

生成完整地址示例：

```sql
SELECT
    id,
    concat_ws('', province, city, district, detail_address) AS full_address
FROM app.user_address;
```

字符串拼接建议如下：

| 场景               | 推荐写法               |
| ------------------ | ---------------------- |
| 简单拼接且字段非空 | `字段1                 |
| 可能存在空值       | `concat` 或 `COALESCE` |
| 带分隔符拼接       | `concat_ws`            |
| 展示字段拼接       | 可以在 SQL 中处理      |
| 复杂展示文案       | 通常建议在应用层处理   |

### 字符串截取

字符串截取用于从文本中提取部分内容，例如前几位编码、手机号中间段、日期字符串片段等。PostgreSQL 常用 `substring`、`left`、`right`。

截取指定位置：

```sql
SELECT
    username,
    substring(username FROM 1 FOR 3) AS username_prefix
FROM app.sys_user;
```

使用简化写法：

```sql
SELECT
    username,
    substring(username, 1, 3) AS username_prefix
FROM app.sys_user;
```

截取左侧字符：

```sql
SELECT
    order_no,
    left(order_no, 2) AS order_prefix
FROM app.order_info;
```

截取右侧字符：

```sql
SELECT
    phone,
    right(phone, 4) AS phone_suffix
FROM app.sys_user;
```

手机号脱敏示例：

```sql
SELECT
    id,
    username,
    CASE
        WHEN phone IS NULL THEN NULL
        WHEN length(phone) < 7 THEN phone
        ELSE left(phone, 3) || '****' || right(phone, 4)
    END AS masked_phone
FROM app.sys_user;
```

按分隔符拆分字符串：

```sql
SELECT
    split_part('system:user:list', ':', 1) AS module_code,
    split_part('system:user:list', ':', 2) AS resource_code,
    split_part('system:user:list', ':', 3) AS action_code;
```

截取建议如下：

| 函数                                 | 说明                   |
| ------------------------------------ | ---------------------- |
| `substring(text, start, count)`      | 从指定位置截取固定长度 |
| `left(text, n)`                      | 截取左侧 n 个字符      |
| `right(text, n)`                     | 截取右侧 n 个字符      |
| `split_part(text, delimiter, field)` | 按分隔符取第几个片段   |
| `length(text)`                       | 获取字符串长度         |

### 字符串替换

字符串替换用于清理文本、脱敏、格式化、移除特殊字符等场景。常用函数包括 `replace`、`regexp_replace` 和 `translate`。

普通替换：

```sql
SELECT
    replace('PostgreSQL-17', '-', ' ') AS result_text;
```

替换手机号中间部分：

```sql
SELECT
    id,
    username,
    CASE
        WHEN phone IS NULL THEN NULL
        WHEN length(phone) < 7 THEN phone
        ELSE overlay(phone placing '****' from 4 for 4)
    END AS masked_phone
FROM app.sys_user;
```

移除空格：

```sql
SELECT
    replace(' PostgreSQL 17 ', ' ', '') AS clean_text;
```

使用正则替换多个空白字符：

```sql
SELECT
    regexp_replace('PostgreSQL     17   使用文档', '\s+', ' ', 'g') AS clean_text;
```

保留数字字符：

```sql
SELECT
    regexp_replace('电话：138-0000-0000', '[^0-9]', '', 'g') AS only_number;
```

批量清理手机号中的非数字字符：

```sql
UPDATE app.sys_user
SET phone = regexp_replace(phone, '[^0-9]', '', 'g')
WHERE phone IS NOT NULL;
```

常用替换函数如下：

| 函数                                                | 说明             |
| --------------------------------------------------- | ---------------- |
| `replace(text, from, to)`                           | 普通字符串替换   |
| `regexp_replace(text, pattern, replacement, flags)` | 正则替换         |
| `overlay(text placing text from start for count)`   | 替换指定位置内容 |
| `trim(text)`                                        | 去除首尾空白     |
| `btrim(text)`                                       | 去除首尾指定字符 |
| `ltrim(text)`                                       | 去除左侧空白     |
| `rtrim(text)`                                       | 去除右侧空白     |

字符串清理建议在数据写入前由应用层完成，数据库层可以用于历史数据修复、导入清洗和报表展示。

### 大小写转换

大小写转换常用于用户名、邮箱、编码、搜索关键字的规范化处理。PostgreSQL 使用 `lower` 和 `upper` 进行大小写转换。

转小写：

```sql
SELECT
    email,
    lower(email) AS lower_email
FROM app.sys_user;
```

转大写：

```sql
SELECT
    username,
    upper(username) AS upper_username
FROM app.sys_user;
```

按邮箱忽略大小写查询：

```sql
SELECT
    id,
    username,
    email
FROM app.sys_user
WHERE lower(email) = lower('ADMIN@EXAMPLE.COM');
```

如果大小写不敏感查询很频繁，可以创建表达式索引：

```sql
CREATE INDEX idx_sys_user_lower_email
ON app.sys_user (lower(email));
```

对应查询：

```sql
SELECT
    id,
    username,
    email
FROM app.sys_user
WHERE lower(email) = lower('ADMIN@EXAMPLE.COM');
```

也可以使用 `ILIKE` 做大小写不敏感模糊匹配：

```sql
SELECT
    id,
    username,
    email
FROM app.sys_user
WHERE email ILIKE '%example.com';
```

大小写处理建议如下：

| 场景       | 建议                          |
| ---------- | ----------------------------- |
| 邮箱唯一   | 可统一转小写后入库            |
| 编码字段   | 建议统一大写或小写            |
| 登录名查询 | 可使用 `lower` 表达式索引     |
| 模糊匹配   | 可以使用 `ILIKE`              |
| 高性能搜索 | 考虑 `pg_trgm` 或专门搜索引擎 |

### 模糊匹配

模糊匹配用于按关键字搜索字符串。PostgreSQL 常用 `LIKE` 和 `ILIKE`。`LIKE` 区分大小写，`ILIKE` 不区分大小写。

查询昵称包含“张”的用户：

```sql
SELECT
    id,
    username,
    nickname
FROM app.sys_user
WHERE nickname LIKE '%张%';
```

查询邮箱包含指定域名，忽略大小写：

```sql
SELECT
    id,
    username,
    email
FROM app.sys_user
WHERE email ILIKE '%example.com';
```

前缀匹配：

```sql
SELECT
    id,
    username
FROM app.sys_user
WHERE username LIKE 'admin%';
```

后缀匹配：

```sql
SELECT
    id,
    username,
    email
FROM app.sys_user
WHERE email LIKE '%@example.com';
```

通配符说明：

| 通配符 | 说明             |
| ------ | ---------------- |
| `%`    | 匹配任意长度字符 |
| `_`    | 匹配单个字符     |

如果需要搜索字面量 `%` 或 `_`，可以使用 `ESCAPE`：

```sql
SELECT
    id,
    title
FROM app.article
WHERE title LIKE '%\_%' ESCAPE '\';
```

模糊匹配性能注意事项：

| 写法                       | 索引友好程度 | 说明                                 |
| -------------------------- | ------------ | ------------------------------------ |
| `username LIKE 'admin%'`   | 较友好       | 前缀匹配可能利用索引                 |
| `username LIKE '%admin'`   | 较差         | 后缀匹配通常难以使用普通 B-Tree 索引 |
| `username LIKE '%admin%'`  | 较差         | 包含匹配通常需要特殊索引             |
| `username ILIKE '%admin%'` | 较差         | 大小写不敏感包含匹配成本更高         |

如果需要高频模糊搜索，可以考虑启用 `pg_trgm` 扩展并创建 GIN 或 GiST 索引：

```sql
CREATE EXTENSION IF NOT EXISTS pg_trgm;

CREATE INDEX idx_sys_user_nickname_trgm
ON app.sys_user
USING GIN (nickname gin_trgm_ops);
```

### 正则匹配

正则匹配用于更复杂的字符串判断，例如校验格式、提取模式、过滤特定字符等。PostgreSQL 支持正则操作符和正则函数。

常用正则操作符如下：

| 操作符 | 说明                   |
| ------ | ---------------------- |
| `~`    | 区分大小写正则匹配     |
| `~*`   | 不区分大小写正则匹配   |
| `!~`   | 区分大小写正则不匹配   |
| `!~*`  | 不区分大小写正则不匹配 |

查询手机号格式正确的数据：

```sql
SELECT
    id,
    username,
    phone
FROM app.sys_user
WHERE phone ~ '^1[0-9]{10}$';
```

查询邮箱格式大致正确的数据：

```sql
SELECT
    id,
    username,
    email
FROM app.sys_user
WHERE email ~* '^[A-Z0-9._%+-]+@[A-Z0-9.-]+\.[A-Z]{2,}$';
```

查询用户名包含非字母数字下划线的数据：

```sql
SELECT
    id,
    username
FROM app.sys_user
WHERE username !~ '^[a-zA-Z0-9_]+$';
```

使用正则替换清理文本：

```sql
SELECT
    regexp_replace('PostgreSQL     17   使用文档', '\s+', ' ', 'g') AS clean_text;
```

使用正则拆分字符串：

```sql
SELECT
    regexp_split_to_array('java, spring;postgresql sql', '[,;\s]+') AS words;
```

正则匹配建议如下：

| 建议                 | 说明                         |
| -------------------- | ---------------------------- |
| 适合复杂格式判断     | 手机号、邮箱、编码规则等     |
| 不适合高频大范围搜索 | 正则匹配通常成本较高         |
| 入库校验优先应用层   | 数据库层可作为兜底约束       |
| 可配合检查约束       | 稳定格式规则可以写入 `CHECK` |
| 复杂搜索考虑全文搜索 | 不要用正则替代全文检索       |

使用检查约束限制编码格式：

```sql
ALTER TABLE app.sys_role
ADD CONSTRAINT ck_sys_role_code_format
CHECK (role_code ~ '^[A-Z0-9_]+$');
```

### 字符串聚合

字符串聚合用于把多行文本合并成一个字符串。PostgreSQL 常用 `string_agg`，适合展示用户角色、标签、分类名称等信息。

按用户聚合角色名称：

```sql
SELECT
    u.id,
    u.username,
    string_agg(r.role_name, ',' ORDER BY r.role_name) AS role_names
FROM app.sys_user u
LEFT JOIN app.sys_user_role ur ON ur.user_id = u.id
LEFT JOIN app.sys_role r ON r.id = ur.role_id
WHERE u.deleted = FALSE
GROUP BY
    u.id,
    u.username;
```

聚合去重角色编码：

```sql
SELECT
    u.id,
    u.username,
    string_agg(DISTINCT r.role_code, ',' ORDER BY r.role_code) AS role_codes
FROM app.sys_user u
LEFT JOIN app.sys_user_role ur ON ur.user_id = u.id
LEFT JOIN app.sys_role r ON r.id = ur.role_id
WHERE u.deleted = FALSE
GROUP BY
    u.id,
    u.username;
```

按订单聚合商品名称：

```sql
SELECT
    o.id,
    o.order_no,
    string_agg(oi.product_name, '、' ORDER BY oi.id) AS product_names
FROM app.order_info o
LEFT JOIN app.order_item oi ON oi.order_id = o.id
GROUP BY
    o.id,
    o.order_no;
```

过滤空值后聚合：

```sql
SELECT
    u.id,
    u.username,
    COALESCE(
        string_agg(r.role_name, ',' ORDER BY r.role_name)
            FILTER (WHERE r.role_name IS NOT NULL),
        ''
    ) AS role_names
FROM app.sys_user u
LEFT JOIN app.sys_user_role ur ON ur.user_id = u.id
LEFT JOIN app.sys_role r ON r.id = ur.role_id
WHERE u.deleted = FALSE
GROUP BY
    u.id,
    u.username;
```

字符串聚合建议如下：

| 建议                      | 说明                        |
| ------------------------- | --------------------------- |
| 聚合时指定排序            | 保证结果稳定                |
| 需要去重时使用 `DISTINCT` | 避免重复关联导致重复文本    |
| 处理空值                  | 使用 `FILTER` 和 `COALESCE` |
| 展示用可以聚合            | 适合报表和列表展示          |
| 不建议持久化聚合字符串    | 关系数据应保留明细表        |

### 全文搜索基础

全文搜索用于按词项匹配文本内容，比简单 `LIKE '%关键字%'` 更适合处理长文本搜索。PostgreSQL 内置全文搜索能力，核心类型和函数包括 `tsvector`、`tsquery`、`to_tsvector`、`to_tsquery`、`plainto_tsquery`、`websearch_to_tsquery`。

基础概念如下：

| 概念              | 说明                           |
| ----------------- | ------------------------------ |
| `tsvector`        | 文档向量，表示被分词后的文本   |
| `tsquery`         | 查询条件，表示搜索词项         |
| `@@`              | 全文搜索匹配操作符             |
| `to_tsvector`     | 把文本转换为全文搜索向量       |
| `to_tsquery`      | 把查询表达式转换为全文搜索条件 |
| `plainto_tsquery` | 把普通文本转换为查询条件       |
| `ts_rank`         | 计算搜索相关度                 |
| `ts_headline`     | 生成搜索高亮文本               |

创建文章表：

```sql
CREATE TABLE app.article_search (
    id BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    title VARCHAR(255) NOT NULL,
    content TEXT NOT NULL,
    create_time TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);
```

插入测试数据：

```sql
INSERT INTO app.article_search (
    title,
    content
) VALUES
    (
        'PostgreSQL 17 使用文档',
        'PostgreSQL 是一个功能强大的开源关系型数据库，支持事务、JSONB、全文搜索和扩展插件。'
    ),
    (
        'Spring Boot 集成 PostgreSQL',
        'Spring Boot 可以通过 JDBC、MyBatis、JPA 等方式连接 PostgreSQL 数据库。'
    );
```

英文全文搜索示例：

```sql
SELECT
    id,
    title,
    content
FROM app.article_search
WHERE to_tsvector('english', title || ' ' || content)
      @@ plainto_tsquery('english', 'PostgreSQL database');
```

按相关度排序：

```sql
SELECT
    id,
    title,
    ts_rank(
        to_tsvector('english', title || ' ' || content),
        plainto_tsquery('english', 'PostgreSQL database')
    ) AS rank_score
FROM app.article_search
WHERE to_tsvector('english', title || ' ' || content)
      @@ plainto_tsquery('english', 'PostgreSQL database')
ORDER BY rank_score DESC;
```

创建全文搜索索引：

```sql
CREATE INDEX idx_article_search_fulltext
ON app.article_search
USING GIN (
    to_tsvector('english', title || ' ' || content)
);
```

使用高亮：

```sql
SELECT
    id,
    title,
    ts_headline(
        'english',
        content,
        plainto_tsquery('english', 'PostgreSQL database')
    ) AS highlighted_content
FROM app.article_search
WHERE to_tsvector('english', title || ' ' || content)
      @@ plainto_tsquery('english', 'PostgreSQL database');
```

如果需要存储生成列来保存搜索向量，可以这样设计：

```sql
CREATE TABLE app.article_search_vector (
    id BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    title VARCHAR(255) NOT NULL,
    content TEXT NOT NULL,
    search_vector TSVECTOR GENERATED ALWAYS AS (
        to_tsvector('english', title || ' ' || content)
    ) STORED,
    create_time TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_article_search_vector
ON app.article_search_vector
USING GIN (search_vector);
```

查询生成列：

```sql
SELECT
    id,
    title
FROM app.article_search_vector
WHERE search_vector @@ plainto_tsquery('english', 'PostgreSQL database');
```

全文搜索使用建议如下：

| 场景               | 建议                                        |
| ------------------ | ------------------------------------------- |
| 英文文本搜索       | PostgreSQL 内置全文搜索较适合               |
| 中文全文搜索       | 需要结合分词扩展或专门搜索引擎              |
| 简单短文本模糊匹配 | 可以使用 `LIKE`、`ILIKE` 或 `pg_trgm`       |
| 长文本搜索         | 使用 `tsvector` + GIN 索引                  |
| 搜索相关度排序     | 使用 `ts_rank`                              |
| 搜索高亮           | 使用 `ts_headline`                          |
| 复杂搜索业务       | 可考虑 Elasticsearch、OpenSearch 等搜索引擎 |

PostgreSQL 全文搜索适合中小规模站内搜索、后台内容搜索和结构化系统内搜索。如果业务需要复杂中文分词、拼音搜索、同义词、错别字纠正、多字段权重和大规模检索，建议使用专门搜索引擎。


## 索引设计

索引用于提升数据检索效率。PostgreSQL 支持 B-Tree、Hash、GiST、SP-GiST、GIN、BRIN 等多种索引访问方法，默认 `CREATE INDEX` 创建的是 B-Tree 索引。不同索引类型适合不同查询场景，不能简单地认为“字段越多索引越多越好”。([PostgreSQL](https://www.postgresql.org/docs/17/indexes.html?utm_source=chatgpt.com))

常见索引类型选择如下：

| 索引类型   | 适用场景                                     |
| ---------- | -------------------------------------------- |
| B-Tree     | 等值查询、范围查询、排序、唯一约束，最常用   |
| Hash       | 简单等值查询，使用场景较少                   |
| GIN        | JSONB、数组、全文搜索、多值字段              |
| GiST       | 几何、范围类型、全文搜索、PostGIS 等         |
| BRIN       | 超大表、时间递增数据、物理顺序和字段值强相关 |
| 表达式索引 | 对函数或表达式查询加速                       |
| 部分索引   | 只索引满足条件的一部分数据                   |
| 唯一索引   | 保证字段或字段组合唯一                       |

### B-Tree 索引

B-Tree 是 PostgreSQL 默认索引类型，也是最常用的索引类型。它适合可排序数据上的等值查询、范围查询、排序、分组和唯一性约束。官方文档说明，默认 `CREATE INDEX` 会创建 B-Tree 索引，并且 B-Tree 适合最常见的场景。([PostgreSQL](https://www.postgresql.org/docs/current/indexes-types.html?utm_source=chatgpt.com))

创建普通 B-Tree 索引：

```sql
CREATE INDEX idx_sys_user_username
ON app.sys_user (username);
```

适合以下查询：

```sql
SELECT
    id,
    username,
    nickname
FROM app.sys_user
WHERE username = 'admin';
```

创建时间范围查询索引：

```sql
CREATE INDEX idx_order_info_create_time
ON app.order_info (create_time);
```

适合以下查询：

```sql
SELECT
    id,
    order_no,
    total_amount,
    create_time
FROM app.order_info
WHERE create_time >= TIMESTAMP '2026-05-01 00:00:00'
  AND create_time < TIMESTAMP '2026-06-01 00:00:00'
ORDER BY create_time DESC;
```

创建排序友好的索引：

```sql
CREATE INDEX idx_order_info_create_time_id_desc
ON app.order_info (create_time DESC, id DESC);
```

适合分页查询：

```sql
SELECT
    id,
    order_no,
    total_amount,
    create_time
FROM app.order_info
WHERE order_status = 'PAID'
ORDER BY create_time DESC, id DESC
LIMIT 20;
```

B-Tree 索引适合以下操作：

| 查询类型   | 示例                                       |
| ---------- | ------------------------------------------ |
| 等值查询   | `username = 'admin'`                       |
| 范围查询   | `create_time >= ... AND create_time < ...` |
| 排序       | `ORDER BY create_time DESC`                |
| 最大最小值 | `MIN(create_time)`、`MAX(create_time)`     |
| 前缀匹配   | `username LIKE 'admin%'`                   |
| 唯一约束   | `UNIQUE (username)`                        |

B-Tree 索引设计建议：

| 建议                       | 说明                                                         |
| -------------------------- | ------------------------------------------------------------ |
| 高频等值字段适合建索引     | 如用户名、订单号、业务编码                                   |
| 高频范围字段适合建索引     | 如创建时间、更新时间、金额                                   |
| 排序字段可纳入索引         | 常见分页排序可用 `(create_time DESC, id DESC)`               |
| 低选择性字段谨慎单独建索引 | 如 `enabled`、`deleted` 单独索引通常价值不高                 |
| 避免重复索引               | `(username)` 和 `(username, id)` 可能存在冗余，需要结合查询判断 |

### Hash 索引

Hash 索引适合简单等值查询。PostgreSQL 支持通过 `USING HASH` 创建 Hash 索引，但普通项目中使用频率较低，因为 B-Tree 同样支持等值查询，并且适用范围更广。官方文档将 Hash 作为 PostgreSQL 内置索引类型之一，可以通过 `USING HASH` 显式创建。([PostgreSQL](https://www.postgresql.org/docs/current/indexes-types.html?utm_source=chatgpt.com))

创建 Hash 索引：

```sql
CREATE INDEX idx_sys_user_username_hash
ON app.sys_user
USING HASH (username);
```

适合等值查询：

```sql
SELECT
    id,
    username,
    nickname
FROM app.sys_user
WHERE username = 'admin';
```

Hash 索引不适合范围查询和排序：

```sql
-- Hash 索引不适合这种范围查询
SELECT
    id,
    username
FROM app.sys_user
WHERE username > 'admin'
ORDER BY username;
```

Hash 索引使用建议：

| 场景         | 建议                                         |
| ------------ | -------------------------------------------- |
| 纯等值查询   | 可以考虑 Hash 索引                           |
| 等值 + 排序  | 优先 B-Tree                                  |
| 范围查询     | 使用 B-Tree                                  |
| 普通业务系统 | 默认优先 B-Tree                              |
| 特殊性能验证 | 可通过 `EXPLAIN ANALYZE` 对比 B-Tree 和 Hash |

实际项目中，如果没有明确测试收益，通常不优先使用 Hash 索引。

### GIN 索引

GIN 是倒排索引，适合一个字段中包含多个元素的场景，例如数组、JSONB、全文搜索向量等。官方文档说明，GIN 适用于包含多个组成值的数据，例如数组，并且可以高效处理元素存在性检查。([PostgreSQL Japan](https://www.postgresql.jp/document/17/html/indexes-types.html?utm_source=chatgpt.com))

数组字段 GIN 索引：

```sql
CREATE INDEX idx_article_tag_codes_gin
ON app.article
USING GIN (tag_codes);
```

适合以下查询：

```sql
SELECT
    id,
    title,
    tag_codes
FROM app.article
WHERE tag_codes @> ARRAY['postgresql'];
```

JSONB 字段 GIN 索引：

```sql
CREATE INDEX idx_event_log_event_data_gin
ON app.event_log
USING GIN (event_data);
```

适合 JSONB 包含查询：

```sql
SELECT
    id,
    event_type,
    event_data
FROM app.event_log
WHERE event_data @> '{"success": true}'::jsonb;
```

全文搜索 GIN 索引：

```sql
CREATE INDEX idx_article_search_fulltext_gin
ON app.article_search
USING GIN (
    to_tsvector('english', title || ' ' || content)
);
```

适合全文搜索：

```sql
SELECT
    id,
    title
FROM app.article_search
WHERE to_tsvector('english', title || ' ' || content)
      @@ plainto_tsquery('english', 'postgresql database');
```

GIN 索引适合以下场景：

| 场景           | 示例                                |
| -------------- | ----------------------------------- |
| 数组包含       | `tag_codes @> ARRAY['postgresql']`  |
| 数组交集       | `tag_codes && ARRAY['java', 'sql']` |
| JSONB 包含     | `event_data @> '{"success": true}'` |
| JSONB key 判断 | `event_data ? 'username'`           |
| 全文搜索       | `tsvector @@ tsquery`               |

GIN 索引写入维护成本通常高于普通 B-Tree 索引，因此不要对所有 JSONB 或数组字段无脑创建 GIN 索引。应根据高频查询条件和执行计划决定。

### GiST 索引

GiST 是一种通用索引框架，适合几何数据、范围类型、全文搜索、相似性搜索以及 PostGIS 等扩展场景。PostgreSQL 官方文档将 GiST 列为内置索引访问方法之一，具体支持的操作符取决于索引策略和操作符类。([PostgreSQL](https://www.postgresql.org/docs/current/indexes-types.html?utm_source=chatgpt.com))

范围类型 GiST 索引示例：

```sql
CREATE TABLE app.room_booking (
    id BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    room_id BIGINT NOT NULL,
    booking_range TSRANGE NOT NULL,
    create_time TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);
```

创建 GiST 索引：

```sql
CREATE INDEX idx_room_booking_range_gist
ON app.room_booking
USING GIST (booking_range);
```

查询某个时间段是否有重叠预订：

```sql
SELECT
    id,
    room_id,
    booking_range
FROM app.room_booking
WHERE booking_range && tsrange(
    TIMESTAMP '2026-05-09 10:00:00',
    TIMESTAMP '2026-05-09 12:00:00',
    '[)'
);
```

几何类型 GiST 索引示例：

```sql
CREATE TABLE app.store_location (
    id BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    store_name VARCHAR(128) NOT NULL,
    location POINT NOT NULL
);

CREATE INDEX idx_store_location_gist
ON app.store_location
USING GIST (location);
```

GiST 索引适合以下场景：

| 场景     | 说明                         |
| -------- | ---------------------------- |
| 范围类型 | 时间范围、数值范围、区间重叠 |
| 几何类型 | 点、线、矩形、圆等           |
| PostGIS  | 空间数据查询                 |
| 排他约束 | 防止时间范围重叠等           |
| 特殊扩展 | 依赖扩展提供的操作符类       |

普通业务系统中 GiST 使用频率低于 B-Tree 和 GIN，但在时间范围冲突、空间查询、PostGIS 场景中非常有价值。

### BRIN 索引

BRIN 是块范围索引，适合超大表，并且字段值和表中物理存储顺序具有较强相关性的场景。例如按时间持续追加写入的日志表、事件表、订单表。官方文档说明，BRIN 会存储连续物理块范围内的值摘要，当字段值和物理顺序相关性较强时最有效。([PostgreSQL](https://www.postgresql.org/docs/current/indexes-types.html?utm_source=chatgpt.com))

日志表示例：

```sql
CREATE TABLE app.operation_log (
    id BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    operation_type VARCHAR(64) NOT NULL,
    operation_content TEXT,
    create_time TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);
```

创建 BRIN 索引：

```sql
CREATE INDEX idx_operation_log_create_time_brin
ON app.operation_log
USING BRIN (create_time);
```

适合按时间范围查询：

```sql
SELECT
    id,
    operation_type,
    create_time
FROM app.operation_log
WHERE create_time >= TIMESTAMP '2026-05-01 00:00:00'
  AND create_time < TIMESTAMP '2026-06-01 00:00:00';
```

BRIN 索引和 B-Tree 索引对比：

| 对比项       | B-Tree                    | BRIN                        |
| ------------ | ------------------------- | --------------------------- |
| 索引体积     | 相对较大                  | 很小                        |
| 精确定位能力 | 强                        | 较弱                        |
| 适合数据量   | 中小表、大表均可          | 特别适合超大表              |
| 适合字段     | 高选择性字段、排序字段    | 与物理顺序相关字段          |
| 常见场景     | 订单号、用户 ID、创建时间 | 日志时间、事件时间、递增 ID |

BRIN 索引适合数据量很大、按时间追加写入、主要按时间范围扫描的表。如果表经常乱序插入或字段值与物理顺序无关，BRIN 效果会下降。

### 唯一索引

唯一索引用于保证字段或字段组合的唯一性。PostgreSQL 中唯一约束通常会自动创建唯一索引，直接创建唯一索引也可以实现唯一性约束。官方 `CREATE INDEX` 文档说明，当前只有 B-Tree 支持唯一索引。([PostgreSQL](https://www.postgresql.org/docs/17/sql-createindex.html?utm_source=chatgpt.com))

创建唯一索引：

```sql
CREATE UNIQUE INDEX uk_sys_user_username
ON app.sys_user (username);
```

适合保证用户名唯一：

```sql
INSERT INTO app.sys_user (
    username,
    nickname
) VALUES (
    'admin',
    '系统管理员'
);
```

创建联合唯一索引：

```sql
CREATE UNIQUE INDEX uk_sys_dict_item_type_code
ON app.sys_dict_item (dict_type, item_code);
```

适合保证同一字典类型下编码唯一：

```sql
SELECT
    id,
    dict_type,
    item_code,
    item_name
FROM app.sys_dict_item
WHERE dict_type = 'user_status'
  AND item_code = 'enabled';
```

软删除场景下的唯一索引：

```sql
CREATE UNIQUE INDEX uk_sys_user_username_not_deleted
ON app.sys_user (username)
WHERE deleted = FALSE;
```

该索引只保证未删除数据中的用户名唯一，已逻辑删除的数据不会参与唯一性判断。

唯一索引设计建议：

| 场景               | 建议                                           |
| ------------------ | ---------------------------------------------- |
| 用户名唯一         | `UNIQUE (username)`                            |
| 订单号唯一         | `UNIQUE (order_no)`                            |
| 租户内编码唯一     | `UNIQUE (tenant_id, code)`                     |
| 软删除唯一         | 使用部分唯一索引                               |
| 邮箱忽略大小写唯一 | 使用表达式唯一索引，如 `UNIQUE (lower(email))` |

邮箱忽略大小写唯一示例：

```sql
CREATE UNIQUE INDEX uk_sys_user_lower_email
ON app.sys_user (lower(email))
WHERE email IS NOT NULL;
```

唯一索引不仅用于提升查询效率，更重要的是保证数据正确性。业务唯一规则应尽量由数据库兜底。

### 联合索引

联合索引也称多列索引，用于多个字段组合查询、排序或唯一性控制。PostgreSQL 支持多列索引，但是否能有效使用取决于查询条件、字段顺序、选择性和排序方式。官方文档中也将多列索引作为索引设计的重要内容。([PostgreSQL](https://www.postgresql.org/docs/17/indexes.html?utm_source=chatgpt.com))

创建订单查询联合索引：

```sql
CREATE INDEX idx_order_info_user_status_time
ON app.order_info (user_id, order_status, create_time DESC);
```

适合以下查询：

```sql
SELECT
    id,
    order_no,
    total_amount,
    create_time
FROM app.order_info
WHERE user_id = 10001
  AND order_status = 'PAID'
ORDER BY create_time DESC
LIMIT 20;
```

租户业务表联合唯一索引：

```sql
CREATE UNIQUE INDEX uk_tenant_config_key
ON app.sys_config (tenant_id, config_key);
```

适合以下查询：

```sql
SELECT
    id,
    config_key,
    config_value
FROM app.sys_config
WHERE tenant_id = 10001
  AND config_key = 'system.name';
```

联合索引字段顺序建议：

| 字段类型         | 放置建议                                       |
| ---------------- | ---------------------------------------------- |
| 等值过滤字段     | 放前面，如 `tenant_id`、`user_id`、`status`    |
| 范围过滤字段     | 通常放在等值字段后，如 `create_time`、`amount` |
| 排序字段         | 结合 `ORDER BY` 放在合适位置                   |
| 高选择性字段     | 通常比低选择性字段更适合作为前导字段           |
| 单独查询频繁字段 | 需要考虑是否要独立索引                         |

示例：订单列表常见查询条件是用户、状态、时间范围和排序。

```sql
CREATE INDEX idx_order_info_user_status_create_id
ON app.order_info (user_id, order_status, create_time DESC, id DESC);
```

适合：

```sql
SELECT
    id,
    order_no,
    order_status,
    total_amount,
    create_time
FROM app.order_info
WHERE user_id = 10001
  AND order_status = 'PAID'
  AND create_time >= TIMESTAMP '2026-05-01 00:00:00'
ORDER BY create_time DESC, id DESC
LIMIT 20;
```

联合索引设计常见误区：

| 误区                 | 说明                                                         |
| -------------------- | ------------------------------------------------------------ |
| 字段越多越好         | 字段过多会增加维护成本，也可能无法被有效利用                 |
| 忽略字段顺序         | 联合索引字段顺序会影响查询匹配                               |
| 重复创建前缀索引     | 已有 `(user_id, status)` 时，单独 `(user_id)` 是否需要要结合查询判断 |
| 只按 WHERE 建索引    | `ORDER BY` 和分页也要一起考虑                                |
| 低选择性字段单独前置 | 如 `deleted`、`enabled` 单独放最前通常价值有限               |

### 表达式索引

表达式索引用于对函数或表达式结果创建索引。官方文档说明，索引字段可以是基于表中一个或多个列计算出来的表达式，例如 `upper(col)`，从而让 `WHERE upper(col) = 'JIM'` 这类查询使用索引。([PostgreSQL](https://www.postgresql.org/docs/17/sql-createindex.html?utm_source=chatgpt.com))

邮箱忽略大小写查询索引：

```sql
CREATE INDEX idx_sys_user_lower_email
ON app.sys_user (lower(email));
```

适合以下查询：

```sql
SELECT
    id,
    username,
    email
FROM app.sys_user
WHERE lower(email) = lower('ADMIN@EXAMPLE.COM');
```

JSONB 字段表达式索引：

```sql
CREATE INDEX idx_order_info_channel
ON app.order_info ((extra_data ->> 'channel'));
```

适合以下查询：

```sql
SELECT
    id,
    order_no,
    extra_data
FROM app.order_info
WHERE extra_data ->> 'channel' = 'APP';
```

数值转换表达式索引：

```sql
CREATE INDEX idx_order_info_coupon_id
ON app.order_info (((extra_data ->> 'couponId')::BIGINT));
```

适合以下查询：

```sql
SELECT
    id,
    order_no
FROM app.order_info
WHERE (extra_data ->> 'couponId')::BIGINT = 30001;
```

日期表达式索引：

```sql
CREATE INDEX idx_order_info_create_date
ON app.order_info ((create_time::date));
```

适合以下查询：

```sql
SELECT
    COUNT(*) AS order_count
FROM app.order_info
WHERE create_time::date = DATE '2026-05-09';
```

不过时间查询更推荐范围条件：

```sql
SELECT
    COUNT(*) AS order_count
FROM app.order_info
WHERE create_time >= TIMESTAMP '2026-05-09 00:00:00'
  AND create_time < TIMESTAMP '2026-05-10 00:00:00';
```

表达式索引使用建议：

| 场景               | 示例                                    |
| ------------------ | --------------------------------------- |
| 忽略大小写查询     | `lower(email)`                          |
| JSON 字段提取      | `(extra_data ->> 'channel')`            |
| 类型转换查询       | `((extra_data ->> 'couponId')::BIGINT)` |
| 日期截断查询       | `(create_time::date)`                   |
| 格式化或清洗后查询 | `trim(code)`、`upper(code)`             |

需要注意，查询表达式必须和索引表达式匹配，优化器才更可能使用该索引。表达式索引中使用的函数需要满足索引定义对函数稳定性的要求，官方文档也说明索引定义中的函数和操作符必须是 immutable。([PostgreSQL](https://www.postgresql.org/docs/17/sql-createindex.html?utm_source=chatgpt.com))

### 部分索引

部分索引只索引表中满足指定条件的数据。官方文档说明，当 `CREATE INDEX` 中存在 `WHERE` 子句时，会创建部分索引，适合只对表中更有价值的一部分数据建立索引，也可以配合 `UNIQUE` 实现部分唯一性。([PostgreSQL](https://www.postgresql.org/docs/17/sql-createindex.html?utm_source=chatgpt.com))

软删除表中只索引未删除数据：

```sql
CREATE INDEX idx_sys_user_not_deleted_username
ON app.sys_user (username)
WHERE deleted = FALSE;
```

适合以下查询：

```sql
SELECT
    id,
    username,
    nickname
FROM app.sys_user
WHERE deleted = FALSE
  AND username = 'admin';
```

只索引待处理订单：

```sql
CREATE INDEX idx_order_info_pending_create_time
ON app.order_info (create_time DESC, id DESC)
WHERE order_status = 'PENDING';
```

适合以下查询：

```sql
SELECT
    id,
    order_no,
    create_time
FROM app.order_info
WHERE order_status = 'PENDING'
ORDER BY create_time DESC, id DESC
LIMIT 50;
```

部分唯一索引：

```sql
CREATE UNIQUE INDEX uk_sys_user_username_active
ON app.sys_user (username)
WHERE deleted = FALSE;
```

该索引用于保证未删除用户中的用户名唯一。

只索引失败日志：

```sql
CREATE INDEX idx_event_log_failed_event
ON app.event_log (event_type, create_time DESC)
WHERE event_data @> '{"success": false}'::jsonb;
```

部分索引适合以下场景：

| 场景           | 示例                         |
| -------------- | ---------------------------- |
| 软删除数据     | `WHERE deleted = FALSE`      |
| 状态表热点数据 | `WHERE status = 'PENDING'`   |
| 异常日志       | `WHERE success = FALSE`      |
| 非空字段       | `WHERE email IS NOT NULL`    |
| 部分唯一约束   | 未删除数据唯一、启用配置唯一 |

部分索引使用建议：

| 建议                   | 说明                             |
| ---------------------- | -------------------------------- |
| 条件必须稳定           | 不适合经常变化且范围很大的条件   |
| 查询条件要包含索引条件 | 查询中需要能推导出部分索引条件   |
| 适合热点子集           | 如未处理、未删除、有效数据       |
| 能显著减小索引体积     | 避免索引大量冷数据               |
| 可用于软删除唯一       | 比全表唯一约束更适合逻辑删除场景 |

### JSONB 索引

JSONB 索引用于提升 JSONB 字段查询性能。常见方式包括 GIN 索引、表达式索引和部分索引。JSONB 查询如果频繁使用 `@>`、`?`、`?|`、`?&`，通常考虑 GIN 索引；如果频繁使用 `->>` 提取字段做等值查询，通常考虑表达式索引。GIN 是 PostgreSQL 内置索引方法之一，适合数组、JSONB、全文搜索等多值结构。([PostgreSQL Japan](https://www.postgresql.jp/document/17/html/indexes-types.html?utm_source=chatgpt.com))

JSONB GIN 索引：

```sql
CREATE INDEX idx_event_log_event_data_gin
ON app.event_log
USING GIN (event_data);
```

适合包含查询：

```sql
SELECT
    id,
    event_type,
    event_data
FROM app.event_log
WHERE event_data @> '{"success": true}'::jsonb;
```

JSONB key 存在查询：

```sql
SELECT
    id,
    event_type,
    event_data
FROM app.event_log
WHERE event_data ? 'username';
```

JSONB 表达式索引：

```sql
CREATE INDEX idx_event_log_username
ON app.event_log ((event_data ->> 'username'));
```

适合字段提取查询：

```sql
SELECT
    id,
    event_type,
    event_data
FROM app.event_log
WHERE event_data ->> 'username' = 'admin';
```

JSONB 数值字段索引：

```sql
CREATE INDEX idx_event_log_user_id
ON app.event_log (((event_data ->> 'userId')::BIGINT));
```

适合：

```sql
SELECT
    id,
    event_type,
    event_data
FROM app.event_log
WHERE (event_data ->> 'userId')::BIGINT = 10001;
```

JSONB 部分索引：

```sql
CREATE INDEX idx_event_log_user_login_username
ON app.event_log ((event_data ->> 'username'))
WHERE event_type = 'USER_LOGIN';
```

适合：

```sql
SELECT
    id,
    event_type,
    event_data
FROM app.event_log
WHERE event_type = 'USER_LOGIN'
  AND event_data ->> 'username' = 'admin';
```

JSONB 索引选择建议：

| 查询方式                                   | 推荐索引                                |
| ------------------------------------------ | --------------------------------------- |
| `event_data @> '{"a": 1}'`                 | GIN                                     |
| `event_data ? 'key'`                       | GIN                                     |
| `event_data ->> 'username' = 'admin'`      | 表达式 B-Tree                           |
| `(event_data ->> 'amount')::numeric > 100` | 表达式索引，需结合查询验证              |
| 某类事件中的 JSON 字段查询                 | 部分表达式索引                          |
| 多条件组合查询                             | 普通字段索引 + JSONB 表达式索引综合评估 |

JSONB 索引优化建议：

| 建议                         | 说明                                |
| ---------------------------- | ----------------------------------- |
| 高频字段普通列化             | 不要长期把核心过滤字段藏在 JSONB 中 |
| 包含查询用 GIN               | `@>`、`?` 适合 GIN                  |
| 提取字段等值查询用表达式索引 | `->>` 场景更适合表达式索引          |
| 避免过多 JSONB 索引          | 写入成本和索引体积会增加            |
| 用执行计划验证               | 通过 `EXPLAIN ANALYZE` 确认是否命中 |

### 全文索引

全文索引用于提升全文搜索性能。PostgreSQL 内置全文搜索能力，常见做法是对 `to_tsvector(...)` 创建 GIN 索引。GIN 是全文搜索中常见的索引方式之一。([PostgreSQL](https://www.postgresql.org/docs/17/indextypes.html?utm_source=chatgpt.com))

创建文章搜索表：

```sql
CREATE TABLE app.article_search (
    id BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    title VARCHAR(255) NOT NULL,
    content TEXT NOT NULL,
    create_time TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);
```

创建全文索引：

```sql
CREATE INDEX idx_article_search_fulltext
ON app.article_search
USING GIN (
    to_tsvector('english', title || ' ' || content)
);
```

全文搜索查询：

```sql
SELECT
    id,
    title,
    ts_rank(
        to_tsvector('english', title || ' ' || content),
        plainto_tsquery('english', 'postgresql database')
    ) AS rank_score
FROM app.article_search
WHERE to_tsvector('english', title || ' ' || content)
      @@ plainto_tsquery('english', 'postgresql database')
ORDER BY rank_score DESC;
```

使用生成列保存搜索向量：

```sql
CREATE TABLE app.article_search_vector (
    id BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    title VARCHAR(255) NOT NULL,
    content TEXT NOT NULL,
    search_vector TSVECTOR GENERATED ALWAYS AS (
        to_tsvector('english', title || ' ' || content)
    ) STORED,
    create_time TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);
```

对生成列创建索引：

```sql
CREATE INDEX idx_article_search_vector_gin
ON app.article_search_vector
USING GIN (search_vector);
```

查询生成列：

```sql
SELECT
    id,
    title
FROM app.article_search_vector
WHERE search_vector @@ plainto_tsquery('english', 'postgresql database');
```

全文索引建议：

| 场景           | 建议                                |
| -------------- | ----------------------------------- |
| 英文全文搜索   | `to_tsvector('english', ...)` + GIN |
| 中文全文搜索   | 需要分词扩展或外部搜索引擎          |
| 简单模糊搜索   | 可以用 `ILIKE` 或 `pg_trgm`         |
| 长文本搜索     | 优先全文索引                        |
| 搜索相关度排序 | 使用 `ts_rank`                      |
| 多字段搜索     | 可以合并字段生成 `tsvector`         |

如果业务需要复杂中文分词、同义词、拼音、纠错、搜索建议和大规模检索，PostgreSQL 全文搜索可能不够，应考虑 Elasticsearch、OpenSearch 等专门搜索引擎。

### 索引创建策略

索引创建策略应从查询模式出发，而不是从字段数量出发。一个好索引应该服务于明确的 SQL，包括过滤条件、排序条件、关联条件、唯一约束或统计场景。

索引设计基本流程：

| 步骤       | 说明                                         |
| ---------- | -------------------------------------------- |
| 收集 SQL   | 找出高频查询、慢查询、核心接口 SQL           |
| 分析条件   | 确认 `WHERE`、`JOIN`、`ORDER BY`、`GROUP BY` |
| 判断选择性 | 高选择性字段更适合索引                       |
| 设计索引   | 单列、联合、表达式、部分索引按场景选择       |
| 验证计划   | 使用 `EXPLAIN` 或 `EXPLAIN ANALYZE`          |
| 观察效果   | 结合线上慢查询、索引使用统计和写入成本       |
| 定期清理   | 移除重复、低效、长期未使用索引               |

常见业务索引示例：

```sql
-- 用户名唯一查询
CREATE UNIQUE INDEX uk_sys_user_username
ON app.sys_user (username);

-- 用户手机号查询
CREATE INDEX idx_sys_user_phone
ON app.sys_user (phone);

-- 订单号唯一查询
CREATE UNIQUE INDEX uk_order_info_order_no
ON app.order_info (order_no);

-- 用户订单列表
CREATE INDEX idx_order_info_user_time
ON app.order_info (user_id, create_time DESC, id DESC);

-- 订单状态列表
CREATE INDEX idx_order_info_status_time
ON app.order_info (order_status, create_time DESC, id DESC);

-- 软删除用户查询
CREATE INDEX idx_sys_user_active_username
ON app.sys_user (username)
WHERE deleted = FALSE;
```

关联字段索引：

```sql
-- 订单按用户关联
CREATE INDEX idx_order_info_user_id
ON app.order_info (user_id);

-- 订单明细按订单关联
CREATE INDEX idx_order_item_order_id
ON app.order_item (order_id);

-- 用户角色关联
CREATE INDEX idx_sys_user_role_user_id
ON app.sys_user_role (user_id);

CREATE INDEX idx_sys_user_role_role_id
ON app.sys_user_role (role_id);
```

索引命名建议：

| 类型      | 命名示例               |
| --------- | ---------------------- |
| 普通索引  | `idx_表名_字段名`      |
| 唯一索引  | `uk_表名_字段名`       |
| 主键索引  | `pk_表名`              |
| GIN 索引  | `idx_表名_字段名_gin`  |
| GiST 索引 | `idx_表名_字段名_gist` |
| BRIN 索引 | `idx_表名_字段名_brin` |
| 部分索引  | `idx_表名_业务含义`    |

查看执行计划：

```sql
EXPLAIN
SELECT
    id,
    order_no,
    total_amount,
    create_time
FROM app.order_info
WHERE user_id = 10001
ORDER BY create_time DESC, id DESC
LIMIT 20;
```

查看真实执行耗时：

```sql
EXPLAIN ANALYZE
SELECT
    id,
    order_no,
    total_amount,
    create_time
FROM app.order_info
WHERE user_id = 10001
ORDER BY create_time DESC, id DESC
LIMIT 20;
```

索引创建建议：

| 建议                       | 说明                             |
| -------------------------- | -------------------------------- |
| 先看 SQL，再建索引         | 不要根据字段名猜索引             |
| 优先服务高频慢查询         | 核心接口优先                     |
| 联合索引覆盖过滤和排序     | 列表查询尤其重要                 |
| 低选择性字段不要单独建索引 | 如布尔字段单列索引通常收益有限   |
| 控制索引数量               | 索引会增加写入和存储成本         |
| 大表建索引考虑并发创建     | 使用 `CREATE INDEX CONCURRENTLY` |

大表生产环境创建索引建议使用并发方式：

```sql
CREATE INDEX CONCURRENTLY idx_order_info_user_time
ON app.order_info (user_id, create_time DESC, id DESC);
```

`CREATE INDEX CONCURRENTLY` 可以降低对写入的阻塞影响，但执行时间通常更长，并且不能在普通事务块中执行。

### 索引失效场景

索引失效通常不是索引真正“坏了”，而是优化器认为使用索引不划算，或者 SQL 写法导致索引无法匹配。索引是否使用应以 `EXPLAIN ANALYZE` 为准。

常见索引失效或不命中的场景如下：

| 场景                   | 示例                      | 优化建议                   |
| ---------------------- | ------------------------- | -------------------------- |
| 对字段使用函数         | `date(create_time) = ...` | 改成范围查询或表达式索引   |
| 类型不一致             | `id = '10001'`            | 保持参数类型和字段类型一致 |
| 前置通配符模糊查询     | `name LIKE '%abc'`        | 使用 `pg_trgm` 或全文搜索  |
| 低选择性字段           | `deleted = false`         | 考虑部分索引或联合索引     |
| OR 条件复杂            | `a = 1 OR b = 2`          | 拆分 SQL 或分别建合适索引  |
| 联合索引字段顺序不匹配 | 只查询非前导字段          | 调整索引顺序或补充索引     |
| 数据量太小             | 小表全表扫描更快          | 不必强制使用索引           |
| 返回数据比例太高       | 命中大量行                | 全表扫描可能更划算         |
| 统计信息过旧           | 优化器估算不准            | 执行 `ANALYZE`             |
| 隐式转换               | 字段和参数类型不同        | 避免隐式转换               |

函数导致普通索引难以使用：

```sql
-- 不推荐
SELECT
    id,
    order_no
FROM app.order_info
WHERE create_time::date = DATE '2026-05-09';
```

推荐改为时间范围：

```sql
SELECT
    id,
    order_no
FROM app.order_info
WHERE create_time >= TIMESTAMP '2026-05-09 00:00:00'
  AND create_time < TIMESTAMP '2026-05-10 00:00:00';
```

前置通配符导致普通 B-Tree 索引难以使用：

```sql
-- 普通 B-Tree 索引通常不适合这种包含匹配
SELECT
    id,
    username,
    nickname
FROM app.sys_user
WHERE nickname LIKE '%管理员%';
```

可以考虑 `pg_trgm`：

```sql
CREATE EXTENSION IF NOT EXISTS pg_trgm;

CREATE INDEX idx_sys_user_nickname_trgm
ON app.sys_user
USING GIN (nickname gin_trgm_ops);
```

对应查询：

```sql
SELECT
    id,
    username,
    nickname
FROM app.sys_user
WHERE nickname ILIKE '%管理员%';
```

联合索引不合理示例：

```sql
CREATE INDEX idx_order_info_status_user_time
ON app.order_info (order_status, user_id, create_time DESC);
```

如果主要查询是按 `user_id` 查订单列表：

```sql
SELECT
    id,
    order_no,
    create_time
FROM app.order_info
WHERE user_id = 10001
ORDER BY create_time DESC
LIMIT 20;
```

此时更合适的索引通常是：

```sql
CREATE INDEX idx_order_info_user_time
ON app.order_info (user_id, create_time DESC, id DESC);
```

索引失效排查建议：

| 步骤          | 命令或动作                        |
| ------------- | --------------------------------- |
| 查看执行计划  | `EXPLAIN`                         |
| 查看真实耗时  | `EXPLAIN ANALYZE`                 |
| 查看索引定义  | `\d+ app.table_name`              |
| 检查统计信息  | `ANALYZE app.table_name`          |
| 检查字段类型  | 对比字段类型和查询参数类型        |
| 检查返回比例  | 命中行过多时全表扫描可能更合理    |
| 检查 SQL 写法 | 函数、隐式转换、模糊匹配、OR 条件 |

### 索引维护

索引维护用于检查索引使用情况、清理无效或重复索引、重建膨胀索引、更新统计信息和控制索引数量。索引会提升读性能，但会增加写入、更新、删除和 VACUUM 成本，因此需要定期治理。

查看表上的索引：

```sql
SELECT
    schemaname,
    tablename,
    indexname,
    indexdef
FROM pg_indexes
WHERE schemaname = 'app'
  AND tablename = 'order_info'
ORDER BY indexname;
```

查看索引使用统计：

```sql
SELECT
    schemaname,
    relname AS table_name,
    indexrelname AS index_name,
    idx_scan,
    idx_tup_read,
    idx_tup_fetch
FROM pg_stat_user_indexes
WHERE schemaname = 'app'
ORDER BY idx_scan ASC, relname, indexrelname;
```

字段说明：

| 字段            | 说明                     |
| --------------- | ------------------------ |
| `idx_scan`      | 索引扫描次数             |
| `idx_tup_read`  | 从索引中读取的索引项数量 |
| `idx_tup_fetch` | 通过索引回表读取的行数量 |

查看表和索引大小：

```sql
SELECT
    schemaname,
    relname AS table_name,
    pg_size_pretty(pg_total_relation_size(relid)) AS total_size,
    pg_size_pretty(pg_relation_size(relid)) AS table_size,
    pg_size_pretty(pg_indexes_size(relid)) AS indexes_size
FROM pg_stat_user_tables
WHERE schemaname = 'app'
ORDER BY pg_total_relation_size(relid) DESC;
```

查看具体索引大小：

```sql
SELECT
    schemaname,
    tablename,
    indexname,
    pg_size_pretty(pg_relation_size(format('%I.%I', schemaname, indexname)::regclass)) AS index_size
FROM pg_indexes
WHERE schemaname = 'app'
ORDER BY pg_relation_size(format('%I.%I', schemaname, indexname)::regclass) DESC;
```

更新统计信息：

```sql
ANALYZE app.order_info;
```

对表执行 VACUUM 和 ANALYZE：

```sql
VACUUM ANALYZE app.order_info;
```

重建索引：

```sql
REINDEX INDEX app.idx_order_info_user_time;
```

并发重建索引：

```sql
REINDEX INDEX CONCURRENTLY app.idx_order_info_user_time;
```

删除无用索引：

```sql
DROP INDEX IF EXISTS app.idx_order_info_unused;
```

生产环境删除索引前，应至少确认以下内容：

| 检查项                 | 说明                                 |
| ---------------------- | ------------------------------------ |
| 是否长期未使用         | 查看 `pg_stat_user_indexes.idx_scan` |
| 是否支持约束           | 唯一约束、主键依赖的索引不能随意删除 |
| 是否服务低频但关键 SQL | 例如月末报表、结算任务               |
| 是否与其他索引重复     | 检查联合索引前缀和表达式是否重叠     |
| 是否有回滚方案         | 删除前记录索引 DDL                   |

索引维护建议：

| 建议                 | 说明                                                |
| -------------------- | --------------------------------------------------- |
| 定期查看慢查询       | 慢 SQL 是索引优化入口                               |
| 定期查看未使用索引   | 长期 `idx_scan = 0` 的索引需要评估                  |
| 控制重复索引         | 避免多个索引覆盖相同查询                            |
| 大表索引用并发操作   | `CREATE INDEX CONCURRENTLY`、`REINDEX CONCURRENTLY` |
| DDL 纳入版本管理     | 索引创建和删除应进入 Flyway 或 Liquibase            |
| 写多读少表少建索引   | 索引会拖慢写入                                      |
| 高并发表谨慎重建索引 | 注意锁、IO、WAL 和执行时间                          |

索引优化的最终标准不是“是否创建了索引”，而是核心 SQL 的执行计划、响应时间、资源消耗和写入成本是否达到预期。新增索引后应通过 `EXPLAIN ANALYZE` 验证效果，并在发布后继续观察慢查询和索引使用统计。


## 查询性能分析

查询性能分析用于定位慢 SQL、判断索引是否命中、识别错误的连接方式、发现扫描行数过多、排序溢出、聚合成本过高等问题。PostgreSQL 中最常用的分析工具是 `EXPLAIN`、`EXPLAIN ANALYZE`、慢查询日志、`pg_stat_statements` 和统计视图。

### EXPLAIN 使用

`EXPLAIN` 用于查看 SQL 的执行计划，但不会真正执行查询语句。它适合在执行前了解优化器准备采用的扫描方式、连接方式、排序方式和成本估算。

基础用法：

```sql
EXPLAIN
SELECT
    id,
    order_no,
    user_id,
    total_amount,
    create_time
FROM app.order_info
WHERE user_id = 10001
ORDER BY create_time DESC, id DESC
LIMIT 20;
```

常用输出格式：

```sql
EXPLAIN (FORMAT TEXT)
SELECT
    id,
    order_no
FROM app.order_info
WHERE user_id = 10001;
```

JSON 格式输出，适合程序分析或保存执行计划：

```sql
EXPLAIN (FORMAT JSON)
SELECT
    id,
    order_no
FROM app.order_info
WHERE user_id = 10001;
```

带更多计划信息：

```sql
EXPLAIN (
    COSTS TRUE,
    VERBOSE TRUE,
    SETTINGS TRUE,
    FORMAT TEXT
)
SELECT
    id,
    order_no,
    total_amount
FROM app.order_info
WHERE order_status = 'PAID'
ORDER BY create_time DESC
LIMIT 20;
```

`EXPLAIN` 常见字段说明如下：

| 字段               | 说明                                          |
| ------------------ | --------------------------------------------- |
| `cost`             | 优化器估算成本，格式通常为 `启动成本..总成本` |
| `rows`             | 优化器估算返回行数                            |
| `width`            | 优化器估算每行平均宽度，单位为字节            |
| `Seq Scan`         | 顺序扫描                                      |
| `Index Scan`       | 索引扫描                                      |
| `Bitmap Heap Scan` | 位图堆扫描                                    |
| `Nested Loop`      | 嵌套循环连接                                  |
| `Hash Join`        | 哈希连接                                      |
| `Merge Join`       | 合并连接                                      |
| `Sort`             | 排序节点                                      |
| `Aggregate`        | 聚合节点                                      |

`EXPLAIN` 不会执行 SQL，因此不会显示真实耗时和真实行数。如果需要确认实际执行情况，应使用 `EXPLAIN ANALYZE`。

### EXPLAIN ANALYZE 使用

`EXPLAIN ANALYZE` 会真正执行 SQL，并返回实际执行时间、实际扫描行数和计划估算值。它是定位慢查询最重要的工具之一。

基础用法：

```sql
EXPLAIN ANALYZE
SELECT
    id,
    order_no,
    user_id,
    total_amount,
    create_time
FROM app.order_info
WHERE user_id = 10001
ORDER BY create_time DESC, id DESC
LIMIT 20;
```

推荐使用更完整的分析选项：

```sql
EXPLAIN (
    ANALYZE TRUE,
    BUFFERS TRUE,
    VERBOSE TRUE,
    TIMING TRUE,
    SUMMARY TRUE
)
SELECT
    id,
    order_no,
    user_id,
    total_amount,
    create_time
FROM app.order_info
WHERE user_id = 10001
ORDER BY create_time DESC, id DESC
LIMIT 20;
```

常用分析选项如下：

| 选项          | 说明                               |
| ------------- | ---------------------------------- |
| `ANALYZE`     | 实际执行 SQL，并输出真实执行时间   |
| `BUFFERS`     | 输出缓冲区命中和读取情况           |
| `VERBOSE`     | 输出更详细的字段和对象信息         |
| `TIMING`      | 输出节点耗时                       |
| `SUMMARY`     | 输出总执行时间、规划时间等汇总信息 |
| `FORMAT JSON` | 使用 JSON 格式输出执行计划         |

查看写操作执行计划时要谨慎，因为 `EXPLAIN ANALYZE` 会真正执行 SQL。对 `UPDATE`、`DELETE`、`INSERT` 分析时建议放在事务中，并在分析后回滚。

```sql
BEGIN;

EXPLAIN ANALYZE
UPDATE app.order_info
SET
    order_status = 'CANCELLED'
WHERE order_status = 'PENDING'
  AND create_time < CURRENT_TIMESTAMP - INTERVAL '30 days';

ROLLBACK;
```

`EXPLAIN ANALYZE` 关注重点如下：

| 关注项                 | 说明                                                         |
| ---------------------- | ------------------------------------------------------------ |
| 实际耗时最高的节点     | 通常是优化入口                                               |
| 实际行数和估算行数差距 | 差距大可能说明统计信息不准                                   |
| 是否全表扫描           | 大表上出现 `Seq Scan` 需要重点关注                           |
| 是否大量排序           | `Sort` 节点可能消耗内存或落盘                                |
| 是否命中索引           | 检查是否出现 `Index Scan`、`Index Only Scan`、`Bitmap Index Scan` |
| 缓冲区读取情况         | `shared read` 较多说明磁盘读取压力大                         |
| 循环次数               | `loops` 很大时要关注嵌套循环成本                             |

### 执行计划阅读

阅读执行计划时，应从最内层节点开始看，再向外层理解数据如何流动。PostgreSQL 执行计划是树形结构，缩进越深的节点越先执行。

示例：

```sql
EXPLAIN ANALYZE
SELECT
    o.id,
    o.order_no,
    u.username
FROM app.order_info o
INNER JOIN app.sys_user u ON u.id = o.user_id
WHERE o.user_id = 10001
ORDER BY o.create_time DESC
LIMIT 20;
```

可能出现的计划结构：

```text
Limit
  -> Sort
       Sort Key: o.create_time DESC
       -> Nested Loop
            -> Index Scan using idx_order_info_user_id on order_info o
            -> Index Scan using sys_user_pkey on sys_user u
```

阅读顺序可以理解为：

1. 先通过 `idx_order_info_user_id` 查询订单表；
2. 再通过用户主键索引查询用户表；
3. 对结果按订单创建时间排序；
4. 最后取前 20 条。

执行计划中常见字段：

| 字段                   | 示例           | 说明                   |
| ---------------------- | -------------- | ---------------------- |
| `actual time`          | `0.032..1.245` | 节点真实开始和结束时间 |
| `rows`                 | `20`           | 节点真实输出行数       |
| `loops`                | `1`            | 节点执行次数           |
| `Planning Time`        | `0.321 ms`     | 生成执行计划耗时       |
| `Execution Time`       | `1.837 ms`     | SQL 实际执行总耗时     |
| `Buffers: shared hit`  | `128`          | 从共享缓冲区命中的页数 |
| `Buffers: shared read` | `32`           | 从磁盘读取的页数       |

重点判断项：

| 判断项                  | 说明                                            |
| ----------------------- | ----------------------------------------------- |
| 估算行数是否准确        | `rows` 估算和实际差距过大时，可能需要 `ANALYZE` |
| 是否出现大表 `Seq Scan` | 大表顺序扫描可能导致慢查询                      |
| 是否出现大排序          | 大量 `Sort` 可能需要索引支持排序                |
| 是否连接顺序异常        | 连接大表前是否已过滤掉足够数据                  |
| 是否循环次数过高        | `Nested Loop` 内层循环过多可能很慢              |
| 是否回表过多            | `Index Scan` 后回表成本可能较高                 |
| 是否读取磁盘过多        | `shared read` 较高可能受 IO 影响                |

执行计划分析不是只看有没有索引，而是看整个查询的数据流是否合理。

### 顺序扫描

顺序扫描对应执行计划中的 `Seq Scan`。它表示 PostgreSQL 逐行扫描整张表。对于小表、返回大量数据的查询、没有合适索引的查询，顺序扫描可能是合理的。

示例：

```sql
EXPLAIN ANALYZE
SELECT
    id,
    order_no,
    total_amount
FROM app.order_info
WHERE order_status = 'PAID';
```

如果表很小，或 `order_status = 'PAID'` 命中了表中大部分数据，优化器可能选择 `Seq Scan`。

顺序扫描常见原因：

| 原因               | 说明                                     |
| ------------------ | ---------------------------------------- |
| 表数据量很小       | 全表扫描比使用索引更快                   |
| 返回比例很高       | 命中大量行时索引扫描成本反而更高         |
| 没有合适索引       | 查询条件字段没有索引                     |
| 统计信息过旧       | 优化器估算不准                           |
| 条件写法不适合索引 | 对字段使用函数、隐式转换、前置模糊匹配等 |
| 查询需要全表聚合   | 如无条件 `COUNT(*)`、全表统计            |

优化示例：为高频过滤字段创建索引。

```sql
CREATE INDEX idx_order_info_user_time
ON app.order_info (user_id, create_time DESC, id DESC);
```

对应查询：

```sql
EXPLAIN ANALYZE
SELECT
    id,
    order_no,
    total_amount,
    create_time
FROM app.order_info
WHERE user_id = 10001
ORDER BY create_time DESC, id DESC
LIMIT 20;
```

需要注意，出现 `Seq Scan` 不一定就是问题。只有当表很大、过滤条件明确、返回行数较少、执行耗时较高时，才需要重点优化。

### 索引扫描

索引扫描用于通过索引定位数据。常见计划节点包括 `Index Scan`、`Index Only Scan` 和 `Bitmap Index Scan`。

普通索引扫描示例：

```sql
EXPLAIN ANALYZE
SELECT
    id,
    username,
    nickname
FROM app.sys_user
WHERE username = 'admin';
```

如果存在索引：

```sql
CREATE INDEX idx_sys_user_username
ON app.sys_user (username);
```

执行计划中可能出现：

```text
Index Scan using idx_sys_user_username on sys_user
```

`Index Scan` 表示先查索引，再根据索引指向的位置回表读取数据。

`Index Only Scan` 表示只通过索引就能返回需要的数据，不需要回表。它通常要求查询字段都能从索引中获得，并且可见性信息允许跳过回表。

覆盖索引示例：

```sql
CREATE INDEX idx_order_info_user_time_cover
ON app.order_info (user_id, create_time DESC, id DESC)
INCLUDE (order_no, total_amount, order_status);
```

适合查询：

```sql
EXPLAIN ANALYZE
SELECT
    order_no,
    total_amount,
    order_status,
    create_time
FROM app.order_info
WHERE user_id = 10001
ORDER BY create_time DESC, id DESC
LIMIT 20;
```

索引扫描常见类型：

| 类型                | 说明                               |
| ------------------- | ---------------------------------- |
| `Index Scan`        | 使用索引定位，再回表读取行         |
| `Index Only Scan`   | 查询结果可由索引提供，尽量减少回表 |
| `Bitmap Index Scan` | 先扫描索引生成位图，再访问表数据   |
| `Bitmap Heap Scan`  | 根据位图访问数据页                 |

索引扫描优化建议：

| 建议                       | 说明                                      |
| -------------------------- | ----------------------------------------- |
| 高频等值查询建索引         | 用户名、订单号、业务编码                  |
| 列表查询考虑排序字段       | 如 `(user_id, create_time DESC, id DESC)` |
| 返回字段较少可考虑覆盖索引 | 使用 `INCLUDE`                            |
| 不要滥用覆盖索引           | 索引体积和写入成本会增加                  |
| 用执行计划确认效果         | 不要只看索引是否存在                      |

### 位图扫描

位图扫描通常由 `Bitmap Index Scan` 和 `Bitmap Heap Scan` 组成。它适合查询命中多行数据但又不是全表大部分数据的场景。PostgreSQL 会先通过索引生成匹配行的位置位图，再批量访问表数据页。

示例：

```sql
EXPLAIN ANALYZE
SELECT
    id,
    order_no,
    user_id,
    order_status,
    create_time
FROM app.order_info
WHERE order_status = 'PAID'
  AND create_time >= TIMESTAMP '2026-05-01 00:00:00';
```

可能的执行计划：

```text
Bitmap Heap Scan on order_info
  Recheck Cond: ...
  -> Bitmap Index Scan on idx_order_info_status_time
```

位图扫描适合以下场景：

| 场景                 | 说明                            |
| -------------------- | ------------------------------- |
| 条件命中中等数量数据 | 比单条回表更适合批量访问        |
| 多个索引条件组合     | 可通过 BitmapAnd、BitmapOr 合并 |
| 返回数据较多         | 比普通 Index Scan 更适合        |
| 数据分布较分散       | 位图可以减少随机访问成本        |

多索引组合示例：

```sql
CREATE INDEX idx_order_info_status
ON app.order_info (order_status);

CREATE INDEX idx_order_info_create_time
ON app.order_info (create_time);
```

查询：

```sql
EXPLAIN ANALYZE
SELECT
    id,
    order_no
FROM app.order_info
WHERE order_status = 'PAID'
  AND create_time >= TIMESTAMP '2026-05-01 00:00:00';
```

执行计划中可能出现 `BitmapAnd`，表示组合多个索引结果。

位图扫描不一定需要优化。如果执行时间较短、读取数据量合理，属于正常计划。如果位图扫描读取大量数据页，可能需要更合适的联合索引。

### 嵌套循环连接

嵌套循环连接对应 `Nested Loop`。它的逻辑是外层表每返回一行，就去内层表查一次匹配数据。对于外层结果集很小、内层有高效索引的场景，嵌套循环连接性能很好。

示例：

```sql
EXPLAIN ANALYZE
SELECT
    o.id,
    o.order_no,
    u.username
FROM app.order_info o
INNER JOIN app.sys_user u ON u.id = o.user_id
WHERE o.id = 1;
```

如果外层订单只有一行，内层通过用户主键索引查询，`Nested Loop` 是合理的。

适合嵌套循环的场景：

| 场景               | 说明                 |
| ------------------ | -------------------- |
| 外层结果集很小     | 例如按主键查询       |
| 内层连接字段有索引 | 每次内层查找成本低   |
| LIMIT 小结果集     | 只需要少量结果       |
| 高选择性条件       | 外层过滤后剩余行很少 |

嵌套循环可能变慢的场景：

```sql
EXPLAIN ANALYZE
SELECT
    o.id,
    o.order_no,
    u.username
FROM app.order_info o
INNER JOIN app.sys_user u ON u.id = o.user_id
WHERE o.create_time >= TIMESTAMP '2026-01-01 00:00:00';
```

如果订单表命中几十万行，且内层连接需要循环几十万次，可能造成性能问题。

优化方向：

| 问题           | 优化方式                     |
| -------------- | ---------------------------- |
| 外层行数过多   | 增加过滤条件，先分页或先聚合 |
| 内层无索引     | 为连接字段创建索引           |
| 连接顺序不合理 | 检查统计信息，执行 `ANALYZE` |
| 返回结果过大   | 限制时间范围或分页           |
| 重复查询明细   | 先预聚合子表，再连接         |

连接字段索引示例：

```sql
CREATE INDEX idx_order_info_user_id
ON app.order_info (user_id);

CREATE INDEX idx_sys_user_id
ON app.sys_user (id);
```

### 哈希连接

哈希连接对应 `Hash Join`。它通常用于连接两张较大的表，优化器会选择一张表构建哈希表，再扫描另一张表进行匹配。哈希连接适合等值连接。

示例：

```sql
EXPLAIN ANALYZE
SELECT
    o.id,
    o.order_no,
    u.username
FROM app.order_info o
INNER JOIN app.sys_user u ON u.id = o.user_id
WHERE o.create_time >= TIMESTAMP '2026-01-01 00:00:00';
```

可能的执行计划：

```text
Hash Join
  Hash Cond: (o.user_id = u.id)
  -> Seq Scan on order_info o
  -> Hash
       -> Seq Scan on sys_user u
```

哈希连接适合以下场景：

| 场景           | 说明                         |
| -------------- | ---------------------------- |
| 等值连接       | 如 `o.user_id = u.id`        |
| 两边数据量较大 | 比嵌套循环更适合             |
| 没有合适排序   | 不适合合并连接时可用哈希连接 |
| 连接结果较多   | 使用哈希匹配降低重复查找成本 |

哈希连接关注点：

| 关注项     | 说明                                |
| ---------- | ----------------------------------- |
| 哈希表大小 | 构建侧数据过大可能占用内存          |
| 是否分批   | 如果内存不足，可能分批处理并增加 IO |
| `work_mem` | 排序和哈希操作会受该参数影响        |
| 连接条件   | 哈希连接主要适合等值连接            |
| 过滤时机   | 应尽量先过滤再参与哈希连接          |

优化方向：

```sql
-- 为过滤字段创建索引，减少进入连接的数据量
CREATE INDEX idx_order_info_create_time
ON app.order_info (create_time);
```

如果哈希连接本身耗时高，应先检查构建哈希表的数据量是否过大，过滤条件是否可以提前下推，以及统计信息是否准确。

### 合并连接

合并连接对应 `Merge Join`。它要求连接两侧数据按连接字段有序，然后像归并排序一样逐步匹配。它适合已经有序的数据、大数据量连接、范围较大的等值连接等场景。

示例：

```sql
EXPLAIN ANALYZE
SELECT
    o.id,
    o.order_no,
    u.username
FROM app.order_info o
INNER JOIN app.sys_user u ON u.id = o.user_id
ORDER BY o.user_id;
```

可能的执行计划：

```text
Merge Join
  Merge Cond: (o.user_id = u.id)
  -> Index Scan using idx_order_info_user_id on order_info o
  -> Index Scan using sys_user_pkey on sys_user u
```

合并连接适合以下场景：

| 场景                 | 说明             |
| -------------------- | ---------------- |
| 两边已按连接字段排序 | 可直接合并       |
| 连接字段有索引       | 索引扫描天然有序 |
| 大结果集连接         | 比嵌套循环更稳定 |
| 需要排序输出         | 排序成本可复用   |

合并连接可能出现额外排序：

```text
Merge Join
  Merge Cond: ...
  -> Sort
  -> Sort
```

如果两侧都需要先排序，成本可能较高。可以通过连接字段索引减少排序成本。

```sql
CREATE INDEX idx_order_info_user_id
ON app.order_info (user_id);
```

连接方式对比：

| 连接方式               | 适合场景                       |
| ---------------------- | ------------------------------ |
| `Nested Loop`          | 外层少量行，内层有索引         |
| `Hash Join`            | 大量等值连接，无需有序         |
| `Merge Join`           | 两侧有序或需要排序输出         |
| `Nested Loop` 慢的原因 | 外层行数太多，内层重复扫描     |
| `Hash Join` 慢的原因   | 哈希表过大，内存不足，过滤太晚 |
| `Merge Join` 慢的原因  | 排序成本高，索引不匹配         |

### 慢查询定位

慢查询定位需要结合数据库日志、`pg_stat_statements`、当前活动会话、锁等待和执行计划综合分析。不要只看单次 SQL 耗时，也要看调用频率、平均耗时、总耗时和资源消耗。

开启慢查询日志的常用配置：

```conf
# 记录执行时间超过 1 秒的 SQL
log_min_duration_statement = 1000

# 记录锁等待
log_lock_waits = on

# 锁等待超过该时间后记录日志
deadlock_timeout = '1s'

# 日志中记录连接、用户、数据库、应用名和客户端地址
log_line_prefix = '%m [%p] user=%u db=%d app=%a client=%h '
```

修改配置后重载：

```sql
SELECT pg_reload_conf();
```

查看当前正在执行的 SQL：

```sql
SELECT
    pid,
    usename,
    datname,
    application_name,
    client_addr,
    state,
    now() - query_start AS running_time,
    wait_event_type,
    wait_event,
    query
FROM pg_stat_activity
WHERE state <> 'idle'
ORDER BY running_time DESC;
```

查看长时间运行 SQL：

```sql
SELECT
    pid,
    usename,
    datname,
    application_name,
    now() - query_start AS running_time,
    query
FROM pg_stat_activity
WHERE state = 'active'
  AND now() - query_start > INTERVAL '30 seconds'
ORDER BY running_time DESC;
```

启用 `pg_stat_statements` 扩展：

```sql
CREATE EXTENSION IF NOT EXISTS pg_stat_statements;
```

查询累计耗时最高的 SQL：

```sql
SELECT
    query,
    calls,
    total_exec_time,
    mean_exec_time,
    max_exec_time,
    rows
FROM pg_stat_statements
ORDER BY total_exec_time DESC
LIMIT 20;
```

查询平均耗时最高的 SQL：

```sql
SELECT
    query,
    calls,
    mean_exec_time,
    max_exec_time,
    rows
FROM pg_stat_statements
WHERE calls > 10
ORDER BY mean_exec_time DESC
LIMIT 20;
```

慢查询定位流程：

| 步骤           | 说明                            |
| -------------- | ------------------------------- |
| 查看慢查询日志 | 找到具体 SQL、耗时、发生时间    |
| 查看调用频率   | 高频 SQL 的总成本可能更高       |
| 查看执行计划   | 使用 `EXPLAIN ANALYZE`          |
| 检查扫描方式   | 是否大表顺序扫描                |
| 检查索引       | 是否缺失、冗余或未命中          |
| 检查行数估算   | 估算和实际差距是否过大          |
| 检查锁等待     | 慢可能不是 SQL 本身，而是等待锁 |
| 检查返回数据量 | 是否返回过多行或字段            |
| 检查应用调用   | 是否循环调用、N+1 查询、未分页  |

慢查询不一定都是缺索引。常见原因还包括锁等待、网络传输大量数据、应用层循环查询、排序落盘、统计信息过旧、连接池耗尽等。

### SQL 优化思路

SQL 优化应先保证语义正确，再分析数据量、索引、执行计划和业务调用方式。不要在未分析执行计划的情况下盲目加索引或重写 SQL。

常见优化方向如下：

| 问题             | 优化方式                       |
| ---------------- | ------------------------------ |
| 查询无分页       | 增加 `LIMIT` 和稳定排序        |
| 大表顺序扫描     | 为过滤条件或排序条件设计索引   |
| 函数导致索引失效 | 改为范围查询或表达式索引       |
| JOIN 结果膨胀    | 先聚合或先分页，再关联         |
| 子查询重复执行   | 改写为 JOIN、CTE 或预聚合      |
| OR 条件复杂      | 拆成 `UNION ALL` 或分别建索引  |
| 排序耗时高       | 创建匹配排序的联合索引         |
| 聚合数据量大     | 先过滤、分区、汇总表或物化视图 |
| 返回字段太多     | 明确字段列表，避免 `SELECT *`  |
| N+1 查询         | 批量查询或一次 JOIN 查询       |
| 统计信息不准     | 执行 `ANALYZE`                 |

示例：避免函数包裹时间字段。

不推荐：

```sql
SELECT
    id,
    order_no
FROM app.order_info
WHERE create_time::date = DATE '2026-05-09';
```

推荐：

```sql
SELECT
    id,
    order_no
FROM app.order_info
WHERE create_time >= TIMESTAMP '2026-05-09 00:00:00'
  AND create_time < TIMESTAMP '2026-05-10 00:00:00';
```

示例：先分页主表，再关联明细。

```sql
WITH page_order AS (
    SELECT
        id,
        order_no,
        user_id,
        total_amount,
        create_time
    FROM app.order_info
    WHERE order_status = 'PAID'
    ORDER BY create_time DESC, id DESC
    LIMIT 20 OFFSET 0
)
SELECT
    o.id,
    o.order_no,
    o.total_amount,
    u.username
FROM page_order o
INNER JOIN app.sys_user u ON u.id = o.user_id
ORDER BY o.create_time DESC, o.id DESC;
```

示例：先预聚合明细，再连接主表。

```sql
WITH item_stat AS (
    SELECT
        order_id,
        COUNT(*) AS item_count,
        SUM(quantity * sale_price) AS item_amount
    FROM app.order_item
    GROUP BY order_id
)
SELECT
    o.id,
    o.order_no,
    o.total_amount,
    COALESCE(s.item_count, 0) AS item_count,
    COALESCE(s.item_amount, 0) AS item_amount
FROM app.order_info o
LEFT JOIN item_stat s ON s.order_id = o.id
WHERE o.order_status = 'PAID';
```

SQL 优化建议：

| 建议                | 说明                                    |
| ------------------- | --------------------------------------- |
| 先看执行计划        | 不凭感觉优化                            |
| 小步验证            | 每次只改一个主要因素                    |
| 关注实际耗时        | `EXPLAIN ANALYZE` 比纯 `EXPLAIN` 更可靠 |
| 关注业务调用次数    | 高频短 SQL 总成本可能很高               |
| 控制结果集大小      | 分页、字段裁剪、条件过滤                |
| 避免过度索引        | 索引会拖慢写入                          |
| 建立慢 SQL 治理机制 | 日志、监控、统计视图、定期复盘          |

## 事务管理

事务用于保证一组数据库操作要么全部成功，要么全部失败。PostgreSQL 支持标准事务语法、保存点、事务隔离级别和 MVCC 机制。项目开发中应合理控制事务边界，避免长事务、锁等待、死锁和连接池资源占用。

### 事务基本语法

事务由 `BEGIN` 开始，通过 `COMMIT` 提交，或通过 `ROLLBACK` 回滚。事务中的多条 SQL 会作为一个整体执行。

基础事务示例：

```sql
BEGIN;

UPDATE app.account_balance
SET
    balance = balance - 100.00,
    update_time = CURRENT_TIMESTAMP
WHERE user_id = 10001
  AND balance >= 100.00;

UPDATE app.account_balance
SET
    balance = balance + 100.00,
    update_time = CURRENT_TIMESTAMP
WHERE user_id = 10002;

INSERT INTO app.account_record (
    user_id,
    amount,
    record_type,
    create_time
) VALUES
    (10001, -100.00, 'TRANSFER_OUT', CURRENT_TIMESTAMP),
    (10002, 100.00, 'TRANSFER_IN', CURRENT_TIMESTAMP);

COMMIT;
```

如果中间任何一步失败，应回滚整个事务：

```sql
BEGIN;

UPDATE app.account_balance
SET balance = balance - 100.00
WHERE user_id = 10001
  AND balance >= 100.00;

-- 如果业务检查失败或 SQL 出错
ROLLBACK;
```

事务常见操作：

| 命令                              | 说明               |
| --------------------------------- | ------------------ |
| `BEGIN`                           | 开启事务           |
| `START TRANSACTION`               | 开启事务，等价写法 |
| `COMMIT`                          | 提交事务           |
| `ROLLBACK`                        | 回滚事务           |
| `SAVEPOINT`                       | 创建保存点         |
| `ROLLBACK TO SAVEPOINT`           | 回滚到保存点       |
| `RELEASE SAVEPOINT`               | 释放保存点         |
| `SET TRANSACTION ISOLATION LEVEL` | 设置事务隔离级别   |

事务边界应尽量放在业务服务层，而不是单条 DAO 或 Mapper 层。对于 Spring Boot 项目，通常使用 `@Transactional` 管理事务边界。

### COMMIT 提交

`COMMIT` 用于提交当前事务，使事务中的所有变更永久生效。提交后，其他事务可以看到这些已提交的数据，具体可见性仍受隔离级别影响。

提交示例：

```sql
BEGIN;

INSERT INTO app.sys_config (
    config_key,
    config_value,
    remark
) VALUES (
    'system.name',
    'PostgreSQL 管理系统',
    '系统名称'
);

COMMIT;
```

提交后查询：

```sql
SELECT
    id,
    config_key,
    config_value
FROM app.sys_config
WHERE config_key = 'system.name';
```

提交注意事项：

| 注意点             | 说明                            |
| ------------------ | ------------------------------- |
| 提交后不能普通回滚 | 已提交数据需要通过反向 SQL 修复 |
| 事务越长风险越高   | 长事务会持有锁和旧版本数据      |
| 提交前确认影响行数 | 尤其是批量更新、删除            |
| 失败后不能继续提交 | 事务进入错误状态时应回滚        |
| DDL 通常也在事务中 | PostgreSQL 中大多数 DDL 可回滚  |

批量修复数据时，建议先查询影响范围，再事务内执行，并在提交前再次确认。

```sql
BEGIN;

SELECT COUNT(*)
FROM app.sys_user
WHERE enabled = FALSE
  AND deleted = FALSE;

UPDATE app.sys_user
SET
    enabled = TRUE,
    update_time = CURRENT_TIMESTAMP
WHERE enabled = FALSE
  AND deleted = FALSE;

-- 确认影响行数符合预期后提交
COMMIT;
```

### ROLLBACK 回滚

`ROLLBACK` 用于撤销当前事务中尚未提交的所有变更。它常用于异常处理、测试 SQL、数据修复前验证和业务失败回退。

回滚插入：

```sql
BEGIN;

INSERT INTO app.sys_config (
    config_key,
    config_value
) VALUES (
    'test.rollback',
    'rollback-value'
);

ROLLBACK;
```

回滚后，上面的数据不会保留。

使用事务测试危险 SQL：

```sql
BEGIN;

DELETE FROM app.sys_user
WHERE deleted = TRUE;

-- 查看将被删除的影响范围或结果
SELECT COUNT(*)
FROM app.sys_user
WHERE deleted = TRUE;

-- 不提交，直接回滚
ROLLBACK;
```

事务中如果 SQL 出错，当前事务通常会进入失败状态，后续 SQL 会被拒绝，直到执行 `ROLLBACK`。

```sql
BEGIN;

-- 假设这里发生错误
INSERT INTO app.sys_user (username)
VALUES (NULL);

-- 当前事务已异常，后续 SQL 通常无法正常执行
SELECT 1;

ROLLBACK;
```

回滚建议如下：

| 场景           | 建议                                                  |
| -------------- | ----------------------------------------------------- |
| 数据修复前验证 | 先 `BEGIN`，执行后确认，再决定 `COMMIT` 或 `ROLLBACK` |
| SQL 执行异常   | 立即 `ROLLBACK`，不要继续复用该事务                   |
| 应用异常       | 由事务管理器自动回滚                                  |
| 批量任务失败   | 结合保存点或分批事务控制影响范围                      |
| 生产操作       | 高危操作建议先在事务中演练                            |

### SAVEPOINT 保存点

`SAVEPOINT` 用于在事务内部创建保存点。出现局部错误时，可以回滚到保存点，而不是回滚整个事务。它适合批量处理、局部容错和复杂脚本。

基础用法：

```sql
BEGIN;

INSERT INTO app.sys_config (
    config_key,
    config_value
) VALUES (
    'config.a',
    'value-a'
);

SAVEPOINT sp_config_b;

INSERT INTO app.sys_config (
    config_key,
    config_value
) VALUES (
    'config.b',
    'value-b'
);

-- 回滚到保存点，只撤销保存点之后的变更
ROLLBACK TO SAVEPOINT sp_config_b;

INSERT INTO app.sys_config (
    config_key,
    config_value
) VALUES (
    'config.c',
    'value-c'
);

COMMIT;
```

释放保存点：

```sql
BEGIN;

SAVEPOINT sp_update_user;

UPDATE app.sys_user
SET
    nickname = '管理员',
    update_time = CURRENT_TIMESTAMP
WHERE username = 'admin';

RELEASE SAVEPOINT sp_update_user;

COMMIT;
```

保存点适合批量导入时跳过局部错误：

```sql
BEGIN;

SAVEPOINT sp_item_1;

INSERT INTO app.sys_config (config_key, config_value)
VALUES ('batch.item.1', 'value-1');

RELEASE SAVEPOINT sp_item_1;

SAVEPOINT sp_item_2;

INSERT INTO app.sys_config (config_key, config_value)
VALUES ('batch.item.2', 'value-2');

-- 如果第二条失败，可以回滚到该保存点
ROLLBACK TO SAVEPOINT sp_item_2;

COMMIT;
```

保存点使用建议：

| 建议           | 说明                          |
| -------------- | ----------------------------- |
| 适合局部容错   | 不需要回滚整个事务            |
| 不要过度使用   | 保存点过多会增加复杂度        |
| 批处理可使用   | 单条失败不影响整个批次        |
| 应用层谨慎嵌套 | Spring 嵌套事务要明确传播行为 |
| 错误后及时处理 | 回滚到保存点后再继续执行      |

### 事务隔离级别

事务隔离级别用于控制并发事务之间能看到哪些数据，以及避免哪些并发异常。PostgreSQL 支持 `READ COMMITTED`、`REPEATABLE READ` 和 `SERIALIZABLE`。`READ UNCOMMITTED` 在 PostgreSQL 中会按 `READ COMMITTED` 处理。

查看当前隔离级别：

```sql
SHOW transaction_isolation;
```

设置当前事务隔离级别：

```sql
BEGIN;

SET TRANSACTION ISOLATION LEVEL REPEATABLE READ;

SELECT
    id,
    order_no,
    total_amount
FROM app.order_info
WHERE user_id = 10001;

COMMIT;
```

开启事务时指定隔离级别：

```sql
BEGIN ISOLATION LEVEL SERIALIZABLE;

SELECT
    id,
    order_no
FROM app.order_info
WHERE order_status = 'PENDING';

COMMIT;
```

隔离级别对比：

| 隔离级别          | 说明                               | 适用场景                 |
| ----------------- | ---------------------------------- | ------------------------ |
| `READ COMMITTED`  | 每条语句看到执行开始前已提交的数据 | 默认级别，适合大多数业务 |
| `REPEATABLE READ` | 同一事务内多次查询看到一致快照     | 需要事务内一致读         |
| `SERIALIZABLE`    | 提供最强隔离，效果接近串行执行     | 严格一致性要求高的场景   |

并发异常说明：

| 异常       | 说明                                   |
| ---------- | -------------------------------------- |
| 脏读       | 读取到其他事务未提交数据               |
| 不可重复读 | 同一事务内两次读取同一行结果不同       |
| 幻读       | 同一事务内两次范围查询出现不同记录集合 |
| 序列化异常 | 串行化隔离下并发冲突，需要重试事务     |

隔离级别越高，并发冲突和重试成本可能越高。普通业务系统不要盲目使用最高隔离级别，应优先通过合理 SQL 条件、行锁、唯一约束和乐观锁解决并发问题。

### 读已提交

`READ COMMITTED` 是 PostgreSQL 默认隔离级别。它的特点是每条 SQL 语句只能看到该语句开始前已经提交的数据。同一个事务中，不同语句可能看到不同的已提交结果。

设置读已提交：

```sql
BEGIN ISOLATION LEVEL READ COMMITTED;

SELECT
    id,
    order_no,
    order_status
FROM app.order_info
WHERE id = 1;

COMMIT;
```

典型行为：

```sql
BEGIN;

-- 第一次查询，看到当前已提交数据
SELECT
    id,
    order_status
FROM app.order_info
WHERE id = 1;

-- 如果其他事务提交了对该订单的修改
-- 当前事务下一条语句可能看到新值

SELECT
    id,
    order_status
FROM app.order_info
WHERE id = 1;

COMMIT;
```

适合场景：

| 场景         | 说明                 |
| ------------ | -------------------- |
| 普通增删改查 | 大多数业务接口       |
| 后台管理系统 | 列表、详情、编辑     |
| 状态更新     | 配合条件更新或乐观锁 |
| 短事务       | 默认推荐             |

状态流转应使用条件更新保证并发安全：

```sql
UPDATE app.order_info
SET
    order_status = 'PAID',
    update_time = CURRENT_TIMESTAMP
WHERE id = 1
  AND order_status = 'PENDING';
```

如果影响行数为 `0`，说明订单不存在或状态已被其他事务修改。应用层应根据影响行数判断是否成功。

### 可重复读

`REPEATABLE READ` 保证同一事务内多次读取看到一致的数据快照。它适合需要事务内一致读的场景，例如生成报表、复杂校验、多次查询必须基于同一时间点的数据等。

基础示例：

```sql
BEGIN ISOLATION LEVEL REPEATABLE READ;

SELECT
    COUNT(*) AS order_count
FROM app.order_info
WHERE order_status = 'PAID';

-- 即使其他事务提交了新订单，当前事务再次查询仍看到同一快照
SELECT
    COUNT(*) AS order_count
FROM app.order_info
WHERE order_status = 'PAID';

COMMIT;
```

适合场景：

| 场景         | 说明                             |
| ------------ | -------------------------------- |
| 一致性报表   | 同一事务内多次统计应基于同一快照 |
| 数据校验     | 多次查询之间不希望结果变化       |
| 批处理读取   | 读取期间保持一致视图             |
| 复杂业务判断 | 多个查询之间需要稳定结果         |

需要注意，`REPEATABLE READ` 不能替代所有并发控制。对于写冲突、库存扣减、状态流转等场景，仍应使用条件更新、行锁、唯一约束或重试机制。

库存扣减示例：

```sql
BEGIN ISOLATION LEVEL REPEATABLE READ;

UPDATE app.product_stock
SET
    stock_count = stock_count - 1,
    update_time = CURRENT_TIMESTAMP
WHERE product_id = 10001
  AND stock_count >= 1;

COMMIT;
```

应用层需要检查影响行数，如果为 `0`，说明库存不足或更新失败。

### 串行化

`SERIALIZABLE` 是 PostgreSQL 中最强的事务隔离级别，执行效果接近事务串行执行。它适合对一致性要求极高且可以接受事务重试的场景。

基础示例：

```sql
BEGIN ISOLATION LEVEL SERIALIZABLE;

SELECT
    SUM(total_amount) AS total_amount
FROM app.order_info
WHERE user_id = 10001
  AND order_status = 'PAID';

INSERT INTO app.account_record (
    user_id,
    amount,
    record_type,
    create_time
) VALUES (
    10001,
    100.00,
    'REWARD',
    CURRENT_TIMESTAMP
);

COMMIT;
```

在高并发情况下，串行化事务可能失败，并返回序列化异常。应用层必须具备重试能力。

典型重试策略：

| 步骤           | 说明                   |
| -------------- | ---------------------- |
| 捕获序列化异常 | 识别事务冲突           |
| 回滚事务       | 当前事务不能继续使用   |
| 等待短暂时间   | 避免立即再次冲突       |
| 重试整个事务   | 必须从事务开始重新执行 |
| 限制重试次数   | 避免无限重试           |

适合场景：

| 场景           | 说明                   |
| -------------- | ---------------------- |
| 强一致财务逻辑 | 严格防止并发异常       |
| 复杂跨行约束   | 无法简单用唯一约束表达 |
| 高价值低频操作 | 可以接受重试成本       |
| 严格一致性校验 | 例如复杂额度控制       |

不建议在所有业务接口中默认使用 `SERIALIZABLE`。它会增加并发冲突概率和重试复杂度。大多数业务优先使用默认隔离级别，并通过唯一约束、行锁、条件更新和乐观锁保证正确性。

### 死锁处理

死锁是指两个或多个事务相互等待对方持有的锁，导致都无法继续执行。PostgreSQL 会自动检测死锁，并中止其中一个事务，让另一个事务继续执行。

典型死锁场景：

事务 A：

```sql
BEGIN;

UPDATE app.account_balance
SET balance = balance - 100
WHERE user_id = 10001;

-- 等待事务 B 持有的 user_id = 10002
UPDATE app.account_balance
SET balance = balance + 100
WHERE user_id = 10002;

COMMIT;
```

事务 B：

```sql
BEGIN;

UPDATE app.account_balance
SET balance = balance - 50
WHERE user_id = 10002;

-- 等待事务 A 持有的 user_id = 10001
UPDATE app.account_balance
SET balance = balance + 50
WHERE user_id = 10001;

COMMIT;
```

如果两个事务加锁顺序相反，就可能产生死锁。

查看锁等待：

```sql
SELECT
    a.pid,
    a.usename,
    a.datname,
    a.application_name,
    a.client_addr,
    a.state,
    a.wait_event_type,
    a.wait_event,
    now() - a.query_start AS running_time,
    a.query
FROM pg_stat_activity a
WHERE a.wait_event_type = 'Lock'
ORDER BY running_time DESC;
```

查看阻塞关系：

```sql
SELECT
    blocked.pid AS blocked_pid,
    blocked.query AS blocked_query,
    blocking.pid AS blocking_pid,
    blocking.query AS blocking_query
FROM pg_stat_activity blocked
JOIN pg_locks blocked_locks
    ON blocked_locks.pid = blocked.pid
JOIN pg_locks blocking_locks
    ON blocking_locks.locktype = blocked_locks.locktype
   AND blocking_locks.database IS NOT DISTINCT FROM blocked_locks.database
   AND blocking_locks.relation IS NOT DISTINCT FROM blocked_locks.relation
   AND blocking_locks.page IS NOT DISTINCT FROM blocked_locks.page
   AND blocking_locks.tuple IS NOT DISTINCT FROM blocked_locks.tuple
   AND blocking_locks.virtualxid IS NOT DISTINCT FROM blocked_locks.virtualxid
   AND blocking_locks.transactionid IS NOT DISTINCT FROM blocked_locks.transactionid
   AND blocking_locks.classid IS NOT DISTINCT FROM blocked_locks.classid
   AND blocking_locks.objid IS NOT DISTINCT FROM blocked_locks.objid
   AND blocking_locks.objsubid IS NOT DISTINCT FROM blocked_locks.objsubid
   AND blocking_locks.pid <> blocked_locks.pid
JOIN pg_stat_activity blocking
    ON blocking.pid = blocking_locks.pid
WHERE NOT blocked_locks.granted
  AND blocking_locks.granted;
```

主动终止阻塞会话需要谨慎：

```sql
SELECT pg_terminate_backend(12345);
```

死锁预防建议：

| 建议                   | 说明                         |
| ---------------------- | ---------------------------- |
| 固定加锁顺序           | 多行更新时按主键升序处理     |
| 缩短事务时间           | 不在事务中做耗时外部调用     |
| 减少交互式事务         | 不长时间打开事务等待人工操作 |
| 批量更新分批执行       | 避免一次锁定大量数据         |
| 使用合适索引           | 无索引更新可能锁定更多行     |
| 捕获死锁异常并重试     | 应用层应支持有限次数重试     |
| 避免事务内混合复杂操作 | 尽量减少锁范围和锁时间       |

按固定顺序更新账户，降低死锁概率：

```sql
BEGIN;

-- 先锁定较小 user_id
SELECT
    user_id
FROM app.account_balance
WHERE user_id IN (10001, 10002)
ORDER BY user_id
FOR UPDATE;

UPDATE app.account_balance
SET balance = balance - 100
WHERE user_id = 10001;

UPDATE app.account_balance
SET balance = balance + 100
WHERE user_id = 10002;

COMMIT;
```

### 事务使用规范

事务使用规范的目标是保证数据一致性，同时控制锁范围、事务时长和并发冲突。项目中事务应围绕业务原子操作设计，不应过大，也不应过小。

基本规范如下：

| 规范                    | 说明                                         |
| ----------------------- | -------------------------------------------- |
| 事务边界放在 Service 层 | 一个业务用例一个事务边界                     |
| 事务尽量短              | 不在事务中执行远程调用、文件上传、长时间计算 |
| 先校验再开启事务        | 可在事务外完成的参数校验不要放进事务         |
| 事务内 SQL 尽量少       | 只放必须保证一致性的数据库操作               |
| 固定更新顺序            | 批量更新多行时按主键排序                     |
| 检查影响行数            | 状态流转、库存扣减必须检查更新结果           |
| 避免长事务              | 长事务会影响 VACUUM 和锁释放                 |
| 异常必须回滚            | 应用层要正确抛出异常或手动回滚               |
| 谨慎使用高隔离级别      | 默认隔离级别适合大多数场景                   |
| 为并发冲突设计重试      | 死锁、序列化失败可有限重试                   |

推荐的状态流转写法：

```sql
UPDATE app.order_info
SET
    order_status = 'PAID',
    update_time = CURRENT_TIMESTAMP
WHERE id = 1
  AND order_status = 'PENDING';
```

不推荐先查再无条件更新：

```sql
-- 不推荐：并发情况下查询后状态可能已变化
SELECT
    order_status
FROM app.order_info
WHERE id = 1;

UPDATE app.order_info
SET order_status = 'PAID'
WHERE id = 1;
```

推荐的库存扣减写法：

```sql
UPDATE app.product_stock
SET
    stock_count = stock_count - 1,
    update_time = CURRENT_TIMESTAMP
WHERE product_id = 10001
  AND stock_count >= 1;
```

应用层检查影响行数：

| 影响行数 | 说明                 |
| -------- | -------------------- |
| `1`      | 扣减成功             |
| `0`      | 库存不足或商品不存在 |

长事务排查：

```sql
SELECT
    pid,
    usename,
    datname,
    application_name,
    state,
    now() - xact_start AS transaction_duration,
    now() - query_start AS query_duration,
    wait_event_type,
    wait_event,
    query
FROM pg_stat_activity
WHERE xact_start IS NOT NULL
ORDER BY transaction_duration DESC;
```

空闲但未提交事务排查：

```sql
SELECT
    pid,
    usename,
    datname,
    application_name,
    state,
    now() - xact_start AS transaction_duration,
    query
FROM pg_stat_activity
WHERE state = 'idle in transaction'
ORDER BY transaction_duration DESC;
```

事务设计建议：

| 场景           | 推荐处理                                      |
| -------------- | --------------------------------------------- |
| 创建订单       | 订单主表、明细、库存扣减放同一事务            |
| 支付回调       | 状态流转使用条件更新，保证幂等                |
| 发 MQ 消息     | 本地事务提交后发送，或使用事务消息、Outbox 表 |
| 调用第三方接口 | 不建议把远程调用包在数据库事务中              |
| 批量导入       | 分批提交，避免超大事务                        |
| 数据修复       | 先备份，事务内执行，确认后提交                |
| 报表查询       | 通常使用只读事务或普通查询                    |
| 高并发扣减     | 条件更新、行锁、乐观锁或队列化处理            |

只读事务示例：

```sql
BEGIN READ ONLY;

SELECT
    COUNT(*) AS order_count,
    SUM(total_amount) AS total_amount
FROM app.order_info
WHERE order_status IN ('PAID', 'FINISHED');

COMMIT;
```

事务不是越大越安全。正确的做法是把必须原子提交的数据变更放入同一事务，把耗时操作、外部调用、文件处理、消息发送等尽量移出事务边界，并通过幂等、补偿和重试机制保证最终一致性。


## 锁机制

锁机制用于控制并发事务之间的数据访问和修改，避免数据不一致、丢失更新、重复处理和并发写冲突。PostgreSQL 基于 MVCC 提供较好的并发读写能力，但涉及数据修改、DDL、行级加锁、外键检查和事务隔离时，仍然需要理解锁的行为。

### 表级锁

表级锁作用于整张表，通常由 SQL 自动获取。例如执行 `SELECT`、`INSERT`、`UPDATE`、`DELETE`、`ALTER TABLE`、`CREATE INDEX` 等语句时，PostgreSQL 会根据操作类型自动加对应的表级锁。

常见表级锁模式如下：

| 锁模式                   | 常见来源                                | 说明                           |
| ------------------------ | --------------------------------------- | ------------------------------ |
| `ACCESS SHARE`           | `SELECT`                                | 普通查询获取，和大多数操作兼容 |
| `ROW SHARE`              | `SELECT FOR UPDATE`、`SELECT FOR SHARE` | 行锁查询时同时获取             |
| `ROW EXCLUSIVE`          | `INSERT`、`UPDATE`、`DELETE`            | 普通写操作获取                 |
| `SHARE UPDATE EXCLUSIVE` | `VACUUM`、`ANALYZE`、部分索引操作       | 防止并发结构变更               |
| `SHARE`                  | `CREATE INDEX`                          | 创建普通索引时可能获取         |
| `SHARE ROW EXCLUSIVE`    | 部分约束操作                            | 比 `SHARE` 更强                |
| `EXCLUSIVE`              | 部分刷新、锁表操作                      | 阻塞较多并发操作               |
| `ACCESS EXCLUSIVE`       | `ALTER TABLE`、`DROP TABLE`、`TRUNCATE` | 最强表锁，会阻塞普通查询       |

手动加表级锁：

```sql
BEGIN;

-- 对订单表加 SHARE 锁，允许读取，但会阻塞部分写入和结构变更
LOCK TABLE app.order_info IN SHARE MODE;

SELECT
    COUNT(*) AS order_count
FROM app.order_info;

COMMIT;
```

加排他表锁：

```sql
BEGIN;

-- 对订单表加 ACCESS EXCLUSIVE 锁，阻塞其他读写，生产环境谨慎使用
LOCK TABLE app.order_info IN ACCESS EXCLUSIVE MODE;

ALTER TABLE app.order_info
ADD COLUMN remark VARCHAR(255);

COMMIT;
```

查看当前表锁：

```sql
SELECT
    l.pid,
    a.usename,
    a.datname,
    a.application_name,
    l.locktype,
    l.mode,
    l.granted,
    c.relname AS table_name,
    a.query
FROM pg_locks l
LEFT JOIN pg_class c ON c.oid = l.relation
LEFT JOIN pg_stat_activity a ON a.pid = l.pid
WHERE c.relname = 'order_info'
ORDER BY l.granted, l.mode;
```

表级锁使用建议：

| 建议               | 说明                                           |
| ------------------ | ---------------------------------------------- |
| 尽量不要手动锁表   | 大多数场景由 PostgreSQL 自动管理               |
| DDL 低峰执行       | `ALTER TABLE`、`DROP TABLE` 等可能阻塞业务查询 |
| 大表变更分步执行   | 避免长时间持有强锁                             |
| 建索引优先并发方式 | 大表使用 `CREATE INDEX CONCURRENTLY`           |
| 监控锁等待         | 通过 `pg_locks` 和 `pg_stat_activity` 排查     |

### 行级锁

行级锁作用于具体数据行，常见于 `UPDATE`、`DELETE`、`SELECT FOR UPDATE`、`SELECT FOR SHARE` 等操作。行级锁不会阻塞普通 `SELECT`，但会阻塞其他事务对同一行的冲突性修改。

更新数据时会自动获取行级锁：

```sql
BEGIN;

UPDATE app.order_info
SET
    order_status = 'PAID',
    update_time = CURRENT_TIMESTAMP
WHERE id = 1
  AND order_status = 'PENDING';

COMMIT;
```

上面 SQL 会对满足条件的订单行加锁，直到事务提交或回滚后释放。

手动获取行级锁：

```sql
BEGIN;

SELECT
    id,
    order_no,
    order_status,
    total_amount
FROM app.order_info
WHERE id = 1
FOR UPDATE;

-- 后续可以安全修改该行
UPDATE app.order_info
SET
    order_status = 'CANCELLED',
    update_time = CURRENT_TIMESTAMP
WHERE id = 1;

COMMIT;
```

行级锁常见模式如下：

| 行级锁              | 说明                                  |
| ------------------- | ------------------------------------- |
| `FOR UPDATE`        | 最强行级更新锁，适合即将修改整行数据  |
| `FOR NO KEY UPDATE` | 更新非键字段时常见，弱于 `FOR UPDATE` |
| `FOR SHARE`         | 共享锁，阻止其他事务更新或删除        |
| `FOR KEY SHARE`     | 较弱共享锁，常用于外键相关检查        |

行级锁使用建议：

| 建议           | 说明                             |
| -------------- | -------------------------------- |
| 锁定范围要小   | `WHERE` 条件必须精准             |
| 锁定顺序要固定 | 多行加锁按主键排序，降低死锁概率 |
| 事务要短       | 行锁会持有到事务结束             |
| 避免无索引更新 | 无索引条件可能扫描并锁定大量数据 |
| 检查影响行数   | 并发更新时用影响行数判断是否成功 |

### 共享锁

共享锁允许多个事务同时读取或持有兼容锁，但会阻止某些冲突性修改。共享锁常用于“读取期间不希望数据被修改”的场景。

使用 `FOR SHARE` 获取行级共享锁：

```sql
BEGIN;

SELECT
    id,
    order_no,
    order_status,
    total_amount
FROM app.order_info
WHERE id = 1
FOR SHARE;

COMMIT;
```

`FOR SHARE` 会阻止其他事务对该行执行冲突性更新或删除，但允许其他事务读取该行。

适合场景：

| 场景                   | 说明                         |
| ---------------------- | ---------------------------- |
| 读取后需要保持引用稳定 | 例如读取父表记录后插入子表   |
| 防止读取期间被删除     | 避免后续逻辑基于不存在的数据 |
| 外键相关业务校验       | 确认被引用数据在事务内稳定   |
| 并发读多写少           | 多个事务可以共享读取         |

示例：创建订单前锁定用户记录，防止用户被并发禁用或删除。

```sql
BEGIN;

SELECT
    id,
    username,
    enabled,
    deleted
FROM app.sys_user
WHERE id = 10001
  AND enabled = TRUE
  AND deleted = FALSE
FOR SHARE;

INSERT INTO app.order_info (
    order_no,
    user_id,
    order_status,
    total_amount
) VALUES (
    'PO202605090001',
    10001,
    'PENDING',
    199.90
);

COMMIT;
```

共享锁不适合用来解决所有并发问题。对于需要修改数据的业务，应优先使用条件更新、`FOR UPDATE` 或乐观锁。

### 排他锁

排他锁用于阻止其他事务对同一资源执行冲突操作。`UPDATE`、`DELETE`、`SELECT FOR UPDATE` 都会涉及行级排他性锁。表级的 `ACCESS EXCLUSIVE` 则是最强锁，会阻塞普通查询。

行级排他锁示例：

```sql
BEGIN;

SELECT
    id,
    product_id,
    stock_count
FROM app.product_stock
WHERE product_id = 10001
FOR UPDATE;

UPDATE app.product_stock
SET
    stock_count = stock_count - 1,
    update_time = CURRENT_TIMESTAMP
WHERE product_id = 10001
  AND stock_count >= 1;

COMMIT;
```

表级排他锁示例：

```sql
BEGIN;

LOCK TABLE app.product_stock IN ACCESS EXCLUSIVE MODE;

ALTER TABLE app.product_stock
ADD COLUMN warning_count INTEGER NOT NULL DEFAULT 0;

COMMIT;
```

排他锁适合以下场景：

| 场景     | 说明                             |
| -------- | -------------------------------- |
| 库存扣减 | 防止多个事务同时修改同一库存行   |
| 余额变更 | 防止并发资金更新冲突             |
| 状态流转 | 防止订单、任务、审批单被重复处理 |
| 数据修复 | 避免修复期间并发写入             |
| DDL 变更 | 结构变更需要强锁保护             |

排他锁使用建议：

| 建议               | 说明                         |
| ------------------ | ---------------------------- |
| 优先行级锁         | 不要轻易使用表级排他锁       |
| 条件必须命中索引   | 防止扫描过多行               |
| 控制事务时长       | 锁持有到事务提交或回滚       |
| 避免事务内远程调用 | 外部接口耗时会延长锁持有时间 |
| 固定加锁顺序       | 多资源更新时降低死锁概率     |

### SELECT FOR UPDATE

`SELECT FOR UPDATE` 用于查询并锁定将要修改的数据行。它常用于库存扣减、余额变更、任务领取、订单状态流转等强并发场景。

锁定订单后修改状态：

```sql
BEGIN;

SELECT
    id,
    order_no,
    order_status
FROM app.order_info
WHERE id = 1
FOR UPDATE;

UPDATE app.order_info
SET
    order_status = 'PAID',
    update_time = CURRENT_TIMESTAMP
WHERE id = 1
  AND order_status = 'PENDING';

COMMIT;
```

任务领取场景可以使用 `FOR UPDATE SKIP LOCKED` 跳过已经被其他事务锁定的任务：

```sql
BEGIN;

WITH picked_task AS (
    SELECT
        id
    FROM app.task_info
    WHERE task_status = 'PENDING'
    ORDER BY id ASC
    LIMIT 10
    FOR UPDATE SKIP LOCKED
)
UPDATE app.task_info t
SET
    task_status = 'PROCESSING',
    update_time = CURRENT_TIMESTAMP
FROM picked_task p
WHERE t.id = p.id
RETURNING
    t.id,
    t.task_name,
    t.task_status;

COMMIT;
```

等待锁超时可以使用 `NOWAIT`，如果行已被锁定，会立即报错：

```sql
BEGIN;

SELECT
    id,
    order_no,
    order_status
FROM app.order_info
WHERE id = 1
FOR UPDATE NOWAIT;

COMMIT;
```

`FOR UPDATE` 常用选项：

| 写法                        | 说明                       |
| --------------------------- | -------------------------- |
| `FOR UPDATE`                | 等待并获取更新锁           |
| `FOR UPDATE NOWAIT`         | 如果无法立即加锁，直接报错 |
| `FOR UPDATE SKIP LOCKED`    | 跳过被锁定的行             |
| `FOR UPDATE OF table_alias` | 多表查询中指定锁定哪张表   |

多表查询中只锁订单表：

```sql
BEGIN;

SELECT
    o.id,
    o.order_no,
    u.username
FROM app.order_info o
INNER JOIN app.sys_user u ON u.id = o.user_id
WHERE o.id = 1
FOR UPDATE OF o;

COMMIT;
```

使用建议：

| 场景             | 建议                                    |
| ---------------- | --------------------------------------- |
| 即将修改查询结果 | 使用 `FOR UPDATE`                       |
| 抢任务           | 使用 `FOR UPDATE SKIP LOCKED`           |
| 不希望等待       | 使用 `FOR UPDATE NOWAIT`                |
| 多表查询         | 使用 `FOR UPDATE OF 别名` 控制锁定表    |
| 高并发状态更新   | 也可直接使用条件 `UPDATE`，减少先查后改 |

### SELECT FOR SHARE

`SELECT FOR SHARE` 用于给查询结果加共享锁，阻止其他事务更新或删除这些行。它适合读取后需要保证数据在当前事务内不被并发修改的场景。

基础示例：

```sql
BEGIN;

SELECT
    id,
    username,
    enabled,
    deleted
FROM app.sys_user
WHERE id = 10001
FOR SHARE;

COMMIT;
```

创建订单前共享锁定用户：

```sql
BEGIN;

SELECT
    id,
    username
FROM app.sys_user
WHERE id = 10001
  AND enabled = TRUE
  AND deleted = FALSE
FOR SHARE;

INSERT INTO app.order_info (
    order_no,
    user_id,
    order_status,
    total_amount
) VALUES (
    'PO202605090002',
    10001,
    'PENDING',
    88.00
);

COMMIT;
```

`FOR SHARE` 和 `FOR UPDATE` 对比：

| 对比项             | `FOR SHARE`              | `FOR UPDATE`                   |
| ------------------ | ------------------------ | ------------------------------ |
| 锁强度             | 较弱                     | 较强                           |
| 典型目的           | 保证读取行不被修改或删除 | 准备修改读取行                 |
| 是否允许其他共享锁 | 通常允许                 | 冲突更多                       |
| 常见场景           | 引用校验、读取稳定性     | 库存、余额、任务领取、状态修改 |

如果后续确实要修改该行，优先使用 `FOR UPDATE`。如果只是要保证读取期间数据不被删除或修改，可以使用 `FOR SHARE`。

### 乐观锁

乐观锁是一种应用层并发控制方式，假设冲突较少，不提前加数据库锁，而是在更新时通过版本号、更新时间或状态条件判断数据是否被其他事务修改。

典型字段设计：

```sql
CREATE TABLE app.product_stock (
    id BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    product_id BIGINT NOT NULL,
    stock_count INTEGER NOT NULL DEFAULT 0,
    version INTEGER NOT NULL DEFAULT 0,
    update_time TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uk_product_stock_product_id UNIQUE (product_id)
);
```

基于版本号更新：

```sql
UPDATE app.product_stock
SET
    stock_count = stock_count - 1,
    version = version + 1,
    update_time = CURRENT_TIMESTAMP
WHERE product_id = 10001
  AND stock_count >= 1
  AND version = 5;
```

如果影响行数为 `1`，表示更新成功；如果为 `0`，表示库存不足或版本号已变化，应用层应提示失败或重试。

基于状态条件的乐观锁：

```sql
UPDATE app.order_info
SET
    order_status = 'PAID',
    update_time = CURRENT_TIMESTAMP
WHERE id = 1
  AND order_status = 'PENDING';
```

这种写法适合订单、任务、审批等状态流转，避免重复支付、重复领取、重复审批。

乐观锁适合场景：

| 场景           | 说明                       |
| -------------- | -------------------------- |
| 冲突概率较低   | 多数请求不会修改同一行     |
| 接口响应要求高 | 不希望长时间等待锁         |
| 状态流转       | 通过状态条件控制并发       |
| 后台编辑       | 通过版本号防止覆盖他人修改 |
| 库存扣减       | 简单扣减可用条件更新       |

乐观锁使用建议：

| 建议                 | 说明                         |
| -------------------- | ---------------------------- |
| 必须检查影响行数     | 不检查就无法知道是否冲突     |
| 版本号随更新递增     | `version = version + 1`      |
| 状态流转带前置状态   | `WHERE status = 'PENDING'`   |
| 冲突后明确处理       | 返回失败、重新查询、有限重试 |
| 不适合长流程强占资源 | 需要独占处理时考虑悲观锁     |

### 悲观锁

悲观锁是一种数据库层并发控制方式，假设冲突可能发生，先锁定数据，再执行业务修改。PostgreSQL 中常通过 `SELECT FOR UPDATE` 实现悲观锁。

库存扣减悲观锁示例：

```sql
BEGIN;

SELECT
    id,
    product_id,
    stock_count
FROM app.product_stock
WHERE product_id = 10001
FOR UPDATE;

UPDATE app.product_stock
SET
    stock_count = stock_count - 1,
    update_time = CURRENT_TIMESTAMP
WHERE product_id = 10001
  AND stock_count >= 1;

COMMIT;
```

账户转账悲观锁示例，先按固定顺序锁定账户，降低死锁风险：

```sql
BEGIN;

SELECT
    user_id,
    balance
FROM app.account_balance
WHERE user_id IN (10001, 10002)
ORDER BY user_id
FOR UPDATE;

UPDATE app.account_balance
SET
    balance = balance - 100.00,
    update_time = CURRENT_TIMESTAMP
WHERE user_id = 10001
  AND balance >= 100.00;

UPDATE app.account_balance
SET
    balance = balance + 100.00,
    update_time = CURRENT_TIMESTAMP
WHERE user_id = 10002;

COMMIT;
```

悲观锁适合场景：

| 场景               | 说明                           |
| ------------------ | ------------------------------ |
| 冲突概率高         | 多个事务经常修改同一行         |
| 强一致要求高       | 余额、库存、核心状态           |
| 需要独占处理       | 任务领取、工单处理             |
| 修改前依赖当前值   | 根据当前余额、库存、状态做判断 |
| 不希望并发重试过多 | 通过等待锁串行化处理           |

悲观锁使用建议：

| 建议                         | 说明                       |
| ---------------------------- | -------------------------- |
| 事务必须短                   | 锁持有时间越长，阻塞越严重 |
| 查询条件必须精准             | 防止锁定过多数据           |
| 多行锁定固定顺序             | 降低死锁概率               |
| 避免事务内外部调用           | 不在锁内调用远程接口       |
| 高并发任务使用 `SKIP LOCKED` | 避免大量 worker 互相等待   |

乐观锁和悲观锁对比：

| 对比项       | 乐观锁                   | 悲观锁               |
| ------------ | ------------------------ | -------------------- |
| 加锁时机     | 更新时校验               | 查询时加锁           |
| 冲突处理     | 更新失败后重试或返回失败 | 等待锁释放           |
| 适合冲突概率 | 低                       | 高                   |
| 典型实现     | `version`、状态条件      | `SELECT FOR UPDATE`  |
| 事务占用     | 较少                     | 较多                 |
| 使用风险     | 忘记检查影响行数         | 长事务、锁等待、死锁 |

### 锁等待排查

锁等待是指事务正在等待其他事务释放锁。表现通常是 SQL 长时间不返回、接口超时、连接池耗尽、数据库活动会话增多。

查看当前锁等待会话：

```sql
SELECT
    pid,
    usename,
    datname,
    application_name,
    client_addr,
    state,
    wait_event_type,
    wait_event,
    now() - query_start AS query_duration,
    now() - xact_start AS transaction_duration,
    query
FROM pg_stat_activity
WHERE wait_event_type = 'Lock'
ORDER BY query_duration DESC;
```

查看阻塞和被阻塞关系：

```sql
SELECT
    blocked.pid AS blocked_pid,
    blocked.usename AS blocked_user,
    blocked.application_name AS blocked_app,
    now() - blocked.query_start AS blocked_duration,
    blocked.query AS blocked_query,
    blocking.pid AS blocking_pid,
    blocking.usename AS blocking_user,
    blocking.application_name AS blocking_app,
    now() - blocking.query_start AS blocking_duration,
    blocking.query AS blocking_query
FROM pg_stat_activity blocked
JOIN pg_locks blocked_locks
    ON blocked_locks.pid = blocked.pid
JOIN pg_locks blocking_locks
    ON blocking_locks.locktype = blocked_locks.locktype
   AND blocking_locks.database IS NOT DISTINCT FROM blocked_locks.database
   AND blocking_locks.relation IS NOT DISTINCT FROM blocked_locks.relation
   AND blocking_locks.page IS NOT DISTINCT FROM blocked_locks.page
   AND blocking_locks.tuple IS NOT DISTINCT FROM blocked_locks.tuple
   AND blocking_locks.virtualxid IS NOT DISTINCT FROM blocked_locks.virtualxid
   AND blocking_locks.transactionid IS NOT DISTINCT FROM blocked_locks.transactionid
   AND blocking_locks.classid IS NOT DISTINCT FROM blocked_locks.classid
   AND blocking_locks.objid IS NOT DISTINCT FROM blocked_locks.objid
   AND blocking_locks.objsubid IS NOT DISTINCT FROM blocked_locks.objsubid
   AND blocking_locks.pid <> blocked_locks.pid
JOIN pg_stat_activity blocking
    ON blocking.pid = blocking_locks.pid
WHERE NOT blocked_locks.granted
  AND blocking_locks.granted
ORDER BY blocked_duration DESC;
```

查看长事务：

```sql
SELECT
    pid,
    usename,
    datname,
    application_name,
    state,
    now() - xact_start AS transaction_duration,
    now() - query_start AS query_duration,
    wait_event_type,
    wait_event,
    query
FROM pg_stat_activity
WHERE xact_start IS NOT NULL
ORDER BY transaction_duration DESC;
```

查看空闲未提交事务：

```sql
SELECT
    pid,
    usename,
    datname,
    application_name,
    client_addr,
    now() - xact_start AS transaction_duration,
    query
FROM pg_stat_activity
WHERE state = 'idle in transaction'
ORDER BY transaction_duration DESC;
```

必要时终止阻塞会话：

```sql
-- 先确认 PID 和业务影响，再终止
SELECT pg_terminate_backend(12345);
```

锁等待排查流程：

| 步骤         | 说明                                  |
| ------------ | ------------------------------------- |
| 找等待会话   | 查询 `wait_event_type = 'Lock'`       |
| 找阻塞源     | 关联 `pg_locks` 和 `pg_stat_activity` |
| 判断事务状态 | 是否 `idle in transaction`            |
| 确认业务影响 | 判断是否可以终止阻塞会话              |
| 分析 SQL     | 是否缺索引、长事务、大批量更新        |
| 修复根因     | 优化 SQL、缩短事务、调整加锁顺序      |

### 锁冲突处理

锁冲突处理包括预防、监控和应急处理。预防比事后终止会话更重要，核心思路是减少锁范围、缩短锁时间、统一加锁顺序、使用合适索引和合理事务边界。

设置锁等待超时：

```sql
-- 当前会话锁等待最多 5 秒
SET lock_timeout = '5s';

UPDATE app.order_info
SET
    order_status = 'CANCELLED',
    update_time = CURRENT_TIMESTAMP
WHERE id = 1;
```

设置语句超时：

```sql
-- 当前会话 SQL 最多执行 30 秒
SET statement_timeout = '30s';
```

事务内局部设置：

```sql
BEGIN;

SET LOCAL lock_timeout = '5s';
SET LOCAL statement_timeout = '30s';

UPDATE app.order_info
SET
    order_status = 'CANCELLED',
    update_time = CURRENT_TIMESTAMP
WHERE id = 1;

COMMIT;
```

按固定顺序处理多行，降低死锁风险：

```sql
BEGIN;

SELECT
    id
FROM app.order_info
WHERE id IN (10, 5, 8)
ORDER BY id
FOR UPDATE;

UPDATE app.order_info
SET
    order_status = 'CANCELLED',
    update_time = CURRENT_TIMESTAMP
WHERE id IN (10, 5, 8);

COMMIT;
```

大批量更新分批执行：

```sql
WITH batch_data AS (
    SELECT
        id
    FROM app.operation_log
    WHERE create_time < CURRENT_TIMESTAMP - INTERVAL '180 days'
    ORDER BY id
    LIMIT 1000
)
DELETE FROM app.operation_log l
USING batch_data b
WHERE l.id = b.id;
```

锁冲突处理建议：

| 场景           | 处理方式                          |
| -------------- | --------------------------------- |
| 普通锁等待     | 设置 `lock_timeout`，避免无限等待 |
| 大批量更新     | 分批执行，减少单事务锁范围        |
| 死锁           | 应用层捕获异常并有限重试          |
| 长事务阻塞     | 排查并终止异常会话                |
| DDL 阻塞业务   | 低峰执行，使用并发索引，拆分变更  |
| 任务抢占       | 使用 `FOR UPDATE SKIP LOCKED`     |
| 高并发状态流转 | 使用条件更新或悲观锁              |

生产规范建议：

| 规范                 | 说明                             |
| -------------------- | -------------------------------- |
| 禁止长时间手动事务   | 不在客户端打开事务后长时间不提交 |
| DDL 必须评估锁       | 大表字段变更、索引创建需提前评估 |
| 关键 SQL 必须有索引  | 更新、删除条件应命中索引         |
| 事务内不调用外部系统 | 避免外部耗时导致锁长期持有       |
| 批处理分批提交       | 降低锁等待和回滚成本             |
| 应用设置超时         | 数据库锁等待不能无限拖住线程     |
| 保留应急脚本         | 快速定位阻塞源和长事务           |

## 视图与物化视图

视图用于封装查询逻辑，对外提供稳定的查询入口。普通视图不保存数据，每次查询时都会展开执行底层 SQL。物化视图会保存查询结果，适合复杂报表、汇总数据和低频刷新场景。两者都能提升 SQL 复用性，但定位不同。

### 普通视图创建

普通视图使用 `CREATE VIEW` 创建，本质上是一个命名查询。它不存储查询结果，查询视图时会访问底层表。

创建用户角色视图：

```sql
CREATE OR REPLACE VIEW app.v_sys_user_role AS
SELECT
    u.id AS user_id,
    u.username,
    u.nickname,
    u.enabled AS user_enabled,
    r.id AS role_id,
    r.role_code,
    r.role_name,
    r.enabled AS role_enabled
FROM app.sys_user u
LEFT JOIN app.sys_user_role ur ON ur.user_id = u.id
LEFT JOIN app.sys_role r ON r.id = ur.role_id
WHERE u.deleted = FALSE;
```

创建订单汇总视图：

```sql
CREATE OR REPLACE VIEW app.v_order_summary AS
SELECT
    o.id AS order_id,
    o.order_no,
    o.user_id,
    u.username,
    u.nickname,
    o.order_status,
    o.total_amount,
    COUNT(oi.id) AS item_count,
    COALESCE(SUM(oi.quantity * oi.sale_price), 0) AS item_amount,
    o.create_time
FROM app.order_info o
INNER JOIN app.sys_user u ON u.id = o.user_id
LEFT JOIN app.order_item oi ON oi.order_id = o.id
GROUP BY
    o.id,
    o.order_no,
    o.user_id,
    u.username,
    u.nickname,
    o.order_status,
    o.total_amount,
    o.create_time;
```

查看视图定义：

```sql
SELECT
    schemaname,
    viewname,
    definition
FROM pg_views
WHERE schemaname = 'app'
ORDER BY viewname;
```

删除视图：

```sql
DROP VIEW IF EXISTS app.v_sys_user_role;
```

普通视图创建建议：

| 建议                     | 说明                           |
| ------------------------ | ------------------------------ |
| 视图名使用 `v_` 前缀     | 便于和普通表区分               |
| 字段别名明确             | 避免多个表同名字段冲突         |
| 不在视图中写过度复杂逻辑 | 复杂逻辑难以排查性能           |
| 不把视图当缓存           | 普通视图不保存结果             |
| 底层表变更要同步检查视图 | 字段删除或改名可能导致视图失效 |

### 普通视图使用

普通视图可以像普通表一样查询。它适合封装复杂 JOIN、统一字段命名、隐藏敏感字段、提供报表查询入口和简化应用 SQL。

查询用户角色视图：

```sql
SELECT
    user_id,
    username,
    nickname,
    role_code,
    role_name
FROM app.v_sys_user_role
WHERE user_enabled = TRUE
ORDER BY user_id, role_code;
```

查询订单汇总视图：

```sql
SELECT
    order_id,
    order_no,
    username,
    order_status,
    total_amount,
    item_count,
    item_amount,
    create_time
FROM app.v_order_summary
WHERE order_status = 'PAID'
ORDER BY create_time DESC
LIMIT 20;
```

应用层可以直接查询视图：

```sql
SELECT
    order_no,
    username,
    total_amount,
    item_count
FROM app.v_order_summary
WHERE create_time >= TIMESTAMP '2026-05-01 00:00:00'
  AND create_time < TIMESTAMP '2026-06-01 00:00:00';
```

视图使用注意事项：

| 注意点               | 说明                         |
| -------------------- | ---------------------------- |
| 普通视图不提升性能   | 它只是封装 SQL，不缓存结果   |
| 视图嵌套不宜过深     | 多层视图会增加理解和优化难度 |
| 过滤条件仍需索引支持 | 底层表字段要有合适索引       |
| 查询计划仍看底层表   | 使用 `EXPLAIN` 分析视图查询  |
| 字段变更会影响视图   | 底层表结构调整后要检查视图   |

分析视图查询计划：

```sql
EXPLAIN ANALYZE
SELECT
    order_no,
    username,
    total_amount
FROM app.v_order_summary
WHERE order_status = 'PAID'
ORDER BY create_time DESC
LIMIT 20;
```

普通视图适合提高 SQL 复用和权限隔离，不适合解决复杂查询慢的问题。如果查询本身很慢，应优化底层 SQL、索引或考虑物化视图。

### 普通视图更新

部分普通视图可以直接执行 `INSERT`、`UPDATE`、`DELETE`，但前提是视图足够简单，例如只基于单表、不包含聚合、分组、去重、连接、集合操作等复杂结构。复杂视图通常不可直接更新。

创建简单可更新视图：

```sql
CREATE OR REPLACE VIEW app.v_enabled_user AS
SELECT
    id,
    username,
    nickname,
    phone,
    email,
    enabled
FROM app.sys_user
WHERE deleted = FALSE
  AND enabled = TRUE;
```

通过视图更新底层表：

```sql
UPDATE app.v_enabled_user
SET
    nickname = '管理员',
    phone = '13800000000'
WHERE username = 'admin';
```

通过视图插入数据：

```sql
INSERT INTO app.v_enabled_user (
    username,
    nickname,
    phone,
    email,
    enabled
) VALUES (
    'view_user',
    '视图用户',
    '13800000009',
    'view_user@example.com',
    TRUE
);
```

使用 `WITH CHECK OPTION` 限制写入后仍必须满足视图条件：

```sql
CREATE OR REPLACE VIEW app.v_enabled_user_checked AS
SELECT
    id,
    username,
    nickname,
    phone,
    email,
    enabled,
    deleted
FROM app.sys_user
WHERE deleted = FALSE
  AND enabled = TRUE
WITH CHECK OPTION;
```

尝试通过该视图写入不满足条件的数据会失败：

```sql
INSERT INTO app.v_enabled_user_checked (
    username,
    nickname,
    enabled,
    deleted
) VALUES (
    'disabled_user',
    '禁用用户',
    FALSE,
    FALSE
);
```

复杂视图通常不能直接更新，例如包含 JOIN 或聚合：

```sql
CREATE OR REPLACE VIEW app.v_user_role_count AS
SELECT
    u.id AS user_id,
    u.username,
    COUNT(ur.role_id) AS role_count
FROM app.sys_user u
LEFT JOIN app.sys_user_role ur ON ur.user_id = u.id
GROUP BY
    u.id,
    u.username;
```

这种视图更适合只读查询。如果确实需要更新复杂视图，可以使用 `INSTEAD OF` 触发器，但普通项目中不建议增加这种复杂度。

普通视图更新建议：

| 场景             | 建议                                     |
| ---------------- | ---------------------------------------- |
| 简单单表视图     | 可以考虑可更新视图                       |
| 复杂 JOIN 视图   | 建议只读                                 |
| 聚合视图         | 只读                                     |
| 对外暴露写接口   | 优先写应用服务层，不建议通过复杂视图写入 |
| 需要限制写入范围 | 使用 `WITH CHECK OPTION`                 |
| 复杂写入逻辑     | 使用 Service 层事务处理                  |

### 物化视图创建

物化视图使用 `CREATE MATERIALIZED VIEW` 创建。它会保存查询结果，查询时读取的是已保存的数据，而不是每次都重新执行底层查询。适合复杂报表、统计汇总、低频更新、高频读取的场景。

创建每日订单统计物化视图：

```sql
CREATE MATERIALIZED VIEW app.mv_daily_order_stat AS
SELECT
    create_time::date AS order_date,
    order_status,
    COUNT(*) AS order_count,
    SUM(total_amount) AS total_amount,
    MIN(create_time) AS first_order_time,
    MAX(create_time) AS last_order_time
FROM app.order_info
GROUP BY
    create_time::date,
    order_status;
```

查询物化视图：

```sql
SELECT
    order_date,
    order_status,
    order_count,
    total_amount
FROM app.mv_daily_order_stat
ORDER BY order_date DESC, order_status;
```

创建时不立即填充数据：

```sql
CREATE MATERIALIZED VIEW app.mv_monthly_order_stat AS
SELECT
    date_trunc('month', create_time)::date AS order_month,
    order_status,
    COUNT(*) AS order_count,
    SUM(total_amount) AS total_amount
FROM app.order_info
GROUP BY
    date_trunc('month', create_time)::date,
    order_status
WITH NO DATA;
```

使用 `WITH NO DATA` 创建后，需要刷新后才能查询：

```sql
REFRESH MATERIALIZED VIEW app.mv_monthly_order_stat;
```

物化视图命名建议使用 `mv_` 前缀，便于和普通表、普通视图区分。

物化视图适合场景：

| 场景           | 说明                 |
| -------------- | -------------------- |
| 报表统计       | 日报、月报、状态分布 |
| 复杂 JOIN 结果 | 多表关联查询成本高   |
| 聚合成本高     | 大表聚合耗时明显     |
| 读多写少       | 可以接受定时刷新     |
| 数据允许延迟   | 不要求实时一致       |

物化视图不适合实时性要求很高的查询，因为它的数据只有在刷新后才会更新。

### 物化视图刷新

物化视图不会自动随底层表变化而变化，需要手动或定时刷新。刷新方式包括普通刷新和并发刷新。

普通刷新：

```sql
REFRESH MATERIALIZED VIEW app.mv_daily_order_stat;
```

普通刷新期间，物化视图可能无法被正常读取，适合低峰期或数据量较小的场景。

并发刷新：

```sql
REFRESH MATERIALIZED VIEW CONCURRENTLY app.mv_daily_order_stat;
```

并发刷新允许刷新期间继续读取物化视图，但需要满足条件：物化视图上必须存在合适的唯一索引，并且物化视图已经填充过数据。

为物化视图创建唯一索引：

```sql
CREATE UNIQUE INDEX uk_mv_daily_order_stat_date_status
ON app.mv_daily_order_stat (order_date, order_status);
```

然后执行并发刷新：

```sql
REFRESH MATERIALIZED VIEW CONCURRENTLY app.mv_daily_order_stat;
```

定时刷新可以由应用任务、`cron`、调度平台或数据库任务调度工具触发。

刷新脚本示例：

```sql
REFRESH MATERIALIZED VIEW CONCURRENTLY app.mv_daily_order_stat;
REFRESH MATERIALIZED VIEW CONCURRENTLY app.mv_monthly_order_stat;
```

刷新建议如下：

| 建议                   | 说明                        |
| ---------------------- | --------------------------- |
| 小数据量可普通刷新     | 简单直接                    |
| 生产查询高频用并发刷新 | 减少读取阻塞                |
| 并发刷新需要唯一索引   | 必须提前设计唯一键          |
| 刷新频率按业务确定     | 例如每 5 分钟、每小时、每天 |
| 刷新失败要告警         | 报表数据可能变旧            |
| 大物化视图错峰刷新     | 避免影响业务高峰            |

物化视图刷新不是增量刷新。每次刷新通常会重新执行物化视图查询，因此底层 SQL、索引和数据量仍然需要优化。

### 物化视图索引

物化视图可以像普通表一样创建索引。索引用于提升物化视图查询性能，也用于支持并发刷新。物化视图创建后，应根据查询条件和刷新方式设计索引。

为日期和状态查询创建唯一索引：

```sql
CREATE UNIQUE INDEX uk_mv_daily_order_stat_date_status
ON app.mv_daily_order_stat (order_date, order_status);
```

为日期排序查询创建索引：

```sql
CREATE INDEX idx_mv_daily_order_stat_date
ON app.mv_daily_order_stat (order_date DESC);
```

查询示例：

```sql
SELECT
    order_date,
    order_status,
    order_count,
    total_amount
FROM app.mv_daily_order_stat
WHERE order_date >= DATE '2026-05-01'
  AND order_date < DATE '2026-06-01'
ORDER BY order_date DESC;
```

多维统计物化视图示例：

```sql
CREATE MATERIALIZED VIEW app.mv_user_order_stat AS
SELECT
    user_id,
    COUNT(*) AS order_count,
    SUM(total_amount) AS total_amount,
    MAX(create_time) AS latest_order_time
FROM app.order_info
WHERE order_status IN ('PAID', 'FINISHED')
GROUP BY user_id;
```

创建索引：

```sql
CREATE UNIQUE INDEX uk_mv_user_order_stat_user_id
ON app.mv_user_order_stat (user_id);

CREATE INDEX idx_mv_user_order_stat_total_amount
ON app.mv_user_order_stat (total_amount DESC);
```

物化视图索引建议：

| 场景         | 推荐索引                 |
| ------------ | ------------------------ |
| 并发刷新     | 唯一索引                 |
| 按日期查询   | 日期字段索引             |
| 按用户查询   | 用户 ID 索引             |
| 排行榜       | 金额、数量等排序字段索引 |
| 多条件筛选   | 联合索引                 |
| 查询字段较少 | 可考虑覆盖索引           |

需要注意，刷新物化视图时索引也需要维护。索引越多，刷新成本越高。因此不要对物化视图无脑创建大量索引，应按真实查询创建。

### 视图权限控制

视图可以用于权限隔离。通过只授权用户访问视图，而不直接访问底层表，可以隐藏敏感字段、限制行范围、统一数据出口。

创建脱敏视图：

```sql
CREATE OR REPLACE VIEW app.v_user_public AS
SELECT
    id,
    username,
    nickname,
    CASE
        WHEN phone IS NULL THEN NULL
        WHEN length(phone) < 7 THEN phone
        ELSE left(phone, 3) || '****' || right(phone, 4)
    END AS masked_phone,
    enabled,
    create_time
FROM app.sys_user
WHERE deleted = FALSE;
```

创建只读角色：

```sql
CREATE ROLE app_viewer NOLOGIN;
```

授予 Schema 使用权限：

```sql
GRANT USAGE ON SCHEMA app TO app_viewer;
```

授予视图查询权限：

```sql
GRANT SELECT ON app.v_user_public TO app_viewer;
```

回收底层表权限：

```sql
REVOKE SELECT ON app.sys_user FROM app_viewer;
```

创建登录用户并继承视图角色：

```sql
CREATE ROLE report_user WITH LOGIN PASSWORD 'report_user_123456';

GRANT app_viewer TO report_user;
```

物化视图授权方式类似普通表：

```sql
GRANT SELECT ON app.mv_daily_order_stat TO app_viewer;
```

查看视图权限：

```sql
SELECT
    table_schema,
    table_name,
    privilege_type,
    grantee
FROM information_schema.role_table_grants
WHERE table_schema = 'app'
  AND table_name IN ('v_user_public', 'mv_daily_order_stat')
ORDER BY table_name, grantee, privilege_type;
```

视图权限控制建议：

| 建议                 | 说明                             |
| -------------------- | -------------------------------- |
| 报表账号只授予视图   | 不直接开放核心表                 |
| 敏感字段在视图中脱敏 | 手机号、邮箱、身份证等           |
| 行级过滤可封装进视图 | 例如只展示未删除、已启用数据     |
| Schema 权限仍然需要  | 需要 `GRANT USAGE ON SCHEMA`     |
| 定期审计权限         | 检查是否误授底层表权限           |
| 复杂权限考虑 RLS     | 行级安全策略比视图更适合复杂权限 |

### 视图使用场景

普通视图和物化视图的使用场景不同。普通视图偏向 SQL 封装、权限隔离和接口稳定；物化视图偏向缓存复杂查询结果、提升报表读取性能。

普通视图适合场景：

| 场景          | 说明                           |
| ------------- | ------------------------------ |
| 封装复杂 JOIN | 简化应用查询 SQL               |
| 统一字段命名  | 对外提供稳定字段               |
| 隐藏敏感字段  | 只暴露脱敏字段                 |
| 限制数据范围  | 例如只展示未删除数据           |
| 兼容历史接口  | 表结构变化后通过视图维持旧字段 |
| 报表只读入口  | 降低直接访问底层表风险         |

物化视图适合场景：

| 场景         | 说明                     |
| ------------ | ------------------------ |
| 大表聚合统计 | 日报、月报、汇总指标     |
| 多表复杂关联 | 预先保存关联结果         |
| 排行榜       | 定时刷新排行榜数据       |
| 读多写少     | 查询频繁，数据延迟可接受 |
| BI 报表      | 降低明细表查询压力       |
| 历史快照     | 固定时间点统计结果       |

选择建议：

| 需求                       | 推荐               |
| -------------------------- | ------------------ |
| 只是简化 SQL               | 普通视图           |
| 需要权限隔离               | 普通视图           |
| 需要脱敏展示               | 普通视图           |
| 复杂查询很慢               | 物化视图           |
| 数据允许延迟               | 物化视图           |
| 要实时数据                 | 普通视图或直接查表 |
| 报表高频访问               | 物化视图           |
| 底层数据变化频繁且必须实时 | 不建议物化视图     |

普通视图示例：对外提供用户公开信息。

```sql
CREATE OR REPLACE VIEW app.v_user_public AS
SELECT
    id,
    username,
    nickname,
    enabled,
    create_time
FROM app.sys_user
WHERE deleted = FALSE;
```

物化视图示例：对外提供每日订单统计。

```sql
CREATE MATERIALIZED VIEW app.mv_daily_order_stat AS
SELECT
    create_time::date AS order_date,
    order_status,
    COUNT(*) AS order_count,
    SUM(total_amount) AS total_amount
FROM app.order_info
GROUP BY
    create_time::date,
    order_status;
```

整体建议：

| 建议                   | 说明                             |
| ---------------------- | -------------------------------- |
| 不要滥用视图嵌套       | 多层视图会增加排查难度           |
| 视图 SQL 也要优化      | 普通视图不自动提升性能           |
| 物化视图要设计刷新策略 | 明确刷新时间、失败告警和数据延迟 |
| 物化视图要建索引       | 否则查询物化视图也可能很慢       |
| 权限通过角色管理       | 不直接给个人账号授权             |
| 视图纳入版本管理       | 创建、修改、删除都应进入迁移脚本 |

视图的价值在于统一查询入口和隔离复杂性，物化视图的价值在于用可接受的数据延迟换取查询性能。实际项目中应根据实时性、性能、权限和维护成本综合选择。


## 函数与存储过程

函数与存储过程用于把可复用的数据库逻辑封装在数据库内部。PostgreSQL 中函数通过 `CREATE FUNCTION` 创建，通常有返回值，可以在 SQL 中调用；存储过程通过 `CREATE PROCEDURE` 创建，使用 `CALL` 调用，更适合封装一组过程性操作。项目开发中应谨慎使用数据库函数和存储过程，避免把大量业务逻辑沉淀到数据库中，导致应用层和数据库层职责混乱。

### 内置函数使用

PostgreSQL 提供了大量内置函数，覆盖字符串、日期时间、数值、JSON、数组、聚合、系统信息等常见场景。日常 SQL 开发中应优先使用内置函数，避免重复造轮子。

常用字符串函数：

```sql
SELECT
    lower('ADMIN@EXAMPLE.COM') AS lower_email,
    upper('postgresql') AS upper_text,
    length('PostgreSQL 17') AS text_length,
    substring('PostgreSQL' FROM 1 FOR 8) AS sub_text,
    replace('PostgreSQL-17', '-', ' ') AS replaced_text,
    concat_ws(' / ', 'PostgreSQL', '17', 'Database') AS joined_text;
```

常用日期时间函数：

```sql
SELECT
    CURRENT_DATE AS current_date,
    CURRENT_TIMESTAMP AS current_timestamp,
    clock_timestamp() AS real_clock_time,
    date_trunc('day', CURRENT_TIMESTAMP) AS current_day_start,
    CURRENT_TIMESTAMP + INTERVAL '7 days' AS after_seven_days,
    EXTRACT(YEAR FROM CURRENT_TIMESTAMP) AS current_year;
```

常用数值函数：

```sql
SELECT
    round(123.4567, 2) AS round_value,
    ceil(123.1) AS ceil_value,
    floor(123.9) AS floor_value,
    abs(-100) AS abs_value,
    power(2, 10) AS power_value;
```

常用 JSONB 函数：

```sql
SELECT
    jsonb_build_object(
        'name', 'PostgreSQL',
        'version', 17,
        'enabled', true
    ) AS json_object,
    jsonb_build_array('java', 'spring-boot', 'postgresql') AS json_array;
```

常用数组函数：

```sql
SELECT
    cardinality(ARRAY['java', 'postgresql', 'sql']) AS array_size,
    array_append(ARRAY['java', 'postgresql'], 'sql') AS appended_array,
    array_remove(ARRAY['java', 'postgresql', 'sql'], 'sql') AS removed_array;
```

常用系统函数：

```sql
SELECT
    current_database() AS database_name,
    current_user AS current_user_name,
    version() AS database_version,
    inet_client_addr() AS client_addr,
    inet_server_addr() AS server_addr;
```

内置函数使用建议如下：

| 建议                         | 说明                                     |
| ---------------------------- | ---------------------------------------- |
| 优先使用内置函数             | 字符串、时间、JSON、数组处理都有成熟函数 |
| WHERE 中慎用函数包裹索引列   | 可能导致普通索引无法使用                 |
| 复杂格式化可放应用层         | 接口展示格式通常由应用层处理             |
| JSON 和数组函数注意类型转换  | `->>` 返回文本，参与数值比较时要显式转换 |
| 高频表达式查询可建表达式索引 | 例如 `lower(email)`、`create_time::date` |

### 自定义函数

自定义函数用于封装可复用逻辑。PostgreSQL 支持使用 SQL、PL/pgSQL、PL/Python 等语言创建函数。普通项目最常用的是 SQL 函数和 PL/pgSQL 函数。

创建函数的一般结构：

```sql
CREATE OR REPLACE FUNCTION app.function_name(
    参数名 参数类型
)
RETURNS 返回类型
LANGUAGE plpgsql
AS $$
BEGIN
    -- 函数逻辑
END;
$$;
```

简单示例：判断用户是否启用。

```sql
CREATE OR REPLACE FUNCTION app.is_user_enabled(
    p_user_id BIGINT
)
RETURNS BOOLEAN
LANGUAGE sql
STABLE
AS $$
    SELECT EXISTS (
        SELECT 1
        FROM app.sys_user
        WHERE id = p_user_id
          AND enabled = TRUE
          AND deleted = FALSE
    );
$$;
```

调用函数：

```sql
SELECT app.is_user_enabled(10001) AS enabled;
```

根据用户名获取用户 ID：

```sql
CREATE OR REPLACE FUNCTION app.get_user_id_by_username(
    p_username VARCHAR
)
RETURNS BIGINT
LANGUAGE sql
STABLE
AS $$
    SELECT id
    FROM app.sys_user
    WHERE username = p_username
      AND deleted = FALSE
    LIMIT 1;
$$;
```

调用示例：

```sql
SELECT app.get_user_id_by_username('admin') AS user_id;
```

自定义函数适合以下场景：

| 场景          | 说明                   |
| ------------- | ---------------------- |
| 重复 SQL 逻辑 | 多处复用同一查询逻辑   |
| 数据转换      | 格式化、脱敏、字段转换 |
| 数据校验      | 简单规则校验           |
| 报表辅助      | 汇总查询中的辅助计算   |
| 触发器函数    | 触发器必须调用函数     |

自定义函数不适合承载复杂业务流程，例如订单全流程、支付回调、审批流程等。这些逻辑更适合放在应用服务层，便于调试、测试、灰度和监控。

### SQL 函数

SQL 函数使用 `LANGUAGE sql` 创建，适合封装单条 SQL 或简单查询逻辑。它结构简单、执行直接，适合无复杂流程控制的场景。

创建金额格式化函数：

```sql
CREATE OR REPLACE FUNCTION app.format_amount(
    p_amount NUMERIC
)
RETURNS TEXT
LANGUAGE sql
IMMUTABLE
AS $$
    SELECT to_char(COALESCE(p_amount, 0), 'FM999999999990.00');
$$;
```

调用示例：

```sql
SELECT app.format_amount(1234.5) AS amount_text;
```

创建手机号脱敏函数：

```sql
CREATE OR REPLACE FUNCTION app.mask_phone(
    p_phone TEXT
)
RETURNS TEXT
LANGUAGE sql
IMMUTABLE
AS $$
    SELECT CASE
        WHEN p_phone IS NULL THEN NULL
        WHEN length(p_phone) < 7 THEN p_phone
        ELSE left(p_phone, 3) || '****' || right(p_phone, 4)
    END;
$$;
```

查询中使用：

```sql
SELECT
    id,
    username,
    app.mask_phone(phone) AS masked_phone
FROM app.sys_user
WHERE deleted = FALSE;
```

创建统计函数：

```sql
CREATE OR REPLACE FUNCTION app.count_user_by_enabled(
    p_enabled BOOLEAN
)
RETURNS BIGINT
LANGUAGE sql
STABLE
AS $$
    SELECT COUNT(*)
    FROM app.sys_user
    WHERE enabled = p_enabled
      AND deleted = FALSE;
$$;
```

调用示例：

```sql
SELECT app.count_user_by_enabled(TRUE) AS enabled_user_count;
```

函数稳定性标记说明：

| 标记        | 说明                                     | 示例                     |
| ----------- | ---------------------------------------- | ------------------------ |
| `IMMUTABLE` | 相同输入永远返回相同结果                 | 字符串脱敏、纯计算       |
| `STABLE`    | 同一条语句中结果稳定，可能依赖数据库状态 | 查询配置、查询用户       |
| `VOLATILE`  | 结果可能随时变化，默认值                 | 随机数、当前时间、写操作 |

SQL 函数建议用于短小、明确、无复杂分支的逻辑。如果需要变量、循环、异常处理，应使用 PL/pgSQL 函数。

### PL/pgSQL 函数

PL/pgSQL 函数支持变量、分支、循环、异常处理和多条 SQL，适合封装较复杂的数据库逻辑。创建 PL/pgSQL 函数前，一般需要确认数据库已支持 `plpgsql` 语言；现代 PostgreSQL 默认可用。

创建用户状态文本函数：

```sql
CREATE OR REPLACE FUNCTION app.get_user_status_text(
    p_user_id BIGINT
)
RETURNS TEXT
LANGUAGE plpgsql
STABLE
AS $$
DECLARE
    v_enabled BOOLEAN;
    v_deleted BOOLEAN;
BEGIN
    SELECT
        enabled,
        deleted
    INTO
        v_enabled,
        v_deleted
    FROM app.sys_user
    WHERE id = p_user_id;

    IF NOT FOUND THEN
        RETURN '用户不存在';
    END IF;

    IF v_deleted THEN
        RETURN '已删除';
    ELSIF v_enabled THEN
        RETURN '启用';
    ELSE
        RETURN '禁用';
    END IF;
END;
$$;
```

调用示例：

```sql
SELECT app.get_user_status_text(10001) AS status_text;
```

创建订单状态变更函数：

```sql
CREATE OR REPLACE FUNCTION app.change_order_status(
    p_order_id BIGINT,
    p_from_status VARCHAR,
    p_to_status VARCHAR
)
RETURNS BOOLEAN
LANGUAGE plpgsql
AS $$
DECLARE
    v_update_count INTEGER;
BEGIN
    UPDATE app.order_info
    SET
        order_status = p_to_status,
        update_time = CURRENT_TIMESTAMP
    WHERE id = p_order_id
      AND order_status = p_from_status;

    GET DIAGNOSTICS v_update_count = ROW_COUNT;

    RETURN v_update_count = 1;
END;
$$;
```

调用示例：

```sql
SELECT app.change_order_status(1, 'PENDING', 'PAID') AS success;
```

返回表数据的 PL/pgSQL 函数：

```sql
CREATE OR REPLACE FUNCTION app.list_user_order(
    p_user_id BIGINT
)
RETURNS TABLE (
    order_id BIGINT,
    order_no VARCHAR,
    order_status VARCHAR,
    total_amount NUMERIC,
    create_time TIMESTAMP
)
LANGUAGE plpgsql
STABLE
AS $$
BEGIN
    RETURN QUERY
    SELECT
        o.id,
        o.order_no,
        o.order_status,
        o.total_amount,
        o.create_time
    FROM app.order_info o
    WHERE o.user_id = p_user_id
    ORDER BY o.create_time DESC, o.id DESC;
END;
$$;
```

调用示例：

```sql
SELECT *
FROM app.list_user_order(10001);
```

PL/pgSQL 使用建议：

| 建议         | 说明                                             |
| ------------ | ------------------------------------------------ |
| 变量命名清晰 | 参数可用 `p_` 前缀，变量可用 `v_` 前缀           |
| 检查影响行数 | 使用 `GET DIAGNOSTICS ... ROW_COUNT`             |
| 函数不要过长 | 复杂业务建议放应用层                             |
| 明确稳定性   | 查询函数使用 `STABLE`，纯计算使用 `IMMUTABLE`    |
| 注意事务边界 | 函数在调用方事务中执行，函数内部不能随意提交事务 |

### 存储过程

存储过程使用 `CREATE PROCEDURE` 创建，使用 `CALL` 调用。它与函数的主要区别是过程不通过 `SELECT` 返回结果，更适合封装一组操作。PostgreSQL 的过程可以在特定条件下进行事务控制，但在应用项目中通常仍建议由应用层管理事务边界。

创建同步订单统计过程：

```sql
CREATE OR REPLACE PROCEDURE app.refresh_daily_order_stat()
LANGUAGE plpgsql
AS $$
BEGIN
    DELETE FROM app.daily_order_stat
    WHERE stat_date >= CURRENT_DATE - INTERVAL '7 days';

    INSERT INTO app.daily_order_stat (
        stat_date,
        order_status,
        order_count,
        total_amount,
        create_time
    )
    SELECT
        create_time::date AS stat_date,
        order_status,
        COUNT(*) AS order_count,
        COALESCE(SUM(total_amount), 0) AS total_amount,
        CURRENT_TIMESTAMP
    FROM app.order_info
    WHERE create_time >= CURRENT_DATE - INTERVAL '7 days'
    GROUP BY
        create_time::date,
        order_status;
END;
$$;
```

调用存储过程：

```sql
CALL app.refresh_daily_order_stat();
```

带参数的存储过程：

```sql
CREATE OR REPLACE PROCEDURE app.archive_deleted_user(
    p_before_time TIMESTAMP
)
LANGUAGE plpgsql
AS $$
BEGIN
    INSERT INTO app.sys_user_archive (
        user_id,
        username,
        nickname,
        archive_time
    )
    SELECT
        id,
        username,
        nickname,
        CURRENT_TIMESTAMP
    FROM app.sys_user
    WHERE deleted = TRUE
      AND update_time < p_before_time;

    DELETE FROM app.sys_user
    WHERE deleted = TRUE
      AND update_time < p_before_time;
END;
$$;
```

调用示例：

```sql
CALL app.archive_deleted_user(CURRENT_TIMESTAMP - INTERVAL '180 days');
```

函数和存储过程对比：

| 对比项     | 函数                        | 存储过程                     |
| ---------- | --------------------------- | ---------------------------- |
| 创建语法   | `CREATE FUNCTION`           | `CREATE PROCEDURE`           |
| 调用方式   | `SELECT function_name(...)` | `CALL procedure_name(...)`   |
| 返回值     | 通常有返回值                | 通常不直接返回查询值         |
| 适合场景   | 计算、查询、触发器函数      | 批处理、维护任务、过程性操作 |
| SQL 中使用 | 可以作为表达式使用          | 不能像函数一样用于普通表达式 |
| 事务控制   | 通常由调用方事务控制        | 可用于过程性事务控制场景     |

项目中建议：查询和计算逻辑使用函数；批处理维护任务可以使用过程；核心业务流程优先由应用服务层实现。

### 参数传递

PostgreSQL 函数和过程支持输入参数、输出参数、默认参数和命名参数调用。参数设计应清晰表达业务含义，避免使用过多位置参数导致调用混乱。

基础输入参数：

```sql
CREATE OR REPLACE FUNCTION app.get_order_amount(
    p_order_no VARCHAR
)
RETURNS NUMERIC
LANGUAGE sql
STABLE
AS $$
    SELECT total_amount
    FROM app.order_info
    WHERE order_no = p_order_no
    LIMIT 1;
$$;
```

调用：

```sql
SELECT app.get_order_amount('PO202605090001') AS total_amount;
```

默认参数：

```sql
CREATE OR REPLACE FUNCTION app.list_recent_order(
    p_limit INTEGER DEFAULT 20
)
RETURNS TABLE (
    order_id BIGINT,
    order_no VARCHAR,
    total_amount NUMERIC,
    create_time TIMESTAMP
)
LANGUAGE sql
STABLE
AS $$
    SELECT
        id,
        order_no,
        total_amount,
        create_time
    FROM app.order_info
    ORDER BY create_time DESC, id DESC
    LIMIT p_limit;
$$;
```

调用默认参数：

```sql
SELECT *
FROM app.list_recent_order();
```

调用指定参数：

```sql
SELECT *
FROM app.list_recent_order(10);
```

命名参数调用：

```sql
SELECT *
FROM app.list_recent_order(p_limit => 5);
```

输出参数示例：

```sql
CREATE OR REPLACE FUNCTION app.get_user_contact(
    p_user_id BIGINT,
    OUT o_username VARCHAR,
    OUT o_phone VARCHAR,
    OUT o_email VARCHAR
)
LANGUAGE sql
STABLE
AS $$
    SELECT
        username,
        phone,
        email
    FROM app.sys_user
    WHERE id = p_user_id;
$$;
```

调用：

```sql
SELECT *
FROM app.get_user_contact(10001);
```

参数设计建议：

| 建议               | 说明                              |
| ------------------ | --------------------------------- |
| 参数使用业务名     | 如 `p_user_id`、`p_order_no`      |
| 控制参数数量       | 参数过多时考虑传 JSONB 或拆分函数 |
| 默认参数放后面     | 便于位置参数调用                  |
| 复杂调用用命名参数 | 提升可读性                        |
| 参数类型明确       | 避免隐式转换导致函数匹配异常      |

### 返回值处理

函数返回值可以是标量、复合类型、表结构、集合或 JSON。返回值设计应符合调用场景：简单计算返回标量，列表查询返回表结构，接口式结果可以返回 JSONB。

返回单个值：

```sql
CREATE OR REPLACE FUNCTION app.count_enabled_user()
RETURNS BIGINT
LANGUAGE sql
STABLE
AS $$
    SELECT COUNT(*)
    FROM app.sys_user
    WHERE enabled = TRUE
      AND deleted = FALSE;
$$;
```

调用：

```sql
SELECT app.count_enabled_user() AS enabled_user_count;
```

返回表结构：

```sql
CREATE OR REPLACE FUNCTION app.search_user(
    p_keyword TEXT
)
RETURNS TABLE (
    user_id BIGINT,
    username VARCHAR,
    nickname VARCHAR,
    phone VARCHAR,
    enabled BOOLEAN
)
LANGUAGE sql
STABLE
AS $$
    SELECT
        id,
        username,
        nickname,
        phone,
        enabled
    FROM app.sys_user
    WHERE deleted = FALSE
      AND (
          username ILIKE '%' || p_keyword || '%'
          OR nickname ILIKE '%' || p_keyword || '%'
      )
    ORDER BY id DESC;
$$;
```

调用：

```sql
SELECT *
FROM app.search_user('admin');
```

返回 JSONB：

```sql
CREATE OR REPLACE FUNCTION app.get_order_detail_json(
    p_order_no VARCHAR
)
RETURNS JSONB
LANGUAGE sql
STABLE
AS $$
    SELECT jsonb_build_object(
        'orderNo', o.order_no,
        'status', o.order_status,
        'amount', o.total_amount,
        'items',
        COALESCE(
            jsonb_agg(
                jsonb_build_object(
                    'productName', oi.product_name,
                    'quantity', oi.quantity,
                    'salePrice', oi.sale_price
                )
                ORDER BY oi.id
            ) FILTER (WHERE oi.id IS NOT NULL),
            '[]'::jsonb
        )
    )
    FROM app.order_info o
    LEFT JOIN app.order_item oi ON oi.order_id = o.id
    WHERE o.order_no = p_order_no
    GROUP BY
        o.order_no,
        o.order_status,
        o.total_amount;
$$;
```

调用：

```sql
SELECT app.get_order_detail_json('PO202605090001') AS order_detail;
```

返回值设计建议：

| 场景         | 推荐返回类型                 |
| ------------ | ---------------------------- |
| 数量统计     | `BIGINT`                     |
| 金额统计     | `NUMERIC`                    |
| 是否存在     | `BOOLEAN`                    |
| 查询列表     | `RETURNS TABLE (...)`        |
| 动态结构     | `JSONB`                      |
| 执行结果     | `BOOLEAN` 或结果码           |
| 多个固定字段 | `OUT` 参数或 `RETURNS TABLE` |

函数返回值应尽量稳定。对应用层调用的函数，避免频繁调整返回字段名称和类型。

### 异常处理

PL/pgSQL 支持 `EXCEPTION` 异常处理。异常处理可用于捕获唯一约束冲突、无数据、非法状态等情况。但不要滥用异常作为正常业务流程控制，否则会降低可读性和性能。

捕获唯一约束冲突：

```sql
CREATE OR REPLACE FUNCTION app.create_config_if_absent(
    p_config_key VARCHAR,
    p_config_value TEXT
)
RETURNS BOOLEAN
LANGUAGE plpgsql
AS $$
BEGIN
    INSERT INTO app.sys_config (
        config_key,
        config_value,
        create_time,
        update_time
    ) VALUES (
        p_config_key,
        p_config_value,
        CURRENT_TIMESTAMP,
        CURRENT_TIMESTAMP
    );

    RETURN TRUE;

EXCEPTION
    WHEN unique_violation THEN
        RETURN FALSE;
END;
$$;
```

调用：

```sql
SELECT app.create_config_if_absent('system.name', '后台系统') AS created;
```

抛出自定义异常：

```sql
CREATE OR REPLACE FUNCTION app.pay_order(
    p_order_id BIGINT
)
RETURNS BOOLEAN
LANGUAGE plpgsql
AS $$
DECLARE
    v_order_status VARCHAR;
BEGIN
    SELECT order_status
    INTO v_order_status
    FROM app.order_info
    WHERE id = p_order_id
    FOR UPDATE;

    IF NOT FOUND THEN
        RAISE EXCEPTION '订单不存在，order_id=%', p_order_id;
    END IF;

    IF v_order_status <> 'PENDING' THEN
        RAISE EXCEPTION '订单状态不允许支付，order_id=%, status=%', p_order_id, v_order_status;
    END IF;

    UPDATE app.order_info
    SET
        order_status = 'PAID',
        update_time = CURRENT_TIMESTAMP
    WHERE id = p_order_id;

    RETURN TRUE;
END;
$$;
```

记录通知信息：

```sql
CREATE OR REPLACE FUNCTION app.demo_raise_message()
RETURNS VOID
LANGUAGE plpgsql
AS $$
BEGIN
    RAISE NOTICE '这是一条 NOTICE 信息';
    RAISE WARNING '这是一条 WARNING 信息';
END;
$$;
```

常见异常类型：

| 异常                    | 说明                     |
| ----------------------- | ------------------------ |
| `unique_violation`      | 唯一约束冲突             |
| `foreign_key_violation` | 外键约束冲突             |
| `not_null_violation`    | 非空约束冲突             |
| `check_violation`       | 检查约束冲突             |
| `division_by_zero`      | 除零错误                 |
| `no_data_found`         | 未找到数据，特定场景使用 |
| `too_many_rows`         | 查询返回多行但期望单行   |

异常处理建议：

| 建议                             | 说明                       |
| -------------------------------- | -------------------------- |
| 优先用条件判断处理正常分支       | 不要把异常当作普通流程     |
| 异常信息包含关键参数             | 便于排查                   |
| 不吞掉关键异常                   | 重要错误应抛出或记录       |
| 应用层也要处理异常               | 数据库异常最终会传递到应用 |
| 核心业务不建议全部写在函数异常中 | 可测试性和可观测性较差     |

### 函数权限

函数和存储过程也是数据库对象，需要控制执行权限。默认情况下，函数可能对 `PUBLIC` 具有执行权限，生产环境中应根据安全要求回收并按角色授权。

创建函数后回收公共执行权限：

```sql
REVOKE EXECUTE ON FUNCTION app.get_user_status_text(BIGINT) FROM PUBLIC;
```

授权给指定角色：

```sql
GRANT EXECUTE ON FUNCTION app.get_user_status_text(BIGINT) TO app_rw;
GRANT EXECUTE ON FUNCTION app.get_user_status_text(BIGINT) TO app_ro;
```

授权存储过程：

```sql
GRANT EXECUTE ON PROCEDURE app.refresh_daily_order_stat() TO app_owner;
```

为 Schema 下所有函数授权：

```sql
GRANT EXECUTE ON ALL FUNCTIONS IN SCHEMA app TO app_rw;
```

设置未来函数默认权限：

```sql
ALTER DEFAULT PRIVILEGES FOR ROLE app_owner IN SCHEMA app
GRANT EXECUTE ON FUNCTIONS TO app_rw;
```

查看函数权限：

```sql
SELECT
    n.nspname AS schema_name,
    p.proname AS function_name,
    pg_get_function_identity_arguments(p.oid) AS arguments,
    p.proacl AS privileges
FROM pg_proc p
JOIN pg_namespace n ON n.oid = p.pronamespace
WHERE n.nspname = 'app'
ORDER BY p.proname;
```

函数权限建议：

| 建议                     | 说明                                   |
| ------------------------ | -------------------------------------- |
| 回收不必要的 PUBLIC 权限 | 避免所有用户都可执行                   |
| 按角色授权               | 不直接给个人账号授权                   |
| 敏感函数单独授权         | 如数据修复、归档、刷新统计             |
| 默认权限要配置           | 确保未来函数自动授权                   |
| 注意 SECURITY DEFINER    | 该模式会以函数所有者权限执行，必须谨慎 |

`SECURITY DEFINER` 函数可以让调用者以函数所有者权限执行逻辑，适合封装受控操作，但存在权限放大风险。使用时必须固定 `search_path` 并严格限制函数内容。

示例：

```sql
CREATE OR REPLACE FUNCTION app.safe_count_user()
RETURNS BIGINT
LANGUAGE sql
SECURITY DEFINER
SET search_path = app, pg_temp
AS $$
    SELECT COUNT(*)
    FROM sys_user
    WHERE deleted = FALSE;
$$;
```

## 触发器

触发器用于在表发生 `INSERT`、`UPDATE`、`DELETE` 等事件时自动执行函数。PostgreSQL 的触发器常用于自动维护审计字段、记录数据变更日志、校验复杂约束、同步派生数据等。项目中应谨慎使用触发器，因为触发器是隐式执行逻辑，过多会增加排查难度。

### 触发器使用场景

触发器适合数据库层必须兜底执行的逻辑，尤其是与数据一致性和审计相关的逻辑。它不适合承载复杂业务流程。

常见适用场景：

| 场景             | 说明                              |
| ---------------- | --------------------------------- |
| 自动维护更新时间 | `UPDATE` 时自动更新 `update_time` |
| 自动写审计日志   | 记录新增、修改、删除前后的数据    |
| 数据变更同步     | 简单同步派生表或统计表            |
| 复杂约束校验     | 普通 `CHECK` 无法表达的规则       |
| 阻止非法操作     | 删除前检查业务条件                |
| 记录操作痕迹     | 记录操作类型、时间、旧值、新值    |

不建议使用触发器的场景：

| 场景               | 原因                         |
| ------------------ | ---------------------------- |
| 复杂业务流程       | 隐式逻辑难以调试和测试       |
| 调用外部接口       | 数据库触发器不适合外部依赖   |
| 发送 MQ 消息       | 失败处理和事务边界复杂       |
| 大量统计实时维护   | 高并发下可能拖慢写入         |
| 替代应用层权限校验 | 权限上下文通常在应用层更完整 |

触发器基本结构：

```sql
-- 1. 创建触发器函数
CREATE OR REPLACE FUNCTION app.trigger_function_name()
RETURNS TRIGGER
LANGUAGE plpgsql
AS $$
BEGIN
    -- 触发器逻辑
    RETURN NEW;
END;
$$;

-- 2. 创建触发器
CREATE TRIGGER trigger_name
BEFORE INSERT OR UPDATE
ON app.table_name
FOR EACH ROW
EXECUTE FUNCTION app.trigger_function_name();
```

触发器函数中常用特殊变量：

| 变量              | 说明                                      |
| ----------------- | ----------------------------------------- |
| `NEW`             | 新行数据，适用于 `INSERT`、`UPDATE`       |
| `OLD`             | 旧行数据，适用于 `UPDATE`、`DELETE`       |
| `TG_OP`           | 操作类型，如 `INSERT`、`UPDATE`、`DELETE` |
| `TG_TABLE_SCHEMA` | 表所在 Schema                             |
| `TG_TABLE_NAME`   | 表名                                      |
| `TG_NAME`         | 触发器名称                                |
| `TG_WHEN`         | 触发时机，如 `BEFORE`、`AFTER`            |

### BEFORE 触发器

`BEFORE` 触发器在数据真正写入表之前执行，适合修改即将写入的数据、校验数据、阻止非法操作等。

自动设置创建时间和更新时间：

```sql
CREATE OR REPLACE FUNCTION app.fill_audit_time_before()
RETURNS TRIGGER
LANGUAGE plpgsql
AS $$
BEGIN
    IF TG_OP = 'INSERT' THEN
        NEW.create_time = COALESCE(NEW.create_time, CURRENT_TIMESTAMP);
        NEW.update_time = COALESCE(NEW.update_time, CURRENT_TIMESTAMP);
    ELSIF TG_OP = 'UPDATE' THEN
        NEW.update_time = CURRENT_TIMESTAMP;
    END IF;

    RETURN NEW;
END;
$$;
```

创建触发器：

```sql
CREATE TRIGGER trg_sys_user_fill_audit_time
BEFORE INSERT OR UPDATE
ON app.sys_user
FOR EACH ROW
EXECUTE FUNCTION app.fill_audit_time_before();
```

阻止非法金额写入：

```sql
CREATE OR REPLACE FUNCTION app.check_order_amount_before()
RETURNS TRIGGER
LANGUAGE plpgsql
AS $$
BEGIN
    IF NEW.total_amount < 0 THEN
        RAISE EXCEPTION '订单金额不能小于 0，order_no=%', NEW.order_no;
    END IF;

    RETURN NEW;
END;
$$;
```

绑定触发器：

```sql
CREATE TRIGGER trg_order_info_check_amount
BEFORE INSERT OR UPDATE
ON app.order_info
FOR EACH ROW
EXECUTE FUNCTION app.check_order_amount_before();
```

`BEFORE` 触发器使用建议：

| 建议             | 说明                              |
| ---------------- | --------------------------------- |
| 适合修改 NEW     | 自动填充字段、规范化数据          |
| 适合阻止非法数据 | 使用 `RAISE EXCEPTION`            |
| 逻辑要短小       | 避免写复杂业务                    |
| 注意返回值       | 行级触发器通常返回 `NEW` 或 `OLD` |
| 避免递归更新同表 | 容易导致触发器递归或复杂锁问题    |

### AFTER 触发器

`AFTER` 触发器在数据写入完成后执行，适合记录日志、同步变更、写审计表等场景。此时数据已经完成变更，触发器中通常不修改 `NEW`，而是使用 `NEW` 和 `OLD` 记录信息。

创建审计日志表：

```sql
CREATE TABLE app.data_change_log (
    id BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    table_schema VARCHAR(64) NOT NULL,
    table_name VARCHAR(128) NOT NULL,
    operation_type VARCHAR(16) NOT NULL,
    old_data JSONB,
    new_data JSONB,
    change_time TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);
```

创建通用变更日志函数：

```sql
CREATE OR REPLACE FUNCTION app.write_data_change_log_after()
RETURNS TRIGGER
LANGUAGE plpgsql
AS $$
BEGIN
    INSERT INTO app.data_change_log (
        table_schema,
        table_name,
        operation_type,
        old_data,
        new_data,
        change_time
    ) VALUES (
        TG_TABLE_SCHEMA,
        TG_TABLE_NAME,
        TG_OP,
        CASE WHEN TG_OP IN ('UPDATE', 'DELETE') THEN to_jsonb(OLD) ELSE NULL END,
        CASE WHEN TG_OP IN ('INSERT', 'UPDATE') THEN to_jsonb(NEW) ELSE NULL END,
        CURRENT_TIMESTAMP
    );

    IF TG_OP = 'DELETE' THEN
        RETURN OLD;
    ELSE
        RETURN NEW;
    END IF;
END;
$$;
```

绑定到用户表：

```sql
CREATE TRIGGER trg_sys_user_change_log
AFTER INSERT OR UPDATE OR DELETE
ON app.sys_user
FOR EACH ROW
EXECUTE FUNCTION app.write_data_change_log_after();
```

`AFTER` 触发器适合场景：

| 场景       | 说明                               |
| ---------- | ---------------------------------- |
| 写变更日志 | 使用 `OLD` 和 `NEW` 记录前后数据   |
| 同步派生表 | 数据写入后同步轻量派生数据         |
| 审计记录   | 记录操作类型、表名和时间           |
| 异步补偿表 | 写入本地 outbox 表，由应用异步处理 |

`AFTER` 触发器不适合修改当前行本身。如果要修改即将写入的当前行，应使用 `BEFORE` 触发器。

### INSERT 触发器

`INSERT` 触发器在插入数据时触发。常用于补充默认字段、生成业务编码、记录新增日志等。

插入时自动生成业务编号：

```sql
CREATE OR REPLACE FUNCTION app.fill_order_no_before_insert()
RETURNS TRIGGER
LANGUAGE plpgsql
AS $$
BEGIN
    IF NEW.order_no IS NULL OR length(trim(NEW.order_no)) = 0 THEN
        NEW.order_no = 'PO' || to_char(CURRENT_TIMESTAMP, 'YYYYMMDDHH24MISSMS');
    END IF;

    RETURN NEW;
END;
$$;
```

绑定 INSERT 触发器：

```sql
CREATE TRIGGER trg_order_info_fill_order_no
BEFORE INSERT
ON app.order_info
FOR EACH ROW
EXECUTE FUNCTION app.fill_order_no_before_insert();
```

插入时记录新增日志：

```sql
CREATE OR REPLACE FUNCTION app.log_insert_after()
RETURNS TRIGGER
LANGUAGE plpgsql
AS $$
BEGIN
    INSERT INTO app.data_change_log (
        table_schema,
        table_name,
        operation_type,
        old_data,
        new_data,
        change_time
    ) VALUES (
        TG_TABLE_SCHEMA,
        TG_TABLE_NAME,
        'INSERT',
        NULL,
        to_jsonb(NEW),
        CURRENT_TIMESTAMP
    );

    RETURN NEW;
END;
$$;
```

绑定触发器：

```sql
CREATE TRIGGER trg_order_info_insert_log
AFTER INSERT
ON app.order_info
FOR EACH ROW
EXECUTE FUNCTION app.log_insert_after();
```

INSERT 触发器建议：

| 建议                           | 说明                                             |
| ------------------------------ | ------------------------------------------------ |
| 默认值优先用 DEFAULT           | 简单默认值不需要触发器                           |
| 复杂生成逻辑可用 BEFORE INSERT | 如业务编码、派生字段                             |
| 日志记录用 AFTER INSERT        | 确保数据已经写入                                 |
| 避免生成不可靠编码             | 高并发下时间戳编码可能冲突，应配合序列或唯一约束 |
| 不要隐藏复杂业务               | 插入副作用要有文档说明                           |

### UPDATE 触发器

`UPDATE` 触发器在更新数据时触发。常用于自动维护 `update_time`、记录变更前后数据、限制状态流转等。

自动更新时间：

```sql
CREATE OR REPLACE FUNCTION app.fill_update_time_before_update()
RETURNS TRIGGER
LANGUAGE plpgsql
AS $$
BEGIN
    NEW.update_time = CURRENT_TIMESTAMP;
    RETURN NEW;
END;
$$;
```

绑定触发器：

```sql
CREATE TRIGGER trg_order_info_update_time
BEFORE UPDATE
ON app.order_info
FOR EACH ROW
EXECUTE FUNCTION app.fill_update_time_before_update();
```

仅当数据实际变化时更新时间：

```sql
CREATE OR REPLACE FUNCTION app.fill_update_time_when_changed()
RETURNS TRIGGER
LANGUAGE plpgsql
AS $$
BEGIN
    IF to_jsonb(NEW) IS DISTINCT FROM to_jsonb(OLD) THEN
        NEW.update_time = CURRENT_TIMESTAMP;
    END IF;

    RETURN NEW;
END;
$$;
```

限制订单状态不能从已完成改回待支付：

```sql
CREATE OR REPLACE FUNCTION app.check_order_status_before_update()
RETURNS TRIGGER
LANGUAGE plpgsql
AS $$
BEGIN
    IF OLD.order_status = 'FINISHED'
       AND NEW.order_status <> 'FINISHED' THEN
        RAISE EXCEPTION '已完成订单不允许修改状态，order_no=%', OLD.order_no;
    END IF;

    RETURN NEW;
END;
$$;
```

绑定触发器：

```sql
CREATE TRIGGER trg_order_info_check_status
BEFORE UPDATE OF order_status
ON app.order_info
FOR EACH ROW
EXECUTE FUNCTION app.check_order_status_before_update();
```

记录更新前后数据：

```sql
CREATE OR REPLACE FUNCTION app.log_update_after()
RETURNS TRIGGER
LANGUAGE plpgsql
AS $$
BEGIN
    INSERT INTO app.data_change_log (
        table_schema,
        table_name,
        operation_type,
        old_data,
        new_data,
        change_time
    ) VALUES (
        TG_TABLE_SCHEMA,
        TG_TABLE_NAME,
        'UPDATE',
        to_jsonb(OLD),
        to_jsonb(NEW),
        CURRENT_TIMESTAMP
    );

    RETURN NEW;
END;
$$;
```

UPDATE 触发器建议：

| 建议                       | 说明                           |
| -------------------------- | ------------------------------ |
| 更新时间适合 BEFORE UPDATE | 直接修改 `NEW.update_time`     |
| 变更日志适合 AFTER UPDATE  | 记录变更完成后的结果           |
| 可以限定字段触发           | 使用 `BEFORE UPDATE OF column` |
| 避免触发器内再更新同表     | 容易递归或造成锁复杂           |
| 高频表谨慎记录全量 JSON    | 日志体积会快速增长             |

### DELETE 触发器

`DELETE` 触发器在删除数据时触发。常用于阻止删除、删除前归档、记录删除日志等。由于物理删除不可恢复，生产系统中核心业务表通常优先使用逻辑删除。

删除前阻止核心配置删除：

```sql
CREATE OR REPLACE FUNCTION app.prevent_core_config_delete()
RETURNS TRIGGER
LANGUAGE plpgsql
AS $$
BEGIN
    IF OLD.config_key LIKE 'system.%' THEN
        RAISE EXCEPTION '系统核心配置不允许删除，config_key=%', OLD.config_key;
    END IF;

    RETURN OLD;
END;
$$;
```

绑定触发器：

```sql
CREATE TRIGGER trg_sys_config_prevent_delete
BEFORE DELETE
ON app.sys_config
FOR EACH ROW
EXECUTE FUNCTION app.prevent_core_config_delete();
```

删除后记录日志：

```sql
CREATE OR REPLACE FUNCTION app.log_delete_after()
RETURNS TRIGGER
LANGUAGE plpgsql
AS $$
BEGIN
    INSERT INTO app.data_change_log (
        table_schema,
        table_name,
        operation_type,
        old_data,
        new_data,
        change_time
    ) VALUES (
        TG_TABLE_SCHEMA,
        TG_TABLE_NAME,
        'DELETE',
        to_jsonb(OLD),
        NULL,
        CURRENT_TIMESTAMP
    );

    RETURN OLD;
END;
$$;
```

绑定触发器：

```sql
CREATE TRIGGER trg_sys_config_delete_log
AFTER DELETE
ON app.sys_config
FOR EACH ROW
EXECUTE FUNCTION app.log_delete_after();
```

删除前归档：

```sql
CREATE OR REPLACE FUNCTION app.archive_user_before_delete()
RETURNS TRIGGER
LANGUAGE plpgsql
AS $$
BEGIN
    INSERT INTO app.sys_user_archive (
        user_id,
        username,
        nickname,
        archive_data,
        archive_time
    ) VALUES (
        OLD.id,
        OLD.username,
        OLD.nickname,
        to_jsonb(OLD),
        CURRENT_TIMESTAMP
    );

    RETURN OLD;
END;
$$;
```

DELETE 触发器建议：

| 建议                     | 说明                   |
| ------------------------ | ---------------------- |
| 核心业务优先逻辑删除     | 减少误删风险           |
| 物理删除前可归档         | 保存旧数据快照         |
| 删除日志使用 OLD         | 删除时没有 `NEW`       |
| 阻止删除用 BEFORE DELETE | 使用 `RAISE EXCEPTION` |
| 大批量删除谨慎触发日志   | 可能产生大量审计数据   |

### 审计字段自动维护

审计字段通常包括 `create_time`、`update_time`、`create_user_id`、`update_user_id`、`version` 等。简单项目中，可以由应用层统一维护；如果希望数据库兜底维护时间字段，可以使用触发器。

通用审计字段表结构示例：

```sql
CREATE TABLE app.audit_demo (
    id BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    business_name VARCHAR(128) NOT NULL,
    enabled BOOLEAN NOT NULL DEFAULT TRUE,
    version INTEGER NOT NULL DEFAULT 0,
    create_time TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_time TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);
```

通用时间维护触发器函数：

```sql
CREATE OR REPLACE FUNCTION app.fill_audit_columns()
RETURNS TRIGGER
LANGUAGE plpgsql
AS $$
BEGIN
    IF TG_OP = 'INSERT' THEN
        NEW.create_time = COALESCE(NEW.create_time, CURRENT_TIMESTAMP);
        NEW.update_time = COALESCE(NEW.update_time, CURRENT_TIMESTAMP);
        NEW.version = COALESCE(NEW.version, 0);
    ELSIF TG_OP = 'UPDATE' THEN
        NEW.update_time = CURRENT_TIMESTAMP;
        NEW.version = COALESCE(OLD.version, 0) + 1;
    END IF;

    RETURN NEW;
END;
$$;
```

绑定到表：

```sql
CREATE TRIGGER trg_audit_demo_fill_columns
BEFORE INSERT OR UPDATE
ON app.audit_demo
FOR EACH ROW
EXECUTE FUNCTION app.fill_audit_columns();
```

测试插入：

```sql
INSERT INTO app.audit_demo (
    business_name
) VALUES (
    '测试数据'
)
RETURNING
    id,
    business_name,
    version,
    create_time,
    update_time;
```

测试更新：

```sql
UPDATE app.audit_demo
SET business_name = '测试数据更新'
WHERE id = 1
RETURNING
    id,
    business_name,
    version,
    create_time,
    update_time;
```

审计字段维护建议：

| 建议                       | 说明                                    |
| -------------------------- | --------------------------------------- |
| 时间字段可由数据库兜底     | `create_time`、`update_time` 适合触发器 |
| 操作人字段通常由应用层维护 | 数据库无法天然知道当前登录用户业务 ID   |
| 版本号可由触发器维护       | 但应用层乐观锁仍需检查版本              |
| 所有表统一规范             | 避免部分表应用层维护，部分表触发器维护  |
| 文档明确副作用             | 开发人员要知道更新会自动修改字段        |

如果应用层使用 MyBatis-Plus、JPA 等框架自动填充审计字段，就不一定需要数据库触发器。二者同时维护时要避免冲突。

### 数据变更日志记录

数据变更日志用于记录表数据新增、修改、删除前后的快照，便于审计、追踪、回滚分析和问题排查。触发器可以实现数据库层统一记录，但会增加写入成本和日志表体积。

创建通用变更日志表：

```sql
CREATE TABLE app.data_change_log (
    id BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    table_schema VARCHAR(64) NOT NULL,
    table_name VARCHAR(128) NOT NULL,
    primary_key_value TEXT,
    operation_type VARCHAR(16) NOT NULL,
    old_data JSONB,
    new_data JSONB,
    change_time TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_data_change_log_table_time
ON app.data_change_log (table_schema, table_name, change_time DESC);

CREATE INDEX idx_data_change_log_operation_time
ON app.data_change_log (operation_type, change_time DESC);
```

创建通用日志函数：

```sql
CREATE OR REPLACE FUNCTION app.write_table_change_log()
RETURNS TRIGGER
LANGUAGE plpgsql
AS $$
DECLARE
    v_primary_key_value TEXT;
BEGIN
    IF TG_OP = 'INSERT' THEN
        v_primary_key_value = COALESCE(NEW.id::TEXT, NULL);
    ELSIF TG_OP = 'UPDATE' THEN
        v_primary_key_value = COALESCE(NEW.id::TEXT, OLD.id::TEXT, NULL);
    ELSE
        v_primary_key_value = COALESCE(OLD.id::TEXT, NULL);
    END IF;

    INSERT INTO app.data_change_log (
        table_schema,
        table_name,
        primary_key_value,
        operation_type,
        old_data,
        new_data,
        change_time
    ) VALUES (
        TG_TABLE_SCHEMA,
        TG_TABLE_NAME,
        v_primary_key_value,
        TG_OP,
        CASE WHEN TG_OP IN ('UPDATE', 'DELETE') THEN to_jsonb(OLD) ELSE NULL END,
        CASE WHEN TG_OP IN ('INSERT', 'UPDATE') THEN to_jsonb(NEW) ELSE NULL END,
        CURRENT_TIMESTAMP
    );

    IF TG_OP = 'DELETE' THEN
        RETURN OLD;
    ELSE
        RETURN NEW;
    END IF;
END;
$$;
```

绑定到订单表：

```sql
CREATE TRIGGER trg_order_info_change_log
AFTER INSERT OR UPDATE OR DELETE
ON app.order_info
FOR EACH ROW
EXECUTE FUNCTION app.write_table_change_log();
```

查询订单表变更历史：

```sql
SELECT
    id,
    table_name,
    primary_key_value,
    operation_type,
    old_data,
    new_data,
    change_time
FROM app.data_change_log
WHERE table_schema = 'app'
  AND table_name = 'order_info'
  AND primary_key_value = '1'
ORDER BY change_time DESC;
```

只记录关键字段变化，可以在触发器中判断字段是否变化：

```sql
CREATE OR REPLACE FUNCTION app.write_order_status_change_log()
RETURNS TRIGGER
LANGUAGE plpgsql
AS $$
BEGIN
    IF OLD.order_status IS DISTINCT FROM NEW.order_status THEN
        INSERT INTO app.data_change_log (
            table_schema,
            table_name,
            primary_key_value,
            operation_type,
            old_data,
            new_data,
            change_time
        ) VALUES (
            TG_TABLE_SCHEMA,
            TG_TABLE_NAME,
            NEW.id::TEXT,
            'UPDATE_STATUS',
            jsonb_build_object('orderStatus', OLD.order_status),
            jsonb_build_object('orderStatus', NEW.order_status),
            CURRENT_TIMESTAMP
        );
    END IF;

    RETURN NEW;
END;
$$;
```

绑定字段级 UPDATE 触发器：

```sql
CREATE TRIGGER trg_order_info_status_change_log
AFTER UPDATE OF order_status
ON app.order_info
FOR EACH ROW
EXECUTE FUNCTION app.write_order_status_change_log();
```

变更日志建议：

| 建议               | 说明                             |
| ------------------ | -------------------------------- |
| 高频表谨慎全量记录 | 日志体积会非常大                 |
| 优先记录关键字段   | 状态、金额、权限、配置等         |
| 日志表要建索引     | 按表名、主键、时间查询           |
| 日志表要定期归档   | 避免无限增长                     |
| 敏感字段要脱敏     | 密码、密钥、身份证等不要明文记录 |
| 明确保留周期       | 按合规和业务要求清理             |

### 触发器性能影响

触发器会在数据变更时自动执行，因此会直接增加 `INSERT`、`UPDATE`、`DELETE` 的执行成本。触发器越复杂、涉及表越多、写入日志越大，对性能影响越明显。

常见性能影响：

| 影响          | 说明                               |
| ------------- | ---------------------------------- |
| 增加写入耗时  | 每行写入都要执行触发器函数         |
| 增加锁时间    | 触发器执行时间也在事务内           |
| 增加 WAL 日志 | 触发器写日志表会产生额外 WAL       |
| 增加表膨胀    | 高频日志写入和更新会增加维护成本   |
| 增加排查难度  | SQL 表面简单，实际隐式执行更多逻辑 |
| 可能递归触发  | 触发器中更新同表容易造成递归       |

查看表触发器：

```sql
SELECT
    event_object_schema,
    event_object_table,
    trigger_name,
    event_manipulation,
    action_timing,
    action_statement
FROM information_schema.triggers
WHERE event_object_schema = 'app'
ORDER BY event_object_table, trigger_name;
```

查看触发器定义：

```sql
SELECT
    tgname AS trigger_name,
    tgrelid::regclass AS table_name,
    pg_get_triggerdef(oid) AS trigger_definition
FROM pg_trigger
WHERE NOT tgisinternal
ORDER BY table_name, trigger_name;
```

临时禁用触发器：

```sql
ALTER TABLE app.order_info DISABLE TRIGGER trg_order_info_change_log;
```

启用触发器：

```sql
ALTER TABLE app.order_info ENABLE TRIGGER trg_order_info_change_log;
```

禁用表上所有用户触发器，谨慎使用：

```sql
ALTER TABLE app.order_info DISABLE TRIGGER USER;
```

重新启用：

```sql
ALTER TABLE app.order_info ENABLE TRIGGER USER;
```

触发器性能优化建议：

| 建议                   | 说明                            |
| ---------------------- | ------------------------------- |
| 触发器逻辑要短         | 只做必要操作                    |
| 避免复杂查询           | 不在触发器中做大表扫描          |
| 避免远程操作           | 触发器内不调用外部接口          |
| 高频表少用全量日志     | 改为关键字段日志或应用层日志    |
| 使用字段级触发         | `UPDATE OF column` 降低触发频率 |
| 合理建日志索引         | 但不要给日志表建过多索引        |
| 大批量导入前评估触发器 | 必要时临时禁用并补偿处理        |
| 避免递归触发           | 不要在触发器中随意更新当前表    |

触发器设计规范：

| 规范           | 说明                                      |
| -------------- | ----------------------------------------- |
| 命名清晰       | 如 `trg_表名_动作_用途`                   |
| 函数复用       | 通用审计函数可被多表复用                  |
| 明确触发时机   | `BEFORE` 用于修改或校验，`AFTER` 用于日志 |
| 明确触发行级别 | 普通审计多用 `FOR EACH ROW`               |
| 纳入版本管理   | 触发器函数和触发器 DDL 应进入迁移脚本     |
| 文档说明副作用 | 告知开发者写表时会自动触发哪些逻辑        |

触发器是强大的数据库自动化机制，但也容易造成隐式副作用。普通项目中建议只把审计字段维护、关键变更日志、必要的数据兜底规则放入触发器；复杂业务流程、外部调用、消息发送和跨系统逻辑应放在应用层处理。


## 序列与自增主键

序列用于生成递增数字，常见于自增主键、业务流水号、批次号等场景。PostgreSQL 支持传统的 `SERIAL` 类型，也支持更符合 SQL 标准的 `IDENTITY` 自增列。新项目中建议优先使用 `IDENTITY`，旧项目或兼容场景仍可能看到 `SERIAL`。

### SERIAL 类型

`SERIAL` 是 PostgreSQL 早期常用的自增写法，它不是一个真正的数据类型，而是建表时的语法简写。使用 `SERIAL` 时，PostgreSQL 会自动创建一个序列，并将字段默认值设置为 `nextval(...)`。

使用 `SERIAL` 创建自增主键：

```sql
CREATE TABLE app.serial_demo (
    id SERIAL PRIMARY KEY,
    business_name VARCHAR(128) NOT NULL,
    create_time TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);
```

等价逻辑可以理解为：

```sql
-- 创建序列
CREATE SEQUENCE app.serial_demo_id_seq;

-- 创建表，字段默认从序列取值
CREATE TABLE app.serial_demo_manual (
    id INTEGER NOT NULL DEFAULT nextval('app.serial_demo_id_seq'),
    business_name VARCHAR(128) NOT NULL,
    create_time TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT pk_serial_demo_manual PRIMARY KEY (id)
);
```

常见 `SERIAL` 类型如下：

| 类型          | 对应整数类型 | 说明                     |
| ------------- | ------------ | ------------------------ |
| `SMALLSERIAL` | `SMALLINT`   | 小范围自增，较少使用     |
| `SERIAL`      | `INTEGER`    | 普通自增，最大约 21 亿   |
| `BIGSERIAL`   | `BIGINT`     | 大范围自增，历史项目常用 |

插入数据时不需要指定自增字段：

```sql
INSERT INTO app.serial_demo (
    business_name
) VALUES (
    'SERIAL 示例数据'
)
RETURNING id, business_name;
```

查看 `SERIAL` 字段默认值：

```sql
SELECT
    column_name,
    column_default
FROM information_schema.columns
WHERE table_schema = 'app'
  AND table_name = 'serial_demo'
ORDER BY ordinal_position;
```

`SERIAL` 使用注意事项：

| 注意点                     | 说明                           |
| -------------------------- | ------------------------------ |
| 不是标准 SQL 类型          | 它是 PostgreSQL 的便捷语法     |
| 会隐式创建序列             | 序列对象独立存在               |
| 删除字段或表时注意序列依赖 | 旧对象迁移时要检查序列是否残留 |
| 新项目不优先推荐           | PostgreSQL 10+ 推荐 `IDENTITY` |
| 大表优先 `BIGSERIAL`       | 避免 `INTEGER` 主键范围不足    |

### IDENTITY 类型

`IDENTITY` 是 SQL 标准自增列写法，PostgreSQL 10 之后支持。新项目中推荐使用 `GENERATED ... AS IDENTITY` 替代 `SERIAL`。

使用 `IDENTITY` 创建自增主键：

```sql
CREATE TABLE app.identity_demo (
    id BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    business_name VARCHAR(128) NOT NULL,
    create_time TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);
```

插入数据：

```sql
INSERT INTO app.identity_demo (
    business_name
) VALUES (
    'IDENTITY 示例数据'
)
RETURNING id, business_name;
```

`IDENTITY` 有两种模式：

| 模式                               | 说明                           |
| ---------------------------------- | ------------------------------ |
| `GENERATED ALWAYS AS IDENTITY`     | 默认不允许手动指定自增列值     |
| `GENERATED BY DEFAULT AS IDENTITY` | 默认自动生成，但允许手动指定值 |

`GENERATED ALWAYS` 示例：

```sql
CREATE TABLE app.identity_always_demo (
    id BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    business_name VARCHAR(128) NOT NULL
);
```

如果需要强制插入指定 ID，可以使用 `OVERRIDING SYSTEM VALUE`：

```sql
INSERT INTO app.identity_always_demo (
    id,
    business_name
)
OVERRIDING SYSTEM VALUE
VALUES (
    10001,
    '手动指定 ID'
);
```

`GENERATED BY DEFAULT` 示例：

```sql
CREATE TABLE app.identity_default_demo (
    id BIGINT GENERATED BY DEFAULT AS IDENTITY PRIMARY KEY,
    business_name VARCHAR(128) NOT NULL
);
```

该模式允许手动插入 ID：

```sql
INSERT INTO app.identity_default_demo (
    id,
    business_name
) VALUES (
    20001,
    '允许手动指定 ID'
);
```

自定义序列参数：

```sql
CREATE TABLE app.identity_option_demo (
    id BIGINT GENERATED ALWAYS AS IDENTITY (
        START WITH 100000
        INCREMENT BY 1
        MINVALUE 100000
        CACHE 100
    ) PRIMARY KEY,
    business_name VARCHAR(128) NOT NULL
);
```

`SERIAL` 与 `IDENTITY` 对比：

| 对比项       | `SERIAL`            | `IDENTITY`                     |
| ------------ | ------------------- | ------------------------------ |
| 标准性       | PostgreSQL 便捷语法 | SQL 标准                       |
| 推荐程度     | 旧项目常见          | 新项目推荐                     |
| 序列管理     | 隐式创建序列        | 自增属性属于字段定义           |
| 手动插入控制 | 相对松散            | `ALWAYS` / `BY DEFAULT` 更清晰 |
| DDL 可读性   | 简单但隐式          | 更明确                         |

新表建议使用 `BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY`，除非项目已有统一规范使用 `BIGSERIAL`。

### 序列创建

序列是独立数据库对象，可以不依赖表单独创建。它适合用于生成业务流水号、批次号、任务编号等递增数字。

创建基础序列：

```sql
CREATE SEQUENCE app.order_no_seq
    START WITH 1
    INCREMENT BY 1
    MINVALUE 1
    NO MAXVALUE
    CACHE 100;
```

序列参数说明：

| 参数           | 说明                   |
| -------------- | ---------------------- |
| `START WITH`   | 起始值                 |
| `INCREMENT BY` | 每次递增步长           |
| `MINVALUE`     | 最小值                 |
| `MAXVALUE`     | 最大值                 |
| `CACHE`        | 缓存数量，提高生成性能 |
| `CYCLE`        | 达到最大值后是否循环   |
| `NO CYCLE`     | 达到最大值后不循环     |

创建带最大值的序列：

```sql
CREATE SEQUENCE app.batch_no_seq
    START WITH 1000
    INCREMENT BY 1
    MINVALUE 1000
    MAXVALUE 99999999
    CACHE 50
    NO CYCLE;
```

查看序列：

```sql
SELECT
    sequence_schema,
    sequence_name,
    data_type,
    start_value,
    minimum_value,
    maximum_value,
    increment
FROM information_schema.sequences
WHERE sequence_schema = 'app'
ORDER BY sequence_name;
```

查看序列当前定义：

```sql
SELECT
    schemaname,
    sequencename,
    start_value,
    min_value,
    max_value,
    increment_by,
    cycle,
    cache_size,
    last_value
FROM pg_sequences
WHERE schemaname = 'app'
ORDER BY sequencename;
```

修改序列参数：

```sql
ALTER SEQUENCE app.order_no_seq
    INCREMENT BY 1
    CACHE 200;
```

删除序列：

```sql
DROP SEQUENCE IF EXISTS app.order_no_seq;
```

序列创建建议：

| 建议                     | 说明                               |
| ------------------------ | ---------------------------------- |
| 序列放在业务 Schema 下   | 例如 `app.order_no_seq`            |
| 命名带业务含义           | 如 `order_no_seq`、`batch_no_seq`  |
| 高频序列设置缓存         | `CACHE` 可以减少序列持久化开销     |
| 不要求无间隙             | 序列可能因回滚、缓存、异常产生跳号 |
| 不用序列表达严格连续编号 | 发票号、凭证号等连续要求要单独设计 |

### 序列使用

序列常用函数包括 `nextval`、`currval`、`lastval` 和 `setval`。最常用的是 `nextval`，用于获取下一个序列值。

获取下一个序列值：

```sql
SELECT nextval('app.order_no_seq') AS next_value;
```

获取当前会话中最近一次取过的当前序列值：

```sql
SELECT currval('app.order_no_seq') AS current_value;
```

需要注意，`currval` 必须在当前会话中先调用过 `nextval`，否则会报错。

使用序列生成业务编号：

```sql
SELECT
    'PO' || to_char(CURRENT_DATE, 'YYYYMMDD')
    || lpad(nextval('app.order_no_seq')::text, 8, '0') AS order_no;
```

插入订单时生成订单号：

```sql
INSERT INTO app.order_info (
    order_no,
    user_id,
    order_status,
    total_amount
) VALUES (
    'PO' || to_char(CURRENT_DATE, 'YYYYMMDD')
    || lpad(nextval('app.order_no_seq')::text, 8, '0'),
    10001,
    'PENDING',
    199.90
)
RETURNING id, order_no;
```

在字段默认值中使用序列：

```sql
CREATE TABLE app.batch_task (
    id BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    batch_no BIGINT NOT NULL DEFAULT nextval('app.batch_no_seq'),
    task_name VARCHAR(128) NOT NULL,
    create_time TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uk_batch_task_batch_no UNIQUE (batch_no)
);
```

查询序列最近值：

```sql
SELECT
    last_value,
    is_called
FROM app.order_no_seq;
```

序列使用注意事项：

| 注意点                   | 说明                           |
| ------------------------ | ------------------------------ |
| `nextval` 会推进序列     | 即使事务回滚，序列值也不会回退 |
| 序列不保证连续           | 回滚、缓存、失败都会造成跳号   |
| 序列适合唯一递增         | 不适合严格无间隙编号           |
| 高频使用建议设置 `CACHE` | 提高性能，但异常时可能跳更多号 |
| 多节点共享数据库序列安全 | 序列在数据库层保证并发唯一     |

如果业务要求编号绝对连续，不能只依赖普通序列，需要设计独立的编号分配表、事务锁和异常补偿机制，同时接受并发性能下降。

### 序列重置

序列重置常用于测试环境清理数据、导入历史数据后校准序列值、修复序列与表中最大 ID 不一致的问题。生产环境重置序列需要谨慎，避免产生主键冲突。

重置序列到指定值：

```sql
SELECT setval('app.order_no_seq', 1, false);
```

参数说明：

| 参数       | 说明                                |
| ---------- | ----------------------------------- |
| 第一个参数 | 序列名称                            |
| 第二个参数 | 设置的目标值                        |
| 第三个参数 | `is_called`，表示该值是否已经被使用 |

`is_called = false` 时，下次 `nextval` 返回设置的值：

```sql
SELECT setval('app.order_no_seq', 1000, false);

SELECT nextval('app.order_no_seq') AS next_value;
```

结果会返回 `1000`。

`is_called = true` 时，下次 `nextval` 返回设置值加递增步长：

```sql
SELECT setval('app.order_no_seq', 1000, true);

SELECT nextval('app.order_no_seq') AS next_value;
```

结果通常返回 `1001`。

将表主键序列重置到当前最大 ID：

```sql
SELECT setval(
    pg_get_serial_sequence('app.serial_demo', 'id'),
    COALESCE((SELECT MAX(id) FROM app.serial_demo), 1),
    true
);
```

对于 `IDENTITY` 字段，也可以使用 `ALTER TABLE` 重启自增值：

```sql
ALTER TABLE app.identity_demo
ALTER COLUMN id RESTART WITH 1;
```

清空表并重置自增：

```sql
TRUNCATE TABLE app.identity_demo RESTART IDENTITY;
```

清空表但不重置自增：

```sql
TRUNCATE TABLE app.identity_demo CONTINUE IDENTITY;
```

序列重置建议：

| 场景                 | 建议                                 |
| -------------------- | ------------------------------------ |
| 测试环境重置数据     | 可以 `TRUNCATE ... RESTART IDENTITY` |
| 生产环境导入历史数据 | 导入后校准到 `MAX(id)`               |
| 主键冲突修复         | 先查询最大值，再 `setval`            |
| 有外键依赖表         | 谨慎 `TRUNCATE CASCADE`              |
| 高并发环境           | 不要在线随意重置序列                 |

### 自增主键设计

自增主键用于为表中每行数据生成唯一标识。PostgreSQL 中推荐使用 `BIGINT GENERATED ALWAYS AS IDENTITY` 作为普通业务表主键。

推荐主键设计：

```sql
CREATE TABLE app.sys_user (
    id BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    username VARCHAR(64) NOT NULL,
    nickname VARCHAR(64),
    enabled BOOLEAN NOT NULL DEFAULT TRUE,
    deleted BOOLEAN NOT NULL DEFAULT FALSE,
    create_time TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_time TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uk_sys_user_username UNIQUE (username)
);
```

订单表主键设计：

```sql
CREATE TABLE app.order_info (
    id BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    order_no VARCHAR(64) NOT NULL,
    user_id BIGINT NOT NULL,
    order_status VARCHAR(32) NOT NULL,
    total_amount NUMERIC(12, 2) NOT NULL DEFAULT 0,
    create_time TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_time TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uk_order_info_order_no UNIQUE (order_no)
);
```

主键和业务唯一字段应分开：

| 字段       | 作用                               |
| ---------- | ---------------------------------- |
| `id`       | 数据库内部主键，稳定、无业务含义   |
| `order_no` | 业务订单号，用于业务展示和外部交互 |
| `user_id`  | 用户表主键引用                     |
| `username` | 用户名，使用唯一约束保证唯一       |

不建议使用业务字段作为主键：

```sql
-- 不推荐：手机号可能变更，也可能涉及隐私和复用问题
CREATE TABLE app.bad_user_demo (
    phone VARCHAR(20) PRIMARY KEY,
    username VARCHAR(64) NOT NULL
);
```

推荐改为无业务含义主键：

```sql
CREATE TABLE app.good_user_demo (
    id BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    phone VARCHAR(20),
    username VARCHAR(64) NOT NULL,
    CONSTRAINT uk_good_user_demo_phone UNIQUE (phone)
);
```

自增主键设计建议：

| 建议                    | 说明                             |
| ----------------------- | -------------------------------- |
| 优先使用 `BIGINT`       | 避免 `INTEGER` 主键范围不足      |
| 优先使用 `IDENTITY`     | 新项目比 `SERIAL` 更推荐         |
| 主键无业务含义          | 不用手机号、邮箱、订单号当主键   |
| 业务唯一另加约束        | 如 `UNIQUE(order_no)`            |
| 对外暴露谨慎使用自增 ID | 可增加 UUID 或业务编号           |
| 分库分表提前规划        | 单库自增 ID 不适合所有分布式场景 |

### 分布式 ID 选择

分布式 ID 用于多节点、多服务、多库或未来可能分库分表的场景。PostgreSQL 自增主键适合单库单实例或集中式数据库生成 ID，但在分布式系统中可能需要雪花 ID、UUID、号段模式等方案。

常见 ID 方案对比：

| 方案              | 类型      | 优点                   | 注意事项             |
| ----------------- | --------- | ---------------------- | -------------------- |
| 数据库 `IDENTITY` | `BIGINT`  | 简单、递增、索引友好   | 依赖单库生成         |
| 数据库序列        | `BIGINT`  | 并发安全、可独立使用   | 仍依赖数据库         |
| 雪花 ID           | `BIGINT`  | 分布式生成、趋势递增   | 依赖时钟和机器号规划 |
| UUID v4           | `UUID`    | 全局唯一、无需中心服务 | 随机写入，索引较大   |
| UUID v7           | `UUID`    | 时间有序、全局唯一     | 需要生成端支持       |
| 号段模式          | `BIGINT`  | 性能高，中心分配压力小 | 实现复杂，需要防重复 |
| 业务编码          | `VARCHAR` | 可读性强               | 不建议作为主键       |

使用 UUID 字段：

```sql
CREATE EXTENSION IF NOT EXISTS pgcrypto;

CREATE TABLE app.file_info (
    id BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    file_uuid UUID NOT NULL DEFAULT gen_random_uuid(),
    file_name VARCHAR(255) NOT NULL,
    create_time TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uk_file_info_file_uuid UNIQUE (file_uuid)
);
```

使用雪花 ID 时，数据库字段通常定义为 `BIGINT`：

```sql
CREATE TABLE app.distributed_order (
    id BIGINT PRIMARY KEY,
    order_no VARCHAR(64) NOT NULL,
    user_id BIGINT NOT NULL,
    total_amount NUMERIC(12, 2) NOT NULL,
    create_time TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uk_distributed_order_order_no UNIQUE (order_no)
);
```

由应用层生成雪花 ID 后插入：

```sql
INSERT INTO app.distributed_order (
    id,
    order_no,
    user_id,
    total_amount
) VALUES (
    9007199254740991,
    'PO202605090001',
    10001,
    199.90
);
```

ID 选择建议：

| 系统类型            | 推荐方案                                   |
| ------------------- | ------------------------------------------ |
| 单体后台系统        | `BIGINT IDENTITY`                          |
| 普通微服务但单库    | `BIGINT IDENTITY` 或数据库序列             |
| 多实例分布式写入    | 雪花 ID                                    |
| 对外暴露不可猜 ID   | 内部 `BIGINT` + 外部 `UUID`                |
| 文件、令牌、事件 ID | `UUID`                                     |
| 未来分库分表        | 雪花 ID 或号段模式                         |
| 严格连续业务编号    | 独立编号表和事务控制，不用普通序列直接保证 |

实践中常用组合是：内部主键使用 `BIGINT`，对外展示使用 `order_no`、`file_uuid` 或业务编码。这样既保证数据库索引效率，又避免直接暴露自增主键。

## 分区表

分区表用于把一张逻辑表拆分成多个物理分区表。应用查询时仍访问父表，PostgreSQL 根据分区规则把数据写入和查询路由到对应分区。分区表适合超大表、按时间归档、按租户隔离、按状态或类型拆分等场景，但会增加 DDL 管理和运维复杂度。

### 分区表使用场景

分区表适合数据量大、生命周期明确、查询条件经常包含分区键的表。典型场景是日志表、订单表、事件表、审计表、流水表和时序类数据。

适合使用分区表的场景：

| 场景               | 说明                             |
| ------------------ | -------------------------------- |
| 大数据量时间表     | 操作日志、登录日志、事件日志     |
| 按时间归档         | 按月、按天删除历史分区           |
| 查询经常带时间条件 | 可触发分区裁剪                   |
| 租户数据量大       | 可按租户 ID 哈希或列表分区       |
| 历史数据清理频繁   | 删除分区比大批量 `DELETE` 更高效 |
| 单表索引过大       | 分区后每个分区维护较小索引       |

不适合使用分区表的场景：

| 场景           | 原因                         |
| -------------- | ---------------------------- |
| 表数据量较小   | 分区管理成本大于收益         |
| 查询不带分区键 | 仍可能扫描大量分区           |
| 分区键经常变化 | 更新分区键会导致跨分区移动   |
| 分区过多       | 规划、查询、维护成本上升     |
| 业务边界不清   | 容易造成后期分区规则变更困难 |

分区方式选择：

| 分区方式 | 适用场景                    |
| -------- | --------------------------- |
| 范围分区 | 按时间、ID 范围、金额区间   |
| 列表分区 | 按地区、状态、租户、类型    |
| 哈希分区 | 按用户 ID、租户 ID 均匀打散 |

分区表设计前必须先回答几个问题：表会增长到多大、主要查询条件是什么、数据如何清理、分区键是否稳定、分区数量如何控制。

### 范围分区

范围分区根据分区键的取值范围存储数据，最常见的是按时间分区，例如按月、按天、按年。范围分区适合日志、订单、流水、事件等持续增长数据。

创建按月范围分区的订单表：

```sql
CREATE TABLE app.order_info_partitioned (
    id BIGINT GENERATED ALWAYS AS IDENTITY,
    order_no VARCHAR(64) NOT NULL,
    user_id BIGINT NOT NULL,
    order_status VARCHAR(32) NOT NULL,
    total_amount NUMERIC(12, 2) NOT NULL DEFAULT 0,
    create_time TIMESTAMP NOT NULL,
    update_time TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (id, create_time)
) PARTITION BY RANGE (create_time);
```

创建 2026 年 5 月分区：

```sql
CREATE TABLE app.order_info_202605
PARTITION OF app.order_info_partitioned
FOR VALUES FROM ('2026-05-01 00:00:00') TO ('2026-06-01 00:00:00');
```

创建 2026 年 6 月分区：

```sql
CREATE TABLE app.order_info_202606
PARTITION OF app.order_info_partitioned
FOR VALUES FROM ('2026-06-01 00:00:00') TO ('2026-07-01 00:00:00');
```

插入数据时直接写父表：

```sql
INSERT INTO app.order_info_partitioned (
    order_no,
    user_id,
    order_status,
    total_amount,
    create_time
) VALUES (
    'PO202605090001',
    10001,
    'PAID',
    199.90,
    TIMESTAMP '2026-05-09 10:00:00'
);
```

查询 5 月订单：

```sql
SELECT
    id,
    order_no,
    user_id,
    total_amount,
    create_time
FROM app.order_info_partitioned
WHERE create_time >= TIMESTAMP '2026-05-01 00:00:00'
  AND create_time < TIMESTAMP '2026-06-01 00:00:00'
ORDER BY create_time DESC;
```

范围分区建议：

| 建议                 | 说明                                    |
| -------------------- | --------------------------------------- |
| 时间分区使用左闭右开 | `FROM ('2026-05-01') TO ('2026-06-01')` |
| 查询必须带时间条件   | 才能有效触发分区裁剪                    |
| 提前创建未来分区     | 避免新数据插入失败                      |
| 历史清理使用删除分区 | 比大批量 `DELETE` 更高效                |
| 分区粒度按数据量决定 | 常见按月、按周、按天                    |

### 列表分区

列表分区根据分区键的离散取值存储数据，适合按地区、租户、业务类型、状态等字段分区。列表分区要求取值集合相对稳定，否则后期维护成本较高。

按地区分区的客户表：

```sql
CREATE TABLE app.customer_partitioned (
    id BIGINT GENERATED ALWAYS AS IDENTITY,
    customer_name VARCHAR(128) NOT NULL,
    region_code VARCHAR(32) NOT NULL,
    phone VARCHAR(20),
    create_time TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (id, region_code)
) PARTITION BY LIST (region_code);
```

创建地区分区：

```sql
CREATE TABLE app.customer_region_east
PARTITION OF app.customer_partitioned
FOR VALUES IN ('EAST', 'SHANGHAI', 'ZHEJIANG', 'JIANGSU');

CREATE TABLE app.customer_region_south
PARTITION OF app.customer_partitioned
FOR VALUES IN ('SOUTH', 'GUANGDONG', 'FUJIAN');

CREATE TABLE app.customer_region_north
PARTITION OF app.customer_partitioned
FOR VALUES IN ('NORTH', 'BEIJING', 'TIANJIN', 'HEBEI');
```

创建默认分区，接收未匹配值：

```sql
CREATE TABLE app.customer_region_default
PARTITION OF app.customer_partitioned
DEFAULT;
```

插入数据：

```sql
INSERT INTO app.customer_partitioned (
    customer_name,
    region_code,
    phone
) VALUES (
    '测试客户',
    'ZHEJIANG',
    '13800000000'
);
```

查询指定区域：

```sql
SELECT
    id,
    customer_name,
    region_code,
    phone
FROM app.customer_partitioned
WHERE region_code = 'ZHEJIANG';
```

列表分区适合场景：

| 场景         | 说明                   |
| ------------ | ---------------------- |
| 地区分区     | 按省份、大区           |
| 租户分区     | 租户数量可控时         |
| 业务类型分区 | 类型稳定时             |
| 状态分区     | 状态生命周期清晰时     |
| 数据归属明确 | 每行只属于一个固定分类 |

列表分区建议：

| 建议             | 说明                           |
| ---------------- | ------------------------------ |
| 取值集合要稳定   | 频繁新增分区会增加维护成本     |
| 可创建默认分区   | 防止未知值插入失败             |
| 分区数量控制合理 | 不要为大量小租户创建大量分区   |
| 查询带分区键     | 例如 `WHERE region_code = ...` |
| 谨慎按状态分区   | 状态变更可能导致跨分区移动     |

### 哈希分区

哈希分区根据分区键的哈希值把数据均匀分散到多个分区中。它适合按用户 ID、租户 ID、设备 ID 等字段均匀分布数据，但不适合按时间快速清理历史数据。

按用户 ID 哈希分区的事件表：

```sql
CREATE TABLE app.user_event_partitioned (
    id BIGINT GENERATED ALWAYS AS IDENTITY,
    user_id BIGINT NOT NULL,
    event_type VARCHAR(64) NOT NULL,
    event_data JSONB NOT NULL DEFAULT '{}'::jsonb,
    create_time TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (id, user_id)
) PARTITION BY HASH (user_id);
```

创建 4 个哈希分区：

```sql
CREATE TABLE app.user_event_p0
PARTITION OF app.user_event_partitioned
FOR VALUES WITH (MODULUS 4, REMAINDER 0);

CREATE TABLE app.user_event_p1
PARTITION OF app.user_event_partitioned
FOR VALUES WITH (MODULUS 4, REMAINDER 1);

CREATE TABLE app.user_event_p2
PARTITION OF app.user_event_partitioned
FOR VALUES WITH (MODULUS 4, REMAINDER 2);

CREATE TABLE app.user_event_p3
PARTITION OF app.user_event_partitioned
FOR VALUES WITH (MODULUS 4, REMAINDER 3);
```

插入数据：

```sql
INSERT INTO app.user_event_partitioned (
    user_id,
    event_type,
    event_data
) VALUES (
    10001,
    'USER_LOGIN',
    '{"clientIp": "127.0.0.1", "success": true}'::jsonb
);
```

按用户查询：

```sql
SELECT
    id,
    user_id,
    event_type,
    event_data,
    create_time
FROM app.user_event_partitioned
WHERE user_id = 10001
ORDER BY create_time DESC;
```

哈希分区适合场景：

| 场景               | 说明                             |
| ------------------ | -------------------------------- |
| 按用户均匀分散     | 用户数据量较大                   |
| 按租户均匀分散     | 租户数据量较均衡                 |
| 避免单分区过热     | 通过哈希打散写入                 |
| 查询经常带分区键   | 如 `WHERE user_id = ...`         |
| 无明显时间清理需求 | 不能像时间分区一样直接删历史月份 |

哈希分区建议：

| 建议             | 说明                     |
| ---------------- | ------------------------ |
| 分区数量提前规划 | 后续调整分区数量成本较高 |
| 查询尽量带哈希键 | 才能定位到分区           |
| 不适合按时间归档 | 历史清理仍需扫描多个分区 |
| 分区数量不宜过多 | 过多分区增加规划成本     |
| 哈希键要稳定     | 不应频繁更新分区键       |

### 分区表创建

创建分区表时，需要先创建父表，再创建具体分区。父表定义字段和分区规则，分区表负责实际存储数据。

创建按月分区日志表：

```sql
CREATE TABLE app.operation_log_partitioned (
    id BIGINT GENERATED ALWAYS AS IDENTITY,
    operation_type VARCHAR(64) NOT NULL,
    operation_content TEXT,
    operator_id BIGINT,
    create_time TIMESTAMP NOT NULL,
    PRIMARY KEY (id, create_time)
) PARTITION BY RANGE (create_time);
```

创建分区：

```sql
CREATE TABLE app.operation_log_202605
PARTITION OF app.operation_log_partitioned
FOR VALUES FROM ('2026-05-01 00:00:00') TO ('2026-06-01 00:00:00');

CREATE TABLE app.operation_log_202606
PARTITION OF app.operation_log_partitioned
FOR VALUES FROM ('2026-06-01 00:00:00') TO ('2026-07-01 00:00:00');
```

创建默认分区：

```sql
CREATE TABLE app.operation_log_default
PARTITION OF app.operation_log_partitioned
DEFAULT;
```

插入测试数据：

```sql
INSERT INTO app.operation_log_partitioned (
    operation_type,
    operation_content,
    operator_id,
    create_time
) VALUES (
    'USER_LOGIN',
    '用户登录系统',
    10001,
    TIMESTAMP '2026-05-09 10:00:00'
);
```

查看分区表结构：

```sql
SELECT
    nmsp_parent.nspname AS parent_schema,
    parent.relname AS parent_table,
    nmsp_child.nspname AS child_schema,
    child.relname AS child_table
FROM pg_inherits
JOIN pg_class parent ON pg_inherits.inhparent = parent.oid
JOIN pg_class child ON pg_inherits.inhrelid = child.oid
JOIN pg_namespace nmsp_parent ON nmsp_parent.oid = parent.relnamespace
JOIN pg_namespace nmsp_child ON nmsp_child.oid = child.relnamespace
WHERE nmsp_parent.nspname = 'app'
  AND parent.relname = 'operation_log_partitioned'
ORDER BY child.relname;
```

分区表创建注意事项：

| 注意点               | 说明                                     |
| -------------------- | ---------------------------------------- |
| 主键通常要包含分区键 | 分区表上的唯一约束需要考虑分区键         |
| 分区键应稳定         | 更新分区键可能导致行移动                 |
| 应提前创建分区       | 没有匹配分区时插入会失败，除非有默认分区 |
| 分区名要规范         | 如 `表名_YYYYMM`                         |
| DDL 纳入版本管理     | 分区创建和删除应脚本化                   |

### 分区维护

分区维护包括创建未来分区、删除历史分区、分离分区、挂载分区、查看分区数据量和管理分区索引。分区表的价值很大一部分来自维护便利性。

创建未来分区：

```sql
CREATE TABLE app.operation_log_202607
PARTITION OF app.operation_log_partitioned
FOR VALUES FROM ('2026-07-01 00:00:00') TO ('2026-08-01 00:00:00');
```

删除历史分区：

```sql
DROP TABLE IF EXISTS app.operation_log_202501;
```

分离历史分区，保留为普通表：

```sql
ALTER TABLE app.operation_log_partitioned
DETACH PARTITION app.operation_log_202501;
```

分离后可以单独备份或归档：

```sql
-- 分离后的表已经不是父表分区，可作为普通表处理
SELECT COUNT(*)
FROM app.operation_log_202501;
```

挂载已有表为分区：

```sql
ALTER TABLE app.operation_log_partitioned
ATTACH PARTITION app.operation_log_202608
FOR VALUES FROM ('2026-08-01 00:00:00') TO ('2026-09-01 00:00:00');
```

查看每个分区数据量：

```sql
SELECT
    inhrelid::regclass AS partition_table,
    reltuples::BIGINT AS estimated_rows
FROM pg_inherits
JOIN pg_class ON pg_class.oid = pg_inherits.inhrelid
WHERE inhparent = 'app.operation_log_partitioned'::regclass
ORDER BY partition_table::text;
```

查看分区大小：

```sql
SELECT
    child.relname AS partition_table,
    pg_size_pretty(pg_total_relation_size(child.oid)) AS total_size
FROM pg_inherits
JOIN pg_class parent ON parent.oid = pg_inherits.inhparent
JOIN pg_class child ON child.oid = pg_inherits.inhrelid
JOIN pg_namespace n ON n.oid = parent.relnamespace
WHERE n.nspname = 'app'
  AND parent.relname = 'operation_log_partitioned'
ORDER BY pg_total_relation_size(child.oid) DESC;
```

分区维护建议：

| 维护项   | 建议                                        |
| -------- | ------------------------------------------- |
| 未来分区 | 提前创建，避免插入失败                      |
| 历史清理 | 优先 `DROP PARTITION` 或 `DETACH PARTITION` |
| 归档     | 先 `DETACH`，再备份和删除                   |
| 统计信息 | 分区表和分区都需要关注 `ANALYZE`            |
| 分区数量 | 控制合理数量，避免规划成本过高              |
| 自动化   | 使用定时任务生成未来分区和清理历史分区      |

### 分区裁剪

分区裁剪是指查询时 PostgreSQL 根据分区条件只扫描相关分区，而不是扫描所有分区。分区裁剪是分区表性能收益的核心来源。

能够触发分区裁剪的查询：

```sql
EXPLAIN
SELECT
    id,
    operation_type,
    create_time
FROM app.operation_log_partitioned
WHERE create_time >= TIMESTAMP '2026-05-01 00:00:00'
  AND create_time < TIMESTAMP '2026-06-01 00:00:00';
```

如果分区裁剪生效，执行计划中应只出现相关分区，例如 `operation_log_202605`。

不能有效裁剪的常见写法：

```sql
-- 不推荐：对分区键使用函数，可能影响分区裁剪和索引使用
SELECT
    id,
    operation_type,
    create_time
FROM app.operation_log_partitioned
WHERE create_time::date = DATE '2026-05-09';
```

推荐改为范围条件：

```sql
SELECT
    id,
    operation_type,
    create_time
FROM app.operation_log_partitioned
WHERE create_time >= TIMESTAMP '2026-05-09 00:00:00'
  AND create_time < TIMESTAMP '2026-05-10 00:00:00';
```

哈希分区裁剪示例：

```sql
EXPLAIN
SELECT
    id,
    user_id,
    event_type,
    create_time
FROM app.user_event_partitioned
WHERE user_id = 10001;
```

列表分区裁剪示例：

```sql
EXPLAIN
SELECT
    id,
    customer_name,
    region_code
FROM app.customer_partitioned
WHERE region_code = 'ZHEJIANG';
```

分区裁剪建议：

| 建议               | 说明                       |
| ------------------ | -------------------------- |
| 查询必须带分区键   | 如时间分区必须带时间范围   |
| 使用明确范围条件   | 左闭右开最清晰             |
| 避免函数包裹分区键 | 如 `create_time::date`     |
| 参数类型保持一致   | 避免隐式转换               |
| 用 `EXPLAIN` 验证  | 确认只扫描目标分区         |
| 避免分区过多       | 分区过多会增加计划阶段成本 |

分区裁剪不生效时，分区表可能比普通表更慢，因为优化器和执行器需要处理更多子表。

### 分区索引

分区表索引可以在父表上创建，也可以在具体分区上创建。在父表上创建索引时，PostgreSQL 会为各分区创建对应索引。分区索引本质上仍然分布在各个分区上。

在父表创建索引：

```sql
CREATE INDEX idx_operation_log_partitioned_type_time
ON app.operation_log_partitioned (operation_type, create_time DESC);
```

在具体分区创建索引：

```sql
CREATE INDEX idx_operation_log_202605_operator_time
ON app.operation_log_202605 (operator_id, create_time DESC);
```

订单分区表常见索引：

```sql
CREATE INDEX idx_order_info_partitioned_user_time
ON app.order_info_partitioned (user_id, create_time DESC, id DESC);

CREATE INDEX idx_order_info_partitioned_status_time
ON app.order_info_partitioned (order_status, create_time DESC, id DESC);
```

分区表唯一约束通常需要包含分区键。示例：

```sql
CREATE TABLE app.order_unique_partitioned (
    id BIGINT GENERATED ALWAYS AS IDENTITY,
    order_no VARCHAR(64) NOT NULL,
    create_time TIMESTAMP NOT NULL,
    total_amount NUMERIC(12, 2) NOT NULL,
    PRIMARY KEY (id, create_time),
    UNIQUE (order_no, create_time)
) PARTITION BY RANGE (create_time);
```

如果想全局保证 `order_no` 唯一，但唯一约束不包含分区键，分区表上会受到限制。常见解决方式包括：

| 方案                         | 说明                               |
| ---------------------------- | ---------------------------------- |
| 唯一键包含分区键             | 如 `UNIQUE(order_no, create_time)` |
| 使用普通非分区表维护唯一映射 | 单独建 `order_no_registry`         |
| 应用层生成全局唯一业务号     | 如雪花 ID、UUID、严格编号服务      |
| 按业务重新选择分区键         | 让唯一键和分区键兼容               |

分区索引建议：

| 建议                 | 说明                        |
| -------------------- | --------------------------- |
| 常用查询索引建在父表 | 让所有分区保持一致索引      |
| 特殊分区可单独建索引 | 历史分区、热点分区可差异化  |
| 分区键常与索引组合   | 如 `(user_id, create_time)` |
| 唯一索引注意分区键   | 分区表全局唯一存在限制      |
| 历史分区索引可裁剪   | 归档分区可减少不必要索引    |
| 控制索引数量         | 每个索引会放大到多个分区    |

### 分区表查询优化

分区表查询优化的核心是让查询条件命中分区键，触发分区裁剪，并在目标分区内使用合适索引。分区表不是自动提升所有查询性能的工具，如果查询不带分区键，反而可能增加成本。

推荐查询写法：

```sql
SELECT
    id,
    order_no,
    user_id,
    order_status,
    total_amount,
    create_time
FROM app.order_info_partitioned
WHERE create_time >= TIMESTAMP '2026-05-01 00:00:00'
  AND create_time < TIMESTAMP '2026-06-01 00:00:00'
  AND user_id = 10001
ORDER BY create_time DESC, id DESC
LIMIT 20;
```

推荐索引：

```sql
CREATE INDEX idx_order_info_partitioned_user_time
ON app.order_info_partitioned (user_id, create_time DESC, id DESC);
```

不推荐查询：

```sql
-- 不带分区键，可能扫描多个或全部分区
SELECT
    id,
    order_no,
    user_id,
    total_amount
FROM app.order_info_partitioned
WHERE user_id = 10001;
```

如果业务经常只按 `user_id` 查询，而不带时间范围，按时间分区可能并不适合该查询。需要考虑联合查询条件、额外索引，甚至重新评估分区键。

按月统计时带时间范围：

```sql
SELECT
    create_time::date AS order_date,
    COUNT(*) AS order_count,
    SUM(total_amount) AS total_amount
FROM app.order_info_partitioned
WHERE create_time >= TIMESTAMP '2026-05-01 00:00:00'
  AND create_time < TIMESTAMP '2026-06-01 00:00:00'
GROUP BY create_time::date
ORDER BY order_date;
```

使用 `EXPLAIN` 验证分区裁剪：

```sql
EXPLAIN ANALYZE
SELECT
    id,
    order_no,
    user_id,
    total_amount,
    create_time
FROM app.order_info_partitioned
WHERE create_time >= TIMESTAMP '2026-05-01 00:00:00'
  AND create_time < TIMESTAMP '2026-06-01 00:00:00'
  AND user_id = 10001
ORDER BY create_time DESC, id DESC
LIMIT 20;
```

分区表查询优化建议：

| 优化方向             | 说明                           |
| -------------------- | ------------------------------ |
| 查询带分区键         | 触发分区裁剪                   |
| 分区键不要被函数包裹 | 避免影响裁剪                   |
| 使用左闭右开时间范围 | 清晰匹配范围分区               |
| 分区内仍要建索引     | 裁剪后还需要快速检索           |
| 控制分区数量         | 避免计划阶段成本过高           |
| 提前过滤再 JOIN      | 先定位分区和数据范围           |
| 定期维护统计信息     | 分区表和分区都要 `ANALYZE`     |
| 历史数据归档         | 老分区可分离、压缩、备份或删除 |

典型优化示例：先筛选目标分区数据，再关联用户表。

```sql
WITH page_order AS (
    SELECT
        id,
        order_no,
        user_id,
        order_status,
        total_amount,
        create_time
    FROM app.order_info_partitioned
    WHERE create_time >= TIMESTAMP '2026-05-01 00:00:00'
      AND create_time < TIMESTAMP '2026-06-01 00:00:00'
      AND order_status = 'PAID'
    ORDER BY create_time DESC, id DESC
    LIMIT 20
)
SELECT
    o.id,
    o.order_no,
    o.order_status,
    o.total_amount,
    o.create_time,
    u.username,
    u.nickname
FROM page_order o
INNER JOIN app.sys_user u ON u.id = o.user_id
ORDER BY o.create_time DESC, o.id DESC;
```

分区表设计和查询必须配套。只创建分区表而不调整 SQL 查询条件和索引设计，通常无法获得预期性能收益。


## 继承与表关系

表继承是 PostgreSQL 提供的一种表结构复用机制。子表可以继承父表的字段和约束，并且查询父表时默认会包含子表数据。它曾经常用于实现手工分区，但 PostgreSQL 10 之后已经提供声明式分区，普通项目中不建议再用传统继承手工实现分区。声明式分区支持范围、列表和哈希分区，插入父表时会根据分区键自动路由到对应分区。([PostgreSQL](https://www.postgresql.org/docs/current/static/ddl-partitioning.html?utm_source=chatgpt.com))

### 表继承概念

表继承允许一个表继承另一个表的字段定义。父表定义通用字段，子表继承这些字段，并且可以额外增加自己的字段。查询父表时，默认会查询父表和所有子表的数据；如果只想查询父表本身，可以使用 `ONLY`。

基础概念如下：

| 概念          | 说明                                 |
| ------------- | ------------------------------------ |
| 父表          | 被继承的表，定义公共字段             |
| 子表          | 继承父表字段的表，可以增加额外字段   |
| `INHERITS`    | 创建子表时声明继承关系               |
| `ONLY`        | 只查询父表本身，不包含子表           |
| `pg_inherits` | 系统目录，用于记录表和索引的继承关系 |

创建父表：

```sql
CREATE TABLE app.base_document (
    id BIGINT GENERATED ALWAYS AS IDENTITY,
    document_no VARCHAR(64) NOT NULL,
    document_title VARCHAR(255) NOT NULL,
    create_time TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);
```

创建子表：

```sql
CREATE TABLE app.contract_document (
    contract_amount NUMERIC(12, 2),
    sign_date DATE
) INHERITS (app.base_document);
```

子表会拥有父表字段和自己的字段。插入子表数据：

```sql
INSERT INTO app.contract_document (
    document_no,
    document_title,
    contract_amount,
    sign_date
) VALUES (
    'HT202605090001',
    '采购合同',
    19999.00,
    DATE '2026-05-09'
);
```

查询父表时，默认包含子表数据：

```sql
SELECT
    id,
    document_no,
    document_title,
    create_time
FROM app.base_document;
```

只查询父表本身：

```sql
SELECT
    id,
    document_no,
    document_title,
    create_time
FROM ONLY app.base_document;
```

查看继承关系：

```sql
SELECT
    parent.relname AS parent_table,
    child.relname AS child_table
FROM pg_inherits i
JOIN pg_class parent ON parent.oid = i.inhparent
JOIN pg_class child ON child.oid = i.inhrelid
JOIN pg_namespace n ON n.oid = parent.relnamespace
WHERE n.nspname = 'app'
ORDER BY parent.relname, child.relname;
```

`pg_inherits` 用于记录表和索引继承层级，每条直接父子关系都会在该系统目录中有一条记录。([PostgreSQL](https://www.postgresql.org/docs/17/catalog-pg-inherits.html?utm_source=chatgpt.com))

### 表继承使用

表继承适合字段结构存在共同部分，但不同子类又有少量差异字段的场景。不过在业务系统中，表继承的使用频率不高，因为它和应用层 ORM、迁移工具、权限控制、索引管理之间的配合成本较高。

示例：定义统一消息父表，并创建短信消息和邮件消息子表。

创建父表：

```sql
CREATE TABLE app.message_base (
    id BIGINT GENERATED ALWAYS AS IDENTITY,
    message_type VARCHAR(32) NOT NULL,
    receiver VARCHAR(255) NOT NULL,
    message_content TEXT NOT NULL,
    send_status VARCHAR(32) NOT NULL DEFAULT 'PENDING',
    create_time TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);
```

创建短信消息子表：

```sql
CREATE TABLE app.sms_message (
    phone VARCHAR(20) NOT NULL,
    sms_provider VARCHAR(64)
) INHERITS (app.message_base);
```

创建邮件消息子表：

```sql
CREATE TABLE app.email_message (
    email VARCHAR(128) NOT NULL,
    email_subject VARCHAR(255)
) INHERITS (app.message_base);
```

插入短信消息：

```sql
INSERT INTO app.sms_message (
    message_type,
    receiver,
    message_content,
    phone,
    sms_provider
) VALUES (
    'SMS',
    '13800000000',
    '验证码为 123456',
    '13800000000',
    'aliyun'
);
```

插入邮件消息：

```sql
INSERT INTO app.email_message (
    message_type,
    receiver,
    message_content,
    email,
    email_subject
) VALUES (
    'EMAIL',
    'admin@example.com',
    '欢迎使用系统',
    'admin@example.com',
    '系统通知'
);
```

查询所有消息公共字段：

```sql
SELECT
    id,
    message_type,
    receiver,
    message_content,
    send_status,
    create_time
FROM app.message_base
ORDER BY create_time DESC;
```

查询短信子表独有字段：

```sql
SELECT
    id,
    message_type,
    receiver,
    phone,
    sms_provider,
    send_status,
    create_time
FROM app.sms_message
ORDER BY create_time DESC;
```

查询表来源：

```sql
SELECT
    tableoid::regclass AS source_table,
    id,
    message_type,
    receiver,
    send_status
FROM app.message_base
ORDER BY create_time DESC;
```

`tableoid::regclass` 可以显示当前行实际来自哪个物理表，在继承查询和分区排查中很有用。

表继承适用场景：

| 场景                 | 说明                           |
| -------------------- | ------------------------------ |
| 多类对象共享公共字段 | 如消息、文档、事件             |
| 子类有少量差异字段   | 子表可以增加字段               |
| 需要查询父类公共视图 | 查询父表返回所有子表公共字段   |
| 特殊分区需求         | 声明式分区无法满足的极少数场景 |
| 历史系统兼容         | 老项目可能使用继承分区         |

普通业务建模中，更常见的做法是使用一张主表加类型字段，或者使用主表加扩展表，而不是直接使用表继承。

### 表继承限制

表继承虽然灵活，但存在不少限制和维护成本。使用前需要明确这些约束，否则容易造成数据一致性、索引、权限和应用映射方面的问题。

常见限制如下：

| 限制                           | 说明                                    |
| ------------------------------ | --------------------------------------- |
| 父表唯一约束不天然覆盖所有子表 | 子表之间可能出现重复值                  |
| 父表索引不会自动覆盖子表       | 子表通常需要单独建索引                  |
| 外键引用父表时要谨慎           | 继承层级和外键语义容易不符合预期        |
| 子表可以增加额外字段           | 查询父表时只能看到公共字段              |
| 应用 ORM 支持复杂              | Java ORM 通常不建议直接依赖数据库表继承 |
| 权限管理更复杂                 | 父表和子表权限都要关注                  |
| 触发器和约束行为需单独确认     | 不同对象的继承行为并不完全一致          |
| 查询规划可能更复杂             | 子表较多时规划和扫描成本增加            |

父表唯一约束不能直接保证所有子表全局唯一的典型问题：

```sql
CREATE TABLE app.inherit_user_base (
    username VARCHAR(64) UNIQUE
);

CREATE TABLE app.inherit_admin_user (
    admin_level INTEGER
) INHERITS (app.inherit_user_base);

CREATE TABLE app.inherit_normal_user (
    user_level INTEGER
) INHERITS (app.inherit_user_base);
```

子表之间的唯一性不能简单依赖父表 `UNIQUE` 约束实现全局唯一。更稳妥的做法是使用单表设计、统一注册表，或者通过应用层和独立约束表控制全局唯一。

查看父表和子表索引：

```sql
SELECT
    schemaname,
    tablename,
    indexname,
    indexdef
FROM pg_indexes
WHERE schemaname = 'app'
  AND tablename IN ('message_base', 'sms_message', 'email_message')
ORDER BY tablename, indexname;
```

只查询父表本身，避免误查子表：

```sql
SELECT
    id,
    message_type,
    receiver
FROM ONLY app.message_base;
```

继承使用建议：

| 建议                     | 说明                                 |
| ------------------------ | ------------------------------------ |
| 新项目谨慎使用继承       | 优先考虑普通关系模型或声明式分区     |
| 不用继承替代面向对象建模 | 数据库表继承和 Java 类继承不是一回事 |
| 子表单独建索引           | 不要假设父表索引自动覆盖子表         |
| 全局唯一要单独设计       | 不要误以为父表唯一约束覆盖所有子表   |
| 查询时明确是否包含子表   | 必要时使用 `ONLY`                    |
| 建模前评估 ORM 支持      | 避免应用层映射复杂化                 |

### 继承与分区区别

继承和声明式分区都涉及父表和子表关系，但目标完全不同。声明式分区是为大表拆分、分区裁剪和数据生命周期管理设计的；表继承是更通用的结构复用机制。PostgreSQL 官方文档也说明，声明式分区适合大多数常见分区场景，继承分区更灵活，但缺少部分内置分区的性能和管理能力。([PostgreSQL](https://www.postgresql.org/docs/current/static/ddl-partitioning.html?utm_source=chatgpt.com))

对比如下：

| 对比项     | 表继承                   | 声明式分区                       |
| ---------- | ------------------------ | -------------------------------- |
| 主要目的   | 表结构复用、特殊继承建模 | 大表拆分、查询裁剪、生命周期管理 |
| 创建语法   | `INHERITS`               | `PARTITION BY`、`PARTITION OF`   |
| 数据路由   | 需要应用或触发器自行控制 | 插入父表自动路由到分区           |
| 子表字段   | 子表可以增加额外字段     | 分区必须和父表列结构一致         |
| 分区方式   | 非常灵活，但需要手动约束 | 支持范围、列表、哈希             |
| 分区裁剪   | 依赖约束排除等机制       | 内置分区裁剪                     |
| 管理便利性 | 手动维护较多             | DDL 更标准                       |
| 新项目推荐 | 谨慎使用                 | 大表分区优先选择                 |

声明式分区示例：

```sql
CREATE TABLE app.log_partitioned (
    id BIGINT GENERATED ALWAYS AS IDENTITY,
    log_type VARCHAR(64) NOT NULL,
    log_content TEXT,
    create_time TIMESTAMP NOT NULL,
    PRIMARY KEY (id, create_time)
) PARTITION BY RANGE (create_time);
```

创建分区：

```sql
CREATE TABLE app.log_partitioned_202605
PARTITION OF app.log_partitioned
FOR VALUES FROM ('2026-05-01 00:00:00') TO ('2026-06-01 00:00:00');
```

继承表示例：

```sql
CREATE TABLE app.log_base (
    id BIGINT GENERATED ALWAYS AS IDENTITY,
    log_type VARCHAR(64) NOT NULL,
    log_content TEXT,
    create_time TIMESTAMP NOT NULL
);

CREATE TABLE app.login_log (
    login_ip VARCHAR(64)
) INHERITS (app.log_base);
```

选择建议：

| 需求                 | 推荐                               |
| -------------------- | ---------------------------------- |
| 大表按时间拆分       | 声明式范围分区                     |
| 大表按租户或地区拆分 | 声明式列表或哈希分区               |
| 子表需要不同字段     | 表继承或普通主扩展表设计           |
| 插入父表自动路由     | 声明式分区                         |
| 需要删除历史月份数据 | 声明式分区                         |
| 想复用公共字段       | 优先考虑普通表设计，必要时才用继承 |
| Java ORM 项目        | 优先普通表或分区表，慎用继承       |

普通项目中，表继承更多是 PostgreSQL 的高级特性，不是常规建模首选。大表治理优先使用声明式分区；类型扩展优先使用单表加类型字段、主表加扩展表，或应用层多态模型。

## 全文搜索

全文搜索用于在文本字段中按词项检索内容，适合文章、文档、备注、日志、商品描述等长文本搜索。PostgreSQL 内置全文搜索能力，核心是把文档转换成 `tsvector`，把用户查询转换成 `tsquery`，再通过 `@@` 操作符匹配。官方文档说明，完整的全文搜索通常包括文档解析、查询解析、相关度排序和结果高亮。([PostgreSQL](https://www.postgresql.org/docs/17/textsearch-controls.html?utm_source=chatgpt.com))

### 全文搜索概念

全文搜索和 `LIKE '%keyword%'` 不同。`LIKE` 是字符串模式匹配，适合短文本和简单模糊查询；全文搜索会对文本进行解析、分词、词形归一化和索引，适合长文本检索。

核心概念如下：

| 概念                   | 说明                                     |
| ---------------------- | ---------------------------------------- |
| `tsvector`             | 文档向量，保存文本分词后的词项和位置信息 |
| `tsquery`              | 查询表达式，表示要匹配的词项和逻辑关系   |
| `@@`                   | 全文搜索匹配操作符                       |
| `to_tsvector`          | 把文本转换为 `tsvector`                  |
| `to_tsquery`           | 把查询表达式转换为 `tsquery`             |
| `plainto_tsquery`      | 把普通文本转换为查询条件                 |
| `websearch_to_tsquery` | 使用类似 Web 搜索语法解析查询            |
| `ts_rank`              | 计算相关度分数                           |
| `ts_headline`          | 生成高亮片段                             |

准备示例表：

```sql
CREATE TABLE app.article_search (
    id BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    title VARCHAR(255) NOT NULL,
    content TEXT NOT NULL,
    author_name VARCHAR(64),
    enabled BOOLEAN NOT NULL DEFAULT TRUE,
    create_time TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);
```

插入示例数据：

```sql
INSERT INTO app.article_search (
    title,
    content,
    author_name
) VALUES
    (
        'PostgreSQL 17 使用文档',
        'PostgreSQL is a powerful open source relational database with JSONB, indexes, transactions and full text search.',
        'Ateng'
    ),
    (
        'Spring Boot 集成 PostgreSQL',
        'Spring Boot applications can connect to PostgreSQL by JDBC, MyBatis, JPA and connection pools.',
        'Ateng'
    ),
    (
        'Database Index Design',
        'A good database index can improve query performance, but too many indexes slow down writes.',
        'Ateng'
    );
```

基础全文搜索：

```sql
SELECT
    id,
    title,
    content
FROM app.article_search
WHERE to_tsvector('english', title || ' ' || content)
      @@ plainto_tsquery('english', 'postgresql database');
```

全文搜索适合场景：

| 场景             | 说明                   |
| ---------------- | ---------------------- |
| 长文本搜索       | 文章、文档、说明、备注 |
| 多词搜索         | 多个关键词组合         |
| 相关度排序       | 匹配结果按相关性排序   |
| 搜索高亮         | 返回命中片段           |
| 英文词形处理     | 如复数、词干归一化     |
| 中小规模站内搜索 | 后台内容搜索、文档搜索 |

如果需要复杂中文分词、拼音搜索、同义词、纠错、搜索建议、复杂排序和海量数据搜索，建议考虑 Elasticsearch、OpenSearch 等专门搜索系统。

### tsvector 使用

`tsvector` 是全文搜索中的文档表示形式。`to_tsvector` 会把文本解析成词项，并记录词项位置。PostgreSQL 官方文档说明，`to_tsvector` 会把文本文档解析为 token，再归一化为 lexeme，并返回包含词项和位置的 `tsvector`。([PostgreSQL](https://www.postgresql.org/docs/17/textsearch-controls.html?utm_source=chatgpt.com))

查看 `tsvector` 结果：

```sql
SELECT
    to_tsvector(
        'english',
        'PostgreSQL is a powerful open source relational database.'
    ) AS search_vector;
```

将标题和内容合并成搜索向量：

```sql
SELECT
    id,
    title,
    to_tsvector('english', title || ' ' || content) AS search_vector
FROM app.article_search;
```

处理空值：

```sql
SELECT
    id,
    title,
    to_tsvector(
        'english',
        COALESCE(title, '') || ' ' || COALESCE(content, '')
    ) AS search_vector
FROM app.article_search;
```

使用权重区分标题和正文：

```sql
SELECT
    id,
    title,
    setweight(to_tsvector('english', COALESCE(title, '')), 'A')
    ||
    setweight(to_tsvector('english', COALESCE(content, '')), 'B') AS search_vector
FROM app.article_search;
```

权重说明：

| 权重 | 常见含义           |
| ---- | ------------------ |
| `A`  | 最重要，例如标题   |
| `B`  | 较重要，例如摘要   |
| `C`  | 普通内容           |
| `D`  | 最低权重，例如备注 |

使用生成列保存 `tsvector`：

```sql
CREATE TABLE app.article_search_vector (
    id BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    title VARCHAR(255) NOT NULL,
    content TEXT NOT NULL,
    search_vector TSVECTOR GENERATED ALWAYS AS (
        setweight(to_tsvector('english', COALESCE(title, '')), 'A')
        ||
        setweight(to_tsvector('english', COALESCE(content, '')), 'B')
    ) STORED,
    enabled BOOLEAN NOT NULL DEFAULT TRUE,
    create_time TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);
```

插入数据时，`search_vector` 会自动生成：

```sql
INSERT INTO app.article_search_vector (
    title,
    content
) VALUES (
    'PostgreSQL Full Text Search',
    'PostgreSQL provides tsvector, tsquery, ranking and highlighting for full text search.'
);
```

查询生成列：

```sql
SELECT
    id,
    title,
    search_vector
FROM app.article_search_vector;
```

`tsvector` 使用建议：

| 建议                 | 说明                       |
| -------------------- | -------------------------- |
| 多字段搜索要合并向量 | 标题、摘要、正文可组合     |
| 空值用 `COALESCE`    | 避免拼接结果为 `NULL`      |
| 重要字段加权         | 标题命中应比正文命中更重要 |
| 高频搜索用生成列     | 减少每次查询重复计算       |
| 配合 GIN 索引        | 提升全文搜索性能           |

### tsquery 使用

`tsquery` 表示全文搜索查询条件。PostgreSQL 提供多种构造查询的函数，包括 `to_tsquery`、`plainto_tsquery`、`phraseto_tsquery` 和 `websearch_to_tsquery`。

基础 `plainto_tsquery`：

```sql
SELECT
    plainto_tsquery('english', 'postgresql database') AS query_text;
```

使用普通文本查询：

```sql
SELECT
    id,
    title
FROM app.article_search
WHERE to_tsvector('english', title || ' ' || content)
      @@ plainto_tsquery('english', 'postgresql database');
```

使用 `to_tsquery` 表达逻辑关系：

```sql
SELECT
    id,
    title
FROM app.article_search
WHERE to_tsvector('english', title || ' ' || content)
      @@ to_tsquery('english', 'postgresql & database');
```

`to_tsquery` 常见操作符：

| 操作符 | 说明                      |
| ------ | ------------------------- |
| `&`    | AND，两个词项都要匹配     |
| `|`    | OR，任意一个词项匹配      |
| `!`    | NOT，排除词项             |
| `<->`  | FOLLOWED BY，短语相邻匹配 |

短语查询：

```sql
SELECT
    id,
    title
FROM app.article_search
WHERE to_tsvector('english', title || ' ' || content)
      @@ phraseto_tsquery('english', 'full text search');
```

Web 风格查询：

```sql
SELECT
    id,
    title
FROM app.article_search
WHERE to_tsvector('english', title || ' ' || content)
      @@ websearch_to_tsquery('english', '"full text search" OR database');
```

不同查询函数对比：

| 函数                   | 适用场景                         |
| ---------------------- | -------------------------------- |
| `plainto_tsquery`      | 用户输入普通关键词，最常用       |
| `to_tsquery`           | 开发者构造精确查询表达式         |
| `phraseto_tsquery`     | 短语搜索                         |
| `websearch_to_tsquery` | 类似搜索引擎语法，更适合用户输入 |

实际项目中，接口搜索框通常优先使用 `websearch_to_tsquery` 或 `plainto_tsquery`，避免用户直接输入 `to_tsquery` 表达式导致语法错误。

### 中文全文搜索方案

PostgreSQL 内置英文等语言的全文搜索配置，但中文没有天然空格分词，直接使用默认全文搜索效果通常不理想。中文搜索一般有三类方案：简单模糊匹配、数据库分词扩展、外部搜索引擎。

方案对比如下：

| 方案             | 说明                                      | 适用场景                                 |
| ---------------- | ----------------------------------------- | ---------------------------------------- |
| `LIKE` / `ILIKE` | 简单包含匹配                              | 小数据量、低频搜索                       |
| `pg_trgm`        | 基于 trigram 的相似匹配                   | 中小规模模糊搜索                         |
| 中文分词扩展     | 如 zhparser、Jieba 相关扩展               | 希望继续在 PostgreSQL 内完成中文全文搜索 |
| 外部搜索引擎     | Elasticsearch、OpenSearch、Meilisearch 等 | 复杂中文搜索、大规模检索                 |

简单中文模糊查询：

```sql
SELECT
    id,
    title,
    content
FROM app.article_search
WHERE title LIKE '%数据库%'
   OR content LIKE '%数据库%';
```

使用 `pg_trgm` 优化包含匹配：

```sql
CREATE EXTENSION IF NOT EXISTS pg_trgm;

CREATE INDEX idx_article_search_title_trgm
ON app.article_search
USING GIN (title gin_trgm_ops);

CREATE INDEX idx_article_search_content_trgm
ON app.article_search
USING GIN (content gin_trgm_ops);
```

查询：

```sql
SELECT
    id,
    title,
    content
FROM app.article_search
WHERE title LIKE '%数据库%'
   OR content LIKE '%数据库%';
```

中文内容如果使用外部分词后再写入搜索字段，可以把分词结果用空格拼接，再使用简单配置：

```sql
CREATE TABLE app.article_cn_search (
    id BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    title VARCHAR(255) NOT NULL,
    content TEXT NOT NULL,
    search_text TEXT NOT NULL,
    search_vector TSVECTOR GENERATED ALWAYS AS (
        to_tsvector('simple', search_text)
    ) STORED,
    create_time TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);
```

插入分词后的搜索文本：

```sql
INSERT INTO app.article_cn_search (
    title,
    content,
    search_text
) VALUES (
    'PostgreSQL 全文搜索',
    'PostgreSQL 支持全文搜索，也可以结合中文分词扩展。',
    'PostgreSQL 全文 搜索 中文 分词 扩展'
);
```

查询：

```sql
SELECT
    id,
    title
FROM app.article_cn_search
WHERE search_vector @@ plainto_tsquery('simple', '全文 搜索');
```

中文搜索建议：

| 场景                   | 建议                                     |
| ---------------------- | ---------------------------------------- |
| 数据量小、搜索简单     | `LIKE` 或 `pg_trgm`                      |
| 只需要后台模糊查询     | `pg_trgm` 通常够用                       |
| 需要中文分词           | 使用 PostgreSQL 中文分词扩展或应用层分词 |
| 需要拼音、同义词、纠错 | 使用专门搜索引擎                         |
| 数据规模大、搜索复杂   | Elasticsearch 或 OpenSearch              |
| 搜索结果要高相关度排序 | 专门搜索引擎更合适                       |

如果只是后台管理系统的标题和备注搜索，`pg_trgm` 通常比完整中文全文搜索方案更简单。如果是面向用户的内容搜索，应尽早评估专门搜索引擎。

### 全文搜索索引

全文搜索如果不建索引，每次查询都需要对文本进行解析和匹配，数据量大时性能会明显下降。常见做法是创建 GIN 索引。PostgreSQL 官方全文搜索文档说明，全文搜索需要把文档转换为 `tsvector`，把查询转换为 `tsquery`；在实际使用中通常会配合索引提升性能。([PostgreSQL](https://www.postgresql.org/docs/17/textsearch-controls.html?utm_source=chatgpt.com))

直接对表达式创建 GIN 索引：

```sql
CREATE INDEX idx_article_search_fulltext
ON app.article_search
USING GIN (
    to_tsvector('english', COALESCE(title, '') || ' ' || COALESCE(content, ''))
);
```

对应查询必须尽量保持表达式一致：

```sql
SELECT
    id,
    title
FROM app.article_search
WHERE to_tsvector('english', COALESCE(title, '') || ' ' || COALESCE(content, ''))
      @@ plainto_tsquery('english', 'postgresql database');
```

使用生成列加索引：

```sql
CREATE TABLE app.article_fulltext (
    id BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    title VARCHAR(255) NOT NULL,
    summary TEXT,
    content TEXT NOT NULL,
    search_vector TSVECTOR GENERATED ALWAYS AS (
        setweight(to_tsvector('english', COALESCE(title, '')), 'A')
        ||
        setweight(to_tsvector('english', COALESCE(summary, '')), 'B')
        ||
        setweight(to_tsvector('english', COALESCE(content, '')), 'C')
    ) STORED,
    enabled BOOLEAN NOT NULL DEFAULT TRUE,
    create_time TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_article_fulltext_search_vector
ON app.article_fulltext
USING GIN (search_vector);
```

查询生成列：

```sql
SELECT
    id,
    title,
    summary
FROM app.article_fulltext
WHERE enabled = TRUE
  AND search_vector @@ websearch_to_tsquery('english', 'postgresql database');
```

如果经常只搜索启用文章，可以创建部分索引：

```sql
CREATE INDEX idx_article_fulltext_enabled_search_vector
ON app.article_fulltext
USING GIN (search_vector)
WHERE enabled = TRUE;
```

查看执行计划：

```sql
EXPLAIN ANALYZE
SELECT
    id,
    title
FROM app.article_fulltext
WHERE enabled = TRUE
  AND search_vector @@ websearch_to_tsquery('english', 'postgresql database');
```

全文索引建议：

| 建议                       | 说明                           |
| -------------------------- | ------------------------------ |
| 高频搜索必须建索引         | 使用 GIN 索引                  |
| 表达式索引要匹配查询表达式 | 否则可能无法使用索引           |
| 生成列更易维护             | 查询时直接使用 `search_vector` |
| 标题和正文设置不同权重     | 提升相关度排序质量             |
| 可创建部分索引             | 如只索引启用内容               |
| 中文搜索慎用默认配置       | 需要分词或其他方案             |

### 搜索排序

全文搜索排序通常使用 `ts_rank` 或 `ts_rank_cd` 计算相关度。PostgreSQL 官方文档说明，相关度排序用于衡量文档和查询之间的匹配程度，内置排序函数会考虑词项出现频率、距离和结构信息，但相关度本身具有业务差异，实际系统可以结合发布时间、点击量等因素定制排序。([PostgreSQL](https://www.postgresql.org/docs/17/textsearch-controls.html?utm_source=chatgpt.com))

基础相关度排序：

```sql
SELECT
    id,
    title,
    ts_rank(
        search_vector,
        websearch_to_tsquery('english', 'postgresql database')
    ) AS rank_score
FROM app.article_fulltext
WHERE search_vector @@ websearch_to_tsquery('english', 'postgresql database')
ORDER BY rank_score DESC;
```

使用权重后的相关度排序：

```sql
SELECT
    id,
    title,
    ts_rank(
        ARRAY[0.1, 0.2, 0.4, 1.0],
        search_vector,
        websearch_to_tsquery('english', 'postgresql database')
    ) AS rank_score
FROM app.article_fulltext
WHERE search_vector @@ websearch_to_tsquery('english', 'postgresql database')
ORDER BY rank_score DESC, create_time DESC;
```

权重数组对应 `D`、`C`、`B`、`A`，通常标题使用 `A` 权重，正文使用 `C` 或 `D`。

结合发布时间排序：

```sql
SELECT
    id,
    title,
    create_time,
    ts_rank(
        search_vector,
        websearch_to_tsquery('english', 'postgresql database')
    ) AS rank_score
FROM app.article_fulltext
WHERE enabled = TRUE
  AND search_vector @@ websearch_to_tsquery('english', 'postgresql database')
ORDER BY
    rank_score DESC,
    create_time DESC
LIMIT 20;
```

结合业务热度排序：

```sql
SELECT
    id,
    title,
    view_count,
    create_time,
    ts_rank(
        search_vector,
        websearch_to_tsquery('english', 'postgresql database')
    ) AS rank_score,
    (
        ts_rank(
            search_vector,
            websearch_to_tsquery('english', 'postgresql database')
        ) * 0.8
        + ln(GREATEST(view_count, 1)) * 0.2
    ) AS final_score
FROM app.article_fulltext
WHERE enabled = TRUE
  AND search_vector @@ websearch_to_tsquery('english', 'postgresql database')
ORDER BY final_score DESC, create_time DESC
LIMIT 20;
```

分页搜索：

```sql
SELECT
    id,
    title,
    ts_rank(
        search_vector,
        websearch_to_tsquery('english', 'postgresql database')
    ) AS rank_score
FROM app.article_fulltext
WHERE enabled = TRUE
  AND search_vector @@ websearch_to_tsquery('english', 'postgresql database')
ORDER BY rank_score DESC, id DESC
LIMIT 20 OFFSET 0;
```

搜索排序建议：

| 建议                        | 说明                                   |
| --------------------------- | -------------------------------------- |
| 使用 `ts_rank` 做基础相关度 | 适合多数搜索列表                       |
| 标题设置更高权重            | 标题命中通常比正文命中更重要           |
| 排序追加稳定字段            | 如 `ORDER BY rank_score DESC, id DESC` |
| 可结合业务指标              | 发布时间、点击量、收藏量等             |
| 避免深分页                  | 搜索结果深分页成本较高                 |
| 大规模复杂排序用搜索引擎    | PostgreSQL 适合中小规模搜索            |

### 搜索高亮

搜索高亮用于在结果中展示命中的片段，提升用户识别搜索结果的效率。PostgreSQL 使用 `ts_headline` 生成高亮文本。官方文档将高亮作为全文搜索控制的一部分，适合把匹配词用指定标签包裹后返回给前端展示。([PostgreSQL](https://www.postgresql.org/docs/17/textsearch-controls.html?utm_source=chatgpt.com))

基础高亮：

```sql
SELECT
    id,
    title,
    ts_headline(
        'english',
        content,
        websearch_to_tsquery('english', 'postgresql database')
    ) AS highlighted_content
FROM app.article_fulltext
WHERE search_vector @@ websearch_to_tsquery('english', 'postgresql database');
```

自定义高亮标签：

```sql
SELECT
    id,
    title,
    ts_headline(
        'english',
        content,
        websearch_to_tsquery('english', 'postgresql database'),
        'StartSel=<mark>, StopSel=</mark>, MaxWords=35, MinWords=15'
    ) AS highlighted_content
FROM app.article_fulltext
WHERE enabled = TRUE
  AND search_vector @@ websearch_to_tsquery('english', 'postgresql database');
```

标题和正文同时高亮：

```sql
SELECT
    id,
    ts_headline(
        'english',
        title,
        websearch_to_tsquery('english', 'postgresql database'),
        'StartSel=<mark>, StopSel=</mark>'
    ) AS highlighted_title,
    ts_headline(
        'english',
        content,
        websearch_to_tsquery('english', 'postgresql database'),
        'StartSel=<mark>, StopSel=</mark>, MaxWords=40, MinWords=20'
    ) AS highlighted_content
FROM app.article_fulltext
WHERE enabled = TRUE
  AND search_vector @@ websearch_to_tsquery('english', 'postgresql database')
ORDER BY
    ts_rank(search_vector, websearch_to_tsquery('english', 'postgresql database')) DESC
LIMIT 20;
```

使用 CTE 避免重复构造查询条件：

```sql
WITH query_param AS (
    SELECT websearch_to_tsquery('english', 'postgresql database') AS query_text
)
SELECT
    a.id,
    ts_headline(
        'english',
        a.title,
        q.query_text,
        'StartSel=<mark>, StopSel=</mark>'
    ) AS highlighted_title,
    ts_headline(
        'english',
        a.content,
        q.query_text,
        'StartSel=<mark>, StopSel=</mark>, MaxWords=40, MinWords=20'
    ) AS highlighted_content,
    ts_rank(a.search_vector, q.query_text) AS rank_score
FROM app.article_fulltext a
CROSS JOIN query_param q
WHERE a.enabled = TRUE
  AND a.search_vector @@ q.query_text
ORDER BY rank_score DESC, a.id DESC
LIMIT 20;
```

常用高亮参数：

| 参数                | 说明             |
| ------------------- | ---------------- |
| `StartSel`          | 命中词开始标签   |
| `StopSel`           | 命中词结束标签   |
| `MaxWords`          | 高亮片段最大词数 |
| `MinWords`          | 高亮片段最小词数 |
| `ShortWord`         | 忽略短词阈值     |
| `HighlightAll`      | 是否高亮整段文本 |
| `MaxFragments`      | 最大片段数量     |
| `FragmentDelimiter` | 多片段分隔符     |

搜索高亮建议：

| 建议                   | 说明                                             |
| ---------------------- | ------------------------------------------------ |
| 前端展示用 `<mark>`    | 简单清晰                                         |
| 控制片段长度           | 避免返回过大正文                                 |
| 只对命中结果高亮       | 先 `@@` 过滤，再 `ts_headline`                   |
| 复杂页面可前端二次处理 | 数据库返回片段，前端负责样式                     |
| 注意 XSS               | 如果原始内容来自用户输入，前端渲染时要做安全处理 |
| 中文高亮依赖分词方案   | 默认英文配置不适合中文自然分词                   |

全文搜索整体建议：

| 需求             | 推荐方案                            |
| ---------------- | ----------------------------------- |
| 英文站内搜索     | PostgreSQL 全文搜索                 |
| 后台短文本搜索   | `ILIKE` 或 `pg_trgm`                |
| 中文简单模糊搜索 | `pg_trgm`                           |
| 中文复杂搜索     | 分词扩展或 Elasticsearch/OpenSearch |
| 需要排序和高亮   | `ts_rank` + `ts_headline`           |
| 高频搜索         | 生成列 `tsvector` + GIN 索引        |
| 多字段权重搜索   | `setweight` 合并向量                |

PostgreSQL 全文搜索适合作为数据库内的中轻量搜索能力。它的优势是部署简单、事务一致、SQL 集成方便；不足是中文分词、复杂相关度、搜索建议和大规模检索能力不如专门搜索引擎。


## 扩展插件

扩展插件用于增强 PostgreSQL 的内置能力，例如 UUID 生成、加密函数、SQL 统计、远程表访问、模糊搜索、空间数据、键值类型和特殊索引操作符类。项目中应按需安装扩展，不建议在业务库中一次性启用所有扩展。

### 扩展查看

扩展查看用于确认当前数据库已经安装了哪些扩展、当前 PostgreSQL 环境支持哪些扩展，以及扩展的默认版本、已安装版本和说明信息。PostgreSQL 官方文档说明，可通过 `pg_available_extensions` 和 `pg_available_extension_versions` 查看当前可加载的扩展。([PostgreSQL](https://www.postgresql.org/docs/17/sql-createextension.html?utm_source=chatgpt.com))

查看当前数据库已安装扩展：

```sql
SELECT
    extname AS extension_name,
    extversion AS extension_version,
    n.nspname AS schema_name
FROM pg_extension e
JOIN pg_namespace n ON n.oid = e.extnamespace
ORDER BY extname;
```

使用 `psql` 查看扩展：

```sql
-- 查看当前数据库已安装扩展
\dx
```

查看当前环境可用扩展：

```sql
SELECT
    name,
    default_version,
    installed_version,
    comment
FROM pg_available_extensions
ORDER BY name;
```

查看指定扩展是否可用：

```sql
SELECT
    name,
    default_version,
    installed_version,
    comment
FROM pg_available_extensions
WHERE name IN (
    'uuid-ossp',
    'pgcrypto',
    'pg_stat_statements',
    'postgres_fdw',
    'pg_trgm',
    'btree_gin',
    'btree_gist',
    'hstore',
    'postgis'
)
ORDER BY name;
```

查看扩展可安装版本：

```sql
SELECT
    name,
    version,
    installed,
    superuser,
    trusted,
    relocatable,
    schema,
    comment
FROM pg_available_extension_versions
WHERE name = 'pg_trgm'
ORDER BY version;
```

查看扩展对象：

```sql
SELECT
    e.extname AS extension_name,
    c.relkind,
    c.relname AS object_name
FROM pg_extension e
JOIN pg_depend d ON d.refobjid = e.oid
JOIN pg_class c ON c.oid = d.objid
WHERE e.extname = 'hstore'
ORDER BY c.relkind, c.relname;
```

扩展查看建议如下：

| 检查项                            | 说明                                        |
| --------------------------------- | ------------------------------------------- |
| `pg_extension`                    | 当前数据库已经安装的扩展                    |
| `pg_available_extensions`         | 当前环境可安装的扩展                        |
| `pg_available_extension_versions` | 可安装版本、是否 trusted、是否可迁移 Schema |
| `\dx`                             | `psql` 中查看已安装扩展                     |
| 扩展对象依赖                      | 删除扩展前要确认是否有业务对象依赖          |

如果 `pg_available_extensions` 中看不到某个扩展，通常说明操作系统层面没有安装对应扩展包，或者 PostgreSQL 安装目录中没有该扩展的控制文件。

### 扩展安装

扩展使用 `CREATE EXTENSION` 安装，使用 `DROP EXTENSION` 删除，使用 `ALTER EXTENSION ... UPDATE` 升级。多数扩展需要较高权限；PostgreSQL 官方文档说明，除 trusted 扩展外，通常需要超级用户运行 `CREATE EXTENSION`，trusted 扩展可以由拥有当前数据库 `CREATE` 权限的用户安装。官方默认 trusted 扩展列表中包括 `btree_gin`、`btree_gist`、`hstore`、`pgcrypto`、`pg_trgm`、`uuid-ossp` 等。([PostgreSQL](https://www.postgresql.org/docs/17/contrib.html?utm_source=chatgpt.com))

基础安装：

```sql
CREATE EXTENSION IF NOT EXISTS pgcrypto;
```

指定 Schema 安装：

```sql
-- 先创建用于放置扩展对象的 Schema
CREATE SCHEMA IF NOT EXISTS ext;

-- 将可迁移的扩展对象安装到 ext Schema
CREATE EXTENSION IF NOT EXISTS hstore WITH SCHEMA ext;
```

安装并自动安装依赖扩展：

```sql
CREATE EXTENSION IF NOT EXISTS postgis CASCADE;
```

查看扩展版本：

```sql
SELECT
    extname,
    extversion
FROM pg_extension
WHERE extname = 'pgcrypto';
```

升级扩展到可用的最新版本：

```sql
ALTER EXTENSION pgcrypto UPDATE;
```

升级到指定版本：

```sql
ALTER EXTENSION pgcrypto UPDATE TO '1.3';
```

删除扩展：

```sql
DROP EXTENSION IF EXISTS hstore;
```

级联删除扩展及依赖对象，谨慎使用：

```sql
DROP EXTENSION IF EXISTS hstore CASCADE;
```

扩展安装建议如下：

| 建议                  | 说明                                           |
| --------------------- | ---------------------------------------------- |
| 按数据库安装          | 扩展安装在当前数据库，不是自动对所有数据库生效 |
| 纳入初始化脚本        | 开发、测试、生产环境保持一致                   |
| 谨慎使用 `CASCADE`    | 安装时会带依赖，删除时可能删除依赖对象         |
| 优先安装到独立 Schema | 如 `ext`，便于管理扩展对象                     |
| 权限最小化            | 不要让普通业务账号随意安装扩展                 |
| 版本升级前测试        | 扩展升级可能影响函数、类型或索引行为           |
| 备份恢复要关注扩展包  | 新环境必须安装对应扩展文件，否则恢复会失败     |

项目初始化脚本示例：

```sql
-- 常用扩展初始化脚本
CREATE SCHEMA IF NOT EXISTS ext;

CREATE EXTENSION IF NOT EXISTS pgcrypto;
CREATE EXTENSION IF NOT EXISTS pg_trgm;
CREATE EXTENSION IF NOT EXISTS btree_gin;
CREATE EXTENSION IF NOT EXISTS btree_gist;
```

如果使用 Docker 官方 PostgreSQL 镜像，常见 contrib 扩展通常已经随镜像安装；如果是系统包安装，可能需要额外安装 `postgresql-contrib` 或具体扩展包。PostGIS 通常需要单独安装 PostGIS 软件包，安装后才能在数据库中执行 `CREATE EXTENSION postgis;`。([PostGIS](https://postgis.net/documentation/getting_started/?utm_source=chatgpt.com))

### uuid-ossp

`uuid-ossp` 用于生成 UUID，提供 `uuid_generate_v1()`、`uuid_generate_v3()`、`uuid_generate_v4()`、`uuid_generate_v5()` 等函数。PostgreSQL 17 文档说明，`uuid-ossp` 可用于按多种标准算法生成 UUID；如果只需要普通随机 UUID，优先考虑 PostgreSQL 内置能力或 `pgcrypto` 中的 `gen_random_uuid()`，`uuid-ossp` 更多用于特殊 UUID 版本需求。([PostgreSQL](https://www.postgresql.org/docs/17/uuid-ossp.html?utm_source=chatgpt.com))

安装扩展：

```sql
CREATE EXTENSION IF NOT EXISTS "uuid-ossp";
```

生成随机 UUID v4：

```sql
SELECT uuid_generate_v4() AS uuid_value;
```

生成基于命名空间的 UUID v5：

```sql
SELECT uuid_generate_v5(
    uuid_ns_url(),
    'https://www.postgresql.org'
) AS uuid_value;
```

使用 UUID 作为对外标识：

```sql
CREATE TABLE app.file_info (
    id BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    file_uuid UUID NOT NULL DEFAULT uuid_generate_v4(),
    file_name VARCHAR(255) NOT NULL,
    file_size BIGINT NOT NULL,
    create_time TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uk_file_info_file_uuid UNIQUE (file_uuid)
);
```

插入数据：

```sql
INSERT INTO app.file_info (
    file_name,
    file_size
) VALUES (
    'demo.pdf',
    10240
)
RETURNING id, file_uuid, file_name;
```

常用函数如下：

| 函数                                | 说明                                              |
| ----------------------------------- | ------------------------------------------------- |
| `uuid_generate_v1()`                | 基于时间戳和 MAC 地址生成，可能暴露机器和时间信息 |
| `uuid_generate_v1mc()`              | 类似 v1，但使用随机 multicast MAC 地址            |
| `uuid_generate_v3(namespace, name)` | 基于命名空间和名称生成，使用 MD5                  |
| `uuid_generate_v4()`                | 基于随机数生成                                    |
| `uuid_generate_v5(namespace, name)` | 基于命名空间和名称生成，使用 SHA-1                |
| `uuid_nil()`                        | 返回 nil UUID                                     |
| `uuid_ns_dns()`                     | DNS 命名空间                                      |
| `uuid_ns_url()`                     | URL 命名空间                                      |

使用建议如下：

| 场景                | 建议                                        |
| ------------------- | ------------------------------------------- |
| 普通随机 UUID       | `gen_random_uuid()` 或 `uuid_generate_v4()` |
| 基于名称可复现 UUID | `uuid_generate_v5()`                        |
| 安全敏感场景        | 避免使用暴露时间和机器信息的 v1             |
| 内部主键            | 高并发表优先 `BIGINT`，UUID 可作为外部 ID   |
| 对外不可猜标识      | UUID 适合                                   |

### pgcrypto

`pgcrypto` 提供加密相关函数，包括哈希、HMAC、密码哈希、PGP 加密、随机字节和 UUID 生成等。PostgreSQL 17 文档说明，`pgcrypto` 提供 PostgreSQL 的加密函数，并且依赖 OpenSSL；它也是默认安装中被标记为 trusted 的扩展之一。([PostgreSQL](https://www.postgresql.org/docs/17/pgcrypto.html?utm_source=chatgpt.com))

安装扩展：

```sql
CREATE EXTENSION IF NOT EXISTS pgcrypto;
```

生成随机 UUID：

```sql
SELECT gen_random_uuid() AS uuid_value;
```

哈希计算：

```sql
SELECT
    encode(digest('hello-postgresql', 'sha256'), 'hex') AS sha256_hex;
```

HMAC 计算：

```sql
SELECT
    encode(
        hmac('hello-postgresql', 'secret-key', 'sha256'),
        'hex'
    ) AS hmac_sha256_hex;
```

密码哈希示例：

```sql
SELECT
    crypt('user-password', gen_salt('bf')) AS password_hash;
```

验证密码：

```sql
SELECT
    crypt('user-password', '$2a$06$exampleSaltAndHashValue') = '$2a$06$exampleSaltAndHashValue' AS matched;
```

随机字节：

```sql
SELECT encode(gen_random_bytes(16), 'hex') AS random_token;
```

业务表示例：

```sql
CREATE TABLE app.api_token (
    id BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    token_id UUID NOT NULL DEFAULT gen_random_uuid(),
    token_name VARCHAR(128) NOT NULL,
    token_secret_hash TEXT NOT NULL,
    enabled BOOLEAN NOT NULL DEFAULT TRUE,
    create_time TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uk_api_token_token_id UNIQUE (token_id)
);
```

插入 token：

```sql
INSERT INTO app.api_token (
    token_name,
    token_secret_hash
) VALUES (
    'open-api-token',
    encode(digest('plain-secret-value', 'sha256'), 'hex')
)
RETURNING id, token_id, token_name;
```

使用建议如下：

| 场景       | 推荐                                             |
| ---------- | ------------------------------------------------ |
| 生成 UUID  | `gen_random_uuid()`                              |
| 数据摘要   | `digest(data, 'sha256')`                         |
| 带密钥摘要 | `hmac(data, key, 'sha256')`                      |
| 密码存储   | `crypt()` + `gen_salt()`，但应用层密码框架更常见 |
| 随机 token | `gen_random_bytes()`                             |
| 文件校验   | `digest(file_content, 'sha256')`                 |

敏感密码处理通常更建议放在应用层成熟安全框架中，例如 Spring Security 的 `BCryptPasswordEncoder`。数据库层 `pgcrypto` 更适合数据摘要、随机值生成、数据库内校验和特定安全工具场景。

### pg_stat_statements

`pg_stat_statements` 用于统计 SQL 的规划和执行信息，是定位慢 SQL、统计高频 SQL、分析总耗时和平均耗时的重要扩展。PostgreSQL 17 文档说明，该模块用于跟踪服务器执行过的所有 SQL 的计划和执行统计；由于需要额外共享内存，必须把 `pg_stat_statements` 加入 `postgresql.conf` 的 `shared_preload_libraries`，通常需要重启服务后生效。([PostgreSQL](https://www.postgresql.org/docs/17/pgstatstatements.html?utm_source=chatgpt.com))

修改配置文件：

文件位置：`postgresql.conf`

```conf
# 加载 pg_stat_statements，需要重启 PostgreSQL
shared_preload_libraries = 'pg_stat_statements'

# 启用 query id 计算，便于 SQL 归一化统计
compute_query_id = on

# 最多跟踪的 SQL 条目数量
pg_stat_statements.max = 10000

# 跟踪顶层和函数内部 SQL
pg_stat_statements.track = all

# 是否跟踪工具类命令
pg_stat_statements.track_utility = on
```

重启 PostgreSQL 后，在目标数据库中安装扩展：

```sql
CREATE EXTENSION IF NOT EXISTS pg_stat_statements;
```

查询累计耗时最高的 SQL：

```sql
SELECT
    query,
    calls,
    total_exec_time,
    mean_exec_time,
    max_exec_time,
    rows
FROM pg_stat_statements
ORDER BY total_exec_time DESC
LIMIT 20;
```

查询平均耗时最高的 SQL：

```sql
SELECT
    query,
    calls,
    mean_exec_time,
    max_exec_time,
    rows
FROM pg_stat_statements
WHERE calls >= 10
ORDER BY mean_exec_time DESC
LIMIT 20;
```

查询读取磁盘块较多的 SQL：

```sql
SELECT
    query,
    calls,
    shared_blks_hit,
    shared_blks_read,
    temp_blks_read,
    temp_blks_written,
    total_exec_time
FROM pg_stat_statements
ORDER BY shared_blks_read DESC
LIMIT 20;
```

重置统计信息：

```sql
SELECT pg_stat_statements_reset();
```

使用建议如下：

| 建议                 | 说明                                     |
| -------------------- | ---------------------------------------- |
| 生产建议启用         | 慢 SQL 治理非常有价值                    |
| 需要重启加载         | 修改 `shared_preload_libraries` 后要重启 |
| 配合慢查询日志       | 日志看单次慢，扩展看累计统计             |
| 关注总耗时和平均耗时 | 高频短 SQL 也可能拖垮系统                |
| 定期导出统计         | 重启或重置后统计会变化                   |
| 权限要控制           | SQL 文本可能包含敏感业务信息             |

### postgres_fdw

`postgres_fdw` 是 PostgreSQL 官方提供的外部数据包装器，用于访问远程 PostgreSQL 数据库中的表。官方文档说明，它比旧的 `dblink` 更透明、更符合标准，并且在很多场景下性能更好；典型使用步骤是安装扩展、创建 foreign server、创建 user mapping、创建 foreign table 或导入远程 Schema。([PostgreSQL](https://www.postgresql.org/docs/17/postgres-fdw.html?utm_source=chatgpt.com))

安装扩展：

```sql
CREATE EXTENSION IF NOT EXISTS postgres_fdw;
```

创建外部服务器：

```sql
CREATE SERVER remote_pg_server
FOREIGN DATA WRAPPER postgres_fdw
OPTIONS (
    host '192.168.1.100',
    port '5432',
    dbname 'remote_db'
);
```

创建用户映射：

```sql
CREATE USER MAPPING FOR app_user
SERVER remote_pg_server
OPTIONS (
    user 'remote_user',
    password 'remote_password'
);
```

创建外部表：

```sql
CREATE FOREIGN TABLE app.remote_user_info (
    id BIGINT NOT NULL,
    username VARCHAR(64) NOT NULL,
    nickname VARCHAR(64),
    create_time TIMESTAMP
)
SERVER remote_pg_server
OPTIONS (
    schema_name 'public',
    table_name 'sys_user'
);
```

查询外部表：

```sql
SELECT
    id,
    username,
    nickname,
    create_time
FROM app.remote_user_info
WHERE username = 'admin';
```

导入远程 Schema：

```sql
IMPORT FOREIGN SCHEMA public
LIMIT TO (sys_user, order_info)
FROM SERVER remote_pg_server
INTO app;
```

查看外部服务器：

```sql
SELECT
    srvname,
    srvtype,
    srvversion,
    srvoptions
FROM pg_foreign_server
ORDER BY srvname;
```

查看用户映射：

```sql
SELECT
    s.srvname,
    u.umuser::regrole AS local_user,
    u.umoptions
FROM pg_user_mappings u
JOIN pg_foreign_server s ON s.oid = u.srvid
ORDER BY s.srvname, local_user::text;
```

使用建议如下：

| 场景                | 建议                             |
| ------------------- | -------------------------------- |
| 跨 PostgreSQL 查询  | 可以使用 `postgres_fdw`          |
| 数据迁移            | 可用于临时迁移和校验             |
| 报表整合            | 可读取远程只读表                 |
| 高频 OLTP 跨库 JOIN | 谨慎使用，网络和远程执行成本较高 |
| 密码管理            | 不要在脚本中明文提交密码         |
| 权限控制            | 外部表和本地用户映射都要严格控制 |

`postgres_fdw` 适合数据库间集成和迁移，不建议把核心在线业务长期设计成大量跨库实时 JOIN。跨库访问会引入网络、远程事务、权限和故障传播问题。

### pg_trgm

`pg_trgm` 提供基于 trigram 的文本相似度函数、操作符和索引操作符类，适合模糊搜索、相似字符串搜索、`LIKE '%keyword%'`、`ILIKE` 和部分正则匹配优化。官方文档说明，`pg_trgm` 提供 GiST 和 GIN 索引操作符类，用于快速相似字符串搜索，并支持 `LIKE`、`ILIKE`、`~`、`~*` 等查询的 trigram 索引搜索。([PostgreSQL](https://www.postgresql.org/docs/17/pgtrgm.html?utm_source=chatgpt.com))

安装扩展：

```sql
CREATE EXTENSION IF NOT EXISTS pg_trgm;
```

查看字符串相似度：

```sql
SELECT
    similarity('postgresql', 'postgre') AS similarity_score;
```

使用相似匹配操作符：

```sql
SELECT
    'postgresql' % 'postgre' AS matched;
```

创建 GIN trigram 索引：

```sql
CREATE INDEX idx_article_title_trgm
ON app.article
USING GIN (title gin_trgm_ops);
```

模糊查询：

```sql
SELECT
    id,
    title
FROM app.article
WHERE title ILIKE '%PostgreSQL%';
```

相似度排序：

```sql
SELECT
    id,
    title,
    similarity(title, 'PostgreSQL 使用') AS similarity_score
FROM app.article
WHERE title % 'PostgreSQL 使用'
ORDER BY similarity_score DESC, id DESC
LIMIT 20;
```

设置当前会话相似度阈值：

```sql
SET pg_trgm.similarity_threshold = 0.4;
```

GiST trigram 索引：

```sql
CREATE INDEX idx_article_title_trgm_gist
ON app.article
USING GIST (title gist_trgm_ops);
```

使用建议如下：

| 场景                    | 建议                               |
| ----------------------- | ---------------------------------- |
| 中文后台模糊搜索        | `pg_trgm` 通常比全文搜索配置更简单 |
| `LIKE '%xxx%'` 高频查询 | 创建 `GIN (... gin_trgm_ops)`      |
| 相似度排序              | 使用 `similarity()` 和 `%`         |
| 最近邻相似搜索          | 可评估 GiST trigram                |
| 超大规模搜索            | 考虑 Elasticsearch 或 OpenSearch   |
| 精确等值查询            | 普通 B-Tree 更合适                 |

`pg_trgm` 是后台管理系统中非常实用的模糊搜索扩展，但它不是完整的中文分词搜索引擎。复杂搜索仍应使用专门搜索系统。

### btree_gin

`btree_gin` 为 GIN 索引提供类似 B-Tree 行为的操作符类，支持多种普通标量类型。官方文档说明，它通常不会超过标准 B-Tree 索引性能，也不能像 B-Tree 那样强制唯一性；它的价值主要在于需要创建多列 GIN 索引，并且其中既有 GIN 可索引字段，也有普通 B-Tree 可索引字段的场景。([PostgreSQL](https://www.postgresql.org/docs/17/btree-gin.html?utm_source=chatgpt.com))

安装扩展：

```sql
CREATE EXTENSION IF NOT EXISTS btree_gin;
```

示例表：

```sql
CREATE TABLE app.article_search_tag (
    id BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    title VARCHAR(255) NOT NULL,
    tag_codes TEXT[] NOT NULL DEFAULT '{}',
    category_code VARCHAR(64) NOT NULL,
    create_time TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);
```

创建多列 GIN 索引：

```sql
CREATE INDEX idx_article_search_tag_gin
ON app.article_search_tag
USING GIN (
    tag_codes,
    category_code
);
```

查询标签和分类：

```sql
SELECT
    id,
    title,
    tag_codes,
    category_code
FROM app.article_search_tag
WHERE tag_codes @> ARRAY['postgresql']
  AND category_code = 'database';
```

普通标量字段等值查询不应优先使用 `btree_gin`：

```sql
-- 普通等值查询更推荐 B-Tree
CREATE INDEX idx_article_search_tag_category_code
ON app.article_search_tag (category_code);
```

使用建议如下：

| 场景                     | 建议                          |
| ------------------------ | ----------------------------- |
| 普通字段等值查询         | 使用 B-Tree                   |
| 普通字段唯一约束         | 使用 B-Tree 唯一索引          |
| 数组 + 普通字段组合 GIN  | 可考虑 `btree_gin`            |
| JSONB + 普通字段组合 GIN | 可结合实际查询评估            |
| 索引是否有效             | 必须用 `EXPLAIN ANALYZE` 验证 |

`btree_gin` 不是 B-Tree 的替代品。普通查询、排序、唯一性仍然优先使用 B-Tree。

### btree_gist

`btree_gist` 为 GiST 索引提供类似 B-Tree 行为的操作符类。官方文档说明，它不会通常超过标准 B-Tree 索引性能，也不能强制唯一性，但在多列 GiST 索引、排他约束、距离操作符和部分特殊场景中有价值。([PostgreSQL](https://www.postgresql.org/docs/current/btree-gist.html?utm_source=chatgpt.com))

安装扩展：

```sql
CREATE EXTENSION IF NOT EXISTS btree_gist;
```

典型场景：防止同一房间的预订时间重叠。

创建预订表：

```sql
CREATE TABLE app.room_booking (
    id BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    room_id BIGINT NOT NULL,
    booking_range TSRANGE NOT NULL,
    create_time TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);
```

添加排他约束：

```sql
ALTER TABLE app.room_booking
ADD CONSTRAINT ex_room_booking_no_overlap
EXCLUDE USING GIST (
    room_id WITH =,
    booking_range WITH &&
);
```

插入预订：

```sql
INSERT INTO app.room_booking (
    room_id,
    booking_range
) VALUES (
    10001,
    tsrange(
        TIMESTAMP '2026-05-09 10:00:00',
        TIMESTAMP '2026-05-09 12:00:00',
        '[)'
    )
);
```

如果同一房间插入重叠时间段，会触发排他约束冲突：

```sql
INSERT INTO app.room_booking (
    room_id,
    booking_range
) VALUES (
    10001,
    tsrange(
        TIMESTAMP '2026-05-09 11:00:00',
        TIMESTAMP '2026-05-09 13:00:00',
        '[)'
    )
);
```

使用建议如下：

| 场景           | 建议                                   |
| -------------- | -------------------------------------- |
| 排他约束       | `btree_gist` 很常用                    |
| 范围不重叠约束 | 如会议室、库存批次、时间段             |
| 多列 GiST 索引 | 一列为范围或空间类型，另一列为普通标量 |
| 普通等值查询   | 仍优先 B-Tree                          |
| 唯一性约束     | 不使用 `btree_gist` 实现唯一           |

`btree_gist` 在排他约束中非常实用，尤其适合“同一资源的时间段不能重叠”这类数据库层强约束。

### hstore

`hstore` 是 PostgreSQL 的键值类型扩展，用于在单个字段中存储一组 key/value 对。官方文档说明，`hstore` 的键和值都是文本字符串，适合存储很少被检查的多属性字段或半结构化数据；`hstore` 支持 key 查询、包含判断、删除、拼接，并支持 GIN 和 GiST 索引。([PostgreSQL](https://www.postgresql.org/docs/17/hstore.html?utm_source=chatgpt.com))

安装扩展：

```sql
CREATE EXTENSION IF NOT EXISTS hstore;
```

创建表：

```sql
CREATE TABLE app.product_attribute (
    id BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    product_name VARCHAR(128) NOT NULL,
    attributes hstore NOT NULL DEFAULT ''::hstore,
    create_time TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);
```

插入键值数据：

```sql
INSERT INTO app.product_attribute (
    product_name,
    attributes
) VALUES (
    '机械键盘',
    'brand => KeyPro, color => black, switch => red'
);
```

查询指定 key：

```sql
SELECT
    id,
    product_name,
    attributes -> 'brand' AS brand,
    attributes -> 'color' AS color
FROM app.product_attribute;
```

判断 key 是否存在：

```sql
SELECT
    id,
    product_name,
    attributes
FROM app.product_attribute
WHERE attributes ? 'switch';
```

判断包含指定键值：

```sql
SELECT
    id,
    product_name,
    attributes
FROM app.product_attribute
WHERE attributes @> 'brand => KeyPro';
```

更新或追加 key：

```sql
UPDATE app.product_attribute
SET attributes = attributes || hstore('layout', '87')
WHERE product_name = '机械键盘';
```

删除 key：

```sql
UPDATE app.product_attribute
SET attributes = delete(attributes, 'switch')
WHERE product_name = '机械键盘';
```

创建 GIN 索引：

```sql
CREATE INDEX idx_product_attribute_attrs_gin
ON app.product_attribute
USING GIN (attributes);
```

使用建议如下：

| 场景                         | 建议                  |
| ---------------------------- | --------------------- |
| 简单键值扩展                 | 可以使用 `hstore`     |
| 值全部是文本                 | `hstore` 合适         |
| 需要嵌套结构                 | 使用 `jsonb`          |
| 需要数组、数字、布尔原生类型 | 使用 `jsonb`          |
| 新项目半结构化数据           | 通常优先 `jsonb`      |
| 历史系统兼容                 | 可能继续使用 `hstore` |

新项目中，`jsonb` 通常比 `hstore` 更通用。`hstore` 更适合简单文本 key/value，或者维护已有历史表结构。

### PostGIS

PostGIS 是 PostgreSQL 的空间数据库扩展，用于存储和查询地理空间数据，例如点、线、面、距离、范围、相交、包含、缓冲区、投影坐标等。PostGIS 官方文档说明，启用空间数据库通常执行 `CREATE EXTENSION postgis;`，并可用 `postgis_full_version()` 查看安装版本。([PostGIS](https://postgis.net/documentation/getting_started/?utm_source=chatgpt.com))

安装 PostGIS 扩展：

```sql
CREATE EXTENSION IF NOT EXISTS postgis;
```

查看 PostGIS 版本：

```sql
SELECT postgis_full_version();
```

查看可用 PostGIS 扩展：

```sql
SELECT
    name,
    default_version,
    installed_version,
    comment
FROM pg_available_extensions
WHERE name LIKE 'postgis%'
ORDER BY name;
```

创建门店位置表：

```sql
CREATE TABLE app.store_location (
    id BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    store_name VARCHAR(128) NOT NULL,
    address VARCHAR(255),
    location GEOGRAPHY(POINT, 4326) NOT NULL,
    create_time TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);
```

插入经纬度点位：

```sql
INSERT INTO app.store_location (
    store_name,
    address,
    location
) VALUES (
    '杭州西湖门店',
    '杭州市西湖区',
    ST_SetSRID(ST_MakePoint(120.130000, 30.260000), 4326)::geography
);
```

查询指定点附近 5 公里门店：

```sql
SELECT
    id,
    store_name,
    address,
    ST_Distance(
        location,
        ST_SetSRID(ST_MakePoint(120.150000, 30.250000), 4326)::geography
    ) AS distance_meter
FROM app.store_location
WHERE ST_DWithin(
    location,
    ST_SetSRID(ST_MakePoint(120.150000, 30.250000), 4326)::geography,
    5000
)
ORDER BY distance_meter ASC;
```

创建空间索引：

```sql
CREATE INDEX idx_store_location_geography
ON app.store_location
USING GIST (location);
```

使用 `geometry` 类型示例：

```sql
CREATE TABLE app.region_area (
    id BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    region_name VARCHAR(128) NOT NULL,
    area GEOMETRY(POLYGON, 4326) NOT NULL,
    create_time TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);
```

常见空间函数：

| 函数              | 说明                 |
| ----------------- | -------------------- |
| `ST_MakePoint`    | 创建点               |
| `ST_SetSRID`      | 设置坐标系 SRID      |
| `ST_Distance`     | 计算距离             |
| `ST_DWithin`      | 判断是否在指定距离内 |
| `ST_Intersects`   | 判断是否相交         |
| `ST_Contains`     | 判断是否包含         |
| `ST_AsText`       | 转为 WKT 文本        |
| `ST_GeomFromText` | 从 WKT 创建几何对象  |

`geometry` 和 `geography` 选择建议：

| 类型           | 适用场景                                  |
| -------------- | ----------------------------------------- |
| `geometry`     | 平面几何、投影坐标、复杂空间分析          |
| `geography`    | 经纬度球面距离计算，适合全球经纬度距离    |
| 简单经纬度展示 | 可用普通 `numeric` 字段，但空间查询能力弱 |
| 地图范围查询   | 使用 PostGIS 类型和 GiST 索引             |

PostGIS 使用建议如下：

| 建议                     | 说明                                       |
| ------------------------ | ------------------------------------------ |
| 生产安装前确认版本兼容   | PostGIS 需要系统层软件包支持               |
| 坐标系统一               | 常见经纬度使用 SRID 4326                   |
| 距离查询建 GiST 索引     | 否则空间查询可能很慢                       |
| 不要经纬度顺序写反       | `ST_MakePoint(longitude, latitude)`        |
| 大量空间计算先做范围过滤 | 减少参与精确计算的数据量                   |
| 复杂 GIS 系统要单独设计  | 涉及投影、拓扑、栅格、路径分析时需专业建模 |

### 扩展插件选择建议

扩展应服务于明确需求，不应因为“可能会用到”就提前启用。建议按照以下方式选择：

| 需求                       | 推荐扩展             |
| -------------------------- | -------------------- |
| 随机 UUID                  | `pgcrypto`           |
| 多版本 UUID 算法           | `uuid-ossp`          |
| SQL 性能统计               | `pg_stat_statements` |
| 跨 PostgreSQL 访问         | `postgres_fdw`       |
| 模糊搜索                   | `pg_trgm`            |
| 数组或 JSONB 组合 GIN 场景 | `btree_gin`          |
| 排他约束、时间段不重叠     | `btree_gist`         |
| 简单文本键值扩展           | `hstore`             |
| 地理空间数据               | `postgis`            |

扩展治理建议如下：

| 建议                 | 说明                                 |
| -------------------- | ------------------------------------ |
| 初始化脚本统一安装   | 保证开发、测试、生产一致             |
| 记录扩展版本         | 便于升级和故障排查                   |
| 权限最小化           | 普通应用账号不应安装或删除扩展       |
| 升级前测试           | 扩展升级可能影响函数、索引和数据类型 |
| 备份恢复检查依赖     | 目标环境必须安装对应扩展文件         |
| 删除扩展前检查依赖   | 避免误删业务对象                     |
| 扩展对象 Schema 规范 | 可迁移扩展建议放入 `ext` Schema      |

常用扩展初始化脚本可以放入数据库初始化或迁移脚本中：

```sql
-- PostgreSQL 常用扩展初始化
CREATE SCHEMA IF NOT EXISTS ext;

-- UUID、哈希和随机值
CREATE EXTENSION IF NOT EXISTS pgcrypto;

-- 模糊搜索
CREATE EXTENSION IF NOT EXISTS pg_trgm;

-- GIN / GiST 复合索引能力
CREATE EXTENSION IF NOT EXISTS btree_gin;
CREATE EXTENSION IF NOT EXISTS btree_gist;

-- 简单键值类型，按需启用
CREATE EXTENSION IF NOT EXISTS hstore;

-- 空间数据，只有 GIS 场景才启用
-- CREATE EXTENSION IF NOT EXISTS postgis;
```

扩展插件是 PostgreSQL 生态能力的重要组成部分。普通 Java 或 Spring Boot 业务系统中，最常用的是 `pgcrypto`、`pg_stat_statements`、`pg_trgm`、`btree_gist`；如果涉及跨库访问、空间数据或历史键值字段，再按需启用 `postgres_fdw`、`PostGIS` 或 `hstore`。


## 权限与安全

权限与安全用于控制谁可以连接数据库、访问哪些对象、执行哪些操作，以及如何保护敏感数据。PostgreSQL 的权限体系由角色、数据库权限、Schema 权限、对象权限、字段权限、函数权限、行级安全策略和连接认证共同组成。项目中应遵循最小权限原则，不建议应用直接使用超级用户或对象 Owner 账号连接数据库。

### 最小权限原则

最小权限原则是指每个账号只拥有完成当前职责所必需的权限。应用账号、只读账号、运维账号、报表账号、迁移账号应分开管理，避免一个账号拥有过多权限。

常见角色划分如下：

| 角色            | 用途           | 权限建议                         |
| --------------- | -------------- | -------------------------------- |
| `app_owner`     | 对象所有者     | 创建表、视图、函数、索引等对象   |
| `app_rw`        | 应用读写角色   | 查询、插入、更新、删除业务表     |
| `app_ro`        | 只读角色       | 查询表、视图、物化视图           |
| `app_migration` | 数据库迁移角色 | 执行 DDL 和迁移脚本              |
| `app_report`    | 报表角色       | 查询指定视图或统计表             |
| `app_admin`     | 管理角色       | 特定运维操作，不直接用于应用连接 |

创建角色示例：

```sql
-- 对象所有者，不用于应用连接
CREATE ROLE app_owner NOLOGIN;

-- 应用读写角色，不直接登录
CREATE ROLE app_rw NOLOGIN;

-- 应用只读角色，不直接登录
CREATE ROLE app_ro NOLOGIN;

-- 实际应用登录账号
CREATE ROLE app_user WITH LOGIN PASSWORD 'change_me_strong_password';

-- 把读写角色授予应用账号
GRANT app_rw TO app_user;
```

创建只读账号：

```sql
CREATE ROLE report_user WITH LOGIN PASSWORD 'change_me_report_password';

GRANT app_ro TO report_user;
```

最小权限建议如下：

| 建议                       | 说明                             |
| -------------------------- | -------------------------------- |
| 应用账号不要使用超级用户   | 避免误删库、误改系统对象         |
| 应用账号不要作为对象 Owner | Owner 权限过大，不利于权限隔离   |
| 读写账号和只读账号分离     | 报表、BI、查询工具使用只读账号   |
| DDL 和 DML 账号分离        | 应用运行账号不应随意执行 DDL     |
| 权限通过角色授予           | 不直接给个人账号逐个授权         |
| 定期审计权限               | 清理离职账号、临时账号和过期权限 |

### 数据库权限

数据库权限控制用户是否可以连接数据库、是否可以在数据库中创建 Schema、是否可以创建临时表等。常见权限包括 `CONNECT`、`CREATE` 和 `TEMPORARY`。

创建数据库并指定所有者：

```sql
CREATE DATABASE app_db OWNER app_owner;
```

授予连接数据库权限：

```sql
GRANT CONNECT ON DATABASE app_db TO app_rw;
GRANT CONNECT ON DATABASE app_db TO app_ro;
```

回收数据库创建权限：

```sql
REVOKE CREATE ON DATABASE app_db FROM PUBLIC;
```

授予迁移角色创建 Schema 的权限：

```sql
GRANT CREATE ON DATABASE app_db TO app_migration;
```

查看数据库权限：

```sql
SELECT
    datname AS database_name,
    datacl AS database_acl
FROM pg_database
WHERE datname = 'app_db';
```

数据库级权限说明：

| 权限                 | 说明                      |
| -------------------- | ------------------------- |
| `CONNECT`            | 允许连接数据库            |
| `CREATE`             | 允许在数据库中创建 Schema |
| `TEMPORARY` / `TEMP` | 允许创建临时表            |

数据库权限建议如下：

| 建议                       | 说明                         |
| -------------------------- | ---------------------------- |
| 普通用户只授予 `CONNECT`   | 不授予数据库级 `CREATE`      |
| 禁止 `PUBLIC` 默认扩散权限 | 回收不必要的公共权限         |
| 迁移账号单独授权           | DDL 权限集中在迁移角色       |
| 应用连接指定数据库         | 不允许应用账号访问无关数据库 |

### Schema 权限

Schema 是数据库对象的命名空间。即使用户有数据库连接权限，也不代表可以访问某个 Schema 中的对象。访问 Schema 通常需要 `USAGE` 权限，创建对象需要 `CREATE` 权限。

创建业务 Schema：

```sql
CREATE SCHEMA IF NOT EXISTS app AUTHORIZATION app_owner;
```

授予 Schema 使用权限：

```sql
GRANT USAGE ON SCHEMA app TO app_rw;
GRANT USAGE ON SCHEMA app TO app_ro;
```

授予迁移角色创建对象权限：

```sql
GRANT USAGE, CREATE ON SCHEMA app TO app_migration;
```

回收 public Schema 创建权限：

```sql
REVOKE CREATE ON SCHEMA public FROM PUBLIC;
```

查看 Schema 权限：

```sql
SELECT
    nspname AS schema_name,
    nspacl AS schema_acl
FROM pg_namespace
WHERE nspname IN ('app', 'public')
ORDER BY nspname;
```

设置默认权限，让未来创建的表自动授权：

```sql
ALTER DEFAULT PRIVILEGES FOR ROLE app_owner IN SCHEMA app
GRANT SELECT, INSERT, UPDATE, DELETE ON TABLES TO app_rw;

ALTER DEFAULT PRIVILEGES FOR ROLE app_owner IN SCHEMA app
GRANT SELECT ON TABLES TO app_ro;

ALTER DEFAULT PRIVILEGES FOR ROLE app_owner IN SCHEMA app
GRANT USAGE, SELECT ON SEQUENCES TO app_rw;
```

Schema 权限建议如下：

| 建议                      | 说明                                |
| ------------------------- | ----------------------------------- |
| 业务对象不要放在 `public` | 建议使用 `app`、`biz` 等业务 Schema |
| 应用角色需要 `USAGE`      | 否则即使表授权也无法访问            |
| 只有迁移角色有 `CREATE`   | 应用运行账号不要创建对象            |
| 配置默认权限              | 避免新增表后忘记授权                |
| 回收 public 创建权限      | 降低误创建对象和命名污染风险        |

### 表权限

表权限用于控制用户是否可以查询、插入、更新、删除、截断或引用表。常见权限包括 `SELECT`、`INSERT`、`UPDATE`、`DELETE`、`TRUNCATE`、`REFERENCES` 和 `TRIGGER`。

授予单表读写权限：

```sql
GRANT SELECT, INSERT, UPDATE, DELETE
ON TABLE app.sys_user
TO app_rw;
```

授予单表只读权限：

```sql
GRANT SELECT
ON TABLE app.sys_user
TO app_ro;
```

授予 Schema 下所有已有表权限：

```sql
GRANT SELECT, INSERT, UPDATE, DELETE
ON ALL TABLES IN SCHEMA app
TO app_rw;

GRANT SELECT
ON ALL TABLES IN SCHEMA app
TO app_ro;
```

回收删除权限：

```sql
REVOKE DELETE
ON TABLE app.sys_user
FROM app_rw;
```

查看表权限：

```sql
SELECT
    table_schema,
    table_name,
    privilege_type,
    grantee
FROM information_schema.role_table_grants
WHERE table_schema = 'app'
ORDER BY table_name, grantee, privilege_type;
```

表权限说明：

| 权限         | 说明         |
| ------------ | ------------ |
| `SELECT`     | 查询表数据   |
| `INSERT`     | 插入数据     |
| `UPDATE`     | 更新数据     |
| `DELETE`     | 删除数据     |
| `TRUNCATE`   | 清空表       |
| `REFERENCES` | 创建外键引用 |
| `TRIGGER`    | 创建触发器   |

表权限建议如下：

| 场景       | 建议                                              |
| ---------- | ------------------------------------------------- |
| 应用业务表 | 授予必要的 `SELECT`、`INSERT`、`UPDATE`、`DELETE` |
| 报表账号   | 只授予 `SELECT`                                   |
| 归档表     | 应用账号通常只读或无权限                          |
| 配置表     | 写权限可以只给后台服务                            |
| 审计日志表 | 应用可写，普通账号不可改不可删                    |
| 高危权限   | `TRUNCATE` 不建议授予普通应用账号                 |

### 字段权限

字段权限用于控制用户能否访问或更新表中的特定字段。它适合保护敏感字段，例如手机号、邮箱、身份证号、密码哈希、密钥等。

授予指定字段查询权限：

```sql
GRANT SELECT (
    id,
    username,
    nickname,
    enabled,
    create_time
)
ON app.sys_user
TO app_ro;
```

回收整表查询权限：

```sql
REVOKE SELECT
ON app.sys_user
FROM app_ro;
```

授予字段级更新权限：

```sql
GRANT UPDATE (
    nickname,
    avatar_url
)
ON app.sys_user
TO app_rw;
```

只允许报表账号查询脱敏视图，而不是原表敏感字段：

```sql
CREATE OR REPLACE VIEW app.v_sys_user_public AS
SELECT
    id,
    username,
    nickname,
    CASE
        WHEN phone IS NULL THEN NULL
        WHEN length(phone) < 7 THEN phone
        ELSE left(phone, 3) || '****' || right(phone, 4)
    END AS masked_phone,
    enabled,
    create_time
FROM app.sys_user
WHERE deleted = FALSE;

GRANT SELECT ON app.v_sys_user_public TO app_ro;
```

查看字段权限：

```sql
SELECT
    table_schema,
    table_name,
    column_name,
    privilege_type,
    grantee
FROM information_schema.column_privileges
WHERE table_schema = 'app'
ORDER BY table_name, column_name, grantee;
```

字段权限建议如下：

| 建议                     | 说明                         |
| ------------------------ | ---------------------------- |
| 敏感字段优先通过视图脱敏 | 比逐个字段授权更易维护       |
| 密码哈希不开放查询       | 应用层也不应返回该字段       |
| 字段级更新谨慎使用       | 复杂权限建议放应用层统一控制 |
| 报表账号只查视图         | 避免直接访问原始敏感字段     |
| 字段授权要文档化         | 否则排查权限问题较复杂       |

### 函数权限

函数权限控制用户是否可以执行函数或存储过程。函数可能读取敏感数据、修改数据、刷新物化视图或执行维护操作，因此应按角色授权。

回收函数公共执行权限：

```sql
REVOKE EXECUTE ON FUNCTION app.get_user_status_text(BIGINT) FROM PUBLIC;
```

授予指定角色执行权限：

```sql
GRANT EXECUTE ON FUNCTION app.get_user_status_text(BIGINT) TO app_rw;
GRANT EXECUTE ON FUNCTION app.get_user_status_text(BIGINT) TO app_ro;
```

授予过程执行权限：

```sql
GRANT EXECUTE ON PROCEDURE app.refresh_daily_order_stat() TO app_migration;
```

授予 Schema 下已有函数执行权限：

```sql
GRANT EXECUTE ON ALL FUNCTIONS IN SCHEMA app TO app_rw;
```

设置未来函数默认权限：

```sql
ALTER DEFAULT PRIVILEGES FOR ROLE app_owner IN SCHEMA app
GRANT EXECUTE ON FUNCTIONS TO app_rw;
```

查看函数权限：

```sql
SELECT
    n.nspname AS schema_name,
    p.proname AS function_name,
    pg_get_function_identity_arguments(p.oid) AS arguments,
    p.proacl AS privileges
FROM pg_proc p
JOIN pg_namespace n ON n.oid = p.pronamespace
WHERE n.nspname = 'app'
ORDER BY p.proname;
```

`SECURITY DEFINER` 函数需要特别谨慎，因为它会以函数所有者权限执行。

```sql
CREATE OR REPLACE FUNCTION app.safe_count_user()
RETURNS BIGINT
LANGUAGE sql
SECURITY DEFINER
SET search_path = app, pg_temp
AS $$
    SELECT COUNT(*)
    FROM sys_user
    WHERE deleted = FALSE;
$$;
```

函数权限建议如下：

| 建议                        | 说明                                    |
| --------------------------- | --------------------------------------- |
| 回收 `PUBLIC` 执行权限      | 防止所有用户默认可执行                  |
| 敏感函数单独授权            | 例如数据修复、刷新统计、归档过程        |
| 谨慎使用 `SECURITY DEFINER` | 防止权限放大                            |
| 固定 `search_path`          | `SECURITY DEFINER` 函数必须避免对象劫持 |
| 函数纳入审计                | 重要函数调用应有日志或应用侧审计        |

### 行级安全策略

行级安全策略，即 RLS，可以按行控制用户能看到或能修改哪些数据。它适合多租户隔离、部门数据隔离、用户只能访问自己数据等场景。

创建多租户订单表：

```sql
CREATE TABLE app.tenant_order (
    id BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    tenant_id BIGINT NOT NULL,
    order_no VARCHAR(64) NOT NULL,
    user_id BIGINT NOT NULL,
    total_amount NUMERIC(12, 2) NOT NULL,
    create_time TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uk_tenant_order_no UNIQUE (tenant_id, order_no)
);
```

启用行级安全：

```sql
ALTER TABLE app.tenant_order ENABLE ROW LEVEL SECURITY;
```

创建查询策略：

```sql
CREATE POLICY tenant_order_select_policy
ON app.tenant_order
FOR SELECT
USING (
    tenant_id = current_setting('app.current_tenant_id')::BIGINT
);
```

创建写入策略：

```sql
CREATE POLICY tenant_order_insert_policy
ON app.tenant_order
FOR INSERT
WITH CHECK (
    tenant_id = current_setting('app.current_tenant_id')::BIGINT
);
```

创建更新策略：

```sql
CREATE POLICY tenant_order_update_policy
ON app.tenant_order
FOR UPDATE
USING (
    tenant_id = current_setting('app.current_tenant_id')::BIGINT
)
WITH CHECK (
    tenant_id = current_setting('app.current_tenant_id')::BIGINT
);
```

应用连接后设置当前租户：

```sql
SET app.current_tenant_id = '10001';

SELECT
    id,
    tenant_id,
    order_no,
    total_amount
FROM app.tenant_order;
```

强制表所有者也遵守 RLS：

```sql
ALTER TABLE app.tenant_order FORCE ROW LEVEL SECURITY;
```

禁用 RLS：

```sql
ALTER TABLE app.tenant_order DISABLE ROW LEVEL SECURITY;
```

查看 RLS 策略：

```sql
SELECT
    schemaname,
    tablename,
    policyname,
    permissive,
    roles,
    cmd,
    qual,
    with_check
FROM pg_policies
WHERE schemaname = 'app'
ORDER BY tablename, policyname;
```

RLS 使用建议如下：

| 建议                   | 说明                                            |
| ---------------------- | ----------------------------------------------- |
| 多租户强隔离可使用 RLS | 数据库层兜底更安全                              |
| 应用层仍要做权限校验   | RLS 不是业务权限的全部                          |
| 策略要覆盖读写         | `SELECT`、`INSERT`、`UPDATE`、`DELETE` 分别考虑 |
| 当前租户变量要可靠设置 | 连接池复用时必须清理或重置                      |
| 性能需要验证           | RLS 条件会参与查询执行                          |
| 管理账号谨慎绕过       | 超级用户和特权角色可能绕过策略                  |

### 连接安全

连接安全包括网络访问控制、认证方式、SSL 加密、连接来源限制、账号权限和连接池配置。PostgreSQL 的连接认证主要由 `pg_hba.conf` 控制。

`pg_hba.conf` 示例：

文件位置：`$PGDATA/pg_hba.conf`

```conf
# TYPE  DATABASE  USER      ADDRESS          METHOD

# 本机连接使用安全密码认证
host    app_db    app_user  127.0.0.1/32     scram-sha-256

# 内网应用服务器访问
host    app_db    app_user  10.0.0.0/24      scram-sha-256

# 报表账号只允许从报表服务器访问
host    app_db    report_user 10.0.1.10/32   scram-sha-256

# 禁止其他来源访问
host    all       all       0.0.0.0/0        reject
```

修改后重载配置：

```sql
SELECT pg_reload_conf();
```

查看当前 SSL 状态：

```sql
SHOW ssl;
```

查看当前连接是否使用 SSL：

```sql
SELECT
    pid,
    usename,
    datname,
    client_addr,
    ssl,
    version,
    cipher
FROM pg_stat_ssl s
JOIN pg_stat_activity a ON a.pid = s.pid
WHERE a.pid = pg_backend_pid();
```

限制用户连接数：

```sql
ALTER ROLE app_user CONNECTION LIMIT 50;
```

设置数据库连接数：

```sql
ALTER DATABASE app_db CONNECTION LIMIT 200;
```

查看当前连接：

```sql
SELECT
    datname,
    usename,
    client_addr,
    application_name,
    state,
    COUNT(*) AS connection_count
FROM pg_stat_activity
GROUP BY
    datname,
    usename,
    client_addr,
    application_name,
    state
ORDER BY connection_count DESC;
```

连接安全建议如下：

| 建议                 | 说明                               |
| -------------------- | ---------------------------------- |
| 使用 `scram-sha-256` | 不建议使用明文或弱认证方式         |
| 限制来源 IP          | 不开放全网访问                     |
| 生产启用 SSL         | 跨网络访问应加密传输               |
| 应用账号限制连接数   | 防止异常耗尽数据库连接             |
| 使用连接池           | Spring Boot 常用 HikariCP          |
| 账号按系统区分       | 不同应用不要共用一个数据库账号     |
| 定期清理空闲连接     | 排查连接泄漏和 idle in transaction |

### 密码策略

密码策略用于保护数据库账号不被弱口令、泄露口令或长期未轮换口令影响。PostgreSQL 本身不提供完整的企业级密码复杂度策略，需要结合认证方式、运维规范、密钥管理系统和账号管理流程实现。

修改用户密码：

```sql
ALTER ROLE app_user WITH PASSWORD 'new_strong_password';
```

设置密码过期时间：

```sql
ALTER ROLE app_user VALID UNTIL '2026-12-31 23:59:59';
```

设置永不过期：

```sql
ALTER ROLE app_user VALID UNTIL 'infinity';
```

查看角色有效期：

```sql
SELECT
    rolname,
    rolcanlogin,
    rolvaliduntil
FROM pg_authid
WHERE rolcanlogin = TRUE
ORDER BY rolname;
```

普通用户通常无权查看 `pg_authid`。可以使用受限视图查看部分角色信息：

```sql
SELECT
    rolname,
    rolcanlogin
FROM pg_roles
WHERE rolcanlogin = TRUE
ORDER BY rolname;
```

密码加密方式配置：

文件位置：`postgresql.conf`

```conf
# 新密码默认使用 SCRAM-SHA-256 存储
password_encryption = scram-sha-256
```

密码策略建议如下：

| 建议                   | 说明                                 |
| ---------------------- | ------------------------------------ |
| 使用强密码             | 足够长度，包含大小写、数字、特殊字符 |
| 使用 `scram-sha-256`   | 比旧的 md5 更安全                    |
| 不在代码中硬编码密码   | 使用环境变量、配置中心或密钥管理系统 |
| 定期轮换密码           | 应用账号、报表账号、运维账号分开轮换 |
| 禁止共享账号           | 每个系统、每类用途使用独立账号       |
| 限制登录来源           | 密码正确也不能从任意网络连接         |
| 离职和项目下线清理账号 | 避免遗留风险                         |

### SQL 注入防护

SQL 注入是指攻击者通过拼接 SQL 参数改变原始 SQL 语义，从而读取、修改或删除非授权数据。防护核心是使用参数化查询，不拼接用户输入到 SQL 字符串中。

不推荐写法：

```sql
-- 不推荐：把用户输入直接拼接到 SQL 中
SELECT
    id,
    username,
    nickname
FROM app.sys_user
WHERE username = '${username}';
```

推荐参数化写法：

```sql
SELECT
    id,
    username,
    nickname
FROM app.sys_user
WHERE username = $1;
```

MyBatis 中推荐使用 `#{}`，不要使用 `${}` 拼接普通参数。

```xml
<select id="selectByUsername" resultType="io.github.atengk.user.entity.SysUser">
    SELECT
        id,
        username,
        nickname,
        phone,
        email
    FROM app.sys_user
    WHERE username = #{username}
      AND deleted = FALSE
</select>
```

动态排序字段不能直接信任前端输入，应使用白名单。

```sql
-- SQL 层示例：排序字段应由应用层白名单控制后再拼接
SELECT
    id,
    username,
    create_time
FROM app.sys_user
WHERE deleted = FALSE
ORDER BY create_time DESC
LIMIT 20;
```

SQL 注入防护建议如下：

| 建议                       | 说明                                 |
| -------------------------- | ------------------------------------ |
| 所有用户输入使用参数绑定   | 不拼接字符串                         |
| MyBatis 使用 `#{}`         | 普通参数不要用 `${}`                 |
| 排序字段使用白名单         | 字段名不能直接参数化，需要应用层校验 |
| 表名和列名不要来自用户输入 | 必须使用映射枚举                     |
| 限制数据库账号权限         | 即使注入成功也降低破坏范围           |
| 错误信息不要暴露 SQL 细节  | 避免泄露表结构                       |
| 日志脱敏                   | 不记录密码、Token、身份证等敏感值    |

### 敏感数据保护

敏感数据包括密码、手机号、邮箱、身份证号、银行卡号、地址、Token、密钥、个人隐私信息和商业数据。保护方式包括最小化采集、加密存储、脱敏展示、字段权限、审计日志和访问控制。

密码字段只存哈希，不存明文：

```sql
CREATE TABLE app.sys_user_auth (
    id BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    user_id BIGINT NOT NULL,
    password_hash TEXT NOT NULL,
    password_update_time TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    create_time TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uk_sys_user_auth_user_id UNIQUE (user_id)
);
```

脱敏视图示例：

```sql
CREATE OR REPLACE VIEW app.v_user_masked AS
SELECT
    id,
    username,
    nickname,
    CASE
        WHEN phone IS NULL THEN NULL
        WHEN length(phone) < 7 THEN phone
        ELSE left(phone, 3) || '****' || right(phone, 4)
    END AS phone,
    CASE
        WHEN email IS NULL THEN NULL
        ELSE regexp_replace(email, '(^.).*(@.*$)', '\1***\2')
    END AS email,
    enabled,
    create_time
FROM app.sys_user
WHERE deleted = FALSE;
```

只授予报表账号访问脱敏视图：

```sql
GRANT SELECT ON app.v_user_masked TO app_ro;

REVOKE SELECT ON app.sys_user FROM app_ro;
```

使用 `pgcrypto` 进行摘要：

```sql
CREATE EXTENSION IF NOT EXISTS pgcrypto;

SELECT
    encode(digest('sensitive-value', 'sha256'), 'hex') AS sha256_value;
```

记录访问审计表：

```sql
CREATE TABLE app.sensitive_access_log (
    id BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    operator_id BIGINT,
    access_type VARCHAR(64) NOT NULL,
    resource_type VARCHAR(64) NOT NULL,
    resource_id VARCHAR(128),
    access_reason VARCHAR(255),
    create_time TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);
```

敏感数据保护建议如下：

| 建议                     | 说明                                 |
| ------------------------ | ------------------------------------ |
| 不存不必要的敏感数据     | 最小化采集是第一原则                 |
| 密码只存哈希             | 不存明文，不可逆加密也不推荐用于密码 |
| 展示使用脱敏视图         | 报表和后台列表不要直接展示原值       |
| 导出数据要审计           | 敏感数据导出必须记录操作人和原因     |
| 日志不打印敏感值         | 密码、Token、密钥必须脱敏            |
| 备份文件同样加密和限权   | 备份往往包含完整敏感数据             |
| 使用字段权限和 RLS       | 数据库层增加兜底                     |
| 访问密钥使用密钥管理系统 | 不写入代码仓库                       |

## 备份与恢复

备份与恢复用于保证数据库在误操作、硬件故障、版本升级、迁移、数据损坏或安全事件后能够恢复。PostgreSQL 常见备份方式包括逻辑备份和物理备份。本章重点覆盖逻辑备份工具 `pg_dump`、`pg_restore` 和常见恢复流程。

### 逻辑备份

逻辑备份是把数据库对象和数据导出为 SQL 或归档格式。常用工具是 `pg_dump` 和 `pg_dumpall`。逻辑备份适合数据库迁移、跨版本升级、小中型数据库备份、表级备份和指定对象恢复。

逻辑备份特点如下：

| 特点           | 说明                                   |
| -------------- | -------------------------------------- |
| 可跨版本恢复   | 通常支持从低版本导入高版本             |
| 可选择对象     | 支持库级、Schema 级、表级备份          |
| 格式灵活       | SQL、custom、directory、tar            |
| 恢复粒度细     | custom 和 directory 格式可指定对象恢复 |
| 备份期间可读写 | `pg_dump` 使用一致性快照               |
| 大库恢复较慢   | 比物理备份更耗时                       |

常见备份格式：

| 格式      | 参数         | 说明                                     |
| --------- | ------------ | ---------------------------------------- |
| plain SQL | 默认或 `-Fp` | 文本 SQL，使用 `psql` 恢复               |
| custom    | `-Fc`        | 自定义归档，使用 `pg_restore` 恢复，推荐 |
| directory | `-Fd`        | 目录格式，可并行备份和恢复               |
| tar       | `-Ft`        | tar 归档格式                             |

逻辑备份建议如下：

| 场景               | 推荐                        |
| ------------------ | --------------------------- |
| 日常中小库备份     | `pg_dump -Fc`               |
| 大库并行备份       | `pg_dump -Fd -j`            |
| 只备份一个表       | `pg_dump -t`                |
| 只备份结构         | `--schema-only`             |
| 只备份数据         | `--data-only`               |
| 全实例角色和表空间 | `pg_dumpall --globals-only` |

### pg_dump 使用

`pg_dump` 用于备份单个数据库。它可以导出完整数据库、指定 Schema、指定表、仅结构、仅数据等。推荐生产备份使用 custom 格式，即 `-Fc`，便于后续用 `pg_restore` 灵活恢复。

备份整个数据库为 custom 格式：

```bash
pg_dump \
  -h 127.0.0.1 \
  -p 5432 \
  -U app_backup \
  -d app_db \
  -F c \
  -f /backup/app_db_20260509.dump
```

上面命令连接 `127.0.0.1:5432`，使用 `app_backup` 用户备份 `app_db` 数据库，`-F c` 表示 custom 格式，输出文件为 `/backup/app_db_20260509.dump`。

备份为 SQL 文件：

```bash
pg_dump \
  -h 127.0.0.1 \
  -p 5432 \
  -U app_backup \
  -d app_db \
  -F p \
  -f /backup/app_db_20260509.sql
```

备份指定 Schema：

```bash
pg_dump \
  -h 127.0.0.1 \
  -p 5432 \
  -U app_backup \
  -d app_db \
  -F c \
  -n app \
  -f /backup/app_schema_20260509.dump
```

备份指定表：

```bash
pg_dump \
  -h 127.0.0.1 \
  -p 5432 \
  -U app_backup \
  -d app_db \
  -F c \
  -t app.sys_user \
  -f /backup/sys_user_20260509.dump
```

只备份表结构：

```bash
pg_dump \
  -h 127.0.0.1 \
  -p 5432 \
  -U app_backup \
  -d app_db \
  --schema-only \
  -F c \
  -f /backup/app_db_schema_20260509.dump
```

只备份数据：

```bash
pg_dump \
  -h 127.0.0.1 \
  -p 5432 \
  -U app_backup \
  -d app_db \
  --data-only \
  -F c \
  -f /backup/app_db_data_20260509.dump
```

使用目录格式并行备份：

```bash
pg_dump \
  -h 127.0.0.1 \
  -p 5432 \
  -U app_backup \
  -d app_db \
  -F d \
  -j 4 \
  -f /backup/app_db_20260509_dir
```

常用参数说明：

| 参数            | 说明                           |
| --------------- | ------------------------------ |
| `-h`            | 数据库主机                     |
| `-p`            | 数据库端口                     |
| `-U`            | 用户名                         |
| `-d`            | 数据库名                       |
| `-F c`          | custom 格式                    |
| `-F p`          | plain SQL 格式                 |
| `-F d`          | directory 格式                 |
| `-f`            | 输出文件或目录                 |
| `-n`            | 指定 Schema                    |
| `-t`            | 指定表                         |
| `--schema-only` | 只备份结构                     |
| `--data-only`   | 只备份数据                     |
| `-j`            | 并行任务数，directory 格式常用 |

安全建议：不要在命令行直接写密码。可以使用 `.pgpass` 文件。

文件位置：`~/.pgpass`

```text
127.0.0.1:5432:app_db:app_backup:your_strong_password
```

设置权限：

```bash
chmod 600 ~/.pgpass
```

`.pgpass` 权限必须足够严格，否则 PostgreSQL 客户端工具会忽略该文件。

### pg_restore 使用

`pg_restore` 用于恢复 `pg_dump` 生成的 custom、directory、tar 格式备份。它不能恢复 plain SQL 文件；SQL 文件应使用 `psql` 执行。

查看备份文件内容：

```bash
pg_restore \
  -l \
  /backup/app_db_20260509.dump
```

恢复到已有数据库：

```bash
pg_restore \
  -h 127.0.0.1 \
  -p 5432 \
  -U app_backup \
  -d app_db_restore \
  /backup/app_db_20260509.dump
```

恢复前清理对象：

```bash
pg_restore \
  -h 127.0.0.1 \
  -p 5432 \
  -U app_backup \
  -d app_db_restore \
  --clean \
  --if-exists \
  /backup/app_db_20260509.dump
```

并行恢复：

```bash
pg_restore \
  -h 127.0.0.1 \
  -p 5432 \
  -U app_backup \
  -d app_db_restore \
  -j 4 \
  /backup/app_db_20260509.dump
```

只恢复指定表：

```bash
pg_restore \
  -h 127.0.0.1 \
  -p 5432 \
  -U app_backup \
  -d app_db_restore \
  -t app.sys_user \
  /backup/app_db_20260509.dump
```

只恢复结构：

```bash
pg_restore \
  -h 127.0.0.1 \
  -p 5432 \
  -U app_backup \
  -d app_db_restore \
  --schema-only \
  /backup/app_db_20260509.dump
```

只恢复数据：

```bash
pg_restore \
  -h 127.0.0.1 \
  -p 5432 \
  -U app_backup \
  -d app_db_restore \
  --data-only \
  /backup/app_db_20260509.dump
```

把 custom 备份转换成 SQL 文件：

```bash
pg_restore \
  -f /backup/app_db_20260509_restore.sql \
  /backup/app_db_20260509.dump
```

常用参数说明：

| 参数            | 说明                     |
| --------------- | ------------------------ |
| `-l`            | 列出归档内容             |
| `-d`            | 指定恢复目标数据库       |
| `--clean`       | 恢复前删除已有对象       |
| `--if-exists`   | 删除对象时加 `IF EXISTS` |
| `--schema-only` | 只恢复结构               |
| `--data-only`   | 只恢复数据               |
| `-t`            | 只恢复指定表             |
| `-n`            | 只恢复指定 Schema        |
| `-j`            | 并行恢复                 |
| `--no-owner`    | 不恢复对象 Owner         |
| `--role`        | 恢复时设置执行角色       |

恢复建议如下：

| 建议                          | 说明                         |
| ----------------------------- | ---------------------------- |
| 先恢复到临时库验证            | 不直接覆盖生产               |
| 使用 `pg_restore -l` 检查内容 | 确认备份包含目标对象         |
| 大库使用并行恢复              | directory 或 custom 格式支持 |
| 跨环境恢复常用 `--no-owner`   | 避免目标环境角色不一致       |
| 恢复后执行验证 SQL            | 检查表数量、关键数据和约束   |

### 数据库级备份

数据库级备份是对单个数据库完整导出，包括 Schema、表、数据、索引、约束、视图、函数等对象。它适合日常备份、版本升级前备份、迁移前备份和生产数据留档。

推荐使用 custom 格式备份：

```bash
pg_dump \
  -h 127.0.0.1 \
  -p 5432 \
  -U app_backup \
  -d app_db \
  -F c \
  -f /backup/app_db_$(date +%Y%m%d_%H%M%S).dump
```

如果需要压缩级别：

```bash
pg_dump \
  -h 127.0.0.1 \
  -p 5432 \
  -U app_backup \
  -d app_db \
  -F c \
  -Z 6 \
  -f /backup/app_db_$(date +%Y%m%d_%H%M%S).dump
```

备份全局对象，例如角色和表空间：

```bash
pg_dumpall \
  -h 127.0.0.1 \
  -p 5432 \
  -U postgres \
  --globals-only \
  -f /backup/globals_$(date +%Y%m%d_%H%M%S).sql
```

恢复全局对象：

```bash
psql \
  -h 127.0.0.1 \
  -p 5432 \
  -U postgres \
  -f /backup/globals_20260509_120000.sql
```

数据库级恢复流程：

```bash
createdb \
  -h 127.0.0.1 \
  -p 5432 \
  -U postgres \
  app_db_restore

pg_restore \
  -h 127.0.0.1 \
  -p 5432 \
  -U postgres \
  -d app_db_restore \
  -j 4 \
  /backup/app_db_20260509_120000.dump
```

数据库级备份建议：

| 建议                     | 说明                       |
| ------------------------ | -------------------------- |
| 备份数据库和全局对象     | 角色权限不完全在单库备份中 |
| 使用时间戳命名           | 便于追踪备份时间           |
| 备份文件只读保存         | 防止误覆盖                 |
| 异地保存                 | 防止单机磁盘故障           |
| 恢复到临时库验证         | 确认备份可用               |
| 记录备份 PostgreSQL 版本 | 跨版本恢复要验证兼容性     |

### 表级备份

表级备份用于导出指定业务表，适合数据修复前留档、单表迁移、误操作恢复和局部数据分析。表级备份通常使用 `pg_dump -t`。

备份单表：

```bash
pg_dump \
  -h 127.0.0.1 \
  -p 5432 \
  -U app_backup \
  -d app_db \
  -F c \
  -t app.sys_user \
  -f /backup/app_sys_user_20260509.dump
```

备份多个表：

```bash
pg_dump \
  -h 127.0.0.1 \
  -p 5432 \
  -U app_backup \
  -d app_db \
  -F c \
  -t app.sys_user \
  -t app.sys_role \
  -t app.sys_user_role \
  -f /backup/app_user_tables_20260509.dump
```

只备份表数据：

```bash
pg_dump \
  -h 127.0.0.1 \
  -p 5432 \
  -U app_backup \
  -d app_db \
  -F c \
  --data-only \
  -t app.sys_user \
  -f /backup/app_sys_user_data_20260509.dump
```

恢复单表到临时库：

```bash
pg_restore \
  -h 127.0.0.1 \
  -p 5432 \
  -U app_backup \
  -d app_db_restore \
  -t app.sys_user \
  /backup/app_sys_user_20260509.dump
```

使用 SQL 临时备份表：

```sql
CREATE TABLE app.sys_user_backup_20260509 AS
SELECT *
FROM app.sys_user;
```

恢复部分数据：

```sql
INSERT INTO app.sys_user (
    id,
    username,
    nickname,
    phone,
    email,
    enabled,
    deleted,
    create_time,
    update_time
)
SELECT
    id,
    username,
    nickname,
    phone,
    email,
    enabled,
    deleted,
    create_time,
    update_time
FROM app.sys_user_backup_20260509
WHERE id = 10001
ON CONFLICT (id) DO NOTHING;
```

表级备份建议如下：

| 场景           | 推荐                                         |
| -------------- | -------------------------------------------- |
| 数据修复前备份 | `CREATE TABLE ... AS SELECT` 或 `pg_dump -t` |
| 跨库迁移单表   | `pg_dump -t`                                 |
| 临时留档       | 备份表加日期后缀                             |
| 大表局部数据   | 可用 `COPY` 或条件导出                       |
| 生产恢复       | 先恢复到临时库，再比对导入                   |

### 全量恢复

全量恢复是把完整数据库备份恢复到目标数据库。常见场景包括灾难恢复、环境克隆、迁移验证、版本升级回退和生产问题复盘。全量恢复前必须确认目标数据库是否可以被覆盖。

从 custom 备份恢复到新库：

```bash
createdb \
  -h 127.0.0.1 \
  -p 5432 \
  -U postgres \
  app_db_restore

pg_restore \
  -h 127.0.0.1 \
  -p 5432 \
  -U postgres \
  -d app_db_restore \
  -j 4 \
  /backup/app_db_20260509.dump
```

恢复 SQL 格式备份：

```bash
createdb \
  -h 127.0.0.1 \
  -p 5432 \
  -U postgres \
  app_db_restore

psql \
  -h 127.0.0.1 \
  -p 5432 \
  -U postgres \
  -d app_db_restore \
  -f /backup/app_db_20260509.sql
```

覆盖恢复已有库时，先断开连接：

```sql
SELECT pg_terminate_backend(pid)
FROM pg_stat_activity
WHERE datname = 'app_db_restore'
  AND pid <> pg_backend_pid();
```

删除并重建目标库：

```bash
dropdb \
  -h 127.0.0.1 \
  -p 5432 \
  -U postgres \
  app_db_restore

createdb \
  -h 127.0.0.1 \
  -p 5432 \
  -U postgres \
  app_db_restore
```

恢复后执行统计信息更新：

```sql
ANALYZE;
```

全量恢复后验证：

```sql
SELECT
    schemaname,
    relname AS table_name,
    n_live_tup AS estimated_rows
FROM pg_stat_user_tables
ORDER BY schemaname, relname;
```

全量恢复建议如下：

| 建议                   | 说明                         |
| ---------------------- | ---------------------------- |
| 优先恢复到新库验证     | 避免直接覆盖生产             |
| 记录恢复开始和结束时间 | 便于评估 RTO                 |
| 恢复前确认角色和扩展   | 缺角色或扩展会导致失败       |
| 恢复后执行 `ANALYZE`   | 帮助优化器生成合理计划       |
| 恢复后检查对象数量     | 表、索引、视图、函数是否完整 |
| 恢复后运行应用回归     | 只看恢复成功不够             |

### 指定对象恢复

指定对象恢复用于从备份中恢复某个表、Schema、函数或数据。custom 和 directory 格式更适合指定对象恢复，因为可以通过 `pg_restore` 选择对象。

查看备份对象列表：

```bash
pg_restore \
  -l \
  /backup/app_db_20260509.dump \
  > /backup/app_db_20260509.list
```

编辑 list 文件，只保留需要恢复的对象，然后执行：

```bash
pg_restore \
  -h 127.0.0.1 \
  -p 5432 \
  -U postgres \
  -d app_db_restore \
  -L /backup/app_db_20260509.list \
  /backup/app_db_20260509.dump
```

恢复指定 Schema：

```bash
pg_restore \
  -h 127.0.0.1 \
  -p 5432 \
  -U postgres \
  -d app_db_restore \
  -n app \
  /backup/app_db_20260509.dump
```

恢复指定表：

```bash
pg_restore \
  -h 127.0.0.1 \
  -p 5432 \
  -U postgres \
  -d app_db_restore \
  -t app.sys_user \
  /backup/app_db_20260509.dump
```

只恢复指定表数据：

```bash
pg_restore \
  -h 127.0.0.1 \
  -p 5432 \
  -U postgres \
  -d app_db_restore \
  --data-only \
  -t app.sys_user \
  /backup/app_db_20260509.dump
```

推荐的指定对象恢复流程：

| 步骤             | 说明                   |
| ---------------- | ---------------------- |
| 恢复到临时库     | 不直接恢复到生产       |
| 对比目标数据     | 确认需要恢复的行或对象 |
| 生成修复 SQL     | 从临时库导出目标数据   |
| 在生产事务中执行 | 可回滚、可验证         |
| 执行后检查       | 核对数量和关键字段     |

从临时库恢复单条数据的思路：

```sql
-- 在临时库中查询目标数据
SELECT *
FROM app.sys_user
WHERE id = 10001;
```

在生产库中执行修复：

```sql
BEGIN;

INSERT INTO app.sys_user (
    id,
    username,
    nickname,
    phone,
    email,
    enabled,
    deleted,
    create_time,
    update_time
) VALUES (
    10001,
    'admin',
    '系统管理员',
    '13800000000',
    'admin@example.com',
    TRUE,
    FALSE,
    TIMESTAMP '2026-05-09 10:00:00',
    TIMESTAMP '2026-05-09 10:00:00'
)
ON CONFLICT (id) DO UPDATE
SET
    username = EXCLUDED.username,
    nickname = EXCLUDED.nickname,
    phone = EXCLUDED.phone,
    email = EXCLUDED.email,
    enabled = EXCLUDED.enabled,
    deleted = EXCLUDED.deleted,
    update_time = CURRENT_TIMESTAMP;

COMMIT;
```

指定对象恢复建议如下：

| 建议                 | 说明                   |
| -------------------- | ---------------------- |
| 优先恢复到临时库     | 避免误覆盖生产         |
| custom 格式更灵活    | 支持对象级恢复         |
| 表恢复要考虑外键依赖 | 单表数据可能依赖其他表 |
| 序列要同步校准       | 恢复主键后检查序列值   |
| 生产修复要事务化     | 便于失败回滚           |
| 操作前再备份目标表   | 防止二次误操作         |

恢复表数据后校准序列：

```sql
SELECT setval(
    pg_get_serial_sequence('app.sys_user', 'id'),
    COALESCE((SELECT MAX(id) FROM app.sys_user), 1),
    true
);
```

如果是 `IDENTITY` 字段，可以使用：

```sql
SELECT setval(
    pg_get_serial_sequence('app.sys_user', 'id'),
    COALESCE((SELECT MAX(id) FROM app.sys_user), 1),
    true
);
```

### 备份脚本设计

备份脚本应做到可配置、可审计、可压缩、可清理、可失败退出，并且避免明文密码出现在脚本中。生产备份脚本建议配合定时任务、监控告警和异地同步。

下面脚本用于按日期备份 PostgreSQL 数据库，并保留最近 7 天备份文件。

文件位置：`/opt/scripts/backup_postgresql.sh`

```bash
#!/usr/bin/env bash
set -euo pipefail

# 数据库连接配置
PG_HOST="127.0.0.1"
PG_PORT="5432"
PG_USER="app_backup"
PG_DATABASE="app_db"

# 备份目录和文件名
BACKUP_DIR="/backup/postgresql"
BACKUP_DATE="$(date +%Y%m%d_%H%M%S)"
BACKUP_FILE="${BACKUP_DIR}/${PG_DATABASE}_${BACKUP_DATE}.dump"

# 保留天数
RETENTION_DAYS=7

# 创建备份目录
mkdir -p "${BACKUP_DIR}"

echo "[INFO] 开始备份数据库：${PG_DATABASE}"

pg_dump \
  -h "${PG_HOST}" \
  -p "${PG_PORT}" \
  -U "${PG_USER}" \
  -d "${PG_DATABASE}" \
  -F c \
  -Z 6 \
  -f "${BACKUP_FILE}"

echo "[INFO] 备份完成：${BACKUP_FILE}"

# 生成校验文件
sha256sum "${BACKUP_FILE}" > "${BACKUP_FILE}.sha256"
echo "[INFO] 校验文件已生成：${BACKUP_FILE}.sha256"

# 清理过期备份
find "${BACKUP_DIR}" \
  -type f \
  -name "${PG_DATABASE}_*.dump" \
  -mtime +"${RETENTION_DAYS}" \
  -print \
  -delete

find "${BACKUP_DIR}" \
  -type f \
  -name "${PG_DATABASE}_*.dump.sha256" \
  -mtime +"${RETENTION_DAYS}" \
  -print \
  -delete

echo "[INFO] 过期备份清理完成，保留天数：${RETENTION_DAYS}"
```

赋予执行权限：

```bash
chmod +x /opt/scripts/backup_postgresql.sh
```

手动执行：

```bash
/opt/scripts/backup_postgresql.sh
```

配置定时任务：

```bash
crontab -e
```

每天凌晨 2 点执行备份：

```cron
0 2 * * * /opt/scripts/backup_postgresql.sh >> /var/log/backup_postgresql.log 2>&1
```

脚本说明如下：

| 配置项           | 说明                 |
| ---------------- | -------------------- |
| `PG_HOST`        | 数据库主机           |
| `PG_PORT`        | 数据库端口           |
| `PG_USER`        | 备份账号             |
| `PG_DATABASE`    | 要备份的数据库       |
| `BACKUP_DIR`     | 备份文件目录         |
| `RETENTION_DAYS` | 本地备份保留天数     |
| `pg_dump -F c`   | 使用 custom 格式备份 |
| `sha256sum`      | 生成文件校验值       |
| `find -mtime`    | 清理过期备份         |

备份脚本建议如下：

| 建议                    | 说明                     |
| ----------------------- | ------------------------ |
| 使用专用备份账号        | 不使用超级用户或应用账号 |
| 使用 `.pgpass` 管理密码 | 不在脚本中写明文密码     |
| 生成校验文件            | 用于验证备份文件完整性   |
| 保留日志                | 方便排查备份失败         |
| 设置失败告警            | cron 失败要通知          |
| 异地同步                | 本机备份不能防止磁盘故障 |
| 定期恢复演练            | 没有验证过的备份不可靠   |

### 备份验证

备份验证用于确认备份文件存在、完整、可恢复、数据正确。只生成备份文件并不代表备份有效，必须定期恢复到临时环境进行验证。

检查备份文件：

```bash
ls -lh /backup/postgresql/app_db_20260509_020000.dump
```

校验 SHA-256：

```bash
sha256sum -c /backup/postgresql/app_db_20260509_020000.dump.sha256
```

查看 custom 备份内容：

```bash
pg_restore \
  -l \
  /backup/postgresql/app_db_20260509_020000.dump \
  | head -n 50
```

恢复到临时数据库：

```bash
createdb \
  -h 127.0.0.1 \
  -p 5432 \
  -U postgres \
  app_db_verify

pg_restore \
  -h 127.0.0.1 \
  -p 5432 \
  -U postgres \
  -d app_db_verify \
  -j 4 \
  /backup/postgresql/app_db_20260509_020000.dump
```

验证表数量：

```sql
SELECT
    COUNT(*) AS table_count
FROM information_schema.tables
WHERE table_schema = 'app'
  AND table_type = 'BASE TABLE';
```

验证关键表数据量：

```sql
SELECT
    'sys_user' AS table_name,
    COUNT(*) AS row_count
FROM app.sys_user
UNION ALL
SELECT
    'order_info' AS table_name,
    COUNT(*) AS row_count
FROM app.order_info
UNION ALL
SELECT
    'order_item' AS table_name,
    COUNT(*) AS row_count
FROM app.order_item;
```

验证核心约束是否存在：

```sql
SELECT
    constraint_schema,
    table_name,
    constraint_name,
    constraint_type
FROM information_schema.table_constraints
WHERE constraint_schema = 'app'
ORDER BY table_name, constraint_name;
```

验证扩展是否恢复：

```sql
SELECT
    extname,
    extversion
FROM pg_extension
ORDER BY extname;
```

验证序列是否正常：

```sql
SELECT
    schemaname,
    sequencename,
    last_value
FROM pg_sequences
WHERE schemaname = 'app'
ORDER BY sequencename;
```

验证完成后删除临时库：

```bash
dropdb \
  -h 127.0.0.1 \
  -p 5432 \
  -U postgres \
  app_db_verify
```

备份验证清单如下：

| 检查项     | 验证方式                                    |
| ---------- | ------------------------------------------- |
| 文件存在   | `ls -lh`                                    |
| 文件完整   | `sha256sum -c`                              |
| 归档可读取 | `pg_restore -l`                             |
| 可以恢复   | 恢复到临时库                                |
| 表对象完整 | 查询 `information_schema.tables`            |
| 数据量合理 | 对比关键表行数                              |
| 约束完整   | 查询 `information_schema.table_constraints` |
| 扩展存在   | 查询 `pg_extension`                         |
| 序列正常   | 查询 `pg_sequences`                         |
| 应用可连接 | 使用应用或脚本做核心接口验证                |

备份策略建议如下：

| 策略               | 说明                          |
| ------------------ | ----------------------------- |
| 每日全量逻辑备份   | 中小库常用                    |
| 重要操作前临时备份 | DDL、批量修复、版本升级前执行 |
| 定期恢复演练       | 至少按月或按季度验证          |
| 本地 + 异地保存    | 避免单点故障                  |
| 设置保留周期       | 如 7 天、30 天、180 天        |
| 敏感备份加密       | 备份文件包含完整数据          |
| 明确 RPO 和 RTO    | 确认最多丢多少数据、多久恢复  |

备份的价值只有在恢复成功时才成立。生产环境应把备份、校验、异地同步、恢复演练和告警作为一套完整流程，而不是只执行 `pg_dump`。

## 数据导入与导出

数据导入与导出用于数据迁移、初始化、备份抽取、报表导出、离线分析和批量数据处理。PostgreSQL 提供 `COPY`、`\copy`、`INSERT`、外部表、FDW、程序批处理等多种方式。实际项目中，小批量数据可以使用 `INSERT`，中大批量数据优先使用 `COPY` 或 `\copy`。

### COPY 导入

`COPY FROM` 用于从文件导入数据到表中，性能通常明显优于逐条 `INSERT`。需要注意，SQL 形式的 `COPY` 读取的是数据库服务器所在机器上的文件，不是客户端机器上的文件。

准备示例表：

```sql
CREATE TABLE app.import_user (
    id BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    username VARCHAR(64) NOT NULL,
    nickname VARCHAR(64),
    phone VARCHAR(20),
    email VARCHAR(128),
    enabled BOOLEAN NOT NULL DEFAULT TRUE,
    create_time TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);
```

从服务器文件导入 CSV 数据：

```sql
COPY app.import_user (
    username,
    nickname,
    phone,
    email,
    enabled
)
FROM '/data/import/import_user.csv'
WITH (
    FORMAT csv,
    HEADER true,
    DELIMITER ',',
    QUOTE '"',
    ESCAPE '"',
    ENCODING 'UTF8'
);
```

CSV 文件示例：

```csv
username,nickname,phone,email,enabled
admin,系统管理员,13800000000,admin@example.com,true
zhangsan,张三,13800000001,zhangsan@example.com,true
lisi,李四,13800000002,lisi@example.com,false
```

如果文件在客户端本机，而不是数据库服务器上，应使用 `psql` 的 `\copy`：

```sql
\copy app.import_user(username, nickname, phone, email, enabled)
FROM './import_user.csv'
WITH (FORMAT csv, HEADER true, DELIMITER ',', ENCODING 'UTF8');
```

`COPY` 与 `\copy` 对比如下：

| 命令         | 文件位置     | 执行方            | 常见用途           |
| ------------ | ------------ | ----------------- | ------------------ |
| `COPY FROM`  | 数据库服务器 | PostgreSQL 服务端 | 服务器本地批量导入 |
| `\copy FROM` | 客户端机器   | `psql` 客户端     | 本地文件导入数据库 |

导入建议如下：

| 建议                | 说明                                 |
| ------------------- | ------------------------------------ |
| 大数据量优先 `COPY` | 比多条 `INSERT` 更高效               |
| 明确字段列表        | 避免表结构变化影响导入               |
| 先导入临时表        | 再清洗、校验、写入正式表             |
| 导入前检查编码      | 推荐统一使用 UTF-8                   |
| 布尔值使用标准格式  | `true` / `false`、`t` / `f`          |
| 时间格式要统一      | 避免隐式转换失败                     |
| 文件路径注意权限    | 服务端 `COPY` 需要数据库进程可读文件 |

### COPY 导出

`COPY TO` 用于把查询结果或表数据导出到文件。SQL 形式的 `COPY TO` 写入数据库服务器所在机器上的文件。

导出整表数据：

```sql
COPY app.import_user (
    id,
    username,
    nickname,
    phone,
    email,
    enabled,
    create_time
)
TO '/data/export/import_user.csv'
WITH (
    FORMAT csv,
    HEADER true,
    DELIMITER ',',
    QUOTE '"',
    ESCAPE '"',
    ENCODING 'UTF8'
);
```

导出查询结果：

```sql
COPY (
    SELECT
        id,
        username,
        nickname,
        phone,
        email,
        enabled,
        create_time
    FROM app.import_user
    WHERE enabled = TRUE
    ORDER BY id
)
TO '/data/export/enabled_user.csv'
WITH (
    FORMAT csv,
    HEADER true,
    ENCODING 'UTF8'
);
```

导出到客户端本机文件：

```sql
\copy (
    SELECT
        id,
        username,
        nickname,
        phone,
        email,
        enabled,
        create_time
    FROM app.import_user
    WHERE enabled = TRUE
    ORDER BY id
)
TO './enabled_user.csv'
WITH (FORMAT csv, HEADER true, ENCODING 'UTF8');
```

导出为制表符分隔文件：

```sql
COPY app.import_user (
    id,
    username,
    nickname,
    phone,
    email
)
TO '/data/export/import_user.tsv'
WITH (
    FORMAT csv,
    HEADER true,
    DELIMITER E'\t',
    ENCODING 'UTF8'
);
```

导出建议如下：

| 场景           | 推荐方式                                |
| -------------- | --------------------------------------- |
| 导出服务器文件 | `COPY TO`                               |
| 导出本地文件   | `\copy TO`                              |
| 导出报表数据   | `COPY (SELECT ...) TO`                  |
| 导出全表       | 明确字段列表后 `COPY table(columns) TO` |
| 导出大数据     | 分批导出或按时间范围导出                |
| 敏感数据导出   | 使用脱敏查询或视图导出                  |

### CSV 导入

CSV 是最常见的数据交换格式。PostgreSQL 导入 CSV 时，需要重点关注字段顺序、表头、空值、分隔符、引号、编码和日期格式。

创建导入目标表：

```sql
CREATE TABLE app.product_import (
    id BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    product_code VARCHAR(64) NOT NULL,
    product_name VARCHAR(128) NOT NULL,
    sale_price NUMERIC(12, 2) NOT NULL,
    enabled BOOLEAN NOT NULL DEFAULT TRUE,
    create_time TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uk_product_import_code UNIQUE (product_code)
);
```

CSV 文件示例：

```csv
product_code,product_name,sale_price,enabled
P10001,机械键盘,199.90,true
P10002,无线鼠标,89.00,true
P10003,显示器,1299.00,false
```

使用 `\copy` 导入客户端 CSV：

```sql
\copy app.product_import(product_code, product_name, sale_price, enabled)
FROM './product_import.csv'
WITH (
    FORMAT csv,
    HEADER true,
    DELIMITER ',',
    QUOTE '"',
    ESCAPE '"',
    NULL '',
    ENCODING 'UTF8'
);
```

如果 CSV 中用 `\N` 表示空值：

```sql
\copy app.product_import(product_code, product_name, sale_price, enabled)
FROM './product_import.csv'
WITH (
    FORMAT csv,
    HEADER true,
    NULL '\N',
    ENCODING 'UTF8'
);
```

导入到临时表再清洗：

```sql
CREATE TEMP TABLE tmp_product_import (
    product_code TEXT,
    product_name TEXT,
    sale_price TEXT,
    enabled TEXT
);

\copy tmp_product_import(product_code, product_name, sale_price, enabled)
FROM './product_import.csv'
WITH (FORMAT csv, HEADER true, ENCODING 'UTF8');

INSERT INTO app.product_import (
    product_code,
    product_name,
    sale_price,
    enabled
)
SELECT
    trim(product_code),
    trim(product_name),
    sale_price::NUMERIC(12, 2),
    COALESCE(enabled::BOOLEAN, TRUE)
FROM tmp_product_import
WHERE product_code IS NOT NULL
  AND trim(product_code) <> '';
```

CSV 导入常见问题：

| 问题               | 处理方式                         |
| ------------------ | -------------------------------- |
| 字段数量不匹配     | 检查字段列表和 CSV 列数          |
| 编码错误           | 统一使用 UTF-8                   |
| 空字符串转数值失败 | 先导入临时表，使用 `NULLIF` 清洗 |
| 日期格式不一致     | 先导入文本字段，再显式转换       |
| 唯一约束冲突       | 导入临时表后使用 `ON CONFLICT`   |
| 布尔值格式错误     | 转换为 `true` / `false`          |

使用 `ON CONFLICT` 合并导入：

```sql
INSERT INTO app.product_import (
    product_code,
    product_name,
    sale_price,
    enabled
)
SELECT
    trim(product_code),
    trim(product_name),
    NULLIF(trim(sale_price), '')::NUMERIC(12, 2),
    COALESCE(NULLIF(trim(enabled), '')::BOOLEAN, TRUE)
FROM tmp_product_import
ON CONFLICT (product_code) DO UPDATE
SET
    product_name = EXCLUDED.product_name,
    sale_price = EXCLUDED.sale_price,
    enabled = EXCLUDED.enabled;
```

### CSV 导出

CSV 导出常用于报表下载、数据分析、离线处理和系统间交换。导出时应明确字段、排序、时间范围和脱敏规则。

导出商品数据：

```sql
\copy (
    SELECT
        product_code,
        product_name,
        sale_price,
        enabled,
        to_char(create_time, 'YYYY-MM-DD HH24:MI:SS') AS create_time
    FROM app.product_import
    ORDER BY product_code
)
TO './product_export.csv'
WITH (
    FORMAT csv,
    HEADER true,
    DELIMITER ',',
    QUOTE '"',
    ESCAPE '"',
    ENCODING 'UTF8'
);
```

导出订单报表：

```sql
\copy (
    SELECT
        o.order_no,
        u.username,
        u.nickname,
        o.order_status,
        o.total_amount,
        to_char(o.create_time, 'YYYY-MM-DD HH24:MI:SS') AS create_time
    FROM app.order_info o
    INNER JOIN app.sys_user u ON u.id = o.user_id
    WHERE o.create_time >= TIMESTAMP '2026-05-01 00:00:00'
      AND o.create_time < TIMESTAMP '2026-06-01 00:00:00'
    ORDER BY o.create_time DESC, o.id DESC
)
TO './order_report_202605.csv'
WITH (FORMAT csv, HEADER true, ENCODING 'UTF8');
```

导出脱敏用户数据：

```sql
\copy (
    SELECT
        id,
        username,
        nickname,
        CASE
            WHEN phone IS NULL THEN NULL
            WHEN length(phone) < 7 THEN phone
            ELSE left(phone, 3) || '****' || right(phone, 4)
        END AS phone,
        enabled,
        to_char(create_time, 'YYYY-MM-DD HH24:MI:SS') AS create_time
    FROM app.sys_user
    WHERE deleted = FALSE
    ORDER BY id
)
TO './user_masked_export.csv'
WITH (FORMAT csv, HEADER true, ENCODING 'UTF8');
```

CSV 导出建议如下：

| 建议             | 说明                               |
| ---------------- | ---------------------------------- |
| 明确字段列表     | 不使用 `SELECT *`                  |
| 固定排序         | 保证多次导出结果稳定               |
| 大数据按范围导出 | 如按日期、ID 分批                  |
| 时间字段格式化   | 便于 Excel 或下游系统识别          |
| 敏感字段脱敏     | 手机号、邮箱、身份证等不要直接导出 |
| 导出行为审计     | 生产敏感数据导出应记录操作人和原因 |

### INSERT 批量导入

`INSERT` 批量导入适合小批量数据、初始化字典、配置数据、测试数据和应用程序批处理。数据量较大时，`COPY` 通常更合适。

多行 `VALUES` 插入：

```sql
INSERT INTO app.sys_config (
    config_key,
    config_value,
    remark
) VALUES
    ('system.name', '后台管理系统', '系统名称'),
    ('system.theme', 'dark', '系统主题'),
    ('system.locale', 'zh-CN', '默认语言');
```

批量 UPSERT：

```sql
INSERT INTO app.sys_config (
    config_key,
    config_value,
    remark
) VALUES
    ('system.name', 'PostgreSQL 管理系统', '系统名称'),
    ('system.theme', 'dark', '系统主题'),
    ('system.locale', 'zh-CN', '默认语言')
ON CONFLICT (config_key) DO UPDATE
SET
    config_value = EXCLUDED.config_value,
    remark = EXCLUDED.remark,
    update_time = CURRENT_TIMESTAMP;
```

从临时表导入正式表：

```sql
INSERT INTO app.product_import (
    product_code,
    product_name,
    sale_price,
    enabled
)
SELECT
    product_code,
    product_name,
    sale_price,
    enabled
FROM tmp_product_import_cleaned
ON CONFLICT (product_code) DO UPDATE
SET
    product_name = EXCLUDED.product_name,
    sale_price = EXCLUDED.sale_price,
    enabled = EXCLUDED.enabled;
```

分批插入建议：

| 场景                   | 建议                          |
| ---------------------- | ----------------------------- |
| 几十到几百条初始化数据 | 多行 `VALUES`                 |
| 几千到几十万行         | 应用批处理或 `COPY`           |
| 百万级以上             | 优先 `COPY`、临时表、分区导入 |
| 有唯一冲突处理         | 使用 `ON CONFLICT`            |
| 数据需要清洗           | 先导入临时表，再转换入正式表  |

应用程序批量写入时，应使用 JDBC batch、MyBatis 批处理或框架批量接口，避免循环逐条提交。

### 外部数据导入

外部数据导入可以来自远程 PostgreSQL、CSV 文件、其他数据库、对象存储、ETL 工具或应用程序。PostgreSQL 常见方式包括 `postgres_fdw`、`file_fdw`、`COPY`、程序读取后批量写入等。

使用 `postgres_fdw` 从远程 PostgreSQL 导入：

```sql
CREATE EXTENSION IF NOT EXISTS postgres_fdw;

CREATE SERVER remote_pg_server
FOREIGN DATA WRAPPER postgres_fdw
OPTIONS (
    host '192.168.1.100',
    port '5432',
    dbname 'remote_db'
);

CREATE USER MAPPING FOR app_user
SERVER remote_pg_server
OPTIONS (
    user 'remote_user',
    password 'remote_password'
);
```

创建外部表：

```sql
CREATE FOREIGN TABLE app.remote_product (
    product_code VARCHAR(64),
    product_name VARCHAR(128),
    sale_price NUMERIC(12, 2),
    enabled BOOLEAN
)
SERVER remote_pg_server
OPTIONS (
    schema_name 'public',
    table_name 'product'
);
```

从远程表导入本地表：

```sql
INSERT INTO app.product_import (
    product_code,
    product_name,
    sale_price,
    enabled
)
SELECT
    product_code,
    product_name,
    sale_price,
    enabled
FROM app.remote_product
ON CONFLICT (product_code) DO UPDATE
SET
    product_name = EXCLUDED.product_name,
    sale_price = EXCLUDED.sale_price,
    enabled = EXCLUDED.enabled;
```

使用 `file_fdw` 读取服务器 CSV 文件：

```sql
CREATE EXTENSION IF NOT EXISTS file_fdw;

CREATE SERVER file_server
FOREIGN DATA WRAPPER file_fdw;

CREATE FOREIGN TABLE app.product_csv_fdw (
    product_code TEXT,
    product_name TEXT,
    sale_price TEXT,
    enabled TEXT
)
SERVER file_server
OPTIONS (
    filename '/data/import/product_import.csv',
    format 'csv',
    header 'true',
    delimiter ',',
    encoding 'UTF8'
);
```

查询外部 CSV：

```sql
SELECT
    product_code,
    product_name,
    sale_price,
    enabled
FROM app.product_csv_fdw;
```

外部数据导入建议如下：

| 来源            | 推荐方式                                   |
| --------------- | ------------------------------------------ |
| 远程 PostgreSQL | `postgres_fdw` 或 `pg_dump` / `pg_restore` |
| 本地 CSV        | `\copy`                                    |
| 服务器 CSV      | `COPY` 或 `file_fdw`                       |
| 其他数据库      | ETL 工具、应用程序、FDW                    |
| 大批量离线数据  | 先落 CSV，再 `COPY`                        |
| 数据清洗复杂    | 临时表 + SQL 清洗 + 正式表写入             |

### 大数据量导入优化

大数据量导入的目标是减少单行写入成本、减少索引和约束维护成本、控制事务大小、降低 WAL 压力，并保证导入后数据可校验。

常见优化手段如下：

| 优化方向           | 说明                       |
| ------------------ | -------------------------- |
| 使用 `COPY`        | 比逐条 `INSERT` 快         |
| 先导入临时表       | 清洗后再写入正式表         |
| 分批导入           | 控制事务大小和锁时间       |
| 暂缓创建索引       | 大量导入后再建索引通常更快 |
| 使用 `UNLOGGED` 表 | 降低 WAL，但崩溃后不可恢复 |
| 关闭无关触发器     | 导入前评估副作用，谨慎操作 |
| 调整维护参数       | 如 `maintenance_work_mem`  |
| 导入后 `ANALYZE`   | 更新统计信息               |
| 使用分区表         | 按分区导入和维护           |

使用 `UNLOGGED` 中间表：

```sql
CREATE UNLOGGED TABLE app.product_import_stage (
    product_code TEXT,
    product_name TEXT,
    sale_price TEXT,
    enabled TEXT
);
```

导入到中间表：

```sql
COPY app.product_import_stage (
    product_code,
    product_name,
    sale_price,
    enabled
)
FROM '/data/import/product_import.csv'
WITH (FORMAT csv, HEADER true, ENCODING 'UTF8');
```

清洗后写入正式表：

```sql
INSERT INTO app.product_import (
    product_code,
    product_name,
    sale_price,
    enabled
)
SELECT
    trim(product_code),
    trim(product_name),
    NULLIF(trim(sale_price), '')::NUMERIC(12, 2),
    COALESCE(NULLIF(trim(enabled), '')::BOOLEAN, TRUE)
FROM app.product_import_stage
WHERE product_code IS NOT NULL
  AND trim(product_code) <> ''
ON CONFLICT (product_code) DO UPDATE
SET
    product_name = EXCLUDED.product_name,
    sale_price = EXCLUDED.sale_price,
    enabled = EXCLUDED.enabled;
```

导入后更新统计信息：

```sql
ANALYZE app.product_import;
```

导入前后创建索引策略：

```sql
-- 大批量首次导入时，可以先导入数据，再创建索引
CREATE INDEX idx_product_import_name
ON app.product_import (product_name);
```

临时禁用用户触发器，谨慎使用：

```sql
ALTER TABLE app.product_import DISABLE TRIGGER USER;

-- 执行大批量导入

ALTER TABLE app.product_import ENABLE TRIGGER USER;
```

大数据量导入建议如下：

| 建议                   | 说明                       |
| ---------------------- | -------------------------- |
| 优先 `COPY`            | 批量导入首选               |
| 使用中间表             | 降低脏数据影响正式表       |
| 导入后校验数量         | 对比源文件行数和目标表行数 |
| 导入后执行 `ANALYZE`   | 避免执行计划不准           |
| 生产环境控制导入时间   | 避免高峰期影响业务         |
| 不盲目禁用约束和触发器 | 必须有补偿校验             |
| 大表分区导入           | 可按月份或批次分区导入     |

## 日志与监控

日志与监控用于发现慢查询、连接异常、锁等待、长事务、表膨胀、索引失效、缓存命中率下降等问题。PostgreSQL 提供数据库日志、统计视图、扩展插件和系统表用于运维分析。生产环境应同时配置日志、监控指标、告警规则和定期巡检 SQL。

### 数据库日志配置

PostgreSQL 日志配置通常在 `postgresql.conf` 中完成。日志应记录时间、进程号、用户、数据库、应用名、客户端地址和 SQL 相关信息，便于排查问题。

常用日志配置如下：

```conf
# 开启日志收集器
logging_collector = on

# 日志目录，相对于 data_directory
log_directory = 'log'

# 日志文件名
log_filename = 'postgresql-%Y-%m-%d_%H%M%S.log'

# 日志轮转
log_rotation_age = 1d
log_rotation_size = 100MB

# 日志格式前缀
log_line_prefix = '%m [%p] user=%u db=%d app=%a client=%h '

# 日志时区
log_timezone = 'Asia/Shanghai'

# 记录锁等待
log_lock_waits = on

# 死锁检测等待时间
deadlock_timeout = '1s'

# 记录执行超过指定时间的 SQL
log_min_duration_statement = 1000

# 记录连接和断开连接
log_connections = on
log_disconnections = on
```

修改配置后重载：

```sql
SELECT pg_reload_conf();
```

查看当前日志相关配置：

```sql
SELECT
    name,
    setting,
    unit,
    context,
    short_desc
FROM pg_settings
WHERE name LIKE 'log_%'
   OR name IN ('logging_collector', 'deadlock_timeout')
ORDER BY name;
```

日志配置建议如下：

| 配置                         | 建议                               |
| ---------------------------- | ---------------------------------- |
| `logging_collector`          | 生产建议开启                       |
| `log_line_prefix`            | 必须包含用户、库、应用名、客户端   |
| `log_min_duration_statement` | 按系统情况设置，如 500ms、1000ms   |
| `log_lock_waits`             | 生产建议开启                       |
| `log_connections`            | 连接异常排查时有用，但日志量会增加 |
| `log_rotation_age`           | 建议按天轮转                       |
| `log_rotation_size`          | 防止单文件过大                     |

日志不要无限保留，应配合日志系统或定时清理策略。

### 慢查询日志

慢查询日志用于记录执行时间超过阈值的 SQL。它适合定位偶发慢 SQL、全表扫描、锁等待、排序落盘和异常请求。

开启慢查询日志：

```conf
# 记录执行超过 1 秒的 SQL
log_min_duration_statement = 1000
```

临时设置当前会话慢查询阈值：

```sql
SET log_min_duration_statement = 500;
```

记录所有 SQL，通常只用于临时排查，不建议长期生产开启：

```sql
SET log_min_duration_statement = 0;
```

关闭慢查询记录：

```sql
SET log_min_duration_statement = -1;
```

配合 `pg_stat_statements` 分析累计慢 SQL：

```sql
CREATE EXTENSION IF NOT EXISTS pg_stat_statements;
```

查询累计执行时间最高的 SQL：

```sql
SELECT
    query,
    calls,
    total_exec_time,
    mean_exec_time,
    max_exec_time,
    rows
FROM pg_stat_statements
ORDER BY total_exec_time DESC
LIMIT 20;
```

查询平均耗时最高的 SQL：

```sql
SELECT
    query,
    calls,
    mean_exec_time,
    max_exec_time,
    rows
FROM pg_stat_statements
WHERE calls >= 10
ORDER BY mean_exec_time DESC
LIMIT 20;
```

慢查询分析建议如下：

| 指标       | 说明                                       |
| ---------- | ------------------------------------------ |
| 单次最慢   | 关注 `max_exec_time`                       |
| 累计最耗时 | 关注 `total_exec_time`                     |
| 高频调用   | 关注 `calls`                               |
| 平均耗时   | 关注 `mean_exec_time`                      |
| 返回行数   | 关注是否返回过多数据                       |
| 磁盘读取   | 关注 `shared_blks_read`                    |
| 临时文件   | 关注 `temp_blks_read`、`temp_blks_written` |

慢查询治理流程：

| 步骤       | 说明                          |
| ---------- | ----------------------------- |
| 找 SQL     | 慢日志或 `pg_stat_statements` |
| 看执行计划 | `EXPLAIN ANALYZE`             |
| 判断瓶颈   | 扫描、排序、连接、锁等待、IO  |
| 优化 SQL   | 改写条件、分页、预聚合        |
| 优化索引   | 建联合、表达式、部分索引      |
| 验证效果   | 再次执行计划和压测            |
| 持续观察   | 发布后观察慢查询和资源指标    |

### 连接数监控

连接数监控用于发现连接池配置不合理、连接泄漏、空闲事务、突发流量和数据库连接耗尽问题。

查看最大连接数：

```sql
SHOW max_connections;
```

查看当前连接数：

```sql
SELECT
    COUNT(*) AS connection_count
FROM pg_stat_activity;
```

按数据库和用户统计连接：

```sql
SELECT
    datname,
    usename,
    application_name,
    state,
    COUNT(*) AS connection_count
FROM pg_stat_activity
GROUP BY
    datname,
    usename,
    application_name,
    state
ORDER BY connection_count DESC;
```

查看活跃连接：

```sql
SELECT
    pid,
    usename,
    datname,
    application_name,
    client_addr,
    state,
    now() - backend_start AS connection_age,
    now() - query_start AS query_duration,
    query
FROM pg_stat_activity
WHERE state <> 'idle'
ORDER BY query_duration DESC;
```

查看空闲事务连接：

```sql
SELECT
    pid,
    usename,
    datname,
    application_name,
    client_addr,
    state,
    now() - xact_start AS transaction_duration,
    query
FROM pg_stat_activity
WHERE state = 'idle in transaction'
ORDER BY transaction_duration DESC;
```

连接数告警建议：

| 指标                | 建议                                 |
| ------------------- | ------------------------------------ |
| 总连接数            | 超过 `max_connections` 的 70% 应关注 |
| 活跃连接数          | 长时间高位说明数据库压力大           |
| idle in transaction | 应设置告警，通常是异常事务           |
| 单应用连接数        | 超出连接池预期需要排查               |
| 等待连接            | 应用连接池等待超时需要联动分析       |

连接数优化建议如下：

| 建议           | 说明                           |
| -------------- | ------------------------------ |
| 使用连接池     | Java 常用 HikariCP             |
| 控制最大池大小 | 不要每个服务都配置过大连接池   |
| 设置连接超时   | 防止请求无限等待               |
| 避免长事务     | 长事务占用连接和锁             |
| 排查连接泄漏   | 关注连接长时间 idle 或 active  |
| 使用 PgBouncer | 大量短连接场景可考虑连接池代理 |

### 事务监控

事务监控用于发现长事务、空闲未提交事务、事务回滚过多、数据库年龄增长等问题。长事务会影响 VACUUM 清理旧版本，可能导致表膨胀。

查看长事务：

```sql
SELECT
    pid,
    usename,
    datname,
    application_name,
    client_addr,
    state,
    now() - xact_start AS transaction_duration,
    now() - query_start AS query_duration,
    wait_event_type,
    wait_event,
    query
FROM pg_stat_activity
WHERE xact_start IS NOT NULL
ORDER BY transaction_duration DESC;
```

查看空闲未提交事务：

```sql
SELECT
    pid,
    usename,
    datname,
    application_name,
    client_addr,
    now() - xact_start AS transaction_duration,
    query
FROM pg_stat_activity
WHERE state = 'idle in transaction'
ORDER BY transaction_duration DESC;
```

按数据库查看提交和回滚次数：

```sql
SELECT
    datname,
    xact_commit,
    xact_rollback,
    ROUND(
        xact_rollback::NUMERIC
        / NULLIF(xact_commit + xact_rollback, 0)
        * 100,
        2
    ) AS rollback_ratio_percent
FROM pg_stat_database
ORDER BY rollback_ratio_percent DESC NULLS LAST;
```

查看数据库事务年龄：

```sql
SELECT
    datname,
    age(datfrozenxid) AS xid_age
FROM pg_database
ORDER BY xid_age DESC;
```

事务监控建议如下：

| 指标                | 风险                             |
| ------------------- | -------------------------------- |
| 长事务              | 阻碍 VACUUM，增加表膨胀          |
| idle in transaction | 持有锁和快照，常见异常           |
| 回滚率过高          | 应用错误、约束冲突或事务设计问题 |
| xid age 过高        | 存在事务 ID 回卷风险             |
| 活跃事务过多        | 数据库并发压力大                 |

事务规范建议：

| 建议                   | 说明                                       |
| ---------------------- | ------------------------------------------ |
| 应用事务要短           | 不在事务内做远程调用                       |
| 查询接口不要打开写事务 | 只读查询不应长期占用事务                   |
| 设置事务超时           | 可用 `idle_in_transaction_session_timeout` |
| 监控回滚率             | 持续升高要排查应用异常                     |
| 批处理分批提交         | 避免超大事务                               |

设置空闲事务超时：

```conf
idle_in_transaction_session_timeout = '60s'
```

### 锁监控

锁监控用于发现 SQL 阻塞、DDL 阻塞业务、批量更新锁表、死锁和长事务持锁等问题。

查看当前锁等待：

```sql
SELECT
    pid,
    usename,
    datname,
    application_name,
    client_addr,
    wait_event_type,
    wait_event,
    now() - query_start AS query_duration,
    query
FROM pg_stat_activity
WHERE wait_event_type = 'Lock'
ORDER BY query_duration DESC;
```

查看阻塞关系：

```sql
SELECT
    blocked.pid AS blocked_pid,
    blocked.usename AS blocked_user,
    blocked.application_name AS blocked_app,
    now() - blocked.query_start AS blocked_duration,
    blocked.query AS blocked_query,
    blocking.pid AS blocking_pid,
    blocking.usename AS blocking_user,
    blocking.application_name AS blocking_app,
    now() - blocking.query_start AS blocking_duration,
    blocking.query AS blocking_query
FROM pg_stat_activity blocked
JOIN pg_locks blocked_locks
    ON blocked_locks.pid = blocked.pid
JOIN pg_locks blocking_locks
    ON blocking_locks.locktype = blocked_locks.locktype
   AND blocking_locks.database IS NOT DISTINCT FROM blocked_locks.database
   AND blocking_locks.relation IS NOT DISTINCT FROM blocked_locks.relation
   AND blocking_locks.page IS NOT DISTINCT FROM blocked_locks.page
   AND blocking_locks.tuple IS NOT DISTINCT FROM blocked_locks.tuple
   AND blocking_locks.virtualxid IS NOT DISTINCT FROM blocked_locks.virtualxid
   AND blocking_locks.transactionid IS NOT DISTINCT FROM blocked_locks.transactionid
   AND blocking_locks.classid IS NOT DISTINCT FROM blocked_locks.classid
   AND blocking_locks.objid IS NOT DISTINCT FROM blocked_locks.objid
   AND blocking_locks.objsubid IS NOT DISTINCT FROM blocked_locks.objsubid
   AND blocking_locks.pid <> blocked_locks.pid
JOIN pg_stat_activity blocking
    ON blocking.pid = blocking_locks.pid
WHERE NOT blocked_locks.granted
  AND blocking_locks.granted
ORDER BY blocked_duration DESC;
```

查看指定表锁：

```sql
SELECT
    l.pid,
    a.usename,
    a.application_name,
    l.locktype,
    l.mode,
    l.granted,
    c.relname AS table_name,
    a.query
FROM pg_locks l
LEFT JOIN pg_class c ON c.oid = l.relation
LEFT JOIN pg_stat_activity a ON a.pid = l.pid
WHERE c.relname = 'order_info'
ORDER BY l.granted, l.mode;
```

必要时终止阻塞会话，执行前必须确认业务影响：

```sql
SELECT pg_terminate_backend(12345);
```

锁监控建议如下：

| 指标         | 说明                                   |
| ------------ | -------------------------------------- |
| 锁等待数量   | 持续增加说明阻塞严重                   |
| 最长等待时间 | 影响接口响应和批任务                   |
| 阻塞源状态   | 如果是 `idle in transaction`，通常异常 |
| 阻塞 SQL     | 重点分析是否 DDL、大更新、大删除       |
| 锁类型       | 表锁、行锁、事务锁要区分               |

锁冲突预防建议：

| 建议                | 说明                 |
| ------------------- | -------------------- |
| DDL 低峰执行        | 避免强锁阻塞业务     |
| 大批量更新分批      | 控制锁范围和事务时间 |
| 更新条件命中索引    | 避免扫描和锁定大量行 |
| 固定加锁顺序        | 降低死锁概率         |
| 设置 `lock_timeout` | 避免无限等待         |

### 表膨胀监控

PostgreSQL 使用 MVCC，`UPDATE` 和 `DELETE` 会产生旧版本行。VACUUM 会清理无效行，但如果长事务阻碍清理，或更新删除频繁，表和索引可能膨胀。

查看表的死元组数量：

```sql
SELECT
    schemaname,
    relname AS table_name,
    n_live_tup,
    n_dead_tup,
    ROUND(
        n_dead_tup::NUMERIC / NULLIF(n_live_tup + n_dead_tup, 0) * 100,
        2
    ) AS dead_tuple_ratio_percent,
    last_vacuum,
    last_autovacuum,
    last_analyze,
    last_autoanalyze
FROM pg_stat_user_tables
ORDER BY n_dead_tup DESC;
```

查看表大小：

```sql
SELECT
    schemaname,
    relname AS table_name,
    pg_size_pretty(pg_total_relation_size(relid)) AS total_size,
    pg_size_pretty(pg_relation_size(relid)) AS table_size,
    pg_size_pretty(pg_indexes_size(relid)) AS indexes_size
FROM pg_stat_user_tables
ORDER BY pg_total_relation_size(relid) DESC;
```

查看自动 VACUUM 情况：

```sql
SELECT
    schemaname,
    relname AS table_name,
    vacuum_count,
    autovacuum_count,
    analyze_count,
    autoanalyze_count,
    last_autovacuum,
    last_autoanalyze
FROM pg_stat_user_tables
ORDER BY autovacuum_count DESC;
```

手动执行 VACUUM：

```sql
VACUUM ANALYZE app.order_info;
```

查看详细 VACUUM 信息：

```sql
VACUUM VERBOSE ANALYZE app.order_info;
```

完全回收空间，生产环境谨慎使用，因为会加较强锁：

```sql
VACUUM FULL app.order_info;
```

表膨胀处理建议如下：

| 场景           | 建议                              |
| -------------- | --------------------------------- |
| 死元组较多     | 检查 autovacuum 是否正常          |
| 长事务存在     | 先处理长事务                      |
| 大量更新删除   | 分批执行，降低单次压力            |
| 表空间持续增长 | 检查更新模式和索引数量            |
| 急需回收磁盘   | 低峰期评估 `VACUUM FULL` 或重建表 |
| 高频更新表     | 调整 autovacuum 参数和 fillfactor |

针对单表设置 autovacuum 参数：

```sql
ALTER TABLE app.order_info SET (
    autovacuum_vacuum_scale_factor = 0.05,
    autovacuum_analyze_scale_factor = 0.05,
    autovacuum_vacuum_threshold = 1000,
    autovacuum_analyze_threshold = 1000
);
```

### 索引使用监控

索引使用监控用于发现长期未使用索引、重复索引、低效索引和索引体积过大的问题。索引可以提升查询性能，但会增加写入成本和存储成本。

查看索引使用统计：

```sql
SELECT
    schemaname,
    relname AS table_name,
    indexrelname AS index_name,
    idx_scan,
    idx_tup_read,
    idx_tup_fetch
FROM pg_stat_user_indexes
ORDER BY idx_scan ASC, relname, indexrelname;
```

查看索引大小：

```sql
SELECT
    schemaname,
    tablename,
    indexname,
    pg_size_pretty(
        pg_relation_size(format('%I.%I', schemaname, indexname)::regclass)
    ) AS index_size
FROM pg_indexes
WHERE schemaname = 'app'
ORDER BY
    pg_relation_size(format('%I.%I', schemaname, indexname)::regclass) DESC;
```

查找可能长期未使用的索引：

```sql
SELECT
    s.schemaname,
    s.relname AS table_name,
    s.indexrelname AS index_name,
    s.idx_scan,
    pg_size_pretty(pg_relation_size(s.indexrelid)) AS index_size
FROM pg_stat_user_indexes s
JOIN pg_index i ON i.indexrelid = s.indexrelid
WHERE s.idx_scan = 0
  AND NOT i.indisprimary
  AND NOT i.indisunique
ORDER BY pg_relation_size(s.indexrelid) DESC;
```

查看表扫描情况：

```sql
SELECT
    schemaname,
    relname AS table_name,
    seq_scan,
    seq_tup_read,
    idx_scan,
    idx_tup_fetch,
    ROUND(
        idx_scan::NUMERIC / NULLIF(seq_scan + idx_scan, 0) * 100,
        2
    ) AS index_scan_ratio_percent
FROM pg_stat_user_tables
ORDER BY seq_scan DESC;
```

索引监控建议如下：

| 指标            | 说明                         |
| --------------- | ---------------------------- |
| `idx_scan`      | 索引扫描次数                 |
| `idx_tup_read`  | 从索引读取的索引项数量       |
| `idx_tup_fetch` | 通过索引回表读取的行数量     |
| 索引大小        | 大索引要关注收益             |
| 未使用索引      | 需要结合业务周期判断能否删除 |
| `seq_scan`      | 大表顺序扫描过多需要分析     |

索引治理建议：

| 建议                           | 说明                           |
| ------------------------------ | ------------------------------ |
| 不立即删除 `idx_scan = 0` 索引 | 可能服务低频月报或约束         |
| 删除前记录 DDL                 | 便于回滚                       |
| 唯一索引和主键索引不能随意删   | 可能保证业务正确性             |
| 检查重复索引                   | 联合索引和单列索引可能重复     |
| 结合慢 SQL 判断                | 监控数据只是线索，不是最终结论 |
| 发布后观察                     | 删除索引后要监控慢查询变化     |

### 缓存命中率

缓存命中率反映数据从 PostgreSQL 共享缓冲区读取的比例。命中率低可能表示工作集大于内存、查询扫描数据过多、索引设计不合理或系统 IO 压力大。

查看数据库缓存命中率：

```sql
SELECT
    datname,
    blks_read,
    blks_hit,
    ROUND(
        blks_hit::NUMERIC / NULLIF(blks_hit + blks_read, 0) * 100,
        2
    ) AS cache_hit_ratio_percent
FROM pg_stat_database
WHERE datname IS NOT NULL
ORDER BY cache_hit_ratio_percent ASC NULLS LAST;
```

查看表级缓存命中率：

```sql
SELECT
    schemaname,
    relname AS table_name,
    heap_blks_read,
    heap_blks_hit,
    ROUND(
        heap_blks_hit::NUMERIC / NULLIF(heap_blks_hit + heap_blks_read, 0) * 100,
        2
    ) AS heap_cache_hit_ratio_percent
FROM pg_statio_user_tables
ORDER BY heap_cache_hit_ratio_percent ASC NULLS LAST;
```

查看索引缓存命中率：

```sql
SELECT
    schemaname,
    relname AS table_name,
    indexrelname AS index_name,
    idx_blks_read,
    idx_blks_hit,
    ROUND(
        idx_blks_hit::NUMERIC / NULLIF(idx_blks_hit + idx_blks_read, 0) * 100,
        2
    ) AS index_cache_hit_ratio_percent
FROM pg_statio_user_indexes
ORDER BY index_cache_hit_ratio_percent ASC NULLS LAST;
```

缓存命中率建议如下：

| 指标             | 说明                     |
| ---------------- | ------------------------ |
| 数据库整体命中率 | 长期偏低要关注内存和 SQL |
| 表级命中率       | 找出频繁读磁盘的大表     |
| 索引命中率       | 判断热点索引是否能被缓存 |
| `blks_read`      | 从磁盘读取块数           |
| `blks_hit`       | 从共享缓冲区命中块数     |

缓存命中率优化方向：

| 问题             | 优化方式                   |
| ---------------- | -------------------------- |
| 全表扫描多       | 优化索引和查询条件         |
| 返回数据过大     | 增加分页、减少字段         |
| 热点数据大于内存 | 增加内存或拆分冷热数据     |
| 索引过大         | 清理无用索引，优化联合索引 |
| 报表查询冲击缓存 | 报表走只读库或离线库       |
| 随机读过多       | 优化查询和数据访问模式     |

缓存命中率不能单独作为性能结论，应结合 SQL 执行计划、磁盘 IO、系统内存和业务访问模式分析。

### pg_stat_activity

`pg_stat_activity` 是最常用的运行时监控视图，用于查看当前连接、正在执行的 SQL、等待事件、事务状态、客户端地址和应用名。

查看当前所有会话：

```sql
SELECT
    pid,
    usename,
    datname,
    application_name,
    client_addr,
    state,
    wait_event_type,
    wait_event,
    backend_start,
    xact_start,
    query_start,
    query
FROM pg_stat_activity
ORDER BY query_start NULLS LAST;
```

查看正在执行的 SQL：

```sql
SELECT
    pid,
    usename,
    datname,
    application_name,
    client_addr,
    now() - query_start AS query_duration,
    wait_event_type,
    wait_event,
    query
FROM pg_stat_activity
WHERE state = 'active'
ORDER BY query_duration DESC;
```

查看长时间 SQL：

```sql
SELECT
    pid,
    usename,
    datname,
    application_name,
    now() - query_start AS query_duration,
    query
FROM pg_stat_activity
WHERE state = 'active'
  AND now() - query_start > INTERVAL '30 seconds'
ORDER BY query_duration DESC;
```

查看空闲事务：

```sql
SELECT
    pid,
    usename,
    datname,
    application_name,
    client_addr,
    now() - xact_start AS transaction_duration,
    query
FROM pg_stat_activity
WHERE state = 'idle in transaction'
ORDER BY transaction_duration DESC;
```

常见字段说明：

| 字段               | 说明                 |
| ------------------ | -------------------- |
| `pid`              | 后端进程 ID          |
| `usename`          | 数据库用户           |
| `datname`          | 数据库名             |
| `application_name` | 应用名               |
| `client_addr`      | 客户端地址           |
| `state`            | 会话状态             |
| `wait_event_type`  | 等待事件类型         |
| `wait_event`       | 等待事件             |
| `xact_start`       | 当前事务开始时间     |
| `query_start`      | 当前 SQL 开始时间    |
| `query`            | 当前或最后执行的 SQL |

会话状态说明：

| 状态                            | 说明                 |
| ------------------------------- | -------------------- |
| `active`                        | 正在执行 SQL         |
| `idle`                          | 空闲等待新命令       |
| `idle in transaction`           | 事务已打开但当前空闲 |
| `idle in transaction (aborted)` | 事务异常后未回滚     |
| `disabled`                      | 后台跟踪被禁用       |

`pg_stat_activity` 是线上排查的第一入口。慢查询、锁等待、连接泄漏和长事务通常都可以从这里开始定位。

### pg_stat_database

`pg_stat_database` 提供数据库级统计信息，包括连接数、事务提交和回滚、读取块、缓存命中、返回行数、临时文件、死锁等。

查看数据库级指标：

```sql
SELECT
    datname,
    numbackends,
    xact_commit,
    xact_rollback,
    blks_read,
    blks_hit,
    tup_returned,
    tup_fetched,
    tup_inserted,
    tup_updated,
    tup_deleted,
    conflicts,
    temp_files,
    temp_bytes,
    deadlocks
FROM pg_stat_database
WHERE datname IS NOT NULL
ORDER BY datname;
```

查看缓存命中率：

```sql
SELECT
    datname,
    blks_read,
    blks_hit,
    ROUND(
        blks_hit::NUMERIC / NULLIF(blks_hit + blks_read, 0) * 100,
        2
    ) AS cache_hit_ratio_percent
FROM pg_stat_database
WHERE datname IS NOT NULL
ORDER BY cache_hit_ratio_percent ASC NULLS LAST;
```

查看回滚比例：

```sql
SELECT
    datname,
    xact_commit,
    xact_rollback,
    ROUND(
        xact_rollback::NUMERIC
        / NULLIF(xact_commit + xact_rollback, 0)
        * 100,
        2
    ) AS rollback_ratio_percent
FROM pg_stat_database
WHERE datname IS NOT NULL
ORDER BY rollback_ratio_percent DESC NULLS LAST;
```

查看临时文件使用：

```sql
SELECT
    datname,
    temp_files,
    pg_size_pretty(temp_bytes) AS temp_size
FROM pg_stat_database
WHERE datname IS NOT NULL
ORDER BY temp_bytes DESC;
```

指标说明：

| 字段            | 说明         |
| --------------- | ------------ |
| `numbackends`   | 当前连接数   |
| `xact_commit`   | 提交事务数   |
| `xact_rollback` | 回滚事务数   |
| `blks_read`     | 磁盘读取块数 |
| `blks_hit`      | 缓存命中块数 |
| `temp_files`    | 临时文件数量 |
| `temp_bytes`    | 临时文件大小 |
| `deadlocks`     | 死锁次数     |
| `tup_inserted`  | 插入行数     |
| `tup_updated`   | 更新行数     |
| `tup_deleted`   | 删除行数     |

`pg_stat_database` 适合看数据库整体健康度。若发现异常，应继续下钻到 `pg_stat_activity`、`pg_stat_user_tables`、`pg_stat_user_indexes` 和慢查询统计。

### pg_stat_user_tables

`pg_stat_user_tables` 提供用户表级别统计信息，包括顺序扫描、索引扫描、插入、更新、删除、死元组、VACUUM、ANALYZE 等。

查看表访问统计：

```sql
SELECT
    schemaname,
    relname AS table_name,
    seq_scan,
    seq_tup_read,
    idx_scan,
    idx_tup_fetch,
    n_tup_ins,
    n_tup_upd,
    n_tup_del,
    n_live_tup,
    n_dead_tup
FROM pg_stat_user_tables
ORDER BY seq_scan DESC;
```

查看死元组比例：

```sql
SELECT
    schemaname,
    relname AS table_name,
    n_live_tup,
    n_dead_tup,
    ROUND(
        n_dead_tup::NUMERIC / NULLIF(n_live_tup + n_dead_tup, 0) * 100,
        2
    ) AS dead_tuple_ratio_percent,
    last_vacuum,
    last_autovacuum,
    last_analyze,
    last_autoanalyze
FROM pg_stat_user_tables
ORDER BY dead_tuple_ratio_percent DESC NULLS LAST;
```

查看顺序扫描较多的大表：

```sql
SELECT
    schemaname,
    relname AS table_name,
    seq_scan,
    seq_tup_read,
    idx_scan,
    n_live_tup,
    pg_size_pretty(pg_total_relation_size(relid)) AS total_size
FROM pg_stat_user_tables
WHERE seq_scan > 0
ORDER BY seq_tup_read DESC
LIMIT 20;
```

查看高更新表：

```sql
SELECT
    schemaname,
    relname AS table_name,
    n_tup_ins,
    n_tup_upd,
    n_tup_del,
    n_dead_tup,
    last_autovacuum
FROM pg_stat_user_tables
ORDER BY n_tup_upd + n_tup_del DESC
LIMIT 20;
```

字段说明：

| 字段               | 说明                  |
| ------------------ | --------------------- |
| `seq_scan`         | 顺序扫描次数          |
| `seq_tup_read`     | 顺序扫描读取行数      |
| `idx_scan`         | 索引扫描次数          |
| `idx_tup_fetch`    | 索引扫描回表读取行数  |
| `n_tup_ins`        | 插入行数              |
| `n_tup_upd`        | 更新行数              |
| `n_tup_del`        | 删除行数              |
| `n_live_tup`       | 估算活行数            |
| `n_dead_tup`       | 估算死元组数          |
| `last_autovacuum`  | 最近自动 VACUUM 时间  |
| `last_autoanalyze` | 最近自动 ANALYZE 时间 |

表级监控建议如下：

| 现象                    | 可能问题                  |
| ----------------------- | ------------------------- |
| `seq_tup_read` 很高     | 大量全表扫描              |
| `n_dead_tup` 很高       | VACUUM 不及时或长事务阻塞 |
| `n_tup_upd` 很高        | 高频更新表，可能膨胀      |
| `last_autoanalyze` 很久 | 统计信息可能过旧          |
| 大表 `idx_scan` 很低    | 索引缺失或查询不走索引    |

### pg_stat_user_indexes

`pg_stat_user_indexes` 提供用户索引级别统计信息，主要用于判断索引是否被使用，以及索引扫描效果。

查看索引使用情况：

```sql
SELECT
    schemaname,
    relname AS table_name,
    indexrelname AS index_name,
    idx_scan,
    idx_tup_read,
    idx_tup_fetch
FROM pg_stat_user_indexes
ORDER BY idx_scan ASC, relname, indexrelname;
```

查看大索引使用情况：

```sql
SELECT
    s.schemaname,
    s.relname AS table_name,
    s.indexrelname AS index_name,
    s.idx_scan,
    s.idx_tup_read,
    s.idx_tup_fetch,
    pg_size_pretty(pg_relation_size(s.indexrelid)) AS index_size
FROM pg_stat_user_indexes s
ORDER BY pg_relation_size(s.indexrelid) DESC
LIMIT 20;
```

查找可能无用的普通索引：

```sql
SELECT
    s.schemaname,
    s.relname AS table_name,
    s.indexrelname AS index_name,
    s.idx_scan,
    pg_size_pretty(pg_relation_size(s.indexrelid)) AS index_size
FROM pg_stat_user_indexes s
JOIN pg_index i ON i.indexrelid = s.indexrelid
WHERE s.idx_scan = 0
  AND NOT i.indisprimary
  AND NOT i.indisunique
ORDER BY pg_relation_size(s.indexrelid) DESC;
```

查看索引定义：

```sql
SELECT
    schemaname,
    tablename,
    indexname,
    indexdef
FROM pg_indexes
WHERE schemaname = 'app'
ORDER BY tablename, indexname;
```

索引统计字段说明：

| 字段            | 说明                   |
| --------------- | ---------------------- |
| `idx_scan`      | 索引被扫描次数         |
| `idx_tup_read`  | 从索引返回的索引项数量 |
| `idx_tup_fetch` | 通过索引获取的表行数量 |
| `indexrelid`    | 索引对象 ID            |
| `relname`       | 表名                   |
| `indexrelname`  | 索引名                 |

索引监控建议如下：

| 建议                           | 说明                         |
| ------------------------------ | ---------------------------- |
| 未使用索引先观察业务周期       | 月报、年报索引可能低频但重要 |
| 主键和唯一索引不按使用次数删除 | 它们还承担约束职责           |
| 大索引重点治理                 | 体积越大，维护成本越高       |
| 结合慢 SQL 分析                | 不是所有低频索引都应删除     |
| 删除前保存 DDL                 | 便于快速回滚                 |
| 定期复盘索引                   | 结合发布变更和查询模式调整   |

日志与监控最终要形成闭环：发现问题、定位 SQL 或对象、验证执行计划、优化 SQL 或索引、发布变更、观察指标变化。只采集指标但不治理，无法真正提升数据库稳定性。

## VACUUM 与统计信息

PostgreSQL 基于 MVCC 实现并发控制，数据更新和删除不会立即覆盖原有数据，而是产生新的行版本或留下旧版本。`VACUUM` 用于清理这些不再可见的旧版本数据，`ANALYZE` 用于更新统计信息，帮助优化器选择更合理的执行计划。生产环境中，`VACUUM`、`ANALYZE` 和 `autovacuum` 是数据库稳定运行的基础机制。

### MVCC 基础

MVCC 是多版本并发控制。它允许读写并发进行：读事务读取符合自己快照的数据版本，写事务创建新的数据版本，而不是直接覆盖旧版本。这样可以减少读写阻塞，但也会产生死元组。

例如执行更新：

```sql
UPDATE app.order_info
SET
    order_status = 'PAID',
    update_time = CURRENT_TIMESTAMP
WHERE id = 1;
```

从逻辑上看，这条记录被更新了；从存储层看，PostgreSQL 通常会生成一个新的行版本，旧版本在没有事务需要它之后变成死元组。死元组不会立刻消失，需要通过 `VACUUM` 清理。

MVCC 相关概念如下：

| 概念       | 说明                                             |
| ---------- | ------------------------------------------------ |
| 行版本     | 同一行数据在不同事务中的多个版本                 |
| 死元组     | 已经被更新或删除，且不再被任何事务需要的旧行版本 |
| 事务快照   | 事务能看到哪些数据版本的规则                     |
| VACUUM     | 清理死元组，使空间可复用                         |
| ANALYZE    | 收集表和字段统计信息                             |
| autovacuum | PostgreSQL 自动执行 VACUUM 和 ANALYZE 的后台机制 |

查看表的活元组和死元组估算值：

```sql
SELECT
    schemaname,
    relname AS table_name,
    n_live_tup,
    n_dead_tup,
    ROUND(
        n_dead_tup::NUMERIC / NULLIF(n_live_tup + n_dead_tup, 0) * 100,
        2
    ) AS dead_tuple_ratio_percent
FROM pg_stat_user_tables
ORDER BY n_dead_tup DESC;
```

MVCC 使用注意事项如下：

| 问题              | 说明                       |
| ----------------- | -------------------------- |
| 长事务            | 会阻止旧版本被清理         |
| 高频更新          | 容易产生大量死元组         |
| 大批量删除        | 删除后不会立即释放磁盘空间 |
| autovacuum 不及时 | 表和索引可能持续膨胀       |
| 统计信息过旧      | 优化器可能选择错误执行计划 |

### VACUUM 使用

`VACUUM` 用于清理死元组，并把可回收空间标记为可复用。普通 `VACUUM` 通常不会把磁盘空间立即归还给操作系统，而是让这些空间在表后续写入时被复用。

对单表执行清理：

```sql
VACUUM app.order_info;
```

对单表执行清理并更新统计信息：

```sql
VACUUM ANALYZE app.order_info;
```

查看详细清理过程：

```sql
VACUUM VERBOSE app.order_info;
```

对整个数据库执行清理：

```sql
VACUUM;
```

普通 `VACUUM` 的特点如下：

| 特点               | 说明                     |
| ------------------ | ------------------------ |
| 不重写整张表       | 相对安全，锁影响较小     |
| 不直接缩小表文件   | 多数情况下只让空间可复用 |
| 可与普通读写并发   | 通常不会阻塞普通 DML     |
| 可以更新可见性映射 | 有利于 `Index Only Scan` |
| 可配合 `ANALYZE`   | 清理同时更新统计信息     |

查看最近 VACUUM 时间：

```sql
SELECT
    schemaname,
    relname AS table_name,
    last_vacuum,
    last_autovacuum,
    vacuum_count,
    autovacuum_count
FROM pg_stat_user_tables
ORDER BY last_autovacuum DESC NULLS LAST;
```

`VACUUM` 使用建议如下：

| 场景         | 建议                                                        |
| ------------ | ----------------------------------------------------------- |
| 普通业务表   | 依赖 autovacuum，必要时手动 `VACUUM ANALYZE`                |
| 大批量更新后 | 手动执行 `VACUUM ANALYZE`                                   |
| 大批量删除后 | 手动执行 `VACUUM ANALYZE`，如需释放磁盘再评估 `VACUUM FULL` |
| 统计信息异常 | 执行 `ANALYZE` 或 `VACUUM ANALYZE`                          |
| 表死元组过多 | 检查长事务和 autovacuum 参数                                |

### VACUUM FULL 使用

`VACUUM FULL` 会重写整张表，移除死元组并把多余磁盘空间归还给操作系统。它可以显著缩小表文件，但会获取强锁，阻塞对表的读写，因此生产环境必须谨慎使用。

执行 `VACUUM FULL`：

```sql
VACUUM FULL app.order_info;
```

执行后更新统计信息：

```sql
ANALYZE app.order_info;
```

`VACUUM FULL` 特点如下：

| 特点             | 说明                       |
| ---------------- | -------------------------- |
| 会重写表         | 生成新的紧凑表文件         |
| 会释放磁盘空间   | 可以把空间归还给操作系统   |
| 需要强锁         | 执行期间会阻塞表读写       |
| 执行时间可能很长 | 大表风险高                 |
| 需要额外磁盘空间 | 重写过程中可能需要临时空间 |
| 索引也会重建     | 表越大成本越高             |

适合使用 `VACUUM FULL` 的场景：

| 场景                   | 说明                       |
| ---------------------- | -------------------------- |
| 大量删除后急需回收磁盘 | 例如删除历史数据后磁盘告急 |
| 表膨胀非常严重         | 普通 VACUUM 无法缩小文件   |
| 可以安排维护窗口       | 能接受表短时间不可访问     |
| 测试环境清理空间       | 影响范围可控               |

生产环境替代方案：

| 方案               | 说明                                    |
| ------------------ | --------------------------------------- |
| 普通 `VACUUM`      | 让空间复用，不强制缩小文件              |
| 分区表删除历史分区 | 比大表 `DELETE` 和 `VACUUM FULL` 更可控 |
| `pg_repack`        | 可在线重组表，但需要额外扩展和评估      |
| 重建表             | 新建表、导入有效数据、切换表名          |
| 低峰维护窗口       | 对大表执行高风险维护操作                |

执行前建议先评估表大小：

```sql
SELECT
    schemaname,
    relname AS table_name,
    pg_size_pretty(pg_total_relation_size(relid)) AS total_size,
    pg_size_pretty(pg_relation_size(relid)) AS table_size,
    pg_size_pretty(pg_indexes_size(relid)) AS indexes_size
FROM pg_stat_user_tables
WHERE schemaname = 'app'
  AND relname = 'order_info';
```

`VACUUM FULL` 使用建议如下：

| 建议               | 说明                              |
| ------------------ | --------------------------------- |
| 不作为日常清理手段 | 日常依赖 autovacuum 和普通 VACUUM |
| 大表必须低峰执行   | 避免阻塞业务                      |
| 执行前确认磁盘空间 | 重写过程需要额外空间              |
| 执行前备份重要数据 | 防止操作失败或误操作              |
| 执行后更新统计信息 | 使用 `ANALYZE`                    |
| 优先考虑分区设计   | 历史数据清理用分区更优            |

### ANALYZE 使用

`ANALYZE` 用于收集表中数据分布统计信息，包括行数估算、字段值分布、空值比例、最常见值等。优化器依赖这些统计信息选择执行计划。如果统计信息过旧，可能导致错误的扫描方式、连接方式或索引选择。

对单表执行统计信息更新：

```sql
ANALYZE app.order_info;
```

对指定列执行统计信息更新：

```sql
ANALYZE app.order_info (order_status, create_time);
```

对整个数据库执行统计信息更新：

```sql
ANALYZE;
```

查看最近 ANALYZE 时间：

```sql
SELECT
    schemaname,
    relname AS table_name,
    last_analyze,
    last_autoanalyze,
    analyze_count,
    autoanalyze_count
FROM pg_stat_user_tables
ORDER BY last_autoanalyze DESC NULLS LAST;
```

大批量导入后执行：

```sql
COPY app.order_info (
    order_no,
    user_id,
    order_status,
    total_amount,
    create_time
)
FROM '/data/import/order_info.csv'
WITH (FORMAT csv, HEADER true, ENCODING 'UTF8');

ANALYZE app.order_info;
```

查看字段统计信息：

```sql
SELECT
    schemaname,
    tablename,
    attname AS column_name,
    null_frac,
    n_distinct,
    most_common_vals,
    most_common_freqs
FROM pg_stats
WHERE schemaname = 'app'
  AND tablename = 'order_info'
ORDER BY attname;
```

常见统计字段说明：

| 字段                | 说明                       |
| ------------------- | -------------------------- |
| `null_frac`         | 空值比例                   |
| `n_distinct`        | 不同值数量估算             |
| `most_common_vals`  | 最常见值                   |
| `most_common_freqs` | 最常见值频率               |
| `histogram_bounds`  | 直方图边界                 |
| `correlation`       | 字段值和物理存储顺序相关性 |

`ANALYZE` 使用建议如下：

| 场景               | 建议                       |
| ------------------ | -------------------------- |
| 大批量导入后       | 立即执行 `ANALYZE`         |
| 大批量更新后       | 执行 `ANALYZE`             |
| 执行计划突然变差   | 检查统计信息是否过旧       |
| 表数据分布变化明显 | 手动执行 `ANALYZE`         |
| 分区表             | 父表和分区都要关注统计信息 |

### 自动清理机制

`autovacuum` 是 PostgreSQL 自动执行 `VACUUM` 和 `ANALYZE` 的后台机制。它会根据表中插入、更新、删除的数据变化量自动触发清理和统计信息更新。

查看 autovacuum 是否开启：

```sql
SHOW autovacuum;
```

查看 autovacuum 相关配置：

```sql
SELECT
    name,
    setting,
    unit,
    context,
    short_desc
FROM pg_settings
WHERE name LIKE 'autovacuum%'
ORDER BY name;
```

查看当前正在执行的 autovacuum：

```sql
SELECT
    pid,
    datname,
    usename,
    application_name,
    state,
    now() - query_start AS running_time,
    query
FROM pg_stat_activity
WHERE query ILIKE 'autovacuum:%'
ORDER BY running_time DESC;
```

查看表级 autovacuum 执行情况：

```sql
SELECT
    schemaname,
    relname AS table_name,
    n_live_tup,
    n_dead_tup,
    last_autovacuum,
    last_autoanalyze,
    autovacuum_count,
    autoanalyze_count
FROM pg_stat_user_tables
ORDER BY n_dead_tup DESC;
```

自动触发大致可以理解为：

```text
触发 VACUUM 的变化量约等于：
autovacuum_vacuum_threshold + autovacuum_vacuum_scale_factor * 表行数

触发 ANALYZE 的变化量约等于：
autovacuum_analyze_threshold + autovacuum_analyze_scale_factor * 表行数
```

常见默认思路如下：

| 参数                              | 说明                           |
| --------------------------------- | ------------------------------ |
| `autovacuum`                      | 是否启用自动清理               |
| `autovacuum_max_workers`          | autovacuum 最大工作进程数      |
| `autovacuum_naptime`              | autovacuum 检查间隔            |
| `autovacuum_vacuum_threshold`     | VACUUM 基础触发阈值            |
| `autovacuum_vacuum_scale_factor`  | VACUUM 按表大小计算的比例阈值  |
| `autovacuum_analyze_threshold`    | ANALYZE 基础触发阈值           |
| `autovacuum_analyze_scale_factor` | ANALYZE 按表大小计算的比例阈值 |
| `autovacuum_vacuum_cost_limit`    | autovacuum 成本限制            |
| `autovacuum_vacuum_cost_delay`    | autovacuum 成本延迟            |

自动清理建议如下：

| 建议                  | 说明                                 |
| --------------------- | ------------------------------------ |
| 不要关闭 autovacuum   | 关闭后容易表膨胀和事务 ID 风险       |
| 大表单独调低比例阈值  | 默认比例对大表可能触发太晚           |
| 高频更新表单独配置    | 让清理更及时                         |
| 监控 long transaction | 长事务会阻碍清理                     |
| 配合表膨胀监控        | 只开启 autovacuum 不代表一定清理及时 |

### 表膨胀处理

表膨胀是指表中包含大量无效空间，导致表文件、索引文件持续变大，查询扫描更多数据页，缓存效率下降。表膨胀通常由高频更新、删除、长事务、autovacuum 不及时或配置不合理引起。

查看死元组比例：

```sql
SELECT
    schemaname,
    relname AS table_name,
    n_live_tup,
    n_dead_tup,
    ROUND(
        n_dead_tup::NUMERIC / NULLIF(n_live_tup + n_dead_tup, 0) * 100,
        2
    ) AS dead_tuple_ratio_percent,
    last_autovacuum,
    last_autoanalyze
FROM pg_stat_user_tables
ORDER BY dead_tuple_ratio_percent DESC NULLS LAST;
```

查看表和索引大小：

```sql
SELECT
    schemaname,
    relname AS table_name,
    pg_size_pretty(pg_relation_size(relid)) AS table_size,
    pg_size_pretty(pg_indexes_size(relid)) AS indexes_size,
    pg_size_pretty(pg_total_relation_size(relid)) AS total_size
FROM pg_stat_user_tables
ORDER BY pg_total_relation_size(relid) DESC;
```

查看长事务是否阻碍清理：

```sql
SELECT
    pid,
    usename,
    datname,
    application_name,
    state,
    now() - xact_start AS transaction_duration,
    query
FROM pg_stat_activity
WHERE xact_start IS NOT NULL
ORDER BY transaction_duration DESC;
```

普通处理方式：

```sql
VACUUM ANALYZE app.order_info;
```

严重膨胀且需要回收磁盘空间时：

```sql
VACUUM FULL app.order_info;
```

重建索引：

```sql
REINDEX INDEX CONCURRENTLY app.idx_order_info_user_time;
```

重建整张表的索引，生产环境需评估影响：

```sql
REINDEX TABLE CONCURRENTLY app.order_info;
```

表膨胀处理流程：

| 步骤            | 说明                                         |
| --------------- | -------------------------------------------- |
| 判断大小        | 查看表、索引、总大小                         |
| 判断死元组      | 查看 `n_dead_tup` 和比例                     |
| 检查长事务      | 先清理阻塞 VACUUM 的长事务                   |
| 检查 autovacuum | 是否长期未执行                               |
| 普通清理        | 先 `VACUUM ANALYZE`                          |
| 回收磁盘        | 低峰评估 `VACUUM FULL`、`pg_repack` 或重建表 |
| 调整参数        | 高频表设置表级 autovacuum 参数               |

降低膨胀的设计建议：

| 建议                     | 说明                          |
| ------------------------ | ----------------------------- |
| 避免频繁更新大字段       | 大 JSONB、TEXT 字段更新成本高 |
| 热字段和冷字段拆表       | 高频更新字段单独放表          |
| 批量删除改为分区清理     | 删除历史分区优于大批量 DELETE |
| 调低大表 autovacuum 阈值 | 避免触发太晚                  |
| 控制长事务               | 长事务是 VACUUM 常见阻塞源    |
| 合理设置 fillfactor      | 高频更新表可预留页内空间      |

设置表的 `fillfactor`：

```sql
ALTER TABLE app.order_info SET (fillfactor = 80);

VACUUM FULL app.order_info;
```

`fillfactor` 会影响后续数据页预留空间。对高频更新表，较低的 `fillfactor` 可能减少页分裂和索引更新成本，但会增加表体积，需要结合场景测试。

### 统计信息更新

统计信息直接影响执行计划。数据分布变化后，如果统计信息没有及时更新，优化器可能错误估算行数，进而选择错误的索引、连接方式或扫描方式。

手动更新统计信息：

```sql
ANALYZE app.order_info;
```

提高某个字段的统计目标：

```sql
ALTER TABLE app.order_info
ALTER COLUMN order_status SET STATISTICS 1000;

ANALYZE app.order_info;
```

查看字段统计目标：

```sql
SELECT
    n.nspname AS schema_name,
    c.relname AS table_name,
    a.attname AS column_name,
    a.attstattarget AS statistics_target
FROM pg_attribute a
JOIN pg_class c ON c.oid = a.attrelid
JOIN pg_namespace n ON n.oid = c.relnamespace
WHERE n.nspname = 'app'
  AND c.relname = 'order_info'
  AND a.attnum > 0
ORDER BY a.attname;
```

查看统计信息：

```sql
SELECT
    schemaname,
    tablename,
    attname AS column_name,
    null_frac,
    n_distinct,
    most_common_vals,
    most_common_freqs
FROM pg_stats
WHERE schemaname = 'app'
  AND tablename = 'order_info'
ORDER BY attname;
```

扩展统计信息适合多列相关性较强的场景。例如 `tenant_id` 和 `order_status` 经常组合查询，单列统计可能不足。

创建扩展统计信息：

```sql
CREATE STATISTICS stat_order_tenant_status
ON tenant_id, order_status
FROM app.order_info;

ANALYZE app.order_info;
```

查看扩展统计信息：

```sql
SELECT
    schemaname,
    tablename,
    statistics_name,
    attnames,
    kinds
FROM pg_stats_ext
WHERE schemaname = 'app'
ORDER BY tablename, statistics_name;
```

统计信息更新建议如下：

| 场景               | 建议                                    |
| ------------------ | --------------------------------------- |
| 大批量导入后       | 执行 `ANALYZE`                          |
| 大批量更新状态后   | 执行 `ANALYZE`                          |
| SQL 行数估算偏差大 | 检查 `EXPLAIN ANALYZE` 中估算和实际差异 |
| 字段分布极不均匀   | 提高字段统计目标                        |
| 多列强相关         | 创建扩展统计信息                        |
| 分区表             | 关注父表和各分区统计信息                |

### autovacuum 参数

`autovacuum` 参数既可以全局配置，也可以按表单独配置。对于普通表，默认配置通常够用；对于大表、高频更新表、日志表、队列表，往往需要表级参数优化。

查看全局配置：

```sql
SELECT
    name,
    setting,
    unit,
    context
FROM pg_settings
WHERE name LIKE 'autovacuum%'
ORDER BY name;
```

为高频更新表设置更积极的清理策略：

```sql
ALTER TABLE app.order_info SET (
    autovacuum_vacuum_scale_factor = 0.02,
    autovacuum_vacuum_threshold = 1000,
    autovacuum_analyze_scale_factor = 0.01,
    autovacuum_analyze_threshold = 1000
);
```

为日志表设置参数：

```sql
ALTER TABLE app.operation_log SET (
    autovacuum_vacuum_scale_factor = 0.05,
    autovacuum_vacuum_threshold = 5000,
    autovacuum_analyze_scale_factor = 0.05,
    autovacuum_analyze_threshold = 5000
);
```

查看表级 autovacuum 配置：

```sql
SELECT
    n.nspname AS schema_name,
    c.relname AS table_name,
    c.reloptions
FROM pg_class c
JOIN pg_namespace n ON n.oid = c.relnamespace
WHERE n.nspname = 'app'
  AND c.reloptions IS NOT NULL
ORDER BY c.relname;
```

清除表级配置，恢复默认：

```sql
ALTER TABLE app.order_info RESET (
    autovacuum_vacuum_scale_factor,
    autovacuum_vacuum_threshold,
    autovacuum_analyze_scale_factor,
    autovacuum_analyze_threshold
);
```

常用参数说明：

| 参数                              | 说明                        |
| --------------------------------- | --------------------------- |
| `autovacuum_vacuum_scale_factor`  | 按表行数比例触发 VACUUM     |
| `autovacuum_vacuum_threshold`     | 触发 VACUUM 的基础变化行数  |
| `autovacuum_analyze_scale_factor` | 按表行数比例触发 ANALYZE    |
| `autovacuum_analyze_threshold`    | 触发 ANALYZE 的基础变化行数 |
| `autovacuum_vacuum_cost_limit`    | autovacuum 成本上限         |
| `autovacuum_vacuum_cost_delay`    | 成本延迟                    |
| `autovacuum_freeze_max_age`       | 防止事务 ID 回卷的冻结阈值  |
| `autovacuum_max_workers`          | 自动清理最大工作进程数      |

参数调整建议：

| 表类型       | 建议                                   |
| ------------ | -------------------------------------- |
| 小表         | 默认配置通常足够                       |
| 大表         | 调低 scale factor，避免触发太晚        |
| 高频更新表   | 更积极地 VACUUM 和 ANALYZE             |
| 只追加日志表 | 重点关注 ANALYZE 和历史清理            |
| 队列表       | 设置很低的 VACUUM 阈值，避免死元组堆积 |
| 分区表       | 对热点分区单独设置参数                 |

`autovacuum` 参数调整应结合监控数据，不建议盲目调大或调小。清理太慢会膨胀，清理太频繁会增加后台 IO 和 CPU 压力。

## 高可用与复制

高可用与复制用于提升数据库可靠性、读扩展能力和灾难恢复能力。PostgreSQL 常见方案包括流复制、主从架构、同步复制、异步复制、复制槽、WAL 归档、故障切换和读写分离。生产系统中，高可用不是只搭一个从库，还需要监控、自动或半自动切换、备份恢复、复制延迟控制和应用连接治理。

### 流复制

流复制是 PostgreSQL 最常用的复制方式。主库持续把 WAL 日志发送给从库，从库接收并重放 WAL，从而保持数据同步。流复制通常用于主从复制、高可用、只读查询和灾备。

流复制基础流程如下：

| 步骤                  | 说明                                   |
| --------------------- | -------------------------------------- |
| 主库开启 WAL 复制配置 | 设置 `wal_level`、`max_wal_senders` 等 |
| 创建复制用户          | 用于从库连接主库拉取 WAL               |
| 配置访问控制          | 在 `pg_hba.conf` 允许从库连接          |
| 从主库做基础备份      | 使用 `pg_basebackup` 初始化从库        |
| 从库配置主库连接信息  | 设置 `primary_conninfo`                |
| 启动从库              | 从库开始接收并重放 WAL                 |

主库配置示例：

文件位置：`postgresql.conf`

```conf
# 支持物理复制
wal_level = replica

# 允许 WAL 发送进程数量
max_wal_senders = 10

# 复制槽数量
max_replication_slots = 10

# WAL 保留大小，避免从库短暂落后时 WAL 过早删除
wal_keep_size = 1GB

# 开启归档时使用，按需配置
archive_mode = on
archive_command = 'test ! -f /data/archive/%f && cp %p /data/archive/%f'
```

创建复制用户：

```sql
CREATE ROLE repl_user
WITH
    REPLICATION
    LOGIN
    PASSWORD 'change_me_repl_password';
```

`pg_hba.conf` 允许从库连接：

```conf
# 允许从库从 10.0.0.20 连接主库做复制
host    replication    repl_user    10.0.0.20/32    scram-sha-256
```

重载主库配置：

```sql
SELECT pg_reload_conf();
```

从库使用 `pg_basebackup` 初始化：

```bash
pg_basebackup \
  -h 10.0.0.10 \
  -p 5432 \
  -U repl_user \
  -D /var/lib/postgresql/17/main \
  -Fp \
  -Xs \
  -P \
  -R
```

参数说明：

| 参数  | 说明                                    |
| ----- | --------------------------------------- |
| `-h`  | 主库地址                                |
| `-p`  | 主库端口                                |
| `-U`  | 复制用户                                |
| `-D`  | 从库数据目录                            |
| `-Fp` | plain 格式                              |
| `-Xs` | 同步流式传输 WAL                        |
| `-P`  | 显示进度                                |
| `-R`  | 自动写入复制连接配置并创建 standby 标识 |

流复制建议如下：

| 建议                  | 说明                             |
| --------------------- | -------------------------------- |
| 复制用户单独创建      | 不使用超级用户                   |
| 复制网络独立或受控    | 限制从库来源 IP                  |
| 监控复制延迟          | 延迟过高会影响读一致性和故障恢复 |
| 配合复制槽或 WAL 保留 | 防止从库落后后无法追上           |
| 定期演练从库提升      | 验证故障切换流程                 |
| 不把备份等同于复制    | 从库会同步误删除，仍需备份       |

### 主从复制

主从复制是一主一从或一主多从架构。主库负责写入，从库接收主库 WAL 并重放，通常用于只读查询、报表查询、容灾备份和故障切换。

主库查看复制状态：

```sql
SELECT
    pid,
    usename,
    application_name,
    client_addr,
    state,
    sync_state,
    sent_lsn,
    write_lsn,
    flush_lsn,
    replay_lsn,
    write_lag,
    flush_lag,
    replay_lag
FROM pg_stat_replication
ORDER BY application_name;
```

从库查看恢复状态：

```sql
SELECT pg_is_in_recovery() AS is_standby;
```

从库查看接收和重放位置：

```sql
SELECT
    pg_last_wal_receive_lsn() AS receive_lsn,
    pg_last_wal_replay_lsn() AS replay_lsn,
    pg_last_xact_replay_timestamp() AS last_replay_time,
    now() - pg_last_xact_replay_timestamp() AS replay_delay;
```

主从复制架构建议：

| 组件     | 建议                                              |
| -------- | ------------------------------------------------- |
| 主库     | 只处理写入和关键读                                |
| 从库     | 处理报表、查询、备份、分析任务                    |
| 应用连接 | 明确读写数据源                                    |
| 监控     | 监控延迟、连接、WAL、从库状态                     |
| 切换     | 使用 Patroni、repmgr、pg_auto_failover 或运维脚本 |
| 备份     | 仍需要独立备份体系                                |

主从复制注意事项：

| 问题                 | 说明                           |
| -------------------- | ------------------------------ |
| 从库可能延迟         | 读取从库可能读不到刚提交的数据 |
| 从库默认只读         | 不能执行普通写入               |
| 主库误删会复制到从库 | 复制不是误操作保护             |
| 复制中断需要尽快处理 | WAL 丢失后从库可能需要重建     |
| 长查询可能影响重放   | 从库长查询可能与 WAL 重放冲突  |

### 同步复制

同步复制要求事务提交时等待至少一个同步从库确认 WAL 已写入或持久化，从而降低主库故障时的数据丢失风险。同步复制提升数据安全性，但会增加事务提交延迟，并依赖从库和网络稳定性。

主库配置同步复制：

文件位置：`postgresql.conf`

```conf
# 指定同步从库名称，名称来自从库 primary_conninfo 的 application_name
synchronous_standby_names = 'standby1'

# 远程写入确认级别
synchronous_commit = on
```

从库连接配置中设置应用名：

```conf
primary_conninfo = 'host=10.0.0.10 port=5432 user=repl_user password=change_me_repl_password application_name=standby1'
```

查看同步状态：

```sql
SELECT
    application_name,
    client_addr,
    state,
    sync_state,
    write_lag,
    flush_lag,
    replay_lag
FROM pg_stat_replication;
```

常见 `sync_state`：

| 状态        | 说明                     |
| ----------- | ------------------------ |
| `sync`      | 当前同步从库             |
| `potential` | 可能成为同步从库         |
| `async`     | 异步从库                 |
| `quorum`    | 法定人数同步模式中的成员 |

多个同步从库示例：

```conf
# 任意 1 个从库确认即可提交
synchronous_standby_names = 'ANY 1 (standby1, standby2)'
```

同步复制适合场景：

| 场景               | 说明                   |
| ------------------ | ---------------------- |
| 数据丢失容忍度低   | 金融、订单、核心交易   |
| 同城双节点         | 网络延迟较低           |
| 可接受写入延迟增加 | 提交需要等待从库确认   |
| 有完善故障切换机制 | 同步从库故障时要能处理 |

同步复制注意事项：

| 注意点               | 说明                               |
| -------------------- | ---------------------------------- |
| 从库异常可能阻塞提交 | 如果没有可用同步从库，写入可能等待 |
| 网络延迟影响写性能   | 提交路径变长                       |
| 不替代备份           | 误删除仍会同步                     |
| 需要监控同步状态     | `sync_state` 变化要告警            |
| 配置前做压测         | 评估写入延迟影响                   |

### 异步复制

异步复制是 PostgreSQL 流复制的常见默认模式。主库提交事务时不等待从库确认，从库稍后接收并重放 WAL。它对主库写入性能影响较小，但主库故障时可能丢失尚未复制到从库的少量数据。

异步复制配置通常不设置同步从库：

```conf
# 不配置 synchronous_standby_names 时，从库通常为异步
# synchronous_standby_names = ''
```

查看异步从库：

```sql
SELECT
    application_name,
    client_addr,
    state,
    sync_state,
    sent_lsn,
    replay_lsn,
    write_lag,
    flush_lag,
    replay_lag
FROM pg_stat_replication
WHERE sync_state = 'async';
```

计算主从 WAL 差距：

```sql
SELECT
    application_name,
    pg_size_pretty(pg_wal_lsn_diff(sent_lsn, replay_lsn)) AS replay_lag_size,
    write_lag,
    flush_lag,
    replay_lag
FROM pg_stat_replication;
```

异步复制适合场景：

| 场景         | 说明                             |
| ------------ | -------------------------------- |
| 普通业务系统 | 性能优先，允许极少量数据丢失风险 |
| 异地灾备     | 网络延迟较大，不适合同步提交     |
| 读扩展       | 从库用于查询和报表               |
| 备份节点     | 从库承担备份任务                 |
| 成本敏感系统 | 架构简单，写入影响小             |

异步复制注意事项：

| 风险                 | 说明                         |
| -------------------- | ---------------------------- |
| 主库故障可能丢数据   | 已提交但未复制的事务可能丢失 |
| 从库读取可能延迟     | 读写分离要考虑一致性         |
| 延迟积压会消耗 WAL   | 复制槽场景尤其明显           |
| 故障切换要评估数据点 | 选择最新从库提升             |

异步复制建议如下：

| 建议               | 说明                        |
| ------------------ | --------------------------- |
| 监控 replay 延迟   | 时间延迟和 WAL 字节差都要看 |
| 关键写后读走主库   | 避免读从库读不到刚写入数据  |
| 配置合理 WAL 保留  | 防止从库短暂中断后无法追上  |
| 故障切换前确认 LSN | 尽量选择最接近主库的从库    |
| 异地复制接受 RPO   | 明确最多可能丢失多少数据    |

### 复制槽

复制槽用于让主库保留从库或逻辑复制消费者尚未接收的 WAL，防止从库短暂离线后因 WAL 被清理而无法继续追赶。复制槽分为物理复制槽和逻辑复制槽。

创建物理复制槽：

```sql
SELECT pg_create_physical_replication_slot('standby1_slot');
```

查看复制槽：

```sql
SELECT
    slot_name,
    slot_type,
    active,
    restart_lsn,
    confirmed_flush_lsn,
    wal_status,
    safe_wal_size
FROM pg_replication_slots
ORDER BY slot_name;
```

从库连接使用复制槽：

```conf
primary_slot_name = 'standby1_slot'
```

删除复制槽：

```sql
SELECT pg_drop_replication_slot('standby1_slot');
```

复制槽的优点：

| 优点              | 说明                          |
| ----------------- | ----------------------------- |
| 防止 WAL 过早删除 | 从库短暂中断后仍可追赶        |
| 复制状态更可靠    | 主库知道消费者需要的 WAL 位置 |
| 适合关键从库      | 避免频繁重建从库              |
| 适合逻辑复制      | 逻辑订阅依赖复制槽            |

复制槽风险：

| 风险         | 说明                   |
| ------------ | ---------------------- |
| 从库长期离线 | 主库会持续保留 WAL     |
| WAL 占满磁盘 | 复制槽未消费时风险很高 |
| 废弃槽未删除 | 可能长期占用 WAL       |
| 监控缺失     | 很难及时发现 WAL 堆积  |

监控复制槽 WAL 保留大小：

```sql
SELECT
    slot_name,
    slot_type,
    active,
    pg_size_pretty(pg_wal_lsn_diff(pg_current_wal_lsn(), restart_lsn)) AS retained_wal_size,
    restart_lsn
FROM pg_replication_slots
WHERE restart_lsn IS NOT NULL
ORDER BY pg_wal_lsn_diff(pg_current_wal_lsn(), restart_lsn) DESC;
```

复制槽建议如下：

| 建议                  | 说明                    |
| --------------------- | ----------------------- |
| 关键从库使用复制槽    | 防止 WAL 丢失           |
| 必须监控 WAL 堆积     | 防止磁盘被写满          |
| 废弃从库及时删槽      | 不再使用的槽必须清理    |
| 配合 WAL 空间告警     | `pg_wal` 目录增长要告警 |
| 合理配置 WAL 保留上限 | 防止无限增长风险        |

### WAL 日志

WAL 是预写日志。PostgreSQL 在修改数据页之前，先把变更写入 WAL。WAL 用于崩溃恢复、流复制、归档恢复和时间点恢复。理解 WAL 对备份、复制和高可用非常重要。

查看当前 WAL 位置：

```sql
SELECT pg_current_wal_lsn() AS current_wal_lsn;
```

查看当前 WAL 文件名：

```sql
SELECT pg_walfile_name(pg_current_wal_lsn()) AS current_wal_file;
```

查看 WAL 目录大小需要在操作系统层执行：

```bash
du -sh "$PGDATA/pg_wal"
```

WAL 相关配置示例：

```conf
# WAL 级别，流复制至少需要 replica
wal_level = replica

# WAL 最大保留目标
max_wal_size = 4GB

# WAL 最小保留目标
min_wal_size = 1GB

# 检查点超时时间
checkpoint_timeout = 15min

# 检查点完成目标，平滑写入
checkpoint_completion_target = 0.9

# WAL 压缩，降低部分 WAL 体积
wal_compression = on
```

WAL 相关概念：

| 概念             | 说明                     |
| ---------------- | ------------------------ |
| LSN              | WAL 日志位置             |
| WAL segment      | WAL 文件段               |
| checkpoint       | 检查点，保证部分脏页落盘 |
| archive          | WAL 归档，用于恢复       |
| replication slot | 保留消费者未消费的 WAL   |
| replay           | 从库重放 WAL             |

WAL 监控建议：

| 指标              | 说明                       |
| ----------------- | -------------------------- |
| `pg_wal` 目录大小 | 是否异常增长               |
| 复制延迟          | 从库是否及时消费 WAL       |
| 复制槽保留大小    | 是否存在废弃槽或离线消费者 |
| 归档失败          | 归档失败会导致 WAL 堆积    |
| checkpoint 频率   | 过于频繁可能影响性能       |

WAL 使用建议如下：

| 建议                    | 说明                             |
| ----------------------- | -------------------------------- |
| 不手动删除 WAL 文件     | 可能导致数据库不可恢复或从库中断 |
| 复制槽必须监控          | 防止 WAL 堆满磁盘                |
| 归档命令要可靠          | 失败要告警                       |
| 大批量导入关注 WAL 增长 | 可考虑分批、UNLOGGED 中间表      |
| 配合备份策略            | WAL 归档是 PITR 的基础           |

### 归档日志

归档日志是把已完成的 WAL 文件复制到安全位置，用于时间点恢复和灾难恢复。开启归档后，PostgreSQL 会通过 `archive_command` 把 WAL 文件保存到归档目录或远程存储。

启用归档：

文件位置：`postgresql.conf`

```conf
# 开启 WAL 归档
archive_mode = on

# 归档命令，确保目标文件不存在时才复制
archive_command = 'test ! -f /data/pg_archive/%f && cp %p /data/pg_archive/%f'

# 归档超时，低写入量系统也会定期切 WAL
archive_timeout = 300s
```

创建归档目录：

```bash
mkdir -p /data/pg_archive
chown postgres:postgres /data/pg_archive
chmod 700 /data/pg_archive
```

查看归档状态：

```sql
SELECT
    archived_count,
    last_archived_wal,
    last_archived_time,
    failed_count,
    last_failed_wal,
    last_failed_time,
    stats_reset
FROM pg_stat_archiver;
```

手动切换 WAL，触发归档：

```sql
SELECT pg_switch_wal();
```

归档失败排查：

```sql
SELECT
    failed_count,
    last_failed_wal,
    last_failed_time
FROM pg_stat_archiver;
```

归档建议如下：

| 建议                       | 说明                         |
| -------------------------- | ---------------------------- |
| 归档目录不要和数据目录同盘 | 防止单盘故障同时丢失         |
| 归档失败必须告警           | 失败会导致 WAL 堆积          |
| 归档文件要异地保存         | 用于灾难恢复                 |
| 定期清理旧归档             | 结合备份保留策略             |
| 恢复演练必须覆盖归档       | 验证 WAL 能否用于恢复        |
| 归档命令必须幂等           | 目标已存在时不能覆盖错误文件 |

归档日志是时间点恢复的基础。只做 `pg_dump` 逻辑备份无法恢复到任意时间点；要实现 PITR，需要基础备份加连续 WAL 归档。

### 故障切换

故障切换是指主库不可用时，将从库提升为新的主库，并让应用切换到新主库。故障切换可以手动执行，也可以通过高可用组件自动执行。

手动提升从库：

```bash
pg_ctl promote -D /var/lib/postgresql/17/main
```

或者在从库执行 SQL：

```sql
SELECT pg_promote();
```

确认从库已经变为主库：

```sql
SELECT pg_is_in_recovery() AS is_standby;
```

返回 `false` 表示当前节点已经不是从库。

故障切换基本流程：

| 步骤                   | 说明                      |
| ---------------------- | ------------------------- |
| 确认主库故障           | 避免误判导致脑裂          |
| 停止旧主库或隔离旧主库 | 防止两个主库同时写入      |
| 选择最新从库           | 比较复制延迟和 LSN        |
| 提升从库为主库         | `pg_promote()` 或工具执行 |
| 切换应用写连接         | DNS、VIP、代理或配置中心  |
| 重建其他从库           | 让新主库成为复制源        |
| 检查业务一致性         | 验证关键表和应用写入      |

查看从库最后重放时间：

```sql
SELECT
    pg_last_wal_receive_lsn() AS receive_lsn,
    pg_last_wal_replay_lsn() AS replay_lsn,
    pg_last_xact_replay_timestamp() AS last_replay_time,
    now() - pg_last_xact_replay_timestamp() AS replay_delay;
```

常见高可用工具：

| 工具             | 说明                                    |
| ---------------- | --------------------------------------- |
| Patroni          | 基于 DCS 的 PostgreSQL 高可用方案，常用 |
| repmgr           | PostgreSQL 复制管理和故障切换工具       |
| pg_auto_failover | PostgreSQL 自动故障切换工具             |
| HAProxy          | 常用于连接路由                          |
| Keepalived       | 常用于 VIP 漂移                         |
| PgBouncer        | 连接池代理，不是完整 HA 工具            |

故障切换风险：

| 风险           | 说明                          |
| -------------- | ----------------------------- |
| 脑裂           | 旧主库和新主库同时写入        |
| 数据丢失       | 异步复制下未复制 WAL 可能丢失 |
| 应用未切换完全 | 部分服务仍写旧主库            |
| 从库过旧       | 提升落后从库导致数据缺口      |
| 恢复流程不熟   | 故障时人工操作容易出错        |

故障切换建议如下：

| 建议                   | 说明                     |
| ---------------------- | ------------------------ |
| 使用成熟 HA 工具       | 不建议完全依赖手工脚本   |
| 定期演练切换           | 确认 RTO 和操作流程      |
| 防止脑裂               | 必须有节点隔离或仲裁机制 |
| 应用连接通过统一入口   | VIP、代理或服务发现      |
| 明确同步或异步策略     | 决定 RPO 风险            |
| 旧主恢复后不能直接加入 | 需要重新作为从库构建     |

### 读写分离

读写分离是指写请求走主库，读请求按一致性要求走主库或从库。它可以降低主库读压力，但会引入复制延迟和读一致性问题。不是所有读都适合走从库。

读写分离基本策略：

| 请求类型     | 推荐数据源       |
| ------------ | ---------------- |
| 写入请求     | 主库             |
| 写后立即读   | 主库             |
| 强一致读     | 主库             |
| 普通列表查询 | 可走从库         |
| 报表统计     | 可走从库或离线库 |
| 后台导出     | 优先从库         |
| 定时分析任务 | 从库或独立分析库 |

查看从库延迟：

```sql
SELECT
    now() - pg_last_xact_replay_timestamp() AS replay_delay,
    pg_last_wal_receive_lsn() AS receive_lsn,
    pg_last_wal_replay_lsn() AS replay_lsn;
```

应用层读写分离需要处理以下问题：

| 问题         | 说明                         |
| ------------ | ---------------------------- |
| 写后读一致性 | 写入后立即查询从库可能查不到 |
| 事务内读写   | 同一事务必须走同一个主库连接 |
| 从库延迟     | 延迟超阈值时读请求应回主库   |
| 只读错误     | 写请求不能路由到从库         |
| 故障切换     | 主从角色变化后路由要更新     |
| 报表长查询   | 从库长查询可能影响 WAL 重放  |

常见实现方式：

| 方式             | 说明                                  |
| ---------------- | ------------------------------------- |
| 应用层多数据源   | Spring Boot 配置主从数据源            |
| 中间件路由       | 使用代理根据 SQL 或角色路由           |
| HAProxy          | 根据主从健康检查路由                  |
| PgBouncer        | 主要做连接池，不负责复杂 SQL 读写拆分 |
| 云数据库读写分离 | 使用云厂商代理能力                    |

读写分离建议如下：

| 建议                 | 说明                    |
| -------------------- | ----------------------- |
| 默认强一致读走主库   | 尤其是写后读            |
| 从库只承载可延迟读   | 报表、列表、导出        |
| 监控复制延迟         | 延迟过高时降级回主库    |
| 应用显式标记只读查询 | 不建议靠 SQL 字符串猜测 |
| 事务内不要切换数据源 | 避免一致性问题          |
| 从库查询设置超时     | 防止长查询影响复制重放  |

读写分离的价值是降低主库读压力，但代价是复杂度上升。普通系统应先优化 SQL、索引、缓存和分页；只有主库读压力确实成为瓶颈时，再引入读写分离。

## 连接池

连接池用于复用数据库连接，减少频繁创建和关闭连接的成本，并控制应用对 PostgreSQL 的并发连接数量。PostgreSQL 的每个连接都会占用数据库进程、内存和调度资源，因此应用不应无限制创建连接。常见连接池包括应用内连接池，例如 Spring Boot 默认的 HikariCP，以及数据库前置连接池代理，例如 PgBouncer。

### 连接数配置

连接数配置需要同时考虑 PostgreSQL 服务端、应用实例数量、每个应用连接池大小、后台任务、运维连接、报表连接和复制连接。连接池不是越大越好，连接数过大会增加上下文切换、内存占用和锁竞争，反而降低整体吞吐。

查看 PostgreSQL 最大连接数：

```sql
SHOW max_connections;
```

查看保留给超级用户的连接数：

```sql
SHOW superuser_reserved_connections;
```

查看当前连接数：

```sql
SELECT
    COUNT(*) AS connection_count
FROM pg_stat_activity;
```

按数据库、用户和应用统计连接数：

```sql
SELECT
    datname,
    usename,
    application_name,
    state,
    COUNT(*) AS connection_count
FROM pg_stat_activity
GROUP BY
    datname,
    usename,
    application_name,
    state
ORDER BY connection_count DESC;
```

查看连接状态分布：

```sql
SELECT
    state,
    COUNT(*) AS connection_count
FROM pg_stat_activity
GROUP BY state
ORDER BY connection_count DESC;
```

常见连接状态说明：

| 状态                            | 说明                           |
| ------------------------------- | ------------------------------ |
| `active`                        | 正在执行 SQL                   |
| `idle`                          | 空闲连接，等待下一次请求       |
| `idle in transaction`           | 事务已打开但当前空闲，风险较高 |
| `idle in transaction (aborted)` | 事务异常后未回滚               |
| `disabled`                      | 后台状态跟踪被禁用             |

应用总连接数估算：

```text
应用最大连接数 =
应用实例数量 × 每个实例最大连接池大小
```

例如：

```text
4 个应用实例 × 每个实例 maximum-pool-size 20 = 80 个应用连接
```

总连接预算还需要预留以下连接：

| 连接来源         | 说明                 |
| ---------------- | -------------------- |
| 应用连接         | 业务服务连接池       |
| 管理连接         | DBA、运维、监控工具  |
| 报表连接         | BI、导出、分析任务   |
| 复制连接         | 主从复制、备份、订阅 |
| 任务连接         | 定时任务、批处理任务 |
| 超级用户保留连接 | 紧急维护入口         |

服务端连接数配置示例：

文件位置：`postgresql.conf`

```conf
# 数据库允许的最大连接数，需要结合内存和应用实例数评估
max_connections = 200

# 保留给超级用户的连接数，防止连接耗尽后无法维护
superuser_reserved_connections = 3
```

修改后通常需要重启 PostgreSQL：

```bash
# 重新启动 PostgreSQL 服务，使 max_connections 生效
systemctl restart postgresql
```

连接数配置建议如下：

| 建议                             | 说明                                       |
| -------------------------------- | ------------------------------------------ |
| 不要盲目调大 `max_connections`   | 连接越多，内存和调度成本越高               |
| 应用连接池总量小于数据库连接上限 | 需要给运维、监控、复制预留连接             |
| 多实例服务要统一计算连接预算     | 每个实例的连接池都会占用数据库连接         |
| 报表和批处理单独限流             | 防止挤占在线业务连接                       |
| 设置连接超时                     | 避免请求长期等待连接                       |
| 监控连接状态                     | 重点关注 `active` 和 `idle in transaction` |

### 连接池使用场景

连接池适合所有长期运行的应用服务，尤其是 Web 服务、微服务、批处理服务和后台管理系统。它通过复用已有连接，降低连接建立成本，并通过最大连接数限制保护数据库。

连接池解决的问题如下：

| 问题           | 说明                             |
| -------------- | -------------------------------- |
| 频繁创建连接   | TCP、认证、会话初始化都有成本    |
| 连接数量失控   | 通过池大小限制最大连接数         |
| 请求等待不可控 | 通过超时参数控制等待时间         |
| 数据库连接耗尽 | 控制应用对数据库的并发压力       |
| 连接泄漏       | 部分连接池可检测长时间未归还连接 |
| 空闲连接过多   | 通过空闲超时回收连接             |

适合使用连接池的场景：

| 场景                 | 说明                         |
| -------------------- | ---------------------------- |
| Spring Boot Web 应用 | 默认使用 HikariCP            |
| 微服务系统           | 每个服务实例都需要连接池     |
| 定时任务服务         | 控制批任务数据库并发         |
| 报表服务             | 独立连接池，避免影响核心业务 |
| 多数据源应用         | 每个数据源单独配置连接池     |
| 短连接应用很多       | 可使用 PgBouncer 聚合连接    |

不合理场景如下：

| 场景                 | 问题                     |
| -------------------- | ------------------------ |
| 每次请求新建连接     | 成本高，容易耗尽连接     |
| 每个应用池过大       | 多实例部署后总连接数失控 |
| 所有服务共用同一账号 | 难以审计和限流           |
| 长事务占用连接       | 连接不能及时归还池       |
| 报表和在线业务共池   | 慢查询可能拖垮在线接口   |

连接池设计建议：

| 建议                 | 说明                       |
| -------------------- | -------------------------- |
| 在线业务和批处理分开 | 使用不同应用、账号或连接池 |
| 主库和从库分开       | 读写分离时分别配置连接池   |
| 每个池设置合理上限   | 避免实例扩容后压垮数据库   |
| 设置连接最大生命周期 | 避免长期连接积累状态问题   |
| 设置连接泄漏检测     | 排查未关闭连接             |
| 设置 SQL 超时        | 防止慢 SQL 长时间占用连接  |

### PgBouncer

PgBouncer 是 PostgreSQL 常用的轻量级连接池代理。它位于应用和 PostgreSQL 之间，应用连接 PgBouncer，PgBouncer 再复用少量后端连接访问 PostgreSQL。它适合大量短连接、PHP/脚本类应用、多微服务实例、连接数过多等场景。

PgBouncer 常见架构如下：

```text
Application 1 \
Application 2  ---> PgBouncer ---> PostgreSQL
Application 3 /
```

PgBouncer 池模式：

| 模式          | 说明                                     | 适用场景             |
| ------------- | ---------------------------------------- | -------------------- |
| `session`     | 客户端会话期间独占服务端连接             | 兼容性最好           |
| `transaction` | 一个事务期间占用服务端连接，事务结束归还 | 最常用，连接复用率高 |
| `statement`   | 每条语句后归还连接                       | 限制最多，兼容性较差 |

`transaction` 模式性能和连接复用率较好，但需要注意会话级特性，例如临时表、会话变量、预处理语句、`LISTEN/NOTIFY`、游标等可能受影响。

PgBouncer 配置示例：

文件位置：`/etc/pgbouncer/pgbouncer.ini`

```ini
[databases]
; 应用访问 app_db 时，PgBouncer 转发到本机 PostgreSQL
app_db = host=127.0.0.1 port=5432 dbname=app_db

[pgbouncer]
; PgBouncer 监听地址和端口
listen_addr = 0.0.0.0
listen_port = 6432

; 用户密码文件
auth_file = /etc/pgbouncer/userlist.txt

; 使用 SCRAM 或 MD5 认证，需和 userlist.txt 中密码格式匹配
auth_type = scram-sha-256

; 推荐优先使用 transaction 模式，但要评估应用兼容性
pool_mode = transaction

; 每个数据库/用户组合的默认后端连接池大小
default_pool_size = 50

; 额外允许的突发连接数
reserve_pool_size = 10

; 客户端最大连接数
max_client_conn = 1000

; 服务端连接空闲多久后关闭
server_idle_timeout = 600

; 客户端空闲超时
client_idle_timeout = 0

; 管理库名称
admin_users = postgres
stats_users = postgres

; 日志配置
log_connections = 1
log_disconnections = 1
log_pooler_errors = 1
```

用户密码文件示例：

文件位置：`/etc/pgbouncer/userlist.txt`

```text
"app_user" "SCRAM-SHA-256$4096:example_salt$example_stored_key:example_server_key"
"postgres" "SCRAM-SHA-256$4096:example_salt$example_stored_key:example_server_key"
```

实际密码哈希应使用工具生成，不要手写示例值。也可以根据认证方式使用 MD5 格式，但新系统更建议使用 SCRAM。

启动 PgBouncer：

```bash
# 启动 PgBouncer
systemctl start pgbouncer

# 设置开机自启
systemctl enable pgbouncer

# 查看运行状态
systemctl status pgbouncer
```

连接 PgBouncer：

```bash
psql \
  -h 127.0.0.1 \
  -p 6432 \
  -U app_user \
  -d app_db
```

查看 PgBouncer 连接池状态：

```sql
SHOW POOLS;
```

查看客户端连接：

```sql
SHOW CLIENTS;
```

查看服务端连接：

```sql
SHOW SERVERS;
```

重新加载 PgBouncer 配置：

```sql
RELOAD;
```

PgBouncer 使用建议如下：

| 建议                         | 说明                               |
| ---------------------------- | ---------------------------------- |
| 短连接多时优先考虑           | 可以显著减少 PostgreSQL 后端连接   |
| Java 应用仍保留 HikariCP     | PgBouncer 和应用连接池可以配合使用 |
| transaction 模式先做兼容测试 | 注意会话级功能                     |
| 不要让 PgBouncer 池过大      | 后端连接仍会压到 PostgreSQL        |
| 监控 `SHOW POOLS`            | 关注等待客户端和后端连接占用       |
| 管理账号单独控制             | 不开放 PgBouncer 管理库给普通用户  |

PgBouncer 不替代数据库高可用。它主要解决连接复用和连接数控制问题，故障切换仍需要 Patroni、HAProxy、Keepalived、云数据库代理或其他高可用组件配合。

### 应用连接池

应用连接池是在应用进程内部维护数据库连接池。Java 和 Spring Boot 默认常用 HikariCP。应用连接池负责控制每个应用实例最多能同时持有多少数据库连接，以及连接等待、空闲回收、生命周期、健康检查和泄漏检测。

应用连接池常见参数如下：

| 参数                     | 说明                 |
| ------------------------ | -------------------- |
| `maximumPoolSize`        | 最大连接数           |
| `minimumIdle`            | 最小空闲连接数       |
| `connectionTimeout`      | 获取连接最大等待时间 |
| `idleTimeout`            | 空闲连接回收时间     |
| `maxLifetime`            | 连接最大生命周期     |
| `keepaliveTime`          | 连接保活间隔         |
| `validationTimeout`      | 连接校验超时         |
| `leakDetectionThreshold` | 连接泄漏检测阈值     |

应用连接池大小估算需要结合数据库能力、实例数量和业务并发。常见误区是把连接池配置得很大，以为可以提升并发。实际上，如果数据库 CPU、IO 或锁已经是瓶颈，增加连接数只会放大排队和争用。

简单估算方式：

```text
单实例 maximumPoolSize =
可分配给该应用的数据库连接数 / 应用实例数量
```

示例：

```text
数据库可分配给订单服务的连接数：80
订单服务实例数：4

每个实例 maximumPoolSize 建议不超过：
80 / 4 = 20
```

应用连接池配置建议如下：

| 场景         | 建议                                 |
| ------------ | ------------------------------------ |
| 普通后台服务 | `maximumPoolSize` 可从 10 到 30 起步 |
| 高并发短 SQL | 结合压测调整                         |
| 慢 SQL 较多  | 不应盲目增大连接池，应先优化 SQL     |
| 多实例部署   | 总连接数必须统一计算                 |
| 报表服务     | 使用独立连接池和只读账号             |
| 批处理任务   | 限制并发和连接池大小                 |

应用连接池使用规范：

| 规范             | 说明                                   |
| ---------------- | -------------------------------------- |
| 连接必须及时归还 | 使用框架事务和模板，不手动长期持有连接 |
| 事务尽量短       | 事务内不做远程调用和大文件处理         |
| 设置连接等待超时 | 避免请求无限等待                       |
| 设置最大生命周期 | 避免长期连接状态异常                   |
| 开启泄漏检测     | 测试和排查阶段尤其有用                 |
| 区分读写数据源   | 主库、从库连接池分开配置               |

### Spring Boot 连接池配置

Spring Boot 默认使用 HikariCP 作为连接池。配置时需要关注连接池大小、连接超时、空闲时间、最大生命周期、连接检测和泄漏检测。生产环境还应配置 `application_name`，方便在 `pg_stat_activity` 中识别应用来源。

基础配置示例：

文件位置：`src/main/resources/application.yml`

```yaml
spring:
  datasource:
    driver-class-name: org.postgresql.Driver
    url: jdbc:postgresql://127.0.0.1:5432/app_db?ApplicationName=ateng-order-service
    username: app_user
    password: ${POSTGRES_PASSWORD}
    hikari:
      # 连接池名称，便于日志和监控识别
      pool-name: order-service-hikari-pool

      # 最大连接数，需要结合实例数量和数据库 max_connections 计算
      maximum-pool-size: 20

      # 最小空闲连接数，普通服务不宜设置过大
      minimum-idle: 5

      # 获取连接最大等待时间，超过后抛出异常
      connection-timeout: 30000

      # 空闲连接最大保留时间
      idle-timeout: 600000

      # 连接最大生命周期，建议小于数据库或网络层连接回收时间
      max-lifetime: 1800000

      # 连接校验超时时间
      validation-timeout: 5000

      # 连接保活时间，避免中间网络设备回收空闲连接
      keepalive-time: 300000

      # 连接泄漏检测阈值，排查阶段可开启，生产需谨慎设置
      leak-detection-threshold: 60000
```

如果应用通过 PgBouncer 连接，JDBC 地址应指向 PgBouncer 端口：

```yaml
spring:
  datasource:
    driver-class-name: org.postgresql.Driver
    url: jdbc:postgresql://127.0.0.1:6432/app_db?ApplicationName=ateng-order-service
    username: app_user
    password: ${POSTGRES_PASSWORD}
    hikari:
      pool-name: order-service-pgbouncer-pool
      maximum-pool-size: 30
      minimum-idle: 5
      connection-timeout: 30000
      idle-timeout: 600000
      max-lifetime: 1200000
```

Spring Boot 多数据源场景中，主库和从库应分别配置连接池。下面是配置结构示例：

```yaml
app:
  datasource:
    master:
      jdbc-url: jdbc:postgresql://10.0.0.10:5432/app_db?ApplicationName=ateng-service-master
      username: app_user
      password: ${POSTGRES_MASTER_PASSWORD}
      maximum-pool-size: 20
      minimum-idle: 5
    replica:
      jdbc-url: jdbc:postgresql://10.0.0.20:5432/app_db?ApplicationName=ateng-service-replica
      username: app_readonly
      password: ${POSTGRES_REPLICA_PASSWORD}
      maximum-pool-size: 15
      minimum-idle: 3
```

常用配置建议：

| 配置项                     | 建议                                       |
| -------------------------- | ------------------------------------------ |
| `maximum-pool-size`        | 从小开始，结合压测和数据库连接预算调整     |
| `minimum-idle`             | 不宜等于最大连接数，避免空闲连接过多       |
| `connection-timeout`       | 通常 10 到 30 秒                           |
| `idle-timeout`             | 控制空闲连接回收                           |
| `max-lifetime`             | 小于数据库、中间代理或负载均衡连接回收时间 |
| `leak-detection-threshold` | 排查连接泄漏时开启                         |
| `ApplicationName`          | 必须配置，便于数据库侧识别应用             |

数据库侧查看 Spring Boot 应用连接：

```sql
SELECT
    pid,
    usename,
    datname,
    application_name,
    client_addr,
    state,
    now() - backend_start AS connection_age,
    now() - query_start AS query_duration,
    query
FROM pg_stat_activity
WHERE application_name LIKE 'ateng-%'
ORDER BY application_name, query_duration DESC NULLS LAST;
```

Spring Boot 连接池配置建议如下：

| 建议                   | 说明                   |
| ---------------------- | ---------------------- |
| 密码使用环境变量       | 不要写死在配置文件中   |
| 配置 `ApplicationName` | 数据库监控中可识别来源 |
| 多实例统一计算连接数   | 避免扩容后连接耗尽     |
| 慢 SQL 不靠加连接解决  | 应先优化 SQL 和索引    |
| 读写分离时分池管理     | 主从连接池独立限流     |
| 压测后再调整参数       | 不凭经验盲目设置很大   |

### 连接泄漏排查

连接泄漏是指应用从连接池获取连接后，没有及时关闭或归还，导致连接池中的可用连接越来越少，最终请求获取连接超时。连接泄漏常见于手动 JDBC 操作未关闭连接、事务未结束、流式查询未关闭、异常分支未释放资源等场景。

应用侧常见异常表现：

```text
Connection is not available, request timed out after 30000ms.
```

数据库侧表现：

| 表现                       | 说明                |
| -------------------------- | ------------------- |
| 应用连接数持续升高         | 连接不释放          |
| 大量 `idle in transaction` | 事务未提交或未回滚  |
| 活跃 SQL 很少但连接池耗尽  | 连接被应用持有      |
| 请求等待连接超时           | HikariCP 无可用连接 |
| CPU 不高但接口阻塞         | 可能卡在获取连接    |

查看应用连接状态：

```sql
SELECT
    pid,
    usename,
    datname,
    application_name,
    client_addr,
    state,
    now() - backend_start AS connection_age,
    now() - xact_start AS transaction_duration,
    now() - query_start AS query_duration,
    wait_event_type,
    wait_event,
    query
FROM pg_stat_activity
WHERE application_name = 'ateng-order-service'
ORDER BY transaction_duration DESC NULLS LAST;
```

查看空闲未提交事务：

```sql
SELECT
    pid,
    usename,
    datname,
    application_name,
    client_addr,
    now() - xact_start AS transaction_duration,
    query
FROM pg_stat_activity
WHERE state = 'idle in transaction'
ORDER BY transaction_duration DESC;
```

HikariCP 开启泄漏检测：

```yaml
spring:
  datasource:
    hikari:
      # 排查连接泄漏时开启，连接被占用超过 60 秒会输出告警日志
      leak-detection-threshold: 60000
```

典型问题代码是手动获取连接后没有关闭。下面示例只用于说明风险：

```java
Connection connection = dataSource.getConnection();
PreparedStatement statement = connection.prepareStatement("SELECT 1");
ResultSet resultSet = statement.executeQuery();
```

上面代码如果没有在 `finally` 或 `try-with-resources` 中关闭资源，就可能造成连接泄漏。推荐使用框架管理连接，例如 MyBatis、JdbcTemplate、JPA，或使用 `try-with-resources`。

手动 JDBC 应使用 `try-with-resources`：

```java
try (
    Connection connection = dataSource.getConnection();
    PreparedStatement statement = connection.prepareStatement("SELECT 1");
    ResultSet resultSet = statement.executeQuery()
) {
    while (resultSet.next()) {
        int value = resultSet.getInt(1);
    }
}
```

连接泄漏排查步骤：

| 步骤                       | 说明                                        |
| -------------------------- | ------------------------------------------- |
| 查看 HikariCP 日志         | 是否有 leak detection 日志                  |
| 查看 `pg_stat_activity`    | 是否大量连接长期不释放                      |
| 查看 `idle in transaction` | 是否事务未结束                              |
| 定位应用接口               | 根据 `application_name`、SQL、日志 traceId  |
| 检查手动 JDBC              | 是否未关闭 Connection、Statement、ResultSet |
| 检查事务注解               | 是否异常被吞掉，事务未正确结束              |
| 检查流式查询               | ResultSet、Cursor 是否及时关闭              |

连接泄漏修复建议：

| 建议                              | 说明                           |
| --------------------------------- | ------------------------------ |
| 优先使用框架管理连接              | MyBatis、JdbcTemplate、JPA     |
| 手动连接必须 `try-with-resources` | 确保异常也能释放               |
| 事务方法不要长时间执行            | 远程调用、文件处理不要放事务内 |
| 不吞异常                          | 否则事务回滚和资源释放可能异常 |
| 流式查询及时关闭                  | 避免长时间占用连接             |
| 开启泄漏检测                      | 排查阶段非常有用               |
| 配置事务超时                      | 防止异常长事务                 |

### 空闲连接处理

空闲连接处理用于控制连接池和数据库中的空闲连接数量，避免连接长时间占用数据库资源。空闲连接本身不一定是问题，连接池保留一定空闲连接可以提升响应速度；真正需要重点处理的是过多空闲连接、空闲事务和长期无效连接。

查看空闲连接：

```sql
SELECT
    pid,
    usename,
    datname,
    application_name,
    client_addr,
    state,
    now() - state_change AS idle_duration,
    query
FROM pg_stat_activity
WHERE state = 'idle'
ORDER BY idle_duration DESC;
```

查看空闲事务：

```sql
SELECT
    pid,
    usename,
    datname,
    application_name,
    client_addr,
    state,
    now() - xact_start AS transaction_duration,
    now() - state_change AS idle_duration,
    query
FROM pg_stat_activity
WHERE state LIKE 'idle in transaction%'
ORDER BY transaction_duration DESC;
```

设置空闲事务超时：

文件位置：`postgresql.conf`

```conf
# 空闲事务超过 60 秒后自动断开，防止长期持有事务和锁
idle_in_transaction_session_timeout = '60s'
```

设置普通会话空闲超时：

```conf
# 普通空闲会话超过 30 分钟后断开，需结合连接池策略评估
idle_session_timeout = '30min'
```

Spring Boot HikariCP 空闲连接配置：

```yaml
spring:
  datasource:
    hikari:
      # 最小空闲连接数，避免保留过多空闲连接
      minimum-idle: 5

      # 空闲连接超过该时间后可被回收
      idle-timeout: 600000

      # 连接最大生命周期，防止连接长期存在
      max-lifetime: 1800000

      # 保活时间，避免网络设备回收连接导致应用拿到坏连接
      keepalive-time: 300000
```

PgBouncer 空闲连接配置：

```ini
[pgbouncer]
; 服务端连接空闲超过 10 分钟后关闭
server_idle_timeout = 600

; 客户端空闲超时，0 表示不主动断开普通空闲客户端
client_idle_timeout = 0

; 空闲事务超时，避免客户端长时间占用事务
idle_transaction_timeout = 60
```

必要时终止异常空闲事务：

```sql
SELECT pg_terminate_backend(pid)
FROM pg_stat_activity
WHERE state = 'idle in transaction'
  AND now() - xact_start > INTERVAL '10 minutes';
```

空闲连接处理建议如下：

| 场景                  | 建议                                       |
| --------------------- | ------------------------------------------ |
| 少量 `idle`           | 正常连接池行为，不必处理                   |
| 大量 `idle`           | 检查连接池 `minimum-idle` 和实例数量       |
| `idle in transaction` | 高风险，应告警并排查                       |
| 长时间空闲连接        | 可设置 `idle_session_timeout`              |
| 网络设备回收连接      | 设置 HikariCP `keepalive-time`             |
| PgBouncer 后端空闲多  | 调整 `server_idle_timeout` 和池大小        |
| 应用连接池过大        | 降低 `maximum-pool-size` 和 `minimum-idle` |

连接池整体建议如下：

| 建议                           | 说明                               |
| ------------------------------ | ---------------------------------- |
| 先计算连接预算                 | 再配置应用池大小                   |
| 优先优化 SQL                   | 不用扩大连接池掩盖慢 SQL           |
| 每个应用配置 `ApplicationName` | 便于数据库侧定位                   |
| 生产设置连接超时               | 获取连接、SQL 执行、事务都要有边界 |
| PgBouncer 用于连接聚合         | 不替代应用内连接池和数据库高可用   |
| 定期检查连接状态               | 重点关注长事务、锁等待和连接泄漏   |
| 多服务分账号分池               | 便于隔离、审计和限流               |

## 项目开发集成

项目开发集成主要关注 PostgreSQL 与 Java、Spring Boot、MyBatis、MyBatis-Plus、JPA、Flyway、Liquibase、多数据源和读写分离的配合方式。实际项目中应统一连接参数、连接池配置、事务边界、SQL 编写规范、迁移脚本规范和数据库账号权限，避免开发环境、测试环境、生产环境行为不一致。

### JDBC 连接

JDBC 是 Java 访问 PostgreSQL 的基础方式。即使项目使用 MyBatis、MyBatis-Plus、JPA 或 Spring Data，本质上也会通过 PostgreSQL JDBC Driver 与数据库通信。

Maven 依赖如下：

```xml
<dependency>
    <groupId>org.postgresql</groupId>
    <artifactId>postgresql</artifactId>
    <version>42.7.4</version>
</dependency>

<dependency>
    <groupId>cn.hutool</groupId>
    <artifactId>hutool-all</artifactId>
    <version>5.8.35</version>
</dependency>
```

JDBC 连接 URL 示例：

```text
jdbc:postgresql://127.0.0.1:5432/app_db?ApplicationName=ateng-jdbc-demo&currentSchema=app&sslmode=disable
```

常用连接参数如下：

| 参数              | 说明                                             |
| ----------------- | ------------------------------------------------ |
| `ApplicationName` | 应用名称，可在 `pg_stat_activity` 中查看         |
| `currentSchema`   | 默认 Schema                                      |
| `sslmode`         | SSL 模式，如 `disable`、`require`、`verify-full` |
| `connectTimeout`  | 建立连接超时时间，单位秒                         |
| `socketTimeout`   | socket 读写超时时间，单位秒                      |
| `tcpKeepAlive`    | 是否启用 TCP KeepAlive                           |
| `stringtype`      | 字符串参数类型处理，某些 JSONB 场景可能用到      |

下面示例演示使用原生 JDBC 查询 PostgreSQL 数据，并使用 Hutool 做字符串判空校验。

```java
package io.github.ateng.postgresql.jdbc;

import cn.hutool.core.util.StrUtil;

import java.math.BigDecimal;
import java.sql.*;

/**
 * PostgreSQL 原生 JDBC 查询示例。
 *
 * @author Ateng
 * @since 2026-05-09
 */
public class PostgreSqlJdbcDemo {

    private static final String URL = "jdbc:postgresql://127.0.0.1:5432/app_db"
            + "?ApplicationName=ateng-jdbc-demo"
            + "&currentSchema=app"
            + "&sslmode=disable";

    private static final String USERNAME = "app_user";

    private static final String PASSWORD = "change_me_password";

    public static void main(String[] args) throws SQLException {
        String orderStatus = "PAID";

        if (StrUtil.isBlank(orderStatus)) {
            throw new IllegalArgumentException("订单状态不能为空");
        }

        String sql = """
                SELECT
                    id,
                    order_no,
                    total_amount,
                    create_time
                FROM app.order_info
                WHERE order_status = ?
                ORDER BY create_time DESC, id DESC
                LIMIT ?
                """;

        try (
                Connection connection = DriverManager.getConnection(URL, USERNAME, PASSWORD);
                PreparedStatement statement = connection.prepareStatement(sql)
        ) {
            statement.setString(1, orderStatus);
            statement.setInt(2, 20);

            try (ResultSet resultSet = statement.executeQuery()) {
                while (resultSet.next()) {
                    long id = resultSet.getLong("id");
                    String orderNo = resultSet.getString("order_no");
                    BigDecimal totalAmount = resultSet.getBigDecimal("total_amount");
                    Timestamp createTime = resultSet.getTimestamp("create_time");

                    System.out.printf(
                            "id=%d, orderNo=%s, totalAmount=%s, createTime=%s%n",
                            id,
                            orderNo,
                            totalAmount,
                            createTime
                    );
                }
            }
        }
    }
}
```

JDBC 使用建议如下：

| 建议                      | 说明                                        |
| ------------------------- | ------------------------------------------- |
| 使用参数化查询            | 防止 SQL 注入                               |
| 使用连接池                | 不要每次请求都新建数据库连接                |
| 使用 `try-with-resources` | 确保连接、语句、结果集释放                  |
| 设置 `ApplicationName`    | 便于数据库侧定位应用连接                    |
| 明确 Schema               | 使用 `currentSchema` 或 SQL 中写完整 Schema |
| 设置连接超时              | 防止网络异常导致线程长期阻塞                |
| 密码不写死                | 使用环境变量、配置中心或密钥管理系统        |

### Spring Boot 集成

Spring Boot 集成 PostgreSQL 通常使用 `spring-boot-starter-jdbc`、`spring-boot-starter-data-jpa`、MyBatis 或 MyBatis-Plus。连接池默认使用 HikariCP。

Maven 依赖如下：

```xml
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-jdbc</artifactId>
</dependency>

<dependency>
    <groupId>org.postgresql</groupId>
    <artifactId>postgresql</artifactId>
    <scope>runtime</scope>
</dependency>

<dependency>
    <groupId>cn.hutool</groupId>
    <artifactId>hutool-all</artifactId>
    <version>5.8.35</version>
</dependency>
```

基础配置如下：

```yaml
spring:
  datasource:
    driver-class-name: org.postgresql.Driver
    url: jdbc:postgresql://127.0.0.1:5432/app_db?ApplicationName=ateng-springboot&currentSchema=app&sslmode=disable
    username: app_user
    password: ${POSTGRES_PASSWORD}
    hikari:
      pool-name: ateng-postgresql-pool
      maximum-pool-size: 20
      minimum-idle: 5
      connection-timeout: 30000
      idle-timeout: 600000
      max-lifetime: 1800000
      keepalive-time: 300000
      validation-timeout: 5000

logging:
  level:
    org.springframework.jdbc.core: info
```

下面示例使用 `JdbcTemplate` 查询订单列表，并使用 Hutool 校验参数。

```java
package io.github.ateng.postgresql.service;

import cn.hutool.core.util.StrUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.sql.Timestamp;
import java.util.List;

/**
 * 基于 JdbcTemplate 的订单查询服务。
 *
 * @author Ateng
 * @since 2026-05-09
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class OrderJdbcService {

    private final JdbcTemplate jdbcTemplate;

    public List<OrderRow> listRecentOrder(String orderStatus, int limit) {
        if (StrUtil.isBlank(orderStatus)) {
            throw new IllegalArgumentException("订单状态不能为空");
        }

        int safeLimit = Math.min(Math.max(limit, 1), 100);

        String sql = """
                SELECT
                    id,
                    order_no,
                    total_amount,
                    create_time
                FROM app.order_info
                WHERE order_status = ?
                ORDER BY create_time DESC, id DESC
                LIMIT ?
                """;

        log.info("查询最近订单，订单状态：{}，限制数量：{}", orderStatus, safeLimit);

        return jdbcTemplate.query(
                sql,
                (rs, rowNum) -> new OrderRow(
                        rs.getLong("id"),
                        rs.getString("order_no"),
                        rs.getBigDecimal("total_amount"),
                        rs.getTimestamp("create_time")
                ),
                orderStatus,
                safeLimit
        );
    }

    /**
     * 订单查询行数据。
     *
     * @author Ateng
     * @since 2026-05-09
     */
    public record OrderRow(
            Long id,
            String orderNo,
            BigDecimal totalAmount,
            Timestamp createTime
    ) {
    }
}
```

Spring Boot 集成建议如下：

| 建议                   | 说明                             |
| ---------------------- | -------------------------------- |
| 使用 HikariCP          | Spring Boot 默认连接池，性能较好 |
| 配置 `ApplicationName` | 数据库侧可识别应用来源           |
| 配置连接池上限         | 多实例部署时必须计算总连接数     |
| 使用参数化 SQL         | 不拼接用户输入                   |
| 控制事务边界           | `@Transactional` 放在 Service 层 |
| 密码使用环境变量       | 不写死在 `application.yml`       |
| SQL 日志分环境开启     | 生产环境不要输出敏感 SQL 参数    |

### MyBatis 集成

MyBatis 适合 SQL 可控性强的项目，便于编写复杂 SQL、动态查询、报表查询和 PostgreSQL 特有语法。使用 PostgreSQL 时，应注意 JSONB、数组、UUID、分页、批量插入和返回主键等写法。

Maven 依赖如下：

```xml
<dependency>
    <groupId>org.mybatis.spring.boot</groupId>
    <artifactId>mybatis-spring-boot-starter</artifactId>
    <version>3.0.4</version>
</dependency>

<dependency>
    <groupId>org.postgresql</groupId>
    <artifactId>postgresql</artifactId>
    <scope>runtime</scope>
</dependency>

<dependency>
    <groupId>cn.hutool</groupId>
    <artifactId>hutool-all</artifactId>
    <version>5.8.35</version>
</dependency>
```

配置示例：

```yaml
spring:
  datasource:
    driver-class-name: org.postgresql.Driver
    url: jdbc:postgresql://127.0.0.1:5432/app_db?ApplicationName=ateng-mybatis&currentSchema=app&sslmode=disable
    username: app_user
    password: ${POSTGRES_PASSWORD}
    hikari:
      maximum-pool-size: 20
      minimum-idle: 5

mybatis:
  mapper-locations: classpath*:mapper/**/*.xml
  type-aliases-package: io.github.ateng.postgresql.entity
  configuration:
    map-underscore-to-camel-case: true
    default-fetch-size: 200
    default-statement-timeout: 30
```

订单实体类如下：

```java
package io.github.ateng.postgresql.entity;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 订单信息实体。
 *
 * @author Ateng
 * @since 2026-05-09
 */
@Data
public class OrderInfo {

    private Long id;

    private String orderNo;

    private Long userId;

    private String orderStatus;

    private BigDecimal totalAmount;

    private LocalDateTime createTime;

    private LocalDateTime updateTime;
}
```

Mapper 接口如下：

```java
package io.github.ateng.postgresql.mapper;

import io.github.ateng.postgresql.entity.OrderInfo;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 订单信息 Mapper。
 *
 * @author Ateng
 * @since 2026-05-09
 */
@Mapper
public interface OrderInfoMapper {

    List<OrderInfo> selectPageByCondition(
            @Param("userId") Long userId,
            @Param("orderStatus") String orderStatus,
            @Param("startTime") LocalDateTime startTime,
            @Param("endTime") LocalDateTime endTime,
            @Param("limit") Integer limit,
            @Param("offset") Integer offset
    );

    int updateStatus(
            @Param("id") Long id,
            @Param("fromStatus") String fromStatus,
            @Param("toStatus") String toStatus
    );

    int batchInsert(@Param("list") List<OrderInfo> list);
}
```

XML 映射文件如下：

```xml
<?xml version="1.0" encoding="UTF-8" ?>
<!DOCTYPE mapper
        PUBLIC "-//mybatis.org//DTD Mapper 3.0//EN"
        "https://mybatis.org/dtd/mybatis-3-mapper.dtd">

<mapper namespace="io.github.ateng.postgresql.mapper.OrderInfoMapper">

    <resultMap id="OrderInfoResultMap" type="io.github.ateng.postgresql.entity.OrderInfo">
        <id column="id" property="id"/>
        <result column="order_no" property="orderNo"/>
        <result column="user_id" property="userId"/>
        <result column="order_status" property="orderStatus"/>
        <result column="total_amount" property="totalAmount"/>
        <result column="create_time" property="createTime"/>
        <result column="update_time" property="updateTime"/>
    </resultMap>

    <select id="selectPageByCondition" resultMap="OrderInfoResultMap">
        SELECT
            id,
            order_no,
            user_id,
            order_status,
            total_amount,
            create_time,
            update_time
        FROM app.order_info
        WHERE 1 = 1
        <if test="userId != null">
            AND user_id = #{userId}
        </if>
        <if test="orderStatus != null and orderStatus != ''">
            AND order_status = #{orderStatus}
        </if>
        <if test="startTime != null">
            AND create_time &gt;= #{startTime}
        </if>
        <if test="endTime != null">
            AND create_time &lt; #{endTime}
        </if>
        ORDER BY create_time DESC, id DESC
        LIMIT #{limit}
        OFFSET #{offset}
    </select>

    <update id="updateStatus">
        UPDATE app.order_info
        SET
            order_status = #{toStatus},
            update_time = CURRENT_TIMESTAMP
        WHERE id = #{id}
          AND order_status = #{fromStatus}
    </update>

    <insert id="batchInsert">
        INSERT INTO app.order_info (
            order_no,
            user_id,
            order_status,
            total_amount,
            create_time,
            update_time
        )
        VALUES
        <foreach collection="list" item="item" separator=",">
            (
                #{item.orderNo},
                #{item.userId},
                #{item.orderStatus},
                #{item.totalAmount},
                COALESCE(#{item.createTime}, CURRENT_TIMESTAMP),
                COALESCE(#{item.updateTime}, CURRENT_TIMESTAMP)
            )
        </foreach>
    </insert>

</mapper>
```

Service 示例中使用 Hutool 判断集合是否为空，并检查状态更新影响行数。

```java
package io.github.ateng.postgresql.service;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.StrUtil;
import io.github.ateng.postgresql.entity.OrderInfo;
import io.github.ateng.postgresql.mapper.OrderInfoMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * MyBatis 订单业务服务。
 *
 * @author Ateng
 * @since 2026-05-09
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class OrderMyBatisService {

    private final OrderInfoMapper orderInfoMapper;

    @Transactional(rollbackFor = Exception.class)
    public void changeOrderStatus(Long orderId, String fromStatus, String toStatus) {
        if (orderId == null) {
            throw new IllegalArgumentException("订单ID不能为空");
        }
        if (StrUtil.hasBlank(fromStatus, toStatus)) {
            throw new IllegalArgumentException("订单状态不能为空");
        }

        int updated = orderInfoMapper.updateStatus(orderId, fromStatus, toStatus);
        if (updated != 1) {
            log.warn("订单状态修改失败，订单ID：{}，原状态：{}，目标状态：{}", orderId, fromStatus, toStatus);
            throw new IllegalStateException("订单状态已变化或订单不存在");
        }

        log.info("订单状态修改成功，订单ID：{}，原状态：{}，目标状态：{}", orderId, fromStatus, toStatus);
    }

    @Transactional(rollbackFor = Exception.class)
    public void batchCreateOrder(List<OrderInfo> orderList) {
        if (CollUtil.isEmpty(orderList)) {
            log.info("批量创建订单数据为空，跳过处理");
            return;
        }

        orderInfoMapper.batchInsert(orderList);
        log.info("批量创建订单完成，数量：{}", orderList.size());
    }
}
```

MyBatis 使用建议如下：

| 建议                       | 说明                                      |
| -------------------------- | ----------------------------------------- |
| 普通参数使用 `#{}`         | 防止 SQL 注入                             |
| `${}` 只用于白名单字段     | 如排序字段、表名动态拼接                  |
| SQL 中明确 Schema          | 生产环境更可控                            |
| 分页必须有稳定排序         | 推荐 `ORDER BY create_time DESC, id DESC` |
| 状态流转检查影响行数       | 防止并发重复更新                          |
| 批量写入控制数量           | 单条 SQL 不宜拼接过大                     |
| 复杂 PostgreSQL 语法写 XML | JSONB、数组、窗口函数更清晰               |

### MyBatis-Plus 集成

MyBatis-Plus 适合标准 CRUD 较多的业务系统。使用 PostgreSQL 时，主键策略建议和数据库保持一致：如果数据库使用 `IDENTITY` 自增主键，实体类可以使用 `IdType.AUTO`；如果应用使用雪花 ID，则使用 `IdType.ASSIGN_ID`。

Maven 依赖如下：

```xml
<dependency>
    <groupId>com.baomidou</groupId>
    <artifactId>mybatis-plus-spring-boot3-starter</artifactId>
    <version>3.5.9</version>
</dependency>

<dependency>
    <groupId>org.postgresql</groupId>
    <artifactId>postgresql</artifactId>
    <scope>runtime</scope>
</dependency>

<dependency>
    <groupId>cn.hutool</groupId>
    <artifactId>hutool-all</artifactId>
    <version>5.8.35</version>
</dependency>
```

配置示例：

```yaml
spring:
  datasource:
    driver-class-name: org.postgresql.Driver
    url: jdbc:postgresql://127.0.0.1:5432/app_db?ApplicationName=ateng-mybatis-plus&currentSchema=app&sslmode=disable
    username: app_user
    password: ${POSTGRES_PASSWORD}

mybatis-plus:
  mapper-locations: classpath*:mapper/**/*.xml
  type-aliases-package: io.github.ateng.postgresql.entity
  global-config:
    db-config:
      id-type: auto
      logic-delete-field: deleted
      logic-delete-value: true
      logic-not-delete-value: false
  configuration:
    map-underscore-to-camel-case: true
```

实体类示例：

```java
package io.github.ateng.postgresql.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 系统用户实体。
 *
 * @author Ateng
 * @since 2026-05-09
 */
@Data
@TableName("app.sys_user")
public class SysUser {

    @TableId(type = IdType.AUTO)
    private Long id;

    private String username;

    private String nickname;

    private String phone;

    private String email;

    private Boolean enabled;

    @TableLogic
    private Boolean deleted;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;
}
```

自动填充处理器如下：

```java
package io.github.ateng.postgresql.config;

import com.baomidou.mybatisplus.core.handlers.MetaObjectHandler;
import lombok.extern.slf4j.Slf4j;
import org.apache.ibatis.reflection.MetaObject;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;

/**
 * MyBatis-Plus 审计字段自动填充处理器。
 *
 * @author Ateng
 * @since 2026-05-09
 */
@Slf4j
@Component
public class MyBatisPlusMetaObjectHandler implements MetaObjectHandler {

    @Override
    public void insertFill(MetaObject metaObject) {
        LocalDateTime now = LocalDateTime.now();
        this.strictInsertFill(metaObject, "createTime", LocalDateTime.class, now);
        this.strictInsertFill(metaObject, "updateTime", LocalDateTime.class, now);
        log.debug("自动填充新增审计字段，时间：{}", now);
    }

    @Override
    public void updateFill(MetaObject metaObject) {
        LocalDateTime now = LocalDateTime.now();
        this.strictUpdateFill(metaObject, "updateTime", LocalDateTime.class, now);
        log.debug("自动填充更新审计字段，时间：{}", now);
    }
}
```

Mapper 示例：

```java
package io.github.ateng.postgresql.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import io.github.ateng.postgresql.entity.SysUser;
import org.apache.ibatis.annotations.Mapper;

/**
 * 系统用户 Mapper。
 *
 * @author Ateng
 * @since 2026-05-09
 */
@Mapper
public interface SysUserMapper extends BaseMapper<SysUser> {
}
```

Service 示例：

```java
package io.github.ateng.postgresql.service;

import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import io.github.ateng.postgresql.entity.SysUser;
import io.github.ateng.postgresql.mapper.SysUserMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * MyBatis-Plus 用户业务服务。
 *
 * @author Ateng
 * @since 2026-05-09
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class SysUserPlusService {

    private final SysUserMapper sysUserMapper;

    public List<SysUser> searchUser(String keyword) {
        LambdaQueryWrapper<SysUser> wrapper = new LambdaQueryWrapper<SysUser>()
                .eq(SysUser::getDeleted, false)
                .and(StrUtil.isNotBlank(keyword), item -> item
                        .like(SysUser::getUsername, keyword)
                        .or()
                        .like(SysUser::getNickname, keyword)
                )
                .orderByDesc(SysUser::getCreateTime)
                .last("LIMIT 20");

        log.info("查询用户列表，关键字：{}", keyword);
        return sysUserMapper.selectList(wrapper);
    }
}
```

MyBatis-Plus 使用建议如下：

| 建议                                  | 说明                                 |
| ------------------------------------- | ------------------------------------ |
| PostgreSQL 自增主键使用 `IdType.AUTO` | 对应数据库 `IDENTITY` 或 `SERIAL`    |
| 应用生成 ID 使用 `IdType.ASSIGN_ID`   | 对应雪花 ID                          |
| 逻辑删除字段用 `BOOLEAN`              | PostgreSQL 中更自然                  |
| 复杂 SQL 仍写 XML                     | 不要强行用 Wrapper 表达复杂查询      |
| 分页插件配置方言                      | PostgreSQL 使用 `DbType.POSTGRE_SQL` |
| 自动填充和数据库触发器避免重复        | 二者选择一种为主                     |
| Wrapper 的 `.last()` 谨慎使用         | 不接收用户输入，避免注入             |

分页插件配置如下：

```java
package io.github.ateng.postgresql.config;

import com.baomidou.mybatisplus.annotation.DbType;
import com.baomidou.mybatisplus.extension.plugins.MybatisPlusInterceptor;
import com.baomidou.mybatisplus.extension.plugins.inner.PaginationInnerInterceptor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * MyBatis-Plus 插件配置。
 *
 * @author Ateng
 * @since 2026-05-09
 */
@Configuration
public class MyBatisPlusConfig {

    @Bean
    public MybatisPlusInterceptor mybatisPlusInterceptor() {
        MybatisPlusInterceptor interceptor = new MybatisPlusInterceptor();
        interceptor.addInnerInterceptor(new PaginationInnerInterceptor(DbType.POSTGRE_SQL));
        return interceptor;
    }
}
```

### JPA 集成

JPA 适合领域模型较稳定、CRUD 较多、对象关系映射需求明显的项目。PostgreSQL 与 JPA 集成时，应重点关注主键生成策略、Schema、字段类型、事务边界、懒加载、批量写入和复杂查询性能。

Maven 依赖如下：

```xml
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-data-jpa</artifactId>
</dependency>

<dependency>
    <groupId>org.postgresql</groupId>
    <artifactId>postgresql</artifactId>
    <scope>runtime</scope>
</dependency>

<dependency>
    <groupId>cn.hutool</groupId>
    <artifactId>hutool-all</artifactId>
    <version>5.8.35</version>
</dependency>
```

配置示例：

```yaml
spring:
  datasource:
    driver-class-name: org.postgresql.Driver
    url: jdbc:postgresql://127.0.0.1:5432/app_db?ApplicationName=ateng-jpa&currentSchema=app&sslmode=disable
    username: app_user
    password: ${POSTGRES_PASSWORD}
  jpa:
    open-in-view: false
    hibernate:
      ddl-auto: validate
    properties:
      hibernate:
        default_schema: app
        format_sql: true
        jdbc:
          batch_size: 100
        order_inserts: true
        order_updates: true
    show-sql: false
```

JPA 实体示例：

```java
package io.github.ateng.postgresql.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * JPA 订单实体。
 *
 * @author Ateng
 * @since 2026-05-09
 */
@Getter
@Setter
@Entity
@Table(name = "order_info", schema = "app")
public class JpaOrderInfo {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "order_no", nullable = false, length = 64, unique = true)
    private String orderNo;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Column(name = "order_status", nullable = false, length = 32)
    private String orderStatus;

    @Column(name = "total_amount", nullable = false, precision = 12, scale = 2)
    private BigDecimal totalAmount;

    @Column(name = "create_time", nullable = false)
    private LocalDateTime createTime;

    @Column(name = "update_time", nullable = false)
    private LocalDateTime updateTime;

    @PrePersist
    public void prePersist() {
        LocalDateTime now = LocalDateTime.now();
        this.createTime = this.createTime == null ? now : this.createTime;
        this.updateTime = this.updateTime == null ? now : this.updateTime;
    }

    @PreUpdate
    public void preUpdate() {
        this.updateTime = LocalDateTime.now();
    }
}
```

Repository 示例：

```java
package io.github.ateng.postgresql.repository;

import io.github.ateng.postgresql.entity.JpaOrderInfo;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDateTime;

/**
 * JPA 订单 Repository。
 *
 * @author Ateng
 * @since 2026-05-09
 */
public interface JpaOrderInfoRepository extends JpaRepository<JpaOrderInfo, Long> {

    Page<JpaOrderInfo> findByUserIdAndOrderStatusAndCreateTimeGreaterThanEqualAndCreateTimeLessThan(
            Long userId,
            String orderStatus,
            LocalDateTime startTime,
            LocalDateTime endTime,
            Pageable pageable
    );
}
```

Service 示例：

```java
package io.github.ateng.postgresql.service;

import cn.hutool.core.util.StrUtil;
import io.github.ateng.postgresql.entity.JpaOrderInfo;
import io.github.ateng.postgresql.repository.JpaOrderInfoRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.*;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

/**
 * JPA 订单查询服务。
 *
 * @author Ateng
 * @since 2026-05-09
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class JpaOrderService {

    private final JpaOrderInfoRepository orderInfoRepository;

    public Page<JpaOrderInfo> pageOrder(
            Long userId,
            String orderStatus,
            LocalDateTime startTime,
            LocalDateTime endTime,
            int page,
            int size
    ) {
        if (userId == null) {
            throw new IllegalArgumentException("用户ID不能为空");
        }
        if (StrUtil.isBlank(orderStatus)) {
            throw new IllegalArgumentException("订单状态不能为空");
        }

        Pageable pageable = PageRequest.of(
                Math.max(page, 0),
                Math.min(Math.max(size, 1), 100),
                Sort.by(Sort.Order.desc("createTime"), Sort.Order.desc("id"))
        );

        log.info("分页查询订单，用户ID：{}，订单状态：{}", userId, orderStatus);

        return orderInfoRepository.findByUserIdAndOrderStatusAndCreateTimeGreaterThanEqualAndCreateTimeLessThan(
                userId,
                orderStatus,
                startTime,
                endTime,
                pageable
        );
    }
}
```

JPA 使用建议如下：

| 建议                             | 说明                                            |
| -------------------------------- | ----------------------------------------------- |
| 生产禁用自动建表                 | `ddl-auto` 使用 `validate` 或 `none`            |
| 复杂 SQL 使用原生 SQL 或 MyBatis | 不要强行用 JPQL 表达复杂报表                    |
| 关闭 `open-in-view`              | 避免 Web 层隐式懒加载和长连接占用               |
| 批量写入配置 batch               | 同时关注 ID 策略和事务大小                      |
| 避免 N+1 查询                    | 使用 fetch join、EntityGraph 或 DTO 查询        |
| 明确 Schema                      | 使用 `hibernate.default_schema` 或实体 `schema` |
| 审计字段统一处理                 | JPA 回调、应用填充、数据库触发器三选一为主      |

### Flyway 集成

Flyway 用于数据库版本迁移，适合管理表结构、索引、视图、函数、触发器、初始化数据等变更。项目中应把数据库 DDL 纳入版本管理，避免手工改库造成环境不一致。

Maven 依赖如下：

```xml
<dependency>
    <groupId>org.flywaydb</groupId>
    <artifactId>flyway-core</artifactId>
</dependency>

<dependency>
    <groupId>org.flywaydb</groupId>
    <artifactId>flyway-database-postgresql</artifactId>
</dependency>
```

Spring Boot 配置如下：

```yaml
spring:
  flyway:
    enabled: true
    locations: classpath:db/migration
    default-schema: app
    schemas: app
    baseline-on-migrate: true
    validate-on-migrate: true
    out-of-order: false
```

迁移脚本目录：

```text
src/main/resources/db/migration
├── V1__create_schema.sql
├── V2__create_order_table.sql
├── V3__create_order_indexes.sql
└── V4__init_sys_config.sql
```

创建 Schema 脚本：

```sql
CREATE SCHEMA IF NOT EXISTS app;
```

建表示例：

```sql
CREATE TABLE IF NOT EXISTS app.order_info (
    id BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    order_no VARCHAR(64) NOT NULL,
    user_id BIGINT NOT NULL,
    order_status VARCHAR(32) NOT NULL,
    total_amount NUMERIC(12, 2) NOT NULL DEFAULT 0,
    create_time TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_time TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uk_order_info_order_no UNIQUE (order_no)
);
```

创建索引脚本：

```sql
CREATE INDEX IF NOT EXISTS idx_order_info_user_time
ON app.order_info (user_id, create_time DESC, id DESC);

CREATE INDEX IF NOT EXISTS idx_order_info_status_time
ON app.order_info (order_status, create_time DESC, id DESC);
```

初始化配置数据：

```sql
INSERT INTO app.sys_config (
    config_key,
    config_value,
    remark
) VALUES
    ('system.name', 'PostgreSQL 管理系统', '系统名称'),
    ('system.locale', 'zh-CN', '默认语言')
ON CONFLICT (config_key) DO UPDATE
SET
    config_value = EXCLUDED.config_value,
    remark = EXCLUDED.remark,
    update_time = CURRENT_TIMESTAMP;
```

Flyway 脚本命名规则：

| 类型       | 示例                    | 说明                           |
| ---------- | ----------------------- | ------------------------------ |
| 版本脚本   | `V1__create_schema.sql` | 只执行一次                     |
| 可重复脚本 | `R__create_views.sql`   | 内容变化后重新执行             |
| 分隔符     | `__`                    | 版本号和描述之间使用两个下划线 |
| 描述       | `create_order_table`    | 使用英文和下划线               |

Flyway 使用建议如下：

| 建议                 | 说明               |
| -------------------- | ------------------ |
| 所有 DDL 纳入 Flyway | 不手工改库         |
| 版本脚本只追加不修改 | 已发布脚本不要改   |
| 初始化数据要幂等     | 使用 `ON CONFLICT` |
| 索引脚本明确命名     | 便于回滚和排查     |
| 生产执行前备份       | 高风险变更必须备份 |
| 大表变更单独评估     | 避免长锁和长事务   |
| 多服务共享库谨慎迁移 | 防止互相影响       |

### Liquibase 集成

Liquibase 也是数据库迁移工具，支持 XML、YAML、JSON、SQL 等格式。相比 Flyway，Liquibase 的变更集模型更强，适合需要精细化变更描述、回滚定义和跨数据库兼容的团队。

Maven 依赖如下：

```xml
<dependency>
    <groupId>org.liquibase</groupId>
    <artifactId>liquibase-core</artifactId>
</dependency>
```

Spring Boot 配置如下：

```yaml
spring:
  liquibase:
    enabled: true
    change-log: classpath:db/changelog/db.changelog-master.yaml
    default-schema: app
    liquibase-schema: app
```

主变更文件：

```yaml
databaseChangeLog:
  - include:
      file: db/changelog/changes/001-create-schema.yaml
  - include:
      file: db/changelog/changes/002-create-order-table.yaml
  - include:
      file: db/changelog/changes/003-create-order-indexes.yaml
```

创建 Schema：

```yaml
databaseChangeLog:
  - changeSet:
      id: 001-create-schema
      author: ateng
      changes:
        - sql:
            sql: CREATE SCHEMA IF NOT EXISTS app;
```

创建订单表：

```yaml
databaseChangeLog:
  - changeSet:
      id: 002-create-order-table
      author: ateng
      changes:
        - createTable:
            schemaName: app
            tableName: order_info
            columns:
              - column:
                  name: id
                  type: BIGINT
                  autoIncrement: true
                  constraints:
                    primaryKey: true
                    primaryKeyName: pk_order_info
                    nullable: false
              - column:
                  name: order_no
                  type: VARCHAR(64)
                  constraints:
                    nullable: false
                    unique: true
                    uniqueConstraintName: uk_order_info_order_no
              - column:
                  name: user_id
                  type: BIGINT
                  constraints:
                    nullable: false
              - column:
                  name: order_status
                  type: VARCHAR(32)
                  constraints:
                    nullable: false
              - column:
                  name: total_amount
                  type: NUMERIC(12,2)
                  defaultValueNumeric: 0
                  constraints:
                    nullable: false
              - column:
                  name: create_time
                  type: TIMESTAMP
                  defaultValueComputed: CURRENT_TIMESTAMP
                  constraints:
                    nullable: false
              - column:
                  name: update_time
                  type: TIMESTAMP
                  defaultValueComputed: CURRENT_TIMESTAMP
                  constraints:
                    nullable: false
```

创建索引：

```yaml
databaseChangeLog:
  - changeSet:
      id: 003-create-order-indexes
      author: ateng
      changes:
        - createIndex:
            schemaName: app
            tableName: order_info
            indexName: idx_order_info_user_time
            columns:
              - column:
                  name: user_id
              - column:
                  name: create_time
                  descending: true
              - column:
                  name: id
                  descending: true
        - createIndex:
            schemaName: app
            tableName: order_info
            indexName: idx_order_info_status_time
            columns:
              - column:
                  name: order_status
              - column:
                  name: create_time
                  descending: true
              - column:
                  name: id
                  descending: true
```

Liquibase 使用建议如下：

| 建议                             | 说明                                       |
| -------------------------------- | ------------------------------------------ |
| changeSet ID 全局唯一            | 避免冲突                                   |
| author 固定规范                  | 便于审计                                   |
| 复杂 PostgreSQL 特性可直接写 SQL | 如 JSONB、分区表、函数、触发器             |
| 生产变更前执行预览               | 使用 updateSQL 查看即将执行 SQL            |
| 回滚策略明确                     | 关键变更提供 rollback                      |
| 和 Flyway 二选一                 | 同一项目不要混用两个迁移工具管理同一批对象 |

Flyway 与 Liquibase 选择建议：

| 场景                | 推荐                    |
| ------------------- | ----------------------- |
| 简单直接 SQL 迁移   | Flyway                  |
| 团队习惯手写 SQL    | Flyway                  |
| 需要结构化变更描述  | Liquibase               |
| 需要复杂回滚管理    | Liquibase               |
| 多数据库兼容        | Liquibase 更有优势      |
| PostgreSQL 专项项目 | 两者都可，Flyway 更轻量 |

### 多数据源配置

多数据源用于一个应用连接多个数据库，常见场景包括业务库与报表库分离、主库与从库分离、多租户独立库、历史库查询、跨系统数据整合等。多数据源配置应明确事务边界和 Mapper 归属，避免误用数据源。

配置示例：

```yaml
app:
  datasource:
    order:
      driver-class-name: org.postgresql.Driver
      jdbc-url: jdbc:postgresql://127.0.0.1:5432/order_db?ApplicationName=ateng-order-db&currentSchema=app
      username: order_user
      password: ${ORDER_DB_PASSWORD}
      maximum-pool-size: 20
      minimum-idle: 5
    report:
      driver-class-name: org.postgresql.Driver
      jdbc-url: jdbc:postgresql://127.0.0.1:5432/report_db?ApplicationName=ateng-report-db&currentSchema=app
      username: report_user
      password: ${REPORT_DB_PASSWORD}
      maximum-pool-size: 10
      minimum-idle: 2
```

数据源属性类如下：

```java
package io.github.ateng.postgresql.config;

import lombok.Data;

/**
 * 数据源连接池配置属性。
 *
 * @author Ateng
 * @since 2026-05-09
 */
@Data
public class DataSourcePoolProperties {

    private String driverClassName;

    private String jdbcUrl;

    private String username;

    private String password;

    private Integer maximumPoolSize = 10;

    private Integer minimumIdle = 2;
}
```

多数据源配置类如下：

```java
package io.github.ateng.postgresql.config;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.*;

import javax.sql.DataSource;

/**
 * 多数据源配置。
 *
 * @author Ateng
 * @since 2026-05-09
 */
@Configuration
public class MultiDataSourceConfig {

    @Bean
    @ConfigurationProperties(prefix = "app.datasource.order")
    public DataSourcePoolProperties orderDataSourceProperties() {
        return new DataSourcePoolProperties();
    }

    @Bean
    @ConfigurationProperties(prefix = "app.datasource.report")
    public DataSourcePoolProperties reportDataSourceProperties() {
        return new DataSourcePoolProperties();
    }

    @Primary
    @Bean(name = "orderDataSource")
    public DataSource orderDataSource(DataSourcePoolProperties orderDataSourceProperties) {
        return createHikariDataSource("order-hikari-pool", orderDataSourceProperties);
    }

    @Bean(name = "reportDataSource")
    public DataSource reportDataSource(DataSourcePoolProperties reportDataSourceProperties) {
        return createHikariDataSource("report-hikari-pool", reportDataSourceProperties);
    }

    private DataSource createHikariDataSource(String poolName, DataSourcePoolProperties properties) {
        HikariConfig config = new HikariConfig();
        config.setPoolName(poolName);
        config.setDriverClassName(properties.getDriverClassName());
        config.setJdbcUrl(properties.getJdbcUrl());
        config.setUsername(properties.getUsername());
        config.setPassword(properties.getPassword());
        config.setMaximumPoolSize(properties.getMaximumPoolSize());
        config.setMinimumIdle(properties.getMinimumIdle());
        config.setConnectionTimeout(30000);
        config.setIdleTimeout(600000);
        config.setMaxLifetime(1800000);
        config.setKeepaliveTime(300000);
        return new HikariDataSource(config);
    }
}
```

如果使用 MyBatis，多数据源通常需要分别配置 `SqlSessionFactory`、`SqlSessionTemplate` 和 Mapper 扫描路径。

订单库 MyBatis 配置如下：

```java
package io.github.ateng.postgresql.config;

import org.apache.ibatis.session.SqlSessionFactory;
import org.mybatis.spring.*;
import org.mybatis.spring.annotation.MapperScan;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.*;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;
import org.springframework.jdbc.datasource.DataSourceTransactionManager;

import javax.sql.DataSource;

/**
 * 订单库 MyBatis 配置。
 *
 * @author Ateng
 * @since 2026-05-09
 */
@Configuration
@MapperScan(
        basePackages = "io.github.ateng.postgresql.order.mapper",
        sqlSessionTemplateRef = "orderSqlSessionTemplate"
)
public class OrderMyBatisConfig {

    @Primary
    @Bean
    public SqlSessionFactory orderSqlSessionFactory(
            @Qualifier("orderDataSource") DataSource dataSource
    ) throws Exception {
        SqlSessionFactoryBean factoryBean = new SqlSessionFactoryBean();
        factoryBean.setDataSource(dataSource);
        factoryBean.setMapperLocations(
                new PathMatchingResourcePatternResolver()
                        .getResources("classpath*:mapper/order/**/*.xml")
        );
        return factoryBean.getObject();
    }

    @Primary
    @Bean
    public SqlSessionTemplate orderSqlSessionTemplate(
            @Qualifier("orderSqlSessionFactory") SqlSessionFactory sqlSessionFactory
    ) {
        return new SqlSessionTemplate(sqlSessionFactory);
    }

    @Primary
    @Bean
    public DataSourceTransactionManager orderTransactionManager(
            @Qualifier("orderDataSource") DataSource dataSource
    ) {
        return new DataSourceTransactionManager(dataSource);
    }
}
```

报表库 MyBatis 配置如下：

```java
package io.github.ateng.postgresql.config;

import org.apache.ibatis.session.SqlSessionFactory;
import org.mybatis.spring.*;
import org.mybatis.spring.annotation.MapperScan;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.*;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;
import org.springframework.jdbc.datasource.DataSourceTransactionManager;

import javax.sql.DataSource;

/**
 * 报表库 MyBatis 配置。
 *
 * @author Ateng
 * @since 2026-05-09
 */
@Configuration
@MapperScan(
        basePackages = "io.github.ateng.postgresql.report.mapper",
        sqlSessionTemplateRef = "reportSqlSessionTemplate"
)
public class ReportMyBatisConfig {

    @Bean
    public SqlSessionFactory reportSqlSessionFactory(
            @Qualifier("reportDataSource") DataSource dataSource
    ) throws Exception {
        SqlSessionFactoryBean factoryBean = new SqlSessionFactoryBean();
        factoryBean.setDataSource(dataSource);
        factoryBean.setMapperLocations(
                new PathMatchingResourcePatternResolver()
                        .getResources("classpath*:mapper/report/**/*.xml")
        );
        return factoryBean.getObject();
    }

    @Bean
    public SqlSessionTemplate reportSqlSessionTemplate(
            @Qualifier("reportSqlSessionFactory") SqlSessionFactory sqlSessionFactory
    ) {
        return new SqlSessionTemplate(sqlSessionFactory);
    }

    @Bean
    public DataSourceTransactionManager reportTransactionManager(
            @Qualifier("reportDataSource") DataSource dataSource
    ) {
        return new DataSourceTransactionManager(dataSource);
    }
}
```

多数据源事务使用示例：

```java
package io.github.ateng.postgresql.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 多数据源业务服务。
 *
 * @author Ateng
 * @since 2026-05-09
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class MultiDataSourceService {

    @Transactional(transactionManager = "orderTransactionManager", rollbackFor = Exception.class)
    public void handleOrderBusiness() {
        log.info("使用订单库事务处理业务");
        // 调用订单库 Mapper
    }

    @Transactional(transactionManager = "reportTransactionManager", readOnly = true)
    public void queryReportBusiness() {
        log.info("使用报表库只读事务查询数据");
        // 调用报表库 Mapper
    }
}
```

多数据源使用建议如下：

| 建议                     | 说明                                     |
| ------------------------ | ---------------------------------------- |
| Mapper 按数据源分包      | 避免误扫到错误数据源                     |
| 每个数据源单独事务管理器 | `@Transactional` 指定 transactionManager |
| 主数据源使用 `@Primary`  | 避免自动注入冲突                         |
| 不轻易跨库事务           | 分布式事务成本高                         |
| 读库账号只授予只读权限   | 数据库层兜底                             |
| 连接池分别限流           | 报表库不要挤占业务库连接                 |
| SQL 迁移脚本分库管理     | Flyway 或 Liquibase 按库配置             |

### 读写分离配置

读写分离用于把写请求路由到主库，把可接受延迟的读请求路由到从库。Spring Boot 中常见实现方式是使用动态数据源，通过注解、AOP 或事务只读属性切换数据源。

读写分离配置示例：

```yaml
app:
  datasource:
    master:
      driver-class-name: org.postgresql.Driver
      jdbc-url: jdbc:postgresql://10.0.0.10:5432/app_db?ApplicationName=ateng-master&currentSchema=app
      username: app_user
      password: ${POSTGRES_MASTER_PASSWORD}
      maximum-pool-size: 20
      minimum-idle: 5
    replica:
      driver-class-name: org.postgresql.Driver
      jdbc-url: jdbc:postgresql://10.0.0.20:5432/app_db?ApplicationName=ateng-replica&currentSchema=app
      username: app_readonly
      password: ${POSTGRES_REPLICA_PASSWORD}
      maximum-pool-size: 15
      minimum-idle: 3
```

数据源上下文如下：

```java
package io.github.ateng.postgresql.datasource;

/**
 * 数据源上下文。
 *
 * @author Ateng
 * @since 2026-05-09
 */
public final class DataSourceContextHolder {

    private static final ThreadLocal<String> CONTEXT = new ThreadLocal<>();

    private DataSourceContextHolder() {
    }

    public static void useMaster() {
        CONTEXT.set("master");
    }

    public static void useReplica() {
        CONTEXT.set("replica");
    }

    public static String getDataSourceKey() {
        return CONTEXT.get();
    }

    public static void clear() {
        CONTEXT.remove();
    }
}
```

动态数据源如下：

```java
package io.github.ateng.postgresql.datasource;

import org.springframework.jdbc.datasource.lookup.AbstractRoutingDataSource;

/**
 * 主从动态路由数据源。
 *
 * @author Ateng
 * @since 2026-05-09
 */
public class ReadWriteRoutingDataSource extends AbstractRoutingDataSource {

    @Override
    protected Object determineCurrentLookupKey() {
        return DataSourceContextHolder.getDataSourceKey();
    }
}
```

数据源配置如下：

```java
package io.github.ateng.postgresql.config;

import io.github.ateng.postgresql.datasource.ReadWriteRoutingDataSource;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.*;

import javax.sql.DataSource;
import java.util.HashMap;
import java.util.Map;

/**
 * 读写分离数据源配置。
 *
 * @author Ateng
 * @since 2026-05-09
 */
@Configuration
public class ReadWriteDataSourceConfig {

    @Primary
    @Bean(name = "routingDataSource")
    public DataSource routingDataSource(
            @Qualifier("masterDataSource") DataSource masterDataSource,
            @Qualifier("replicaDataSource") DataSource replicaDataSource
    ) {
        Map<Object, Object> targetDataSources = new HashMap<>();
        targetDataSources.put("master", masterDataSource);
        targetDataSources.put("replica", replicaDataSource);

        ReadWriteRoutingDataSource routingDataSource = new ReadWriteRoutingDataSource();
        routingDataSource.setDefaultTargetDataSource(masterDataSource);
        routingDataSource.setTargetDataSources(targetDataSources);
        return routingDataSource;
    }
}
```

定义只读注解：

```java
package io.github.ateng.postgresql.datasource;

import java.lang.annotation.*;

/**
 * 使用从库查询注解。
 *
 * @author Ateng
 * @since 2026-05-09
 */
@Target({ElementType.METHOD, ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface ReadOnlyDataSource {
}
```

AOP 路由逻辑如下：

```java
package io.github.ateng.postgresql.datasource;

import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.*;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

/**
 * 读写分离数据源切换切面。
 *
 * @author Ateng
 * @since 2026-05-09
 */
@Slf4j
@Aspect
@Component
@Order(0)
public class ReadWriteDataSourceAspect {

    @Around("@annotation(io.github.ateng.postgresql.datasource.ReadOnlyDataSource)")
    public Object useReplica(ProceedingJoinPoint joinPoint) throws Throwable {
        try {
            DataSourceContextHolder.useReplica();
            log.debug("切换到从库数据源");
            return joinPoint.proceed();
        } finally {
            DataSourceContextHolder.clear();
            log.debug("清理数据源上下文");
        }
    }
}
```

Service 使用示例：

```java
package io.github.ateng.postgresql.service;

import io.github.ateng.postgresql.datasource.ReadOnlyDataSource;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 读写分离订单服务。
 *
 * @author Ateng
 * @since 2026-05-09
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ReadWriteOrderService {

    @Transactional(rollbackFor = Exception.class)
    public void createOrder() {
        log.info("创建订单，使用主库");
        // 写操作走主库
    }

    @ReadOnlyDataSource
    @Transactional(readOnly = true)
    public void listOrder() {
        log.info("查询订单列表，使用从库");
        // 可接受延迟的查询走从库
    }

    @Transactional(readOnly = true)
    public void getOrderAfterCreate() {
        log.info("写后立即读订单，使用主库保证一致性");
        // 写后读、强一致读走主库
    }
}
```

读写分离使用建议如下：

| 建议                        | 说明                         |
| --------------------------- | ---------------------------- |
| 写操作必须走主库            | 从库默认只读                 |
| 写后读走主库                | 避免复制延迟导致读不到       |
| 强一致读走主库              | 例如支付结果、订单提交后详情 |
| 报表和列表可走从库          | 可接受一定延迟               |
| 从库账号只读                | 数据库权限层面防止误写       |
| 监控复制延迟                | 延迟过高时降级走主库         |
| 事务内不要切换数据源        | 同一事务保持同一连接         |
| 不建议仅靠 SQL 自动识别读写 | 注解或明确路由更可控         |

读写分离不是性能优化的第一步。应先优化 SQL、索引、分页、缓存和连接池；当主库读压力仍然明显时，再引入读写分离。

## 数据库版本管理

数据库版本管理用于把数据库结构、基础数据、权限、索引、函数、视图、触发器等变更纳入代码仓库和发布流程，避免开发、测试、预发、生产环境结构不一致。PostgreSQL 项目中常用 Flyway 或 Liquibase 管理版本，核心原则是：所有数据库变更都必须脚本化、可追踪、可审查、可重复执行或明确只能执行一次。

### DDL 脚本管理

DDL 脚本用于管理数据库结构变更，例如 Schema、表、字段、索引、约束、视图、函数、触发器、分区表和扩展插件。DDL 脚本必须进入版本库，不能只在数据库客户端手工执行。

常见 DDL 类型如下：

| 类型   | 示例                                            |
| ------ | ----------------------------------------------- |
| Schema | `CREATE SCHEMA`                                 |
| 表     | `CREATE TABLE`、`ALTER TABLE`、`DROP TABLE`     |
| 字段   | `ADD COLUMN`、`ALTER COLUMN`、`DROP COLUMN`     |
| 约束   | `PRIMARY KEY`、`UNIQUE`、`CHECK`、`FOREIGN KEY` |
| 索引   | `CREATE INDEX`、`DROP INDEX`                    |
| 视图   | `CREATE VIEW`、`CREATE MATERIALIZED VIEW`       |
| 函数   | `CREATE FUNCTION`                               |
| 触发器 | `CREATE TRIGGER`                                |
| 扩展   | `CREATE EXTENSION`                              |

推荐目录结构：

```text
src/main/resources/db/migration
├── V1__create_schema.sql
├── V2__create_user_tables.sql
├── V3__create_order_tables.sql
├── V4__create_indexes.sql
├── V5__create_views.sql
├── V6__create_functions.sql
└── V7__create_triggers.sql
```

创建 Schema 脚本：

```sql
CREATE SCHEMA IF NOT EXISTS app;
```

创建业务表示例：

```sql
CREATE TABLE IF NOT EXISTS app.order_info (
    id BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    order_no VARCHAR(64) NOT NULL,
    user_id BIGINT NOT NULL,
    order_status VARCHAR(32) NOT NULL,
    total_amount NUMERIC(12, 2) NOT NULL DEFAULT 0,
    deleted BOOLEAN NOT NULL DEFAULT FALSE,
    create_time TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_time TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uk_order_info_order_no UNIQUE (order_no)
);
```

创建索引脚本：

```sql
CREATE INDEX IF NOT EXISTS idx_order_info_user_time
ON app.order_info (user_id, create_time DESC, id DESC);

CREATE INDEX IF NOT EXISTS idx_order_info_status_time
ON app.order_info (order_status, create_time DESC, id DESC);

CREATE INDEX IF NOT EXISTS idx_order_info_not_deleted_time
ON app.order_info (create_time DESC, id DESC)
WHERE deleted = FALSE;
```

字段变更脚本：

```sql
ALTER TABLE app.order_info
ADD COLUMN IF NOT EXISTS pay_time TIMESTAMP;

COMMENT ON COLUMN app.order_info.pay_time IS '支付时间';
```

DDL 脚本管理建议如下：

| 建议                  | 说明                                   |
| --------------------- | -------------------------------------- |
| 所有 DDL 进入代码仓库 | 不允许只手工改库                       |
| 脚本按版本递增        | 便于追踪变更顺序                       |
| 已发布脚本不修改      | 后续变更新增脚本                       |
| 大表 DDL 单独评估     | 避免长锁、长事务                       |
| 索引命名统一          | 如 `idx_表名_字段`、`uk_表名_字段`     |
| 危险 DDL 必须评审     | `DROP`、字段类型变更、大表加非空字段等 |
| 脚本执行前备份        | 生产变更前必须有回退手段               |

### DML 脚本管理

DML 脚本用于管理基础数据、字典数据、配置数据、菜单权限、角色权限和数据修复。DML 脚本与业务数据直接相关，必须幂等、可重复执行，并且避免误覆盖生产数据。

常见 DML 类型如下：

| 类型       | 示例                   |
| ---------- | ---------------------- |
| 初始化配置 | 系统参数、开关配置     |
| 字典数据   | 状态、类型、枚举值     |
| 菜单权限   | 后台菜单、按钮权限     |
| 角色权限   | 默认角色、默认授权     |
| 数据修复   | 修正异常状态、补齐字段 |
| 数据迁移   | 旧字段迁移到新字段     |

推荐使用 `INSERT ... ON CONFLICT` 实现幂等初始化：

```sql
INSERT INTO app.sys_config (
    config_key,
    config_value,
    remark,
    create_time,
    update_time
) VALUES
    (
        'system.name',
        'PostgreSQL 管理系统',
        '系统名称',
        CURRENT_TIMESTAMP,
        CURRENT_TIMESTAMP
    ),
    (
        'system.locale',
        'zh-CN',
        '系统默认语言',
        CURRENT_TIMESTAMP,
        CURRENT_TIMESTAMP
    )
ON CONFLICT (config_key) DO UPDATE
SET
    config_value = EXCLUDED.config_value,
    remark = EXCLUDED.remark,
    update_time = CURRENT_TIMESTAMP;
```

字典数据初始化：

```sql
INSERT INTO app.sys_dict_item (
    dict_type,
    item_code,
    item_name,
    sort_order,
    enabled,
    create_time,
    update_time
) VALUES
    ('order_status', 'PENDING', '待支付', 1, TRUE, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
    ('order_status', 'PAID', '已支付', 2, TRUE, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
    ('order_status', 'CANCELLED', '已取消', 3, TRUE, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
    ('order_status', 'FINISHED', '已完成', 4, TRUE, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)
ON CONFLICT (dict_type, item_code) DO UPDATE
SET
    item_name = EXCLUDED.item_name,
    sort_order = EXCLUDED.sort_order,
    enabled = EXCLUDED.enabled,
    update_time = CURRENT_TIMESTAMP;
```

数据修复脚本应先查询影响范围：

```sql
SELECT
    COUNT(*) AS affected_count
FROM app.order_info
WHERE order_status = 'PAID'
  AND pay_time IS NULL;
```

再执行修复：

```sql
UPDATE app.order_info
SET
    pay_time = update_time,
    update_time = CURRENT_TIMESTAMP
WHERE order_status = 'PAID'
  AND pay_time IS NULL;
```

DML 脚本管理建议如下：

| 建议               | 说明                           |
| ------------------ | ------------------------------ |
| 初始化数据必须幂等 | 使用 `ON CONFLICT`             |
| 数据修复先查后改   | 明确影响范围                   |
| 生产修复使用事务   | 可回滚、可验证                 |
| 避免无条件更新     | 必须有明确 `WHERE` 条件        |
| 敏感数据不进脚本   | 密码、密钥、Token 不提交仓库   |
| 大批量修复分批执行 | 避免长事务和大量锁             |
| 保留执行记录       | 记录脚本版本、执行时间、执行人 |

### 初始化脚本

初始化脚本用于新环境首次创建数据库结构和基础数据。它通常包括 Schema、扩展、表结构、索引、基础配置、默认角色、默认字典等内容。

推荐初始化顺序如下：

| 顺序 | 内容                   |
| ---- | ---------------------- |
| 1    | 创建 Schema            |
| 2    | 安装扩展插件           |
| 3    | 创建基础表             |
| 4    | 创建业务表             |
| 5    | 创建约束和索引         |
| 6    | 创建视图、函数、触发器 |
| 7    | 初始化字典和配置       |
| 8    | 授权角色权限           |

初始化扩展示例：

```sql
CREATE SCHEMA IF NOT EXISTS app;

CREATE EXTENSION IF NOT EXISTS pgcrypto;
CREATE EXTENSION IF NOT EXISTS pg_trgm;
CREATE EXTENSION IF NOT EXISTS btree_gist;
```

初始化系统配置表：

```sql
CREATE TABLE IF NOT EXISTS app.sys_config (
    id BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    config_key VARCHAR(128) NOT NULL,
    config_value TEXT,
    remark VARCHAR(255),
    create_time TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_time TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uk_sys_config_key UNIQUE (config_key)
);
```

初始化默认配置：

```sql
INSERT INTO app.sys_config (
    config_key,
    config_value,
    remark
) VALUES
    ('system.name', 'PostgreSQL 管理系统', '系统名称'),
    ('system.version', '1.0.0', '系统版本'),
    ('system.locale', 'zh-CN', '默认语言')
ON CONFLICT (config_key) DO UPDATE
SET
    config_value = EXCLUDED.config_value,
    remark = EXCLUDED.remark,
    update_time = CURRENT_TIMESTAMP;
```

初始化脚本建议如下：

| 建议                     | 说明                                      |
| ------------------------ | ----------------------------------------- |
| 保持可重复执行           | 尽量使用 `IF NOT EXISTS` 和 `ON CONFLICT` |
| 初始化数据和测试数据分离 | 生产环境不能包含测试账号和测试数据        |
| 环境变量不要写入 SQL     | 密码和密钥由部署系统注入                  |
| 初始化脚本按模块拆分     | 用户、订单、权限、配置分开                |
| 新环境自动执行           | 集成到 Flyway、Liquibase 或部署脚本       |
| 初始化后执行验证         | 检查表、索引、基础数据是否完整            |

### 变更脚本

变更脚本用于已存在环境的数据库升级。它通常对应一次需求、缺陷修复或版本发布。变更脚本必须考虑向前兼容、锁影响、数据迁移、回滚方案和灰度发布。

常见变更类型如下：

| 类型         | 示例                         |
| ------------ | ---------------------------- |
| 新增字段     | `ADD COLUMN`                 |
| 修改字段类型 | `ALTER COLUMN TYPE`          |
| 新增索引     | `CREATE INDEX`               |
| 新增表       | `CREATE TABLE`               |
| 新增约束     | `ADD CONSTRAINT`             |
| 拆表迁移     | 新表 + 数据迁移              |
| 视图调整     | `CREATE OR REPLACE VIEW`     |
| 函数调整     | `CREATE OR REPLACE FUNCTION` |

新增可空字段通常风险较低：

```sql
ALTER TABLE app.order_info
ADD COLUMN IF NOT EXISTS remark VARCHAR(255);

COMMENT ON COLUMN app.order_info.remark IS '订单备注';
```

新增带默认值字段要评估表大小和版本行为：

```sql
ALTER TABLE app.order_info
ADD COLUMN IF NOT EXISTS source_type VARCHAR(32);

UPDATE app.order_info
SET source_type = 'APP'
WHERE source_type IS NULL;

ALTER TABLE app.order_info
ALTER COLUMN source_type SET DEFAULT 'APP';

ALTER TABLE app.order_info
ALTER COLUMN source_type SET NOT NULL;
```

上面采用分步方式，避免在大表上一条 DDL 同时完成复杂变更。

生产大表新增索引建议使用并发创建：

```sql
CREATE INDEX CONCURRENTLY IF NOT EXISTS idx_order_info_source_time
ON app.order_info (source_type, create_time DESC);
```

需要注意，`CREATE INDEX CONCURRENTLY` 不能放在普通事务块中执行。如果迁移工具默认把脚本放在事务里，需要单独配置。

变更脚本建议如下：

| 建议                     | 说明                           |
| ------------------------ | ------------------------------ |
| 一个脚本只做一类清晰变更 | 降低排查难度                   |
| 大表变更分阶段           | 新增字段、回填数据、加约束分开 |
| 新增索引用并发方式       | 降低阻塞写入风险               |
| 删除字段延后执行         | 先应用不再使用，再下个版本删除 |
| 保持向前兼容             | 滚动发布时新旧应用都能运行     |
| 高风险变更先预发验证     | 使用生产相近数据量测试         |
| 记录变更说明             | 说明影响范围和回滚方式         |

### 回滚脚本

回滚脚本用于变更失败后恢复到变更前状态。并不是所有数据库变更都能简单回滚，尤其是字段删除、数据删除、字段类型收缩、数据迁移覆盖等操作。因此每个高风险变更都应提前设计回滚策略。

常见回滚类型如下：

| 变更         | 回滚方式                           |
| ------------ | ---------------------------------- |
| 新增字段     | `DROP COLUMN`，前提是未被应用依赖  |
| 新增索引     | `DROP INDEX`                       |
| 新增表       | `DROP TABLE`，前提是无有效业务数据 |
| 新增配置     | `DELETE` 或恢复旧值                |
| 数据修复     | 根据备份表恢复                     |
| 字段类型变更 | 可能无法无损回滚                   |
| 删除字段     | 通常依赖备份恢复                   |

新增索引的回滚：

```sql
DROP INDEX CONCURRENTLY IF EXISTS app.idx_order_info_source_time;
```

新增字段的回滚：

```sql
ALTER TABLE app.order_info
DROP COLUMN IF EXISTS source_type;
```

配置变更回滚：

```sql
UPDATE app.sys_config
SET
    config_value = 'old-value',
    update_time = CURRENT_TIMESTAMP
WHERE config_key = 'system.some_config';
```

数据修复前先创建备份表：

```sql
CREATE TABLE app.order_info_backup_20260509 AS
SELECT *
FROM app.order_info
WHERE order_status = 'PAID'
  AND pay_time IS NULL;
```

数据修复回滚：

```sql
UPDATE app.order_info o
SET
    pay_time = b.pay_time,
    update_time = CURRENT_TIMESTAMP
FROM app.order_info_backup_20260509 b
WHERE o.id = b.id;
```

回滚脚本建议如下：

| 建议                     | 说明                                   |
| ------------------------ | -------------------------------------- |
| 高风险变更必须有回滚脚本 | 发布前准备好                           |
| 数据变更先备份影响范围   | 不依赖全库恢复                         |
| 回滚脚本也要测试         | 不能只写不验证                         |
| 删除类变更谨慎           | 删除字段、删表、删数据通常难以快速回滚 |
| 回滚和应用版本联动       | 数据库回滚不等于应用一定可回滚         |
| 记录不可逆变更           | 明确说明只能通过备份恢复               |

### 数据迁移脚本

数据迁移脚本用于将旧结构中的数据迁移到新结构，常见于字段拆分、表拆分、状态枚举变更、JSONB 字段普通列化、历史表归档等场景。数据迁移需要特别关注数据量、执行时间、锁范围、失败重试和幂等性。

示例：将订单扩展信息中的渠道字段迁移到普通列。

先新增字段：

```sql
ALTER TABLE app.order_info
ADD COLUMN IF NOT EXISTS channel_code VARCHAR(32);

COMMENT ON COLUMN app.order_info.channel_code IS '订单渠道编码';
```

分批迁移数据：

```sql
WITH batch_data AS (
    SELECT
        id,
        extra_data ->> 'channel' AS channel_code
    FROM app.order_info
    WHERE channel_code IS NULL
      AND extra_data ? 'channel'
    ORDER BY id
    LIMIT 1000
)
UPDATE app.order_info o
SET
    channel_code = b.channel_code,
    update_time = CURRENT_TIMESTAMP
FROM batch_data b
WHERE o.id = b.id;
```

验证剩余未迁移数量：

```sql
SELECT
    COUNT(*) AS remain_count
FROM app.order_info
WHERE channel_code IS NULL
  AND extra_data ? 'channel';
```

迁移完成后创建索引：

```sql
CREATE INDEX CONCURRENTLY IF NOT EXISTS idx_order_info_channel_time
ON app.order_info (channel_code, create_time DESC);
```

示例：旧状态值迁移为新状态值。

```sql
UPDATE app.order_info
SET
    order_status = CASE order_status
        WHEN 'WAIT_PAY' THEN 'PENDING'
        WHEN 'PAY_SUCCESS' THEN 'PAID'
        WHEN 'CLOSE' THEN 'CANCELLED'
        ELSE order_status
    END,
    update_time = CURRENT_TIMESTAMP
WHERE order_status IN ('WAIT_PAY', 'PAY_SUCCESS', 'CLOSE');
```

迁移前后校验：

```sql
SELECT
    order_status,
    COUNT(*) AS order_count
FROM app.order_info
GROUP BY order_status
ORDER BY order_status;
```

数据迁移建议如下：

| 建议               | 说明                         |
| ------------------ | ---------------------------- |
| 大数据量分批执行   | 避免长事务和锁等待           |
| 迁移脚本保持幂等   | 支持失败后重复执行           |
| 先新增后迁移再切换 | 不要一步到位高风险变更       |
| 迁移前备份关键数据 | 尤其是覆盖式更新             |
| 迁移后做数据校验   | 数量、金额、状态分布都要核对 |
| 应用兼容新旧字段   | 滚动发布期间尤其重要         |
| 删除旧字段延后     | 确认新版本稳定后再删除       |

典型安全迁移流程如下：

| 阶段   | 动作                 |
| ------ | -------------------- |
| 第一步 | 新增新字段或新表     |
| 第二步 | 应用双写或兼容读     |
| 第三步 | 批量迁移历史数据     |
| 第四步 | 校验新旧数据一致     |
| 第五步 | 应用切换读取新字段   |
| 第六步 | 观察稳定后删除旧字段 |

### 环境差异管理

环境差异管理用于保证开发、测试、预发、生产数据库结构一致，同时允许必要的配置差异。结构差异越多，发布风险越高。数据库对象应由同一套迁移脚本生成，环境差异只应体现在连接信息、账号权限、配置值和数据规模上。

常见环境差异如下：

| 差异类型 | 示例                   | 管理方式             |
| -------- | ---------------------- | -------------------- |
| 连接信息 | host、port、database   | 配置文件或环境变量   |
| 账号密码 | app_user、report_user  | 密钥管理系统         |
| 数据规模 | 测试数据少、生产数据多 | 不通过结构脚本区分   |
| 配置值   | 回调地址、开关值       | 环境配置表或配置中心 |
| 权限     | 生产更严格             | 权限脚本区分环境     |
| 扩展插件 | PostGIS 等按需安装     | 初始化检查           |
| 性能参数 | 连接池、超时           | 应用配置区分         |

不推荐为不同环境维护不同结构脚本：

```text
不推荐：
db/dev/create_tables.sql
db/test/create_tables.sql
db/prod/create_tables.sql
```

推荐统一迁移脚本：

```text
推荐：
db/migration/V1__create_schema.sql
db/migration/V2__create_tables.sql
db/migration/V3__create_indexes.sql
```

环境配置数据可以单独管理：

```text
src/main/resources/db/env
├── dev/V1001__init_dev_config.sql
├── test/V1001__init_test_config.sql
└── prod/V1001__init_prod_config.sql
```

生产配置初始化示例：

```sql
INSERT INTO app.sys_config (
    config_key,
    config_value,
    remark
) VALUES
    ('system.env', 'prod', '当前运行环境'),
    ('system.debug', 'false', '是否开启调试模式')
ON CONFLICT (config_key) DO UPDATE
SET
    config_value = EXCLUDED.config_value,
    remark = EXCLUDED.remark,
    update_time = CURRENT_TIMESTAMP;
```

检查环境对象差异：

```sql
SELECT
    table_schema,
    table_name,
    table_type
FROM information_schema.tables
WHERE table_schema = 'app'
ORDER BY table_name;
```

检查字段差异：

```sql
SELECT
    table_schema,
    table_name,
    column_name,
    data_type,
    is_nullable,
    column_default
FROM information_schema.columns
WHERE table_schema = 'app'
ORDER BY table_name, ordinal_position;
```

检查索引差异：

```sql
SELECT
    schemaname,
    tablename,
    indexname,
    indexdef
FROM pg_indexes
WHERE schemaname = 'app'
ORDER BY tablename, indexname;
```

环境差异管理建议如下：

| 建议                       | 说明                           |
| -------------------------- | ------------------------------ |
| 所有环境使用同一套结构脚本 | 避免结构漂移                   |
| 环境变量管理连接和密码     | 不写死在脚本中                 |
| 配置数据允许环境差异       | 但要显式管理                   |
| 生产权限更严格             | 应用账号不应有 DDL 权限        |
| 定期对比结构               | 发现手工改库痕迹               |
| 预发环境贴近生产           | 尤其是扩展、版本、参数、数据量 |
| 禁止跳过迁移版本           | 避免环境版本链断裂             |

### 发布流程集成

数据库版本管理必须集成到发布流程中。应用发布和数据库变更往往相互依赖，尤其是字段新增、字段删除、表拆分、索引新增、数据迁移等操作。发布前应明确数据库脚本执行顺序、应用版本兼容性、回滚方式和验证步骤。

推荐发布流程如下：

| 阶段     | 动作                         |
| -------- | ---------------------------- |
| 开发阶段 | 编写 DDL、DML、迁移脚本      |
| 代码评审 | 同时评审数据库脚本           |
| 测试环境 | 自动执行迁移并跑测试         |
| 预发环境 | 使用接近生产数据验证         |
| 发布前   | 备份、确认脚本、确认回滚方案 |
| 发布中   | 先执行兼容性数据库变更       |
| 应用发布 | 滚动发布或灰度发布           |
| 发布后   | 执行验证 SQL 和监控观察      |
| 稳定后   | 清理旧字段、旧索引、旧数据   |

兼容性发布示例：新增字段。

第一版数据库先新增字段，应用仍可不使用：

```sql
ALTER TABLE app.order_info
ADD COLUMN IF NOT EXISTS source_type VARCHAR(32);

ALTER TABLE app.order_info
ALTER COLUMN source_type SET DEFAULT 'APP';
```

第二步发布应用，开始写入新字段。

第三步回填历史数据：

```sql
UPDATE app.order_info
SET source_type = 'APP'
WHERE source_type IS NULL;
```

第四步确认无空值后再加非空约束：

```sql
ALTER TABLE app.order_info
ALTER COLUMN source_type SET NOT NULL;
```

发布后验证 SQL：

```sql
SELECT
    COUNT(*) AS null_source_type_count
FROM app.order_info
WHERE source_type IS NULL;
```

大表变更发布建议如下：

| 变更         | 发布策略                           |
| ------------ | ---------------------------------- |
| 新增可空字段 | 可先执行                           |
| 新增非空字段 | 分步：加字段、回填、加默认、加非空 |
| 新增索引     | 使用 `CREATE INDEX CONCURRENTLY`   |
| 删除字段     | 延后到确认应用不再使用后           |
| 修改字段类型 | 新增字段迁移后切换，避免直接强改   |
| 大批量更新   | 分批执行，避开高峰                 |
| 表拆分       | 双写、迁移、校验、切读、清理       |

CI/CD 集成建议如下：

| 建议                 | 说明                       |
| -------------------- | -------------------------- |
| 自动执行迁移         | 测试环境和预发环境自动化   |
| 生产执行需审批       | 高风险变更人工确认         |
| 发布前检查数据库版本 | 确认迁移历史完整           |
| 迁移失败阻断发布     | 不允许应用带着失败结构启动 |
| 脚本执行日志归档     | 记录版本、耗时、执行结果   |
| 发布后自动验证       | 执行关键 SQL 校验          |
| 监控慢 SQL 和锁等待  | 新索引、字段变更后重点观察 |

Flyway 发布检查示例：

```sql
SELECT
    installed_rank,
    version,
    description,
    type,
    script,
    installed_by,
    installed_on,
    execution_time,
    success
FROM app.flyway_schema_history
ORDER BY installed_rank DESC;
```

Liquibase 发布检查示例：

```sql
SELECT
    id,
    author,
    filename,
    dateexecuted,
    orderexecuted,
    exectype,
    md5sum,
    description
FROM app.databasechangelog
ORDER BY orderexecuted DESC;
```

发布流程中的数据库变更原则如下：

| 原则                     | 说明                         |
| ------------------------ | ---------------------------- |
| 先兼容，后切换，最后清理 | 避免滚动发布期间新旧版本冲突 |
| 先结构，后数据，再约束   | 大表变更更安全               |
| 先灰度，后全量           | 复杂变更逐步放量             |
| 先备份，后执行           | 高风险操作必须可恢复         |
| 先验证，后收尾           | 不确认成功不做清理           |
| 发布和回滚都要演练       | 只会发布不会回滚风险很高     |

数据库版本管理的目标不是单纯保存 SQL 文件，而是让数据库变更具备工程化能力：可审查、可执行、可验证、可追踪、可回滚。对于 Java 和 Spring Boot 项目，建议优先选择 Flyway 或 Liquibase，并将迁移脚本纳入 CI/CD 发布链路。

## 常用业务设计

常用业务设计用于沉淀项目中高频出现的表结构模式，例如用户、字典、配置、订单、日志、审计字段、逻辑删除、多租户、树形结构和标签结构。表结构设计应优先保证数据一致性、查询性能、扩展性和可维护性，不建议为了短期开发方便忽略约束、索引和字段语义。

### 用户表设计

用户表用于存储系统用户的基础信息。实际项目中，用户基础信息、认证信息、角色关系、登录日志通常应拆分设计，不建议把密码、角色、登录记录全部放在同一张用户表中。

用户基础表示例：

```sql
CREATE TABLE app.sys_user (
    id BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    username VARCHAR(64) NOT NULL,
    nickname VARCHAR(64),
    phone VARCHAR(20),
    email VARCHAR(128),
    avatar_url VARCHAR(512),
    enabled BOOLEAN NOT NULL DEFAULT TRUE,
    locked BOOLEAN NOT NULL DEFAULT FALSE,
    deleted BOOLEAN NOT NULL DEFAULT FALSE,
    version INTEGER NOT NULL DEFAULT 0,
    create_user_id BIGINT,
    update_user_id BIGINT,
    create_time TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_time TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uk_sys_user_username UNIQUE (username),
    CONSTRAINT uk_sys_user_phone UNIQUE (phone),
    CONSTRAINT uk_sys_user_email UNIQUE (email)
);

COMMENT ON TABLE app.sys_user IS '系统用户表';
COMMENT ON COLUMN app.sys_user.id IS '用户ID';
COMMENT ON COLUMN app.sys_user.username IS '用户名';
COMMENT ON COLUMN app.sys_user.nickname IS '用户昵称';
COMMENT ON COLUMN app.sys_user.phone IS '手机号';
COMMENT ON COLUMN app.sys_user.email IS '邮箱';
COMMENT ON COLUMN app.sys_user.avatar_url IS '头像地址';
COMMENT ON COLUMN app.sys_user.enabled IS '是否启用';
COMMENT ON COLUMN app.sys_user.locked IS '是否锁定';
COMMENT ON COLUMN app.sys_user.deleted IS '是否删除';
COMMENT ON COLUMN app.sys_user.version IS '乐观锁版本号';
```

用户认证信息单独建表：

```sql
CREATE TABLE app.sys_user_auth (
    id BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    user_id BIGINT NOT NULL,
    password_hash TEXT NOT NULL,
    password_update_time TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    last_login_time TIMESTAMP,
    create_time TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_time TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uk_sys_user_auth_user_id UNIQUE (user_id),
    CONSTRAINT fk_sys_user_auth_user_id
        FOREIGN KEY (user_id)
        REFERENCES app.sys_user (id)
);

COMMENT ON TABLE app.sys_user_auth IS '用户认证信息表';
COMMENT ON COLUMN app.sys_user_auth.password_hash IS '密码哈希';
```

常用索引：

```sql
CREATE INDEX idx_sys_user_enabled_deleted
ON app.sys_user (enabled, deleted);

CREATE INDEX idx_sys_user_create_time
ON app.sys_user (create_time DESC, id DESC);

CREATE INDEX idx_sys_user_nickname_trgm
ON app.sys_user
USING GIN (nickname gin_trgm_ops);
```

用户查询示例：

```sql
SELECT
    id,
    username,
    nickname,
    phone,
    email,
    enabled,
    locked,
    create_time
FROM app.sys_user
WHERE deleted = FALSE
  AND enabled = TRUE
ORDER BY create_time DESC, id DESC
LIMIT 20;
```

用户表设计建议如下：

| 建议                                | 说明                                   |
| ----------------------------------- | -------------------------------------- |
| 用户基础信息和认证信息分表          | 密码哈希等敏感字段隔离                 |
| 用户名、手机号、邮箱加唯一约束      | 由数据库兜底保证唯一                   |
| 密码只存哈希                        | 不存明文，不存可逆密码                 |
| 使用 `enabled` 和 `locked` 区分状态 | 禁用和锁定语义不同                     |
| 使用逻辑删除字段                    | 用户数据通常不物理删除                 |
| 敏感字段查询走脱敏视图              | 报表和后台列表不要直接暴露手机号、邮箱 |
| 常用查询字段建索引                  | 如 `deleted`、`enabled`、`create_time` |

### 字典表设计

字典表用于维护系统中的枚举、状态、类型、分类等基础数据，例如订单状态、用户性别、消息类型、支付方式等。字典表适合后台可维护、需要排序、需要启停、需要多语言扩展的枚举数据。

字典类型表：

```sql
CREATE TABLE app.sys_dict_type (
    id BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    dict_type VARCHAR(64) NOT NULL,
    dict_name VARCHAR(128) NOT NULL,
    remark VARCHAR(255),
    enabled BOOLEAN NOT NULL DEFAULT TRUE,
    deleted BOOLEAN NOT NULL DEFAULT FALSE,
    create_time TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_time TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uk_sys_dict_type UNIQUE (dict_type)
);

COMMENT ON TABLE app.sys_dict_type IS '字典类型表';
COMMENT ON COLUMN app.sys_dict_type.dict_type IS '字典类型编码';
COMMENT ON COLUMN app.sys_dict_type.dict_name IS '字典类型名称';
```

字典项表：

```sql
CREATE TABLE app.sys_dict_item (
    id BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    dict_type VARCHAR(64) NOT NULL,
    item_code VARCHAR(64) NOT NULL,
    item_name VARCHAR(128) NOT NULL,
    item_value VARCHAR(255),
    sort_order INTEGER NOT NULL DEFAULT 0,
    remark VARCHAR(255),
    enabled BOOLEAN NOT NULL DEFAULT TRUE,
    deleted BOOLEAN NOT NULL DEFAULT FALSE,
    create_time TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_time TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uk_sys_dict_item UNIQUE (dict_type, item_code)
);

COMMENT ON TABLE app.sys_dict_item IS '字典项表';
COMMENT ON COLUMN app.sys_dict_item.dict_type IS '字典类型编码';
COMMENT ON COLUMN app.sys_dict_item.item_code IS '字典项编码';
COMMENT ON COLUMN app.sys_dict_item.item_name IS '字典项名称';
COMMENT ON COLUMN app.sys_dict_item.sort_order IS '排序号';
```

初始化订单状态字典：

```sql
INSERT INTO app.sys_dict_type (
    dict_type,
    dict_name,
    remark
) VALUES (
    'order_status',
    '订单状态',
    '订单主流程状态'
)
ON CONFLICT (dict_type) DO UPDATE
SET
    dict_name = EXCLUDED.dict_name,
    remark = EXCLUDED.remark,
    update_time = CURRENT_TIMESTAMP;

INSERT INTO app.sys_dict_item (
    dict_type,
    item_code,
    item_name,
    sort_order,
    enabled
) VALUES
    ('order_status', 'PENDING', '待支付', 1, TRUE),
    ('order_status', 'PAID', '已支付', 2, TRUE),
    ('order_status', 'CANCELLED', '已取消', 3, TRUE),
    ('order_status', 'FINISHED', '已完成', 4, TRUE)
ON CONFLICT (dict_type, item_code) DO UPDATE
SET
    item_name = EXCLUDED.item_name,
    sort_order = EXCLUDED.sort_order,
    enabled = EXCLUDED.enabled,
    update_time = CURRENT_TIMESTAMP;
```

查询字典项：

```sql
SELECT
    item_code,
    item_name,
    item_value,
    sort_order
FROM app.sys_dict_item
WHERE dict_type = 'order_status'
  AND enabled = TRUE
  AND deleted = FALSE
ORDER BY sort_order ASC, id ASC;
```

字典表设计建议如下：

| 建议               | 说明                                    |
| ------------------ | --------------------------------------- |
| 类型表和明细表分开 | 方便管理多个字典类型                    |
| 字典编码保持稳定   | 不要随意修改 `dict_type` 和 `item_code` |
| 使用唯一约束兜底   | `UNIQUE(dict_type, item_code)`          |
| 支持排序和启停     | 后台展示和业务过滤常用                  |
| 字典数据初始化幂等 | 使用 `ON CONFLICT`                      |
| 高频字典可缓存     | 应用层缓存减少数据库查询                |
| 不要滥用字典       | 强业务状态仍需代码枚举配合控制流转      |

### 配置表设计

配置表用于保存系统运行参数、业务开关、第三方配置、功能开关等。配置表应支持唯一配置键、值类型、启停状态、备注和更新时间。敏感配置不建议明文保存在普通配置表中。

配置表示例：

```sql
CREATE TABLE app.sys_config (
    id BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    config_key VARCHAR(128) NOT NULL,
    config_value TEXT,
    value_type VARCHAR(32) NOT NULL DEFAULT 'TEXT',
    config_group VARCHAR(64) NOT NULL DEFAULT 'default',
    remark VARCHAR(255),
    enabled BOOLEAN NOT NULL DEFAULT TRUE,
    deleted BOOLEAN NOT NULL DEFAULT FALSE,
    create_time TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_time TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uk_sys_config_key UNIQUE (config_key),
    CONSTRAINT ck_sys_config_value_type CHECK (
        value_type IN ('TEXT', 'NUMBER', 'BOOLEAN', 'JSON')
    )
);

COMMENT ON TABLE app.sys_config IS '系统配置表';
COMMENT ON COLUMN app.sys_config.config_key IS '配置键';
COMMENT ON COLUMN app.sys_config.config_value IS '配置值';
COMMENT ON COLUMN app.sys_config.value_type IS '配置值类型';
COMMENT ON COLUMN app.sys_config.config_group IS '配置分组';
```

初始化配置：

```sql
INSERT INTO app.sys_config (
    config_key,
    config_value,
    value_type,
    config_group,
    remark
) VALUES
    ('system.name', 'PostgreSQL 管理系统', 'TEXT', 'system', '系统名称'),
    ('system.debug', 'false', 'BOOLEAN', 'system', '是否开启调试'),
    ('order.timeout.minutes', '30', 'NUMBER', 'order', '订单超时时间，单位分钟')
ON CONFLICT (config_key) DO UPDATE
SET
    config_value = EXCLUDED.config_value,
    value_type = EXCLUDED.value_type,
    config_group = EXCLUDED.config_group,
    remark = EXCLUDED.remark,
    update_time = CURRENT_TIMESTAMP;
```

查询配置：

```sql
SELECT
    config_key,
    config_value,
    value_type
FROM app.sys_config
WHERE config_key = 'order.timeout.minutes'
  AND enabled = TRUE
  AND deleted = FALSE;
```

配置表设计建议如下：

| 建议                 | 说明                               |
| -------------------- | ---------------------------------- |
| 配置键唯一           | 使用 `UNIQUE(config_key)`          |
| 配置键使用层级命名   | 如 `order.timeout.minutes`         |
| 增加配置类型         | 便于应用转换和校验                 |
| 配置初始化幂等       | 使用 `ON CONFLICT`                 |
| 敏感配置不要明文保存 | 密码、密钥、Token 使用密钥管理系统 |
| 配置变更要审计       | 记录修改人、修改前后值             |
| 高频配置应用层缓存   | 配合刷新机制或版本号               |

### 订单表设计

订单表是典型核心业务表，通常包含主表、明细表、支付表、状态流转日志等。订单主表保存订单维度的信息，订单明细保存商品行，状态流转日志保存关键状态变化。

订单主表：

```sql
CREATE TABLE app.order_info (
    id BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    order_no VARCHAR(64) NOT NULL,
    user_id BIGINT NOT NULL,
    order_status VARCHAR(32) NOT NULL,
    total_amount NUMERIC(12, 2) NOT NULL DEFAULT 0,
    pay_amount NUMERIC(12, 2) NOT NULL DEFAULT 0,
    source_type VARCHAR(32),
    pay_time TIMESTAMP,
    cancel_time TIMESTAMP,
    finish_time TIMESTAMP,
    remark VARCHAR(255),
    deleted BOOLEAN NOT NULL DEFAULT FALSE,
    version INTEGER NOT NULL DEFAULT 0,
    create_time TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_time TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uk_order_info_order_no UNIQUE (order_no),
    CONSTRAINT ck_order_info_total_amount CHECK (total_amount >= 0),
    CONSTRAINT ck_order_info_pay_amount CHECK (pay_amount >= 0)
);

COMMENT ON TABLE app.order_info IS '订单主表';
COMMENT ON COLUMN app.order_info.order_no IS '订单号';
COMMENT ON COLUMN app.order_info.order_status IS '订单状态';
COMMENT ON COLUMN app.order_info.total_amount IS '订单总金额';
COMMENT ON COLUMN app.order_info.pay_amount IS '实付金额';
```

订单明细表：

```sql
CREATE TABLE app.order_item (
    id BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    order_id BIGINT NOT NULL,
    product_id BIGINT NOT NULL,
    product_name VARCHAR(128) NOT NULL,
    quantity INTEGER NOT NULL,
    sale_price NUMERIC(12, 2) NOT NULL,
    item_amount NUMERIC(12, 2) NOT NULL,
    create_time TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_order_item_order_id
        FOREIGN KEY (order_id)
        REFERENCES app.order_info (id),
    CONSTRAINT ck_order_item_quantity CHECK (quantity > 0),
    CONSTRAINT ck_order_item_sale_price CHECK (sale_price >= 0),
    CONSTRAINT ck_order_item_amount CHECK (item_amount >= 0)
);

COMMENT ON TABLE app.order_item IS '订单明细表';
```

订单状态日志表：

```sql
CREATE TABLE app.order_status_log (
    id BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    order_id BIGINT NOT NULL,
    order_no VARCHAR(64) NOT NULL,
    from_status VARCHAR(32),
    to_status VARCHAR(32) NOT NULL,
    operation_type VARCHAR(64),
    operation_remark VARCHAR(255),
    operator_id BIGINT,
    create_time TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

COMMENT ON TABLE app.order_status_log IS '订单状态变更日志表';
```

常用索引：

```sql
CREATE INDEX idx_order_info_user_time
ON app.order_info (user_id, create_time DESC, id DESC);

CREATE INDEX idx_order_info_status_time
ON app.order_info (order_status, create_time DESC, id DESC);

CREATE INDEX idx_order_info_pay_time
ON app.order_info (pay_time DESC)
WHERE pay_time IS NOT NULL;

CREATE INDEX idx_order_item_order_id
ON app.order_item (order_id);

CREATE INDEX idx_order_status_log_order_id
ON app.order_status_log (order_id, create_time DESC);
```

状态流转更新：

```sql
UPDATE app.order_info
SET
    order_status = 'PAID',
    pay_time = CURRENT_TIMESTAMP,
    version = version + 1,
    update_time = CURRENT_TIMESTAMP
WHERE id = 1
  AND order_status = 'PENDING';
```

订单表设计建议如下：

| 建议                       | 说明                         |
| -------------------------- | ---------------------------- |
| 主键和订单号分开           | 主键内部使用，订单号对外展示 |
| 金额使用 `NUMERIC`         | 不使用浮点类型               |
| 状态字段使用稳定编码       | 如 `PENDING`、`PAID`         |
| 状态流转使用条件更新       | 防止并发重复处理             |
| 明细表通过 `order_id` 关联 | 明细不要冗余过多主表字段     |
| 状态变化记录日志           | 核心业务可追溯               |
| 大订单表考虑分区           | 常按 `create_time` 范围分区  |

### 日志表设计

日志表用于记录操作日志、登录日志、接口日志、任务日志、数据变更日志等。日志表通常写多读少、数据量大、生命周期明确，适合按时间分区和定期归档。

操作日志表：

```sql
CREATE TABLE app.operation_log (
    id BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    operation_type VARCHAR(64) NOT NULL,
    operation_name VARCHAR(128),
    operator_id BIGINT,
    operator_name VARCHAR(64),
    request_method VARCHAR(16),
    request_uri VARCHAR(512),
    client_ip VARCHAR(64),
    user_agent TEXT,
    request_data JSONB,
    response_data JSONB,
    success BOOLEAN NOT NULL DEFAULT TRUE,
    error_message TEXT,
    cost_time_ms INTEGER,
    create_time TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

COMMENT ON TABLE app.operation_log IS '操作日志表';
COMMENT ON COLUMN app.operation_log.operation_type IS '操作类型';
COMMENT ON COLUMN app.operation_log.cost_time_ms IS '耗时，单位毫秒';
```

登录日志表：

```sql
CREATE TABLE app.login_log (
    id BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    user_id BIGINT,
    username VARCHAR(64),
    login_ip VARCHAR(64),
    user_agent TEXT,
    login_result VARCHAR(32) NOT NULL,
    failure_reason VARCHAR(255),
    create_time TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

COMMENT ON TABLE app.login_log IS '登录日志表';
```

常用索引：

```sql
CREATE INDEX idx_operation_log_type_time
ON app.operation_log (operation_type, create_time DESC);

CREATE INDEX idx_operation_log_operator_time
ON app.operation_log (operator_id, create_time DESC);

CREATE INDEX idx_operation_log_create_time
ON app.operation_log (create_time DESC);

CREATE INDEX idx_login_log_username_time
ON app.login_log (username, create_time DESC);
```

按时间范围查询日志：

```sql
SELECT
    id,
    operation_type,
    operation_name,
    operator_name,
    success,
    cost_time_ms,
    create_time
FROM app.operation_log
WHERE create_time >= TIMESTAMP '2026-05-01 00:00:00'
  AND create_time < TIMESTAMP '2026-06-01 00:00:00'
ORDER BY create_time DESC
LIMIT 100;
```

日志表设计建议如下：

| 建议                   | 说明                                  |
| ---------------------- | ------------------------------------- |
| 日志表避免外键过多     | 防止日志写入受业务表影响              |
| 日志内容控制大小       | 请求和响应 JSON 不宜无限制存储        |
| 敏感字段脱敏           | 密码、Token、身份证等不能明文写入日志 |
| 按时间建索引           | 日志查询通常按时间范围                |
| 大日志表使用分区       | 按月或按天分区                        |
| 设置保留周期           | 定期归档或删除历史日志                |
| 写入失败不应影响主流程 | 应用层日志写入要有降级策略            |

### 审计字段设计

审计字段用于记录数据的创建人、更新人、创建时间、更新时间、版本号等。多数业务表都应统一包含审计字段，便于追踪数据来源、排查问题和做并发控制。

常见审计字段如下：

| 字段             | 类型        | 说明         |
| ---------------- | ----------- | ------------ |
| `create_user_id` | `BIGINT`    | 创建人 ID    |
| `update_user_id` | `BIGINT`    | 更新人 ID    |
| `create_time`    | `TIMESTAMP` | 创建时间     |
| `update_time`    | `TIMESTAMP` | 更新时间     |
| `version`        | `INTEGER`   | 乐观锁版本号 |
| `deleted`        | `BOOLEAN`   | 逻辑删除标记 |

业务表示例：

```sql
CREATE TABLE app.audit_business_demo (
    id BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    business_code VARCHAR(64) NOT NULL,
    business_name VARCHAR(128) NOT NULL,
    enabled BOOLEAN NOT NULL DEFAULT TRUE,
    deleted BOOLEAN NOT NULL DEFAULT FALSE,
    version INTEGER NOT NULL DEFAULT 0,
    create_user_id BIGINT,
    update_user_id BIGINT,
    create_time TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_time TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uk_audit_business_demo_code UNIQUE (business_code)
);

COMMENT ON TABLE app.audit_business_demo IS '审计字段示例表';
COMMENT ON COLUMN app.audit_business_demo.version IS '乐观锁版本号';
```

更新时维护审计字段：

```sql
UPDATE app.audit_business_demo
SET
    business_name = '新的业务名称',
    update_user_id = 10001,
    update_time = CURRENT_TIMESTAMP,
    version = version + 1
WHERE id = 1
  AND version = 0;
```

审计字段设计建议如下：

| 建议                       | 说明                                         |
| -------------------------- | -------------------------------------------- |
| 所有核心业务表统一字段     | 降低理解成本                                 |
| 时间字段使用数据库默认值   | `CURRENT_TIMESTAMP` 兜底                     |
| 操作人由应用层传入         | 数据库通常不知道业务用户 ID                  |
| 乐观锁用于并发编辑         | 更新时检查 `version`                         |
| 逻辑删除字段使用 `BOOLEAN` | PostgreSQL 中语义清晰                        |
| 字段命名保持统一           | 不同表不要混用 `gmt_create`、`created_at` 等 |
| 自动填充策略统一           | 应用层、ORM 或触发器选择一种为主             |

### 软删除设计

软删除，也叫逻辑删除，是指通过字段标记数据已删除，而不是物理删除。它适合用户、订单、配置、字典等需要保留历史和可恢复的数据。日志、临时表、缓存表等可以根据场景物理删除。

基础字段设计：

```sql
deleted BOOLEAN NOT NULL DEFAULT FALSE
```

带删除时间和删除人的设计：

```sql
deleted BOOLEAN NOT NULL DEFAULT FALSE,
delete_user_id BIGINT,
delete_time TIMESTAMP
```

示例表：

```sql
CREATE TABLE app.soft_delete_demo (
    id BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    business_code VARCHAR(64) NOT NULL,
    business_name VARCHAR(128) NOT NULL,
    deleted BOOLEAN NOT NULL DEFAULT FALSE,
    delete_user_id BIGINT,
    delete_time TIMESTAMP,
    create_time TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_time TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uk_soft_delete_demo_code UNIQUE (business_code)
);
```

执行逻辑删除：

```sql
UPDATE app.soft_delete_demo
SET
    deleted = TRUE,
    delete_user_id = 10001,
    delete_time = CURRENT_TIMESTAMP,
    update_time = CURRENT_TIMESTAMP
WHERE id = 1
  AND deleted = FALSE;
```

查询未删除数据：

```sql
SELECT
    id,
    business_code,
    business_name,
    create_time
FROM app.soft_delete_demo
WHERE deleted = FALSE
ORDER BY create_time DESC;
```

如果业务编码允许删除后重新使用，可以创建部分唯一索引：

```sql
DROP INDEX IF EXISTS app.uk_soft_delete_demo_code;

CREATE UNIQUE INDEX uk_soft_delete_demo_code_not_deleted
ON app.soft_delete_demo (business_code)
WHERE deleted = FALSE;
```

软删除设计建议如下：

| 建议                               | 说明                    |
| ---------------------------------- | ----------------------- |
| 核心业务表优先软删除               | 保留可追溯性            |
| 所有查询默认过滤 `deleted = FALSE` | 避免展示已删除数据      |
| 唯一约束按业务决定                 | 是否允许删除后复用编码  |
| 可增加删除人和删除时间             | 便于审计                |
| 历史数据定期归档                   | 逻辑删除不等于永不清理  |
| 大量软删除仍会膨胀                 | 需要 VACUUM、归档或分区 |
| 物理删除前备份                     | 清理历史数据要可追踪    |

### 多租户设计

多租户设计用于让一套系统服务多个租户。常见方案包括共享库共享表、共享库独立 Schema、独立数据库。不同方案在隔离性、成本、运维复杂度和扩展性上差异明显。

多租户方案对比：

| 方案              | 说明                 | 优点           | 缺点                              |
| ----------------- | -------------------- | -------------- | --------------------------------- |
| 共享库共享表      | 表中增加 `tenant_id` | 成本低，易运维 | 隔离性弱，所有 SQL 必须带租户条件 |
| 共享库独立 Schema | 每个租户一个 Schema  | 隔离性较好     | Schema 多时迁移复杂               |
| 独立数据库        | 每租户独立库         | 隔离性最好     | 成本和运维复杂度高                |

共享表设计示例：

```sql
CREATE TABLE app.tenant_info (
    id BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    tenant_code VARCHAR(64) NOT NULL,
    tenant_name VARCHAR(128) NOT NULL,
    enabled BOOLEAN NOT NULL DEFAULT TRUE,
    deleted BOOLEAN NOT NULL DEFAULT FALSE,
    create_time TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_time TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uk_tenant_info_code UNIQUE (tenant_code)
);

CREATE TABLE app.tenant_order (
    id BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    tenant_id BIGINT NOT NULL,
    order_no VARCHAR(64) NOT NULL,
    user_id BIGINT NOT NULL,
    order_status VARCHAR(32) NOT NULL,
    total_amount NUMERIC(12, 2) NOT NULL DEFAULT 0,
    create_time TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_time TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uk_tenant_order_no UNIQUE (tenant_id, order_no),
    CONSTRAINT fk_tenant_order_tenant_id
        FOREIGN KEY (tenant_id)
        REFERENCES app.tenant_info (id)
);
```

常用索引：

```sql
CREATE INDEX idx_tenant_order_tenant_time
ON app.tenant_order (tenant_id, create_time DESC, id DESC);

CREATE INDEX idx_tenant_order_tenant_status_time
ON app.tenant_order (tenant_id, order_status, create_time DESC);
```

租户查询必须带 `tenant_id`：

```sql
SELECT
    id,
    tenant_id,
    order_no,
    order_status,
    total_amount,
    create_time
FROM app.tenant_order
WHERE tenant_id = 10001
  AND order_status = 'PAID'
ORDER BY create_time DESC, id DESC
LIMIT 20;
```

可以使用 RLS 做数据库层隔离兜底：

```sql
ALTER TABLE app.tenant_order ENABLE ROW LEVEL SECURITY;

CREATE POLICY tenant_order_policy
ON app.tenant_order
USING (
    tenant_id = current_setting('app.current_tenant_id')::BIGINT
)
WITH CHECK (
    tenant_id = current_setting('app.current_tenant_id')::BIGINT
);
```

多租户设计建议如下：

| 建议                       | 说明                         |
| -------------------------- | ---------------------------- |
| 中小系统优先共享表         | 成本和复杂度最低             |
| 所有业务表统一 `tenant_id` | 查询、唯一约束、索引都要包含 |
| 唯一约束带租户             | 如 `UNIQUE(tenant_id, code)` |
| SQL 必须带租户条件         | 防止跨租户数据泄露           |
| 可用 RLS 兜底              | 增加数据库层保护             |
| 大租户可独立库             | 避免大租户影响其他租户       |
| 分区可按租户或时间设计     | 根据查询模式决定             |

### 树形结构设计

树形结构用于组织部门、菜单、分类、区域等层级数据。常见设计方式包括邻接表、路径枚举、闭包表。最常用的是邻接表，即每条记录保存自己的父节点 ID。

邻接表示例：

```sql
CREATE TABLE app.sys_dept (
    id BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    parent_id BIGINT,
    dept_code VARCHAR(64) NOT NULL,
    dept_name VARCHAR(128) NOT NULL,
    sort_order INTEGER NOT NULL DEFAULT 0,
    level_no INTEGER NOT NULL DEFAULT 1,
    path_ids TEXT,
    enabled BOOLEAN NOT NULL DEFAULT TRUE,
    deleted BOOLEAN NOT NULL DEFAULT FALSE,
    create_time TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_time TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uk_sys_dept_code UNIQUE (dept_code),
    CONSTRAINT fk_sys_dept_parent_id
        FOREIGN KEY (parent_id)
        REFERENCES app.sys_dept (id)
);

COMMENT ON TABLE app.sys_dept IS '部门表';
COMMENT ON COLUMN app.sys_dept.parent_id IS '父部门ID';
COMMENT ON COLUMN app.sys_dept.path_ids IS '祖先路径ID，例如 /1/2/3/';
```

常用索引：

```sql
CREATE INDEX idx_sys_dept_parent_id
ON app.sys_dept (parent_id);

CREATE INDEX idx_sys_dept_path_ids
ON app.sys_dept (path_ids);

CREATE INDEX idx_sys_dept_sort
ON app.sys_dept (parent_id, sort_order ASC, id ASC);
```

查询直接子节点：

```sql
SELECT
    id,
    parent_id,
    dept_code,
    dept_name,
    sort_order
FROM app.sys_dept
WHERE parent_id = 1
  AND deleted = FALSE
ORDER BY sort_order ASC, id ASC;
```

递归查询整棵子树：

```sql
WITH RECURSIVE dept_tree AS (
    SELECT
        id,
        parent_id,
        dept_code,
        dept_name,
        sort_order,
        1 AS depth
    FROM app.sys_dept
    WHERE id = 1
      AND deleted = FALSE

    UNION ALL

    SELECT
        d.id,
        d.parent_id,
        d.dept_code,
        d.dept_name,
        d.sort_order,
        t.depth + 1 AS depth
    FROM app.sys_dept d
    INNER JOIN dept_tree t ON d.parent_id = t.id
    WHERE d.deleted = FALSE
)
SELECT
    id,
    parent_id,
    dept_code,
    dept_name,
    depth
FROM dept_tree
ORDER BY depth ASC, sort_order ASC, id ASC;
```

查询祖先路径可以通过 `path_ids` 加速：

```sql
SELECT
    id,
    dept_code,
    dept_name
FROM app.sys_dept
WHERE path_ids LIKE '/1/2/%'
  AND deleted = FALSE
ORDER BY level_no ASC, sort_order ASC;
```

树形结构设计建议如下：

| 建议               | 说明                   |
| ------------------ | ---------------------- |
| 普通层级使用邻接表 | 简单、通用             |
| 增加 `sort_order`  | 支持同级排序           |
| 增加 `level_no`    | 便于展示和限制层级     |
| 可冗余 `path_ids`  | 提升查询子树和祖先效率 |
| 移动节点要维护路径 | 子节点路径也要同步更新 |
| 防止循环引用       | 应用层或数据库函数校验 |
| 大型树考虑闭包表   | 复杂祖先后代查询更高效 |

### 标签结构设计

标签结构用于给文章、商品、用户、订单等对象打标签。常见设计是标签表加对象标签关系表。标签本身独立维护，对象与标签多对多关联。

标签表：

```sql
CREATE TABLE app.tag_info (
    id BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    tag_code VARCHAR(64) NOT NULL,
    tag_name VARCHAR(128) NOT NULL,
    tag_type VARCHAR(64) NOT NULL DEFAULT 'default',
    color VARCHAR(32),
    enabled BOOLEAN NOT NULL DEFAULT TRUE,
    deleted BOOLEAN NOT NULL DEFAULT FALSE,
    create_time TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_time TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uk_tag_info_code UNIQUE (tag_code)
);

COMMENT ON TABLE app.tag_info IS '标签表';
COMMENT ON COLUMN app.tag_info.tag_type IS '标签类型';
```

对象标签关系表：

```sql
CREATE TABLE app.object_tag_relation (
    id BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    object_type VARCHAR(64) NOT NULL,
    object_id BIGINT NOT NULL,
    tag_id BIGINT NOT NULL,
    create_time TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uk_object_tag_relation UNIQUE (object_type, object_id, tag_id),
    CONSTRAINT fk_object_tag_relation_tag_id
        FOREIGN KEY (tag_id)
        REFERENCES app.tag_info (id)
);

COMMENT ON TABLE app.object_tag_relation IS '对象标签关系表';
COMMENT ON COLUMN app.object_tag_relation.object_type IS '对象类型，例如 article、product、user';
COMMENT ON COLUMN app.object_tag_relation.object_id IS '对象ID';
```

常用索引：

```sql
CREATE INDEX idx_object_tag_relation_object
ON app.object_tag_relation (object_type, object_id);

CREATE INDEX idx_object_tag_relation_tag
ON app.object_tag_relation (tag_id, object_type);
```

查询对象标签：

```sql
SELECT
    t.id,
    t.tag_code,
    t.tag_name,
    t.color
FROM app.object_tag_relation r
INNER JOIN app.tag_info t ON t.id = r.tag_id
WHERE r.object_type = 'article'
  AND r.object_id = 10001
  AND t.enabled = TRUE
  AND t.deleted = FALSE
ORDER BY t.tag_name ASC;
```

查询拥有某个标签的对象：

```sql
SELECT
    r.object_id
FROM app.object_tag_relation r
INNER JOIN app.tag_info t ON t.id = r.tag_id
WHERE r.object_type = 'article'
  AND t.tag_code = 'postgresql';
```

为对象批量设置标签时，可以先删除旧关系再插入新关系：

```sql
BEGIN;

DELETE FROM app.object_tag_relation
WHERE object_type = 'article'
  AND object_id = 10001;

INSERT INTO app.object_tag_relation (
    object_type,
    object_id,
    tag_id
) VALUES
    ('article', 10001, 1),
    ('article', 10001, 2),
    ('article', 10001, 3)
ON CONFLICT (object_type, object_id, tag_id) DO NOTHING;

COMMIT;
```

标签结构设计建议如下：

| 建议                          | 说明                             |
| ----------------------------- | -------------------------------- |
| 标签和对象关系分表            | 支持多对多                       |
| 关系表增加唯一约束            | 防止重复打标签                   |
| 使用 `object_type` 支持多对象 | 文章、商品、用户可共用标签体系   |
| 高频查询建双向索引            | 按对象查标签、按标签查对象       |
| 标签名可变，编码稳定          | 业务逻辑使用 `tag_code`          |
| 标签数量过多要分页            | 不要一次加载所有对象标签         |
| 标签统计可异步维护            | 热门标签、标签数量可以缓存或汇总 |

## 常见问题排查

常见问题排查用于快速定位连接失败、权限不足、编码问题、时区问题、锁等待、死锁、慢查询、索引未命中、表膨胀和数据不一致等问题。排查时应先确认现象，再收集证据，最后定位根因，不要直接凭经验修改配置或加索引。

### 连接失败

连接失败通常发生在应用启动、客户端连接、迁移脚本执行或服务重启后。常见原因包括地址端口错误、数据库未启动、账号密码错误、`pg_hba.conf` 未放行、网络不通、SSL 配置不一致、连接数耗尽等。

检查数据库是否监听端口：

```bash
ss -lntp | grep 5432
```

测试连接：

```bash
psql \
  -h 127.0.0.1 \
  -p 5432 \
  -U app_user \
  -d app_db
```

查看数据库连接配置：

```sql
SHOW listen_addresses;
SHOW port;
SHOW max_connections;
```

查看当前连接数：

```sql
SELECT
    COUNT(*) AS connection_count
FROM pg_stat_activity;
```

检查 `pg_hba.conf`：

```conf
# 示例：允许应用服务器连接 app_db
host    app_db    app_user    10.0.0.0/24    scram-sha-256
```

修改后重载：

```sql
SELECT pg_reload_conf();
```

常见连接错误和处理方式：

| 错误现象                       | 可能原因               | 处理方式                         |
| ------------------------------ | ---------------------- | -------------------------------- |
| connection refused             | 数据库未启动或端口不对 | 检查服务和端口                   |
| no pg_hba.conf entry           | `pg_hba.conf` 未放行   | 添加来源 IP 和认证规则           |
| password authentication failed | 密码错误               | 重置密码或检查配置               |
| database does not exist        | 数据库名错误           | 创建数据库或修改连接 URL         |
| role does not exist            | 用户不存在             | 创建角色                         |
| too many connections           | 连接耗尽               | 排查连接池和连接泄漏             |
| SSL error                      | SSL 模式不匹配         | 检查 `sslmode` 和服务端 SSL 配置 |

### 权限不足

权限不足通常表现为无法连接数据库、无法访问 Schema、无法查询表、无法执行函数、无法使用序列、无法创建对象等。PostgreSQL 权限分层明显，只有表权限不够，通常还需要 Schema 的 `USAGE` 权限。

常见错误：

```text
ERROR: permission denied for schema app
ERROR: permission denied for table sys_user
ERROR: permission denied for sequence sys_user_id_seq
ERROR: permission denied for function app.some_function
```

查看当前用户：

```sql
SELECT current_user, session_user;
```

授予 Schema 使用权限：

```sql
GRANT USAGE ON SCHEMA app TO app_rw;
```

授予表权限：

```sql
GRANT SELECT, INSERT, UPDATE, DELETE
ON ALL TABLES IN SCHEMA app
TO app_rw;
```

授予序列权限：

```sql
GRANT USAGE, SELECT
ON ALL SEQUENCES IN SCHEMA app
TO app_rw;
```

授予函数执行权限：

```sql
GRANT EXECUTE
ON ALL FUNCTIONS IN SCHEMA app
TO app_rw;
```

设置未来对象默认权限：

```sql
ALTER DEFAULT PRIVILEGES FOR ROLE app_owner IN SCHEMA app
GRANT SELECT, INSERT, UPDATE, DELETE ON TABLES TO app_rw;

ALTER DEFAULT PRIVILEGES FOR ROLE app_owner IN SCHEMA app
GRANT USAGE, SELECT ON SEQUENCES TO app_rw;

ALTER DEFAULT PRIVILEGES FOR ROLE app_owner IN SCHEMA app
GRANT EXECUTE ON FUNCTIONS TO app_rw;
```

查看表权限：

```sql
SELECT
    table_schema,
    table_name,
    privilege_type,
    grantee
FROM information_schema.role_table_grants
WHERE table_schema = 'app'
ORDER BY table_name, grantee, privilege_type;
```

权限不足排查建议如下：

| 检查项           | 说明                                          |
| ---------------- | --------------------------------------------- |
| 数据库 `CONNECT` | 是否能连接数据库                              |
| Schema `USAGE`   | 是否能访问 Schema                             |
| 表权限           | 是否有 `SELECT`、`INSERT`、`UPDATE`、`DELETE` |
| 序列权限         | 自增主键插入是否需要序列权限                  |
| 函数权限         | 是否有 `EXECUTE`                              |
| 默认权限         | 新表是否自动授权                              |
| RLS 策略         | 是否被行级安全策略过滤                        |

### 编码问题

编码问题常见于 CSV 导入、客户端显示乱码、应用写入异常、中文查询异常等。PostgreSQL 推荐使用 UTF-8 编码。

查看数据库编码：

```sql
SHOW server_encoding;
SHOW client_encoding;
```

查看数据库列表和编码：

```sql
SELECT
    datname,
    pg_encoding_to_char(encoding) AS encoding,
    datcollate,
    datctype
FROM pg_database
ORDER BY datname;
```

设置当前会话客户端编码：

```sql
SET client_encoding = 'UTF8';
```

创建数据库时指定编码：

```sql
CREATE DATABASE app_db
WITH
    OWNER = app_owner
    ENCODING = 'UTF8'
    TEMPLATE = template0
    LC_COLLATE = 'C'
    LC_CTYPE = 'C';
```

CSV 导入指定编码：

```sql
COPY app.import_user (
    username,
    nickname
)
FROM '/data/import/user.csv'
WITH (
    FORMAT csv,
    HEADER true,
    ENCODING 'UTF8'
);
```

编码问题排查建议如下：

| 问题           | 处理方式                               |
| -------------- | -------------------------------------- |
| 中文乱码       | 检查源文件、客户端、数据库编码是否一致 |
| CSV 导入失败   | 确认文件为 UTF-8                       |
| 客户端显示异常 | 检查终端编码和 `client_encoding`       |
| 排序规则异常   | 检查 `LC_COLLATE`                      |
| 跨系统导入     | 先转换编码再导入                       |
| Excel 导出乱码 | 可使用 UTF-8 with BOM 或让前端处理     |

Linux 转换文件编码示例：

```bash
iconv -f GBK -t UTF-8 source.csv > source_utf8.csv
```

### 时区问题

时区问题常见于应用时间和数据库时间相差 8 小时、导出时间不一致、跨地区用户时间展示异常、`timestamp` 和 `timestamptz` 混用等。

查看时区：

```sql
SHOW timezone;
```

查看当前时间：

```sql
SELECT
    CURRENT_TIMESTAMP AS current_timestamp_value,
    now() AS now_value,
    clock_timestamp() AS clock_timestamp_value,
    CURRENT_DATE AS current_date_value;
```

设置当前会话时区：

```sql
SET TIME ZONE 'Asia/Shanghai';
```

数据库配置时区：

文件位置：`postgresql.conf`

```conf
timezone = 'Asia/Shanghai'
log_timezone = 'Asia/Shanghai'
```

字段类型选择建议：

| 类型                                       | 说明                       | 建议               |
| ------------------------------------------ | -------------------------- | ------------------ |
| `timestamp without time zone`              | 不带时区，只保存日期时间值 | 国内单时区业务常用 |
| `timestamp with time zone` / `timestamptz` | 按时区规则转换显示         | 多时区系统更适合   |
| `date`                                     | 日期                       | 生日、业务日期     |
| `time`                                     | 时间                       | 每日固定时间       |

查看字段类型：

```sql
SELECT
    table_schema,
    table_name,
    column_name,
    data_type
FROM information_schema.columns
WHERE table_schema = 'app'
  AND column_name LIKE '%time%'
ORDER BY table_name, ordinal_position;
```

时区问题排查建议如下：

| 检查项            | 说明                           |
| ----------------- | ------------------------------ |
| 数据库 `timezone` | 服务端时区                     |
| JVM 时区          | Java 应用运行时区              |
| JDBC 参数         | 是否指定时区相关参数           |
| 字段类型          | `timestamp` 还是 `timestamptz` |
| 前端展示          | 是否做了本地时区转换           |
| 日志时间          | 数据库日志和应用日志是否统一   |

项目建议如下：

| 建议                               | 说明                        |
| ---------------------------------- | --------------------------- |
| 单地区系统统一使用 `Asia/Shanghai` | 数据库、JVM、服务器保持一致 |
| 多地区系统优先 UTC 存储            | 前端按用户时区展示          |
| 字段类型统一                       | 不同表不要混用时间语义      |
| 接口使用 ISO-8601                  | 减少歧义                    |
| 避免字符串存时间                   | 使用时间类型                |

### 锁等待

锁等待表现为 SQL 长时间不返回、接口超时、批处理卡住、连接池耗尽等。常见原因包括长事务、批量更新、DDL 加强锁、无索引更新、事务未提交等。

查看锁等待会话：

```sql
SELECT
    pid,
    usename,
    datname,
    application_name,
    client_addr,
    wait_event_type,
    wait_event,
    now() - query_start AS query_duration,
    now() - xact_start AS transaction_duration,
    query
FROM pg_stat_activity
WHERE wait_event_type = 'Lock'
ORDER BY query_duration DESC;
```

查看阻塞关系：

```sql
SELECT
    blocked.pid AS blocked_pid,
    blocked.query AS blocked_query,
    now() - blocked.query_start AS blocked_duration,
    blocking.pid AS blocking_pid,
    blocking.query AS blocking_query,
    now() - blocking.query_start AS blocking_duration
FROM pg_stat_activity blocked
JOIN pg_locks blocked_locks
    ON blocked_locks.pid = blocked.pid
JOIN pg_locks blocking_locks
    ON blocking_locks.locktype = blocked_locks.locktype
   AND blocking_locks.database IS NOT DISTINCT FROM blocked_locks.database
   AND blocking_locks.relation IS NOT DISTINCT FROM blocked_locks.relation
   AND blocking_locks.page IS NOT DISTINCT FROM blocked_locks.page
   AND blocking_locks.tuple IS NOT DISTINCT FROM blocked_locks.tuple
   AND blocking_locks.virtualxid IS NOT DISTINCT FROM blocked_locks.virtualxid
   AND blocking_locks.transactionid IS NOT DISTINCT FROM blocked_locks.transactionid
   AND blocking_locks.classid IS NOT DISTINCT FROM blocked_locks.classid
   AND blocking_locks.objid IS NOT DISTINCT FROM blocked_locks.objid
   AND blocking_locks.objsubid IS NOT DISTINCT FROM blocked_locks.objsubid
   AND blocking_locks.pid <> blocked_locks.pid
JOIN pg_stat_activity blocking
    ON blocking.pid = blocking_locks.pid
WHERE NOT blocked_locks.granted
  AND blocking_locks.granted
ORDER BY blocked_duration DESC;
```

设置锁等待超时：

```sql
SET lock_timeout = '5s';
```

锁等待处理建议如下：

| 场景                       | 处理方式                   |
| -------------------------- | -------------------------- |
| 普通等待                   | 找阻塞源，确认是否正常事务 |
| `idle in transaction` 阻塞 | 通常可以考虑终止异常会话   |
| DDL 阻塞                   | 低峰执行，缩短事务         |
| 批量更新阻塞               | 分批处理                   |
| 无索引更新                 | 补充合适索引               |
| 高频冲突                   | 固定加锁顺序，缩短事务     |

终止阻塞会话前必须确认业务影响：

```sql
SELECT pg_terminate_backend(12345);
```

### 死锁

死锁是多个事务互相等待对方持有的锁，导致无法继续执行。PostgreSQL 会自动检测死锁，并中止其中一个事务。应用应捕获死锁异常并有限重试。

典型死锁原因是加锁顺序不一致：

```text
事务 A：先更新 user_id = 1，再更新 user_id = 2
事务 B：先更新 user_id = 2，再更新 user_id = 1
```

推荐固定加锁顺序：

```sql
BEGIN;

SELECT
    user_id
FROM app.account_balance
WHERE user_id IN (1, 2)
ORDER BY user_id
FOR UPDATE;

UPDATE app.account_balance
SET balance = balance - 100
WHERE user_id = 1
  AND balance >= 100;

UPDATE app.account_balance
SET balance = balance + 100
WHERE user_id = 2;

COMMIT;
```

查看死锁统计：

```sql
SELECT
    datname,
    deadlocks
FROM pg_stat_database
ORDER BY deadlocks DESC;
```

开启锁等待日志：

```conf
log_lock_waits = on
deadlock_timeout = '1s'
```

死锁处理建议如下：

| 建议             | 说明                       |
| ---------------- | -------------------------- |
| 固定加锁顺序     | 多行、多表更新时尤其重要   |
| 缩短事务时间     | 不在事务内做远程调用       |
| 批量操作分批提交 | 降低锁范围                 |
| 更新条件命中索引 | 避免锁范围扩大             |
| 捕获死锁异常重试 | 应用层有限次数重试         |
| 排查死锁日志     | 根据日志还原两个事务的 SQL |

### 慢查询

慢查询可能由全表扫描、索引缺失、索引失效、排序落盘、连接方式不合理、统计信息过旧、锁等待、返回数据量过大等原因造成。

使用 `EXPLAIN ANALYZE` 查看真实执行计划：

```sql
EXPLAIN (
    ANALYZE TRUE,
    BUFFERS TRUE,
    VERBOSE TRUE,
    SUMMARY TRUE
)
SELECT
    id,
    order_no,
    user_id,
    total_amount,
    create_time
FROM app.order_info
WHERE user_id = 10001
ORDER BY create_time DESC, id DESC
LIMIT 20;
```

查看正在运行的慢 SQL：

```sql
SELECT
    pid,
    usename,
    datname,
    application_name,
    now() - query_start AS query_duration,
    wait_event_type,
    wait_event,
    query
FROM pg_stat_activity
WHERE state = 'active'
  AND now() - query_start > INTERVAL '10 seconds'
ORDER BY query_duration DESC;
```

使用 `pg_stat_statements` 查看累计耗时：

```sql
SELECT
    query,
    calls,
    total_exec_time,
    mean_exec_time,
    max_exec_time,
    rows
FROM pg_stat_statements
ORDER BY total_exec_time DESC
LIMIT 20;
```

慢查询排查步骤：

| 步骤         | 说明                                             |
| ------------ | ------------------------------------------------ |
| 找 SQL       | 慢日志、`pg_stat_activity`、`pg_stat_statements` |
| 看执行计划   | `EXPLAIN ANALYZE`                                |
| 看扫描方式   | 是否 `Seq Scan`、`Index Scan`                    |
| 看行数估算   | 估算和实际是否差距大                             |
| 看排序和聚合 | 是否大排序、大聚合                               |
| 看 Buffers   | 是否大量磁盘读取                                 |
| 看锁等待     | 慢是否因为等待锁                                 |
| 优化并验证   | 调整 SQL、索引或统计信息                         |

慢查询优化建议如下：

| 问题           | 处理方式                   |
| -------------- | -------------------------- |
| 大表全表扫描   | 增加合适索引               |
| 排序慢         | 建立匹配排序的联合索引     |
| 返回数据过多   | 分页、减少字段             |
| JOIN 慢        | 检查连接字段索引和过滤顺序 |
| 统计信息旧     | 执行 `ANALYZE`             |
| 深分页慢       | 使用游标分页或基于 ID 翻页 |
| 函数包裹索引列 | 改为范围条件或表达式索引   |

### 索引未命中

索引未命中表现为明明创建了索引，但执行计划仍然使用顺序扫描。原因可能是返回数据比例太高、条件写法不匹配、隐式类型转换、函数包裹字段、统计信息不准、联合索引顺序不合适等。

查看执行计划：

```sql
EXPLAIN ANALYZE
SELECT
    id,
    order_no,
    create_time
FROM app.order_info
WHERE create_time::date = DATE '2026-05-09';
```

不推荐写法：

```sql
SELECT
    id,
    order_no
FROM app.order_info
WHERE create_time::date = DATE '2026-05-09';
```

推荐改为范围查询：

```sql
SELECT
    id,
    order_no
FROM app.order_info
WHERE create_time >= TIMESTAMP '2026-05-09 00:00:00'
  AND create_time < TIMESTAMP '2026-05-10 00:00:00';
```

常见索引失效或未命中原因：

| 原因               | 示例                   | 处理方式                      |
| ------------------ | ---------------------- | ----------------------------- |
| 函数包裹字段       | `date(create_time)`    | 改范围查询或建表达式索引      |
| 前置模糊匹配       | `LIKE '%abc'`          | 使用 `pg_trgm` 或全文搜索     |
| 隐式类型转换       | 字符串和数字混比       | 参数类型保持一致              |
| 返回比例过高       | 查询大部分数据         | 顺序扫描可能更合理            |
| 联合索引顺序不匹配 | 索引 `(a,b)`，只查 `b` | 调整索引或查询条件            |
| 统计信息过旧       | 行数估算错误           | 执行 `ANALYZE`                |
| OR 条件复杂        | 多字段 OR              | 拆分 `UNION ALL` 或建合适索引 |

表达式索引示例：

```sql
CREATE INDEX idx_order_info_create_date
ON app.order_info ((create_time::date));
```

`pg_trgm` 模糊搜索索引：

```sql
CREATE EXTENSION IF NOT EXISTS pg_trgm;

CREATE INDEX idx_sys_user_nickname_trgm
ON app.sys_user
USING GIN (nickname gin_trgm_ops);
```

索引未命中排查建议：

| 步骤             | 说明                           |
| ---------------- | ------------------------------ |
| 查看执行计划     | 确认是否真的未使用索引         |
| 检查查询条件     | 是否和索引字段匹配             |
| 检查字段类型     | 参数类型是否一致               |
| 检查数据选择性   | 返回比例高时不用索引正常       |
| 检查统计信息     | 执行 `ANALYZE`                 |
| 检查联合索引顺序 | 等值、范围、排序字段顺序要合理 |
| 验证优化效果     | 修改后重新 `EXPLAIN ANALYZE`   |

### 表膨胀

表膨胀通常由大量更新、删除、长事务阻止 VACUUM、autovacuum 不及时引起。表现为表文件很大、查询变慢、缓存命中率下降、磁盘持续增长。

查看死元组：

```sql
SELECT
    schemaname,
    relname AS table_name,
    n_live_tup,
    n_dead_tup,
    ROUND(
        n_dead_tup::NUMERIC / NULLIF(n_live_tup + n_dead_tup, 0) * 100,
        2
    ) AS dead_tuple_ratio_percent,
    last_vacuum,
    last_autovacuum
FROM pg_stat_user_tables
ORDER BY n_dead_tup DESC;
```

查看表大小：

```sql
SELECT
    schemaname,
    relname AS table_name,
    pg_size_pretty(pg_relation_size(relid)) AS table_size,
    pg_size_pretty(pg_indexes_size(relid)) AS indexes_size,
    pg_size_pretty(pg_total_relation_size(relid)) AS total_size
FROM pg_stat_user_tables
ORDER BY pg_total_relation_size(relid) DESC;
```

普通清理：

```sql
VACUUM ANALYZE app.order_info;
```

严重膨胀并且需要回收磁盘空间：

```sql
VACUUM FULL app.order_info;
```

表膨胀排查建议如下：

| 检查项           | 说明                     |
| ---------------- | ------------------------ |
| 死元组数量       | `n_dead_tup` 是否很高    |
| 最近 autovacuum  | 是否长期未执行           |
| 长事务           | 是否阻止清理             |
| 表更新频率       | 高频更新容易膨胀         |
| 索引大小         | 索引也会膨胀             |
| 历史数据清理方式 | 大量 DELETE 不如分区删除 |

处理建议如下：

| 场景         | 处理方式               |
| ------------ | ---------------------- |
| 普通死元组多 | `VACUUM ANALYZE`       |
| 统计信息旧   | `ANALYZE`              |
| 磁盘急需回收 | 低峰评估 `VACUUM FULL` |
| 高频更新表   | 调整 autovacuum 参数   |
| 历史日志表   | 使用分区和归档         |
| 长事务阻塞   | 先终止或修复长事务来源 |

### 数据不一致

数据不一致可能来自并发更新、事务边界错误、应用逻辑重复执行、缺少唯一约束、外键缺失、读写分离延迟、缓存未刷新、手工改库、迁移脚本问题等。排查时应先明确“不一致”的定义，再对比数据来源和时间线。

常见类型如下：

| 类型                 | 示例                     |
| -------------------- | ------------------------ |
| 主表和明细金额不一致 | 订单总额不等于明细合计   |
| 状态不一致           | 支付成功但订单仍是待支付 |
| 数量不一致           | 库存扣减和订单数量不一致 |
| 缓存和数据库不一致   | 页面显示旧数据           |
| 主从数据不一致       | 读从库有延迟             |
| 重复数据             | 缺唯一约束或幂等控制     |
| 孤儿数据             | 明细表找不到主表         |

检查订单金额一致性：

```sql
SELECT
    o.id,
    o.order_no,
    o.total_amount,
    COALESCE(SUM(i.item_amount), 0) AS item_total_amount
FROM app.order_info o
LEFT JOIN app.order_item i ON i.order_id = o.id
GROUP BY
    o.id,
    o.order_no,
    o.total_amount
HAVING o.total_amount <> COALESCE(SUM(i.item_amount), 0);
```

检查重复订单号：

```sql
SELECT
    order_no,
    COUNT(*) AS repeat_count
FROM app.order_info
GROUP BY order_no
HAVING COUNT(*) > 1;
```

检查孤儿明细：

```sql
SELECT
    i.id,
    i.order_id,
    i.product_name
FROM app.order_item i
LEFT JOIN app.order_info o ON o.id = i.order_id
WHERE o.id IS NULL;
```

检查主从延迟：

```sql
SELECT
    pg_last_wal_receive_lsn() AS receive_lsn,
    pg_last_wal_replay_lsn() AS replay_lsn,
    now() - pg_last_xact_replay_timestamp() AS replay_delay;
```

数据修复前备份影响范围：

```sql
CREATE TABLE app.order_info_fix_backup_20260509 AS
SELECT *
FROM app.order_info
WHERE id IN (
    SELECT
        o.id
    FROM app.order_info o
    LEFT JOIN app.order_item i ON i.order_id = o.id
    GROUP BY o.id
    HAVING o.total_amount <> COALESCE(SUM(i.item_amount), 0)
);
```

修复订单金额：

```sql
UPDATE app.order_info o
SET
    total_amount = s.item_total_amount,
    update_time = CURRENT_TIMESTAMP
FROM (
    SELECT
        order_id,
        COALESCE(SUM(item_amount), 0) AS item_total_amount
    FROM app.order_item
    GROUP BY order_id
) s
WHERE o.id = s.order_id
  AND o.total_amount <> s.item_total_amount;
```

数据不一致排查流程：

| 步骤       | 说明                           |
| ---------- | ------------------------------ |
| 明确定义   | 哪些字段、哪些表不一致         |
| 定位范围   | 影响多少条、从什么时候开始     |
| 查变更来源 | 应用日志、操作日志、数据库日志 |
| 查约束缺失 | 是否缺唯一约束、外键、检查约束 |
| 查并发问题 | 是否缺条件更新、乐观锁、幂等   |
| 查事务问题 | 是否部分提交、异常未回滚       |
| 查读写分离 | 是否从库延迟导致读旧数据       |
| 备份后修复 | 先备份影响范围，再事务内修复   |
| 增加防护   | 补约束、补索引、补幂等、补监控 |

数据一致性设计建议如下：

| 建议                   | 说明                         |
| ---------------------- | ---------------------------- |
| 关键唯一性用数据库约束 | 不只依赖应用判断             |
| 状态流转使用条件更新   | `WHERE status = 原状态`      |
| 金额和数量使用事务处理 | 主表、明细、库存同事务       |
| 支付回调必须幂等       | 根据业务流水号唯一约束控制   |
| 读写分离下写后读走主库 | 避免从库延迟                 |
| 缓存更新要有策略       | 删除缓存、延迟双删或消息同步 |
| 修复脚本必须可回滚     | 先备份，再修复，再校验       |

## 开发规范

开发规范用于统一 PostgreSQL 在项目中的表结构、字段、索引、SQL、事务、权限、脚本、性能和安全要求。规范的目标不是限制开发，而是降低协作成本、减少线上事故、提升可维护性。

### 命名规范

数据库对象命名应统一使用小写字母、数字和下划线，避免大小写混用。PostgreSQL 中未加双引号的对象名会自动转为小写，因此不建议创建带大写字母或特殊字符的对象名。

推荐命名方式：

| 对象     | 推荐格式                        | 示例                         |
| -------- | ------------------------------- | ---------------------------- |
| Schema   | 小写业务名                      | `app`、`report`、`archive`   |
| 表名     | 小写下划线                      | `sys_user`、`order_info`     |
| 字段名   | 小写下划线                      | `user_id`、`create_time`     |
| 主键     | `pk_表名`                       | `pk_order_info`              |
| 唯一约束 | `uk_表名_字段`                  | `uk_sys_user_username`       |
| 普通索引 | `idx_表名_字段`                 | `idx_order_info_user_time`   |
| 外键     | `fk_表名_字段`                  | `fk_order_item_order_id`     |
| 检查约束 | `ck_表名_字段`                  | `ck_order_info_amount`       |
| 视图     | `v_业务名`                      | `v_order_summary`            |
| 物化视图 | `mv_业务名`                     | `mv_daily_order_stat`        |
| 函数     | 动词或业务动作                  | `get_user_status_text`       |
| 触发器   | `trg_表名_动作_用途`            | `trg_order_info_update_time` |
| 序列     | `seq_业务名` 或 `表名_字段_seq` | `seq_order_no`               |

推荐写法：

```sql
CREATE TABLE app.order_info (
    id BIGINT GENERATED ALWAYS AS IDENTITY,
    order_no VARCHAR(64) NOT NULL,
    user_id BIGINT NOT NULL,
    order_status VARCHAR(32) NOT NULL,
    total_amount NUMERIC(12, 2) NOT NULL DEFAULT 0,
    create_time TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_time TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT pk_order_info PRIMARY KEY (id),
    CONSTRAINT uk_order_info_order_no UNIQUE (order_no),
    CONSTRAINT ck_order_info_total_amount CHECK (total_amount >= 0)
);
```

不推荐写法：

```sql
-- 不推荐：混用大小写，需要双引号访问
CREATE TABLE app."OrderInfo" (
    "ID" BIGINT PRIMARY KEY,
    "OrderNo" VARCHAR(64)
);
```

命名规范建议如下：

| 建议                     | 说明                                           |
| ------------------------ | ---------------------------------------------- |
| 统一小写下划线           | 避免双引号对象名                               |
| 表名表达业务含义         | 不使用 `table1`、`data_info` 这类模糊名称      |
| 字段名避免缩写过度       | `create_time` 优于 `ct`                        |
| 约束和索引必须显式命名   | 便于排查、删除、迁移                           |
| 布尔字段使用语义明确名称 | 如 `enabled`、`deleted`、`locked`              |
| 时间字段统一命名         | 如 `create_time`、`update_time`、`delete_time` |
| 金额字段明确单位和语义   | 如 `total_amount`、`pay_amount`                |

### 字段类型规范

字段类型应优先选择语义明确、范围合适、便于索引和比较的类型。不要为了省事全部使用 `TEXT`，也不要用字符串存储数字、时间、布尔值。

常用字段类型建议：

| 场景       | 推荐类型             | 说明                      |
| ---------- | -------------------- | ------------------------- |
| 主键 ID    | `BIGINT`             | 配合 `IDENTITY` 或雪花 ID |
| 业务编码   | `VARCHAR(64)`        | 如订单号、用户编码        |
| 名称       | `VARCHAR(128)`       | 用户名、商品名、角色名    |
| 描述       | `TEXT`               | 长文本描述                |
| 金额       | `NUMERIC(12, 2)`     | 不使用浮点数              |
| 数量       | `INTEGER` / `BIGINT` | 根据范围选择              |
| 状态       | `VARCHAR(32)`        | 使用稳定英文编码          |
| 布尔值     | `BOOLEAN`            | 不使用 `0/1` 字符串       |
| 创建时间   | `TIMESTAMP`          | 单时区系统常用            |
| 多时区时间 | `TIMESTAMPTZ`        | 全球化系统使用            |
| JSON 数据  | `JSONB`              | 优先 `JSONB`              |
| UUID       | `UUID`               | 外部不可猜 ID             |
| IP 地址    | `INET`               | 存储 IPv4 / IPv6          |
| 标签数组   | `TEXT[]`             | 简单数组场景              |

示例：

```sql
CREATE TABLE app.field_type_demo (
    id BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    business_code VARCHAR(64) NOT NULL,
    business_name VARCHAR(128) NOT NULL,
    description TEXT,
    total_amount NUMERIC(12, 2) NOT NULL DEFAULT 0,
    quantity INTEGER NOT NULL DEFAULT 0,
    enabled BOOLEAN NOT NULL DEFAULT TRUE,
    business_time TIMESTAMP NOT NULL,
    extra_data JSONB NOT NULL DEFAULT '{}'::jsonb,
    external_id UUID NOT NULL DEFAULT gen_random_uuid(),
    client_ip INET,
    tag_codes TEXT[] NOT NULL DEFAULT '{}',
    create_time TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);
```

字段类型规范建议如下：

| 建议                  | 说明                                       |
| --------------------- | ------------------------------------------ |
| 主键默认使用 `BIGINT` | 避免 `INTEGER` 后期范围不足                |
| 金额使用 `NUMERIC`    | 禁止使用 `REAL`、`DOUBLE PRECISION` 存金额 |
| 时间使用时间类型      | 不用字符串存时间                           |
| 状态使用稳定编码      | 如 `PENDING`、`PAID`                       |
| JSON 优先 `JSONB`     | 查询和索引能力更好                         |
| 布尔值使用 `BOOLEAN`  | 语义清晰                                   |
| 字段长度合理限制      | 业务编码、手机号、名称应限制长度           |
| 可枚举字段加检查约束  | 数据库层兜底                               |

检查约束示例：

```sql
ALTER TABLE app.order_info
ADD CONSTRAINT ck_order_info_status
CHECK (order_status IN ('PENDING', 'PAID', 'CANCELLED', 'FINISHED'));
```

### 主键规范

主键用于唯一标识一行数据，应稳定、无业务含义、不可复用。业务系统中推荐使用 `BIGINT GENERATED ALWAYS AS IDENTITY` 作为单库主键。如果是分布式系统，可以使用应用层生成的雪花 ID。

推荐主键：

```sql
CREATE TABLE app.sys_role (
    id BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    role_code VARCHAR(64) NOT NULL,
    role_name VARCHAR(128) NOT NULL,
    enabled BOOLEAN NOT NULL DEFAULT TRUE,
    create_time TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uk_sys_role_code UNIQUE (role_code)
);
```

分布式 ID 主键：

```sql
CREATE TABLE app.distributed_order (
    id BIGINT PRIMARY KEY,
    order_no VARCHAR(64) NOT NULL,
    user_id BIGINT NOT NULL,
    total_amount NUMERIC(12, 2) NOT NULL,
    create_time TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uk_distributed_order_no UNIQUE (order_no)
);
```

不推荐使用业务字段作为主键：

```sql
-- 不推荐：手机号可能变更，也可能存在隐私风险
CREATE TABLE app.bad_user (
    phone VARCHAR(20) PRIMARY KEY,
    username VARCHAR(64) NOT NULL
);
```

推荐改为：

```sql
CREATE TABLE app.good_user (
    id BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    phone VARCHAR(20),
    username VARCHAR(64) NOT NULL,
    CONSTRAINT uk_good_user_phone UNIQUE (phone)
);
```

主键规范建议如下：

| 建议                        | 说明                               |
| --------------------------- | ---------------------------------- |
| 默认使用 `id BIGINT`        | 统一主键类型                       |
| 主键无业务含义              | 不使用手机号、邮箱、订单号作为主键 |
| 业务唯一另加唯一约束        | 如 `UNIQUE(order_no)`              |
| 单表只保留一个主键          | 复合主键谨慎使用                   |
| 分布式场景提前规划          | 使用雪花 ID 或号段 ID              |
| 对外暴露避免直接使用自增 ID | 可使用 UUID 或业务编号             |
| 主键一旦生成不修改          | 保持引用稳定                       |

### 索引规范

索引用于提升查询性能，但会增加写入成本、存储成本和维护成本。索引应基于真实查询设计，不应无脑给所有字段建索引。

常见索引类型选择：

| 场景           | 推荐索引        |
| -------------- | --------------- |
| 等值查询       | B-Tree          |
| 范围查询       | B-Tree          |
| 排序分页       | 联合 B-Tree     |
| 唯一性约束     | Unique Index    |
| JSONB 包含查询 | GIN             |
| 数组包含查询   | GIN             |
| 模糊搜索       | `pg_trgm` + GIN |
| 空间查询       | GiST            |
| 大型时序表     | BRIN            |
| 条件过滤       | 部分索引        |
| 函数表达式查询 | 表达式索引      |

联合索引示例：

```sql
CREATE INDEX idx_order_info_user_time
ON app.order_info (user_id, create_time DESC, id DESC);
```

适配查询：

```sql
SELECT
    id,
    order_no,
    total_amount,
    create_time
FROM app.order_info
WHERE user_id = 10001
ORDER BY create_time DESC, id DESC
LIMIT 20;
```

部分索引示例：

```sql
CREATE INDEX idx_order_info_not_deleted_time
ON app.order_info (create_time DESC, id DESC)
WHERE deleted = FALSE;
```

表达式索引示例：

```sql
CREATE INDEX idx_sys_user_lower_username
ON app.sys_user (lower(username));
```

适配查询：

```sql
SELECT
    id,
    username,
    nickname
FROM app.sys_user
WHERE lower(username) = lower('Admin');
```

索引规范建议如下：

| 建议                         | 说明                                   |
| ---------------------------- | -------------------------------------- |
| 按查询设计索引               | 不按字段盲目建索引                     |
| 联合索引考虑等值、范围、排序 | 常见顺序是等值字段、范围字段、排序字段 |
| 大表建索引用 `CONCURRENTLY`  | 降低写入阻塞                           |
| 唯一性用唯一索引或唯一约束   | 不只依赖应用判断                       |
| 逻辑删除表可用部分索引       | 如 `WHERE deleted = FALSE`             |
| 模糊查询使用 `pg_trgm`       | 普通 B-Tree 不支持前置模糊             |
| 定期清理无用索引             | 结合 `pg_stat_user_indexes` 分析       |
| 删除索引前保留 DDL           | 便于回滚                               |

大表创建索引：

```sql
CREATE INDEX CONCURRENTLY IF NOT EXISTS idx_order_info_status_time
ON app.order_info (order_status, create_time DESC, id DESC);
```

删除索引：

```sql
DROP INDEX CONCURRENTLY IF EXISTS app.idx_order_info_status_time;
```

### SQL 编写规范

SQL 编写应做到字段明确、条件清晰、参数化、防注入、可使用索引、可读性强。不要使用不可控的动态 SQL，不要在大表上无条件更新或删除。

推荐查询写法：

```sql
SELECT
    id,
    order_no,
    user_id,
    order_status,
    total_amount,
    create_time
FROM app.order_info
WHERE user_id = 10001
  AND deleted = FALSE
ORDER BY create_time DESC, id DESC
LIMIT 20;
```

不推荐：

```sql
-- 不推荐：SELECT * 不利于字段控制和索引覆盖
SELECT *
FROM app.order_info;

-- 不推荐：没有 WHERE 条件的更新风险极高
UPDATE app.order_info
SET order_status = 'CANCELLED';
```

分页规范：

```sql
SELECT
    id,
    order_no,
    total_amount,
    create_time
FROM app.order_info
WHERE user_id = 10001
  AND create_time < TIMESTAMP '2026-05-09 12:00:00'
ORDER BY create_time DESC, id DESC
LIMIT 20;
```

深分页不推荐：

```sql
-- 不推荐：OFFSET 很大时性能差
SELECT
    id,
    order_no
FROM app.order_info
ORDER BY create_time DESC
LIMIT 20 OFFSET 100000;
```

更新状态时必须带前置状态：

```sql
UPDATE app.order_info
SET
    order_status = 'PAID',
    pay_time = CURRENT_TIMESTAMP,
    update_time = CURRENT_TIMESTAMP
WHERE id = 1
  AND order_status = 'PENDING';
```

SQL 编写建议如下：

| 建议                   | 说明                                    |
| ---------------------- | --------------------------------------- |
| 禁止 `SELECT *`        | 明确字段，减少传输和耦合                |
| 查询必须有明确条件     | 大表查询必须带可索引条件                |
| 分页必须有稳定排序     | 如 `ORDER BY create_time DESC, id DESC` |
| 大表避免深分页         | 使用游标分页或基于 ID 翻页              |
| 更新删除必须有 WHERE   | 生产执行前先 SELECT 影响范围            |
| 状态流转使用条件更新   | 防止并发重复处理                        |
| 不在索引列上随意套函数 | 可能导致索引失效                        |
| 动态排序使用白名单     | 防止 SQL 注入                           |
| 复杂 SQL 要看执行计划  | 使用 `EXPLAIN ANALYZE` 验证             |

### 事务使用规范

事务用于保证一组操作要么全部成功，要么全部失败。事务边界应尽量短，不要在事务中执行远程调用、文件处理、长时间计算或等待用户输入。

推荐事务：

```sql
BEGIN;

UPDATE app.order_info
SET
    order_status = 'PAID',
    pay_time = CURRENT_TIMESTAMP,
    update_time = CURRENT_TIMESTAMP
WHERE id = 1
  AND order_status = 'PENDING';

INSERT INTO app.order_status_log (
    order_id,
    order_no,
    from_status,
    to_status,
    operation_type
) VALUES (
    1,
    'PO202605090001',
    'PENDING',
    'PAID',
    'PAY_SUCCESS'
);

COMMIT;
```

异常时回滚：

```sql
ROLLBACK;
```

保存点示例：

```sql
BEGIN;

SAVEPOINT before_import;

INSERT INTO app.sys_config (
    config_key,
    config_value
) VALUES (
    'demo.key',
    'demo.value'
);

ROLLBACK TO SAVEPOINT before_import;

COMMIT;
```

事务规范建议如下：

| 建议                 | 说明                         |
| -------------------- | ---------------------------- |
| 事务尽量短           | 减少锁持有时间               |
| 事务内不调用外部接口 | 防止外部耗时导致锁等待       |
| 事务内不等待用户输入 | 避免长事务                   |
| 批量处理分批提交     | 避免超大事务                 |
| 多资源加锁顺序固定   | 降低死锁概率                 |
| 重要更新检查影响行数 | 判断并发冲突                 |
| 设置事务超时         | 防止异常事务长期存在         |
| 只读事务标记只读     | 应用层使用 `readOnly = true` |

Spring 事务建议：

```java
package io.github.ateng.postgresql.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 订单事务处理服务。
 *
 * @author Ateng
 * @since 2026-05-09
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class OrderTransactionService {

    @Transactional(rollbackFor = Exception.class)
    public void payOrder(Long orderId) {
        log.info("开始支付订单，订单ID：{}", orderId);
        // 只放数据库一致性相关操作
        // 不在事务中调用外部支付接口
    }
}
```

### 权限使用规范

权限应按角色管理，遵循最小权限原则。应用账号不应使用超级用户，不应拥有不必要的 DDL 权限。只读账号和读写账号必须分离。

推荐角色：

```sql
CREATE ROLE app_owner NOLOGIN;
CREATE ROLE app_rw NOLOGIN;
CREATE ROLE app_ro NOLOGIN;

CREATE ROLE app_user WITH LOGIN PASSWORD 'change_me_password';
CREATE ROLE report_user WITH LOGIN PASSWORD 'change_me_report_password';

GRANT app_rw TO app_user;
GRANT app_ro TO report_user;
```

授权：

```sql
GRANT CONNECT ON DATABASE app_db TO app_rw;
GRANT CONNECT ON DATABASE app_db TO app_ro;

GRANT USAGE ON SCHEMA app TO app_rw;
GRANT USAGE ON SCHEMA app TO app_ro;

GRANT SELECT, INSERT, UPDATE, DELETE
ON ALL TABLES IN SCHEMA app
TO app_rw;

GRANT SELECT
ON ALL TABLES IN SCHEMA app
TO app_ro;

GRANT USAGE, SELECT
ON ALL SEQUENCES IN SCHEMA app
TO app_rw;
```

默认权限：

```sql
ALTER DEFAULT PRIVILEGES FOR ROLE app_owner IN SCHEMA app
GRANT SELECT, INSERT, UPDATE, DELETE ON TABLES TO app_rw;

ALTER DEFAULT PRIVILEGES FOR ROLE app_owner IN SCHEMA app
GRANT SELECT ON TABLES TO app_ro;

ALTER DEFAULT PRIVILEGES FOR ROLE app_owner IN SCHEMA app
GRANT USAGE, SELECT ON SEQUENCES TO app_rw;
```

权限规范建议如下：

| 建议                   | 说明                     |
| ---------------------- | ------------------------ |
| 应用不使用超级用户     | 降低误操作影响           |
| 应用账号不做对象 Owner | Owner 权限过大           |
| 读写账号和只读账号分离 | 报表和导出走只读         |
| DDL 由迁移账号执行     | 应用运行账号不执行 DDL   |
| 权限通过角色授予       | 不直接给个人账号逐表授权 |
| 回收 public 默认权限   | 防止权限扩散             |
| 定期审计账号和权限     | 清理废弃账号、临时账号   |
| 敏感表单独授权         | 如用户认证表、密钥表     |

### 脚本管理规范

数据库脚本包括 DDL、DML、初始化脚本、变更脚本、回滚脚本和数据迁移脚本。脚本必须纳入版本库，并通过 Flyway、Liquibase 或标准发布流程执行。

推荐目录：

```text
src/main/resources/db
├── migration
│   ├── V1__create_schema.sql
│   ├── V2__create_tables.sql
│   ├── V3__create_indexes.sql
│   └── V4__init_data.sql
├── rollback
│   ├── R2__drop_tables.sql
│   └── R3__drop_indexes.sql
└── manual
    └── 20260509_fix_order_amount.sql
```

Flyway 命名：

```text
V1__create_schema.sql
V2__create_user_tables.sql
V3__create_order_tables.sql
V4__create_indexes.sql
V5__init_dict_data.sql
```

数据修复脚本模板：

```sql
BEGIN;

-- 1. 备份影响范围
CREATE TABLE app.fix_order_amount_backup_20260509 AS
SELECT *
FROM app.order_info
WHERE id IN (1, 2, 3);

-- 2. 修复数据
UPDATE app.order_info
SET
    total_amount = 100.00,
    update_time = CURRENT_TIMESTAMP
WHERE id = 1;

-- 3. 验证结果
SELECT
    id,
    order_no,
    total_amount
FROM app.order_info
WHERE id IN (1, 2, 3);

COMMIT;
```

脚本管理规范建议如下：

| 建议                   | 说明                              |
| ---------------------- | --------------------------------- |
| 所有脚本进入代码仓库   | 不手工改库                        |
| 已发布脚本不修改       | 新增版本脚本修复                  |
| 初始化数据幂等         | 使用 `ON CONFLICT`                |
| 高风险脚本提供回滚     | 发布前准备                        |
| 数据修复先备份影响范围 | 不直接覆盖生产数据                |
| 脚本执行记录归档       | 包含执行人、时间、结果            |
| 生产脚本必须评审       | 尤其是 `DROP`、`DELETE`、`UPDATE` |
| 大表变更单独评估       | 锁、耗时、回滚方案都要明确        |

### 性能优化规范

性能优化应基于证据，而不是凭经验盲目加索引或改参数。优化流程应包括发现问题、定位 SQL、分析执行计划、修改 SQL 或索引、验证效果和上线观察。

执行计划分析：

```sql
EXPLAIN (
    ANALYZE TRUE,
    BUFFERS TRUE,
    VERBOSE TRUE,
    SUMMARY TRUE
)
SELECT
    id,
    order_no,
    total_amount,
    create_time
FROM app.order_info
WHERE user_id = 10001
ORDER BY create_time DESC, id DESC
LIMIT 20;
```

常用慢 SQL 统计：

```sql
SELECT
    query,
    calls,
    total_exec_time,
    mean_exec_time,
    max_exec_time,
    rows
FROM pg_stat_statements
ORDER BY total_exec_time DESC
LIMIT 20;
```

性能优化规范建议如下：

| 建议                   | 说明                       |
| ---------------------- | -------------------------- |
| 优先定位 SQL           | 不先改数据库参数           |
| 使用 `EXPLAIN ANALYZE` | 看真实执行情况             |
| 大表查询必须可走索引   | 条件和排序要匹配索引       |
| 避免深分页             | 使用基于游标或 ID 的分页   |
| 避免返回过多字段       | 不使用 `SELECT *`          |
| 批量操作分批执行       | 控制锁和事务大小           |
| 定期更新统计信息       | 大批量变更后执行 `ANALYZE` |
| 监控表膨胀和索引使用   | 定期治理                   |
| 报表查询隔离           | 使用从库、物化视图或离线库 |
| 优化后必须验证         | 对比执行计划和耗时         |

分页优化示例：

```sql
SELECT
    id,
    order_no,
    total_amount,
    create_time
FROM app.order_info
WHERE user_id = 10001
  AND (
      create_time < TIMESTAMP '2026-05-09 10:00:00'
      OR (
          create_time = TIMESTAMP '2026-05-09 10:00:00'
          AND id < 100000
      )
  )
ORDER BY create_time DESC, id DESC
LIMIT 20;
```

### 安全开发规范

安全开发规范包括 SQL 注入防护、最小权限、敏感数据保护、日志脱敏、备份保护、连接安全和审计。数据库安全不是只靠 DBA，应用开发也必须遵守安全边界。

SQL 注入不推荐写法：

```xml
<!-- 不推荐：用户输入不能直接使用 ${} -->
<select id="selectByUsername" resultType="SysUser">
    SELECT *
    FROM app.sys_user
    WHERE username = '${username}'
</select>
```

推荐写法：

```xml
<select id="selectByUsername" resultType="SysUser">
    SELECT
        id,
        username,
        nickname,
        phone,
        email
    FROM app.sys_user
    WHERE username = #{username}
      AND deleted = FALSE
</select>
```

敏感字段脱敏视图：

```sql
CREATE OR REPLACE VIEW app.v_sys_user_masked AS
SELECT
    id,
    username,
    nickname,
    CASE
        WHEN phone IS NULL THEN NULL
        WHEN length(phone) < 7 THEN phone
        ELSE left(phone, 3) || '****' || right(phone, 4)
    END AS phone,
    CASE
        WHEN email IS NULL THEN NULL
        ELSE regexp_replace(email, '(^.).*(@.*$)', '\1***\2')
    END AS email,
    enabled,
    create_time
FROM app.sys_user
WHERE deleted = FALSE;
```

安全开发建议如下：

| 建议                       | 说明                              |
| -------------------------- | --------------------------------- |
| 所有用户输入参数化         | 防止 SQL 注入                     |
| 排序字段使用白名单         | 字段名不能直接接收前端输入        |
| 应用账号最小权限           | 不使用超级用户                    |
| 密码和密钥不入库或加密管理 | 使用配置中心或密钥系统            |
| 日志不打印敏感数据         | 密码、Token、身份证、银行卡等脱敏 |
| 备份文件加密和限权         | 备份包含完整数据                  |
| 报表查询走脱敏视图         | 不直接开放敏感表                  |
| 生产关闭详细错误输出       | 不暴露 SQL 和表结构               |
| 定期审计权限               | 检查账号、角色、授权              |
| 开启连接安全控制           | `pg_hba.conf` 限制来源 IP         |

## 运维常用命令

运维常用命令包括服务管理、连接管理、数据库管理、用户管理、表管理、索引管理、备份恢复和监控排查。实际生产环境中，命令执行前应确认目标环境、数据库名、用户、影响范围和回滚方案。

### 服务管理命令

使用 systemd 管理 PostgreSQL 服务：

```bash
# 查看服务状态
systemctl status postgresql

# 启动服务
systemctl start postgresql

# 停止服务
systemctl stop postgresql

# 重启服务
systemctl restart postgresql

# 重新加载配置
systemctl reload postgresql

# 设置开机自启
systemctl enable postgresql
```

使用 `pg_ctl` 管理服务：

```bash
# 启动 PostgreSQL
pg_ctl start -D /var/lib/postgresql/17/main

# 停止 PostgreSQL
pg_ctl stop -D /var/lib/postgresql/17/main -m fast

# 重启 PostgreSQL
pg_ctl restart -D /var/lib/postgresql/17/main -m fast

# 重新加载配置
pg_ctl reload -D /var/lib/postgresql/17/main

# 查看状态
pg_ctl status -D /var/lib/postgresql/17/main
```

Docker 管理命令：

```bash
# 查看容器
docker ps

# 启动容器
docker start postgres17

# 停止容器
docker stop postgres17

# 重启容器
docker restart postgres17

# 查看日志
docker logs -f postgres17

# 进入容器
docker exec -it postgres17 bash
```

查看配置文件位置：

```sql
SHOW config_file;
SHOW hba_file;
SHOW data_directory;
```

重新加载配置：

```sql
SELECT pg_reload_conf();
```

服务管理建议如下：

| 命令               | 使用场景                                              |
| ------------------ | ----------------------------------------------------- |
| `reload`           | 修改 `pg_hba.conf`、部分参数                          |
| `restart`          | 修改 `max_connections`、`shared_buffers` 等需重启参数 |
| `status`           | 查看服务是否正常                                      |
| `logs`             | 排查启动失败和运行错误                                |
| `pg_reload_conf()` | SQL 内重载配置                                        |

### 连接管理命令

使用 `psql` 连接数据库：

```bash
psql \
  -h 127.0.0.1 \
  -p 5432 \
  -U app_user \
  -d app_db
```

使用连接字符串：

```bash
psql "postgresql://app_user@127.0.0.1:5432/app_db?sslmode=disable"
```

检查 PostgreSQL 是否可连接：

```bash
pg_isready \
  -h 127.0.0.1 \
  -p 5432 \
  -d app_db \
  -U app_user
```

`psql` 常用元命令：

```sql
-- 查看数据库
\l

-- 切换数据库
\c app_db

-- 查看 Schema
\dn

-- 查看表
\dt app.*

-- 查看表结构
\d app.order_info

-- 查看索引
\di app.*

-- 查看视图
\dv app.*

-- 查看函数
\df app.*

-- 打开扩展显示
\x

-- 查看当前连接信息
\conninfo

-- 退出
\q
```

查看当前连接：

```sql
SELECT
    pid,
    usename,
    datname,
    application_name,
    client_addr,
    state,
    backend_start,
    query
FROM pg_stat_activity
ORDER BY backend_start DESC;
```

终止指定连接：

```sql
SELECT pg_terminate_backend(12345);
```

取消指定 SQL，不断开连接：

```sql
SELECT pg_cancel_backend(12345);
```

连接管理建议如下：

| 命令                   | 说明               |
| ---------------------- | ------------------ |
| `pg_isready`           | 健康检查           |
| `pg_cancel_backend`    | 取消正在执行的 SQL |
| `pg_terminate_backend` | 强制断开连接       |
| `\conninfo`            | 查看当前连接信息   |
| `pg_stat_activity`     | 查看连接详情       |

### 数据库管理命令

创建数据库：

```bash
createdb \
  -h 127.0.0.1 \
  -p 5432 \
  -U postgres \
  -O app_owner \
  app_db
```

删除数据库：

```bash
dropdb \
  -h 127.0.0.1 \
  -p 5432 \
  -U postgres \
  app_db
```

SQL 创建数据库：

```sql
CREATE DATABASE app_db
WITH
    OWNER = app_owner
    ENCODING = 'UTF8'
    TEMPLATE = template0;
```

查看数据库：

```sql
SELECT
    datname,
    pg_encoding_to_char(encoding) AS encoding,
    datcollate,
    datctype,
    datallowconn
FROM pg_database
ORDER BY datname;
```

查看数据库大小：

```sql
SELECT
    datname,
    pg_size_pretty(pg_database_size(datname)) AS database_size
FROM pg_database
ORDER BY pg_database_size(datname) DESC;
```

修改数据库所有者：

```sql
ALTER DATABASE app_db OWNER TO app_owner;
```

限制数据库连接数：

```sql
ALTER DATABASE app_db CONNECTION LIMIT 200;
```

断开指定数据库连接：

```sql
SELECT pg_terminate_backend(pid)
FROM pg_stat_activity
WHERE datname = 'app_db'
  AND pid <> pg_backend_pid();
```

数据库管理建议如下：

| 操作       | 注意事项               |
| ---------- | ---------------------- |
| 删除数据库 | 先确认连接和备份       |
| 修改连接数 | 结合应用连接池总量     |
| 修改 Owner | 确认权限继承和默认权限 |
| 创建数据库 | 统一编码和排序规则     |
| 断开连接   | 确认是否有业务请求     |

### 用户管理命令

创建登录用户：

```sql
CREATE ROLE app_user
WITH
    LOGIN
    PASSWORD 'change_me_password';
```

创建角色组：

```sql
CREATE ROLE app_rw NOLOGIN;
CREATE ROLE app_ro NOLOGIN;
```

授权角色：

```sql
GRANT app_rw TO app_user;
GRANT app_ro TO report_user;
```

修改密码：

```sql
ALTER ROLE app_user WITH PASSWORD 'new_strong_password';
```

限制连接数：

```sql
ALTER ROLE app_user CONNECTION LIMIT 50;
```

设置默认搜索路径：

```sql
ALTER ROLE app_user
IN DATABASE app_db
SET search_path = app, public;
```

查看用户和角色：

```sql
SELECT
    rolname,
    rolcanlogin,
    rolsuper,
    rolcreatedb,
    rolcreaterole,
    rolreplication,
    rolconnlimit
FROM pg_roles
ORDER BY rolname;
```

删除用户：

```sql
DROP ROLE IF EXISTS old_user;
```

如果用户拥有对象，需要先变更对象所有者或清理依赖：

```sql
REASSIGN OWNED BY old_user TO app_owner;
DROP OWNED BY old_user;
DROP ROLE old_user;
```

用户管理建议如下：

| 建议                   | 说明                   |
| ---------------------- | ---------------------- |
| 应用账号不要是超级用户 | 避免高危误操作         |
| 角色组和登录账号分离   | 权限授予角色组         |
| 密码定期轮换           | 配合配置中心更新       |
| 限制连接数             | 防止单账号耗尽连接     |
| 离职账号及时禁用       | 回收权限和对象         |
| 使用 `REASSIGN OWNED`  | 删除用户前处理对象归属 |

### 表管理命令

查看表：

```sql
SELECT
    schemaname,
    tablename,
    tableowner
FROM pg_tables
WHERE schemaname = 'app'
ORDER BY tablename;
```

查看表结构：

```sql
\d app.order_info
```

查看字段信息：

```sql
SELECT
    column_name,
    data_type,
    is_nullable,
    column_default
FROM information_schema.columns
WHERE table_schema = 'app'
  AND table_name = 'order_info'
ORDER BY ordinal_position;
```

查看表大小：

```sql
SELECT
    schemaname,
    relname AS table_name,
    pg_size_pretty(pg_relation_size(relid)) AS table_size,
    pg_size_pretty(pg_indexes_size(relid)) AS indexes_size,
    pg_size_pretty(pg_total_relation_size(relid)) AS total_size
FROM pg_stat_user_tables
ORDER BY pg_total_relation_size(relid) DESC;
```

重命名表：

```sql
ALTER TABLE app.order_info
RENAME TO order_info_old;
```

新增字段：

```sql
ALTER TABLE app.order_info
ADD COLUMN IF NOT EXISTS remark VARCHAR(255);
```

修改字段默认值：

```sql
ALTER TABLE app.order_info
ALTER COLUMN order_status SET DEFAULT 'PENDING';
```

设置非空：

```sql
ALTER TABLE app.order_info
ALTER COLUMN order_status SET NOT NULL;
```

删除字段：

```sql
ALTER TABLE app.order_info
DROP COLUMN IF EXISTS remark;
```

清空表：

```sql
TRUNCATE TABLE app.order_info;
```

清空表并重置自增：

```sql
TRUNCATE TABLE app.order_info RESTART IDENTITY;
```

表维护命令：

```sql
VACUUM ANALYZE app.order_info;

ANALYZE app.order_info;

VACUUM VERBOSE app.order_info;
```

表管理建议如下：

| 操作          | 注意事项                      |
| ------------- | ----------------------------- |
| `ALTER TABLE` | 大表变更评估锁                |
| `DROP COLUMN` | 先确认应用不再使用            |
| `TRUNCATE`    | 高危操作，生产谨慎            |
| `VACUUM FULL` | 会强锁表，低峰执行            |
| `ANALYZE`     | 大批量导入后执行              |
| 表重命名      | 注意视图、函数、应用 SQL 依赖 |

### 索引管理命令

查看索引：

```sql
SELECT
    schemaname,
    tablename,
    indexname,
    indexdef
FROM pg_indexes
WHERE schemaname = 'app'
ORDER BY tablename, indexname;
```

查看索引大小：

```sql
SELECT
    schemaname,
    tablename,
    indexname,
    pg_size_pretty(
        pg_relation_size(format('%I.%I', schemaname, indexname)::regclass)
    ) AS index_size
FROM pg_indexes
WHERE schemaname = 'app'
ORDER BY
    pg_relation_size(format('%I.%I', schemaname, indexname)::regclass) DESC;
```

创建普通索引：

```sql
CREATE INDEX idx_order_info_user_time
ON app.order_info (user_id, create_time DESC, id DESC);
```

并发创建索引：

```sql
CREATE INDEX CONCURRENTLY idx_order_info_status_time
ON app.order_info (order_status, create_time DESC, id DESC);
```

创建唯一索引：

```sql
CREATE UNIQUE INDEX uk_order_info_order_no
ON app.order_info (order_no);
```

创建部分索引：

```sql
CREATE INDEX idx_order_info_deleted_false_time
ON app.order_info (create_time DESC, id DESC)
WHERE deleted = FALSE;
```

删除索引：

```sql
DROP INDEX IF EXISTS app.idx_order_info_user_time;
```

并发删除索引：

```sql
DROP INDEX CONCURRENTLY IF EXISTS app.idx_order_info_status_time;
```

重建索引：

```sql
REINDEX INDEX app.idx_order_info_user_time;
```

并发重建索引：

```sql
REINDEX INDEX CONCURRENTLY app.idx_order_info_user_time;
```

查看索引使用情况：

```sql
SELECT
    schemaname,
    relname AS table_name,
    indexrelname AS index_name,
    idx_scan,
    idx_tup_read,
    idx_tup_fetch
FROM pg_stat_user_indexes
ORDER BY idx_scan ASC, relname, indexrelname;
```

索引管理建议如下：

| 操作       | 建议                         |
| ---------- | ---------------------------- |
| 大表建索引 | 使用 `CONCURRENTLY`          |
| 删除索引   | 先观察使用情况，保留回滚 DDL |
| 唯一索引   | 确认是否承担业务约束         |
| 重建索引   | 大索引低峰执行               |
| 表达式索引 | 查询表达式必须匹配           |
| 部分索引   | 查询条件必须包含索引谓词     |

### 备份恢复命令

使用 `pg_dump` 备份数据库：

```bash
pg_dump \
  -h 127.0.0.1 \
  -p 5432 \
  -U app_backup \
  -d app_db \
  -F c \
  -Z 6 \
  -f /backup/app_db_$(date +%Y%m%d_%H%M%S).dump
```

备份指定表：

```bash
pg_dump \
  -h 127.0.0.1 \
  -p 5432 \
  -U app_backup \
  -d app_db \
  -F c \
  -t app.order_info \
  -f /backup/order_info_$(date +%Y%m%d_%H%M%S).dump
```

备份全局对象：

```bash
pg_dumpall \
  -h 127.0.0.1 \
  -p 5432 \
  -U postgres \
  --globals-only \
  -f /backup/globals_$(date +%Y%m%d_%H%M%S).sql
```

查看备份内容：

```bash
pg_restore \
  -l \
  /backup/app_db_20260509_120000.dump
```

恢复到新数据库：

```bash
createdb \
  -h 127.0.0.1 \
  -p 5432 \
  -U postgres \
  app_db_restore

pg_restore \
  -h 127.0.0.1 \
  -p 5432 \
  -U postgres \
  -d app_db_restore \
  -j 4 \
  /backup/app_db_20260509_120000.dump
```

恢复 SQL 文件：

```bash
psql \
  -h 127.0.0.1 \
  -p 5432 \
  -U postgres \
  -d app_db_restore \
  -f /backup/app_db_20260509_120000.sql
```

基础备份：

```bash
pg_basebackup \
  -h 127.0.0.1 \
  -p 5432 \
  -U repl_user \
  -D /backup/basebackup_20260509 \
  -Fp \
  -Xs \
  -P
```

备份文件校验：

```bash
sha256sum /backup/app_db_20260509_120000.dump \
  > /backup/app_db_20260509_120000.dump.sha256

sha256sum -c /backup/app_db_20260509_120000.dump.sha256
```

备份恢复建议如下：

| 建议                     | 说明                           |
| ------------------------ | ------------------------------ |
| 逻辑备份使用 custom 格式 | 便于 `pg_restore`              |
| 恢复先到临时库           | 不直接覆盖生产                 |
| 备份后生成校验值         | 防止文件损坏                   |
| 定期恢复演练             | 证明备份可用                   |
| 敏感备份加密保存         | 备份包含完整数据               |
| 全局对象单独备份         | 角色和表空间不完全在单库备份中 |
| 恢复后执行验证 SQL       | 检查表、数据、约束、扩展       |

### 监控排查命令

查看当前活动 SQL：

```sql
SELECT
    pid,
    usename,
    datname,
    application_name,
    client_addr,
    state,
    now() - query_start AS query_duration,
    wait_event_type,
    wait_event,
    query
FROM pg_stat_activity
WHERE state = 'active'
ORDER BY query_duration DESC;
```

查看连接数：

```sql
SELECT
    datname,
    usename,
    application_name,
    state,
    COUNT(*) AS connection_count
FROM pg_stat_activity
GROUP BY
    datname,
    usename,
    application_name,
    state
ORDER BY connection_count DESC;
```

查看长事务：

```sql
SELECT
    pid,
    usename,
    datname,
    application_name,
    state,
    now() - xact_start AS transaction_duration,
    now() - query_start AS query_duration,
    query
FROM pg_stat_activity
WHERE xact_start IS NOT NULL
ORDER BY transaction_duration DESC;
```

查看锁等待：

```sql
SELECT
    pid,
    usename,
    datname,
    application_name,
    wait_event_type,
    wait_event,
    now() - query_start AS query_duration,
    query
FROM pg_stat_activity
WHERE wait_event_type = 'Lock'
ORDER BY query_duration DESC;
```

查看阻塞关系：

```sql
SELECT
    blocked.pid AS blocked_pid,
    blocked.query AS blocked_query,
    now() - blocked.query_start AS blocked_duration,
    blocking.pid AS blocking_pid,
    blocking.query AS blocking_query,
    now() - blocking.query_start AS blocking_duration
FROM pg_stat_activity blocked
JOIN pg_locks blocked_locks
    ON blocked_locks.pid = blocked.pid
JOIN pg_locks blocking_locks
    ON blocking_locks.locktype = blocked_locks.locktype
   AND blocking_locks.database IS NOT DISTINCT FROM blocked_locks.database
   AND blocking_locks.relation IS NOT DISTINCT FROM blocked_locks.relation
   AND blocking_locks.page IS NOT DISTINCT FROM blocked_locks.page
   AND blocking_locks.tuple IS NOT DISTINCT FROM blocked_locks.tuple
   AND blocking_locks.virtualxid IS NOT DISTINCT FROM blocked_locks.virtualxid
   AND blocking_locks.transactionid IS NOT DISTINCT FROM blocked_locks.transactionid
   AND blocking_locks.classid IS NOT DISTINCT FROM blocked_locks.classid
   AND blocking_locks.objid IS NOT DISTINCT FROM blocked_locks.objid
   AND blocking_locks.objsubid IS NOT DISTINCT FROM blocked_locks.objsubid
   AND blocking_locks.pid <> blocked_locks.pid
JOIN pg_stat_activity blocking
    ON blocking.pid = blocking_locks.pid
WHERE NOT blocked_locks.granted
  AND blocking_locks.granted
ORDER BY blocked_duration DESC;
```

查看数据库缓存命中率：

```sql
SELECT
    datname,
    blks_read,
    blks_hit,
    ROUND(
        blks_hit::NUMERIC / NULLIF(blks_hit + blks_read, 0) * 100,
        2
    ) AS cache_hit_ratio_percent
FROM pg_stat_database
WHERE datname IS NOT NULL
ORDER BY cache_hit_ratio_percent ASC NULLS LAST;
```

查看表死元组：

```sql
SELECT
    schemaname,
    relname AS table_name,
    n_live_tup,
    n_dead_tup,
    ROUND(
        n_dead_tup::NUMERIC / NULLIF(n_live_tup + n_dead_tup, 0) * 100,
        2
    ) AS dead_tuple_ratio_percent,
    last_autovacuum,
    last_autoanalyze
FROM pg_stat_user_tables
ORDER BY n_dead_tup DESC;
```

查看表大小：

```sql
SELECT
    schemaname,
    relname AS table_name,
    pg_size_pretty(pg_relation_size(relid)) AS table_size,
    pg_size_pretty(pg_indexes_size(relid)) AS indexes_size,
    pg_size_pretty(pg_total_relation_size(relid)) AS total_size
FROM pg_stat_user_tables
ORDER BY pg_total_relation_size(relid) DESC;
```

查看索引使用情况：

```sql
SELECT
    schemaname,
    relname AS table_name,
    indexrelname AS index_name,
    idx_scan,
    idx_tup_read,
    idx_tup_fetch,
    pg_size_pretty(pg_relation_size(indexrelid)) AS index_size
FROM pg_stat_user_indexes
ORDER BY idx_scan ASC, pg_relation_size(indexrelid) DESC;
```

查看复制状态：

```sql
SELECT
    pid,
    usename,
    application_name,
    client_addr,
    state,
    sync_state,
    sent_lsn,
    write_lsn,
    flush_lsn,
    replay_lsn,
    write_lag,
    flush_lag,
    replay_lag
FROM pg_stat_replication;
```

查看归档状态：

```sql
SELECT
    archived_count,
    last_archived_wal,
    last_archived_time,
    failed_count,
    last_failed_wal,
    last_failed_time
FROM pg_stat_archiver;
```

查看复制槽 WAL 保留：

```sql
SELECT
    slot_name,
    slot_type,
    active,
    restart_lsn,
    pg_size_pretty(
        pg_wal_lsn_diff(pg_current_wal_lsn(), restart_lsn)
    ) AS retained_wal_size
FROM pg_replication_slots
WHERE restart_lsn IS NOT NULL
ORDER BY pg_wal_lsn_diff(pg_current_wal_lsn(), restart_lsn) DESC;
```

监控排查建议如下：

| 问题       | 首先查看                                         |
| ---------- | ------------------------------------------------ |
| 接口慢     | `pg_stat_activity`、慢日志、`pg_stat_statements` |
| 连接耗尽   | `pg_stat_activity` 连接分布                      |
| 锁等待     | `pg_locks` + `pg_stat_activity`                  |
| 表变大     | `pg_stat_user_tables`、表大小                    |
| 索引无效   | `pg_stat_user_indexes`、执行计划                 |
| 从库延迟   | `pg_stat_replication`                            |
| WAL 暴涨   | `pg_replication_slots`、`pg_stat_archiver`       |
| 缓存命中低 | `pg_stat_database`、`pg_statio_user_tables`      |
| 死锁       | 数据库日志、`pg_stat_database.deadlocks`         |

运维命令执行前应先确认目标库、目标表、目标用户和影响范围。生产环境中的 `DROP`、`TRUNCATE`、`VACUUM FULL`、`REINDEX`、`pg_terminate_backend`、大批量 `UPDATE` 和 `DELETE` 都属于高风险操作，必须先备份、评估锁影响，并准备回滚方案。

## PostgreSQL 17 项目实践清单

项目实践清单用于在开发、评审、测试、上线和运维阶段快速检查 PostgreSQL 使用是否规范。清单不替代详细设计文档，但可以作为项目落地前的最低检查标准。

### 开发环境清单

开发环境应保证 PostgreSQL 版本、字符集、时区、扩展、Schema、账号权限和迁移工具与测试、生产环境保持一致。开发环境不要求资源规格一致，但数据库行为应尽量一致。

| 检查项          | 要求                                                        | 是否完成 |
| --------------- | ----------------------------------------------------------- | -------- |
| PostgreSQL 版本 | 使用 PostgreSQL 17，开发、测试、生产主版本一致              | ☐        |
| 数据库编码      | 使用 `UTF8`                                                 | ☐        |
| 数据库时区      | 单地区项目统一使用 `Asia/Shanghai`；多地区项目统一 UTC 策略 | ☐        |
| Schema          | 业务对象放在独立 Schema，如 `app`，不直接使用 `public`      | ☐        |
| 连接账号        | 开发环境使用普通应用账号，不使用超级用户连接应用            | ☐        |
| 连接池          | Spring Boot 配置 HikariCP，设置合理连接数                   | ☐        |
| JDBC URL        | 配置 `ApplicationName` 和 `currentSchema`                   | ☐        |
| 迁移工具        | Flyway 或 Liquibase 已启用                                  | ☐        |
| 初始化脚本      | Schema、扩展、基础表、字典数据可自动初始化                  | ☐        |
| 常用扩展        | 按需安装 `pgcrypto`、`pg_trgm`、`btree_gist` 等             | ☐        |
| 本地数据        | 使用脱敏数据或测试数据，不使用生产敏感数据                  | ☐        |
| 日志配置        | 应用日志不输出密码、Token、密钥等敏感信息                   | ☐        |

开发环境推荐连接配置：

```yaml
spring:
  datasource:
    driver-class-name: org.postgresql.Driver
    url: jdbc:postgresql://127.0.0.1:5432/app_db?ApplicationName=ateng-dev-service&currentSchema=app&sslmode=disable
    username: app_user
    password: ${POSTGRES_PASSWORD}
    hikari:
      pool-name: ateng-dev-postgresql-pool
      maximum-pool-size: 10
      minimum-idle: 2
      connection-timeout: 30000
      idle-timeout: 600000
      max-lifetime: 1800000
```

开发环境检查 SQL：

```sql
SELECT
    version() AS postgresql_version,
    current_database() AS database_name,
    current_schema() AS current_schema_name,
    current_user AS current_user_name;

SHOW server_encoding;
SHOW timezone;
```

开发环境建议如下：

| 建议                       | 说明                 |
| -------------------------- | -------------------- |
| 使用 Docker 快速搭建本地库 | 保证版本一致         |
| 初始化脚本一键执行         | 降低新同事接入成本   |
| 不在本地脚本中写死密码     | 使用环境变量         |
| 使用迁移工具管理结构       | 不手工维护本地库     |
| 测试数据单独管理           | 不混入生产初始化脚本 |

### 表结构设计清单

表结构设计应优先保证字段语义清晰、类型合理、约束完整、索引可规划、审计字段统一。表设计评审时，应重点检查主键、唯一约束、字段类型、默认值、非空约束、注释和业务边界。

| 检查项       | 要求                                                         | 是否完成 |
| ------------ | ------------------------------------------------------------ | -------- |
| 表名规范     | 使用小写下划线，名称表达业务含义                             | ☐        |
| 主键         | 默认使用 `id BIGINT GENERATED ALWAYS AS IDENTITY`            | ☐        |
| 业务唯一约束 | 订单号、用户名、编码等使用唯一约束兜底                       | ☐        |
| 字段类型     | 金额使用 `NUMERIC`，时间使用时间类型，布尔使用 `BOOLEAN`     | ☐        |
| 非空约束     | 必填字段设置 `NOT NULL`                                      | ☐        |
| 默认值       | 状态、布尔、时间等字段设置合理默认值                         | ☐        |
| 检查约束     | 金额、数量、状态枚举等增加 `CHECK`                           | ☐        |
| 外键         | 核心关系按业务需要设置外键或在应用层明确约束策略             | ☐        |
| 审计字段     | 包含 `create_time`、`update_time`、必要时包含 `create_user_id`、`update_user_id` | ☐        |
| 逻辑删除     | 核心业务表包含 `deleted BOOLEAN NOT NULL DEFAULT FALSE`      | ☐        |
| 乐观锁       | 并发编辑表包含 `version INTEGER NOT NULL DEFAULT 0`          | ☐        |
| 字段注释     | 表和关键字段使用 `COMMENT` 说明                              | ☐        |
| 大字段       | `TEXT`、`JSONB`、大对象字段评估查询和更新成本                | ☐        |
| 多租户       | 多租户表包含 `tenant_id`，唯一约束和索引包含租户字段         | ☐        |
| 分区         | 大日志表、订单流水表评估是否按时间分区                       | ☐        |

推荐基础表模板：

```sql
CREATE TABLE app.business_demo (
    id BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    business_code VARCHAR(64) NOT NULL,
    business_name VARCHAR(128) NOT NULL,
    business_status VARCHAR(32) NOT NULL DEFAULT 'ENABLED',
    enabled BOOLEAN NOT NULL DEFAULT TRUE,
    deleted BOOLEAN NOT NULL DEFAULT FALSE,
    version INTEGER NOT NULL DEFAULT 0,
    create_user_id BIGINT,
    update_user_id BIGINT,
    create_time TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_time TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uk_business_demo_code UNIQUE (business_code),
    CONSTRAINT ck_business_demo_status CHECK (
        business_status IN ('ENABLED', 'DISABLED')
    )
);

COMMENT ON TABLE app.business_demo IS '业务示例表';
COMMENT ON COLUMN app.business_demo.business_code IS '业务编码';
COMMENT ON COLUMN app.business_demo.business_name IS '业务名称';
COMMENT ON COLUMN app.business_demo.business_status IS '业务状态';
```

表结构设计建议如下：

| 建议                       | 说明                           |
| -------------------------- | ------------------------------ |
| 不使用业务字段作为主键     | 业务字段可能变更               |
| 不用字符串存储数字和时间   | 影响校验、排序和索引           |
| 不盲目使用 JSONB           | 稳定字段应拆成普通列           |
| 重要业务规则由数据库兜底   | 唯一性、非空、金额非负等       |
| 逻辑删除表考虑部分唯一索引 | 允许删除后复用编码时使用       |
| 大表设计提前考虑归档       | 日志、流水、订单历史表尤其重要 |

### SQL 开发清单

SQL 开发应保证可读、可维护、可使用索引、可防注入，并且符合事务和分页规范。所有复杂 SQL 在上线前都应使用 `EXPLAIN ANALYZE` 验证执行计划。

| 检查项     | 要求                                           | 是否完成 |
| ---------- | ---------------------------------------------- | -------- |
| 字段明确   | 禁止在业务 SQL 中使用 `SELECT *`               | ☐        |
| 参数绑定   | 所有用户输入使用参数绑定，不拼接 SQL           | ☐        |
| 查询条件   | 大表查询必须带有效过滤条件                     | ☐        |
| 分页排序   | 分页查询必须有稳定排序字段                     | ☐        |
| 深分页     | 避免大 OFFSET，优先游标分页或基于 ID 翻页      | ☐        |
| 更新条件   | `UPDATE` 和 `DELETE` 必须有明确 `WHERE` 条件   | ☐        |
| 状态流转   | 状态更新必须带原状态条件                       | ☐        |
| 空值处理   | 明确处理 `NULL`，合理使用 `COALESCE`、`NULLIF` | ☐        |
| 时间范围   | 时间查询使用左闭右开区间                       | ☐        |
| 索引列函数 | 避免在索引列上直接套函数                       | ☐        |
| 动态排序   | 排序字段使用应用层白名单                       | ☐        |
| JSONB 查询 | 高频 JSONB 查询必须评估索引                    | ☐        |
| 批量操作   | 大批量写入分批执行                             | ☐        |
| 执行计划   | 复杂 SQL 已执行 `EXPLAIN ANALYZE`              | ☐        |

推荐分页 SQL：

```sql
SELECT
    id,
    order_no,
    user_id,
    order_status,
    total_amount,
    create_time
FROM app.order_info
WHERE user_id = 10001
  AND deleted = FALSE
ORDER BY create_time DESC, id DESC
LIMIT 20;
```

推荐时间范围查询：

```sql
SELECT
    id,
    order_no,
    total_amount,
    create_time
FROM app.order_info
WHERE create_time >= TIMESTAMP '2026-05-01 00:00:00'
  AND create_time < TIMESTAMP '2026-06-01 00:00:00'
ORDER BY create_time DESC, id DESC;
```

推荐状态流转更新：

```sql
UPDATE app.order_info
SET
    order_status = 'PAID',
    pay_time = CURRENT_TIMESTAMP,
    update_time = CURRENT_TIMESTAMP,
    version = version + 1
WHERE id = 1
  AND order_status = 'PENDING'
  AND deleted = FALSE;
```

执行计划检查：

```sql
EXPLAIN (
    ANALYZE TRUE,
    BUFFERS TRUE,
    VERBOSE TRUE,
    SUMMARY TRUE
)
SELECT
    id,
    order_no,
    total_amount,
    create_time
FROM app.order_info
WHERE user_id = 10001
  AND deleted = FALSE
ORDER BY create_time DESC, id DESC
LIMIT 20;
```

SQL 开发建议如下：

| 建议                             | 说明                           |
| -------------------------------- | ------------------------------ |
| 先写正确，再看计划               | 不凭感觉优化                   |
| SQL 参数类型和字段类型一致       | 避免隐式类型转换               |
| 大表查询避免全表扫描             | 除非确实需要扫描大部分数据     |
| 报表 SQL 单独评审                | 复杂聚合和 JOIN 容易影响业务库 |
| 数据修复 SQL 先 SELECT 后 UPDATE | 明确影响范围                   |
| 所有危险 SQL 事务化执行          | 便于回滚                       |

### 索引设计清单

索引设计应围绕查询条件、排序方式、关联字段和唯一性要求展开。索引不是越多越好，每个索引都会增加写入成本和存储成本。

| 检查项     | 要求                                     | 是否完成 |
| ---------- | ---------------------------------------- | -------- |
| 主键索引   | 主键自动创建索引                         | ☐        |
| 唯一索引   | 业务唯一字段使用唯一约束或唯一索引       | ☐        |
| 查询索引   | 高频查询条件已有匹配索引                 | ☐        |
| 排序索引   | 分页排序字段和索引顺序匹配               | ☐        |
| JOIN 索引  | 关联字段有索引                           | ☐        |
| 联合索引   | 联合索引字段顺序符合查询模式             | ☐        |
| 部分索引   | 逻辑删除、高频状态过滤使用部分索引       | ☐        |
| 表达式索引 | 函数查询使用表达式索引                   | ☐        |
| JSONB 索引 | 高频 JSONB 包含查询使用 GIN 索引         | ☐        |
| 模糊搜索   | `LIKE '%xxx%'` 使用 `pg_trgm` 或搜索方案 | ☐        |
| 大表建索引 | 使用 `CREATE INDEX CONCURRENTLY`         | ☐        |
| 冗余索引   | 检查是否存在重复或低价值索引             | ☐        |
| 索引命名   | 索引名称符合规范                         | ☐        |

常用索引示例：

```sql
CREATE INDEX idx_order_info_user_time
ON app.order_info (user_id, create_time DESC, id DESC);

CREATE INDEX idx_order_info_status_time
ON app.order_info (order_status, create_time DESC, id DESC);

CREATE INDEX idx_order_info_not_deleted_time
ON app.order_info (create_time DESC, id DESC)
WHERE deleted = FALSE;
```

大表并发建索引：

```sql
CREATE INDEX CONCURRENTLY IF NOT EXISTS idx_order_info_pay_time
ON app.order_info (pay_time DESC)
WHERE pay_time IS NOT NULL;
```

检查索引使用：

```sql
SELECT
    schemaname,
    relname AS table_name,
    indexrelname AS index_name,
    idx_scan,
    idx_tup_read,
    idx_tup_fetch,
    pg_size_pretty(pg_relation_size(indexrelid)) AS index_size
FROM pg_stat_user_indexes
WHERE schemaname = 'app'
ORDER BY idx_scan ASC, pg_relation_size(indexrelid) DESC;
```

索引设计建议如下：

| 建议                     | 说明                                   |
| ------------------------ | -------------------------------------- |
| 每个索引必须能解释用途   | 不建无来源索引                         |
| 联合索引服务具体 SQL     | 不只看字段是否出现                     |
| 写多读少表控制索引数量   | 索引越多写入越慢                       |
| 低选择性字段不单独建索引 | 如单独 `deleted`、`enabled` 通常收益低 |
| 删除索引前观察业务周期   | 月报索引可能低频但重要                 |
| 索引上线后复查执行计划   | 确认实际命中                           |

### 权限配置清单

权限配置应遵循最小权限原则。应用账号、只读账号、迁移账号、运维账号应分离，生产环境严禁应用使用超级用户。

| 检查项      | 要求                                  | 是否完成 |
| ----------- | ------------------------------------- | -------- |
| 超级用户    | 应用不使用超级用户                    | ☐        |
| 账号分离    | 应用读写、只读、迁移、运维账号分离    | ☐        |
| 角色授权    | 权限授予角色，不直接散落给个人账号    | ☐        |
| 数据库权限  | 应用账号仅授予必要 `CONNECT`          | ☐        |
| Schema 权限 | 应用账号有 `USAGE`，无不必要 `CREATE` | ☐        |
| 表权限      | 读写账号和只读账号权限区分            | ☐        |
| 序列权限    | 自增表插入账号有序列使用权限          | ☐        |
| 函数权限    | 敏感函数回收 `PUBLIC` 权限            | ☐        |
| 默认权限    | 新建表、序列、函数自动授权            | ☐        |
| public 权限 | 回收 `public` Schema 的不必要创建权限 | ☐        |
| 敏感表      | 用户认证、密钥、审计表单独控制权限    | ☐        |
| RLS         | 多租户强隔离场景评估行级安全策略      | ☐        |

推荐角色初始化：

```sql
CREATE ROLE app_owner NOLOGIN;
CREATE ROLE app_rw NOLOGIN;
CREATE ROLE app_ro NOLOGIN;
CREATE ROLE app_migration NOLOGIN;

CREATE ROLE app_user WITH LOGIN PASSWORD 'change_me_password';
CREATE ROLE report_user WITH LOGIN PASSWORD 'change_me_report_password';

GRANT app_rw TO app_user;
GRANT app_ro TO report_user;
```

基础授权：

```sql
GRANT CONNECT ON DATABASE app_db TO app_rw;
GRANT CONNECT ON DATABASE app_db TO app_ro;

GRANT USAGE ON SCHEMA app TO app_rw;
GRANT USAGE ON SCHEMA app TO app_ro;

GRANT SELECT, INSERT, UPDATE, DELETE
ON ALL TABLES IN SCHEMA app
TO app_rw;

GRANT SELECT
ON ALL TABLES IN SCHEMA app
TO app_ro;

GRANT USAGE, SELECT
ON ALL SEQUENCES IN SCHEMA app
TO app_rw;
```

默认权限：

```sql
ALTER DEFAULT PRIVILEGES FOR ROLE app_owner IN SCHEMA app
GRANT SELECT, INSERT, UPDATE, DELETE ON TABLES TO app_rw;

ALTER DEFAULT PRIVILEGES FOR ROLE app_owner IN SCHEMA app
GRANT SELECT ON TABLES TO app_ro;

ALTER DEFAULT PRIVILEGES FOR ROLE app_owner IN SCHEMA app
GRANT USAGE, SELECT ON SEQUENCES TO app_rw;

ALTER DEFAULT PRIVILEGES FOR ROLE app_owner IN SCHEMA app
GRANT EXECUTE ON FUNCTIONS TO app_rw;
```

权限检查 SQL：

```sql
SELECT
    table_schema,
    table_name,
    grantee,
    privilege_type
FROM information_schema.role_table_grants
WHERE table_schema = 'app'
ORDER BY table_name, grantee, privilege_type;
```

权限配置建议如下：

| 建议                   | 说明                                           |
| ---------------------- | ---------------------------------------------- |
| 生产账号权限从严       | 开发环境也不要使用超级用户习惯                 |
| 迁移账号和运行账号分离 | 防止应用运行时执行 DDL                         |
| 报表账号只读           | 必要时只开放脱敏视图                           |
| 定期审计权限           | 清理废弃账号和临时授权                         |
| 敏感函数单独授权       | 数据修复、归档、刷新统计等函数不能开放给所有人 |

### 备份恢复清单

备份恢复清单用于确认数据库具备可恢复能力。没有经过恢复验证的备份不能视为有效备份。生产环境应同时关注备份文件、校验、异地保存、恢复演练和权限控制。

| 检查项   | 要求                                                | 是否完成 |
| -------- | --------------------------------------------------- | -------- |
| 备份策略 | 明确全量、增量或 WAL 归档策略                       | ☐        |
| 逻辑备份 | 中小库使用 `pg_dump -Fc` 定期备份                   | ☐        |
| 全局对象 | 角色、表空间等使用 `pg_dumpall --globals-only` 备份 | ☐        |
| 备份账号 | 使用专用备份账号，不使用应用账号                    | ☐        |
| 备份命名 | 文件名包含数据库名和时间戳                          | ☐        |
| 校验文件 | 生成 SHA-256 校验文件                               | ☐        |
| 备份加密 | 含敏感数据的备份文件加密保存                        | ☐        |
| 异地保存 | 备份文件不只保存在数据库服务器本机                  | ☐        |
| 保留周期 | 明确保留 7 天、30 天、180 天等策略                  | ☐        |
| 恢复验证 | 定期恢复到临时库验证                                | ☐        |
| 恢复文档 | 有完整恢复步骤和责任人                              | ☐        |
| 恢复演练 | 至少按月或按季度演练                                | ☐        |

逻辑备份命令：

```bash
pg_dump \
  -h 127.0.0.1 \
  -p 5432 \
  -U app_backup \
  -d app_db \
  -F c \
  -Z 6 \
  -f /backup/app_db_$(date +%Y%m%d_%H%M%S).dump
```

全局对象备份：

```bash
pg_dumpall \
  -h 127.0.0.1 \
  -p 5432 \
  -U postgres \
  --globals-only \
  -f /backup/globals_$(date +%Y%m%d_%H%M%S).sql
```

生成校验文件：

```bash
sha256sum /backup/app_db_20260509_020000.dump \
  > /backup/app_db_20260509_020000.dump.sha256
```

恢复验证：

```bash
createdb \
  -h 127.0.0.1 \
  -p 5432 \
  -U postgres \
  app_db_verify

pg_restore \
  -h 127.0.0.1 \
  -p 5432 \
  -U postgres \
  -d app_db_verify \
  -j 4 \
  /backup/app_db_20260509_020000.dump
```

恢复后验证 SQL：

```sql
SELECT
    COUNT(*) AS table_count
FROM information_schema.tables
WHERE table_schema = 'app'
  AND table_type = 'BASE TABLE';

SELECT
    extname,
    extversion
FROM pg_extension
ORDER BY extname;
```

备份恢复建议如下：

| 建议               | 说明                             |
| ------------------ | -------------------------------- |
| 备份和恢复一起设计 | 只备份不演练没有意义             |
| 重要变更前临时备份 | DDL、大批量修复、版本升级前执行  |
| 备份文件权限从严   | 备份包含完整敏感数据             |
| 生产恢复先到临时库 | 不直接覆盖生产                   |
| 恢复后执行数据校验 | 检查表数量、关键数据、约束、扩展 |
| 明确 RPO 和 RTO    | 最多丢多少数据、多久恢复         |

### 性能检查清单

性能检查用于在上线前或问题排查时确认数据库是否存在慢 SQL、索引缺失、连接异常、长事务、锁等待、表膨胀、统计信息过旧等问题。性能检查应基于统计视图、执行计划和监控数据。

| 检查项     | 要求                                    | 是否完成 |
| ---------- | --------------------------------------- | -------- |
| 慢查询     | 已开启慢查询日志或 `pg_stat_statements` | ☐        |
| 执行计划   | 核心 SQL 已执行 `EXPLAIN ANALYZE`       | ☐        |
| 索引命中   | 高频查询能使用合适索引                  | ☐        |
| 连接数     | 当前连接数和连接池配置合理              | ☐        |
| 长事务     | 无长期 `idle in transaction`            | ☐        |
| 锁等待     | 无异常锁等待                            | ☐        |
| 表膨胀     | 死元组比例可控                          | ☐        |
| 统计信息   | 大表最近执行过 `ANALYZE` 或自动分析正常 | ☐        |
| 缓存命中率 | 数据库和热点表缓存命中率正常            | ☐        |
| 索引使用   | 无明显冗余大索引                        | ☐        |
| 复制延迟   | 主从环境复制延迟可接受                  | ☐        |
| WAL 堆积   | 复制槽和归档状态正常                    | ☐        |

查看当前慢 SQL：

```sql
SELECT
    pid,
    usename,
    datname,
    application_name,
    now() - query_start AS query_duration,
    wait_event_type,
    wait_event,
    query
FROM pg_stat_activity
WHERE state = 'active'
ORDER BY query_duration DESC
LIMIT 20;
```

查看累计 SQL 统计：

```sql
SELECT
    query,
    calls,
    total_exec_time,
    mean_exec_time,
    max_exec_time,
    rows
FROM pg_stat_statements
ORDER BY total_exec_time DESC
LIMIT 20;
```

查看连接分布：

```sql
SELECT
    datname,
    usename,
    application_name,
    state,
    COUNT(*) AS connection_count
FROM pg_stat_activity
GROUP BY
    datname,
    usename,
    application_name,
    state
ORDER BY connection_count DESC;
```

查看长事务：

```sql
SELECT
    pid,
    usename,
    datname,
    application_name,
    state,
    now() - xact_start AS transaction_duration,
    query
FROM pg_stat_activity
WHERE xact_start IS NOT NULL
ORDER BY transaction_duration DESC;
```

查看表膨胀线索：

```sql
SELECT
    schemaname,
    relname AS table_name,
    n_live_tup,
    n_dead_tup,
    ROUND(
        n_dead_tup::NUMERIC / NULLIF(n_live_tup + n_dead_tup, 0) * 100,
        2
    ) AS dead_tuple_ratio_percent,
    last_autovacuum,
    last_autoanalyze
FROM pg_stat_user_tables
ORDER BY n_dead_tup DESC
LIMIT 20;
```

查看索引使用：

```sql
SELECT
    schemaname,
    relname AS table_name,
    indexrelname AS index_name,
    idx_scan,
    pg_size_pretty(pg_relation_size(indexrelid)) AS index_size
FROM pg_stat_user_indexes
ORDER BY idx_scan ASC, pg_relation_size(indexrelid) DESC
LIMIT 50;
```

性能检查建议如下：

| 建议                       | 说明                     |
| -------------------------- | ------------------------ |
| 每次上线前检查核心 SQL     | 尤其是新增接口和报表     |
| 压测前后对比统计           | 观察连接、慢 SQL、锁等待 |
| 发现慢 SQL 先看计划        | 不直接调大连接池         |
| 大批量导入后执行 `ANALYZE` | 避免统计信息过旧         |
| 大表删除后检查膨胀         | 必要时维护或分区归档     |
| 索引治理要谨慎             | 低频索引可能服务周期任务 |

### 上线发布清单

上线发布清单用于确认数据库变更、应用版本、配置、权限、备份、回滚和监控都已准备就绪。数据库变更往往比应用发布更难回滚，因此必须提前评审和演练。

| 检查项   | 要求                                     | 是否完成 |
| -------- | ---------------------------------------- | -------- |
| 脚本评审 | DDL、DML、迁移脚本已经评审               | ☐        |
| 脚本版本 | Flyway 或 Liquibase 版本号连续、命名规范 | ☐        |
| 兼容性   | 数据库变更兼容当前和新应用版本           | ☐        |
| 大表变更 | 大表 DDL、索引、数据迁移已评估锁和耗时   | ☐        |
| 并发索引 | 大表索引使用 `CONCURRENTLY`              | ☐        |
| 数据备份 | 发布前已完成必要备份                     | ☐        |
| 回滚脚本 | 高风险变更有回滚脚本或恢复方案           | ☐        |
| 数据迁移 | 迁移脚本幂等，可重复执行                 | ☐        |
| 配置检查 | 数据库连接、账号、密码、Schema 正确      | ☐        |
| 权限检查 | 新表、新序列、新函数权限已配置           | ☐        |
| 监控告警 | 慢查询、连接数、锁等待、错误率已监控     | ☐        |
| 发布窗口 | 高风险变更安排在低峰期                   | ☐        |
| 验证 SQL | 发布后验证 SQL 已准备                    | ☐        |
| 责任人   | 发布、验证、回滚责任人明确               | ☐        |

发布前检查迁移历史：

```sql
SELECT
    installed_rank,
    version,
    description,
    type,
    script,
    installed_by,
    installed_on,
    execution_time,
    success
FROM app.flyway_schema_history
ORDER BY installed_rank DESC;
```

新增表后检查权限：

```sql
SELECT
    table_schema,
    table_name,
    grantee,
    privilege_type
FROM information_schema.role_table_grants
WHERE table_schema = 'app'
ORDER BY table_name, grantee, privilege_type;
```

发布后检查连接和错误 SQL：

```sql
SELECT
    pid,
    usename,
    datname,
    application_name,
    state,
    now() - query_start AS query_duration,
    wait_event_type,
    wait_event,
    query
FROM pg_stat_activity
WHERE state = 'active'
ORDER BY query_duration DESC
LIMIT 20;
```

发布后核心数据验证示例：

```sql
SELECT
    COUNT(*) AS order_count
FROM app.order_info
WHERE create_time >= CURRENT_DATE;

SELECT
    order_status,
    COUNT(*) AS order_count
FROM app.order_info
GROUP BY order_status
ORDER BY order_status;
```

上线发布建议如下：

| 建议                         | 说明                           |
| ---------------------------- | ------------------------------ |
| 先数据库兼容变更，再发布应用 | 新旧应用都能运行               |
| 删除字段延后                 | 确认应用不再使用后再删除       |
| 大表变更分阶段执行           | 加字段、回填、加约束分开       |
| 数据迁移分批执行             | 避免长事务和大锁               |
| 发布后观察慢 SQL             | 新功能可能带来新查询压力       |
| 回滚前确认数据影响           | 应用回滚不一定等于数据库可回滚 |
| 所有操作留痕                 | 记录执行人、时间、脚本、结果   |

最终实践建议如下：

| 阶段     | 重点                             |
| -------- | -------------------------------- |
| 开发阶段 | 类型、约束、SQL、索引规范        |
| 测试阶段 | 迁移脚本、执行计划、异常场景     |
| 预发阶段 | 接近生产数据量验证               |
| 发布阶段 | 备份、兼容、回滚、监控           |
| 运行阶段 | 慢 SQL、锁、连接、膨胀、备份验证 |
| 迭代阶段 | 索引治理、归档清理、权限审计     |

PostgreSQL 17 项目实践的核心原则是：结构脚本化、权限最小化、SQL 可解释、索引有依据、备份可恢复、上线可回滚、运行可观测。
