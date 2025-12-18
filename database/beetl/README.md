# Beetl

Beetl 是一个高性能、强表达能力的 Java 模板引擎，非常适合生成 SQL、DDL、代码等“结构化文本”。



## 基础配置

### 添加依赖

```xml
    <!-- 项目属性 -->
    <properties>
        <java.version>21</java.version>
        <project.build.sourceEncoding>UTF-8</project.build.sourceEncoding>
        <project.reporting.outputEncoding>UTF-8</project.reporting.outputEncoding>
        <maven-compiler.version>3.14.1</maven-compiler.version>
        <spring-boot.version>3.5.7</spring-boot.version>
        <lombok.version>1.18.42</lombok.version>
        <hutool.version>5.8.35</hutool.version>
        <beetl.version>3.20.0.RELEASE</beetl.version>
    </properties>
    <!-- 项目依赖 -->
    <dependencies>
        <!-- Spring Boot Web Starter: 包含用于构建Web应用程序的Spring Boot依赖项 -->
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-web</artifactId>
        </dependency>

        <!-- Spring Boot Starter Test: 包含用于测试Spring Boot应用程序的依赖项 -->
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-test</artifactId>
            <scope>test</scope>
        </dependency>

        <!-- Lombok: 简化Java代码编写的依赖项 -->
        <!-- https://mvnrepository.com/artifact/org.projectlombok/lombok -->
        <dependency>
            <groupId>org.projectlombok</groupId>
            <artifactId>lombok</artifactId>
            <version>${lombok.version}</version>
            <scope>provided</scope>
        </dependency>

        <!-- Hutool: Java工具库，提供了许多实用的工具方法 -->
        <dependency>
            <groupId>cn.hutool</groupId>
            <artifactId>hutool-all</artifactId>
            <version>${hutool.version}</version>
        </dependency>

        <!-- MySQL数据库驱动 -->
        <dependency>
            <groupId>com.mysql</groupId>
            <artifactId>mysql-connector-j</artifactId>
        </dependency>

        <!-- Postgresql数据库驱动 -->
        <dependency>
            <groupId>org.postgresql</groupId>
            <artifactId>postgresql</artifactId>
        </dependency>

        <!-- HikariCP 数据源 依赖 -->
        <dependency>
            <groupId>com.zaxxer</groupId>
            <artifactId>HikariCP</artifactId>
        </dependency>

        <!-- Spring JDBC 数据库框架 -->
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-jdbc</artifactId>
        </dependency>

       <!-- Beetl 是一个高性能、强表达能力的 Java 模板引擎，非常适合生成 SQL、DDL、代码等“结构化文本”。 -->
        <dependency>
            <groupId>com.ibeetl</groupId>
            <artifactId>beetl</artifactId>
            <version>${beetl.version}</version>
        </dependency>

    </dependencies>
```

### 添加配置

```yaml
---
# 数据库的相关配置
spring:
  datasource:
    mysql:
      jdbc-url: jdbc:mysql://47.108.128.105:20001/kongyu
      username: kongyu
      password: kongyu
      driver-class-name: com.mysql.cj.jdbc.Driver
      maximum-pool-size: 20
      minimum-idle: 5
      connection-timeout: 30000
      idle-timeout: 600000
      max-lifetime: 1800000
      pool-name: MysqlHikariPool
      validation-timeout: 5000
    postgresql:
      jdbc-url: jdbc:postgresql://47.108.128.105:20002/kongyu?currentSchema=public&stringtype=unspecified
      username: kongyu
      password: kongyu
      driver-class-name: org.postgresql.Driver
      maximum-pool-size: 20
      minimum-idle: 5
      connection-timeout: 30000
      idle-timeout: 600000
      max-lifetime: 1800000
      pool-name: PostgresHikariPool
      validation-timeout: 5000
```

### 多数据源配置类

