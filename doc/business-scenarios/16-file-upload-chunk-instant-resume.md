# 文件上传、分片上传、秒传、权限访问

本案例实现 Java 后端项目中常见的文件服务能力，覆盖普通上传、分片上传、秒传、断点续传、文件权限访问、临时下载链接生成等核心场景。

整体方案采用 Spring Boot 作为业务入口，MinIO 作为对象存储，MySQL 保存文件元数据，Redis 缓存分片上传状态。后端不直接暴露 MinIO 文件地址，而是通过业务接口完成权限校验后，再生成临时访问链接。

## 业务场景说明

在实际项目中，文件上传通常不仅是把文件保存到服务器目录，还需要处理文件去重、大文件上传失败重试、断点续传、访问权限控制、下载链接防盗用等问题。

本案例适合以下业务场景：

| 场景           | 说明                                       |
| -------------- | ------------------------------------------ |
| 工单附件       | 用户提交工单时上传图片、文档、压缩包等附件 |
| 后台素材管理   | 管理图片、视频、Excel、PDF 等业务素材      |
| 用户头像上传   | 上传小文件，并返回可访问的文件地址         |
| 大文件上传     | 上传视频、压缩包、大型报表等大文件         |
| 私有文件下载   | 文件需要登录并校验权限后才能访问           |
| 多用户文件复用 | 不同用户上传相同文件时，底层存储只保存一份 |

本案例将文件分为两个层次：

| 层次     | 说明                                                   |
| -------- | ------------------------------------------------------ |
| 物理文件 | MinIO 中真实保存的文件对象，同一个文件 Hash 只保存一份 |
| 业务文件 | 用户上传产生的文件记录，可以多个用户引用同一个物理文件 |

这样设计的好处是：既能避免重复存储，又能保留每个用户自己的上传记录和访问权限。

### 功能目标

本案例的目标是实现一个可直接集成到 Spring Boot 项目中的文件服务模块，重点实现核心业务能力，不扩展成完整网盘系统。

需要实现的功能目标如下：

| 目标           | 说明                                                         |
| -------------- | ------------------------------------------------------------ |
| 普通文件上传   | 支持小文件直接上传到后端，并转存到 MinIO                     |
| 文件元数据保存 | 保存文件名、文件大小、文件类型、文件 Hash、存储路径、上传人等信息 |
| 文件秒传       | 根据文件 Hash 判断文件是否已存在，存在则直接生成业务文件记录 |
| 分片上传       | 支持大文件按分片上传，降低单次上传失败成本                   |
| 断点续传       | 支持查询已上传分片，只上传缺失分片                           |
| 分片合并       | 所有分片上传完成后，后端合并为完整文件                       |
| 权限访问       | 文件默认私有，访问前必须经过后端权限校验                     |
| 临时下载链接   | 权限校验通过后，生成 MinIO 预签名下载地址                    |
| 文件复用       | 多个用户上传相同文件时，复用同一份物理文件                   |

本案例默认实现以下规则：

| 规则         | 说明                                         |
| ------------ | -------------------------------------------- |
| 文件唯一标识 | 使用 SHA-256 作为文件内容唯一标识            |
| 分片大小     | 默认每片 5MB，可通过配置调整                 |
| 分片标识     | 使用 `fileHash + chunkIndex` 标识单个分片    |
| 文件存储     | 完整文件和临时分片都存储到 MinIO             |
| 文件权限     | 示例中先实现“上传人可访问”和“公开文件可访问” |
| 下载方式     | 后端鉴权后生成短时间有效的 MinIO 预签名 URL  |

### 核心能力

本模块围绕上传、去重、合并、访问四个方向实现核心能力。

| 能力           | 说明                                                         |
| -------------- | ------------------------------------------------------------ |
| 普通上传       | 前端通过 `multipart/form-data` 上传文件，后端接收后计算 Hash，上传到 MinIO，并保存文件记录 |
| 秒传检测       | 前端先计算文件 Hash，后端根据 Hash 判断物理文件是否已存在    |
| 秒传保存       | 如果物理文件已存在，则不再上传文件内容，直接创建当前用户的业务文件记录 |
| 分片初始化     | 前端上传大文件前，先创建分片上传任务，后端返回任务编号和已上传分片 |
| 分片上传       | 前端按分片编号逐片上传，后端将分片保存到 MinIO 临时目录      |
| 已上传分片查询 | 前端可查询当前文件已经上传的分片，用于断点续传               |
| 分片合并       | 所有分片上传完成后，后端按分片顺序合并为完整文件             |
| 文件详情查询   | 根据文件 ID 查询文件名、大小、类型、上传时间、访问权限等信息 |
| 权限校验       | 下载或访问文件前，校验当前用户是否有权限访问                 |
| 临时链接生成   | 权限校验通过后，生成有效期较短的 MinIO 预签名 URL            |

核心流程如下：

```text
前端选择文件
    ↓
前端计算文件 SHA-256
    ↓
调用秒传检测接口
    ↓
文件已存在：直接秒传保存
    ↓
文件不存在：判断是否需要分片上传
    ↓
小文件：普通上传
    ↓
大文件：初始化分片任务，上传缺失分片，最后合并
    ↓
保存文件元数据
    ↓
访问文件时先校验权限
    ↓
生成临时下载链接
```

接口规划如下：

| 接口                               | 方法   | 说明             |
| ---------------------------------- | ------ | ---------------- |
| `/api/files/upload`                | `POST` | 普通文件上传     |
| `/api/files/instant-check`         | `POST` | 秒传检测         |
| `/api/files/instant-save`          | `POST` | 秒传保存         |
| `/api/files/chunk/init`            | `POST` | 初始化分片上传   |
| `/api/files/chunk/upload`          | `POST` | 上传单个分片     |
| `/api/files/chunk/uploaded`        | `GET`  | 查询已上传分片   |
| `/api/files/chunk/merge`           | `POST` | 合并分片         |
| `/api/files/{fileId}`              | `GET`  | 查询文件详情     |
| `/api/files/{fileId}/download-url` | `GET`  | 获取临时下载链接 |

### 技术选型

本案例采用 Spring Boot 3.x 技术栈实现，兼顾工程实用性、代码简洁度和后续扩展能力。

| 技术              | 用途                                       |
| ----------------- | ------------------------------------------ |
| JDK 17+           | Spring Boot 3.x 基础运行环境               |
| Spring Boot 3.x   | 项目基础框架                               |
| Spring Web        | 提供文件上传、下载、分片上传接口           |
| MyBatis-Plus      | 操作文件元数据表、物理文件表、分片任务表   |
| MySQL 8.x         | 持久化保存文件信息、上传任务、分片记录     |
| Redis             | 缓存分片上传状态，提高查询已上传分片的效率 |
| MinIO             | 保存完整文件和临时分片文件                 |
| Sa-Token          | 获取当前登录用户，做接口登录校验和权限扩展 |
| Hutool            | 文件 Hash、字符串、集合、日期等工具处理    |
| Lombok            | 简化实体类、DTO、VO、日志代码              |
| Knife4j / OpenAPI | 可选，用于接口调试和接口文档查看           |

推荐的实现方案如下：

| 模块     | 方案                                     |
| -------- | ---------------------------------------- |
| 文件存储 | 使用 MinIO 私有 Bucket                   |
| 文件去重 | 使用 SHA-256 判断文件内容是否相同        |
| 秒传判断 | 根据文件 Hash 查询物理文件记录           |
| 分片存储 | 分片先保存到 MinIO 临时目录              |
| 分片合并 | 后端按分片序号读取并合并为完整对象       |
| 文件访问 | 后端鉴权后生成 MinIO 预签名 URL          |
| 分片状态 | MySQL 持久化，Redis 做查询加速           |
| 权限模型 | 上传人权限为主，预留角色、部门、租户扩展 |

后续实现默认使用以下基础包路径：

```text
io.github.atengk.file
```

推荐模块目录如下：

```text
src/main/java/io/github/atengk/file
├── config
│   └── MinioProperties.java
├── controller
│   ├── FileController.java
│   └── FileChunkController.java
├── domain
│   ├── entity
│   │   ├── FileInfo.java
│   │   ├── FileStorage.java
│   │   └── FileChunkTask.java
│   ├── dto
│   │   ├── FileInstantCheckDTO.java
│   │   ├── FileInstantSaveDTO.java
│   │   ├── FileChunkInitDTO.java
│   │   ├── FileChunkUploadDTO.java
│   │   └── FileChunkMergeDTO.java
│   └── vo
│       ├── FileInfoVO.java
│       ├── FileInstantCheckVO.java
│       ├── FileChunkInitVO.java
│       └── FileUploadedChunkVO.java
├── mapper
│   ├── FileInfoMapper.java
│   ├── FileStorageMapper.java
│   └── FileChunkTaskMapper.java
├── service
│   ├── FileService.java
│   ├── FileChunkService.java
│   └── impl
│       ├── FileServiceImpl.java
│       └── FileChunkServiceImpl.java
└── storage
    └── MinioStorageService.java
```

## 环境准备

本节先准备项目依赖、MinIO 服务、Spring Boot 配置和数据库表。后续代码实现会默认基于这些配置展开。

### Maven 依赖

下面依赖用于整合 Web 接口、MinIO 对象存储、MyBatis-Plus、Redis、Sa-Token、Hutool、Lombok 和 MySQL。

```xml
<dependencies>
    <!-- Web 接口与 multipart 文件上传支持 -->
    <dependency>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-starter-web</artifactId>
    </dependency>

    <!-- 参数校验，用于 DTO 入参校验 -->
    <dependency>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-starter-validation</artifactId>
    </dependency>

    <!-- Redis，用于缓存分片上传状态 -->
    <dependency>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-starter-data-redis</artifactId>
    </dependency>

    <!-- MyBatis-Plus，用于文件元数据和分片任务的数据访问 -->
    <dependency>
        <groupId>com.baomidou</groupId>
        <artifactId>mybatis-plus-spring-boot3-starter</artifactId>
        <version>${mybatis-plus.version}</version>
    </dependency>

    <!-- MySQL 驱动 -->
    <dependency>
        <groupId>com.mysql</groupId>
        <artifactId>mysql-connector-j</artifactId>
        <scope>runtime</scope>
    </dependency>

    <!-- MinIO Java SDK，用于上传、合并、生成预签名访问链接 -->
    <dependency>
        <groupId>io.minio</groupId>
        <artifactId>minio</artifactId>
        <version>${minio.version}</version>
    </dependency>

    <!-- Sa-Token，用于获取当前登录用户和权限扩展 -->
    <dependency>
        <groupId>cn.dev33</groupId>
        <artifactId>sa-token-spring-boot3-starter</artifactId>
        <version>${sa-token.version}</version>
    </dependency>

    <!-- Hutool，提供 Hash、字符串、集合、日期等常用工具 -->
    <dependency>
        <groupId>cn.hutool</groupId>
        <artifactId>hutool-all</artifactId>
        <version>${hutool.version}</version>
    </dependency>

    <!-- Lombok，简化实体类、DTO、VO 和日志代码 -->
    <dependency>
        <groupId>org.projectlombok</groupId>
        <artifactId>lombok</artifactId>
        <optional>true</optional>
    </dependency>

    <!-- Knife4j，可选，用于接口调试和文档查看 -->
    <dependency>
        <groupId>com.github.xiaoymin</groupId>
        <artifactId>knife4j-openapi3-jakarta-spring-boot-starter</artifactId>
        <version>${knife4j.version}</version>
    </dependency>
</dependencies>
```

推荐在 `pom.xml` 中统一维护版本号，避免依赖版本散落在多个位置。

```xml
<properties>
    <!-- Java 版本，Spring Boot 3 推荐 JDK 17+ -->
    <java.version>17</java.version>

    <!-- 数据访问增强工具 -->
    <mybatis-plus.version>3.5.7</mybatis-plus.version>

    <!-- MinIO 对象存储 SDK -->
    <minio.version>8.5.12</minio.version>

    <!-- Sa-Token 权限认证框架 -->
    <sa-token.version>1.39.0</sa-token.version>

    <!-- Hutool 工具类库 -->
    <hutool.version>5.8.32</hutool.version>

    <!-- 接口文档工具，可按项目实际情况移除 -->
    <knife4j.version>4.5.0</knife4j.version>
</properties>
```

如果项目已经使用统一的 BOM 或公司脚手架管理依赖版本，保留依赖声明即可，版本号交给父工程管理。

### MinIO 服务准备

本案例使用 MinIO 作为对象存储服务。文件内容不直接保存到业务服务器本地磁盘，而是统一保存到 MinIO 的私有 Bucket 中。

下面使用 Docker Compose 启动一个本地 MinIO 服务。

```yaml
version: "3.8"

services:
  minio:
    image: minio/minio:latest
    container_name: file-minio
    restart: always
    command: server /data --console-address ":9001"
    ports:
      - "9000:9000" # MinIO API 端口，后端程序访问该端口
      - "9001:9001" # MinIO 控制台端口，浏览器访问该端口
    environment:
      MINIO_ROOT_USER: admin
      MINIO_ROOT_PASSWORD: admin123456
    volumes:
      - ./data/minio:/data
```

在 `docker-compose.yml` 所在目录执行下面命令启动 MinIO。

```bash
docker compose up -d
```

启动后访问 MinIO 控制台：

```text
http://localhost:9001
```

登录账号：

```text
用户名：admin
密码：admin123456
```

进入控制台后创建 Bucket：

```text
file-bucket
```

Bucket 建议保持私有，不要设置为公开读。文件下载统一通过后端接口鉴权后生成临时访问链接。

本案例约定 MinIO 对象路径如下：

| 类型     | 对象路径示例                      | 说明                           |
| -------- | --------------------------------- | ------------------------------ |
| 完整文件 | `files/2026/05/15/{fileHash}.pdf` | 最终合并或普通上传后的完整文件 |
| 临时分片 | `chunks/{fileHash}/{chunkIndex}`  | 分片上传过程中的临时对象       |
| 图片文件 | `files/2026/05/15/{fileHash}.png` | 图片、头像、素材等普通文件     |

### Spring Boot 配置

下面配置用于连接 MySQL、Redis 和 MinIO，同时限制上传大小、配置分片大小和临时下载链接有效期。

文件位置：`src/main/resources/application.yml`

