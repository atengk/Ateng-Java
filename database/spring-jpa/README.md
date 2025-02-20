# Spring Data JPA

**Spring Data JPA** 是 Spring 项目中一个用于简化 Java 持久化（数据库操作）的框架，它基于 **JPA（Java Persistence API）** 规范提供了一层高级的抽象，允许开发者通过简单的方式与数据库交互。Spring Data JPA 通过减少样板代码，简化了与数据库的交互，特别是在使用关系型数据库时，开发者不需要编写复杂的 SQL 查询，Spring Data JPA 会自动根据方法名生成 SQL 查询，或者通过 JPQL（Java Persistence Query Language）进行查询。

Spring Data JPA 的主要特点包括：

- **简化数据库访问**：通过提供接口，开发者无需实现实际的数据库操作逻辑。
- **自动查询生成**：Spring Data JPA 可以根据方法名自动生成 SQL 查询。
- **支持分页和排序**：内置对分页查询和排序的支持。
- **与 Hibernate 集成**：Spring Data JPA 底层通常是基于 Hibernate 实现的，但也可以与其他 JPA 实现（如 EclipseLink）兼容。

**Spring Data JPA 工作原理**

Spring Data JPA 的核心工作原理是通过定义 Repository 接口（通常继承 `JpaRepository` 或 `CrudRepository`）来提供数据库操作，而无需手动实现这些方法。Spring Data JPA 会根据接口方法的签名自动生成 SQL 或 JPQL 查询。

**Spring Data JPA 关键组件**

1. **Entity**: 一个表示数据库表的 Java 类，通常会使用 `@Entity` 注解标识。
2. **Repository**: 用于定义数据库操作的接口，通常继承 `JpaRepository` 或 `CrudRepository`。
3. **Query Method**: 可以通过方法命名规则，Spring Data JPA 会根据方法名称自动生成查询。
4. **JPQL**: Java Persistence Query Language，用于执行更复杂的查询，类似于 SQL，但操作的是实体对象。



## 基础准备

### 添加依赖

#### 添加基础依赖

```xml
        <!-- MySQL数据库驱动 -->
        <dependency>
            <groupId>com.mysql</groupId>
            <artifactId>mysql-connector-j</artifactId>
        </dependency>

        <!-- Spring Boot Starter Data JPA -->
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-data-jpa</artifactId>
        </dependency>
```

#### 添加数据源依赖

以下任选一种数据库即可

- HikariCP

JPA依赖中默认已经包含了该依赖（在spring-boot-starter-jdbc中）

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



### 编辑配置文件

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

#### JPA配置

```yaml
---
# JPA 配置
spring:
  jpa:
    show-sql: true
    hibernate:
      ddl-auto: updateById
```

**`spring.jpa.hibernate.ddl-auto` 选项**

`ddl-auto` 参数的值决定了 Hibernate 在应用启动时如何管理数据库的表结构。常见的选项有：

1. **`none`**（默认值，JPA 规范的默认值）：表示 Hibernate 不会自动执行任何 schema 更新或创建操作。也就是说，它不会对数据库表做任何更改。如果数据库表已经存在，Hibernate 不会进行任何修改。
2. **`update`**：表示 Hibernate 会检查实体类（`@Entity` 类）和数据库中的表结构，如果数据库表结构不同，它会尝试自动更新数据库表以使其与实体类的结构保持一致。例如，新增字段、修改字段类型等。**注意：**它不会删除表或修改现有数据。
3. **`create`**：表示每次启动应用时，Hibernate 会根据实体类重新生成数据库表（如果表已经存在，则会删除再创建）。这通常用于开发和测试阶段，但在生产环境中不推荐使用，因为它会删除数据库中的所有数据。
4. **`create-drop`**：表示 Hibernate 会在启动时创建数据库表，并在应用停止时删除这些表。通常用于单元测试和开发环境。这个选项会在每次应用启动时删除所有表，所以它会丢失表中的数据。
5. **`validate`**：表示 Hibernate 在启动时会验证数据库中是否存在与实体类对应的表结构（字段名、字段类型等）。如果表结构不匹配，它会抛出异常并中止应用程序启动。这个选项非常适合生产环境，因为它确保了数据库的表结构与实体类一致，但不会自动更新数据库。



### 创建实体类

```java
package local.ateng.java.jpa.entity;

import com.alibaba.fastjson2.annotation.JSONField;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.Comment;

import java.io.Serial;
import java.io.Serializable;
import java.time.LocalDateTime;


@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity(name = "my_user")
public class MyUser implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * 用户ID，主键，自增
     */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY) // 使用数据库的自增策略
    @Column(name = "id", nullable = false)
    @Comment("用户ID，主键，自增")
    private Long id;

    /**
     * 用户名
     */
    @Column(name = "name", nullable = false, unique = true)
    @Comment("用户姓名")
    private String name;

    /**
     * 用户年龄，允许为空
     */
    @Column(name = "age")
    @Comment("用户年龄")
    private Integer age;

    /**
     * 用户分数
     */
    @Column(name = "score", columnDefinition = "DOUBLE DEFAULT 0.0")
    @Comment("用户分数")
    private Double score;

    /**
     * 用户生日，允许为空
     */
    @Column(name = "birthday")
    @Comment("用户生日")
    private LocalDateTime birthday;

    /**
     * 用户所在省份，允许为空
     */
    @Column(name = "province")
    @Comment("用户所在省份")
    private String province;

    /**
     * 用户所在城市，允许为空
     */
    @Column(name = "city")
    @Comment("用户所在城市")
    private String city;

    /**
     * 记录创建时间，默认当前时间
     */
    @JSONField(format = "yyyy-MM-dd HH:mm:ss.SSS")
    @Column(name = "createTime")
    @Comment("创建时间")
    private LocalDateTime createTime;

}
```



