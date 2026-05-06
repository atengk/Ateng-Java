# Spring Boot Banner

## Banner 概述

Banner 是 Spring Boot 应用启动时输出到控制台或日志中的启动标识。它通常以 ASCII 文本形式展示，用于标识当前应用、运行环境、版本信息或团队标识。Spring Boot 支持通过 classpath 下的 `banner.txt` 替换默认 Banner，也支持通过 `spring.banner.location` 指定自定义 Banner 文件位置。([Home](https://docs.spring.io/spring-boot/docs/3.0.0/reference/html/features.html?utm_source=chatgpt.com))

### Banner 的作用

Banner 的主要作用是提升应用启动阶段的可识别性。在本地开发、测试部署、生产发布或多服务排查时，启动日志中的 Banner 可以帮助开发人员快速判断当前启动的是哪个服务、哪个版本以及哪个运行环境。

常见使用场景如下：

| 场景     | 说明                                         |
| -------- | -------------------------------------------- |
| 应用识别 | 展示系统名称、服务名称或模块名称             |
| 环境识别 | 展示 `dev`、`test`、`prod` 等运行环境        |
| 版本识别 | 展示应用版本、构建版本或 Spring Boot 版本    |
| 团队标识 | 展示团队名称、项目 Logo 或组织标识           |
| 启动提示 | 展示文档地址、维护人、注意事项等低频变更信息 |

示例 Banner 文件内容如下。

文件位置：`src/main/resources/banner.txt`

```txt
============================================================
  Spring Boot Banner Demo
============================================================

应用名称：${spring.application.name}
应用版本：${application.formatted-version}
Spring Boot：${spring-boot.formatted-version}
运行环境：${spring.profiles.active}
```

在实际项目中，Banner 不建议承载复杂业务信息。它更适合展示启动阶段需要快速识别的静态或半静态信息，例如应用名、服务名、版本号和环境名。

### Spring Boot 3 中的 Banner 加载机制

Spring Boot 3 的 Banner 加载发生在应用启动早期，由 `SpringApplication` 负责处理。应用执行 `SpringApplication.run(...)` 后，Spring Boot 会准备运行环境，并根据 Banner 相关配置决定是否加载、如何加载以及输出到哪里。

Banner 的加载逻辑可以概括为：

1. Spring Boot 读取 `spring.main.banner-mode`，判断是否需要输出 Banner；
2. 如果 Banner 未关闭，优先检查是否配置了 `spring.banner.location`；
3. 如果没有指定自定义位置，则从 classpath 根路径查找 `banner.txt`；
4. 如果没有找到自定义 Banner，则使用 Spring Boot 内置默认 Banner；
5. 根据 `spring.main.banner-mode` 的值，将 Banner 输出到控制台、日志，或者不输出；
6. 已打印的 Banner 会注册为名为 `springBootBanner` 的单例 Bean。([Home](https://docs.spring.io/spring-boot/docs/3.0.0/reference/html/features.html?utm_source=chatgpt.com))

Banner 文件中可以使用 Spring `Environment` 中的属性，也可以使用 Spring Boot 提供的内置占位符，例如 `${spring-boot.version}`、`${spring-boot.formatted-version}`、`${application.title}`、`${application.version}`、`${application.formatted-version}` 等。([Home](https://docs.spring.io/spring-boot/docs/3.0.0/reference/html/features.html?utm_source=chatgpt.com))

常用占位符如下：

| 占位符                             | 说明                         |
| ---------------------------------- | ---------------------------- |
| `${spring.application.name}`       | 当前应用名称，来自配置文件   |
| `${spring.profiles.active}`        | 当前激活环境                 |
| `${spring-boot.version}`           | 当前使用的 Spring Boot 版本  |
| `${spring-boot.formatted-version}` | 格式化后的 Spring Boot 版本  |
| `${application.title}`             | 应用标题，来自 `MANIFEST.MF` |
| `${application.version}`           | 应用版本，来自 `MANIFEST.MF` |
| `${application.formatted-version}` | 格式化后的应用版本           |

需要注意，`${application.title}`、`${application.version}` 和 `${application.formatted-version}` 依赖打包后的 `MANIFEST.MF` 信息。Spring Boot 文档说明，这些 `application.*` 变量通常在使用 `java -jar` 或 Spring Boot launcher 启动时可用；如果以普通 `java -cp <classpath> <mainclass>` 方式启动未解压应用，这些变量可能无法解析。([docs.enterprise.spring.io](https://docs.enterprise.spring.io/spring-boot/docs/3.2.17/reference/htmlsingle/?utm_source=chatgpt.com))

## Banner 配置方式

Spring Boot 3 中 Banner 的常用配置主要包括默认 Banner 文件、自定义 Banner 文件和关闭 Banner 输出。普通业务项目中，推荐优先使用配置文件方式管理 Banner，避免将环境差异写死在 Java 启动类中。

### 默认 Banner 文件

默认 Banner 文件名为 `banner.txt`，应放在 classpath 根目录下。对于标准 Maven 或 Gradle 项目，通常放在 `src/main/resources/banner.txt`。Spring Boot 会自动识别该文件，并在应用启动时替换默认 Banner。([Home](https://docs.spring.io/spring-boot/docs/3.0.0/reference/html/features.html?utm_source=chatgpt.com))

推荐项目结构如下：

```text
spring-boot-banner-demo
├── pom.xml
└── src
    └── main
        ├── java
        │   └── io
        │       └── github
        │           └── atengk
        │               └── BannerApplication.java
        └── resources
            ├── application.yml
            └── banner.txt
```

默认 Banner 文件示例。

文件位置：`src/main/resources/banner.txt`

```txt
  ____                              ____              _
 / ___| _ __  _ __(_)_ __   __ _  | __ )  ___   ___ | |_
 \___ \| '_ \| '__| | '_ \ / _` | |  _ \ / _ \ / _ \| __|
  ___) | |_) | |  | | | | | (_| | | |_) | (_) | (_) | |_
 |____/| .__/|_|  |_|_| |_|\__, | |____/ \___/ \___/ \__|
       |_|                 |___/

应用名称：${spring.application.name}
运行环境：${spring.profiles.active}
Spring Boot：${spring-boot.formatted-version}
```

基础配置如下。

文件位置：`src/main/resources/application.yml`

```yaml
spring:
  application:
    # 应用名称，可在 banner.txt 中通过 ${spring.application.name} 引用
    name: spring-boot-banner-demo

  profiles:
    # 当前激活环境，可在 banner.txt 中通过 ${spring.profiles.active} 引用
    active: dev
```

启动应用后，控制台会优先输出 `src/main/resources/banner.txt` 中定义的内容。如果没有该文件，Spring Boot 会输出内置默认 Banner。

### 自定义 Banner 文件

如果项目不希望使用默认文件名 `banner.txt`，或者需要按目录、环境、模块管理多个 Banner 文件，可以通过 `spring.banner.location` 指定 Banner 文件位置。Banner 文件默认使用 UTF-8 编码；如果文件使用其他编码，可以通过 `spring.banner.charset` 指定编码。([Home](https://docs.spring.io/spring-boot/docs/3.0.0/reference/html/features.html?utm_source=chatgpt.com))

推荐目录结构如下：

```text
src/main/resources
├── application.yml
└── banner
    ├── banner-dev.txt
    ├── banner-test.txt
    └── banner-prod.txt
```

开发环境 Banner 示例。

文件位置：`src/main/resources/banner/banner-dev.txt`

```txt
============================================================
  DEV 环境启动
============================================================

应用名称：${spring.application.name}
运行环境：dev
Spring Boot：${spring-boot.formatted-version}
```

生产环境 Banner 示例。

文件位置：`src/main/resources/banner/banner-prod.txt`

```txt
============================================================
  PROD 环境启动
============================================================

应用名称：${spring.application.name}
运行环境：prod
应用版本：${application.formatted-version}
Spring Boot：${spring-boot.formatted-version}
```

通过 `application.yml` 指定自定义 Banner 文件位置。

文件位置：`src/main/resources/application.yml`

```yaml
spring:
  application:
    # 应用名称
    name: spring-boot-banner-demo

  banner:
    # 指定自定义 Banner 文件位置
    location: classpath:banner/banner-dev.txt

    # Banner 文件编码，默认 UTF-8
    charset: UTF-8
```

如果需要按环境切换 Banner，可以将 Banner 位置写入不同的 profile 配置文件中。

文件位置：`src/main/resources/application-dev.yml`

```yaml
spring:
  banner:
    # 开发环境 Banner
    location: classpath:banner/banner-dev.txt
```

文件位置：`src/main/resources/application-test.yml`

```yaml
spring:
  banner:
    # 测试环境 Banner
    location: classpath:banner/banner-test.txt
```

文件位置：`src/main/resources/application-prod.yml`

```yaml
spring:
  banner:
    # 生产环境 Banner
    location: classpath:banner/banner-prod.txt
```

启动生产环境示例：

```bash
java -jar target/spring-boot-banner-demo.jar --spring.profiles.active=prod
```

该命令会激活 `prod` 环境，并读取 `application-prod.yml` 中配置的 Banner 文件位置。

### 关闭 Banner 输出

如果项目不需要启动 Banner，可以通过 `spring.main.banner-mode` 关闭。该配置支持 `console`、`log` 和 `off` 三种模式，分别表示输出到控制台、输出到日志系统和关闭输出。([Home](https://docs.spring.io/spring-boot/docs/3.0.0/reference/html/features.html?utm_source=chatgpt.com))

常用取值如下：

| 配置值    | 说明                       |
| --------- | -------------------------- |
| `console` | 输出到控制台，常用默认方式 |
| `log`     | 输出到日志系统             |
| `off`     | 关闭 Banner 输出           |

通过 `application.yml` 关闭 Banner。

文件位置：`src/main/resources/application.yml`

```yaml
spring:
  main:
    # 关闭 Spring Boot 启动 Banner
    banner-mode: off
```

通过 `application.properties` 关闭 Banner。

文件位置：`src/main/resources/application.properties`

```properties
# 关闭 Spring Boot 启动 Banner
spring.main.banner-mode=off
```

通过启动参数临时关闭 Banner。

```bash
java -jar target/spring-boot-banner-demo.jar --spring.main.banner-mode=off
```

通过 Java 启动类关闭 Banner。

文件位置：`src/main/java/io/github/atengk/BannerApplication.java`

```java
package io.github.atengk;

import org.springframework.boot.Banner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * Banner 示例应用启动类
 *
 * @author Ateng
 * @since 2026-05-06
 */
@SpringBootApplication
public class BannerApplication {

    /**
     * 应用启动入口
     *
     * @param args 启动参数
     */
    public static void main(String[] args) {
        SpringApplication application = new SpringApplication(BannerApplication.class);

        // 关闭启动 Banner 输出
        application.setBannerMode(Banner.Mode.OFF);

        application.run(args);
    }

}
```

配置文件方式适合大多数业务项目；启动参数方式适合临时覆盖或容器部署；代码方式更适合统一启动器、脚手架工程或平台级基础工程。普通 Spring Boot 项目中，建议优先使用 `spring.main.banner-mode=off` 控制 Banner 输出，避免将环境差异固化到启动类代码中。



## Banner 内容开发

Banner 内容开发主要关注三个方面：文本内容、ANSI 样式和变量占位符。Spring Boot 3 支持在 `banner.txt` 中编写普通文本，也支持使用 `Environment` 中的配置属性和 Spring Boot 提供的内置 Banner 变量。官方文档说明，Banner 可以通过 classpath 中的 `banner.txt` 替换，也可以通过 `spring.banner.location` 指定位置；如果编码不是 UTF-8，可以通过 `spring.banner.charset` 设置编码。([Home](https://docs.spring.io/spring-boot/docs/3.0.0/reference/html/features.html?utm_source=chatgpt.com))

### 文本 Banner

文本 Banner 是最常见的 Banner 形式，通常使用 ASCII 字符、分隔线和少量说明信息组成。它的优势是兼容性好、维护简单、不会依赖图片渲染能力，适合大多数 Spring Boot 3 项目。

推荐将文本 Banner 放在默认路径：

```text
src/main/resources/banner.txt
```

一个基础文本 Banner 示例：

文件位置：`src/main/resources/banner.txt`

```txt
============================================================
  Spring Boot Banner Demo
============================================================

应用名称：${spring.application.name}
运行环境：${spring.profiles.active}
Spring Boot：${spring-boot.formatted-version}

启动说明：服务启动中，请查看后续日志确认端口和健康状态。
============================================================
```

如果需要更明显的项目标识，可以使用 ASCII 风格的应用名称：

文件位置：`src/main/resources/banner.txt`

```txt
 ____                              ____              _
/ ___| _ __  _ __(_)_ __   __ _  | __ )  ___   ___ | |_
\___ \| '_ \| '__| | '_ \ / _` | |  _ \ / _ \ / _ \| __|
 ___) | |_) | |  | | | | | (_| | | |_) | (_) | (_) | |_
|____/| .__/|_|  |_|_| |_|\__, | |____/ \___/ \___/ \__|
      |_|                 |___/

应用名称：${spring.application.name}
应用版本：${application.formatted-version}
运行环境：${spring.profiles.active}
```

文本 Banner 编写时建议控制宽度，避免在普通终端、Docker 日志、Kubernetes Pod 日志或日志平台中出现换行错位。一般建议单行长度控制在 80 到 120 个字符以内。

### ANSI 颜色样式

Spring Boot Banner 支持 ANSI 占位符，可以在 `banner.txt` 中设置前景色、背景色和文本样式。官方文档说明，Banner 变量中可以使用 `${Ansi.NAME}`，也可以使用 `${AnsiColor.NAME}`、`${AnsiBackground.NAME}`、`${AnsiStyle.NAME}` 这类 ANSI escape code 相关占位符。([Home](https://docs.spring.io/spring-boot/docs/3.0.0/reference/html/features.html?utm_source=chatgpt.com))

常用 ANSI 样式如下：

| 类型   | 示例                  | 说明               |
| ------ | --------------------- | ------------------ |
| 前景色 | `${AnsiColor.GREEN}`  | 设置后续文本为绿色 |
| 前景色 | `${AnsiColor.RED}`    | 设置后续文本为红色 |
| 前景色 | `${AnsiColor.YELLOW}` | 设置后续文本为黄色 |
| 样式   | `${AnsiStyle.BOLD}`   | 设置后续文本加粗   |
| 重置   | `${AnsiStyle.NORMAL}` | 恢复默认样式       |

带颜色的 Banner 示例：

文件位置：`src/main/resources/banner.txt`

```txt
${AnsiColor.GREEN}
============================================================
  Spring Boot Banner Demo
============================================================
${AnsiStyle.NORMAL}

${AnsiColor.CYAN}应用名称：${spring.application.name}${AnsiStyle.NORMAL}
${AnsiColor.YELLOW}运行环境：${spring.profiles.active}${AnsiStyle.NORMAL}
${AnsiColor.BLUE}Spring Boot：${spring-boot.formatted-version}${AnsiStyle.NORMAL}

${AnsiColor.GREEN}服务启动中，请等待端口初始化完成...${AnsiStyle.NORMAL}
```

如果要区分不同环境，可以在不同环境的 Banner 中使用不同颜色。例如开发环境使用绿色，测试环境使用黄色，生产环境使用红色。生产环境 Banner 不建议使用过于复杂的样式，重点应放在环境标识清晰、版本信息准确、日志显示稳定。

生产环境 Banner 示例：

文件位置：`src/main/resources/banner/banner-prod.txt`

```txt
${AnsiColor.RED}${AnsiStyle.BOLD}
============================================================
  PROD ENVIRONMENT
============================================================
${AnsiStyle.NORMAL}

应用名称：${spring.application.name}
应用版本：${application.formatted-version}
运行环境：prod
Spring Boot：${spring-boot.formatted-version}

${AnsiColor.RED}注意：当前为生产环境，请谨慎操作。${AnsiStyle.NORMAL}
```

需要注意，并不是所有运行环境都会完整显示 ANSI 颜色。例如部分日志采集平台、IDE 控制台、CI/CD 日志或容器日志查看器可能会过滤或转义 ANSI 字符。因此，Banner 的核心信息不要只依赖颜色表达，应同时使用明确的文本标识。

### 变量占位符

Spring Boot Banner 支持在 `banner.txt` 中使用变量占位符。占位符可以来自 Spring `Environment`，也可以来自 Spring Boot 内置 Banner 变量。官方文档说明，`banner.txt` 中可以使用 `Environment` 中可用的任意 key，同时也支持 `${application.version}`、`${application.formatted-version}`、`${spring-boot.version}`、`${spring-boot.formatted-version}`、`${application.title}` 等内置变量。([Home](https://docs.spring.io/spring-boot/docs/3.0.0/reference/html/features.html?utm_source=chatgpt.com))

常用变量如下：

| 占位符                             | 说明                         |
| ---------------------------------- | ---------------------------- |
| `${spring.application.name}`       | 应用名称，来自配置文件       |
| `${spring.profiles.active}`        | 当前激活环境                 |
| `${server.port}`                   | 服务端口                     |
| `${spring-boot.version}`           | Spring Boot 版本             |
| `${spring-boot.formatted-version}` | 格式化后的 Spring Boot 版本  |
| `${application.title}`             | 应用标题，来自 `MANIFEST.MF` |
| `${application.version}`           | 应用版本，来自 `MANIFEST.MF` |
| `${application.formatted-version}` | 格式化后的应用版本           |

变量占位符示例：

文件位置：`src/main/resources/banner.txt`

```txt
============================================================
  ${spring.application.name}
============================================================

应用名称：${spring.application.name}
应用版本：${application.formatted-version}
运行环境：${spring.profiles.active}
服务端口：${server.port}
Spring Boot：${spring-boot.formatted-version}

============================================================
```

配套配置如下：

文件位置：`src/main/resources/application.yml`

```yaml
server:
  # 服务端口，可在 Banner 中通过 ${server.port} 引用
  port: 8080

spring:
  application:
    # 应用名称，可在 Banner 中通过 ${spring.application.name} 引用
    name: spring-boot-banner-demo

  profiles:
    # 当前激活环境，可在 Banner 中通过 ${spring.profiles.active} 引用
    active: dev
```

需要注意，`application.title`、`application.version` 和 `application.formatted-version` 依赖应用清单信息。官方文档说明，这些 `application.*` 属性只有在使用 Spring Boot launcher 启动时可用，例如使用 `java -jar` 启动打包后的应用；如果运行未解压 Jar 并通过普通 `java -cp <classpath> <mainclass>` 方式启动，相关变量不会被解析。([Home](https://docs.spring.io/spring-boot/docs/3.0.x/reference/html/features.html?utm_source=chatgpt.com))

## Banner 文件位置

Banner 文件位置决定了 Spring Boot 从哪里读取启动 Banner。常见方式有两种：将 `banner.txt` 放在 classpath 根目录，或者通过配置文件指定 Banner 文件位置。

### classpath 根目录

最推荐的方式是将 `banner.txt` 放在 classpath 根目录。对于标准 Maven 或 Gradle 项目，classpath 根目录通常对应 `src/main/resources` 目录。Spring Boot 官方文档说明，可以通过在 classpath 中添加 `banner.txt` 修改启动 Banner。([Home](https://docs.spring.io/spring-boot/docs/3.0.0/reference/html/features.html?utm_source=chatgpt.com))

标准路径如下：

```text
src/main/resources/banner.txt
```

推荐项目结构：

```text
spring-boot-banner-demo
├── pom.xml
└── src
    └── main
        ├── java
        │   └── io
        │       └── github
        │           └── atengk
        │               └── BannerApplication.java
        └── resources
            ├── application.yml
            └── banner.txt
```

默认 Banner 文件内容：

文件位置：`src/main/resources/banner.txt`

```txt
============================================================
  ${spring.application.name}
============================================================

运行环境：${spring.profiles.active}
服务端口：${server.port}
Spring Boot：${spring-boot.formatted-version}
```

这种方式不需要额外配置，适合只有一个统一 Banner 的项目。只要 `banner.txt` 被正确打包到 classpath 中，Spring Boot 启动时就会自动加载。

可以通过以下命令验证打包后是否包含 `banner.txt`：

```bash
jar tf target/spring-boot-banner-demo.jar | grep banner.txt
```

如果输出中包含类似内容，说明 Banner 文件已经进入 Jar 包：

```text
BOOT-INF/classes/banner.txt
```

### 配置文件指定位置

如果需要使用非默认文件名、按环境区分 Banner，或者将 Banner 文件统一放入子目录，可以通过 `spring.banner.location` 指定文件位置。官方文档和配置属性说明均列出 `spring.banner.location` 用于设置 Banner 文本资源位置，默认值为 `classpath:banner.txt`。([Home](https://docs.spring.io/spring-boot/docs/3.0.0/reference/html/features.html?utm_source=chatgpt.com))

推荐目录结构：

```text
src/main/resources
├── application.yml
└── banner
    ├── banner-dev.txt
    ├── banner-test.txt
    └── banner-prod.txt
```

统一配置示例：

文件位置：`src/main/resources/application.yml`

```yaml
spring:
  banner:
    # 指定 Banner 文件位置
    location: classpath:banner/banner-dev.txt

    # Banner 文件编码，默认 UTF-8
    charset: UTF-8
```

按环境指定 Banner 示例：

文件位置：`src/main/resources/application-dev.yml`

```yaml
spring:
  banner:
    # 开发环境 Banner
    location: classpath:banner/banner-dev.txt
```

文件位置：`src/main/resources/application-test.yml`

```yaml
spring:
  banner:
    # 测试环境 Banner
    location: classpath:banner/banner-test.txt
```

文件位置：`src/main/resources/application-prod.yml`

```yaml
spring:
  banner:
    # 生产环境 Banner
    location: classpath:banner/banner-prod.txt
```

启动指定环境：

```bash
java -jar target/spring-boot-banner-demo.jar --spring.profiles.active=prod
```

此时 Spring Boot 会加载 `application-prod.yml`，并按照其中的 `spring.banner.location` 读取生产环境 Banner。

## 常用配置示例

常用配置主要包括 `application.yml` 和 `application.properties` 两种写法。两者功能一致，区别在于配置格式不同。Spring Boot 官方属性文档中列出了 `spring.banner.charset` 和 `spring.banner.location`，其中 `spring.banner.charset` 默认值为 `UTF-8`，`spring.banner.location` 默认值为 `classpath:banner.txt`。([Home](https://docs.spring.io/spring-boot/appendix/application-properties/?utm_source=chatgpt.com))

### application.yml 配置

`application.yml` 更适合层级较多的 Spring Boot 配置，结构清晰，适合中大型项目使用。Banner 相关配置通常放在 `spring.banner` 和 `spring.main` 下。

文件位置：`src/main/resources/application.yml`

```yaml
server:
  # 服务端口，可在 banner.txt 中通过 ${server.port} 引用
  port: 8080

spring:
  application:
    # 应用名称，可在 banner.txt 中通过 ${spring.application.name} 引用
    name: spring-boot-banner-demo

  profiles:
    # 当前激活环境，可在 banner.txt 中通过 ${spring.profiles.active} 引用
    active: dev

  banner:
    # Banner 文件位置，默认是 classpath:banner.txt
    location: classpath:banner/banner-dev.txt

    # Banner 文件编码，默认 UTF-8
    charset: UTF-8

  main:
    # Banner 输出模式：console 输出到控制台，log 输出到日志，off 关闭
    # YAML 中建议给 off 加引号，避免被解析为布尔值
    banner-mode: "console"
```

如果要关闭 Banner，可以改为：

文件位置：`src/main/resources/application.yml`

```yaml
spring:
  main:
    # 关闭 Banner 输出
    banner-mode: "off"
```

Spring Boot 文档中给出的 YAML 示例也使用 `banner-mode: "off"` 这种写法；这可以避免 YAML 将 `off` 当作布尔值解析。([Home](https://docs.spring.io/spring-boot/3.5/how-to/properties-and-configuration.html?utm_source=chatgpt.com))

### application.properties 配置

`application.properties` 适合配置项较少、偏传统的 Spring Boot 项目。Banner 相关配置使用点号形式书写。

文件位置：`src/main/resources/application.properties`

```properties
# 服务端口，可在 banner.txt 中通过 ${server.port} 引用
server.port=8080

# 应用名称，可在 banner.txt 中通过 ${spring.application.name} 引用
spring.application.name=spring-boot-banner-demo

# 当前激活环境，可在 banner.txt 中通过 ${spring.profiles.active} 引用
spring.profiles.active=dev

# Banner 文件位置，默认是 classpath:banner.txt
spring.banner.location=classpath:banner/banner-dev.txt

# Banner 文件编码，默认 UTF-8
spring.banner.charset=UTF-8

# Banner 输出模式：console 输出到控制台，log 输出到日志，off 关闭
spring.main.banner-mode=console
```

关闭 Banner 的 `properties` 写法如下：

文件位置：`src/main/resources/application.properties`

```properties
# 关闭 Banner 输出
spring.main.banner-mode=off
```

如果通过命令行临时覆盖，也可以这样写：

```bash
java -jar target/spring-boot-banner-demo.jar --spring.main.banner-mode=off
```

`application.yml` 和 `application.properties` 不建议在同一个项目中同时维护同一批 Banner 配置。为了避免配置覆盖关系不清晰，建议统一使用一种格式，并将不同环境的 Banner 文件位置放到对应的 profile 配置文件中。



## 启动效果验证

Banner 配置完成后，需要通过本地启动、Jar 包启动和多环境启动等方式确认显示效果。验证时重点检查三个方面：Banner 是否被加载、占位符是否正确解析、不同环境是否加载了正确的 Banner 文件。Spring Boot 支持通过 classpath 下的 `banner.txt` 或 `spring.banner.location` 指定 Banner 文件，并可通过 `spring.main.banner-mode` 控制输出到控制台、日志或关闭输出。([Home](https://docs.spring.io/spring-boot/reference/features/spring-application.html?utm_source=chatgpt.com))

### 控制台输出验证

控制台输出验证用于确认 Banner 是否正常加载。最直接的方式是在本地启动应用，观察控制台最前面的启动输出内容。

确认默认 Banner 文件存在：

```text
src/main/resources/banner.txt
```

示例 Banner 内容：

文件位置：`src/main/resources/banner.txt`

```txt
============================================================
  ${spring.application.name}
============================================================

应用版本：${application.formatted-version}
运行环境：${spring.profiles.active}
服务端口：${server.port}
Spring Boot：${spring-boot.formatted-version}

服务正在启动，请查看后续日志确认启动状态。
============================================================
```

配套配置如下：

文件位置：`src/main/resources/application.yml`

```yaml
server:
  # 服务端口，可在 Banner 中通过 ${server.port} 引用
  port: 8080

spring:
  application:
    # 应用名称，可在 Banner 中通过 ${spring.application.name} 引用
    name: spring-boot-banner-demo

  profiles:
    # 当前激活环境
    active: dev

  main:
    # Banner 输出到控制台
    banner-mode: "console"
```

使用 Maven 启动应用：

```bash
mvn spring-boot:run
```

或先打包再启动：

```bash
mvn clean package -DskipTests
java -jar target/spring-boot-banner-demo.jar
```

启动成功后，控制台应能看到类似内容：

```text
============================================================
  spring-boot-banner-demo
============================================================

应用版本：(v1.0.0)
运行环境：dev
服务端口：8080
Spring Boot：(v3.x.x)

服务正在启动，请查看后续日志确认启动状态。
============================================================
```

如果控制台没有显示自定义 Banner，应优先检查以下内容：

| 检查项     | 说明                                                         |
| ---------- | ------------------------------------------------------------ |
| 文件路径   | 默认文件必须放在 `src/main/resources/banner.txt`             |
| 文件名     | 默认文件名必须是 `banner.txt`，不是 `Banner.txt` 或 `banner.text` |
| 打包结果   | 打包后 Jar 中应包含 `BOOT-INF/classes/banner.txt`            |
| 输出模式   | `spring.main.banner-mode` 不能配置为 `off`                   |
| 自定义路径 | 如果配置了 `spring.banner.location`，应确认路径真实存在      |
| 编码格式   | 推荐使用 UTF-8，非 UTF-8 需要配置 `spring.banner.charset`    |

可以使用以下命令检查 Jar 包中是否包含 Banner 文件：

```bash
jar tf target/spring-boot-banner-demo.jar | grep banner
```

正常情况下，应看到类似输出：

```text
BOOT-INF/classes/banner.txt
```

如果使用了自定义 Banner 目录，则可能看到：

```text
BOOT-INF/classes/banner/banner-dev.txt
BOOT-INF/classes/banner/banner-test.txt
BOOT-INF/classes/banner/banner-prod.txt
```

### 不同环境下的显示差异

不同环境下的 Banner 验证主要用于确认 `dev`、`test`、`prod` 等环境是否加载了正确的 Banner 文件。该方式常用于微服务项目、生产部署项目和需要明显区分环境的后端系统。

推荐目录结构如下：

```text
src/main/resources
├── application.yml
├── application-dev.yml
├── application-test.yml
├── application-prod.yml
└── banner
    ├── banner-dev.txt
    ├── banner-test.txt
    └── banner-prod.txt
```

公共配置：

文件位置：`src/main/resources/application.yml`

```yaml
server:
  # 服务端口
  port: 8080

spring:
  application:
    # 应用名称
    name: spring-boot-banner-demo

  main:
    # 默认输出 Banner 到控制台
    banner-mode: "console"
```

开发环境配置：

文件位置：`src/main/resources/application-dev.yml`

```yaml
spring:
  banner:
    # 开发环境 Banner
    location: classpath:banner/banner-dev.txt
```

测试环境配置：

文件位置：`src/main/resources/application-test.yml`

```yaml
spring:
  banner:
    # 测试环境 Banner
    location: classpath:banner/banner-test.txt
```

生产环境配置：

文件位置：`src/main/resources/application-prod.yml`

```yaml
spring:
  banner:
    # 生产环境 Banner
    location: classpath:banner/banner-prod.txt
```

开发环境 Banner：

文件位置：`src/main/resources/banner/banner-dev.txt`

```txt
${AnsiColor.GREEN}
============================================================
  DEV ENVIRONMENT
============================================================
${AnsiStyle.NORMAL}

应用名称：${spring.application.name}
运行环境：dev
服务端口：${server.port}
Spring Boot：${spring-boot.formatted-version}
```

测试环境 Banner：

文件位置：`src/main/resources/banner/banner-test.txt`

```txt
${AnsiColor.YELLOW}
============================================================
  TEST ENVIRONMENT
============================================================
${AnsiStyle.NORMAL}

应用名称：${spring.application.name}
运行环境：test
服务端口：${server.port}
Spring Boot：${spring-boot.formatted-version}
```

生产环境 Banner：

文件位置：`src/main/resources/banner/banner-prod.txt`

```txt
${AnsiColor.RED}${AnsiStyle.BOLD}
============================================================
  PROD ENVIRONMENT
============================================================
${AnsiStyle.NORMAL}

应用名称：${spring.application.name}
运行环境：prod
服务端口：${server.port}
Spring Boot：${spring-boot.formatted-version}

注意：当前为生产环境，请谨慎操作。
```

分别启动不同环境进行验证：

```bash
java -jar target/spring-boot-banner-demo.jar --spring.profiles.active=dev
java -jar target/spring-boot-banner-demo.jar --spring.profiles.active=test
java -jar target/spring-boot-banner-demo.jar --spring.profiles.active=prod
```

验证结果应满足以下要求：

| 启动环境 | 期望加载文件             | 期望显示内容            |
| -------- | ------------------------ | ----------------------- |
| `dev`    | `banner/banner-dev.txt`  | 显示 `DEV ENVIRONMENT`  |
| `test`   | `banner/banner-test.txt` | 显示 `TEST ENVIRONMENT` |
| `prod`   | `banner/banner-prod.txt` | 显示 `PROD ENVIRONMENT` |

需要注意，ANSI 颜色在不同终端、IDE、CI/CD 平台、容器日志和日志采集平台中的显示效果可能不同。Spring Boot Banner 支持 `${AnsiColor.NAME}`、`${AnsiBackground.NAME}`、`${AnsiStyle.NAME}` 等 ANSI 占位符，但运行环境是否完整显示颜色取决于终端或日志平台能力。([Home](https://docs.spring.io/spring-boot/reference/features/spring-application.html?utm_source=chatgpt.com))

## 实践建议

Banner 本身不是核心业务能力，但它位于应用启动日志的最前面，适合承载应用识别、环境识别和版本识别等信息。实践中应保持 Banner 简洁、稳定、可读，避免将复杂业务说明、敏感信息或频繁变更内容写入 Banner。

### Banner 内容规范

Banner 内容建议遵循“清晰、简短、稳定”的原则。它的目标是帮助开发和运维人员快速识别应用，而不是替代启动日志、配置中心或监控平台。

推荐包含以下信息：

| 信息              | 是否推荐 | 说明                                   |
| ----------------- | -------- | -------------------------------------- |
| 应用名称          | 推荐     | 用于识别当前服务                       |
| 当前环境          | 推荐     | 用于区分 `dev`、`test`、`prod`         |
| 应用版本          | 推荐     | 便于确认部署版本                       |
| Spring Boot 版本  | 可选     | 便于技术排查                           |
| 服务端口          | 可选     | 本地开发和测试环境较有用               |
| 文档地址          | 可选     | 可放内部文档、接口文档或运维说明       |
| 负责人信息        | 谨慎     | 可写团队名称，不建议写个人敏感联系方式 |
| 密钥、Token、账号 | 禁止     | 不应出现在启动日志中                   |

推荐 Banner 内容：

```txt
============================================================
  ${spring.application.name}
============================================================

应用版本：${application.formatted-version}
运行环境：${spring.profiles.active}
服务端口：${server.port}
Spring Boot：${spring-boot.formatted-version}

接口文档：http://localhost:${server.port}/doc.html
============================================================
```

不推荐 Banner 内容：

```txt
============================================================
  PROD SYSTEM
============================================================

数据库地址：jdbc:mysql://10.0.0.12:3306/order
数据库账号：root
数据库密码：123456
AccessToken：xxxxxx
============================================================
```

不推荐的原因是 Banner 会出现在启动日志中，而启动日志通常会被 IDE、服务器、容器平台、CI/CD 系统、日志采集平台或运维平台保存。任何账号、密码、Token、内网敏感地址都不应写入 Banner。

### 多环境 Banner 管理

多环境 Banner 管理建议使用 profile 配置文件完成，不建议在 Java 启动类中根据环境手动判断 Banner 文件。配置文件方式更清晰，也更符合 Spring Boot 外部化配置习惯。

推荐结构：

```text
src/main/resources
├── application.yml
├── application-dev.yml
├── application-test.yml
├── application-prod.yml
└── banner
    ├── banner-dev.txt
    ├── banner-test.txt
    └── banner-prod.txt
```

推荐规则：

| 环境   | Banner 风格 | 内容重点                                     |
| ------ | ----------- | -------------------------------------------- |
| `dev`  | 简洁、可读  | 应用名、端口、开发环境标识                   |
| `test` | 明确、区分  | 应用名、测试环境标识、版本信息               |
| `prod` | 稳定、醒目  | 应用名、生产环境标识、版本信息、谨慎操作提示 |

生产环境 Banner 建议控制内容，避免复杂颜色和过长 ASCII 图案：

```txt
============================================================
  PROD - ${spring.application.name}
============================================================

应用版本：${application.formatted-version}
运行环境：prod
Spring Boot：${spring-boot.formatted-version}

注意：当前为生产环境，请确认发布版本和变更窗口。
============================================================
```

多环境启动命令示例：

```bash
# 开发环境
java -jar target/spring-boot-banner-demo.jar --spring.profiles.active=dev

# 测试环境
java -jar target/spring-boot-banner-demo.jar --spring.profiles.active=test

# 生产环境
java -jar target/spring-boot-banner-demo.jar --spring.profiles.active=prod
```

如果使用 Docker 或 Kubernetes 部署，可以通过环境变量或启动参数控制环境：

```bash
java -jar app.jar --spring.profiles.active=${SPRING_PROFILES_ACTIVE}
```

对应 Docker 运行示例：

```bash
docker run -e SPRING_PROFILES_ACTIVE=prod spring-boot-banner-demo:1.0.0
```

这种方式可以让同一个镜像在不同环境加载不同 profile 配置，从而加载不同 Banner 文件。

### 常见问题处理

Banner 常见问题通常集中在文件未加载、变量未解析、颜色不显示和关闭配置不生效几个方面。排查时建议优先检查文件位置、配置项、启动参数和打包结果。

| 问题                             | 可能原因                          | 处理方式                                                  |
| -------------------------------- | --------------------------------- | --------------------------------------------------------- |
| 自定义 Banner 没有显示           | `banner.txt` 路径错误             | 确认文件位于 `src/main/resources/banner.txt`              |
| 仍显示 Spring Boot 默认 Banner   | 自定义文件未进入 classpath        | 检查 Jar 中是否包含 `BOOT-INF/classes/banner.txt`         |
| 完全没有 Banner                  | `spring.main.banner-mode=off`     | 修改为 `console` 或删除该配置                             |
| 自定义路径不生效                 | `spring.banner.location` 配置错误 | 检查路径是否为 `classpath:banner/banner-dev.txt`          |
| 中文乱码                         | 文件编码不一致                    | 使用 UTF-8 保存文件，或配置 `spring.banner.charset=UTF-8` |
| ANSI 颜色不显示                  | 终端或日志平台不支持              | 保留文本标识，不只依赖颜色区分环境                        |
| `${application.version}` 未解析  | 启动方式或清单信息不满足条件      | 使用 `java -jar` 启动，并确认打包清单包含版本信息         |
| `${spring.profiles.active}` 为空 | 未激活任何 profile                | 启动时添加 `--spring.profiles.active=dev`                 |
| YAML 中 `off` 异常               | `off` 可能被 YAML 当作布尔值处理  | 建议写成 `banner-mode: "off"`                             |

检查 Banner 是否被打包：

```bash
jar tf target/spring-boot-banner-demo.jar | grep banner
```

检查当前启动环境：

```bash
java -jar target/spring-boot-banner-demo.jar --spring.profiles.active=dev
```

临时关闭 Banner 验证：

```bash
java -jar target/spring-boot-banner-demo.jar --spring.main.banner-mode=off
```

临时指定 Banner 文件验证：

```bash
java -jar target/spring-boot-banner-demo.jar \
  --spring.banner.location=classpath:banner/banner-test.txt \
  --spring.profiles.active=test
```

如果使用 `${application.version}`、`${application.formatted-version}` 或 `${application.title}` 这类变量，需要注意它们来自 `MANIFEST.MF`。Spring Boot 官方文档说明，这些 `application.*` Banner 变量只有在使用 Spring Boot launcher 启动时可用，例如 `java -jar`；如果使用普通 `java -cp <classpath> <mainclass>` 方式启动未解压应用，相关变量可能无法解析。([Home](https://docs.spring.io/spring-boot/reference/features/spring-application.html?utm_source=chatgpt.com))

最终建议如下：

| 建议项   | 推荐做法                                    |
| -------- | ------------------------------------------- |
| 文件位置 | 单环境使用 `src/main/resources/banner.txt`  |
| 多环境   | 使用 `spring.banner.location` 配合 profile  |
| 输出模式 | 默认使用 `spring.main.banner-mode=console`  |
| 关闭方式 | 使用配置文件或启动参数，不建议写死在代码中  |
| 内容长度 | 控制 Banner 高度和单行宽度，避免日志冗长    |
| 敏感信息 | 禁止写入账号、密码、Token、内网敏感地址     |
| 版本变量 | 打包后使用 `java -jar` 验证                 |
| 颜色样式 | 可以使用 ANSI，但不能只依赖颜色表达环境差异 |