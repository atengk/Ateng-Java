# Spring Data JPA

Spring Data JPA 是 Spring Data 体系中面向关系型数据库持久化开发的模块，核心目标是降低 JPA Repository、查询方法、分页排序、事务集成等重复编码成本。它不是 JPA 规范本身，也不是 ORM 实现，而是在 JPA 与 Spring 之间提供更高层的 Repository 抽象。Spring Data JPA 官方定位是为 Jakarta Persistence API 提供 Repository 支持，简化访问 JPA 数据源的应用开发。([Home](https://docs.spring.io/spring-data/jpa/docs/3.1.8/reference/html/?utm_source=chatgpt.com))

## 技术概述

本部分用于说明 Spring Data JPA 在 Spring Boot 3 项目中的定位、它与 JPA、Hibernate 的关系，以及项目中选择或避免使用它的边界。先明确这些概念，可以避免把 Spring Data JPA、JPA 规范、Hibernate 实现混为一谈。

### Spring Data JPA 定位

Spring Data JPA 主要解决的是数据访问层的标准化开发问题。它通过 `Repository`、`JpaRepository`、方法命名查询、`@Query`、分页排序、Specification 等机制，把常见的 CRUD 与查询逻辑从手写 DAO 代码中抽象出来。

在 Spring Boot 3 项目中，典型分层如下：

```text
Controller
   ↓
Service
   ↓
Repository（Spring Data JPA）
   ↓
JPA Provider（通常是 Hibernate）
   ↓
Database
```

Spring Data JPA 通常负责以下内容：

| 能力              | 说明                                                     |
| ----------------- | -------------------------------------------------------- |
| Repository 抽象   | 通过接口继承 `JpaRepository` 获得基础 CRUD 能力          |
| 方法命名查询      | 根据方法名自动生成查询语句，例如 `findByUsername`        |
| JPQL / Native SQL | 通过 `@Query` 编写面向实体或原生 SQL 的查询              |
| 分页与排序        | 使用 `Pageable`、`Page`、`Sort` 统一处理分页排序         |
| 动态查询          | 使用 `Specification` 或 Criteria API 拼接动态条件        |
| 事务集成          | 与 Spring `@Transactional` 配合完成事务控制              |
| 审计能力          | 配合 JPA Auditing 处理创建时间、更新时间、创建人、更新人 |

Spring Data JPA 不直接负责数据库连接池管理、SQL 方言底层执行、对象状态跟踪等底层 ORM 工作。这些通常由 Hibernate、JDBC Driver、连接池和数据库自身完成。Spring Boot 的 `spring-boot-starter-data-jpa` 会带入 Hibernate、Spring Data JPA 和 Spring ORM 等关键依赖，用于快速启动 JPA 数据访问开发。([Home](https://docs.spring.io/spring-boot/reference/data/sql.html?utm_source=chatgpt.com))

### JPA 与 Hibernate 关系

JPA 是一套 Java 持久化规范，在 Spring Boot 3 中对应 Jakarta Persistence API。它定义了实体映射、生命周期、`EntityManager`、JPQL、事务集成等标准能力，但规范本身不负责具体执行。

Hibernate 是 JPA 的常用实现之一，负责真正的 ORM 运行时能力，例如：

| 层级            | 角色            | 示例                                               |
| --------------- | --------------- | -------------------------------------------------- |
| JPA             | 持久化规范      | `@Entity`、`@Id`、`EntityManager`、JPQL            |
| Hibernate       | JPA 实现        | SQL 生成、一级缓存、脏检查、懒加载、方言适配       |
| Spring Data JPA | Repository 抽象 | `JpaRepository`、方法命名查询、分页、Specification |
| Spring Boot     | 自动配置        | 数据源、Entity 扫描、事务管理器、JPA 参数绑定      |

在 Spring Boot 3 中，一般不需要手动编写传统 `persistence.xml`。Spring Boot 会基于自动配置包扫描实体类，常见的 `@Entity`、`@Embeddable`、`@MappedSuperclass` 都会被识别。([Home](https://docs.spring.io/spring-boot/reference/data/sql.html?utm_source=chatgpt.com))

需要注意的是，Spring Boot 3 基于 Jakarta EE 9+ 命名空间，实体注解应使用 `jakarta.persistence.*`，而不是旧版 `javax.persistence.*`。例如：

```java
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
```

### 适用场景与使用边界

Spring Data JPA 适合以实体模型为中心、表结构相对稳定、业务查询复杂度中等的后台管理系统、企业内部系统、单体服务或中小型微服务。

适合使用的场景：

| 场景               | 说明                                                 |
| ------------------ | ---------------------------------------------------- |
| 标准 CRUD 较多     | 例如用户、角色、部门、订单、字典、配置等基础数据维护 |
| 实体关系明确       | 表之间有清晰的一对多、多对一、一对一关系             |
| 业务更关注对象模型 | 希望围绕实体对象进行开发，而不是围绕 SQL 语句开发    |
| 分页列表较多       | 后台管理系统常见的分页、排序、条件查询               |
| 事务边界清晰       | 业务逻辑主要在 Service 层控制事务                    |
| 数据库兼容性有要求 | 希望减少直接绑定某一种数据库 SQL 语法                |

不太适合或需要谨慎使用的场景：

| 场景                          | 原因                                                         |
| ----------------------------- | ------------------------------------------------------------ |
| 报表型复杂 SQL                | 多表聚合、窗口函数、复杂统计更适合 MyBatis、JdbcTemplate 或专门的 SQL 层 |
| 高度依赖数据库特性            | 如大量存储过程、复杂 CTE、数据库专有函数                     |
| 超大批量写入                  | JPA 默认实体状态管理会带来额外内存和脏检查成本               |
| 查询结果不是实体模型          | 多表宽表 DTO、复杂投影场景需要额外设计                       |
| 对 SQL 完全可控要求高         | JPA 自动生成 SQL，调优时需要理解 Hibernate 行为              |
| 团队 SQL 能力强且偏过程式开发 | MyBatis-Plus 或 MyBatis 可能更直接                           |

实践中可以采用混合策略：常规实体 CRUD 使用 Spring Data JPA；复杂报表、批量同步、性能敏感 SQL 使用 JdbcTemplate、MyBatis 或数据库原生能力。这样既保留 JPA 的开发效率，又避免在复杂 SQL 场景中强行套用 ORM。

## 环境准备

本部分用于搭建 Spring Boot 3 + Spring Data JPA 的基础运行环境，包括 JDK、Maven 依赖、数据库连接和 JPA 配置。后续实体建模、Repository 开发、Service 事务控制都应基于这里的配置展开。

### Spring Boot 3 基础环境

Spring Boot 3 项目建议使用以下基础环境：

| 环境项      | 建议版本                 | 说明                                               |
| ----------- | ------------------------ | -------------------------------------------------- |
| JDK         | 17 或更高                | Spring Boot 3 的基础运行要求通常以 Java 17+ 为基线 |
| Spring Boot | 3.x                      | 使用 Jakarta 命名空间                              |
| Maven       | 3.8+                     | 用于依赖管理和项目构建                             |
| 数据库      | MySQL 8 / PostgreSQL 14+ | 示例以 MySQL 8 为主                                |
| IDE         | IntelliJ IDEA            | 推荐开启 Lombok 插件和注解处理                     |

推荐项目结构如下：

```text
spring-data-jpa-demo
├── pom.xml
└── src
    └── main
        ├── java
        │   └── io
        │       └── github
        │           └── atengk
        │               └── JpaApplication.java
        └── resources
            └── application.yml
```

基础启动类放在根包 `io.github.atengk` 下，便于 Spring Boot 自动扫描 Controller、Service、Repository、Entity 等组件。

文件位置：`src/main/java/io/github/atengk/JpaApplication.java`

```java
package io.github.atengk;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * Spring Data JPA 示例项目启动类
 *
 * @author Ateng
 * @since 2026-05-04
 */
@SpringBootApplication
public class JpaApplication {

    /**
     * 应用启动入口
     *
     * @param args 启动参数
     */
    public static void main(String[] args) {
        SpringApplication.run(JpaApplication.class, args);
    }

}
```

### Maven 依赖配置

Maven 依赖需要包含 Web、Spring Data JPA、数据库驱动、Lombok、Hutool 等基础依赖。`spring-boot-starter-data-jpa` 会提供 JPA、Hibernate、Spring Data JPA、Spring ORM 等核心能力。([Home](https://docs.spring.io/spring-boot/reference/data/sql.html?utm_source=chatgpt.com))

文件位置：`pom.xml`

```xml
<project xmlns="http://maven.apache.org/POM/4.0.0"
         xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
         xsi:schemaLocation="http://maven.apache.org/POM/4.0.0 https://maven.apache.org/xsd/maven-4.0.0.xsd">

    <modelVersion>4.0.0</modelVersion>

    <groupId>io.github.atengk</groupId>
    <artifactId>spring-data-jpa-demo</artifactId>
    <version>1.0.0</version>
    <name>spring-data-jpa-demo</name>
    <description>Spring Boot 3 Spring Data JPA 示例项目</description>

    <parent>
        <!-- Spring Boot 统一管理依赖版本 -->
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-starter-parent</artifactId>
        <version>3.3.6</version>
        <relativePath/>
    </parent>

    <properties>
        <!-- Spring Boot 3 建议使用 Java 17 或更高版本 -->
        <java.version>17</java.version>
        <!-- Hutool 工具类库版本 -->
        <hutool.version>5.8.34</hutool.version>
    </properties>

    <dependencies>
        <!-- Web 开发基础依赖，提供 Controller、JSON、Tomcat 等能力 -->
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-web</artifactId>
        </dependency>

        <!-- Spring Data JPA，包含 Repository 抽象、JPA 集成、Hibernate 等能力 -->
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-data-jpa</artifactId>
        </dependency>

        <!-- MySQL JDBC 驱动，用于连接 MySQL 8 数据库 -->
        <dependency>
            <groupId>com.mysql</groupId>
            <artifactId>mysql-connector-j</artifactId>
            <scope>runtime</scope>
        </dependency>

        <!-- Lombok，减少 Getter、Setter、构造方法等样板代码 -->
        <dependency>
            <groupId>org.projectlombok</groupId>
            <artifactId>lombok</artifactId>
            <optional>true</optional>
        </dependency>

        <!-- Hutool，提供字符串、日期、集合、对象、JSON 等常用工具类 -->
        <dependency>
            <groupId>cn.hutool</groupId>
            <artifactId>hutool-all</artifactId>
            <version>${hutool.version}</version>
        </dependency>

        <!-- 测试依赖，用于 Repository、Service、接口测试 -->
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-test</artifactId>
            <scope>test</scope>
        </dependency>

        <!-- JPA 测试依赖，可用于 Repository 层切片测试 -->
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-testcontainers</artifactId>
            <scope>test</scope>
        </dependency>
    </dependencies>

    <build>
        <plugins>
            <plugin>
                <!-- Spring Boot Maven 插件，用于打包和运行应用 -->
                <groupId>org.springframework.boot</groupId>
                <artifactId>spring-boot-maven-plugin</artifactId>
            </plugin>
        </plugins>
    </build>

</project>
```

如果项目只做 Repository 或 Service 层验证，可以暂时不引入 `spring-boot-starter-web`。如果要开发 REST API，则建议保留。

### 数据库连接配置

数据库连接配置放在 `application.yml` 中。示例以 MySQL 8 为准，生产环境建议把账号、密码、连接地址放到环境变量或配置中心中，避免直接写死在代码仓库。

文件位置：`src/main/resources/application.yml`

```yaml
server:
  # 应用服务端口
  port: 8080

spring:
  application:
    # 应用名称
    name: spring-data-jpa-demo

  datasource:
    # MySQL 连接地址，useSSL=false 仅用于本地开发示例
    url: jdbc:mysql://localhost:3306/jpa_demo?useUnicode=true&characterEncoding=utf8&serverTimezone=Asia/Shanghai&useSSL=false&allowPublicKeyRetrieval=true
    # 数据库用户名
    username: root
    # 数据库密码，生产环境建议改为环境变量注入
    password: 123456
    # MySQL 8 驱动类
    driver-class-name: com.mysql.cj.jdbc.Driver

    hikari:
      # 连接池名称
      pool-name: JpaHikariPool
      # 最大连接数，根据业务并发和数据库承载能力调整
      maximum-pool-size: 20
      # 最小空闲连接数
      minimum-idle: 5
      # 获取连接超时时间，单位毫秒
      connection-timeout: 30000
      # 空闲连接最大存活时间，单位毫秒
      idle-timeout: 600000
      # 连接最大生命周期，单位毫秒
      max-lifetime: 1800000
```

本地测试前需要先创建数据库：

```sql
-- 创建 Spring Data JPA 示例数据库
CREATE DATABASE IF NOT EXISTS jpa_demo
    DEFAULT CHARACTER SET utf8mb4
    COLLATE utf8mb4_0900_ai_ci;
```

执行方式示例：

```bash
# 使用 MySQL 客户端连接本地数据库
mysql -uroot -p

# 进入 MySQL 后执行建库 SQL
CREATE DATABASE IF NOT EXISTS jpa_demo
    DEFAULT CHARACTER SET utf8mb4
    COLLATE utf8mb4_0900_ai_ci;
```

### JPA 基础配置

JPA 配置主要控制 Hibernate 建表策略、SQL 日志、懒加载边界、数据库方言、格式化 SQL 等行为。Spring Boot 支持通过 `spring.jpa.*` 属性配置 JPA 和 Hibernate；对于 Hibernate 原生属性，可以通过 `spring.jpa.properties.*` 传递，前缀会在传入 Hibernate 时被剥离。([Home](https://docs.spring.io/spring-boot/reference/data/sql.html?utm_source=chatgpt.com))

文件位置：`src/main/resources/application.yml`

```yaml
spring:
  jpa:
    # 是否在控制台输出 SQL，开发环境可开启，生产环境建议关闭
    show-sql: false

    hibernate:
      # DDL 策略：
      # none：不处理表结构
      # validate：启动时校验表结构
      # update：根据实体增量更新表结构，开发环境可用，生产环境谨慎
      # create：启动时删除并重建表
      # create-drop：启动时建表，关闭时删表
      ddl-auto: update

    properties:
      hibernate:
        # 格式化 SQL，便于开发环境查看
        format_sql: true
        # 高亮 SQL，控制台支持时更易读
        highlight_sql: true
        # 指定 Hibernate 方言，一般 Spring Boot 可自动识别；复杂场景可显式配置
        dialect: org.hibernate.dialect.MySQLDialect
        jdbc:
          # 批量写入大小，后续批量新增、更新章节可结合使用
          batch_size: 100
        order_inserts: true
        order_updates: true

    # Web 项目中建议关闭 Open EntityManager in View，避免视图层触发懒加载 SQL
    open-in-view: false

logging:
  level:
    # 输出 Hibernate 执行 SQL，开发环境排查问题时开启
    org.hibernate.SQL: debug
    # 输出 SQL 参数绑定信息，日志较多，按需开启
    org.hibernate.orm.jdbc.bind: trace
```

`spring.jpa.hibernate.ddl-auto` 的常用值包括 `none`、`validate`、`update`、`create`、`create-drop`。Spring Boot 对嵌入式数据库和真实数据库有不同默认策略；真实数据库通常默认不自动建表，因此建议显式配置该参数。([Home](https://docs.spring.io/spring-boot/how-to/data-initialization.html?utm_source=chatgpt.com))

开发环境可以使用：

```yaml
spring:
  jpa:
    hibernate:
      # 开发环境允许实体变更后自动更新表结构
      ddl-auto: update
```

测试环境可以使用：

```yaml
spring:
  jpa:
    hibernate:
      # 测试环境启动时重建表，适合临时验证，不保留数据
      ddl-auto: create-drop
```

生产环境建议使用：

```yaml
spring:
  jpa:
    hibernate:
      # 生产环境只校验实体与表结构是否匹配，不自动改表
      ddl-auto: validate
```

生产环境更推荐使用 Flyway 或 Liquibase 管理数据库结构变更，而不是依赖 Hibernate 自动改表。Spring Boot 官方也建议数据库初始化机制保持单一，避免同时混用 Hibernate 自动建表、`schema.sql`、`data.sql`、Flyway 或 Liquibase。([Home](https://docs.spring.io/spring-boot/how-to/data-initialization.html?utm_source=chatgpt.com))

完成以上配置后，可以使用以下命令启动项目：

```bash
# 在项目根目录执行，启动 Spring Boot 应用
mvn spring-boot:run
```

启动后重点观察以下内容：

```text
Started JpaApplication
HikariPool-1 - Start completed
Initialized JPA EntityManagerFactory
```

如果启动失败，优先检查数据库地址、账号密码、数据库是否存在、MySQL 驱动是否正确、`ddl-auto` 是否符合当前环境。



## 实体建模

实体建模用于把数据库表结构映射为 Java 对象，是 Spring Data JPA 开发的基础。实体设计时需要同时考虑数据库字段、对象关系、主键生成方式、时间字段维护、枚举存储方式以及后续 Repository 查询的便利性。

### 实体类定义

JPA 实体类通常使用 `@Entity` 标识，使用 `@Table` 指定表名。实体类不建议使用 Lombok 的 `@Data`，因为 `@Data` 会自动生成 `toString`、`equals`、`hashCode`，在存在双向关联时容易触发循环引用或懒加载问题。推荐使用 `@Getter`、`@Setter`，并根据需要手动控制构造方法和业务方法。

一个基础实体通常包含以下特点：

| 要点              | 说明                                                   |
| ----------------- | ------------------------------------------------------ |
| `@Entity`         | 标识当前类为 JPA 实体                                  |
| `@Table`          | 指定数据库表名                                         |
| `@Id`             | 标识主键字段                                           |
| `@GeneratedValue` | 指定主键生成策略                                       |
| 无参构造方法      | JPA 创建实体对象时需要                                 |
| 非 final 类       | 避免影响 Hibernate 代理                                |
| 字段使用包装类型  | 例如 `Long`、`Integer`，避免基础类型默认值干扰业务判断 |

先定义一个通用实体基类，用于承载主键、创建时间、更新时间等公共字段。

文件位置：`src/main/java/io/github/atengk/common/entity/BaseEntity.java`

```java
package io.github.atengk.common.entity;

import jakarta.persistence.Column;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.MappedSuperclass;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import lombok.Getter;
import lombok.Setter;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * JPA 通用实体基类
 *
 * @author Ateng
 * @since 2026-05-04
 */
@Getter
@Setter
@MappedSuperclass
public abstract class BaseEntity implements Serializable {

    /**
     * 主键 ID
     */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id", nullable = false, updatable = false)
    private Long id;

    /**
     * 创建时间
     */
    @Column(name = "create_time", nullable = false, updatable = false)
    private LocalDateTime createTime;

    /**
     * 更新时间
     */
    @Column(name = "update_time", nullable = false)
    private LocalDateTime updateTime;

    /**
     * 数据持久化前自动填充时间
     */
    @PrePersist
    protected void prePersist() {
        LocalDateTime now = LocalDateTime.now();
        this.createTime = now;
        this.updateTime = now;
    }

    /**
     * 数据更新前自动刷新更新时间
     */
    @PreUpdate
    protected void preUpdate() {
        this.updateTime = LocalDateTime.now();
    }

}
```

`@MappedSuperclass` 表示该类不是独立的数据表，而是把字段映射能力提供给子类继承。后续实体类继承 `BaseEntity` 后，会自动拥有 `id`、`create_time`、`update_time` 字段。

下面定义一个用户实体，演示普通字段、枚举字段、布尔字段和时间字段的常见写法。

文件位置：`src/main/java/io/github/atengk/module/user/entity/UserEntity.java`

```java
package io.github.atengk.module.user.entity;

import io.github.atengk.common.entity.BaseEntity;
import io.github.atengk.module.user.enums.UserStatusEnum;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Enumerated;
import jakarta.persistence.EnumType;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

/**
 * 用户实体
 *
 * @author Ateng
 * @since 2026-05-04
 */
@Getter
@Setter
@Entity
@Table(name = "sys_user")
public class UserEntity extends BaseEntity {

    /**
     * 用户名
     */
    @Column(name = "username", nullable = false, length = 64, unique = true)
    private String username;

    /**
     * 用户昵称
     */
    @Column(name = "nickname", nullable = false, length = 64)
    private String nickname;

    /**
     * 手机号
     */
    @Column(name = "phone", length = 20)
    private String phone;

    /**
     * 邮箱
     */
    @Column(name = "email", length = 128)
    private String email;

    /**
     * 用户状态
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 32)
    private UserStatusEnum status;

    /**
     * 是否删除
     */
    @Column(name = "deleted", nullable = false)
    private Boolean deleted = Boolean.FALSE;

}
```

### 主键策略

主键策略决定实体新增时主键如何生成。Spring Data JPA 中主键通常使用 `@Id` 和 `@GeneratedValue` 配置。

常见策略如下：

| 策略                      | 说明                               | 适用场景                       |
| ------------------------- | ---------------------------------- | ------------------------------ |
| `GenerationType.IDENTITY` | 依赖数据库自增主键                 | MySQL 常用                     |
| `GenerationType.SEQUENCE` | 依赖数据库序列                     | PostgreSQL、Oracle 常用        |
| `GenerationType.AUTO`     | 由 JPA Provider 根据数据库自动选择 | 简单项目可用，但可控性较弱     |
| UUID                      | 使用 UUID 作为主键                 | 分布式系统、无需数据库自增场景 |
| 手动赋值                  | 应用层生成主键                     | 雪花 ID、业务编码等场景        |

MySQL 项目中最常见的是自增主键。

```java
@Id
@GeneratedValue(strategy = GenerationType.IDENTITY)
@Column(name = "id", nullable = false, updatable = false)
private Long id;
```

PostgreSQL 或 Oracle 中可以使用序列主键。

```java
@Id
@GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "user_seq")
@SequenceGenerator(name = "user_seq", sequenceName = "seq_sys_user", allocationSize = 1)
@Column(name = "id", nullable = false, updatable = false)
private Long id;
```

如果希望使用 UUID 主键，可以使用 Hibernate 6 提供的 UUID 生成能力。Spring Boot 3 默认使用 Hibernate 6，实体注解仍然使用 `jakarta.persistence.*`，Hibernate 扩展注解使用 `org.hibernate.annotations.*`。

文件位置：`src/main/java/io/github/atengk/module/file/entity/FileEntity.java`

```java
package io.github.atengk.module.file.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.UuidGenerator;

/**
 * 文件实体
 *
 * @author Ateng
 * @since 2026-05-04
 */
@Getter
@Setter
@Entity
@Table(name = "sys_file")
public class FileEntity {

    /**
     * 文件 ID
     */
    @Id
    @UuidGenerator
    @Column(name = "id", nullable = false, updatable = false, length = 36)
    private String id;

    /**
     * 原始文件名
     */
    @Column(name = "original_name", nullable = false, length = 255)
    private String originalName;

    /**
     * 文件访问地址
     */
    @Column(name = "url", nullable = false, length = 500)
    private String url;

}
```

一般建议：

| 数据库               | 推荐主键策略                    |
| -------------------- | ------------------------------- |
| MySQL                | `IDENTITY` 或应用层雪花 ID      |
| PostgreSQL           | `SEQUENCE` 或数据库自增         |
| Oracle               | `SEQUENCE`                      |
| 分布式写入           | UUID、雪花 ID、业务统一 ID 服务 |
| 需要排序和分页稳定性 | 数值型主键更友好                |

如果系统规模不大，后台管理类项目使用 `Long + IDENTITY` 即可。如果是多服务、多库、多节点写入，建议使用雪花 ID 或统一 ID 服务，避免依赖单库自增主键。

### 字段映射

字段映射主要通过 `@Column` 完成，用于控制数据库字段名、长度、是否为空、是否唯一、是否可更新等属性。

常见字段映射写法如下：

```java
@Column(name = "username", nullable = false, length = 64, unique = true)
private String username;

@Column(name = "age")
private Integer age;

@Column(name = "amount", precision = 18, scale = 2)
private BigDecimal amount;

@Column(name = "remark", length = 500)
private String remark;

@Column(name = "enabled", nullable = false)
private Boolean enabled = Boolean.TRUE;
```

常用 `@Column` 属性说明：

| 属性               | 说明                               |
| ------------------ | ---------------------------------- |
| `name`             | 数据库字段名                       |
| `nullable`         | 是否允许为空                       |
| `length`           | 字符串字段长度                     |
| `unique`           | 是否添加唯一约束                   |
| `insertable`       | 是否参与 insert                    |
| `updatable`        | 是否参与 update                    |
| `precision`        | 数值总位数                         |
| `scale`            | 小数位数                           |
| `columnDefinition` | 直接指定数据库字段定义，不建议滥用 |

对于大文本字段，可以使用 `@Lob`。

```java
@Lob
@Column(name = "content")
private String content;
```

对于金额字段，推荐使用 `BigDecimal`，不要使用 `Double` 或 `Float`。

```java
@Column(name = "pay_amount", nullable = false, precision = 18, scale = 2)
private BigDecimal payAmount;
```

对于逻辑删除字段，建议使用 `Boolean` 或固定数值状态。

```java
@Column(name = "deleted", nullable = false)
private Boolean deleted = Boolean.FALSE;
```

需要注意，JPA 不会自动理解“逻辑删除”的业务语义。是否过滤已删除数据，需要在 Repository 查询、Specification 条件、Hibernate Filter 或业务层中明确处理。

### 时间字段处理

时间字段通常包括创建时间、更新时间、删除时间、业务发生时间等。Spring Boot 3 项目中推荐使用 Java 8 时间类型，例如 `LocalDateTime`、`LocalDate`、`LocalTime`。

常见映射如下：

```java
@Column(name = "create_time", nullable = false, updatable = false)
private LocalDateTime createTime;

@Column(name = "update_time", nullable = false)
private LocalDateTime updateTime;

@Column(name = "birthday")
private LocalDate birthday;
```

时间字段维护有两种常见方式。

第一种方式是使用 JPA 生命周期回调，也就是前面 `BaseEntity` 中的 `@PrePersist` 和 `@PreUpdate`。这种方式简单直接，适合大部分项目。

```java
@PrePersist
protected void prePersist() {
    LocalDateTime now = LocalDateTime.now();
    this.createTime = now;
    this.updateTime = now;
}

@PreUpdate
protected void preUpdate() {
    this.updateTime = LocalDateTime.now();
}
```

第二种方式是使用 Spring Data JPA Auditing。它更适合需要统一处理创建时间、更新时间、创建人、更新人的项目。后续 `数据审计` 章节可以专门展开。

基础启用方式如下。

文件位置：`src/main/java/io/github/atengk/common/config/JpaAuditingConfig.java`

```java
package io.github.atengk.common.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;

/**
 * JPA 审计配置
 *
 * @author Ateng
 * @since 2026-05-04
 */
@Configuration
@EnableJpaAuditing
public class JpaAuditingConfig {
}
```

对应实体字段示例：

```java
@CreatedDate
@Column(name = "create_time", nullable = false, updatable = false)
private LocalDateTime createTime;

@LastModifiedDate
@Column(name = "update_time", nullable = false)
private LocalDateTime updateTime;
```

如果使用 Auditing，需要实体类或基类增加监听器。

```java
@EntityListeners(AuditingEntityListener.class)
```

时间字段建议统一使用数据库时区和应用时区。MySQL 连接地址中建议明确设置：

```yaml
spring:
  datasource:
    url: jdbc:mysql://localhost:3306/jpa_demo?useUnicode=true&characterEncoding=utf8&serverTimezone=Asia/Shanghai&useSSL=false&allowPublicKeyRetrieval=true
```

### 枚举字段映射

枚举字段推荐使用 `@Enumerated(EnumType.STRING)` 存储枚举名称，而不是使用 `EnumType.ORDINAL` 存储枚举下标。

文件位置：`src/main/java/io/github/atengk/module/user/enums/UserStatusEnum.java`

```java
package io.github.atengk.module.user.enums;

import lombok.Getter;

/**
 * 用户状态枚举
 *
 * @author Ateng
 * @since 2026-05-04
 */
@Getter
public enum UserStatusEnum {

    /**
     * 正常
     */
    ENABLED("正常"),

    /**
     * 禁用
     */
    DISABLED("禁用");

    private final String description;

    UserStatusEnum(String description) {
        this.description = description;
    }

}
```

实体中使用枚举字段：

```java
@Enumerated(EnumType.STRING)
@Column(name = "status", nullable = false, length = 32)
private UserStatusEnum status;
```

对应数据库字段建议使用 `varchar` 类型。

```sql
CREATE TABLE sys_user (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '主键 ID',
    username VARCHAR(64) NOT NULL UNIQUE COMMENT '用户名',
    nickname VARCHAR(64) NOT NULL COMMENT '用户昵称',
    status VARCHAR(32) NOT NULL COMMENT '用户状态',
    deleted BIT NOT NULL DEFAULT 0 COMMENT '是否删除',
    create_time DATETIME NOT NULL COMMENT '创建时间',
    update_time DATETIME NOT NULL COMMENT '更新时间'
) COMMENT '用户表';
```

不推荐使用 `EnumType.ORDINAL`：

```java
@Enumerated(EnumType.ORDINAL)
private UserStatusEnum status;
```

原因是枚举顺序一旦调整，数据库中保存的数字含义就可能改变，导致历史数据解释错误。

如果数据库中必须存储自定义编码，例如 `1` 表示正常、`2` 表示禁用，可以使用 `AttributeConverter`。

文件位置：`src/main/java/io/github/atengk/module/user/enums/UserTypeEnum.java`

```java
package io.github.atengk.module.user.enums;

import lombok.Getter;

/**
 * 用户类型枚举
 *
 * @author Ateng
 * @since 2026-05-04
 */
@Getter
public enum UserTypeEnum {

    /**
     * 普通用户
     */
    NORMAL(1, "普通用户"),

    /**
     * 管理员
     */
    ADMIN(2, "管理员");

    private final Integer code;

    private final String description;

    UserTypeEnum(Integer code, String description) {
        this.code = code;
        this.description = description;
    }

}
```

下面的转换器负责把枚举和数据库编码互相转换。

文件位置：`src/main/java/io/github/atengk/module/user/converter/UserTypeEnumConverter.java`

```java
package io.github.atengk.module.user.converter;

import cn.hutool.core.util.ObjectUtil;
import io.github.atengk.module.user.enums.UserTypeEnum;
import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

/**
 * 用户类型枚举转换器
 *
 * @author Ateng
 * @since 2026-05-04
 */
@Converter(autoApply = false)
public class UserTypeEnumConverter implements AttributeConverter<UserTypeEnum, Integer> {

    /**
     * 枚举转换为数据库字段
     *
     * @param attribute 枚举值
     * @return 数据库存储编码
     */
    @Override
    public Integer convertToDatabaseColumn(UserTypeEnum attribute) {
        return ObjectUtil.isNull(attribute) ? null : attribute.getCode();
    }

    /**
     * 数据库字段转换为枚举
     *
     * @param dbData 数据库存储编码
     * @return 枚举值
     */
    @Override
    public UserTypeEnum convertToEntityAttribute(Integer dbData) {
        if (ObjectUtil.isNull(dbData)) {
            return null;
        }
        for (UserTypeEnum item : UserTypeEnum.values()) {
            if (ObjectUtil.equal(item.getCode(), dbData)) {
                return item;
            }
        }
        throw new IllegalArgumentException("用户类型编码不合法：" + dbData);
    }

}
```

实体中使用转换器：

```java
@Convert(converter = UserTypeEnumConverter.class)
@Column(name = "user_type", nullable = false)
private UserTypeEnum userType;
```

## 表关系映射

表关系映射用于描述实体之间的一对一、一对多、多对一、多对多关系。JPA 可以通过对象关系自动维护外键关系，但实际项目中必须谨慎设计加载方式、级联范围和 JSON 返回结构，避免 N+1 查询、循环引用和误删除数据。

### 一对一关系

一对一关系表示一条主表数据对应一条从表数据，例如用户和用户详情。推荐由从表持有外键，也就是 `sys_user_profile.user_id` 指向 `sys_user.id`。

用户实体中维护一对一关系：

文件位置：`src/main/java/io/github/atengk/module/user/entity/UserEntity.java`

```java
package io.github.atengk.module.user.entity;

import io.github.atengk.common.entity.BaseEntity;
import io.github.atengk.module.user.enums.UserStatusEnum;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Enumerated;
import jakarta.persistence.EnumType;
import jakarta.persistence.FetchType;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

/**
 * 用户实体
 *
 * @author Ateng
 * @since 2026-05-04
 */
@Getter
@Setter
@Entity
@Table(name = "sys_user")
public class UserEntity extends BaseEntity {

    /**
     * 用户名
     */
    @Column(name = "username", nullable = false, length = 64, unique = true)
    private String username;

    /**
     * 用户昵称
     */
    @Column(name = "nickname", nullable = false, length = 64)
    private String nickname;

    /**
     * 用户状态
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 32)
    private UserStatusEnum status;

    /**
     * 用户详情
     */
    @OneToOne(mappedBy = "user", fetch = FetchType.LAZY, cascade = CascadeType.ALL, orphanRemoval = true)
    private UserProfileEntity profile;

    /**
     * 设置用户详情并维护双向关系
     *
     * @param profile 用户详情
     */
    public void setProfile(UserProfileEntity profile) {
        this.profile = profile;
        if (profile != null) {
            profile.setUser(this);
        }
    }

}
```

用户详情实体持有外键：

文件位置：`src/main/java/io/github/atengk/module/user/entity/UserProfileEntity.java`

```java
package io.github.atengk.module.user.entity;

import io.github.atengk.common.entity.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

/**
 * 用户详情实体
 *
 * @author Ateng
 * @since 2026-05-04
 */
@Getter
@Setter
@Entity
@Table(name = "sys_user_profile")
public class UserProfileEntity extends BaseEntity {

    /**
     * 真实姓名
     */
    @Column(name = "real_name", length = 64)
    private String realName;

    /**
     * 身份证号
     */
    @Column(name = "id_card", length = 32)
    private String idCard;

    /**
     * 联系地址
     */
    @Column(name = "address", length = 255)
    private String address;

    /**
     * 所属用户
     */
    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false, unique = true)
    private UserEntity user;

}
```

一对一关系中，`@JoinColumn(name = "user_id")` 表示当前表保存外键。`mappedBy = "user"` 表示用户实体不是关系维护方，真正维护关系的是 `UserProfileEntity.user` 字段。

### 一对多关系

一对多关系表示一条主表数据对应多条子表数据，例如订单和订单明细。实际数据库中通常由“多”的一方持有外键，也就是 `order_item.order_id` 指向 `order.id`。

订单实体维护订单明细集合：

文件位置：`src/main/java/io/github/atengk/module/order/entity/OrderEntity.java`

```java
package io.github.atengk.module.order.entity;

import io.github.atengk.common.entity.BaseEntity;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

/**
 * 订单实体
 *
 * @author Ateng
 * @since 2026-05-04
 */
@Getter
@Setter
@Entity
@Table(name = "biz_order")
public class OrderEntity extends BaseEntity {

    /**
     * 订单编号
     */
    @Column(name = "order_no", nullable = false, length = 64, unique = true)
    private String orderNo;

    /**
     * 订单金额
     */
    @Column(name = "total_amount", nullable = false, precision = 18, scale = 2)
    private BigDecimal totalAmount;

    /**
     * 订单明细列表
     */
    @OneToMany(mappedBy = "order", fetch = FetchType.LAZY, cascade = CascadeType.ALL, orphanRemoval = true)
    private List<OrderItemEntity> items = new ArrayList<>();

    /**
     * 添加订单明细
     *
     * @param item 订单明细
     */
    public void addItem(OrderItemEntity item) {
        this.items.add(item);
        item.setOrder(this);
    }

    /**
     * 移除订单明细
     *
     * @param item 订单明细
     */
    public void removeItem(OrderItemEntity item) {
        this.items.remove(item);
        item.setOrder(null);
    }

}
```

一对多关系中不建议只操作集合而不维护子对象的反向引用。`addItem` 和 `removeItem` 的作用是保证 Java 对象关系和数据库外键关系一致。

### 多对一关系

多对一关系是一对多关系的反向表达。订单明细属于某一个订单，所以订单明细到订单是多对一。

文件位置：`src/main/java/io/github/atengk/module/order/entity/OrderItemEntity.java`

```java
package io.github.atengk.module.order.entity;

import io.github.atengk.common.entity.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;

/**
 * 订单明细实体
 *
 * @author Ateng
 * @since 2026-05-04
 */
@Getter
@Setter
@Entity
@Table(name = "biz_order_item")
public class OrderItemEntity extends BaseEntity {

    /**
     * 商品名称
     */
    @Column(name = "product_name", nullable = false, length = 128)
    private String productName;

    /**
     * 商品数量
     */
    @Column(name = "quantity", nullable = false)
    private Integer quantity;

    /**
     * 商品单价
     */
    @Column(name = "price", nullable = false, precision = 18, scale = 2)
    private BigDecimal price;

    /**
     * 所属订单
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "order_id", nullable = false)
    private OrderEntity order;

}
```

多对一关系中，`@JoinColumn(name = "order_id")` 表示当前表 `biz_order_item` 中存在 `order_id` 外键字段。

默认情况下，`@ManyToOne` 的默认加载方式是 `EAGER`，容易在查询列表时引发额外 SQL。实际项目中建议显式指定为 `FetchType.LAZY`。

```java
@ManyToOne(fetch = FetchType.LAZY)
@JoinColumn(name = "order_id", nullable = false)
private OrderEntity order;
```

### 多对多关系

多对多关系表示两张表之间通过中间表关联，例如用户和角色。JPA 可以直接使用 `@ManyToMany` 映射中间表，但在真实业务中，如果中间表需要保存创建时间、授权人、数据范围等字段，更推荐把中间表建模为独立实体。

简单多对多关系示例如下。

文件位置：`src/main/java/io/github/atengk/module/role/entity/RoleEntity.java`

```java
package io.github.atengk.module.role.entity;

import io.github.atengk.common.entity.BaseEntity;
import io.github.atengk.module.user.entity.UserEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.ManyToMany;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

import java.util.HashSet;
import java.util.Set;

/**
 * 角色实体
 *
 * @author Ateng
 * @since 2026-05-04
 */
@Getter
@Setter
@Entity
@Table(name = "sys_role")
public class RoleEntity extends BaseEntity {

    /**
     * 角色编码
     */
    @Column(name = "role_code", nullable = false, length = 64, unique = true)
    private String roleCode;

    /**
     * 角色名称
     */
    @Column(name = "role_name", nullable = false, length = 64)
    private String roleName;

    /**
     * 拥有该角色的用户
     */
    @ManyToMany(mappedBy = "roles", fetch = FetchType.LAZY)
    private Set<UserEntity> users = new HashSet<>();

}
```

用户实体中维护用户和角色的中间表关系：

```java
@ManyToMany(fetch = FetchType.LAZY)
@JoinTable(
        name = "sys_user_role",
        joinColumns = @JoinColumn(name = "user_id"),
        inverseJoinColumns = @JoinColumn(name = "role_id")
)
private Set<RoleEntity> roles = new HashSet<>();
```

如果中间表需要扩展字段，推荐改成两个一对多和多对一关系。

文件位置：`src/main/java/io/github/atengk/module/user/entity/UserRoleEntity.java`

```java
package io.github.atengk.module.user.entity;

import io.github.atengk.common.entity.BaseEntity;
import io.github.atengk.module.role.entity.RoleEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

/**
 * 用户角色关联实体
 *
 * @author Ateng
 * @since 2026-05-04
 */
@Getter
@Setter
@Entity
@Table(name = "sys_user_role")
public class UserRoleEntity extends BaseEntity {

    /**
     * 用户
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private UserEntity user;

    /**
     * 角色
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "role_id", nullable = false)
    private RoleEntity role;

    /**
     * 授权人 ID
     */
    @Column(name = "grant_user_id")
    private Long grantUserId;

    /**
     * 授权时间
     */
    @Column(name = "grant_time", nullable = false)
    private LocalDateTime grantTime;

}
```

这种方式虽然代码多一些，但更适合真实项目，因为关联表可以独立保存业务字段，也方便后续分页、审计、逻辑删除和权限控制。

### 级联操作与孤儿删除

级联操作用于控制父实体保存、更新、删除时，是否同步操作关联实体。孤儿删除用于控制子实体从父集合中移除后，是否自动删除数据库记录。

常见级联类型如下：

| 级联类型              | 说明                       |
| --------------------- | -------------------------- |
| `CascadeType.PERSIST` | 保存父实体时同步保存子实体 |
| `CascadeType.MERGE`   | 合并父实体时同步合并子实体 |
| `CascadeType.REMOVE`  | 删除父实体时同步删除子实体 |
| `CascadeType.REFRESH` | 刷新父实体时同步刷新子实体 |
| `CascadeType.DETACH`  | 分离父实体时同步分离子实体 |
| `CascadeType.ALL`     | 包含以上所有级联操作       |

订单和订单明细通常适合使用级联和孤儿删除：

```java
@OneToMany(mappedBy = "order", fetch = FetchType.LAZY, cascade = CascadeType.ALL, orphanRemoval = true)
private List<OrderItemEntity> items = new ArrayList<>();
```

该配置表示：

| 配置                        | 效果                                                 |
| --------------------------- | ---------------------------------------------------- |
| `cascade = CascadeType.ALL` | 保存、更新、删除订单时联动处理订单明细               |
| `orphanRemoval = true`      | 明细从订单集合中移除后，数据库中的明细记录也会被删除 |
| `mappedBy = "order"`        | 外键由 `OrderItemEntity.order` 维护                  |
| `fetch = FetchType.LAZY`    | 查询订单时不立即加载明细集合                         |

适合开启级联删除的关系：

| 场景           | 是否适合                           |
| -------------- | ---------------------------------- |
| 订单和订单明细 | 适合，明细通常不能脱离订单独立存在 |
| 用户和用户详情 | 适合，详情通常依附用户存在         |
| 部门和用户     | 不建议，删除部门不应直接删除用户   |
| 用户和角色     | 不建议，删除用户不应删除角色       |
| 文章和评论     | 视业务而定，评论是否保留需要明确   |

使用级联时要特别注意，不要在共享实体上随意使用 `CascadeType.REMOVE` 或 `CascadeType.ALL`。例如用户和角色是共享关系，如果删除一个用户时级联删除角色，会导致其他用户关联的角色也被误删。

推荐规则：

| 关系类型                   | 推荐配置                                          |
| -------------------------- | ------------------------------------------------- |
| 强依赖子对象               | `cascade = CascadeType.ALL, orphanRemoval = true` |
| 弱关联对象                 | 不配置级联删除                                    |
| 字典、角色、组织等共享数据 | 禁止随意级联删除                                  |
| 多对多关系                 | 谨慎使用级联，通常不使用 `REMOVE`                 |
| 聚合根内部子表             | 可以使用级联和孤儿删除                            |

删除订单明细时，应通过维护父对象集合完成，而不是只把子对象外键置空。

```java
order.removeItem(orderItem);
orderRepository.save(order);
```

这样 `orphanRemoval = true` 才能识别该子对象已经脱离父集合，并自动执行删除。

实体关系映射的实践建议是：默认使用懒加载，谨慎使用级联，少用直接多对多，避免把实体对象直接返回给前端。接口层建议使用 DTO 或 VO 转换，防止懒加载异常、循环引用和隐式 SQL 查询。



## Repository 开发

Repository 层是 Spring Data JPA 的数据访问入口，主要负责实体的 CRUD、条件查询、分页排序、JPQL 查询和 Native SQL 查询。Spring Data JPA 支持通过方法名派生查询，也支持使用 `@Query` 声明 JPQL 或原生 SQL；当查询复杂度继续上升时，可以再引入 Specification 或 Criteria API。Spring Data JPA 官方文档也明确说明，查询可以通过方法名派生，也可以通过声明式字符串查询实现。([Home](https://docs.spring.io/spring-data/jpa/reference/jpa/query-methods.html?utm_source=chatgpt.com))

### JpaRepository 基础用法

`JpaRepository<T, ID>` 是 Spring Data JPA 最常用的 Repository 接口，它继承了 CRUD、分页、排序、Example 查询等能力，并额外提供了 `flush`、`saveAndFlush`、`saveAllAndFlush`、批量删除等 JPA 相关方法。([Home](https://docs.spring.io/spring-data/jpa/reference/4.1/api/java/org/springframework/data/jpa/repository/JpaRepository.html?utm_source=chatgpt.com))

常见基础方法如下：

| 方法                | 说明                         |
| ------------------- | ---------------------------- |
| `save(entity)`      | 新增或更新实体               |
| `saveAll(entities)` | 批量保存实体                 |
| `findById(id)`      | 根据主键查询                 |
| `findAll()`         | 查询全部数据                 |
| `findAll(Pageable)` | 分页查询                     |
| `findAll(Sort)`     | 排序查询                     |
| `existsById(id)`    | 判断主键是否存在             |
| `count()`           | 统计总数                     |
| `deleteById(id)`    | 根据主键删除                 |
| `delete(entity)`    | 删除指定实体                 |
| `flush()`           | 手动刷新持久化上下文到数据库 |

Repository 接口通常只需要继承 `JpaRepository`，不需要写实现类。

文件位置：`src/main/java/io/github/atengk/module/user/repository/UserRepository.java`

```java
package io.github.atengk.module.user.repository;

import io.github.atengk.module.user.entity.UserEntity;
import io.github.atengk.module.user.enums.UserStatusEnum;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

/**
 * 用户 Repository
 *
 * @author Ateng
 * @since 2026-05-04
 */
public interface UserRepository extends JpaRepository<UserEntity, Long> {

    Optional<UserEntity> findByUsernameAndDeletedFalse(String username);

    boolean existsByUsernameAndDeletedFalse(String username);

    List<UserEntity> findByStatusAndDeletedFalse(UserStatusEnum status);

}
```

基础调用示例一般放在 Service 层，不建议在 Controller 中直接调用 Repository。

文件位置：`src/main/java/io/github/atengk/module/user/service/impl/UserQueryDemoService.java`

```java
package io.github.atengk.module.user.service.impl;

import cn.hutool.core.util.ObjectUtil;
import io.github.atengk.module.user.entity.UserEntity;
import io.github.atengk.module.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.Optional;

/**
 * 用户查询示例服务
 *
 * @author Ateng
 * @since 2026-05-04
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class UserQueryDemoService {

    private final UserRepository userRepository;

    public UserEntity getUserById(Long id) {
        if (ObjectUtil.isNull(id)) {
            throw new IllegalArgumentException("用户ID不能为空");
        }

        Optional<UserEntity> optional = userRepository.findById(id);
        if (optional.isEmpty()) {
            log.warn("用户不存在，用户ID：{}", id);
            throw new IllegalArgumentException("用户不存在");
        }

        return optional.get();
    }

}
```

这里使用 `Optional<UserEntity>` 处理可能不存在的数据，避免直接调用 `getReferenceById` 后在访问属性时才触发实体不存在异常。`getReferenceById` 返回的是实体引用，适合只需要建立外键关联、不需要立即查询完整实体的场景。`JpaRepository` 官方 API 中也将 `getOne`、`getById` 标记为旧方式，建议使用 `getReferenceById`。([Home](https://docs.spring.io/spring-data/jpa/reference/4.1/api/java/org/springframework/data/jpa/repository/JpaRepository.html?utm_source=chatgpt.com))

### 方法命名查询

方法命名查询是 Spring Data JPA 的高频用法。它会根据 Repository 方法名解析查询条件，例如 `findByUsername`、`existsByEmail`、`countByStatus`。官方文档将这种方式称为 Query Methods，支持根据方法名称自动创建查询。([Home](https://docs.spring.io/spring-data/jpa/reference/repositories/query-methods-details.html?utm_source=chatgpt.com))

常见方法命名规则如下：

| 方法关键词     | 示例                              | 说明         |
| -------------- | --------------------------------- | ------------ |
| `findBy`       | `findByUsername`                  | 根据字段查询 |
| `existsBy`     | `existsByUsername`                | 判断是否存在 |
| `countBy`      | `countByStatus`                   | 统计数量     |
| `deleteBy`     | `deleteByUsername`                | 根据条件删除 |
| `And`          | `findByUsernameAndDeletedFalse`   | 多条件与     |
| `Or`           | `findByPhoneOrEmail`              | 多条件或     |
| `Containing`   | `findByNicknameContaining`        | like 查询    |
| `StartingWith` | `findByUsernameStartingWith`      | 前缀匹配     |
| `EndingWith`   | `findByEmailEndingWith`           | 后缀匹配     |
| `In`           | `findByIdIn`                      | in 查询      |
| `Between`      | `findByCreateTimeBetween`         | 范围查询     |
| `OrderBy`      | `findByDeletedFalseOrderByIdDesc` | 固定排序     |

在用户 Repository 中补充常用方法命名查询。

文件位置：`src/main/java/io/github/atengk/module/user/repository/UserRepository.java`

```java
package io.github.atengk.module.user.repository;

import io.github.atengk.module.user.entity.UserEntity;
import io.github.atengk.module.user.enums.UserStatusEnum;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

/**
 * 用户 Repository
 *
 * @author Ateng
 * @since 2026-05-04
 */
public interface UserRepository extends JpaRepository<UserEntity, Long> {

    Optional<UserEntity> findByUsernameAndDeletedFalse(String username);

    boolean existsByUsernameAndDeletedFalse(String username);

    boolean existsByPhoneAndDeletedFalse(String phone);

    long countByStatusAndDeletedFalse(UserStatusEnum status);

    List<UserEntity> findByIdInAndDeletedFalse(Collection<Long> ids);

    List<UserEntity> findByStatusAndDeletedFalseOrderByIdDesc(UserStatusEnum status);

    Page<UserEntity> findByNicknameContainingAndDeletedFalse(String nickname, Pageable pageable);

    List<UserEntity> findByCreateTimeBetweenAndDeletedFalse(LocalDateTime startTime, LocalDateTime endTime);

}
```

方法命名查询适合字段较少、条件明确、查询逻辑简单的场景。如果方法名变得过长，例如 `findByUsernameContainingAndPhoneContainingAndStatusAndCreateTimeBetweenAndDeletedFalseOrderByIdDesc`，说明查询已经不适合继续使用方法命名，应改用 JPQL、Specification 或 Criteria API。

### JPQL 查询

JPQL 是面向实体对象的查询语言，查询语句中使用的是实体类名和实体属性名，而不是数据库表名和字段名。Spring Data JPA 可以通过 `@Query` 直接在 Repository 方法上声明 JPQL 查询；官方文档也说明，`@Query` 声明的查询优先级高于命名查询。([Home](https://docs.spring.io/spring-data/jpa/reference/jpa/query-methods.html?utm_source=chatgpt.com))

JPQL 查询适合以下场景：

| 场景               | 说明                                 |
| ------------------ | ------------------------------------ |
| 方法名过长         | 查询条件较多，方法命名不再清晰       |
| 需要自定义查询字段 | 查询部分字段或聚合结果               |
| 需要关联查询       | 使用 `join`、`left join`             |
| 需要更新或删除     | 使用 `@Modifying` 配合 update/delete |
| 需要 DTO 投影      | 查询结果直接构造成 DTO               |

先定义一个用户列表 VO，用于接收 JPQL 投影结果。

文件位置：`src/main/java/io/github/atengk/module/user/vo/UserPageVO.java`

```java
package io.github.atengk.module.user.vo;

import io.github.atengk.module.user.enums.UserStatusEnum;

import java.time.LocalDateTime;

/**
 * 用户分页展示 VO
 *
 * @author Ateng
 * @since 2026-05-04
 */
public record UserPageVO(
        Long id,
        String username,
        String nickname,
        String phone,
        String email,
        UserStatusEnum status,
        LocalDateTime createTime
) {
}
```

下面的 Repository 方法演示 JPQL 条件查询、DTO 投影、更新语句和逻辑删除语句。

文件位置：`src/main/java/io/github/atengk/module/user/repository/UserRepository.java`

```java
package io.github.atengk.module.user.repository;

import io.github.atengk.module.user.entity.UserEntity;
import io.github.atengk.module.user.enums.UserStatusEnum;
import io.github.atengk.module.user.vo.UserPageVO;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;

import java.time.LocalDateTime;
import java.util.Optional;

/**
 * 用户 Repository
 *
 * @author Ateng
 * @since 2026-05-04
 */
public interface UserRepository extends JpaRepository<UserEntity, Long> {

    Optional<UserEntity> findByUsernameAndDeletedFalse(String username);

    boolean existsByUsernameAndDeletedFalse(String username);

    @Query("""
            select new io.github.atengk.module.user.vo.UserPageVO(
                u.id,
                u.username,
                u.nickname,
                u.phone,
                u.email,
                u.status,
                u.createTime
            )
            from UserEntity u
            where u.deleted = false
              and (:keyword is null or u.username like concat('%', :keyword, '%')
                   or u.nickname like concat('%', :keyword, '%'))
              and (:status is null or u.status = :status)
            """)
    Page<UserPageVO> pageUser(String keyword, UserStatusEnum status, Pageable pageable);

    @Query("""
            select u
            from UserEntity u
            where u.deleted = false
              and u.username = :username
            """)
    Optional<UserEntity> queryByUsername(String username);

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("""
            update UserEntity u
            set u.status = :status,
                u.updateTime = :updateTime
            where u.id = :id
              and u.deleted = false
            """)
    int updateStatus(Long id, UserStatusEnum status, LocalDateTime updateTime);

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("""
            update UserEntity u
            set u.deleted = true,
                u.updateTime = :updateTime
            where u.id = :id
              and u.deleted = false
            """)
    int logicalDeleteById(Long id, LocalDateTime updateTime);

}
```

`@Modifying` 用于标识当前查询是更新或删除语句，而不是普通 select 查询。`clearAutomatically = true` 可以在执行更新后清理持久化上下文，降低实体状态与数据库数据不一致的风险。

JPQL 中使用的是 `UserEntity`、`username`、`nickname`、`createTime` 这些实体和属性名称，不是 `sys_user`、`user_name`、`create_time` 这类数据库对象名称。

### Native SQL 查询

Native SQL 是直接面向数据库表和字段的原生 SQL 查询。它适合复杂报表、数据库函数、窗口函数、复杂联表、性能调优 SQL 等场景。Spring Data JPA 支持 `@Query(nativeQuery = true)`，新版本文档中也提供了 `@NativeQuery` 作为原生查询的组合注解；复杂分页原生查询通常需要额外提供 `countQuery`。([Home](https://docs.spring.io/spring-data/jpa/reference/3.5/jpa/query-methods.html?utm_source=chatgpt.com))

Native SQL 示例：

文件位置：`src/main/java/io/github/atengk/module/user/repository/UserRepository.java`

```java
package io.github.atengk.module.user.repository;

import io.github.atengk.module.user.entity.UserEntity;
import io.github.atengk.module.user.enums.UserStatusEnum;
import io.github.atengk.module.user.vo.UserPageVO;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

/**
 * 用户 Repository
 *
 * @author Ateng
 * @since 2026-05-04
 */
public interface UserRepository extends JpaRepository<UserEntity, Long> {

    @Query(
            value = """
                    select *
                    from sys_user
                    where deleted = false
                      and username = :username
                    limit 1
                    """,
            nativeQuery = true
    )
    UserEntity nativeFindByUsername(String username);

    @Query(
            value = """
                    select *
                    from sys_user
                    where deleted = false
                      and (:keyword is null or username like concat('%', :keyword, '%')
                           or nickname like concat('%', :keyword, '%'))
                      and (:status is null or status = :status)
                    order by id desc
                    """,
            countQuery = """
                    select count(1)
                    from sys_user
                    where deleted = false
                      and (:keyword is null or username like concat('%', :keyword, '%')
                           or nickname like concat('%', :keyword, '%'))
                      and (:status is null or status = :status)
                    """,
            nativeQuery = true
    )
    Page<UserEntity> nativePageUser(String keyword, String status, Pageable pageable);

}
```

Native SQL 中使用的是数据库表名和字段名，例如 `sys_user`、`deleted`、`username`、`nickname`。如果返回实体对象，查询结果字段需要能映射到实体字段，否则容易出现字段缺失或类型转换问题。

对于分页 Native SQL，建议显式写 `countQuery`。简单查询 Spring Data JPA 可以尝试自动处理分页和排序，但复杂 SQL 通常需要手动声明统计 SQL，否则可能出现 count 解析失败或分页结果异常。([Home](https://docs.spring.io/spring-data/jpa/reference/3.5/jpa/query-methods.html?utm_source=chatgpt.com))

Native SQL 不建议滥用。一般规则是：普通实体查询优先用方法命名、JPQL 或 Specification；复杂统计、性能敏感 SQL、数据库特性 SQL 再使用 Native SQL。

### 分页与排序

分页和排序是后台管理系统最常见的数据访问能力。Spring Data 使用 `Pageable` 表示分页请求，使用 `Page<T>` 表示分页结果，使用 `Sort` 表示排序规则。官方文档说明，排序可以通过 `PageRequest` 或直接使用 `Sort` 完成，排序属性需要能匹配领域模型属性或查询别名。([Home](https://docs.spring.io/spring-data/jpa/reference/jpa/query-methods.html?utm_source=chatgpt.com))

分页查询请求对象如下。

文件位置：`src/main/java/io/github/atengk/module/user/dto/UserPageQuery.java`

```java
package io.github.atengk.module.user.dto;

import io.github.atengk.module.user.enums.UserStatusEnum;

/**
 * 用户分页查询参数
 *
 * @author Ateng
 * @since 2026-05-04
 */
public record UserPageQuery(
        String keyword,
        UserStatusEnum status,
        Integer pageNum,
        Integer pageSize
) {
}
```

分页工具类用于统一处理页码、每页数量和排序字段。

文件位置：`src/main/java/io/github/atengk/common/util/PageUtil.java`

```java
package io.github.atengk.common.util;

import cn.hutool.core.util.ObjectUtil;
import cn.hutool.core.util.StrUtil;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;

/**
 * 分页工具类
 *
 * @author Ateng
 * @since 2026-05-04
 */
public class PageUtil {

    private static final int DEFAULT_PAGE_NUM = 1;

    private static final int DEFAULT_PAGE_SIZE = 10;

    private static final int MAX_PAGE_SIZE = 200;

    private PageUtil() {
    }

    public static Pageable buildPageable(Integer pageNum, Integer pageSize) {
        int current = ObjectUtil.defaultIfNull(pageNum, DEFAULT_PAGE_NUM);
        int size = ObjectUtil.defaultIfNull(pageSize, DEFAULT_PAGE_SIZE);

        current = Math.max(current, DEFAULT_PAGE_NUM);
        size = Math.min(Math.max(size, 1), MAX_PAGE_SIZE);

        return PageRequest.of(current - 1, size, Sort.by(Sort.Direction.DESC, "id"));
    }

    public static Pageable buildPageable(Integer pageNum, Integer pageSize, String sortField, Sort.Direction direction) {
        int current = ObjectUtil.defaultIfNull(pageNum, DEFAULT_PAGE_NUM);
        int size = ObjectUtil.defaultIfNull(pageSize, DEFAULT_PAGE_SIZE);
        String field = StrUtil.blankToDefault(sortField, "id");
        Sort.Direction sortDirection = ObjectUtil.defaultIfNull(direction, Sort.Direction.DESC);

        current = Math.max(current, DEFAULT_PAGE_NUM);
        size = Math.min(Math.max(size, 1), MAX_PAGE_SIZE);

        return PageRequest.of(current - 1, size, Sort.by(sortDirection, field));
    }

}
```

分页结果封装对象如下。

文件位置：`src/main/java/io/github/atengk/common/model/PageResult.java`

```java
package io.github.atengk.common.model;

import org.springframework.data.domain.Page;

import java.util.List;

/**
 * 分页结果对象
 *
 * @author Ateng
 * @since 2026-05-04
 */
public record PageResult<T>(
        List<T> records,
        long total,
        int pageNum,
        int pageSize,
        int totalPages
) {

    public static <T> PageResult<T> of(Page<T> page) {
        return new PageResult<>(
                page.getContent(),
                page.getTotalElements(),
                page.getNumber() + 1,
                page.getSize(),
                page.getTotalPages()
        );
    }

}
```

Service 层调用分页查询时，应把前端页码从 `1` 起转换为 Spring Data 的 `0` 起页码。

```java
Pageable pageable = PageUtil.buildPageable(query.pageNum(), query.pageSize());
Page<UserPageVO> page = userRepository.pageUser(query.keyword(), query.status(), pageable);
PageResult<UserPageVO> result = PageResult.of(page);
```

排序字段需要谨慎处理，不建议直接让前端传任意字段参与排序。对于公开接口，可以使用白名单字段映射，避免非法字段导致异常，也避免把实体内部字段暴露给前端。

## Service 业务开发

Service 层负责业务编排、事务控制、参数校验、异常处理、DTO 到实体转换以及多个 Repository 的协作。Repository 应保持数据访问职责，不建议把复杂业务判断写入 Repository。

Spring Data JPA 官方文档说明，Repository 继承自 `CrudRepository` 的方法默认有事务配置，读操作通常是只读事务，其他操作使用普通事务；如果需要定义多个 Repository 调用组成的业务边界，通常应在 Service 或 Facade 层使用 `@Transactional`。([Home](https://docs.spring.io/spring-data/jpa/reference/jpa/transactions.html?utm_source=chatgpt.com))

### 基础 CRUD 封装

基础 CRUD 不建议让 Controller 直接操作实体。推荐使用 DTO 接收入参，Service 负责校验和转换，VO 返回给前端。

先定义新增和修改请求对象。

文件位置：`src/main/java/io/github/atengk/module/user/dto/UserCreateRequest.java`

```java
package io.github.atengk.module.user.dto;

import io.github.atengk.module.user.enums.UserStatusEnum;

/**
 * 用户新增请求
 *
 * @author Ateng
 * @since 2026-05-04
 */
public record UserCreateRequest(
        String username,
        String nickname,
        String phone,
        String email,
        UserStatusEnum status
) {
}
```

文件位置：`src/main/java/io/github/atengk/module/user/dto/UserUpdateRequest.java`

```java
package io.github.atengk.module.user.dto;

import io.github.atengk.module.user.enums.UserStatusEnum;

/**
 * 用户修改请求
 *
 * @author Ateng
 * @since 2026-05-04
 */
public record UserUpdateRequest(
        Long id,
        String nickname,
        String phone,
        String email,
        UserStatusEnum status
) {
}
```

用户详情 VO 如下。

文件位置：`src/main/java/io/github/atengk/module/user/vo/UserDetailVO.java`

```java
package io.github.atengk.module.user.vo;

import io.github.atengk.module.user.enums.UserStatusEnum;

import java.time.LocalDateTime;

/**
 * 用户详情 VO
 *
 * @author Ateng
 * @since 2026-05-04
 */
public record UserDetailVO(
        Long id,
        String username,
        String nickname,
        String phone,
        String email,
        UserStatusEnum status,
        LocalDateTime createTime,
        LocalDateTime updateTime
) {
}
```

Service 接口定义如下。

文件位置：`src/main/java/io/github/atengk/module/user/service/UserService.java`

```java
package io.github.atengk.module.user.service;

import io.github.atengk.common.model.PageResult;
import io.github.atengk.module.user.dto.UserCreateRequest;
import io.github.atengk.module.user.dto.UserPageQuery;
import io.github.atengk.module.user.dto.UserUpdateRequest;
import io.github.atengk.module.user.vo.UserDetailVO;
import io.github.atengk.module.user.vo.UserPageVO;

import java.util.List;

/**
 * 用户服务接口
 *
 * @author Ateng
 * @since 2026-05-04
 */
public interface UserService {

    Long create(UserCreateRequest request);

    void update(UserUpdateRequest request);

    void delete(Long id);

    UserDetailVO getDetail(Long id);

    PageResult<UserPageVO> page(UserPageQuery query);

    List<Long> batchCreate(List<UserCreateRequest> requests);

}
```

Service 实现类完成基础 CRUD 封装。

文件位置：`src/main/java/io/github/atengk/module/user/service/impl/UserServiceImpl.java`

```java
package io.github.atengk.module.user.service.impl;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.ObjectUtil;
import cn.hutool.core.util.StrUtil;
import io.github.atengk.common.model.PageResult;
import io.github.atengk.common.util.PageUtil;
import io.github.atengk.module.user.dto.UserCreateRequest;
import io.github.atengk.module.user.dto.UserPageQuery;
import io.github.atengk.module.user.dto.UserUpdateRequest;
import io.github.atengk.module.user.entity.UserEntity;
import io.github.atengk.module.user.enums.UserStatusEnum;
import io.github.atengk.module.user.repository.UserRepository;
import io.github.atengk.module.user.service.UserService;
import io.github.atengk.module.user.vo.UserDetailVO;
import io.github.atengk.module.user.vo.UserPageVO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * 用户服务实现
 *
 * @author Ateng
 * @since 2026-05-04
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class UserServiceImpl implements UserService {

    private final UserRepository userRepository;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long create(UserCreateRequest request) {
        checkCreateRequest(request);

        if (userRepository.existsByUsernameAndDeletedFalse(request.username())) {
            throw new IllegalArgumentException("用户名已存在");
        }

        UserEntity entity = new UserEntity();
        entity.setUsername(request.username());
        entity.setNickname(request.nickname());
        entity.setPhone(request.phone());
        entity.setEmail(request.email());
        entity.setStatus(ObjectUtil.defaultIfNull(request.status(), UserStatusEnum.ENABLED));
        entity.setDeleted(Boolean.FALSE);

        UserEntity saved = userRepository.save(entity);
        log.info("新增用户成功，用户ID：{}，用户名：{}", saved.getId(), saved.getUsername());
        return saved.getId();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void update(UserUpdateRequest request) {
        checkUpdateRequest(request);

        UserEntity entity = userRepository.findById(request.id())
                .filter(item -> !Boolean.TRUE.equals(item.getDeleted()))
                .orElseThrow(() -> new IllegalArgumentException("用户不存在"));

        entity.setNickname(request.nickname());
        entity.setPhone(request.phone());
        entity.setEmail(request.email());
        entity.setStatus(ObjectUtil.defaultIfNull(request.status(), entity.getStatus()));

        userRepository.save(entity);
        log.info("修改用户成功，用户ID：{}", entity.getId());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void delete(Long id) {
        if (ObjectUtil.isNull(id)) {
            throw new IllegalArgumentException("用户ID不能为空");
        }

        UserEntity entity = userRepository.findById(id)
                .filter(item -> !Boolean.TRUE.equals(item.getDeleted()))
                .orElseThrow(() -> new IllegalArgumentException("用户不存在"));

        entity.setDeleted(Boolean.TRUE);
        userRepository.save(entity);
        log.info("逻辑删除用户成功，用户ID：{}", id);
    }

    @Override
    @Transactional(readOnly = true)
    public UserDetailVO getDetail(Long id) {
        if (ObjectUtil.isNull(id)) {
            throw new IllegalArgumentException("用户ID不能为空");
        }

        UserEntity entity = userRepository.findById(id)
                .filter(item -> !Boolean.TRUE.equals(item.getDeleted()))
                .orElseThrow(() -> new IllegalArgumentException("用户不存在"));

        return toDetailVO(entity);
    }

    @Override
    @Transactional(readOnly = true)
    public PageResult<UserPageVO> page(UserPageQuery query) {
        Pageable pageable = PageUtil.buildPageable(query.pageNum(), query.pageSize());
        Page<UserPageVO> page = userRepository.pageUser(
                StrUtil.blankToNull(query.keyword()),
                query.status(),
                pageable
        );
        return PageResult.of(page);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public List<Long> batchCreate(List<UserCreateRequest> requests) {
        if (CollUtil.isEmpty(requests)) {
            throw new IllegalArgumentException("用户列表不能为空");
        }

        List<UserEntity> entities = requests.stream()
                .peek(this::checkCreateRequest)
                .map(this::toEntity)
                .toList();

        List<UserEntity> savedList = userRepository.saveAll(entities);
        log.info("批量新增用户成功，数量：{}", savedList.size());

        return savedList.stream()
                .map(UserEntity::getId)
                .toList();
    }

    private void checkCreateRequest(UserCreateRequest request) {
        if (ObjectUtil.isNull(request)) {
            throw new IllegalArgumentException("用户新增参数不能为空");
        }
        if (StrUtil.isBlank(request.username())) {
            throw new IllegalArgumentException("用户名不能为空");
        }
        if (StrUtil.isBlank(request.nickname())) {
            throw new IllegalArgumentException("用户昵称不能为空");
        }
    }

    private void checkUpdateRequest(UserUpdateRequest request) {
        if (ObjectUtil.isNull(request)) {
            throw new IllegalArgumentException("用户修改参数不能为空");
        }
        if (ObjectUtil.isNull(request.id())) {
            throw new IllegalArgumentException("用户ID不能为空");
        }
        if (StrUtil.isBlank(request.nickname())) {
            throw new IllegalArgumentException("用户昵称不能为空");
        }
    }

    private UserEntity toEntity(UserCreateRequest request) {
        UserEntity entity = new UserEntity();
        entity.setUsername(request.username());
        entity.setNickname(request.nickname());
        entity.setPhone(request.phone());
        entity.setEmail(request.email());
        entity.setStatus(ObjectUtil.defaultIfNull(request.status(), UserStatusEnum.ENABLED));
        entity.setDeleted(Boolean.FALSE);
        return entity;
    }

    private UserDetailVO toDetailVO(UserEntity entity) {
        return new UserDetailVO(
                entity.getId(),
                entity.getUsername(),
                entity.getNickname(),
                entity.getPhone(),
                entity.getEmail(),
                entity.getStatus(),
                entity.getCreateTime(),
                entity.getUpdateTime()
        );
    }

}
```

这里的 CRUD 封装有几个关键点：新增前做唯一性校验；修改和删除前先判断数据是否存在；查询详情和分页使用只读事务；删除默认走逻辑删除；实体对象不直接返回给前端。

### 事务控制

Spring Data JPA 项目中，事务边界应优先放在 Service 层，而不是 Controller 层或 Repository 层。Repository 的基础方法虽然已有默认事务配置，但多个 Repository 调用、业务校验、状态变更、批量处理等场景仍需要在 Service 层明确声明事务边界。([Home](https://docs.spring.io/spring-data/jpa/reference/jpa/transactions.html?utm_source=chatgpt.com))

常见事务配置如下：

```java
@Transactional(rollbackFor = Exception.class)
public void update(UserUpdateRequest request) {
    // 写操作事务
}

@Transactional(readOnly = true)
public UserDetailVO getDetail(Long id) {
    // 只读查询事务
}
```

事务使用建议：

| 场景               | 推荐配置                                        |
| ------------------ | ----------------------------------------------- |
| 新增、修改、删除   | `@Transactional(rollbackFor = Exception.class)` |
| 查询详情、分页查询 | `@Transactional(readOnly = true)`               |
| 多表写入           | 在最外层 Service 方法加事务                     |
| 批量导入           | 分批提交，避免单事务过大                        |
| 调用外部接口       | 谨慎放在事务内，避免长事务                      |
| 私有方法调用       | 不要依赖 `private` 方法上的事务注解             |

错误示例：

```java
private void updateUser() {
    // private 方法上的 @Transactional 不会通过 Spring AOP 代理生效
}
```

正确做法是把事务加在对外暴露的 `public` Service 方法上。

```java
@Transactional(rollbackFor = Exception.class)
public void updateUser() {
    // 事务在 Spring 代理调用 public 方法时生效
}
```

如果一个方法中既有数据库写入，又有远程调用，应尽量缩短事务范围。例如先完成参数校验和远程数据准备，再开启事务写数据库。不要在事务中长时间等待外部接口、文件上传、消息发送等操作。

### 批量新增与更新

批量新增可以使用 `saveAll`，但需要注意它不是数据库层面的单条批量 SQL 魔法。JPA 会管理每个实体的状态，批量数据量很大时会产生持久化上下文膨胀问题。`JpaRepository` 也提供了 `saveAllAndFlush`、`flush`、批量删除等 JPA 扩展方法。([Home](https://docs.spring.io/spring-data/jpa/reference/4.1/api/java/org/springframework/data/jpa/repository/JpaRepository.html?utm_source=chatgpt.com))

小批量新增可以直接使用：

```java
List<UserEntity> savedList = userRepository.saveAll(entities);
```

如果是大批量导入，建议分批处理，并在配置中开启 Hibernate batch 参数。

文件位置：`src/main/resources/application.yml`

```yaml
spring:
  jpa:
    properties:
      hibernate:
        jdbc:
          # 每批 JDBC 写入数量，需要结合数据库和连接池能力调整
          batch_size: 100
        # 尽量按插入语句排序，提高批处理效果
        order_inserts: true
        # 尽量按更新语句排序，提高批处理效果
        order_updates: true
```

批量新增服务示例：

文件位置：`src/main/java/io/github/atengk/module/user/service/impl/UserBatchService.java`

```java
package io.github.atengk.module.user.service.impl;

import cn.hutool.core.collection.CollUtil;
import io.github.atengk.module.user.dto.UserCreateRequest;
import io.github.atengk.module.user.entity.UserEntity;
import io.github.atengk.module.user.enums.UserStatusEnum;
import io.github.atengk.module.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * 用户批量处理服务
 *
 * @author Ateng
 * @since 2026-05-04
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class UserBatchService {

    private static final int BATCH_SIZE = 500;

    private final UserRepository userRepository;

    @Transactional(rollbackFor = Exception.class)
    public int batchCreate(List<UserCreateRequest> requests) {
        if (CollUtil.isEmpty(requests)) {
            return 0;
        }

        int total = 0;
        List<List<UserCreateRequest>> partitions = CollUtil.split(requests, BATCH_SIZE);
        for (List<UserCreateRequest> partition : partitions) {
            List<UserEntity> entities = partition.stream()
                    .map(this::toEntity)
                    .toList();

            userRepository.saveAllAndFlush(entities);
            total += entities.size();
            log.info("批量新增用户分片完成，本次数量：{}，累计数量：{}", entities.size(), total);
        }

        return total;
    }

    private UserEntity toEntity(UserCreateRequest request) {
        UserEntity entity = new UserEntity();
        entity.setUsername(request.username());
        entity.setNickname(request.nickname());
        entity.setPhone(request.phone());
        entity.setEmail(request.email());
        entity.setStatus(request.status() == null ? UserStatusEnum.ENABLED : request.status());
        entity.setDeleted(Boolean.FALSE);
        return entity;
    }

}
```

批量更新可以使用两种方式。

第一种方式是查询出实体后逐个修改，再由 JPA 脏检查生成更新 SQL。

```java
List<UserEntity> users = userRepository.findByIdInAndDeletedFalse(ids);
users.forEach(user -> user.setStatus(UserStatusEnum.DISABLED));
userRepository.saveAll(users);
```

这种方式会触发实体生命周期、审计字段和脏检查，业务语义完整，但数据量大时性能一般。

第二种方式是使用 JPQL 批量更新。

文件位置：`src/main/java/io/github/atengk/module/user/repository/UserRepository.java`

```java
package io.github.atengk.module.user.repository;

import io.github.atengk.module.user.entity.UserEntity;
import io.github.atengk.module.user.enums.UserStatusEnum;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;

import java.time.LocalDateTime;
import java.util.Collection;

/**
 * 用户 Repository
 *
 * @author Ateng
 * @since 2026-05-04
 */
public interface UserRepository extends JpaRepository<UserEntity, Long> {

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("""
            update UserEntity u
            set u.status = :status,
                u.updateTime = :updateTime
            where u.id in :ids
              and u.deleted = false
            """)
    int batchUpdateStatus(Collection<Long> ids, UserStatusEnum status, LocalDateTime updateTime);

}
```

Service 调用如下：

```java
int count = userRepository.batchUpdateStatus(ids, UserStatusEnum.DISABLED, LocalDateTime.now());
log.info("批量修改用户状态完成，影响行数：{}", count);
```

JPQL 批量更新性能更好，但它绕过实体逐个加载和部分实体生命周期逻辑。使用后要注意持久化上下文清理问题，因此上面示例配置了 `clearAutomatically = true` 和 `flushAutomatically = true`。

### 逻辑删除处理

逻辑删除是后台系统常见的数据保留策略。它通过 `deleted`、`delete_time`、`delete_user_id` 等字段标识数据已删除，而不是直接执行物理删除。

实体字段示例：

```java
@Column(name = "deleted", nullable = false)
private Boolean deleted = Boolean.FALSE;
```

Repository 层可以统一在查询方法中追加 `DeletedFalse`。

```java
Optional<UserEntity> findByUsernameAndDeletedFalse(String username);

Page<UserEntity> findByNicknameContainingAndDeletedFalse(String nickname, Pageable pageable);

List<UserEntity> findByIdInAndDeletedFalse(Collection<Long> ids);
```

逻辑删除也可以通过 JPQL update 实现，避免先查实体再保存。

文件位置：`src/main/java/io/github/atengk/module/user/repository/UserRepository.java`

```java
package io.github.atengk.module.user.repository;

import io.github.atengk.module.user.entity.UserEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;

import java.time.LocalDateTime;

/**
 * 用户 Repository
 *
 * @author Ateng
 * @since 2026-05-04
 */
public interface UserRepository extends JpaRepository<UserEntity, Long> {

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("""
            update UserEntity u
            set u.deleted = true,
                u.updateTime = :updateTime
            where u.id = :id
              and u.deleted = false
            """)
    int logicalDeleteById(Long id, LocalDateTime updateTime);

}
```

Service 层调用如下：

```java
@Transactional(rollbackFor = Exception.class)
public void delete(Long id) {
    if (ObjectUtil.isNull(id)) {
        throw new IllegalArgumentException("用户ID不能为空");
    }

    int count = userRepository.logicalDeleteById(id, LocalDateTime.now());
    if (count == 0) {
        log.warn("逻辑删除用户失败，用户不存在或已删除，用户ID：{}", id);
        throw new IllegalArgumentException("用户不存在或已删除");
    }

    log.info("逻辑删除用户成功，用户ID：{}", id);
}
```

逻辑删除的关键点不是删除本身，而是所有查询都必须统一过滤已删除数据。否则分页列表、详情接口、唯一性校验、关联查询都可能查到已删除记录。

常见处理方式：

| 方式                            | 说明                               |
| ------------------------------- | ---------------------------------- |
| 方法命名追加 `DeletedFalse`     | 简单直接，适合小项目               |
| JPQL 统一加 `u.deleted = false` | 查询可控，适合中等复杂度           |
| Specification 统一拼接删除条件  | 动态查询中比较常用                 |
| Hibernate `@Where`              | 可自动追加条件，但隐式行为较强     |
| 数据库视图                      | 可用于复杂兼容场景，但维护成本较高 |

如果系统已经有严格的逻辑删除要求，建议在 Repository 方法、Specification、业务查询规范中明确约定：除管理端回收站或审计查询外，默认只查询 `deleted = false` 的数据。

### 数据校验与异常处理

Service 层的数据校验主要处理业务校验，例如用户名唯一、数据是否存在、状态是否允许变更。字段格式类校验可以交给 Controller 入参的 Bean Validation，例如 `@NotBlank`、`@NotNull`、`@Size`、`@Email`。

推荐添加校验依赖。

文件位置：`pom.xml`

```xml
<dependency>
    <!-- 参数校验依赖，提供 @NotBlank、@NotNull、@Valid 等能力 -->
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-validation</artifactId>
</dependency>
```

自定义业务异常如下。

文件位置：`src/main/java/io/github/atengk/common/exception/BizException.java`

```java
package io.github.atengk.common.exception;

/**
 * 业务异常
 *
 * @author Ateng
 * @since 2026-05-04
 */
public class BizException extends RuntimeException {

    private final String code;

    public BizException(String message) {
        super(message);
        this.code = "BIZ_ERROR";
    }

    public BizException(String code, String message) {
        super(message);
        this.code = code;
    }

    public String getCode() {
        return code;
    }

}
```

统一响应对象如下。

文件位置：`src/main/java/io/github/atengk/common/model/ApiResult.java`

```java
package io.github.atengk.common.model;

/**
 * 接口统一响应对象
 *
 * @author Ateng
 * @since 2026-05-04
 */
public record ApiResult<T>(
        String code,
        String message,
        T data
) {

    public static <T> ApiResult<T> success(T data) {
        return new ApiResult<>("200", "操作成功", data);
    }

    public static <T> ApiResult<T> fail(String code, String message) {
        return new ApiResult<>(code, message, null);
    }

}
```

统一异常处理器如下。

文件位置：`src/main/java/io/github/atengk/common/handler/GlobalExceptionHandler.java`

```java
package io.github.atengk.common.handler;

import cn.hutool.core.util.StrUtil;
import io.github.atengk.common.exception.BizException;
import io.github.atengk.common.model.ApiResult;
import lombok.extern.slf4j.Slf4j;
import org.springframework.validation.BindException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

/**
 * 全局异常处理器
 *
 * @author Ateng
 * @since 2026-05-04
 */
@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(BizException.class)
    public ApiResult<Void> handleBizException(BizException e) {
        log.warn("业务处理失败：{}", e.getMessage());
        return ApiResult.fail(e.getCode(), e.getMessage());
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ApiResult<Void> handleIllegalArgumentException(IllegalArgumentException e) {
        log.warn("参数处理失败：{}", e.getMessage());
        return ApiResult.fail("PARAM_ERROR", e.getMessage());
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ApiResult<Void> handleMethodArgumentNotValidException(MethodArgumentNotValidException e) {
        String message = e.getBindingResult()
                .getFieldErrors()
                .stream()
                .findFirst()
                .map(error -> StrUtil.format("{} {}", error.getField(), error.getDefaultMessage()))
                .orElse("参数校验失败");

        log.warn("请求参数校验失败：{}", message);
        return ApiResult.fail("PARAM_ERROR", message);
    }

    @ExceptionHandler(BindException.class)
    public ApiResult<Void> handleBindException(BindException e) {
        String message = e.getBindingResult()
                .getFieldErrors()
                .stream()
                .findFirst()
                .map(error -> StrUtil.format("{} {}", error.getField(), error.getDefaultMessage()))
                .orElse("参数绑定失败");

        log.warn("请求参数绑定失败：{}", message);
        return ApiResult.fail("PARAM_ERROR", message);
    }

    @ExceptionHandler(Exception.class)
    public ApiResult<Void> handleException(Exception e) {
        log.error("系统异常", e);
        return ApiResult.fail("SYSTEM_ERROR", "系统异常，请稍后重试");
    }

}
```

在 Service 中建议抛出业务异常，而不是到处抛 `RuntimeException`。

```java
if (userRepository.existsByUsernameAndDeletedFalse(request.username())) {
    throw new BizException("用户名已存在");
}
```

参数校验可以在 DTO 中声明。

文件位置：`src/main/java/io/github/atengk/module/user/dto/UserCreateRequest.java`

```java
package io.github.atengk.module.user.dto;

import io.github.atengk.module.user.enums.UserStatusEnum;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * 用户新增请求
 *
 * @author Ateng
 * @since 2026-05-04
 */
public record UserCreateRequest(

        @NotBlank(message = "用户名不能为空")
        @Size(max = 64, message = "用户名长度不能超过64位")
        String username,

        @NotBlank(message = "用户昵称不能为空")
        @Size(max = 64, message = "用户昵称长度不能超过64位")
        String nickname,

        @Size(max = 20, message = "手机号长度不能超过20位")
        String phone,

        @Email(message = "邮箱格式不正确")
        @Size(max = 128, message = "邮箱长度不能超过128位")
        String email,

        UserStatusEnum status
) {
}
```

Controller 接收参数时使用 `@Valid` 才会触发 Bean Validation。

```java
@PostMapping
public ApiResult<Long> create(@Valid @RequestBody UserCreateRequest request) {
    return ApiResult.success(userService.create(request));
}
```

数据校验建议分层处理：Controller 负责格式校验，Service 负责业务校验，Repository 负责数据访问。不要把业务校验散落在 Controller，也不要让 Repository 承担业务流程判断。



## 动态查询

动态查询用于处理查询条件不固定的场景，例如用户分页列表中，前端可能传入关键字、状态、创建时间范围，也可能只传入其中一部分。Spring Data JPA 中常用 `Specification` 和 Criteria API 实现动态条件拼接，适合后台管理系统的列表查询、组合筛选和分页检索。

### Specification 查询

`Specification` 是 Spring Data JPA 对 JPA Criteria 查询的一层封装。使用它之前，Repository 需要额外继承 `JpaSpecificationExecutor<T>`，这样就可以使用 `findAll(Specification)`、`findAll(Specification, Pageable)`、`count(Specification)` 等方法。

先调整用户 Repository。

文件位置：`src/main/java/io/github/atengk/module/user/repository/UserRepository.java`

```java
package io.github.atengk.module.user.repository;

import io.github.atengk.module.user.entity.UserEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

/**
 * 用户 Repository
 *
 * @author Ateng
 * @since 2026-05-04
 */
public interface UserRepository extends JpaRepository<UserEntity, Long>, JpaSpecificationExecutor<UserEntity> {
}
```

分页查询请求对象用于接收动态条件。

文件位置：`src/main/java/io/github/atengk/module/user/dto/UserPageQuery.java`

```java
package io.github.atengk.module.user.dto;

import io.github.atengk.module.user.enums.UserStatusEnum;

import java.time.LocalDateTime;

/**
 * 用户分页查询参数
 *
 * @author Ateng
 * @since 2026-05-04
 */
public record UserPageQuery(
        String keyword,
        UserStatusEnum status,
        LocalDateTime startTime,
        LocalDateTime endTime,
        Integer pageNum,
        Integer pageSize
) {
}
```

下面的类用于统一构建用户动态查询条件。

文件位置：`src/main/java/io/github/atengk/module/user/spec/UserSpecifications.java`

```java
package io.github.atengk.module.user.spec;

import cn.hutool.core.util.ObjectUtil;
import cn.hutool.core.util.StrUtil;
import io.github.atengk.module.user.dto.UserPageQuery;
import io.github.atengk.module.user.entity.UserEntity;
import jakarta.persistence.criteria.Predicate;
import org.springframework.data.jpa.domain.Specification;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * 用户动态查询条件
 *
 * @author Ateng
 * @since 2026-05-04
 */
public class UserSpecifications {

    private UserSpecifications() {
    }

    /**
     * 构建用户分页查询条件
     *
     * @param query 查询参数
     * @return 动态查询条件
     */
    public static Specification<UserEntity> build(UserPageQuery query) {
        return (root, criteriaQuery, criteriaBuilder) -> {
            List<Predicate> predicates = new ArrayList<>();

            // 默认过滤逻辑删除数据
            predicates.add(criteriaBuilder.isFalse(root.get("deleted")));

            if (ObjectUtil.isNull(query)) {
                return criteriaBuilder.and(predicates.toArray(Predicate[]::new));
            }

            if (StrUtil.isNotBlank(query.keyword())) {
                String keyword = StrUtil.format("%{}%", query.keyword().trim());
                Predicate usernameLike = criteriaBuilder.like(root.get("username"), keyword);
                Predicate nicknameLike = criteriaBuilder.like(root.get("nickname"), keyword);
                Predicate phoneLike = criteriaBuilder.like(root.get("phone"), keyword);
                predicates.add(criteriaBuilder.or(usernameLike, nicknameLike, phoneLike));
            }

            if (ObjectUtil.isNotNull(query.status())) {
                predicates.add(criteriaBuilder.equal(root.get("status"), query.status()));
            }

            LocalDateTime startTime = query.startTime();
            if (ObjectUtil.isNotNull(startTime)) {
                predicates.add(criteriaBuilder.greaterThanOrEqualTo(root.get("createTime"), startTime));
            }

            LocalDateTime endTime = query.endTime();
            if (ObjectUtil.isNotNull(endTime)) {
                predicates.add(criteriaBuilder.lessThanOrEqualTo(root.get("createTime"), endTime));
            }

            return criteriaBuilder.and(predicates.toArray(Predicate[]::new));
        };
    }

}
```

这类写法的核心是：前端传入哪个条件，就拼接哪个条件；没有传入的条件不参与 SQL。`deleted = false` 属于业务默认条件，应在动态查询中固定追加，避免列表查出已删除数据。

### Criteria API 基础用法

Criteria API 是 JPA 提供的类型化查询构建方式。`Specification` 底层也是基于 Criteria API 实现的。直接使用 Criteria API 时，通常会操作 `EntityManager`、`CriteriaBuilder`、`CriteriaQuery`、`Root` 和 `Predicate`。

如果只是普通动态查询，优先使用 `Specification`。如果需要更精细地控制查询字段、分组、聚合、复杂排序、DTO 构造，可以直接使用 Criteria API。

下面定义一个自定义查询 Repository，用于演示直接使用 Criteria API 查询用户列表。

文件位置：`src/main/java/io/github/atengk/module/user/repository/UserCriteriaRepository.java`

```java
package io.github.atengk.module.user.repository;

import io.github.atengk.module.user.dto.UserPageQuery;
import io.github.atengk.module.user.entity.UserEntity;

import java.util.List;

/**
 * 用户 Criteria 查询 Repository
 *
 * @author Ateng
 * @since 2026-05-04
 */
public interface UserCriteriaRepository {

    /**
     * 查询用户列表
     *
     * @param query 查询条件
     * @param offset 起始位置
     * @param limit 查询数量
     * @return 用户列表
     */
    List<UserEntity> search(UserPageQuery query, int offset, int limit);

    /**
     * 统计用户数量
     *
     * @param query 查询条件
     * @return 用户数量
     */
    long count(UserPageQuery query);

}
```

下面是直接使用 Criteria API 的实现类。

文件位置：`src/main/java/io/github/atengk/module/user/repository/impl/UserCriteriaRepositoryImpl.java`

```java
package io.github.atengk.module.user.repository.impl;

import cn.hutool.core.util.ObjectUtil;
import cn.hutool.core.util.StrUtil;
import io.github.atengk.module.user.dto.UserPageQuery;
import io.github.atengk.module.user.entity.UserEntity;
import io.github.atengk.module.user.repository.UserCriteriaRepository;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.TypedQuery;
import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.CriteriaQuery;
import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.criteria.Root;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * 用户 Criteria 查询 Repository 实现
 *
 * @author Ateng
 * @since 2026-05-04
 */
@Slf4j
@Repository
public class UserCriteriaRepositoryImpl implements UserCriteriaRepository {

    @PersistenceContext
    private EntityManager entityManager;

    /**
     * 查询用户列表
     *
     * @param query 查询条件
     * @param offset 起始位置
     * @param limit 查询数量
     * @return 用户列表
     */
    @Override
    public List<UserEntity> search(UserPageQuery query, int offset, int limit) {
        CriteriaBuilder builder = entityManager.getCriteriaBuilder();
        CriteriaQuery<UserEntity> criteriaQuery = builder.createQuery(UserEntity.class);
        Root<UserEntity> root = criteriaQuery.from(UserEntity.class);

        List<Predicate> predicates = buildPredicates(query, builder, root);
        criteriaQuery.where(predicates.toArray(Predicate[]::new));
        criteriaQuery.orderBy(builder.desc(root.get("id")));

        TypedQuery<UserEntity> typedQuery = entityManager.createQuery(criteriaQuery);
        typedQuery.setFirstResult(Math.max(offset, 0));
        typedQuery.setMaxResults(Math.max(limit, 1));

        List<UserEntity> result = typedQuery.getResultList();
        log.info("Criteria 查询用户列表完成，数量：{}", result.size());
        return result;
    }

    /**
     * 统计用户数量
     *
     * @param query 查询条件
     * @return 用户数量
     */
    @Override
    public long count(UserPageQuery query) {
        CriteriaBuilder builder = entityManager.getCriteriaBuilder();
        CriteriaQuery<Long> criteriaQuery = builder.createQuery(Long.class);
        Root<UserEntity> root = criteriaQuery.from(UserEntity.class);

        List<Predicate> predicates = buildPredicates(query, builder, root);
        criteriaQuery.select(builder.count(root));
        criteriaQuery.where(predicates.toArray(Predicate[]::new));

        return entityManager.createQuery(criteriaQuery).getSingleResult();
    }

    private List<Predicate> buildPredicates(UserPageQuery query, CriteriaBuilder builder, Root<UserEntity> root) {
        List<Predicate> predicates = new ArrayList<>();

        predicates.add(builder.isFalse(root.get("deleted")));

        if (ObjectUtil.isNull(query)) {
            return predicates;
        }

        if (StrUtil.isNotBlank(query.keyword())) {
            String keyword = StrUtil.format("%{}%", query.keyword().trim());
            Predicate usernameLike = builder.like(root.get("username"), keyword);
            Predicate nicknameLike = builder.like(root.get("nickname"), keyword);
            Predicate phoneLike = builder.like(root.get("phone"), keyword);
            predicates.add(builder.or(usernameLike, nicknameLike, phoneLike));
        }

        if (ObjectUtil.isNotNull(query.status())) {
            predicates.add(builder.equal(root.get("status"), query.status()));
        }

        LocalDateTime startTime = query.startTime();
        if (ObjectUtil.isNotNull(startTime)) {
            predicates.add(builder.greaterThanOrEqualTo(root.get("createTime"), startTime));
        }

        LocalDateTime endTime = query.endTime();
        if (ObjectUtil.isNotNull(endTime)) {
            predicates.add(builder.lessThanOrEqualTo(root.get("createTime"), endTime));
        }

        return predicates;
    }

}
```

Criteria API 的优势是控制能力强，缺点是代码量明显增加。后台管理系统中的普通分页筛选，一般使用 `Specification` 更清晰；复杂统计、动态投影、分组聚合时，再考虑直接使用 Criteria API。

### 多条件组合查询

多条件组合查询的核心是把每个查询条件拆成独立的 `Specification`，再按业务需要组合。这样可以避免把所有逻辑都堆在一个方法中，也方便复用常用条件，例如“未删除”“按状态查询”“按关键字查询”。

下面定义更细粒度的用户查询条件。

文件位置：`src/main/java/io/github/atengk/module/user/spec/UserSpecBuilder.java`

```java
package io.github.atengk.module.user.spec;

import cn.hutool.core.util.ObjectUtil;
import cn.hutool.core.util.StrUtil;
import io.github.atengk.module.user.entity.UserEntity;
import io.github.atengk.module.user.enums.UserStatusEnum;
import org.springframework.data.jpa.domain.Specification;

import java.time.LocalDateTime;

/**
 * 用户查询条件构建器
 *
 * @author Ateng
 * @since 2026-05-04
 */
public class UserSpecBuilder {

    private UserSpecBuilder() {
    }

    /**
     * 未删除条件
     *
     * @return 查询条件
     */
    public static Specification<UserEntity> notDeleted() {
        return (root, query, builder) -> builder.isFalse(root.get("deleted"));
    }

    /**
     * 关键字匹配条件
     *
     * @param keyword 关键字
     * @return 查询条件
     */
    public static Specification<UserEntity> keywordLike(String keyword) {
        return (root, query, builder) -> {
            if (StrUtil.isBlank(keyword)) {
                return builder.conjunction();
            }
            String value = StrUtil.format("%{}%", keyword.trim());
            return builder.or(
                    builder.like(root.get("username"), value),
                    builder.like(root.get("nickname"), value),
                    builder.like(root.get("phone"), value)
            );
        };
    }

    /**
     * 用户状态条件
     *
     * @param status 用户状态
     * @return 查询条件
     */
    public static Specification<UserEntity> statusEq(UserStatusEnum status) {
        return (root, query, builder) -> {
            if (ObjectUtil.isNull(status)) {
                return builder.conjunction();
            }
            return builder.equal(root.get("status"), status);
        };
    }

    /**
     * 创建时间开始条件
     *
     * @param startTime 开始时间
     * @return 查询条件
     */
    public static Specification<UserEntity> createTimeGe(LocalDateTime startTime) {
        return (root, query, builder) -> {
            if (ObjectUtil.isNull(startTime)) {
                return builder.conjunction();
            }
            return builder.greaterThanOrEqualTo(root.get("createTime"), startTime);
        };
    }

    /**
     * 创建时间结束条件
     *
     * @param endTime 结束时间
     * @return 查询条件
     */
    public static Specification<UserEntity> createTimeLe(LocalDateTime endTime) {
        return (root, query, builder) -> {
            if (ObjectUtil.isNull(endTime)) {
                return builder.conjunction();
            }
            return builder.lessThanOrEqualTo(root.get("createTime"), endTime);
        };
    }

}
```

Service 中可以按条件组合查询。

```java
Specification<UserEntity> specification = Specification
        .allOf(
                UserSpecBuilder.notDeleted(),
                UserSpecBuilder.keywordLike(query.keyword()),
                UserSpecBuilder.statusEq(query.status()),
                UserSpecBuilder.createTimeGe(query.startTime()),
                UserSpecBuilder.createTimeLe(query.endTime())
        );
```

如果当前 Spring Data JPA 版本不支持 `Specification.allOf`，可以使用链式 `and` 方式。

```java
Specification<UserEntity> specification = UserSpecBuilder.notDeleted()
        .and(UserSpecBuilder.keywordLike(query.keyword()))
        .and(UserSpecBuilder.statusEq(query.status()))
        .and(UserSpecBuilder.createTimeGe(query.startTime()))
        .and(UserSpecBuilder.createTimeLe(query.endTime()));
```

多条件组合查询建议遵循几个规则：默认条件单独封装，例如 `notDeleted()`；可选条件返回 `builder.conjunction()`，表示不影响最终查询；复杂 `or` 条件要放在一个独立方法中，避免和外层 `and` 混在一起导致逻辑错误。

### 分页动态查询

分页动态查询是 `Specification` 最常见的使用场景。它一般由 Controller 接收查询条件，Service 构建 `Specification` 和 `Pageable`，Repository 执行分页查询，最后转换为前端需要的分页 VO。

下面给出 Service 层完整示例。

文件位置：`src/main/java/io/github/atengk/module/user/service/impl/UserDynamicQueryService.java`

```java
package io.github.atengk.module.user.service.impl;

import cn.hutool.core.util.ObjectUtil;
import io.github.atengk.common.model.PageResult;
import io.github.atengk.common.util.PageUtil;
import io.github.atengk.module.user.dto.UserPageQuery;
import io.github.atengk.module.user.entity.UserEntity;
import io.github.atengk.module.user.repository.UserRepository;
import io.github.atengk.module.user.spec.UserSpecBuilder;
import io.github.atengk.module.user.vo.UserPageVO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 用户动态查询服务
 *
 * @author Ateng
 * @since 2026-05-04
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class UserDynamicQueryService {

    private final UserRepository userRepository;

    /**
     * 分页查询用户
     *
     * @param query 查询参数
     * @return 分页结果
     */
    @Transactional(readOnly = true)
    public PageResult<UserPageVO> page(UserPageQuery query) {
        UserPageQuery safeQuery = ObjectUtil.defaultIfNull(
                query,
                new UserPageQuery(null, null, null, null, 1, 10)
        );

        Pageable pageable = PageUtil.buildPageable(safeQuery.pageNum(), safeQuery.pageSize());

        Specification<UserEntity> specification = UserSpecBuilder.notDeleted()
                .and(UserSpecBuilder.keywordLike(safeQuery.keyword()))
                .and(UserSpecBuilder.statusEq(safeQuery.status()))
                .and(UserSpecBuilder.createTimeGe(safeQuery.startTime()))
                .and(UserSpecBuilder.createTimeLe(safeQuery.endTime()));

        Page<UserEntity> entityPage = userRepository.findAll(specification, pageable);
        Page<UserPageVO> voPage = entityPage.map(this::toPageVO);

        log.info("分页动态查询用户完成，总数：{}，当前页数量：{}", voPage.getTotalElements(), voPage.getNumberOfElements());
        return PageResult.of(voPage);
    }

    private UserPageVO toPageVO(UserEntity entity) {
        return new UserPageVO(
                entity.getId(),
                entity.getUsername(),
                entity.getNickname(),
                entity.getPhone(),
                entity.getEmail(),
                entity.getStatus(),
                entity.getCreateTime()
        );
    }

}
```

如果接口层需要提供分页查询接口，可以这样调用。

```java
@GetMapping("/page")
public ApiResult<PageResult<UserPageVO>> page(UserPageQuery query) {
    return ApiResult.success(userDynamicQueryService.page(query));
}
```

分页动态查询要注意排序字段。默认可以统一按 `id desc` 或 `createTime desc` 排序，不建议直接把前端传入的任意字段作为排序字段。若确实需要前端控制排序，应做字段白名单映射，例如只允许 `id`、`createTime`、`username`。

## 数据审计

数据审计用于自动维护创建时间、更新时间、创建人、更新人等字段。Spring Data JPA 提供 Auditing 能力，可以通过 `@CreatedDate`、`@LastModifiedDate`、`@CreatedBy`、`@LastModifiedBy` 自动填充审计字段。它适合后台管理系统、业务系统和需要追踪数据来源的服务端项目。

### 创建时间与更新时间

如果项目只需要创建时间和更新时间，可以使用 JPA 生命周期回调，例如 `@PrePersist` 和 `@PreUpdate`。如果后续还要维护创建人、更新人，推荐直接使用 Spring Data JPA Auditing，避免两套机制混用。

启用 Auditing 后，审计基类可以这样设计。

文件位置：`src/main/java/io/github/atengk/common/entity/AuditableBaseEntity.java`

```java
package io.github.atengk.common.entity;

import jakarta.persistence.Column;
import jakarta.persistence.EntityListeners;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.MappedSuperclass;
import lombok.Getter;
import lombok.Setter;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * JPA 审计实体基类
 *
 * @author Ateng
 * @since 2026-05-04
 */
@Getter
@Setter
@MappedSuperclass
@EntityListeners(AuditingEntityListener.class)
public abstract class AuditableBaseEntity implements Serializable {

    /**
     * 主键 ID
     */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id", nullable = false, updatable = false)
    private Long id;

    /**
     * 创建时间
     */
    @CreatedDate
    @Column(name = "create_time", nullable = false, updatable = false)
    private LocalDateTime createTime;

    /**
     * 更新时间
     */
    @LastModifiedDate
    @Column(name = "update_time", nullable = false)
    private LocalDateTime updateTime;

}
```

实体类继承该基类后，新增时会自动填充 `createTime` 和 `updateTime`，修改时会自动刷新 `updateTime`。

```java
@Getter
@Setter
@Entity
@Table(name = "sys_user")
public class UserEntity extends AuditableBaseEntity {

    @Column(name = "username", nullable = false, length = 64, unique = true)
    private String username;

    @Column(name = "nickname", nullable = false, length = 64)
    private String nickname;

}
```

使用 Auditing 后，不建议再对同一批字段同时使用 `@PrePersist`、`@PreUpdate` 手动赋值，否则字段维护来源不统一，后续排查问题会变得困难。

### 创建人与更新人

创建人和更新人需要通过 `AuditorAware<T>` 获取当前操作人。`T` 的类型应与实体字段类型一致，常见选择是 `Long` 用户 ID、`String` 用户名或租户系统中的操作人编码。

先定义包含创建人和更新人的审计基类。

文件位置：`src/main/java/io/github/atengk/common/entity/FullAuditableBaseEntity.java`

```java
package io.github.atengk.common.entity;

import jakarta.persistence.Column;
import jakarta.persistence.EntityListeners;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.MappedSuperclass;
import lombok.Getter;
import lombok.Setter;
import org.springframework.data.annotation.CreatedBy;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedBy;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * JPA 完整审计实体基类
 *
 * @author Ateng
 * @since 2026-05-04
 */
@Getter
@Setter
@MappedSuperclass
@EntityListeners(AuditingEntityListener.class)
public abstract class FullAuditableBaseEntity implements Serializable {

    /**
     * 主键 ID
     */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id", nullable = false, updatable = false)
    private Long id;

    /**
     * 创建人 ID
     */
    @CreatedBy
    @Column(name = "create_user_id", updatable = false)
    private Long createUserId;

    /**
     * 更新人 ID
     */
    @LastModifiedBy
    @Column(name = "update_user_id")
    private Long updateUserId;

    /**
     * 创建时间
     */
    @CreatedDate
    @Column(name = "create_time", nullable = false, updatable = false)
    private LocalDateTime createTime;

    /**
     * 更新时间
     */
    @LastModifiedDate
    @Column(name = "update_time", nullable = false)
    private LocalDateTime updateTime;

}
```

为了让 `@CreatedBy` 和 `@LastModifiedBy` 能获取当前登录用户，需要提供当前用户上下文。下面是一个简单的 ThreadLocal 示例，实际项目中可以替换为 Sa-Token、Spring Security 或网关传入的登录用户信息。

文件位置：`src/main/java/io/github/atengk/common/context/LoginUserContext.java`

```java
package io.github.atengk.common.context;

import cn.hutool.core.util.ObjectUtil;

/**
 * 登录用户上下文
 *
 * @author Ateng
 * @since 2026-05-04
 */
public class LoginUserContext {

    private static final ThreadLocal<Long> USER_ID_HOLDER = new ThreadLocal<>();

    private LoginUserContext() {
    }

    /**
     * 设置当前用户 ID
     *
     * @param userId 用户 ID
     */
    public static void setUserId(Long userId) {
        USER_ID_HOLDER.set(userId);
    }

    /**
     * 获取当前用户 ID
     *
     * @return 用户 ID
     */
    public static Long getUserId() {
        Long userId = USER_ID_HOLDER.get();
        return ObjectUtil.defaultIfNull(userId, 0L);
    }

    /**
     * 清理当前用户上下文
     */
    public static void clear() {
        USER_ID_HOLDER.remove();
    }

}
```

为了演示审计功能，可以通过请求头临时传入用户 ID。生产环境应从登录态中读取，不建议完全依赖前端请求头。

文件位置：`src/main/java/io/github/atengk/common/filter/LoginUserContextFilter.java`

```java
package io.github.atengk.common.filter;

import cn.hutool.core.convert.Convert;
import cn.hutool.core.util.StrUtil;
import io.github.atengk.common.context.LoginUserContext;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

/**
 * 登录用户上下文过滤器
 *
 * @author Ateng
 * @since 2026-05-04
 */
@Slf4j
public class LoginUserContextFilter extends OncePerRequestFilter {

    private static final String USER_ID_HEADER = "X-User-Id";

    /**
     * 处理请求用户上下文
     *
     * @param request 请求对象
     * @param response 响应对象
     * @param filterChain 过滤器链
     * @throws ServletException Servlet 异常
     * @throws IOException IO 异常
     */
    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        try {
            String userIdValue = request.getHeader(USER_ID_HEADER);
            if (StrUtil.isNotBlank(userIdValue)) {
                Long userId = Convert.toLong(userIdValue);
                LoginUserContext.setUserId(userId);
                log.debug("设置当前请求用户ID：{}", userId);
            }
            filterChain.doFilter(request, response);
        } finally {
            LoginUserContext.clear();
        }
    }

}
```

注册过滤器。

文件位置：`src/main/java/io/github/atengk/common/config/WebFilterConfig.java`

```java
package io.github.atengk.common.config;

import io.github.atengk.common.filter.LoginUserContextFilter;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Web 过滤器配置
 *
 * @author Ateng
 * @since 2026-05-04
 */
@Configuration
public class WebFilterConfig {

    /**
     * 注册登录用户上下文过滤器
     *
     * @return 过滤器注册对象
     */
    @Bean
    public FilterRegistrationBean<LoginUserContextFilter> loginUserContextFilter() {
        FilterRegistrationBean<LoginUserContextFilter> registrationBean = new FilterRegistrationBean<>();
        registrationBean.setFilter(new LoginUserContextFilter());
        registrationBean.addUrlPatterns("/*");
        registrationBean.setOrder(1);
        return registrationBean;
    }

}
```

### JPA Auditing 配置

JPA Auditing 需要通过 `@EnableJpaAuditing` 开启，并提供 `AuditorAware<Long>` Bean。`AuditorAware` 返回的值会自动写入 `@CreatedBy` 和 `@LastModifiedBy` 标注的字段。

文件位置：`src/main/java/io/github/atengk/common/config/JpaAuditingConfig.java`

```java
package io.github.atengk.common.config;

import cn.hutool.core.util.ObjectUtil;
import io.github.atengk.common.context.LoginUserContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.domain.AuditorAware;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;

import java.util.Optional;

/**
 * JPA 审计配置
 *
 * @author Ateng
 * @since 2026-05-04
 */
@Configuration
@EnableJpaAuditing(auditorAwareRef = "auditorAware")
public class JpaAuditingConfig {

    /**
     * 当前审计用户提供者
     *
     * @return 当前用户 ID
     */
    @Bean
    public AuditorAware<Long> auditorAware() {
        return () -> {
            Long userId = LoginUserContext.getUserId();
            if (ObjectUtil.isNull(userId)) {
                return Optional.of(0L);
            }
            return Optional.of(userId);
        };
    }

}
```

实体类继承完整审计基类。

文件位置：`src/main/java/io/github/atengk/module/user/entity/UserEntity.java`

```java
package io.github.atengk.module.user.entity;

import io.github.atengk.common.entity.FullAuditableBaseEntity;
import io.github.atengk.module.user.enums.UserStatusEnum;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Enumerated;
import jakarta.persistence.EnumType;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

/**
 * 用户实体
 *
 * @author Ateng
 * @since 2026-05-04
 */
@Getter
@Setter
@Entity
@Table(name = "sys_user")
public class UserEntity extends FullAuditableBaseEntity {

    /**
     * 用户名
     */
    @Column(name = "username", nullable = false, length = 64, unique = true)
    private String username;

    /**
     * 用户昵称
     */
    @Column(name = "nickname", nullable = false, length = 64)
    private String nickname;

    /**
     * 手机号
     */
    @Column(name = "phone", length = 20)
    private String phone;

    /**
     * 邮箱
     */
    @Column(name = "email", length = 128)
    private String email;

    /**
     * 用户状态
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 32)
    private UserStatusEnum status;

    /**
     * 是否删除
     */
    @Column(name = "deleted", nullable = false)
    private Boolean deleted = Boolean.FALSE;

}
```

对应表结构需要包含审计字段。

```sql
CREATE TABLE sys_user (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '主键 ID',
    username VARCHAR(64) NOT NULL UNIQUE COMMENT '用户名',
    nickname VARCHAR(64) NOT NULL COMMENT '用户昵称',
    phone VARCHAR(20) NULL COMMENT '手机号',
    email VARCHAR(128) NULL COMMENT '邮箱',
    status VARCHAR(32) NOT NULL COMMENT '用户状态',
    deleted BIT NOT NULL DEFAULT 0 COMMENT '是否删除',
    create_user_id BIGINT NULL COMMENT '创建人 ID',
    update_user_id BIGINT NULL COMMENT '更新人 ID',
    create_time DATETIME NOT NULL COMMENT '创建时间',
    update_time DATETIME NOT NULL COMMENT '更新时间'
) COMMENT '用户表';
```

验证审计字段时，可以通过请求头传入用户 ID 后调用新增或修改接口。

```bash
# 新增用户，模拟当前操作人为 10001
curl -X POST "http://localhost:8080/users" \
  -H "Content-Type: application/json" \
  -H "X-User-Id: 10001" \
  -d '{
    "username": "zhangsan",
    "nickname": "张三",
    "phone": "13800000000",
    "email": "zhangsan@example.com",
    "status": "ENABLED"
  }'
```

新增后检查数据库字段：

```sql
SELECT
    id,
    username,
    create_user_id,
    update_user_id,
    create_time,
    update_time
FROM sys_user
WHERE username = 'zhangsan';
```

正常情况下，新增数据会自动写入 `create_user_id`、`update_user_id`、`create_time` 和 `update_time`。修改数据时，`update_user_id` 和 `update_time` 会自动更新，`create_user_id` 和 `create_time` 不应变化。

数据审计的实践建议是：时间字段优先统一交给 JPA Auditing 维护；用户字段通过统一登录上下文获取；创建字段设置 `updatable = false`；不要在 Service 中手动重复维护同一批审计字段。



## 接口开发

接口开发用于把前面完成的 Entity、Repository、Service 能力暴露为 REST API。Controller 层只负责接收参数、触发校验、调用 Service、返回统一响应，不建议在 Controller 中直接写业务逻辑或直接操作 Repository。

本章节基于前文已有对象继续展开：

```text
io.github.atengk.common.model.ApiResult
io.github.atengk.common.model.PageResult
io.github.atengk.module.user.dto.UserCreateRequest
io.github.atengk.module.user.dto.UserUpdateRequest
io.github.atengk.module.user.dto.UserPageQuery
io.github.atengk.module.user.vo.UserDetailVO
io.github.atengk.module.user.vo.UserPageVO
io.github.atengk.module.user.service.UserService
```

用户接口统一路径约定如下：

| 功能     | 请求方式 | 接口路径          | 说明                 |
| -------- | -------- | ----------------- | -------------------- |
| 新增用户 | `POST`   | `/api/users`      | 新增一条用户数据     |
| 修改用户 | `PUT`    | `/api/users/{id}` | 根据 ID 修改用户数据 |
| 删除用户 | `DELETE` | `/api/users/{id}` | 根据 ID 逻辑删除用户 |
| 用户详情 | `GET`    | `/api/users/{id}` | 根据 ID 查询详情     |
| 分页查询 | `GET`    | `/api/users/page` | 按条件分页查询用户   |

### 新增接口

新增接口用于创建用户。Controller 接收 `UserCreateRequest`，通过 `@Valid` 触发参数校验，然后调用 Service 完成用户名唯一性校验、实体转换和数据保存。

下面是用户接口 Controller 的完整写法，包含新增、修改、删除、详情和分页接口。

文件位置：`src/main/java/io/github/atengk/module/user/controller/UserController.java`

```java
package io.github.atengk.module.user.controller;

import io.github.atengk.common.model.ApiResult;
import io.github.atengk.common.model.PageResult;
import io.github.atengk.module.user.dto.UserCreateRequest;
import io.github.atengk.module.user.dto.UserPageQuery;
import io.github.atengk.module.user.dto.UserUpdateRequest;
import io.github.atengk.module.user.service.UserService;
import io.github.atengk.module.user.vo.UserDetailVO;
import io.github.atengk.module.user.vo.UserPageVO;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

/**
 * 用户接口
 *
 * @author Ateng
 * @since 2026-05-04
 */
@Slf4j
@Validated
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/users")
public class UserController {

    private final UserService userService;

    /**
     * 新增用户
     *
     * @param request 新增参数
     * @return 用户 ID
     */
    @PostMapping
    public ApiResult<Long> create(@Valid @RequestBody UserCreateRequest request) {
        Long id = userService.create(request);
        log.info("新增用户接口调用成功，用户ID：{}", id);
        return ApiResult.success(id);
    }

    /**
     * 修改用户
     *
     * @param id 用户 ID
     * @param request 修改参数
     * @return 操作结果
     */
    @PutMapping("/{id}")
    public ApiResult<Void> update(@PathVariable("id") @NotNull(message = "用户ID不能为空") Long id,
                                  @Valid @RequestBody UserUpdateRequest request) {
        UserUpdateRequest actualRequest = new UserUpdateRequest(
                id,
                request.nickname(),
                request.phone(),
                request.email(),
                request.status()
        );
        userService.update(actualRequest);
        log.info("修改用户接口调用成功，用户ID：{}", id);
        return ApiResult.success(null);
    }

    /**
     * 删除用户
     *
     * @param id 用户 ID
     * @return 操作结果
     */
    @DeleteMapping("/{id}")
    public ApiResult<Void> delete(@PathVariable("id") @NotNull(message = "用户ID不能为空") Long id) {
        userService.delete(id);
        log.info("删除用户接口调用成功，用户ID：{}", id);
        return ApiResult.success(null);
    }

    /**
     * 查询用户详情
     *
     * @param id 用户 ID
     * @return 用户详情
     */
    @GetMapping("/{id}")
    public ApiResult<UserDetailVO> detail(@PathVariable("id") @NotNull(message = "用户ID不能为空") Long id) {
        UserDetailVO detail = userService.getDetail(id);
        return ApiResult.success(detail);
    }

    /**
     * 分页查询用户
     *
     * @param query 查询参数
     * @return 分页结果
     */
    @GetMapping("/page")
    public ApiResult<PageResult<UserPageVO>> page(@Valid UserPageQuery query) {
        PageResult<UserPageVO> result = userService.page(query);
        return ApiResult.success(result);
    }

}
```

新增接口请求示例：

```bash
curl -X POST "http://localhost:8080/api/users" \
  -H "Content-Type: application/json" \
  -H "X-User-Id: 10001" \
  -d '{
    "username": "zhangsan",
    "nickname": "张三",
    "phone": "13800000000",
    "email": "zhangsan@example.com",
    "status": "ENABLED"
  }'
```

成功响应示例：

```json
{
  "code": "200",
  "message": "操作成功",
  "data": 1
}
```

新增接口需要注意两类校验：字段级校验和业务级校验。字段级校验放在 DTO 中，例如用户名不能为空、邮箱格式正确；业务级校验放在 Service 中，例如用户名不能重复。

### 修改接口

修改接口用于根据用户 ID 更新用户昵称、手机号、邮箱、状态等字段。接口路径中的 ID 应作为最终用户 ID，避免前端请求体中的 ID 与路径 ID 不一致。

修改请求示例：

```bash
curl -X PUT "http://localhost:8080/api/users/1" \
  -H "Content-Type: application/json" \
  -H "X-User-Id: 10002" \
  -d '{
    "nickname": "张三-修改",
    "phone": "13900000000",
    "email": "zhangsan_new@example.com",
    "status": "ENABLED"
  }'
```

成功响应示例：

```json
{
  "code": "200",
  "message": "操作成功",
  "data": null
}
```

修改接口建议遵循以下规则：

| 规则                 | 说明                                           |
| -------------------- | ---------------------------------------------- |
| 以路径 ID 为准       | `PUT /api/users/{id}` 中的 `id` 是最终修改目标 |
| Service 校验数据存在 | 不存在或已删除时抛出业务异常                   |
| 不允许修改主键       | 主键只用于定位数据，不参与更新                 |
| 谨慎允许修改唯一字段 | 如用户名、手机号、邮箱，需要额外唯一性校验     |
| 不直接覆盖全部字段   | 可按业务区分全量修改和局部修改                 |

如果需要支持局部修改，可以额外提供 `PATCH /api/users/{id}`，但要明确哪些字段允许为空、哪些字段为空表示不修改，避免修改语义混乱。

### 删除接口

删除接口建议默认使用逻辑删除，而不是物理删除。前文已经在 Service 中通过 `deleted = true` 实现逻辑删除，接口层只需要调用 `userService.delete(id)`。

删除请求示例：

```bash
curl -X DELETE "http://localhost:8080/api/users/1" \
  -H "X-User-Id: 10003"
```

成功响应示例：

```json
{
  "code": "200",
  "message": "操作成功",
  "data": null
}
```

删除接口需要重点处理幂等性。常见策略有两种：

| 策略     | 行为                             |
| -------- | -------------------------------- |
| 严格模式 | 数据不存在或已删除时返回业务错误 |
| 幂等模式 | 数据不存在或已删除时仍返回成功   |

后台管理系统通常使用严格模式，便于前端提示“数据不存在或已删除”。开放 API 或重复调用风险较高的接口，可以考虑幂等模式。

### 详情接口

详情接口用于根据 ID 查询单条用户数据。接口返回对象建议使用 VO，不建议直接返回 JPA Entity。直接返回实体容易触发懒加载、循环引用、字段泄露等问题。

详情请求示例：

```bash
curl -X GET "http://localhost:8080/api/users/1"
```

成功响应示例：

```json
{
  "code": "200",
  "message": "操作成功",
  "data": {
    "id": 1,
    "username": "zhangsan",
    "nickname": "张三",
    "phone": "13800000000",
    "email": "zhangsan@example.com",
    "status": "ENABLED",
    "createTime": "2026-05-04T10:00:00",
    "updateTime": "2026-05-04T10:00:00"
  }
}
```

详情接口一般只返回前端需要展示的字段。对于密码、密钥、内部状态、逻辑删除标识、审计字段等敏感或内部字段，应通过 VO 控制是否返回。

如果详情页需要同时展示用户角色、部门、权限等关联数据，不建议直接依赖实体懒加载自动序列化。推荐在 Service 中显式查询并组装 VO。

### 分页查询接口

分页查询接口用于后台列表页。它通常支持关键字、状态、创建时间范围、页码、每页数量等参数。分页接口建议使用 GET 查询参数，复杂筛选条件也可以使用 POST 查询对象。

分页请求示例：

```bash
curl -X GET "http://localhost:8080/api/users/page?keyword=zhang&status=ENABLED&pageNum=1&pageSize=10"
```

成功响应示例：

```json
{
  "code": "200",
  "message": "操作成功",
  "data": {
    "records": [
      {
        "id": 1,
        "username": "zhangsan",
        "nickname": "张三",
        "phone": "13800000000",
        "email": "zhangsan@example.com",
        "status": "ENABLED",
        "createTime": "2026-05-04T10:00:00"
      }
    ],
    "total": 1,
    "pageNum": 1,
    "pageSize": 10,
    "totalPages": 1
  }
}
```

分页接口建议设置默认页码和最大每页数量。前文 `PageUtil` 中已经限制 `pageSize` 最大为 `200`，可以避免前端传入过大的分页参数导致慢查询或内存压力。

分页查询的实践建议：

| 建议                 | 说明                                                     |
| -------------------- | -------------------------------------------------------- |
| 页码从 1 开始        | 对前端更友好，Service 内部转换为 Spring Data 的 0 起页码 |
| 返回总数             | 后台管理系统通常需要分页器总数                           |
| 不返回 Entity        | 使用 VO 控制返回字段                                     |
| 默认稳定排序         | 例如 `id desc` 或 `createTime desc`                      |
| 限制 pageSize        | 避免一次查询过多数据                                     |
| 查询默认过滤 deleted | 避免查出逻辑删除数据                                     |

## 性能优化

Spring Data JPA 的性能优化重点不是简单“少用 JPA”，而是理解 Hibernate 的加载机制、SQL 生成方式、持久化上下文、批量操作和关联查询行为。多数性能问题来自隐式 SQL、懒加载误用、N+1 查询、分页关联查询不当和批量写入方式不合理。

### 懒加载与立即加载

JPA 关联关系有两种主要加载方式：懒加载和立即加载。

| 加载方式          | 说明                         | 特点                         |
| ----------------- | ---------------------------- | ---------------------------- |
| `FetchType.LAZY`  | 使用关联对象时才查询         | 查询更轻，但需要事务边界支持 |
| `FetchType.EAGER` | 查询主对象时立即查询关联对象 | 使用方便，但容易产生额外 SQL |

常见默认行为如下：

| 关系注解      | 默认加载方式 | 推荐显式配置        |
| ------------- | ------------ | ------------------- |
| `@OneToOne`   | `EAGER`      | 多数场景改为 `LAZY` |
| `@ManyToOne`  | `EAGER`      | 多数场景改为 `LAZY` |
| `@OneToMany`  | `LAZY`       | 保持 `LAZY`         |
| `@ManyToMany` | `LAZY`       | 保持 `LAZY`         |

实际项目中建议统一显式配置懒加载。

```java
@ManyToOne(fetch = FetchType.LAZY)
@JoinColumn(name = "dept_id")
private DeptEntity dept;
@OneToMany(mappedBy = "order", fetch = FetchType.LAZY)
private List<OrderItemEntity> items = new ArrayList<>();
```

懒加载需要注意事务边界。如果在 Service 方法事务结束后，Controller 或 JSON 序列化过程再访问懒加载属性，容易出现懒加载异常。推荐做法是在 Service 事务内把需要的数据转换为 VO。

下面示例演示在 Service 中显式转换用户详情 VO，避免 Controller 直接访问懒加载对象。

文件位置：`src/main/java/io/github/atengk/module/user/service/impl/UserDetailService.java`

```java
package io.github.atengk.module.user.service.impl;

import cn.hutool.core.util.ObjectUtil;
import io.github.atengk.module.user.entity.UserEntity;
import io.github.atengk.module.user.repository.UserRepository;
import io.github.atengk.module.user.vo.UserDetailVO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 用户详情查询服务
 *
 * @author Ateng
 * @since 2026-05-04
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class UserDetailService {

    private final UserRepository userRepository;

    /**
     * 查询用户详情
     *
     * @param id 用户 ID
     * @return 用户详情
     */
    @Transactional(readOnly = true)
    public UserDetailVO getDetail(Long id) {
        if (ObjectUtil.isNull(id)) {
            throw new IllegalArgumentException("用户ID不能为空");
        }

        UserEntity entity = userRepository.findById(id)
                .filter(item -> !Boolean.TRUE.equals(item.getDeleted()))
                .orElseThrow(() -> new IllegalArgumentException("用户不存在"));

        log.info("查询用户详情成功，用户ID：{}", id);
        return new UserDetailVO(
                entity.getId(),
                entity.getUsername(),
                entity.getNickname(),
                entity.getPhone(),
                entity.getEmail(),
                entity.getStatus(),
                entity.getCreateTime(),
                entity.getUpdateTime()
        );
    }

}
```

如果已经在 `application.yml` 中设置：

```yaml
spring:
  jpa:
    open-in-view: false
```

就更应该在 Service 层完成所有数据准备和 VO 转换。关闭 Open EntityManager in View 可以减少视图层隐式 SQL，但也要求开发者明确处理懒加载边界。

### N+1 查询问题

N+1 查询是 JPA 项目中最常见的性能问题之一。它通常发生在先查询 N 条主表数据，再循环访问每条数据的关联对象时。

例如，查询用户列表后循环访问用户角色：

```java
List<UserEntity> users = userRepository.findAll();
for (UserEntity user : users) {
    user.getRoles().size();
}
```

可能产生如下 SQL 行为：

```text
1 条 SQL：查询用户列表
N 条 SQL：分别查询每个用户的角色
```

这就是 N+1 查询。数据量较小时不明显，列表数据变多后会造成大量 SQL。

常见优化方式如下：

| 方式                  | 适用场景                             |
| --------------------- | ------------------------------------ |
| DTO 投影              | 列表只需要部分字段                   |
| `join fetch`          | 查询详情或少量数据时同时加载关联对象 |
| `@EntityGraph`        | 声明式指定需要加载的关联对象         |
| 批量查询后手动组装    | 多集合、多分页、复杂列表场景         |
| Hibernate batch fetch | 缓解多条懒加载 SQL                   |
| 避免实体直接序列化    | 防止 JSON 触发懒加载                 |

对于分页列表，优先使用 DTO 投影，而不是加载完整实体和全部关联对象。

文件位置：`src/main/java/io/github/atengk/module/user/vo/UserRolePageVO.java`

```java
package io.github.atengk.module.user.vo;

/**
 * 用户角色分页展示 VO
 *
 * @author Ateng
 * @since 2026-05-04
 */
public record UserRolePageVO(
        Long userId,
        String username,
        String nickname,
        Long roleId,
        String roleName
) {
}
```

使用 JPQL DTO 投影查询需要展示的字段。

文件位置：`src/main/java/io/github/atengk/module/user/repository/UserRoleQueryRepository.java`

```java
package io.github.atengk.module.user.repository;

import io.github.atengk.module.user.vo.UserRolePageVO;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.Query;

/**
 * 用户角色查询 Repository
 *
 * @author Ateng
 * @since 2026-05-04
 */
public interface UserRoleQueryRepository {

    /**
     * 分页查询用户角色展示数据
     *
     * @param keyword 关键字
     * @param pageable 分页参数
     * @return 分页结果
     */
    @Query("""
            select new io.github.atengk.module.user.vo.UserRolePageVO(
                u.id,
                u.username,
                u.nickname,
                r.id,
                r.roleName
            )
            from UserEntity u
            left join u.roles r
            where u.deleted = false
              and (:keyword is null or u.username like concat('%', :keyword, '%')
                   or u.nickname like concat('%', :keyword, '%'))
            """)
    Page<UserRolePageVO> pageUserRole(String keyword, Pageable pageable);

}
```

对于详情查询，可以使用 `join fetch` 一次性加载需要的关联对象。

```java
@Query("""
        select distinct u
        from UserEntity u
        left join fetch u.roles
        where u.id = :id
          and u.deleted = false
        """)
Optional<UserEntity> findDetailWithRoles(Long id);
```

`join fetch` 不建议随意用于多集合分页查询。对集合关联做分页时，数据库分页和 Hibernate 去重可能产生结果不稳定或内存分页风险。分页列表更推荐 DTO 投影或两阶段查询：先分页查主表 ID，再根据 ID 批量查关联数据并组装。

### EntityGraph 使用

`EntityGraph` 是 JPA 提供的声明式抓取图能力，可以在不修改实体默认懒加载配置的情况下，为某个查询指定需要额外加载的关联属性。它适合详情页、编辑页、导出等需要一次性加载关联数据的场景。

假设用户实体中有角色集合：

```java
@ManyToMany(fetch = FetchType.LAZY)
@JoinTable(
        name = "sys_user_role",
        joinColumns = @JoinColumn(name = "user_id"),
        inverseJoinColumns = @JoinColumn(name = "role_id")
)
private Set<RoleEntity> roles = new HashSet<>();
```

可以在 Repository 方法上使用 `@EntityGraph`。

文件位置：`src/main/java/io/github/atengk/module/user/repository/UserRepository.java`

```java
package io.github.atengk.module.user.repository;

import io.github.atengk.module.user.entity.UserEntity;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

/**
 * 用户 Repository
 *
 * @author Ateng
 * @since 2026-05-04
 */
public interface UserRepository extends JpaRepository<UserEntity, Long> {

    /**
     * 查询用户并加载角色
     *
     * @param id 用户 ID
     * @return 用户实体
     */
    @EntityGraph(attributePaths = {"roles"})
    Optional<UserEntity> findWithRolesById(Long id);

}
```

Service 中调用时，角色集合会在查询用户时一并加载。

```java
UserEntity user = userRepository.findWithRolesById(id)
        .filter(item -> !Boolean.TRUE.equals(item.getDeleted()))
        .orElseThrow(() -> new IllegalArgumentException("用户不存在"));

int roleCount = user.getRoles().size();
log.info("查询用户角色成功，用户ID：{}，角色数量：{}", id, roleCount);
```

如果需要复用抓取图，也可以在实体类上定义命名实体图。

文件位置：`src/main/java/io/github/atengk/module/user/entity/UserEntity.java`

```java
package io.github.atengk.module.user.entity;

import io.github.atengk.common.entity.FullAuditableBaseEntity;
import io.github.atengk.module.role.entity.RoleEntity;
import io.github.atengk.module.user.enums.UserStatusEnum;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EntityGraph;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.JoinTable;
import jakarta.persistence.ManyToMany;
import jakarta.persistence.NamedAttributeNode;
import jakarta.persistence.NamedEntityGraph;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

import java.util.HashSet;
import java.util.Set;

/**
 * 用户实体
 *
 * @author Ateng
 * @since 2026-05-04
 */
@Getter
@Setter
@Entity
@Table(name = "sys_user")
@NamedEntityGraph(
        name = "UserEntity.roles",
        attributeNodes = {
                @NamedAttributeNode("roles")
        }
)
public class UserEntity extends FullAuditableBaseEntity {

    /**
     * 用户名
     */
    @Column(name = "username", nullable = false, length = 64, unique = true)
    private String username;

    /**
     * 用户昵称
     */
    @Column(name = "nickname", nullable = false, length = 64)
    private String nickname;

    /**
     * 用户状态
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 32)
    private UserStatusEnum status;

    /**
     * 是否删除
     */
    @Column(name = "deleted", nullable = false)
    private Boolean deleted = Boolean.FALSE;

    /**
     * 用户角色
     */
    @ManyToMany(fetch = FetchType.LAZY)
    @JoinTable(
            name = "sys_user_role",
            joinColumns = @JoinColumn(name = "user_id"),
            inverseJoinColumns = @JoinColumn(name = "role_id")
    )
    private Set<RoleEntity> roles = new HashSet<>();

}
```

Repository 中引用命名实体图：

```java
@EntityGraph(value = "UserEntity.roles")
Optional<UserEntity> findGraphById(Long id);
```

`EntityGraph` 的使用建议：

| 场景                       | 建议                     |
| -------------------------- | ------------------------ |
| 查询详情并加载少量关联     | 适合使用                 |
| 分页列表加载单个多对一关联 | 可以考虑                 |
| 分页列表加载集合关联       | 谨慎使用                 |
| 多个集合同时加载           | 容易产生笛卡尔积，不建议 |
| 接口直接返回实体           | 不建议，应转换为 VO      |

`EntityGraph` 不是万能性能优化工具。它解决的是“某个查询需要加载哪些关联”的问题，不应替代合理的查询模型和 DTO 设计。

### 批量操作优化

JPA 的批量操作需要特别注意持久化上下文。大量实体被 `saveAll` 后，会被 EntityManager 管理，如果不分批刷新和清理，容易造成内存占用升高。批量更新如果使用实体逐个修改，也可能产生大量 SQL。

先在配置中开启 Hibernate 批处理参数。

文件位置：`src/main/resources/application.yml`

```yaml
spring:
  jpa:
    properties:
      hibernate:
        jdbc:
          # JDBC 批处理大小，根据数据库性能和业务压力调整
          batch_size: 100
        # 插入语句排序，有利于同类 SQL 合并批处理
        order_inserts: true
        # 更新语句排序，有利于同类 SQL 合并批处理
        order_updates: true
        # 批量更新存在版本字段时可开启，按项目情况使用
        batch_versioned_data: true
```

对于大批量新增，可以直接使用 `EntityManager` 分批 `persist`、`flush`、`clear`。

文件位置：`src/main/java/io/github/atengk/module/user/service/impl/UserImportService.java`

```java
package io.github.atengk.module.user.service.impl;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.ObjectUtil;
import io.github.atengk.module.user.dto.UserCreateRequest;
import io.github.atengk.module.user.entity.UserEntity;
import io.github.atengk.module.user.enums.UserStatusEnum;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * 用户导入服务
 *
 * @author Ateng
 * @since 2026-05-04
 */
@Slf4j
@Service
public class UserImportService {

    private static final int BATCH_SIZE = 500;

    @PersistenceContext
    private EntityManager entityManager;

    /**
     * 批量导入用户
     *
     * @param requests 用户新增参数列表
     * @return 导入数量
     */
    @Transactional(rollbackFor = Exception.class)
    public int importUsers(List<UserCreateRequest> requests) {
        if (CollUtil.isEmpty(requests)) {
            return 0;
        }

        int total = 0;
        for (int i = 0; i < requests.size(); i++) {
            UserEntity entity = toEntity(requests.get(i));
            entityManager.persist(entity);
            total++;

            if (total % BATCH_SIZE == 0) {
                entityManager.flush();
                entityManager.clear();
                log.info("用户导入分批提交完成，累计数量：{}", total);
            }
        }

        entityManager.flush();
        entityManager.clear();
        log.info("用户导入完成，总数量：{}", total);
        return total;
    }

    private UserEntity toEntity(UserCreateRequest request) {
        if (ObjectUtil.isNull(request)) {
            throw new IllegalArgumentException("用户导入数据不能为空");
        }

        UserEntity entity = new UserEntity();
        entity.setUsername(request.username());
        entity.setNickname(request.nickname());
        entity.setPhone(request.phone());
        entity.setEmail(request.email());
        entity.setStatus(ObjectUtil.defaultIfNull(request.status(), UserStatusEnum.ENABLED));
        entity.setDeleted(Boolean.FALSE);
        return entity;
    }

}
```

对于批量更新，优先使用 JPQL update，避免先查出大量实体再逐个修改。

文件位置：`src/main/java/io/github/atengk/module/user/repository/UserRepository.java`

```java
package io.github.atengk.module.user.repository;

import io.github.atengk.module.user.entity.UserEntity;
import io.github.atengk.module.user.enums.UserStatusEnum;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;

import java.time.LocalDateTime;
import java.util.Collection;

/**
 * 用户 Repository
 *
 * @author Ateng
 * @since 2026-05-04
 */
public interface UserRepository extends JpaRepository<UserEntity, Long> {

    /**
     * 批量修改用户状态
     *
     * @param ids 用户 ID 集合
     * @param status 用户状态
     * @param updateTime 更新时间
     * @return 影响行数
     */
    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("""
            update UserEntity u
            set u.status = :status,
                u.updateTime = :updateTime
            where u.id in :ids
              and u.deleted = false
            """)
    int batchUpdateStatus(Collection<Long> ids, UserStatusEnum status, LocalDateTime updateTime);

}
```

批量更新 Service 示例：

文件位置：`src/main/java/io/github/atengk/module/user/service/impl/UserBatchUpdateService.java`

```java
package io.github.atengk.module.user.service.impl;

import cn.hutool.core.collection.CollUtil;
import io.github.atengk.module.user.enums.UserStatusEnum;
import io.github.atengk.module.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Collection;

/**
 * 用户批量更新服务
 *
 * @author Ateng
 * @since 2026-05-04
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class UserBatchUpdateService {

    private final UserRepository userRepository;

    /**
     * 批量禁用用户
     *
     * @param ids 用户 ID 集合
     * @return 影响行数
     */
    @Transactional(rollbackFor = Exception.class)
    public int disableUsers(Collection<Long> ids) {
        if (CollUtil.isEmpty(ids)) {
            return 0;
        }

        int count = userRepository.batchUpdateStatus(ids, UserStatusEnum.DISABLED, LocalDateTime.now());
        log.info("批量禁用用户完成，影响行数：{}", count);
        return count;
    }

}
```

批量操作优化建议：

| 场景             | 推荐方式                                            |
| ---------------- | --------------------------------------------------- |
| 几十条以内新增   | `saveAll` 即可                                      |
| 几百到几千条新增 | 配置 Hibernate batch，并分批 `saveAllAndFlush`      |
| 更大规模导入     | 使用 `EntityManager` 分批 `persist + flush + clear` |
| 大量状态变更     | 使用 JPQL update                                    |
| 复杂批量 SQL     | 使用 Native SQL、JdbcTemplate 或数据库批处理        |
| 批量删除         | 优先逻辑删除，必要时使用 JPQL update                |
| 超大数据同步     | 不建议完全依赖 JPA，考虑批处理框架或数据库导入工具  |

性能优化的核心规则是：查询列表时少加载，查询详情时按需加载，批量写入时分批刷新，复杂统计时不要强行实体化。JPA 可以提升常规业务开发效率，但需要对生成 SQL 和事务边界保持清晰控制。



## 测试验证

测试验证用于确认 Repository 查询、Service 事务逻辑、接口入参校验、SQL 执行行为是否符合预期。Spring Boot 提供了多种测试方式：Repository 层可以使用 `@DataJpaTest` 做 JPA 切片测试，Service 层可以使用 `@SpringBootTest` 加载完整 Spring 容器，接口层可以使用 `MockMvc` 或真实 HTTP 工具联调。`@DataJpaTest` 默认只加载 JPA 相关组件，扫描实体和 Repository，并且测试默认带事务，测试结束后会回滚；如果类路径中有嵌入式数据库，也会自动配置嵌入式数据库。([Home](https://docs.spring.io/spring-boot/reference/testing/spring-boot-applications.html?utm_source=chatgpt.com))

### Repository 单元测试

Repository 测试主要验证方法命名查询、JPQL、Native SQL、分页排序、逻辑删除过滤等数据访问行为。它不应该测试复杂业务流程，重点是确认 Repository 方法能正确生成 SQL 并返回预期结果。

测试环境建议增加 H2 数据库依赖，用于快速执行 Repository 层测试。

文件位置：`pom.xml`

```xml
<dependency>
    <!-- H2 嵌入式数据库，仅用于单元测试和 Repository 切片测试 -->
    <groupId>com.h2database</groupId>
    <artifactId>h2</artifactId>
    <scope>test</scope>
</dependency>
```

测试环境配置如下。

文件位置：`src/test/resources/application-test.yml`

```yaml
spring:
  datasource:
    # H2 内存数据库，MySQL 模式用于尽量兼容 MySQL SQL 语法
    url: jdbc:h2:mem:jpa_test;MODE=MySQL;DATABASE_TO_LOWER=TRUE;DB_CLOSE_DELAY=-1
    username: sa
    password:
    driver-class-name: org.h2.Driver

  jpa:
    hibernate:
      # 测试环境启动时创建表，测试结束后由内存数据库自动释放
      ddl-auto: create-drop
    # Repository 测试阶段可以开启 SQL 输出
    show-sql: true
    properties:
      hibernate:
        # 格式化 SQL，便于观察
        format_sql: true
        # 使用 H2 方言
        dialect: org.hibernate.dialect.H2Dialect
    # 测试阶段建议关闭 Open EntityManager in View
    open-in-view: false

logging:
  level:
    # 输出 SQL
    org.hibernate.SQL: debug
    # 输出 SQL 参数绑定
    org.hibernate.orm.jdbc.bind: trace
```

如果项目使用了 JPA Auditing，`@DataJpaTest` 场景下需要额外导入审计配置，否则 `@CreatedDate`、`@LastModifiedDate` 可能不会自动填充。下面示例通过 `@Import(JpaAuditingConfig.class)` 引入审计配置。

Repository 测试类如下，用于验证用户名查询、逻辑删除过滤和分页查询。

文件位置：`src/test/java/io/github/atengk/module/user/repository/UserRepositoryTest.java`

```java
package io.github.atengk.module.user.repository;

import io.github.atengk.common.config.JpaAuditingConfig;
import io.github.atengk.module.user.entity.UserEntity;
import io.github.atengk.module.user.enums.UserStatusEnum;
import io.github.atengk.module.user.vo.UserPageVO;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.test.context.ActiveProfiles;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 用户 Repository 测试
 *
 * @author Ateng
 * @since 2026-05-04
 */
@DataJpaTest
@ActiveProfiles("test")
@Import(JpaAuditingConfig.class)
class UserRepositoryTest {

    @Autowired
    private UserRepository userRepository;

    @Test
    @DisplayName("根据用户名查询未删除用户")
    void findByUsernameAndDeletedFalseShouldReturnUser() {
        UserEntity user = buildUser("zhangsan", "张三", UserStatusEnum.ENABLED, Boolean.FALSE);
        userRepository.saveAndFlush(user);

        Optional<UserEntity> optional = userRepository.findByUsernameAndDeletedFalse("zhangsan");

        assertThat(optional).isPresent();
        assertThat(optional.get().getNickname()).isEqualTo("张三");
        assertThat(optional.get().getDeleted()).isFalse();
    }

    @Test
    @DisplayName("逻辑删除用户不应被查询出来")
    void findByUsernameAndDeletedFalseShouldIgnoreDeletedUser() {
        UserEntity user = buildUser("lisi", "李四", UserStatusEnum.ENABLED, Boolean.TRUE);
        userRepository.saveAndFlush(user);

        Optional<UserEntity> optional = userRepository.findByUsernameAndDeletedFalse("lisi");

        assertThat(optional).isEmpty();
    }

    @Test
    @DisplayName("分页查询用户")
    void pageUserShouldReturnPageResult() {
        userRepository.save(buildUser("user01", "用户01", UserStatusEnum.ENABLED, Boolean.FALSE));
        userRepository.save(buildUser("user02", "用户02", UserStatusEnum.DISABLED, Boolean.FALSE));
        userRepository.saveAndFlush(buildUser("admin01", "管理员01", UserStatusEnum.ENABLED, Boolean.FALSE));

        Page<UserPageVO> page = userRepository.pageUser("user", null, PageRequest.of(0, 10));

        assertThat(page.getTotalElements()).isEqualTo(2);
        assertThat(page.getContent()).extracting(UserPageVO::username)
                .containsExactlyInAnyOrder("user01", "user02");
    }

    private UserEntity buildUser(String username, String nickname, UserStatusEnum status, Boolean deleted) {
        UserEntity entity = new UserEntity();
        entity.setUsername(username);
        entity.setNickname(nickname);
        entity.setPhone("13800000000");
        entity.setEmail(username + "@example.com");
        entity.setStatus(status);
        entity.setDeleted(deleted);
        return entity;
    }

}
```

执行测试命令：

```bash
# 执行全部测试
mvn test

# 只执行用户 Repository 测试
mvn -Dtest=UserRepositoryTest test
```

Repository 测试中，H2 适合验证通用 JPA 行为和简单 SQL。涉及 MySQL 专有函数、复杂 Native SQL、索引执行计划、JSON 字段、窗口函数时，不建议只依赖 H2，应使用真实 MySQL、Testcontainers 或测试环境数据库。

### Service 业务测试

Service 测试用于验证业务规则、事务行为、异常分支、逻辑删除、批量处理等内容。与 Repository 测试不同，Service 测试通常需要加载完整 Spring 容器，因此使用 `@SpringBootTest` 更合适。Spring Boot 官方说明，`@SpringBootTest` 会通过 `SpringApplication` 创建测试用的 `ApplicationContext`，适合需要 Spring Boot 完整能力的集成测试。([Home](https://docs.spring.io/spring-boot/reference/testing/spring-boot-applications.html?utm_source=chatgpt.com))

下面示例验证新增用户、重复用户名异常、逻辑删除和分页查询。

文件位置：`src/test/java/io/github/atengk/module/user/service/UserServiceTest.java`

```java
package io.github.atengk.module.user.service;

import io.github.atengk.module.user.dto.UserCreateRequest;
import io.github.atengk.module.user.dto.UserPageQuery;
import io.github.atengk.module.user.dto.UserUpdateRequest;
import io.github.atengk.module.user.enums.UserStatusEnum;
import io.github.atengk.module.user.vo.UserDetailVO;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * 用户 Service 测试
 *
 * @author Ateng
 * @since 2026-05-04
 */
@SpringBootTest
@ActiveProfiles("test")
@Transactional
class UserServiceTest {

    @Autowired
    private UserService userService;

    @Test
    @DisplayName("新增用户成功")
    void createShouldSuccess() {
        UserCreateRequest request = new UserCreateRequest(
                "wangwu",
                "王五",
                "13800000001",
                "wangwu@example.com",
                UserStatusEnum.ENABLED
        );

        Long id = userService.create(request);
        UserDetailVO detail = userService.getDetail(id);

        assertThat(id).isNotNull();
        assertThat(detail.username()).isEqualTo("wangwu");
        assertThat(detail.nickname()).isEqualTo("王五");
    }

    @Test
    @DisplayName("用户名重复时新增失败")
    void createShouldFailWhenUsernameExists() {
        UserCreateRequest request = new UserCreateRequest(
                "zhaoliu",
                "赵六",
                "13800000002",
                "zhaoliu@example.com",
                UserStatusEnum.ENABLED
        );

        userService.create(request);

        assertThatThrownBy(() -> userService.create(request))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("用户名已存在");
    }

    @Test
    @DisplayName("修改用户成功")
    void updateShouldSuccess() {
        Long id = userService.create(new UserCreateRequest(
                "sunqi",
                "孙七",
                "13800000003",
                "sunqi@example.com",
                UserStatusEnum.ENABLED
        ));

        userService.update(new UserUpdateRequest(
                id,
                "孙七-修改",
                "13900000003",
                "sunqi_new@example.com",
                UserStatusEnum.DISABLED
        ));

        UserDetailVO detail = userService.getDetail(id);

        assertThat(detail.nickname()).isEqualTo("孙七-修改");
        assertThat(detail.phone()).isEqualTo("13900000003");
        assertThat(detail.status()).isEqualTo(UserStatusEnum.DISABLED);
    }

    @Test
    @DisplayName("逻辑删除后详情查询失败")
    void deleteShouldMakeDetailUnavailable() {
        Long id = userService.create(new UserCreateRequest(
                "zhouba",
                "周八",
                "13800000004",
                "zhouba@example.com",
                UserStatusEnum.ENABLED
        ));

        userService.delete(id);

        assertThatThrownBy(() -> userService.getDetail(id))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("用户不存在");
    }

    @Test
    @DisplayName("分页查询用户成功")
    void pageShouldSuccess() {
        userService.create(new UserCreateRequest("page01", "分页01", null, null, UserStatusEnum.ENABLED));
        userService.create(new UserCreateRequest("page02", "分页02", null, null, UserStatusEnum.ENABLED));

        var result = userService.page(new UserPageQuery(
                "page",
                UserStatusEnum.ENABLED,
                null,
                null,
                1,
                10
        ));

        assertThat(result.total()).isGreaterThanOrEqualTo(2);
        assertThat(result.records()).isNotEmpty();
    }

}
```

Service 测试类上加 `@Transactional` 时，测试方法结束后事务会回滚，适合保证测试数据不污染环境。若需要验证事务提交后的数据库状态，可以去掉类上的 `@Transactional`，并在测试前后手动清理数据。

### 接口联调测试

接口联调用于验证 Controller 参数绑定、Bean Validation、统一响应、异常处理、HTTP 状态、JSON 返回结构是否符合预期。可以使用 `MockMvc` 做自动化接口测试，也可以使用 `curl`、Postman、Apifox 等工具进行人工联调。

先给出 `MockMvc` 测试示例。

文件位置：`src/test/java/io/github/atengk/module/user/controller/UserControllerTest.java`

```java
package io.github.atengk.module.user.controller;

import cn.hutool.json.JSONUtil;
import io.github.atengk.module.user.dto.UserCreateRequest;
import io.github.atengk.module.user.enums.UserStatusEnum;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.test.web.servlet.MockMvc;

import static org.hamcrest.Matchers.notNullValue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;

/**
 * 用户接口测试
 *
 * @author Ateng
 * @since 2026-05-04
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
class UserControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    @DisplayName("新增用户接口成功")
    void createShouldSuccess() throws Exception {
        UserCreateRequest request = new UserCreateRequest(
                "api_user_01",
                "接口用户01",
                "13800001001",
                "api_user_01@example.com",
                UserStatusEnum.ENABLED
        );

        mockMvc.perform(post("/api/users")
                        .header("X-User-Id", "10001")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(JSONUtil.toJsonStr(request)))
                .andExpect(jsonPath("$.code").value("200"))
                .andExpect(jsonPath("$.data", notNullValue()));
    }

    @Test
    @DisplayName("新增用户参数校验失败")
    void createShouldFailWhenUsernameBlank() throws Exception {
        String body = """
                {
                  "username": "",
                  "nickname": "接口用户02",
                  "phone": "13800001002",
                  "email": "api_user_02@example.com",
                  "status": "ENABLED"
                }
                """;

        mockMvc.perform(post("/api/users")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(jsonPath("$.code").value("PARAM_ERROR"));
    }

    @Test
    @DisplayName("分页查询接口成功")
    void pageShouldSuccess() throws Exception {
        mockMvc.perform(get("/api/users/page")
                        .param("pageNum", "1")
                        .param("pageSize", "10"))
                .andExpect(jsonPath("$.code").value("200"))
                .andExpect(jsonPath("$.data.records").exists())
                .andExpect(jsonPath("$.data.total").exists());
    }

    @Test
    @DisplayName("删除不存在用户返回业务错误")
    void deleteShouldFailWhenUserNotExists() throws Exception {
        mockMvc.perform(delete("/api/users/999999"))
                .andExpect(jsonPath("$.code").value("PARAM_ERROR"));
    }

}
```

接口联调也可以直接使用 `curl`。

新增接口：

```bash
curl -X POST "http://localhost:8080/api/users" \
  -H "Content-Type: application/json" \
  -H "X-User-Id: 10001" \
  -d '{
    "username": "curl_user_01",
    "nickname": "curl用户01",
    "phone": "13800002001",
    "email": "curl_user_01@example.com",
    "status": "ENABLED"
  }'
```

修改接口：

```bash
curl -X PUT "http://localhost:8080/api/users/1" \
  -H "Content-Type: application/json" \
  -H "X-User-Id: 10002" \
  -d '{
    "nickname": "curl用户01-修改",
    "phone": "13900002001",
    "email": "curl_user_01_new@example.com",
    "status": "DISABLED"
  }'
```

详情接口：

```bash
curl -X GET "http://localhost:8080/api/users/1"
```

分页接口：

```bash
curl -X GET "http://localhost:8080/api/users/page?keyword=curl&pageNum=1&pageSize=10"
```

删除接口：

```bash
curl -X DELETE "http://localhost:8080/api/users/1" \
  -H "X-User-Id: 10003"
```

接口联调时重点检查：参数校验是否生效、异常是否被统一处理、逻辑删除后是否无法查询详情、分页总数是否正确、返回对象是否没有暴露 Entity 内部字段。

### SQL 日志验证

SQL 日志验证用于确认 JPA 实际生成的 SQL 是否符合预期。它尤其适合排查 N+1 查询、懒加载触发时机、分页 SQL、JPQL 更新语句、逻辑删除过滤条件是否生效等问题。

开发环境可以打开 Hibernate SQL 和参数绑定日志。

文件位置：`src/main/resources/application-dev.yml`

```yaml
spring:
  jpa:
    # 控制台直接输出 SQL，开发阶段可开启，生产环境不建议开启
    show-sql: false
    properties:
      hibernate:
        # 格式化 SQL
        format_sql: true
        # 控制台高亮 SQL
        highlight_sql: true

logging:
  level:
    # 输出 Hibernate 执行的 SQL
    org.hibernate.SQL: debug
    # 输出 SQL 参数绑定值
    org.hibernate.orm.jdbc.bind: trace
    # 输出事务边界日志，排查事务是否开启
    org.springframework.transaction.interceptor: trace
```

Spring Boot 的数据库初始化文档也说明，可以通过开启 `org.hibernate.SQL` logger 输出 Hibernate schema 或 SQL 创建行为；`ddl-auto` 支持 `none`、`validate`、`update`、`create`、`create-drop`，且真实数据库默认通常不是自动建表模式，因此建议显式配置。([Home](https://docs.spring.io/spring-boot/how-to/data-initialization.html?utm_source=chatgpt.com))

正常查询用户详情时，日志中应看到类似 SQL：

```sql
select
    u1_0.id,
    u1_0.create_time,
    u1_0.create_user_id,
    u1_0.deleted,
    u1_0.email,
    u1_0.nickname,
    u1_0.phone,
    u1_0.status,
    u1_0.update_time,
    u1_0.update_user_id,
    u1_0.username
from
    sys_user u1_0
where
    u1_0.id = ?
```

如果执行分页查询，应重点检查是否包含逻辑删除过滤条件：

```sql
where
    u1_0.deleted = false
```

如果访问详情时加载角色，应检查是否出现大量重复查询。出现如下模式时，通常说明存在 N+1 查询：

```text
select * from sys_user where deleted = false
select * from sys_role where user_id = ?
select * from sys_role where user_id = ?
select * from sys_role where user_id = ?
```

SQL 日志验证建议只在开发、测试环境开启。生产环境开启参数绑定日志会输出大量日志，甚至可能暴露敏感数据。

## 常见问题

Spring Data JPA 常见问题主要集中在表结构自动生成、懒加载异常、事务失效和 JSON 序列化循环引用。多数问题不是 JPA 本身不可用，而是实体关系、事务边界、加载策略和接口返回模型没有设计清楚。

### 表结构自动生成问题

表结构自动生成问题通常与 `spring.jpa.hibernate.ddl-auto` 配置有关。常见现象包括：启动后没有建表、字段没有更新、表被清空、生产环境表结构被意外修改、`data.sql` 执行顺序异常等。

常见配置含义如下：

| 配置值        | 行为                       | 建议使用环境        |
| ------------- | -------------------------- | ------------------- |
| `none`        | 不处理表结构               | 生产环境常用        |
| `validate`    | 启动时校验表结构，不修改表 | 生产环境推荐        |
| `update`      | 根据实体尝试更新表结构     | 开发环境可用        |
| `create`      | 启动时删除并重建表         | 临时测试            |
| `create-drop` | 启动时建表，关闭时删表     | 单元测试、临时 Demo |

Spring Boot 官方建议数据库 schema 生成机制保持单一；如果使用 Flyway 或 Liquibase，就不建议同时混用 Hibernate 自动建表、`schema.sql`、`data.sql` 等多套初始化机制。([Home](https://docs.spring.io/spring-boot/how-to/data-initialization.html?utm_source=chatgpt.com))

开发环境可以使用：

```yaml
spring:
  jpa:
    hibernate:
      # 开发环境可用，实体变更后尝试更新表结构
      ddl-auto: update
```

生产环境建议使用：

```yaml
spring:
  jpa:
    hibernate:
      # 生产环境只校验表结构，不自动修改表
      ddl-auto: validate
```

如果已经使用 Flyway，建议这样配置：

```yaml
spring:
  jpa:
    hibernate:
      # 表结构由 Flyway 管理，Hibernate 不自动处理表结构
      ddl-auto: validate

  flyway:
    # 启用 Flyway 数据库迁移
    enabled: true
    # 迁移脚本路径
    locations: classpath:db/migration
```

常见问题与处理方式：

| 问题                 | 原因                                     | 处理方式                                   |
| -------------------- | ---------------------------------------- | ------------------------------------------ |
| 启动后没有建表       | `ddl-auto=none` 或数据库不是嵌入式数据库 | 开发环境显式设置 `update` 或 `create-drop` |
| 表被清空             | 使用了 `create` 或 `create-drop`         | 非临时测试环境禁止使用                     |
| 字段类型不符合预期   | Hibernate 自动推断和数据库设计不一致     | 使用迁移脚本显式建表                       |
| `data.sql` 插入失败  | 执行顺序早于 JPA 建表                    | 使用 Flyway，或配置延迟初始化              |
| 生产环境表结构不可控 | 使用了 `update`                          | 改为 `validate` 并使用 Flyway/Liquibase    |

实际项目建议：开发初期可以用 `update` 提高效率；进入联调、测试、生产后，表结构变更应通过 SQL 迁移脚本管理，不再依赖 Hibernate 自动改表。

### 懒加载异常问题

懒加载异常常见报错是：

```text
org.hibernate.LazyInitializationException: could not initialize proxy - no Session
```

它通常发生在事务结束后访问懒加载属性。例如 Service 查询出用户实体后直接返回给 Controller，Controller 或 Jackson 序列化时访问 `user.getRoles()`，此时 EntityManager 已关闭，懒加载无法继续执行。

常见错误写法：

```java
@GetMapping("/{id}")
public ApiResult<UserEntity> detail(@PathVariable Long id) {
    return ApiResult.success(userRepository.findById(id).orElseThrow());
}
```

这种写法有三个问题：Controller 直接操作 Repository；直接返回 Entity；JSON 序列化可能触发懒加载。

推荐写法是在 Service 的事务内查询并转换为 VO。

```java
@Transactional(readOnly = true)
public UserDetailVO getDetail(Long id) {
    UserEntity entity = userRepository.findWithRolesById(id)
            .filter(item -> !Boolean.TRUE.equals(item.getDeleted()))
            .orElseThrow(() -> new IllegalArgumentException("用户不存在"));

    return new UserDetailVO(
            entity.getId(),
            entity.getUsername(),
            entity.getNickname(),
            entity.getPhone(),
            entity.getEmail(),
            entity.getStatus(),
            entity.getCreateTime(),
            entity.getUpdateTime()
    );
}
```

如果详情确实需要加载角色，可以使用 `@EntityGraph` 或 `join fetch`。

```java
@EntityGraph(attributePaths = {"roles"})
Optional<UserEntity> findWithRolesById(Long id);
```

Spring Boot Web 应用默认会注册 Open EntityManager in View，以允许视图层进行懒加载；如果不希望这种行为，应将 `spring.jpa.open-in-view` 设置为 `false`。([Home](https://docs.spring.io/spring-boot/docs/3.0.6/reference/htmlsingle?utm_source=chatgpt.com))

推荐配置：

```yaml
spring:
  jpa:
    # 关闭视图层懒加载，避免 Controller/Jackson 序列化阶段隐式执行 SQL
    open-in-view: false
```

处理懒加载异常的推荐顺序：

| 优先级 | 处理方式                             | 说明                         |
| ------ | ------------------------------------ | ---------------------------- |
| 1      | Service 内转换 VO                    | 最推荐，接口返回可控         |
| 2      | 使用 `@EntityGraph`                  | 适合详情查询加载少量关联     |
| 3      | 使用 JPQL `join fetch`               | 适合明确的关联查询           |
| 4      | DTO 投影                             | 适合列表页和只读展示         |
| 不推荐 | 开启 Open EntityManager in View 兜底 | 容易隐藏隐式 SQL 和 N+1 问题 |

### 事务失效问题

事务失效通常是 Spring AOP 代理没有生效，或者异常类型没有触发回滚。Spring Data JPA Repository 的基础 CRUD 方法默认已有事务配置，读操作通常是只读事务，其他操作是普通事务；但多个 Repository 组合、复杂业务逻辑、批量处理等场景应在 Service 层定义事务边界。([Home](https://docs.spring.io/spring-data/jpa/reference/jpa/transactions.html?utm_source=chatgpt.com))

常见事务失效场景如下：

| 场景                             | 原因                            | 处理方式                                   |
| -------------------------------- | ------------------------------- | ------------------------------------------ |
| 同类内部方法调用                 | 没有经过 Spring 代理            | 把事务方法放到另一个 Service，或由外部调用 |
| `private` 方法加事务             | Spring AOP 默认无法代理私有方法 | 事务加到 `public` Service 方法上           |
| 抛出受检异常不回滚               | 默认只回滚运行时异常            | 使用 `rollbackFor = Exception.class`       |
| 方法不是 Spring Bean             | 没有被容器管理                  | 使用 `@Service`、`@Component` 注册 Bean    |
| 异常被 catch 后吞掉              | 事务感知不到异常                | 重新抛出异常或手动标记回滚                 |
| `@Transactional` 加在 Controller | 事务边界过大                    | 放到 Service 层                            |
| 多线程中使用事务                 | 新线程不继承当前事务            | 在线程内重新开启事务或改为同步处理         |

错误示例：同类内部调用导致事务不生效。

```java
@Service
public class UserWrongService {

    public void importUsers() {
        // 当前调用没有经过 Spring 事务代理
        this.saveUser();
    }

    @Transactional(rollbackFor = Exception.class)
    public void saveUser() {
        // 事务可能不生效
    }

}
```

推荐写法：事务放在外部可代理的 Service 方法上。

文件位置：`src/main/java/io/github/atengk/module/user/service/impl/UserTransactionService.java`

```java
package io.github.atengk.module.user.service.impl;

import cn.hutool.core.collection.CollUtil;
import io.github.atengk.module.user.dto.UserCreateRequest;
import io.github.atengk.module.user.entity.UserEntity;
import io.github.atengk.module.user.enums.UserStatusEnum;
import io.github.atengk.module.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * 用户事务示例服务
 *
 * @author Ateng
 * @since 2026-05-04
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class UserTransactionService {

    private final UserRepository userRepository;

    @Transactional(rollbackFor = Exception.class)
    public void importUsers(List<UserCreateRequest> requests) {
        if (CollUtil.isEmpty(requests)) {
            return;
        }

        for (UserCreateRequest request : requests) {
            UserEntity entity = new UserEntity();
            entity.setUsername(request.username());
            entity.setNickname(request.nickname());
            entity.setPhone(request.phone());
            entity.setEmail(request.email());
            entity.setStatus(request.status() == null ? UserStatusEnum.ENABLED : request.status());
            entity.setDeleted(Boolean.FALSE);
            userRepository.save(entity);
        }

        log.info("用户导入事务执行完成，数量：{}", requests.size());
    }

}
```

如果必须捕获异常，也要重新抛出，避免事务被误判为正常完成。

```java
@Transactional(rollbackFor = Exception.class)
public void updateUserStatus(Long id) {
    try {
        // 执行业务更新
    } catch (Exception e) {
        log.error("修改用户状态失败，用户ID：{}", id, e);
        throw e;
    }
}
```

事务排查时可以打开日志：

```yaml
logging:
  level:
    # 输出事务拦截器日志，用于确认方法是否进入事务代理
    org.springframework.transaction.interceptor: trace
```

如果日志中看不到目标 Service 方法的事务开启和提交信息，优先检查方法是否为 Spring Bean 的 `public` 方法、是否发生了同类内部调用、是否被代理对象调用。

### JSON 序列化循环引用问题

JSON 序列化循环引用通常发生在双向实体关系中。例如 `UserEntity` 中有 `roles`，`RoleEntity` 中又有 `users`。当 Controller 直接返回实体时，Jackson 会不断序列化彼此引用，最终可能出现栈溢出、响应过大或懒加载异常。

典型问题结构：

```java
public class UserEntity {

    private Set<RoleEntity> roles;

}

public class RoleEntity {

    private Set<UserEntity> users;

}
```

如果直接返回 `UserEntity`：

```java
@GetMapping("/{id}")
public UserEntity detail(@PathVariable Long id) {
    return userRepository.findById(id).orElseThrow();
}
```

可能出现：

```text
StackOverflowError
Could not write JSON
LazyInitializationException
```

最推荐的处理方式是不要直接返回 Entity，而是返回 VO。

文件位置：`src/main/java/io/github/atengk/module/user/vo/UserRoleDetailVO.java`

```java
package io.github.atengk.module.user.vo;

import java.util.List;

/**
 * 用户角色详情 VO
 *
 * @author Ateng
 * @since 2026-05-04
 */
public record UserRoleDetailVO(
        Long id,
        String username,
        String nickname,
        List<RoleItemVO> roles
) {

    /**
     * 角色项 VO
     *
     * @author Ateng
     * @since 2026-05-04
     */
    public record RoleItemVO(
            Long id,
            String roleCode,
            String roleName
    ) {
    }

}
```

Service 中显式组装返回结构。

```java
@Transactional(readOnly = true)
public UserRoleDetailVO getUserRoleDetail(Long id) {
    UserEntity user = userRepository.findWithRolesById(id)
            .filter(item -> !Boolean.TRUE.equals(item.getDeleted()))
            .orElseThrow(() -> new IllegalArgumentException("用户不存在"));

    List<UserRoleDetailVO.RoleItemVO> roles = user.getRoles()
            .stream()
            .map(role -> new UserRoleDetailVO.RoleItemVO(
                    role.getId(),
                    role.getRoleCode(),
                    role.getRoleName()
            ))
            .toList();

    return new UserRoleDetailVO(
            user.getId(),
            user.getUsername(),
            user.getNickname(),
            roles
    );
}
```

如果只是临时处理序列化问题，也可以使用 Jackson 注解，例如 `@JsonIgnore`、`@JsonManagedReference`、`@JsonBackReference`，但这会把接口返回结构和实体模型耦合在一起，不适合复杂业务系统长期维护。

示例：

```java
@JsonIgnore
@ManyToMany(mappedBy = "roles", fetch = FetchType.LAZY)
private Set<UserEntity> users = new HashSet<>();
```

处理 JSON 循环引用的推荐顺序：

| 优先级 | 处理方式                                       | 说明                         |
| ------ | ---------------------------------------------- | ---------------------------- |
| 1      | Entity 转 VO                                   | 最推荐，接口结构清晰         |
| 2      | DTO 投影查询                                   | 适合列表和只读接口           |
| 3      | `@JsonIgnore`                                  | 临时或内部接口可用           |
| 4      | `@JsonManagedReference` / `@JsonBackReference` | 简单父子关系可用             |
| 不推荐 | 直接返回双向关联 Entity                        | 容易产生循环引用和懒加载问题 |

总结来说，JPA 实体应该主要服务于 ORM 映射和业务状态变更，REST API 返回对象应该使用 DTO 或 VO。这样可以同时规避循环引用、懒加载异常、字段泄露和隐式 SQL。