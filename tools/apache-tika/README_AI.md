# Apache Tika 文档解析

Apache Tika 文档解析用于在 Spring Boot 3 项目中实现常见文档的文本提取、元数据读取和文件类型识别。该能力通常作为文件上传、全文检索、知识库、内容审核、文档预览等业务的基础能力，为后续存储、检索、分析和 AI 处理提供结构化文本数据。

## 项目概述

本章节说明 Apache Tika 文档解析模块在系统中的职责边界、适用场景和技术组合。该模块重点解决“文件内容如何被识别和提取”的问题，不直接承担文件存储、权限控制、全文索引等外围业务职责。

### 功能定位

Apache Tika 文档解析模块主要定位为通用文档内容抽取组件。它对上层业务提供统一的解析入口，对底层文件格式差异进行屏蔽，使业务代码不需要分别处理 PDF、Word、Excel、PPT、TXT、HTML 等不同格式。

在 Spring Boot 3 项目中，该模块通常位于文件上传服务和业务处理服务之间。用户上传文件后，系统先完成文件大小、文件类型和基础安全校验，再调用 Tika 进行内容解析，最后将解析结果返回给业务层使用。

核心能力包括：

| 功能           | 说明                                                         |
| -------------- | ------------------------------------------------------------ |
| 文本提取       | 从 PDF、Word、Excel、PPT、TXT、HTML 等文件中提取正文文本     |
| 元数据提取     | 读取文件标题、作者、创建时间、修改时间、页数、字符集等信息   |
| 文件类型识别   | 识别文件真实 MIME 类型，降低仅依赖扩展名带来的误判风险       |
| 多格式统一解析 | 对不同文档格式提供统一解析入口，减少业务代码复杂度           |
| 异常统一封装   | 对文件损坏、格式不支持、文件过大、解析失败等情况进行统一处理 |

实际开发中，不建议在 Controller 层直接使用 Tika API。更合理的方式是封装 `DocumentParseService`、`TikaParseHelper` 或类似工具类，由服务层统一处理解析逻辑、异常转换、日志记录和返回结构。

### 应用场景

Apache Tika 适合用于需要从非结构化文件中提取文本内容的业务系统。它的核心价值在于支持格式广、调用方式统一，能够降低系统对多个文件解析库的直接依赖。

常见应用场景包括：

| 场景         | 说明                                                         |
| ------------ | ------------------------------------------------------------ |
| 文档全文检索 | 提取文档正文后写入 Elasticsearch、OpenSearch 或数据库全文索引 |
| 企业知识库   | 将 PDF、Word、PPT 等资料解析为文本，作为知识库内容来源       |
| 文件内容审核 | 对上传文档中的文本进行敏感词检测、合规审查或风险识别         |
| 附件内容预览 | 提取文档纯文本，用于生成摘要、搜索片段或简单预览内容         |
| 文档归档入库 | 将文件正文和元数据结构化保存，便于后续查询和统计             |
| AI 文档问答  | 将解析后的文本切分并写入向量数据库，作为 RAG 知识来源        |

如果系统中存在大量文件解析任务，建议将解析逻辑设计为独立服务能力。文件服务负责上传和存储，解析服务负责文本抽取，检索服务负责索引构建。这样后续扩展异步解析、大文件队列、OCR 识别或分布式解析时，系统改动会更小。

### 技术选型

本项目采用 Spring Boot 3 + Apache Tika 作为核心技术组合。Spring Boot 3 负责接口开发、文件上传、参数配置和异常处理；Apache Tika 负责文件类型检测、文本提取和元数据读取。

推荐技术选型如下：

| 技术        | 推荐版本 | 作用                                                      |
| ----------- | -------- | --------------------------------------------------------- |
| JDK         | 17 或 21 | Spring Boot 3 要求至少 Java 17，生产环境建议使用 LTS 版本 |
| Spring Boot | 3.5.14   | 构建 REST 接口、配置管理、文件上传和异常处理              |
| Apache Tika | 3.3.0    | 实现文档检测、文本提取和元数据提取                        |
| Maven       | 3.6.3+   | 项目构建与依赖管理                                        |
| Hutool      | 5.8.x    | 文件名、字符串、集合、异常信息等通用工具处理              |
| Lombok      | 1.18.x   | 简化 DTO、VO、配置类代码                                  |

