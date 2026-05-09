# Spring Boot 集成 Warm-Flow 工作流引擎开发

## 项目概述

本项目基于 Spring Boot 3 集成 Warm-Flow 工作流引擎，用于在业务系统中实现流程定义、流程发起、任务审批、流程流转、审批记录、流程状态同步和流程图展示等能力。

Warm-Flow 作为轻量级工作流引擎，适合中后台管理系统中常见的审批流场景，例如请假审批、报销审批、采购审批、合同审批、用章审批、项目立项审批等。通过引入工作流引擎，可以将业务数据处理与审批流程流转解耦，降低审批逻辑硬编码带来的维护成本。

### 开发背景

在传统业务系统中，审批流程通常直接写在业务代码中。例如请假单、报销单、采购单等模块各自维护审批节点、审批人、审批状态和审批记录。随着业务流程复杂度提升，这种方式会出现以下问题：

1. 流程节点与业务代码强耦合，审批规则变更时需要修改代码。
2. 不同业务模块重复实现审批逻辑，开发成本较高。
3. 审批状态、审批记录、待办任务等数据难以统一管理。
4. 条件分支、动态办理人、退回、转办、委派等复杂流程能力扩展困难。
5. 流程图展示、流程追踪、审批历史查看等功能需要额外开发。

引入 Warm-Flow 后，可以将流程定义、流程实例、审批任务、流程变量、监听器和业务回调统一交给流程引擎管理。业务系统只需要关注业务单据本身，并在关键节点与流程引擎交互即可。

本项目的核心思路是：业务系统负责业务数据，Warm-Flow 负责流程流转，两者通过业务主键、流程实例 ID 和流程状态进行关联。

### 功能目标

本项目目标是完成 Spring Boot 3 与 Warm-Flow 的基础集成，并封装一套适合业务系统复用的工作流能力。

主要功能目标如下：

| 功能模块       | 目标说明                                                   |
| -------------- | ---------------------------------------------------------- |
| 流程定义管理   | 支持流程模型维护、流程定义导入、流程发布和流程版本管理     |
| 流程实例管理   | 支持绑定业务单据发起流程，查询流程实例状态，撤销或终止流程 |
| 任务审批管理   | 支持待办查询、已办查询、审批通过、驳回、退回、转办和委派   |
| 办理人扩展     | 支持按用户、角色、部门、岗位或业务规则动态解析办理人       |
| 条件表达式     | 支持根据流程变量控制条件分支，例如金额、类型、部门、级别等 |
| 监听器扩展     | 支持流程启动、任务创建、任务完成、流程结束等节点的业务回调 |
| 业务系统集成   | 支持业务单据与流程实例绑定，流程状态同步，审批记录落库     |
| 前端页面对接   | 支持流程发起页、待办任务页、审批详情页和流程图展示页对接   |
| 日志与异常处理 | 支持流程操作日志、异常捕获、业务错误提示和问题排查         |

集成完成后，业务模块在发起审批时只需要传入业务单据 ID、流程定义标识、发起人、流程变量等参数，即可复用统一的流程能力。

### 技术选型

本项目采用 Spring Boot 3 作为后端基础框架，结合 MyBatis-Plus、MySQL、Warm-Flow 和常用后端组件完成工作流能力集成。

| 技术                       | 推荐版本            | 说明                                                  |
| -------------------------- | ------------------- | ----------------------------------------------------- |
| JDK                        | 17 或 21            | Spring Boot 3 要求 JDK 17 及以上版本                  |
| Spring Boot                | 3.x                 | 后端基础框架，提供 Web、配置、事务、校验、日志等能力  |
| Warm-Flow                  | 按官方稳定版本选择  | 工作流引擎，负责流程定义、流程实例和审批任务流转      |
| MySQL                      | 8.0.x               | 存储业务数据和流程引擎数据                            |
| MyBatis-Plus               | 3.5.x               | 负责业务表 CRUD、分页查询和条件构造                   |
| Maven                      | 3.6.3+              | 项目构建与依赖管理                                    |
| Hutool                     | 5.8.x               | 常用工具类，处理字符串、集合、日期、JSON、Bean 转换等 |
| Lombok                     | 1.18.x              | 简化 Entity、DTO、VO、Service 等样板代码              |
| Spring Validation          | 随 Spring Boot 管理 | 接口参数校验                                          |
| Knife4j / SpringDoc        | 按项目规范选择      | 接口文档生成                                          |
| Sa-Token / Spring Security | 按项目权限体系选择  | 登录认证、接口鉴权、用户上下文获取                    |

ORM 层推荐使用 MyBatis-Plus。对于后台管理系统来说，MyBatis-Plus 具备较好的 CRUD 封装能力，能够降低业务表开发成本，也便于后续与分页插件、租户插件、数据权限插件等能力集成。

## 环境准备

本章节用于说明项目开发前需要准备的基础运行环境、数据库环境、ORM 选择和 Maven 依赖配置。建议先完成 JDK、Maven、MySQL 和 Spring Boot 基础项目搭建，再接入 Warm-Flow 相关依赖。

### JDK 与 Spring Boot 版本

Spring Boot 3 要求 JDK 17 及以上版本，因此本项目推荐使用 JDK 17 或 JDK 21。JDK 17 是 Spring Boot 3 的最低长期支持版本，JDK 21 是较新的长期支持版本，新项目可以根据公司基础镜像、运维规范和生产环境兼容性进行选择。

推荐环境如下：

| 环境项      | 推荐版本      | 说明                                         |
| ----------- | ------------- | -------------------------------------------- |
| JDK         | 17 或 21      | Spring Boot 3 运行基础环境                   |
| Spring Boot | 3.x           | 项目基础框架                                 |
| Maven       | 3.6.3+        | 项目构建工具                                 |
| 编码        | UTF-8         | 统一源码、配置文件、SQL 脚本和数据库连接编码 |
| 时区        | Asia/Shanghai | 统一接口入参、数据库时间和日志时间           |

开发环境检查命令如下：

```bash
# 查看 JDK 版本，确认主版本为 17 或 21
java -version

# 查看 Maven 版本，确认 Maven 版本不低于 3.6.3
mvn -version

# 查看 Git 版本，确认本地具备代码拉取和版本管理能力
git --version
```

如果项目从 Spring Boot 2 升级到 Spring Boot 3，需要重点检查 `javax.*` 到 `jakarta.*` 的包名迁移问题。例如 Servlet、Validation、Persistence 等相关依赖都可能涉及迁移。

### 数据库与 ORM 选择

本项目推荐使用 MySQL 8.0.x 作为数据库，使用 MyBatis-Plus 作为业务 ORM 框架，并选择 Warm-Flow 对应的 MyBatis-Plus 适配依赖。

推荐组合如下：

| 类型     | 选型                          | 说明                                               |
| -------- | ----------------------------- | -------------------------------------------------- |
| 数据库   | MySQL 8.0.x                   | 中后台系统常用，生态成熟，便于部署和维护           |
| ORM      | MyBatis-Plus                  | 适合业务表 CRUD、分页、条件查询和通用 Service 封装 |
| 连接池   | HikariCP                      | Spring Boot 默认连接池，性能稳定                   |
| 事务管理 | Spring Transaction            | 保证流程流转与业务状态更新的一致性                 |
| 数据迁移 | SQL 脚本 / Flyway / Liquibase | 根据团队规范选择，建议将流程表脚本纳入版本管理     |

数据库初始化建议分为两部分：

| 脚本类型         | 说明                                                         |
| ---------------- | ------------------------------------------------------------ |
| Warm-Flow 引擎表 | 用于存储流程定义、流程实例、任务、流程变量、历史记录等流程数据 |
| 业务扩展表       | 用于存储业务单据、流程业务绑定关系、审批记录、操作日志等业务数据 |

创建数据库示例：

```sql
-- 创建业务数据库，字符集统一使用 utf8mb4
CREATE DATABASE IF NOT EXISTS workflow_boot
DEFAULT CHARACTER SET utf8mb4
DEFAULT COLLATE utf8mb4_unicode_ci;
```

业务表中建议保留以下流程关联字段：

| 字段                | 说明                                                   |
| ------------------- | ------------------------------------------------------ |
| business_id         | 业务单据主键                                           |
| business_code       | 业务单据编号                                           |
| process_def_key     | 流程定义标识                                           |
| process_instance_id | 流程实例 ID                                            |
| process_status      | 业务流程状态，例如草稿、审批中、已通过、已驳回、已撤销 |
| start_user_id       | 流程发起人                                             |
| start_time          | 流程发起时间                                           |
| finish_time         | 流程结束时间                                           |

通过业务表与流程实例 ID 进行关联，可以在业务列表、审批详情、流程图展示和审批记录中统一追踪流程状态。

### Maven 依赖配置

本项目使用 Maven 管理依赖。依赖配置中需要引入 Spring Boot Web、参数校验、MyBatis-Plus、Warm-Flow、MySQL 驱动、Hutool、Lombok 和测试依赖。

文件位置：`pom.xml`

下面配置用于 Spring Boot 3、MyBatis-Plus、Warm-Flow 的基础依赖管理。

```xml
<properties>
    <!-- JDK 版本：Spring Boot 3 最低要求 Java 17 -->
    <java.version>17</java.version>

    <!-- Spring Boot 版本：按公司项目基线调整 -->
    <spring-boot.version>3.2.7</spring-boot.version>

    <!-- Warm-Flow 版本：按官方稳定版本调整 -->
    <warm-flow.version>1.8.5</warm-flow.version>

    <!-- Hutool 工具包版本 -->
    <hutool.version>5.8.36</hutool.version>

    <!-- MyBatis-Plus 版本 -->
    <mybatis-plus.version>3.5.8</mybatis-plus.version>

    <!-- MySQL 驱动版本 -->
    <mysql.version>8.4.0</mysql.version>
</properties>

<dependencyManagement>
    <dependencies>
        <!-- Spring Boot 依赖版本管理，统一控制 Spring 生态依赖版本 -->
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-dependencies</artifactId>
            <version>${spring-boot.version}</version>
            <type>pom</type>
            <scope>import</scope>
        </dependency>
    </dependencies>
</dependencyManagement>

<dependencies>
    <!-- Web MVC 能力，用于提供流程定义、流程实例、任务审批等 REST 接口 -->
    <dependency>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-starter-web</artifactId>
    </dependency>

    <!-- 参数校验能力，用于 Controller 入参校验 -->
    <dependency>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-starter-validation</artifactId>
    </dependency>

    <!-- AOP 能力，可用于操作日志、权限校验、流程扩展切面等场景 -->
    <dependency>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-starter-aop</artifactId>
    </dependency>

    <!-- MyBatis-Plus Spring Boot 3 Starter，用于业务表 CRUD 和分页查询 -->
    <dependency>
        <groupId>com.baomidou</groupId>
        <artifactId>mybatis-plus-spring-boot3-starter</artifactId>
        <version>${mybatis-plus.version}</version>
    </dependency>

    <!-- Warm-Flow MyBatis-Plus Spring Boot 3 适配依赖，用于集成流程引擎 -->
    <dependency>
        <groupId>org.dromara.warm</groupId>
        <artifactId>warm-flow-mybatis-plus-sb3-starter</artifactId>
        <version>${warm-flow.version}</version>
    </dependency>

    <!-- MySQL 驱动，用于连接 MySQL 8 数据库 -->
    <dependency>
        <groupId>com.mysql</groupId>
        <artifactId>mysql-connector-j</artifactId>
        <version>${mysql.version}</version>
        <scope>runtime</scope>
    </dependency>

    <!-- Hutool 工具包，用于字符串、集合、日期、JSON、Bean 等常用处理 -->
    <dependency>
        <groupId>cn.hutool</groupId>
        <artifactId>hutool-all</artifactId>
        <version>${hutool.version}</version>
    </dependency>

    <!-- Lombok，用于减少 DTO、VO、Entity 中的样板代码 -->
    <dependency>
        <groupId>org.projectlombok</groupId>
        <artifactId>lombok</artifactId>
        <optional>true</optional>
    </dependency>

    <!-- Spring Boot 测试依赖，用于流程发起、审批流转、条件分支等测试 -->
    <dependency>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-starter-test</artifactId>
        <scope>test</scope>
    </dependency>
</dependencies>

<build>
    <plugins>
        <!-- Spring Boot Maven 插件，用于打包可执行 Jar -->
        <plugin>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-maven-plugin</artifactId>
        </plugin>

        <!-- Maven 编译插件，统一源码和目标字节码版本 -->
        <plugin>
            <groupId>org.apache.maven.plugins</groupId>
            <artifactId>maven-compiler-plugin</artifactId>
            <configuration>
                <source>${java.version}</source>
                <target>${java.version}</target>
                <encoding>UTF-8</encoding>
            </configuration>
        </plugin>
    </plugins>
</build>
```

如果项目使用原生 MyBatis，而不是 MyBatis-Plus，可以将 Warm-Flow 依赖替换为对应的 MyBatis Starter。

```xml
<!-- Warm-Flow MyBatis Spring Boot Starter，用于原生 MyBatis 技术栈 -->
<dependency>
    <groupId>org.dromara.warm</groupId>
    <artifactId>warm-flow-mybatis-sb-starter</artifactId>
    <version>${warm-flow.version}</version>
</dependency>
```

依赖配置完成后，可以执行以下命令验证依赖是否能够正常解析：

```bash
# 清理并编译项目，验证 Maven 依赖和源码编译是否正常
mvn clean compile

# 查看 Warm-Flow 相关依赖树，排查版本冲突
mvn dependency:tree -Dincludes=org.dromara.warm

# 查看 MyBatis-Plus 相关依赖树，确认 Spring Boot 3 Starter 是否生效
mvn dependency:tree -Dincludes=com.baomidou
```

如果出现依赖冲突，建议优先检查以下内容：

1. 是否混用了 Spring Boot 2 和 Spring Boot 3 的依赖。
2. 是否存在 `javax.*` 与 `jakarta.*` 包冲突。
3. Warm-Flow Starter 是否与当前 ORM 技术栈匹配。
4. MyBatis-Plus Starter 是否使用了 Spring Boot 3 对应版本。
5. 项目中是否存在旧版接口文档、权限框架、Servlet 组件或 Validation 组件。



## Warm-Flow 基础集成

