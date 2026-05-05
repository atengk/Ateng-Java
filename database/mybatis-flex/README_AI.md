# MyBatis-Flex

## 技术概述

本部分用于说明 MyBatis-Flex 在 Spring Boot 3 项目中的定位、核心能力，以及与常见 MyBatis 增强框架的差异。MyBatis-Flex 本质上仍然基于 MyBatis，适合作为业务系统中的数据访问层增强工具，用来减少重复 CRUD、分页、条件查询和动态 SQL 编写工作。

### MyBatis-Flex 简介

MyBatis-Flex 是一个 MyBatis 增强框架，官方定位为“轻量、高性能、灵活”的持久层框架。它保留 MyBatis 的 Mapper、XML、SQL 可控性，同时提供了 `BaseMapper`、`QueryWrapper`、分页查询、逻辑删除、乐观锁、多数据源、数据填充、SQL 审计等常用能力。官方文档强调 MyBatis-Flex 除 MyBatis 外没有额外第三方依赖，不依赖拦截器实现核心功能，运行过程中也不做 SQL 解析，因此更便于排查 SQL 执行链路和性能问题。

在 Spring Boot 3 项目中，MyBatis-Flex 通常承担以下职责：

1. 简化单表 CRUD 开发，Mapper 继承 `BaseMapper<T>` 后即可获得常见增删改查能力。
2. 使用 `QueryWrapper` 构建类型更安全、IDE 友好的条件查询，减少字符串字段名硬编码。
3. 兼容 MyBatis 原生 XML Mapper，复杂 SQL、报表 SQL、多表统计 SQL 仍然可以放在 XML 中维护。
4. 支持分页、逻辑删除、乐观锁、多数据源、动态表名、多租户等企业应用常见能力。
5. 支持 `Db + Row` 模式，在部分场景下可以不定义实体类，直接面向数据库行数据操作。

简单理解，MyBatis-Flex 不是替代 MyBatis，而是在 MyBatis 之上补齐日常业务开发中高频、重复、易出错的部分。

### 与 MyBatis-Plus 的差异

MyBatis-Flex 和 MyBatis-Plus 都属于 MyBatis 增强框架，都能提供基础 CRUD、分页查询、逻辑删除、乐观锁、数据填充等能力。差异主要体现在设计取向、查询表达方式、多表查询能力和依赖模型上。MyBatis-Flex 官方对比文档中列出了两者在多表查询、复合主键、无 SQL 解析分页、`Db + Row`、SQL 审计、多数据源事务等方面的差异；这些结论应按官方对比口径理解，实际选型仍需结合项目验证。

| 对比维度 | MyBatis-Flex                                                 | MyBatis-Plus                                                 |
| -------- | ------------------------------------------------------------ | ------------------------------------------------------------ |
| 框架定位 | 更强调轻量、灵活、SQL 可控、QueryWrapper 表达能力            | 生态成熟，使用群体大，CRUD 能力稳定                          |
| 依赖模型 | 官方强调除 MyBatis 外无额外第三方依赖                        | 功能体系更完整，但整体封装更重                               |
| 查询构建 | 使用 `QueryWrapper`，配合 APT 生成的表定义类，字段可获得 IDE 提示 | 常用 `QueryWrapper`、`LambdaQueryWrapper`，Lambda 查询能减少字段硬编码 |
| 多表查询 | 原生支持 `from` 多表、`left join`、`inner join`、`union`、`union all` 等写法 | 标准能力更偏单表，复杂多表通常依赖 XML、自写 SQL 或扩展方案  |
| 复合主键 | 官方对比中标注支持多主键、复合主键                           | 官方对比中标注不支持复合主键                                 |
| SQL 控制 | 更接近原生 SQL 结构，适合复杂查询表达                        | 单表 CRUD 和常规条件查询开发效率高                           |
| XML 兼容 | 兼容 MyBatis 原生 XML                                        | 兼容 MyBatis 原生 XML                                        |
| 适合团队 | 希望保留 SQL 可控性，同时提升复杂查询开发效率的团队          | 已有 MyBatis-Plus 技术栈、偏标准 CRUD 的团队                 |

示例上，MyBatis-Flex 的查询通常可以写成接近 SQL 结构的链式形式，并通过 APT 生成的 `ACCOUNT` 这类表定义对象引用字段；官方快速开始也说明 `ACCOUNT` 是 MyBatis-Flex 通过 APT 自动生成后静态导入使用的。

```java
QueryWrapper queryWrapper = QueryWrapper.create()
        .select()
        .where(ACCOUNT.AGE.eq(18));

Account account = accountMapper.selectOneByQuery(queryWrapper);
```

选型建议上，如果项目主要是标准后台管理系统、以单表 CRUD 为主，MyBatis-Plus 的资料、插件和团队熟悉度通常更有优势。如果项目中复杂查询、多表关联、动态 SQL、SQL 可读性和 SQL 执行链路可控性更重要，可以优先考虑 MyBatis-Flex。

### 适用场景

