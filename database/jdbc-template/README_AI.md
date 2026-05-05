# JDBC Template

JDBC Template 是 Spring 对原生 JDBC 的轻量封装，适合在 Spring Boot 3 项目中直接编写 SQL、控制查询性能、减少 ORM 抽象成本。Spring Framework 文档明确说明，`JdbcTemplate` 会处理 JDBC 资源创建与释放，应用代码主要负责提供 SQL 和结果集提取逻辑。([Home](https://docs.spring.io/spring-framework/reference/data-access/jdbc/core.html?utm_source=chatgpt.com))

## 技术概述

本节用于说明 JDBC Template 在 Spring Boot 3 数据访问体系中的定位、适用场景，以及它和 JPA、MyBatis 的主要差异。

### JDBC Template 定位

`JdbcTemplate` 位于 Spring JDBC 模块中，是对原生 JDBC 的模板化封装。它不提供 ORM 能力，也不负责自动生成 SQL，而是帮助开发者处理 `Connection`、`PreparedStatement`、`ResultSet` 等底层资源管理，并提供统一的查询、更新、批处理和异常转换能力。Spring Boot 3 会自动配置 `JdbcTemplate` 和 `NamedParameterJdbcTemplate`，业务组件可以直接通过构造方法注入使用。([Home](https://docs.spring.io/spring-boot/docs/3.0.12/reference/html/data.html?utm_source=chatgpt.com))

在实际项目中，`JdbcTemplate` 更接近“SQL 执行工具”而不是“持久层框架”。它保留了 SQL 的完全控制权，适合对 SQL 可读性、执行计划、复杂查询、批量写入有明确要求的场景。

### 适用场景

`JdbcTemplate` 适合以下开发场景：

| 场景                | 说明                                         |
| ------------------- | -------------------------------------------- |
| 简单 CRUD           | 表结构清晰、SQL 简单，不需要复杂 ORM 映射    |
| 报表查询            | SQL 复杂、字段多、聚合多，直接写 SQL 更清晰  |
| 批量写入            | 需要使用 JDBC 批处理提高写入效率             |
| 存量系统维护        | 数据库表结构固定，不希望引入实体关系映射     |
| 对 SQL 可控性要求高 | 需要直接控制 SQL、索引命中、分页语句、锁语句 |
| 轻量服务            | 不想引入 MyBatis、JPA 等更重的数据访问层     |

不适合的场景主要包括：领域模型关系复杂、实体生命周期管理要求高、需要自动脏检查、级联保存、复杂对象关系映射等。这类场景通常更适合 JPA 或 MyBatis。

### 与 JPA、MyBatis 的对比

JPA 是标准 ORM 技术，Spring Boot 3 文档将其描述为用于将对象映射到关系数据库的标准技术；MyBatis 则以 SQL 映射为核心，官方文档强调其 Mapper XML 和 Mapped Statements，目标是让开发者专注 SQL，同时减少原始 JDBC 样板代码。([Home](https://docs.spring.io/spring-boot/docs/3.0.12/reference/html/data.html?utm_source=chatgpt.com))

| 对比项     | JdbcTemplate           | JPA                                 | MyBatis                  |
| ---------- | ---------------------- | ----------------------------------- | ------------------------ |
| 核心思想   | 手写 SQL，模板执行     | 面向对象，实体映射                  | 手写 SQL，Mapper 映射    |
| SQL 控制权 | 最高                   | 较低，复杂 SQL 需 JPQL/Native SQL   | 高                       |
| 学习成本   | 低                     | 中高                                | 中                       |
| 映射能力   | 手动映射               | 自动 ORM 映射                       | 半自动映射               |
| 复杂查询   | 强，直接 SQL           | 一般，复杂场景容易退化为 Native SQL | 强                       |
| 批量操作   | 强                     | 依赖实现和配置                      | 强                       |
| 适合项目   | 轻量服务、报表、批处理 | 领域模型复杂的业务系统              | SQL 较多的中大型业务系统 |
| 维护成本   | SQL 分散时成本上升     | 实体关系复杂时成本上升              | Mapper 较多时成本上升    |

实践中可以这样选择：

- 查询简单、项目轻量、希望直接控制 SQL：优先选择 `JdbcTemplate`。
- 业务模型复杂、实体关系明显、偏领域建模：优先选择 JPA。
- SQL 较多、需要 XML 管理 SQL、团队熟悉 Mapper 模式：优先选择 MyBatis。

## 环境准备

本节给出 Spring Boot 3 集成 JDBC Template 的基础依赖、数据库连接配置和初始化脚本。Spring Boot 的 `DataSource` 配置由 `spring.datasource.*` 控制，通常只需要配置数据库连接 URL、用户名和密码，驱动类大多数情况下可由 Spring Boot 根据 JDBC URL 推断。([Home](https://docs.spring.io/spring-boot/docs/3.0.12/reference/html/data.html?utm_source=chatgpt.com))

### 项目依赖配置

在已有 Spring Boot 3 项目的 `pom.xml` 中添加以下依赖。版本建议由 Spring Boot Parent 或 BOM 统一管理，业务模块不要单独指定 Spring 相关依赖版本。

`spring-boot-starter-jdbc` 提供 Spring JDBC、`JdbcTemplate` 和默认连接池支持；MySQL 驱动用于连接 MySQL 数据库；Hutool 用于常用工具处理；Lombok 用于减少实体类样板代码。

```xml
<dependencies>
    <!-- Spring JDBC：提供 JdbcTemplate、NamedParameterJdbcTemplate、事务等能力 -->
    <dependency>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-starter-jdbc</artifactId>
    </dependency>

    <!-- Spring Web：用于后续 Controller 接口验证，可按需添加 -->
    <dependency>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-starter-web</artifactId>
    </dependency>

    <!-- MySQL 驱动：运行时连接 MySQL 数据库 -->
    <dependency>
        <groupId>com.mysql</groupId>
        <artifactId>mysql-connector-j</artifactId>
        <scope>runtime</scope>
    </dependency>

    <!-- Hutool：常用字符串、集合、日期等工具类 -->
    <dependency>
        <groupId>cn.hutool</groupId>
        <artifactId>hutool-all</artifactId>
        <version>5.8.38</version>
    </dependency>

    <!-- Lombok：减少实体类 getter、setter、构造器等样板代码 -->
    <dependency>
        <groupId>org.projectlombok</groupId>
        <artifactId>lombok</artifactId>
        <optional>true</optional>
    </dependency>

    <!-- Spring Boot Test：用于后续单元测试和集成测试 -->
    <dependency>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-starter-test</artifactId>
        <scope>test</scope>
    </dependency>
</dependencies>
```

### 数据库连接配置

文件位置：`src/main/resources/application.yml`

下面配置用于连接本地 MySQL，并对 `JdbcTemplate` 查询结果数量、查询超时时间做基础限制。`spring.jdbc.template.*` 是 Spring Boot 暴露的 JdbcTemplate 定制配置项。([Home](https://docs.spring.io/spring-boot/docs/3.0.12/reference/html/data.html?utm_source=chatgpt.com))

```yaml
server:
  port: 8080

spring:
  application:
    name: springboot3-jdbc-template-demo

  datasource:
    # 数据库连接地址，按实际库名、时区、字符集调整
    url: jdbc:mysql://127.0.0.1:3306/demo_jdbc?useUnicode=true&characterEncoding=utf8&serverTimezone=Asia/Shanghai&useSSL=false
    # 数据库用户名
    username: root
    # 数据库密码，生产环境建议使用环境变量或配置中心
    password: root
    # MySQL 8+ 驱动类；一般可由 Spring Boot 根据 URL 推断
    driver-class-name: com.mysql.cj.jdbc.Driver
    hikari:
      # 连接池名称，便于日志排查
      pool-name: HikariPool-JdbcTemplate
      # 最大连接数，根据业务并发和数据库规格调整
      maximum-pool-size: 10
      # 最小空闲连接数
      minimum-idle: 2
      # 获取连接最大等待时间，单位毫秒
      connection-timeout: 30000

  jdbc:
    template:
      # 单次查询最大返回行数，防止误查询过大结果集
      max-rows: 1000
      # SQL 查询超时时间，单位秒
      query-timeout: 10

logging:
  level:
    # 查看当前示例代码日志
    io.github.atengk: debug
```

### 数据源初始化

Spring Boot 支持通过基础 SQL 脚本初始化数据库结构和数据。默认会加载类路径下的 `schema.sql` 和 `data.sql`，也可以使用 `spring.sql.init.schema-locations`、`spring.sql.init.data-locations` 自定义路径；对非嵌入式数据库，如果希望启动时也执行初始化脚本，需要将 `spring.sql.init.mode` 设置为 `always`。([Home](https://docs.spring.io/spring-boot/docs/3.0.12/reference/html/howto.html?utm_source=chatgpt.com))

文件位置：`src/main/resources/application.yml`

```yaml
spring:
  sql:
    init:
      # always：每次启动都尝试执行初始化脚本；生产环境谨慎使用
      mode: always
      # 指定建表脚本位置
      schema-locations: classpath:sql/schema.sql
      # 指定初始化数据脚本位置
      data-locations: classpath:sql/data.sql
      # SQL 执行失败时是否继续启动；开发环境可为 false，便于尽早发现问题
      continue-on-error: false
```

文件位置：`src/main/resources/sql/schema.sql`

下面脚本创建用户表，字段名采用数据库常见的下划线命名，Java 对象中使用驼峰命名。

```sql
-- 用户表：用于演示 JdbcTemplate 基础 CRUD
CREATE TABLE IF NOT EXISTS sys_user (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '主键ID',
    username VARCHAR(64) NOT NULL COMMENT '用户名',
    nickname VARCHAR(64) NOT NULL COMMENT '昵称',
    age INT DEFAULT NULL COMMENT '年龄',
    status TINYINT NOT NULL DEFAULT 1 COMMENT '状态：0禁用，1启用',
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    UNIQUE KEY uk_sys_user_username (username)
) COMMENT = '系统用户表';
```

文件位置：`src/main/resources/sql/data.sql`

下面脚本写入初始化数据，便于启动后直接验证查询、更新、删除方法。

```sql
-- 初始化用户数据：用于本地开发和接口验证
INSERT INTO sys_user (username, nickname, age, status)
VALUES
    ('admin', '管理员', 30, 1),
    ('test', '测试用户', 22, 1)
ON DUPLICATE KEY UPDATE
    nickname = VALUES(nickname),
    age = VALUES(age),
    status = VALUES(status);
```

## 基础使用

本节通过一个最小可运行的 Repository 示例演示 `JdbcTemplate` 的注入、单条查询、列表查询、新增、更新和删除。Spring Framework 的 `JdbcTemplate` API 提供 `queryForObject`、`query`、`update`、`batchUpdate` 等常用方法，可覆盖大多数基础 JDBC 操作。([Home](https://docs.spring.io/spring-framework/docs/current/javadoc-api/org/springframework/jdbc/core/JdbcTemplate.html?utm_source=chatgpt.com))

### 示例文件结构

```text
src/main/java/io/github/atengk/jdbc/entity/SysUser.java
src/main/java/io/github/atengk/jdbc/repository/SysUserRepository.java
```

### JdbcTemplate 注入方式

推荐使用构造方法注入，便于单元测试，也避免字段注入带来的不可变性问题。Spring Boot 自动配置 `JdbcTemplate` 后，Repository、Service、Component 中可以直接声明构造参数。

文件位置：`src/main/java/io/github/atengk/jdbc/entity/SysUser.java`

下面实体类用于承接 `sys_user` 表查询结果。

```java
package io.github.atengk.jdbc.entity;

import lombok.Data;

import java.time.LocalDateTime;

/**
 * 系统用户实体
 *
 * @author Ateng
 * @since 2026-05-04
 */
@Data
public class SysUser {

    /**
     * 主键ID
     */
    private Long id;

    /**
     * 用户名
     */
    private String username;

    /**
     * 昵称
     */
    private String nickname;

    /**
     * 年龄
     */
    private Integer age;

    /**
     * 状态：0禁用，1启用
     */
    private Integer status;

    /**
     * 创建时间
     */
    private LocalDateTime createTime;

    /**
     * 更新时间
     */
    private LocalDateTime updateTime;
}
```

文件位置：`src/main/java/io/github/atengk/jdbc/repository/SysUserRepository.java`

下面 Repository 使用构造方法注入 `JdbcTemplate`，并通过自定义 `RowMapper` 完成结果集到 Java 对象的映射。

```java
package io.github.atengk.jdbc.repository;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.text.CharSequenceUtil;
import io.github.atengk.jdbc.entity.SysUser;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * 系统用户 JDBC Repository
 *
 * @author Ateng
 * @since 2026-05-04
 */
@Slf4j
@Repository
@RequiredArgsConstructor
public class SysUserRepository {

    private final JdbcTemplate jdbcTemplate;

    /**
     * 用户结果集映射
     */
    private static final RowMapper<SysUser> USER_ROW_MAPPER = (rs, rowNum) -> {
        SysUser user = new SysUser();
        user.setId(rs.getLong("id"));
        user.setUsername(rs.getString("username"));
        user.setNickname(rs.getString("nickname"));
        user.setAge(rs.getObject("age", Integer.class));
        user.setStatus(rs.getObject("status", Integer.class));
        user.setCreateTime(rs.getTimestamp("create_time").toLocalDateTime());
        user.setUpdateTime(rs.getTimestamp("update_time").toLocalDateTime());
        return user;
    };

    /**
     * 根据ID查询单个用户
     *
     * @param id 用户ID
     * @return 用户信息，不存在时返回 null
     */
    public SysUser findById(Long id) {
        String sql = """
                SELECT id, username, nickname, age, status, create_time, update_time
                FROM sys_user
                WHERE id = ?
                """;
        try {
            return jdbcTemplate.queryForObject(sql, USER_ROW_MAPPER, id);
        } catch (EmptyResultDataAccessException e) {
            log.info("用户不存在，id={}", id);
            return null;
        }
    }

    /**
     * 查询用户列表
     *
     * @param keyword 用户名或昵称关键字
     * @return 用户列表
     */
    public List<SysUser> findList(String keyword) {
        String baseSql = """
                SELECT id, username, nickname, age, status, create_time, update_time
                FROM sys_user
                """;

        if (CharSequenceUtil.isBlank(keyword)) {
            String sql = baseSql + " ORDER BY id DESC";
            return jdbcTemplate.query(sql, USER_ROW_MAPPER);
        }

        String sql = baseSql + """
                WHERE username LIKE ? OR nickname LIKE ?
                ORDER BY id DESC
                """;
        String likeKeyword = "%" + keyword + "%";
        return jdbcTemplate.query(sql, USER_ROW_MAPPER, likeKeyword, likeKeyword);
    }

    /**
     * 新增用户
     *
     * @param user 用户信息
     * @return 影响行数
     */
    public int insert(SysUser user) {
        String sql = """
                INSERT INTO sys_user (username, nickname, age, status)
                VALUES (?, ?, ?, ?)
                """;
        int rows = jdbcTemplate.update(
                sql,
                user.getUsername(),
                user.getNickname(),
                user.getAge(),
                user.getStatus()
        );
        log.info("新增用户完成，username={}，影响行数={}", user.getUsername(), rows);
        return rows;
    }

    /**
     * 更新用户
     *
     * @param user 用户信息
     * @return 影响行数
     */
    public int updateById(SysUser user) {
        String sql = """
                UPDATE sys_user
                SET nickname = ?, age = ?, status = ?
                WHERE id = ?
                """;
        int rows = jdbcTemplate.update(
                sql,
                user.getNickname(),
                user.getAge(),
                user.getStatus(),
                user.getId()
        );
        log.info("更新用户完成，id={}，影响行数={}", user.getId(), rows);
        return rows;
    }

    /**
     * 根据ID删除用户
     *
     * @param id 用户ID
     * @return 影响行数
     */
    public int deleteById(Long id) {
        String sql = "DELETE FROM sys_user WHERE id = ?";
        int rows = jdbcTemplate.update(sql, id);
        log.info("删除用户完成，id={}，影响行数={}", id, rows);
        return rows;
    }

    /**
     * 判断查询结果是否为空
     *
     * @param keyword 查询关键字
     * @return true 表示存在用户，false 表示不存在
     */
    public boolean existsByKeyword(String keyword) {
        List<SysUser> users = this.findList(keyword);
        return CollUtil.isNotEmpty(users);
    }
}
```

### 查询单条数据

单条查询通常使用 `queryForObject`。需要注意的是，当查询结果为空时，`queryForObject` 会抛出 `EmptyResultDataAccessException`，所以业务代码中应当明确处理“无数据”的情况，而不是让异常直接向上抛出。

```java
SysUser user = sysUserRepository.findById(1L);
if (user == null) {
    log.info("未查询到用户");
} else {
    log.info("查询到用户：{}", user.getUsername());
}
```

### 查询列表数据

列表查询通常使用 `query`。如果查询条件可选，建议在 Repository 层中统一拼接 SQL，并使用占位符传参，不要直接拼接用户输入，避免 SQL 注入风险。

```java
List<SysUser> users = sysUserRepository.findList("admin");
if (CollUtil.isEmpty(users)) {
    log.info("未查询到匹配用户");
} else {
    log.info("查询用户数量：{}", users.size());
}
```

### 新增数据

新增、更新、删除都使用 `jdbcTemplate.update(...)`。该方法返回 SQL 影响行数，业务层可以根据返回值判断操作是否成功。

```java
SysUser user = new SysUser();
user.setUsername("zhangsan");
user.setNickname("张三");
user.setAge(25);
user.setStatus(1);

int rows = sysUserRepository.insert(user);
log.info("新增结果，影响行数={}", rows);
```

### 更新数据

更新时建议以主键作为条件，并在业务层判断影响行数。如果影响行数为 `0`，通常表示记录不存在或数据未发生变化，需要结合业务语义处理。

```java
SysUser user = new SysUser();
user.setId(1L);
user.setNickname("系统管理员");
user.setAge(31);
user.setStatus(1);

int rows = sysUserRepository.updateById(user);
if (rows == 0) {
    log.info("用户更新失败，记录不存在，id={}", user.getId());
}
```

### 删除数据

删除数据时建议优先采用逻辑删除；如果确实需要物理删除，应当在 Service 层增加权限校验、业务状态校验和操作日志。这里仅演示基础物理删除。

```java
Long id = 2L;
int rows = sysUserRepository.deleteById(id);
if (rows > 0) {
    log.info("用户删除成功，id={}", id);
} else {
    log.info("用户删除失败，记录不存在，id={}", id);
}
```

### 基础使用注意事项

`JdbcTemplate` 虽然轻量，但 SQL、参数、映射都需要开发者自己维护，因此建议遵循以下规则：

1. SQL 参数必须使用 `?` 占位符或 `NamedParameterJdbcTemplate` 命名参数，不要拼接用户输入。
2. 查询字段建议显式列出，不建议长期使用 `SELECT *`。
3. 表字段和 Java 字段命名不一致时，优先使用自定义 `RowMapper`，避免隐式映射错误。
4. 单条查询要处理空结果，避免无数据场景抛出异常影响业务流程。
5. 新增、更新、删除要检查影响行数，并输出关键业务日志。
6. 复杂 SQL 建议集中管理，避免散落在多个 Service 方法中。

## 数据映射

数据映射用于将数据库查询结果转换为 Java 对象。`JdbcTemplate` 本身不做 ORM 映射，开发者需要通过 `RowMapper`、`BeanPropertyRowMapper` 或自定义映射逻辑完成字段转换。`RowMapper` 适合高频、明确、可控的映射；`BeanPropertyRowMapper` 适合字段命名规范且映射逻辑简单的场景；复杂结果集则建议使用自定义映射。

### RowMapper 使用

`RowMapper<T>` 是 JDBC Template 中最常用的行映射接口，它负责把 `ResultSet` 的当前行转换成一个 Java 对象。Spring 的 `query`、`queryForObject` 等方法都可以接收 `RowMapper` 参数，用于将每一行结果映射为目标对象。`NamedParameterJdbcTemplate` 也提供了类似的 `query` 和 `queryForObject` 方法，并支持配合 `RowMapper` 使用。([Home](https://docs.spring.io/spring-framework/docs/current/javadoc-api/org/springframework/jdbc/core/namedparam/NamedParameterJdbcTemplate.html?utm_source=chatgpt.com))

推荐将常用映射逻辑单独抽取为类，避免在每个 Repository 方法中重复编写字段映射代码。

文件位置：`src/main/java/io/github/atengk/jdbc/mapper/SysUserRowMapper.java`

下面代码定义用户表的通用 `RowMapper`，用于将 `sys_user` 查询结果映射为 `SysUser` 对象。

```java
package io.github.atengk.jdbc.mapper;

import io.github.atengk.jdbc.entity.SysUser;
import org.springframework.jdbc.core.RowMapper;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.LocalDateTime;

/**
 * 系统用户结果集行映射
 *
 * @author Ateng
 * @since 2026-05-04
 */
public class SysUserRowMapper implements RowMapper<SysUser> {

    /**
     * 映射当前行数据
     *
     * @param rs 当前结果集
     * @param rowNum 当前行号
     * @return 用户实体
     * @throws SQLException SQL异常
     */
    @Override
    public SysUser mapRow(ResultSet rs, int rowNum) throws SQLException {
        SysUser user = new SysUser();
        user.setId(rs.getLong("id"));
        user.setUsername(rs.getString("username"));
        user.setNickname(rs.getString("nickname"));
        user.setAge(rs.getObject("age", Integer.class));
        user.setStatus(rs.getObject("status", Integer.class));
        user.setCreateTime(toLocalDateTime(rs.getTimestamp("create_time")));
        user.setUpdateTime(toLocalDateTime(rs.getTimestamp("update_time")));
        return user;
    }

    /**
     * Timestamp 转 LocalDateTime
     *
     * @param timestamp 数据库时间
     * @return 本地时间
     */
    private LocalDateTime toLocalDateTime(Timestamp timestamp) {
        return timestamp == null ? null : timestamp.toLocalDateTime();
    }
}
```

文件位置：`src/main/java/io/github/atengk/jdbc/repository/SysUserMappingRepository.java`

下面代码演示如何在 Repository 中复用 `SysUserRowMapper` 完成单条查询和列表查询。

```java
package io.github.atengk.jdbc.repository;

import cn.hutool.core.text.CharSequenceUtil;
import io.github.atengk.jdbc.entity.SysUser;
import io.github.atengk.jdbc.mapper.SysUserRowMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * 系统用户映射 Repository
 *
 * @author Ateng
 * @since 2026-05-04
 */
@Slf4j
@Repository
@RequiredArgsConstructor
public class SysUserMappingRepository {

    private final JdbcTemplate jdbcTemplate;

    private static final SysUserRowMapper USER_ROW_MAPPER = new SysUserRowMapper();

    /**
     * 根据用户名查询用户
     *
     * @param username 用户名
     * @return 用户信息，不存在时返回 null
     */
    public SysUser findByUsername(String username) {
        if (CharSequenceUtil.isBlank(username)) {
            log.info("用户名为空，跳过查询");
            return null;
        }

        String sql = """
                SELECT id, username, nickname, age, status, create_time, update_time
                FROM sys_user
                WHERE username = ?
                """;

        try {
            return jdbcTemplate.queryForObject(sql, USER_ROW_MAPPER, username);
        } catch (EmptyResultDataAccessException e) {
            log.info("根据用户名未查询到用户，username={}", username);
            return null;
        }
    }

    /**
     * 根据状态查询用户列表
     *
     * @param status 用户状态
     * @return 用户列表
     */
    public List<SysUser> findByStatus(Integer status) {
        String sql = """
                SELECT id, username, nickname, age, status, create_time, update_time
                FROM sys_user
                WHERE status = ?
                ORDER BY id DESC
                """;
        return jdbcTemplate.query(sql, USER_ROW_MAPPER, status);
    }
}
```

`RowMapper` 的优点是字段映射清晰、性能稳定、调试方便。它适合生产项目中的核心查询，尤其适合字段类型需要显式处理、数据库字段和 Java 字段不完全一致、查询字段来自多表关联的场景。

### BeanPropertyRowMapper 使用

`BeanPropertyRowMapper` 是 Spring 提供的便捷映射实现，可以根据列名和 Java Bean 属性名自动匹配字段。它支持将下划线风格的数据库列名映射到驼峰风格的 Java 属性，例如 `create_time` 可以映射到 `createTime`。不过官方文档也明确说明，它更偏向便利性而不是高性能；高频核心查询建议使用自定义 `RowMapper`。([Home](https://docs.spring.io/spring-framework/docs/current/javadoc-api/org/springframework/jdbc/core/BeanPropertyRowMapper.html?utm_source=chatgpt.com))

使用 `BeanPropertyRowMapper` 时，目标类需要有无参构造方法，并且字段需要有对应的 setter 方法。前文的 `SysUser` 使用 Lombok `@Data`，满足这个要求。

文件位置：`src/main/java/io/github/atengk/jdbc/repository/SysUserBeanMapperRepository.java`

下面代码演示使用 `BeanPropertyRowMapper` 查询单条数据和分页列表。

```java
package io.github.atengk.jdbc.repository;

import io.github.atengk.jdbc.entity.SysUser;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.jdbc.core.BeanPropertyRowMapper;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * 系统用户 Bean 映射 Repository
 *
 * @author Ateng
 * @since 2026-05-04
 */
@Slf4j
@Repository
@RequiredArgsConstructor
public class SysUserBeanMapperRepository {

    private final JdbcTemplate jdbcTemplate;

    private static final BeanPropertyRowMapper<SysUser> USER_BEAN_ROW_MAPPER =
            BeanPropertyRowMapper.newInstance(SysUser.class);

    /**
     * 根据ID查询用户
     *
     * @param id 用户ID
     * @return 用户信息，不存在时返回 null
     */
    public SysUser findById(Long id) {
        String sql = """
                SELECT id, username, nickname, age, status, create_time, update_time
                FROM sys_user
                WHERE id = ?
                """;

        try {
            return jdbcTemplate.queryForObject(sql, USER_BEAN_ROW_MAPPER, id);
        } catch (EmptyResultDataAccessException e) {
            log.info("根据ID未查询到用户，id={}", id);
            return null;
        }
    }

    /**
     * 分页查询用户列表
     *
     * @param offset 偏移量
     * @param size 每页数量
     * @return 用户列表
     */
    public List<SysUser> findPage(int offset, int size) {
        String sql = """
                SELECT id, username, nickname, age, status, create_time, update_time
                FROM sys_user
                ORDER BY id DESC
                LIMIT ?, ?
                """;
        return jdbcTemplate.query(sql, USER_BEAN_ROW_MAPPER, offset, size);
    }
}
```

如果查询字段名和 Java 属性名无法自动匹配，可以在 SQL 中使用别名。比如数据库字段是 `user_name`，Java 字段是 `username`，可以写成：

```sql
SELECT user_name AS username
FROM sys_user
```

`BeanPropertyRowMapper` 适合开发效率优先的场景，例如后台管理、低频查询、简单表查询。对于复杂查询、性能敏感查询、字段类型需要特殊处理的查询，仍然建议使用显式 `RowMapper`。

### 自定义结果集映射

自定义结果集映射适合处理非实体类结果，例如统计对象、VO 对象、枚举展示字段、时间格式化字段、多表关联字段等。此时不建议强行复用 Entity，而应定义专门的 VO 或 DTO。

文件位置：`src/main/java/io/github/atengk/jdbc/vo/SysUserView.java`

下面 VO 用于接口展示，比实体类多了 `statusName` 字段。

```java
package io.github.atengk.jdbc.vo;

import lombok.Data;

import java.time.LocalDateTime;

/**
 * 系统用户展示对象
 *
 * @author Ateng
 * @since 2026-05-04
 */
@Data
public class SysUserView {

    /**
     * 主键ID
     */
    private Long id;

    /**
     * 用户名
     */
    private String username;

    /**
     * 昵称
     */
    private String nickname;

    /**
     * 年龄
     */
    private Integer age;

    /**
     * 状态
     */
    private Integer status;

    /**
     * 状态名称
     */
    private String statusName;

    /**
     * 创建时间
     */
    private LocalDateTime createTime;
}
```

文件位置：`src/main/java/io/github/atengk/jdbc/repository/SysUserViewRepository.java`

下面代码在映射过程中完成状态名称转换，适合接口直接返回展示对象。

```java
package io.github.atengk.jdbc.repository;

import io.github.atengk.jdbc.vo.SysUserView;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.List;

/**
 * 系统用户展示 Repository
 *
 * @author Ateng
 * @since 2026-05-04
 */
@Slf4j
@Repository
@RequiredArgsConstructor
public class SysUserViewRepository {

    private final JdbcTemplate jdbcTemplate;

    /**
     * 查询用户展示列表
     *
     * @return 用户展示列表
     */
    public List<SysUserView> findUserViews() {
        String sql = """
                SELECT id, username, nickname, age, status, create_time
                FROM sys_user
                ORDER BY id DESC
                """;

        return jdbcTemplate.query(sql, (rs, rowNum) -> {
            SysUserView view = new SysUserView();
            view.setId(rs.getLong("id"));
            view.setUsername(rs.getString("username"));
            view.setNickname(rs.getString("nickname"));
            view.setAge(rs.getObject("age", Integer.class));
            view.setStatus(rs.getObject("status", Integer.class));
            view.setStatusName(convertStatusName(view.getStatus()));

            Timestamp createTime = rs.getTimestamp("create_time");
            view.setCreateTime(createTime == null ? null : createTime.toLocalDateTime());

            return view;
        });
    }

    /**
     * 转换状态名称
     *
     * @param status 状态
     * @return 状态名称
     */
    private String convertStatusName(Integer status) {
        if (status == null) {
            return "未知";
        }
        return switch (status) {
            case 0 -> "禁用";
            case 1 -> "启用";
            default -> "未知";
        };
    }
}
```

自定义映射的关键原则是：查询结果服务于哪个业务视图，就定义哪个对象承接，不要为了省事把所有查询结果都塞进 Entity。Entity 应保持和表结构接近，VO/DTO 应服务于接口、页面或业务流程。

## 参数处理

参数处理决定了 SQL 的安全性、可读性和可维护性。`JdbcTemplate` 支持传统的 `?` 占位符参数，适合参数数量少、顺序清晰的 SQL；`NamedParameterJdbcTemplate` 支持命名参数，适合参数较多、动态条件多、`IN` 查询、批量处理等场景。Spring Boot 会自动配置 `JdbcTemplate` 和 `NamedParameterJdbcTemplate`，可以直接注入使用；`NamedParameterJdbcTemplate` 默认复用底层的 `JdbcTemplate`。([Home](https://docs.spring.io/spring-boot/reference/data/sql.html?utm_source=chatgpt.com))

### 占位符参数

占位符参数使用 `?` 表示 SQL 参数，实际值按照方法参数顺序绑定。它的优点是简单直接，缺点是参数较多时可读性下降，且顺序写错不容易在编译期发现。

文件位置：`src/main/java/io/github/atengk/jdbc/repository/SysUserParamRepository.java`

下面代码演示常见的占位符参数写法，包括等值查询、模糊查询和动态 `IN` 查询。

```java
package io.github.atengk.jdbc.repository;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.text.CharSequenceUtil;
import io.github.atengk.jdbc.entity.SysUser;
import io.github.atengk.jdbc.mapper.SysUserRowMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * 系统用户参数 Repository
 *
 * @author Ateng
 * @since 2026-05-04
 */
@Slf4j
@Repository
@RequiredArgsConstructor
public class SysUserParamRepository {

    private final JdbcTemplate jdbcTemplate;

    private static final SysUserRowMapper USER_ROW_MAPPER = new SysUserRowMapper();

    /**
     * 根据用户名和状态查询用户
     *
     * @param username 用户名
     * @param status 状态
     * @return 用户列表
     */
    public List<SysUser> findByUsernameAndStatus(String username, Integer status) {
        String sql = """
                SELECT id, username, nickname, age, status, create_time, update_time
                FROM sys_user
                WHERE username = ? AND status = ?
                ORDER BY id DESC
                """;
        return jdbcTemplate.query(sql, USER_ROW_MAPPER, username, status);
    }

    /**
     * 根据关键字模糊查询用户
     *
     * @param keyword 用户名或昵称关键字
     * @return 用户列表
     */
    public List<SysUser> findByKeyword(String keyword) {
        String sql = """
                SELECT id, username, nickname, age, status, create_time, update_time
                FROM sys_user
                WHERE username LIKE ? OR nickname LIKE ?
                ORDER BY id DESC
                """;

        String safeKeyword = CharSequenceUtil.blankToDefault(keyword, "");
        String likeKeyword = "%" + safeKeyword + "%";
        return jdbcTemplate.query(sql, USER_ROW_MAPPER, likeKeyword, likeKeyword);
    }

    /**
     * 根据ID集合查询用户
     *
     * @param ids 用户ID集合
     * @return 用户列表
     */
    public List<SysUser> findByIds(List<Long> ids) {
        if (CollUtil.isEmpty(ids)) {
            log.info("用户ID集合为空，返回空列表");
            return List.of();
        }

        String placeholders = CollUtil.join(ids.stream().map(id -> "?").toList(), ",");
        String sql = """
                SELECT id, username, nickname, age, status, create_time, update_time
                FROM sys_user
                WHERE id IN (%s)
                ORDER BY id DESC
                """.formatted(placeholders);

        return jdbcTemplate.query(sql, USER_ROW_MAPPER, ids.toArray());
    }
}
```

占位符参数需要特别注意参数顺序。例如 `WHERE username = ? AND status = ?` 必须保证传参顺序是 `username, status`。当 SQL 参数超过 3 个，或者存在多个同类型参数时，建议改用命名参数降低维护风险。

### NamedParameterJdbcTemplate

`NamedParameterJdbcTemplate` 使用 `:参数名` 绑定参数，比 `?` 更适合复杂 SQL。它支持 `Map`、`MapSqlParameterSource`、`BeanPropertySqlParameterSource` 等参数来源。`SqlParameterSource` 是 Spring 为命名 SQL 参数提供的统一参数接口，参数值和类型都通过名称识别。([Home](https://docs.spring.io/spring-framework/docs/current/javadoc-api/org/springframework/jdbc/core/namedparam/SqlParameterSource.html?utm_source=chatgpt.com))

文件位置：`src/main/java/io/github/atengk/jdbc/repository/SysUserNamedParamRepository.java`

下面代码演示命名参数的常见用法，包括普通查询、`IN` 查询和新增数据。

```java
package io.github.atengk.jdbc.repository;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.map.MapUtil;
import io.github.atengk.jdbc.entity.SysUser;
import io.github.atengk.jdbc.mapper.SysUserRowMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.jdbc.core.namedparam.BeanPropertySqlParameterSource;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Map;

/**
 * 系统用户命名参数 Repository
 *
 * @author Ateng
 * @since 2026-05-04
 */
@Slf4j
@Repository
@RequiredArgsConstructor
public class SysUserNamedParamRepository {

    private final NamedParameterJdbcTemplate namedParameterJdbcTemplate;

    private static final SysUserRowMapper USER_ROW_MAPPER = new SysUserRowMapper();

    /**
     * 根据状态查询用户列表
     *
     * @param status 用户状态
     * @return 用户列表
     */
    public List<SysUser> findByStatus(Integer status) {
        String sql = """
                SELECT id, username, nickname, age, status, create_time, update_time
                FROM sys_user
                WHERE status = :status
                ORDER BY id DESC
                """;

        Map<String, Object> params = MapUtil.<String, Object>builder()
                .put("status", status)
                .build();

        return namedParameterJdbcTemplate.query(sql, params, USER_ROW_MAPPER);
    }

    /**
     * 根据ID集合查询用户列表
     *
     * @param ids 用户ID集合
     * @return 用户列表
     */
    public List<SysUser> findByIds(List<Long> ids) {
        if (CollUtil.isEmpty(ids)) {
            log.info("用户ID集合为空，返回空列表");
            return List.of();
        }

        String sql = """
                SELECT id, username, nickname, age, status, create_time, update_time
                FROM sys_user
                WHERE id IN (:ids)
                ORDER BY id DESC
                """;

        MapSqlParameterSource params = new MapSqlParameterSource()
                .addValue("ids", ids);

        return namedParameterJdbcTemplate.query(sql, params, USER_ROW_MAPPER);
    }

    /**
     * 新增用户
     *
     * @param user 用户信息
     * @return 影响行数
     */
    public int insert(SysUser user) {
        String sql = """
                INSERT INTO sys_user (username, nickname, age, status)
                VALUES (:username, :nickname, :age, :status)
                """;

        BeanPropertySqlParameterSource params = new BeanPropertySqlParameterSource(user);
        int rows = namedParameterJdbcTemplate.update(sql, params);
        log.info("命名参数新增用户完成，username={}，影响行数={}", user.getUsername(), rows);
        return rows;
    }
}
```

命名参数的优势主要体现在可读性和可维护性。比如下面这段 SQL 中，即使参数顺序调整，也不会影响绑定结果：

```sql
SELECT id, username, nickname, age, status, create_time, update_time
FROM sys_user
WHERE status = :status
  AND username = :username
```

对比 `?` 占位符，命名参数更适合下列场景：

| 场景                 | 推荐方式                                 |
| -------------------- | ---------------------------------------- |
| 参数少且顺序清晰     | `JdbcTemplate` + `?`                     |
| 参数多且字段相似     | `NamedParameterJdbcTemplate`             |
| `IN` 查询            | `NamedParameterJdbcTemplate`             |
| 通过对象属性绑定参数 | `BeanPropertySqlParameterSource`         |
| 批量插入对象列表     | `NamedParameterJdbcTemplate.batchUpdate` |

### 批量参数处理

批量参数处理用于批量新增、批量更新或批量删除。Spring Framework 文档说明，`JdbcTemplate` 和 `NamedParameterJdbcTemplate` 都支持批量更新；命名参数批处理可以通过 `SqlParameterSourceUtils.createBatch` 将 JavaBean 或 `Map` 数组转换为批量参数。([Home](https://docs.spring.io/spring-framework/reference/data-access/jdbc/advanced.html?utm_source=chatgpt.com))

文件位置：`src/main/java/io/github/atengk/jdbc/repository/SysUserBatchParamRepository.java`

下面代码同时演示 `JdbcTemplate.batchUpdate` 和 `NamedParameterJdbcTemplate.batchUpdate` 两种批量参数处理方式。

```java
package io.github.atengk.jdbc.repository;

import cn.hutool.core.collection.CollUtil;
import io.github.atengk.jdbc.entity.SysUser;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.jdbc.core.BatchPreparedStatementSetter;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.jdbc.core.namedparam.SqlParameterSourceUtils;
import org.springframework.stereotype.Repository;

import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.List;

/**
 * 系统用户批量参数 Repository
 *
 * @author Ateng
 * @since 2026-05-04
 */
@Slf4j
@Repository
@RequiredArgsConstructor
public class SysUserBatchParamRepository {

    private final JdbcTemplate jdbcTemplate;

    private final NamedParameterJdbcTemplate namedParameterJdbcTemplate;

    /**
     * 使用 JdbcTemplate 批量新增用户
     *
     * @param users 用户列表
     * @return 每条SQL影响行数
     */
    public int[] batchInsertByJdbcTemplate(List<SysUser> users) {
        if (CollUtil.isEmpty(users)) {
            log.info("批量新增用户列表为空，跳过处理");
            return new int[0];
        }

        String sql = """
                INSERT INTO sys_user (username, nickname, age, status)
                VALUES (?, ?, ?, ?)
                """;

        int[] rows = jdbcTemplate.batchUpdate(sql, new BatchPreparedStatementSetter() {

            /**
             * 设置批量参数
             *
             * @param ps 预编译SQL对象
             * @param i 当前索引
             * @throws SQLException SQL异常
             */
            @Override
            public void setValues(PreparedStatement ps, int i) throws SQLException {
                SysUser user = users.get(i);
                ps.setString(1, user.getUsername());
                ps.setString(2, user.getNickname());

                if (user.getAge() == null) {
                    ps.setObject(3, null);
                } else {
                    ps.setInt(3, user.getAge());
                }

                ps.setInt(4, user.getStatus());
            }

            /**
             * 获取批量数量
             *
             * @return 批量数量
             */
            @Override
            public int getBatchSize() {
                return users.size();
            }
        });

        log.info("JdbcTemplate 批量新增用户完成，数量={}", rows.length);
        return rows;
    }

    /**
     * 使用 NamedParameterJdbcTemplate 批量新增用户
     *
     * @param users 用户列表
     * @return 每条SQL影响行数
     */
    public int[] batchInsertByNamedParameter(List<SysUser> users) {
        if (CollUtil.isEmpty(users)) {
            log.info("命名参数批量新增用户列表为空，跳过处理");
            return new int[0];
        }

        String sql = """
                INSERT INTO sys_user (username, nickname, age, status)
                VALUES (:username, :nickname, :age, :status)
                """;

        int[] rows = namedParameterJdbcTemplate.batchUpdate(
                sql,
                SqlParameterSourceUtils.createBatch(users)
        );

        log.info("NamedParameterJdbcTemplate 批量新增用户完成，数量={}", rows.length);
        return rows;
    }

    /**
     * 批量更新用户状态
     *
     * @param users 用户列表
     * @return 每条SQL影响行数
     */
    public int[] batchUpdateStatus(List<SysUser> users) {
        if (CollUtil.isEmpty(users)) {
            log.info("批量更新用户状态列表为空，跳过处理");
            return new int[0];
        }

        String sql = """
                UPDATE sys_user
                SET status = :status
                WHERE id = :id
                """;

        int[] rows = namedParameterJdbcTemplate.batchUpdate(
                sql,
                SqlParameterSourceUtils.createBatch(users)
        );

        log.info("批量更新用户状态完成，数量={}", rows.length);
        return rows;
    }
}
```

批量参数处理建议遵循以下规则：

1. 批量数据为空时直接返回，避免执行无意义 SQL。
2. 批量新增优先使用 `batchUpdate`，不要在循环中反复调用单条 `update`。
3. 数据量很大时不要一次性提交全部数据，应按 500、1000 或业务可接受的批次拆分。
4. 批量操作建议放在事务中，避免部分成功、部分失败导致数据状态不一致。
5. `NamedParameterJdbcTemplate` 更适合对象批量入库，字段多时可读性明显优于 `?` 占位符。

这一节完成后，后续“事务管理”可以直接基于这些批量方法继续扩展 `@Transactional`、`TransactionTemplate` 和异常回滚规则。



## 事务管理

事务管理用于保证多个数据库操作要么全部成功，要么全部失败。Spring 推荐多数业务场景优先使用声明式事务，因为它对业务代码侵入较低；编程式事务适合事务边界需要在代码中精细控制的场景。Spring 声明式事务基于 AOP 实现，默认情况下运行时异常会触发事务回滚，受检异常不会自动回滚，需要通过 `rollbackFor` 等规则显式指定。([Home](https://docs.spring.io/spring-framework/reference/data-access/transaction/declarative.html?utm_source=chatgpt.com))

### 声明式事务配置

声明式事务通常使用 `@Transactional` 注解完成，建议放在 Service 层，而不是 Repository 层。Repository 只负责执行 SQL，Service 负责组织业务流程和事务边界。Spring 的声明式事务支持可以将事务行为配置到方法级别，适合大多数增删改业务。([Home](https://docs.spring.io/spring-framework/reference/data-access/transaction/declarative.html?utm_source=chatgpt.com))

文件结构如下：

```text
src/main/java/io/github/atengk/jdbc/repository/SysUserTxRepository.java
src/main/java/io/github/atengk/jdbc/service/SysUserTxService.java
src/main/java/io/github/atengk/jdbc/service/impl/SysUserTxServiceImpl.java
```

文件位置：`src/main/java/io/github/atengk/jdbc/repository/SysUserTxRepository.java`

下面 Repository 提供事务示例需要的基础 SQL 操作，事务不放在这里控制。

```java
package io.github.atengk.jdbc.repository;

import io.github.atengk.jdbc.entity.SysUser;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

/**
 * 系统用户事务 Repository
 *
 * @author Ateng
 * @since 2026-05-04
 */
@Slf4j
@Repository
@RequiredArgsConstructor
public class SysUserTxRepository {

    private final JdbcTemplate jdbcTemplate;

    /**
     * 新增用户
     *
     * @param user 用户信息
     * @return 影响行数
     */
    public int insert(SysUser user) {
        String sql = """
                INSERT INTO sys_user (username, nickname, age, status)
                VALUES (?, ?, ?, ?)
                """;
        return jdbcTemplate.update(
                sql,
                user.getUsername(),
                user.getNickname(),
                user.getAge(),
                user.getStatus()
        );
    }

    /**
     * 根据ID更新用户状态
     *
     * @param id 用户ID
     * @param status 用户状态
     * @return 影响行数
     */
    public int updateStatusById(Long id, Integer status) {
        String sql = """
                UPDATE sys_user
                SET status = ?
                WHERE id = ?
                """;
        return jdbcTemplate.update(sql, status, id);
    }

    /**
     * 根据ID删除用户
     *
     * @param id 用户ID
     * @return 影响行数
     */
    public int deleteById(Long id) {
        String sql = "DELETE FROM sys_user WHERE id = ?";
        return jdbcTemplate.update(sql, id);
    }
}
```

文件位置：`src/main/java/io/github/atengk/jdbc/service/SysUserTxService.java`

下面接口定义事务业务方法，具体事务注解放在实现类上，便于统一控制事务语义。

```java
package io.github.atengk.jdbc.service;

import io.github.atengk.jdbc.entity.SysUser;

/**
 * 系统用户事务 Service
 *
 * @author Ateng
 * @since 2026-05-04
 */
public interface SysUserTxService {

    /**
     * 创建用户并启用
     *
     * @param user 用户信息
     */
    void createAndEnable(SysUser user);

    /**
     * 创建用户后模拟异常
     *
     * @param user 用户信息
     */
    void createThenThrowException(SysUser user);

    /**
     * 删除用户并记录业务日志
     *
     * @param id 用户ID
     */
    void deleteUser(Long id);
}
```

文件位置：`src/main/java/io/github/atengk/jdbc/service/impl/SysUserTxServiceImpl.java`

下面 Service 使用 `@Transactional` 控制事务边界。方法执行过程中只要抛出符合回滚规则的异常，前面已经执行的数据库修改会回滚。

```java
package io.github.atengk.jdbc.service.impl;

import cn.hutool.core.lang.Assert;
import cn.hutool.core.text.CharSequenceUtil;
import io.github.atengk.jdbc.entity.SysUser;
import io.github.atengk.jdbc.repository.SysUserTxRepository;
import io.github.atengk.jdbc.service.SysUserTxService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 系统用户事务 Service 实现
 *
 * @author Ateng
 * @since 2026-05-04
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class SysUserTxServiceImpl implements SysUserTxService {

    private final SysUserTxRepository sysUserTxRepository;

    /**
     * 创建用户并启用
     *
     * @param user 用户信息
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void createAndEnable(SysUser user) {
        checkUser(user);

        int insertRows = sysUserTxRepository.insert(user);
        log.info("新增用户完成，username={}，影响行数={}", user.getUsername(), insertRows);

        /*
         * 示例中假设新增后需要启用某个已知用户。
         * 实际项目中，如果需要拿到自增ID，后续可使用 KeyHolder 获取主键。
         */
        log.info("用户创建事务执行完成，username={}", user.getUsername());
    }

    /**
     * 创建用户后模拟异常
     *
     * @param user 用户信息
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void createThenThrowException(SysUser user) {
        checkUser(user);

        int rows = sysUserTxRepository.insert(user);
        log.info("新增用户完成，username={}，影响行数={}", user.getUsername(), rows);

        throw new IllegalStateException("模拟业务异常，验证事务回滚");
    }

    /**
     * 删除用户并记录业务日志
     *
     * @param id 用户ID
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteUser(Long id) {
        Assert.notNull(id, "用户ID不能为空");

        int rows = sysUserTxRepository.deleteById(id);
        if (rows == 0) {
            log.info("删除用户失败，记录不存在，id={}", id);
            return;
        }

        log.info("删除用户成功，id={}，影响行数={}", id, rows);
    }

    /**
     * 校验用户信息
     *
     * @param user 用户信息
     */
    private void checkUser(SysUser user) {
        Assert.notNull(user, "用户信息不能为空");
        Assert.isTrue(CharSequenceUtil.isNotBlank(user.getUsername()), "用户名不能为空");
        Assert.isTrue(CharSequenceUtil.isNotBlank(user.getNickname()), "昵称不能为空");
        Assert.notNull(user.getStatus(), "用户状态不能为空");
    }
}
```

声明式事务使用时需要注意：

| 注意项                        | 说明                                             |
| ----------------------------- | ------------------------------------------------ |
| 事务建议放在 Service 层       | Service 负责业务流程，Repository 只负责 SQL      |
| 方法需要通过 Spring Bean 调用 | 同类内部方法调用通常不会经过代理，事务可能不生效 |
| 默认回滚运行时异常            | 受检异常需要配置 `rollbackFor`                   |
| 查询方法可配置只读事务        | 例如 `@Transactional(readOnly = true)`           |
| 不建议事务中执行远程调用      | 避免事务长时间占用数据库连接                     |

### 编程式事务处理

编程式事务适合事务边界需要在代码中动态控制的场景，例如部分失败只回滚局部逻辑、根据执行结果手动标记回滚、在同一个方法中使用不同事务配置等。Spring 官方文档推荐命令式流程中使用 `TransactionTemplate`，它通过回调方式执行事务代码，并减少事务资源获取与释放的样板代码。([Home](https://docs.spring.io/spring-framework/reference/data-access/transaction/programmatic.html?utm_source=chatgpt.com))

文件位置：`src/main/java/io/github/atengk/jdbc/service/impl/SysUserProgrammaticTxService.java`

下面代码使用 `TransactionTemplate` 显式控制事务。出现业务异常时，可以通过 `status.setRollbackOnly()` 手动标记当前事务回滚。([Home](https://docs.spring.io/spring-framework/reference/data-access/transaction/programmatic.html?utm_source=chatgpt.com))

```java
package io.github.atengk.jdbc.service.impl;

import cn.hutool.core.lang.Assert;
import io.github.atengk.jdbc.entity.SysUser;
import io.github.atengk.jdbc.repository.SysUserTxRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionTemplate;

/**
 * 系统用户编程式事务 Service
 *
 * @author Ateng
 * @since 2026-05-04
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class SysUserProgrammaticTxService {

    private final TransactionTemplate transactionTemplate;

    private final SysUserTxRepository sysUserTxRepository;

    /**
     * 使用 TransactionTemplate 创建用户
     *
     * @param user 用户信息
     * @return true 表示成功，false 表示失败
     */
    public Boolean createUserWithTransaction(SysUser user) {
        Assert.notNull(user, "用户信息不能为空");

        return transactionTemplate.execute(status -> {
            try {
                int rows = sysUserTxRepository.insert(user);
                log.info("编程式事务新增用户完成，username={}，影响行数={}", user.getUsername(), rows);

                if (rows != 1) {
                    log.info("新增用户影响行数异常，标记事务回滚，username={}", user.getUsername());
                    status.setRollbackOnly();
                    return false;
                }

                return true;
            } catch (Exception e) {
                log.error("编程式事务新增用户异常，标记事务回滚，username={}", user.getUsername(), e);
                status.setRollbackOnly();
                return false;
            }
        });
    }

    /**
     * 使用 TransactionTemplate 删除用户
     *
     * @param id 用户ID
     * @return true 表示成功，false 表示失败
     */
    public Boolean deleteUserWithTransaction(Long id) {
        Assert.notNull(id, "用户ID不能为空");

        return transactionTemplate.execute(status -> {
            int rows = sysUserTxRepository.deleteById(id);
            if (rows == 0) {
                log.info("编程式事务删除用户失败，记录不存在，id={}", id);
                status.setRollbackOnly();
                return false;
            }

            log.info("编程式事务删除用户成功，id={}，影响行数={}", id, rows);
            return true;
        });
    }
}
```

声明式事务和编程式事务的选择建议如下：

| 场景                                 | 推荐方式              |
| ------------------------------------ | --------------------- |
| 常规新增、更新、删除                 | `@Transactional`      |
| 多个 Repository 操作组成一个业务流程 | `@Transactional`      |
| 需要动态决定是否回滚                 | `TransactionTemplate` |
| 一个方法中需要多个不同事务边界       | `TransactionTemplate` |
| 希望业务代码少依赖 Spring 事务 API   | `@Transactional`      |

### 事务回滚规则

事务回滚规则决定哪些异常会导致事务回滚。Spring 声明式事务默认只对未检查异常，即 `RuntimeException` 及其子类，以及 `Error` 回滚；对于受检异常，需要显式配置 `rollbackFor`。在业务代码中，推荐统一使用 `@Transactional(rollbackFor = Exception.class)`，除非你明确希望某些受检异常不回滚。([Home](https://docs.spring.io/spring-framework/reference/data-access/transaction/declarative.html?utm_source=chatgpt.com))

文件位置：`src/main/java/io/github/atengk/jdbc/exception/UserImportException.java`

下面自定义异常用于演示受检异常回滚。

```java
package io.github.atengk.jdbc.exception;

/**
 * 用户导入异常
 *
 * @author Ateng
 * @since 2026-05-04
 */
public class UserImportException extends Exception {

    /**
     * 构造用户导入异常
     *
     * @param message 异常信息
     */
    public UserImportException(String message) {
        super(message);
    }
}
```

文件位置：`src/main/java/io/github/atengk/jdbc/service/impl/SysUserRollbackRuleService.java`

下面代码演示受检异常和运行时异常的回滚规则差异。实际项目中建议明确写 `rollbackFor = Exception.class`，降低维护风险。

```java
package io.github.atengk.jdbc.service.impl;

import io.github.atengk.jdbc.entity.SysUser;
import io.github.atengk.jdbc.exception.UserImportException;
import io.github.atengk.jdbc.repository.SysUserTxRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 系统用户回滚规则 Service
 *
 * @author Ateng
 * @since 2026-05-04
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class SysUserRollbackRuleService {

    private final SysUserTxRepository sysUserTxRepository;

    /**
     * 运行时异常默认回滚
     *
     * @param user 用户信息
     */
    @Transactional
    public void rollbackByRuntimeException(SysUser user) {
        sysUserTxRepository.insert(user);
        log.info("新增用户后抛出运行时异常，username={}", user.getUsername());

        throw new IllegalStateException("运行时异常默认触发回滚");
    }

    /**
     * 受检异常需要显式配置 rollbackFor
     *
     * @param user 用户信息
     * @throws UserImportException 用户导入异常
     */
    @Transactional(rollbackFor = UserImportException.class)
    public void rollbackByCheckedException(SysUser user) throws UserImportException {
        sysUserTxRepository.insert(user);
        log.info("新增用户后抛出受检异常，username={}", user.getUsername());

        throw new UserImportException("受检异常通过 rollbackFor 触发回滚");
    }

    /**
     * 指定异常不回滚
     *
     * @param user 用户信息
     */
    @Transactional(noRollbackFor = IllegalArgumentException.class)
    public void noRollbackByBusinessException(SysUser user) {
        sysUserTxRepository.insert(user);
        log.info("新增用户后抛出指定不回滚异常，username={}", user.getUsername());

        throw new IllegalArgumentException("该异常不会触发事务回滚");
    }
}
```

事务回滚建议如下：

1. 增删改业务方法建议统一使用 `@Transactional(rollbackFor = Exception.class)`。
2. 不要在事务方法中吞掉异常；如果捕获异常后不继续抛出，事务通常不会自动回滚。
3. 如果确实要捕获异常并回滚，声明式事务中可继续抛出异常，编程式事务中可调用 `status.setRollbackOnly()`。
4. 事务方法避免使用 `private` 修饰，也避免依赖同类内部方法调用触发事务。
5. 大事务中不要包含耗时远程接口、文件上传下载、大量计算等操作。

## 批量操作

批量操作用于减少应用和数据库之间的往返次数，适合批量新增、批量更新、批量删除等场景。Spring JDBC 文档说明，多数 JDBC 驱动在对同一个预编译语句执行多次更新时可以通过批处理提升性能，`batchUpdate` 会返回每条批处理语句的影响行数数组。([Home](https://docs.spring.io/spring-framework/reference/data-access/jdbc/advanced.html?utm_source=chatgpt.com))

### 批量新增

批量新增建议优先使用 `NamedParameterJdbcTemplate.batchUpdate`。当实体字段较多时，命名参数比 `?` 占位符更容易维护。Spring JDBC 支持使用 `SqlParameterSourceUtils.createBatch` 将 JavaBean、`Map` 等对象转换为批量参数。([Home](https://docs.spring.io/spring-framework/reference/data-access/jdbc/advanced.html?utm_source=chatgpt.com))

文件位置：`src/main/java/io/github/atengk/jdbc/repository/SysUserBatchRepository.java`

下面 Repository 集中演示批量新增、批量更新和批量删除。

```java
package io.github.atengk.jdbc.repository;

import cn.hutool.core.collection.CollUtil;
import io.github.atengk.jdbc.entity.SysUser;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.ParameterizedPreparedStatementSetter;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.jdbc.core.namedparam.SqlParameterSourceUtils;
import org.springframework.stereotype.Repository;

import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.List;

/**
 * 系统用户批量操作 Repository
 *
 * @author Ateng
 * @since 2026-05-04
 */
@Slf4j
@Repository
@RequiredArgsConstructor
public class SysUserBatchRepository {

    private final JdbcTemplate jdbcTemplate;

    private final NamedParameterJdbcTemplate namedParameterJdbcTemplate;

    /**
     * 批量新增用户
     *
     * @param users 用户列表
     * @return 每条SQL影响行数
     */
    public int[] batchInsert(List<SysUser> users) {
        if (CollUtil.isEmpty(users)) {
            log.info("批量新增用户列表为空，跳过处理");
            return new int[0];
        }

        String sql = """
                INSERT INTO sys_user (username, nickname, age, status)
                VALUES (:username, :nickname, :age, :status)
                """;

        int[] rows = namedParameterJdbcTemplate.batchUpdate(
                sql,
                SqlParameterSourceUtils.createBatch(users)
        );

        log.info("批量新增用户完成，提交数量={}，返回结果数量={}", users.size(), rows.length);
        return rows;
    }

    /**
     * 批量更新用户状态
     *
     * @param users 用户列表
     * @return 每条SQL影响行数
     */
    public int[] batchUpdateStatus(List<SysUser> users) {
        if (CollUtil.isEmpty(users)) {
            log.info("批量更新用户状态列表为空，跳过处理");
            return new int[0];
        }

        String sql = """
                UPDATE sys_user
                SET status = :status
                WHERE id = :id
                """;

        int[] rows = namedParameterJdbcTemplate.batchUpdate(
                sql,
                SqlParameterSourceUtils.createBatch(users)
        );

        log.info("批量更新用户状态完成，提交数量={}，返回结果数量={}", users.size(), rows.length);
        return rows;
    }

    /**
     * 批量删除用户
     *
     * @param ids 用户ID列表
     * @param batchSize 每批数量
     * @return 每批每条SQL影响行数
     */
    public int[][] batchDeleteByIds(List<Long> ids, int batchSize) {
        if (CollUtil.isEmpty(ids)) {
            log.info("批量删除用户ID列表为空，跳过处理");
            return new int[0][0];
        }

        String sql = "DELETE FROM sys_user WHERE id = ?";
        int actualBatchSize = Math.max(batchSize, 1);

        int[][] rows = jdbcTemplate.batchUpdate(sql, ids, actualBatchSize, new ParameterizedPreparedStatementSetter<Long>() {

            /**
             * 设置批量删除参数
             *
             * @param ps 预编译SQL对象
             * @param id 用户ID
             * @throws SQLException SQL异常
             */
            @Override
            public void setValues(PreparedStatement ps, Long id) throws SQLException {
                ps.setLong(1, id);
            }
        });

        log.info("批量删除用户完成，提交数量={}，批次大小={}", ids.size(), actualBatchSize);
        return rows;
    }
}
```

批量新增调用示例：

```java
List<SysUser> users = List.of(
        buildUser("batch_001", "批量用户001", 20),
        buildUser("batch_002", "批量用户002", 21),
        buildUser("batch_003", "批量用户003", 22)
);

int[] rows = sysUserBatchRepository.batchInsert(users);
log.info("批量新增返回结果数量={}", rows.length);
```

辅助构造方法示例：

```java
/**
 * 构造用户对象
 *
 * @param username 用户名
 * @param nickname 昵称
 * @param age 年龄
 * @return 用户对象
 */
private SysUser buildUser(String username, String nickname, Integer age) {
    SysUser user = new SysUser();
    user.setUsername(username);
    user.setNickname(nickname);
    user.setAge(age);
    user.setStatus(1);
    return user;
}
```

### 批量更新

批量更新适合统一调整状态、批量修改字段、批量同步外部系统数据等场景。批量更新需要注意每条记录的主键或唯一键必须明确，否则容易造成误更新。

文件位置：`src/main/java/io/github/atengk/jdbc/service/impl/SysUserBatchService.java`

下面 Service 使用事务包裹批量新增、批量更新和批量删除，避免批量操作部分成功、部分失败导致数据不一致。

```java
package io.github.atengk.jdbc.service.impl;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.lang.Assert;
import io.github.atengk.jdbc.entity.SysUser;
import io.github.atengk.jdbc.repository.SysUserBatchRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * 系统用户批量操作 Service
 *
 * @author Ateng
 * @since 2026-05-04
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class SysUserBatchService {

    private final SysUserBatchRepository sysUserBatchRepository;

    /**
     * 批量导入用户
     *
     * @param users 用户列表
     * @return 导入数量
     */
    @Transactional(rollbackFor = Exception.class)
    public int importUsers(List<SysUser> users) {
        if (CollUtil.isEmpty(users)) {
            log.info("导入用户列表为空，直接返回");
            return 0;
        }

        int[] rows = sysUserBatchRepository.batchInsert(users);
        int successCount = countSuccess(rows);

        Assert.isTrue(successCount == users.size(), "批量导入用户数量不一致");
        log.info("批量导入用户成功，数量={}", successCount);
        return successCount;
    }

    /**
     * 批量启用用户
     *
     * @param ids 用户ID列表
     * @return 更新数量
     */
    @Transactional(rollbackFor = Exception.class)
    public int enableUsers(List<Long> ids) {
        if (CollUtil.isEmpty(ids)) {
            log.info("批量启用用户ID列表为空，直接返回");
            return 0;
        }

        List<SysUser> users = ids.stream()
                .map(id -> {
                    SysUser user = new SysUser();
                    user.setId(id);
                    user.setStatus(1);
                    return user;
                })
                .toList();

        int[] rows = sysUserBatchRepository.batchUpdateStatus(users);
        int successCount = countSuccess(rows);

        log.info("批量启用用户完成，提交数量={}，成功数量={}", ids.size(), successCount);
        return successCount;
    }

    /**
     * 批量禁用用户
     *
     * @param ids 用户ID列表
     * @return 更新数量
     */
    @Transactional(rollbackFor = Exception.class)
    public int disableUsers(List<Long> ids) {
        if (CollUtil.isEmpty(ids)) {
            log.info("批量禁用用户ID列表为空，直接返回");
            return 0;
        }

        List<SysUser> users = ids.stream()
                .map(id -> {
                    SysUser user = new SysUser();
                    user.setId(id);
                    user.setStatus(0);
                    return user;
                })
                .toList();

        int[] rows = sysUserBatchRepository.batchUpdateStatus(users);
        int successCount = countSuccess(rows);

        log.info("批量禁用用户完成，提交数量={}，成功数量={}", ids.size(), successCount);
        return successCount;
    }

    /**
     * 统计成功数量
     *
     * @param rows 批处理返回结果
     * @return 成功数量
     */
    private int countSuccess(int[] rows) {
        int count = 0;
        for (int row : rows) {
            /*
             * row >= 0 表示驱动返回了明确影响行数。
             * row == -2 表示 Statement.SUCCESS_NO_INFO，代表执行成功但影响行数未知。
             */
            if (row > 0 || row == -2) {
                count++;
            }
        }
        return count;
    }
}
```

`batchUpdate` 返回的 `int[]` 中，每个元素对应一条批处理语句的执行结果；如果 JDBC 驱动无法返回明确影响行数，可能返回 `-2`，表示执行成功但影响行数未知。([Home](https://docs.spring.io/spring-framework/reference/data-access/jdbc/advanced.html?utm_source=chatgpt.com))

### 批量删除

批量删除分为两类：一类是 `DELETE FROM table WHERE id IN (...)`，适合 ID 数量较少的场景；另一类是 `batchUpdate` 多次执行 `DELETE FROM table WHERE id = ?`，适合控制批次大小、降低单条 SQL 参数数量的场景。大多数业务系统中，删除建议优先做逻辑删除；确实需要物理删除时，应放在事务中，并记录关键操作日志。

Service 中调用批量删除：

```java
/**
 * 批量删除用户
 *
 * @param ids 用户ID列表
 * @return 删除数量
 */
@Transactional(rollbackFor = Exception.class)
public int deleteUsers(List<Long> ids) {
    if (CollUtil.isEmpty(ids)) {
        log.info("批量删除用户ID列表为空，直接返回");
        return 0;
    }

    int batchSize = 500;
    int[][] rows = sysUserBatchRepository.batchDeleteByIds(ids, batchSize);

    int successCount = 0;
    for (int[] batchRows : rows) {
        successCount += countSuccess(batchRows);
    }

    log.info("批量删除用户完成，提交数量={}，成功数量={}", ids.size(), successCount);
    return successCount;
}
```

如果采用逻辑删除，可以将表结构增加 `deleted` 字段：

```sql
-- 逻辑删除字段：0未删除，1已删除
ALTER TABLE sys_user
ADD COLUMN deleted TINYINT NOT NULL DEFAULT 0 COMMENT '删除状态：0未删除，1已删除';
```

逻辑删除的批量更新 SQL 如下：

```java
/**
 * 批量逻辑删除用户
 *
 * @param ids 用户ID列表
 * @return 每条SQL影响行数
 */
public int[][] batchLogicDeleteByIds(List<Long> ids) {
    if (CollUtil.isEmpty(ids)) {
        log.info("批量逻辑删除用户ID列表为空，跳过处理");
        return new int[0][0];
    }

    String sql = """
            UPDATE sys_user
            SET deleted = 1
            WHERE id = ?
            """;

    return jdbcTemplate.batchUpdate(sql, ids, 500, (ps, id) -> ps.setLong(1, id));
}
```

批量操作建议如下：

| 建议                   | 说明                                       |
| ---------------------- | ------------------------------------------ |
| 控制批次大小           | 常用批次大小可从 500 或 1000 开始压测      |
| 避免循环单条提交       | 不要在循环中反复调用单条 `update`          |
| 使用事务包裹批量写操作 | 避免部分成功导致数据不一致                 |
| 注意唯一键冲突         | 批量新增前确认唯一约束和冲突处理策略       |
| 大批量删除优先逻辑删除 | 物理删除可能造成锁等待、主从延迟和审计困难 |
| 记录关键日志           | 记录提交数量、成功数量、业务批次号等信息   |



## 异常处理

异常处理用于统一管理数据库访问过程中出现的问题，例如 SQL 语法错误、唯一键冲突、数据不存在、连接失败、查询超时等。Spring JDBC 会将底层 `SQLException` 转换为 Spring 统一的数据访问异常体系，业务代码通常不需要直接处理原生 JDBC 异常。

在项目实践中，建议遵循以下原则：

1. Repository 层只处理明确可恢复或有业务含义的异常，例如数据不存在、唯一键冲突。
2. Service 层负责将数据访问异常转换为业务异常。
3. Controller 层不直接捕获数据库异常，由全局异常处理器统一返回标准响应。
4. 事务方法中捕获异常后，如果需要回滚，必须重新抛出异常或手动标记回滚。
5. 日志中记录关键业务参数，但不要输出敏感信息，例如密码、密钥、完整身份证号等。

### 数据访问异常体系

Spring JDBC 的核心异常基类是 `DataAccessException`。它是运行时异常，Repository 或 Service 方法不需要在方法签名中强制声明。常见异常如下：

| 异常类型                                 | 常见原因                     | 处理建议                             |
| ---------------------------------------- | ---------------------------- | ------------------------------------ |
| `EmptyResultDataAccessException`         | 单条查询无结果               | 转换为 `null`、`Optional` 或业务异常 |
| `DuplicateKeyException`                  | 唯一键或主键冲突             | 提示数据已存在                       |
| `DataIntegrityViolationException`        | 非空、外键、长度、约束异常   | 提示数据不合法或约束冲突             |
| `BadSqlGrammarException`                 | SQL 语法错误、表名字段名错误 | 记录错误日志，提示系统异常           |
| `CannotGetJdbcConnectionException`       | 数据库连接失败               | 记录错误日志，提示数据库不可用       |
| `QueryTimeoutException`                  | SQL 执行超时                 | 记录慢 SQL 线索，提示稍后重试        |
| `IncorrectResultSizeDataAccessException` | 期望单条结果但返回多条       | 修正查询条件或唯一约束               |

`EmptyResultDataAccessException` 是业务中最常见的异常之一。比如使用 `queryForObject` 查询单条数据时，如果没有查到数据，就会抛出该异常。它不一定代表系统错误，很多时候只是正常的“数据不存在”场景。

示例处理方式：

```java
try {
    return jdbcTemplate.queryForObject(sql, rowMapper, id);
} catch (EmptyResultDataAccessException e) {
    log.info("未查询到用户，id={}", id);
    return null;
}
```

对于 `DuplicateKeyException`、`DataIntegrityViolationException` 这类异常，通常不建议在 Repository 层吞掉，而是转换为业务异常向上抛出，交给统一异常处理器返回标准响应。

### 常见异常处理

本节给出 Repository 层常见异常处理示例。代码中只捕获有明确业务含义的异常，不建议大范围捕获 `Exception`，否则容易掩盖真实问题，也可能影响事务回滚。

文件位置：`src/main/java/io/github/atengk/jdbc/repository/SysUserExceptionRepository.java`

下面 Repository 演示查询不存在、唯一键冲突、更新失败等常见数据库异常处理方式。

```java
package io.github.atengk.jdbc.repository;

import cn.hutool.core.lang.Assert;
import cn.hutool.core.text.CharSequenceUtil;
import io.github.atengk.jdbc.common.BusinessException;
import io.github.atengk.jdbc.common.ErrorCode;
import io.github.atengk.jdbc.entity.SysUser;
import io.github.atengk.jdbc.mapper.SysUserRowMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

/**
 * 系统用户异常处理 Repository
 *
 * @author Ateng
 * @since 2026-05-04
 */
@Slf4j
@Repository
@RequiredArgsConstructor
public class SysUserExceptionRepository {

    private final JdbcTemplate jdbcTemplate;

    private static final SysUserRowMapper USER_ROW_MAPPER = new SysUserRowMapper();

    /**
     * 根据ID查询用户，不存在时返回 null
     *
     * @param id 用户ID
     * @return 用户信息
     */
    public SysUser findNullableById(Long id) {
        Assert.notNull(id, "用户ID不能为空");

        String sql = """
                SELECT id, username, nickname, age, status, create_time, update_time
                FROM sys_user
                WHERE id = ?
                """;

        try {
            return jdbcTemplate.queryForObject(sql, USER_ROW_MAPPER, id);
        } catch (EmptyResultDataAccessException e) {
            log.info("用户不存在，id={}", id);
            return null;
        }
    }

    /**
     * 根据ID查询用户，不存在时抛出业务异常
     *
     * @param id 用户ID
     * @return 用户信息
     */
    public SysUser findRequiredById(Long id) {
        SysUser user = this.findNullableById(id);
        if (user == null) {
            throw new BusinessException(ErrorCode.DATA_NOT_FOUND, "用户不存在");
        }
        return user;
    }

    /**
     * 新增用户
     *
     * @param user 用户信息
     * @return 影响行数
     */
    public int insert(SysUser user) {
        checkUser(user);

        String sql = """
                INSERT INTO sys_user (username, nickname, age, status)
                VALUES (?, ?, ?, ?)
                """;

        try {
            int rows = jdbcTemplate.update(
                    sql,
                    user.getUsername(),
                    user.getNickname(),
                    user.getAge(),
                    user.getStatus()
            );
            log.info("新增用户成功，username={}，影响行数={}", user.getUsername(), rows);
            return rows;
        } catch (DuplicateKeyException e) {
            log.info("新增用户失败，用户名已存在，username={}", user.getUsername());
            throw new BusinessException(ErrorCode.DATA_DUPLICATE, "用户名已存在", e);
        } catch (DataIntegrityViolationException e) {
            log.info("新增用户失败，数据完整性校验不通过，username={}", user.getUsername());
            throw new BusinessException(ErrorCode.DATA_CONSTRAINT_ERROR, "用户数据不符合数据库约束", e);
        }
    }

    /**
     * 根据ID更新用户状态
     *
     * @param id 用户ID
     * @param status 用户状态
     * @return 影响行数
     */
    public int updateStatusById(Long id, Integer status) {
        Assert.notNull(id, "用户ID不能为空");
        Assert.notNull(status, "用户状态不能为空");

        String sql = """
                UPDATE sys_user
                SET status = ?
                WHERE id = ?
                """;

        int rows = jdbcTemplate.update(sql, status, id);
        if (rows == 0) {
            log.info("更新用户状态失败，用户不存在，id={}", id);
            throw new BusinessException(ErrorCode.DATA_NOT_FOUND, "用户不存在");
        }

        log.info("更新用户状态成功，id={}，status={}，影响行数={}", id, status, rows);
        return rows;
    }

    /**
     * 校验用户信息
     *
     * @param user 用户信息
     */
    private void checkUser(SysUser user) {
        Assert.notNull(user, "用户信息不能为空");
        Assert.isTrue(CharSequenceUtil.isNotBlank(user.getUsername()), "用户名不能为空");
        Assert.isTrue(CharSequenceUtil.isNotBlank(user.getNickname()), "昵称不能为空");
        Assert.notNull(user.getStatus(), "用户状态不能为空");
    }
}
```

这里有两个常用处理策略：

第一，查询方法如果允许数据不存在，可以返回 `null` 或 `Optional<T>`。例如后台详情页查询时，数据不存在可以交给 Service 判断。

第二，如果业务上要求数据必须存在，则 Repository 或 Service 可以直接抛出业务异常。例如更新用户状态前，用户不存在就应该抛出 `BusinessException`，而不是静默返回。

Service 层调用示例：

```java
/**
 * 启用用户
 *
 * @param id 用户ID
 */
@Transactional(rollbackFor = Exception.class)
public void enableUser(Long id) {
    int rows = sysUserExceptionRepository.updateStatusById(id, 1);
    log.info("启用用户完成，id={}，影响行数={}", id, rows);
}
```

需要注意，事务方法中不要随意捕获异常后只记录日志。如果这样写，事务可能不会回滚：

```java
try {
    sysUserExceptionRepository.insert(user);
} catch (Exception e) {
    log.error("新增用户异常", e);
}
```

如果捕获异常后仍然需要事务回滚，应继续抛出异常：

```java
try {
    sysUserExceptionRepository.insert(user);
} catch (Exception e) {
    log.error("新增用户异常，准备回滚事务", e);
    throw e;
}
```

### 统一异常封装

统一异常封装用于保证接口返回格式一致，避免数据库异常直接暴露给前端。推荐定义统一响应对象、错误码、业务异常和全局异常处理器。

文件结构如下：

```text
src/main/java/io/github/atengk/jdbc/common/ErrorCode.java
src/main/java/io/github/atengk/jdbc/common/CommonResult.java
src/main/java/io/github/atengk/jdbc/common/BusinessException.java
src/main/java/io/github/atengk/jdbc/common/JdbcExceptionResolver.java
src/main/java/io/github/atengk/jdbc/handler/GlobalExceptionHandler.java
```

文件位置：`src/main/java/io/github/atengk/jdbc/common/ErrorCode.java`

下面枚举定义接口通用错误码，数据库相关错误单独划分，便于前端和日志排查。

```java
package io.github.atengk.jdbc.common;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

/**
 * 接口错误码
 *
 * @author Ateng
 * @since 2026-05-04
 */
@Getter
@RequiredArgsConstructor
public enum ErrorCode {

    /**
     * 成功
     */
    SUCCESS("0", "成功"),

    /**
     * 请求参数错误
     */
    PARAM_ERROR("400", "请求参数错误"),

    /**
     * 数据不存在
     */
    DATA_NOT_FOUND("404", "数据不存在"),

    /**
     * 数据已存在
     */
    DATA_DUPLICATE("409", "数据已存在"),

    /**
     * 数据库约束异常
     */
    DATA_CONSTRAINT_ERROR("5001", "数据不符合数据库约束"),

    /**
     * 数据访问异常
     */
    DATA_ACCESS_ERROR("5002", "数据访问异常"),

    /**
     * 系统异常
     */
    SYSTEM_ERROR("5000", "系统异常");

    private final String code;

    private final String message;
}
```

文件位置：`src/main/java/io/github/atengk/jdbc/common/CommonResult.java`

下面响应对象用于统一 Controller 返回格式。

```java
package io.github.atengk.jdbc.common;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 通用接口响应
 *
 * @author Ateng
 * @since 2026-05-04
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class CommonResult<T> {

    /**
     * 响应编码
     */
    private String code;

    /**
     * 响应消息
     */
    private String message;

    /**
     * 响应数据
     */
    private T data;

    /**
     * 成功响应
     *
     * @param data 响应数据
     * @return 通用响应
     * @param <T> 数据类型
     */
    public static <T> CommonResult<T> success(T data) {
        return new CommonResult<>(ErrorCode.SUCCESS.getCode(), ErrorCode.SUCCESS.getMessage(), data);
    }

    /**
     * 失败响应
     *
     * @param errorCode 错误码
     * @return 通用响应
     * @param <T> 数据类型
     */
    public static <T> CommonResult<T> fail(ErrorCode errorCode) {
        return new CommonResult<>(errorCode.getCode(), errorCode.getMessage(), null);
    }

    /**
     * 失败响应
     *
     * @param errorCode 错误码
     * @param message 响应消息
     * @return 通用响应
     * @param <T> 数据类型
     */
    public static <T> CommonResult<T> fail(ErrorCode errorCode, String message) {
        return new CommonResult<>(errorCode.getCode(), message, null);
    }

    /**
     * 失败响应
     *
     * @param code 响应编码
     * @param message 响应消息
     * @return 通用响应
     * @param <T> 数据类型
     */
    public static <T> CommonResult<T> fail(String code, String message) {
        return new CommonResult<>(code, message, null);
    }
}
```

文件位置：`src/main/java/io/github/atengk/jdbc/common/BusinessException.java`

下面业务异常用于封装可预期的业务错误，例如用户不存在、用户名重复、数据校验不通过等。

```java
package io.github.atengk.jdbc.common;

import lombok.Getter;

/**
 * 业务异常
 *
 * @author Ateng
 * @since 2026-05-04
 */
@Getter
public class BusinessException extends RuntimeException {

    /**
     * 错误码
     */
    private final ErrorCode errorCode;

    /**
     * 创建业务异常
     *
     * @param errorCode 错误码
     */
    public BusinessException(ErrorCode errorCode) {
        super(errorCode.getMessage());
        this.errorCode = errorCode;
    }

    /**
     * 创建业务异常
     *
     * @param errorCode 错误码
     * @param message 异常消息
     */
    public BusinessException(ErrorCode errorCode, String message) {
        super(message);
        this.errorCode = errorCode;
    }

    /**
     * 创建业务异常
     *
     * @param errorCode 错误码
     * @param message 异常消息
     * @param cause 原始异常
     */
    public BusinessException(ErrorCode errorCode, String message, Throwable cause) {
        super(message, cause);
        this.errorCode = errorCode;
    }
}
```

文件位置：`src/main/java/io/github/atengk/jdbc/common/JdbcExceptionResolver.java`

下面工具类用于将 Spring JDBC 异常转换为业务异常，避免全局异常处理器中堆积大量判断逻辑。

```java
package io.github.atengk.jdbc.common;

import cn.hutool.core.text.CharSequenceUtil;
import org.springframework.dao.DataAccessException;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.dao.QueryTimeoutException;
import org.springframework.jdbc.BadSqlGrammarException;
import org.springframework.jdbc.CannotGetJdbcConnectionException;

/**
 * JDBC 异常解析工具
 *
 * @author Ateng
 * @since 2026-05-04
 */
public final class JdbcExceptionResolver {

    private JdbcExceptionResolver() {
    }

    /**
     * 转换数据访问异常
     *
     * @param exception 数据访问异常
     * @param operation 操作名称
     * @return 业务异常
     */
    public static BusinessException resolve(DataAccessException exception, String operation) {
        String safeOperation = CharSequenceUtil.blankToDefault(operation, "数据库操作");

        if (exception instanceof EmptyResultDataAccessException) {
            return new BusinessException(ErrorCode.DATA_NOT_FOUND, safeOperation + "失败，数据不存在", exception);
        }

        if (exception instanceof DuplicateKeyException) {
            return new BusinessException(ErrorCode.DATA_DUPLICATE, safeOperation + "失败，数据已存在", exception);
        }

        if (exception instanceof DataIntegrityViolationException) {
            return new BusinessException(ErrorCode.DATA_CONSTRAINT_ERROR, safeOperation + "失败，数据不符合数据库约束", exception);
        }

        if (exception instanceof BadSqlGrammarException) {
            return new BusinessException(ErrorCode.DATA_ACCESS_ERROR, safeOperation + "失败，SQL语法异常", exception);
        }

        if (exception instanceof CannotGetJdbcConnectionException) {
            return new BusinessException(ErrorCode.DATA_ACCESS_ERROR, safeOperation + "失败，数据库连接异常", exception);
        }

        if (exception instanceof QueryTimeoutException) {
            return new BusinessException(ErrorCode.DATA_ACCESS_ERROR, safeOperation + "失败，查询超时", exception);
        }

        return new BusinessException(ErrorCode.DATA_ACCESS_ERROR, safeOperation + "失败，请稍后重试", exception);
    }
}
```

文件位置：`src/main/java/io/github/atengk/jdbc/handler/GlobalExceptionHandler.java`

下面全局异常处理器统一处理业务异常、数据库访问异常和未知异常，避免异常信息直接暴露给前端。

```java
package io.github.atengk.jdbc.handler;

import io.github.atengk.jdbc.common.BusinessException;
import io.github.atengk.jdbc.common.CommonResult;
import io.github.atengk.jdbc.common.ErrorCode;
import io.github.atengk.jdbc.common.JdbcExceptionResolver;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataAccessException;
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

    /**
     * 处理业务异常
     *
     * @param exception 业务异常
     * @return 通用响应
     */
    @ExceptionHandler(BusinessException.class)
    public CommonResult<Void> handleBusinessException(BusinessException exception) {
        log.info("业务异常，code={}，message={}",
                exception.getErrorCode().getCode(),
                exception.getMessage());
        return CommonResult.fail(exception.getErrorCode(), exception.getMessage());
    }

    /**
     * 处理数据访问异常
     *
     * @param exception 数据访问异常
     * @return 通用响应
     */
    @ExceptionHandler(DataAccessException.class)
    public CommonResult<Void> handleDataAccessException(DataAccessException exception) {
        BusinessException businessException = JdbcExceptionResolver.resolve(exception, "数据访问");
        log.error("数据访问异常，message={}", businessException.getMessage(), exception);
        return CommonResult.fail(businessException.getErrorCode(), businessException.getMessage());
    }

    /**
     * 处理参数异常
     *
     * @param exception 参数异常
     * @return 通用响应
     */
    @ExceptionHandler(IllegalArgumentException.class)
    public CommonResult<Void> handleIllegalArgumentException(IllegalArgumentException exception) {
        log.info("请求参数异常，message={}", exception.getMessage());
        return CommonResult.fail(ErrorCode.PARAM_ERROR, exception.getMessage());
    }

    /**
     * 处理未知异常
     *
     * @param exception 未知异常
     * @return 通用响应
     */
    @ExceptionHandler(Exception.class)
    public CommonResult<Void> handleException(Exception exception) {
        log.error("系统异常", exception);
        return CommonResult.fail(ErrorCode.SYSTEM_ERROR, "系统异常，请稍后重试");
    }
}
```

Controller 返回示例：

```java
/**
 * 根据ID查询用户
 *
 * @param id 用户ID
 * @return 用户信息
 */
@GetMapping("/{id}")
public CommonResult<SysUser> getById(@PathVariable Long id) {
    SysUser user = sysUserExceptionRepository.findRequiredById(id);
    return CommonResult.success(user);
}
```

接口异常返回示例：

```json
{
  "code": "404",
  "message": "用户不存在",
  "data": null
}
```

数据库唯一键冲突返回示例：

```json
{
  "code": "409",
  "message": "用户名已存在",
  "data": null
}
```

异常处理建议如下：

| 建议                      | 说明                                        |
| ------------------------- | ------------------------------------------- |
| 不直接暴露数据库异常      | 避免前端看到 SQL、表名、字段名等内部信息    |
| Repository 不捕获所有异常 | 只处理有明确业务含义的异常                  |
| Service 负责业务语义转换  | 例如“用户不存在”“订单状态不允许修改”        |
| 全局异常处理统一兜底      | 保证接口返回结构一致                        |
| 事务方法中异常要继续抛出  | 避免异常被吞掉导致事务不回滚                |
| 日志区分级别              | 可预期业务异常用 `info`，系统异常用 `error` |
| 错误码保持稳定            | 前端、网关、监控系统可以基于错误码做处理    |

这一章完成后，后续“实战案例”可以直接复用这里的 `CommonResult`、`BusinessException`、以下继续补充“实战案例”和“测试与验证”两部分，承接前文的统一异常处理、统一响应结构、`JdbcTemplate` 基础使用、事务和批量操作内容。

## 实战案例

本节通过一个用户管理案例串联 `JdbcTemplate` 的表设计、Repository、Service 和 Controller。示例以 Spring Boot 3、MySQL、`JdbcTemplate`、统一响应对象 `CommonResult`、统一异常处理器 `GlobalExceptionHandler` 为基础。`JdbcTemplate` 的 `query`、`queryForObject`、`update` 等方法可以直接执行 SQL 并配合 `RowMapper` 完成结果映射，适合这种轻量 CRUD 场景。([Home](https://docs.spring.io/spring-framework/docs/current/javadoc-api/org/springframework/jdbc/core/JdbcTemplate.html?utm_source=chatgpt.com))

### 用户表设计

用户表用于演示新增用户、查询详情、查询列表、更新状态和删除用户。字段保持简单，重点展示 JDBC Template 的开发方式。

文件位置：`src/main/resources/sql/schema.sql`

下面 SQL 创建用户表，并增加唯一索引、状态字段和时间字段，便于后续演示异常处理、查询和更新。

```sql
-- 用户表：用于演示 JdbcTemplate 实战案例
CREATE TABLE IF NOT EXISTS sys_user (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '主键ID',
    username VARCHAR(64) NOT NULL COMMENT '用户名',
    nickname VARCHAR(64) NOT NULL COMMENT '昵称',
    age INT DEFAULT NULL COMMENT '年龄',
    status TINYINT NOT NULL DEFAULT 1 COMMENT '状态：0禁用，1启用',
    deleted TINYINT NOT NULL DEFAULT 0 COMMENT '删除状态：0未删除，1已删除',
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    UNIQUE KEY uk_sys_user_username (username),
    KEY idx_sys_user_status (status),
    KEY idx_sys_user_deleted (deleted)
) COMMENT = '系统用户表';
```

文件位置：`src/main/resources/sql/data.sql`

下面 SQL 初始化两条测试数据。Spring Boot 可以通过基础 SQL 脚本初始化数据库，默认支持 `schema.sql` 和 `data.sql`，也可以通过 `spring.sql.init.schema-locations` 和 `spring.sql.init.data-locations` 自定义脚本路径。([Home](https://docs.spring.io/spring-boot/how-to/data-initialization.html?utm_source=chatgpt.com))

```sql
-- 初始化用户数据：用于本地接口验证
INSERT INTO sys_user (username, nickname, age, status, deleted)
VALUES
    ('admin', '管理员', 30, 1, 0),
    ('test', '测试用户', 22, 1, 0)
ON DUPLICATE KEY UPDATE
    nickname = VALUES(nickname),
    age = VALUES(age),
    status = VALUES(status),
    deleted = VALUES(deleted);
```

如果使用前文的自定义脚本路径，需要保证 `application.yml` 中包含以下配置：

```yaml
spring:
  sql:
    init:
      # 非嵌入式数据库也执行初始化脚本，生产环境谨慎开启
      mode: always
      # 建表脚本路径
      schema-locations: classpath:sql/schema.sql
      # 初始化数据脚本路径
      data-locations: classpath:sql/data.sql
      # 脚本失败时快速失败，便于开发阶段发现问题
      continue-on-error: false
```

### Repository 层实现

Repository 层只负责 SQL 执行和结果映射，不处理复杂业务流程。这里使用 `JdbcTemplate` 完成基础 CRUD，并将“数据不存在”“用户名重复”等明确异常转换为前文定义的 `BusinessException`。

文件结构如下：

```text
src/main/java/io/github/atengk/jdbc/dto/UserCreateRequest.java
src/main/java/io/github/atengk/jdbc/dto/UserUpdateStatusRequest.java
src/main/java/io/github/atengk/jdbc/entity/SysUser.java
src/main/java/io/github/atengk/jdbc/vo/UserResponse.java
src/main/java/io/github/atengk/jdbc/mapper/SysUserRowMapper.java
src/main/java/io/github/atengk/jdbc/repository/SysUserRepository.java
```

文件位置：`src/main/java/io/github/atengk/jdbc/dto/UserCreateRequest.java`

下面 DTO 用于接收新增用户请求参数。

```java
package io.github.atengk.jdbc.dto;

import lombok.Data;

/**
 * 用户新增请求
 *
 * @author Ateng
 * @since 2026-05-04
 */
@Data
public class UserCreateRequest {

    /**
     * 用户名
     */
    private String username;

    /**
     * 昵称
     */
    private String nickname;

    /**
     * 年龄
     */
    private Integer age;
}
```

文件位置：`src/main/java/io/github/atengk/jdbc/dto/UserUpdateStatusRequest.java`

下面 DTO 用于接收用户状态更新请求。

```java
package io.github.atengk.jdbc.dto;

import lombok.Data;

/**
 * 用户状态更新请求
 *
 * @author Ateng
 * @since 2026-05-04
 */
@Data
public class UserUpdateStatusRequest {

    /**
     * 用户状态：0禁用，1启用
     */
    private Integer status;
}
```

文件位置：`src/main/java/io/github/atengk/jdbc/vo/UserResponse.java`

下面 VO 用于接口返回，避免直接把数据库实体暴露给前端。

```java
package io.github.atengk.jdbc.vo;

import lombok.Data;

import java.time.LocalDateTime;

/**
 * 用户响应对象
 *
 * @author Ateng
 * @since 2026-05-04
 */
@Data
public class UserResponse {

    /**
     * 主键ID
     */
    private Long id;

    /**
     * 用户名
     */
    private String username;

    /**
     * 昵称
     */
    private String nickname;

    /**
     * 年龄
     */
    private Integer age;

    /**
     * 状态：0禁用，1启用
     */
    private Integer status;

    /**
     * 状态名称
     */
    private String statusName;

    /**
     * 创建时间
     */
    private LocalDateTime createTime;
}
```

文件位置：`src/main/java/io/github/atengk/jdbc/repository/SysUserRepository.java`

下面 Repository 提供新增、详情、列表、状态更新和逻辑删除方法。查询单条数据时，`queryForObject` 适合配合 `RowMapper` 返回单个对象；当查询结果数量不符合预期时，Spring JDBC 会抛出数据访问异常。([Home](https://docs.spring.io/spring-framework/docs/6.1.12/javadoc-api/org/springframework/jdbc/core/JdbcTemplate.html?utm_source=chatgpt.com))

```java
package io.github.atengk.jdbc.repository;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.text.CharSequenceUtil;
import io.github.atengk.jdbc.common.BusinessException;
import io.github.atengk.jdbc.common.ErrorCode;
import io.github.atengk.jdbc.entity.SysUser;
import io.github.atengk.jdbc.mapper.SysUserRowMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * 系统用户 Repository
 *
 * @author Ateng
 * @since 2026-05-04
 */
@Slf4j
@Repository
@RequiredArgsConstructor
public class SysUserRepository {

    private final JdbcTemplate jdbcTemplate;

    private static final SysUserRowMapper USER_ROW_MAPPER = new SysUserRowMapper();

    /**
     * 新增用户
     *
     * @param user 用户信息
     * @return 影响行数
     */
    public int insert(SysUser user) {
        String sql = """
                INSERT INTO sys_user (username, nickname, age, status, deleted)
                VALUES (?, ?, ?, ?, 0)
                """;

        try {
            int rows = jdbcTemplate.update(
                    sql,
                    user.getUsername(),
                    user.getNickname(),
                    user.getAge(),
                    user.getStatus()
            );
            log.info("新增用户成功，username={}，影响行数={}", user.getUsername(), rows);
            return rows;
        } catch (DuplicateKeyException e) {
            log.info("新增用户失败，用户名已存在，username={}", user.getUsername());
            throw new BusinessException(ErrorCode.DATA_DUPLICATE, "用户名已存在", e);
        }
    }

    /**
     * 根据ID查询用户
     *
     * @param id 用户ID
     * @return 用户信息，不存在时返回 null
     */
    public SysUser findById(Long id) {
        String sql = """
                SELECT id, username, nickname, age, status, deleted, create_time, update_time
                FROM sys_user
                WHERE id = ? AND deleted = 0
                """;

        try {
            return jdbcTemplate.queryForObject(sql, USER_ROW_MAPPER, id);
        } catch (EmptyResultDataAccessException e) {
            log.info("用户不存在，id={}", id);
            return null;
        }
    }

    /**
     * 根据ID查询必存在用户
     *
     * @param id 用户ID
     * @return 用户信息
     */
    public SysUser findRequiredById(Long id) {
        SysUser user = this.findById(id);
        if (user == null) {
            throw new BusinessException(ErrorCode.DATA_NOT_FOUND, "用户不存在");
        }
        return user;
    }

    /**
     * 查询用户列表
     *
     * @param keyword 关键字
     * @param status 用户状态
     * @return 用户列表
     */
    public List<SysUser> findList(String keyword, Integer status) {
        StringBuilder sql = new StringBuilder("""
                SELECT id, username, nickname, age, status, deleted, create_time, update_time
                FROM sys_user
                WHERE deleted = 0
                """);

        List<Object> params = CollUtil.newArrayList();

        if (CharSequenceUtil.isNotBlank(keyword)) {
            sql.append(" AND (username LIKE ? OR nickname LIKE ?) ");
            String likeKeyword = "%" + keyword + "%";
            params.add(likeKeyword);
            params.add(likeKeyword);
        }

        if (status != null) {
            sql.append(" AND status = ? ");
            params.add(status);
        }

        sql.append(" ORDER BY id DESC ");

        return jdbcTemplate.query(sql.toString(), USER_ROW_MAPPER, params.toArray());
    }

    /**
     * 根据ID更新用户状态
     *
     * @param id 用户ID
     * @param status 用户状态
     * @return 影响行数
     */
    public int updateStatusById(Long id, Integer status) {
        String sql = """
                UPDATE sys_user
                SET status = ?
                WHERE id = ? AND deleted = 0
                """;

        int rows = jdbcTemplate.update(sql, status, id);
        if (rows == 0) {
            log.info("更新用户状态失败，用户不存在，id={}", id);
            throw new BusinessException(ErrorCode.DATA_NOT_FOUND, "用户不存在");
        }

        log.info("更新用户状态成功，id={}，status={}，影响行数={}", id, status, rows);
        return rows;
    }

    /**
     * 根据ID逻辑删除用户
     *
     * @param id 用户ID
     * @return 影响行数
     */
    public int logicDeleteById(Long id) {
        String sql = """
                UPDATE sys_user
                SET deleted = 1
                WHERE id = ? AND deleted = 0
                """;

        int rows = jdbcTemplate.update(sql, id);
        if (rows == 0) {
            log.info("删除用户失败，用户不存在，id={}", id);
            throw new BusinessException(ErrorCode.DATA_NOT_FOUND, "用户不存在");
        }

        log.info("删除用户成功，id={}，影响行数={}", id, rows);
        return rows;
    }
}
```

### Service 层调用

Service 层负责参数校验、业务规则、事务边界和对象转换。Repository 层抛出的 `BusinessException` 会继续向上抛出，最终由 `GlobalExceptionHandler` 统一转换为 `CommonResult`。

文件结构如下：

```text
src/main/java/io/github/atengk/jdbc/service/SysUserService.java
src/main/java/io/github/atengk/jdbc/service/impl/SysUserServiceImpl.java
```

文件位置：`src/main/java/io/github/atengk/jdbc/service/SysUserService.java`

下面接口定义用户管理对外提供的业务能力。

```java
package io.github.atengk.jdbc.service;

import io.github.atengk.jdbc.dto.UserCreateRequest;
import io.github.atengk.jdbc.dto.UserUpdateStatusRequest;
import io.github.atengk.jdbc.vo.UserResponse;

import java.util.List;

/**
 * 系统用户 Service
 *
 * @author Ateng
 * @since 2026-05-04
 */
public interface SysUserService {

    /**
     * 新增用户
     *
     * @param request 新增请求
     * @return 用户ID
     */
    Long createUser(UserCreateRequest request);

    /**
     * 查询用户详情
     *
     * @param id 用户ID
     * @return 用户详情
     */
    UserResponse getUser(Long id);

    /**
     * 查询用户列表
     *
     * @param keyword 关键字
     * @param status 用户状态
     * @return 用户列表
     */
    List<UserResponse> listUsers(String keyword, Integer status);

    /**
     * 更新用户状态
     *
     * @param id 用户ID
     * @param request 状态更新请求
     */
    void updateStatus(Long id, UserUpdateStatusRequest request);

    /**
     * 删除用户
     *
     * @param id 用户ID
     */
    void deleteUser(Long id);
}
```

文件位置：`src/main/java/io/github/atengk/jdbc/service/impl/SysUserServiceImpl.java`

下面 Service 使用 `@Transactional` 控制增删改事务，并使用 Hutool 完成参数校验和字符串处理。

```java
package io.github.atengk.jdbc.service.impl;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.lang.Assert;
import cn.hutool.core.text.CharSequenceUtil;
import io.github.atengk.jdbc.dto.UserCreateRequest;
import io.github.atengk.jdbc.dto.UserUpdateStatusRequest;
import io.github.atengk.jdbc.entity.SysUser;
import io.github.atengk.jdbc.repository.SysUserRepository;
import io.github.atengk.jdbc.service.SysUserService;
import io.github.atengk.jdbc.vo.UserResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * 系统用户 Service 实现
 *
 * @author Ateng
 * @since 2026-05-04
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class SysUserServiceImpl implements SysUserService {

    private final SysUserRepository sysUserRepository;

    /**
     * 新增用户
     *
     * @param request 新增请求
     * @return 用户ID
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long createUser(UserCreateRequest request) {
        checkCreateRequest(request);

        SysUser user = new SysUser();
        user.setUsername(request.getUsername());
        user.setNickname(request.getNickname());
        user.setAge(request.getAge());
        user.setStatus(1);

        sysUserRepository.insert(user);
        log.info("用户创建完成，username={}", request.getUsername());

        /*
         * 当前示例 Repository insert 未返回自增ID。
         * 如果业务必须返回真实ID，可使用 KeyHolder 获取自增主键。
         */
        SysUser savedUser = sysUserRepository.findList(request.getUsername(), 1)
                .stream()
                .filter(item -> CharSequenceUtil.equals(item.getUsername(), request.getUsername()))
                .findFirst()
                .orElseThrow(() -> new IllegalStateException("用户创建后查询失败"));

        return savedUser.getId();
    }

    /**
     * 查询用户详情
     *
     * @param id 用户ID
     * @return 用户详情
     */
    @Override
    public UserResponse getUser(Long id) {
        Assert.notNull(id, "用户ID不能为空");
        SysUser user = sysUserRepository.findRequiredById(id);
        return toResponse(user);
    }

    /**
     * 查询用户列表
     *
     * @param keyword 关键字
     * @param status 用户状态
     * @return 用户列表
     */
    @Override
    public List<UserResponse> listUsers(String keyword, Integer status) {
        List<SysUser> users = sysUserRepository.findList(keyword, status);
        if (CollUtil.isEmpty(users)) {
            return List.of();
        }
        return users.stream().map(this::toResponse).toList();
    }

    /**
     * 更新用户状态
     *
     * @param id 用户ID
     * @param request 状态更新请求
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updateStatus(Long id, UserUpdateStatusRequest request) {
        Assert.notNull(id, "用户ID不能为空");
        Assert.notNull(request, "状态更新请求不能为空");
        Assert.notNull(request.getStatus(), "用户状态不能为空");
        Assert.isTrue(request.getStatus() == 0 || request.getStatus() == 1, "用户状态只能是0或1");

        sysUserRepository.updateStatusById(id, request.getStatus());
        log.info("用户状态更新完成，id={}，status={}", id, request.getStatus());
    }

    /**
     * 删除用户
     *
     * @param id 用户ID
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteUser(Long id) {
        Assert.notNull(id, "用户ID不能为空");
        sysUserRepository.logicDeleteById(id);
        log.info("用户逻辑删除完成，id={}", id);
    }

    /**
     * 校验新增请求
     *
     * @param request 新增请求
     */
    private void checkCreateRequest(UserCreateRequest request) {
        Assert.notNull(request, "新增请求不能为空");
        Assert.isTrue(CharSequenceUtil.isNotBlank(request.getUsername()), "用户名不能为空");
        Assert.isTrue(CharSequenceUtil.isNotBlank(request.getNickname()), "昵称不能为空");

        if (request.getAge() != null) {
            Assert.isTrue(request.getAge() >= 0 && request.getAge() <= 150, "年龄范围不合法");
        }
    }

    /**
     * 转换响应对象
     *
     * @param user 用户实体
     * @return 用户响应对象
     */
    private UserResponse toResponse(SysUser user) {
        UserResponse response = new UserResponse();
        response.setId(user.getId());
        response.setUsername(user.getUsername());
        response.setNickname(user.getNickname());
        response.setAge(user.getAge());
        response.setStatus(user.getStatus());
        response.setStatusName(convertStatusName(user.getStatus()));
        response.setCreateTime(user.getCreateTime());
        return response;
    }

    /**
     * 转换状态名称
     *
     * @param status 状态
     * @return 状态名称
     */
    private String convertStatusName(Integer status) {
        if (status == null) {
            return "未知";
        }
        return switch (status) {
            case 0 -> "禁用";
            case 1 -> "启用";
            default -> "未知";
        };
    }
}
```

上面的 `createUser` 为了保持 `insert` 示例简单，通过用户名反查创建后的用户 ID。生产项目更推荐使用 `GeneratedKeyHolder` 获取数据库自增主键，避免新增后再次查询。

可以将 Repository 的新增方法改成下面这样：

```java
 /**
  * 新增用户并返回自增ID
  *
  * @param user 用户信息
  * @return 自增ID
  */
public Long insertAndReturnId(SysUser user) {
    String sql = """
            INSERT INTO sys_user (username, nickname, age, status, deleted)
            VALUES (?, ?, ?, ?, 0)
            """;

    KeyHolder keyHolder = new GeneratedKeyHolder();

    jdbcTemplate.update(connection -> {
        PreparedStatement ps = connection.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS);
        ps.setString(1, user.getUsername());
        ps.setString(2, user.getNickname());
        ps.setObject(3, user.getAge());
        ps.setInt(4, user.getStatus());
        return ps;
    }, keyHolder);

    Number key = keyHolder.getKey();
    Assert.notNull(key, "获取用户自增ID失败");

    Long id = key.longValue();
    log.info("新增用户成功，username={}，id={}", user.getUsername(), id);
    return id;
}
```

对应需要补充的 import 如下：

```java
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;

import java.sql.PreparedStatement;
import java.sql.Statement;
```

### Controller 接口验证

Controller 层只负责接收请求、调用 Service、返回统一响应。异常不在 Controller 中捕获，由前文的 `GlobalExceptionHandler` 统一处理。

文件位置：`src/main/java/io/github/atengk/jdbc/controller/SysUserController.java`

下面 Controller 提供用户新增、详情、列表、状态更新和删除接口，所有接口统一返回 `CommonResult`。

```java
package io.github.atengk.jdbc.controller;

import io.github.atengk.jdbc.common.CommonResult;
import io.github.atengk.jdbc.dto.UserCreateRequest;
import io.github.atengk.jdbc.dto.UserUpdateStatusRequest;
import io.github.atengk.jdbc.service.SysUserService;
import io.github.atengk.jdbc.vo.UserResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 系统用户 Controller
 *
 * @author Ateng
 * @since 2026-05-04
 */
@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
public class SysUserController {

    private final SysUserService sysUserService;

    /**
     * 新增用户
     *
     * @param request 新增请求
     * @return 用户ID
     */
    @PostMapping
    public CommonResult<Long> createUser(@RequestBody UserCreateRequest request) {
        Long id = sysUserService.createUser(request);
        return CommonResult.success(id);
    }

    /**
     * 查询用户详情
     *
     * @param id 用户ID
     * @return 用户详情
     */
    @GetMapping("/{id}")
    public CommonResult<UserResponse> getUser(@PathVariable Long id) {
        UserResponse user = sysUserService.getUser(id);
        return CommonResult.success(user);
    }

    /**
     * 查询用户列表
     *
     * @param keyword 关键字
     * @param status 用户状态
     * @return 用户列表
     */
    @GetMapping
    public CommonResult<List<UserResponse>> listUsers(
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) Integer status) {
        List<UserResponse> users = sysUserService.listUsers(keyword, status);
        return CommonResult.success(users);
    }

    /**
     * 更新用户状态
     *
     * @param id 用户ID
     * @param request 状态更新请求
     * @return 空响应
     */
    @PutMapping("/{id}/status")
    public CommonResult<Void> updateStatus(
            @PathVariable Long id,
            @RequestBody UserUpdateStatusRequest request) {
        sysUserService.updateStatus(id, request);
        return CommonResult.success(null);
    }

    /**
     * 删除用户
     *
     * @param id 用户ID
     * @return 空响应
     */
    @DeleteMapping("/{id}")
    public CommonResult<Void> deleteUser(@PathVariable Long id) {
        sysUserService.deleteUser(id);
        return CommonResult.success(null);
    }
}
```

接口验证命令如下：

```bash
# 新增用户
curl -X POST "http://localhost:8080/api/users" \
  -H "Content-Type: application/json" \
  -d '{
    "username": "lisi",
    "nickname": "李四",
    "age": 26
  }'

# 查询用户详情
curl -X GET "http://localhost:8080/api/users/1"

# 查询用户列表
curl -X GET "http://localhost:8080/api/users?keyword=admin&status=1"

# 禁用用户
curl -X PUT "http://localhost:8080/api/users/1/status" \
  -H "Content-Type: application/json" \
  -d '{
    "status": 0
  }'

# 删除用户
curl -X DELETE "http://localhost:8080/api/users/1"
```

新增成功响应示例：

```json
{
  "code": "0",
  "message": "成功",
  "data": 3
}
```

查询详情响应示例：

```json
{
  "code": "0",
  "message": "成功",
  "data": {
    "id": 1,
    "username": "admin",
    "nickname": "管理员",
    "age": 30,
    "status": 1,
    "statusName": "启用",
    "createTime": "2026-05-04T10:00:00"
  }
}
```

用户不存在响应示例：

```json
{
  "code": "404",
  "message": "用户不存在",
  "data": null
}
```

## 测试与验证

测试与验证用于确认 Repository SQL、Service 业务逻辑和 Controller 接口行为是否符合预期。这里给出三类测试：单元测试、集成测试和接口测试。`TestRestTemplate` 是 Spring Boot 提供的面向集成测试的 HTTP 客户端，在使用 `@SpringBootTest(webEnvironment = RANDOM_PORT)` 启动嵌入式 Web 环境时可以自动注入。([Home](https://docs.spring.io/spring-boot/3.4/api/java/org/springframework/boot/test/web/client/TestRestTemplate.html?utm_source=chatgpt.com))

### 单元测试

单元测试重点验证 Service 的业务逻辑，不启动完整 Spring 容器。这里使用 Mockito 模拟 Repository，测试参数校验、正常调用和异常场景。

文件位置：`src/test/java/io/github/atengk/jdbc/service/SysUserServiceImplTest.java`

下面测试类验证 `SysUserServiceImpl` 的新增、查询和更新逻辑。

```java
package io.github.atengk.jdbc.service;

import io.github.atengk.jdbc.dto.UserCreateRequest;
import io.github.atengk.jdbc.dto.UserUpdateStatusRequest;
import io.github.atengk.jdbc.entity.SysUser;
import io.github.atengk.jdbc.repository.SysUserRepository;
import io.github.atengk.jdbc.service.impl.SysUserServiceImpl;
import io.github.atengk.jdbc.vo.UserResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * 系统用户 Service 单元测试
 *
 * @author Ateng
 * @since 2026-05-04
 */
class SysUserServiceImplTest {

    private SysUserRepository sysUserRepository;

    private SysUserServiceImpl sysUserService;

    /**
     * 初始化测试对象
     */
    @BeforeEach
    void setUp() {
        sysUserRepository = Mockito.mock(SysUserRepository.class);
        sysUserService = new SysUserServiceImpl(sysUserRepository);
    }

    /**
     * 测试查询用户详情
     */
    @Test
    void getUserShouldReturnUserResponse() {
        SysUser user = buildUser(1L, "admin", "管理员", 1);
        Mockito.when(sysUserRepository.findRequiredById(1L)).thenReturn(user);

        UserResponse response = sysUserService.getUser(1L);

        assertThat(response).isNotNull();
        assertThat(response.getId()).isEqualTo(1L);
        assertThat(response.getUsername()).isEqualTo("admin");
        assertThat(response.getStatusName()).isEqualTo("启用");
    }

    /**
     * 测试查询用户列表
     */
    @Test
    void listUsersShouldReturnUserResponses() {
        SysUser user = buildUser(1L, "admin", "管理员", 1);
        Mockito.when(sysUserRepository.findList("admin", 1)).thenReturn(List.of(user));

        List<UserResponse> responses = sysUserService.listUsers("admin", 1);

        assertThat(responses).hasSize(1);
        assertThat(responses.get(0).getUsername()).isEqualTo("admin");
    }

    /**
     * 测试更新状态参数非法
     */
    @Test
    void updateStatusShouldThrowExceptionWhenStatusInvalid() {
        UserUpdateStatusRequest request = new UserUpdateStatusRequest();
        request.setStatus(3);

        assertThatThrownBy(() -> sysUserService.updateStatus(1L, request))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("用户状态只能是0或1");
    }

    /**
     * 测试新增用户参数非法
     */
    @Test
    void createUserShouldThrowExceptionWhenUsernameBlank() {
        UserCreateRequest request = new UserCreateRequest();
        request.setUsername("");
        request.setNickname("测试用户");
        request.setAge(20);

        assertThatThrownBy(() -> sysUserService.createUser(request))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("用户名不能为空");
    }

    /**
     * 构造用户实体
     *
     * @param id 用户ID
     * @param username 用户名
     * @param nickname 昵称
     * @param status 状态
     * @return 用户实体
     */
    private SysUser buildUser(Long id, String username, String nickname, Integer status) {
        SysUser user = new SysUser();
        user.setId(id);
        user.setUsername(username);
        user.setNickname(nickname);
        user.setAge(30);
        user.setStatus(status);
        user.setDeleted(0);
        user.setCreateTime(LocalDateTime.now());
        user.setUpdateTime(LocalDateTime.now());
        return user;
    }
}
```

单元测试执行命令：

```bash
# 执行所有测试
mvn test

# 执行指定测试类
mvn -Dtest=SysUserServiceImplTest test
```

### 集成测试

集成测试重点验证 Repository SQL 是否可以真实执行。为了避免依赖本地 MySQL，可以在测试环境使用 H2 内存数据库，并启用 MySQL 兼容模式。Spring Boot 的 SQL 初始化机制可以加载测试目录下的 `schema.sql` 和 `data.sql`，用于准备测试表和测试数据。([Home](https://docs.spring.io/spring-boot/how-to/data-initialization.html?utm_source=chatgpt.com))

测试依赖如下：

```xml
<!-- H2：测试环境内存数据库，避免依赖本地 MySQL -->
<dependency>
    <groupId>com.h2database</groupId>
    <artifactId>h2</artifactId>
    <scope>test</scope>
</dependency>
```

文件位置：`src/test/resources/application-test.yml`

下面配置用于测试环境连接 H2，并开启 SQL 初始化。

```yaml
spring:
  datasource:
    # H2 MySQL 兼容模式，用于测试常见 MySQL SQL
    url: jdbc:h2:mem:jdbc_template_test;MODE=MySQL;DATABASE_TO_LOWER=TRUE;DB_CLOSE_DELAY=-1
    driver-class-name: org.h2.Driver
    username: sa
    password:

  sql:
    init:
      # 测试启动时执行 schema.sql 和 data.sql
      mode: always
      schema-locations: classpath:schema.sql
      data-locations: classpath:data.sql

logging:
  level:
    io.github.atengk: debug
```

文件位置：`src/test/resources/schema.sql`

测试环境建表脚本如下：

```sql
DROP TABLE IF EXISTS sys_user;

CREATE TABLE sys_user (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    username VARCHAR(64) NOT NULL,
    nickname VARCHAR(64) NOT NULL,
    age INT DEFAULT NULL,
    status TINYINT NOT NULL DEFAULT 1,
    deleted TINYINT NOT NULL DEFAULT 0,
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    UNIQUE KEY uk_sys_user_username (username)
);
```

文件位置：`src/test/resources/data.sql`

测试环境初始化数据如下：

```sql
INSERT INTO sys_user (username, nickname, age, status, deleted)
VALUES
    ('admin', '管理员', 30, 1, 0),
    ('disabled_user', '禁用用户', 28, 0, 0),
    ('deleted_user', '已删除用户', 25, 1, 1);
```

文件位置：`src/test/java/io/github/atengk/jdbc/repository/SysUserRepositoryTest.java`

下面集成测试启动 JDBC 测试切片，验证 Repository 的查询、新增、更新和逻辑删除。

```java
package io.github.atengk.jdbc.repository;

import io.github.atengk.jdbc.common.BusinessException;
import io.github.atengk.jdbc.entity.SysUser;
import io.github.atengk.jdbc.mapper.SysUserRowMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.JdbcTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * 系统用户 Repository 集成测试
 *
 * @author Ateng
 * @since 2026-05-04
 */
@JdbcTest
@ActiveProfiles("test")
@Import({SysUserRepository.class, SysUserRowMapper.class})
class SysUserRepositoryTest {

    @Autowired
    private SysUserRepository sysUserRepository;

    /**
     * 测试根据ID查询用户
     */
    @Test
    void findByIdShouldReturnUser() {
        SysUser user = sysUserRepository.findById(1L);

        assertThat(user).isNotNull();
        assertThat(user.getUsername()).isEqualTo("admin");
        assertThat(user.getStatus()).isEqualTo(1);
    }

    /**
     * 测试查询用户列表
     */
    @Test
    void findListShouldReturnUsers() {
        List<SysUser> users = sysUserRepository.findList(null, 1);

        assertThat(users).isNotEmpty();
        assertThat(users)
                .extracting(SysUser::getUsername)
                .contains("admin")
                .doesNotContain("deleted_user");
    }

    /**
     * 测试新增用户
     */
    @Test
    void insertShouldCreateUser() {
        SysUser user = new SysUser();
        user.setUsername("new_user");
        user.setNickname("新用户");
        user.setAge(18);
        user.setStatus(1);

        int rows = sysUserRepository.insert(user);

        assertThat(rows).isEqualTo(1);
        assertThat(sysUserRepository.findList("new_user", 1)).hasSize(1);
    }

    /**
     * 测试重复用户名
     */
    @Test
    void insertShouldThrowExceptionWhenUsernameDuplicate() {
        SysUser user = new SysUser();
        user.setUsername("admin");
        user.setNickname("重复管理员");
        user.setAge(31);
        user.setStatus(1);

        assertThatThrownBy(() -> sysUserRepository.insert(user))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("用户名已存在");
    }

    /**
     * 测试更新用户状态
     */
    @Test
    void updateStatusByIdShouldUpdateUserStatus() {
        int rows = sysUserRepository.updateStatusById(1L, 0);

        SysUser user = sysUserRepository.findById(1L);
        assertThat(rows).isEqualTo(1);
        assertThat(user.getStatus()).isEqualTo(0);
    }

    /**
     * 测试逻辑删除用户
     */
    @Test
    void logicDeleteByIdShouldHideUser() {
        int rows = sysUserRepository.logicDeleteById(1L);

        SysUser user = sysUserRepository.findById(1L);
        assertThat(rows).isEqualTo(1);
        assertThat(user).isNull();
    }
}
```

如果 `@Import({SysUserRepository.class, SysUserRowMapper.class})` 中 `SysUserRowMapper` 不是 Spring Bean，可以去掉它；当前示例中的 Repository 使用 `new SysUserRowMapper()` 常量，不强制依赖容器注入。

集成测试执行命令：

```bash
# 使用 test profile 执行 Repository 集成测试
mvn -Dtest=SysUserRepositoryTest test
```

### 接口测试

接口测试用于验证 Controller、Service、Repository、异常处理器和统一响应对象是否完整串联。这里使用 `@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)` 启动真实 Web 环境，并通过 `TestRestTemplate` 调用 HTTP 接口。`TestRestTemplate` 对 4xx 和 5xx 响应不会直接抛异常，而是允许通过响应状态码和响应体进行断言，适合接口集成测试。([Home](https://docs.spring.io/spring-boot/3.4/api/java/org/springframework/boot/test/web/client/TestRestTemplate.html?utm_source=chatgpt.com))

文件位置：`src/test/java/io/github/atengk/jdbc/controller/SysUserControllerTest.java`

下面接口测试覆盖新增、列表查询、详情查询、状态更新、删除和异常响应。

```java
package io.github.atengk.jdbc.controller;

import io.github.atengk.jdbc.common.CommonResult;
import io.github.atengk.jdbc.dto.UserCreateRequest;
import io.github.atengk.jdbc.dto.UserUpdateStatusRequest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.*;
import org.springframework.test.context.ActiveProfiles;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 系统用户 Controller 接口测试
 *
 * @author Ateng
 * @since 2026-05-04
 */
@ActiveProfiles("test")
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class SysUserControllerTest {

    @Autowired
    private TestRestTemplate testRestTemplate;

    /**
     * 测试新增用户
     */
    @Test
    void createUserShouldReturnSuccess() {
        UserCreateRequest request = new UserCreateRequest();
        request.setUsername("api_user");
        request.setNickname("接口用户");
        request.setAge(23);

        ResponseEntity<CommonResult<Long>> response = testRestTemplate.exchange(
                "/api/users",
                HttpMethod.POST,
                new HttpEntity<>(request, jsonHeaders()),
                new ParameterizedTypeReference<>() {
                }
        );

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().getCode()).isEqualTo("0");
        assertThat(response.getBody().getData()).isNotNull();
    }

    /**
     * 测试查询用户列表
     */
    @Test
    void listUsersShouldReturnSuccess() {
        ResponseEntity<CommonResult<List<Map<String, Object>>>> response = testRestTemplate.exchange(
                "/api/users?status=1",
                HttpMethod.GET,
                null,
                new ParameterizedTypeReference<>() {
                }
        );

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().getCode()).isEqualTo("0");
        assertThat(response.getBody().getData()).isNotEmpty();
    }

    /**
     * 测试查询用户详情
     */
    @Test
    void getUserShouldReturnSuccess() {
        ResponseEntity<CommonResult<Map<String, Object>>> response = testRestTemplate.exchange(
                "/api/users/1",
                HttpMethod.GET,
                null,
                new ParameterizedTypeReference<>() {
                }
        );

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().getCode()).isEqualTo("0");
        assertThat(response.getBody().getData().get("username")).isEqualTo("admin");
    }

    /**
     * 测试更新用户状态
     */
    @Test
    void updateStatusShouldReturnSuccess() {
        UserUpdateStatusRequest request = new UserUpdateStatusRequest();
        request.setStatus(0);

        ResponseEntity<CommonResult<Void>> response = testRestTemplate.exchange(
                "/api/users/1/status",
                HttpMethod.PUT,
                new HttpEntity<>(request, jsonHeaders()),
                new ParameterizedTypeReference<>() {
                }
        );

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().getCode()).isEqualTo("0");
    }

    /**
     * 测试删除用户
     */
    @Test
    void deleteUserShouldReturnSuccess() {
        ResponseEntity<CommonResult<Void>> response = testRestTemplate.exchange(
                "/api/users/2",
                HttpMethod.DELETE,
                null,
                new ParameterizedTypeReference<>() {
                }
        );

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().getCode()).isEqualTo("0");
    }

    /**
     * 测试用户不存在异常响应
     */
    @Test
    void getUserShouldReturnFailWhenUserNotFound() {
        ResponseEntity<CommonResult<Void>> response = testRestTemplate.exchange(
                "/api/users/99999",
                HttpMethod.GET,
                null,
                new ParameterizedTypeReference<>() {
                }
        );

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().getCode()).isEqualTo("404");
        assertThat(response.getBody().getMessage()).isEqualTo("用户不存在");
    }

    /**
     * 构造 JSON 请求头
     *
     * @return 请求头
     */
    private HttpHeaders jsonHeaders() {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        return headers;
    }
}
```

接口测试执行命令：

```bash
# 执行 Controller 接口测试
mvn -Dtest=SysUserControllerTest test

# 执行全部测试
mvn test
```

测试建议如下：

| 测试类型            | 重点                                 | 推荐工具                              |
| ------------------- | ------------------------------------ | ------------------------------------- |
| 单元测试            | Service 参数校验、业务分支、异常分支 | JUnit 5、Mockito、AssertJ             |
| Repository 集成测试 | SQL 正确性、映射正确性、唯一键异常   | `@JdbcTest`、H2 或 Testcontainers     |
| Controller 接口测试 | 请求响应、统一异常、JSON 结构        | `@SpringBootTest`、`TestRestTemplate` |
| 手工接口验证        | 本地调试、联调确认                   | curl、Postman、Apifox                 |

实战案例完成后，整个 JDBC Template 文档已经覆盖了从基础配置、CRUD、映射、参数、事务、批量、异常到接口和测试的完整开发链路。

## 开发建议

JDBC Template 本身比较轻量，优点是直接、透明、可控；缺点是 SQL、参数、映射和异常处理都需要开发者自己维护。因此在实际项目中，应该通过统一的 SQL 管理方式、日志规范和分层规范降低后期维护成本。

### SQL 管理方式

SQL 管理的核心目标是可读、可查、可复用、可维护。小型项目可以直接将 SQL 写在 Repository 方法中；中大型项目建议按模块集中管理 SQL，避免 SQL 散落在 Service、Controller 或工具类中。

常见 SQL 管理方式如下：

| 管理方式                | 适用场景               | 优点                     | 缺点                   |
| ----------------------- | ---------------------- | ------------------------ | ---------------------- |
| Repository 内直接写 SQL | 简单 CRUD、小型项目    | 直观、跳转方便           | SQL 多了以后类会变长   |
| SQL 常量类              | 中等复杂度项目         | SQL 可复用、便于统一查看 | 动态 SQL 可读性一般    |
| SQL Provider 类         | 动态条件较多           | 拼接逻辑集中             | 需要控制复杂度         |
| 外部 `.sql` 文件        | 报表、复杂查询、长 SQL | SQL 可独立维护           | 加载、参数替换需要封装 |
| 引入 MyBatis            | SQL 数量非常多         | XML 管理成熟             | 技术栈变重             |

对于 JDBC Template 项目，推荐优先采用以下规则：

1. 简单 SQL 可以放在 Repository 方法内部。
2. 复用 SQL 或较长 SQL 放到独立常量类。
3. 动态 SQL 使用专门的条件构造方法，不要在业务代码中随意拼接。
4. 禁止在 Controller 中出现 SQL。
5. 禁止直接拼接用户输入，必须使用 `?` 或命名参数。
6. 复杂报表 SQL 可以独立放到 `resources/sql` 目录，并通过工具类读取。

#### Repository 内直接写 SQL

这种方式适合简单、短小、只在当前方法中使用的 SQL。SQL 与方法逻辑放在一起，阅读和调试都比较方便。

文件位置：`src/main/java/io/github/atengk/jdbc/repository/UserSimpleSqlRepository.java`

下面代码演示简单 SQL 直接写在 Repository 方法中的方式。

```java
package io.github.atengk.jdbc.repository;

import io.github.atengk.jdbc.entity.SysUser;
import io.github.atengk.jdbc.mapper.SysUserRowMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * 用户简单 SQL Repository
 *
 * @author Ateng
 * @since 2026-05-04
 */
@Repository
@RequiredArgsConstructor
public class UserSimpleSqlRepository {

    private final JdbcTemplate jdbcTemplate;

    private static final SysUserRowMapper USER_ROW_MAPPER = new SysUserRowMapper();

    /**
     * 查询启用用户列表
     *
     * @return 用户列表
     */
    public List<SysUser> findEnabledUsers() {
        String sql = """
                SELECT id, username, nickname, age, status, deleted, create_time, update_time
                FROM sys_user
                WHERE status = 1 AND deleted = 0
                ORDER BY id DESC
                """;
        return jdbcTemplate.query(sql, USER_ROW_MAPPER);
    }
}
```

#### SQL 常量类管理

当 SQL 被多个方法复用，或者 Repository 中 SQL 数量明显变多时，可以抽取 SQL 常量类。这样可以让 Repository 方法更聚焦参数绑定和执行逻辑。

文件位置：`src/main/java/io/github/atengk/jdbc/sql/SysUserSql.java`

下面代码集中管理用户模块常用 SQL。

```java
package io.github.atengk.jdbc.sql;

/**
 * 系统用户 SQL 常量
 *
 * @author Ateng
 * @since 2026-05-04
 */
public final class SysUserSql {

    private SysUserSql() {
    }

    /**
     * 用户基础查询字段
     */
    public static final String USER_COLUMNS = """
            id, username, nickname, age, status, deleted, create_time, update_time
            """;

    /**
     * 根据ID查询未删除用户
     */
    public static final String FIND_BY_ID = """
            SELECT id, username, nickname, age, status, deleted, create_time, update_time
            FROM sys_user
            WHERE id = ? AND deleted = 0
            """;

    /**
     * 新增用户
     */
    public static final String INSERT = """
            INSERT INTO sys_user (username, nickname, age, status, deleted)
            VALUES (?, ?, ?, ?, 0)
            """;

    /**
     * 更新用户状态
     */
    public static final String UPDATE_STATUS_BY_ID = """
            UPDATE sys_user
            SET status = ?
            WHERE id = ? AND deleted = 0
            """;

    /**
     * 逻辑删除用户
     */
    public static final String LOGIC_DELETE_BY_ID = """
            UPDATE sys_user
            SET deleted = 1
            WHERE id = ? AND deleted = 0
            """;
}
```

Repository 中使用 SQL 常量。

```java
package io.github.atengk.jdbc.repository;

import io.github.atengk.jdbc.entity.SysUser;
import io.github.atengk.jdbc.mapper.SysUserRowMapper;
import io.github.atengk.jdbc.sql.SysUserSql;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

/**
 * 用户 SQL 常量 Repository
 *
 * @author Ateng
 * @since 2026-05-04
 */
@Repository
@RequiredArgsConstructor
public class UserSqlConstantRepository {

    private final JdbcTemplate jdbcTemplate;

    private static final SysUserRowMapper USER_ROW_MAPPER = new SysUserRowMapper();

    /**
     * 根据ID查询用户
     *
     * @param id 用户ID
     * @return 用户信息
     */
    public SysUser findById(Long id) {
        return jdbcTemplate.queryForObject(SysUserSql.FIND_BY_ID, USER_ROW_MAPPER, id);
    }

    /**
     * 更新用户状态
     *
     * @param id 用户ID
     * @param status 用户状态
     * @return 影响行数
     */
    public int updateStatusById(Long id, Integer status) {
        return jdbcTemplate.update(SysUserSql.UPDATE_STATUS_BY_ID, status, id);
    }
}
```

#### 动态 SQL 管理

动态 SQL 不建议在 Service 中拼接。Service 只负责业务条件，Repository 或 SQL Provider 负责 SQL 构建。参数和 SQL 应该同步构建，避免 SQL 和参数顺序不一致。

文件位置：`src/main/java/io/github/atengk/jdbc/sql/SysUserSqlProvider.java`

下面代码使用简单的条件对象构建动态 SQL 和参数。

```java
package io.github.atengk.jdbc.sql;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.text.CharSequenceUtil;
import lombok.Getter;

import java.util.List;

/**
 * 系统用户 SQL 构造器
 *
 * @author Ateng
 * @since 2026-05-04
 */
public class SysUserSqlProvider {

    /**
     * 构建用户列表查询 SQL
     *
     * @param keyword 关键字
     * @param status 用户状态
     * @return SQL 构建结果
     */
    public SqlBuildResult buildListSql(String keyword, Integer status) {
        StringBuilder sql = new StringBuilder("""
                SELECT id, username, nickname, age, status, deleted, create_time, update_time
                FROM sys_user
                WHERE deleted = 0
                """);

        List<Object> params = CollUtil.newArrayList();

        if (CharSequenceUtil.isNotBlank(keyword)) {
            sql.append(" AND (username LIKE ? OR nickname LIKE ?) ");
            String likeKeyword = "%" + keyword + "%";
            params.add(likeKeyword);
            params.add(likeKeyword);
        }

        if (status != null) {
            sql.append(" AND status = ? ");
            params.add(status);
        }

        sql.append(" ORDER BY id DESC ");

        return new SqlBuildResult(sql.toString(), params.toArray());
    }

    /**
     * SQL 构建结果
     *
     * @author Ateng
     * @since 2026-05-04
     */
    @Getter
    public static class SqlBuildResult {

        /**
         * SQL语句
         */
        private final String sql;

        /**
         * SQL参数
         */
        private final Object[] params;

        /**
         * 创建 SQL 构建结果
         *
         * @param sql SQL语句
         * @param params SQL参数
         */
        public SqlBuildResult(String sql, Object[] params) {
            this.sql = sql;
            this.params = params;
        }
    }
}
```

使用动态 SQL 构造器的 Repository 示例。

```java
package io.github.atengk.jdbc.repository;

import io.github.atengk.jdbc.entity.SysUser;
import io.github.atengk.jdbc.mapper.SysUserRowMapper;
import io.github.atengk.jdbc.sql.SysUserSqlProvider;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * 用户动态 SQL Repository
 *
 * @author Ateng
 * @since 2026-05-04
 */
@Repository
@RequiredArgsConstructor
public class UserDynamicSqlRepository {

    private final JdbcTemplate jdbcTemplate;

    private static final SysUserRowMapper USER_ROW_MAPPER = new SysUserRowMapper();

    private final SysUserSqlProvider sysUserSqlProvider = new SysUserSqlProvider();

    /**
     * 查询用户列表
     *
     * @param keyword 关键字
     * @param status 用户状态
     * @return 用户列表
     */
    public List<SysUser> findList(String keyword, Integer status) {
        SysUserSqlProvider.SqlBuildResult result = sysUserSqlProvider.buildListSql(keyword, status);
        return jdbcTemplate.query(result.getSql(), USER_ROW_MAPPER, result.getParams());
    }
}
```

动态 SQL 建议控制复杂度。如果条件非常多、SQL 复用非常频繁，或者团队已经使用 MyBatis，那么复杂查询更适合交给 MyBatis XML 管理，不要把 JDBC Template 写成一个低配 SQL 框架。

### 日志输出规范

日志输出的目标是方便排查问题，而不是把所有数据都打印出来。JDBC Template 项目中，日志重点应该覆盖业务操作、SQL 执行结果、异常信息、批量处理数量和事务关键节点。

推荐日志级别如下：

| 日志级别 | 使用场景                           | 示例                                 |
| -------- | ---------------------------------- | ------------------------------------ |
| `debug`  | 开发阶段排查 SQL 参数、分支细节    | 查询条件、分页参数                   |
| `info`   | 正常业务关键节点                   | 新增成功、更新成功、批量完成         |
| `warn`   | 可恢复但需要关注的问题             | 查询结果为空、重复请求、业务状态异常 |
| `error`  | 系统异常、数据库异常、不可恢复异常 | SQL 异常、连接失败、事务失败         |

日志输出建议：

1. 新增、更新、删除操作记录业务主键和影响行数。
2. 批量操作记录提交数量、成功数量、批次大小。
3. 查询类方法一般不打印完整结果集，只打印查询条件和数量。
4. 异常日志必须包含关键业务参数和异常堆栈。
5. 不打印密码、Token、密钥、完整手机号、身份证号等敏感数据。
6. 不在循环中大量输出 `info` 日志，大批量处理可按批次输出。
7. SQL 参数日志只建议开发环境开启，生产环境谨慎打开。

#### 业务日志示例

文件位置：`src/main/java/io/github/atengk/jdbc/service/impl/UserLogExampleService.java`

下面代码演示新增、更新、删除、批量操作中的日志输出方式。

```java
package io.github.atengk.jdbc.service.impl;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.lang.Assert;
import io.github.atengk.jdbc.entity.SysUser;
import io.github.atengk.jdbc.repository.SysUserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * 用户日志示例 Service
 *
 * @author Ateng
 * @since 2026-05-04
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class UserLogExampleService {

    private final SysUserRepository sysUserRepository;

    /**
     * 更新用户状态
     *
     * @param id 用户ID
     * @param status 用户状态
     */
    public void updateUserStatus(Long id, Integer status) {
        Assert.notNull(id, "用户ID不能为空");
        Assert.notNull(status, "用户状态不能为空");

        log.debug("准备更新用户状态，id={}，status={}", id, status);

        int rows = sysUserRepository.updateStatusById(id, status);
        if (rows == 0) {
            log.warn("用户状态未更新，用户不存在或状态未变化，id={}，status={}", id, status);
            return;
        }

        log.info("用户状态更新成功，id={}，status={}，影响行数={}", id, status, rows);
    }

    /**
     * 查询用户列表
     *
     * @param keyword 关键字
     * @param status 状态
     * @return 用户列表
     */
    public List<SysUser> listUsers(String keyword, Integer status) {
        log.debug("准备查询用户列表，keyword={}，status={}", keyword, status);

        List<SysUser> users = sysUserRepository.findList(keyword, status);
        log.info("用户列表查询完成，keyword={}，status={}，数量={}",
                keyword, status, CollUtil.size(users));

        return users;
    }
}
```

#### SQL 日志配置建议

JDBC Template 默认不会像 MyBatis 那样直接打印完整 SQL 和参数。如果需要排查 SQL，可以通过日志级别、数据库审计、P6Spy、datasource-proxy 等方式实现。开发阶段可以适当提高 Spring JDBC 日志级别。

文件位置：`src/main/resources/application-dev.yml`

下面配置用于开发环境查看 JDBC 相关日志。

```yaml
logging:
  level:
    # 项目业务日志
    io.github.atengk: debug
    # Spring JDBC 核心日志，开发环境可打开
    org.springframework.jdbc.core: debug
    # Spring 事务日志，排查事务边界时可打开
    org.springframework.transaction: debug
```

生产环境建议降低日志级别，避免输出过多内部细节。

文件位置：`src/main/resources/application-prod.yml`

下面配置用于生产环境控制日志输出。

```yaml
logging:
  level:
    # 生产环境业务日志以 info 为主
    io.github.atengk: info
    # 生产环境不建议长期输出 JDBC debug 日志
    org.springframework.jdbc.core: info
    # 事务日志保持默认或 info
    org.springframework.transaction: info
```

#### 异常日志规范

异常日志需要区分业务异常和系统异常。业务异常通常是可预期的，例如用户不存在、用户名重复，使用 `info` 或 `warn` 即可；数据库连接失败、SQL 语法错误、未知异常应使用 `error` 并打印堆栈。

```java
/**
 * 处理用户创建异常
 *
 * @param username 用户名
 * @param exception 异常
 */
private void logCreateUserException(String username, Exception exception) {
    log.error("创建用户失败，username={}", username, exception);
}
```

不要这样输出异常，因为会丢失堆栈信息：

```java
log.error("创建用户失败，username={}，error={}", username, exception.getMessage());
```

建议这样输出异常：

```java
log.error("创建用户失败，username={}", username, exception);
```

### 代码分层规范

代码分层的核心目标是职责清晰。JDBC Template 项目不能因为直接写 SQL 就把所有逻辑堆在一个类中。推荐至少保持 Controller、Service、Repository、DTO、VO、Entity、Mapper、Common 这几类结构。

推荐目录结构如下：

```text
src/main/java/io/github/atengk/jdbc
├── common
│   ├── BusinessException.java
│   ├── CommonResult.java
│   ├── ErrorCode.java
│   └── JdbcExceptionResolver.java
├── config
│   └── JdbcConfig.java
├── controller
│   └── SysUserController.java
├── dto
│   ├── UserCreateRequest.java
│   └── UserUpdateStatusRequest.java
├── entity
│   └── SysUser.java
├── handler
│   └── GlobalExceptionHandler.java
├── mapper
│   └── SysUserRowMapper.java
├── repository
│   └── SysUserRepository.java
├── service
│   ├── SysUserService.java
│   └── impl
│       └── SysUserServiceImpl.java
├── sql
│   ├── SysUserSql.java
│   └── SysUserSqlProvider.java
└── vo
    └── UserResponse.java
```

各层职责建议如下：

| 层级       | 职责                         | 不建议做的事                       |
| ---------- | ---------------------------- | ---------------------------------- |
| Controller | 接收请求、返回响应           | 写 SQL、写复杂业务逻辑             |
| Service    | 参数校验、业务编排、事务控制 | 拼接复杂 SQL、直接处理 ResultSet   |
| Repository | SQL 执行、参数绑定、结果映射 | 处理复杂业务规则、返回接口响应对象 |
| Entity     | 对应数据库表结构             | 混入页面展示字段                   |
| DTO        | 接收请求参数                 | 直接作为数据库实体入库             |
| VO         | 返回接口展示数据             | 参与数据库持久化                   |
| RowMapper  | 处理 ResultSet 映射          | 写业务规则                         |
| Common     | 通用响应、异常、错误码       | 放具体业务 SQL                     |

#### Controller 层规范

Controller 只做请求入口，不写业务细节，不捕获数据库异常，不返回数据库实体。

```java
package io.github.atengk.jdbc.controller;

import io.github.atengk.jdbc.common.CommonResult;
import io.github.atengk.jdbc.dto.UserCreateRequest;
import io.github.atengk.jdbc.service.SysUserService;
import io.github.atengk.jdbc.vo.UserResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 用户分层示例 Controller
 *
 * @author Ateng
 * @since 2026-05-04
 */
@RestController
@RequestMapping("/api/layer/users")
@RequiredArgsConstructor
public class UserLayerController {

    private final SysUserService sysUserService;

    /**
     * 创建用户
     *
     * @param request 创建请求
     * @return 用户ID
     */
    @PostMapping
    public CommonResult<Long> createUser(@RequestBody UserCreateRequest request) {
        return CommonResult.success(sysUserService.createUser(request));
    }

    /**
     * 查询用户列表
     *
     * @param keyword 关键字
     * @param status 状态
     * @return 用户列表
     */
    @GetMapping
    public CommonResult<List<UserResponse>> listUsers(
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) Integer status) {
        return CommonResult.success(sysUserService.listUsers(keyword, status));
    }
}
```

#### Service 层规范

Service 负责业务语义、事务边界和对象转换。增删改方法建议配置 `@Transactional(rollbackFor = Exception.class)`，查询方法可以不加事务，或者根据项目规范添加只读事务。

```java
package io.github.atengk.jdbc.service.impl;

import cn.hutool.core.lang.Assert;
import cn.hutool.core.text.CharSequenceUtil;
import io.github.atengk.jdbc.dto.UserCreateRequest;
import io.github.atengk.jdbc.entity.SysUser;
import io.github.atengk.jdbc.repository.SysUserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 用户分层示例 Service
 *
 * @author Ateng
 * @since 2026-05-04
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class UserLayerService {

    private final SysUserRepository sysUserRepository;

    /**
     * 创建用户
     *
     * @param request 创建请求
     * @return 用户ID
     */
    @Transactional(rollbackFor = Exception.class)
    public Long createUser(UserCreateRequest request) {
        Assert.notNull(request, "创建请求不能为空");
        Assert.isTrue(CharSequenceUtil.isNotBlank(request.getUsername()), "用户名不能为空");
        Assert.isTrue(CharSequenceUtil.isNotBlank(request.getNickname()), "昵称不能为空");

        SysUser user = new SysUser();
        user.setUsername(request.getUsername());
        user.setNickname(request.getNickname());
        user.setAge(request.getAge());
        user.setStatus(1);

        sysUserRepository.insert(user);
        log.info("用户创建成功，username={}", request.getUsername());

        return null;
    }
}
```

上面的 `return null` 只用于说明分层职责。实际项目中应使用 `KeyHolder` 返回自增 ID，或者由业务生成 ID 后再写入数据库。

#### Repository 层规范

Repository 层负责 SQL，不返回 `CommonResult`，不依赖 Controller，不处理 HTTP 语义。Repository 可以抛出 `BusinessException`，但只建议用于数据库结果能够直接表达的业务异常，例如数据不存在、唯一键冲突。

```java
package io.github.atengk.jdbc.repository;

import io.github.atengk.jdbc.entity.SysUser;
import io.github.atengk.jdbc.mapper.SysUserRowMapper;
import io.github.atengk.jdbc.sql.SysUserSql;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

/**
 * 用户分层示例 Repository
 *
 * @author Ateng
 * @since 2026-05-04
 */
@Repository
@RequiredArgsConstructor
public class UserLayerRepository {

    private final JdbcTemplate jdbcTemplate;

    private static final SysUserRowMapper USER_ROW_MAPPER = new SysUserRowMapper();

    /**
     * 根据ID查询用户
     *
     * @param id 用户ID
     * @return 用户信息
     */
    public SysUser findById(Long id) {
        return jdbcTemplate.queryForObject(SysUserSql.FIND_BY_ID, USER_ROW_MAPPER, id);
    }
}
```

#### Entity、DTO、VO 分离

不要用一个类贯穿数据库、接口入参、接口出参。长期来看，这会导致字段污染、安全风险和版本维护困难。

推荐规则：

1. `Entity` 对应数据库表字段。
2. `DTO` 对应接口请求参数。
3. `VO` 对应接口响应字段。
4. 不在 `Entity` 中增加仅前端展示用的字段。
5. 不让前端直接传入完整 `Entity`，避免越权修改状态、删除标记等字段。
6. 对象转换可以手写，也可以使用 MapStruct、BeanUtil 等工具。

使用 Hutool `BeanUtil` 做简单对象转换示例。

```java
package io.github.atengk.jdbc.converter;

import cn.hutool.core.bean.BeanUtil;
import io.github.atengk.jdbc.entity.SysUser;
import io.github.atengk.jdbc.vo.UserResponse;

/**
 * 用户对象转换器
 *
 * @author Ateng
 * @since 2026-05-04
 */
public final class UserConverter {

    private UserConverter() {
    }

    /**
     * 转换用户响应对象
     *
     * @param user 用户实体
     * @return 用户响应对象
     */
    public static UserResponse toResponse(SysUser user) {
        UserResponse response = BeanUtil.copyProperties(user, UserResponse.class);
        response.setStatusName(convertStatusName(user.getStatus()));
        return response;
    }

    /**
     * 转换状态名称
     *
     * @param status 状态
     * @return 状态名称
     */
    private static String convertStatusName(Integer status) {
        if (status == null) {
            return "未知";
        }
        return switch (status) {
            case 0 -> "禁用";
            case 1 -> "启用";
            default -> "未知";
        };
    }
}
```

#### 分层调用链路

一次标准请求的调用链路应保持如下方向：

```text
HTTP Request
    ↓
Controller
    ↓
Service
    ↓
Repository
    ↓
JdbcTemplate
    ↓
Database
```

返回链路如下：

```text
Database ResultSet
    ↓
RowMapper / BeanPropertyRowMapper
    ↓
Entity
    ↓
Service 转换
    ↓
VO
    ↓
CommonResult
    ↓
HTTP Response
```

不要出现下面这些反向依赖：

```text
Repository 依赖 Controller
Repository 返回 CommonResult
Service 直接处理 ResultSet
Controller 直接使用 JdbcTemplate
Entity 直接作为新增接口入参
```

### 开发建议总结

JDBC Template 项目的质量主要取决于工程规范，而不是框架能力本身。建议在项目初期就约定以下规则：

1. SQL 统一放在 Repository 或 SQL 常量类中。
2. Controller 只处理请求和响应，不写 SQL。
3. Service 负责事务、校验、业务流程和对象转换。
4. Repository 负责 SQL 执行、参数绑定和结果映射。
5. 所有用户输入必须使用占位符或命名参数绑定。
6. 查询字段显式列出，不长期使用 `SELECT *`。
7. 单条查询必须处理数据不存在场景。
8. 增删改操作必须检查影响行数。
9. 批量操作必须控制批次大小，并记录提交数量和成功数量。
10. 异常由全局异常处理器统一返回，避免暴露数据库内部信息。
11. 日志记录关键参数和影响行数，不记录敏感数据。
12. Entity、DTO、VO 分离，避免接口和数据库模型互相污染。
