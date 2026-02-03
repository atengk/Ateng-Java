# SpringBoot 2

## 版本信息

| 组件       | 版本   |
| ---------- | ------ |
| JDK        | 8      |
| Maven      | 3.9.12 |
| SpringBoot | 2.7.18 |



## 基础配置

### 配置 pom.xml

```xml
<?xml version="1.0" encoding="UTF-8"?>
<project xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance" xmlns="http://maven.apache.org/POM/4.0.0"
         xsi:schemaLocation="http://maven.apache.org/POM/4.0.0 https://maven.apache.org/xsd/maven-4.0.0.xsd">
    <!-- 项目模型版本 -->
    <modelVersion>4.0.0</modelVersion>

    <!-- 项目坐标 -->
    <groupId>io.github.atengk</groupId>
    <artifactId>boot2-web</artifactId>
    <version>1.0.0</version>
    <name>boot2-web</name>
    <description>SpringBoot2 演示模块</description>
    <url>https://atengk.github.io/dev</url>

    <!-- 项目属性 -->
    <properties>
        <java.version>8</java.version>
        <project.build.sourceEncoding>UTF-8</project.build.sourceEncoding>
        <project.reporting.outputEncoding>UTF-8</project.reporting.outputEncoding>
        <spring-boot.version>2.7.18</spring-boot.version>
        <maven-compiler.version>3.14.1</maven-compiler.version>
        <lombok.version>1.18.42</lombok.version>
        <hutool.version>5.8.43</hutool.version>
        <fastjson2.version>2.0.53</fastjson2.version>
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

        <!-- Hutool 工具类 -->
        <dependency>
            <groupId>cn.hutool</groupId>
            <artifactId>hutool-all</artifactId>
            <version>${hutool.version}</version>
        </dependency>

        <!-- 高性能的JSON库 -->
        <dependency>
            <groupId>com.alibaba.fastjson2</groupId>
            <artifactId>fastjson2</artifactId>
            <version>${fastjson2.version}</version>
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
        <!-- 阿里云中央仓库 -->
        <repository>
            <id>aliyun-central</id>
            <name>阿里云 Maven Central 镜像</name>
            <url>https://maven.aliyun.com/repository/central</url>
        </repository>

        <!-- Maven 官方中央仓库 -->
        <repository>
            <id>maven-central</id>
            <name>Maven 官方中央仓库</name>
            <url>https://repo.maven.apache.org/maven2/</url>
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

### 配置 application.yml

```yaml
server:
  port: 12000
spring:
  application:
    name: ${project.artifactId}

```

### 配置 SpringBootApplication

```java
package io.github.atengk.web;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public class SpringBoot2WebApplication {

    public static void main(String[] args) {
        SpringApplication.run(SpringBoot2WebApplication.class, args);
    }

}
```



## 使用 Springboot 2

### 创建 Controller

```java
package io.github.atengk.web.controller;

import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

/**
 * Spring Boot 2 Web 演示 Controller
 *
 * <p>主要用于验证：
 * <ul>
 *     <li>Spring Boot 2 是否能正常启动</li>
 *     <li>Web 模块是否可用</li>
 *     <li>JSON 返回是否正常</li>
 *     <li>参数绑定、路径变量、请求体解析是否正常</li>
 * </ul>
 *
 * @author 孔余
 * @since 2026-01-29
 */
@RestController
@RequestMapping("/api/demo")
public class DemoController {

    /**
     * 基础连通性测试接口
     *
     * <p>访问：
     * http://localhost:12000/api/demo/hello
     */
    @GetMapping("/hello")
    public String hello() {
        return "Hello Spring Boot 4, time = " + LocalDateTime.now();
    }

    /**
     * 返回 JSON 对象示例
     *
     * <p>访问：
     * http://localhost:12000/api/demo/info
     */
    @GetMapping("/info")
    public Map<String, Object> info() {
        Map<String, Object> map = new HashMap<>();
        map.put("project", "boot4-web");
        map.put("version", "1.0.0");
        map.put("framework", "Spring Boot 4");
        map.put("time", LocalDateTime.now());
        return map;
    }

    /**
     * 路径变量演示
     *
     * <p>访问：
     * http://localhost:12000/api/demo/user/1001
     */
    @GetMapping("/user/{id}")
    public Map<String, Object> getUser(@PathVariable("id") Long id) {
        Map<String, Object> map = new HashMap<>();
        map.put("id", id);
        map.put("username", "user-" + id);
        map.put("createTime", LocalDateTime.now());
        return map;
    }

    /**
     * 请求参数演示
     *
     * <p>访问：
     * http://localhost:12000/api/demo/param?name=atengk&age=18
     */
    @GetMapping("/param")
    public Map<String, Object> param(
            @RequestParam("name") String name,
            @RequestParam(value = "age", required = false) Integer age) {

        Map<String, Object> map = new HashMap<>();
        map.put("name", name);
        map.put("age", age);
        map.put("time", LocalDateTime.now());
        return map;
    }

    /**
     * POST JSON 请求体演示
     *
     * <p>请求示例：
     * <pre>
     * {
     *   "username": "admin",
     *   "password": "123456"
     * }
     * </pre>
     *
     * <p>POST：
     * http://localhost:12000/api/demo/login
     */
    @PostMapping("/login")
    public Map<String, Object> login(@RequestBody Map<String, Object> body) {
        Map<String, Object> result = new HashMap<>();
        result.put("requestBody", body);
        result.put("login", true);
        result.put("time", LocalDateTime.now());
        return result;
    }

    /**
     * 异常测试接口
     *
     * <p>访问：
     * http://localhost:12000/api/demo/error
     */
    @GetMapping("/error")
    public String error() {
        throw new RuntimeException("Spring Boot 4 异常演示接口");
    }
}
```

