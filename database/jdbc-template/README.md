# Spring JDBC Template

Spring JDBC 是一种简化 JDBC 操作的框架，提供了一个核心的 `JdbcTemplate` 类，能够简化数据库的连接管理、SQL 执行和结果处理等。



## 基础配置

### 添加依赖

#### 添加基础依赖

```xml
        <!-- MySQL数据库驱动 -->
        <dependency>
            <groupId>com.mysql</groupId>
            <artifactId>mysql-connector-j</artifactId>
        </dependency>

        <!-- Spring JDBC 数据库框架 -->
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-jdbc</artifactId>
        </dependency>
```

#### 添加数据源依赖

以下任选一种数据库即可

- HikariCP

jdbc依赖中默认已经包含了该依赖

```xml
<!-- HikariCP 数据源 依赖 -->
<dependency>
    <groupId>com.zaxxer</groupId>
    <artifactId>HikariCP</artifactId>
</dependency>
```

- Druid

```xml
<!-- Druid 数据源: 高性能数据库连接池 -->
<!-- https://mvnrepository.com/artifact/com.alibaba/druid-spring-boot-starter -->
<dependency>
    <groupId>com.alibaba</groupId>
    <artifactId>druid-spring-boot-3-starter</artifactId>
    <version>${druid.version}</version>
</dependency>
```



### 编辑配置

编辑配置文件 `application.yml`

#### 数据库配置

```yaml
---
# 数据库的相关配置
spring:
  datasource:
    url: jdbc:mysql://192.168.1.10:35725/kongyu  # MySQL数据库连接URL
    #url: jdbc:postgresql://192.168.1.10:32297/kongyu?currentSchema=public&stringtype=unspecified  # PostgreSQL数据库连接URL
    username: root  # 数据库用户名
    password: Admin@123  # 数据库密码
    # driver-class-name: com.mysql.cj.jdbc.Driver  # 数据库驱动类，框架会自动适配
    type: com.alibaba.druid.pool.DruidDataSource  # 使用Druid数据源
    # Druid连接池配置 https://github.com/alibaba/druid/tree/master/druid-spring-boot-starter
    druid:
      initial-size: 10  # 初始化连接池大小
      min-idle: 10  # 最小空闲连接数
      max-active: 1000  # 最大活跃连接数
      max-wait: 10000  # 获取连接的最大等待时间，单位毫秒
      async-init: true
```

如果使用的是 **HikariCP** ，配置如下

```yaml
spring:
  datasource:
    # ...
    type: com.zaxxer.hikari.HikariDataSource  # 使用 HikariCP 数据源
    hikari:
      maximum-pool-size: 1000  # 最大连接池大小
      minimum-idle: 10  # 最小空闲连接数
      idle-timeout: 30000  # 空闲连接超时时间，单位毫秒
      connection-timeout: 30000  # 获取连接的最大等待时间，单位毫秒
```

#### JDBC配置

```yaml
---
# JDBC 配置
spring:
  jdbc:
    template:
      fetch-size: 1000
      max-rows: 1000
```



## 数据准备

### 创建实体类

```java
package local.ateng.java.jdbc.entity;

import com.alibaba.fastjson2.annotation.JSONField;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serial;
import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.Date;


@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MyUser implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * 用户ID，主键，自增
     */
    private Long id;

    /**
     * 用户名
     */
    private String name;

    /**
     * 用户年龄，允许为空
     */
    private Integer age;

    /**
     * 用户分数，默认为0
     */
    private Double score;

    /**
     * 用户生日，允许为空
     */
    private Date birthday;

    /**
     * 用户所在省份，允许为空
     */
    private String province;

    /**
     * 用户所在城市，允许为空
     */
    private String city;

    /**
     * 记录创建时间，默认当前时间
     */
    @JSONField(format = "yyyy-MM-dd HH:mm:ss.SSS")
    private LocalDateTime createTime;

}
```



## 使用JDBC Template

### 创建测试类

