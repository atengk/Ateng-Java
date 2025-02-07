# MapStructPlus

Mapstruct Plus 是 Mapstruct 的增强工具，在 Mapstruct 的基础上，实现了自动生成 Mapper 接口的功能，并强化了部分功能，使 Java 类型转换更加便捷、优雅。

参考：[官方文档](https://www.mapstruct.plus/)



## 基础配置

### 依赖配置

**添加依赖**

```xml
<mapstruct-plus.version>1.4.6</mapstruct-plus.version>
<!-- Mapstruct Plus依赖 -->
<dependency>
    <groupId>io.github.linpeilie</groupId>
    <artifactId>mapstruct-plus-spring-boot-starter</artifactId>
    <version>${mapstruct-plus.version}</version>
</dependency>
```

**添加annotation**

```xml
<annotationProcessorPaths>
    <path>
        <groupId>io.github.linpeilie</groupId>
        <artifactId>mapstruct-plus-processor</artifactId>
        <version>${mapstruct-plus.version}</version>
    </path>
    <path>
        <groupId>org.projectlombok</groupId>
        <artifactId>lombok</artifactId>
        <version>${lombok.version}</version>
    </path>
    <path>
        <groupId>org.projectlombok</groupId>
        <artifactId>lombok-mapstruct-binding</artifactId>
        <version>0.2.0</version>
    </path>
</annotationProcessorPaths>
```

**完整的pom.xml文件如下**

```xml
<?xml version="1.0" encoding="UTF-8"?>
<project xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance" xmlns="http://maven.apache.org/POM/4.0.0"
         xsi:schemaLocation="http://maven.apache.org/POM/4.0.0 https://maven.apache.org/xsd/maven-4.0.0.xsd">
    <!-- 项目模型版本 -->
    <modelVersion>4.0.0</modelVersion>

    <!-- 项目坐标 -->
    <groupId>local.ateng.java</groupId>
    <artifactId>mapstruct-plus</artifactId>
    <version>v1.0</version>
    <name>mapstruct-plus</name>
    <description>MapStructPlus可能是最简单最强大的Java Bean转换工具</description>
    <url>https://www.mapstruct.plus/</url>

    <!-- 项目属性 -->
    <properties>
        <java.version>21</java.version>
        <project.build.sourceEncoding>UTF-8</project.build.sourceEncoding>
        <project.reporting.outputEncoding>UTF-8</project.reporting.outputEncoding>
        <spring-boot.version>3.4.1</spring-boot.version>
        <maven-compiler.version>3.12.1</maven-compiler.version>
        <lombok.version>1.18.36</lombok.version>
        <mapstruct-plus.version>1.4.6</mapstruct-plus.version>
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

        <!-- JavaFaker: 用于生成虚假数据的Java库 -->
        <dependency>
            <groupId>com.github.javafaker</groupId>
            <artifactId>javafaker</artifactId>
            <version>1.0.2</version>
        </dependency>

        <!-- Mapstruct Plus依赖 -->
        <dependency>
            <groupId>io.github.linpeilie</groupId>
            <artifactId>mapstruct-plus-spring-boot-starter</artifactId>
            <version>${mapstruct-plus.version}</version>
        </dependency>

    </dependencies>

    <!-- Spring Boot 依赖管理 -->
    <dependencyManagement>
        <dependencies>
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
                    <annotationProcessorPaths>
                        <path>
                            <groupId>io.github.linpeilie</groupId>
                            <artifactId>mapstruct-plus-processor</artifactId>
                            <version>${mapstruct-plus.version}</version>
                        </path>
                        <path>
                            <groupId>org.projectlombok</groupId>
                            <artifactId>lombok</artifactId>
                            <version>${lombok.version}</version>
                        </path>
                        <path>
                            <groupId>org.projectlombok</groupId>
                            <artifactId>lombok-mapstruct-binding</artifactId>
                            <version>0.2.0</version>
                        </path>
                    </annotationProcessorPaths>
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

### 创建DTO

```java
package local.ateng.java.mapstruct.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serial;
import java.io.Serializable;
import java.math.BigDecimal;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MyUserDto implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    private Long id;
    private String name;
    private String phoneNumber;
}
```

### 创建实体类

注意需要添加 `@AutoMapper(target = MyUserDto.class)` 注解

```java
package local.ateng.java.mapstruct.entity;

import io.github.linpeilie.annotations.AutoMapper;
import local.ateng.java.mapstruct.dto.MyUserDto;
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
@AutoMapper(target = MyUserDto.class)
public class MyUser implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * 主键id
     */
    private Long id;

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

### 创建数据生成器

```java
package local.ateng.java.mapstruct.init;

import com.github.javafaker.Faker;
import local.ateng.java.mapstruct.entity.MyUser;
import lombok.Getter;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.UUID;

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
            user.setId(UUID.randomUUID().getMostSignificantBits() & Long.MAX_VALUE);
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



## 使用MapStructPlus

参考：[官方文档](https://www.mapstruct.plus/guide/class-convert.html)

### 创建测试类

```java
package local.ateng.java.mapstruct;

import io.github.linpeilie.Converter;
import local.ateng.java.mapstruct.dto.MyUserDto;
import local.ateng.java.mapstruct.entity.MyUser;
import local.ateng.java.mapstruct.init.InitData;
import lombok.RequiredArgsConstructor;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest
@RequiredArgsConstructor(onConstructor = @__(@Autowired))
public class MapStructPlusTests {
    private final Converter converter;
}
```

### 对象转换

```java
    @Test
    void test() {
        MyUser myUser = InitData.list.get(0);
        MyUserDto dto = converter.convert(myUser, MyUserDto.class);
        System.out.println(dto);
    }
```

### 指定字段转换

在DTO中添加字段

```java
private BigDecimal score;
private String dateTime;
```

完整DTO如下

```java
public class MyUserDto implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    private Long id;
    private String name;
    private String phoneNumber;
    private BigDecimal score;
    private String dateTime;
}
```

在实体类中给这两个字段添加注解

```java
@AutoMapping(numberFormat = "$0.00")
private BigDecimal score;
@AutoMapping(target = "dateTime", dateFormat = "yyyy-MM-dd HH:mm:ss")
private LocalDateTime createTime;
```

再次运行测试，效果如下

MyUserDto(id=7900202065095051457, name=钱立轩, phoneNumber=15181581966, score=0.86, dateTime=2025-02-07 17:13:50)

