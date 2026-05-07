# Spring Boot 集成 AWS S3 开发

本文档用于说明 Spring Boot 3 项目集成 AWS S3 的基础开发方式，重点覆盖对象存储模块的功能定位、典型使用场景、能力边界，以及正式编码前需要完成的 AWS S3、IAM 和项目环境准备工作。

## 模块概述

本模块用于为 Spring Boot 3 业务系统提供统一的对象存储能力，将文件、图片、文档、报表、压缩包等二进制资源从应用服务器中剥离出来，统一存储到 AWS S3。业务系统通过该模块完成文件上传、下载、删除、访问地址生成和基础校验，不直接关注底层对象存储的连接、认证和 API 调用细节。

### 功能定位

Spring Boot 集成 AWS S3 模块的核心定位是“文件存储基础能力封装”。它通常作为公共基础模块存在，为上层业务模块提供稳定、统一、可复用的文件存储能力。

在系统架构中，该模块一般位于基础服务层，对上暴露 Service 方法或 REST API，对下通过 AWS SDK 调用 S3 服务。业务模块只需要传入文件、业务类型、文件路径或对象 Key，即可完成文件存储操作。

该模块主要负责以下能力：

| 功能     | 说明                                           |
| -------- | ---------------------------------------------- |
| 文件上传 | 接收业务文件并上传到指定 S3 Bucket             |
| 文件下载 | 根据对象 Key 下载文件内容                      |
| 文件删除 | 根据对象 Key 删除 S3 中的对象                  |
| 地址生成 | 生成文件访问地址或临时授权访问地址             |
| 路径封装 | 根据业务类型、日期、用户 ID 等规则生成对象 Key |
| 文件校验 | 校验文件大小、文件类型、空文件和文件名         |
| 异常转换 | 将 S3 相关异常转换为系统统一业务异常           |

该模块不建议直接绑定某个具体业务表。例如用户头像、合同附件、商品图片、报表文件等业务都可以复用同一套 S3 存储能力，但具体文件元数据是否入库、绑定到哪张业务表，应由业务模块自行处理。

### 使用场景

AWS S3 适合存储非结构化文件资源，尤其适合文件数量较多、文件体积较大、需要云端持久化存储或需要跨服务访问的场景。

常见使用场景包括：

| 场景     | 示例                                         |
| -------- | -------------------------------------------- |
| 用户资源 | 用户头像、实名认证图片、个人附件             |
| 业务附件 | 合同文件、审批附件、工单附件                 |
| 内容资源 | 商品图片、文章图片、Banner 图、视频资源      |
| 导入导出 | Excel 导入模板、导出报表、批量处理结果文件   |
| 系统归档 | 日志归档、备份文件、历史数据包               |
| 临时文件 | 临时预览文件、异步任务生成文件、短期下载文件 |

典型文件上传流程如下：

1. 前端通过 `multipart/form-data` 请求提交文件。
2. 后端接收文件并进行大小、类型、空文件校验。
3. 后端根据业务类型生成 S3 对象 Key。
4. 后端调用 AWS S3 SDK 上传文件。
5. 上传成功后返回文件 Key、文件名、文件大小、访问地址等信息。
6. 业务模块根据需要保存文件元数据。

典型文件访问方式可以分为三类：

| 访问方式     | 说明                       | 适用场景                       |
| ------------ | -------------------------- | ------------------------------ |
| 公开访问     | 文件可直接通过 URL 访问    | 商品图、公开资源、官网素材     |
| 后端代理下载 | 文件由后端读取后返回给前端 | 合同、证件、隐私附件           |
| 临时授权访问 | 生成带有效期的临时访问地址 | 临时下载、文件预览、跨系统传递 |

生产环境中，推荐默认使用私有 Bucket，通过后端接口或临时授权地址控制访问权限。只有明确需要公开访问的静态资源，才建议使用公共访问策略或配合 CDN 进行分发。

### 功能边界

本模块只负责 Spring Boot 应用与 AWS S3 的基础集成，不负责完整文件管理平台的所有业务能力。明确功能边界可以降低模块复杂度，也便于后续替换存储实现，例如切换到 MinIO、阿里云 OSS 或其他对象存储服务。

本模块包含以下内容：

| 能力            | 是否包含 | 说明                               |
| --------------- | -------- | ---------------------------------- |
| S3 客户端初始化 | 是       | 创建并管理 S3 客户端               |
| 文件上传        | 是       | 支持单文件上传，后续可扩展批量上传 |
| 文件下载        | 是       | 支持按对象 Key 下载文件            |
| 文件删除        | 是       | 支持按对象 Key 删除文件            |
| 地址生成        | 是       | 支持普通访问地址或临时访问地址     |
| 文件基础校验    | 是       | 校验大小、类型、文件名和空文件     |
| 异常封装        | 是       | 统一处理 S3 操作异常               |
| 文件元数据入库  | 可选     | 由具体业务模块决定                 |
| 图片压缩裁剪    | 否       | 建议独立为图片处理模块             |
| 病毒扫描        | 否       | 建议接入独立安全扫描服务           |
| CDN 配置        | 否       | 通常由基础设施或运维侧负责         |
| 文件权限审批    | 否       | 属于业务权限系统范围               |

在设计时应避免让 S3 模块承担过多业务逻辑。比如“某个用户是否可以下载某份合同”属于业务权限判断，不应写死在 S3 存储服务中。S3 模块只需要根据合法的对象 Key 完成文件读写操作。

## 环境准备

环境准备阶段用于确保 Spring Boot 3 应用在正式开发前具备可用的 S3 Bucket、最小化 IAM 权限和基础运行环境。该阶段不涉及具体业务代码，但会直接影响后续 S3 客户端初始化、文件上传下载和问题排查。

### AWS S3 存储桶准备

在接入 AWS S3 前，需要先创建用于存放文件的 S3 Bucket。Bucket 是 S3 中存储对象的基础容器，应用上传的每个文件都会以 Object 的形式保存在指定 Bucket 下。

建议按照环境和业务域规划 Bucket，避免开发、测试和生产环境混用同一个 Bucket。

| 环境     | Bucket 示例               | 说明             |
| -------- | ------------------------- | ---------------- |
| 开发环境 | `ateng-dev-file-storage`  | 开发联调使用     |
| 测试环境 | `ateng-test-file-storage` | 测试验证使用     |
| 生产环境 | `ateng-prod-file-storage` | 生产业务文件存储 |

如果系统文件类型较多，也可以按业务域拆分 Bucket。

| 业务域   | Bucket 示例               |
| -------- | ------------------------- |
| 用户中心 | `ateng-prod-user-files`   |
| 内容管理 | `ateng-prod-cms-files`    |
| 报表中心 | `ateng-prod-report-files` |
| 日志归档 | `ateng-prod-log-archive`  |

创建 Bucket 时建议关注以下配置：

| 配置项                 | 建议                                             |
| ---------------------- | ------------------------------------------------ |
| Region                 | 选择靠近应用部署区域或主要用户区域的 Region      |
| Bucket 命名            | 使用项目、环境、业务域组合命名                   |
| Public Access          | 默认阻止公共访问，除非明确需要公开资源           |
| Object Ownership       | 建议使用 Bucket owner enforced                   |
| Server-side Encryption | 建议开启默认服务端加密                           |
| Versioning             | 重要文件可开启版本控制                           |
| Lifecycle              | 临时文件、导出文件、归档文件建议配置生命周期清理 |

如果使用 AWS CLI 创建开发环境 Bucket，可以参考以下命令。

```bash
# 设置 AWS 区域和 Bucket 名称
export AWS_REGION=ap-northeast-1
export S3_BUCKET=ateng-dev-file-storage

# 创建 S3 Bucket
aws s3api create-bucket \
  --bucket ${S3_BUCKET} \
  --region ${AWS_REGION} \
  --create-bucket-configuration LocationConstraint=${AWS_REGION}

# 开启默认服务端加密
aws s3api put-bucket-encryption \
  --bucket ${S3_BUCKET} \
  --server-side-encryption-configuration '{
    "Rules": [
      {
        "ApplyServerSideEncryptionByDefault": {
          "SSEAlgorithm": "AES256"
        }
      }
    ]
  }'

# 检查 Bucket 是否可访问
aws s3api head-bucket --bucket ${S3_BUCKET}
```

以上命令中，`AWS_REGION` 表示 Bucket 所在区域，`S3_BUCKET` 表示 Bucket 名称。生产环境建议通过 Terraform、CloudFormation、CDK 或统一云资源管理流程创建 Bucket，避免手工创建导致命名、权限、加密和生命周期策略不一致。

### IAM 权限配置

Spring Boot 应用访问 AWS S3 时需要具备对应的 IAM 权限。生产环境不建议使用 AWS Root 账号或长期固定密钥，推荐使用 IAM Role，例如 ECS Task Role、EKS IRSA、EC2 Instance Profile 等方式授权。

本地开发阶段可以使用 IAM User 的 Access Key 进行联调，但 Access Key 不应写入代码、不应提交到 Git 仓库，也不应直接放在公开配置文件中。

S3 文件操作常用权限如下：

| 权限              | 用途                     |
| ----------------- | ------------------------ |
| `s3:PutObject`    | 上传文件                 |
| `s3:GetObject`    | 下载或读取文件           |
| `s3:DeleteObject` | 删除文件                 |
| `s3:HeadObject`   | 查询文件元数据           |
| `s3:ListBucket`   | 查询 Bucket 下的对象列表 |

推荐使用最小权限策略，只允许应用访问指定 Bucket。以下策略适用于基础上传、下载、删除和对象元数据查询场景。

```json
{
  "Version": "2012-10-17",
  "Statement": [
    {
      "Sid": "BucketListPermission",
      "Effect": "Allow",
      "Action": [
        "s3:ListBucket"
      ],
      "Resource": [
        "arn:aws:s3:::ateng-dev-file-storage"
      ]
    },
    {
      "Sid": "ObjectReadWriteDeletePermission",
      "Effect": "Allow",
      "Action": [
        "s3:PutObject",
        "s3:GetObject",
        "s3:DeleteObject",
        "s3:HeadObject"
      ],
      "Resource": [
        "arn:aws:s3:::ateng-dev-file-storage/*"
      ]
    }
  ]
}
```

如果需要限制应用只能访问某个业务目录，可以进一步收敛对象路径。

```json
{
  "Version": "2012-10-17",
  "Statement": [
    {
      "Sid": "BusinessObjectPermission",
      "Effect": "Allow",
      "Action": [
        "s3:PutObject",
        "s3:GetObject",
        "s3:DeleteObject",
        "s3:HeadObject"
      ],
      "Resource": [
        "arn:aws:s3:::ateng-dev-file-storage/business/*"
      ]
    }
  ]
}
```

权限配置时需要区分 Bucket 级权限和 Object 级权限。

| 权限类型  | Resource 示例                | 常见 Action                                                  |
| --------- | ---------------------------- | ------------------------------------------------------------ |
| Bucket 级 | `arn:aws:s3:::bucket-name`   | `s3:ListBucket`                                              |
| Object 级 | `arn:aws:s3:::bucket-name/*` | `s3:PutObject`、`s3:GetObject`、`s3:DeleteObject`、`s3:HeadObject` |

本地开发可以通过环境变量或 AWS CLI Profile 配置访问凭证。

```bash
# 方式一：通过环境变量配置 AWS 凭证
export AWS_ACCESS_KEY_ID=你的AccessKeyId
export AWS_SECRET_ACCESS_KEY=你的SecretAccessKey
export AWS_REGION=ap-northeast-1

# 方式二：通过 AWS CLI Profile 配置凭证
aws configure --profile ateng-dev

# 验证当前身份
aws sts get-caller-identity
```

如果使用容器或云服务器部署，建议让应用通过运行环境自动获取 IAM Role 凭证，避免在 `application.yml` 中配置长期密钥。

### Spring Boot 3 项目环境

Spring Boot 3 项目需要使用 Java 17 或更高版本。开发前需要确认本地 JDK、构建工具、AWS CLI 和项目基础依赖已经准备完成。

推荐基础环境如下：

| 环境项       | 推荐配置             |
| ------------ | -------------------- |
| JDK          | Java 17 或 Java 21   |
| Spring Boot  | Spring Boot 3.x      |
| Maven        | Maven 3.8+           |
| AWS SDK      | AWS SDK for Java 2.x |
| 构建方式     | Maven                |
| 配置文件     | `application.yml`    |
| 本地验证工具 | AWS CLI              |

可以通过以下命令检查本地环境。

