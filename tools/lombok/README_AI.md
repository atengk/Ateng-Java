# Lombok

Lombok 是 Java 开发中常用的编译期代码生成工具，主要用于减少 Getter、Setter、构造方法、`toString`、`equals`、`hashCode`、Builder 构建器和日志对象等样板代码。在 Spring Boot 3 项目中，Lombok 常用于 DTO、VO、Entity、配置属性对象、Service 构造器注入和日志对象声明等开发场景。

## Lombok 概述

本节介绍 Lombok 在 Spring Boot 3 项目中的功能定位、适用场景和使用注意事项。Lombok 的核心作用是提升编码效率，但它并不替代对象建模、参数校验、业务封装和工程规范。

### 功能定位

Lombok 是一个基于注解处理机制的 Java 编译期增强工具。开发人员在类、字段或构造器上添加 Lombok 注解后，Lombok 会在编译阶段生成对应的 Java 代码，从而减少重复样板代码。

Lombok 在 Spring Boot 3 项目中的常见定位如下：

| 功能方向     | 常用注解                                                     | 说明                                                         |
| ------------ | ------------------------------------------------------------ | ------------------------------------------------------------ |
| 属性访问方法 | `@Getter`、`@Setter`                                         | 为字段生成 Getter 和 Setter 方法                             |
| 字符串输出   | `@ToString`                                                  | 生成 `toString()` 方法，便于调试和日志输出                   |
| 对象比较     | `@EqualsAndHashCode`                                         | 生成 `equals()` 和 `hashCode()` 方法                         |
| 构造方法     | `@NoArgsConstructor`、`@AllArgsConstructor`、`@RequiredArgsConstructor` | 生成无参构造、全参构造和必填参数构造                         |
| 数据对象     | `@Data`                                                      | 组合生成 Getter、Setter、`toString`、`equals`、`hashCode` 等方法 |
| 构建对象     | `@Builder`                                                   | 使用 Builder 模式创建对象                                    |
| 日志对象     | `@Slf4j`                                                     | 自动生成 `log` 日志对象                                      |