```sql
package local.ateng.java.jdbc;

import local.ateng.java.jdbc.entity.MyUser;
import lombok.RequiredArgsConstructor;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;

import java.util.List;
import java.util.Map;

@SpringBootTest
@RequiredArgsConstructor(onConstructor = @__(@Autowired))
public class JDBCTemplateTests {
    private final JdbcTemplate jdbcTemplate;

    @Test
    public void test() {
        String sql = "SELECT * FROM my_user";
        List<Map<String, Object>> list = jdbcTemplate.queryForList(sql);
        System.out.println(list);
    }

}
```

### 使用

```java
package local.ateng.java.jdbc;

import local.ateng.java.jdbc.entity.MyUser;
import lombok.RequiredArgsConstructor;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.BeanPropertyRowMapper;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;

import java.sql.PreparedStatement;
import java.sql.Statement;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

@SpringBootTest
@RequiredArgsConstructor(onConstructor = @__(@Autowired))
public class JDBCTemplateTests {

    private final JdbcTemplate jdbcTemplate;

    /**
     * 查询多行，返回 Map 列表
     */
    @Test
    public void testQueryListMap() {
        String sql = "SELECT * FROM my_user";
        List<Map<String, Object>> list = jdbcTemplate.queryForList(sql);
        System.out.println(list);
    }

    /**
     * 查询多行，RowMapper 手动映射为实体类
     */
    @Test
    public void testQueryListRowMapper() {
        String sql = "SELECT * FROM my_user";
        List<MyUser> list = jdbcTemplate.query(sql, (rs, rowNum) -> {
            MyUser user = new MyUser();
            user.setId(rs.getLong("id"));
            user.setName(rs.getString("name"));
            user.setAge(rs.getInt("age"));
            user.setScore(rs.getDouble("score"));
            user.setBirthday(rs.getDate("birthday"));
            user.setProvince(rs.getString("province"));
            user.setCity(rs.getString("city"));
            return user;
        });
        System.out.println(list);
    }

    /**
     * 使用 BeanPropertyRowMapper 自动映射为实体类
     * 字段名需与数据库字段一致或能映射
     */
    @Test
    public void testBeanPropertyRowMapper() {
        String sql = "SELECT * FROM my_user";
        List<MyUser> list = jdbcTemplate.query(
                sql,
                new BeanPropertyRowMapper<>(MyUser.class)
        );
        System.out.println(list);
    }

    /**
     * 查询单个对象（queryForObject）
     */
    @Test
    public void testQueryForObject() {
        String sql = "SELECT name FROM my_user WHERE id = ?";
        String name = jdbcTemplate.queryForObject(sql, String.class, 1L);
        System.out.println("用户名：" + name);
    }

    /**
     * 查询单行 Map（queryForMap）
     */
    @Test
    public void testQueryForMap() {
        String sql = "SELECT * FROM my_user WHERE id = ?";
        Map<String, Object> result = jdbcTemplate.queryForMap(sql, 1L);
        System.out.println(result);
    }

    /**
     * 查询单值：例如 count(*)
     */
    @Test
    public void testQueryForSingleValue() {
        String sql = "SELECT COUNT(1) FROM my_user";
        Integer count = jdbcTemplate.queryForObject(sql, Integer.class);
        System.out.println("总数：" + count);
    }

    /**
     * 判断某行是否存在
     */
    @Test
    public void testExists() {
        String sql = "SELECT COUNT(1) FROM my_user WHERE id = ?";
        Integer count = jdbcTemplate.queryForObject(sql, Integer.class, 1L);
        boolean exists = count != null && count > 0;
        System.out.println("是否存在：" + exists);
    }

    /**
     * 简单更新
     */
    @Test
    public void testUpdate() {
        String sql = "UPDATE my_user SET name = ? WHERE id = ?";
        int update = jdbcTemplate.update(sql, "阿腾", 1L);
        System.out.println("更新行数：" + update);
    }

    /**
     * 插入数据并返回主键
     */
    @Test
    public void testInsertReturnKey() {
        String sql = "INSERT INTO my_user(name, age, score) VALUES (?, ?, ?)";
        KeyHolder keyHolder = new GeneratedKeyHolder();

        jdbcTemplate.update(connection -> {
            PreparedStatement ps = connection.prepareStatement(
                    sql,
                    Statement.RETURN_GENERATED_KEYS
            );
            ps.setString(1, "小明");
            ps.setInt(2, 20);
            ps.setDouble(3, 88.5);
            return ps;
        }, keyHolder);

        Number key = keyHolder.getKey();
        System.out.println("生成的主键：" + key);
    }

    /**
     * 删除数据
     */
    @Test
    public void testDelete() {
        String sql = "DELETE FROM my_user WHERE id = ?";
        int delete = jdbcTemplate.update(sql, 10L);
        System.out.println("删除行数：" + delete);
    }

    /**
     * 批量更新（批处理）
     */
    @Test
    public void testBatchUpdate() {
        String sql = "UPDATE my_user SET age = ? WHERE id = ?";
        List<Object[]> batchArgs = Arrays.asList(
                new Object[]{20, 1L},
                new Object[]{22, 2L},
                new Object[]{30, 3L}
        );

        int[] results = jdbcTemplate.batchUpdate(sql, batchArgs);
        System.out.println("批量结果：" + Arrays.toString(results));
    }

    /**
     * 批量插入
     */
    @Test
    public void testBatchInsert() {
        String sql = "INSERT INTO my_user(name, age, score) VALUES (?, ?, ?)";
        List<Object[]> params = Arrays.asList(
                new Object[]{"用户A", 18, 90.5},
                new Object[]{"用户B", 20, 86.0},
                new Object[]{"用户C", 25, 95.0}
        );

        int[] results = jdbcTemplate.batchUpdate(sql, params);
        System.out.println("批量插入：" + Arrays.toString(results));
    }

    @Test
    public void testPagination() {
        // ---------- 分页参数 ----------
        int page = 1; // 页码（从1开始）
        int size = 5; // 每页条数
        int offset = (page - 1) * size;

        // ---------- 查询条件 ----------
        String sqlBase = "SELECT id, name, age, score, birthday, province, city FROM my_user WHERE age > ?";
        Object[] params = new Object[]{10};

        // ---------- 统计总数 ----------
        String sqlCount = "SELECT COUNT(1) FROM (" + sqlBase + ") AS temp_count";
        Long total = jdbcTemplate.queryForObject(sqlCount, params, Long.class);
        System.out.println("总条数：" + total);

        // ---------- 分页查询 ----------
        String sqlPage = sqlBase + " LIMIT ? OFFSET ?";
        Object[] pageParams = Arrays.copyOf(params, params.length + 2);
        pageParams[params.length] = size;
        pageParams[params.length + 1] = offset;

        List<MyUser> pageList = jdbcTemplate.query(sqlPage, pageParams, new BeanPropertyRowMapper<>(MyUser.class));

        // ---------- 输出分页结果 ----------
        System.out.println("当前页码：" + page);
        System.out.println("每页条数：" + size);
        System.out.println("分页数据：" + pageList);
    }


}
```





## 多数据源

### 添加相关驱动

```xml
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
```

### 添加配置

```yaml
---
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
package local.ateng.java.jdbc.config;

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
package local.ateng.java.jdbc.config;


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

### 使用

```java
package local.ateng.java.jdbc.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/demo")
@RequiredArgsConstructor
public class DemoController {
    private final JdbcTemplate mysqlJdbcTemplate;
    private final JdbcTemplate postgresqlJdbcTemplate;

    @GetMapping("/mysqlPrimary")
    public String mysqlPrimary() {
        return mysqlJdbcTemplate.queryForObject("SELECT NOW()", String.class);
    }

    @GetMapping("/postgresqlSecondary")
    public String postgresqlSecondary() {
        return postgresqlJdbcTemplate.queryForObject("SELECT NOW()", String.class);
    }

}

```

