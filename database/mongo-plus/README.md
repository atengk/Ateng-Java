# MongoPlus

[Mongo-Plus](https://gitee.com/anwena/mongo-plus)（简称 MP）是一个 [MongoDB](https://www.mongodb.com/) 的操作工具，可和现有mongoDB框架结合使用，为简化开发、提高效率而生。

**特性**

- **无侵入**：只做增强不做改变，引入它不会对现有工程产生影响，如丝般顺滑
- **损耗小**：启动即会自动注入基本 CURD，性能基本无损耗，直接面向对象操作
- **强大的 CRUD 操作**：通用 Service，仅仅通过少量配置即可实现单表大部分 CRUD 操作，更有强大的条件构造器，满足各类使用需求
- **支持 Lambda 形式调用**：通过 Lambda 表达式，方便的编写各类查询条件，无需再担心字段写错
- **支持主键自动生成**：支持多达 5 种主键策略（内含分布式唯一 ID 生成器 - Sequence），可自由配置，完美解决主键问题
- **支持自定义全局通用操作**：支持全局通用方法注入
- **支持无实体类情况下的操作**
- **支持动态数据源**
- **支持逻辑删除、防止全集合更新和删除、自动填充等等功能**

参考：[官方文档](https://www.mongoplus.cn/docs/preface.html)



## 基础配置

### 依赖配置

添加依赖

```xml
<mongo-plus.version>2.1.6.1</mongo-plus.version>
<!-- MongoPlus 依赖 -->
<dependency>
    <groupId>com.mongoplus</groupId>
    <artifactId>mongo-plus-boot-starter</artifactId>
    <version>${mongo-plus.version}</version>
</dependency>
```

完整的pom.xml文件如下

```xml
<?xml version="1.0" encoding="UTF-8"?>
<project xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance" xmlns="http://maven.apache.org/POM/4.0.0"
         xsi:schemaLocation="http://maven.apache.org/POM/4.0.0 https://maven.apache.org/xsd/maven-4.0.0.xsd">
    <!-- 项目模型版本 -->
    <modelVersion>4.0.0</modelVersion>

    <!-- 项目坐标 -->
    <groupId>local.ateng.java</groupId>
    <artifactId>mongo-plus</artifactId>
    <version>v1.0</version>
    <name>mongo-plus</name>
    <description>
        MongoPlus: 🔥🔥🔥使用MyBatisPlus的方式，优雅的操作MongoDB
    </description>
    <url>https://www.mongoplus.cn/</url>

    <!-- 项目属性 -->
    <properties>
        <java.version>21</java.version>
        <project.build.sourceEncoding>UTF-8</project.build.sourceEncoding>
        <project.reporting.outputEncoding>UTF-8</project.reporting.outputEncoding>
        <spring-boot.version>3.4.1</spring-boot.version>
        <maven-compiler.version>3.12.1</maven-compiler.version>
        <lombok.version>1.18.36</lombok.version>
        <fastjson2.version>2.0.53</fastjson2.version>
        <hutool.version>5.8.35</hutool.version>
        <mongo-plus.version>2.1.6.1</mongo-plus.version>
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

        <!-- 高性能的JSON库 -->
        <!-- https://github.com/alibaba/fastjson2/wiki/fastjson2_intro_cn#0-fastjson-20%E4%BB%8B%E7%BB%8D -->
        <dependency>
            <groupId>com.alibaba.fastjson2</groupId>
            <artifactId>fastjson2</artifactId>
            <version>${fastjson2.version}</version>
        </dependency>
        <!-- 在 Spring 中集成 Fastjson2 -->
        <!-- https://github.com/alibaba/fastjson2/blob/main/docs/spring_support_cn.md -->
        <dependency>
            <groupId>com.alibaba.fastjson2</groupId>
            <artifactId>fastjson2-extension-spring6</artifactId>
            <version>${fastjson2.version}</version>
        </dependency>

        <!-- Hutool: Java工具库，提供了许多实用的工具方法 -->
        <dependency>
            <groupId>cn.hutool</groupId>
            <artifactId>hutool-all</artifactId>
            <version>${hutool.version}</version>
        </dependency>

        <!-- JavaFaker: 用于生成虚假数据的Java库 -->
        <dependency>
            <groupId>com.github.javafaker</groupId>
            <artifactId>javafaker</artifactId>
            <version>1.0.2</version>
        </dependency>

        <!-- MongoPlus 依赖 -->
        <dependency>
            <groupId>com.mongoplus</groupId>
            <artifactId>mongo-plus-boot-starter</artifactId>
            <version>${mongo-plus.version}</version>
        </dependency>

    </dependencies>

    <!-- Spring Boot 依赖管理 -->
    <dependencyManagement>
        <dependencies>
            <!-- SpringBoot 依赖管理 -->
            <dependency>
                <groupId>org.springframework.boot</groupId>
                <artifactId>spring-boot-dependencies</artifactId>
                <version>${spring-boot.version}</version>
                <type>pom</type>
                <scope>import</scope>
            </dependency>
        </dependencies>
    </dependencyManagement>

    <!-- 插件仓库配置 -->
    <repositories>
        <!-- Central Repository -->
        <repository>
            <id>central</id>
            <name>阿里云中央仓库</name>
            <url>https://maven.aliyun.com/repository/central</url>
            <!--<name>Maven官方中央仓库</name>
            <url>https://repo.maven.apache.org/maven2/</url>-->
        </repository>
    </repositories>

    <!-- 构建配置 -->
    <build>
        <finalName>${project.name}-${project.version}</finalName>
        <plugins>
            <!-- Maven 编译插件 -->
            <plugin>
                <groupId>org.apache.maven.plugins</groupId>
                <artifactId>maven-compiler-plugin</artifactId>
                <version>${maven-compiler.version}</version>
                <configuration>
                    <source>${java.version}</source>
                    <target>${java.version}</target>
                    <encoding>${project.build.sourceEncoding}</encoding>
                    <!-- 编译参数 -->
                    <compilerArgs>
                        <!-- 启用Java 8参数名称保留功能 -->
                        <arg>-parameters</arg>
                    </compilerArgs>
                </configuration>
            </plugin>

            <!-- Spring Boot Maven 插件 -->
            <plugin>
                <groupId>org.springframework.boot</groupId>
                <artifactId>spring-boot-maven-plugin</artifactId>
                <version>${spring-boot.version}</version>
                <executions>
                    <execution>
                        <id>repackage</id>
                        <goals>
                            <goal>repackage</goal>
                        </goals>
                    </execution>
                </executions>
            </plugin>
        </plugins>
        <resources>
            <!-- 第一个资源配置块 -->
            <resource>
                <directory>src/main/resources</directory>
                <filtering>false</filtering>
            </resource>
            <!-- 第二个资源配置块 -->
            <resource>
                <directory>src/main/resources</directory>
                <includes>
                    <include>application*</include>
                    <include>bootstrap*.yml</include>
                    <include>common*</include>
                    <include>banner*</include>
                </includes>
                <filtering>true</filtering>
            </resource>
        </resources>
    </build>

</project>
```

### 编辑配置文件

编辑`application.yml`配置文件，详情参考官网的[配置文档](https://www.easy-es.cn/pages/eddebb/)

```yaml
server:
  port: 15007
  servlet:
    context-path: /
spring:
  main:
    web-application-type: servlet
  application:
    name: ${project.artifactId}
---
# MongoPlus 的相关配置
mongo-plus:
  data:
    mongodb:
      host: 192.168.1.10   #ip
      port: 33627   #端口
      database: ateng    #数据库名
      username: root    #用户名，没有可不填（若账号中出现@,!等等符号，不需要再进行转码！！！）
      password: Admin@123    #密码，同上（若密码中出现@,!等等符号，不需要再进行转码！！！）
      authenticationDatabase: admin     #验证数据库
      connectTimeoutMS: 50000   #在超时之前等待连接打开的最长时间（以毫秒为单位）
  # 日志配置
  log: true
  format: true
  pretty: true
  configuration:
    # 字段配置
    field:
      camelToUnderline: true
      ignoringNull: false
    # 集合配置
    collection:
      mappingStrategy: CAMEL_TO_UNDERLINE
      block-attack-inner: true
    banner: false
```

### 创建实体类

注解详情参考[官网文档](https://www.easy-es.cn/pages/ac41f0/)

```java
package local.ateng.java.mongo.entity;

import com.mongoplus.annotation.ID;
import com.mongoplus.annotation.collection.CollectionName;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serial;
import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Date;


@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@CollectionName("my_user")
public class MyUser implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * 主键id
     */
    @ID
    private String id;

    /**
     * 名称
     */
    private String name;

    /**
     * 年龄
     */
    private Integer age;

    /**
     * 手机号码
     */
    private String phoneNumber;

    /**
     * 邮箱
     */
    private String email;

    /**
     * IP地址
     */
    private String ipaddress;

    /**
     * 公司名称
     */
    private String company;

    /**
     * 分数
     */
    private BigDecimal score;

    /**
     * 生日
     */
    private Date birthday;

    /**
     * 所在省份
     */
    private String province;

    /**
     * 所在城市
     */
    private String city;

    /**
     * 地址
     */
    private String address;

    /**
     * 经纬度(lat,lng)
     */
    private String location;

    /**
     * 一段文字
     */
    private String paragraph;

    /**
     * 记录创建时间，默认当前时间
     */
    private LocalDateTime createTime;

}
```

### 创建Service

```java
package local.ateng.java.mongo.service;

import com.mongoplus.service.IService;
import local.ateng.java.mongo.entity.MyUser;

public interface MyUserService extends IService<MyUser> {
}
```

### 创建ServiceImpl

```java
package local.ateng.java.mongo.service.impl;

import com.mongoplus.service.impl.ServiceImpl;
import local.ateng.java.mongo.entity.MyUser;
import local.ateng.java.mongo.service.MyUserService;
import org.springframework.stereotype.Service;

@Service
public class MyUserServiceImpl extends ServiceImpl<MyUser> implements MyUserService {
}
```

### 创建数据生成器

```java
package local.ateng.java.mongo.init;

import com.github.javafaker.Faker;
import local.ateng.java.mongo.entity.MyUser;
import lombok.Getter;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * 初始化数据
 *
 * @author 孔余
 * @email 2385569970@qq.com
 * @since 2025-01-09
 */
@Getter
public class InitData {
    public static List<MyUser> list;

    static {
        //生成测试数据
        // 创建一个Java Faker实例，指定Locale为中文
        Faker faker = new Faker(new Locale("zh-CN"));
        List<MyUser> userList = new ArrayList();
        for (int i = 1; i <= 1000; i++) {
            MyUser user = new MyUser();
            user.setName(faker.name().fullName());
            user.setAge(faker.number().numberBetween(0, 100));
            user.setPhoneNumber(faker.phoneNumber().cellPhone());
            user.setEmail(faker.internet().emailAddress());
            user.setIpaddress(faker.internet().ipV4Address());
            user.setCompany(faker.company().name());
            user.setScore(BigDecimal.valueOf(faker.number().randomDouble(2, 0, 100)));
            user.setBirthday(faker.date().birthday());
            user.setProvince(faker.address().state());
            user.setCity(faker.address().cityName());
            user.setAddress(faker.address().fullAddress());
            user.setLocation(faker.address().latitude() + "," + faker.address().longitude());
            user.setParagraph(faker.lorem().paragraph());
            user.setCreateTime(LocalDateTime.now());
            userList.add(user);
        }
        list = userList;
    }
}
```



## 使用MongoPlus

### 创建测试类

```java
package local.ateng.java.mongo;

import cn.hutool.core.date.LocalDateTimeUtil;
import com.mongoplus.model.PageResult;
import local.ateng.java.mongo.entity.MyUser;
import local.ateng.java.mongo.init.InitData;
import local.ateng.java.mongo.service.MyUserService;
import lombok.RequiredArgsConstructor;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.List;

@SpringBootTest
@RequiredArgsConstructor(onConstructor = @__(@Autowired))
public class MongoPlusTests {
    private final MyUserService myUserService;
}
```

### 批量写入数据

```java
    @Test
    public void saveBatch() {
        // 批量写入数据
        Boolean result = myUserService.saveBatch(InitData.list);
        System.out.println(result);
    }
```

### 查询数据列表

```java
    @Test
    public void list() {
        // 查询数据列表
        List<MyUser> list = myUserService.list();
        System.out.println(list);
    }
```

### 模糊查询

```java
    @Test
    public void listByLike() {
        // 模糊查询
        List<MyUser> list = myUserService.lambdaQuery()
                .like(MyUser::getProvince, "重庆")
                .orderByAsc(MyUser::getCreateTime)
                .list();
        System.out.println(list);
    }
```

### 时间范围查询

```java
    @Test
    public void listByTime() {
        // 时间范围查询
        List<MyUser> list = myUserService.lambdaQuery()
                .between(MyUser::getBirthday, LocalDateTimeUtil.parse("1990-01-01 00:00:00", "yyyy-MM-dd HH:mm:ss"), LocalDateTimeUtil.parse("2000-01-01 00:00:00", "yyyy-MM-dd HH:mm:ss"), true)
                .orderByAsc(MyUser::getCreateTime)
                .list();
        System.out.println(list);
    }
```

### 分页查询

```java
    @Test
    public void page() {
        // 分页查询
        PageResult<MyUser> page = myUserService.lambdaQuery()
                .orderByDesc(MyUser::getCreateTime)
                .page(1, 20);
        System.out.println(page);
    }
```