Lombok 官方 Maven 配置中通常将 Lombok 设置为 `provided` 依赖，因为它主要在编译阶段生成代码，不需要作为运行时依赖参与应用运行。([Project Lombok](https://projectlombok.org/setup/maven?utm_source=chatgpt.com))

### 适用场景

Lombok 适合用于字段较多、结构清晰、重复样板代码明显的 Java 类。它可以让开发人员把注意力集中在业务字段、接口参数、返回结构和业务逻辑上，而不是重复编写固定格式的方法。

在 Spring Boot 3 项目中，常见适用场景如下：

| 场景               | 推荐注解                                         | 使用说明                                           |
| ------------------ | ------------------------------------------------ | -------------------------------------------------- |
| DTO 请求参数对象   | `@Getter`、`@Setter`、`@ToString`                | 用于接收 Controller 请求参数或 Service 入参        |
| VO 响应对象        | `@Getter`、`@Setter`、`@Builder`                 | 用于封装接口响应数据                               |
| Entity 实体对象    | `@Getter`、`@Setter`                             | 用于数据库实体映射，建议按需生成方法               |
| 配置属性对象       | `@Getter`、`@Setter`、`@ConfigurationProperties` | 用于接收 `application.yml` 中的配置                |
| Service 构造器注入 | `@RequiredArgsConstructor`                       | 配合 `final` 字段实现 Spring 依赖注入              |
| 日志输出           | `@Slf4j`                                         | 在 Controller、Service、定时任务等类中声明日志对象 |
| 对象构建           | `@Builder`                                       | 适合字段较多、构造参数较复杂的对象                 |

对于普通参数对象，可以使用 Lombok 简化代码；对于核心领域对象、持久化实体和存在继承关系的类，建议按需使用具体注解，避免使用过宽的组合注解。

### 使用注意事项

Lombok 会影响 Java 类的编译结果，因此团队项目中需要统一 JDK 版本、Lombok 版本、构建配置和 IDE 注解处理配置。配置不一致时，容易出现命令行编译成功但 IDE 标红，或本地运行正常但流水线编译失败的问题。

使用 Lombok 时建议遵循以下规范：

| 注意事项                          | 说明                                                      |
| --------------------------------- | --------------------------------------------------------- |
| 谨慎使用 `@Data`                  | `@Data` 会一次性生成多个方法，不适合所有类                |
| Entity 不建议滥用 `@Data`         | 实体类可能涉及主键比较、懒加载、循环引用等问题            |
| `@ToString` 避免循环引用          | 双向关联对象可能导致 `toString()` 递归调用                |
| `@EqualsAndHashCode` 注意继承关系 | 子类比较逻辑可能需要显式配置 `callSuper`                  |
| `@Builder` 注意默认值             | 字段默认值需要配合 `@Builder.Default` 才能被 Builder 使用 |
| 构造器注入字段建议使用 `final`    | `@RequiredArgsConstructor` 会为 `final` 字段生成构造方法  |
| IDE 必须开启注解处理              | 否则可能出现 Getter、Setter、构造器无法识别的问题         |
| 关注 JDK 兼容性                   | 新 JDK 版本可能需要升级 Lombok 版本                       |

建议在项目中采用“按需使用”的方式。简单 DTO、VO 可以适当使用 `@Data`；Entity、领域对象、继承类和关键业务对象建议使用 `@Getter`、`@Setter`、`@ToString`、`@EqualsAndHashCode` 等明确注解组合。

## 环境准备

本节说明在 Spring Boot 3 项目中使用 Lombok 前需要准备的基础环境，包括 JDK 版本、Spring Boot 3 项目结构和 IDE 插件配置。环境准备完成后，再进行 Maven、Gradle 和编译插件相关配置会更稳定。

### JDK 版本要求

Spring Boot 3 基于 Spring Framework 6，要求使用 JDK 17 或更高版本。Spring Boot 3.3.x 官方系统要求中明确说明至少需要 Java 17，并对 Maven、Gradle 等构建工具版本也有对应要求。([Spring Enterprise Docs](https://docs.enterprise.spring.io/spring-boot/system-requirements.html?utm_source=chatgpt.com))

推荐版本如下：

| 工具   | 推荐版本       | 说明                                                     |
| ------ | -------------- | -------------------------------------------------------- |
| JDK    | 17 或 21       | Spring Boot 3 最低要求 JDK 17，JDK 21 是常用长期支持版本 |
| Maven  | 3.8.x 或 3.9.x | 满足 Spring Boot 3 项目构建要求                          |
| Gradle | 8.x            | 新项目建议使用 Gradle 8.x                                |
| Lombok | 使用较新稳定版 | 避免因 JDK 版本较新导致注解处理兼容问题                  |

检查本地 Java 版本：

```bash
java -version
javac -version
```

上述命令用于查看 Java 运行环境和 Java 编译器版本。输出结果中应能看到 `17`、`21` 或更高版本。

检查 Maven 当前使用的 Java 版本：

```bash
mvn -version
```

该命令不仅会显示 Maven 版本，还会显示 Maven 当前绑定的 Java 版本。项目构建时应确保 Maven 使用的 JDK 与 IDE 配置的 JDK 保持一致。

检查 Gradle 当前使用的 Java 版本：

```bash
./gradlew -version
```

如果命令行 JDK、IDE JDK 和 CI/CD 流水线 JDK 不一致，可能导致 Lombok 注解在不同环境下表现不一致。因此，团队项目建议统一使用 JDK 17 或 JDK 21，并在开发文档和流水线配置中明确版本。

### Spring Boot 3 项目环境

Spring Boot 3 项目可以使用 Maven 或 Gradle 管理依赖。Lombok 不是 Spring Boot 专属组件，但在 Spring Boot 项目中通常通过构建工具引入，并作为编译期注解处理器参与编译。

推荐项目结构如下：

```text
springboot3-lombok-demo
├── pom.xml
├── src
│   ├── main
│   │   ├── java
│   │   │   └── io
│   │   │       └── github
│   │   │           └── atengk
│   │   │               ├── LombokApplication.java
│   │   │               ├── controller
│   │   │               ├── service
│   │   │               ├── entity
│   │   │               ├── dto
│   │   │               └── vo
│   │   └── resources
│   │       └── application.yml
│   └── test
│       └── java
```

在 Spring Boot 3 项目中使用 Lombok 时，需要重点确认以下内容：

| 检查项           | 要求                                                        |
| ---------------- | ----------------------------------------------------------- |
| 项目 JDK         | 使用 JDK 17 或更高版本                                      |
| Spring Boot 版本 | 使用 Spring Boot 3.x                                        |
| 包名体系         | 使用 `jakarta.*`，避免继续使用旧的 `javax.*`                |
| 构建工具         | Maven 或 Gradle 能够正常刷新依赖                            |
| 注解处理         | IDE 和构建工具均能识别 Lombok 注解处理器                    |
| 编译验证         | 能通过 `mvn clean compile` 或 `./gradlew clean compileJava` |

Maven 项目中，Lombok 通常配置为 `provided` 依赖；Gradle 项目中，Lombok 通常配置为 `compileOnly` 和 `annotationProcessor`。Lombok 官方 Gradle 文档也明确给出了 `compileOnly` 与 `annotationProcessor` 的配置方式。([Project Lombok](https://projectlombok.org/setup/gradle?utm_source=chatgpt.com))

### IDE 插件配置

Lombok 依赖 IDE 对注解处理的支持。即使 Maven 或 Gradle 配置正确，如果 IDE 没有启用注解处理，开发时仍可能出现 Getter、Setter、Builder、构造方法或日志对象无法识别的问题。

IntelliJ IDEA 推荐配置如下：

1. 打开 `Settings`。
2. 进入 `Build, Execution, Deployment`。
3. 进入 `Compiler`。
4. 进入 `Annotation Processors`。
5. 勾选 `Enable annotation processing`。
6. 确认 Project SDK 使用 JDK 17 或更高版本。
7. 重新加载 Maven 或 Gradle 项目。

Maven 项目可以通过以下方式重新加载依赖：

```text
右侧 Maven 面板 -> Reload All Maven Projects
```

Gradle 项目可以通过以下方式重新加载依赖：

```text
右侧 Gradle 面板 -> Reload All Gradle Projects
```

如果 IntelliJ IDEA 仍然存在缓存异常，可以执行：

```text
File -> Invalidate Caches... -> Invalidate and Restart
```

Eclipse 或 Spring Tools Suite 使用 Lombok 时，通常需要将 Lombok 安装到 IDE 中。常见方式是执行本地 Lombok Jar 包，并通过安装器写入 IDE 启动配置。

进入本地 Maven 仓库中的 Lombok 目录后执行安装：

```bash
cd ~/.m2/repository/org/projectlombok/lombok
java -jar lombok-版本号.jar
```

安装完成后，检查 Eclipse 或 STS 的启动配置文件中是否存在类似配置：

```text
-javaagent:/path/to/lombok.jar
```

VS Code 使用 Lombok 时，建议安装 Java 和 Spring Boot 相关扩展，并确保 Java Language Server 能正确识别 Maven 或 Gradle 项目。

推荐扩展如下：

| 扩展                                   | 作用                       |
| -------------------------------------- | -------------------------- |
| Extension Pack for Java                | 提供 Java 项目基础开发能力 |
| Spring Boot Extension Pack             | 提供 Spring Boot 项目支持  |
| Lombok Annotations Support for VS Code | 增强 Lombok 注解识别能力   |

完成 IDE 配置后，可以通过以下命令验证 Lombok 是否正常参与编译：

```bash
mvn clean compile
```

或：

```bash
./gradlew clean compileJava
```

如果命令行编译成功但 IDE 仍然标红，通常是 IDE 缓存、项目 SDK、依赖刷新或注解处理配置问题。优先检查 JDK 版本、注解处理开关和构建工具依赖刷新状态。



## 依赖配置

本节说明 Spring Boot 3 项目中 Lombok 的 Maven、Gradle 和编译插件配置方式。Lombok 属于编译期工具，主要在编译阶段生成代码，通常不需要作为运行时依赖进入最终应用包。

### Maven 依赖配置

Maven 项目中，Lombok 一般配置在 `pom.xml` 的 `dependencies` 节点中。Spring Boot 项目通常使用 `spring-boot-starter-parent` 管理依赖版本，因此 Lombok 可以不显式指定版本；如果项目没有继承 Spring Boot Parent，则建议在 `properties` 中统一声明 Lombok 版本。

文件位置：`pom.xml`

```xml
<dependencies>
    <!-- Spring Boot Web：用于构建 REST API 示例项目 -->
    <dependency>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-starter-web</artifactId>
    </dependency>

    <!-- 参数校验：Spring Boot 3 使用 jakarta.validation 包体系 -->
    <dependency>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-starter-validation</artifactId>
    </dependency>

    <!-- Hutool：常用 Java 工具类库，便于处理字符串、集合、日期等通用逻辑 -->
    <dependency>
        <groupId>cn.hutool</groupId>
        <artifactId>hutool-all</artifactId>
        <version>5.8.35</version>
    </dependency>

    <!-- Lombok：编译期生成 Getter、Setter、构造器、Builder、日志对象等代码 -->
    <dependency>
        <groupId>org.projectlombok</groupId>
        <artifactId>lombok</artifactId>
        <scope>provided</scope>
    </dependency>

    <!-- Spring Boot Test：用于单元测试和编译验证 -->
    <dependency>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-starter-test</artifactId>
        <scope>test</scope>
    </dependency>
</dependencies>
```

如果项目没有继承 `spring-boot-starter-parent`，建议显式声明 Lombok 版本，避免不同模块之间版本不一致。

文件位置：`pom.xml`

```xml
<properties>
    <!-- JDK 版本：Spring Boot 3 最低要求 JDK 17 -->
    <java.version>17</java.version>

    <!-- Lombok 版本：团队项目建议统一管理 -->
    <lombok.version>1.18.36</lombok.version>

    <!-- Hutool 版本：统一工具类库版本 -->
    <hutool.version>5.8.35</hutool.version>
</properties>

<dependencies>
    <!-- Lombok：编译期依赖，不进入运行时包 -->
    <dependency>
        <groupId>org.projectlombok</groupId>
        <artifactId>lombok</artifactId>
        <version>${lombok.version}</version>
        <scope>provided</scope>
    </dependency>

    <!-- Hutool：常用工具类库 -->
    <dependency>
        <groupId>cn.hutool</groupId>
        <artifactId>hutool-all</artifactId>
        <version>${hutool.version}</version>
    </dependency>
</dependencies>
```

`provided` 表示 Lombok 只在编译阶段使用，不会作为运行时依赖打入最终应用包。对于 Spring Boot 后端项目，推荐保持该配置，避免无意义地增加运行时依赖。

### Gradle 依赖配置

Gradle 项目中，Lombok 通常需要同时配置 `compileOnly` 和 `annotationProcessor`。前者表示 Lombok 只参与编译期依赖解析，后者表示 Lombok 作为注解处理器参与代码生成。

文件位置：`build.gradle`

```gradle
plugins {
    id 'java'
    id 'org.springframework.boot' version '3.3.6'
    id 'io.spring.dependency-management' version '1.1.6'
}

group = 'io.github.atengk'
version = '1.0.0'

java {
    // Spring Boot 3 项目建议使用 JDK 17 或 JDK 21
    sourceCompatibility = JavaVersion.VERSION_17
    targetCompatibility = JavaVersion.VERSION_17
}

repositories {
    mavenCentral()
}

dependencies {
    // Spring Boot Web：用于构建 REST API 示例项目
    implementation 'org.springframework.boot:spring-boot-starter-web'

    // 参数校验：Spring Boot 3 使用 jakarta.validation 包体系
    implementation 'org.springframework.boot:spring-boot-starter-validation'

    // Hutool：常用 Java 工具类库
    implementation 'cn.hutool:hutool-all:5.8.35'

    // Lombok：仅编译期可见，不进入运行时 classpath
    compileOnly 'org.projectlombok:lombok'

    // Lombok 注解处理器：编译阶段生成对应代码
    annotationProcessor 'org.projectlombok:lombok'

    // 测试代码如需使用 Lombok，也需要单独配置
    testCompileOnly 'org.projectlombok:lombok'
    testAnnotationProcessor 'org.projectlombok:lombok'

    // Spring Boot Test：用于单元测试和编译验证
    testImplementation 'org.springframework.boot:spring-boot-starter-test'
}

tasks.named('test') {
    useJUnitPlatform()
}
```

如果使用 Kotlin DSL，配置方式如下。

文件位置：`build.gradle.kts`

```kotlin
plugins {
    java
    id("org.springframework.boot") version "3.3.6"
    id("io.spring.dependency-management") version "1.1.6"
}

group = "io.github.atengk"
version = "1.0.0"

java {
    // Spring Boot 3 项目建议使用 JDK 17 或 JDK 21
    sourceCompatibility = JavaVersion.VERSION_17
    targetCompatibility = JavaVersion.VERSION_17
}

repositories {
    mavenCentral()
}

dependencies {
    // Spring Boot Web：用于构建 REST API 示例项目
    implementation("org.springframework.boot:spring-boot-starter-web")

    // 参数校验：Spring Boot 3 使用 jakarta.validation 包体系
    implementation("org.springframework.boot:spring-boot-starter-validation")

    // Hutool：常用 Java 工具类库
    implementation("cn.hutool:hutool-all:5.8.35")

    // Lombok：仅编译期可见
    compileOnly("org.projectlombok:lombok")

    // Lombok 注解处理器：编译阶段生成代码
    annotationProcessor("org.projectlombok:lombok")

    // 测试代码中使用 Lombok 时需要配置
    testCompileOnly("org.projectlombok:lombok")
    testAnnotationProcessor("org.projectlombok:lombok")

    // Spring Boot Test：用于单元测试和编译验证
    testImplementation("org.springframework.boot:spring-boot-starter-test")
}

tasks.withType<Test> {
    useJUnitPlatform()
}
```

Gradle 项目如果只配置 `compileOnly` 而没有配置 `annotationProcessor`，可能导致代码中可以引用 Lombok 注解，但编译阶段无法生成对应方法。

### 编译插件配置

编译插件用于明确告诉构建工具在编译阶段启用 Lombok 注解处理器。对于简单 Spring Boot 项目，只配置 Lombok 依赖通常已经可以正常工作；对于多模块项目、公司脚手架项目或 CI/CD 构建环境，建议显式配置编译插件，减少环境差异导致的问题。

Maven 项目可以显式配置 `maven-compiler-plugin`，指定 Lombok 作为注解处理器。

文件位置：`pom.xml`

```xml
<build>
    <plugins>
        <!-- Maven 编译插件：指定 JDK 版本和 Lombok 注解处理器 -->
        <plugin>
            <groupId>org.apache.maven.plugins</groupId>
            <artifactId>maven-compiler-plugin</artifactId>
            <configuration>
                <!-- Spring Boot 3 最低要求 JDK 17 -->
                <source>${java.version}</source>
                <target>${java.version}</target>

                <!-- 显式声明注解处理器，避免 CI/CD 或多模块项目中处理器识别异常 -->
                <annotationProcessorPaths>
                    <path>
                        <groupId>org.projectlombok</groupId>
                        <artifactId>lombok</artifactId>
                        <version>${lombok.version}</version>
                    </path>
                </annotationProcessorPaths>
            </configuration>
        </plugin>

        <!-- Spring Boot 打包插件：排除 Lombok，避免进入最终运行包 -->
        <plugin>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-maven-plugin</artifactId>
            <configuration>
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
```

如果项目继承了 `spring-boot-starter-parent`，`maven-compiler-plugin` 通常可以不显式配置版本；如果没有继承 Spring Boot Parent，建议统一管理插件版本。

完成配置后，可以使用以下命令验证 Lombok 是否正常参与编译。

```bash
# 清理旧编译产物并重新编译项目
mvn clean compile
```

该命令会触发 Maven 编译流程。如果 Lombok 配置正常，使用 `@Getter`、`@Setter`、`@Builder`、`@RequiredArgsConstructor` 等注解的类应能正常编译通过。

Gradle 项目可以通过以下命令验证。

```bash
# 清理旧编译产物并重新编译 Java 代码
./gradlew clean compileJava
```

如果编译失败，应优先检查以下内容：JDK 版本是否为 17 或以上、Lombok 依赖是否配置完整、IDE 是否开启注解处理、Maven 或 Gradle 是否刷新依赖、CI/CD 使用的 JDK 是否与本地一致。

## 常用注解

本节介绍 Lombok 在 Spring Boot 3 项目中最常用的注解，并给出适合后端开发场景的代码示例。实际开发中应根据类的职责选择注解，不建议为了省事而在所有类上统一使用 `@Data`。

### Getter 与 Setter

`@Getter` 和 `@Setter` 用于为字段生成 Getter 和 Setter 方法。它们是 Lombok 中最基础、最安全、最常用的注解，适合用于 DTO、VO、Entity 和配置属性对象。

文件位置：`src/main/java/io/github/atengk/lombok/dto/UserCreateDTO.java`

下面的代码定义用户创建请求参数对象，通过 Lombok 生成 Getter 和 Setter 方法。

```java
package io.github.atengk.lombok.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

/**
 * 用户创建请求参数
 *
 * @author Ateng
 * @since 2026-05-06
 */
@Getter
@Setter
@ToString
public class UserCreateDTO {

    /**
     * 用户名称
     */
    @NotBlank(message = "用户名称不能为空")
    private String username;

    /**
     * 用户昵称
     */
    @NotBlank(message = "用户昵称不能为空")
    private String nickname;

    /**
     * 用户年龄
     */
    @NotNull(message = "用户年龄不能为空")
    private Integer age;
}
```

使用 `@Getter` 和 `@Setter` 后，业务代码可以直接调用 `getUsername()`、`setUsername()`、`getAge()`、`setAge()` 等方法。源代码中虽然看不到这些方法，但编译后的字节码中会存在对应方法。

适用建议如下：

| 场景         | 建议                             |
| ------------ | -------------------------------- |
| DTO          | 推荐使用                         |
| VO           | 推荐使用                         |
| Entity       | 推荐使用                         |
| 配置属性类   | 推荐使用                         |
| 核心领域对象 | 按需使用，避免暴露不该修改的字段 |

### ToString 与 EqualsAndHashCode

`@ToString` 用于生成 `toString()` 方法，便于日志输出和调试。`@EqualsAndHashCode` 用于生成 `equals()` 和 `hashCode()` 方法，便于对象比较、集合去重和哈希结构存储。

文件位置：`src/main/java/io/github/atengk/lombok/entity/UserEntity.java`

下面的代码定义用户实体对象，并通过指定字段控制对象输出和对象比较逻辑。

```java
package io.github.atengk.lombok.entity;

import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

import java.time.LocalDateTime;

/**
 * 用户实体对象
 *
 * @author Ateng
 * @since 2026-05-06
 */
@Getter
@Setter
@ToString
@EqualsAndHashCode(of = "id")
public class UserEntity {

    /**
     * 用户ID
     */
    private Long id;

    /**
     * 用户名称
     */
    private String username;

    /**
     * 用户昵称
     */
    private String nickname;

    /**
     * 创建时间
     */
    private LocalDateTime createTime;
}
```

`@EqualsAndHashCode(of = "id")` 表示只使用 `id` 字段参与 `equals()` 和 `hashCode()` 计算。对于数据库实体对象，这种方式比直接使用全部字段更稳定。

如果对象中存在敏感字段，可以通过 `@ToString.Exclude` 排除。

文件位置：`src/main/java/io/github/atengk/lombok/dto/LoginDTO.java`

下面的代码在输出登录参数时排除密码字段，避免敏感信息进入日志。

```java
package io.github.atengk.lombok.dto;

import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

/**
 * 登录请求参数
 *
 * @author Ateng
 * @since 2026-05-06
 */
@Getter
@Setter
@ToString
public class LoginDTO {

    /**
     * 登录账号
     */
    private String username;

    /**
     * 登录密码
     */
    @ToString.Exclude
    private String password;
}
```

使用建议如下：

| 注解                 | 建议                                        |
| -------------------- | ------------------------------------------- |
| `@ToString`          | DTO、VO、普通配置对象可以使用               |
| `@ToString.Exclude`  | 密码、Token、密钥、大字段、关联对象建议排除 |
| `@EqualsAndHashCode` | Entity 建议明确指定参与比较的字段           |
| `callSuper`          | 存在继承关系时需要明确是否包含父类字段      |

### NoArgsConstructor 与 AllArgsConstructor

`@NoArgsConstructor` 用于生成无参构造方法，`@AllArgsConstructor` 用于生成包含所有字段的全参构造方法。它们常用于实体类、参数对象、测试对象和需要框架反射创建实例的场景。

文件位置：`src/main/java/io/github/atengk/lombok/vo/UserVO.java`

下面的代码定义用户响应对象，同时提供无参构造和全参构造。

```java
package io.github.atengk.lombok.vo;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

/**
 * 用户响应对象
 *
 * @author Ateng
 * @since 2026-05-06
 */
@Getter
@Setter
@ToString
@NoArgsConstructor
@AllArgsConstructor
public class UserVO {

    /**
     * 用户ID
     */
    private Long id;

    /**
     * 用户名称
     */
    private String username;

    /**
     * 用户昵称
     */
    private String nickname;
}
```

使用示例如下。

```java
UserVO emptyUser = new UserVO();

UserVO user = new UserVO(1L, "ateng", "阿腾");
```

对于 JPA Entity、JSON 反序列化对象或某些框架需要反射创建实例的类，`@NoArgsConstructor` 较常见。对于测试代码、简单响应对象或手动构造对象的场景，`@AllArgsConstructor` 可以减少构造方法编写。

使用建议如下：

| 注解                       | 适用场景                           |
| -------------------------- | ---------------------------------- |
| `@NoArgsConstructor`       | Entity、反序列化对象、配置对象     |
| `@AllArgsConstructor`      | VO、测试对象、简单数据对象         |
| `@RequiredArgsConstructor` | Service 构造器注入、不可变依赖对象 |

### RequiredArgsConstructor

`@RequiredArgsConstructor` 会为 `final` 字段和标记了 `@NonNull` 的字段生成构造方法。在 Spring Boot 3 中，它最常用于 Service、Controller、Component 等类的构造器注入。

相比字段注入，构造器注入更利于单元测试，也能让依赖关系更明确。配合 `final` 字段使用时，可以保证依赖对象在实例创建后不可被重新赋值。

文件位置：`src/main/java/io/github/atengk/lombok/service/UserService.java`

下面的代码定义用户服务接口。

```java
package io.github.atengk.lombok.service;

import io.github.atengk.lombok.dto.UserCreateDTO;
import io.github.atengk.lombok.vo.UserVO;

/**
 * 用户服务接口
 *
 * @author Ateng
 * @since 2026-05-06
 */
public interface UserService {

    /**
     * 创建用户
     *
     * @param dto 用户创建参数
     * @return 用户响应对象
     */
    UserVO createUser(UserCreateDTO dto);
}
```

文件位置：`src/main/java/io/github/atengk/lombok/service/impl/UserServiceImpl.java`

下面的代码使用 `@RequiredArgsConstructor` 完成构造器注入，并通过 Hutool 处理字符串校验逻辑。

```java
package io.github.atengk.lombok.service.impl;

import cn.hutool.core.util.StrUtil;
import io.github.atengk.lombok.dto.UserCreateDTO;
import io.github.atengk.lombok.service.UserService;
import io.github.atengk.lombok.vo.UserVO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

/**
 * 用户服务实现类
 *
 * @author Ateng
 * @since 2026-05-06
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class UserServiceImpl implements UserService {

    /**
     * 示例依赖对象，实际项目中可以替换为 Mapper、Repository 或远程服务客户端
     */
    private final UserValidateService userValidateService;

    /**
     * 创建用户
     *
     * @param dto 用户创建参数
     * @return 用户响应对象
     */
    @Override
    public UserVO createUser(UserCreateDTO dto) {
        if (StrUtil.isBlank(dto.getUsername())) {
            log.warn("创建用户失败，用户名称为空");
            throw new IllegalArgumentException("用户名称不能为空");
        }

        userValidateService.checkUsername(dto.getUsername());

        log.info("创建用户成功，username={}", dto.getUsername());
        return new UserVO(1L, dto.getUsername(), dto.getNickname());
    }
}
```

文件位置：`src/main/java/io/github/atengk/lombok/service/impl/UserValidateService.java`

下面的代码定义用户校验服务，供构造器注入示例使用。

```java
package io.github.atengk.lombok.service.impl;

import cn.hutool.core.util.StrUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

/**
 * 用户校验服务
 *
 * @author Ateng
 * @since 2026-05-06
 */
@Slf4j
@Service
public class UserValidateService {

    /**
     * 校验用户名称
     *
     * @param username 用户名称
     */
    public void checkUsername(String username) {
        if (StrUtil.equalsIgnoreCase(username, "admin")) {
            log.warn("用户名称校验失败，username={}", username);
            throw new IllegalArgumentException("admin 为系统保留名称");
        }
    }
}
```

`@RequiredArgsConstructor` 推荐用于 Spring Bean 依赖注入，不建议和字段上的 `@Autowired` 混合使用。一个类中保持统一注入风格更容易维护。

### Data

`@Data` 是 Lombok 的组合注解，等价于同时使用 `@Getter`、`@Setter`、`@ToString`、`@EqualsAndHashCode` 和 `@RequiredArgsConstructor` 的主要能力。它适合简单数据载体，但不适合所有对象。

文件位置：`src/main/java/io/github/atengk/lombok/dto/UserQueryDTO.java`

下面的代码定义用户分页查询参数对象，字段结构简单，可以使用 `@Data`。

```java
package io.github.atengk.lombok.dto;

import lombok.Data;

/**
 * 用户查询参数
 *
 * @author Ateng
 * @since 2026-05-06
 */
@Data
public class UserQueryDTO {

    /**
     * 用户名称
     */
    private String username;

    /**
     * 当前页码
     */
    private Integer pageNum = 1;

    /**
     * 每页条数
     */
    private Integer pageSize = 10;
}
```

`@Data` 使用方便，但在以下场景不建议直接使用：

| 场景               | 原因                                    | 建议                                                     |
| ------------------ | --------------------------------------- | -------------------------------------------------------- |
| Entity 实体类      | `equals`、`hashCode` 可能受全部字段影响 | 使用 `@Getter`、`@Setter`，按需配置 `@EqualsAndHashCode` |
| 存在双向关联的对象 | `toString` 可能产生递归调用             | 使用 `@ToString.Exclude` 或避免生成                      |
| 包含敏感字段的对象 | `toString` 可能输出密码、Token 等信息   | 使用 `@ToString.Exclude` 或不用 `@Data`                  |
| 核心领域对象       | Setter 过多会破坏封装                   | 按业务语义提供方法                                       |

简单 DTO、查询参数对象和内部临时数据对象可以使用 `@Data`。实体对象、领域对象和安全敏感对象建议使用更明确的注解组合。

### Builder

`@Builder` 用于生成 Builder 构建器，适合字段较多、构造参数较复杂、希望提升对象创建可读性的场景。它常用于 VO、DTO 转换结果、消息对象和第三方接口请求对象。

文件位置：`src/main/java/io/github/atengk/lombok/vo/UserDetailVO.java`

下面的代码定义用户详情响应对象，并使用 `@Builder` 构建实例。

```java
package io.github.atengk.lombok.vo;

import lombok.Builder;
import lombok.Getter;
import lombok.ToString;

import java.time.LocalDateTime;

/**
 * 用户详情响应对象
 *
 * @author Ateng
 * @since 2026-05-06
 */
@Getter
@ToString
@Builder
public class UserDetailVO {

    /**
     * 用户ID
     */
    private Long id;

    /**
     * 用户名称
     */
    private String username;

    /**
     * 用户昵称
     */
    private String nickname;

    /**
     * 用户状态
     */
    @Builder.Default
    private String status = "ENABLE";

    /**
     * 创建时间
     */
    private LocalDateTime createTime;
}
```

使用方式如下。

```java
UserDetailVO userDetail = UserDetailVO.builder()
        .id(1L)
        .username("ateng")
        .nickname("阿腾")
        .createTime(LocalDateTime.now())
        .build();
```

如果字段有默认值，并且希望通过 Builder 创建对象时仍然保留默认值，需要使用 `@Builder.Default`。否则，Builder 构建对象时未设置该字段会得到类型默认值，例如对象类型为 `null`。

在业务转换中，`@Builder` 可以提升响应对象组装的可读性。

文件位置：`src/main/java/io/github/atengk/lombok/converter/UserConverter.java`

下面的代码使用 Builder 将实体对象转换为响应对象。

```java
package io.github.atengk.lombok.converter;

import cn.hutool.core.util.StrUtil;
import io.github.atengk.lombok.entity.UserEntity;
import io.github.atengk.lombok.vo.UserDetailVO;

/**
 * 用户对象转换器
 *
 * @author Ateng
 * @since 2026-05-06
 */
public class UserConverter {

    /**
     * 转换为用户详情响应对象
     *
     * @param entity 用户实体对象
     * @return 用户详情响应对象
     */
    public static UserDetailVO toDetailVO(UserEntity entity) {
        String nickname = StrUtil.blankToDefault(entity.getNickname(), entity.getUsername());

        return UserDetailVO.builder()
                .id(entity.getId())
                .username(entity.getUsername())
                .nickname(nickname)
                .createTime(entity.getCreateTime())
                .build();
    }
}
```

`@Builder` 不建议在所有对象上默认使用。对于字段较少的对象，普通构造方法或 Setter 更直接；对于字段较多、构建过程存在可选参数的对象，Builder 更清晰。

### Slf4j

`@Slf4j` 用于为类自动生成 SLF4J 日志对象，相当于声明 `private static final Logger log = LoggerFactory.getLogger(当前类.class)`。在 Spring Boot 3 项目中，`@Slf4j` 常用于 Controller、Service、定时任务、监听器和外部接口调用类。

文件位置：`src/main/java/io/github/atengk/lombok/controller/UserController.java`

下面的代码在 Controller 中使用 `@Slf4j` 输出接口访问日志。

```java
package io.github.atengk.lombok.controller;

import io.github.atengk.lombok.dto.UserCreateDTO;
import io.github.atengk.lombok.service.UserService;
import io.github.atengk.lombok.vo.UserVO;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

/**
 * 用户接口控制器
 *
 * @author Ateng
 * @since 2026-05-06
 */
@Slf4j
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/users")
public class UserController {

    private final UserService userService;

    /**
     * 创建用户
     *
     * @param dto 用户创建参数
     * @return 用户响应对象
     */
    @PostMapping
    public UserVO createUser(@Valid @RequestBody UserCreateDTO dto) {
        log.info("接收到创建用户请求，username={}", dto.getUsername());
        return userService.createUser(dto);
    }
}
```

文件位置：`src/main/java/io/github/atengk/lombok/task/UserStatisticsTask.java`

下面的代码在定时任务中使用 `@Slf4j` 输出任务执行日志。

```java
package io.github.atengk.lombok.task;

import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * 用户统计任务
 *
 * @author Ateng
 * @since 2026-05-06
 */
@Slf4j
@Component
public class UserStatisticsTask {

    /**
     * 统计用户数据
     */
    @Scheduled(cron = "0 0/10 * * * ?")
    public void statisticsUser() {
        log.info("开始统计用户数据");

        try {
            // 这里编写用户统计业务逻辑
            log.info("用户数据统计完成");
        } catch (Exception e) {
            log.error("用户数据统计失败", e);
        }
    }
}
```

日志输出建议遵循以下原则：

| 场景           | 日志级别 | 说明                                      |
| -------------- | -------- | ----------------------------------------- |
| 正常业务流程   | `info`   | 记录关键业务动作，不记录过多细节          |
| 参数不符合预期 | `warn`   | 记录异常分支或可恢复问题                  |
| 系统异常       | `error`  | 记录异常堆栈，便于排查问题                |
| 调试细节       | `debug`  | 仅用于开发或排查问题                      |
| 敏感信息       | 不输出   | 密码、Token、密钥、身份证号等不得直接输出 |

接口验证示例：

```bash
# 调用创建用户接口，验证 @Slf4j、@RequiredArgsConstructor、@Getter、@Setter 等注解是否正常工作
curl -X POST "http://localhost:8080/api/users" \
  -H "Content-Type: application/json" \
  -d '{
    "username": "ateng",
    "nickname": "阿腾",
    "age": 18
  }'
```

如果接口可以正常返回数据，并且控制台中输出了对应日志，说明 Lombok 的日志对象、Getter、Setter 和构造器注入都已经正常生效。



## 实体类开发

实体类开发主要包括 DTO、VO、Entity 和配置属性对象。Lombok 在这些对象中的作用是减少 Getter、Setter、构造方法、Builder 和 `toString` 等样板代码，让类结构更聚焦于字段定义和业务语义。

### DTO 对象

DTO 通常用于接收接口请求参数、Service 层入参或模块之间的数据传递。在 Spring Boot 3 中，请求参数对象一般会配合 `jakarta.validation` 做参数校验，并使用 Lombok 生成基础访问方法。

文件位置：`src/main/java/io/github/atengk/lombok/dto/UserCreateDTO.java`

下面的代码定义用户创建请求参数对象，适合用于 Controller 的 `@RequestBody` 参数接收。

```java
package io.github.atengk.lombok.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

/**
 * 用户创建请求参数
 *
 * @author Ateng
 * @since 2026-05-06
 */
@Getter
@Setter
@ToString
public class UserCreateDTO {

    /**
     * 用户名称
     */
    @NotBlank(message = "用户名称不能为空")
    private String username;

    /**
     * 用户昵称
     */
    @NotBlank(message = "用户昵称不能为空")
    private String nickname;

    /**
     * 用户年龄
     */
    @NotNull(message = "用户年龄不能为空")
    @Min(value = 1, message = "用户年龄不能小于1")
    @Max(value = 120, message = "用户年龄不能大于120")
    private Integer age;

    /**
     * 手机号码
     */
    @NotBlank(message = "手机号码不能为空")
    private String mobile;
}
```

文件位置：`src/main/java/io/github/atengk/lombok/dto/UserQueryDTO.java`

下面的代码定义用户查询请求参数对象，适合用于分页查询、条件查询等场景。

```java
package io.github.atengk.lombok.dto;

import lombok.Data;

/**
 * 用户查询请求参数
 *
 * @author Ateng
 * @since 2026-05-06
 */
@Data
public class UserQueryDTO {

    /**
     * 用户名称
     */
    private String username;

    /**
     * 手机号码
     */
    private String mobile;

    /**
     * 当前页码
     */
    private Integer pageNum = 1;

    /**
     * 每页条数
     */
    private Integer pageSize = 10;
}
```

DTO 使用建议如下：

| 建议                                                      | 说明                               |
| --------------------------------------------------------- | ---------------------------------- |
| 请求参数 DTO 推荐配合 `jakarta.validation`                | 保证参数进入业务层前完成基础校验   |
| 简单查询 DTO 可以使用 `@Data`                             | 字段简单、无敏感信息时可以简化代码 |
| 创建、修改 DTO 建议使用 `@Getter`、`@Setter`、`@ToString` | 注解职责更明确，便于控制输出内容   |
| 密码、Token 等敏感字段不要直接输出                        | 可以使用 `@ToString.Exclude` 排除  |

### VO 对象

VO 通常用于接口响应对象，负责把后端数据转换成前端需要的展示结构。VO 不建议直接复用 Entity，避免数据库字段、内部状态或敏感字段暴露到接口响应中。

文件位置：`src/main/java/io/github/atengk/lombok/vo/UserVO.java`

下面的代码定义用户基础响应对象，适合用于列表接口、详情接口中的用户基础信息返回。

```java
package io.github.atengk.lombok.vo;

import lombok.Builder;
import lombok.Getter;
import lombok.ToString;

import java.time.LocalDateTime;

/**
 * 用户响应对象
 *
 * @author Ateng
 * @since 2026-05-06
 */
@Getter
@ToString
@Builder
public class UserVO {

    /**
     * 用户ID
     */
    private Long id;

    /**
     * 用户名称
     */
    private String username;

    /**
     * 用户昵称
     */
    private String nickname;

    /**
     * 手机号码
     */
    private String mobile;

    /**
     * 用户状态
     */
    @Builder.Default
    private String status = "ENABLE";

    /**
     * 创建时间
     */
    private LocalDateTime createTime;
}
```

文件位置：`src/main/java/io/github/atengk/lombok/vo/UserStatisticsVO.java`

下面的代码定义用户统计响应对象，适合用于统计接口或后台首页概览接口。

```java
package io.github.atengk.lombok.vo;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

/**
 * 用户统计响应对象
 *
 * @author Ateng
 * @since 2026-05-06
 */
@Getter
@Setter
@ToString
@NoArgsConstructor
@AllArgsConstructor
public class UserStatisticsVO {

    /**
     * 用户总数
     */
    private Long totalCount;

    /**
     * 启用用户数
     */
    private Long enableCount;

    /**
     * 禁用用户数
     */
    private Long disableCount;
}
```

VO 使用建议如下：

| 建议                          | 说明                                 |
| ----------------------------- | ------------------------------------ |
| 不直接返回 Entity             | 避免暴露数据库字段和内部字段         |
| 字段较多时推荐使用 `@Builder` | 提升对象构建可读性                   |
| 响应对象可减少 Setter         | 对外响应对象通常创建后不需要频繁修改 |
| 默认值配合 `@Builder.Default` | 避免 Builder 构建时默认值丢失        |

### Entity 对象

Entity 通常表示数据库实体对象，用于持久层映射。Entity 可以使用 Lombok 生成 Getter 和 Setter，但需要谨慎使用 `@Data`，避免自动生成的 `equals`、`hashCode` 和 `toString` 引发持久化对象比较、循环引用或敏感字段输出问题。

文件位置：`src/main/java/io/github/atengk/lombok/entity/UserEntity.java`

下面的代码定义用户实体对象，示例使用 Lombok 生成基础方法，并显式控制对象比较字段。

```java
package io.github.atengk.lombok.entity;

import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

import java.time.LocalDateTime;

/**
 * 用户实体对象
 *
 * @author Ateng
 * @since 2026-05-06
 */
@Getter
@Setter
@ToString
@EqualsAndHashCode(of = "id")
public class UserEntity {

    /**
     * 用户ID
     */
    private Long id;

    /**
     * 用户名称
     */
    private String username;

    /**
     * 用户昵称
     */
    private String nickname;

    /**
     * 手机号码
     */
    private String mobile;

    /**
     * 用户状态：ENABLE 启用，DISABLE 禁用
     */
    private String status;

    /**
     * 创建时间
     */
    private LocalDateTime createTime;

    /**
     * 更新时间
     */
    private LocalDateTime updateTime;
}
```

如果项目已经集成 MyBatis-Plus，可以在 Entity 中增加表映射注解。

文件位置：`src/main/java/io/github/atengk/lombok/entity/UserEntity.java`

下面的代码展示 MyBatis-Plus 场景下的用户实体对象写法。

```java
package io.github.atengk.lombok.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

import java.time.LocalDateTime;

/**
 * 用户实体对象
 *
 * @author Ateng
 * @since 2026-05-06
 */
@Getter
@Setter
@ToString
@EqualsAndHashCode(of = "id")
@TableName("sys_user")
public class UserEntity {

    /**
     * 用户ID
     */
    @TableId(type = IdType.AUTO)
    private Long id;

    /**
     * 用户名称
     */
    private String username;

    /**
     * 用户昵称
     */
    private String nickname;

    /**
     * 手机号码
     */
    private String mobile;

    /**
     * 用户状态：ENABLE 启用，DISABLE 禁用
     */
    private String status;

    /**
     * 创建时间
     */
    private LocalDateTime createTime;

    /**
     * 更新时间
     */
    private LocalDateTime updateTime;
}
```

Entity 使用建议如下：

| 建议                                   | 说明                                                |
| -------------------------------------- | --------------------------------------------------- |
| 推荐使用 `@Getter`、`@Setter`          | 满足 ORM 框架字段访问需求                           |
| 谨慎使用 `@Data`                       | 避免自动生成过宽的 `equals`、`hashCode`、`toString` |
| `@EqualsAndHashCode` 建议指定主键字段  | 实体比较逻辑更稳定                                  |
| `@ToString` 避免输出敏感字段和关联对象 | 防止日志泄漏或递归调用                              |
| 不建议把 Entity 直接返回给前端         | 应转换为 VO 后返回                                  |

### 配置属性对象

配置属性对象用于接收 `application.yml` 或 `application.properties` 中的业务配置。Spring Boot 3 中通常使用 `@ConfigurationProperties` 绑定配置项，并使用 Lombok 生成 Getter 和 Setter。

文件位置：`src/main/resources/application.yml`

下面的配置定义用户模块相关参数，用于演示配置属性绑定。

```yaml
# 用户模块配置
app:
  user:
    # 是否启用用户注册
    register-enabled: true
    # 默认用户状态
    default-status: ENABLE
    # 默认分页大小
    default-page-size: 10
    # 手机号脱敏开关
    mobile-mask-enabled: true
```

文件位置：`src/main/java/io/github/atengk/lombok/config/UserProperties.java`

下面的代码定义用户模块配置属性对象，用于绑定 `app.user` 下的配置项。

```java
package io.github.atengk.lombok.config;

import lombok.Getter;
import lombok.Setter;
import lombok.ToString;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * 用户模块配置属性
 *
 * @author Ateng
 * @since 2026-05-06
 */
@Getter
@Setter
@ToString
@Component
@ConfigurationProperties(prefix = "app.user")
public class UserProperties {

    /**
     * 是否启用用户注册
     */
    private Boolean registerEnabled = Boolean.TRUE;

    /**
     * 默认用户状态
     */
    private String defaultStatus = "ENABLE";

    /**
     * 默认分页大小
     */
    private Integer defaultPageSize = 10;

    /**
     * 是否启用手机号脱敏
     */
    private Boolean mobileMaskEnabled = Boolean.TRUE;
}
```

配置属性对象使用建议如下：

| 建议                                         | 说明                                  |
| -------------------------------------------- | ------------------------------------- |
| 使用 `@ConfigurationProperties` 承载业务配置 | 比零散使用 `@Value` 更容易维护        |
| 配置对象建议提供默认值                       | 避免配置缺失导致空指针问题            |
| 使用 `@Getter`、`@Setter`                    | 便于 Spring Boot 绑定配置             |
| 谨慎使用 `@ToString`                         | 配置中包含密钥、Token、密码时不要输出 |

## 业务代码开发

业务代码中使用 Lombok 的重点不是减少几行代码，而是让依赖注入、日志对象、响应对象构建和链式配置更清晰。本节通过常见业务类示例说明 Lombok 在 Spring Boot 3 项目中的实际用法。

### 日志对象使用

业务类中可以使用 `@Slf4j` 自动生成日志对象，避免手动声明 `LoggerFactory.getLogger(...)`。日志建议只记录关键业务动作、异常分支和系统错误，不要输出密码、Token、身份证号等敏感信息。

文件位置：`src/main/java/io/github/atengk/lombok/service/UserLogService.java`

下面的代码展示 Service 中使用 `@Slf4j` 记录业务日志和异常日志。

```java
package io.github.atengk.lombok.service;

import cn.hutool.core.util.DesensitizedUtil;
import cn.hutool.core.util.StrUtil;
import io.github.atengk.lombok.dto.UserCreateDTO;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

/**
 * 用户日志服务
 *
 * @author Ateng
 * @since 2026-05-06
 */
@Slf4j
@Service
public class UserLogService {

    /**
     * 记录用户创建日志
     *
     * @param dto 用户创建参数
     */
    public void recordCreateLog(UserCreateDTO dto) {
        if (dto == null) {
            log.warn("记录用户创建日志失败，请求参数为空");
            return;
        }

        String mobile = StrUtil.isBlank(dto.getMobile())
                ? "未填写"
                : DesensitizedUtil.mobilePhone(dto.getMobile());

        log.info("创建用户请求，username={}，mobile={}", dto.getUsername(), mobile);
    }

    /**
     * 记录用户创建异常日志
     *
     * @param username 用户名称
     * @param e        异常信息
     */
    public void recordCreateError(String username, Exception e) {
        log.error("创建用户异常，username={}", username, e);
    }
}
```

日志使用建议如下：

| 场景               | 推荐级别 | 示例                               |
| ------------------ | -------- | ---------------------------------- |
| 正常业务动作       | `info`   | 用户创建成功、订单提交成功         |
| 参数异常或业务拦截 | `warn`   | 参数为空、重复提交、状态不允许     |
| 系统异常           | `error`  | 数据库异常、远程调用异常           |
| 调试细节           | `debug`  | 查询条件、中间变量，仅开发排查使用 |

### 构造器注入

Spring Boot 3 项目中推荐使用构造器注入。配合 Lombok 的 `@RequiredArgsConstructor`，可以减少构造方法代码，同时保留依赖不可变的设计风格。

文件位置：`src/main/java/io/github/atengk/lombok/service/UserService.java`

下面的代码定义用户业务接口。

```java
package io.github.atengk.lombok.service;

import io.github.atengk.lombok.dto.UserCreateDTO;
import io.github.atengk.lombok.vo.UserVO;

/**
 * 用户业务接口
 *
 * @author Ateng
 * @since 2026-05-06
 */
public interface UserService {

    /**
     * 创建用户
     *
     * @param dto 用户创建参数
     * @return 用户响应对象
     */
    UserVO createUser(UserCreateDTO dto);
}
```

文件位置：`src/main/java/io/github/atengk/lombok/service/impl/UserServiceImpl.java`

下面的代码使用 `@RequiredArgsConstructor` 注入配置属性对象和日志服务。

```java
package io.github.atengk.lombok.service.impl;

import cn.hutool.core.util.StrUtil;
import io.github.atengk.lombok.config.UserProperties;
import io.github.atengk.lombok.dto.UserCreateDTO;
import io.github.atengk.lombok.service.UserLogService;
import io.github.atengk.lombok.service.UserService;
import io.github.atengk.lombok.vo.UserVO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

/**
 * 用户业务实现类
 *
 * @author Ateng
 * @since 2026-05-06
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class UserServiceImpl implements UserService {

    /**
     * 用户模块配置
     */
    private final UserProperties userProperties;

    /**
     * 用户日志服务
     */
    private final UserLogService userLogService;

    /**
     * 创建用户
     *
     * @param dto 用户创建参数
     * @return 用户响应对象
     */
    @Override
    public UserVO createUser(UserCreateDTO dto) {
        if (!Boolean.TRUE.equals(userProperties.getRegisterEnabled())) {
            log.warn("创建用户失败，用户注册功能未启用");
            throw new IllegalStateException("用户注册功能未启用");
        }

        if (StrUtil.isBlank(dto.getUsername())) {
            log.warn("创建用户失败，用户名称为空");
            throw new IllegalArgumentException("用户名称不能为空");
        }

        userLogService.recordCreateLog(dto);

        UserVO userVO = UserVO.builder()
                .id(1L)
                .username(dto.getUsername())
                .nickname(dto.getNickname())
                .mobile(dto.getMobile())
                .status(userProperties.getDefaultStatus())
                .createTime(LocalDateTime.now())
                .build();

        log.info("创建用户成功，username={}", dto.getUsername());
        return userVO;
    }
}
```

构造器注入使用建议如下：

| 建议                                | 说明                       |
| ----------------------------------- | -------------------------- |
| 依赖字段使用 `final` 修饰           | 让依赖在对象创建后不可变   |
| 类上使用 `@RequiredArgsConstructor` | 自动生成构造方法           |
| 不混用字段注入和构造器注入          | 保持注入风格统一           |
| 依赖较多时检查类职责                | 依赖过多可能说明类职责过重 |

### Builder 构建对象

Builder 适合用于构建字段较多的 VO、消息对象、第三方接口参数对象。相比全参构造方法，Builder 可以通过字段名表达参数含义，降低参数顺序错误的风险。

文件位置：`src/main/java/io/github/atengk/lombok/converter/UserConverter.java`

下面的代码通过 Builder 将 Entity 转换为 VO，并使用 Hutool 处理默认昵称和手机号脱敏。

```java
package io.github.atengk.lombok.converter;

import cn.hutool.core.util.DesensitizedUtil;
import cn.hutool.core.util.StrUtil;
import io.github.atengk.lombok.config.UserProperties;
import io.github.atengk.lombok.entity.UserEntity;
import io.github.atengk.lombok.vo.UserVO;

/**
 * 用户对象转换器
 *
 * @author Ateng
 * @since 2026-05-06
 */
public class UserConverter {

    /**
     * 转换为用户响应对象
     *
     * @param entity         用户实体对象
     * @param userProperties 用户配置属性
     * @return 用户响应对象
     */
    public static UserVO toVO(UserEntity entity, UserProperties userProperties) {
        String nickname = StrUtil.blankToDefault(entity.getNickname(), entity.getUsername());
        String mobile = Boolean.TRUE.equals(userProperties.getMobileMaskEnabled())
                ? DesensitizedUtil.mobilePhone(entity.getMobile())
                : entity.getMobile();

        return UserVO.builder()
                .id(entity.getId())
                .username(entity.getUsername())
                .nickname(nickname)
                .mobile(mobile)
                .status(entity.getStatus())
                .createTime(entity.getCreateTime())
                .build();
    }
}
```

文件位置：`src/main/java/io/github/atengk/lombok/service/UserBuildService.java`

下面的代码展示在业务代码中构建默认用户响应对象。

```java
package io.github.atengk.lombok.service;

import io.github.atengk.lombok.config.UserProperties;
import io.github.atengk.lombok.vo.UserVO;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

/**
 * 用户构建服务
 *
 * @author Ateng
 * @since 2026-05-06
 */
@Service
@RequiredArgsConstructor
public class UserBuildService {

    /**
     * 用户模块配置
     */
    private final UserProperties userProperties;

    /**
     * 构建默认用户
     *
     * @return 用户响应对象
     */
    public UserVO buildDefaultUser() {
        return UserVO.builder()
                .id(0L)
                .username("default-user")
                .nickname("默认用户")
                .mobile("未填写")
                .status(userProperties.getDefaultStatus())
                .createTime(LocalDateTime.now())
                .build();
    }
}
```

Builder 使用建议如下：

| 场景                 | 建议                                 |
| -------------------- | ------------------------------------ |
| 字段较多的响应对象   | 推荐使用                             |
| 有多个可选字段的对象 | 推荐使用                             |
| 字段较少的简单对象   | 普通构造方法或 Setter 即可           |
| 字段有默认值         | 使用 `@Builder.Default`              |
| 需要对象不可变       | 可以配合 `@Getter`，减少 Setter 暴露 |

### 链式访问配置

Lombok 的 `@Accessors(chain = true)` 可以让 Setter 方法返回当前对象，从而支持链式调用。它适合配置对象、构建参数对象和测试数据构造场景，但不建议在所有业务对象中默认启用。

文件位置：`src/main/java/io/github/atengk/lombok/dto/UserImportDTO.java`

下面的代码定义用户导入参数对象，并启用链式访问。

```java
package io.github.atengk.lombok.dto;

import lombok.Getter;
import lombok.Setter;
import lombok.ToString;
import lombok.experimental.Accessors;

/**
 * 用户导入参数
 *
 * @author Ateng
 * @since 2026-05-06
 */
@Getter
@Setter
@ToString
@Accessors(chain = true)
public class UserImportDTO {

    /**
     * 文件地址
     */
    private String fileUrl;

    /**
     * 是否覆盖已有用户
     */
    private Boolean overrideEnabled;

    /**
     * 默认用户状态
     */
    private String defaultStatus;

    /**
     * 操作人
     */
    private String operator;
}
```

使用方式如下。

```java
UserImportDTO importDTO = new UserImportDTO()
        .setFileUrl("https://example.com/user.xlsx")
        .setOverrideEnabled(Boolean.FALSE)
        .setDefaultStatus("ENABLE")
        .setOperator("ateng");
```

文件位置：`src/main/java/io/github/atengk/lombok/service/UserImportService.java`

下面的代码在业务服务中使用链式访问对象，并通过 Hutool 判断导入文件地址是否为空。

```java
package io.github.atengk.lombok.service;

import cn.hutool.core.util.StrUtil;
import io.github.atengk.lombok.dto.UserImportDTO;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

/**
 * 用户导入服务
 *
 * @author Ateng
 * @since 2026-05-06
 */
@Slf4j
@Service
public class UserImportService {

    /**
     * 导入用户
     *
     * @param importDTO 用户导入参数
     */
    public void importUser(UserImportDTO importDTO) {
        if (importDTO == null || StrUtil.isBlank(importDTO.getFileUrl())) {
            log.warn("用户导入失败，文件地址为空");
            throw new IllegalArgumentException("文件地址不能为空");
        }

        log.info("开始导入用户，fileUrl={}，operator={}", importDTO.getFileUrl(), importDTO.getOperator());

        // 这里编写读取文件、解析数据、批量保存等业务逻辑

        log.info("用户导入完成，operator={}", importDTO.getOperator());
    }
}
```

链式访问使用建议如下：

| 建议                   | 说明                                                |
| ---------------------- | --------------------------------------------------- |
| 测试数据构造可以使用   | 代码更紧凑                                          |
| 配置型参数对象可以使用 | 连续设置多个配置项更清晰                            |
| API 请求 DTO 谨慎使用  | 可能影响部分框架或团队对 JavaBean Setter 习惯的理解 |
| Entity 不建议默认使用  | 持久化对象保持普通 JavaBean 风格更稳妥              |

完成以上代码后，可以通过编译命令验证 Lombok 注解是否正常生效。

```bash
mvn clean compile
```

或：

```bash
./gradlew clean compileJava
```

如果编译通过，说明 DTO、VO、Entity、配置属性对象，以及日志对象、构造器注入、Builder 构建和链式访问配置均能正常参与编译。



## Spring Boot 3 实战示例

本节通过一个用户模块示例说明 Lombok 在 Spring Boot 3 项目中的实际使用方式。示例覆盖 Controller、Service、配置类和参数对象，重点展示 `@Getter`、`@Setter`、`@ToString`、`@Builder`、`@RequiredArgsConstructor`、`@Slf4j` 等注解在真实业务代码中的组合使用。

### Controller 示例

Controller 层负责接收 HTTP 请求、校验请求参数、调用业务服务并返回接口结果。使用 Lombok 的 `@RequiredArgsConstructor` 可以完成构造器注入，使用 `@Slf4j` 可以自动生成日志对象。

文件位置：`src/main/java/io/github/atengk/lombok/controller/UserController.java`

下面的代码定义用户接口控制器，包含创建用户和查询用户详情两个接口。

```java
package io.github.atengk.lombok.controller;

import io.github.atengk.lombok.dto.UserCreateDTO;
import io.github.atengk.lombok.service.UserService;
import io.github.atengk.lombok.vo.UserVO;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

/**
 * 用户接口控制器
 *
 * @author Ateng
 * @since 2026-05-06
 */
@Slf4j
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/users")
public class UserController {

    /**
     * 用户业务服务
     */
    private final UserService userService;

    /**
     * 创建用户
     *
     * @param dto 用户创建参数
     * @return 用户响应对象
     */
    @PostMapping
    public UserVO createUser(@Valid @RequestBody UserCreateDTO dto) {
        log.info("接收到创建用户请求，username={}", dto.getUsername());
        return userService.createUser(dto);
    }

    /**
     * 查询用户详情
     *
     * @param id 用户ID
     * @return 用户响应对象
     */
    @GetMapping("/{id}")
    public UserVO getUser(@PathVariable Long id) {
        log.info("接收到查询用户详情请求，id={}", id);
        return userService.getUser(id);
    }
}
```

该示例中，`@RequiredArgsConstructor` 会为 `final UserService userService` 生成构造方法，Spring 容器会通过构造器完成依赖注入。`@Slf4j` 会生成 `log` 日志对象，可以直接在接口方法中输出日志。

接口调用示例：

```bash
# 创建用户
curl -X POST "http://localhost:8080/api/users" \
  -H "Content-Type: application/json" \
  -d '{
    "username": "ateng",
    "nickname": "阿腾",
    "age": 18,
    "mobile": "13800138000"
  }'

# 查询用户详情
curl -X GET "http://localhost:8080/api/users/1"
```

### Service 示例

Service 层负责处理业务逻辑。使用 Lombok 的 `@RequiredArgsConstructor` 可以简化依赖注入，使用 `@Slf4j` 可以输出关键业务日志，使用 Hutool 可以简化字符串、脱敏和默认值处理。

文件位置：`src/main/java/io/github/atengk/lombok/service/UserService.java`

下面的代码定义用户业务接口。

```java
package io.github.atengk.lombok.service;

import io.github.atengk.lombok.dto.UserCreateDTO;
import io.github.atengk.lombok.vo.UserVO;

/**
 * 用户业务接口
 *
 * @author Ateng
 * @since 2026-05-06
 */
public interface UserService {

    /**
     * 创建用户
     *
     * @param dto 用户创建参数
     * @return 用户响应对象
     */
    UserVO createUser(UserCreateDTO dto);

    /**
     * 查询用户详情
     *
     * @param id 用户ID
     * @return 用户响应对象
     */
    UserVO getUser(Long id);
}
```

文件位置：`src/main/java/io/github/atengk/lombok/service/impl/UserServiceImpl.java`

下面的代码定义用户业务实现类，演示 Lombok 在业务代码中的典型组合用法。

```java
package io.github.atengk.lombok.service.impl;

import cn.hutool.core.util.DesensitizedUtil;
import cn.hutool.core.util.StrUtil;
import io.github.atengk.lombok.config.UserProperties;
import io.github.atengk.lombok.dto.UserCreateDTO;
import io.github.atengk.lombok.service.UserService;
import io.github.atengk.lombok.vo.UserVO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

/**
 * 用户业务实现类
 *
 * @author Ateng
 * @since 2026-05-06
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class UserServiceImpl implements UserService {

    /**
     * 用户模块配置
     */
    private final UserProperties userProperties;

    /**
     * 创建用户
     *
     * @param dto 用户创建参数
     * @return 用户响应对象
     */
    @Override
    public UserVO createUser(UserCreateDTO dto) {
        if (!Boolean.TRUE.equals(userProperties.getRegisterEnabled())) {
            log.warn("创建用户失败，用户注册功能未启用");
            throw new IllegalStateException("用户注册功能未启用");
        }

        if (StrUtil.isBlank(dto.getUsername())) {
            log.warn("创建用户失败，用户名称为空");
            throw new IllegalArgumentException("用户名称不能为空");
        }

        String mobile = Boolean.TRUE.equals(userProperties.getMobileMaskEnabled())
                ? DesensitizedUtil.mobilePhone(dto.getMobile())
                : dto.getMobile();

        UserVO userVO = UserVO.builder()
                .id(1L)
                .username(dto.getUsername())
                .nickname(StrUtil.blankToDefault(dto.getNickname(), dto.getUsername()))
                .mobile(mobile)
                .status(userProperties.getDefaultStatus())
                .createTime(LocalDateTime.now())
                .build();

        log.info("创建用户成功，username={}，status={}", userVO.getUsername(), userVO.getStatus());
        return userVO;
    }

    /**
     * 查询用户详情
     *
     * @param id 用户ID
     * @return 用户响应对象
     */
    @Override
    public UserVO getUser(Long id) {
        if (id == null || id <= 0) {
            log.warn("查询用户详情失败，用户ID不合法，id={}", id);
            throw new IllegalArgumentException("用户ID不合法");
        }

        UserVO userVO = UserVO.builder()
                .id(id)
                .username("ateng")
                .nickname("阿腾")
                .mobile("138****8000")
                .status(userProperties.getDefaultStatus())
                .createTime(LocalDateTime.now())
                .build();

        log.info("查询用户详情成功，id={}", id);
        return userVO;
    }
}
```

该示例中，`@RequiredArgsConstructor` 负责注入 `UserProperties`，`@Slf4j` 负责生成日志对象，`@Builder` 负责构建 `UserVO`。业务判断中使用 Hutool 的 `StrUtil` 和 `DesensitizedUtil`，可以减少空值判断和脱敏处理的样板代码。

### 配置类示例

配置类用于集中管理业务配置。Spring Boot 3 中推荐使用 `@ConfigurationProperties` 绑定业务配置，配合 Lombok 生成 Getter 和 Setter。相比在代码中大量使用 `@Value`，配置属性对象更适合中大型项目维护。

文件位置：`src/main/resources/application.yml`

下面的配置定义用户模块的业务参数。

```yaml
server:
  # 应用端口
  port: 8080

spring:
  application:
    # 应用名称
    name: springboot3-lombok-demo

app:
  user:
    # 是否启用用户注册
    register-enabled: true
    # 默认用户状态
    default-status: ENABLE
    # 默认分页大小
    default-page-size: 10
    # 是否启用手机号脱敏
    mobile-mask-enabled: true
```

文件位置：`src/main/java/io/github/atengk/lombok/config/UserProperties.java`

下面的代码定义用户模块配置属性对象。

```java
package io.github.atengk.lombok.config;

import lombok.Getter;
import lombok.Setter;
import lombok.ToString;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * 用户模块配置属性
 *
 * @author Ateng
 * @since 2026-05-06
 */
@Getter
@Setter
@ToString
@ConfigurationProperties(prefix = "app.user")
public class UserProperties {

    /**
     * 是否启用用户注册
     */
    private Boolean registerEnabled = Boolean.TRUE;

    /**
     * 默认用户状态
     */
    private String defaultStatus = "ENABLE";

    /**
     * 默认分页大小
     */
    private Integer defaultPageSize = 10;

    /**
     * 是否启用手机号脱敏
     */
    private Boolean mobileMaskEnabled = Boolean.TRUE;
}
```

文件位置：`src/main/java/io/github/atengk/lombok/config/UserModuleConfig.java`

下面的代码启用用户模块配置属性绑定。

```java
package io.github.atengk.lombok.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;

/**
 * 用户模块配置类
 *
 * @author Ateng
 * @since 2026-05-06
 */
@Slf4j
@Configuration
@EnableConfigurationProperties(UserProperties.class)
public class UserModuleConfig {

    /**
     * 初始化用户模块配置
     */
    public UserModuleConfig() {
        log.info("用户模块配置初始化完成");
    }
}
```

配置属性对象使用 Lombok 时，通常需要 `@Getter` 和 `@Setter`。Spring Boot 绑定配置属性时需要写入字段值，如果只加 `@Getter` 而没有构造器绑定配置，则属性可能无法正常注入。

### 参数对象示例

参数对象用于承载接口入参。Spring Boot 3 项目中，请求参数对象一般配合 `jakarta.validation` 做校验，配合 Lombok 生成 Getter、Setter 和 `toString`。对于敏感字段，需要通过 `@ToString.Exclude` 避免输出到日志中。

文件位置：`src/main/java/io/github/atengk/lombok/dto/UserCreateDTO.java`

下面的代码定义用户创建参数对象。

```java
package io.github.atengk.lombok.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

/**
 * 用户创建参数对象
 *
 * @author Ateng
 * @since 2026-05-06
 */
@Getter
@Setter
@ToString
public class UserCreateDTO {

    /**
     * 用户名称
     */
    @NotBlank(message = "用户名称不能为空")
    private String username;

    /**
     * 用户昵称
     */
    private String nickname;

    /**
     * 用户年龄
     */
    @NotNull(message = "用户年龄不能为空")
    @Min(value = 1, message = "用户年龄不能小于1")
    @Max(value = 120, message = "用户年龄不能大于120")
    private Integer age;

    /**
     * 手机号码
     */
    @NotBlank(message = "手机号码不能为空")
    private String mobile;
}
```

文件位置：`src/main/java/io/github/atengk/lombok/dto/UserLoginDTO.java`

下面的代码定义登录参数对象，并排除密码字段的日志输出。

```java
package io.github.atengk.lombok.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

/**
 * 用户登录参数对象
 *
 * @author Ateng
 * @since 2026-05-06
 */
@Getter
@Setter
@ToString
public class UserLoginDTO {

    /**
     * 登录账号
     */
    @NotBlank(message = "登录账号不能为空")
    private String username;

    /**
     * 登录密码
     */
    @ToString.Exclude
    @NotBlank(message = "登录密码不能为空")
    private String password;
}
```

文件位置：`src/main/java/io/github/atengk/lombok/vo/UserVO.java`

下面的代码定义用户响应对象，使用 Builder 构建返回结果。

```java
package io.github.atengk.lombok.vo;

import lombok.Builder;
import lombok.Getter;
import lombok.ToString;

import java.time.LocalDateTime;

/**
 * 用户响应对象
 *
 * @author Ateng
 * @since 2026-05-06
 */
@Getter
@ToString
@Builder
public class UserVO {

    /**
     * 用户ID
     */
    private Long id;

    /**
     * 用户名称
     */
    private String username;

    /**
     * 用户昵称
     */
    private String nickname;

    /**
     * 手机号码
     */
    private String mobile;

    /**
     * 用户状态
     */
    @Builder.Default
    private String status = "ENABLE";

    /**
     * 创建时间
     */
    private LocalDateTime createTime;
}
```

参数对象使用建议如下：

| 对象类型 | 推荐注解                                  | 说明                       |
| -------- | ----------------------------------------- | -------------------------- |
| 请求 DTO | `@Getter`、`@Setter`、`@ToString`         | 适合参数接收和校验         |
| 登录 DTO | `@Getter`、`@Setter`、`@ToString.Exclude` | 避免密码输出到日志         |
| 响应 VO  | `@Getter`、`@Builder`、`@ToString`        | 创建后通常不需要修改       |
| 查询 DTO | `@Data` 或 `@Getter`、`@Setter`           | 简单查询参数可使用 `@Data` |

## 编译与运行验证

完成 Lombok 依赖配置、IDE 配置和业务代码编写后，需要通过本地编译、单元测试和启动运行进行验证。验证的目标是确认 Lombok 注解可以正常参与编译，Spring 依赖注入可以正常完成，接口调用可以得到预期结果。

### 本地编译检查

本地编译检查用于验证 Lombok 注解是否正常生效。因为 Lombok 是编译期工具，所以编译阶段能否通过是最直接的判断方式。

Maven 项目执行以下命令：

```bash
# 清理旧编译产物并重新编译
mvn clean compile
```

Gradle 项目执行以下命令：

```bash
# 清理旧编译产物并重新编译 Java 代码
./gradlew clean compileJava
```

如果项目中包含单元测试，可以执行完整校验：

```bash
# Maven 执行测试
mvn clean test

# Gradle 执行测试
./gradlew clean test
```

编译通过后，说明 `@Getter`、`@Setter`、`@Builder`、`@RequiredArgsConstructor`、`@Slf4j` 等注解已经正常参与编译。如果编译失败，应优先检查 Lombok 依赖、注解处理器配置、JDK 版本和 IDE 注解处理开关。

### 单元测试验证

单元测试可以验证 Controller 接口、Service 构造器注入和 Builder 构建对象是否正常工作。下面示例使用 `MockMvc` 调用接口，验证创建用户接口是否返回成功结果。

文件位置：`src/test/java/io/github/atengk/lombok/controller/UserControllerTest.java`

下面的代码定义用户接口测试类，用于验证 Lombok 相关代码在 Spring Boot 测试环境中是否正常运行。

```java
package io.github.atengk.lombok.controller;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import static org.hamcrest.Matchers.equalTo;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * 用户接口测试
 *
 * @author Ateng
 * @since 2026-05-06
 */
@SpringBootTest
@AutoConfigureMockMvc
class UserControllerTest {

    /**
     * MockMvc 测试客户端
     */
    @Autowired
    private MockMvc mockMvc;

    /**
     * 测试创建用户
     *
     * @throws Exception 测试异常
     */
    @Test
    @DisplayName("创建用户成功")
    void createUserSuccess() throws Exception {
        String requestBody = """
                {
                  "username": "ateng",
                  "nickname": "阿腾",
                  "age": 18,
                  "mobile": "13800138000"
                }
                """;

        mockMvc.perform(post("/api/users")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.username", equalTo("ateng")))
                .andExpect(jsonPath("$.nickname", equalTo("阿腾")))
                .andExpect(jsonPath("$.status", equalTo("ENABLE")));
    }

    /**
     * 测试查询用户详情
     *
     * @throws Exception 测试异常
     */
    @Test
    @DisplayName("查询用户详情成功")
    void getUserSuccess() throws Exception {
        mockMvc.perform(get("/api/users/{id}", 1L))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id", equalTo(1)))
                .andExpect(jsonPath("$.username", equalTo("ateng")))
                .andExpect(jsonPath("$.status", equalTo("ENABLE")));
    }
}
```

执行测试命令：

```bash
# Maven 执行单元测试
mvn test
```

或：

```bash
# Gradle 执行单元测试
./gradlew test
```

如果测试通过，说明 Controller、Service、配置属性对象、Builder 构建对象和构造器注入都可以在 Spring Boot 3 环境中正常工作。

### 启动运行验证

启动运行验证用于确认应用可以正常启动，配置属性可以正常加载，接口可以正常访问，日志可以正常输出。

Maven 项目启动命令如下：

```bash
# 使用 Maven 启动 Spring Boot 应用
mvn spring-boot:run
```

Gradle 项目启动命令如下：

```bash
# 使用 Gradle 启动 Spring Boot 应用
./gradlew bootRun
```

也可以先打包再启动：

```bash
# Maven 打包
mvn clean package

# 启动 Jar 包
java -jar target/springboot3-lombok-demo-1.0.0.jar
```

启动成功后，控制台应能看到类似日志：

```text
用户模块配置初始化完成
Started LombokApplication in 2.345 seconds
```

接口验证命令如下：

```bash
# 创建用户
curl -X POST "http://localhost:8080/api/users" \
  -H "Content-Type: application/json" \
  -d '{
    "username": "ateng",
    "nickname": "阿腾",
    "age": 18,
    "mobile": "13800138000"
  }'
```

预期响应示例：

```json
{
  "id": 1,
  "username": "ateng",
  "nickname": "阿腾",
  "mobile": "138****8000",
  "status": "ENABLE",
  "createTime": "2026-05-06T10:30:00"
}
```

查询用户详情命令如下：

```bash
# 查询用户详情
curl -X GET "http://localhost:8080/api/users/1"
```

预期响应示例：

```json
{
  "id": 1,
  "username": "ateng",
  "nickname": "阿腾",
  "mobile": "138****8000",
  "status": "ENABLE",
  "createTime": "2026-05-06T10:30:00"
}
```

如果接口返回正常，并且控制台输出了 Controller 和 Service 中的日志，说明 Lombok 在当前 Spring Boot 3 项目中已经正常生效。

## 常见问题

本节整理 Lombok 在 Spring Boot 3 项目中的常见问题。排查时建议先从编译结果入手，再检查 IDE、依赖、插件和注解使用方式。

### 注解不生效

注解不生效通常表现为 Getter、Setter、Builder、构造方法或日志对象无法识别。该问题一般和依赖配置、注解处理器配置、IDE 配置或 JDK 版本有关。

常见原因和处理方式如下：

| 问题原因           | 现象                       | 处理方式                                              |
| ------------------ | -------------------------- | ----------------------------------------------------- |
| 未引入 Lombok 依赖 | 编译时报找不到 Lombok 注解 | 在 Maven 或 Gradle 中添加 Lombok 依赖                 |
| 未配置注解处理器   | 注解存在但方法未生成       | Gradle 配置 `annotationProcessor`，Maven 检查编译插件 |
| IDE 未开启注解处理 | 命令行编译正常，IDE 标红   | 开启 IDE Annotation Processing                        |
| Lombok 版本过旧    | 新 JDK 下编译异常          | 升级 Lombok 到较新稳定版本                            |
| 依赖未刷新         | 代码仍然标红               | 重新加载 Maven 或 Gradle 项目                         |

Maven 项目优先检查以下配置：

```xml
<!-- Lombok：编译期代码生成工具 -->
<dependency>
    <groupId>org.projectlombok</groupId>
    <artifactId>lombok</artifactId>
    <scope>provided</scope>
</dependency>
```

Gradle 项目优先检查以下配置：

```gradle
// Lombok：仅编译期可见
compileOnly 'org.projectlombok:lombok'

// Lombok 注解处理器：编译阶段生成代码
annotationProcessor 'org.projectlombok:lombok'
```

验证命令如下：

```bash
# Maven 编译验证
mvn clean compile

# Gradle 编译验证
./gradlew clean compileJava
```

如果命令行编译通过，但 IDE 仍然标红，问题通常在 IDE 配置或缓存中。

### IDE 编译报错

IDE 编译报错通常发生在开发环境中，常见表现为 `getXxx()`、`setXxx()`、`builder()`、`log` 等符号无法识别，但命令行执行 `mvn clean compile` 或 `./gradlew clean compileJava` 可能是正常的。

IntelliJ IDEA 排查步骤如下：

1. 检查 `Settings -> Build, Execution, Deployment -> Compiler -> Annotation Processors`。
2. 确认已勾选 `Enable annotation processing`。
3. 检查 Project SDK 是否为 JDK 17 或更高版本。
4. 重新加载 Maven 或 Gradle 项目。
5. 执行 `File -> Invalidate Caches... -> Invalidate and Restart`。

Eclipse 或 Spring Tools Suite 排查步骤如下：

1. 确认已经安装 Lombok 到 IDE。
2. 检查 `eclipse.ini` 或 STS 启动配置中是否存在 `-javaagent`。
3. 完全退出 IDE 后重新打开。
4. 刷新 Maven 或 Gradle 项目。
5. 检查项目 JDK 是否为 17 或更高版本。

VS Code 排查步骤如下：

1. 确认安装 Java 扩展包和 Lombok 支持扩展。
2. 确认 Java Language Server 正常启动。
3. 执行 `Java: Clean Java Language Server Workspace`。
4. 重新打开项目。
5. 执行 Maven 或 Gradle 编译命令确认项目本身没有问题。

判断问题归属时，可以先执行：

```bash
mvn clean compile
```

如果命令行编译失败，说明项目配置或代码本身存在问题；如果命令行编译成功但 IDE 报错，说明重点应排查 IDE 注解处理和缓存。

### 构造器注入异常

构造器注入异常通常发生在使用 `@RequiredArgsConstructor` 时。常见现象包括 Spring Bean 无法注入、构造方法未生成、依赖字段为 `null` 或循环依赖启动失败。

常见原因和处理方式如下：

| 问题原因                        | 示例                                   | 处理方式                                      |
| ------------------------------- | -------------------------------------- | --------------------------------------------- |
| 字段未使用 `final`              | `private UserService userService;`     | 改为 `private final UserService userService;` |
| 缺少 `@RequiredArgsConstructor` | 类中没有构造方法                       | 在类上添加 `@RequiredArgsConstructor`         |
| 注入类未注册为 Bean             | 普通类未加 `@Service`、`@Component`    | 添加对应 Spring 注解                          |
| 存在循环依赖                    | A 注入 B，B 又注入 A                   | 拆分职责或调整依赖方向                        |
| 混用字段注入和构造器注入        | 同一类中同时使用 `@Autowired` 字段注入 | 统一使用构造器注入                            |

错误写法示例：

```java
package io.github.atengk.lombok.service.impl;

import io.github.atengk.lombok.config.UserProperties;
import org.springframework.stereotype.Service;

/**
 * 错误的用户服务示例
 *
 * @author Ateng
 * @since 2026-05-06
 */
@Service
public class ErrorUserService {

    /**
     * 未使用 final，@RequiredArgsConstructor 不会为该字段生成构造参数
     */
    private UserProperties userProperties;
}
```

推荐写法如下：

```java
package io.github.atengk.lombok.service.impl;

import io.github.atengk.lombok.config.UserProperties;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

/**
 * 正确的用户服务示例
 *
 * @author Ateng
 * @since 2026-05-06
 */
@Service
@RequiredArgsConstructor
public class CorrectUserService {

    /**
     * 使用 final 字段，@RequiredArgsConstructor 会生成构造器参数
     */
    private final UserProperties userProperties;
}
```

如果构造器注入失败，应先检查依赖字段是否为 `final`，再检查依赖类是否已经被 Spring 容器管理。

### Builder 默认值问题

`@Builder` 默认不会直接使用字段初始化值。也就是说，如果字段写了默认值，但没有使用 `@Builder.Default`，通过 Builder 创建对象时该字段可能会变成 `null`、`0` 或 `false` 等类型默认值。

错误写法示例：

```java
package io.github.atengk.lombok.vo;

import lombok.Builder;
import lombok.Getter;

/**
 * 错误的 Builder 默认值示例
 *
 * @author Ateng
 * @since 2026-05-06
 */
@Getter
@Builder
public class ErrorUserVO {

    /**
     * 通过 Builder 创建对象时，该默认值可能不会生效
     */
    private String status = "ENABLE";
}
```

使用方式如下：

```java
ErrorUserVO userVO = ErrorUserVO.builder()
        .build();

System.out.println(userVO.getStatus());
```

上面的代码可能输出 `null`，因为 Builder 没有自动使用字段初始化值。

推荐写法如下：

```java
package io.github.atengk.lombok.vo;

import lombok.Builder;
import lombok.Getter;

/**
 * 正确的 Builder 默认值示例
 *
 * @author Ateng
 * @since 2026-05-06
 */
@Getter
@Builder
public class CorrectUserVO {

    /**
     * 使用 Builder 构建对象时保留默认状态
     */
    @Builder.Default
    private String status = "ENABLE";
}
```

验证代码如下：

```java
CorrectUserVO userVO = CorrectUserVO.builder()
        .build();

System.out.println(userVO.getStatus());
```

此时输出结果为：

```text
ENABLE
```

Builder 默认值问题排查建议如下：

| 场景                 | 处理方式                                  |
| -------------------- | ----------------------------------------- |
| 字段需要固定默认值   | 使用 `@Builder.Default`                   |
| 默认值来自配置文件   | 在业务代码中从配置属性对象读取后显式设置  |
| 默认值需要动态计算   | 在 Service 或转换器中计算后再调用 Builder |
| 字段必须由调用方传入 | 不设置默认值，在业务层做参数校验          |

对于响应 VO、消息对象和复杂参数对象，建议把默认值规则写清楚。如果默认值来自配置文件，不建议直接写死在 VO 中，而应在 Service 或 Converter 中读取配置后设置。