```java
package io.github.atengk.beetl.config;

import com.zaxxer.hikari.HikariDataSource;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import javax.sql.DataSource;

/**
 * 多数据源配置类
 * 用于创建主从两个 DataSource。
 */
@Configuration
public class DataSourceConfig {

    /**
     * MySQL 数据源
     *
     * @return DataSource
     */
    @Bean(name = "mysqlDataSource")
    @ConfigurationProperties(prefix = "spring.datasource.mysql")
    public DataSource mysqlDataSource() {
        return new HikariDataSource();
    }

    /**
     * PostgreSQL 数据源
     *
     * @return DataSource
     */
    @Bean(name = "postgresqlDataSource")
    @ConfigurationProperties(prefix = "spring.datasource.postgresql")
    public DataSource postgresqlDataSource() {
        return new HikariDataSource();
    }
}



```

### 多 JdbcTemplate 配置类

```java
package io.github.atengk.beetl.config;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.jdbc.core.JdbcTemplate;

import javax.sql.DataSource;

/**
 * 多 JdbcTemplate 配置类
 * 通过 @Qualifier 精准绑定不同的数据源。
 */
@Configuration
public class JdbcTemplateConfig {

    /**
     * mysql JdbcTemplate
     *
     * @param dataSource 主数据源
     * @return JdbcTemplate
     */
    @Bean(name = "mysqlJdbcTemplate")
    public JdbcTemplate mysqlJdbcTemplate(
            @Qualifier("mysqlDataSource") DataSource dataSource) {
        return new JdbcTemplate(dataSource);
    }

    /**
     * postgresql JdbcTemplate
     *
     * @param dataSource 次数据源
     * @return JdbcTemplate
     */
    @Bean(name = "postgresqlJdbcTemplate")
    public JdbcTemplate postgresqlJdbcTemplate(
            @Qualifier("postgresqlDataSource") DataSource dataSource) {
        return new JdbcTemplate(dataSource);
    }
}


```



## Beetl配置

### 创建tbl模版

#### MySQL

resources/ddl/mysql/create_table.tbl

```
CREATE TABLE
<% if(table.ifNotExists != null && table.ifNotExists){ %>
IF NOT EXISTS
<% } %>
${table.tableName} (
<%
/* ================= 字段定义 ================= */
for(col in table.columns){
%>
    ${col.name}
    <% /* ---------- 类型 ---------- */ %>
    ${col.type}
    <% if(col.length != null){ %>(${col.length})<% } %>
    <% if(col.precision != null && col.scale != null){ %>(${col.precision},${col.scale})<% } %>
    <% if(col.unsigned != null && col.unsigned){ %> UNSIGNED<% } %>

    <% /* ---------- 约束 ---------- */ %>
    <% if(col.nullable != null && !col.nullable){ %> NOT NULL<% } %>
    <% if(col.unique != null && col.unique){ %> UNIQUE<% } %>
    <% if(col.autoIncrement != null && col.autoIncrement){ %> AUTO_INCREMENT<% } %>

    <% /* ---------- 默认值 ---------- */ %>
    <% if(col.defaultValue != null){ %> DEFAULT ${col.defaultValue}<% } %>
    <% if(col.onUpdateCurrentTimestamp != null && col.onUpdateCurrentTimestamp){ %> ON UPDATE CURRENT_TIMESTAMP<% } %>

    <% /* ---------- 注释 ---------- */ %>
    <% if(col.comment != null){ %> COMMENT '${col.comment}'<% } %>

    <% if(!colLP.last){ %>,<% } %>
<%
}
%>

<%
/* ================= 主键 ================= */
var hasPk = false;
for(col in table.columns){
    if(col.primaryKey != null && col.primaryKey){
        hasPk = true;
        break;
    }
}
if(hasPk){
%>,
    PRIMARY KEY (
    <%
    var first = true;
    for(col in table.columns){
        if(col.primaryKey != null && col.primaryKey){
            if(!first){ %>, <% }
            %>${col.name}<%
            first = false;
        }
    }
    %>
    )
<%
}
%>
)
<% /* ================= 表属性 ================= */ %>
<% if(table.engine != null){ %> ENGINE=${table.engine}<% } %>
<% if(table.charset != null){ %> DEFAULT CHARSET=${table.charset}<% } %>
<% if(table.collation != null){ %> COLLATE=${table.collation}<% } %>
<% if(table.comment != null){ %> COMMENT='${table.comment}'<% } %>;

```