```bash
# 查看 JDK 版本
java -version

# 查看 Maven 版本
mvn -version

# 查看 AWS CLI 版本
aws --version

# 验证 AWS 当前登录身份
aws sts get-caller-identity
```

推荐项目目录结构如下：

```text
springboot-s3-demo
├── pom.xml
├── src
│   ├── main
│   │   ├── java
│   │   │   └── io
│   │   │       └── github
│   │   │           └── atengk
│   │   │               ├── Application.java
│   │   │               ├── config
│   │   │               │   └── S3ClientConfig.java
│   │   │               ├── properties
│   │   │               │   └── S3Properties.java
│   │   │               ├── service
│   │   │               │   ├── S3FileService.java
│   │   │               │   └── impl
│   │   │               │       └── S3FileServiceImpl.java
│   │   │               ├── controller
│   │   │               │   └── S3FileController.java
│   │   │               └── model
│   │   │                   └── vo
│   │   │                       └── S3UploadResultVO.java
│   │   └── resources
│   │       └── application.yml
│   └── test
│       └── java
```

进入后续开发前，建议先确认以下事项：

| 检查项        | 说明                                           |
| ------------- | ---------------------------------------------- |
| JDK 版本      | 满足 Spring Boot 3 运行要求                    |
| Bucket 可用性 | 已创建目标 S3 Bucket                           |
| Region 配置   | 应用配置的 Region 与 Bucket 所在 Region 一致   |
| IAM 权限      | 当前凭证具备上传、下载、删除和查询权限         |
| 网络连通性    | 应用运行环境可以访问 AWS S3                    |
| 环境隔离      | 开发、测试、生产使用不同 Bucket 或不同路径前缀 |
| 密钥安全      | Access Key 不写入代码仓库                      |

可以使用以下命令完成 S3 基础连通性验证。

```bash
# 设置测试 Bucket
export S3_BUCKET=ateng-dev-file-storage

# 检查 Bucket 是否存在并可访问
aws s3api head-bucket --bucket ${S3_BUCKET}

# 创建测试文件
echo "hello s3" > /tmp/s3-test.txt

# 上传测试文件
aws s3 cp /tmp/s3-test.txt s3://${S3_BUCKET}/test/s3-test.txt

# 下载测试文件
aws s3 cp s3://${S3_BUCKET}/test/s3-test.txt /tmp/s3-test-download.txt

# 删除测试文件
aws s3 rm s3://${S3_BUCKET}/test/s3-test.txt
```

如果上述命令均执行成功，说明 Bucket、Region、IAM 权限和本地 AWS 凭证已经可用。后续即可进入“依赖与配置”章节，开始引入 AWS SDK、配置 S3 连接参数，并封装 Spring Boot 3 项目中的 S3 客户端。



## 依赖与配置

本章节用于完成 Spring Boot 3 集成 AWS S3 前的基础依赖和配置准备，包括 Maven 依赖、S3 连接参数、配置属性封装等内容。完成本章节后，项目中即可通过配置文件管理 Bucket、Region、访问凭证和访问地址等参数。

### Maven 依赖配置

Spring Boot 3 项目建议使用 AWS SDK for Java 2.x。该版本采用模块化依赖方式，可以只引入 S3 相关模块，避免引入过多无关依赖。

文件位置：`pom.xml`

以下配置用于引入 Spring Web、参数校验、Lombok、Hutool 和 AWS S3 SDK。

```xml
<properties>
    <!-- Java 版本，Spring Boot 3 至少需要 Java 17 -->
    <java.version>17</java.version>

    <!-- AWS SDK 版本建议由项目统一依赖基线管理 -->
    <aws.sdk.version>2.25.70</aws.sdk.version>

    <!-- Hutool 工具类版本 -->
    <hutool.version>5.8.27</hutool.version>
</properties>

<dependencyManagement>
    <dependencies>
        <!-- AWS SDK BOM，用于统一管理 AWS SDK 各模块版本 -->
        <dependency>
            <groupId>software.amazon.awssdk</groupId>
            <artifactId>bom</artifactId>
            <version>${aws.sdk.version}</version>
            <type>pom</type>
            <scope>import</scope>
        </dependency>
    </dependencies>
</dependencyManagement>

<dependencies>
    <!-- Web 能力，用于后续提供文件上传、下载接口 -->
    <dependency>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-starter-web</artifactId>
    </dependency>

    <!-- 参数校验能力，用于配置类、接口参数校验 -->
    <dependency>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-starter-validation</artifactId>
    </dependency>

    <!-- AWS S3 SDK，用于调用 S3 上传、下载、删除等接口 -->
    <dependency>
        <groupId>software.amazon.awssdk</groupId>
        <artifactId>s3</artifactId>
    </dependency>

    <!-- Hutool 工具类，用于字符串、文件名、日期、ID 等处理 -->
    <dependency>
        <groupId>cn.hutool</groupId>
        <artifactId>hutool-all</artifactId>
        <version>${hutool.version}</version>
    </dependency>

    <!-- Lombok，用于简化 Getter、Setter、构造方法和日志对象 -->
    <dependency>
        <groupId>org.projectlombok</groupId>
        <artifactId>lombok</artifactId>
        <optional>true</optional>
    </dependency>

    <!-- 单元测试依赖 -->
    <dependency>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-starter-test</artifactId>
        <scope>test</scope>
    </dependency>
</dependencies>
```

如果项目已经在父工程中统一管理了 `spring-boot-starter-web`、`lombok`、`hutool-all` 或 AWS SDK 版本，则子模块中只需要保留必要依赖声明，不需要重复声明版本号。

### AWS S3 连接配置

S3 连接配置建议统一放在 `application.yml` 中，通过自定义配置前缀管理。开发环境可以显式配置 `access-key` 和 `secret-key`，生产环境建议留空，由 ECS、EKS、EC2 或其他运行环境自动提供 IAM Role 凭证。

文件位置：`src/main/resources/application.yml`

以下配置用于定义 S3 Bucket、Region、访问凭证、访问地址和临时链接有效期。

```yaml
server:
  port: 8080

spring:
  application:
    name: springboot-s3-demo
  servlet:
    multipart:
      # 单个文件最大上传大小
      max-file-size: 100MB
      # 单次请求最大上传大小
      max-request-size: 100MB

storage:
  s3:
    # AWS S3 区域，需要与 Bucket 所在区域一致
    region: ap-northeast-1

    # S3 Bucket 名称
    bucket: ateng-dev-file-storage

    # 自定义访问域名，可为空
    # 如果接入 CloudFront 或自定义 CDN，可以配置为 https://static.example.com
    public-base-url: ""

    # 是否启用 path-style 访问
    # AWS S3 默认使用虚拟主机风格，一般保持 false
    path-style-access-enabled: false

    # 自定义 endpoint，可为空
    # 使用 AWS S3 时通常不需要配置；兼容 MinIO 等对象存储时才需要
    endpoint: ""

    # 预签名访问地址有效期，单位秒
    presigned-duration-seconds: 600

    credentials:
      # 本地开发可配置；生产环境建议留空，使用 IAM Role
      access-key: ""
      secret-key: ""
```

配置项说明如下：

| 配置项                                  | 说明                                         |
| --------------------------------------- | -------------------------------------------- |
| `storage.s3.region`                     | S3 Bucket 所在 AWS Region                    |
| `storage.s3.bucket`                     | 当前项目使用的 Bucket 名称                   |
| `storage.s3.public-base-url`            | 文件公开访问基础地址，可用于 CDN 域名        |
| `storage.s3.path-style-access-enabled`  | 是否启用 path-style 访问                     |
| `storage.s3.endpoint`                   | 自定义 S3 Endpoint，兼容非 AWS S3 服务时使用 |
| `storage.s3.presigned-duration-seconds` | 预签名 URL 默认有效期                        |
| `storage.s3.credentials.access-key`     | AWS Access Key，本地开发可用                 |
| `storage.s3.credentials.secret-key`     | AWS Secret Key，本地开发可用                 |

生产环境不建议在配置文件中写入长期密钥。更推荐的方式是让应用通过默认凭证链自动获取凭证，例如环境变量、AWS Profile、ECS Task Role、EKS IRSA 或 EC2 Instance Profile。

### 配置属性封装

配置属性封装用于将 `application.yml` 中的 S3 配置映射为 Java 对象，后续初始化 `S3Client` 和 `S3Presigner` 时可以直接注入使用。

文件位置：`src/main/java/io/github/atengk/properties/S3Properties.java`

以下配置类用于承接 `storage.s3` 下的全部配置项。

```java
package io.github.atengk.properties;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Positive;
import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

/**
 * AWS S3 配置属性
 *
 * @author Ateng
 * @since 2026-05-07
 */
@Data
@Validated
@ConfigurationProperties(prefix = "storage.s3")
public class S3Properties {

    /**
     * AWS 区域
     */
    @NotBlank(message = "S3 区域不能为空")
    private String region;

    /**
     * Bucket 名称
     */
    @NotBlank(message = "S3 Bucket 不能为空")
    private String bucket;

    /**
     * 公开访问基础地址，例如 CDN 域名
     */
    private String publicBaseUrl;

    /**
     * 自定义 Endpoint，使用 AWS S3 时可为空
     */
    private String endpoint;

    /**
     * 是否启用 path-style 访问
     */
    private Boolean pathStyleAccessEnabled = false;

    /**
     * 预签名 URL 有效期，单位秒
     */
    @Positive(message = "预签名 URL 有效期必须大于 0")
    private Long presignedDurationSeconds = 600L;

    /**
     * AWS 凭证配置
     */
    @Valid
    private Credentials credentials = new Credentials();

    /**
     * AWS 凭证属性
     *
     * @author Ateng
     * @since 2026-05-07
     */
    @Data
    public static class Credentials {

        /**
         * Access Key，本地开发可配置
         */
        private String accessKey;

        /**
         * Secret Key，本地开发可配置
         */
        private String secretKey;
    }
}
```

该配置类中只强制校验 `region` 和 `bucket`，没有强制校验 `access-key` 和 `secret-key`。这样可以同时兼容本地显式密钥和生产环境 IAM Role 自动授权。

## 核心实现

本章节用于实现 Spring Boot 3 项目中操作 AWS S3 的核心代码，包括 S3 客户端初始化、文件上传、文件下载、文件删除和文件访问地址生成。核心实现建议放在基础设施层或公共存储模块中，供业务模块复用。

建议文件结构如下：

```text
src/main/java/io/github/atengk
├── config
│   └── S3ClientConfig.java
├── exception
│   └── S3FileException.java
├── model
│   └── vo
│       ├── S3DownloadResultVO.java
│       └── S3UploadResultVO.java
├── properties
│   └── S3Properties.java
└── service
    ├── S3FileService.java
    └── impl
        └── S3FileServiceImpl.java
```

### S3 客户端初始化

S3 客户端初始化用于创建 `S3Client` 和 `S3Presigner`。`S3Client` 负责上传、下载、删除等常规对象操作，`S3Presigner` 负责生成临时授权访问地址。

文件位置：`src/main/java/io/github/atengk/config/S3ClientConfig.java`

以下配置类根据 `application.yml` 中的参数创建 S3 客户端 Bean。

```java
package io.github.atengk.config;

import cn.hutool.core.util.StrUtil;
import io.github.atengk.properties.S3Properties;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.AwsCredentialsProvider;
import software.amazon.awssdk.auth.credentials.DefaultCredentialsProvider;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.S3Configuration;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;

import java.net.URI;

/**
 * AWS S3 客户端配置
 *
 * @author Ateng
 * @since 2026-05-07
 */
@Slf4j
@Configuration
@RequiredArgsConstructor
@EnableConfigurationProperties(S3Properties.class)
public class S3ClientConfig {

    private final S3Properties s3Properties;

    /**
     * 创建 S3 同步客户端
     *
     * @return S3 客户端
     */
    @Bean
    public S3Client s3Client() {
        S3Client.Builder builder = S3Client.builder()
                .region(Region.of(s3Properties.getRegion()))
                .credentialsProvider(credentialsProvider())
                .serviceConfiguration(S3Configuration.builder()
                        .pathStyleAccessEnabled(Boolean.TRUE.equals(s3Properties.getPathStyleAccessEnabled()))
                        .build());

        if (StrUtil.isNotBlank(s3Properties.getEndpoint())) {
            builder.endpointOverride(URI.create(s3Properties.getEndpoint()));
            log.info("初始化 S3Client，使用自定义 Endpoint：{}", s3Properties.getEndpoint());
        } else {
            log.info("初始化 S3Client，使用 AWS S3 默认 Endpoint，Region：{}", s3Properties.getRegion());
        }

        return builder.build();
    }

    /**
     * 创建 S3 预签名客户端
     *
     * @return S3 预签名客户端
     */
    @Bean
    public S3Presigner s3Presigner() {
        S3Presigner.Builder builder = S3Presigner.builder()
                .region(Region.of(s3Properties.getRegion()))
                .credentialsProvider(credentialsProvider());

        if (StrUtil.isNotBlank(s3Properties.getEndpoint())) {
            builder.endpointOverride(URI.create(s3Properties.getEndpoint()));
        }

        log.info("初始化 S3Presigner，预签名默认有效期：{} 秒", s3Properties.getPresignedDurationSeconds());
        return builder.build();
    }

    /**
     * 创建 AWS 凭证提供器
     *
     * @return AWS 凭证提供器
     */
    private AwsCredentialsProvider credentialsProvider() {
        S3Properties.Credentials credentials = s3Properties.getCredentials();

        if (credentials != null
                && StrUtil.isAllNotBlank(credentials.getAccessKey(), credentials.getSecretKey())) {
            log.info("S3 使用配置文件中的静态凭证");
            AwsBasicCredentials awsBasicCredentials = AwsBasicCredentials.create(
                    credentials.getAccessKey(),
                    credentials.getSecretKey()
            );
            return StaticCredentialsProvider.create(awsBasicCredentials);
        }

        log.info("S3 使用默认凭证链");
        return DefaultCredentialsProvider.create();
    }
}
```

