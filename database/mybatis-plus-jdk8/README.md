# MyBatis Plus

[MyBatis-Plus](https://github.com/baomidou/mybatis-plus) 是一个 [MyBatis](https://www.mybatis.org/mybatis-3/) 的增强工具，在 MyBatis 的基础上只做增强不做改变，为简化开发、提高效率而生。

- [官网地址](https://baomidou.com/introduce/)

**特性**

- **无侵入**：只做增强不做改变，引入它不会对现有工程产生影响，如丝般顺滑
- **损耗小**：启动即会自动注入基本 CURD，性能基本无损耗，直接面向对象操作
- **强大的 CRUD 操作**：内置通用 Mapper、通用 Service，仅仅通过少量配置即可实现单表大部分 CRUD 操作，更有强大的条件构造器，满足各类使用需求
- **支持 Lambda 形式调用**：通过 Lambda 表达式，方便的编写各类查询条件，无需再担心字段写错
- **支持主键自动生成**：支持多达 4 种主键策略（内含分布式唯一 ID 生成器 - Sequence），可自由配置，完美解决主键问题
- **支持 ActiveRecord 模式**：支持 ActiveRecord 形式调用，实体类只需继承 Model 类即可进行强大的 CRUD 操作
- **支持自定义全局通用操作**：支持全局通用方法注入（ Write once, use anywhere ）
- **内置代码生成器**：采用代码或者 Maven 插件可快速生成 Mapper 、 Model 、 Service 、 Controller 层代码，支持模板引擎，更有超多自定义配置等您来使用
- **内置分页插件**：基于 MyBatis 物理分页，开发者无需关心具体操作，配置好插件之后，写分页等同于普通 List 查询
- **分页插件支持多种数据库**：支持 MySQL、MariaDB、Oracle、DB2、H2、HSQL、SQLite、Postgre、SQLServer 等多种数据库
- **内置性能分析插件**：可输出 SQL 语句以及其执行时间，建议开发测试时启用该功能，能快速揪出慢查询
- **内置全局拦截插件**：提供全表 delete 、 update 操作智能分析阻断，也可自定义拦截规则，预防误操作



## 基础配置

### 添加依赖

参考官网链接：[地址](https://mybatis-flex.com/zh/intro/maven.html)

#### 添加属性

```xml
    <!-- 项目属性 -->
    <properties>
        <mybatis-plus.version>3.5.10</mybatis-plus.version>
        <druid.version>1.2.24</druid.version>
    </properties>
```

#### 添加依赖管理

```xml
    <!-- Spring Boot 依赖管理 -->
    <dependencyManagement>
        <dependencies>
            <!-- MyBatis Plus 依赖管理 -->
            <dependency>
                <groupId>com.baomidou</groupId>
                <artifactId>mybatis-plus-bom</artifactId>
                <version>${mybatis-plus.version}</version>
                <type>pom</type>
                <scope>import</scope>
            </dependency>
        </dependencies>
    </dependencyManagement>
```

#### 添加依赖

```xml
        <!-- Mybatis Plus 数据库框架 -->
        <dependency>
            <groupId>com.baomidou</groupId>
            <artifactId>mybatis-plus-spring-boot3-starter</artifactId>
        </dependency>
        <!-- MyBatis Plus 增加工具包 -->
        <dependency>
            <groupId>com.baomidou</groupId>
            <artifactId>mybatis-plus-jsqlparser</artifactId>
        </dependency>
        <!-- MyBatis Plus 代码生成器 -->
        <dependency>
            <groupId>com.baomidou</groupId>
            <artifactId>mybatis-plus-generator</artifactId>
        </dependency>
        <!-- 代码生成器的模板引擎 VelocityTemplateEngine -->
        <dependency>
            <groupId>org.apache.velocity</groupId>
            <artifactId>velocity-engine-core</artifactId>
            <version>2.3</version>
        </dependency>
        <!-- 代码生成器的模板引擎 FreemarkerTemplateEngine -->
        <!--<dependency>
            <groupId>org.freemarker</groupId>
            <artifactId>freemarker</artifactId>
            <version>2.3.31</version>
        </dependency>-->
        <!-- MyBatis-Plus SQL分析与打印功能 -->
        <!-- 有性能损耗，在生产环境中谨慎使用 -->
        <dependency>
            <groupId>com.github.gavlyukovskiy</groupId>
            <artifactId>p6spy-spring-boot-starter</artifactId>
            <version>1.10.0</version>
        </dependency>
```

#### 添加数据源依赖

以下任选一种数据库即可

- HikariCP

Mybatis-Plus依赖中默认已经包含了该依赖（在spring-boot-starter-jdbc中）

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

#### 添加数据库驱动

根据实际情况选择数据库驱动

- MySQL

URL: jdbc:mysql://192.168.1.10:35725/kongyu

```xml
<!-- MySQL数据库驱动 -->
<dependency>
    <groupId>com.mysql</groupId>
    <artifactId>mysql-connector-j</artifactId>
</dependency>
```

- PostgreSQL

URL: jdbc:postgresql://192.168.1.10:32297/kongyu?currentSchema=public&stringtype=unspecified

```xml
<!-- Postgresql数据库驱动 -->
<dependency>
    <groupId>org.postgresql</groupId>
    <artifactId>postgresql</artifactId>
</dependency>
```

- 达梦数据库

jdbc:dm://10.1.244.201:20026?schema=traffic_visualize&compatibleMode=mysql&characterEncoding=UTF-8&useUnicode=true&useSSL=false&tinyInt1isBit=false&allowPublicKeyRetrieval=true&serverTimezone=Asia/Shanghai

```xml
<!-- 达梦数据库驱动 -->
<dependency>
    <groupId>com.dameng</groupId>
    <artifactId>DmJdbcDriver18</artifactId>
    <version>8.1.3.140</version>
</dependency>
```



### 编辑配置文件

编辑 `application.yml` 配置文件

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
# Mybatis Plus的配置 https://baomidou.com/reference
mybatis-plus:
  global-config:
    banner: false
  configuration:
    log-impl: org.apache.ibatis.logging.nologging.NoLoggingImpl
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



### 编辑 `Configuration` 文件

#### 创建配置

**创建 `MyBatisPlusConfiguration`**

分页插件的DbType需要根据实际数据库类型填写，我这里是MySQL

```java
package local.ateng.java.mybatis.config;

import com.baomidou.mybatisplus.annotation.DbType;
import com.baomidou.mybatisplus.extension.plugins.MybatisPlusInterceptor;
import com.baomidou.mybatisplus.extension.plugins.inner.PaginationInnerInterceptor;
import org.mybatis.spring.annotation.MapperScan;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@MapperScan("local.ateng.java.mybatis.**.mapper")
public class MyBatisPlusConfiguration {

    /**
     * 注册 MyBatis-Plus 拦截器。
     * <p>
     * 该拦截器支持分页、乐观锁、防全表更新删除等功能。
     * 当前仅启用分页插件。
     * </p>
     * <p>
     * ⚠️ 注意：MyBatis-Plus 3.5 中，多个 InnerInterceptor 的执行顺序
     * 与注册顺序一致。分页插件会修改 SQL 以实现分页功能，
     * 因此建议分页插件 **最后注册**，以保证其他自定义或内置插件
     * 能在原始 SQL 上执行操作，避免分页逻辑被提前应用导致异常。
     * </p>
     *
     * @return MybatisPlusInterceptor 拦截器实例
     */
    @Bean
    public MybatisPlusInterceptor mybatisPlusInterceptor() {
        MybatisPlusInterceptor interceptor = new MybatisPlusInterceptor();

        // 分页插件配置
        PaginationInnerInterceptor paginationInterceptor = new PaginationInnerInterceptor();
        // 设置数据库类型（推荐明确指定，避免推断错误）
        paginationInterceptor.setDbType(DbType.MYSQL);
        // 溢出总页数后是否进行处理，true 返回首页，false 继续请求
        paginationInterceptor.setOverflow(false);
        // 单页最大记录数，-1 表示不受限制
        paginationInterceptor.setMaxLimit(1000L);

        interceptor.addInnerInterceptor(paginationInterceptor);
        return interceptor;
    }
}
```

#### 创建代码生成器

参考官网文档：[地址](https://baomidou.com/guides/new-code-generator/)

使用的时候修改以下配置：

- 修改数据库的信息dataSource
- 需要生成的表GenerateTable
- 其他的根据实际情况修改

```java
package local.ateng.java.mybatis.utils;

import com.baomidou.mybatisplus.generator.FastAutoGenerator;
import com.baomidou.mybatisplus.generator.config.OutputFile;

import java.io.File;
import java.net.URISyntaxException;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;


/**
 * 代码生成器
 * 全新的 MyBatis-Plus 代码生成器，通过 builder 模式可以快速生成你想要的代码，快速且优雅，跟随下面的代码一睹为快。
 * https://baomidou.com/guides/new-code-generator/
 *
 * @author 孔余
 * @email 2385569970@qq.com
 * @since 2025-01-13
 */
public class MybatisPlusGenerator {
    // 根包名
    private static final String BasePackage = "local.ateng.java.mybatis";
    // 子包名，例如 ${BasePackage}.${ChildPackage} => ${BasePackage}.system
    private static final String ChildPackage = "";
    // 需要生成的表
    private static final List<String> GenerateTable = Arrays.asList(
            "my_user", "my_order"
    );

    public static void main(String[] args) {
        PathEntity path = getPath();
        FastAutoGenerator.create("jdbc:mysql://192.168.1.10:35725/kongyu", "root", "Admin@123")
                .globalConfig(builder -> builder
                        .author("Ateng")
                        .outputDir(path.getSourceDir())
                        .commentDate("yyyy-MM-dd")
                        .disableOpenDir()
                )
                .packageConfig(builder -> builder
                        .parent(path.getBasePackage())
                        .entity("entity")
                        .mapper("mapper")
                        .service("service")
                        .serviceImpl("service.impl")
                        .xml("mapper.xml")
                        .pathInfo(Collections.singletonMap(OutputFile.xml, path.getMapperXmlPath())) // 设置 Mapper XML 文件生成路径
                )
                .strategyConfig(builder -> builder
                        .addInclude(GenerateTable) // 设置需要生成的表名
                        .entityBuilder() // Entity 策略配置
                        .enableLombok() // 启用 Lombok
                        .enableTableFieldAnnotation() // 启用字段注解
                        .controllerBuilder()// Controller 策略配置
                        .enableRestStyle() // 启用 REST 风格
                )
                .execute();
    }

    /**
     * 获取当前模块的路径
     *
     * @return
     */
    public static String getModulePath() {
        // 获取当前类的路径
        String path = null;
        try {
            path = MybatisPlusGenerator.class.getProtectionDomain().getCodeSource().getLocation().toURI().getPath();
        } catch (URISyntaxException e) {
            throw new RuntimeException(e);
        }
        // 获取父目录（模块目录）
        File moduleDir = new File(path).getParentFile();
        return moduleDir.getPath().replace("\\target", "");
    }

    /**
     * 获取配置需要的路径
     */
    public static PathEntity getPath() {
        String sourceDir = getModulePath() + "/src/main/java";
        String basePath = BasePackage.replaceAll("^\\.|\\.$", "");
        String mapperPath = getModulePath() + "/src/main/resources/mapper";
        if (!ChildPackage.isBlank()) {
            basePath = basePath + "." + ChildPackage.replaceAll("^\\.|\\.$|^/|/$", "");
            mapperPath = mapperPath + "/" + ChildPackage.replaceAll("^\\.|\\.$|^/|/$", "");
        }
        return new PathEntity(sourceDir, basePath, mapperPath);
    }

    /**
     * 设置路径的类
     */
    public static class PathEntity {
        private String sourceDir;
        private String basePackage;
        private String mapperXmlPath;

        public PathEntity(String sourceDir, String basePackage, String mapperXmlPath) {
            this.sourceDir = sourceDir;
            this.basePackage = basePackage;
            this.mapperXmlPath = mapperXmlPath;
        }

        public String getSourceDir() {
            return sourceDir;
        }

        public String getBasePackage() {
            return basePackage;
        }

        public String getMapperXmlPath() {
            return mapperXmlPath;
        }
    }

}
```



## 数据库表准备

### 创建表

**创建表**

```sql
-- 用户表
create table if not exists my_user
(
    id          bigint auto_increment
        primary key comment '用户ID，主键，自增',
    name        varchar(50)                              not null comment '用户名',
    age         int                                      null comment '用户年龄，允许为空',
    score       double                                   default 0 comment '用户分数，默认为0',
    birthday    datetime(3)                              null comment '用户生日，允许为空',
    province    varchar(50)                              null comment '用户所在省份，允许为空',
    city        varchar(50)                              null comment '用户所在城市，允许为空',
    create_time datetime(3) default CURRENT_TIMESTAMP(3) not null comment '记录创建时间，默认当前时间'
) comment '用户信息表，存储用户的基本信息';

-- 订单表
create table if not exists kongyu.my_order
(
    id           bigint auto_increment
        primary key comment '订单ID，主键，自增',
    user_id      bigint         not null comment '用户ID，外键，关联用户表',
    date         date           not null comment '订单日期',
    total_amount decimal(10, 2) not null comment '订单总金额，精确到小数点后两位',
    constraint fk_my_order_user foreign key (user_id) references my_user (id) on delete cascade on update cascade
) comment '订单信息表，存储用户的订单数据';
```

**插入数据**

将项目模块下 `/data` 目录的SQL运行在数据库中



## 代码生成

配置数据库信息和需要生成的表后，运行代码生成器 `MybatisPlusGenerator` 。

生成后如下图所示：

![image-20250113151611057](./assets/image-20250113151611057.png)



## 基础查询

### 测试类准备

```java
package local.ateng.java.mybatis;

import local.ateng.java.mybatisjdk8.service.IMyOrderService;
import local.ateng.java.mybatisjdk8.service.IMyUserService;
import lombok.RequiredArgsConstructor;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;


/**
 * 基础查询
 *
 * @author 孔余
 * @email 2385569970@qq.com
 * @since 2025-01-10
 */
@SpringBootTest
@RequiredArgsConstructor(onConstructor = @__(@Autowired))
public class BasicQueryTests {
    private final IMyUserService myUserService;
    private final IMyOrderService myOrderService;

    @Test
    void test() {
        long count = myUserService.count();
        System.out.println(count);
    }

}
```

### 条件查询

**查询id是88的一条数据**

```java
    @Test
    void test01() {
        // 查询id是88的一条数据
        MyUser user = myUserService.lambdaQuery()
                .eq(MyUser::getId, 88)
                .one();
        System.out.println(user);
    }
```

**查询id是88到90(包含)这个范围内的数据**

```java
    @Test
    void test02() {
        // 查询id是88到90(包含)这个范围内的数据
        List<MyUser> userList = myUserService.lambdaQuery()
                .between(MyUser::getId, 88, 90)
                .list();
        System.out.println(userList);
    }
```

**查询所有的区域**

```java
    @Test
    void test03() {
        // 查询所有的区域
        List<MyUser> list = myUserService.query()
                .select("DISTINCT city")
                .list();
        System.out.println(list.stream().map(MyUser::getCity).toList());
    }
```

**查询创建时间是2024年8月的数据数量**

```java
    @Test
    void test04() {
        // 查询创建时间是2024年8月的数据数量
        QueryWrapper<MyUser> wrapper = new QueryWrapper<MyUser>()
                .select("DATE_FORMAT(create_time, '%Y-%m') as month", "COUNT(*) as count")
                .groupBy("DATE_FORMAT(create_time, '%Y-%m')")
                .having("month = '2025-01'");
        List<Map<String, Object>> list = myUserService.listMaps(wrapper);
        System.out.println(list);
    }
```

**查询并按照创建时间排序(降序)，创建时间一样则按照id排序(降序)**

```java
    @Test
    void test05() {
        // 查询并按照创建时间排序(降序)，创建时间一样则按照id排序(降序)
        List<MyUser> userList = myUserService.lambdaQuery()
                .between(MyUser::getId, 88, 90)
                .orderByDesc(MyUser::getCreateTime, MyUser::getId)
                .list();
        System.out.println(userList);
    }
```

**分页查询**

```java
    @Test
    void test06() {
        // 引入 MyBatis-Plus 分页插件
        Page<MyUser> page = new Page<>(2, 10);  // 第2页，每页10条记录
        // 分页查询
        page = myUserService.lambdaQuery()
                .between(MyUser::getId, 88, 888)
                .page(page);
        // 获取分页结果
        List<MyUser> users = page.getRecords();  // 分页数据
        long total = page.getTotal();  // 总记录数
        long pages = page.getPages();  // 总页数
        // 输出查询结果
        System.out.println(page);
        System.out.println("Total: " + total);
        System.out.println("Pages: " + pages);
        users.forEach(user -> System.out.println(user));
    }
```



## JSON字段

### 创建表

创建表

```sql
drop table if exists my_json;
create table my_json
(
    id           bigint auto_increment primary key,
    name         varchar(16) not null comment '名称',
    my_json_object json comment 'JSONObject数据',
    my_json_array  json comment 'JSONOArray数据'
) comment 'Json表';
```

写入数据

```sql
 INSERT INTO my_json (name, my_json_object, my_json_array) VALUES
 ('Alice',  '{"age": 25, "city": "Shanghai"}',  '["reading", "cycling"]'),
 ('Bob',    '{"age": 30, "city": "Beijing"}',   '["chess", "music"]'),
 ('Charlie', '{"age": 35, "city": "Guangzhou"}', '["food", "travel"]'),
 ('Diana',  '{"age": 40, "city": "Shenzhen"}',  '["movie", "art"]');
```

### 使用IService的方式

需要 实体类配置 的注解属性

使用 `MybatisPlusGenerator` 生成代码，然后修改实体类的JSON字段配置

#### @TableName配置

添加 `autoResultMap = true`

```
@TableName(value = "my_json", autoResultMap = true)
```

#### @TableField配置

添加 `typeHandler = JacksonTypeHandler.class` 或者 `Fastjson2TypeHandler.class`

```
@TableField(value = "my_json_object", typeHandler = JacksonTypeHandler.class)
```

### 使用Mapper XML的方式

```xml
<?xml version="1.0" encoding="UTF-8"?>
<!DOCTYPE mapper PUBLIC "-//mybatis.org//DTD Mapper 3.0//EN" "http://mybatis.org/dtd/mybatis-3-mapper.dtd">
<mapper namespace="local.ateng.java.mybatisjdk8.mapper.MyJsonMapper">

    <resultMap id="myJsonResultMap" type="local.ateng.java.mybatisjdk8.entity.MyJson">
        <result column="my_json_object" property="myJsonObject"
                typeHandler="com.baomidou.mybatisplus.extension.handlers.JacksonTypeHandler"/>
        <result column="my_json_array" property="myJsonArray"
                typeHandler="com.baomidou.mybatisplus.extension.handlers.JacksonTypeHandler"/>
    </resultMap>

    <select id="selectMyJson" resultMap="myJsonResultMap">
        select id, name, my_json_object, my_json_array from my_json;
    </select>
</mapper>
```

## 枚举字段

### 创建枚举

根据框架使用的序列化来选择：

- Jackson 版本
- Fastjson1 版本

- Fastjson2 版本

```java
import com.alibaba.fastjson.annotation.JSONField;
import com.baomidou.mybatisplus.annotation.EnumValue;
import lombok.AllArgsConstructor;
import lombok.Getter;

@AllArgsConstructor
@Getter
public enum StatusEnum {

    OFFLINE(0, "离线"),
    ONLINE(1, "在线");

    @EnumValue
    private final int code;
    //@JsonValue // Jackson
    //@JSONField(value = true) // Fastjson2
    private final String name;

    @JSONField // Fastjson1
    public String getValue() {
        return this.name;
    }

}
```

#### Jackson 版本

```java
package local.ateng.java.mybatisjdk8.enums;

import com.baomidou.mybatisplus.annotation.EnumValue;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;

/**
 * 状态枚举（支持 MyBatis-Plus 与 Jackson 序列化、反序列化）。
 *
 * <p>主要功能：
 * <ul>
 *     <li>通过 {@link EnumValue} 注解，支持 MyBatis-Plus 将枚举存储到数据库时使用 {@code code} 字段。</li>
 *     <li>通过 {@link JsonValue} 注解，支持 Jackson 在序列化枚举时输出 {@code name} 字段。</li>
 *     <li>通过 {@link JsonCreator} 注解，支持 Jackson 在反序列化时根据 {@code code} 自动映射枚举。</li>
 * </ul>
 *
 * <p>适用场景：
 * <ul>
 *     <li>数据库中存储数值型状态码。</li>
 *     <li>接口返回时需要输出中文或自定义描述。</li>
 * </ul>
 * </p>
 *
 * @author 孔余
 * @since 2025-08-25
 */
public enum StatusEnumJackson {

    /**
     * 离线状态。
     */
    OFFLINE(0, "离线"),

    /**
     * 在线状态。
     */
    ONLINE(1, "在线");

    /**
     * 枚举对应的数据库存储值。
     *
     * <p>该值与数据库字段绑定，通常为数值型标识。</p>
     */
    @EnumValue
    private final Integer code;

    /**
     * 枚举的展示名称。
     *
     * <p>在 Jackson 中，若字段或方法标注 {@link JsonValue}，
     * 则该值在序列化时会作为枚举的 JSON 输出。</p>
     *
     * <p>示例：
     * <pre>
     *     StatusEnumJackson.ONLINE  -> "在线"
     *     StatusEnumJackson.OFFLINE -> "离线"
     * </pre>
     * </p>
     */
    @JsonValue
    private final String name;

    StatusEnumJackson(int code, String name) {
        this.code = code;
        this.name = name;
    }

    /**
     * 获取枚举对应的存储值。
     *
     * @return 数据库存储的数值型标识
     */
    public Integer getCode() {
        return this.code;
    }

    /**
     * 获取枚举的展示名称。
     *
     * <p>在 Jackson 中，该方法通常不会直接影响序列化结果，
     * 因为 {@link JsonValue} 已经标注在 {@code name} 字段上。</p>
     *
     * @return 枚举展示名称（中文）
     */
    public String getName() {
        return this.name;
    }

    /**
     * 根据存储值反序列化为枚举。
     *
     * <p>配合 {@link JsonCreator} 使用，Jackson 在反序列化时会调用该方法。</p>
     *
     * <p>示例：
     * <pre>
     *     // JSON: {"status":1} -> ONLINE
     *     StatusEnumJackson.fromCode(1); // ONLINE
     *
     *     // JSON: {"status":0} -> OFFLINE
     *     StatusEnumJackson.fromCode(0); // OFFLINE
     *
     *     // JSON: {"status":99} -> null
     *     StatusEnumJackson.fromCode(99); // null
     * </pre>
     * </p>
     *
     * @param code 数值型标识，可能为 null 或不在定义范围
     * @return 对应的枚举常量；未匹配时返回 null（可根据业务修改为默认值）
     */
    @JsonCreator
    public static StatusEnumJackson fromCode(Integer code) {
        if (code == null) {
            return null;
        }
        for (StatusEnumJackson e : values()) {
            if (e.code.equals(code)) {
                return e;
            }
        }
        return null;
    }

}
```

#### Fastjson1 版本

```java
package local.ateng.java.mybatisjdk8.enums;

import com.alibaba.fastjson.annotation.JSONCreator;
import com.alibaba.fastjson.annotation.JSONField;
import com.baomidou.mybatisplus.annotation.EnumValue;

/**
 * 状态枚举（支持 MyBatis-Plus 与 Fastjson1 序列化、反序列化）。
 *
 * <p>主要功能：
 * <ul>
 *     <li>通过 {@link EnumValue} 注解，支持 MyBatis-Plus 将枚举存储到数据库时使用 {@code code} 字段。</li>
 *     <li>通过 {@link JSONField} 注解，支持 Fastjson1 在序列化枚举时输出 {@code name} 字段。</li>
 *     <li>通过 {@link JSONCreator} 注解，支持 Fastjson1 在反序列化时根据 {@code code} 自动映射枚举。</li>
 * </ul>
 *
 * <p>适用场景：
 * <ul>
 *     <li>数据库中存储数值型状态码。</li>
 *     <li>接口返回时需要输出中文或自定义描述。</li>
 * </ul>
 * </p>
 *
 * @author 孔余
 * @since 2025-08-25
 */
public enum StatusEnumFastjson1 {

    /**
     * 离线状态。
     */
    OFFLINE(0, "离线"),

    /**
     * 在线状态。
     */
    ONLINE(1, "在线");

    /**
     * 枚举对应的数据库存储值。
     *
     * <p>该值与数据库字段绑定，通常为数值型标识。</p>
     */
    @EnumValue
    private final Integer code;

    /**
     * 枚举的展示名称。
     *
     * <p>该值作为接口返回时的中文描述，或前端显示的名称。</p>
     */
    private final String name;

    StatusEnumFastjson1(int code, String name) {
        this.code = code;
        this.name = name;
    }

    /**
     * 获取枚举对应的存储值。
     *
     * @return 数据库存储的数值型标识
     */
    public Integer getCode() {
        return this.code;
    }

    /**
     * 获取枚举的展示名称。
     *
     * <p>配合 {@link JSONField} 使用，Fastjson1 在序列化时会输出此值。</p>
     *
     * <p>示例：
     * <pre>
     *     StatusEnumFastjson1.ONLINE  -> "在线"
     *     StatusEnumFastjson1.OFFLINE -> "离线"
     * </pre>
     * </p>
     *
     * @return 枚举展示名称（中文）
     */
    @JSONField
    public String getName() {
        return this.name;
    }

    /**
     * 根据存储值反序列化为枚举。
     *
     * <p>配合 {@link JSONCreator} 使用，Fastjson1 在反序列化时会调用该方法。</p>
     *
     * <p>示例：
     * <pre>
     *     // JSON: {"status":1} -> ONLINE
     *     StatusEnumFastjson1.fromCode(1); // ONLINE
     *
     *     // JSON: {"status":0} -> OFFLINE
     *     StatusEnumFastjson1.fromCode(0); // OFFLINE
     *
     *     // JSON: {"status":99} -> null
     *     StatusEnumFastjson1.fromCode(99); // null
     * </pre>
     * </p>
     *
     * @param code 数值型标识，可能为 null 或不在定义范围
     * @return 对应的枚举常量；未匹配时返回 null（可根据业务修改为默认值）
     */
    @JSONCreator
    public static StatusEnumFastjson1 fromCode(Integer code) {
        if (code == null) {
            return null;
        }
        for (StatusEnumFastjson1 e : values()) {
            if (e.code.equals(code)) {
                return e;
            }
        }
        return null;
    }

}
```

#### Fastjson2 版本

```java
package local.ateng.java.mybatisjdk8.enums;

import com.alibaba.fastjson2.annotation.JSONCreator;
import com.alibaba.fastjson2.annotation.JSONField;
import com.baomidou.mybatisplus.annotation.EnumValue;

/**
 * 状态枚举（支持 MyBatis-Plus 与 Fastjson2 序列化、反序列化）。
 *
 * <p>主要功能：
 * <ul>
 *     <li>通过 {@link EnumValue} 注解，支持 MyBatis-Plus 将枚举存储到数据库时使用 {@code code} 字段。</li>
 *     <li>通过 {@link JSONField} 注解，支持 Fastjson2 在序列化枚举时输出 {@code name} 字段。</li>
 *     <li>通过 {@link JSONCreator} 注解，支持 Fastjson2 在反序列化时根据 {@code code} 自动映射枚举。</li>
 * </ul>
 *
 * <p>适用场景：
 * <ul>
 *     <li>数据库中存储数值型状态码。</li>
 *     <li>接口返回时需要输出中文或自定义描述。</li>
 * </ul>
 * </p>
 *
 * @author 孔余
 * @since 2025-08-25
 */
public enum StatusEnumFastjson2 {

    /**
     * 离线状态。
     */
    OFFLINE(0, "离线"),

    /**
     * 在线状态。
     */
    ONLINE(1, "在线");

    /**
     * 枚举对应的数据库存储值。
     *
     * <p>该值与数据库字段绑定，通常为数值型标识。</p>
     */
    @EnumValue
    private final Integer code;

    /**
     * 枚举的展示名称。
     *
     * <p>该值作为接口返回时的中文描述，或前端显示的名称。</p>
     *
     * <p>在 Fastjson2 中，若字段上标记 {@code @JSONField(value = true)}，
     * 则该字段在序列化时会作为枚举的输出值。</p>
     */
    @JSONField(value = true)
    private final String name;

    StatusEnumFastjson2(int code, String name) {
        this.code = code;
        this.name = name;
    }

    /**
     * 获取枚举对应的存储值。
     *
     * @return 数据库存储的数值型标识
     */
    public Integer getCode() {
        return this.code;
    }

    /**
     * 获取枚举的展示名称。
     *
     * <p>在 Fastjson2 中，该方法通常不会直接影响序列化结果，
     * 因为 {@link JSONField} 已经标注在 {@code name} 字段上。</p>
     *
     * @return 枚举展示名称（中文）
     */
    public String getName() {
        return this.name;
    }

    /**
     * 根据存储值反序列化为枚举。
     *
     * <p>配合 {@link JSONCreator} 使用，Fastjson2 在反序列化时会调用该方法。</p>
     *
     * <p>示例：
     * <pre>
     *     // JSON: {"status":1} -> ONLINE
     *     StatusEnumFastjson2.fromCode(1); // ONLINE
     *
     *     // JSON: {"status":0} -> OFFLINE
     *     StatusEnumFastjson2.fromCode(0); // OFFLINE
     *
     *     // JSON: {"status":99} -> null
     *     StatusEnumFastjson2.fromCode(99); // null
     * </pre>
     * </p>
     *
     * @param code 数值型标识，可能为 null 或不在定义范围
     * @return 对应的枚举常量；未匹配时返回 null（可根据业务修改为默认值）
     */
    @JSONCreator
    public static StatusEnumFastjson2 fromCode(Integer code) {
        if (code == null) {
            return null;
        }
        for (StatusEnumFastjson2 e : values()) {
            if (e.code.equals(code)) {
                return e;
            }
        }
        return null;
    }

}
```

### 数据库实体字段

直接将枚举字段的类型修改为相关的枚举，这样新增到数据库时，实际存入的是枚举的code，通过接口将数据序列化出去返回的事枚举的name。

```java
@TableField("status")
private StatusEnum status;
```

### 枚举字典

通过 `EnumUtil.getAllBaseEnumMap()` 方法获取到相关枚举的字典，返回给前端

### 数据流总结

| 流程            | 数据类型       | 说明                      |
| --------------- | -------------- | ------------------------- |
| 后端 → 前端展示 | name（字符串） | 用户看到可读标签          |
| 前端下拉选择    | code（整数）   | 用户选择后，前端提交 code |
| 前端 → 后端提交 | code（整数）   | MyBatis-Plus 存入数据库   |
| 数据库存储      | code（整数）   | 枚举在 DB 中存 code       |
| GET 接口返回    | name（字符串） | 方便展示                  |



## 多数据源

参考官网文档：[地址](https://github.com/baomidou/dynamic-datasource)

### 添加依赖

```xml
<!-- MyBatis Plus 多数据源 -->
<dependency>
    <groupId>com.baomidou</groupId>
    <artifactId>dynamic-datasource-spring-boot3-starter</artifactId>
    <version>4.3.1</version>
</dependency>
```

### 编辑配置文件

**编辑 `application.yml` 添加多数据源配置**

原有的spring.datasource可以注释

```yaml
---
# 数据库的相关配置
#spring:
#  datasource:
#    url: jdbc:mysql://192.168.1.10:35725/kongyu  # MySQL数据库连接URL
#    #url: jdbc:postgresql://192.168.1.10:32297/kongyu?currentSchema=public&stringtype=unspecified  # PostgreSQL数据库连接URL
#    username: root  # 数据库用户名
#    password: Admin@123  # 数据库密码
#    # driver-class-name: com.mysql.cj.jdbc.Driver  # 数据库驱动类，框架会自动适配
#    type: com.alibaba.druid.pool.DruidDataSource  # 使用Druid数据源
#    # Druid连接池配置 https://github.com/alibaba/druid/tree/master/druid-spring-boot-starter
#    druid:
#      initial-size: 10  # 初始化连接池大小
#      min-idle: 10  # 最小空闲连接数
#      max-active: 1000  # 最大活跃连接数
#      max-wait: 10000  # 获取连接的最大等待时间，单位毫秒
#      async-init: true
spring:
  datasource:
    dynamic:
      primary: mysql
      strict: false
      datasource:
        mysql:
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
        doris:
          type: com.alibaba.druid.pool.DruidDataSource
          url: jdbc:mysql://192.168.1.12:9030/kongyu
          username: admin
          password: Admin@123
          # Druid连接池配置 https://github.com/alibaba/druid/tree/master/druid-spring-boot-starter
          druid:
            initial-size: 10  # 初始化连接池大小
            min-idle: 10  # 最小空闲连接数
            max-active: 1000  # 最大活跃连接数
            max-wait: 10000  # 获取连接的最大等待时间，单位毫秒
            async-init: true
```

### 使用多数据源

**创建测试类使用第二个指定的数据源**

执行代码后输出的内容就是Doris中表的数据，详细使用参考[官方文档](https://github.com/baomidou/dynamic-datasource)

```java
@Service
@DS("slave")
public class UserServiceImpl implements UserService {

    @Autowired
    private JdbcTemplate jdbcTemplate;

    public List selectAll() {
        return jdbcTemplate.queryForList("select * from user");
    }

    @Override
    @DS("slave_1")
    public List selectByCondition() {
        return jdbcTemplate.queryForList("select * from user where age >10");
    }
}
```



## 使用Mapper XML

### 开始

#### ${ew.sqlSegment} 和 ${ew.customSqlSegment} 区别

- **普通 XML 自定义分页、多条件查询** → 用 `${ew.customSqlSegment}`（简单、省心）
- **复杂 SQL、JOIN 场景中部分条件需要自己控制位置** → 用 `${ew.sqlSegment}`（更灵活）

| 特性                   | `ew.sqlSegment`       | `ew.customSqlSegment` |
| ---------------------- | --------------------- | --------------------- |
| 是否包含 `WHERE`/`AND` | ❌ 否                  | ✅ 自动加              |
| 是否包含排序、分组     | ❌ 一般不包含          | ✅ 包含                |
| 是否包含逻辑删除条件   | ✅ 包含                | ✅ 包含                |
| 适用场景               | 自己完全控制 SQL 结构 | 快速接在 FROM 后面    |



### 基本使用

#### 创建Mapper

```java
package local.ateng.java.mybatis.mapper;

import com.alibaba.fastjson2.JSONObject;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import local.ateng.java.mybatisjdk8.entity.MyUser;
import org.apache.ibatis.annotations.Param;

import java.util.List;


/**
 * <p>
 * 用户信息表，存储用户的基本信息 Mapper 接口
 * </p>
 *
 * @author 孔余
 * @since 2025-01-13
 */
public interface MyUserMapper extends BaseMapper<MyUser> {

    List<MyUser> selectAllUsers();

    MyUser selectUserById(@Param("id") Long id);

    // 根据查询条件获取用户及其订单信息
    List<JSONObject> selectUsersWithOrders(@Param("orderId") Long orderId);

}
```

#### 创建Mapper.xml

```java
<?xml version="1.0" encoding="UTF-8"?>
<!DOCTYPE mapper PUBLIC "-//mybatis.org//DTD Mapper 3.0//EN" "http://mybatis.org/dtd/mybatis-3-mapper.dtd">
<mapper namespace="local.ateng.java.mybatis.mapper.MyUserMapper">

    <!-- 查询所有用户 -->
    <select id="selectAllUsers" resultType="local.ateng.java.mybatis.entity.MyUser">
        SELECT * FROM my_user;
    </select>

    <!-- 根据ID查询用户 -->
    <select id="selectUserById" parameterType="java.lang.Long" resultType="local.ateng.java.mybatis.entity.MyUser">
        SELECT * FROM my_user WHERE id = #{id};
    </select>

    <!-- 查询所有用户及其对应的订单信息 -->
    <select id="selectUsersWithOrders" resultType="com.alibaba.fastjson2.JSONObject">
        SELECT
            u.id as id,
            u.name,
            u.age,
            u.score,
            u.birthday,
            u.province,
            u.city,
            u.create_time,
            o.id as order_id,
            o.date as order_date,
            o.total_amount as order_total_amount
        FROM my_user u
        LEFT JOIN my_order o ON u.id = o.user_id
        WHERE 1=1
            <if test="orderId != null">AND o.id = #{orderId}</if>
    </select>

</mapper>
```

#### 测试使用

```java
package local.ateng.java.mybatis;

import com.alibaba.fastjson2.JSONObject;
import local.ateng.java.mybatisjdk8.entity.MyUser;
import local.ateng.java.mybatisjdk8.mapper.MyUserMapper;
import lombok.RequiredArgsConstructor;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.List;

@SpringBootTest
@RequiredArgsConstructor(onConstructor = @__(@Autowired))
public class MapperTests {
    private final MyUserMapper myUserMapper;

    @Test
    void test01() {
        List<MyUser> list = myUserMapper.selectAllUsers();
        System.out.println(list);
    }

    @Test
    void test02() {
        MyUser myUser = myUserMapper.selectUserById(1L);
        System.out.println(myUser);
    }

    @Test
    void test03() {
        List<JSONObject> list = myUserMapper.selectUsersWithOrders(1L);
        System.out.println(list);
    }
}
```

### 分页查询

#### 创建Mapper

- **定义**：在 Mapper 方法中传入 `Page` 对象和查询参数，实现分页查询。

```java
public interface MyUserMapper extends BaseMapper<MyUser> {

    // 分页查询
    IPage<JSONObject> selectUsersWithOrderPage(Page page, @Param("city") String city);
}
```

**执行过程**：

1. **自动执行总数查询**（`COUNT`），用于获取满足条件的总记录数。
2. **执行带 `LIMIT` 的分页查询**，返回当前页数据。

**原理**：
 MyBatis-Plus 内置分页拦截器自动拦截查询，先执行总数查询，再追加分页 SQL（`LIMIT offset, size`），最后封装为 `IPage` 对象返回。

**返回值**：
 `IPage` 包含当前页数据列表、总记录数、总页数等信息，方便分页展示和逻辑处理。

**优势**：
 免写复杂分页 SQL，减少错误，提升开发效率。

#### 创建Mapper.xml

```xml
<?xml version="1.0" encoding="UTF-8"?>
<!DOCTYPE mapper PUBLIC "-//mybatis.org//DTD Mapper 3.0//EN" "http://mybatis.org/dtd/mybatis-3-mapper.dtd">
<mapper namespace="local.ateng.java.mybatisjdk8.mapper.MyUserMapper">

    <select id="selectUsersWithOrderPage" resultType="com.alibaba.fastjson2.JSONObject">
        SELECT
        u.id as id,
        u.name,
        u.age,
        u.score,
        u.birthday,
        u.province,
        u.city,
        u.create_time,
        o.id as order_id,
        o.date as order_date,
        o.total_amount as order_total_amount
        FROM my_user u
        LEFT JOIN my_order o ON u.id = o.user_id
        <where>
            <if test="city != null">
                u.city like concat('%', #{city}, '%')
            </if>
        </where>
    </select>

</mapper>

```

#### 测试使用

```java
@Test
void test05() {
    IPage<JSONObject> page = myUserMapper.selectUsersWithOrderPage(new Page(1, 20), "重");
    System.out.println(page);
}
```

输出内容

```
2025-06-16T21:08:33.640+08:00  INFO 37408 --- [mybatis-plus] [           main] p6spy                                    : #1750079313640 | took 18ms | statement | connection 0| url jdbc:mysql://192.168.1.10:35725/kongyu
SELECT COUNT(*) AS total FROM my_user u WHERE u.city LIKE concat('%', ?, '%')
SELECT COUNT(*) AS total FROM my_user u WHERE u.city LIKE concat('%', '重', '%');
2025-06-16T21:08:33.661+08:00  INFO 37408 --- [mybatis-plus] [           main] p6spy                                    : #1750079313661 | took 5ms | statement | connection 0| url jdbc:mysql://192.168.1.10:35725/kongyu
SELECT
        u.id as id,
        u.name,
        u.age,
        u.score,
        u.birthday,
        u.province,
        u.city,
        u.create_time,
        o.id as order_id,
        o.date as order_date,
        o.total_amount as order_total_amount
        FROM my_user u
        LEFT JOIN my_order o ON u.id = o.user_id
         WHERE u.city like concat('%', ?, '%') LIMIT ?
SELECT
        u.id as id,
        u.name,
        u.age,
        u.score,
        u.birthday,
        u.province,
        u.city,
        u.create_time,
        o.id as order_id,
        o.date as order_date,
        o.total_amount as order_total_amount
        FROM my_user u
        LEFT JOIN my_order o ON u.id = o.user_id
         WHERE u.city like concat('%', '重', '%') LIMIT 3;
Page{records=[{"id":1,"name":"阿腾","age":25,"score":99.99,"birthday":"2025-01-24 00:00:00","province":"重庆","city":"重庆","create_time":"2025-01-24 22:33:08.822","order_id":542,"order_date":"2007-05-08","order_total_amount":398.58}, {"id":1,"name":"阿腾","age":25,"score":99.99,"birthday":"2025-01-24 00:00:00","province":"重庆","city":"重庆","create_time":"2025-01-24 22:33:08.822","order_id":973,"order_date":"2008-10-27","order_total_amount":830.81}, {"id":2,"name":"阿腾","age":25,"score":99.99,"birthday":"2025-01-24 00:00:00","province":"重庆","city":"重庆"}], total=85, size=3, current=1, orders=[], optimizeCountSql=true, searchCount=true, optimizeJoinOfCountSql=true, maxLimit=null, countId='null'}
```

### 使用QueryWrapper

#### 创建Mapper

**重点：** 参数名仍然必须是 `"ew"`，MyBatis-Plus 才能识别并自动拼接条件。

```java
public interface MyUserMapper extends BaseMapper<MyUser> {

    // 分页查询，传入wrapper
    IPage<JSONObject> selectUsersWithOrderPageWrapper(Page page, @Param(Constants.WRAPPER) QueryWrapper<MyUser> wrapper);
}
```

#### 创建Mapper.xml

传 `wrapper` 给自定义 SQL 时，在where条件中加 `${ew.sqlSegment}`。

```xml
<?xml version="1.0" encoding="UTF-8"?>
<!DOCTYPE mapper PUBLIC "-//mybatis.org//DTD Mapper 3.0//EN" "http://mybatis.org/dtd/mybatis-3-mapper.dtd">
<mapper namespace="local.ateng.java.mybatisjdk8.mapper.MyUserMapper">

    <select id="selectUsersWithOrderPageWrapper" resultType="com.alibaba.fastjson2.JSONObject">
        SELECT
        u.id as id,
        u.name,
        u.age,
        u.score,
        u.birthday,
        u.province,
        u.city,
        u.create_time,
        o.id as order_id,
        o.date as order_date,
        o.total_amount as order_total_amount
        FROM my_user u
        LEFT JOIN my_order o ON u.id = o.user_id
        <where>
            0 = 0 and
            ${ew.sqlSegment}
        </where>
    </select>

</mapper>

```

#### 测试使用

```java
@Test
void test06() {
    QueryWrapper<MyUser> wrapper = new QueryWrapper<>();
    wrapper.like("city", "重");
    wrapper.eq("u.id", 1);
    wrapper.orderByAsc("u.id");
    IPage<JSONObject> page = myUserMapper.selectUsersWithOrderPageWrapper(new Page(1, 3), wrapper);
    System.out.println(page);
}
```

输出内容

```
2025-06-16T21:08:02.429+08:00  INFO 32540 --- [mybatis-plus] [           main] p6spy                                    : #1750079282429 | took 5ms | statement | connection 0| url jdbc:mysql://192.168.1.10:35725/kongyu
SELECT COUNT(*) AS total FROM my_user u WHERE (city LIKE ? AND u.id = ?)
SELECT COUNT(*) AS total FROM my_user u WHERE (city LIKE '%重%' AND u.id = 1);
2025-06-16T21:08:02.448+08:00  INFO 32540 --- [mybatis-plus] [           main] p6spy                                    : #1750079282448 | took 2ms | statement | connection 0| url jdbc:mysql://192.168.1.10:35725/kongyu
SELECT
            u.id as id,
            u.name,
            u.age,
            u.score,
            u.birthday,
            u.province,
            u.city,
            u.create_time,
            o.id as order_id,
            o.date as order_date,
            o.total_amount as order_total_amount
        FROM my_user u
            LEFT JOIN my_order o ON u.id = o.user_id
         WHERE (city LIKE ? AND u.id = ?) LIMIT ?
SELECT
            u.id as id,
            u.name,
            u.age,
            u.score,
            u.birthday,
            u.province,
            u.city,
            u.create_time,
            o.id as order_id,
            o.date as order_date,
            o.total_amount as order_total_amount
        FROM my_user u
            LEFT JOIN my_order o ON u.id = o.user_id
         WHERE (city LIKE '%重%' AND u.id = 1) LIMIT 3;
Page{records=[{"id":1,"name":"阿腾","age":25,"score":99.99,"birthday":"2025-01-24 00:00:00","province":"重庆","city":"重庆","create_time":"2025-01-24 22:33:08.822","order_id":542,"order_date":"2007-05-08","order_total_amount":398.58}, {"id":1,"name":"阿腾","age":25,"score":99.99,"birthday":"2025-01-24 00:00:00","province":"重庆","city":"重庆","create_time":"2025-01-24 22:33:08.822","order_id":973,"order_date":"2008-10-27","order_total_amount":830.81}], total=1, size=3, current=1, orders=[], optimizeCountSql=true, searchCount=true, optimizeJoinOfCountSql=true, maxLimit=null, countId='null'}
```

### 使用LambdaQueryWrapper

使用 LambdaQueryWrapper 比 QueryWrapper 的好处是，能将实体类字段名称自动映射为数据库表字段名称。

#### 创建Mapper

**重点：** 参数名仍然必须是 `"ew"`，MyBatis-Plus 才能识别并自动拼接条件。

```java
public interface MyUserMapper extends BaseMapper<MyUser> {

    // 分页查询，传入wrapper
    IPage<JSONObject> selectUsersWithOrderPageWrapper(Page page, @Param(Constants.WRAPPER) Wrapper wrapper);
}
```

#### 创建Mapper.xml

传 `wrapper` 给自定义 SQL 时，在where条件中加 `${ew.sqlSegment}`。

```xml
<?xml version="1.0" encoding="UTF-8"?>
<!DOCTYPE mapper PUBLIC "-//mybatis.org//DTD Mapper 3.0//EN" "http://mybatis.org/dtd/mybatis-3-mapper.dtd">
<mapper namespace="local.ateng.java.mybatisjdk8.mapper.MyUserMapper">

    <select id="selectUsersWithOrderPageWrapper" resultType="com.alibaba.fastjson2.JSONObject">
        SELECT
        u.id as id,
        u.name,
        u.age,
        u.score,
        u.birthday,
        u.province,
        u.city,
        u.create_time,
        o.id as order_id,
        o.date as order_date,
        o.total_amount as order_total_amount
        FROM my_user u
        LEFT JOIN my_order o ON u.id = o.user_id
        <where>
            0 = 0 and
            ${ew.sqlSegment}
        </where>
    </select>

</mapper>
```

#### 测试使用

```java
@Test
void test07() {
    LambdaQueryWrapper<MyUser> wrapper = Wrappers.lambdaQuery();
    wrapper.like(MyUser::getCity, "重");
    Page<JSONObject> page = new Page(1, 3);
    IPage<JSONObject> pageList = myUserMapper.selectUsersWithOrderPageWrapper(page, wrapper);
    System.out.println(pageList);
}
```

输出内容

```
2025-06-18T22:44:30.748+08:00  INFO 41124 --- [mybatis-plus] [           main] p6spy                                    : #1750257870748 | took 13ms | statement | connection 0| url jdbc:mysql://192.168.1.10:35725/kongyu
SELECT COUNT(*) AS total FROM my_user u WHERE 0 = 0 AND (city LIKE ?)
SELECT COUNT(*) AS total FROM my_user u WHERE 0 = 0 AND (city LIKE '%重%');
2025-06-18T22:44:30.766+08:00  INFO 41124 --- [mybatis-plus] [           main] p6spy                                    : #1750257870766 | took 2ms | statement | connection 0| url jdbc:mysql://192.168.1.10:35725/kongyu
SELECT
            u.id as id,
            u.name,
            u.age,
            u.score,
            u.birthday,
            u.province,
            u.city,
            u.create_time,
            o.id as order_id,
            o.date as order_date,
            o.total_amount as order_total_amount
        FROM my_user u
        LEFT JOIN my_order o ON u.id = o.user_id
         WHERE 0 = 0 and
            (city LIKE ?) LIMIT ?
SELECT
            u.id as id,
            u.name,
            u.age,
            u.score,
            u.birthday,
            u.province,
            u.city,
            u.create_time,
            o.id as order_id,
            o.date as order_date,
            o.total_amount as order_total_amount
        FROM my_user u
        LEFT JOIN my_order o ON u.id = o.user_id
         WHERE 0 = 0 and
            (city LIKE '%重%') LIMIT 3;
Page{records=[{"id":1,"name":"阿腾","age":25,"score":99.99,"birthday":"2025-01-24 00:00:00","province":"重庆","city":"重庆","create_time":"2025-01-24 22:33:08.822","order_id":542,"order_date":"2007-05-08","order_total_amount":398.58}, {"id":1,"name":"阿腾","age":25,"score":99.99,"birthday":"2025-01-24 00:00:00","province":"重庆","city":"重庆","create_time":"2025-01-24 22:33:08.822","order_id":973,"order_date":"2008-10-27","order_total_amount":830.81}, {"id":2,"name":"阿腾","age":25,"score":99.99,"birthday":"2025-01-24 00:00:00","province":"重庆","city":"重庆"}], total=85, size=3, current=1, orders=[], optimizeCountSql=true, searchCount=true, optimizeJoinOfCountSql=true, maxLimit=null, countId='null'}
```

### 分页自定义Count

在一些复杂SQL情况下，MybatisPlus的分页查询Count可能会出现不正确的情况，这里可以使用 **CTE** 或者 **自定义查询Count** 来解决

CTE 的SQL示例

```sql
    <select id="selectUsersWithOrderPageWrapper" resultType="com.alibaba.fastjson2.JSONObject">
        with result as (
            SELECT
                u.id as id,
                u.name,
                u.age,
                u.score,
                u.birthday,
                u.province,
                u.city,
                u.create_time,
                o.id as order_id,
                o.date as order_date,
                o.total_amount as order_total_amount
            FROM my_user u
            LEFT JOIN my_order o ON u.id = o.user_id
        )
        select * from result
        <where>
            0 = 0 and
            ${ew.sqlSegment}
        </where>
    </select>
```

自定义查询Count如下：

#### 创建Mapper

**重点：** 参数名仍然必须是 `"ew"`，MyBatis-Plus 才能识别并自动拼接条件。

```java
public interface MyUserMapper extends BaseMapper<MyUser> {

    // 分页查询，传入wrapper
    IPage<JSONObject> selectUsersWithOrderPageWrapper(Page page, @Param(Constants.WRAPPER) QueryWrapper<MyUser> wrapper);
}
```

#### 创建Mapper.xml

注意 `selectUsersWithOrderPageWrapperCount` 用于后续配置分页查询Count

```xml
    <select id="selectUsersWithOrderPageWrapperCount" resultType="java.lang.Long">
        SELECT
        COUNT(1) AS total
        FROM my_user u
        LEFT JOIN my_order o ON u.id = o.user_id
        <where>
            0 = 0 and
            ${ew.sqlSegment}
        </where>
    </select>
    <select id="selectUsersWithOrderPageWrapper" resultType="com.alibaba.fastjson2.JSONObject">
        SELECT
            u.id as id,
            u.name,
            u.age,
            u.score,
            u.birthday,
            u.province,
            u.city,
            u.create_time,
            o.id as order_id,
            o.date as order_date,
            o.total_amount as order_total_amount
        FROM my_user u
            LEFT JOIN my_order o ON u.id = o.user_id
        <where>
            0 = 0 and
            ${ew.sqlSegment}
        </where>
    </select>
```

#### 测试使用

`page.setCountId("selectUsersWithOrderPageWrapperCount");` 设置查询分页的Mapper id

```java
    @Test
    void test06() {
        QueryWrapper<MyUser> wrapper = new QueryWrapper<>();
        wrapper.like("city", "重");
        wrapper.eq("u.id", 1);
        wrapper.orderByAsc("u.id");
        Page<JSONObject> page = new Page(1, 3);
        page.setCountId("selectUsersWithOrderPageWrapperCount");
        IPage<JSONObject> pageList = myUserMapper.selectUsersWithOrderPageWrapper(page, wrapper);
        System.out.println(pageList);
    }
```

输出内容

```
2025-06-17T21:07:45.371+08:00  INFO 21272 --- [mybatis-plus] [           main] p6spy                                    : #1750165665371 | took 5ms | statement | connection 0| url jdbc:mysql://192.168.1.10:35725/kongyu
SELECT
        COUNT(1)
        FROM my_user u
        LEFT JOIN my_order o ON u.id = o.user_id
         WHERE 0 = 0 and
            (city LIKE ? AND u.id = ?) ORDER BY u.id ASC
SELECT
        COUNT(1)
        FROM my_user u
        LEFT JOIN my_order o ON u.id = o.user_id
         WHERE 0 = 0 and
            (city LIKE '%重%' AND u.id = 1) ORDER BY u.id ASC;
2025-06-17T21:07:45.389+08:00  INFO 21272 --- [mybatis-plus] [           main] p6spy                                    : #1750165665389 | took 2ms | statement | connection 0| url jdbc:mysql://192.168.1.10:35725/kongyu
SELECT
            u.id as id,
            u.name,
            u.age,
            u.score,
            u.birthday,
            u.province,
            u.city,
            u.create_time,
            o.id as order_id,
            o.date as order_date,
            o.total_amount as order_total_amount
        FROM my_user u
            LEFT JOIN my_order o ON u.id = o.user_id
         WHERE 0 = 0 and
            (city LIKE ? AND u.id = ?) ORDER BY u.id ASC LIMIT ?
SELECT
            u.id as id,
            u.name,
            u.age,
            u.score,
            u.birthday,
            u.province,
            u.city,
            u.create_time,
            o.id as order_id,
            o.date as order_date,
            o.total_amount as order_total_amount
        FROM my_user u
            LEFT JOIN my_order o ON u.id = o.user_id
         WHERE 0 = 0 and
            (city LIKE '%重%' AND u.id = 1) ORDER BY u.id ASC LIMIT 3;
Page{records=[{"id":1,"name":"阿腾","age":25,"score":99.99,"birthday":"2025-01-24 00:00:00","province":"重庆","city":"重庆","create_time":"2025-01-24 22:33:08.822","order_id":542,"order_date":"2007-05-08","order_total_amount":398.58}, {"id":1,"name":"阿腾","age":25,"score":99.99,"birthday":"2025-01-24 00:00:00","province":"重庆","city":"重庆","create_time":"2025-01-24 22:33:08.822","order_id":973,"order_date":"2008-10-27","order_total_amount":830.81}], total=2, size=3, current=1, orders=[], optimizeCountSql=true, searchCount=true, optimizeJoinOfCountSql=true, maxLimit=null, countId='selectUsersWithOrderPageWrapperCount'}
```

### 使用Wrapper+自定义查询条件

使用 LambdaQueryWrapper 比 QueryWrapper 的好处是，能将实体类字段名称自动映射为数据库表字段名称。

#### 创建Mapper

**重点：** 参数名仍然必须是 `"ew"`，MyBatis-Plus 才能识别并自动拼接条件。

```java
public interface MyUserMapper extends BaseMapper<MyUser> {

    // 分页查询，传入wrapper、自定义查询条件
    IPage<JSONObject> selectUsersWithOrderPageWrapperQuery(IPage page, @Param(Constants.WRAPPER) Wrapper wrapper, @Param("query") Map<String, Object> query);

}
```

#### 创建Mapper.xml

传 `wrapper` 给自定义 SQL 时，在where条件中加 `${ew.customSqlSegment}`。

```xml
<?xml version="1.0" encoding="UTF-8"?>
<!DOCTYPE mapper PUBLIC "-//mybatis.org//DTD Mapper 3.0//EN" "http://mybatis.org/dtd/mybatis-3-mapper.dtd">
<mapper namespace="local.ateng.java.mybatisjdk8.mapper.MyUserMapper">

   <select id="selectUsersWithOrderPageWrapperQuery" resultType="com.alibaba.fastjson2.JSONObject">
        with my_table as (
            SELECT
                u.id as id,
                u.name,
                u.age,
                u.score,
                u.birthday,
                u.province,
                u.city,
                u.create_time,
                o.id as order_id,
                o.date as order_date,
                o.total_amount as order_total_amount
            FROM my_user u
                LEFT JOIN my_order o ON u.id = o.user_id
            <if test="query._id != null">
                and u.id = #{query._id}
            </if>
        ) select * from my_table
        ${ew.customSqlSegment}
    </select>

</mapper>
```

#### 测试使用

```java
    @Test
    void test08() {
        LambdaQueryWrapper<MyUser> wrapper = Wrappers.lambdaQuery();
        wrapper.like(MyUser::getCity, "重");
        IPage<JSONObject> page = new Page(1, 3);
        Map<String, Object> map = new HashMap<>();
        map.put("_id", 1);
        page = myUserMapper.selectUsersWithOrderPageWrapperQuery(page, wrapper, map);
        System.out.println(page);
    }
```

输出内容

```
2025-08-15T17:46:56.304+08:00  INFO 13076 --- [mybatis-plus] [           main] p6spy                                    : #1755251216304 | took 40ms | statement | connection 0| url jdbc:mysql://192.168.1.10:35725/kongyu
WITH my_table AS (SELECT u.id AS id, u.name, u.age, u.score, u.birthday, u.province, u.city, u.create_time, o.id AS order_id, o.date AS order_date, o.total_amount AS order_total_amount FROM my_user u LEFT JOIN my_order o ON u.id = o.user_id AND u.id = ?) SELECT COUNT(*) AS total FROM my_table WHERE (city LIKE ?)
WITH my_table AS (SELECT u.id AS id, u.name, u.age, u.score, u.birthday, u.province, u.city, u.create_time, o.id AS order_id, o.date AS order_date, o.total_amount AS order_total_amount FROM my_user u LEFT JOIN my_order o ON u.id = o.user_id AND u.id = 1) SELECT COUNT(*) AS total FROM my_table WHERE (city LIKE '%重%');
2025-08-15T17:46:56.354+08:00  INFO 13076 --- [mybatis-plus] [           main] p6spy                                    : #1755251216354 | took 33ms | statement | connection 0| url jdbc:mysql://192.168.1.10:35725/kongyu
with my_table as (
            SELECT
                u.id as id,
                u.name,
                u.age,
                u.score,
                u.birthday,
                u.province,
                u.city,
                u.create_time,
                o.id as order_id,
                o.date as order_date,
                o.total_amount as order_total_amount
            FROM my_user u
                LEFT JOIN my_order o ON u.id = o.user_id
             
                and u.id = ?
             
        ) select * from my_table
        WHERE (city LIKE ?) LIMIT ?
with my_table as (
            SELECT
                u.id as id,
                u.name,
                u.age,
                u.score,
                u.birthday,
                u.province,
                u.city,
                u.create_time,
                o.id as order_id,
                o.date as order_date,
                o.total_amount as order_total_amount
            FROM my_user u
                LEFT JOIN my_order o ON u.id = o.user_id
             
                and u.id = 1
             
        ) select * from my_table
        WHERE (city LIKE '%重%') LIMIT 3;
Page{records=[{"id":1,"name":"阿腾","age":25,"score":99.99,"birthday":"2025-01-24 00:00:00","province":"重庆","city":"重庆","create_time":"2025-01-24 22:33:08.822","order_id":542,"order_date":"2007-05-08","order_total_amount":398.58}, {"id":1,"name":"阿腾","age":25,"score":99.99,"birthday":"2025-01-24 00:00:00","province":"重庆","city":"重庆","create_time":"2025-01-24 22:33:08.822","order_id":973,"order_date":"2008-10-27","order_total_amount":830.81}, {"id":2,"name":"阿腾","age":25,"score":99.99,"birthday":"2025-01-24 00:00:00","province":"重庆","city":"重庆"}], total=86, size=3, current=1, orders=[], optimizeCountSql=true, searchCount=true, optimizeJoinOfCountSql=true, maxLimit=null, countId='null'}
```



## 🌟 Mapper XML常用标签整理

------

### 🟣 `#{}` 和 `${}` 的主要差异

|      | `#{}`                                           | `${}`                             |
| ---- | ----------------------------------------------- | --------------------------------- |
| 作用 | **推荐**，按占位绑定，由 PreparedStatement 设置 | **纯文本拼接**，适用表/列动态拼接 |
| 风险 | 安全（防 SQL 注入）                             | 有风险（容易 SQL 注入）           |
| 渲染 | 渲染时为 `?`                                    | 渲染时为具体文本                  |
| 建议 | 优先使用                                        | 仅在需要时（如列名、表名拼接）    |

------

### 🟣 基本标签（适用增删改查）

🔹`<select>` — 定义**数据的读取语句**

✅适用条件：按条件进行数据**检索**。
 ✅作用：将数据从表中**查出**，可以绑定到对象或者 List。

```xml
<select id="findById" parameterType="java.lang.Long" resultMap="BaseResultMap">
  SELECT * FROM user WHERE id = #{id}
</select>
```

------

🔹`<insert>` — 定义**插入语句**

✅适用条件：插入数据时使用。
 ✅作用：将对象中准备好的数据插入到表中。

```xml
<insert id="insertUser" parameterType="User">
  INSERT INTO user (username, password) VALUES (#{username}, #{password})
</insert>
```

------

🔹`<update>` — 定义**修改语句**

✅适用条件：需要修改表中现有数据时。
 ✅作用：按条件修改指定的数据列。

```xml
<update id="updateUsername" parameterType="User">
  UPDATE user SET username = #{username} WHERE id = #{id}
</update>
```

------

🔹`<delete>` — 定义**删除语句**

✅适用条件：按条件删除数据时。
 ✅作用：从表中移除符合条件的数据。

```xml
<delete id="deleteById" parameterType="long">
  DELETE FROM user WHERE id = #{id}
</delete>
```

------

### 🟣 动态标签（适用条件拼接）

🔹`<![CDATA[]]>` —转义操作

✅适用条件：需要写出 `>` 或 `<` 等需要转义的条件时。
 ✅作用：保持语法简洁，与 MyBatis 无关，仅仅是为了避免解析错误。

```xml
<select id="findAllGreaterThanId" parameterType="java.lang.Long">
  SELECT * FROM user WHERE id <![CDATA[ > ]]> #{id}
</select>
```

------

🔹`<if>` —按条件拼接片段

✅适用条件：需要有条件地拼接不同的 `WHERE` 子句时。
 ✅作用：若条件为 true 则拼接其中的 SQL。

```xml
<select id="findByConditions" parameterType="User">
  SELECT * FROM user WHERE 1 = 1
  <if test="username != null">
    AND username = #{username}
  </if>
</select>
```

------

🔹`<choose>` —按条件进行分枝处理

✅适用条件：有多个条件时，按**第一个为 true 的条件**拼接。
 ✅作用：适用**if-else**结构。

```xml
<select id="findByOption" parameterType="User">
  SELECT * FROM user WHERE 1 = 1
  <choose>
    <when test="username != null">
      AND username = #{username}
    </when>
    <when test="email != null">
      AND email = #{email}
    </when>
    <otherwise>
      LIMIT 10
    </otherwise>
  </choose>
</select>
```

------

🔹`<where>` —智能拼接 `AND/OR`

✅适用条件：有条件时自动插入 `WHERE` ，且会移除最前多余的 `AND/OR`。
 ✅作用：简化拼接语法。

```xml
<select id="findAllWithWhere" parameterType="User">
  SELECT * FROM user
  <where>
    <if test="username != null">
      AND username = #{username}
    </if>
    <if test="email != null">
      OR email = #{email}
    </if>
  </where>
</select>
```

------

🔹`<trim>` —按规则清理拼接

✅适用条件：需要按规则移除指定前后关键字时。
 ✅作用：可以指定 `suffixOverrides` 或 `prefixOverrides`。

```xml
<select id="findAllWithTrim" parameterType="User">
  SELECT * FROM user
  <trim prefix="WHERE" prefixOverrides="AND|OR">
    <if test="username != null">
      AND username = #{username}
    </if>
    <if test="email != null">
      OR email = #{email}
    </if>
  </trim>
</select>
```

------

🔹`<foreach>` —适用批量条件（in语法）

✅适用条件：需要对一个数组/ List进行批量拼接时。
 ✅作用：可以轻松实现 `in (...)` 查询。

```xml
<select id="findByIds" parameterType="list">
  SELECT * FROM user WHERE id IN
  <foreach item="id" collection="list" open="(" separator="," close=")"> 
    #{id} 
  </foreach>
</select>
```

------

### 🟣 resultMap —列与对象的高度自由映射

✅适用条件：列名与对象属性不一一对应时，或者需要进行关联时。
 ✅作用：可以进行一对一、一对多甚至是有参赋值。

| 标签            | 作用                         |
| --------------- | ---------------------------- |
| `<id>`          | 定义**主键列**对应哪个属性   |
| `<result>`      | 定义普通列与对象哪个属性对应 |
| `<association>` | 定义一对一时的关联           |
| `<collection>`  | 定义一对多时的关联           |
| `<constructor>` | 适用有参构造时进行赋值       |

------

#### 🟣 一对一

association

✅适用条件：需要联合表进行**关联**时（1对1）。
 ✅作用：可以轻松地将关联表的数据按对象进行嵌套。

```xml
<resultMap id="UserWithProfile" type="User">
  <id column="id_field" property="id" />
  <result column="username_field" property="username" />
  
  <association property="profile" javaType="Profile">
    <id column="profile_id_field" property="id" />
    <result column="profile_name_field" property="profileName" />
  </association>
</resultMap>
```

直接映射嵌套属性

```xml
<resultMap id="UserWithProfileSimple" type="User">
  <id column="id_field" property="id" />
  <result column="username_field" property="username" />
  <result column="profile_id_field" property="profile.id" />
  <result column="profile_name_field" property="profile.profileName" />
</resultMap>
```

------

#### 🟣 一对多（collection）

✅适用条件：需要获取**一对多**的数据时（如一个用户有多个购买记录)。
 ✅作用：可以将关联的数据按 List 映射到对象中。

```xml
<resultMap id="UserWithOrders" type="User">
  <id column="id_field" property="id" />
  <result column="username_field" property="username" />
  
  <collection property="orders" ofType="Order">
    <id column="order_id_field" property="id" />
    <result column="order_number_field" property="orderNumber" />
  </collection>
</resultMap>
```



## TypeHandler

### UUIDTypeHandler

```java
package local.ateng.java.mybatisjdk8.handler;

import org.apache.ibatis.type.BaseTypeHandler;
import org.apache.ibatis.type.JdbcType;

import java.nio.ByteBuffer;
import java.sql.*;
import java.util.UUID;

/**
 * MyBatis 类型处理器：将 MySQL 的 binary(16) 字段与 Java 的 UUID 类型进行映射转换。
 * <p>
 * 用于处理数据库中使用 UUID_TO_BIN(uuid()) 生成的顺序 UUID（二进制格式），
 * Java 端字段必须使用 {@link java.util.UUID} 类型。
 * </p>
 *
 * <pre>
 * 数据库字段类型：binary(16)
 * Java 字段类型：java.util.UUID
 * </pre>
 * <p>
 * 示例使用：
 * <pre>
 * &#64;TableField(typeHandler = UUIDTypeHandler.class)
 * private UUID uuid;
 * </pre>
 * <p>
 * 注意：实体类字段必须为 {@code UUID} 类型，不能使用 {@code byte[]} 或 {@code String}。
 *
 * @author 孔余
 * @since 2025-07-27
 */
@MappedJdbcTypes({JdbcType.VARCHAR, JdbcType.VARBINARY, JdbcType.OTHER}) // 数据库字段类型
@MappedTypes(UUID.class)           // Java 类型
public class UUIDTypeHandler extends BaseTypeHandler<UUID> {

    /**
     * 将 Java UUID 类型参数设置到 PreparedStatement 中，以字节数组形式写入 binary(16) 字段。
     *
     * @param ps       PreparedStatement 对象
     * @param i        参数索引（从1开始）
     * @param uuid     要写入的 UUID 值，不能为空
     * @param jdbcType JDBC 类型（可为空）
     * @throws SQLException SQL 操作异常
     */
    @Override
    public void setNonNullParameter(PreparedStatement ps, int i, UUID uuid, JdbcType jdbcType) throws SQLException {
        try {
            ps.setBytes(i, uuidToBytes(uuid));
        } catch (Exception e) {
            ps.setNull(i, Types.NULL);
        }
    }

    /**
     * 从结果集中获取 UUID 值（通过列名），并将 binary(16) 转为 UUID 类型。
     *
     * @param rs         结果集对象
     * @param columnName 列名
     * @return 对应的 UUID 值，如果字段为 null 则返回 null
     * @throws SQLException SQL 操作异常
     */
    @Override
    public UUID getNullableResult(ResultSet rs, String columnName) throws SQLException {
        byte[] bytes = rs.getBytes(columnName);
        return bytes != null ? bytesToUUID(bytes) : null;
    }

    /**
     * 从结果集中获取 UUID 值（通过列索引），并将 binary(16) 转为 UUID 类型。
     *
     * @param rs          结果集对象
     * @param columnIndex 列索引（从1开始）
     * @return 对应的 UUID 值，如果字段为 null 则返回 null
     * @throws SQLException SQL 操作异常
     */
    @Override
    public UUID getNullableResult(ResultSet rs, int columnIndex) throws SQLException {
        byte[] bytes = rs.getBytes(columnIndex);
        return bytes != null ? bytesToUUID(bytes) : null;
    }

    /**
     * 从存储过程中获取 UUID 值（通过列索引），并将 binary(16) 转为 UUID 类型。
     *
     * @param cs          CallableStatement 对象
     * @param columnIndex 列索引（从1开始）
     * @return 对应的 UUID 值，如果字段为 null 则返回 null
     * @throws SQLException SQL 操作异常
     */
    @Override
    public UUID getNullableResult(CallableStatement cs, int columnIndex) throws SQLException {
        byte[] bytes = cs.getBytes(columnIndex);
        return bytes != null ? bytesToUUID(bytes) : null;
    }

    /**
     * 将 UUID 对象转换为 16 字节的二进制数组。
     *
     * @param uuid 要转换的 UUID
     * @return 二进制数组表示的 UUID（长度为16）
     */
    private byte[] uuidToBytes(UUID uuid) {
        ByteBuffer buffer = ByteBuffer.allocate(16);
        buffer.putLong(uuid.getMostSignificantBits());
        buffer.putLong(uuid.getLeastSignificantBits());
        return buffer.array();
    }

    /**
     * 将 16 字节的二进制数组转换为 UUID 对象。
     *
     * @param bytes 长度为16的字节数组
     * @return 对应的 UUID 对象
     */
    private UUID bytesToUUID(byte[] bytes) {
        try {
            if (bytes == null || bytes.length != 16) {
                return null;
            }
            ByteBuffer buffer = ByteBuffer.wrap(bytes);
            long high = buffer.getLong();
            long low = buffer.getLong();
            return new UUID(high, low);
        } catch (Exception e) {
            return null;
        }
    }

}



```

### IPAddressTypeHandler

```java
package local.ateng.java.mybatisjdk8.handler;

import org.apache.ibatis.type.BaseTypeHandler;
import org.apache.ibatis.type.JdbcType;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.sql.*;

/**
 * MyBatis 类型处理器：将 MySQL 的 varbinary(16) 字段与 Java 中的 IP 字符串进行转换。
 * <p>
 * 在数据库中使用 INET6_ATON('127.0.01') | INET6_ATON('::1') 生成的 IPv4或者IPv6地址（二进制格式）
 * 支持 IPv4（4字节）和 IPv6（16字节）地址的互相映射。
 * 如果字段内容非法或解析异常，则返回 null。
 * </p>
 *
 * <pre>
 * 数据库字段类型：varbinary(16)
 * Java 字段类型：String（如 "192.168.1.1" 或 "::1"）
 * </pre>
 * <p>
 * 示例使用：
 * <pre>
 * &#64;TableField(typeHandler = IPAddressTypeHandler.class)
 * private String ipAddress;
 * </pre>
 *
 * @author 孔余
 * @since 2025-07-27
 */
public class IPAddressTypeHandler extends BaseTypeHandler<String> {

    /**
     * 设置非空 IP 字符串参数到 PreparedStatement，写入为对应字节数组。
     *
     * @param ps       PreparedStatement 对象
     * @param i        参数索引
     * @param ip       IP 地址字符串（IPv4 或 IPv6）
     * @param jdbcType JDBC 类型
     * @throws SQLException SQL异常
     */
    @Override
    public void setNonNullParameter(PreparedStatement ps, int i, String ip, JdbcType jdbcType) throws SQLException {
        try {
            byte[] addressBytes = InetAddress.getByName(ip).getAddress();
            ps.setBytes(i, addressBytes);
        } catch (UnknownHostException e) {
            ps.setNull(i, Types.NULL);
        }
    }

    /**
     * 通过列名获取 IP 字符串（从结果集）
     */
    @Override
    public String getNullableResult(ResultSet rs, String columnName) throws SQLException {
        return toIpString(rs.getBytes(columnName));
    }

    /**
     * 通过列索引获取 IP 字符串（从结果集）
     */
    @Override
    public String getNullableResult(ResultSet rs, int columnIndex) throws SQLException {
        return toIpString(rs.getBytes(columnIndex));
    }

    /**
     * 从存储过程中通过列索引获取 IP 字符串
     */
    @Override
    public String getNullableResult(CallableStatement cs, int columnIndex) throws SQLException {
        return toIpString(cs.getBytes(columnIndex));
    }

    /**
     * 将 IP 地址字节数组转换为字符串（IPv4 或 IPv6），非法时返回 null。
     *
     * @param bytes IP 字节数组（应为 4 或 16 字节）
     * @return 字符串形式的 IP 地址，或 null
     */
    private String toIpString(byte[] bytes) {
        if (bytes == null || bytes.length == 0) {
            return null;
        }
        try {
            // 自动兼容 IPv4（4字节）和 IPv6（16字节）
            InetAddress byAddress = InetAddress.getByAddress(bytes);
            return byAddress.getHostAddress();
        } catch (UnknownHostException e) {
            return null;
        }
    }

}


```

### Base64TypeHandler

```java
package local.ateng.java.mybatisjdk8.handler;

import org.apache.ibatis.type.BaseTypeHandler;
import org.apache.ibatis.type.JdbcType;

import java.sql.*;
import java.util.Base64;

/**
 * MyBatis TypeHandler：用于将 MySQL 中的二进制字段（如 BLOB、BINARY、VARBINARY）与 Java 字段进行映射。
 *
 * <p>功能说明：
 * <ul>
 *   <li>将数据库中的二进制数据（byte[]）转换为 Base64 字符串，用于 Java 字段是 String 的情况</li>
 *   <li>将 Java 中的 Base64 字符串解码为 byte[] 后写入数据库</li>
 *   <li>支持查询时自动判断字段是否为 null，避免异常</li>
 * </ul>
 *
 * <p>适用数据库字段类型：
 * <ul>
 *   <li>BLOB</li>
 *   <li>BINARY(n)</li>
 *   <li>VARBINARY(n)</li>
 * </ul>
 *
 * <p>适用 Java 字段类型：
 * <ul>
 *   <li>String（Base64 格式）</li>
 * </ul>
 *
 * <p>使用示例：
 * <pre>{@code
 * @TableField(typeHandler = Base64TypeHandler.class)
 * private String binaryData;
 * }</pre>
 *
 * <p>注意事项：
 * <ul>
 *   <li>若字段为 null，查询时将返回 null，不抛出异常</li>
 *   <li>编码格式为标准 Base64，不包含换行</li>
 * </ul>
 *
 * @author 孔余
 * @since 2025-07-27
 */
public class Base64TypeHandler extends BaseTypeHandler<String> {

    /**
     * 设置非空参数：将 Base64 字符串解码为 byte[] 写入数据库
     *
     * @param ps        PreparedStatement 对象
     * @param i         参数索引（从1开始）
     * @param parameter Base64 编码字符串
     * @param jdbcType  JDBC 类型（应为 BLOB）
     * @throws SQLException SQL 异常
     */
    @Override
    public void setNonNullParameter(PreparedStatement ps, int i, String parameter, JdbcType jdbcType) throws SQLException {
        try {
            byte[] decoded = Base64.getDecoder().decode(parameter);
            ps.setBytes(i, decoded);
        } catch (Exception e) {
            ps.setNull(i, Types.NULL);
        }
    }

    /**
     * 通过列名获取结果：将 byte[] 转为 Base64 字符串
     *
     * @param rs         结果集
     * @param columnName 列名
     * @return Base64 编码字符串，异常或为空时返回 null
     * @throws SQLException SQL 异常
     */
    @Override
    public String getNullableResult(ResultSet rs, String columnName) throws SQLException {
        try {
            byte[] bytes = rs.getBytes(columnName);
            return bytes != null ? Base64.getEncoder().encodeToString(bytes) : null;
        } catch (Exception e) {
            return null;
        }
    }

    /**
     * 通过列索引获取结果：将 byte[] 转为 Base64 字符串
     *
     * @param rs          结果集
     * @param columnIndex 列索引（从1开始）
     * @return Base64 编码字符串，异常或为空时返回 null
     * @throws SQLException SQL 异常
     */
    @Override
    public String getNullableResult(ResultSet rs, int columnIndex) throws SQLException {
        try {
            byte[] bytes = rs.getBytes(columnIndex);
            return bytes != null ? Base64.getEncoder().encodeToString(bytes) : null;
        } catch (Exception e) {
            return null;
        }
    }

    /**
     * 通过 CallableStatement 获取结果：将 byte[] 转为 Base64 字符串
     *
     * @param cs          CallableStatement 对象
     * @param columnIndex 输出参数索引
     * @return Base64 编码字符串，异常或为空时返回 null
     * @throws SQLException SQL 异常
     */
    @Override
    public String getNullableResult(CallableStatement cs, int columnIndex) throws SQLException {
        try {
            byte[] bytes = cs.getBytes(columnIndex);
            return bytes != null ? Base64.getEncoder().encodeToString(bytes) : null;
        } catch (Exception e) {
            return null;
        }
    }
}


```

### GeometryTypeHandler

添加依赖

```xml
<!-- 地理空间数据处理库 -->
<dependency>
    <groupId>org.locationtech.jts</groupId>
    <artifactId>jts-core</artifactId>
    <version>1.20.0</version>
</dependency>
```

数据构建

```java
GeometryFactory factory = new GeometryFactory(new PrecisionModel(), 4326);
Point point = factory.createPoint(new Coordinate(106.55, 29.56));
```

实体类字段

```java
/**
 * 地理坐标（经纬度）
 */
@TableField(value = "location", typeHandler = GeometryTypeHandler.class)
private Geometry location;
```

具体代码

```java
package local.ateng.java.mybatisjdk8.handler;

import org.apache.ibatis.type.BaseTypeHandler;
import org.apache.ibatis.type.JdbcType;
import org.locationtech.jts.geom.Geometry;
import org.locationtech.jts.io.ByteOrderValues;
import org.locationtech.jts.io.WKBReader;
import org.locationtech.jts.io.WKBWriter;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.sql.*;
import java.util.Arrays;

/**
 * MyBatis Plus 类型处理器：用于映射 MySQL 中的 Geometry 字段与 JTS 的 Geometry 对象
 *
 * <p>注意：</p>
 * <ul>
 *   <li>写入时使用 WKB（带 SRID）格式</li>
 *   <li>读取时自动跳过前4字节的 SRID 并返回 Geometry 对象</li>
 *   <li>如解析失败，返回 null，不抛出异常</li>
 * </ul>
 *
 * <p>建议 MySQL 字段类型为 <code>geometry SRID 4326</code></p>
 *
 * @author 孔余
 * @since 2025-07-27
 */
@MappedJdbcTypes({JdbcType.OTHER}) // 数据库字段类型
@MappedTypes(Geometry.class)           // Java 类型
public class GeometryTypeHandler extends BaseTypeHandler<Geometry> {

    /**
     * 设置非空参数到 PreparedStatement 中，使用带 SRID 的 WKB 格式。
     *
     * @param ps        预编译 SQL 语句
     * @param i         参数索引（从1开始）
     * @param parameter Geometry 参数
     * @param jdbcType  JDBC 类型（可为空）
     * @throws SQLException SQL 异常
     */
    @Override
    public void setNonNullParameter(PreparedStatement ps, int i, Geometry parameter, JdbcType jdbcType) throws SQLException {
        if (parameter == null) {
            ps.setNull(i, Types.BINARY);
            return;
        }

        try {
            // 获取 SRID，默认 4326
            int srid = parameter.getSRID() > 0 ? parameter.getSRID() : 4326;

            // 使用 WKBWriter 生成 2D 小端 WKB，禁用 EWKB 扩展（Z/M/SRID）
            WKBWriter wkbWriter = new WKBWriter(2, ByteOrderValues.LITTLE_ENDIAN, false);
            byte[] wkb = wkbWriter.write(parameter);

            // 拼接 SRID（4 字节小端序）和 WKB
            ByteBuffer buffer = ByteBuffer.allocate(4 + wkb.length);
            buffer.order(ByteOrder.LITTLE_ENDIAN);
            buffer.putInt(srid);
            buffer.put(wkb);

            // 设置参数值为 MySQL 支持的 EWKB 格式二进制
            ps.setBytes(i, buffer.array());
        } catch (Exception e) {
            // 保证接口契约，设置为 SQL NULL，避免报错
            ps.setNull(i, Types.BINARY);
        }
    }

    /**
     * 从 ResultSet 中获取 Geometry 对象（按列名）
     *
     * @param rs         结果集
     * @param columnName 列名
     * @return Geometry 对象或 null
     * @throws SQLException SQL 异常
     */
    @Override
    public Geometry getNullableResult(ResultSet rs, String columnName) throws SQLException {
        return parseGeometry(rs.getBytes(columnName));
    }

    /**
     * 从 ResultSet 中获取 Geometry 对象（按列索引）
     *
     * @param rs          结果集
     * @param columnIndex 列索引（从1开始）
     * @return Geometry 对象或 null
     * @throws SQLException SQL 异常
     */
    @Override
    public Geometry getNullableResult(ResultSet rs, int columnIndex) throws SQLException {
        return parseGeometry(rs.getBytes(columnIndex));
    }

    /**
     * 从 CallableStatement 中获取 Geometry 对象
     *
     * @param cs          存储过程调用
     * @param columnIndex 列索引（从1开始）
     * @return Geometry 对象或 null
     * @throws SQLException SQL 异常
     */
    @Override
    public Geometry getNullableResult(CallableStatement cs, int columnIndex) throws SQLException {
        return parseGeometry(cs.getBytes(columnIndex));
    }

    /**
     * 解析 MySQL 返回的 GEOMETRY 字节流（包含 SRID 前缀）
     *
     * @param bytes GEOMETRY 字节流
     * @return Geometry 对象或 null（如果失败）
     */
    private Geometry parseGeometry(byte[] bytes) {
        if (bytes == null || bytes.length < 5) {
            return null;
        }

        try {
            // 提取 SRID（前4字节）
            ByteBuffer sridBuffer = ByteBuffer.wrap(bytes, 0, 4).order(ByteOrder.LITTLE_ENDIAN);
            int srid = sridBuffer.getInt();

            // 提取 WKB 并解析
            byte[] wkb = Arrays.copyOfRange(bytes, 4, bytes.length);
            WKBReader reader = new WKBReader();
            Geometry geometry = reader.read(wkb);
            geometry.setSRID(srid);

            return geometry;
        } catch (Exception e) {
            return null; // 解析失败返回 null
        }
    }
}

```

### FastjsonTypeHandler

在 `Fastjson1` 在存储 Bean、List数据都可以，并且不会出现类型擦除的问题（Fastjson1的反序列化不会受@type顺序的影响，Fastjson2必须在数据第一个才能自动映射）。

注意Fastjson1开启了 `SerializerFeature.WriteClassName` 会使 Double 这类数据序列化后的值带有D后缀，导致JSON数据格式出现问题！！！

```java
package local.ateng.java.mybatisjdk8.handler;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.parser.Feature;
import com.alibaba.fastjson.serializer.SerializerFeature;
import com.baomidou.mybatisplus.extension.handlers.AbstractJsonTypeHandler;

/**
 * 通用的 Fastjson 类型处理器，用于 MyBatis Plus 中将 Java 对象与 JSON 字段互相转换。
 * <p>
 * 本处理器使用 Fastjson 1.x 实现，支持自动类型识别、空值处理、类型信息保留等功能。
 * 通常用于如下场景：
 * <pre>{@code
 * @TableField(typeHandler = JacksonTypeHandler.class)
 * private MyEntity data;
 * }</pre>
 * <pre>{@code
 *  * @TableField(typeHandler = JacksonTypeHandler.class)
 *  * private List<MyEntity> dataList;
 *  * }</pre>
 *
 * @param <T> 要序列化或反序列化的目标 Java 类型
 * @author 孔余
 * @since 2025-07-28
 */
@MappedJdbcTypes({JdbcType.VARCHAR, JdbcType.LONGVARCHAR, JdbcType.OTHER}) // 数据库字段类型
@MappedTypes({Map.class, List.class, JSONObject.class, JSONArray.class})     // Java 类型
public class FastjsonTypeHandler<T> extends AbstractJsonTypeHandler<T> {

    /**
     * 目标类型的 Class 对象，用于反序列化
     */
    private final Class<T> type;

    /**
     * 构造方法，指定当前处理的对象类型
     *
     * @param type 要处理的 Java 类型
     */
    public FastjsonTypeHandler(Class<T> type) {
        this.type = type;
    }

    /**
     * 将 JSON 字符串解析为 Java 对象
     *
     * @param json 数据库中的 JSON 字符串
     * @return Java 对象，解析失败或为空时返回 null
     */
    @Override
    protected T parse(String json) {
        try {
            return JSON.parseObject(
                    json,
                    this.type,
                    // 支持 "@type" 字段进行自动类型识别（适用于多态反序列化）
                    Feature.SupportAutoType,
                    // 当 JSON 中存在 Java 类中没有的字段时忽略，不抛出异常
                    Feature.IgnoreNotMatch
            );
        } catch (Exception e) {
            // 解析失败时返回 null（可视情况记录日志）
            return null;
        }
    }

    /**
     * 将 Java 对象序列化为 JSON 字符串，用于写入数据库字段
     *
     * @param obj Java 对象
     * @return JSON 字符串，序列化失败或对象为 null 时返回 null
     */
    @Override
    protected String toJson(T obj) {
        try {
            if (obj == null) {
                return null;
            }

            return JSON.toJSONString(obj,
                    // 添加 "@type" 字段，保留类的全限定名，便于反序列化时识别原类型
                    SerializerFeature.WriteClassName,
                    // Map 类型字段即使为 null 也输出
                    SerializerFeature.WriteMapNullValue,
                    // 将 null 的 List 类型字段序列化为空数组 []
                    SerializerFeature.WriteNullListAsEmpty,
                    // 将 null 的字符串字段序列化为空字符串 ""
                    SerializerFeature.WriteNullStringAsEmpty,
                    // 将 null 的数字字段序列化为 0
                    SerializerFeature.WriteNullNumberAsZero,
                    // 将 null 的布尔字段序列化为 false
                    SerializerFeature.WriteNullBooleanAsFalse,
                    // 禁用循环引用检测，提高性能（如果存在对象引用自身需谨慎）
                    SerializerFeature.DisableCircularReferenceDetect
            );
        } catch (Exception e) {
            // 序列化失败时返回 null（可根据需要记录错误日志）
            return null;
        }
    }
}
```

### Fastjson2TypeHandler

在 `Fastjson1` 在存储 Bean 可以，但 List 会出现类型擦除的问题，原因是 数据库JSON字段存储数据后顺序会变。

在 `Fastjson2` 中 `@type` 字段必须在JSON数据的第一个，不然无法自动解析，并且还需要开启 `AutoTypeFilter` 指定自动解析白名单包路径。

这里有个问题，在 MySQL 中设置的JSON字段存储JSON数据后会自动调整JSON字段的顺序，导致最终查询出来自动解析类型会变成默认的JSONObject，为了解决这个问题可以直接将JSON字段类型改为text或者其他字符串类型，当然这不是最优的方式，最优的方式就是下面的设置 TypeReference 然后每个需要解析的指定具体的类型。

```java
package local.ateng.java.mybatisjdk8.handler;

import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.JSONReader;
import com.alibaba.fastjson2.JSONWriter;
import com.baomidou.mybatisplus.extension.handlers.AbstractJsonTypeHandler;

/**
 * 通用的 Fastjson2 类型处理器，用于 MyBatis Plus 中将对象以 JSON 格式读写数据库字段。
 * <p>
 * 适用于 JSON 字段与自定义 Java 对象之间的转换，
 * 实现了序列化与反序列化的逻辑，支持自动类型识别和特定的序列化配置。
 * 通常用于如下场景：
 * <pre>{@code
 * @TableField(typeHandler = JacksonTypeHandler.class)
 * private MyEntity data;
 * }</pre>
 *
 * @param <T> 要序列化或反序列化的目标类型
 * @author 孔余
 * @since 2025-07-28
 */
@MappedJdbcTypes({JdbcType.VARCHAR, JdbcType.LONGVARCHAR, JdbcType.OTHER}) // 数据库字段类型
@MappedTypes({Map.class, List.class, JSONObject.class, JSONArray.class})     // Java 类型
public class Fastjson2TypeHandler<T> extends AbstractJsonTypeHandler<T> {

    /**
     * 要处理的目标类型
     */
    private final Class<T> type;

    /**
     * 构造方法，指定处理的 Java 类型
     *
     * @param type 目标类类型
     */
    public Fastjson2TypeHandler(Class<T> type) {
        this.type = type;
    }

    /**
     * 将 JSON 字符串解析为对象
     *
     * @param json 数据库中存储的 JSON 字符串
     * @return 解析后的 Java 对象，解析失败或为空则返回 null
     */
    @Override
    protected T parse(String json) {
        if (json == null || json.trim().isEmpty()) {
            return null;
        }

        return JSON.parseObject(
                json,
                type,
                // 开启自动类型识别，仅允许指定包名
                JSONReader.autoTypeFilter("local.kongyu.java.", "local.ateng.java"),
                // 开启智能字段匹配（允许字段名不完全匹配）
                JSONReader.Feature.SupportSmartMatch
        );
    }

    /**
     * 将对象序列化为 JSON 字符串，用于写入数据库
     *
     * @param obj Java 对象
     * @return 序列化后的 JSON 字符串，失败或为空返回 null
     */
    @Override
    protected String toJson(T obj) {
        try {
            if (obj == null) {
                return null;
            }

            return JSON.toJSONString(
                    obj,
                    // 序列化时输出类型信息（用于反序列化）
                    JSONWriter.Feature.WriteClassName,
                    // 不输出数字类型的类名（如 Integer、Long 等）
                    JSONWriter.Feature.NotWriteNumberClassName,
                    // 不输出 Set 类型的类名（如 HashSet）
                    JSONWriter.Feature.NotWriteSetClassName,
                    // 序列化时包含值为 null 的字段
                    JSONWriter.Feature.WriteNulls,
                    // 为兼容 JavaScript，大整数转为字符串输出
                    JSONWriter.Feature.BrowserCompatible,
                    // 序列化 BigDecimal 时使用非科学计数法（toPlainString）
                    JSONWriter.Feature.WriteBigDecimalAsPlain
            );
        } catch (Exception e) {
            // 序列化失败返回 null（可视情况记录日志）
            return null;
        }
    }
}

```

### Fastjson2GenericTypeReferenceHandler

用这个的主要原因是List数据或者其他有泛型的数据会出现类型擦除的问题，这里使用TypeReference给每个数据创建一个TypeHandler使用。

#### 创建 Fastjson2GenericTypeReferenceHandler

```java
package local.ateng.java.mybatisjdk8.handler;

import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.JSONReader;
import com.alibaba.fastjson2.JSONWriter;
import com.alibaba.fastjson2.TypeReference;
import com.baomidou.mybatisplus.extension.handlers.AbstractJsonTypeHandler;

import java.lang.reflect.Type;

/**
 * 通用的 Fastjson2 泛型类型处理器，用于 MyBatis Plus 将复杂泛型结构与 JSON 之间进行序列化/反序列化。
 * <p>
 * 相比普通的类型处理器，此类支持 {@link java.util.List}、{@link java.util.Map}、嵌套泛型等复杂类型。
 * 通过 {@link TypeReference} 保留泛型类型信息。
 *
 * @param <T> 要处理的 Java 泛型类型
 * @author 孔余
 * @since 2025-07-28
 */
public class Fastjson2GenericTypeReferenceHandler<T> extends AbstractJsonTypeHandler<T> {

    /**
     * 目标泛型类型，使用 Type 而不是 Class 以支持嵌套泛型结构。
     */
    private final Type type;

    /**
     * 构造函数，接收带泛型的类型引用用于保留完整类型信息。
     *
     * @param typeReference TypeReference<T> 用于描述泛型类型
     */
    public Fastjson2GenericTypeReferenceHandler(TypeReference<T> typeReference) {
        this.type = typeReference.getType();
    }

    /**
     * 解析 JSON 字符串为 Java 泛型对象
     *
     * @param json 数据库中的 JSON 字符串
     * @return 反序列化后的 Java 对象，失败或为空时返回 null
     */
    @Override
    protected T parse(String json) {
        if (json == null || json.trim().isEmpty()) {
            return null;
        }
        try {
            return JSON.parseObject(
                    json,
                    type,
                    // 支持字段名称的智能匹配，如驼峰与下划线形式自动转换
                    JSONReader.Feature.SupportSmartMatch
            );
        } catch (Exception e) {
            // 解析异常时返回 null（可视情况添加日志）
            return null;
        }
    }

    /**
     * 将 Java 泛型对象序列化为 JSON 字符串，用于写入数据库字段
     *
     * @param obj Java 对象
     * @return JSON 字符串，失败或对象为 null 时返回 null
     */
    @Override
    protected String toJson(T obj) {
        try {
            if (obj == null) {
                return null;
            }
            return JSON.toJSONString(
                    obj,
                    // 不输出数字类型的类名（节省输出）
                    JSONWriter.Feature.NotWriteNumberClassName,
                    // 不输出 Set 类型的类名
                    JSONWriter.Feature.NotWriteSetClassName,
                    // 序列化时包含 null 字段，保持字段完整性
                    JSONWriter.Feature.WriteNulls,
                    // 为兼容 JS，大整数用字符串输出，避免精度丢失
                    JSONWriter.Feature.BrowserCompatible,
                    // BigDecimal 用 plain string 输出，避免科学计数法
                    JSONWriter.Feature.WriteBigDecimalAsPlain
            );
        } catch (Exception e) {
            // 序列化失败时返回 null（可添加日志）
            return null;
        }
    }
}

```

#### 创建 `List<MyData>` 专用的TypeHandler

对于存储 List 这种有泛型的数据就得专门创建一个TypeHandler来使用

```java
package local.ateng.java.mybatisjdk8.handler;

import com.alibaba.fastjson2.TypeReference;
import local.ateng.java.mybatisjdk8.entity.MyData;

import java.util.List;

/**
 * Fastjson2 的自定义类型处理器，用于处理 {@code List<MyData>} 类型与 JSON 字段之间的转换。
 * <p>
 * 继承自 {@link Fastjson2GenericTypeReferenceHandler}，通过传入 {@link TypeReference} 保留泛型类型信息，
 * 实现对嵌套集合类型的正确序列化与反序列化。
 *
 * <p>通常在 MyBatis Plus 中用于如下字段：</p>
 *
 * <pre>{@code
 * @TableField(typeHandler = Fastjson2ListMyDataTypeHandler.class)
 * private List<MyData> dataList;
 * }</pre>
 *
 * @author 孔余
 * @since 2025-07-28
 */
@MappedJdbcTypes({JdbcType.VARCHAR, JdbcType.LONGVARCHAR, JdbcType.OTHER}) // 数据库字段类型
@MappedTypes({List.class})     // Java 类型
public class Fastjson2ListMyDataTypeHandler extends Fastjson2GenericTypeReferenceHandler<List<MyData>> {

    /**
     * 默认构造函数，传入 List<MyData> 的类型引用以保留泛型信息。
     */
    public Fastjson2ListMyDataTypeHandler() {
        super(new TypeReference<List<MyData>>() {
        });
    }

}
```

#### 创建 `MyData` 专用的TypeHandler

对于 Bean 这种实体类，没有其他特点的序列化需求使用 Mybatis Plus自带的 Fastjson2TypeHandler 就够用了，或者上面自定义配置的Fastjson2TypeHandler也可以。这里只是给出 Bean 这种的用法，实际根据需求选择。

```java
package local.ateng.java.mybatisjdk8.handler;

import com.alibaba.fastjson2.TypeReference;
import local.ateng.java.mybatisjdk8.entity.MyData;

/**
 * Fastjson2 类型处理器，用于将 {@link MyData} 类型与 JSON 字段之间进行序列化与反序列化。
 * <p>
 * 继承自 {@link Fastjson2GenericTypeReferenceHandler}，通过 {@link TypeReference} 保留泛型类型信息，
 * 实现对 MyBatis Plus 字段的自动 JSON 映射。
 *
 * <p>典型用法如下：</p>
 * <pre>{@code
 * @TableField(typeHandler = Fastjson2MyDataTypeHandler.class)
 * private MyData data;
 * }</pre>
 *
 * @author 孔余
 * @since 2025-07-28
 */
@MappedJdbcTypes({JdbcType.VARCHAR, JdbcType.LONGVARCHAR, JdbcType.OTHER}) // 数据库字段类型
@MappedTypes({MyData.class})     // Java 类型
public class Fastjson2MyDataTypeHandler extends Fastjson2GenericTypeReferenceHandler<MyData> {

    /**
     * 默认构造方法，传入 MyData 的类型引用，用于保留类型信息以支持反序列化。
     */
    public Fastjson2MyDataTypeHandler() {
        super(new TypeReference<MyData>() {
        });
    }
}
```



### JacksonTypeHandler

```java
package local.ateng.java.mybatisjdk8.handler;


import com.baomidou.mybatisplus.extension.handlers.AbstractJsonTypeHandler;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.databind.jsontype.impl.LaissezFaireSubTypeValidator;
import com.fasterxml.jackson.databind.module.SimpleModule;
import com.fasterxml.jackson.databind.ser.std.ToStringSerializer;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.fasterxml.jackson.datatype.jsr310.deser.LocalDateDeserializer;
import com.fasterxml.jackson.datatype.jsr310.deser.LocalDateTimeDeserializer;
import com.fasterxml.jackson.datatype.jsr310.ser.LocalDateSerializer;
import com.fasterxml.jackson.datatype.jsr310.ser.LocalDateTimeSerializer;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.text.SimpleDateFormat;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.TimeZone;

/**
 * 通用的 Jackson 类型处理器，用于将 JSON 字段与 Java 对象之间互相转换。
 * <p>
 * 该处理器基于 Jackson 实现，适用于 MyBatis Plus 的 JSON 类型字段映射。
 * <p>
 * 通常用于如下场景：
 * <pre>{@code
 * @TableField(typeHandler = JacksonTypeHandler.class)
 * private MyEntity data;
 * }</pre>
 * <pre>{@code
 *  * @TableField(typeHandler = JacksonTypeHandler.class)
 *  * private List<MyEntity> dataList;
 *  * }</pre>
 *
 * @param <T> 要序列化或反序列化的目标类型
 * @author 孔余
 * @since 2025-07-28
 */
@MappedJdbcTypes({JdbcType.VARCHAR, JdbcType.LONGVARCHAR, JdbcType.OTHER}) // 数据库字段类型
@MappedTypes({Map.class, List.class, JsonNode.class, ObjectNode.class, ArrayNode.class})     // Java 类型
public class JacksonTypeHandler<T> extends AbstractJsonTypeHandler<T> {

    /**
     * Jackson 的全局 ObjectMapper 实例（懒加载、单例）
     */
    private static ObjectMapper OBJECT_MAPPER;

    /**
     * 目标类型的 Class 对象，用于反序列化
     */
    private final Class<T> type;

    /**
     * 构造函数，指定当前处理的对象类型
     *
     * @param type 要处理的 Java 类型
     */
    public JacksonTypeHandler(Class<T> type) {
        this.type = type;
    }

    /**
     * 反序列化 JSON 字符串为 Java 对象
     *
     * @param json JSON 字符串
     * @return Java 对象，失败或为空时返回 null
     */
    @Override
    protected T parse(String json) {
        try {
            return getObjectMapper().readValue(json, this.type);
        } catch (Exception e) {
            // 可按需添加日志记录
            return null;
        }
    }

    /**
     * 将 Java 对象序列化为 JSON 字符串
     *
     * @param obj Java 对象
     * @return JSON 字符串，失败或对象为 null 时返回 null
     */
    @Override
    protected String toJson(T obj) {
        try {
            if (obj == null) {
                return null;
            }
            return getObjectMapper().writeValueAsString(obj);
        } catch (Exception e) {
            // 可按需添加日志记录
            return null;
        }
    }

    // 日期与时间格式化
    public static String DEFAULT_TIME_ZONE = "Asia/Shanghai";
    public static String DEFAULT_DATE_FORMAT = "yyyy-MM-dd";
    public static String DEFAULT_DATE_TIME_FORMAT = "yyyy-MM-dd HH:mm:ss.SSS";

    public static ObjectMapper getObjectMapper() {
        if (OBJECT_MAPPER == null) {
            synchronized (JacksonTypeHandler.class) {
                if (OBJECT_MAPPER == null) {
                    OBJECT_MAPPER = new ObjectMapper();
                    // 配置日期和时间的序列化与反序列化
                    customizeJsonDateTime(OBJECT_MAPPER, DEFAULT_TIME_ZONE, DEFAULT_DATE_FORMAT, DEFAULT_DATE_TIME_FORMAT);
                    // 配置 JSON 序列化相关设置
                    customizeJsonSerialization(OBJECT_MAPPER);
                    // 配置 JSON 反序列化相关设置
                    customizeJsonDeserialization(OBJECT_MAPPER);
                    // 配置 JSON 解析相关设置
                    customizeJsonParsing(OBJECT_MAPPER);
                    // 配置反序列化时自动转换的设置
                    customizeJsonClassType(OBJECT_MAPPER);
                }
            }
        }
        return OBJECT_MAPPER;
    }

    /**
     * 自定义 Jackson 时间日期的序列化和反序列化规则
     *
     * @param objectMapper Jackson 的 ObjectMapper 实例
     */
    public static void customizeJsonDateTime(ObjectMapper objectMapper, String timeZone, String dateFormat, String dateTimeFormat) {
        // 设置全局时区，确保 Date 类型数据使用此时区
        objectMapper.setTimeZone(TimeZone.getTimeZone(timeZone));

        // 关闭默认时间戳序列化，改为标准格式
        objectMapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);

        // 避免与 JavaTimeModule 冲突
        objectMapper.setDateFormat(new SimpleDateFormat(dateTimeFormat));

        // Java 8 时间模块
        JavaTimeModule javaTimeModule = new JavaTimeModule();

        // LocalDateTime 序列化 & 反序列化
        javaTimeModule.addSerializer(LocalDateTime.class,
                new LocalDateTimeSerializer(DateTimeFormatter.ofPattern(dateTimeFormat)));
        javaTimeModule.addDeserializer(LocalDateTime.class,
                new LocalDateTimeDeserializer(DateTimeFormatter.ofPattern(dateTimeFormat)));

        // LocalDate 序列化 & 反序列化
        javaTimeModule.addSerializer(LocalDate.class,
                new LocalDateSerializer(DateTimeFormatter.ofPattern(dateFormat)));
        javaTimeModule.addDeserializer(LocalDate.class,
                new LocalDateDeserializer(DateTimeFormatter.ofPattern(dateFormat)));

        // 注册 JavaTimeModule
        objectMapper.registerModule(javaTimeModule);
    }

    /**
     * 自定义 Jackson 序列化规则
     *
     * @param objectMapper Jackson 的 ObjectMapper 实例
     */
    public static void customizeJsonSerialization(ObjectMapper objectMapper) {
        // 关闭 JSON 美化输出（生产环境建议关闭，提高性能）
        objectMapper.disable(SerializationFeature.INDENT_OUTPUT);

        // 避免 "No serializer found for class" 异常
        objectMapper.disable(SerializationFeature.FAIL_ON_EMPTY_BEANS);

        // 过滤 null 值，减少 JSON 体积
        //objectMapper.setSerializationInclusion(JsonInclude.Include.NON_NULL);

        // 过滤空集合、空字符串等无效数据，进一步精简 JSON
        //objectMapper.setSerializationInclusion(JsonInclude.Include.NON_EMPTY);

        // 不过滤 null、空集合、空字符串等无效数据值，保持数据的原始状态
        objectMapper.setSerializationInclusion(JsonInclude.Include.ALWAYS);

        // 枚举类型：使用 `toString()` 方式序列化，而不是默认的 `name()`
        objectMapper.enable(SerializationFeature.WRITE_ENUMS_USING_TO_STRING);

        // BigDecimal 序列化时不使用科学计数法，确保数据精确
        objectMapper.enable(SerializationFeature.WRITE_BIGDECIMAL_AS_PLAIN);

        // 排序字段名，保证 JSON 输出的键顺序固定（有助于缓存和数据比对）
        objectMapper.enable(SerializationFeature.ORDER_MAP_ENTRIES_BY_KEYS);

        // 将 Long 和 BigInteger 序列化为字符串，防止 JavaScript 丢失精度
        SimpleModule simpleModule = new SimpleModule();
        ToStringSerializer stringSerializer = ToStringSerializer.instance;
        simpleModule.addSerializer(BigInteger.class, stringSerializer);
        simpleModule.addSerializer(BigDecimal.class, stringSerializer);
        simpleModule.addSerializer(BigInteger.class, stringSerializer);
        simpleModule.addSerializer(Long.class, stringSerializer);
        simpleModule.addSerializer(Long.TYPE, stringSerializer);
        objectMapper.registerModule(simpleModule);
    }

    /**
     * 自定义 Jackson 反序列化规则
     *
     * @param objectMapper Jackson 的 ObjectMapper 实例
     */
    public static void customizeJsonDeserialization(ObjectMapper objectMapper) {
        // 允许单个值转数组（例如 1 -> [1]）
        objectMapper.enable(DeserializationFeature.ACCEPT_SINGLE_VALUE_AS_ARRAY);

        // 忽略未知字段（避免因缺少字段报错，提升兼容性）
        objectMapper.disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES);

        // 处理 原始类型（如 int, long, boolean 等）在反序列化时如果遇到 null 值将其替换为默认值，而不是抛出异常
        objectMapper.configure(DeserializationFeature.FAIL_ON_NULL_FOR_PRIMITIVES, false);

        // 使用 BigDecimal 反序列化浮点数，避免精度丢失
        objectMapper.enable(DeserializationFeature.USE_BIG_DECIMAL_FOR_FLOATS);

        // 使用枚举的 `toString()` 方法进行反序列化，而不是默认的 `name()`
        objectMapper.enable(DeserializationFeature.READ_ENUMS_USING_TO_STRING);

        // 允许特殊字符转义
        objectMapper.enable(JsonParser.Feature.ALLOW_UNQUOTED_CONTROL_CHARS);
        objectMapper.enable(JsonParser.Feature.ALLOW_BACKSLASH_ESCAPING_ANY_CHARACTER);

        // 错误时提供类型检查，增强反序列化稳定性
        objectMapper.enable(DeserializationFeature.FAIL_ON_INVALID_SUBTYPE);
    }

    /**
     * 自定义 Jackson JSON 解析设置
     *
     * @param objectMapper Jackson 的 ObjectMapper 实例
     */
    public static void customizeJsonParsing(ObjectMapper objectMapper) {
        // 允许 JSON 中带注释，方便开发阶段使用
        objectMapper.configure(JsonParser.Feature.ALLOW_COMMENTS, true);

        // 允许字段名不带引号（可处理某些特殊格式的 JSON）
        objectMapper.configure(JsonParser.Feature.ALLOW_UNQUOTED_FIELD_NAMES, true);

        // 允许单引号作为 JSON 字符串的定界符（适用于某些特殊格式）
        objectMapper.configure(JsonParser.Feature.ALLOW_SINGLE_QUOTES, true);

        // 允许控制字符的转义（例如，`\n` 或 `\t`）
        objectMapper.configure(JsonParser.Feature.ALLOW_UNQUOTED_CONTROL_CHARS, true);

        // 允许反斜杠转义任何字符（如：`\\`）
        objectMapper.configure(JsonParser.Feature.ALLOW_BACKSLASH_ESCAPING_ANY_CHARACTER, true);

        // 允许无效的 UTF-8 字符（如果 JSON 编码不完全符合标准）
        objectMapper.configure(JsonParser.Feature.IGNORE_UNDEFINED, true);

        // 允许 JSON 中无序字段（通常是为了性能优化）
        objectMapper.configure(JsonParser.Feature.ALLOW_NON_NUMERIC_NUMBERS, true);
    }

    /**
     * 自定义 ObjectMapper 配置以启用默认类型标记。
     * 该方法的作用是在 JSON 序列化和反序列化时包含类类型信息，
     * 以便在反序列化时能够正确地识别对象的具体类型。
     *
     * @param objectMapper 要配置的 ObjectMapper 实例
     */
    public static void customizeJsonClassType(ObjectMapper objectMapper) {
        // 启用默认类型标记，使 JSON 中包含对象的类信息
        objectMapper.activateDefaultTyping(
                // 允许所有子类型的验证器（最宽松）
                LaissezFaireSubTypeValidator.instance,
                // 仅对非 final 类启用类型信息
                ObjectMapper.DefaultTyping.NON_FINAL,
                // 以 JSON 属性的形式存储类型信息
                JsonTypeInfo.As.PROPERTY
        );
    }

}
```

### 注册全局TypeHandler

注册后，MyBatis 会自动根据 `@MappedJdbcTypes` 和 `@MappedTypes` 匹配，几乎不用额外写 `typeHandler`。

```java
@Configuration
@MapperScan("local.ateng.java.mybatisjdk8.**.mapper")
public class MyBatisPlusConfiguration {

    /**
     * 自定义 MyBatis 全局配置。
     * <p>
     * 方式一：逐个注册 TypeHandler。
     * 方式二：包扫描注册（推荐）。
     * </p>
     *
     * @return ConfigurationCustomizer 配置定制器
     */
    @Bean
    public ConfigurationCustomizer configurationCustomizer() {
        return configuration -> {
            // 方式一：逐个注册自定义 TypeHandler
            configuration.getTypeHandlerRegistry().register(GeometryTypeHandler.class);
            configuration.getTypeHandlerRegistry().register(JacksonTypeHandler.class);
            configuration.getTypeHandlerRegistry().register(UUIDTypeHandler.class);

            // 方式二：包扫描注册，推荐用于统一管理 TypeHandler
            // configuration.getTypeHandlerRegistry()
            //         .register("com.example.mybatis.handler");
        };
    }
}
```



## 拦截器Interceptor