```yaml
server:
  port: 8080

spring:
  application:
    name: file-upload-demo

  datasource:
    driver-class-name: com.mysql.cj.jdbc.Driver
    url: jdbc:mysql://localhost:3306/file_demo?useUnicode=true&characterEncoding=utf8&serverTimezone=Asia/Shanghai&useSSL=false
    username: root
    password: root

  data:
    redis:
      host: localhost
      port: 6379
      database: 0
      timeout: 5s

  servlet:
    multipart:
      # 单个请求文件最大大小，普通上传和分片上传都会受该配置影响
      max-file-size: 200MB
      # 单次请求最大大小，需要大于 max-file-size
      max-request-size: 220MB

mybatis-plus:
  mapper-locations: classpath*:/mapper/**/*.xml
  configuration:
    # 开发阶段建议开启，便于排查 SQL 字段映射问题
    map-underscore-to-camel-case: true
  global-config:
    db-config:
      # 使用雪花 ID，适合分布式场景
      id-type: assign_id
      # 逻辑删除字段
      logic-delete-field: deleted
      logic-delete-value: 1
      logic-not-delete-value: 0

sa-token:
  # Token 名称，可按项目规范调整
  token-name: Authorization
  # Token 有效期，单位秒
  timeout: 2592000
  # 是否允许同一账号多地登录
  is-concurrent: true
  # 是否从请求头读取 Token
  is-read-header: true

file:
  minio:
    endpoint: http://localhost:9000
    access-key: admin
    secret-key: admin123456
    bucket-name: file-bucket
    # 下载链接有效期，单位分钟
    presigned-expire-minutes: 10
    # 默认分片大小，单位字节，5MB
    chunk-size: 5242880
```

后续会通过 `file.minio` 配置项绑定到 `MinioProperties` 配置类中，业务代码不直接硬编码 MinIO 地址、Bucket、AccessKey 等信息。

### 数据表设计

本案例将文件相关数据拆成四张表：

| 表名              | 作用                                     |
| ----------------- | ---------------------------------------- |
| `file_storage`    | 保存物理文件信息，同一个 Hash 只保存一份 |
| `file_info`       | 保存用户维度的业务文件记录               |
| `file_chunk_task` | 保存分片上传任务                         |
| `file_chunk_part` | 保存已上传的分片记录                     |

下面 SQL 用于创建文件上传模块需要的核心表。

```sql
CREATE TABLE file_storage (
    id BIGINT NOT NULL COMMENT '主键ID',
    file_hash VARCHAR(128) NOT NULL COMMENT '文件SHA-256 Hash',
    bucket_name VARCHAR(100) NOT NULL COMMENT 'MinIO Bucket名称',
    object_name VARCHAR(500) NOT NULL COMMENT 'MinIO对象名称',
    file_size BIGINT NOT NULL COMMENT '文件大小，单位字节',
    content_type VARCHAR(200) DEFAULT NULL COMMENT '文件MIME类型',
    file_ext VARCHAR(50) DEFAULT NULL COMMENT '文件扩展名',
    ref_count INT NOT NULL DEFAULT 1 COMMENT '引用次数',
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    deleted TINYINT NOT NULL DEFAULT 0 COMMENT '逻辑删除：0未删除，1已删除',
    PRIMARY KEY (id),
    UNIQUE KEY uk_file_hash (file_hash),
    KEY idx_create_time (create_time)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='物理文件存储表';

CREATE TABLE file_info (
    id BIGINT NOT NULL COMMENT '主键ID',
    storage_id BIGINT NOT NULL COMMENT '物理文件ID',
    original_name VARCHAR(255) NOT NULL COMMENT '原始文件名',
    file_size BIGINT NOT NULL COMMENT '文件大小，单位字节',
    content_type VARCHAR(200) DEFAULT NULL COMMENT '文件MIME类型',
    file_ext VARCHAR(50) DEFAULT NULL COMMENT '文件扩展名',
    access_type VARCHAR(20) NOT NULL DEFAULT 'PRIVATE' COMMENT '访问类型：PRIVATE私有，PUBLIC公开',
    uploader_id BIGINT NOT NULL COMMENT '上传人ID',
    upload_source VARCHAR(50) DEFAULT NULL COMMENT '上传来源，例如avatar、attachment、material',
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    deleted TINYINT NOT NULL DEFAULT 0 COMMENT '逻辑删除：0未删除，1已删除',
    PRIMARY KEY (id),
    KEY idx_storage_id (storage_id),
    KEY idx_uploader_id (uploader_id),
    KEY idx_create_time (create_time)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='业务文件信息表';

CREATE TABLE file_chunk_task (
    id BIGINT NOT NULL COMMENT '主键ID',
    file_hash VARCHAR(128) NOT NULL COMMENT '文件SHA-256 Hash',
    original_name VARCHAR(255) NOT NULL COMMENT '原始文件名',
    file_size BIGINT NOT NULL COMMENT '文件大小，单位字节',
    chunk_size BIGINT NOT NULL COMMENT '分片大小，单位字节',
    total_chunks INT NOT NULL COMMENT '总分片数',
    content_type VARCHAR(200) DEFAULT NULL COMMENT '文件MIME类型',
    file_ext VARCHAR(50) DEFAULT NULL COMMENT '文件扩展名',
    status VARCHAR(20) NOT NULL DEFAULT 'UPLOADING' COMMENT '任务状态：UPLOADING上传中，MERGED已合并，FAILED失败',
    uploader_id BIGINT NOT NULL COMMENT '上传人ID',
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    deleted TINYINT NOT NULL DEFAULT 0 COMMENT '逻辑删除：0未删除，1已删除',
    PRIMARY KEY (id),
    UNIQUE KEY uk_file_hash_uploader (file_hash, uploader_id),
    KEY idx_status (status),
    KEY idx_create_time (create_time)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='分片上传任务表';

CREATE TABLE file_chunk_part (
    id BIGINT NOT NULL COMMENT '主键ID',
    task_id BIGINT NOT NULL COMMENT '分片任务ID',
    file_hash VARCHAR(128) NOT NULL COMMENT '文件SHA-256 Hash',
    chunk_index INT NOT NULL COMMENT '分片序号，从0开始',
    chunk_size BIGINT NOT NULL COMMENT '当前分片大小，单位字节',
    bucket_name VARCHAR(100) NOT NULL COMMENT 'MinIO Bucket名称',
    object_name VARCHAR(500) NOT NULL COMMENT 'MinIO分片对象名称',
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    deleted TINYINT NOT NULL DEFAULT 0 COMMENT '逻辑删除：0未删除，1已删除',
    PRIMARY KEY (id),
    UNIQUE KEY uk_task_chunk (task_id, chunk_index),
    KEY idx_file_hash (file_hash),
    KEY idx_create_time (create_time)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='分片上传记录表';
```

表关系说明：

| 关系                        | 说明                                                     |
| --------------------------- | -------------------------------------------------------- |
| `file_info.storage_id`      | 关联 `file_storage.id`，表示当前业务文件引用哪个物理文件 |
| `file_chunk_part.task_id`   | 关联 `file_chunk_task.id`，表示当前分片属于哪个上传任务  |
| `file_storage.file_hash`    | 文件内容唯一标识，用于秒传判断                           |
| `file_chunk_task.file_hash` | 分片任务的文件唯一标识，用于断点续传和合并               |

Redis 中建议使用以下 Key：

| Key                            | 类型   | 说明                     |
| ------------------------------ | ------ | ------------------------ |
| `file:chunk:uploaded:{taskId}` | Set    | 已上传分片序号集合       |
| `file:chunk:lock:{fileHash}`   | String | 文件合并锁，防止重复合并 |
| `file:instant:{fileHash}`      | String | 秒传检测缓存，可选       |

## 核心流程设计

本节描述文件模块的主要业务流程。后续 Controller、Service 和 MinIO 存储层代码会严格按照这些流程实现。

### 普通文件上传流程

普通上传适合小文件，例如头像、图片、PDF、Excel、Word、普通附件等。前端直接提交 `multipart/form-data` 请求，后端接收文件后上传到 MinIO。

```text
前端选择文件
    ↓
调用普通上传接口
    ↓
后端校验文件是否为空、大小是否合法
    ↓
后端计算文件 SHA-256
    ↓
根据 Hash 查询 file_storage
    ↓
物理文件已存在：复用 file_storage
    ↓
物理文件不存在：上传文件到 MinIO，并写入 file_storage
    ↓
写入 file_info 业务文件记录
    ↓
返回文件 ID、文件名、大小、访问类型
```

普通上传的核心处理点：

| 处理点       | 说明                                             |
| ------------ | ------------------------------------------------ |
| 文件校验     | 校验文件是否为空、大小是否超过限制               |
| Hash 计算    | 使用 SHA-256 计算文件唯一标识                    |
| 物理文件去重 | 相同 Hash 的文件只保存一份                       |
| 元数据落库   | 保存业务文件记录，便于后续权限控制               |
| 下载地址     | 上传接口不直接返回永久地址，下载时再生成临时链接 |

### 文件秒传流程

秒传适合大文件上传前的预检查。前端先在浏览器中计算文件 SHA-256，然后请求后端判断文件是否已经存在。

```text
前端选择文件
    ↓
前端计算文件 SHA-256
    ↓
调用秒传检测接口
    ↓
后端根据 Hash 查询 file_storage
    ↓
文件不存在：返回需要上传
    ↓
文件已存在：返回可秒传
    ↓
前端调用秒传保存接口
    ↓
后端创建 file_info 业务文件记录
    ↓
返回文件 ID
```

秒传接口拆成两个步骤：

| 步骤     | 接口                       | 说明                                 |
| -------- | -------------------------- | ------------------------------------ |
| 秒传检测 | `/api/files/instant-check` | 判断文件 Hash 是否已存在             |
| 秒传保存 | `/api/files/instant-save`  | 文件存在时创建当前用户的业务文件记录 |

这样设计的原因是：检测接口只判断物理文件是否存在，不直接创建业务文件记录；保存接口由前端确认文件名、上传来源、权限类型后再调用。

### 分片上传流程

分片上传适合大文件。前端先计算文件 Hash，然后初始化上传任务，再查询已上传分片，只上传缺失部分。

```text
前端选择大文件
    ↓
前端计算文件 SHA-256
    ↓
调用秒传检测接口
    ↓
文件已存在：走秒传保存
    ↓
文件不存在：调用分片初始化接口
    ↓
后端创建或返回已有 file_chunk_task
    ↓
前端查询已上传分片
    ↓
前端过滤缺失分片
    ↓
逐个上传缺失分片
    ↓
后端保存分片到 MinIO 临时目录
    ↓
写入 file_chunk_part，并同步 Redis Set
    ↓
所有分片上传完成后调用合并接口
```

分片对象路径约定如下：

```text
chunks/{fileHash}/{chunkIndex}
```

例如：

```text
chunks/9f86d081884c7d659a2feaa0c55ad015/0
chunks/9f86d081884c7d659a2feaa0c55ad015/1
chunks/9f86d081884c7d659a2feaa0c55ad015/2
```

分片上传需要保证接口幂等：

| 场景             | 处理方式                                                |
| ---------------- | ------------------------------------------------------- |
| 同一分片重复上传 | 根据 `task_id + chunk_index` 判断，已存在则直接返回成功 |
| 上传中断         | 前端重新查询已上传分片，只补传缺失分片                  |
| 任务已合并       | 禁止继续上传分片                                        |
| 分片序号非法     | 后端校验 `chunkIndex >= 0` 且 `< totalChunks`           |

### 文件合并流程

所有分片上传完成后，前端调用合并接口。后端需要先校验分片数量，再按分片序号合并为完整文件。

```text
前端调用合并接口
    ↓
后端查询分片任务
    ↓
校验任务状态是否为 UPLOADING
    ↓
加 Redis 合并锁，防止重复合并
    ↓
查询 file_chunk_part 分片记录
    ↓
校验分片数量是否等于 total_chunks
    ↓
按 chunk_index 升序读取 MinIO 分片对象
    ↓
合并上传为完整文件对象
    ↓
写入或复用 file_storage
    ↓
写入 file_info 业务文件记录
    ↓
更新 file_chunk_task 状态为 MERGED
    ↓
删除 MinIO 临时分片对象
    ↓
释放 Redis 合并锁
```

完整文件对象路径约定如下：

```text
files/yyyy/MM/dd/{fileHash}.{ext}
```

例如：

```text
files/2026/05/15/9f86d081884c7d659a2feaa0c55ad015.pdf
```

合并时需要注意：

| 注意点       | 说明                                                    |
| ------------ | ------------------------------------------------------- |
| 防重复合并   | 使用 Redis 锁控制同一个 `fileHash` 同一时间只能合并一次 |
| 分片顺序     | 必须按 `chunk_index` 升序合并                           |
| 分片完整性   | 分片数量必须等于 `total_chunks`                         |
| 合并后校验   | 合并后的文件 Hash 应与前端传入 Hash 一致                |
| 临时文件清理 | 合并成功后删除临时分片，避免 MinIO 空间浪费             |

### 文件权限访问流程

文件默认作为私有资源处理，前端不能直接拼接 MinIO 地址访问。所有下载行为都先经过后端接口。

```text
前端请求文件临时下载链接
    ↓
后端读取当前登录用户
    ↓
根据 fileId 查询 file_info
    ↓
查询关联的 file_storage
    ↓
判断 access_type
    ↓
PUBLIC：允许访问
    ↓
PRIVATE：校验 uploader_id 是否等于当前用户 ID
    ↓
权限通过：生成 MinIO 预签名 URL
    ↓
权限失败：返回无权访问
```

本案例先实现最小权限模型：

| 文件类型  | 访问规则       |
| --------- | -------------- |
| `PUBLIC`  | 登录用户可访问 |
| `PRIVATE` | 仅上传人可访问 |

后续可扩展以下权限模型：

| 扩展方向 | 说明                                       |
| -------- | ------------------------------------------ |
| 部门权限 | 同部门用户可访问                           |
| 角色权限 | 指定角色可访问                             |
| 租户隔离 | 不同租户文件隔离                           |
| 业务绑定 | 文件绑定订单、工单、项目后，按业务权限判断 |
| 分享链接 | 生成带过期时间和访问码的分享链接           |

## 项目代码结构

本节固定后续代码文件的位置。完整实现会按照 Controller、Service、Storage、Mapper、DTO、VO 分层编写。

推荐基础包路径：

```text
io.github.atengk.file
```

推荐目录结构如下：