#### PostgreSQL

resources/ddl/postgresql/create_table.tbl

```
CREATE TABLE
<% if(table.ifNotExists != null && table.ifNotExists){ %>
IF NOT EXISTS
<% } %>
${table.tableName} (
<%
/* ================= 字段定义 ================= */
for(col in table.columns){
%>
    ${col.name}
    <% /* ---------- 类型 ---------- */ %>
    ${col.type}
    <% if(col.length != null){ %>(${col.length})<% } %>
    <% if(col.precision != null && col.scale != null){ %>(${col.precision},${col.scale})<% } %>

    <% /* ---------- 自增（PostgreSQL） ---------- */ %>
    <% if(col.autoIncrement != null && col.autoIncrement){ %>
     GENERATED BY DEFAULT AS IDENTITY
    <% } %>

    <% /* ---------- 约束 ---------- */ %>
    <% if(col.nullable != null && !col.nullable){ %> NOT NULL<% } %>
    <% if(col.unique != null && col.unique){ %> UNIQUE<% } %>

    <% /* ---------- 默认值 ---------- */ %>
    <% if(col.defaultValue != null){ %> DEFAULT ${col.defaultValue}<% } %>

    <% /* ---------- 注释（PostgreSQL 不支持列内 COMMENT） ---------- */ %>

    <% if(!colLP.last){ %>,<% } %>
<%
}
%>

<%
/* ================= 主键 ================= */
var hasPk = false;
for(col in table.columns){
    if(col.primaryKey != null && col.primaryKey){
        hasPk = true;
        break;
    }
}
if(hasPk){
%>,
    PRIMARY KEY (
    <%
    var first = true;
    for(col in table.columns){
        if(col.primaryKey != null && col.primaryKey){
            if(!first){ %>, <% }
            %>${col.name}<%
            first = false;
        }
    }
    %>
    )
<%
}
%>
);

<%
/* ================= 表注释 ================= */
if(table.comment != null){
%>
COMMENT ON TABLE ${table.tableName} IS '${table.comment}';
<%
}

/* ================= 字段注释 ================= */
for(col in table.columns){
    if(col.comment != null){
%>
COMMENT ON COLUMN ${table.tableName}.${col.name} IS '${col.comment}';
<%
    }
}
%>

```



### 创建实体类

#### ColumnMeta

```java
package io.github.atengk.beetl.model;

import lombok.Data;

/**
 * 表字段定义
 *
 * @author 孔余
 * @since 2025-12-18
 */
@Data
public class ColumnMeta {

    /**
     * 字段名
     */
    private String name;

    /**
     * 字段类型（varchar / bigint / decimal 等）
     */
    private String type;

    /**
     * 长度（varchar、char）
     */
    private Integer length;

    /**
     * 精度（decimal）
     */
    private Integer precision;

    /**
     * 小数位（decimal）
     */
    private Integer scale;

    /**
     * 是否无符号（int / bigint）
     */
    private Boolean unsigned;

    /**
     * 是否主键
     */
    private Boolean primaryKey;

    /**
     * 是否自增
     */
    private Boolean autoIncrement;

    /**
     * 是否唯一
     */
    private Boolean unique;

    /**
     * 是否允许为空
     */
    private Boolean nullable;

    /**
     * 默认值（不自动加引号）
     */
    private String defaultValue;

    /**
     * 是否 ON UPDATE CURRENT_TIMESTAMP
     */
    private Boolean onUpdateCurrentTimestamp;

    /**
     * 字段备注
     */
    private String comment;
}

```

#### TableMeta

```java
package io.github.atengk.beetl.model;

import lombok.Data;

import java.util.List;

/**
 * 表结构定义
 */
@Data
public class TableMeta {

    /**
     * 表名
     */
    private String tableName;

    /**
     * 是否忽略表已存在（CREATE TABLE IF NOT EXISTS）
     */
    private Boolean ifNotExists;

    /**
     * 表备注
     */
    private String comment;

    /**
     * 存储引擎
     */
    private String engine;

    /**
     * 字符集
     */
    private String charset;

    /**
     * 排序规则
     */
    private String collation;

    /**
     * 字段列表
     */
    private List<ColumnMeta> columns;
}

```