本章节用于完成 Warm-Flow 在 Spring Boot 3 项目中的基础接入，包括流程引擎表初始化、项目配置、启动检查和基础接口验证。Warm-Flow 官方仓库说明组件脚本位于 `sql` 目录，首次导入需要先创建数据库并执行对应数据库的全量脚本，版本升级时再执行对应升级脚本。([GitHub](https://github.com/dromara/warm-flow?utm_source=chatgpt.com))

### 数据库脚本初始化

Warm-Flow 需要在业务数据库中初始化流程引擎表，用于保存流程定义、流程节点、流程跳转、流程实例、待办任务、历史任务和办理人等数据。建议将 Warm-Flow 官方 SQL 脚本纳入项目的数据库版本管理中，避免开发环境、测试环境和生产环境脚本不一致。

脚本执行顺序建议如下：

| 顺序 | 脚本类型           | 说明                                     |
| ---- | ------------------ | ---------------------------------------- |
| 1    | 创建业务数据库     | 创建项目使用的 MySQL 数据库              |
| 2    | Warm-Flow 全量脚本 | 初始化 Warm-Flow 引擎表                  |
| 3    | 业务扩展表脚本     | 初始化业务单据表、流程绑定表、审批记录表 |
| 4    | 初始化流程定义数据 | 导入默认流程定义 JSON                    |
| 5    | 初始化测试数据     | 开发环境可选，生产环境不建议执行         |

创建数据库示例：

```sql
-- 创建工作流业务数据库
CREATE DATABASE IF NOT EXISTS workflow_boot
DEFAULT CHARACTER SET utf8mb4
DEFAULT COLLATE utf8mb4_unicode_ci;

-- 切换数据库
USE workflow_boot;
```

业务系统建议额外维护一张流程业务绑定表，用于建立业务单据与流程实例之间的关系。Warm-Flow 负责流程流转，业务系统负责业务状态，两者通过 `business_id` 和 `instance_id` 进行关联。

```sql
-- 业务流程绑定表：用于关联业务单据与流程实例
CREATE TABLE IF NOT EXISTS wf_business_flow (
    id BIGINT NOT NULL COMMENT '主键ID',
    business_id VARCHAR(64) NOT NULL COMMENT '业务主键',
    business_code VARCHAR(64) DEFAULT NULL COMMENT '业务编号',
    business_type VARCHAR(64) NOT NULL COMMENT '业务类型，例如 leave、expense、purchase',
    flow_code VARCHAR(64) NOT NULL COMMENT '流程编码',
    instance_id BIGINT DEFAULT NULL COMMENT '流程实例ID',
    process_status VARCHAR(32) NOT NULL COMMENT '流程状态',
    start_user_id VARCHAR(64) DEFAULT NULL COMMENT '发起人ID',
    start_time DATETIME DEFAULT NULL COMMENT '发起时间',
    finish_time DATETIME DEFAULT NULL COMMENT '完成时间',
    create_time DATETIME NOT NULL COMMENT '创建时间',
    update_time DATETIME DEFAULT NULL COMMENT '更新时间',
    deleted TINYINT NOT NULL DEFAULT 0 COMMENT '删除标识：0未删除，1已删除',
    PRIMARY KEY (id),
    KEY idx_business_id (business_id),
    KEY idx_instance_id (instance_id),
    KEY idx_flow_code (flow_code)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='业务流程绑定表';
```

审批记录也建议由业务系统单独落库。Warm-Flow 的历史任务表用于引擎内部流转追踪，业务审批记录表用于业务页面展示、审计查询和消息通知。

```sql
-- 业务审批记录表：用于业务侧展示审批轨迹
CREATE TABLE IF NOT EXISTS wf_approval_record (
    id BIGINT NOT NULL COMMENT '主键ID',
    business_id VARCHAR(64) NOT NULL COMMENT '业务主键',
    instance_id BIGINT NOT NULL COMMENT '流程实例ID',
    task_id BIGINT DEFAULT NULL COMMENT '任务ID',
    node_code VARCHAR(64) DEFAULT NULL COMMENT '节点编码',
    node_name VARCHAR(128) DEFAULT NULL COMMENT '节点名称',
    operator_id VARCHAR(64) NOT NULL COMMENT '操作人ID',
    operator_name VARCHAR(64) DEFAULT NULL COMMENT '操作人名称',
    action_type VARCHAR(32) NOT NULL COMMENT '审批动作：PASS、REJECT、REVOKE、TERMINATION',
    message VARCHAR(500) DEFAULT NULL COMMENT '审批意见',
    operate_time DATETIME NOT NULL COMMENT '操作时间',
    PRIMARY KEY (id),
    KEY idx_business_id (business_id),
    KEY idx_instance_id (instance_id),
    KEY idx_task_id (task_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='业务审批记录表';
```

脚本执行完成后，需要检查 Warm-Flow 引擎表和业务扩展表是否创建成功。

```sql
-- 查看 Warm-Flow 相关表
SHOW TABLES LIKE 'flow_%';

-- 查看业务流程扩展表
SHOW TABLES LIKE 'wf_%';
```

### 工作流引擎配置

Warm-Flow 在 Spring Boot 项目中通常通过 Starter 自动装配核心服务。项目侧主要需要配置数据源、MyBatis-Plus、Mapper 扫描、日志级别和业务侧流程参数。Warm-Flow 官方说明支持 MyBatis、MyBatis-Plus、MyBatis-Flex、JPA、Easy-Query、BeetlSql 等 ORM，也支持 MySQL、Oracle、PostgreSQL、SQL Server 等数据库。([Warm-Flow官网](https://www.warm-flow.com/?utm_source=chatgpt.com))

文件位置：`src/main/resources/application.yml`

下面配置用于 Spring Boot 3、MyBatis-Plus、MySQL 和 Warm-Flow 基础集成。

```yaml
server:
  port: 8080

spring:
  application:
    name: workflow-boot

  datasource:
    driver-class-name: com.mysql.cj.jdbc.Driver
    url: jdbc:mysql://localhost:3306/workflow_boot?useUnicode=true&characterEncoding=utf8&serverTimezone=Asia/Shanghai&useSSL=false
    username: root
    password: root

  jackson:
    date-format: yyyy-MM-dd HH:mm:ss
    time-zone: Asia/Shanghai

mybatis-plus:
  # 扫描业务 Mapper XML，如果项目没有 XML Mapper 可以不配置
  mapper-locations: classpath*:/mapper/**/*.xml

  configuration:
    # SQL 日志，开发环境可打开，生产环境建议关闭或交给日志系统控制
    log-impl: org.apache.ibatis.logging.stdout.StdOutImpl

  global-config:
    db-config:
      # 主键策略按项目规范调整
      id-type: assign_id
      # 逻辑删除字段，业务表可使用；Warm-Flow 引擎表是否使用按官方脚本为准
      logic-delete-field: deleted
      logic-delete-value: 1
      logic-not-delete-value: 0

logging:
  level:
    io.github.atengk: debug
    com.warm.flow: info

workflow:
  # 业务侧默认流程配置，便于 Service 中统一读取
  default-flow-code: leave_process
  # 是否在开发环境自动导入流程定义，生产环境建议关闭
  auto-import-definition: false
```

如果项目使用 MyBatis-Plus，需要在启动类上配置 Mapper 扫描路径。Warm-Flow Starter 会提供引擎侧能力，业务项目仍需要扫描自己的 Mapper。

文件位置：`src/main/java/io/github/atengk/WorkflowBootApplication.java`

```java
package io.github.atengk;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * 工作流服务启动类
 *
 * @author Ateng
 * @since 2026-05-08
 */
@MapperScan("io.github.atengk.**.mapper")
@SpringBootApplication
public class WorkflowBootApplication {

    public static void main(String[] args) {
        SpringApplication.run(WorkflowBootApplication.class, args);
    }

}
```

为了避免后续业务模块直接散落调用 Warm-Flow 原生接口，建议在业务系统中封装一个统一的门面服务。Warm-Flow 官方接口文档中，`DefService` 用于流程定义导入导出，`InsService.start` 用于发起流程实例，`TaskService.skip`、`revoke`、`termination` 等用于任务流转、撤销和终止。([Warm-Flow官网](https://www.warm-flow.com/master/primary/api.html?utm_source=chatgpt.com))

文件位置：`src/main/java/io/github/atengk/workflow/service/WarmFlowFacadeService.java`

```java
package io.github.atengk.workflow.service;

import cn.hutool.core.map.MapUtil;
import cn.hutool.core.util.StrUtil;
import com.warm.flow.core.dto.FlowParams;
import com.warm.flow.core.entity.Definition;
import com.warm.flow.core.entity.Instance;
import com.warm.flow.core.enums.SkipType;
import com.warm.flow.core.service.DefService;
import com.warm.flow.core.service.InsService;
import com.warm.flow.core.service.TaskService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;
import java.util.Map;

/**
 * Warm-Flow 工作流门面服务
 *
 * @author Ateng
 * @since 2026-05-08
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class WarmFlowFacadeService {

    private final DefService defService;
    private final InsService insService;
    private final TaskService taskService;

    /**
     * 通过文件导入流程定义
     *
     * @param file 流程定义文件
     * @return 流程定义
     */
    public Definition importDefinition(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new IllegalArgumentException("流程定义文件不能为空");
        }

        try (InputStream inputStream = file.getInputStream()) {
            Definition definition = defService.importIs(inputStream);
            log.info("流程定义导入成功，文件名：{}", file.getOriginalFilename());
            return definition;
        } catch (IOException e) {
            log.error("流程定义文件读取失败，文件名：{}", file.getOriginalFilename(), e);
            throw new IllegalStateException("流程定义文件读取失败", e);
        }
    }

    /**
     * 通过 JSON 字符串导入流程定义
     *
     * @param definitionJson 流程定义 JSON
     * @return 流程定义
     */
    public Definition importDefinitionJson(String definitionJson) {
        if (StrUtil.isBlank(definitionJson)) {
            throw new IllegalArgumentException("流程定义 JSON 不能为空");
        }

        Definition definition = defService.importJson(definitionJson);
        log.info("流程定义 JSON 导入成功");
        return definition;
    }

    /**
     * 发起流程实例
     *
     * @param businessId 业务主键
     * @param flowCode 流程编码
     * @param handler 发起人ID
     * @param variable 流程变量
     * @return 流程实例
     */
    public Instance startProcess(String businessId, String flowCode, String handler, Map<String, Object> variable) {
        if (StrUtil.hasBlank(businessId, flowCode, handler)) {
            throw new IllegalArgumentException("业务主键、流程编码、发起人不能为空");
        }

        FlowParams flowParams = FlowParams.build()
                .flowCode(flowCode)
                .handler(handler);

        if (MapUtil.isNotEmpty(variable)) {
            flowParams.variable(variable);
        }

        Instance instance = insService.start(businessId, flowParams);
        log.info("流程发起成功，业务ID：{}，流程编码：{}，实例ID：{}", businessId, flowCode, instance.getId());
        return instance;
    }

    /**
     * 审批通过
     *
     * @param taskId 任务ID
     * @param handler 办理人ID
     * @param message 审批意见
     * @param variable 流程变量
     * @return 流程实例
     */
    public Instance pass(Long taskId, String handler, String message, Map<String, Object> variable) {
        FlowParams flowParams = FlowParams.build()
                .skipType(SkipType.PASS.getKey())
                .handler(handler)
                .message(message);

        if (MapUtil.isNotEmpty(variable)) {
            flowParams.variable(variable);
        }

        Instance instance = taskService.skip(taskId, flowParams);
        log.info("流程审批通过，任务ID：{}，办理人：{}", taskId, handler);
        return instance;
    }

    /**
     * 撤销流程
     *
     * @param instanceId 流程实例ID
     * @param handler 操作人ID
     * @param message 撤销原因
     * @return 流程实例
     */
    public Instance revoke(Long instanceId, String handler, String message) {
        FlowParams flowParams = FlowParams.build()
                .handler(handler)
                .message(message);

        Instance instance = taskService.revoke(instanceId, flowParams);
        log.info("流程撤销成功，实例ID：{}，操作人：{}", instanceId, handler);
        return instance;
    }

    /**
     * 终止流程
     *
     * @param taskId 任务ID
     * @param handler 操作人ID
     * @param message 终止原因
     * @return 流程实例
     */
    public Instance terminate(Long taskId, String handler, String message) {
        FlowParams flowParams = FlowParams.build()
                .handler(handler)
                .message(message);

        Instance instance = taskService.termination(taskId, flowParams);
        log.info("流程终止成功，任务ID：{}，操作人：{}", taskId, handler);
        return instance;
    }

}
```

### 启动与基础验证

项目启动前，需要确认 Maven 依赖、数据库连接、Warm-Flow 表结构和业务扩展表均已准备完成。Warm-Flow 官方接口文档显示流程定义支持通过输入流、JSON 字符串和 JSON 对象导入，流程实例启动时需要传入 `businessId` 和 `flowParams`，其中 `flowCode` 是必传字段。([Warm-Flow官网](https://www.warm-flow.com/master/primary/api.html?utm_source=chatgpt.com))

启动项目：

```bash
# 清理并编译项目
mvn clean compile

# 启动 Spring Boot 服务
mvn spring-boot:run
```

启动后检查日志中是否存在以下信息：

```text
Started WorkflowBootApplication
```

基础验证建议按以下顺序执行：

| 验证项       | 验证方式                     | 预期结果                         |
| ------------ | ---------------------------- | -------------------------------- |
| 数据库连接   | 启动项目或执行简单查询       | 无连接异常                       |
| Warm-Flow 表 | `SHOW TABLES LIKE 'flow_%';` | 能看到流程引擎表                 |
| 业务扩展表   | `SHOW TABLES LIKE 'wf_%';`   | 能看到业务流程绑定表和审批记录表 |
| 流程定义导入 | 调用导入接口                 | 返回流程定义对象                 |
| 流程实例发起 | 调用发起流程接口             | 返回流程实例 ID                  |
| 待办任务生成 | 查询待办任务                 | 当前节点生成待办                 |

建议先使用最简单的线性流程进行验证：

```text
开始节点 -> 部门负责人审批 -> 结束节点
```

测试变量示例：

```json
{
  "businessId": "LEAVE202605080001",
  "flowCode": "leave_process",
  "handler": "10001",
  "variable": {
    "days": 3,
    "deptId": "D001",
    "amount": 0,
    "initiator": "10001"
  }
}
```

如果流程中包含互斥网关，需要确保流程变量与网关条件表达式一致。Warm-Flow 官方文档说明流程变量为 `Map` 类型，可用于条件表达式判断、办理人表达式解析和监听器处理；网关条件可以结合流程变量决定流向。([Warm-Flow官网](https://www.warm-flow.com/v1.8.2/primary/variable.html?utm_source=chatgpt.com))

## 流程定义管理

流程定义管理用于维护流程模型、流程编码、流程版本和流程发布状态。业务系统不建议直接让所有业务模块操作 Warm-Flow 原生流程定义接口，而是通过统一的流程定义管理服务完成导入、查询、发布和版本控制。

### 流程模型设计

流程模型设计是工作流集成中的关键环节，直接决定后续流程实例流转、审批任务生成、条件分支判断和业务状态同步的复杂度。建议先从业务审批场景出发，明确流程节点、节点编码、办理人规则、跳转条件和结束状态。

流程模型设计建议如下：

| 设计项   | 建议                                                         |
| -------- | ------------------------------------------------------------ |
| 流程编码 | 使用稳定英文编码，例如 `leave_process`、`expense_process`    |
| 节点编码 | 使用语义化编码，例如 `start`、`dept_approve`、`finance_approve`、`end` |
| 节点名称 | 使用业务可读名称，例如“部门负责人审批”                       |
| 办理人   | 优先使用用户、角色、部门、岗位或表达式，不建议写死单个测试账号 |
| 条件分支 | 使用流程变量控制，例如 `amount`、`days`、`deptId`            |
| 结束节点 | 明确通过、驳回、终止等状态与业务状态的映射                   |
| 扩展属性 | 用于前端展示、抄送人、节点配置、业务标识等扩展场景           |

常见请假审批流程示例：

```text
开始节点
  -> 发起人提交
  -> 部门负责人审批
  -> 条件判断：请假天数是否大于 3 天
      -> 是：分管领导审批
      -> 否：结束
  -> 结束节点
```

流程变量建议统一命名，避免不同业务流程中使用不一致的字段名。

| 变量名       | 类型       | 说明                       |
| ------------ | ---------- | -------------------------- |
| businessId   | String     | 业务主键                   |
| businessType | String     | 业务类型                   |
| initiator    | String     | 发起人ID                   |
| deptId       | String     | 发起部门ID                 |
| amount       | BigDecimal | 金额类业务使用             |
| days         | Integer    | 请假、出差等天数类业务使用 |
| level        | String     | 业务等级                   |
| approved     | Boolean    | 是否审批通过               |

Warm-Flow 条件表达式支持默认表达式、SpEL 表达式以及大于、等于、小于、包含等内置表达式类型。设计条件分支时，应保证变量类型与表达式类型一致，尤其是数字、字符串和 Long 类型比较。([Warm-Flow官网](https://www.warm-flow.com/v1.8.0/primary/condition.html?utm_source=chatgpt.com))

条件表达式示例：

```text
# 请假天数大于 3 天
gt@@days|3

# 金额大于等于 5000
ge@@amount|5000

# 默认表达式
default@@${days > 3}

# SpEL 表达式
spel@@#{@workflowCondition.checkAmount(#amount)}
```

### 流程定义导入

流程定义导入用于将流程设计器生成的流程定义 JSON 导入 Warm-Flow 引擎表。Warm-Flow 官方接口文档中，`DefService` 提供了 `importIs`、`importJson`、`importDef`、`saveAndInitNode`、`exportJson`、`getAllDataDefinition` 等流程定义相关接口。([Warm-Flow官网](https://www.warm-flow.com/master/primary/api.html?utm_source=chatgpt.com))

流程定义导入方式建议分为三类：

| 导入方式        | 适用场景                                   |
| --------------- | ------------------------------------------ |
| 文件上传导入    | 后台管理页面上传流程定义 JSON              |
| JSON 字符串导入 | 初始化脚本、接口导入、自动化测试           |
| 启动时自动导入  | 开发环境快速初始化，不建议生产环境默认开启 |

文件导入接口示例：

```http
POST /workflow/definition/import
Content-Type: multipart/form-data

file=@leave_process.json
```

JSON 导入接口示例：

```http
POST /workflow/definition/import-json
Content-Type: application/json

{
  "flowCode": "leave_process",
  "definitionJson": "{...}"
}
```

流程定义 JSON 文件建议放在项目资源目录中，便于开发环境自动初始化。

```text
src/main/resources/
└── workflow/
    └── definition/
        ├── leave_process.json
        ├── expense_process.json
        └── purchase_process.json
```

开发环境可提供一个初始化组件，在配置开启时自动导入默认流程定义。生产环境建议通过后台管理页面或数据库脚本方式发布流程定义，避免每次启动重复覆盖正式流程。

文件位置：`src/main/java/io/github/atengk/workflow/runner/WarmFlowDefinitionRunner.java`

```java
package io.github.atengk.workflow.runner;

import cn.hutool.core.io.resource.ResourceUtil;
import cn.hutool.core.util.StrUtil;
import com.warm.flow.core.service.DefService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

/**
 * Warm-Flow 流程定义初始化器
 *
 * @author Ateng
 * @since 2026-05-08
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class WarmFlowDefinitionRunner implements CommandLineRunner {

    private final DefService defService;

    @Value("${workflow.auto-import-definition:false}")
    private Boolean autoImportDefinition;

    /**
     * 项目启动时按配置决定是否自动导入流程定义
     *
     * @param args 启动参数
     */
    @Override
    public void run(String... args) {
        if (!Boolean.TRUE.equals(autoImportDefinition)) {
            log.info("未开启流程定义自动导入");
            return;
        }

        String definitionJson = ResourceUtil.readUtf8Str("workflow/definition/leave_process.json");
        if (StrUtil.isBlank(definitionJson)) {
            log.warn("流程定义文件为空，跳过自动导入");
            return;
        }

        defService.importJson(definitionJson);
        log.info("流程定义自动导入完成，文件：workflow/definition/leave_process.json");
    }

}
```

### 流程版本管理

流程版本管理用于解决流程定义变更后的兼容问题。实际项目中，审批流程经常会因为组织架构、审批金额、节点规则和风控要求变化而调整。如果直接修改已经运行的流程定义，可能影响历史实例和正在审批中的实例，因此建议采用“新流程版本影响新实例，旧流程版本保留历史实例”的策略。

版本管理建议如下：

| 场景                   | 处理方式                       |
| ---------------------- | ------------------------------ |
| 流程未发布             | 允许编辑、删除、重新导入       |
| 流程已发布但未发起实例 | 可重新导入或覆盖               |
| 流程已发起实例         | 不建议直接覆盖，建议创建新版本 |
| 旧版本仍有运行中实例   | 保留旧版本，直到实例全部结束   |
| 新业务单据发起流程     | 使用最新启用版本               |
| 历史审批详情查看       | 按实例绑定的定义版本展示       |

业务侧可以维护一张流程定义版本表，用于管理业务流程编码与 Warm-Flow 流程定义之间的关系。

```sql
-- 业务流程定义版本表
CREATE TABLE IF NOT EXISTS wf_definition_version (
    id BIGINT NOT NULL COMMENT '主键ID',
    flow_code VARCHAR(64) NOT NULL COMMENT '流程编码',
    flow_name VARCHAR(128) NOT NULL COMMENT '流程名称',
    version_no INT NOT NULL COMMENT '版本号',
    definition_id BIGINT DEFAULT NULL COMMENT 'Warm-Flow 流程定义ID',
    status VARCHAR(32) NOT NULL COMMENT '状态：DRAFT、PUBLISHED、DISABLED',
    remark VARCHAR(500) DEFAULT NULL COMMENT '备注',
    publish_time DATETIME DEFAULT NULL COMMENT '发布时间',
    create_time DATETIME NOT NULL COMMENT '创建时间',
    update_time DATETIME DEFAULT NULL COMMENT '更新时间',
    PRIMARY KEY (id),
    UNIQUE KEY uk_flow_code_version (flow_code, version_no),
    KEY idx_definition_id (definition_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='业务流程定义版本表';
```

推荐的版本发布流程如下：

```text
设计流程模型
  -> 导出流程定义 JSON
  -> 导入测试环境
  -> 发起测试流程
  -> 验证审批流转
  -> 生成新版本号
  -> 发布流程版本
  -> 新业务单据使用新版本
```

版本号建议由业务侧控制，例如：

```text
leave_process:v1
leave_process:v2
expense_process:v1
purchase_process:v3
```

流程版本发布后，不建议直接删除旧流程定义。对于已存在流程实例的定义，删除或覆盖可能导致审批详情、流程图、历史记录无法正确追踪。

## 流程实例管理

流程实例管理用于处理业务单据与流程运行实例之间的关系。业务系统在发起审批时创建流程实例，在审批过程中查询实例状态，在撤销、终止或完成时同步业务状态。

### 发起流程

发起流程是业务系统与 Warm-Flow 集成的核心入口。Warm-Flow 官方接口文档说明，`InsService.start(businessId, flowParams)` 用于开启流程实例，其中 `flowParams` 中的 `flowCode` 为必传字段，`handler`、`variable`、`ext`、`nextHandler`、`flowStatus` 等字段可按业务需要传入。([Warm-Flow官网](https://www.warm-flow.com/master/primary/api.html?utm_source=chatgpt.com))

发起流程时建议完成以下操作：

| 步骤 | 说明                           |
| ---- | ------------------------------ |
| 1    | 校验业务单据是否存在           |
| 2    | 校验业务单据是否允许发起流程   |
| 3    | 查询业务流程编码和当前启用版本 |
| 4    | 组装流程变量                   |
| 5    | 调用 Warm-Flow 发起流程        |
| 6    | 保存业务单据与流程实例绑定关系 |
| 7    | 更新业务单据流程状态为审批中   |
| 8    | 记录流程发起日志               |

发起流程参数示例：

```json
{
  "businessId": "LEAVE202605080001",
  "businessType": "leave",
  "flowCode": "leave_process",
  "handler": "10001",
  "variable": {
    "days": 5,
    "deptId": "D001",
    "initiator": "10001",
    "leaderId": "10002"
  }
}
```

业务 Service 中发起流程示例：

```java
Map<String, Object> variable = MapUtil.<String, Object>builder()
        .put("businessId", leaveId)
        .put("businessType", "leave")
        .put("days", leaveDays)
        .put("deptId", deptId)
        .put("initiator", userId)
        .build();

Instance instance = warmFlowFacadeService.startProcess(
        leaveId,
        "leave_process",
        userId,
        variable
);
```

发起流程后，需要将流程实例 ID 回写到业务表或流程绑定表中。

```sql
-- 示例：更新业务流程绑定关系
UPDATE wf_business_flow
SET instance_id = ?,
    process_status = 'APPROVING',
    start_time = NOW(),
    update_time = NOW()
WHERE business_id = ?;
```

业务状态建议统一定义：

| 状态       | 说明   |
| ---------- | ------ |
| DRAFT      | 草稿   |
| APPROVING  | 审批中 |
| APPROVED   | 已通过 |
| REJECTED   | 已驳回 |
| REVOKED    | 已撤销 |
| TERMINATED | 已终止 |

### 查询流程实例

流程实例查询主要用于业务详情页、审批详情页、流程追踪页和流程图展示页。查询时建议以业务主键为入口，先查询业务流程绑定表，再根据流程实例 ID 查询 Warm-Flow 流程实例和待办任务。

查询链路如下：

```text
business_id
  -> wf_business_flow
  -> instance_id
  -> Warm-Flow 流程实例
  -> 当前节点、当前待办、历史任务、流程变量
```

查询结果建议返回给前端以下数据：

| 字段            | 说明         |
| --------------- | ------------ |
| businessId      | 业务主键     |
| businessCode    | 业务编号     |
| flowCode        | 流程编码     |
| instanceId      | 流程实例 ID  |
| processStatus   | 业务流程状态 |
| currentNodeCode | 当前节点编码 |
| currentNodeName | 当前节点名称 |
| currentHandlers | 当前办理人   |
| startUserId     | 发起人       |
| startTime       | 发起时间     |
| finishTime      | 完成时间     |
| approvalRecords | 审批记录列表 |

流程实例查询返回示例：

```json
{
  "businessId": "LEAVE202605080001",
  "businessCode": "QJ202605080001",
  "flowCode": "leave_process",
  "instanceId": 1260200765054652416,
  "processStatus": "APPROVING",
  "currentNodeCode": "dept_approve",
  "currentNodeName": "部门负责人审批",
  "currentHandlers": [
    {
      "userId": "10002",
      "userName": "部门负责人"
    }
  ],
  "startUserId": "10001",
  "startTime": "2026-05-08 10:00:00",
  "finishTime": null
}
```

对于业务列表页，不建议每行实时查询大量 Warm-Flow 明细数据。可以将常用字段冗余到业务表或流程绑定表中，例如流程状态、实例 ID、当前节点名称、当前办理人摘要等，以提升列表查询性能。

### 撤销与终止流程

撤销和终止是流程实例管理中的高风险操作，需要结合业务规则、权限规则和流程状态严格控制。Warm-Flow 官方接口文档中，`TaskService.revoke(instanceId, flowParams)` 用于撤销流程，`TaskService.termination(taskId, flowParams)` 用于终止流程；`skipByInsId` 和 `skip` 的区别在于前者通过实例 ID 流转，后者通过任务 ID 流转，当一个流程实例存在多个当前任务时，使用实例 ID 可能无法明确要处理哪一个任务，因此待办审批场景更推荐使用任务 ID。([Warm-Flow官网](https://www.warm-flow.com/master/primary/api.html?utm_source=chatgpt.com))

撤销流程适用于发起人主动撤回流程，例如单据填写错误、业务取消或需要重新编辑。撤销通常要求流程未结束，并且当前业务状态仍为审批中。

撤销流程业务规则建议如下：

| 规则                       | 说明                                       |
| -------------------------- | ------------------------------------------ |
| 只能发起人撤销             | 普通用户只能撤销自己发起的流程             |
| 已完成流程不可撤销         | 已通过、已终止、已撤销的流程不允许重复撤销 |
| 撤销需要填写原因           | 便于审批记录和审计追踪                     |
| 撤销后业务回到草稿或已撤销 | 根据业务是否允许重新提交决定               |
| 撤销操作需要落库           | 记录操作人、时间、原因和实例 ID            |

撤销接口请求示例：

```json
{
  "instanceId": 1260200765054652416,
  "handler": "10001",
  "message": "请假时间填写错误，撤回后重新提交"
}
```

撤销后的业务处理建议：

```text
调用 taskService.revoke
  -> 更新业务流程状态为 REVOKED 或 DRAFT
  -> 写入 wf_approval_record
  -> 发送撤销通知
  -> 返回最新流程状态
```

终止流程适用于管理员、流程负责人或当前审批人在特殊情况下结束流程，例如业务已取消、数据异常、审批不再需要继续。终止通常比撤销权限更高，不建议对普通发起人开放。

终止流程业务规则建议如下：

| 规则                 | 说明                                         |
| -------------------- | -------------------------------------------- |
| 限制操作角色         | 通常只允许管理员、流程管理员或当前审批人终止 |
| 必须填写终止原因     | 便于审计                                     |
| 已结束流程不可终止   | 避免重复操作                                 |
| 终止后不可继续审批   | 业务状态应变为已终止                         |
| 终止操作需要记录日志 | 包含操作人、任务 ID、实例 ID、原因           |

终止流程请求示例：

```json
{
  "taskId": 1260200765054652416,
  "handler": "10002",
  "message": "业务单据已作废，终止审批流程"
}
```

终止后的业务处理建议：

```text
调用 taskService.termination
  -> 更新业务流程状态为 TERMINATED
  -> 更新业务单据状态
  -> 写入审批记录
  -> 清理或关闭业务待办提醒
  -> 发送终止通知
```

撤销与终止的区别如下：

| 操作 | 发起人               | 典型场景                         | 结果                           |
| ---- | -------------------- | -------------------------------- | ------------------------------ |
| 撤销 | 通常是流程发起人     | 单据填错、需要重新提交           | 流程退回或撤回，业务可回到草稿 |
| 终止 | 通常是管理员或审批人 | 业务取消、异常关闭、无需继续审批 | 流程直接结束，业务状态为已终止 |

在接口实现上，撤销和终止都必须放在事务中处理。流程状态更新、业务状态更新、审批记录落库、消息通知记录应保持一致。如果消息通知依赖外部 MQ，建议先提交数据库事务，再通过可靠事件表或消息补偿机制异步发送通知。



## 任务审批管理

任务审批管理用于处理流程运行过程中的待办任务、已办任务和审批动作。业务系统通常不会直接把 Warm-Flow 原始任务对象暴露给前端，而是封装成统一的审批任务接口，返回业务标题、业务编号、当前节点、发起人、发起时间、审批状态和可执行操作。

Warm-Flow 的 `TaskService` 提供流程通过、退回、任意跳转、撤销、终止、转办、委派、加签、减签、修改办理人等能力；其中 `skip(taskId, flowParams)` 可通过 `skipType` 控制通过或退回，`transfer(taskId, flowParams)` 用于转办，`depute(taskId, flowParams)` 用于委派。([Warm-Flow官网](https://www.warm-flow.com/master/primary/api.html?utm_source=chatgpt.com))

### 待办任务查询

待办任务查询用于展示当前登录用户需要处理的审批任务。查询时不能只按用户 ID 查询，还需要结合用户拥有的角色、部门、岗位等权限标识，因为流程节点办理人可能配置为用户、角色、部门或表达式。

待办查询建议使用以下查询条件：

| 查询条件    | 说明                                   |
| ----------- | -------------------------------------- |
| 当前用户 ID | 用于查询直接分配给当前用户的任务       |
| 角色标识    | 例如 `role:admin`、`role:dept_manager` |
| 部门标识    | 例如 `dept:1001`                       |
| 岗位标识    | 例如 `post:leader`                     |
| 业务类型    | 按请假、报销、采购等业务类型过滤       |
| 流程编码    | 按流程定义过滤                         |
| 当前节点    | 按审批节点过滤                         |
| 发起人      | 查询某个用户发起的审批                 |
| 时间范围    | 按任务创建时间或到达时间过滤           |

待办任务返回字段建议如下：

| 字段          | 说明               |
| ------------- | ------------------ |
| taskId        | 任务 ID            |
| instanceId    | 流程实例 ID        |
| businessId    | 业务主键           |
| businessCode  | 业务编号           |
| businessTitle | 业务标题           |
| businessType  | 业务类型           |
| flowCode      | 流程编码           |
| nodeCode      | 当前节点编码       |
| nodeName      | 当前节点名称       |
| startUserId   | 发起人 ID          |
| startUserName | 发起人名称         |
| createTime    | 任务创建时间       |
| processStatus | 流程状态           |
| actions       | 当前用户可执行动作 |

接口设计示例：

```http
GET /workflow/tasks/todo?pageNum=1&pageSize=10&businessType=leave&keyword=请假
```

返回示例：

```json
{
  "code": 200,
  "message": "操作成功",
  "data": {
    "total": 1,
    "records": [
      {
        "taskId": 1260200765054652416,
        "instanceId": 1260200765054652417,
        "businessId": "LEAVE202605080001",
        "businessCode": "QJ202605080001",
        "businessTitle": "张三请假申请",
        "businessType": "leave",
        "flowCode": "leave_process",
        "nodeCode": "dept_approve",
        "nodeName": "部门负责人审批",
        "startUserId": "10001",
        "startUserName": "张三",
        "createTime": "2026-05-08 10:00:00",
        "processStatus": "APPROVING",
        "actions": ["PASS", "REJECT", "TRANSFER", "DEPUTE"]
      }
    ]
  }
}
```

待办查询实现时，建议先获取当前登录用户的权限标识集合，再查询 Warm-Flow 当前任务和业务扩展表。Warm-Flow 的办理人权限处理器中也有类似概念：`permissions()` 用于返回当前用户的权限集合，`getHandler()` 用于返回当前办理人的唯一标识。([Warm-Flow官网](https://www.warm-flow.com/master/primary/permission_handler.html?utm_source=chatgpt.com))

待办查询逻辑示例：

```text
获取当前登录用户
  -> 查询用户角色、部门、岗位
  -> 组装 permissionFlag 集合
  -> 查询当前用户可办理任务
  -> 关联业务流程绑定表
  -> 关联业务单据表
  -> 组装待办任务列表
```

待办查询注意事项：

1. 列表页不建议返回完整流程变量，避免响应数据过大。
2. 当前节点办理人如果是角色或部门，需要在权限判断时转换为用户可识别的权限标识。
3. 并行任务、会签任务可能存在同一个流程实例下多个待办任务，前端应以 `taskId` 作为审批操作主键。
4. 审批接口应再次校验任务权限，不能只依赖待办列表是否展示。

### 已办任务查询

已办任务查询用于展示当前用户已经处理过的审批任务。已办数据通常来自 Warm-Flow 历史任务数据和业务审批记录表。为了便于业务页面展示，建议业务系统单独维护审批记录表，例如前文中的 `wf_approval_record`。

已办查询建议返回以下字段：

| 字段          | 说明         |
| ------------- | ------------ |
| recordId      | 审批记录 ID  |
| taskId        | 任务 ID      |
| instanceId    | 流程实例 ID  |
| businessId    | 业务主键     |
| businessCode  | 业务编号     |
| businessTitle | 业务标题     |
| nodeCode      | 审批节点编码 |
| nodeName      | 审批节点名称 |
| actionType    | 审批动作     |
| message       | 审批意见     |
| operateTime   | 操作时间     |
| processStatus | 当前流程状态 |

接口设计示例：

```http
GET /workflow/tasks/done?pageNum=1&pageSize=10&businessType=expense&startTime=2026-05-01&endTime=2026-05-08
```

返回示例：

```json
{
  "code": 200,
  "message": "操作成功",
  "data": {
    "total": 1,
    "records": [
      {
        "recordId": 1260200765054652500,
        "taskId": 1260200765054652416,
        "instanceId": 1260200765054652417,
        "businessId": "EXP202605080001",
        "businessCode": "BX202605080001",
        "businessTitle": "差旅报销申请",
        "nodeCode": "finance_approve",
        "nodeName": "财务审批",
        "actionType": "PASS",
        "message": "审批通过",
        "operateTime": "2026-05-08 11:20:00",
        "processStatus": "APPROVING"
      }
    ]
  }
}
```

已办查询建议以业务审批记录表为主，Warm-Flow 历史任务表为辅。业务审批记录表更适合前端展示，因为它可以冗余业务标题、业务编号、审批意见、操作人名称和业务状态，避免每次查询都进行多表关联。

### 审批通过

审批通过用于当前办理人同意当前任务，并推动流程进入下一个节点或结束节点。Warm-Flow 提供 `pass` 方法，也可以通过 `skip(taskId, flowParams)` 设置 `skipType` 为通过类型完成流转；`skip` 的 `flowParams` 支持审批意见、办理人、权限标识、流程变量、下个任务办理人、自定义流程状态和是否忽略权限校验等参数。([Warm-Flow官网](https://www.warm-flow.com/master/primary/api.html?utm_source=chatgpt.com))

审批通过请求参数建议如下：

| 参数              | 必填 | 说明                       |
| ----------------- | ---- | -------------------------- |
| taskId            | 是   | 当前任务 ID                |
| message           | 否   | 审批意见                   |
| variable          | 否   | 审批时追加或覆盖的流程变量 |
| nextHandler       | 否   | 指定下一节点办理人         |
| nextHandlerAppend | 否   | 是否追加下一节点办理人     |
| businessStatus    | 否   | 业务侧状态                 |

接口设计示例：

```http
POST /workflow/tasks/pass
Content-Type: application/json
{
  "taskId": 1260200765054652416,
  "message": "同意",
  "variable": {
    "approved": true,
    "amount": 3000
  },
  "nextHandler": ["10003"],
  "nextHandlerAppend": false
}
```

审批通过处理流程：

```text
校验任务是否存在
  -> 校验当前用户是否有办理权限
  -> 校验业务单据是否处于审批中
  -> 调用 Warm-Flow 审批通过接口
  -> 写入业务审批记录
  -> 更新业务流程绑定表
  -> 如果流程结束，更新业务单据状态为已通过
  -> 返回最新流程状态
```

业务侧封装示例：

文件位置：`src/main/java/io/github/atengk/workflow/service/WarmFlowTaskService.java`

下面代码封装审批通过、驳回、转办和委派等核心任务操作，供 Controller 或业务 Service 调用。

```java
package io.github.atengk.workflow.service;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.map.MapUtil;
import cn.hutool.core.util.StrUtil;
import com.warm.flow.core.dto.FlowParams;
import com.warm.flow.core.entity.Instance;
import com.warm.flow.core.enums.SkipType;
import com.warm.flow.core.service.TaskService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;

/**
 * Warm-Flow 任务审批服务
 *
 * @author Ateng
 * @since 2026-05-08
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class WarmFlowTaskService {

    private final TaskService taskService;

    /**
     * 审批通过
     *
     * @param taskId 任务ID
     * @param handler 当前办理人
     * @param permissionFlag 权限标识集合
     * @param message 审批意见
     * @param variable 流程变量
     * @return 流程实例
     */
    public Instance pass(Long taskId,
                         String handler,
                         List<String> permissionFlag,
                         String message,
                         Map<String, Object> variable) {
        checkTaskAndHandler(taskId, handler);

        FlowParams flowParams = FlowParams.build()
                .skipType(SkipType.PASS.getKey())
                .handler(handler)
                .permissionFlag(permissionFlag)
                .message(StrUtil.blankToDefault(message, "审批通过"));

        if (MapUtil.isNotEmpty(variable)) {
            flowParams.variable(variable);
        }

        Instance instance = taskService.skip(taskId, flowParams);
        log.info("审批通过成功，任务ID：{}，办理人：{}，实例ID：{}", taskId, handler, instance.getId());
        return instance;
    }

    /**
     * 驳回到指定节点
     *
     * @param taskId 任务ID
     * @param nodeCode 目标节点编码
     * @param handler 当前办理人
     * @param permissionFlag 权限标识集合
     * @param message 驳回原因
     * @param variable 流程变量
     * @return 流程实例
     */
    public Instance reject(Long taskId,
                           String nodeCode,
                           String handler,
                           List<String> permissionFlag,
                           String message,
                           Map<String, Object> variable) {
        checkTaskAndHandler(taskId, handler);
        if (StrUtil.isBlank(nodeCode)) {
            throw new IllegalArgumentException("驳回目标节点不能为空");
        }

        FlowParams flowParams = FlowParams.build()
                .skipType(SkipType.REJECT.getKey())
                .nodeCode(nodeCode)
                .handler(handler)
                .permissionFlag(permissionFlag)
                .message(StrUtil.blankToDefault(message, "驳回"));

        if (MapUtil.isNotEmpty(variable)) {
            flowParams.variable(variable);
        }

        Instance instance = taskService.skip(taskId, flowParams);
        log.info("流程驳回成功，任务ID：{}，目标节点：{}，办理人：{}", taskId, nodeCode, handler);
        return instance;
    }

    /**
     * 转办任务
     *
     * @param taskId 任务ID
     * @param handler 当前办理人
     * @param permissionFlag 权限标识集合
     * @param addHandlers 转办对象
     * @param message 转办意见
     */
    public void transfer(Long taskId,
                         String handler,
                         List<String> permissionFlag,
                         List<String> addHandlers,
                         String message) {
        checkTaskAndHandler(taskId, handler);
        if (CollUtil.isEmpty(addHandlers)) {
            throw new IllegalArgumentException("转办对象不能为空");
        }

        FlowParams flowParams = FlowParams.build()
                .handler(handler)
                .permissionFlag(permissionFlag)
                .addHandlers(addHandlers)
                .message(StrUtil.blankToDefault(message, "转办"));

        boolean result = taskService.transfer(taskId, flowParams);
        log.info("任务转办完成，任务ID：{}，当前办理人：{}，转办对象：{}，结果：{}", taskId, handler, addHandlers, result);
    }

    /**
     * 委派任务
     *
     * @param taskId 任务ID
     * @param handler 当前办理人
     * @param permissionFlag 权限标识集合
     * @param addHandlers 委派对象
     * @param message 委派意见
     */
    public void depute(Long taskId,
                       String handler,
                       List<String> permissionFlag,
                       List<String> addHandlers,
                       String message) {
        checkTaskAndHandler(taskId, handler);
        if (CollUtil.isEmpty(addHandlers)) {
            throw new IllegalArgumentException("委派对象不能为空");
        }

        FlowParams flowParams = FlowParams.build()
                .handler(handler)
                .permissionFlag(permissionFlag)
                .addHandlers(addHandlers)
                .message(StrUtil.blankToDefault(message, "委派"));

        boolean result = taskService.depute(taskId, flowParams);
        log.info("任务委派完成，任务ID：{}，当前办理人：{}，委派对象：{}，结果：{}", taskId, handler, addHandlers, result);
    }

    /**
     * 校验任务和办理人
     *
     * @param taskId 任务ID
     * @param handler 办理人
     */
    private void checkTaskAndHandler(Long taskId, String handler) {
        if (taskId == null) {
            throw new IllegalArgumentException("任务ID不能为空");
        }
        if (StrUtil.isBlank(handler)) {
            throw new IllegalArgumentException("当前办理人不能为空");
        }
    }

}
```

审批通过完成后，需要根据返回的流程实例状态同步业务状态。如果流程已经结束，业务单据状态应更新为 `APPROVED`；如果流程仍在流转，业务状态继续保持 `APPROVING`。

### 驳回与退回

驳回与退回都属于不同形式的反向流转，但在业务语义上建议区分清楚。驳回通常表示审批不同意，可能直接回到发起人或指定节点；退回通常表示材料不完整、需要补充，可以回到上一个节点或最近办理节点。Warm-Flow 提供 `reject`、`rejectAtWill`、`rejectLast`、`rejectLastByInsId` 等能力，支持退回到指定节点或上一个任务。([Warm-Flow官网](https://www.warm-flow.com/master/primary/api.html?utm_source=chatgpt.com))

建议业务系统定义以下动作：

| 动作            | 含义         | 推荐使用场景                     |
| --------------- | ------------ | -------------------------------- |
| REJECT          | 驳回         | 审批不同意，退回发起人或指定节点 |
| RETURN          | 退回         | 材料补充，退回上一个审批节点     |
| REJECT_LAST     | 驳回上一步   | 固定回到上一个办理节点           |
| REJECT_TO_START | 驳回发起人   | 重新修改后再次提交               |
| REJECT_TO_NODE  | 驳回指定节点 | 管理员或复杂审批场景使用         |

驳回请求示例：

```json
{
  "taskId": 1260200765054652416,
  "nodeCode": "start",
  "message": "申请材料不完整，请补充附件",
  "variable": {
    "approved": false,
    "rejectReason": "申请材料不完整"
  }
}
```

退回上一步请求示例：

```json
{
  "taskId": 1260200765054652416,
  "message": "请上一步补充说明"
}
```

驳回处理流程：

```text
校验任务是否存在
  -> 校验当前用户是否有办理权限
  -> 校验目标节点是否合法
  -> 调用 Warm-Flow 驳回或退回接口
  -> 写入审批记录
  -> 更新当前节点和当前办理人摘要
  -> 根据业务规则更新业务状态
```

驳回到指定节点时，应避免选择后置节点。Warm-Flow 官方接口文档也明确提示，任意退回到指定节点时不应选择后置节点。([Warm-Flow官网](https://www.warm-flow.com/master/primary/api.html?utm_source=chatgpt.com))

业务状态建议如下：

| 场景                     | 业务状态          |
| ------------------------ | ----------------- |
| 驳回发起人并允许重新提交 | REJECTED 或 DRAFT |
| 驳回上一步继续审批       | APPROVING         |
| 审批明确不同意且流程结束 | REJECTED          |
| 管理员终止流程           | TERMINATED        |

### 转办与委派

转办和委派都属于当前任务办理人变更，但业务语义不同。转办表示当前办理人不再处理该任务，任务交给其他人继续办理；委派表示当前办理人请其他人协助处理，委派人处理完成后，任务仍回到原办理人。Warm-Flow 官方文档说明，转办和委派会删除当前办理人权限；如果节点配置的是角色，可能无法删除当前办理人，此时建议通过办理人权限处理器将角色或部门转换为具体用户 ID。([Warm-Flow官网](https://www.warm-flow.com/master/primary/api.html?utm_source=chatgpt.com))

转办与委派区别如下：

| 操作 | 当前办理人是否继续办理 | 任务是否回到原办理人 | 适用场景                               |
| ---- | ---------------------- | -------------------- | -------------------------------------- |
| 转办 | 否                     | 否                   | 当前审批人不负责该事项，转给其他人处理 |
| 委派 | 暂不处理               | 是                   | 当前审批人需要他人协助给出意见         |
| 加签 | 继续参与               | 视流程配置而定       | 需要增加其他审批人共同参与             |
| 减签 | 减少办理人             | 否                   | 删除不需要参与的办理人                 |

转办请求示例：

```json
{
  "taskId": 1260200765054652416,
  "addHandlers": ["10005"],
  "message": "该事项由李四负责，转交处理"
}
```

委派请求示例：

```json
{
  "taskId": 1260200765054652416,
  "addHandlers": ["10006"],
  "message": "请协助确认费用明细"
}
```

转办处理流程：

```text
校验当前用户是否为任务办理人
  -> 校验转办对象是否有效
  -> 校验不能转办给自己
  -> 调用 taskService.transfer
  -> 写入审批记录
  -> 发送待办通知给新办理人
```

委派处理流程：

```text
校验当前用户是否为任务办理人
  -> 校验委派对象是否有效
  -> 调用 taskService.depute
  -> 写入审批记录
  -> 发送委派通知
  -> 委派人处理后任务回到原办理人
```

转办和委派接口必须进行用户有效性校验。建议校验目标用户是否在职、是否属于当前租户、是否具备对应业务权限，并避免把任务转办或委派给已禁用用户。

## 办理人与权限扩展

办理人与权限扩展用于解决“谁可以审批”和“如何识别审批权限”的问题。流程设计器中可以配置用户、角色、部门、岗位或表达式，后端则需要将这些配置与业务系统的用户中心、角色体系和数据权限体系打通。

Warm-Flow 的 `PermissionHandler` 用于统一获取当前办理人的权限标识和办理人唯一标识；其中 `permissions()` 返回当前用户权限集合，`getHandler()` 返回当前办理人 ID，`convertPermissions()` 可将角色、部门等权限标识转换成具体用户。([Warm-Flow官网](https://www.warm-flow.com/master/primary/permission_handler.html?utm_source=chatgpt.com))

### 办理人表达式配置

办理人表达式用于动态指定流程节点的办理人。适用于审批人不能在流程设计阶段固定下来的场景，例如部门负责人、项目负责人、金额对应审批人、表单中选择的下一审批人等。

常见办理人配置方式如下：

| 配置方式    | 示例                                  | 说明                            |
| ----------- | ------------------------------------- | ------------------------------- |
| 固定用户    | `10001`                               | 固定由某个用户审批              |
| 角色        | `role:dept_manager`                   | 当前角色下的用户可审批          |
| 部门        | `dept:1001`                           | 当前部门下的用户可审批          |
| 岗位        | `post:leader`                         | 某个岗位下的用户可审批          |
| 变量表达式  | `${leaderId}`                         | 从流程变量中获取办理人          |
| SpEL 表达式 | `#{@userResolver.getLeader(#deptId)}` | 调用 Spring Bean 动态解析办理人 |

Warm-Flow 办理人变更文档说明，动态指定办理人时，可以在流程设计中配置类似 `${handler1}` 的办理人表达式，并在前置节点办理时通过流程变量传入 `handler1` 的值；该值可以是单个用户，也可以是一组用户。([Warm-Flow官网](https://www.warm-flow.com/master/advanced/change_of_handler.html?utm_source=chatgpt.com))

流程变量示例：

```json
{
  "leaderId": "10002",
  "financeHandlers": ["10003", "10004"],
  "deptId": "D001",
  "amount": 8000
}
```

审批通过时动态指定下一节点办理人：

```java
Map<String, Object> variable = MapUtil.<String, Object>builder()
        .put("leaderId", "10002")
        .put("financeHandlers", ListUtil.of("10003", "10004"))
        .put("amount", 8000)
        .build();

FlowParams flowParams = FlowParams.build()
        .skipType(SkipType.PASS.getKey())
        .handler("10001")
        .message("提交部门负责人审批")
        .variable(variable);
```

使用办理人表达式时建议遵守以下规则：

1. 表达式变量名统一，例如 `leaderId`、`managerId`、`financeHandlers`。
2. 办理人表达式只负责解析办理人，不建议在表达式中写复杂业务逻辑。
3. 复杂规则应封装到 Spring Bean 中，再通过 SpEL 调用。
4. 变量值尽量使用用户 ID 列表，减少角色和部门在任务流转中的二次解析成本。
5. 金额、部门、岗位等条件应在流程变量中明确传递，避免监听器重复查询业务表。

### 用户与角色适配

用户与角色适配用于将业务系统中的用户、角色、部门、岗位转换为 Warm-Flow 可识别的办理人权限标识。建议统一使用带前缀的权限标识，避免用户 ID、角色 ID、部门 ID 之间出现值冲突。

权限标识建议如下：

| 类型 | 格式                | 示例                |
| ---- | ------------------- | ------------------- |
| 用户 | `user:{userId}`     | `user:10001`        |
| 角色 | `role:{roleCode}`   | `role:dept_manager` |
| 部门 | `dept:{deptId}`     | `dept:1001`         |
| 岗位 | `post:{postCode}`   | `post:leader`       |
| 租户 | `tenant:{tenantId}` | `tenant:1`          |

如果流程节点最终需要精确到用户，建议在 `convertPermissions()` 中将角色、部门、岗位转换为具体用户 ID。Warm-Flow 官方文档也说明，转办和委派场景下如果节点配置为角色，可能无法删除当前办理人，因此需要把角色全部转换为用户 ID。([Warm-Flow官网](https://www.warm-flow.com/master/advanced/change_of_handler.html?utm_source=chatgpt.com))

文件位置：`src/main/java/io/github/atengk/workflow/handler/CustomPermissionHandler.java`

下面代码用于适配当前登录用户的办理人 ID、权限标识和角色/部门到用户的转换逻辑。示例中的 `LoginUserContext`、`UserPermissionService` 需要替换为项目实际的登录用户上下文和用户权限服务。

```java
package io.github.atengk.workflow.handler;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.StrUtil;
import com.warm.flow.core.handler.PermissionHandler;
import io.github.atengk.system.context.LoginUserContext;
import io.github.atengk.system.model.LoginUser;
import io.github.atengk.system.service.UserPermissionService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

/**
 * Warm-Flow 办理人权限处理器
 *
 * @author Ateng
 * @since 2026-05-08
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class CustomPermissionHandler implements PermissionHandler {

    private final LoginUserContext loginUserContext;
    private final UserPermissionService userPermissionService;

    /**
     * 获取当前用户权限标识集合
     *
     * @return 权限标识集合
     */
    @Override
    public List<String> permissions() {
        LoginUser loginUser = loginUserContext.getLoginUser();
        if (loginUser == null || StrUtil.isBlank(loginUser.getUserId())) {
            log.warn("获取流程权限标识失败，当前登录用户为空");
            return List.of();
        }

        List<String> permissions = new ArrayList<>();

        // 用户标识
        permissions.add("user:" + loginUser.getUserId());

        // 角色标识
        if (CollUtil.isNotEmpty(loginUser.getRoleCodes())) {
            loginUser.getRoleCodes().forEach(roleCode -> permissions.add("role:" + roleCode));
        }

        // 部门标识
        if (StrUtil.isNotBlank(loginUser.getDeptId())) {
            permissions.add("dept:" + loginUser.getDeptId());
        }

        // 岗位标识
        if (CollUtil.isNotEmpty(loginUser.getPostCodes())) {
            loginUser.getPostCodes().forEach(postCode -> permissions.add("post:" + postCode));
        }

        log.debug("获取流程权限标识成功，用户ID：{}，权限数量：{}", loginUser.getUserId(), permissions.size());
        return permissions;
    }

    /**
     * 获取当前办理人唯一标识
     *
     * @return 当前办理人ID
     */
    @Override
    public String getHandler() {
        LoginUser loginUser = loginUserContext.getLoginUser();
        if (loginUser == null) {
            log.warn("获取当前流程办理人失败，登录用户为空");
            return null;
        }
        return loginUser.getUserId();
    }

    /**
     * 将角色、部门、岗位等权限标识转换为具体用户ID
     *
     * @param permissions 权限标识集合
     * @return 用户ID集合
     */
    @Override
    public List<String> convertPermissions(List<String> permissions) {
        if (CollUtil.isEmpty(permissions)) {
            return List.of();
        }

        List<String> userIds = userPermissionService.convertToUserIds(permissions);
        log.debug("流程办理人权限转换完成，原始权限：{}，用户数量：{}", permissions, userIds.size());
        return userIds;
    }

}
```

业务系统中建议封装一个用户权限服务，专门处理角色、部门、岗位到用户的转换。

```text
role:dept_manager
  -> 查询拥有该角色的用户
dept:1001
  -> 查询该部门负责人或部门成员
post:leader
  -> 查询拥有该岗位的用户
user:10001
  -> 直接返回用户 10001
```

转换时需要注意以下问题：

1. 禁用用户不能作为办理人。
2. 离职用户不能作为办理人。
3. 跨租户用户不能作为办理人。
4. 部门标识转换为用户时，要明确是部门负责人还是部门成员。
5. 角色下存在大量用户时，应结合业务规则缩小范围。
6. 转换结果为空时，应抛出明确异常，避免流程进入无人办理状态。

### 数据权限控制

数据权限控制用于限制用户可以查看、审批和管理哪些流程数据。工作流系统中至少存在三类权限：任务办理权限、流程查看权限和流程管理权限。

权限类型建议如下：

| 权限类型     | 说明                                 | 示例                                           |
| ------------ | ------------------------------------ | ---------------------------------------------- |
| 办理权限     | 用户是否可以处理当前任务             | 当前用户是任务办理人、角色办理人、委派人       |
| 查看权限     | 用户是否可以查看流程详情             | 发起人、当前审批人、历史审批人、抄送人、管理员 |
| 管理权限     | 用户是否可以终止、干预、重新分配任务 | 流程管理员、系统管理员                         |
| 数据范围权限 | 用户能查看哪些组织范围内的流程       | 本人、本部门、本部门及下级、全部               |

数据权限建议按接口类型分别控制：

| 接口类型     | 控制方式                                         |
| ------------ | ------------------------------------------------ |
| 待办任务     | 按当前用户权限标识查询                           |
| 已办任务     | 按操作人 ID 查询                                 |
| 我的发起     | 按发起人 ID 查询                                 |
| 流程详情     | 发起人、办理人、历史办理人、抄送人或管理员可查看 |
| 审批动作     | 必须校验当前用户是否有任务办理权限               |
| 流程终止     | 仅管理员、流程管理员或业务负责人可操作           |
| 流程定义管理 | 仅流程管理员可操作                               |

审批接口不能只依赖前端按钮控制，后端必须进行二次权限校验。建议审批接口处理顺序如下：

```text
获取当前登录用户
  -> 获取用户权限标识集合
  -> 查询任务是否存在
  -> 校验任务是否处于可办理状态
  -> 校验当前用户是否具备办理权限
  -> 校验业务单据是否允许审批
  -> 执行 Warm-Flow 审批动作
  -> 写入审批记录和操作日志
```

数据权限控制可以通过以下方式实现：

| 实现方式                   | 说明                                     |
| -------------------------- | ---------------------------------------- |
| Service 层显式校验         | 审批、撤销、终止等核心动作必须显式校验   |
| MyBatis-Plus 数据权限插件  | 适合列表查询自动追加部门、租户等过滤条件 |
| AOP 权限切面               | 适合统一校验流程查看、流程管理权限       |
| Sa-Token / Spring Security | 适合接口级鉴权和角色权限判断             |
| 业务状态机                 | 防止非法状态下执行审批动作               |

流程数据权限建议与业务数据权限保持一致。例如用户不能查看某个采购单，就不应通过流程详情接口查看该采购单的审批记录；用户没有某个报销单的审批权限，也不能通过直接调用审批接口完成审批。

审批接口最终需要同时满足以下条件：

```text
用户已登录
  AND 用户具备接口权限
  AND 用户具备任务办理权限
  AND 用户具备业务数据权限
  AND 业务单据处于可审批状态
  AND 流程任务处于可办理状态
```

对于管理员操作，可以允许 `ignore=true` 忽略 Warm-Flow 的办理权限校验，但业务系统仍应记录管理员操作日志，并限制只有流程管理员或系统管理员可以使用该能力。Warm-Flow 的 `FlowParams` 支持 `ignore` 参数用于忽略权限校验，通常适合管理员干预类场景。([Warm-Flow官网](https://www.warm-flow.com/master/primary/api.html?utm_source=chatgpt.com))



## 条件表达式与流程变量

条件表达式与流程变量用于控制流程运行过程中的动态行为。流程变量通常在发起流程或审批流转时传入，后续可用于条件分支判断、办理人动态解析和监听器业务处理。Warm-Flow 官方文档说明，流程变量是 `Map` 类型，可在流程执行中传递数据，条件表达式、办理人表达式和监听器都可以使用流程变量。([Warm-Flow官网](https://www.warm-flow.com/v1.8.2/primary/variable.html?utm_source=chatgpt.com))

### 流程变量传递

流程变量应只保存流程判断和节点处理所需的关键数据，不建议把完整业务对象、大字段内容或敏感数据全部塞入流程变量。业务详情应通过 `businessId` 回查业务表，流程变量只负责保存分支判断、办理人解析和监听器处理所需的轻量字段。

常用流程变量建议如下：

| 变量名          | 类型                | 说明                                          |
| --------------- | ------------------- | --------------------------------------------- |
| businessId      | String              | 业务主键                                      |
| businessType    | String              | 业务类型，例如 `leave`、`expense`、`purchase` |
| initiator       | String              | 发起人 ID                                     |
| deptId          | String              | 发起人部门 ID                                 |
| amount          | BigDecimal / String | 金额类业务变量                                |
| days            | Integer / String    | 请假、出差等天数变量                          |
| leaderId        | String              | 部门负责人 ID                                 |
| financeHandlers | List                | 财务审批人集合                                |
| approved        | Boolean             | 审批结果                                      |
| rejectReason    | String              | 驳回原因                                      |

发起流程时传递变量示例：

```java
Map<String, Object> variable = MapUtil.<String, Object>builder()
        .put("businessId", leaveId)
        .put("businessType", "leave")
        .put("initiator", currentUserId)
        .put("deptId", currentDeptId)
        .put("days", leaveDays)
        .put("leaderId", leaderUserId)
        .build();

FlowParams flowParams = FlowParams.build()
        .flowCode("leave_process")
        .handler(currentUserId)
        .variable(variable);

Instance instance = insService.start(leaveId, flowParams);
```

审批流转时也可以继续传递或更新流程变量，例如审批通过时追加审批结果、金额确认结果或下一节点办理人。

```java
Map<String, Object> variable = MapUtil.<String, Object>builder()
        .put("approved", true)
        .put("approveUserId", currentUserId)
        .put("approveTime", DateUtil.now())
        .put("financeHandlers", ListUtil.of("10003", "10004"))
        .build();

FlowParams flowParams = FlowParams.build()
        .skipType(SkipType.PASS.getKey())
        .handler(currentUserId)
        .message("审批通过")
        .variable(variable);

Instance instance = taskService.skip(taskId, flowParams);
```

建议在项目中封装统一的变量构建工具，避免不同业务模块重复拼接变量名。变量名一旦被流程定义、条件表达式或监听器引用，就不应随意修改。

文件位置：`src/main/java/io/github/atengk/workflow/support/WorkflowVariableBuilder.java`

下面工具类用于统一构建流程变量，避免各业务模块散落硬编码变量名。

```java
package io.github.atengk.workflow.support;

import cn.hutool.core.map.MapUtil;
import cn.hutool.core.util.StrUtil;

import java.math.BigDecimal;
import java.util.Map;

/**
 * 工作流变量构建工具
 *
 * @author Ateng
 * @since 2026-05-08
 */
public final class WorkflowVariableBuilder {

    private WorkflowVariableBuilder() {
    }

    /**
     * 构建请假流程变量
     *
     * @param businessId 业务主键
     * @param initiator 发起人ID
     * @param deptId 部门ID
     * @param days 请假天数
     * @param leaderId 负责人ID
     * @return 流程变量
     */
    public static Map<String, Object> buildLeaveVariable(String businessId,
                                                         String initiator,
                                                         String deptId,
                                                         Integer days,
                                                         String leaderId) {
        if (StrUtil.hasBlank(businessId, initiator, deptId)) {
            throw new IllegalArgumentException("业务主键、发起人、部门不能为空");
        }

        return MapUtil.<String, Object>builder()
                .put("businessId", businessId)
                .put("businessType", "leave")
                .put("initiator", initiator)
                .put("deptId", deptId)
                .put("days", days)
                .put("leaderId", leaderId)
                .build();
    }

    /**
     * 构建报销流程变量
     *
     * @param businessId 业务主键
     * @param initiator 发起人ID
     * @param deptId 部门ID
     * @param amount 报销金额
     * @return 流程变量
     */
    public static Map<String, Object> buildExpenseVariable(String businessId,
                                                           String initiator,
                                                           String deptId,
                                                           BigDecimal amount) {
        if (StrUtil.hasBlank(businessId, initiator, deptId)) {
            throw new IllegalArgumentException("业务主键、发起人、部门不能为空");
        }

        return MapUtil.<String, Object>builder()
                .put("businessId", businessId)
                .put("businessType", "expense")
                .put("initiator", initiator)
                .put("deptId", deptId)
                .put("amount", amount)
                .build();
    }

}
```

流程变量使用注意事项：

1. 变量名应统一维护，避免 `userId`、`startUserId`、`initiator` 混用。
2. 金额、天数等用于条件判断的字段，应保证类型可比较。
3. 不建议将完整业务实体直接存入流程变量，避免变量过大和序列化问题。
4. 敏感字段不建议进入流程变量，例如身份证号、银行卡号、手机号明文等。
5. 前端传入变量必须经过后端校验和重组，不能直接透传到流程引擎。

### 条件表达式配置

条件表达式主要用于互斥网关等分支节点，决定流程应该进入哪条后续分支。Warm-Flow 官方文档说明，条件表达式支持默认表达式、SpEL 表达式、大于、大于等于、等于、不等于、小于、小于等于、包含、不包含以及自定义表达式等类型。([Warm-Flow官网](https://www.warm-flow.com/master/primary/condition.html?utm_source=chatgpt.com))

常用表达式示例：

| 表达式类型  | 示例                                                  | 说明                      |
| ----------- | ----------------------------------------------------- | ------------------------- |
| 默认表达式  | `default@@${days > 3}`                                | 使用变量直接判断          |
| SpEL 表达式 | `spel@@#{@workflowCondition.amountGt(#amount, 5000)}` | 调用 Spring Bean 方法判断 |
| 大于        | `gt@@days                                             | 3`                        |
| 大于等于    | `ge@@amount                                           | 5000`                     |
| 等于        | `eq@@businessType                                     | leave`                    |
| 不等于      | `ne@@deptId                                           | D001`                     |
| 包含        | `like@@roleCodes                                      | admin`                    |
| 不包含      | `notLike@@roleCodes                                   | guest`                    |

请假流程条件示例：

```text
开始
  -> 提交申请
  -> 部门负责人审批
  -> 条件分支
      -> days > 3：分管领导审批
      -> days <= 3：结束
```

对应条件表达式：

```text
gt@@days|3
le@@days|3
```

报销流程条件示例：

```text
开始
  -> 提交报销
  -> 部门负责人审批
  -> 条件分支
      -> amount >= 5000：财务经理审批
      -> amount < 5000：财务专员审批
```

对应条件表达式：

```text
ge@@amount|5000
lt@@amount|5000
```

条件表达式配置建议：

1. 简单数字、字符串判断优先使用内置表达式，例如 `gt`、`ge`、`eq`。
2. 多字段组合判断使用默认表达式或 SpEL 表达式。
3. 复杂业务判断不要直接写在流程定义中，应封装到 Spring Bean。
4. 条件分支应尽量互斥，避免多个分支同时满足。
5. 建议保留默认兜底分支，避免流程变量缺失导致流程无法继续。

### SpEL 表达式扩展

SpEL 表达式适合处理复杂流程条件，例如金额判断、部门规则、用户等级、项目类型、业务白名单等。Warm-Flow 条件表达式支持通过 `spel@@#{@beanName.method(#variable)}` 的方式调用 Spring 容器中的 Bean 方法。([Warm-Flow官网](https://www.warm-flow.com/master/primary/condition.html?utm_source=chatgpt.com))

文件位置：`src/main/java/io/github/atengk/workflow/expression/WorkflowCondition.java`

下面代码用于扩展流程条件判断逻辑，供 Warm-Flow SpEL 条件表达式调用。

```java
package io.github.atengk.workflow.expression;

import cn.hutool.core.convert.Convert;
import cn.hutool.core.util.NumberUtil;
import cn.hutool.core.util.StrUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.Set;

/**
 * 工作流条件表达式扩展
 *
 * @author Ateng
 * @since 2026-05-08
 */
@Slf4j
@Component("workflowCondition")
public class WorkflowCondition {

    private static final Set<String> HIGH_RISK_BUSINESS_TYPES = Set.of("contract", "purchase", "payment");

    /**
     * 判断金额是否大于等于指定阈值
     *
     * @param amount 金额
     * @param threshold 阈值
     * @return 是否满足条件
     */
    public boolean amountGe(Object amount, Object threshold) {
        BigDecimal amountValue = Convert.toBigDecimal(amount, BigDecimal.ZERO);
        BigDecimal thresholdValue = Convert.toBigDecimal(threshold, BigDecimal.ZERO);
        boolean result = NumberUtil.compare(amountValue, thresholdValue) >= 0;
        log.debug("流程金额条件判断，金额：{}，阈值：{}，结果：{}", amountValue, thresholdValue, result);
        return result;
    }

    /**
     * 判断请假天数是否大于指定阈值
     *
     * @param days 请假天数
     * @param threshold 阈值
     * @return 是否满足条件
     */
    public boolean daysGt(Object days, Object threshold) {
        Integer dayValue = Convert.toInt(days, 0);
        Integer thresholdValue = Convert.toInt(threshold, 0);
        boolean result = dayValue > thresholdValue;
        log.debug("流程天数条件判断，天数：{}，阈值：{}，结果：{}", dayValue, thresholdValue, result);
        return result;
    }

    /**
     * 判断是否高风险业务
     *
     * @param businessType 业务类型
     * @return 是否高风险
     */
    public boolean highRiskBusiness(String businessType) {
        boolean result = StrUtil.isNotBlank(businessType) && HIGH_RISK_BUSINESS_TYPES.contains(businessType);
        log.debug("流程高风险业务判断，业务类型：{}，结果：{}", businessType, result);
        return result;
    }

}
```

SpEL 条件表达式配置示例：

```text
spel@@#{@workflowCondition.amountGe(#amount, 5000)}
spel@@#{@workflowCondition.daysGt(#days, 3)}
spel@@#{@workflowCondition.highRiskBusiness(#businessType)}
```

SpEL 表达式使用注意事项：

1. Bean 名称需要与 `@Component("workflowCondition")` 中的名称一致。
2. 表达式中的变量名需要与流程变量 Map 中的 key 一致。
3. 方法返回值必须是 `boolean` 或可转换为布尔值的结果。
4. 表达式方法内部应具备空值处理能力，避免变量缺失导致流程异常。
5. 表达式方法不建议执行写库、发消息等副作用操作，写操作应放到监听器或业务 Service 中。

## 监听器开发

监听器用于在流程运行的关键生命周期中插入业务处理逻辑，例如初始化数据、动态设置办理人、审批完成后同步业务状态、任务创建后发送待办通知等。Warm-Flow 官方文档说明，监听器支持类包名配置和表达式配置，并支持 `start`、`assignment`、`finish`、`create` 四类监听器。([Warm-Flow官网](https://www.warm-flow.com/v1.8.2/advanced/listener.html?utm_source=chatgpt.com))

### 流程监听器

流程监听器配置在流程定义上，作用范围是当前流程定义。只要流程定义中的节点触发对应生命周期，就会执行流程监听器。Warm-Flow 文档说明监听器作用范围包括节点监听器、流程监听器和全局监听器，执行顺序为节点监听器、流程监听器、全局监听器。([Warm-Flow官网](https://www.warm-flow.com/master/advanced/listener.html?utm_source=chatgpt.com))

流程监听器适合处理通用流程级逻辑，例如：

| 监听器类型 | 适用场景                                           |
| ---------- | -------------------------------------------------- |
| start      | 流程开始时初始化变量、设置默认办理人、记录启动日志 |
| assignment | 分派办理人时动态调整待办任务信息                   |
| finish     | 当前任务完成后同步业务状态、写审批记录、发送通知   |
| create     | 新任务创建后生成待办提醒、消息通知、任务摘要       |

流程监听器配置示例：

```text
listener_type = start,finish
listener_path = io.github.atengk.workflow.listener.FlowStartListener,io.github.atengk.workflow.listener.FlowFinishListener
```

如果使用 Spring Bean 表达式方式配置，可以使用：

```text
listener_type = finish
listener_path = #{@flowFinishListener.notify(#listenerVariable)}
```

文件位置：`src/main/java/io/github/atengk/workflow/listener/FlowFinishListener.java`

下面监听器用于在任务完成后同步业务流程状态，并记录流程完成日志。`ListenerVariable` 中包含流程实例、节点、任务、新创建任务集合和流程变量等信息，官方源码显示其提供 `getInstance()`、`getNode()`、`getTask()`、`getNextTasks()` 和 `getVariable()` 等方法。([Gitee](https://gitee.com/dromara/warm-flow/blob/659b5c8f085d64d14972843080bbb2539eda4a4f/warm-flow-core/src/main/java/com/warm/flow/core/listener/ListenerVariable.java?utm_source=chatgpt.com))

```java
package io.github.atengk.workflow.listener;

import cn.hutool.core.map.MapUtil;
import cn.hutool.core.util.ObjectUtil;
import com.warm.flow.core.entity.Instance;
import com.warm.flow.core.listener.Listener;
import com.warm.flow.core.listener.ListenerVariable;
import io.github.atengk.workflow.service.WorkflowBusinessService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.Map;

/**
 * 流程完成监听器
 *
 * @author Ateng
 * @since 2026-05-08
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class FlowFinishListener implements Listener {

    private final WorkflowBusinessService workflowBusinessService;

    /**
     * 监听流程任务完成事件
     *
     * @param variable 监听器变量
     */
    @Override
    public void notify(ListenerVariable variable) {
        if (ObjectUtil.isNull(variable) || ObjectUtil.isNull(variable.getInstance())) {
            log.warn("流程完成监听器跳过处理，监听器变量为空");
            return;
        }

        Instance instance = variable.getInstance();
        Map<String, Object> flowVariable = variable.getVariable();
        String businessId = MapUtil.getStr(flowVariable, "businessId");
        String businessType = MapUtil.getStr(flowVariable, "businessType");

        workflowBusinessService.refreshBusinessStatus(businessId, businessType, instance);
        log.info("流程完成监听器处理成功，业务ID：{}，业务类型：{}，实例ID：{}", businessId, businessType, instance.getId());
    }

}
```

如果当前项目中的 Warm-Flow 版本接口包名与示例存在差异，应以当前依赖版本中的实际包名为准。可在 IDE 中通过 `Listener` 和 `ListenerVariable` 类跳转确认。

### 任务监听器

任务监听器更适合处理节点级逻辑，例如任务创建后发送待办通知、分派办理人时动态调整办理人、某个审批节点完成后写入特定业务记录等。Warm-Flow 监听器文档说明，`create` 在任务创建时执行，`assignment` 用于分派办理人时动态修改待办任务信息，`finish` 在当前任务完成后执行。([Warm-Flow官网](https://www.warm-flow.com/v1.8.2/advanced/listener.html?utm_source=chatgpt.com))

任务监听器常见用途如下：

| 类型       | 用途                                       |
| ---------- | ------------------------------------------ |
| create     | 生成站内信、短信、邮件、企业微信、钉钉待办 |
| assignment | 动态设置任务办理人、补充权限标识           |
| finish     | 记录审批动作、同步节点结果、更新业务摘要   |
| start      | 节点开始办理时初始化节点上下文             |

文件位置：`src/main/java/io/github/atengk/workflow/listener/TaskCreateListener.java`

下面监听器用于在新任务创建后发送待办通知。实际项目中可以替换为 MQ、站内信、短信或第三方消息平台。

```java
package io.github.atengk.workflow.listener;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.map.MapUtil;
import com.warm.flow.core.entity.Task;
import com.warm.flow.core.listener.Listener;
import com.warm.flow.core.listener.ListenerVariable;
import io.github.atengk.workflow.service.WorkflowNotifyService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;

/**
 * 任务创建监听器
 *
 * @author Ateng
 * @since 2026-05-08
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class TaskCreateListener implements Listener {

    private final WorkflowNotifyService workflowNotifyService;

    /**
     * 监听新任务创建事件
     *
     * @param variable 监听器变量
     */
    @Override
    public void notify(ListenerVariable variable) {
        List<Task> nextTasks = variable.getNextTasks();
        if (CollUtil.isEmpty(nextTasks)) {
            log.debug("任务创建监听器未发现新任务，跳过通知");
            return;
        }

        Map<String, Object> flowVariable = variable.getVariable();
        String businessId = MapUtil.getStr(flowVariable, "businessId");
        String businessType = MapUtil.getStr(flowVariable, "businessType");

        for (Task task : nextTasks) {
            workflowNotifyService.sendTodoNotify(businessId, businessType, task);
            log.info("待办任务通知已发送，业务ID：{}，任务ID：{}", businessId, task.getId());
        }
    }

}
```

监听器配置示例：

```text
listener_type = create
listener_path = io.github.atengk.workflow.listener.TaskCreateListener
```

或者使用 Spring Bean 表达式：

```text
listener_type = create
listener_path = #{@taskCreateListener.notify(#listenerVariable)}
```

任务监听器注意事项：

1. 监听器中不要执行耗时过长的同步逻辑。
2. 发送消息建议通过 MQ 或异步事件处理。
3. 监听器异常会影响流程执行时，应明确异常处理策略。
4. 核心业务状态同步建议保证事务一致性。
5. 通知类逻辑可允许失败重试，不能阻断审批主流程。

### 业务回调处理

业务回调处理用于把流程引擎事件转换为业务系统动作，例如更新业务单据状态、记录审批轨迹、发送消息通知、刷新待办缓存等。建议将监听器作为入口，将具体业务处理下沉到业务 Service，避免监听器中堆积复杂逻辑。

业务回调建议分层如下：

```text
Warm-Flow Listener
  -> WorkflowCallbackService
  -> 业务 Service
  -> 业务表 / 审批记录表 / 消息服务
```

文件位置：`src/main/java/io/github/atengk/workflow/service/WorkflowCallbackService.java`

下面服务用于统一处理流程回调，监听器只负责调用该服务。

```java
package io.github.atengk.workflow.service;

import cn.hutool.core.map.MapUtil;
import cn.hutool.core.util.StrUtil;
import com.warm.flow.core.entity.Instance;
import com.warm.flow.core.entity.Task;
import com.warm.flow.core.listener.ListenerVariable;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.Map;

/**
 * 工作流业务回调服务
 *
 * @author Ateng
 * @since 2026-05-08
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class WorkflowCallbackService {

    private final WorkflowBusinessService workflowBusinessService;
    private final WorkflowApprovalRecordService workflowApprovalRecordService;

    /**
     * 处理任务完成回调
     *
     * @param listenerVariable 监听器变量
     */
    public void handleTaskFinish(ListenerVariable listenerVariable) {
        Instance instance = listenerVariable.getInstance();
        Task task = listenerVariable.getTask();
        Map<String, Object> variable = listenerVariable.getVariable();

        String businessId = MapUtil.getStr(variable, "businessId");
        String businessType = MapUtil.getStr(variable, "businessType");
        String actionType = MapUtil.getStr(variable, "actionType", "PASS");

        if (StrUtil.hasBlank(businessId, businessType)) {
            log.warn("任务完成回调缺少业务标识，实例ID：{}", instance == null ? null : instance.getId());
            return;
        }

        workflowApprovalRecordService.recordByTask(businessId, businessType, instance, task, actionType, variable);
        workflowBusinessService.refreshBusinessStatus(businessId, businessType, instance);

        log.info("任务完成回调处理成功，业务ID：{}，业务类型：{}，动作：{}", businessId, businessType, actionType);
    }

}
```

回调处理原则：

1. 监听器只做轻量分发，不写复杂业务逻辑。
2. 业务状态同步必须幂等，避免重复监听或重试导致状态异常。
3. 审批记录落库应保留任务 ID、实例 ID、节点编码、操作人和审批意见。
4. 消息通知建议异步处理，避免第三方服务异常影响流程主链路。
5. 回调异常需要记录清楚业务 ID、实例 ID、任务 ID，便于排查。

## 业务系统集成

业务系统集成是工作流落地的关键。Warm-Flow 负责流程流转，业务系统负责业务单据、业务状态、审批记录、权限控制和页面展示。两者应通过业务主键、流程编码和流程实例 ID 建立稳定关联。

### 业务单据绑定

业务单据绑定用于记录某个业务单据对应的流程实例。建议业务系统维护独立的流程绑定表，不要只依赖业务主表字段。这样可以支持不同业务类型统一查询流程状态，也便于后续扩展多流程、多版本和审计能力。

绑定关系建议如下：

| 字段           | 说明        |
| -------------- | ----------- |
| business_id    | 业务主键    |
| business_code  | 业务编号    |
| business_type  | 业务类型    |
| flow_code      | 流程编码    |
| instance_id    | 流程实例 ID |
| process_status | 流程状态    |
| start_user_id  | 发起人      |
| start_time     | 发起时间    |
| finish_time    | 结束时间    |

发起流程后的绑定处理流程：

```text
校验业务单据
  -> 构建流程变量
  -> 调用 insService.start
  -> 保存 wf_business_flow
  -> 更新业务单据状态为 APPROVING
  -> 写入审批记录
```

文件位置：`src/main/java/io/github/atengk/workflow/service/WorkflowBusinessBindService.java`

下面服务用于保存业务单据与流程实例的绑定关系。

```java
package io.github.atengk.workflow.service;

import cn.hutool.core.date.DateUtil;
import cn.hutool.core.util.StrUtil;
import com.warm.flow.core.entity.Instance;
import io.github.atengk.workflow.entity.WorkflowBusinessFlow;
import io.github.atengk.workflow.mapper.WorkflowBusinessFlowMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

/**
 * 业务流程绑定服务
 *
 * @author Ateng
 * @since 2026-05-08
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class WorkflowBusinessBindService {

    private final WorkflowBusinessFlowMapper workflowBusinessFlowMapper;

    /**
     * 保存业务流程绑定关系
     *
     * @param businessId 业务主键
     * @param businessCode 业务编号
     * @param businessType 业务类型
     * @param flowCode 流程编码
     * @param startUserId 发起人ID
     * @param instance 流程实例
     */
    public void bind(String businessId,
                     String businessCode,
                     String businessType,
                     String flowCode,
                     String startUserId,
                     Instance instance) {
        if (StrUtil.hasBlank(businessId, businessType, flowCode, startUserId)) {
            throw new IllegalArgumentException("业务流程绑定参数不能为空");
        }
        if (instance == null || instance.getId() == null) {
            throw new IllegalArgumentException("流程实例不能为空");
        }

        WorkflowBusinessFlow entity = new WorkflowBusinessFlow();
        entity.setBusinessId(businessId);
        entity.setBusinessCode(businessCode);
        entity.setBusinessType(businessType);
        entity.setFlowCode(flowCode);
        entity.setInstanceId(instance.getId());
        entity.setProcessStatus("APPROVING");
        entity.setStartUserId(startUserId);
        entity.setStartTime(DateUtil.date());
        entity.setCreateTime(DateUtil.date());

        workflowBusinessFlowMapper.insert(entity);
        log.info("业务流程绑定成功，业务ID：{}，流程编码：{}，实例ID：{}", businessId, flowCode, instance.getId());
    }

}
```

业务单据绑定注意事项：

1. 同一个业务单据是否允许多次发起流程，需要由业务规则明确。
2. 重新提交时，可以复用原业务单据，但建议创建新的流程实例。
3. 已审批完成的流程实例不建议复用。
4. 业务主键和流程实例 ID 都应建立索引。
5. 列表页高频字段可以冗余到绑定表，例如当前节点名称、当前办理人摘要等。

### 流程状态同步

流程状态同步用于保证业务单据状态与流程实例状态一致。审批流转过程中，Warm-Flow 的实例状态是流程引擎状态，业务系统还需要维护自己的业务状态，例如草稿、审批中、已通过、已驳回、已撤销、已终止。

推荐状态映射如下：

| 流程动作           | 业务流程状态 | 业务单据状态  |
| ------------------ | ------------ | ------------- |
| 发起流程           | APPROVING    | 审批中        |
| 审批通过且未结束   | APPROVING    | 审批中        |
| 审批通过且流程结束 | APPROVED     | 已通过        |
| 驳回发起人         | REJECTED     | 已驳回 / 草稿 |
| 撤销流程           | REVOKED      | 已撤销 / 草稿 |
| 终止流程           | TERMINATED   | 已终止        |

流程状态同步建议放在业务 Service 中，不建议在 Controller 中直接更新多个表。

文件位置：`src/main/java/io/github/atengk/workflow/service/WorkflowBusinessService.java`

下面服务用于根据流程实例状态刷新业务侧流程状态。示例中的 `isFinished` 判断需要按当前 Warm-Flow 版本的实例状态字段进行调整。

```java
package io.github.atengk.workflow.service;

import cn.hutool.core.util.StrUtil;
import com.warm.flow.core.entity.Instance;
import io.github.atengk.workflow.mapper.WorkflowBusinessFlowMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 工作流业务状态服务
 *
 * @author Ateng
 * @since 2026-05-08
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class WorkflowBusinessService {

    private final WorkflowBusinessFlowMapper workflowBusinessFlowMapper;

    /**
     * 刷新业务流程状态
     *
     * @param businessId 业务主键
     * @param businessType 业务类型
     * @param instance 流程实例
     */
    @Transactional(rollbackFor = Exception.class)
    public void refreshBusinessStatus(String businessId, String businessType, Instance instance) {
        if (StrUtil.hasBlank(businessId, businessType)) {
            log.warn("刷新业务流程状态失败，业务标识为空");
            return;
        }
        if (instance == null) {
            log.warn("刷新业务流程状态失败，流程实例为空，业务ID：{}", businessId);
            return;
        }

        String processStatus = resolveProcessStatus(instance);
        workflowBusinessFlowMapper.updateStatusByBusinessId(businessId, businessType, processStatus);

        log.info("业务流程状态刷新成功，业务ID：{}，业务类型：{}，流程状态：{}", businessId, businessType, processStatus);
    }

    /**
     * 根据流程实例解析业务流程状态
     *
     * @param instance 流程实例
     * @return 业务流程状态
     */
    private String resolveProcessStatus(Instance instance) {
        // 不同 Warm-Flow 版本实例状态字段可能存在差异，这里按项目实际状态枚举调整
        Object status = instance.getFlowStatus();
        if (status == null) {
            return "APPROVING";
        }

        String statusValue = String.valueOf(status);
        if (StrUtil.equalsAnyIgnoreCase(statusValue, "PASS", "FINISH", "DONE", "APPROVED")) {
            return "APPROVED";
        }
        if (StrUtil.equalsAnyIgnoreCase(statusValue, "REJECT", "REJECTED")) {
            return "REJECTED";
        }
        if (StrUtil.equalsAnyIgnoreCase(statusValue, "REVOKE", "REVOKED")) {
            return "REVOKED";
        }
        if (StrUtil.equalsAnyIgnoreCase(statusValue, "TERMINATION", "TERMINATED")) {
            return "TERMINATED";
        }
        return "APPROVING";
    }

}
```

Mapper 示例：

```java
package io.github.atengk.workflow.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import io.github.atengk.workflow.entity.WorkflowBusinessFlow;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Update;

/**
 * 业务流程绑定 Mapper
 *
 * @author Ateng
 * @since 2026-05-08
 */
@Mapper
public interface WorkflowBusinessFlowMapper extends BaseMapper<WorkflowBusinessFlow> {

    /**
     * 根据业务主键更新流程状态
     *
     * @param businessId 业务主键
     * @param businessType 业务类型
     * @param processStatus 流程状态
     * @return 影响行数
     */
    @Update("""
            UPDATE wf_business_flow
            SET process_status = #{processStatus},
                update_time = NOW()
            WHERE business_id = #{businessId}
              AND business_type = #{businessType}
              AND deleted = 0
            """)
    int updateStatusByBusinessId(@Param("businessId") String businessId,
                                 @Param("businessType") String businessType,
                                 @Param("processStatus") String processStatus);

}
```

流程状态同步注意事项：

1. 发起流程、审批通过、驳回、撤销、终止都需要同步状态。
2. 状态同步应放在事务中，避免流程已流转但业务状态未更新。
3. 如果流程监听器和审批接口都可能更新状态，需要保证幂等。
4. 业务状态字段建议使用枚举统一管理。
5. 流程引擎状态和业务状态不要强行共用一个枚举。

### 审批记录落库

审批记录落库用于展示审批轨迹、支持审计查询、追踪异常问题和生成业务报表。Warm-Flow 内部会维护流程历史数据，但业务系统仍建议维护自己的审批记录表，用于保存业务标题、审批意见、操作人名称、业务状态和扩展字段。

审批记录建议包含以下字段：

| 字段          | 说明                                          |
| ------------- | --------------------------------------------- |
| business_id   | 业务主键                                      |
| instance_id   | 流程实例 ID                                   |
| task_id       | 任务 ID                                       |
| node_code     | 节点编码                                      |
| node_name     | 节点名称                                      |
| operator_id   | 操作人 ID                                     |
| operator_name | 操作人名称                                    |
| action_type   | 操作类型，例如 PASS、REJECT、TRANSFER、DEPUTE |
| message       | 审批意见                                      |
| operate_time  | 操作时间                                      |

文件位置：`src/main/java/io/github/atengk/workflow/service/WorkflowApprovalRecordService.java`

下面服务用于统一记录审批动作，审批接口和监听器都可以调用。

```java
package io.github.atengk.workflow.service;

import cn.hutool.core.date.DateUtil;
import cn.hutool.core.map.MapUtil;
import cn.hutool.core.util.StrUtil;
import com.warm.flow.core.entity.Instance;
import com.warm.flow.core.entity.Task;
import io.github.atengk.workflow.entity.WorkflowApprovalRecord;
import io.github.atengk.workflow.mapper.WorkflowApprovalRecordMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.Map;

/**
 * 工作流审批记录服务
 *
 * @author Ateng
 * @since 2026-05-08
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class WorkflowApprovalRecordService {

    private final WorkflowApprovalRecordMapper workflowApprovalRecordMapper;

    /**
     * 根据任务记录审批轨迹
     *
     * @param businessId 业务主键
     * @param businessType 业务类型
     * @param instance 流程实例
     * @param task 当前任务
     * @param actionType 操作类型
     * @param variable 流程变量
     */
    public void recordByTask(String businessId,
                             String businessType,
                             Instance instance,
                             Task task,
                             String actionType,
                             Map<String, Object> variable) {
        if (StrUtil.hasBlank(businessId, businessType, actionType)) {
            log.warn("审批记录落库跳过，关键参数为空，业务ID：{}，业务类型：{}，动作：{}", businessId, businessType, actionType);
            return;
        }

        WorkflowApprovalRecord record = new WorkflowApprovalRecord();
        record.setBusinessId(businessId);
        record.setBusinessType(businessType);
        record.setInstanceId(instance == null ? null : instance.getId());
        record.setTaskId(task == null ? null : task.getId());
        record.setNodeCode(task == null ? null : task.getNodeCode());
        record.setNodeName(task == null ? null : task.getNodeName());
        record.setOperatorId(MapUtil.getStr(variable, "operatorId"));
        record.setOperatorName(MapUtil.getStr(variable, "operatorName"));
        record.setActionType(actionType);
        record.setMessage(MapUtil.getStr(variable, "message"));
        record.setOperateTime(DateUtil.date());

        workflowApprovalRecordMapper.insert(record);
        log.info("审批记录落库成功，业务ID：{}，实例ID：{}，任务ID：{}，动作：{}",
                businessId, record.getInstanceId(), record.getTaskId(), actionType);
    }

}
```

审批记录 Entity 示例：

```java
package io.github.atengk.workflow.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.util.Date;

/**
 * 工作流审批记录实体
 *
 * @author Ateng
 * @since 2026-05-08
 */
@Data
@TableName("wf_approval_record")
public class WorkflowApprovalRecord {

    private Long id;

    private String businessId;

    private String businessType;

    private Long instanceId;

    private Long taskId;

    private String nodeCode;

    private String nodeName;

    private String operatorId;

    private String operatorName;

    private String actionType;

    private String message;

    private Date operateTime;

}
```

审批记录查询返回示例：

```json
{
  "businessId": "LEAVE202605080001",
  "records": [
    {
      "nodeName": "提交申请",
      "operatorName": "张三",
      "actionType": "START",
      "message": "提交请假申请",
      "operateTime": "2026-05-08 09:30:00"
    },
    {
      "nodeName": "部门负责人审批",
      "operatorName": "李四",
      "actionType": "PASS",
      "message": "同意",
      "operateTime": "2026-05-08 10:20:00"
    }
  ]
}
```

审批记录落库注意事项：

1. 审批记录应以追加方式保存，不建议覆盖历史记录。
2. 操作人 ID 和操作人名称建议同时保存，避免用户名称变更影响历史展示。
3. 审批意见需要限制长度，避免超长文本影响页面和数据库。
4. 转办、委派、撤销、终止也应记录为审批轨迹。
5. 审批记录查询应按 `operate_time` 正序返回，便于前端展示时间线。

## 接口设计

接口设计用于将 Warm-Flow 的流程定义、流程实例、任务审批和流程图能力封装成业务系统统一 REST API。后端不建议直接把 Warm-Flow 原始对象完整暴露给前端，而应通过 DTO、VO 和统一响应结构屏蔽引擎细节，保证后续更换流程引擎版本或调整业务字段时接口尽量稳定。

Warm-Flow 官方接口文档中，`DefService` 提供流程定义导入、导出、发布、取消发布、挂起、激活、删除和查询设计数据等能力；`InsService.start(businessId, flowParams)` 用于发起流程实例；`TaskService` 提供通过、退回、任意跳转、撤销、终止、转办、委派等任务处理能力。接口封装时应围绕这些核心服务进行二次封装。([Warm-Flow官网](https://www.warm-flow.com/master/primary/api.html?utm_source=chatgpt.com))

### 流程定义接口

流程定义接口用于维护流程模型、导入流程定义、发布流程定义、查询流程定义版本和获取流程设计数据。该接口通常只开放给流程管理员或系统管理员，普通业务用户不应具备流程定义管理权限。

接口清单建议如下：

| 接口名称          | 请求方式 | 接口路径                               | 说明                         |
| ----------------- | -------- | -------------------------------------- | ---------------------------- |
| 分页查询流程定义  | GET      | `/workflow/definitions`                | 查询流程定义列表             |
| 查询流程定义详情  | GET      | `/workflow/definitions/{id}`           | 查询流程定义详情             |
| 上传导入流程定义  | POST     | `/workflow/definitions/import`         | 上传 JSON 文件导入流程定义   |
| JSON 导入流程定义 | POST     | `/workflow/definitions/import-json`    | 通过 JSON 字符串导入流程定义 |
| 发布流程定义      | PUT      | `/workflow/definitions/{id}/publish`   | 发布流程定义                 |
| 取消发布流程定义  | PUT      | `/workflow/definitions/{id}/unpublish` | 取消发布                     |
| 激活流程定义      | PUT      | `/workflow/definitions/{id}/active`    | 激活流程定义                 |
| 挂起流程定义      | PUT      | `/workflow/definitions/{id}/inactive`  | 挂起流程定义                 |
| 删除流程定义      | DELETE   | `/workflow/definitions/{id}`           | 删除流程定义                 |
| 导出流程定义      | GET      | `/workflow/definitions/{id}/export`    | 导出流程定义 JSON            |
| 查询设计数据      | GET      | `/workflow/definitions/{id}/design`    | 获取流程设计器渲染数据       |

导入流程定义请求示例：

```http
POST /workflow/definitions/import
Content-Type: multipart/form-data

file=@leave_process.json
```

JSON 导入流程定义请求示例：

```json
{
  "flowCode": "leave_process",
  "flowName": "请假审批流程",
  "definitionJson": "{...}"
}
```

流程定义列表返回示例：

```json
{
  "code": 200,
  "message": "操作成功",
  "data": {
    "total": 2,
    "records": [
      {
        "id": 1260200765054652416,
        "flowCode": "leave_process",
        "flowName": "请假审批流程",
        "versionNo": 1,
        "publishStatus": "PUBLISHED",
        "activeStatus": "ACTIVE",
        "createTime": "2026-05-08 10:00:00"
      }
    ]
  }
}
```

流程定义接口实现建议：

1. 导入流程定义时校验 `flowCode` 是否重复。
2. 发布前必须完成流程结构校验，避免缺少开始节点、结束节点或办理人配置。
3. 已发布且存在运行中实例的流程定义不建议直接删除。
4. 流程定义变更应优先创建新版本，而不是覆盖旧版本。
5. 流程定义接口应只允许流程管理员访问。

### 流程实例接口

流程实例接口用于处理业务单据发起流程、查询流程进度、查看流程实例详情、撤销流程和终止流程。业务系统应以业务单据为入口，而不是要求前端直接理解 Warm-Flow 的全部内部对象。

接口清单建议如下：

| 接口名称           | 请求方式 | 接口路径                                     | 说明                       |
| ------------------ | -------- | -------------------------------------------- | -------------------------- |
| 发起流程           | POST     | `/workflow/instances/start`                  | 根据业务单据发起流程       |
| 查询实例详情       | GET      | `/workflow/instances/{instanceId}`           | 查询流程实例详情           |
| 按业务 ID 查询实例 | GET      | `/workflow/instances/business/{businessId}`  | 查询业务单据绑定的流程实例 |
| 查询我的发起       | GET      | `/workflow/instances/my-started`             | 查询当前用户发起的流程     |
| 撤销流程           | POST     | `/workflow/instances/{instanceId}/revoke`    | 发起人撤销流程             |
| 终止流程           | POST     | `/workflow/instances/{instanceId}/terminate` | 管理员或有权限用户终止流程 |
| 挂起实例           | PUT      | `/workflow/instances/{instanceId}/inactive`  | 挂起流程实例               |
| 激活实例           | PUT      | `/workflow/instances/{instanceId}/active`    | 激活流程实例               |

发起流程请求示例：

```json
{
  "businessId": "LEAVE202605080001",
  "businessCode": "QJ202605080001",
  "businessType": "leave",
  "flowCode": "leave_process",
  "variable": {
    "days": 3,
    "deptId": "D001",
    "initiator": "10001",
    "leaderId": "10002"
  }
}
```

发起流程返回示例：

```json
{
  "code": 200,
  "message": "操作成功",
  "data": {
    "businessId": "LEAVE202605080001",
    "flowCode": "leave_process",
    "instanceId": 1260200765054652416,
    "processStatus": "APPROVING",
    "startTime": "2026-05-08 10:30:00"
  }
}
```

流程实例详情返回示例：

```json
{
  "code": 200,
  "message": "操作成功",
  "data": {
    "instanceId": 1260200765054652416,
    "businessId": "LEAVE202605080001",
    "businessCode": "QJ202605080001",
    "businessType": "leave",
    "flowCode": "leave_process",
    "processStatus": "APPROVING",
    "currentNodeCode": "dept_approve",
    "currentNodeName": "部门负责人审批",
    "currentHandlers": [
      {
        "userId": "10002",
        "userName": "李四"
      }
    ],
    "startUserId": "10001",
    "startUserName": "张三",
    "startTime": "2026-05-08 10:30:00",
    "finishTime": null
  }
}
```

撤销流程请求示例：

```json
{
  "message": "申请内容填写错误，撤回后重新提交"
}
```

终止流程请求示例：

```json
{
  "message": "业务单据已作废，终止审批流程"
}
```

流程实例接口实现建议：

1. 发起流程前必须校验业务单据是否存在。
2. 发起流程前必须校验业务单据当前状态是否允许提交。
3. 撤销流程通常只允许流程发起人操作。
4. 终止流程通常只允许管理员、流程管理员或业务负责人操作。
5. 挂起和激活属于流程管理能力，不建议对普通用户开放。

### 任务审批接口

任务审批接口用于处理待办查询、已办查询、审批通过、驳回、退回、转办和委派。审批动作必须以 `taskId` 为主键执行，因为同一个流程实例可能同时存在多个当前任务，例如并行审批、会签、加签场景。

接口清单建议如下：

| 接口名称       | 请求方式 | 接口路径                                | 说明                   |
| -------------- | -------- | --------------------------------------- | ---------------------- |
| 查询待办任务   | GET      | `/workflow/tasks/todo`                  | 查询当前用户待办       |
| 查询已办任务   | GET      | `/workflow/tasks/done`                  | 查询当前用户已办       |
| 查询任务详情   | GET      | `/workflow/tasks/{taskId}`              | 查询任务详情           |
| 审批通过       | POST     | `/workflow/tasks/{taskId}/pass`         | 当前任务审批通过       |
| 驳回指定节点   | POST     | `/workflow/tasks/{taskId}/reject`       | 驳回到指定节点         |
| 退回上一步     | POST     | `/workflow/tasks/{taskId}/reject-last`  | 退回上一个任务         |
| 转办任务       | POST     | `/workflow/tasks/{taskId}/transfer`     | 将任务转给其他用户     |
| 委派任务       | POST     | `/workflow/tasks/{taskId}/depute`       | 委派其他用户协助处理   |
| 查询可退回节点 | GET      | `/workflow/tasks/{taskId}/reject-nodes` | 查询当前任务可退回节点 |

待办查询请求示例：

```http
GET /workflow/tasks/todo?pageNum=1&pageSize=10&businessType=leave&keyword=请假
```

审批通过请求示例：

```json
{
  "message": "同意",
  "variable": {
    "approved": true,
    "amount": 3000
  },
  "nextHandlers": ["10003"],
  "nextHandlerAppend": false
}
```

驳回请求示例：

```json
{
  "nodeCode": "start",
  "message": "申请材料不完整，请补充附件",
  "variable": {
    "approved": false,
    "rejectReason": "申请材料不完整"
  }
}
```

转办请求示例：

```json
{
  "targetUserIds": ["10005"],
  "message": "该事项由王五负责，转交处理"
}
```

委派请求示例：

```json
{
  "targetUserIds": ["10006"],
  "message": "请协助核对费用明细"
}
```

任务审批接口实现建议：

1. 所有审批动作都必须校验当前用户是否具备任务办理权限。
2. 不能只依赖前端按钮控制审批权限。
3. 审批通过、驳回、转办、委派都应写入审批记录。
4. 转办和委派目标用户必须校验是否存在、是否启用、是否属于当前租户。
5. 驳回指定节点时应限制可驳回范围，避免驳回到后置节点。
6. 审批接口应使用事务，保证流程流转、业务状态更新和审批记录落库一致。

任务审批 Controller 示例：

文件位置：`src/main/java/io/github/atengk/workflow/controller/WorkflowTaskController.java`

下面控制器用于暴露任务审批相关接口，实际项目中可根据统一响应对象和权限框架进行调整。

```java
package io.github.atengk.workflow.controller;

import io.github.atengk.workflow.dto.TaskApproveRequest;
import io.github.atengk.workflow.dto.TaskTransferRequest;
import io.github.atengk.workflow.service.WorkflowTaskAppService;
import io.github.atengk.workflow.vo.ApiResult;
import lombok.RequiredArgsConstructor;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

/**
 * 工作流任务审批接口
 *
 * @author Ateng
 * @since 2026-05-08
 */
@RestController
@RequiredArgsConstructor
@RequestMapping("/workflow/tasks")
public class WorkflowTaskController {

    private final WorkflowTaskAppService workflowTaskAppService;

    /**
     * 审批通过
     *
     * @param taskId 任务ID
     * @param request 审批请求
     * @return 操作结果
     */
    @PostMapping("/{taskId}/pass")
    public ApiResult<Void> pass(@PathVariable Long taskId, @Validated @RequestBody TaskApproveRequest request) {
        workflowTaskAppService.pass(taskId, request);
        return ApiResult.success();
    }

    /**
     * 驳回指定节点
     *
     * @param taskId 任务ID
     * @param request 驳回请求
     * @return 操作结果
     */
    @PostMapping("/{taskId}/reject")
    public ApiResult<Void> reject(@PathVariable Long taskId, @Validated @RequestBody TaskApproveRequest request) {
        workflowTaskAppService.reject(taskId, request);
        return ApiResult.success();
    }

    /**
     * 转办任务
     *
     * @param taskId 任务ID
     * @param request 转办请求
     * @return 操作结果
     */
    @PostMapping("/{taskId}/transfer")
    public ApiResult<Void> transfer(@PathVariable Long taskId, @Validated @RequestBody TaskTransferRequest request) {
        workflowTaskAppService.transfer(taskId, request);
        return ApiResult.success();
    }

    /**
     * 委派任务
     *
     * @param taskId 任务ID
     * @param request 委派请求
     * @return 操作结果
     */
    @PostMapping("/{taskId}/depute")
    public ApiResult<Void> depute(@PathVariable Long taskId, @Validated @RequestBody TaskTransferRequest request) {
        workflowTaskAppService.depute(taskId, request);
        return ApiResult.success();
    }

}
```

审批请求 DTO 示例：

文件位置：`src/main/java/io/github/atengk/workflow/dto/TaskApproveRequest.java`

```java
package io.github.atengk.workflow.dto;

import jakarta.validation.constraints.Size;
import lombok.Data;

import java.util.List;
import java.util.Map;

/**
 * 任务审批请求
 *
 * @author Ateng
 * @since 2026-05-08
 */
@Data
public class TaskApproveRequest {

    /**
     * 目标节点编码，驳回或任意跳转时使用
     */
    private String nodeCode;

    /**
     * 审批意见
     */
    @Size(max = 500, message = "审批意见不能超过500个字符")
    private String message;

    /**
     * 流程变量
     */
    private Map<String, Object> variable;

    /**
     * 下一节点办理人
     */
    private List<String> nextHandlers;

    /**
     * 是否追加下一节点办理人
     */
    private Boolean nextHandlerAppend;

}
```

转办委派请求 DTO 示例：

文件位置：`src/main/java/io/github/atengk/workflow/dto/TaskTransferRequest.java`

```java
package io.github.atengk.workflow.dto;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.util.List;

/**
 * 任务转办委派请求
 *
 * @author Ateng
 * @since 2026-05-08
 */
@Data
public class TaskTransferRequest {

    /**
     * 目标用户ID集合
     */
    @NotEmpty(message = "目标用户不能为空")
    private List<String> targetUserIds;

    /**
     * 操作意见
     */
    @Size(max = 500, message = "操作意见不能超过500个字符")
    private String message;

}
```

### 流程图接口

流程图接口用于前端展示流程定义图、流程实例流转图和当前节点状态。Warm-Flow 官方流程图管理文档说明，查看流程图可访问后端地址 `/warm-flow-ui/index.html?id=${insId}&type=FlowChart&Authorization=token`，其中 `insId` 是流程实例 ID，`type` 固定为 `FlowChart`，`Authorization` 可传用户 token 以共享后端权限。([Warm-Flow官网](https://www.warm-flow.com/v1.8.2/primary/chart_manage.html?utm_source=chatgpt.com))

流程图接口建议分为两类：

| 类型                | 说明                                                 |
| ------------------- | ---------------------------------------------------- |
| iframe 页面地址接口 | 返回 Warm-Flow UI 流程图页面地址，由前端 iframe 展示 |
| 流程图数据接口      | 返回流程定义或流程实例图数据，由前端自定义渲染       |

接口清单建议如下：

| 接口名称           | 请求方式 | 接口路径                                             | 说明                                     |
| ------------------ | -------- | ---------------------------------------------------- | ---------------------------------------- |
| 获取实例流程图地址 | GET      | `/workflow/charts/instances/{instanceId}/url`        | 返回 iframe 页面地址                     |
| 获取定义设计数据   | GET      | `/workflow/charts/definitions/{definitionId}/design` | 返回流程定义设计数据                     |
| 获取实例流转数据   | GET      | `/workflow/charts/instances/{instanceId}`            | 返回流程实例节点、边、当前节点和历史节点 |
| 获取审批轨迹       | GET      | `/workflow/charts/instances/{instanceId}/records`    | 返回流程时间线数据                       |

流程图地址返回示例：

```json
{
  "code": 200,
  "message": "操作成功",
  "data": {
    "url": "/warm-flow-ui/index.html?id=1260200765054652416&type=FlowChart&Authorization=Bearer xxx"
  }
}
```

流程图数据返回示例：

```json
{
  "code": 200,
  "message": "操作成功",
  "data": {
    "instanceId": 1260200765054652416,
    "currentNodeCode": "dept_approve",
    "nodes": [
      {
        "nodeCode": "start",
        "nodeName": "提交申请",
        "status": "FINISHED"
      },
      {
        "nodeCode": "dept_approve",
        "nodeName": "部门负责人审批",
        "status": "RUNNING"
      },
      {
        "nodeCode": "end",
        "nodeName": "结束",
        "status": "WAITING"
      }
    ],
    "edges": [
      {
        "source": "start",
        "target": "dept_approve"
      },
      {
        "source": "dept_approve",
        "target": "end"
      }
    ]
  }
}
```

流程图接口实现建议：

1. 使用 iframe 集成时，后端应控制 token 传递方式，避免敏感 token 泄漏。
2. 如果使用前端自定义渲染，应统一返回节点、连线、当前节点、已完成节点和审批记录。
3. 查询流程图前应校验当前用户是否具备流程查看权限。
4. 流程图数据不应泄漏用户无权查看的业务字段。
5. 已归档流程也应支持查看历史流程图和审批轨迹。

## 前端页面对接

前端页面对接用于将后端流程能力接入业务页面。建议把流程能力封装成独立的前端 API 模块和通用组件，例如流程发起按钮、待办列表、审批弹窗、审批记录时间线和流程图组件。

### 流程发起页面

流程发起页面通常嵌入在业务单据详情页或编辑页中，例如请假申请、报销申请、采购申请等。用户先填写业务表单，保存业务单据后再发起流程。

推荐页面流程如下：

```text
填写业务表单
  -> 保存业务单据
  -> 校验业务状态
  -> 选择或匹配流程编码
  -> 组装流程变量
  -> 调用发起流程接口
  -> 更新页面状态为审批中
```

前端调用示例：

```typescript
// 发起流程
export function startWorkflow(data: {
  businessId: string
  businessCode: string
  businessType: string
  flowCode: string
  variable: Record<string, unknown>
}) {
  return request.post('/workflow/instances/start', data)
}
```

流程发起页面建议展示以下内容：

| 页面区域 | 展示内容                     |
| -------- | ---------------------------- |
| 业务表单 | 请假、报销、采购等业务字段   |
| 流程信息 | 流程名称、流程编码、当前版本 |
| 发起说明 | 申请原因、备注、附件         |
| 下一节点 | 下一审批节点和审批人         |
| 操作按钮 | 保存草稿、提交审批、返回     |

流程发起注意事项：

1. 业务单据必须先保存，再发起流程。
2. 发起流程后业务表单通常不允许随意编辑。
3. 如果需要撤回后重新编辑，应在撤销后恢复编辑权限。
4. 流程变量应由后端根据业务单据重新组装，前端只传必要字段。
5. 发起失败时不能把业务状态改为审批中。

### 待办任务页面

待办任务页面用于展示当前用户需要处理的任务，是审批功能中使用频率最高的页面。页面应支持分页、关键字搜索、业务类型过滤、时间范围过滤和快速审批入口。

待办页面字段建议如下：

| 字段              | 说明         |
| ----------------- | ------------ |
| businessTitle     | 业务标题     |
| businessCode      | 业务编号     |
| businessTypeName  | 业务类型名称 |
| nodeName          | 当前审批节点 |
| startUserName     | 发起人       |
| createTime        | 到达时间     |
| processStatusName | 流程状态     |
| actions           | 可执行操作   |

前端调用示例：

```typescript
// 查询待办任务
export function getTodoTasks(params: {
  pageNum: number
  pageSize: number
  businessType?: string
  keyword?: string
}) {
  return request.get('/workflow/tasks/todo', { params })
}
```

待办页面操作建议：

| 操作       | 说明                       |
| ---------- | -------------------------- |
| 查看详情   | 打开业务详情和审批信息     |
| 审批通过   | 打开审批通过弹窗           |
| 驳回       | 打开驳回弹窗并选择目标节点 |
| 转办       | 选择目标用户并提交转办     |
| 委派       | 选择目标用户并提交委派     |
| 查看流程图 | 打开流程图弹窗或页面       |

待办页面注意事项：

1. 操作按钮必须以后端返回的 `actions` 为准。
2. 前端隐藏按钮不等于权限控制，后端仍必须校验。
3. 审批提交后应刷新待办列表。
4. 批量审批要谨慎开放，复杂流程建议先不支持。
5. 待办列表不建议一次性加载流程图和完整审批记录。

### 审批详情页面

审批详情页面用于展示业务单据详情、流程实例信息、当前任务信息、审批记录和可执行审批动作。该页面通常由待办列表、已办列表、我的发起列表和业务详情页跳转进入。

页面结构建议如下：

| 区域     | 内容                                           |
| -------- | ---------------------------------------------- |
| 基本信息 | 业务编号、业务类型、发起人、发起时间、流程状态 |
| 业务详情 | 请假、报销、采购等业务字段                     |
| 当前任务 | 当前节点、当前办理人、到达时间                 |
| 审批记录 | 历史审批节点、操作人、意见、时间               |
| 流程图   | 流程节点、当前节点、已完成节点                 |
| 操作区   | 通过、驳回、转办、委派、撤销、终止             |

审批详情接口调用顺序建议：

```text
进入页面
  -> 查询业务详情
  -> 查询流程实例详情
  -> 查询审批记录
  -> 查询当前任务可执行动作
  -> 按需加载流程图
```

审批提交示例：

```typescript
// 审批通过
export function passTask(taskId: string | number, data: {
  message?: string
  variable?: Record<string, unknown>
  nextHandlers?: string[]
  nextHandlerAppend?: boolean
}) {
  return request.post(`/workflow/tasks/${taskId}/pass`, data)
}

// 驳回任务
export function rejectTask(taskId: string | number, data: {
  nodeCode: string
  message: string
  variable?: Record<string, unknown>
}) {
  return request.post(`/workflow/tasks/${taskId}/reject`, data)
}
```

审批详情页面注意事项：

1. 审批意见建议限制长度，例如 500 字。
2. 驳回时应先查询可驳回节点，不允许前端随意输入节点编码。
3. 转办和委派用户选择器应过滤禁用用户和无权限用户。
4. 流程结束后操作按钮应全部隐藏。
5. 已办详情通常只允许查看，不允许重复审批。

### 流程图展示页面

流程图展示页面用于展示流程定义图或流程实例运行图。可以采用 Warm-Flow 自带 UI 的 iframe 集成方式，也可以由前端基于后端返回的节点和连线数据自定义渲染。Warm-Flow 官方流程图文档给出的 iframe 入口为 `/warm-flow-ui/index.html?id=${insId}&type=FlowChart&Authorization=token`，适合快速接入流程图查看能力。([Warm-Flow官网](https://www.warm-flow.com/v1.8.2/primary/chart_manage.html?utm_source=chatgpt.com))

iframe 集成示例：

```vue
<template>
  <div class="workflow-chart">
    <iframe
      v-if="chartUrl"
      :src="chartUrl"
      class="workflow-chart__iframe"
    />
  </div>
</template>

<script setup lang="ts">
import { onMounted, ref } from 'vue'
import { getWorkflowChartUrl } from '@/api/workflow'

const props = defineProps<{
  instanceId: string | number
}>()

const chartUrl = ref('')

onMounted(async () => {
  const res = await getWorkflowChartUrl(props.instanceId)
  chartUrl.value = res.data.url
})
</script>

<style scoped lang="scss">
.workflow-chart {
  width: 100%;
  height: 640px;

  &__iframe {
    width: 100%;
    height: 100%;
    border: none;
  }
}
</style>
```

流程图 API 调用示例：

```typescript
// 获取流程图 iframe 地址
export function getWorkflowChartUrl(instanceId: string | number) {
  return request.get(`/workflow/charts/instances/${instanceId}/url`)
}
```

流程图展示注意事项：

1. iframe 高度需要根据弹窗或页面布局动态调整。
2. token 不建议长期暴露在 URL 中，可结合短期票据或后端代理方案优化。
3. 自定义渲染流程图时，应明确节点状态：未开始、进行中、已完成、已驳回、已终止。
4. 历史流程图应按实例绑定的流程定义版本展示，不应始终展示最新版流程定义。
5. 流程图查看接口必须做权限校验。

## 异常处理与日志

异常处理与日志用于保证流程相关问题能够被准确提示、快速定位和审计追踪。工作流系统涉及业务状态、流程状态、审批权限和数据一致性，异常处理不能只返回通用系统错误，而应明确区分业务异常、流程异常和系统异常。

### 业务异常处理

业务异常是指业务规则不满足导致的错误，例如单据不存在、状态不允许提交、当前用户无审批权限、目标转办用户无效等。业务异常应返回明确错误码和错误信息，便于前端展示和用户处理。

常见业务异常如下：

| 异常场景       | 错误码                      | 提示信息                  |
| -------------- | --------------------------- | ------------------------- |
| 业务单据不存在 | `BIZ_NOT_FOUND`             | 业务单据不存在            |
| 状态不允许提交 | `BIZ_STATUS_INVALID`        | 当前状态不允许提交审批    |
| 重复发起流程   | `FLOW_ALREADY_STARTED`      | 当前单据已发起流程        |
| 无审批权限     | `TASK_PERMISSION_DENIED`    | 当前用户无权处理该任务    |
| 审批意见过长   | `APPROVAL_MESSAGE_TOO_LONG` | 审批意见不能超过500个字符 |
| 转办用户无效   | `TRANSFER_USER_INVALID`     | 转办用户不存在或已禁用    |

业务异常类示例：

文件位置：`src/main/java/io/github/atengk/common/exception/BusinessException.java`

下面异常类用于封装可预期的业务规则错误。

```java
package io.github.atengk.common.exception;

import cn.hutool.core.util.StrUtil;
import lombok.Getter;

/**
 * 业务异常
 *
 * @author Ateng
 * @since 2026-05-08
 */
@Getter
public class BusinessException extends RuntimeException {

    private final String code;

    public BusinessException(String message) {
        super(message);
        this.code = "BUSINESS_ERROR";
    }

    public BusinessException(String code, String message) {
        super(StrUtil.blankToDefault(message, "业务处理失败"));
        this.code = StrUtil.blankToDefault(code, "BUSINESS_ERROR");
    }

}
```

统一异常处理示例：

文件位置：`src/main/java/io/github/atengk/common/handler/GlobalExceptionHandler.java`

下面处理器用于统一拦截参数校验异常、业务异常和系统异常。

```java
package io.github.atengk.common.handler;

import cn.hutool.core.exceptions.ExceptionUtil;
import cn.hutool.core.util.StrUtil;
import io.github.atengk.common.exception.BusinessException;
import io.github.atengk.workflow.vo.ApiResult;
import jakarta.validation.ConstraintViolationException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

/**
 * 全局异常处理器
 *
 * @author Ateng
 * @since 2026-05-08
 */
@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    /**
     * 处理业务异常
     *
     * @param exception 业务异常
     * @return 统一响应
     */
    @ExceptionHandler(BusinessException.class)
    public ApiResult<Void> handleBusinessException(BusinessException exception) {
        log.warn("业务处理失败，错误码：{}，错误信息：{}", exception.getCode(), exception.getMessage());
        return ApiResult.fail(exception.getCode(), exception.getMessage());
    }

    /**
     * 处理请求体参数校验异常
     *
     * @param exception 参数校验异常
     * @return 统一响应
     */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ApiResult<Void> handleMethodArgumentNotValidException(MethodArgumentNotValidException exception) {
        String message = exception.getBindingResult()
                .getFieldErrors()
                .stream()
                .findFirst()
                .map(error -> StrUtil.format("{}：{}", error.getField(), error.getDefaultMessage()))
                .orElse("请求参数校验失败");

        log.warn("请求参数校验失败，错误信息：{}", message);
        return ApiResult.fail("PARAM_INVALID", message);
    }

    /**
     * 处理普通参数校验异常
     *
     * @param exception 参数校验异常
     * @return 统一响应
     */
    @ExceptionHandler(ConstraintViolationException.class)
    public ApiResult<Void> handleConstraintViolationException(ConstraintViolationException exception) {
        log.warn("请求参数校验失败，错误信息：{}", exception.getMessage());
        return ApiResult.fail("PARAM_INVALID", exception.getMessage());
    }

    /**
     * 处理系统异常
     *
     * @param exception 系统异常
     * @return 统一响应
     */
    @ExceptionHandler(Exception.class)
    public ApiResult<Void> handleException(Exception exception) {
        log.error("系统异常，异常摘要：{}", ExceptionUtil.getMessage(exception), exception);
        return ApiResult.fail("SYSTEM_ERROR", "系统繁忙，请稍后再试");
    }

}
```

业务异常处理建议：

1. 用户可理解的错误使用业务异常，不要直接抛出空指针或数据库异常。
2. 参数错误返回 `PARAM_INVALID`，权限错误返回 `PERMISSION_DENIED`。
3. 审批权限不足应返回明确提示，不要统一返回“系统异常”。
4. 生产环境不要把堆栈信息返回给前端。
5. 后端日志必须保留完整异常堆栈，便于排查。

### 流程异常处理

流程异常是指调用 Warm-Flow 流程引擎时发生的异常，例如流程定义不存在、流程未发布、任务不存在、任务已办理、办理人无权限、流程变量缺失、条件表达式执行失败等。

常见流程异常如下：

| 异常场景       | 处理建议                         |
| -------------- | -------------------------------- |
| 流程定义不存在 | 提示流程未配置，联系管理员       |
| 流程定义未发布 | 提示流程未发布，禁止发起         |
| 任务不存在     | 提示任务不存在或已处理           |
| 当前用户无权限 | 提示当前用户无权办理             |
| 条件表达式失败 | 记录流程变量和表达式信息         |
| 办理人为空     | 阻断流程并提示管理员检查节点配置 |
| 流程实例已结束 | 禁止重复审批、撤销或终止         |

流程异常转换示例：

文件位置：`src/main/java/io/github/atengk/workflow/support/WorkflowExceptionTranslator.java`

下面工具类用于将流程引擎异常转换为业务系统可识别的异常。

```java
package io.github.atengk.workflow.support;

import cn.hutool.core.exceptions.ExceptionUtil;
import cn.hutool.core.util.StrUtil;
import io.github.atengk.common.exception.BusinessException;
import lombok.extern.slf4j.Slf4j;

/**
 * 工作流异常转换器
 *
 * @author Ateng
 * @since 2026-05-08
 */
@Slf4j
public final class WorkflowExceptionTranslator {

    private WorkflowExceptionTranslator() {
    }

    /**
     * 转换流程异常
     *
     * @param exception 原始异常
     * @param businessId 业务主键
     * @param instanceId 流程实例ID
     * @return 业务异常
     */
    public static BusinessException translate(Exception exception, String businessId, Long instanceId) {
        String message = ExceptionUtil.getMessage(exception);
        log.error("流程处理异常，业务ID：{}，实例ID：{}，异常信息：{}", businessId, instanceId, message, exception);

        if (StrUtil.containsAnyIgnoreCase(message, "permission", "权限", "办理人")) {
            return new BusinessException("TASK_PERMISSION_DENIED", "当前用户无权处理该任务");
        }
        if (StrUtil.containsAnyIgnoreCase(message, "definition", "流程定义")) {
            return new BusinessException("FLOW_DEFINITION_INVALID", "流程定义不存在或未发布");
        }
        if (StrUtil.containsAnyIgnoreCase(message, "task", "任务")) {
            return new BusinessException("FLOW_TASK_INVALID", "任务不存在或已处理");
        }
        if (StrUtil.containsAnyIgnoreCase(message, "variable", "变量", "expression", "表达式")) {
            return new BusinessException("FLOW_VARIABLE_INVALID", "流程变量或条件表达式配置异常");
        }

        return new BusinessException("FLOW_ENGINE_ERROR", "流程处理失败，请联系管理员");
    }

}
```

流程服务中使用示例：

```java
try {
    taskService.skip(taskId, flowParams);
} catch (Exception e) {
    throw WorkflowExceptionTranslator.translate(e, businessId, instanceId);
}
```

流程异常处理建议：

1. 流程变量、流程编码、业务 ID、实例 ID、任务 ID 必须写入日志。
2. 条件表达式异常应记录变量快照，但要过滤敏感字段。
3. 办理人为空属于配置错误，应提示管理员处理。
4. 重复审批属于常见并发问题，应返回“任务已处理”。
5. 流程引擎异常不要直接返回原始异常信息给前端。

### 操作日志记录

操作日志用于审计用户在流程系统中的关键行为，包括发起流程、审批通过、驳回、转办、委派、撤销、终止、发布流程定义、挂起流程定义等。操作日志与审批记录不同，审批记录面向业务流程轨迹，操作日志面向系统审计和问题排查。

操作日志建议记录以下字段：

| 字段           | 说明        |
| -------------- | ----------- |
| operator_id    | 操作人 ID   |
| operator_name  | 操作人名称  |
| operation_type | 操作类型    |
| business_id    | 业务主键    |
| instance_id    | 流程实例 ID |
| task_id        | 任务 ID     |
| request_uri    | 请求路径    |
| request_method | 请求方式    |
| request_param  | 请求参数    |
| result_status  | 操作结果    |
| error_message  | 异常信息    |
| operate_time   | 操作时间    |
| cost_time      | 耗时        |

操作类型建议如下：

| 操作类型           | 说明         |
| ------------------ | ------------ |
| START_PROCESS      | 发起流程     |
| PASS_TASK          | 审批通过     |
| REJECT_TASK        | 驳回任务     |
| TRANSFER_TASK      | 转办任务     |
| DEPUTE_TASK        | 委派任务     |
| REVOKE_PROCESS     | 撤销流程     |
| TERMINATE_PROCESS  | 终止流程     |
| PUBLISH_DEFINITION | 发布流程定义 |
| IMPORT_DEFINITION  | 导入流程定义 |
| VIEW_CHART         | 查看流程图   |

操作日志注解示例：

文件位置：`src/main/java/io/github/atengk/common/log/OperationLog.java`

下面注解用于标记需要记录操作日志的接口方法。

```java
package io.github.atengk.common.log;

import java.lang.annotation.*;

/**
 * 操作日志注解
 *
 * @author Ateng
 * @since 2026-05-08
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface OperationLog {

    /**
     * 操作类型
     *
     * @return 操作类型
     */
    String type();

    /**
     * 操作说明
     *
     * @return 操作说明
     */
    String description();

}
```

操作日志切面示例：

文件位置：`src/main/java/io/github/atengk/common/log/OperationLogAspect.java`

下面切面用于记录接口操作日志，包含请求路径、请求方式、耗时和异常信息。

```java
package io.github.atengk.common.log;

import cn.hutool.core.date.DateUtil;
import cn.hutool.core.exceptions.ExceptionUtil;
import cn.hutool.core.util.ArrayUtil;
import cn.hutool.json.JSONUtil;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.stereotype.Component;

/**
 * 操作日志切面
 *
 * @author Ateng
 * @since 2026-05-08
 */
@Slf4j
@Aspect
@Component
public class OperationLogAspect {

    private final HttpServletRequest request;

    public OperationLogAspect(HttpServletRequest request) {
        this.request = request;
    }

    /**
     * 记录操作日志
     *
     * @param joinPoint 切点
     * @return 方法执行结果
     * @throws Throwable 原始异常
     */
    @Around("@annotation(io.github.atengk.common.log.OperationLog)")
    public Object around(ProceedingJoinPoint joinPoint) throws Throwable {
        long startTime = System.currentTimeMillis();
        MethodSignature signature = (MethodSignature) joinPoint.getSignature();
        OperationLog operationLog = signature.getMethod().getAnnotation(OperationLog.class);

        String uri = request.getRequestURI();
        String method = request.getMethod();
        String params = ArrayUtil.isEmpty(joinPoint.getArgs()) ? "" : JSONUtil.toJsonStr(joinPoint.getArgs());

        try {
            Object result = joinPoint.proceed();
            long costTime = System.currentTimeMillis() - startTime;

            log.info("操作日志，类型：{}，说明：{}，路径：{}，方式：{}，耗时：{}ms，时间：{}",
                    operationLog.type(), operationLog.description(), uri, method, costTime, DateUtil.now());

            return result;
        } catch (Throwable throwable) {
            long costTime = System.currentTimeMillis() - startTime;

            log.error("操作失败，类型：{}，说明：{}，路径：{}，方式：{}，参数：{}，耗时：{}ms，异常：{}",
                    operationLog.type(),
                    operationLog.description(),
                    uri,
                    method,
                    params,
                    costTime,
                    ExceptionUtil.getMessage(throwable),
                    throwable);

            throw throwable;
        }
    }

}
```

接口使用示例：

```java
@OperationLog(type = "PASS_TASK", description = "审批通过")
@PostMapping("/{taskId}/pass")
public ApiResult<Void> pass(@PathVariable Long taskId, @Validated @RequestBody TaskApproveRequest request) {
    workflowTaskAppService.pass(taskId, request);
    return ApiResult.success();
}
```

操作日志记录建议：

1. 发起流程、审批通过、驳回、撤销、终止必须记录日志。
2. 请求参数中应脱敏手机号、身份证号、银行卡号、token 等敏感信息。
3. 操作日志建议异步落库，避免影响主流程性能。
4. 审批记录和操作日志不要混用，前者面向业务轨迹，后者面向系统审计。
5. 生产环境建议结合 traceId，实现一次请求内 Controller、Service、SQL、流程引擎日志的链路追踪。



## 测试验证

测试验证用于确认 Warm-Flow 与业务系统集成后的核心流程是否稳定，包括流程发起、任务流转、条件分支、权限边界、业务状态同步和审批记录落库。工作流测试不能只验证接口是否返回成功，还需要检查流程实例、待办任务、业务状态和审批轨迹是否一致。

建议将测试分为三类：

| 测试类型     | 说明                                                   |
| ------------ | ------------------------------------------------------ |
| 单元测试     | 验证变量构建、条件表达式、状态映射、权限解析等局部逻辑 |
| 集成测试     | 验证流程发起、审批通过、驳回、撤销、终止等完整链路     |
| 页面联调测试 | 验证前端流程发起页、待办页、审批详情页和流程图页面     |

测试前需要准备以下基础数据：

| 数据类型 | 示例                                                 |
| -------- | ---------------------------------------------------- |
| 流程定义 | `leave_process`、`expense_process`                   |
| 测试用户 | 发起人、部门负责人、财务审批人、管理员               |
| 用户权限 | 用户角色、部门、岗位、租户                           |
| 业务单据 | 请假单、报销单、采购单                               |
| 流程变量 | `businessId`、`days`、`amount`、`deptId`、`leaderId` |

### 流程发起测试

流程发起测试用于验证业务单据是否能够成功启动流程实例，并生成第一批待办任务。测试重点是业务状态、流程实例 ID、流程变量和待办任务是否正确。

测试场景建议如下：

| 场景               | 输入                      | 预期结果                            |
| ------------------ | ------------------------- | ----------------------------------- |
| 正常发起请假流程   | 有效请假单、有效流程编码  | 返回流程实例 ID，业务状态变为审批中 |
| 重复发起流程       | 同一业务单据重复提交      | 返回重复发起错误                    |
| 流程定义不存在     | 错误 `flowCode`           | 返回流程定义不存在或未发布          |
| 业务状态不允许提交 | 已审批通过的单据再次提交  | 返回状态不允许提交                  |
| 流程变量缺失       | 缺少 `leaderId` 或 `days` | 返回变量缺失或办理人为空            |

发起流程接口测试示例：

```bash
# 发起请假审批流程
curl -X POST 'http://localhost:8080/workflow/instances/start' \
  -H 'Content-Type: application/json' \
  -H 'Authorization: Bearer test-token' \
  -d '{
    "businessId": "LEAVE202605080001",
    "businessCode": "QJ202605080001",
    "businessType": "leave",
    "flowCode": "leave_process",
    "variable": {
      "days": 3,
      "deptId": "D001",
      "initiator": "10001",
      "leaderId": "10002"
    }
  }'
```

预期返回：

```json
{
  "code": 200,
  "message": "操作成功",
  "data": {
    "businessId": "LEAVE202605080001",
    "flowCode": "leave_process",
    "instanceId": 1260200765054652416,
    "processStatus": "APPROVING",
    "startTime": "2026-05-08 10:30:00"
  }
}
```

发起成功后需要检查数据库：

```sql
-- 检查业务流程绑定关系
SELECT business_id,
       business_code,
       business_type,
       flow_code,
       instance_id,
       process_status,
       start_user_id,
       start_time
FROM wf_business_flow
WHERE business_id = 'LEAVE202605080001';

-- 检查审批记录是否写入发起记录
SELECT business_id,
       instance_id,
       node_name,
       operator_id,
       action_type,
       message,
       operate_time
FROM wf_approval_record
WHERE business_id = 'LEAVE202605080001'
ORDER BY operate_time ASC;
```

流程发起集成测试示例：

文件位置：`src/test/java/io/github/atengk/workflow/WorkflowStartTest.java`

下面测试类用于验证流程发起后是否生成流程实例，并完成业务绑定。

```java
package io.github.atengk.workflow;

import cn.hutool.core.map.MapUtil;
import cn.hutool.core.util.StrUtil;
import com.warm.flow.core.entity.Instance;
import io.github.atengk.workflow.service.WarmFlowFacadeService;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.Map;

/**
 * 工作流发起测试
 *
 * @author Ateng
 * @since 2026-05-08
 */
@Slf4j
@SpringBootTest
class WorkflowStartTest {

    @Autowired
    private WarmFlowFacadeService warmFlowFacadeService;

    /**
     * 测试正常发起请假流程
     */
    @Test
    void testStartLeaveProcess() {
        String businessId = "LEAVE202605080001";
        String flowCode = "leave_process";
        String handler = "10001";

        Map<String, Object> variable = MapUtil.<String, Object>builder()
                .put("businessId", businessId)
                .put("businessType", "leave")
                .put("initiator", handler)
                .put("deptId", "D001")
                .put("days", 3)
                .put("leaderId", "10002")
                .build();

        Instance instance = warmFlowFacadeService.startProcess(businessId, flowCode, handler, variable);

        Assertions.assertNotNull(instance, "流程实例不能为空");
        Assertions.assertNotNull(instance.getId(), "流程实例ID不能为空");
        Assertions.assertTrue(StrUtil.isNotBlank(String.valueOf(instance.getId())), "流程实例ID不能为空字符串");

        log.info("流程发起测试通过，业务ID：{}，实例ID：{}", businessId, instance.getId());
    }

}
```

流程发起测试通过的判定标准：

1. 接口返回流程实例 ID。
2. `wf_business_flow` 中存在业务绑定记录。
3. 业务状态为 `APPROVING`。
4. 当前审批节点生成待办任务。
5. 审批记录中存在发起记录。
6. 日志中可以追踪业务 ID、流程编码和实例 ID。

### 审批流转测试

审批流转测试用于验证任务从一个节点流转到下一个节点，或者最终进入结束节点。测试时应覆盖审批通过、驳回、退回、转办、委派、撤销和终止等关键动作。

审批通过测试场景：

| 场景               | 操作                | 预期结果                 |
| ------------------ | ------------------- | ------------------------ |
| 部门负责人审批通过 | 当前任务执行通过    | 进入下一节点或结束       |
| 审批意见为空       | 默认审批意见        | 正常通过                 |
| 指定下一办理人     | 传入 `nextHandlers` | 下一节点办理人为指定用户 |
| 重复审批           | 同一任务重复提交    | 第二次返回任务已处理     |
| 非办理人审批       | 无权限用户提交审批  | 返回无权处理任务         |

审批通过接口测试示例：

```bash
# 审批通过当前任务
curl -X POST 'http://localhost:8080/workflow/tasks/1260200765054652416/pass' \
  -H 'Content-Type: application/json' \
  -H 'Authorization: Bearer leader-token' \
  -d '{
    "message": "同意",
    "variable": {
      "approved": true,
      "operatorId": "10002",
      "operatorName": "部门负责人"
    }
  }'
```

驳回接口测试示例：

```bash
# 驳回到发起节点
curl -X POST 'http://localhost:8080/workflow/tasks/1260200765054652416/reject' \
  -H 'Content-Type: application/json' \
  -H 'Authorization: Bearer leader-token' \
  -d '{
    "nodeCode": "start",
    "message": "申请材料不完整，请补充附件",
    "variable": {
      "approved": false,
      "rejectReason": "申请材料不完整",
      "operatorId": "10002",
      "operatorName": "部门负责人"
    }
  }'
```

转办接口测试示例：

```bash
# 转办任务
curl -X POST 'http://localhost:8080/workflow/tasks/1260200765054652416/transfer' \
  -H 'Content-Type: application/json' \
  -H 'Authorization: Bearer leader-token' \
  -d '{
    "targetUserIds": ["10005"],
    "message": "该事项由王五负责，转交处理"
  }'
```

审批流转后需要检查：

```sql
-- 检查审批记录
SELECT node_name,
       operator_id,
       operator_name,
       action_type,
       message,
       operate_time
FROM wf_approval_record
WHERE business_id = 'LEAVE202605080001'
ORDER BY operate_time ASC;

-- 检查业务流程状态
SELECT business_id,
       instance_id,
       process_status,
       update_time
FROM wf_business_flow
WHERE business_id = 'LEAVE202605080001';
```

审批流转测试通过的判定标准：

1. 当前任务被正确处理。
2. 下一个节点任务正确生成。
3. 当前办理人或下一办理人符合预期。
4. 审批记录按时间顺序追加。
5. 业务状态正确同步。
6. 重复审批、越权审批会被拦截。
7. 页面待办和已办列表数据同步变化。

### 条件分支测试

条件分支测试用于验证流程变量是否能够正确驱动流程走向。典型场景包括请假天数、大额报销、采购金额、合同类型、部门类型等条件判断。

测试场景建议如下：

| 场景                  | 流程变量                  | 预期流向                         |
| --------------------- | ------------------------- | -------------------------------- |
| 请假 3 天以内         | `days = 3`                | 部门负责人审批后结束             |
| 请假超过 3 天         | `days = 5`                | 部门负责人审批后进入分管领导审批 |
| 报销金额小于 5000     | `amount = 3000`           | 财务专员审批                     |
| 报销金额大于等于 5000 | `amount = 8000`           | 财务经理审批                     |
| 变量缺失              | 缺少 `days` 或 `amount`   | 返回变量错误或走默认分支         |
| 多条件组合            | `amount` + `businessType` | 进入对应高风险审批节点           |

请假条件分支测试数据：

```json
{
  "businessId": "LEAVE202605080002",
  "businessType": "leave",
  "flowCode": "leave_process",
  "variable": {
    "days": 5,
    "deptId": "D001",
    "initiator": "10001",
    "leaderId": "10002",
    "directorId": "10003"
  }
}
```

报销条件分支测试数据：

```json
{
  "businessId": "EXP202605080001",
  "businessType": "expense",
  "flowCode": "expense_process",
  "variable": {
    "amount": 8000,
    "deptId": "D001",
    "initiator": "10001",
    "financeHandler": "10004",
    "financeManager": "10005"
  }
}
```

条件表达式单元测试示例：

文件位置：`src/test/java/io/github/atengk/workflow/WorkflowConditionTest.java`

下面测试类用于验证 SpEL 扩展 Bean 中的条件判断逻辑是否符合预期。

```java
package io.github.atengk.workflow;

import io.github.atengk.workflow.expression.WorkflowCondition;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

/**
 * 工作流条件表达式测试
 *
 * @author Ateng
 * @since 2026-05-08
 */
@Slf4j
class WorkflowConditionTest {

    private final WorkflowCondition workflowCondition = new WorkflowCondition();

    /**
     * 测试金额大于等于阈值
     */
    @Test
    void testAmountGe() {
        boolean result = workflowCondition.amountGe(new BigDecimal("8000"), new BigDecimal("5000"));
        Assertions.assertTrue(result, "金额8000应满足大于等于5000条件");
        log.info("金额条件测试通过，结果：{}", result);
    }

    /**
     * 测试请假天数大于阈值
     */
    @Test
    void testDaysGt() {
        boolean result = workflowCondition.daysGt(5, 3);
        Assertions.assertTrue(result, "请假5天应满足大于3天条件");
        log.info("请假天数条件测试通过，结果：{}", result);
    }

    /**
     * 测试高风险业务判断
     */
    @Test
    void testHighRiskBusiness() {
        boolean result = workflowCondition.highRiskBusiness("contract");
        Assertions.assertTrue(result, "contract 应识别为高风险业务");
        log.info("高风险业务条件测试通过，结果：{}", result);
    }

}
```

条件分支测试通过的判定标准：

1. 不同流程变量进入不同审批节点。
2. 条件表达式能正确处理数字、字符串和布尔值。
3. SpEL 表达式能正确调用 Spring Bean。
4. 变量缺失时有明确异常或默认分支。
5. 条件分支不会出现多个互斥分支同时命中。
6. 流程图中当前节点与预期流向一致。

### 权限边界测试

权限边界测试用于验证用户只能查看、处理和管理自己有权限的数据。工作流系统必须重点测试越权审批、越权查看、越权转办和管理员干预等场景。

权限测试场景建议如下：

| 场景             | 操作用户       | 预期结果         |
| ---------------- | -------------- | ---------------- |
| 办理人审批       | 当前任务办理人 | 审批成功         |
| 非办理人审批     | 普通无关用户   | 返回无权处理     |
| 发起人撤销       | 流程发起人     | 撤销成功         |
| 非发起人撤销     | 普通用户       | 返回无权撤销     |
| 管理员终止       | 流程管理员     | 终止成功         |
| 普通用户终止     | 普通用户       | 返回无权终止     |
| 查看本人发起流程 | 发起人         | 查看成功         |
| 查看他人无关流程 | 普通用户       | 返回无权查看     |
| 转办给禁用用户   | 当前办理人     | 返回目标用户无效 |

非办理人审批测试示例：

```bash
# 使用无权限用户 token 审批任务，预期返回无权处理
curl -X POST 'http://localhost:8080/workflow/tasks/1260200765054652416/pass' \
  -H 'Content-Type: application/json' \
  -H 'Authorization: Bearer no-permission-token' \
  -d '{
    "message": "尝试越权审批"
  }'
```

预期返回：

```json
{
  "code": 403,
  "message": "当前用户无权处理该任务",
  "data": null
}
```

权限边界测试检查点：

1. 审批接口必须校验任务办理权限。
2. 流程详情接口必须校验查看权限。
3. 流程图接口必须校验查看权限。
4. 撤销接口必须校验发起人或管理员权限。
5. 终止接口必须校验流程管理员权限。
6. 转办和委派目标用户必须校验有效性。
7. 管理员使用忽略权限能力时必须记录操作日志。

权限测试通过的判定标准：

1. 合法用户可以完成对应操作。
2. 非法用户无法通过直接调用接口绕过权限。
3. 前端按钮隐藏和后端权限校验一致。
4. 所有越权操作都有明确错误提示。
5. 越权访问不会返回敏感业务数据。
6. 操作日志中记录越权请求的关键信息。

## 部署与上线

部署与上线用于将 Warm-Flow 工作流能力发布到测试环境、预发环境和生产环境。上线前需要重点确认数据库脚本、流程定义、配置项、权限策略、日志监控和回滚方案。

上线过程建议按以下顺序执行：

```text
准备数据库脚本
  -> 发布应用配置
  -> 初始化 Warm-Flow 引擎表
  -> 初始化业务扩展表
  -> 导入流程定义
  -> 部署应用服务
  -> 执行上线验证流程
  -> 开放业务入口
```

### 数据库脚本发布

数据库脚本发布包括 Warm-Flow 引擎表脚本、业务扩展表脚本、索引脚本、初始化数据脚本和流程定义数据脚本。生产环境脚本必须经过测试环境和预发环境验证，不建议直接在生产环境首次执行未验证脚本。

脚本分类建议如下：

| 脚本类型   | 示例文件                              | 说明                         |
| ---------- | ------------------------------------- | ---------------------------- |
| 引擎表脚本 | `V1.0.0__warm_flow_schema.sql`        | Warm-Flow 官方初始化脚本     |
| 业务表脚本 | `V1.0.1__workflow_business_table.sql` | 业务流程绑定表、审批记录表   |
| 索引脚本   | `V1.0.2__workflow_index.sql`          | 高频查询字段索引             |
| 初始化数据 | `V1.0.3__workflow_init_data.sql`      | 流程状态、业务类型等基础数据 |
| 流程定义   | `V1.0.4__workflow_definition.sql`     | 默认流程定义数据或导入记录   |

推荐脚本目录：

```text
db/
├── mysql/
│   ├── V1.0.0__warm_flow_schema.sql
│   ├── V1.0.1__workflow_business_table.sql
│   ├── V1.0.2__workflow_index.sql
│   ├── V1.0.3__workflow_init_data.sql
│   └── V1.0.4__workflow_definition.sql
└── rollback/
    ├── R1.0.1__workflow_business_table_rollback.sql
    └── R1.0.2__workflow_index_rollback.sql
```

业务扩展表发布脚本示例：

```sql
-- 业务流程绑定表
CREATE TABLE IF NOT EXISTS wf_business_flow (
    id BIGINT NOT NULL COMMENT '主键ID',
    business_id VARCHAR(64) NOT NULL COMMENT '业务主键',
    business_code VARCHAR(64) DEFAULT NULL COMMENT '业务编号',
    business_type VARCHAR(64) NOT NULL COMMENT '业务类型',
    flow_code VARCHAR(64) NOT NULL COMMENT '流程编码',
    instance_id BIGINT DEFAULT NULL COMMENT '流程实例ID',
    process_status VARCHAR(32) NOT NULL COMMENT '流程状态',
    start_user_id VARCHAR(64) DEFAULT NULL COMMENT '发起人ID',
    start_time DATETIME DEFAULT NULL COMMENT '发起时间',
    finish_time DATETIME DEFAULT NULL COMMENT '完成时间',
    create_time DATETIME NOT NULL COMMENT '创建时间',
    update_time DATETIME DEFAULT NULL COMMENT '更新时间',
    deleted TINYINT NOT NULL DEFAULT 0 COMMENT '删除标识：0未删除，1已删除',
    PRIMARY KEY (id),
    KEY idx_business_id (business_id),
    KEY idx_instance_id (instance_id),
    KEY idx_flow_code (flow_code),
    KEY idx_business_type_status (business_type, process_status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='业务流程绑定表';

-- 业务审批记录表
CREATE TABLE IF NOT EXISTS wf_approval_record (
    id BIGINT NOT NULL COMMENT '主键ID',
    business_id VARCHAR(64) NOT NULL COMMENT '业务主键',
    business_type VARCHAR(64) NOT NULL COMMENT '业务类型',
    instance_id BIGINT DEFAULT NULL COMMENT '流程实例ID',
    task_id BIGINT DEFAULT NULL COMMENT '任务ID',
    node_code VARCHAR(64) DEFAULT NULL COMMENT '节点编码',
    node_name VARCHAR(128) DEFAULT NULL COMMENT '节点名称',
    operator_id VARCHAR(64) DEFAULT NULL COMMENT '操作人ID',
    operator_name VARCHAR(64) DEFAULT NULL COMMENT '操作人名称',
    action_type VARCHAR(32) NOT NULL COMMENT '审批动作',
    message VARCHAR(500) DEFAULT NULL COMMENT '审批意见',
    operate_time DATETIME NOT NULL COMMENT '操作时间',
    PRIMARY KEY (id),
    KEY idx_business_id (business_id),
    KEY idx_instance_id (instance_id),
    KEY idx_task_id (task_id),
    KEY idx_operator_id (operator_id),
    KEY idx_operate_time (operate_time)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='业务审批记录表';
```

上线前数据库检查 SQL：

```sql
-- 检查 Warm-Flow 引擎表
SHOW TABLES LIKE 'flow_%';

-- 检查业务流程扩展表
SHOW TABLES LIKE 'wf_%';

-- 检查流程绑定表索引
SHOW INDEX FROM wf_business_flow;

-- 检查审批记录表索引
SHOW INDEX FROM wf_approval_record;

-- 检查是否存在流程定义数据
SELECT COUNT(*) AS definition_count
FROM flow_definition;
```

数据库脚本发布注意事项：

1. 生产环境执行脚本前必须备份数据库。
2. DDL 脚本应评估锁表风险，避免业务高峰期执行。
3. Warm-Flow 官方升级脚本需要按版本顺序执行。
4. 业务表新增字段应优先允许为空，再逐步补数据和加约束。
5. 删除字段、删除表、清空数据必须提供回滚方案。
6. 流程定义发布后应锁定版本，避免直接覆盖运行中流程。

### 配置项整理

配置项整理用于确保不同环境的数据库、日志、流程定义导入、权限、消息通知和流程图地址配置正确。生产环境不建议开启自动导入流程定义，也不建议开启 MyBatis SQL 控制台输出。

生产环境配置建议如下：

文件位置：`src/main/resources/application-prod.yml`

下面配置用于生产环境部署，重点关闭开发调试项，并显式配置工作流相关参数。

```yaml
server:
  port: 8080

spring:
  application:
    name: workflow-boot

  datasource:
    driver-class-name: com.mysql.cj.jdbc.Driver
    url: jdbc:mysql://${MYSQL_HOST:127.0.0.1}:${MYSQL_PORT:3306}/${MYSQL_DATABASE:workflow_boot}?useUnicode=true&characterEncoding=utf8&serverTimezone=Asia/Shanghai&useSSL=false
    username: ${MYSQL_USERNAME}
    password: ${MYSQL_PASSWORD}
    hikari:
      # 最大连接数根据业务并发和数据库规格调整
      maximum-pool-size: 20
      # 最小空闲连接数
      minimum-idle: 5
      # 连接超时时间，单位毫秒
      connection-timeout: 30000

  jackson:
    date-format: yyyy-MM-dd HH:mm:ss
    time-zone: Asia/Shanghai

mybatis-plus:
  mapper-locations: classpath*:/mapper/**/*.xml
  configuration:
    # 生产环境不要使用 StdOutImpl 输出 SQL
    log-impl: org.apache.ibatis.logging.nologging.NoLoggingImpl
  global-config:
    db-config:
      id-type: assign_id
      logic-delete-field: deleted
      logic-delete-value: 1
      logic-not-delete-value: 0

workflow:
  # 默认流程编码，可按业务模块覆盖
  default-flow-code: ${WORKFLOW_DEFAULT_FLOW_CODE:leave_process}
  # 生产环境禁止启动时自动导入流程定义
  auto-import-definition: false
  # 流程图访问地址，按网关或服务地址配置
  chart-base-url: ${WORKFLOW_CHART_BASE_URL:/warm-flow-ui/index.html}
  # 管理员干预能力开关
  admin-intervention-enabled: ${WORKFLOW_ADMIN_INTERVENTION_ENABLED:false}

logging:
  level:
    root: info
    io.github.atengk: info
    com.warm.flow: info
```

环境变量建议：

| 变量名                                | 说明                   |
| ------------------------------------- | ---------------------- |
| `SPRING_PROFILES_ACTIVE`              | 当前环境，例如 `prod`  |
| `MYSQL_HOST`                          | MySQL 地址             |
| `MYSQL_PORT`                          | MySQL 端口             |
| `MYSQL_DATABASE`                      | 数据库名称             |
| `MYSQL_USERNAME`                      | 数据库用户名           |
| `MYSQL_PASSWORD`                      | 数据库密码             |
| `WORKFLOW_DEFAULT_FLOW_CODE`          | 默认流程编码           |
| `WORKFLOW_CHART_BASE_URL`             | 流程图访问地址         |
| `WORKFLOW_ADMIN_INTERVENTION_ENABLED` | 是否开启管理员干预能力 |

启动命令示例：

```bash
# 使用生产环境配置启动服务
java -jar workflow-boot.jar \
  --spring.profiles.active=prod \
  --MYSQL_HOST=10.0.0.10 \
  --MYSQL_PORT=3306 \
  --MYSQL_DATABASE=workflow_boot \
  --MYSQL_USERNAME=workflow_user \
  --MYSQL_PASSWORD='******'
```

Docker 启动示例：

```bash
# 使用 Docker 启动工作流服务
docker run -d \
  --name workflow-boot \
  -p 8080:8080 \
  -e SPRING_PROFILES_ACTIVE=prod \
  -e MYSQL_HOST=10.0.0.10 \
  -e MYSQL_PORT=3306 \
  -e MYSQL_DATABASE=workflow_boot \
  -e MYSQL_USERNAME=workflow_user \
  -e MYSQL_PASSWORD='******' \
  -e WORKFLOW_ADMIN_INTERVENTION_ENABLED=false \
  workflow-boot:1.0.0
```

配置项检查清单：

| 检查项           | 生产建议                  |
| ---------------- | ------------------------- |
| 自动导入流程定义 | 关闭                      |
| SQL 控制台输出   | 关闭                      |
| 日志级别         | `info` 或按模块精细配置   |
| 数据库账号       | 使用最小权限账号          |
| 管理员干预       | 默认关闭，按需开启        |
| 流程图地址       | 通过网关或统一域名访问    |
| Token 传递       | 避免长期 token 暴露在 URL |
| 时区             | 统一 `Asia/Shanghai`      |
| 字符集           | 统一 `utf8mb4`            |

### 上线验证流程

上线验证流程用于确认生产环境部署后，流程引擎、业务接口、前端页面和权限控制均可正常使用。验证应先在管理员或测试账号范围内完成，再逐步开放给真实业务用户。

上线验证步骤建议如下：

| 步骤 | 验证内容     | 预期结果                       |
| ---- | ------------ | ------------------------------ |
| 1    | 应用启动     | 服务正常启动，无 Bean 创建失败 |
| 2    | 数据库连接   | 数据源连接正常                 |
| 3    | 引擎表检查   | Warm-Flow 引擎表存在           |
| 4    | 业务表检查   | 绑定表、审批记录表存在         |
| 5    | 流程定义检查 | 流程定义已导入并发布           |
| 6    | 发起流程     | 测试业务单据可成功发起         |
| 7    | 待办查询     | 审批人可看到待办               |
| 8    | 审批通过     | 任务可正常流转                 |
| 9    | 条件分支     | 不同变量进入不同节点           |
| 10   | 审批记录     | 审批轨迹完整                   |
| 11   | 流程图       | 可查看实例流程图               |
| 12   | 权限边界     | 非办理人不能审批               |
| 13   | 日志监控     | 操作日志和异常日志正常         |
| 14   | 回滚预案     | 确认回滚脚本和镜像可用         |

上线健康检查接口建议：

```http
GET /actuator/health
```

预期返回：

```json
{
  "status": "UP"
}
```

上线后基础验证命令：

```bash
# 检查服务健康状态
curl -X GET 'http://localhost:8080/actuator/health'

# 检查待办接口是否可访问
curl -X GET 'http://localhost:8080/workflow/tasks/todo?pageNum=1&pageSize=10' \
  -H 'Authorization: Bearer test-token'

# 检查我的发起接口是否可访问
curl -X GET 'http://localhost:8080/workflow/instances/my-started?pageNum=1&pageSize=10' \
  -H 'Authorization: Bearer test-token'
```

上线验证业务闭环：

```text
测试账号创建业务单据
  -> 发起审批流程
  -> 审批人查看待办
  -> 审批人审批通过
  -> 根据条件进入下一节点或结束
  -> 查看审批记录
  -> 查看流程图
  -> 校验业务状态
```

上线后观察指标建议：

| 指标           | 说明                                   |
| -------------- | -------------------------------------- |
| 流程发起成功率 | 发起接口成功数量 / 总请求数量          |
| 审批成功率     | 审批动作成功数量 / 总审批请求数量      |
| 流程异常数量   | 流程定义、任务、变量、权限相关异常     |
| 待办生成数量   | 每日新增待办任务数量                   |
| 平均审批耗时   | 从任务创建到任务完成的平均耗时         |
| 接口响应时间   | 待办、审批、详情、流程图接口耗时       |
| 数据库慢 SQL   | 流程任务、审批记录、业务绑定表查询性能 |

上线回滚建议：

1. 应用回滚优先使用上一版本镜像或 Jar。
2. 数据库新增表通常不需要立即删除，避免二次风险。
3. 如果流程定义发布错误，应禁用错误版本并发布修正版本。
4. 已发起的异常流程实例应由流程管理员人工终止或迁移。
5. 回滚后必须检查业务单据状态和流程实例状态是否一致。
6. 回滚过程中的所有管理员操作都应记录操作日志。

上线完成后的最终确认清单：

| 确认项           | 结果 |
| ---------------- | ---- |
| 应用服务已启动   | 通过 |
| 数据库脚本已执行 | 通过 |
| 流程定义已发布   | 通过 |
| 流程发起成功     | 通过 |
| 审批流转成功     | 通过 |
| 条件分支正确     | 通过 |
| 权限控制有效     | 通过 |
| 审批记录完整     | 通过 |
| 流程图展示正常   | 通过 |
| 操作日志正常     | 通过 |
| 异常告警正常     | 通过 |
| 回滚方案已确认   | 通过 |