```text
src/main/java/io/github/atengk/file
├── config
│   ├── MinioConfig.java
│   └── MinioProperties.java
├── controller
│   ├── FileController.java
│   └── FileChunkController.java
├── domain
│   ├── entity
│   │   ├── FileInfo.java
│   │   ├── FileStorage.java
│   │   ├── FileChunkTask.java
│   │   └── FileChunkPart.java
│   ├── dto
│   │   ├── FileInstantCheckDTO.java
│   │   ├── FileInstantSaveDTO.java
│   │   ├── FileUploadDTO.java
│   │   ├── FileChunkInitDTO.java
│   │   ├── FileChunkUploadDTO.java
│   │   └── FileChunkMergeDTO.java
│   └── vo
│       ├── FileInfoVO.java
│       ├── FileInstantCheckVO.java
│       ├── FileChunkInitVO.java
│       ├── FileUploadedChunkVO.java
│       └── FileDownloadUrlVO.java
├── mapper
│   ├── FileInfoMapper.java
│   ├── FileStorageMapper.java
│   ├── FileChunkTaskMapper.java
│   └── FileChunkPartMapper.java
├── service
│   ├── FileService.java
│   ├── FileChunkService.java
│   └── impl
│       ├── FileServiceImpl.java
│       └── FileChunkServiceImpl.java
├── storage
│   └── MinioStorageService.java
└── enums
    ├── FileAccessTypeEnum.java
    └── FileChunkTaskStatusEnum.java
```

资源文件目录如下：

```text
src/main/resources
├── application.yml
└── mapper
    ├── FileInfoMapper.xml
    ├── FileStorageMapper.xml
    ├── FileChunkTaskMapper.xml
    └── FileChunkPartMapper.xml
```

### Controller 接口层

Controller 层只负责接收请求、校验基础参数、调用 Service，不直接处理 MinIO 上传细节。

| 类名                  | 作用                                             |
| --------------------- | ------------------------------------------------ |
| `FileController`      | 普通上传、秒传检测、秒传保存、文件详情、下载链接 |
| `FileChunkController` | 分片初始化、分片上传、已上传分片查询、分片合并   |

接口划分如下：

| Controller            | 接口                                   | 说明           |
| --------------------- | -------------------------------------- | -------------- |
| `FileController`      | `POST /api/files/upload`               | 普通文件上传   |
| `FileController`      | `POST /api/files/instant-check`        | 秒传检测       |
| `FileController`      | `POST /api/files/instant-save`         | 秒传保存       |
| `FileController`      | `GET /api/files/{fileId}`              | 文件详情       |
| `FileController`      | `GET /api/files/{fileId}/download-url` | 临时下载链接   |
| `FileChunkController` | `POST /api/files/chunk/init`           | 初始化分片任务 |
| `FileChunkController` | `POST /api/files/chunk/upload`         | 上传单个分片   |
| `FileChunkController` | `GET /api/files/chunk/uploaded`        | 查询已上传分片 |
| `FileChunkController` | `POST /api/files/chunk/merge`          | 合并分片       |

### Service 业务层

Service 层负责文件模块的核心业务编排，包括秒传判断、文件记录保存、权限校验、分片任务管理、分片合并等逻辑。

| 类名                   | 作用                               |
| ---------------------- | ---------------------------------- |
| `FileService`          | 普通文件、秒传、文件详情、下载链接 |
| `FileChunkService`     | 分片初始化、上传、查询、合并       |
| `FileServiceImpl`      | 文件业务实现                       |
| `FileChunkServiceImpl` | 分片业务实现                       |

核心方法规划如下：

| Service            | 方法                 | 说明             |
| ------------------ | -------------------- | ---------------- |
| `FileService`      | `upload`             | 普通上传         |
| `FileService`      | `instantCheck`       | 秒传检测         |
| `FileService`      | `instantSave`        | 秒传保存         |
| `FileService`      | `detail`             | 文件详情         |
| `FileService`      | `getDownloadUrl`     | 获取临时下载链接 |
| `FileChunkService` | `initChunkTask`      | 初始化分片任务   |
| `FileChunkService` | `uploadChunk`        | 上传分片         |
| `FileChunkService` | `listUploadedChunks` | 查询已上传分片   |
| `FileChunkService` | `mergeChunks`        | 合并分片         |

### MinIO 存储层

MinIO 存储层封装对象存储操作，避免业务代码直接调用 MinIO SDK。后续如果替换为阿里云 OSS、腾讯云 COS 或本地存储，只需要替换该层实现。

| 类名                  | 作用                                   |
| --------------------- | -------------------------------------- |
| `MinioConfig`         | 初始化 MinIO 客户端                    |
| `MinioProperties`     | 绑定 MinIO 配置项                      |
| `MinioStorageService` | 封装上传、合并、删除、预签名链接等操作 |

MinIO 存储层方法规划如下：

| 方法              | 说明                   |
| ----------------- | ---------------------- |
| `uploadFile`      | 上传普通文件           |
| `uploadChunk`     | 上传分片文件           |
| `composeObject`   | 合并多个分片为完整文件 |
| `removeObject`    | 删除单个对象           |
| `removeObjects`   | 批量删除对象           |
| `getPresignedUrl` | 生成临时访问链接       |
| `objectExists`    | 判断对象是否存在       |

### Mapper 数据访问层

Mapper 层只负责数据库访问，不写业务判断。业务条件在 Service 层组织，Mapper 保持简单清晰。

| Mapper                | 对应表            | 说明               |
| --------------------- | ----------------- | ------------------ |
| `FileStorageMapper`   | `file_storage`    | 查询和保存物理文件 |
| `FileInfoMapper`      | `file_info`       | 查询和保存业务文件 |
| `FileChunkTaskMapper` | `file_chunk_task` | 查询和保存分片任务 |
| `FileChunkPartMapper` | `file_chunk_part` | 查询和保存分片记录 |

常用查询规划如下：

| 查询                                        | 说明                 |
| ------------------------------------------- | -------------------- |
| 根据 `file_hash` 查询物理文件               | 用于秒传判断         |
| 根据 `file_id` 查询业务文件                 | 用于详情和下载鉴权   |
| 根据 `file_hash + uploader_id` 查询分片任务 | 用于断点续传         |
| 根据 `task_id` 查询已上传分片               | 用于前端过滤缺失分片 |
| 根据 `task_id + chunk_index` 查询分片       | 用于分片上传幂等     |

### DTO 与 VO 对象

DTO 用于接收前端请求参数，VO 用于返回前端展示结果。实体类不直接作为接口入参或响应对象，避免数据库字段暴露到接口层。

DTO 规划如下：

| DTO                   | 说明                                           |
| --------------------- | ---------------------------------------------- |
| `FileUploadDTO`       | 普通上传附加参数，例如访问类型、上传来源       |
| `FileInstantCheckDTO` | 秒传检测参数，包含文件 Hash、文件名、文件大小  |
| `FileInstantSaveDTO`  | 秒传保存参数，包含文件 Hash、文件名、访问类型  |
| `FileChunkInitDTO`    | 初始化分片任务参数                             |
| `FileChunkUploadDTO`  | 分片上传参数，包含任务 ID、分片序号            |
| `FileChunkMergeDTO`   | 分片合并参数，包含任务 ID、文件 Hash、访问类型 |

VO 规划如下：

| VO                    | 说明             |
| --------------------- | ---------------- |
| `FileInfoVO`          | 文件详情返回对象 |
| `FileInstantCheckVO`  | 秒传检测结果     |
| `FileChunkInitVO`     | 分片初始化结果   |
| `FileUploadedChunkVO` | 已上传分片列表   |
| `FileDownloadUrlVO`   | 临时下载链接结果 |

后续代码实现会按照以上结构展开，优先完成核心功能，非必要的后台管理、复杂授权、文件预览、异步清理任务可以作为扩展功能处理。

## 普通文件上传实现

普通文件上传适合头像、图片、PDF、Excel、Word、普通附件等小文件场景。核心逻辑是：后端接收文件，计算文件 Hash，根据 Hash 判断物理文件是否已存在，不存在则上传到 MinIO，最后保存一条业务文件记录。

本节代码默认已经存在以下 Mapper 和 Entity：

```text
FileInfoMapper extends BaseMapper<FileInfo>
FileStorageMapper extends BaseMapper<FileStorage>
FileChunkTaskMapper extends BaseMapper<FileChunkTask>
FileChunkPartMapper extends BaseMapper<FileChunkPart>
```

代码中的 `R<T>` 表示项目统一响应对象。如果你的项目中叫 `Result<T>`、`AjaxResult` 或其他名称，直接替换即可。

### 上传接口定义

文件上传接口放在 `FileController` 中，负责普通上传、秒传检测、秒传保存、文件详情、临时下载链接等入口。

文件位置：`src/main/java/io/github/atengk/file/controller/FileController.java`

```java
package io.github.atengk.file.controller;

import io.github.atengk.file.domain.dto.FileInstantCheckDTO;
import io.github.atengk.file.domain.dto.FileInstantSaveDTO;
import io.github.atengk.file.domain.vo.FileDownloadUrlVO;
import io.github.atengk.file.domain.vo.FileInfoVO;
import io.github.atengk.file.domain.vo.FileInstantCheckVO;
import io.github.atengk.file.service.FileService;
import io.github.atengk.system.common.R;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

/**
 * 文件管理接口
 *
 * @author Ateng
 * @since 2026-05-15
 */
@RestController
@RequestMapping("/api/files")
@RequiredArgsConstructor
public class FileController {

    private final FileService fileService;

    /**
     * 普通文件上传
     *
     * @param file 文件
     * @param accessType 访问类型：PRIVATE私有，PUBLIC公开
     * @param uploadSource 上传来源
     * @return 文件信息
     */
    @PostMapping(value = "/upload", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public R<FileInfoVO> upload(@RequestPart("file") MultipartFile file,
                                @RequestParam(defaultValue = "PRIVATE") String accessType,
                                @RequestParam(required = false) String uploadSource) {
        return R.ok(fileService.upload(file, accessType, uploadSource));
    }

    /**
     * 秒传检测
     *
     * @param dto 秒传检测参数
     * @return 秒传检测结果
     */
    @PostMapping("/instant-check")
    public R<FileInstantCheckVO> instantCheck(@Valid @RequestBody FileInstantCheckDTO dto) {
        return R.ok(fileService.instantCheck(dto));
    }

    /**
     * 秒传保存
     *
     * @param dto 秒传保存参数
     * @return 文件信息
     */
    @PostMapping("/instant-save")
    public R<FileInfoVO> instantSave(@Valid @RequestBody FileInstantSaveDTO dto) {
        return R.ok(fileService.instantSave(dto));
    }

    /**
     * 查询文件详情
     *
     * @param fileId 文件ID
     * @return 文件详情
     */
    @GetMapping("/{fileId}")
    public R<FileInfoVO> detail(@PathVariable Long fileId) {
        return R.ok(fileService.detail(fileId));
    }

    /**
     * 获取临时下载链接
     *
     * @param fileId 文件ID
     * @return 临时下载链接
     */
    @GetMapping("/{fileId}/download-url")
    public R<FileDownloadUrlVO> downloadUrl(@PathVariable Long fileId) {
        return R.ok(fileService.getDownloadUrl(fileId));
    }
}
```

文件业务接口定义普通上传、秒传、详情查询和访问地址生成方法。

文件位置：`src/main/java/io/github/atengk/file/service/FileService.java`

```java
package io.github.atengk.file.service;

import io.github.atengk.file.domain.dto.FileInstantCheckDTO;
import io.github.atengk.file.domain.dto.FileInstantSaveDTO;
import io.github.atengk.file.domain.vo.FileDownloadUrlVO;
import io.github.atengk.file.domain.vo.FileInfoVO;
import io.github.atengk.file.domain.vo.FileInstantCheckVO;
import org.springframework.web.multipart.MultipartFile;

/**
 * 文件业务接口
 *
 * @author Ateng
 * @since 2026-05-15
 */
public interface FileService {

    /**
     * 普通文件上传
     *
     * @param file 文件
     * @param accessType 访问类型
     * @param uploadSource 上传来源
     * @return 文件信息
     */
    FileInfoVO upload(MultipartFile file, String accessType, String uploadSource);

    /**
     * 秒传检测
     *
     * @param dto 秒传检测参数
     * @return 检测结果
     */
    FileInstantCheckVO instantCheck(FileInstantCheckDTO dto);

    /**
     * 秒传保存
     *
     * @param dto 秒传保存参数
     * @return 文件信息
     */
    FileInfoVO instantSave(FileInstantSaveDTO dto);

    /**
     * 查询文件详情
     *
     * @param fileId 文件ID
     * @return 文件详情
     */
    FileInfoVO detail(Long fileId);

    /**
     * 获取文件临时下载链接
     *
     * @param fileId 文件ID
     * @return 下载链接
     */
    FileDownloadUrlVO getDownloadUrl(Long fileId);
}
```

MinIO 存储服务封装对象上传、分片上传、分片合并、对象删除和临时链接生成，避免业务层直接操作 MinIO SDK。

文件位置：`src/main/java/io/github/atengk/file/storage/MinioStorageService.java`