### 创建DDL 服务

```java
package io.github.atengk.beetl.service;

import cn.hutool.db.sql.SqlUtil;
import io.github.atengk.beetl.enums.DatabaseType;
import io.github.atengk.beetl.model.TableMeta;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.beetl.core.GroupTemplate;
import org.beetl.core.Template;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.SQLException;

/**
 * 动态 DDL 服务
 *
 * @author 孔余
 * @since 2025-12-18
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class DynamicDdlService {


    private final GroupTemplate groupTemplate;

    /**
     * 创建表
     *
     * @param dataSource 数据源
     * @param tableMeta  表结构
     */
    public void createTable(DataSource dataSource, TableMeta tableMeta) {
        DatabaseType databaseType = resolveDatabaseType(dataSource);
        String templatePath = resolveCreateTableTemplate(databaseType);

        Template template = groupTemplate.getTemplate(templatePath);
        template.binding("table", tableMeta);

        String ddl = template.render();
        log.info("创建表DDL [{}]:\n{}", databaseType, SqlUtil.formatSql(ddl));

        JdbcTemplate jdbcTemplate = new JdbcTemplate(dataSource);
        jdbcTemplate.execute(ddl);
    }

    /**
     * 解析数据库类型
     *
     * @param dataSource 数据源
     * @return 数据库类型
     */
    private DatabaseType resolveDatabaseType(DataSource dataSource) {
        try (Connection connection = dataSource.getConnection()) {
            DatabaseMetaData metaData = connection.getMetaData();
            return DatabaseType.fromProductName(metaData.getDatabaseProductName());
        } catch (SQLException e) {
            throw new IllegalStateException("Failed to resolve database type", e);
        }
    }

    /**
     * 获取建表模板路径
     *
     * @param databaseType 数据库类型
     * @return 模板路径
     */
    private String resolveCreateTableTemplate(DatabaseType databaseType) {
        switch (databaseType) {
            case MYSQL:
                return "ddl/mysql/create_table.tbl";
            case POSTGRESQL:
                return "ddl/postgresql/create_table.tbl";
            default:
                throw new UnsupportedOperationException(
                        "Create table not supported for database type: " + databaseType
                );
        }
    }
}

```



## 使用

### MySQL