该配置类支持两种凭证模式：

| 模式       | 说明                                                     |
| ---------- | -------------------------------------------------------- |
| 静态凭证   | `application.yml` 中配置 `access-key` 和 `secret-key`    |
| 默认凭证链 | 不配置密钥，由环境变量、AWS Profile 或 IAM Role 自动提供 |

本地开发可以使用静态凭证或 AWS Profile；生产环境建议使用默认凭证链，并通过云资源运行环境绑定 IAM Role。

### 文件上传实现

文件上传实现用于接收 `MultipartFile`，生成 S3 对象 Key，并通过 `S3Client#putObject` 上传到指定 Bucket。上传成功后返回对象 Key、文件名、文件大小、文件类型、ETag 和访问地址。

文件位置：`src/main/java/io/github/atengk/model/vo/S3UploadResultVO.java`

以下对象用于封装文件上传后的返回结果。

```java
package io.github.atengk.model.vo;

import lombok.Builder;
import lombok.Data;

/**
 * S3 文件上传结果
 *
 * @author Ateng
 * @since 2026-05-07
 */
@Data
@Builder
public class S3UploadResultVO {

    /**
     * Bucket 名称
     */
    private String bucket;

    /**
     * S3 对象 Key
     */
    private String objectKey;

    /**
     * 原始文件名
     */
    private String originalFilename;

    /**
     * 文件大小，单位字节
     */
    private Long size;

    /**
     * 文件内容类型
     */
    private String contentType;

    /**
     * S3 ETag
     */
    private String eTag;

    /**
     * 文件访问地址
     */
    private String url;
}
```

文件位置：`src/main/java/io/github/atengk/model/vo/S3DownloadResultVO.java`

以下对象用于封装文件下载结果。

```java
package io.github.atengk.model.vo;

import lombok.Builder;
import lombok.Data;

/**
 * S3 文件下载结果
 *
 * @author Ateng
 * @since 2026-05-07
 */
@Data
@Builder
public class S3DownloadResultVO {

    /**
     * S3 对象 Key
     */
    private String objectKey;

    /**
     * 文件字节内容
     */
    private byte[] bytes;

    /**
     * 文件大小，单位字节
     */
    private Long size;

    /**
     * 文件内容类型
     */
    private String contentType;

    /**
     * S3 ETag
     */
    private String eTag;
}
```

文件位置：`src/main/java/io/github/atengk/exception/S3FileException.java`

以下异常类用于统一封装 S3 文件操作异常。

```java
package io.github.atengk.exception;

/**
 * S3 文件操作异常
 *
 * @author Ateng
 * @since 2026-05-07
 */
public class S3FileException extends RuntimeException {

    /**
     * 创建 S3 文件操作异常
     *
     * @param message 异常消息
     */
    public S3FileException(String message) {
        super(message);
    }

    /**
     * 创建 S3 文件操作异常
     *
     * @param message 异常消息
     * @param cause 原始异常
     */
    public S3FileException(String message, Throwable cause) {
        super(message, cause);
    }
}
```

文件位置：`src/main/java/io/github/atengk/service/S3FileService.java`

以下接口定义 S3 文件存储的基础能力。

```java
package io.github.atengk.service;

import io.github.atengk.model.vo.S3DownloadResultVO;
import io.github.atengk.model.vo.S3UploadResultVO;
import org.springframework.web.multipart.MultipartFile;

/**
 * S3 文件服务
 *
 * @author Ateng
 * @since 2026-05-07
 */
public interface S3FileService {

    /**
     * 上传文件
     *
     * @param file 文件对象
     * @param bizType 业务类型
     * @return 上传结果
     */
    S3UploadResultVO upload(MultipartFile file, String bizType);

    /**
     * 下载文件
     *
     * @param objectKey S3 对象 Key
     * @return 下载结果
     */
    S3DownloadResultVO download(String objectKey);

    /**
     * 删除文件
     *
     * @param objectKey S3 对象 Key
     */
    void delete(String objectKey);

    /**
     * 生成文件访问地址
     *
     * @param objectKey S3 对象 Key
     * @return 文件访问地址
     */
    String generateAccessUrl(String objectKey);

    /**
     * 生成预签名下载地址
     *
     * @param objectKey S3 对象 Key
     * @return 预签名下载地址
     */
    String generatePresignedUrl(String objectKey);
}
```

文件位置：`src/main/java/io/github/atengk/service/impl/S3FileServiceImpl.java`

以下实现类完成文件上传、下载、删除和访问地址生成。

```java
package io.github.atengk.service.impl;

import cn.hutool.core.date.DateUtil;
import cn.hutool.core.io.FileUtil;
import cn.hutool.core.util.IdUtil;
import cn.hutool.core.util.ObjectUtil;
import cn.hutool.core.util.StrUtil;
import io.github.atengk.exception.S3FileException;
import io.github.atengk.model.vo.S3DownloadResultVO;
import io.github.atengk.model.vo.S3UploadResultVO;
import io.github.atengk.properties.S3Properties;
import io.github.atengk.service.S3FileService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import software.amazon.awssdk.core.ResponseBytes;
import software.amazon.awssdk.core.exception.SdkClientException;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.DeleteObjectRequest;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.services.s3.model.GetObjectResponse;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.services.s3.model.PutObjectResponse;
import software.amazon.awssdk.services.s3.model.S3Exception;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;
import software.amazon.awssdk.services.s3.presigner.model.GetObjectPresignRequest;

import java.io.IOException;
import java.io.InputStream;
import java.time.Duration;
import java.util.Date;

/**
 * S3 文件服务实现
 *
 * @author Ateng
 * @since 2026-05-07
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class S3FileServiceImpl implements S3FileService {

    private static final String DEFAULT_CONTENT_TYPE = "application/octet-stream";
    private static final String DEFAULT_BIZ_TYPE = "common";

    private final S3Client s3Client;
    private final S3Presigner s3Presigner;
    private final S3Properties s3Properties;

    /**
     * 上传文件到 S3
     *
     * @param file 文件对象
     * @param bizType 业务类型
     * @return 上传结果
     */
    @Override
    public S3UploadResultVO upload(MultipartFile file, String bizType) {
        validateFile(file);

        String objectKey = buildObjectKey(file.getOriginalFilename(), bizType);
        String contentType = StrUtil.blankToDefault(file.getContentType(), DEFAULT_CONTENT_TYPE);

        try (InputStream inputStream = file.getInputStream()) {
            PutObjectRequest putObjectRequest = PutObjectRequest.builder()
                    .bucket(s3Properties.getBucket())
                    .key(objectKey)
                    .contentType(contentType)
                    .contentLength(file.getSize())
                    .build();

            PutObjectResponse response = s3Client.putObject(
                    putObjectRequest,
                    RequestBody.fromInputStream(inputStream, file.getSize())
            );

            String accessUrl = generateAccessUrl(objectKey);
            log.info("文件上传 S3 成功，bucket：{}，objectKey：{}，size：{}",
                    s3Properties.getBucket(), objectKey, file.getSize());

            return S3UploadResultVO.builder()
                    .bucket(s3Properties.getBucket())
                    .objectKey(objectKey)
                    .originalFilename(FileUtil.getName(file.getOriginalFilename()))
                    .size(file.getSize())
                    .contentType(contentType)
                    .eTag(response.eTag())
                    .url(accessUrl)
                    .build();
        } catch (IOException e) {
            log.error("读取上传文件失败，文件名：{}", file.getOriginalFilename(), e);
            throw new S3FileException("读取上传文件失败", e);
        } catch (S3Exception | SdkClientException e) {
            log.error("文件上传 S3 失败，bucket：{}，objectKey：{}", s3Properties.getBucket(), objectKey, e);
            throw new S3FileException("文件上传 S3 失败", e);
        }
    }

    /**
     * 从 S3 下载文件
     *
     * @param objectKey S3 对象 Key
     * @return 下载结果
     */
    @Override
    public S3DownloadResultVO download(String objectKey) {
        validateObjectKey(objectKey);

        try {
            GetObjectRequest getObjectRequest = GetObjectRequest.builder()
                    .bucket(s3Properties.getBucket())
                    .key(objectKey)
                    .build();

            ResponseBytes<GetObjectResponse> responseBytes = s3Client.getObjectAsBytes(getObjectRequest);
            GetObjectResponse response = responseBytes.response();

            log.info("文件从 S3 下载成功，bucket：{}，objectKey：{}，size：{}",
                    s3Properties.getBucket(), objectKey, response.contentLength());

            return S3DownloadResultVO.builder()
                    .objectKey(objectKey)
                    .bytes(responseBytes.asByteArray())
                    .size(response.contentLength())
                    .contentType(response.contentType())
                    .eTag(response.eTag())
                    .build();
        } catch (S3Exception | SdkClientException e) {
            log.error("文件从 S3 下载失败，bucket：{}，objectKey：{}", s3Properties.getBucket(), objectKey, e);
            throw new S3FileException("文件从 S3 下载失败", e);
        }
    }

    /**
     * 删除 S3 文件
     *
     * @param objectKey S3 对象 Key
     */
    @Override
    public void delete(String objectKey) {
        validateObjectKey(objectKey);

        try {
            DeleteObjectRequest deleteObjectRequest = DeleteObjectRequest.builder()
                    .bucket(s3Properties.getBucket())
                    .key(objectKey)
                    .build();

            s3Client.deleteObject(deleteObjectRequest);
            log.info("文件从 S3 删除成功，bucket：{}，objectKey：{}", s3Properties.getBucket(), objectKey);
        } catch (S3Exception | SdkClientException e) {
            log.error("文件从 S3 删除失败，bucket：{}，objectKey：{}", s3Properties.getBucket(), objectKey, e);
            throw new S3FileException("文件从 S3 删除失败", e);
        }
    }

    /**
     * 生成文件访问地址
     *
     * @param objectKey S3 对象 Key
     * @return 文件访问地址
     */
    @Override
    public String generateAccessUrl(String objectKey) {
        validateObjectKey(objectKey);

        if (StrUtil.isNotBlank(s3Properties.getPublicBaseUrl())) {
            return StrUtil.removeSuffix(s3Properties.getPublicBaseUrl(), "/") + "/" + objectKey;
        }

        return StrUtil.format(
                "https://{}.s3.{}.amazonaws.com/{}",
                s3Properties.getBucket(),
                s3Properties.getRegion(),
                objectKey
        );
    }

    /**
     * 生成预签名下载地址
     *
     * @param objectKey S3 对象 Key
     * @return 预签名下载地址
     */
    @Override
    public String generatePresignedUrl(String objectKey) {
        validateObjectKey(objectKey);

        try {
            GetObjectRequest getObjectRequest = GetObjectRequest.builder()
                    .bucket(s3Properties.getBucket())
                    .key(objectKey)
                    .build();

            GetObjectPresignRequest presignRequest = GetObjectPresignRequest.builder()
                    .signatureDuration(Duration.ofSeconds(s3Properties.getPresignedDurationSeconds()))
                    .getObjectRequest(getObjectRequest)
                    .build();

            String url = s3Presigner.presignGetObject(presignRequest).url().toString();
            log.info("生成 S3 预签名地址成功，bucket：{}，objectKey：{}，有效期：{} 秒",
                    s3Properties.getBucket(), objectKey, s3Properties.getPresignedDurationSeconds());
            return url;
        } catch (S3Exception | SdkClientException e) {
            log.error("生成 S3 预签名地址失败，bucket：{}，objectKey：{}", s3Properties.getBucket(), objectKey, e);
            throw new S3FileException("生成 S3 预签名地址失败", e);
        }
    }

    /**
     * 校验上传文件
     *
     * @param file 文件对象
     */
    private void validateFile(MultipartFile file) {
        if (ObjectUtil.isNull(file) || file.isEmpty()) {
            throw new S3FileException("上传文件不能为空");
        }

        if (StrUtil.isBlank(file.getOriginalFilename())) {
            throw new S3FileException("上传文件名不能为空");
        }
    }

    /**
     * 校验对象 Key
     *
     * @param objectKey S3 对象 Key
     */
    private void validateObjectKey(String objectKey) {
        if (StrUtil.isBlank(objectKey)) {
            throw new S3FileException("S3 对象 Key 不能为空");
        }
    }

    /**
     * 构建 S3 对象 Key
     *
     * @param originalFilename 原始文件名
     * @param bizType 业务类型
     * @return S3 对象 Key
     */
    private String buildObjectKey(String originalFilename, String bizType) {
        String safeBizType = StrUtil.blankToDefault(bizType, DEFAULT_BIZ_TYPE)
                .replaceAll("[^a-zA-Z0-9_-]", "-");

        String datePath = DateUtil.format(new Date(), "yyyy/MM/dd");
        String extension = FileUtil.extName(originalFilename);
        String filename = StrUtil.isBlank(extension)
                ? IdUtil.fastSimpleUUID()
                : IdUtil.fastSimpleUUID() + "." + extension;

        return StrUtil.format("{}/{}/{}", safeBizType, datePath, filename);
    }
}
```