```java
package io.github.atengk.file.storage;

import cn.hutool.core.collection.CollUtil;
import io.github.atengk.file.config.MinioProperties;
import io.minio.*;
import io.minio.http.Method;
import io.minio.messages.DeleteObject;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.io.InputStream;
import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * MinIO 文件存储服务
 *
 * @author Ateng
 * @since 2026-05-15
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class MinioStorageService {

    private final MinioClient minioClient;
    private final MinioProperties minioProperties;

    /**
     * 上传完整文件
     *
     * @param objectName 对象名称
     * @param inputStream 文件流
     * @param size 文件大小
     * @param contentType 文件类型
     */
    public void uploadFile(String objectName, InputStream inputStream, long size, String contentType) {
        try {
            minioClient.putObject(
                    PutObjectArgs.builder()
                            .bucket(minioProperties.getBucketName())
                            .object(objectName)
                            .stream(inputStream, size, -1)
                            .contentType(contentType)
                            .build()
            );
            log.info("文件上传到MinIO成功，对象：{}", objectName);
        } catch (Exception e) {
            log.error("文件上传到MinIO失败，对象：{}", objectName, e);
            throw new RuntimeException("文件上传失败");
        }
    }

    /**
     * 上传分片文件
     *
     * @param objectName 分片对象名称
     * @param inputStream 分片文件流
     * @param size 分片大小
     */
    public void uploadChunk(String objectName, InputStream inputStream, long size) {
        uploadFile(objectName, inputStream, size, "application/octet-stream");
    }

    /**
     * 合并分片对象
     *
     * @param targetObjectName 目标对象名称
     * @param sourceObjectNames 分片对象名称列表
     */
    public void composeObject(String targetObjectName, List<String> sourceObjectNames) {
        if (CollUtil.isEmpty(sourceObjectNames)) {
            throw new IllegalArgumentException("分片对象不能为空");
        }

        try {
            List<ComposeSource> sources = sourceObjectNames.stream()
                    .map(objectName -> ComposeSource.builder()
                            .bucket(minioProperties.getBucketName())
                            .object(objectName)
                            .build())
                    .toList();

            minioClient.composeObject(
                    ComposeObjectArgs.builder()
                            .bucket(minioProperties.getBucketName())
                            .object(targetObjectName)
                            .sources(sources)
                            .build()
            );
            log.info("MinIO分片合并成功，目标对象：{}", targetObjectName);
        } catch (Exception e) {
            log.error("MinIO分片合并失败，目标对象：{}", targetObjectName, e);
            throw new RuntimeException("文件分片合并失败");
        }
    }

    /**
     * 生成临时访问链接
     *
     * @param objectName 对象名称
     * @return 临时访问链接
     */
    public String getPresignedUrl(String objectName) {
        try {
            return minioClient.getPresignedObjectUrl(
                    GetPresignedObjectUrlArgs.builder()
                            .bucket(minioProperties.getBucketName())
                            .object(objectName)
                            .method(Method.GET)
                            .expiry(minioProperties.getPresignedExpireMinutes(), TimeUnit.MINUTES)
                            .build()
            );
        } catch (Exception e) {
            log.error("生成MinIO临时访问链接失败，对象：{}", objectName, e);
            throw new RuntimeException("生成文件访问链接失败");
        }
    }

    /**
     * 批量删除对象
     *
     * @param objectNames 对象名称列表
     */
    public void removeObjects(List<String> objectNames) {
        if (CollUtil.isEmpty(objectNames)) {
            return;
        }

        try {
            List<DeleteObject> deleteObjects = objectNames.stream()
                    .map(DeleteObject::new)
                    .toList();

            minioClient.removeObjects(
                    RemoveObjectsArgs.builder()
                            .bucket(minioProperties.getBucketName())
                            .objects(deleteObjects)
                            .build()
            ).forEach(result -> {
                try {
                    result.get();
                } catch (Exception e) {
                    log.warn("删除MinIO对象失败", e);
                }
            });
            log.info("MinIO对象批量删除完成，数量：{}", objectNames.size());
        } catch (Exception e) {
            log.warn("MinIO对象批量删除异常", e);
        }
    }
}
```

### 文件信息保存

文件信息保存分为两层：`file_storage` 保存物理文件，`file_info` 保存业务文件记录。普通上传时，如果 `file_storage` 中已经存在相同 Hash 的文件，则不重复上传到 MinIO，只新增一条 `file_info` 记录。

文件 Hash 工具类用于计算文件 SHA-256。普通上传时后端会重新计算 Hash，不直接信任前端传入值。

文件位置：`src/main/java/io/github/atengk/file/util/FileHashUtils.java`

```java
package io.github.atengk.file.util;

import cn.hutool.crypto.SecureUtil;

import java.io.InputStream;

/**
 * 文件Hash工具类
 *
 * @author Ateng
 * @since 2026-05-15
 */
public final class FileHashUtils {

    private FileHashUtils() {
    }

    /**
     * 计算文件SHA-256
     *
     * @param inputStream 文件输入流
     * @return SHA-256值
     */
    public static String sha256(InputStream inputStream) {
        return SecureUtil.sha256().digestHex(inputStream);
    }
}
```

文件业务实现类完成普通上传、秒传、文件详情和下载链接生成。

文件位置：`src/main/java/io/github/atengk/file/service/impl/FileServiceImpl.java`

```java
package io.github.atengk.file.service.impl;

import cn.dev33.satoken.stp.StpUtil;
import cn.hutool.core.io.FileUtil;
import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import io.github.atengk.file.config.MinioProperties;
import io.github.atengk.file.domain.dto.FileInstantCheckDTO;
import io.github.atengk.file.domain.dto.FileInstantSaveDTO;
import io.github.atengk.file.domain.entity.FileInfo;
import io.github.atengk.file.domain.entity.FileStorage;
import io.github.atengk.file.domain.vo.FileDownloadUrlVO;
import io.github.atengk.file.domain.vo.FileInfoVO;
import io.github.atengk.file.domain.vo.FileInstantCheckVO;
import io.github.atengk.file.mapper.FileInfoMapper;
import io.github.atengk.file.mapper.FileStorageMapper;
import io.github.atengk.file.service.FileService;
import io.github.atengk.file.storage.MinioStorageService;
import io.github.atengk.file.util.FileHashUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.InputStream;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 文件业务实现
 *
 * @author Ateng
 * @since 2026-05-15
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class FileServiceImpl implements FileService {

    private static final String ACCESS_PRIVATE = "PRIVATE";
    private static final String ACCESS_PUBLIC = "PUBLIC";

    private final FileInfoMapper fileInfoMapper;
    private final FileStorageMapper fileStorageMapper;
    private final MinioStorageService minioStorageService;
    private final MinioProperties minioProperties;

    /**
     * 普通文件上传
     *
     * @param file 文件
     * @param accessType 访问类型
     * @param uploadSource 上传来源
     * @return 文件信息
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public FileInfoVO upload(MultipartFile file, String accessType, String uploadSource) {
        validateFile(file);

        Long userId = StpUtil.getLoginIdAsLong();
        String originalName = StrUtil.blankToDefault(file.getOriginalFilename(), "unknown");
        String fileExt = FileUtil.extName(originalName);
        String contentType = StrUtil.blankToDefault(file.getContentType(), "application/octet-stream");

        String fileHash;
        try (InputStream inputStream = file.getInputStream()) {
            fileHash = FileHashUtils.sha256(inputStream);
        } catch (Exception e) {
            log.error("计算文件Hash失败，文件名：{}", originalName, e);
            throw new RuntimeException("计算文件Hash失败");
        }

        FileStorage storage = getOrCreateStorage(file, fileHash, fileExt, contentType);
        FileInfo fileInfo = createFileInfo(storage, originalName, accessType, uploadSource, userId);

        log.info("普通文件上传成功，用户ID：{}，文件ID：{}，文件名：{}", userId, fileInfo.getId(), originalName);
        return toFileInfoVO(fileInfo);
    }

    /**
     * 秒传检测
     *
     * @param dto 秒传检测参数
     * @return 检测结果
     */
    @Override
    public FileInstantCheckVO instantCheck(FileInstantCheckDTO dto) {
        FileStorage storage = getStorageByHash(dto.getFileHash());
        boolean exists = storage != null;

        return FileInstantCheckVO.builder()
                .fileHash(dto.getFileHash())
                .exists(exists)
                .storageId(exists ? storage.getId() : null)
                .message(exists ? "文件已存在，可以秒传" : "文件不存在，需要上传")
                .build();
    }

    /**
     * 秒传保存
     *
     * @param dto 秒传保存参数
     * @return 文件信息
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public FileInfoVO instantSave(FileInstantSaveDTO dto) {
        Long userId = StpUtil.getLoginIdAsLong();
        FileStorage storage = getStorageByHash(dto.getFileHash());
        if (storage == null) {
            throw new IllegalArgumentException("文件不存在，无法秒传");
        }

        increaseRefCount(storage.getId());

        FileInfo fileInfo = createFileInfo(storage, dto.getOriginalName(), dto.getAccessType(), dto.getUploadSource(), userId);
        log.info("文件秒传成功，用户ID：{}，文件ID：{}，Hash：{}", userId, fileInfo.getId(), dto.getFileHash());
        return toFileInfoVO(fileInfo);
    }

    /**
     * 查询文件详情
     *
     * @param fileId 文件ID
     * @return 文件详情
     */
    @Override
    public FileInfoVO detail(Long fileId) {
        FileInfo fileInfo = getFileInfo(fileId);
        checkAccess(fileInfo);
        return toFileInfoVO(fileInfo);
    }

    /**
     * 获取文件临时下载链接
     *
     * @param fileId 文件ID
     * @return 下载链接
     */
    @Override
    public FileDownloadUrlVO getDownloadUrl(Long fileId) {
        FileInfo fileInfo = getFileInfo(fileId);
        checkAccess(fileInfo);

        FileStorage storage = fileStorageMapper.selectById(fileInfo.getStorageId());
        if (storage == null) {
            throw new IllegalArgumentException("物理文件不存在");
        }

        String url = minioStorageService.getPresignedUrl(storage.getObjectName());
        return FileDownloadUrlVO.builder()
                .fileId(fileInfo.getId())
                .originalName(fileInfo.getOriginalName())
                .url(url)
                .expireMinutes(minioProperties.getPresignedExpireMinutes())
                .build();
    }

    /**
     * 获取或创建物理文件记录
     *
     * @param file 文件
     * @param fileHash 文件Hash
     * @param fileExt 文件扩展名
     * @param contentType 文件类型
     * @return 物理文件记录
     */
    private FileStorage getOrCreateStorage(MultipartFile file, String fileHash, String fileExt, String contentType) {
        FileStorage existsStorage = getStorageByHash(fileHash);
        if (existsStorage != null) {
            increaseRefCount(existsStorage.getId());
            log.info("文件已存在，复用物理文件，Hash：{}", fileHash);
            return existsStorage;
        }

        String objectName = buildObjectName(fileHash, fileExt);
        try (InputStream inputStream = file.getInputStream()) {
            minioStorageService.uploadFile(objectName, inputStream, file.getSize(), contentType);
        } catch (Exception e) {
            log.error("上传文件到MinIO失败，Hash：{}", fileHash, e);
            throw new RuntimeException("上传文件失败");
        }

        FileStorage storage = new FileStorage();
        storage.setFileHash(fileHash);
        storage.setBucketName(minioProperties.getBucketName());
        storage.setObjectName(objectName);
        storage.setFileSize(file.getSize());
        storage.setContentType(contentType);
        storage.setFileExt(fileExt);
        storage.setRefCount(1);
        storage.setCreateTime(LocalDateTime.now());
        storage.setUpdateTime(LocalDateTime.now());
        storage.setDeleted(0);

        fileStorageMapper.insert(storage);
        return storage;
    }

    /**
     * 创建业务文件记录
     *
     * @param storage 物理文件
     * @param originalName 原始文件名
     * @param accessType 访问类型
     * @param uploadSource 上传来源
     * @param userId 用户ID
     * @return 业务文件记录
     */
    private FileInfo createFileInfo(FileStorage storage, String originalName, String accessType, String uploadSource, Long userId) {
        FileInfo fileInfo = new FileInfo();
        fileInfo.setStorageId(storage.getId());
        fileInfo.setOriginalName(StrUtil.blankToDefault(originalName, "unknown"));
        fileInfo.setFileSize(storage.getFileSize());
        fileInfo.setContentType(storage.getContentType());
        fileInfo.setFileExt(storage.getFileExt());
        fileInfo.setAccessType(normalizeAccessType(accessType));
        fileInfo.setUploaderId(userId);
        fileInfo.setUploadSource(uploadSource);
        fileInfo.setCreateTime(LocalDateTime.now());
        fileInfo.setUpdateTime(LocalDateTime.now());
        fileInfo.setDeleted(0);

        fileInfoMapper.insert(fileInfo);
        return fileInfo;
    }

    /**
     * 根据Hash查询物理文件
     *
     * @param fileHash 文件Hash
     * @return 物理文件
     */
    private FileStorage getStorageByHash(String fileHash) {
        return fileStorageMapper.selectOne(
                Wrappers.lambdaQuery(FileStorage.class)
                        .eq(FileStorage::getFileHash, fileHash)
                        .eq(FileStorage::getDeleted, 0)
                        .last("LIMIT 1")
        );
    }

    /**
     * 增加物理文件引用次数
     *
     * @param storageId 物理文件ID
     */
    private void increaseRefCount(Long storageId) {
        fileStorageMapper.update(
                null,
                Wrappers.lambdaUpdate(FileStorage.class)
                        .eq(FileStorage::getId, storageId)
                        .setSql("ref_count = ref_count + 1")
        );
    }

    /**
     * 查询业务文件
     *
     * @param fileId 文件ID
     * @return 业务文件
     */
    private FileInfo getFileInfo(Long fileId) {
        FileInfo fileInfo = fileInfoMapper.selectOne(
                Wrappers.lambdaQuery(FileInfo.class)
                        .eq(FileInfo::getId, fileId)
                        .eq(FileInfo::getDeleted, 0)
                        .last("LIMIT 1")
        );
        if (fileInfo == null) {
            throw new IllegalArgumentException("文件不存在");
        }
        return fileInfo;
    }

    /**
     * 校验文件访问权限
     *
     * @param fileInfo 文件信息
     */
    private void checkAccess(FileInfo fileInfo) {
        if (ACCESS_PUBLIC.equals(fileInfo.getAccessType())) {
            return;
        }

        Long userId = StpUtil.getLoginIdAsLong();
        if (!userId.equals(fileInfo.getUploaderId())) {
            log.warn("文件访问被拒绝，用户ID：{}，文件ID：{}", userId, fileInfo.getId());
            throw new IllegalArgumentException("无权访问该文件");
        }
    }

    /**
     * 校验上传文件
     *
     * @param file 文件
     */
    private void validateFile(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new IllegalArgumentException("上传文件不能为空");
        }
    }

    /**
     * 规范化访问类型
     *
     * @param accessType 访问类型
     * @return 访问类型
     */
    private String normalizeAccessType(String accessType) {
        String value = StrUtil.blankToDefault(accessType, ACCESS_PRIVATE).toUpperCase();
        if (!ACCESS_PRIVATE.equals(value) && !ACCESS_PUBLIC.equals(value)) {
            throw new IllegalArgumentException("文件访问类型不合法");
        }
        return value;
    }

    /**
     * 构建完整文件对象名称
     *
     * @param fileHash 文件Hash
     * @param fileExt 扩展名
     * @return 对象名称
     */
    private String buildObjectName(String fileHash, String fileExt) {
        LocalDate now = LocalDate.now();
        String suffix = StrUtil.isBlank(fileExt) ? "" : "." + fileExt;
        return StrUtil.format("files/{}/{}/{}/{}{}",
                now.getYear(),
                String.format("%02d", now.getMonthValue()),
                String.format("%02d", now.getDayOfMonth()),
                fileHash,
                suffix);
    }

    /**
     * 转换文件详情VO
     *
     * @param fileInfo 文件信息
     * @return 文件详情VO
     */
    private FileInfoVO toFileInfoVO(FileInfo fileInfo) {
        return FileInfoVO.builder()
                .id(fileInfo.getId())
                .originalName(fileInfo.getOriginalName())
                .fileSize(fileInfo.getFileSize())
                .fileSizeText(FileUtil.readableFileSize(fileInfo.getFileSize()))
                .contentType(fileInfo.getContentType())
                .fileExt(fileInfo.getFileExt())
                .accessType(fileInfo.getAccessType())
                .uploadSource(fileInfo.getUploadSource())
                .createTime(fileInfo.getCreateTime())
                .build();
    }
}
```