MyBatis-Flex 适合用于 Spring Boot 3 后端服务中的数据访问层，尤其适合既需要提高开发效率，又不希望完全丢失 SQL 控制权的项目。官方文档明确列出了基础 CRUD、分页查询、自动映射、关联查询、批量操作、链式操作、QueryWrapper、Db + Row、Active Record、IService 等基础功能，也提供逻辑删除、乐观锁、数据填充、SQL 审计、多数据源、动态表名、多租户等核心功能。([MyBatis-Flex](https://mybatis-flex.com/zh/intro/getting-started.html))

适合使用 MyBatis-Flex 的场景包括：

1. 后台管理系统、业务中台、运营平台等典型 CRUD 项目。
2. 存在较多动态条件查询、分页查询、排序查询的业务模块。
3. 需要频繁编写多表关联、子查询、聚合查询，但又不希望全部堆在 XML 中的项目。
4. 需要保留 MyBatis XML 能力，复杂 SQL 仍由开发者直接控制的项目。
5. 需要逻辑删除、乐观锁、数据填充、多租户、多数据源等企业级持久层能力的系统。
6. 团队希望减少字符串字段名硬编码，并希望字段具备 IDE 提示和重构支持。

不太适合的场景包括：

1. 项目已经深度依赖 JPA/Hibernate 的实体关系模型和自动脏检查机制。
2. 团队已经大规模沉淀 MyBatis-Plus 插件、规范、代码生成器和封装基类，迁移成本较高。
3. 系统 SQL 极少、数据访问非常简单，引入增强框架收益有限。
4. 项目对框架生态成熟度、第三方资料数量、招聘匹配度要求高于框架轻量性。

## 环境准备

本部分用于准备 Spring Boot 3 集成 MyBatis-Flex 所需的 JDK、Maven 依赖和数据库连接配置。下面示例默认使用 MySQL 8、Spring Boot 3.5.x、MyBatis-Flex Spring Boot 3 Starter，并采用 Maven 作为构建工具。

### JDK 与 Spring Boot 版本

Spring Boot 3 要求至少使用 Java 17。以 Spring Boot 3.3.x 官方系统要求为例，其要求 Java 17 或更高版本，并要求 Maven 3.6.3 或更高版本；Spring Boot 3.0.x 也从 Java 17 开始。([Spring 企业文档](https://docs.enterprise.spring.io/spring-boot/system-requirements.html?utm_source=chatgpt.com))

推荐版本如下：

| 组件         | 推荐版本  | 说明                                                         |
| ------------ | --------- | ------------------------------------------------------------ |
| JDK          | 17 或 21  | Spring Boot 3 最低要求 Java 17；生产环境可优先选择 LTS 版本  |
| Spring Boot  | 3.5.x     | Spring Boot 3 当前仍适合 Spring Boot 3 项目；如需 Spring Boot 4，应使用对应 boot4 starter |
| Maven        | 3.6.3+    | Spring Boot 3.3.x 明确支持 Maven 3.6.3 或更高版本            |
| MyBatis-Flex | 1.11.7    | Maven Central 当前展示 `mybatis-flex-spring-boot3-starter` 版本为 1.11.7 |
| 数据库       | MySQL 8.x | 示例使用 MySQL；也可换成 PostgreSQL、Oracle、达梦等关系型数据库 |

本地环境可以使用以下命令检查版本：

```bash
# 查看 JDK 版本，Spring Boot 3 需要 Java 17+
java -version

# 查看 Maven 版本，建议使用 3.6.3+
mvn -version
```

### Maven 依赖配置

Spring Boot 3 项目需要使用 `mybatis-flex-spring-boot3-starter`，不能使用 Spring Boot 2 对应的 `mybatis-flex-spring-boot-starter`。MyBatis-Flex 官方快速开始文档明确说明：Spring Boot v3.x 需要将依赖修改为 `mybatis-flex-spring-boot3-starter`；官方示例当前使用版本为 `1.11.7`。([MyBatis-Flex](https://mybatis-flex.com/zh/intro/getting-started.html))

文件位置：`pom.xml`

```xml
<project xmlns="http://maven.apache.org/POM/4.0.0"
         xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
         xsi:schemaLocation="http://maven.apache.org/POM/4.0.0 https://maven.apache.org/xsd/maven-4.0.0.xsd">

    <modelVersion>4.0.0</modelVersion>

    <parent>
        <!-- Spring Boot 3 父工程，统一管理 Spring 生态依赖版本 -->
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-starter-parent</artifactId>
        <version>3.5.13</version>
        <relativePath/>
    </parent>

    <groupId>io.github.atengk</groupId>
    <artifactId>springboot3-mybatis-flex-demo</artifactId>
    <version>1.0.0</version>
    <name>springboot3-mybatis-flex-demo</name>
    <description>Spring Boot 3 集成 MyBatis-Flex 示例项目</description>

    <properties>
        <!-- Spring Boot 3 最低要求 Java 17 -->
        <java.version>17</java.version>

        <!-- MyBatis-Flex Spring Boot 3 Starter 当前可用版本 -->
        <mybatis-flex.version>1.11.7</mybatis-flex.version>

        <!-- Hutool 工具类，后续业务代码中可用于字符串、集合、日期、对象判断等处理 -->
        <hutool.version>5.8.35</hutool.version>
    </properties>

    <dependencies>
        <!-- Web 开发依赖，用于编写 REST API -->
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-web</artifactId>
        </dependency>

        <!-- MyBatis-Flex 对 Spring Boot 3 的自动配置支持 -->
        <dependency>
            <groupId>com.mybatis-flex</groupId>
            <artifactId>mybatis-flex-spring-boot3-starter</artifactId>
            <version>${mybatis-flex.version}</version>
        </dependency>

        <!-- MySQL JDBC 驱动，运行时连接 MySQL 数据库 -->
        <dependency>
            <groupId>com.mysql</groupId>
            <artifactId>mysql-connector-j</artifactId>
            <scope>runtime</scope>
        </dependency>

        <!-- HikariCP 数据库连接池，Spring Boot 默认也常用该连接池 -->
        <dependency>
            <groupId>com.zaxxer</groupId>
            <artifactId>HikariCP</artifactId>
        </dependency>

        <!-- Lombok，减少实体类、DTO、VO 中的 getter/setter 样板代码 -->
        <dependency>
            <groupId>org.projectlombok</groupId>
            <artifactId>lombok</artifactId>
            <optional>true</optional>
        </dependency>

        <!-- Hutool 常用工具包，适合业务参数判断、集合处理、日期处理等场景 -->
        <dependency>
            <groupId>cn.hutool</groupId>
            <artifactId>hutool-all</artifactId>
            <version>${hutool.version}</version>
        </dependency>

        <!-- 单元测试依赖，用于 Mapper、Service、Controller 测试 -->
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-test</artifactId>
            <scope>test</scope>
        </dependency>
    </dependencies>

</project>
```

需要注意，Maven Central 当前展示 `mybatis-flex-spring-boot3-starter` 的最新版本为 `1.11.7`，并给出了对应 Maven 坐标。([Maven Central](https://central.sonatype.com/artifact/com.mybatis-flex/mybatis-flex-spring-boot3-starter))

### 数据库连接配置

数据库连接配置主要放在 `application.yml` 中。MyBatis-Flex 官方快速开始中使用 `spring.datasource` 配置数据源，并在 Spring Boot 启动类上使用 `@MapperScan` 扫描 Mapper 包。([MyBatis-Flex](https://mybatis-flex.com/zh/intro/getting-started.html))

文件位置：`src/main/resources/application.yml`

```yaml
server:
  # 服务端口
  port: 8080

spring:
  application:
    # 应用名称
    name: springboot3-mybatis-flex-demo

  datasource:
    # MySQL 连接地址，根据实际库名、地址、端口修改
    url: jdbc:mysql://localhost:3306/flex_demo?useUnicode=true&characterEncoding=utf8&serverTimezone=Asia/Shanghai&useSSL=false&allowPublicKeyRetrieval=true

    # 数据库用户名
    username: root

    # 数据库密码，生产环境建议通过环境变量或配置中心注入
    password: 123456

    # MySQL 8 JDBC 驱动
    driver-class-name: com.mysql.cj.jdbc.Driver

    hikari:
      # 连接池名称，便于日志和监控识别
      pool-name: HikariPool-MyBatisFlex

      # 最小空闲连接数
      minimum-idle: 5

      # 最大连接数，根据应用并发量和数据库承载能力调整
      maximum-pool-size: 20

      # 连接最大存活时间，单位毫秒
      max-lifetime: 1800000

      # 获取连接超时时间，单位毫秒
      connection-timeout: 30000

mybatis-flex:
  # Mapper XML 文件扫描路径；默认也是 classpath*:/mapper/**/*.xml
  mapper-locations: classpath*:/mapper/**/*.xml

  configuration:
    # 开启下划线转驼峰，数据库 user_name 可映射到 Java userName
    map-underscore-to-camel-case: true

    # 控制台输出 SQL，开发环境可开启；生产环境建议关闭或改用日志框架控制
    log-impl: org.apache.ibatis.logging.stdout.StdOutImpl

  global-config:
    # 是否打印 MyBatis-Flex Banner
    print-banner: true

    # 默认逻辑删除正常值
    normal-value-of-logic-delete: 0

    # 默认逻辑删除已删除值
    deleted-value-of-logic-delete: 1

    # 默认逻辑删除字段名
    logic-delete-column: deleted

logging:
  level:
    # 项目 Mapper 包日志级别，用于观察 SQL 执行情况
    io.github.atengk: debug
```

MyBatis-Flex 的 Spring Boot 配置项主要分为 `datasource`、`configuration`、`global-config`、`admin-config`、`seata-config` 等部分。其中 `mapper-locations` 默认值为 `classpath*:/mapper/**/*.xml`，`configuration` 对应 MyBatis 原生配置，`global-config` 用于配置 MyBatis-Flex 的全局行为，例如 Banner、全局主键策略、逻辑删除值、逻辑删除字段、多租户字段、乐观锁字段等。([MyBatis-Flex](https://mybatis-flex.com/zh/base/configuration.html))

如果项目使用 XML Mapper，建议保持以下目录结构：

```text
src
└── main
    ├── java
    │   └── io/github/atengk
    │       ├── Springboot3MybatisFlexApplication.java
    │       ├── entity
    │       ├── mapper
    │       ├── service
    │       └── controller
    └── resources
        ├── application.yml
        └── mapper
            └── UserMapper.xml
```

启动类中需要扫描 Mapper 接口所在包。

文件位置：`src/main/java/io/github/atengk/Springboot3MybatisFlexApplication.java`

```java
package io.github.atengk;

import com.mybatisflex.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * Spring Boot 3 集成 MyBatis-Flex 启动类
 *
 * @author Ateng
 * @since 2026-05-05
 */
@MapperScan("io.github.atengk.mapper")
@SpringBootApplication
public class Springboot3MybatisFlexApplication {

    /**
     * 启动应用
     *
     * @param args 启动参数
     */
    public static void main(String[] args) {
        SpringApplication.run(Springboot3MybatisFlexApplication.class, args);
    }

}
```

完成以上配置后，项目已经具备 MyBatis-Flex 的基础运行环境。后续可以继续补充实体类、Mapper、Service、Controller、分页查询、事务处理和 SQL 日志验证等章节。



## 基础配置

基础配置主要包含数据源、MyBatis-Flex 运行参数和日志输出三部分。Spring Boot 3 项目中，一般先通过 `application.yml` 完成数据库连接和 MyBatis-Flex 基础参数配置，再根据开发环境决定是否开启 SQL 打印、审计日志等调试能力。

### 数据源配置

数据源配置用于告诉 Spring Boot 和 MyBatis-Flex 应用应该连接哪个数据库。单数据源项目可以直接使用 Spring Boot 标准的 `spring.datasource` 配置；如果项目存在多数据源，可以使用 MyBatis-Flex 提供的 `mybatis-flex.datasource` 配置。MyBatis-Flex 官方文档说明，多数据源配置支持 `druid`、`hikaricp`、`dbcp2`、`beecp` 等数据源，并且可以通过数据源名称进行动态切换。([MyBatis-Flex](https://mybatis-flex.com/zh/core/multi-datasource.html?utm_source=chatgpt.com))

单数据源推荐配置如下。

文件位置：`src/main/resources/application.yml`

```yaml
server:
  # 服务端口
  port: 8080

spring:
  application:
    # 应用名称
    name: springboot3-mybatis-flex-demo

  datasource:
    # MySQL 连接地址，flex_demo 为数据库名称
    url: jdbc:mysql://localhost:3306/flex_demo?useUnicode=true&characterEncoding=utf8&serverTimezone=Asia/Shanghai&useSSL=false&allowPublicKeyRetrieval=true

    # 数据库用户名
    username: root

    # 数据库密码，生产环境建议改为环境变量或配置中心注入
    password: 123456

    # MySQL 8 驱动类
    driver-class-name: com.mysql.cj.jdbc.Driver

    hikari:
      # 连接池名称，便于日志排查
      pool-name: HikariPool-MyBatisFlex

      # 最小空闲连接数
      minimum-idle: 5

      # 最大连接数，根据接口并发量和数据库连接上限调整
      maximum-pool-size: 20

      # 获取连接超时时间，单位毫秒
      connection-timeout: 30000

      # 连接最大生命周期，单位毫秒
      max-lifetime: 1800000

      # 空闲连接最大存活时间，单位毫秒
      idle-timeout: 600000
```

如果项目需要多数据源，可以改用 MyBatis-Flex 的多数据源配置。`master`、`slave` 是数据源名称，后续可以通过 `@UseDataSource("slave")`、`@Table(dataSource = "slave")` 或 `DataSourceKey.use("slave")` 指定数据源。官方文档给出的数据源切换优先级为：`DataSourceKey.use()` 高于方法上的 `@UseDataSource()`，再高于类上的 `@UseDataSource()`，最后是实体类上的 `@Table(dataSource = "...")`。([MyBatis-Flex](https://mybatis-flex.com/zh/core/multi-datasource.html?utm_source=chatgpt.com))

文件位置：`src/main/resources/application.yml`

```yaml
mybatis-flex:
  datasource:
    master:
      # 主库，处理主要写操作
      type: hikari
      url: jdbc:mysql://localhost:3306/flex_demo?useUnicode=true&characterEncoding=utf8&serverTimezone=Asia/Shanghai&useSSL=false&allowPublicKeyRetrieval=true
      username: root
      password: 123456
      driver-class-name: com.mysql.cj.jdbc.Driver
      maximum-pool-size: 20
      minimum-idle: 5

    slave:
      # 从库，适合读多写少场景
      type: hikari
      url: jdbc:mysql://localhost:3306/flex_demo_read?useUnicode=true&characterEncoding=utf8&serverTimezone=Asia/Shanghai&useSSL=false&allowPublicKeyRetrieval=true
      username: root
      password: 123456
      driver-class-name: com.mysql.cj.jdbc.Driver
      maximum-pool-size: 20
      minimum-idle: 5
```

在常规业务系统中，建议先从单数据源开始。只有在确实存在读写分离、租户隔离、业务库拆分或跨库查询场景时，再引入多数据源配置，避免过早增加事务处理和数据一致性复杂度。

### MyBatis-Flex 配置项

MyBatis-Flex 的 Spring Boot 配置主要放在 `mybatis-flex` 节点下。官方配置文档说明，`configuration` 对应 MyBatis 原生配置，`global-config` 对应 MyBatis-Flex 的全局配置，例如全局主键策略、逻辑删除值、逻辑删除字段、Banner 打印、SQL 审计、Seata 配置等。([MyBatis-Flex](https://mybatis-flex.com/zh/base/configuration.html?utm_source=chatgpt.com))

文件位置：`src/main/resources/application.yml`

```yaml
mybatis-flex:
  # Mapper XML 扫描路径，默认也是 classpath*:/mapper/**/*.xml
  mapper-locations: classpath*:/mapper/**/*.xml

  configuration:
    # 开启数据库字段下划线到 Java 属性驼峰的自动映射
    map-underscore-to-camel-case: true

    # 开发环境可以开启 MyBatis 原生 SQL 输出；生产环境建议关闭
    log-impl: org.apache.ibatis.logging.stdout.StdOutImpl

    # 查询结果中字段值为 null 时，是否调用 setter
    call-setters-on-nulls: true

  global-config:
    # 是否打印 MyBatis-Flex Banner 和版本信息
    print-banner: true

    # 逻辑删除正常值
    normal-value-of-logic-delete: 0

    # 逻辑删除已删除值
    deleted-value-of-logic-delete: 1

    # 默认逻辑删除字段名；如果实体字段已使用 @Column(isLogicDelete = true)，这里可作为全局约定
    logic-delete-column: deleted
```

常用配置说明如下。

| 配置项                                        | 作用                       | 建议                                                |
| --------------------------------------------- | -------------------------- | --------------------------------------------------- |
| `mapper-locations`                            | 指定 XML Mapper 文件路径   | XML 放在 `resources/mapper/**/*.xml` 时保持默认即可 |
| `configuration.map-underscore-to-camel-case`  | 下划线字段自动映射驼峰属性 | 建议开启                                            |
| `configuration.log-impl`                      | MyBatis 原生 SQL 日志实现  | 仅开发环境开启                                      |
| `global-config.print-banner`                  | 是否打印框架 Banner        | 开发环境可开启，生产环境可关闭                      |
| `global-config.logic-delete-column`           | 全局逻辑删除字段           | 建议统一为 `deleted` 或 `del_flag`                  |
| `global-config.normal-value-of-logic-delete`  | 未删除状态值               | 通常使用 `0`                                        |
| `global-config.deleted-value-of-logic-delete` | 已删除状态值               | 通常使用 `1`                                        |

如果需要通过 Java 代码扩展 MyBatis-Flex 初始化逻辑，可以实现 `MyBatisFlexCustomizer`。官方文档说明，该接口适合配置全局参数、自定义主键生成器、多租户、动态表名、逻辑删除处理器、SQL 审计、SQL 打印、数据源解密器、自定义方言等能力。([MyBatis-Flex](https://mybatis-flex.com/zh/base/mybatis-flex-customizer.html?utm_source=chatgpt.com))

下面的配置类用于集中管理 MyBatis-Flex 自定义配置，适合后续扩展全局主键、租户、SQL 审计等功能。

文件位置：`src/main/java/io/github/atengk/config/MyBatisFlexConfiguration.java`

```java
package io.github.atengk.config;

import com.mybatisflex.core.FlexGlobalConfig;
import com.mybatisflex.spring.boot.MyBatisFlexCustomizer;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Configuration;

/**
 * MyBatis-Flex 全局配置
 *
 * @author Ateng
 * @since 2026-05-05
 */
@Slf4j
@Configuration
public class MyBatisFlexConfiguration implements MyBatisFlexCustomizer {

    /**
     * 自定义 MyBatis-Flex 全局配置
     *
     * @param globalConfig 全局配置对象
     */
    @Override
    public void customize(FlexGlobalConfig globalConfig) {
        log.info("初始化 MyBatis-Flex 全局配置");
    }

}
```

### 日志输出配置

日志输出主要用于开发阶段查看 SQL、参数、执行耗时和慢 SQL。MyBatis-Flex 支持多种 SQL 打印方式：可以使用 MyBatis 原生 `StdOutImpl`，也可以使用 MyBatis-Flex 内置的 SQL 审计能力，将完整 SQL 和耗时输出到控制台或日志文件。官方 SQL 打印文档说明，`AuditManager` 配合 `ConsoleMessageCollector` 或自定义日志收集器可以输出完整 SQL 和执行耗时。([MyBatis-Flex](https://mybatis-flex.com/zh/core/sql-print.html?utm_source=chatgpt.com))

开发环境可以先使用简单配置。

文件位置：`src/main/resources/application-dev.yml`

```yaml
mybatis-flex:
  configuration:
    # 开发环境打印 SQL 到控制台
    log-impl: org.apache.ibatis.logging.stdout.StdOutImpl

logging:
  level:
    # 项目 Mapper 包日志级别
    io.github.atengk.mapper: debug

    # MyBatis-Flex SQL 审计日志名称
    mybatis-flex-sql: info
```

如果希望日志中输出完整 SQL 和执行耗时，建议使用 MyBatis-Flex 的审计日志方式。该方式比单纯的 MyBatis 原生日志更适合排查接口慢查询。

下面的配置类用于开发环境输出 SQL 审计日志。

文件位置：`src/main/java/io/github/atengk/config/MyBatisFlexSqlLogConfiguration.java`

```java
package io.github.atengk.config;

import com.mybatisflex.core.audit.AuditManager;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;

/**
 * MyBatis-Flex SQL 日志配置
 *
 * @author Ateng
 * @since 2026-05-05
 */
@Slf4j
@Profile("dev")
@Configuration
public class MyBatisFlexSqlLogConfiguration {

    private static final Logger SQL_LOGGER = LoggerFactory.getLogger("mybatis-flex-sql");

    /**
     * 初始化 SQL 审计日志
     */
    public MyBatisFlexSqlLogConfiguration() {
        AuditManager.setAuditEnable(true);
        AuditManager.setMessageCollector(auditMessage -> SQL_LOGGER.info(
                "SQL执行完成，耗时：{}ms，语句：{}",
                auditMessage.getElapsedTime(),
                auditMessage.getFullSql()
        ));
        log.info("已开启 MyBatis-Flex SQL 审计日志");
    }

}
```

生产环境建议不要直接开启完整 SQL 控制台输出，原因是完整 SQL 可能包含手机号、身份证号、Token、业务编号等敏感数据。生产环境如果确实需要排查 SQL，建议按日志级别、链路 ID、慢 SQL 阈值或临时开关进行控制。

## 实体类开发

实体类用于描述 Java 对象与数据库表之间的映射关系。MyBatis-Flex 通过 `@Table` 标识表，通过 `@Id` 标识主键，通过 `@Column` 定义字段映射、逻辑删除、乐观锁、默认值、大字段忽略等行为。官方文档说明，`@Table` 用于描述实体类和数据库表的关系，`@Id` 用于声明主键和主键策略，`@Column` 用于配置字段映射和字段行为。([MyBatis-Flex](https://mybatis-flex.com/zh/core/table.html?utm_source=chatgpt.com))

### 表实体映射

表实体映射的核心是让实体类字段与数据库表字段保持清晰的一一对应关系。默认情况下，MyBatis-Flex 支持驼峰属性到下划线字段的映射，例如 `userName` 对应 `user_name`。如果数据库字段名和 Java 属性名不符合默认规则，可以使用 `@Column("字段名")` 显式指定。

示例表结构如下。

文件位置：`src/main/resources/sql/sys_user.sql`

```sql
CREATE TABLE `sys_user`
(
    `id`          BIGINT      NOT NULL AUTO_INCREMENT COMMENT '主键ID',
    `username`    VARCHAR(64) NOT NULL COMMENT '用户名',
    `nickname`    VARCHAR(64)          DEFAULT NULL COMMENT '用户昵称',
    `mobile`      VARCHAR(20)          DEFAULT NULL COMMENT '手机号',
    `status`      TINYINT     NOT NULL DEFAULT 1 COMMENT '状态：0禁用，1启用',
    `deleted`     TINYINT     NOT NULL DEFAULT 0 COMMENT '逻辑删除：0正常，1删除',
    `version`     INT         NOT NULL DEFAULT 0 COMMENT '乐观锁版本号',
    `create_time` DATETIME    NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time` DATETIME    NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_sys_user_username` (`username`)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COMMENT = '系统用户表';
```

下面的实体类映射 `sys_user` 表，包含主键、普通字段、逻辑删除、乐观锁、插入更新时间字段和非数据库字段。

文件位置：`src/main/java/io/github/atengk/entity/SysUser.java`

```java
package io.github.atengk.entity;

import com.mybatisflex.annotation.Column;
import com.mybatisflex.annotation.Id;
import com.mybatisflex.annotation.KeyType;
import com.mybatisflex.annotation.Table;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 系统用户实体
 *
 * @author Ateng
 * @since 2026-05-05
 */
@Data
@Table(value = "sys_user")
public class SysUser implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * 主键ID
     */
    @Id(keyType = KeyType.Auto)
    private Long id;

    /**
     * 用户名
     */
    private String username;

    /**
     * 用户昵称
     */
    private String nickname;

    /**
     * 手机号
     */
    private String mobile;

    /**
     * 状态：0禁用，1启用
     */
    private Integer status;

    /**
     * 逻辑删除：0正常，1删除
     */
    @Column(isLogicDelete = true)
    private Integer deleted;

    /**
     * 乐观锁版本号
     */
    @Column(version = true)
    private Integer version;

    /**
     * 创建时间
     */
    @Column(value = "create_time", onInsertValue = "now()")
    private LocalDateTime createTime;

    /**
     * 更新时间
     */
    @Column(value = "update_time", onInsertValue = "now()", onUpdateValue = "now()")
    private LocalDateTime updateTime;

    /**
     * 非数据库字段，仅用于接口展示或临时业务处理
     */
    @Column(ignore = true)
    private String statusName;

}
```

`@Table(value = "sys_user")` 用于指定实体类对应的数据库表。官方 `@Table` 文档说明，该注解还支持 `schema`、`camelToUnderline`、`dataSource`、插入监听、更新监听等属性；其中 `camelToUnderline` 默认开启，即默认使用驼峰属性到下划线字段的映射规则。([MyBatis-Flex](https://mybatis-flex.com/zh/core/table.html?utm_source=chatgpt.com))

### 主键策略配置

主键策略决定新增数据时主键由谁生成。MyBatis-Flex 使用 `@Id` 标识主键字段，通过 `keyType` 指定主键生成方式。官方文档列出的 `KeyType` 包括 `Auto`、`Sequence`、`Generator`、`None` 四种；同时，MyBatis-Flex 支持在一个实体类中声明多个 `@Id`，用于多主键或复合主键场景。([MyBatis-Flex](https://mybatis-flex.com/zh/core/id.html?utm_source=chatgpt.com))

| 主键策略            | 说明                         | 适用场景                          |
| ------------------- | ---------------------------- | --------------------------------- |
| `KeyType.Auto`      | 数据库自增主键               | MySQL 自增 ID、简单业务表         |
| `KeyType.None`      | 不自动生成，业务代码自行设置 | 雪花 ID、外部系统 ID、手动赋值 ID |
| `KeyType.Generator` | 使用 MyBatis-Flex 主键生成器 | UUID、自定义 ID 生成规则          |
| `KeyType.Sequence`  | 通过数据库序列生成           | Oracle、PostgreSQL Sequence 场景  |

自增主键写法如下，适合 MySQL 中 `AUTO_INCREMENT` 字段。

```java
@Id(keyType = KeyType.Auto)
private Long id;
```

业务手动赋值主键写法如下，适合在保存前由业务代码生成 ID，例如对接分布式 ID 服务。

```java
@Id(keyType = KeyType.None)
private Long id;
```

UUID 或自定义生成器写法如下，适合不依赖数据库自增的业务表。

```java
@Id(keyType = KeyType.Generator, value = "uuid")
private String id;
```

复合主键可以在同一个实体类中声明多个 `@Id`。这种方式适合关系表、明细表、租户隔离表等场景。

下面的实体类用于演示订单商品关系表的复合主键映射。

文件位置：`src/main/java/io/github/atengk/entity/OrderItem.java`

```java
package io.github.atengk.entity;

import com.mybatisflex.annotation.Id;
import com.mybatisflex.annotation.KeyType;
import com.mybatisflex.annotation.Table;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.math.BigDecimal;

/**
 * 订单商品关系实体
 *
 * @author Ateng
 * @since 2026-05-05
 */
@Data
@Table(value = "order_item")
public class OrderItem implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * 订单ID
     */
    @Id(keyType = KeyType.None)
    private Long orderId;

    /**
     * 商品ID
     */
    @Id(keyType = KeyType.None)
    private Long productId;

    /**
     * 商品数量
     */
    private Integer quantity;

    /**
     * 商品单价
     */
    private BigDecimal price;

}
```

业务系统中建议优先使用数据库自增 ID 或统一的分布式 ID 策略，不建议同一个项目中混用过多主键生成方式。复合主键虽然 MyBatis-Flex 支持，但会增加接口参数、更新条件、缓存 Key 和关联查询的复杂度，应只在关系表或确实需要联合唯一标识的场景中使用。

### 字段映射配置

字段映射配置用于处理数据库字段名、Java 属性名和字段行为之间的差异。MyBatis-Flex 的 `@Column` 支持显式字段名、忽略字段、插入默认值、更新默认值、大字段标识、逻辑删除、乐观锁、`jdbcType`、`typeHandler` 等配置。官方文档特别说明，`onInsertValue` 和 `onUpdateValue` 会直接参与 SQL 拼接，而不是通过 JDBC 参数绑定，因此配置值必须是可信的 SQL 片段。([MyBatis-Flex](https://mybatis-flex.com/zh/core/column.html?utm_source=chatgpt.com))

常见字段映射方式如下。

| 配置方式   | 示例                               | 说明                              |
| ---------- | ---------------------------------- | --------------------------------- |
| 默认映射   | `private String userName;`         | 默认映射到 `user_name`            |
| 指定字段名 | `@Column("login_name")`            | Java 属性和数据库字段不一致时使用 |
| 忽略字段   | `@Column(ignore = true)`           | 字段不参与数据库读写              |
| 逻辑删除   | `@Column(isLogicDelete = true)`    | 删除时更新删除标识，不物理删除    |
| 乐观锁     | `@Column(version = true)`          | 更新时校验版本号并自动递增        |
| 插入默认值 | `@Column(onInsertValue = "now()")` | 新增时由数据库函数生成值          |
| 更新默认值 | `@Column(onUpdateValue = "now()")` | 修改时由数据库函数更新值          |
| 大字段     | `@Column(isLarge = true)`          | APT 生成默认查询列时排除该字段    |

逻辑删除字段示例如下。MyBatis-Flex 官方逻辑删除文档说明，删除操作不会真正删除数据，而是修改逻辑删除字段；默认正常值为 `0`，删除值为 `1`，也可以通过全局配置调整。([MyBatis-Flex](https://mybatis-flex.com/zh/core/logic-delete.html?utm_source=chatgpt.com))

```java
@Column(isLogicDelete = true)
private Integer deleted;
```

乐观锁字段示例如下。官方乐观锁文档说明，一个表中只能有一个 `@Column(version = true)` 字段；更新成功后版本号会自动加 `1`，新增时如果未设置版本号，默认会设置为 `0`。([MyBatis-Flex](https://mybatis-flex.com/zh/core/version.html?utm_source=chatgpt.com))

```java
@Column(version = true)
private Integer version;
```

插入时间和更新时间字段示例如下。该配置适合由数据库统一生成时间，避免应用服务器时间和数据库服务器时间不一致。

```java
@Column(value = "create_time", onInsertValue = "now()")
private LocalDateTime createTime;

@Column(value = "update_time", onInsertValue = "now()", onUpdateValue = "now()")
private LocalDateTime updateTime;
```

非数据库字段示例如下。该字段只用于接口展示、临时计算或业务组装，不会参与 SQL 查询、插入和更新。

```java
@Column(ignore = true)
private String statusName;
```

如果字段内容较大，例如文章正文、JSON 快照、日志详情等，可以标识为大字段。官方 `@Column` 文档说明，标识为 `isLarge = true` 的字段不会被 APT 生成到默认查询列中，适合避免列表查询时默认加载大字段。([MyBatis-Flex](https://mybatis-flex.com/zh/core/column.html?utm_source=chatgpt.com))

```java
@Column(isLarge = true)
private String content;
```

字段映射建议保持以下规范：数据库字段统一使用下划线命名，Java 属性统一使用驼峰命名；通用字段建议统一为 `create_time`、`update_time`、`deleted`、`version`；逻辑删除和乐观锁字段应在所有核心业务表中保持同名同义，避免后续封装通用查询、通用更新和审计逻辑时出现差异。

## Mapper 层开发

Mapper 层负责承接数据库访问逻辑。MyBatis-Flex 中的 Mapper 通常继承 `BaseMapper<T>`，即可获得基础增删改查、条件查询和分页查询能力；对于复杂 SQL，可以继续使用 MyBatis 原生注解或 XML Mapper，不会影响 MyBatis 原生能力。官方文档说明，MyBatis-Flex 内置 `BaseMapper`，提供基础 CRUD、分页查询等方法；同时也兼容 MyBatis 原生注解和 XML 写法。([MyBatis-Flex](https://mybatis-flex.com/zh/base/add-delete-update.html?utm_source=chatgpt.com))

### BaseMapper 使用

`BaseMapper<T>` 是 MyBatis-Flex Mapper 层最常用的基础接口。业务 Mapper 继承后，可以直接使用 `insert`、`insertSelective`、`deleteById`、`update`、`selectOneById`、`selectListByQuery`、`paginate` 等方法。官方查询文档中也说明，`BaseMapper` 提供了 `selectOneById`、`selectOneByQuery`、`selectListByQuery`、`paginate` 等常用查询和分页方法。([MyBatis-Flex](https://mybatis-flex.com/zh/base/query.html?utm_source=chatgpt.com))

文件位置：`src/main/java/io/github/atengk/mapper/SysUserMapper.java`

```java
package io.github.atengk.mapper;

import com.mybatisflex.core.BaseMapper;
import io.github.atengk.entity.SysUser;

/**
 * 系统用户 Mapper
 *
 * @author Ateng
 * @since 2026-05-05
 */
public interface SysUserMapper extends BaseMapper<SysUser> {

}
```

继承 `BaseMapper<SysUser>` 后，可以在 Service 或测试类中直接调用基础方法。

下面代码演示 Mapper 的基础 CRUD 调用方式，适合放在单元测试或 Service 实现中验证 Mapper 是否可用。

```java
// 新增用户，insertSelective 会忽略 null 字段，使数据库默认值生效
SysUser user = new SysUser();
user.setUsername("admin");
user.setNickname("管理员");
user.setMobile("13800000000");
user.setStatus(1);
sysUserMapper.insertSelective(user);

// 根据主键查询
SysUser dbUser = sysUserMapper.selectOneById(user.getId());

// 修改用户，update 默认忽略 null 字段
dbUser.setNickname("系统管理员");
sysUserMapper.update(dbUser);

// 根据主键删除；如果实体配置了逻辑删除字段，则执行逻辑删除
sysUserMapper.deleteById(user.getId());
```

需要注意，`insert` 和 `insertSelective` 的行为不同。官方文档说明，`insert(entity)` 不忽略 `null` 值，`insertSelective(entity)` 会忽略 `null` 值；如果数据库字段设置了默认值，新增时通常优先使用 `insertSelective`。删除方法包括 `deleteById`、`deleteBatchByIds`、`deleteByMap`、`deleteByQuery` 等；更新方法包括 `update`、`updateByQuery`、`updateByCondition` 等。([MyBatis-Flex](https://mybatis-flex.com/zh/base/add-delete-update.html?utm_source=chatgpt.com))

### 自定义查询方法

自定义查询方法适合处理 `BaseMapper` 无法直接表达，或者希望封装为明确业务语义的查询。例如根据用户名查询用户、查询启用用户列表、统计某状态用户数量等。MyBatis-Flex 支持继续使用 MyBatis 原生 `@Select`、`@Insert`、`@Update`、`@Delete` 注解。官方文档说明，在 MyBatis-Flex 中使用 MyBatis 原生注解和原生 MyBatis 的用法一致。([MyBatis-Flex](https://mybatis-flex.com/zh/intro/use-mybatis-native.html?utm_source=chatgpt.com))

文件位置：`src/main/java/io/github/atengk/mapper/SysUserMapper.java`

```java
package io.github.atengk.mapper;

import com.mybatisflex.core.BaseMapper;
import io.github.atengk.entity.SysUser;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

/**
 * 系统用户 Mapper
 *
 * @author Ateng
 * @since 2026-05-05
 */
public interface SysUserMapper extends BaseMapper<SysUser> {

    /**
     * 根据用户名查询用户
     *
     * @param username 用户名
     * @return 用户信息
     */
    @Select("""
            select id,
                   username,
                   nickname,
                   mobile,
                   status,
                   deleted,
                   version,
                   create_time,
                   update_time
            from sys_user
            where username = #{username}
              and deleted = 0
            limit 1
            """)
    SysUser selectByUsername(@Param("username") String username);

    /**
     * 查询指定状态的用户列表
     *
     * @param status 用户状态
     * @return 用户列表
     */
    @Select("""
            select id,
                   username,
                   nickname,
                   mobile,
                   status,
                   deleted,
                   version,
                   create_time,
                   update_time
            from sys_user
            where status = #{status}
              and deleted = 0
            order by id desc
            """)
    List<SysUser> selectListByStatus(@Param("status") Integer status);

}
```

如果查询条件需要动态拼接，建议优先使用 `QueryWrapper`。MyBatis-Flex 官方文档说明，`QueryWrapper` 是构造 SQL 的核心工具，并支持通过 APT 自动生成的表定义类获得字段提示；例如 `ACCOUNT.ID.ge(100)` 中的 `ACCOUNT` 就是由 APT 根据实体类自动生成的表定义对象。([MyBatis-Flex](https://mybatis-flex.com/zh/base/querywrapper.html?utm_source=chatgpt.com))

下面代码演示基于 `QueryWrapper` 的自定义条件查询。示例中的 `SYS_USER` 来自 MyBatis-Flex APT 生成类，默认会在 `target/generated-sources/annotations` 下生成；如果实体类为 `io.github.atengk.entity.SysUser`，默认表定义类通常位于 `io.github.atengk.entity.table.SysUserTableDef`。APT 默认生成表定义辅助类，默认包名规则为 `${entityPackage}.table`。([MyBatis-Flex](https://mybatis-flex.com/zh/others/apt.html?utm_source=chatgpt.com))

文件位置：`src/main/java/io/github/atengk/service/impl/SysUserQueryExample.java`

```java
package io.github.atengk.service.impl;

import cn.hutool.core.util.StrUtil;
import com.mybatisflex.core.query.QueryWrapper;
import io.github.atengk.entity.SysUser;
import io.github.atengk.mapper.SysUserMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;

import static io.github.atengk.entity.table.SysUserTableDef.SYS_USER;

/**
 * 系统用户查询示例
 *
 * @author Ateng
 * @since 2026-05-05
 */
@Component
@RequiredArgsConstructor
public class SysUserQueryExample {

    private final SysUserMapper sysUserMapper;

    /**
     * 根据用户名关键字查询启用用户
     *
     * @param keyword 用户名或昵称关键字
     * @return 用户列表
     */
    public List<SysUser> listEnabledUsers(String keyword) {
        QueryWrapper queryWrapper = QueryWrapper.create()
                .select()
                .from(SYS_USER)
                .where(SYS_USER.STATUS.eq(1))
                .and(SYS_USER.DELETED.eq(0))
                .orderBy(SYS_USER.ID.desc());

        if (StrUtil.isNotBlank(keyword)) {
            queryWrapper.and(
                    SYS_USER.USERNAME.like(keyword)
                            .or(SYS_USER.NICKNAME.like(keyword))
            );
        }

        return sysUserMapper.selectListByQuery(queryWrapper);
    }

}
```

使用 `QueryWrapper` 时，建议将常用动态条件封装到 Service 层，不要在 Controller 中直接拼复杂查询。这样可以避免接口层承担过多 SQL 细节，也便于后续统一处理租户、权限、逻辑删除、状态过滤等规则。

### XML Mapper 配置

XML Mapper 适合维护复杂 SQL，例如多表关联、统计报表、复杂动态条件、批量更新、数据库特定语法等。MyBatis-Flex 不限制 MyBatis 原生 XML 用法；只要配置好 `mapper-locations`，即可像原生 MyBatis 一样编写 XML。官方配置文档说明，`mapper-locations` 用于指定 MyBatis Mapper XML 文件路径，默认值为 `classpath*:/mapper/**/*.xml`。([MyBatis-Flex](https://mybatis-flex.com/zh/base/configuration.html?utm_source=chatgpt.com))

文件位置：`src/main/resources/application.yml`

```yaml
mybatis-flex:
  # Mapper XML 文件扫描路径
  mapper-locations: classpath*:/mapper/**/*.xml

  configuration:
    # 数据库下划线字段自动映射为 Java 驼峰属性
    map-underscore-to-camel-case: true
```

Mapper 接口中声明 XML 对应的方法。

文件位置：`src/main/java/io/github/atengk/mapper/SysUserMapper.java`

```java
package io.github.atengk.mapper;

import com.mybatisflex.core.BaseMapper;
import io.github.atengk.entity.SysUser;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * 系统用户 Mapper
 *
 * @author Ateng
 * @since 2026-05-05
 */
public interface SysUserMapper extends BaseMapper<SysUser> {

    /**
     * 根据关键字查询用户列表
     *
     * @param keyword 用户名、昵称或手机号关键字
     * @param status 用户状态
     * @return 用户列表
     */
    List<SysUser> selectListByKeyword(@Param("keyword") String keyword,
                                      @Param("status") Integer status);

}
```

XML 文件的 `namespace` 必须与 Mapper 接口全限定类名一致，`id` 必须与接口方法名一致。

文件位置：`src/main/resources/mapper/SysUserMapper.xml`

```xml
<?xml version="1.0" encoding="UTF-8" ?>
<!DOCTYPE mapper
        PUBLIC "-//mybatis.org//DTD Mapper 3.0//EN"
        "https://mybatis.org/dtd/mybatis-3-mapper.dtd">

<mapper namespace="io.github.atengk.mapper.SysUserMapper">

    <!-- 用户基础字段，避免多个 SQL 重复维护字段列表 -->
    <sql id="BaseColumnList">
        id,
        username,
        nickname,
        mobile,
        status,
        deleted,
        version,
        create_time,
        update_time
    </sql>

    <!-- 根据关键字和状态查询用户列表 -->
    <select id="selectListByKeyword" resultType="io.github.atengk.entity.SysUser">
        select
        <include refid="BaseColumnList"/>
        from sys_user
        where deleted = 0

        <if test="status != null">
            and status = #{status}
        </if>

        <if test="keyword != null and keyword != ''">
            and (
                username like concat('%', #{keyword}, '%')
                or nickname like concat('%', #{keyword}, '%')
                or mobile like concat('%', #{keyword}, '%')
            )
        </if>

        order by id desc
    </select>

</mapper>
```

XML Mapper 推荐用于复杂查询，不建议把所有简单 CRUD 都写成 XML。基础新增、修改、删除、主键查询和简单条件查询应优先使用 `BaseMapper` 和 `QueryWrapper`；复杂 SQL 再进入 XML，以保证代码量和 SQL 可维护性之间的平衡。

## Service 层开发

Service 层用于组织业务逻辑、封装事务边界、屏蔽 Mapper 细节，并向 Controller 提供稳定的业务方法。MyBatis-Flex 提供了顶级 `IService<T>` 接口和默认实现类 `ServiceImpl<M, T>`，用于减少 Service 层重复定义基础 CRUD 方法。官方文档说明，`IService` 提供简单常用的增删改查方法，复杂业务仍然建议通过 Mapper 处理。([MyBatis-Flex](https://mybatis-flex.com/zh/base/service.html?utm_source=chatgpt.com))

### 基础 CRUD 封装

基础 CRUD 封装建议采用 `IService<T>` 加 `ServiceImpl<M, T>` 的形式。这样 Service 层可以直接复用 `save`、`saveOrUpdate`、`removeById`、`getById`、`list` 等基础能力，同时在业务方法中加入参数校验、日志、异常处理和事务控制。官方 Service 文档给出的写法也是让业务接口继承 `IService<T>`，实现类继承 `ServiceImpl<Mapper, Entity>`。([MyBatis-Flex](https://mybatis-flex.com/zh/base/service.html?utm_source=chatgpt.com))

文件位置：`src/main/java/io/github/atengk/dto/SysUserCreateDTO.java`

```java
package io.github.atengk.dto;

import lombok.Data;

/**
 * 系统用户新增参数
 *
 * @author Ateng
 * @since 2026-05-05
 */
@Data
public class SysUserCreateDTO {

    /**
     * 用户名
     */
    private String username;

    /**
     * 用户昵称
     */
    private String nickname;

    /**
     * 手机号
     */
    private String mobile;

    /**
     * 状态：0禁用，1启用
     */
    private Integer status;

}
```

文件位置：`src/main/java/io/github/atengk/dto/SysUserUpdateDTO.java`

```java
package io.github.atengk.dto;

import lombok.Data;

/**
 * 系统用户修改参数
 *
 * @author Ateng
 * @since 2026-05-05
 */
@Data
public class SysUserUpdateDTO {

    /**
     * 主键ID
     */
    private Long id;

    /**
     * 用户昵称
     */
    private String nickname;

    /**
     * 手机号
     */
    private String mobile;

    /**
     * 状态：0禁用，1启用
     */
    private Integer status;

}
```

文件位置：`src/main/java/io/github/atengk/service/SysUserService.java`

```java
package io.github.atengk.service;

import com.mybatisflex.core.service.IService;
import io.github.atengk.dto.SysUserCreateDTO;
import io.github.atengk.dto.SysUserUpdateDTO;
import io.github.atengk.entity.SysUser;

/**
 * 系统用户 Service
 *
 * @author Ateng
 * @since 2026-05-05
 */
public interface SysUserService extends IService<SysUser> {

    /**
     * 新增用户
     *
     * @param createDTO 新增参数
     * @return 用户ID
     */
    Long createUser(SysUserCreateDTO createDTO);

    /**
     * 修改用户
     *
     * @param updateDTO 修改参数
     * @return 是否修改成功
     */
    Boolean updateUser(SysUserUpdateDTO updateDTO);

    /**
     * 删除用户
     *
     * @param id 用户ID
     * @return 是否删除成功
     */
    Boolean deleteUser(Long id);

}
```

下面实现类封装基础新增、修改和删除逻辑。示例中使用 Hutool 做参数判断，使用日志记录关键业务操作。

文件位置：`src/main/java/io/github/atengk/service/impl/SysUserServiceImpl.java`

```java
package io.github.atengk.service.impl;

import cn.hutool.core.util.ObjectUtil;
import cn.hutool.core.util.StrUtil;
import com.mybatisflex.spring.service.impl.ServiceImpl;
import io.github.atengk.dto.SysUserCreateDTO;
import io.github.atengk.dto.SysUserUpdateDTO;
import io.github.atengk.entity.SysUser;
import io.github.atengk.mapper.SysUserMapper;
import io.github.atengk.service.SysUserService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 系统用户 Service 实现
 *
 * @author Ateng
 * @since 2026-05-05
 */
@Slf4j
@Service
public class SysUserServiceImpl extends ServiceImpl<SysUserMapper, SysUser> implements SysUserService {

    /**
     * 新增用户
     *
     * @param createDTO 新增参数
     * @return 用户ID
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long createUser(SysUserCreateDTO createDTO) {
        if (ObjectUtil.isNull(createDTO)) {
            throw new IllegalArgumentException("新增用户参数不能为空");
        }
        if (StrUtil.isBlank(createDTO.getUsername())) {
            throw new IllegalArgumentException("用户名不能为空");
        }

        SysUser existUser = mapper.selectByUsername(createDTO.getUsername());
        if (ObjectUtil.isNotNull(existUser)) {
            throw new IllegalArgumentException("用户名已存在");
        }

        SysUser user = new SysUser();
        user.setUsername(createDTO.getUsername());
        user.setNickname(createDTO.getNickname());
        user.setMobile(createDTO.getMobile());
        user.setStatus(ObjectUtil.defaultIfNull(createDTO.getStatus(), 1));

        mapper.insertSelective(user);
        log.info("新增系统用户成功，用户ID：{}，用户名：{}", user.getId(), user.getUsername());
        return user.getId();
    }

    /**
     * 修改用户
     *
     * @param updateDTO 修改参数
     * @return 是否修改成功
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public Boolean updateUser(SysUserUpdateDTO updateDTO) {
        if (ObjectUtil.isNull(updateDTO) || ObjectUtil.isNull(updateDTO.getId())) {
            throw new IllegalArgumentException("用户ID不能为空");
        }

        SysUser dbUser = mapper.selectOneById(updateDTO.getId());
        if (ObjectUtil.isNull(dbUser)) {
            throw new IllegalArgumentException("用户不存在或已删除");
        }

        SysUser user = new SysUser();
        user.setId(updateDTO.getId());
        user.setNickname(updateDTO.getNickname());
        user.setMobile(updateDTO.getMobile());
        user.setStatus(updateDTO.getStatus());

        int rows = mapper.update(user);
        log.info("修改系统用户完成，用户ID：{}，影响行数：{}", updateDTO.getId(), rows);
        return rows > 0;
    }

    /**
     * 删除用户
     *
     * @param id 用户ID
     * @return 是否删除成功
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public Boolean deleteUser(Long id) {
        if (ObjectUtil.isNull(id)) {
            throw new IllegalArgumentException("用户ID不能为空");
        }

        int rows = mapper.deleteById(id);
        log.info("删除系统用户完成，用户ID：{}，影响行数：{}", id, rows);
        return rows > 0;
    }

}
```

如果实体类配置了 `@Column(isLogicDelete = true)`，调用 `deleteById` 时会执行逻辑删除而不是物理删除。官方逻辑删除文档说明，删除操作会更新逻辑删除字段，查询时也会自动追加正常状态条件；`selectOneById`、`selectListBy...`、`selectCountBy...`、`paginate` 等方法都会自动处理逻辑删除条件。([MyBatis-Flex](https://mybatis-flex.com/zh/core/logic-delete.html?utm_source=chatgpt.com))

### 条件查询封装

条件查询封装用于把页面查询参数转换为数据库查询条件。建议 Controller 只接收参数，Service 负责构建 `QueryWrapper`，Mapper 只负责执行查询。MyBatis-Flex 官方文档说明，`QueryWrapper` 支持 `where`、`and`、`or`、`orderBy`、`groupBy`、`having`、`limit`、`offset`、多表查询、函数查询等能力。([MyBatis-Flex](https://mybatis-flex.com/zh/base/querywrapper.html?utm_source=chatgpt.com))

文件位置：`src/main/java/io/github/atengk/dto/SysUserQueryDTO.java`

```java
package io.github.atengk.dto;

import lombok.Data;

/**
 * 系统用户查询参数
 *
 * @author Ateng
 * @since 2026-05-05
 */
@Data
public class SysUserQueryDTO {

    /**
     * 关键字：用户名、昵称、手机号
     */
    private String keyword;

    /**
     * 状态：0禁用，1启用
     */
    private Integer status;

}
```

在 Service 接口中增加条件查询方法。

文件位置：`src/main/java/io/github/atengk/service/SysUserService.java`

```java
package io.github.atengk.service;

import com.mybatisflex.core.service.IService;
import io.github.atengk.dto.SysUserCreateDTO;
import io.github.atengk.dto.SysUserQueryDTO;
import io.github.atengk.dto.SysUserUpdateDTO;
import io.github.atengk.entity.SysUser;

import java.util.List;

/**
 * 系统用户 Service
 *
 * @author Ateng
 * @since 2026-05-05
 */
public interface SysUserService extends IService<SysUser> {

    /**
     * 新增用户
     *
     * @param createDTO 新增参数
     * @return 用户ID
     */
    Long createUser(SysUserCreateDTO createDTO);

    /**
     * 修改用户
     *
     * @param updateDTO 修改参数
     * @return 是否修改成功
     */
    Boolean updateUser(SysUserUpdateDTO updateDTO);

    /**
     * 删除用户
     *
     * @param id 用户ID
     * @return 是否删除成功
     */
    Boolean deleteUser(Long id);

    /**
     * 条件查询用户列表
     *
     * @param queryDTO 查询参数
     * @return 用户列表
     */
    List<SysUser> listUsers(SysUserQueryDTO queryDTO);

}
```

下面实现条件查询封装，使用 Hutool 判断字符串和对象是否为空。

文件位置：`src/main/java/io/github/atengk/service/impl/SysUserServiceImpl.java`

```java
package io.github.atengk.service.impl;

import cn.hutool.core.util.ObjectUtil;
import cn.hutool.core.util.StrUtil;
import com.mybatisflex.core.query.QueryWrapper;
import com.mybatisflex.spring.service.impl.ServiceImpl;
import io.github.atengk.dto.SysUserCreateDTO;
import io.github.atengk.dto.SysUserQueryDTO;
import io.github.atengk.dto.SysUserUpdateDTO;
import io.github.atengk.entity.SysUser;
import io.github.atengk.mapper.SysUserMapper;
import io.github.atengk.service.SysUserService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

import static io.github.atengk.entity.table.SysUserTableDef.SYS_USER;

/**
 * 系统用户 Service 实现
 *
 * @author Ateng
 * @since 2026-05-05
 */
@Slf4j
@Service
public class SysUserServiceImpl extends ServiceImpl<SysUserMapper, SysUser> implements SysUserService {

    /**
     * 条件查询用户列表
     *
     * @param queryDTO 查询参数
     * @return 用户列表
     */
    @Override
    public List<SysUser> listUsers(SysUserQueryDTO queryDTO) {
        QueryWrapper queryWrapper = buildUserQueryWrapper(queryDTO)
                .orderBy(SYS_USER.ID.desc());

        List<SysUser> users = mapper.selectListByQuery(queryWrapper);
        log.info("条件查询系统用户完成，数量：{}", users.size());
        return users;
    }

    /**
     * 构建用户查询条件
     *
     * @param queryDTO 查询参数
     * @return 查询条件
     */
    private QueryWrapper buildUserQueryWrapper(SysUserQueryDTO queryDTO) {
        QueryWrapper queryWrapper = QueryWrapper.create()
                .select()
                .from(SYS_USER)
                .where(SYS_USER.DELETED.eq(0));

        if (ObjectUtil.isNull(queryDTO)) {
            return queryWrapper;
        }

        if (ObjectUtil.isNotNull(queryDTO.getStatus())) {
            queryWrapper.and(SYS_USER.STATUS.eq(queryDTO.getStatus()));
        }

        if (StrUtil.isNotBlank(queryDTO.getKeyword())) {
            String keyword = queryDTO.getKeyword();
            queryWrapper.and(
                    SYS_USER.USERNAME.like(keyword)
                            .or(SYS_USER.NICKNAME.like(keyword))
                            .or(SYS_USER.MOBILE.like(keyword))
            );
        }

        return queryWrapper;
    }

    // 其他 createUser、updateUser、deleteUser 方法保持不变
}
```

如果不希望依赖 APT 生成的 `SYS_USER` 表定义对象，也可以使用 XML Mapper 或 `QueryMethods.column("字段名")` 方式构建条件。但在实际项目中，更推荐启用 APT 表定义类，因为字段引用具备 IDE 提示和重构支持，能减少字符串字段名写错的问题。官方 APT 文档说明，APT 会在项目编译时根据 Entity 字段生成表定义辅助类，执行 `mvn clean package` 也会触发生成。([MyBatis-Flex](https://mybatis-flex.com/zh/others/apt.html?utm_source=chatgpt.com))

### 分页查询实现

分页查询适合后台管理列表、移动端数据滚动加载、搜索列表等场景。MyBatis-Flex 的 `BaseMapper` 提供 `paginate(pageNumber, pageSize, queryWrapper)`、`paginate(page, queryWrapper)` 等分页方法；官方查询分页文档列出了多种 `paginate` 和 `paginateWithRelations` 方法。([MyBatis-Flex](https://mybatis-flex.com/zh/base/query.html?utm_source=chatgpt.com))

文件位置：`src/main/java/io/github/atengk/dto/PageQueryDTO.java`

```java
package io.github.atengk.dto;

import lombok.Data;

/**
 * 分页查询基础参数
 *
 * @author Ateng
 * @since 2026-05-05
 */
@Data
public class PageQueryDTO {

    /**
     * 当前页码，从 1 开始
     */
    private Integer pageNumber = 1;

    /**
     * 每页数量
     */
    private Integer pageSize = 10;

}
```

文件位置：`src/main/java/io/github/atengk/dto/SysUserPageDTO.java`

```java
package io.github.atengk.dto;

import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 系统用户分页查询参数
 *
 * @author Ateng
 * @since 2026-05-05
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class SysUserPageDTO extends PageQueryDTO {

    /**
     * 关键字：用户名、昵称、手机号
     */
    private String keyword;

    /**
     * 状态：0禁用，1启用
     */
    private Integer status;

}
```

在 Service 接口中增加分页查询方法。

文件位置：`src/main/java/io/github/atengk/service/SysUserService.java`

```java
package io.github.atengk.service;

import com.mybatisflex.core.paginate.Page;
import com.mybatisflex.core.service.IService;
import io.github.atengk.dto.SysUserCreateDTO;
import io.github.atengk.dto.SysUserPageDTO;
import io.github.atengk.dto.SysUserQueryDTO;
import io.github.atengk.dto.SysUserUpdateDTO;
import io.github.atengk.entity.SysUser;

import java.util.List;

/**
 * 系统用户 Service
 *
 * @author Ateng
 * @since 2026-05-05
 */
public interface SysUserService extends IService<SysUser> {

    /**
     * 新增用户
     *
     * @param createDTO 新增参数
     * @return 用户ID
     */
    Long createUser(SysUserCreateDTO createDTO);

    /**
     * 修改用户
     *
     * @param updateDTO 修改参数
     * @return 是否修改成功
     */
    Boolean updateUser(SysUserUpdateDTO updateDTO);

    /**
     * 删除用户
     *
     * @param id 用户ID
     * @return 是否删除成功
     */
    Boolean deleteUser(Long id);

    /**
     * 条件查询用户列表
     *
     * @param queryDTO 查询参数
     * @return 用户列表
     */
    List<SysUser> listUsers(SysUserQueryDTO queryDTO);

    /**
     * 分页查询用户列表
     *
     * @param pageDTO 分页查询参数
     * @return 分页结果
     */
    Page<SysUser> pageUsers(SysUserPageDTO pageDTO);

}
```

下面实现分页查询。分页参数在 Service 中做兜底处理，避免前端传入空值、负数或过大的分页大小。

文件位置：`src/main/java/io/github/atengk/service/impl/SysUserServiceImpl.java`

```java
package io.github.atengk.service.impl;

import cn.hutool.core.util.NumberUtil;
import cn.hutool.core.util.ObjectUtil;
import cn.hutool.core.util.StrUtil;
import com.mybatisflex.core.paginate.Page;
import com.mybatisflex.core.query.QueryWrapper;
import com.mybatisflex.spring.service.impl.ServiceImpl;
import io.github.atengk.dto.SysUserCreateDTO;
import io.github.atengk.dto.SysUserPageDTO;
import io.github.atengk.dto.SysUserQueryDTO;
import io.github.atengk.dto.SysUserUpdateDTO;
import io.github.atengk.entity.SysUser;
import io.github.atengk.mapper.SysUserMapper;
import io.github.atengk.service.SysUserService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

import static io.github.atengk.entity.table.SysUserTableDef.SYS_USER;

/**
 * 系统用户 Service 实现
 *
 * @author Ateng
 * @since 2026-05-05
 */
@Slf4j
@Service
public class SysUserServiceImpl extends ServiceImpl<SysUserMapper, SysUser> implements SysUserService {

    /**
     * 分页查询用户列表
     *
     * @param pageDTO 分页查询参数
     * @return 分页结果
     */
    @Override
    public Page<SysUser> pageUsers(SysUserPageDTO pageDTO) {
        int pageNumber = getSafePageNumber(pageDTO);
        int pageSize = getSafePageSize(pageDTO);

        QueryWrapper queryWrapper = buildUserPageQueryWrapper(pageDTO)
                .orderBy(SYS_USER.ID.desc());

        Page<SysUser> page = mapper.paginate(pageNumber, pageSize, queryWrapper);
        log.info("分页查询系统用户完成，页码：{}，每页：{}，总数：{}",
                pageNumber, pageSize, page.getTotalRow());
        return page;
    }

    /**
     * 构建分页查询条件
     *
     * @param pageDTO 分页查询参数
     * @return 查询条件
     */
    private QueryWrapper buildUserPageQueryWrapper(SysUserPageDTO pageDTO) {
        QueryWrapper queryWrapper = QueryWrapper.create()
                .select()
                .from(SYS_USER)
                .where(SYS_USER.DELETED.eq(0));

        if (ObjectUtil.isNull(pageDTO)) {
            return queryWrapper;
        }

        if (ObjectUtil.isNotNull(pageDTO.getStatus())) {
            queryWrapper.and(SYS_USER.STATUS.eq(pageDTO.getStatus()));
        }

        if (StrUtil.isNotBlank(pageDTO.getKeyword())) {
            String keyword = pageDTO.getKeyword();
            queryWrapper.and(
                    SYS_USER.USERNAME.like(keyword)
                            .or(SYS_USER.NICKNAME.like(keyword))
                            .or(SYS_USER.MOBILE.like(keyword))
            );
        }

        return queryWrapper;
    }

    /**
     * 获取安全页码
     *
     * @param pageDTO 分页参数
     * @return 页码
     */
    private int getSafePageNumber(SysUserPageDTO pageDTO) {
        if (ObjectUtil.isNull(pageDTO) || ObjectUtil.isNull(pageDTO.getPageNumber())) {
            return 1;
        }
        return Math.max(pageDTO.getPageNumber(), 1);
    }

    /**
     * 获取安全分页大小
     *
     * @param pageDTO 分页参数
     * @return 分页大小
     */
    private int getSafePageSize(SysUserPageDTO pageDTO) {
        if (ObjectUtil.isNull(pageDTO) || ObjectUtil.isNull(pageDTO.getPageSize())) {
            return 10;
        }

        int pageSize = pageDTO.getPageSize();
        if (NumberUtil.isGreater(pageSize, 100)) {
            return 100;
        }

        return Math.max(pageSize, 1);
    }

    // 其他 createUser、updateUser、deleteUser、listUsers 方法保持不变
}
```

分页接口建议统一限制最大 `pageSize`，例如最大 100 或 200，避免前端一次性拉取过多数据造成慢 SQL、内存占用过高或接口超时。列表查询默认排序也应固定，例如按 `id desc` 或 `create_time desc`，避免数据库在无稳定排序条件时出现分页数据重复或遗漏。

## 查询构建器

查询构建器用于以 Java 链式 API 的方式组织 SQL 查询条件。MyBatis-Flex 的 `QueryWrapper` 是构造 SQL 的核心工具，支持 `select`、`from`、`where`、`and`、`or`、`groupBy`、`having`、`orderBy`、`join`、`limit`、`offset` 等常见 SQL 结构；配合 APT 生成的表定义类，可以减少字段名硬编码。官方文档中也说明，`QueryWrapper` 可用于构造 SQL，并且示例中的表对象如 `ACCOUNT` 由 APT 自动生成。([MyBatis-Flex](https://mybatis-flex.com/zh/base/querywrapper.html?utm_source=chatgpt.com))

### QueryWrapper 使用

`QueryWrapper` 适合封装列表查询、详情查询、统计查询和复杂条件查询。基础用法是先通过 `QueryWrapper.create()` 创建查询对象，再指定查询列、查询表、查询条件和排序条件，最后交给 Mapper 执行。官方文档中列出了 `selectListByQuery(queryWrapper)`、`selectOneByQuery(queryWrapper)`、`selectCountByQuery(queryWrapper)`、`paginate(pageNumber, pageSize, queryWrapper)` 等常用方法。([MyBatis-Flex](https://mybatis-flex.com/zh/base/query.html?utm_source=chatgpt.com))

下面示例基于前文的 `SysUser` 实体和 `SysUserMapper`，演示常见查询写法。

文件位置：`src/main/java/io/github/atengk/service/impl/SysUserQueryService.java`

```java
package io.github.atengk.service.impl;

import cn.hutool.core.util.ObjectUtil;
import cn.hutool.core.util.StrUtil;
import com.mybatisflex.core.query.QueryWrapper;
import io.github.atengk.entity.SysUser;
import io.github.atengk.mapper.SysUserMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;

import static io.github.atengk.entity.table.SysUserTableDef.SYS_USER;

/**
 * 系统用户查询服务
 *
 * @author Ateng
 * @since 2026-05-05
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class SysUserQueryService {

    private final SysUserMapper sysUserMapper;

    /**
     * 根据用户ID查询用户
     *
     * @param id 用户ID
     * @return 用户信息
     */
    public SysUser getUserById(Long id) {
        if (ObjectUtil.isNull(id)) {
            throw new IllegalArgumentException("用户ID不能为空");
        }

        QueryWrapper queryWrapper = QueryWrapper.create()
                .select()
                .from(SYS_USER)
                .where(SYS_USER.ID.eq(id))
                .and(SYS_USER.DELETED.eq(0));

        SysUser user = sysUserMapper.selectOneByQuery(queryWrapper);
        log.info("根据ID查询系统用户完成，用户ID：{}", id);
        return user;
    }

    /**
     * 根据用户名查询用户
     *
     * @param username 用户名
     * @return 用户信息
     */
    public SysUser getUserByUsername(String username) {
        if (StrUtil.isBlank(username)) {
            throw new IllegalArgumentException("用户名不能为空");
        }

        QueryWrapper queryWrapper = QueryWrapper.create()
                .select(SYS_USER.ID, SYS_USER.USERNAME, SYS_USER.NICKNAME, SYS_USER.MOBILE, SYS_USER.STATUS)
                .from(SYS_USER)
                .where(SYS_USER.USERNAME.eq(username))
                .and(SYS_USER.DELETED.eq(0));

        SysUser user = sysUserMapper.selectOneByQuery(queryWrapper);
        log.info("根据用户名查询系统用户完成，用户名：{}", username);
        return user;
    }

    /**
     * 查询启用用户列表
     *
     * @return 用户列表
     */
    public List<SysUser> listEnabledUsers() {
        QueryWrapper queryWrapper = QueryWrapper.create()
                .select()
                .from(SYS_USER)
                .where(SYS_USER.STATUS.eq(1))
                .and(SYS_USER.DELETED.eq(0))
                .orderBy(SYS_USER.ID.desc());

        List<SysUser> users = sysUserMapper.selectListByQuery(queryWrapper);
        log.info("查询启用系统用户完成，数量：{}", users.size());
        return users;
    }

}
```

`QueryWrapper` 查询建议优先放在 Service 层封装，不建议在 Controller 中直接拼接查询条件。Controller 应只负责接收请求参数和返回结果；Service 负责参数校验、条件拼接、默认过滤条件和日志记录；Mapper 负责执行最终 SQL。

### 条件动态拼接

条件动态拼接用于处理“参数有值才参与查询”的场景。例如用户列表页中，关键字、状态、开始时间、结束时间都可能为空。动态条件建议通过 `if` 判断逐步追加，避免生成无意义的 SQL 条件。MyBatis-Flex 的 `QueryWrapper` 支持 `and(...)`、`or(...)`、嵌套条件等写法，官方示例中也展示了 `and(condition.or(...))`、`or(condition.and(...))` 这类复合条件用法。([MyBatis-Flex](https://mybatis-flex.com/zh/base/querywrapper.html?utm_source=chatgpt.com))

文件位置：`src/main/java/io/github/atengk/dto/SysUserQueryDTO.java`

```java
package io.github.atengk.dto;

import lombok.Data;

import java.time.LocalDateTime;

/**
 * 系统用户查询参数
 *
 * @author Ateng
 * @since 2026-05-05
 */
@Data
public class SysUserQueryDTO {

    /**
     * 关键字：用户名、昵称、手机号
     */
    private String keyword;

    /**
     * 状态：0禁用，1启用
     */
    private Integer status;

    /**
     * 创建开始时间
     */
    private LocalDateTime startTime;

    /**
     * 创建结束时间
     */
    private LocalDateTime endTime;

}
```

下面代码封装用户查询条件，适合被列表查询和分页查询复用。

文件位置：`src/main/java/io/github/atengk/service/impl/SysUserQueryWrapperBuilder.java`

```java
package io.github.atengk.service.impl;

import cn.hutool.core.util.ObjectUtil;
import cn.hutool.core.util.StrUtil;
import com.mybatisflex.core.query.QueryWrapper;
import io.github.atengk.dto.SysUserQueryDTO;
import org.springframework.stereotype.Component;

import static io.github.atengk.entity.table.SysUserTableDef.SYS_USER;

/**
 * 系统用户查询条件构建器
 *
 * @author Ateng
 * @since 2026-05-05
 */
@Component
public class SysUserQueryWrapperBuilder {

    /**
     * 构建用户查询条件
     *
     * @param queryDTO 查询参数
     * @return 查询条件
     */
    public QueryWrapper build(SysUserQueryDTO queryDTO) {
        QueryWrapper queryWrapper = QueryWrapper.create()
                .select()
                .from(SYS_USER)
                .where(SYS_USER.DELETED.eq(0));

        if (ObjectUtil.isNull(queryDTO)) {
            return queryWrapper;
        }

        if (ObjectUtil.isNotNull(queryDTO.getStatus())) {
            queryWrapper.and(SYS_USER.STATUS.eq(queryDTO.getStatus()));
        }

        if (ObjectUtil.isNotNull(queryDTO.getStartTime())) {
            queryWrapper.and(SYS_USER.CREATE_TIME.ge(queryDTO.getStartTime()));
        }

        if (ObjectUtil.isNotNull(queryDTO.getEndTime())) {
            queryWrapper.and(SYS_USER.CREATE_TIME.le(queryDTO.getEndTime()));
        }

        if (StrUtil.isNotBlank(queryDTO.getKeyword())) {
            String keyword = StrUtil.trim(queryDTO.getKeyword());
            queryWrapper.and(
                    SYS_USER.USERNAME.like(keyword)
                            .or(SYS_USER.NICKNAME.like(keyword))
                            .or(SYS_USER.MOBILE.like(keyword))
            );
        }

        return queryWrapper;
    }

}
```

在实际项目中，动态条件拼接要注意两点：第一，接口入参必须先做空值和范围校验，避免无效条件进入 SQL；第二，关键字查询应限制字段范围，不要为了方便对所有字段做 `like`，否则容易导致全表扫描。

### 排序与分组

排序用于控制列表数据的返回顺序，分组用于统计和聚合查询。MyBatis-Flex 的 `QueryWrapper` 支持 `orderBy`、`groupBy`、`having`，官方示例中展示了 `groupBy(ACCOUNT.USER_NAME)`、`having(ACCOUNT.AGE.between(18,25))`、`orderBy(ACCOUNT.AGE.asc(), ACCOUNT.USER_NAME.desc().nullsLast())` 以及动态排序 `orderBy(字段, true/false/null)`。([MyBatis-Flex](https://mybatis-flex.com/zh/base/querywrapper.html?utm_source=chatgpt.com))

普通排序示例如下。

```java
QueryWrapper queryWrapper = QueryWrapper.create()
        .select()
        .from(SYS_USER)
        .where(SYS_USER.DELETED.eq(0))
        .orderBy(SYS_USER.CREATE_TIME.desc(), SYS_USER.ID.desc());
```

分组统计适合状态统计、按日期统计、按组织统计等场景。下面示例按用户状态统计人数，并返回 Map 结果。

文件位置：`src/main/java/io/github/atengk/service/impl/SysUserStatisticsService.java`

```java
package io.github.atengk.service.impl;

import cn.hutool.core.collection.CollUtil;
import com.mybatisflex.core.query.QueryWrapper;
import com.mybatisflex.core.row.Row;
import io.github.atengk.mapper.SysUserMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;

import static com.mybatisflex.core.query.QueryMethods.count;
import static io.github.atengk.entity.table.SysUserTableDef.SYS_USER;

/**
 * 系统用户统计服务
 *
 * @author Ateng
 * @since 2026-05-05
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class SysUserStatisticsService {

    private final SysUserMapper sysUserMapper;

    /**
     * 按状态统计用户数量
     *
     * @return 统计结果
     */
    public List<Row> countUsersByStatus() {
        QueryWrapper queryWrapper = QueryWrapper.create()
                .select(SYS_USER.STATUS, count(SYS_USER.ID).as("user_count"))
                .from(SYS_USER)
                .where(SYS_USER.DELETED.eq(0))
                .groupBy(SYS_USER.STATUS)
                .orderBy(SYS_USER.STATUS.asc());

        List<Row> rows = sysUserMapper.selectRowsByQuery(queryWrapper);
        log.info("按状态统计系统用户完成，分组数量：{}", CollUtil.size(rows));
        return rows;
    }

    /**
     * 查询用户数量大于指定值的状态分组
     *
     * @param minCount 最小用户数量
     * @return 统计结果
     */
    public List<Row> countUsersByStatusHaving(Long minCount) {
        QueryWrapper queryWrapper = QueryWrapper.create()
                .select(SYS_USER.STATUS, count(SYS_USER.ID).as("user_count"))
                .from(SYS_USER)
                .where(SYS_USER.DELETED.eq(0))
                .groupBy(SYS_USER.STATUS)
                .having(count(SYS_USER.ID).ge(minCount))
                .orderBy(SYS_USER.STATUS.asc());

        List<Row> rows = sysUserMapper.selectRowsByQuery(queryWrapper);
        log.info("按状态统计系统用户并过滤分组完成，最小数量：{}，分组数量：{}", minCount, CollUtil.size(rows));
        return rows;
    }

}
```

分组查询时建议只查询分组字段和聚合字段，不要直接 `select()` 查询所有字段。否则在 MySQL 开启 `ONLY_FULL_GROUP_BY` 时可能报错，在其他数据库中也容易产生语义不清晰的结果。

## 分页与排序

分页与排序通常同时出现，主要用于后台管理列表、搜索结果列表和移动端分页加载。MyBatis-Flex 的 `BaseMapper` 已提供分页方法，例如 `paginate(pageNumber, pageSize, queryWrapper)`、`paginate(page, queryWrapper)`、`paginateAs(...)`、`paginateWithRelations(...)` 等；分页返回结果为 `Page<T>`，包含 `records`、`pageNumber`、`pageSize`、`totalPage`、`totalRow` 等信息。([MyBatis-Flex](https://mybatis-flex.com/zh/base/query.html?utm_source=chatgpt.com))

### 分页插件配置

MyBatis-Flex 的分页使用方式与 MyBatis-Plus 常见的“分页拦截器插件配置”不同。基于 `BaseMapper` 的常规分页场景，一般不需要额外注册分页插件，直接调用 `mapper.paginate(pageNumber, pageSize, queryWrapper)` 即可。官方查询分页文档列出的分页能力都在 `BaseMapper` 中提供，包括普通分页、关联分页、指定 VO 类型分页等。([MyBatis-Flex](https://mybatis-flex.com/zh/base/query.html?utm_source=chatgpt.com))

因此，本节的配置重点不是新增分页拦截器，而是确认 Mapper 扫描、XML 路径和数据库方言能正常工作。

文件位置：`src/main/resources/application.yml`

```yaml
mybatis-flex:
  # Mapper XML 扫描路径
  mapper-locations: classpath*:/mapper/**/*.xml

  configuration:
    # 开启下划线字段到驼峰属性的映射
    map-underscore-to-camel-case: true

  global-config:
    # 开发环境可打印 Banner，生产环境可关闭
    print-banner: true
```

如果需要使用 `limit`、`offset` 这类手动分页写法，MyBatis-Flex 会根据当前数据库类型生成不同 SQL。官方 `QueryWrapper` 文档说明，`limit...offset` 示例中框架能够自动识别当前数据库并生成不同数据库的分页 SQL，也可以通过 `DialectFactory` 注册或改写方言实现。([MyBatis-Flex](https://mybatis-flex.com/zh/base/querywrapper.html?utm_source=chatgpt.com))

```java
QueryWrapper queryWrapper = QueryWrapper.create()
        .select()
        .from(SYS_USER)
        .where(SYS_USER.DELETED.eq(0))
        .orderBy(SYS_USER.ID.desc())
        .limit(10)
        .offset(20);
```

业务分页优先使用 `paginate`，不建议在常规列表接口中手动计算 `limit` 和 `offset`。手动分页适合极少数自定义 SQL 拼接场景，普通后台列表直接使用 `Page<T>` 更清晰。

### 分页接口开发

分页接口建议统一接收 `pageNumber`、`pageSize`、查询条件和排序参数。Service 层负责分页参数兜底、最大分页大小限制、排序字段白名单校验，Mapper 层只接收最终构建好的 `QueryWrapper`。

文件位置：`src/main/java/io/github/atengk/dto/SysUserPageDTO.java`

```java
package io.github.atengk.dto;

import lombok.Data;

import java.time.LocalDateTime;

/**
 * 系统用户分页查询参数
 *
 * @author Ateng
 * @since 2026-05-05
 */
@Data
public class SysUserPageDTO {

    /**
     * 当前页码，从 1 开始
     */
    private Integer pageNumber = 1;

    /**
     * 每页数量
     */
    private Integer pageSize = 10;

    /**
     * 关键字：用户名、昵称、手机号
     */
    private String keyword;

    /**
     * 状态：0禁用，1启用
     */
    private Integer status;

    /**
     * 创建开始时间
     */
    private LocalDateTime startTime;

    /**
     * 创建结束时间
     */
    private LocalDateTime endTime;

    /**
     * 排序字段，例如 id、username、createTime
     */
    private String sortField;

    /**
     * 排序方向：asc 或 desc
     */
    private String sortOrder;

}
```

文件位置：`src/main/java/io/github/atengk/controller/SysUserController.java`

```java
package io.github.atengk.controller;

import com.mybatisflex.core.paginate.Page;
import io.github.atengk.dto.SysUserPageDTO;
import io.github.atengk.entity.SysUser;
import io.github.atengk.service.SysUserService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

/**
 * 系统用户接口
 *
 * @author Ateng
 * @since 2026-05-05
 */
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/users")
public class SysUserController {

    private final SysUserService sysUserService;

    /**
     * 分页查询系统用户
     *
     * @param pageDTO 分页查询参数
     * @return 分页结果
     */
    @GetMapping("/page")
    public Page<SysUser> pageUsers(SysUserPageDTO pageDTO) {
        return sysUserService.pageUsers(pageDTO);
    }

}
```

Service 接口中增加分页方法。

文件位置：`src/main/java/io/github/atengk/service/SysUserService.java`

```java
package io.github.atengk.service;

import com.mybatisflex.core.paginate.Page;
import com.mybatisflex.core.service.IService;
import io.github.atengk.dto.SysUserPageDTO;
import io.github.atengk.entity.SysUser;

/**
 * 系统用户 Service
 *
 * @author Ateng
 * @since 2026-05-05
 */
public interface SysUserService extends IService<SysUser> {

    /**
     * 分页查询用户列表
     *
     * @param pageDTO 分页查询参数
     * @return 分页结果
     */
    Page<SysUser> pageUsers(SysUserPageDTO pageDTO);

}
```

分页查询实现如下。这里同时处理了查询条件、分页参数和默认排序。

文件位置：`src/main/java/io/github/atengk/service/impl/SysUserServiceImpl.java`

```java
package io.github.atengk.service.impl;

import cn.hutool.core.util.NumberUtil;
import cn.hutool.core.util.ObjectUtil;
import cn.hutool.core.util.StrUtil;
import com.mybatisflex.core.paginate.Page;
import com.mybatisflex.core.query.QueryWrapper;
import com.mybatisflex.spring.service.impl.ServiceImpl;
import io.github.atengk.dto.SysUserPageDTO;
import io.github.atengk.entity.SysUser;
import io.github.atengk.mapper.SysUserMapper;
import io.github.atengk.service.SysUserService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import static io.github.atengk.entity.table.SysUserTableDef.SYS_USER;

/**
 * 系统用户 Service 实现
 *
 * @author Ateng
 * @since 2026-05-05
 */
@Slf4j
@Service
public class SysUserServiceImpl extends ServiceImpl<SysUserMapper, SysUser> implements SysUserService {

    /**
     * 分页查询用户列表
     *
     * @param pageDTO 分页查询参数
     * @return 分页结果
     */
    @Override
    public Page<SysUser> pageUsers(SysUserPageDTO pageDTO) {
        int pageNumber = getSafePageNumber(pageDTO);
        int pageSize = getSafePageSize(pageDTO);

        QueryWrapper queryWrapper = buildPageQueryWrapper(pageDTO);
        applyDefaultSort(queryWrapper);

        Page<SysUser> page = mapper.paginate(pageNumber, pageSize, queryWrapper);
        log.info("分页查询系统用户完成，页码：{}，每页：{}，总数：{}",
                pageNumber, pageSize, page.getTotalRow());
        return page;
    }

    /**
     * 构建分页查询条件
     *
     * @param pageDTO 分页查询参数
     * @return 查询条件
     */
    private QueryWrapper buildPageQueryWrapper(SysUserPageDTO pageDTO) {
        QueryWrapper queryWrapper = QueryWrapper.create()
                .select()
                .from(SYS_USER)
                .where(SYS_USER.DELETED.eq(0));

        if (ObjectUtil.isNull(pageDTO)) {
            return queryWrapper;
        }

        if (ObjectUtil.isNotNull(pageDTO.getStatus())) {
            queryWrapper.and(SYS_USER.STATUS.eq(pageDTO.getStatus()));
        }

        if (ObjectUtil.isNotNull(pageDTO.getStartTime())) {
            queryWrapper.and(SYS_USER.CREATE_TIME.ge(pageDTO.getStartTime()));
        }

        if (ObjectUtil.isNotNull(pageDTO.getEndTime())) {
            queryWrapper.and(SYS_USER.CREATE_TIME.le(pageDTO.getEndTime()));
        }

        if (StrUtil.isNotBlank(pageDTO.getKeyword())) {
            String keyword = StrUtil.trim(pageDTO.getKeyword());
            queryWrapper.and(
                    SYS_USER.USERNAME.like(keyword)
                            .or(SYS_USER.NICKNAME.like(keyword))
                            .or(SYS_USER.MOBILE.like(keyword))
            );
        }

        return queryWrapper;
    }

    /**
     * 应用默认排序
     *
     * @param queryWrapper 查询条件
     */
    private void applyDefaultSort(QueryWrapper queryWrapper) {
        queryWrapper.orderBy(SYS_USER.CREATE_TIME.desc(), SYS_USER.ID.desc());
    }

    /**
     * 获取安全页码
     *
     * @param pageDTO 分页参数
     * @return 页码
     */
    private int getSafePageNumber(SysUserPageDTO pageDTO) {
        if (ObjectUtil.isNull(pageDTO) || ObjectUtil.isNull(pageDTO.getPageNumber())) {
            return 1;
        }
        return Math.max(pageDTO.getPageNumber(), 1);
    }

    /**
     * 获取安全分页大小
     *
     * @param pageDTO 分页参数
     * @return 分页大小
     */
    private int getSafePageSize(SysUserPageDTO pageDTO) {
        if (ObjectUtil.isNull(pageDTO) || ObjectUtil.isNull(pageDTO.getPageSize())) {
            return 10;
        }

        int pageSize = pageDTO.getPageSize();
        if (NumberUtil.isGreater(pageSize, 100)) {
            return 100;
        }

        return Math.max(pageSize, 1);
    }

}
```

接口调用示例如下。

```bash
curl -G 'http://localhost:8080/api/users/page' \
  --data-urlencode 'pageNumber=1' \
  --data-urlencode 'pageSize=10' \
  --data-urlencode 'keyword=admin' \
  --data-urlencode 'status=1'
```

分页接口建议统一约定页码从 `1` 开始，`pageSize` 设置最大值，例如 `100`。对于后台列表页，如果第一页已经返回了 `totalRow`，后续页是否继续查询总数可以按业务需要优化；MyBatis-Flex 官方文档说明，`paginate` 支持传入 `totalRow`，传入后可避免再次查询总数据量。([MyBatis-Flex](https://mybatis-flex.com/zh/base/query.html?utm_source=chatgpt.com))

### 排序参数处理

排序参数不能直接拼接到 SQL 中，否则容易出现 SQL 注入风险。推荐做法是维护“前端排序字段”到“数据库字段对象”的白名单映射，只允许排序固定字段。MyBatis-Flex 的 `orderBy` 支持动态升序、降序和不排序，官方示例说明动态条件取值为 `true` 表示升序，`false` 表示降序，`null` 表示不排序。([MyBatis-Flex](https://mybatis-flex.com/zh/base/querywrapper.html?utm_source=chatgpt.com))

下面代码给分页查询增加安全排序处理。

文件位置：`src/main/java/io/github/atengk/service/impl/SysUserSortHandler.java`

```java
package io.github.atengk.service.impl;

import cn.hutool.core.util.StrUtil;
import com.mybatisflex.core.query.QueryColumn;
import com.mybatisflex.core.query.QueryWrapper;
import io.github.atengk.dto.SysUserPageDTO;
import org.springframework.stereotype.Component;

import java.util.Map;

import static io.github.atengk.entity.table.SysUserTableDef.SYS_USER;

/**
 * 系统用户排序处理器
 *
 * @author Ateng
 * @since 2026-05-05
 */
@Component
public class SysUserSortHandler {

    private static final String ASC = "asc";

    private static final String DESC = "desc";

    private static final Map<String, QueryColumn> SORT_COLUMN_MAP = Map.of(
            "id", SYS_USER.ID,
            "username", SYS_USER.USERNAME,
            "status", SYS_USER.STATUS,
            "createTime", SYS_USER.CREATE_TIME,
            "updateTime", SYS_USER.UPDATE_TIME
    );

    /**
     * 应用排序条件
     *
     * @param queryWrapper 查询条件
     * @param pageDTO 分页参数
     */
    public void applySort(QueryWrapper queryWrapper, SysUserPageDTO pageDTO) {
        if (pageDTO == null || StrUtil.isBlank(pageDTO.getSortField())) {
            applyDefaultSort(queryWrapper);
            return;
        }

        QueryColumn sortColumn = SORT_COLUMN_MAP.get(pageDTO.getSortField());
        if (sortColumn == null) {
            applyDefaultSort(queryWrapper);
            return;
        }

        String sortOrder = StrUtil.blankToDefault(pageDTO.getSortOrder(), DESC);
        if (StrUtil.equalsIgnoreCase(ASC, sortOrder)) {
            queryWrapper.orderBy(sortColumn.asc(), SYS_USER.ID.desc());
            return;
        }

        if (StrUtil.equalsIgnoreCase(DESC, sortOrder)) {
            queryWrapper.orderBy(sortColumn.desc(), SYS_USER.ID.desc());
            return;
        }

        applyDefaultSort(queryWrapper);
    }

    /**
     * 应用默认排序
     *
     * @param queryWrapper 查询条件
     */
    private void applyDefaultSort(QueryWrapper queryWrapper) {
        queryWrapper.orderBy(SYS_USER.CREATE_TIME.desc(), SYS_USER.ID.desc());
    }

}
```

Service 中使用排序处理器。

文件位置：`src/main/java/io/github/atengk/service/impl/SysUserServiceImpl.java`

```java
package io.github.atengk.service.impl;

import cn.hutool.core.util.NumberUtil;
import cn.hutool.core.util.ObjectUtil;
import cn.hutool.core.util.StrUtil;
import com.mybatisflex.core.paginate.Page;
import com.mybatisflex.core.query.QueryWrapper;
import com.mybatisflex.spring.service.impl.ServiceImpl;
import io.github.atengk.dto.SysUserPageDTO;
import io.github.atengk.entity.SysUser;
import io.github.atengk.mapper.SysUserMapper;
import io.github.atengk.service.SysUserService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import static io.github.atengk.entity.table.SysUserTableDef.SYS_USER;

/**
 * 系统用户 Service 实现
 *
 * @author Ateng
 * @since 2026-05-05
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class SysUserServiceImpl extends ServiceImpl<SysUserMapper, SysUser> implements SysUserService {

    private final SysUserSortHandler sysUserSortHandler;

    /**
     * 分页查询用户列表
     *
     * @param pageDTO 分页查询参数
     * @return 分页结果
     */
    @Override
    public Page<SysUser> pageUsers(SysUserPageDTO pageDTO) {
        int pageNumber = getSafePageNumber(pageDTO);
        int pageSize = getSafePageSize(pageDTO);

        QueryWrapper queryWrapper = buildPageQueryWrapper(pageDTO);
        sysUserSortHandler.applySort(queryWrapper, pageDTO);

        Page<SysUser> page = mapper.paginate(pageNumber, pageSize, queryWrapper);
        log.info("分页查询系统用户完成，页码：{}，每页：{}，排序字段：{}，排序方向：{}",
                pageNumber,
                pageSize,
                ObjectUtil.isNull(pageDTO) ? null : pageDTO.getSortField(),
                ObjectUtil.isNull(pageDTO) ? null : pageDTO.getSortOrder());
        return page;
    }

    /**
     * 构建分页查询条件
     *
     * @param pageDTO 分页查询参数
     * @return 查询条件
     */
    private QueryWrapper buildPageQueryWrapper(SysUserPageDTO pageDTO) {
        QueryWrapper queryWrapper = QueryWrapper.create()
                .select()
                .from(SYS_USER)
                .where(SYS_USER.DELETED.eq(0));

        if (ObjectUtil.isNull(pageDTO)) {
            return queryWrapper;
        }

        if (ObjectUtil.isNotNull(pageDTO.getStatus())) {
            queryWrapper.and(SYS_USER.STATUS.eq(pageDTO.getStatus()));
        }

        if (ObjectUtil.isNotNull(pageDTO.getStartTime())) {
            queryWrapper.and(SYS_USER.CREATE_TIME.ge(pageDTO.getStartTime()));
        }

        if (ObjectUtil.isNotNull(pageDTO.getEndTime())) {
            queryWrapper.and(SYS_USER.CREATE_TIME.le(pageDTO.getEndTime()));
        }

        if (StrUtil.isNotBlank(pageDTO.getKeyword())) {
            String keyword = StrUtil.trim(pageDTO.getKeyword());
            queryWrapper.and(
                    SYS_USER.USERNAME.like(keyword)
                            .or(SYS_USER.NICKNAME.like(keyword))
                            .or(SYS_USER.MOBILE.like(keyword))
            );
        }

        return queryWrapper;
    }

    /**
     * 获取安全页码
     *
     * @param pageDTO 分页参数
     * @return 页码
     */
    private int getSafePageNumber(SysUserPageDTO pageDTO) {
        if (ObjectUtil.isNull(pageDTO) || ObjectUtil.isNull(pageDTO.getPageNumber())) {
            return 1;
        }
        return Math.max(pageDTO.getPageNumber(), 1);
    }

    /**
     * 获取安全分页大小
     *
     * @param pageDTO 分页参数
     * @return 分页大小
     */
    private int getSafePageSize(SysUserPageDTO pageDTO) {
        if (ObjectUtil.isNull(pageDTO) || ObjectUtil.isNull(pageDTO.getPageSize())) {
            return 10;
        }

        int pageSize = pageDTO.getPageSize();
        if (NumberUtil.isGreater(pageSize, 100)) {
            return 100;
        }

        return Math.max(pageSize, 1);
    }

}
```

支持排序的接口调用示例如下。

```bash
curl -G 'http://localhost:8080/api/users/page' \
  --data-urlencode 'pageNumber=1' \
  --data-urlencode 'pageSize=10' \
  --data-urlencode 'keyword=admin' \
  --data-urlencode 'sortField=createTime' \
  --data-urlencode 'sortOrder=desc'
```

排序字段建议只暴露业务语义字段，例如 `id`、`username`、`status`、`createTime`，不要让前端直接传数据库列名。默认排序建议追加主键倒序，例如 `create_time desc, id desc`，这样可以减少相同时间数据在翻页时顺序不稳定的问题。原始大纲结构来自你上传的文件。

## 事务处理

事务处理用于保证一组数据库操作要么全部成功，要么全部失败。Spring Boot 3 项目中，常规业务建议优先使用 Spring 的 `@Transactional` 声明式事务；MyBatis-Flex 也提供了 `Db.tx()` 和 `Db.txWithResult()` 进行编程式事务管理，并且官方说明在 Spring Boot 场景下可以直接使用 `@Transactional`。([MyBatis-Flex](https://mybatis-flex.com/zh/core/tx.html?utm_source=chatgpt.com))

### 声明式事务

声明式事务通常放在 Service 层，而不是 Controller 层或 Mapper 层。Service 层负责组合多个 Mapper 操作、校验业务规则、控制事务边界。Spring 的 `@Transactional` 默认传播行为是 `REQUIRED`，默认遇到 `RuntimeException` 或 `Error` 回滚，遇到受检异常不会自动回滚；如果希望所有异常都回滚，建议显式配置 `rollbackFor = Exception.class`。([Home](https://docs.spring.io/spring-framework/docs/current/javadoc-api/org/springframework/transaction/annotation/Transactional.html?utm_source=chatgpt.com))

下面代码演示新增用户时检查用户名唯一性，并在同一个事务中完成新增操作。

文件位置：`src/main/java/io/github/atengk/service/impl/SysUserServiceImpl.java`

```java
package io.github.atengk.service.impl;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.util.ObjectUtil;
import cn.hutool.core.util.StrUtil;
import com.mybatisflex.spring.service.impl.ServiceImpl;
import io.github.atengk.dto.SysUserCreateDTO;
import io.github.atengk.entity.SysUser;
import io.github.atengk.mapper.SysUserMapper;
import io.github.atengk.service.SysUserService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 系统用户 Service 实现
 *
 * @author Ateng
 * @since 2026-05-05
 */
@Slf4j
@Service
public class SysUserServiceImpl extends ServiceImpl<SysUserMapper, SysUser> implements SysUserService {

    /**
     * 新增用户
     *
     * @param createDTO 新增参数
     * @return 用户ID
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long createUser(SysUserCreateDTO createDTO) {
        if (ObjectUtil.isNull(createDTO)) {
            throw new IllegalArgumentException("新增用户参数不能为空");
        }
        if (StrUtil.isBlank(createDTO.getUsername())) {
            throw new IllegalArgumentException("用户名不能为空");
        }

        SysUser existUser = mapper.selectByUsername(createDTO.getUsername());
        if (ObjectUtil.isNotNull(existUser)) {
            throw new IllegalArgumentException("用户名已存在");
        }

        SysUser user = BeanUtil.copyProperties(createDTO, SysUser.class);
        user.setStatus(ObjectUtil.defaultIfNull(user.getStatus(), 1));

        mapper.insertSelective(user);
        log.info("新增系统用户成功，用户ID：{}，用户名：{}", user.getId(), user.getUsername());
        return user.getId();
    }

}
```

如果业务中更适合使用编程式事务，可以使用 MyBatis-Flex 的 `Db.tx()`。官方文档说明，`Db.tx()` 返回 `false`、`null` 或抛出异常时会回滚，只有返回 `true` 时才提交；`Db.txWithResult()` 只有抛出异常时才回滚。([MyBatis-Flex](https://mybatis-flex.com/zh/core/tx.html?utm_source=chatgpt.com))

```java
boolean success = Db.tx(() -> {
    SysUser user = new SysUser();
    user.setUsername("admin");
    user.setNickname("管理员");
    user.setStatus(1);

    mapper.insertSelective(user);
    log.info("编程式事务新增用户完成，用户ID：{}", user.getId());
    return true;
});
```

在 Spring Boot 项目中，不建议同一个业务方法里混用过多事务风格。普通业务使用 `@Transactional` 即可；只有在需要明确控制返回值提交、局部事务、特殊传播行为时，再使用 `Db.tx()` 或 Spring 的 `TransactionTemplate`。

### 批量操作事务

批量操作通常用于导入数据、批量启用、批量禁用、批量删除等场景。这类操作必须放在事务中，避免执行到一半失败后产生部分成功、部分失败的数据状态。MyBatis-Flex 的 `IService` 提供 `saveBatch(entities)` 和 `saveBatch(entities, size)`；官方也说明 `BaseMapper.insertBatch` 会组装一条批量插入 SQL，小批量效率高，但数据过多时 SQL 会很大，因此更适合 100 条以内的小批量插入。([MyBatis-Flex](https://mybatis-flex.com/zh/base/service.html?utm_source=chatgpt.com))

下面代码演示批量新增用户，使用 `saveBatch(users, 500)` 按批次写入，并使用事务保证批量操作的一致性。

文件位置：`src/main/java/io/github/atengk/dto/SysUserBatchCreateDTO.java`

```java
package io.github.atengk.dto;

import lombok.Data;

import java.util.List;

/**
 * 系统用户批量新增参数
 *
 * @author Ateng
 * @since 2026-05-05
 */
@Data
public class SysUserBatchCreateDTO {

    /**
     * 用户列表
     */
    private List<SysUserCreateDTO> users;

}
```

下面代码用于批量新增系统用户，适合 Excel 导入、批量初始化账号等场景。

文件位置：`src/main/java/io/github/atengk/service/impl/SysUserServiceImpl.java`

```java
package io.github.atengk.service.impl;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.ObjectUtil;
import cn.hutool.core.util.StrUtil;
import com.mybatisflex.spring.service.impl.ServiceImpl;
import io.github.atengk.dto.SysUserBatchCreateDTO;
import io.github.atengk.dto.SysUserCreateDTO;
import io.github.atengk.entity.SysUser;
import io.github.atengk.mapper.SysUserMapper;
import io.github.atengk.service.SysUserService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * 系统用户 Service 实现
 *
 * @author Ateng
 * @since 2026-05-05
 */
@Slf4j
@Service
public class SysUserServiceImpl extends ServiceImpl<SysUserMapper, SysUser> implements SysUserService {

    private static final int BATCH_SIZE = 500;

    /**
     * 批量新增用户
     *
     * @param batchCreateDTO 批量新增参数
     * @return 是否新增成功
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public Boolean batchCreateUsers(SysUserBatchCreateDTO batchCreateDTO) {
        if (ObjectUtil.isNull(batchCreateDTO) || CollUtil.isEmpty(batchCreateDTO.getUsers())) {
            throw new IllegalArgumentException("批量新增用户列表不能为空");
        }

        List<SysUser> users = batchCreateDTO.getUsers().stream()
                .peek(this::checkCreateUserParam)
                .map(item -> {
                    SysUser user = BeanUtil.copyProperties(item, SysUser.class);
                    user.setStatus(ObjectUtil.defaultIfNull(user.getStatus(), 1));
                    return user;
                })
                .toList();

        boolean success = saveBatch(users, BATCH_SIZE);
        log.info("批量新增系统用户完成，数量：{}，结果：{}", users.size(), success);
        return success;
    }

    /**
     * 校验新增用户参数
     *
     * @param createDTO 新增参数
     */
    private void checkCreateUserParam(SysUserCreateDTO createDTO) {
        if (ObjectUtil.isNull(createDTO)) {
            throw new IllegalArgumentException("用户参数不能为空");
        }
        if (StrUtil.isBlank(createDTO.getUsername())) {
            throw new IllegalArgumentException("用户名不能为空");
        }
    }

}
```

批量修改状态的场景，也应放在事务中处理。对于少量 ID，可以使用 `updateByQuery`；对于大批量 ID，建议分批处理，并控制单次 SQL 的参数数量。

下面代码用于批量修改用户状态。

```java
@Transactional(rollbackFor = Exception.class)
public Boolean batchUpdateStatus(List<Long> ids, Integer status) {
    if (CollUtil.isEmpty(ids)) {
        throw new IllegalArgumentException("用户ID列表不能为空");
    }
    if (ObjectUtil.isNull(status)) {
        throw new IllegalArgumentException("用户状态不能为空");
    }

    SysUser updateUser = new SysUser();
    updateUser.setStatus(status);

    QueryWrapper queryWrapper = QueryWrapper.create()
            .where(SYS_USER.ID.in(ids))
            .and(SYS_USER.DELETED.eq(0));

    int rows = mapper.updateByQuery(updateUser, queryWrapper);
    log.info("批量修改用户状态完成，目标数量：{}，影响行数：{}", ids.size(), rows);
    return rows > 0;
}
```

### 异常回滚规则

异常回滚规则决定事务遇到异常时是提交还是回滚。Spring 官方文档说明，如果没有自定义回滚规则，`@Transactional` 默认对 `RuntimeException` 和 `Error` 回滚，不对受检异常回滚；可以通过 `rollbackFor`、`noRollbackFor`、`rollbackForClassName`、`noRollbackForClassName` 配置回滚规则。([Home](https://docs.spring.io/spring-framework/docs/current/javadoc-api/org/springframework/transaction/annotation/Transactional.html?utm_source=chatgpt.com))

推荐规则如下。

| 场景             | 推荐写法                                        | 说明                                                         |
| ---------------- | ----------------------------------------------- | ------------------------------------------------------------ |
| 常规业务写操作   | `@Transactional(rollbackFor = Exception.class)` | 业务异常、SQL 异常、受检异常统一回滚                         |
| 只读查询         | `@Transactional(readOnly = true)`               | 明确只读语义，便于事务管理器优化                             |
| 批量导入         | `@Transactional(rollbackFor = Exception.class)` | 任意一条失败时整体回滚                                       |
| 明确不回滚的异常 | `noRollbackFor = XxxException.class`            | 仅在业务明确允许提交时使用                                   |
| 多数据源事务     | 谨慎使用本地事务                                | MyBatis-Flex 官方说明多数据源可保持相同 commit 或 rollback 行为，但不能保证多个数据源之间的绝对原子性；分布式强一致应考虑 Seata 等方案。([MyBatis-Flex](https://mybatis-flex.com/zh/core/tx.html?utm_source=chatgpt.com)) |

下面定义一个业务运行时异常，适合在 Service 中抛出。运行时异常会触发默认回滚；如果统一写了 `rollbackFor = Exception.class`，受检异常也能按规则回滚。

文件位置：`src/main/java/io/github/atengk/common/exception/ServiceException.java`

```java
package io.github.atengk.common.exception;

/**
 * 业务异常
 *
 * @author Ateng
 * @since 2026-05-05
 */
public class ServiceException extends RuntimeException {

    /**
     * 创建业务异常
     *
     * @param message 异常消息
     */
    public ServiceException(String message) {
        super(message);
    }

}
```

下面示例演示事务中抛出业务异常后回滚。

```java
@Transactional(rollbackFor = Exception.class)
public Long createUserWithRollback(SysUserCreateDTO createDTO) {
    if (ObjectUtil.isNull(createDTO) || StrUtil.isBlank(createDTO.getUsername())) {
        throw new ServiceException("用户名不能为空");
    }

    SysUser user = BeanUtil.copyProperties(createDTO, SysUser.class);
    mapper.insertSelective(user);

    if (StrUtil.startWith(user.getUsername(), "test_")) {
        throw new ServiceException("测试账号不允许写入正式库");
    }

    log.info("新增系统用户成功，用户ID：{}", user.getId());
    return user.getId();
}
```

事务方法还需要注意调用方式：`@Transactional` 应加在由 Spring 管理的 Bean 的公开业务方法上，并通过 Spring 代理调用。不要在同一个类内部用 `this.xxx()` 调用另一个带事务的方法，否则事务代理可能不会生效。

## 常用功能实现

本部分将前面的 Mapper、Service、QueryWrapper、分页、事务组合起来，形成后台业务中最常见的新增、修改、删除、条件查询和分页查询。MyBatis-Flex 的 `BaseMapper` 提供 `insert`、`insertSelective`、`deleteById`、`deleteByQuery`、`update`、`updateByQuery` 等基础增删改方法；查询分页则提供 `selectOneById`、`selectListByQuery`、`paginate` 等能力。([MyBatis-Flex](https://mybatis-flex.com/zh/base/add-delete-update.html?utm_source=chatgpt.com))

### 新增数据

新增数据建议使用 `insertSelective` 或 Service 的 `save`。`insertSelective` 会忽略 `null` 字段，使数据库默认值生效；官方文档也说明 `insert(entity)` 不忽略 `null`，而 `insertSelective(entity)` 会忽略 `null` 数据。([MyBatis-Flex](https://mybatis-flex.com/zh/base/add-delete-update.html?utm_source=chatgpt.com))

文件位置：`src/main/java/io/github/atengk/dto/SysUserCreateDTO.java`

```java
package io.github.atengk.dto;

import lombok.Data;

/**
 * 系统用户新增参数
 *
 * @author Ateng
 * @since 2026-05-05
 */
@Data
public class SysUserCreateDTO {

    /**
     * 用户名
     */
    private String username;

    /**
     * 用户昵称
     */
    private String nickname;

    /**
     * 手机号
     */
    private String mobile;

    /**
     * 状态：0禁用，1启用
     */
    private Integer status;

}
```

下面代码用于新增系统用户，并在新增前校验用户名唯一性。

```java
@Transactional(rollbackFor = Exception.class)
public Long createUser(SysUserCreateDTO createDTO) {
    if (ObjectUtil.isNull(createDTO)) {
        throw new ServiceException("新增用户参数不能为空");
    }
    if (StrUtil.isBlank(createDTO.getUsername())) {
        throw new ServiceException("用户名不能为空");
    }

    SysUser existUser = mapper.selectByUsername(createDTO.getUsername());
    if (ObjectUtil.isNotNull(existUser)) {
        throw new ServiceException("用户名已存在");
    }

    SysUser user = BeanUtil.copyProperties(createDTO, SysUser.class);
    user.setStatus(ObjectUtil.defaultIfNull(user.getStatus(), 1));

    mapper.insertSelective(user);
    log.info("新增系统用户成功，用户ID：{}，用户名：{}", user.getId(), user.getUsername());
    return user.getId();
}
```

调用示例：

```bash
curl -X POST 'http://localhost:8080/api/users' \
  -H 'Content-Type: application/json' \
  -d '{
    "username": "admin",
    "nickname": "管理员",
    "mobile": "13800000000",
    "status": 1
  }'
```

### 修改数据

修改数据建议按主键更新，并限制可修改字段。MyBatis-Flex 的 `update(entity)` 会根据主键更新数据，实体中为 `null` 的属性默认不会更新到数据库；如果实体配置了乐观锁字段，更新时会自动追加版本条件并在更新成功后递增版本号。([MyBatis-Flex](https://mybatis-flex.com/zh/base/add-delete-update.html?utm_source=chatgpt.com))

文件位置：`src/main/java/io/github/atengk/dto/SysUserUpdateDTO.java`

```java
package io.github.atengk.dto;

import lombok.Data;

/**
 * 系统用户修改参数
 *
 * @author Ateng
 * @since 2026-05-05
 */
@Data
public class SysUserUpdateDTO {

    /**
     * 主键ID
     */
    private Long id;

    /**
     * 用户昵称
     */
    private String nickname;

    /**
     * 手机号
     */
    private String mobile;

    /**
     * 状态：0禁用，1启用
     */
    private Integer status;

    /**
     * 乐观锁版本号
     */
    private Integer version;

}
```

下面代码用于修改系统用户，只允许修改昵称、手机号、状态等业务字段。

```java
@Transactional(rollbackFor = Exception.class)
public Boolean updateUser(SysUserUpdateDTO updateDTO) {
    if (ObjectUtil.isNull(updateDTO) || ObjectUtil.isNull(updateDTO.getId())) {
        throw new ServiceException("用户ID不能为空");
    }

    SysUser dbUser = mapper.selectOneById(updateDTO.getId());
    if (ObjectUtil.isNull(dbUser)) {
        throw new ServiceException("用户不存在或已删除");
    }

    SysUser user = new SysUser();
    user.setId(updateDTO.getId());
    user.setNickname(updateDTO.getNickname());
    user.setMobile(updateDTO.getMobile());
    user.setStatus(updateDTO.getStatus());
    user.setVersion(updateDTO.getVersion());

    int rows = mapper.update(user);
    if (rows <= 0) {
        throw new ServiceException("用户修改失败，请刷新后重试");
    }

    log.info("修改系统用户成功，用户ID：{}，影响行数：{}", updateDTO.getId(), rows);
    return true;
}
```

调用示例：

```bash
curl -X PUT 'http://localhost:8080/api/users/1' \
  -H 'Content-Type: application/json' \
  -d '{
    "nickname": "系统管理员",
    "mobile": "13900000000",
    "status": 1,
    "version": 0
  }'
```

### 删除数据

删除数据可以分为物理删除和逻辑删除。MyBatis-Flex 官方逻辑删除文档说明，配置 `@Column(isLogicDelete = true)` 后，执行 `deleteById` 时不会真正删除数据，而是更新逻辑删除字段；后续 `selectOneById`、`selectListBy...`、`selectCountBy...`、`paginate` 等方法也会自动添加逻辑删除条件。([MyBatis-Flex](https://mybatis-flex.com/zh/core/logic-delete.html?utm_source=chatgpt.com))

下面代码用于按主键删除用户。如果实体类已经配置逻辑删除字段，实际执行的是逻辑删除。

```java
@Transactional(rollbackFor = Exception.class)
public Boolean deleteUser(Long id) {
    if (ObjectUtil.isNull(id)) {
        throw new ServiceException("用户ID不能为空");
    }

    SysUser dbUser = mapper.selectOneById(id);
    if (ObjectUtil.isNull(dbUser)) {
        throw new ServiceException("用户不存在或已删除");
    }

    int rows = mapper.deleteById(id);
    log.info("删除系统用户完成，用户ID：{}，影响行数：{}", id, rows);
    return rows > 0;
}
```

批量删除可以使用 `deleteBatchByIds` 或 Service 的 `removeByIds`。如果删除量较大，建议拆分批次处理。

```java
@Transactional(rollbackFor = Exception.class)
public Boolean batchDeleteUsers(List<Long> ids) {
    if (CollUtil.isEmpty(ids)) {
        throw new ServiceException("用户ID列表不能为空");
    }

    int rows = mapper.deleteBatchByIds(ids);
    log.info("批量删除系统用户完成，目标数量：{}，影响行数：{}", ids.size(), rows);
    return rows > 0;
}
```

调用示例：

```bash
curl -X DELETE 'http://localhost:8080/api/users/1'
```

### 条件查询

条件查询适合用户列表筛选、关键字搜索、状态筛选、时间范围筛选等场景。MyBatis-Flex 的 `QueryWrapper` 支持动态构造 `where`、`and`、`or` 等条件，再通过 `selectListByQuery` 执行查询。([MyBatis-Flex](https://mybatis-flex.com/zh/base/query.html?utm_source=chatgpt.com))

文件位置：`src/main/java/io/github/atengk/dto/SysUserQueryDTO.java`

```java
package io.github.atengk.dto;

import lombok.Data;

import java.time.LocalDateTime;

/**
 * 系统用户查询参数
 *
 * @author Ateng
 * @since 2026-05-05
 */
@Data
public class SysUserQueryDTO {

    /**
     * 关键字：用户名、昵称、手机号
     */
    private String keyword;

    /**
     * 状态：0禁用，1启用
     */
    private Integer status;

    /**
     * 创建开始时间
     */
    private LocalDateTime startTime;

    /**
     * 创建结束时间
     */
    private LocalDateTime endTime;

}
```

下面代码用于根据关键字、状态、创建时间范围查询用户列表。

```java
public List<SysUser> listUsers(SysUserQueryDTO queryDTO) {
    QueryWrapper queryWrapper = buildUserQueryWrapper(queryDTO)
            .orderBy(SYS_USER.CREATE_TIME.desc(), SYS_USER.ID.desc());

    List<SysUser> users = mapper.selectListByQuery(queryWrapper);
    log.info("条件查询系统用户完成，数量：{}", users.size());
    return users;
}

/**
 * 构建用户查询条件
 *
 * @param queryDTO 查询参数
 * @return 查询条件
 */
private QueryWrapper buildUserQueryWrapper(SysUserQueryDTO queryDTO) {
    QueryWrapper queryWrapper = QueryWrapper.create()
            .select()
            .from(SYS_USER)
            .where(SYS_USER.DELETED.eq(0));

    if (ObjectUtil.isNull(queryDTO)) {
        return queryWrapper;
    }

    if (ObjectUtil.isNotNull(queryDTO.getStatus())) {
        queryWrapper.and(SYS_USER.STATUS.eq(queryDTO.getStatus()));
    }

    if (ObjectUtil.isNotNull(queryDTO.getStartTime())) {
        queryWrapper.and(SYS_USER.CREATE_TIME.ge(queryDTO.getStartTime()));
    }

    if (ObjectUtil.isNotNull(queryDTO.getEndTime())) {
        queryWrapper.and(SYS_USER.CREATE_TIME.le(queryDTO.getEndTime()));
    }

    if (StrUtil.isNotBlank(queryDTO.getKeyword())) {
        String keyword = StrUtil.trim(queryDTO.getKeyword());
        queryWrapper.and(
                SYS_USER.USERNAME.like(keyword)
                        .or(SYS_USER.NICKNAME.like(keyword))
                        .or(SYS_USER.MOBILE.like(keyword))
        );
    }

    return queryWrapper;
}
```

调用示例：

```bash
curl -G 'http://localhost:8080/api/users' \
  --data-urlencode 'keyword=admin' \
  --data-urlencode 'status=1' \
  --data-urlencode 'startTime=2026-01-01T00:00:00' \
  --data-urlencode 'endTime=2026-12-31T23:59:59'
```

### 分页查询

分页查询用于后台列表页和搜索结果页。MyBatis-Flex 的 `BaseMapper` 提供 `paginate(pageNumber, pageSize, queryWrapper)` 等分页方法，`pageNumber` 从 `1` 开始，返回值 `Page<T>` 包含当前页数据、页码、每页数量、总页数和总记录数。([MyBatis-Flex](https://mybatis-flex.com/zh/base/query.html?utm_source=chatgpt.com))

文件位置：`src/main/java/io/github/atengk/dto/SysUserPageDTO.java`

```java
package io.github.atengk.dto;

import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 系统用户分页查询参数
 *
 * @author Ateng
 * @since 2026-05-05
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class SysUserPageDTO extends SysUserQueryDTO {

    /**
     * 当前页码，从 1 开始
     */
    private Integer pageNumber = 1;

    /**
     * 每页数量
     */
    private Integer pageSize = 10;

}
```

下面代码用于分页查询用户列表，并对分页参数做安全兜底。

```java
public Page<SysUser> pageUsers(SysUserPageDTO pageDTO) {
    int pageNumber = getSafePageNumber(pageDTO);
    int pageSize = getSafePageSize(pageDTO);

    QueryWrapper queryWrapper = buildUserQueryWrapper(pageDTO)
            .orderBy(SYS_USER.CREATE_TIME.desc(), SYS_USER.ID.desc());

    Page<SysUser> page = mapper.paginate(pageNumber, pageSize, queryWrapper);
    log.info("分页查询系统用户完成，页码：{}，每页：{}，总数：{}",
            pageNumber, pageSize, page.getTotalRow());
    return page;
}

/**
 * 获取安全页码
 *
 * @param pageDTO 分页参数
 * @return 页码
 */
private int getSafePageNumber(SysUserPageDTO pageDTO) {
    if (ObjectUtil.isNull(pageDTO) || ObjectUtil.isNull(pageDTO.getPageNumber())) {
        return 1;
    }
    return Math.max(pageDTO.getPageNumber(), 1);
}

/**
 * 获取安全分页大小
 *
 * @param pageDTO 分页参数
 * @return 分页大小
 */
private int getSafePageSize(SysUserPageDTO pageDTO) {
    if (ObjectUtil.isNull(pageDTO) || ObjectUtil.isNull(pageDTO.getPageSize())) {
        return 10;
    }

    int pageSize = pageDTO.getPageSize();
    if (NumberUtil.isGreater(pageSize, 100)) {
        return 100;
    }

    return Math.max(pageSize, 1);
}
```

Controller 可以统一暴露新增、修改、删除、条件查询和分页查询接口。

文件位置：`src/main/java/io/github/atengk/controller/SysUserController.java`

```java
package io.github.atengk.controller;

import com.mybatisflex.core.paginate.Page;
import io.github.atengk.dto.SysUserCreateDTO;
import io.github.atengk.dto.SysUserPageDTO;
import io.github.atengk.dto.SysUserQueryDTO;
import io.github.atengk.dto.SysUserUpdateDTO;
import io.github.atengk.entity.SysUser;
import io.github.atengk.service.SysUserService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 系统用户接口
 *
 * @author Ateng
 * @since 2026-05-05
 */
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/users")
public class SysUserController {

    private final SysUserService sysUserService;

    /**
     * 新增用户
     *
     * @param createDTO 新增参数
     * @return 用户ID
     */
    @PostMapping
    public Long createUser(@RequestBody SysUserCreateDTO createDTO) {
        return sysUserService.createUser(createDTO);
    }

    /**
     * 修改用户
     *
     * @param id 用户ID
     * @param updateDTO 修改参数
     * @return 是否修改成功
     */
    @PutMapping("/{id}")
    public Boolean updateUser(@PathVariable Long id, @RequestBody SysUserUpdateDTO updateDTO) {
        updateDTO.setId(id);
        return sysUserService.updateUser(updateDTO);
    }

    /**
     * 删除用户
     *
     * @param id 用户ID
     * @return 是否删除成功
     */
    @DeleteMapping("/{id}")
    public Boolean deleteUser(@PathVariable Long id) {
        return sysUserService.deleteUser(id);
    }

    /**
     * 条件查询用户列表
     *
     * @param queryDTO 查询参数
     * @return 用户列表
     */
    @GetMapping
    public List<SysUser> listUsers(SysUserQueryDTO queryDTO) {
        return sysUserService.listUsers(queryDTO);
    }

    /**
     * 分页查询用户列表
     *
     * @param pageDTO 分页查询参数
     * @return 分页结果
     */
    @GetMapping("/page")
    public Page<SysUser> pageUsers(SysUserPageDTO pageDTO) {
        return sysUserService.pageUsers(pageDTO);
    }

}
```

分页调用示例：

```bash
curl -G 'http://localhost:8080/api/users/page' \
  --data-urlencode 'pageNumber=1' \
  --data-urlencode 'pageSize=10' \
  --data-urlencode 'keyword=admin' \
  --data-urlencode 'status=1'
```



## 接口层开发

接口层用于对外暴露 REST API，负责接收请求参数、调用 Service 层业务方法、返回统一响应结果。Controller 不建议直接操作 Mapper，也不建议在 Controller 中拼接 `QueryWrapper`；查询条件、事务、业务校验和数据库访问应放在 Service 层。Spring Boot 测试文档也说明，Spring Boot 应用测试可以通过 `@SpringBootTest` 加载应用上下文，并可结合 MockMvc 测试 Web 端点，这也要求接口层职责清晰、边界稳定。([Home](https://docs.spring.io/spring-boot/reference/testing/spring-boot-applications.html?utm_source=chatgpt.com))

### Controller 接口设计

Controller 接口设计建议遵循 REST 风格：新增使用 `POST`，修改使用 `PUT`，删除使用 `DELETE`，详情和列表查询使用 `GET`。接口路径应围绕资源命名，例如系统用户模块统一使用 `/api/users`。

接口设计示例：

| 功能     | 请求方法 | 接口路径          | 说明                 |
| -------- | -------- | ----------------- | -------------------- |
| 新增用户 | `POST`   | `/api/users`      | 创建系统用户         |
| 修改用户 | `PUT`    | `/api/users/{id}` | 根据 ID 修改用户     |
| 删除用户 | `DELETE` | `/api/users/{id}` | 根据 ID 删除用户     |
| 查询详情 | `GET`    | `/api/users/{id}` | 根据 ID 查询用户     |
| 条件查询 | `GET`    | `/api/users`      | 根据条件查询用户列表 |
| 分页查询 | `GET`    | `/api/users/page` | 分页查询用户列表     |

下面代码给出完整 Controller 示例，统一返回 `Result<T>`，并把业务逻辑委托给 Service 层。

文件位置：`src/main/java/io/github/atengk/controller/SysUserController.java`

```java
package io.github.atengk.controller;

import com.mybatisflex.core.paginate.Page;
import io.github.atengk.common.result.Result;
import io.github.atengk.dto.SysUserCreateDTO;
import io.github.atengk.dto.SysUserPageDTO;
import io.github.atengk.dto.SysUserQueryDTO;
import io.github.atengk.dto.SysUserUpdateDTO;
import io.github.atengk.entity.SysUser;
import io.github.atengk.service.SysUserService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 系统用户接口
 *
 * @author Ateng
 * @since 2026-05-05
 */
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/users")
public class SysUserController {

    private final SysUserService sysUserService;

    /**
     * 新增用户
     *
     * @param createDTO 新增参数
     * @return 用户ID
     */
    @PostMapping
    public Result<Long> createUser(@Valid @RequestBody SysUserCreateDTO createDTO) {
        return Result.success(sysUserService.createUser(createDTO));
    }

    /**
     * 修改用户
     *
     * @param id 用户ID
     * @param updateDTO 修改参数
     * @return 是否修改成功
     */
    @PutMapping("/{id}")
    public Result<Boolean> updateUser(@PathVariable Long id,
                                      @Valid @RequestBody SysUserUpdateDTO updateDTO) {
        updateDTO.setId(id);
        return Result.success(sysUserService.updateUser(updateDTO));
    }

    /**
     * 删除用户
     *
     * @param id 用户ID
     * @return 是否删除成功
     */
    @DeleteMapping("/{id}")
    public Result<Boolean> deleteUser(@PathVariable Long id) {
        return Result.success(sysUserService.deleteUser(id));
    }

    /**
     * 查询用户详情
     *
     * @param id 用户ID
     * @return 用户详情
     */
    @GetMapping("/{id}")
    public Result<SysUser> getUser(@PathVariable Long id) {
        return Result.success(sysUserService.getById(id));
    }

    /**
     * 条件查询用户列表
     *
     * @param queryDTO 查询参数
     * @return 用户列表
     */
    @GetMapping
    public Result<List<SysUser>> listUsers(SysUserQueryDTO queryDTO) {
        return Result.success(sysUserService.listUsers(queryDTO));
    }

    /**
     * 分页查询用户列表
     *
     * @param pageDTO 分页查询参数
     * @return 分页结果
     */
    @GetMapping("/page")
    public Result<Page<SysUser>> pageUsers(SysUserPageDTO pageDTO) {
        return Result.success(sysUserService.pageUsers(pageDTO));
    }

}
```

如果使用 `@Valid` 或 `@Validated` 参数校验，需要引入 `spring-boot-starter-validation`。Spring Boot 3 使用 Jakarta Validation 包路径，因此校验注解应使用 `jakarta.validation.*`。

文件位置：`pom.xml`

```xml
<!-- 参数校验依赖，支持 @Valid、@NotBlank、@NotNull 等注解 -->
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-validation</artifactId>
</dependency>
```

### 请求参数接收

请求参数接收需要区分三类：路径参数、查询参数和请求体参数。路径参数使用 `@PathVariable`，查询参数可以直接使用 DTO 接收，JSON 请求体使用 `@RequestBody` 接收。对于新增、修改这类写操作，应配合参数校验注解限制必填字段、字段长度和数值范围。

新增参数 DTO 如下。

文件位置：`src/main/java/io/github/atengk/dto/SysUserCreateDTO.java`

```java
package io.github.atengk.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import lombok.Data;
import org.hibernate.validator.constraints.Length;

/**
 * 系统用户新增参数
 *
 * @author Ateng
 * @since 2026-05-05
 */
@Data
public class SysUserCreateDTO {

    /**
     * 用户名
     */
    @NotBlank(message = "用户名不能为空")
    @Length(max = 64, message = "用户名长度不能超过64个字符")
    private String username;

    /**
     * 用户昵称
     */
    @Length(max = 64, message = "用户昵称长度不能超过64个字符")
    private String nickname;

    /**
     * 手机号
     */
    @Pattern(regexp = "^$|^1[3-9]\\d{9}$", message = "手机号格式不正确")
    private String mobile;

    /**
     * 状态：0禁用，1启用
     */
    @NotNull(message = "用户状态不能为空")
    private Integer status;

}
```

修改参数 DTO 如下。

文件位置：`src/main/java/io/github/atengk/dto/SysUserUpdateDTO.java`

```java
package io.github.atengk.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import lombok.Data;
import org.hibernate.validator.constraints.Length;

/**
 * 系统用户修改参数
 *
 * @author Ateng
 * @since 2026-05-05
 */
@Data
public class SysUserUpdateDTO {

    /**
     * 主键ID
     */
    private Long id;

    /**
     * 用户昵称
     */
    @Length(max = 64, message = "用户昵称长度不能超过64个字符")
    private String nickname;

    /**
     * 手机号
     */
    @Pattern(regexp = "^$|^1[3-9]\\d{9}$", message = "手机号格式不正确")
    private String mobile;

    /**
     * 状态：0禁用，1启用
     */
    @NotNull(message = "用户状态不能为空")
    private Integer status;

    /**
     * 乐观锁版本号
     */
    private Integer version;

}
```

分页查询参数 DTO 如下。查询接口通常使用 GET 请求，参数直接拼接在 URL Query String 中即可。

文件位置：`src/main/java/io/github/atengk/dto/SysUserPageDTO.java`

```java
package io.github.atengk.dto;

import lombok.Data;
import org.springframework.format.annotation.DateTimeFormat;

import java.time.LocalDateTime;

/**
 * 系统用户分页查询参数
 *
 * @author Ateng
 * @since 2026-05-05
 */
@Data
public class SysUserPageDTO {

    /**
     * 当前页码，从 1 开始
     */
    private Integer pageNumber = 1;

    /**
     * 每页数量
     */
    private Integer pageSize = 10;

    /**
     * 关键字：用户名、昵称、手机号
     */
    private String keyword;

    /**
     * 状态：0禁用，1启用
     */
    private Integer status;

    /**
     * 创建开始时间
     */
    @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime startTime;

    /**
     * 创建结束时间
     */
    @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime endTime;

    /**
     * 排序字段
     */
    private String sortField;

    /**
     * 排序方向：asc 或 desc
     */
    private String sortOrder;

}
```

接口调用示例：

```bash
# 新增用户
curl -X POST 'http://localhost:8080/api/users' \
  -H 'Content-Type: application/json' \
  -d '{
    "username": "admin",
    "nickname": "管理员",
    "mobile": "13800000000",
    "status": 1
  }'

# 分页查询用户
curl -G 'http://localhost:8080/api/users/page' \
  --data-urlencode 'pageNumber=1' \
  --data-urlencode 'pageSize=10' \
  --data-urlencode 'keyword=admin' \
  --data-urlencode 'status=1' \
  --data-urlencode 'sortField=createTime' \
  --data-urlencode 'sortOrder=desc'
```

### 统一响应结果

统一响应结果用于固定接口返回结构，便于前端统一处理成功、失败、错误码、错误消息和数据内容。常见结构为 `code`、`message`、`data`、`timestamp`。对于中后台系统，建议约定 `200` 表示业务成功，非 `200` 表示业务失败或系统异常。

统一响应类如下。

文件位置：`src/main/java/io/github/atengk/common/result/Result.java`

```java
package io.github.atengk.common.result;

import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 统一响应结果
 *
 * @author Ateng
 * @since 2026-05-05
 */
@Data
public class Result<T> implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * 成功状态码
     */
    public static final Integer SUCCESS_CODE = 200;

    /**
     * 失败状态码
     */
    public static final Integer FAIL_CODE = 500;

    /**
     * 状态码
     */
    private Integer code;

    /**
     * 响应消息
     */
    private String message;

    /**
     * 响应数据
     */
    private T data;

    /**
     * 响应时间
     */
    private LocalDateTime timestamp;

    /**
     * 创建成功响应
     *
     * @param data 响应数据
     * @return 统一响应结果
     */
    public static <T> Result<T> success(T data) {
        Result<T> result = new Result<>();
        result.setCode(SUCCESS_CODE);
        result.setMessage("操作成功");
        result.setData(data);
        result.setTimestamp(LocalDateTime.now());
        return result;
    }

    /**
     * 创建失败响应
     *
     * @param message 失败消息
     * @return 统一响应结果
     */
    public static <T> Result<T> fail(String message) {
        Result<T> result = new Result<>();
        result.setCode(FAIL_CODE);
        result.setMessage(message);
        result.setTimestamp(LocalDateTime.now());
        return result;
    }

}
```

建议同时增加全局异常处理器，将参数校验异常、业务异常和系统异常转换为统一响应结构。

文件位置：`src/main/java/io/github/atengk/common/handler/GlobalExceptionHandler.java`

```java
package io.github.atengk.common.handler;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.StrUtil;
import io.github.atengk.common.exception.ServiceException;
import io.github.atengk.common.result.Result;
import lombok.extern.slf4j.Slf4j;
import org.springframework.validation.BindException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

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
     * 处理业务异常
     *
     * @param exception 业务异常
     * @return 统一响应结果
     */
    @ExceptionHandler(ServiceException.class)
    public Result<Void> handleServiceException(ServiceException exception) {
        log.warn("业务处理失败：{}", exception.getMessage());
        return Result.fail(exception.getMessage());
    }

    /**
     * 处理 JSON 请求体参数校验异常
     *
     * @param exception 参数校验异常
     * @return 统一响应结果
     */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public Result<Void> handleMethodArgumentNotValidException(MethodArgumentNotValidException exception) {
        String message = exception.getBindingResult()
                .getFieldErrors()
                .stream()
                .findFirst()
                .map(error -> StrUtil.format("{}：{}", error.getField(), error.getDefaultMessage()))
                .orElse("请求参数校验失败");

        log.warn("请求体参数校验失败：{}", message);
        return Result.fail(message);
    }

    /**
     * 处理表单和查询参数绑定异常
     *
     * @param exception 参数绑定异常
     * @return 统一响应结果
     */
    @ExceptionHandler(BindException.class)
    public Result<Void> handleBindException(BindException exception) {
        String message = CollUtil.isEmpty(exception.getFieldErrors())
                ? "请求参数绑定失败"
                : StrUtil.format("{}：{}", exception.getFieldErrors().get(0).getField(),
                exception.getFieldErrors().get(0).getDefaultMessage());

        log.warn("请求参数绑定失败：{}", message);
        return Result.fail(message);
    }

    /**
     * 处理未知异常
     *
     * @param exception 未知异常
     * @return 统一响应结果
     */
    @ExceptionHandler(Exception.class)
    public Result<Void> handleException(Exception exception) {
        log.error("系统异常", exception);
        return Result.fail("系统繁忙，请稍后再试");
    }

}
```

统一响应结果不要直接暴露 Java 异常堆栈、SQL 错误详情或数据库字段名给前端。开发环境可以查看日志定位问题，生产环境应返回稳定、可理解、无敏感信息的错误消息。

## 测试与验证

测试与验证用于确认 Mapper、Service、Controller 和 SQL 日志是否按预期工作。Spring Boot 官方文档说明，`@SpringBootTest` 可用于创建应用测试上下文，MockMvc 可用于在不真实启动服务器的情况下测试 Spring MVC Web 端点。([Home](https://docs.spring.io/spring-boot/reference/testing/spring-boot-applications.html?utm_source=chatgpt.com))

### 单元测试

单元测试建议从 Service 层开始，验证新增、修改、删除、条件查询和分页查询是否正常。由于本示例涉及数据库写入，测试方法上可以添加 `@Transactional`，让测试结束后自动回滚，避免污染本地测试库。Spring `@Transactional` 默认遇到 `RuntimeException` 和 `Error` 回滚；如果业务需要受检异常也回滚，业务方法中建议显式使用 `rollbackFor = Exception.class`。([Home](https://docs.spring.io/spring-framework/docs/current/javadoc-api/org/springframework/transaction/annotation/Transactional.html?utm_source=chatgpt.com))

测试环境配置如下。

文件位置：`src/test/resources/application-test.yml`

```yaml
spring:
  datasource:
    # 测试库地址，建议使用独立测试库
    url: jdbc:mysql://localhost:3306/flex_demo_test?useUnicode=true&characterEncoding=utf8&serverTimezone=Asia/Shanghai&useSSL=false&allowPublicKeyRetrieval=true
    username: root
    password: 123456
    driver-class-name: com.mysql.cj.jdbc.Driver

mybatis-flex:
  mapper-locations: classpath*:/mapper/**/*.xml
  configuration:
    # 测试环境开启 SQL 输出，便于断言和排查
    log-impl: org.apache.ibatis.logging.stdout.StdOutImpl
    map-underscore-to-camel-case: true

logging:
  level:
    io.github.atengk: debug
    mybatis-flex-sql: info
```

下面测试类验证 Service 层新增、修改、分页查询能力。

文件位置：`src/test/java/io/github/atengk/service/SysUserServiceTest.java`

```java
package io.github.atengk.service;

import cn.hutool.core.util.ObjectUtil;
import com.mybatisflex.core.paginate.Page;
import io.github.atengk.dto.SysUserCreateDTO;
import io.github.atengk.dto.SysUserPageDTO;
import io.github.atengk.dto.SysUserUpdateDTO;
import io.github.atengk.entity.SysUser;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

/**
 * 系统用户 Service 测试
 *
 * @author Ateng
 * @since 2026-05-05
 */
@Slf4j
@Transactional
@ActiveProfiles("test")
@SpringBootTest
class SysUserServiceTest {

    @Autowired
    private SysUserService sysUserService;

    /**
     * 测试新增用户
     */
    @Test
    void testCreateUser() {
        SysUserCreateDTO createDTO = new SysUserCreateDTO();
        createDTO.setUsername("test_admin");
        createDTO.setNickname("测试管理员");
        createDTO.setMobile("13800000000");
        createDTO.setStatus(1);

        Long userId = sysUserService.createUser(createDTO);

        Assertions.assertTrue(ObjectUtil.isNotNull(userId));
        log.info("新增用户测试通过，用户ID：{}", userId);
    }

    /**
     * 测试修改用户
     */
    @Test
    void testUpdateUser() {
        SysUserCreateDTO createDTO = new SysUserCreateDTO();
        createDTO.setUsername("test_update_user");
        createDTO.setNickname("修改前用户");
        createDTO.setMobile("13800000001");
        createDTO.setStatus(1);

        Long userId = sysUserService.createUser(createDTO);

        SysUserUpdateDTO updateDTO = new SysUserUpdateDTO();
        updateDTO.setId(userId);
        updateDTO.setNickname("修改后用户");
        updateDTO.setMobile("13900000001");
        updateDTO.setStatus(1);

        Boolean result = sysUserService.updateUser(updateDTO);

        Assertions.assertTrue(result);
        log.info("修改用户测试通过，用户ID：{}", userId);
    }

    /**
     * 测试分页查询用户
     */
    @Test
    void testPageUsers() {
        SysUserPageDTO pageDTO = new SysUserPageDTO();
        pageDTO.setPageNumber(1);
        pageDTO.setPageSize(10);
        pageDTO.setKeyword("test");
        pageDTO.setStatus(1);

        Page<SysUser> page = sysUserService.pageUsers(pageDTO);

        Assertions.assertNotNull(page);
        Assertions.assertTrue(page.getPageNumber() >= 1);
        Assertions.assertTrue(page.getPageSize() >= 1);
        log.info("分页查询测试通过，总数：{}", page.getTotalRow());
    }

}
```

如果测试库中已有唯一索引，例如 `username` 唯一，测试数据需要避免重复。可以使用时间戳或 UUID 拼接用户名，也可以在测试方法执行前清理测试数据。

### 接口测试

接口测试用于验证 Controller 参数接收、统一响应结构、异常处理和 HTTP 状态是否符合预期。可以使用 curl、Postman、Apifox，也可以使用 MockMvc 自动化测试。Spring Boot 文档说明，默认情况下 `@SpringBootTest` 不启动真实服务器，而是创建 Mock Web 环境；结合 MockMvc 可以测试 MVC 端点。([Home](https://docs.spring.io/spring-boot/reference/testing/spring-boot-applications.html?utm_source=chatgpt.com))

下面是 curl 接口测试命令。

```bash
# 新增用户
curl -X POST 'http://localhost:8080/api/users' \
  -H 'Content-Type: application/json' \
  -d '{
    "username": "api_admin",
    "nickname": "接口管理员",
    "mobile": "13800000002",
    "status": 1
  }'

# 修改用户
curl -X PUT 'http://localhost:8080/api/users/1' \
  -H 'Content-Type: application/json' \
  -d '{
    "nickname": "接口管理员-修改",
    "mobile": "13900000002",
    "status": 1,
    "version": 0
  }'

# 查询详情
curl -X GET 'http://localhost:8080/api/users/1'

# 条件查询
curl -G 'http://localhost:8080/api/users' \
  --data-urlencode 'keyword=admin' \
  --data-urlencode 'status=1'

# 分页查询
curl -G 'http://localhost:8080/api/users/page' \
  --data-urlencode 'pageNumber=1' \
  --data-urlencode 'pageSize=10' \
  --data-urlencode 'keyword=admin' \
  --data-urlencode 'sortField=createTime' \
  --data-urlencode 'sortOrder=desc'

# 删除用户
curl -X DELETE 'http://localhost:8080/api/users/1'
```

MockMvc 自动化测试如下。

文件位置：`src/test/java/io/github/atengk/controller/SysUserControllerTest.java`

```java
package io.github.atengk.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.github.atengk.dto.SysUserCreateDTO;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import static org.hamcrest.Matchers.equalTo;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * 系统用户接口测试
 *
 * @author Ateng
 * @since 2026-05-05
 */
@Slf4j
@Transactional
@ActiveProfiles("test")
@AutoConfigureMockMvc
@SpringBootTest
class SysUserControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    /**
     * 测试新增用户接口
     *
     * @throws Exception 测试异常
     */
    @Test
    void testCreateUserApi() throws Exception {
        SysUserCreateDTO createDTO = new SysUserCreateDTO();
        createDTO.setUsername("mock_admin");
        createDTO.setNickname("Mock管理员");
        createDTO.setMobile("13800000003");
        createDTO.setStatus(1);

        mockMvc.perform(post("/api/users")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(createDTO)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code", equalTo(200)))
                .andExpect(jsonPath("$.message", equalTo("操作成功")))
                .andExpect(jsonPath("$.data").exists());

        log.info("新增用户接口测试通过");
    }

    /**
     * 测试分页查询接口
     *
     * @throws Exception 测试异常
     */
    @Test
    void testPageUsersApi() throws Exception {
        mockMvc.perform(get("/api/users/page")
                        .param("pageNumber", "1")
                        .param("pageSize", "10")
                        .param("keyword", "admin")
                        .param("status", "1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code", equalTo(200)))
                .andExpect(jsonPath("$.data").exists());

        log.info("分页查询接口测试通过");
    }

}
```

接口测试建议覆盖正常请求、必填参数缺失、参数格式错误、业务异常和空数据场景。对于新增和修改接口，应重点测试参数校验和唯一性约束；对于分页接口，应测试 `pageNumber`、`pageSize` 的边界值。

### SQL 日志验证

SQL 日志验证用于确认实际执行的 SQL 是否符合预期，尤其适合排查条件拼接、分页查询、逻辑删除、乐观锁和慢查询问题。MyBatis-Flex 官方文档说明，可以通过 SQL 审计模块开启 SQL 打印，`AuditManager.setAuditEnable(true)` 开启审计，并通过 `ConsoleMessageCollector` 或自定义日志收集器输出完整 SQL 和执行耗时。([MyBatis-Flex](https://mybatis-flex.com/zh/core/sql-print.html?utm_source=chatgpt.com))

开发环境推荐使用日志方式输出 SQL，便于按 logger 名称、级别和文件进行管理。

文件位置：`src/main/java/io/github/atengk/config/MyBatisFlexSqlLogConfiguration.java`

```java
package io.github.atengk.config;

import com.mybatisflex.core.audit.AuditManager;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;

/**
 * MyBatis-Flex SQL 日志配置
 *
 * @author Ateng
 * @since 2026-05-05
 */
@Slf4j
@Profile({"dev", "test"})
@Configuration
public class MyBatisFlexSqlLogConfiguration {

    private static final Logger SQL_LOGGER = LoggerFactory.getLogger("mybatis-flex-sql");

    /**
     * 初始化 SQL 审计日志
     */
    public MyBatisFlexSqlLogConfiguration() {
        AuditManager.setAuditEnable(true);
        AuditManager.setMessageCollector(auditMessage -> SQL_LOGGER.info(
                "SQL执行完成，耗时：{}ms，语句：{}",
                auditMessage.getElapsedTime(),
                auditMessage.getFullSql()
        ));
        log.info("已开启 MyBatis-Flex SQL 审计日志");
    }

}
```

日志配置如下。

文件位置：`src/main/resources/application-dev.yml`

```yaml
logging:
  level:
    # 项目包日志
    io.github.atengk: debug

    # MyBatis-Flex SQL 审计日志
    mybatis-flex-sql: info
```

验证 SQL 日志时重点观察以下内容：

| 验证点   | 期望结果                                             |
| -------- | ---------------------------------------------------- |
| 条件查询 | 关键字、状态、时间范围只在有值时拼接                 |
| 分页查询 | SQL 中出现分页语句，并执行总数查询                   |
| 逻辑删除 | 查询自动过滤 `deleted = 0`，删除执行更新逻辑删除字段 |
| 乐观锁   | 修改时带版本号条件，成功后版本号递增                 |
| 排序参数 | 只出现白名单允许的排序字段                           |
| 执行耗时 | 慢 SQL 能从日志中快速定位                            |

生产环境不建议长期输出完整 SQL。完整 SQL 可能包含手机号、身份证号、Token、业务编号等敏感数据。如果必须开启，应配合日志脱敏、慢 SQL 阈值、短期开关和访问权限控制。

## 项目实践建议

项目实践建议用于统一团队开发规范，减少后续维护成本。MyBatis-Flex 已经提供 `BaseMapper`、`QueryWrapper`、`IService`、分页查询、SQL 审计等能力，其中 `BaseMapper` 内置基础增删改查和分页查询，`insertSelective` 会忽略空值，`deleteById`、`deleteBatchByIds`、`deleteByQuery` 等用于删除数据。([MyBatis-Flex](https://mybatis-flex.com/zh/base/add-delete-update.html?utm_source=chatgpt.com)) 规范化使用这些能力，比每个模块单独封装一套写法更容易维护。

### 分层规范

Spring Boot 3 + MyBatis-Flex 项目建议采用清晰的分层结构。Controller 负责接口，Service 负责业务，Mapper 负责数据库访问，Entity 负责表映射，DTO/VO 负责请求和响应模型。

推荐目录结构如下。

```text
src
└── main
    ├── java
    │   └── io/github/atengk
    │       ├── common
    │       │   ├── exception
    │       │   ├── handler
    │       │   └── result
    │       ├── config
    │       ├── controller
    │       ├── dto
    │       ├── entity
    │       ├── mapper
    │       ├── service
    │       │   └── impl
    │       └── vo
    └── resources
        ├── application.yml
        ├── application-dev.yml
        └── mapper
```

各层职责建议如下。

| 分层           | 主要职责                     | 不建议做的事                     |
| -------------- | ---------------------------- | -------------------------------- |
| `controller`   | 接收请求、参数校验、返回响应 | 不直接操作 Mapper，不拼 SQL      |
| `service`      | 业务规则、事务控制、条件封装 | 不返回数据库异常细节给前端       |
| `service.impl` | 业务实现、调用 Mapper        | 不写过长方法，复杂逻辑拆私有方法 |
| `mapper`       | 数据访问、XML SQL 映射       | 不写业务判断                     |
| `entity`       | 数据库表映射                 | 不混入复杂接口展示字段           |
| `dto`          | 请求参数                     | 不直接作为数据库实体保存复杂对象 |
| `vo`           | 接口响应对象                 | 不承担数据库写入职责             |
| `common`       | 统一结果、异常、工具封装     | 不放具体业务逻辑                 |
| `config`       | 配置类                       | 不写业务流程                     |

Service 方法建议按业务语义命名，例如 `createUser`、`updateUser`、`deleteUser`、`pageUsers`，而不是简单复用 `insert`、`update`、`delete` 这类数据库动作命名。这样 Controller 调用时语义更清楚，也便于后续加权限、审计、消息通知、缓存刷新等逻辑。

### 命名规范

命名规范直接影响代码可读性和团队协作效率。MyBatis-Flex 项目中，建议数据库、实体、Mapper、Service、DTO、VO、XML 保持固定命名规则，减少查找成本。

推荐命名规则如下。

| 类型         | 命名示例             | 说明                       |
| ------------ | -------------------- | -------------------------- |
| 数据库表     | `sys_user`           | 小写下划线，按模块前缀区分 |
| 表字段       | `create_time`        | 小写下划线                 |
| 实体类       | `SysUser`            | 与表名对应，使用大驼峰     |
| Mapper       | `SysUserMapper`      | 实体名 + `Mapper`          |
| Service      | `SysUserService`     | 实体名 + `Service`         |
| Service 实现 | `SysUserServiceImpl` | 实体名 + `ServiceImpl`     |
| Controller   | `SysUserController`  | 实体名 + `Controller`      |
| 新增 DTO     | `SysUserCreateDTO`   | 业务对象 + 操作 + `DTO`    |
| 修改 DTO     | `SysUserUpdateDTO`   | 业务对象 + 操作 + `DTO`    |
| 查询 DTO     | `SysUserQueryDTO`    | 业务对象 + 查询 + `DTO`    |
| 分页 DTO     | `SysUserPageDTO`     | 业务对象 + 分页 + `DTO`    |
| 响应 VO      | `SysUserVO`          | 业务对象 + `VO`            |
| XML 文件     | `SysUserMapper.xml`  | 与 Mapper 接口同名         |

数据库字段建议统一：

| 字段          | 含义         |
| ------------- | ------------ |
| `id`          | 主键         |
| `create_time` | 创建时间     |
| `update_time` | 更新时间     |
| `deleted`     | 逻辑删除标识 |
| `version`     | 乐观锁版本号 |
| `create_by`   | 创建人       |
| `update_by`   | 更新人       |
| `remark`      | 备注         |

MyBatis-Flex 的 `QueryWrapper` 支持通过 APT 生成的表定义类引用字段，官方文档中也说明 `ACCOUNT.ID.ge(100)` 中的 `ACCOUNT` 来自 APT 自动生成。使用表定义类可以减少字符串字段名硬编码，并提升 IDE 提示和重构安全性。([MyBatis-Flex](https://mybatis-flex.com/zh/base/querywrapper.html?utm_source=chatgpt.com))

### 常见问题处理

常见问题处理用于记录开发中高频出现的配置、映射、分页、逻辑删除和 SQL 日志问题。遇到问题时，应优先从依赖版本、Mapper 扫描、实体注解、XML 路径、SQL 日志、数据库字段和事务边界排查。

| 问题                        | 常见原因                                         | 处理方式                                                |
| --------------------------- | ------------------------------------------------ | ------------------------------------------------------- |
| Mapper 无法注入             | 未配置 `@MapperScan` 或扫描包错误                | 在启动类增加 `@MapperScan("io.github.atengk.mapper")`   |
| XML SQL 不生效              | `mapper-locations` 路径错误或 XML namespace 错误 | 检查 `classpath*:/mapper/**/*.xml` 和 Mapper 全限定名   |
| 查询不到已存在数据          | 逻辑删除字段生效，数据已被标记删除               | 检查 `deleted` 字段值和 `@Column(isLogicDelete = true)` |
| 新增后默认值未生效          | 使用 `insert` 插入了 `null`                      | 改用 `insertSelective`                                  |
| 修改失败                    | 乐观锁版本号不一致或 ID 为空                     | 检查 `version` 和主键参数                               |
| 分页数据顺序不稳定          | 缺少固定排序字段                                 | 增加 `create_time desc, id desc`                        |
| 排序存在风险                | 前端直接传数据库字段名                           | 使用排序字段白名单映射                                  |
| SQL 没有打印                | 未开启 MyBatis 日志或 MyBatis-Flex SQL 审计      | 配置 `AuditManager` 或 `log-impl`                       |
| 单元测试污染数据            | 测试未使用事务回滚或独立测试库                   | 使用 `@Transactional` 和 `application-test.yml`         |
| Controller 参数时间接收失败 | 时间格式不匹配                                   | 使用 `@DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss")` |

逻辑删除相关问题需要特别注意。MyBatis-Flex 官方文档说明，配置逻辑删除后，`deleteById` 实际执行更新逻辑删除字段，而不是物理删除；后续 `selectOneById`、`selectListBy...`、`selectCountBy...`、`paginate` 等方法也会自动添加逻辑删除条件。([MyBatis-Flex](https://mybatis-flex.com/zh/core/logic-delete.html?utm_source=chatgpt.com))

分页相关问题也应按官方能力使用。MyBatis-Flex 的 `BaseMapper` 提供 `paginate(pageNumber, pageSize, queryWrapper)`、`paginate(page, queryWrapper)`、`paginateWithRelations(...)` 等分页方法，普通分页列表不需要额外自行拼 `limit` 和 `offset`。([MyBatis-Flex](https://mybatis-flex.com/zh/base/query.html?utm_source=chatgpt.com))

最终建议：

1. 简单 CRUD 使用 `BaseMapper` 或 `IService`。
2. 动态查询使用 `QueryWrapper`。
3. 复杂 SQL 使用 XML Mapper。
4. 写操作统一放在 Service 层，并配置事务。
5. 接口返回统一使用 `Result<T>`。
6. 排序字段必须使用白名单。
7. 开发和测试环境开启 SQL 日志，生产环境谨慎开启完整 SQL。
8. 测试使用独立测试库，避免污染开发和生产数据。