```java
package io.github.atengk.beetl;

import io.github.atengk.beetl.model.ColumnMeta;
import io.github.atengk.beetl.model.TableMeta;
import io.github.atengk.beetl.service.DynamicDdlService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.test.context.SpringBootTest;

import javax.sql.DataSource;
import java.util.List;

@SpringBootTest
public class DatabaseBeetlMySQLTests {

    @Autowired
    private DynamicDdlService dynamicDdlService;
    @Qualifier("mysqlDataSource")
    @Autowired
    DataSource dataSource;

    /**
     * 常用字段创建示例
     * CREATE TABLE t_demo_all_columns
     * (
     *     id          bigint UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '主键ID',
     *     biz_no      varchar(64)     NOT NULL UNIQUE COMMENT '业务编号',
     *     name        varchar(128)    NOT NULL COMMENT '名称',
     *     status      tinyint         NOT NULL DEFAULT 0 COMMENT '状态：0-禁用 1-启用',
     *     amount      decimal(10, 2)  NOT NULL DEFAULT 0.00 COMMENT '金额',
     *     remark      text COMMENT '备注',
     *     create_time datetime        NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
     *     update_time datetime        NOT NULL
     *         ON UPDATE
     *             CURRENT_TIMESTAMP COMMENT '更新时间',
     *     deleted     tinyint         NOT NULL DEFAULT 0 COMMENT '逻辑删除标识',
     *     PRIMARY KEY (id)
     * ) ENGINE = InnoDB
     *   DEFAULT CHARSET = utf8mb4
     *   COLLATE = utf8mb4_general_ci COMMENT ='字段能力对照表';
     */
    @Test
    public void testCreateTable_AllCommonColumns() {

        TableMeta table = new TableMeta();
        table.setTableName("t_demo_all_columns");
        table.setIfNotExists(true);
        table.setComment("字段能力对照表");
        table.setEngine("InnoDB");
        table.setCharset("utf8mb4");
        table.setCollation("utf8mb4_general_ci");

        ColumnMeta id = new ColumnMeta();
        id.setName("id");
        id.setType("bigint");
        id.setUnsigned(true);
        id.setPrimaryKey(true);
        id.setAutoIncrement(true);
        id.setNullable(false);
        id.setComment("主键ID");

        ColumnMeta bizNo = new ColumnMeta();
        bizNo.setName("biz_no");
        bizNo.setType("varchar");
        bizNo.setLength(64);
        bizNo.setUnique(true);
        bizNo.setNullable(false);
        bizNo.setComment("业务编号");

        ColumnMeta name = new ColumnMeta();
        name.setName("name");
        name.setType("varchar");
        name.setLength(128);
        name.setNullable(false);
        name.setComment("名称");

        ColumnMeta status = new ColumnMeta();
        status.setName("status");
        status.setType("tinyint");
        status.setNullable(false);
        status.setDefaultValue("0");
        status.setComment("状态：0-禁用 1-启用");

        ColumnMeta amount = new ColumnMeta();
        amount.setName("amount");
        amount.setType("decimal");
        amount.setPrecision(10);
        amount.setScale(2);
        amount.setNullable(false);
        amount.setDefaultValue("0.00");
        amount.setComment("金额");

        ColumnMeta remark = new ColumnMeta();
        remark.setName("remark");
        remark.setType("text");
        remark.setNullable(true);
        remark.setComment("备注");

        ColumnMeta createTime = new ColumnMeta();
        createTime.setName("create_time");
        createTime.setType("datetime");
        createTime.setNullable(false);
        createTime.setDefaultValue("CURRENT_TIMESTAMP");
        createTime.setComment("创建时间");

        ColumnMeta updateTime = new ColumnMeta();
        updateTime.setName("update_time");
        updateTime.setType("datetime");
        updateTime.setNullable(false);
        updateTime.setOnUpdateCurrentTimestamp(true);
        updateTime.setComment("更新时间");

        ColumnMeta deleted = new ColumnMeta();
        deleted.setName("deleted");
        deleted.setType("tinyint");
        deleted.setNullable(false);
        deleted.setDefaultValue("0");
        deleted.setComment("逻辑删除标识");

        table.setColumns(List.of(
                id,
                bizNo,
                name,
                status,
                amount,
                remark,
                createTime,
                updateTime,
                deleted
        ));

        dynamicDdlService.createTable(dataSource, table);
    }

    /**
     * 联合主键创建示例
     * CREATE TABLE t_demo_order_item
     * (
     *     order_id     bigint UNSIGNED NOT NULL COMMENT '订单ID',
     *     item_id      bigint UNSIGNED NOT NULL COMMENT '明细ID',
     *     product_code varchar(64)     NOT NULL COMMENT '商品编码',
     *     quantity     int             NOT NULL DEFAULT 1 COMMENT '数量',
     *     create_time  datetime        NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
     *     PRIMARY KEY (order_id, item_id)
     * ) ENGINE = InnoDB
     *   DEFAULT CHARSET = utf8mb4
     *   COLLATE = utf8mb4_general_ci COMMENT ='订单明细（联合主键示例）';
     */
    @Test
    public void testCreateTable_CompositePrimaryKey() {

        TableMeta table = new TableMeta();
        table.setTableName("t_demo_order_item");
        table.setIfNotExists(true);
        table.setComment("订单明细（联合主键示例）");
        table.setEngine("InnoDB");
        table.setCharset("utf8mb4");
        table.setCollation("utf8mb4_general_ci");

        ColumnMeta orderId = new ColumnMeta();
        orderId.setName("order_id");
        orderId.setType("bigint");
        orderId.setUnsigned(true);
        orderId.setPrimaryKey(true);
        orderId.setNullable(false);
        orderId.setComment("订单ID");

        ColumnMeta itemId = new ColumnMeta();
        itemId.setName("item_id");
        itemId.setType("bigint");
        itemId.setUnsigned(true);
        itemId.setPrimaryKey(true);
        itemId.setNullable(false);
        itemId.setComment("明细ID");

        ColumnMeta productCode = new ColumnMeta();
        productCode.setName("product_code");
        productCode.setType("varchar");
        productCode.setLength(64);
        productCode.setNullable(false);
        productCode.setComment("商品编码");

        ColumnMeta quantity = new ColumnMeta();
        quantity.setName("quantity");
        quantity.setType("int");
        quantity.setNullable(false);
        quantity.setDefaultValue("1");
        quantity.setComment("数量");

        ColumnMeta createTime = new ColumnMeta();
        createTime.setName("create_time");
        createTime.setType("datetime");
        createTime.setNullable(false);
        createTime.setDefaultValue("CURRENT_TIMESTAMP");
        createTime.setComment("创建时间");

        table.setColumns(List.of(
                orderId,
                itemId,
                productCode,
                quantity,
                createTime
        ));

        dynamicDdlService.createTable(dataSource, table);
    }

    /**
     * 创建JSON字段表
     * CREATE TABLE t_demo_json
     * (
     *     id          bigint UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '主键',
     *     config      json            NOT NULL COMMENT '配置信息(JSON)',
     *     ext         json COMMENT '扩展字段',
     *     create_time datetime        NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
     *     PRIMARY KEY (id)
     * ) ENGINE = InnoDB
     *   DEFAULT CHARSET = utf8mb4
     *   COLLATE = utf8mb4_general_ci COMMENT ='JSON 字段示例';
     */
    @Test
    public void testCreateTable_WithJsonColumn() {

        TableMeta table = new TableMeta();
        table.setTableName("t_demo_json");
        table.setIfNotExists(true);
        table.setComment("JSON 字段示例");
        table.setEngine("InnoDB");
        table.setCharset("utf8mb4");
        table.setCollation("utf8mb4_general_ci");

        ColumnMeta id = new ColumnMeta();
        id.setName("id");
        id.setType("bigint");
        id.setUnsigned(true);
        id.setPrimaryKey(true);
        id.setAutoIncrement(true);
        id.setNullable(false);
        id.setComment("主键");

        ColumnMeta config = new ColumnMeta();
        config.setName("config");
        config.setType("json");
        config.setNullable(false);
        config.setComment("配置信息(JSON)");

        ColumnMeta ext = new ColumnMeta();
        ext.setName("ext");
        ext.setType("json");
        ext.setNullable(true);
        ext.setComment("扩展字段");

        ColumnMeta createTime = new ColumnMeta();
        createTime.setName("create_time");
        createTime.setType("datetime");
        createTime.setNullable(false);
        createTime.setDefaultValue("CURRENT_TIMESTAMP");
        createTime.setComment("创建时间");

        table.setColumns(List.of(
                id,
                config,
                ext,
                createTime
        ));

        dynamicDdlService.createTable(dataSource, table);
    }


}

```