### 文件访问地址生成

文件访问地址不在上传时直接返回永久 URL，而是在用户需要下载时调用 `/api/files/{fileId}/download-url`。后端先校验权限，再生成 MinIO 临时链接。

文件位置：`src/main/java/io/github/atengk/file/domain/vo/FileDownloadUrlVO.java`

```java
package io.github.atengk.file.domain.vo;

import lombok.Builder;
import lombok.Data;

/**
 * 文件下载链接返回对象
 *
 * @author Ateng
 * @since 2026-05-15
 */
@Data
@Builder
public class FileDownloadUrlVO {

    private Long fileId;

    private String originalName;

    private String url;

    private Integer expireMinutes;
}
```

文件详情返回对象用于普通上传、秒传保存、分片合并后的统一响应。

文件位置：`src/main/java/io/github/atengk/file/domain/vo/FileInfoVO.java`

```java
package io.github.atengk.file.domain.vo;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 文件信息返回对象
 *
 * @author Ateng
 * @since 2026-05-15
 */
@Data
@Builder
public class FileInfoVO {

    private Long id;

    private String originalName;

    private Long fileSize;

    private String fileSizeText;

    private String contentType;

    private String fileExt;

    private String accessType;

    private String uploadSource;

    private LocalDateTime createTime;
}
```

接口调用示例：

```bash
curl -X POST "http://localhost:8080/api/files/upload" \
  -H "Authorization: Bearer your-token" \
  -F "file=@/Users/ateng/demo.pdf" \
  -F "accessType=PRIVATE" \
  -F "uploadSource=attachment"
```

获取临时下载链接：

```bash
curl -X GET "http://localhost:8080/api/files/10001/download-url" \
  -H "Authorization: Bearer your-token"
```

## 文件秒传实现

文件秒传的关键是文件唯一标识。前端在上传前计算完整文件的 SHA-256，然后调用后端检测接口。如果后端已经存在该 Hash 对应的物理文件，则不需要再上传文件内容，只需要创建当前用户的业务文件记录。

### 文件唯一标识计算

服务端普通上传时会重新计算 SHA-256，保证最终入库的 Hash 可信。大文件秒传场景下，前端计算 Hash 后传给后端，后端根据 `file_storage.file_hash` 判断物理文件是否存在。

后端计算 Hash 已经使用前面的 `FileHashUtils`，核心逻辑如下：

```java
try (InputStream inputStream = file.getInputStream()) {
    String fileHash = FileHashUtils.sha256(inputStream);
}
```

前端上传大文件时建议按分片读取文件内容计算 SHA-256，避免一次性读取整个大文件导致浏览器内存压力过大。后端只要求前端最终传入完整文件的 SHA-256 字符串即可。

### 秒传检测接口

秒传检测请求对象包含文件 Hash、原始文件名和文件大小。后端主要根据 `fileHash` 判断物理文件是否存在。

文件位置：`src/main/java/io/github/atengk/file/domain/dto/FileInstantCheckDTO.java`

```java
package io.github.atengk.file.domain.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

/**
 * 文件秒传检测请求对象
 *
 * @author Ateng
 * @since 2026-05-15
 */
@Data
public class FileInstantCheckDTO {

    @NotBlank(message = "文件Hash不能为空")
    private String fileHash;

    @NotBlank(message = "原始文件名不能为空")
    private String originalName;

    @NotNull(message = "文件大小不能为空")
    private Long fileSize;
}
```

文件位置：`src/main/java/io/github/atengk/file/domain/vo/FileInstantCheckVO.java`

```java
package io.github.atengk.file.domain.vo;

import lombok.Builder;
import lombok.Data;

/**
 * 文件秒传检测返回对象
 *
 * @author Ateng
 * @since 2026-05-15
 */
@Data
@Builder
public class FileInstantCheckVO {

    private String fileHash;

    private Boolean exists;

    private Long storageId;

    private String message;
}
```

秒传检测接口调用示例：

```bash
curl -X POST "http://localhost:8080/api/files/instant-check" \
  -H "Authorization: Bearer your-token" \
  -H "Content-Type: application/json" \
  -d '{
    "fileHash": "9f86d081884c7d659a2feaa0c55ad015a3bf4f1b2b0b822cd15d6c15b0f00a08",
    "originalName": "demo.pdf",
    "fileSize": 102400
  }'
```

响应示例：

```json
{
  "code": 200,
  "message": "操作成功",
  "data": {
    "fileHash": "9f86d081884c7d659a2feaa0c55ad015a3bf4f1b2b0b822cd15d6c15b0f00a08",
    "exists": true,
    "storageId": 10001,
    "message": "文件已存在，可以秒传"
  }
}
```

### 秒传业务处理

秒传保存请求对象用于在物理文件已经存在时创建业务文件记录。

文件位置：`src/main/java/io/github/atengk/file/domain/dto/FileInstantSaveDTO.java`

```java
package io.github.atengk.file.domain.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

/**
 * 文件秒传保存请求对象
 *
 * @author Ateng
 * @since 2026-05-15
 */
@Data
public class FileInstantSaveDTO {

    @NotBlank(message = "文件Hash不能为空")
    private String fileHash;

    @NotBlank(message = "原始文件名不能为空")
    private String originalName;

    @NotNull(message = "文件大小不能为空")
    private Long fileSize;

    private String contentType;

    private String accessType = "PRIVATE";

    private String uploadSource;
}
```

秒传保存核心逻辑已经在 `FileServiceImpl.instantSave` 中实现：

```java
FileStorage storage = getStorageByHash(dto.getFileHash());
if (storage == null) {
    throw new IllegalArgumentException("文件不存在，无法秒传");
}

increaseRefCount(storage.getId());
FileInfo fileInfo = createFileInfo(storage, dto.getOriginalName(), dto.getAccessType(), dto.getUploadSource(), userId);
```

秒传保存接口调用示例：

```bash
curl -X POST "http://localhost:8080/api/files/instant-save" \
  -H "Authorization: Bearer your-token" \
  -H "Content-Type: application/json" \
  -d '{
    "fileHash": "9f86d081884c7d659a2feaa0c55ad015a3bf4f1b2b0b822cd15d6c15b0f00a08",
    "originalName": "demo.pdf",
    "fileSize": 102400,
    "contentType": "application/pdf",
    "accessType": "PRIVATE",
    "uploadSource": "attachment"
  }'
```

## 分片上传实现

分片上传适合大文件上传。前端先计算完整文件 Hash，再调用初始化接口创建上传任务，然后查询已上传分片，只上传缺失分片。所有分片上传完成后，前端调用合并接口，由后端合并为完整文件。

### 分片初始化接口

初始化分片任务时，后端根据 `fileHash + uploaderId` 查询是否已经存在上传任务。如果存在，则直接返回任务信息和已上传分片；如果不存在，则创建新的上传任务。

文件位置：`src/main/java/io/github/atengk/file/domain/dto/FileChunkInitDTO.java`

```java
package io.github.atengk.file.domain.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

/**
 * 分片上传初始化请求对象
 *
 * @author Ateng
 * @since 2026-05-15
 */
@Data
public class FileChunkInitDTO {

    @NotBlank(message = "文件Hash不能为空")
    private String fileHash;

    @NotBlank(message = "原始文件名不能为空")
    private String originalName;

    @NotNull(message = "文件大小不能为空")
    private Long fileSize;

    private Long chunkSize;

    private String contentType;

    private String accessType = "PRIVATE";

    private String uploadSource;
}
```

文件位置：`src/main/java/io/github/atengk/file/domain/vo/FileChunkInitVO.java`

```java
package io.github.atengk.file.domain.vo;

import lombok.Builder;
import lombok.Data;

import java.util.List;

/**
 * 分片上传初始化返回对象
 *
 * @author Ateng
 * @since 2026-05-15
 */
@Data
@Builder
public class FileChunkInitVO {

    private Long taskId;

    private String fileHash;

    private Long chunkSize;

    private Integer totalChunks;

    private List<Integer> uploadedChunks;
}
```

分片业务接口定义如下。

文件位置：`src/main/java/io/github/atengk/file/service/FileChunkService.java`

```java
package io.github.atengk.file.service;

import io.github.atengk.file.domain.dto.FileChunkInitDTO;
import io.github.atengk.file.domain.dto.FileChunkMergeDTO;
import io.github.atengk.file.domain.vo.FileChunkInitVO;
import io.github.atengk.file.domain.vo.FileInfoVO;
import io.github.atengk.file.domain.vo.FileUploadedChunkVO;
import org.springframework.web.multipart.MultipartFile;

/**
 * 文件分片业务接口
 *
 * @author Ateng
 * @since 2026-05-15
 */
public interface FileChunkService {

    /**
     * 初始化分片上传任务
     *
     * @param dto 初始化参数
     * @return 初始化结果
     */
    FileChunkInitVO initChunkTask(FileChunkInitDTO dto);

    /**
     * 上传单个分片
     *
     * @param taskId 任务ID
     * @param chunkIndex 分片序号
     * @param file 分片文件
     */
    void uploadChunk(Long taskId, Integer chunkIndex, MultipartFile file);

    /**
     * 查询已上传分片
     *
     * @param taskId 任务ID
     * @return 已上传分片
     */
    FileUploadedChunkVO listUploadedChunks(Long taskId);

    /**
     * 合并分片
     *
     * @param dto 合并参数
     * @return 文件信息
     */
    FileInfoVO mergeChunks(FileChunkMergeDTO dto);
}
```

分片接口放在 `FileChunkController` 中。

文件位置：`src/main/java/io/github/atengk/file/controller/FileChunkController.java`

```java
package io.github.atengk.file.controller;

import io.github.atengk.file.domain.dto.FileChunkInitDTO;
import io.github.atengk.file.domain.dto.FileChunkMergeDTO;
import io.github.atengk.file.domain.vo.FileChunkInitVO;
import io.github.atengk.file.domain.vo.FileInfoVO;
import io.github.atengk.file.domain.vo.FileUploadedChunkVO;
import io.github.atengk.file.service.FileChunkService;
import io.github.atengk.system.common.R;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

/**
 * 文件分片上传接口
 *
 * @author Ateng
 * @since 2026-05-15
 */
@RestController
@RequestMapping("/api/files/chunk")
@RequiredArgsConstructor
public class FileChunkController {

    private final FileChunkService fileChunkService;

    /**
     * 初始化分片上传任务
     *
     * @param dto 初始化参数
     * @return 初始化结果
     */
    @PostMapping("/init")
    public R<FileChunkInitVO> init(@Valid @RequestBody FileChunkInitDTO dto) {
        return R.ok(fileChunkService.initChunkTask(dto));
    }

    /**
     * 上传单个分片
     *
     * @param taskId 任务ID
     * @param chunkIndex 分片序号
     * @param file 分片文件
     * @return 上传结果
     */
    @PostMapping(value = "/upload", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public R<Boolean> upload(@RequestParam Long taskId,
                             @RequestParam Integer chunkIndex,
                             @RequestPart("file") MultipartFile file) {
        fileChunkService.uploadChunk(taskId, chunkIndex, file);
        return R.ok(Boolean.TRUE);
    }

    /**
     * 查询已上传分片
     *
     * @param taskId 任务ID
     * @return 已上传分片
     */
    @GetMapping("/uploaded")
    public R<FileUploadedChunkVO> uploaded(@RequestParam Long taskId) {
        return R.ok(fileChunkService.listUploadedChunks(taskId));
    }

    /**
     * 合并分片
     *
     * @param dto 合并参数
     * @return 文件信息
     */
    @PostMapping("/merge")
    public R<FileInfoVO> merge(@Valid @RequestBody FileChunkMergeDTO dto) {
        return R.ok(fileChunkService.mergeChunks(dto));
    }
}
```

### 分片上传接口

分片上传接口需要保证幂等。如果同一个 `taskId + chunkIndex` 已经上传过，则直接返回成功，避免前端重试导致重复写入。

分片上传、查询和合并的返回对象如下。

文件位置：`src/main/java/io/github/atengk/file/domain/vo/FileUploadedChunkVO.java`

```java
package io.github.atengk.file.domain.vo;

import lombok.Builder;
import lombok.Data;

import java.util.List;

/**
 * 已上传分片返回对象
 *
 * @author Ateng
 * @since 2026-05-15
 */
@Data
@Builder
public class FileUploadedChunkVO {

    private Long taskId;

    private List<Integer> uploadedChunks;
}
```

文件位置：`src/main/java/io/github/atengk/file/domain/dto/FileChunkMergeDTO.java`

```java
package io.github.atengk.file.domain.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

/**
 * 分片合并请求对象
 *
 * @author Ateng
 * @since 2026-05-15
 */
@Data
public class FileChunkMergeDTO {

    @NotNull(message = "任务ID不能为空")
    private Long taskId;

    @NotBlank(message = "文件Hash不能为空")
    private String fileHash;

    @NotBlank(message = "原始文件名不能为空")
    private String originalName;

    private String accessType = "PRIVATE";

    private String uploadSource;
}
```

分片上传业务实现如下，核心点包括任务初始化、分片幂等上传、Redis 缓存已上传分片、合并锁、MinIO 合并对象、清理临时分片。

文件位置：`src/main/java/io/github/atengk/file/service/impl/FileChunkServiceImpl.java`

