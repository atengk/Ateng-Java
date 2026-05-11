# Spring Boot 集成 OnlyOffice 开发

本文档用于说明如何在 Spring Boot 3 项目中集成 OnlyOffice Docs，实现 Office 文档的在线预览、在线编辑、保存回调和文件存储管理。整体架构中，Spring Boot 负责业务权限、文件管理、编辑器配置生成、文件下载和保存回调；OnlyOffice Docs 负责文档渲染、在线编辑和协同编辑。

## 项目概述

本章节主要说明 Spring Boot 集成 OnlyOffice 的功能边界、适用场景和最终集成目标。OnlyOffice Docs 是在线办公套件，支持在浏览器中处理文本文档、电子表格、演示文稿、PDF 和表单等内容；在本项目中，它作为独立的文档编辑服务接入 Spring Boot 业务系统。([ONLYOFFICE - Cloud Office Applications](https://helpcenter.onlyoffice.com/docs/getting-started?utm_source=chatgpt.com))

### 功能定位

本功能定位为业务系统内嵌式 Office 文档在线处理能力。用户不需要下载文件到本地，也不需要安装 Office 客户端，即可在浏览器中完成文档预览、在线编辑和协同处理。

在整体职责划分上，Spring Boot 不负责解析和渲染 Office 文件，而是作为文档管理服务存在。Spring Boot 主要负责文档元数据管理、文件下载接口、编辑器配置生成、权限判断、回调处理和文件保存。OnlyOffice Docs 负责文档打开、渲染、编辑、协同编辑和编辑结果生成。

功能边界可以拆分为以下几类：

| 功能     | 说明                                                         |
| -------- | ------------------------------------------------------------ |
| 文档预览 | 用户在浏览器中查看 Word、Excel、PowerPoint、PDF 等文件。     |
| 文档编辑 | 用户在浏览器中修改文档内容，并由 OnlyOffice Docs 管理编辑过程。 |
| 保存回调 | OnlyOffice Docs 在文档状态变化时回调 Spring Boot 服务。      |
| 文件下载 | OnlyOffice Docs 通过 Spring Boot 暴露的文件地址读取原始文档。 |
| 文件保存 | Spring Boot 根据回调中的文件地址下载编辑后的新文件并保存。   |
| 权限控制 | Spring Boot 根据业务用户、角色、文档状态控制查看和编辑权限。 |
| 存储扩展 | 初期使用本地磁盘，后续可扩展为 MinIO、OSS、COS、S3 等对象存储。 |

从系统集成角度看，本功能不是单独做一个文件服务器，也不是只做一个文档预览页面，而是要完成“业务文档进入编辑器、编辑器读取文档、用户编辑、OnlyOffice 回调、业务系统保存新文档”的完整闭环。

### 使用场景

OnlyOffice 适合接入到已经存在文档管理、流程审批、合同管理、知识库、项目管理或协同办公需求的业务系统中。它主要解决业务系统中的 Office 文件无法在线打开、在线编辑和统一回写的问题。

常见使用场景如下：

| 使用场景       | 业务说明                                                     |
| -------------- | ------------------------------------------------------------ |
| 合同在线编辑   | 合同模板上传后，业务人员在线修改合同内容，保存后回写到系统文件库。 |
| 审批附件预览   | 审批流程中的 Word、Excel、PPT 附件不需要下载，直接在审批页面中预览。 |
| 项目文档协同   | 项目成员在线编辑需求文档、会议纪要、测试报告和交付文档。     |
| 报表文件查看   | 系统生成 Excel 报表后，用户直接在线查看、筛选或编辑。        |
| 模板文件维护   | 管理员维护合同模板、通知模板、报价模板、报告模板等业务模板。 |
| 知识库文档管理 | 企业知识库中的 Office 文件可以直接在线查看和维护。           |

在这些场景中，最关键的是网络可达性。OnlyOffice Docs 必须能够访问 Spring Boot 提供的文件下载地址；同时，Spring Boot 也必须能够接收 OnlyOffice Docs 发起的保存回调请求。OnlyOffice 官方回调机制通过 `callbackUrl` 向文档存储服务发送 POST 请求，并通过 `status` 表示当前文档状态。([ONLYOFFICE API](https://api.onlyoffice.com/docs/docs-api/usage-api/callback-handler/?utm_source=chatgpt.com))

### 集成目标

本次集成的目标是完成 Spring Boot 3 与 OnlyOffice Docs 的基础闭环，使业务系统具备可用的在线文档处理能力。第一阶段不追求复杂的版本管理、协同审计和对象存储，而是优先保证核心链路稳定。

基础集成目标如下：

| 目标             | 说明                                                         |
| ---------------- | ------------------------------------------------------------ |
| 生成编辑器配置   | Spring Boot 根据文档 ID 生成 OnlyOffice 前端初始化配置。     |
| 提供文件下载接口 | OnlyOffice Docs 可以通过 HTTP 地址下载原始文档。             |
| 加载在线编辑器   | 前端页面加载 OnlyOffice Docs 的 `api.js` 并初始化 `DocsAPI.DocEditor`。 |
| 支持预览和编辑   | 根据权限控制文档以预览模式或编辑模式打开。                   |
| 接收保存回调     | Spring Boot 提供 `callbackUrl` 接收 OnlyOffice Docs 回调。   |
| 保存编辑结果     | 当回调状态表示需要保存时，Spring Boot 下载新文件并保存。     |
| 统一权限校验     | 查看、编辑、下载、保存均经过业务权限判断。                   |

基础链路如下：

```text
用户打开文档
  ↓
前端请求 Spring Boot 获取编辑器配置
  ↓
Spring Boot 返回 OnlyOffice DocumentEditor 配置
  ↓
前端加载 OnlyOffice Docs 编辑器
  ↓
OnlyOffice Docs 请求 Spring Boot 文件下载接口
  ↓
用户在线预览或编辑文档
  ↓
OnlyOffice Docs 调用 Spring Boot 回调接口
  ↓
Spring Boot 下载编辑后的文件并保存
```

后续扩展目标如下：

| 扩展方向     | 说明                                                         |
| ------------ | ------------------------------------------------------------ |
| 文档版本管理 | 每次保存生成新版本，支持历史版本查看和恢复。                 |
| 对象存储接入 | 文件从本地磁盘迁移到 MinIO、OSS、COS、S3 等存储。            |
| 操作日志审计 | 记录用户打开、编辑、保存、下载等操作。                       |
| 强制保存     | 支持用户点击保存按钮后触发 Force Save。                      |
| 多租户隔离   | 不同租户的文件目录、访问权限、回调处理相互隔离。             |
| HTTPS 接入   | 生产环境通过 HTTPS 暴露 Spring Boot 和 OnlyOffice Docs 服务。 |

## 环境准备

本章节主要准备 Spring Boot 3 运行环境、OnlyOffice Docs 服务和文档存储目录。OnlyOffice 集成不只是启动两个服务，还需要保证 Spring Boot 与 OnlyOffice Docs 之间可以双向访问。

### Spring Boot 3 基础环境

Spring Boot 3 项目建议使用 JDK 17 或以上版本。以 Spring Boot 3.2.x 为例，官方要求 Java 17，并支持 Maven 3.6.3 或更高版本。([Home](https://docs.spring.io/spring-boot/docs/3.2.2/reference/html/getting-started.html?utm_source=chatgpt.com))

推荐基础环境如下：

| 组件        | 建议版本     | 说明                         |
| ----------- | ------------ | ---------------------------- |
| JDK         | 17 或以上    | Spring Boot 3 基础运行环境。 |
| Spring Boot | 3.x          | 建议使用项目统一版本。       |
| Maven       | 3.6.3 或以上 | 用于依赖管理和项目构建。     |
| Docker      | 20.x 或以上  | 用于部署 OnlyOffice Docs。   |
| 操作系统    | Linux 优先   | 生产环境建议使用 Linux。     |

建议项目目录结构如下：

```text
springboot-onlyoffice
├── deploy
│   └── onlyoffice
│       └── docker-compose.yml
├── storage
│   ├── files
│   └── temp
├── src
│   └── main
│       ├── java
│       │   └── io
│       │       └── github
│       │           └── atengk
│       │               └── onlyoffice
│       │                   ├── config
│       │                   ├── controller
│       │                   ├── service
│       │                   ├── storage
│       │                   └── vo
│       └── resources
│           └── application.yml
└── pom.xml
```

目录说明如下：

| 目录                | 说明                                         |
| ------------------- | -------------------------------------------- |
| `deploy/onlyoffice` | 保存 OnlyOffice Docs 部署文件。              |
| `storage/files`     | 保存业务文档原文件或最终文件。               |
| `storage/temp`      | 保存回调下载文件、转换过程文件或临时文件。   |
| `config`            | 保存 OnlyOffice、JWT、文件访问地址等配置类。 |
| `controller`        | 提供编辑器配置接口、文件下载接口和回调接口。 |
| `service`           | 处理文档业务逻辑。                           |
| `storage`           | 封装本地文件存储或对象存储操作。             |
| `vo`                | 定义返回给前端的编辑器配置对象。             |

基础环境验证命令如下：

```bash
# 查看 Java 版本，确认使用 JDK 17 或以上
java -version

# 查看 Maven 版本，确认 Maven 可正常构建 Spring Boot 3 项目
mvn -version

# 编译项目，跳过测试用于快速验证环境
mvn clean package -DskipTests

# 启动 Spring Boot 服务
java -jar target/springboot-onlyoffice.jar
```

这些命令用于确认本地 Java、Maven 和 Spring Boot 启动链路正常。OnlyOffice 集成还需要额外验证网络访问关系：OnlyOffice Docs 容器是否能访问 Spring Boot 的文件下载地址，Spring Boot 是否能接收 OnlyOffice Docs 的保存回调。

### OnlyOffice Docs 服务部署

OnlyOffice Docs 建议使用 Docker 独立部署，避免和 Spring Boot 服务耦合。OnlyOffice Docs 支持通过自托管方式部署，并在前端页面中通过 `https://documentserver/web-apps/apps/api/documents/api.js` 加载编辑器 API。([ONLYOFFICE API](https://api.onlyoffice.com/docs/docs-api/get-started/installation/self-hosted/?utm_source=chatgpt.com))

从 OnlyOffice Docs 7.2 开始，JWT 默认启用，并会自动生成密钥。为了避免服务重启、升级或多环境配置不一致导致 JWT 校验失败，建议显式配置固定的 `JWT_SECRET`，并保证它与 Spring Boot 中的配置完全一致。([ONLYOFFICE - Cloud Office Applications](https://helpcenter.onlyoffice.com/docs/installation/docs-configure-jwt.aspx?utm_source=chatgpt.com))

文件位置：`deploy/onlyoffice/docker-compose.yml`

```yaml
services:
  onlyoffice-documentserver:
    image: onlyoffice/documentserver:latest
    container_name: onlyoffice-documentserver
    restart: always
    ports:
      # 宿主机 8088 端口映射到容器 80 端口
      - "8088:80"
    environment:
      # 开启 JWT 校验，生产环境必须开启
      JWT_ENABLED: "true"
      # JWT 密钥需要与 Spring Boot 配置保持一致
      JWT_SECRET: "onlyoffice_jwt_secret_please_change"
    volumes:
      # 日志目录，便于排查文档加载和回调问题
      - ./data/logs:/var/log/onlyoffice
      # 数据目录，保存 OnlyOffice 运行数据
      - ./data/data:/var/www/onlyoffice/Data
      # 运行库目录，保存服务运行过程中的数据
      - ./data/lib:/var/lib/onlyoffice
      # 内置数据库目录
      - ./data/db:/var/lib/postgresql
```

在 `deploy/onlyoffice` 目录执行以下命令启动服务：

```bash
# 进入 OnlyOffice 部署目录
cd deploy/onlyoffice

# 创建挂载目录，避免 Docker 自动创建目录后权限不可控
mkdir -p data/logs data/data data/lib data/db

# 启动 OnlyOffice Docs
docker compose up -d

# 查看容器状态
docker compose ps

# 查看启动日志
docker logs -f onlyoffice-documentserver
```

命令说明如下：

| 命令                                       | 说明                                         |
| ------------------------------------------ | -------------------------------------------- |
| `docker compose up -d`                     | 后台启动 OnlyOffice Docs。                   |
| `docker compose ps`                        | 查看容器是否正常运行。                       |
| `docker logs -f onlyoffice-documentserver` | 查看启动日志和运行异常。                     |
| `mkdir -p data/...`                        | 创建宿主机挂载目录，便于日志排查和数据保留。 |

服务启动后，开发环境可访问：

```text
http://localhost:8088
```

如果部署在服务器上，需要将 `localhost` 替换为服务器 IP 或域名，例如：

```text
http://192.168.1.100:8088
https://office.example.com
```

生产环境建议通过 Nginx 或网关提供 HTTPS 访问。前端页面后续会通过以下地址加载 OnlyOffice Docs 的 JavaScript API：

```text
http://localhost:8088/web-apps/apps/api/documents/api.js
```

如果使用域名，则地址示例如下：

```text
https://office.example.com/web-apps/apps/api/documents/api.js
```

### 文档存储服务准备

文档存储服务是 Spring Boot 集成 OnlyOffice 的核心前置条件。OnlyOffice Docs 不直接读取业务数据库中的文件内容，而是通过 Spring Boot 提供的 HTTP 文件地址下载文档；编辑完成后，OnlyOffice Docs 再通过回调接口通知 Spring Boot 下载编辑后的新文件。官方回调文档中，`status=2` 表示文档已准备好保存，回调响应必须返回 `{"error":0}`，否则编辑器会显示保存错误。([ONLYOFFICE API](https://api.onlyoffice.com/docs/docs-api/usage-api/callback-handler/?utm_source=chatgpt.com))

开发阶段可以先使用本地磁盘作为文件存储，目录结构如下：

```text
storage
├── files
│   ├── 2026
│   │   └── 05
│   │       └── demo.docx
│   └── template
│       └── contract-template.docx
└── temp
    ├── callback
    └── download
```

目录说明如下：

| 目录                     | 用途                                           |
| ------------------------ | ---------------------------------------------- |
| `storage/files`          | 保存业务文档原文件或最终版本文件。             |
| `storage/files/template` | 保存合同模板、报告模板等模板文件。             |
| `storage/temp/callback`  | 保存回调处理过程中的临时文件。                 |
| `storage/temp/download`  | 保存 OnlyOffice 返回文件下载过程中的临时文件。 |

Spring Boot 配置文件中建议先预留文档存储路径、OnlyOffice 服务地址、Spring Boot 外部访问地址和 JWT 密钥。

文件位置：`src/main/resources/application.yml`

```yaml
server:
  port: 8080

spring:
  application:
    name: springboot-onlyoffice

onlyoffice:
  # OnlyOffice Docs 服务地址，前端基于该地址加载 api.js
  document-server-url: http://localhost:8088

  # Spring Boot 对外访问地址，OnlyOffice Docs 必须能够访问该地址
  app-base-url: http://host.docker.internal:8080

  # JWT 密钥，必须与 OnlyOffice Docs 的 JWT_SECRET 保持一致
  jwt-secret: onlyoffice_jwt_secret_please_change

document:
  storage:
    # 本地文件存储根目录
    root-path: ./storage/files

    # 临时文件目录，用于保存回调下载文件
    temp-path: ./storage/temp
```

这里需要重点确认 `onlyoffice.app-base-url` 的可达性。OnlyOffice Docs 运行在 Docker 容器中时，`localhost:8080` 通常表示容器自身，而不是宿主机上的 Spring Boot 服务。因此开发环境需要根据操作系统选择合适的访问地址。

| 环境             | Spring Boot 地址写法               | 说明                                   |
| ---------------- | ---------------------------------- | -------------------------------------- |
| Docker Desktop   | `http://host.docker.internal:8080` | 容器访问宿主机 Spring Boot 服务。      |
| Linux Docker     | `http://宿主机IP:8080`             | 使用宿主机真实内网 IP。                |
| 同一 Docker 网络 | `http://springboot服务名:8080`     | Spring Boot 也容器化时使用服务名访问。 |
| 生产环境         | `https://api.example.com`          | 使用网关、Nginx 或负载均衡地址。       |

文档存储服务至少需要提供以下能力：

| 能力             | 说明                                                         |
| ---------------- | ------------------------------------------------------------ |
| 文档元信息查询   | 根据文档 ID 查询文件名、扩展名、存储路径、大小、版本号等信息。 |
| 文件下载地址生成 | 生成 OnlyOffice Docs 可以访问的文档下载 URL。                |
| 文件下载接口     | 返回原始文档二进制内容。                                     |
| 回调接口         | 接收 OnlyOffice Docs 的文档状态通知。                        |
| 编辑结果下载     | 根据回调中的 `url` 下载编辑后的文件。                        |
| 文件保存         | 将新文件覆盖原文件或保存为新版本。                           |
| 操作日志记录     | 记录用户、时间、文档 ID、保存结果和异常信息。                |

开发阶段可以准备一个测试文件：

```bash
# 创建本地存储目录
mkdir -p storage/files storage/temp/callback storage/temp/download

# 将测试文档复制到本地存储目录
cp ~/Documents/demo.docx storage/files/demo.docx

# 查看文件是否存在
ls -lh storage/files
```

命令说明如下：

| 命令                                                         | 说明                               |
| ------------------------------------------------------------ | ---------------------------------- |
| `mkdir -p storage/files storage/temp/callback storage/temp/download` | 创建本地文件目录和临时文件目录。   |
| `cp ~/Documents/demo.docx storage/files/demo.docx`           | 放入一个测试 Word 文档。           |
| `ls -lh storage/files`                                       | 确认文件大小、权限和路径是否正确。 |

完成环境准备后，需要确认以下检查项：

| 检查项                 | 通过标准                                                     |
| ---------------------- | ------------------------------------------------------------ |
| Spring Boot 可启动     | `http://localhost:8080` 可正常访问。                         |
| OnlyOffice Docs 可访问 | `http://localhost:8088` 可正常打开。                         |
| 编辑器 API 可访问      | `/web-apps/apps/api/documents/api.js` 可正常加载。           |
| 文件下载地址可访问     | OnlyOffice Docs 容器内可以访问 Spring Boot 文件下载接口。    |
| 回调地址可访问         | OnlyOffice Docs 可以 POST 请求到 Spring Boot 回调接口。      |
| JWT 密钥一致           | `application.yml` 中的 `onlyoffice.jwt-secret` 与 Docker 环境变量 `JWT_SECRET` 一致。 |
| 测试文件存在           | `storage/files/demo.docx` 存在，并且 Spring Boot 进程有读取权限。 |

只有这些检查项通过后，后续“编辑器配置生成”“文件下载接口”“回调处理接口”和“文档保存逻辑”等章节才能正常联调。



## 核心配置

本章节主要说明 Spring Boot 集成 OnlyOffice Docs 时需要准备的核心配置，包括 Maven 依赖、OnlyOffice 服务地址、JWT 安全配置和文件访问地址配置。OnlyOffice 编辑器初始化配置通常由 `config`、`document`、`editorConfig`、`permissions`、`token` 等部分组成，其中 `document.url`、`document.key`、`document.title`、`editorConfig.callbackUrl` 是后续预览、编辑和保存链路中的关键参数。OnlyOffice 官方文档说明，`document` 用于描述文件本身，`editorConfig` 用于描述编辑器界面、打开模式、回调地址和用户信息，`permissions` 用于控制编辑、下载、打印、评论等权限。([ONLYOFFICE API](https://api.onlyoffice.com/docs/docs-api/usage-api/config/document/?utm_source=chatgpt.com))

### Maven 依赖配置

Maven 依赖主要包含 Spring Web、参数校验、配置提示、Lombok、Hutool 和 JWT 工具库。Spring Web 用于提供编辑器配置接口、文件下载接口和回调接口；Hutool 用于字符串、文件、集合等常用处理；JWT 工具库用于生成 OnlyOffice 编辑器初始化配置中的 `token`。

文件位置：`pom.xml`

```xml
<dependencies>
    <!-- Spring Web：提供 REST 接口、文件下载接口、OnlyOffice 回调接口 -->
    <dependency>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-starter-web</artifactId>
    </dependency>

    <!-- 参数校验：用于校验配置项、接口参数和回调参数 -->
    <dependency>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-starter-validation</artifactId>
    </dependency>

    <!-- 配置元数据提示：让 application.yml 中的自定义配置具备 IDE 提示 -->
    <dependency>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-configuration-processor</artifactId>
        <optional>true</optional>
    </dependency>

    <!-- Lombok：简化 Getter、Setter、构造器、日志对象等代码 -->
    <dependency>
        <groupId>org.projectlombok</groupId>
        <artifactId>lombok</artifactId>
        <optional>true</optional>
    </dependency>

    <!-- Hutool：常用工具类，处理字符串、文件、URL、集合、JSON 等操作 -->
    <dependency>
        <groupId>cn.hutool</groupId>
        <artifactId>hutool-all</artifactId>
        <version>5.8.35</version>
    </dependency>

    <!-- JJWT API：生成和解析 JWT Token -->
    <dependency>
        <groupId>io.jsonwebtoken</groupId>
        <artifactId>jjwt-api</artifactId>
        <version>0.12.6</version>
    </dependency>

    <!-- JJWT 实现：运行时签名和验签实现 -->
    <dependency>
        <groupId>io.jsonwebtoken</groupId>
        <artifactId>jjwt-impl</artifactId>
        <version>0.12.6</version>
        <scope>runtime</scope>
    </dependency>

    <!-- JJWT Jackson：JWT Payload JSON 序列化和反序列化 -->
    <dependency>
        <groupId>io.jsonwebtoken</groupId>
        <artifactId>jjwt-jackson</artifactId>
        <version>0.12.6</version>
        <scope>runtime</scope>
    </dependency>
</dependencies>
```

如果项目已经统一管理了 Hutool、JJWT 或 Lombok 版本，可以将版本号放到父工程的 `dependencyManagement` 中。OnlyOffice 集成不强制要求使用某一个 JWT Java 库，只要最终生成的 `token` 能够使用与 OnlyOffice Docs 相同的密钥完成签名即可。

### OnlyOffice 服务地址配置

OnlyOffice 服务地址配置用于告诉前端从哪里加载编辑器脚本，以及告诉 Spring Boot 当前文档服务部署在哪里。前端页面需要加载 `Document Server` 提供的 `api.js`，并通过 `DocsAPI.DocEditor` 创建编辑器实例；官方文档说明，`DocsAPI.DocEditor` 是嵌入式文档编辑器的入口类，需要传入页面容器 ID 和配置对象。([ONLYOFFICE API](https://api.onlyoffice.com/docs/docs-api/usage-api/doceditor/?utm_source=chatgpt.com))

文件位置：`src/main/resources/application.yml`

```yaml
server:
  port: 8080

spring:
  application:
    name: springboot-onlyoffice

onlyoffice:
  # OnlyOffice Docs 服务地址，用于加载 /web-apps/apps/api/documents/api.js
  document-server-url: http://localhost:8088

  # Spring Boot 对外访问地址，必须能被 OnlyOffice Docs 容器访问
  app-base-url: http://host.docker.internal:8080

  # 编辑器默认语言，zh 表示简体中文界面
  lang: zh

  # 编辑器显示类型：desktop、mobile、embedded
  editor-type: desktop

  jwt:
    # 是否启用 JWT。生产环境建议开启
    enabled: true

    # JWT 密钥，必须与 OnlyOffice Docs 的 JWT_SECRET 一致
    secret: onlyoffice_jwt_secret_please_change_32

  storage:
    # 文档下载接口前缀
    download-path: /api/onlyoffice/file/download

    # OnlyOffice 回调接口前缀
    callback-path: /api/onlyoffice/callback

    # 本地文件存储根目录
    root-path: ./storage/files

    # 临时文件目录
    temp-path: ./storage/temp
```

配置项说明如下：

| 配置项                             | 说明                                                     |
| ---------------------------------- | -------------------------------------------------------- |
| `onlyoffice.document-server-url`   | OnlyOffice Docs 服务地址，前端通过该地址加载编辑器脚本。 |
| `onlyoffice.app-base-url`          | Spring Boot 对外地址，OnlyOffice Docs 必须能访问该地址。 |
| `onlyoffice.lang`                  | 编辑器界面语言。                                         |
| `onlyoffice.editor-type`           | 编辑器类型，常用值为 `desktop`。                         |
| `onlyoffice.jwt.enabled`           | 是否生成 OnlyOffice 初始化配置的 JWT Token。             |
| `onlyoffice.jwt.secret`            | JWT 签名密钥，需要与 OnlyOffice Docs 部署配置一致。      |
| `onlyoffice.storage.download-path` | 文件下载接口路径。                                       |
| `onlyoffice.storage.callback-path` | 回调接口路径。                                           |
| `onlyoffice.storage.root-path`     | 本地文件存储根路径。                                     |
| `onlyoffice.storage.temp-path`     | 临时文件根路径。                                         |

建议为 OnlyOffice 配置创建独立属性类，避免在业务代码中散落读取 `@Value`。

文件位置：`src/main/java/io/github/atengk/onlyoffice/config/OnlyOfficeProperties.java`

下面的配置类用于承接 `application.yml` 中的 `onlyoffice` 配置，后续生成编辑器配置、文件地址和 JWT Token 时统一从该类读取。

```java
package io.github.atengk.onlyoffice.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * OnlyOffice 配置属性
 *
 * @author Ateng
 * @since 2026-05-09
 */
@Data
@Component
@ConfigurationProperties(prefix = "onlyoffice")
public class OnlyOfficeProperties {

    /**
     * OnlyOffice Docs 服务地址
     */
    private String documentServerUrl;

    /**
     * Spring Boot 对外访问地址
     */
    private String appBaseUrl;

    /**
     * 编辑器语言
     */
    private String lang = "zh";

    /**
     * 编辑器类型
     */
    private String editorType = "desktop";

    /**
     * JWT 配置
     */
    private Jwt jwt = new Jwt();

    /**
     * 存储配置
     */
    private Storage storage = new Storage();

    /**
     * JWT 配置属性
     *
     * @author Ateng
     * @since 2026-05-09
     */
    @Data
    public static class Jwt {

        /**
         * 是否启用 JWT
         */
        private Boolean enabled = true;

        /**
         * JWT 签名密钥
         */
        private String secret;
    }

    /**
     * 文档存储配置属性
     *
     * @author Ateng
     * @since 2026-05-09
     */
    @Data
    public static class Storage {

        /**
         * 文件下载接口路径
         */
        private String downloadPath = "/api/onlyoffice/file/download";

        /**
         * 回调接口路径
         */
        private String callbackPath = "/api/onlyoffice/callback";

        /**
         * 本地文件存储根路径
         */
        private String rootPath = "./storage/files";

        /**
         * 临时文件根路径
         */
        private String tempPath = "./storage/temp";
    }
}
```

### JWT 安全配置

JWT 安全配置用于保护 OnlyOffice 编辑器初始化配置和服务间通信。OnlyOffice 官方文档说明，JWT 会被添加到 Document Editor 初始化配置中，OnlyOffice Docs 使用相同的密钥验证 Token；从 OnlyOffice Docs 7.2 开始，JWT 默认启用，并且如果未显式指定 `JWT_SECRET`，可能因自动生成密钥导致集成端校验不一致，因此 Docker 部署建议通过环境变量固定 `JWT_SECRET`。([ONLYOFFICE - Cloud Office Applications](https://helpcenter.onlyoffice.com/docs/installation/docs-configure-jwt.aspx?utm_source=chatgpt.com))

OnlyOffice Docker 配置中的密钥示例如下：

文件位置：`deploy/onlyoffice/docker-compose.yml`

```yaml
services:
  onlyoffice-documentserver:
    image: onlyoffice/documentserver:latest
    container_name: onlyoffice-documentserver
    restart: always
    ports:
      - "8088:80"
    environment:
      # 启用 JWT 校验
      JWT_ENABLED: "true"
      # 必须与 Spring Boot onlyoffice.jwt.secret 保持一致
      JWT_SECRET: "onlyoffice_jwt_secret_please_change_32"
```

Spring Boot 侧需要用同一个密钥对编辑器配置进行签名。OnlyOffice 的基础配置中包含 `token` 字段，用于保存加密签名；`documentType`、`type`、`height`、`width`、`token` 都属于初始化配置的基础字段。([ONLYOFFICE API](https://api.onlyoffice.com/docs/docs-api/usage-api/config/?utm_source=chatgpt.com))

文件位置：`src/main/java/io/github/atengk/onlyoffice/config/OnlyOfficeJwtService.java`

下面的工具类用于生成和校验 OnlyOffice JWT Token。生成编辑器配置时，将完整配置对象作为 Payload 签名后放入顶层 `token` 字段。

```java
package io.github.atengk.onlyoffice.config;

import cn.hutool.core.util.CharsetUtil;
import cn.hutool.core.util.StrUtil;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.util.Map;

/**
 * OnlyOffice JWT 服务
 *
 * @author Ateng
 * @since 2026-05-09
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class OnlyOfficeJwtService {

    private final OnlyOfficeProperties onlyOfficeProperties;

    /**
     * 创建 OnlyOffice JWT Token
     *
     * @param payload Token 载荷
     * @return JWT Token
     */
    public String createToken(Map<String, Object> payload) {
        if (!Boolean.TRUE.equals(onlyOfficeProperties.getJwt().getEnabled())) {
            return null;
        }
        SecretKey secretKey = buildSecretKey();
        return Jwts.builder()
                .claims(payload)
                .signWith(secretKey, Jwts.SIG.HS256)
                .compact();
    }

    /**
     * 校验 OnlyOffice JWT Token
     *
     * @param token JWT Token
     * @return 是否校验通过
     */
    public boolean verifyToken(String token) {
        if (!Boolean.TRUE.equals(onlyOfficeProperties.getJwt().getEnabled())) {
            return true;
        }
        if (StrUtil.isBlank(token)) {
            log.warn("OnlyOffice JWT校验失败，Token为空");
            return false;
        }
        try {
            Jwts.parser()
                    .verifyWith(buildSecretKey())
                    .build()
                    .parseSignedClaims(token);
            return true;
        } catch (Exception ex) {
            log.warn("OnlyOffice JWT校验失败，原因：{}", ex.getMessage());
            return false;
        }
    }

    /**
     * 构建签名密钥
     *
     * @return 签名密钥
     */
    private SecretKey buildSecretKey() {
        String secret = onlyOfficeProperties.getJwt().getSecret();
        if (StrUtil.isBlank(secret)) {
            throw new IllegalStateException("OnlyOffice JWT密钥不能为空");
        }
        return Keys.hmacShaKeyFor(secret.getBytes(CharsetUtil.CHARSET_UTF_8));
    }
}
```

JWT 配置注意事项如下：

| 注意事项               | 说明                                                         |
| ---------------------- | ------------------------------------------------------------ |
| 密钥必须一致           | Docker 中的 `JWT_SECRET` 必须与 Spring Boot 中的 `onlyoffice.jwt.secret` 完全一致。 |
| 密钥长度要足够         | 使用 HS256 时建议密钥不少于 32 个字节，避免签名库认为密钥强度不足。 |
| 生产环境必须替换默认值 | 不要使用示例密钥上线。                                       |
| 多环境独立配置         | 开发、测试、生产环境应使用不同密钥。                         |
| 不要在前端硬编码密钥   | 前端只接收后端生成后的 `token`，不能保存原始密钥。           |

### 文件访问地址配置

文件访问地址配置用于生成 `document.url` 和 `editorConfig.callbackUrl`。OnlyOffice Docs 打开文档时，会根据 `document.url` 下载源文件；文档状态变化时，会根据 `editorConfig.callbackUrl` 回调 Spring Boot。官方文档中 `document.url` 是必填项，表示源文档的绝对 URL；`editorConfig.callbackUrl` 也是必填项，表示文档存储服务的绝对回调地址。([ONLYOFFICE API](https://api.onlyoffice.com/docs/docs-api/usage-api/config/document/?utm_source=chatgpt.com))

建议统一生成文件下载地址和回调地址，避免在多个业务类中手动拼接。

地址规则如下：

| 地址类型       | 示例                                                         | 用途                                     |
| -------------- | ------------------------------------------------------------ | ---------------------------------------- |
| 文件下载地址   | `http://host.docker.internal:8080/api/onlyoffice/file/download/1001` | OnlyOffice Docs 下载原始文档。           |
| 回调地址       | `http://host.docker.internal:8080/api/onlyoffice/callback/1001` | OnlyOffice Docs 通知文档状态和保存结果。 |
| 编辑器脚本地址 | `http://localhost:8088/web-apps/apps/api/documents/api.js`   | 前端加载 OnlyOffice 编辑器 API。         |

文件位置：`src/main/java/io/github/atengk/onlyoffice/config/OnlyOfficeUrlBuilder.java`

下面的地址构建类用于统一生成 OnlyOffice 所需的脚本地址、文件下载地址和保存回调地址。

```java
package io.github.atengk.onlyoffice.config;

import cn.hutool.core.util.StrUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

/**
 * OnlyOffice 地址构建器
 *
 * @author Ateng
 * @since 2026-05-09
 */
@Component
@RequiredArgsConstructor
public class OnlyOfficeUrlBuilder {

    private final OnlyOfficeProperties onlyOfficeProperties;

    /**
     * 构建编辑器脚本地址
     *
     * @return api.js 地址
     */
    public String buildApiJsUrl() {
        return normalizeUrl(onlyOfficeProperties.getDocumentServerUrl())
                + "/web-apps/apps/api/documents/api.js";
    }

    /**
     * 构建文档下载地址
     *
     * @param documentId 文档ID
     * @return 文档下载地址
     */
    public String buildDownloadUrl(Long documentId) {
        return normalizeUrl(onlyOfficeProperties.getAppBaseUrl())
                + onlyOfficeProperties.getStorage().getDownloadPath()
                + "/"
                + documentId;
    }

    /**
     * 构建保存回调地址
     *
     * @param documentId 文档ID
     * @return 保存回调地址
     */
    public String buildCallbackUrl(Long documentId) {
        return normalizeUrl(onlyOfficeProperties.getAppBaseUrl())
                + onlyOfficeProperties.getStorage().getCallbackPath()
                + "/"
                + documentId;
    }

    /**
     * 规范化地址，移除结尾斜杠
     *
     * @param url 原始地址
     * @return 规范化后的地址
     */
    private String normalizeUrl(String url) {
        if (StrUtil.isBlank(url)) {
            throw new IllegalArgumentException("OnlyOffice地址不能为空");
        }
        return StrUtil.removeSuffix(url, "/");
    }
}
```

文件访问地址需要重点关注 Docker 网络。OnlyOffice Docs 如果运行在容器中，`document.url` 和 `callbackUrl` 必须从容器内部可访问。开发环境中，Spring Boot 在宿主机运行时，Docker Desktop 通常可以使用 `host.docker.internal`；Linux 环境则建议使用宿主机内网 IP 或将 Spring Boot 也部署到同一个 Docker 网络中。

配置检查示例如下：

```bash
# 进入 OnlyOffice Docs 容器
docker exec -it onlyoffice-documentserver bash

# 在容器内部测试 Spring Boot 文件下载接口是否可访问
curl -I http://host.docker.internal:8080/api/onlyoffice/file/download/1001

# 在容器内部测试 Spring Boot 回调接口是否可访问
curl -I http://host.docker.internal:8080/api/onlyoffice/callback/1001
```

如果容器内部无法访问 Spring Boot 的接口，前端页面即使能打开编辑器，也可能出现文档加载失败、下载失败、回调失败或保存失败。

## 整体流程设计

本章节说明文档预览、文档编辑和文档保存回调的完整链路。OnlyOffice 集成的核心不是单个接口，而是前端、Spring Boot、OnlyOffice Docs、文档存储之间的多次交互。

### 文档预览流程

文档预览流程用于只读打开文档。此时用户可以查看文档内容，但不能修改并保存文档。OnlyOffice 官方说明，打开模式由 `editorConfig.mode` 控制，可使用 `view` 表示查看模式；如果 `document.permissions.edit` 为 `false`，文档会以查看器方式打开，即使 `mode` 设置为 `edit` 也不能切换到编辑。([ONLYOFFICE API](https://api.onlyoffice.com/docs/docs-api/more-information/faq/editing/?utm_source=chatgpt.com))

预览流程如下：

```text
用户点击预览文档
  ↓
前端请求 Spring Boot 获取编辑器配置
  ↓
Spring Boot 校验用户是否具备查看权限
  ↓
Spring Boot 查询文档元信息
  ↓
Spring Boot 生成 document.url 文件下载地址
  ↓
Spring Boot 生成 editorConfig.callbackUrl 回调地址
  ↓
Spring Boot 设置 editorConfig.mode = view
  ↓
Spring Boot 设置 document.permissions.edit = false
  ↓
Spring Boot 返回编辑器配置
  ↓
前端加载 OnlyOffice api.js
  ↓
前端创建 DocsAPI.DocEditor
  ↓
OnlyOffice Docs 请求 document.url 下载文档
  ↓
浏览器展示文档预览页面
```

预览模式建议配置如下：

```json
{
  "documentType": "word",
  "type": "desktop",
  "height": "100%",
  "width": "100%",
  "document": {
    "title": "demo.docx",
    "url": "http://host.docker.internal:8080/api/onlyoffice/file/download/1001",
    "fileType": "docx",
    "key": "doc-1001-v1",
    "permissions": {
      "edit": false,
      "download": true,
      "print": true,
      "comment": false,
      "review": false
    }
  },
  "editorConfig": {
    "mode": "view",
    "lang": "zh",
    "callbackUrl": "http://host.docker.internal:8080/api/onlyoffice/callback/1001",
    "user": {
      "id": "10001",
      "name": "张三"
    }
  }
}
```

预览流程中的关键点如下：

| 关键点                      | 说明                                                         |
| --------------------------- | ------------------------------------------------------------ |
| `editorConfig.mode`         | 设置为 `view`，表示查看模式。                                |
| `document.permissions.edit` | 设置为 `false`，禁止编辑。                                   |
| `document.url`              | 必须是 OnlyOffice Docs 可访问的绝对地址。                    |
| `document.key`              | 同一版本文档应保持稳定；文档内容保存为新版本后应生成新 key。 |
| `callbackUrl`               | 预览模式也建议提供，便于接收文档连接、关闭等状态。           |

预览流程主要验证文件下载能力。如果文档无法打开，优先检查 `document.url` 是否可以从 OnlyOffice Docs 容器内部访问。

### 文档编辑流程

文档编辑流程用于允许用户在线修改文档内容。编辑模式下，Spring Boot 需要先完成业务权限校验，再返回可编辑的 OnlyOffice 配置。OnlyOffice 配置中，`editorConfig.mode` 可以设置为 `edit`，同时 `document.permissions.edit` 也需要设置为 `true`，否则用户无法真正编辑文档。([ONLYOFFICE API](https://api.onlyoffice.com/docs/docs-api/usage-api/config/editor/?utm_source=chatgpt.com))

编辑流程如下：

```text
用户点击编辑文档
  ↓
前端请求 Spring Boot 获取编辑器配置
  ↓
Spring Boot 校验用户是否具备编辑权限
  ↓
Spring Boot 判断文档状态是否允许编辑
  ↓
Spring Boot 查询文档元信息和当前版本号
  ↓
Spring Boot 生成 document.url 文件下载地址
  ↓
Spring Boot 生成 document.key 文档唯一标识
  ↓
Spring Boot 生成 callbackUrl 保存回调地址
  ↓
Spring Boot 设置 editorConfig.mode = edit
  ↓
Spring Boot 设置 document.permissions.edit = true
  ↓
Spring Boot 根据完整配置生成 JWT token
  ↓
Spring Boot 返回编辑器配置
  ↓
前端创建 DocsAPI.DocEditor
  ↓
OnlyOffice Docs 下载源文件并进入编辑器
  ↓
用户在线编辑文档
```

编辑模式建议配置如下：

```json
{
  "documentType": "word",
  "type": "desktop",
  "height": "100%",
  "width": "100%",
  "document": {
    "title": "demo.docx",
    "url": "http://host.docker.internal:8080/api/onlyoffice/file/download/1001",
    "fileType": "docx",
    "key": "doc-1001-v1",
    "permissions": {
      "edit": true,
      "download": true,
      "print": true,
      "comment": true,
      "review": false
    }
  },
  "editorConfig": {
    "mode": "edit",
    "lang": "zh",
    "callbackUrl": "http://host.docker.internal:8080/api/onlyoffice/callback/1001",
    "user": {
      "id": "10001",
      "name": "张三"
    },
    "coEditing": {
      "mode": "fast",
      "change": true
    }
  },
  "token": "后端生成的JWT_TOKEN"
}
```

编辑流程中的关键点如下：

| 关键点         | 说明                                                         |
| -------------- | ------------------------------------------------------------ |
| 编辑权限       | 不能只依赖前端按钮控制，后端生成配置前必须校验。             |
| 文档状态       | 已归档、已锁定、审批完成等状态可禁止编辑。                   |
| `document.key` | 用于 OnlyOffice 识别文档，同一内容版本保持稳定，内容变化后重新生成。 |
| `callbackUrl`  | 保存结果依赖该地址，必须可被 OnlyOffice Docs 访问。          |
| `token`        | JWT 开启后，初始化配置需要签名。                             |
| 用户 ID        | `editorConfig.user.id` 建议使用稳定且不敏感的用户标识。      |

编辑模式下需要注意缓存问题。OnlyOffice 官方说明，`document.key` 是服务识别文档的唯一标识；如果发送已知 key，文档可能从缓存中获取，因此每次文档编辑并保存后，新的文档版本应生成新的 `document.key`。([ONLYOFFICE API](https://api.onlyoffice.com/docs/docs-api/usage-api/config/document/?utm_source=chatgpt.com))

### 文档保存回调流程

文档保存回调流程是 OnlyOffice 集成中最容易出问题的部分。用户编辑文档后，并不是前端直接把文件上传给 Spring Boot，而是 OnlyOffice Docs 在合适的时机通过 `callbackUrl` 通知 Spring Boot。官方回调文档说明，文档编辑服务会使用 POST 请求把文档状态发送到 `callbackUrl`，其中 `status=2` 表示文档已准备好保存，`url` 表示编辑后文件的下载地址；存储服务必须返回 `{"error":0}`，否则编辑器会显示错误。([ONLYOFFICE API](https://api.onlyoffice.com/docs/docs-api/usage-api/callback-handler/?utm_source=chatgpt.com))

保存回调流程如下：

```text
用户编辑文档
  ↓
用户关闭编辑器或触发保存
  ↓
OnlyOffice Docs 判断文档状态
  ↓
OnlyOffice Docs POST 请求 callbackUrl
  ↓
Spring Boot 接收回调请求
  ↓
Spring Boot 校验 JWT 或回调来源
  ↓
Spring Boot 读取 status、key、url、users 等参数
  ↓
如果 status = 2，表示文档准备好保存
  ↓
Spring Boot 根据回调 url 下载编辑后的文件
  ↓
Spring Boot 保存文件到本地磁盘或对象存储
  ↓
Spring Boot 更新文档版本、大小、更新时间、documentKey
  ↓
Spring Boot 返回 {"error":0}
```

常见回调状态如下：

| status | 含义             | 处理方式                                                  |
| ------ | ---------------- | --------------------------------------------------------- |
| `1`    | 文档正在编辑     | 通常记录日志即可，不保存文件。                            |
| `2`    | 文档准备好保存   | 下载 `url` 对应的新文件并保存。                           |
| `3`    | 文档保存发生错误 | 记录错误日志，可通知管理员排查。                          |
| `4`    | 文档关闭且无修改 | 不需要保存文件。                                          |
| `6`    | 强制保存成功     | 如果启用强制保存，可下载 `url` 并保存临时版本或当前版本。 |
| `7`    | 强制保存失败     | 记录错误日志。                                            |

回调请求示例：

```json
{
  "key": "doc-1001-v1",
  "status": 2,
  "url": "http://onlyoffice-documentserver/cache/files/edited-demo.docx",
  "filetype": "docx",
  "users": [
    "10001"
  ]
}
```

回调响应必须返回：

```json
{
  "error": 0
}
```

保存回调的处理原则如下：

| 原则               | 说明                                                         |
| ------------------ | ------------------------------------------------------------ |
| 快速响应           | 回调接口不要做过多阻塞操作，大文件保存可结合异步处理，但必须保证 OnlyOffice 得到正确响应。 |
| 状态分支清晰       | 只有 `status=2` 或需要支持强制保存时的 `status=6` 才下载文件。 |
| 文件下载要校验     | 下载前校验 `url` 是否为空，下载后校验文件大小是否正常。      |
| 保存成功再更新版本 | 文件落盘或上传对象存储成功后，再更新数据库中的版本信息。     |
| 回调必须返回 JSON  | 成功返回 `{"error":0}`，失败可返回非 0 错误码。              |
| 日志必须完整       | 记录 documentId、key、status、users、保存路径和异常原因。    |

保存回调与 `document.key` 的关系需要特别注意。保存成功后，如果系统将文档作为新版本管理，需要生成新的文档版本号和新的 `document.key`。否则下一次打开文档时，OnlyOffice Docs 可能继续使用旧缓存，导致用户看到的不是最新文件。



## 后端核心实现

本章节给出 Spring Boot 3 集成 OnlyOffice 的核心后端代码。示例以本地磁盘作为文档存储，目标是先跑通“获取编辑器配置、下载文档、接收回调、保存编辑结果”的基础闭环。后续如果接入数据库、MinIO、OSS 或权限系统，只需要替换文档查询、权限判断和文件存储部分。

本节代码基于前面章节已经配置好的 `OnlyOfficeProperties`、`OnlyOfficeUrlBuilder` 和 `OnlyOfficeJwtService`。OnlyOffice 前端初始化时需要传入 `document`、`documentType`、`editorConfig` 等配置项；其中 `document.url` 用于让 OnlyOffice Docs 下载原始文件，`editorConfig.callbackUrl` 用于接收文档状态回调，`DocsAPI.DocEditor` 通过页面容器 ID 和配置对象创建编辑器实例。([ONLYOFFICE API](https://api.onlyoffice.com/docs/docs-api/usage-api/doceditor/?utm_source=chatgpt.com))

建议文件结构如下：

```text
src/main/java/io/github/atengk/onlyoffice
├── controller
│   └── OnlyOfficeController.java
├── service
│   ├── OnlyOfficeDocumentService.java
│   └── impl
│       └── OnlyOfficeDocumentServiceImpl.java
└── vo
    ├── DocumentInfo.java
    ├── OnlyOfficeCallbackRequest.java
    ├── OnlyOfficeCallbackResponse.java
    └── OnlyOfficeEditorConfigResponse.java
```

### 文档信息构建

文档信息构建用于将业务系统中的文档记录转换为 OnlyOffice 能识别的基础信息，例如文件名、文件类型、文档类型、文件路径、下载地址、回调地址和文档唯一标识。实际项目中这些信息通常来自数据库表，例如 `document_info`、`sys_file`、`file_record` 等；示例中先从本地目录读取文件。

文件位置：`src/main/java/io/github/atengk/onlyoffice/vo/DocumentInfo.java`

下面的类用于描述业务文档基础信息，后续生成 OnlyOffice 配置、文件下载和保存回调都会使用该对象。

```java
package io.github.atengk.onlyoffice.vo;

import lombok.Builder;
import lombok.Data;

import java.nio.file.Path;

/**
 * 文档基础信息
 *
 * @author Ateng
 * @since 2026-05-09
 */
@Data
@Builder
public class DocumentInfo {

    /**
     * 文档ID
     */
    private Long documentId;

    /**
     * 文件名称
     */
    private String fileName;

    /**
     * 文件扩展名
     */
    private String fileType;

    /**
     * OnlyOffice 文档类型：word、cell、slide
     */
    private String documentType;

    /**
     * 文档版本号
     */
    private Integer version;

    /**
     * 文件大小
     */
    private Long fileSize;

    /**
     * 本地文件路径
     */
    private Path filePath;

    /**
     * OnlyOffice 文档唯一标识
     */
    private String documentKey;
}
```

文件位置：`src/main/java/io/github/atengk/onlyoffice/vo/OnlyOfficeEditorConfigResponse.java`

下面的类用于返回前端页面需要的编辑器脚本地址和编辑器初始化配置。

```java
package io.github.atengk.onlyoffice.vo;

import lombok.Builder;
import lombok.Data;

import java.util.Map;

/**
 * OnlyOffice 编辑器配置响应
 *
 * @author Ateng
 * @since 2026-05-09
 */
@Data
@Builder
public class OnlyOfficeEditorConfigResponse {

    /**
     * OnlyOffice api.js 地址
     */
    private String apiJsUrl;

    /**
     * 编辑器初始化配置
     */
    private Map<String, Object> config;
}
```

文件位置：`src/main/java/io/github/atengk/onlyoffice/vo/OnlyOfficeCallbackRequest.java`

下面的类用于接收 OnlyOffice Docs 的回调请求。回调中的 `status` 表示文档状态，`url` 表示编辑后文件的下载地址；OnlyOffice 官方说明，`status=2` 表示文档准备好保存，`status=6` 表示强制保存场景下当前文档状态已保存。([ONLYOFFICE API](https://api.onlyoffice.com/docs/docs-api/usage-api/callback-handler/?utm_source=chatgpt.com))

```java
package io.github.atengk.onlyoffice.vo;

import lombok.Data;

import java.util.List;
import java.util.Map;

/**
 * OnlyOffice 回调请求
 *
 * @author Ateng
 * @since 2026-05-09
 */
@Data
public class OnlyOfficeCallbackRequest {

    /**
     * 文档唯一标识
     */
    private String key;

    /**
     * 文档状态
     */
    private Integer status;

    /**
     * 编辑后文件下载地址
     */
    private String url;

    /**
     * 文件类型
     */
    private String filetype;

    /**
     * 当前编辑用户
     */
    private List<String> users;

    /**
     * 变更历史地址
     */
    private String changesurl;

    /**
     * 强制保存类型
     */
    private Integer forcesavetype;

    /**
     * 历史版本信息
     */
    private Map<String, Object> history;

    /**
     * JWT Token，启用 JWT 后可能出现在请求体中
     */
    private String token;
}
```

文件位置：`src/main/java/io/github/atengk/onlyoffice/vo/OnlyOfficeCallbackResponse.java`

下面的类用于返回 OnlyOffice 回调处理结果。OnlyOffice 要求文档存储服务返回 `{"error":0}`，否则编辑器会显示错误信息。([ONLYOFFICE API](https://api.onlyoffice.com/docs/docs-api/usage-api/callback-handler/?utm_source=chatgpt.com))

```java
package io.github.atengk.onlyoffice.vo;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * OnlyOffice 回调响应
 *
 * @author Ateng
 * @since 2026-05-09
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class OnlyOfficeCallbackResponse {

    /**
     * 错误码，0表示成功
     */
    private Integer error;

    /**
     * 成功响应
     *
     * @return 回调响应
     */
    public static OnlyOfficeCallbackResponse success() {
        return new OnlyOfficeCallbackResponse(0);
    }

    /**
     * 失败响应
     *
     * @return 回调响应
     */
    public static OnlyOfficeCallbackResponse fail() {
        return new OnlyOfficeCallbackResponse(1);
    }
}
```

文档信息构建的核心规则如下：

| 字段           | 构建规则                                           |
| -------------- | -------------------------------------------------- |
| `documentId`   | 使用业务系统中的文档主键。                         |
| `fileName`     | 使用原始文件名，例如 `demo.docx`。                 |
| `fileType`     | 使用文件扩展名，例如 `docx`、`xlsx`、`pptx`。      |
| `documentType` | 根据文件扩展名映射为 `word`、`cell`、`slide`。     |
| `version`      | 本地示例固定为 `1`，正式项目建议使用数据库版本号。 |
| `documentKey`  | 建议由文档 ID 和版本号生成，例如 `doc-1001-v1`。   |
| `filePath`     | 指向本地磁盘文件路径或对象存储文件路径。           |

### 编辑器配置生成

编辑器配置生成是后端最核心的逻辑。前端并不直接拼接 OnlyOffice 配置，而是通过 Spring Boot 获取后端生成好的配置对象。这样可以保证文件地址、回调地址、权限、用户信息和 JWT Token 都由后端统一控制。

文件位置：`src/main/java/io/github/atengk/onlyoffice/service/OnlyOfficeDocumentService.java`

下面的接口定义 OnlyOffice 文档服务的核心能力，包括生成编辑器配置、读取文件资源和处理回调。

```java
package io.github.atengk.onlyoffice.service;

import io.github.atengk.onlyoffice.vo.OnlyOfficeCallbackRequest;
import io.github.atengk.onlyoffice.vo.OnlyOfficeCallbackResponse;
import io.github.atengk.onlyoffice.vo.OnlyOfficeEditorConfigResponse;
import org.springframework.core.io.Resource;

/**
 * OnlyOffice 文档服务
 *
 * @author Ateng
 * @since 2026-05-09
 */
public interface OnlyOfficeDocumentService {

    /**
     * 构建编辑器配置
     *
     * @param documentId 文档ID
     * @param mode 打开模式：view、edit
     * @return 编辑器配置
     */
    OnlyOfficeEditorConfigResponse buildEditorConfig(Long documentId, String mode);

    /**
     * 获取文档下载资源
     *
     * @param documentId 文档ID
     * @return 文件资源
     */
    Resource loadDocumentResource(Long documentId);

    /**
     * 获取文件名称
     *
     * @param documentId 文档ID
     * @return 文件名称
     */
    String getFileName(Long documentId);

    /**
     * 处理 OnlyOffice 回调
     *
     * @param documentId 文档ID
     * @param request 回调请求
     * @return 回调响应
     */
    OnlyOfficeCallbackResponse handleCallback(Long documentId, OnlyOfficeCallbackRequest request);
}
```

文件位置：`src/main/java/io/github/atengk/onlyoffice/service/impl/OnlyOfficeDocumentServiceImpl.java`

下面的服务实现用于完成本地文件版 OnlyOffice 集成。示例约定 `storage/files/{documentId}.docx` 为本地文件路径，例如文档 ID 为 `1001` 时，对应文件为 `storage/files/1001.docx`。

```java
package io.github.atengk.onlyoffice.service.impl;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.io.FileUtil;
import cn.hutool.core.lang.UUID;
import cn.hutool.core.map.MapUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.http.HttpUtil;
import io.github.atengk.onlyoffice.config.OnlyOfficeJwtService;
import io.github.atengk.onlyoffice.config.OnlyOfficeProperties;
import io.github.atengk.onlyoffice.config.OnlyOfficeUrlBuilder;
import io.github.atengk.onlyoffice.service.OnlyOfficeDocumentService;
import io.github.atengk.onlyoffice.vo.DocumentInfo;
import io.github.atengk.onlyoffice.vo.OnlyOfficeCallbackRequest;
import io.github.atengk.onlyoffice.vo.OnlyOfficeCallbackResponse;
import io.github.atengk.onlyoffice.vo.OnlyOfficeEditorConfigResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Service;

import java.io.File;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * OnlyOffice 文档服务实现
 *
 * @author Ateng
 * @since 2026-05-09
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class OnlyOfficeDocumentServiceImpl implements OnlyOfficeDocumentService {

    private static final String MODE_EDIT = "edit";

    private static final String MODE_VIEW = "view";

    private static final List<Integer> NEED_SAVE_STATUS_LIST = CollUtil.newArrayList(2, 6);

    private final OnlyOfficeProperties onlyOfficeProperties;

    private final OnlyOfficeUrlBuilder onlyOfficeUrlBuilder;

    private final OnlyOfficeJwtService onlyOfficeJwtService;

    /**
     * 构建编辑器配置
     *
     * @param documentId 文档ID
     * @param mode 打开模式：view、edit
     * @return 编辑器配置
     */
    @Override
    public OnlyOfficeEditorConfigResponse buildEditorConfig(Long documentId, String mode) {
        DocumentInfo documentInfo = this.buildDocumentInfo(documentId);
        boolean editMode = StrUtil.equalsIgnoreCase(MODE_EDIT, mode);

        Map<String, Object> config = new LinkedHashMap<>();
        config.put("documentType", documentInfo.getDocumentType());
        config.put("type", onlyOfficeProperties.getEditorType());
        config.put("height", "100%");
        config.put("width", "100%");
        config.put("document", this.buildDocumentConfig(documentInfo, editMode));
        config.put("editorConfig", this.buildEditorConfig(documentInfo, editMode));

        if (Boolean.TRUE.equals(onlyOfficeProperties.getJwt().getEnabled())) {
            String token = onlyOfficeJwtService.createToken(config);
            config.put("token", token);
        }

        log.info("OnlyOffice编辑器配置生成完成，文档ID：{}，模式：{}", documentId, editMode ? MODE_EDIT : MODE_VIEW);
        return OnlyOfficeEditorConfigResponse.builder()
                .apiJsUrl(onlyOfficeUrlBuilder.buildApiJsUrl())
                .config(config)
                .build();
    }

    /**
     * 获取文档下载资源
     *
     * @param documentId 文档ID
     * @return 文件资源
     */
    @Override
    public Resource loadDocumentResource(Long documentId) {
        DocumentInfo documentInfo = this.buildDocumentInfo(documentId);
        return new FileSystemResource(documentInfo.getFilePath());
    }

    /**
     * 获取文件名称
     *
     * @param documentId 文档ID
     * @return 文件名称
     */
    @Override
    public String getFileName(Long documentId) {
        return this.buildDocumentInfo(documentId).getFileName();
    }

    /**
     * 处理 OnlyOffice 回调
     *
     * @param documentId 文档ID
     * @param request 回调请求
     * @return 回调响应
     */
    @Override
    public OnlyOfficeCallbackResponse handleCallback(Long documentId, OnlyOfficeCallbackRequest request) {
        if (request == null || request.getStatus() == null) {
            log.warn("OnlyOffice回调参数为空，文档ID：{}", documentId);
            return OnlyOfficeCallbackResponse.fail();
        }

        log.info("收到OnlyOffice回调，文档ID：{}，状态：{}，Key：{}，用户：{}",
                documentId, request.getStatus(), request.getKey(), request.getUsers());

        if (!NEED_SAVE_STATUS_LIST.contains(request.getStatus())) {
            return OnlyOfficeCallbackResponse.success();
        }

        if (StrUtil.isBlank(request.getUrl())) {
            log.warn("OnlyOffice回调保存地址为空，文档ID：{}，状态：{}", documentId, request.getStatus());
            return OnlyOfficeCallbackResponse.fail();
        }

        try {
            this.saveEditedDocument(documentId, request);
            return OnlyOfficeCallbackResponse.success();
        } catch (Exception ex) {
            log.error("OnlyOffice文档保存失败，文档ID：{}，原因：{}", documentId, ex.getMessage(), ex);
            return OnlyOfficeCallbackResponse.fail();
        }
    }

    /**
     * 构建文档信息
     *
     * @param documentId 文档ID
     * @return 文档信息
     */
    private DocumentInfo buildDocumentInfo(Long documentId) {
        if (documentId == null) {
            throw new IllegalArgumentException("文档ID不能为空");
        }

        Path rootPath = Path.of(onlyOfficeProperties.getStorage().getRootPath()).toAbsolutePath().normalize();
        Path filePath = rootPath.resolve(documentId + ".docx").normalize();

        File file = filePath.toFile();
        if (!FileUtil.exist(file)) {
            throw new IllegalArgumentException(StrUtil.format("文档文件不存在，文档ID：{}，路径：{}", documentId, filePath));
        }

        String fileName = FileUtil.getName(file);
        String fileType = StrUtil.lowerFirst(FileUtil.extName(file));
        Integer version = 1;

        return DocumentInfo.builder()
                .documentId(documentId)
                .fileName(fileName)
                .fileType(fileType)
                .documentType(this.resolveDocumentType(fileType))
                .version(version)
                .fileSize(FileUtil.size(file))
                .filePath(filePath)
                .documentKey(StrUtil.format("doc-{}-v{}", documentId, version))
                .build();
    }

    /**
     * 构建 document 配置
     *
     * @param documentInfo 文档信息
     * @param editMode 是否编辑模式
     * @return document 配置
     */
    private Map<String, Object> buildDocumentConfig(DocumentInfo documentInfo, boolean editMode) {
        Map<String, Object> document = new LinkedHashMap<>();
        document.put("title", documentInfo.getFileName());
        document.put("url", onlyOfficeUrlBuilder.buildDownloadUrl(documentInfo.getDocumentId()));
        document.put("fileType", documentInfo.getFileType());
        document.put("key", documentInfo.getDocumentKey());
        document.put("permissions", this.buildPermissions(editMode));
        return document;
    }

    /**
     * 构建 editorConfig 配置
     *
     * @param documentInfo 文档信息
     * @param editMode 是否编辑模式
     * @return editorConfig 配置
     */
    private Map<String, Object> buildEditorConfig(DocumentInfo documentInfo, boolean editMode) {
        Map<String, Object> editorConfig = new LinkedHashMap<>();
        editorConfig.put("mode", editMode ? MODE_EDIT : MODE_VIEW);
        editorConfig.put("lang", onlyOfficeProperties.getLang());
        editorConfig.put("callbackUrl", onlyOfficeUrlBuilder.buildCallbackUrl(documentInfo.getDocumentId()));

        Map<String, Object> user = MapUtil.builder(new LinkedHashMap<String, Object>())
                .put("id", "10001")
                .put("name", "演示用户")
                .build();
        editorConfig.put("user", user);

        if (editMode) {
            Map<String, Object> coEditing = MapUtil.builder(new LinkedHashMap<String, Object>())
                    .put("mode", "fast")
                    .put("change", true)
                    .build();
            editorConfig.put("coEditing", coEditing);
        }

        return editorConfig;
    }

    /**
     * 构建文档权限
     *
     * @param editMode 是否编辑模式
     * @return 权限配置
     */
    private Map<String, Object> buildPermissions(boolean editMode) {
        return MapUtil.builder(new LinkedHashMap<String, Object>())
                .put("edit", editMode)
                .put("download", true)
                .put("print", true)
                .put("comment", editMode)
                .put("review", false)
                .build();
    }

    /**
     * 保存编辑后的文档
     *
     * @param documentId 文档ID
     * @param request 回调请求
     */
    private void saveEditedDocument(Long documentId, OnlyOfficeCallbackRequest request) {
        DocumentInfo documentInfo = this.buildDocumentInfo(documentId);

        String fileType = StrUtil.blankToDefault(request.getFiletype(), documentInfo.getFileType());
        String tempFileName = StrUtil.format("{}-{}.{}", documentId, UUID.fastUUID().toString(true), fileType);

        File tempFile = FileUtil.file(onlyOfficeProperties.getStorage().getTempPath(), "callback", tempFileName);
        FileUtil.mkParentDirs(tempFile);

        long size = HttpUtil.downloadFile(request.getUrl(), tempFile);
        if (size <= 0 || !FileUtil.exist(tempFile)) {
            throw new IllegalStateException("编辑后的文档下载失败");
        }

        File targetFile = documentInfo.getFilePath().toFile();
        FileUtil.copy(tempFile, targetFile, true);

        log.info("OnlyOffice文档保存成功，文档ID：{}，保存路径：{}，文件大小：{}",
                documentId, targetFile.getAbsolutePath(), FileUtil.size(targetFile));
    }

    /**
     * 解析 OnlyOffice 文档类型
     *
     * @param fileType 文件扩展名
     * @return 文档类型
     */
    private String resolveDocumentType(String fileType) {
        if (StrUtil.equalsAnyIgnoreCase(fileType, "doc", "docx", "odt", "rtf", "txt")) {
            return "word";
        }
        if (StrUtil.equalsAnyIgnoreCase(fileType, "xls", "xlsx", "ods", "csv")) {
            return "cell";
        }
        if (StrUtil.equalsAnyIgnoreCase(fileType, "ppt", "pptx", "odp")) {
            return "slide";
        }
        throw new IllegalArgumentException(StrUtil.format("暂不支持的文件类型：{}", fileType));
    }
}
```

这段实现中，`buildEditorConfig` 是编辑器配置生成入口，`loadDocumentResource` 是文件下载入口，`handleCallback` 是回调处理入口，`saveEditedDocument` 是文档保存入口。实际业务中建议将 `buildDocumentInfo` 替换为数据库查询，例如通过 MyBatis-Plus 查询文档表，再根据存储类型加载本地文件或对象存储文件。

### 文件下载接口

文件下载接口用于让 OnlyOffice Docs 下载原始文档。这里必须返回真实的文档二进制流，而不是业务 JSON。`document.url` 必须配置为 OnlyOffice Docs 服务可访问的绝对地址，否则编辑器页面会出现文档加载失败。

文件位置：`src/main/java/io/github/atengk/onlyoffice/controller/OnlyOfficeController.java`

下面的控制器提供编辑器配置接口、文件下载接口和回调接口。

```java
package io.github.atengk.onlyoffice.controller;

import cn.hutool.core.util.StrUtil;
import io.github.atengk.onlyoffice.service.OnlyOfficeDocumentService;
import io.github.atengk.onlyoffice.vo.OnlyOfficeCallbackRequest;
import io.github.atengk.onlyoffice.vo.OnlyOfficeCallbackResponse;
import io.github.atengk.onlyoffice.vo.OnlyOfficeEditorConfigResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.Resource;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.nio.charset.StandardCharsets;

/**
 * OnlyOffice 控制器
 *
 * @author Ateng
 * @since 2026-05-09
 */
@Slf4j
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/onlyoffice")
public class OnlyOfficeController {

    private final OnlyOfficeDocumentService onlyOfficeDocumentService;

    /**
     * 获取编辑器配置
     *
     * @param documentId 文档ID
     * @param mode 打开模式：view、edit
     * @return 编辑器配置
     */
    @GetMapping("/config/{documentId}")
    public OnlyOfficeEditorConfigResponse getEditorConfig(@PathVariable Long documentId,
                                                          @RequestParam(defaultValue = "view") String mode) {
        String openMode = StrUtil.equalsIgnoreCase("edit", mode) ? "edit" : "view";
        log.info("获取OnlyOffice编辑器配置，文档ID：{}，模式：{}", documentId, openMode);
        return onlyOfficeDocumentService.buildEditorConfig(documentId, openMode);
    }

    /**
     * 下载文档文件
     *
     * @param documentId 文档ID
     * @return 文件资源
     */
    @GetMapping("/file/download/{documentId}")
    public ResponseEntity<Resource> downloadFile(@PathVariable Long documentId) {
        Resource resource = onlyOfficeDocumentService.loadDocumentResource(documentId);
        String fileName = onlyOfficeDocumentService.getFileName(documentId);

        ContentDisposition contentDisposition = ContentDisposition.inline()
                .filename(fileName, StandardCharsets.UTF_8)
                .build();

        log.info("OnlyOffice下载文档文件，文档ID：{}，文件名：{}", documentId, fileName);
        return ResponseEntity.ok()
                .contentType(MediaType.APPLICATION_OCTET_STREAM)
                .header(HttpHeaders.CONTENT_DISPOSITION, contentDisposition.toString())
                .body(resource);
    }

    /**
     * 处理 OnlyOffice 回调
     *
     * @param documentId 文档ID
     * @param request 回调请求
     * @return 回调响应
     */
    @PostMapping("/callback/{documentId}")
    public OnlyOfficeCallbackResponse callback(@PathVariable Long documentId,
                                               @RequestBody OnlyOfficeCallbackRequest request) {
        return onlyOfficeDocumentService.handleCallback(documentId, request);
    }
}
```

接口说明如下：

| 接口                                         | 方法 | 说明                               |
| -------------------------------------------- | ---- | ---------------------------------- |
| `/api/onlyoffice/config/{documentId}`        | GET  | 获取 OnlyOffice 编辑器初始化配置。 |
| `/api/onlyoffice/file/download/{documentId}` | GET  | 下载原始文档文件。                 |
| `/api/onlyoffice/callback/{documentId}`      | POST | 接收 OnlyOffice Docs 保存回调。    |

接口调用示例：

```bash
# 获取预览模式配置
curl "http://localhost:8080/api/onlyoffice/config/1001?mode=view"

# 获取编辑模式配置
curl "http://localhost:8080/api/onlyoffice/config/1001?mode=edit"

# 下载文档文件
curl -I "http://localhost:8080/api/onlyoffice/file/download/1001"
```

### 回调处理接口

回调处理接口用于接收 OnlyOffice Docs 的文档状态通知。OnlyOffice 文档编辑服务会向 `callbackUrl` 发送 POST 请求，请求体中包含 `key`、`status`、`url`、`users` 等信息。`status=2` 表示文档已准备好保存，`url` 字段是编辑后文档的下载地址；回调接口必须返回 `{"error":0}`，否则编辑器会显示错误。([ONLYOFFICE API](https://api.onlyoffice.com/docs/docs-api/usage-api/callback-handler/?utm_source=chatgpt.com))

回调状态处理建议如下：

| status | 含义             | 后端处理                                |
| ------ | ---------------- | --------------------------------------- |
| `1`    | 文档正在编辑     | 记录日志即可。                          |
| `2`    | 文档准备好保存   | 下载 `url` 指向的新文件并保存。         |
| `3`    | 文档保存发生错误 | 记录错误日志，可返回失败。              |
| `4`    | 文档关闭且无修改 | 不需要保存。                            |
| `6`    | 强制保存成功     | 可下载 `url` 并保存当前版本或临时版本。 |
| `7`    | 强制保存失败     | 记录错误日志。                          |

回调请求示例：

```json
{
  "key": "doc-1001-v1",
  "status": 2,
  "url": "http://onlyoffice-documentserver/cache/files/edited-demo.docx",
  "filetype": "docx",
  "users": [
    "10001"
  ]
}
```

回调响应示例：

```json
{
  "error": 0
}
```

本地调试时可以使用以下命令模拟 OnlyOffice 保存回调：

```bash
curl -X POST "http://localhost:8080/api/onlyoffice/callback/1001" \
  -H "Content-Type: application/json" \
  -d '{
    "key": "doc-1001-v1",
    "status": 4,
    "users": ["10001"]
  }'
```

该命令只模拟 `status=4` 的无修改关闭场景，不会触发文件下载保存。真实保存场景中的 `url` 通常由 OnlyOffice Docs 生成，指向编辑后的临时文件地址。

### 文档保存逻辑

文档保存逻辑由回调接口触发。OnlyOffice Docs 不会直接把文件内容放在回调请求体中，而是把编辑后文件的下载地址放在 `url` 字段中。Spring Boot 需要读取该地址，下载新文件，然后覆盖原文件或生成新版本。([ONLYOFFICE API](https://api.onlyoffice.com/docs/docs-api/usage-api/callback-handler/?utm_source=chatgpt.com))

保存逻辑建议按以下顺序处理：

```text
接收回调请求
  ↓
判断 status 是否需要保存
  ↓
校验 url 是否为空
  ↓
下载编辑后的文件到临时目录
  ↓
校验临时文件是否存在、大小是否正常
  ↓
覆盖原文件或保存为新版本
  ↓
更新文档版本、大小、更新时间、documentKey
  ↓
记录保存日志
  ↓
返回 {"error":0}
```

本地文件版可以直接覆盖原文件。正式业务系统建议使用版本化保存，例如：

| 保存方式     | 说明                                                   |
| ------------ | ------------------------------------------------------ |
| 覆盖保存     | 编辑后文件直接覆盖原文件，实现简单，但不保留历史版本。 |
| 版本保存     | 每次保存生成新版本文件，例如 `1001_v2.docx`，可回滚。  |
| 临时保存     | 强制保存时先保存到临时版本，用户关闭后再生成正式版本。 |
| 对象存储保存 | 下载后上传到 MinIO、OSS、COS 或 S3，再更新文件记录。   |

保存成功后需要更新 `documentKey`。OnlyOffice 使用 `document.key` 识别文档版本，如果文档内容已经变化但仍使用旧 key，可能会继续命中旧缓存。官方文档也将 `key` 定义为文档标识，配置中需要保证它能代表当前文档版本。([ONLYOFFICE API](https://api.onlyoffice.com/docs/docs-api/usage-api/doceditor/?utm_source=chatgpt.com))

生产环境建议增加以下处理：

| 处理项       | 说明                                     |
| ------------ | ---------------------------------------- |
| 回调鉴权     | 校验 JWT、来源 IP 或内部网关签名。       |
| 异常告警     | 下载失败、保存失败、文件为空时发送告警。 |
| 并发控制     | 多人协作编辑时避免保存覆盖冲突。         |
| 文件锁       | 审批、归档、删除中的文档禁止保存。       |
| 版本记录     | 保存成功后写入文档版本表。               |
| 临时文件清理 | 定时清理 `storage/temp` 下的过期文件。   |

## 前端接入方式

本章节说明 Vue 3 前端如何接入 OnlyOffice 编辑器。前端主要负责加载 OnlyOffice Docs 的 `api.js`、请求 Spring Boot 获取编辑器配置、创建 `DocsAPI.DocEditor` 实例，以及根据业务按钮切换预览模式和编辑模式。`DocsAPI.DocEditor` 是 OnlyOffice Docs API 的主入口，创建时需要传入页面容器 ID 和配置对象。([ONLYOFFICE API](https://api.onlyoffice.com/docs/docs-api/usage-api/doceditor/?utm_source=chatgpt.com))

### 编辑器页面初始化

编辑器页面初始化分为三步：先请求后端获取配置，再动态加载 `api.js`，最后创建编辑器实例。前端不要硬编码 `document.url`、`callbackUrl`、`token` 和权限配置，这些都应由 Spring Boot 后端返回。

文件位置：`src/api/onlyoffice.ts`

下面的 API 文件用于请求后端获取 OnlyOffice 编辑器配置。

```typescript
import axios from 'axios'

export interface OnlyOfficeEditorConfigResponse {
  apiJsUrl: string
  config: Record<string, any>
}

/**
 * 获取 OnlyOffice 编辑器配置
 *
 * @param documentId 文档ID
 * @param mode 打开模式
 */
export function getOnlyOfficeConfig(documentId: string | number, mode: 'view' | 'edit') {
  return axios.get<OnlyOfficeEditorConfigResponse>(`/api/onlyoffice/config/${documentId}`, {
    params: {
      mode
    }
  })
}
```

文件位置：`src/views/onlyoffice/OnlyOfficeEditor.vue`

下面的页面组件用于加载 OnlyOffice 编辑器。页面通过路由参数读取 `documentId` 和 `mode`，然后初始化编辑器。

```vue
<template>
  <div class="onlyoffice-page">
    <div class="onlyoffice-toolbar">
      <div class="onlyoffice-title">
        <span>OnlyOffice 文档编辑器</span>
        <span class="onlyoffice-mode">{{ modeText }}</span>
      </div>

      <div class="onlyoffice-actions">
        <button
          type="button"
          :class="{ active: mode === 'view' }"
          @click="switchMode('view')"
        >
          预览
        </button>
        <button
          type="button"
          :class="{ active: mode === 'edit' }"
          @click="switchMode('edit')"
        >
          编辑
        </button>
      </div>
    </div>

    <div v-if="loading" class="onlyoffice-loading">
      编辑器加载中...
    </div>

    <div v-if="errorMessage" class="onlyoffice-error">
      {{ errorMessage }}
    </div>

    <div id="onlyoffice-editor" class="onlyoffice-editor"></div>
  </div>
</template>

<script setup lang="ts">
import { computed, nextTick, onBeforeUnmount, onMounted, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { getOnlyOfficeConfig } from '@/api/onlyoffice'

declare global {
  interface Window {
    DocsAPI?: {
      DocEditor: new (placeholder: string, config: Record<string, any>) => any
    }
  }
}

const route = useRoute()
const router = useRouter()

const loading = ref(false)
const errorMessage = ref('')
const mode = ref<'view' | 'edit'>((route.query.mode as 'view' | 'edit') || 'view')
const docEditor = ref<any>(null)

const documentId = computed(() => String(route.params.documentId || route.query.documentId || ''))

const modeText = computed(() => {
  return mode.value === 'edit' ? '编辑模式' : '预览模式'
})

onMounted(async () => {
  await initEditor()
})

onBeforeUnmount(() => {
  destroyEditor()
})

/**
 * 初始化编辑器
 */
async function initEditor() {
  if (!documentId.value) {
    errorMessage.value = '文档ID不能为空'
    return
  }

  loading.value = true
  errorMessage.value = ''

  try {
    destroyEditor()

    const { data } = await getOnlyOfficeConfig(documentId.value, mode.value)
    await loadOnlyOfficeScript(data.apiJsUrl)
    await nextTick()

    if (!window.DocsAPI) {
      throw new Error('OnlyOffice DocsAPI 加载失败')
    }

    docEditor.value = new window.DocsAPI.DocEditor('onlyoffice-editor', data.config)
  } catch (error: any) {
    errorMessage.value = error?.message || 'OnlyOffice 编辑器初始化失败'
    console.error('OnlyOffice编辑器初始化失败：', error)
  } finally {
    loading.value = false
  }
}

/**
 * 加载 OnlyOffice api.js
 *
 * @param apiJsUrl api.js 地址
 */
function loadOnlyOfficeScript(apiJsUrl: string) {
  return new Promise<void>((resolve, reject) => {
    const existScript = document.querySelector<HTMLScriptElement>(`script[src="${apiJsUrl}"]`)
    if (existScript) {
      resolve()
      return
    }

    const script = document.createElement('script')
    script.src = apiJsUrl
    script.type = 'text/javascript'
    script.onload = () => resolve()
    script.onerror = () => reject(new Error('OnlyOffice api.js 加载失败'))
    document.body.appendChild(script)
  })
}

/**
 * 切换打开模式
 *
 * @param targetMode 目标模式
 */
async function switchMode(targetMode: 'view' | 'edit') {
  if (mode.value === targetMode) {
    return
  }

  mode.value = targetMode
  await router.replace({
    query: {
      ...route.query,
      mode: targetMode
    }
  })

  await initEditor()
}

/**
 * 销毁编辑器
 */
function destroyEditor() {
  if (docEditor.value && typeof docEditor.value.destroyEditor === 'function') {
    docEditor.value.destroyEditor()
  }
  docEditor.value = null
}
</script>

<style scoped lang="scss">
.onlyoffice-page {
  display: flex;
  flex-direction: column;
  width: 100%;
  height: 100vh;
  background: #f5f7fa;
}

.onlyoffice-toolbar {
  display: flex;
  align-items: center;
  justify-content: space-between;
  height: 48px;
  padding: 0 16px;
  background: #ffffff;
  border-bottom: 1px solid #e5e7eb;
}

.onlyoffice-title {
  display: flex;
  align-items: center;
  gap: 12px;
  font-size: 15px;
  font-weight: 600;
}

.onlyoffice-mode {
  padding: 2px 8px;
  font-size: 12px;
  color: #606266;
  background: #f2f3f5;
  border-radius: 4px;
}

.onlyoffice-actions {
  display: flex;
  gap: 8px;
}

.onlyoffice-actions button {
  padding: 6px 14px;
  cursor: pointer;
  background: #ffffff;
  border: 1px solid #dcdfe6;
  border-radius: 4px;
}

.onlyoffice-actions button.active {
  color: #ffffff;
  background: #409eff;
  border-color: #409eff;
}

.onlyoffice-loading,
.onlyoffice-error {
  padding: 12px 16px;
  font-size: 14px;
}

.onlyoffice-error {
  color: #f56c6c;
}

.onlyoffice-editor {
  flex: 1;
  width: 100%;
  min-height: 0;
}
</style>
```

路由配置示例：

```typescript
{
  path: '/onlyoffice/:documentId',
  name: 'OnlyOfficeEditor',
  component: () => import('@/views/onlyoffice/OnlyOfficeEditor.vue')
}
```

访问示例：

```text
http://localhost:5173/onlyoffice/1001?mode=view
http://localhost:5173/onlyoffice/1001?mode=edit
```

### DocumentEditor 参数配置

`DocumentEditor` 参数配置由后端返回，前端只负责透传给 `new DocsAPI.DocEditor('onlyoffice-editor', config)`。OnlyOffice 官方示例中，最小配置包含 `document.fileType`、`document.key`、`document.title`、`document.url`、`documentType` 和 `editorConfig.callbackUrl`。([ONLYOFFICE API](https://api.onlyoffice.com/docs/docs-api/usage-api/doceditor/?utm_source=chatgpt.com))

典型配置结构如下：

```json
{
  "documentType": "word",
  "type": "desktop",
  "height": "100%",
  "width": "100%",
  "document": {
    "title": "1001.docx",
    "url": "http://host.docker.internal:8080/api/onlyoffice/file/download/1001",
    "fileType": "docx",
    "key": "doc-1001-v1",
    "permissions": {
      "edit": true,
      "download": true,
      "print": true,
      "comment": true,
      "review": false
    }
  },
  "editorConfig": {
    "mode": "edit",
    "lang": "zh",
    "callbackUrl": "http://host.docker.internal:8080/api/onlyoffice/callback/1001",
    "user": {
      "id": "10001",
      "name": "演示用户"
    },
    "coEditing": {
      "mode": "fast",
      "change": true
    }
  },
  "token": "后端生成的JWT_TOKEN"
}
```

关键参数说明如下：

| 参数                        | 说明                                         |
| --------------------------- | -------------------------------------------- |
| `documentType`              | 文档类型，常见值为 `word`、`cell`、`slide`。 |
| `type`                      | 编辑器类型，常用 `desktop`。                 |
| `document.title`            | 编辑器中显示的文件名。                       |
| `document.url`              | OnlyOffice Docs 下载原始文件的地址。         |
| `document.fileType`         | 文件扩展名，例如 `docx`、`xlsx`、`pptx`。    |
| `document.key`              | 文档唯一标识，建议包含文档 ID 和版本号。     |
| `document.permissions.edit` | 是否允许编辑。                               |
| `editorConfig.mode`         | 打开模式，`view` 或 `edit`。                 |
| `editorConfig.callbackUrl`  | OnlyOffice Docs 保存回调地址。               |
| `editorConfig.user`         | 当前用户信息，用于协同编辑和日志识别。       |
| `token`                     | 启用 JWT 后由后端生成的签名 Token。          |

权限配置需要注意 `edit` 和 `comment` 的关系。OnlyOffice 权限文档说明，`comment` 用于控制是否允许评论；评论功能只有在编辑器模式为 `edit` 时才可用，且 `comment` 默认值会受 `edit` 影响。([ONLYOFFICE API](https://api.onlyoffice.com/docs/docs-api/usage-api/config/document/permissions/?utm_source=chatgpt.com))

### 预览与编辑模式切换

预览与编辑模式切换不建议只在前端修改 `mode` 字段完成。正确做法是前端切换模式后重新请求后端配置，由后端重新判断当前用户是否具备编辑权限，再返回对应的 `editorConfig.mode` 和 `document.permissions`。

预览模式配置重点如下：

```json
{
  "document": {
    "permissions": {
      "edit": false,
      "download": true,
      "print": true,
      "comment": false,
      "review": false
    }
  },
  "editorConfig": {
    "mode": "view"
  }
}
```

编辑模式配置重点如下：

```json
{
  "document": {
    "permissions": {
      "edit": true,
      "download": true,
      "print": true,
      "comment": true,
      "review": false
    }
  },
  "editorConfig": {
    "mode": "edit"
  }
}
```

推荐切换流程如下：

```text
用户点击预览或编辑按钮
  ↓
前端更新 mode 参数
  ↓
前端销毁当前 DocEditor 实例
  ↓
前端重新请求 /api/onlyoffice/config/{documentId}?mode=xxx
  ↓
后端校验当前用户权限
  ↓
后端返回新的 OnlyOffice 配置
  ↓
前端重新创建 DocsAPI.DocEditor
```

实际项目中，后端必须控制最终权限。例如用户点击“编辑”按钮时，如果当前用户没有编辑权限，后端应返回 `mode=view` 或直接返回无权限错误，而不能依赖前端隐藏按钮。

权限判断建议如下：

| 场景                       | 后端处理                                  |
| -------------------------- | ----------------------------------------- |
| 用户有查看权限，无编辑权限 | 返回预览配置，`edit=false`，`mode=view`。 |
| 用户有编辑权限，文档可编辑 | 返回编辑配置，`edit=true`，`mode=edit`。  |
| 文档已归档                 | 禁止编辑，只允许预览。                    |
| 文档被锁定                 | 禁止编辑，提示文档正在被处理。            |
| 用户无查看权限             | 直接返回 403 或业务无权限错误。           |

前端切换时需要先销毁旧编辑器实例，否则同一个页面容器中可能残留旧的 OnlyOffice iframe，造成重复加载、页面空白或模式切换不生效。OnlyOffice `DocEditor` 实例提供 `destroyEditor` 方法，可在组件卸载或重新初始化前调用。([ONLYOFFICE API](https://api.onlyoffice.com/docs/docs-api/usage-api/doceditor/?utm_source=chatgpt.com))



## 权限与安全

本章节主要说明 Spring Boot 集成 OnlyOffice 时的权限控制和安全校验。OnlyOffice 的权限配置可以控制文档是否允许编辑、下载、打印、评论等操作，但这些配置不能替代后端权限校验；后端仍然必须在生成编辑器配置、下载文件、处理回调和保存文件时进行业务级校验。OnlyOffice 官方权限配置中，`permissions` 用于控制文档是否可编辑、下载、评论等，其中 `comment` 的可用性还会受到编辑器模式影响。([ONLYOFFICE API](https://api.onlyoffice.com/docs/docs-api/usage-api/config/document/permissions/?utm_source=chatgpt.com))

### 文档访问权限控制

文档访问权限控制用于判断当前用户是否可以查看某个文档。访问权限不能只依赖前端路由或按钮隐藏，因为 OnlyOffice Docs 会直接访问 Spring Boot 的文件下载接口；如果下载接口不做权限校验，用户即使没有页面入口，也可能通过文档 ID 拼接下载地址获取文件。

访问权限建议在以下位置进行校验：

| 校验位置           | 说明                                                       |
| ------------------ | ---------------------------------------------------------- |
| 获取编辑器配置接口 | 用户无查看权限时，不返回 OnlyOffice 配置。                 |
| 文件下载接口       | OnlyOffice Docs 下载文件前，后端需要确认下载地址是否合法。 |
| 文档详情接口       | 业务页面展示文档信息前，需要校验当前用户是否可见。         |
| 文档列表接口       | 只返回当前用户有权访问的文档。                             |

基础权限判断可以先按“文档所有者、所属部门、共享范围、管理员角色”进行设计。

文件位置：`src/main/java/io/github/atengk/onlyoffice/security/DocumentPermissionResult.java`

下面的类用于封装文档权限判断结果，便于后续生成预览模式、编辑模式和文件下载校验。

```java
package io.github.atengk.onlyoffice.security;

import lombok.Builder;
import lombok.Data;

/**
 * 文档权限判断结果
 *
 * @author Ateng
 * @since 2026-05-09
 */
@Data
@Builder
public class DocumentPermissionResult {

    /**
     * 是否允许查看
     */
    private Boolean allowView;

    /**
     * 是否允许编辑
     */
    private Boolean allowEdit;

    /**
     * 是否允许下载
     */
    private Boolean allowDownload;

    /**
     * 是否允许打印
     */
    private Boolean allowPrint;

    /**
     * 无权限原因
     */
    private String reason;
}
```

文件位置：`src/main/java/io/github/atengk/onlyoffice/security/DocumentPermissionService.java`

下面的接口用于统一处理文档访问权限和编辑权限。实际项目中可以对接 Sa-Token、Spring Security、RBAC 权限表、部门数据权限或租户权限。

```java
package io.github.atengk.onlyoffice.security;

/**
 * 文档权限服务
 *
 * @author Ateng
 * @since 2026-05-09
 */
public interface DocumentPermissionService {

    /**
     * 校验文档权限
     *
     * @param documentId 文档ID
     * @param userId 用户ID
     * @return 权限判断结果
     */
    DocumentPermissionResult checkPermission(Long documentId, String userId);
}
```

文件位置：`src/main/java/io/github/atengk/onlyoffice/security/DefaultDocumentPermissionService.java`

下面的示例实现用于演示权限控制逻辑。正式项目中应将 `userId`、角色、部门、租户和文档状态从登录上下文及数据库中读取。

```java
package io.github.atengk.onlyoffice.security;

import cn.hutool.core.util.StrUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

/**
 * 默认文档权限服务
 *
 * @author Ateng
 * @since 2026-05-09
 */
@Slf4j
@Service
public class DefaultDocumentPermissionService implements DocumentPermissionService {

    /**
     * 校验文档权限
     *
     * @param documentId 文档ID
     * @param userId 用户ID
     * @return 权限判断结果
     */
    @Override
    public DocumentPermissionResult checkPermission(Long documentId, String userId) {
        if (documentId == null) {
            return DocumentPermissionResult.builder()
                    .allowView(false)
                    .allowEdit(false)
                    .allowDownload(false)
                    .allowPrint(false)
                    .reason("文档ID不能为空")
                    .build();
        }

        if (StrUtil.isBlank(userId)) {
            log.warn("文档权限校验失败，用户ID为空，文档ID：{}", documentId);
            return DocumentPermissionResult.builder()
                    .allowView(false)
                    .allowEdit(false)
                    .allowDownload(false)
                    .allowPrint(false)
                    .reason("用户未登录")
                    .build();
        }

        // 示例逻辑：演示用户允许查看、编辑、下载和打印
        boolean demoUser = StrUtil.equals(userId, "10001");

        return DocumentPermissionResult.builder()
                .allowView(demoUser)
                .allowEdit(demoUser)
                .allowDownload(demoUser)
                .allowPrint(demoUser)
                .reason(demoUser ? null : "用户无文档访问权限")
                .build();
    }
}
```

接入权限服务后，生成编辑器配置时应先校验查看权限。用户没有查看权限时，直接返回 403 或业务异常，不应返回 OnlyOffice 配置。

核心处理逻辑如下：

```java
DocumentPermissionResult permission = documentPermissionService.checkPermission(documentId, currentUserId);

if (!Boolean.TRUE.equals(permission.getAllowView())) {
    throw new AccessDeniedException(permission.getReason());
}
```

文档下载接口也需要校验。OnlyOffice Docs 访问 `document.url` 时，本质上也是访问 Spring Boot 文件下载接口。如果下载地址是公网可访问地址，建议通过短期签名参数、一次性访问令牌、网关内网访问或 OnlyOffice 专用下载授权来降低文件泄露风险。

### 编辑权限控制

编辑权限控制用于判断当前用户是否可以修改文档内容。OnlyOffice 配置中可以通过 `editorConfig.mode` 和 `document.permissions.edit` 控制编辑器是否进入编辑状态，但最终权限仍然必须由 Spring Boot 决定。OnlyOffice 权限文档中，`edit` 控制是否允许编辑，`comment` 控制是否允许评论，且评论功能在编辑器模式为 `edit` 时才可用于编辑器。([ONLYOFFICE API](https://api.onlyoffice.com/docs/docs-api/usage-api/config/document/permissions/?utm_source=chatgpt.com))

编辑权限建议同时受以下因素影响：

| 因素     | 说明                                                   |
| -------- | ------------------------------------------------------ |
| 用户权限 | 当前用户是否具备该文档的编辑权限。                     |
| 文档状态 | 已归档、已发布、审批完成、已锁定的文档通常不允许编辑。 |
| 文件类型 | 部分格式只允许预览，不允许在线编辑。                   |
| 租户范围 | 多租户系统中用户只能编辑本租户文档。                   |
| 业务流程 | 流程审批中的文档可能只允许指定节点处理人编辑。         |

生成编辑器权限配置时，不要直接使用前端传入的 `mode`。前端传入的 `mode=edit` 只能表示用户希望编辑，后端需要重新判断是否真正允许编辑。

示例逻辑如下：

```java
boolean requestEditMode = StrUtil.equalsIgnoreCase("edit", mode);
DocumentPermissionResult permission = documentPermissionService.checkPermission(documentId, currentUserId);

if (!Boolean.TRUE.equals(permission.getAllowView())) {
    throw new AccessDeniedException(permission.getReason());
}

boolean realEditMode = requestEditMode && Boolean.TRUE.equals(permission.getAllowEdit());
```

权限映射建议如下：

| 后端判断结果           | `editorConfig.mode` | `permissions.edit` | 说明                             |
| ---------------------- | ------------------- | ------------------ | -------------------------------- |
| 有查看权限，无编辑权限 | `view`              | `false`            | 只允许预览。                     |
| 有查看权限，有编辑权限 | `edit`              | `true`             | 允许在线编辑。                   |
| 无查看权限             | 不返回配置          | 不返回配置         | 直接返回无权限。                 |
| 文档已归档             | `view`              | `false`            | 即使用户有编辑权限，也只能查看。 |
| 文档被锁定             | `view` 或返回错误   | `false`            | 根据业务规则处理。               |

在前面 `OnlyOfficeDocumentServiceImpl` 的 `buildPermissions` 方法中，可以改造为接收完整权限对象。

```java
/**
 * 构建文档权限
 *
 * @param editMode 是否编辑模式
 * @param permission 权限判断结果
 * @return 权限配置
 */
private Map<String, Object> buildPermissions(boolean editMode, DocumentPermissionResult permission) {
    return MapUtil.builder(new LinkedHashMap<String, Object>())
            .put("edit", editMode && Boolean.TRUE.equals(permission.getAllowEdit()))
            .put("download", Boolean.TRUE.equals(permission.getAllowDownload()))
            .put("print", Boolean.TRUE.equals(permission.getAllowPrint()))
            .put("comment", editMode && Boolean.TRUE.equals(permission.getAllowEdit()))
            .put("review", false)
            .build();
}
```

编辑权限还需要与保存回调联动。不能认为用户能打开编辑器就一定可以保存，回调保存时仍然要根据 `documentId`、`key`、文档状态和版本号校验当前文档是否允许被覆盖或生成新版本。

### 回调请求校验

回调请求校验用于确认请求确实来自可信的 OnlyOffice Docs 服务，并且回调内容与当前业务文档匹配。OnlyOffice 回调会使用 `callbackUrl` 向文档存储服务发送 POST 请求；回调体中包含 `key`、`status`、`url`、`users` 等字段，其中 `status=2` 表示文档准备好保存，`url` 表示编辑后文件的下载地址，文档存储服务需要返回 `{"error":0}`。([ONLYOFFICE API](https://api.onlyoffice.com/docs/docs-api/usage-api/callback-handler/?utm_source=chatgpt.com))

回调请求建议校验以下内容：

| 校验项       | 说明                                                     |
| ------------ | -------------------------------------------------------- |
| `documentId` | 回调路径中的文档 ID 必须存在。                           |
| `key`        | 回调中的 `key` 必须与当前文档版本的 `documentKey` 一致。 |
| `status`     | 必须是 OnlyOffice 支持的状态值。                         |
| `url`        | 当 `status=2` 或 `status=6` 时，`url` 不能为空。         |
| JWT Token    | 启用 JWT 后应校验请求 Token 或请求头 Token。             |
| 来源地址     | 生产环境可限制 OnlyOffice Docs 服务 IP 或网关来源。      |
| 文档状态     | 已归档、已删除、已锁定文档不允许被保存覆盖。             |

OnlyOffice JWT 用于保护文档访问和服务间通信。官方说明中，JWT Token 会在初始化 Document Editor 时加入配置，并在内部服务命令交换时使用相同密钥签名和验证；从 OnlyOffice Docs 7.2 起，JWT 默认启用并会自动生成密钥，因此生产部署应显式配置自己的密钥，并在连接端使用同一个密钥。([ONLYOFFICE - Cloud Office Applications](https://helpcenter.onlyoffice.com/docs/installation/docs-configure-jwt.aspx?utm_source=chatgpt.com))

文件位置：`src/main/java/io/github/atengk/onlyoffice/security/OnlyOfficeCallbackValidator.java`

下面的校验器用于集中处理回调参数校验。示例重点校验 `status`、`key`、`url`，JWT 校验可以复用前面章节的 `OnlyOfficeJwtService`。

```java
package io.github.atengk.onlyoffice.security;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.StrUtil;
import io.github.atengk.onlyoffice.config.OnlyOfficeJwtService;
import io.github.atengk.onlyoffice.vo.DocumentInfo;
import io.github.atengk.onlyoffice.vo.OnlyOfficeCallbackRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * OnlyOffice 回调校验器
 *
 * @author Ateng
 * @since 2026-05-09
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class OnlyOfficeCallbackValidator {

    private static final List<Integer> VALID_STATUS_LIST = CollUtil.newArrayList(1, 2, 3, 4, 6, 7);

    private static final List<Integer> NEED_URL_STATUS_LIST = CollUtil.newArrayList(2, 6);

    private final OnlyOfficeJwtService onlyOfficeJwtService;

    /**
     * 校验回调请求
     *
     * @param documentInfo 文档信息
     * @param request 回调请求
     * @return 是否校验通过
     */
    public boolean validate(DocumentInfo documentInfo, OnlyOfficeCallbackRequest request) {
        if (documentInfo == null || request == null) {
            log.warn("OnlyOffice回调校验失败，文档信息或请求体为空");
            return false;
        }

        if (!VALID_STATUS_LIST.contains(request.getStatus())) {
            log.warn("OnlyOffice回调校验失败，非法状态值，文档ID：{}，状态：{}",
                    documentInfo.getDocumentId(), request.getStatus());
            return false;
        }

        if (StrUtil.isBlank(request.getKey())) {
            log.warn("OnlyOffice回调校验失败，Key为空，文档ID：{}", documentInfo.getDocumentId());
            return false;
        }

        if (!StrUtil.equals(request.getKey(), documentInfo.getDocumentKey())) {
            log.warn("OnlyOffice回调校验失败，Key不一致，文档ID：{}，请求Key：{}，当前Key：{}",
                    documentInfo.getDocumentId(), request.getKey(), documentInfo.getDocumentKey());
            return false;
        }

        if (NEED_URL_STATUS_LIST.contains(request.getStatus()) && StrUtil.isBlank(request.getUrl())) {
            log.warn("OnlyOffice回调校验失败，保存地址为空，文档ID：{}，状态：{}",
                    documentInfo.getDocumentId(), request.getStatus());
            return false;
        }

        if (StrUtil.isNotBlank(request.getToken()) && !onlyOfficeJwtService.verifyToken(request.getToken())) {
            log.warn("OnlyOffice回调校验失败，JWT Token无效，文档ID：{}", documentInfo.getDocumentId());
            return false;
        }

        return true;
    }
}
```

在回调处理方法中建议先校验，再保存文件。

```java
if (!onlyOfficeCallbackValidator.validate(documentInfo, request)) {
    return OnlyOfficeCallbackResponse.fail();
}
```

生产环境还可以增加请求来源校验。常见方式包括：

| 方式         | 说明                                                         |
| ------------ | ------------------------------------------------------------ |
| 内网部署     | OnlyOffice Docs 与 Spring Boot 在同一内网，不直接暴露回调接口到公网。 |
| 网关白名单   | Nginx 或网关只允许 OnlyOffice Docs 服务器 IP 访问回调路径。  |
| 独立回调路径 | 回调接口使用内部域名，例如 `http://api-internal.example.com`。 |
| 短期签名参数 | `callbackUrl` 拼接短期签名，回调时后端校验签名。             |
| JWT 校验     | 对初始化配置和回调请求中的 Token 进行校验。                  |

## 文件管理

本章节主要说明 OnlyOffice 集成中的文件存储、对象存储扩展和文档版本管理。OnlyOffice Docs 负责编辑过程，但最终文件仍然需要由业务系统保存和管理。Spring Boot 需要明确文档文件放在哪里、如何下载、如何覆盖或生成版本，以及如何保证缓存和版本号一致。

### 本地文件存储

本地文件存储适合开发环境、单机部署或小规模内部系统。它的优点是实现简单、调试方便；缺点是扩展性弱，不适合多实例部署，也不适合海量文件管理。

推荐本地存储目录如下：

```text
storage
├── files
│   └── 2026
│       └── 05
│           └── 1001
│               ├── v1.docx
│               └── v2.docx
└── temp
    ├── callback
    └── download
```

目录说明如下：

| 目录                                     | 说明                                         |
| ---------------------------------------- | -------------------------------------------- |
| `storage/files`                          | 保存正式文档文件。                           |
| `storage/files/{yyyy}/{MM}/{documentId}` | 按年月和文档 ID 分目录，避免单目录文件过多。 |
| `storage/temp/callback`                  | 保存 OnlyOffice 回调下载下来的临时文件。     |
| `storage/temp/download`                  | 保存其他下载或转换过程中的临时文件。         |

文件位置：`src/main/java/io/github/atengk/onlyoffice/storage/DocumentStorageService.java`

下面的接口用于抽象文档存储能力。后续本地存储、MinIO、OSS、COS、S3 都可以实现该接口。

```java
package io.github.atengk.onlyoffice.storage;

import org.springframework.core.io.Resource;

import java.io.File;
import java.io.InputStream;

/**
 * 文档存储服务
 *
 * @author Ateng
 * @since 2026-05-09
 */
public interface DocumentStorageService {

    /**
     * 读取文档资源
     *
     * @param storagePath 存储路径
     * @return 文档资源
     */
    Resource loadAsResource(String storagePath);

    /**
     * 保存文档文件
     *
     * @param storagePath 存储路径
     * @param sourceFile 源文件
     */
    void save(String storagePath, File sourceFile);

    /**
     * 保存文档输入流
     *
     * @param storagePath 存储路径
     * @param inputStream 输入流
     */
    void save(String storagePath, InputStream inputStream);

    /**
     * 判断文件是否存在
     *
     * @param storagePath 存储路径
     * @return 是否存在
     */
    boolean exists(String storagePath);
}
```

文件位置：`src/main/java/io/github/atengk/onlyoffice/storage/LocalDocumentStorageService.java`

下面的实现类用于本地磁盘存储，适合开发阶段和单机部署。

```java
package io.github.atengk.onlyoffice.storage;

import cn.hutool.core.io.FileUtil;
import cn.hutool.core.util.StrUtil;
import io.github.atengk.onlyoffice.config.OnlyOfficeProperties;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Service;

import java.io.File;
import java.io.InputStream;
import java.nio.file.Path;

/**
 * 本地文档存储服务
 *
 * @author Ateng
 * @since 2026-05-09
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class LocalDocumentStorageService implements DocumentStorageService {

    private final OnlyOfficeProperties onlyOfficeProperties;

    /**
     * 读取文档资源
     *
     * @param storagePath 存储路径
     * @return 文档资源
     */
    @Override
    public Resource loadAsResource(String storagePath) {
        File file = this.resolveFile(storagePath);
        if (!FileUtil.exist(file)) {
            throw new IllegalArgumentException(StrUtil.format("文档文件不存在，路径：{}", file.getAbsolutePath()));
        }
        return new FileSystemResource(file);
    }

    /**
     * 保存文档文件
     *
     * @param storagePath 存储路径
     * @param sourceFile 源文件
     */
    @Override
    public void save(String storagePath, File sourceFile) {
        if (!FileUtil.exist(sourceFile)) {
            throw new IllegalArgumentException("源文件不存在");
        }

        File targetFile = this.resolveFile(storagePath);
        FileUtil.mkParentDirs(targetFile);
        FileUtil.copy(sourceFile, targetFile, true);

        log.info("本地文档保存成功，路径：{}，大小：{}", targetFile.getAbsolutePath(), FileUtil.size(targetFile));
    }

    /**
     * 保存文档输入流
     *
     * @param storagePath 存储路径
     * @param inputStream 输入流
     */
    @Override
    public void save(String storagePath, InputStream inputStream) {
        File targetFile = this.resolveFile(storagePath);
        FileUtil.mkParentDirs(targetFile);
        FileUtil.writeFromStream(inputStream, targetFile);

        log.info("本地文档流保存成功，路径：{}，大小：{}", targetFile.getAbsolutePath(), FileUtil.size(targetFile));
    }

    /**
     * 判断文件是否存在
     *
     * @param storagePath 存储路径
     * @return 是否存在
     */
    @Override
    public boolean exists(String storagePath) {
        return FileUtil.exist(this.resolveFile(storagePath));
    }

    /**
     * 解析本地文件路径
     *
     * @param storagePath 存储路径
     * @return 本地文件
     */
    private File resolveFile(String storagePath) {
        if (StrUtil.isBlank(storagePath)) {
            throw new IllegalArgumentException("存储路径不能为空");
        }

        Path rootPath = Path.of(onlyOfficeProperties.getStorage().getRootPath()).toAbsolutePath().normalize();
        Path filePath = rootPath.resolve(storagePath).normalize();

        if (!filePath.startsWith(rootPath)) {
            throw new SecurityException("非法文件路径访问");
        }

        return filePath.toFile();
    }
}
```

本地存储需要重点防止路径穿越。上面的 `resolveFile` 方法通过 `normalize` 和 `startsWith(rootPath)` 校验最终路径必须位于文件存储根目录下，避免用户构造 `../../` 访问系统文件。

本地存储适用条件如下：

| 条件         | 说明                             |
| ------------ | -------------------------------- |
| 单机部署     | Spring Boot 只有一个实例。       |
| 文件量较小   | 文件规模可控，不需要分布式存储。 |
| 内部系统     | 主要用于内网办公或开发测试。     |
| 无高可用要求 | 服务器故障时允许人工恢复。       |

### 对象存储扩展

对象存储扩展适合生产环境、多实例部署和大文件管理。Spring Boot 多实例部署时，如果仍使用本地磁盘，OnlyOffice 回调可能落到任意实例，导致文件保存位置不一致；对象存储可以让所有实例读写同一份文件。

常见对象存储方案如下：

| 存储方案   | 说明                            |
| ---------- | ------------------------------- |
| MinIO      | 适合私有化部署，兼容 S3 协议。  |
| 阿里云 OSS | 适合阿里云环境。                |
| 腾讯云 COS | 适合腾讯云环境。                |
| AWS S3     | 适合 AWS 或兼容 S3 的对象存储。 |

对象存储扩展时，建议保持 `DocumentStorageService` 接口不变，只新增实现类。例如：

```text
src/main/java/io/github/atengk/onlyoffice/storage
├── DocumentStorageService.java
├── LocalDocumentStorageService.java
└── MinioDocumentStorageService.java
```

MinIO Maven 依赖示例：

```xml
<!-- MinIO：用于对象存储文件上传、下载和删除 -->
<dependency>
    <groupId>io.minio</groupId>
    <artifactId>minio</artifactId>
    <version>8.5.17</version>
</dependency>
```

配置示例：

```yaml
minio:
  # MinIO 服务地址
  endpoint: http://localhost:9000
  # 访问密钥
  access-key: minioadmin
  # 访问密钥对应的密文
  secret-key: minioadmin
  # 文档存储桶
  bucket: onlyoffice-documents
```

对象存储版核心实现思路如下：

```java
/**
 * 保存 OnlyOffice 回调文件到对象存储
 *
 * @param objectName 对象名称
 * @param tempFile 临时文件
 */
public void saveEditedFileToObjectStorage(String objectName, File tempFile) {
    // 1. 校验临时文件是否存在
    // 2. 上传到对象存储
    // 3. 校验上传结果
    // 4. 更新数据库中的文件地址、大小、版本号和 documentKey
}
```

对象存储接入后，文件下载接口通常有两种实现：

| 实现方式     | 说明                                                         |
| ------------ | ------------------------------------------------------------ |
| 后端代理下载 | OnlyOffice Docs 请求 Spring Boot，Spring Boot 从对象存储读取后返回。权限最安全，但占用后端带宽。 |
| 预签名 URL   | Spring Boot 生成对象存储短期签名地址，OnlyOffice Docs 直接下载。性能更好，但要控制过期时间。 |

对于私有文档，建议优先使用后端代理下载，统一经过业务权限校验。对于大文件或高并发场景，可以使用短期预签名 URL，但签名有效期不宜过长。

对象存储扩展注意事项如下：

| 注意事项                 | 说明                                                         |
| ------------------------ | ------------------------------------------------------------ |
| 存储路径不可暴露敏感信息 | 对象名称不要直接使用用户手机号、身份证、合同编号等敏感数据。 |
| 使用私有桶               | 文档类文件不建议使用公共读桶。                               |
| 控制签名有效期           | 预签名地址建议几分钟内过期。                                 |
| 保留版本记录             | 保存新文件时不要直接覆盖历史版本。                           |
| 增加重试机制             | 网络抖动时，上传和下载需要有重试或失败补偿。                 |

### 文档版本管理

文档版本管理用于解决在线编辑后的文件覆盖、历史追踪和缓存更新问题。OnlyOffice 使用 `document.key` 标识文档版本，如果文档内容发生变化但仍使用旧 key，可能会命中缓存，导致再次打开时看到旧内容；因此保存成功后应更新版本号，并生成新的 `documentKey`。OnlyOffice 文档配置中，`key` 是文档标识，回调中的 `key` 也是编辑中文档的标识。([ONLYOFFICE API](https://api.onlyoffice.com/docs/docs-api/usage-api/callback-handler/?utm_source=chatgpt.com))

推荐版本规则如下：

| 字段           | 示例                   | 说明                      |
| -------------- | ---------------------- | ------------------------- |
| `document_id`  | `1001`                 | 文档主 ID，多个版本共享。 |
| `version`      | `1`、`2`、`3`          | 文档版本号。              |
| `document_key` | `doc-1001-v3`          | OnlyOffice 文档 key。     |
| `storage_path` | `2026/05/1001/v3.docx` | 当前版本文件路径。        |
| `file_size`    | `102400`               | 当前版本文件大小。        |
| `latest`       | `true`                 | 是否最新版本。            |

推荐数据表结构如下：

```sql
CREATE TABLE document_file (
    id BIGINT PRIMARY KEY,
    document_name VARCHAR(255) NOT NULL COMMENT '文档名称',
    current_version INT NOT NULL DEFAULT 1 COMMENT '当前版本号',
    current_document_key VARCHAR(128) NOT NULL COMMENT '当前OnlyOffice文档Key',
    current_storage_path VARCHAR(512) NOT NULL COMMENT '当前版本存储路径',
    file_type VARCHAR(32) NOT NULL COMMENT '文件类型',
    file_size BIGINT NOT NULL DEFAULT 0 COMMENT '文件大小',
    status VARCHAR(32) NOT NULL DEFAULT 'NORMAL' COMMENT '文档状态',
    created_by VARCHAR(64) NOT NULL COMMENT '创建人',
    created_at DATETIME NOT NULL COMMENT '创建时间',
    updated_by VARCHAR(64) DEFAULT NULL COMMENT '更新人',
    updated_at DATETIME DEFAULT NULL COMMENT '更新时间'
) COMMENT '文档主表';

CREATE TABLE document_file_version (
    id BIGINT PRIMARY KEY,
    document_id BIGINT NOT NULL COMMENT '文档ID',
    version INT NOT NULL COMMENT '版本号',
    document_key VARCHAR(128) NOT NULL COMMENT 'OnlyOffice文档Key',
    storage_path VARCHAR(512) NOT NULL COMMENT '文件存储路径',
    file_name VARCHAR(255) NOT NULL COMMENT '文件名称',
    file_type VARCHAR(32) NOT NULL COMMENT '文件类型',
    file_size BIGINT NOT NULL DEFAULT 0 COMMENT '文件大小',
    save_type VARCHAR(32) NOT NULL COMMENT '保存类型：AUTO、FORCE、MANUAL',
    created_by VARCHAR(64) DEFAULT NULL COMMENT '创建人',
    created_at DATETIME NOT NULL COMMENT '创建时间',
    remark VARCHAR(512) DEFAULT NULL COMMENT '备注'
) COMMENT '文档版本表';
```

保存新版本的推荐流程如下：

```text
OnlyOffice 回调 status=2
  ↓
校验 documentId 和 key
  ↓
下载编辑后的文件到临时目录
  ↓
生成新版本号 version + 1
  ↓
生成新 documentKey，例如 doc-1001-v2
  ↓
生成新 storagePath，例如 2026/05/1001/v2.docx
  ↓
保存新文件到本地或对象存储
  ↓
插入 document_file_version 版本记录
  ↓
更新 document_file 当前版本信息
  ↓
返回 {"error":0}
```

版本 key 生成建议如下：

```java
/**
 * 生成 OnlyOffice 文档 Key
 *
 * @param documentId 文档ID
 * @param version 版本号
 * @return 文档Key
 */
public String buildDocumentKey(Long documentId, Integer version) {
    return StrUtil.format("doc-{}-v{}", documentId, version);
}
```

存储路径生成建议如下：

```java
/**
 * 生成版本文件存储路径
 *
 * @param documentId 文档ID
 * @param version 版本号
 * @param fileType 文件类型
 * @return 存储路径
 */
public String buildVersionStoragePath(Long documentId, Integer version, String fileType) {
    return StrUtil.format("{}/{}/{}/v{}.{}",
            DateUtil.format(DateUtil.date(), "yyyy"),
            DateUtil.format(DateUtil.date(), "MM"),
            documentId,
            version,
            fileType);
}
```

版本管理需要注意以下问题：

| 问题     | 建议                                                 |
| -------- | ---------------------------------------------------- |
| 并发保存 | 对同一文档保存时使用数据库乐观锁或分布式锁。         |
| 版本回滚 | 回滚时不要删除历史版本，只将当前版本指向历史版本。   |
| 强制保存 | `status=6` 可以保存为临时版本或自动保存版本。        |
| 缓存刷新 | 新版本保存成功后必须生成新的 `documentKey`。         |
| 历史清理 | 可按业务策略保留最近 N 个版本或指定天数内版本。      |
| 审计日志 | 记录保存用户、保存时间、来源状态、文件大小和版本号。 |

最小可用版本策略可以先这样设计：`status=2` 生成正式版本，`status=6` 生成自动保存版本或直接忽略，等业务需要“手动保存按钮”或“断线恢复”时再完善强制保存策略。



## 接口设计

本章节定义 Spring Boot 与前端、OnlyOffice Docs 之间交互的核心接口。接口设计需要围绕四类能力展开：前端获取编辑器配置、OnlyOffice Docs 下载源文件、OnlyOffice Docs 回调保存结果、前端查询文档状态。OnlyOffice 的前端编辑器通过 `DocsAPI.DocEditor` 创建实例，构造参数为页面容器 ID 和配置对象；配置对象中通常包含 `document`、`documentType`、`editorConfig` 等核心参数。([ONLYOFFICE API](https://api.onlyoffice.com/docs/docs-api/usage-api/doceditor/?utm_source=chatgpt.com))

### 获取编辑器配置接口

获取编辑器配置接口由前端调用，用于打开文档预览或编辑页面。后端根据文档 ID、打开模式、当前登录用户和业务权限生成 OnlyOffice 初始化配置。

接口定义如下：

| 项目         | 说明                                  |
| ------------ | ------------------------------------- |
| 接口地址     | `/api/onlyoffice/config/{documentId}` |
| 请求方式     | `GET`                                 |
| 调用方       | 前端页面                              |
| 处理方       | Spring Boot                           |
| 作用         | 获取 OnlyOffice 编辑器初始化配置      |
| 是否鉴权     | 是                                    |
| 是否需要登录 | 是                                    |

请求参数如下：

| 参数         | 位置  | 类型   | 必填 | 说明                                                      |
| ------------ | ----- | ------ | ---- | --------------------------------------------------------- |
| `documentId` | Path  | Long   | 是   | 文档 ID。                                                 |
| `mode`       | Query | String | 否   | 打开模式，`view` 表示预览，`edit` 表示编辑，默认 `view`。 |

请求示例：

```bash
# 获取预览模式配置
curl "http://localhost:8080/api/onlyoffice/config/1001?mode=view"

# 获取编辑模式配置
curl "http://localhost:8080/api/onlyoffice/config/1001?mode=edit"
```

响应示例：

```json
{
  "apiJsUrl": "http://localhost:8088/web-apps/apps/api/documents/api.js",
  "config": {
    "documentType": "word",
    "type": "desktop",
    "height": "100%",
    "width": "100%",
    "document": {
      "title": "1001.docx",
      "url": "http://host.docker.internal:8080/api/onlyoffice/file/download/1001",
      "fileType": "docx",
      "key": "doc-1001-v1",
      "permissions": {
        "edit": true,
        "download": true,
        "print": true,
        "comment": true,
        "review": false
      }
    },
    "editorConfig": {
      "mode": "edit",
      "lang": "zh",
      "callbackUrl": "http://host.docker.internal:8080/api/onlyoffice/callback/1001",
      "user": {
        "id": "10001",
        "name": "演示用户"
      }
    },
    "token": "后端生成的JWT_TOKEN"
  }
}
```

字段说明如下：

| 字段                              | 说明                                                   |
| --------------------------------- | ------------------------------------------------------ |
| `apiJsUrl`                        | OnlyOffice Docs 的 `api.js` 地址，前端动态加载该脚本。 |
| `config.documentType`             | 文档类型，常见值为 `word`、`cell`、`slide`。           |
| `config.document.url`             | OnlyOffice Docs 下载原始文件的地址。                   |
| `config.document.key`             | 文档唯一标识，建议包含文档 ID 和版本号。               |
| `config.editorConfig.callbackUrl` | OnlyOffice Docs 保存回调地址。                         |
| `config.token`                    | 启用 JWT 后由后端生成的签名 Token。                    |

后端处理规则如下：

| 规则             | 说明                                                     |
| ---------------- | -------------------------------------------------------- |
| 校验登录状态     | 未登录用户不能获取配置。                                 |
| 校验查看权限     | 无查看权限时直接返回 403 或业务无权限错误。              |
| 校验编辑权限     | 请求 `mode=edit` 时，后端仍需重新判断是否允许编辑。      |
| 生成文件下载地址 | `document.url` 必须是 OnlyOffice Docs 可访问的绝对地址。 |
| 生成回调地址     | `callbackUrl` 必须是 OnlyOffice Docs 可访问的绝对地址。  |
| 生成 JWT Token   | OnlyOffice JWT 开启时，需要对配置进行签名。              |

### 文件下载接口

文件下载接口由 OnlyOffice Docs 调用，用于读取原始文档。该接口不能返回业务 JSON，必须返回文档二进制内容。OnlyOffice 官方最小配置示例中，`document.url` 表示源文档地址，`document.key` 表示文档标识，`editorConfig.callbackUrl` 表示回调地址。([ONLYOFFICE API](https://api.onlyoffice.com/docs/docs-api/usage-api/doceditor/?utm_source=chatgpt.com))

接口定义如下：

| 项目     | 说明                                         |
| -------- | -------------------------------------------- |
| 接口地址 | `/api/onlyoffice/file/download/{documentId}` |
| 请求方式 | `GET`                                        |
| 调用方   | OnlyOffice Docs                              |
| 处理方   | Spring Boot                                  |
| 作用     | 下载原始文档文件                             |
| 是否鉴权 | 建议鉴权                                     |
| 响应类型 | 文件二进制流                                 |

请求参数如下：

| 参数         | 位置 | 类型 | 必填 | 说明      |
| ------------ | ---- | ---- | ---- | --------- |
| `documentId` | Path | Long | 是   | 文档 ID。 |

请求示例：

```bash
# 查看响应头，确认文件接口是否可访问
curl -I "http://localhost:8080/api/onlyoffice/file/download/1001"

# 下载文件到本地验证内容
curl -o 1001.docx "http://localhost:8080/api/onlyoffice/file/download/1001"
```

响应头建议如下：

```text
HTTP/1.1 200 OK
Content-Type: application/octet-stream
Content-Disposition: inline; filename*=UTF-8''1001.docx
Content-Length: 102400
```

后端处理规则如下：

| 规则             | 说明                                           |
| ---------------- | ---------------------------------------------- |
| 校验文档是否存在 | 文档 ID 不存在时返回 404 或业务错误。          |
| 校验文件是否存在 | 存储路径对应文件不存在时返回 404。             |
| 防止路径穿越     | 本地存储时必须校验文件路径不能跳出存储根目录。 |
| 返回二进制文件   | 不能返回统一响应对象。                         |
| 设置文件名       | 使用 `Content-Disposition` 返回正确文件名。    |
| 设置缓存策略     | 开发阶段可不缓存，生产可结合文档版本设置缓存。 |

文件下载接口常见响应码如下：

| 状态码 | 场景                         |
| ------ | ---------------------------- |
| `200`  | 文件正常返回。               |
| `403`  | 当前请求无权访问该文件。     |
| `404`  | 文档记录或文件不存在。       |
| `500`  | 文件读取失败或存储服务异常。 |

### OnlyOffice 回调接口

OnlyOffice 回调接口由 OnlyOffice Docs 调用，用于通知 Spring Boot 文档编辑状态和保存结果。OnlyOffice 官方回调文档说明，文档编辑服务会向 `callbackUrl` 发送 POST 请求；`status=2` 表示文档已准备好保存，`url` 字段表示编辑后文件的下载地址；文档存储服务必须返回 `{"error":0}`，否则编辑器会显示错误。([ONLYOFFICE API](https://api.onlyoffice.com/docs/docs-api/usage-api/callback-handler/?utm_source=chatgpt.com))

接口定义如下：

| 项目     | 说明                                    |
| -------- | --------------------------------------- |
| 接口地址 | `/api/onlyoffice/callback/{documentId}` |
| 请求方式 | `POST`                                  |
| 调用方   | OnlyOffice Docs                         |
| 处理方   | Spring Boot                             |
| 作用     | 接收文档状态回调并保存编辑结果          |
| 是否鉴权 | 是                                      |
| 响应类型 | JSON                                    |

请求参数如下：

| 参数            | 位置 | 类型    | 必填 | 说明                                 |
| --------------- | ---- | ------- | ---- | ------------------------------------ |
| `documentId`    | Path | Long    | 是   | 文档 ID。                            |
| `key`           | Body | String  | 是   | 文档唯一标识。                       |
| `status`        | Body | Integer | 是   | 文档状态。                           |
| `url`           | Body | String  | 否   | 编辑后文件下载地址，保存场景下存在。 |
| `filetype`      | Body | String  | 否   | 文件类型。                           |
| `users`         | Body | Array   | 否   | 当前编辑用户或最后编辑用户。         |
| `changesurl`    | Body | String  | 否   | 变更历史文件地址。                   |
| `history`       | Body | Object  | 否   | 历史版本信息。                       |
| `forcesavetype` | Body | Integer | 否   | 强制保存类型。                       |
| `token`         | Body | String  | 否   | JWT Token。                          |

回调请求示例：

```json
{
  "key": "doc-1001-v1",
  "status": 2,
  "url": "http://onlyoffice-documentserver/cache/files/edited-document.docx",
  "filetype": "docx",
  "users": [
    "10001"
  ],
  "history": {
    "serverVersion": 1,
    "changes": {}
  }
}
```

成功响应示例：

```json
{
  "error": 0
}
```

失败响应示例：

```json
{
  "error": 1
}
```

回调状态处理规则如下：

| status | 含义             | 处理方式                           |
| ------ | ---------------- | ---------------------------------- |
| `1`    | 文档正在编辑     | 记录日志，不保存文件。             |
| `2`    | 文档准备好保存   | 下载 `url` 文件，保存正式版本。    |
| `3`    | 文档保存发生错误 | 记录异常，可返回失败。             |
| `4`    | 文档关闭且无修改 | 记录日志，不保存文件。             |
| `6`    | 强制保存成功     | 按业务需要保存自动版本或临时版本。 |
| `7`    | 强制保存失败     | 记录异常，不保存文件。             |

回调处理流程如下：

```text
接收回调请求
  ↓
校验 documentId 是否存在
  ↓
校验 key 是否匹配当前文档版本
  ↓
校验 status 是否合法
  ↓
判断 status 是否需要保存
  ↓
status=2 或 status=6 时校验 url 是否为空
  ↓
下载 url 对应的新文件到临时目录
  ↓
保存到正式存储目录或对象存储
  ↓
更新文档版本、大小、更新时间和 documentKey
  ↓
返回 {"error":0}
```

### 文档状态查询接口

文档状态查询接口由前端调用，用于展示文档当前状态，例如是否可预览、是否可编辑、当前版本、是否锁定、最后保存时间等。该接口不是 OnlyOffice 必须接口，但在业务系统中建议提供，便于前端在进入编辑器前展示状态和控制按钮。

接口定义如下：

| 项目     | 说明                                  |
| -------- | ------------------------------------- |
| 接口地址 | `/api/onlyoffice/status/{documentId}` |
| 请求方式 | `GET`                                 |
| 调用方   | 前端页面                              |
| 处理方   | Spring Boot                           |
| 作用     | 查询文档状态、权限和版本信息          |
| 是否鉴权 | 是                                    |
| 响应类型 | JSON                                  |

响应示例：

```json
{
  "documentId": 1001,
  "fileName": "1001.docx",
  "fileType": "docx",
  "documentType": "word",
  "version": 2,
  "documentKey": "doc-1001-v2",
  "status": "NORMAL",
  "locked": false,
  "allowView": true,
  "allowEdit": true,
  "allowDownload": true,
  "lastSavedAt": "2026-05-09 14:30:00",
  "lastSavedBy": "10001"
}
```

字段说明如下：

| 字段            | 说明                                            |
| --------------- | ----------------------------------------------- |
| `documentId`    | 文档 ID。                                       |
| `fileName`      | 当前文档文件名。                                |
| `fileType`      | 文件扩展名。                                    |
| `documentType`  | OnlyOffice 文档类型。                           |
| `version`       | 当前版本号。                                    |
| `documentKey`   | 当前版本对应的 OnlyOffice 文档 Key。            |
| `status`        | 业务状态，例如 `NORMAL`、`ARCHIVED`、`LOCKED`。 |
| `locked`        | 是否被锁定。                                    |
| `allowView`     | 当前用户是否允许查看。                          |
| `allowEdit`     | 当前用户是否允许编辑。                          |
| `allowDownload` | 当前用户是否允许下载。                          |
| `lastSavedAt`   | 最后保存时间。                                  |
| `lastSavedBy`   | 最后保存用户。                                  |

文件位置：`src/main/java/io/github/atengk/onlyoffice/vo/DocumentStatusResponse.java`

下面的类用于返回文档状态查询结果。

```java
package io.github.atengk.onlyoffice.vo;

import lombok.Builder;
import lombok.Data;

/**
 * 文档状态响应
 *
 * @author Ateng
 * @since 2026-05-09
 */
@Data
@Builder
public class DocumentStatusResponse {

    /**
     * 文档ID
     */
    private Long documentId;

    /**
     * 文件名称
     */
    private String fileName;

    /**
     * 文件类型
     */
    private String fileType;

    /**
     * OnlyOffice 文档类型
     */
    private String documentType;

    /**
     * 当前版本号
     */
    private Integer version;

    /**
     * 当前文档Key
     */
    private String documentKey;

    /**
     * 文档状态
     */
    private String status;

    /**
     * 是否锁定
     */
    private Boolean locked;

    /**
     * 是否允许查看
     */
    private Boolean allowView;

    /**
     * 是否允许编辑
     */
    private Boolean allowEdit;

    /**
     * 是否允许下载
     */
    private Boolean allowDownload;

    /**
     * 最后保存时间
     */
    private String lastSavedAt;

    /**
     * 最后保存用户
     */
    private String lastSavedBy;
}
```

接口实现时建议从数据库读取文档主表和版本表，再结合当前登录用户权限生成状态响应。前端根据 `allowEdit` 控制编辑按钮显示，但后端仍然必须在获取编辑器配置接口中再次校验。

## 联调与验证

本章节用于验证 Spring Boot、OnlyOffice Docs、前端页面和文件存储之间是否完成闭环。OnlyOffice 集成问题通常出现在网络访问、文件地址、回调地址、JWT 密钥和文件保存路径上，因此联调应按链路顺序逐项验证。

### OnlyOffice 服务连通性验证

OnlyOffice 服务连通性验证用于确认 OnlyOffice Docs 是否正常启动、前端是否能加载编辑器脚本、OnlyOffice 容器是否能访问 Spring Boot 服务。

验证 OnlyOffice 首页：

```bash
# 宿主机访问 OnlyOffice Docs
curl -I http://localhost:8088
```

预期结果：

```text
HTTP/1.1 200 OK
```

验证 `api.js` 是否可访问。OnlyOffice 的 `DocsAPI` 全局对象由 `web-apps/apps/api/documents/api.js` 脚本提供，前端加载该脚本后才能创建 `DocsAPI.DocEditor`。([ONLYOFFICE API](https://api.onlyoffice.com/docs/docs-api/usage-api/doceditor/?utm_source=chatgpt.com))

```bash
# 验证编辑器 API 脚本是否可访问
curl -I http://localhost:8088/web-apps/apps/api/documents/api.js
```

预期结果：

```text
HTTP/1.1 200 OK
Content-Type: application/javascript
```

从 OnlyOffice 容器内部验证 Spring Boot 文件下载接口：

```bash
# 进入 OnlyOffice Docs 容器
docker exec -it onlyoffice-documentserver bash

# 在容器内部验证 Spring Boot 下载接口
curl -I http://host.docker.internal:8080/api/onlyoffice/file/download/1001

# 在容器内部验证 Spring Boot 回调接口路径是否可达
curl -I http://host.docker.internal:8080/api/onlyoffice/callback/1001
```

验证结果说明如下：

| 结果               | 说明                                                     |
| ------------------ | -------------------------------------------------------- |
| 下载接口返回 `200` | OnlyOffice Docs 可以访问 Spring Boot 文件接口。          |
| 下载接口返回 `403` | 权限校验失败，需要检查下载授权策略。                     |
| 下载接口返回 `404` | 文档 ID 或文件不存在。                                   |
| 连接超时           | 容器无法访问 Spring Boot 地址，需要检查 `app-base-url`。 |
| 回调接口返回 `405` | 正常现象，说明路径存在但不支持 GET；回调接口使用 POST。  |

### 文档预览验证

文档预览验证用于确认前端可以正常加载 OnlyOffice 编辑器，OnlyOffice Docs 可以下载源文件，并以只读模式打开文档。

验证步骤如下：

| 步骤 | 操作                           | 预期结果                                       |
| ---- | ------------------------------ | ---------------------------------------------- |
| 1    | 准备 `storage/files/1001.docx` | 文件存在且 Spring Boot 有读取权限。            |
| 2    | 启动 Spring Boot               | `http://localhost:8080` 可访问。               |
| 3    | 启动 OnlyOffice Docs           | `http://localhost:8088` 可访问。               |
| 4    | 请求配置接口                   | 返回 `mode=view` 和 `permissions.edit=false`。 |
| 5    | 打开前端页面                   | 编辑器正常加载。                               |
| 6    | 查看文档内容                   | 文档内容可见，不能编辑。                       |

请求预览配置：

```bash
curl "http://localhost:8080/api/onlyoffice/config/1001?mode=view"
```

需要重点检查响应中的以下字段：

```json
{
  "config": {
    "document": {
      "url": "http://host.docker.internal:8080/api/onlyoffice/file/download/1001",
      "key": "doc-1001-v1",
      "permissions": {
        "edit": false
      }
    },
    "editorConfig": {
      "mode": "view",
      "callbackUrl": "http://host.docker.internal:8080/api/onlyoffice/callback/1001"
    }
  }
}
```

预览验证通过标准如下：

| 检查项            | 通过标准                                             |
| ----------------- | ---------------------------------------------------- |
| `api.js` 加载成功 | 浏览器 Network 中 `api.js` 返回 `200`。              |
| 配置接口成功      | `/api/onlyoffice/config/1001?mode=view` 返回 `200`。 |
| 文件下载成功      | OnlyOffice Docs 请求 `document.url` 返回文件流。     |
| 页面无明显报错    | 浏览器控制台没有 `DocsAPI` 未定义或脚本加载失败。    |
| 文档不可编辑      | 页面只能查看，不能输入或修改内容。                   |

### 在线编辑验证

在线编辑验证用于确认用户可以在编辑模式下修改文档，且编辑器配置中的权限和模式正确。OnlyOffice 的 `DocEditor` 使用配置对象创建实例，`editorConfig.mode` 和 `document.permissions` 会影响文档是否以编辑能力打开。([ONLYOFFICE API](https://api.onlyoffice.com/docs/docs-api/usage-api/doceditor/?utm_source=chatgpt.com))

验证步骤如下：

| 步骤 | 操作                  | 预期结果                                      |
| ---- | --------------------- | --------------------------------------------- |
| 1    | 请求编辑模式配置      | 返回 `mode=edit` 和 `permissions.edit=true`。 |
| 2    | 打开前端编辑页面      | 编辑器进入编辑模式。                          |
| 3    | 修改文档内容          | 页面允许输入和修改。                          |
| 4    | 关闭编辑器或离开页面  | OnlyOffice Docs 后续触发回调。                |
| 5    | 查看 Spring Boot 日志 | 能看到回调状态日志。                          |

请求编辑配置：

```bash
curl "http://localhost:8080/api/onlyoffice/config/1001?mode=edit"
```

需要重点检查响应中的以下字段：

```json
{
  "config": {
    "document": {
      "permissions": {
        "edit": true,
        "comment": true,
        "download": true
      }
    },
    "editorConfig": {
      "mode": "edit"
    }
  }
}
```

在线编辑验证通过标准如下：

| 检查项         | 通过标准                                |
| -------------- | --------------------------------------- |
| 用户有编辑权限 | 后端返回编辑模式配置。                  |
| 页面可编辑     | 文档中可以输入、删除、修改内容。        |
| 回调地址存在   | 配置中包含 `editorConfig.callbackUrl`。 |
| 文档 Key 正确  | `document.key` 与当前版本一致。         |
| JWT 正确       | 启用 JWT 时编辑器无 Token 校验错误。    |

编辑验证时应注意，只有前端页面能编辑并不代表保存成功。保存是否成功必须通过回调日志、文件内容变化和版本记录确认。

### 回调保存验证

回调保存验证用于确认 OnlyOffice Docs 能够将编辑后的文件通知 Spring Boot，并由 Spring Boot 下载新文件保存。OnlyOffice 回调中，`status=2` 表示文档准备好保存；回调响应必须返回 `{"error":0}`。([ONLYOFFICE API](https://api.onlyoffice.com/docs/docs-api/usage-api/callback-handler/?utm_source=chatgpt.com))

验证步骤如下：

| 步骤 | 操作                  | 预期结果                                   |
| ---- | --------------------- | ------------------------------------------ |
| 1    | 编辑文档内容          | 文档内容发生变化。                         |
| 2    | 关闭编辑器页面        | OnlyOffice Docs 在合适时机触发回调。       |
| 3    | 查看 Spring Boot 日志 | 出现 `status=2` 或 `status=6` 的回调日志。 |
| 4    | 检查保存目录          | 原文件被覆盖或生成新版本文件。             |
| 5    | 重新打开文档          | 能看到最新内容。                           |
| 6    | 检查响应              | 回调接口返回 `{"error":0}`。               |

本地可先模拟无修改关闭场景：

```bash
curl -X POST "http://localhost:8080/api/onlyoffice/callback/1001" \
  -H "Content-Type: application/json" \
  -d '{
    "key": "doc-1001-v1",
    "status": 4
  }'
```

预期响应：

```json
{
  "error": 0
}
```

真实保存场景需要由 OnlyOffice Docs 生成 `url`，不建议手工伪造。回调请求中的 `url` 是编辑后文件地址，Spring Boot 需要根据该地址下载新文件并保存。([ONLYOFFICE API](https://api.onlyoffice.com/docs/docs-api/usage-api/callback-handler/?utm_source=chatgpt.com))

回调保存验证通过标准如下：

| 检查项         | 通过标准                                   |
| -------------- | ------------------------------------------ |
| 回调接口可访问 | OnlyOffice Docs 能 POST 到 `callbackUrl`。 |
| 回调状态正确   | 修改并关闭后出现 `status=2`。              |
| 文件下载成功   | Spring Boot 能下载回调中的 `url`。         |
| 文件保存成功   | 本地文件或对象存储文件更新。               |
| 返回值正确     | 回调响应为 `{"error":0}`。                 |
| 重新打开正常   | 再次打开能看到修改后的内容。               |

## 常见问题处理

本章节整理 OnlyOffice 集成中最常见的问题，包括文档无法加载、回调地址无法访问、JWT 校验失败和文件保存失败。排查时建议优先按链路定位：前端是否加载 `api.js`，OnlyOffice Docs 是否能访问 `document.url`，Spring Boot 是否能接收 `callbackUrl`，JWT 密钥是否一致，保存路径是否可写。

### 文档无法加载

文档无法加载通常表现为编辑器页面打开后提示下载失败、文档加载失败、空白页或长时间加载。该问题大多与 `api.js` 地址、`document.url` 地址、文件类型、文件权限或 Docker 网络有关。

常见原因如下：

| 原因                    | 说明                                              | 处理方式                                      |
| ----------------------- | ------------------------------------------------- | --------------------------------------------- |
| `api.js` 无法加载       | 前端无法访问 OnlyOffice Docs 服务                 | 检查 `document-server-url` 和浏览器 Network。 |
| `document.url` 无法访问 | OnlyOffice Docs 容器访问不到 Spring Boot 下载接口 | 在容器内使用 `curl -I document.url` 验证。    |
| 文件不存在              | 本地路径或对象存储路径错误                        | 检查文件路径和数据库记录。                    |
| 文件类型不支持          | 扩展名映射错误或文件损坏                          | 检查 `fileType` 和实际文件内容。              |
| 返回 JSON 而不是文件流  | 下载接口被统一响应包装                            | 下载接口必须直接返回二进制文件。              |
| 文档 Key 不正确         | 版本变化后仍使用旧 Key                            | 保存新版本后更新 `document.key`。             |

排查命令如下：

```bash
# 检查 OnlyOffice api.js
curl -I http://localhost:8088/web-apps/apps/api/documents/api.js

# 检查 Spring Boot 下载接口
curl -I http://localhost:8080/api/onlyoffice/file/download/1001

# 从 OnlyOffice 容器内部检查下载接口
docker exec -it onlyoffice-documentserver bash
curl -I http://host.docker.internal:8080/api/onlyoffice/file/download/1001
```

重点检查项如下：

| 检查项     | 正确结果                                 |
| ---------- | ---------------------------------------- |
| `api.js`   | 返回 `200`，Content-Type 为 JavaScript。 |
| 下载接口   | 返回 `200`，Content-Type 为文件流。      |
| 文件名     | `Content-Disposition` 中包含正确文件名。 |
| 文件大小   | `Content-Length` 大于 0。                |
| 容器内访问 | OnlyOffice 容器内部可以访问下载地址。    |

### 回调地址无法访问

回调地址无法访问通常表现为文档可以打开和编辑，但关闭后无法保存，或者 Spring Boot 没有任何回调日志。OnlyOffice Docs 会根据 `editorConfig.callbackUrl` 向文档存储服务发送 POST 请求，因此该地址必须从 OnlyOffice Docs 服务所在环境可访问。([ONLYOFFICE API](https://api.onlyoffice.com/docs/docs-api/usage-api/callback-handler/?utm_source=chatgpt.com))

常见原因如下：

| 原因                             | 说明                                       | 处理方式                                            |
| -------------------------------- | ------------------------------------------ | --------------------------------------------------- |
| `callbackUrl` 使用了 `localhost` | 容器内的 `localhost` 指向容器自身          | 改为 `host.docker.internal`、宿主机 IP 或内部域名。 |
| 接口只允许 GET                   | 回调接口必须支持 POST                      | 使用 `@PostMapping`。                               |
| 防火墙拦截                       | OnlyOffice 服务器无法访问 Spring Boot 端口 | 放通端口或使用内网地址。                            |
| 网关未转发                       | Nginx 没有转发回调路径                     | 增加 `/api/onlyoffice/callback` 转发规则。          |
| HTTPS 证书问题                   | OnlyOffice 访问 HTTPS 地址失败             | 配置可信证书或统一使用内网 HTTP。                   |
| 权限拦截                         | 登录拦截器拦截了 OnlyOffice 回调           | 对回调路径使用专门鉴权策略。                        |

排查命令如下：

```bash
# 从宿主机验证回调接口是否存在
curl -I http://localhost:8080/api/onlyoffice/callback/1001

# 从 OnlyOffice 容器内部验证回调地址是否可达
docker exec -it onlyoffice-documentserver bash
curl -I http://host.docker.internal:8080/api/onlyoffice/callback/1001
```

如果返回 `405 Method Not Allowed`，通常说明路径存在但 GET 方法不支持，这对回调接口是可以接受的，因为真实回调使用 POST。可以进一步使用 POST 模拟：

```bash
curl -X POST "http://localhost:8080/api/onlyoffice/callback/1001" \
  -H "Content-Type: application/json" \
  -d '{
    "key": "doc-1001-v1",
    "status": 4
  }'
```

预期返回：

```json
{
  "error": 0
}
```

### JWT 校验失败

JWT 校验失败通常表现为编辑器无法加载文档、OnlyOffice 报 Token 错误、回调被后端拒绝，或者服务重启后突然无法打开文档。OnlyOffice 官方文档说明，从 7.2 版本开始 JWT 默认启用，并且如果没有显式配置密钥，服务可能自动生成密钥；因此 Spring Boot 和 OnlyOffice Docs 必须使用同一个固定密钥。([ONLYOFFICE API](https://api.onlyoffice.com/docs/docs-api/usage-api/callback-handler/?utm_source=chatgpt.com))

常见原因如下：

| 原因                  | 说明                                                         | 处理方式                                |
| --------------------- | ------------------------------------------------------------ | --------------------------------------- |
| 密钥不一致            | Docker 中 `JWT_SECRET` 与 Spring Boot 中的 `onlyoffice.jwt.secret` 不一致 | 两侧配置为相同值。                      |
| 密钥长度不足          | HS256 签名密钥过短                                           | 使用不少于 32 字节的密钥。              |
| 未重启服务            | 修改密钥后 OnlyOffice 或 Spring Boot 未重启                  | 修改后重启两端服务。                    |
| Token 生成内容不一致  | 后端签名的 Payload 与实际配置不一致                          | 先生成完整 config，再基于 config 签名。 |
| 前端篡改配置          | 前端修改了后端签名后的配置内容                               | 前端只透传配置，不修改核心字段。        |
| 回调 Token 位置不一致 | 后端只从请求体取 Token，但实际可能来自 Header                | 兼容请求体和 Header 读取策略。          |

Docker 配置检查：

```yaml
services:
  onlyoffice-documentserver:
    image: onlyoffice/documentserver:latest
    environment:
      # 开启 JWT
      JWT_ENABLED: "true"
      # 与 Spring Boot 配置保持一致
      JWT_SECRET: "onlyoffice_jwt_secret_please_change_32"
```

Spring Boot 配置检查：

```yaml
onlyoffice:
  jwt:
    # 与 Docker 中 JWT_ENABLED 保持一致
    enabled: true
    # 与 Docker 中 JWT_SECRET 保持一致
    secret: onlyoffice_jwt_secret_please_change_32
```

修改密钥后执行：

```bash
# 重启 OnlyOffice Docs
docker compose restart onlyoffice-documentserver

# 重启 Spring Boot 服务
java -jar target/springboot-onlyoffice.jar
```

JWT 排查顺序如下：

| 顺序 | 检查项                              |
| ---- | ----------------------------------- |
| 1    | 检查 OnlyOffice Docs 是否启用 JWT。 |
| 2    | 检查 Spring Boot 是否启用 JWT。     |
| 3    | 检查两侧密钥是否完全一致。          |
| 4    | 检查密钥长度是否满足签名算法要求。  |
| 5    | 检查后端是否先生成完整配置再签名。  |
| 6    | 检查前端是否修改了后端返回的配置。  |

### 文件保存失败

文件保存失败通常表现为编辑完成后关闭页面，但原文件没有变化，或者编辑器提示保存错误。OnlyOffice 回调中只有在特定状态下才需要保存文件，例如 `status=2` 表示文档准备好保存，`status=6` 表示强制保存成功；保存时需要使用回调中的 `url` 下载编辑后文件，并在成功后返回 `{"error":0}`。([ONLYOFFICE API](https://api.onlyoffice.com/docs/docs-api/usage-api/callback-handler/?utm_source=chatgpt.com))

常见原因如下：

| 原因                | 说明                                               | 处理方式                                     |
| ------------------- | -------------------------------------------------- | -------------------------------------------- |
| 没有收到 `status=2` | 用户未修改内容或回调地址不可达                     | 检查回调日志和 `callbackUrl`。               |
| `url` 为空          | 当前回调状态不需要保存或请求异常                   | 只在 `status=2`、`status=6` 时下载文件。     |
| 下载 `url` 失败     | Spring Boot 访问不到 OnlyOffice 生成的临时文件地址 | 检查 Spring Boot 到 OnlyOffice Docs 的网络。 |
| 本地目录无权限      | Spring Boot 对保存目录没有写权限                   | 修改目录权限或运行用户。                     |
| 文件被占用          | 目标文件正在被其他进程占用                         | 使用临时文件替换策略。                       |
| 版本未更新          | 文件保存了，但再次打开仍使用旧缓存                 | 保存成功后生成新的 `documentKey`。           |
| 回调返回错误        | 回调接口返回非 `{"error":0}`                       | 成功保存后必须返回 `error=0`。               |

排查 Spring Boot 日志时重点查找：

```text
收到OnlyOffice回调，文档ID：1001，状态：2
OnlyOffice文档保存成功，文档ID：1001
OnlyOffice文档保存失败，文档ID：1001
```

验证保存目录权限：

```bash
# 查看存储目录权限
ls -ld storage storage/files storage/temp

# 查看文件是否存在
ls -lh storage/files

# 给当前用户增加读写权限，按实际部署用户调整
chmod -R u+rw storage
```

验证 Spring Boot 是否能访问 OnlyOffice 回调文件地址：

```bash
# 将回调日志中的 url 复制出来验证
curl -I "http://onlyoffice-documentserver/cache/files/edited-document.docx"
```

文件保存失败排查顺序如下：

| 顺序 | 检查项                                       |
| ---- | -------------------------------------------- |
| 1    | Spring Boot 是否收到回调。                   |
| 2    | 回调 `status` 是否为 `2` 或 `6`。            |
| 3    | 回调 `url` 是否存在。                        |
| 4    | Spring Boot 是否能下载 `url`。               |
| 5    | 临时文件是否成功生成。                       |
| 6    | 正式存储路径是否有写权限。                   |
| 7    | 保存成功后是否更新文档版本和 `documentKey`。 |
| 8    | 回调响应是否为 `{"error":0}`。               |

生产环境建议将文件保存设计为“下载到临时文件、校验大小、替换正式文件、更新数据库版本、清理临时文件”的顺序，避免下载失败或半写入导致正式文件损坏。