### PostgreSQL

```java
package io.github.atengk.beetl;

import io.github.atengk.beetl.model.ColumnMeta;
import io.github.atengk.beetl.model.TableMeta;
import io.github.atengk.beetl.service.DynamicDdlService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.test.context.SpringBootTest;

import javax.sql.DataSource;
import java.util.List;

@SpringBootTest
public class DatabaseBeetlPostgreSQLTests {

    @Autowired
    private DynamicDdlService dynamicDdlService;
    @Qualifier("postgresqlDataSource")
    @Autowired
    DataSource dataSource;

    /**
     * 常用字段创建示例
     * CREATE TABLE t_demo_all_columns
     * (
     *     id          bigint UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '主键ID',
     *     biz_no      varchar(64)     NOT NULL UNIQUE COMMENT '业务编号',
     *     name        varchar(128)    NOT NULL COMMENT '名称',
     *     status      tinyint         NOT NULL DEFAULT 0 COMMENT '状态：0-禁用 1-启用',
     *     amount      decimal(10, 2)  NOT NULL DEFAULT 0.00 COMMENT '金额',
     *     remark      text COMMENT '备注',
     *     create_time datetime        NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
     *     update_time datetime        NOT NULL
     *         ON UPDATE
     *             CURRENT_TIMESTAMP COMMENT '更新时间',
     *     deleted     tinyint         NOT NULL DEFAULT 0 COMMENT '逻辑删除标识',
     *     PRIMARY KEY (id)
     * ) ENGINE = InnoDB
     *   DEFAULT CHARSET = utf8mb4
     *   COLLATE = utf8mb4_general_ci COMMENT ='字段能力对照表';
     */
    @Test
    public void testCreateTable_AllCommonColumns_Pgsql() {

        TableMeta table = new TableMeta();
        table.setTableName("t_demo_all_columns");
        table.setIfNotExists(true);
        table.setComment("字段能力对照表");

        ColumnMeta id = new ColumnMeta();
        id.setName("id");
        id.setType("bigint");
        id.setPrimaryKey(true);
        id.setAutoIncrement(true);
        id.setNullable(false);
        id.setComment("主键ID");

        ColumnMeta bizNo = new ColumnMeta();
        bizNo.setName("biz_no");
        bizNo.setType("varchar");
        bizNo.setLength(64);
        bizNo.setUnique(true);
        bizNo.setNullable(false);
        bizNo.setComment("业务编号");

        ColumnMeta name = new ColumnMeta();
        name.setName("name");
        name.setType("varchar");
        name.setLength(128);
        name.setNullable(false);
        name.setComment("名称");

        ColumnMeta status = new ColumnMeta();
        status.setName("status");
        status.setType("smallint");
        status.setNullable(false);
        status.setDefaultValue("0");
        status.setComment("状态：0-禁用 1-启用");

        ColumnMeta amount = new ColumnMeta();
        amount.setName("amount");
        amount.setType("numeric");
        amount.setPrecision(10);
        amount.setScale(2);
        amount.setNullable(false);
        amount.setDefaultValue("0.00");
        amount.setComment("金额");

        ColumnMeta remark = new ColumnMeta();
        remark.setName("remark");
        remark.setType("text");
        remark.setNullable(true);
        remark.setComment("备注");

        ColumnMeta createTime = new ColumnMeta();
        createTime.setName("create_time");
        createTime.setType("timestamp");
        createTime.setNullable(false);
        createTime.setDefaultValue("CURRENT_TIMESTAMP");
        createTime.setComment("创建时间");

        ColumnMeta updateTime = new ColumnMeta();
        updateTime.setName("update_time");
        updateTime.setType("timestamp");
        updateTime.setNullable(false);
        updateTime.setDefaultValue("CURRENT_TIMESTAMP");
        updateTime.setComment("更新时间");

        ColumnMeta deleted = new ColumnMeta();
        deleted.setName("deleted");
        deleted.setType("smallint");
        deleted.setNullable(false);
        deleted.setDefaultValue("0");
        deleted.setComment("逻辑删除标识");

        table.setColumns(List.of(
                id,
                bizNo,
                name,
                status,
                amount,
                remark,
                createTime,
                updateTime,
                deleted
        ));

        dynamicDdlService.createTable(dataSource, table);
    }

    /**
     * 联合主键创建示例
     * CREATE TABLE IF NOT EXISTS t_demo_order_item
     * (
     *     order_id     bigint      NOT NULL,
     *     item_id      bigint      NOT NULL,
     *     product_code varchar(64) NOT NULL,
     *     quantity     int         NOT NULL DEFAULT 1,
     *     create_time  timestamp   NOT NULL DEFAULT CURRENT_TIMESTAMP,
     *     PRIMARY KEY (order_id, item_id)
     * );
     * COMMENT
     *     ON TABLE t_demo_order_item IS '订单明细（联合主键示例）';
     * COMMENT
     *     ON COLUMN t_demo_order_item.order_id IS '订单ID';
     * COMMENT
     *     ON COLUMN t_demo_order_item.item_id IS '明细ID';
     * COMMENT
     *     ON COLUMN t_demo_order_item.product_code IS '商品编码';
     * COMMENT
     *     ON COLUMN t_demo_order_item.quantity IS '数量';
     * COMMENT
     *     ON COLUMN t_demo_order_item.create_time IS '创建时间';
     */
    @Test
    public void testCreateTable_CompositePrimaryKey() {

        TableMeta table = new TableMeta();
        table.setTableName("t_demo_order_item");
        table.setIfNotExists(true);
        table.setComment("订单明细（联合主键示例）");
        

        ColumnMeta orderId = new ColumnMeta();
        orderId.setName("order_id");
        orderId.setType("bigint");
        orderId.setPrimaryKey(true);
        orderId.setNullable(false);
        orderId.setComment("订单ID");

        ColumnMeta itemId = new ColumnMeta();
        itemId.setName("item_id");
        itemId.setType("bigint");
        itemId.setPrimaryKey(true);
        itemId.setNullable(false);
        itemId.setComment("明细ID");

        ColumnMeta productCode = new ColumnMeta();
        productCode.setName("product_code");
        productCode.setType("varchar");
        productCode.setLength(64);
        productCode.setNullable(false);
        productCode.setComment("商品编码");

        ColumnMeta quantity = new ColumnMeta();
        quantity.setName("quantity");
        quantity.setType("int");
        quantity.setNullable(false);
        quantity.setDefaultValue("1");
        quantity.setComment("数量");

        ColumnMeta createTime = new ColumnMeta();
        createTime.setName("create_time");
        createTime.setType("timestamp");
        createTime.setNullable(false);
        createTime.setDefaultValue("CURRENT_TIMESTAMP");
        createTime.setComment("创建时间");

        table.setColumns(List.of(
                orderId,
                itemId,
                productCode,
                quantity,
                createTime
        ));

        dynamicDdlService.createTable(dataSource, table);
    }

    /**
     * 创建JSON字段表
     * CREATE TABLE IF NOT EXISTS t_demo_json
     * (
     *     id          bigint GENERATED ALWAYS AS IDENTITY NOT NULL,
     *     config      json                                NOT NULL,
     *     ext         json,
     *     create_time timestamp                           NOT NULL DEFAULT CURRENT_TIMESTAMP,
     *     PRIMARY KEY (id)
     * );
     * COMMENT
     *     ON TABLE t_demo_json IS 'JSON 字段示例';
     * COMMENT
     *     ON COLUMN t_demo_json.id IS '主键';
     * COMMENT
     *     ON COLUMN t_demo_json.config IS '配置信息(JSON)';
     * COMMENT
     *     ON COLUMN t_demo_json.ext IS '扩展字段';
     * COMMENT
     *     ON COLUMN t_demo_json.create_time IS '创建时间';
     */
    @Test
    public void testCreateTable_WithJsonColumn() {

        TableMeta table = new TableMeta();
        table.setTableName("t_demo_json");
        table.setIfNotExists(true);
        table.setComment("JSON 字段示例");
        

        ColumnMeta id = new ColumnMeta();
        id.setName("id");
        id.setType("bigint");
        id.setPrimaryKey(true);
        id.setAutoIncrement(true);
        id.setNullable(false);
        id.setComment("主键");

        ColumnMeta config = new ColumnMeta();
        config.setName("config");
        config.setType("json");
        config.setNullable(false);
        config.setComment("配置信息(JSON)");

        ColumnMeta ext = new ColumnMeta();
        ext.setName("ext");
        ext.setType("json");
        ext.setNullable(true);
        ext.setComment("扩展字段");

        ColumnMeta createTime = new ColumnMeta();
        createTime.setName("create_time");
        createTime.setType("timestamp");
        createTime.setNullable(false);
        createTime.setDefaultValue("CURRENT_TIMESTAMP");
        createTime.setComment("创建时间");

        table.setColumns(List.of(
                id,
                config,
                ext,
                createTime
        ));

        dynamicDdlService.createTable(dataSource, table);
    }


}

```