该实现中的对象 Key 生成规则如下：

```text
业务类型/年/月/日/随机文件名.扩展名
```

示例：

```text
avatar/2026/05/07/6d2f1e8d4b62461d96b8e6c0f33e4a89.png
contract/2026/05/07/93d8f6b7e39f4f16a8ab22e2c5d7b1a1.pdf
report/2026/05/07/bc72e3f53a6444d0af61a6d9e11a8a08.xlsx
```

### 文件下载实现

文件下载通过 `S3Client#getObjectAsBytes` 实现，适合中小文件下载场景。该方法会将文件完整读取到内存中，再返回给调用方。

当前实现方法如下：

```java
@Override
public S3DownloadResultVO download(String objectKey) {
    validateObjectKey(objectKey);

    try {
        GetObjectRequest getObjectRequest = GetObjectRequest.builder()
                .bucket(s3Properties.getBucket())
                .key(objectKey)
                .build();

        ResponseBytes<GetObjectResponse> responseBytes = s3Client.getObjectAsBytes(getObjectRequest);
        GetObjectResponse response = responseBytes.response();

        log.info("文件从 S3 下载成功，bucket：{}，objectKey：{}，size：{}",
                s3Properties.getBucket(), objectKey, response.contentLength());

        return S3DownloadResultVO.builder()
                .objectKey(objectKey)
                .bytes(responseBytes.asByteArray())
                .size(response.contentLength())
                .contentType(response.contentType())
                .eTag(response.eTag())
                .build();
    } catch (S3Exception | SdkClientException e) {
        log.error("文件从 S3 下载失败，bucket：{}，objectKey：{}", s3Properties.getBucket(), objectKey, e);
        throw new S3FileException("文件从 S3 下载失败", e);
    }
}
```

如果业务存在大文件下载，例如几百 MB 或数 GB 文件，不建议使用 `getObjectAsBytes`。这种场景应使用流式下载方式，将 S3 响应流直接写入 `HttpServletResponse`，避免文件内容一次性加载到 JVM 内存中。

### 文件删除实现

文件删除通过 `S3Client#deleteObject` 实现。删除操作只需要传入 Bucket 和对象 Key，不需要传入文件名或访问地址。

当前实现方法如下：

```java
@Override
public void delete(String objectKey) {
    validateObjectKey(objectKey);

    try {
        DeleteObjectRequest deleteObjectRequest = DeleteObjectRequest.builder()
                .bucket(s3Properties.getBucket())
                .key(objectKey)
                .build();

        s3Client.deleteObject(deleteObjectRequest);
        log.info("文件从 S3 删除成功，bucket：{}，objectKey：{}", s3Properties.getBucket(), objectKey);
    } catch (S3Exception | SdkClientException e) {
        log.error("文件从 S3 删除失败，bucket：{}，objectKey：{}", s3Properties.getBucket(), objectKey, e);
        throw new S3FileException("文件从 S3 删除失败", e);
    }
}
```

业务系统删除文件时，建议先删除 S3 对象，再更新业务数据库中的文件记录。如果业务对一致性要求较高，可以将删除操作设计为异步补偿流程，避免 S3 删除成功但数据库更新失败，或数据库删除成功但 S3 对象残留。

### 文件访问地址生成

文件访问地址生成分为普通访问地址和预签名访问地址两种方式。

普通访问地址适合公开资源或已经接入 CDN 的资源。如果配置了 `public-base-url`，系统会优先使用该地址拼接对象 Key。

```java
@Override
public String generateAccessUrl(String objectKey) {
    validateObjectKey(objectKey);

    if (StrUtil.isNotBlank(s3Properties.getPublicBaseUrl())) {
        return StrUtil.removeSuffix(s3Properties.getPublicBaseUrl(), "/") + "/" + objectKey;
    }

    return StrUtil.format(
            "https://{}.s3.{}.amazonaws.com/{}",
            s3Properties.getBucket(),
            s3Properties.getRegion(),
            objectKey
    );
}
```

示例结果如下：

```text
https://ateng-dev-file-storage.s3.ap-northeast-1.amazonaws.com/avatar/2026/05/07/test.png
```

如果配置了 CDN 地址：

```yaml
storage:
  s3:
    public-base-url: https://static.example.com
```

则生成结果如下：

```text
https://static.example.com/avatar/2026/05/07/test.png
```

预签名访问地址适合私有 Bucket 下的临时下载、预览或跨系统传递。该地址带有签名和有效期，到期后自动失效。

```java
@Override
public String generatePresignedUrl(String objectKey) {
    validateObjectKey(objectKey);

    try {
        GetObjectRequest getObjectRequest = GetObjectRequest.builder()
                .bucket(s3Properties.getBucket())
                .key(objectKey)
                .build();

        GetObjectPresignRequest presignRequest = GetObjectPresignRequest.builder()
                .signatureDuration(Duration.ofSeconds(s3Properties.getPresignedDurationSeconds()))
                .getObjectRequest(getObjectRequest)
                .build();

        String url = s3Presigner.presignGetObject(presignRequest).url().toString();
        log.info("生成 S3 预签名地址成功，bucket：{}，objectKey：{}，有效期：{} 秒",
                s3Properties.getBucket(), objectKey, s3Properties.getPresignedDurationSeconds());
        return url;
    } catch (S3Exception | SdkClientException e) {
        log.error("生成 S3 预签名地址失败，bucket：{}，objectKey：{}", s3Properties.getBucket(), objectKey, e);
        throw new S3FileException("生成 S3 预签名地址失败", e);
    }
}
```

两种访问地址的使用建议如下：

| 地址类型       | 是否需要 Bucket 公开 | 适用场景                                 |
| -------------- | -------------------- | ---------------------------------------- |
| 普通访问地址   | 通常需要             | 公开图片、公开文件、CDN 静态资源         |
| 预签名访问地址 | 不需要               | 私有文件、临时下载、临时预览             |
| 后端代理下载   | 不需要               | 权限严格、需要审计、需要隐藏真实对象地址 |

生产系统中，如果文件包含用户隐私、合同、证件、订单附件等敏感内容，建议使用预签名地址或后端代理下载，不建议直接返回永久公开 URL。



## 接口设计

本章节用于定义 Spring Boot 3 对外暴露的 S3 文件操作接口，包括文件上传、文件下载、文件删除和文件信息查询。接口层只负责 HTTP 请求参数接收、基础参数校验和响应封装，具体 S3 操作逻辑应下沉到 `S3FileService` 中。

建议接口统一前缀如下：

```text
/api/s3/files
```

接口清单如下：

| 接口名称       | 请求方式 | 接口地址                      | 说明                  |
| -------------- | -------- | ----------------------------- | --------------------- |
| 文件上传       | `POST`   | `/api/s3/files/upload`        | 上传文件到 S3         |
| 文件下载       | `GET`    | `/api/s3/files/download`      | 根据对象 Key 下载文件 |
| 文件删除       | `DELETE` | `/api/s3/files/delete`        | 根据对象 Key 删除文件 |
| 文件信息查询   | `GET`    | `/api/s3/files/info`          | 查询 S3 文件基础信息  |
| 预签名地址生成 | `GET`    | `/api/s3/files/presigned-url` | 生成临时访问地址      |

### 文件上传接口

文件上传接口用于接收前端提交的 `multipart/form-data` 文件，并将文件上传到 AWS S3。上传成功后返回文件对象 Key、原始文件名、文件大小、文件类型和访问地址。

文件位置：`src/main/java/io/github/atengk/model/vo/ApiResult.java`

以下响应对象用于统一接口返回结构，后续也可以替换为项目已有的统一响应类。

```java
package io.github.atengk.model.vo;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 接口统一响应结果
 *
 * @author Ateng
 * @since 2026-05-07
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ApiResult<T> {

    /**
     * 响应状态码
     */
    private Integer code;

    /**
     * 响应消息
     */
    private String message;

    /**
     * 响应数据
     */
    private T data;

    /**
     * 返回成功响应
     *
     * @param data 响应数据
     * @return 统一响应结果
     */
    public static <T> ApiResult<T> success(T data) {
        return new ApiResult<>(200, "操作成功", data);
    }

    /**
     * 返回成功响应
     *
     * @return 统一响应结果
     */
    public static <T> ApiResult<T> success() {
        return new ApiResult<>(200, "操作成功", null);
    }

    /**
     * 返回失败响应
     *
     * @param message 错误消息
     * @return 统一响应结果
     */
    public static <T> ApiResult<T> fail(String message) {
        return new ApiResult<>(500, message, null);
    }
}
```

文件位置：`src/main/java/io/github/atengk/controller/S3FileController.java`

以下控制器提供上传、下载、删除、文件信息查询和预签名地址生成接口。

```java
package io.github.atengk.controller;

import cn.hutool.core.net.URLEncoder;
import cn.hutool.core.util.StrUtil;
import io.github.atengk.model.vo.ApiResult;
import io.github.atengk.model.vo.S3DownloadResultVO;
import io.github.atengk.model.vo.S3FileInfoVO;
import io.github.atengk.model.vo.S3UploadResultVO;
import io.github.atengk.service.S3FileService;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.constraints.NotBlank;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

/**
 * S3 文件接口
 *
 * @author Ateng
 * @since 2026-05-07
 */
@Slf4j
@Validated
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/s3/files")
public class S3FileController {

    private final S3FileService s3FileService;

    /**
     * 上传文件
     *
     * @param file 文件对象
     * @param bizType 业务类型
     * @return 上传结果
     */
    @PostMapping("/upload")
    public ApiResult<S3UploadResultVO> upload(@RequestPart("file") MultipartFile file,
                                              @RequestParam(value = "bizType", required = false) String bizType) {
        S3UploadResultVO result = s3FileService.upload(file, bizType);
        log.info("文件上传接口调用成功，objectKey：{}", result.getObjectKey());
        return ApiResult.success(result);
    }

    /**
     * 下载文件
     *
     * @param objectKey S3 对象 Key
     * @param response HTTP 响应对象
     */
    @GetMapping("/download")
    public void download(@RequestParam("objectKey") @NotBlank(message = "对象 Key 不能为空") String objectKey,
                         HttpServletResponse response) {
        S3DownloadResultVO result = s3FileService.download(objectKey);

        String filename = StrUtil.subAfter(objectKey, "/", true);
        String encodedFilename = URLEncoder.DEFAULT.encode(filename, StandardCharsets.UTF_8);

        response.setContentType(StrUtil.blankToDefault(result.getContentType(), "application/octet-stream"));
        response.setContentLengthLong(result.getSize());
        response.setHeader("Content-Disposition", "attachment; filename*=UTF-8''" + encodedFilename);

        try {
            response.getOutputStream().write(result.getBytes());
            response.getOutputStream().flush();
            log.info("文件下载接口响应成功，objectKey：{}", objectKey);
        } catch (IOException e) {
            log.error("文件下载接口响应失败，objectKey：{}", objectKey, e);
            throw new IllegalStateException("文件下载响应失败", e);
        }
    }

    /**
     * 删除文件
     *
     * @param objectKey S3 对象 Key
     * @return 删除结果
     */
    @DeleteMapping("/delete")
    public ApiResult<Void> delete(@RequestParam("objectKey") @NotBlank(message = "对象 Key 不能为空") String objectKey) {
        s3FileService.delete(objectKey);
        log.info("文件删除接口调用成功，objectKey：{}", objectKey);
        return ApiResult.success();
    }

    /**
     * 查询文件信息
     *
     * @param objectKey S3 对象 Key
     * @return 文件信息
     */
    @GetMapping("/info")
    public ApiResult<S3FileInfoVO> info(@RequestParam("objectKey") @NotBlank(message = "对象 Key 不能为空") String objectKey) {
        S3FileInfoVO result = s3FileService.getInfo(objectKey);
        return ApiResult.success(result);
    }

    /**
     * 生成预签名访问地址
     *
     * @param objectKey S3 对象 Key
     * @return 预签名访问地址
     */
    @GetMapping("/presigned-url")
    public ApiResult<String> presignedUrl(@RequestParam("objectKey") @NotBlank(message = "对象 Key 不能为空") String objectKey) {
        String url = s3FileService.generatePresignedUrl(objectKey);
        return ApiResult.success(url);
    }
}
```

