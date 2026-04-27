# SpringBoot 3

## 版本信息

| 组件       | 版本   |
| ---------- | ------ |
| JDK        | 21     |
| Maven      | 3.9.12 |
| SpringBoot | 3.5.14 |



## 基础配置

### 配置 pom.xml

```xml
<?xml version="1.0" encoding="UTF-8"?>
<project xmlns="http://maven.apache.org/POM/4.0.0"
         xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
         xsi:schemaLocation="http://maven.apache.org/POM/4.0.0 https://maven.apache.org/xsd/maven-4.0.0.xsd">

    <!-- Maven 模型版本 -->
    <modelVersion>4.0.0</modelVersion>

    <!-- 项目基础信息 -->
    <groupId>io.github.atengk</groupId>
    <artifactId>boot3-web</artifactId>
    <version>1.0.0</version>
    <name>boot3-web</name>
    <description>Spring Boot 3 Web 演示模块</description>
    <url>https://atengk.github.io</url>

    <!-- 源码管理信息 -->
    <scm>
        <url>https://github.com/atengk/Ateng-Java</url>
        <connection>scm:git:https://github.com/atengk/Ateng-Java.git</connection>
        <developerConnection>scm:git:https://github.com/atengk/Ateng-Java.git</developerConnection>
        <tag>HEAD</tag>
    </scm>
    
    <!-- 开发者信息 -->
    <developers>
        <developer>
            <id>atengk</id>
            <name>Ateng</name>
            <email>kongyu2385569970@gmail.com</email>
            <url>https://atengk.github.io</url>
            <roles>
                <role>developer</role>
                <role>maintainer</role>
            </roles>
            <timezone>Asia/Shanghai</timezone>
        </developer>
    </developers>

    <!-- 构建参数与依赖版本统一管理 -->
    <properties>
        <!-- JDK 版本 -->
        <java.version>21</java.version>

        <!-- 编码配置 -->
        <project.build.sourceEncoding>UTF-8</project.build.sourceEncoding>
        <project.reporting.outputEncoding>UTF-8</project.reporting.outputEncoding>

        <!-- 核心依赖版本 -->
        <spring-boot.version>3.5.14</spring-boot.version>
        <maven-compiler.version>3.14.1</maven-compiler.version>
        <lombok.version>1.18.44</lombok.version>
        <hutool.version>5.8.44</hutool.version>
        <fastjson2.version>2.0.61</fastjson2.version>
    </properties>

    <!-- 项目依赖 -->
    <dependencies>
        <!-- Spring Boot Web 场景依赖，提供 MVC、内嵌 Tomcat、参数校验等能力 -->
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-web</artifactId>
        </dependency>

        <!-- Spring Boot 测试依赖，包含 JUnit、Mockito、Spring Test 等 -->
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-test</artifactId>
            <scope>test</scope>
        </dependency>

        <!-- Lombok，用于简化样板代码，编译期生效，不参与运行时打包 -->
        <dependency>
            <groupId>org.projectlombok</groupId>
            <artifactId>lombok</artifactId>
            <version>${lombok.version}</version>
            <scope>provided</scope>
            <optional>true</optional>
        </dependency>

        <!-- Hutool 常用工具集 -->
        <dependency>
            <groupId>cn.hutool</groupId>
            <artifactId>hutool-all</artifactId>
            <version>${hutool.version}</version>
        </dependency>

        <!-- Fastjson2，用于高性能 JSON 序列化与反序列化 -->
        <dependency>
            <groupId>com.alibaba.fastjson2</groupId>
            <artifactId>fastjson2</artifactId>
            <version>${fastjson2.version}</version>
        </dependency>
    </dependencies>

    <!-- 依赖版本管理 -->
    <dependencyManagement>
        <dependencies>
            <!-- 导入 Spring Boot 依赖管理 BOM -->
            <dependency>
                <groupId>org.springframework.boot</groupId>
                <artifactId>spring-boot-dependencies</artifactId>
                <version>${spring-boot.version}</version>
                <type>pom</type>
                <scope>import</scope>
            </dependency>
        </dependencies>
    </dependencyManagement>

    <!-- 远程仓库配置 -->
    <repositories>
        <!-- 阿里云 Maven Central 镜像仓库 -->
        <repository>
            <id>aliyun-central</id>
            <name>Aliyun Maven Central Mirror</name>
            <url>https://maven.aliyun.com/repository/central</url>
        </repository>

        <!-- Maven Central 官方仓库 -->
        <repository>
            <id>maven-central</id>
            <name>Maven Central</name>
            <url>https://repo.maven.apache.org/maven2/</url>
        </repository>
    </repositories>

    <!-- 构建配置 -->
    <build>
        <!-- 最终构件名称 -->
        <finalName>${project.artifactId}-${project.version}</finalName>

        <!-- 资源文件处理 -->
        <resources>
            <!-- 普通资源文件：不启用变量替换，并排除需要过滤的配置文件，避免重复处理 -->
            <resource>
                <directory>src/main/resources</directory>
                <filtering>false</filtering>
                <excludes>
                    <!-- Spring Boot 配置文件 -->
                    <exclude>application*.yml</exclude>
                    <exclude>application*.yaml</exclude>
                    <exclude>application*.properties</exclude>

                    <!-- Bootstrap 配置文件 -->
                    <exclude>bootstrap*.yml</exclude>
                    <exclude>bootstrap*.yaml</exclude>
                    <exclude>bootstrap*.properties</exclude>

                    <!-- 通用配置文件 -->
                    <exclude>common*.yml</exclude>
                    <exclude>common*.yaml</exclude>
                    <exclude>common*.properties</exclude>

                    <!-- Banner 文件 -->
                    <exclude>banner.txt</exclude>
                    <exclude>banner*.txt</exclude>
                </excludes>
            </resource>

            <!-- 需要参与变量替换的资源文件 -->
            <resource>
                <directory>src/main/resources</directory>
                <filtering>true</filtering>
                <includes>
                    <!-- Spring Boot 配置文件 -->
                    <include>application*.yml</include>
                    <include>application*.yaml</include>
                    <include>application*.properties</include>

                    <!-- Bootstrap 配置文件 -->
                    <include>bootstrap*.yml</include>
                    <include>bootstrap*.yaml</include>
                    <include>bootstrap*.properties</include>

                    <!-- 通用配置文件 -->
                    <include>common*.yml</include>
                    <include>common*.yaml</include>
                    <include>common*.properties</include>

                    <!-- Banner 文件 -->
                    <include>banner.txt</include>
                    <include>banner*.txt</include>
                </includes>
            </resource>
        </resources>

        <!-- 构建插件 -->
        <plugins>
            <!-- Maven 编译插件 -->
            <plugin>
                <groupId>org.apache.maven.plugins</groupId>
                <artifactId>maven-compiler-plugin</artifactId>
                <version>${maven-compiler.version}</version>
                <configuration>
                    <!-- 使用 release 统一指定目标 JDK，避免 source/target 组合带来的兼容性歧义 -->
                    <release>${java.version}</release>
                    <encoding>${project.build.sourceEncoding}</encoding>

                    <!-- 保留方法参数名，便于 Spring MVC、反射、参数绑定等场景使用 -->
                    <parameters>true</parameters>

                    <!-- 显式声明注解处理器，提升 IDE 与 Maven 编译表现一致性 -->
                    <annotationProcessorPaths>
                        <path>
                            <groupId>org.projectlombok</groupId>
                            <artifactId>lombok</artifactId>
                            <version>${lombok.version}</version>
                        </path>
                    </annotationProcessorPaths>
                </configuration>
            </plugin>

            <!-- Spring Boot 打包插件 -->
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
                <configuration>
                    <!-- 避免将 Lombok 打入最终可执行 Jar -->
                    <excludes>
                        <exclude>
                            <groupId>org.projectlombok</groupId>
                            <artifactId>lombok</artifactId>
                        </exclude>
                    </excludes>
                </configuration>
            </plugin>
        </plugins>
    </build>

</project>
```

### 配置 application.yml

```yaml
---
# 服务配置
server:
  # 服务端口
  port: 12000

# Spring 配置
spring:
  application:
    # 应用名称
    name: ${project.artifactId}

# 日志配置
logging:
  level:
    # 默认日志级别
    root: INFO
    # 项目包日志级别
    io.github.atengk: DEBUG
---
```

### 配置 SpringBootApplication

```java
package io.github.atengk.web;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public class SpringBoot3WebApplication {

    public static void main(String[] args) {
        SpringApplication.run(SpringBoot3WebApplication.class, args);
    }

}
```



## 使用 Springboot 3

### 创建 Controller

```java
package io.github.atengk.web.controller;

import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

/**
 * Spring Boot 3 Web 演示 Controller
 *
 * <p>主要用于验证：
 * <ul>
 *     <li>Spring Boot 3 是否能正常启动</li>
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

