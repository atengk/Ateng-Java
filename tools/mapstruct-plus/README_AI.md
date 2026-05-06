# MapStruct Plus

## 技术概述

### MapStruct Plus 简介

MapStruct Plus 是基于 MapStruct 的 Java Bean 转换增强工具，主要用于解决实体类、DTO、VO、请求参数对象之间的属性转换问题。它在保留 MapStruct 编译期生成转换代码、高性能、类型安全等特性的基础上，进一步简化了 Mapper 接口的编写成本，使开发者可以通过少量注解完成常见对象转换。官方文档将其定位为 MapStruct 的增强工具，并说明它可以在 MapStruct 基础上自动生成 Mapper 接口，从而让 Java 类型转换更加便捷。([mapstruct.plus](https://www.mapstruct.plus/))

在 Spring Boot 3 项目中，MapStruct Plus 通常用于替代手写转换代码、`BeanUtils.copyProperties` 以及大量重复的字段赋值逻辑。由于转换实现类在编译期生成，运行时不会依赖反射完成常规属性复制，因此更适合对性能、可维护性和类型安全有要求的后端项目。

典型转换链路如下：

```text
Entity  <->  DTO  <->  VO
数据库实体     业务传输对象     接口返回对象
```

在实际开发中，可以将数据库实体类与接口返回对象解耦，避免 Controller 直接暴露 Entity，也可以减少 Service 层中重复的字段复制代码。

### 与 MapStruct 的关系

MapStruct 是底层对象映射框架，它通过注解处理器在编译期生成类型安全的 Mapper 实现类；MapStruct Plus 是对 MapStruct 的增强封装，主要解决 MapStruct 原生使用时需要手动定义 Mapper 接口、维护转换方法较多的问题。MapStruct 官方说明其核心机制是通过 annotation processor 生成类型安全的 Bean mapping 代码，而 MapStruct Plus 官方说明其在 MapStruct 基础上增强了自动生成 Mapper 接口等能力。([mapstruct.org](https://mapstruct.org/documentation/stable/reference/html/index.html?utm_source=chatgpt.com))

二者关系可以理解为：

| 对比项      | MapStruct                               | MapStruct Plus                                     |
| ----------- | --------------------------------------- | -------------------------------------------------- |
| 核心定位    | Java Bean 映射代码生成器                | MapStruct 增强工具                                 |
| 生成时机    | 编译期生成 Mapper 实现类                | 编译期生成转换相关代码                             |
| 使用方式    | 通常需要手写 `@Mapper` 接口             | 通过 `@AutoMapper` 等注解减少 Mapper 编写          |
| Spring 集成 | 可通过 `componentModel = "spring"` 集成 | 提供 Spring Boot Starter，便于在 Spring 项目中使用 |
| 适合场景    | 需要精细控制映射逻辑                    | 需要快速完成常规对象转换                           |

需要注意的是，MapStruct Plus 并不是替代 MapStruct 的全新映射引擎，而是在 MapStruct 之上做增强。对于复杂字段映射、自定义转换、表达式转换等场景，仍然需要理解 MapStruct 的基本映射规则。

### 适用场景

MapStruct Plus 适用于 Spring Boot 后端项目中高频出现的对象转换场景，尤其适合领域对象、持久化对象、接口对象之间字段结构相近但分层明确的项目。官方文档说明 MapStruct Plus 支持 JDK8~17、Spring Boot2~3，并强调其基于注解处理器、转换代码在编译期完成生成。([mapstruct.plus](https://www.mapstruct.plus/))

常见适用场景如下：

| 场景               | 说明                                    |
| ------------------ | --------------------------------------- |
| Entity 转 VO       | 查询数据库实体后，转换为接口响应对象    |
| DTO 转 Entity      | 新增、修改接口入参转换为数据库实体      |
| VO 转 DTO          | 多层接口或服务间调用时进行对象隔离      |
| 集合转换           | `List<Entity>` 转 `List<VO>`            |
| 分页转换           | 分页查询结果中的记录对象转换            |
| 嵌套对象转换       | 用户、部门、角色等组合对象转换          |
| 枚举与字典字段转换 | 枚举值、状态值、展示文本之间转换        |
| 日期时间字段转换   | `LocalDateTime`、字符串、时间戳之间转换 |

不建议在以下场景中过度使用：

| 场景             | 原因                                              |
| ---------------- | ------------------------------------------------- |
| 字段差异极大     | 转换规则会变复杂，可读性下降                      |
| 涉及大量业务计算 | 建议放在 Service 或领域方法中处理                 |
| 需要动态字段映射 | MapStruct Plus 以编译期生成为主，不适合强动态规则 |
| 仅一两个字段赋值 | 手写可能更直接                                    |

## 环境准备

### JDK 版本要求

Spring Boot 3 项目建议使用 JDK 17 及以上版本。Spring Boot 3.0.x 官方要求 Java 17，并且 Spring Boot 3.3.x 仍然要求至少 Java 17，因此在 Spring Boot 3 技术栈中，JDK 17 应作为最低基线。([Home](https://docs.spring.io/spring-boot/docs/3.0.5/reference/html/getting-started.html?utm_source=chatgpt.com))

推荐版本如下：

| 项目类型                  | 推荐 JDK         |
| ------------------------- | ---------------- |
| Spring Boot 3.0.x         | JDK 17           |
| Spring Boot 3.1.x ~ 3.3.x | JDK 17 或 JDK 21 |
| 新项目                    | 优先 JDK 21 LTS  |
| 老项目迁移                | 先升级到 JDK 17  |

可以通过以下命令检查本地 JDK 版本：

```bash
# 查看当前 Java 运行环境版本
java -version

# 查看当前 Maven 使用的 Java 版本
mvn -version
```

输出中需要确认 `java.version` 至少为 `17`。如果 `mvn -version` 显示的 Java 版本低于项目要求，即使系统安装了 JDK 17，也需要检查 `JAVA_HOME` 和 IDE Maven Runner 的 JDK 配置。

### Spring Boot 版本要求

MapStruct Plus 官方说明兼容 Spring Boot 2~3；本文档面向 Spring Boot 3 项目，因此建议选择 Spring Boot 3.0 及以上版本，并统一使用 Jakarta EE 命名空间相关依赖。([mapstruct.plus](https://www.mapstruct.plus/))

推荐版本组合如下：

| 组件           | 推荐版本 |
| -------------- | -------- |
| JDK            | 17+      |
| Spring Boot    | 3.0+     |
| MapStruct Plus | 1.5.0    |
| MapStruct      | 1.6.3    |
| Maven          | 3.6.3+   |

MapStruct Plus `1.5.0` 是当前官方文档和 Maven Central 展示的版本，该版本将 MapStruct 升级到 `1.6.3`。([mapstruct.plus](https://www.mapstruct.plus/))

### Maven 依赖配置

在 Spring Boot 3 项目的 `pom.xml` 中，建议统一通过 `<properties>` 管理版本号，避免依赖和编译插件中的版本不一致。MapStruct Plus 的 Spring Boot Starter 当前 Maven 坐标为 `io.github.linpeilie:mapstruct-plus-spring-boot-starter:1.5.0`。([Maven Central](https://central.sonatype.com/artifact/io.github.linpeilie/mapstruct-plus-spring-boot-starter?utm_source=chatgpt.com))

文件位置：`pom.xml`

```xml
<properties>
    <!-- Spring Boot 3 项目建议使用 JDK 17 或更高版本 -->
    <java.version>17</java.version>

    <!-- MapStruct Plus 当前稳定版本 -->
    <mapstruct-plus.version>1.5.0</mapstruct-plus.version>

    <!-- MapStruct Plus 1.5.0 对应的 MapStruct 版本 -->
    <mapstruct.version>1.6.3</mapstruct.version>

    <!-- Lombok 用于减少实体类、DTO、VO 样板代码 -->
    <lombok.version>1.18.36</lombok.version>

    <!-- 解决 Lombok 与 MapStruct 注解处理顺序问题 -->
    <lombok-mapstruct-binding.version>0.2.0</lombok-mapstruct-binding.version>
</properties>

<dependencies>
    <!-- Spring Boot Web 基础依赖，用于 Controller、JSON 序列化等 Web 场景 -->
    <dependency>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-starter-web</artifactId>
    </dependency>

    <!-- MapStruct Plus Spring Boot Starter，提供对象转换能力与 Spring Boot 集成 -->
    <dependency>
        <groupId>io.github.linpeilie</groupId>
        <artifactId>mapstruct-plus-spring-boot-starter</artifactId>
        <version>${mapstruct-plus.version}</version>
    </dependency>

    <!-- Lombok，简化 Entity、DTO、VO 的 getter、setter、构造器等代码 -->
    <dependency>
        <groupId>org.projectlombok</groupId>
        <artifactId>lombok</artifactId>
        <version>${lombok.version}</version>
        <optional>true</optional>
    </dependency>

    <!-- Hutool 工具包，项目中可用于字符串、集合、日期等通用处理 -->
    <dependency>
        <groupId>cn.hutool</groupId>
        <artifactId>hutool-all</artifactId>
        <version>5.8.36</version>
    </dependency>

    <!-- 单元测试依赖，用于验证对象转换结果 -->
    <dependency>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-starter-test</artifactId>
        <scope>test</scope>
    </dependency>
</dependencies>
```

如果项目已经通过 Spring Boot Parent 管理依赖版本，`spring-boot-starter-web` 和 `spring-boot-starter-test` 通常不需要单独指定版本。MapStruct Plus 版本建议显式声明，便于后续升级和排查生成代码变化。

### 编译插件配置

MapStruct Plus 和 MapStruct 都依赖注解处理器在编译期生成代码，因此 Maven 编译插件需要正确配置 `annotationProcessorPaths`。MapStruct 官方安装文档也采用 `maven-compiler-plugin` 配置 annotation processor 的方式。([mapstruct.org](https://mapstruct.org/documentation/installation/?utm_source=chatgpt.com))

文件位置：`pom.xml`

```xml
<build>
    <plugins>
        <!-- Spring Boot 打包插件，用于生成可执行 Jar -->
        <plugin>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-maven-plugin</artifactId>
            <configuration>
                <!-- 打包时排除 Lombok，避免无意义依赖进入运行包 -->
                <excludes>
                    <exclude>
                        <groupId>org.projectlombok</groupId>
                        <artifactId>lombok</artifactId>
                    </exclude>
                </excludes>
            </configuration>
        </plugin>

        <!-- Maven 编译插件，配置 JDK 版本和注解处理器 -->
        <plugin>
            <groupId>org.apache.maven.plugins</groupId>
            <artifactId>maven-compiler-plugin</artifactId>
            <version>3.13.0</version>
            <configuration>
                <!-- Spring Boot 3 项目最低建议 JDK 17 -->
                <source>${java.version}</source>
                <target>${java.version}</target>
                <encoding>UTF-8</encoding>

                <!-- 显式声明注解处理器，确保 MapStruct Plus 能在编译期生成转换代码 -->
                <annotationProcessorPaths>
                    <!-- MapStruct Plus 注解处理器 -->
                    <path>
                        <groupId>io.github.linpeilie</groupId>
                        <artifactId>mapstruct-plus-processor</artifactId>
                        <version>${mapstruct-plus.version}</version>
                    </path>

                    <!-- Lombok 注解处理器 -->
                    <path>
                        <groupId>org.projectlombok</groupId>
                        <artifactId>lombok</artifactId>
                        <version>${lombok.version}</version>
                    </path>

                    <!-- Lombok 与 MapStruct 协作绑定，避免生成代码读取不到 Lombok 属性 -->
                    <path>
                        <groupId>org.projectlombok</groupId>
                        <artifactId>lombok-mapstruct-binding</artifactId>
                        <version>${lombok-mapstruct-binding.version}</version>
                    </path>
                </annotationProcessorPaths>

                <!-- MapStruct 编译参数，可减少生成代码中的时间戳差异，便于代码审查 -->
                <compilerArgs>
                    <arg>-Amapstruct.suppressGeneratorTimestamp=true</arg>
                    <arg>-Amapstruct.suppressGeneratorVersionInfoComment=true</arg>
                </compilerArgs>
            </configuration>
        </plugin>
    </plugins>
</build>
```

`mapstruct-plus-processor` 是 MapStruct Plus 的编译期处理器，Maven Central 显示其依赖 `mapstruct-plus` 和 `org.mapstruct:mapstruct-processor`，因此通常只需要在 `annotationProcessorPaths` 中显式加入 `mapstruct-plus-processor` 即可。([Maven Central](https://central.sonatype.com/artifact/io.github.linpeilie/mapstruct-plus-processor?utm_source=chatgpt.com))

配置完成后，可以执行以下命令验证编译是否正常：

```bash
# 清理并重新编译项目，触发注解处理器生成转换代码
mvn clean compile

# 查看编译期生成的源码目录
ls target/generated-sources/annotations
```

如果编译成功，并且 `target/generated-sources/annotations` 下出现 MapStruct Plus 或 MapStruct 生成的转换类，说明注解处理器配置生效。若 IDE 中无法识别生成代码，需要检查 IntelliJ IDEA 的 Annotation Processing 是否启用，或重新导入 Maven 项目。



## 基础配置

本节用于说明 MapStruct Plus 在 Spring Boot 3 项目中的基础使用配置，包括编译期注解处理器、Spring 扫描范围、统一转换封装和基础转换代码结构。MapStruct Plus 基于注解处理器在编译期生成转换代码，核心使用方式是给源类或目标类增加 `@AutoMapper`，然后通过 `Converter` 执行对象转换。([Libraries](https://libraries.io/maven/io.github.linpeilie%3Amapstruct-plus-object-convert?utm_source=chatgpt.com))

### 注解处理器配置

MapStruct Plus 的转换代码在编译期生成，因此 Maven 编译插件必须启用注解处理器。Spring Boot 3 项目如果同时使用 Lombok，需要将 `lombok`、`lombok-mapstruct-binding` 和 `mapstruct-plus-processor` 一起配置到 `annotationProcessorPaths` 中，避免编译时读取不到 Lombok 生成的 getter、setter。

文件位置：`pom.xml`

```xml
<build>
    <plugins>
        <!-- Spring Boot 打包插件，用于生成可执行 Jar -->
        <plugin>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-maven-plugin</artifactId>
            <configuration>
                <!-- Lombok 只参与编译，不需要进入运行包 -->
                <excludes>
                    <exclude>
                        <groupId>org.projectlombok</groupId>
                        <artifactId>lombok</artifactId>
                    </exclude>
                </excludes>
            </configuration>
        </plugin>

        <!-- Maven 编译插件，负责触发 MapStruct Plus 注解处理器 -->
        <plugin>
            <groupId>org.apache.maven.plugins</groupId>
            <artifactId>maven-compiler-plugin</artifactId>
            <version>3.13.0</version>
            <configuration>
                <!-- Spring Boot 3 推荐 JDK 17 及以上 -->
                <source>${java.version}</source>
                <target>${java.version}</target>
                <encoding>UTF-8</encoding>

                <!-- 编译期注解处理器配置 -->
                <annotationProcessorPaths>
                    <!-- MapStruct Plus 注解处理器，用于生成 Mapper 接口和实现类 -->
                    <path>
                        <groupId>io.github.linpeilie</groupId>
                        <artifactId>mapstruct-plus-processor</artifactId>
                        <version>${mapstruct-plus.version}</version>
                    </path>

                    <!-- Lombok 注解处理器，用于生成 getter、setter、构造器等代码 -->
                    <path>
                        <groupId>org.projectlombok</groupId>
                        <artifactId>lombok</artifactId>
                        <version>${lombok.version}</version>
                    </path>

                    <!-- Lombok 与 MapStruct 协作绑定，解决属性识别顺序问题 -->
                    <path>
                        <groupId>org.projectlombok</groupId>
                        <artifactId>lombok-mapstruct-binding</artifactId>
                        <version>${lombok-mapstruct-binding.version}</version>
                    </path>
                </annotationProcessorPaths>

                <!-- 减少生成代码中的时间戳和版本注释，便于代码审查 -->
                <compilerArgs>
                    <arg>-Amapstruct.suppressGeneratorTimestamp=true</arg>
                    <arg>-Amapstruct.suppressGeneratorVersionInfoComment=true</arg>
                </compilerArgs>
            </configuration>
        </plugin>
    </plugins>
</build>
```

配置完成后，在项目根目录执行以下命令验证编译结果：

```bash
# 清理并重新编译项目，触发注解处理器生成代码
mvn clean compile

# 查看编译期生成的转换代码
ls target/generated-sources/annotations
```

如果 `target/generated-sources/annotations` 目录下出现 MapStruct Plus 生成的 Mapper 或 Adapter 相关代码，说明注解处理器已经生效。若 Maven 编译正常但 IDE 提示找不到生成类，需要在 IntelliJ IDEA 中启用 `Annotation Processing`，然后重新导入 Maven 项目。

### Mapper 扫描配置

MapStruct Plus 在 Spring Boot Starter 场景下通常不需要额外配置类似 MyBatis 的 `@MapperScan`。需要保证的是：Spring Boot 启动类扫描范围覆盖业务代码包，编译生成的 Mapper 实现类能够作为 Spring Bean 被容器管理。MapStruct 的 `componentModel = "spring"` 会让生成的 Mapper 实现类成为 Spring Bean，可通过依赖注入使用；而 MyBatis 的 `@MapperScan` 是扫描数据库 Mapper 接口，两者不要混用概念。([mapstruct.org](https://mapstruct.org/documentation/1.6/api/org/mapstruct/MapperConfig.html?utm_source=chatgpt.com))

文件位置：`src/main/java/io/github/atengk/MapstructPlusApplication.java`

```java
package io.github.atengk;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * MapStruct Plus 示例项目启动类.
 *
 * @author Ateng
 * @since 2026-05-06
 */
@SpringBootApplication(scanBasePackages = "io.github.atengk")
public class MapstructPlusApplication {

    public static void main(String[] args) {
        SpringApplication.run(MapstructPlusApplication.class, args);
    }

}
```

如果项目中同时使用 MyBatis-Plus，`@MapperScan` 只用于数据库访问层 Mapper，例如：

```java
package io.github.atengk;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * 同时集成 MyBatis-Plus 时的启动类示例.
 *
 * @author Ateng
 * @since 2026-05-06
 */
@MapperScan("io.github.atengk.**.mapper")
@SpringBootApplication(scanBasePackages = "io.github.atengk")
public class MapstructPlusApplication {
}
```

这里的 `@MapperScan("io.github.atengk.**.mapper")` 只扫描 MyBatis Mapper，不负责扫描 MapStruct Plus 的转换类。MapStruct Plus 的转换能力主要由编译期生成代码和 Spring Boot Starter 自动配置共同完成。

### 全局转换配置

全局转换配置建议从两个层面处理：第一层是编译期统一策略，例如通过 MapStruct Plus 和 MapStruct 的默认配置控制空值策略、未映射字段策略；第二层是运行期统一封装，例如在项目中封装一个对象转换服务，避免各业务类直接散落调用 `Converter`。MapStruct Plus `1.5.0` 已在更新说明中提到 `MapperConfig` 支持 `uses`，可用于配置全局共享的自定义转换类。([mapstruct.plus](https://www.mapstruct.plus/?utm_source=chatgpt.com))

建议在业务项目中封装统一转换服务，后续 Service、Controller 只注入该服务，不直接依赖底层转换细节。

文件结构如下：

```text
src/main/java/io/github/atengk/mapstruct/common/PageResult.java
src/main/java/io/github/atengk/mapstruct/service/ObjectConvertService.java
```

下面定义一个通用分页结果对象，用于后续分页对象转换示例。

文件位置：`src/main/java/io/github/atengk/mapstruct/common/PageResult.java`

```java
package io.github.atengk.mapstruct.common;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.Accessors;

import java.util.List;

/**
 * 通用分页结果.
 *
 * @author Ateng
 * @since 2026-05-06
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Accessors(chain = true)
public class PageResult<T> {

    private Long current;

    private Long size;

    private Long total;

    private List<T> records;

}
```

下面封装统一对象转换服务，内部使用 MapStruct Plus 的 `Converter`，并使用 Hutool 处理空值和集合判断。

文件位置：`src/main/java/io/github/atengk/mapstruct/service/ObjectConvertService.java`

```java
package io.github.atengk.mapstruct.service;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.lang.Assert;
import cn.hutool.core.util.ObjectUtil;
import io.github.atengk.mapstruct.common.PageResult;
import io.github.linpeilie.Converter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * 对象转换服务.
 *
 * @author Ateng
 * @since 2026-05-06
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ObjectConvertService {

    private final Converter converter;

    public <T> T convert(Object source, Class<T> targetType) {
        Assert.notNull(targetType, "目标类型不能为空");
        if (ObjectUtil.isNull(source)) {
            log.debug("对象转换跳过，源对象为空，目标类型：{}", targetType.getName());
            return null;
        }
        return converter.convert(source, targetType);
    }

    public <S, T> List<T> convertList(List<S> sourceList, Class<T> targetType) {
        Assert.notNull(targetType, "目标类型不能为空");
        if (CollUtil.isEmpty(sourceList)) {
            log.debug("集合转换跳过，源集合为空，目标类型：{}", targetType.getName());
            return CollUtil.newArrayList();
        }
        return sourceList.stream()
                .filter(ObjectUtil::isNotNull)
                .map(source -> converter.convert(source, targetType))
                .toList();
    }

    public <S, T> PageResult<T> convertPage(PageResult<S> sourcePage, Class<T> targetType) {
        Assert.notNull(targetType, "目标类型不能为空");
        if (ObjectUtil.isNull(sourcePage)) {
            log.debug("分页转换跳过，源分页对象为空，目标类型：{}", targetType.getName());
            return new PageResult<T>()
                    .setCurrent(1L)
                    .setSize(0L)
                    .setTotal(0L)
                    .setRecords(CollUtil.newArrayList());
        }

        List<T> targetRecords = this.convertList(sourcePage.getRecords(), targetType);
        return new PageResult<T>()
                .setCurrent(sourcePage.getCurrent())
                .setSize(sourcePage.getSize())
                .setTotal(sourcePage.getTotal())
                .setRecords(targetRecords);
    }

}
```

该封装适合在业务层统一调用，后续如果需要加入空字符串处理、枚举转换、审计字段过滤、分页框架适配，也可以集中维护，避免散落在各个 Service 中。

## 基础对象转换

本节通过用户对象示例说明 MapStruct Plus 的基础转换方式。示例采用 `UserEntity` 和 `UserDTO`，字段名称和类型保持一致，适合演示最常见的 Entity 与 DTO 转换。MapStruct Plus 官方快速示例也是通过 `@AutoMapper(target = xxx.class)` 配置对象关系，然后注入 `Converter` 调用 `convert` 方法完成转换。([Libraries](https://libraries.io/maven/io.github.linpeilie%3Amapstruct-plus-object-convert?utm_source=chatgpt.com))

### 实体类与 DTO 定义

基础对象转换需要先定义源对象和目标对象。通常建议在 Entity 或 DTO 中任选一方增加 `@AutoMapper(target = 目标类.class)`，不要两边重复配置相同关系，避免维护混乱。

文件结构如下：

```text
src/main/java/io/github/atengk/mapstruct/entity/UserEntity.java
src/main/java/io/github/atengk/mapstruct/dto/UserDTO.java
```

下面定义数据库实体对象，模拟从数据库查询出的用户数据。

文件位置：`src/main/java/io/github/atengk/mapstruct/entity/UserEntity.java`

```java
package io.github.atengk.mapstruct.entity;

import io.github.atengk.mapstruct.dto.UserDTO;
import io.github.linpeilie.annotations.AutoMapper;
import lombok.Data;
import lombok.experimental.Accessors;

import java.time.LocalDateTime;

/**
 * 用户实体对象.
 *
 * @author Ateng
 * @since 2026-05-06
 */
@Data
@Accessors(chain = true)
@AutoMapper(target = UserDTO.class)
public class UserEntity {

    private Long id;

    private String username;

    private String nickname;

    private String mobile;

    private Boolean enabled;

    private LocalDateTime createdAt;

}
```

下面定义用户 DTO，用于 Service 层传输或 Controller 层返回。

文件位置：`src/main/java/io/github/atengk/mapstruct/dto/UserDTO.java`

```java
package io.github.atengk.mapstruct.dto;

import lombok.Data;
import lombok.experimental.Accessors;

import java.time.LocalDateTime;

/**
 * 用户数据传输对象.
 *
 * @author Ateng
 * @since 2026-05-06
 */
@Data
@Accessors(chain = true)
public class UserDTO {

    private Long id;

    private String username;

    private String nickname;

    private String mobile;

    private Boolean enabled;

    private LocalDateTime createdAt;

}
```

上述两个类字段名称和类型完全一致，MapStruct Plus 会在编译期生成对应转换代码。执行 `mvn clean compile` 后，可以在 `target/generated-sources/annotations` 中查看生成结果。

### 单对象转换

单对象转换适用于查询详情、新增参数转换、修改参数转换等场景。业务代码中推荐通过前面封装的 `ObjectConvertService` 统一转换，保持调用方式一致。

文件位置：`src/main/java/io/github/atengk/mapstruct/service/UserExampleService.java`

```java
package io.github.atengk.mapstruct.service;

import cn.hutool.core.date.LocalDateTimeUtil;
import io.github.atengk.mapstruct.dto.UserDTO;
import io.github.atengk.mapstruct.entity.UserEntity;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

/**
 * 用户转换示例服务.
 *
 * @author Ateng
 * @since 2026-05-06
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class UserExampleService {

    private final ObjectConvertService objectConvertService;

    public UserDTO queryUserDetail(Long userId) {
        UserEntity userEntity = new UserEntity()
                .setId(userId)
                .setUsername("ateng")
                .setNickname("阿腾")
                .setMobile("13800000000")
                .setEnabled(true)
                .setCreatedAt(LocalDateTimeUtil.now());

        UserDTO userDTO = objectConvertService.convert(userEntity, UserDTO.class);
        log.info("用户详情转换完成，用户ID：{}", userId);
        return userDTO;
    }

    public UserEntity buildUserEntity(UserDTO userDTO) {
        UserEntity userEntity = objectConvertService.convert(userDTO, UserEntity.class);
        log.info("用户DTO转换为实体完成，用户ID：{}", userEntity.getId());
        return userEntity;
    }

}
```

调用方式如下：

```java
UserDTO userDTO = userExampleService.queryUserDetail(1L);
UserEntity userEntity = userExampleService.buildUserEntity(userDTO);
```

转换结果中，`id`、`username`、`nickname`、`mobile`、`enabled`、`createdAt` 会按照同名同类型字段自动赋值。

### 集合对象转换

集合对象转换适用于列表查询、批量导出、批量处理等场景。为了避免各业务代码重复写空集合判断和 Stream 转换逻辑，建议通过统一转换服务中的 `convertList` 方法处理。

文件位置：`src/main/java/io/github/atengk/mapstruct/service/UserListExampleService.java`

```java
package io.github.atengk.mapstruct.service;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.date.LocalDateTimeUtil;
import io.github.atengk.mapstruct.dto.UserDTO;
import io.github.atengk.mapstruct.entity.UserEntity;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * 用户集合转换示例服务.
 *
 * @author Ateng
 * @since 2026-05-06
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class UserListExampleService {

    private final ObjectConvertService objectConvertService;

    public List<UserDTO> queryUserList() {
        List<UserEntity> userEntityList = CollUtil.newArrayList(
                new UserEntity()
                        .setId(1L)
                        .setUsername("ateng")
                        .setNickname("阿腾")
                        .setMobile("13800000001")
                        .setEnabled(true)
                        .setCreatedAt(LocalDateTimeUtil.now()),
                new UserEntity()
                        .setId(2L)
                        .setUsername("zhangsan")
                        .setNickname("张三")
                        .setMobile("13800000002")
                        .setEnabled(false)
                        .setCreatedAt(LocalDateTimeUtil.now())
        );

        List<UserDTO> userDTOList = objectConvertService.convertList(userEntityList, UserDTO.class);
        log.info("用户列表转换完成，数量：{}", userDTOList.size());
        return userDTOList;
    }

}
```

集合转换时需要注意两点：第一，源集合为空时应返回空集合，不建议返回 `null`；第二，集合中的单个元素如果可能为空，应在统一转换方法中进行过滤或明确处理，避免生成代码在业务调用链中出现空指针问题。

### 分页对象转换

分页对象转换适用于分页查询接口。实际项目中可能使用 MyBatis-Plus 的 `Page<T>`、Spring Data 的 `Page<T>`，或者自定义分页对象。核心思路一致：分页元数据保持不变，只转换 `records` 中的数据类型。

下面使用前面定义的 `PageResult<T>` 演示分页转换。

文件位置：`src/main/java/io/github/atengk/mapstruct/service/UserPageExampleService.java`

```java
package io.github.atengk.mapstruct.service;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.date.LocalDateTimeUtil;
import io.github.atengk.mapstruct.common.PageResult;
import io.github.atengk.mapstruct.dto.UserDTO;
import io.github.atengk.mapstruct.entity.UserEntity;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * 用户分页转换示例服务.
 *
 * @author Ateng
 * @since 2026-05-06
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class UserPageExampleService {

    private final ObjectConvertService objectConvertService;

    public PageResult<UserDTO> queryUserPage(Long current, Long size) {
        List<UserEntity> records = CollUtil.newArrayList(
                new UserEntity()
                        .setId(1L)
                        .setUsername("ateng")
                        .setNickname("阿腾")
                        .setMobile("13800000001")
                        .setEnabled(true)
                        .setCreatedAt(LocalDateTimeUtil.now()),
                new UserEntity()
                        .setId(2L)
                        .setUsername("lisi")
                        .setNickname("李四")
                        .setMobile("13800000002")
                        .setEnabled(true)
                        .setCreatedAt(LocalDateTimeUtil.now())
        );

        PageResult<UserEntity> entityPage = new PageResult<UserEntity>()
                .setCurrent(current)
                .setSize(size)
                .setTotal(2L)
                .setRecords(records);

        PageResult<UserDTO> dtoPage = objectConvertService.convertPage(entityPage, UserDTO.class);
        log.info("用户分页转换完成，当前页：{}，每页大小：{}，总数：{}", current, size, dtoPage.getTotal());
        return dtoPage;
    }

}
```

分页转换后的返回结构如下：

```json
{
  "current": 1,
  "size": 10,
  "total": 2,
  "records": [
    {
      "id": 1,
      "username": "ateng",
      "nickname": "阿腾",
      "mobile": "13800000001",
      "enabled": true,
      "createdAt": "2026-05-06T10:30:00"
    },
    {
      "id": 2,
      "username": "lisi",
      "nickname": "李四",
      "mobile": "13800000002",
      "enabled": true,
      "createdAt": "2026-05-06T10:30:00"
    }
  ]
}
```

验证方式如下：

```bash
# 编译项目，确认 MapStruct Plus 生成代码无异常
mvn clean compile

# 如果已经编写测试类，可以直接执行测试
mvn test
```

分页转换的重点是只转换业务数据列表，不改变分页元信息。`current`、`size`、`total` 等字段应直接沿用源分页对象，`records` 使用 MapStruct Plus 转换为目标类型集合。



## 常用注解使用

本节说明 MapStruct Plus 中常用注解的使用方式，重点覆盖对象映射声明、反向映射、字段名不一致映射和字段忽略。MapStruct Plus 的基础使用方式是给需要转换的类添加 `@AutoMapper`，然后通过 `Converter` 调用 `convert` 方法完成转换；框架会在编译期生成对应转换代码。([Libraries](https://libraries.io/maven/io.github.linpeilie%3Amapstruct-plus?utm_source=chatgpt.com))

### AutoMapper 注解

`@AutoMapper` 用于声明当前类可以转换为哪个目标类型。最常见的用法是在 Entity 上配置目标 DTO 或 VO，后续通过 `Converter` 完成转换。MapStruct Plus 官方说明其支持单个类配置多个类型转换，并且 `1.5.0` 版本中 `AutoMapper`、`AutoMapping`、`ReverseAutoMapping` 支持重复配置。([mapstruct.plus](https://www.mapstruct.plus/?utm_source=chatgpt.com))

文件结构如下：

```text
src/main/java/io/github/atengk/mapstruct/entity/UserEntity.java
src/main/java/io/github/atengk/mapstruct/dto/UserDTO.java
src/main/java/io/github/atengk/mapstruct/vo/UserVO.java
```

下面在用户实体类上配置两个目标类型，分别用于业务层传输和接口层返回。

文件位置：`src/main/java/io/github/atengk/mapstruct/entity/UserEntity.java`

```java
package io.github.atengk.mapstruct.entity;

import io.github.atengk.mapstruct.dto.UserDTO;
import io.github.atengk.mapstruct.vo.UserVO;
import io.github.linpeilie.annotations.AutoMapper;
import lombok.Data;
import lombok.experimental.Accessors;

import java.time.LocalDateTime;

/**
 * 用户实体对象.
 *
 * @author Ateng
 * @since 2026-05-06
 */
@Data
@Accessors(chain = true)
@AutoMapper(target = UserDTO.class)
@AutoMapper(target = UserVO.class)
public class UserEntity {

    private Long id;

    private String username;

    private String nickname;

    private String mobile;

    private Boolean enabled;

    private LocalDateTime createdAt;

}
```

文件位置：`src/main/java/io/github/atengk/mapstruct/dto/UserDTO.java`

```java
package io.github.atengk.mapstruct.dto;

import lombok.Data;
import lombok.experimental.Accessors;

import java.time.LocalDateTime;

/**
 * 用户数据传输对象.
 *
 * @author Ateng
 * @since 2026-05-06
 */
@Data
@Accessors(chain = true)
public class UserDTO {

    private Long id;

    private String username;

    private String nickname;

    private String mobile;

    private Boolean enabled;

    private LocalDateTime createdAt;

}
```

文件位置：`src/main/java/io/github/atengk/mapstruct/vo/UserVO.java`

```java
package io.github.atengk.mapstruct.vo;

import lombok.Data;
import lombok.experimental.Accessors;

/**
 * 用户接口返回对象.
 *
 * @author Ateng
 * @since 2026-05-06
 */
@Data
@Accessors(chain = true)
public class UserVO {

    private Long id;

    private String username;

    private String nickname;

    private String mobile;

}
```

调用方式如下：

```java
UserDTO userDTO = converter.convert(userEntity, UserDTO.class);
UserVO userVO = converter.convert(userEntity, UserVO.class);
```

`@AutoMapper` 适合字段结构相近的对象转换。如果目标类字段少于源类字段，一般不需要额外处理；如果目标类字段名称不同、类型不同或需要忽略，则需要配合 `@AutoMapping` 使用。

### ReverseAutoMapping 注解

`@ReverseAutoMapping` 用于配置反向转换规则。默认情况下，在一个类上添加 `@AutoMapper` 后，MapStruct Plus 会生成源类到目标类、目标类到源类的转换能力；但源类上配置的 `@AutoMapping` 自定义字段规则不会自动等价应用到反向转换中，因此需要通过 `@ReverseAutoMapping` 明确声明反向规则。公开教程中也强调：`@ReverseAutoMapping` 的 `source` 表示目标类中的属性，`target` 表示源类中的属性。([CSDN](https://blog.csdn.net/Lzp_csdn1/article/details/131798360?utm_source=chatgpt.com))

文件结构如下：

```text
src/main/java/io/github/atengk/mapstruct/entity/StudentEntity.java
src/main/java/io/github/atengk/mapstruct/dto/StudentDTO.java
```

下面在源类中同时配置正向和反向字段映射。

文件位置：`src/main/java/io/github/atengk/mapstruct/entity/StudentEntity.java`

```java
package io.github.atengk.mapstruct.entity;

import io.github.atengk.mapstruct.dto.StudentDTO;
import io.github.linpeilie.annotations.AutoMapper;
import io.github.linpeilie.annotations.AutoMapping;
import io.github.linpeilie.annotations.ReverseAutoMapping;
import lombok.Data;
import lombok.experimental.Accessors;

/**
 * 学生实体对象.
 *
 * @author Ateng
 * @since 2026-05-06
 */
@Data
@Accessors(chain = true)
@AutoMapper(target = StudentDTO.class)
public class StudentEntity {

    private Long id;

    @AutoMapping(source = "name", target = "studentName")
    @ReverseAutoMapping(source = "studentName", target = "name")
    private String name;

    @AutoMapping(source = "age", target = "studentAge")
    @ReverseAutoMapping(source = "studentAge", target = "age")
    private Integer age;

}
```

文件位置：`src/main/java/io/github/atengk/mapstruct/dto/StudentDTO.java`

```java
package io.github.atengk.mapstruct.dto;

import lombok.Data;
import lombok.experimental.Accessors;

/**
 * 学生数据传输对象.
 *
 * @author Ateng
 * @since 2026-05-06
 */
@Data
@Accessors(chain = true)
public class StudentDTO {

    private Long id;

    private String studentName;

    private Integer studentAge;

}
```

调用方式如下：

```java
StudentDTO studentDTO = converter.convert(studentEntity, StudentDTO.class);
StudentEntity newStudentEntity = converter.convert(studentDTO, StudentEntity.class);
```

转换方向如下：

```text
StudentEntity.name  -> StudentDTO.studentName
StudentDTO.studentName -> StudentEntity.name

StudentEntity.age   -> StudentDTO.studentAge
StudentDTO.studentAge  -> StudentEntity.age
```

如果目标类也可以修改，更推荐在目标类上单独添加 `@AutoMapper` 和对应的字段映射规则；如果目标类属于外部依赖、接口 SDK 或公共模型，不方便修改，则可以在源类中集中使用 `@ReverseAutoMapping`。

### 字段映射配置

字段映射配置用于处理源对象和目标对象字段名称不一致的情况。`@AutoMapping` 提供 `source` 和 `target` 属性，语义与 MapStruct 原生 `@Mapping` 接近；MapStruct 原生 `@Mapping` 也支持通过 `source` 指定源字段、通过 `target` 指定目标字段。([mapstruct.org](https://mapstruct.org/documentation/dev/api/org/mapstruct/Mapping.html?utm_source=chatgpt.com))

文件结构如下：

```text
src/main/java/io/github/atengk/mapstruct/entity/UserProfileEntity.java
src/main/java/io/github/atengk/mapstruct/dto/UserProfileDTO.java
```

下面将实体类中的 `mobile` 映射到 DTO 中的 `phoneNumber`，将 `createdAt` 映射到 `registerTime`。

文件位置：`src/main/java/io/github/atengk/mapstruct/entity/UserProfileEntity.java`

```java
package io.github.atengk.mapstruct.entity;

import io.github.atengk.mapstruct.dto.UserProfileDTO;
import io.github.linpeilie.annotations.AutoMapper;
import io.github.linpeilie.annotations.AutoMapping;
import lombok.Data;
import lombok.experimental.Accessors;

import java.time.LocalDateTime;

/**
 * 用户档案实体对象.
 *
 * @author Ateng
 * @since 2026-05-06
 */
@Data
@Accessors(chain = true)
@AutoMapper(target = UserProfileDTO.class)
public class UserProfileEntity {

    private Long id;

    private String username;

    @AutoMapping(source = "mobile", target = "phoneNumber")
    private String mobile;

    @AutoMapping(source = "createdAt", target = "registerTime")
    private LocalDateTime createdAt;

}
```

文件位置：`src/main/java/io/github/atengk/mapstruct/dto/UserProfileDTO.java`

```java
package io.github.atengk.mapstruct.dto;

import lombok.Data;
import lombok.experimental.Accessors;

import java.time.LocalDateTime;

/**
 * 用户档案数据传输对象.
 *
 * @author Ateng
 * @since 2026-05-06
 */
@Data
@Accessors(chain = true)
public class UserProfileDTO {

    private Long id;

    private String username;

    private String phoneNumber;

    private LocalDateTime registerTime;

}
```

字段映射后的效果如下：

```text
UserProfileEntity.mobile    -> UserProfileDTO.phoneNumber
UserProfileEntity.createdAt -> UserProfileDTO.registerTime
```

字段名称不一致时，应优先通过注解明确声明映射关系，不建议依赖中间转换逻辑或手动补字段，否则后续字段改名时不容易定位问题。

### 忽略字段配置

忽略字段配置用于处理敏感字段、系统字段、审计字段和不希望透出的字段。例如密码、盐值、删除标记、内部备注等字段不应该出现在 DTO 或 VO 中。MapStruct 原生 `@Mapping` 提供 `ignore` 属性，用于声明目标字段不参与映射；MapStruct Plus 的 `@AutoMapping` 也可以通过 `ignore = true` 忽略字段。([mapstruct.org](https://mapstruct.org/documentation/dev/api/org/mapstruct/Mapping.html?utm_source=chatgpt.com))

文件结构如下：

```text
src/main/java/io/github/atengk/mapstruct/entity/SystemUserEntity.java
src/main/java/io/github/atengk/mapstruct/vo/SystemUserVO.java
```

下面在实体类中忽略密码字段和删除标记字段，避免接口返回时暴露内部数据。

文件位置：`src/main/java/io/github/atengk/mapstruct/entity/SystemUserEntity.java`

```java
package io.github.atengk.mapstruct.entity;

import io.github.atengk.mapstruct.vo.SystemUserVO;
import io.github.linpeilie.annotations.AutoMapper;
import io.github.linpeilie.annotations.AutoMapping;
import lombok.Data;
import lombok.experimental.Accessors;

/**
 * 系统用户实体对象.
 *
 * @author Ateng
 * @since 2026-05-06
 */
@Data
@Accessors(chain = true)
@AutoMapper(target = SystemUserVO.class)
public class SystemUserEntity {

    private Long id;

    private String username;

    private String nickname;

    @AutoMapping(target = "password", ignore = true)
    private String password;

    @AutoMapping(target = "deleted", ignore = true)
    private Boolean deleted;

}
```

文件位置：`src/main/java/io/github/atengk/mapstruct/vo/SystemUserVO.java`

```java
package io.github.atengk.mapstruct.vo;

import lombok.Data;
import lombok.experimental.Accessors;

/**
 * 系统用户接口返回对象.
 *
 * @author Ateng
 * @since 2026-05-06
 */
@Data
@Accessors(chain = true)
public class SystemUserVO {

    private Long id;

    private String username;

    private String nickname;

    private String password;

    private Boolean deleted;

}
```

转换后，`password` 和 `deleted` 字段不会从源对象赋值到目标对象。实际项目中更推荐直接从 VO 中删除敏感字段；如果目标类来自公共模型、历史接口或兼容旧结构，才使用 `ignore = true` 阻断赋值。

## 高级转换场景

本节说明嵌套对象、枚举字段、日期时间和自定义类型转换等复杂场景。MapStruct Plus 基于 MapStruct，底层仍使用编译期生成的类型安全代码；MapStruct 本身支持自定义 Mapper、`uses`、字段映射、限定方法等能力，因此复杂场景可以在 MapStruct Plus 注解方式和 MapStruct 原生 Mapper 方式之间组合使用。([mapstruct.org](https://mapstruct.org/documentation/1.5/api/org/mapstruct/Mapper.html?utm_source=chatgpt.com))

### 嵌套对象转换

嵌套对象转换适用于对象中包含另一个对象的情况，例如用户对象中包含部门对象、订单对象中包含收货地址对象。只要嵌套对象之间也存在转换关系，MapStruct Plus 生成转换代码时就可以继续调用对应的嵌套转换逻辑。

文件结构如下：

```text
src/main/java/io/github/atengk/mapstruct/entity/DepartmentEntity.java
src/main/java/io/github/atengk/mapstruct/dto/DepartmentDTO.java
src/main/java/io/github/atengk/mapstruct/entity/UserDetailEntity.java
src/main/java/io/github/atengk/mapstruct/dto/UserDetailDTO.java
```

下面先定义部门实体和部门 DTO。

文件位置：`src/main/java/io/github/atengk/mapstruct/entity/DepartmentEntity.java`

```java
package io.github.atengk.mapstruct.entity;

import io.github.atengk.mapstruct.dto.DepartmentDTO;
import io.github.linpeilie.annotations.AutoMapper;
import lombok.Data;
import lombok.experimental.Accessors;

/**
 * 部门实体对象.
 *
 * @author Ateng
 * @since 2026-05-06
 */
@Data
@Accessors(chain = true)
@AutoMapper(target = DepartmentDTO.class)
public class DepartmentEntity {

    private Long id;

    private String deptCode;

    private String deptName;

}
```

文件位置：`src/main/java/io/github/atengk/mapstruct/dto/DepartmentDTO.java`

```java
package io.github.atengk.mapstruct.dto;

import lombok.Data;
import lombok.experimental.Accessors;

/**
 * 部门数据传输对象.
 *
 * @author Ateng
 * @since 2026-05-06
 */
@Data
@Accessors(chain = true)
public class DepartmentDTO {

    private Long id;

    private String deptCode;

    private String deptName;

}
```

下面定义用户详情对象，用户中包含部门对象。

文件位置：`src/main/java/io/github/atengk/mapstruct/entity/UserDetailEntity.java`

```java
package io.github.atengk.mapstruct.entity;

import io.github.atengk.mapstruct.dto.UserDetailDTO;
import io.github.linpeilie.annotations.AutoMapper;
import lombok.Data;
import lombok.experimental.Accessors;

/**
 * 用户详情实体对象.
 *
 * @author Ateng
 * @since 2026-05-06
 */
@Data
@Accessors(chain = true)
@AutoMapper(target = UserDetailDTO.class)
public class UserDetailEntity {

    private Long id;

    private String username;

    private String nickname;

    private DepartmentEntity department;

}
```

文件位置：`src/main/java/io/github/atengk/mapstruct/dto/UserDetailDTO.java`

```java
package io.github.atengk.mapstruct.dto;

import lombok.Data;
import lombok.experimental.Accessors;

/**
 * 用户详情数据传输对象.
 *
 * @author Ateng
 * @since 2026-05-06
 */
@Data
@Accessors(chain = true)
public class UserDetailDTO {

    private Long id;

    private String username;

    private String nickname;

    private DepartmentDTO department;

}
```

调用方式如下：

```java
UserDetailDTO userDetailDTO = converter.convert(userDetailEntity, UserDetailDTO.class);
```

转换效果如下：

```text
UserDetailEntity.department -> UserDetailDTO.department
DepartmentEntity            -> DepartmentDTO
```

如果对象存在父子互相引用，例如树形节点中同时存在 `parent` 和 `children`，普通递归转换可能导致栈溢出。MapStruct Plus `1.4.0` 开始提供 `cycleAvoiding` 属性用于处理循环嵌套对象转换，启用后会在转换链路中维护上下文对象，避免重复转换同一个对象。([cnblogs.com](https://www.cnblogs.com/linpeilie/p/18077972?utm_source=chatgpt.com))

```java
@AutoMapper(target = TreeNodeDTO.class, cycleAvoiding = true)
public class TreeNodeEntity {
    private TreeNodeEntity parent;
    private List<TreeNodeEntity> children;
}
```

循环嵌套只在确实存在双向引用或复杂对象图时开启。普通单向嵌套对象不需要启用，否则会增加生成代码复杂度。

### 枚举字段转换

枚举字段转换常见于状态字段、类型字段、来源字段等场景。最简单的情况是源对象和目标对象使用同一个枚举类型，此时可以直接按同名字段转换；如果目标对象需要展示枚举编码或枚举描述，则需要通过字段映射或自定义转换方法处理。

文件结构如下：

```text
src/main/java/io/github/atengk/mapstruct/enums/UserStatus.java
src/main/java/io/github/atengk/mapstruct/entity/UserStatusEntity.java
src/main/java/io/github/atengk/mapstruct/dto/UserStatusDTO.java
```

下面定义用户状态枚举。

文件位置：`src/main/java/io/github/atengk/mapstruct/enums/UserStatus.java`

```java
package io.github.atengk.mapstruct.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * 用户状态枚举.
 *
 * @author Ateng
 * @since 2026-05-06
 */
@Getter
@AllArgsConstructor
public enum UserStatus {

    ENABLED("enabled", "启用"),

    DISABLED("disabled", "禁用");

    private final String code;

    private final String name;

}
```

下面将枚举字段拆分为状态编码和状态名称。

文件位置：`src/main/java/io/github/atengk/mapstruct/entity/UserStatusEntity.java`

```java
package io.github.atengk.mapstruct.entity;

import io.github.atengk.mapstruct.dto.UserStatusDTO;
import io.github.atengk.mapstruct.enums.UserStatus;
import io.github.linpeilie.annotations.AutoMapper;
import io.github.linpeilie.annotations.AutoMapping;
import lombok.Data;
import lombok.experimental.Accessors;

/**
 * 用户状态实体对象.
 *
 * @author Ateng
 * @since 2026-05-06
 */
@Data
@Accessors(chain = true)
@AutoMapper(target = UserStatusDTO.class)
public class UserStatusEntity {

    private Long id;

    private String username;

    @AutoMapping(source = "status.code", target = "statusCode")
    @AutoMapping(source = "status.name", target = "statusName")
    private UserStatus status;

}
```

文件位置：`src/main/java/io/github/atengk/mapstruct/dto/UserStatusDTO.java`

```java
package io.github.atengk.mapstruct.dto;

import lombok.Data;
import lombok.experimental.Accessors;

/**
 * 用户状态数据传输对象.
 *
 * @author Ateng
 * @since 2026-05-06
 */
@Data
@Accessors(chain = true)
public class UserStatusDTO {

    private Long id;

    private String username;

    private String statusCode;

    private String statusName;

}
```

转换效果如下：

```json
{
  "id": 1,
  "username": "ateng",
  "statusCode": "enabled",
  "statusName": "启用"
}
```

如果需要从 `statusCode` 反向还原为 `UserStatus` 枚举，不建议仅靠字段名称自动推断。建议定义明确的转换方法，或者在业务层根据编码查找枚举，避免非法编码导致转换异常。

### 日期时间转换

日期时间转换常见于 `LocalDateTime`、`Date`、时间戳、字符串之间的转换。后端内部对象建议优先使用 `LocalDateTime`，接口返回格式可以交给 Jackson 全局时间格式化处理；只有在 DTO 中明确需要字符串字段时，才建议在 Mapper 中进行格式化转换。MapStruct 支持使用自定义 mapper 和 `uses` 引入额外转换逻辑。([mapstruct.org](https://mapstruct.org/documentation/1.5/api/org/mapstruct/Mapper.html?utm_source=chatgpt.com))

文件结构如下：

```text
src/main/java/io/github/atengk/mapstruct/entity/UserTimeEntity.java
src/main/java/io/github/atengk/mapstruct/dto/UserTimeDTO.java
src/main/java/io/github/atengk/mapstruct/mapper/UserTimeMapper.java
```

下面定义时间字段源对象和目标对象。

文件位置：`src/main/java/io/github/atengk/mapstruct/entity/UserTimeEntity.java`

```java
package io.github.atengk.mapstruct.entity;

import lombok.Data;
import lombok.experimental.Accessors;

import java.time.LocalDateTime;

/**
 * 用户时间实体对象.
 *
 * @author Ateng
 * @since 2026-05-06
 */
@Data
@Accessors(chain = true)
public class UserTimeEntity {

    private Long id;

    private String username;

    private LocalDateTime createdAt;

}
```

文件位置：`src/main/java/io/github/atengk/mapstruct/dto/UserTimeDTO.java`

```java
package io.github.atengk.mapstruct.dto;

import lombok.Data;
import lombok.experimental.Accessors;

/**
 * 用户时间数据传输对象.
 *
 * @author Ateng
 * @since 2026-05-06
 */
@Data
@Accessors(chain = true)
public class UserTimeDTO {

    private Long id;

    private String username;

    private String createdAtText;

}
```

对于格式化逻辑较明确的场景，可以直接定义 MapStruct Mapper，并继承 MapStruct Plus 的 `BaseMapper`，这样仍然可以作为项目统一转换体系的一部分使用。

文件位置：`src/main/java/io/github/atengk/mapstruct/mapper/UserTimeMapper.java`

```java
package io.github.atengk.mapstruct.mapper;

import cn.hutool.core.date.DatePattern;
import cn.hutool.core.date.LocalDateTimeUtil;
import cn.hutool.core.util.ObjectUtil;
import io.github.atengk.mapstruct.dto.UserTimeDTO;
import io.github.atengk.mapstruct.entity.UserTimeEntity;
import io.github.linpeilie.BaseMapper;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingConstants;
import org.mapstruct.Named;

import java.time.LocalDateTime;

/**
 * 用户时间转换 Mapper.
 *
 * @author Ateng
 * @since 2026-05-06
 */
@Mapper(componentModel = MappingConstants.ComponentModel.SPRING)
public interface UserTimeMapper extends BaseMapper<UserTimeEntity, UserTimeDTO> {

    /**
     * 转换用户时间对象.
     *
     * @param source 用户时间实体对象
     * @return 用户时间数据传输对象
     */
    @Override
    @Mapping(target = "createdAtText", source = "createdAt", qualifiedByName = "formatDateTime")
    UserTimeDTO convert(UserTimeEntity source);

    /**
     * 格式化日期时间.
     *
     * @param value 日期时间
     * @return 格式化后的日期时间字符串
     */
    @Named("formatDateTime")
    default String formatDateTime(LocalDateTime value) {
        if (ObjectUtil.isNull(value)) {
            return null;
        }
        return LocalDateTimeUtil.format(value, DatePattern.NORM_DATETIME_FORMATTER);
    }

}
```

调用方式如下：

```java
UserTimeDTO userTimeDTO = userTimeMapper.convert(userTimeEntity);
```

如果项目接口时间格式全局统一，推荐优先配置 Jackson：

```yaml
spring:
  jackson:
    # 统一接口 JSON 时间格式
    date-format: yyyy-MM-dd HH:mm:ss
    time-zone: Asia/Shanghai
```

对象转换层只负责对象结构转换，不建议承担过多展示格式逻辑。只有当一个 DTO 中同时存在多个不同时间展示格式时，才建议在 Mapper 中进行精细化格式转换。

### 自定义类型转换

自定义类型转换适用于字段类型不一致且无法通过简单字段映射解决的场景，例如金额分转元、枚举编码转枚举对象、JSON 字符串转对象、数据库字典值转展示文本等。MapStruct 原生支持通过 `uses` 引入其他 Mapper 或转换类，生成的 Mapper 会注入并调用这些转换逻辑。([mapstruct.org](https://mapstruct.org/documentation/1.5/api/org/mapstruct/Mapper.html?utm_source=chatgpt.com))

文件结构如下：

```text
src/main/java/io/github/atengk/mapstruct/entity/OrderEntity.java
src/main/java/io/github/atengk/mapstruct/dto/OrderDTO.java
src/main/java/io/github/atengk/mapstruct/mapper/MoneyConvertMapper.java
src/main/java/io/github/atengk/mapstruct/mapper/OrderMapper.java
```

下面定义订单实体，金额以分为单位存储。

文件位置：`src/main/java/io/github/atengk/mapstruct/entity/OrderEntity.java`

```java
package io.github.atengk.mapstruct.entity;

import lombok.Data;
import lombok.experimental.Accessors;

/**
 * 订单实体对象.
 *
 * @author Ateng
 * @since 2026-05-06
 */
@Data
@Accessors(chain = true)
public class OrderEntity {

    private Long id;

    private String orderNo;

    private Long amountCent;

}
```

文件位置：`src/main/java/io/github/atengk/mapstruct/dto/OrderDTO.java`

```java
package io.github.atengk.mapstruct.dto;

import lombok.Data;
import lombok.experimental.Accessors;

/**
 * 订单数据传输对象.
 *
 * @author Ateng
 * @since 2026-05-06
 */
@Data
@Accessors(chain = true)
public class OrderDTO {

    private Long id;

    private String orderNo;

    private String amountText;

}
```

下面定义金额转换 Mapper，使用 Hutool 处理空值和金额计算。

文件位置：`src/main/java/io/github/atengk/mapstruct/mapper/MoneyConvertMapper.java`

```java
package io.github.atengk.mapstruct.mapper;

import cn.hutool.core.util.NumberUtil;
import cn.hutool.core.util.ObjectUtil;
import org.mapstruct.Named;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;

/**
 * 金额转换 Mapper.
 *
 * @author Ateng
 * @since 2026-05-06
 */
@Component
public class MoneyConvertMapper {

    /**
     * 分转元展示文本.
     *
     * @param amountCent 金额，单位：分
     * @return 金额文本，单位：元
     */
    @Named("centToYuanText")
    public String centToYuanText(Long amountCent) {
        if (ObjectUtil.isNull(amountCent)) {
            return null;
        }
        BigDecimal yuan = NumberUtil.div(amountCent, 100, 2);
        return NumberUtil.toStr(yuan);
    }

}
```

下面定义订单转换 Mapper，通过 `uses` 引入金额转换逻辑。

文件位置：`src/main/java/io/github/atengk/mapstruct/mapper/OrderMapper.java`

```java
package io.github.atengk.mapstruct.mapper;

import io.github.atengk.mapstruct.dto.OrderDTO;
import io.github.atengk.mapstruct.entity.OrderEntity;
import io.github.linpeilie.BaseMapper;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingConstants;

/**
 * 订单转换 Mapper.
 *
 * @author Ateng
 * @since 2026-05-06
 */
@Mapper(
        componentModel = MappingConstants.ComponentModel.SPRING,
        uses = MoneyConvertMapper.class
)
public interface OrderMapper extends BaseMapper<OrderEntity, OrderDTO> {

    /**
     * 转换订单对象.
     *
     * @param source 订单实体对象
     * @return 订单数据传输对象
     */
    @Override
    @Mapping(target = "amountText", source = "amountCent", qualifiedByName = "centToYuanText")
    OrderDTO convert(OrderEntity source);

}
```

调用方式如下：

```java
OrderDTO orderDTO = orderMapper.convert(orderEntity);
```

转换结果示例：

```json
{
  "id": 1001,
  "orderNo": "NO202605060001",
  "amountText": "128.88"
}
```

自定义类型转换建议遵循以下规则：

| 场景                     | 建议                                              |
| ------------------------ | ------------------------------------------------- |
| 字段名不同但类型相同     | 使用 `@AutoMapping(source, target)`               |
| 字段类型不同但规则简单   | 使用 MapStruct Mapper 的 `@Mapping` 配合 `@Named` |
| 多个对象复用同一转换规则 | 抽取独立转换类，通过 `uses` 引入                  |
| 涉及数据库查询或远程调用 | 不建议放在 Mapper 中，建议在 Service 层处理       |
| 涉及金额、时间、枚举     | 保持规则集中，避免多个 Mapper 重复实现            |

验证方式如下：

```bash
# 编译并生成 MapStruct Plus / MapStruct 转换代码
mvn clean compile

# 查看生成代码，确认自定义转换方法是否被调用
ls target/generated-sources/annotations
```

复杂转换规则应尽量保持“可编译期检查、可单元测试、无副作用”。Mapper 层只做结构转换和轻量格式处理，不建议包含数据库访问、缓存读写、接口调用等业务逻辑。



## 与 Spring Boot 3 集成

本节说明 MapStruct Plus 在 Spring Boot 3 项目中的集成方式。实际开发中建议将对象转换能力集中放在 Service 层或公共转换服务中，Controller 只负责接收请求和返回结果，避免在接口层堆积转换细节。

### Bean 注入方式

在 Spring Boot 3 项目中引入 `mapstruct-plus-spring-boot-starter` 后，可以直接注入 `Converter` 使用。`Converter` 是 MapStruct Plus 提供的统一转换入口，适合处理大多数单对象、集合对象和基础类型转换场景。

文件位置：`src/main/java/io/github/atengk/mapstruct/service/UserConvertDemoService.java`

```java
package io.github.atengk.mapstruct.service;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.ObjectUtil;
import io.github.atengk.mapstruct.dto.UserDTO;
import io.github.atengk.mapstruct.entity.UserEntity;
import io.github.linpeilie.Converter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * 用户转换演示服务.
 *
 * @author Ateng
 * @since 2026-05-06
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class UserConvertDemoService {

    private final Converter converter;

    public UserDTO convertOne(UserEntity userEntity) {
        if (ObjectUtil.isNull(userEntity)) {
            log.debug("用户单对象转换跳过，源对象为空");
            return null;
        }
        return converter.convert(userEntity, UserDTO.class);
    }

    public List<UserDTO> convertList(List<UserEntity> userEntityList) {
        if (CollUtil.isEmpty(userEntityList)) {
            log.debug("用户集合转换跳过，源集合为空");
            return CollUtil.newArrayList();
        }
        return converter.convert(userEntityList, UserDTO.class);
    }

}
```

如果项目中已经封装了 `ObjectConvertService`，更推荐业务代码注入封装后的服务，而不是到处直接注入 `Converter`。这样后续可以统一处理空值、集合、分页、日志和异常信息。

```java
private final ObjectConvertService objectConvertService;
```

两种方式对比如下：

| 注入方式                    | 适用场景                                               |
| --------------------------- | ------------------------------------------------------ |
| 直接注入 `Converter`        | 简单项目、示例项目、转换逻辑较少                       |
| 注入 `ObjectConvertService` | 中大型项目、需要统一空值处理、分页转换、日志规范       |
| 注入自定义 `Mapper`         | 字段规则复杂、需要 `@Mapping`、`@Named`、`uses` 等能力 |

### Service 层调用方式

Service 层是对象转换最常见的位置。一般建议在 Service 中完成 Entity、DTO、VO、请求参数对象之间的转换，Controller 不直接操作 Entity，也不直接暴露数据库对象。

典型调用流程如下：

```text
Controller 入参
    -> Service
        -> Param 转 Entity
        -> 执行业务逻辑
        -> Entity 转 VO
    -> Controller 返回 VO
```

文件位置：`src/main/java/io/github/atengk/mapstruct/service/UserApplicationService.java`

```java
package io.github.atengk.mapstruct.service;

import cn.hutool.core.lang.Assert;
import cn.hutool.core.util.ObjectUtil;
import cn.hutool.core.util.StrUtil;
import io.github.atengk.mapstruct.entity.UserEntity;
import io.github.atengk.mapstruct.param.UserCreateParam;
import io.github.atengk.mapstruct.param.UserUpdateParam;
import io.github.atengk.mapstruct.vo.UserInfoVO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

/**
 * 用户应用服务.
 *
 * @author Ateng
 * @since 2026-05-06
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class UserApplicationService {

    private final ObjectConvertService objectConvertService;

    public UserInfoVO createUser(UserCreateParam param) {
        Assert.notNull(param, "新增用户参数不能为空");
        Assert.isTrue(StrUtil.isNotBlank(param.getUsername()), "用户名不能为空");

        UserEntity userEntity = objectConvertService.convert(param, UserEntity.class);
        userEntity.setEnabled(true);
        userEntity.setCreatedAt(LocalDateTime.now());
        userEntity.setUpdatedAt(LocalDateTime.now());

        log.info("新增用户转换完成，用户名：{}", userEntity.getUsername());

        return objectConvertService.convert(userEntity, UserInfoVO.class);
    }

    public UserInfoVO updateUser(UserUpdateParam param) {
        Assert.notNull(param, "修改用户参数不能为空");
        Assert.notNull(param.getId(), "用户ID不能为空");

        UserEntity userEntity = objectConvertService.convert(param, UserEntity.class);
        userEntity.setUpdatedAt(LocalDateTime.now());

        if (ObjectUtil.isNull(userEntity.getEnabled())) {
            userEntity.setEnabled(true);
        }

        log.info("修改用户转换完成，用户ID：{}", userEntity.getId());

        return objectConvertService.convert(userEntity, UserInfoVO.class);
    }

}
```

Service 层转换建议遵循以下原则：

| 原则                           | 说明                                            |
| ------------------------------ | ----------------------------------------------- |
| Controller 不返回 Entity       | 避免数据库字段、内部字段泄露                    |
| Service 内完成 Param 转 Entity | 业务层掌握入参到领域对象的构造过程              |
| 查询结果统一转 VO              | 接口返回结构保持稳定                            |
| 复杂字段不要硬塞给 Mapper      | 涉及数据库、缓存、远程调用的逻辑放在 Service 中 |
| 空值和集合统一封装             | 避免每个业务方法重复判断                        |

### Controller 层返回对象转换

Controller 层可以调用 Service 返回已经转换好的 VO，也可以在少数简单接口中进行轻量转换。更推荐第一种方式：Controller 只处理 HTTP 请求、响应包装和参数校验，具体转换由 Service 完成。

文件结构如下：

```text
src/main/java/io/github/atengk/mapstruct/common/ApiResult.java
src/main/java/io/github/atengk/mapstruct/controller/UserController.java
```

下面定义统一接口返回对象。

文件位置：`src/main/java/io/github/atengk/mapstruct/common/ApiResult.java`

```java
package io.github.atengk.mapstruct.common;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 接口统一返回结果.
 *
 * @author Ateng
 * @since 2026-05-06
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ApiResult<T> {

    private Integer code;

    private String message;

    private T data;

    public static <T> ApiResult<T> ok(T data) {
        return new ApiResult<>(200, "操作成功", data);
    }

    public static <T> ApiResult<T> fail(String message) {
        return new ApiResult<>(500, message, null);
    }

}
```

下面定义用户 Controller，接口层只调用 Service，不直接处理转换细节。

文件位置：`src/main/java/io/github/atengk/mapstruct/controller/UserController.java`

```java
package io.github.atengk.mapstruct.controller;

import io.github.atengk.mapstruct.common.ApiResult;
import io.github.atengk.mapstruct.param.UserCreateParam;
import io.github.atengk.mapstruct.param.UserUpdateParam;
import io.github.atengk.mapstruct.service.UserInfoService;
import io.github.atengk.mapstruct.vo.UserInfoVO;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 用户接口控制器.
 *
 * @author Ateng
 * @since 2026-05-06
 */
@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
public class UserController {

    private final UserInfoService userInfoService;

    @PostMapping
    public ApiResult<UserInfoVO> createUser(@RequestBody UserCreateParam param) {
        return ApiResult.ok(userInfoService.createUser(param));
    }

    @PutMapping("/{id}")
    public ApiResult<UserInfoVO> updateUser(@PathVariable Long id, @RequestBody UserUpdateParam param) {
        param.setId(id);
        return ApiResult.ok(userInfoService.updateUser(param));
    }

    @GetMapping("/{id}")
    public ApiResult<UserInfoVO> getUser(@PathVariable Long id) {
        return ApiResult.ok(userInfoService.getUser(id));
    }

    @GetMapping
    public ApiResult<List<UserInfoVO>> listUsers() {
        return ApiResult.ok(userInfoService.listUsers());
    }

}
```

接口调用示例：

```bash
# 新增用户
curl -X POST "http://localhost:8080/api/users" \
  -H "Content-Type: application/json" \
  -d '{
    "username": "ateng",
    "nickname": "阿腾",
    "mobile": "13800000000"
  }'

# 修改用户
curl -X PUT "http://localhost:8080/api/users/1" \
  -H "Content-Type: application/json" \
  -d '{
    "nickname": "阿腾-修改",
    "mobile": "13900000000",
    "enabled": true
  }'

# 查询详情
curl -X GET "http://localhost:8080/api/users/1"

# 查询列表
curl -X GET "http://localhost:8080/api/users"
```

Controller 层不要直接返回 `UserEntity`。如果后续数据库表新增 `password`、`deleted`、`tenantId`、`internalRemark` 等字段，直接返回 Entity 容易导致敏感字段泄露。

## 实战开发示例

本节给出一个可直接放入 Spring Boot 3 项目的用户信息转换示例。为了聚焦 MapStruct Plus 的使用方式，示例使用内存 `Map` 模拟数据库存储；实际项目中可以替换为 MyBatis-Plus Mapper、JPA Repository 或其他持久层组件。

### 用户信息转换示例

用户信息转换示例包含三类对象：`UserEntity` 表示数据库实体，`UserCreateParam` 和 `UserUpdateParam` 表示接口入参，`UserInfoVO` 表示接口返回对象。转换关系由 `@AutoMapper` 声明。

文件结构如下：

```text
src/main/java/io/github/atengk/mapstruct/entity/UserEntity.java
src/main/java/io/github/atengk/mapstruct/param/UserCreateParam.java
src/main/java/io/github/atengk/mapstruct/param/UserUpdateParam.java
src/main/java/io/github/atengk/mapstruct/vo/UserInfoVO.java
```

下面定义用户实体对象。

文件位置：`src/main/java/io/github/atengk/mapstruct/entity/UserEntity.java`

```java
package io.github.atengk.mapstruct.entity;

import io.github.atengk.mapstruct.vo.UserInfoVO;
import io.github.linpeilie.annotations.AutoMapper;
import lombok.Data;
import lombok.experimental.Accessors;

import java.time.LocalDateTime;

/**
 * 用户实体对象.
 *
 * @author Ateng
 * @since 2026-05-06
 */
@Data
@Accessors(chain = true)
@AutoMapper(target = UserInfoVO.class)
public class UserEntity {

    private Long id;

    private String username;

    private String nickname;

    private String mobile;

    private Boolean enabled;

    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;

}
```

下面定义用户接口返回对象。

文件位置：`src/main/java/io/github/atengk/mapstruct/vo/UserInfoVO.java`

```java
package io.github.atengk.mapstruct.vo;

import lombok.Data;
import lombok.experimental.Accessors;

import java.time.LocalDateTime;

/**
 * 用户信息返回对象.
 *
 * @author Ateng
 * @since 2026-05-06
 */
@Data
@Accessors(chain = true)
public class UserInfoVO {

    private Long id;

    private String username;

    private String nickname;

    private String mobile;

    private Boolean enabled;

    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;

}
```

由于 `UserEntity` 和 `UserInfoVO` 字段名称、字段类型一致，MapStruct Plus 可以自动完成转换：

```java
UserInfoVO userInfoVO = objectConvertService.convert(userEntity, UserInfoVO.class);
```

该转换适合详情查询、列表查询、分页查询等读操作场景。

### 新增与修改参数转换示例

新增和修改参数通常不应该直接复用 Entity。新增参数一般不包含 `id`、`createdAt`、`updatedAt` 等系统字段；修改参数通常包含 `id`，但不允许客户端直接控制创建时间等字段。

下面定义新增用户参数。

文件位置：`src/main/java/io/github/atengk/mapstruct/param/UserCreateParam.java`

```java
package io.github.atengk.mapstruct.param;

import io.github.atengk.mapstruct.entity.UserEntity;
import io.github.linpeilie.annotations.AutoMapper;
import lombok.Data;
import lombok.experimental.Accessors;

/**
 * 新增用户参数.
 *
 * @author Ateng
 * @since 2026-05-06
 */
@Data
@Accessors(chain = true)
@AutoMapper(target = UserEntity.class)
public class UserCreateParam {

    private String username;

    private String nickname;

    private String mobile;

}
```

下面定义修改用户参数。

文件位置：`src/main/java/io/github/atengk/mapstruct/param/UserUpdateParam.java`

```java
package io.github.atengk.mapstruct.param;

import io.github.atengk.mapstruct.entity.UserEntity;
import io.github.linpeilie.annotations.AutoMapper;
import lombok.Data;
import lombok.experimental.Accessors;

/**
 * 修改用户参数.
 *
 * @author Ateng
 * @since 2026-05-06
 */
@Data
@Accessors(chain = true)
@AutoMapper(target = UserEntity.class)
public class UserUpdateParam {

    private Long id;

    private String nickname;

    private String mobile;

    private Boolean enabled;

}
```

下面定义用户业务服务，演示新增、修改、查询详情和查询列表中的对象转换。

文件位置：`src/main/java/io/github/atengk/mapstruct/service/UserInfoService.java`

```java
package io.github.atengk.mapstruct.service;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.lang.Assert;
import cn.hutool.core.map.MapUtil;
import cn.hutool.core.util.ObjectUtil;
import cn.hutool.core.util.StrUtil;
import io.github.atengk.mapstruct.entity.UserEntity;
import io.github.atengk.mapstruct.param.UserCreateParam;
import io.github.atengk.mapstruct.param.UserUpdateParam;
import io.github.atengk.mapstruct.vo.UserInfoVO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

/**
 * 用户信息服务.
 *
 * @author Ateng
 * @since 2026-05-06
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class UserInfoService {

    private final ObjectConvertService objectConvertService;

    private final AtomicLong idGenerator = new AtomicLong(0);

    private final Map<Long, UserEntity> userStore = new ConcurrentHashMap<>();

    public UserInfoVO createUser(UserCreateParam param) {
        Assert.notNull(param, "新增用户参数不能为空");
        Assert.isTrue(StrUtil.isNotBlank(param.getUsername()), "用户名不能为空");
        Assert.isTrue(StrUtil.isNotBlank(param.getMobile()), "手机号不能为空");

        UserEntity userEntity = objectConvertService.convert(param, UserEntity.class);
        LocalDateTime now = LocalDateTime.now();

        userEntity.setId(idGenerator.incrementAndGet());
        userEntity.setEnabled(true);
        userEntity.setCreatedAt(now);
        userEntity.setUpdatedAt(now);

        userStore.put(userEntity.getId(), userEntity);
        log.info("新增用户成功，用户ID：{}，用户名：{}", userEntity.getId(), userEntity.getUsername());

        return objectConvertService.convert(userEntity, UserInfoVO.class);
    }

    public UserInfoVO updateUser(UserUpdateParam param) {
        Assert.notNull(param, "修改用户参数不能为空");
        Assert.notNull(param.getId(), "用户ID不能为空");

        UserEntity oldEntity = userStore.get(param.getId());
        Assert.notNull(oldEntity, "用户不存在");

        UserEntity updateEntity = objectConvertService.convert(param, UserEntity.class);

        if (StrUtil.isNotBlank(updateEntity.getNickname())) {
            oldEntity.setNickname(updateEntity.getNickname());
        }
        if (StrUtil.isNotBlank(updateEntity.getMobile())) {
            oldEntity.setMobile(updateEntity.getMobile());
        }
        if (ObjectUtil.isNotNull(updateEntity.getEnabled())) {
            oldEntity.setEnabled(updateEntity.getEnabled());
        }

        oldEntity.setUpdatedAt(LocalDateTime.now());
        userStore.put(oldEntity.getId(), oldEntity);

        log.info("修改用户成功，用户ID：{}", oldEntity.getId());

        return objectConvertService.convert(oldEntity, UserInfoVO.class);
    }

    public UserInfoVO getUser(Long id) {
        Assert.notNull(id, "用户ID不能为空");

        UserEntity userEntity = userStore.get(id);
        Assert.notNull(userEntity, "用户不存在");

        log.info("查询用户详情，用户ID：{}", id);
        return objectConvertService.convert(userEntity, UserInfoVO.class);
    }

    public List<UserInfoVO> listUsers() {
        if (MapUtil.isEmpty(userStore)) {
            log.info("查询用户列表，当前无用户数据");
            return CollUtil.newArrayList();
        }

        List<UserEntity> userEntityList = userStore.values()
                .stream()
                .sorted(Comparator.comparing(UserEntity::getId))
                .toList();

        log.info("查询用户列表，数量：{}", userEntityList.size());
        return objectConvertService.convertList(userEntityList, UserInfoVO.class);
    }

}
```

新增用户的转换链路如下：

```text
UserCreateParam -> UserEntity -> UserInfoVO
```

修改用户的转换链路如下：

```text
UserUpdateParam -> UserEntity -> 合并旧数据 -> UserInfoVO
```

这里需要注意，修改操作不能简单地把 `UserUpdateParam` 直接转换成 Entity 后覆盖旧对象，否则可能把未传字段覆盖为 `null`。实际项目中可以根据业务语义选择“全量更新”或“非空字段更新”。

### 查询结果转换示例

查询结果转换主要包括详情转换、列表转换和分页转换。详情转换是单对象转换，列表转换是集合转换，分页转换则需要保留分页元数据，只转换分页记录。

详情查询转换：

```java
UserEntity userEntity = userStore.get(id);
UserInfoVO userInfoVO = objectConvertService.convert(userEntity, UserInfoVO.class);
```

列表查询转换：

```java
List<UserEntity> userEntityList = userStore.values()
        .stream()
        .sorted(Comparator.comparing(UserEntity::getId))
        .toList();

List<UserInfoVO> userInfoVOList = objectConvertService.convertList(userEntityList, UserInfoVO.class);
```

如果继续使用前文定义的 `PageResult<T>`，分页查询可以这样处理。

文件位置：`src/main/java/io/github/atengk/mapstruct/service/UserPageQueryService.java`

```java
package io.github.atengk.mapstruct.service;

import cn.hutool.core.collection.CollUtil;
import io.github.atengk.mapstruct.common.PageResult;
import io.github.atengk.mapstruct.entity.UserEntity;
import io.github.atengk.mapstruct.vo.UserInfoVO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * 用户分页查询服务.
 *
 * @author Ateng
 * @since 2026-05-06
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class UserPageQueryService {

    private final ObjectConvertService objectConvertService;

    public PageResult<UserInfoVO> convertUserPage() {
        List<UserEntity> records = CollUtil.newArrayList(
                new UserEntity()
                        .setId(1L)
                        .setUsername("ateng")
                        .setNickname("阿腾")
                        .setMobile("13800000000")
                        .setEnabled(true),
                new UserEntity()
                        .setId(2L)
                        .setUsername("zhangsan")
                        .setNickname("张三")
                        .setMobile("13900000000")
                        .setEnabled(true)
        );

        PageResult<UserEntity> entityPage = new PageResult<UserEntity>()
                .setCurrent(1L)
                .setSize(10L)
                .setTotal(2L)
                .setRecords(records);

        PageResult<UserInfoVO> voPage = objectConvertService.convertPage(entityPage, UserInfoVO.class);
        log.info("用户分页结果转换完成，总数：{}", voPage.getTotal());
        return voPage;
    }

}
```

分页返回示例：

```json
{
  "code": 200,
  "message": "操作成功",
  "data": {
    "current": 1,
    "size": 10,
    "total": 2,
    "records": [
      {
        "id": 1,
        "username": "ateng",
        "nickname": "阿腾",
        "mobile": "13800000000",
        "enabled": true,
        "createdAt": null,
        "updatedAt": null
      },
      {
        "id": 2,
        "username": "zhangsan",
        "nickname": "张三",
        "mobile": "13900000000",
        "enabled": true,
        "createdAt": null,
        "updatedAt": null
      }
    ]
  }
}
```

完整验证命令如下：

```bash
# 编译项目，触发 MapStruct Plus 生成转换代码
mvn clean compile

# 启动 Spring Boot 3 项目
mvn spring-boot:run

# 新增用户
curl -X POST "http://localhost:8080/api/users" \
  -H "Content-Type: application/json" \
  -d '{
    "username": "ateng",
    "nickname": "阿腾",
    "mobile": "13800000000"
  }'

# 查询用户列表
curl -X GET "http://localhost:8080/api/users"
```

开发中建议将转换逻辑统一放在 Service 层，Controller 只返回 VO 或分页 VO。对于字段名称一致、类型一致的常规转换，使用 `@AutoMapper` 即可；对于字段差异、类型差异、字典枚举、金额时间等复杂转换，再引入 `@AutoMapping`、自定义 Mapper 或业务层补充处理。



## 测试与验证

本节用于说明 MapStruct Plus 转换逻辑的测试方式。对象转换虽然属于基础代码，但它直接影响接口入参、业务对象和返回对象之间的数据准确性，因此需要通过单元测试覆盖单对象、集合、分页、字段忽略、字段重命名和自定义转换等场景。

### 单元测试编写

单元测试建议放在 `src/test/java` 目录下，优先测试项目封装后的 `ObjectConvertService`，而不是每个业务类中重复测试 `Converter`。这样可以同时验证 Spring Boot 容器、MapStruct Plus 自动配置、注解处理器生成代码和统一转换封装是否都正常工作。

测试依赖如果前面已经引入 `spring-boot-starter-test`，这里不需要重复添加。若项目中没有测试依赖，可以在 `pom.xml` 中补充：

```xml
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-test</artifactId>
    <scope>test</scope>
</dependency>
```

测试文件结构如下：

```text
src/test/java/io/github/atengk/mapstruct/MapStructPlusConvertTest.java
```

下面的测试类覆盖单对象转换、集合转换、分页转换和新增参数转换。

文件位置：`src/test/java/io/github/atengk/mapstruct/MapStructPlusConvertTest.java`

```java
package io.github.atengk.mapstruct;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.date.LocalDateTimeUtil;
import io.github.atengk.mapstruct.common.PageResult;
import io.github.atengk.mapstruct.entity.UserEntity;
import io.github.atengk.mapstruct.param.UserCreateParam;
import io.github.atengk.mapstruct.service.ObjectConvertService;
import io.github.atengk.mapstruct.vo.UserInfoVO;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.List;

/**
 * MapStruct Plus 对象转换测试.
 *
 * @author Ateng
 * @since 2026-05-06
 */
@SpringBootTest
class MapStructPlusConvertTest {

    @Autowired
    private ObjectConvertService objectConvertService;

    @Test
    void testConvertUserEntityToUserInfoVO() {
        UserEntity userEntity = new UserEntity()
                .setId(1L)
                .setUsername("ateng")
                .setNickname("阿腾")
                .setMobile("13800000000")
                .setEnabled(true)
                .setCreatedAt(LocalDateTimeUtil.now())
                .setUpdatedAt(LocalDateTimeUtil.now());

        UserInfoVO userInfoVO = objectConvertService.convert(userEntity, UserInfoVO.class);

        Assertions.assertNotNull(userInfoVO);
        Assertions.assertEquals(userEntity.getId(), userInfoVO.getId());
        Assertions.assertEquals(userEntity.getUsername(), userInfoVO.getUsername());
        Assertions.assertEquals(userEntity.getNickname(), userInfoVO.getNickname());
        Assertions.assertEquals(userEntity.getMobile(), userInfoVO.getMobile());
        Assertions.assertEquals(userEntity.getEnabled(), userInfoVO.getEnabled());
    }

    @Test
    void testConvertUserEntityListToUserInfoVOList() {
        List<UserEntity> userEntityList = CollUtil.newArrayList(
                new UserEntity()
                        .setId(1L)
                        .setUsername("ateng")
                        .setNickname("阿腾")
                        .setMobile("13800000001")
                        .setEnabled(true),
                new UserEntity()
                        .setId(2L)
                        .setUsername("zhangsan")
                        .setNickname("张三")
                        .setMobile("13800000002")
                        .setEnabled(false)
        );

        List<UserInfoVO> userInfoVOList = objectConvertService.convertList(userEntityList, UserInfoVO.class);

        Assertions.assertEquals(2, userInfoVOList.size());
        Assertions.assertEquals("ateng", userInfoVOList.get(0).getUsername());
        Assertions.assertEquals("zhangsan", userInfoVOList.get(1).getUsername());
    }

    @Test
    void testConvertUserPage() {
        PageResult<UserEntity> entityPage = new PageResult<UserEntity>()
                .setCurrent(1L)
                .setSize(10L)
                .setTotal(2L)
                .setRecords(CollUtil.newArrayList(
                        new UserEntity()
                                .setId(1L)
                                .setUsername("ateng")
                                .setNickname("阿腾")
                                .setMobile("13800000001")
                                .setEnabled(true),
                        new UserEntity()
                                .setId(2L)
                                .setUsername("lisi")
                                .setNickname("李四")
                                .setMobile("13800000002")
                                .setEnabled(true)
                ));

        PageResult<UserInfoVO> voPage = objectConvertService.convertPage(entityPage, UserInfoVO.class);

        Assertions.assertEquals(entityPage.getCurrent(), voPage.getCurrent());
        Assertions.assertEquals(entityPage.getSize(), voPage.getSize());
        Assertions.assertEquals(entityPage.getTotal(), voPage.getTotal());
        Assertions.assertEquals(2, voPage.getRecords().size());
        Assertions.assertEquals("ateng", voPage.getRecords().get(0).getUsername());
    }

    @Test
    void testConvertUserCreateParamToUserEntity() {
        UserCreateParam param = new UserCreateParam()
                .setUsername("ateng")
                .setNickname("阿腾")
                .setMobile("13800000000");

        UserEntity userEntity = objectConvertService.convert(param, UserEntity.class);

        Assertions.assertNotNull(userEntity);
        Assertions.assertEquals(param.getUsername(), userEntity.getUsername());
        Assertions.assertEquals(param.getNickname(), userEntity.getNickname());
        Assertions.assertEquals(param.getMobile(), userEntity.getMobile());
        Assertions.assertNull(userEntity.getId());
        Assertions.assertNull(userEntity.getCreatedAt());
    }

}
```

执行测试命令如下：

```bash
# 清理旧编译产物并执行全部测试
mvn clean test

# 只执行 MapStruct Plus 转换测试类
mvn -Dtest=MapStructPlusConvertTest test
```

`mvn clean test` 会先触发编译，MapStruct Plus 注解处理器会在编译阶段生成转换代码，然后 JUnit 再执行测试。如果编译阶段失败，应优先检查 Maven 注解处理器配置，而不是先排查测试代码。

### 转换结果校验

转换结果校验的重点不是只判断对象非空，而是确认关键字段是否准确转换、敏感字段是否被忽略、分页元数据是否保留、集合空值是否符合项目约定。

建议按照以下维度校验：

| 校验项     | 说明                                          |
| ---------- | --------------------------------------------- |
| 对象非空   | 源对象不为空时，目标对象应正常生成            |
| 同名字段   | `id`、`username`、`mobile` 等字段应保持一致   |
| 字段重命名 | `mobile -> phoneNumber` 等映射关系应准确      |
| 忽略字段   | `password`、`deleted` 等字段不应被赋值        |
| 类型转换   | 枚举、金额、时间等字段应符合预期格式          |
| 集合转换   | 集合数量、顺序和元素字段应正确                |
| 分页转换   | `current`、`size`、`total` 不应被转换逻辑破坏 |
| 空值处理   | 空对象、空集合、空字段应符合统一约定          |

字段重命名可以单独写测试，避免后续字段调整时静默出错。

文件位置：`src/test/java/io/github/atengk/mapstruct/UserProfileConvertTest.java`

```java
package io.github.atengk.mapstruct;

import io.github.atengk.mapstruct.dto.UserProfileDTO;
import io.github.atengk.mapstruct.entity.UserProfileEntity;
import io.github.atengk.mapstruct.service.ObjectConvertService;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

/**
 * 用户档案字段映射测试.
 *
 * @author Ateng
 * @since 2026-05-06
 */
@SpringBootTest
class UserProfileConvertTest {

    @Autowired
    private ObjectConvertService objectConvertService;

    @Test
    void testFieldMapping() {
        UserProfileEntity entity = new UserProfileEntity()
                .setId(1L)
                .setUsername("ateng")
                .setMobile("13800000000");

        UserProfileDTO dto = objectConvertService.convert(entity, UserProfileDTO.class);

        Assertions.assertNotNull(dto);
        Assertions.assertEquals(entity.getId(), dto.getId());
        Assertions.assertEquals(entity.getUsername(), dto.getUsername());
        Assertions.assertEquals(entity.getMobile(), dto.getPhoneNumber());
    }

}
```

敏感字段忽略也需要独立测试。尤其是接口返回对象中暂时保留了 `password`、`deleted` 等字段时，必须通过测试确认它们不会被源对象赋值。

文件位置：`src/test/java/io/github/atengk/mapstruct/SystemUserConvertTest.java`

```java
package io.github.atengk.mapstruct;

import io.github.atengk.mapstruct.entity.SystemUserEntity;
import io.github.atengk.mapstruct.service.ObjectConvertService;
import io.github.atengk.mapstruct.vo.SystemUserVO;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

/**
 * 系统用户敏感字段转换测试.
 *
 * @author Ateng
 * @since 2026-05-06
 */
@SpringBootTest
class SystemUserConvertTest {

    @Autowired
    private ObjectConvertService objectConvertService;

    @Test
    void testIgnoreSensitiveFields() {
        SystemUserEntity entity = new SystemUserEntity()
                .setId(1L)
                .setUsername("admin")
                .setNickname("管理员")
                .setPassword("123456")
                .setDeleted(false);

        SystemUserVO vo = objectConvertService.convert(entity, SystemUserVO.class);

        Assertions.assertNotNull(vo);
        Assertions.assertEquals(entity.getId(), vo.getId());
        Assertions.assertEquals(entity.getUsername(), vo.getUsername());
        Assertions.assertEquals(entity.getNickname(), vo.getNickname());
        Assertions.assertNull(vo.getPassword());
        Assertions.assertNull(vo.getDeleted());
    }

}
```

如果项目中使用接口快照测试，也可以将转换结果序列化为 JSON 后对比关键字段。但对象转换测试不建议过度依赖完整 JSON 字符串，因为字段顺序、时间格式、空值序列化策略变更可能导致测试噪声增加。

### 常见异常排查

MapStruct Plus 的问题大多发生在编译期。遇到转换失败时，应先执行 `mvn clean compile`，确认生成代码是否正常，再检查 Spring Bean、字段映射和自定义 Mapper 配置。

常见问题如下：

| 异常或现象                                  | 常见原因                                                     | 处理方式                                                   |
| ------------------------------------------- | ------------------------------------------------------------ | ---------------------------------------------------------- |
| `NoSuchBeanDefinitionException: Converter`  | 未引入 `mapstruct-plus-spring-boot-starter`，或 Spring Boot 扫描范围不正确 | 检查 Maven 依赖和启动类 `scanBasePackages`                 |
| `target/generated-sources/annotations` 为空 | 注解处理器未生效                                             | 检查 `maven-compiler-plugin` 的 `annotationProcessorPaths` |
| 编译提示 `Unknown property xxx`             | `@AutoMapping` 的 `source` 或 `target` 字段名写错            | 核对源类、目标类字段名称和 getter、setter                  |
| Lombok 字段无法识别                         | Lombok 与 MapStruct 注解处理顺序问题                         | 添加 `lombok-mapstruct-binding`                            |
| 转换后字段为 `null`                         | 字段名不一致、类型不匹配或未配置映射规则                     | 增加 `@AutoMapping` 或自定义 Mapper                        |
| 集合转换结果为空                            | 源集合为空或统一转换封装做了空集合返回                       | 检查源数据和 `convertList` 空值逻辑                        |
| 分页元数据丢失                              | 只转换了 records，没有复制分页字段                           | 使用统一 `convertPage` 方法                                |
| 敏感字段被返回                              | VO 中保留敏感字段且未配置忽略                                | 删除 VO 敏感字段或配置 `ignore = true`                     |
| 自定义转换方法未调用                        | `@Named` 名称不一致，或 `uses` 未引入转换类                  | 检查 `qualifiedByName` 和 `uses` 配置                      |
| 循环对象转换栈溢出                          | 对象存在双向引用                                             | 启用循环避免配置或拆分返回对象结构                         |

建议使用下面的命令进行最小化排查：

```bash
# 清理旧代码，避免历史生成类干扰
mvn clean

# 只执行编译，确认注解处理器是否正常生成代码
mvn compile

# 查看生成代码目录
find target/generated-sources/annotations -type f | head -20

# 执行指定测试类，缩小排查范围
mvn -Dtest=MapStructPlusConvertTest test
```

命令说明：`mvn clean` 用于删除历史编译产物；`mvn compile` 用于触发 MapStruct Plus 注解处理器；`find target/generated-sources/annotations` 用于确认生成类是否存在；`-Dtest=xxx` 可以只执行指定测试类，便于定位某一类转换问题。

如果出现字段转换结果不符合预期，应优先查看生成代码。生成代码路径通常位于：

```text
target/generated-sources/annotations
```

生成代码能直接反映最终调用了哪些 setter、是否调用自定义转换方法、是否忽略了某些字段。相比只看注解，查看生成代码更容易定位问题。

## 最佳实践

本节给出 MapStruct Plus 在 Spring Boot 3 项目中的落地规范。对象转换本身不复杂，但如果 DTO、VO、Mapper 和字段规则缺少统一约定，项目规模变大后容易出现重复映射、敏感字段泄露、转换规则分散和接口结构混乱等问题。

### DTO 与 VO 分层规范

DTO 与 VO 建议按职责拆分。DTO 偏向业务层和服务间数据传输，VO 偏向接口返回展示，Param 或 Command 偏向接口入参。不要用一个对象同时承担新增入参、修改入参、查询返回和数据库实体职责。

推荐分层如下：

| 类型    | 用途                   | 示例                                      |
| ------- | ---------------------- | ----------------------------------------- |
| Entity  | 数据库实体或持久化对象 | `UserEntity`                              |
| Param   | Controller 入参对象    | `UserCreateParam`、`UserUpdateParam`      |
| DTO     | Service 层传输对象     | `UserDTO`、`UserDetailDTO`                |
| VO      | Controller 返回对象    | `UserInfoVO`、`UserPageVO`                |
| Query   | 查询条件对象           | `UserPageQuery`                           |
| Command | 业务命令对象           | `UserCreateCommand`、`UserDisableCommand` |

推荐包结构如下：

```text
src/main/java/io/github/atengk/user
├── controller
│   └── UserController.java
├── service
│   ├── UserService.java
│   └── impl
│       └── UserServiceImpl.java
├── entity
│   └── UserEntity.java
├── param
│   ├── UserCreateParam.java
│   └── UserUpdateParam.java
├── query
│   └── UserPageQuery.java
├── dto
│   └── UserDTO.java
├── vo
│   └── UserInfoVO.java
└── mapper
    └── UserMapper.java
```

对象转换方向建议保持清晰：

```text
新增接口：UserCreateParam -> UserEntity -> UserInfoVO
修改接口：UserUpdateParam -> UserEntity -> UserInfoVO
查询详情：UserEntity -> UserInfoVO
查询列表：List<UserEntity> -> List<UserInfoVO>
分页查询：PageResult<UserEntity> -> PageResult<UserInfoVO>
```

不要在 Controller 中直接返回 Entity，也不要在 Entity 上堆积接口展示字段。Entity 应服务于持久化，VO 应服务于接口契约，两者职责不同。

### Mapper 命名规范

MapStruct Plus 常规转换可以不手写 Mapper，只通过 `@AutoMapper` 和 `Converter` 完成；当字段规则复杂、存在自定义类型转换、需要复用 MapStruct 原生能力时，再定义独立 Mapper。命名应体现业务对象和转换方向，避免出现 `CommonMapper`、`MyMapper`、`DataMapper` 这类含义不清的名称。

推荐命名如下：

| 场景             | 命名                    |
| ---------------- | ----------------------- |
| 用户对象转换     | `UserMapper`            |
| 用户时间字段转换 | `UserTimeMapper`        |
| 订单金额转换     | `OrderAmountMapper`     |
| 字典值转换       | `DictConvertMapper`     |
| 枚举转换         | `EnumConvertMapper`     |
| 公共金额转换     | `MoneyConvertMapper`    |
| 公共日期转换     | `DateTimeConvertMapper` |

自定义 Mapper 建议放在业务模块的 `mapper` 包中，公共转换类可以放在 `common.convert` 或 `common.mapper` 包中。

文件位置：`src/main/java/io/github/atengk/user/mapper/UserMapper.java`

```java
package io.github.atengk.user.mapper;

import io.github.atengk.user.entity.UserEntity;
import io.github.atengk.user.vo.UserInfoVO;
import io.github.linpeilie.BaseMapper;
import org.mapstruct.Mapper;
import org.mapstruct.MappingConstants;

/**
 * 用户对象转换 Mapper.
 *
 * @author Ateng
 * @since 2026-05-06
 */
@Mapper(componentModel = MappingConstants.ComponentModel.SPRING)
public interface UserMapper extends BaseMapper<UserEntity, UserInfoVO> {
}
```

如果只是同名同类型字段转换，优先使用 `@AutoMapper`：

```java
@AutoMapper(target = UserInfoVO.class)
public class UserEntity {
}
```

如果存在复杂字段转换，再使用手写 Mapper：

```java
@Mapper(
        componentModel = MappingConstants.ComponentModel.SPRING,
        uses = {DateTimeConvertMapper.class, MoneyConvertMapper.class}
)
public interface OrderMapper extends BaseMapper<OrderEntity, OrderVO> {
}
```

命名规范建议：

| 规范                | 说明                                                      |
| ------------------- | --------------------------------------------------------- |
| 按业务命名          | 使用 `UserMapper`、`OrderMapper`，不要使用 `EntityMapper` |
| 公共转换单独命名    | 金额、时间、枚举等公共逻辑放到独立 Mapper                 |
| 避免万能 Mapper     | 不要把所有对象转换都塞进一个 `CommonMapper`               |
| Mapper 不写业务逻辑 | 不访问数据库、不调用远程接口、不操作缓存                  |
| 方法名保持稳定      | 常规转换使用 `convert`，特殊转换使用明确名称              |

### 字段转换维护建议

字段转换规则应尽量靠近字段定义或业务 Mapper，避免散落在 Controller、Service 和工具类中。字段名不一致、字段忽略、枚举转换、金额转换、时间格式转换都应该有明确维护位置。

推荐维护策略如下：

| 转换类型           | 推荐维护位置                                  |
| ------------------ | --------------------------------------------- |
| 同名同类型字段     | `@AutoMapper` 自动转换                        |
| 字段名不同         | `@AutoMapping(source, target)`                |
| 敏感字段忽略       | VO 删除字段，或 `@AutoMapping(ignore = true)` |
| 枚举编码转文本     | 枚举转换类或业务 Mapper                       |
| 金额分转元         | `MoneyConvertMapper`                          |
| 时间格式化         | Jackson 全局配置或 `DateTimeConvertMapper`    |
| 分页转换           | 统一 `ObjectConvertService.convertPage`       |
| 涉及查询的补充字段 | Service 层处理                                |

字段转换维护时，建议遵循以下规则：

1. 优先保持字段名称一致。Entity、DTO、VO 中含义相同的字段尽量使用同一个名称，例如统一使用 `username`，不要一处叫 `userName`，另一处叫 `accountName`。
2. 敏感字段不要出现在 VO 中。密码、盐值、逻辑删除标记、租户内部字段、内部备注等字段应从返回对象中删除，而不是只依赖忽略规则。
3. 修改接口不要直接覆盖 Entity。`UserUpdateParam` 转为 `UserEntity` 后，应在 Service 层按业务规则合并旧数据，避免未传字段被覆盖为 `null`。
4. 自定义转换方法保持无副作用。Mapper 中只做内存计算、格式化和结构转换，不访问数据库、不调用 Redis、不请求第三方接口。
5. 转换规则变更必须补测试。新增字段映射、忽略字段、枚举展示字段、金额字段时，应同步补充单元测试，避免接口返回结构静默变化。
6. 定期查看生成代码。遇到复杂转换或字段不符合预期时，优先查看 `target/generated-sources/annotations` 下的生成类，确认最终生成逻辑。

项目中可以建立一份字段转换维护清单，作为代码评审依据：

```text
字段转换评审清单：
1. Entity 是否直接暴露到 Controller 返回值中
2. VO 是否包含 password、deleted、tenantId 等敏感或内部字段
3. 字段名不一致时是否配置了 @AutoMapping
4. 字段类型不一致时是否存在明确转换方法
5. 新增字段是否补充了转换测试
6. 分页转换是否保留 current、size、total 等元数据
7. 修改接口是否避免 null 覆盖旧值
8. Mapper 中是否混入数据库、缓存、远程调用等业务逻辑
```

最终建议是：简单转换交给 `@AutoMapper`，复杂字段交给自定义 Mapper，业务补充字段交给 Service，接口返回结构交给 VO。这样能保持 MapStruct Plus 的编译期优势，同时避免转换规则和业务逻辑混在一起。