上传接口说明如下：

| 项目           | 内容                                                  |
| -------------- | ----------------------------------------------------- |
| 请求方式       | `POST`                                                |
| 请求地址       | `/api/s3/files/upload`                                |
| Content-Type   | `multipart/form-data`                                 |
| 参数 `file`    | 上传文件，必填                                        |
| 参数 `bizType` | 业务类型，非必填，例如 `avatar`、`contract`、`report` |

调用示例：

```bash
curl -X POST "http://localhost:8080/api/s3/files/upload" \
  -F "file=@/tmp/test.png" \
  -F "bizType=avatar"
```

响应示例：

```json
{
  "code": 200,
  "message": "操作成功",
  "data": {
    "bucket": "ateng-dev-file-storage",
    "objectKey": "avatar/2026/05/07/6d2f1e8d4b62461d96b8e6c0f33e4a89.png",
    "originalFilename": "test.png",
    "size": 20480,
    "contentType": "image/png",
    "eTag": "\"9b2cf535f27731c974343645a3985328\"",
    "url": "https://ateng-dev-file-storage.s3.ap-northeast-1.amazonaws.com/avatar/2026/05/07/6d2f1e8d4b62461d96b8e6c0f33e4a89.png"
  }
}
```

### 文件下载接口

文件下载接口用于根据 S3 对象 Key 下载文件。当前示例使用后端代理下载方式，即后端先从 S3 获取文件字节，再写入 HTTP 响应。

接口说明如下：

| 项目             | 内容                     |
| ---------------- | ------------------------ |
| 请求方式         | `GET`                    |
| 请求地址         | `/api/s3/files/download` |
| 参数 `objectKey` | S3 对象 Key，必填        |
| 响应类型         | 文件流                   |

调用示例：

```bash
curl -X GET "http://localhost:8080/api/s3/files/download?objectKey=avatar/2026/05/07/test.png" \
  -o test.png
```

后端代理下载适合权限严格、需要鉴权审计、不希望暴露真实 S3 地址的场景。如果文件较大，建议将 `download` 方法改为流式读取，避免将完整文件加载到内存。

当前接口下载流程如下：

1. 校验 `objectKey` 是否为空。
2. 调用 `s3FileService.download(objectKey)` 从 S3 获取文件。
3. 设置 `Content-Type`、`Content-Length` 和 `Content-Disposition`。
4. 将文件字节写入 `HttpServletResponse`。
5. 浏览器或客户端接收文件流。

### 文件删除接口

文件删除接口用于根据对象 Key 删除 S3 中的文件。删除接口只删除 S3 对象，不直接删除业务数据库中的文件记录。业务系统如果保存了文件元数据，应在业务服务中编排“删除 S3 对象”和“更新数据库记录”。

接口说明如下：

| 项目             | 内容                   |
| ---------------- | ---------------------- |
| 请求方式         | `DELETE`               |
| 请求地址         | `/api/s3/files/delete` |
| 参数 `objectKey` | S3 对象 Key，必填      |
| 响应类型         | JSON                   |

调用示例：

```bash
curl -X DELETE "http://localhost:8080/api/s3/files/delete?objectKey=avatar/2026/05/07/test.png"
```

响应示例：

```json
{
  "code": 200,
  "message": "操作成功",
  "data": null
}
```

删除接口需要注意以下事项：

| 注意项     | 说明                                                 |
| ---------- | ---------------------------------------------------- |
| 权限控制   | 删除接口必须经过业务鉴权，避免误删                   |
| 幂等处理   | 删除不存在的对象时，需要根据业务要求决定是否返回成功 |
| 元数据同步 | 如果数据库中保存了文件记录，应同步更新记录状态       |
| 审计日志   | 生产环境建议记录删除人、删除时间、业务 ID 和对象 Key |

### 文件信息查询接口

文件信息查询接口用于根据对象 Key 查询 S3 文件的基础元数据，例如文件大小、文件类型、ETag、最后修改时间和访问地址。该接口通常用于文件详情页、附件列表、下载前校验或管理后台文件排查。

文件位置：`src/main/java/io/github/atengk/model/vo/S3FileInfoVO.java`

以下对象用于封装 S3 文件信息查询结果。

```java
package io.github.atengk.model.vo;

import lombok.Builder;
import lombok.Data;

import java.time.Instant;

/**
 * S3 文件信息
 *
 * @author Ateng
 * @since 2026-05-07
 */
@Data
@Builder
public class S3FileInfoVO {

    /**
     * Bucket 名称
     */
    private String bucket;

    /**
     * S3 对象 Key
     */
    private String objectKey;

    /**
     * 文件大小，单位字节
     */
    private Long size;

    /**
     * 文件内容类型
     */
    private String contentType;

    /**
     * S3 ETag
     */
    private String eTag;

    /**
     * 最后修改时间
     */
    private Instant lastModified;

    /**
     * 文件访问地址
     */
    private String url;
}
```

需要在 `S3FileService` 中增加文件信息查询方法。

文件位置：`src/main/java/io/github/atengk/service/S3FileService.java`

```java
/**
 * 查询文件信息
 *
 * @param objectKey S3 对象 Key
 * @return 文件信息
 */
S3FileInfoVO getInfo(String objectKey);
```

需要在 `S3FileServiceImpl` 中增加以下实现方法。

文件位置：`src/main/java/io/github/atengk/service/impl/S3FileServiceImpl.java`

以下方法通过 `headObject` 查询 S3 对象元数据，不会下载文件内容。

```java
/**
 * 查询 S3 文件信息
 *
 * @param objectKey S3 对象 Key
 * @return 文件信息
 */
@Override
public S3FileInfoVO getInfo(String objectKey) {
    validateObjectKey(objectKey);

    try {
        HeadObjectRequest request = HeadObjectRequest.builder()
                .bucket(s3Properties.getBucket())
                .key(objectKey)
                .build();

        HeadObjectResponse response = s3Client.headObject(request);

        log.info("查询 S3 文件信息成功，bucket：{}，objectKey：{}",
                s3Properties.getBucket(), objectKey);

        return S3FileInfoVO.builder()
                .bucket(s3Properties.getBucket())
                .objectKey(objectKey)
                .size(response.contentLength())
                .contentType(response.contentType())
                .eTag(response.eTag())
                .lastModified(response.lastModified())
                .url(generateAccessUrl(objectKey))
                .build();
    } catch (S3Exception | SdkClientException e) {
        log.error("查询 S3 文件信息失败，bucket：{}，objectKey：{}",
                s3Properties.getBucket(), objectKey, e);
        throw new S3FileException("查询 S3 文件信息失败", e);
    }
}
```

同时需要补充以下导入：

```java
import io.github.atengk.model.vo.S3FileInfoVO;
import software.amazon.awssdk.services.s3.model.HeadObjectRequest;
import software.amazon.awssdk.services.s3.model.HeadObjectResponse;
```

接口说明如下：

| 项目             | 内容                 |
| ---------------- | -------------------- |
| 请求方式         | `GET`                |
| 请求地址         | `/api/s3/files/info` |
| 参数 `objectKey` | S3 对象 Key，必填    |
| 响应类型         | JSON                 |

调用示例：

```bash
curl -X GET "http://localhost:8080/api/s3/files/info?objectKey=avatar/2026/05/07/test.png"
```

响应示例：

```json
{
  "code": 200,
  "message": "操作成功",
  "data": {
    "bucket": "ateng-dev-file-storage",
    "objectKey": "avatar/2026/05/07/test.png",
    "size": 20480,
    "contentType": "image/png",
    "eTag": "\"9b2cf535f27731c974343645a3985328\"",
    "lastModified": "2026-05-07T06:20:30Z",
    "url": "https://ateng-dev-file-storage.s3.ap-northeast-1.amazonaws.com/avatar/2026/05/07/test.png"
  }
}
```

## 业务封装

业务封装用于在基础 S3 操作之上增加更贴近业务系统的规则，例如文件命名、路径规划、文件类型校验和文件大小限制。该层逻辑不应直接写死在 Controller 中，而应封装到 Service 或独立工具类中，保证多个业务模块复用同一套规则。

建议新增以下配置项：

文件位置：`src/main/resources/application.yml`

```yaml
storage:
  s3:
    region: ap-northeast-1
    bucket: ateng-dev-file-storage
    public-base-url: ""
    path-style-access-enabled: false
    endpoint: ""
    presigned-duration-seconds: 600

    credentials:
      access-key: ""
      secret-key: ""

    upload:
      # 单个文件最大大小，单位 MB
      max-size-mb: 100

      # 允许上传的文件扩展名
      allowed-extensions:
        - jpg
        - jpeg
        - png
        - gif
        - pdf
        - doc
        - docx
        - xls
        - xlsx
        - zip

      # 允许上传的 Content-Type
      allowed-content-types:
        - image/jpeg
        - image/png
        - image/gif
        - application/pdf
        - application/msword
        - application/vnd.openxmlformats-officedocument.wordprocessingml.document
        - application/vnd.ms-excel
        - application/vnd.openxmlformats-officedocument.spreadsheetml.sheet
        - application/zip
```

对应需要扩展 `S3Properties`。

文件位置：`src/main/java/io/github/atengk/properties/S3Properties.java`

以下配置片段添加到 `S3Properties` 类中。

```java
/**
 * 上传配置
 */
@Valid
private Upload upload = new Upload();

/**
 * S3 上传配置
 *
 * @author Ateng
 * @since 2026-05-07
 */
@Data
public static class Upload {

    /**
     * 单个文件最大大小，单位 MB
     */
    @Positive(message = "上传文件最大大小必须大于 0")
    private Long maxSizeMb = 100L;

    /**
     * 允许上传的文件扩展名
     */
    private List<String> allowedExtensions = List.of();

    /**
     * 允许上传的 Content-Type
     */
    private List<String> allowedContentTypes = List.of();
}
```

同时需要补充以下导入：

```java
import java.util.List;
```

### 文件命名规则

文件命名规则用于避免原始文件名重复、特殊字符污染路径、中文文件名编码问题和潜在的路径穿越风险。上传到 S3 的对象 Key 不建议直接使用用户上传的原始文件名，而应生成新的安全文件名。

推荐命名规则如下：

```text
UUID.原始扩展名
```

示例：

```text
6d2f1e8d4b62461d96b8e6c0f33e4a89.png
93d8f6b7e39f4f16a8ab22e2c5d7b1a1.pdf
bc72e3f53a6444d0af61a6d9e11a8a08.xlsx
```

命名规则说明：

| 规则                       | 说明                             |
| -------------------------- | -------------------------------- |
| 不使用原始文件名作为对象名 | 避免重名、特殊字符和敏感信息泄露 |
| 保留扩展名                 | 便于识别文件类型和浏览器处理     |
| 使用 UUID                  | 保证文件名唯一性                 |
| 扩展名转小写               | 避免 `JPG`、`jpg` 混用           |
| 不信任前端文件名           | 原始文件名只作为展示信息保存     |

可以将文件命名逻辑抽取为独立工具类。

文件位置：`src/main/java/io/github/atengk/util/S3FileNameUtil.java`

以下工具类用于生成安全文件名和清洗业务类型。

