# ip2region

## 功能概述

本章节用于说明 ip2region 在 SpringBoot3 项目中的功能定位、适用场景和技术选型。ip2region 主要用于将客户端 IP 地址解析为对应的地理归属地信息，适合在后端系统中作为基础能力组件进行封装和复用。

### 功能定位

ip2region 在 SpringBoot3 项目中定位为离线 IP 归属地查询组件，用于根据 IPv4 或 IPv6 地址查询对应的国家、省份、城市、运营商等区域信息。

该组件不直接负责获取客户端真实 IP，也不直接参与登录认证、风控判定或权限控制，而是作为基础工具能力，为访问日志、登录日志、审计记录、风控分析、运营统计等业务模块提供 IP 地区解析结果。

在实际开发中，建议将 ip2region 封装为独立的 Spring Bean 或 Service，由业务代码统一调用。这样可以集中处理 xdb 文件加载、查询对象初始化、异常兜底、结果格式转换和后续扩展，避免在 Controller、Filter、Interceptor 等位置重复编写查询逻辑。

### 使用场景

ip2region 适合用于需要根据 IP 地址补充地区信息的业务场景，常见场景包括用户登录地区识别、访问日志增强、接口访问来源统计、后台操作审计、异常登录提醒和基础风控分析。

在用户登录场景中，系统可以在用户登录成功后解析客户端 IP，并将登录国家、省份、城市、运营商等信息写入登录日志，便于用户查看最近登录记录，也便于后台排查异常访问。

在访问日志场景中，系统可以将 IP 归属地信息追加到访问日志中，与请求路径、请求方法、响应状态、耗时、用户标识等字段一起记录，方便后续按地区统计访问量、分析接口调用来源或排查异常流量。

在风控分析场景中，IP 归属地可以作为辅助判断条件，例如识别短时间内跨地区登录、异常地区访问、代理网络访问等情况。但需要注意，IP 归属地并不是强身份依据，不能单独作为风控结论，应结合账号行为、设备指纹、登录历史等信息综合判断。

### 技术选型

本项目采用 SpringBoot3 集成 ip2region 的方式实现离线 IP 归属地查询。SpringBoot3 负责 Web 服务、配置管理和 Bean 生命周期管理，ip2region 负责基于 xdb 数据文件执行本地查询，Hutool 用于字符串、文件、路径和空值等通用工具处理。

| 技术组件  | 选型        | 说明                                              |
| --------- | ----------- | ------------------------------------------------- |
| JDK       | Java 17+    | SpringBoot3 推荐使用 Java 17 及以上版本。         |
| Web 框架  | SpringBoot3 | 提供接口服务、配置加载、Bean 管理和自动装配能力。 |
| IP 查询库 | ip2region   | 提供离线 IP 归属地查询能力。                      |
| 数据文件  | xdb         | ip2region 使用的本地离线数据文件。                |
| 工具类    | Hutool      | 简化字符串判断、文件处理、路径处理等常见操作。    |
| 构建工具  | Maven       | 管理项目依赖和构建流程。                          |

ip2region 的核心优势是本地离线查询，不依赖第三方公网接口，因此适合部署在内网环境、私有化环境和对稳定性要求较高的业务系统中。相比调用远程 IP 查询接口，本地 xdb 查询可以减少网络依赖、降低调用延迟，并避免外部接口限流或不可用带来的影响。

在查询模式上，实际项目中通常会优先选择缓存查询方式，将 xdb 文件的索引或内容加载到内存中，以减少频繁磁盘读取带来的性能损耗。对于普通业务系统，可以优先使用索引缓存模式；对于访问量较高、对查询延迟更敏感的系统，可以考虑使用全量内存缓存模式。

## 环境准备

本章节用于准备 SpringBoot3 项目集成 ip2region 所需的基础环境，包括项目运行环境、Maven 依赖配置和 xdb 数据文件准备。完成本章节后，后续即可继续进行配置设计、查询组件封装和 Web 接口集成。

### SpringBoot3 项目环境

在集成 ip2region 前，需要先准备一个可以正常启动的 SpringBoot3 Web 项目。项目建议使用 Java 17 或更高版本，并统一使用 UTF-8 编码，避免 IP 归属地中的中文信息在日志或接口响应中出现乱码。

推荐环境如下：

| 环境项       | 推荐配置                        | 说明                                         |
| ------------ | ------------------------------- | -------------------------------------------- |
| JDK          | 17+                             | SpringBoot3 基础运行环境。                   |
| Spring Boot  | 3.x                             | 示例基于 SpringBoot3 Web 项目。              |
| Maven        | 3.6+                            | 用于依赖管理和项目构建。                     |
| 编码         | UTF-8                           | 保证中文地区信息正常显示。                   |
| 数据文件目录 | `src/main/resources/ip2region/` | 开发环境可将 xdb 文件放在 resources 目录下。 |

推荐项目目录结构如下：

```text
springboot-ip2region-demo
├── pom.xml
└── src
    └── main
        ├── java
        │   └── io
        │       └── github
        │           └── atengk
        │               └── ipregion
        │                   └── IpRegionApplication.java
        └── resources
            ├── application.yml
            └── ip2region
                └── ip2region.xdb
```

启动类放在项目根包下，用于启动 SpringBoot3 应用。

文件位置：`src/main/java/io/github/atengk/ipregion/IpRegionApplication.java`

```java
package io.github.atengk.ipregion;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * IP 归属地查询服务启动类
 *
 * @author Ateng
 * @since 2026-05-06
 */
@SpringBootApplication
public class IpRegionApplication {

    public static void main(String[] args) {
        SpringApplication.run(IpRegionApplication.class, args);
    }

}
```

### ip2region 依赖配置

本节用于配置项目所需依赖。`spring-boot-starter-web` 用于提供 Web 接口能力，`ip2region` 用于执行离线 IP 归属地查询，`hutool-all` 用于处理字符串、文件和路径等通用逻辑，`lombok` 用于简化配置类和数据对象代码。

文件位置：`pom.xml`

```xml
<dependencies>
    <!-- SpringBoot Web 支持，用于提供 Controller 接口和内置 Web 容器 -->
    <dependency>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-starter-web</artifactId>
    </dependency>

    <!-- 参数校验支持，后续可用于校验 IP 查询接口入参 -->
    <dependency>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-starter-validation</artifactId>
    </dependency>

    <!-- ip2region 离线 IP 归属地查询库 -->
    <dependency>
        <groupId>org.lionsoul</groupId>
        <artifactId>ip2region</artifactId>
        <version>3.3.7</version>
    </dependency>

    <!-- Hutool 工具类库，用于字符串、文件、路径等通用处理 -->
    <dependency>
        <groupId>cn.hutool</groupId>
        <artifactId>hutool-all</artifactId>
        <version>5.8.38</version>
    </dependency>

    <!-- Lombok 简化实体类、配置类和日志对象声明 -->
    <dependency>
        <groupId>org.projectlombok</groupId>
        <artifactId>lombok</artifactId>
        <optional>true</optional>
    </dependency>

    <!-- SpringBoot 测试支持，用于后续单元测试和接口测试 -->
    <dependency>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-starter-test</artifactId>
        <scope>test</scope>
    </dependency>
</dependencies>
```

如果项目使用 Spring Boot Parent 管理版本，可以在 `properties` 中统一声明 Java 版本和源码编码。

文件位置：`pom.xml`

```xml
<properties>
    <!-- SpringBoot3 推荐使用 Java 17 或更高版本 -->
    <java.version>17</java.version>

    <!-- 统一项目源码编码，避免中文归属地信息乱码 -->
    <project.build.sourceEncoding>UTF-8</project.build.sourceEncoding>
</properties>
```

依赖配置完成后，可以执行以下命令确认 ip2region 是否已经被 Maven 正确引入。

```bash
mvn dependency:tree | grep ip2region
```

如果命令输出中包含 `org.lionsoul:ip2region`，说明依赖已经正常加载。如果没有输出，需要检查 Maven 仓库配置、依赖坐标、版本号以及网络代理配置是否正确。

### xdb 数据文件准备

xdb 是 ip2region 使用的离线 IP 数据文件，项目运行时需要通过该文件完成 IP 地址与地区信息的匹配。没有 xdb 文件时，查询组件无法正常初始化，也无法执行 IP 归属地查询。

开发环境建议将 xdb 文件放在 `src/main/resources/ip2region/` 目录下，便于随项目一起打包和本地调试。

```text
src/main/resources/ip2region/ip2region.xdb
```

对应的配置可以先写入 `application.yml`，后续在“配置设计”章节中继续扩展查询模式、文件路径和 Bean 初始化配置。

文件位置：`src/main/resources/application.yml`

```yaml
server:
  port: 8080

ip2region:
  # 是否启用 IP 归属地查询功能
  enabled: true

  # xdb 数据文件路径，开发环境可以使用 classpath 路径
  xdb-path: classpath:ip2region/ip2region.xdb

  # 查询缓存策略：file、vector-index、memory
  # file：直接文件查询，内存占用低
  # vector-index：加载索引到内存，适合大多数业务场景
  # memory：加载完整 xdb 到内存，查询性能更好
  cache-policy: vector-index
```

生产环境更推荐将 xdb 文件放到外部目录，并通过配置文件指定绝对路径。这样可以在不重新打包应用的情况下更新 IP 数据文件。

文件位置：`application-prod.yml`

```yaml
ip2region:
  # 生产环境推荐使用外部文件路径，便于独立更新 xdb 数据
  xdb-path: /data/ip2region/ip2region.xdb

  # 生产环境通常推荐使用索引缓存或全量内存缓存
  cache-policy: vector-index
```

Linux 服务器中可以按如下方式准备 xdb 文件目录。

```bash
# 创建 xdb 文件存放目录
mkdir -p /data/ip2region

# 将 xdb 文件复制到生产目录
cp ip2region.xdb /data/ip2region/ip2region.xdb

# 查看文件是否存在以及大小是否正常
ls -lh /data/ip2region/ip2region.xdb
```

上述命令中，`mkdir -p` 用于创建数据文件目录；`cp` 用于复制 xdb 文件；`ls -lh` 用于确认文件是否存在以及文件大小是否符合预期。生产环境需要确保 SpringBoot 应用运行用户对 `/data/ip2region/ip2region.xdb` 文件具备读取权限。

如果 xdb 文件放在 jar 包内部，后续初始化时不能简单地把 `classpath:` 路径当作普通磁盘文件路径处理。可以在应用启动时通过 `ResourceLoader` 获取资源，再根据查询模式选择读取文件流、复制到临时目录或加载到内存中。该处理逻辑建议放在配置类或初始化组件中统一完成，避免业务代码直接处理资源路径。

## 配置设计

本章节用于设计 ip2region 在 SpringBoot3 项目中的配置结构，包括 xdb 文件路径、查询模式和 Bean 初始化方式。配置设计的目标是让开发环境和生产环境都能通过配置切换文件路径和查询策略，避免在业务代码中硬编码 xdb 文件位置或查询模式。

### xdb 文件路径配置

xdb 文件路径用于指定 ip2region 查询组件加载的数据文件位置。开发环境通常可以将 xdb 文件放在 `resources` 目录下，通过 `classpath:` 路径加载；生产环境更推荐使用外部绝对路径，便于独立更新 xdb 文件，不需要重新打包应用。

推荐在 `application.yml` 中统一配置 xdb 文件路径。

文件位置：`src/main/resources/application.yml`

```yaml
ip2region:
  # 是否启用 IP 归属地查询功能
  enabled: true

  # xdb 文件路径
  # 开发环境可以使用 classpath:ip2region/ip2region.xdb
  # 生产环境推荐使用 /data/ip2region/ip2region.xdb
  xdb-path: classpath:ip2region/ip2region.xdb

  # 查询模式：file、vector-index、memory
  cache-policy: vector-index

  # 是否在项目启动时初始化查询组件
  init-on-startup: true

  # 查询失败时是否返回空对象，false 表示直接抛出业务异常
  return-empty-when-error: true
```

