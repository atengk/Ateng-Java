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
    <% if(col.autoIncrement != null && col.autoIncrement){ %> AUTO_INCREMENT<% } %>

    <% /* ---------- 默认值 ---------- */ %>
    <% if(col.defaultValue != null){ %> DEFAULT (${col.defaultValue})<% } %>
    <% if(col.onUpdateCurrentTimestamp != null && col.onUpdateCurrentTimestamp){ %> ON UPDATE CURRENT_TIMESTAMP<% } %>

    <% /* ---------- 注释 ---------- */ %>
    <% if(col.comment != null){ %> COMMENT '${col.comment}'<% } %>

    <% if(!colLP.last){ %>,<% } %>
<%
}
%>

<%
/* ================= 索引定义 ================= */
if(table.indexes != null && table.indexes.~size > 0){
    for(idx in table.indexes){
%>,
    <% /* ---------- PRIMARY KEY ---------- */ %>
    <% if(idx.type == "PRIMARY"){ %>
    PRIMARY KEY (
        <%
        for(c in idx.columns){
        %>${c.columnName}<% if(c.order != null){ %> ${c.order}<% } %><% if(!cLP.last){ %>, <% } %>
        <% } %>
    )

    <% /* ---------- UNIQUE ---------- */ %>
    <% } else if(idx.type == "UNIQUE"){ %>
    UNIQUE KEY ${idx.name}
        <% if(idx.using != null){ %> USING ${idx.using}<% } %>
    (
        <%
        for(c in idx.columns){
        %>${c.columnName}<% if(c.order != null){ %> ${c.order}<% } %><% if(!cLP.last){ %>, <% } %>
        <% } %>
    )

    <% /* ---------- FULLTEXT ---------- */ %>
    <% } else if(idx.type == "FULLTEXT"){ %>
    FULLTEXT KEY ${idx.name}
    (
        <%
        for(c in idx.columns){
        %>${c.columnName}<% if(!cLP.last){ %>, <% } %>
        <% } %>
    )

    <% /* ---------- NORMAL ---------- */ %>
    <% } else { %>
    KEY ${idx.name}
        <% if(idx.using != null){ %> USING ${idx.using}<% } %>
    (
        <%
        for(c in idx.columns){
        %>${c.columnName}<% if(c.order != null){ %> ${c.order}<% } %><% if(!cLP.last){ %>, <% } %>
        <% } %>
    )
    <% } %>
<%
    }
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
    ${col.name} ${col.type}
    <%
    /* ---------- varchar / char ---------- */
    if(
        col.length != null
        && (
            col.type == "character varying"
            || col.type == "varchar"
            || col.type == "character"
            || col.type == "char"
        )
    ){
    %>(${col.length})<%
    }
    %>

    <%
    /* ---------- numeric / decimal ---------- */
    if(
        col.precision != null
        && col.scale != null
        && (
            col.type == "numeric"
            || col.type == "decimal"
        )
    ){
    %>(${col.precision},${col.scale})<%
    }
    %>

    <%
    /* ---------- identity ---------- */
    if(col.autoIncrement != null && col.autoIncrement){
    %> GENERATED BY DEFAULT AS IDENTITY<%
    }
    %>

    <%
    /* ---------- NOT NULL ---------- */
    if(col.nullable != null && !col.nullable){
    %> NOT NULL<%
    }
    %>

    <%
    /* ---------- DEFAULT ---------- */
    if(col.defaultValue != null){
    %> DEFAULT ${col.defaultValue}<%
    }
    %>

    <%
    if(!colLP.last){
    %>,<%
    }
    %>
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
/* ================= UNIQUE 索引（替代 UNIQUE 约束） ================= */
if(table.indexes != null && table.indexes.~size > 0){
    for(idx in table.indexes){
        if(idx.type == "UNIQUE"){
%>
CREATE UNIQUE INDEX IF NOT EXISTS ${idx.name}
ON ${table.tableName} (
    <%
    for(c in idx.columns){
    %>${c.columnName}<% if(!cLP.last){ %>, <% } %>
    <% } %>
);
<%
        }
    }
}
%>

<%
/* ================= 普通 / 全文索引 ================= */
if(table.indexes != null && table.indexes.~size > 0){
    for(idx in table.indexes){
        if(idx.type == "NORMAL"){
%>
CREATE INDEX IF NOT EXISTS ${idx.name}
ON ${table.tableName}
<% if(idx.using != null){ %> USING ${idx.using}<% } %>
(
    <%
    for(c in idx.columns){
    %>${c.columnName}<% if(c.order != null){ %> ${c.order}<% } %><% if(!cLP.last){ %>, <% } %>
    <% } %>
);
<%
        } else if(idx.type == "FULLTEXT"){
%>
CREATE INDEX IF NOT EXISTS ${idx.name}
ON ${table.tableName}
USING GIN (
    <%
    for(c in idx.columns){
    %>to_tsvector('simple', ${c.columnName})<% if(!cLP.last){ %>, <% } %>
    <% } %>
);
<%
        }
    }
}
%>

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

#### IndexColumnMeta

```java
package io.github.atengk.beetl.model;

import lombok.Data;

/**
 * 索引字段元数据
 *
 * @author 孔余
 * @since 2025-12-18
 */
@Data
public class IndexColumnMeta {

    /**
     * 字段名
     */
    private String columnName;

    /**
     * 排序方式 ASC / DESC
     */
    private String order;
}

```

#### IndexMeta

```java
package io.github.atengk.beetl.model;

import lombok.Data;

import java.util.ArrayList;
import java.util.List;

/**
 * 索引元数据
 *
 * @author 孔余
 * @since 2025-12-18
 */
@Data
public class IndexMeta {

    /**
     * 索引名称
     */
    private String name;

    /**
     * PRIMARY / UNIQUE / NORMAL / FULLTEXT
     */
    private String type;

    /**
     * BTREE / HASH
     */
    private String using;

    /**
     * 索引字段
     */
    private List<IndexColumnMeta> columns = new ArrayList<>();
}

```

#### TableMeta

```java
package io.github.atengk.beetl.model;

import lombok.Data;

import java.util.List;

/**
 * 表结构定义
 *
 * @author 孔余
 * @since 2025-12-18
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

    /**
     * 索引列表
     */
    private List<IndexMeta> indexes;
}

```

### 创建MySQL工具类

```java
package io.github.atengk.beetl.utils;

import io.github.atengk.beetl.model.*;
import org.springframework.jdbc.core.JdbcTemplate;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * MySQL 表结构元数据加载工具类
 *
 * <p>
 * 用于从 information_schema 中反向解析表结构，
 * 并组装为 TableMeta / ColumnMeta / IndexMeta 模型。
 * </p>
 *
 * <p>
 * 设计为纯静态工具类，不依赖 Spring 容器，
 * JdbcTemplate 由调用方显式传入。
 * </p>
 *
 * @author 孔余
 * @since 2025-12-24
 */
public final class TableUtil {

    /**
     * 私有构造函数，禁止实例化
     */
    private TableUtil() {
        throw new UnsupportedOperationException("工具类不可实例化");
    }

    /**
     * 加载完整表元数据
     * <p>
     * 自动从当前连接中获取 schema
     * </p>
     *
     * @param jdbcTemplate JdbcTemplate
     * @param tableName    表名
     * @return 表元数据
     */
    public static TableMeta loadTableMeta(
            JdbcTemplate jdbcTemplate,
            String tableName
    ) {

        String schema = getCurrentSchema(jdbcTemplate);
        return loadTableMeta(jdbcTemplate, schema, tableName);
    }

    /**
     * 获取当前连接使用的数据库名
     *
     * @param jdbcTemplate JdbcTemplate
     * @return schema 名称
     */
    private static String getCurrentSchema(JdbcTemplate jdbcTemplate) {

        String sql = "select database()";

        return jdbcTemplate.queryForObject(
                sql,
                String.class
        );
    }

    /**
     * 加载完整表元数据
     *
     * @param jdbcTemplate JdbcTemplate
     * @param schema       数据库名
     * @param tableName    表名
     * @return 表元数据
     */
    public static TableMeta loadTableMeta(
            JdbcTemplate jdbcTemplate,
            String schema,
            String tableName
    ) {

        TableMeta tableMeta = queryTable(jdbcTemplate, schema, tableName);

        List<ColumnMeta> columns = queryColumns(jdbcTemplate, schema, tableName);
        Map<String, ColumnMeta> columnMap = columns.stream()
                .collect(Collectors.toMap(
                        ColumnMeta::getName,
                        Function.identity()
                ));

        List<IndexMeta> indexes = queryIndexes(jdbcTemplate, schema, tableName);

        for (IndexMeta index : indexes) {
            if (index.getType() == "PRIMARY") {
                for (IndexColumnMeta indexColumn : index.getColumns()) {
                    ColumnMeta columnMeta = columnMap.get(indexColumn.getColumnName());
                    if (columnMeta != null) {
                        columnMeta.setPrimaryKey(true);
                    }
                }
            }
        }

        tableMeta.setColumns(columns);
        tableMeta.setIndexes(indexes);
        return tableMeta;
    }

    /**
     * 查询表基本信息
     *
     * @param jdbcTemplate JdbcTemplate
     * @param schema       数据库名
     * @param tableName    表名
     * @return 表元数据
     */
    private static TableMeta queryTable(
            JdbcTemplate jdbcTemplate,
            String schema,
            String tableName
    ) {

        String sql =
                "select table_name, table_comment, engine, table_collation " +
                        "from information_schema.tables " +
                        "where table_schema = ? and table_name = ?";

        return jdbcTemplate.queryForObject(
                sql,
                new Object[]{schema, tableName},
                (rs, rowNum) -> {
                    TableMeta table = new TableMeta();
                    table.setTableName(rs.getString("table_name"));
                    table.setComment(rs.getString("table_comment"));
                    table.setEngine(rs.getString("engine"));
                    table.setCharset(extractCharset(rs.getString("table_collation")));
                    return table;
                }
        );
    }

    /**
     * 查询字段信息
     *
     * @param jdbcTemplate JdbcTemplate
     * @param schema       数据库名
     * @param tableName    表名
     * @return 字段列表
     */
    private static List<ColumnMeta> queryColumns(
            JdbcTemplate jdbcTemplate,
            String schema,
            String tableName
    ) {

        String sql =
                "select column_name, column_type, is_nullable, column_default, extra, column_comment " +
                        "from information_schema.columns " +
                        "where table_schema = ? and table_name = ? " +
                        "order by ordinal_position";

        return jdbcTemplate.query(
                sql,
                new Object[]{schema, tableName},
                (rs, rowNum) -> {
                    ColumnMeta column = new ColumnMeta();
                    column.setName(rs.getString("column_name"));
                    column.setType(rs.getString("column_type"));
                    column.setNullable("YES".equals(rs.getString("is_nullable")));
                    column.setDefaultValue(rs.getString("column_default"));

                    String extra = rs.getString("extra");
                    column.setAutoIncrement(
                            extra != null && extra.contains("auto_increment")
                    );
                    column.setOnUpdateCurrentTimestamp(
                            extra != null && extra.toLowerCase().contains("on update current_timestamp")
                    );
                    column.setPrimaryKey(false);
                    column.setComment(rs.getString("column_comment"));
                    return column;
                }
        );
    }

    /**
     * 查询索引信息
     *
     * @param jdbcTemplate JdbcTemplate
     * @param schema       数据库名
     * @param tableName    表名
     * @return 索引列表
     */
    private static List<IndexMeta> queryIndexes(
            JdbcTemplate jdbcTemplate,
            String schema,
            String tableName
    ) {

        String sql =
                "select index_name, non_unique, column_name, seq_in_index, collation, index_type " +
                        "from information_schema.statistics " +
                        "where table_schema = ? and table_name = ? " +
                        "order by index_name, seq_in_index";

        Map<String, IndexMeta> indexMap = new LinkedHashMap<>();

        jdbcTemplate.query(
                sql,
                new Object[]{schema, tableName},
                rs -> {
                    String indexName = rs.getString("index_name");

                    IndexMeta indexMeta = indexMap.computeIfAbsent(indexName, key -> {
                        IndexMeta meta = new IndexMeta();
                        meta.setName(indexName);

                        try {
                            int nonUnique = rs.getInt("non_unique");

                            if ("PRIMARY".equals(indexName)) {
                                meta.setType("PRIMARY");
                            } else if (nonUnique == 0) {
                                meta.setType("UNIQUE");
                            } else {
                                meta.setType("NORMAL");
                            }

                            meta.setUsing(rs.getString("index_type"));
                            meta.setColumns(new ArrayList<>());
                        } catch (SQLException e) {
                            throw new RuntimeException(e);
                        }

                        return meta;
                    });

                    IndexColumnMeta columnMeta = new IndexColumnMeta();
                    columnMeta.setColumnName(rs.getString("column_name"));

                    String collation = rs.getString("collation");
                    if ("A".equals(collation)) {
                        columnMeta.setOrder("ASC");
                    } else if ("D".equals(collation)) {
                        columnMeta.setOrder("DESC");
                    }

                    indexMeta.getColumns().add(columnMeta);
                }
        );

        return new ArrayList<>(indexMap.values());
    }

    /**
     * 从 collation 中提取字符集
     *
     * @param collation 排序规则
     * @return 字符集
     */
    private static String extractCharset(String collation) {
        if (collation == null) {
            return null;
        }
        int index = collation.indexOf('_');
        if (index > 0) {
            return collation.substring(0, index);
        }
        return collation;
    }

    /**
     * 规范化表索引定义
     *
     * <p>
     * 如果 ColumnMeta 中声明了 primaryKey，但未显式定义 PRIMARY 索引，
     * 则自动补充 PRIMARY 索引定义
     * </p>
     */
    public static void normalizePrimaryIndex(TableMeta tableMeta) {

        if (tableMeta.getColumns() == null || tableMeta.getColumns().isEmpty()) {
            return;
        }

        boolean hasPrimaryIndex = tableMeta.getIndexes() != null
                && tableMeta.getIndexes().stream()
                .anyMatch(idx -> idx.getType() == "PRIMARY");

        if (hasPrimaryIndex) {
            return;
        }

        List<IndexColumnMeta> pkColumns = tableMeta.getColumns().stream()
                .filter(col -> Boolean.TRUE.equals(col.getPrimaryKey()))
                .map(col -> {
                    IndexColumnMeta ic = new IndexColumnMeta();
                    ic.setColumnName(col.getName());
                    return ic;
                })
                .collect(Collectors.toList());

        if (pkColumns.isEmpty()) {
            return;
        }

        IndexMeta primaryIndex = new IndexMeta();
        primaryIndex.setName("PRIMARY");
        primaryIndex.setType("PRIMARY");
        primaryIndex.setColumns(pkColumns);

        if (tableMeta.getIndexes() == null) {
            tableMeta.setIndexes(new ArrayList<>());
        }

        tableMeta.getIndexes().add(0, primaryIndex);
    }


}

```

### 创建PostgreSQL工具类

```java
package io.github.atengk.beetl.utils;

import io.github.atengk.beetl.model.*;
import org.springframework.jdbc.core.JdbcTemplate;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * PostgreSQL 表结构元数据加载工具类（PostgreSQL 16）
 *
 * <p>
 * 用于从 information_schema / pg_catalog 中反向解析表结构，
 * 并组装为 TableMeta / ColumnMeta / IndexMeta 模型。
 * </p>
 *
 * <p>
 * 设计为纯静态工具类，不依赖 Spring 容器，
 * JdbcTemplate 由调用方显式传入。
 * </p>
 *
 * @author 孔余
 * @since 2025-12-24
 */
public final class TablePGUtil {

    private TablePGUtil() {
        throw new UnsupportedOperationException("工具类不可实例化");
    }

    /**
     * 加载完整表元数据（使用当前 schema）
     *
     * @param jdbcTemplate JdbcTemplate
     * @param tableName    表名
     * @return 表元数据
     */
    public static TableMeta loadTableMeta(
            JdbcTemplate jdbcTemplate,
            String tableName
    ) {

        String schema = getCurrentSchema(jdbcTemplate);
        return loadTableMeta(jdbcTemplate, schema, tableName);
    }

    /**
     * 获取当前 schema
     *
     * @param jdbcTemplate JdbcTemplate
     * @return schema 名称
     */
    private static String getCurrentSchema(JdbcTemplate jdbcTemplate) {

        String sql = "select current_schema()";
        return jdbcTemplate.queryForObject(sql, String.class);
    }

    /**
     * 加载完整表元数据
     *
     * @param jdbcTemplate JdbcTemplate
     * @param schema       schema 名称
     * @param tableName    表名
     * @return 表元数据
     */
    public static TableMeta loadTableMeta(
            JdbcTemplate jdbcTemplate,
            String schema,
            String tableName
    ) {

        TableMeta tableMeta = queryTable(jdbcTemplate, schema, tableName);

        List<ColumnMeta> columns = queryColumns(jdbcTemplate, schema, tableName);
        Map<String, ColumnMeta> columnMap = columns.stream()
                .collect(Collectors.toMap(
                        ColumnMeta::getName,
                        Function.identity()
                ));

        List<IndexMeta> indexes = queryIndexes(jdbcTemplate, schema, tableName);

        for (IndexMeta index : indexes) {
            if ("PRIMARY".equals(index.getType())) {
                for (IndexColumnMeta indexColumn : index.getColumns()) {
                    ColumnMeta columnMeta = columnMap.get(indexColumn.getColumnName());
                    if (columnMeta != null) {
                        columnMeta.setPrimaryKey(true);
                    }
                }
            }
        }

        tableMeta.setColumns(columns);
        tableMeta.setIndexes(indexes);
        return tableMeta;
    }

    /**
     * 查询表基本信息
     */
    private static TableMeta queryTable(
            JdbcTemplate jdbcTemplate,
            String schema,
            String tableName
    ) {

        String sql =
                "select c.relname as table_name, " +
                        "obj_description(c.oid) as table_comment " +
                        "from pg_class c " +
                        "join pg_namespace n on n.oid = c.relnamespace " +
                        "where n.nspname = ? and c.relname = ?";

        return jdbcTemplate.queryForObject(
                sql,
                new Object[]{schema, tableName},
                (rs, rowNum) -> {
                    TableMeta table = new TableMeta();
                    table.setTableName(rs.getString("table_name"));
                    table.setComment(rs.getString("table_comment"));
                    table.setEngine(null);
                    table.setCharset(null);
                    return table;
                }
        );
    }

    /**
     * 查询 PostgreSQL 表字段信息（PostgreSQL 16）
     *
     * <p>
     * 使用 information_schema.columns + pg_catalog 补充 identity 信息，
     * 适配 Beetl PostgreSQL 建表模板。
     * </p>
     */
    private static List<ColumnMeta> queryColumns(
            JdbcTemplate jdbcTemplate,
            String schema,
            String tableName
    ) {

        String sql =
                "select " +
                        "c.column_name, " +
                        "c.data_type, " +
                        "c.is_nullable, " +
                        "c.column_default, " +
                        "c.character_maximum_length, " +
                        "c.numeric_precision, " +
                        "c.numeric_scale, " +
                        "c.is_identity, " +
                        "c.identity_generation, " +
                        "c.udt_name, " +
                        "pgd.description as column_comment " +
                        "from information_schema.columns c " +
                        "left join pg_catalog.pg_class pc " +
                        "  on pc.relname = c.table_name " +
                        "left join pg_catalog.pg_namespace pn " +
                        "  on pn.nspname = c.table_schema " +
                        "left join pg_catalog.pg_description pgd " +
                        "  on pgd.objoid = pc.oid " +
                        " and pgd.objsubid = c.ordinal_position " +
                        "where c.table_schema = ? " +
                        "  and c.table_name = ? " +
                        "order by c.ordinal_position";

        return jdbcTemplate.query(
                sql,
                new Object[]{schema, tableName},
                (rs, rowNum) -> {

                    ColumnMeta column = new ColumnMeta();

                    /* ---------- 基本信息 ---------- */
                    column.setName(rs.getString("column_name"));
                    String dataType = rs.getString("data_type");
                    String udtName = rs.getString("udt_name");

                    if ("USER-DEFINED".equals(dataType)) {
                        column.setType(udtName);
                    } else {
                        column.setType(dataType);
                    }

                    /* ---------- nullable ---------- */
                    column.setNullable("YES".equals(rs.getString("is_nullable")));

                    /* ---------- 默认值 ---------- */
                    String defaultValue = rs.getString("column_default");
                    if (defaultValue != null) {
                        column.setDefaultValue(defaultValue);
                    }

                    /* ---------- varchar / char ---------- */
                    Integer charLen = rs.getObject(
                            "character_maximum_length",
                            Integer.class
                    );
                    if (charLen != null) {
                        column.setLength(charLen);
                    }

                    /* ---------- numeric / decimal ---------- */
                    Integer precision = rs.getObject(
                            "numeric_precision",
                            Integer.class
                    );
                    Integer scale = rs.getObject(
                            "numeric_scale",
                            Integer.class
                    );
                    if (precision != null && scale != null) {
                        column.setPrecision(precision);
                        column.setScale(scale);
                    }

                    /* ---------- identity（自增） ---------- */
                    String isIdentity = rs.getString("is_identity");
                    if ("YES".equals(isIdentity)) {
                        column.setAutoIncrement(true);
                    } else {
                        column.setAutoIncrement(false);
                    }

                    /* ---------- PostgreSQL 不支持 ON UPDATE ---------- */
                    column.setOnUpdateCurrentTimestamp(false);

                    /* ---------- 主键默认 false（后续由索引补） ---------- */
                    column.setPrimaryKey(false);

                    /* ---------- 注释 ---------- */
                    column.setComment(rs.getString("column_comment"));

                    return column;
                }
        );
    }

    /**
     * 查询索引信息
     */
    private static List<IndexMeta> queryIndexes(
            JdbcTemplate jdbcTemplate,
            String schema,
            String tableName
    ) {

        String sql =
                "select " +
                        "i.relname as index_name, " +
                        "ix.indisunique, " +
                        "ix.indisprimary, " +
                        "a.attname as column_name, " +
                        "am.amname as index_type, " +
                        "case when (ix.indoption[a.attnum - 1] & 1) = 1 then 'DESC' else 'ASC' end as sort_order, " +
                        "array_position(ix.indkey, a.attnum) as seq " +
                        "from pg_class t " +
                        "join pg_namespace n on n.oid = t.relnamespace " +
                        "join pg_index ix on t.oid = ix.indrelid " +
                        "join pg_class i on i.oid = ix.indexrelid " +
                        "join pg_am am on i.relam = am.oid " +
                        "join pg_attribute a on a.attrelid = t.oid and a.attnum = any(ix.indkey) " +
                        "where n.nspname = ? and t.relname = ? " +
                        "order by index_name, seq";

        Map<String, IndexMeta> indexMap = new LinkedHashMap<>();

        jdbcTemplate.query(
                sql,
                new Object[]{schema, tableName},
                rs -> {
                    String indexName = rs.getString("index_name");

                    IndexMeta indexMeta = indexMap.computeIfAbsent(indexName, key -> {
                        IndexMeta meta = new IndexMeta();
                        meta.setName(indexName);

                        boolean primary = false;
                        boolean unique = false;
                        try {
                            primary = rs.getBoolean("indisprimary");
                            unique = rs.getBoolean("indisunique");
                        } catch (SQLException e) {
                            throw new RuntimeException(e);
                        }

                        if (primary) {
                            meta.setType("PRIMARY");
                        } else if (unique) {
                            meta.setType("UNIQUE");
                        } else {
                            meta.setType("NORMAL");
                        }

                        try {
                            meta.setUsing(rs.getString("index_type"));
                        } catch (SQLException e) {
                            throw new RuntimeException(e);
                        }
                        meta.setColumns(new ArrayList<>());
                        return meta;
                    });

                    IndexColumnMeta columnMeta = new IndexColumnMeta();
                    columnMeta.setColumnName(rs.getString("column_name"));
                    columnMeta.setOrder(rs.getString("sort_order"));

                    indexMeta.getColumns().add(columnMeta);
                }
        );

        return new ArrayList<>(indexMap.values());
    }

    /**
     * 规范化主键索引
     */
    public static void normalizePrimaryIndex(TableMeta tableMeta) {

        if (tableMeta.getColumns() == null || tableMeta.getColumns().isEmpty()) {
            return;
        }

        boolean hasPrimaryIndex = tableMeta.getIndexes() != null
                && tableMeta.getIndexes().stream()
                .anyMatch(idx -> "PRIMARY".equals(idx.getType()));

        if (hasPrimaryIndex) {
            return;
        }

        List<IndexColumnMeta> pkColumns = tableMeta.getColumns().stream()
                .filter(col -> Boolean.TRUE.equals(col.getPrimaryKey()))
                .map(col -> {
                    IndexColumnMeta ic = new IndexColumnMeta();
                    ic.setColumnName(col.getName());
                    return ic;
                })
                .collect(Collectors.toList());

        if (pkColumns.isEmpty()) {
            return;
        }

        IndexMeta primaryIndex = new IndexMeta();
        primaryIndex.setName("PRIMARY");
        primaryIndex.setType("PRIMARY");
        primaryIndex.setColumns(pkColumns);

        if (tableMeta.getIndexes() == null) {
            tableMeta.setIndexes(new ArrayList<>());
        }

        tableMeta.getIndexes().add(0, primaryIndex);
    }
}

```

### 创建DDL 服务

```java
package io.github.atengk.beetl.service;

import cn.hutool.db.sql.SqlUtil;
import io.github.atengk.beetl.enums.DatabaseType;
import io.github.atengk.beetl.model.TableMeta;
import io.github.atengk.beetl.utils.TableUtil;
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

        TableUtil.normalizePrimaryIndex(tableMeta);

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

#### 创建

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

#### SQL转换成实体类

```java
package io.github.atengk.beetl;

import cn.hutool.json.JSONUtil;
import io.github.atengk.beetl.model.TableMeta;
import io.github.atengk.beetl.service.DynamicDdlService;
import io.github.atengk.beetl.utils.TableUtil;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;

@SpringBootTest
public class MySQLTests {
    @Qualifier("mysqlJdbcTemplate")
    @Autowired
    private JdbcTemplate jdbcTemplate;
    @Autowired
    private DynamicDdlService dynamicDdlService;

    @Test
    public void test() {
        TableMeta tableMeta = TableUtil.loadTableMeta(jdbcTemplate, "project");
        System.out.println(JSONUtil.toJsonPrettyStr(tableMeta));
    }

    @Test
    public void test2() {
        TableMeta tableMeta = TableUtil.loadTableMeta(jdbcTemplate, "project");
        tableMeta.setIfNotExists(true);
        dynamicDdlService.createTable(jdbcTemplate.getDataSource(), tableMeta);
    }

}

```



### PostgreSQL

#### 创建

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

#### SQL转换成实体类

```java
package io.github.atengk.beetl;

import cn.hutool.json.JSONUtil;
import io.github.atengk.beetl.model.TableMeta;
import io.github.atengk.beetl.service.DynamicDdlService;
import io.github.atengk.beetl.utils.TablePGUtil;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;

@SpringBootTest
public class PostgreSQLTests {
    @Qualifier("postgresqlJdbcTemplate")
    @Autowired
    private JdbcTemplate jdbcTemplate;
    @Autowired
    private DynamicDdlService dynamicDdlService;

    @Test
    public void test() {
        TableMeta tableMeta = TablePGUtil.loadTableMeta(jdbcTemplate, "project");
        System.out.println(JSONUtil.toJsonPrettyStr(tableMeta));
    }

    @Test
    public void test2() {
        TableMeta tableMeta = TablePGUtil.loadTableMeta(jdbcTemplate, "project");
        tableMeta.setIfNotExists(true);
        dynamicDdlService.createTable(jdbcTemplate.getDataSource(), tableMeta);
    }

}

```