```java
package io.github.atengk.util;

import cn.hutool.core.io.FileUtil;
import cn.hutool.core.util.IdUtil;
import cn.hutool.core.util.StrUtil;

/**
 * S3 文件命名工具
 *
 * @author Ateng
 * @since 2026-05-07
 */
public class S3FileNameUtil {

    private static final String DEFAULT_BIZ_TYPE = "common";

    /**
     * 生成安全文件名
     *
     * @param originalFilename 原始文件名
     * @return 安全文件名
     */
    public static String generateSafeFilename(String originalFilename) {
        String extension = FileUtil.extName(originalFilename);

        if (StrUtil.isBlank(extension)) {
            return IdUtil.fastSimpleUUID();
        }

        return IdUtil.fastSimpleUUID() + "." + extension.toLowerCase();
    }

    /**
     * 清洗业务类型
     *
     * @param bizType 业务类型
     * @return 安全业务类型
     */
    public static String cleanBizType(String bizType) {
        return StrUtil.blankToDefault(bizType, DEFAULT_BIZ_TYPE)
                .replaceAll("[^a-zA-Z0-9_-]", "-")
                .toLowerCase();
    }

    private S3FileNameUtil() {
    }
}
```

原始文件名可以保存到业务数据库中，用于页面展示或下载时回显，但不建议参与 S3 对象 Key 的直接生成。

### 文件路径规划

文件路径规划用于控制 S3 中对象 Key 的组织方式。合理的路径可以提高可读性，也便于后续按业务、日期、用户或模块进行清理和排查。

推荐默认路径规则如下：

```text
业务类型/年/月/日/文件名
```

示例：

```text
avatar/2026/05/07/6d2f1e8d4b62461d96b8e6c0f33e4a89.png
contract/2026/05/07/93d8f6b7e39f4f16a8ab22e2c5d7b1a1.pdf
report/2026/05/07/bc72e3f53a6444d0af61a6d9e11a8a08.xlsx
```

如果文件与用户强相关，可以增加用户维度：

```text
业务类型/用户ID/年/月/日/文件名
```

示例：

```text
avatar/10001/2026/05/07/6d2f1e8d4b62461d96b8e6c0f33e4a89.png
```

如果文件与租户强相关，可以增加租户维度：

```text
租户ID/业务类型/年/月/日/文件名
```

示例：

```text
tenant-a/contract/2026/05/07/93d8f6b7e39f4f16a8ab22e2c5d7b1a1.pdf
```

可以将路径规划封装为工具类。

文件位置：`src/main/java/io/github/atengk/util/S3ObjectKeyUtil.java`

以下工具类用于生成标准 S3 对象 Key。

```java
package io.github.atengk.util;

import cn.hutool.core.date.DateUtil;
import cn.hutool.core.util.StrUtil;

import java.util.Date;

/**
 * S3 对象 Key 工具
 *
 * @author Ateng
 * @since 2026-05-07
 */
public class S3ObjectKeyUtil {

    /**
     * 构建默认对象 Key
     *
     * @param originalFilename 原始文件名
     * @param bizType 业务类型
     * @return S3 对象 Key
     */
    public static String buildDefaultKey(String originalFilename, String bizType) {
        String safeBizType = S3FileNameUtil.cleanBizType(bizType);
        String datePath = DateUtil.format(new Date(), "yyyy/MM/dd");
        String safeFilename = S3FileNameUtil.generateSafeFilename(originalFilename);

        return StrUtil.format("{}/{}/{}", safeBizType, datePath, safeFilename);
    }

    /**
     * 构建带用户维度的对象 Key
     *
     * @param originalFilename 原始文件名
     * @param bizType 业务类型
     * @param userId 用户 ID
     * @return S3 对象 Key
     */
    public static String buildUserKey(String originalFilename, String bizType, Long userId) {
        String safeBizType = S3FileNameUtil.cleanBizType(bizType);
        String datePath = DateUtil.format(new Date(), "yyyy/MM/dd");
        String safeFilename = S3FileNameUtil.generateSafeFilename(originalFilename);

        return StrUtil.format("{}/{}/{}/{}", safeBizType, userId, datePath, safeFilename);
    }

    /**
     * 构建带租户维度的对象 Key
     *
     * @param originalFilename 原始文件名
     * @param tenantCode 租户编码
     * @param bizType 业务类型
     * @return S3 对象 Key
     */
    public static String buildTenantKey(String originalFilename, String tenantCode, String bizType) {
        String safeTenantCode = StrUtil.blankToDefault(tenantCode, "default")
                .replaceAll("[^a-zA-Z0-9_-]", "-")
                .toLowerCase();
        String safeBizType = S3FileNameUtil.cleanBizType(bizType);
        String datePath = DateUtil.format(new Date(), "yyyy/MM/dd");
        String safeFilename = S3FileNameUtil.generateSafeFilename(originalFilename);

        return StrUtil.format("{}/{}/{}/{}", safeTenantCode, safeBizType, datePath, safeFilename);
    }

    private S3ObjectKeyUtil() {
    }
}
```

如果使用该工具类，可以将前文 `S3FileServiceImpl` 中的 `buildObjectKey` 方法替换为：

```java
String objectKey = S3ObjectKeyUtil.buildDefaultKey(file.getOriginalFilename(), bizType);
```

同时补充导入：

```java
import io.github.atengk.util.S3ObjectKeyUtil;
```

路径规划建议如下：

| 场景         | 推荐路径                                 |
| ------------ | ---------------------------------------- |
| 普通业务文件 | `bizType/yyyy/MM/dd/filename`            |
| 用户私有文件 | `bizType/userId/yyyy/MM/dd/filename`     |
| 多租户文件   | `tenantCode/bizType/yyyy/MM/dd/filename` |
| 临时文件     | `temp/yyyy/MM/dd/filename`               |
| 导出文件     | `export/bizType/yyyy/MM/dd/filename`     |
| 日志归档     | `archive/log/yyyy/MM/dd/filename`        |

### 文件类型校验

文件类型校验用于限制用户上传的文件范围，避免上传可执行脚本、未知格式文件或与业务无关的文件。文件类型校验建议同时校验扩展名和 `Content-Type`，但不能完全依赖前端传入的 `Content-Type`。

校验规则建议如下：

| 校验项       | 说明                                                  |
| ------------ | ----------------------------------------------------- |
| 扩展名       | 从原始文件名中提取，例如 `png`、`pdf`、`xlsx`         |
| Content-Type | 从 `MultipartFile#getContentType()` 获取              |
| 空文件       | 禁止上传空文件                                        |
| 文件名       | 禁止空文件名                                          |
| 业务类型     | 可按业务类型配置不同允许范围                          |
| 危险类型     | 默认拒绝 `exe`、`sh`、`bat`、`cmd`、`js` 等可执行文件 |

文件位置：`src/main/java/io/github/atengk/service/impl/S3FileServiceImpl.java`

可以将 `validateFile` 方法扩展为以下实现。

```java
/**
 * 校验上传文件
 *
 * @param file 文件对象
 */
private void validateFile(MultipartFile file) {
    if (ObjectUtil.isNull(file) || file.isEmpty()) {
        throw new S3FileException("上传文件不能为空");
    }

    String originalFilename = file.getOriginalFilename();
    if (StrUtil.isBlank(originalFilename)) {
        throw new S3FileException("上传文件名不能为空");
    }

    validateFileSize(file);
    validateFileExtension(originalFilename);
    validateContentType(file);
}
```

以下方法用于校验文件扩展名。

```java
/**
 * 校验文件扩展名
 *
 * @param originalFilename 原始文件名
 */
private void validateFileExtension(String originalFilename) {
    String extension = FileUtil.extName(originalFilename);

    if (StrUtil.isBlank(extension)) {
        throw new S3FileException("文件扩展名不能为空");
    }

    String normalizedExtension = extension.toLowerCase();
    if (CollUtil.isNotEmpty(s3Properties.getUpload().getAllowedExtensions())
            && !s3Properties.getUpload().getAllowedExtensions().contains(normalizedExtension)) {
        throw new S3FileException("不支持的文件扩展名：" + normalizedExtension);
    }
}
```

以下方法用于校验文件 `Content-Type`。

```java
/**
 * 校验文件 Content-Type
 *
 * @param file 文件对象
 */
private void validateContentType(MultipartFile file) {
    String contentType = file.getContentType();

    if (StrUtil.isBlank(contentType)) {
        throw new S3FileException("文件 Content-Type 不能为空");
    }

    if (CollUtil.isNotEmpty(s3Properties.getUpload().getAllowedContentTypes())
            && !s3Properties.getUpload().getAllowedContentTypes().contains(contentType)) {
        throw new S3FileException("不支持的文件类型：" + contentType);
    }
}
```

需要补充以下导入：

```java
import cn.hutool.core.collection.CollUtil;
```

需要注意，扩展名和 `Content-Type` 校验只能作为基础校验。如果业务安全要求较高，例如实名认证材料、合同文件、压缩包上传等场景，建议进一步接入文件魔数识别、杀毒扫描或内容安全检测服务。

### 文件大小限制

文件大小限制用于防止用户上传超大文件导致网络、内存、S3 流量和业务存储成本异常。文件大小限制建议分为两层：Spring Boot Multipart 限制和业务层限制。

第一层是 Spring Boot 上传限制，在 `application.yml` 中配置：

```yaml
spring:
  servlet:
    multipart:
      # 单个文件最大上传大小
      max-file-size: 100MB
      # 单次请求最大上传大小
      max-request-size: 100MB
```

第二层是业务层限制，在 `storage.s3.upload.max-size-mb` 中配置：

```yaml
storage:
  s3:
    upload:
      # 单个文件最大大小，单位 MB
      max-size-mb: 100
```

文件位置：`src/main/java/io/github/atengk/service/impl/S3FileServiceImpl.java`

以下方法用于进行业务层文件大小校验。

```java
/**
 * 校验文件大小
 *
 * @param file 文件对象
 */
private void validateFileSize(MultipartFile file) {
    long maxSizeBytes = s3Properties.getUpload().getMaxSizeMb() * 1024 * 1024;

    if (file.getSize() > maxSizeBytes) {
        throw new S3FileException(StrUtil.format(
                "文件大小不能超过 {}MB，当前文件大小为 {}MB",
                s3Properties.getUpload().getMaxSizeMb(),
                file.getSize() / 1024 / 1024
        ));
    }
}
```

不同业务场景建议使用不同文件大小限制：

| 场景           | 建议限制                           |
| -------------- | ---------------------------------- |
| 用户头像       | 2MB - 5MB                          |
| 普通图片       | 10MB                               |
| PDF 合同       | 20MB - 50MB                        |
| Excel 导入文件 | 10MB - 30MB                        |
| 导出报表       | 50MB - 200MB                       |
| 压缩包         | 100MB - 500MB                      |
| 视频文件       | 根据业务单独设计，建议使用分片上传 |

如果需要支持大文件上传，建议不要直接使用普通 `MultipartFile` 单请求上传。更合适的方案是使用 S3 Multipart Upload、前端直传 S3、预签名上传 URL 或分片上传流程。普通后端代理上传更适合中小文件场景。



## 异常处理

异常处理用于统一接管 S3 操作异常、接口参数校验异常、文件上传异常和系统未知异常，避免 Controller 中到处编写 `try-catch`。在 Spring Boot 项目中，建议通过 `@RestControllerAdvice` 实现全局异常处理，并将异常结果转换为统一响应结构。

异常处理建议覆盖以下类型：

| 异常类型                                  | 说明                                          |
| ----------------------------------------- | --------------------------------------------- |
| `S3FileException`                         | 业务封装的 S3 文件操作异常                    |
| `S3Exception`                             | AWS S3 服务端返回的异常                       |
| `SdkClientException`                      | AWS SDK 客户端异常，例如网络、凭证、连接超时  |
| `MethodArgumentNotValidException`         | `@RequestBody` 参数校验失败                   |
| `ConstraintViolationException`            | `@RequestParam`、`@PathVariable` 参数校验失败 |
| `MissingServletRequestParameterException` | 缺少必要请求参数                              |
| `MaxUploadSizeExceededException`          | 上传文件超过 Spring Multipart 限制            |
| `Exception`                               | 未预期的系统异常                              |

### S3 操作异常

S3 操作异常主要来源于 AWS SDK 调用过程，包括上传失败、下载失败、删除失败、对象不存在、权限不足、Bucket 不存在、Region 不匹配等问题。业务代码中不建议直接向前端抛出 AWS SDK 原始异常，而应转换为系统内部的业务异常。

前文已经定义了基础异常类 `S3FileException`，这里可以进一步增加错误码字段，便于前端和日志系统识别具体错误类型。

