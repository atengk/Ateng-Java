# Spring Boot 集成 Flowable 工作流开发

本文档用于说明如何在 Spring Boot 3 项目中集成 Flowable 工作流引擎，重点覆盖流程部署、流程启动、任务办理、审批记录、业务数据关联、权限集成以及流程图展示等后续开发内容。Spring Boot 3 起步版本要求 Java 17，Flowable 7.x 是面向 Spring Boot 3、Jakarta EE 9/10 和 Java 17 基线的版本线；Flowable 8 后续版本基于 Spring Boot 4 和 Spring 7，因此本文档建议以 Flowable 7.x 作为 Spring Boot 3 项目的工作流集成基线。([Home](https://docs.spring.io/spring-boot/docs/3.0.x/reference/html/getting-started.html))

## 项目概述

本项目以 Spring Boot 3 为基础，集成 Flowable BPMN 工作流引擎，为业务系统提供标准化、可扩展、可追踪的审批流能力。系统不直接把审批逻辑硬编码在业务代码中，而是通过 BPMN 流程定义描述审批节点、审批人、候选组、条件分支、会签、驳回和终止等流程规则。

项目整体目标是将“业务数据”和“流程状态”解耦：业务表负责保存请假、报销、采购、合同等业务单据数据，Flowable 负责维护流程定义、流程实例、运行任务、历史任务和流程变量。业务系统通过流程实例 ID、业务主键和流程变量完成两者关联。

### 功能定位

本模块定位为业务系统中的“通用流程审批能力层”，主要负责流程引擎集成、流程操作封装和审批业务扩展，不直接绑定某一个固定业务场景。

核心功能包括：

| 功能模块     | 说明                                        |
| ------------ | ------------------------------------------- |
| 流程定义管理 | 部署、查询、挂起、激活 BPMN 流程定义        |
| 流程实例管理 | 启动流程、查询流程进度、终止流程、撤回流程  |
| 待办任务管理 | 查询当前用户待办、候选任务、组任务          |
| 已办任务管理 | 查询用户已处理任务和历史审批记录            |
| 任务审批处理 | 完成任务、通过、驳回、转办、委派、加签等    |
| 业务数据关联 | 通过 `businessKey` 或业务扩展表关联业务单据 |
| 审批记录维护 | 记录审批人、审批意见、审批动作、审批时间    |
| 流程图展示   | 展示 BPMN 流程图、当前节点、高亮历史轨迹    |
| 权限集成     | 对接系统用户、角色、部门、岗位和数据权限    |

Flowable 在 Spring Boot 中通过 Starter 方式集成后，会自动创建流程引擎相关 Bean，例如 `RepositoryService`、`RuntimeService`、`TaskService` 等服务对象，业务系统可以基于这些服务完成流程定义、流程实例和任务操作。Flowable 官方文档也说明，加入 Flowable Spring Boot Starter 后，会创建流程引擎、暴露 Flowable 服务为 Spring Bean，并支持自动部署流程资源。([Flowable](https://www.flowable.com/open-source/docs/bpmn/ch05a-Spring-Boot/))

### 适用场景

本项目适用于需要“流程可配置、审批可追踪、规则可扩展”的后台管理系统或企业业务系统。典型场景包括：

| 业务场景     | 说明                                                   |
| ------------ | ------------------------------------------------------ |
| 请假审批     | 员工提交请假申请，根据天数、部门、岗位流转至不同审批人 |
| 报销审批     | 根据报销金额进入主管、财务、总经理等不同审批链路       |
| 采购审批     | 根据采购类型、金额、预算状态进行条件分支审批           |
| 合同审批     | 支持法务、财务、业务负责人多角色审批和会签             |
| 用章审批     | 按部门、印章类型、紧急程度控制审批流程                 |
| 项目立项     | 支持项目负责人、部门负责人、PMO、财务联合审核          |
| 数据变更审批 | 对敏感数据修改、权限变更、配置发布增加审批控制         |

适合使用 Flowable 的场景通常具备以下特征：

1. 审批节点会随业务变化调整，不适合完全写死在代码中。
2. 审批流程需要保留完整历史轨迹。
3. 审批人、候选组、条件分支需要动态计算。
4. 业务系统中存在多个类似审批场景，希望复用同一套工作流能力。
5. 需要后续支持流程图展示、流程监控、流程撤回、流程终止等能力。

不适合使用 Flowable 的场景包括：非常简单且长期固定的状态流转、只有两个状态的开关型业务、完全不需要审批记录和流程追踪的轻量功能。此类场景直接使用业务状态字段即可，避免引入工作流引擎造成复杂度上升。

### 技术选型

本项目采用 Spring Boot 3 + Flowable 7.x 的技术组合。Flowable 7.0.0 发布说明中明确提到其面向 Spring Boot 3、Jakarta EE 9/10，并以 Java 17 作为基线；Flowable 7.2.0 于 2025 年 8 月发布，且官方说明后续 Flowable 8 会基于 Spring Boot 4，因此 Spring Boot 3 项目建议锁定 Flowable 7.x 版本线。([Flowable](https://www.flowable.com/blog/releases/flowable-open-source-7-0-0-release))

推荐技术栈如下：

| 技术              | 推荐版本         | 用途                                             |
| ----------------- | ---------------- | ------------------------------------------------ |
| JDK               | 17 或 21         | Spring Boot 3 和 Flowable 7 的运行基础           |
| Spring Boot       | 3.x              | 应用基础框架、自动配置、Web 接口、事务管理       |
| Flowable          | 7.2.0            | BPMN 流程引擎、任务引擎、历史数据管理            |
| Maven             | 3.6.3+           | 项目构建和依赖管理                               |
| MySQL             | 8.0 / 8.4        | 业务数据表和 Flowable 引擎表存储                 |
| MyBatis-Plus      | 3.5.x            | 业务表 CRUD 和扩展查询                           |
| Hutool            | 5.8.x            | 常用工具类封装，例如字符串、集合、日期、对象处理 |
| Lombok            | 1.18.x           | 简化实体类、DTO、VO 编写                         |
| Spring Validation | Spring Boot 内置 | 接口参数校验                                     |
| Springdoc OpenAPI | 2.x              | 接口文档生成                                     |
| Docker            | 可选             | 本地 MySQL、Redis 等环境快速启动                 |

版本选择原则如下：

1. Spring Boot 3 项目必须使用 Java 17 及以上版本，开发环境、编译环境、运行环境应保持一致。([Home](https://docs.spring.io/spring-boot/docs/3.0.x/reference/html/getting-started.html))
2. Flowable 选择 7.x 版本线，不建议在 Spring Boot 3 项目中直接升级到 Flowable 8，除非项目同步升级到 Spring Boot 4。
3. 数据库建议使用 MySQL 8 或 PostgreSQL，生产环境不建议使用 H2。
4. Flowable 表结构由引擎自动维护时，数据库账号需要具备建表、改表等 DDL 权限；Flowable 文档说明其应用启动时可自动创建数据库结构，并要求数据库用户具备 schema 编辑权限。([documentation.flowable.com](https://documentation.flowable.com/latest/admin/installs/system-requirements))

## 环境准备

环境准备部分主要明确本项目运行所需的软件版本、数据库要求、构建工具和基础依赖。由于工作流引擎会自动创建并维护大量运行表、历史表和配置表，因此数据库权限、字符集、时区和版本兼容性需要在开发前统一确认。

### Spring Boot 3 版本要求

Spring Boot 3 基于 Spring Framework 6 和 Jakarta EE 命名空间体系，和 Spring Boot 2 相比存在明显升级差异。最关键的变化是 Java 基线提升到 Java 17，并且常见的 `javax.*` 包迁移为 `jakarta.*` 包。

本项目建议使用以下版本基线：

| 项目             | 要求                                                  |
| ---------------- | ----------------------------------------------------- |
| JDK              | 17+，推荐 JDK 17 LTS 或 JDK 21 LTS                    |
| Spring Boot      | 3.2.x、3.3.x、3.4.x、3.5.x 均可，根据公司依赖基线统一 |
| Spring Framework | 由 Spring Boot 自动管理，不单独指定                   |
| Maven            | 3.6.3+                                                |
| 编码             | UTF-8                                                 |
| 时区             | Asia/Shanghai 或业务系统统一时区                      |

Spring Boot 3.0 官方文档说明该版本要求 Java 17，并提供 Maven 与 Gradle 构建工具支持；后续 Spring Boot 3.x 版本仍延续 Java 17 作为基础要求。([Home](https://docs.spring.io/spring-boot/docs/3.0.x/reference/html/getting-started.html))

开发时需要确认本地 JDK 和 Maven 版本：

```bash
# 查看 Java 版本，要求 17 或以上
java -version

# 查看 Maven 版本，建议 3.6.3 或以上
mvn -v
```

如果本地同时安装多个 JDK，需要确保 IDE、命令行和 Maven 使用的是同一个 JDK 版本。否则可能出现本地能编译、启动时失败，或者 IDE 报 `Unsupported class file major version` 的问题。

### Flowable 版本选择

Flowable 是一套基于 BPMN、CMMN、DMN 标准的流程引擎。本文档重点使用 BPMN 能力，即流程定义、流程实例、用户任务、网关、流程变量和历史轨迹。

Spring Boot 3 项目建议使用 Flowable 7.x。Flowable 7.0.0 发布说明中明确提到该版本支持 Spring Boot 3，并以 Java 17 作为基线；Flowable 7.2.0 是 2025 年发布的 7.x 后续版本，而官方也说明 Flowable 8 会转向 Spring Boot 4、Spring 7 和 Jackson 3。([Flowable](https://www.flowable.com/blog/releases/flowable-open-source-7-0-0-release))

推荐依赖版本：

```xml
<properties>
    <!-- Spring Boot 3 项目建议使用 Java 17 或以上版本 -->
    <java.version>17</java.version>

    <!-- Flowable 7.x 适合 Spring Boot 3 版本线 -->
    <flowable.version>7.2.0</flowable.version>

    <!-- Hutool 工具类，统一处理字符串、集合、日期等常用逻辑 -->
    <hutool.version>5.8.38</hutool.version>
</properties>
```

Flowable Starter 选择建议如下：

| 依赖                                    | 适用场景                                               |
| --------------------------------------- | ------------------------------------------------------ |
| `flowable-spring-boot-starter`          | 引入完整 Flowable 能力，适合学习、统一封装和多引擎场景 |
| `flowable-spring-boot-starter-process`  | 只使用 BPMN 流程能力时更轻量                           |
| `flowable-spring-boot-starter-rest`     | 需要直接暴露 Flowable 官方 REST 接口时使用             |
| `flowable-spring-boot-starter-actuator` | 需要接入 Spring Boot Actuator 监控时使用               |

Flowable 官方 Spring Boot 文档说明，创建 Spring Boot 项目后可以添加 `flowable-spring-boot-starter` 或 `flowable-spring-boot-starter-rest`，如果不需要所有引擎能力，则可以选择其他更细分的 Starter。([Flowable](https://www.flowable.com/open-source/docs/bpmn/ch05a-Spring-Boot/))

推荐在业务系统中优先使用自己的 Controller 和 Service 封装 Flowable API，而不是直接把 Flowable 官方 REST API 暴露给前端。这样更容易接入系统用户、角色、租户、数据权限、审批记录和统一异常处理。

### 数据库准备

Flowable 会创建多类引擎表，包括流程定义表、运行时表、任务表、历史表、变量表、身份表、定时任务表等。开发环境可以让 Flowable 自动建表，测试和生产环境建议根据团队规范决定是否自动建表或使用 SQL 脚本初始化。

推荐数据库配置：

| 项目     | 建议                                   |
| -------- | -------------------------------------- |
| 数据库   | MySQL 8.0+ 或 PostgreSQL 13+           |
| 字符集   | `utf8mb4`                              |
| 排序规则 | `utf8mb4_0900_ai_ci` 或团队统一规则    |
| 时区     | `Asia/Shanghai` 或 `UTC`，需与应用统一 |
| 账号权限 | 开发环境允许 DDL，生产环境按规范收敛   |
| 连接池   | Spring Boot 默认 HikariCP              |
| 事务     | 使用 Spring 声明式事务管理             |

Flowable 文档说明，其产品使用关系型数据库存储数据，并通过 JDBC 连接数据库；数据库也是 Flowable 性能中的重要组成部分，需要提供足够的硬件资源和合理的数据库配置。([documentation.flowable.com](https://documentation.flowable.com/latest/admin/installs/system-requirements))

开发环境可以创建独立数据库：

```sql
-- 创建 Flowable 示例数据库
CREATE DATABASE flowable_boot
    DEFAULT CHARACTER SET utf8mb4
    DEFAULT COLLATE utf8mb4_0900_ai_ci;

-- 创建业务用户，实际密码请替换为安全密码
CREATE USER 'flowable_user'@'%' IDENTIFIED BY 'Flowable@123456';

-- 开发环境授权，允许 Flowable 自动创建和更新引擎表
GRANT ALL PRIVILEGES ON flowable_boot.* TO 'flowable_user'@'%';

-- 刷新权限
FLUSH PRIVILEGES;
```

开发环境推荐先开启 Flowable 自动建表，便于快速启动和验证流程能力。生产环境建议结合数据库变更规范处理，可以采用以下策略之一：

| 策略       | 说明                                                     |
| ---------- | -------------------------------------------------------- |
| 自动建表   | 开发环境使用，启动应用时由 Flowable 自动创建表           |
| 手动初始化 | 测试、预发、生产环境使用 SQL 脚本初始化                  |
| 受控升级   | 生产环境升级 Flowable 版本前，先评估表结构变更和回滚方案 |

需要注意的是，Flowable 引擎表和业务表建议放在同一个数据库实例内，是否放在同一个 schema 根据项目规范决定。中小型单体系统可以放在同一个库中；多租户或平台型系统可以考虑按 schema、租户字段或独立库进行隔离。

### 开发工具与依赖说明

开发工具建议统一，避免不同开发环境导致编译、格式化、运行结果不一致。最低要求是 IDE 支持 Java 17、Maven 和 Spring Boot 3。

推荐开发工具如下：

| 工具                        | 说明                                   |
| --------------------------- | -------------------------------------- |
| IntelliJ IDEA 2024+         | Java 和 Spring Boot 主要开发工具       |
| JDK 17 / 21                 | 项目编译和运行环境                     |
| Maven 3.6.3+                | 依赖管理和项目构建                     |
| MySQL 8.x                   | 本地开发数据库                         |
| DBeaver / DataGrip          | 数据库连接和表结构查看                 |
| Postman / Apifox            | 接口调试                               |
| Git                         | 代码版本管理                           |
| Docker Desktop              | 可选，用于快速启动 MySQL、Redis 等依赖 |
| Flowable Design / BPMN 插件 | 可选，用于绘制 BPMN 流程图             |

基础 Maven 依赖建议如下，后续章节可以在此基础上继续扩展业务模块、权限模块和流程接口。

这段配置放在项目根目录 `pom.xml` 中，用于声明 Spring Boot、Flowable、数据库驱动、MyBatis-Plus、Hutool 和接口文档等基础依赖。

```xml
<properties>
    <!-- Java 版本：Spring Boot 3 和 Flowable 7 推荐使用 Java 17+ -->
    <java.version>17</java.version>

    <!-- Flowable 7.x：适配 Spring Boot 3 版本线 -->
    <flowable.version>7.2.0</flowable.version>

    <!-- MyBatis-Plus：业务表 CRUD 和分页查询 -->
    <mybatis-plus.version>3.5.12</mybatis-plus.version>

    <!-- Hutool：常用工具类 -->
    <hutool.version>5.8.38</hutool.version>

    <!-- Springdoc：OpenAPI 接口文档 -->
    <springdoc.version>2.8.13</springdoc.version>
</properties>

<dependencies>
    <!-- Web 接口开发，包含 Spring MVC 和内置 Tomcat -->
    <dependency>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-starter-web</artifactId>
    </dependency>

    <!-- 参数校验，支持 @Valid、@NotBlank、@NotNull 等注解 -->
    <dependency>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-starter-validation</artifactId>
    </dependency>

    <!-- Flowable 工作流引擎 Starter，提供流程部署、启动、任务、历史等核心能力 -->
    <dependency>
        <groupId>org.flowable</groupId>
        <artifactId>flowable-spring-boot-starter</artifactId>
        <version>${flowable.version}</version>
    </dependency>

    <!-- MySQL JDBC 驱动，用于连接 MySQL 数据库 -->
    <dependency>
        <groupId>com.mysql</groupId>
        <artifactId>mysql-connector-j</artifactId>
        <scope>runtime</scope>
    </dependency>

    <!-- MyBatis-Plus，用于业务表 CRUD 和分页查询 -->
    <dependency>
        <groupId>com.baomidou</groupId>
        <artifactId>mybatis-plus-spring-boot3-starter</artifactId>
        <version>${mybatis-plus.version}</version>
    </dependency>

    <!-- Hutool 工具类，减少重复工具代码 -->
    <dependency>
        <groupId>cn.hutool</groupId>
        <artifactId>hutool-all</artifactId>
        <version>${hutool.version}</version>
    </dependency>

    <!-- Lombok，简化 DTO、VO、Entity 等对象代码 -->
    <dependency>
        <groupId>org.projectlombok</groupId>
        <artifactId>lombok</artifactId>
        <optional>true</optional>
    </dependency>

    <!-- Actuator，用于暴露健康检查和运行状态 -->
    <dependency>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-starter-actuator</artifactId>
    </dependency>

    <!-- OpenAPI 文档，便于调试流程相关接口 -->
    <dependency>
        <groupId>org.springdoc</groupId>
        <artifactId>springdoc-openapi-starter-webmvc-ui</artifactId>
        <version>${springdoc.version}</version>
    </dependency>

    <!-- 单元测试和集成测试 -->
    <dependency>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-starter-test</artifactId>
        <scope>test</scope>
    </dependency>
</dependencies>
```

依赖使用说明：

| 依赖                                  | 作用                                        |
| ------------------------------------- | ------------------------------------------- |
| `spring-boot-starter-web`             | 提供流程管理、审批办理等 REST 接口能力      |
| `flowable-spring-boot-starter`        | 自动装配 Flowable 引擎和核心服务            |
| `mybatis-plus-spring-boot3-starter`   | 管理业务表，例如请假单、审批记录表          |
| `mysql-connector-j`                   | 连接 MySQL 数据库                           |
| `hutool-all`                          | 处理字符串、日期、集合、Bean 转换等通用逻辑 |
| `springdoc-openapi-starter-webmvc-ui` | 生成接口文档，便于测试流程接口              |
| `spring-boot-starter-actuator`        | 提供服务健康检查和基础监控能力              |

基础环境验证命令如下：

```bash
# 编译项目，确认依赖可以正常下载和编译
mvn clean package -DskipTests

# 启动项目
mvn spring-boot:run

# 查看依赖树，排查版本冲突
mvn dependency:tree
```

启动成功后，需要重点检查日志中是否出现 Flowable 引擎初始化信息、数据库连接成功信息以及自动建表信息。Flowable 官方文档示例中也说明，在加入 Starter 后，启动时会创建流程引擎、暴露服务 Bean，并在需要时创建对应数据库表结构。([Flowable](https://www.flowable.com/open-source/docs/bpmn/ch05a-Spring-Boot/))



## 项目初始化

项目初始化阶段主要完成基础工程创建、依赖引入、配置文件编写、Flowable 自动建表策略配置以及目录结构规划。完成本章节后，项目应具备 Spring Boot 正常启动、数据库连接正常、Flowable 引擎自动初始化、BPMN 文件可自动部署的基础能力。

Flowable 的 Spring Boot Starter 会在应用启动时自动创建流程引擎相关 Bean，并将 Flowable 服务暴露为 Spring Bean；默认情况下，放在 `processes` 目录下的 BPMN 2.0 流程定义可以被自动部署。官方文档也列出了 `flowable.check-process-definitions`、`flowable.database-schema-update`、`flowable.history-level`、`flowable.process-definition-location-prefix` 等常用配置项。([Flowable](https://www.flowable.com/open-source/docs/bpmn/ch05a-Spring-Boot/?utm_source=chatgpt.com))

### Maven 依赖配置

Maven 依赖配置用于引入 Spring Boot Web、Flowable 工作流引擎、数据库驱动、MyBatis-Plus、Hutool、Lombok 和接口文档等基础能力。这里建议将 Flowable 版本统一定义在 `properties` 中，避免多模块项目出现版本不一致。

文件位置：`pom.xml`

```xml
<properties>
    <!-- Java 版本：Spring Boot 3 和 Flowable 7 建议使用 Java 17+ -->
    <java.version>17</java.version>

    <!-- Flowable 版本：Spring Boot 3 项目建议使用 Flowable 7.x -->
    <flowable.version>7.2.0</flowable.version>

    <!-- MyBatis-Plus：业务表 CRUD、分页、条件构造器 -->
    <mybatis-plus.version>3.5.12</mybatis-plus.version>

    <!-- Hutool：常用工具类，减少重复工具代码 -->
    <hutool.version>5.8.38</hutool.version>

    <!-- Springdoc：接口文档和调试页面 -->
    <springdoc.version>2.8.13</springdoc.version>
</properties>

<dependencies>
    <!-- Web 接口开发，提供 Controller、REST API、JSON 序列化能力 -->
    <dependency>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-starter-web</artifactId>
    </dependency>

    <!-- 参数校验，支持 @Valid、@NotBlank、@NotNull 等注解 -->
    <dependency>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-starter-validation</artifactId>
    </dependency>

    <!-- Flowable 工作流引擎 Starter，包含 BPMN、CMMN、DMN、IDM 等能力 -->
    <dependency>
        <groupId>org.flowable</groupId>
        <artifactId>flowable-spring-boot-starter</artifactId>
        <version>${flowable.version}</version>
    </dependency>

    <!-- MySQL 数据库驱动，用于连接 MySQL 8.x -->
    <dependency>
        <groupId>com.mysql</groupId>
        <artifactId>mysql-connector-j</artifactId>
        <scope>runtime</scope>
    </dependency>

    <!-- MyBatis-Plus Spring Boot 3 Starter，用于业务表开发 -->
    <dependency>
        <groupId>com.baomidou</groupId>
        <artifactId>mybatis-plus-spring-boot3-starter</artifactId>
        <version>${mybatis-plus.version}</version>
    </dependency>

    <!-- Hutool 工具类，常用于字符串、集合、日期、Bean 等处理 -->
    <dependency>
        <groupId>cn.hutool</groupId>
        <artifactId>hutool-all</artifactId>
        <version>${hutool.version}</version>
    </dependency>

    <!-- Lombok，简化 DTO、VO、Entity、日志对象等样板代码 -->
    <dependency>
        <groupId>org.projectlombok</groupId>
        <artifactId>lombok</artifactId>
        <optional>true</optional>
    </dependency>

    <!-- Actuator，提供健康检查和运行状态监控 -->
    <dependency>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-starter-actuator</artifactId>
    </dependency>

    <!-- OpenAPI 文档，用于查看和调试接口 -->
    <dependency>
        <groupId>org.springdoc</groupId>
        <artifactId>springdoc-openapi-starter-webmvc-ui</artifactId>
        <version>${springdoc.version}</version>
    </dependency>

    <!-- 单元测试和集成测试 -->
    <dependency>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-starter-test</artifactId>
        <scope>test</scope>
    </dependency>
</dependencies>
```

如果项目只使用 BPMN 流程能力，也可以将 `flowable-spring-boot-starter` 替换为 `flowable-spring-boot-starter-process`。Flowable 官方文档列出了多个 Starter，其中 `flowable-spring-boot-starter-process` 用于独立启动流程引擎，`flowable-spring-boot-starter` 则包含 Process、CMMN、DMN 和 IDM 等引擎能力。([Flowable](https://www.flowable.com/open-source/docs/bpmn/ch05a-Spring-Boot/?utm_source=chatgpt.com))

### application 配置

`application.yml` 用于配置应用端口、数据库连接、MyBatis-Plus、Flowable 引擎参数、日志级别和接口文档访问路径。开发环境建议先使用单独的数据库，避免 Flowable 引擎表和其他项目混用导致表结构难以维护。

文件位置：`src/main/resources/application.yml`

```yaml
server:
  # 应用端口
  port: 8080

spring:
  application:
    # 应用名称
    name: springboot-flowable-demo

  datasource:
    # MySQL 连接地址，serverTimezone 需与业务系统时区策略保持一致
    url: jdbc:mysql://127.0.0.1:3306/flowable_boot?useUnicode=true&characterEncoding=utf8&useSSL=false&serverTimezone=Asia/Shanghai&allowPublicKeyRetrieval=true
    # 数据库用户名
    username: flowable_user
    # 数据库密码，生产环境建议通过环境变量或配置中心注入
    password: Flowable@123456
    driver-class-name: com.mysql.cj.jdbc.Driver

    hikari:
      # 连接池名称
      pool-name: FlowableHikariCP
      # 最小空闲连接数
      minimum-idle: 5
      # 最大连接数
      maximum-pool-size: 20
      # 连接超时时间，单位毫秒
      connection-timeout: 30000
      # 空闲连接最大存活时间，单位毫秒
      idle-timeout: 600000
      # 连接最大生命周期，单位毫秒
      max-lifetime: 1800000

mybatis-plus:
  # Mapper XML 文件位置
  mapper-locations: classpath*:/mapper/**/*.xml
  # 实体类包路径
  type-aliases-package: io.github.atengk.flowable.entity
  configuration:
    # 开启下划线转驼峰
    map-underscore-to-camel-case: true
    # 关闭一级缓存跨语句影响，避免复杂审批查询出现脏读认知
    local-cache-scope: statement
  global-config:
    db-config:
      # 主键策略，业务表推荐使用雪花 ID 或数据库自增，按项目规范统一
      id-type: assign_id

flowable:
  # 是否检查并自动部署流程定义，默认会扫描 processes 目录
  check-process-definitions: true

  # Flowable 数据库表结构维护策略，开发环境可使用 true
  database-schema-update: true

  # 开启历史数据记录，便于查询已办任务、审批轨迹和流程审计
  db-history-used: true

  # 历史级别：audit 适合常规审批系统，full 会记录更多变量明细
  history-level: audit

  # 自动部署名称
  deployment-name: SpringBootFlowableAutoDeployment

  # BPMN 文件扫描路径，默认也是 classpath*:/processes/
  process-definition-location-prefix: classpath*:/processes/

  # BPMN 文件后缀
  process-definition-location-suffixes:
    - "**.bpmn20.xml"
    - "**.bpmn"

  # 异步执行器，涉及定时器、异步任务时需要开启
  async-executor-activate: true

springdoc:
  swagger-ui:
    # Swagger UI 访问路径
    path: /swagger-ui.html
  api-docs:
    # OpenAPI JSON 地址
    path: /v3/api-docs

management:
  endpoints:
    web:
      exposure:
        # 暴露健康检查和基础运行信息
        include: health,info

logging:
  level:
    # 项目日志级别
    io.github.atengk: debug
    # Flowable 日志级别，开发阶段可调整为 debug 排查流程流转问题
    org.flowable: info
```

配置完成后，可以通过以下命令启动项目：

```bash
# 编译项目
mvn clean package -DskipTests

# 启动项目
mvn spring-boot:run
```

启动后重点检查三类日志：第一类是数据源连接是否成功；第二类是 Flowable 引擎是否初始化成功；第三类是是否扫描并部署了 `src/main/resources/processes` 目录下的 BPMN 文件。

### Flowable 自动建表配置

Flowable 自动建表配置用于控制应用启动时是否自动创建或更新 Flowable 引擎表。开发环境推荐开启自动建表，生产环境建议关闭自动变更，由数据库脚本或发布流程统一管理表结构。

核心配置如下：

```yaml
flowable:
  # 开发环境：自动检查并更新 Flowable 表结构
  database-schema-update: true
```

常见配置策略如下：

| 配置值        | 说明                                       | 推荐环境           |
| ------------- | ------------------------------------------ | ------------------ |
| `true`        | 启动时自动检查并更新 Flowable 表结构       | 本地开发、测试环境 |
| `false`       | 不自动创建或更新表结构，表不存在时启动失败 | 生产环境           |
| `create-drop` | 启动时建表，关闭时删除表                   | 临时验证、单元测试 |
| `drop-create` | 启动时先删表再建表                         | 本地实验环境       |

开发阶段第一次启动成功后，数据库中会出现多组 Flowable 表。常见表名前缀如下：

| 表前缀      | 说明                                       |
| ----------- | ------------------------------------------ |
| `ACT_RE_*`  | 流程定义、部署资源等静态仓库数据           |
| `ACT_RU_*`  | 运行时流程实例、任务、变量、执行实例等数据 |
| `ACT_HI_*`  | 历史流程实例、历史任务、历史变量等数据     |
| `ACT_GE_*`  | 通用数据，例如字节数组、属性表             |
| `ACT_ID_*`  | Flowable 身份体系相关表                    |
| `ACT_EVT_*` | 事件日志相关表                             |

在实际业务系统中，建议不要直接修改 Flowable 引擎表。业务状态、审批摘要、审批记录应保存到业务表中，Flowable 表只作为流程引擎运行依据和历史查询来源。

生产环境推荐配置如下：

```yaml
flowable:
  # 生产环境不建议自动变更表结构
  database-schema-update: false

  # 保留历史数据，用于审批轨迹和审计
  db-history-used: true

  # 常规审批场景使用 audit 即可
  history-level: audit
```

如果生产环境启动时报 Flowable 表不存在，应先执行与当前 Flowable 版本匹配的建表 SQL，再启动应用。不要直接在生产环境将 `database-schema-update` 临时改为 `true`，否则表结构变更不可控。

### 项目目录结构

项目目录结构应将 Flowable 引擎操作、业务审批逻辑、流程模型资源、接口对象和持久层对象分开，避免后续流程功能膨胀后难以维护。

推荐目录结构如下：

```text
springboot-flowable-demo
├── pom.xml
├── src
│   ├── main
│   │   ├── java
│   │   │   └── io
│   │   │       └── github
│   │   │           └── atengk
│   │   │               └── flowable
│   │   │                   ├── FlowableApplication.java
│   │   │                   ├── common
│   │   │                   │   ├── result
│   │   │                   │   └── exception
│   │   │                   ├── config
│   │   │                   │   └── FlowableConfig.java
│   │   │                   ├── controller
│   │   │                   │   ├── FlowDefinitionController.java
│   │   │                   │   ├── FlowInstanceController.java
│   │   │                   │   └── FlowTaskController.java
│   │   │                   ├── service
│   │   │                   │   ├── FlowDefinitionService.java
│   │   │                   │   ├── FlowInstanceService.java
│   │   │                   │   ├── FlowTaskService.java
│   │   │                   │   └── impl
│   │   │                   ├── mapper
│   │   │                   │   ├── FlowBusinessMapper.java
│   │   │                   │   └── FlowApprovalRecordMapper.java
│   │   │                   ├── entity
│   │   │                   │   ├── FlowBusinessEntity.java
│   │   │                   │   └── FlowApprovalRecordEntity.java
│   │   │                   ├── dto
│   │   │                   │   ├── StartProcessDTO.java
│   │   │                   │   └── CompleteTaskDTO.java
│   │   │                   ├── vo
│   │   │                   │   ├── TaskTodoVO.java
│   │   │                   │   └── ProcessInstanceVO.java
│   │   │                   └── listener
│   │   │                       ├── LeaveTaskListener.java
│   │   │                       └── ProcessEndListener.java
│   │   └── resources
│   │       ├── application.yml
│   │       ├── mapper
│   │       │   └── flowable
│   │       └── processes
│   │           └── leave-approve.bpmn20.xml
│   └── test
│       └── java
│           └── io
│               └── github
│                   └── atengk
│                       └── flowable
│                           └── FlowableApplicationTests.java
```

目录职责说明如下：

| 目录                  | 说明                                       |
| --------------------- | ------------------------------------------ |
| `controller`          | 对外提供流程定义、流程实例、任务办理等接口 |
| `service`             | 封装 Flowable API 和业务审批逻辑           |
| `mapper`              | 操作业务审批表，不直接操作 Flowable 引擎表 |
| `entity`              | 业务表实体，例如审批记录、业务流程关联表   |
| `dto`                 | 接口请求对象，例如启动流程、审批任务       |
| `vo`                  | 接口响应对象，例如待办任务、流程进度       |
| `listener`            | Flowable 执行监听器、任务监听器            |
| `resources/processes` | 存放 BPMN 流程定义文件                     |
| `resources/mapper`    | MyBatis-Plus 自定义 XML SQL                |

主启动类如下，用于启动 Spring Boot 应用并触发 Flowable 自动配置。

文件位置：`src/main/java/io/github/atengk/flowable/FlowableApplication.java`

```java
package io.github.atengk.flowable;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * Spring Boot 集成 Flowable 工作流启动类。
 *
 * @author Ateng
 * @since 2026-05-08
 */
@SpringBootApplication
public class FlowableApplication {

    /**
     * 应用启动入口。
     *
     * @param args 启动参数
     */
    public static void main(String[] args) {
        SpringApplication.run(FlowableApplication.class, args);
    }

}
```

启动类不需要手动创建 `ProcessEngine`。在引入 Flowable Starter 后，Spring Boot 自动配置会完成流程引擎初始化，并将 `RepositoryService`、`RuntimeService`、`TaskService`、`HistoryService` 等服务注册为 Spring Bean。Flowable 官方文档说明，加入 Starter 后会自动创建流程引擎、暴露 Flowable 服务 Bean，并自动部署指定目录中的流程定义。([Flowable](https://www.flowable.com/open-source/docs/bpmn/ch05a-Spring-Boot?utm_source=chatgpt.com))

## Flowable 基础概念

Flowable 基础概念用于理解后续流程开发中反复出现的核心对象。掌握这些概念后，才能清楚地区分“流程模板”“正在运行的流程”“当前待办任务”“候选人”“办理人”和“流程变量”等对象之间的关系。

在常规审批系统中，可以简单理解为：流程定义是审批模板，流程实例是某一次具体审批，任务节点是当前需要人处理的审批动作，候选人和办理人决定任务由谁处理，流程变量则决定流程如何分支和流转。

### 流程定义

流程定义是 BPMN 文件部署到 Flowable 后生成的流程模板。它描述了流程从开始到结束的完整结构，包括开始事件、用户任务、网关、连线、结束事件、监听器、候选人和条件表达式等内容。

例如，一个请假审批流程可以定义为：

```text
开始 → 提交申请 → 部门经理审批 → 人事备案 → 结束
```

流程定义的核心信息通常包括：

| 字段           | 说明                                                         |
| -------------- | ------------------------------------------------------------ |
| `id`           | 流程定义 ID，由 Flowable 生成，通常包含 key、版本号和内部 ID |
| `key`          | 流程定义 Key，业务启动流程时通常使用该字段                   |
| `name`         | 流程名称，例如“请假审批流程”                                 |
| `version`      | 流程版本，每次重新部署相同 key 的流程通常会生成新版本        |
| `deploymentId` | 部署 ID，用于关联一次部署记录                                |
| `resourceName` | BPMN 资源文件名称                                            |

示例 BPMN 文件如下，用于定义一个最简单的单节点审批流程。

文件位置：`src/main/resources/processes/leave-approve.bpmn20.xml`

```xml
<?xml version="1.0" encoding="UTF-8"?>
<definitions
        xmlns="http://www.omg.org/spec/BPMN/20100524/MODEL"
        xmlns:flowable="http://flowable.org/bpmn"
        targetNamespace="Examples">

    <!-- 流程定义：id 是启动流程时常用的流程 key -->
    <process id="leaveApprove" name="请假审批流程" isExecutable="true">

        <!-- 开始节点 -->
        <startEvent id="startEvent" name="开始"/>

        <!-- 连线：开始节点到用户任务 -->
        <sequenceFlow id="flow_start_manager" sourceRef="startEvent" targetRef="managerApprove"/>

        <!-- 用户任务：部门经理审批，assignee 从流程变量 managerUserId 中读取 -->
        <userTask id="managerApprove"
                  name="部门经理审批"
                  flowable:assignee="${managerUserId}"/>

        <!-- 连线：用户任务到结束节点 -->
        <sequenceFlow id="flow_manager_end" sourceRef="managerApprove" targetRef="endEvent"/>

        <!-- 结束节点 -->
        <endEvent id="endEvent" name="结束"/>

    </process>
</definitions>
```

部署后，业务系统一般通过 `RepositoryService` 查询流程定义，通过 `RuntimeService` 按流程定义 Key 启动流程实例。

### 流程实例

流程实例是流程定义的一次具体运行。流程定义类似“模板”，流程实例类似“某一张具体单据发起的审批”。

例如，`leaveApprove` 是请假审批流程定义；员工张三提交了一张请假单后，系统启动一条流程实例，这条实例只对应张三这一次请假审批。

流程实例常用信息如下：

| 字段                  | 说明                               |
| --------------------- | ---------------------------------- |
| `processInstanceId`   | 流程实例 ID，Flowable 生成         |
| `processDefinitionId` | 所属流程定义 ID                    |
| `businessKey`         | 业务主键，通常保存业务单据 ID      |
| `startUserId`         | 发起人 ID                          |
| `startTime`           | 流程启动时间                       |
| `endTime`             | 流程结束时间，运行中流程为空       |
| `variables`           | 流程变量，例如审批人、金额、状态等 |

启动流程时，建议传入 `businessKey`，将流程实例和业务表主键绑定：

```java
runtimeService.startProcessInstanceByKey(
        "leaveApprove",
        businessId,
        variables
);
```

其中：

| 参数           | 说明                                                       |
| -------------- | ---------------------------------------------------------- |
| `leaveApprove` | 流程定义 Key，对应 BPMN 中的 `<process id="leaveApprove">` |
| `businessId`   | 业务主键，例如请假单 ID                                    |
| `variables`    | 流程变量，例如申请人、部门经理、请假天数                   |

业务系统不建议只依赖 Flowable 表保存业务状态。更稳妥的做法是：业务表保存审批状态，Flowable 保存流程运行状态，二者通过 `businessKey` 或业务流程关联表建立关系。

### 任务节点

任务节点是流程实例运行过程中需要处理的具体工作项。审批系统中最常用的是用户任务，即 BPMN 中的 `userTask`。

例如流程运行到“部门经理审批”时，Flowable 会生成一条运行时任务。该任务可以被指定办理人查询到，并在审批通过或驳回后完成。

用户任务常用字段如下：

| 字段                  | 说明                                |
| --------------------- | ----------------------------------- |
| `taskId`              | 任务 ID，办理任务时必须使用         |
| `name`                | 任务名称，例如“部门经理审批”        |
| `assignee`            | 当前办理人                          |
| `owner`               | 任务拥有者，委派场景常用            |
| `processInstanceId`   | 所属流程实例 ID                     |
| `processDefinitionId` | 所属流程定义 ID                     |
| `taskDefinitionKey`   | BPMN 节点 ID，例如 `managerApprove` |
| `createTime`          | 任务创建时间                        |

待办任务查询通常使用 `TaskService`：

```java
List<Task> taskList = taskService.createTaskQuery()
        .taskAssignee(userId)
        .orderByTaskCreateTime()
        .desc()
        .list();
```

完成任务通常使用：

```java
taskService.complete(taskId, variables);
```

其中 `variables` 可以传入审批结果、审批意见、下一节点审批人等信息。对于带条件网关的流程，流程变量会直接影响流程走向。

### 候选人与办理人

候选人和办理人用于控制任务由谁处理。二者区别非常重要，直接影响待办查询和任务领取逻辑。

| 概念                    | 说明                                         |
| ----------------------- | -------------------------------------------- |
| 办理人 `assignee`       | 明确指定任务当前由某一个用户处理             |
| 候选人 `candidateUser`  | 多个候选用户都有资格处理任务，但需要先领取   |
| 候选组 `candidateGroup` | 某个角色、岗位、部门下的用户都有资格处理任务 |
| 领取 `claim`            | 候选任务被某个用户领取后，该用户成为办理人   |
| 归还 `unclaim`          | 办理人释放任务，使任务重新回到候选状态       |

直接指定办理人的 BPMN 示例：

```xml
<!-- 当前任务直接分配给 managerUserId 对应的用户 -->
<userTask id="managerApprove"
          name="部门经理审批"
          flowable:assignee="${managerUserId}"/>
```

配置候选组的 BPMN 示例：

```xml
<!-- 当前任务分配给部门经理角色，具体用户需要先领取任务 -->
<userTask id="managerApprove"
          name="部门经理审批"
          flowable:candidateGroups="dept_manager"/>
```

候选任务查询示例：

```java
List<Task> candidateTasks = taskService.createTaskQuery()
        .taskCandidateUser(userId)
        .orderByTaskCreateTime()
        .desc()
        .list();
```

任务领取示例：

```java
taskService.claim(taskId, userId);
```

实际项目中，`candidateGroups` 不建议直接使用中文角色名，建议使用稳定编码，例如 `dept_manager`、`finance_manager`、`hr_admin`。前端展示时再根据系统角色表转换为中文名称。

### 流程变量

流程变量是流程运行过程中的上下文数据，用于传递审批人、业务参数、分支条件和临时状态。Flowable 中的流程变量可以在流程实例、任务节点、表达式、监听器和服务任务中使用。

常见流程变量包括：

| 变量名           | 示例值      | 说明           |
| ---------------- | ----------- | -------------- |
| `applyUserId`    | `10001`     | 申请人 ID      |
| `managerUserId`  | `10002`     | 部门经理 ID    |
| `leaveDays`      | `3`         | 请假天数       |
| `approved`       | `true`      | 审批是否通过   |
| `approveComment` | `同意`      | 审批意见       |
| `businessStatus` | `APPROVING` | 业务审批状态   |
| `nextUserId`     | `10003`     | 下一节点审批人 |

启动流程时设置变量：

```java
Map<String, Object> variables = new HashMap<>();
variables.put("applyUserId", applyUserId);
variables.put("managerUserId", managerUserId);
variables.put("leaveDays", leaveDays);

runtimeService.startProcessInstanceByKey("leaveApprove", businessId, variables);
```

完成任务时设置变量：

```java
Map<String, Object> variables = new HashMap<>();
variables.put("approved", true);
variables.put("approveComment", "同意");

taskService.complete(taskId, variables);
```

在 BPMN 条件表达式中使用变量：

```xml
<!-- 请假天数小于等于 3 天，走部门经理审批 -->
<sequenceFlow id="flow_manager"
              sourceRef="exclusiveGateway"
              targetRef="managerApprove">
    <conditionExpression xsi:type="tFormalExpression">
        ${leaveDays <= 3}
    </conditionExpression>
</sequenceFlow>

<!-- 请假天数大于 3 天，走总经理审批 -->
<sequenceFlow id="flow_boss"
              sourceRef="exclusiveGateway"
              targetRef="bossApprove">
    <conditionExpression xsi:type="tFormalExpression">
        ${leaveDays > 3}
    </conditionExpression>
</sequenceFlow>
```

流程变量使用建议：

| 建议                 | 说明                                                       |
| -------------------- | ---------------------------------------------------------- |
| 变量名保持稳定       | BPMN 表达式依赖变量名，随意修改会导致流程异常              |
| 不存大对象           | 流程变量建议保存简单类型、ID、编码，不建议保存复杂业务对象 |
| 关键业务状态落业务表 | Flowable 变量用于流转判断，业务表才是业务状态主数据        |
| 审批意见单独入表     | 不建议只依赖变量保存审批意见，应写入审批记录表             |
| 分支变量提前校验     | 启动流程或完成任务前校验变量是否完整，避免表达式异常       |

在后续核心功能开发中，流程变量会贯穿流程启动、任务审批、条件网关、监听器、审批记录和流程图展示等功能。建议在项目中统一封装变量名称常量，避免字符串散落在 Controller、Service 和 BPMN 文件中。



## 流程建模

流程建模用于将业务审批规则转换为 BPMN 流程定义文件。Flowable 执行流程时，并不是直接读取 Java 代码中的审批逻辑，而是读取 BPMN 文件中定义的节点、连线、网关、审批人、候选组和条件表达式。

在实际项目中，流程建模应重点关注四类内容：流程节点是否完整、节点 ID 是否稳定、审批人来源是否可配置、条件表达式变量是否和后端代码保持一致。BPMN 文件一旦投入生产使用，节点 ID、流程 Key 和变量名称不建议随意修改，否则可能影响运行中的流程实例。

### BPMN 文件结构

BPMN 文件本质是一个 XML 文件，通常放在 `src/main/resources/processes` 目录下。Flowable 启动时会根据 `flowable.process-definition-location-prefix` 配置自动扫描并部署这些文件。

一个基础 BPMN 文件通常包含以下内容：

| 组成部分              | 说明                                         |
| --------------------- | -------------------------------------------- |
| `definitions`         | BPMN XML 根节点，声明命名空间                |
| `process`             | 流程定义主体，`id` 通常作为流程定义 Key 使用 |
| `startEvent`          | 开始事件                                     |
| `userTask`            | 用户任务，表示需要人工审批                   |
| `exclusiveGateway`    | 排他网关，根据条件选择一条分支               |
| `sequenceFlow`        | 流程连线，连接不同节点                       |
| `conditionExpression` | 条件表达式，用于控制网关流向                 |
| `endEvent`            | 结束事件                                     |

下面是一个带审批分支的请假流程示例。流程规则为：员工提交请假申请后，如果请假天数小于等于 3 天，则部门经理审批后结束；如果请假天数大于 3 天，则部门经理审批后继续流转到总经理审批。

文件位置：`src/main/resources/processes/leave-approve.bpmn20.xml`

```xml
<?xml version="1.0" encoding="UTF-8"?>
<definitions
        xmlns="http://www.omg.org/spec/BPMN/20100524/MODEL"
        xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
        xmlns:flowable="http://flowable.org/bpmn"
        targetNamespace="Examples">

    <!--
        流程定义：
        id：流程定义 Key，后端启动流程时使用
        name：流程名称，用于管理端展示
        isExecutable：必须为 true，否则 Flowable 不会作为可执行流程处理
    -->
    <process id="leaveApprove" name="请假审批流程" isExecutable="true">

        <!-- 开始事件 -->
        <startEvent id="startEvent" name="开始"/>

        <!-- 提交申请后进入部门经理审批 -->
        <sequenceFlow id="flow_start_manager"
                      sourceRef="startEvent"
                      targetRef="managerApprove"/>

        <!-- 部门经理审批，办理人来自流程变量 managerUserId -->
        <userTask id="managerApprove"
                  name="部门经理审批"
                  flowable:assignee="${managerUserId}"/>

        <!-- 部门经理审批完成后进入请假天数判断网关 -->
        <sequenceFlow id="flow_manager_gateway"
                      sourceRef="managerApprove"
                      targetRef="leaveDaysGateway"/>

        <!-- 排他网关：根据 leaveDays 判断是否需要总经理审批 -->
        <exclusiveGateway id="leaveDaysGateway" name="请假天数判断"/>

        <!-- 请假天数小于等于 3 天，直接结束 -->
        <sequenceFlow id="flow_short_leave"
                      sourceRef="leaveDaysGateway"
                      targetRef="endEvent">
            <conditionExpression xsi:type="tFormalExpression">
                <![CDATA[${leaveDays <= 3}]]>
            </conditionExpression>
        </sequenceFlow>

        <!-- 请假天数大于 3 天，进入总经理审批 -->
        <sequenceFlow id="flow_long_leave"
                      sourceRef="leaveDaysGateway"
                      targetRef="bossApprove">
            <conditionExpression xsi:type="tFormalExpression">
                <![CDATA[${leaveDays > 3}]]>
            </conditionExpression>
        </sequenceFlow>

        <!-- 总经理审批，办理人来自流程变量 bossUserId -->
        <userTask id="bossApprove"
                  name="总经理审批"
                  flowable:assignee="${bossUserId}"/>

        <!-- 总经理审批完成后结束 -->
        <sequenceFlow id="flow_boss_end"
                      sourceRef="bossApprove"
                      targetRef="endEvent"/>

        <!-- 结束事件 -->
        <endEvent id="endEvent" name="结束"/>

    </process>
</definitions>
```

建模时建议遵守以下规则：

| 规则                  | 说明                                    |
| --------------------- | --------------------------------------- |
| 流程 Key 使用英文编码 | 例如 `leaveApprove`、`expenseApprove`   |
| 节点 ID 保持稳定      | 运行中流程实例依赖节点 ID，避免频繁修改 |
| 节点名称使用中文      | 便于前端展示和审批记录阅读              |
| 变量名称统一管理      | 后端常量、BPMN 表达式、接口参数保持一致 |
| 表达式使用 CDATA      | 避免 XML 特殊字符导致解析异常           |
| 流程文件按业务命名    | 例如 `leave-approve.bpmn20.xml`         |

### 用户任务配置

用户任务是审批系统中最常用的节点类型，表示流程运行到该节点时需要人工处理。用户任务可以配置固定办理人、动态办理人、候选人、候选组、监听器和到期时间等属性。

常见配置方式如下：

```xml
<!-- 方式一：固定办理人，不推荐用于正式业务，只适合测试 -->
<userTask id="managerApprove"
          name="部门经理审批"
          flowable:assignee="10001"/>

<!-- 方式二：动态办理人，推荐使用 -->
<userTask id="managerApprove"
          name="部门经理审批"
          flowable:assignee="${managerUserId}"/>

<!-- 方式三：候选人，多个用户都可以领取该任务 -->
<userTask id="financeApprove"
          name="财务审批"
          flowable:candidateUsers="${financeUserIds}"/>

<!-- 方式四：候选组，角色或岗位下的用户可以领取该任务 -->
<userTask id="hrApprove"
          name="人事审批"
          flowable:candidateGroups="hr_admin"/>
```

配置方式选择建议如下：

| 配置方式          | 适用场景                             |
| ----------------- | ------------------------------------ |
| `assignee`        | 明确知道下一节点审批人，例如直属上级 |
| `candidateUsers`  | 多个具体用户都有资格审批             |
| `candidateGroups` | 某个角色、岗位、部门有资格审批       |
| 任务监听器        | 审批人需要通过复杂业务规则动态计算   |
| 表达式            | 审批人可以从流程变量中直接获取       |

在 Spring Boot 项目中，推荐将审批人计算逻辑放在业务代码中完成，然后通过流程变量传入 BPMN。这样 BPMN 文件只描述流程结构，不承载过多复杂业务判断。

例如启动流程时传入：

```java
Map<String, Object> variables = new HashMap<>();
variables.put("managerUserId", "10001");
variables.put("bossUserId", "10002");
variables.put("leaveDays", 5);

runtimeService.startProcessInstanceByKey("leaveApprove", businessId, variables);
```

对应 BPMN 中使用：

```xml
<userTask id="managerApprove"
          name="部门经理审批"
          flowable:assignee="${managerUserId}"/>
```

如果审批人来自候选组，建议候选组使用系统角色编码，例如：

```xml
<userTask id="financeApprove"
          name="财务审批"
          flowable:candidateGroups="finance_manager"/>
```

不要直接使用“财务经理”“部门主管”等中文名称作为候选组编码。中文名称适合展示，编码适合流程流转和权限匹配。

### 网关配置

网关用于控制流程分支。审批系统中最常用的是排他网关，即 `exclusiveGateway`。排他网关会根据条件表达式选择一条满足条件的流转路径。

常见网关类型如下：

| 网关类型 | BPMN 节点           | 说明                           |
| -------- | ------------------- | ------------------------------ |
| 排他网关 | `exclusiveGateway`  | 多选一，只走一条满足条件的分支 |
| 并行网关 | `parallelGateway`   | 同时走多条分支，适合并行审批   |
| 包容网关 | `inclusiveGateway`  | 可以走一条或多条满足条件的分支 |
| 事件网关 | `eventBasedGateway` | 根据事件触发结果选择分支       |

排他网关示例：

```xml
<!-- 报销金额判断网关 -->
<exclusiveGateway id="amountGateway" name="报销金额判断"/>

<!-- 金额小于等于 5000，部门经理审批后结束 -->
<sequenceFlow id="flow_normal_amount"
              sourceRef="amountGateway"
              targetRef="endEvent">
    <conditionExpression xsi:type="tFormalExpression">
        <![CDATA[${amount <= 5000}]]>
    </conditionExpression>
</sequenceFlow>

<!-- 金额大于 5000，继续进入财务审批 -->
<sequenceFlow id="flow_large_amount"
              sourceRef="amountGateway"
              targetRef="financeApprove">
    <conditionExpression xsi:type="tFormalExpression">
        <![CDATA[${amount > 5000}]]>
    </conditionExpression>
</sequenceFlow>
```

并行网关适合多个节点同时审批，例如法务和财务并行审批：

```xml
<!-- 并行拆分 -->
<parallelGateway id="parallelStart" name="并行审批开始"/>

<sequenceFlow id="flow_to_legal"
              sourceRef="parallelStart"
              targetRef="legalApprove"/>

<sequenceFlow id="flow_to_finance"
              sourceRef="parallelStart"
              targetRef="financeApprove"/>

<!-- 法务审批 -->
<userTask id="legalApprove"
          name="法务审批"
          flowable:candidateGroups="legal_manager"/>

<!-- 财务审批 -->
<userTask id="financeApprove"
          name="财务审批"
          flowable:candidateGroups="finance_manager"/>

<!-- 并行汇聚 -->
<parallelGateway id="parallelEnd" name="并行审批结束"/>
```

网关配置注意事项：

| 注意事项               | 说明                                       |
| ---------------------- | ------------------------------------------ |
| 排他网关分支条件要互斥 | 避免多个条件同时成立导致流程结果不符合预期 |
| 建议设置默认流转       | 避免所有条件都不满足时流程异常             |
| 条件变量提前校验       | 后端启动或审批前确认变量存在               |
| 并行网关必须成对使用   | 拆分和汇聚要匹配，否则流程可能无法结束     |
| 网关不要写复杂业务逻辑 | 复杂逻辑应在后端计算后写入简单变量         |

### 条件表达式配置

条件表达式用于根据流程变量控制流程走向。Flowable 常用 UEL 表达式，例如 `${approved == true}`、`${amount > 5000}`、`${leaveDays <= 3}`。

常见表达式示例：

```xml
<!-- 布尔判断 -->
<conditionExpression xsi:type="tFormalExpression">
    <![CDATA[${approved == true}]]>
</conditionExpression>

<!-- 数值比较 -->
<conditionExpression xsi:type="tFormalExpression">
    <![CDATA[${amount > 5000}]]>
</conditionExpression>

<!-- 字符串比较 -->
<conditionExpression xsi:type="tFormalExpression">
    <![CDATA[${approveResult == 'PASS'}]]>
</conditionExpression>

<!-- 多条件判断 -->
<conditionExpression xsi:type="tFormalExpression">
    <![CDATA[${amount > 5000 && urgent == true}]]>
</conditionExpression>
```

为了降低表达式错误，建议后端统一定义变量常量。

文件位置：`src/main/java/io/github/atengk/flowable/common/constant/FlowVariableConstants.java`

```java
package io.github.atengk.flowable.common.constant;

/**
 * 流程变量常量。
 *
 * @author Ateng
 * @since 2026-05-08
 */
public final class FlowVariableConstants {

    /**
     * 申请人 ID。
     */
    public static final String APPLY_USER_ID = "applyUserId";

    /**
     * 部门经理用户 ID。
     */
    public static final String MANAGER_USER_ID = "managerUserId";

    /**
     * 总经理用户 ID。
     */
    public static final String BOSS_USER_ID = "bossUserId";

    /**
     * 请假天数。
     */
    public static final String LEAVE_DAYS = "leaveDays";

    /**
     * 审批结果。
     */
    public static final String APPROVE_RESULT = "approveResult";

    /**
     * 审批意见。
     */
    public static final String APPROVE_COMMENT = "approveComment";

    private FlowVariableConstants() {
    }

}
```

使用变量常量后，后端代码、BPMN 表达式和接口文档应保持一致。例如 BPMN 中使用 `${leaveDays > 3}`，后端启动流程时必须传入 `leaveDays`，否则流程运行到网关时会出现表达式解析异常。

条件表达式使用建议：

| 建议                  | 说明                                   |
| --------------------- | -------------------------------------- |
| 使用简单变量          | 表达式中只做判断，不做复杂计算         |
| 使用稳定变量名        | 变量名修改需要同步 BPMN、后端、前端    |
| 数值类型保持一致      | 避免字符串 `"5000"` 和数字 `5000` 混用 |
| 使用 CDATA 包裹表达式 | 避免 `<`、`>`、`&&` 影响 XML 解析      |
| 后端提前校验变量      | 启动流程和完成任务前检查关键变量       |

### 流程部署方式

流程部署是将 BPMN 文件发布到 Flowable 引擎中的过程。部署后，Flowable 会生成流程定义，后续才能通过流程定义 Key 启动流程实例。

常见部署方式如下：

| 部署方式       | 说明                                      | 适用场景             |
| -------------- | ----------------------------------------- | -------------------- |
| 自动部署       | 应用启动时扫描 `resources/processes` 目录 | 固定流程、开发测试   |
| 手动部署       | 通过接口上传 BPMN 文件部署                | 后台流程管理         |
| Classpath 部署 | 通过代码读取 classpath 文件部署           | 初始化脚本、单元测试 |
| 字符串部署     | 将 BPMN XML 字符串直接部署                | 在线流程设计器       |
| 租户部署       | 部署时指定 tenantId                       | 多租户系统           |

自动部署依赖配置如下：

```yaml
flowable:
  # 开启流程定义检查和自动部署
  check-process-definitions: true

  # 扫描 BPMN 文件目录
  process-definition-location-prefix: classpath*:/processes/

  # 支持的流程文件后缀
  process-definition-location-suffixes:
    - "**.bpmn20.xml"
    - "**.bpmn"
```

手动部署通常用于流程管理后台，管理员上传 BPMN 文件后，系统调用 `RepositoryService` 完成部署。手动部署代码会在“核心功能开发”中的“流程部署”小节给出。

## 核心功能开发

核心功能开发用于封装 Flowable 的常用 API，包括流程部署、流程启动、待办任务查询、已办任务查询、任务审批、流程驳回、流程终止和流程撤回。实际项目中不建议 Controller 直接调用 Flowable 原生 Service，应通过业务 Service 封装统一逻辑、参数校验、异常处理、日志和审批记录保存。

本章节先给出一套可复用的基础代码骨架，后续可以在此基础上继续接入业务表、审批记录表、权限系统和流程图展示。

### 流程部署

流程部署用于将 BPMN 文件发布到 Flowable 引擎中。部署成功后，Flowable 会生成流程定义，后续可以通过流程定义 Key 启动流程实例。

文件位置：`src/main/java/io/github/atengk/flowable/service/FlowDefinitionService.java`

```java
package io.github.atengk.flowable.service;

import org.springframework.web.multipart.MultipartFile;

/**
 * 流程定义服务。
 *
 * @author Ateng
 * @since 2026-05-08
 */
public interface FlowDefinitionService {

    /**
     * 上传并部署 BPMN 文件。
     *
     * @param file BPMN 文件
     * @param deploymentName 部署名称
     * @return 部署 ID
     */
    String deployByFile(MultipartFile file, String deploymentName);

}
```

下面的实现类用于校验并部署 BPMN 文件，支持 `.bpmn` 和 `.bpmn20.xml` 两类常见后缀。

文件位置：`src/main/java/io/github/atengk/flowable/service/impl/FlowDefinitionServiceImpl.java`

```java
package io.github.atengk.flowable.service.impl;

import cn.hutool.core.util.StrUtil;
import io.github.atengk.flowable.service.FlowDefinitionService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.flowable.engine.RepositoryService;
import org.flowable.engine.repository.Deployment;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;

/**
 * 流程定义服务实现。
 *
 * @author Ateng
 * @since 2026-05-08
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class FlowDefinitionServiceImpl implements FlowDefinitionService {

    private final RepositoryService repositoryService;

    /**
     * 上传并部署 BPMN 文件。
     *
     * @param file BPMN 文件
     * @param deploymentName 部署名称
     * @return 部署 ID
     */
    @Override
    public String deployByFile(MultipartFile file, String deploymentName) {
        if (file == null || file.isEmpty()) {
            throw new IllegalArgumentException("BPMN 文件不能为空");
        }

        String originalFilename = file.getOriginalFilename();
        if (StrUtil.isBlank(originalFilename)) {
            throw new IllegalArgumentException("BPMN 文件名称不能为空");
        }

        boolean validFile = StrUtil.endWithIgnoreCase(originalFilename, ".bpmn")
                || StrUtil.endWithIgnoreCase(originalFilename, ".bpmn20.xml");
        if (!validFile) {
            throw new IllegalArgumentException("仅支持 .bpmn 或 .bpmn20.xml 文件");
        }

        String name = StrUtil.blankToDefault(deploymentName, originalFilename);

        try (InputStream inputStream = file.getInputStream()) {
            Deployment deployment = repositoryService.createDeployment()
                    .name(name)
                    .addInputStream(originalFilename, inputStream)
                    .deploy();

            log.info("流程部署成功，部署ID：{}，部署名称：{}，文件名称：{}",
                    deployment.getId(), deployment.getName(), originalFilename);
            return deployment.getId();
        } catch (IOException e) {
            log.error("读取 BPMN 文件失败，文件名称：{}", originalFilename, e);
            throw new IllegalStateException("读取 BPMN 文件失败");
        }
    }

}
```

接口示例：

```http
POST /flow/definition/deploy
Content-Type: multipart/form-data

file: leave-approve.bpmn20.xml
deploymentName: 请假审批流程
```

部署成功后，可以在 `ACT_RE_DEPLOYMENT`、`ACT_RE_PROCDEF` 等表中查看部署记录和流程定义记录。

### 流程启动

流程启动用于基于某个流程定义 Key 创建一条流程实例。启动时建议传入 `businessKey`，用于关联业务表主键，例如请假单 ID、报销单 ID、合同 ID。

文件位置：`src/main/java/io/github/atengk/flowable/dto/StartProcessDTO.java`

```java
package io.github.atengk.flowable.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

import java.util.Map;

/**
 * 启动流程请求参数。
 *
 * @author Ateng
 * @since 2026-05-08
 */
@Data
public class StartProcessDTO {

    /**
     * 流程定义 Key。
     */
    @NotBlank(message = "流程定义 Key 不能为空")
    private String processDefinitionKey;

    /**
     * 业务主键。
     */
    @NotBlank(message = "业务主键不能为空")
    private String businessKey;

    /**
     * 发起人用户 ID。
     */
    @NotBlank(message = "发起人用户 ID 不能为空")
    private String startUserId;

    /**
     * 流程变量。
     */
    private Map<String, Object> variables;

}
```

文件位置：`src/main/java/io/github/atengk/flowable/service/FlowInstanceService.java`

```java
package io.github.atengk.flowable.service;

import io.github.atengk.flowable.dto.StartProcessDTO;

/**
 * 流程实例服务。
 *
 * @author Ateng
 * @since 2026-05-08
 */
public interface FlowInstanceService {

    /**
     * 启动流程实例。
     *
     * @param dto 启动流程请求参数
     * @return 流程实例 ID
     */
    String startProcess(StartProcessDTO dto);

    /**
     * 终止流程实例。
     *
     * @param processInstanceId 流程实例 ID
     * @param reason 终止原因
     */
    void terminateProcess(String processInstanceId, String reason);

    /**
     * 撤回流程实例到指定节点。
     *
     * @param processInstanceId 流程实例 ID
     * @param targetActivityId 目标节点 ID
     * @param reason 撤回原因
     */
    void withdrawProcess(String processInstanceId, String targetActivityId, String reason);

}
```

下面的实现类用于启动、终止和撤回流程实例。启动流程时会设置发起人，并将发起人 ID 放入流程变量中。

文件位置：`src/main/java/io/github/atengk/flowable/service/impl/FlowInstanceServiceImpl.java`

```java
package io.github.atengk.flowable.service.impl;

import cn.hutool.core.map.MapUtil;
import cn.hutool.core.util.StrUtil;
import io.github.atengk.flowable.common.constant.FlowVariableConstants;
import io.github.atengk.flowable.dto.StartProcessDTO;
import io.github.atengk.flowable.service.FlowInstanceService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.flowable.engine.HistoryService;
import org.flowable.engine.IdentityService;
import org.flowable.engine.RuntimeService;
import org.flowable.engine.history.HistoricActivityInstance;
import org.flowable.engine.runtime.ProcessInstance;
import org.flowable.task.api.Task;
import org.flowable.engine.TaskService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 流程实例服务实现。
 *
 * @author Ateng
 * @since 2026-05-08
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class FlowInstanceServiceImpl implements FlowInstanceService {

    private final RuntimeService runtimeService;
    private final IdentityService identityService;
    private final TaskService taskService;
    private final HistoryService historyService;

    /**
     * 启动流程实例。
     *
     * @param dto 启动流程请求参数
     * @return 流程实例 ID
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public String startProcess(StartProcessDTO dto) {
        Map<String, Object> variables = new HashMap<>();
        if (MapUtil.isNotEmpty(dto.getVariables())) {
            variables.putAll(dto.getVariables());
        }
        variables.put(FlowVariableConstants.APPLY_USER_ID, dto.getStartUserId());

        identityService.setAuthenticatedUserId(dto.getStartUserId());
        try {
            ProcessInstance processInstance = runtimeService.startProcessInstanceByKey(
                    dto.getProcessDefinitionKey(),
                    dto.getBusinessKey(),
                    variables
            );

            log.info("流程启动成功，流程定义Key：{}，业务主键：{}，流程实例ID：{}，发起人：{}",
                    dto.getProcessDefinitionKey(),
                    dto.getBusinessKey(),
                    processInstance.getProcessInstanceId(),
                    dto.getStartUserId());

            return processInstance.getProcessInstanceId();
        } finally {
            identityService.setAuthenticatedUserId(null);
        }
    }

    /**
     * 终止流程实例。
     *
     * @param processInstanceId 流程实例 ID
     * @param reason 终止原因
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void terminateProcess(String processInstanceId, String reason) {
        ProcessInstance processInstance = runtimeService.createProcessInstanceQuery()
                .processInstanceId(processInstanceId)
                .singleResult();

        if (processInstance == null) {
            throw new IllegalArgumentException("流程实例不存在或已结束");
        }

        String deleteReason = StrUtil.blankToDefault(reason, "人工终止流程");
        runtimeService.deleteProcessInstance(processInstanceId, deleteReason);

        log.info("流程终止成功，流程实例ID：{}，终止原因：{}", processInstanceId, deleteReason);
    }

    /**
     * 撤回流程实例到指定节点。
     *
     * @param processInstanceId 流程实例 ID
     * @param targetActivityId 目标节点 ID
     * @param reason 撤回原因
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void withdrawProcess(String processInstanceId, String targetActivityId, String reason) {
        if (StrUtil.isBlank(targetActivityId)) {
            throw new IllegalArgumentException("撤回目标节点不能为空");
        }

        Task currentTask = taskService.createTaskQuery()
                .processInstanceId(processInstanceId)
                .singleResult();

        if (currentTask == null) {
            throw new IllegalArgumentException("当前流程不存在待办任务，无法撤回");
        }

        List<HistoricActivityInstance> historyList = historyService.createHistoricActivityInstanceQuery()
                .processInstanceId(processInstanceId)
                .activityId(targetActivityId)
                .finished()
                .orderByHistoricActivityInstanceEndTime()
                .desc()
                .list();

        if (historyList.isEmpty()) {
            throw new IllegalArgumentException("目标节点未执行过，无法撤回");
        }

        runtimeService.createChangeActivityStateBuilder()
                .processInstanceId(processInstanceId)
                .moveActivityIdTo(currentTask.getTaskDefinitionKey(), targetActivityId)
                .changeState();

        log.info("流程撤回成功，流程实例ID：{}，当前节点：{}，目标节点：{}，撤回原因：{}",
                processInstanceId,
                currentTask.getTaskDefinitionKey(),
                targetActivityId,
                StrUtil.blankToDefault(reason, "发起人撤回"));
    }

}
```

启动流程接口示例：

```http
POST /flow/instance/start
Content-Type: application/json

{
  "processDefinitionKey": "leaveApprove",
  "businessKey": "LEAVE_10001",
  "startUserId": "10001",
  "variables": {
    "managerUserId": "10002",
    "bossUserId": "10003",
    "leaveDays": 5
  }
}
```

启动成功后，应同步更新业务表审批状态，例如从 `DRAFT` 改为 `APPROVING`。不要只依赖 Flowable 运行时表判断业务状态。

### 待办任务查询

待办任务查询用于查询当前用户需要处理的任务。待办任务通常分为两类：一类是已经分配给当前用户的任务，即 `assignee` 等于当前用户；另一类是当前用户作为候选人或候选组成员可以领取的任务。

文件位置：`src/main/java/io/github/atengk/flowable/vo/TaskTodoVO.java`

```java
package io.github.atengk.flowable.vo;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 待办任务响应对象。
 *
 * @author Ateng
 * @since 2026-05-08
 */
@Data
@Builder
public class TaskTodoVO {

    /**
     * 任务 ID。
     */
    private String taskId;

    /**
     * 任务名称。
     */
    private String taskName;

    /**
     * 流程实例 ID。
     */
    private String processInstanceId;

    /**
     * 流程定义 ID。
     */
    private String processDefinitionId;

    /**
     * 任务节点 Key。
     */
    private String taskDefinitionKey;

    /**
     * 办理人。
     */
    private String assignee;

    /**
     * 创建时间。
     */
    private LocalDateTime createTime;

}
```

文件位置：`src/main/java/io/github/atengk/flowable/service/FlowTaskService.java`

```java
package io.github.atengk.flowable.service;

import io.github.atengk.flowable.dto.CompleteTaskDTO;
import io.github.atengk.flowable.vo.TaskDoneVO;
import io.github.atengk.flowable.vo.TaskTodoVO;

import java.util.List;

/**
 * 流程任务服务。
 *
 * @author Ateng
 * @since 2026-05-08
 */
public interface FlowTaskService {

    /**
     * 查询用户待办任务。
     *
     * @param userId 用户 ID
     * @return 待办任务列表
     */
    List<TaskTodoVO> listTodoTasks(String userId);

    /**
     * 查询用户已办任务。
     *
     * @param userId 用户 ID
     * @return 已办任务列表
     */
    List<TaskDoneVO> listDoneTasks(String userId);

    /**
     * 审批任务。
     *
     * @param dto 审批任务参数
     */
    void completeTask(CompleteTaskDTO dto);

    /**
     * 驳回任务到指定节点。
     *
     * @param taskId 当前任务 ID
     * @param targetActivityId 目标节点 ID
     * @param userId 操作用户 ID
     * @param comment 审批意见
     */
    void rejectTask(String taskId, String targetActivityId, String userId, String comment);

}
```

待办任务查询实现放在 `FlowTaskServiceImpl` 中。这里同时查询分配给当前用户的任务和候选任务，并做去重处理。

```java
package io.github.atengk.flowable.service.impl;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.date.LocalDateTimeUtil;
import cn.hutool.core.util.StrUtil;
import io.github.atengk.flowable.common.constant.FlowVariableConstants;
import io.github.atengk.flowable.dto.CompleteTaskDTO;
import io.github.atengk.flowable.service.FlowTaskService;
import io.github.atengk.flowable.vo.TaskDoneVO;
import io.github.atengk.flowable.vo.TaskTodoVO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.flowable.engine.HistoryService;
import org.flowable.engine.RuntimeService;
import org.flowable.engine.TaskService;
import org.flowable.engine.history.HistoricTaskInstance;
import org.flowable.task.api.Task;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

/**
 * 流程任务服务实现。
 *
 * @author Ateng
 * @since 2026-05-08
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class FlowTaskServiceImpl implements FlowTaskService {

    private final TaskService taskService;
    private final HistoryService historyService;
    private final RuntimeService runtimeService;

    /**
     * 查询用户待办任务。
     *
     * @param userId 用户 ID
     * @return 待办任务列表
     */
    @Override
    public List<TaskTodoVO> listTodoTasks(String userId) {
        if (StrUtil.isBlank(userId)) {
            throw new IllegalArgumentException("用户 ID 不能为空");
        }

        List<Task> assigneeTasks = taskService.createTaskQuery()
                .taskAssignee(userId)
                .orderByTaskCreateTime()
                .desc()
                .list();

        List<Task> candidateTasks = taskService.createTaskQuery()
                .taskCandidateUser(userId)
                .orderByTaskCreateTime()
                .desc()
                .list();

        List<Task> allTasks = CollUtil.unionDistinct(assigneeTasks, candidateTasks);

        return allTasks.stream()
                .sorted(Comparator.comparing(Task::getCreateTime).reversed())
                .map(this::toTodoVO)
                .collect(Collectors.toList());
    }

    /**
     * 查询用户已办任务。
     *
     * @param userId 用户 ID
     * @return 已办任务列表
     */
    @Override
    public List<TaskDoneVO> listDoneTasks(String userId) {
        if (StrUtil.isBlank(userId)) {
            throw new IllegalArgumentException("用户 ID 不能为空");
        }

        return historyService.createHistoricTaskInstanceQuery()
                .taskAssignee(userId)
                .finished()
                .orderByHistoricTaskInstanceEndTime()
                .desc()
                .list()
                .stream()
                .map(this::toDoneVO)
                .collect(Collectors.toList());
    }

    /**
     * 审批任务。
     *
     * @param dto 审批任务参数
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void completeTask(CompleteTaskDTO dto) {
        Task task = taskService.createTaskQuery()
                .taskId(dto.getTaskId())
                .singleResult();

        if (task == null) {
            throw new IllegalArgumentException("任务不存在或已被处理");
        }

        if (StrUtil.isNotBlank(task.getAssignee()) && !StrUtil.equals(task.getAssignee(), dto.getUserId())) {
            throw new IllegalStateException("当前任务不属于该用户，无法审批");
        }

        if (StrUtil.isBlank(task.getAssignee())) {
            taskService.claim(dto.getTaskId(), dto.getUserId());
            log.info("候选任务领取成功，任务ID：{}，用户ID：{}", dto.getTaskId(), dto.getUserId());
        }

        Map<String, Object> variables = new HashMap<>();
        if (dto.getVariables() != null) {
            variables.putAll(dto.getVariables());
        }
        variables.put(FlowVariableConstants.APPROVE_RESULT, dto.getApproveResult());
        variables.put(FlowVariableConstants.APPROVE_COMMENT, dto.getComment());

        taskService.addComment(dto.getTaskId(), task.getProcessInstanceId(), dto.getComment());
        taskService.complete(dto.getTaskId(), variables);

        log.info("任务审批完成，任务ID：{}，流程实例ID：{}，用户ID：{}，审批结果：{}",
                dto.getTaskId(), task.getProcessInstanceId(), dto.getUserId(), dto.getApproveResult());
    }

    /**
     * 驳回任务到指定节点。
     *
     * @param taskId 当前任务 ID
     * @param targetActivityId 目标节点 ID
     * @param userId 操作用户 ID
     * @param comment 审批意见
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void rejectTask(String taskId, String targetActivityId, String userId, String comment) {
        Task task = taskService.createTaskQuery()
                .taskId(taskId)
                .singleResult();

        if (task == null) {
            throw new IllegalArgumentException("任务不存在或已被处理");
        }

        if (StrUtil.isBlank(targetActivityId)) {
            throw new IllegalArgumentException("驳回目标节点不能为空");
        }

        if (StrUtil.isNotBlank(task.getAssignee()) && !StrUtil.equals(task.getAssignee(), userId)) {
            throw new IllegalStateException("当前任务不属于该用户，无法驳回");
        }

        if (StrUtil.isBlank(task.getAssignee())) {
            taskService.claim(taskId, userId);
        }

        String rejectComment = StrUtil.blankToDefault(comment, "驳回");
        taskService.addComment(taskId, task.getProcessInstanceId(), rejectComment);

        runtimeService.createChangeActivityStateBuilder()
                .processInstanceId(task.getProcessInstanceId())
                .moveActivityIdTo(task.getTaskDefinitionKey(), targetActivityId)
                .changeState();

        log.info("任务驳回成功，任务ID：{}，流程实例ID：{}，当前节点：{}，目标节点：{}，操作人：{}",
                taskId, task.getProcessInstanceId(), task.getTaskDefinitionKey(), targetActivityId, userId);
    }

    /**
     * 转换为待办任务响应对象。
     *
     * @param task 任务对象
     * @return 待办任务响应对象
     */
    private TaskTodoVO toTodoVO(Task task) {
        LocalDateTime createTime = task.getCreateTime() == null
                ? null
                : LocalDateTimeUtil.of(task.getCreateTime());

        return TaskTodoVO.builder()
                .taskId(task.getId())
                .taskName(task.getName())
                .processInstanceId(task.getProcessInstanceId())
                .processDefinitionId(task.getProcessDefinitionId())
                .taskDefinitionKey(task.getTaskDefinitionKey())
                .assignee(task.getAssignee())
                .createTime(createTime)
                .build();
    }

    /**
     * 转换为已办任务响应对象。
     *
     * @param task 历史任务对象
     * @return 已办任务响应对象
     */
    private TaskDoneVO toDoneVO(HistoricTaskInstance task) {
        LocalDateTime startTime = task.getStartTime() == null
                ? null
                : LocalDateTimeUtil.of(task.getStartTime());
        LocalDateTime endTime = task.getEndTime() == null
                ? null
                : LocalDateTimeUtil.of(task.getEndTime());

        return TaskDoneVO.builder()
                .taskId(task.getId())
                .taskName(task.getName())
                .processInstanceId(task.getProcessInstanceId())
                .processDefinitionId(task.getProcessDefinitionId())
                .taskDefinitionKey(task.getTaskDefinitionKey())
                .assignee(task.getAssignee())
                .startTime(startTime)
                .endTime(endTime)
                .build();
    }

}
```

待办查询接口示例：

```http
GET /flow/task/todo?userId=10002
```

返回示例：

```json
[
  {
    "taskId": "25005",
    "taskName": "部门经理审批",
    "processInstanceId": "25001",
    "processDefinitionId": "leaveApprove:1:20004",
    "taskDefinitionKey": "managerApprove",
    "assignee": "10002",
    "createTime": "2026-05-08T10:30:00"
  }
]
```

### 已办任务查询

已办任务查询用于查询当前用户已经处理完成的历史任务。该功能依赖 Flowable 历史表，因此必须开启历史配置。

```yaml
flowable:
  # 开启历史数据
  db-history-used: true

  # audit 可满足大多数审批轨迹查询场景
  history-level: audit
```

文件位置：`src/main/java/io/github/atengk/flowable/vo/TaskDoneVO.java`

```java
package io.github.atengk.flowable.vo;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 已办任务响应对象。
 *
 * @author Ateng
 * @since 2026-05-08
 */
@Data
@Builder
public class TaskDoneVO {

    /**
     * 任务 ID。
     */
    private String taskId;

    /**
     * 任务名称。
     */
    private String taskName;

    /**
     * 流程实例 ID。
     */
    private String processInstanceId;

    /**
     * 流程定义 ID。
     */
    private String processDefinitionId;

    /**
     * 任务节点 Key。
     */
    private String taskDefinitionKey;

    /**
     * 办理人。
     */
    private String assignee;

    /**
     * 开始时间。
     */
    private LocalDateTime startTime;

    /**
     * 结束时间。
     */
    private LocalDateTime endTime;

}
```

已办查询接口示例：

```http
GET /flow/task/done?userId=10002
```

已办任务一般只表示 Flowable 层面的任务完成记录。如果系统需要展示审批意见、审批动作、审批状态、附件等业务信息，建议额外维护业务审批记录表，例如 `flow_approval_record`。

### 任务审批

任务审批用于完成当前用户任务，并将审批结果、审批意见、下一节点审批人等变量传入流程引擎。对于带条件网关的流程，审批时传入的变量会影响流程后续走向。

文件位置：`src/main/java/io/github/atengk/flowable/dto/CompleteTaskDTO.java`

```java
package io.github.atengk.flowable.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

import java.util.Map;

/**
 * 审批任务请求参数。
 *
 * @author Ateng
 * @since 2026-05-08
 */
@Data
public class CompleteTaskDTO {

    /**
     * 任务 ID。
     */
    @NotBlank(message = "任务 ID 不能为空")
    private String taskId;

    /**
     * 当前审批用户 ID。
     */
    @NotBlank(message = "审批用户 ID 不能为空")
    private String userId;

    /**
     * 审批结果，例如 PASS、REJECT。
     */
    @NotBlank(message = "审批结果不能为空")
    private String approveResult;

    /**
     * 审批意见。
     */
    private String comment;

    /**
     * 流程变量。
     */
    private Map<String, Object> variables;

}
```

审批接口示例：

```http
POST /flow/task/complete
Content-Type: application/json

{
  "taskId": "25005",
  "userId": "10002",
  "approveResult": "PASS",
  "comment": "同意",
  "variables": {
    "bossUserId": "10003"
  }
}
```

对于有候选人的任务，审批前可以先领取任务，再完成任务。上面的 `completeTask` 方法已经做了兼容：如果任务没有办理人，则先 `claim` 到当前用户，再执行 `complete`。

审批完成后建议同步做以下业务动作：

| 动作             | 说明                                     |
| ---------------- | ---------------------------------------- |
| 保存审批记录     | 保存审批人、审批意见、审批结果、审批时间 |
| 更新业务状态     | 例如 `APPROVING`、`APPROVED`、`REJECTED` |
| 判断流程是否结束 | 如果流程结束，将业务表状态改为最终状态   |
| 发送消息通知     | 通知下一节点审批人或申请人               |
| 记录操作日志     | 便于问题追踪和审计                       |

### 流程驳回

流程驳回通常表示审批人不同意当前申请，并将流程退回到指定历史节点或发起人节点。Flowable 没有一个适用于所有业务的“驳回”标准动作，常见实现方式有两种。

第一种是通过流程变量走网关分支，例如审批结果为 `REJECT` 时流向结束节点，并将业务状态标记为已驳回。第二种是通过动态变更流程状态，将当前节点移动到指定历史节点。

推荐策略如下：

| 驳回方式     | 说明                                      | 适用场景               |
| ------------ | ----------------------------------------- | ---------------------- |
| 网关驳回     | 审批结果为 `REJECT`，流程流向驳回结束节点 | 简单驳回、流程结束     |
| 节点跳转驳回 | 当前节点跳转回申请节点或上一审批节点      | 需要重新提交、重新审批 |
| 业务状态驳回 | 结束流程，同时业务表标记为 `REJECTED`     | 不允许修改后再提交     |
| 退回发起人   | 回到提交人修改节点                        | 常见单据审批           |

BPMN 网关驳回示例：

```xml
<exclusiveGateway id="approveGateway" name="审批结果判断"/>

<sequenceFlow id="flow_pass"
              sourceRef="approveGateway"
              targetRef="nextApprove">
    <conditionExpression xsi:type="tFormalExpression">
        <![CDATA[${approveResult == 'PASS'}]]>
    </conditionExpression>
</sequenceFlow>

<sequenceFlow id="flow_reject"
              sourceRef="approveGateway"
              targetRef="rejectEnd">
    <conditionExpression xsi:type="tFormalExpression">
        <![CDATA[${approveResult == 'REJECT'}]]>
    </conditionExpression>
</sequenceFlow>

<endEvent id="rejectEnd" name="驳回结束"/>
```

动态驳回接口示例：

```http
POST /flow/task/reject
Content-Type: application/json

{
  "taskId": "25005",
  "targetActivityId": "submitApply",
  "userId": "10002",
  "comment": "材料不完整，请补充后重新提交"
}
```

动态驳回需要注意：目标节点必须是当前流程实例中允许回退的节点。正式项目中不建议让前端随意传入 `targetActivityId`，应由后端根据历史轨迹和业务规则计算可驳回节点列表。

### 流程终止

流程终止表示强制结束正在运行的流程实例。终止通常由管理员、发起人或具备特定权限的用户执行，适用于业务作废、重复提交、异常流程处理等场景。

终止流程示例：

```http
POST /flow/instance/terminate
Content-Type: application/json

{
  "processInstanceId": "25001",
  "reason": "业务单据作废，终止审批流程"
}
```

终止流程的核心代码已在 `FlowInstanceServiceImpl#terminateProcess` 中给出：

```java
runtimeService.deleteProcessInstance(processInstanceId, deleteReason);
```

终止流程注意事项：

| 注意事项           | 说明                                               |
| ------------------ | -------------------------------------------------- |
| 终止前校验权限     | 只有发起人、管理员或业务授权人可以终止             |
| 同步业务状态       | 业务表应更新为 `TERMINATED` 或 `CANCELED`          |
| 保存操作记录       | 记录终止人、终止原因、终止时间                     |
| 区分正常结束和终止 | 正常结束是流程走到结束节点，终止是人为删除运行实例 |
| 谨慎用于生产       | 终止会影响运行时任务，需要保留审计信息             |

### 流程撤回

流程撤回通常指发起人在下一审批人尚未处理前，将流程从当前节点拉回到发起人节点或提交节点。撤回和驳回不同：驳回由审批人发起，撤回通常由申请人发起。

常见撤回规则如下：

| 规则                   | 说明                                           |
| ---------------------- | ---------------------------------------------- |
| 只能撤回运行中流程     | 已结束流程不能撤回                             |
| 下一节点未处理才能撤回 | 如果审批人已经处理，不允许撤回                 |
| 只能撤回到指定节点     | 通常是申请人修改节点或提交节点                 |
| 需要校验发起人身份     | 非发起人不能随意撤回                           |
| 撤回后更新业务状态     | 例如从 `APPROVING` 改为 `DRAFT` 或 `WITHDRAWN` |

撤回接口示例：

```http
POST /flow/instance/withdraw
Content-Type: application/json

{
  "processInstanceId": "25001",
  "targetActivityId": "submitApply",
  "reason": "申请内容填写错误，撤回修改"
}
```

撤回核心实现方式如下：

```java
runtimeService.createChangeActivityStateBuilder()
        .processInstanceId(processInstanceId)
        .moveActivityIdTo(currentTask.getTaskDefinitionKey(), targetActivityId)
        .changeState();
```

撤回功能建议增加以下校验：

| 校验项               | 说明                                 |
| -------------------- | ------------------------------------ |
| 流程是否存在         | 防止撤回已结束或不存在的流程         |
| 当前是否有待办任务   | 没有待办任务时无法撤回               |
| 当前任务是否未处理   | 如果任务已完成，不能按旧任务撤回     |
| 目标节点是否合法     | 只能撤回到允许的节点                 |
| 操作人是否为发起人   | 避免越权撤回                         |
| 业务状态是否允许撤回 | 已归档、已支付、已生效的数据不应撤回 |

在生产系统中，撤回不建议完全依赖 Flowable 的状态变更能力。更完整的实现应同时维护业务表状态、审批记录、操作日志和消息通知，确保用户在前端看到的业务状态和 Flowable 实际流程状态一致。



## 业务集成设计

业务集成设计用于解决 Flowable 流程数据和业务系统数据如何协同的问题。Flowable 负责流程定义、流程实例、任务、变量、历史轨迹等引擎数据，业务系统负责保存具体业务单据、审批状态、审批记录、业务附件、业务扩展字段等数据。

实际项目中不建议把业务数据全部塞进 Flowable 流程变量，也不建议直接修改 Flowable 引擎表。推荐做法是：业务表保存业务主数据，Flowable 保存流程运行数据，中间通过 `businessKey`、`processInstanceId` 和业务流程关联表进行绑定。

### 业务表设计

业务表设计需要同时考虑“具体业务单据”和“通用流程关联信息”。如果系统只有一个审批场景，可以直接在业务单据表中增加流程字段；如果系统有多个审批场景，例如请假、报销、采购、合同等，建议增加一张通用流程业务关联表。

推荐至少包含三类表：

| 表名                   | 说明                                                     |
| ---------------------- | -------------------------------------------------------- |
| `leave_application`    | 示例业务表，请假申请单                                   |
| `flow_business`        | 通用流程业务关联表，保存业务主键、流程实例、审批状态     |
| `flow_approval_record` | 通用审批记录表，保存审批动作、审批意见、办理人、任务节点 |

下面 SQL 用于创建通用流程业务关联表和审批记录表，适合作为多审批场景的基础表结构。

```sql
-- 通用流程业务关联表
CREATE TABLE flow_business (
    id BIGINT NOT NULL COMMENT '主键ID',
    business_key VARCHAR(64) NOT NULL COMMENT '业务主键，例如请假单ID、报销单ID',
    business_type VARCHAR(64) NOT NULL COMMENT '业务类型，例如 LEAVE、EXPENSE、CONTRACT',
    business_title VARCHAR(255) DEFAULT NULL COMMENT '业务标题，用于待办列表展示',
    process_definition_key VARCHAR(128) NOT NULL COMMENT '流程定义Key',
    process_instance_id VARCHAR(64) DEFAULT NULL COMMENT '流程实例ID',
    current_task_id VARCHAR(64) DEFAULT NULL COMMENT '当前任务ID',
    current_task_name VARCHAR(128) DEFAULT NULL COMMENT '当前任务名称',
    start_user_id VARCHAR(64) NOT NULL COMMENT '发起人用户ID',
    status VARCHAR(32) NOT NULL COMMENT '审批状态：DRAFT、APPROVING、APPROVED、REJECTED、WITHDRAWN、TERMINATED',
    tenant_id VARCHAR(64) DEFAULT NULL COMMENT '租户ID，多租户系统使用',
    version INT NOT NULL DEFAULT 0 COMMENT '乐观锁版本号',
    deleted TINYINT NOT NULL DEFAULT 0 COMMENT '逻辑删除标识：0未删除，1已删除',
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (id),
    UNIQUE KEY uk_business_type_key (business_type, business_key),
    UNIQUE KEY uk_process_instance_id (process_instance_id),
    KEY idx_status (status),
    KEY idx_start_user_id (start_user_id),
    KEY idx_create_time (create_time)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='通用流程业务关联表';

-- 通用审批记录表
CREATE TABLE flow_approval_record (
    id BIGINT NOT NULL COMMENT '主键ID',
    business_key VARCHAR(64) NOT NULL COMMENT '业务主键',
    business_type VARCHAR(64) NOT NULL COMMENT '业务类型',
    process_instance_id VARCHAR(64) NOT NULL COMMENT '流程实例ID',
    process_definition_id VARCHAR(128) DEFAULT NULL COMMENT '流程定义ID',
    task_id VARCHAR(64) DEFAULT NULL COMMENT '任务ID',
    task_definition_key VARCHAR(128) DEFAULT NULL COMMENT '任务节点Key',
    task_name VARCHAR(128) DEFAULT NULL COMMENT '任务名称',
    approver_id VARCHAR(64) NOT NULL COMMENT '审批人用户ID',
    approver_name VARCHAR(128) DEFAULT NULL COMMENT '审批人名称',
    approve_action VARCHAR(32) NOT NULL COMMENT '审批动作：SUBMIT、PASS、REJECT、WITHDRAW、TERMINATE、TRANSFER',
    approve_comment VARCHAR(1000) DEFAULT NULL COMMENT '审批意见',
    variables_json TEXT DEFAULT NULL COMMENT '审批时提交的流程变量JSON',
    approve_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '审批时间',
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    PRIMARY KEY (id),
    KEY idx_business_type_key (business_type, business_key),
    KEY idx_process_instance_id (process_instance_id),
    KEY idx_task_id (task_id),
    KEY idx_approver_id (approver_id),
    KEY idx_approve_time (approve_time)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='通用审批记录表';
```

如果以请假审批作为示例业务，可以设计如下业务表。

```sql
-- 请假申请表，作为具体业务单据示例
CREATE TABLE leave_application (
    id BIGINT NOT NULL COMMENT '主键ID',
    apply_user_id VARCHAR(64) NOT NULL COMMENT '申请人用户ID',
    apply_user_name VARCHAR(128) DEFAULT NULL COMMENT '申请人名称',
    leave_type VARCHAR(32) NOT NULL COMMENT '请假类型：ANNUAL、SICK、PERSONAL',
    leave_days DECIMAL(10, 2) NOT NULL COMMENT '请假天数',
    start_time DATETIME NOT NULL COMMENT '开始时间',
    end_time DATETIME NOT NULL COMMENT '结束时间',
    reason VARCHAR(1000) DEFAULT NULL COMMENT '请假原因',
    approval_status VARCHAR(32) NOT NULL COMMENT '审批状态',
    process_instance_id VARCHAR(64) DEFAULT NULL COMMENT '流程实例ID',
    deleted TINYINT NOT NULL DEFAULT 0 COMMENT '逻辑删除标识：0未删除，1已删除',
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (id),
    KEY idx_apply_user_id (apply_user_id),
    KEY idx_approval_status (approval_status),
    KEY idx_process_instance_id (process_instance_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='请假申请表';
```

表设计建议如下：

| 建议                     | 说明                                       |
| ------------------------ | ------------------------------------------ |
| 业务表保留审批状态       | 前端列表和业务查询不应依赖 Flowable 运行表 |
| 通用表保留 `businessKey` | 统一关联不同业务类型的单据                 |
| 保留 `processInstanceId` | 便于查询流程轨迹、当前节点和历史任务       |
| 审批记录单独保存         | Flowable 历史表不完全等同于业务审批记录    |
| 审批状态使用枚举         | 避免字符串散落在代码中                     |
| 不直接改 Flowable 表     | Flowable 表由引擎维护，业务侧只查询不修改  |

### 流程实例与业务数据关联

流程实例与业务数据关联主要依赖两个字段：`businessKey` 和 `processInstanceId`。

`businessKey` 是启动流程实例时传入的业务主键，一般使用业务单据 ID。`processInstanceId` 是 Flowable 启动流程后生成的流程实例 ID。业务系统应同时保存这两个字段。

推荐关联方式如下：

```text
业务单据ID
   ↓
businessKey
   ↓
Flowable 流程实例 processInstanceId
   ↓
运行任务、历史任务、审批轨迹、流程图高亮
```

启动流程时推荐使用以下顺序：

1. 保存业务单据，生成业务主键。
2. 保存或初始化 `flow_business` 记录，状态为 `APPROVING`。
3. 组装流程变量，例如申请人、审批人、金额、天数。
4. 调用 `RuntimeService#startProcessInstanceByKey` 启动流程。
5. 将返回的 `processInstanceId` 回写到业务表和 `flow_business` 表。
6. 保存一条 `SUBMIT` 审批记录。

下面代码演示流程启动时的业务关联逻辑，实际项目中可以放在具体业务 Service 中，例如请假申请 Service。

文件位置：`src/main/java/io/github/atengk/flowable/service/impl/LeaveApplicationServiceImpl.java`

```java
package io.github.atengk.flowable.service.impl;

import cn.hutool.core.map.MapUtil;
import cn.hutool.core.util.IdUtil;
import cn.hutool.core.util.StrUtil;
import io.github.atengk.flowable.common.constant.FlowVariableConstants;
import io.github.atengk.flowable.common.enums.FlowApprovalStatusEnum;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.flowable.engine.IdentityService;
import org.flowable.engine.RuntimeService;
import org.flowable.engine.runtime.ProcessInstance;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Map;

/**
 * 请假申请业务服务实现。
 *
 * @author Ateng
 * @since 2026-05-08
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class LeaveApplicationServiceImpl {

    private final RuntimeService runtimeService;
    private final IdentityService identityService;

    /**
     * 提交请假申请并启动审批流程。
     *
     * @param applyUserId 申请人ID
     * @param managerUserId 部门经理ID
     * @param bossUserId 总经理ID
     * @param leaveDays 请假天数
     * @return 流程实例ID
     */
    @Transactional(rollbackFor = Exception.class)
    public String submitLeave(String applyUserId, String managerUserId, String bossUserId, Integer leaveDays) {
        if (StrUtil.hasBlank(applyUserId, managerUserId)) {
            throw new IllegalArgumentException("申请人和部门经理不能为空");
        }

        String businessKey = IdUtil.getSnowflakeNextIdStr();

        Map<String, Object> variables = MapUtil.<String, Object>builder()
                .put(FlowVariableConstants.APPLY_USER_ID, applyUserId)
                .put(FlowVariableConstants.MANAGER_USER_ID, managerUserId)
                .put(FlowVariableConstants.BOSS_USER_ID, bossUserId)
                .put(FlowVariableConstants.LEAVE_DAYS, leaveDays)
                .build();

        identityService.setAuthenticatedUserId(applyUserId);
        try {
            ProcessInstance processInstance = runtimeService.startProcessInstanceByKey(
                    "leaveApprove",
                    businessKey,
                    variables
            );

            /*
             * 实际项目中这里需要继续执行：
             * 1. 保存 leave_application 表
             * 2. 保存 flow_business 表
             * 3. 保存 flow_approval_record 表，动作 SUBMIT
             */
            log.info("请假申请提交成功，业务主键：{}，流程实例ID：{}，审批状态：{}",
                    businessKey,
                    processInstance.getProcessInstanceId(),
                    FlowApprovalStatusEnum.APPROVING.getCode());

            return processInstance.getProcessInstanceId();
        } finally {
            identityService.setAuthenticatedUserId(null);
        }
    }

}
```

该示例省略了 Mapper 和实体保存逻辑，重点展示业务单据、流程变量和流程实例的关联顺序。正式项目中，上述保存业务单据、启动流程、写审批记录应放在同一个事务中。

### 审批状态维护

审批状态维护用于让业务系统明确知道当前单据处于什么状态。Flowable 的运行时表可以反映流程是否运行中，但业务列表、统计报表、权限控制、消息通知更适合使用业务状态字段完成。

推荐审批状态如下：

| 状态         | 说明                   |
| ------------ | ---------------------- |
| `DRAFT`      | 草稿，尚未提交审批     |
| `APPROVING`  | 审批中                 |
| `APPROVED`   | 审批通过，流程正常结束 |
| `REJECTED`   | 审批驳回               |
| `WITHDRAWN`  | 发起人撤回             |
| `TERMINATED` | 管理员或业务方终止     |
| `CANCELED`   | 业务单据取消或作废     |

审批状态枚举如下。

文件位置：`src/main/java/io/github/atengk/flowable/common/enums/FlowApprovalStatusEnum.java`

```java
package io.github.atengk.flowable.common.enums;

import cn.hutool.core.util.StrUtil;
import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.Arrays;

/**
 * 流程审批状态枚举。
 *
 * @author Ateng
 * @since 2026-05-08
 */
@Getter
@AllArgsConstructor
public enum FlowApprovalStatusEnum {

    DRAFT("DRAFT", "草稿"),
    APPROVING("APPROVING", "审批中"),
    APPROVED("APPROVED", "审批通过"),
    REJECTED("REJECTED", "审批驳回"),
    WITHDRAWN("WITHDRAWN", "已撤回"),
    TERMINATED("TERMINATED", "已终止"),
    CANCELED("CANCELED", "已取消");

    private final String code;
    private final String desc;

    /**
     * 根据编码获取枚举。
     *
     * @param code 状态编码
     * @return 审批状态枚举
     */
    public static FlowApprovalStatusEnum of(String code) {
        return Arrays.stream(values())
                .filter(item -> StrUtil.equals(item.getCode(), code))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("不支持的审批状态：" + code));
    }

}
```

状态流转建议如下：

| 操作               | 原状态                           | 目标状态     |
| ------------------ | -------------------------------- | ------------ |
| 保存草稿           | 无                               | `DRAFT`      |
| 提交审批           | `DRAFT`、`WITHDRAWN`、`REJECTED` | `APPROVING`  |
| 审批通过且流程结束 | `APPROVING`                      | `APPROVED`   |
| 审批驳回并结束     | `APPROVING`                      | `REJECTED`   |
| 发起人撤回         | `APPROVING`                      | `WITHDRAWN`  |
| 管理员终止         | `APPROVING`                      | `TERMINATED` |
| 业务取消           | `DRAFT`、`WITHDRAWN`、`REJECTED` | `CANCELED`   |

状态维护建议放在业务 Service 层统一处理，不建议在 Controller 中直接更新状态。涉及审批通过、驳回、终止、撤回等操作时，应同时更新业务表、`flow_business` 表和审批记录表。

### 审批记录保存

审批记录用于展示审批轨迹和满足审计要求。虽然 Flowable 自带历史任务表和评论表，但实际业务系统通常还需要保存审批动作、审批意见、审批人名称、业务类型、业务标题、附件信息、操作来源等扩展信息，因此建议维护独立审批记录表。

审批动作建议如下：

| 动作        | 说明       |
| ----------- | ---------- |
| `SUBMIT`    | 提交审批   |
| `PASS`      | 审批通过   |
| `REJECT`    | 审批驳回   |
| `WITHDRAW`  | 发起人撤回 |
| `TERMINATE` | 流程终止   |
| `TRANSFER`  | 转办       |
| `DELEGATE`  | 委派       |
| `ADD_SIGN`  | 加签       |

审批动作枚举如下。

文件位置：`src/main/java/io/github/atengk/flowable/common/enums/FlowApprovalActionEnum.java`

```java
package io.github.atengk.flowable.common.enums;

import cn.hutool.core.util.StrUtil;
import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.Arrays;

/**
 * 流程审批动作枚举。
 *
 * @author Ateng
 * @since 2026-05-08
 */
@Getter
@AllArgsConstructor
public enum FlowApprovalActionEnum {

    SUBMIT("SUBMIT", "提交审批"),
    PASS("PASS", "审批通过"),
    REJECT("REJECT", "审批驳回"),
    WITHDRAW("WITHDRAW", "撤回流程"),
    TERMINATE("TERMINATE", "终止流程"),
    TRANSFER("TRANSFER", "转办任务"),
    DELEGATE("DELEGATE", "委派任务"),
    ADD_SIGN("ADD_SIGN", "加签");

    private final String code;
    private final String desc;

    /**
     * 根据编码获取枚举。
     *
     * @param code 动作编码
     * @return 审批动作枚举
     */
    public static FlowApprovalActionEnum of(String code) {
        return Arrays.stream(values())
                .filter(item -> StrUtil.equals(item.getCode(), code))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("不支持的审批动作：" + code));
    }

}
```

审批记录保存时机如下：

| 时机      | 保存内容                                |
| --------- | --------------------------------------- |
| 提交审批  | 发起人、业务主键、流程实例 ID、提交动作 |
| 审批通过  | 当前任务、审批人、审批意见、审批变量    |
| 驳回      | 当前任务、目标节点、驳回原因            |
| 撤回      | 发起人、当前节点、撤回原因              |
| 终止      | 操作人、终止原因                        |
| 转办/委派 | 原办理人、新办理人、操作原因            |

审批记录保存逻辑建议封装为独立 Service，例如 `FlowApprovalRecordService`，由流程启动、审批、驳回、撤回、终止等业务调用，避免重复拼装记录字段。

### 流程变量传递

流程变量用于在流程运行过程中传递上下文数据，例如审批人、审批结果、业务金额、请假天数、下一节点候选人等。变量可以在 BPMN 表达式、用户任务、监听器和后端查询中使用。

流程变量分为两类：

| 类型     | 说明                             | 示例                                       |
| -------- | -------------------------------- | ------------------------------------------ |
| 全局变量 | 绑定到流程实例，整个流程可见     | `applyUserId`、`leaveDays`、`businessType` |
| 局部变量 | 绑定到任务或执行节点，仅局部可见 | 当前任务临时审批意见                       |

推荐常用变量如下：

| 变量名           | 说明            |
| ---------------- | --------------- |
| `applyUserId`    | 申请人用户 ID   |
| `businessKey`    | 业务主键        |
| `businessType`   | 业务类型        |
| `managerUserId`  | 部门经理用户 ID |
| `bossUserId`     | 总经理用户 ID   |
| `approveResult`  | 审批结果        |
| `approveComment` | 审批意见        |
| `leaveDays`      | 请假天数        |
| `amount`         | 金额            |
| `nextUserId`     | 下一节点审批人  |

变量传递建议：

| 建议                 | 说明                                     |
| -------------------- | ---------------------------------------- |
| 只传轻量数据         | 传 ID、编码、金额、布尔值，不传复杂对象  |
| 变量名统一常量化     | 后端和 BPMN 文件保持一致                 |
| 关键业务数据落业务表 | 不要把业务主数据只保存在流程变量中       |
| 条件变量提前校验     | 避免网关表达式运行时失败                 |
| 审批意见写审批记录   | 变量只作为流程流转上下文，不替代审批记录 |

审批时传递变量示例：

```json
{
  "taskId": "25005",
  "userId": "10002",
  "approveResult": "PASS",
  "comment": "同意",
  "variables": {
    "approveResult": "PASS",
    "approveComment": "同意",
    "bossUserId": "10003"
  }
}
```

对于条件网关，后端必须确保变量类型和表达式匹配。例如 BPMN 中使用 `${amount > 5000}`，后端应传入数字类型，不应传入字符串 `"5000"`。

## 接口设计

接口设计用于对外提供流程定义、流程实例、任务办理、审批记录和流程图查看能力。实际项目中建议统一接口前缀，例如 `/api/flow`，并在 Controller 层只处理参数接收和响应封装，核心逻辑放在 Service 层完成。

接口返回结构建议统一，例如：

```json
{
  "code": 200,
  "message": "操作成功",
  "data": {}
}
```

### 流程定义接口

流程定义接口用于管理 BPMN 流程模板，包括部署流程、查询流程定义、查看 BPMN XML、挂起流程定义和激活流程定义。

推荐接口如下：

| 接口                                                   | 方法   | 说明                 |
| ------------------------------------------------------ | ------ | -------------------- |
| `/api/flow/definitions/deploy`                         | `POST` | 上传 BPMN 文件并部署 |
| `/api/flow/definitions`                                | `GET`  | 分页查询流程定义     |
| `/api/flow/definitions/{processDefinitionId}`          | `GET`  | 查询流程定义详情     |
| `/api/flow/definitions/{processDefinitionId}/xml`      | `GET`  | 查看流程定义 XML     |
| `/api/flow/definitions/{processDefinitionId}/suspend`  | `PUT`  | 挂起流程定义         |
| `/api/flow/definitions/{processDefinitionId}/activate` | `PUT`  | 激活流程定义         |

流程定义 Controller 示例负责接收 BPMN 文件并调用部署服务。

文件位置：`src/main/java/io/github/atengk/flowable/controller/FlowDefinitionController.java`

```java
package io.github.atengk.flowable.controller;

import io.github.atengk.flowable.service.FlowDefinitionService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

/**
 * 流程定义接口。
 *
 * @author Ateng
 * @since 2026-05-08
 */
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/flow/definitions")
public class FlowDefinitionController {

    private final FlowDefinitionService flowDefinitionService;

    /**
     * 上传并部署 BPMN 文件。
     *
     * @param file BPMN 文件
     * @param deploymentName 部署名称
     * @return 部署 ID
     */
    @PostMapping("/deploy")
    public String deploy(@RequestPart("file") MultipartFile file,
                         @RequestParam(required = false) String deploymentName) {
        return flowDefinitionService.deployByFile(file, deploymentName);
    }

}
```

请求示例：

```bash
curl -X POST "http://localhost:8080/api/flow/definitions/deploy" \
  -F "file=@leave-approve.bpmn20.xml" \
  -F "deploymentName=请假审批流程"
```

### 流程实例接口

流程实例接口用于启动流程、查询流程详情、终止流程、撤回流程和查询流程进度。流程实例接口通常需要和业务权限绑定，例如只有发起人可以撤回，管理员可以终止。

推荐接口如下：

| 接口                                                 | 方法   | 说明             |
| ---------------------------------------------------- | ------ | ---------------- |
| `/api/flow/instances/start`                          | `POST` | 启动流程实例     |
| `/api/flow/instances/{processInstanceId}`            | `GET`  | 查询流程实例详情 |
| `/api/flow/instances/{processInstanceId}/terminate`  | `POST` | 终止流程实例     |
| `/api/flow/instances/{processInstanceId}/withdraw`   | `POST` | 撤回流程实例     |
| `/api/flow/instances/{processInstanceId}/activities` | `GET`  | 查询流程活动轨迹 |

流程实例 Controller 示例用于启动、终止和撤回流程。

文件位置：`src/main/java/io/github/atengk/flowable/controller/FlowInstanceController.java`

```java
package io.github.atengk.flowable.controller;

import io.github.atengk.flowable.dto.StartProcessDTO;
import io.github.atengk.flowable.service.FlowInstanceService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

/**
 * 流程实例接口。
 *
 * @author Ateng
 * @since 2026-05-08
 */
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/flow/instances")
public class FlowInstanceController {

    private final FlowInstanceService flowInstanceService;

    /**
     * 启动流程实例。
     *
     * @param dto 启动流程请求参数
     * @return 流程实例 ID
     */
    @PostMapping("/start")
    public String start(@Valid @RequestBody StartProcessDTO dto) {
        return flowInstanceService.startProcess(dto);
    }

    /**
     * 终止流程实例。
     *
     * @param processInstanceId 流程实例 ID
     * @param dto 终止请求参数
     */
    @PostMapping("/{processInstanceId}/terminate")
    public void terminate(@PathVariable String processInstanceId,
                          @Valid @RequestBody TerminateProcessRequest dto) {
        flowInstanceService.terminateProcess(processInstanceId, dto.getReason());
    }

    /**
     * 撤回流程实例。
     *
     * @param processInstanceId 流程实例 ID
     * @param dto 撤回请求参数
     */
    @PostMapping("/{processInstanceId}/withdraw")
    public void withdraw(@PathVariable String processInstanceId,
                         @Valid @RequestBody WithdrawProcessRequest dto) {
        flowInstanceService.withdrawProcess(processInstanceId, dto.getTargetActivityId(), dto.getReason());
    }

    /**
     * 终止流程请求参数。
     *
     * @author Ateng
     * @since 2026-05-08
     */
    @Data
    public static class TerminateProcessRequest {

        /**
         * 终止原因。
         */
        @NotBlank(message = "终止原因不能为空")
        private String reason;

    }

    /**
     * 撤回流程请求参数。
     *
     * @author Ateng
     * @since 2026-05-08
     */
    @Data
    public static class WithdrawProcessRequest {

        /**
         * 目标节点 ID。
         */
        @NotBlank(message = "目标节点 ID 不能为空")
        private String targetActivityId;

        /**
         * 撤回原因。
         */
        private String reason;

    }

}
```

启动流程请求示例：

```json
{
  "processDefinitionKey": "leaveApprove",
  "businessKey": "1864200000000000001",
  "startUserId": "10001",
  "variables": {
    "managerUserId": "10002",
    "bossUserId": "10003",
    "leaveDays": 5
  }
}
```

### 任务办理接口

任务办理接口用于处理待办任务、候选任务、审批通过、审批驳回、任务领取、任务归还、转办和委派等操作。审批系统最核心的接口通常集中在这一组。

推荐接口如下：

| 接口                                | 方法   | 说明                   |
| ----------------------------------- | ------ | ---------------------- |
| `/api/flow/tasks/todo`              | `GET`  | 查询我的待办任务       |
| `/api/flow/tasks/done`              | `GET`  | 查询我的已办任务       |
| `/api/flow/tasks/{taskId}/claim`    | `POST` | 领取候选任务           |
| `/api/flow/tasks/{taskId}/unclaim`  | `POST` | 归还任务               |
| `/api/flow/tasks/complete`          | `POST` | 审批通过或提交审批结果 |
| `/api/flow/tasks/{taskId}/reject`   | `POST` | 驳回任务               |
| `/api/flow/tasks/{taskId}/transfer` | `POST` | 转办任务               |
| `/api/flow/tasks/{taskId}/delegate` | `POST` | 委派任务               |

任务办理 Controller 示例用于查询待办、已办、审批和驳回。

文件位置：`src/main/java/io/github/atengk/flowable/controller/FlowTaskController.java`

```java
package io.github.atengk.flowable.controller;

import io.github.atengk.flowable.dto.CompleteTaskDTO;
import io.github.atengk.flowable.service.FlowTaskService;
import io.github.atengk.flowable.vo.TaskDoneVO;
import io.github.atengk.flowable.vo.TaskTodoVO;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 流程任务接口。
 *
 * @author Ateng
 * @since 2026-05-08
 */
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/flow/tasks")
public class FlowTaskController {

    private final FlowTaskService flowTaskService;

    /**
     * 查询用户待办任务。
     *
     * @param userId 用户 ID
     * @return 待办任务列表
     */
    @GetMapping("/todo")
    public List<TaskTodoVO> todo(@RequestParam String userId) {
        return flowTaskService.listTodoTasks(userId);
    }

    /**
     * 查询用户已办任务。
     *
     * @param userId 用户 ID
     * @return 已办任务列表
     */
    @GetMapping("/done")
    public List<TaskDoneVO> done(@RequestParam String userId) {
        return flowTaskService.listDoneTasks(userId);
    }

    /**
     * 审批任务。
     *
     * @param dto 审批请求参数
     */
    @PostMapping("/complete")
    public void complete(@Valid @RequestBody CompleteTaskDTO dto) {
        flowTaskService.completeTask(dto);
    }

    /**
     * 驳回任务。
     *
     * @param taskId 任务 ID
     * @param dto 驳回请求参数
     */
    @PostMapping("/{taskId}/reject")
    public void reject(@PathVariable String taskId,
                       @Valid @RequestBody RejectTaskRequest dto) {
        flowTaskService.rejectTask(taskId, dto.getTargetActivityId(), dto.getUserId(), dto.getComment());
    }

    /**
     * 驳回任务请求参数。
     *
     * @author Ateng
     * @since 2026-05-08
     */
    @Data
    public static class RejectTaskRequest {

        /**
         * 目标节点 ID。
         */
        @NotBlank(message = "目标节点 ID 不能为空")
        private String targetActivityId;

        /**
         * 操作用户 ID。
         */
        @NotBlank(message = "操作用户 ID 不能为空")
        private String userId;

        /**
         * 驳回意见。
         */
        private String comment;

    }

}
```

审批请求示例：

```json
{
  "taskId": "25005",
  "userId": "10002",
  "approveResult": "PASS",
  "comment": "同意",
  "variables": {
    "approveResult": "PASS",
    "approveComment": "同意",
    "bossUserId": "10003"
  }
}
```

驳回请求示例：

```json
{
  "targetActivityId": "submitApply",
  "userId": "10002",
  "comment": "申请材料不完整，请补充后重新提交"
}
```

### 审批记录接口

审批记录接口用于查询某个业务单据或流程实例的审批轨迹。审批记录通常来自业务审批记录表，而不是只依赖 Flowable 历史任务表。

推荐接口如下：

| 接口                                                         | 方法  | 说明                             |
| ------------------------------------------------------------ | ----- | -------------------------------- |
| `/api/flow/approval-records`                                 | `GET` | 按业务类型和业务主键查询审批记录 |
| `/api/flow/approval-records/by-instance/{processInstanceId}` | `GET` | 按流程实例 ID 查询审批记录       |
| `/api/flow/approval-records/{id}`                            | `GET` | 查询审批记录详情                 |

审批记录响应字段建议如下：

| 字段                | 说明        |
| ------------------- | ----------- |
| `id`                | 审批记录 ID |
| `businessKey`       | 业务主键    |
| `businessType`      | 业务类型    |
| `processInstanceId` | 流程实例 ID |
| `taskId`            | 任务 ID     |
| `taskName`          | 任务名称    |
| `approverId`        | 审批人 ID   |
| `approverName`      | 审批人名称  |
| `approveAction`     | 审批动作    |
| `approveComment`    | 审批意见    |
| `approveTime`       | 审批时间    |

接口示例：

```http
GET /api/flow/approval-records?businessType=LEAVE&businessKey=1864200000000000001
```

返回示例：

```json
[
  {
    "id": "1864200000000001001",
    "businessKey": "1864200000000000001",
    "businessType": "LEAVE",
    "processInstanceId": "25001",
    "taskId": "25005",
    "taskName": "部门经理审批",
    "approverId": "10002",
    "approverName": "李经理",
    "approveAction": "PASS",
    "approveComment": "同意",
    "approveTime": "2026-05-08 10:30:00"
  }
]
```

审批记录接口应支持按业务单据查询，因为前端通常是在业务详情页展示审批轨迹，而不是先知道流程实例 ID。

### 流程图查看接口

流程图查看接口用于展示 BPMN 流程图、当前节点、高亮历史节点和审批轨迹。常见实现方式有两种：后端生成流程图图片或 SVG；后端返回 BPMN XML 与高亮信息，由前端使用 `bpmn-js` 渲染。

推荐优先使用前端 `bpmn-js` 渲染方式，后端只提供 BPMN XML、已完成节点、当前节点和已走连线。这样前端交互性更好，也便于适配不同 UI 风格。

推荐接口如下：

| 接口                                              | 方法  | 说明                           |
| ------------------------------------------------- | ----- | ------------------------------ |
| `/api/flow/diagram/{processInstanceId}`           | `GET` | 获取流程图展示数据             |
| `/api/flow/diagram/{processInstanceId}/xml`       | `GET` | 获取 BPMN XML                  |
| `/api/flow/diagram/{processInstanceId}/highlight` | `GET` | 获取高亮节点和连线             |
| `/api/flow/diagram/{processInstanceId}/image`     | `GET` | 获取后端生成的流程图图片，可选 |

流程图数据返回结构建议如下：

```json
{
  "processInstanceId": "25001",
  "processDefinitionId": "leaveApprove:1:20004",
  "bpmnXml": "<?xml version=\"1.0\" encoding=\"UTF-8\"?>...",
  "currentActivityIds": [
    "bossApprove"
  ],
  "finishedActivityIds": [
    "startEvent",
    "managerApprove",
    "leaveDaysGateway"
  ],
  "finishedFlowIds": [
    "flow_start_manager",
    "flow_manager_gateway",
    "flow_long_leave"
  ]
}
```

流程图查看接口的核心数据来源如下：

| 数据     | 来源                                                 |
| -------- | ---------------------------------------------------- |
| BPMN XML | `RepositoryService#getResourceAsStream`              |
| 当前节点 | `RuntimeService#getActiveActivityIds`                |
| 历史节点 | `HistoryService#createHistoricActivityInstanceQuery` |
| 已走连线 | 根据历史活动节点和 BPMN 模型计算                     |
| 审批记录 | `flow_approval_record` 业务表                        |

流程图查看接口注意事项：

| 注意事项                       | 说明                                          |
| ------------------------------ | --------------------------------------------- |
| 不建议每次重新计算所有数据     | 可以对流程定义 XML 做缓存                     |
| 当前节点可能有多个             | 并行审批时会同时存在多个当前节点              |
| 历史节点需要按时间排序         | 用于还原流程轨迹                              |
| 已走连线需要结合 BPMN 模型计算 | 不能只查历史活动表                            |
| 前端渲染更灵活                 | 推荐返回 XML 和高亮数组，由前端处理颜色和交互 |

对于后台管理系统，建议流程图接口和审批记录接口配合使用：流程图展示节点进度，审批记录展示具体处理人、处理时间和审批意见。



## 权限与用户体系

权限与用户体系用于解决 Flowable 流程任务和业务系统用户、角色、部门、岗位之间的关系。实际项目中不建议直接依赖 Flowable 自带的 `ACT_ID_*` 身份表作为主用户体系，而应以业务系统已有的用户、角色、部门、岗位、数据权限为准，Flowable 只负责记录任务的办理人、候选人和候选组编码。

推荐做法是：业务系统维护用户和权限，Flowable 使用用户 ID、角色编码、岗位编码或部门编码作为任务分配标识。查询待办任务时，后端根据当前登录用户解析出用户 ID 和角色编码列表，再组合查询 `assignee`、`candidateUser` 和 `candidateGroup` 任务。

### 用户信息集成

用户信息集成用于将业务系统当前登录用户和 Flowable 任务办理人关联起来。Flowable 的任务办理人本质是字符串，通常可以直接使用业务系统用户 ID，例如 `10001`、`admin`、`zhangsan`。

推荐统一使用以下用户标识策略：

| 标识     | 说明                                      | 示例            |
| -------- | ----------------------------------------- | --------------- |
| 用户 ID  | Flowable `assignee`、`candidateUser` 使用 | `10001`         |
| 用户名   | 页面展示使用                              | `张三`          |
| 部门 ID  | 数据权限、审批人计算使用                  | `D001`          |
| 角色编码 | 候选组使用                                | `dept_manager`  |
| 岗位编码 | 候选组或审批规则使用                      | `project_owner` |

用户信息不建议直接从 Flowable 身份表读取，而应通过系统用户服务获取。可以定义一个工作流用户服务接口，用于屏蔽具体权限框架差异，例如 Sa-Token、Spring Security、Shiro 或自研权限系统。

文件位置：`src/main/java/io/github/atengk/flowable/security/FlowUserInfo.java`

```java
package io.github.atengk.flowable.security;

import lombok.Builder;
import lombok.Data;

import java.util.List;

/**
 * 工作流用户信息。
 *
 * @author Ateng
 * @since 2026-05-08
 */
@Data
@Builder
public class FlowUserInfo {

    /**
     * 用户 ID，作为 Flowable assignee 或 candidateUser。
     */
    private String userId;

    /**
     * 用户名称，用于审批记录和页面展示。
     */
    private String userName;

    /**
     * 部门 ID。
     */
    private String deptId;

    /**
     * 角色编码列表，作为 Flowable candidateGroup。
     */
    private List<String> roleCodes;

    /**
     * 岗位编码列表，可作为候选组或审批规则输入。
     */
    private List<String> postCodes;

}
```

下面接口用于抽象当前登录用户、用户详情和上级审批人查询，后续可以由业务系统权限模块实现。

文件位置：`src/main/java/io/github/atengk/flowable/security/FlowUserService.java`

```java
package io.github.atengk.flowable.security;

import java.util.List;

/**
 * 工作流用户服务。
 *
 * @author Ateng
 * @since 2026-05-08
 */
public interface FlowUserService {

    /**
     * 获取当前登录用户。
     *
     * @return 当前登录用户信息
     */
    FlowUserInfo getCurrentUser();

    /**
     * 根据用户 ID 查询用户信息。
     *
     * @param userId 用户 ID
     * @return 用户信息
     */
    FlowUserInfo getByUserId(String userId);

    /**
     * 查询用户角色编码列表。
     *
     * @param userId 用户 ID
     * @return 角色编码列表
     */
    List<String> listRoleCodes(String userId);

    /**
     * 查询直属上级用户 ID。
     *
     * @param userId 用户 ID
     * @return 上级用户 ID
     */
    String getLeaderUserId(String userId);

}
```

下面示例实现用内存数据模拟用户体系，正式项目中应替换为查询用户中心、权限服务或数据库。

文件位置：`src/main/java/io/github/atengk/flowable/security/MockFlowUserServiceImpl.java`

```java
package io.github.atengk.flowable.security;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.map.MapUtil;
import cn.hutool.core.util.StrUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;

/**
 * 模拟工作流用户服务实现。
 *
 * @author Ateng
 * @since 2026-05-08
 */
@Slf4j
@Service
public class MockFlowUserServiceImpl implements FlowUserService {

    private static final Map<String, FlowUserInfo> USER_MAP = MapUtil.<String, FlowUserInfo>builder()
            .put("10001", FlowUserInfo.builder()
                    .userId("10001")
                    .userName("张三")
                    .deptId("D001")
                    .roleCodes(CollUtil.newArrayList("employee"))
                    .postCodes(CollUtil.newArrayList("developer"))
                    .build())
            .put("10002", FlowUserInfo.builder()
                    .userId("10002")
                    .userName("李经理")
                    .deptId("D001")
                    .roleCodes(CollUtil.newArrayList("dept_manager"))
                    .postCodes(CollUtil.newArrayList("manager"))
                    .build())
            .put("10003", FlowUserInfo.builder()
                    .userId("10003")
                    .userName("王总")
                    .deptId("D000")
                    .roleCodes(CollUtil.newArrayList("general_manager"))
                    .postCodes(CollUtil.newArrayList("boss"))
                    .build())
            .build();

    /**
     * 获取当前登录用户。
     *
     * @return 当前登录用户信息
     */
    @Override
    public FlowUserInfo getCurrentUser() {
        /*
         * 正式项目中这里应从 Sa-Token、Spring Security 或网关上下文中获取当前用户。
         * 示例固定返回 10001。
         */
        return getByUserId("10001");
    }

    /**
     * 根据用户 ID 查询用户信息。
     *
     * @param userId 用户 ID
     * @return 用户信息
     */
    @Override
    public FlowUserInfo getByUserId(String userId) {
        if (StrUtil.isBlank(userId)) {
            throw new IllegalArgumentException("用户 ID 不能为空");
        }

        FlowUserInfo userInfo = USER_MAP.get(userId);
        if (userInfo == null) {
            log.warn("用户不存在，用户ID：{}", userId);
            throw new IllegalArgumentException("用户不存在：" + userId);
        }
        return userInfo;
    }

    /**
     * 查询用户角色编码列表。
     *
     * @param userId 用户 ID
     * @return 角色编码列表
     */
    @Override
    public List<String> listRoleCodes(String userId) {
        FlowUserInfo userInfo = getByUserId(userId);
        return CollUtil.emptyIfNull(userInfo.getRoleCodes());
    }

    /**
     * 查询直属上级用户 ID。
     *
     * @param userId 用户 ID
     * @return 上级用户 ID
     */
    @Override
    public String getLeaderUserId(String userId) {
        if (StrUtil.equals(userId, "10001")) {
            return "10002";
        }
        return "10003";
    }

}
```

集成建议如下：

| 建议                     | 说明                                                    |
| ------------------------ | ------------------------------------------------------- |
| Flowable 使用业务用户 ID | `assignee` 和 `candidateUser` 统一保存业务用户 ID       |
| 审批记录保存用户名称     | 避免用户改名后历史记录不可读                            |
| 当前用户从安全上下文获取 | Controller 不应直接信任前端传入的 `userId`              |
| 用户服务独立封装         | 避免流程模块强依赖具体权限框架                          |
| 流程发起人写入 Flowable  | 启动流程时使用 `IdentityService#setAuthenticatedUserId` |

### 角色信息集成

角色信息集成用于支持候选组审批，例如“部门经理审批”“财务审批”“人事审批”等场景。Flowable 的候选组本质也是字符串，推荐使用系统角色编码作为候选组编码。

角色编码建议如下：

| 角色编码          | 说明       |
| ----------------- | ---------- |
| `employee`        | 普通员工   |
| `dept_manager`    | 部门经理   |
| `finance_manager` | 财务经理   |
| `hr_admin`        | 人事管理员 |
| `general_manager` | 总经理     |
| `flow_admin`      | 流程管理员 |

BPMN 中可以直接配置候选组：

```xml
<userTask id="financeApprove"
          name="财务审批"
          flowable:candidateGroups="finance_manager"/>
```

也可以通过变量动态传入候选组：

```xml
<userTask id="financeApprove"
          name="财务审批"
          flowable:candidateGroups="${financeGroupCode}"/>
```

后端查询候选组任务时，需要先查询当前用户拥有的角色编码，再调用 `taskCandidateGroupIn`。

下面代码用于按当前用户和角色编码查询待办任务，适合补充到 `FlowTaskServiceImpl` 中。

```java
List<String> roleCodes = flowUserService.listRoleCodes(userId);

List<Task> candidateGroupTasks = CollUtil.isEmpty(roleCodes)
        ? CollUtil.newArrayList()
        : taskService.createTaskQuery()
                .taskCandidateGroupIn(roleCodes)
                .orderByTaskCreateTime()
                .desc()
                .list();
```

角色集成注意事项：

| 注意事项                     | 说明                                               |
| ---------------------------- | -------------------------------------------------- |
| 使用角色编码，不使用角色名称 | 编码稳定，名称可变                                 |
| 候选组和权限角色保持一致     | 避免流程可见但系统无权限，或系统有权限但查不到任务 |
| 支持多角色查询               | 用户可能同时拥有多个审批角色                       |
| 角色变更影响待办             | 候选组任务实时依赖当前角色，需要明确业务预期       |
| 历史记录保存实际审批人       | 候选组只表示资格，最终审批人仍应落到用户 ID        |

### 候选人配置

候选人配置用于指定某些具体用户拥有处理任务的资格。候选人和办理人不同，候选人需要先领取任务，领取后才成为办理人。

适合使用候选人的场景：

| 场景                   | 示例                           |
| ---------------------- | ------------------------------ |
| 多个固定审批人任选其一 | 财务 A、财务 B 任一人审批      |
| 动态计算审批人列表     | 根据项目成员计算候选审批人     |
| 多人共享待办池         | 客服、运营、财务共享任务       |
| 需要抢单式处理         | 任一候选人领取后其他人不可处理 |

BPMN 固定候选人示例：

```xml
<userTask id="financeApprove"
          name="财务审批"
          flowable:candidateUsers="10010,10011,10012"/>
```

BPMN 动态候选人示例：

```xml
<userTask id="financeApprove"
          name="财务审批"
          flowable:candidateUsers="${financeUserIds}"/>
```

启动流程或完成上一个节点时传入候选人变量：

```java
Map<String, Object> variables = new HashMap<>();
variables.put("financeUserIds", "10010,10011,10012");

taskService.complete(taskId, variables);
```

候选任务领取代码如下：

```java
Task task = taskService.createTaskQuery()
        .taskId(taskId)
        .singleResult();

if (task == null) {
    throw new IllegalArgumentException("任务不存在或已被处理");
}

taskService.claim(taskId, userId);
log.info("任务领取成功，任务ID：{}，用户ID：{}", taskId, userId);
```

候选人配置建议：

| 建议               | 说明                             |
| ------------------ | -------------------------------- |
| 候选人使用用户 ID  | 不使用手机号、姓名等可变字段     |
| 多候选人用逗号分隔 | BPMN 中常见写法为 `10010,10011`  |
| 领取前校验资格     | 防止非候选人越权领取任务         |
| 审批前自动领取     | 简化前端操作，但后端仍需校验权限 |
| 领取后保存审批记录 | 记录谁领取、谁审批，便于追踪     |

### 候选组配置

候选组配置用于将任务分配给角色、部门、岗位或其他组织编码。它比候选人更适合企业审批系统，因为流程通常绑定“角色职责”，而不是绑定固定人员。

候选组可分为以下几类：

| 候选组类型 | 示例                  | 说明                 |
| ---------- | --------------------- | -------------------- |
| 角色候选组 | `finance_manager`     | 财务经理角色可审批   |
| 部门候选组 | `dept:D001`           | 某个部门下用户可审批 |
| 岗位候选组 | `post:project_owner`  | 项目负责人岗位可审批 |
| 自定义组   | `flow:contract_legal` | 合同法务审批组       |

候选组编码建议增加前缀，便于区分来源：

```text
role:finance_manager
dept:D001
post:project_owner
flow:contract_legal
```

BPMN 示例：

```xml
<userTask id="contractLegalApprove"
          name="合同法务审批"
          flowable:candidateGroups="role:legal_manager"/>
```

如果 Flowable 候选组使用了前缀，查询时需要将当前用户的角色、部门、岗位转换为候选组编码。

下面工具类用于构建用户候选组编码。

文件位置：`src/main/java/io/github/atengk/flowable/security/FlowCandidateGroupBuilder.java`

```java
package io.github.atengk.flowable.security;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.StrUtil;

import java.util.ArrayList;
import java.util.List;

/**
 * 工作流候选组构建器。
 *
 * @author Ateng
 * @since 2026-05-08
 */
public final class FlowCandidateGroupBuilder {

    private FlowCandidateGroupBuilder() {
    }

    /**
     * 根据用户信息构建候选组编码。
     *
     * @param userInfo 用户信息
     * @return 候选组编码列表
     */
    public static List<String> buildGroups(FlowUserInfo userInfo) {
        List<String> groups = new ArrayList<>();
        if (userInfo == null) {
            return groups;
        }

        if (StrUtil.isNotBlank(userInfo.getDeptId())) {
            groups.add("dept:" + userInfo.getDeptId());
        }

        CollUtil.emptyIfNull(userInfo.getRoleCodes())
                .forEach(roleCode -> groups.add("role:" + roleCode));

        CollUtil.emptyIfNull(userInfo.getPostCodes())
                .forEach(postCode -> groups.add("post:" + postCode));

        return groups;
    }

}
```

候选组待办查询示例：

```java
FlowUserInfo userInfo = flowUserService.getByUserId(userId);
List<String> candidateGroups = FlowCandidateGroupBuilder.buildGroups(userInfo);

List<Task> groupTasks = CollUtil.isEmpty(candidateGroups)
        ? CollUtil.newArrayList()
        : taskService.createTaskQuery()
                .taskCandidateGroupIn(candidateGroups)
                .orderByTaskCreateTime()
                .desc()
                .list();
```

候选组配置注意事项：

| 注意事项               | 说明                                        |
| ---------------------- | ------------------------------------------- |
| 编码格式要统一         | 例如统一使用 `role:`、`dept:`、`post:` 前缀 |
| 不把中文名称写进 BPMN  | 中文名称变更会影响流程配置                  |
| 多租户需要带租户标识   | 例如 `tenantA:role:finance_manager`         |
| 候选组不等于最终审批人 | 审批记录必须保存实际审批人                  |
| 查询时合并多类候选组   | 用户角色、部门、岗位都可能产生候选任务      |

### 数据权限控制

数据权限控制用于限制用户只能查看和操作自己有权限的流程数据。Flowable 的任务查询只能解决“任务是否分配给当前用户”，不能完全替代业务系统的数据权限。

常见数据权限包括：

| 权限类型     | 说明                                         |
| ------------ | -------------------------------------------- |
| 我的待办     | 当前用户作为办理人、候选人、候选组成员的任务 |
| 我的已办     | 当前用户已经审批过的任务                     |
| 我发起的     | 当前用户发起的流程实例                       |
| 本部门流程   | 当前用户所在部门下的流程                     |
| 管理员流程   | 流程管理员可查看全部流程                     |
| 业务数据权限 | 根据业务表部门、租户、项目等字段控制         |

待办任务权限控制建议：

```text
用户可见待办 =
    assignee = 当前用户
    OR candidateUser = 当前用户
    OR candidateGroup IN 当前用户候选组编码列表
```

业务列表权限控制建议：

```text
用户可见业务数据 =
    发起人 = 当前用户
    OR 当前用户处理过
    OR 当前用户有当前节点处理权限
    OR 当前用户拥有业务数据权限
    OR 当前用户是流程管理员
```

审批操作权限必须在后端校验，不能只依赖前端按钮控制。下面方法可用于判断用户是否有权处理当前任务。

文件位置：`src/main/java/io/github/atengk/flowable/security/FlowTaskPermissionService.java`

```java
package io.github.atengk.flowable.security;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.StrUtil;
import lombok.RequiredArgsConstructor;
import org.flowable.engine.TaskService;
import org.flowable.task.api.Task;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * 流程任务权限服务。
 *
 * @author Ateng
 * @since 2026-05-08
 */
@Service
@RequiredArgsConstructor
public class FlowTaskPermissionService {

    private final TaskService taskService;
    private final FlowUserService flowUserService;

    /**
     * 判断用户是否可以办理任务。
     *
     * @param taskId 任务 ID
     * @param userId 用户 ID
     * @return 是否可以办理
     */
    public boolean canHandleTask(String taskId, String userId) {
        if (StrUtil.hasBlank(taskId, userId)) {
            return false;
        }

        Task task = taskService.createTaskQuery()
                .taskId(taskId)
                .singleResult();

        if (task == null) {
            return false;
        }

        if (StrUtil.equals(task.getAssignee(), userId)) {
            return true;
        }

        long candidateUserCount = taskService.createTaskQuery()
                .taskId(taskId)
                .taskCandidateUser(userId)
                .count();
        if (candidateUserCount > 0) {
            return true;
        }

        FlowUserInfo userInfo = flowUserService.getByUserId(userId);
        List<String> groups = FlowCandidateGroupBuilder.buildGroups(userInfo);
        if (CollUtil.isEmpty(groups)) {
            return false;
        }

        long candidateGroupCount = taskService.createTaskQuery()
                .taskId(taskId)
                .taskCandidateGroupIn(groups)
                .count();

        return candidateGroupCount > 0;
    }

}
```

在任务审批前调用：

```java
boolean canHandle = flowTaskPermissionService.canHandleTask(dto.getTaskId(), dto.getUserId());
if (!canHandle) {
    throw new IllegalStateException("当前用户无权办理该任务");
}
```

数据权限控制建议：

| 建议                       | 说明                                       |
| -------------------------- | ------------------------------------------ |
| 后端强制校验               | 前端按钮隐藏不能替代后端权限校验           |
| 任务权限和业务权限都要校验 | 有任务权限不代表有业务数据查看权限         |
| 管理员权限单独处理         | 管理员可查看流程，但审批操作仍需谨慎       |
| 操作记录保留用户信息       | 权限问题排查依赖审计记录                   |
| 多租户必须隔离             | 流程定义、流程实例、业务表都要考虑租户字段 |

## 监听器与扩展

监听器与扩展用于在流程运行过程中插入业务逻辑，例如流程启动时初始化数据、任务创建时发送通知、任务完成时保存审批记录、流程结束时更新业务状态、特定条件下自动审批等。

Flowable 常用监听器包括执行监听器和任务监听器。执行监听器关注流程实例和节点执行过程，任务监听器关注用户任务的创建、分配、完成等生命周期事件。

### 执行监听器

执行监听器用于监听流程或节点的执行事件，例如流程开始、流程结束、连线流转、节点进入、节点离开等。常见用途包括更新业务状态、记录流程日志、发送流程结束通知等。

常见执行事件如下：

| 事件             | 说明           |
| ---------------- | -------------- |
| `start`          | 流程或节点开始 |
| `end`            | 流程或节点结束 |
| `take`           | 连线被执行     |
| `start` + 流程级 | 流程实例启动   |
| `end` + 流程级   | 流程实例结束   |

推荐使用 `delegateExpression` 方式配置监听器，让监听器类交给 Spring 容器管理，这样可以注入业务 Service、Mapper、消息服务等 Bean。

BPMN 配置示例：

```xml
<process id="leaveApprove" name="请假审批流程" isExecutable="true">
    <extensionElements>
        <!-- 流程结束时触发，用于更新业务状态和发送通知 -->
        <flowable:executionListener event="end"
                                    delegateExpression="${processEndExecutionListener}"/>
    </extensionElements>

    <!-- 其他流程节点省略 -->
</process>
```

下面监听器用于在流程正常结束时输出日志，并预留业务状态更新和消息通知扩展点。

文件位置：`src/main/java/io/github/atengk/flowable/listener/ProcessEndExecutionListener.java`

```java
package io.github.atengk.flowable.listener;

import cn.hutool.core.util.StrUtil;
import io.github.atengk.flowable.common.enums.FlowApprovalStatusEnum;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.flowable.engine.delegate.DelegateExecution;
import org.flowable.engine.delegate.ExecutionListener;
import org.springframework.stereotype.Component;

/**
 * 流程结束执行监听器。
 *
 * @author Ateng
 * @since 2026-05-08
 */
@Slf4j
@Component("processEndExecutionListener")
@RequiredArgsConstructor
public class ProcessEndExecutionListener implements ExecutionListener {

    /**
     * 流程结束事件处理。
     *
     * @param execution 执行上下文
     */
    @Override
    public void notify(DelegateExecution execution) {
        String processInstanceId = execution.getProcessInstanceId();
        String processDefinitionId = execution.getProcessDefinitionId();
        String businessKey = execution.getProcessInstanceBusinessKey();

        if (StrUtil.isBlank(businessKey)) {
            log.warn("流程结束但业务主键为空，流程实例ID：{}，流程定义ID：{}",
                    processInstanceId, processDefinitionId);
            return;
        }

        /*
         * 正式项目中这里建议执行：
         * 1. 查询 flow_business 记录
         * 2. 判断是否为正常通过结束，更新状态为 APPROVED
         * 3. 更新具体业务表状态
         * 4. 保存流程结束记录
         * 5. 通知发起人流程已结束
         */
        log.info("流程正常结束，流程实例ID：{}，流程定义ID：{}，业务主键：{}，目标状态：{}",
                processInstanceId,
                processDefinitionId,
                businessKey,
                FlowApprovalStatusEnum.APPROVED.getCode());
    }

}
```

执行监听器注意事项：

| 注意事项                      | 说明                               |
| ----------------------------- | ---------------------------------- |
| 推荐使用 `delegateExpression` | 便于注入 Spring Bean               |
| 监听器逻辑保持轻量            | 不要在监听器中写大量复杂业务       |
| 注意事务边界                  | 监听器通常和流程操作处于同一事务中 |
| 异常会影响流程流转            | 监听器抛异常可能导致任务完成失败   |
| 关键操作要有日志              | 流程问题排查依赖监听器日志         |

### 任务监听器

任务监听器用于监听用户任务生命周期事件，例如任务创建、分配、完成、删除等。它比执行监听器更适合处理人工审批相关扩展，例如通知审批人、记录任务创建日志、动态设置审批人等。

常见任务事件如下：

| 事件         | 说明     |
| ------------ | -------- |
| `create`     | 任务创建 |
| `assignment` | 任务分配 |
| `complete`   | 任务完成 |
| `delete`     | 任务删除 |

BPMN 配置示例：

```xml
<userTask id="managerApprove"
          name="部门经理审批"
          flowable:assignee="${managerUserId}">
    <extensionElements>
        <!-- 任务创建时发送待办通知 -->
        <flowable:taskListener event="create"
                               delegateExpression="${approvalTaskCreateListener}"/>

        <!-- 任务完成时记录扩展日志 -->
        <flowable:taskListener event="complete"
                               delegateExpression="${approvalTaskCompleteListener}"/>
    </extensionElements>
</userTask>
```

下面监听器用于任务创建时发送待办消息通知。

文件位置：`src/main/java/io/github/atengk/flowable/listener/ApprovalTaskCreateListener.java`

```java
package io.github.atengk.flowable.listener;

import cn.hutool.core.util.StrUtil;
import io.github.atengk.flowable.message.FlowMessageService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.flowable.task.service.delegate.DelegateTask;
import org.flowable.task.service.delegate.TaskListener;
import org.springframework.stereotype.Component;

/**
 * 审批任务创建监听器。
 *
 * @author Ateng
 * @since 2026-05-08
 */
@Slf4j
@Component("approvalTaskCreateListener")
@RequiredArgsConstructor
public class ApprovalTaskCreateListener implements TaskListener {

    private final FlowMessageService flowMessageService;

    /**
     * 任务创建事件处理。
     *
     * @param delegateTask 任务上下文
     */
    @Override
    public void notify(DelegateTask delegateTask) {
        String taskId = delegateTask.getId();
        String taskName = delegateTask.getName();
        String assignee = delegateTask.getAssignee();
        String processInstanceId = delegateTask.getProcessInstanceId();

        log.info("审批任务已创建，任务ID：{}，任务名称：{}，流程实例ID：{}，办理人：{}",
                taskId, taskName, processInstanceId, assignee);

        if (StrUtil.isNotBlank(assignee)) {
            flowMessageService.sendTodoMessage(assignee, taskId, taskName, processInstanceId);
        }
    }

}
```

下面监听器用于任务完成时输出关键日志，并预留审批记录扩展点。

文件位置：`src/main/java/io/github/atengk/flowable/listener/ApprovalTaskCompleteListener.java`

```java
package io.github.atengk.flowable.listener;

import cn.hutool.core.util.StrUtil;
import lombok.extern.slf4j.Slf4j;
import org.flowable.task.service.delegate.DelegateTask;
import org.flowable.task.service.delegate.TaskListener;
import org.springframework.stereotype.Component;

/**
 * 审批任务完成监听器。
 *
 * @author Ateng
 * @since 2026-05-08
 */
@Slf4j
@Component("approvalTaskCompleteListener")
public class ApprovalTaskCompleteListener implements TaskListener {

    /**
     * 任务完成事件处理。
     *
     * @param delegateTask 任务上下文
     */
    @Override
    public void notify(DelegateTask delegateTask) {
        String approveResult = StrUtil.toString(delegateTask.getVariable("approveResult"));
        String approveComment = StrUtil.toString(delegateTask.getVariable("approveComment"));

        /*
         * 正式项目中这里可以补充：
         * 1. 保存审批记录
         * 2. 更新 flow_business 当前节点信息
         * 3. 发送下一节点通知
         */
        log.info("审批任务已完成，任务ID：{}，任务名称：{}，办理人：{}，审批结果：{}，审批意见：{}",
                delegateTask.getId(),
                delegateTask.getName(),
                delegateTask.getAssignee(),
                approveResult,
                approveComment);
    }

}
```

任务监听器使用建议：

| 建议                        | 说明                             |
| --------------------------- | -------------------------------- |
| `create` 事件发送待办通知   | 新任务生成后通知审批人           |
| `complete` 事件保存扩展日志 | 记录任务完成时的变量和处理人     |
| 动态审批人优先在启动前计算  | 复杂场景再使用监听器动态分配     |
| 不在监听器中吞异常          | 异常应被日志记录并按业务策略处理 |
| 监听器逻辑避免过重          | 复杂逻辑封装到 Service 中调用    |

### 自定义审批逻辑

自定义审批逻辑用于处理 BPMN 配置无法简单表达的业务规则，例如根据金额、部门、岗位、申请人层级、项目负责人等动态计算审批人。

推荐把审批规则封装为独立服务，而不是把复杂逻辑写在 Controller、监听器或 BPMN 表达式中。

文件位置：`src/main/java/io/github/atengk/flowable/approval/ApprovalUserResolver.java`

```java
package io.github.atengk.flowable.approval;

import java.util.Map;

/**
 * 审批人解析器。
 *
 * @author Ateng
 * @since 2026-05-08
 */
public interface ApprovalUserResolver {

    /**
     * 解析审批人变量。
     *
     * @param businessType 业务类型
     * @param applyUserId 申请人用户 ID
     * @param businessData 业务数据
     * @return 审批人相关流程变量
     */
    Map<String, Object> resolve(String businessType, String applyUserId, Map<String, Object> businessData);

}
```

下面实现类演示根据请假天数动态设置部门经理和总经理审批人。

文件位置：`src/main/java/io/github/atengk/flowable/approval/DefaultApprovalUserResolver.java`

```java
package io.github.atengk.flowable.approval;

import cn.hutool.core.convert.Convert;
import cn.hutool.core.map.MapUtil;
import cn.hutool.core.util.StrUtil;
import io.github.atengk.flowable.common.constant.FlowVariableConstants;
import io.github.atengk.flowable.security.FlowUserService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.Map;

/**
 * 默认审批人解析器。
 *
 * @author Ateng
 * @since 2026-05-08
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class DefaultApprovalUserResolver implements ApprovalUserResolver {

    private final FlowUserService flowUserService;

    /**
     * 解析审批人变量。
     *
     * @param businessType 业务类型
     * @param applyUserId 申请人用户 ID
     * @param businessData 业务数据
     * @return 审批人相关流程变量
     */
    @Override
    public Map<String, Object> resolve(String businessType, String applyUserId, Map<String, Object> businessData) {
        if (StrUtil.hasBlank(businessType, applyUserId)) {
            throw new IllegalArgumentException("业务类型和申请人不能为空");
        }

        Integer leaveDays = Convert.toInt(MapUtil.get(businessData, "leaveDays", Integer.class), 0);
        String managerUserId = flowUserService.getLeaderUserId(applyUserId);

        MapUtil.MapBuilder<String, Object> builder = MapUtil.<String, Object>builder()
                .put(FlowVariableConstants.APPLY_USER_ID, applyUserId)
                .put(FlowVariableConstants.MANAGER_USER_ID, managerUserId)
                .put(FlowVariableConstants.LEAVE_DAYS, leaveDays);

        if (leaveDays > 3) {
            String bossUserId = flowUserService.getLeaderUserId(managerUserId);
            builder.put(FlowVariableConstants.BOSS_USER_ID, bossUserId);
        }

        Map<String, Object> variables = builder.build();
        log.info("审批人解析完成，业务类型：{}，申请人：{}，流程变量：{}",
                businessType, applyUserId, variables);
        return variables;
    }

}
```

自定义审批逻辑建议：

| 建议               | 说明                               |
| ------------------ | ---------------------------------- |
| 审批人计算前置     | 启动流程或进入节点前准备好变量     |
| 规则服务独立封装   | 便于单元测试和复用                 |
| BPMN 只保留结构    | 避免在 BPMN 表达式中写复杂业务规则 |
| 规则结果落日志     | 审批人分配错误时便于排查           |
| 关键结果写审批记录 | 审批人来源、规则命中情况可按需记录 |

### 自动审批处理

自动审批处理用于在特定条件下自动完成任务，例如申请人和审批人相同、金额低于阈值、重复审批人跳过、系统管理员预审批等场景。

常见自动审批场景如下：

| 场景                   | 说明                                 |
| ---------------------- | ------------------------------------ |
| 申请人与审批人相同     | 自动通过当前节点                     |
| 连续节点审批人为同一人 | 后续节点自动跳过或自动通过           |
| 小额报销               | 金额低于阈值自动通过                 |
| 系统任务               | 某些节点不需要人工参与               |
| 超时默认处理           | 到期后自动通过或自动驳回，需谨慎使用 |

自动审批可以在任务创建监听器中触发，也可以在流程提交后由业务服务主动检查当前任务。推荐做法是封装独立自动审批服务，监听器只调用服务，不直接写复杂规则。

文件位置：`src/main/java/io/github/atengk/flowable/approval/AutoApprovalService.java`

```java
package io.github.atengk.flowable.approval;

/**
 * 自动审批服务。
 *
 * @author Ateng
 * @since 2026-05-08
 */
public interface AutoApprovalService {

    /**
     * 尝试自动审批任务。
     *
     * @param taskId 任务 ID
     */
    void tryAutoApprove(String taskId);

}
```

下面实现类演示“申请人和当前审批人相同则自动通过”的处理方式。

文件位置：`src/main/java/io/github/atengk/flowable/approval/DefaultAutoApprovalServiceImpl.java`

```java
package io.github.atengk.flowable.approval;

import cn.hutool.core.map.MapUtil;
import cn.hutool.core.util.StrUtil;
import io.github.atengk.flowable.common.constant.FlowVariableConstants;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.flowable.engine.TaskService;
import org.flowable.task.api.Task;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Map;

/**
 * 默认自动审批服务实现。
 *
 * @author Ateng
 * @since 2026-05-08
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class DefaultAutoApprovalServiceImpl implements AutoApprovalService {

    private final TaskService taskService;

    /**
     * 尝试自动审批任务。
     *
     * @param taskId 任务 ID
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void tryAutoApprove(String taskId) {
        if (StrUtil.isBlank(taskId)) {
            return;
        }

        Task task = taskService.createTaskQuery()
                .taskId(taskId)
                .includeProcessVariables()
                .singleResult();

        if (task == null) {
            log.warn("自动审批跳过，任务不存在，任务ID：{}", taskId);
            return;
        }

        String assignee = task.getAssignee();
        String applyUserId = StrUtil.toString(task.getProcessVariables().get(FlowVariableConstants.APPLY_USER_ID));

        if (StrUtil.isBlank(assignee) || StrUtil.isBlank(applyUserId)) {
            log.info("自动审批跳过，办理人或申请人为空，任务ID：{}，办理人：{}，申请人：{}",
                    taskId, assignee, applyUserId);
            return;
        }

        if (!StrUtil.equals(assignee, applyUserId)) {
            log.info("自动审批条件不满足，任务ID：{}，办理人：{}，申请人：{}",
                    taskId, assignee, applyUserId);
            return;
        }

        Map<String, Object> variables = MapUtil.<String, Object>builder()
                .put(FlowVariableConstants.APPROVE_RESULT, "PASS")
                .put(FlowVariableConstants.APPROVE_COMMENT, "申请人与审批人相同，系统自动通过")
                .build();

        taskService.addComment(taskId, task.getProcessInstanceId(), "申请人与审批人相同，系统自动通过");
        taskService.complete(taskId, variables);

        log.info("自动审批完成，任务ID：{}，流程实例ID：{}，审批人：{}",
                taskId, task.getProcessInstanceId(), assignee);
    }

}
```

在任务创建监听器中调用自动审批服务：

```java
autoApprovalService.tryAutoApprove(delegateTask.getId());
```

自动审批注意事项：

| 注意事项           | 说明                                       |
| ------------------ | ------------------------------------------ |
| 自动审批必须可追踪 | 审批记录中应标记为系统自动审批             |
| 谨慎自动驳回       | 自动驳回影响较大，需要明确业务规则         |
| 防止递归触发       | 自动完成任务可能继续创建新任务，需控制循环 |
| 保留规则命中日志   | 便于审计和问题排查                         |
| 规则应可配置       | 金额阈值、跳过条件不建议写死               |

### 消息通知扩展

消息通知扩展用于在流程运行过程中通知相关用户，例如待办任务通知、审批结果通知、驳回通知、撤回通知、流程结束通知等。

常见通知渠道包括：

| 渠道            | 说明                       |
| --------------- | -------------------------- |
| 系统站内信      | 后台系统内通知             |
| 邮件            | 审批提醒、流程完成通知     |
| 企业微信 / 钉钉 | 企业办公场景常用           |
| 短信            | 紧急审批或关键业务         |
| WebSocket       | 实时刷新待办数量           |
| MQ              | 异步通知，降低流程事务耗时 |

推荐先定义统一消息服务接口，再由不同渠道实现。

文件位置：`src/main/java/io/github/atengk/flowable/message/FlowMessageService.java`

```java
package io.github.atengk.flowable.message;

/**
 * 流程消息服务。
 *
 * @author Ateng
 * @since 2026-05-08
 */
public interface FlowMessageService {

    /**
     * 发送待办消息。
     *
     * @param receiverUserId 接收人用户 ID
     * @param taskId 任务 ID
     * @param taskName 任务名称
     * @param processInstanceId 流程实例 ID
     */
    void sendTodoMessage(String receiverUserId, String taskId, String taskName, String processInstanceId);

    /**
     * 发送审批结果消息。
     *
     * @param receiverUserId 接收人用户 ID
     * @param businessKey 业务主键
     * @param approveResult 审批结果
     * @param comment 审批意见
     */
    void sendApproveResultMessage(String receiverUserId, String businessKey, String approveResult, String comment);

}
```

下面实现类先使用日志模拟消息发送，正式项目中可以替换为站内信、MQ、企业微信或邮件发送。

文件位置：`src/main/java/io/github/atengk/flowable/message/LogFlowMessageServiceImpl.java`

```java
package io.github.atengk.flowable.message;

import cn.hutool.core.util.StrUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

/**
 * 日志型流程消息服务实现。
 *
 * @author Ateng
 * @since 2026-05-08
 */
@Slf4j
@Service
public class LogFlowMessageServiceImpl implements FlowMessageService {

    /**
     * 发送待办消息。
     *
     * @param receiverUserId 接收人用户 ID
     * @param taskId 任务 ID
     * @param taskName 任务名称
     * @param processInstanceId 流程实例 ID
     */
    @Override
    public void sendTodoMessage(String receiverUserId, String taskId, String taskName, String processInstanceId) {
        if (StrUtil.hasBlank(receiverUserId, taskId)) {
            log.warn("待办消息发送跳过，接收人或任务ID为空，接收人：{}，任务ID：{}", receiverUserId, taskId);
            return;
        }

        log.info("发送待办消息，接收人：{}，任务ID：{}，任务名称：{}，流程实例ID：{}",
                receiverUserId, taskId, taskName, processInstanceId);
    }

    /**
     * 发送审批结果消息。
     *
     * @param receiverUserId 接收人用户 ID
     * @param businessKey 业务主键
     * @param approveResult 审批结果
     * @param comment 审批意见
     */
    @Override
    public void sendApproveResultMessage(String receiverUserId, String businessKey, String approveResult, String comment) {
        if (StrUtil.hasBlank(receiverUserId, businessKey)) {
            log.warn("审批结果消息发送跳过，接收人或业务主键为空，接收人：{}，业务主键：{}",
                    receiverUserId, businessKey);
            return;
        }

        log.info("发送审批结果消息，接收人：{}，业务主键：{}，审批结果：{}，审批意见：{}",
                receiverUserId, businessKey, approveResult, comment);
    }

}
```

消息通知建议异步化，避免外部消息服务异常影响流程审批主事务。可以采用以下方式：

| 方式        | 说明                                 |
| ----------- | ------------------------------------ |
| Spring 事件 | 简单项目可用，解耦流程逻辑和通知逻辑 |
| MQ 消息     | 中大型系统推荐，可靠性和扩展性更好   |
| 事务后事件  | 避免事务回滚后消息已发送             |
| 定时补偿    | 处理消息发送失败场景                 |
| WebSocket   | 用于实时刷新前端待办数量             |

如果使用 Spring 事件，审批服务只发布事件，监听器异步发送消息。这样可以减少流程操作和消息发送之间的耦合。

文件位置：`src/main/java/io/github/atengk/flowable/message/FlowTodoMessageEvent.java`

```java
package io.github.atengk.flowable.message;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * 流程待办消息事件。
 *
 * @author Ateng
 * @since 2026-05-08
 */
@Getter
@AllArgsConstructor
public class FlowTodoMessageEvent {

    /**
     * 接收人用户 ID。
     */
    private final String receiverUserId;

    /**
     * 任务 ID。
     */
    private final String taskId;

    /**
     * 任务名称。
     */
    private final String taskName;

    /**
     * 流程实例 ID。
     */
    private final String processInstanceId;

}
```

文件位置：`src/main/java/io/github/atengk/flowable/message/FlowMessageEventListener.java`

```java
package io.github.atengk.flowable.message;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

/**
 * 流程消息事件监听器。
 *
 * @author Ateng
 * @since 2026-05-08
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class FlowMessageEventListener {

    private final FlowMessageService flowMessageService;

    /**
     * 处理流程待办消息事件。
     *
     * @param event 待办消息事件
     */
    @EventListener
    public void handleTodoMessage(FlowTodoMessageEvent event) {
        log.info("接收到流程待办消息事件，接收人：{}，任务ID：{}",
                event.getReceiverUserId(), event.getTaskId());

        flowMessageService.sendTodoMessage(
                event.getReceiverUserId(),
                event.getTaskId(),
                event.getTaskName(),
                event.getProcessInstanceId()
        );
    }

}
```

消息通知扩展注意事项：

| 注意事项                     | 说明                                             |
| ---------------------------- | ------------------------------------------------ |
| 不在流程事务中直接调用慢接口 | 邮件、企业微信、短信建议异步发送                 |
| 消息内容保存业务主键         | 便于用户点击后跳转业务详情                       |
| 消息发送失败要有补偿         | 可通过消息表、MQ 重试或定时任务处理              |
| 通知对象要准确               | 候选组任务可能没有固定办理人，需要通知候选组用户 |
| 注意敏感信息                 | 邮件和外部 IM 不应暴露敏感审批内容               |



## 流程图与进度展示

流程图与进度展示用于让用户直观看到审批流程当前走到哪个节点、哪些节点已经处理、哪些连线已经经过，以及每个审批节点的处理人、处理时间和审批意见。后端通常负责提供 BPMN XML、当前节点、历史节点、已走连线和审批记录，前端负责使用 `bpmn-js` 或自定义流程图组件完成渲染和高亮。

推荐实现方式是：后端不直接生成图片，而是返回 BPMN XML 和高亮数据。这样前端可以灵活控制节点颜色、线条颜色、审批弹窗、节点点击事件和页面样式。

### BPMN XML 解析

BPMN XML 解析用于读取流程定义文件内容，并返回给前端进行流程图渲染。对于已经部署到 Flowable 的流程定义，可以通过 `RepositoryService` 根据流程定义 ID 读取部署资源。

流程图展示常用数据来源如下：

| 数据         | 来源                                                 |
| ------------ | ---------------------------------------------------- |
| BPMN XML     | `RepositoryService#getResourceAsStream`              |
| BPMN 模型    | `RepositoryService#getBpmnModel`                     |
| 流程定义     | `RepositoryService#createProcessDefinitionQuery`     |
| 运行中实例   | `RuntimeService#createProcessInstanceQuery`          |
| 当前活动节点 | `RuntimeService#getActiveActivityIds`                |
| 历史活动节点 | `HistoryService#createHistoricActivityInstanceQuery` |
| 审批记录     | 业务表 `flow_approval_record`                        |

流程图响应对象如下，用于统一返回 XML、当前节点、历史节点和已走连线。

文件位置：`src/main/java/io/github/atengk/flowable/vo/ProcessDiagramVO.java`

```java
package io.github.atengk.flowable.vo;

import lombok.Builder;
import lombok.Data;

import java.util.List;

/**
 * 流程图响应对象。
 *
 * @author Ateng
 * @since 2026-05-08
 */
@Data
@Builder
public class ProcessDiagramVO {

    /**
     * 流程实例 ID。
     */
    private String processInstanceId;

    /**
     * 流程定义 ID。
     */
    private String processDefinitionId;

    /**
     * BPMN XML 内容。
     */
    private String bpmnXml;

    /**
     * 当前活动节点 ID 列表。
     */
    private List<String> currentActivityIds;

    /**
     * 已完成节点 ID 列表。
     */
    private List<String> finishedActivityIds;

    /**
     * 已走连线 ID 列表。
     */
    private List<String> finishedFlowIds;

}
```

下面的服务接口用于提供流程图展示数据。

文件位置：`src/main/java/io/github/atengk/flowable/service/FlowDiagramService.java`

```java
package io.github.atengk.flowable.service;

import io.github.atengk.flowable.vo.ProcessDiagramVO;

/**
 * 流程图服务。
 *
 * @author Ateng
 * @since 2026-05-08
 */
public interface FlowDiagramService {

    /**
     * 获取流程图展示数据。
     *
     * @param processInstanceId 流程实例 ID
     * @return 流程图展示数据
     */
    ProcessDiagramVO getDiagram(String processInstanceId);

}
```

下面实现类用于读取 BPMN XML、当前节点、历史节点和历史连线，适合提供给前端流程图页面使用。

文件位置：`src/main/java/io/github/atengk/flowable/service/impl/FlowDiagramServiceImpl.java`

```java
package io.github.atengk.flowable.service.impl;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.io.IoUtil;
import cn.hutool.core.util.StrUtil;
import io.github.atengk.flowable.service.FlowDiagramService;
import io.github.atengk.flowable.vo.ProcessDiagramVO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.flowable.common.engine.api.FlowableObjectNotFoundException;
import org.flowable.engine.HistoryService;
import org.flowable.engine.RepositoryService;
import org.flowable.engine.RuntimeService;
import org.flowable.engine.history.HistoricActivityInstance;
import org.flowable.engine.history.HistoricProcessInstance;
import org.flowable.engine.repository.ProcessDefinition;
import org.flowable.engine.runtime.ProcessInstance;
import org.springframework.stereotype.Service;

import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

/**
 * 流程图服务实现。
 *
 * @author Ateng
 * @since 2026-05-08
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class FlowDiagramServiceImpl implements FlowDiagramService {

    private static final String ACTIVITY_TYPE_SEQUENCE_FLOW = "sequenceFlow";

    private final RepositoryService repositoryService;
    private final RuntimeService runtimeService;
    private final HistoryService historyService;

    /**
     * 获取流程图展示数据。
     *
     * @param processInstanceId 流程实例 ID
     * @return 流程图展示数据
     */
    @Override
    public ProcessDiagramVO getDiagram(String processInstanceId) {
        if (StrUtil.isBlank(processInstanceId)) {
            throw new IllegalArgumentException("流程实例 ID 不能为空");
        }

        String processDefinitionId = getProcessDefinitionId(processInstanceId);
        String bpmnXml = getBpmnXml(processDefinitionId);
        List<String> currentActivityIds = getCurrentActivityIds(processInstanceId);
        List<HistoricActivityInstance> historicActivityList = getHistoricActivityList(processInstanceId);

        List<String> finishedActivityIds = historicActivityList.stream()
                .filter(item -> StrUtil.isNotBlank(item.getActivityId()))
                .filter(item -> !StrUtil.equals(item.getActivityType(), ACTIVITY_TYPE_SEQUENCE_FLOW))
                .filter(item -> item.getEndTime() != null)
                .map(HistoricActivityInstance::getActivityId)
                .distinct()
                .collect(Collectors.toList());

        List<String> finishedFlowIds = historicActivityList.stream()
                .filter(item -> StrUtil.equals(item.getActivityType(), ACTIVITY_TYPE_SEQUENCE_FLOW))
                .map(HistoricActivityInstance::getActivityId)
                .filter(StrUtil::isNotBlank)
                .distinct()
                .collect(Collectors.toList());

        log.info("流程图数据查询完成，流程实例ID：{}，流程定义ID：{}，当前节点数：{}，历史节点数：{}，历史连线数：{}",
                processInstanceId,
                processDefinitionId,
                currentActivityIds.size(),
                finishedActivityIds.size(),
                finishedFlowIds.size());

        return ProcessDiagramVO.builder()
                .processInstanceId(processInstanceId)
                .processDefinitionId(processDefinitionId)
                .bpmnXml(bpmnXml)
                .currentActivityIds(currentActivityIds)
                .finishedActivityIds(finishedActivityIds)
                .finishedFlowIds(finishedFlowIds)
                .build();
    }

    /**
     * 获取流程定义 ID。
     *
     * @param processInstanceId 流程实例 ID
     * @return 流程定义 ID
     */
    private String getProcessDefinitionId(String processInstanceId) {
        ProcessInstance runtimeInstance = runtimeService.createProcessInstanceQuery()
                .processInstanceId(processInstanceId)
                .singleResult();

        if (runtimeInstance != null) {
            return runtimeInstance.getProcessDefinitionId();
        }

        HistoricProcessInstance historicInstance = historyService.createHistoricProcessInstanceQuery()
                .processInstanceId(processInstanceId)
                .singleResult();

        if (historicInstance != null) {
            return historicInstance.getProcessDefinitionId();
        }

        throw new FlowableObjectNotFoundException("流程实例不存在：" + processInstanceId);
    }

    /**
     * 获取 BPMN XML 内容。
     *
     * @param processDefinitionId 流程定义 ID
     * @return BPMN XML 内容
     */
    private String getBpmnXml(String processDefinitionId) {
        ProcessDefinition processDefinition = repositoryService.createProcessDefinitionQuery()
                .processDefinitionId(processDefinitionId)
                .singleResult();

        if (processDefinition == null) {
            throw new FlowableObjectNotFoundException("流程定义不存在：" + processDefinitionId);
        }

        try (InputStream inputStream = repositoryService.getResourceAsStream(
                processDefinition.getDeploymentId(),
                processDefinition.getResourceName())) {
            if (inputStream == null) {
                throw new FlowableObjectNotFoundException("流程定义资源不存在：" + processDefinition.getResourceName());
            }
            return IoUtil.read(inputStream, StandardCharsets.UTF_8);
        } catch (FlowableObjectNotFoundException e) {
            throw e;
        } catch (Exception e) {
            log.error("读取 BPMN XML 失败，流程定义ID：{}", processDefinitionId, e);
            throw new IllegalStateException("读取 BPMN XML 失败");
        }
    }

    /**
     * 获取当前活动节点 ID。
     *
     * @param processInstanceId 流程实例 ID
     * @return 当前活动节点 ID 列表
     */
    private List<String> getCurrentActivityIds(String processInstanceId) {
        ProcessInstance runtimeInstance = runtimeService.createProcessInstanceQuery()
                .processInstanceId(processInstanceId)
                .singleResult();

        if (runtimeInstance == null) {
            return CollUtil.newArrayList();
        }

        List<String> activityIds = runtimeService.getActiveActivityIds(processInstanceId);
        return activityIds == null ? CollUtil.newArrayList() : activityIds;
    }

    /**
     * 获取历史活动节点。
     *
     * @param processInstanceId 流程实例 ID
     * @return 历史活动节点列表
     */
    private List<HistoricActivityInstance> getHistoricActivityList(String processInstanceId) {
        return historyService.createHistoricActivityInstanceQuery()
                .processInstanceId(processInstanceId)
                .orderByHistoricActivityInstanceStartTime()
                .asc()
                .list()
                .stream()
                .filter(Objects::nonNull)
                .collect(Collectors.toList());
    }

}
```

流程图接口 Controller 示例：

文件位置：`src/main/java/io/github/atengk/flowable/controller/FlowDiagramController.java`

```java
package io.github.atengk.flowable.controller;

import io.github.atengk.flowable.service.FlowDiagramService;
import io.github.atengk.flowable.vo.ProcessDiagramVO;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

/**
 * 流程图查看接口。
 *
 * @author Ateng
 * @since 2026-05-08
 */
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/flow/diagram")
public class FlowDiagramController {

    private final FlowDiagramService flowDiagramService;

    /**
     * 获取流程图展示数据。
     *
     * @param processInstanceId 流程实例 ID
     * @return 流程图展示数据
     */
    @GetMapping("/{processInstanceId}")
    public ProcessDiagramVO getDiagram(@PathVariable String processInstanceId) {
        return flowDiagramService.getDiagram(processInstanceId);
    }

}
```

接口调用示例：

```bash
curl -X GET "http://localhost:8080/api/flow/diagram/25001"
```

返回示例：

```json
{
  "processInstanceId": "25001",
  "processDefinitionId": "leaveApprove:1:20004",
  "bpmnXml": "<?xml version=\"1.0\" encoding=\"UTF-8\"?>...",
  "currentActivityIds": [
    "bossApprove"
  ],
  "finishedActivityIds": [
    "startEvent",
    "managerApprove",
    "leaveDaysGateway"
  ],
  "finishedFlowIds": [
    "flow_start_manager",
    "flow_manager_gateway",
    "flow_long_leave"
  ]
}
```

### 当前节点高亮

当前节点高亮用于展示流程正在等待处理的节点。对于串行审批，当前节点通常只有一个；对于并行审批，当前节点可能有多个，例如法务审批和财务审批同时进行。

当前节点查询核心代码如下：

```java
List<String> currentActivityIds = runtimeService.getActiveActivityIds(processInstanceId);
```

当前节点高亮建议前端使用固定颜色，例如橙色或蓝色，和历史完成节点区分开：

| 节点类型   | 建议展示           |
| ---------- | ------------------ |
| 当前节点   | 高亮边框或高亮背景 |
| 已完成节点 | 绿色或完成标识     |
| 未经过节点 | 默认样式           |
| 驳回节点   | 红色或警告标识     |
| 终止节点   | 灰色或终止标识     |

当前节点可能为空，常见原因如下：

| 场景             | 说明                             |
| ---------------- | -------------------------------- |
| 流程已正常结束   | 已经走到结束事件                 |
| 流程已终止       | 运行时实例已删除                 |
| 流程实例不存在   | 参数错误或数据已清理             |
| 流程处于异步等待 | 需要结合运行时作业和历史数据判断 |

因此流程图接口不能只依赖当前节点判断流程是否存在，应优先通过运行时实例和历史实例同时查询。

### 历史节点高亮

历史节点高亮用于展示流程已经走过哪些节点。后端通常从 `HistoryService#createHistoricActivityInstanceQuery` 查询历史活动节点，然后过滤已结束的活动节点。

历史节点查询核心代码如下：

```java
List<HistoricActivityInstance> historicActivityList = historyService.createHistoricActivityInstanceQuery()
        .processInstanceId(processInstanceId)
        .orderByHistoricActivityInstanceStartTime()
        .asc()
        .list();
```

常见历史活动类型如下：

| 活动类型           | 说明     |
| ------------------ | -------- |
| `startEvent`       | 开始事件 |
| `userTask`         | 用户任务 |
| `exclusiveGateway` | 排他网关 |
| `parallelGateway`  | 并行网关 |
| `endEvent`         | 结束事件 |
| `sequenceFlow`     | 流程连线 |

如果需要高亮已走连线，可以从历史活动中筛选 `activityType = sequenceFlow` 的记录：

```java
List<String> finishedFlowIds = historicActivityList.stream()
        .filter(item -> StrUtil.equals(item.getActivityType(), "sequenceFlow"))
        .map(HistoricActivityInstance::getActivityId)
        .filter(StrUtil::isNotBlank)
        .distinct()
        .toList();
```

历史节点高亮注意事项：

| 注意事项                 | 说明                                               |
| ------------------------ | -------------------------------------------------- |
| 并行流程会有多个历史分支 | 前端需要支持多个节点同时高亮                       |
| 驳回后节点可能重复出现   | 同一节点可能被多次执行，展示时可去重或展示次数     |
| 只看节点不够完整         | 应同时高亮已走连线                                 |
| 已终止流程仍可展示历史   | 当前节点为空，但历史节点仍可查询                   |
| 历史级别需要开启         | `flowable.db-history-used=true` 且历史级别不能过低 |

对于驳回、撤回、重复审批的流程，建议审批轨迹列表保留完整顺序，流程图节点高亮可以去重展示。这样既能保证图形简洁，也能保证轨迹明细可追溯。

### 流程审批轨迹展示

流程审批轨迹展示用于展示每个节点的处理人、处理动作、处理意见和处理时间。推荐以业务审批记录表 `flow_approval_record` 作为主要数据源，Flowable 历史任务表作为补充数据源。

审批轨迹建议展示字段如下：

| 字段                | 说明                                 |
| ------------------- | ------------------------------------ |
| `taskName`          | 审批节点名称                         |
| `approverName`      | 审批人名称                           |
| `approveAction`     | 审批动作，例如提交、通过、驳回、撤回 |
| `approveComment`    | 审批意见                             |
| `approveTime`       | 审批时间                             |
| `duration`          | 处理耗时                             |
| `taskDefinitionKey` | 节点 Key，用于和流程图节点关联       |

审批轨迹响应对象如下：

文件位置：`src/main/java/io/github/atengk/flowable/vo/ApprovalTimelineVO.java`

```java
package io.github.atengk.flowable.vo;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 审批轨迹响应对象。
 *
 * @author Ateng
 * @since 2026-05-08
 */
@Data
@Builder
public class ApprovalTimelineVO {

    /**
     * 任务 ID。
     */
    private String taskId;

    /**
     * 任务节点 Key。
     */
    private String taskDefinitionKey;

    /**
     * 任务名称。
     */
    private String taskName;

    /**
     * 审批人 ID。
     */
    private String approverId;

    /**
     * 审批人名称。
     */
    private String approverName;

    /**
     * 审批动作。
     */
    private String approveAction;

    /**
     * 审批意见。
     */
    private String approveComment;

    /**
     * 审批时间。
     */
    private LocalDateTime approveTime;

}
```

如果项目已经维护 `flow_approval_record` 表，轨迹查询应优先按 `processInstanceId` 查询该表：

```sql
-- 按流程实例查询审批轨迹
SELECT
    task_id,
    task_definition_key,
    task_name,
    approver_id,
    approver_name,
    approve_action,
    approve_comment,
    approve_time
FROM flow_approval_record
WHERE process_instance_id = #{processInstanceId}
ORDER BY approve_time ASC;
```

审批轨迹展示建议：

| 建议                  | 说明                         |
| --------------------- | ---------------------------- |
| 轨迹按时间正序展示    | 用户更容易理解流程推进顺序   |
| 提交动作也要展示      | 发起人提交是流程轨迹的一部分 |
| 驳回和撤回要突出      | 使用状态文案或图标区分       |
| 审批意见保留原文      | 便于后续审计                 |
| 节点 Key 和流程图关联 | 点击轨迹可定位到流程图节点   |

流程图和轨迹的推荐组合展示方式如下：

```text
上方：BPMN 流程图，高亮当前节点、已完成节点、已走连线
下方：审批轨迹时间线，展示处理人、动作、意见、时间
右侧：当前待办信息，展示当前任务、候选人、办理人
```

## 异常处理

异常处理用于统一封装 Flowable 常见错误、业务校验错误和事务回滚策略。工作流系统的异常不能只返回系统内部错误信息，应转换为业务可理解的提示，例如“任务不存在或已被处理”“流程实例不存在或已结束”“审批变量缺失，请检查流程配置”等。

在 Spring Boot 项目中，建议使用全局异常处理器统一捕获异常，并在流程 Service 中主动进行前置校验，避免把 Flowable 原始异常直接暴露给前端。

### 流程不存在处理

流程不存在通常出现在启动流程、查询流程图、终止流程、撤回流程等操作中。常见原因包括流程定义 Key 错误、流程实例 ID 错误、流程已经结束、流程已经被终止或历史数据被清理。

流程不存在可以分为两类：

| 类型           | 说明                              | 处理方式                     |
| -------------- | --------------------------------- | ---------------------------- |
| 流程定义不存在 | 根据流程定义 Key 或 ID 查不到定义 | 提示流程未部署或已删除       |
| 流程实例不存在 | 运行时和历史表都查不到实例        | 提示流程不存在               |
| 运行实例不存在 | 运行时表查不到，但历史表存在      | 说明流程已结束或已终止       |
| 当前任务不存在 | 流程存在，但当前没有待办任务      | 可能流程已结束或任务已被处理 |

建议封装流程实例校验方法：

文件位置：`src/main/java/io/github/atengk/flowable/support/FlowAssert.java`

```java
package io.github.atengk.flowable.support;

import cn.hutool.core.util.StrUtil;
import org.flowable.common.engine.api.FlowableObjectNotFoundException;
import org.flowable.engine.HistoryService;
import org.flowable.engine.RuntimeService;
import org.flowable.engine.history.HistoricProcessInstance;
import org.flowable.engine.runtime.ProcessInstance;

/**
 * 工作流断言工具。
 *
 * @author Ateng
 * @since 2026-05-08
 */
public final class FlowAssert {

    private FlowAssert() {
    }

    /**
     * 校验运行中流程实例是否存在。
     *
     * @param runtimeService 运行时服务
     * @param processInstanceId 流程实例 ID
     * @return 运行中流程实例
     */
    public static ProcessInstance requireRunningProcess(RuntimeService runtimeService, String processInstanceId) {
        if (StrUtil.isBlank(processInstanceId)) {
            throw new IllegalArgumentException("流程实例 ID 不能为空");
        }

        ProcessInstance processInstance = runtimeService.createProcessInstanceQuery()
                .processInstanceId(processInstanceId)
                .singleResult();

        if (processInstance == null) {
            throw new FlowableObjectNotFoundException("流程实例不存在或已结束：" + processInstanceId);
        }

        return processInstance;
    }

    /**
     * 校验历史流程实例是否存在。
     *
     * @param historyService 历史服务
     * @param processInstanceId 流程实例 ID
     * @return 历史流程实例
     */
    public static HistoricProcessInstance requireHistoricProcess(HistoryService historyService, String processInstanceId) {
        if (StrUtil.isBlank(processInstanceId)) {
            throw new IllegalArgumentException("流程实例 ID 不能为空");
        }

        HistoricProcessInstance processInstance = historyService.createHistoricProcessInstanceQuery()
                .processInstanceId(processInstanceId)
                .singleResult();

        if (processInstance == null) {
            throw new FlowableObjectNotFoundException("流程实例不存在：" + processInstanceId);
        }

        return processInstance;
    }

}
```

使用示例：

```java
ProcessInstance processInstance = FlowAssert.requireRunningProcess(runtimeService, processInstanceId);
```

流程不存在处理建议：

| 建议                         | 说明                             |
| ---------------------------- | -------------------------------- |
| 查询流程图时同时查运行和历史 | 已结束流程也应允许查看           |
| 终止和撤回只允许运行中流程   | 已结束流程不能终止或撤回         |
| 启动流程前校验流程定义 Key   | 避免启动时抛出底层异常           |
| 返回明确业务提示             | 不直接返回 Flowable 原始堆栈信息 |

### 任务不存在处理

任务不存在是审批系统中最常见的异常之一。它通常不一定是系统错误，也可能是任务已经被其他人处理、候选任务已经被领取、流程已经结束或前端重复提交。

任务不存在常见场景如下：

| 场景                 | 说明                                 |
| -------------------- | ------------------------------------ |
| 用户重复点击审批按钮 | 第一次成功，第二次任务已不存在       |
| 多个候选人同时处理   | A 已领取或完成，B 再处理时任务不存在 |
| 流程被终止           | 当前运行任务被删除                   |
| 流程已结束           | 当前节点已完成并流转结束             |
| 前端缓存旧任务 ID    | 待办列表未刷新                       |

建议封装任务校验方法：

文件位置：`src/main/java/io/github/atengk/flowable/support/FlowTaskAssert.java`

```java
package io.github.atengk.flowable.support;

import cn.hutool.core.util.StrUtil;
import org.flowable.common.engine.api.FlowableObjectNotFoundException;
import org.flowable.engine.TaskService;
import org.flowable.task.api.Task;

/**
 * 工作流任务断言工具。
 *
 * @author Ateng
 * @since 2026-05-08
 */
public final class FlowTaskAssert {

    private FlowTaskAssert() {
    }

    /**
     * 校验任务是否存在。
     *
     * @param taskService 任务服务
     * @param taskId 任务 ID
     * @return 任务对象
     */
    public static Task requireTask(TaskService taskService, String taskId) {
        if (StrUtil.isBlank(taskId)) {
            throw new IllegalArgumentException("任务 ID 不能为空");
        }

        Task task = taskService.createTaskQuery()
                .taskId(taskId)
                .singleResult();

        if (task == null) {
            throw new FlowableObjectNotFoundException("任务不存在或已被处理：" + taskId);
        }

        return task;
    }

    /**
     * 校验任务办理人。
     *
     * @param task 任务对象
     * @param userId 用户 ID
     */
    public static void requireAssignee(Task task, String userId) {
        if (StrUtil.isBlank(userId)) {
            throw new IllegalArgumentException("用户 ID 不能为空");
        }

        if (StrUtil.isNotBlank(task.getAssignee()) && !StrUtil.equals(task.getAssignee(), userId)) {
            throw new IllegalStateException("当前任务不属于该用户，无法办理");
        }
    }

}
```

使用示例：

```java
Task task = FlowTaskAssert.requireTask(taskService, dto.getTaskId());
FlowTaskAssert.requireAssignee(task, dto.getUserId());
```

任务不存在处理建议：

| 建议                   | 说明                               |
| ---------------------- | ---------------------------------- |
| 审批前重新查询任务     | 不信任前端缓存的待办数据           |
| 返回“任务已被处理”     | 比“任务不存在”更符合用户理解       |
| 候选任务审批前先领取   | 避免并发审批冲突                   |
| 审批成功后刷新待办列表 | 防止用户重复点击                   |
| 操作按钮做防重复提交   | 前端按钮禁用只是辅助，后端仍要校验 |

### 重复审批处理

重复审批通常由用户重复点击、接口重试、浏览器刷新、候选任务并发抢占等原因导致。重复审批必须在后端处理，否则可能出现审批记录重复、业务状态错乱或流程异常。

重复审批常见原因如下：

| 原因             | 说明                           |
| ---------------- | ------------------------------ |
| 用户双击审批按钮 | 同一个请求短时间内提交两次     |
| 前端超时后重试   | 第一次实际成功，第二次又提交   |
| 网关或客户端重试 | 网络层重复发送请求             |
| 候选人并发审批   | 多人同时处理同一候选任务       |
| 页面数据过期     | 用户停留页面过久，任务已被处理 |

推荐使用以下组合策略：

| 策略             | 说明                           |
| ---------------- | ------------------------------ |
| 任务存在校验     | 审批前必须重新查询运行任务     |
| 权限校验         | 确认当前用户有权处理任务       |
| 业务幂等键       | 对审批请求生成唯一操作键       |
| 审批记录唯一约束 | 防止同一任务同一动作重复入库   |
| 乐观锁           | 更新业务状态时校验版本号       |
| 分布式锁         | 高并发审批场景对 `taskId` 加锁 |

审批记录表可以增加唯一约束，防止同一任务重复保存通过或驳回记录：

```sql
-- 防止同一个任务被同一个人重复保存审批动作
ALTER TABLE flow_approval_record
ADD UNIQUE KEY uk_task_approver_action (task_id, approver_id, approve_action);
```

对于单体项目或低并发系统，可以先使用数据库唯一约束和任务存在校验。对于分布式部署或高并发候选任务，建议使用 Redis 分布式锁或数据库悲观锁。

下面示例演示使用 Redisson 对任务审批加锁。该代码需要项目额外引入 Redisson 依赖，并配置 Redis。

文件位置：`src/main/java/io/github/atengk/flowable/support/FlowTaskLockService.java`

```java
package io.github.atengk.flowable.support;

import cn.hutool.core.util.StrUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.function.Supplier;

/**
 * 流程任务锁服务。
 *
 * @author Ateng
 * @since 2026-05-08
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class FlowTaskLockService {

    private final RedissonClient redissonClient;

    /**
     * 使用任务锁执行操作。
     *
     * @param taskId 任务 ID
     * @param supplier 操作逻辑
     * @return 执行结果
     * @param <T> 返回类型
     */
    public <T> T executeWithTaskLock(String taskId, Supplier<T> supplier) {
        if (StrUtil.isBlank(taskId)) {
            throw new IllegalArgumentException("任务 ID 不能为空");
        }

        String lockKey = "flowable:task:lock:" + taskId;
        RLock lock = redissonClient.getLock(lockKey);
        boolean locked = false;

        try {
            locked = lock.tryLock(Duration.ofSeconds(3), Duration.ofSeconds(30));
            if (!locked) {
                throw new IllegalStateException("当前任务正在处理中，请稍后重试");
            }

            log.info("获取任务审批锁成功，任务ID：{}", taskId);
            return supplier.get();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("获取任务审批锁被中断");
        } finally {
            if (locked && lock.isHeldByCurrentThread()) {
                lock.unlock();
                log.info("释放任务审批锁成功，任务ID：{}", taskId);
            }
        }
    }

}
```

使用方式：

```java
flowTaskLockService.executeWithTaskLock(dto.getTaskId(), () -> {
    completeTask(dto);
    return null;
});
```

重复审批处理建议：

| 建议               | 说明                             |
| ------------------ | -------------------------------- |
| 不只依赖前端防抖   | 后端必须具备幂等保护             |
| 高并发候选任务加锁 | 防止多人同时完成同一任务         |
| 审批记录加唯一约束 | 数据库兜底防止重复写入           |
| 提示语保持业务化   | 返回“任务已处理，请刷新待办列表” |
| 操作完成后刷新任务 | 前端审批成功后重新拉取待办       |

### 表达式异常处理

表达式异常通常发生在流程流转到条件网关、动态办理人、候选人、监听器表达式时。常见表现是任务完成失败、流程无法继续流转或启动流程失败。

常见表达式异常如下：

| 场景           | 示例                            | 原因                      |
| -------------- | ------------------------------- | ------------------------- |
| 变量不存在     | `${leaveDays > 3}`              | 后端未传 `leaveDays`      |
| 类型不匹配     | `${amount > 5000}`              | `amount` 传成字符串或空值 |
| 审批人为空     | `${managerUserId}`              | 审批人计算失败            |
| Bean 不存在    | `${approvalTaskCreateListener}` | Spring Bean 名称不一致    |
| 表达式语法错误 | `${approveResult = 'PASS'}`     | 表达式写法错误            |

表达式异常处理应以前置校验为主，而不是等 Flowable 抛出异常。对于每个流程定义，建议维护一份启动变量和审批变量清单。

变量校验工具示例如下：

文件位置：`src/main/java/io/github/atengk/flowable/support/FlowVariableValidator.java`

```java
package io.github.atengk.flowable.support;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.map.MapUtil;
import cn.hutool.core.util.StrUtil;

import java.util.List;
import java.util.Map;

/**
 * 流程变量校验器。
 *
 * @author Ateng
 * @since 2026-05-08
 */
public final class FlowVariableValidator {

    private FlowVariableValidator() {
    }

    /**
     * 校验必填流程变量。
     *
     * @param variables 流程变量
     * @param requiredKeys 必填变量 Key
     */
    public static void requireVariables(Map<String, Object> variables, List<String> requiredKeys) {
        if (CollUtil.isEmpty(requiredKeys)) {
            return;
        }

        if (MapUtil.isEmpty(variables)) {
            throw new IllegalArgumentException("流程变量不能为空");
        }

        List<String> missingKeys = requiredKeys.stream()
                .filter(key -> !variables.containsKey(key) || variables.get(key) == null)
                .toList();

        if (CollUtil.isNotEmpty(missingKeys)) {
            throw new IllegalArgumentException("缺少必要流程变量：" + StrUtil.join(",", missingKeys));
        }
    }

    /**
     * 校验字符串变量不能为空。
     *
     * @param variables 流程变量
     * @param keys 变量 Key
     */
    public static void requireNotBlank(Map<String, Object> variables, List<String> keys) {
        if (CollUtil.isEmpty(keys)) {
            return;
        }

        List<String> blankKeys = keys.stream()
                .filter(key -> StrUtil.isBlankIfStr(MapUtil.get(variables, key, Object.class)))
                .toList();

        if (CollUtil.isNotEmpty(blankKeys)) {
            throw new IllegalArgumentException("流程变量不能为空：" + StrUtil.join(",", blankKeys));
        }
    }

}
```

启动请假流程前校验变量：

```java
FlowVariableValidator.requireVariables(
        variables,
        List.of("applyUserId", "managerUserId", "leaveDays")
);

FlowVariableValidator.requireNotBlank(
        variables,
        List.of("applyUserId", "managerUserId")
);
```

表达式异常处理建议：

| 建议                    | 说明                                         |
| ----------------------- | -------------------------------------------- |
| 启动流程前校验变量      | 尤其是审批人、金额、天数等条件变量           |
| 完成任务前校验变量      | 驳回、通过、下一审批人等变量要完整           |
| BPMN 表达式使用简单判断 | 复杂逻辑放到后端计算                         |
| 监听器 Bean 名称固定    | `@Component("xxxListener")` 和 BPMN 保持一致 |
| 表达式使用 CDATA        | 避免 XML 特殊字符影响解析                    |
| 错误日志保留流程实例 ID | 便于定位具体流程和节点                       |

### 事务回滚处理

事务回滚处理用于保证业务表、审批记录和 Flowable 流程状态一致。工作流操作通常不是单纯调用 Flowable API，而是同时涉及业务表状态更新、审批记录保存、消息通知、附件处理等操作。

推荐事务边界如下：

| 操作               | 是否放入同一事务 | 说明                       |
| ------------------ | ---------------- | -------------------------- |
| 保存业务单据       | 是               | 和启动流程保持一致         |
| 启动流程实例       | 是               | 启动失败时业务状态应回滚   |
| 更新业务审批状态   | 是               | 和任务完成保持一致         |
| 保存审批记录       | 是               | 审批失败时不应保存成功记录 |
| 完成 Flowable 任务 | 是               | 和业务状态更新保持一致     |
| 发送消息通知       | 否               | 建议事务提交后异步发送     |
| 调用外部系统       | 否               | 避免外部接口失败影响主事务 |

审批通过的事务示例：

```text
开始事务
  1. 查询任务并校验权限
  2. 保存审批记录
  3. 完成 Flowable 任务
  4. 判断流程是否结束
  5. 更新业务审批状态
提交事务
  6. 事务提交后发送消息通知
```

Service 方法应使用 `@Transactional(rollbackFor = Exception.class)`，确保受检异常和运行时异常都能触发回滚。

文件位置：`src/main/java/io/github/atengk/flowable/service/impl/FlowApprovalTransactionService.java`

```java
package io.github.atengk.flowable.service.impl;

import cn.hutool.core.map.MapUtil;
import cn.hutool.core.util.StrUtil;
import io.github.atengk.flowable.common.constant.FlowVariableConstants;
import io.github.atengk.flowable.dto.CompleteTaskDTO;
import io.github.atengk.flowable.support.FlowTaskAssert;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.flowable.engine.RuntimeService;
import org.flowable.engine.TaskService;
import org.flowable.task.api.Task;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashMap;
import java.util.Map;

/**
 * 流程审批事务服务。
 *
 * @author Ateng
 * @since 2026-05-08
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class FlowApprovalTransactionService {

    private final TaskService taskService;
    private final RuntimeService runtimeService;
    private final ApplicationEventPublisher applicationEventPublisher;

    /**
     * 在事务中完成审批任务。
     *
     * @param dto 审批任务请求参数
     */
    @Transactional(rollbackFor = Exception.class)
    public void approveInTransaction(CompleteTaskDTO dto) {
        Task task = FlowTaskAssert.requireTask(taskService, dto.getTaskId());
        FlowTaskAssert.requireAssignee(task, dto.getUserId());

        if (StrUtil.isBlank(task.getAssignee())) {
            taskService.claim(dto.getTaskId(), dto.getUserId());
            log.info("候选任务自动领取成功，任务ID：{}，用户ID：{}", dto.getTaskId(), dto.getUserId());
        }

        Map<String, Object> variables = new HashMap<>();
        if (MapUtil.isNotEmpty(dto.getVariables())) {
            variables.putAll(dto.getVariables());
        }
        variables.put(FlowVariableConstants.APPROVE_RESULT, dto.getApproveResult());
        variables.put(FlowVariableConstants.APPROVE_COMMENT, dto.getComment());

        /*
         * 正式项目中这里建议先保存审批记录。
         * 如果后续 complete 失败，审批记录会随事务回滚。
         */

        taskService.addComment(dto.getTaskId(), task.getProcessInstanceId(), dto.getComment());
        taskService.complete(dto.getTaskId(), variables);

        boolean processEnded = runtimeService.createProcessInstanceQuery()
                .processInstanceId(task.getProcessInstanceId())
                .singleResult() == null;

        if (processEnded) {
            /*
             * 正式项目中这里更新 flow_business 和具体业务表状态为 APPROVED。
             */
            log.info("审批完成且流程已结束，流程实例ID：{}", task.getProcessInstanceId());
        } else {
            /*
             * 正式项目中这里更新 flow_business 当前任务信息。
             */
            log.info("审批完成且流程继续流转，流程实例ID：{}", task.getProcessInstanceId());
        }

        /*
         * 建议只发布事件，不在事务中直接调用邮件、短信、企业微信等外部服务。
         * 更严谨的做法是使用事务提交后事件或 MQ 事务消息。
         */
        log.info("审批事务处理完成，任务ID：{}，流程实例ID：{}，审批人：{}，审批结果：{}",
                dto.getTaskId(), task.getProcessInstanceId(), dto.getUserId(), dto.getApproveResult());
    }

}
```

为了统一异常响应，可以增加全局异常处理器。下面示例将 Flowable 常见异常、参数异常和业务异常转换为统一 JSON。

文件位置：`src/main/java/io/github/atengk/flowable/common/result/ApiResult.java`

```java
package io.github.atengk.flowable.common.result;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 接口统一响应结果。
 *
 * @author Ateng
 * @since 2026-05-08
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ApiResult<T> {

    /**
     * 响应码。
     */
    private Integer code;

    /**
     * 响应消息。
     */
    private String message;

    /**
     * 响应数据。
     */
    private T data;

    /**
     * 成功响应。
     *
     * @param data 响应数据
     * @return 统一响应结果
     * @param <T> 数据类型
     */
    public static <T> ApiResult<T> success(T data) {
        return new ApiResult<>(200, "操作成功", data);
    }

    /**
     * 失败响应。
     *
     * @param code 响应码
     * @param message 响应消息
     * @return 统一响应结果
     * @param <T> 数据类型
     */
    public static <T> ApiResult<T> fail(Integer code, String message) {
        return new ApiResult<>(code, message, null);
    }

}
```

文件位置：`src/main/java/io/github/atengk/flowable/common/exception/GlobalExceptionHandler.java`

```java
package io.github.atengk.flowable.common.exception;

import cn.hutool.core.util.StrUtil;
import io.github.atengk.flowable.common.result.ApiResult;
import jakarta.validation.ConstraintViolationException;
import lombok.extern.slf4j.Slf4j;
import org.flowable.common.engine.api.FlowableException;
import org.flowable.common.engine.api.FlowableObjectNotFoundException;
import org.springframework.validation.BindException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

/**
 * 全局异常处理器。
 *
 * @author Ateng
 * @since 2026-05-08
 */
@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    /**
     * 处理参数校验异常。
     *
     * @param e 参数校验异常
     * @return 统一响应结果
     */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ApiResult<Void> handleMethodArgumentNotValidException(MethodArgumentNotValidException e) {
        String message = e.getBindingResult().getFieldErrors().stream()
                .findFirst()
                .map(error -> StrUtil.blankToDefault(error.getDefaultMessage(), "参数校验失败"))
                .orElse("参数校验失败");
        return ApiResult.fail(400, message);
    }

    /**
     * 处理绑定异常。
     *
     * @param e 绑定异常
     * @return 统一响应结果
     */
    @ExceptionHandler(BindException.class)
    public ApiResult<Void> handleBindException(BindException e) {
        String message = e.getBindingResult().getFieldErrors().stream()
                .findFirst()
                .map(error -> StrUtil.blankToDefault(error.getDefaultMessage(), "参数绑定失败"))
                .orElse("参数绑定失败");
        return ApiResult.fail(400, message);
    }

    /**
     * 处理约束校验异常。
     *
     * @param e 约束校验异常
     * @return 统一响应结果
     */
    @ExceptionHandler(ConstraintViolationException.class)
    public ApiResult<Void> handleConstraintViolationException(ConstraintViolationException e) {
        return ApiResult.fail(400, e.getMessage());
    }

    /**
     * 处理非法参数异常。
     *
     * @param e 非法参数异常
     * @return 统一响应结果
     */
    @ExceptionHandler(IllegalArgumentException.class)
    public ApiResult<Void> handleIllegalArgumentException(IllegalArgumentException e) {
        return ApiResult.fail(400, e.getMessage());
    }

    /**
     * 处理非法状态异常。
     *
     * @param e 非法状态异常
     * @return 统一响应结果
     */
    @ExceptionHandler(IllegalStateException.class)
    public ApiResult<Void> handleIllegalStateException(IllegalStateException e) {
        return ApiResult.fail(409, e.getMessage());
    }

    /**
     * 处理 Flowable 对象不存在异常。
     *
     * @param e Flowable 对象不存在异常
     * @return 统一响应结果
     */
    @ExceptionHandler(FlowableObjectNotFoundException.class)
    public ApiResult<Void> handleFlowableObjectNotFoundException(FlowableObjectNotFoundException e) {
        log.warn("Flowable 对象不存在：{}", e.getMessage());
        return ApiResult.fail(404, e.getMessage());
    }

    /**
     * 处理 Flowable 通用异常。
     *
     * @param e Flowable 通用异常
     * @return 统一响应结果
     */
    @ExceptionHandler(FlowableException.class)
    public ApiResult<Void> handleFlowableException(FlowableException e) {
        log.error("Flowable 执行异常：{}", e.getMessage(), e);
        return ApiResult.fail(500, "流程执行异常，请检查流程配置或联系管理员");
    }

    /**
     * 处理其他系统异常。
     *
     * @param e 系统异常
     * @return 统一响应结果
     */
    @ExceptionHandler(Exception.class)
    public ApiResult<Void> handleException(Exception e) {
        log.error("系统异常", e);
        return ApiResult.fail(500, "系统异常，请联系管理员");
    }

}
```

事务回滚注意事项：

| 注意事项                             | 说明                           |
| ------------------------------------ | ------------------------------ |
| Service 层加事务                     | Controller 不负责事务控制      |
| 使用 `rollbackFor = Exception.class` | 保证受检异常也触发回滚         |
| Flowable 操作和业务更新同事务        | 避免流程状态和业务状态不一致   |
| 外部通知异步发送                     | 避免消息失败导致审批失败       |
| 审批记录先写入再完成任务             | 若任务完成失败，记录随事务回滚 |
| 避免监听器过重                       | 监听器异常可能导致主流程回滚   |

最终建议是将异常处理分为三层：第一层在 Controller 做参数校验，第二层在 Service 做业务校验和权限校验，第三层通过全局异常处理器统一转换响应。这样可以保证前端提示稳定、后端日志完整、流程数据和业务数据保持一致。



## 测试与验证

测试与验证用于确认 Flowable 集成是否可用、BPMN 流程是否可部署、流程实例是否能正常启动、任务是否能按预期审批、网关条件是否正确流转，以及异常场景是否能够返回稳定的业务提示。

工作流测试不建议只验证接口是否返回成功，还需要验证数据库状态、流程实例状态、任务数量、历史记录、业务状态和审批记录是否一致。尤其是审批通过、驳回、撤回、终止等操作，应同时检查 Flowable 表和业务表。

### 流程部署测试

流程部署测试用于验证 BPMN 文件是否符合 Flowable 解析要求，流程定义是否能正常写入 `ACT_RE_*` 相关表。部署失败通常说明 BPMN XML 格式错误、流程 ID 重复异常、表达式配置错误或文件路径不正确。

测试前需要确保测试环境可以正常初始化 Flowable 表。推荐测试环境使用独立 profile，避免污染开发库。

文件位置：`src/test/resources/application-test.yml`

```yaml
spring:
  datasource:
    # 测试环境使用 H2 内存数据库，避免污染本地 MySQL
    url: jdbc:h2:mem:flowable-test;MODE=MySQL;DB_CLOSE_DELAY=-1;DATABASE_TO_LOWER=TRUE
    driver-class-name: org.h2.Driver
    username: sa
    password:

flowable:
  # 测试环境允许自动建表
  database-schema-update: true
  # 开启历史数据，便于验证已办和轨迹
  db-history-used: true
  # 测试审批轨迹建议使用 audit
  history-level: audit
  # 测试用例中手动部署流程，避免自动部署干扰断言
  check-process-definitions: false

logging:
  level:
    io.github.atengk: debug
    org.flowable: info
```

如果使用 H2 作为测试数据库，需要在 `pom.xml` 中增加测试依赖。

```xml
<!-- H2 测试数据库，仅用于单元测试和集成测试 -->
<dependency>
    <groupId>com.h2database</groupId>
    <artifactId>h2</artifactId>
    <scope>test</scope>
</dependency>
```

下面测试类用于验证 BPMN 文件可以正常部署，并能查询到流程定义。

文件位置：`src/test/java/io/github/atengk/flowable/FlowableDeploymentTests.java`

```java
package io.github.atengk.flowable;

import org.flowable.engine.RepositoryService;
import org.flowable.engine.repository.Deployment;
import org.flowable.engine.repository.ProcessDefinition;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.core.io.ClassPathResource;
import org.springframework.test.context.ActiveProfiles;

import java.io.InputStream;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Flowable 流程部署测试。
 *
 * @author Ateng
 * @since 2026-05-08
 */
@SpringBootTest
@ActiveProfiles("test")
class FlowableDeploymentTests {

    @Autowired
    private RepositoryService repositoryService;

    /**
     * 测试部署请假审批流程。
     *
     * @throws Exception 测试异常
     */
    @Test
    void deployLeaveApproveProcess() throws Exception {
        ClassPathResource resource = new ClassPathResource("processes/leave-approve.bpmn20.xml");

        try (InputStream inputStream = resource.getInputStream()) {
            Deployment deployment = repositoryService.createDeployment()
                    .name("请假审批流程测试部署")
                    .addInputStream("leave-approve.bpmn20.xml", inputStream)
                    .deploy();

            assertThat(deployment.getId()).isNotBlank();

            ProcessDefinition processDefinition = repositoryService.createProcessDefinitionQuery()
                    .deploymentId(deployment.getId())
                    .processDefinitionKey("leaveApprove")
                    .singleResult();

            assertThat(processDefinition).isNotNull();
            assertThat(processDefinition.getKey()).isEqualTo("leaveApprove");
            assertThat(processDefinition.getName()).isEqualTo("请假审批流程");
        }
    }

}
```

验证重点如下：

| 验证项            | 说明                            |
| ----------------- | ------------------------------- |
| 部署 ID 不为空    | 表示部署记录生成成功            |
| 流程定义 Key 正确 | BPMN 中 `process id` 与代码一致 |
| 流程名称正确      | 便于后台展示                    |
| 数据库表正常生成  | 测试环境自动建表成功            |
| 无 BPMN 解析异常  | XML 结构、命名空间、表达式正确  |

### 流程启动测试

流程启动测试用于验证流程定义部署后，是否可以根据流程定义 Key 启动流程实例，并正确生成第一个待办任务。

测试流程启动时必须传入 BPMN 中使用的必要变量，例如 `managerUserId`、`bossUserId`、`leaveDays`。如果变量缺失，流程可能在启动或流转到网关时失败。

下面测试类用于完成“部署流程 → 启动流程 → 查询第一个任务”的完整验证。

文件位置：`src/test/java/io/github/atengk/flowable/FlowableStartProcessTests.java`

```java
package io.github.atengk.flowable;

import cn.hutool.core.map.MapUtil;
import org.flowable.engine.RepositoryService;
import org.flowable.engine.RuntimeService;
import org.flowable.engine.runtime.ProcessInstance;
import org.flowable.task.api.Task;
import org.flowable.engine.TaskService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.core.io.ClassPathResource;
import org.springframework.test.context.ActiveProfiles;

import java.io.InputStream;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Flowable 流程启动测试。
 *
 * @author Ateng
 * @since 2026-05-08
 */
@SpringBootTest
@ActiveProfiles("test")
class FlowableStartProcessTests {

    @Autowired
    private RepositoryService repositoryService;

    @Autowired
    private RuntimeService runtimeService;

    @Autowired
    private TaskService taskService;

    /**
     * 每个测试前部署流程。
     *
     * @throws Exception 测试异常
     */
    @BeforeEach
    void deployProcess() throws Exception {
        ClassPathResource resource = new ClassPathResource("processes/leave-approve.bpmn20.xml");
        try (InputStream inputStream = resource.getInputStream()) {
            repositoryService.createDeployment()
                    .name("请假审批流程测试部署")
                    .addInputStream("leave-approve.bpmn20.xml", inputStream)
                    .deploy();
        }
    }

    /**
     * 测试启动请假审批流程。
     */
    @Test
    void startLeaveApproveProcess() {
        Map<String, Object> variables = MapUtil.<String, Object>builder()
                .put("applyUserId", "10001")
                .put("managerUserId", "10002")
                .put("bossUserId", "10003")
                .put("leaveDays", 5)
                .build();

        ProcessInstance processInstance = runtimeService.startProcessInstanceByKey(
                "leaveApprove",
                "LEAVE_10001",
                variables
        );

        assertThat(processInstance.getId()).isNotBlank();
        assertThat(processInstance.getBusinessKey()).isEqualTo("LEAVE_10001");

        Task task = taskService.createTaskQuery()
                .processInstanceId(processInstance.getId())
                .singleResult();

        assertThat(task).isNotNull();
        assertThat(task.getTaskDefinitionKey()).isEqualTo("managerApprove");
        assertThat(task.getAssignee()).isEqualTo("10002");
    }

}
```

流程启动后需要重点验证：

| 验证项             | 说明                       |
| ------------------ | -------------------------- |
| 流程实例 ID 不为空 | 流程启动成功               |
| `businessKey` 正确 | 流程实例和业务单据绑定成功 |
| 第一个任务存在     | 流程已经流转到人工审批节点 |
| 办理人正确         | `managerUserId` 变量生效   |
| 流程变量正确       | 网关和后续节点依赖变量完整 |

接口测试命令如下：

```bash
curl -X POST "http://localhost:8080/api/flow/instances/start" \
  -H "Content-Type: application/json" \
  -d '{
    "processDefinitionKey": "leaveApprove",
    "businessKey": "LEAVE_10001",
    "startUserId": "10001",
    "variables": {
      "managerUserId": "10002",
      "bossUserId": "10003",
      "leaveDays": 5
    }
  }'
```

### 任务审批测试

任务审批测试用于验证当前任务可以被正确完成，审批意见可以保存，流程变量可以传递，并且流程会进入下一节点或结束。

下面测试示例覆盖“启动流程 → 部门经理审批 → 总经理审批 → 流程结束”的完整链路。

文件位置：`src/test/java/io/github/atengk/flowable/FlowableTaskCompleteTests.java`

```java
package io.github.atengk.flowable;

import cn.hutool.core.map.MapUtil;
import org.flowable.engine.HistoryService;
import org.flowable.engine.RepositoryService;
import org.flowable.engine.RuntimeService;
import org.flowable.engine.history.HistoricProcessInstance;
import org.flowable.engine.runtime.ProcessInstance;
import org.flowable.task.api.Task;
import org.flowable.engine.TaskService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.core.io.ClassPathResource;
import org.springframework.test.context.ActiveProfiles;

import java.io.InputStream;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Flowable 任务审批测试。
 *
 * @author Ateng
 * @since 2026-05-08
 */
@SpringBootTest
@ActiveProfiles("test")
class FlowableTaskCompleteTests {

    @Autowired
    private RepositoryService repositoryService;

    @Autowired
    private RuntimeService runtimeService;

    @Autowired
    private TaskService taskService;

    @Autowired
    private HistoryService historyService;

    /**
     * 每个测试前部署流程。
     *
     * @throws Exception 测试异常
     */
    @BeforeEach
    void deployProcess() throws Exception {
        ClassPathResource resource = new ClassPathResource("processes/leave-approve.bpmn20.xml");
        try (InputStream inputStream = resource.getInputStream()) {
            repositoryService.createDeployment()
                    .name("请假审批流程测试部署")
                    .addInputStream("leave-approve.bpmn20.xml", inputStream)
                    .deploy();
        }
    }

    /**
     * 测试完成完整审批流程。
     */
    @Test
    void completeLeaveApproveProcess() {
        Map<String, Object> variables = MapUtil.<String, Object>builder()
                .put("applyUserId", "10001")
                .put("managerUserId", "10002")
                .put("bossUserId", "10003")
                .put("leaveDays", 5)
                .build();

        ProcessInstance processInstance = runtimeService.startProcessInstanceByKey(
                "leaveApprove",
                "LEAVE_10002",
                variables
        );

        Task managerTask = taskService.createTaskQuery()
                .processInstanceId(processInstance.getId())
                .taskDefinitionKey("managerApprove")
                .singleResult();

        assertThat(managerTask).isNotNull();
        taskService.addComment(managerTask.getId(), processInstance.getId(), "部门经理同意");
        taskService.complete(managerTask.getId(), MapUtil.<String, Object>builder()
                .put("approveResult", "PASS")
                .put("approveComment", "部门经理同意")
                .build());

        Task bossTask = taskService.createTaskQuery()
                .processInstanceId(processInstance.getId())
                .taskDefinitionKey("bossApprove")
                .singleResult();

        assertThat(bossTask).isNotNull();
        assertThat(bossTask.getAssignee()).isEqualTo("10003");

        taskService.addComment(bossTask.getId(), processInstance.getId(), "总经理同意");
        taskService.complete(bossTask.getId(), MapUtil.<String, Object>builder()
                .put("approveResult", "PASS")
                .put("approveComment", "总经理同意")
                .build());

        ProcessInstance runningInstance = runtimeService.createProcessInstanceQuery()
                .processInstanceId(processInstance.getId())
                .singleResult();

        assertThat(runningInstance).isNull();

        HistoricProcessInstance historicProcessInstance = historyService.createHistoricProcessInstanceQuery()
                .processInstanceId(processInstance.getId())
                .finished()
                .singleResult();

        assertThat(historicProcessInstance).isNotNull();
        assertThat(historicProcessInstance.getEndTime()).isNotNull();
    }

}
```

任务审批验证重点如下：

| 验证项           | 说明                         |
| ---------------- | ---------------------------- |
| 当前任务存在     | 任务没有被提前处理           |
| 办理人正确       | 当前用户有权审批             |
| 审批变量传递成功 | 网关或后续节点能读取变量     |
| 审批意见保存成功 | 可从评论或业务审批记录查询   |
| 审批后节点正确   | 流转到下一节点或结束         |
| 流程结束状态正确 | 运行时实例消失，历史实例存在 |

接口测试命令如下：

```bash
curl -X POST "http://localhost:8080/api/flow/tasks/complete" \
  -H "Content-Type: application/json" \
  -d '{
    "taskId": "25005",
    "userId": "10002",
    "approveResult": "PASS",
    "comment": "同意",
    "variables": {
      "approveResult": "PASS",
      "approveComment": "同意"
    }
  }'
```

### 网关流转测试

网关流转测试用于验证 BPMN 条件表达式是否按照业务规则选择正确路径。以请假流程为例，`leaveDays <= 3` 时部门经理审批后直接结束；`leaveDays > 3` 时需要继续进入总经理审批。

下面测试类覆盖短假和长假两个分支。

文件位置：`src/test/java/io/github/atengk/flowable/FlowableGatewayTests.java`

```java
package io.github.atengk.flowable;

import cn.hutool.core.map.MapUtil;
import org.flowable.engine.RepositoryService;
import org.flowable.engine.RuntimeService;
import org.flowable.engine.runtime.ProcessInstance;
import org.flowable.task.api.Task;
import org.flowable.engine.TaskService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.core.io.ClassPathResource;
import org.springframework.test.context.ActiveProfiles;

import java.io.InputStream;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Flowable 网关流转测试。
 *
 * @author Ateng
 * @since 2026-05-08
 */
@SpringBootTest
@ActiveProfiles("test")
class FlowableGatewayTests {

    @Autowired
    private RepositoryService repositoryService;

    @Autowired
    private RuntimeService runtimeService;

    @Autowired
    private TaskService taskService;

    /**
     * 每个测试前部署流程。
     *
     * @throws Exception 测试异常
     */
    @BeforeEach
    void deployProcess() throws Exception {
        ClassPathResource resource = new ClassPathResource("processes/leave-approve.bpmn20.xml");
        try (InputStream inputStream = resource.getInputStream()) {
            repositoryService.createDeployment()
                    .name("请假审批流程测试部署")
                    .addInputStream("leave-approve.bpmn20.xml", inputStream)
                    .deploy();
        }
    }

    /**
     * 测试请假天数小于等于 3 天时直接结束。
     */
    @Test
    void shortLeaveShouldEndAfterManagerApprove() {
        ProcessInstance processInstance = startProcess("LEAVE_SHORT", 3);

        Task managerTask = taskService.createTaskQuery()
                .processInstanceId(processInstance.getId())
                .taskDefinitionKey("managerApprove")
                .singleResult();

        taskService.complete(managerTask.getId(), MapUtil.<String, Object>builder()
                .put("approveResult", "PASS")
                .build());

        ProcessInstance runningInstance = runtimeService.createProcessInstanceQuery()
                .processInstanceId(processInstance.getId())
                .singleResult();

        assertThat(runningInstance).isNull();
    }

    /**
     * 测试请假天数大于 3 天时进入总经理审批。
     */
    @Test
    void longLeaveShouldGoToBossApprove() {
        ProcessInstance processInstance = startProcess("LEAVE_LONG", 5);

        Task managerTask = taskService.createTaskQuery()
                .processInstanceId(processInstance.getId())
                .taskDefinitionKey("managerApprove")
                .singleResult();

        taskService.complete(managerTask.getId(), MapUtil.<String, Object>builder()
                .put("approveResult", "PASS")
                .build());

        Task bossTask = taskService.createTaskQuery()
                .processInstanceId(processInstance.getId())
                .taskDefinitionKey("bossApprove")
                .singleResult();

        assertThat(bossTask).isNotNull();
        assertThat(bossTask.getAssignee()).isEqualTo("10003");
    }

    /**
     * 启动请假流程。
     *
     * @param businessKey 业务主键
     * @param leaveDays 请假天数
     * @return 流程实例
     */
    private ProcessInstance startProcess(String businessKey, Integer leaveDays) {
        Map<String, Object> variables = MapUtil.<String, Object>builder()
                .put("applyUserId", "10001")
                .put("managerUserId", "10002")
                .put("bossUserId", "10003")
                .put("leaveDays", leaveDays)
                .build();

        return runtimeService.startProcessInstanceByKey("leaveApprove", businessKey, variables);
    }

}
```

网关测试重点如下：

| 验证项       | 说明                         |
| ------------ | ---------------------------- |
| 条件变量存在 | 例如 `leaveDays`、`amount`   |
| 变量类型正确 | 数值比较应传数字，不传字符串 |
| 条件互斥     | 避免多条分支同时满足         |
| 默认分支可控 | 避免没有条件满足时流程异常   |
| 分支结果正确 | 流转到预期节点或结束事件     |

### 异常场景测试

异常场景测试用于验证系统在流程不存在、任务不存在、重复审批、变量缺失、权限不足等情况下能返回稳定错误，而不是直接抛出底层堆栈信息。

建议覆盖以下异常场景：

| 场景           | 输入                         | 预期结果               |
| -------------- | ---------------------------- | ---------------------- |
| 流程定义不存在 | 错误 `processDefinitionKey`  | 返回流程定义不存在     |
| 流程实例不存在 | 错误 `processInstanceId`     | 返回流程实例不存在     |
| 任务不存在     | 错误 `taskId`                | 返回任务不存在或已处理 |
| 重复审批       | 同一个 `taskId` 连续审批两次 | 第二次返回任务已处理   |
| 变量缺失       | 缺少 `leaveDays`             | 返回流程变量缺失       |
| 权限不足       | 非办理人审批                 | 返回无权办理           |
| 表达式异常     | BPMN 表达式变量类型错误      | 返回流程配置异常       |

下面示例测试重复审批场景。

文件位置：`src/test/java/io/github/atengk/flowable/FlowableExceptionTests.java`

```java
package io.github.atengk.flowable;

import cn.hutool.core.map.MapUtil;
import org.flowable.engine.RepositoryService;
import org.flowable.engine.RuntimeService;
import org.flowable.engine.runtime.ProcessInstance;
import org.flowable.task.api.Task;
import org.flowable.engine.TaskService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.core.io.ClassPathResource;
import org.springframework.test.context.ActiveProfiles;

import java.io.InputStream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Flowable 异常场景测试。
 *
 * @author Ateng
 * @since 2026-05-08
 */
@SpringBootTest
@ActiveProfiles("test")
class FlowableExceptionTests {

    @Autowired
    private RepositoryService repositoryService;

    @Autowired
    private RuntimeService runtimeService;

    @Autowired
    private TaskService taskService;

    /**
     * 每个测试前部署流程。
     *
     * @throws Exception 测试异常
     */
    @BeforeEach
    void deployProcess() throws Exception {
        ClassPathResource resource = new ClassPathResource("processes/leave-approve.bpmn20.xml");
        try (InputStream inputStream = resource.getInputStream()) {
            repositoryService.createDeployment()
                    .name("请假审批流程测试部署")
                    .addInputStream("leave-approve.bpmn20.xml", inputStream)
                    .deploy();
        }
    }

    /**
     * 测试重复审批任务。
     */
    @Test
    void duplicateCompleteTaskShouldFail() {
        ProcessInstance processInstance = runtimeService.startProcessInstanceByKey(
                "leaveApprove",
                "LEAVE_DUPLICATE",
                MapUtil.<String, Object>builder()
                        .put("applyUserId", "10001")
                        .put("managerUserId", "10002")
                        .put("bossUserId", "10003")
                        .put("leaveDays", 3)
                        .build()
        );

        Task task = taskService.createTaskQuery()
                .processInstanceId(processInstance.getId())
                .singleResult();

        assertThat(task).isNotNull();

        taskService.complete(task.getId(), MapUtil.<String, Object>builder()
                .put("approveResult", "PASS")
                .build());

        assertThatThrownBy(() -> taskService.complete(task.getId()))
                .isInstanceOf(Exception.class);
    }

}
```

异常测试建议不仅使用 JUnit，还要通过接口测试验证全局异常处理器返回格式是否稳定：

```bash
curl -X POST "http://localhost:8080/api/flow/tasks/complete" \
  -H "Content-Type: application/json" \
  -d '{
    "taskId": "not_exists_task_id",
    "userId": "10002",
    "approveResult": "PASS",
    "comment": "同意"
  }'
```

预期返回：

```json
{
  "code": 404,
  "message": "任务不存在或已被处理：not_exists_task_id",
  "data": null
}
```

## 部署与运行

部署与运行用于说明项目从开发环境迁移到测试、预发、生产环境时需要处理的数据库初始化、配置文件调整、日志配置和常见问题。Flowable 项目部署时的重点不是应用能否启动，而是流程表结构、流程定义版本、业务状态、历史数据和事务一致性是否可控。

生产环境不建议使用自动建表，不建议随意删除 Flowable 表，不建议直接修改运行中流程定义对应的 BPMN 文件。

### 数据库初始化

数据库初始化包括创建业务库、创建数据库账号、初始化 Flowable 引擎表和业务扩展表。开发环境可以让 Flowable 自动建表，生产环境建议使用 SQL 脚本或数据库变更工具统一管理。

开发环境数据库初始化示例：

```sql
-- 创建业务数据库
CREATE DATABASE flowable_boot
    DEFAULT CHARACTER SET utf8mb4
    DEFAULT COLLATE utf8mb4_0900_ai_ci;

-- 创建应用账号，实际生产密码应使用强密码并由配置中心管理
CREATE USER 'flowable_user'@'%' IDENTIFIED BY 'Flowable@123456';

-- 开发环境授权，允许自动建表和修改表结构
GRANT ALL PRIVILEGES ON flowable_boot.* TO 'flowable_user'@'%';

-- 刷新权限
FLUSH PRIVILEGES;
```

生产环境建议拆分权限：

| 环境     | 建议权限                                       |
| -------- | ---------------------------------------------- |
| 本地开发 | 允许 DDL、DML                                  |
| 测试环境 | 可允许 DDL，但需记录变更                       |
| 预发环境 | 建议手动执行 SQL，应用账号只保留 DML           |
| 生产环境 | 应用账号原则上只保留 DML，表结构变更走发布流程 |

生产环境 `flowable.database-schema-update` 建议关闭：

```yaml
flowable:
  # 生产环境关闭自动建表和自动升级表结构
  database-schema-update: false
  # 保留历史数据，支持审批审计
  db-history-used: true
  # 常规审批系统使用 audit 即可
  history-level: audit
```

业务扩展表需要提前初始化，例如：

```sql
-- 初始化通用流程业务关联表、审批记录表、具体业务表
SOURCE /opt/app/sql/flow_business.sql;
SOURCE /opt/app/sql/flow_approval_record.sql;
SOURCE /opt/app/sql/leave_application.sql;
```

如果团队使用 Flyway 或 Liquibase，建议将业务表 SQL 纳入版本管理；Flowable 引擎表是否纳入数据库变更工具，需要结合团队规范决定。中小项目可以预先执行 Flowable 对应版本的建表 SQL，业务扩展表则统一走迁移脚本。

### 配置文件调整

配置文件调整用于适配不同运行环境。建议通过 Spring profile 区分 `dev`、`test`、`prod`，并将数据库密码、Redis 密码、消息服务密钥等敏感信息交给环境变量或配置中心管理。

推荐配置文件结构如下：

```text
src/main/resources
├── application.yml
├── application-dev.yml
├── application-test.yml
└── application-prod.yml
```

通用配置放在 `application.yml`：

```yaml
server:
  port: 8080

spring:
  application:
    name: springboot-flowable-demo

mybatis-plus:
  mapper-locations: classpath*:/mapper/**/*.xml
  configuration:
    map-underscore-to-camel-case: true

flowable:
  # 默认开启历史数据
  db-history-used: true
  # 默认历史级别
  history-level: audit
  # 默认扫描流程定义目录
  process-definition-location-prefix: classpath*:/processes/
  process-definition-location-suffixes:
    - "**.bpmn20.xml"
    - "**.bpmn"
```

开发环境配置放在 `application-dev.yml`：

```yaml
spring:
  datasource:
    url: jdbc:mysql://127.0.0.1:3306/flowable_boot?useUnicode=true&characterEncoding=utf8&useSSL=false&serverTimezone=Asia/Shanghai&allowPublicKeyRetrieval=true
    username: flowable_user
    password: Flowable@123456
    driver-class-name: com.mysql.cj.jdbc.Driver

flowable:
  # 开发环境允许自动建表
  database-schema-update: true
  # 开发环境允许自动扫描并部署 resources/processes 下的流程
  check-process-definitions: true

logging:
  level:
    io.github.atengk: debug
    org.flowable: info
```

生产环境配置放在 `application-prod.yml`：

```yaml
spring:
  datasource:
    url: ${MYSQL_URL}
    username: ${MYSQL_USERNAME}
    password: ${MYSQL_PASSWORD}
    driver-class-name: com.mysql.cj.jdbc.Driver
    hikari:
      pool-name: FlowableHikariCP
      minimum-idle: 10
      maximum-pool-size: 50
      connection-timeout: 30000
      idle-timeout: 600000
      max-lifetime: 1800000

flowable:
  # 生产环境关闭自动建表，避免不可控表结构变更
  database-schema-update: false
  # 是否自动部署流程定义需按发布策略决定
  check-process-definitions: false
  db-history-used: true
  history-level: audit
  async-executor-activate: true

logging:
  level:
    io.github.atengk: info
    org.flowable: warn
```

启动命令示例：

```bash
# 开发环境启动
java -jar springboot-flowable-demo.jar --spring.profiles.active=dev

# 生产环境启动
java -jar springboot-flowable-demo.jar --spring.profiles.active=prod
```

生产环境建议将敏感配置通过环境变量注入：

```bash
export MYSQL_URL="jdbc:mysql://10.0.0.10:3306/flowable_boot?useUnicode=true&characterEncoding=utf8&useSSL=false&serverTimezone=Asia/Shanghai"
export MYSQL_USERNAME="flowable_user"
export MYSQL_PASSWORD="实际生产密码"

java -jar springboot-flowable-demo.jar --spring.profiles.active=prod
```

配置调整注意事项：

| 配置项                      | 生产建议                                    |
| --------------------------- | ------------------------------------------- |
| `database-schema-update`    | 设置为 `false`                              |
| `check-process-definitions` | 建议关闭，流程发布由后台或脚本控制          |
| `history-level`             | 常规审批使用 `audit`                        |
| `async-executor-activate`   | 使用定时器、异步任务时开启                  |
| 数据库密码                  | 使用环境变量或配置中心                      |
| 日志级别                    | 业务包 `info`，Flowable 包 `warn` 或 `info` |

### 日志配置

日志配置用于记录流程部署、流程启动、任务审批、驳回、撤回、终止、监听器执行和异常信息。工作流系统的问题排查高度依赖日志，建议日志中至少包含流程实例 ID、任务 ID、业务主键、审批人和审批动作。

推荐日志字段如下：

| 字段                   | 说明                      |
| ---------------------- | ------------------------- |
| `processInstanceId`    | 流程实例 ID               |
| `processDefinitionKey` | 流程定义 Key              |
| `businessKey`          | 业务主键                  |
| `taskId`               | 任务 ID                   |
| `taskDefinitionKey`    | 节点 Key                  |
| `userId`               | 操作用户                  |
| `approveAction`        | 审批动作                  |
| `approveResult`        | 审批结果                  |
| `traceId`              | 链路追踪 ID，如果系统支持 |

推荐使用 `logback-spring.xml` 管理日志输出。

文件位置：`src/main/resources/logback-spring.xml`

```xml
<?xml version="1.0" encoding="UTF-8"?>
<configuration scan="true">

    <!-- 应用名称 -->
    <springProperty scope="context" name="APP_NAME" source="spring.application.name" defaultValue="springboot-flowable-demo"/>

    <!-- 日志目录，可通过环境变量 LOG_PATH 覆盖 -->
    <property name="LOG_PATH" value="${LOG_PATH:-./logs}"/>

    <!-- 控制台日志格式 -->
    <property name="CONSOLE_PATTERN"
              value="%d{yyyy-MM-dd HH:mm:ss.SSS} %-5level [%thread] %logger{36} - %msg%n"/>

    <!-- 文件日志格式 -->
    <property name="FILE_PATTERN"
              value="%d{yyyy-MM-dd HH:mm:ss.SSS} %-5level [%thread] %logger{64} - %msg%n"/>

    <!-- 控制台输出 -->
    <appender name="CONSOLE" class="ch.qos.logback.core.ConsoleAppender">
        <encoder>
            <pattern>${CONSOLE_PATTERN}</pattern>
            <charset>UTF-8</charset>
        </encoder>
    </appender>

    <!-- 业务日志文件 -->
    <appender name="APP_FILE" class="ch.qos.logback.core.rolling.RollingFileAppender">
        <file>${LOG_PATH}/${APP_NAME}.log</file>
        <encoder>
            <pattern>${FILE_PATTERN}</pattern>
            <charset>UTF-8</charset>
        </encoder>
        <rollingPolicy class="ch.qos.logback.core.rolling.TimeBasedRollingPolicy">
            <!-- 按天归档日志 -->
            <fileNamePattern>${LOG_PATH}/${APP_NAME}.%d{yyyy-MM-dd}.%i.log.gz</fileNamePattern>
            <maxHistory>30</maxHistory>
            <timeBasedFileNamingAndTriggeringPolicy class="ch.qos.logback.core.rolling.SizeAndTimeBasedFNATP">
                <!-- 单个日志文件最大 100MB -->
                <maxFileSize>100MB</maxFileSize>
            </timeBasedFileNamingAndTriggeringPolicy>
        </rollingPolicy>
    </appender>

    <!-- Flowable 日志：生产环境不建议开启 debug -->
    <logger name="org.flowable" level="INFO"/>

    <!-- 项目业务日志 -->
    <logger name="io.github.atengk" level="INFO"/>

    <!-- 根日志 -->
    <root level="INFO">
        <appender-ref ref="CONSOLE"/>
        <appender-ref ref="APP_FILE"/>
    </root>

</configuration>
```

业务代码中的日志建议：

```java
log.info("任务审批完成，任务ID：{}，流程实例ID：{}，业务主键：{}，审批人：{}，审批结果：{}",
        taskId, processInstanceId, businessKey, userId, approveResult);
```

异常日志建议：

```java
log.error("流程任务审批失败，任务ID：{}，流程实例ID：{}，操作人：{}",
        taskId, processInstanceId, userId, e);
```

日志配置注意事项：

| 注意事项                        | 说明                               |
| ------------------------------- | ---------------------------------- |
| 生产不要长期开启 Flowable debug | 日志量大，影响性能和存储           |
| 关键流程操作必须打日志          | 部署、启动、审批、驳回、撤回、终止 |
| 异常日志保留堆栈                | 便于定位 Flowable 内部异常         |
| 日志中避免敏感数据              | 不打印身份证、银行卡、密钥等信息   |
| 建议接入 traceId                | 便于网关、应用、数据库日志串联     |

### 常见运行问题处理

常见运行问题处理用于快速定位开发、测试、生产环境中经常出现的 Flowable 集成问题。排查问题时建议先看应用启动日志，再看 Flowable 表数据，最后结合业务表和审批记录分析。

常见问题如下：

| 问题                       | 常见原因                                         | 处理方式                                      |
| -------------------------- | ------------------------------------------------ | --------------------------------------------- |
| 启动时报 Flowable 表不存在 | 生产关闭自动建表但未初始化表                     | 执行对应版本 Flowable SQL                     |
| BPMN 文件未自动部署        | 文件路径不对或 `check-process-definitions=false` | 检查 `resources/processes` 和配置             |
| 启动流程提示流程定义不存在 | 流程未部署或 Key 写错                            | 查询 `ACT_RE_PROCDEF` 确认 Key                |
| 任务查询为空               | 办理人、候选人、候选组不匹配                     | 检查 BPMN 配置和用户角色编码                  |
| 审批时报任务不存在         | 任务已被处理或流程已流转                         | 刷新待办并做后端幂等处理                      |
| 网关流转失败               | 条件变量缺失或类型错误                           | 检查流程变量和表达式                          |
| 监听器不执行               | BPMN Bean 名称错误或未纳入 Spring                | 检查 `delegateExpression` 和 `@Component`     |
| 流程结束但业务状态未更新   | 监听器异常或业务状态未维护                       | 检查监听器日志和事务                          |
| 历史任务查不到             | 历史配置未开启                                   | 开启 `db-history-used` 和合理 `history-level` |
| 自动建表失败               | 数据库账号没有 DDL 权限                          | 开发授权 DDL，生产手动初始化                  |

常用排查 SQL 如下：

```sql
-- 查询流程定义
SELECT ID_, KEY_, NAME_, VERSION_, DEPLOYMENT_ID_
FROM ACT_RE_PROCDEF
WHERE KEY_ = 'leaveApprove'
ORDER BY VERSION_ DESC;

-- 查询运行中流程实例
SELECT ID_, PROC_INST_ID_, PROC_DEF_ID_, BUSINESS_KEY_
FROM ACT_RU_EXECUTION
WHERE BUSINESS_KEY_ = 'LEAVE_10001';

-- 查询当前运行任务
SELECT ID_, NAME_, ASSIGNEE_, PROC_INST_ID_, TASK_DEF_KEY_, CREATE_TIME_
FROM ACT_RU_TASK
WHERE PROC_INST_ID_ = '25001';

-- 查询历史任务
SELECT ID_, NAME_, ASSIGNEE_, PROC_INST_ID_, TASK_DEF_KEY_, START_TIME_, END_TIME_
FROM ACT_HI_TASKINST
WHERE PROC_INST_ID_ = '25001'
ORDER BY START_TIME_ ASC;

-- 查询流程变量
SELECT NAME_, TYPE_, TEXT_, LONG_, DOUBLE_, PROC_INST_ID_
FROM ACT_RU_VARIABLE
WHERE PROC_INST_ID_ = '25001';

-- 查询历史流程实例
SELECT ID_, PROC_INST_ID_, PROC_DEF_ID_, BUSINESS_KEY_, START_TIME_, END_TIME_, DELETE_REASON_
FROM ACT_HI_PROCINST
WHERE PROC_INST_ID_ = '25001';
```

运行问题排查顺序建议如下：

```text
1. 看应用启动日志
   确认数据库连接、Flowable 引擎初始化、流程部署是否正常。

2. 查流程定义表
   确认流程 Key、版本号、部署 ID 是否存在。

3. 查运行任务表
   确认当前任务是否存在、办理人是否正确。

4. 查流程变量表
   确认网关表达式和动态审批人依赖的变量是否存在。

5. 查历史任务表
   确认流程是否已经被处理、结束、驳回或终止。

6. 查业务表和审批记录表
   确认业务状态是否和 Flowable 状态一致。
```

生产运行建议：

| 建议                 | 说明                                       |
| -------------------- | ------------------------------------------ |
| 流程发布要可回滚     | 新版本 BPMN 发布前先测试验证               |
| 不修改运行中流程定义 | 新流程规则通过新版本部署                   |
| 数据库定期备份       | Flowable 历史表和业务审批记录都很关键      |
| 历史表定期归档       | 审批量大时需要归档策略                     |
| 监控接口耗时         | 待办查询、流程图查询、历史轨迹查询容易变慢 |
| 保留操作审计         | 审批、驳回、撤回、终止必须可追溯           |
| 定期检查异常流程     | 例如长时间停留、无办理人、变量缺失的流程   |

最终验证标准是：应用可以稳定启动，流程可以部署和启动，待办能够正确生成，审批能按 BPMN 规则流转，业务状态和审批记录能同步维护，异常场景能返回清晰提示，生产配置不会自动修改表结构。