Spring Boot 3.5.14 已发布并可从 Maven Central 获取；Spring Boot 3 体系要求至少 Java 17，Maven 构建工具建议使用 3.6.3 或更高版本。([Home](https://spring.io/blog/2026/04/23/spring-boot-3-5-14-available-now?utm_source=chatgpt.com))

Apache Tika 推荐使用 3.3.0。Maven Central 已提供 `tika-core` 3.3.0 版本，适合作为 Spring Boot 3 项目中的 Tika 版本基线。([ipv6.repo1.maven.org](https://ipv6.repo1.maven.org/maven2/org/apache/tika/tika-core/3.3.0/?utm_source=chatgpt.com))

## 环境准备

本章节用于明确开发 Apache Tika 文档解析模块所需的基础环境。版本需要尽量固定，避免不同开发环境下出现依赖冲突、解析结果不一致或线上线下行为差异。

### JDK 与 Spring Boot 版本

Spring Boot 3 项目建议使用 JDK 17 或 JDK 21。JDK 17 是最低要求，JDK 21 是较新的长期支持版本。对于已有项目，可以优先遵循公司基础镜像和 CI/CD 环境；对于新项目，可以优先选择 JDK 21。

推荐配置如下：

| 环境项       | 推荐值     | 说明                               |
| ------------ | ---------- | ---------------------------------- |
| JDK          | 17 或 21   | Spring Boot 3 至少需要 Java 17     |
| Spring Boot  | 3.5.14     | Spring Boot 3 当前可用稳定版本之一 |
| Maven        | 3.6.3+     | 满足 Spring Boot 3 构建要求        |
| 项目编码     | UTF-8      | 避免中文文件名和中文内容解析乱码   |
| 文件上传大小 | 按业务设置 | 例如单文件 50MB、单次请求 100MB    |

可以通过以下命令检查本地开发环境：

```bash
# 查看 Java 版本
java -version

# 查看 Maven 版本
mvn -version
```

如果项目使用 Docker 部署，建议开发环境、构建环境和运行环境使用一致的 JDK 主版本。例如本地使用 JDK 17，CI/CD 和运行镜像也应优先使用 JDK 17，减少因 JDK 差异导致的字体、字符集、临时文件和解析行为问题。

### Apache Tika 版本选择

Apache Tika 版本需要结合 Spring Boot 3、JDK 版本和项目稳定性要求选择。对于 Spring Boot 3 新项目，建议使用 Apache Tika 3.x，不建议继续使用较旧的 1.x 版本。

推荐依赖组合如下：

| 依赖                            | 推荐版本 | 说明                                                   |
| ------------------------------- | -------- | ------------------------------------------------------ |
| `tika-core`                     | 3.3.0    | Tika 核心 API，提供类型检测、元数据、门面类等基础能力  |
| `tika-parsers-standard-package` | 3.3.0    | 标准解析器集合，支持 PDF、Office、HTML、TXT 等常见格式 |
| `tika-bom`                      | 3.3.0    | 统一管理 Tika 模块版本，减少依赖版本不一致问题         |

版本选择建议：

1. 如果只需要文件类型识别，可以只引入 `tika-core`。
2. 如果需要解析 PDF、Word、Excel、PPT、HTML、TXT 等常见文档，需要引入 `tika-parsers-standard-package`。
3. 如果项目中使用多个 Tika 模块，建议通过 `tika-bom` 统一管理版本。
4. 如果需要图片文字识别，Tika 不能完全替代 OCR，需要额外结合 Tesseract、PaddleOCR 或第三方 OCR 服务。
5. 如果业务对安全要求较高，需要限制上传文件大小、解析超时时间和可解析文件类型，避免异常文件消耗过多系统资源。

### Maven 依赖配置

下面给出 Spring Boot 3 集成 Apache Tika 的 Maven 配置。该配置使用 `spring-boot-starter-parent` 管理 Spring Boot 依赖版本，并通过 `tika-bom` 统一管理 Tika 相关依赖版本。

文件位置：`pom.xml`

```xml
<project xmlns="http://maven.apache.org/POM/4.0.0"
         xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
         xsi:schemaLocation="http://maven.apache.org/POM/4.0.0 https://maven.apache.org/xsd/maven-4.0.0.xsd">

    <modelVersion>4.0.0</modelVersion>

    <!-- Spring Boot 3 父工程，用于统一管理 Spring 生态依赖版本 -->
    <parent>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-starter-parent</artifactId>
        <version>3.5.14</version>
        <relativePath/>
    </parent>

    <groupId>io.github.atengk</groupId>
    <artifactId>springboot3-tika-document-parse</artifactId>
    <version>1.0.0</version>
    <name>springboot3-tika-document-parse</name>
    <description>Spring Boot 3 集成 Apache Tika 实现文档解析</description>

    <properties>
        <!-- Spring Boot 3 最低要求 Java 17 -->
        <java.version>17</java.version>

        <!-- Apache Tika 统一版本 -->
        <tika.version>3.3.0</tika.version>

        <!-- Hutool 工具类版本 -->
        <hutool.version>5.8.38</hutool.version>
    </properties>

    <dependencyManagement>
        <dependencies>
            <!-- Apache Tika BOM：统一管理 Tika 模块版本 -->
            <dependency>
                <groupId>org.apache.tika</groupId>
                <artifactId>tika-bom</artifactId>
                <version>${tika.version}</version>
                <type>pom</type>
                <scope>import</scope>
            </dependency>
        </dependencies>
    </dependencyManagement>

    <dependencies>
        <!-- Web 接口依赖：用于提供文件上传和文档解析接口 -->
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-web</artifactId>
        </dependency>

        <!-- 参数校验依赖：用于校验上传参数、文件大小和业务参数 -->
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-validation</artifactId>
        </dependency>

        <!-- Apache Tika 核心依赖：提供 Tika 门面类、Metadata、MediaType、Detector 等能力 -->
        <dependency>
            <groupId>org.apache.tika</groupId>
            <artifactId>tika-core</artifactId>
        </dependency>

        <!-- Apache Tika 标准解析器包：支持 PDF、Office、HTML、TXT 等常见文档格式 -->
        <dependency>
            <groupId>org.apache.tika</groupId>
            <artifactId>tika-parsers-standard-package</artifactId>
            <type>pom</type>
        </dependency>

        <!-- Hutool 工具包：用于文件名、字符串、集合和异常信息处理 -->
        <dependency>
            <groupId>cn.hutool</groupId>
            <artifactId>hutool-all</artifactId>
            <version>${hutool.version}</version>
        </dependency>

        <!-- Lombok：简化 DTO、VO、配置类中的样板代码 -->
        <dependency>
            <groupId>org.projectlombok</groupId>
            <artifactId>lombok</artifactId>
            <optional>true</optional>
        </dependency>

        <!-- 测试依赖：用于单元测试和接口测试 -->
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-test</artifactId>
            <scope>test</scope>
        </dependency>
    </dependencies>

    <build>
        <plugins>
            <!-- Spring Boot Maven 插件：用于打包可执行 Jar -->
            <plugin>
                <groupId>org.springframework.boot</groupId>
                <artifactId>spring-boot-maven-plugin</artifactId>
            </plugin>
        </plugins>
    </build>
</project>
```

依赖配置完成后，可以执行以下命令验证项目依赖是否正常：

```bash
# 清理并编译项目，验证依赖是否能正常下载和编译
mvn clean compile

# 查看 Tika 相关依赖树，确认 Tika 模块版本是否一致
mvn dependency:tree -Dincludes=org.apache.tika
```

如果执行 `mvn clean compile` 成功，说明基础依赖配置正常。如果依赖树中出现多个不同版本的 Tika 模块，需要优先检查是否有其他依赖间接引入了旧版本 Tika，并通过 `dependencyManagement` 统一版本。



## 基础配置

本章节用于定义文档解析模块运行时需要的基础参数，包括文件上传大小、解析文本长度、允许解析的文件类型和临时文件目录。配置应尽量集中在 `application.yml` 中，避免解析规则散落在 Controller 或 Service 代码里。

### 文件上传配置

Spring Boot 3 默认支持 `multipart/form-data` 文件上传，但实际项目中必须显式配置单文件大小、单次请求大小和上传开关。文档解析通常涉及 PDF、Word、Excel、PPT 等文件，文件体积可能较大，因此需要根据业务场景设置合理限制。

文件位置：`src/main/resources/application.yml`

```yaml
server:
  port: 8080

spring:
  servlet:
    multipart:
      # 是否启用文件上传
      enabled: true
      # 单个文件最大大小
      max-file-size: 50MB
      # 单次请求最大大小
      max-request-size: 100MB
      # 文件达到该阈值后写入磁盘临时目录，避免全部占用内存
      file-size-threshold: 2MB
```

上述配置中，`max-file-size` 控制单个上传文件的最大大小，`max-request-size` 控制一次请求中所有文件和表单参数的总大小。对于文档解析接口，建议先从 50MB 开始限制，后续根据大文件解析耗时、内存占用和业务需求调整。

### 解析参数配置

解析参数用于控制 Tika 解析行为，例如最大文本长度、允许的扩展名、允许的 MIME 类型和解析超时提示。由于 Tika 支持的文件格式非常多，生产环境不建议对所有格式完全放开，应根据业务需求设置白名单。

文件位置：`src/main/resources/application.yml`

```yaml
document:
  parse:
    # 是否启用文档解析功能
    enabled: true

    # 解析出的最大文本长度，避免超大文档导致内存占用过高
    max-text-length: 200000

    # 是否提取元数据
    extract-metadata: true

    # 是否在返回结果中包含空元数据字段
    include-empty-metadata: false

    # 允许解析的文件扩展名
    allowed-extensions:
      - pdf
      - doc
      - docx
      - xls
      - xlsx
      - ppt
      - pptx
      - txt
      - html
      - htm
      - md
      - csv

    # 允许解析的 MIME 类型
    allowed-content-types:
      - application/pdf
      - application/msword
      - application/vnd.openxmlformats-officedocument.wordprocessingml.document
      - application/vnd.ms-excel
      - application/vnd.openxmlformats-officedocument.spreadsheetml.sheet
      - application/vnd.ms-powerpoint
      - application/vnd.openxmlformats-officedocument.presentationml.presentation
      - text/plain
      - text/html
      - text/markdown
      - text/csv
```

为了让配置能够被 Spring Boot 自动绑定，需要定义配置属性类。

文件位置：`src/main/java/io/github/atengk/tika/config/DocumentParseProperties.java`

下面的配置类用于接收 `application.yml` 中的 `document.parse` 参数，并在服务层统一读取解析规则。

```java
package io.github.atengk.tika.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

/**
 * 文档解析配置属性
 *
 * @author Ateng
 * @since 2026-05-06
 */
@Data
@Component
@ConfigurationProperties(prefix = "document.parse")
public class DocumentParseProperties {

    /**
     * 是否启用文档解析
     */
    private Boolean enabled = true;

    /**
     * 最大文本解析长度
     */
    private Integer maxTextLength = 200000;

    /**
     * 是否提取元数据
     */
    private Boolean extractMetadata = true;

    /**
     * 是否包含空元数据
     */
    private Boolean includeEmptyMetadata = false;

    /**
     * 允许的文件扩展名
     */
    private List<String> allowedExtensions = new ArrayList<>();

    /**
     * 允许的 MIME 类型
     */
    private List<String> allowedContentTypes = new ArrayList<>();
}
```

### 临时文件配置

文档解析过程中，上传文件可能会先进入 Servlet 容器的临时目录。对于大文件或高并发上传场景，建议显式指定临时目录，避免默认临时目录空间不足或不可控。

文件位置：`src/main/resources/application.yml`

```yaml
spring:
  servlet:
    multipart:
      # 上传临时文件目录，生产环境建议挂载到独立磁盘目录
      location: /data/app/tika/tmp
```

Linux 环境需要提前创建目录并设置应用运行用户的读写权限。

```bash
# 创建文档解析临时目录
mkdir -p /data/app/tika/tmp

# 设置目录权限，appuser 替换为实际运行 Spring Boot 应用的用户
chown -R appuser:appuser /data/app/tika/tmp

# 查看目录权限
ls -ld /data/app/tika/tmp
```

该目录只用于上传过程中的临时文件落盘，不建议作为正式文件存储目录。正式文件应保存到对象存储、文件服务器、NAS 或业务指定的持久化目录中。

## 核心功能设计

本章节说明文档解析模块的核心能力设计。模块应围绕“文本提取、元数据提取、文件类型识别、多格式解析”四类能力展开，并通过统一的服务接口对外提供功能。

### 文档文本提取

文档文本提取是模块的核心功能。业务上传文件后，后端通过 Apache Tika 读取文件输入流，将 PDF、Word、Excel、PPT、TXT、HTML 等文件中的可读文本提取出来，并返回给调用方。

文本提取流程如下：

1. 接收上传文件 `MultipartFile`。
2. 校验文件是否为空。
3. 校验文件扩展名是否在白名单内。
4. 使用 Tika 检测文件真实 MIME 类型。
5. 校验 MIME 类型是否允许解析。
6. 调用 Tika Parser 提取正文文本。
7. 对文本进行清洗，例如去除首尾空白、限制最大长度。
8. 返回解析结果。

文本提取时需要注意两个问题。第一，不能无限制解析超大文本，否则可能造成内存压力。第二，解析结果不一定完全符合原始文档排版，Tika 的输出重点是“文本内容”，不是“版式还原”。

### 文档元数据提取

文档元数据提取用于读取文件本身携带的属性信息，例如标题、作者、创建时间、修改时间、页数、字符集、内容类型等。不同文件格式支持的元数据字段不完全一致，因此返回结果建议使用 `Map<String, String>` 结构。

常见元数据字段包括：

| 元数据             | 说明               |
| ------------------ | ------------------ |
| `Content-Type`     | 文件内容类型       |
| `resourceName`     | 文件资源名称       |
| `title`            | 文档标题           |
| `Author`           | 文档作者           |
| `Creation-Date`    | 创建时间           |
| `Last-Modified`    | 最后修改时间       |
| `xmpTPg:NPages`    | PDF 页数或文档页数 |
| `Content-Encoding` | 内容编码           |

元数据只能作为辅助信息使用，不能完全依赖。部分文档可能没有元数据，部分文档元数据可能被伪造或为空。

### 文件类型识别

文件类型识别用于判断上传文件的真实类型。实际业务中，不能只根据扩展名判断文件类型，因为用户可以将 `.exe`、`.zip` 或其他文件改名为 `.pdf` 上传。

推荐采用“双重校验”策略：

| 校验方式      | 说明                                         |
| ------------- | -------------------------------------------- |
| 扩展名校验    | 根据原始文件名提取扩展名，判断是否在白名单中 |
| MIME 类型检测 | 使用 Tika 根据文件内容检测真实类型           |
| 白名单匹配    | MIME 类型必须在允许列表中                    |
| 异常拦截      | 文件为空、类型未知、类型不支持时直接拒绝     |

文件类型识别可以作为独立接口提供。例如前端上传文件前，先调用类型检测接口，提示用户文件是否支持解析。

### 多格式文件解析

Apache Tika 的优势是通过统一 API 解析多种文件格式。后端不需要为 PDF、Word、Excel、PPT 分别编写完全独立的解析逻辑，而是通过统一的工具类进行处理。

建议支持的基础格式如下：

| 文件类型   | 扩展名                | 说明                    |
| ---------- | --------------------- | ----------------------- |
| PDF        | `.pdf`                | 常见电子文档格式        |
| Word       | `.doc`、`.docx`       | 合同、报告、说明文档    |
| Excel      | `.xls`、`.xlsx`       | 表格、清单、数据文件    |
| PowerPoint | `.ppt`、`.pptx`       | 课件、方案、汇报材料    |
| 文本文档   | `.txt`、`.md`、`.csv` | 普通文本、Markdown、CSV |
| 网页文档   | `.html`、`.htm`       | HTML 页面或富文本内容   |

多格式解析时，接口返回结构应保持一致。无论上传的是 PDF 还是 Word，调用方都应拿到统一的字段，例如文件名、扩展名、内容类型、文本内容、元数据和解析耗时。

## 后端实现

本章节给出 Spring Boot 3 集成 Apache Tika 的关键后端实现。代码采用 Controller、Service、工具类、配置类和 VO 分层设计，便于后续扩展异常处理、异步解析、文件存储和全文检索。

建议项目结构如下：

```text
src/main/java/io/github/atengk/tika
├── config
│   └── DocumentParseProperties.java
├── controller
│   └── DocumentParseController.java
├── service
│   ├── DocumentParseService.java
│   └── impl
│       └── DocumentParseServiceImpl.java
├── support
│   └── TikaParseHelper.java
└── vo
    ├── ApiResult.java
    └── DocumentParseResultVO.java
```

### Tika 解析工具类封装

Tika 解析工具类负责封装 Apache Tika 的底层调用，包括文本提取、元数据读取和 MIME 类型检测。业务服务层只调用工具类，不直接操作 Tika Parser。

文件位置：`src/main/java/io/github/atengk/tika/support/TikaParseHelper.java`

下面的工具类使用 `AutoDetectParser` 执行正文和元数据解析，并使用 `Tika` 门面类执行文件类型检测。

```java
package io.github.atengk.tika.support;

import cn.hutool.core.map.MapUtil;
import cn.hutool.core.util.StrUtil;
import io.github.atengk.tika.config.DocumentParseProperties;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.tika.Tika;
import org.apache.tika.exception.TikaException;
import org.apache.tika.metadata.Metadata;
import org.apache.tika.parser.AutoDetectParser;
import org.apache.tika.parser.ParseContext;
import org.apache.tika.sax.BodyContentHandler;
import org.springframework.stereotype.Component;
import org.xml.sax.SAXException;

import java.io.IOException;
import java.io.InputStream;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Apache Tika 文档解析工具类
 *
 * @author Ateng
 * @since 2026-05-06
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class TikaParseHelper {

    private final DocumentParseProperties documentParseProperties;

    private final Tika tika = new Tika();

    private final AutoDetectParser autoDetectParser = new AutoDetectParser();

    /**
     * 提取文档正文和元数据
     *
     * @param inputStream 文件输入流
     * @param fileName    文件名称
     * @return 解析结果
     */
    public ParsePayload parse(InputStream inputStream, String fileName) {
        Metadata metadata = new Metadata();
        metadata.set(Metadata.RESOURCE_NAME_KEY, StrUtil.blankToDefault(fileName, "unknown"));

        BodyContentHandler handler = new BodyContentHandler(documentParseProperties.getMaxTextLength());
        ParseContext parseContext = new ParseContext();

        try {
            autoDetectParser.parse(inputStream, handler, metadata, parseContext);
            String text = StrUtil.trim(handler.toString());
            Map<String, String> metadataMap = convertMetadata(metadata);

            log.info("文档解析完成，文件名：{}，文本长度：{}，元数据数量：{}",
                    fileName, StrUtil.length(text), metadataMap.size());

            return new ParsePayload(text, metadataMap);
        } catch (IOException | SAXException | TikaException e) {
            log.error("文档解析失败，文件名：{}，原因：{}", fileName, e.getMessage(), e);
            throw new IllegalStateException("文档解析失败：" + e.getMessage(), e);
        }
    }

    /**
     * 检测文件 MIME 类型
     *
     * @param inputStream 文件输入流
     * @param fileName    文件名称
     * @return MIME 类型
     */
    public String detectContentType(InputStream inputStream, String fileName) {
        try {
            String contentType = tika.detect(inputStream, fileName);
            log.info("文件类型检测完成，文件名：{}，MIME类型：{}", fileName, contentType);
            return contentType;
        } catch (IOException e) {
            log.error("文件类型检测失败，文件名：{}，原因：{}", fileName, e.getMessage(), e);
            throw new IllegalStateException("文件类型检测失败：" + e.getMessage(), e);
        }
    }

    /**
     * 转换元数据
     *
     * @param metadata Tika 元数据
     * @return 元数据 Map
     */
    private Map<String, String> convertMetadata(Metadata metadata) {
        Map<String, String> metadataMap = new LinkedHashMap<>();

        for (String name : metadata.names()) {
            String value = metadata.get(name);
            if (documentParseProperties.getIncludeEmptyMetadata() || StrUtil.isNotBlank(value)) {
                metadataMap.put(name, value);
            }
        }

        return MapUtil.sort(metadataMap);
    }

    /**
     * Tika 解析负载
     *
     * @param text     文档正文
     * @param metadata 文档元数据
     */
    public record ParsePayload(String text, Map<String, String> metadata) {
    }
}
```

该工具类中，`BodyContentHandler` 使用 `maxTextLength` 控制最大解析文本长度，避免超大文档一次性提取过多文本。`Metadata.RESOURCE_NAME_KEY` 用于向 Tika 提供文件名信息，有助于部分场景下的类型识别和解析器选择。

### 文件上传接口实现

文件上传接口负责接收前端上传的文档，并调用服务层完成解析。Controller 层只负责参数接收和响应包装，不直接编写解析逻辑。

文件位置：`src/main/java/io/github/atengk/tika/controller/DocumentParseController.java`

下面的 Controller 提供两个接口：一个用于完整解析文档，一个用于检测文件类型。

```java
package io.github.atengk.tika.controller;

import io.github.atengk.tika.service.DocumentParseService;
import io.github.atengk.tika.vo.ApiResult;
import io.github.atengk.tika.vo.DocumentParseResultVO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

/**
 * 文档解析接口
 *
 * @author Ateng
 * @since 2026-05-06
 */
@Slf4j
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/document")
public class DocumentParseController {

    private final DocumentParseService documentParseService;

    /**
     * 解析上传文档
     *
     * @param file 上传文件
     * @return 文档解析结果
     */
    @PostMapping("/parse")
    public ApiResult<DocumentParseResultVO> parse(@RequestPart("file") MultipartFile file) {
        log.info("接收到文档解析请求，文件名：{}，文件大小：{}", file.getOriginalFilename(), file.getSize());
        return ApiResult.success(documentParseService.parse(file));
    }

    /**
     * 检测上传文件类型
     *
     * @param file 上传文件
     * @return 文件类型信息
     */
    @PostMapping("/detect")
    public ApiResult<DocumentParseResultVO> detect(@RequestPart("file") MultipartFile file) {
        log.info("接收到文件类型检测请求，文件名：{}，文件大小：{}", file.getOriginalFilename(), file.getSize());
        return ApiResult.success(documentParseService.detect(file));
    }

    /**
     * 健康检查
     *
     * @return 检查结果
     */
    @GetMapping("/health")
    public ApiResult<String> health() {
        return ApiResult.success("Apache Tika 文档解析服务运行正常");
    }
}
```

接口路径设计如下：

| 接口                   | 方法 | 说明                             |
| ---------------------- | ---- | -------------------------------- |
| `/api/document/parse`  | POST | 上传文件并解析正文、元数据和类型 |
| `/api/document/detect` | POST | 上传文件并检测真实 MIME 类型     |
| `/api/document/health` | GET  | 检查解析服务是否可访问           |

### 文档解析服务实现

文档解析服务负责业务校验、扩展名校验、MIME 类型校验、解析耗时统计和结果组装。这里将上传文件校验和 Tika 工具类调用放在服务层，便于后续扩展统一异常处理或异步解析。

文件位置：`src/main/java/io/github/atengk/tika/service/DocumentParseService.java`

```java
package io.github.atengk.tika.service;

import io.github.atengk.tika.vo.DocumentParseResultVO;
import org.springframework.web.multipart.MultipartFile;

/**
 * 文档解析服务
 *
 * @author Ateng
 * @since 2026-05-06
 */
public interface DocumentParseService {

    /**
     * 解析文档内容
     *
     * @param file 上传文件
     * @return 文档解析结果
     */
    DocumentParseResultVO parse(MultipartFile file);

    /**
     * 检测文件类型
     *
     * @param file 上传文件
     * @return 文件类型检测结果
     */
    DocumentParseResultVO detect(MultipartFile file);
}
```

文件位置：`src/main/java/io/github/atengk/tika/service/impl/DocumentParseServiceImpl.java`

下面的服务实现类完成文件校验、类型检测、正文解析和返回结果组装。

```java
package io.github.atengk.tika.service.impl;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.io.FileUtil;
import cn.hutool.core.io.file.FileNameUtil;
import cn.hutool.core.util.StrUtil;
import io.github.atengk.tika.config.DocumentParseProperties;
import io.github.atengk.tika.service.DocumentParseService;
import io.github.atengk.tika.support.TikaParseHelper;
import io.github.atengk.tika.vo.DocumentParseResultVO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;
import java.time.Duration;
import java.time.Instant;
import java.util.Locale;
import java.util.Map;

/**
 * 文档解析服务实现类
 *
 * @author Ateng
 * @since 2026-05-06
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class DocumentParseServiceImpl implements DocumentParseService {

    private final DocumentParseProperties documentParseProperties;

    private final TikaParseHelper tikaParseHelper;

    /**
     * 解析文档内容
     *
     * @param file 上传文件
     * @return 文档解析结果
     */
    @Override
    public DocumentParseResultVO parse(MultipartFile file) {
        Instant start = Instant.now();
        validateEnabled();
        validateFile(file);

        String originalFilename = StrUtil.blankToDefault(file.getOriginalFilename(), "unknown");
        String extension = getExtension(originalFilename);
        validateExtension(extension);

        String contentType = detectContentType(file, originalFilename);
        validateContentType(contentType);

        TikaParseHelper.ParsePayload payload;
        try (InputStream inputStream = file.getInputStream()) {
            payload = tikaParseHelper.parse(inputStream, originalFilename);
        } catch (IOException e) {
            log.error("读取上传文件失败，文件名：{}，原因：{}", originalFilename, e.getMessage(), e);
            throw new IllegalStateException("读取上传文件失败：" + e.getMessage(), e);
        }

        long costMillis = Duration.between(start, Instant.now()).toMillis();

        DocumentParseResultVO result = DocumentParseResultVO.builder()
                .fileName(originalFilename)
                .extension(extension)
                .fileSize(file.getSize())
                .contentType(contentType)
                .text(payload.text())
                .textLength(StrUtil.length(payload.text()))
                .metadata(payload.metadata())
                .parseCostMillis(costMillis)
                .success(true)
                .message("文档解析成功")
                .build();

        log.info("文档解析成功，文件名：{}，MIME类型：{}，耗时：{}ms",
                originalFilename, contentType, costMillis);

        return result;
    }

    /**
     * 检测文件类型
     *
     * @param file 上传文件
     * @return 文件类型检测结果
     */
    @Override
    public DocumentParseResultVO detect(MultipartFile file) {
        Instant start = Instant.now();
        validateEnabled();
        validateFile(file);

        String originalFilename = StrUtil.blankToDefault(file.getOriginalFilename(), "unknown");
        String extension = getExtension(originalFilename);
        validateExtension(extension);

        String contentType = detectContentType(file, originalFilename);
        validateContentType(contentType);

        long costMillis = Duration.between(start, Instant.now()).toMillis();

        return DocumentParseResultVO.builder()
                .fileName(originalFilename)
                .extension(extension)
                .fileSize(file.getSize())
                .contentType(contentType)
                .text("")
                .textLength(0)
                .metadata(Map.of())
                .parseCostMillis(costMillis)
                .success(true)
                .message("文件类型检测成功")
                .build();
    }

    /**
     * 校验解析功能是否启用
     */
    private void validateEnabled() {
        if (!Boolean.TRUE.equals(documentParseProperties.getEnabled())) {
            log.warn("文档解析功能未启用");
            throw new IllegalStateException("文档解析功能未启用");
        }
    }

    /**
     * 校验上传文件
     *
     * @param file 上传文件
     */
    private void validateFile(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            log.warn("上传文件为空");
            throw new IllegalArgumentException("上传文件不能为空");
        }

        String originalFilename = file.getOriginalFilename();
        if (StrUtil.isBlank(originalFilename)) {
            log.warn("上传文件名称为空");
            throw new IllegalArgumentException("上传文件名称不能为空");
        }
    }

    /**
     * 获取文件扩展名
     *
     * @param fileName 文件名
     * @return 扩展名
     */
    private String getExtension(String fileName) {
        String extension = FileNameUtil.extName(fileName);
        return StrUtil.blankToDefault(extension, "").toLowerCase(Locale.ROOT);
    }

    /**
     * 校验文件扩展名
     *
     * @param extension 文件扩展名
     */
    private void validateExtension(String extension) {
        if (StrUtil.isBlank(extension)) {
            log.warn("文件扩展名为空");
            throw new IllegalArgumentException("文件扩展名不能为空");
        }

        if (CollUtil.isNotEmpty(documentParseProperties.getAllowedExtensions())
                && !documentParseProperties.getAllowedExtensions().contains(extension)) {
            log.warn("不支持的文件扩展名：{}", extension);
            throw new IllegalArgumentException("不支持的文件扩展名：" + extension);
        }
    }

    /**
     * 检测 MIME 类型
     *
     * @param file     上传文件
     * @param fileName 文件名
     * @return MIME 类型
     */
    private String detectContentType(MultipartFile file, String fileName) {
        try (InputStream inputStream = file.getInputStream()) {
            return tikaParseHelper.detectContentType(inputStream, fileName);
        } catch (IOException e) {
            log.error("读取文件类型检测流失败，文件名：{}，原因：{}", fileName, e.getMessage(), e);
            throw new IllegalStateException("读取文件类型检测流失败：" + e.getMessage(), e);
        }
    }

    /**
     * 校验 MIME 类型
     *
     * @param contentType MIME 类型
     */
    private void validateContentType(String contentType) {
        if (StrUtil.isBlank(contentType)) {
            log.warn("文件 MIME 类型为空");
            throw new IllegalArgumentException("文件 MIME 类型不能为空");
        }

        if (CollUtil.isNotEmpty(documentParseProperties.getAllowedContentTypes())
                && !documentParseProperties.getAllowedContentTypes().contains(contentType)) {
            log.warn("不支持的文件 MIME 类型：{}", contentType);
            throw new IllegalArgumentException("不支持的文件 MIME 类型：" + contentType);
        }
    }
}
```

说明：上面代码中引入了 Hutool 的 `StrUtil`、`CollUtil` 和 `FileNameUtil`，用于处理字符串、集合和文件扩展名。服务层同时校验扩展名和 MIME 类型，避免只依赖文件名导致类型伪造问题。

如果实际项目中存在同一文件需要多次读取流的问题，`MultipartFile#getInputStream()` 可以重复获取输入流；如果接入的是对象存储或网络流，则需要先落盘或缓存到临时文件，再执行检测和解析。

### 解析结果返回设计

解析结果返回结构需要兼顾接口稳定性和业务扩展性。建议将文件基础信息、解析文本、元数据、耗时和状态信息统一封装为 VO，方便前端展示和后续写入数据库或搜索引擎。

文件位置：`src/main/java/io/github/atengk/tika/vo/DocumentParseResultVO.java`

下面的 VO 用于承载文档解析接口和类型检测接口的统一返回数据。

```java
package io.github.atengk.tika.vo;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Map;

/**
 * 文档解析结果返回对象
 *
 * @author Ateng
 * @since 2026-05-06
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DocumentParseResultVO {

    /**
     * 原始文件名
     */
    private String fileName;

    /**
     * 文件扩展名
     */
    private String extension;

    /**
     * 文件大小，单位：字节
     */
    private Long fileSize;

    /**
     * MIME 类型
     */
    private String contentType;

    /**
     * 文档文本内容
     */
    private String text;

    /**
     * 文本长度
     */
    private Integer textLength;

    /**
     * 文档元数据
     */
    private Map<String, String> metadata;

    /**
     * 解析耗时，单位：毫秒
     */
    private Long parseCostMillis;

    /**
     * 是否成功
     */
    private Boolean success;

    /**
     * 返回消息
     */
    private String message;
}
```

文件位置：`src/main/java/io/github/atengk/tika/vo/ApiResult.java`

下面的统一响应对象用于包装接口返回结果，实际项目中也可以替换为已有的全局响应结构。

```java
package io.github.atengk.tika.vo;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 接口统一响应对象
 *
 * @author Ateng
 * @since 2026-05-06
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ApiResult<T> {

    /**
     * 状态码
     */
    private Integer code;

    /**
     * 返回消息
     */
    private String message;

    /**
     * 响应数据
     */
    private T data;

    /**
     * 响应时间
     */
    private LocalDateTime timestamp;

    /**
     * 成功响应
     *
     * @param data 响应数据
     * @param <T>  数据类型
     * @return 响应对象
     */
    public static <T> ApiResult<T> success(T data) {
        return new ApiResult<>(200, "操作成功", data, LocalDateTime.now());
    }

    /**
     * 失败响应
     *
     * @param code    状态码
     * @param message 错误消息
     * @param <T>     数据类型
     * @return 响应对象
     */
    public static <T> ApiResult<T> fail(Integer code, String message) {
        return new ApiResult<>(code, message, null, LocalDateTime.now());
    }
}
```

解析接口返回示例：

```json
{
  "code": 200,
  "message": "操作成功",
  "data": {
    "fileName": "项目说明文档.docx",
    "extension": "docx",
    "fileSize": 245760,
    "contentType": "application/vnd.openxmlformats-officedocument.wordprocessingml.document",
    "text": "这里是文档正文内容...",
    "textLength": 12680,
    "metadata": {
      "Content-Type": "application/vnd.openxmlformats-officedocument.wordprocessingml.document",
      "resourceName": "项目说明文档.docx",
      "dc:title": "项目说明文档",
      "Author": "Ateng"
    },
    "parseCostMillis": 356,
    "success": true,
    "message": "文档解析成功"
  },
  "timestamp": "2026-05-06T10:30:00"
}
```

可以使用以下命令验证接口：

```bash
# 解析文档正文和元数据
curl -X POST "http://localhost:8080/api/document/parse" \
  -F "file=@/data/test/项目说明文档.docx"

# 只检测文件类型
curl -X POST "http://localhost:8080/api/document/detect" \
  -F "file=@/data/test/项目说明文档.docx"

# 检查服务状态
curl -X GET "http://localhost:8080/api/document/health"
```

接口验证时建议至少准备以下文件：一个正常 PDF、一个正常 Word、一个正常 Excel、一个正常 PPT、一个普通 TXT，以及一个被错误改名的文件。这样可以同时验证文本提取、元数据读取、MIME 类型识别和异常拦截是否符合预期。



## 接口设计

本章节定义文档解析模块对外提供的 REST 接口，包括文件解析、文件类型检测和元数据读取。接口统一采用 `multipart/form-data` 上传文件，返回结构统一使用 `ApiResult<T>` 包装，便于前端和其他后端服务集成。

接口基础路径建议统一为：

```text
/api/document
```

接口清单如下：

| 接口                     | 请求方式 | 说明                             |
| ------------------------ | -------- | -------------------------------- |
| `/api/document/parse`    | POST     | 解析文件正文、元数据和 MIME 类型 |
| `/api/document/detect`   | POST     | 检测上传文件真实 MIME 类型       |
| `/api/document/metadata` | POST     | 读取上传文件元数据               |

### 文件解析接口

文件解析接口用于接收上传文档，并返回文件基础信息、真实 MIME 类型、正文文本、元数据和解析耗时。该接口适合用于全文检索、知识库入库、文件内容审核和 AI 文档问答等场景。

接口定义如下：

| 项目         | 内容                               |
| ------------ | ---------------------------------- |
| 请求地址     | `/api/document/parse`              |
| 请求方式     | `POST`                             |
| Content-Type | `multipart/form-data`              |
| 请求参数     | `file`                             |
| 返回类型     | `ApiResult<DocumentParseResultVO>` |

请求参数说明：

| 参数名 | 类型            | 是否必填 | 说明             |
| ------ | --------------- | -------- | ---------------- |
| `file` | `MultipartFile` | 是       | 待解析的上传文件 |

返回字段说明：

| 字段              | 类型                  | 说明                    |
| ----------------- | --------------------- | ----------------------- |
| `fileName`        | `String`              | 原始文件名              |
| `extension`       | `String`              | 文件扩展名              |
| `fileSize`        | `Long`                | 文件大小，单位字节      |
| `contentType`     | `String`              | Tika 检测出的 MIME 类型 |
| `text`            | `String`              | 文档正文文本            |
| `textLength`      | `Integer`             | 正文文本长度            |
| `metadata`        | `Map<String, String>` | 文档元数据              |
| `parseCostMillis` | `Long`                | 解析耗时，单位毫秒      |
| `success`         | `Boolean`             | 是否解析成功            |
| `message`         | `String`              | 解析结果说明            |

请求示例：

```bash
# 上传文档并解析正文、元数据和文件类型
curl -X POST "http://localhost:8080/api/document/parse" \
  -F "file=@/data/test/项目说明文档.docx"
```

响应示例：

```json
{
  "code": 200,
  "message": "操作成功",
  "data": {
    "fileName": "项目说明文档.docx",
    "extension": "docx",
    "fileSize": 245760,
    "contentType": "application/vnd.openxmlformats-officedocument.wordprocessingml.document",
    "text": "这里是文档正文内容...",
    "textLength": 12680,
    "metadata": {
      "Content-Type": "application/vnd.openxmlformats-officedocument.wordprocessingml.document",
      "resourceName": "项目说明文档.docx",
      "dc:title": "项目说明文档",
      "Author": "Ateng"
    },
    "parseCostMillis": 356,
    "success": true,
    "message": "文档解析成功"
  },
  "timestamp": "2026-05-06T10:30:00"
}
```

接口处理流程如下：

```text
上传文件
  -> 校验文件是否为空
  -> 校验文件扩展名
  -> 使用 Tika 检测 MIME 类型
  -> 校验 MIME 类型白名单
  -> 使用 Tika 提取正文和元数据
  -> 组装 DocumentParseResultVO
  -> 返回统一响应
```

### 文件类型检测接口

文件类型检测接口用于识别上传文件的真实 MIME 类型。该接口不会提取正文内容，适合用于上传前校验、文件安全检查或前端提示文件是否支持解析。

接口定义如下：

| 项目         | 内容                               |
| ------------ | ---------------------------------- |
| 请求地址     | `/api/document/detect`             |
| 请求方式     | `POST`                             |
| Content-Type | `multipart/form-data`              |
| 请求参数     | `file`                             |
| 返回类型     | `ApiResult<DocumentParseResultVO>` |

请求参数说明：

| 参数名 | 类型            | 是否必填 | 说明             |
| ------ | --------------- | -------- | ---------------- |
| `file` | `MultipartFile` | 是       | 待检测的上传文件 |

请求示例：

```bash
# 上传文件并检测真实 MIME 类型
curl -X POST "http://localhost:8080/api/document/detect" \
  -F "file=@/data/test/项目说明文档.docx"
```

响应示例：

```json
{
  "code": 200,
  "message": "操作成功",
  "data": {
    "fileName": "项目说明文档.docx",
    "extension": "docx",
    "fileSize": 245760,
    "contentType": "application/vnd.openxmlformats-officedocument.wordprocessingml.document",
    "text": "",
    "textLength": 0,
    "metadata": {},
    "parseCostMillis": 42,
    "success": true,
    "message": "文件类型检测成功"
  },
  "timestamp": "2026-05-06T10:30:00"
}
```

文件类型检测接口建议同时保留扩展名校验和 MIME 类型校验。扩展名用于快速过滤明显不支持的文件，MIME 类型用于识别真实内容类型，避免用户通过修改文件后缀绕过限制。

### 元数据读取接口

元数据读取接口用于读取文档中携带的属性信息，例如标题、作者、创建时间、修改时间、页数、字符集和内容类型。该接口通常不返回正文文本，适合用于文件归档、文档信息展示和数据入库前预处理。

接口定义如下：

| 项目         | 内容                               |
| ------------ | ---------------------------------- |
| 请求地址     | `/api/document/metadata`           |
| 请求方式     | `POST`                             |
| Content-Type | `multipart/form-data`              |
| 请求参数     | `file`                             |
| 返回类型     | `ApiResult<DocumentParseResultVO>` |

请求参数说明：

| 参数名 | 类型            | 是否必填 | 说明                   |
| ------ | --------------- | -------- | ---------------------- |
| `file` | `MultipartFile` | 是       | 待读取元数据的上传文件 |

请求示例：

```bash
# 上传文件并读取元数据
curl -X POST "http://localhost:8080/api/document/metadata" \
  -F "file=@/data/test/项目说明文档.docx"
```

响应示例：

```json
{
  "code": 200,
  "message": "操作成功",
  "data": {
    "fileName": "项目说明文档.docx",
    "extension": "docx",
    "fileSize": 245760,
    "contentType": "application/vnd.openxmlformats-officedocument.wordprocessingml.document",
    "text": "",
    "textLength": 0,
    "metadata": {
      "Content-Type": "application/vnd.openxmlformats-officedocument.wordprocessingml.document",
      "resourceName": "项目说明文档.docx",
      "dc:title": "项目说明文档",
      "Author": "Ateng",
      "Creation-Date": "2026-05-06T09:30:00Z"
    },
    "parseCostMillis": 128,
    "success": true,
    "message": "文档元数据读取成功"
  },
  "timestamp": "2026-05-06T10:30:00"
}
```

为了支持元数据读取接口，需要在前文服务接口和 Controller 中补充 `metadata` 方法。

文件位置：`src/main/java/io/github/atengk/tika/service/DocumentParseService.java`

下面的服务接口在原有 `parse` 和 `detect` 基础上增加元数据读取方法。

```java
package io.github.atengk.tika.service;

import io.github.atengk.tika.vo.DocumentParseResultVO;
import org.springframework.web.multipart.MultipartFile;

/**
 * 文档解析服务
 *
 * @author Ateng
 * @since 2026-05-06
 */
public interface DocumentParseService {

    /**
     * 解析文档内容
     *
     * @param file 上传文件
     * @return 文档解析结果
     */
    DocumentParseResultVO parse(MultipartFile file);

    /**
     * 检测文件类型
     *
     * @param file 上传文件
     * @return 文件类型检测结果
     */
    DocumentParseResultVO detect(MultipartFile file);

    /**
     * 读取文档元数据
     *
     * @param file 上传文件
     * @return 文档元数据读取结果
     */
    DocumentParseResultVO metadata(MultipartFile file);
}
```

文件位置：`src/main/java/io/github/atengk/tika/controller/DocumentParseController.java`

下面是在 Controller 中补充的元数据读取接口方法，可直接添加到已有 `DocumentParseController` 类中。

```java
/**
 * 读取上传文件元数据
 *
 * @param file 上传文件
 * @return 文档元数据读取结果
 */
@PostMapping("/metadata")
public ApiResult<DocumentParseResultVO> metadata(@RequestPart("file") MultipartFile file) {
    log.info("接收到文档元数据读取请求，文件名：{}，文件大小：{}", file.getOriginalFilename(), file.getSize());
    return ApiResult.success(documentParseService.metadata(file));
}
```

文件位置：`src/main/java/io/github/atengk/tika/service/impl/DocumentParseServiceImpl.java`

下面是在服务实现类中补充的元数据读取方法，可添加到已有 `DocumentParseServiceImpl` 类中。

```java
/**
 * 读取文档元数据
 *
 * @param file 上传文件
 * @return 文档元数据读取结果
 */
@Override
public DocumentParseResultVO metadata(MultipartFile file) {
    Instant start = Instant.now();
    validateEnabled();
    validateFile(file);

    String originalFilename = StrUtil.blankToDefault(file.getOriginalFilename(), "unknown");
    String extension = getExtension(originalFilename);
    validateExtension(extension);

    String contentType = detectContentType(file, originalFilename);
    validateContentType(contentType);

    TikaParseHelper.ParsePayload payload;
    try (InputStream inputStream = file.getInputStream()) {
        payload = tikaParseHelper.parse(inputStream, originalFilename);
    } catch (IOException e) {
        log.error("读取上传文件失败，文件名：{}，原因：{}", originalFilename, e.getMessage(), e);
        throw new IllegalStateException("读取上传文件失败：" + e.getMessage(), e);
    }

    long costMillis = Duration.between(start, Instant.now()).toMillis();

    log.info("文档元数据读取成功，文件名：{}，元数据数量：{}，耗时：{}ms",
            originalFilename, payload.metadata().size(), costMillis);

    return DocumentParseResultVO.builder()
            .fileName(originalFilename)
            .extension(extension)
            .fileSize(file.getSize())
            .contentType(contentType)
            .text("")
            .textLength(0)
            .metadata(payload.metadata())
            .parseCostMillis(costMillis)
            .success(true)
            .message("文档元数据读取成功")
            .build();
}
```

说明：上述元数据接口为了复用前面的 `TikaParseHelper#parse` 方法，会同时触发一次解析流程，只是在返回结果中不返回正文文本。如果后续需要优化性能，可以在工具类中单独封装 `parseMetadata` 方法，仅提取元数据，减少正文处理开销。

## 异常处理

本章节定义文档解析模块中的异常分类和统一处理方式。文档解析属于非结构化文件处理，实际运行中容易遇到文件为空、文件过大、格式不支持、文件损坏、解析失败等问题，因此需要统一返回明确的错误码和错误信息。

建议定义异常码如下：

| 错误码  | 异常类型     | 说明                               |
| ------- | ------------ | ---------------------------------- |
| `40001` | 文件格式异常 | 文件扩展名或 MIME 类型不在白名单内 |
| `40002` | 文件大小异常 | 上传文件超过系统限制               |
| `40003` | 解析失败异常 | Tika 解析失败、文件损坏或读取异常  |
| `40004` | 文件参数异常 | 文件为空、文件名为空或请求参数缺失 |
| `50000` | 系统异常     | 未预期的服务端异常                 |

建议将文档解析相关异常封装为独立异常类，并在全局异常处理器中统一转换为 `ApiResult`。

### 文件格式异常

文件格式异常用于处理扩展名不支持、MIME 类型不支持、文件真实类型与业务规则不匹配等情况。该异常通常由服务层在扩展名校验和 MIME 类型校验阶段抛出。

文件位置：`src/main/java/io/github/atengk/tika/exception/DocumentFormatException.java`

下面的异常类用于表示文件格式不支持或文件类型不符合解析规则。

```java
package io.github.atengk.tika.exception;

/**
 * 文档格式异常
 *
 * @author Ateng
 * @since 2026-05-06
 */
public class DocumentFormatException extends RuntimeException {

    /**
     * 创建文档格式异常
     *
     * @param message 异常消息
     */
    public DocumentFormatException(String message) {
        super(message);
    }

    /**
     * 创建文档格式异常
     *
     * @param message 异常消息
     * @param cause   原始异常
     */
    public DocumentFormatException(String message, Throwable cause) {
        super(message, cause);
    }
}
```

服务层格式校验建议改为抛出 `DocumentFormatException`。

```java
/**
 * 校验文件扩展名
 *
 * @param extension 文件扩展名
 */
private void validateExtension(String extension) {
    if (StrUtil.isBlank(extension)) {
        log.warn("文件扩展名为空");
        throw new DocumentFormatException("文件扩展名不能为空");
    }

    if (CollUtil.isNotEmpty(documentParseProperties.getAllowedExtensions())
            && !documentParseProperties.getAllowedExtensions().contains(extension)) {
        log.warn("不支持的文件扩展名：{}", extension);
        throw new DocumentFormatException("不支持的文件扩展名：" + extension);
    }
}

/**
 * 校验 MIME 类型
 *
 * @param contentType MIME 类型
 */
private void validateContentType(String contentType) {
    if (StrUtil.isBlank(contentType)) {
        log.warn("文件 MIME 类型为空");
        throw new DocumentFormatException("文件 MIME 类型不能为空");
    }

    if (CollUtil.isNotEmpty(documentParseProperties.getAllowedContentTypes())
            && !documentParseProperties.getAllowedContentTypes().contains(contentType)) {
        log.warn("不支持的文件 MIME 类型：{}", contentType);
        throw new DocumentFormatException("不支持的文件 MIME 类型：" + contentType);
    }
}
```

说明：文件格式异常属于客户端请求问题，接口通常返回 HTTP 400 或业务错误码 `40001`。前端可根据该错误提示用户“当前文件格式不支持解析”。

### 文件大小异常

文件大小异常用于处理上传文件超过 `spring.servlet.multipart.max-file-size` 或 `spring.servlet.multipart.max-request-size` 配置限制的情况。该异常通常在请求进入 Controller 前由 Spring MVC 或 Servlet 容器抛出。

如果使用前文配置：

```yaml
spring:
  servlet:
    multipart:
      max-file-size: 50MB
      max-request-size: 100MB
```

当上传单文件超过 50MB，或者单次请求超过 100MB 时，后端需要返回清晰错误信息，而不是默认的服务器异常页面。

文件大小异常由全局异常处理器统一拦截 `MaxUploadSizeExceededException`。

```java
/**
 * 处理文件大小超过限制异常
 *
 * @param e 文件大小异常
 * @return 统一响应
 */
@ExceptionHandler(MaxUploadSizeExceededException.class)
@ResponseStatus(HttpStatus.BAD_REQUEST)
public ApiResult<Void> handleMaxUploadSizeExceededException(MaxUploadSizeExceededException e) {
    log.warn("上传文件超过大小限制，原因：{}", e.getMessage());
    return ApiResult.fail(40002, "上传文件超过大小限制，请检查 max-file-size 和 max-request-size 配置");
}
```

说明：文件大小限制应同时在后端和前端进行控制。前端限制用于减少无效上传，后端限制用于保证安全性和稳定性，不能只依赖前端判断。

### 解析失败异常

解析失败异常用于处理 Tika 解析过程中出现的问题，例如文件损坏、文件内容异常、解析器不支持、输入流读取失败、文档加密或文本过大等。该类异常通常由 Tika 工具类捕获底层异常后转换抛出。

文件位置：`src/main/java/io/github/atengk/tika/exception/DocumentParseException.java`

下面的异常类用于表示文档解析过程失败。

```java
package io.github.atengk.tika.exception;

/**
 * 文档解析异常
 *
 * @author Ateng
 * @since 2026-05-06
 */
public class DocumentParseException extends RuntimeException {

    /**
     * 创建文档解析异常
     *
     * @param message 异常消息
     */
    public DocumentParseException(String message) {
        super(message);
    }

    /**
     * 创建文档解析异常
     *
     * @param message 异常消息
     * @param cause   原始异常
     */
    public DocumentParseException(String message, Throwable cause) {
        super(message, cause);
    }
}
```

建议将 `TikaParseHelper` 中的 `IllegalStateException` 替换为 `DocumentParseException`。

```java
try {
    autoDetectParser.parse(inputStream, handler, metadata, parseContext);
    String text = StrUtil.trim(handler.toString());
    Map<String, String> metadataMap = convertMetadata(metadata);

    log.info("文档解析完成，文件名：{}，文本长度：{}，元数据数量：{}",
            fileName, StrUtil.length(text), metadataMap.size());

    return new ParsePayload(text, metadataMap);
} catch (IOException | SAXException | TikaException e) {
    log.error("文档解析失败，文件名：{}，原因：{}", fileName, e.getMessage(), e);
    throw new DocumentParseException("文档解析失败，请检查文件是否损坏、加密或格式异常", e);
}
```

文件位置：`src/main/java/io/github/atengk/tika/exception/GlobalExceptionHandler.java`

下面的全局异常处理器用于统一处理文件格式、文件大小、解析失败和通用参数异常。

```java
package io.github.atengk.tika.exception;

import io.github.atengk.tika.vo.ApiResult;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.MissingServletRequestPartException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.multipart.MaxUploadSizeExceededException;

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
     * 处理文件格式异常
     *
     * @param e 文件格式异常
     * @return 统一响应
     */
    @ExceptionHandler(DocumentFormatException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ApiResult<Void> handleDocumentFormatException(DocumentFormatException e) {
        log.warn("文件格式校验失败，原因：{}", e.getMessage());
        return ApiResult.fail(40001, e.getMessage());
    }

    /**
     * 处理文件大小超过限制异常
     *
     * @param e 文件大小异常
     * @return 统一响应
     */
    @ExceptionHandler(MaxUploadSizeExceededException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ApiResult<Void> handleMaxUploadSizeExceededException(MaxUploadSizeExceededException e) {
        log.warn("上传文件超过大小限制，原因：{}", e.getMessage());
        return ApiResult.fail(40002, "上传文件超过大小限制，请上传符合大小限制的文件");
    }

    /**
     * 处理文档解析异常
     *
     * @param e 文档解析异常
     * @return 统一响应
     */
    @ExceptionHandler(DocumentParseException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ApiResult<Void> handleDocumentParseException(DocumentParseException e) {
        log.warn("文档解析失败，原因：{}", e.getMessage());
        return ApiResult.fail(40003, e.getMessage());
    }

    /**
     * 处理文件参数缺失异常
     *
     * @param e 参数缺失异常
     * @return 统一响应
     */
    @ExceptionHandler(MissingServletRequestPartException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ApiResult<Void> handleMissingServletRequestPartException(MissingServletRequestPartException e) {
        log.warn("上传文件参数缺失，参数名：{}", e.getRequestPartName());
        return ApiResult.fail(40004, "缺少上传文件参数：" + e.getRequestPartName());
    }

    /**
     * 处理参数异常
     *
     * @param e 参数异常
     * @return 统一响应
     */
    @ExceptionHandler(IllegalArgumentException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ApiResult<Void> handleIllegalArgumentException(IllegalArgumentException e) {
        log.warn("请求参数校验失败，原因：{}", e.getMessage());
        return ApiResult.fail(40004, e.getMessage());
    }

    /**
     * 处理未知系统异常
     *
     * @param e 未知异常
     * @return 统一响应
     */
    @ExceptionHandler(Exception.class)
    @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
    public ApiResult<Void> handleException(Exception e) {
        log.error("系统异常，原因：{}", e.getMessage(), e);
        return ApiResult.fail(50000, "系统异常，请联系管理员");
    }
}
```

异常响应示例：

```json
{
  "code": 40001,
  "message": "不支持的文件扩展名：exe",
  "data": null,
  "timestamp": "2026-05-06T10:40:00"
}
```

异常处理验证命令如下：

```bash
# 验证文件格式异常：上传不支持的文件类型
curl -X POST "http://localhost:8080/api/document/parse" \
  -F "file=@/data/test/demo.exe"

# 验证文件大小异常：上传超过 max-file-size 限制的文件
curl -X POST "http://localhost:8080/api/document/parse" \
  -F "file=@/data/test/large.pdf"

# 验证文件参数异常：不传 file 参数
curl -X POST "http://localhost:8080/api/document/parse"

# 验证解析失败异常：上传损坏或加密的 PDF 文件
curl -X POST "http://localhost:8080/api/document/parse" \
  -F "file=@/data/test/broken.pdf"
```

通过以上异常处理设计，接口调用方可以明确区分“文件不支持”“文件过大”“解析失败”和“请求参数错误”等不同问题，便于前端展示提示信息，也便于后端通过日志快速定位解析失败原因。



## 功能验证

本章节用于验证 Apache Tika 文档解析模块是否满足基础使用要求。验证范围包括常见格式解析、大文件解析和异常文件处理，重点确认接口可用性、解析结果完整性、异常返回稳定性和日志可追踪性。

建议准备如下测试文件目录：

```text
/data/test/tika
├── normal
│   ├── sample.pdf
│   ├── sample.docx
│   ├── sample.xlsx
│   ├── sample.pptx
│   ├── sample.txt
│   ├── sample.md
│   └── sample.html
├── large
│   └── large.pdf
└── error
    ├── empty.txt
    ├── broken.pdf
    ├── encrypted.pdf
    ├── fake.pdf
    └── unsupported.exe
```

其中 `normal` 目录用于验证常见文档格式，`large` 目录用于验证大文件解析表现，`error` 目录用于验证异常文件处理逻辑。

### 常见文档格式测试

常见文档格式测试用于确认 PDF、Word、Excel、PPT、TXT、Markdown、HTML 等文件是否能够被正常识别和解析。测试重点不是还原文档排版，而是验证正文文本、元数据、MIME 类型和解析耗时是否能正常返回。

建议测试清单如下：

| 文件类型   | 示例文件      | 预期结果                                      |
| ---------- | ------------- | --------------------------------------------- |
| PDF        | `sample.pdf`  | 能提取正文，返回 `application/pdf`            |
| Word       | `sample.docx` | 能提取正文，返回 Office Word MIME 类型        |
| Excel      | `sample.xlsx` | 能提取单元格文本，返回 Office Excel MIME 类型 |
| PowerPoint | `sample.pptx` | 能提取幻灯片文本，返回 Office PPT MIME 类型   |
| TXT        | `sample.txt`  | 能提取纯文本，返回 `text/plain`               |
| Markdown   | `sample.md`   | 能提取 Markdown 文本，返回文本类型            |
| HTML       | `sample.html` | 能提取页面文本，返回 `text/html`              |

可以使用以下脚本批量验证常见格式解析接口。

文件位置：`scripts/test-normal-documents.sh`

```bash
#!/usr/bin/env bash

# 文档解析服务地址
BASE_URL="http://localhost:8080/api/document"

# 测试文件目录
TEST_DIR="/data/test/tika/normal"

# 遍历常见文档并调用解析接口
for file in "${TEST_DIR}"/*; do
  echo "开始解析文件：${file}"

  curl -s -X POST "${BASE_URL}/parse" \
    -F "file=@${file}" \
    | jq '.code, .message, .data.fileName, .data.contentType, .data.textLength, .data.parseCostMillis'

  echo "----------------------------------------"
done
```

执行脚本：

```bash
# 授权脚本执行权限
chmod +x scripts/test-normal-documents.sh

# 执行常见格式解析测试
./scripts/test-normal-documents.sh
```

验证时重点观察以下字段：

| 字段                   | 验证点                   |
| ---------------------- | ------------------------ |
| `code`                 | 正常情况下应返回 `200`   |
| `data.fileName`        | 应与上传文件名一致       |
| `data.contentType`     | 应能识别为真实 MIME 类型 |
| `data.textLength`      | 正常文档应大于 `0`       |
| `data.metadata`        | 能返回部分元数据信息     |
| `data.parseCostMillis` | 应在业务可接受范围内     |

如果 `textLength` 为 `0`，需要结合文件内容判断是否正常。例如空 TXT 文件返回 `0` 是合理结果，但正常 Word 或 PDF 返回 `0` 通常说明文件内容不可提取、文件加密、文件损坏或解析器未正确引入。

可以补充一个 Spring Boot 集成测试，验证接口是否能处理基础上传请求。

文件位置：`src/test/java/io/github/atengk/tika/DocumentParseControllerTest.java`

下面的测试类用于通过 MockMvc 验证文件解析、类型检测和元数据读取接口是否可用。

```java
package io.github.atengk.tika;

import cn.hutool.core.util.CharsetUtil;
import cn.hutool.core.util.StrUtil;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.web.servlet.MockMvc;

import java.nio.charset.StandardCharsets;

import static org.hamcrest.Matchers.greaterThan;
import static org.hamcrest.Matchers.notBlankString;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * 文档解析接口测试
 *
 * @author Ateng
 * @since 2026-05-06
 */
@SpringBootTest
@AutoConfigureMockMvc
class DocumentParseControllerTest {

    @Autowired
    private MockMvc mockMvc;

    /**
     * 测试 TXT 文件解析
     *
     * @throws Exception 测试异常
     */
    @Test
    void shouldParseTxtFile() throws Exception {
        String content = "Apache Tika 文档解析测试内容";
        MockMultipartFile file = new MockMultipartFile(
                "file",
                "sample.txt",
                "text/plain",
                content.getBytes(CharsetUtil.CHARSET_UTF_8)
        );

        mockMvc.perform(multipart("/api/document/parse").file(file))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.fileName").value("sample.txt"))
                .andExpect(jsonPath("$.data.contentType").value("text/plain"))
                .andExpect(jsonPath("$.data.text", notBlankString()))
                .andExpect(jsonPath("$.data.textLength", greaterThan(0)));
    }

    /**
     * 测试文件类型检测
     *
     * @throws Exception 测试异常
     */
    @Test
    void shouldDetectTxtFileType() throws Exception {
        MockMultipartFile file = new MockMultipartFile(
                "file",
                "sample.txt",
                "text/plain",
                StrUtil.bytes("文件类型检测测试", StandardCharsets.UTF_8)
        );

        mockMvc.perform(multipart("/api/document/detect").file(file))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.contentType").value("text/plain"))
                .andExpect(jsonPath("$.data.textLength").value(0));
    }

    /**
     * 测试元数据读取
     *
     * @throws Exception 测试异常
     */
    @Test
    void shouldReadMetadata() throws Exception {
        MockMultipartFile file = new MockMultipartFile(
                "file",
                "sample.txt",
                "text/plain",
                "元数据读取测试".getBytes(CharsetUtil.CHARSET_UTF_8)
        );

        mockMvc.perform(multipart("/api/document/metadata").file(file))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.fileName").value("sample.txt"))
                .andExpect(jsonPath("$.data.metadata").exists());
    }
}
```

执行测试命令：

```bash
# 执行全部测试
mvn test

# 只执行文档解析接口测试
mvn -Dtest=DocumentParseControllerTest test
```

该测试使用 `MockMultipartFile` 构造内存文件，不依赖外部文件路径，适合在 CI/CD 中快速验证接口基础能力。真实 PDF、Word、Excel、PPT 文件仍建议通过接口脚本进行补充验证。

### 大文件解析测试

大文件解析测试用于验证系统在处理较大 PDF、Word、PPT 或 Excel 文件时的稳定性。测试重点包括上传限制是否生效、解析耗时是否可接受、内存是否稳定、临时文件目录是否正常使用。

建议测试维度如下：

| 测试项       | 说明                                           |
| ------------ | ---------------------------------------------- |
| 文件大小     | 覆盖 10MB、30MB、50MB 等不同大小               |
| 文件类型     | 优先测试 PDF、PPT、Excel 等常见大文件          |
| 解析耗时     | 记录 `parseCostMillis`，判断是否满足业务要求   |
| 文本长度限制 | 验证 `document.parse.max-text-length` 是否生效 |
| 临时目录     | 观察 `/data/app/tika/tmp` 是否正常写入和释放   |
| JVM 内存     | 观察解析过程中是否出现频繁 Full GC 或 OOM      |

可以使用以下命令测试大文件解析：

```bash
# 解析大文件
curl -X POST "http://localhost:8080/api/document/parse" \
  -F "file=@/data/test/tika/large/large.pdf"
```

如果需要观察响应耗时，可以使用 `curl` 的时间统计参数。

```bash
# 统计大文件上传和解析耗时
curl -o /tmp/tika-large-result.json \
  -w "HTTP状态码: %{http_code}\n总耗时: %{time_total}s\n上传耗时: %{time_pretransfer}s\n下载大小: %{size_download} bytes\n" \
  -X POST "http://localhost:8080/api/document/parse" \
  -F "file=@/data/test/tika/large/large.pdf"
```

查看解析结果摘要：

```bash
# 查看响应中的关键字段
jq '.code, .message, .data.fileName, .data.fileSize, .data.contentType, .data.textLength, .data.parseCostMillis' \
  /tmp/tika-large-result.json
```

大文件测试通过标准建议如下：

| 指标      | 建议标准                                             |
| --------- | ---------------------------------------------------- |
| HTTP 状态 | 正常文件返回 `200`                                   |
| 解析耗时  | 应在业务可接受范围内，例如 5 秒以内或按业务 SLA 定义 |
| 文本长度  | 不应超过 `document.parse.max-text-length` 配置       |
| 内存表现  | 不应出现 OOM 或频繁 Full GC                          |
| 临时目录  | 不应长期堆积大量临时文件                             |
| 错误响应  | 超过上传大小限制时应返回明确错误码 `40002`           |

如果大文件解析耗时较长，不建议在同步接口中无限等待。可以将解析任务改造成异步任务：上传文件后立即返回任务 ID，由后台线程池、MQ 或 XXL-JOB 执行解析，前端轮询任务状态。

### 异常文件测试

异常文件测试用于验证系统对不合法文件、损坏文件、空文件、伪造后缀文件和加密文件的处理能力。该类测试可以确认异常处理器是否能够返回统一错误码，而不是直接暴露底层异常堆栈。

建议测试清单如下：

| 异常文件     | 示例              | 预期结果                                   |
| ------------ | ----------------- | ------------------------------------------ |
| 空文件       | `empty.txt`       | 返回参数异常或解析结果为空，按业务规则处理 |
| 损坏文件     | `broken.pdf`      | 返回 `40003` 解析失败异常                  |
| 加密文件     | `encrypted.pdf`   | 返回 `40003` 解析失败异常                  |
| 后缀伪造文件 | `fake.pdf`        | MIME 类型检测不匹配时返回格式异常          |
| 不支持格式   | `unsupported.exe` | 返回 `40001` 文件格式异常                  |
| 超大文件     | `too-large.pdf`   | 返回 `40002` 文件大小异常                  |
| 缺少参数     | 不传 `file`       | 返回 `40004` 文件参数异常                  |

可以使用以下脚本批量测试异常文件。

文件位置：`scripts/test-error-documents.sh`

```bash
#!/usr/bin/env bash

# 文档解析服务地址
BASE_URL="http://localhost:8080/api/document"

# 异常文件目录
TEST_DIR="/data/test/tika/error"

# 遍历异常文件并调用解析接口
for file in "${TEST_DIR}"/*; do
  echo "开始测试异常文件：${file}"

  curl -s -X POST "${BASE_URL}/parse" \
    -F "file=@${file}" \
    | jq '.code, .message, .data'

  echo "----------------------------------------"
done

echo "开始测试缺少 file 参数"
curl -s -X POST "${BASE_URL}/parse" | jq '.code, .message, .data'
```

执行脚本：

```bash
# 授权脚本执行权限
chmod +x scripts/test-error-documents.sh

# 执行异常文件测试
./scripts/test-error-documents.sh
```

可以补充以下单元测试，验证异常响应结构。

文件位置：`src/test/java/io/github/atengk/tika/DocumentParseExceptionTest.java`

下面的测试类用于验证空文件、不支持格式和缺少文件参数时的异常返回。

```java
package io.github.atengk.tika;

import cn.hutool.core.util.CharsetUtil;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * 文档解析异常测试
 *
 * @author Ateng
 * @since 2026-05-06
 */
@SpringBootTest
@AutoConfigureMockMvc
class DocumentParseExceptionTest {

    @Autowired
    private MockMvc mockMvc;

    /**
     * 测试不支持的文件扩展名
     *
     * @throws Exception 测试异常
     */
    @Test
    void shouldReturnFormatErrorWhenExtensionUnsupported() throws Exception {
        MockMultipartFile file = new MockMultipartFile(
                "file",
                "unsupported.exe",
                "application/octet-stream",
                "invalid content".getBytes(CharsetUtil.CHARSET_UTF_8)
        );

        mockMvc.perform(multipart("/api/document/parse").file(file))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value(40001));
    }

    /**
     * 测试空文件
     *
     * @throws Exception 测试异常
     */
    @Test
    void shouldReturnParamErrorWhenFileEmpty() throws Exception {
        MockMultipartFile file = new MockMultipartFile(
                "file",
                "empty.txt",
                "text/plain",
                new byte[0]
        );

        mockMvc.perform(multipart("/api/document/parse").file(file))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value(40004));
    }

    /**
     * 测试缺少文件参数
     *
     * @throws Exception 测试异常
     */
    @Test
    void shouldReturnParamErrorWhenFileMissing() throws Exception {
        mockMvc.perform(post("/api/document/parse"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value(40004));
    }
}
```

执行异常测试：

```bash
# 只执行异常测试类
mvn -Dtest=DocumentParseExceptionTest test
```

异常文件测试通过后，应确认日志中能够看到清晰的错误原因。例如文件格式异常使用 `warn` 日志即可，系统未知异常和解析器底层异常应使用 `error` 日志并保留异常堆栈，便于后续排查。

## 项目总结

本章节对 Spring Boot 3 集成 Apache Tika 文档解析模块进行总结，并说明后续可以扩展的方向。当前实现已经覆盖文件上传、类型检测、正文解析、元数据读取、异常处理和基础验证，能够满足常见文档解析场景。

### 功能回顾

本项目围绕“上传文档并提取可用文本”完成了一个基础可落地的文档解析模块。模块使用 Spring Boot 3 提供 Web 接口能力，使用 Apache Tika 负责具体文件解析能力，并通过统一返回结构对外输出解析结果。

已完成的核心能力包括：

| 功能         | 说明                                                         |
| ------------ | ------------------------------------------------------------ |
| 文件上传配置 | 通过 `spring.servlet.multipart` 控制上传大小、请求大小和临时目录 |
| 解析参数配置 | 通过 `document.parse` 管理解析开关、文本长度、扩展名白名单和 MIME 白名单 |
| 文本提取     | 使用 Tika 从常见文档中提取正文文本                           |
| 元数据提取   | 使用 Tika 读取文档标题、作者、创建时间、内容类型等元数据     |
| 文件类型识别 | 使用 Tika 检测真实 MIME 类型，降低后缀伪造风险               |
| 多格式解析   | 支持 PDF、Word、Excel、PPT、TXT、Markdown、HTML、CSV 等常见格式 |
| 接口封装     | 提供 `/parse`、`/detect`、`/metadata` 三类接口               |
| 异常处理     | 统一处理格式异常、大小异常、解析失败和参数异常               |
| 功能验证     | 提供 curl、Shell 脚本和 MockMvc 测试方式                     |

当前实现适合作为文件服务、知识库系统、全文检索系统或 AI 文档问答系统的前置解析模块。对于普通同步解析场景，现有接口已经可以直接使用；对于高并发、大文件或长耗时解析场景，则需要进一步扩展异步能力。

### 后续扩展方向

后续可以从性能、安全、可观测性和业务集成四个方向继续增强文档解析模块。

扩展方向建议如下：

| 方向           | 说明                                                      |
| -------------- | --------------------------------------------------------- |
| 异步解析       | 上传文件后返回任务 ID，由线程池、MQ 或 XXL-JOB 后台解析   |
| 解析任务表     | 将解析状态、文件信息、耗时、错误信息保存到数据库          |
| 对象存储集成   | 文件保存到 MinIO、S3、OSS，再由解析服务读取文件流         |
| 全文检索集成   | 将解析文本写入 Elasticsearch 或 OpenSearch                |
| RAG 知识库集成 | 将解析文本切分后写入向量数据库，用于 AI 问答              |
| OCR 扩展       | 对扫描版 PDF 或图片接入 Tesseract、PaddleOCR 或第三方 OCR |
| 解析超时控制   | 对大文件或异常文件设置超时时间，避免线程长时间阻塞        |
| 文件安全扫描   | 集成杀毒、文件魔数检测、压缩包安全检查等能力              |
| 解析结果缓存   | 对相同文件 MD5/SHA256 命中的内容直接复用解析结果          |
| 监控告警       | 统计解析耗时、失败率、文件大小分布和异常类型              |

如果要支持异步解析，可以增加如下数据表设计：

```sql
CREATE TABLE document_parse_task (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '主键ID',
    task_id VARCHAR(64) NOT NULL COMMENT '解析任务ID',
    file_name VARCHAR(255) NOT NULL COMMENT '原始文件名',
    file_path VARCHAR(500) NOT NULL COMMENT '文件存储路径',
    file_size BIGINT NOT NULL COMMENT '文件大小，单位字节',
    content_type VARCHAR(200) DEFAULT NULL COMMENT 'MIME类型',
    parse_status VARCHAR(32) NOT NULL COMMENT '解析状态：PENDING、RUNNING、SUCCESS、FAILED',
    text_length INT DEFAULT 0 COMMENT '解析文本长度',
    error_message VARCHAR(1000) DEFAULT NULL COMMENT '错误信息',
    parse_cost_millis BIGINT DEFAULT 0 COMMENT '解析耗时，单位毫秒',
    create_time DATETIME NOT NULL COMMENT '创建时间',
    update_time DATETIME NOT NULL COMMENT '更新时间',
    UNIQUE KEY uk_task_id (task_id),
    KEY idx_parse_status (parse_status),
    KEY idx_create_time (create_time)
) COMMENT='文档解析任务表';
```

如果要支持全文检索，可以将解析结果转换为索引文档：

```json
{
  "fileName": "项目说明文档.docx",
  "extension": "docx",
  "contentType": "application/vnd.openxmlformats-officedocument.wordprocessingml.document",
  "text": "文档正文内容",
  "metadata": {
    "Author": "Ateng",
    "dc:title": "项目说明文档"
  },
  "textLength": 12680,
  "parseTime": "2026-05-06T10:30:00"
}
```

如果要支持 AI 文档问答，可以在解析后增加如下处理链路：

```text
文件上传
  -> Tika 文本解析
  -> 文本清洗
  -> 文本分段
  -> 生成向量
  -> 写入向量数据库
  -> 用户提问
  -> 相似度检索
  -> 大模型生成答案
```

对于生产环境，建议优先补充以下能力：异步解析、解析任务状态、文件指纹去重、解析超时控制、解析失败重试和监控告警。这样可以避免大文件或异常文件影响主业务接口稳定性，也便于后续将文档解析模块独立为可复用的基础服务。