```java
package io.github.atengk.file.service.impl;

import cn.dev33.satoken.stp.StpUtil;
import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.io.FileUtil;
import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import io.github.atengk.file.config.MinioProperties;
import io.github.atengk.file.domain.dto.FileChunkInitDTO;
import io.github.atengk.file.domain.dto.FileChunkMergeDTO;
import io.github.atengk.file.domain.entity.FileChunkPart;
import io.github.atengk.file.domain.entity.FileChunkTask;
import io.github.atengk.file.domain.entity.FileInfo;
import io.github.atengk.file.domain.entity.FileStorage;
import io.github.atengk.file.domain.vo.FileChunkInitVO;
import io.github.atengk.file.domain.vo.FileInfoVO;
import io.github.atengk.file.domain.vo.FileUploadedChunkVO;
import io.github.atengk.file.mapper.FileChunkPartMapper;
import io.github.atengk.file.mapper.FileChunkTaskMapper;
import io.github.atengk.file.mapper.FileInfoMapper;
import io.github.atengk.file.mapper.FileStorageMapper;
import io.github.atengk.file.service.FileChunkService;
import io.github.atengk.file.storage.MinioStorageService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.InputStream;
import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.List;
import java.util.Set;

/**
 * 文件分片业务实现
 *
 * @author Ateng
 * @since 2026-05-15
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class FileChunkServiceImpl implements FileChunkService {

    private static final String STATUS_UPLOADING = "UPLOADING";
    private static final String STATUS_MERGED = "MERGED";

    private static final String ACCESS_PRIVATE = "PRIVATE";
    private static final String ACCESS_PUBLIC = "PUBLIC";

    private static final String REDIS_UPLOADED_CHUNKS = "file:chunk:uploaded:";
    private static final String REDIS_MERGE_LOCK = "file:chunk:lock:";

    private final FileChunkTaskMapper fileChunkTaskMapper;
    private final FileChunkPartMapper fileChunkPartMapper;
    private final FileStorageMapper fileStorageMapper;
    private final FileInfoMapper fileInfoMapper;
    private final MinioStorageService minioStorageService;
    private final MinioProperties minioProperties;
    private final StringRedisTemplate stringRedisTemplate;

    /**
     * 初始化分片上传任务
     *
     * @param dto 初始化参数
     * @return 初始化结果
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public FileChunkInitVO initChunkTask(FileChunkInitDTO dto) {
        Long userId = StpUtil.getLoginIdAsLong();
        Long chunkSize = dto.getChunkSize() == null ? minioProperties.getChunkSize() : dto.getChunkSize();
        int totalChunks = Math.toIntExact((dto.getFileSize() + chunkSize - 1) / chunkSize);

        FileChunkTask existsTask = fileChunkTaskMapper.selectOne(
                Wrappers.lambdaQuery(FileChunkTask.class)
                        .eq(FileChunkTask::getFileHash, dto.getFileHash())
                        .eq(FileChunkTask::getUploaderId, userId)
                        .eq(FileChunkTask::getDeleted, 0)
                        .last("LIMIT 1")
        );

        if (existsTask != null) {
            log.info("分片任务已存在，直接返回，任务ID：{}", existsTask.getId());
            return FileChunkInitVO.builder()
                    .taskId(existsTask.getId())
                    .fileHash(existsTask.getFileHash())
                    .chunkSize(existsTask.getChunkSize())
                    .totalChunks(existsTask.getTotalChunks())
                    .uploadedChunks(listUploadedChunks(existsTask.getId()).getUploadedChunks())
                    .build();
        }

        FileChunkTask task = new FileChunkTask();
        task.setFileHash(dto.getFileHash());
        task.setOriginalName(dto.getOriginalName());
        task.setFileSize(dto.getFileSize());
        task.setChunkSize(chunkSize);
        task.setTotalChunks(totalChunks);
        task.setContentType(StrUtil.blankToDefault(dto.getContentType(), "application/octet-stream"));
        task.setFileExt(FileUtil.extName(dto.getOriginalName()));
        task.setStatus(STATUS_UPLOADING);
        task.setUploaderId(userId);
        task.setCreateTime(LocalDateTime.now());
        task.setUpdateTime(LocalDateTime.now());
        task.setDeleted(0);

        fileChunkTaskMapper.insert(task);
        log.info("初始化分片任务成功，用户ID：{}，任务ID：{}，总分片数：{}", userId, task.getId(), totalChunks);

        return FileChunkInitVO.builder()
                .taskId(task.getId())
                .fileHash(task.getFileHash())
                .chunkSize(task.getChunkSize())
                .totalChunks(task.getTotalChunks())
                .uploadedChunks(List.of())
                .build();
    }

    /**
     * 上传单个分片
     *
     * @param taskId 任务ID
     * @param chunkIndex 分片序号
     * @param file 分片文件
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void uploadChunk(Long taskId, Integer chunkIndex, MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new IllegalArgumentException("分片文件不能为空");
        }

        FileChunkTask task = getTask(taskId);
        checkTaskOwner(task);
        checkChunkIndex(task, chunkIndex);

        if (!STATUS_UPLOADING.equals(task.getStatus())) {
            throw new IllegalArgumentException("当前分片任务不允许继续上传");
        }

        FileChunkPart existsPart = getChunkPart(taskId, chunkIndex);
        if (existsPart != null) {
            log.info("分片已存在，跳过重复上传，任务ID：{}，分片序号：{}", taskId, chunkIndex);
            cacheUploadedChunk(taskId, chunkIndex);
            return;
        }

        String objectName = buildChunkObjectName(task.getFileHash(), chunkIndex);
        try (InputStream inputStream = file.getInputStream()) {
            minioStorageService.uploadChunk(objectName, inputStream, file.getSize());
        } catch (Exception e) {
            log.error("上传分片失败，任务ID：{}，分片序号：{}", taskId, chunkIndex, e);
            throw new RuntimeException("上传分片失败");
        }

        FileChunkPart part = new FileChunkPart();
        part.setTaskId(taskId);
        part.setFileHash(task.getFileHash());
        part.setChunkIndex(chunkIndex);
        part.setChunkSize(file.getSize());
        part.setBucketName(minioProperties.getBucketName());
        part.setObjectName(objectName);
        part.setCreateTime(LocalDateTime.now());
        part.setDeleted(0);

        fileChunkPartMapper.insert(part);
        cacheUploadedChunk(taskId, chunkIndex);

        log.info("分片上传成功，任务ID：{}，分片序号：{}", taskId, chunkIndex);
    }

    /**
     * 查询已上传分片
     *
     * @param taskId 任务ID
     * @return 已上传分片
     */
    @Override
    public FileUploadedChunkVO listUploadedChunks(Long taskId) {
        FileChunkTask task = getTask(taskId);
        checkTaskOwner(task);

        String redisKey = REDIS_UPLOADED_CHUNKS + taskId;
        Set<String> cachedChunks = stringRedisTemplate.opsForSet().members(redisKey);

        if (CollUtil.isNotEmpty(cachedChunks)) {
            List<Integer> uploadedChunks = cachedChunks.stream()
                    .map(Integer::valueOf)
                    .sorted()
                    .toList();

            return FileUploadedChunkVO.builder()
                    .taskId(taskId)
                    .uploadedChunks(uploadedChunks)
                    .build();
        }

        List<FileChunkPart> parts = fileChunkPartMapper.selectList(
                Wrappers.lambdaQuery(FileChunkPart.class)
                        .eq(FileChunkPart::getTaskId, taskId)
                        .eq(FileChunkPart::getDeleted, 0)
                        .orderByAsc(FileChunkPart::getChunkIndex)
        );

        List<Integer> uploadedChunks = parts.stream()
                .map(FileChunkPart::getChunkIndex)
                .toList();

        if (CollUtil.isNotEmpty(uploadedChunks)) {
            String[] values = uploadedChunks.stream()
                    .map(String::valueOf)
                    .toArray(String[]::new);
            stringRedisTemplate.opsForSet().add(redisKey, values);
        }

        return FileUploadedChunkVO.builder()
                .taskId(taskId)
                .uploadedChunks(uploadedChunks)
                .build();
    }

    /**
     * 合并分片
     *
     * @param dto 合并参数
     * @return 文件信息
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public FileInfoVO mergeChunks(FileChunkMergeDTO dto) {
        Long userId = StpUtil.getLoginIdAsLong();
        FileChunkTask task = getTask(dto.getTaskId());
        checkTaskOwner(task);

        if (!StrUtil.equals(task.getFileHash(), dto.getFileHash())) {
            throw new IllegalArgumentException("文件Hash与任务不匹配");
        }
        if (!STATUS_UPLOADING.equals(task.getStatus())) {
            throw new IllegalArgumentException("当前任务状态不允许合并");
        }

        String lockKey = REDIS_MERGE_LOCK + task.getFileHash();
        Boolean locked = stringRedisTemplate.opsForValue()
                .setIfAbsent(lockKey, String.valueOf(task.getId()), Duration.ofMinutes(10));

        if (!Boolean.TRUE.equals(locked)) {
            throw new IllegalArgumentException("文件正在合并中，请勿重复提交");
        }

        try {
            FileStorage existsStorage = getStorageByHash(task.getFileHash());
            if (existsStorage != null) {
                FileInfo fileInfo = createFileInfo(existsStorage, dto.getOriginalName(), dto.getAccessType(), dto.getUploadSource(), userId);
                updateTaskMerged(task.getId());
                cleanChunkObjects(task.getId());
                log.info("合并时发现物理文件已存在，直接复用，任务ID：{}，文件ID：{}", task.getId(), fileInfo.getId());
                return toFileInfoVO(fileInfo);
            }

            List<FileChunkPart> parts = fileChunkPartMapper.selectList(
                    Wrappers.lambdaQuery(FileChunkPart.class)
                            .eq(FileChunkPart::getTaskId, task.getId())
                            .eq(FileChunkPart::getDeleted, 0)
                            .orderByAsc(FileChunkPart::getChunkIndex)
            );

            if (parts.size() != task.getTotalChunks()) {
                throw new IllegalArgumentException("分片未上传完整，无法合并");
            }

            List<String> sourceObjectNames = parts.stream()
                    .sorted(Comparator.comparing(FileChunkPart::getChunkIndex))
                    .map(FileChunkPart::getObjectName)
                    .toList();

            String targetObjectName = buildFileObjectName(task.getFileHash(), task.getFileExt());
            minioStorageService.composeObject(targetObjectName, sourceObjectNames);

            FileStorage storage = new FileStorage();
            storage.setFileHash(task.getFileHash());
            storage.setBucketName(minioProperties.getBucketName());
            storage.setObjectName(targetObjectName);
            storage.setFileSize(task.getFileSize());
            storage.setContentType(task.getContentType());
            storage.setFileExt(task.getFileExt());
            storage.setRefCount(1);
            storage.setCreateTime(LocalDateTime.now());
            storage.setUpdateTime(LocalDateTime.now());
            storage.setDeleted(0);
            fileStorageMapper.insert(storage);

            FileInfo fileInfo = createFileInfo(storage, dto.getOriginalName(), dto.getAccessType(), dto.getUploadSource(), userId);
            updateTaskMerged(task.getId());
            cleanChunkObjects(task.getId());

            log.info("分片合并成功，任务ID：{}，文件ID：{}", task.getId(), fileInfo.getId());
            return toFileInfoVO(fileInfo);
        } finally {
            stringRedisTemplate.delete(lockKey);
        }
    }

    /**
     * 获取分片任务
     *
     * @param taskId 任务ID
     * @return 分片任务
     */
    private FileChunkTask getTask(Long taskId) {
        FileChunkTask task = fileChunkTaskMapper.selectOne(
                Wrappers.lambdaQuery(FileChunkTask.class)
                        .eq(FileChunkTask::getId, taskId)
                        .eq(FileChunkTask::getDeleted, 0)
                        .last("LIMIT 1")
        );
        if (task == null) {
            throw new IllegalArgumentException("分片任务不存在");
        }
        return task;
    }

    /**
     * 校验任务归属
     *
     * @param task 分片任务
     */
    private void checkTaskOwner(FileChunkTask task) {
        Long userId = StpUtil.getLoginIdAsLong();
        if (!userId.equals(task.getUploaderId())) {
            log.warn("分片任务访问被拒绝，用户ID：{}，任务ID：{}", userId, task.getId());
            throw new IllegalArgumentException("无权操作该分片任务");
        }
    }

    /**
     * 校验分片序号
     *
     * @param task 分片任务
     * @param chunkIndex 分片序号
     */
    private void checkChunkIndex(FileChunkTask task, Integer chunkIndex) {
        if (chunkIndex == null || chunkIndex < 0 || chunkIndex >= task.getTotalChunks()) {
            throw new IllegalArgumentException("分片序号不合法");
        }
    }

    /**
     * 查询分片记录
     *
     * @param taskId 任务ID
     * @param chunkIndex 分片序号
     * @return 分片记录
     */
    private FileChunkPart getChunkPart(Long taskId, Integer chunkIndex) {
        return fileChunkPartMapper.selectOne(
                Wrappers.lambdaQuery(FileChunkPart.class)
                        .eq(FileChunkPart::getTaskId, taskId)
                        .eq(FileChunkPart::getChunkIndex, chunkIndex)
                        .eq(FileChunkPart::getDeleted, 0)
                        .last("LIMIT 1")
        );
    }

    /**
     * 缓存已上传分片
     *
     * @param taskId 任务ID
     * @param chunkIndex 分片序号
     */
    private void cacheUploadedChunk(Long taskId, Integer chunkIndex) {
        stringRedisTemplate.opsForSet().add(REDIS_UPLOADED_CHUNKS + taskId, String.valueOf(chunkIndex));
    }

    /**
     * 根据Hash查询物理文件
     *
     * @param fileHash 文件Hash
     * @return 物理文件
     */
    private FileStorage getStorageByHash(String fileHash) {
        return fileStorageMapper.selectOne(
                Wrappers.lambdaQuery(FileStorage.class)
                        .eq(FileStorage::getFileHash, fileHash)
                        .eq(FileStorage::getDeleted, 0)
                        .last("LIMIT 1")
        );
    }

    /**
     * 创建业务文件记录
     *
     * @param storage 物理文件
     * @param originalName 原始文件名
     * @param accessType 访问类型
     * @param uploadSource 上传来源
     * @param userId 用户ID
     * @return 业务文件记录
     */
    private FileInfo createFileInfo(FileStorage storage, String originalName, String accessType, String uploadSource, Long userId) {
        FileInfo fileInfo = new FileInfo();
        fileInfo.setStorageId(storage.getId());
        fileInfo.setOriginalName(StrUtil.blankToDefault(originalName, "unknown"));
        fileInfo.setFileSize(storage.getFileSize());
        fileInfo.setContentType(storage.getContentType());
        fileInfo.setFileExt(storage.getFileExt());
        fileInfo.setAccessType(normalizeAccessType(accessType));
        fileInfo.setUploaderId(userId);
        fileInfo.setUploadSource(uploadSource);
        fileInfo.setCreateTime(LocalDateTime.now());
        fileInfo.setUpdateTime(LocalDateTime.now());
        fileInfo.setDeleted(0);

        fileInfoMapper.insert(fileInfo);
        return fileInfo;
    }

    /**
     * 更新任务为已合并
     *
     * @param taskId 任务ID
     */
    private void updateTaskMerged(Long taskId) {
        fileChunkTaskMapper.update(
                null,
                Wrappers.lambdaUpdate(FileChunkTask.class)
                        .eq(FileChunkTask::getId, taskId)
                        .set(FileChunkTask::getStatus, STATUS_MERGED)
                        .set(FileChunkTask::getUpdateTime, LocalDateTime.now())
        );
    }

    /**
     * 清理临时分片对象
     *
     * @param taskId 任务ID
     */
    private void cleanChunkObjects(Long taskId) {
        List<FileChunkPart> parts = fileChunkPartMapper.selectList(
                Wrappers.lambdaQuery(FileChunkPart.class)
                        .eq(FileChunkPart::getTaskId, taskId)
                        .eq(FileChunkPart::getDeleted, 0)
        );

        List<String> objectNames = parts.stream()
                .map(FileChunkPart::getObjectName)
                .toList();

        minioStorageService.removeObjects(objectNames);
        stringRedisTemplate.delete(REDIS_UPLOADED_CHUNKS + taskId);
    }

    /**
     * 构建分片对象名称
     *
     * @param fileHash 文件Hash
     * @param chunkIndex 分片序号
     * @return 分片对象名称
     */
    private String buildChunkObjectName(String fileHash, Integer chunkIndex) {
        return StrUtil.format("chunks/{}/{}", fileHash, chunkIndex);
    }

    /**
     * 构建完整文件对象名称
     *
     * @param fileHash 文件Hash
     * @param fileExt 扩展名
     * @return 完整文件对象名称
     */
    private String buildFileObjectName(String fileHash, String fileExt) {
        LocalDate now = LocalDate.now();
        String suffix = StrUtil.isBlank(fileExt) ? "" : "." + fileExt;
        return StrUtil.format("files/{}/{}/{}/{}{}",
                now.getYear(),
                String.format("%02d", now.getMonthValue()),
                String.format("%02d", now.getDayOfMonth()),
                fileHash,
                suffix);
    }

    /**
     * 规范化访问类型
     *
     * @param accessType 访问类型
     * @return 访问类型
     */
    private String normalizeAccessType(String accessType) {
        String value = StrUtil.blankToDefault(accessType, ACCESS_PRIVATE).toUpperCase();
        if (!ACCESS_PRIVATE.equals(value) && !ACCESS_PUBLIC.equals(value)) {
            throw new IllegalArgumentException("文件访问类型不合法");
        }
        return value;
    }

    /**
     * 转换文件信息VO
     *
     * @param fileInfo 文件信息
     * @return 文件信息VO
     */
    private FileInfoVO toFileInfoVO(FileInfo fileInfo) {
        return FileInfoVO.builder()
                .id(fileInfo.getId())
                .originalName(fileInfo.getOriginalName())
                .fileSize(fileInfo.getFileSize())
                .fileSizeText(FileUtil.readableFileSize(fileInfo.getFileSize()))
                .contentType(fileInfo.getContentType())
                .fileExt(fileInfo.getFileExt())
                .accessType(fileInfo.getAccessType())
                .uploadSource(fileInfo.getUploadSource())
                .createTime(fileInfo.getCreateTime())
                .build();
    }
}
```