文件位置：`src/main/java/io/github/atengk/exception/S3FileException.java`

以下异常类用于统一封装 S3 文件操作异常，并支持携带业务错误码。

```java
package io.github.atengk.exception;

import lombok.Getter;

/**
 * S3 文件操作异常
 *
 * @author Ateng
 * @since 2026-05-07
 */
@Getter
public class S3FileException extends RuntimeException {

    /**
     * 错误码
     */
    private final Integer code;

    /**
     * 创建 S3 文件操作异常
     *
     * @param message 异常消息
     */
    public S3FileException(String message) {
        super(message);
        this.code = 500;
    }

    /**
     * 创建 S3 文件操作异常
     *
     * @param code 错误码
     * @param message 异常消息
     */
    public S3FileException(Integer code, String message) {
        super(message);
        this.code = code;
    }

    /**
     * 创建 S3 文件操作异常
     *
     * @param message 异常消息
     * @param cause 原始异常
     */
    public S3FileException(String message, Throwable cause) {
        super(message, cause);
        this.code = 500;
    }

    /**
     * 创建 S3 文件操作异常
     *
     * @param code 错误码
     * @param message 异常消息
     * @param cause 原始异常
     */
    public S3FileException(Integer code, String message, Throwable cause) {
        super(message, cause);
        this.code = code;
    }
}
```

可以在 `S3FileServiceImpl` 中根据 AWS S3 返回的错误类型转换异常消息。常见 S3 异常处理建议如下：

| S3 状态码     | 常见原因                         | 建议提示                  |
| ------------- | -------------------------------- | ------------------------- |
| `403`         | IAM 权限不足、Bucket Policy 拒绝 | 当前账号无权操作该文件    |
| `404`         | Bucket 不存在、对象不存在        | 文件不存在或已被删除      |
| `301` / `400` | Region 不匹配或请求参数错误      | S3 区域或请求参数配置错误 |
| `500`         | AWS S3 服务端异常                | S3 服务异常，请稍后重试   |
| `503`         | S3 限流或服务暂时不可用          | S3 服务繁忙，请稍后重试   |

文件位置：`src/main/java/io/github/atengk/service/impl/S3FileServiceImpl.java`

以下方法用于将 AWS SDK 异常转换为业务异常，建议放在 `S3FileServiceImpl` 中复用。

```java
/**
 * 转换 S3 异常
 *
 * @param action 操作名称
 * @param objectKey S3 对象 Key
 * @param e S3 原始异常
 * @return S3 文件操作异常
 */
private S3FileException convertS3Exception(String action, String objectKey, S3Exception e) {
    int statusCode = e.statusCode();
    String awsErrorCode = e.awsErrorDetails() == null ? "" : e.awsErrorDetails().errorCode();

    log.error("S3 操作失败，action：{}，bucket：{}，objectKey：{}，statusCode：{}，awsErrorCode：{}",
            action, s3Properties.getBucket(), objectKey, statusCode, awsErrorCode, e);

    if (statusCode == 403) {
        return new S3FileException(403, "当前账号无权操作该文件", e);
    }

    if (statusCode == 404) {
        return new S3FileException(404, "文件不存在或已被删除", e);
    }

    if (statusCode == 301 || statusCode == 400) {
        return new S3FileException(500, "S3 区域或请求参数配置错误", e);
    }

    if (statusCode == 503) {
        return new S3FileException(503, "S3 服务繁忙，请稍后重试", e);
    }

    return new S3FileException(500, action + "失败", e);
}
```

在上传、下载、删除、查询文件信息等方法中，可以将原来的 `catch (S3Exception | SdkClientException e)` 拆分处理。

```java
try {
    // 执行 S3 操作
} catch (S3Exception e) {
    throw convertS3Exception("文件上传 S3", objectKey, e);
} catch (SdkClientException e) {
    log.error("S3 客户端调用失败，bucket：{}，objectKey：{}", s3Properties.getBucket(), objectKey, e);
    throw new S3FileException(500, "S3 客户端调用失败，请检查网络、凭证或 Endpoint 配置", e);
}
```

这种方式可以让业务层保留清晰的错误语义，同时避免将 AWS SDK 的底层异常结构直接暴露给前端。

### 参数校验异常

参数校验异常主要发生在 Controller 层，例如上传文件为空、对象 Key 为空、缺少必要请求参数、文件超过上传限制等。建议通过 Bean Validation 和全局异常处理统一返回错误信息。

在 Controller 中可以使用以下方式触发参数校验：

```java
@GetMapping("/download")
public void download(@RequestParam("objectKey") @NotBlank(message = "对象 Key 不能为空") String objectKey,
                     HttpServletResponse response) {
    // 文件下载逻辑
}
```

上传文件大小超过限制时，Spring Boot 会抛出 `MaxUploadSizeExceededException`。如果请求参数缺失，会抛出 `MissingServletRequestParameterException`。这些异常都应该在全局异常处理器中统一处理。

文件位置：`src/main/java/io/github/atengk/handler/GlobalExceptionHandler.java`

以下全局异常处理器用于处理参数校验、文件上传、S3 操作和系统未知异常。

```java
package io.github.atengk.handler;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.StrUtil;
import io.github.atengk.exception.S3FileException;
import io.github.atengk.model.vo.ApiResult;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.ConstraintViolationException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.multipart.MaxUploadSizeExceededException;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * 全局异常处理器
 *
 * @author Ateng
 * @since 2026-05-07
 */
@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    /**
     * 处理 S3 文件操作异常
     *
     * @param e S3 文件操作异常
     * @return 统一响应结果
     */
    @ExceptionHandler(S3FileException.class)
    public ApiResult<Void> handleS3FileException(S3FileException e) {
        log.warn("S3 文件操作异常，code：{}，message：{}", e.getCode(), e.getMessage());
        return ApiResult.fail(e.getCode(), e.getMessage());
    }

    /**
     * 处理请求体参数校验异常
     *
     * @param e 请求体参数校验异常
     * @return 统一响应结果
     */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ApiResult<Void> handleMethodArgumentNotValidException(MethodArgumentNotValidException e) {
        List<FieldError> fieldErrors = e.getBindingResult().getFieldErrors();
        String message = CollUtil.isEmpty(fieldErrors)
                ? "请求参数校验失败"
                : fieldErrors.stream()
                .map(error -> error.getField() + " " + error.getDefaultMessage())
                .collect(Collectors.joining("；"));

        log.warn("请求体参数校验失败：{}", message);
        return ApiResult.fail(400, message);
    }

    /**
     * 处理普通参数校验异常
     *
     * @param e 普通参数校验异常
     * @return 统一响应结果
     */
    @ExceptionHandler(ConstraintViolationException.class)
    public ApiResult<Void> handleConstraintViolationException(ConstraintViolationException e) {
        Set<ConstraintViolation<?>> violations = e.getConstraintViolations();
        String message = CollUtil.isEmpty(violations)
                ? "请求参数校验失败"
                : violations.stream()
                .map(ConstraintViolation::getMessage)
                .collect(Collectors.joining("；"));

        log.warn("请求参数校验失败：{}", message);
        return ApiResult.fail(400, message);
    }

    /**
     * 处理缺少请求参数异常
     *
     * @param e 缺少请求参数异常
     * @return 统一响应结果
     */
    @ExceptionHandler(MissingServletRequestParameterException.class)
    public ApiResult<Void> handleMissingServletRequestParameterException(MissingServletRequestParameterException e) {
        String message = StrUtil.format("缺少必要请求参数：{}", e.getParameterName());
        log.warn("缺少必要请求参数：{}", e.getParameterName());
        return ApiResult.fail(400, message);
    }

    /**
     * 处理请求体不可读异常
     *
     * @param e 请求体不可读异常
     * @return 统一响应结果
     */
    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ApiResult<Void> handleHttpMessageNotReadableException(HttpMessageNotReadableException e) {
        log.warn("请求体格式错误：{}", e.getMessage());
        return ApiResult.fail(400, "请求体格式错误");
    }

    /**
     * 处理上传文件超过限制异常
     *
     * @param e 上传文件超过限制异常
     * @return 统一响应结果
     */
    @ExceptionHandler(MaxUploadSizeExceededException.class)
    public ApiResult<Void> handleMaxUploadSizeExceededException(MaxUploadSizeExceededException e) {
        log.warn("上传文件超过系统限制：{}", e.getMessage());
        return ApiResult.fail(400, "上传文件超过系统限制");
    }

    /**
     * 处理系统未知异常
     *
     * @param e 系统未知异常
     * @return 统一响应结果
     */
    @ExceptionHandler(Exception.class)
    public ApiResult<Void> handleException(Exception e) {
        log.error("系统未知异常", e);
        return ApiResult.fail(500, "系统异常，请联系管理员");
    }
}
```

该处理器中，参数错误统一返回 `400`，S3 权限错误可以返回 `403`，对象不存在可以返回 `404`，其他服务端错误返回 `500`。如果项目已有统一异常体系，可以将 `S3FileException` 转换为项目已有的 `BusinessException`。

### 统一响应封装

统一响应封装用于保证所有 JSON 接口返回结构一致。文件下载接口属于文件流响应，不需要使用统一 JSON 包装；上传、删除、查询文件信息、生成预签名地址等接口建议统一返回 `ApiResult<T>`。

文件位置：`src/main/java/io/github/atengk/model/vo/ApiResult.java`

以下响应类是前文 `ApiResult` 的增强版本，支持自定义状态码和错误消息。

```java
package io.github.atengk.model.vo;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 接口统一响应结果
 *
 * @author Ateng
 * @since 2026-05-07
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ApiResult<T> {

    /**
     * 响应状态码
     */
    private Integer code;

    /**
     * 响应消息
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
     * 返回成功响应
     *
     * @param data 响应数据
     * @return 统一响应结果
     */
    public static <T> ApiResult<T> success(T data) {
        return new ApiResult<>(200, "操作成功", data, LocalDateTime.now());
    }

    /**
     * 返回成功响应
     *
     * @return 统一响应结果
     */
    public static <T> ApiResult<T> success() {
        return new ApiResult<>(200, "操作成功", null, LocalDateTime.now());
    }

    /**
     * 返回失败响应
     *
     * @param message 错误消息
     * @return 统一响应结果
     */
    public static <T> ApiResult<T> fail(String message) {
        return new ApiResult<>(500, message, null, LocalDateTime.now());
    }

    /**
     * 返回失败响应
     *
     * @param code 错误码
     * @param message 错误消息
     * @return 统一响应结果
     */
    public static <T> ApiResult<T> fail(Integer code, String message) {
        return new ApiResult<>(code, message, null, LocalDateTime.now());
    }
}
```

统一响应格式示例：

```json
{
  "code": 200,
  "message": "操作成功",
  "data": {
    "objectKey": "avatar/2026/05/07/test.png"
  },
  "timestamp": "2026-05-07T16:30:00"
}
```

异常响应格式示例：

```json
{
  "code": 404,
  "message": "文件不存在或已被删除",
  "data": null,
  "timestamp": "2026-05-07T16:31:10"
}
```

接口返回建议如下：

| 场景               | HTTP 响应体                   |
| ------------------ | ----------------------------- |
| 上传成功           | `ApiResult<S3UploadResultVO>` |
| 删除成功           | `ApiResult<Void>`             |
| 文件信息查询成功   | `ApiResult<S3FileInfoVO>`     |
| 预签名地址生成成功 | `ApiResult<String>`           |
| 文件下载成功       | 文件流，不包装 JSON           |
| 业务异常           | `ApiResult<Void>`             |
| 参数异常           | `ApiResult<Void>`             |

如果系统中已有统一响应结构，例如 `Result<T>`、`R<T>`、`CommonResult<T>`，建议复用已有结构，不要在同一个项目中出现多套响应格式。

## 使用与验证

本章节用于说明 S3 文件模块开发完成后的接口调用方式、功能测试方式和常见问题排查方法。建议先通过 AWS CLI 验证 Bucket 和 IAM 权限，再启动 Spring Boot 项目验证接口，最后补充自动化测试和异常场景测试。

### 接口调用示例

接口调用前，需要确认 Spring Boot 应用已经启动，并且 `application.yml` 中的 `storage.s3.bucket`、`storage.s3.region`、凭证或 IAM Role 已配置正确。

启动应用：

```bash
mvn spring-boot:run
```

如果项目已经打包，可以使用以下方式启动：

```bash
mvn clean package -DskipTests

java -jar target/springboot-s3-demo.jar
```

上传文件示例：

```bash
curl -X POST "http://localhost:8080/api/s3/files/upload" \
  -F "file=@/tmp/test.png" \
  -F "bizType=avatar"
```

