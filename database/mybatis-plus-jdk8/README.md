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
     * 添加分页插件
     * https://baomidou.com/plugins/pagination/
     */
    @Bean
    public MybatisPlusInterceptor mybatisPlusInterceptor() {
        MybatisPlusInterceptor interceptor = new MybatisPlusInterceptor();
        interceptor.addInnerInterceptor(new PaginationInnerInterceptor(DbType.MYSQL)); // 如果配置多个插件, 切记分页最后添加
        // 如果有多数据源可以不配具体类型, 否则都建议配上具体的 DbType
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
    IPage<JSONObject> selectUsersWithOrderPageWrapper(Page page, @Param("ew") QueryWrapper<MyUser> wrapper);
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
    IPage<JSONObject> selectUsersWithOrderPageWrapper(Page page, @Param("ew") QueryWrapper<MyUser> wrapper);
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

```java
package local.ateng.java.mybatisjdk8.handler;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.parser.Feature;
import com.alibaba.fastjson.parser.ParserConfig;
import com.alibaba.fastjson.serializer.SerializerFeature;
import com.baomidou.mybatisplus.extension.handlers.AbstractJsonTypeHandler;

/**
 * MyBatis Plus 使用 Fastjson 实现的自定义 TypeHandler
 * <p>
 * 该类用于 JSON 字符串与 Java 对象之间的转换，推荐用于字段存储为 JSON 的场景。
 * 在处理过程中，对 JSON 的解析、序列化做了一些容错处理，比如：
 * 1. JSON 字符串字段有多余属性时不抛出异常。
 * 2. 对 JSON 解析和序列化时的配置进行自定义，以确保兼容性和性能。
 *
 * @param <T> JSON 对应的 Java 类型
 * @since 2025-07-25
 */
public class FastjsonTypeHandler<T> extends AbstractJsonTypeHandler<T> {

    // 存储 Java 对象的目标类型
    private final Class<T> type;

    /**
     * 构造方法，初始化目标类型
     *
     * @param type Java 对象的类型
     */
    public FastjsonTypeHandler(Class<T> type) {
        this.type = type;
    }

    /**
     * 解析 JSON 字符串为 Java 对象
     * <p>
     * 通过 Fastjson 库的 JSON.parseObject 方法，将 JSON 字符串转换为指定的 Java 对象。
     * 同时，配置了自动类型支持、忽略不匹配字段以及支持数组映射为对象等特性。
     *
     * @param json JSON 字符串
     * @return 转换后的 Java 对象，如果解析失败返回 null
     */
    @Override
    protected T parse(String json) {
        try {
            // 配置 ParserConfig，设置自动类型支持，并允许包名前缀
            ParserConfig config = new ParserConfig();
            config.addAccept("local.ateng.java.");

            return JSON.parseObject(
                    json, this.type,
                    config,
                    // 支持 @type 字段进行反序列化（用于多态、自动识别类型）
                    Feature.SupportAutoType,
                    // JSON 中有多余字段时忽略，不抛异常
                    Feature.IgnoreNotMatch,
                    // 支持将 JSON 数组映射为 Java Bean（按顺序赋值）
                    Feature.SupportArrayToBean
            );
        } catch (Exception e) {
            // 解析失败时，不抛出异常，直接返回 null
            return null;
        }
    }

    /**
     * 将 Java 对象转换为 JSON 字符串
     * <p>
     * 使用 Fastjson 序列化对象时，启用了多个序列化特性，包括：
     * 1. 输出类名（支持多态反序列化）。
     * 2. 包含 null 值字段。
     * 3. 关闭循环引用检测以提高性能。
     *
     * @param obj Java 对象
     * @return 对象的 JSON 字符串表示
     */
    @Override
    protected String toJson(T obj) {
        try {
            if (obj == null) {
                return null;
            }

            // 序列化对象为 JSON 字符串，并启用相关特性
            return JSON.toJSONString(obj,
                    // 添加 @type 字段（全类名），支持反序列化为原类型
                    SerializerFeature.WriteClassName,
                    // Map 中字段即使为 null 也输出
                    SerializerFeature.WriteMapNullValue,
                    // 将 null 的 List 序列化为 []
                    SerializerFeature.WriteNullListAsEmpty,
                    // 将 null 的字符串序列化为 ""
                    SerializerFeature.WriteNullStringAsEmpty,
                    // null 的数字字段序列化为 0
                    SerializerFeature.WriteNullNumberAsZero,
                    // null 的布尔字段序列化为 false
                    SerializerFeature.WriteNullBooleanAsFalse,
                    // 关闭循环引用检测（性能更高）
                    SerializerFeature.DisableCircularReferenceDetect
            );
        } catch (Exception e) {
            return null;
        }
    }

}
```

### Fastjson2TypeHandler

```java

```

### JacksonTypeHandler

```java

```