### 已上传分片查询接口

已上传分片查询用于断点续传。前端上传中断后，可以重新调用该接口，拿到已经上传成功的分片序号，只补传缺失分片。

接口调用示例：

```bash
curl -X GET "http://localhost:8080/api/files/chunk/uploaded?taskId=20001" \
  -H "Authorization: Bearer your-token"
```

响应示例：

```json
{
  "code": 200,
  "message": "操作成功",
  "data": {
    "taskId": 20001,
    "uploadedChunks": [0, 1, 2, 5, 6]
  }
}
```

前端处理逻辑：

```text
全部分片序号：[0, 1, 2, 3, 4, 5, 6]
已上传分片：[0, 1, 2, 5, 6]
需要补传分片：[3, 4]
```

### 分片合并接口

分片合并接口在所有分片上传完成后调用。后端会校验任务归属、任务状态、分片数量，然后使用 MinIO 的 compose 能力将多个分片对象合并为完整对象。

初始化分片任务：

```bash
curl -X POST "http://localhost:8080/api/files/chunk/init" \
  -H "Authorization: Bearer your-token" \
  -H "Content-Type: application/json" \
  -d '{
    "fileHash": "9f86d081884c7d659a2feaa0c55ad015a3bf4f1b2b0b822cd15d6c15b0f00a08",
    "originalName": "big-video.mp4",
    "fileSize": 524288000,
    "chunkSize": 5242880,
    "contentType": "video/mp4",
    "accessType": "PRIVATE",
    "uploadSource": "material"
  }'
```

上传单个分片：

```bash
curl -X POST "http://localhost:8080/api/files/chunk/upload" \
  -H "Authorization: Bearer your-token" \
  -F "taskId=20001" \
  -F "chunkIndex=0" \
  -F "file=@/Users/ateng/chunks/0"
```

合并分片：

```bash
curl -X POST "http://localhost:8080/api/files/chunk/merge" \
  -H "Authorization: Bearer your-token" \
  -H "Content-Type: application/json" \
  -d '{
    "taskId": 20001,
    "fileHash": "9f86d081884c7d659a2feaa0c55ad015a3bf4f1b2b0b822cd15d6c15b0f00a08",
    "originalName": "big-video.mp4",
    "accessType": "PRIVATE",
    "uploadSource": "material"
  }'
```

合并成功后会返回普通文件信息：

```json
{
  "code": 200,
  "message": "操作成功",
  "data": {
    "id": 30001,
    "originalName": "big-video.mp4",
    "fileSize": 524288000,
    "fileSizeText": "500 MB",
    "contentType": "video/mp4",
    "fileExt": "mp4",
    "accessType": "PRIVATE",
    "uploadSource": "material",
    "createTime": "2026-05-15T10:30:00"
  }
}
```

完整上传链路建议按下面顺序调用：

```text
1. 前端计算完整文件 SHA-256
2. 调用 /api/files/instant-check
3. 如果 exists=true，调用 /api/files/instant-save
4. 如果 exists=false，调用 /api/files/chunk/init
5. 调用 /api/files/chunk/uploaded 获取已上传分片
6. 只上传缺失分片到 /api/files/chunk/upload
7. 所有分片上传完成后，调用 /api/files/chunk/merge
8. 需要下载时，调用 /api/files/{fileId}/download-url
```

## 权限访问实现

文件默认按照私有资源处理，不直接暴露 MinIO 永久访问地址。前端访问文件时，需要先请求后端接口，由后端校验当前用户是否有权限访问，权限通过后再生成临时访问链接或通过后端代理下载。

### 文件权限模型

本案例先实现最小可用权限模型，满足大多数后台系统的文件访问需求。

| 权限类型  | 说明                       |
| --------- | -------------------------- |
| `PRIVATE` | 私有文件，仅上传人可以访问 |
| `PUBLIC`  | 公开文件，登录用户可以访问 |

后续可以在此基础上扩展部门权限、角色权限、租户隔离、业务单据权限、分享链接权限等。

访问类型枚举用于统一管理文件权限值，避免业务代码中到处硬编码字符串。

文件位置：`src/main/java/io/github/atengk/file/enums/FileAccessTypeEnum.java`

```java
package io.github.atengk.file.enums;

import lombok.Getter;

/**
 * 文件访问类型枚举
 *
 * @author Ateng
 * @since 2026-05-15
 */
@Getter
public enum FileAccessTypeEnum {

    PRIVATE("PRIVATE", "私有"),

    PUBLIC("PUBLIC", "公开");

    private final String code;

    private final String desc;

    FileAccessTypeEnum(String code, String desc) {
        this.code = code;
        this.desc = desc;
    }
}
```

### 私有文件访问校验

文件访问校验建议单独封装成一个服务，避免普通上传、文件详情、下载链接、代理下载等接口重复写权限判断逻辑。

文件位置：`src/main/java/io/github/atengk/file/service/FileAccessService.java`

```java
package io.github.atengk.file.service;

import io.github.atengk.file.domain.entity.FileInfo;

/**
 * 文件访问权限服务
 *
 * @author Ateng
 * @since 2026-05-15
 */
public interface FileAccessService {

    /**
     * 校验当前用户是否可以访问文件
     *
     * @param fileInfo 文件信息
     */
    void checkAccess(FileInfo fileInfo);
}
```

文件访问权限实现类用于校验公开文件和私有文件访问规则。

文件位置：`src/main/java/io/github/atengk/file/service/impl/FileAccessServiceImpl.java`

```java
package io.github.atengk.file.service.impl;

import cn.dev33.satoken.stp.StpUtil;
import cn.hutool.core.util.ObjUtil;
import io.github.atengk.file.domain.entity.FileInfo;
import io.github.atengk.file.enums.FileAccessTypeEnum;
import io.github.atengk.file.service.FileAccessService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

/**
 * 文件访问权限服务实现
 *
 * @author Ateng
 * @since 2026-05-15
 */
@Slf4j
@Service
public class FileAccessServiceImpl implements FileAccessService {

    /**
     * 校验当前用户是否可以访问文件
     *
     * @param fileInfo 文件信息
     */
    @Override
    public void checkAccess(FileInfo fileInfo) {
        if (fileInfo == null) {
            throw new IllegalArgumentException("文件不存在");
        }

        if (FileAccessTypeEnum.PUBLIC.getCode().equals(fileInfo.getAccessType())) {
            return;
        }

        Long userId = StpUtil.getLoginIdAsLong();
        if (ObjUtil.notEqual(userId, fileInfo.getUploaderId())) {
            log.warn("文件访问被拒绝，当前用户ID：{}，文件ID：{}，上传人ID：{}",
                    userId, fileInfo.getId(), fileInfo.getUploaderId());
            throw new IllegalArgumentException("无权访问该文件");
        }
    }
}
```

在 `FileServiceImpl` 中注入 `FileAccessService`，将原来的私有 `checkAccess` 方法替换为统一服务调用。

```java
private final FileAccessService fileAccessService;
```

查询文件详情时校验权限：

```java
@Override
public FileInfoVO detail(Long fileId) {
    FileInfo fileInfo = getFileInfo(fileId);
    fileAccessService.checkAccess(fileInfo);
    return toFileInfoVO(fileInfo);
}
```

### 临时访问链接生成

临时访问链接适合前端直接下载文件。后端只返回短时间有效的 MinIO 预签名 URL，不暴露永久地址。

`MinioStorageService` 中已经封装了预签名链接生成方法：

```java
public String getPresignedUrl(String objectName) {
    try {
        return minioClient.getPresignedObjectUrl(
                GetPresignedObjectUrlArgs.builder()
                        .bucket(minioProperties.getBucketName())
                        .object(objectName)
                        .method(Method.GET)
                        .expiry(minioProperties.getPresignedExpireMinutes(), TimeUnit.MINUTES)
                        .build()
        );
    } catch (Exception e) {
        log.error("生成MinIO临时访问链接失败，对象：{}", objectName, e);
        throw new RuntimeException("生成文件访问链接失败");
    }
}
```

文件业务层获取下载链接时，必须先校验权限，再查询物理文件，最后生成临时链接。

文件位置：`src/main/java/io/github/atengk/file/service/impl/FileServiceImpl.java`

```java
@Override
public FileDownloadUrlVO getDownloadUrl(Long fileId) {
    FileInfo fileInfo = getFileInfo(fileId);
    fileAccessService.checkAccess(fileInfo);

    FileStorage storage = fileStorageMapper.selectById(fileInfo.getStorageId());
    if (storage == null) {
        throw new IllegalArgumentException("物理文件不存在");
    }

    String url = minioStorageService.getPresignedUrl(storage.getObjectName());
    return FileDownloadUrlVO.builder()
            .fileId(fileInfo.getId())
            .originalName(fileInfo.getOriginalName())
            .url(url)
            .expireMinutes(minioProperties.getPresignedExpireMinutes())
            .build();
}
```

临时下载链接接口：

```text
GET /api/files/{fileId}/download-url
```

返回示例：

```json
{
  "code": 200,
  "message": "操作成功",
  "data": {
    "fileId": 30001,
    "originalName": "demo.pdf",
    "url": "http://localhost:9000/file-bucket/files/2026/05/15/xxx.pdf?X-Amz-Algorithm=...",
    "expireMinutes": 10
  }
}
```

### 文件下载接口

如果业务需要审计下载日志、限制下载频率、隐藏 MinIO 地址，或者需要统一通过后端下载，可以增加后端代理下载接口。

先在 `MinioStorageService` 中增加读取对象方法。

文件位置：`src/main/java/io/github/atengk/file/storage/MinioStorageService.java`

```java
/**
 * 读取MinIO对象流
 *
 * @param objectName 对象名称
 * @return 对象输入流
 */
public InputStream getObject(String objectName) {
    try {
        return minioClient.getObject(
                GetObjectArgs.builder()
                        .bucket(minioProperties.getBucketName())
                        .object(objectName)
                        .build()
        );
    } catch (Exception e) {
        log.error("读取MinIO对象失败，对象：{}", objectName, e);
        throw new RuntimeException("读取文件失败");
    }
}
```

然后在 `FileService` 中增加代理下载方法。

文件位置：`src/main/java/io/github/atengk/file/service/FileService.java`

```java
/**
 * 后端代理下载文件
 *
 * @param fileId 文件ID
 * @param response HTTP响应
 */
void download(Long fileId, HttpServletResponse response);
```

需要引入：

```java
import jakarta.servlet.http.HttpServletResponse;
```

`FileServiceImpl` 中实现代理下载。该方法会先校验权限，再从 MinIO 读取文件流并写入响应。

文件位置：`src/main/java/io/github/atengk/file/service/impl/FileServiceImpl.java`

```java
@Override
public void download(Long fileId, HttpServletResponse response) {
    FileInfo fileInfo = getFileInfo(fileId);
    fileAccessService.checkAccess(fileInfo);

    FileStorage storage = fileStorageMapper.selectById(fileInfo.getStorageId());
    if (storage == null) {
        throw new IllegalArgumentException("物理文件不存在");
    }

    String contentType = StrUtil.blankToDefault(fileInfo.getContentType(), "application/octet-stream");
    String encodedName = encodeFilename(fileInfo.getOriginalName());

    response.setContentType(contentType);
    response.setHeader("Content-Disposition", "attachment; filename*=UTF-8''" + encodedName);
    response.setHeader("Content-Length", String.valueOf(fileInfo.getFileSize()));

    try (InputStream inputStream = minioStorageService.getObject(storage.getObjectName())) {
        cn.hutool.core.io.IoUtil.copy(inputStream, response.getOutputStream());
        response.flushBuffer();
        log.info("文件下载成功，文件ID：{}，文件名：{}", fileInfo.getId(), fileInfo.getOriginalName());
    } catch (Exception e) {
        log.error("文件下载失败，文件ID：{}", fileId, e);
        throw new RuntimeException("文件下载失败");
    }
}

/**
 * 编码下载文件名
 *
 * @param filename 文件名
 * @return 编码后的文件名
 */
private String encodeFilename(String filename) {
    try {
        return java.net.URLEncoder.encode(filename, java.nio.charset.StandardCharsets.UTF_8)
                .replace("+", "%20");
    } catch (Exception e) {
        return "download";
    }
}
```

