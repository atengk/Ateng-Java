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

### 查询数据

```java
    @Test
    public void test01() {
        // 查询
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
```

### 更新数据

```sql
    @Test
    public void test02() {
        // 更新
        String sql = "UPDATE my_user SET name = ? WHERE id = ?";
        int update = jdbcTemplate.update(sql, "阿腾", "1");
        System.out.println("更新行数：" + update);
    }
```