### 创建Repository

```java
package local.ateng.java.jpa.repository;

import local.ateng.java.jpa.entity.MyUser;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

// UserRepository 扩展 JpaRepository
public interface MyUserRepository extends JpaRepository<MyUser, Long> {

    @Modifying
    @Query(value = "TRUNCATE TABLE my_user", nativeQuery = true)
    void truncateTable();

    @Query("SELECT u FROM my_user u " +
            "WHERE (:name IS NULL OR :name = '' OR u.name LIKE :name) " +
            "AND (:age IS NULL OR u.age > :age)")
    List<MyUser> findCustomList(@Param("name") String name, @Param("age") Integer age);

    @Query("SELECT u FROM my_user u WHERE u.id = :id")
    MyUser findCustomOne(@Param("id") Long id);
}
```



### 创建Service

**创建interface**

```java
package local.ateng.java.jpa.service;

import local.ateng.java.jpa.entity.MyUser;

import java.util.List;

public interface MyUserService {

    void save(MyUser myUser);

    void saveAll(List<MyUser> myUsers);

    void update(MyUser myUser);

    void deleteById(Long id);

    List<MyUser> findAll();

    MyUser findById(Long id);

    void truncate();

    MyUser findCustomOne(Long id);

    List<MyUser> findCustomList(String name, Integer age);

}
```

**创建interface impl**

```java
package local.ateng.java.jpa.service.impl;

import local.ateng.java.jpa.entity.MyUser;
import local.ateng.java.jpa.repository.MyUserRepository;
import local.ateng.java.jpa.service.MyUserService;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor(onConstructor = @__(@Autowired))
public class MyUserServiceImpl implements MyUserService {
    private final MyUserRepository myUserRepository;


    @Override
    public void save(MyUser myUser) {
        MyUser user = myUserRepository.save(myUser);
        System.out.println(user);
    }

    @Override
    public void saveAll(List<MyUser> myUsers) {
        myUserRepository.saveAll(myUsers);
    }

    @Override
    public void update(MyUser myUser) {
        myUserRepository.save(myUser);
    }

    @Override
    public void deleteById(Long id) {
        myUserRepository.deleteById(id);
    }

    @Override
    public List<MyUser> findAll() {
        return myUserRepository.findAll();
    }

    @Override
    public MyUser findById(Long id) {
        return myUserRepository.findById(id).get();
    }

    @Override
    @Transactional
    public void truncate() {
        myUserRepository.truncateTable();
    }

    @Override
    public MyUser findCustomOne(Long id) {
        return myUserRepository.findCustomOne(id);
    }

    @Override
    public List<MyUser> findCustomList(String name, Integer age) {
        return myUserRepository.findCustomList(name, age);
    }
}
```



## 使用JPA

### 创建测试类

```java
package local.ateng.java.jpa;

import local.ateng.java.jpa.entity.MyUser;
import local.ateng.java.jpa.service.MyUserService;
import lombok.RequiredArgsConstructor;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@SpringBootTest
@RequiredArgsConstructor(onConstructor = @__(@Autowired))
public class JpaTests {
    private final MyUserService myUserService;

}
```

### 保存数据

```java
    @Test
    public void test01() {
        // 保存数据
        myUserService.save(new MyUser(null, "jpa", 24, 1.1, LocalDateTime.now(),"重庆","重庆",LocalDateTime.now()));
    }
```

### 查看数据

```java
    @Test
    public void test02() {
        // 查看数据
        List<MyUser> list = myUserService.findAll();
        System.out.println(list);
    }
    @Test
    public void test03() {
        // 根据id查询数据
        MyUser myUser = myUserService.findById(1L);
        System.out.println(myUser);
    }
```

### 保存或更新数据

```java
    @Test
    public void test04() {
        // 根据数据保存或更新数据
        myUserService.update(new MyUser(1L, "阿腾2", 24, 1.1, LocalDateTime.now(),"重庆","重庆",LocalDateTime.now()));
    }
```

### 删除数据

```java
    @Test
    public void test05() {
        // 删除数据
        myUserService.deleteById(1000L);
    }
```

### 批量保存数据

```java
    @Test
    public void test06() {
        // 批量保存数据
        ArrayList<MyUser> list = new ArrayList<>();
        list.add(new MyUser(null, "jpa1", 24, 1.1, LocalDateTime.now(),"重庆","重庆",LocalDateTime.now()));
        list.add(new MyUser(null, "jpa2", 24, 1.2, LocalDateTime.now(),"重庆","重庆",LocalDateTime.now()));
        myUserService.saveAll(list);
    }
```

### 清空表

```java
    @Test
    public void test07() {
        // 清空表
        myUserService.truncate();
    }
```

### 自定义SQL查询1

```java
    @Test
    public void test08() {
        // 自定义SQL查询，一个条件参数和返回一个值
        MyUser myUser = myUserService.findCustomOne(1L);
        System.out.println(myUser);
    }
```

### 批量保存数据

```java
    @Test
    public void test09() {
        // 自定义SQL查询，多个条件参数和返回列表
        List<MyUser> userList = myUserService.findCustomList("jpa%", 18);
        System.out.println(userList);
    }
```