如果生产环境使用外部 xdb 文件，可以在 `application-prod.yml` 中覆盖配置。

文件位置：`src/main/resources/application-prod.yml`

```yaml
ip2region:
  enabled: true
  xdb-path: /data/ip2region/ip2region.xdb
  cache-policy: vector-index
  init-on-startup: true
  return-empty-when-error: true
```

配置说明如下：

| 配置项                              | 示例值                              | 说明                             |
| ----------------------------------- | ----------------------------------- | -------------------------------- |
| `ip2region.enabled`                 | `true`                              | 是否启用 IP 归属地查询功能。     |
| `ip2region.xdb-path`                | `classpath:ip2region/ip2region.xdb` | xdb 数据文件路径。               |
| `ip2region.cache-policy`            | `vector-index`                      | 查询缓存策略。                   |
| `ip2region.init-on-startup`         | `true`                              | 是否在应用启动时初始化查询对象。 |
| `ip2region.return-empty-when-error` | `true`                              | 查询异常时是否返回空结果。       |

为了便于后续注入配置，需要创建配置属性类。

文件位置：`src/main/java/io/github/atengk/ipregion/config/Ip2RegionProperties.java`

```java
package io.github.atengk.ipregion.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * ip2region 配置属性
 *
 * @author Ateng
 * @since 2026-05-06
 */
@Data
@ConfigurationProperties(prefix = "ip2region")
public class Ip2RegionProperties {

    /**
     * 是否启用 IP 归属地查询
     */
    private Boolean enabled = true;

    /**
     * xdb 文件路径，支持 classpath: 和普通文件路径
     */
    private String xdbPath = "classpath:ip2region/ip2region.xdb";

    /**
     * 查询缓存策略：file、vector-index、memory
     */
    private String cachePolicy = "vector-index";

    /**
     * 是否启动时初始化
     */
    private Boolean initOnStartup = true;

    /**
     * 查询失败时是否返回空结果
     */
    private Boolean returnEmptyWhenError = true;

}
```

### 查询模式配置

查询模式用于控制 ip2region 查询时的数据加载方式。不同模式的内存占用、查询性能和并发适配能力不同，需要根据项目访问量和部署资源选择。

常见查询模式如下：

| 查询模式 | 配置值         | 说明                                | 适用场景                           |
| -------- | -------------- | ----------------------------------- | ---------------------------------- |
| 文件查询 | `file`         | 直接基于 xdb 文件查询，内存占用低。 | 低频查询、资源受限环境。           |
| 索引缓存 | `vector-index` | 加载索引到内存，减少部分磁盘 IO。   | 推荐默认模式，适合大多数业务系统。 |
| 全量内存 | `memory`       | 将完整 xdb 内容加载到内存。         | 高频查询、对响应延迟敏感的系统。   |