上传成功响应示例：

```json
{
  "code": 200,
  "message": "操作成功",
  "data": {
    "bucket": "ateng-dev-file-storage",
    "objectKey": "avatar/2026/05/07/6d2f1e8d4b62461d96b8e6c0f33e4a89.png",
    "originalFilename": "test.png",
    "size": 20480,
    "contentType": "image/png",
    "eTag": "\"9b2cf535f27731c974343645a3985328\"",
    "url": "https://ateng-dev-file-storage.s3.ap-northeast-1.amazonaws.com/avatar/2026/05/07/6d2f1e8d4b62461d96b8e6c0f33e4a89.png"
  },
  "timestamp": "2026-05-07T16:30:00"
}
```

查询文件信息示例：

```bash
curl -X GET "http://localhost:8080/api/s3/files/info?objectKey=avatar/2026/05/07/6d2f1e8d4b62461d96b8e6c0f33e4a89.png"
```

查询成功响应示例：

```json
{
  "code": 200,
  "message": "操作成功",
  "data": {
    "bucket": "ateng-dev-file-storage",
    "objectKey": "avatar/2026/05/07/6d2f1e8d4b62461d96b8e6c0f33e4a89.png",
    "size": 20480,
    "contentType": "image/png",
    "eTag": "\"9b2cf535f27731c974343645a3985328\"",
    "lastModified": "2026-05-07T07:30:00Z",
    "url": "https://ateng-dev-file-storage.s3.ap-northeast-1.amazonaws.com/avatar/2026/05/07/6d2f1e8d4b62461d96b8e6c0f33e4a89.png"
  },
  "timestamp": "2026-05-07T16:31:00"
}
```

生成预签名下载地址示例：

```bash
curl -X GET "http://localhost:8080/api/s3/files/presigned-url?objectKey=avatar/2026/05/07/6d2f1e8d4b62461d96b8e6c0f33e4a89.png"
```

预签名地址响应示例：

```json
{
  "code": 200,
  "message": "操作成功",
  "data": "https://ateng-dev-file-storage.s3.ap-northeast-1.amazonaws.com/avatar/2026/05/07/6d2f1e8d4b62461d96b8e6c0f33e4a89.png?X-Amz-Algorithm=AWS4-HMAC-SHA256&X-Amz-Credential=...",
  "timestamp": "2026-05-07T16:32:00"
}
```

下载文件示例：

```bash
curl -X GET "http://localhost:8080/api/s3/files/download?objectKey=avatar/2026/05/07/6d2f1e8d4b62461d96b8e6c0f33e4a89.png" \
  -o /tmp/download-test.png
```

删除文件示例：

```bash
curl -X DELETE "http://localhost:8080/api/s3/files/delete?objectKey=avatar/2026/05/07/6d2f1e8d4b62461d96b8e6c0f33e4a89.png"
```

删除成功响应示例：

```json
{
  "code": 200,
  "message": "操作成功",
  "data": null,
  "timestamp": "2026-05-07T16:33:00"
}
```

参数异常调用示例：

```bash
curl -X GET "http://localhost:8080/api/s3/files/info"
```

异常响应示例：

```json
{
  "code": 400,
  "message": "缺少必要请求参数：objectKey",
  "data": null,
  "timestamp": "2026-05-07T16:34:00"
}
```

### 功能测试方式

功能测试建议分为三类：AWS CLI 连通性测试、接口联调测试和自动化测试。先验证 AWS 侧可用，再验证 Spring Boot 应用，最后补充自动化测试，可以降低问题定位成本。

首先使用 AWS CLI 验证 Bucket 和权限：

```bash
# 设置测试 Bucket
export S3_BUCKET=ateng-dev-file-storage

# 检查 Bucket 是否可访问
aws s3api head-bucket --bucket ${S3_BUCKET}

# 创建测试文件
echo "hello s3" > /tmp/s3-test.txt

# 上传测试文件
aws s3 cp /tmp/s3-test.txt s3://${S3_BUCKET}/test/s3-test.txt

# 查询文件信息
aws s3api head-object \
  --bucket ${S3_BUCKET} \
  --key test/s3-test.txt

# 下载测试文件
aws s3 cp s3://${S3_BUCKET}/test/s3-test.txt /tmp/s3-test-download.txt

# 删除测试文件
aws s3 rm s3://${S3_BUCKET}/test/s3-test.txt
```

以上命令用于验证当前凭证是否具备 Bucket 访问、上传、查询、下载和删除权限。如果 AWS CLI 不可用，Spring Boot 应用通常也无法正常访问 S3。

接口联调测试建议覆盖以下场景：

| 测试场景           | 预期结果                 |
| ------------------ | ------------------------ |
| 上传合法图片       | 返回对象 Key 和访问地址  |
| 上传合法 PDF       | 返回对象 Key 和访问地址  |
| 上传空文件         | 返回参数或业务异常       |
| 上传超大文件       | 返回文件大小限制异常     |
| 上传不允许的扩展名 | 返回文件类型不支持       |
| 查询存在的文件     | 返回文件大小、类型、ETag |
| 查询不存在的文件   | 返回文件不存在           |
| 下载存在的文件     | 正常返回文件流           |
| 删除存在的文件     | 返回操作成功             |
| 删除后再次查询     | 返回文件不存在或已删除   |
| 生成预签名地址     | 返回带签名参数的临时 URL |

可以使用 JUnit 编写 Service 层集成测试。该测试会真实访问 S3，因此建议只在开发环境或测试环境执行，不要直接连接生产 Bucket。

文件位置：`src/test/java/io/github/atengk/service/S3FileServiceTest.java`

以下测试类用于验证上传、查询、生成预签名地址和删除流程。

```java
package io.github.atengk.service;

import cn.hutool.core.util.StrUtil;
import io.github.atengk.model.vo.S3FileInfoVO;
import io.github.atengk.model.vo.S3UploadResultVO;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.mock.web.MockMultipartFile;

/**
 * S3 文件服务测试
 *
 * @author Ateng
 * @since 2026-05-07
 */
@Slf4j
@SpringBootTest
class S3FileServiceTest {

    @Autowired
    private S3FileService s3FileService;

    /**
     * 测试上传、查询、生成预签名地址和删除文件
     */
    @Test
    void testUploadInfoPresignedUrlAndDelete() {
        MockMultipartFile file = new MockMultipartFile(
                "file",
                "test.txt",
                "text/plain",
                "hello s3".getBytes()
        );

        S3UploadResultVO uploadResult = s3FileService.upload(file, "test");
        Assertions.assertNotNull(uploadResult);
        Assertions.assertTrue(StrUtil.isNotBlank(uploadResult.getObjectKey()));

        log.info("测试上传文件成功，objectKey：{}", uploadResult.getObjectKey());

        S3FileInfoVO info = s3FileService.getInfo(uploadResult.getObjectKey());
        Assertions.assertNotNull(info);
        Assertions.assertEquals(uploadResult.getObjectKey(), info.getObjectKey());

        String presignedUrl = s3FileService.generatePresignedUrl(uploadResult.getObjectKey());
        Assertions.assertTrue(StrUtil.isNotBlank(presignedUrl));
        Assertions.assertTrue(presignedUrl.contains("X-Amz-Signature"));

        s3FileService.delete(uploadResult.getObjectKey());
        log.info("测试删除文件成功，objectKey：{}", uploadResult.getObjectKey());
    }
}
```

如果当前 `application.yml` 中配置了文件类型白名单，测试上传 `text/plain` 前需要将 `txt` 和 `text/plain` 加入允许列表。

```yaml
storage:
  s3:
    upload:
      allowed-extensions:
        - txt
      allowed-content-types:
        - text/plain
```

也可以通过 Postman、Apifox 或 curl 进行手动测试。手动测试时建议记录上传接口返回的 `objectKey`，后续查询、下载、删除接口都依赖该值。

### 常见问题排查

S3 集成问题通常集中在凭证、权限、Region、Bucket 配置、文件大小限制、公共访问策略和对象 Key 上。排查时建议先用 AWS CLI 验证，再查看 Spring Boot 日志，最后检查 IAM Policy、Bucket Policy 和应用配置。

常见问题如下：

| 问题                         | 常见原因                               | 处理方式                                                     |
| ---------------------------- | -------------------------------------- | ------------------------------------------------------------ |
| `AccessDenied`               | IAM 权限不足或 Bucket Policy 拒绝      | 检查 `s3:PutObject`、`s3:GetObject`、`s3:DeleteObject`、`s3:HeadObject` 权限 |
| `NoSuchBucket`               | Bucket 名称错误或环境配置错误          | 检查 `storage.s3.bucket`                                     |
| `NoSuchKey`                  | 对象 Key 不存在                        | 检查上传接口返回的 `objectKey` 是否完整                      |
| `PermanentRedirect`          | Region 配置错误                        | 检查 `storage.s3.region` 是否与 Bucket 区域一致              |
| `SignatureDoesNotMatch`      | 密钥错误、签名区域错误或系统时间偏差   | 检查 Access Key、Secret Key、Region 和服务器时间             |
| `Unable to load credentials` | 默认凭证链没有找到可用凭证             | 配置环境变量、AWS Profile 或 IAM Role                        |
| 上传文件返回 413             | 文件超过网关或 Spring 限制             | 调整 Nginx、网关和 `spring.servlet.multipart` 配置           |
| 下载文件乱码                 | `Content-Disposition` 文件名编码不正确 | 使用 UTF-8 编码设置响应头                                    |
| 公开 URL 无法访问            | Bucket 阻止公共访问或对象非公开        | 使用预签名 URL 或配置公共访问策略                            |
| 预签名 URL 过期              | 超过配置有效期                         | 调整 `presigned-duration-seconds` 或重新生成 URL             |

凭证问题排查：

```bash
# 查看当前 AWS 身份
aws sts get-caller-identity

# 查看当前环境变量是否存在
echo $AWS_ACCESS_KEY_ID
echo $AWS_REGION

# 使用指定 Profile 验证身份
aws sts get-caller-identity --profile ateng-dev
```

Bucket 和 Region 问题排查：

```bash
# 查询 Bucket 所在区域
aws s3api get-bucket-location --bucket ateng-dev-file-storage

# 检查 Bucket 是否可访问
aws s3api head-bucket --bucket ateng-dev-file-storage
```

对象 Key 问题排查：

```bash
# 查询指定前缀下的对象
aws s3 ls s3://ateng-dev-file-storage/avatar/2026/05/07/

# 查询对象元数据
aws s3api head-object \
  --bucket ateng-dev-file-storage \
  --key avatar/2026/05/07/test.png
```

权限问题排查：

```bash
# 测试上传权限
echo "permission test" > /tmp/permission-test.txt
aws s3 cp /tmp/permission-test.txt s3://ateng-dev-file-storage/test/permission-test.txt

# 测试下载权限
aws s3 cp s3://ateng-dev-file-storage/test/permission-test.txt /tmp/permission-test-download.txt

# 测试删除权限
aws s3 rm s3://ateng-dev-file-storage/test/permission-test.txt
```

上传文件超过限制时，需要同时检查 Spring Boot、网关和反向代理配置。如果应用前面有 Nginx，还需要配置 `client_max_body_size`。

文件位置：`/etc/nginx/nginx.conf`

```nginx
http {
    # 允许最大上传 100MB
    client_max_body_size 100m;
}
```

修改 Nginx 后需要重新加载配置：

```bash
# 检查 Nginx 配置
nginx -t

# 重新加载 Nginx
nginx -s reload
```

排查建议顺序如下：

1. 使用 `aws sts get-caller-identity` 确认当前凭证身份。
2. 使用 `aws s3api head-bucket` 确认 Bucket 可访问。
3. 使用 AWS CLI 手动上传、下载、删除测试文件。
4. 检查 `application.yml` 中的 Bucket、Region、Endpoint 和凭证配置。
5. 查看 Spring Boot 日志中的 `statusCode` 和 `awsErrorCode`。
6. 检查 IAM Policy 和 Bucket Policy 是否允许当前操作。
7. 检查文件大小、文件类型、对象 Key 是否符合业务限制。
8. 如果是公网访问失败，检查 Bucket 公共访问策略、CloudFront、CDN 或预签名 URL 是否正确。

生产环境建议将 S3 操作日志接入统一日志平台，至少记录 `bucket`、`objectKey`、`bizType`、操作类型、操作人、请求 ID、异常状态码和耗时。这样在排查文件丢失、权限不足、下载失败等问题时，可以快速定位是应用配置问题、权限问题、网络问题还是 AWS S3 返回异常。