最后在 `FileController` 中增加下载接口。

文件位置：`src/main/java/io/github/atengk/file/controller/FileController.java`

```java
/**
 * 后端代理下载文件
 *
 * @param fileId 文件ID
 * @param response HTTP响应
 */
@GetMapping("/{fileId}/download")
public void download(@PathVariable Long fileId, jakarta.servlet.http.HttpServletResponse response) {
    fileService.download(fileId, response);
}
```

两种下载方式建议如下：

| 方式         | 适用场景                                       |
| ------------ | ---------------------------------------------- |
| 临时链接下载 | 文件较大、下载并发较高、希望减少后端流量压力   |
| 后端代理下载 | 需要审计、限流、隐藏对象存储地址、统一下载出口 |

## 接口测试

本节使用 `curl` 进行接口验证。下面示例默认服务地址为：

```text
http://localhost:8080
```

Token 示例变量：

```bash
TOKEN="Bearer your-token"
```

测试文件示例：

```bash
FILE_PATH="./demo.pdf"
FILE_HASH=$(shasum -a 256 "$FILE_PATH" | awk '{print $1}')
FILE_SIZE=$(wc -c < "$FILE_PATH" | tr -d ' ')
```

### 普通上传测试

普通上传直接提交文件、访问类型和上传来源。

```bash
curl -X POST "http://localhost:8080/api/files/upload" \
  -H "Authorization: ${TOKEN}" \
  -F "file=@${FILE_PATH}" \
  -F "accessType=PRIVATE" \
  -F "uploadSource=attachment"
```

预期结果：

```json
{
  "code": 200,
  "message": "操作成功",
  "data": {
    "id": 30001,
    "originalName": "demo.pdf",
    "fileSize": 102400,
    "fileSizeText": "100 KB",
    "contentType": "application/pdf",
    "fileExt": "pdf",
    "accessType": "PRIVATE",
    "uploadSource": "attachment"
  }
}
```

根据文件 ID 查询详情：

```bash
curl -X GET "http://localhost:8080/api/files/30001" \
  -H "Authorization: ${TOKEN}"
```

### 秒传测试

先调用秒传检测接口。

```bash
curl -X POST "http://localhost:8080/api/files/instant-check" \
  -H "Authorization: ${TOKEN}" \
  -H "Content-Type: application/json" \
  -d "{
    \"fileHash\": \"${FILE_HASH}\",
    \"originalName\": \"demo.pdf\",
    \"fileSize\": ${FILE_SIZE}
  }"
```

如果返回 `exists=true`，说明物理文件已经存在，可以调用秒传保存接口。

```bash
curl -X POST "http://localhost:8080/api/files/instant-save" \
  -H "Authorization: ${TOKEN}" \
  -H "Content-Type: application/json" \
  -d "{
    \"fileHash\": \"${FILE_HASH}\",
    \"originalName\": \"demo-copy.pdf\",
    \"fileSize\": ${FILE_SIZE},
    \"contentType\": \"application/pdf\",
    \"accessType\": \"PRIVATE\",
    \"uploadSource\": \"attachment\"
  }"
```

验证重点：

| 检查项                   | 预期结果                         |
| ------------------------ | -------------------------------- |
| `file_storage`           | 相同 `file_hash` 只有一条记录    |
| `file_info`              | 每次秒传都会新增一条业务文件记录 |
| `file_storage.ref_count` | 秒传成功后引用次数增加           |
| MinIO                    | 不会重复上传相同文件内容         |

### 分片上传测试

准备一个大文件，并计算 Hash。

```bash
BIG_FILE="./big-video.mp4"
BIG_HASH=$(shasum -a 256 "$BIG_FILE" | awk '{print $1}')
BIG_SIZE=$(wc -c < "$BIG_FILE" | tr -d ' ')
CHUNK_SIZE=5242880
```

初始化分片任务：

```bash
curl -X POST "http://localhost:8080/api/files/chunk/init" \
  -H "Authorization: ${TOKEN}" \
  -H "Content-Type: application/json" \
  -d "{
    \"fileHash\": \"${BIG_HASH}\",
    \"originalName\": \"big-video.mp4\",
    \"fileSize\": ${BIG_SIZE},
    \"chunkSize\": ${CHUNK_SIZE},
    \"contentType\": \"video/mp4\",
    \"accessType\": \"PRIVATE\",
    \"uploadSource\": \"material\"
  }"
```

假设返回任务 ID 为 `20001`，先切分本地文件。

```bash
mkdir -p ./chunks
split -b ${CHUNK_SIZE} -d -a 4 "$BIG_FILE" ./chunks/chunk_
```

上传所有分片：

```bash
TASK_ID=20001
INDEX=0

for PART in ./chunks/chunk_*; do
  curl -X POST "http://localhost:8080/api/files/chunk/upload" \
    -H "Authorization: ${TOKEN}" \
    -F "taskId=${TASK_ID}" \
    -F "chunkIndex=${INDEX}" \
    -F "file=@${PART}"

  INDEX=$((INDEX + 1))
done
```

查询已上传分片：

```bash
curl -X GET "http://localhost:8080/api/files/chunk/uploaded?taskId=${TASK_ID}" \
  -H "Authorization: ${TOKEN}"
```

合并分片：

```bash
curl -X POST "http://localhost:8080/api/files/chunk/merge" \
  -H "Authorization: ${TOKEN}" \
  -H "Content-Type: application/json" \
  -d "{
    \"taskId\": ${TASK_ID},
    \"fileHash\": \"${BIG_HASH}\",
    \"originalName\": \"big-video.mp4\",
    \"accessType\": \"PRIVATE\",
    \"uploadSource\": \"material\"
  }"
```

验证重点：

| 检查项                   | 预期结果                              |
| ------------------------ | ------------------------------------- |
| `file_chunk_task.status` | 合并成功后变为 `MERGED`               |
| `file_chunk_part`        | 分片记录数量等于总分片数              |
| `file_storage`           | 新增完整文件物理记录                  |
| `file_info`              | 新增当前用户业务文件记录              |
| MinIO                    | `files/yyyy/MM/dd/` 下生成完整文件    |
| MinIO                    | `chunks/{fileHash}/` 下临时分片被清理 |

### 权限访问测试

获取临时下载链接：

```bash
curl -X GET "http://localhost:8080/api/files/30001/download-url" \
  -H "Authorization: ${TOKEN}"
```

使用后端代理下载：

```bash
curl -L -X GET "http://localhost:8080/api/files/30001/download" \
  -H "Authorization: ${TOKEN}" \
  -o "./download-demo.pdf"
```

权限验证可以按下面方式测试：

| 测试方式                        | 预期结果                 |
| ------------------------------- | ------------------------ |
| 上传人访问自己的 `PRIVATE` 文件 | 允许访问                 |
| 其他用户访问该 `PRIVATE` 文件   | 返回无权访问             |
| 登录用户访问 `PUBLIC` 文件      | 允许访问                 |
| 未登录访问接口                  | 被 Sa-Token 拦截         |
| 临时链接过期后再次访问          | MinIO 返回过期或签名失效 |

## 前端调用示例

下面示例使用 `axios` 调用后端接口。普通上传可以直接使用 `FormData`，分片上传需要先计算文件 Hash，再按固定大小切片上传。

### 普通上传调用

普通上传适合小文件。前端只需要把文件、访问类型和上传来源放入 `FormData` 即可。

```javascript
import axios from 'axios'

const request = axios.create({
  baseURL: 'http://localhost:8080',
  timeout: 30000
})

request.interceptors.request.use(config => {
  config.headers.Authorization = `Bearer ${localStorage.getItem('token')}`
  return config
})

export async function uploadFile(file) {
  const formData = new FormData()
  formData.append('file', file)
  formData.append('accessType', 'PRIVATE')
  formData.append('uploadSource', 'attachment')

  const res = await request.post('/api/files/upload', formData, {
    headers: {
      'Content-Type': 'multipart/form-data'
    }
  })

  return res.data.data
}
```

页面中调用：

```javascript
async function handleUpload(event) {
  const file = event.target.files[0]
  if (!file) {
    return
  }

  const fileInfo = await uploadFile(file)
  console.log('上传成功：', fileInfo)
}
```

### 分片上传调用

分片上传适合大文件。示例为了便于理解，使用浏览器原生 `crypto.subtle.digest` 计算 SHA-256。生产环境上传超大文件时，建议放到 Web Worker 中计算 Hash，避免阻塞页面。

先定义文件 Hash 计算方法。

```javascript
export async function calculateSha256(file) {
  const buffer = await file.arrayBuffer()
  const hashBuffer = await crypto.subtle.digest('SHA-256', buffer)
  const hashArray = Array.from(new Uint8Array(hashBuffer))
  return hashArray.map(item => item.toString(16).padStart(2, '0')).join('')
}
```

分片上传主流程如下。

```javascript
const CHUNK_SIZE = 5 * 1024 * 1024

export async function uploadFileByChunk(file) {
  const fileHash = await calculateSha256(file)

  const checkRes = await request.post('/api/files/instant-check', {
    fileHash,
    originalName: file.name,
    fileSize: file.size
  })

  if (checkRes.data.data.exists) {
    const saveRes = await request.post('/api/files/instant-save', {
      fileHash,
      originalName: file.name,
      fileSize: file.size,
      contentType: file.type,
      accessType: 'PRIVATE',
      uploadSource: 'material'
    })
    return saveRes.data.data
  }

  const initRes = await request.post('/api/files/chunk/init', {
    fileHash,
    originalName: file.name,
    fileSize: file.size,
    chunkSize: CHUNK_SIZE,
    contentType: file.type,
    accessType: 'PRIVATE',
    uploadSource: 'material'
  })

  const task = initRes.data.data
  const uploadedSet = new Set(task.uploadedChunks || [])
  const totalChunks = Math.ceil(file.size / CHUNK_SIZE)

  for (let index = 0; index < totalChunks; index++) {
    if (uploadedSet.has(index)) {
      continue
    }

    const start = index * CHUNK_SIZE
    const end = Math.min(file.size, start + CHUNK_SIZE)
    const chunk = file.slice(start, end)

    const formData = new FormData()
    formData.append('taskId', task.taskId)
    formData.append('chunkIndex', index)
    formData.append('file', chunk)

    await request.post('/api/files/chunk/upload', formData, {
      headers: {
        'Content-Type': 'multipart/form-data'
      }
    })

    console.log(`分片上传完成：${index + 1}/${totalChunks}`)
  }

  const mergeRes = await request.post('/api/files/chunk/merge', {
    taskId: task.taskId,
    fileHash,
    originalName: file.name,
    accessType: 'PRIVATE',
    uploadSource: 'material'
  })

  return mergeRes.data.data
}
```

页面中调用：

```javascript
async function handleChunkUpload(event) {
  const file = event.target.files[0]
  if (!file) {
    return
  }

  const fileInfo = await uploadFileByChunk(file)
  console.log('分片上传完成：', fileInfo)
}
```

### 秒传调用

秒传通常作为上传前置步骤。前端先计算文件 Hash，再调用检测接口。如果后端返回文件已存在，则调用秒传保存接口；否则走普通上传或分片上传。

```javascript
export async function instantUpload(file) {
  const fileHash = await calculateSha256(file)

  const checkRes = await request.post('/api/files/instant-check', {
    fileHash,
    originalName: file.name,
    fileSize: file.size
  })

  const checkResult = checkRes.data.data
  if (!checkResult.exists) {
    return {
      instant: false,
      fileHash
    }
  }

  const saveRes = await request.post('/api/files/instant-save', {
    fileHash,
    originalName: file.name,
    fileSize: file.size,
    contentType: file.type,
    accessType: 'PRIVATE',
    uploadSource: 'attachment'
  })

  return {
    instant: true,
    fileInfo: saveRes.data.data
  }
}
```

组合上传策略可以这样处理：

```javascript
export async function smartUpload(file) {
  const instantResult = await instantUpload(file)

  if (instantResult.instant) {
    return instantResult.fileInfo
  }

  if (file.size <= 20 * 1024 * 1024) {
    return uploadFile(file)
  }

  return uploadFileByChunk(file)
}
```

前端下载文件时，推荐先获取临时链接，再跳转下载。

```javascript
export async function downloadByPresignedUrl(fileId) {
  const res = await request.get(`/api/files/${fileId}/download-url`)
  const url = res.data.data.url
  window.open(url, '_blank')
}
```

如果使用后端代理下载：

```javascript
export function downloadByBackend(fileId) {
  window.open(`http://localhost:8080/api/files/${fileId}/download`, '_blank')
}
```

## 小结

本案例完成了 Java 后端项目中常见文件服务的核心能力：普通上传、文件秒传、分片上传、断点续传、分片合并、权限访问、临时下载链接和后端代理下载。

核心设计可以总结为三点：

| 设计点       | 说明                                                         |
| ------------ | ------------------------------------------------------------ |
| 文件内容去重 | 使用 SHA-256 作为文件唯一标识，相同文件只在 MinIO 保存一份   |
| 文件记录分层 | `file_storage` 保存物理文件，`file_info` 保存用户业务文件记录 |
| 文件访问受控 | MinIO Bucket 保持私有，下载前必须经过后端权限校验            |

实际项目中可以继续扩展以下能力：

| 扩展能力       | 说明                                              |
| -------------- | ------------------------------------------------- |
| 文件类型白名单 | 限制只允许上传图片、PDF、Office、压缩包等指定类型 |
| 文件大小限制   | 按上传来源配置不同大小限制                        |
| 病毒扫描       | 文件上传后接入安全扫描服务                        |
| 图片压缩       | 图片上传后异步生成缩略图                          |
| 文件预览       | PDF、图片、视频提供在线预览能力                   |
| 下载审计       | 记录用户下载时间、IP、文件ID                      |
| 分享链接       | 生成带过期时间和访问码的外部分享链接              |
| 异步清理       | 定时清理上传失败的临时分片                        |

至此，这个文件上传模块已经具备可落地集成的核心能力。后续只需要根据项目现有的统一响应类、用户体系、异常处理和权限体系做少量适配即可。