ip2region 官方说明中，xdb 支持文件查询、VectorIndex 缓存和全量 xdb 缓存，其中 VectorIndex 缓存会使用固定索引数据减少一次 IO，全量缓存会将整个 xdb 文件加载到内存以获得更低查询延迟。([GitHub](https://github.com/lionsoul2014/ip2region?utm_source=chatgpt.com))

为了避免业务代码中直接使用字符串判断，可以定义枚举统一管理查询模式。

文件位置：`src/main/java/io/github/atengk/ipregion/enums/Ip2RegionCachePolicy.java`

```java
package io.github.atengk.ipregion.enums;

import cn.hutool.core.util.StrUtil;
import lombok.Getter;

/**
 * ip2region 查询缓存策略
 *
 * @author Ateng
 * @since 2026-05-06
 */
@Getter
public enum Ip2RegionCachePolicy {

    /**
     * 文件查询
     */
    FILE("file"),

    /**
     * VectorIndex 索引缓存
     */
    VECTOR_INDEX("vector-index"),

    /**
     * 全量内存缓存
     */
    MEMORY("memory");

    private final String value;

    Ip2RegionCachePolicy(String value) {
        this.value = value;
    }

    /**
     * 根据配置值解析缓存策略
     *
     * @param value 配置值
     * @return 缓存策略
     */
    public static Ip2RegionCachePolicy of(String value) {
        if (StrUtil.isBlank(value)) {
            return VECTOR_INDEX;
        }
        for (Ip2RegionCachePolicy item : values()) {
            if (StrUtil.equalsIgnoreCase(item.getValue(), value)) {
                return item;
            }
        }
        return VECTOR_INDEX;
    }

}
```

实际业务系统推荐默认使用 `vector-index`。该模式兼顾查询性能和内存占用，适合登录日志、访问日志、后台查询接口等常见业务场景。如果系统访问量较高，并且 xdb 文件大小在可接受范围内，可以切换为 `memory`。

### Bean 初始化配置

Bean 初始化配置用于在 Spring 容器启动时创建 ip2region 查询对象，并将其交由 Spring 管理。这样业务代码只需要注入封装后的查询服务，不需要关心 xdb 文件读取、路径解析和 Searcher 创建细节。

ip2region 当前 Java Binding 中提供 `org.lionsoul.ip2region.service` 包下的查询服务，并支持通过 `ConfigBuilder` 设置 xdb 文件、缓存策略等信息；相关版本中还支持 `setXdbFile`、`setXdbInputStream` 等方式构建配置。([GitExtract](https://gitextract.com/lionsoul2014/ip2region?utm_source=chatgpt.com))

为了兼容 `classpath:` 和外部文件路径，建议先封装一个 xdb 文件解析工具方法。对于 `classpath:` 路径，可以将资源复制到临时目录，再交给 ip2region 进行初始化；对于普通路径，直接读取外部文件。

文件位置：`src/main/java/io/github/atengk/ipregion/config/Ip2RegionAutoConfiguration.java`

```java
package io.github.atengk.ipregion.config;

import cn.hutool.core.io.FileUtil;
import cn.hutool.core.util.StrUtil;
import io.github.atengk.ipregion.service.IpRegionService;
import io.github.atengk.ipregion.service.impl.IpRegionServiceImpl;
import lombok.extern.slf4j.Slf4j;
import org.lionsoul.ip2region.xdb.Searcher;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.ResourceLoaderAware;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.Resource;
import org.springframework.core.io.ResourceLoader;

import java.io.File;
import java.io.InputStream;
import java.nio.file.Files;

/**
 * ip2region 自动配置类
 *
 * @author Ateng
 * @since 2026-05-06
 */
@Slf4j
@Configuration
@EnableConfigurationProperties(Ip2RegionProperties.class)
@ConditionalOnProperty(prefix = "ip2region", name = "enabled", havingValue = "true", matchIfMissing = true)
public class Ip2RegionAutoConfiguration implements ResourceLoaderAware {

    private ResourceLoader resourceLoader;

    @Bean(destroyMethod = "close")
    public Searcher ip2RegionSearcher(Ip2RegionProperties properties) throws Exception {
        File xdbFile = this.resolveXdbFile(properties.getXdbPath());
        log.info("初始化 ip2region 查询组件，xdb文件：{}，查询模式：{}", xdbFile.getAbsolutePath(), properties.getCachePolicy());

        // 说明：
        // 不同 ip2region 版本的 Searcher 初始化 API 可能略有差异。
        // 如果项目使用的是新版 service.Ip2Region，也可以在此处替换为官方 service 配置方式。
        return Searcher.newWithFileOnly(xdbFile.getAbsolutePath());
    }

    @Bean
    public IpRegionService ipRegionService(Searcher searcher, Ip2RegionProperties properties) {
        return new IpRegionServiceImpl(searcher, properties);
    }

    /**
     * 解析 xdb 文件
     *
     * @param xdbPath xdb 文件路径
     * @return xdb 文件
     * @throws Exception 文件处理异常
     */
    private File resolveXdbFile(String xdbPath) throws Exception {
        if (StrUtil.isBlank(xdbPath)) {
            throw new IllegalArgumentException("xdb 文件路径不能为空");
        }

        if (!StrUtil.startWithIgnoreCase(xdbPath, "classpath:")) {
            File file = FileUtil.file(xdbPath);
            if (!FileUtil.exist(file)) {
                throw new IllegalArgumentException("xdb 文件不存在：" + xdbPath);
            }
            return file;
        }

        Resource resource = resourceLoader.getResource(xdbPath);
        if (!resource.exists()) {
            throw new IllegalArgumentException("classpath 下未找到 xdb 文件：" + xdbPath);
        }

        File tempFile = Files.createTempFile("ip2region-", ".xdb").toFile();
        tempFile.deleteOnExit();

        try (InputStream inputStream = resource.getInputStream()) {
            FileUtil.writeFromStream(inputStream, tempFile);
        }

        log.info("classpath xdb 文件已复制到临时目录：{}", tempFile.getAbsolutePath());
        return tempFile;
    }

    @Override
    public void setResourceLoader(ResourceLoader resourceLoader) {
        this.resourceLoader = resourceLoader;
    }

}
```

上面的配置类完成了三个动作：读取 `ip2region` 配置、解析 xdb 文件路径、创建查询 Service。后续业务代码只需要注入 `IpRegionService` 即可完成 IP 归属地查询。

需要注意，`Searcher.newWithFileOnly(xdbFile.getAbsolutePath())` 适用于常见 ip2region Java 客户端版本。如果你使用的是新版 `org.lionsoul.ip2region.service.Ip2Region` 服务类，可以将该 Bean 替换为官方 `ConfigBuilder` + `Ip2Region.create(...)` 的方式。新版 Java 查询服务在后续版本中增强了 IP 版本自动判断和线程安全查询能力。([新发布](https://newreleases.io/project/github/lionsoul2014/ip2region/release/v3.10.0?utm_source=chatgpt.com))

## 核心实现

本章节用于封装 ip2region 查询组件，包括查询服务接口、Searcher 初始化、IP 地址解析和查询结果结构化处理。核心实现的目标是让业务代码拿到结构化对象，而不是直接处理 `国家|省份|城市|运营商` 这类原始字符串。

### ip2region 查询组件封装

查询组件建议采用接口 + 实现类的方式封装。接口对外提供稳定的查询方法，实现类内部处理 ip2region 调用、异常兜底、结果转换和日志输出。

先定义 IP 归属地响应对象。

文件位置：`src/main/java/io/github/atengk/ipregion/model/IpRegionInfo.java`

```java
package io.github.atengk.ipregion.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * IP 归属地信息
 *
 * @author Ateng
 * @since 2026-05-06
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class IpRegionInfo {

    /**
     * 原始 IP 地址
     */
    private String ip;

    /**
     * 国家
     */
    private String country;

    /**
     * 省份
     */
    private String province;

    /**
     * 城市
     */
    private String city;

    /**
     * 运营商
     */
    private String isp;

    /**
     * 原始查询结果
     */
    private String rawRegion;

}
```

定义查询服务接口。

文件位置：`src/main/java/io/github/atengk/ipregion/service/IpRegionService.java`

```java
package io.github.atengk.ipregion.service;

import io.github.atengk.ipregion.model.IpRegionInfo;

/**
 * IP 归属地查询服务
 *
 * @author Ateng
 * @since 2026-05-06
 */
public interface IpRegionService {

    /**
     * 根据 IP 查询归属地
     *
     * @param ip IP 地址
     * @return IP 归属地信息
     */
    IpRegionInfo search(String ip);

}
```

查询服务实现类中统一完成参数校验、调用 Searcher、解析返回结果和异常处理。

文件位置：`src/main/java/io/github/atengk/ipregion/service/impl/IpRegionServiceImpl.java`

```java
package io.github.atengk.ipregion.service.impl;

import cn.hutool.core.util.StrUtil;
import io.github.atengk.ipregion.config.Ip2RegionProperties;
import io.github.atengk.ipregion.model.IpRegionInfo;
import io.github.atengk.ipregion.service.IpRegionService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.lionsoul.ip2region.xdb.Searcher;

import java.net.InetAddress;

/**
 * IP 归属地查询服务实现
 *
 * @author Ateng
 * @since 2026-05-06
 */
@Slf4j
@RequiredArgsConstructor
public class IpRegionServiceImpl implements IpRegionService {

    private final Searcher searcher;

    private final Ip2RegionProperties properties;

    @Override
    public IpRegionInfo search(String ip) {
        if (StrUtil.isBlank(ip)) {
            log.warn("IP归属地查询失败，IP地址为空");
            return empty(ip);
        }

        if (!isValidIp(ip)) {
            log.warn("IP归属地查询失败，非法IP地址：{}", ip);
            return empty(ip);
        }

        try {
            String region = searcher.search(ip);
            return parseRegion(ip, region);
        } catch (Exception e) {
            log.error("IP归属地查询异常，IP地址：{}", ip, e);
            if (Boolean.TRUE.equals(properties.getReturnEmptyWhenError())) {
                return empty(ip);
            }
            throw new IllegalStateException("IP归属地查询失败：" + ip, e);
        }
    }

    /**
     * 校验 IP 地址是否合法
     *
     * @param ip IP 地址
     * @return 是否合法
     */
    private boolean isValidIp(String ip) {
        try {
            InetAddress.getByName(ip);
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    /**
     * 解析 ip2region 原始结果
     *
     * @param ip     IP 地址
     * @param region 原始区域信息
     * @return IP 归属地信息
     */
    private IpRegionInfo parseRegion(String ip, String region) {
        if (StrUtil.isBlank(region)) {
            log.warn("IP归属地查询结果为空，IP地址：{}", ip);
            return empty(ip);
        }

        String[] items = StrUtil.splitToArray(region, '|');

        return IpRegionInfo.builder()
                .ip(ip)
                .country(getRegionItem(items, 0))
                .province(getRegionItem(items, 1))
                .city(getRegionItem(items, 2))
                .isp(getRegionItem(items, 3))
                .rawRegion(region)
                .build();
    }

    /**
     * 获取区域字段
     *
     * @param items 区域字段数组
     * @param index 字段下标
     * @return 字段值
     */
    private String getRegionItem(String[] items, int index) {
        if (items == null || index >= items.length) {
            return StrUtil.EMPTY;
        }
        String value = items[index];
        if (StrUtil.equals(value, "0")) {
            return StrUtil.EMPTY;
        }
        return StrUtil.blankToDefault(value, StrUtil.EMPTY);
    }

    /**
     * 构建空归属地对象
     *
     * @param ip IP 地址
     * @return 空归属地对象
     */
    private IpRegionInfo empty(String ip) {
        return IpRegionInfo.builder()
                .ip(StrUtil.blankToDefault(ip, StrUtil.EMPTY))
                .country(StrUtil.EMPTY)
                .province(StrUtil.EMPTY)
                .city(StrUtil.EMPTY)
                .isp(StrUtil.EMPTY)
                .rawRegion(StrUtil.EMPTY)
                .build();
    }

}
```

### xdb Searcher 初始化

xdb Searcher 初始化是整个查询能力的关键步骤。初始化时需要确认 xdb 文件存在、应用进程具备读取权限，并根据配置选择合适的缓存模式。

如果项目使用的 ip2region 版本支持不同缓存模式的初始化方法，可以按如下思路扩展配置类。

文件位置：`src/main/java/io/github/atengk/ipregion/config/Ip2RegionSearcherFactory.java`

```java
package io.github.atengk.ipregion.config;

import io.github.atengk.ipregion.enums.Ip2RegionCachePolicy;
import lombok.extern.slf4j.Slf4j;
import org.lionsoul.ip2region.xdb.Searcher;

import java.io.File;

/**
 * ip2region Searcher 工厂
 *
 * @author Ateng
 * @since 2026-05-06
 */
@Slf4j
public class Ip2RegionSearcherFactory {

    /**
     * 创建 Searcher
     *
     * @param xdbFile     xdb 文件
     * @param cachePolicy 缓存策略
     * @return Searcher
     * @throws Exception 初始化异常
     */
    public static Searcher create(File xdbFile, String cachePolicy) throws Exception {
        Ip2RegionCachePolicy policy = Ip2RegionCachePolicy.of(cachePolicy);
        log.info("创建 ip2region Searcher，文件：{}，缓存策略：{}", xdbFile.getAbsolutePath(), policy.getValue());

        return switch (policy) {
            case FILE -> Searcher.newWithFileOnly(xdbFile.getAbsolutePath());
            case VECTOR_INDEX -> Searcher.newWithVectorIndex(xdbFile.getAbsolutePath(), Searcher.loadVectorIndexFromFile(xdbFile.getAbsolutePath()));
            case MEMORY -> Searcher.newWithBuffer(Searcher.loadContentFromFile(xdbFile.getAbsolutePath()));
        };
    }

    private Ip2RegionSearcherFactory() {
    }

}
```

然后将自动配置类中的 Searcher 创建逻辑替换为工厂调用。

```java
@Bean(destroyMethod = "close")
public Searcher ip2RegionSearcher(Ip2RegionProperties properties) throws Exception {
    File xdbFile = this.resolveXdbFile(properties.getXdbPath());
    return Ip2RegionSearcherFactory.create(xdbFile, properties.getCachePolicy());
}
```

这种写法将 Searcher 初始化细节从配置类中拆出去，后续如果需要升级 ip2region 版本、支持 IPv6、切换新版 `Ip2Region` 服务类，只需要调整工厂类，不需要修改业务查询服务。

### IP 地址解析方法

IP 地址解析方法用于将传入的 IP 字符串规范化，并过滤非法值、空值、本地回环地址等特殊场景。业务系统中常见的 IP 来源包括接口参数、请求 Header、Nginx 转发头、网关转发头和 Servlet 请求对象。

在查询组件内部，建议只处理已经确认的 IP 字符串；真实客户端 IP 的提取逻辑应放在 Web 工具类中统一处理，避免查询服务承担过多职责。

下面是查询服务中对 IP 地址的基本处理规则：

| 场景        | 处理方式                                 |
| ----------- | ---------------------------------------- |
| 空字符串    | 返回空归属地对象。                       |
| 非法 IP     | 返回空归属地对象或抛出业务异常。         |
| `127.0.0.1` | 可返回空归属地对象，也可按内网地址处理。 |
| `localhost` | 不建议直接查询，应转换或忽略。           |
| 多级代理 IP | 应在 Web 层提取第一个有效公网 IP。       |

如果需要在查询前做更严格的过滤，可以增加一个 IP 工具类。

文件位置：`src/main/java/io/github/atengk/ipregion/util/IpAddressUtils.java`

```java
package io.github.atengk.ipregion.util;

import cn.hutool.core.util.StrUtil;

import java.net.InetAddress;

/**
 * IP 地址工具类
 *
 * @author Ateng
 * @since 2026-05-06
 */
public class IpAddressUtils {

    /**
     * 判断是否为合法 IP
     *
     * @param ip IP 地址
     * @return 是否合法
     */
    public static boolean isValidIp(String ip) {
        if (StrUtil.isBlank(ip)) {
            return false;
        }
        try {
            InetAddress.getByName(ip);
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    /**
     * 判断是否为本地 IP
     *
     * @param ip IP 地址
     * @return 是否本地 IP
     */
    public static boolean isLocalIp(String ip) {
        return StrUtil.equalsAny(ip, "127.0.0.1", "0:0:0:0:0:0:0:1", "::1");
    }

    /**
     * 标准化 IP 字符串
     *
     * @param ip IP 地址
     * @return 标准化后的 IP
     */
    public static String normalize(String ip) {
        if (StrUtil.isBlank(ip)) {
            return StrUtil.EMPTY;
        }
        return StrUtil.trim(ip);
    }

    private IpAddressUtils() {
    }

}
```

### 查询结果结构化处理

ip2region 查询结果通常是一个使用竖线分隔的字符串，例如：

```text
中国|广东省|深圳市|电信|CN
```

业务系统不建议直接存储和传递该原始字符串，而应转换为结构化对象。结构化处理可以提升字段可读性，也便于后续扩展数据库字段、接口响应字段或日志字段。

建议转换规则如下：

| 原始字段位置 | 字段含义 | Java 字段  |
| ------------ | -------- | ---------- |
| 第 1 段      | 国家     | `country`  |
| 第 2 段      | 省份     | `province` |
| 第 3 段      | 城市     | `city`     |
| 第 4 段      | 运营商   | `isp`      |
| 第 5 段      | 国家代码 | 可按需扩展 |

如果需要保留国家代码，可以扩展 `IpRegionInfo` 对象。

```java
/**
 * 国家代码
 */
private String countryCode;
```

然后调整解析逻辑：

```java
.countryCode(getRegionItem(items, 4))
```

查询结果结构化后，业务模块可以直接使用：

```java
IpRegionInfo regionInfo = ipRegionService.search("114.114.114.114");
String loginRegion = StrUtil.format("{}{}{}", regionInfo.getCountry(), regionInfo.getProvince(), regionInfo.getCity());
```

这种方式比直接拼接原始字符串更清晰，也更适合后续做日志检索、数据库存储和接口返回。

## Web 集成

本章节用于说明如何在 SpringBoot3 Web 项目中获取请求 IP、处理代理 Header，并提供一个 Controller 接口示例。Web 集成部分的重点是正确提取客户端 IP，而不是简单使用 `request.getRemoteAddr()`。

### 请求 IP 获取方式

在 SpringBoot3 Web 项目中，可以通过 `HttpServletRequest` 获取请求 IP。最直接的方式是调用 `request.getRemoteAddr()`，但在经过 Nginx、网关、负载均衡或 CDN 之后，该值通常是代理服务器地址，不一定是真实客户端 IP。

因此，建议封装统一的客户端 IP 获取工具类，按常见 Header 顺序尝试获取真实 IP。

常见 Header 包括：

| Header                 | 说明                               |
| ---------------------- | ---------------------------------- |
| `X-Forwarded-For`      | 常见代理转发 IP，可能包含多个 IP。 |
| `X-Real-IP`            | Nginx 常用真实 IP Header。         |
| `Proxy-Client-IP`      | 部分代理服务器使用。               |
| `WL-Proxy-Client-IP`   | WebLogic 场景可能出现。            |
| `HTTP_CLIENT_IP`       | 部分代理环境可能出现。             |
| `HTTP_X_FORWARDED_FOR` | 部分代理环境可能出现。             |

文件位置：`src/main/java/io/github/atengk/ipregion/util/ClientIpUtils.java`

```java
package io.github.atengk.ipregion.util;

import cn.hutool.core.util.StrUtil;
import jakarta.servlet.http.HttpServletRequest;

import java.util.List;

/**
 * 客户端 IP 工具类
 *
 * @author Ateng
 * @since 2026-05-06
 */
public class ClientIpUtils {

    private static final String UNKNOWN = "unknown";

    private static final List<String> IP_HEADERS = List.of(
            "X-Forwarded-For",
            "X-Real-IP",
            "Proxy-Client-IP",
            "WL-Proxy-Client-IP",
            "HTTP_CLIENT_IP",
            "HTTP_X_FORWARDED_FOR"
    );

    /**
     * 获取客户端 IP
     *
     * @param request HTTP 请求
     * @return 客户端 IP
     */
    public static String getClientIp(HttpServletRequest request) {
        if (request == null) {
            return StrUtil.EMPTY;
        }

        for (String header : IP_HEADERS) {
            String ip = request.getHeader(header);
            if (isValidHeaderIp(ip)) {
                return getFirstIp(ip);
            }
        }

        return StrUtil.blankToDefault(request.getRemoteAddr(), StrUtil.EMPTY);
    }

    /**
     * 判断 Header IP 是否有效
     *
     * @param ip Header 中的 IP
     * @return 是否有效
     */
    private static boolean isValidHeaderIp(String ip) {
        return StrUtil.isNotBlank(ip) && !StrUtil.equalsIgnoreCase(UNKNOWN, ip);
    }

    /**
     * 获取第一个 IP
     *
     * @param ip 多级代理 IP
     * @return 第一个 IP
     */
    private static String getFirstIp(String ip) {
        if (StrUtil.contains(ip, StrUtil.COMMA)) {
            return StrUtil.trim(StrUtil.subBefore(ip, StrUtil.COMMA, false));
        }
        return StrUtil.trim(ip);
    }

    private ClientIpUtils() {
    }

}
```

### Header 代理场景处理

在 Nginx、网关或负载均衡场景下，请求通常会经过多层代理。此时 `X-Forwarded-For` 可能包含多个 IP，例如：

```text
203.0.113.10, 10.0.0.12, 10.0.0.15
```

通常情况下，第一个 IP 是原始客户端 IP，后面的 IP 是经过的代理节点。因此工具类中默认取第一个 IP 作为客户端 IP。

如果使用 Nginx，建议在 Nginx 配置中显式传递真实 IP。

文件位置：`/etc/nginx/conf.d/app.conf`

```nginx
location / {
    proxy_pass http://127.0.0.1:8080;

    # 传递客户端真实 IP
    proxy_set_header X-Real-IP $remote_addr;

    # 传递代理链路 IP
    proxy_set_header X-Forwarded-For $proxy_add_x_forwarded_for;

    # 传递原始 Host
    proxy_set_header Host $host;

    # 传递请求协议
    proxy_set_header X-Forwarded-Proto $scheme;
}
```

如果系统部署在 Spring Cloud Gateway、Kubernetes Ingress、CDN 或 SLB 后面，需要确认上游是否已经正确设置 `X-Forwarded-For` 或 `X-Real-IP`。如果 Header 可以被外部客户端伪造，应只信任来自可信代理层写入的 Header，避免直接使用客户端传入的伪造 IP。

实际生产环境中，建议遵循以下规则：

| 场景                      | 建议                                        |
| ------------------------- | ------------------------------------------- |
| 单体服务直接暴露          | 可以使用 `request.getRemoteAddr()`。        |
| Nginx 反向代理            | 优先读取 `X-Real-IP` 或 `X-Forwarded-For`。 |
| 多级代理                  | 从 `X-Forwarded-For` 中取第一个有效 IP。    |
| 公网客户端可直接传 Header | 不应盲目信任 Header，应在网关层覆盖。       |
| 内网调用                  | 可根据业务需要忽略内网 IP 或单独标记。      |

### Controller 接口示例

Controller 用于提供一个简单的 IP 归属地查询接口，方便开发、测试和业务系统调用。接口支持两种方式：一种是通过请求参数传入 IP；另一种是不传 IP，自动解析当前请求的客户端 IP。

文件位置：`src/main/java/io/github/atengk/ipregion/controller/IpRegionController.java`

```java
package io.github.atengk.ipregion.controller;

import cn.hutool.core.util.StrUtil;
import io.github.atengk.ipregion.model.IpRegionInfo;
import io.github.atengk.ipregion.service.IpRegionService;
import io.github.atengk.ipregion.util.ClientIpUtils;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;


/**
 * IP 归属地查询接口
 *
 * @author Ateng
 * @since 2026-05-06
 */
@Slf4j
@RestController
@RequiredArgsConstructor
public class IpRegionController {

    private final IpRegionService ipRegionService;

    /**
     * 根据指定 IP 查询归属地
     *
     * @param ip IP 地址
     * @return IP 归属地信息
     */
    @GetMapping("/api/ip-region/search")
    public IpRegionInfo search(@RequestParam String ip) {
        log.info("查询指定IP归属地，IP地址：{}", ip);
        return ipRegionService.search(ip);
    }

    /**
     * 查询当前请求客户端 IP 归属地
     *
     * @param request HTTP 请求
     * @return IP 归属地信息
     */
    @GetMapping("/api/ip-region/current")
    public IpRegionInfo current(HttpServletRequest request) {
        String clientIp = ClientIpUtils.getClientIp(request);
        log.info("查询当前请求IP归属地，IP地址：{}", clientIp);
        return ipRegionService.search(clientIp);
    }

    /**
     * 自动查询 IP 归属地
     *
     * @param ip      可选 IP 地址
     * @param request HTTP 请求
     * @return IP 归属地信息
     */
    @GetMapping("/api/ip-region")
    public IpRegionInfo query(@RequestParam(required = false) String ip, HttpServletRequest request) {
        String queryIp = StrUtil.blankToDefault(ip, ClientIpUtils.getClientIp(request));
        log.info("查询IP归属地，IP地址：{}", queryIp);
        return ipRegionService.search(queryIp);
    }

}
```

接口说明如下：

| 接口路径                                   | 请求方式 | 说明                                           |
| ------------------------------------------ | -------- | ---------------------------------------------- |
| `/api/ip-region/search?ip=114.114.114.114` | GET      | 查询指定 IP 的归属地。                         |
| `/api/ip-region/current`                   | GET      | 查询当前请求客户端 IP 的归属地。               |
| `/api/ip-region?ip=114.114.114.114`        | GET      | 有 IP 时查询指定 IP，无 IP 时查询当前请求 IP。 |

可以使用以下命令验证接口。

```bash
# 查询指定 IP
curl "http://localhost:8080/api/ip-region/search?ip=114.114.114.114"

# 查询当前请求 IP
curl "http://localhost:8080/api/ip-region/current"

# 模拟代理 Header 查询当前请求 IP
curl -H "X-Forwarded-For: 114.114.114.114, 10.0.0.1" \
  "http://localhost:8080/api/ip-region/current"
```

响应示例：

```json
{
  "ip": "114.114.114.114",
  "country": "中国",
  "province": "江苏省",
  "city": "南京市",
  "isp": "",
  "rawRegion": "中国|江苏省|南京市|0|CN"
}
```

上述接口示例适合开发环境验证和后台管理接口使用。生产环境中，如果该接口暴露给外部系统，建议增加访问控制、限流策略和参数校验，避免被频繁调用造成不必要的资源消耗。



## 业务使用

本章节用于说明 ip2region 查询能力在业务系统中的具体使用方式。前面已经完成了查询组件、请求 IP 获取工具和 Controller 示例，本章节重点说明如何在普通业务代码、访问日志和用户登录流程中复用 `IpRegionService`。

### IP 归属地查询

IP 归属地查询是最基础的业务使用方式。业务代码只需要注入 `IpRegionService`，传入待查询的 IP 地址，即可获取结构化的 `IpRegionInfo` 对象。

适用场景包括后台手动查询 IP、接口入参 IP 解析、管理端展示 IP 来源、定时任务补全历史日志地区信息等。

文件位置：`src/main/java/io/github/atengk/ipregion/service/IpQueryBizService.java`

下面的代码演示如何在普通业务 Service 中调用 IP 归属地查询组件，并对查询结果进行业务格式化。

```java
package io.github.atengk.ipregion.service;

import cn.hutool.core.util.StrUtil;
import io.github.atengk.ipregion.model.IpRegionInfo;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

/**
 * IP 查询业务服务
 *
 * @author Ateng
 * @since 2026-05-06
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class IpQueryBizService {

    private final IpRegionService ipRegionService;

    /**
     * 查询 IP 归属地文本
     *
     * @param ip IP 地址
     * @return 归属地文本
     */
    public String getRegionText(String ip) {
        IpRegionInfo regionInfo = ipRegionService.search(ip);
        String regionText = buildRegionText(regionInfo);

        log.info("IP归属地业务查询完成，IP地址：{}，归属地：{}", ip, regionText);
        return regionText;
    }

    /**
     * 查询 IP 归属地详情
     *
     * @param ip IP 地址
     * @return IP 归属地信息
     */
    public IpRegionInfo getRegionInfo(String ip) {
        IpRegionInfo regionInfo = ipRegionService.search(ip);
        log.info("IP归属地详情查询完成，IP地址：{}，原始结果：{}", ip, regionInfo.getRawRegion());
        return regionInfo;
    }

    /**
     * 构建归属地文本
     *
     * @param regionInfo IP 归属地信息
     * @return 归属地文本
     */
    private String buildRegionText(IpRegionInfo regionInfo) {
        if (regionInfo == null) {
            return StrUtil.EMPTY;
        }

        String regionText = StrUtil.format("{}{}{}",
                StrUtil.blankToDefault(regionInfo.getCountry(), StrUtil.EMPTY),
                StrUtil.blankToDefault(regionInfo.getProvince(), StrUtil.EMPTY),
                StrUtil.blankToDefault(regionInfo.getCity(), StrUtil.EMPTY)
        );

        if (StrUtil.isNotBlank(regionInfo.getIsp())) {
            regionText = StrUtil.format("{} {}", regionText, regionInfo.getIsp());
        }

        return StrUtil.blankToDefault(regionText, StrUtil.EMPTY);
    }

}
```

业务调用示例：

```java
String regionText = ipQueryBizService.getRegionText("114.114.114.114");
```

如果查询结果为中国江苏南京，可以得到类似结果：

```text
中国江苏省南京市
```

如果业务只需要省市信息，可以在业务层只取 `province` 和 `city` 字段，不建议直接截取 `rawRegion` 原始字符串。

### 访问日志增强

访问日志增强用于在每次 HTTP 请求完成后，将客户端 IP、归属地、请求路径、请求方法、状态码、耗时等信息统一记录下来。这样可以在日志平台、数据库或审计系统中按地区分析访问来源。

推荐使用 Spring MVC `HandlerInterceptor` 实现访问日志增强。相比在每个 Controller 中手动记录，拦截器可以统一处理所有接口请求，避免业务代码重复。

文件位置：`src/main/java/io/github/atengk/ipregion/web/AccessLogInterceptor.java`

下面的拦截器会在请求完成后获取客户端 IP，并调用 `IpRegionService` 查询归属地，然后输出增强后的访问日志。

```java
package io.github.atengk.ipregion.web;

import cn.hutool.core.util.StrUtil;
import io.github.atengk.ipregion.model.IpRegionInfo;
import io.github.atengk.ipregion.service.IpRegionService;
import io.github.atengk.ipregion.util.ClientIpUtils;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

/**
 * 访问日志拦截器
 *
 * @author Ateng
 * @since 2026-05-06
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class AccessLogInterceptor implements HandlerInterceptor {

    private static final String START_TIME_ATTR = "REQUEST_START_TIME";

    private final IpRegionService ipRegionService;

    /**
     * 请求进入时记录开始时间
     *
     * @param request  HTTP 请求
     * @param response HTTP 响应
     * @param handler  处理器
     * @return 是否继续执行
     */
    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
        request.setAttribute(START_TIME_ATTR, System.currentTimeMillis());
        return true;
    }

    /**
     * 请求完成后输出增强访问日志
     *
     * @param request  HTTP 请求
     * @param response HTTP 响应
     * @param handler  处理器
     * @param ex       异常信息
     */
    @Override
    public void afterCompletion(HttpServletRequest request, HttpServletResponse response, Object handler, Exception ex) {
        long costTime = getCostTime(request);
        String clientIp = ClientIpUtils.getClientIp(request);
        IpRegionInfo regionInfo = ipRegionService.search(clientIp);
        String regionText = buildRegionText(regionInfo);

        log.info("接口访问日志，IP地址：{}，归属地：{}，请求方法：{}，请求路径：{}，状态码：{}，耗时：{}ms",
                clientIp,
                regionText,
                request.getMethod(),
                request.getRequestURI(),
                response.getStatus(),
                costTime
        );

        if (ex != null) {
            log.warn("接口访问异常，IP地址：{}，请求路径：{}，异常信息：{}",
                    clientIp,
                    request.getRequestURI(),
                    ex.getMessage()
            );
        }
    }

    /**
     * 获取请求耗时
     *
     * @param request HTTP 请求
     * @return 请求耗时
     */
    private long getCostTime(HttpServletRequest request) {
        Object startTime = request.getAttribute(START_TIME_ATTR);
        if (startTime instanceof Long time) {
            return System.currentTimeMillis() - time;
        }
        return 0L;
    }

    /**
     * 构建归属地文本
     *
     * @param regionInfo IP 归属地信息
     * @return 归属地文本
     */
    private String buildRegionText(IpRegionInfo regionInfo) {
        if (regionInfo == null) {
            return StrUtil.EMPTY;
        }

        String regionText = StrUtil.format("{}{}{}",
                StrUtil.blankToDefault(regionInfo.getCountry(), StrUtil.EMPTY),
                StrUtil.blankToDefault(regionInfo.getProvince(), StrUtil.EMPTY),
                StrUtil.blankToDefault(regionInfo.getCity(), StrUtil.EMPTY)
        );

        if (StrUtil.isNotBlank(regionInfo.getIsp())) {
            return StrUtil.format("{} {}", regionText, regionInfo.getIsp());
        }

        return regionText;
    }

}
```

需要将拦截器注册到 Spring MVC 中。

文件位置：`src/main/java/io/github/atengk/ipregion/config/WebMvcConfig.java`

下面的配置类用于注册访问日志拦截器，并排除部分无需记录的接口路径。

```java
package io.github.atengk.ipregion.config;

import io.github.atengk.ipregion.web.AccessLogInterceptor;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * Web MVC 配置
 *
 * @author Ateng
 * @since 2026-05-06
 */
@Configuration
@RequiredArgsConstructor
public class WebMvcConfig implements WebMvcConfigurer {

    private final AccessLogInterceptor accessLogInterceptor;

    /**
     * 注册拦截器
     *
     * @param registry 拦截器注册器
     */
    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(accessLogInterceptor)
                .addPathPatterns("/**")
                .excludePathPatterns(
                        "/actuator/**",
                        "/favicon.ico",
                        "/error"
                );
    }

}
```

访问日志输出示例：

```text
接口访问日志，IP地址：114.114.114.114，归属地：中国江苏省南京市，请求方法：GET，请求路径：/api/ip-region/current，状态码：200，耗时：15ms
```

如果项目已经有统一的访问日志表，可以在 `afterCompletion` 中将 `IpRegionInfo` 的字段写入数据库，例如 `ip_country`、`ip_province`、`ip_city`、`ip_isp`、`ip_raw_region`。如果接口访问量较大，不建议每次都同步写库，可以改为写入 MQ 或日志平台。

### 用户登录地区识别

用户登录地区识别用于在用户登录成功后记录登录 IP 和归属地信息。该信息通常用于最近登录记录、账号安全提醒、异常登录识别和后台审计。

推荐在登录成功后调用 `IpRegionService`，将客户端 IP、国家、省份、城市、运营商等信息写入登录日志表。不要在登录失败前频繁查询 IP 归属地，避免被恶意请求放大查询压力。

先定义登录请求对象。

文件位置：`src/main/java/io/github/atengk/ipregion/model/LoginRequest.java`

```java
package io.github.atengk.ipregion.model;

import lombok.Data;

/**
 * 登录请求参数
 *
 * @author Ateng
 * @since 2026-05-06
 */
@Data
public class LoginRequest {

    /**
     * 用户名
     */
    private String username;

    /**
     * 密码
     */
    private String password;

}
```

再定义登录结果对象。

文件位置：`src/main/java/io/github/atengk/ipregion/model/LoginResult.java`

```java
package io.github.atengk.ipregion.model;

import lombok.Builder;
import lombok.Data;

/**
 * 登录结果
 *
 * @author Ateng
 * @since 2026-05-06
 */
@Data
@Builder
public class LoginResult {

    /**
     * 用户名
     */
    private String username;

    /**
     * 访问令牌
     */
    private String token;

    /**
     * 登录 IP
     */
    private String loginIp;

    /**
     * 登录地区
     */
    private String loginRegion;

}
```

下面的示例 Service 演示在登录成功后识别登录地区。实际项目中，用户名密码校验、Token 生成和登录日志入库应替换为项目已有逻辑。

文件位置：`src/main/java/io/github/atengk/ipregion/service/UserLoginService.java`

```java
package io.github.atengk.ipregion.service;

import cn.hutool.core.util.IdUtil;
import cn.hutool.core.util.StrUtil;
import io.github.atengk.ipregion.model.IpRegionInfo;
import io.github.atengk.ipregion.model.LoginRequest;
import io.github.atengk.ipregion.model.LoginResult;
import io.github.atengk.ipregion.util.ClientIpUtils;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

/**
 * 用户登录服务
 *
 * @author Ateng
 * @since 2026-05-06
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class UserLoginService {

    private final IpRegionService ipRegionService;

    /**
     * 用户登录
     *
     * @param loginRequest 登录请求
     * @param request      HTTP 请求
     * @return 登录结果
     */
    public LoginResult login(LoginRequest loginRequest, HttpServletRequest request) {
        if (loginRequest == null || StrUtil.isBlank(loginRequest.getUsername()) || StrUtil.isBlank(loginRequest.getPassword())) {
            throw new IllegalArgumentException("用户名或密码不能为空");
        }

        // 示例：这里仅演示流程，真实项目应替换为数据库用户校验、密码匹配和账号状态检查
        String username = loginRequest.getUsername();
        String token = IdUtil.fastSimpleUUID();

        String clientIp = ClientIpUtils.getClientIp(request);
        IpRegionInfo regionInfo = ipRegionService.search(clientIp);
        String loginRegion = buildLoginRegion(regionInfo);

        log.info("用户登录成功，用户名：{}，登录IP：{}，登录地区：{}", username, clientIp, loginRegion);

        // 示例：这里可以将 username、clientIp、regionInfo、loginTime 等信息写入登录日志表
        return LoginResult.builder()
                .username(username)
                .token(token)
                .loginIp(clientIp)
                .loginRegion(loginRegion)
                .build();
    }

    /**
     * 构建登录地区
     *
     * @param regionInfo IP 归属地信息
     * @return 登录地区
     */
    private String buildLoginRegion(IpRegionInfo regionInfo) {
        if (regionInfo == null) {
            return StrUtil.EMPTY;
        }

        String regionText = StrUtil.format("{}{}{}",
                StrUtil.blankToDefault(regionInfo.getCountry(), StrUtil.EMPTY),
                StrUtil.blankToDefault(regionInfo.getProvince(), StrUtil.EMPTY),
                StrUtil.blankToDefault(regionInfo.getCity(), StrUtil.EMPTY)
        );

        if (StrUtil.isNotBlank(regionInfo.getIsp())) {
            return StrUtil.format("{} {}", regionText, regionInfo.getIsp());
        }

        return StrUtil.blankToDefault(regionText, StrUtil.EMPTY);
    }

}
```

提供一个登录接口用于验证效果。

文件位置：`src/main/java/io/github/atengk/ipregion/controller/LoginController.java`

```java
package io.github.atengk.ipregion.controller;

import io.github.atengk.ipregion.model.LoginRequest;
import io.github.atengk.ipregion.model.LoginResult;
import io.github.atengk.ipregion.service.UserLoginService;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

/**
 * 用户登录接口
 *
 * @author Ateng
 * @since 2026-05-06
 */
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/auth")
public class LoginController {

    private final UserLoginService userLoginService;

    /**
     * 用户登录
     *
     * @param loginRequest 登录请求
     * @param request      HTTP 请求
     * @return 登录结果
     */
    @PostMapping("/login")
    public LoginResult login(@RequestBody LoginRequest loginRequest, HttpServletRequest request) {
        return userLoginService.login(loginRequest, request);
    }

}
```

接口测试命令如下：

```bash
curl -X POST "http://localhost:8080/api/auth/login" \
  -H "Content-Type: application/json" \
  -H "X-Forwarded-For: 114.114.114.114" \
  -d '{
    "username": "admin",
    "password": "123456"
  }'
```

响应示例：

```json
{
  "username": "admin",
  "token": "6d23b6406ec34d5c8c2f5f0de6c4a11d",
  "loginIp": "114.114.114.114",
  "loginRegion": "中国江苏省南京市"
}
```

在生产环境中，登录地区识别建议只在登录成功后执行，并将解析结果与登录日志一起持久化。对于登录失败日志，也可以记录 IP 和地区，但需要结合限流策略，避免恶意请求频繁触发查询。

## 异常与边界处理

本章节用于说明 ip2region 集成过程中常见的异常和边界场景，包括非法 IP、xdb 文件加载失败和查询结果为空。异常处理的目标是保证业务系统可控降级，避免 IP 归属地查询失败影响核心业务流程。

### 非法 IP 处理

非法 IP 是业务接口中最常见的边界输入，包括空字符串、格式错误、多个 IP 拼接错误、`unknown`、`localhost`、非法 IPv6 字符串等。对于非法 IP，推荐默认返回空归属地对象，并记录警告日志；如果是管理端主动查询接口，可以直接返回参数错误。

常见非法输入示例：

| 输入值                      | 问题说明                 | 建议处理               |
| --------------------------- | ------------------------ | ---------------------- |
| 空字符串                    | 没有传入 IP              | 返回空对象或参数错误。 |
| `unknown`                   | 代理 Header 中常见无效值 | 忽略该 Header。        |
| `localhost`                 | 不是标准 IP 地址         | 不执行归属地查询。     |
| `114.114.114.114, 10.0.0.1` | 多级代理 IP 未拆分       | 取第一个有效 IP。      |
| `999.999.999.999`           | 非法 IPv4                | 返回空对象或参数错误。 |

可以定义统一异常类，用于主动查询接口或严格业务场景。

文件位置：`src/main/java/io/github/atengk/ipregion/exception/IpRegionException.java`

```java
package io.github.atengk.ipregion.exception;

/**
 * IP 归属地查询异常
 *
 * @author Ateng
 * @since 2026-05-06
 */
public class IpRegionException extends RuntimeException {

    public IpRegionException(String message) {
        super(message);
    }

    public IpRegionException(String message, Throwable cause) {
        super(message, cause);
    }

}
```

如果需要对 Controller 参数做严格处理，可以新增一个严格查询方法，非法 IP 时直接抛出异常。

文件位置：`src/main/java/io/github/atengk/ipregion/service/StrictIpRegionService.java`

```java
package io.github.atengk.ipregion.service;

import cn.hutool.core.util.StrUtil;
import io.github.atengk.ipregion.exception.IpRegionException;
import io.github.atengk.ipregion.model.IpRegionInfo;
import io.github.atengk.ipregion.util.IpAddressUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

/**
 * 严格 IP 归属地查询服务
 *
 * @author Ateng
 * @since 2026-05-06
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class StrictIpRegionService {

    private final IpRegionService ipRegionService;

    /**
     * 严格查询 IP 归属地
     *
     * @param ip IP 地址
     * @return IP 归属地信息
     */
    public IpRegionInfo searchStrict(String ip) {
        String normalizedIp = IpAddressUtils.normalize(ip);
        if (StrUtil.isBlank(normalizedIp)) {
            throw new IpRegionException("IP地址不能为空");
        }

        if (!IpAddressUtils.isValidIp(normalizedIp)) {
            log.warn("严格IP归属地查询失败，非法IP地址：{}", normalizedIp);
            throw new IpRegionException("非法IP地址：" + normalizedIp);
        }

        return ipRegionService.search(normalizedIp);
    }

}
```

可以配合全局异常处理器统一返回错误响应。

文件位置：`src/main/java/io/github/atengk/ipregion/exception/GlobalExceptionHandler.java`

```java
package io.github.atengk.ipregion.exception;

import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * 全局异常处理器
 *
 * @author Ateng
 * @since 2026-05-06
 */
@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    /**
     * 处理 IP 归属地异常
     *
     * @param e IP 归属地异常
     * @return 错误响应
     */
    @ExceptionHandler(IpRegionException.class)
    public Map<String, Object> handleIpRegionException(IpRegionException e) {
        log.warn("IP归属地业务异常：{}", e.getMessage());

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("code", "IP_REGION_ERROR");
        result.put("message", e.getMessage());
        return result;
    }

    /**
     * 处理参数异常
     *
     * @param e 参数异常
     * @return 错误响应
     */
    @ExceptionHandler(IllegalArgumentException.class)
    public Map<String, Object> handleIllegalArgumentException(IllegalArgumentException e) {
        log.warn("请求参数异常：{}", e.getMessage());

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("code", "PARAM_ERROR");
        result.put("message", e.getMessage());
        return result;
    }

}
```

实际项目中，如果已经有统一响应对象，例如 `Result<T>`、`R<T>` 或 `ApiResult<T>`，可以将上面的 `Map<String, Object>` 替换为项目统一返回结构。

### xdb 文件加载失败处理

xdb 文件加载失败通常发生在应用启动阶段。常见原因包括文件路径配置错误、文件不存在、文件权限不足、classpath 资源未打包、容器挂载路径错误、xdb 文件损坏等。

常见问题和处理方式如下：

| 问题               | 可能原因               | 处理方式                                |
| ------------------ | ---------------------- | --------------------------------------- |
| 文件不存在         | `xdb-path` 配置错误    | 检查配置路径和文件是否存在。            |
| 权限不足           | 应用用户无读取权限     | 给应用运行用户添加读取权限。            |
| classpath 加载失败 | 文件未放入 `resources` | 检查打包后的 jar 是否包含 xdb 文件。    |
| 容器路径错误       | Docker volume 未挂载   | 检查容器内 `/data/ip2region` 是否存在。 |
| 文件损坏           | xdb 文件不完整         | 重新下载或替换 xdb 文件。               |

推荐在应用启动时主动校验 xdb 文件。如果校验失败，可以选择两种策略：一种是启动失败，适合 IP 归属地是核心功能的系统；另一种是降级启动，适合 IP 归属地只是日志增强能力的系统。

如果希望 xdb 文件加载失败时直接阻止应用启动，可以在初始化阶段抛出异常。前面 `Ip2RegionAutoConfiguration` 中的 `resolveXdbFile` 已经采用这种方式。

如果希望系统降级启动，可以提供一个空实现 Service 作为兜底。该方式适合访问日志增强、登录地区识别等非核心业务场景。

文件位置：`src/main/java/io/github/atengk/ipregion/service/impl/EmptyIpRegionServiceImpl.java`

下面的空实现用于在 ip2region 不可用时返回空归属地对象，避免影响主流程。

```java
package io.github.atengk.ipregion.service.impl;

import cn.hutool.core.util.StrUtil;
import io.github.atengk.ipregion.model.IpRegionInfo;
import io.github.atengk.ipregion.service.IpRegionService;
import lombok.extern.slf4j.Slf4j;

/**
 * 空 IP 归属地查询服务实现
 *
 * @author Ateng
 * @since 2026-05-06
 */
@Slf4j
public class EmptyIpRegionServiceImpl implements IpRegionService {

    /**
     * 返回空 IP 归属地信息
     *
     * @param ip IP 地址
     * @return 空 IP 归属地信息
     */
    @Override
    public IpRegionInfo search(String ip) {
        log.warn("ip2region 查询组件不可用，返回空归属地结果，IP地址：{}", ip);

        return IpRegionInfo.builder()
                .ip(StrUtil.blankToDefault(ip, StrUtil.EMPTY))
                .country(StrUtil.EMPTY)
                .province(StrUtil.EMPTY)
                .city(StrUtil.EMPTY)
                .isp(StrUtil.EMPTY)
                .rawRegion(StrUtil.EMPTY)
                .build();
    }

}
```

如果采用降级启动模式，可以在配置类中捕获初始化异常，并返回空实现。示例逻辑如下：

```java
@Bean
public IpRegionService ipRegionService(Ip2RegionProperties properties) {
    try {
        File xdbFile = this.resolveXdbFile(properties.getXdbPath());
        Searcher searcher = Ip2RegionSearcherFactory.create(xdbFile, properties.getCachePolicy());
        return new IpRegionServiceImpl(searcher, properties);
    } catch (Exception e) {
        log.error("ip2region 初始化失败，启用空查询实现，xdb路径：{}", properties.getXdbPath(), e);
        return new EmptyIpRegionServiceImpl();
    }
}
```

需要注意，如果使用这种降级方式，就不应再单独声明 `Searcher` Bean，否则 `Searcher` 初始化失败仍然会导致应用启动失败。此时应由 `IpRegionService` Bean 内部统一完成初始化和降级处理。

生产环境建议根据业务重要性选择策略：

| 业务定位            | 推荐策略                                 |
| ------------------- | ---------------------------------------- |
| IP 查询是核心功能   | xdb 加载失败时阻止启动。                 |
| IP 查询只是日志增强 | xdb 加载失败时降级为空结果。             |
| IP 查询用于风控判断 | 建议阻止启动或触发告警。                 |
| IP 查询用于运营统计 | 可以降级，但必须记录错误日志和监控告警。 |

### 查询结果为空处理

查询结果为空可能由多种原因导致，例如 IP 不在 xdb 数据范围内、内网 IP、回环地址、保留地址、xdb 数据版本较旧、查询异常被兜底处理等。

常见空结果场景如下：

| 场景       | 示例               | 建议处理                   |
| ---------- | ------------------ | -------------------------- |
| 内网 IP    | `192.168.1.10`     | 标记为内网地址。           |
| 本地回环   | `127.0.0.1`        | 标记为本机地址。           |
| 保留地址   | `0.0.0.0`          | 返回空结果。               |
| 查询无匹配 | xdb 数据未覆盖     | 返回空结果并记录调试日志。 |
| 查询异常   | 文件异常或查询失败 | 按配置返回空对象或抛异常。 |

为了让业务显示更友好，可以增加一个归属地格式化工具类，将空结果、本地 IP、内网 IP 转换为可读文本。

文件位置：`src/main/java/io/github/atengk/ipregion/util/IpRegionFormatUtils.java`

下面的工具类用于统一格式化 IP 归属地展示文本，避免各个业务模块重复拼接字段。

```java
package io.github.atengk.ipregion.util;

import cn.hutool.core.net.NetUtil;
import cn.hutool.core.util.StrUtil;
import io.github.atengk.ipregion.model.IpRegionInfo;

/**
 * IP 归属地格式化工具类
 *
 * @author Ateng
 * @since 2026-05-06
 */
public class IpRegionFormatUtils {

    /**
     * 格式化归属地文本
     *
     * @param regionInfo IP 归属地信息
     * @return 归属地文本
     */
    public static String formatRegionText(IpRegionInfo regionInfo) {
        if (regionInfo == null || StrUtil.isBlank(regionInfo.getIp())) {
            return "未知";
        }

        String ip = regionInfo.getIp();
        if (IpAddressUtils.isLocalIp(ip)) {
            return "本机地址";
        }

        if (NetUtil.isInnerIP(ip)) {
            return "内网地址";
        }

        String regionText = StrUtil.format("{}{}{}",
                StrUtil.blankToDefault(regionInfo.getCountry(), StrUtil.EMPTY),
                StrUtil.blankToDefault(regionInfo.getProvince(), StrUtil.EMPTY),
                StrUtil.blankToDefault(regionInfo.getCity(), StrUtil.EMPTY)
        );

        if (StrUtil.isNotBlank(regionInfo.getIsp())) {
            regionText = StrUtil.format("{} {}", regionText, regionInfo.getIsp());
        }

        return StrUtil.blankToDefault(regionText, "未知");
    }

    /**
     * 判断查询结果是否为空
     *
     * @param regionInfo IP 归属地信息
     * @return 是否为空结果
     */
    public static boolean isEmptyRegion(IpRegionInfo regionInfo) {
        if (regionInfo == null) {
            return true;
        }

        return StrUtil.isAllBlank(
                regionInfo.getCountry(),
                regionInfo.getProvince(),
                regionInfo.getCity(),
                regionInfo.getIsp(),
                regionInfo.getRawRegion()
        );
    }

    private IpRegionFormatUtils() {
    }

}
```

业务代码可以统一使用该工具类处理展示值。

```java
IpRegionInfo regionInfo = ipRegionService.search("192.168.1.10");
String regionText = IpRegionFormatUtils.formatRegionText(regionInfo);
```

对于内网地址，返回示例：

```text
内网地址
```

对于本地回环地址，返回示例：

```text
本机地址
```

对于无法识别的公网 IP，返回示例：

```text
未知
```

查询结果为空时，不建议在访问日志、登录日志等非核心流程中抛出异常。更推荐记录空结果并继续主流程，避免 IP 归属地查询影响用户登录、接口访问和业务操作。对于后台主动查询接口，可以根据产品需求返回“未知”或明确提示“未查询到归属地信息”。



## 测试验证

本章节用于验证 ip2region 查询组件、Web 接口和常见 IP 样例是否符合预期。测试建议分为三类：单元测试验证核心解析逻辑，接口测试验证 Controller 调用链路，样例测试验证常见公网 IP、内网 IP、本机 IP 和非法 IP 的边界表现。

### 单元测试

单元测试主要验证不依赖真实 Web 容器的核心逻辑，例如 IP 地址校验、归属地格式化、查询服务异常兜底和原始结果结构化解析。由于单元测试不应强依赖真实 xdb 文件，可以通过 Mockito 模拟 `Searcher` 返回结果。

测试依赖前面已经在 `pom.xml` 中配置过 `spring-boot-starter-test`，该依赖默认包含 JUnit 5、AssertJ、Mockito 和 Spring Test。

文件位置：`src/test/java/io/github/atengk/ipregion/service/IpRegionServiceImplTest.java`

下面的测试类用于验证 `IpRegionServiceImpl` 在正常结果、空 IP、非法 IP、查询异常等场景下的处理结果。

```java
package io.github.atengk.ipregion.service;

import io.github.atengk.ipregion.config.Ip2RegionProperties;
import io.github.atengk.ipregion.model.IpRegionInfo;
import io.github.atengk.ipregion.service.impl.IpRegionServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.lionsoul.ip2region.xdb.Searcher;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

/**
 * IP 归属地查询服务测试
 *
 * @author Ateng
 * @since 2026-05-06
 */
class IpRegionServiceImplTest {

    private Searcher searcher;

    private IpRegionService ipRegionService;

    @BeforeEach
    void setUp() {
        this.searcher = mock(Searcher.class);

        Ip2RegionProperties properties = new Ip2RegionProperties();
        properties.setReturnEmptyWhenError(true);

        this.ipRegionService = new IpRegionServiceImpl(searcher, properties);
    }

    /**
     * 测试正常 IP 查询
     *
     * @throws Exception 模拟查询异常
     */
    @Test
    void shouldReturnRegionInfoWhenIpValid() throws Exception {
        when(searcher.search("114.114.114.114")).thenReturn("中国|江苏省|南京市|电信|CN");

        IpRegionInfo regionInfo = ipRegionService.search("114.114.114.114");

        assertThat(regionInfo).isNotNull();
        assertThat(regionInfo.getIp()).isEqualTo("114.114.114.114");
        assertThat(regionInfo.getCountry()).isEqualTo("中国");
        assertThat(regionInfo.getProvince()).isEqualTo("江苏省");
        assertThat(regionInfo.getCity()).isEqualTo("南京市");
        assertThat(regionInfo.getIsp()).isEqualTo("电信");
        assertThat(regionInfo.getRawRegion()).isEqualTo("中国|江苏省|南京市|电信|CN");

        verify(searcher, times(1)).search("114.114.114.114");
    }

    /**
     * 测试空 IP 查询
     */
    @Test
    void shouldReturnEmptyWhenIpBlank() {
        IpRegionInfo regionInfo = ipRegionService.search("");

        assertThat(regionInfo).isNotNull();
        assertThat(regionInfo.getIp()).isEmpty();
        assertThat(regionInfo.getCountry()).isEmpty();
        assertThat(regionInfo.getProvince()).isEmpty();
        assertThat(regionInfo.getCity()).isEmpty();
        assertThat(regionInfo.getIsp()).isEmpty();

        verifyNoInteractions(searcher);
    }

    /**
     * 测试非法 IP 查询
     */
    @Test
    void shouldReturnEmptyWhenIpInvalid() {
        IpRegionInfo regionInfo = ipRegionService.search("999.999.999.999");

        assertThat(regionInfo).isNotNull();
        assertThat(regionInfo.getIp()).isEqualTo("999.999.999.999");
        assertThat(regionInfo.getRawRegion()).isEmpty();

        verifyNoInteractions(searcher);
    }

    /**
     * 测试查询异常时返回空结果
     *
     * @throws Exception 模拟查询异常
     */
    @Test
    void shouldReturnEmptyWhenSearcherThrowException() throws Exception {
        when(searcher.search("114.114.114.114")).thenThrow(new RuntimeException("xdb 查询失败"));

        IpRegionInfo regionInfo = ipRegionService.search("114.114.114.114");

        assertThat(regionInfo).isNotNull();
        assertThat(regionInfo.getIp()).isEqualTo("114.114.114.114");
        assertThat(regionInfo.getRawRegion()).isEmpty();

        verify(searcher, times(1)).search("114.114.114.114");
    }

}
```

文件位置：`src/test/java/io/github/atengk/ipregion/util/IpRegionFormatUtilsTest.java`

下面的测试类用于验证归属地格式化工具对公网 IP、内网 IP、本机 IP 和空结果的展示处理。

```java
package io.github.atengk.ipregion.util;

import io.github.atengk.ipregion.model.IpRegionInfo;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * IP 归属地格式化工具测试
 *
 * @author Ateng
 * @since 2026-05-06
 */
class IpRegionFormatUtilsTest {

    /**
     * 测试公网 IP 归属地格式化
     */
    @Test
    void shouldFormatPublicIpRegion() {
        IpRegionInfo regionInfo = IpRegionInfo.builder()
                .ip("114.114.114.114")
                .country("中国")
                .province("江苏省")
                .city("南京市")
                .isp("电信")
                .rawRegion("中国|江苏省|南京市|电信|CN")
                .build();

        String regionText = IpRegionFormatUtils.formatRegionText(regionInfo);

        assertThat(regionText).isEqualTo("中国江苏省南京市 电信");
    }

    /**
     * 测试内网 IP 格式化
     */
    @Test
    void shouldReturnInnerIpText() {
        IpRegionInfo regionInfo = IpRegionInfo.builder()
                .ip("192.168.1.10")
                .build();

        String regionText = IpRegionFormatUtils.formatRegionText(regionInfo);

        assertThat(regionText).isEqualTo("内网地址");
    }

    /**
     * 测试本机 IP 格式化
     */
    @Test
    void shouldReturnLocalIpText() {
        IpRegionInfo regionInfo = IpRegionInfo.builder()
                .ip("127.0.0.1")
                .build();

        String regionText = IpRegionFormatUtils.formatRegionText(regionInfo);

        assertThat(regionText).isEqualTo("本机地址");
    }

    /**
     * 测试空结果格式化
     */
    @Test
    void shouldReturnUnknownWhenRegionEmpty() {
        IpRegionInfo regionInfo = IpRegionInfo.builder()
                .ip("8.8.8.8")
                .build();

        String regionText = IpRegionFormatUtils.formatRegionText(regionInfo);

        assertThat(regionText).isEqualTo("未知");
    }

}
```

执行单元测试：

```bash
mvn test
```

如果只执行指定测试类，可以使用：

```bash
mvn -Dtest=IpRegionServiceImplTest test
mvn -Dtest=IpRegionFormatUtilsTest test
```

上述命令中，`mvn test` 会执行全部测试；`-Dtest=类名` 用于只运行指定测试类，适合开发阶段快速验证某一部分逻辑。

### 接口测试

接口测试用于验证 Controller 层是否可以正常接收请求、解析请求参数、读取代理 Header，并返回结构化 JSON 响应。这里使用 `MockMvc` 进行测试，不需要真正启动 HTTP 端口。

文件位置：`src/test/java/io/github/atengk/ipregion/controller/IpRegionControllerTest.java`

下面的测试类用于验证指定 IP 查询、当前请求 IP 查询和代理 Header 场景。

```java
package io.github.atengk.ipregion.controller;

import io.github.atengk.ipregion.model.IpRegionInfo;
import io.github.atengk.ipregion.service.IpRegionService;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentMatchers;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * IP 归属地查询接口测试
 *
 * @author Ateng
 * @since 2026-05-06
 */
@WebMvcTest(IpRegionController.class)
class IpRegionControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private IpRegionService ipRegionService;

    /**
     * 测试指定 IP 查询接口
     *
     * @throws Exception 接口测试异常
     */
    @Test
    void shouldSearchRegionByRequestParam() throws Exception {
        when(ipRegionService.search("114.114.114.114")).thenReturn(IpRegionInfo.builder()
                .ip("114.114.114.114")
                .country("中国")
                .province("江苏省")
                .city("南京市")
                .isp("电信")
                .rawRegion("中国|江苏省|南京市|电信|CN")
                .build());

        mockMvc.perform(get("/api/ip-region/search")
                        .param("ip", "114.114.114.114"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.ip").value("114.114.114.114"))
                .andExpect(jsonPath("$.country").value("中国"))
                .andExpect(jsonPath("$.province").value("江苏省"))
                .andExpect(jsonPath("$.city").value("南京市"))
                .andExpect(jsonPath("$.isp").value("电信"));
    }

    /**
     * 测试当前请求 IP 查询接口
     *
     * @throws Exception 接口测试异常
     */
    @Test
    void shouldSearchCurrentRequestIpRegion() throws Exception {
        when(ipRegionService.search("114.114.114.114")).thenReturn(IpRegionInfo.builder()
                .ip("114.114.114.114")
                .country("中国")
                .province("江苏省")
                .city("南京市")
                .isp("")
                .rawRegion("中国|江苏省|南京市|0|CN")
                .build());

        mockMvc.perform(get("/api/ip-region/current")
                        .header("X-Forwarded-For", "114.114.114.114, 10.0.0.1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.ip").value("114.114.114.114"))
                .andExpect(jsonPath("$.country").value("中国"))
                .andExpect(jsonPath("$.province").value("江苏省"))
                .andExpect(jsonPath("$.city").value("南京市"));
    }

    /**
     * 测试自动查询接口
     *
     * @throws Exception 接口测试异常
     */
    @Test
    void shouldSearchRegionByAutoQuery() throws Exception {
        when(ipRegionService.search(ArgumentMatchers.anyString())).thenReturn(IpRegionInfo.builder()
                .ip("8.8.8.8")
                .country("美国")
                .province("")
                .city("")
                .isp("")
                .rawRegion("美国|0|0|0|US")
                .build());

        mockMvc.perform(get("/api/ip-region")
                        .param("ip", "8.8.8.8"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.ip").value("8.8.8.8"))
                .andExpect(jsonPath("$.country").value("美国"));
    }

}
```

执行接口测试：

```bash
mvn -Dtest=IpRegionControllerTest test
```

如果项目中启用了 Spring Security、Sa-Token 或其他认证拦截器，需要在接口测试中关闭安全过滤器，或者为测试请求补充认证信息。否则接口测试可能返回 `401` 或 `403`，并不是 ip2region 查询逻辑本身失败。

也可以使用 curl 进行手动接口验证。项目启动后执行：

```bash
# 查询指定 IP
curl "http://localhost:8080/api/ip-region/search?ip=114.114.114.114"

# 查询当前请求 IP
curl "http://localhost:8080/api/ip-region/current"

# 模拟 Nginx 或网关转发 Header
curl -H "X-Forwarded-For: 114.114.114.114, 10.0.0.1" \
  "http://localhost:8080/api/ip-region/current"

# 自动查询，有 ip 参数时使用参数，没有 ip 参数时使用当前请求 IP
curl "http://localhost:8080/api/ip-region?ip=8.8.8.8"
```

预期响应结构如下：

```json
{
  "ip": "114.114.114.114",
  "country": "中国",
  "province": "江苏省",
  "city": "南京市",
  "isp": "电信",
  "rawRegion": "中国|江苏省|南京市|电信|CN"
}
```

接口测试重点不是强校验某个 IP 一定属于某个城市，因为 xdb 数据文件版本不同，返回结果可能存在差异。接口测试应优先校验响应结构、HTTP 状态码、参数传递和 Header 解析逻辑。

### 常见 IP 样例验证

常见 IP 样例验证用于确认不同类型 IP 的处理是否符合预期，包括公网 IP、DNS IP、内网 IP、本机 IP、非法 IP 和多级代理 IP。

建议准备一组固定样例，在开发环境、测试环境和生产环境发布前执行验证。

| 样例 IP                     | 类型        | 预期处理                     |
| --------------------------- | ----------- | ---------------------------- |
| `114.114.114.114`           | 国内公网 IP | 返回国内归属地信息。         |
| `8.8.8.8`                   | 国外公网 IP | 返回国外归属地信息。         |
| `192.168.1.10`              | 内网 IP     | 展示为内网地址或空归属地。   |
| `127.0.0.1`                 | 本机 IP     | 展示为本机地址或空归属地。   |
| `999.999.999.999`           | 非法 IP     | 返回空对象或参数错误。       |
| `114.114.114.114, 10.0.0.1` | 多级代理 IP | Web 层取第一个有效 IP。      |
| `unknown`                   | 无效代理值  | 忽略该 Header 或返回空对象。 |

可以增加一个参数化测试，统一验证多个样例的查询行为。

文件位置：`src/test/java/io/github/atengk/ipregion/util/ClientIpUtilsTest.java`

下面的测试类用于验证从请求 Header 中提取真实客户端 IP 的逻辑。

```java
package io.github.atengk.ipregion.util;

import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 客户端 IP 工具测试
 *
 * @author Ateng
 * @since 2026-05-06
 */
class ClientIpUtilsTest {

    /**
     * 测试从 X-Forwarded-For 获取第一个 IP
     */
    @Test
    void shouldGetFirstIpFromForwardedFor() {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader("X-Forwarded-For", "114.114.114.114, 10.0.0.1");

        String clientIp = ClientIpUtils.getClientIp(request);

        assertThat(clientIp).isEqualTo("114.114.114.114");
    }

    /**
     * 测试忽略 unknown Header
     */
    @Test
    void shouldIgnoreUnknownHeader() {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader("X-Forwarded-For", "unknown");
        request.addHeader("X-Real-IP", "8.8.8.8");

        String clientIp = ClientIpUtils.getClientIp(request);

        assertThat(clientIp).isEqualTo("8.8.8.8");
    }

    /**
     * 测试没有代理 Header 时使用 RemoteAddr
     */
    @Test
    void shouldUseRemoteAddrWhenNoHeader() {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setRemoteAddr("127.0.0.1");

        String clientIp = ClientIpUtils.getClientIp(request);

        assertThat(clientIp).isEqualTo("127.0.0.1");
    }

}
```

如果需要使用真实 xdb 文件进行集成测试，可以在 `src/test/resources/ip2region/ip2region.xdb` 放入测试数据文件，并通过 SpringBootTest 加载真实上下文。

文件位置：`src/test/java/io/github/atengk/ipregion/IpRegionIntegrationTest.java`

下面的集成测试会启动 Spring 容器并调用真实 `IpRegionService`，适合在确认 xdb 文件已准备好后执行。

```java
package io.github.atengk.ipregion;

import io.github.atengk.ipregion.model.IpRegionInfo;
import io.github.atengk.ipregion.service.IpRegionService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * IP 归属地集成测试
 *
 * @author Ateng
 * @since 2026-05-06
 */
@SpringBootTest(properties = {
        "ip2region.enabled=true",
        "ip2region.xdb-path=classpath:ip2region/ip2region.xdb",
        "ip2region.cache-policy=vector-index"
})
class IpRegionIntegrationTest {

    @Autowired
    private IpRegionService ipRegionService;

    /**
     * 测试真实 xdb 文件查询
     */
    @Test
    void shouldSearchRegionByRealXdbFile() {
        IpRegionInfo regionInfo = ipRegionService.search("114.114.114.114");

        assertThat(regionInfo).isNotNull();
        assertThat(regionInfo.getIp()).isEqualTo("114.114.114.114");
        assertThat(regionInfo.getRawRegion()).isNotBlank();
    }

}
```

集成测试不建议强断言 `province` 或 `city` 的具体值，因为不同版本 xdb 数据可能返回略有差异。更稳妥的断言方式是校验 `ip`、`rawRegion`、响应对象不为空，以及关键字段存在即可。

## 部署说明

本章节用于说明 ip2region 在生产环境中的部署方式，包括 xdb 文件是否打包进 jar、容器环境如何挂载外部路径，以及生产环境需要关注的文件更新、权限、缓存模式和监控告警问题。

### xdb 文件打包方式

xdb 文件有两种常见部署方式：一种是随应用一起打包进 jar，另一种是放在服务器或容器的外部目录中。开发环境可以使用 jar 内置方式，生产环境更推荐外部文件方式。

| 方式       | 路径示例                            | 优点                                      | 缺点                        | 推荐场景                                     |
| ---------- | ----------------------------------- | ----------------------------------------- | --------------------------- | -------------------------------------------- |
| 打包进 jar | `classpath:ip2region/ip2region.xdb` | 部署简单，文件随应用发布。                | 更新 xdb 需要重新打包应用。 | 本地开发、测试环境、小型项目。               |
| 外部文件   | `/data/ip2region/ip2region.xdb`     | 可独立更新 xdb 文件，不需要重新构建应用。 | 需要维护服务器路径和权限。  | 生产环境、容器环境、数据需要定期更新的系统。 |

如果选择打包进 jar，需要将 xdb 文件放到 resources 目录。

```text
src/main/resources/ip2region/ip2region.xdb
```

对应配置如下：

```yaml
ip2region:
  # 开发或测试环境可使用 classpath 路径
  xdb-path: classpath:ip2region/ip2region.xdb
  cache-policy: vector-index
```

打包命令如下：

```bash
mvn clean package -DskipTests
```

可以通过以下命令检查 jar 包中是否包含 xdb 文件：

```bash
jar tf target/*.jar | grep ip2region.xdb
```

如果命令输出中可以看到 `BOOT-INF/classes/ip2region/ip2region.xdb`，说明 xdb 文件已经被打包到 jar 内。需要注意，如果代码中使用的是 `File` 方式直接读取 xdb 文件，jar 内资源不能总是被当作普通磁盘文件读取，建议在初始化阶段使用 `ResourceLoader` 读取并复制到临时文件，或者使用支持 `InputStream` 的加载方式。

生产环境推荐使用外部文件路径：

```yaml
ip2region:
  # 生产环境推荐外部路径，便于独立更新 xdb 文件
  xdb-path: /data/ip2region/ip2region.xdb
  cache-policy: vector-index
```

服务器目录准备命令如下：

```bash
# 创建 xdb 文件目录
mkdir -p /data/ip2region

# 上传或复制 xdb 文件
cp ip2region.xdb /data/ip2region/ip2region.xdb

# 设置文件读取权限
chmod 644 /data/ip2region/ip2region.xdb

# 查看文件是否存在
ls -lh /data/ip2region/ip2region.xdb
```

上述命令中，`chmod 644` 表示文件所有者可读写，其他用户可读。实际生产环境需要结合应用运行用户和服务器安全策略设置权限，原则是应用只需要读取权限，不需要写入权限。

### 容器环境路径配置

在 Docker 或 Kubernetes 环境中，推荐将 xdb 文件作为外部文件挂载到容器内固定目录，例如 `/data/ip2region/ip2region.xdb`。这样可以避免 xdb 文件跟随镜像一起发布，也便于后续单独更新数据文件。

Dockerfile 示例：

文件位置：`Dockerfile`

下面的 Dockerfile 用于构建 SpringBoot3 应用镜像，xdb 文件不放入镜像，而是通过运行时挂载提供。

```dockerfile
# 使用 JDK 17 运行 SpringBoot3 应用
FROM eclipse-temurin:17-jre

# 设置工作目录
WORKDIR /app

# 复制应用 jar 包
COPY target/*.jar /app/app.jar

# 设置 JVM 参数和 Spring Profile
ENV JAVA_OPTS="-Xms512m -Xmx512m"
ENV SPRING_PROFILES_ACTIVE="prod"

# 暴露应用端口
EXPOSE 8080

# 启动应用
ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS -jar /app/app.jar"]
```

构建镜像：

```bash
docker build -t springboot-ip2region-demo:1.0.0 .
```

运行容器时挂载 xdb 文件目录：

```bash
docker run -d \
  --name springboot-ip2region-demo \
  -p 8080:8080 \
  -e SPRING_PROFILES_ACTIVE=prod \
  -v /data/ip2region:/data/ip2region:ro \
  springboot-ip2region-demo:1.0.0
```

上述命令中，`-v /data/ip2region:/data/ip2region:ro` 表示将宿主机 `/data/ip2region` 目录只读挂载到容器内相同路径。`:ro` 可以避免容器内应用误修改 xdb 文件。

容器启动后，可以进入容器检查文件是否存在：

```bash
docker exec -it springboot-ip2region-demo sh
ls -lh /data/ip2region/ip2region.xdb
```

如果使用 Docker Compose，可以这样配置：

文件位置：`docker-compose.yml`

下面的 Compose 配置用于启动 SpringBoot3 服务，并将宿主机 xdb 目录只读挂载到容器中。

```yaml
services:
  springboot-ip2region-demo:
    image: springboot-ip2region-demo:1.0.0
    container_name: springboot-ip2region-demo
    ports:
      - "8080:8080"
    environment:
      # 使用生产环境配置
      SPRING_PROFILES_ACTIVE: prod

      # JVM 参数
      JAVA_OPTS: "-Xms512m -Xmx512m"
    volumes:
      # 只读挂载 xdb 文件目录
      - /data/ip2region:/data/ip2region:ro
    restart: always
```

启动命令：

```bash
docker compose up -d
docker compose logs -f springboot-ip2region-demo
```

如果部署在 Kubernetes 中，可以通过 `ConfigMap`、`PersistentVolume` 或镜像内置文件提供 xdb。由于 xdb 文件可能较大，不建议使用普通 ConfigMap 存放较大的二进制文件，更推荐使用 PVC 或镜像外部挂载。

Kubernetes Deployment 示例：

文件位置：`k8s/deployment.yaml`

下面的配置示例通过 PVC 将 xdb 文件挂载到容器内 `/data/ip2region` 目录。

```yaml
apiVersion: apps/v1
kind: Deployment
metadata:
  name: springboot-ip2region-demo
spec:
  replicas: 2
  selector:
    matchLabels:
      app: springboot-ip2region-demo
  template:
    metadata:
      labels:
        app: springboot-ip2region-demo
    spec:
      containers:
        - name: springboot-ip2region-demo
          image: springboot-ip2region-demo:1.0.0
          ports:
            - containerPort: 8080
          env:
            # 使用生产环境配置
            - name: SPRING_PROFILES_ACTIVE
              value: "prod"
            # JVM 参数
            - name: JAVA_OPTS
              value: "-Xms512m -Xmx512m"
          volumeMounts:
            # 只读挂载 xdb 文件目录
            - name: ip2region-data
              mountPath: /data/ip2region
              readOnly: true
      volumes:
        - name: ip2region-data
          persistentVolumeClaim:
            claimName: ip2region-data-pvc
```

容器环境的 `application-prod.yml` 建议固定为外部路径：

```yaml
ip2region:
  enabled: true
  xdb-path: /data/ip2region/ip2region.xdb
  cache-policy: vector-index
  init-on-startup: true
  return-empty-when-error: true
```

部署后可以通过接口验证容器内查询是否正常：

```bash
curl "http://服务器IP:8080/api/ip-region/search?ip=114.114.114.114"
```

如果接口返回空结果或应用启动失败，需要优先检查容器内文件路径、文件权限、挂载目录、Profile 配置和应用日志。

### 生产环境注意事项

生产环境中，ip2region 虽然是本地查询组件，但仍然需要关注数据文件更新、缓存策略、异常降级、接口安全、日志成本和代理 Header 可信度等问题。

第一，xdb 文件应建立更新机制。IP 归属地数据会随运营商、云厂商和网络资源分配变化而变化，如果长期不更新，查询结果可能逐渐失准。生产环境建议将 xdb 文件作为独立资源维护，并记录文件版本、更新时间和来源。

第二，生产环境应优先使用 `vector-index` 或 `memory` 查询模式。`vector-index` 适合大多数系统，内存占用较低且性能稳定；`memory` 适合高频查询场景，但需要评估 xdb 文件大小和 JVM 内存配置。访问量较高时，不建议使用纯文件查询模式。

第三，访问日志增强场景需要控制日志量。如果每个请求都同步查询 IP 并输出完整访问日志，可能增加日志存储和索引成本。对于高 QPS 服务，可以考虑只对关键接口记录归属地，或者将访问日志异步写入 MQ、日志平台、ClickHouse、Elasticsearch 等存储系统。

第四，Header 代理场景必须明确可信代理边界。`X-Forwarded-For` 可以被客户端伪造，应用不应盲目信任公网请求直接携带的 Header。更稳妥的方式是在 Nginx、Ingress、API Gateway 等可信代理层覆盖 Header，并只允许应用从可信代理接收流量。

第五，登录地区识别不能作为唯一安全依据。IP 归属地可能受到代理、VPN、NAT、移动网络出口变化等因素影响，只能作为账号安全和风控分析的辅助字段。异常登录判断应结合设备、时间、行为、账号历史和认证方式综合处理。

第六，异常降级策略要明确。如果 IP 归属地是核心功能，例如专门提供 IP 查询服务，xdb 加载失败时应阻止应用启动；如果只是访问日志增强能力，可以降级为空结果，但必须输出错误日志并接入监控告警。

第七，外部 xdb 文件建议只读挂载。应用进程只需要读取 xdb 文件，不需要写入权限。Docker 和 Kubernetes 中建议使用只读挂载，Linux 文件权限建议遵循最小权限原则。

第八，发布前应执行基础验证。至少需要确认应用启动成功、xdb 文件路径正确、指定 IP 查询正常、Header 代理解析正常、非法 IP 不影响主流程、日志输出符合预期。

生产环境发布检查清单如下：

| 检查项           | 检查方式                               | 预期结果                               |
| ---------------- | -------------------------------------- | -------------------------------------- |
| xdb 文件存在     | `ls -lh /data/ip2region/ip2region.xdb` | 文件存在且大小正常。                   |
| 文件权限正确     | `ls -l /data/ip2region/ip2region.xdb`  | 应用运行用户具备读取权限。             |
| 配置路径正确     | 查看 `application-prod.yml`            | `xdb-path` 指向真实路径。              |
| 应用启动正常     | 查看应用日志                           | Searcher 初始化成功，无 xdb 加载异常。 |
| 接口查询正常     | curl 查询指定 IP                       | 返回结构化归属地 JSON。                |
| 代理 Header 生效 | curl 携带 `X-Forwarded-For`            | 取第一个有效 IP 查询。                 |
| 非法 IP 可控     | 查询非法 IP                            | 返回空结果或参数错误，不出现 500。     |
| 监控告警配置     | 检查日志平台或监控系统                 | xdb 加载失败、查询异常可被发现。       |

发布后可以执行以下命令做基础验证：

```bash
# 查看容器或进程日志
docker logs -f springboot-ip2region-demo

# 验证指定 IP 查询
curl "http://localhost:8080/api/ip-region/search?ip=114.114.114.114"

# 验证代理 Header
curl -H "X-Forwarded-For: 8.8.8.8, 10.0.0.1" \
  "http://localhost:8080/api/ip-region/current"

# 验证非法 IP
curl "http://localhost:8080/api/ip-region/search?ip=999.999.999.999"
```

对于生产系统，建议将 ip2region 初始化成功日志作为发布检查项之一。例如启动日志中应能看到类似内容：

```text
初始化 ip2region 查询组件，xdb文件：/data/ip2region/ip2region.xdb，查询模式：vector-index
```

如果启动日志中出现 xdb 文件不存在、权限不足或初始化失败，应先修复部署路径和文件权限，再对外开放服务。