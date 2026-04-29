# AWS S3

AWS SDK for S3 是亚马逊官方提供的开发工具包，用于与 Amazon S3（Simple Storage Service）进行交互。它支持文件上传、下载、删除、列举对象等功能，并封装了身份验证、分段上传、权限控制等操作，方便开发者在 Java、Python、Node.js 等语言中高效地集成 S3 服务。

- [官网连接](https://aws.amazon.com/cn/sdk-for-java/)



## 添加依赖

使用 `S3Client`，并显式配置 Apache HTTP Client 的连接池、超时、代理和 TLS 策略。

```xml
<properties>
    <!-- AWS SDK 2.x 版本统一由 BOM 管理 -->
    <awssdk.version>2.42.41</awssdk.version>

    <!-- Hutool 使用 5.8.x 稳定线，按项目统一版本维护 -->
    <hutool.version>5.8.44</hutool.version>
</properties>

<dependencyManagement>
    <dependencies>
        <!-- AWS SDK BOM：统一管理 software.amazon.awssdk 下各模块版本 -->
        <dependency>
            <groupId>software.amazon.awssdk</groupId>
            <artifactId>bom</artifactId>
            <version>${awssdk.version}</version>
            <type>pom</type>
            <scope>import</scope>
        </dependency>
    </dependencies>
</dependencyManagement>

<dependencies>
    <!-- AWS SDK S3 客户端 -->
    <dependency>
        <groupId>software.amazon.awssdk</groupId>
        <artifactId>s3</artifactId>
    </dependency>

    <!-- 显式配置 ApacheHttpClient 时需要添加该依赖 -->
    <dependency>
        <groupId>software.amazon.awssdk</groupId>
        <artifactId>apache-client</artifactId>
    </dependency>

    <!-- Spring Boot 配置属性校验 -->
    <dependency>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-starter-validation</artifactId>
    </dependency>

    <!-- 配置元数据提示，便于 IDE 自动补全 application.yml -->
    <dependency>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-configuration-processor</artifactId>
        <optional>true</optional>
    </dependency>

    <!-- Hutool 工具类 -->
    <dependency>
        <groupId>cn.hutool</groupId>
        <artifactId>hutool-all</artifactId>
        <version>${hutool.version}</version>
    </dependency>

    <!-- Lombok，减少配置类样板代码 -->
    <dependency>
        <groupId>org.projectlombok</groupId>
        <artifactId>lombok</artifactId>
        <optional>true</optional>
    </dependency>
</dependencies>
```

## 编辑配置文件

生产环境不建议把 `access-key` 和 `secret-key` 写死在配置文件中。AWS 官方 SDK 的默认凭证链会依次从系统属性、环境变量、Web Identity、Profile、容器凭证、EC2 Instance Profile 等位置加载凭证，适合 ECS、EKS、EC2、CI/CD 和本地开发统一使用。([AWS 文档](https://docs.aws.amazon.com/sdk-for-java/latest/developer-guide/credentials-chain.html?utm_source=chatgpt.com))

文件位置：`src/main/resources/application.yml`

```yaml
server:
  port: 14002
  servlet:
    context-path: /

spring:
  main:
    web-application-type: servlet
  application:
    name: ${project.artifactId}
  servlet:
    multipart:
      # 单文件上传大小限制
      max-file-size: ${APP_UPLOAD_MAX_FILE_SIZE:100MB}
      # 单次请求总大小限制
      max-request-size: ${APP_UPLOAD_MAX_REQUEST_SIZE:100MB}

---
# S3 配置
s3:
  # 是否启用 S3 自动配置
  enabled: ${S3_ENABLED:true}

  # 默认桶名称
  bucket-name: ${S3_BUCKET_NAME:data}

  # AWS 区域；兼容 MinIO、Ceph、SeaweedFS 等服务时通常也需要填写一个固定区域
  region: ${AWS_REGION:us-east-1}

  # 真实 AWS S3 生产环境建议留空，让 SDK 使用区域默认 endpoint
  # 兼容 S3 服务示例：http://192.168.1.12:20006
  endpoint: ${S3_ENDPOINT:}

  # true：强制使用 http://host/bucket/key，适合 IP、内网域名、MinIO、Ceph 等兼容 S3 服务
  # false：默认使用虚拟主机风格，适合标准 AWS S3
  path-style-access: ${S3_PATH_STYLE_ACCESS:false}

  # 是否启用响应校验；部分兼容 S3 服务如果校验不兼容，可临时关闭
  checksum-validation-enabled: ${S3_CHECKSUM_VALIDATION_ENABLED:true}

  # 是否启用分块编码；部分老旧兼容 S3 服务如果不兼容，可关闭
  chunked-encoding-enabled: ${S3_CHUNKED_ENCODING_ENABLED:true}

  credentials:
    # default：默认凭证链，推荐生产使用
    # static：静态 access-key/secret-key，适合本地 MinIO 或私有兼容 S3
    # profile：读取 ~/.aws/credentials 或 ~/.aws/config 中的指定 profile
    type: ${S3_CREDENTIALS_TYPE:default}

    # type=static 时使用；不要在生产配置文件中明文写死
    access-key: ${S3_ACCESS_KEY:}
    secret-key: ${S3_SECRET_KEY:}

    # 临时凭证可选项
    session-token: ${S3_SESSION_TOKEN:}

    # type=profile 时使用
    profile-name: ${AWS_PROFILE:default}

  http:
    # 建立 TCP 连接超时
    connection-timeout: ${S3_CONNECTION_TIMEOUT:5s}

    # 等待服务端响应数据超时
    socket-timeout: ${S3_SOCKET_TIMEOUT:60s}

    # 从连接池获取连接超时
    connection-acquisition-timeout: ${S3_CONNECTION_ACQUISITION_TIMEOUT:10s}

    # 最大连接数，按并发上传/下载量调整
    max-connections: ${S3_MAX_CONNECTIONS:100}

    # 长连接保活
    tcp-keep-alive: ${S3_TCP_KEEP_ALIVE:true}

    # 仅允许本地/测试环境开启；生产必须使用可信证书
    trust-all-certificates: ${S3_TRUST_ALL_CERTIFICATES:false}

    proxy:
      # 是否启用 HTTP 代理
      enabled: ${S3_PROXY_ENABLED:false}
      # 示例：http://127.0.0.1:7890
      endpoint: ${S3_PROXY_ENDPOINT:}
      username: ${S3_PROXY_USERNAME:}
      password: ${S3_PROXY_PASSWORD:}

  client:
    # 单次 API 调用总超时，包含重试耗时
    api-call-timeout: ${S3_API_CALL_TIMEOUT:2m}

    # 单次请求尝试超时
    api-call-attempt-timeout: ${S3_API_CALL_ATTEMPT_TIMEOUT:30s}

  presign:
    # 默认预签名 URL 有效期
    default-expire: ${S3_PRESIGN_DEFAULT_EXPIRE:10m}

    # S3 预签名 URL 最大不能超过 7 天
    max-expire: ${S3_PRESIGN_MAX_EXPIRE:7d}
```

`path-style-access` 是否强制开启要看部署场景。AWS SDK 的 S3 配置本身会根据 endpoint 和 bucket 自动判断访问风格；但如果设置为 `true`，会强制所有请求使用路径风格，适合 IP endpoint 或很多 S3 兼容服务。([AWS 文档](https://docs.aws.amazon.com/java/api/latest/software/amazon/awssdk/services/s3/S3Configuration.html?utm_source=chatgpt.com))

## 生产环境推荐配置

下面给出三种可直接复制使用的配置方式，分别对应 `default`、`static`、`profile` 三种凭证模式。三种配置都保留了完整的 S3、HTTP、代理和预签名参数，复制后只需要按环境修改少量变量即可。

AWS SDK for Java 2.x 的默认凭证链会自动从 JVM 系统属性、环境变量、Web Identity、AWS Profile、ECS 容器凭证、EC2 Instance Profile 等位置查找凭证，因此标准 AWS 云上环境优先推荐使用 `type: default`。([AWS 文档](https://docs.aws.amazon.com/sdk-for-java/latest/developer-guide/credentials-chain.html?utm_source=chatgpt.com))

### 标准 AWS S3，使用默认凭证链

该配置适合部署在 EC2、ECS、EKS、Lambda、CI/CD 或已经通过环境变量注入 AWS 凭证的生产环境。标准 AWS S3 不需要配置 `endpoint`，也不建议强制开启 `path-style-access`。

```yaml
# S3 配置：标准 AWS S3 推荐配置
s3:
  # 是否启用 S3 自动配置
  enabled: ${S3_ENABLED:true}

  # 默认桶名称
  bucket-name: ${S3_BUCKET_NAME:prod-bucket}

  # AWS 区域，例如 ap-northeast-1、ap-southeast-1、us-east-1
  region: ${AWS_REGION:ap-northeast-1}

  # 标准 AWS S3 建议留空，由 SDK 根据 region 自动解析 endpoint
  endpoint: ${S3_ENDPOINT:}

  # 标准 AWS S3 推荐 false，使用虚拟主机风格访问
  path-style-access: ${S3_PATH_STYLE_ACCESS:false}

  # 是否启用响应 checksum 校验
  checksum-validation-enabled: ${S3_CHECKSUM_VALIDATION_ENABLED:true}

  # 是否启用分块编码，标准 AWS S3 推荐 true
  chunked-encoding-enabled: ${S3_CHUNKED_ENCODING_ENABLED:true}

  credentials:
    # default：使用 AWS SDK 默认凭证链
    type: ${S3_CREDENTIALS_TYPE:default}

    # default 模式下无需配置 access-key 和 secret-key，保留为空即可
    access-key: ${S3_ACCESS_KEY:}
    secret-key: ${S3_SECRET_KEY:}
    session-token: ${S3_SESSION_TOKEN:}

    # default 模式下通常不需要 profile-name
    profile-name: ${AWS_PROFILE:default}

  http:
    # 建立 TCP 连接超时
    connection-timeout: ${S3_CONNECTION_TIMEOUT:5s}

    # Socket 读超时，大文件上传下载可适当增大
    socket-timeout: ${S3_SOCKET_TIMEOUT:60s}

    # 从连接池获取连接的超时时间
    connection-acquisition-timeout: ${S3_CONNECTION_ACQUISITION_TIMEOUT:10s}

    # 最大连接数，按并发上传/下载量调整
    max-connections: ${S3_MAX_CONNECTIONS:100}

    # 是否开启 TCP KeepAlive
    tcp-keep-alive: ${S3_TCP_KEEP_ALIVE:true}

    # 生产环境必须为 false，避免跳过 TLS 证书校验
    trust-all-certificates: ${S3_TRUST_ALL_CERTIFICATES:false}

    proxy:
      # 是否启用 HTTP 代理
      enabled: ${S3_PROXY_ENABLED:false}

      # 代理地址，例如：http://127.0.0.1:7890
      endpoint: ${S3_PROXY_ENDPOINT:}

      # 代理用户名，可选
      username: ${S3_PROXY_USERNAME:}

      # 代理密码，可选
      password: ${S3_PROXY_PASSWORD:}

  client:
    # 单次 API 调用总超时，包含重试耗时
    api-call-timeout: ${S3_API_CALL_TIMEOUT:2m}

    # 单次请求尝试超时
    api-call-attempt-timeout: ${S3_API_CALL_ATTEMPT_TIMEOUT:30s}

  presign:
    # 默认预签名 URL 有效期
    default-expire: ${S3_PRESIGN_DEFAULT_EXPIRE:10m}

    # S3 预签名 URL 最大不能超过 7 天
    max-expire: ${S3_PRESIGN_MAX_EXPIRE:7d}
```

### 兼容 S3 服务，使用静态 AK/SK

该配置适合 MinIO、Ceph、SeaweedFS、私有云 S3 网关、华为 OBS S3 兼容接口、阿里云 OSS S3 兼容接口等场景。兼容 S3 服务通常需要指定 `endpoint`，并开启 `path-style-access`。

AWS SDK 的 `pathStyleAccessEnabled` 设置为 `true` 后，会强制所有请求使用路径风格访问；如果不设置，SDK 会根据 endpoint 和 bucket 自动判断。IP 地址、内网域名、MinIO、Ceph 等场景通常更适合路径风格访问。([AWS 文档](https://docs.aws.amazon.com/java/api/latest/software/amazon/awssdk/services/s3/S3Configuration.html?utm_source=chatgpt.com))

```yaml
# S3 配置：MinIO、Ceph、SeaweedFS 等兼容 S3 服务推荐配置
s3:
  # 是否启用 S3 自动配置
  enabled: ${S3_ENABLED:true}

  # 默认桶名称
  bucket-name: ${S3_BUCKET_NAME:data}

  # 兼容 S3 服务通常也需要填写 region，常用 us-east-1
  region: ${AWS_REGION:us-east-1}

  # 兼容 S3 服务必须配置 endpoint
  # 示例：http://192.168.1.12:40006
  # 示例：https://minio.example.com
  endpoint: ${S3_ENDPOINT:http://192.168.1.12:40006}

  # 兼容 S3 服务推荐 true，使用 http://host/bucket/key
  path-style-access: ${S3_PATH_STYLE_ACCESS:true}

  # 大多数兼容 S3 服务可保持 true；如果服务端校验兼容性异常，可改为 false
  checksum-validation-enabled: ${S3_CHECKSUM_VALIDATION_ENABLED:true}

  # 大多数兼容 S3 服务可保持 true；如果 PutObject 或 UploadPart 报分块编码兼容问题，可改为 false
  chunked-encoding-enabled: ${S3_CHUNKED_ENCODING_ENABLED:true}

  credentials:
    # static：使用配置文件或环境变量中的 AK/SK
    type: ${S3_CREDENTIALS_TYPE:static}

    # 生产环境建议通过环境变量注入，不要直接写死在 application.yml
    access-key: ${S3_ACCESS_KEY:admin}
    secret-key: ${S3_SECRET_KEY:Admin@123}

    # 如果使用临时凭证则填写；普通 MinIO/兼容 S3 通常留空
    session-token: ${S3_SESSION_TOKEN:}

    # static 模式下不使用 profile-name，保留默认值即可
    profile-name: ${AWS_PROFILE:default}

  http:
    # 建立 TCP 连接超时
    connection-timeout: ${S3_CONNECTION_TIMEOUT:5s}

    # Socket 读超时，大文件上传下载可适当增大
    socket-timeout: ${S3_SOCKET_TIMEOUT:60s}

    # 从连接池获取连接的超时时间
    connection-acquisition-timeout: ${S3_CONNECTION_ACQUISITION_TIMEOUT:10s}

    # 最大连接数，按并发上传/下载量调整
    max-connections: ${S3_MAX_CONNECTIONS:100}

    # 是否开启 TCP KeepAlive
    tcp-keep-alive: ${S3_TCP_KEEP_ALIVE:true}

    # 仅本地测试自签证书时可临时改为 true，生产环境必须为 false
    trust-all-certificates: ${S3_TRUST_ALL_CERTIFICATES:false}

    proxy:
      # 是否启用 HTTP 代理
      enabled: ${S3_PROXY_ENABLED:false}

      # 代理地址，例如：http://127.0.0.1:7890
      endpoint: ${S3_PROXY_ENDPOINT:}

      # 代理用户名，可选
      username: ${S3_PROXY_USERNAME:}

      # 代理密码，可选
      password: ${S3_PROXY_PASSWORD:}

  client:
    # 单次 API 调用总超时，包含重试耗时
    api-call-timeout: ${S3_API_CALL_TIMEOUT:2m}

    # 单次请求尝试超时
    api-call-attempt-timeout: ${S3_API_CALL_ATTEMPT_TIMEOUT:30s}

  presign:
    # 默认预签名 URL 有效期
    default-expire: ${S3_PRESIGN_DEFAULT_EXPIRE:10m}

    # S3 预签名 URL 最大不能超过 7 天
    max-expire: ${S3_PRESIGN_MAX_EXPIRE:7d}
```

如果兼容 S3 服务对 AWS SDK 的校验或分块上传支持不完整，可以改成下面这样：

```yaml
s3:
  checksum-validation-enabled: false
  chunked-encoding-enabled: false
```

这两个参数不建议默认关闭。只有在兼容 S3 服务出现 `checksum`、`chunked encoding`、`signature mismatch`、`not implemented` 等兼容性问题时再调整。

### 本地开发或跳板机环境，使用 AWS Profile

该配置适合开发人员本机通过 `~/.aws/credentials` 和 `~/.aws/config` 管理多套 AWS 账号时使用。例如本地有 `default`、`dev`、`test`、`prod` 等多个 profile，可以通过 `AWS_PROFILE` 或 `s3.credentials.profile-name` 指定。

AWS SDK for Java 2.x 支持从共享的 `credentials` 和 `config` 文件中读取 profile 配置，也支持在默认凭证链中读取 profile。([AWS 文档](https://docs.aws.amazon.com/sdk-for-java/latest/developer-guide/credentials-chain.html?utm_source=chatgpt.com))

```yaml
# S3 配置：本地开发或多账号环境，使用 AWS Profile
s3:
  # 是否启用 S3 自动配置
  enabled: ${S3_ENABLED:true}

  # 默认桶名称
  bucket-name: ${S3_BUCKET_NAME:dev-bucket}

  # AWS 区域
  region: ${AWS_REGION:ap-northeast-1}

  # 使用标准 AWS S3 时留空；如果 profile 用于访问兼容 S3，可改成对应 endpoint
  endpoint: ${S3_ENDPOINT:}

  # 标准 AWS S3 推荐 false；如果访问 MinIO、Ceph 等兼容 S3，可改为 true
  path-style-access: ${S3_PATH_STYLE_ACCESS:false}

  # 是否启用响应 checksum 校验
  checksum-validation-enabled: ${S3_CHECKSUM_VALIDATION_ENABLED:true}

  # 是否启用分块编码
  chunked-encoding-enabled: ${S3_CHUNKED_ENCODING_ENABLED:true}

  credentials:
    # profile：使用 ~/.aws/credentials 或 ~/.aws/config 中的指定 profile
    type: ${S3_CREDENTIALS_TYPE:profile}

    # profile 模式下无需配置 access-key 和 secret-key，保留为空即可
    access-key: ${S3_ACCESS_KEY:}
    secret-key: ${S3_SECRET_KEY:}
    session-token: ${S3_SESSION_TOKEN:}

    # 指定本地 AWS Profile 名称
    profile-name: ${AWS_PROFILE:dev}

  http:
    # 建立 TCP 连接超时
    connection-timeout: ${S3_CONNECTION_TIMEOUT:5s}

    # Socket 读超时，大文件上传下载可适当增大
    socket-timeout: ${S3_SOCKET_TIMEOUT:60s}

    # 从连接池获取连接的超时时间
    connection-acquisition-timeout: ${S3_CONNECTION_ACQUISITION_TIMEOUT:10s}

    # 最大连接数，按并发上传/下载量调整
    max-connections: ${S3_MAX_CONNECTIONS:100}

    # 是否开启 TCP KeepAlive
    tcp-keep-alive: ${S3_TCP_KEEP_ALIVE:true}

    # 生产环境必须为 false
    trust-all-certificates: ${S3_TRUST_ALL_CERTIFICATES:false}

    proxy:
      # 是否启用 HTTP 代理
      enabled: ${S3_PROXY_ENABLED:false}

      # 代理地址，例如：http://127.0.0.1:7890
      endpoint: ${S3_PROXY_ENDPOINT:}

      # 代理用户名，可选
      username: ${S3_PROXY_USERNAME:}

      # 代理密码，可选
      password: ${S3_PROXY_PASSWORD:}

  client:
    # 单次 API 调用总超时，包含重试耗时
    api-call-timeout: ${S3_API_CALL_TIMEOUT:2m}

    # 单次请求尝试超时
    api-call-attempt-timeout: ${S3_API_CALL_ATTEMPT_TIMEOUT:30s}

  presign:
    # 默认预签名 URL 有效期
    default-expire: ${S3_PRESIGN_DEFAULT_EXPIRE:10m}

    # S3 预签名 URL 最大不能超过 7 天
    max-expire: ${S3_PRESIGN_MAX_EXPIRE:7d}
```

本地 profile 文件示例：

文件位置：`~/.aws/credentials`

```ini
[dev]
aws_access_key_id = your-access-key
aws_secret_access_key = your-secret-key

[test]
aws_access_key_id = your-test-access-key
aws_secret_access_key = your-test-secret-key
```

文件位置：`~/.aws/config`

```ini
[profile dev]
region = ap-northeast-1
output = json

[profile test]
region = ap-northeast-1
output = json
```

启动时可以通过环境变量切换 profile：

```bash
export AWS_PROFILE=dev
export S3_CREDENTIALS_TYPE=profile
export S3_BUCKET_NAME=dev-bucket

java -jar app.jar
```

## 创建配置属性类

这个类负责承载完整 S3 配置，并通过 Spring Boot Configuration Properties 进行类型安全绑定。

文件位置：`src/main/java/io/github/atengk/aws/s3/config/S3Properties.java`

```java
package io.github.atengk.aws.s3.config;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

import java.time.Duration;

/**
 * S3 配置属性
 *
 * @author Ateng
 * @since 2026-04-28
 */
@Data
@Validated
@ConfigurationProperties(prefix = "s3")
public class S3Properties {

    /**
     * 是否启用 S3 自动配置
     */
    private Boolean enabled = true;

    /**
     * 默认桶名称
     */
    @NotBlank(message = "S3 bucket-name 不能为空")
    private String bucketName;

    /**
     * S3 区域
     */
    @NotBlank(message = "S3 region 不能为空")
    private String region = "us-east-1";

    /**
     * 自定义 endpoint；标准 AWS S3 可留空
     */
    private String endpoint;

    /**
     * 是否强制路径风格访问
     */
    private Boolean pathStyleAccess = false;

    /**
     * 是否启用响应 checksum 校验
     */
    private Boolean checksumValidationEnabled = true;

    /**
     * 是否启用 chunked encoding
     */
    private Boolean chunkedEncodingEnabled = true;

    /**
     * 凭证配置
     */
    @Valid
    private Credentials credentials = new Credentials();

    /**
     * HTTP 客户端配置
     */
    @Valid
    private Http http = new Http();

    /**
     * SDK Client 配置
     */
    @Valid
    private Client client = new Client();

    /**
     * 预签名配置
     */
    @Valid
    private Presign presign = new Presign();

    /**
     * S3 凭证配置
     *
     * @author Ateng
     * @since 2026-04-28
     */
    @Data
    public static class Credentials {

        /**
         * 凭证类型
         */
        private CredentialsType type = CredentialsType.DEFAULT;

        /**
         * 静态 Access Key
         */
        private String accessKey;

        /**
         * 静态 Secret Key
         */
        private String secretKey;

        /**
         * 临时凭证 Session Token
         */
        private String sessionToken;

        /**
         * AWS Profile 名称
         */
        private String profileName = "default";
    }

    /**
     * S3 HTTP 客户端配置
     *
     * @author Ateng
     * @since 2026-04-28
     */
    @Data
    public static class Http {

        /**
         * 建立连接超时
         */
        private Duration connectionTimeout = Duration.ofSeconds(5);

        /**
         * Socket 读超时
         */
        private Duration socketTimeout = Duration.ofSeconds(60);

        /**
         * 连接池获取连接超时
         */
        private Duration connectionAcquisitionTimeout = Duration.ofSeconds(10);

        /**
         * 最大连接数
         */
        @Min(value = 1, message = "S3 max-connections 不能小于 1")
        @Max(value = 10000, message = "S3 max-connections 不能大于 10000")
        private Integer maxConnections = 100;

        /**
         * 是否开启 TCP KeepAlive
         */
        private Boolean tcpKeepAlive = true;

        /**
         * 是否忽略 TLS 证书校验
         */
        private Boolean trustAllCertificates = false;

        /**
         * 代理配置
         */
        @Valid
        private Proxy proxy = new Proxy();
    }

    /**
     * S3 HTTP 代理配置
     *
     * @author Ateng
     * @since 2026-04-28
     */
    @Data
    public static class Proxy {

        /**
         * 是否启用代理
         */
        private Boolean enabled = false;

        /**
         * 代理 endpoint，例如：http://127.0.0.1:7890
         */
        private String endpoint;

        /**
         * 代理用户名
         */
        private String username;

        /**
         * 代理密码
         */
        private String password;
    }

    /**
     * S3 Client 配置
     *
     * @author Ateng
     * @since 2026-04-28
     */
    @Data
    public static class Client {

        /**
         * 单次 API 调用总超时
         */
        private Duration apiCallTimeout = Duration.ofMinutes(2);

        /**
         * 单次请求尝试超时
         */
        private Duration apiCallAttemptTimeout = Duration.ofSeconds(30);
    }

    /**
     * S3 预签名配置
     *
     * @author Ateng
     * @since 2026-04-28
     */
    @Data
    public static class Presign {

        /**
         * 默认预签名有效期
         */
        private Duration defaultExpire = Duration.ofMinutes(10);

        /**
         * 最大预签名有效期，S3 最大支持 7 天
         */
        private Duration maxExpire = Duration.ofDays(7);
    }

    /**
     * S3 凭证类型
     *
     * @author Ateng
     * @since 2026-04-28
     */
    public enum CredentialsType {

        /**
         * 默认凭证链
         */
        DEFAULT,

        /**
         * 静态 AK/SK
         */
        STATIC,

        /**
         * AWS Profile
         */
        PROFILE
    }
}
```

## 创建配置类

这个配置类同时创建 `S3Client` 和 `S3Presigner`。`S3Client` 负责实际 HTTP 请求，因此需要 HTTP 客户端、超时、连接池和 TLS 配置；`S3Presigner` 只负责签名生成预签名请求，不需要 HTTP Client。官方文档也说明预签名请求本质是把 S3 请求签名后交给调用方执行，并且有效期最长不能超过 7 天。([AWS 文档](https://docs.aws.amazon.com/java/api/latest/software/amazon/awssdk/services/s3/presigner/S3Presigner.html?utm_source=chatgpt.com))

AWS SDK for Java 2.x 默认不会为 API 调用设置总超时和单次尝试超时，生产环境建议显式配置这两个超时，避免网络异常时请求无限等待。([AWS 文档](https://docs.aws.amazon.com/sdk-for-java/latest/developer-guide/timeouts.html?utm_source=chatgpt.com))

文件位置：`src/main/java/io/github/atengk/aws/s3/config/S3Config.java`

```java
package io.github.atengk.aws.s3.config;

import cn.hutool.core.lang.Assert;
import cn.hutool.core.util.BooleanUtil;
import cn.hutool.core.util.StrUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.AwsCredentialsProvider;
import software.amazon.awssdk.auth.credentials.AwsSessionCredentials;
import software.amazon.awssdk.auth.credentials.DefaultCredentialsProvider;
import software.amazon.awssdk.auth.credentials.ProfileCredentialsProvider;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.core.client.config.ClientOverrideConfiguration;
import software.amazon.awssdk.http.apache.ApacheHttpClient;
import software.amazon.awssdk.http.apache.ProxyConfiguration;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.S3ClientBuilder;
import software.amazon.awssdk.services.s3.S3Configuration;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;

import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;
import java.net.URI;
import java.security.cert.X509Certificate;

/**
 * S3 客户端配置
 *
 * @author Ateng
 * @since 2026-04-28
 */
@Slf4j
@Configuration
@RequiredArgsConstructor
@EnableConfigurationProperties(S3Properties.class)
@ConditionalOnProperty(prefix = "s3", name = "enabled", havingValue = "true", matchIfMissing = true)
public class S3Config {

    private final S3Properties s3Properties;

    /**
     * 创建 S3 同步客户端
     *
     * @return S3Client
     */
    @Bean
    public S3Client s3Client() {
        S3ClientBuilder builder = S3Client.builder()
                .region(Region.of(s3Properties.getRegion()))
                .credentialsProvider(buildCredentialsProvider())
                .serviceConfiguration(buildS3Configuration())
                .overrideConfiguration(buildClientOverrideConfiguration())
                .httpClientBuilder(buildHttpClientBuilder());

        if (StrUtil.isNotBlank(s3Properties.getEndpoint())) {
            builder.endpointOverride(URI.create(s3Properties.getEndpoint()));
        }

        log.info("初始化 S3Client 完成，region={}，endpoint={}，pathStyleAccess={}",
                s3Properties.getRegion(),
                StrUtil.blankToDefault(s3Properties.getEndpoint(), "AWS默认Endpoint"),
                BooleanUtil.isTrue(s3Properties.getPathStyleAccess()));

        return builder.build();
    }

    /**
     * 创建 S3 预签名客户端
     *
     * @return S3Presigner
     */
    @Bean
    public S3Presigner s3Presigner() {
        S3Presigner.Builder builder = S3Presigner.builder()
                .region(Region.of(s3Properties.getRegion()))
                .credentialsProvider(buildCredentialsProvider())
                .serviceConfiguration(buildS3Configuration());

        if (StrUtil.isNotBlank(s3Properties.getEndpoint())) {
            builder.endpointOverride(URI.create(s3Properties.getEndpoint()));
        }

        log.info("初始化 S3Presigner 完成，region={}，endpoint={}",
                s3Properties.getRegion(),
                StrUtil.blankToDefault(s3Properties.getEndpoint(), "AWS默认Endpoint"));

        return builder.build();
    }

    /**
     * 构建 S3 服务配置
     *
     * @return S3Configuration
     */
    private S3Configuration buildS3Configuration() {
        return S3Configuration.builder()
                .pathStyleAccessEnabled(BooleanUtil.isTrue(s3Properties.getPathStyleAccess()))
                .checksumValidationEnabled(BooleanUtil.isTrue(s3Properties.getChecksumValidationEnabled()))
                .chunkedEncodingEnabled(BooleanUtil.isTrue(s3Properties.getChunkedEncodingEnabled()))
                .build();
    }

    /**
     * 构建 SDK Client 覆盖配置
     *
     * @return ClientOverrideConfiguration
     */
    private ClientOverrideConfiguration buildClientOverrideConfiguration() {
        S3Properties.Client client = s3Properties.getClient();

        return ClientOverrideConfiguration.builder()
                .apiCallTimeout(client.getApiCallTimeout())
                .apiCallAttemptTimeout(client.getApiCallAttemptTimeout())
                .build();
    }

    /**
     * 构建 Apache HTTP 客户端
     *
     * @return ApacheHttpClient.Builder
     */
    private ApacheHttpClient.Builder buildHttpClientBuilder() {
        S3Properties.Http http = s3Properties.getHttp();

        ApacheHttpClient.Builder builder = ApacheHttpClient.builder()
                .connectionTimeout(http.getConnectionTimeout())
                .socketTimeout(http.getSocketTimeout())
                .connectionAcquisitionTimeout(http.getConnectionAcquisitionTimeout())
                .maxConnections(http.getMaxConnections())
                .tcpKeepAlive(BooleanUtil.isTrue(http.getTcpKeepAlive()));

        if (BooleanUtil.isTrue(http.getTrustAllCertificates())) {
            log.warn("S3 已启用忽略 TLS 证书校验，仅建议在本地或测试环境使用，生产环境请关闭");
            builder.tlsTrustManagersProvider(S3Config::trustAllCerts);
        }

        configureProxy(builder, http.getProxy());
        return builder;
    }

    /**
     * 配置 HTTP 代理
     *
     * @param builder HTTP 客户端构建器
     * @param proxy   代理配置
     */
    private void configureProxy(ApacheHttpClient.Builder builder, S3Properties.Proxy proxy) {
        if (proxy == null || !BooleanUtil.isTrue(proxy.getEnabled())) {
            return;
        }

        Assert.notBlank(proxy.getEndpoint(), "启用 S3 HTTP 代理时必须配置 s3.http.proxy.endpoint");

        ProxyConfiguration.Builder proxyBuilder = ProxyConfiguration.builder()
                .endpoint(URI.create(proxy.getEndpoint()));

        if (StrUtil.isNotBlank(proxy.getUsername())) {
            proxyBuilder.username(proxy.getUsername());
        }
        if (StrUtil.isNotBlank(proxy.getPassword())) {
            proxyBuilder.password(proxy.getPassword());
        }

        builder.proxyConfiguration(proxyBuilder.build());

        log.info("S3 HTTP 代理已启用，endpoint={}", proxy.getEndpoint());
    }

    /**
     * 构建 AWS 凭证提供者
     *
     * @return AwsCredentialsProvider
     */
    private AwsCredentialsProvider buildCredentialsProvider() {
        S3Properties.Credentials credentials = s3Properties.getCredentials();
        S3Properties.CredentialsType type = credentials.getType() == null
                ? S3Properties.CredentialsType.DEFAULT
                : credentials.getType();

        return switch (type) {
            case DEFAULT -> {
                log.info("S3 使用默认凭证链加载凭证");
                yield DefaultCredentialsProvider.create();
            }
            case PROFILE -> {
                String profileName = StrUtil.blankToDefault(credentials.getProfileName(), "default");
                log.info("S3 使用 AWS Profile 加载凭证，profileName={}", profileName);
                yield ProfileCredentialsProvider.builder()
                        .profileName(profileName)
                        .build();
            }
            case STATIC -> {
                Assert.notBlank(credentials.getAccessKey(), "S3 credentials.type=static 时 access-key 不能为空");
                Assert.notBlank(credentials.getSecretKey(), "S3 credentials.type=static 时 secret-key 不能为空");

                if (StrUtil.isNotBlank(credentials.getSessionToken())) {
                    log.info("S3 使用静态临时凭证加载凭证");
                    yield StaticCredentialsProvider.create(AwsSessionCredentials.create(
                            credentials.getAccessKey(),
                            credentials.getSecretKey(),
                            credentials.getSessionToken()
                    ));
                }

                log.info("S3 使用静态 AK/SK 加载凭证");
                yield StaticCredentialsProvider.create(AwsBasicCredentials.create(
                        credentials.getAccessKey(),
                        credentials.getSecretKey()
                ));
            }
        };
    }

    /**
     * 构建信任所有证书的 TrustManager
     *
     * @return TrustManager 数组
     */
    private static TrustManager[] trustAllCerts() {
        return new TrustManager[]{
                new X509TrustManager() {

                    /**
                     * 校验客户端证书
                     *
                     * @param chain    证书链
                     * @param authType 认证类型
                     */
                    @Override
                    public void checkClientTrusted(X509Certificate[] chain, String authType) {
                        // 本地或测试环境忽略客户端证书校验
                    }

                    /**
                     * 校验服务端证书
                     *
                     * @param chain    证书链
                     * @param authType 认证类型
                     */
                    @Override
                    public void checkServerTrusted(X509Certificate[] chain, String authType) {
                        // 本地或测试环境忽略服务端证书校验
                    }

                    /**
                     * 获取受信任 CA
                     *
                     * @return 证书数组
                     */
                    @Override
                    public X509Certificate[] getAcceptedIssuers() {
                        return new X509Certificate[0];
                    }
                }
        };
    }
}
```



## 创建服务接口

```java
package io.github.atengk.aws.s3.service;

import io.github.atengk.aws.s3.model.enums.S3ObjectAcl;
import io.github.atengk.aws.s3.model.enums.S3StorageClass;
import io.github.atengk.aws.s3.model.info.S3MultipartUploadInfo;
import io.github.atengk.aws.s3.model.info.S3MultipartUploadPartInfo;
import io.github.atengk.aws.s3.model.info.S3ObjectInfo;
import io.github.atengk.aws.s3.model.info.S3ObjectVersionInfo;
import io.github.atengk.aws.s3.model.request.*;
import io.github.atengk.aws.s3.model.result.*;
import org.springframework.core.io.Resource;
import org.springframework.web.multipart.MultipartFile;

import java.io.InputStream;
import java.net.URI;
import java.nio.file.Path;
import java.time.Duration;
import java.util.List;
import java.util.Map;

/**
 * S3 文件服务接口
 *
 * @author Ateng
 * @since 2026-04-28
 */
public interface S3Service {

    // ==================== 存储桶管理 ====================

    /**
     * 判断存储桶是否存在。
     *
     * @param bucketName 存储桶名称
     * @return true 存在，false 不存在
     */
    boolean bucketExists(String bucketName);

    /**
     * 创建存储桶。
     *
     * @param bucketName 存储桶名称
     */
    void createBucket(String bucketName);

    /**
     * 存储桶不存在时创建。
     *
     * @param bucketName 存储桶名称
     */
    void createBucketIfAbsent(String bucketName);

    /**
     * 删除存储桶。
     *
     * @param bucketName 存储桶名称
     */
    void deleteBucket(String bucketName);

    /**
     * 查询当前凭证可见的存储桶列表。
     *
     * @return 存储桶名称列表
     */
    List<String> listBuckets();

    /**
     * 获取默认存储桶名称。
     *
     * @return 默认存储桶名称
     */
    String getDefaultBucketName();

    /**
     * 解析存储桶名称，为空时返回默认存储桶。
     *
     * @param bucketName 存储桶名称
     * @return 解析后的存储桶名称
     */
    String resolveBucketName(String bucketName);

    // ==================== 对象基础操作 ====================

    /**
     * 判断默认存储桶下的对象是否存在。
     *
     * @param objectKey 对象 Key
     * @return true 存在，false 不存在
     */
    boolean objectExists(String objectKey);

    /**
     * 判断指定存储桶下的对象是否存在。
     *
     * @param bucketName 存储桶名称
     * @param objectKey  对象 Key
     * @return true 存在，false 不存在
     */
    boolean objectExists(String bucketName, String objectKey);

    /**
     * 获取默认存储桶下的对象信息。
     *
     * @param objectKey 对象 Key
     * @return 对象信息
     */
    S3ObjectInfo getObjectInfo(String objectKey);

    /**
     * 获取指定存储桶下的对象信息。
     *
     * @param bucketName 存储桶名称
     * @param objectKey  对象 Key
     * @return 对象信息
     */
    S3ObjectInfo getObjectInfo(String bucketName, String objectKey);

    /**
     * 获取默认存储桶下的对象大小。
     *
     * @param objectKey 对象 Key
     * @return 对象大小，单位字节
     */
    Long getObjectSize(String objectKey);

    /**
     * 获取指定存储桶下的对象大小。
     *
     * @param bucketName 存储桶名称
     * @param objectKey  对象 Key
     * @return 对象大小，单位字节
     */
    Long getObjectSize(String bucketName, String objectKey);

    /**
     * 获取默认存储桶下对象的 Content-Type。
     *
     * @param objectKey 对象 Key
     * @return Content-Type
     */
    String getObjectContentType(String objectKey);

    /**
     * 获取指定存储桶下对象的 Content-Type。
     *
     * @param bucketName 存储桶名称
     * @param objectKey  对象 Key
     * @return Content-Type
     */
    String getObjectContentType(String bucketName, String objectKey);

    // ==================== 对象上传 ====================

    /**
     * 上传文件到默认存储桶，并自动生成对象 Key。
     *
     * @param file 上传文件
     * @return 上传结果
     */
    S3UploadResult upload(MultipartFile file);

    /**
     * 上传文件到默认存储桶。
     *
     * @param objectKey 对象 Key
     * @param file      上传文件
     * @return 上传结果
     */
    S3UploadResult upload(String objectKey, MultipartFile file);

    /**
     * 上传文件到指定存储桶。
     *
     * @param bucketName 存储桶名称
     * @param objectKey  对象 Key
     * @param file       上传文件
     * @return 上传结果
     */
    S3UploadResult upload(String bucketName, String objectKey, MultipartFile file);

    /**
     * 上传字节数组到默认存储桶。
     *
     * @param bytes     字节数组
     * @param objectKey 对象 Key
     * @return 上传结果
     */
    S3UploadResult upload(byte[] bytes, String objectKey);

    /**
     * 上传字节数组到指定存储桶。
     *
     * @param bucketName 存储桶名称
     * @param objectKey  对象 Key
     * @param bytes      字节数组
     * @return 上传结果
     */
    S3UploadResult upload(String bucketName, String objectKey, byte[] bytes);

    /**
     * 上传输入流到默认存储桶。
     *
     * @param inputStream   输入流
     * @param objectKey     对象 Key
     * @param contentLength 内容长度
     * @return 上传结果
     */
    S3UploadResult upload(InputStream inputStream, String objectKey, long contentLength);

    /**
     * 上传输入流到指定存储桶。
     *
     * @param bucketName    存储桶名称
     * @param objectKey     对象 Key
     * @param inputStream   输入流
     * @param contentLength 内容长度
     * @return 上传结果
     */
    S3UploadResult upload(String bucketName, String objectKey, InputStream inputStream, long contentLength);

    /**
     * 上传本地文件到默认存储桶。
     *
     * @param filePath  本地文件路径
     * @param objectKey 对象 Key
     * @return 上传结果
     */
    S3UploadResult upload(Path filePath, String objectKey);

    /**
     * 上传本地文件到指定存储桶。
     *
     * @param bucketName 存储桶名称
     * @param objectKey  对象 Key
     * @param filePath   本地文件路径
     * @return 上传结果
     */
    S3UploadResult upload(String bucketName, String objectKey, Path filePath);

    /**
     * 根据上传请求上传对象。
     *
     * @param request 上传请求
     * @return 上传结果
     */
    S3UploadResult upload(S3UploadRequest request);

    /**
     * 批量上传对象。
     *
     * @param requests 上传请求列表
     * @return 上传结果列表
     */
    List<S3UploadResult> uploadBatch(List<S3UploadRequest> requests);

    // ==================== 对象下载 ====================

    /**
     * 下载默认存储桶下的对象为字节数组。
     *
     * @param objectKey 对象 Key
     * @return 字节数组
     */
    byte[] downloadAsBytes(String objectKey);

    /**
     * 下载指定存储桶下的对象为字节数组。
     *
     * @param bucketName 存储桶名称
     * @param objectKey  对象 Key
     * @return 字节数组
     */
    byte[] downloadAsBytes(String bucketName, String objectKey);

    /**
     * 下载默认存储桶下的对象为输入流。
     *
     * @param objectKey 对象 Key
     * @return 输入流，调用方负责关闭
     */
    InputStream downloadAsStream(String objectKey);

    /**
     * 下载指定存储桶下的对象为输入流。
     *
     * @param bucketName 存储桶名称
     * @param objectKey  对象 Key
     * @return 输入流，调用方负责关闭
     */
    InputStream downloadAsStream(String bucketName, String objectKey);

    /**
     * 下载默认存储桶下的对象为 Spring Resource。
     *
     * @param objectKey 对象 Key
     * @return Spring Resource
     */
    Resource downloadAsResource(String objectKey);

    /**
     * 下载指定存储桶下的对象为 Spring Resource。
     *
     * @param bucketName 存储桶名称
     * @param objectKey  对象 Key
     * @return Spring Resource
     */
    Resource downloadAsResource(String bucketName, String objectKey);

    /**
     * 下载默认存储桶下的对象到本地文件。
     *
     * @param objectKey  对象 Key
     * @param targetPath 本地目标路径
     */
    void downloadToFile(String objectKey, Path targetPath);

    /**
     * 下载指定存储桶下的对象到本地文件。
     *
     * @param bucketName 存储桶名称
     * @param objectKey  对象 Key
     * @param targetPath 本地目标路径
     */
    void downloadToFile(String bucketName, String objectKey, Path targetPath);

    /**
     * 根据下载请求下载对象。
     *
     * @param request 下载请求
     * @return 下载结果
     */
    S3DownloadResult download(S3DownloadRequest request);

    // ==================== 对象删除 ====================

    /**
     * 删除默认存储桶下的对象。
     *
     * @param objectKey 对象 Key
     */
    void delete(String objectKey);

    /**
     * 删除指定存储桶下的对象。
     *
     * @param bucketName 存储桶名称
     * @param objectKey  对象 Key
     */
    void delete(String bucketName, String objectKey);

    /**
     * 批量删除默认存储桶下的对象。
     *
     * @param objectKeys 对象 Key 列表
     */
    void deleteBatch(List<String> objectKeys);

    /**
     * 批量删除指定存储桶下的对象。
     *
     * @param bucketName 存储桶名称
     * @param objectKeys 对象 Key 列表
     */
    void deleteBatch(String bucketName, List<String> objectKeys);

    /**
     * 删除默认存储桶下指定前缀的对象。
     *
     * @param prefix 对象 Key 前缀
     * @return 删除成功数量
     */
    Long deleteByPrefix(String prefix);

    /**
     * 删除指定存储桶下指定前缀的对象。
     *
     * @param bucketName 存储桶名称
     * @param prefix     对象 Key 前缀
     * @return 删除成功数量
     */
    Long deleteByPrefix(String bucketName, String prefix);

    /**
     * 删除指定版本的对象。
     *
     * @param bucketName 存储桶名称
     * @param objectKey  对象 Key
     * @param versionId  版本 ID
     */
    void deleteVersion(String bucketName, String objectKey, String versionId);

    /**
     * 批量删除默认存储桶下的对象，并返回详细结果。
     *
     * @param objectKeys 对象 Key 列表
     * @return 删除详细结果
     */
    S3DeleteResult deleteBatchDetailed(List<String> objectKeys);

    /**
     * 批量删除指定存储桶下的对象，并返回详细结果。
     *
     * @param bucketName 存储桶名称
     * @param objectKeys 对象 Key 列表
     * @return 删除详细结果
     */
    S3DeleteResult deleteBatchDetailed(String bucketName, List<String> objectKeys);

    /**
     * 删除默认存储桶下指定前缀的对象，并返回详细结果。
     *
     * @param prefix 对象 Key 前缀
     * @return 删除详细结果
     */
    S3DeleteResult deleteByPrefixDetailed(String prefix);

    /**
     * 删除指定存储桶下指定前缀的对象，并返回详细结果。
     *
     * @param bucketName 存储桶名称
     * @param prefix     对象 Key 前缀
     * @return 删除详细结果
     */
    S3DeleteResult deleteByPrefixDetailed(String bucketName, String prefix);

    // ==================== 对象复制与移动 ====================

    /**
     * 在默认存储桶内复制对象。
     *
     * @param sourceKey 源对象 Key
     * @param targetKey 目标对象 Key
     * @return 复制结果
     */
    S3CopyResult copy(String sourceKey, String targetKey);

    /**
     * 复制对象到指定存储桶。
     *
     * @param sourceBucketName 源存储桶名称
     * @param sourceKey        源对象 Key
     * @param targetBucketName 目标存储桶名称
     * @param targetKey        目标对象 Key
     * @return 复制结果
     */
    S3CopyResult copy(String sourceBucketName, String sourceKey, String targetBucketName, String targetKey);

    /**
     * 根据复制请求复制对象。
     *
     * @param request 复制请求
     * @return 复制结果
     */
    S3CopyResult copy(S3CopyRequest request);

    /**
     * 在默认存储桶内移动对象。
     *
     * @param sourceKey 源对象 Key
     * @param targetKey 目标对象 Key
     * @return 移动结果
     */
    S3MoveResult move(String sourceKey, String targetKey);

    /**
     * 移动对象到指定存储桶。
     *
     * @param sourceBucketName 源存储桶名称
     * @param sourceKey        源对象 Key
     * @param targetBucketName 目标存储桶名称
     * @param targetKey        目标对象 Key
     * @return 移动结果
     */
    S3MoveResult move(String sourceBucketName, String sourceKey, String targetBucketName, String targetKey);

    /**
     * 根据移动请求移动对象。
     *
     * @param request 移动请求
     * @return 移动结果
     */
    S3MoveResult move(S3MoveRequest request);

    /**
     * 在默认存储桶内重命名对象。
     *
     * @param sourceKey 源对象 Key
     * @param targetKey 目标对象 Key
     */
    void rename(String sourceKey, String targetKey);

    /**
     * 在指定存储桶内重命名对象。
     *
     * @param bucketName 存储桶名称
     * @param sourceKey  源对象 Key
     * @param targetKey  目标对象 Key
     */
    void rename(String bucketName, String sourceKey, String targetKey);

    // ==================== 对象列表与分页查询 ====================

    /**
     * 查询默认存储桶根层级对象列表。
     *
     * @return 对象信息列表
     */
    List<S3ObjectInfo> listObjects();

    /**
     * 查询默认存储桶指定前缀当前层级对象列表。
     *
     * @param prefix 对象 Key 前缀
     * @return 对象信息列表
     */
    List<S3ObjectInfo> listObjects(String prefix);

    /**
     * 查询指定存储桶指定前缀当前层级对象列表。
     *
     * @param bucketName 存储桶名称
     * @param prefix     对象 Key 前缀
     * @return 对象信息列表
     */
    List<S3ObjectInfo> listObjects(String bucketName, String prefix);

    /**
     * 分页查询对象列表。
     *
     * @param request 列表查询请求
     * @return 对象分页结果
     */
    S3ObjectPage listObjectsPage(S3ListRequest request);

    /**
     * 递归查询默认存储桶指定前缀下的对象列表。
     *
     * @param prefix 对象 Key 前缀
     * @return 对象信息列表
     */
    List<S3ObjectInfo> listObjectsRecursive(String prefix);

    /**
     * 递归查询指定存储桶指定前缀下的对象列表。
     *
     * @param bucketName 存储桶名称
     * @param prefix     对象 Key 前缀
     * @return 对象信息列表
     */
    List<S3ObjectInfo> listObjectsRecursive(String bucketName, String prefix);

    /**
     * 递归查询默认存储桶指定前缀下的对象 Key 列表。
     *
     * @param prefix 对象 Key 前缀
     * @return 对象 Key 列表
     */
    List<String> listObjectKeys(String prefix);

    /**
     * 递归查询指定存储桶指定前缀下的对象 Key 列表。
     *
     * @param bucketName 存储桶名称
     * @param prefix     对象 Key 前缀
     * @return 对象 Key 列表
     */
    List<String> listObjectKeys(String bucketName, String prefix);

    /**
     * 查询对象版本列表。
     *
     * @param bucketName 存储桶名称
     * @param prefix     对象 Key 前缀
     * @return 对象版本信息列表
     */
    List<S3ObjectVersionInfo> listObjectVersions(String bucketName, String prefix);

    // ==================== 目录语义操作 ====================

    /**
     * 在默认存储桶下创建目录占位对象。
     *
     * @param directoryKey 目录 Key
     */
    void createDirectory(String directoryKey);

    /**
     * 在指定存储桶下创建目录占位对象。
     *
     * @param bucketName   存储桶名称
     * @param directoryKey 目录 Key
     */
    void createDirectory(String bucketName, String directoryKey);

    /**
     * 判断默认存储桶下的目录是否存在。
     *
     * @param directoryKey 目录 Key
     * @return true 存在，false 不存在
     */
    boolean directoryExists(String directoryKey);

    /**
     * 判断指定存储桶下的目录是否存在。
     *
     * @param bucketName   存储桶名称
     * @param directoryKey 目录 Key
     * @return true 存在，false 不存在
     */
    boolean directoryExists(String bucketName, String directoryKey);

    /**
     * 删除默认存储桶下的目录及其对象。
     *
     * @param directoryKey 目录 Key
     * @return 删除成功数量
     */
    Long deleteDirectory(String directoryKey);

    /**
     * 删除指定存储桶下的目录及其对象。
     *
     * @param bucketName   存储桶名称
     * @param directoryKey 目录 Key
     * @return 删除成功数量
     */
    Long deleteDirectory(String bucketName, String directoryKey);

    /**
     * 查询默认存储桶下目录当前层级对象列表。
     *
     * @param directoryKey 目录 Key
     * @return 对象信息列表
     */
    List<S3ObjectInfo> listDirectory(String directoryKey);

    /**
     * 查询指定存储桶下目录当前层级对象列表。
     *
     * @param bucketName   存储桶名称
     * @param directoryKey 目录 Key
     * @return 对象信息列表
     */
    List<S3ObjectInfo> listDirectory(String bucketName, String directoryKey);

    // ==================== 预签名 URL ====================

    /**
     * 生成默认存储桶下对象的下载预签名 URL。
     *
     * @param objectKey 对象 Key
     * @return 下载预签名 URL
     */
    URI generateDownloadUrl(String objectKey);

    /**
     * 生成指定存储桶下对象的下载预签名 URL。
     *
     * @param bucketName 存储桶名称
     * @param objectKey  对象 Key
     * @return 下载预签名 URL
     */
    URI generateDownloadUrl(String bucketName, String objectKey);

    /**
     * 生成默认存储桶下对象的下载预签名 URL。
     *
     * @param objectKey 对象 Key
     * @param expire    有效期
     * @return 下载预签名 URL
     */
    URI generateDownloadUrl(String objectKey, Duration expire);

    /**
     * 生成指定存储桶下对象的下载预签名 URL。
     *
     * @param bucketName 存储桶名称
     * @param objectKey  对象 Key
     * @param expire     有效期
     * @return 下载预签名 URL
     */
    URI generateDownloadUrl(String bucketName, String objectKey, Duration expire);

    /**
     * 生成默认存储桶下对象的上传预签名 URL。
     *
     * @param objectKey 对象 Key
     * @param expire    有效期
     * @return 上传预签名 URL
     */
    URI generateUploadUrl(String objectKey, Duration expire);

    /**
     * 生成指定存储桶下对象的上传预签名 URL。
     *
     * @param bucketName 存储桶名称
     * @param objectKey  对象 Key
     * @param expire     有效期
     * @return 上传预签名 URL
     */
    URI generateUploadUrl(String bucketName, String objectKey, Duration expire);

    /**
     * 生成默认存储桶下对象的 HEAD 预签名 URL。
     *
     * @param objectKey 对象 Key
     * @param expire    有效期
     * @return HEAD 预签名 URL
     */
    URI generateHeadUrl(String objectKey, Duration expire);

    /**
     * 生成指定存储桶下对象的 HEAD 预签名 URL。
     *
     * @param bucketName 存储桶名称
     * @param objectKey  对象 Key
     * @param expire     有效期
     * @return HEAD 预签名 URL
     */
    URI generateHeadUrl(String bucketName, String objectKey, Duration expire);

    /**
     * 根据请求生成预签名 URL。
     *
     * @param request 预签名 URL 请求
     * @return 预签名 URL 结果
     */
    S3PresignedUrlResult generatePresignedUrl(S3PresignedUrlRequest request);

    /**
     * 生成默认存储桶下对象的删除预签名 URL。
     *
     * @param objectKey 对象 Key
     * @param expire    有效期
     * @return 删除预签名 URL
     */
    URI generateDeleteUrl(String objectKey, Duration expire);

    /**
     * 生成指定存储桶下对象的删除预签名 URL。
     *
     * @param bucketName 存储桶名称
     * @param objectKey  对象 Key
     * @param expire     有效期
     * @return 删除预签名 URL
     */
    URI generateDeleteUrl(String bucketName, String objectKey, Duration expire);

    // ==================== 元数据管理 ====================

    /**
     * 获取默认存储桶下对象的自定义元数据。
     *
     * @param objectKey 对象 Key
     * @return 自定义元数据
     */
    Map<String, String> getMetadata(String objectKey);

    /**
     * 获取指定存储桶下对象的自定义元数据。
     *
     * @param bucketName 存储桶名称
     * @param objectKey  对象 Key
     * @return 自定义元数据
     */
    Map<String, String> getMetadata(String bucketName, String objectKey);

    /**
     * 替换默认存储桶下对象的自定义元数据。
     *
     * @param objectKey 对象 Key
     * @param metadata  自定义元数据
     */
    void replaceMetadata(String objectKey, Map<String, String> metadata);

    /**
     * 替换指定存储桶下对象的自定义元数据。
     *
     * @param bucketName 存储桶名称
     * @param objectKey  对象 Key
     * @param metadata   自定义元数据
     */
    void replaceMetadata(String bucketName, String objectKey, Map<String, String> metadata);

    /**
     * 根据请求替换对象自定义元数据。
     *
     * @param request 元数据替换请求
     */
    void replaceMetadata(S3MetadataRequest request);

    // ==================== 标签管理 ====================

    /**
     * 获取默认存储桶下对象标签。
     *
     * @param objectKey 对象 Key
     * @return 标签 Map
     */
    Map<String, String> getTags(String objectKey);

    /**
     * 获取指定存储桶下对象标签。
     *
     * @param bucketName 存储桶名称
     * @param objectKey  对象 Key
     * @return 标签 Map
     */
    Map<String, String> getTags(String bucketName, String objectKey);

    /**
     * 写入默认存储桶下对象标签。
     *
     * @param objectKey 对象 Key
     * @param tags      标签 Map
     */
    void putTags(String objectKey, Map<String, String> tags);

    /**
     * 写入指定存储桶下对象标签。
     *
     * @param bucketName 存储桶名称
     * @param objectKey  对象 Key
     * @param tags       标签 Map
     */
    void putTags(String bucketName, String objectKey, Map<String, String> tags);

    /**
     * 删除默认存储桶下对象标签。
     *
     * @param objectKey 对象 Key
     */
    void deleteTags(String objectKey);

    /**
     * 删除指定存储桶下对象标签。
     *
     * @param bucketName 存储桶名称
     * @param objectKey  对象 Key
     */
    void deleteTags(String bucketName, String objectKey);

    /**
     * 获取指定版本对象的标签。
     *
     * @param bucketName 存储桶名称
     * @param objectKey  对象 Key
     * @param versionId  版本 ID
     * @return 标签 Map
     */
    Map<String, String> getTags(String bucketName, String objectKey, String versionId);

    /**
     * 写入指定版本对象的标签。
     *
     * @param bucketName 存储桶名称
     * @param objectKey  对象 Key
     * @param versionId  版本 ID
     * @param tags       标签 Map
     */
    void putTags(String bucketName, String objectKey, String versionId, Map<String, String> tags);

    /**
     * 删除指定版本对象的标签。
     *
     * @param bucketName 存储桶名称
     * @param objectKey  对象 Key
     * @param versionId  版本 ID
     */
    void deleteTags(String bucketName, String objectKey, String versionId);

    // ==================== 访问控制与存储属性 ====================

    /**
     * 设置默认存储桶下对象 ACL。
     *
     * @param objectKey 对象 Key
     * @param acl       对象 ACL
     */
    void setObjectAcl(String objectKey, S3ObjectAcl acl);

    /**
     * 设置指定存储桶下对象 ACL。
     *
     * @param bucketName 存储桶名称
     * @param objectKey  对象 Key
     * @param acl        对象 ACL
     */
    void setObjectAcl(String bucketName, String objectKey, S3ObjectAcl acl);

    /**
     * 修改默认存储桶下对象存储类型。
     *
     * @param objectKey    对象 Key
     * @param storageClass 存储类型
     */
    void changeStorageClass(String objectKey, S3StorageClass storageClass);

    /**
     * 修改指定存储桶下对象存储类型。
     *
     * @param bucketName   存储桶名称
     * @param objectKey    对象 Key
     * @param storageClass 存储类型
     */
    void changeStorageClass(String bucketName, String objectKey, S3StorageClass storageClass);

    /**
     * 恢复默认存储桶下归档对象。
     *
     * @param objectKey 对象 Key
     * @param days      恢复副本保留天数
     */
    void restoreArchiveObject(String objectKey, Integer days);

    /**
     * 恢复指定存储桶下归档对象。
     *
     * @param bucketName 存储桶名称
     * @param objectKey  对象 Key
     * @param days       恢复副本保留天数
     */
    void restoreArchiveObject(String bucketName, String objectKey, Integer days);

    // ==================== 分片上传 ====================

    /**
     * 初始化分片上传。
     *
     * @param request 分片上传初始化请求
     * @return 分片上传初始化结果
     */
    S3MultipartUploadInitResult initMultipartUpload(S3MultipartUploadInitRequest request);

    /**
     * 上传分片。
     *
     * @param request 分片上传请求
     * @return 分片上传结果
     */
    S3MultipartUploadPartResult uploadPart(S3MultipartUploadPartRequest request);

    /**
     * 完成分片上传。
     *
     * @param request 分片上传完成请求
     * @return 上传结果
     */
    S3UploadResult completeMultipartUpload(S3MultipartUploadCompleteRequest request);

    /**
     * 终止分片上传。
     *
     * @param bucketName 存储桶名称
     * @param objectKey  对象 Key
     * @param uploadId   上传 ID
     */
    void abortMultipartUpload(String bucketName, String objectKey, String uploadId);

    /**
     * 查询未完成的分片上传任务。
     *
     * @param bucketName 存储桶名称
     * @param prefix     对象 Key 前缀
     * @return 分片上传任务列表
     */
    List<S3MultipartUploadInfo> listMultipartUploads(String bucketName, String prefix);

    /**
     * 查询指定分片上传任务已上传的分片列表。
     *
     * @param bucketName 存储桶名称
     * @param objectKey  对象 Key
     * @param uploadId   上传 ID
     * @return 已上传分片列表
     */
    List<S3MultipartUploadPartInfo> listMultipartUploadParts(String bucketName, String objectKey, String uploadId);

    // ==================== 工具方法 ====================

    /**
     * 规范化对象 Key。
     *
     * @param objectKey 对象 Key
     * @return 规范化后的对象 Key
     */
    String normalizeObjectKey(String objectKey);

    /**
     * 构建对象 Key。
     *
     * @param directory 目录
     * @param filename  文件名
     * @return 对象 Key
     */
    String buildObjectKey(String directory, String filename);

    /**
     * 获取对象 Key 中的文件名。
     *
     * @param objectKey 对象 Key
     * @return 文件名
     */
    String getFilename(String objectKey);

    /**
     * 获取对象 Key 中的扩展名。
     *
     * @param objectKey 对象 Key
     * @return 扩展名，不包含点号
     */
    String getExtension(String objectKey);

    /**
     * 获取默认存储桶下对象的公开访问 URL。
     *
     * @param objectKey 对象 Key
     * @return 公开访问 URL
     */
    String getPublicUrl(String objectKey);

    /**
     * 获取指定存储桶下对象的公开访问 URL。
     *
     * @param bucketName 存储桶名称
     * @param objectKey  对象 Key
     * @return 公开访问 URL
     */
    String getPublicUrl(String bucketName, String objectKey);

}
```



## 创建服务实现

```java
package io.github.atengk.aws.s3.service.impl;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.date.DateUtil;
import cn.hutool.core.io.file.FileNameUtil;
import cn.hutool.core.lang.Assert;
import cn.hutool.core.map.MapUtil;
import cn.hutool.core.util.BooleanUtil;
import cn.hutool.core.util.IdUtil;
import cn.hutool.core.util.StrUtil;
import io.github.atengk.aws.s3.config.S3Properties;
import io.github.atengk.aws.s3.model.enums.S3ObjectAcl;
import io.github.atengk.aws.s3.model.enums.S3PresignedUrlMethod;
import io.github.atengk.aws.s3.model.enums.S3ServerSideEncryption;
import io.github.atengk.aws.s3.model.enums.S3StorageClass;
import io.github.atengk.aws.s3.model.info.S3MultipartUploadInfo;
import io.github.atengk.aws.s3.model.info.S3MultipartUploadPartInfo;
import io.github.atengk.aws.s3.model.info.S3ObjectInfo;
import io.github.atengk.aws.s3.model.info.S3ObjectVersionInfo;
import io.github.atengk.aws.s3.model.request.*;
import io.github.atengk.aws.s3.model.result.*;
import io.github.atengk.aws.s3.service.S3Service;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.InputStreamResource;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import software.amazon.awssdk.awscore.AwsRequestOverrideConfiguration;
import software.amazon.awssdk.core.ResponseBytes;
import software.amazon.awssdk.core.ResponseInputStream;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.core.sync.ResponseTransformer;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.*;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;
import software.amazon.awssdk.services.s3.presigner.model.*;

import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.net.URI;
import java.net.URLConnection;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.*;

/**
 * S3 文件服务实现
 *
 * @author Ateng
 * @since 2026-04-28
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class S3ServiceImpl implements S3Service {

    private final S3Client s3Client;

    private final S3Presigner s3Presigner;

    private final S3Properties s3Properties;

    private static final String REGION_US_EAST_1 = "us-east-1";

    private static final String DEFAULT_CONTENT_TYPE = "application/octet-stream";

    private static final String DEFAULT_UPLOAD_DIRECTORY = "upload";

    private static final DateTimeFormatter DEFAULT_DATE_PATH_FORMATTER = DateTimeFormatter.ofPattern("yyyy/MM/dd");

    private static final Duration AWS_S3_MAX_PRESIGN_EXPIRE = Duration.ofDays(7);

    private static final Duration DEFAULT_MIN_PRESIGN_EXPIRE = Duration.ofSeconds(1);

    // ==================== 存储桶管理 ====================

    /**
     * 判断存储桶是否存在
     *
     * @param bucketName 存储桶名称
     * @return true 存在，false 不存在
     */
    @Override
    public boolean bucketExists(String bucketName) {
        String resolvedBucketName = resolveBucketName(bucketName);

        try {
            HeadBucketRequest request = HeadBucketRequest.builder()
                    .bucket(resolvedBucketName)
                    .build();

            s3Client.headBucket(request);
            return true;
        } catch (NoSuchBucketException e) {
            log.info("S3 存储桶不存在，bucketName={}", resolvedBucketName);
            return false;
        } catch (S3Exception e) {
            if (isNotFound(e)) {
                log.info("S3 存储桶不存在，bucketName={}", resolvedBucketName);
                return false;
            }

            if (isForbidden(e)) {
                log.warn("S3 存储桶存在但当前凭证无访问权限，bucketName={}，errorCode={}",
                        resolvedBucketName, getErrorCode(e));
                return true;
            }

            log.error("检查 S3 存储桶失败，bucketName={}，statusCode={}，errorCode={}，message={}",
                    resolvedBucketName, e.statusCode(), getErrorCode(e), e.getMessage(), e);
            throw e;
        }
    }

    /**
     * 创建存储桶
     *
     * @param bucketName 存储桶名称
     */
    @Override
    public void createBucket(String bucketName) {
        String resolvedBucketName = resolveBucketName(bucketName);

        try {
            CreateBucketRequest request = buildCreateBucketRequest(resolvedBucketName);
            s3Client.createBucket(request);

            log.info("创建 S3 存储桶成功，bucketName={}，region={}",
                    resolvedBucketName, s3Properties.getRegion());
        } catch (BucketAlreadyOwnedByYouException e) {
            log.info("S3 存储桶已存在且归当前凭证所有，bucketName={}", resolvedBucketName);
        } catch (BucketAlreadyExistsException e) {
            log.error("S3 存储桶已被其他账号占用，bucketName={}", resolvedBucketName, e);
            throw e;
        } catch (S3Exception e) {
            if (isBucketAlreadyOwnedByYou(e)) {
                log.info("S3 存储桶已存在且归当前凭证所有，bucketName={}，errorCode={}",
                        resolvedBucketName, getErrorCode(e));
                return;
            }

            if (isBucketAlreadyExists(e)) {
                log.error("S3 存储桶已存在或已被占用，bucketName={}，statusCode={}，errorCode={}",
                        resolvedBucketName, e.statusCode(), getErrorCode(e), e);
                throw e;
            }

            log.error("创建 S3 存储桶失败，bucketName={}，statusCode={}，errorCode={}，message={}",
                    resolvedBucketName, e.statusCode(), getErrorCode(e), e.getMessage(), e);
            throw e;
        }
    }

    /**
     * 存储桶不存在时创建
     *
     * @param bucketName 存储桶名称
     */
    @Override
    public void createBucketIfAbsent(String bucketName) {
        String resolvedBucketName = resolveBucketName(bucketName);

        if (bucketExists(resolvedBucketName)) {
            log.info("S3 存储桶已存在，跳过创建，bucketName={}", resolvedBucketName);
            return;
        }

        createBucket(resolvedBucketName);
    }

    /**
     * 删除存储桶
     *
     * @param bucketName 存储桶名称
     */
    @Override
    public void deleteBucket(String bucketName) {
        String resolvedBucketName = resolveBucketName(bucketName);

        try {
            DeleteBucketRequest request = DeleteBucketRequest.builder()
                    .bucket(resolvedBucketName)
                    .build();

            s3Client.deleteBucket(request);
            log.info("删除 S3 存储桶成功，bucketName={}", resolvedBucketName);
        } catch (NoSuchBucketException e) {
            log.warn("删除 S3 存储桶时发现存储桶不存在，bucketName={}", resolvedBucketName);
            throw e;
        } catch (S3Exception e) {
            log.error("删除 S3 存储桶失败，bucketName={}，statusCode={}，errorCode={}，message={}",
                    resolvedBucketName, e.statusCode(), getErrorCode(e), e.getMessage(), e);
            throw e;
        }
    }

    /**
     * 查询当前凭证可见的存储桶列表
     *
     * @return 存储桶名称列表
     */
    @Override
    public List<String> listBuckets() {
        try {
            ListBucketsResponse response = s3Client.listBuckets();

            return response.buckets()
                    .stream()
                    .map(Bucket::name)
                    .filter(StrUtil::isNotBlank)
                    .toList();
        } catch (S3Exception e) {
            log.error("查询 S3 存储桶列表失败，statusCode={}，errorCode={}，message={}",
                    e.statusCode(), getErrorCode(e), e.getMessage(), e);
            throw e;
        }
    }

    /**
     * 获取默认存储桶名称
     *
     * @return 默认存储桶名称
     */
    @Override
    public String getDefaultBucketName() {
        String bucketName = StrUtil.trim(s3Properties.getBucketName());
        Assert.notBlank(bucketName, "S3 默认存储桶名称不能为空");
        return bucketName;
    }

    /**
     * 解析存储桶名称，为空时使用默认存储桶
     *
     * @param bucketName 存储桶名称
     * @return 最终存储桶名称
     */
    @Override
    public String resolveBucketName(String bucketName) {
        String resolvedBucketName = StrUtil.blankToDefault(StrUtil.trim(bucketName), getDefaultBucketName());
        Assert.notBlank(resolvedBucketName, "S3 存储桶名称不能为空");
        return resolvedBucketName;
    }

    // ==================== 工具方法 ====================

    /**
     * 规范化对象 Key
     *
     * @param objectKey 对象 Key
     * @return 规范化后的对象 Key
     */
    @Override
    public String normalizeObjectKey(String objectKey) {
        String normalizedObjectKey = StrUtil.trim(objectKey);
        Assert.notBlank(normalizedObjectKey, "S3 对象 Key 不能为空");

        normalizedObjectKey = StrUtil.replace(normalizedObjectKey, "\\", "/");
        normalizedObjectKey = normalizedObjectKey.replaceAll("/{2,}", "/");

        while (StrUtil.startWith(normalizedObjectKey, "/")) {
            normalizedObjectKey = StrUtil.removePrefix(normalizedObjectKey, "/");
        }

        while (StrUtil.startWith(normalizedObjectKey, "./")) {
            normalizedObjectKey = StrUtil.removePrefix(normalizedObjectKey, "./");
        }

        Assert.notBlank(normalizedObjectKey, "S3 对象 Key 不能为空");
        return normalizedObjectKey;
    }

    /**
     * 构建对象 Key
     *
     * @param directory 目录
     * @param filename  文件名
     * @return 对象 Key
     */
    @Override
    public String buildObjectKey(String directory, String filename) {
        String normalizedFilename = normalizeObjectKey(filename);

        if (StrUtil.isBlank(directory)) {
            return normalizedFilename;
        }

        String normalizedDirectory = normalizeObjectKey(directory);
        normalizedDirectory = StrUtil.removeSuffix(normalizedDirectory, "/");

        return normalizedDirectory + "/" + normalizedFilename;
    }

    /**
     * 获取文件名
     *
     * @param objectKey 对象 Key
     * @return 文件名
     */
    @Override
    public String getFilename(String objectKey) {
        String normalizedObjectKey = normalizeObjectKey(objectKey);
        String keyWithoutTrailingSlash = StrUtil.removeSuffix(normalizedObjectKey, "/");

        if (StrUtil.isBlank(keyWithoutTrailingSlash)) {
            return StrUtil.EMPTY;
        }

        return FileNameUtil.getName(keyWithoutTrailingSlash);
    }

    /**
     * 获取文件扩展名
     *
     * @param objectKey 对象 Key
     * @return 文件扩展名，不包含点号
     */
    @Override
    public String getExtension(String objectKey) {
        String filename = getFilename(objectKey);

        if (StrUtil.isBlank(filename)) {
            return StrUtil.EMPTY;
        }

        return StrUtil.blankToDefault(FileNameUtil.extName(filename), StrUtil.EMPTY);
    }

    /**
     * 获取默认存储桶下对象的公开访问 URL
     *
     * @param objectKey 对象 Key
     * @return 公开访问 URL
     */
    @Override
    public String getPublicUrl(String objectKey) {
        return getPublicUrl(getDefaultBucketName(), objectKey);
    }

    /**
     * 获取指定存储桶下对象的公开访问 URL
     *
     * @param bucketName 存储桶名称
     * @param objectKey  对象 Key
     * @return 公开访问 URL
     */
    @Override
    public String getPublicUrl(String bucketName, String objectKey) {
        String resolvedBucketName = resolveBucketName(bucketName);
        String normalizedObjectKey = normalizeObjectKey(objectKey);
        String encodedObjectKey = encodeObjectKey(normalizedObjectKey);

        String endpoint = normalizeEndpoint(s3Properties.getEndpoint());
        boolean pathStyleAccess = BooleanUtil.isTrue(s3Properties.getPathStyleAccess());

        if (StrUtil.isBlank(endpoint)) {
            return buildAwsDefaultPublicUrl(resolvedBucketName, encodedObjectKey, pathStyleAccess);
        }

        URI endpointUri = URI.create(endpoint);
        Assert.notBlank(endpointUri.getScheme(), "S3 endpoint 必须包含协议，例如：https://minio.example.com");
        Assert.notBlank(endpointUri.getHost(), "S3 endpoint 必须包含主机，例如：https://minio.example.com");

        if (pathStyleAccess || isEndpointNotSuitableForVirtualHost(endpointUri)) {
            return joinUrlPath(endpoint, encodePathSegment(resolvedBucketName), encodedObjectKey);
        }

        return buildVirtualHostPublicUrl(endpointUri, resolvedBucketName, encodedObjectKey);
    }

    // ==================== 对象基础操作 ====================

    /**
     * 判断默认存储桶下的对象是否存在
     *
     * @param objectKey 对象 Key
     * @return true 存在，false 不存在
     */
    @Override
    public boolean objectExists(String objectKey) {
        return objectExists(getDefaultBucketName(), objectKey);
    }

    /**
     * 判断指定存储桶下的对象是否存在
     *
     * @param bucketName 存储桶名称
     * @param objectKey  对象 Key
     * @return true 存在，false 不存在
     */
    @Override
    public boolean objectExists(String bucketName, String objectKey) {
        String resolvedBucketName = resolveBucketName(bucketName);
        String normalizedObjectKey = normalizeObjectKey(objectKey);

        try {
            HeadObjectRequest request = buildHeadObjectRequest(resolvedBucketName, normalizedObjectKey);
            s3Client.headObject(request);
            return true;
        } catch (NoSuchKeyException e) {
            log.info("S3 对象不存在，bucketName={}，objectKey={}", resolvedBucketName, normalizedObjectKey);
            return false;
        } catch (NoSuchBucketException e) {
            log.info("S3 存储桶不存在，bucketName={}，objectKey={}", resolvedBucketName, normalizedObjectKey);
            return false;
        } catch (S3Exception e) {
            if (isNotFound(e)) {
                log.info("S3 对象不存在，bucketName={}，objectKey={}，errorCode={}",
                        resolvedBucketName, normalizedObjectKey, getErrorCode(e));
                return false;
            }

            log.error("检查 S3 对象是否存在失败，bucketName={}，objectKey={}，statusCode={}，errorCode={}，message={}",
                    resolvedBucketName, normalizedObjectKey, e.statusCode(), getErrorCode(e), e.getMessage(), e);
            throw e;
        }
    }

    /**
     * 获取默认存储桶下的对象信息
     *
     * @param objectKey 对象 Key
     * @return 对象信息
     */
    @Override
    public S3ObjectInfo getObjectInfo(String objectKey) {
        return getObjectInfo(getDefaultBucketName(), objectKey);
    }

    /**
     * 获取指定存储桶下的对象信息
     *
     * @param bucketName 存储桶名称
     * @param objectKey  对象 Key
     * @return 对象信息
     */
    @Override
    public S3ObjectInfo getObjectInfo(String bucketName, String objectKey) {
        String resolvedBucketName = resolveBucketName(bucketName);
        String normalizedObjectKey = normalizeObjectKey(objectKey);

        try {
            HeadObjectRequest request = buildHeadObjectRequest(resolvedBucketName, normalizedObjectKey);
            HeadObjectResponse response = s3Client.headObject(request);

            return buildS3ObjectInfo(resolvedBucketName, normalizedObjectKey, response);
        } catch (NoSuchKeyException e) {
            log.warn("获取 S3 对象信息失败，对象不存在，bucketName={}，objectKey={}",
                    resolvedBucketName, normalizedObjectKey);
            throw e;
        } catch (NoSuchBucketException e) {
            log.warn("获取 S3 对象信息失败，存储桶不存在，bucketName={}，objectKey={}",
                    resolvedBucketName, normalizedObjectKey);
            throw e;
        } catch (S3Exception e) {
            log.error("获取 S3 对象信息失败，bucketName={}，objectKey={}，statusCode={}，errorCode={}，message={}",
                    resolvedBucketName, normalizedObjectKey, e.statusCode(), getErrorCode(e), e.getMessage(), e);
            throw e;
        }
    }

    /**
     * 获取默认存储桶下的对象大小
     *
     * @param objectKey 对象 Key
     * @return 对象大小，单位字节
     */
    @Override
    public Long getObjectSize(String objectKey) {
        return getObjectSize(getDefaultBucketName(), objectKey);
    }

    /**
     * 获取指定存储桶下的对象大小
     *
     * @param bucketName 存储桶名称
     * @param objectKey  对象 Key
     * @return 对象大小，单位字节
     */
    @Override
    public Long getObjectSize(String bucketName, String objectKey) {
        return getObjectInfo(bucketName, objectKey).size();
    }

    /**
     * 获取默认存储桶下的对象 Content-Type
     *
     * @param objectKey 对象 Key
     * @return Content-Type
     */
    @Override
    public String getObjectContentType(String objectKey) {
        return getObjectContentType(getDefaultBucketName(), objectKey);
    }

    /**
     * 获取指定存储桶下的对象 Content-Type
     *
     * @param bucketName 存储桶名称
     * @param objectKey  对象 Key
     * @return Content-Type
     */
    @Override
    public String getObjectContentType(String bucketName, String objectKey) {
        return getObjectInfo(bucketName, objectKey).contentType();
    }

    // ==================== 对象上传 ====================

    /**
     * 上传 MultipartFile 到默认存储桶，自动生成对象 Key
     *
     * @param file 上传文件
     * @return 上传结果
     */
    @Override
    public S3UploadResult upload(MultipartFile file) {
        Assert.notNull(file, "上传文件不能为空");

        String objectKey = buildAutoObjectKey(file.getOriginalFilename());
        return upload(objectKey, file);
    }

    /**
     * 上传 MultipartFile 到默认存储桶
     *
     * @param objectKey 对象 Key
     * @param file      上传文件
     * @return 上传结果
     */
    @Override
    public S3UploadResult upload(String objectKey, MultipartFile file) {
        return upload(getDefaultBucketName(), objectKey, file);
    }

    /**
     * 上传 MultipartFile 到指定存储桶
     *
     * @param bucketName 存储桶名称
     * @param objectKey  对象 Key
     * @param file       上传文件
     * @return 上传结果
     */
    @Override
    public S3UploadResult upload(String bucketName, String objectKey, MultipartFile file) {
        Assert.notNull(file, "上传文件不能为空");

        String resolvedBucketName = resolveBucketName(bucketName);
        String normalizedObjectKey = normalizeObjectKey(objectKey);
        String originalFilename = normalizeOriginalFilename(file.getOriginalFilename());
        long contentLength = file.getSize();
        String contentType = resolveContentType(file.getContentType(), normalizedObjectKey);

        try (InputStream inputStream = file.getInputStream()) {
            S3UploadRequest request = new S3UploadRequest(
                    resolvedBucketName,
                    normalizedObjectKey,
                    inputStream,
                    contentLength,
                    contentType,
                    Map.of(),
                    Map.of(),
                    null,
                    null,
                    null,
                    null
            );

            return uploadInternal(request, originalFilename, RequestBody.fromInputStream(inputStream, contentLength));
        } catch (IOException e) {
            log.error("读取 MultipartFile 上传流失败，bucketName={}，objectKey={}，originalFilename={}",
                    resolvedBucketName, normalizedObjectKey, originalFilename, e);
            throw new UncheckedIOException("读取 MultipartFile 上传流失败", e);
        }
    }

    /**
     * 上传字节数组到默认存储桶
     *
     * @param bytes     字节数组
     * @param objectKey 对象 Key
     * @return 上传结果
     */
    @Override
    public S3UploadResult upload(byte[] bytes, String objectKey) {
        return upload(getDefaultBucketName(), objectKey, bytes);
    }

    /**
     * 上传字节数组到指定存储桶
     *
     * @param bucketName 存储桶名称
     * @param objectKey  对象 Key
     * @param bytes      字节数组
     * @return 上传结果
     */
    @Override
    public S3UploadResult upload(String bucketName, String objectKey, byte[] bytes) {
        Assert.notNull(bytes, "上传字节数组不能为空");

        String resolvedBucketName = resolveBucketName(bucketName);
        String normalizedObjectKey = normalizeObjectKey(objectKey);
        long contentLength = bytes.length;

        S3UploadRequest request = new S3UploadRequest(
                resolvedBucketName,
                normalizedObjectKey,
                null,
                contentLength,
                resolveContentType(null, normalizedObjectKey),
                Map.of(),
                Map.of(),
                null,
                null,
                null,
                null
        );

        return uploadInternal(request, getFilename(normalizedObjectKey), RequestBody.fromBytes(bytes));
    }

    /**
     * 上传输入流到默认存储桶
     *
     * @param inputStream   输入流
     * @param objectKey     对象 Key
     * @param contentLength 内容长度
     * @return 上传结果
     */
    @Override
    public S3UploadResult upload(InputStream inputStream, String objectKey, long contentLength) {
        return upload(getDefaultBucketName(), objectKey, inputStream, contentLength);
    }

    /**
     * 上传输入流到指定存储桶
     *
     * @param bucketName    存储桶名称
     * @param objectKey     对象 Key
     * @param inputStream   输入流
     * @param contentLength 内容长度
     * @return 上传结果
     */
    @Override
    public S3UploadResult upload(String bucketName, String objectKey, InputStream inputStream, long contentLength) {
        Assert.notNull(inputStream, "上传输入流不能为空");
        Assert.isTrue(contentLength >= 0, "S3 上传内容长度不能小于 0");

        String resolvedBucketName = resolveBucketName(bucketName);
        String normalizedObjectKey = normalizeObjectKey(objectKey);

        S3UploadRequest request = new S3UploadRequest(
                resolvedBucketName,
                normalizedObjectKey,
                inputStream,
                contentLength,
                resolveContentType(null, normalizedObjectKey),
                Map.of(),
                Map.of(),
                null,
                null,
                null,
                null
        );

        return upload(request);
    }

    /**
     * 上传本地文件到默认存储桶
     *
     * @param filePath  本地文件路径
     * @param objectKey 对象 Key
     * @return 上传结果
     */
    @Override
    public S3UploadResult upload(Path filePath, String objectKey) {
        return upload(getDefaultBucketName(), objectKey, filePath);
    }

    /**
     * 上传本地文件到指定存储桶
     *
     * @param bucketName 存储桶名称
     * @param objectKey  对象 Key
     * @param filePath   本地文件路径
     * @return 上传结果
     */
    @Override
    public S3UploadResult upload(String bucketName, String objectKey, Path filePath) {
        Assert.notNull(filePath, "上传文件路径不能为空");
        Assert.isTrue(Files.exists(filePath), "上传文件不存在：{}", filePath);
        Assert.isTrue(Files.isRegularFile(filePath), "上传路径不是普通文件：{}", filePath);

        String resolvedBucketName = resolveBucketName(bucketName);
        String normalizedObjectKey = normalizeObjectKey(objectKey);
        String originalFilename = filePath.getFileName() == null ? getFilename(normalizedObjectKey) : filePath.getFileName().toString();

        try {
            long contentLength = Files.size(filePath);
            String contentType = resolveContentType(Files.probeContentType(filePath), normalizedObjectKey);

            S3UploadRequest request = new S3UploadRequest(
                    resolvedBucketName,
                    normalizedObjectKey,
                    null,
                    contentLength,
                    contentType,
                    Map.of(),
                    Map.of(),
                    null,
                    null,
                    null,
                    null
            );

            return uploadInternal(request, originalFilename, RequestBody.fromFile(filePath));
        } catch (IOException e) {
            log.error("读取本地文件上传失败，bucketName={}，objectKey={}，filePath={}",
                    resolvedBucketName, normalizedObjectKey, filePath, e);
            throw new UncheckedIOException("读取本地文件上传失败：" + filePath, e);
        }
    }

    /**
     * 根据上传请求上传对象
     *
     * @param request 上传请求
     * @return 上传结果
     */
    @Override
    public S3UploadResult upload(S3UploadRequest request) {
        Assert.notNull(request, "S3 上传请求不能为空");
        Assert.notNull(request.inputStream(), "S3 上传输入流不能为空");
        Assert.notNull(request.contentLength(), "S3 上传内容长度不能为空");
        Assert.isTrue(request.contentLength() >= 0, "S3 上传内容长度不能小于 0");

        String normalizedObjectKey = normalizeObjectKey(request.objectKey());

        return uploadInternal(
                request,
                getFilename(normalizedObjectKey),
                RequestBody.fromInputStream(request.inputStream(), request.contentLength())
        );
    }

    /**
     * 批量上传对象
     *
     * @param requests 上传请求列表
     * @return 上传结果列表
     */
    @Override
    public List<S3UploadResult> uploadBatch(List<S3UploadRequest> requests) {
        if (CollUtil.isEmpty(requests)) {
            return List.of();
        }

        List<S3UploadResult> results = new ArrayList<>(requests.size());

        for (int i = 0; i < requests.size(); i++) {
            S3UploadRequest request = requests.get(i);
            Assert.notNull(request, "S3 批量上传请求不能为空，index={}", i);

            try {
                results.add(upload(request));
            } catch (RuntimeException e) {
                log.error("S3 批量上传失败，index={}，bucketName={}，objectKey={}",
                        i, request.bucketName(), request.objectKey(), e);
                throw e;
            }
        }

        log.info("S3 批量上传完成，total={}", results.size());
        return results;
    }

    // ==================== 对象下载 ====================

    /**
     * 下载默认存储桶下的对象为字节数组
     *
     * @param objectKey 对象 Key
     * @return 字节数组
     */
    @Override
    public byte[] downloadAsBytes(String objectKey) {
        return downloadAsBytes(getDefaultBucketName(), objectKey);
    }

    /**
     * 下载指定存储桶下的对象为字节数组
     *
     * @param bucketName 存储桶名称
     * @param objectKey  对象 Key
     * @return 字节数组
     */
    @Override
    public byte[] downloadAsBytes(String bucketName, String objectKey) {
        String resolvedBucketName = resolveBucketName(bucketName);
        String normalizedObjectKey = normalizeObjectKey(objectKey);

        try {
            GetObjectRequest request = buildGetObjectRequest(resolvedBucketName, normalizedObjectKey);
            ResponseBytes<GetObjectResponse> responseBytes = s3Client.getObject(request, ResponseTransformer.toBytes());

            log.info("下载 S3 对象为字节数组成功，bucketName={}，objectKey={}，size={}",
                    resolvedBucketName, normalizedObjectKey, responseBytes.asByteArray().length);

            return responseBytes.asByteArray();
        } catch (NoSuchKeyException e) {
            log.warn("下载 S3 对象失败，对象不存在，bucketName={}，objectKey={}",
                    resolvedBucketName, normalizedObjectKey);
            throw e;
        } catch (NoSuchBucketException e) {
            log.warn("下载 S3 对象失败，存储桶不存在，bucketName={}，objectKey={}",
                    resolvedBucketName, normalizedObjectKey);
            throw e;
        } catch (S3Exception e) {
            log.error("下载 S3 对象为字节数组失败，bucketName={}，objectKey={}，statusCode={}，errorCode={}，message={}",
                    resolvedBucketName, normalizedObjectKey, e.statusCode(), getErrorCode(e), e.getMessage(), e);
            throw e;
        }
    }

    /**
     * 下载默认存储桶下的对象为输入流
     *
     * @param objectKey 对象 Key
     * @return 输入流，调用方需要关闭
     */
    @Override
    public InputStream downloadAsStream(String objectKey) {
        return downloadAsStream(getDefaultBucketName(), objectKey);
    }

    /**
     * 下载指定存储桶下的对象为输入流
     *
     * @param bucketName 存储桶名称
     * @param objectKey  对象 Key
     * @return 输入流，调用方需要关闭
     */
    @Override
    public InputStream downloadAsStream(String bucketName, String objectKey) {
        S3DownloadRequest request = new S3DownloadRequest(
                bucketName,
                objectKey,
                null,
                null,
                null
        );

        return download(request).inputStream();
    }

    /**
     * 下载默认存储桶下的对象为 Spring Resource
     *
     * @param objectKey 对象 Key
     * @return Spring Resource
     */
    @Override
    public Resource downloadAsResource(String objectKey) {
        return downloadAsResource(getDefaultBucketName(), objectKey);
    }

    /**
     * 下载指定存储桶下的对象为 Spring Resource
     *
     * @param bucketName 存储桶名称
     * @param objectKey  对象 Key
     * @return Spring Resource
     */
    @Override
    public Resource downloadAsResource(String bucketName, String objectKey) {
        S3DownloadRequest request = new S3DownloadRequest(
                bucketName,
                objectKey,
                null,
                null,
                null
        );

        S3DownloadResult result = download(request);

        return new InputStreamResource(result.inputStream()) {

            /**
             * 获取资源文件名
             *
             * @return 文件名
             */
            @Override
            public String getFilename() {
                return result.filename();
            }

            /**
             * 获取资源内容长度
             *
             * @return 内容长度
             */
            @Override
            public long contentLength() {
                return result.contentLength() == null ? -1L : result.contentLength();
            }
        };
    }

    /**
     * 下载默认存储桶下的对象到本地文件
     *
     * @param objectKey  对象 Key
     * @param targetPath 本地目标路径
     */
    @Override
    public void downloadToFile(String objectKey, Path targetPath) {
        downloadToFile(getDefaultBucketName(), objectKey, targetPath);
    }

    /**
     * 下载指定存储桶下的对象到本地文件
     *
     * @param bucketName 存储桶名称
     * @param objectKey  对象 Key
     * @param targetPath 本地目标路径
     */
    @Override
    public void downloadToFile(String bucketName, String objectKey, Path targetPath) {
        Assert.notNull(targetPath, "S3 下载目标路径不能为空");
        Assert.isFalse(Files.isDirectory(targetPath), "S3 下载目标路径不能是目录：{}", targetPath);

        String resolvedBucketName = resolveBucketName(bucketName);
        String normalizedObjectKey = normalizeObjectKey(objectKey);

        try {
            Path parentPath = targetPath.toAbsolutePath().getParent();
            if (parentPath != null) {
                Files.createDirectories(parentPath);
            }

            GetObjectRequest request = buildGetObjectRequest(resolvedBucketName, normalizedObjectKey);
            s3Client.getObject(request, ResponseTransformer.toFile(targetPath));

            log.info("下载 S3 对象到本地文件成功，bucketName={}，objectKey={}，targetPath={}",
                    resolvedBucketName, normalizedObjectKey, targetPath);
        } catch (IOException e) {
            log.error("创建 S3 下载目标文件目录失败，bucketName={}，objectKey={}，targetPath={}",
                    resolvedBucketName, normalizedObjectKey, targetPath, e);
            throw new UncheckedIOException("创建 S3 下载目标文件目录失败：" + targetPath, e);
        } catch (NoSuchKeyException e) {
            log.warn("下载 S3 对象到本地文件失败，对象不存在，bucketName={}，objectKey={}，targetPath={}",
                    resolvedBucketName, normalizedObjectKey, targetPath);
            throw e;
        } catch (NoSuchBucketException e) {
            log.warn("下载 S3 对象到本地文件失败，存储桶不存在，bucketName={}，objectKey={}，targetPath={}",
                    resolvedBucketName, normalizedObjectKey, targetPath);
            throw e;
        } catch (S3Exception e) {
            log.error("下载 S3 对象到本地文件失败，bucketName={}，objectKey={}，targetPath={}，statusCode={}，errorCode={}，message={}",
                    resolvedBucketName, normalizedObjectKey, targetPath, e.statusCode(), getErrorCode(e), e.getMessage(), e);
            throw e;
        }
    }

    /**
     * 根据下载请求下载对象
     *
     * @param request 下载请求
     * @return 下载结果，inputStream 需要调用方关闭
     */
    @Override
    public S3DownloadResult download(S3DownloadRequest request) {
        Assert.notNull(request, "S3 下载请求不能为空");

        String resolvedBucketName = resolveBucketName(request.bucketName());
        String normalizedObjectKey = normalizeObjectKey(request.objectKey());

        try {
            GetObjectRequest getObjectRequest = buildGetObjectRequest(request);
            ResponseInputStream<GetObjectResponse> responseInputStream = s3Client.getObject(getObjectRequest);
            GetObjectResponse response = responseInputStream.response();

            S3DownloadResult result = new S3DownloadResult(
                    resolvedBucketName,
                    normalizedObjectKey,
                    getFilename(normalizedObjectKey),
                    response.contentType(),
                    response.contentLength(),
                    normalizeMetadata(response.metadata()),
                    responseInputStream
            );

            log.info("打开 S3 对象下载流成功，bucketName={}，objectKey={}，versionId={}，range={}，contentLength={}",
                    resolvedBucketName,
                    normalizedObjectKey,
                    StrUtil.blankToDefault(request.versionId(), "默认版本"),
                    StrUtil.blankToDefault(getDownloadRange(request.rangeStart(), request.rangeEnd()), "完整对象"),
                    result.contentLength());

            return result;
        } catch (NoSuchKeyException e) {
            log.warn("打开 S3 对象下载流失败，对象不存在，bucketName={}，objectKey={}",
                    resolvedBucketName, normalizedObjectKey);
            throw e;
        } catch (NoSuchBucketException e) {
            log.warn("打开 S3 对象下载流失败，存储桶不存在，bucketName={}，objectKey={}",
                    resolvedBucketName, normalizedObjectKey);
            throw e;
        } catch (S3Exception e) {
            log.error("打开 S3 对象下载流失败，bucketName={}，objectKey={}，statusCode={}，errorCode={}，message={}",
                    resolvedBucketName, normalizedObjectKey, e.statusCode(), getErrorCode(e), e.getMessage(), e);
            throw e;
        }
    }

    // ==================== 对象删除 ====================

    /**
     * 删除默认存储桶下的对象
     *
     * @param objectKey 对象 Key
     */
    @Override
    public void delete(String objectKey) {
        delete(getDefaultBucketName(), objectKey);
    }

    /**
     * 删除指定存储桶下的对象
     *
     * @param bucketName 存储桶名称
     * @param objectKey  对象 Key
     */
    @Override
    public void delete(String bucketName, String objectKey) {
        String resolvedBucketName = resolveBucketName(bucketName);
        String normalizedObjectKey = normalizeObjectKey(objectKey);

        try {
            DeleteObjectRequest request = DeleteObjectRequest.builder()
                    .bucket(resolvedBucketName)
                    .key(normalizedObjectKey)
                    .build();

            s3Client.deleteObject(request);

            log.info("删除 S3 对象成功，bucketName={}，objectKey={}", resolvedBucketName, normalizedObjectKey);
        } catch (NoSuchBucketException e) {
            log.warn("删除 S3 对象失败，存储桶不存在，bucketName={}，objectKey={}",
                    resolvedBucketName, normalizedObjectKey);
            throw e;
        } catch (S3Exception e) {
            log.error("删除 S3 对象失败，bucketName={}，objectKey={}，statusCode={}，errorCode={}，message={}",
                    resolvedBucketName, normalizedObjectKey, e.statusCode(), getErrorCode(e), e.getMessage(), e);
            throw e;
        }
    }

    /**
     * 批量删除默认存储桶下的对象
     *
     * @param objectKeys 对象 Key 列表
     */
    @Override
    public void deleteBatch(List<String> objectKeys) {
        deleteBatch(getDefaultBucketName(), objectKeys);
    }

    /**
     * 批量删除指定存储桶下的对象
     *
     * @param bucketName 存储桶名称
     * @param objectKeys 对象 Key 列表
     */
    @Override
    public void deleteBatch(String bucketName, List<String> objectKeys) {
        S3DeleteResult result = deleteBatchDetailed(bucketName, objectKeys);
        assertNoDeleteErrors(result);
    }

    /**
     * 删除默认存储桶下指定前缀的对象
     *
     * @param prefix 对象 Key 前缀
     * @return 删除成功数量
     */
    @Override
    public Long deleteByPrefix(String prefix) {
        return deleteByPrefix(getDefaultBucketName(), prefix);
    }

    /**
     * 删除指定存储桶下指定前缀的对象
     *
     * @param bucketName 存储桶名称
     * @param prefix     对象 Key 前缀
     * @return 删除成功数量
     */
    @Override
    public Long deleteByPrefix(String bucketName, String prefix) {
        S3DeleteResult result = deleteByPrefixDetailed(bucketName, prefix);
        assertNoDeleteErrors(result);
        return result.deletedCount();
    }

    /**
     * 删除指定版本的对象
     *
     * @param bucketName 存储桶名称
     * @param objectKey  对象 Key
     * @param versionId  版本 ID
     */
    @Override
    public void deleteVersion(String bucketName, String objectKey, String versionId) {
        String resolvedBucketName = resolveBucketName(bucketName);
        String normalizedObjectKey = normalizeObjectKey(objectKey);
        String resolvedVersionId = StrUtil.trim(versionId);

        Assert.notBlank(resolvedVersionId, "S3 对象版本 ID 不能为空");

        try {
            DeleteObjectRequest request = DeleteObjectRequest.builder()
                    .bucket(resolvedBucketName)
                    .key(normalizedObjectKey)
                    .versionId(resolvedVersionId)
                    .build();

            s3Client.deleteObject(request);

            log.info("删除 S3 指定版本对象成功，bucketName={}，objectKey={}，versionId={}",
                    resolvedBucketName, normalizedObjectKey, resolvedVersionId);
        } catch (NoSuchBucketException e) {
            log.warn("删除 S3 指定版本对象失败，存储桶不存在，bucketName={}，objectKey={}，versionId={}",
                    resolvedBucketName, normalizedObjectKey, resolvedVersionId);
            throw e;
        } catch (S3Exception e) {
            log.error("删除 S3 指定版本对象失败，bucketName={}，objectKey={}，versionId={}，statusCode={}，errorCode={}，message={}",
                    resolvedBucketName, normalizedObjectKey, resolvedVersionId,
                    e.statusCode(), getErrorCode(e), e.getMessage(), e);
            throw e;
        }
    }

    /**
     * 批量删除默认存储桶下的对象，并返回详细结果
     *
     * @param objectKeys 对象 Key 列表
     * @return 删除详细结果
     */
    @Override
    public S3DeleteResult deleteBatchDetailed(List<String> objectKeys) {
        return deleteBatchDetailed(getDefaultBucketName(), objectKeys);
    }

    /**
     * 批量删除指定存储桶下的对象，并返回详细结果
     *
     * @param bucketName 存储桶名称
     * @param objectKeys 对象 Key 列表
     * @return 删除详细结果
     */
    @Override
    public S3DeleteResult deleteBatchDetailed(String bucketName, List<String> objectKeys) {
        String resolvedBucketName = resolveBucketName(bucketName);

        if (CollUtil.isEmpty(objectKeys)) {
            return emptyDeleteResult(resolvedBucketName);
        }

        List<ObjectIdentifier> objectIdentifiers = buildObjectIdentifiers(objectKeys);
        if (CollUtil.isEmpty(objectIdentifiers)) {
            return emptyDeleteResult(resolvedBucketName);
        }

        return deleteObjectIdentifiers(resolvedBucketName, objectIdentifiers);
    }

    /**
     * 删除默认存储桶下指定前缀的对象，并返回详细结果
     *
     * @param prefix 对象 Key 前缀
     * @return 删除详细结果
     */
    @Override
    public S3DeleteResult deleteByPrefixDetailed(String prefix) {
        return deleteByPrefixDetailed(getDefaultBucketName(), prefix);
    }

    /**
     * 删除指定存储桶下指定前缀的对象，并返回详细结果
     *
     * @param bucketName 存储桶名称
     * @param prefix     对象 Key 前缀
     * @return 删除详细结果
     */
    @Override
    public S3DeleteResult deleteByPrefixDetailed(String bucketName, String prefix) {
        String resolvedBucketName = resolveBucketName(bucketName);
        String normalizedPrefix = normalizeObjectKey(prefix);

        List<ObjectIdentifier> objectIdentifiers = listObjectIdentifiersByPrefix(resolvedBucketName, normalizedPrefix);
        if (CollUtil.isEmpty(objectIdentifiers)) {
            log.info("按前缀删除 S3 对象完成，未匹配到对象，bucketName={}，prefix={}",
                    resolvedBucketName, normalizedPrefix);
            return emptyDeleteResult(resolvedBucketName);
        }

        S3DeleteResult result = deleteObjectIdentifiers(resolvedBucketName, objectIdentifiers);

        log.info("按前缀删除 S3 对象完成，bucketName={}，prefix={}，deletedCount={}，errorCount={}",
                resolvedBucketName, normalizedPrefix, result.deletedCount(), result.errorCount());

        return result;
    }

    // ==================== 对象复制与移动 ====================

    /**
     * 在默认存储桶内复制对象
     *
     * @param sourceKey 源对象 Key
     * @param targetKey 目标对象 Key
     * @return 复制结果
     */
    @Override
    public S3CopyResult copy(String sourceKey, String targetKey) {
        return copy(getDefaultBucketName(), sourceKey, getDefaultBucketName(), targetKey);
    }

    /**
     * 复制对象
     *
     * @param sourceBucketName 源存储桶名称
     * @param sourceKey        源对象 Key
     * @param targetBucketName 目标存储桶名称
     * @param targetKey        目标对象 Key
     * @return 复制结果
     */
    @Override
    public S3CopyResult copy(String sourceBucketName, String sourceKey, String targetBucketName, String targetKey) {
        S3CopyRequest request = new S3CopyRequest(
                sourceBucketName,
                sourceKey,
                null,
                targetBucketName,
                targetKey,
                Map.of(),
                Map.of(),
                false,
                false,
                null,
                null,
                null,
                null
        );

        return copy(request);
    }

    /**
     * 根据复制请求复制对象
     *
     * @param request 复制请求
     * @return 复制结果
     */
    @Override
    public S3CopyResult copy(S3CopyRequest request) {
        Assert.notNull(request, "S3 复制请求不能为空");

        String sourceBucketName = resolveBucketName(request.sourceBucketName());
        String sourceKey = normalizeObjectKey(request.sourceKey());
        String targetBucketName = resolveBucketName(request.targetBucketName());
        String targetKey = normalizeObjectKey(request.targetKey());

        try {
            CopyObjectRequest copyObjectRequest = buildCopyObjectRequest(request);
            CopyObjectResponse response = s3Client.copyObject(copyObjectRequest);

            S3CopyResult result = new S3CopyResult(
                    sourceBucketName,
                    sourceKey,
                    targetBucketName,
                    targetKey,
                    response.copyObjectResult() == null ? null : response.copyObjectResult().eTag(),
                    response.versionId(),
                    response.copyObjectResult() == null ? Instant.now() : response.copyObjectResult().lastModified()
            );

            log.info("复制 S3 对象成功，sourceBucketName={}，sourceKey={}，targetBucketName={}，targetKey={}",
                    sourceBucketName, sourceKey, targetBucketName, targetKey);

            return result;
        } catch (NoSuchBucketException e) {
            log.warn("复制 S3 对象失败，存储桶不存在，sourceBucketName={}，sourceKey={}，targetBucketName={}，targetKey={}",
                    sourceBucketName, sourceKey, targetBucketName, targetKey);
            throw e;
        } catch (NoSuchKeyException e) {
            log.warn("复制 S3 对象失败，源对象不存在，sourceBucketName={}，sourceKey={}，targetBucketName={}，targetKey={}",
                    sourceBucketName, sourceKey, targetBucketName, targetKey);
            throw e;
        } catch (S3Exception e) {
            log.error("复制 S3 对象失败，sourceBucketName={}，sourceKey={}，targetBucketName={}，targetKey={}，statusCode={}，errorCode={}，message={}",
                    sourceBucketName, sourceKey, targetBucketName, targetKey,
                    e.statusCode(), getErrorCode(e), e.getMessage(), e);
            throw e;
        }
    }

    /**
     * 在默认存储桶内移动对象
     *
     * @param sourceKey 源对象 Key
     * @param targetKey 目标对象 Key
     * @return 移动结果
     */
    @Override
    public S3MoveResult move(String sourceKey, String targetKey) {
        return move(getDefaultBucketName(), sourceKey, getDefaultBucketName(), targetKey);
    }

    /**
     * 移动对象
     *
     * @param sourceBucketName 源存储桶名称
     * @param sourceKey        源对象 Key
     * @param targetBucketName 目标存储桶名称
     * @param targetKey        目标对象 Key
     * @return 移动结果
     */
    @Override
    public S3MoveResult move(String sourceBucketName, String sourceKey, String targetBucketName, String targetKey) {
        S3MoveRequest request = new S3MoveRequest(
                sourceBucketName,
                sourceKey,
                targetBucketName,
                targetKey,
                true
        );

        return move(request);
    }

    /**
     * 根据移动请求移动对象
     *
     * @param request 移动请求
     * @return 移动结果
     */
    @Override
    public S3MoveResult move(S3MoveRequest request) {
        Assert.notNull(request, "S3 移动请求不能为空");

        String sourceBucketName = resolveBucketName(request.sourceBucketName());
        String sourceKey = normalizeObjectKey(request.sourceKey());
        String targetBucketName = resolveBucketName(request.targetBucketName());
        String targetKey = normalizeObjectKey(request.targetKey());
        boolean overwrite = request.overwrite() == null || BooleanUtil.isTrue(request.overwrite());

        if (isSameObject(sourceBucketName, sourceKey, targetBucketName, targetKey)) {
            log.info("S3 源对象与目标对象一致，跳过移动，bucketName={}，objectKey={}", sourceBucketName, sourceKey);

            return new S3MoveResult(
                    sourceBucketName,
                    sourceKey,
                    targetBucketName,
                    targetKey,
                    false,
                    Instant.now()
            );
        }

        if (!overwrite && objectExists(targetBucketName, targetKey)) {
            String message = StrUtil.format(
                    "S3 移动目标对象已存在，targetBucketName={}，targetKey={}",
                    targetBucketName,
                    targetKey
            );
            throw new IllegalStateException(message);
        }

        S3CopyRequest copyRequest = new S3CopyRequest(
                sourceBucketName,
                sourceKey,
                null,
                targetBucketName,
                targetKey,
                Map.of(),
                Map.of(),
                false,
                false,
                null,
                null,
                null,
                null
        );

        copy(copyRequest);
        delete(sourceBucketName, sourceKey);

        S3MoveResult result = new S3MoveResult(
                sourceBucketName,
                sourceKey,
                targetBucketName,
                targetKey,
                true,
                Instant.now()
        );

        log.info("移动 S3 对象成功，sourceBucketName={}，sourceKey={}，targetBucketName={}，targetKey={}，overwrite={}",
                sourceBucketName, sourceKey, targetBucketName, targetKey, overwrite);

        return result;
    }

    /**
     * 在默认存储桶内重命名对象
     *
     * @param sourceKey 源对象 Key
     * @param targetKey 目标对象 Key
     */
    @Override
    public void rename(String sourceKey, String targetKey) {
        rename(getDefaultBucketName(), sourceKey, targetKey);
    }

    /**
     * 在指定存储桶内重命名对象
     *
     * @param bucketName 存储桶名称
     * @param sourceKey  源对象 Key
     * @param targetKey  目标对象 Key
     */
    @Override
    public void rename(String bucketName, String sourceKey, String targetKey) {
        move(bucketName, sourceKey, bucketName, targetKey);
    }

    // ==================== 对象列表与分页查询 ====================

    /**
     * 查询默认存储桶当前层级对象列表
     *
     * @return 对象信息列表
     */
    @Override
    public List<S3ObjectInfo> listObjects() {
        return listObjects(getDefaultBucketName(), null);
    }

    /**
     * 查询默认存储桶指定前缀当前层级对象列表
     *
     * @param prefix 对象 Key 前缀
     * @return 对象信息列表
     */
    @Override
    public List<S3ObjectInfo> listObjects(String prefix) {
        return listObjects(getDefaultBucketName(), prefix);
    }

    /**
     * 查询指定存储桶指定前缀当前层级对象列表
     *
     * @param bucketName 存储桶名称
     * @param prefix     对象 Key 前缀
     * @return 对象信息列表
     */
    @Override
    public List<S3ObjectInfo> listObjects(String bucketName, String prefix) {
        String resolvedBucketName = resolveBucketName(bucketName);
        String normalizedPrefix = normalizeObjectPrefix(prefix);

        List<S3ObjectInfo> objects = new ArrayList<>();
        String continuationToken = null;

        do {
            S3ListRequest request = new S3ListRequest(
                    resolvedBucketName,
                    normalizedPrefix,
                    "/",
                    1000,
                    continuationToken,
                    false
            );

            S3ObjectPage page = listObjectsPage(request);
            objects.addAll(page.objects());
            continuationToken = page.nextContinuationToken();
        } while (StrUtil.isNotBlank(continuationToken));

        return objects;
    }

    /**
     * 分页查询对象列表
     *
     * @param request 列表查询请求
     * @return 对象分页结果
     */
    @Override
    public S3ObjectPage listObjectsPage(S3ListRequest request) {
        Assert.notNull(request, "S3 对象列表查询请求不能为空");

        String resolvedBucketName = resolveBucketName(request.bucketName());
        String normalizedPrefix = normalizeObjectPrefix(request.prefix());
        Integer maxKeys = normalizeMaxKeys(request.maxKeys());

        try {
            ListObjectsV2Request listObjectsRequest = buildListObjectsV2Request(
                    resolvedBucketName,
                    normalizedPrefix,
                    request.delimiter(),
                    maxKeys,
                    request.continuationToken(),
                    request.recursive()
            );

            ListObjectsV2Response response = s3Client.listObjectsV2(listObjectsRequest);

            List<S3ObjectInfo> objects = response.contents()
                    .stream()
                    .map(object -> buildS3ObjectInfo(resolvedBucketName, object))
                    .toList();

            List<String> commonPrefixes = response.commonPrefixes()
                    .stream()
                    .map(CommonPrefix::prefix)
                    .filter(StrUtil::isNotBlank)
                    .toList();

            return new S3ObjectPage(
                    resolvedBucketName,
                    normalizedPrefix,
                    objects,
                    commonPrefixes,
                    response.isTruncated(),
                    response.nextContinuationToken(),
                    maxKeys
            );
        } catch (NoSuchBucketException e) {
            log.warn("分页查询 S3 对象列表失败，存储桶不存在，bucketName={}，prefix={}",
                    resolvedBucketName, normalizedPrefix);
            throw e;
        } catch (S3Exception e) {
            log.error("分页查询 S3 对象列表失败，bucketName={}，prefix={}，statusCode={}，errorCode={}，message={}",
                    resolvedBucketName, normalizedPrefix, e.statusCode(), getErrorCode(e), e.getMessage(), e);
            throw e;
        }
    }

    /**
     * 递归查询默认存储桶指定前缀下的全部对象
     *
     * @param prefix 对象 Key 前缀
     * @return 对象信息列表
     */
    @Override
    public List<S3ObjectInfo> listObjectsRecursive(String prefix) {
        return listObjectsRecursive(getDefaultBucketName(), prefix);
    }

    /**
     * 递归查询指定存储桶指定前缀下的全部对象
     *
     * @param bucketName 存储桶名称
     * @param prefix     对象 Key 前缀
     * @return 对象信息列表
     */
    @Override
    public List<S3ObjectInfo> listObjectsRecursive(String bucketName, String prefix) {
        String resolvedBucketName = resolveBucketName(bucketName);
        String normalizedPrefix = normalizeObjectPrefix(prefix);

        List<S3ObjectInfo> objects = new ArrayList<>();
        String continuationToken = null;

        do {
            S3ListRequest request = new S3ListRequest(
                    resolvedBucketName,
                    normalizedPrefix,
                    null,
                    1000,
                    continuationToken,
                    true
            );

            S3ObjectPage page = listObjectsPage(request);
            objects.addAll(page.objects());
            continuationToken = page.nextContinuationToken();
        } while (StrUtil.isNotBlank(continuationToken));

        return objects;
    }

    /**
     * 递归查询默认存储桶指定前缀下的对象 Key 列表
     *
     * @param prefix 对象 Key 前缀
     * @return 对象 Key 列表
     */
    @Override
    public List<String> listObjectKeys(String prefix) {
        return listObjectKeys(getDefaultBucketName(), prefix);
    }

    /**
     * 递归查询指定存储桶指定前缀下的对象 Key 列表
     *
     * @param bucketName 存储桶名称
     * @param prefix     对象 Key 前缀
     * @return 对象 Key 列表
     */
    @Override
    public List<String> listObjectKeys(String bucketName, String prefix) {
        return listObjectsRecursive(bucketName, prefix)
                .stream()
                .map(S3ObjectInfo::objectKey)
                .filter(StrUtil::isNotBlank)
                .toList();
    }

    /**
     * 查询对象版本列表
     *
     * @param bucketName 存储桶名称
     * @param prefix     对象 Key 前缀
     * @return 对象版本信息列表
     */
    @Override
    public List<S3ObjectVersionInfo> listObjectVersions(String bucketName, String prefix) {
        String resolvedBucketName = resolveBucketName(bucketName);
        String normalizedPrefix = normalizeObjectPrefix(prefix);

        List<S3ObjectVersionInfo> versions = new ArrayList<>();
        String keyMarker = null;
        String versionIdMarker = null;

        try {
            do {
                ListObjectVersionsRequest.Builder builder = ListObjectVersionsRequest.builder()
                        .bucket(resolvedBucketName)
                        .maxKeys(1000);

                if (StrUtil.isNotBlank(normalizedPrefix)) {
                    builder.prefix(normalizedPrefix);
                }

                if (StrUtil.isNotBlank(keyMarker)) {
                    builder.keyMarker(keyMarker);
                }

                if (StrUtil.isNotBlank(versionIdMarker)) {
                    builder.versionIdMarker(versionIdMarker);
                }

                ListObjectVersionsResponse response = s3Client.listObjectVersions(builder.build());

                versions.addAll(response.versions()
                        .stream()
                        .map(objectVersion -> buildS3ObjectVersionInfo(resolvedBucketName, objectVersion))
                        .toList());

                versions.addAll(response.deleteMarkers()
                        .stream()
                        .map(deleteMarker -> buildS3ObjectVersionInfo(resolvedBucketName, deleteMarker))
                        .toList());

                keyMarker = response.nextKeyMarker();
                versionIdMarker = response.nextVersionIdMarker();
            } while (StrUtil.isNotBlank(keyMarker));

            return versions;
        } catch (NoSuchBucketException e) {
            log.warn("查询 S3 对象版本列表失败，存储桶不存在，bucketName={}，prefix={}",
                    resolvedBucketName, normalizedPrefix);
            throw e;
        } catch (S3Exception e) {
            log.error("查询 S3 对象版本列表失败，bucketName={}，prefix={}，statusCode={}，errorCode={}，message={}",
                    resolvedBucketName, normalizedPrefix, e.statusCode(), getErrorCode(e), e.getMessage(), e);
            throw e;
        }
    }

    // ==================== 目录语义操作 ====================

    /**
     * 在默认存储桶下创建目录占位对象
     *
     * @param directoryKey 目录 Key
     */
    @Override
    public void createDirectory(String directoryKey) {
        createDirectory(getDefaultBucketName(), directoryKey);
    }

    /**
     * 在指定存储桶下创建目录占位对象
     *
     * @param bucketName   存储桶名称
     * @param directoryKey 目录 Key
     */
    @Override
    public void createDirectory(String bucketName, String directoryKey) {
        String resolvedBucketName = resolveBucketName(bucketName);
        String normalizedDirectoryKey = normalizeDirectoryKey(directoryKey);

        try {
            PutObjectRequest request = PutObjectRequest.builder()
                    .bucket(resolvedBucketName)
                    .key(normalizedDirectoryKey)
                    .contentLength(0L)
                    .contentType("application/x-directory")
                    .build();

            PutObjectResponse response = s3Client.putObject(request, RequestBody.empty());

            log.info("创建 S3 目录占位对象成功，bucketName={}，directoryKey={}，eTag={}，versionId={}",
                    resolvedBucketName, normalizedDirectoryKey, response.eTag(), response.versionId());
        } catch (NoSuchBucketException e) {
            log.warn("创建 S3 目录失败，存储桶不存在，bucketName={}，directoryKey={}",
                    resolvedBucketName, normalizedDirectoryKey);
            throw e;
        } catch (S3Exception e) {
            log.error("创建 S3 目录失败，bucketName={}，directoryKey={}，statusCode={}，errorCode={}，message={}",
                    resolvedBucketName, normalizedDirectoryKey, e.statusCode(), getErrorCode(e), e.getMessage(), e);
            throw e;
        }
    }

    /**
     * 判断默认存储桶下的目录是否存在
     *
     * @param directoryKey 目录 Key
     * @return true 存在，false 不存在
     */
    @Override
    public boolean directoryExists(String directoryKey) {
        return directoryExists(getDefaultBucketName(), directoryKey);
    }

    /**
     * 判断指定存储桶下的目录是否存在
     *
     * @param bucketName   存储桶名称
     * @param directoryKey 目录 Key
     * @return true 存在，false 不存在
     */
    @Override
    public boolean directoryExists(String bucketName, String directoryKey) {
        String resolvedBucketName = resolveBucketName(bucketName);
        String normalizedDirectoryKey = normalizeDirectoryKey(directoryKey);

        if (objectExists(resolvedBucketName, normalizedDirectoryKey)) {
            return true;
        }

        try {
            ListObjectsV2Request request = ListObjectsV2Request.builder()
                    .bucket(resolvedBucketName)
                    .prefix(normalizedDirectoryKey)
                    .maxKeys(1)
                    .build();

            ListObjectsV2Response response = s3Client.listObjectsV2(request);
            boolean exists = CollUtil.isNotEmpty(response.contents());

            if (!exists) {
                log.info("S3 目录不存在，bucketName={}，directoryKey={}",
                        resolvedBucketName, normalizedDirectoryKey);
            }

            return exists;
        } catch (NoSuchBucketException e) {
            log.info("判断 S3 目录是否存在失败，存储桶不存在，bucketName={}，directoryKey={}",
                    resolvedBucketName, normalizedDirectoryKey);
            return false;
        } catch (S3Exception e) {
            if (isNotFound(e)) {
                log.info("S3 目录不存在，bucketName={}，directoryKey={}，errorCode={}",
                        resolvedBucketName, normalizedDirectoryKey, getErrorCode(e));
                return false;
            }

            log.error("判断 S3 目录是否存在失败，bucketName={}，directoryKey={}，statusCode={}，errorCode={}，message={}",
                    resolvedBucketName, normalizedDirectoryKey, e.statusCode(), getErrorCode(e), e.getMessage(), e);
            throw e;
        }
    }

    /**
     * 删除默认存储桶下的目录及其所有对象
     *
     * @param directoryKey 目录 Key
     * @return 删除成功数量
     */
    @Override
    public Long deleteDirectory(String directoryKey) {
        return deleteDirectory(getDefaultBucketName(), directoryKey);
    }

    /**
     * 删除指定存储桶下的目录及其所有对象
     *
     * @param bucketName   存储桶名称
     * @param directoryKey 目录 Key
     * @return 删除成功数量
     */
    @Override
    public Long deleteDirectory(String bucketName, String directoryKey) {
        String resolvedBucketName = resolveBucketName(bucketName);
        String normalizedDirectoryKey = normalizeDirectoryKey(directoryKey);

        S3DeleteResult result = deleteByPrefixDetailed(resolvedBucketName, normalizedDirectoryKey);
        assertNoDeleteErrors(result);

        log.info("删除 S3 目录完成，bucketName={}，directoryKey={}，deletedCount={}",
                resolvedBucketName, normalizedDirectoryKey, result.deletedCount());

        return result.deletedCount();
    }

    /**
     * 查询默认存储桶下目录当前层级对象列表
     *
     * @param directoryKey 目录 Key
     * @return 对象信息列表
     */
    @Override
    public List<S3ObjectInfo> listDirectory(String directoryKey) {
        return listDirectory(getDefaultBucketName(), directoryKey);
    }

    /**
     * 查询指定存储桶下目录当前层级对象列表
     *
     * @param bucketName   存储桶名称
     * @param directoryKey 目录 Key
     * @return 对象信息列表
     */
    @Override
    public List<S3ObjectInfo> listDirectory(String bucketName, String directoryKey) {
        String resolvedBucketName = resolveBucketName(bucketName);
        String normalizedDirectoryKey = normalizeDirectoryKey(directoryKey);

        S3ListRequest request = new S3ListRequest(
                resolvedBucketName,
                normalizedDirectoryKey,
                "/",
                1000,
                null,
                false
        );

        List<S3ObjectInfo> objects = new ArrayList<>();
        String continuationToken = null;

        do {
            S3ObjectPage page = listObjectsPage(new S3ListRequest(
                    request.bucketName(),
                    request.prefix(),
                    request.delimiter(),
                    request.maxKeys(),
                    continuationToken,
                    request.recursive()
            ));

            objects.addAll(page.objects()
                    .stream()
                    .filter(object -> !StrUtil.equals(object.objectKey(), normalizedDirectoryKey))
                    .toList());

            continuationToken = page.nextContinuationToken();
        } while (StrUtil.isNotBlank(continuationToken));

        return objects;
    }

    // ==================== 预签名 URL ====================

    /**
     * 生成默认存储桶下对象的下载预签名 URL
     *
     * @param objectKey 对象 Key
     * @return 下载预签名 URL
     */
    @Override
    public URI generateDownloadUrl(String objectKey) {
        return generateDownloadUrl(getDefaultBucketName(), objectKey);
    }

    /**
     * 生成指定存储桶下对象的下载预签名 URL
     *
     * @param bucketName 存储桶名称
     * @param objectKey  对象 Key
     * @return 下载预签名 URL
     */
    @Override
    public URI generateDownloadUrl(String bucketName, String objectKey) {
        return generateDownloadUrl(bucketName, objectKey, null);
    }

    /**
     * 生成默认存储桶下对象的下载预签名 URL
     *
     * @param objectKey 对象 Key
     * @param expire    有效期
     * @return 下载预签名 URL
     */
    @Override
    public URI generateDownloadUrl(String objectKey, Duration expire) {
        return generateDownloadUrl(getDefaultBucketName(), objectKey, expire);
    }

    /**
     * 生成指定存储桶下对象的下载预签名 URL
     *
     * @param bucketName 存储桶名称
     * @param objectKey  对象 Key
     * @param expire     有效期
     * @return 下载预签名 URL
     */
    @Override
    public URI generateDownloadUrl(String bucketName, String objectKey, Duration expire) {
        S3PresignedUrlRequest request = new S3PresignedUrlRequest(
                bucketName,
                objectKey,
                S3PresignedUrlMethod.GET,
                expire,
                null,
                null,
                Map.of(),
                Map.of()
        );

        return generatePresignedUrl(request).url();
    }

    /**
     * 生成默认存储桶下对象的上传预签名 URL
     *
     * @param objectKey 对象 Key
     * @param expire    有效期
     * @return 上传预签名 URL
     */
    @Override
    public URI generateUploadUrl(String objectKey, Duration expire) {
        return generateUploadUrl(getDefaultBucketName(), objectKey, expire);
    }

    /**
     * 生成指定存储桶下对象的上传预签名 URL
     *
     * @param bucketName 存储桶名称
     * @param objectKey  对象 Key
     * @param expire     有效期
     * @return 上传预签名 URL
     */
    @Override
    public URI generateUploadUrl(String bucketName, String objectKey, Duration expire) {
        S3PresignedUrlRequest request = new S3PresignedUrlRequest(
                bucketName,
                objectKey,
                S3PresignedUrlMethod.PUT,
                expire,
                null,
                null,
                Map.of(),
                Map.of()
        );

        return generatePresignedUrl(request).url();
    }

    /**
     * 生成默认存储桶下对象的 HEAD 预签名 URL
     *
     * @param objectKey 对象 Key
     * @param expire    有效期
     * @return HEAD 预签名 URL
     */
    @Override
    public URI generateHeadUrl(String objectKey, Duration expire) {
        return generateHeadUrl(getDefaultBucketName(), objectKey, expire);
    }

    /**
     * 生成指定存储桶下对象的 HEAD 预签名 URL
     *
     * @param bucketName 存储桶名称
     * @param objectKey  对象 Key
     * @param expire     有效期
     * @return HEAD 预签名 URL
     */
    @Override
    public URI generateHeadUrl(String bucketName, String objectKey, Duration expire) {
        S3PresignedUrlRequest request = new S3PresignedUrlRequest(
                bucketName,
                objectKey,
                S3PresignedUrlMethod.HEAD,
                expire,
                null,
                null,
                Map.of(),
                Map.of()
        );

        return generatePresignedUrl(request).url();
    }

    /**
     * 生成默认存储桶下对象的删除预签名 URL
     *
     * @param objectKey 对象 Key
     * @param expire    有效期
     * @return 删除预签名 URL
     */
    @Override
    public URI generateDeleteUrl(String objectKey, Duration expire) {
        return generateDeleteUrl(getDefaultBucketName(), objectKey, expire);
    }

    /**
     * 生成指定存储桶下对象的删除预签名 URL
     *
     * @param bucketName 存储桶名称
     * @param objectKey  对象 Key
     * @param expire     有效期
     * @return 删除预签名 URL
     */
    @Override
    public URI generateDeleteUrl(String bucketName, String objectKey, Duration expire) {
        S3PresignedUrlRequest request = new S3PresignedUrlRequest(
                bucketName,
                objectKey,
                S3PresignedUrlMethod.DELETE,
                expire,
                null,
                null,
                Map.of(),
                Map.of()
        );

        return generatePresignedUrl(request).url();
    }

    /**
     * 生成预签名 URL
     *
     * @param request 预签名 URL 请求
     * @return 预签名 URL 结果
     */
    @Override
    public S3PresignedUrlResult generatePresignedUrl(S3PresignedUrlRequest request) {
        Assert.notNull(request, "S3 预签名 URL 请求不能为空");

        String resolvedBucketName = resolveBucketName(request.bucketName());
        String normalizedObjectKey = normalizeObjectKey(request.objectKey());
        S3PresignedUrlMethod method = request.method() == null ? S3PresignedUrlMethod.GET : request.method();
        Duration signatureDuration = normalizePresignExpire(request.expire());

        URI url = switch (method) {
            case GET -> presignGetObject(request, resolvedBucketName, normalizedObjectKey, signatureDuration);
            case PUT -> presignPutObject(request, resolvedBucketName, normalizedObjectKey, signatureDuration);
            case HEAD -> presignHeadObject(request, resolvedBucketName, normalizedObjectKey, signatureDuration);
            case DELETE -> presignDeleteObject(request, resolvedBucketName, normalizedObjectKey, signatureDuration);
        };

        Instant expireTime = Instant.now().plus(signatureDuration);

        log.info("生成 S3 预签名 URL 成功，bucketName={}，objectKey={}，method={}，expireTime={}",
                resolvedBucketName, normalizedObjectKey, method, DateUtil.date(expireTime.toEpochMilli()));

        return new S3PresignedUrlResult(
                resolvedBucketName,
                normalizedObjectKey,
                method,
                url,
                expireTime
        );
    }

    // ==================== 元数据管理 ====================

    /**
     * 获取默认存储桶下对象的自定义元数据
     *
     * @param objectKey 对象 Key
     * @return 自定义元数据
     */
    @Override
    public Map<String, String> getMetadata(String objectKey) {
        return getMetadata(getDefaultBucketName(), objectKey);
    }

    /**
     * 获取指定存储桶下对象的自定义元数据
     *
     * @param bucketName 存储桶名称
     * @param objectKey  对象 Key
     * @return 自定义元数据
     */
    @Override
    public Map<String, String> getMetadata(String bucketName, String objectKey) {
        String resolvedBucketName = resolveBucketName(bucketName);
        String normalizedObjectKey = normalizeObjectKey(objectKey);

        try {
            HeadObjectRequest request = buildHeadObjectRequest(resolvedBucketName, normalizedObjectKey);
            HeadObjectResponse response = s3Client.headObject(request);

            return response.metadata() == null ? Map.of() : Map.copyOf(response.metadata());
        } catch (NoSuchKeyException e) {
            log.warn("获取 S3 对象元数据失败，对象不存在，bucketName={}，objectKey={}",
                    resolvedBucketName, normalizedObjectKey);
            throw e;
        } catch (NoSuchBucketException e) {
            log.warn("获取 S3 对象元数据失败，存储桶不存在，bucketName={}，objectKey={}",
                    resolvedBucketName, normalizedObjectKey);
            throw e;
        } catch (S3Exception e) {
            log.error("获取 S3 对象元数据失败，bucketName={}，objectKey={}，statusCode={}，errorCode={}，message={}",
                    resolvedBucketName, normalizedObjectKey, e.statusCode(), getErrorCode(e), e.getMessage(), e);
            throw e;
        }
    }

    /**
     * 替换默认存储桶下对象的自定义元数据
     *
     * @param objectKey 对象 Key
     * @param metadata  新元数据
     */
    @Override
    public void replaceMetadata(String objectKey, Map<String, String> metadata) {
        replaceMetadata(getDefaultBucketName(), objectKey, metadata);
    }

    /**
     * 替换指定存储桶下对象的自定义元数据
     *
     * @param bucketName 存储桶名称
     * @param objectKey  对象 Key
     * @param metadata   新元数据
     */
    @Override
    public void replaceMetadata(String bucketName, String objectKey, Map<String, String> metadata) {
        S3MetadataRequest request = new S3MetadataRequest(
                bucketName,
                objectKey,
                metadata,
                true,
                false,
                null
        );

        replaceMetadata(request);
    }

    /**
     * 根据请求替换对象自定义元数据
     *
     * @param request 元数据替换请求
     */
    @Override
    public void replaceMetadata(S3MetadataRequest request) {
        Assert.notNull(request, "S3 元数据替换请求不能为空");

        String resolvedBucketName = resolveBucketName(request.bucketName());
        String normalizedObjectKey = normalizeObjectKey(request.objectKey());
        Map<String, String> metadata = normalizeMetadata(request.metadata());
        boolean preserveAcl = BooleanUtil.isTrue(request.preserveAcl());

        GetObjectAclResponse aclResponse = null;
        if (preserveAcl) {
            aclResponse = getObjectAcl(resolvedBucketName, normalizedObjectKey);
        }

        try {
            CopyObjectRequest copyObjectRequest = buildReplaceMetadataCopyRequest(
                    resolvedBucketName,
                    normalizedObjectKey,
                    metadata,
                    request
            );

            CopyObjectResponse response = s3Client.copyObject(copyObjectRequest);

            if (preserveAcl && aclResponse != null) {
                putObjectAcl(resolvedBucketName, normalizedObjectKey, aclResponse);
            }

            log.info("替换 S3 对象元数据成功，bucketName={}，objectKey={}，metadataSize={}，versionId={}，eTag={}",
                    resolvedBucketName,
                    normalizedObjectKey,
                    metadata.size(),
                    response.versionId(),
                    response.copyObjectResult() == null ? null : response.copyObjectResult().eTag());
        } catch (NoSuchBucketException e) {
            log.warn("替换 S3 对象元数据失败，存储桶不存在，bucketName={}，objectKey={}",
                    resolvedBucketName, normalizedObjectKey);
            throw e;
        } catch (NoSuchKeyException e) {
            log.warn("替换 S3 对象元数据失败，对象不存在，bucketName={}，objectKey={}",
                    resolvedBucketName, normalizedObjectKey);
            throw e;
        } catch (S3Exception e) {
            log.error("替换 S3 对象元数据失败，bucketName={}，objectKey={}，statusCode={}，errorCode={}，message={}",
                    resolvedBucketName, normalizedObjectKey, e.statusCode(), getErrorCode(e), e.getMessage(), e);
            throw e;
        }
    }

    // ==================== 标签管理 ====================

    /**
     * 获取默认存储桶下对象标签
     *
     * @param objectKey 对象 Key
     * @return 标签 Map
     */
    @Override
    public Map<String, String> getTags(String objectKey) {
        return getTags(getDefaultBucketName(), objectKey);
    }

    /**
     * 获取指定存储桶下对象标签
     *
     * @param bucketName 存储桶名称
     * @param objectKey  对象 Key
     * @return 标签 Map
     */
    @Override
    public Map<String, String> getTags(String bucketName, String objectKey) {
        return getTags(bucketName, objectKey, null);
    }

    /**
     * 写入默认存储桶下对象标签
     *
     * @param objectKey 对象 Key
     * @param tags      标签 Map
     */
    @Override
    public void putTags(String objectKey, Map<String, String> tags) {
        putTags(getDefaultBucketName(), objectKey, tags);
    }

    /**
     * 写入指定存储桶下对象标签
     *
     * @param bucketName 存储桶名称
     * @param objectKey  对象 Key
     * @param tags       标签 Map
     */
    @Override
    public void putTags(String bucketName, String objectKey, Map<String, String> tags) {
        putTags(bucketName, objectKey, null, tags);
    }

    /**
     * 删除默认存储桶下对象标签
     *
     * @param objectKey 对象 Key
     */
    @Override
    public void deleteTags(String objectKey) {
        deleteTags(getDefaultBucketName(), objectKey);
    }

    /**
     * 删除指定存储桶下对象标签
     *
     * @param bucketName 存储桶名称
     * @param objectKey  对象 Key
     */
    @Override
    public void deleteTags(String bucketName, String objectKey) {
        deleteTags(bucketName, objectKey, null);
    }

    /**
     * 获取指定对象版本的标签
     *
     * @param bucketName 存储桶名称
     * @param objectKey  对象 Key
     * @param versionId  版本 ID
     * @return 标签 Map
     */
    @Override
    public Map<String, String> getTags(String bucketName, String objectKey, String versionId) {
        String resolvedBucketName = resolveBucketName(bucketName);
        String normalizedObjectKey = normalizeObjectKey(objectKey);
        String resolvedVersionId = StrUtil.trim(versionId);

        try {
            GetObjectTaggingRequest.Builder builder = GetObjectTaggingRequest.builder()
                    .bucket(resolvedBucketName)
                    .key(normalizedObjectKey);

            if (StrUtil.isNotBlank(resolvedVersionId)) {
                builder.versionId(resolvedVersionId);
            }

            GetObjectTaggingResponse response = s3Client.getObjectTagging(builder.build());
            return toTagMap(response.tagSet());
        } catch (NoSuchKeyException e) {
            log.warn("获取 S3 对象标签失败，对象不存在，bucketName={}，objectKey={}，versionId={}",
                    resolvedBucketName, normalizedObjectKey, StrUtil.blankToDefault(resolvedVersionId, "默认版本"));
            throw e;
        } catch (NoSuchBucketException e) {
            log.warn("获取 S3 对象标签失败，存储桶不存在，bucketName={}，objectKey={}，versionId={}",
                    resolvedBucketName, normalizedObjectKey, StrUtil.blankToDefault(resolvedVersionId, "默认版本"));
            throw e;
        } catch (S3Exception e) {
            log.error("获取 S3 对象标签失败，bucketName={}，objectKey={}，versionId={}，statusCode={}，errorCode={}，message={}",
                    resolvedBucketName,
                    normalizedObjectKey,
                    StrUtil.blankToDefault(resolvedVersionId, "默认版本"),
                    e.statusCode(),
                    getErrorCode(e),
                    e.getMessage(),
                    e);
            throw e;
        }
    }

    /**
     * 写入指定对象版本的标签
     *
     * @param bucketName 存储桶名称
     * @param objectKey  对象 Key
     * @param versionId  版本 ID
     * @param tags       标签 Map
     */
    @Override
    public void putTags(String bucketName, String objectKey, String versionId, Map<String, String> tags) {
        String resolvedBucketName = resolveBucketName(bucketName);
        String normalizedObjectKey = normalizeObjectKey(objectKey);
        String resolvedVersionId = StrUtil.trim(versionId);
        Map<String, String> normalizedTags = normalizeTags(tags);

        if (MapUtil.isEmpty(normalizedTags)) {
            deleteTags(resolvedBucketName, normalizedObjectKey, resolvedVersionId);
            return;
        }

        try {
            PutObjectTaggingRequest.Builder builder = PutObjectTaggingRequest.builder()
                    .bucket(resolvedBucketName)
                    .key(normalizedObjectKey)
                    .tagging(Tagging.builder()
                            .tagSet(toTagList(normalizedTags))
                            .build());

            if (StrUtil.isNotBlank(resolvedVersionId)) {
                builder.versionId(resolvedVersionId);
            }

            s3Client.putObjectTagging(builder.build());

            log.info("写入 S3 对象标签成功，bucketName={}，objectKey={}，versionId={}，tagSize={}",
                    resolvedBucketName,
                    normalizedObjectKey,
                    StrUtil.blankToDefault(resolvedVersionId, "默认版本"),
                    normalizedTags.size());
        } catch (NoSuchKeyException e) {
            log.warn("写入 S3 对象标签失败，对象不存在，bucketName={}，objectKey={}，versionId={}",
                    resolvedBucketName, normalizedObjectKey, StrUtil.blankToDefault(resolvedVersionId, "默认版本"));
            throw e;
        } catch (NoSuchBucketException e) {
            log.warn("写入 S3 对象标签失败，存储桶不存在，bucketName={}，objectKey={}，versionId={}",
                    resolvedBucketName, normalizedObjectKey, StrUtil.blankToDefault(resolvedVersionId, "默认版本"));
            throw e;
        } catch (S3Exception e) {
            log.error("写入 S3 对象标签失败，bucketName={}，objectKey={}，versionId={}，statusCode={}，errorCode={}，message={}",
                    resolvedBucketName,
                    normalizedObjectKey,
                    StrUtil.blankToDefault(resolvedVersionId, "默认版本"),
                    e.statusCode(),
                    getErrorCode(e),
                    e.getMessage(),
                    e);
            throw e;
        }
    }

    /**
     * 删除指定对象版本的标签
     *
     * @param bucketName 存储桶名称
     * @param objectKey  对象 Key
     * @param versionId  版本 ID
     */
    @Override
    public void deleteTags(String bucketName, String objectKey, String versionId) {
        String resolvedBucketName = resolveBucketName(bucketName);
        String normalizedObjectKey = normalizeObjectKey(objectKey);
        String resolvedVersionId = StrUtil.trim(versionId);

        try {
            DeleteObjectTaggingRequest.Builder builder = DeleteObjectTaggingRequest.builder()
                    .bucket(resolvedBucketName)
                    .key(normalizedObjectKey);

            if (StrUtil.isNotBlank(resolvedVersionId)) {
                builder.versionId(resolvedVersionId);
            }

            s3Client.deleteObjectTagging(builder.build());

            log.info("删除 S3 对象标签成功，bucketName={}，objectKey={}，versionId={}",
                    resolvedBucketName,
                    normalizedObjectKey,
                    StrUtil.blankToDefault(resolvedVersionId, "默认版本"));
        } catch (NoSuchKeyException e) {
            log.warn("删除 S3 对象标签失败，对象不存在，bucketName={}，objectKey={}，versionId={}",
                    resolvedBucketName, normalizedObjectKey, StrUtil.blankToDefault(resolvedVersionId, "默认版本"));
            throw e;
        } catch (NoSuchBucketException e) {
            log.warn("删除 S3 对象标签失败，存储桶不存在，bucketName={}，objectKey={}，versionId={}",
                    resolvedBucketName, normalizedObjectKey, StrUtil.blankToDefault(resolvedVersionId, "默认版本"));
            throw e;
        } catch (S3Exception e) {
            log.error("删除 S3 对象标签失败，bucketName={}，objectKey={}，versionId={}，statusCode={}，errorCode={}，message={}",
                    resolvedBucketName,
                    normalizedObjectKey,
                    StrUtil.blankToDefault(resolvedVersionId, "默认版本"),
                    e.statusCode(),
                    getErrorCode(e),
                    e.getMessage(),
                    e);
            throw e;
        }
    }

    // ==================== 访问控制与存储属性 ====================

    /**
     * 设置默认存储桶下对象 ACL
     *
     * @param objectKey 对象 Key
     * @param acl       对象 ACL
     */
    @Override
    public void setObjectAcl(String objectKey, S3ObjectAcl acl) {
        setObjectAcl(getDefaultBucketName(), objectKey, acl);
    }

    /**
     * 设置指定存储桶下对象 ACL
     *
     * @param bucketName 存储桶名称
     * @param objectKey  对象 Key
     * @param acl        对象 ACL
     */
    @Override
    public void setObjectAcl(String bucketName, String objectKey, S3ObjectAcl acl) {
        String resolvedBucketName = resolveBucketName(bucketName);
        String normalizedObjectKey = normalizeObjectKey(objectKey);

        Assert.notNull(acl, "S3 对象 ACL 不能为空");

        try {
            PutObjectAclRequest request = PutObjectAclRequest.builder()
                    .bucket(resolvedBucketName)
                    .key(normalizedObjectKey)
                    .acl(toAwsObjectCannedAcl(acl))
                    .build();

            s3Client.putObjectAcl(request);

            log.info("设置 S3 对象 ACL 成功，bucketName={}，objectKey={}，acl={}",
                    resolvedBucketName, normalizedObjectKey, acl);
        } catch (NoSuchKeyException e) {
            log.warn("设置 S3 对象 ACL 失败，对象不存在，bucketName={}，objectKey={}，acl={}",
                    resolvedBucketName, normalizedObjectKey, acl);
            throw e;
        } catch (NoSuchBucketException e) {
            log.warn("设置 S3 对象 ACL 失败，存储桶不存在，bucketName={}，objectKey={}，acl={}",
                    resolvedBucketName, normalizedObjectKey, acl);
            throw e;
        } catch (S3Exception e) {
            log.error("设置 S3 对象 ACL 失败，bucketName={}，objectKey={}，acl={}，statusCode={}，errorCode={}，message={}",
                    resolvedBucketName,
                    normalizedObjectKey,
                    acl,
                    e.statusCode(),
                    getErrorCode(e),
                    e.getMessage(),
                    e);
            throw e;
        }
    }

    /**
     * 修改默认存储桶下对象存储类型
     *
     * @param objectKey    对象 Key
     * @param storageClass 存储类型
     */
    @Override
    public void changeStorageClass(String objectKey, S3StorageClass storageClass) {
        changeStorageClass(getDefaultBucketName(), objectKey, storageClass);
    }

    /**
     * 修改指定存储桶下对象存储类型
     *
     * @param bucketName   存储桶名称
     * @param objectKey    对象 Key
     * @param storageClass 存储类型
     */
    @Override
    public void changeStorageClass(String bucketName, String objectKey, S3StorageClass storageClass) {
        String resolvedBucketName = resolveBucketName(bucketName);
        String normalizedObjectKey = normalizeObjectKey(objectKey);

        Assert.notNull(storageClass, "S3 存储类型不能为空");

        try {
            CopyObjectRequest request = buildChangeStorageClassRequest(
                    resolvedBucketName,
                    normalizedObjectKey,
                    storageClass
            );

            CopyObjectResponse response = s3Client.copyObject(request);

            log.info("修改 S3 对象存储类型成功，bucketName={}，objectKey={}，storageClass={}，versionId={}，eTag={}",
                    resolvedBucketName,
                    normalizedObjectKey,
                    storageClass,
                    response.versionId(),
                    response.copyObjectResult() == null ? null : response.copyObjectResult().eTag());
        } catch (NoSuchKeyException e) {
            log.warn("修改 S3 对象存储类型失败，对象不存在，bucketName={}，objectKey={}，storageClass={}",
                    resolvedBucketName, normalizedObjectKey, storageClass);
            throw e;
        } catch (NoSuchBucketException e) {
            log.warn("修改 S3 对象存储类型失败，存储桶不存在，bucketName={}，objectKey={}，storageClass={}",
                    resolvedBucketName, normalizedObjectKey, storageClass);
            throw e;
        } catch (S3Exception e) {
            log.error("修改 S3 对象存储类型失败，bucketName={}，objectKey={}，storageClass={}，statusCode={}，errorCode={}，message={}",
                    resolvedBucketName,
                    normalizedObjectKey,
                    storageClass,
                    e.statusCode(),
                    getErrorCode(e),
                    e.getMessage(),
                    e);
            throw e;
        }
    }

    /**
     * 恢复默认存储桶下归档对象
     *
     * @param objectKey 对象 Key
     * @param days      恢复副本保留天数
     */
    @Override
    public void restoreArchiveObject(String objectKey, Integer days) {
        restoreArchiveObject(getDefaultBucketName(), objectKey, days);
    }

    /**
     * 恢复指定存储桶下归档对象
     *
     * @param bucketName 存储桶名称
     * @param objectKey  对象 Key
     * @param days       恢复副本保留天数
     */
    @Override
    public void restoreArchiveObject(String bucketName, String objectKey, Integer days) {
        String resolvedBucketName = resolveBucketName(bucketName);
        String normalizedObjectKey = normalizeObjectKey(objectKey);
        Integer resolvedDays = normalizeRestoreDays(days);

        try {
            RestoreObjectRequest request = RestoreObjectRequest.builder()
                    .bucket(resolvedBucketName)
                    .key(normalizedObjectKey)
                    .restoreRequest(RestoreRequest.builder()
                            .days(resolvedDays)
                            .glacierJobParameters(GlacierJobParameters.builder()
                                    .tier(Tier.STANDARD)
                                    .build())
                            .build())
                    .build();

            s3Client.restoreObject(request);

            log.info("发起 S3 归档对象恢复成功，bucketName={}，objectKey={}，days={}，tier={}",
                    resolvedBucketName, normalizedObjectKey, resolvedDays, Tier.STANDARD);
        } catch (NoSuchKeyException e) {
            log.warn("恢复 S3 归档对象失败，对象不存在，bucketName={}，objectKey={}，days={}",
                    resolvedBucketName, normalizedObjectKey, resolvedDays);
            throw e;
        } catch (NoSuchBucketException e) {
            log.warn("恢复 S3 归档对象失败，存储桶不存在，bucketName={}，objectKey={}，days={}",
                    resolvedBucketName, normalizedObjectKey, resolvedDays);
            throw e;
        } catch (S3Exception e) {
            if (StrUtil.equalsAnyIgnoreCase(getErrorCode(e), "RestoreAlreadyInProgress")) {
                log.info("S3 归档对象恢复任务已在进行中，bucketName={}，objectKey={}，days={}",
                        resolvedBucketName, normalizedObjectKey, resolvedDays);
                return;
            }

            if (StrUtil.equalsAnyIgnoreCase(getErrorCode(e), "ObjectAlreadyInActiveTierError")) {
                log.info("S3 对象已处于可访问存储层，无需恢复，bucketName={}，objectKey={}",
                        resolvedBucketName, normalizedObjectKey);
                return;
            }

            log.error("恢复 S3 归档对象失败，bucketName={}，objectKey={}，days={}，statusCode={}，errorCode={}，message={}",
                    resolvedBucketName,
                    normalizedObjectKey,
                    resolvedDays,
                    e.statusCode(),
                    getErrorCode(e),
                    e.getMessage(),
                    e);
            throw e;
        }
    }

    // ==================== 分片上传 ====================

    /**
     * 初始化分片上传
     *
     * @param request 分片上传初始化请求
     * @return 分片上传初始化结果
     */
    @Override
    public S3MultipartUploadInitResult initMultipartUpload(S3MultipartUploadInitRequest request) {
        Assert.notNull(request, "S3 分片上传初始化请求不能为空");

        String resolvedBucketName = resolveBucketName(request.bucketName());
        String normalizedObjectKey = normalizeObjectKey(request.objectKey());

        try {
            CreateMultipartUploadRequest createRequest = buildCreateMultipartUploadRequest(
                    request,
                    resolvedBucketName,
                    normalizedObjectKey
            );

            CreateMultipartUploadResponse response = s3Client.createMultipartUpload(createRequest);

            log.info("初始化 S3 分片上传成功，bucketName={}，objectKey={}，uploadId={}",
                    resolvedBucketName, normalizedObjectKey, response.uploadId());

            return new S3MultipartUploadInitResult(
                    resolvedBucketName,
                    normalizedObjectKey,
                    response.uploadId()
            );
        } catch (NoSuchBucketException e) {
            log.warn("初始化 S3 分片上传失败，存储桶不存在，bucketName={}，objectKey={}",
                    resolvedBucketName, normalizedObjectKey);
            throw e;
        } catch (S3Exception e) {
            log.error("初始化 S3 分片上传失败，bucketName={}，objectKey={}，statusCode={}，errorCode={}，message={}",
                    resolvedBucketName, normalizedObjectKey, e.statusCode(), getErrorCode(e), e.getMessage(), e);
            throw e;
        }
    }

    /**
     * 上传分片
     *
     * @param request 分片上传请求
     * @return 分片上传结果
     */
    @Override
    public S3MultipartUploadPartResult uploadPart(S3MultipartUploadPartRequest request) {
        Assert.notNull(request, "S3 分片上传请求不能为空");

        String resolvedBucketName = resolveBucketName(request.bucketName());
        String normalizedObjectKey = normalizeObjectKey(request.objectKey());
        String resolvedUploadId = normalizeUploadId(request.uploadId());
        Integer resolvedPartNumber = normalizePartNumber(request.partNumber());

        Assert.notNull(request.inputStream(), "S3 分片上传输入流不能为空");
        Assert.notNull(request.contentLength(), "S3 分片上传内容长度不能为空");
        Assert.isTrue(request.contentLength() > 0, "S3 分片上传内容长度必须大于 0");

        try {
            UploadPartRequest uploadPartRequest = UploadPartRequest.builder()
                    .bucket(resolvedBucketName)
                    .key(normalizedObjectKey)
                    .uploadId(resolvedUploadId)
                    .partNumber(resolvedPartNumber)
                    .contentLength(request.contentLength())
                    .build();

            UploadPartResponse response = s3Client.uploadPart(
                    uploadPartRequest,
                    RequestBody.fromInputStream(request.inputStream(), request.contentLength())
            );

            log.info("上传 S3 分片成功，bucketName={}，objectKey={}，uploadId={}，partNumber={}，size={}，eTag={}",
                    resolvedBucketName,
                    normalizedObjectKey,
                    resolvedUploadId,
                    resolvedPartNumber,
                    request.contentLength(),
                    response.eTag());

            return new S3MultipartUploadPartResult(
                    resolvedPartNumber,
                    response.eTag(),
                    request.contentLength()
            );
        } catch (NoSuchBucketException e) {
            log.warn("上传 S3 分片失败，存储桶不存在，bucketName={}，objectKey={}，uploadId={}，partNumber={}",
                    resolvedBucketName, normalizedObjectKey, resolvedUploadId, resolvedPartNumber);
            throw e;
        } catch (S3Exception e) {
            log.error("上传 S3 分片失败，bucketName={}，objectKey={}，uploadId={}，partNumber={}，statusCode={}，errorCode={}，message={}",
                    resolvedBucketName,
                    normalizedObjectKey,
                    resolvedUploadId,
                    resolvedPartNumber,
                    e.statusCode(),
                    getErrorCode(e),
                    e.getMessage(),
                    e);
            throw e;
        }
    }

    /**
     * 完成分片上传
     *
     * @param request 分片上传完成请求
     * @return 上传结果
     */
    @Override
    public S3UploadResult completeMultipartUpload(S3MultipartUploadCompleteRequest request) {
        Assert.notNull(request, "S3 完成分片上传请求不能为空");

        String resolvedBucketName = resolveBucketName(request.bucketName());
        String normalizedObjectKey = normalizeObjectKey(request.objectKey());
        String resolvedUploadId = normalizeUploadId(request.uploadId());
        List<S3MultipartUploadPartResult> normalizedParts = normalizeMultipartUploadParts(request.parts());

        try {
            CompleteMultipartUploadRequest completeRequest = CompleteMultipartUploadRequest.builder()
                    .bucket(resolvedBucketName)
                    .key(normalizedObjectKey)
                    .uploadId(resolvedUploadId)
                    .multipartUpload(CompletedMultipartUpload.builder()
                            .parts(toCompletedParts(normalizedParts))
                            .build())
                    .build();

            CompleteMultipartUploadResponse response = s3Client.completeMultipartUpload(completeRequest);

            S3ObjectInfo objectInfo = tryGetObjectInfoAfterComplete(resolvedBucketName, normalizedObjectKey);
            Long totalSize = objectInfo == null ? sumMultipartUploadPartSize(normalizedParts) : objectInfo.size();
            String contentType = objectInfo == null ? null : objectInfo.contentType();

            S3UploadResult result = new S3UploadResult(
                    resolvedBucketName,
                    normalizedObjectKey,
                    getFilename(normalizedObjectKey),
                    contentType,
                    totalSize,
                    response.eTag(),
                    response.versionId(),
                    getPublicUrl(resolvedBucketName, normalizedObjectKey),
                    Instant.now()
            );

            log.info("完成 S3 分片上传成功，bucketName={}，objectKey={}，uploadId={}，partCount={}，size={}，eTag={}",
                    resolvedBucketName,
                    normalizedObjectKey,
                    resolvedUploadId,
                    normalizedParts.size(),
                    result.size(),
                    result.eTag());

            return result;
        } catch (NoSuchBucketException e) {
            log.warn("完成 S3 分片上传失败，存储桶不存在，bucketName={}，objectKey={}，uploadId={}",
                    resolvedBucketName, normalizedObjectKey, resolvedUploadId);
            throw e;
        } catch (NoSuchKeyException e) {
            log.warn("完成 S3 分片上传失败，对象不存在，bucketName={}，objectKey={}，uploadId={}",
                    resolvedBucketName, normalizedObjectKey, resolvedUploadId);
            throw e;
        } catch (S3Exception e) {
            log.error("完成 S3 分片上传失败，bucketName={}，objectKey={}，uploadId={}，statusCode={}，errorCode={}，message={}",
                    resolvedBucketName,
                    normalizedObjectKey,
                    resolvedUploadId,
                    e.statusCode(),
                    getErrorCode(e),
                    e.getMessage(),
                    e);
            throw e;
        }
    }

    /**
     * 终止分片上传
     *
     * @param bucketName 存储桶名称
     * @param objectKey  对象 Key
     * @param uploadId   上传 ID
     */
    @Override
    public void abortMultipartUpload(String bucketName, String objectKey, String uploadId) {
        String resolvedBucketName = resolveBucketName(bucketName);
        String normalizedObjectKey = normalizeObjectKey(objectKey);
        String resolvedUploadId = normalizeUploadId(uploadId);

        try {
            AbortMultipartUploadRequest request = AbortMultipartUploadRequest.builder()
                    .bucket(resolvedBucketName)
                    .key(normalizedObjectKey)
                    .uploadId(resolvedUploadId)
                    .build();

            s3Client.abortMultipartUpload(request);

            log.info("终止 S3 分片上传成功，bucketName={}，objectKey={}，uploadId={}",
                    resolvedBucketName, normalizedObjectKey, resolvedUploadId);
        } catch (NoSuchBucketException e) {
            log.warn("终止 S3 分片上传失败，存储桶不存在，bucketName={}，objectKey={}，uploadId={}",
                    resolvedBucketName, normalizedObjectKey, resolvedUploadId);
            throw e;
        } catch (S3Exception e) {
            if (isNotFound(e)) {
                log.info("终止 S3 分片上传时任务已不存在，bucketName={}，objectKey={}，uploadId={}，errorCode={}",
                        resolvedBucketName, normalizedObjectKey, resolvedUploadId, getErrorCode(e));
                return;
            }

            log.error("终止 S3 分片上传失败，bucketName={}，objectKey={}，uploadId={}，statusCode={}，errorCode={}，message={}",
                    resolvedBucketName,
                    normalizedObjectKey,
                    resolvedUploadId,
                    e.statusCode(),
                    getErrorCode(e),
                    e.getMessage(),
                    e);
            throw e;
        }
    }

    /**
     * 查询未完成的分片上传任务
     *
     * @param bucketName 存储桶名称
     * @param prefix     对象 Key 前缀
     * @return 分片上传任务列表
     */
    @Override
    public List<S3MultipartUploadInfo> listMultipartUploads(String bucketName, String prefix) {
        String resolvedBucketName = resolveBucketName(bucketName);
        String normalizedPrefix = normalizeObjectPrefix(prefix);

        List<S3MultipartUploadInfo> uploads = new ArrayList<>();
        String keyMarker = null;
        String uploadIdMarker = null;

        try {
            do {
                ListMultipartUploadsRequest.Builder builder = ListMultipartUploadsRequest.builder()
                        .bucket(resolvedBucketName)
                        .maxUploads(1000);

                if (StrUtil.isNotBlank(normalizedPrefix)) {
                    builder.prefix(normalizedPrefix);
                }

                if (StrUtil.isNotBlank(keyMarker)) {
                    builder.keyMarker(keyMarker);
                }

                if (StrUtil.isNotBlank(uploadIdMarker)) {
                    builder.uploadIdMarker(uploadIdMarker);
                }

                ListMultipartUploadsResponse response = s3Client.listMultipartUploads(builder.build());

                uploads.addAll(response.uploads()
                        .stream()
                        .map(upload -> buildS3MultipartUploadInfo(resolvedBucketName, upload))
                        .toList());

                keyMarker = response.nextKeyMarker();
                uploadIdMarker = response.nextUploadIdMarker();
            } while (StrUtil.isNotBlank(keyMarker));

            return uploads;
        } catch (NoSuchBucketException e) {
            log.warn("查询 S3 未完成分片上传任务失败，存储桶不存在，bucketName={}，prefix={}",
                    resolvedBucketName, normalizedPrefix);
            throw e;
        } catch (S3Exception e) {
            log.error("查询 S3 未完成分片上传任务失败，bucketName={}，prefix={}，statusCode={}，errorCode={}，message={}",
                    resolvedBucketName, normalizedPrefix, e.statusCode(), getErrorCode(e), e.getMessage(), e);
            throw e;
        }
    }

    /**
     * 查询指定分片上传任务已上传的分片列表
     *
     * @param bucketName 存储桶名称
     * @param objectKey  对象 Key
     * @param uploadId   上传 ID
     * @return 已上传分片列表
     */
    @Override
    public List<S3MultipartUploadPartInfo> listMultipartUploadParts(String bucketName, String objectKey, String uploadId) {
        String resolvedBucketName = resolveBucketName(bucketName);
        String normalizedObjectKey = normalizeObjectKey(objectKey);
        String resolvedUploadId = normalizeUploadId(uploadId);

        List<S3MultipartUploadPartInfo> parts = new ArrayList<>();
        Integer partNumberMarker = null;

        try {
            do {
                ListPartsRequest.Builder builder = ListPartsRequest.builder()
                        .bucket(resolvedBucketName)
                        .key(normalizedObjectKey)
                        .uploadId(resolvedUploadId)
                        .maxParts(1000);

                if (partNumberMarker != null) {
                    builder.partNumberMarker(partNumberMarker);
                }

                ListPartsResponse response = s3Client.listParts(builder.build());

                parts.addAll(response.parts()
                        .stream()
                        .map(this::buildS3MultipartUploadPartInfo)
                        .toList());

                partNumberMarker = response.nextPartNumberMarker();
            } while (partNumberMarker != null);

            return parts;
        } catch (NoSuchBucketException e) {
            log.warn("查询 S3 已上传分片列表失败，存储桶不存在，bucketName={}，objectKey={}，uploadId={}",
                    resolvedBucketName, normalizedObjectKey, resolvedUploadId);
            throw e;
        } catch (S3Exception e) {
            if (isNotFound(e)) {
                log.info("查询 S3 已上传分片列表时任务不存在，bucketName={}，objectKey={}，uploadId={}，errorCode={}",
                        resolvedBucketName, normalizedObjectKey, resolvedUploadId, getErrorCode(e));
                return List.of();
            }

            log.error("查询 S3 已上传分片列表失败，bucketName={}，objectKey={}，uploadId={}，statusCode={}，errorCode={}，message={}",
                    resolvedBucketName,
                    normalizedObjectKey,
                    resolvedUploadId,
                    e.statusCode(),
                    getErrorCode(e),
                    e.getMessage(),
                    e);
            throw e;
        }
    }

    // ==================== 私有辅助方法 ====================

    /**
     * 构建创建存储桶请求
     *
     * @param bucketName 存储桶名称
     * @return 创建存储桶请求
     */
    private CreateBucketRequest buildCreateBucketRequest(String bucketName) {
        CreateBucketRequest.Builder builder = CreateBucketRequest.builder()
                .bucket(bucketName);

        String region = StrUtil.trim(s3Properties.getRegion());

        if (StrUtil.isNotBlank(region) && !StrUtil.equalsIgnoreCase(REGION_US_EAST_1, region)) {
            CreateBucketConfiguration configuration = CreateBucketConfiguration.builder()
                    .locationConstraint(BucketLocationConstraint.fromValue(region))
                    .build();

            builder.createBucketConfiguration(configuration);
        }

        return builder.build();
    }

    /**
     * 判断是否为资源不存在异常
     *
     * @param e S3 异常
     * @return true 是，false 否
     */
    private boolean isNotFound(S3Exception e) {
        return e.statusCode() == 404
                || StrUtil.equalsAnyIgnoreCase(getErrorCode(e), "NoSuchBucket", "NotFound");
    }

    /**
     * 判断是否为无权限异常
     *
     * @param e S3 异常
     * @return true 是，false 否
     */
    private boolean isForbidden(S3Exception e) {
        return e.statusCode() == 403
                || StrUtil.equalsAnyIgnoreCase(getErrorCode(e), "AccessDenied", "Forbidden");
    }

    /**
     * 判断存储桶是否已归当前凭证所有
     *
     * @param e S3 异常
     * @return true 是，false 否
     */
    private boolean isBucketAlreadyOwnedByYou(S3Exception e) {
        return StrUtil.equalsAnyIgnoreCase(getErrorCode(e), "BucketAlreadyOwnedByYou");
    }

    /**
     * 判断存储桶是否已经存在
     *
     * @param e S3 异常
     * @return true 是，false 否
     */
    private boolean isBucketAlreadyExists(S3Exception e) {
        return e.statusCode() == 409
                || StrUtil.equalsAnyIgnoreCase(getErrorCode(e), "BucketAlreadyExists");
    }

    /**
     * 获取 S3 错误码
     *
     * @param e S3 异常
     * @return 错误码
     */
    private String getErrorCode(S3Exception e) {
        if (e == null || e.awsErrorDetails() == null) {
            return null;
        }
        return e.awsErrorDetails().errorCode();
    }

    /**
     * 构建标准 AWS S3 公开访问 URL
     *
     * @param bucketName       存储桶名称
     * @param encodedObjectKey 已编码对象 Key
     * @param pathStyleAccess  是否路径风格访问
     * @return 公开访问 URL
     */
    private String buildAwsDefaultPublicUrl(String bucketName, String encodedObjectKey, boolean pathStyleAccess) {
        String region = StrUtil.blankToDefault(StrUtil.trim(s3Properties.getRegion()), REGION_US_EAST_1);
        String encodedBucketName = encodePathSegment(bucketName);

        if (pathStyleAccess) {
            return "https://s3." + region + ".amazonaws.com/" + encodedBucketName + "/" + encodedObjectKey;
        }

        return "https://" + bucketName + ".s3." + region + ".amazonaws.com/" + encodedObjectKey;
    }

    /**
     * 构建虚拟主机风格公开访问 URL
     *
     * @param endpointUri      endpoint URI
     * @param bucketName       存储桶名称
     * @param encodedObjectKey 已编码对象 Key
     * @return 公开访问 URL
     */
    private String buildVirtualHostPublicUrl(URI endpointUri, String bucketName, String encodedObjectKey) {
        String scheme = endpointUri.getScheme();
        String host = endpointUri.getHost();
        int port = endpointUri.getPort();
        String rawPath = StrUtil.blankToDefault(endpointUri.getRawPath(), StrUtil.EMPTY);

        String authority = bucketName + "." + host;
        if (port > 0) {
            authority = authority + ":" + port;
        }

        String baseUrl = scheme + "://" + authority;
        if (StrUtil.isNotBlank(rawPath) && !StrUtil.equals(rawPath, "/")) {
            baseUrl = joinUrlPath(baseUrl, StrUtil.removePrefix(rawPath, "/"));
        }

        return joinUrlPath(baseUrl, encodedObjectKey);
    }

    /**
     * 规范化 endpoint
     *
     * @param endpoint endpoint
     * @return 规范化后的 endpoint
     */
    private String normalizeEndpoint(String endpoint) {
        String normalizedEndpoint = StrUtil.trim(endpoint);

        while (StrUtil.endWith(normalizedEndpoint, "/")) {
            normalizedEndpoint = StrUtil.removeSuffix(normalizedEndpoint, "/");
        }

        return normalizedEndpoint;
    }

    /**
     * 拼接 URL 路径
     *
     * @param baseUrl URL 基础地址
     * @param paths   路径片段
     * @return 拼接后的 URL
     */
    private String joinUrlPath(String baseUrl, String... paths) {
        String url = StrUtil.removeSuffix(baseUrl, "/");

        for (String path : paths) {
            if (StrUtil.isBlank(path)) {
                continue;
            }

            url = url + "/" + StrUtil.removePrefix(path, "/");
        }

        return url;
    }

    /**
     * 编码对象 Key，保留路径分隔符
     *
     * @param objectKey 对象 Key
     * @return 编码后的对象 Key
     */
    private String encodeObjectKey(String objectKey) {
        String[] parts = objectKey.split("/", -1);
        StringBuilder builder = new StringBuilder();

        for (int i = 0; i < parts.length; i++) {
            if (i > 0) {
                builder.append("/");
            }
            builder.append(encodePathSegment(parts[i]));
        }

        return builder.toString();
    }

    /**
     * 编码 URL 路径片段
     *
     * @param pathSegment 路径片段
     * @return 编码后的路径片段
     */
    private String encodePathSegment(String pathSegment) {
        if (StrUtil.isEmpty(pathSegment)) {
            return StrUtil.EMPTY;
        }

        return URLEncoder.encode(pathSegment, StandardCharsets.UTF_8)
                .replace("+", "%20")
                .replace("%7E", "~");
    }

    /**
     * 判断 endpoint 是否不适合虚拟主机风格访问
     *
     * @param endpointUri endpoint URI
     * @return true 不适合，false 适合
     */
    private boolean isEndpointNotSuitableForVirtualHost(URI endpointUri) {
        String host = endpointUri.getHost();

        if (StrUtil.isBlank(host)) {
            return true;
        }

        return StrUtil.equalsAnyIgnoreCase(host, "localhost")
                || host.matches("\\d{1,3}(\\.\\d{1,3}){3}")
                || StrUtil.contains(host, ":");
    }

    /**
     * 构建 HeadObject 请求
     *
     * @param bucketName 存储桶名称
     * @param objectKey  对象 Key
     * @return HeadObject 请求
     */
    private HeadObjectRequest buildHeadObjectRequest(String bucketName, String objectKey) {
        return HeadObjectRequest.builder()
                .bucket(bucketName)
                .key(objectKey)
                .build();
    }

    /**
     * 构建 S3 对象信息
     *
     * @param bucketName 存储桶名称
     * @param objectKey  对象 Key
     * @param response   HeadObject 响应
     * @return S3 对象信息
     */
    private S3ObjectInfo buildS3ObjectInfo(String bucketName, String objectKey, HeadObjectResponse response) {
        Map<String, String> metadata = response.metadata() == null
                ? Map.of()
                : Map.copyOf(response.metadata());

        return new S3ObjectInfo(
                bucketName,
                objectKey,
                getFilename(objectKey),
                response.contentType(),
                response.contentLength(),
                response.eTag(),
                response.versionId(),
                toS3StorageClass(response.storageClassAsString()),
                response.lastModified(),
                metadata,
                Map.of()
        );
    }

    /**
     * 转换 S3 存储类型
     *
     * @param storageClass 存储类型字符串
     * @return 存储类型枚举
     */
    private S3StorageClass toS3StorageClass(String storageClass) {
        if (StrUtil.isBlank(storageClass)) {
            return S3StorageClass.STANDARD;
        }

        try {
            return S3StorageClass.valueOf(storageClass);
        } catch (IllegalArgumentException e) {
            log.warn("发现未适配的 S3 存储类型，storageClass={}", storageClass);
            return null;
        }
    }

    /**
     * 执行对象上传
     *
     * @param request          上传请求
     * @param originalFilename 原始文件名
     * @param requestBody      请求体
     * @return 上传结果
     */
    private S3UploadResult uploadInternal(S3UploadRequest request, String originalFilename, RequestBody requestBody) {
        Assert.notNull(request, "S3 上传请求不能为空");
        Assert.notNull(requestBody, "S3 上传请求体不能为空");

        PutObjectRequest putObjectRequest = buildPutObjectRequest(request);
        String bucketName = putObjectRequest.bucket();
        String objectKey = putObjectRequest.key();
        String resolvedOriginalFilename = StrUtil.blankToDefault(StrUtil.trim(originalFilename), getFilename(objectKey));

        try {
            PutObjectResponse response = s3Client.putObject(putObjectRequest, requestBody);

            S3UploadResult result = new S3UploadResult(
                    bucketName,
                    objectKey,
                    resolvedOriginalFilename,
                    putObjectRequest.contentType(),
                    putObjectRequest.contentLength(),
                    response.eTag(),
                    response.versionId(),
                    getPublicUrl(bucketName, objectKey),
                    Instant.now()
            );

            log.info("上传 S3 对象成功，bucketName={}，objectKey={}，size={}，contentType={}",
                    bucketName, objectKey, result.size(), result.contentType());

            return result;
        } catch (S3Exception e) {
            log.error("上传 S3 对象失败，bucketName={}，objectKey={}，statusCode={}，errorCode={}，message={}",
                    bucketName, objectKey, e.statusCode(), getErrorCode(e), e.getMessage(), e);
            throw e;
        }
    }

    /**
     * 构建 PutObject 请求
     *
     * @param request 上传请求
     * @return PutObject 请求
     */
    private PutObjectRequest buildPutObjectRequest(S3UploadRequest request) {
        String bucketName = resolveBucketName(request.bucketName());
        String objectKey = normalizeObjectKey(request.objectKey());

        Assert.notNull(request.contentLength(), "S3 上传内容长度不能为空");
        Assert.isTrue(request.contentLength() >= 0, "S3 上传内容长度不能小于 0");

        PutObjectRequest.Builder builder = PutObjectRequest.builder()
                .bucket(bucketName)
                .key(objectKey)
                .contentLength(request.contentLength())
                .contentType(resolveContentType(request.contentType(), objectKey));

        Map<String, String> metadata = normalizeMetadata(request.metadata());
        if (MapUtil.isNotEmpty(metadata)) {
            builder.metadata(metadata);
        }

        String tagging = buildTagging(request.tags());
        if (StrUtil.isNotBlank(tagging)) {
            builder.tagging(tagging);
        }

        if (request.acl() != null) {
            builder.acl(toAwsObjectCannedAcl(request.acl()));
        }

        if (request.storageClass() != null) {
            builder.storageClass(toAwsStorageClass(request.storageClass()));
        }

        configureServerSideEncryption(builder, request.serverSideEncryption(), request.kmsKeyId());

        return builder.build();
    }

    /**
     * 生成自动上传对象 Key
     *
     * @param originalFilename 原始文件名
     * @return 对象 Key
     */
    private String buildAutoObjectKey(String originalFilename) {
        String filename = normalizeOriginalFilename(originalFilename);
        String extension = FileNameUtil.extName(filename);

        String generatedFilename = IdUtil.fastSimpleUUID();
        if (StrUtil.isNotBlank(extension)) {
            generatedFilename = generatedFilename + "." + extension;
        }

        String directory = DEFAULT_UPLOAD_DIRECTORY + "/" + LocalDate.now().format(DEFAULT_DATE_PATH_FORMATTER);
        return buildObjectKey(directory, generatedFilename);
    }

    /**
     * 规范化原始文件名
     *
     * @param originalFilename 原始文件名
     * @return 规范化后的文件名
     */
    private String normalizeOriginalFilename(String originalFilename) {
        String filename = StrUtil.trim(originalFilename);

        if (StrUtil.isBlank(filename)) {
            return "file";
        }

        filename = StrUtil.replace(filename, "\\", "/");
        filename = FileNameUtil.getName(filename);

        return StrUtil.blankToDefault(filename, "file");
    }

    /**
     * 解析 Content-Type
     *
     * @param contentType 显式 Content-Type
     * @param objectKey   对象 Key
     * @return Content-Type
     */
    private String resolveContentType(String contentType, String objectKey) {
        if (StrUtil.isNotBlank(contentType)) {
            return StrUtil.trim(contentType);
        }

        String guessedContentType = URLConnection.guessContentTypeFromName(objectKey);
        return StrUtil.blankToDefault(guessedContentType, DEFAULT_CONTENT_TYPE);
    }

    /**
     * 规范化对象元数据
     *
     * @param metadata 元数据
     * @return 规范化后的元数据
     */
    private Map<String, String> normalizeMetadata(Map<String, String> metadata) {
        if (MapUtil.isEmpty(metadata)) {
            return Map.of();
        }

        Map<String, String> normalizedMetadata = new LinkedHashMap<>();

        metadata.forEach((key, value) -> {
            if (StrUtil.isBlank(key) || value == null) {
                return;
            }
            normalizedMetadata.put(StrUtil.trim(key), value);
        });

        return normalizedMetadata;
    }

    /**
     * 构建 S3 标签字符串
     *
     * @param tags 标签
     * @return 标签字符串
     */
    private String buildTagging(Map<String, String> tags) {
        if (MapUtil.isEmpty(tags)) {
            return null;
        }

        Map<String, String> normalizedTags = new LinkedHashMap<>();

        tags.forEach((key, value) -> {
            if (StrUtil.isBlank(key) || value == null) {
                return;
            }
            normalizedTags.put(StrUtil.trim(key), value);
        });

        if (MapUtil.isEmpty(normalizedTags)) {
            return null;
        }

        Assert.isTrue(normalizedTags.size() <= 10, "S3 对象标签数量不能超过 10 个");

        List<String> tagParts = new ArrayList<>(normalizedTags.size());
        normalizedTags.forEach((key, value) -> tagParts.add(encodePathSegment(key) + "=" + encodePathSegment(value)));

        return String.join("&", tagParts);
    }

    /**
     * 配置服务端加密
     *
     * @param builder              PutObject 请求构建器
     * @param serverSideEncryption 服务端加密方式
     * @param kmsKeyId             KMS Key ID
     */
    private void configureServerSideEncryption(PutObjectRequest.Builder builder,
                                               S3ServerSideEncryption serverSideEncryption,
                                               String kmsKeyId) {
        if (serverSideEncryption == null && StrUtil.isBlank(kmsKeyId)) {
            return;
        }

        S3ServerSideEncryption resolvedEncryption = serverSideEncryption;
        if (resolvedEncryption == null) {
            resolvedEncryption = S3ServerSideEncryption.AWS_KMS;
        }

        builder.serverSideEncryption(toAwsServerSideEncryption(resolvedEncryption));

        if (resolvedEncryption == S3ServerSideEncryption.AWS_KMS) {
            Assert.notBlank(kmsKeyId, "S3 使用 AWS_KMS 服务端加密时 kmsKeyId 不能为空");
            builder.ssekmsKeyId(kmsKeyId);
        }
    }

    /**
     * 转换对象 ACL
     *
     * @param acl 对象 ACL
     * @return AWS SDK 对象 ACL
     */
    private ObjectCannedACL toAwsObjectCannedAcl(S3ObjectAcl acl) {
        return switch (acl) {
            case PRIVATE -> ObjectCannedACL.PRIVATE;
            case PUBLIC_READ -> ObjectCannedACL.PUBLIC_READ;
            case PUBLIC_READ_WRITE -> ObjectCannedACL.PUBLIC_READ_WRITE;
            case AUTHENTICATED_READ -> ObjectCannedACL.AUTHENTICATED_READ;
            case BUCKET_OWNER_READ -> ObjectCannedACL.BUCKET_OWNER_READ;
            case BUCKET_OWNER_FULL_CONTROL -> ObjectCannedACL.BUCKET_OWNER_FULL_CONTROL;
        };
    }

    /**
     * 转换存储类型
     *
     * @param storageClass 存储类型
     * @return AWS SDK 存储类型
     */
    private StorageClass toAwsStorageClass(S3StorageClass storageClass) {
        return switch (storageClass) {
            case STANDARD -> StorageClass.STANDARD;
            case INTELLIGENT_TIERING -> StorageClass.INTELLIGENT_TIERING;
            case STANDARD_IA -> StorageClass.STANDARD_IA;
            case ONEZONE_IA -> StorageClass.ONEZONE_IA;
            case GLACIER -> StorageClass.GLACIER;
            case DEEP_ARCHIVE -> StorageClass.DEEP_ARCHIVE;
            case REDUCED_REDUNDANCY -> StorageClass.REDUCED_REDUNDANCY;
        };
    }

    /**
     * 转换服务端加密方式
     *
     * @param serverSideEncryption 服务端加密方式
     * @return AWS SDK 服务端加密方式
     */
    private ServerSideEncryption toAwsServerSideEncryption(S3ServerSideEncryption serverSideEncryption) {
        return switch (serverSideEncryption) {
            case AES256 -> ServerSideEncryption.AES256;
            case AWS_KMS -> ServerSideEncryption.AWS_KMS;
        };
    }

    /**
     * 构建 GetObject 请求
     *
     * @param bucketName 存储桶名称
     * @param objectKey  对象 Key
     * @return GetObject 请求
     */
    private GetObjectRequest buildGetObjectRequest(String bucketName, String objectKey) {
        return GetObjectRequest.builder()
                .bucket(bucketName)
                .key(objectKey)
                .build();
    }

    /**
     * 构建 GetObject 请求
     *
     * @param request 下载请求
     * @return GetObject 请求
     */
    private GetObjectRequest buildGetObjectRequest(S3DownloadRequest request) {
        String bucketName = resolveBucketName(request.bucketName());
        String objectKey = normalizeObjectKey(request.objectKey());

        GetObjectRequest.Builder builder = GetObjectRequest.builder()
                .bucket(bucketName)
                .key(objectKey);

        if (StrUtil.isNotBlank(request.versionId())) {
            builder.versionId(request.versionId());
        }

        String range = getDownloadRange(request.rangeStart(), request.rangeEnd());
        if (StrUtil.isNotBlank(range)) {
            builder.range(range);
        }

        return builder.build();
    }

    /**
     * 构建 Range 下载头
     *
     * @param rangeStart 起始位置
     * @param rangeEnd   结束位置
     * @return Range 下载头
     */
    private String getDownloadRange(Long rangeStart, Long rangeEnd) {
        if (rangeStart == null && rangeEnd == null) {
            return null;
        }

        if (rangeStart != null) {
            Assert.isTrue(rangeStart >= 0, "S3 Range 下载起始位置不能小于 0");
        }

        if (rangeEnd != null) {
            Assert.isTrue(rangeEnd >= 0, "S3 Range 下载结束位置不能小于 0");
        }

        if (rangeStart != null && rangeEnd != null) {
            Assert.isTrue(rangeEnd >= rangeStart, "S3 Range 下载结束位置不能小于起始位置");
            return "bytes=" + rangeStart + "-" + rangeEnd;
        }

        if (rangeStart != null) {
            return "bytes=" + rangeStart + "-";
        }

        return "bytes=0-" + rangeEnd;
    }

    /**
     * 构建对象标识列表
     *
     * @param objectKeys 对象 Key 列表
     * @return 对象标识列表
     */
    private List<ObjectIdentifier> buildObjectIdentifiers(List<String> objectKeys) {
        Set<String> normalizedObjectKeys = new LinkedHashSet<>();

        for (String objectKey : objectKeys) {
            normalizedObjectKeys.add(normalizeObjectKey(objectKey));
        }

        return normalizedObjectKeys.stream()
                .map(objectKey -> ObjectIdentifier.builder()
                        .key(objectKey)
                        .build())
                .toList();
    }

    /**
     * 查询指定前缀下的对象标识列表
     *
     * @param bucketName 存储桶名称
     * @param prefix     对象 Key 前缀
     * @return 对象标识列表
     */
    private List<ObjectIdentifier> listObjectIdentifiersByPrefix(String bucketName, String prefix) {
        List<ObjectIdentifier> objectIdentifiers = new ArrayList<>();
        String continuationToken = null;

        try {
            do {
                ListObjectsV2Request.Builder builder = ListObjectsV2Request.builder()
                        .bucket(bucketName)
                        .prefix(prefix)
                        .maxKeys(1000);

                if (StrUtil.isNotBlank(continuationToken)) {
                    builder.continuationToken(continuationToken);
                }

                ListObjectsV2Response response = s3Client.listObjectsV2(builder.build());

                for (S3Object object : response.contents()) {
                    if (object == null || StrUtil.isBlank(object.key())) {
                        continue;
                    }

                    objectIdentifiers.add(ObjectIdentifier.builder()
                            .key(object.key())
                            .build());
                }

                continuationToken = response.nextContinuationToken();
            } while (StrUtil.isNotBlank(continuationToken));

            return objectIdentifiers;
        } catch (NoSuchBucketException e) {
            log.warn("按前缀查询 S3 对象失败，存储桶不存在，bucketName={}，prefix={}", bucketName, prefix);
            throw e;
        } catch (S3Exception e) {
            log.error("按前缀查询 S3 对象失败，bucketName={}，prefix={}，statusCode={}，errorCode={}，message={}",
                    bucketName, prefix, e.statusCode(), getErrorCode(e), e.getMessage(), e);
            throw e;
        }
    }

    /**
     * 删除对象标识列表
     *
     * @param bucketName        存储桶名称
     * @param objectIdentifiers 对象标识列表
     * @return 删除详细结果
     */
    private S3DeleteResult deleteObjectIdentifiers(String bucketName, List<ObjectIdentifier> objectIdentifiers) {
        if (CollUtil.isEmpty(objectIdentifiers)) {
            return emptyDeleteResult(bucketName);
        }

        List<String> deletedObjectKeys = new ArrayList<>();
        List<S3DeleteError> deleteErrors = new ArrayList<>();

        for (List<ObjectIdentifier> batch : partitionObjectIdentifiers(objectIdentifiers, 1000)) {
            S3DeleteResult batchResult = deleteObjectIdentifierBatch(bucketName, batch);
            deletedObjectKeys.addAll(batchResult.deletedObjectKeys());
            deleteErrors.addAll(batchResult.errors());
        }

        S3DeleteResult result = new S3DeleteResult(
                bucketName,
                List.copyOf(deletedObjectKeys),
                List.copyOf(deleteErrors),
                (long) deletedObjectKeys.size(),
                (long) deleteErrors.size()
        );

        log.info("批量删除 S3 对象完成，bucketName={}，deletedCount={}，errorCount={}",
                bucketName, result.deletedCount(), result.errorCount());

        return result;
    }

    /**
     * 删除单批对象标识列表
     *
     * @param bucketName        存储桶名称
     * @param objectIdentifiers 对象标识列表
     * @return 删除详细结果
     */
    private S3DeleteResult deleteObjectIdentifierBatch(String bucketName, List<ObjectIdentifier> objectIdentifiers) {
        if (CollUtil.isEmpty(objectIdentifiers)) {
            return emptyDeleteResult(bucketName);
        }

        try {
            DeleteObjectsRequest request = DeleteObjectsRequest.builder()
                    .bucket(bucketName)
                    .delete(Delete.builder()
                            .objects(objectIdentifiers)
                            .quiet(false)
                            .build())
                    .build();

            DeleteObjectsResponse response = s3Client.deleteObjects(request);

            List<String> deletedObjectKeys = response.deleted()
                    .stream()
                    .map(DeletedObject::key)
                    .filter(StrUtil::isNotBlank)
                    .toList();

            List<S3DeleteError> deleteErrors = response.errors()
                    .stream()
                    .map(this::toS3DeleteError)
                    .toList();

            return new S3DeleteResult(
                    bucketName,
                    deletedObjectKeys,
                    deleteErrors,
                    (long) deletedObjectKeys.size(),
                    (long) deleteErrors.size()
            );
        } catch (NoSuchBucketException e) {
            log.warn("批量删除 S3 对象失败，存储桶不存在，bucketName={}，count={}",
                    bucketName, objectIdentifiers.size());
            throw e;
        } catch (S3Exception e) {
            log.error("批量删除 S3 对象失败，bucketName={}，count={}，statusCode={}，errorCode={}，message={}",
                    bucketName, objectIdentifiers.size(), e.statusCode(), getErrorCode(e), e.getMessage(), e);
            throw e;
        }
    }

    /**
     * 分片对象标识列表
     *
     * @param objectIdentifiers 对象标识列表
     * @param batchSize         每批数量
     * @return 分片后的对象标识列表
     */
    private List<List<ObjectIdentifier>> partitionObjectIdentifiers(List<ObjectIdentifier> objectIdentifiers, int batchSize) {
        Assert.isTrue(batchSize > 0, "S3 批量删除分片大小必须大于 0");

        if (CollUtil.isEmpty(objectIdentifiers)) {
            return List.of();
        }

        List<List<ObjectIdentifier>> partitions = new ArrayList<>();

        for (int start = 0; start < objectIdentifiers.size(); start += batchSize) {
            int end = Math.min(start + batchSize, objectIdentifiers.size());
            partitions.add(objectIdentifiers.subList(start, end));
        }

        return partitions;
    }

    /**
     * 转换删除错误信息
     *
     * @param error S3 删除错误
     * @return 删除错误信息
     */
    private S3DeleteError toS3DeleteError(S3Error error) {
        return new S3DeleteError(
                error.key(),
                error.versionId(),
                error.code(),
                error.message()
        );
    }

    /**
     * 构建空删除结果
     *
     * @param bucketName 存储桶名称
     * @return 空删除结果
     */
    private S3DeleteResult emptyDeleteResult(String bucketName) {
        return new S3DeleteResult(
                bucketName,
                List.of(),
                List.of(),
                0L,
                0L
        );
    }

    /**
     * 校验删除结果是否存在失败项
     *
     * @param result 删除结果
     */
    private void assertNoDeleteErrors(S3DeleteResult result) {
        if (result == null || result.errorCount() == null || result.errorCount() <= 0) {
            return;
        }

        S3DeleteError firstError = result.errors().getFirst();

        String message = StrUtil.format(
                "S3 批量删除存在失败项，bucketName={}，deletedCount={}，errorCount={}，firstErrorKey={}，firstErrorCode={}，firstErrorMessage={}",
                result.bucketName(),
                result.deletedCount(),
                result.errorCount(),
                firstError.objectKey(),
                firstError.code(),
                firstError.message()
        );

        throw new IllegalStateException(message);
    }

    /**
     * 构建 CopyObject 请求
     *
     * @param request 复制请求
     * @return CopyObject 请求
     */
    private CopyObjectRequest buildCopyObjectRequest(S3CopyRequest request) {
        String sourceBucketName = resolveBucketName(request.sourceBucketName());
        String sourceKey = normalizeObjectKey(request.sourceKey());
        String targetBucketName = resolveBucketName(request.targetBucketName());
        String targetKey = normalizeObjectKey(request.targetKey());

        CopyObjectRequest.Builder builder = CopyObjectRequest.builder()
                .copySource(buildCopySource(sourceBucketName, sourceKey, request.sourceVersionId()))
                .sourceBucket(sourceBucketName)
                .sourceKey(sourceKey)
                .destinationBucket(targetBucketName)
                .destinationKey(targetKey)
                .bucket(targetBucketName)
                .key(targetKey);

        configureCopyMetadata(builder, request);
        configureCopyTags(builder, request);

        if (request.acl() != null) {
            builder.acl(toAwsObjectCannedAcl(request.acl()));
        }

        if (request.storageClass() != null) {
            builder.storageClass(toAwsStorageClass(request.storageClass()));
        }

        configureCopyServerSideEncryption(builder, request.serverSideEncryption(), request.kmsKeyId());

        return builder.build();
    }

    /**
     * 构建复制源路径
     *
     * @param bucketName 存储桶名称
     * @param objectKey  对象 Key
     * @param versionId  源对象版本 ID
     * @return 复制源路径
     */
    private String buildCopySource(String bucketName, String objectKey, String versionId) {
        String copySource = encodePathSegment(bucketName) + "/" + encodeObjectKey(objectKey);

        if (StrUtil.isNotBlank(versionId)) {
            copySource = copySource + "?versionId=" + encodePathSegment(versionId);
        }

        return copySource;
    }

    /**
     * 配置复制对象元数据
     *
     * @param builder CopyObject 请求构建器
     * @param request 复制请求
     */
    private void configureCopyMetadata(CopyObjectRequest.Builder builder, S3CopyRequest request) {
        boolean replaceMetadata = BooleanUtil.isTrue(request.replaceMetadata());

        if (!replaceMetadata) {
            builder.metadataDirective(MetadataDirective.COPY);
            return;
        }

        builder.metadataDirective(MetadataDirective.REPLACE);

        Map<String, String> metadata = normalizeMetadata(request.metadata());
        if (MapUtil.isNotEmpty(metadata)) {
            builder.metadata(metadata);
        }
    }

    /**
     * 配置复制对象标签
     *
     * @param builder CopyObject 请求构建器
     * @param request 复制请求
     */
    private void configureCopyTags(CopyObjectRequest.Builder builder, S3CopyRequest request) {
        boolean replaceTags = BooleanUtil.isTrue(request.replaceTags());

        if (!replaceTags) {
            builder.taggingDirective(TaggingDirective.COPY);
            return;
        }

        builder.taggingDirective(TaggingDirective.REPLACE);

        String tagging = buildTagging(request.tags());
        builder.tagging(StrUtil.blankToDefault(tagging, StrUtil.EMPTY));
    }

    /**
     * 配置复制对象服务端加密
     *
     * @param builder              CopyObject 请求构建器
     * @param serverSideEncryption 服务端加密方式
     * @param kmsKeyId             KMS Key ID
     */
    private void configureCopyServerSideEncryption(CopyObjectRequest.Builder builder,
                                                   S3ServerSideEncryption serverSideEncryption,
                                                   String kmsKeyId) {
        if (serverSideEncryption == null && StrUtil.isBlank(kmsKeyId)) {
            return;
        }

        S3ServerSideEncryption resolvedEncryption = serverSideEncryption;
        if (resolvedEncryption == null) {
            resolvedEncryption = S3ServerSideEncryption.AWS_KMS;
        }

        builder.serverSideEncryption(toAwsServerSideEncryption(resolvedEncryption));

        if (resolvedEncryption == S3ServerSideEncryption.AWS_KMS) {
            Assert.notBlank(kmsKeyId, "S3 使用 AWS_KMS 服务端加密时 kmsKeyId 不能为空");
            builder.ssekmsKeyId(kmsKeyId);
        }
    }

    /**
     * 判断是否为同一个对象
     *
     * @param sourceBucketName 源存储桶名称
     * @param sourceKey        源对象 Key
     * @param targetBucketName 目标存储桶名称
     * @param targetKey        目标对象 Key
     * @return true 是，false 否
     */
    private boolean isSameObject(String sourceBucketName, String sourceKey, String targetBucketName, String targetKey) {
        return StrUtil.equals(sourceBucketName, targetBucketName)
                && StrUtil.equals(sourceKey, targetKey);
    }

    /**
     * 构建 ListObjectsV2 请求
     *
     * @param bucketName        存储桶名称
     * @param prefix            对象 Key 前缀
     * @param delimiter         分隔符
     * @param maxKeys           最大返回数量
     * @param continuationToken 分页令牌
     * @param recursive         是否递归查询
     * @return ListObjectsV2 请求
     */
    private ListObjectsV2Request buildListObjectsV2Request(String bucketName,
                                                           String prefix,
                                                           String delimiter,
                                                           Integer maxKeys,
                                                           String continuationToken,
                                                           Boolean recursive) {
        ListObjectsV2Request.Builder builder = ListObjectsV2Request.builder()
                .bucket(bucketName)
                .maxKeys(normalizeMaxKeys(maxKeys));

        if (StrUtil.isNotBlank(prefix)) {
            builder.prefix(prefix);
        }

        if (!BooleanUtil.isTrue(recursive) && StrUtil.isNotBlank(delimiter)) {
            builder.delimiter(delimiter);
        }

        if (StrUtil.isNotBlank(continuationToken)) {
            builder.continuationToken(continuationToken);
        }

        return builder.build();
    }

    /**
     * 规范化对象 Key 前缀
     *
     * @param prefix 对象 Key 前缀
     * @return 规范化后的对象 Key 前缀
     */
    private String normalizeObjectPrefix(String prefix) {
        String normalizedPrefix = StrUtil.trim(prefix);

        if (StrUtil.isBlank(normalizedPrefix)) {
            return null;
        }

        normalizedPrefix = StrUtil.replace(normalizedPrefix, "\\", "/");
        normalizedPrefix = normalizedPrefix.replaceAll("/{2,}", "/");

        while (StrUtil.startWith(normalizedPrefix, "/")) {
            normalizedPrefix = StrUtil.removePrefix(normalizedPrefix, "/");
        }

        while (StrUtil.startWith(normalizedPrefix, "./")) {
            normalizedPrefix = StrUtil.removePrefix(normalizedPrefix, "./");
        }

        return StrUtil.blankToDefault(normalizedPrefix, null);
    }

    /**
     * 规范化分页最大返回数量
     *
     * @param maxKeys 最大返回数量
     * @return 规范化后的最大返回数量
     */
    private Integer normalizeMaxKeys(Integer maxKeys) {
        if (maxKeys == null) {
            return 1000;
        }

        if (maxKeys < 1) {
            return 1;
        }

        return Math.min(maxKeys, 1000);
    }

    /**
     * 根据 ListObjectsV2 对象构建对象信息
     *
     * @param bucketName 存储桶名称
     * @param object     S3 对象
     * @return 对象信息
     */
    private S3ObjectInfo buildS3ObjectInfo(String bucketName, S3Object object) {
        return new S3ObjectInfo(
                bucketName,
                object.key(),
                getFilename(object.key()),
                null,
                object.size(),
                object.eTag(),
                null,
                toS3StorageClass(object.storageClassAsString()),
                object.lastModified(),
                Map.of(),
                Map.of()
        );
    }

    /**
     * 根据对象版本构建对象版本信息
     *
     * @param bucketName    存储桶名称
     * @param objectVersion 对象版本
     * @return 对象版本信息
     */
    private S3ObjectVersionInfo buildS3ObjectVersionInfo(String bucketName, ObjectVersion objectVersion) {
        return new S3ObjectVersionInfo(
                bucketName,
                objectVersion.key(),
                objectVersion.versionId(),
                objectVersion.isLatest(),
                false,
                objectVersion.size(),
                objectVersion.eTag(),
                objectVersion.lastModified()
        );
    }

    /**
     * 根据删除标记构建对象版本信息
     *
     * @param bucketName   存储桶名称
     * @param deleteMarker 删除标记
     * @return 对象版本信息
     */
    private S3ObjectVersionInfo buildS3ObjectVersionInfo(String bucketName, DeleteMarkerEntry deleteMarker) {
        return new S3ObjectVersionInfo(
                bucketName,
                deleteMarker.key(),
                deleteMarker.versionId(),
                deleteMarker.isLatest(),
                true,
                null,
                null,
                deleteMarker.lastModified()
        );
    }

    /**
     * 规范化目录 Key
     *
     * @param directoryKey 目录 Key
     * @return 规范化后的目录 Key
     */
    private String normalizeDirectoryKey(String directoryKey) {
        String normalizedDirectoryKey = normalizeObjectKey(directoryKey);
        normalizedDirectoryKey = StrUtil.removeSuffix(normalizedDirectoryKey, "/");

        Assert.notBlank(normalizedDirectoryKey, "S3 目录 Key 不能为空");

        return normalizedDirectoryKey + "/";
    }

    /**
     * 生成 GET 对象预签名 URL
     *
     * @param request           预签名请求
     * @param bucketName        存储桶名称
     * @param objectKey         对象 Key
     * @param signatureDuration 签名有效期
     * @return 预签名 URL
     */
    private URI presignGetObject(S3PresignedUrlRequest request,
                                 String bucketName,
                                 String objectKey,
                                 Duration signatureDuration) {
        GetObjectRequest.Builder getObjectBuilder = GetObjectRequest.builder()
                .bucket(bucketName)
                .key(objectKey);

        configureGetObjectResponseHeaders(getObjectBuilder, request);
        configureRequestHeaders(getObjectBuilder, request.requestHeaders());

        GetObjectPresignRequest presignRequest = GetObjectPresignRequest.builder()
                .signatureDuration(signatureDuration)
                .getObjectRequest(getObjectBuilder.build())
                .build();

        PresignedGetObjectRequest presignedRequest = s3Presigner.presignGetObject(presignRequest);
        return URI.create(presignedRequest.url().toString());
    }

    /**
     * 生成 PUT 对象预签名 URL
     *
     * @param request           预签名请求
     * @param bucketName        存储桶名称
     * @param objectKey         对象 Key
     * @param signatureDuration 签名有效期
     * @return 预签名 URL
     */
    private URI presignPutObject(S3PresignedUrlRequest request,
                                 String bucketName,
                                 String objectKey,
                                 Duration signatureDuration) {
        PutObjectRequest.Builder putObjectBuilder = PutObjectRequest.builder()
                .bucket(bucketName)
                .key(objectKey)
                .contentType(resolveContentType(request.contentType(), objectKey));

        configureRequestHeaders(putObjectBuilder, request.requestHeaders());

        PutObjectPresignRequest presignRequest = PutObjectPresignRequest.builder()
                .signatureDuration(signatureDuration)
                .putObjectRequest(putObjectBuilder.build())
                .build();

        PresignedPutObjectRequest presignedRequest = s3Presigner.presignPutObject(presignRequest);
        return URI.create(presignedRequest.url().toString());
    }

    /**
     * 生成 HEAD 对象预签名 URL
     *
     * @param request           预签名请求
     * @param bucketName        存储桶名称
     * @param objectKey         对象 Key
     * @param signatureDuration 签名有效期
     * @return 预签名 URL
     */
    private URI presignHeadObject(S3PresignedUrlRequest request,
                                  String bucketName,
                                  String objectKey,
                                  Duration signatureDuration) {
        HeadObjectRequest.Builder headObjectBuilder = HeadObjectRequest.builder()
                .bucket(bucketName)
                .key(objectKey);

        configureRequestHeaders(headObjectBuilder, request.requestHeaders());

        HeadObjectPresignRequest presignRequest = HeadObjectPresignRequest.builder()
                .signatureDuration(signatureDuration)
                .headObjectRequest(headObjectBuilder.build())
                .build();

        PresignedHeadObjectRequest presignedRequest = s3Presigner.presignHeadObject(presignRequest);
        return URI.create(presignedRequest.url().toString());
    }

    /**
     * 生成 DELETE 对象预签名 URL
     *
     * @param request           预签名请求
     * @param bucketName        存储桶名称
     * @param objectKey         对象 Key
     * @param signatureDuration 签名有效期
     * @return 预签名 URL
     */
    private URI presignDeleteObject(S3PresignedUrlRequest request,
                                    String bucketName,
                                    String objectKey,
                                    Duration signatureDuration) {
        DeleteObjectRequest.Builder deleteObjectBuilder = DeleteObjectRequest.builder()
                .bucket(bucketName)
                .key(objectKey);

        configureRequestHeaders(deleteObjectBuilder, request.requestHeaders());

        DeleteObjectPresignRequest presignRequest = DeleteObjectPresignRequest.builder()
                .signatureDuration(signatureDuration)
                .deleteObjectRequest(deleteObjectBuilder.build())
                .build();

        PresignedDeleteObjectRequest presignedRequest = s3Presigner.presignDeleteObject(presignRequest);
        return URI.create(presignedRequest.url().toString());
    }

    /**
     * 规范化预签名 URL 有效期
     *
     * @param expire 有效期
     * @return 规范化后的有效期
     */
    private Duration normalizePresignExpire(Duration expire) {
        Duration defaultExpire = s3Properties.getPresign() == null
                ? Duration.ofMinutes(10)
                : s3Properties.getPresign().getDefaultExpire();

        Duration maxExpire = s3Properties.getPresign() == null
                ? AWS_S3_MAX_PRESIGN_EXPIRE
                : s3Properties.getPresign().getMaxExpire();

        Duration resolvedExpire = expire == null ? defaultExpire : expire;
        Duration resolvedMaxExpire = maxExpire == null ? AWS_S3_MAX_PRESIGN_EXPIRE : maxExpire;

        if (resolvedMaxExpire.compareTo(AWS_S3_MAX_PRESIGN_EXPIRE) > 0) {
            resolvedMaxExpire = AWS_S3_MAX_PRESIGN_EXPIRE;
        }

        if (resolvedExpire.compareTo(DEFAULT_MIN_PRESIGN_EXPIRE) < 0) {
            return DEFAULT_MIN_PRESIGN_EXPIRE;
        }

        if (resolvedExpire.compareTo(resolvedMaxExpire) > 0) {
            log.warn("S3 预签名 URL 有效期超过最大限制，expire={}，maxExpire={}，已自动裁剪",
                    resolvedExpire, resolvedMaxExpire);
            return resolvedMaxExpire;
        }

        return resolvedExpire;
    }

    /**
     * 配置 GET 对象响应头
     *
     * @param builder GetObject 请求构建器
     * @param request 预签名 URL 请求
     */
    private void configureGetObjectResponseHeaders(GetObjectRequest.Builder builder, S3PresignedUrlRequest request) {
        if (StrUtil.isNotBlank(request.contentType())) {
            builder.responseContentType(StrUtil.trim(request.contentType()));
        }

        if (StrUtil.isNotBlank(request.filename())) {
            builder.responseContentDisposition(buildContentDisposition(request.filename()));
        }

        Map<String, String> responseHeaders = request.responseHeaders();
        if (MapUtil.isEmpty(responseHeaders)) {
            return;
        }

        responseHeaders.forEach((name, value) -> {
            if (StrUtil.isBlank(name) || value == null) {
                return;
            }

            String normalizedName = StrUtil.trim(name).toLowerCase(Locale.ROOT);
            String normalizedValue = StrUtil.trim(value);

            switch (normalizedName) {
                case "response-content-type", "content-type" -> builder.responseContentType(normalizedValue);
                case "response-content-disposition", "content-disposition" ->
                        builder.responseContentDisposition(normalizedValue);
                case "response-cache-control", "cache-control" -> builder.responseCacheControl(normalizedValue);
                case "response-content-encoding", "content-encoding" ->
                        builder.responseContentEncoding(normalizedValue);
                case "response-content-language", "content-language" ->
                        builder.responseContentLanguage(normalizedValue);
                default -> log.warn("忽略不支持的 S3 GET 预签名响应头，name={}", name);
            }
        });
    }

    /**
     * 配置请求头
     *
     * @param builder        GetObject 请求构建器
     * @param requestHeaders 请求头
     */
    private void configureRequestHeaders(GetObjectRequest.Builder builder, Map<String, String> requestHeaders) {
        AwsRequestOverrideConfiguration overrideConfiguration = buildRequestOverrideConfiguration(requestHeaders);
        if (overrideConfiguration != null) {
            builder.overrideConfiguration(overrideConfiguration);
        }
    }

    /**
     * 配置请求头
     *
     * @param builder        PutObject 请求构建器
     * @param requestHeaders 请求头
     */
    private void configureRequestHeaders(PutObjectRequest.Builder builder, Map<String, String> requestHeaders) {
        AwsRequestOverrideConfiguration overrideConfiguration = buildRequestOverrideConfiguration(requestHeaders);
        if (overrideConfiguration != null) {
            builder.overrideConfiguration(overrideConfiguration);
        }
    }

    /**
     * 配置请求头
     *
     * @param builder        HeadObject 请求构建器
     * @param requestHeaders 请求头
     */
    private void configureRequestHeaders(HeadObjectRequest.Builder builder, Map<String, String> requestHeaders) {
        AwsRequestOverrideConfiguration overrideConfiguration = buildRequestOverrideConfiguration(requestHeaders);
        if (overrideConfiguration != null) {
            builder.overrideConfiguration(overrideConfiguration);
        }
    }

    /**
     * 配置请求头
     *
     * @param builder        DeleteObject 请求构建器
     * @param requestHeaders 请求头
     */
    private void configureRequestHeaders(DeleteObjectRequest.Builder builder, Map<String, String> requestHeaders) {
        AwsRequestOverrideConfiguration overrideConfiguration = buildRequestOverrideConfiguration(requestHeaders);
        if (overrideConfiguration != null) {
            builder.overrideConfiguration(overrideConfiguration);
        }
    }

    /**
     * 构建请求覆盖配置
     *
     * @param requestHeaders 请求头
     * @return 请求覆盖配置
     */
    private AwsRequestOverrideConfiguration buildRequestOverrideConfiguration(Map<String, String> requestHeaders) {
        if (MapUtil.isEmpty(requestHeaders)) {
            return null;
        }

        AwsRequestOverrideConfiguration.Builder builder = AwsRequestOverrideConfiguration.builder();

        requestHeaders.forEach((name, value) -> {
            if (StrUtil.isBlank(name) || value == null) {
                return;
            }

            builder.putHeader(StrUtil.trim(name), StrUtil.trim(value));
        });

        return builder.build();
    }

    /**
     * 构建 Content-Disposition
     *
     * @param filename 文件名
     * @return Content-Disposition
     */
    private String buildContentDisposition(String filename) {
        String normalizedFilename = normalizeOriginalFilename(filename);
        String escapedFilename = StrUtil.replace(normalizedFilename, "\"", "\\\"");
        String encodedFilename = encodePathSegment(normalizedFilename);

        return "attachment; filename=\"" + escapedFilename + "\"; filename*=UTF-8''" + encodedFilename;
    }

    /**
     * 构建替换元数据的 CopyObject 请求
     *
     * @param bucketName 存储桶名称
     * @param objectKey  对象 Key
     * @param metadata   新元数据
     * @param request    元数据替换请求
     * @return CopyObject 请求
     */
    private CopyObjectRequest buildReplaceMetadataCopyRequest(String bucketName,
                                                              String objectKey,
                                                              Map<String, String> metadata,
                                                              S3MetadataRequest request) {
        CopyObjectRequest.Builder builder = CopyObjectRequest.builder()
                .copySource(buildCopySource(bucketName, objectKey, null))
                .bucket(bucketName)
                .key(objectKey)
                .metadataDirective(MetadataDirective.REPLACE)
                .metadata(metadata);

        if (BooleanUtil.isTrue(request.preserveTags())) {
            builder.taggingDirective(TaggingDirective.COPY);
        } else {
            builder.taggingDirective(TaggingDirective.REPLACE)
                    .tagging(StrUtil.EMPTY);
        }

        if (request.storageClass() != null) {
            builder.storageClass(toAwsStorageClass(request.storageClass()));
        }

        return builder.build();
    }

    /**
     * 获取对象 ACL
     *
     * @param bucketName 存储桶名称
     * @param objectKey  对象 Key
     * @return 对象 ACL 响应
     */
    private GetObjectAclResponse getObjectAcl(String bucketName, String objectKey) {
        try {
            GetObjectAclRequest request = GetObjectAclRequest.builder()
                    .bucket(bucketName)
                    .key(objectKey)
                    .build();

            return s3Client.getObjectAcl(request);
        } catch (S3Exception e) {
            log.error("获取 S3 对象 ACL 失败，bucketName={}，objectKey={}，statusCode={}，errorCode={}，message={}",
                    bucketName, objectKey, e.statusCode(), getErrorCode(e), e.getMessage(), e);
            throw e;
        }
    }

    /**
     * 写回对象 ACL
     *
     * @param bucketName  存储桶名称
     * @param objectKey   对象 Key
     * @param aclResponse 原对象 ACL 响应
     */
    private void putObjectAcl(String bucketName, String objectKey, GetObjectAclResponse aclResponse) {
        try {
            AccessControlPolicy accessControlPolicy = AccessControlPolicy.builder()
                    .owner(aclResponse.owner())
                    .grants(aclResponse.grants())
                    .build();

            PutObjectAclRequest request = PutObjectAclRequest.builder()
                    .bucket(bucketName)
                    .key(objectKey)
                    .accessControlPolicy(accessControlPolicy)
                    .build();

            s3Client.putObjectAcl(request);

            log.info("恢复 S3 对象 ACL 成功，bucketName={}，objectKey={}", bucketName, objectKey);
        } catch (S3Exception e) {
            log.error("恢复 S3 对象 ACL 失败，bucketName={}，objectKey={}，statusCode={}，errorCode={}，message={}",
                    bucketName, objectKey, e.statusCode(), getErrorCode(e), e.getMessage(), e);
            throw e;
        }
    }

    /**
     * 规范化对象标签
     *
     * @param tags 标签 Map
     * @return 规范化后的标签 Map
     */
    private Map<String, String> normalizeTags(Map<String, String> tags) {
        if (MapUtil.isEmpty(tags)) {
            return Map.of();
        }

        Map<String, String> normalizedTags = new LinkedHashMap<>();

        tags.forEach((key, value) -> {
            if (StrUtil.isBlank(key) || value == null) {
                return;
            }

            normalizedTags.put(StrUtil.trim(key), StrUtil.trim(value));
        });

        Assert.isTrue(normalizedTags.size() <= 10, "S3 对象标签数量不能超过 10 个");

        return normalizedTags;
    }

    /**
     * 转换为 AWS SDK 标签列表
     *
     * @param tags 标签 Map
     * @return AWS SDK 标签列表
     */
    private List<Tag> toTagList(Map<String, String> tags) {
        if (MapUtil.isEmpty(tags)) {
            return List.of();
        }

        return tags.entrySet()
                .stream()
                .map(entry -> Tag.builder()
                        .key(entry.getKey())
                        .value(entry.getValue())
                        .build())
                .toList();
    }

    /**
     * 转换为标签 Map
     *
     * @param tags AWS SDK 标签列表
     * @return 标签 Map
     */
    private Map<String, String> toTagMap(List<Tag> tags) {
        if (CollUtil.isEmpty(tags)) {
            return Map.of();
        }

        Map<String, String> tagMap = new LinkedHashMap<>();

        for (Tag tag : tags) {
            if (tag == null || StrUtil.isBlank(tag.key())) {
                continue;
            }

            tagMap.put(tag.key(), StrUtil.blankToDefault(tag.value(), StrUtil.EMPTY));
        }

        return Map.copyOf(tagMap);
    }

    /**
     * 构建修改对象存储类型的 CopyObject 请求
     *
     * @param bucketName   存储桶名称
     * @param objectKey    对象 Key
     * @param storageClass 存储类型
     * @return CopyObject 请求
     */
    private CopyObjectRequest buildChangeStorageClassRequest(String bucketName,
                                                             String objectKey,
                                                             S3StorageClass storageClass) {
        return CopyObjectRequest.builder()
                .copySource(buildCopySource(bucketName, objectKey, null))
                .bucket(bucketName)
                .key(objectKey)
                .metadataDirective(MetadataDirective.COPY)
                .taggingDirective(TaggingDirective.COPY)
                .storageClass(toAwsStorageClass(storageClass))
                .build();
    }

    /**
     * 规范化归档恢复天数
     *
     * @param days 恢复副本保留天数
     * @return 规范化后的恢复副本保留天数
     */
    private Integer normalizeRestoreDays(Integer days) {
        Integer resolvedDays = days == null ? 1 : days;
        Assert.isTrue(resolvedDays >= 1, "S3 归档对象恢复天数不能小于 1");
        return resolvedDays;
    }

    /**
     * 构建初始化分片上传请求
     *
     * @param request    初始化请求
     * @param bucketName 存储桶名称
     * @param objectKey  对象 Key
     * @return 初始化分片上传请求
     */
    private CreateMultipartUploadRequest buildCreateMultipartUploadRequest(S3MultipartUploadInitRequest request,
                                                                           String bucketName,
                                                                           String objectKey) {
        CreateMultipartUploadRequest.Builder builder = CreateMultipartUploadRequest.builder()
                .bucket(bucketName)
                .key(objectKey)
                .contentType(resolveContentType(request.contentType(), objectKey));

        Map<String, String> metadata = normalizeMetadata(request.metadata());
        if (MapUtil.isNotEmpty(metadata)) {
            builder.metadata(metadata);
        }

        String tagging = buildTagging(request.tags());
        if (StrUtil.isNotBlank(tagging)) {
            builder.tagging(tagging);
        }

        if (request.acl() != null) {
            builder.acl(toAwsObjectCannedAcl(request.acl()));
        }

        if (request.storageClass() != null) {
            builder.storageClass(toAwsStorageClass(request.storageClass()));
        }

        configureMultipartServerSideEncryption(builder, request.serverSideEncryption(), request.kmsKeyId());

        return builder.build();
    }

    /**
     * 配置分片上传服务端加密
     *
     * @param builder              初始化分片上传请求构建器
     * @param serverSideEncryption 服务端加密方式
     * @param kmsKeyId             KMS Key ID
     */
    private void configureMultipartServerSideEncryption(CreateMultipartUploadRequest.Builder builder,
                                                        S3ServerSideEncryption serverSideEncryption,
                                                        String kmsKeyId) {
        if (serverSideEncryption == null && StrUtil.isBlank(kmsKeyId)) {
            return;
        }

        S3ServerSideEncryption resolvedEncryption = serverSideEncryption;
        if (resolvedEncryption == null) {
            resolvedEncryption = S3ServerSideEncryption.AWS_KMS;
        }

        builder.serverSideEncryption(toAwsServerSideEncryption(resolvedEncryption));

        if (resolvedEncryption == S3ServerSideEncryption.AWS_KMS) {
            Assert.notBlank(kmsKeyId, "S3 使用 AWS_KMS 服务端加密时 kmsKeyId 不能为空");
            builder.ssekmsKeyId(kmsKeyId);
        }
    }

    /**
     * 规范化上传 ID
     *
     * @param uploadId 上传 ID
     * @return 规范化后的上传 ID
     */
    private String normalizeUploadId(String uploadId) {
        String resolvedUploadId = StrUtil.trim(uploadId);
        Assert.notBlank(resolvedUploadId, "S3 分片上传 uploadId 不能为空");
        return resolvedUploadId;
    }

    /**
     * 规范化分片编号
     *
     * @param partNumber 分片编号
     * @return 规范化后的分片编号
     */
    private Integer normalizePartNumber(Integer partNumber) {
        Assert.notNull(partNumber, "S3 分片编号不能为空");
        Assert.isTrue(partNumber >= 1, "S3 分片编号不能小于 1");
        Assert.isTrue(partNumber <= 10000, "S3 分片编号不能大于 10000");
        return partNumber;
    }

    /**
     * 规范化完成分片上传的分片列表
     *
     * @param parts 分片上传结果列表
     * @return 规范化后的分片上传结果列表
     */
    private List<S3MultipartUploadPartResult> normalizeMultipartUploadParts(List<S3MultipartUploadPartResult> parts) {
        Assert.notEmpty(parts, "S3 完成分片上传时分片列表不能为空");

        return parts.stream()
                .peek(part -> {
                    Assert.notNull(part, "S3 分片信息不能为空");
                    normalizePartNumber(part.partNumber());
                    Assert.notBlank(part.eTag(), "S3 分片 eTag 不能为空，partNumber={}", part.partNumber());
                })
                .sorted(Comparator.comparing(S3MultipartUploadPartResult::partNumber))
                .toList();
    }

    /**
     * 转换为 AWS SDK 完成分片列表
     *
     * @param parts 分片上传结果列表
     * @return AWS SDK 完成分片列表
     */
    private List<CompletedPart> toCompletedParts(List<S3MultipartUploadPartResult> parts) {
        return parts.stream()
                .map(part -> CompletedPart.builder()
                        .partNumber(part.partNumber())
                        .eTag(part.eTag())
                        .build())
                .toList();
    }

    /**
     * 统计分片总大小
     *
     * @param parts 分片上传结果列表
     * @return 分片总大小
     */
    private Long sumMultipartUploadPartSize(List<S3MultipartUploadPartResult> parts) {
        if (CollUtil.isEmpty(parts)) {
            return 0L;
        }

        return parts.stream()
                .map(S3MultipartUploadPartResult::size)
                .filter(size -> size != null && size > 0)
                .reduce(0L, Long::sum);
    }

    /**
     * 完成分片上传后尝试读取对象信息
     *
     * @param bucketName 存储桶名称
     * @param objectKey  对象 Key
     * @return 对象信息，读取失败时返回 null
     */
    private S3ObjectInfo tryGetObjectInfoAfterComplete(String bucketName, String objectKey) {
        try {
            return getObjectInfo(bucketName, objectKey);
        } catch (RuntimeException e) {
            log.warn("完成 S3 分片上传后读取对象信息失败，将使用分片结果构建上传结果，bucketName={}，objectKey={}",
                    bucketName, objectKey, e);
            return null;
        }
    }

    /**
     * 构建分片上传任务信息
     *
     * @param bucketName 存储桶名称
     * @param upload     分片上传任务
     * @return 分片上传任务信息
     */
    private S3MultipartUploadInfo buildS3MultipartUploadInfo(String bucketName, MultipartUpload upload) {
        return new S3MultipartUploadInfo(
                bucketName,
                upload.key(),
                upload.uploadId(),
                upload.initiated()
        );
    }

    /**
     * 构建已上传分片信息
     *
     * @param part 已上传分片
     * @return 已上传分片信息
     */
    private S3MultipartUploadPartInfo buildS3MultipartUploadPartInfo(Part part) {
        return new S3MultipartUploadPartInfo(
                part.partNumber(),
                part.eTag(),
                part.size(),
                part.lastModified()
        );
    }
}
```



## 创建控制器示例

### 存储桶管理

```java
package io.github.atengk.aws.s3.controller;

import cn.hutool.core.lang.Dict;
import cn.hutool.core.util.StrUtil;
import io.github.atengk.aws.s3.service.S3Service;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * S3 存储桶管理接口
 *
 * @author Ateng
 * @since 2026-04-28
 */
@Slf4j
@Validated
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/s3/buckets")
public class S3BucketController {

    private final S3Service s3Service;

    /**
     * 查询当前凭证可见的存储桶列表
     * <p>
     * curl 使用示例：
     * curl -X GET "http://localhost:14002/api/s3/buckets"
     *
     * @return 存储桶名称列表
     */
    @GetMapping
    public List<String> listBuckets() {
        List<String> buckets = s3Service.listBuckets();

        log.info("查询 S3 存储桶列表成功，count={}", buckets.size());
        return buckets;
    }

    /**
     * 获取默认存储桶名称
     * <p>
     * curl 使用示例：
     * curl -X GET "http://localhost:14002/api/s3/buckets/default"
     *
     * @return 默认存储桶信息
     */
    @GetMapping("/default")
    public Dict getDefaultBucketName() {
        String bucketName = s3Service.getDefaultBucketName();

        log.info("获取 S3 默认存储桶名称成功，bucketName={}", bucketName);
        return Dict.create()
                .set("bucketName", bucketName);
    }

    /**
     * 解析存储桶名称，为空时返回默认存储桶
     * <p>
     * curl 使用示例：
     * curl -X GET "http://localhost:14002/api/s3/buckets/resolve"
     * <p>
     * curl 使用示例：
     * curl -X GET "http://localhost:14002/api/s3/buckets/resolve?bucketName=data"
     *
     * @param bucketName 存储桶名称，可为空
     * @return 解析后的存储桶名称
     */
    @GetMapping("/resolve")
    public Dict resolveBucketName(@RequestParam(required = false) String bucketName) {
        String resolvedBucketName = s3Service.resolveBucketName(bucketName);

        log.info("解析 S3 存储桶名称成功，inputBucketName={}，resolvedBucketName={}",
                StrUtil.blankToDefault(bucketName, "默认存储桶"),
                resolvedBucketName);

        return Dict.create()
                .set("bucketName", resolvedBucketName);
    }

    /**
     * 判断存储桶是否存在
     * <p>
     * bucketName 不传时判断默认存储桶。
     * <p>
     * curl 使用示例：
     * curl -X GET "http://localhost:14002/api/s3/buckets/exists"
     * <p>
     * curl 使用示例：
     * curl -X GET "http://localhost:14002/api/s3/buckets/exists?bucketName=data"
     *
     * @param bucketName 存储桶名称，可为空
     * @return 是否存在
     */
    @GetMapping("/exists")
    public Dict bucketExists(@RequestParam(required = false) String bucketName) {
        String resolvedBucketName = s3Service.resolveBucketName(bucketName);
        boolean exists = s3Service.bucketExists(resolvedBucketName);

        log.info("检查 S3 存储桶是否存在完成，bucketName={}，exists={}", resolvedBucketName, exists);
        return Dict.create()
                .set("bucketName", resolvedBucketName)
                .set("exists", exists);
    }

    /**
     * 创建存储桶
     * <p>
     * curl 使用示例：
     * curl -X POST "http://localhost:14002/api/s3/buckets/data"
     *
     * @param bucketName 存储桶名称
     * @return 创建结果
     */
    @PostMapping("/{bucketName}")
    public Dict createBucket(@PathVariable String bucketName) {
        String resolvedBucketName = s3Service.resolveBucketName(bucketName);
        s3Service.createBucket(resolvedBucketName);

        log.info("创建 S3 存储桶接口调用成功，bucketName={}", resolvedBucketName);
        return Dict.create()
                .set("bucketName", resolvedBucketName)
                .set("created", true);
    }

    /**
     * 存储桶不存在时创建
     * <p>
     * curl 使用示例：
     * curl -X POST "http://localhost:14002/api/s3/buckets/data/ensure"
     *
     * @param bucketName 存储桶名称
     * @return 处理结果
     */
    @PostMapping("/{bucketName}/ensure")
    public Dict createBucketIfAbsent(@PathVariable String bucketName) {
        String resolvedBucketName = s3Service.resolveBucketName(bucketName);
        s3Service.createBucketIfAbsent(resolvedBucketName);

        log.info("确保 S3 存储桶存在接口调用成功，bucketName={}", resolvedBucketName);
        return Dict.create()
                .set("bucketName", resolvedBucketName)
                .set("ensured", true);
    }

    /**
     * 删除存储桶
     * <p>
     * 说明：S3 删除存储桶前通常要求存储桶为空。
     * <p>
     * curl 使用示例：
     * curl -X DELETE "http://localhost:14002/api/s3/buckets/data"
     *
     * @param bucketName 存储桶名称
     * @return 删除结果
     */
    @DeleteMapping("/{bucketName}")
    public Dict deleteBucket(@PathVariable String bucketName) {
        String resolvedBucketName = s3Service.resolveBucketName(bucketName);
        s3Service.deleteBucket(resolvedBucketName);

        log.info("删除 S3 存储桶接口调用成功，bucketName={}", resolvedBucketName);
        return Dict.create()
                .set("bucketName", resolvedBucketName)
                .set("deleted", true);
    }
}
```

### 工具方法

```java
package io.github.atengk.aws.s3.controller;

import cn.hutool.core.lang.Dict;
import cn.hutool.core.util.StrUtil;
import io.github.atengk.aws.s3.service.S3Service;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * S3 对象工具方法接口
 *
 * @author Ateng
 * @since 2026-04-28
 */
@Slf4j
@Validated
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/s3/objects/tools")
public class S3ObjectToolController {

    private final S3Service s3Service;

    /**
     * 规范化对象 Key
     * <p>
     * 会去除首部 /，替换反斜杠为正斜杠，并压缩连续斜杠。
     * <p>
     * curl 使用示例：
     * curl -X GET "http://localhost:14002/api/s3/objects/tools/normalize-key?objectKey=/upload//2026\\04\\28/test.txt"
     *
     * @param objectKey 原始对象 Key
     * @return 规范化后的对象 Key
     */
    @GetMapping("/normalize-key")
    public Dict normalizeObjectKey(@RequestParam String objectKey) {
        String normalizedObjectKey = s3Service.normalizeObjectKey(objectKey);

        log.info("规范化 S3 对象 Key 成功，objectKey={}，normalizedObjectKey={}",
                objectKey, normalizedObjectKey);

        return Dict.create()
                .set("objectKey", objectKey)
                .set("normalizedObjectKey", normalizedObjectKey);
    }

    /**
     * 构建对象 Key
     * <p>
     * directory 为空时只使用 filename。
     * <p>
     * curl 使用示例：
     * curl -X GET "http://localhost:14002/api/s3/objects/tools/build-key?directory=upload/2026/04/28&filename=test.txt"
     * <p>
     * curl 使用示例：
     * curl -X GET "http://localhost:14002/api/s3/objects/tools/build-key?filename=test.txt"
     *
     * @param directory 目录，可为空
     * @param filename  文件名
     * @return 构建后的对象 Key
     */
    @GetMapping("/build-key")
    public Dict buildObjectKey(@RequestParam(required = false) String directory,
                               @RequestParam String filename) {
        String objectKey = s3Service.buildObjectKey(directory, filename);

        log.info("构建 S3 对象 Key 成功，directory={}，filename={}，objectKey={}",
                StrUtil.blankToDefault(directory, "空目录"),
                filename,
                objectKey);

        return Dict.create()
                .set("directory", directory)
                .set("filename", filename)
                .set("objectKey", objectKey);
    }

    /**
     * 获取对象 Key 中的文件名
     * <p>
     * curl 使用示例：
     * curl -X GET "http://localhost:14002/api/s3/objects/tools/filename?objectKey=upload/2026/04/28/test.txt"
     *
     * @param objectKey 对象 Key
     * @return 文件名
     */
    @GetMapping("/filename")
    public Dict getFilename(@RequestParam String objectKey) {
        String filename = s3Service.getFilename(objectKey);

        log.info("解析 S3 对象文件名成功，objectKey={}，filename={}", objectKey, filename);

        return Dict.create()
                .set("objectKey", objectKey)
                .set("filename", filename);
    }

    /**
     * 获取对象 Key 中的扩展名
     * <p>
     * 返回值不包含点号。
     * <p>
     * curl 使用示例：
     * curl -X GET "http://localhost:14002/api/s3/objects/tools/extension?objectKey=upload/2026/04/28/test.txt"
     * <p>
     * curl 使用示例：
     * curl -X GET "http://localhost:14002/api/s3/objects/tools/extension?objectKey=upload/2026/04/28/README"
     *
     * @param objectKey 对象 Key
     * @return 扩展名
     */
    @GetMapping("/extension")
    public Dict getExtension(@RequestParam String objectKey) {
        String extension = s3Service.getExtension(objectKey);

        log.info("解析 S3 对象扩展名成功，objectKey={}，extension={}",
                objectKey,
                StrUtil.blankToDefault(extension, "无扩展名"));

        return Dict.create()
                .set("objectKey", objectKey)
                .set("extension", extension);
    }

    /**
     * 获取默认存储桶下对象的公开访问 URL
     * <p>
     * 该 URL 不是预签名 URL。
     * 对象是否可访问取决于桶策略、对象 ACL、网关或 CDN 配置。
     * <p>
     * curl 使用示例：
     * curl -X GET "http://localhost:14002/api/s3/objects/tools/public-url?objectKey=upload/2026/04/28/test.txt"
     *
     * @param objectKey 对象 Key
     * @return 公开访问 URL
     */
    @GetMapping("/public-url")
    public Dict getPublicUrl(@RequestParam String objectKey) {
        String bucketName = s3Service.getDefaultBucketName();
        String publicUrl = s3Service.getPublicUrl(objectKey);

        log.info("生成 S3 默认存储桶对象公开访问 URL 成功，bucketName={}，objectKey={}",
                bucketName, objectKey);

        return Dict.create()
                .set("bucketName", bucketName)
                .set("objectKey", objectKey)
                .set("url", publicUrl);
    }

    /**
     * 获取指定存储桶下对象的公开访问 URL
     * <p>
     * 该 URL 不是预签名 URL。
     * 对象是否可访问取决于桶策略、对象 ACL、网关或 CDN 配置。
     * <p>
     * curl 使用示例：
     * curl -X GET "http://localhost:14002/api/s3/objects/tools/public-url/bucket?bucketName=data&objectKey=upload/2026/04/28/test.txt"
     *
     * @param bucketName 存储桶名称
     * @param objectKey  对象 Key
     * @return 公开访问 URL
     */
    @GetMapping("/public-url/bucket")
    public Dict getPublicUrlByBucket(@RequestParam String bucketName,
                                     @RequestParam String objectKey) {
        String resolvedBucketName = s3Service.resolveBucketName(bucketName);
        String publicUrl = s3Service.getPublicUrl(resolvedBucketName, objectKey);

        log.info("生成 S3 指定存储桶对象公开访问 URL 成功，bucketName={}，objectKey={}",
                resolvedBucketName, objectKey);

        return Dict.create()
                .set("bucketName", resolvedBucketName)
                .set("objectKey", objectKey)
                .set("url", publicUrl);
    }
}
```

### 对象基础操作

```java
package io.github.atengk.aws.s3.controller;

import cn.hutool.core.lang.Dict;
import cn.hutool.core.util.StrUtil;
import io.github.atengk.aws.s3.model.info.S3ObjectInfo;
import io.github.atengk.aws.s3.service.S3Service;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * S3 对象基础操作接口
 *
 * @author Ateng
 * @since 2026-04-28
 */
@Slf4j
@Validated
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/s3/objects/basic")
public class S3ObjectBasicController {

    private final S3Service s3Service;

    /**
     * 判断对象是否存在
     * <p>
     * bucketName 不传时使用默认存储桶。
     * <p>
     * curl 使用示例：
     * curl -X GET "http://localhost:14002/api/s3/objects/basic/exists?objectKey=upload/2026/04/28/test.txt"
     * <p>
     * curl 使用示例：
     * curl -X GET "http://localhost:14002/api/s3/objects/basic/exists?bucketName=data&objectKey=upload/2026/04/28/test.txt"
     *
     * @param bucketName 存储桶名称，可为空
     * @param objectKey  对象 Key
     * @return 是否存在
     */
    @GetMapping("/exists")
    public Dict objectExists(@RequestParam(required = false) String bucketName,
                             @RequestParam String objectKey) {
        String resolvedBucketName = s3Service.resolveBucketName(bucketName);
        boolean exists = s3Service.objectExists(resolvedBucketName, objectKey);

        log.info("检查 S3 对象是否存在完成，bucketName={}，objectKey={}，exists={}",
                resolvedBucketName, objectKey, exists);

        return Dict.create()
                .set("bucketName", resolvedBucketName)
                .set("objectKey", objectKey)
                .set("exists", exists);
    }

    /**
     * 获取对象基础信息
     * <p>
     * bucketName 不传时使用默认存储桶。
     * <p>
     * curl 使用示例：
     * curl -X GET "http://localhost:14002/api/s3/objects/basic/info?objectKey=upload/2026/04/28/test.txt"
     * <p>
     * curl 使用示例：
     * curl -X GET "http://localhost:14002/api/s3/objects/basic/info?bucketName=data&objectKey=upload/2026/04/28/test.txt"
     *
     * @param bucketName 存储桶名称，可为空
     * @param objectKey  对象 Key
     * @return 对象基础信息
     */
    @GetMapping("/info")
    public S3ObjectInfo getObjectInfo(@RequestParam(required = false) String bucketName,
                                      @RequestParam String objectKey) {
        String resolvedBucketName = s3Service.resolveBucketName(bucketName);
        S3ObjectInfo objectInfo = s3Service.getObjectInfo(resolvedBucketName, objectKey);

        log.info("获取 S3 对象基础信息成功，bucketName={}，objectKey={}，size={}，contentType={}",
                resolvedBucketName, objectKey, objectInfo.size(), objectInfo.contentType());

        return objectInfo;
    }

    /**
     * 获取对象大小
     * <p>
     * bucketName 不传时使用默认存储桶。
     * <p>
     * curl 使用示例：
     * curl -X GET "http://localhost:14002/api/s3/objects/basic/size?objectKey=upload/2026/04/28/test.txt"
     * <p>
     * curl 使用示例：
     * curl -X GET "http://localhost:14002/api/s3/objects/basic/size?bucketName=data&objectKey=upload/2026/04/28/test.txt"
     *
     * @param bucketName 存储桶名称，可为空
     * @param objectKey  对象 Key
     * @return 对象大小，单位字节
     */
    @GetMapping("/size")
    public Dict getObjectSize(@RequestParam(required = false) String bucketName,
                              @RequestParam String objectKey) {
        String resolvedBucketName = s3Service.resolveBucketName(bucketName);
        Long size = s3Service.getObjectSize(resolvedBucketName, objectKey);

        log.info("获取 S3 对象大小成功，bucketName={}，objectKey={}，size={}",
                resolvedBucketName, objectKey, size);

        return Dict.create()
                .set("bucketName", resolvedBucketName)
                .set("objectKey", objectKey)
                .set("size", size);
    }

    /**
     * 获取对象 Content-Type
     * <p>
     * bucketName 不传时使用默认存储桶。
     * <p>
     * curl 使用示例：
     * curl -X GET "http://localhost:14002/api/s3/objects/basic/content-type?objectKey=upload/2026/04/28/test.txt"
     * <p>
     * curl 使用示例：
     * curl -X GET "http://localhost:14002/api/s3/objects/basic/content-type?bucketName=data&objectKey=upload/2026/04/28/test.txt"
     *
     * @param bucketName 存储桶名称，可为空
     * @param objectKey  对象 Key
     * @return 对象 Content-Type
     */
    @GetMapping("/content-type")
    public Dict getObjectContentType(@RequestParam(required = false) String bucketName,
                                     @RequestParam String objectKey) {
        String resolvedBucketName = s3Service.resolveBucketName(bucketName);
        String contentType = s3Service.getObjectContentType(resolvedBucketName, objectKey);

        log.info("获取 S3 对象 Content-Type 成功，bucketName={}，objectKey={}，contentType={}",
                resolvedBucketName,
                objectKey,
                StrUtil.blankToDefault(contentType, "未设置"));

        return Dict.create()
                .set("bucketName", resolvedBucketName)
                .set("objectKey", objectKey)
                .set("contentType", contentType);
    }
}
```

### 对象上传

```java
package io.github.atengk.aws.s3.controller;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.io.IoUtil;
import cn.hutool.core.lang.Assert;
import cn.hutool.core.util.StrUtil;
import cn.hutool.json.JSONObject;
import cn.hutool.json.JSONUtil;
import io.github.atengk.aws.s3.model.enums.S3ObjectAcl;
import io.github.atengk.aws.s3.model.enums.S3ServerSideEncryption;
import io.github.atengk.aws.s3.model.enums.S3StorageClass;
import io.github.atengk.aws.s3.model.request.S3UploadRequest;
import io.github.atengk.aws.s3.model.result.S3UploadResult;
import io.github.atengk.aws.s3.service.S3Service;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * S3 对象上传接口
 *
 * @author Ateng
 * @since 2026-04-28
 */
@Slf4j
@Validated
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/s3/objects/upload")
public class S3ObjectUploadController {

    private final S3Service s3Service;

    /**
     * 上传 MultipartFile 文件
     * <p>
     * objectKey 不传时，仅支持上传到默认存储桶，并由 Service 自动生成对象 Key。
     * bucketName 不传时使用默认存储桶。
     * <p>
     * curl 使用示例：
     * curl -X POST "http://localhost:14002/api/s3/objects/upload/file" \
     * -F "file=@/data/test.txt"
     * <p>
     * curl 使用示例：
     * curl -X POST "http://localhost:14002/api/s3/objects/upload/file" \
     * -F "bucketName=data" \
     * -F "objectKey=upload/2026/04/28/test.txt" \
     * -F "file=@/data/test.txt"
     *
     * @param bucketName 存储桶名称，可为空
     * @param objectKey  对象 Key，可为空
     * @param file       上传文件
     * @return 上传结果
     */
    @PostMapping(value = "/file", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public S3UploadResult uploadFile(@RequestParam(required = false) String bucketName,
                                     @RequestParam(required = false) String objectKey,
                                     @RequestParam MultipartFile file) {
        Assert.notNull(file, "上传文件不能为空");

        if (StrUtil.isBlank(objectKey)) {
            Assert.isTrue(
                    StrUtil.isBlank(bucketName),
                    "objectKey 为空时不支持指定 bucketName，请传入 objectKey 或使用默认存储桶自动生成对象 Key"
            );

            S3UploadResult result = s3Service.upload(file);

            log.info("上传 S3 文件成功，bucketName={}，objectKey={}，originalFilename={}，size={}",
                    result.bucketName(), result.objectKey(), result.originalFilename(), result.size());
            return result;
        }

        String resolvedBucketName = s3Service.resolveBucketName(bucketName);
        S3UploadResult result = s3Service.upload(resolvedBucketName, objectKey, file);

        log.info("上传 S3 文件成功，bucketName={}，objectKey={}，originalFilename={}，size={}",
                result.bucketName(), result.objectKey(), result.originalFilename(), result.size());

        return result;
    }

    /**
     * 上传二进制请求体
     * <p>
     * bucketName 不传时使用默认存储桶。
     * <p>
     * curl 使用示例：
     * curl -X POST "http://localhost:14002/api/s3/objects/upload/bytes?objectKey=upload/2026/04/28/test.bin" \
     * -H "Content-Type: application/octet-stream" \
     * --data-binary "@/data/test.bin"
     * <p>
     * curl 使用示例：
     * curl -X POST "http://localhost:14002/api/s3/objects/upload/bytes?bucketName=data&objectKey=upload/2026/04/28/test.bin" \
     * -H "Content-Type: application/octet-stream" \
     * --data-binary "@/data/test.bin"
     *
     * @param bucketName 存储桶名称，可为空
     * @param objectKey  对象 Key
     * @param bytes      二进制内容
     * @return 上传结果
     */
    @PostMapping(value = "/bytes", consumes = MediaType.APPLICATION_OCTET_STREAM_VALUE)
    public S3UploadResult uploadBytes(@RequestParam(required = false) String bucketName,
                                      @RequestParam String objectKey,
                                      @RequestBody byte[] bytes) {
        Assert.notNull(bytes, "上传字节数组不能为空");

        String resolvedBucketName = s3Service.resolveBucketName(bucketName);
        S3UploadResult result = s3Service.upload(resolvedBucketName, objectKey, bytes);

        log.info("上传 S3 二进制对象成功，bucketName={}，objectKey={}，size={}",
                result.bucketName(), result.objectKey(), result.size());

        return result;
    }

    /**
     * 上传服务端本地文件
     * <p>
     * 说明：filePath 是服务端机器上的文件路径，不是客户端本机路径。
     * bucketName 不传时使用默认存储桶。
     * <p>
     * curl 使用示例：
     * curl -X POST "http://localhost:14002/api/s3/objects/upload/local-file" \
     * -d "filePath=/data/test.xlsx" \
     * -d "objectKey=excel/test.xlsx"
     * <p>
     * curl 使用示例：
     * curl -X POST "http://localhost:14002/api/s3/objects/upload/local-file" \
     * -d "bucketName=data" \
     * -d "filePath=/data/test.xlsx" \
     * -d "objectKey=excel/test.xlsx"
     *
     * @param bucketName 存储桶名称，可为空
     * @param objectKey  对象 Key
     * @param filePath   服务端本地文件路径
     * @return 上传结果
     */
    @PostMapping(value = "/local-file", consumes = MediaType.APPLICATION_FORM_URLENCODED_VALUE)
    public S3UploadResult uploadLocalFile(@RequestParam(required = false) String bucketName,
                                          @RequestParam String objectKey,
                                          @RequestParam String filePath) {
        String resolvedBucketName = s3Service.resolveBucketName(bucketName);
        S3UploadResult result = s3Service.upload(resolvedBucketName, objectKey, Path.of(filePath));

        log.info("上传 S3 本地文件成功，bucketName={}，objectKey={}，filePath={}，size={}",
                result.bucketName(), result.objectKey(), filePath, result.size());

        return result;
    }

    /**
     * 高级上传 MultipartFile 文件
     * <p>
     * 支持设置 contentType、metadata、tags、ACL、存储类型和服务端加密。
     * metadataJson 和 tagsJson 为 JSON 对象字符串。
     * bucketName 不传时使用默认存储桶。
     * <p>
     * curl 使用示例：
     * curl -X POST "http://localhost:14002/api/s3/objects/upload/advanced" \
     * -F "bucketName=data" \
     * -F "objectKey=secure/report.pdf" \
     * -F "contentType=application/pdf" \
     * -F "metadataJson={\"biz-type\":\"report\",\"owner\":\"ateng\"}" \
     * -F "tagsJson={\"env\":\"prod\",\"type\":\"pdf\"}" \
     * -F "acl=PRIVATE" \
     * -F "storageClass=STANDARD" \
     * -F "serverSideEncryption=AWS_KMS" \
     * -F "kmsKeyId=your-kms-key-id" \
     * -F "file=@/data/report.pdf"
     * <p>
     * curl 使用示例：
     * curl -X POST "http://localhost:14002/api/s3/objects/upload/advanced" \
     * -F "objectKey=image/avatar.png" \
     * -F "tagsJson={\"scene\":\"avatar\"}" \
     * -F "file=@/data/avatar.png"
     *
     * @param bucketName           存储桶名称，可为空
     * @param objectKey            对象 Key
     * @param file                 上传文件
     * @param contentType          Content-Type，可为空
     * @param metadataJson         元数据 JSON，可为空
     * @param tagsJson             标签 JSON，可为空
     * @param acl                  对象 ACL，可为空
     * @param storageClass         存储类型，可为空
     * @param serverSideEncryption 服务端加密方式，可为空
     * @param kmsKeyId             KMS Key ID，可为空
     * @return 上传结果
     */
    @PostMapping(value = "/advanced", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public S3UploadResult uploadAdvanced(@RequestParam(required = false) String bucketName,
                                         @RequestParam String objectKey,
                                         @RequestParam MultipartFile file,
                                         @RequestParam(required = false) String contentType,
                                         @RequestParam(required = false) String metadataJson,
                                         @RequestParam(required = false) String tagsJson,
                                         @RequestParam(required = false) String acl,
                                         @RequestParam(required = false) String storageClass,
                                         @RequestParam(required = false) String serverSideEncryption,
                                         @RequestParam(required = false) String kmsKeyId) {
        Assert.notNull(file, "上传文件不能为空");

        String resolvedBucketName = s3Service.resolveBucketName(bucketName);
        String resolvedContentType = StrUtil.blankToDefault(contentType, file.getContentType());

        try (InputStream inputStream = file.getInputStream()) {
            S3UploadRequest request = new S3UploadRequest(
                    resolvedBucketName,
                    objectKey,
                    inputStream,
                    file.getSize(),
                    resolvedContentType,
                    parseStringMap(metadataJson),
                    parseStringMap(tagsJson),
                    parseEnum(S3ObjectAcl.class, acl, "S3 对象 ACL 不合法"),
                    parseEnum(S3StorageClass.class, storageClass, "S3 存储类型不合法"),
                    parseEnum(S3ServerSideEncryption.class, serverSideEncryption, "S3 服务端加密方式不合法"),
                    blankToNull(kmsKeyId)
            );

            S3UploadResult result = s3Service.upload(request);

            log.info("高级上传 S3 文件成功，bucketName={}，objectKey={}，originalFilename={}，size={}",
                    result.bucketName(), result.objectKey(), result.originalFilename(), result.size());

            return result;
        } catch (IOException e) {
            log.error("读取高级上传文件流失败，bucketName={}，objectKey={}，originalFilename={}",
                    resolvedBucketName, objectKey, file.getOriginalFilename(), e);
            throw new UncheckedIOException("读取高级上传文件流失败", e);
        }
    }

    /**
     * 批量上传 MultipartFile 文件
     * <p>
     * files 和 objectKeys 按顺序一一对应。
     * metadataJson、tagsJson、acl、storageClass、serverSideEncryption、kmsKeyId 会应用到本次批量上传的所有文件。
     * bucketName 不传时使用默认存储桶。
     * <p>
     * curl 使用示例：
     * curl -X POST "http://localhost:14002/api/s3/objects/upload/batch" \
     * -F "bucketName=data" \
     * -F "objectKeys=batch/a.txt" \
     * -F "objectKeys=batch/b.txt" \
     * -F "files=@/data/a.txt" \
     * -F "files=@/data/b.txt"
     * <p>
     * curl 使用示例：
     * curl -X POST "http://localhost:14002/api/s3/objects/upload/batch" \
     * -F "objectKeys=batch/a.txt" \
     * -F "objectKeys=batch/b.txt" \
     * -F "tagsJson={\"batch\":\"true\"}" \
     * -F "acl=PRIVATE" \
     * -F "files=@/data/a.txt" \
     * -F "files=@/data/b.txt"
     *
     * @param bucketName           存储桶名称，可为空
     * @param files                上传文件列表
     * @param objectKeys           对象 Key 列表
     * @param metadataJson         元数据 JSON，可为空
     * @param tagsJson             标签 JSON，可为空
     * @param acl                  对象 ACL，可为空
     * @param storageClass         存储类型，可为空
     * @param serverSideEncryption 服务端加密方式，可为空
     * @param kmsKeyId             KMS Key ID，可为空
     * @return 上传结果列表
     */
    @PostMapping(value = "/batch", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public List<S3UploadResult> uploadBatch(@RequestParam(required = false) String bucketName,
                                            @RequestParam List<MultipartFile> files,
                                            @RequestParam List<String> objectKeys,
                                            @RequestParam(required = false) String metadataJson,
                                            @RequestParam(required = false) String tagsJson,
                                            @RequestParam(required = false) String acl,
                                            @RequestParam(required = false) String storageClass,
                                            @RequestParam(required = false) String serverSideEncryption,
                                            @RequestParam(required = false) String kmsKeyId) {
        Assert.notEmpty(files, "批量上传文件不能为空");
        Assert.notEmpty(objectKeys, "批量上传对象 Key 不能为空");
        Assert.isTrue(files.size() == objectKeys.size(), "批量上传 files 和 objectKeys 数量必须一致");

        String resolvedBucketName = s3Service.resolveBucketName(bucketName);
        Map<String, String> metadata = parseStringMap(metadataJson);
        Map<String, String> tags = parseStringMap(tagsJson);
        S3ObjectAcl resolvedAcl = parseEnum(S3ObjectAcl.class, acl, "S3 对象 ACL 不合法");
        S3StorageClass resolvedStorageClass = parseEnum(S3StorageClass.class, storageClass, "S3 存储类型不合法");
        S3ServerSideEncryption resolvedServerSideEncryption = parseEnum(
                S3ServerSideEncryption.class,
                serverSideEncryption,
                "S3 服务端加密方式不合法"
        );

        List<InputStream> inputStreams = new ArrayList<>(files.size());
        List<S3UploadRequest> requests = new ArrayList<>(files.size());

        try {
            for (int i = 0; i < files.size(); i++) {
                MultipartFile file = files.get(i);
                String objectKey = objectKeys.get(i);

                Assert.notNull(file, "批量上传文件不能为空，index={}", i);
                Assert.notBlank(objectKey, "批量上传对象 Key 不能为空，index={}", i);

                InputStream inputStream = file.getInputStream();
                inputStreams.add(inputStream);

                requests.add(new S3UploadRequest(
                        resolvedBucketName,
                        objectKey,
                        inputStream,
                        file.getSize(),
                        file.getContentType(),
                        metadata,
                        tags,
                        resolvedAcl,
                        resolvedStorageClass,
                        resolvedServerSideEncryption,
                        blankToNull(kmsKeyId)
                ));
            }

            List<S3UploadResult> results = s3Service.uploadBatch(requests);

            log.info("批量上传 S3 文件成功，bucketName={}，count={}", resolvedBucketName, results.size());
            return results;
        } catch (IOException e) {
            log.error("读取批量上传文件流失败，bucketName={}", resolvedBucketName, e);
            throw new UncheckedIOException("读取批量上传文件流失败", e);
        } finally {
            for (InputStream inputStream : inputStreams) {
                IoUtil.close(inputStream);
            }
        }
    }

    /**
     * 解析 JSON 字符串为 Map
     *
     * @param json JSON 字符串
     * @return 字符串 Map
     */
    private Map<String, String> parseStringMap(String json) {
        if (StrUtil.isBlank(json)) {
            return Map.of();
        }

        Assert.isTrue(JSONUtil.isTypeJSONObject(json), "参数必须是 JSON 对象字符串：{}", json);

        JSONObject jsonObject = JSONUtil.parseObj(json);
        if (jsonObject.isEmpty()) {
            return Map.of();
        }

        Map<String, String> map = new LinkedHashMap<>();

        jsonObject.forEach((key, value) -> {
            if (StrUtil.isBlank(key) || value == null) {
                return;
            }

            map.put(StrUtil.trim(key), StrUtil.toString(value));
        });

        return map;
    }

    /**
     * 解析枚举
     *
     * @param enumClass    枚举类型
     * @param value        枚举值
     * @param errorMessage 错误信息
     * @param <E>          枚举泛型
     * @return 枚举值
     */
    private <E extends Enum<E>> E parseEnum(Class<E> enumClass, String value, String errorMessage) {
        if (StrUtil.isBlank(value)) {
            return null;
        }

        try {
            return Enum.valueOf(enumClass, StrUtil.trim(value).toUpperCase());
        } catch (IllegalArgumentException e) {
            String enumValues = CollUtil.join(List.of(enumClass.getEnumConstants()), ",");
            throw new IllegalArgumentException(errorMessage + "，value=" + value + "，可选值：" + enumValues, e);
        }
    }

    /**
     * 空白字符串转 null，并去除前后空格
     *
     * @param value 字符串
     * @return 非空白字符串或 null
     */
    private String blankToNull(String value) {
        return StrUtil.isBlank(value) ? null : StrUtil.trim(value);
    }
}
```

### 对象下载

```java
package io.github.atengk.aws.s3.controller;

import cn.hutool.core.io.FileUtil;
import cn.hutool.core.lang.Dict;
import cn.hutool.core.util.StrUtil;
import io.github.atengk.aws.s3.model.request.S3DownloadRequest;
import io.github.atengk.aws.s3.model.result.S3DownloadResult;
import io.github.atengk.aws.s3.service.S3Service;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.InputStreamResource;
import org.springframework.core.io.Resource;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;

/**
 * S3 对象下载接口
 *
 * @author Ateng
 * @since 2026-04-28
 */
@Slf4j
@Validated
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/s3/objects/download")
public class S3ObjectDownloadController {

    private final S3Service s3Service;

    /**
     * 下载对象为字节数组
     * <p>
     * bucketName 不传时使用默认存储桶。
     * <p>
     * curl 使用示例：
     * curl -X GET "http://localhost:14002/api/s3/objects/download/bytes?objectKey=upload/2026/04/28/test.txt" \
     * -o test.txt
     * <p>
     * curl 使用示例：
     * curl -X GET "http://localhost:14002/api/s3/objects/download/bytes?bucketName=data&objectKey=upload/2026/04/28/test.txt" \
     * -o test.txt
     *
     * @param bucketName 存储桶名称，可为空
     * @param objectKey  对象 Key
     * @return 字节数组响应
     */
    @GetMapping("/bytes")
    public ResponseEntity<byte[]> downloadAsBytes(@RequestParam(required = false) String bucketName,
                                                  @RequestParam String objectKey) {
        String resolvedBucketName = s3Service.resolveBucketName(bucketName);
        byte[] bytes = s3Service.downloadAsBytes(resolvedBucketName, objectKey);
        String filename = s3Service.getFilename(objectKey);
        String contentType = s3Service.getObjectContentType(resolvedBucketName, objectKey);

        log.info("下载 S3 对象为字节数组成功，bucketName={}，objectKey={}，size={}",
                resolvedBucketName, objectKey, bytes.length);

        return ResponseEntity.ok()
                .headers(buildDownloadHeaders(filename, contentType, (long) bytes.length))
                .body(bytes);
    }

    /**
     * 流式下载对象
     * <p>
     * bucketName 不传时使用默认存储桶。
     * <p>
     * curl 使用示例：
     * curl -X GET "http://localhost:14002/api/s3/objects/download/stream?objectKey=video/demo.mp4" \
     * -o demo.mp4
     * <p>
     * curl 使用示例：
     * curl -X GET "http://localhost:14002/api/s3/objects/download/stream?bucketName=data&objectKey=video/demo.mp4" \
     * -o demo.mp4
     *
     * @param bucketName 存储桶名称，可为空
     * @param objectKey  对象 Key
     * @return 流式下载响应
     */
    @GetMapping("/stream")
    public ResponseEntity<Resource> downloadAsStream(@RequestParam(required = false) String bucketName,
                                                     @RequestParam String objectKey) {
        String resolvedBucketName = s3Service.resolveBucketName(bucketName);
        S3DownloadResult result = s3Service.download(new S3DownloadRequest(
                resolvedBucketName,
                objectKey,
                null,
                null,
                null
        ));

        Resource resource = buildInputStreamResource(result);

        log.info("流式下载 S3 对象成功，bucketName={}，objectKey={}，contentLength={}",
                result.bucketName(), result.objectKey(), result.contentLength());

        return ResponseEntity.ok()
                .headers(buildDownloadHeaders(result.filename(), result.contentType(), result.contentLength()))
                .body(resource);
    }

    /**
     * 下载对象为 Spring Resource
     * <p>
     * bucketName 不传时使用默认存储桶。
     * <p>
     * curl 使用示例：
     * curl -X GET "http://localhost:14002/api/s3/objects/download/resource?objectKey=image/avatar.png" \
     * -o avatar.png
     * <p>
     * curl 使用示例：
     * curl -X GET "http://localhost:14002/api/s3/objects/download/resource?bucketName=data&objectKey=image/avatar.png" \
     * -o avatar.png
     *
     * @param bucketName 存储桶名称，可为空
     * @param objectKey  对象 Key
     * @return Resource 下载响应
     */
    @GetMapping("/resource")
    public ResponseEntity<Resource> downloadAsResource(@RequestParam(required = false) String bucketName,
                                                       @RequestParam String objectKey) {
        String resolvedBucketName = s3Service.resolveBucketName(bucketName);
        Resource resource = s3Service.downloadAsResource(resolvedBucketName, objectKey);
        String filename = StrUtil.blankToDefault(resource.getFilename(), s3Service.getFilename(objectKey));
        String contentType = s3Service.getObjectContentType(resolvedBucketName, objectKey);
        Long contentLength = s3Service.getObjectSize(resolvedBucketName, objectKey);

        log.info("下载 S3 对象为 Resource 成功，bucketName={}，objectKey={}，contentLength={}",
                resolvedBucketName, objectKey, contentLength);

        return ResponseEntity.ok()
                .headers(buildDownloadHeaders(filename, contentType, contentLength))
                .body(resource);
    }

    /**
     * 下载对象到服务端本地文件
     * <p>
     * 说明：targetPath 是服务端机器上的目标路径，不是客户端本机路径。
     * bucketName 不传时使用默认存储桶。
     * <p>
     * curl 使用示例：
     * curl -X GET "http://localhost:14002/api/s3/objects/download/local-file?objectKey=excel/report.xlsx&targetPath=/data/download/report.xlsx"
     * <p>
     * curl 使用示例：
     * curl -X GET "http://localhost:14002/api/s3/objects/download/local-file?bucketName=data&objectKey=excel/report.xlsx&targetPath=/data/download/report.xlsx"
     *
     * @param bucketName 存储桶名称，可为空
     * @param objectKey  对象 Key
     * @param targetPath 服务端本地目标路径
     * @return 下载结果
     */
    @GetMapping("/local-file")
    public Dict downloadToLocalFile(@RequestParam(required = false) String bucketName,
                                    @RequestParam String objectKey,
                                    @RequestParam String targetPath) {
        String resolvedBucketName = s3Service.resolveBucketName(bucketName);
        Path resolvedTargetPath = Path.of(targetPath);

        s3Service.downloadToFile(resolvedBucketName, objectKey, resolvedTargetPath);

        log.info("下载 S3 对象到服务端本地文件成功，bucketName={}，objectKey={}，targetPath={}",
                resolvedBucketName, objectKey, resolvedTargetPath);

        return Dict.create()
                .set("bucketName", resolvedBucketName)
                .set("objectKey", objectKey)
                .set("targetPath", resolvedTargetPath.toString())
                .set("filename", FileUtil.getName(resolvedTargetPath.toString()))
                .set("downloaded", true);
    }

    /**
     * 高级下载对象
     * <p>
     * 支持 versionId 和 Range 下载。
     * bucketName 不传时使用默认存储桶。
     * filename 不传时使用 objectKey 中的文件名。
     * <p>
     * curl 使用示例：
     * curl -X GET "http://localhost:14002/api/s3/objects/download/advanced?objectKey=video/demo.mp4&rangeStart=0&rangeEnd=1048575" \
     * -o demo.part
     * <p>
     * curl 使用示例：
     * curl -X GET "http://localhost:14002/api/s3/objects/download/advanced?bucketName=data&objectKey=archive/report.pdf&versionId=your-version-id&filename=report.pdf" \
     * -o report.pdf
     *
     * @param bucketName 存储桶名称，可为空
     * @param objectKey  对象 Key
     * @param versionId  对象版本 ID，可为空
     * @param rangeStart Range 起始字节，可为空
     * @param rangeEnd   Range 结束字节，可为空
     * @param filename   下载文件名，可为空
     * @return 高级下载响应
     */
    @GetMapping("/advanced")
    public ResponseEntity<Resource> downloadAdvanced(@RequestParam(required = false) String bucketName,
                                                     @RequestParam String objectKey,
                                                     @RequestParam(required = false) String versionId,
                                                     @RequestParam(required = false) Long rangeStart,
                                                     @RequestParam(required = false) Long rangeEnd,
                                                     @RequestParam(required = false) String filename) {
        String resolvedBucketName = s3Service.resolveBucketName(bucketName);

        S3DownloadRequest request = new S3DownloadRequest(
                resolvedBucketName,
                objectKey,
                blankToNull(versionId),
                rangeStart,
                rangeEnd
        );

        S3DownloadResult result = s3Service.download(request);
        Resource resource = buildInputStreamResource(result);
        String resolvedFilename = StrUtil.blankToDefault(filename, result.filename());

        HttpHeaders headers = buildDownloadHeaders(resolvedFilename, result.contentType(), result.contentLength());
        if (rangeStart != null || rangeEnd != null) {
            headers.set(HttpHeaders.ACCEPT_RANGES, "bytes");
        }

        log.info("高级下载 S3 对象成功，bucketName={}，objectKey={}，versionId={}，rangeStart={}，rangeEnd={}，contentLength={}",
                result.bucketName(),
                result.objectKey(),
                StrUtil.blankToDefault(versionId, "默认版本"),
                rangeStart,
                rangeEnd,
                result.contentLength());

        return ResponseEntity.ok()
                .headers(headers)
                .body(resource);
    }

    /**
     * 构建输入流资源
     *
     * @param result 下载结果
     * @return 输入流资源
     */
    private Resource buildInputStreamResource(S3DownloadResult result) {
        InputStream inputStream = result.inputStream();

        return new InputStreamResource(inputStream) {

            /**
             * 获取资源文件名
             *
             * @return 文件名
             */
            @Override
            public String getFilename() {
                return result.filename();
            }

            /**
             * 获取资源内容长度
             *
             * @return 内容长度
             */
            @Override
            public long contentLength() {
                return result.contentLength() == null ? -1L : result.contentLength();
            }
        };
    }

    /**
     * 构建下载响应头
     *
     * @param filename      文件名
     * @param contentType   Content-Type
     * @param contentLength 内容长度
     * @return 响应头
     */
    private HttpHeaders buildDownloadHeaders(String filename, String contentType, Long contentLength) {
        String resolvedFilename = StrUtil.blankToDefault(filename, "download");
        MediaType mediaType = parseMediaType(contentType);

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(mediaType);
        headers.setContentDisposition(ContentDisposition.attachment()
                .filename(resolvedFilename, StandardCharsets.UTF_8)
                .build());

        if (contentLength != null && contentLength >= 0) {
            headers.setContentLength(contentLength);
        }

        return headers;
    }

    /**
     * 解析 MediaType
     *
     * @param contentType Content-Type
     * @return MediaType
     */
    private MediaType parseMediaType(String contentType) {
        if (StrUtil.isBlank(contentType)) {
            return MediaType.APPLICATION_OCTET_STREAM;
        }

        try {
            return MediaType.parseMediaType(contentType);
        } catch (IllegalArgumentException e) {
            log.warn("S3 对象 Content-Type 不合法，使用默认下载类型，contentType={}", contentType);
            return MediaType.APPLICATION_OCTET_STREAM;
        }
    }

    /**
     * 空白字符串转 null，并去除前后空格
     *
     * @param value 字符串
     * @return 非空白字符串或 null
     */
    private String blankToNull(String value) {
        return StrUtil.isBlank(value) ? null : StrUtil.trim(value);
    }
}
```

### 对象删除

```java
package io.github.atengk.aws.s3.controller;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.lang.Assert;
import cn.hutool.core.lang.Dict;
import cn.hutool.core.util.BooleanUtil;
import cn.hutool.core.util.StrUtil;
import io.github.atengk.aws.s3.model.result.S3DeleteResult;
import io.github.atengk.aws.s3.service.S3Service;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * S3 对象删除接口
 *
 * @author Ateng
 * @since 2026-04-28
 */
@Slf4j
@Validated
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/s3/objects/delete")
public class S3ObjectDeleteController {

    private final S3Service s3Service;

    /**
     * 删除单个对象
     * <p>
     * bucketName 不传时使用默认存储桶。
     * <p>
     * curl 使用示例：
     * curl -X DELETE "http://localhost:14002/api/s3/objects/delete?objectKey=upload/2026/04/28/test.txt"
     * <p>
     * curl 使用示例：
     * curl -X DELETE "http://localhost:14002/api/s3/objects/delete?bucketName=data&objectKey=upload/2026/04/28/test.txt"
     *
     * @param bucketName 存储桶名称，可为空
     * @param objectKey  对象 Key
     * @return 删除结果
     */
    @DeleteMapping
    public Dict delete(@RequestParam(required = false) String bucketName,
                       @RequestParam String objectKey) {
        String resolvedBucketName = s3Service.resolveBucketName(bucketName);
        s3Service.delete(resolvedBucketName, objectKey);

        log.info("删除 S3 单个对象成功，bucketName={}，objectKey={}", resolvedBucketName, objectKey);

        return Dict.create()
                .set("bucketName", resolvedBucketName)
                .set("objectKey", objectKey)
                .set("deleted", true);
    }

    /**
     * 批量删除对象
     * <p>
     * bucketName 不传时使用默认存储桶。
     * objectKeys 使用重复参数传递。
     * <p>
     * curl 使用示例：
     * curl -X DELETE "http://localhost:14002/api/s3/objects/delete/batch?objectKeys=batch/a.txt&objectKeys=batch/b.txt"
     * <p>
     * curl 使用示例：
     * curl -X DELETE "http://localhost:14002/api/s3/objects/delete/batch?bucketName=data&objectKeys=batch/a.txt&objectKeys=batch/b.txt"
     *
     * @param bucketName 存储桶名称，可为空
     * @param objectKeys 对象 Key 列表
     * @return 删除结果
     */
    @DeleteMapping("/batch")
    public Dict deleteBatch(@RequestParam(required = false) String bucketName,
                            @RequestParam List<String> objectKeys) {
        Assert.notEmpty(objectKeys, "批量删除对象 Key 不能为空");

        String resolvedBucketName = s3Service.resolveBucketName(bucketName);
        s3Service.deleteBatch(resolvedBucketName, objectKeys);

        log.info("批量删除 S3 对象成功，bucketName={}，count={}", resolvedBucketName, objectKeys.size());

        return Dict.create()
                .set("bucketName", resolvedBucketName)
                .set("objectKeys", objectKeys)
                .set("count", objectKeys.size())
                .set("deleted", true);
    }

    /**
     * 批量删除对象，并返回详细删除结果
     * <p>
     * bucketName 不传时使用默认存储桶。
     * objectKeys 使用重复参数传递。
     * <p>
     * curl 使用示例：
     * curl -X DELETE "http://localhost:14002/api/s3/objects/delete/batch/detailed?objectKeys=batch/a.txt&objectKeys=batch/b.txt"
     * <p>
     * curl 使用示例：
     * curl -X DELETE "http://localhost:14002/api/s3/objects/delete/batch/detailed?bucketName=data&objectKeys=batch/a.txt&objectKeys=batch/b.txt"
     *
     * @param bucketName 存储桶名称，可为空
     * @param objectKeys 对象 Key 列表
     * @return 详细删除结果
     */
    @DeleteMapping("/batch/detailed")
    public S3DeleteResult deleteBatchDetailed(@RequestParam(required = false) String bucketName,
                                              @RequestParam List<String> objectKeys) {
        Assert.notEmpty(objectKeys, "批量删除对象 Key 不能为空");

        String resolvedBucketName = s3Service.resolveBucketName(bucketName);
        S3DeleteResult result = s3Service.deleteBatchDetailed(resolvedBucketName, objectKeys);

        log.info("批量删除 S3 对象完成，bucketName={}，deletedCount={}，errorCount={}",
                resolvedBucketName, result.deletedCount(), result.errorCount());

        return result;
    }

    /**
     * 按前缀删除对象
     * <p>
     * bucketName 不传时使用默认存储桶。
     * 该接口会删除 prefix 匹配到的全部对象，必须传 confirm=true。
     * <p>
     * curl 使用示例：
     * curl -X DELETE "http://localhost:14002/api/s3/objects/delete/prefix?prefix=temp/&confirm=true"
     * <p>
     * curl 使用示例：
     * curl -X DELETE "http://localhost:14002/api/s3/objects/delete/prefix?bucketName=data&prefix=temp/&confirm=true"
     *
     * @param bucketName 存储桶名称，可为空
     * @param prefix     对象 Key 前缀
     * @param confirm    是否确认删除
     * @return 删除结果
     */
    @DeleteMapping("/prefix")
    public Dict deleteByPrefix(@RequestParam(required = false) String bucketName,
                               @RequestParam String prefix,
                               @RequestParam(required = false) Boolean confirm) {
        assertDeletePrefixConfirmed(prefix, confirm);

        String resolvedBucketName = s3Service.resolveBucketName(bucketName);
        Long deletedCount = s3Service.deleteByPrefix(resolvedBucketName, prefix);

        log.info("按前缀删除 S3 对象成功，bucketName={}，prefix={}，deletedCount={}",
                resolvedBucketName, prefix, deletedCount);

        return Dict.create()
                .set("bucketName", resolvedBucketName)
                .set("prefix", prefix)
                .set("deletedCount", deletedCount)
                .set("deleted", true);
    }

    /**
     * 按前缀删除对象，并返回详细删除结果
     * <p>
     * bucketName 不传时使用默认存储桶。
     * 该接口会删除 prefix 匹配到的全部对象，必须传 confirm=true。
     * <p>
     * curl 使用示例：
     * curl -X DELETE "http://localhost:14002/api/s3/objects/delete/prefix/detailed?prefix=temp/&confirm=true"
     * <p>
     * curl 使用示例：
     * curl -X DELETE "http://localhost:14002/api/s3/objects/delete/prefix/detailed?bucketName=data&prefix=temp/&confirm=true"
     *
     * @param bucketName 存储桶名称，可为空
     * @param prefix     对象 Key 前缀
     * @param confirm    是否确认删除
     * @return 详细删除结果
     */
    @DeleteMapping("/prefix/detailed")
    public S3DeleteResult deleteByPrefixDetailed(@RequestParam(required = false) String bucketName,
                                                 @RequestParam String prefix,
                                                 @RequestParam(required = false) Boolean confirm) {
        assertDeletePrefixConfirmed(prefix, confirm);

        String resolvedBucketName = s3Service.resolveBucketName(bucketName);
        S3DeleteResult result = s3Service.deleteByPrefixDetailed(resolvedBucketName, prefix);

        log.info("按前缀删除 S3 对象完成，bucketName={}，prefix={}，deletedCount={}，errorCount={}",
                resolvedBucketName, prefix, result.deletedCount(), result.errorCount());

        return result;
    }

    /**
     * 删除指定版本对象
     * <p>
     * bucketName 必传。
     * <p>
     * curl 使用示例：
     * curl -X DELETE "http://localhost:14002/api/s3/objects/delete/version?bucketName=data&objectKey=archive/report.pdf&versionId=your-version-id"
     *
     * @param bucketName 存储桶名称
     * @param objectKey  对象 Key
     * @param versionId  版本 ID
     * @return 删除结果
     */
    @DeleteMapping("/version")
    public Dict deleteVersion(@RequestParam String bucketName,
                              @RequestParam String objectKey,
                              @RequestParam String versionId) {
        String resolvedBucketName = s3Service.resolveBucketName(bucketName);
        s3Service.deleteVersion(resolvedBucketName, objectKey, versionId);

        log.info("删除 S3 指定版本对象成功，bucketName={}，objectKey={}，versionId={}",
                resolvedBucketName, objectKey, versionId);

        return Dict.create()
                .set("bucketName", resolvedBucketName)
                .set("objectKey", objectKey)
                .set("versionId", versionId)
                .set("deleted", true);
    }

    /**
     * 校验按前缀删除确认参数
     *
     * @param prefix  对象 Key 前缀
     * @param confirm 是否确认删除
     */
    private void assertDeletePrefixConfirmed(String prefix, Boolean confirm) {
        Assert.notBlank(prefix, "按前缀删除时 prefix 不能为空");
        Assert.isTrue(BooleanUtil.isTrue(confirm), "按前缀删除属于高风险操作，必须传 confirm=true");

        String normalizedPrefix = StrUtil.trim(prefix);
        Assert.isTrue(!StrUtil.equals(normalizedPrefix, "/"), "禁止使用根路径前缀删除对象");
        Assert.isTrue(!StrUtil.equals(normalizedPrefix, "*"), "禁止使用通配符前缀删除对象");
        Assert.isTrue(CollUtil.newArrayList(normalizedPrefix.split("/")).stream().anyMatch(StrUtil::isNotBlank),
                "按前缀删除时 prefix 不能是空路径");
    }
}
```

### 对象复制与移动

```java
package io.github.atengk.aws.s3.controller;

import cn.hutool.core.lang.Assert;
import cn.hutool.core.lang.Dict;
import cn.hutool.core.util.BooleanUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.json.JSONObject;
import cn.hutool.json.JSONUtil;
import io.github.atengk.aws.s3.model.enums.S3ObjectAcl;
import io.github.atengk.aws.s3.model.enums.S3ServerSideEncryption;
import io.github.atengk.aws.s3.model.enums.S3StorageClass;
import io.github.atengk.aws.s3.model.request.S3CopyRequest;
import io.github.atengk.aws.s3.model.request.S3MoveRequest;
import io.github.atengk.aws.s3.model.result.S3CopyResult;
import io.github.atengk.aws.s3.model.result.S3MoveResult;
import io.github.atengk.aws.s3.service.S3Service;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * S3 对象复制与移动接口
 *
 * @author Ateng
 * @since 2026-04-28
 */
@Slf4j
@Validated
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/s3/objects/transfer")
public class S3ObjectTransferController {

    private final S3Service s3Service;

    /**
     * 在默认存储桶内复制对象
     * <p>
     * curl 使用示例：
     * curl -X POST "http://localhost:14002/api/s3/objects/transfer/copy" \
     * -d "sourceKey=upload/a.txt" \
     * -d "targetKey=backup/a.txt"
     *
     * @param sourceKey 源对象 Key
     * @param targetKey 目标对象 Key
     * @return 复制结果
     */
    @PostMapping(value = "/copy", consumes = MediaType.APPLICATION_FORM_URLENCODED_VALUE)
    public S3CopyResult copy(@RequestParam String sourceKey,
                             @RequestParam String targetKey) {
        S3CopyResult result = s3Service.copy(sourceKey, targetKey);

        log.info("复制 S3 对象成功，sourceBucketName={}，sourceKey={}，targetBucketName={}，targetKey={}",
                result.sourceBucketName(), result.sourceKey(), result.targetBucketName(), result.targetKey());

        return result;
    }

    /**
     * 跨存储桶复制对象
     * <p>
     * curl 使用示例：
     * curl -X POST "http://localhost:14002/api/s3/objects/transfer/copy/cross-bucket" \
     * -d "sourceBucketName=data" \
     * -d "sourceKey=upload/a.txt" \
     * -d "targetBucketName=archive" \
     * -d "targetKey=backup/a.txt"
     *
     * @param sourceBucketName 源存储桶名称
     * @param sourceKey        源对象 Key
     * @param targetBucketName 目标存储桶名称
     * @param targetKey        目标对象 Key
     * @return 复制结果
     */
    @PostMapping(value = "/copy/cross-bucket", consumes = MediaType.APPLICATION_FORM_URLENCODED_VALUE)
    public S3CopyResult copyCrossBucket(@RequestParam String sourceBucketName,
                                        @RequestParam String sourceKey,
                                        @RequestParam String targetBucketName,
                                        @RequestParam String targetKey) {
        S3CopyResult result = s3Service.copy(sourceBucketName, sourceKey, targetBucketName, targetKey);

        log.info("跨桶复制 S3 对象成功，sourceBucketName={}，sourceKey={}，targetBucketName={}，targetKey={}",
                result.sourceBucketName(), result.sourceKey(), result.targetBucketName(), result.targetKey());

        return result;
    }

    /**
     * 高级复制对象
     * <p>
     * 支持 sourceVersionId、metadata、tags、replaceMetadata、replaceTags、ACL、存储类型和服务端加密。
     * metadataJson 和 tagsJson 为 JSON 对象字符串。
     * sourceBucketName 或 targetBucketName 不传时使用默认存储桶。
     * <p>
     * curl 使用示例：
     * curl -X POST "http://localhost:14002/api/s3/objects/transfer/copy/advanced" \
     * -d "sourceBucketName=data" \
     * -d "sourceKey=upload/report.pdf" \
     * -d "sourceVersionId=your-source-version-id" \
     * -d "targetBucketName=archive" \
     * -d "targetKey=backup/report.pdf" \
     * -d "replaceMetadata=true" \
     * -d "metadataJson={\"biz-type\":\"report\",\"owner\":\"ateng\"}" \
     * -d "replaceTags=true" \
     * -d "tagsJson={\"env\":\"prod\",\"type\":\"pdf\"}" \
     * -d "acl=PRIVATE" \
     * -d "storageClass=STANDARD" \
     * -d "serverSideEncryption=AWS_KMS" \
     * -d "kmsKeyId=your-kms-key-id"
     * <p>
     * curl 使用示例：
     * curl -X POST "http://localhost:14002/api/s3/objects/transfer/copy/advanced" \
     * -d "sourceKey=upload/a.txt" \
     * -d "targetKey=backup/a.txt" \
     * -d "replaceTags=false"
     *
     * @param sourceBucketName     源存储桶名称，可为空
     * @param sourceKey            源对象 Key
     * @param sourceVersionId      源对象版本 ID，可为空
     * @param targetBucketName     目标存储桶名称，可为空
     * @param targetKey            目标对象 Key
     * @param metadataJson         元数据 JSON，可为空
     * @param tagsJson             标签 JSON，可为空
     * @param replaceMetadata      是否替换元数据
     * @param replaceTags          是否替换标签
     * @param acl                  对象 ACL，可为空
     * @param storageClass         存储类型，可为空
     * @param serverSideEncryption 服务端加密方式，可为空
     * @param kmsKeyId             KMS Key ID，可为空
     * @return 复制结果
     */
    @PostMapping(value = "/copy/advanced", consumes = MediaType.APPLICATION_FORM_URLENCODED_VALUE)
    public S3CopyResult copyAdvanced(@RequestParam(required = false) String sourceBucketName,
                                     @RequestParam String sourceKey,
                                     @RequestParam(required = false) String sourceVersionId,
                                     @RequestParam(required = false) String targetBucketName,
                                     @RequestParam String targetKey,
                                     @RequestParam(required = false) String metadataJson,
                                     @RequestParam(required = false) String tagsJson,
                                     @RequestParam(required = false) Boolean replaceMetadata,
                                     @RequestParam(required = false) Boolean replaceTags,
                                     @RequestParam(required = false) String acl,
                                     @RequestParam(required = false) String storageClass,
                                     @RequestParam(required = false) String serverSideEncryption,
                                     @RequestParam(required = false) String kmsKeyId) {
        String resolvedSourceBucketName = s3Service.resolveBucketName(sourceBucketName);
        String resolvedTargetBucketName = s3Service.resolveBucketName(targetBucketName);

        S3CopyRequest request = new S3CopyRequest(
                resolvedSourceBucketName,
                sourceKey,
                blankToNull(sourceVersionId),
                resolvedTargetBucketName,
                targetKey,
                parseStringMap(metadataJson),
                parseStringMap(tagsJson),
                BooleanUtil.isTrue(replaceMetadata),
                BooleanUtil.isTrue(replaceTags),
                parseEnum(S3ObjectAcl.class, acl, "S3 对象 ACL 不合法"),
                parseEnum(S3StorageClass.class, storageClass, "S3 存储类型不合法"),
                parseEnum(S3ServerSideEncryption.class, serverSideEncryption, "S3 服务端加密方式不合法"),
                blankToNull(kmsKeyId)
        );

        S3CopyResult result = s3Service.copy(request);

        log.info("高级复制 S3 对象成功，sourceBucketName={}，sourceKey={}，targetBucketName={}，targetKey={}，replaceMetadata={}，replaceTags={}",
                result.sourceBucketName(),
                result.sourceKey(),
                result.targetBucketName(),
                result.targetKey(),
                BooleanUtil.isTrue(replaceMetadata),
                BooleanUtil.isTrue(replaceTags));

        return result;
    }

    /**
     * 在默认存储桶内移动对象
     * <p>
     * curl 使用示例：
     * curl -X POST "http://localhost:14002/api/s3/objects/transfer/move" \
     * -d "sourceKey=upload/a.txt" \
     * -d "targetKey=archive/a.txt"
     *
     * @param sourceKey 源对象 Key
     * @param targetKey 目标对象 Key
     * @return 移动结果
     */
    @PostMapping(value = "/move", consumes = MediaType.APPLICATION_FORM_URLENCODED_VALUE)
    public S3MoveResult move(@RequestParam String sourceKey,
                             @RequestParam String targetKey) {
        S3MoveResult result = s3Service.move(sourceKey, targetKey);

        log.info("移动 S3 对象成功，sourceBucketName={}，sourceKey={}，targetBucketName={}，targetKey={}，deletedSource={}",
                result.sourceBucketName(),
                result.sourceKey(),
                result.targetBucketName(),
                result.targetKey(),
                result.deletedSource());

        return result;
    }

    /**
     * 跨存储桶移动对象
     * <p>
     * curl 使用示例：
     * curl -X POST "http://localhost:14002/api/s3/objects/transfer/move/cross-bucket" \
     * -d "sourceBucketName=data" \
     * -d "sourceKey=upload/a.txt" \
     * -d "targetBucketName=archive" \
     * -d "targetKey=backup/a.txt"
     *
     * @param sourceBucketName 源存储桶名称
     * @param sourceKey        源对象 Key
     * @param targetBucketName 目标存储桶名称
     * @param targetKey        目标对象 Key
     * @return 移动结果
     */
    @PostMapping(value = "/move/cross-bucket", consumes = MediaType.APPLICATION_FORM_URLENCODED_VALUE)
    public S3MoveResult moveCrossBucket(@RequestParam String sourceBucketName,
                                        @RequestParam String sourceKey,
                                        @RequestParam String targetBucketName,
                                        @RequestParam String targetKey) {
        S3MoveResult result = s3Service.move(sourceBucketName, sourceKey, targetBucketName, targetKey);

        log.info("跨桶移动 S3 对象成功，sourceBucketName={}，sourceKey={}，targetBucketName={}，targetKey={}，deletedSource={}",
                result.sourceBucketName(),
                result.sourceKey(),
                result.targetBucketName(),
                result.targetKey(),
                result.deletedSource());

        return result;
    }

    /**
     * 高级移动对象
     * <p>
     * 支持控制目标对象存在时是否覆盖。
     * sourceBucketName 或 targetBucketName 不传时使用默认存储桶。
     * <p>
     * curl 使用示例：
     * curl -X POST "http://localhost:14002/api/s3/objects/transfer/move/advanced" \
     * -d "sourceBucketName=data" \
     * -d "sourceKey=upload/a.txt" \
     * -d "targetBucketName=archive" \
     * -d "targetKey=backup/a.txt" \
     * -d "overwrite=false"
     * <p>
     * curl 使用示例：
     * curl -X POST "http://localhost:14002/api/s3/objects/transfer/move/advanced" \
     * -d "sourceKey=temp/a.txt" \
     * -d "targetKey=done/a.txt" \
     * -d "overwrite=true"
     *
     * @param sourceBucketName 源存储桶名称，可为空
     * @param sourceKey        源对象 Key
     * @param targetBucketName 目标存储桶名称，可为空
     * @param targetKey        目标对象 Key
     * @param overwrite        是否覆盖目标对象，可为空，默认由 Service 处理
     * @return 移动结果
     */
    @PostMapping(value = "/move/advanced", consumes = MediaType.APPLICATION_FORM_URLENCODED_VALUE)
    public S3MoveResult moveAdvanced(@RequestParam(required = false) String sourceBucketName,
                                     @RequestParam String sourceKey,
                                     @RequestParam(required = false) String targetBucketName,
                                     @RequestParam String targetKey,
                                     @RequestParam(required = false) Boolean overwrite) {
        String resolvedSourceBucketName = s3Service.resolveBucketName(sourceBucketName);
        String resolvedTargetBucketName = s3Service.resolveBucketName(targetBucketName);

        S3MoveRequest request = new S3MoveRequest(
                resolvedSourceBucketName,
                sourceKey,
                resolvedTargetBucketName,
                targetKey,
                overwrite
        );

        S3MoveResult result = s3Service.move(request);

        log.info("高级移动 S3 对象成功，sourceBucketName={}，sourceKey={}，targetBucketName={}，targetKey={}，overwrite={}，deletedSource={}",
                result.sourceBucketName(),
                result.sourceKey(),
                result.targetBucketName(),
                result.targetKey(),
                overwrite,
                result.deletedSource());

        return result;
    }

    /**
     * 在默认存储桶内重命名对象
     * <p>
     * curl 使用示例：
     * curl -X POST "http://localhost:14002/api/s3/objects/transfer/rename" \
     * -d "sourceKey=upload/old-name.txt" \
     * -d "targetKey=upload/new-name.txt"
     *
     * @param sourceKey 源对象 Key
     * @param targetKey 目标对象 Key
     * @return 重命名结果
     */
    @PostMapping(value = "/rename", consumes = MediaType.APPLICATION_FORM_URLENCODED_VALUE)
    public Dict rename(@RequestParam String sourceKey,
                       @RequestParam String targetKey) {
        s3Service.rename(sourceKey, targetKey);

        log.info("重命名默认存储桶 S3 对象成功，sourceKey={}，targetKey={}", sourceKey, targetKey);

        return Dict.create()
                .set("bucketName", s3Service.getDefaultBucketName())
                .set("sourceKey", sourceKey)
                .set("targetKey", targetKey)
                .set("renamed", true);
    }

    /**
     * 在指定存储桶内重命名对象
     * <p>
     * curl 使用示例：
     * curl -X POST "http://localhost:14002/api/s3/objects/transfer/rename/bucket" \
     * -d "bucketName=data" \
     * -d "sourceKey=upload/old-name.txt" \
     * -d "targetKey=upload/new-name.txt"
     *
     * @param bucketName 存储桶名称
     * @param sourceKey  源对象 Key
     * @param targetKey  目标对象 Key
     * @return 重命名结果
     */
    @PostMapping(value = "/rename/bucket", consumes = MediaType.APPLICATION_FORM_URLENCODED_VALUE)
    public Dict renameInBucket(@RequestParam String bucketName,
                               @RequestParam String sourceKey,
                               @RequestParam String targetKey) {
        String resolvedBucketName = s3Service.resolveBucketName(bucketName);
        s3Service.rename(resolvedBucketName, sourceKey, targetKey);

        log.info("重命名指定存储桶 S3 对象成功，bucketName={}，sourceKey={}，targetKey={}",
                resolvedBucketName, sourceKey, targetKey);

        return Dict.create()
                .set("bucketName", resolvedBucketName)
                .set("sourceKey", sourceKey)
                .set("targetKey", targetKey)
                .set("renamed", true);
    }

    /**
     * 解析 JSON 字符串为 Map
     *
     * @param json JSON 字符串
     * @return 字符串 Map
     */
    private Map<String, String> parseStringMap(String json) {
        if (StrUtil.isBlank(json)) {
            return Map.of();
        }

        Assert.isTrue(JSONUtil.isTypeJSONObject(json), "参数必须是 JSON 对象字符串：{}", json);

        JSONObject jsonObject = JSONUtil.parseObj(json);
        if (jsonObject.isEmpty()) {
            return Map.of();
        }

        Map<String, String> map = new LinkedHashMap<>();

        jsonObject.forEach((key, value) -> {
            if (StrUtil.isBlank(key) || value == null) {
                return;
            }

            map.put(StrUtil.trim(key), String.valueOf(value));
        });

        return map;
    }

    /**
     * 解析枚举
     *
     * @param enumClass    枚举类型
     * @param value        枚举值
     * @param errorMessage 错误信息
     * @param <E>          枚举泛型
     * @return 枚举值
     */
    private <E extends Enum<E>> E parseEnum(Class<E> enumClass, String value, String errorMessage) {
        if (StrUtil.isBlank(value)) {
            return null;
        }

        try {
            return Enum.valueOf(enumClass, StrUtil.trim(value).toUpperCase());
        } catch (IllegalArgumentException e) {
            String enumValues = Arrays.stream(enumClass.getEnumConstants())
                    .map(Enum::name)
                    .collect(Collectors.joining(","));

            throw new IllegalArgumentException(errorMessage + "，value=" + value + "，可选值：" + enumValues, e);
        }
    }

    /**
     * 空白字符串转 null，并去除前后空格
     *
     * @param value 字符串
     * @return 非空白字符串或 null
     */
    private String blankToNull(String value) {
        return StrUtil.isBlank(value) ? null : StrUtil.trim(value);
    }
}
```

### 对象列表与分页查询

```java
package io.github.atengk.aws.s3.controller;

import cn.hutool.core.util.BooleanUtil;
import cn.hutool.core.util.StrUtil;
import io.github.atengk.aws.s3.model.info.S3ObjectInfo;
import io.github.atengk.aws.s3.model.info.S3ObjectVersionInfo;
import io.github.atengk.aws.s3.model.request.S3ListRequest;
import io.github.atengk.aws.s3.model.result.S3ObjectPage;
import io.github.atengk.aws.s3.service.S3Service;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * S3 对象列表与分页查询接口
 *
 * @author Ateng
 * @since 2026-04-28
 */
@Slf4j
@Validated
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/s3/objects/list")
public class S3ObjectListController {

    private final S3Service s3Service;

    /**
     * 查询对象当前层级列表
     * <p>
     * bucketName 不传时使用默认存储桶。
     * prefix 不传时查询根层级。
     * 当前层级列表默认不递归展开子目录。
     * <p>
     * curl 使用示例：
     * curl -X GET "http://localhost:14002/api/s3/objects/list"
     * <p>
     * curl 使用示例：
     * curl -X GET "http://localhost:14002/api/s3/objects/list?bucketName=data&prefix=upload/"
     *
     * @param bucketName 存储桶名称，可为空
     * @param prefix     对象 Key 前缀，可为空
     * @return 对象信息列表
     */
    @GetMapping
    public List<S3ObjectInfo> listObjects(@RequestParam(required = false) String bucketName,
                                          @RequestParam(required = false) String prefix) {
        String resolvedBucketName = s3Service.resolveBucketName(bucketName);
        List<S3ObjectInfo> objects = s3Service.listObjects(resolvedBucketName, prefix);

        log.info("查询 S3 当前层级对象列表成功，bucketName={}，prefix={}，count={}",
                resolvedBucketName,
                StrUtil.blankToDefault(prefix, "根层级"),
                objects.size());

        return objects;
    }

    /**
     * 分页查询对象列表
     * <p>
     * bucketName 不传时使用默认存储桶。
     * delimiter 常用值为 /，用于按目录层级聚合 commonPrefixes。
     * recursive=true 时会忽略 delimiter，递归查询对象。
     * <p>
     * curl 使用示例：
     * curl -X GET "http://localhost:14002/api/s3/objects/list/page?bucketName=data&prefix=upload/&delimiter=/&maxKeys=100"
     * <p>
     * curl 使用示例：
     * curl -X GET "http://localhost:14002/api/s3/objects/list/page?bucketName=data&prefix=upload/&maxKeys=100&continuationToken=your-next-token&recursive=true"
     *
     * @param bucketName        存储桶名称，可为空
     * @param prefix            对象 Key 前缀，可为空
     * @param delimiter         分隔符，可为空
     * @param maxKeys           最大返回数量，可为空
     * @param continuationToken 分页令牌，可为空
     * @param recursive         是否递归查询，可为空
     * @return 对象分页结果
     */
    @GetMapping("/page")
    public S3ObjectPage listObjectsPage(@RequestParam(required = false) String bucketName,
                                        @RequestParam(required = false) String prefix,
                                        @RequestParam(required = false) String delimiter,
                                        @RequestParam(required = false) Integer maxKeys,
                                        @RequestParam(required = false) String continuationToken,
                                        @RequestParam(required = false) Boolean recursive) {
        String resolvedBucketName = s3Service.resolveBucketName(bucketName);

        S3ListRequest request = new S3ListRequest(
                resolvedBucketName,
                blankToNull(prefix),
                blankToNull(delimiter),
                maxKeys,
                blankToNull(continuationToken),
                BooleanUtil.isTrue(recursive)
        );

        S3ObjectPage page = s3Service.listObjectsPage(request);

        log.info("分页查询 S3 对象列表成功，bucketName={}，prefix={}，maxKeys={}，recursive={}，objectCount={}，commonPrefixCount={}，truncated={}",
                page.bucketName(),
                StrUtil.blankToDefault(page.prefix(), "根层级"),
                page.maxKeys(),
                BooleanUtil.isTrue(recursive),
                page.objects() == null ? 0 : page.objects().size(),
                page.commonPrefixes() == null ? 0 : page.commonPrefixes().size(),
                page.truncated());

        return page;
    }

    /**
     * 递归查询对象列表
     * <p>
     * bucketName 不传时使用默认存储桶。
     * prefix 不传时递归查询整个存储桶。
     * <p>
     * curl 使用示例：
     * curl -X GET "http://localhost:14002/api/s3/objects/list/recursive"
     * <p>
     * curl 使用示例：
     * curl -X GET "http://localhost:14002/api/s3/objects/list/recursive?bucketName=data&prefix=upload/"
     *
     * @param bucketName 存储桶名称，可为空
     * @param prefix     对象 Key 前缀，可为空
     * @return 对象信息列表
     */
    @GetMapping("/recursive")
    public List<S3ObjectInfo> listObjectsRecursive(@RequestParam(required = false) String bucketName,
                                                   @RequestParam(required = false) String prefix) {
        String resolvedBucketName = s3Service.resolveBucketName(bucketName);
        List<S3ObjectInfo> objects = s3Service.listObjectsRecursive(resolvedBucketName, prefix);

        log.info("递归查询 S3 对象列表成功，bucketName={}，prefix={}，count={}",
                resolvedBucketName,
                StrUtil.blankToDefault(prefix, "全部对象"),
                objects.size());

        return objects;
    }

    /**
     * 递归查询对象 Key 列表
     * <p>
     * bucketName 不传时使用默认存储桶。
     * prefix 不传时递归查询整个存储桶下的对象 Key。
     * <p>
     * curl 使用示例：
     * curl -X GET "http://localhost:14002/api/s3/objects/list/keys"
     * <p>
     * curl 使用示例：
     * curl -X GET "http://localhost:14002/api/s3/objects/list/keys?bucketName=data&prefix=upload/"
     *
     * @param bucketName 存储桶名称，可为空
     * @param prefix     对象 Key 前缀，可为空
     * @return 对象 Key 列表
     */
    @GetMapping("/keys")
    public List<String> listObjectKeys(@RequestParam(required = false) String bucketName,
                                       @RequestParam(required = false) String prefix) {
        String resolvedBucketName = s3Service.resolveBucketName(bucketName);
        List<String> objectKeys = s3Service.listObjectKeys(resolvedBucketName, prefix);

        log.info("递归查询 S3 对象 Key 列表成功，bucketName={}，prefix={}，count={}",
                resolvedBucketName,
                StrUtil.blankToDefault(prefix, "全部对象"),
                objectKeys.size());

        return objectKeys;
    }

    /**
     * 查询对象版本列表
     * <p>
     * bucketName 不传时使用默认存储桶。
     * prefix 不传时查询整个存储桶的对象版本。
     * 返回结果包含普通对象版本和删除标记。
     * <p>
     * curl 使用示例：
     * curl -X GET "http://localhost:14002/api/s3/objects/list/versions?bucketName=data&prefix=archive/"
     * <p>
     * curl 使用示例：
     * curl -X GET "http://localhost:14002/api/s3/objects/list/versions?prefix=archive/report.pdf"
     *
     * @param bucketName 存储桶名称，可为空
     * @param prefix     对象 Key 前缀，可为空
     * @return 对象版本信息列表
     */
    @GetMapping("/versions")
    public List<S3ObjectVersionInfo> listObjectVersions(@RequestParam(required = false) String bucketName,
                                                        @RequestParam(required = false) String prefix) {
        String resolvedBucketName = s3Service.resolveBucketName(bucketName);
        List<S3ObjectVersionInfo> versions = s3Service.listObjectVersions(resolvedBucketName, prefix);

        log.info("查询 S3 对象版本列表成功，bucketName={}，prefix={}，count={}",
                resolvedBucketName,
                StrUtil.blankToDefault(prefix, "全部版本"),
                versions.size());

        return versions;
    }

    /**
     * 空白字符串转 null，并去除前后空格
     *
     * @param value 字符串
     * @return 非空白字符串或 null
     */
    private String blankToNull(String value) {
        return StrUtil.isBlank(value) ? null : StrUtil.trim(value);
    }
}
```

### 目录语义操作

```java
package io.github.atengk.aws.s3.controller;

import cn.hutool.core.lang.Assert;
import cn.hutool.core.lang.Dict;
import cn.hutool.core.util.BooleanUtil;
import cn.hutool.core.util.StrUtil;
import io.github.atengk.aws.s3.model.info.S3ObjectInfo;
import io.github.atengk.aws.s3.service.S3Service;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * S3 目录语义操作接口
 *
 * @author Ateng
 * @since 2026-04-28
 */
@Slf4j
@Validated
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/s3/directories")
public class S3DirectoryController {

    private final S3Service s3Service;

    /**
     * 创建目录占位对象
     * <p>
     * bucketName 不传时使用默认存储桶。
     * S3 没有真实目录，该接口会创建一个以 / 结尾的零字节对象作为目录占位。
     * <p>
     * curl 使用示例：
     * curl -X POST "http://localhost:14002/api/s3/directories" \
     * -d "directoryKey=upload/2026/04/28/"
     * <p>
     * curl 使用示例：
     * curl -X POST "http://localhost:14002/api/s3/directories" \
     * -d "bucketName=data" \
     * -d "directoryKey=upload/2026/04/28/"
     *
     * @param bucketName   存储桶名称，可为空
     * @param directoryKey 目录 Key
     * @return 创建结果
     */
    @PostMapping(consumes = MediaType.APPLICATION_FORM_URLENCODED_VALUE)
    public Dict createDirectory(@RequestParam(required = false) String bucketName,
                                @RequestParam String directoryKey) {
        Assert.notBlank(directoryKey, "S3 目录 Key 不能为空");

        String resolvedBucketName = s3Service.resolveBucketName(bucketName);
        s3Service.createDirectory(resolvedBucketName, directoryKey);

        log.info("创建 S3 目录成功，bucketName={}，directoryKey={}", resolvedBucketName, directoryKey);

        return Dict.create()
                .set("bucketName", resolvedBucketName)
                .set("directoryKey", directoryKey)
                .set("created", true);
    }

    /**
     * 判断目录是否存在
     * <p>
     * bucketName 不传时使用默认存储桶。
     * 如果目录占位对象不存在，但该前缀下存在对象，也会认为目录存在。
     * <p>
     * curl 使用示例：
     * curl -X GET "http://localhost:14002/api/s3/directories/exists?directoryKey=upload/2026/04/28/"
     * <p>
     * curl 使用示例：
     * curl -X GET "http://localhost:14002/api/s3/directories/exists?bucketName=data&directoryKey=upload/2026/04/28/"
     *
     * @param bucketName   存储桶名称，可为空
     * @param directoryKey 目录 Key
     * @return 是否存在
     */
    @GetMapping("/exists")
    public Dict directoryExists(@RequestParam(required = false) String bucketName,
                                @RequestParam String directoryKey) {
        Assert.notBlank(directoryKey, "S3 目录 Key 不能为空");

        String resolvedBucketName = s3Service.resolveBucketName(bucketName);
        boolean exists = s3Service.directoryExists(resolvedBucketName, directoryKey);

        log.info("检查 S3 目录是否存在完成，bucketName={}，directoryKey={}，exists={}",
                resolvedBucketName, directoryKey, exists);

        return Dict.create()
                .set("bucketName", resolvedBucketName)
                .set("directoryKey", directoryKey)
                .set("exists", exists);
    }

    /**
     * 删除目录及其下所有对象
     * <p>
     * bucketName 不传时使用默认存储桶。
     * 该接口会删除 directoryKey 前缀匹配到的所有对象，必须传 confirm=true。
     * <p>
     * curl 使用示例：
     * curl -X DELETE "http://localhost:14002/api/s3/directories?directoryKey=temp/&confirm=true"
     * <p>
     * curl 使用示例：
     * curl -X DELETE "http://localhost:14002/api/s3/directories?bucketName=data&directoryKey=temp/&confirm=true"
     *
     * @param bucketName   存储桶名称，可为空
     * @param directoryKey 目录 Key
     * @param confirm      是否确认删除
     * @return 删除结果
     */
    @DeleteMapping
    public Dict deleteDirectory(@RequestParam(required = false) String bucketName,
                                @RequestParam String directoryKey,
                                @RequestParam(required = false) Boolean confirm) {
        assertDeleteDirectoryConfirmed(directoryKey, confirm);

        String resolvedBucketName = s3Service.resolveBucketName(bucketName);
        Long deletedCount = s3Service.deleteDirectory(resolvedBucketName, directoryKey);

        log.info("删除 S3 目录成功，bucketName={}，directoryKey={}，deletedCount={}",
                resolvedBucketName, directoryKey, deletedCount);

        return Dict.create()
                .set("bucketName", resolvedBucketName)
                .set("directoryKey", directoryKey)
                .set("deletedCount", deletedCount)
                .set("deleted", true);
    }

    /**
     * 查询目录当前层级对象列表
     * <p>
     * bucketName 不传时使用默认存储桶。
     * 该接口默认只查询当前目录层级，不递归展开子目录。
     * <p>
     * curl 使用示例：
     * curl -X GET "http://localhost:14002/api/s3/directories/list?directoryKey=upload/2026/04/28/"
     * <p>
     * curl 使用示例：
     * curl -X GET "http://localhost:14002/api/s3/directories/list?bucketName=data&directoryKey=upload/2026/04/28/"
     *
     * @param bucketName   存储桶名称，可为空
     * @param directoryKey 目录 Key
     * @return 当前层级对象列表
     */
    @GetMapping("/list")
    public List<S3ObjectInfo> listDirectory(@RequestParam(required = false) String bucketName,
                                            @RequestParam String directoryKey) {
        Assert.notBlank(directoryKey, "S3 目录 Key 不能为空");

        String resolvedBucketName = s3Service.resolveBucketName(bucketName);
        List<S3ObjectInfo> objects = s3Service.listDirectory(resolvedBucketName, directoryKey);

        log.info("查询 S3 目录当前层级对象列表成功，bucketName={}，directoryKey={}，count={}",
                resolvedBucketName, directoryKey, objects.size());

        return objects;
    }

    /**
     * 校验目录删除确认参数
     *
     * @param directoryKey 目录 Key
     * @param confirm      是否确认删除
     */
    private void assertDeleteDirectoryConfirmed(String directoryKey, Boolean confirm) {
        Assert.notBlank(directoryKey, "删除 S3 目录时 directoryKey 不能为空");
        Assert.isTrue(BooleanUtil.isTrue(confirm), "删除 S3 目录属于高风险操作，必须传 confirm=true");

        String normalizedDirectoryKey = StrUtil.trim(directoryKey);
        Assert.isTrue(!StrUtil.equals(normalizedDirectoryKey, "/"), "禁止删除根目录");
        Assert.isTrue(!StrUtil.equals(normalizedDirectoryKey, "*"), "禁止使用通配符删除目录");
        Assert.isTrue(StrUtil.isNotBlank(StrUtil.removeAll(normalizedDirectoryKey, "/")),
                "删除 S3 目录时 directoryKey 不能是空路径");
    }
}
```

### 预签名 URL

```java
package io.github.atengk.aws.s3.controller;

import cn.hutool.core.lang.Assert;
import cn.hutool.core.lang.Dict;
import cn.hutool.core.util.StrUtil;
import cn.hutool.json.JSONObject;
import cn.hutool.json.JSONUtil;
import io.github.atengk.aws.s3.model.enums.S3PresignedUrlMethod;
import io.github.atengk.aws.s3.model.request.S3PresignedUrlRequest;
import io.github.atengk.aws.s3.model.result.S3PresignedUrlResult;
import io.github.atengk.aws.s3.service.S3Service;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.convert.DurationStyle;
import org.springframework.http.MediaType;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.net.URI;
import java.time.Duration;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * S3 预签名 URL 接口
 *
 * @author Ateng
 * @since 2026-04-28
 */
@Slf4j
@Validated
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/s3/presigned-urls")
public class S3PresignedUrlController {

    private final S3Service s3Service;

    /**
     * 生成对象下载预签名 URL
     * <p>
     * bucketName 不传时使用默认存储桶。
     * expire 不传时使用配置中的默认过期时间。
     * <p>
     * curl 使用示例：
     * curl -X GET "http://localhost:14002/api/s3/presigned-urls/download?objectKey=upload/2026/04/28/test.txt"
     * <p>
     * curl 使用示例：
     * curl -X GET "http://localhost:14002/api/s3/presigned-urls/download?bucketName=data&objectKey=upload/2026/04/28/test.txt&expire=10m"
     *
     * @param bucketName 存储桶名称，可为空
     * @param objectKey  对象 Key
     * @param expire     有效期，可为空，例如 10m、30s、2h、7d、PT10M
     * @return 预签名 URL 信息
     */
    @GetMapping("/download")
    public Dict generateDownloadUrl(@RequestParam(required = false) String bucketName,
                                    @RequestParam String objectKey,
                                    @RequestParam(required = false) String expire) {
        String resolvedBucketName = s3Service.resolveBucketName(bucketName);
        Duration resolvedExpire = parseDuration(expire);

        URI url = resolvedExpire == null
                ? s3Service.generateDownloadUrl(resolvedBucketName, objectKey)
                : s3Service.generateDownloadUrl(resolvedBucketName, objectKey, resolvedExpire);

        log.info("生成 S3 下载预签名 URL 成功，bucketName={}，objectKey={}，expire={}",
                resolvedBucketName, objectKey, StrUtil.blankToDefault(expire, "默认配置"));

        return Dict.create()
                .set("bucketName", resolvedBucketName)
                .set("objectKey", objectKey)
                .set("method", S3PresignedUrlMethod.GET)
                .set("url", url.toString());
    }

    /**
     * 生成对象上传预签名 URL
     * <p>
     * bucketName 不传时使用默认存储桶。
     * expire 不传时使用配置中的默认过期时间。
     * <p>
     * curl 使用示例：
     * curl -X GET "http://localhost:14002/api/s3/presigned-urls/upload?objectKey=upload/2026/04/28/test.txt&expire=10m"
     * <p>
     * curl 使用示例：
     * curl -X GET "http://localhost:14002/api/s3/presigned-urls/upload?bucketName=data&objectKey=upload/2026/04/28/test.txt&expire=10m"
     *
     * @param bucketName 存储桶名称，可为空
     * @param objectKey  对象 Key
     * @param expire     有效期，可为空，例如 10m、30s、2h、7d、PT10M
     * @return 预签名 URL 信息
     */
    @GetMapping("/upload")
    public Dict generateUploadUrl(@RequestParam(required = false) String bucketName,
                                  @RequestParam String objectKey,
                                  @RequestParam(required = false) String expire) {
        String resolvedBucketName = s3Service.resolveBucketName(bucketName);
        Duration resolvedExpire = parseDuration(expire);

        URI url = s3Service.generateUploadUrl(resolvedBucketName, objectKey, resolvedExpire);

        log.info("生成 S3 上传预签名 URL 成功，bucketName={}，objectKey={}，expire={}",
                resolvedBucketName, objectKey, StrUtil.blankToDefault(expire, "默认配置"));

        return Dict.create()
                .set("bucketName", resolvedBucketName)
                .set("objectKey", objectKey)
                .set("method", S3PresignedUrlMethod.PUT)
                .set("url", url.toString());
    }

    /**
     * 生成对象 HEAD 预签名 URL
     * <p>
     * bucketName 不传时使用默认存储桶。
     * expire 不传时使用配置中的默认过期时间。
     * <p>
     * curl 使用示例：
     * curl -X GET "http://localhost:14002/api/s3/presigned-urls/head?objectKey=upload/2026/04/28/test.txt&expire=10m"
     * <p>
     * curl 使用示例：
     * curl -X GET "http://localhost:14002/api/s3/presigned-urls/head?bucketName=data&objectKey=upload/2026/04/28/test.txt&expire=10m"
     *
     * @param bucketName 存储桶名称，可为空
     * @param objectKey  对象 Key
     * @param expire     有效期，可为空，例如 10m、30s、2h、7d、PT10M
     * @return 预签名 URL 信息
     */
    @GetMapping("/head")
    public Dict generateHeadUrl(@RequestParam(required = false) String bucketName,
                                @RequestParam String objectKey,
                                @RequestParam(required = false) String expire) {
        String resolvedBucketName = s3Service.resolveBucketName(bucketName);
        Duration resolvedExpire = parseDuration(expire);

        URI url = s3Service.generateHeadUrl(resolvedBucketName, objectKey, resolvedExpire);

        log.info("生成 S3 HEAD 预签名 URL 成功，bucketName={}，objectKey={}，expire={}",
                resolvedBucketName, objectKey, StrUtil.blankToDefault(expire, "默认配置"));

        return Dict.create()
                .set("bucketName", resolvedBucketName)
                .set("objectKey", objectKey)
                .set("method", S3PresignedUrlMethod.HEAD)
                .set("url", url.toString());
    }

    /**
     * 生成对象删除预签名 URL
     * <p>
     * bucketName 不传时使用默认存储桶。
     * expire 不传时使用配置中的默认过期时间。
     * <p>
     * curl 使用示例：
     * curl -X GET "http://localhost:14002/api/s3/presigned-urls/delete?objectKey=temp/a.txt&expire=10m"
     * <p>
     * curl 使用示例：
     * curl -X GET "http://localhost:14002/api/s3/presigned-urls/delete?bucketName=data&objectKey=temp/a.txt&expire=10m"
     *
     * @param bucketName 存储桶名称，可为空
     * @param objectKey  对象 Key
     * @param expire     有效期，可为空，例如 10m、30s、2h、7d、PT10M
     * @return 预签名 URL 信息
     */
    @GetMapping("/delete")
    public Dict generateDeleteUrl(@RequestParam(required = false) String bucketName,
                                  @RequestParam String objectKey,
                                  @RequestParam(required = false) String expire) {
        String resolvedBucketName = s3Service.resolveBucketName(bucketName);
        Duration resolvedExpire = parseDuration(expire);

        URI url = s3Service.generateDeleteUrl(resolvedBucketName, objectKey, resolvedExpire);

        log.info("生成 S3 删除预签名 URL 成功，bucketName={}，objectKey={}，expire={}",
                resolvedBucketName, objectKey, StrUtil.blankToDefault(expire, "默认配置"));

        return Dict.create()
                .set("bucketName", resolvedBucketName)
                .set("objectKey", objectKey)
                .set("method", S3PresignedUrlMethod.DELETE)
                .set("url", url.toString());
    }

    /**
     * 生成高级预签名 URL
     * <p>
     * 支持 GET、PUT、HEAD、DELETE。
     * requestHeadersJson 和 responseHeadersJson 为 JSON 对象字符串。
     * responseHeadersJson 主要用于 GET 下载 URL 的响应头覆盖。
     * bucketName 不传时使用默认存储桶。
     * <p>
     * curl 使用示例：
     * curl -X POST "http://localhost:14002/api/s3/presigned-urls/generate" \
     * -d "bucketName=data" \
     * -d "objectKey=upload/report.pdf" \
     * -d "method=GET" \
     * -d "expire=10m" \
     * -d "filename=report.pdf" \
     * -d "responseHeadersJson={\"response-content-type\":\"application/pdf\"}"
     * <p>
     * curl 使用示例：
     * curl -X POST "http://localhost:14002/api/s3/presigned-urls/generate" \
     * -d "objectKey=upload/test.txt" \
     * -d "method=PUT" \
     * -d "expire=10m" \
     * -d "contentType=text/plain" \
     * -d "requestHeadersJson={\"x-amz-meta-source\":\"presigned-url\"}"
     *
     * @param bucketName          存储桶名称，可为空
     * @param objectKey           对象 Key
     * @param method              请求方法，可为空，默认 GET
     * @param expire              有效期，可为空，例如 10m、30s、2h、7d、PT10M
     * @param contentType         Content-Type，可为空
     * @param filename            下载文件名，可为空
     * @param requestHeadersJson  参与签名的请求头 JSON，可为空
     * @param responseHeadersJson GET 响应头覆盖 JSON，可为空
     * @return 预签名 URL 结果
     */
    @PostMapping(value = "/generate", consumes = MediaType.APPLICATION_FORM_URLENCODED_VALUE)
    public S3PresignedUrlResult generatePresignedUrl(@RequestParam(required = false) String bucketName,
                                                     @RequestParam String objectKey,
                                                     @RequestParam(required = false) String method,
                                                     @RequestParam(required = false) String expire,
                                                     @RequestParam(required = false) String contentType,
                                                     @RequestParam(required = false) String filename,
                                                     @RequestParam(required = false) String requestHeadersJson,
                                                     @RequestParam(required = false) String responseHeadersJson) {
        String resolvedBucketName = s3Service.resolveBucketName(bucketName);
        S3PresignedUrlMethod resolvedMethod = parseEnum(
                S3PresignedUrlMethod.class,
                StrUtil.blankToDefault(method, S3PresignedUrlMethod.GET.name()),
                "S3 预签名 URL 方法不合法"
        );

        S3PresignedUrlRequest request = new S3PresignedUrlRequest(
                resolvedBucketName,
                objectKey,
                resolvedMethod,
                parseDuration(expire),
                blankToNull(contentType),
                blankToNull(filename),
                parseStringMap(requestHeadersJson),
                parseStringMap(responseHeadersJson)
        );

        S3PresignedUrlResult result = s3Service.generatePresignedUrl(request);

        log.info("生成 S3 高级预签名 URL 成功，bucketName={}，objectKey={}，method={}，expire={}",
                result.bucketName(), result.objectKey(), result.method(), StrUtil.blankToDefault(expire, "默认配置"));

        return result;
    }

    /**
     * 解析 Duration 字符串
     *
     * @param value Duration 字符串
     * @return Duration
     */
    private Duration parseDuration(String value) {
        if (StrUtil.isBlank(value)) {
            return null;
        }

        try {
            return DurationStyle.detectAndParse(StrUtil.trim(value));
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("S3 预签名 URL 有效期格式不合法，value=" + value + "，示例：10m、30s、2h、7d、PT10M", e);
        }
    }

    /**
     * 解析 JSON 字符串为 Map
     *
     * @param json JSON 字符串
     * @return 字符串 Map
     */
    private Map<String, String> parseStringMap(String json) {
        if (StrUtil.isBlank(json)) {
            return Map.of();
        }

        Assert.isTrue(JSONUtil.isTypeJSONObject(json), "参数必须是 JSON 对象字符串：{}", json);

        JSONObject jsonObject = JSONUtil.parseObj(json);
        if (jsonObject.isEmpty()) {
            return Map.of();
        }

        Map<String, String> map = new LinkedHashMap<>();

        jsonObject.forEach((key, value) -> {
            if (StrUtil.isBlank(key) || value == null) {
                return;
            }

            map.put(StrUtil.trim(key), StrUtil.toString(value));
        });

        return map;
    }

    /**
     * 解析枚举
     *
     * @param enumClass    枚举类型
     * @param value        枚举值
     * @param errorMessage 错误信息
     * @param <E>          枚举泛型
     * @return 枚举值
     */
    private <E extends Enum<E>> E parseEnum(Class<E> enumClass, String value, String errorMessage) {
        if (StrUtil.isBlank(value)) {
            return null;
        }

        try {
            return Enum.valueOf(enumClass, StrUtil.trim(value).toUpperCase());
        } catch (IllegalArgumentException e) {
            String enumValues = Arrays.stream(enumClass.getEnumConstants())
                    .map(Enum::name)
                    .collect(Collectors.joining(","));

            throw new IllegalArgumentException(errorMessage + "，value=" + value + "，可选值：" + enumValues, e);
        }
    }

    /**
     * 空白字符串转 null，并去除前后空格
     *
     * @param value 字符串
     * @return 非空白字符串或 null
     */
    private String blankToNull(String value) {
        return StrUtil.isBlank(value) ? null : StrUtil.trim(value);
    }
}
```

### 元数据管理

```java
package io.github.atengk.aws.s3.controller;

import cn.hutool.core.lang.Assert;
import cn.hutool.core.lang.Dict;
import cn.hutool.core.util.BooleanUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.json.JSONObject;
import cn.hutool.json.JSONUtil;
import io.github.atengk.aws.s3.model.enums.S3StorageClass;
import io.github.atengk.aws.s3.model.request.S3MetadataRequest;
import io.github.atengk.aws.s3.service.S3Service;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * S3 对象元数据管理接口
 *
 * @author Ateng
 * @since 2026-04-28
 */
@Slf4j
@Validated
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/s3/objects/metadata")
public class S3ObjectMetadataController {

    private final S3Service s3Service;

    /**
     * 获取对象自定义元数据
     * <p>
     * bucketName 不传时使用默认存储桶。
     * 返回的是对象自定义 metadata，不包含 Content-Type、Content-Length、ETag 等系统元数据。
     * <p>
     * curl 使用示例：
     * curl -X GET "http://localhost:14002/api/s3/objects/metadata?objectKey=upload/2026/04/28/test.txt"
     * <p>
     * curl 使用示例：
     * curl -X GET "http://localhost:14002/api/s3/objects/metadata?bucketName=data&objectKey=upload/2026/04/28/test.txt"
     *
     * @param bucketName 存储桶名称，可为空
     * @param objectKey  对象 Key
     * @return 自定义元数据
     */
    @GetMapping
    public Dict getMetadata(@RequestParam(required = false) String bucketName,
                            @RequestParam String objectKey) {
        String resolvedBucketName = s3Service.resolveBucketName(bucketName);
        Map<String, String> metadata = s3Service.getMetadata(resolvedBucketName, objectKey);

        log.info("获取 S3 对象元数据成功，bucketName={}，objectKey={}，metadataSize={}",
                resolvedBucketName, objectKey, metadata.size());

        return Dict.create()
                .set("bucketName", resolvedBucketName)
                .set("objectKey", objectKey)
                .set("metadata", metadata);
    }

    /**
     * 替换对象自定义元数据
     * <p>
     * bucketName 不传时使用默认存储桶。
     * metadataJson 为 JSON 对象字符串。
     * 该接口会完整替换原对象自定义 metadata，不是增量合并。
     * <p>
     * curl 使用示例：
     * curl -X PUT "http://localhost:14002/api/s3/objects/metadata" \
     * -d "objectKey=upload/2026/04/28/test.txt" \
     * -d "metadataJson={\"biz-type\":\"report\",\"owner\":\"ateng\"}"
     * <p>
     * curl 使用示例：
     * curl -X PUT "http://localhost:14002/api/s3/objects/metadata" \
     * -d "bucketName=data" \
     * -d "objectKey=upload/2026/04/28/test.txt" \
     * -d "metadataJson={\"source\":\"api\",\"trace-id\":\"abc123\"}"
     * <p>
     * curl 使用示例：
     * curl -X PUT "http://localhost:14002/api/s3/objects/metadata" \
     * -d "bucketName=data" \
     * -d "objectKey=upload/2026/04/28/test.txt" \
     * -d "metadataJson={}"
     *
     * @param bucketName   存储桶名称，可为空
     * @param objectKey    对象 Key
     * @param metadataJson 元数据 JSON 对象字符串，可为空或 {}
     * @return 替换结果
     */
    @PutMapping(consumes = MediaType.APPLICATION_FORM_URLENCODED_VALUE)
    public Dict replaceMetadata(@RequestParam(required = false) String bucketName,
                                @RequestParam String objectKey,
                                @RequestParam(required = false) String metadataJson) {
        String resolvedBucketName = s3Service.resolveBucketName(bucketName);
        Map<String, String> metadata = parseStringMap(metadataJson);

        s3Service.replaceMetadata(resolvedBucketName, objectKey, metadata);

        log.info("替换 S3 对象元数据成功，bucketName={}，objectKey={}，metadataSize={}",
                resolvedBucketName, objectKey, metadata.size());

        return Dict.create()
                .set("bucketName", resolvedBucketName)
                .set("objectKey", objectKey)
                .set("metadata", metadata)
                .set("replaced", true);
    }

    /**
     * 高级替换对象自定义元数据
     * <p>
     * bucketName 不传时使用默认存储桶。
     * metadataJson 为 JSON 对象字符串。
     * preserveTags=true 表示保留原对象标签。
     * preserveAcl=true 表示复制前读取原 ACL，复制后再写回原 ACL。
     * storageClass 不传时保留服务端默认处理，不主动修改存储类型。
     * <p>
     * curl 使用示例：
     * curl -X PUT "http://localhost:14002/api/s3/objects/metadata/advanced" \
     * -d "bucketName=data" \
     * -d "objectKey=upload/report.pdf" \
     * -d "metadataJson={\"biz-type\":\"report\",\"owner\":\"ateng\"}" \
     * -d "preserveTags=true" \
     * -d "preserveAcl=false" \
     * -d "storageClass=STANDARD"
     * <p>
     * curl 使用示例：
     * curl -X PUT "http://localhost:14002/api/s3/objects/metadata/advanced" \
     * -d "objectKey=upload/test.txt" \
     * -d "metadataJson={}" \
     * -d "preserveTags=false" \
     * -d "preserveAcl=false"
     *
     * @param bucketName   存储桶名称，可为空
     * @param objectKey    对象 Key
     * @param metadataJson 元数据 JSON 对象字符串，可为空或 {}
     * @param preserveTags 是否保留原标签，可为空
     * @param preserveAcl  是否保留原 ACL，可为空
     * @param storageClass 存储类型，可为空
     * @return 替换结果
     */
    @PutMapping(value = "/advanced", consumes = MediaType.APPLICATION_FORM_URLENCODED_VALUE)
    public Dict replaceMetadataAdvanced(@RequestParam(required = false) String bucketName,
                                        @RequestParam String objectKey,
                                        @RequestParam(required = false) String metadataJson,
                                        @RequestParam(required = false) Boolean preserveTags,
                                        @RequestParam(required = false) Boolean preserveAcl,
                                        @RequestParam(required = false) String storageClass) {
        String resolvedBucketName = s3Service.resolveBucketName(bucketName);
        Map<String, String> metadata = parseStringMap(metadataJson);
        S3StorageClass resolvedStorageClass = parseEnum(
                S3StorageClass.class,
                storageClass,
                "S3 存储类型不合法"
        );

        S3MetadataRequest request = new S3MetadataRequest(
                resolvedBucketName,
                objectKey,
                metadata,
                BooleanUtil.isTrue(preserveTags),
                BooleanUtil.isTrue(preserveAcl),
                resolvedStorageClass
        );

        s3Service.replaceMetadata(request);

        log.info("高级替换 S3 对象元数据成功，bucketName={}，objectKey={}，metadataSize={}，preserveTags={}，preserveAcl={}，storageClass={}",
                resolvedBucketName,
                objectKey,
                metadata.size(),
                BooleanUtil.isTrue(preserveTags),
                BooleanUtil.isTrue(preserveAcl),
                resolvedStorageClass);

        return Dict.create()
                .set("bucketName", resolvedBucketName)
                .set("objectKey", objectKey)
                .set("metadata", metadata)
                .set("preserveTags", BooleanUtil.isTrue(preserveTags))
                .set("preserveAcl", BooleanUtil.isTrue(preserveAcl))
                .set("storageClass", resolvedStorageClass)
                .set("replaced", true);
    }

    /**
     * 解析 JSON 字符串为 Map
     *
     * @param json JSON 字符串
     * @return 字符串 Map
     */
    private Map<String, String> parseStringMap(String json) {
        if (StrUtil.isBlank(json)) {
            return Map.of();
        }

        Assert.isTrue(JSONUtil.isTypeJSONObject(json), "参数必须是 JSON 对象字符串：{}", json);

        JSONObject jsonObject = JSONUtil.parseObj(json);
        if (jsonObject.isEmpty()) {
            return Map.of();
        }

        Map<String, String> map = new LinkedHashMap<>();

        jsonObject.forEach((key, value) -> {
            if (StrUtil.isBlank(key) || value == null) {
                return;
            }

            map.put(StrUtil.trim(key), StrUtil.toString(value));
        });

        return map;
    }

    /**
     * 解析枚举
     *
     * @param enumClass    枚举类型
     * @param value        枚举值
     * @param errorMessage 错误信息
     * @param <E>          枚举泛型
     * @return 枚举值
     */
    private <E extends Enum<E>> E parseEnum(Class<E> enumClass, String value, String errorMessage) {
        if (StrUtil.isBlank(value)) {
            return null;
        }

        try {
            return Enum.valueOf(enumClass, StrUtil.trim(value).toUpperCase());
        } catch (IllegalArgumentException e) {
            String enumValues = Arrays.stream(enumClass.getEnumConstants())
                    .map(Enum::name)
                    .collect(Collectors.joining(","));

            throw new IllegalArgumentException(errorMessage + "，value=" + value + "，可选值：" + enumValues, e);
        }
    }
}
```

### 标签管理

```java
package io.github.atengk.aws.s3.controller;

import cn.hutool.core.lang.Assert;
import cn.hutool.core.lang.Dict;
import cn.hutool.core.util.StrUtil;
import cn.hutool.json.JSONObject;
import cn.hutool.json.JSONUtil;
import io.github.atengk.aws.s3.service.S3Service;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * S3 对象标签管理接口
 *
 * @author Ateng
 * @since 2026-04-28
 */
@Slf4j
@Validated
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/s3/objects/tags")
public class S3ObjectTagController {

    private final S3Service s3Service;

    /**
     * 获取对象标签
     * <p>
     * bucketName 不传时使用默认存储桶。
     * <p>
     * curl 使用示例：
     * curl -X GET "http://localhost:14002/api/s3/objects/tags?objectKey=upload/2026/04/28/test.txt"
     * <p>
     * curl 使用示例：
     * curl -X GET "http://localhost:14002/api/s3/objects/tags?bucketName=data&objectKey=upload/2026/04/28/test.txt"
     *
     * @param bucketName 存储桶名称，可为空
     * @param objectKey  对象 Key
     * @return 对象标签
     */
    @GetMapping
    public Dict getTags(@RequestParam(required = false) String bucketName,
                        @RequestParam String objectKey) {
        String resolvedBucketName = s3Service.resolveBucketName(bucketName);
        Map<String, String> tags = s3Service.getTags(resolvedBucketName, objectKey);

        log.info("获取 S3 对象标签成功，bucketName={}，objectKey={}，tagSize={}",
                resolvedBucketName, objectKey, tags.size());

        return Dict.create()
                .set("bucketName", resolvedBucketName)
                .set("objectKey", objectKey)
                .set("tags", tags);
    }

    /**
     * 获取指定版本对象标签
     * <p>
     * bucketName 不传时使用默认存储桶。
     * <p>
     * curl 使用示例：
     * curl -X GET "http://localhost:14002/api/s3/objects/tags/version?objectKey=archive/report.pdf&versionId=your-version-id"
     * <p>
     * curl 使用示例：
     * curl -X GET "http://localhost:14002/api/s3/objects/tags/version?bucketName=data&objectKey=archive/report.pdf&versionId=your-version-id"
     *
     * @param bucketName 存储桶名称，可为空
     * @param objectKey  对象 Key
     * @param versionId  对象版本 ID
     * @return 对象标签
     */
    @GetMapping("/version")
    public Dict getTagsByVersion(@RequestParam(required = false) String bucketName,
                                 @RequestParam String objectKey,
                                 @RequestParam String versionId) {
        String resolvedBucketName = s3Service.resolveBucketName(bucketName);
        Map<String, String> tags = s3Service.getTags(resolvedBucketName, objectKey, versionId);

        log.info("获取 S3 指定版本对象标签成功，bucketName={}，objectKey={}，versionId={}，tagSize={}",
                resolvedBucketName, objectKey, versionId, tags.size());

        return Dict.create()
                .set("bucketName", resolvedBucketName)
                .set("objectKey", objectKey)
                .set("versionId", versionId)
                .set("tags", tags);
    }

    /**
     * 写入对象标签
     * <p>
     * bucketName 不传时使用默认存储桶。
     * tagsJson 为 JSON 对象字符串。
     * 该接口会完整覆盖原对象标签，不是增量合并。
     * tagsJson 为空或 {} 时，会清空对象标签。
     * <p>
     * curl 使用示例：
     * curl -X PUT "http://localhost:14002/api/s3/objects/tags" \
     * -d "objectKey=upload/2026/04/28/test.txt" \
     * -d "tagsJson={\"env\":\"dev\",\"type\":\"text\"}"
     * <p>
     * curl 使用示例：
     * curl -X PUT "http://localhost:14002/api/s3/objects/tags" \
     * -d "bucketName=data" \
     * -d "objectKey=upload/2026/04/28/test.txt" \
     * -d "tagsJson={\"env\":\"prod\",\"owner\":\"ateng\"}"
     * <p>
     * curl 使用示例：
     * curl -X PUT "http://localhost:14002/api/s3/objects/tags" \
     * -d "bucketName=data" \
     * -d "objectKey=upload/2026/04/28/test.txt" \
     * -d "tagsJson={}"
     *
     * @param bucketName 存储桶名称，可为空
     * @param objectKey  对象 Key
     * @param tagsJson   标签 JSON 对象字符串，可为空或 {}
     * @return 写入结果
     */
    @PutMapping(consumes = MediaType.APPLICATION_FORM_URLENCODED_VALUE)
    public Dict putTags(@RequestParam(required = false) String bucketName,
                        @RequestParam String objectKey,
                        @RequestParam(required = false) String tagsJson) {
        String resolvedBucketName = s3Service.resolveBucketName(bucketName);
        Map<String, String> tags = parseStringMap(tagsJson);

        s3Service.putTags(resolvedBucketName, objectKey, tags);

        log.info("写入 S3 对象标签成功，bucketName={}，objectKey={}，tagSize={}",
                resolvedBucketName, objectKey, tags.size());

        return Dict.create()
                .set("bucketName", resolvedBucketName)
                .set("objectKey", objectKey)
                .set("tags", tags)
                .set("updated", true);
    }

    /**
     * 写入指定版本对象标签
     * <p>
     * bucketName 不传时使用默认存储桶。
     * tagsJson 为 JSON 对象字符串。
     * 该接口会完整覆盖指定版本对象标签，不是增量合并。
     * tagsJson 为空或 {} 时，会清空指定版本对象标签。
     * <p>
     * curl 使用示例：
     * curl -X PUT "http://localhost:14002/api/s3/objects/tags/version" \
     * -d "objectKey=archive/report.pdf" \
     * -d "versionId=your-version-id" \
     * -d "tagsJson={\"status\":\"archived\",\"year\":\"2026\"}"
     * <p>
     * curl 使用示例：
     * curl -X PUT "http://localhost:14002/api/s3/objects/tags/version" \
     * -d "bucketName=data" \
     * -d "objectKey=archive/report.pdf" \
     * -d "versionId=your-version-id" \
     * -d "tagsJson={\"status\":\"verified\"}"
     *
     * @param bucketName 存储桶名称，可为空
     * @param objectKey  对象 Key
     * @param versionId  对象版本 ID
     * @param tagsJson   标签 JSON 对象字符串，可为空或 {}
     * @return 写入结果
     */
    @PutMapping(value = "/version", consumes = MediaType.APPLICATION_FORM_URLENCODED_VALUE)
    public Dict putTagsByVersion(@RequestParam(required = false) String bucketName,
                                 @RequestParam String objectKey,
                                 @RequestParam String versionId,
                                 @RequestParam(required = false) String tagsJson) {
        String resolvedBucketName = s3Service.resolveBucketName(bucketName);
        Map<String, String> tags = parseStringMap(tagsJson);

        s3Service.putTags(resolvedBucketName, objectKey, versionId, tags);

        log.info("写入 S3 指定版本对象标签成功，bucketName={}，objectKey={}，versionId={}，tagSize={}",
                resolvedBucketName, objectKey, versionId, tags.size());

        return Dict.create()
                .set("bucketName", resolvedBucketName)
                .set("objectKey", objectKey)
                .set("versionId", versionId)
                .set("tags", tags)
                .set("updated", true);
    }

    /**
     * 删除对象标签
     * <p>
     * bucketName 不传时使用默认存储桶。
     * <p>
     * curl 使用示例：
     * curl -X DELETE "http://localhost:14002/api/s3/objects/tags?objectKey=upload/2026/04/28/test.txt"
     * <p>
     * curl 使用示例：
     * curl -X DELETE "http://localhost:14002/api/s3/objects/tags?bucketName=data&objectKey=upload/2026/04/28/test.txt"
     *
     * @param bucketName 存储桶名称，可为空
     * @param objectKey  对象 Key
     * @return 删除结果
     */
    @DeleteMapping
    public Dict deleteTags(@RequestParam(required = false) String bucketName,
                           @RequestParam String objectKey) {
        String resolvedBucketName = s3Service.resolveBucketName(bucketName);
        s3Service.deleteTags(resolvedBucketName, objectKey);

        log.info("删除 S3 对象标签成功，bucketName={}，objectKey={}", resolvedBucketName, objectKey);

        return Dict.create()
                .set("bucketName", resolvedBucketName)
                .set("objectKey", objectKey)
                .set("deleted", true);
    }

    /**
     * 删除指定版本对象标签
     * <p>
     * bucketName 不传时使用默认存储桶。
     * <p>
     * curl 使用示例：
     * curl -X DELETE "http://localhost:14002/api/s3/objects/tags/version?objectKey=archive/report.pdf&versionId=your-version-id"
     * <p>
     * curl 使用示例：
     * curl -X DELETE "http://localhost:14002/api/s3/objects/tags/version?bucketName=data&objectKey=archive/report.pdf&versionId=your-version-id"
     *
     * @param bucketName 存储桶名称，可为空
     * @param objectKey  对象 Key
     * @param versionId  对象版本 ID
     * @return 删除结果
     */
    @DeleteMapping("/version")
    public Dict deleteTagsByVersion(@RequestParam(required = false) String bucketName,
                                    @RequestParam String objectKey,
                                    @RequestParam String versionId) {
        String resolvedBucketName = s3Service.resolveBucketName(bucketName);
        s3Service.deleteTags(resolvedBucketName, objectKey, versionId);

        log.info("删除 S3 指定版本对象标签成功，bucketName={}，objectKey={}，versionId={}",
                resolvedBucketName, objectKey, versionId);

        return Dict.create()
                .set("bucketName", resolvedBucketName)
                .set("objectKey", objectKey)
                .set("versionId", versionId)
                .set("deleted", true);
    }

    /**
     * 解析 JSON 字符串为 Map
     *
     * @param json JSON 字符串
     * @return 字符串 Map
     */
    private Map<String, String> parseStringMap(String json) {
        if (StrUtil.isBlank(json)) {
            return Map.of();
        }

        Assert.isTrue(JSONUtil.isTypeJSONObject(json), "参数必须是 JSON 对象字符串：{}", json);

        JSONObject jsonObject = JSONUtil.parseObj(json);
        if (jsonObject.isEmpty()) {
            return Map.of();
        }

        Map<String, String> map = new LinkedHashMap<>();

        jsonObject.forEach((key, value) -> {
            if (StrUtil.isBlank(key) || value == null) {
                return;
            }

            map.put(StrUtil.trim(key), StrUtil.toString(value));
        });

        return map;
    }
}
```

### 访问控制与存储属性

```java
package io.github.atengk.aws.s3.controller;

import cn.hutool.core.lang.Dict;
import cn.hutool.core.util.StrUtil;
import io.github.atengk.aws.s3.model.enums.S3ObjectAcl;
import io.github.atengk.aws.s3.model.enums.S3StorageClass;
import io.github.atengk.aws.s3.service.S3Service;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.Arrays;
import java.util.stream.Collectors;

/**
 * S3 对象访问控制与存储属性接口
 *
 * @author Ateng
 * @since 2026-04-28
 */
@Slf4j
@Validated
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/s3/objects/attributes")
public class S3ObjectAttributeController {

    private final S3Service s3Service;

    /**
     * 设置对象 ACL
     * <p>
     * bucketName 不传时使用默认存储桶。
     * 常用 acl：PRIVATE、PUBLIC_READ、BUCKET_OWNER_FULL_CONTROL。
     * <p>
     * curl 使用示例：
     * curl -X PUT "http://localhost:14002/api/s3/objects/attributes/acl" \
     * -d "objectKey=upload/2026/04/28/test.txt" \
     * -d "acl=PRIVATE"
     * <p>
     * curl 使用示例：
     * curl -X PUT "http://localhost:14002/api/s3/objects/attributes/acl" \
     * -d "bucketName=data" \
     * -d "objectKey=upload/2026/04/28/test.txt" \
     * -d "acl=PUBLIC_READ"
     *
     * @param bucketName 存储桶名称，可为空
     * @param objectKey  对象 Key
     * @param acl        对象 ACL
     * @return 设置结果
     */
    @PutMapping(value = "/acl", consumes = MediaType.APPLICATION_FORM_URLENCODED_VALUE)
    public Dict setObjectAcl(@RequestParam(required = false) String bucketName,
                             @RequestParam String objectKey,
                             @RequestParam String acl) {
        String resolvedBucketName = s3Service.resolveBucketName(bucketName);
        S3ObjectAcl resolvedAcl = parseEnum(S3ObjectAcl.class, acl, "S3 对象 ACL 不合法");

        s3Service.setObjectAcl(resolvedBucketName, objectKey, resolvedAcl);

        log.info("设置 S3 对象 ACL 成功，bucketName={}，objectKey={}，acl={}",
                resolvedBucketName, objectKey, resolvedAcl);

        return Dict.create()
                .set("bucketName", resolvedBucketName)
                .set("objectKey", objectKey)
                .set("acl", resolvedAcl)
                .set("updated", true);
    }

    /**
     * 修改对象存储类型
     * <p>
     * bucketName 不传时使用默认存储桶。
     * 常用 storageClass：STANDARD、INTELLIGENT_TIERING、STANDARD_IA、ONEZONE_IA、GLACIER、DEEP_ARCHIVE。
     * <p>
     * curl 使用示例：
     * curl -X PUT "http://localhost:14002/api/s3/objects/attributes/storage-class" \
     * -d "objectKey=upload/2026/04/28/test.txt" \
     * -d "storageClass=STANDARD_IA"
     * <p>
     * curl 使用示例：
     * curl -X PUT "http://localhost:14002/api/s3/objects/attributes/storage-class" \
     * -d "bucketName=data" \
     * -d "objectKey=archive/report.pdf" \
     * -d "storageClass=GLACIER"
     *
     * @param bucketName   存储桶名称，可为空
     * @param objectKey    对象 Key
     * @param storageClass 存储类型
     * @return 修改结果
     */
    @PutMapping(value = "/storage-class", consumes = MediaType.APPLICATION_FORM_URLENCODED_VALUE)
    public Dict changeStorageClass(@RequestParam(required = false) String bucketName,
                                   @RequestParam String objectKey,
                                   @RequestParam String storageClass) {
        String resolvedBucketName = s3Service.resolveBucketName(bucketName);
        S3StorageClass resolvedStorageClass = parseEnum(
                S3StorageClass.class,
                storageClass,
                "S3 存储类型不合法"
        );

        s3Service.changeStorageClass(resolvedBucketName, objectKey, resolvedStorageClass);

        log.info("修改 S3 对象存储类型成功，bucketName={}，objectKey={}，storageClass={}",
                resolvedBucketName, objectKey, resolvedStorageClass);

        return Dict.create()
                .set("bucketName", resolvedBucketName)
                .set("objectKey", objectKey)
                .set("storageClass", resolvedStorageClass)
                .set("updated", true);
    }

    /**
     * 发起归档对象恢复任务
     * <p>
     * bucketName 不传时使用默认存储桶。
     * days 表示恢复后的临时副本保留天数，不传时由 Service 使用默认值。
     * 适用于 GLACIER、DEEP_ARCHIVE 等归档存储类型对象。
     * <p>
     * curl 使用示例：
     * curl -X POST "http://localhost:14002/api/s3/objects/attributes/restore" \
     * -d "objectKey=archive/report.pdf" \
     * -d "days=7"
     * <p>
     * curl 使用示例：
     * curl -X POST "http://localhost:14002/api/s3/objects/attributes/restore" \
     * -d "bucketName=data" \
     * -d "objectKey=archive/report.pdf" \
     * -d "days=3"
     *
     * @param bucketName 存储桶名称，可为空
     * @param objectKey  对象 Key
     * @param days       恢复副本保留天数，可为空
     * @return 恢复任务发起结果
     */
    @PostMapping(value = "/restore", consumes = MediaType.APPLICATION_FORM_URLENCODED_VALUE)
    public Dict restoreArchiveObject(@RequestParam(required = false) String bucketName,
                                     @RequestParam String objectKey,
                                     @RequestParam(required = false) Integer days) {
        String resolvedBucketName = s3Service.resolveBucketName(bucketName);

        s3Service.restoreArchiveObject(resolvedBucketName, objectKey, days);

        log.info("发起 S3 归档对象恢复任务成功，bucketName={}，objectKey={}，days={}",
                resolvedBucketName,
                objectKey,
                days == null ? "默认值" : days);

        return Dict.create()
                .set("bucketName", resolvedBucketName)
                .set("objectKey", objectKey)
                .set("days", days)
                .set("restored", true);
    }

    /**
     * 解析枚举
     *
     * @param enumClass    枚举类型
     * @param value        枚举值
     * @param errorMessage 错误信息
     * @param <E>          枚举泛型
     * @return 枚举值
     */
    private <E extends Enum<E>> E parseEnum(Class<E> enumClass, String value, String errorMessage) {
        if (StrUtil.isBlank(value)) {
            return null;
        }

        try {
            return Enum.valueOf(enumClass, StrUtil.trim(value).toUpperCase());
        } catch (IllegalArgumentException e) {
            String enumValues = Arrays.stream(enumClass.getEnumConstants())
                    .map(Enum::name)
                    .collect(Collectors.joining(","));

            throw new IllegalArgumentException(errorMessage + "，value=" + value + "，可选值：" + enumValues, e);
        }
    }
}
```

### 分片上传

```java
package io.github.atengk.aws.s3.controller;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.lang.Assert;
import cn.hutool.core.lang.Dict;
import cn.hutool.core.util.StrUtil;
import cn.hutool.json.JSONArray;
import cn.hutool.json.JSONObject;
import cn.hutool.json.JSONUtil;
import io.github.atengk.aws.s3.model.enums.S3ObjectAcl;
import io.github.atengk.aws.s3.model.enums.S3ServerSideEncryption;
import io.github.atengk.aws.s3.model.enums.S3StorageClass;
import io.github.atengk.aws.s3.model.info.S3MultipartUploadInfo;
import io.github.atengk.aws.s3.model.info.S3MultipartUploadPartInfo;
import io.github.atengk.aws.s3.model.request.S3MultipartUploadCompleteRequest;
import io.github.atengk.aws.s3.model.request.S3MultipartUploadInitRequest;
import io.github.atengk.aws.s3.model.request.S3MultipartUploadPartRequest;
import io.github.atengk.aws.s3.model.result.S3MultipartUploadInitResult;
import io.github.atengk.aws.s3.model.result.S3MultipartUploadPartResult;
import io.github.atengk.aws.s3.model.result.S3UploadResult;
import io.github.atengk.aws.s3.service.S3Service;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.util.*;
import java.util.stream.Collectors;

/**
 * S3 分片上传接口
 *
 * @author Ateng
 * @since 2026-04-28
 */
@Slf4j
@Validated
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/s3/multipart-uploads")
public class S3MultipartUploadController {

    private final S3Service s3Service;

    /**
     * 初始化分片上传
     * <p>
     * bucketName 不传时使用默认存储桶。
     * metadataJson 和 tagsJson 为 JSON 对象字符串。
     * <p>
     * curl 使用示例：
     * curl -X POST "http://localhost:14002/api/s3/multipart-uploads/init" \
     * -d "bucketName=data" \
     * -d "objectKey=large/video.mp4" \
     * -d "contentType=video/mp4" \
     * -d "metadataJson={\"biz-type\":\"video\",\"owner\":\"ateng\"}" \
     * -d "tagsJson={\"scene\":\"multipart\",\"env\":\"dev\"}" \
     * -d "acl=PRIVATE" \
     * -d "storageClass=STANDARD"
     * <p>
     * curl 使用示例：
     * curl -X POST "http://localhost:14002/api/s3/multipart-uploads/init" \
     * -d "objectKey=secure/archive.zip" \
     * -d "contentType=application/zip" \
     * -d "serverSideEncryption=AWS_KMS" \
     * -d "kmsKeyId=your-kms-key-id"
     *
     * @param bucketName           存储桶名称，可为空
     * @param objectKey            对象 Key
     * @param contentType          Content-Type，可为空
     * @param metadataJson         元数据 JSON，可为空
     * @param tagsJson             标签 JSON，可为空
     * @param acl                  对象 ACL，可为空
     * @param storageClass         存储类型，可为空
     * @param serverSideEncryption 服务端加密方式，可为空
     * @param kmsKeyId             KMS Key ID，可为空
     * @return 分片上传初始化结果
     */
    @PostMapping(value = "/init", consumes = MediaType.APPLICATION_FORM_URLENCODED_VALUE)
    public S3MultipartUploadInitResult initMultipartUpload(@RequestParam(required = false) String bucketName,
                                                           @RequestParam String objectKey,
                                                           @RequestParam(required = false) String contentType,
                                                           @RequestParam(required = false) String metadataJson,
                                                           @RequestParam(required = false) String tagsJson,
                                                           @RequestParam(required = false) String acl,
                                                           @RequestParam(required = false) String storageClass,
                                                           @RequestParam(required = false) String serverSideEncryption,
                                                           @RequestParam(required = false) String kmsKeyId) {
        String resolvedBucketName = s3Service.resolveBucketName(bucketName);

        S3MultipartUploadInitRequest request = new S3MultipartUploadInitRequest(
                resolvedBucketName,
                objectKey,
                blankToNull(contentType),
                parseStringMap(metadataJson),
                parseStringMap(tagsJson),
                parseEnum(S3ObjectAcl.class, acl, "S3 对象 ACL 不合法"),
                parseEnum(S3StorageClass.class, storageClass, "S3 存储类型不合法"),
                parseEnum(S3ServerSideEncryption.class, serverSideEncryption, "S3 服务端加密方式不合法"),
                blankToNull(kmsKeyId)
        );

        S3MultipartUploadInitResult result = s3Service.initMultipartUpload(request);

        log.info("初始化 S3 分片上传成功，bucketName={}，objectKey={}，uploadId={}",
                result.bucketName(), result.objectKey(), result.uploadId());

        return result;
    }

    /**
     * 上传单个分片
     * <p>
     * bucketName 不传时使用默认存储桶。
     * partNumber 从 1 开始，最大 10000。
     * <p>
     * curl 使用示例：
     * curl -X POST "http://localhost:14002/api/s3/multipart-uploads/part" \
     * -F "bucketName=data" \
     * -F "objectKey=large/video.mp4" \
     * -F "uploadId=your-upload-id" \
     * -F "partNumber=1" \
     * -F "file=@/data/parts/video.part001"
     * <p>
     * curl 使用示例：
     * curl -X POST "http://localhost:14002/api/s3/multipart-uploads/part" \
     * -F "objectKey=large/video.mp4" \
     * -F "uploadId=your-upload-id" \
     * -F "partNumber=2" \
     * -F "file=@/data/parts/video.part002"
     *
     * @param bucketName 存储桶名称，可为空
     * @param objectKey  对象 Key
     * @param uploadId   上传 ID
     * @param partNumber 分片编号
     * @param file       分片文件
     * @return 分片上传结果
     */
    @PostMapping(value = "/part", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public S3MultipartUploadPartResult uploadPart(@RequestParam(required = false) String bucketName,
                                                  @RequestParam String objectKey,
                                                  @RequestParam String uploadId,
                                                  @RequestParam Integer partNumber,
                                                  @RequestParam MultipartFile file) {
        Assert.notNull(file, "S3 分片文件不能为空");

        String resolvedBucketName = s3Service.resolveBucketName(bucketName);

        try (InputStream inputStream = file.getInputStream()) {
            S3MultipartUploadPartRequest request = new S3MultipartUploadPartRequest(
                    resolvedBucketName,
                    objectKey,
                    uploadId,
                    partNumber,
                    inputStream,
                    file.getSize()
            );

            S3MultipartUploadPartResult result = s3Service.uploadPart(request);

            log.info("上传 S3 分片成功，bucketName={}，objectKey={}，uploadId={}，partNumber={}，size={}，eTag={}",
                    resolvedBucketName, objectKey, uploadId, result.partNumber(), result.size(), result.eTag());

            return result;
        } catch (IOException e) {
            log.error("读取 S3 分片文件流失败，bucketName={}，objectKey={}，uploadId={}，partNumber={}，filename={}",
                    resolvedBucketName, objectKey, uploadId, partNumber, file.getOriginalFilename(), e);
            throw new UncheckedIOException("读取 S3 分片文件流失败", e);
        }
    }

    /**
     * 完成分片上传
     * <p>
     * bucketName 不传时使用默认存储桶。
     * partsJson 为 JSON 数组字符串，数组元素必须包含 partNumber 和 eTag，size 可选。
     * <p>
     * curl 使用示例：
     * curl -X POST "http://localhost:14002/api/s3/multipart-uploads/complete" \
     * -d "bucketName=data" \
     * -d "objectKey=large/video.mp4" \
     * -d "uploadId=your-upload-id" \
     * -d "partsJson=[{\"partNumber\":1,\"eTag\":\"etag-1\",\"size\":5242880},{\"partNumber\":2,\"eTag\":\"etag-2\",\"size\":5242880}]"
     * <p>
     * curl 使用示例：
     * curl -X POST "http://localhost:14002/api/s3/multipart-uploads/complete" \
     * -d "objectKey=large/video.mp4" \
     * -d "uploadId=your-upload-id" \
     * -d "partsJson=[{\"partNumber\":1,\"eTag\":\"etag-1\"},{\"partNumber\":2,\"eTag\":\"etag-2\"}]"
     *
     * @param bucketName 存储桶名称，可为空
     * @param objectKey  对象 Key
     * @param uploadId   上传 ID
     * @param partsJson  分片结果 JSON 数组
     * @return 上传结果
     */
    @PostMapping(value = "/complete", consumes = MediaType.APPLICATION_FORM_URLENCODED_VALUE)
    public S3UploadResult completeMultipartUpload(@RequestParam(required = false) String bucketName,
                                                  @RequestParam String objectKey,
                                                  @RequestParam String uploadId,
                                                  @RequestParam String partsJson) {
        String resolvedBucketName = s3Service.resolveBucketName(bucketName);
        List<S3MultipartUploadPartResult> parts = parseMultipartUploadParts(partsJson);

        S3MultipartUploadCompleteRequest request = new S3MultipartUploadCompleteRequest(
                resolvedBucketName,
                objectKey,
                uploadId,
                parts
        );

        S3UploadResult result = s3Service.completeMultipartUpload(request);

        log.info("完成 S3 分片上传成功，bucketName={}，objectKey={}，uploadId={}，partCount={}，size={}，eTag={}",
                result.bucketName(), result.objectKey(), uploadId, parts.size(), result.size(), result.eTag());

        return result;
    }

    /**
     * 终止分片上传
     * <p>
     * bucketName 不传时使用默认存储桶。
     * 终止后，已上传但未完成合并的分片会被释放。
     * <p>
     * curl 使用示例：
     * curl -X DELETE "http://localhost:14002/api/s3/multipart-uploads?bucketName=data&objectKey=large/video.mp4&uploadId=your-upload-id"
     * <p>
     * curl 使用示例：
     * curl -X DELETE "http://localhost:14002/api/s3/multipart-uploads?objectKey=large/video.mp4&uploadId=your-upload-id"
     *
     * @param bucketName 存储桶名称，可为空
     * @param objectKey  对象 Key
     * @param uploadId   上传 ID
     * @return 终止结果
     */
    @DeleteMapping
    public Dict abortMultipartUpload(@RequestParam(required = false) String bucketName,
                                     @RequestParam String objectKey,
                                     @RequestParam String uploadId) {
        String resolvedBucketName = s3Service.resolveBucketName(bucketName);

        s3Service.abortMultipartUpload(resolvedBucketName, objectKey, uploadId);

        log.info("终止 S3 分片上传成功，bucketName={}，objectKey={}，uploadId={}",
                resolvedBucketName, objectKey, uploadId);

        return Dict.create()
                .set("bucketName", resolvedBucketName)
                .set("objectKey", objectKey)
                .set("uploadId", uploadId)
                .set("aborted", true);
    }

    /**
     * 查询未完成的分片上传任务
     * <p>
     * bucketName 不传时使用默认存储桶。
     * prefix 不传时查询整个存储桶下未完成的分片上传任务。
     * <p>
     * curl 使用示例：
     * curl -X GET "http://localhost:14002/api/s3/multipart-uploads?bucketName=data&prefix=large/"
     * <p>
     * curl 使用示例：
     * curl -X GET "http://localhost:14002/api/s3/multipart-uploads?prefix=large/"
     *
     * @param bucketName 存储桶名称，可为空
     * @param prefix     对象 Key 前缀，可为空
     * @return 未完成分片上传任务列表
     */
    @GetMapping
    public List<S3MultipartUploadInfo> listMultipartUploads(@RequestParam(required = false) String bucketName,
                                                            @RequestParam(required = false) String prefix) {
        String resolvedBucketName = s3Service.resolveBucketName(bucketName);
        List<S3MultipartUploadInfo> uploads = s3Service.listMultipartUploads(resolvedBucketName, prefix);

        log.info("查询 S3 未完成分片上传任务成功，bucketName={}，prefix={}，count={}",
                resolvedBucketName, StrUtil.blankToDefault(prefix, "全部"), uploads.size());

        return uploads;
    }

    /**
     * 查询指定分片上传任务已上传的分片列表
     * <p>
     * bucketName 不传时使用默认存储桶。
     * <p>
     * curl 使用示例：
     * curl -X GET "http://localhost:14002/api/s3/multipart-uploads/parts?bucketName=data&objectKey=large/video.mp4&uploadId=your-upload-id"
     * <p>
     * curl 使用示例：
     * curl -X GET "http://localhost:14002/api/s3/multipart-uploads/parts?objectKey=large/video.mp4&uploadId=your-upload-id"
     *
     * @param bucketName 存储桶名称，可为空
     * @param objectKey  对象 Key
     * @param uploadId   上传 ID
     * @return 已上传分片列表
     */
    @GetMapping("/parts")
    public List<S3MultipartUploadPartInfo> listMultipartUploadParts(@RequestParam(required = false) String bucketName,
                                                                    @RequestParam String objectKey,
                                                                    @RequestParam String uploadId) {
        String resolvedBucketName = s3Service.resolveBucketName(bucketName);
        List<S3MultipartUploadPartInfo> parts = s3Service.listMultipartUploadParts(
                resolvedBucketName,
                objectKey,
                uploadId
        );

        log.info("查询 S3 已上传分片列表成功，bucketName={}，objectKey={}，uploadId={}，count={}",
                resolvedBucketName, objectKey, uploadId, parts.size());

        return parts;
    }

    /**
     * 解析 JSON 字符串为 Map
     *
     * @param json JSON 字符串
     * @return 字符串 Map
     */
    private Map<String, String> parseStringMap(String json) {
        if (StrUtil.isBlank(json)) {
            return Map.of();
        }

        Assert.isTrue(JSONUtil.isTypeJSONObject(json), "参数必须是 JSON 对象字符串：{}", json);

        JSONObject jsonObject = JSONUtil.parseObj(json);
        if (jsonObject.isEmpty()) {
            return Map.of();
        }

        Map<String, String> map = new LinkedHashMap<>();

        jsonObject.forEach((key, value) -> {
            if (StrUtil.isBlank(key) || value == null) {
                return;
            }

            map.put(StrUtil.trim(key), StrUtil.toString(value));
        });

        return map;
    }

    /**
     * 解析完成分片上传的分片列表
     *
     * @param partsJson 分片 JSON 数组字符串
     * @return 分片上传结果列表
     */
    private List<S3MultipartUploadPartResult> parseMultipartUploadParts(String partsJson) {
        Assert.notBlank(partsJson, "S3 完成分片上传 partsJson 不能为空");
        Assert.isTrue(JSONUtil.isTypeJSONArray(partsJson), "partsJson 必须是 JSON 数组字符串");

        JSONArray jsonArray = JSONUtil.parseArray(partsJson);
        Assert.isTrue(CollUtil.isNotEmpty(jsonArray), "S3 完成分片上传分片列表不能为空");

        List<S3MultipartUploadPartResult> parts = new ArrayList<>(jsonArray.size());

        for (int i = 0; i < jsonArray.size(); i++) {
            Object item = jsonArray.get(i);
            JSONObject jsonObject = JSONUtil.parseObj(item);

            Integer partNumber = jsonObject.getInt("partNumber");
            String eTag = jsonObject.getStr("eTag");
            Long size = jsonObject.getLong("size");

            Assert.notNull(partNumber, "S3 分片 partNumber 不能为空，index={}", i);
            Assert.notBlank(eTag, "S3 分片 eTag 不能为空，index={}", i);

            parts.add(new S3MultipartUploadPartResult(
                    partNumber,
                    eTag,
                    size
            ));
        }

        return parts;
    }

    /**
     * 解析枚举
     *
     * @param enumClass    枚举类型
     * @param value        枚举值
     * @param errorMessage 错误信息
     * @param <E>          枚举泛型
     * @return 枚举值
     */
    private <E extends Enum<E>> E parseEnum(Class<E> enumClass, String value, String errorMessage) {
        if (StrUtil.isBlank(value)) {
            return null;
        }

        try {
            return Enum.valueOf(enumClass, StrUtil.trim(value).toUpperCase());
        } catch (IllegalArgumentException e) {
            String enumValues = Arrays.stream(enumClass.getEnumConstants())
                    .map(Enum::name)
                    .collect(Collectors.joining(","));

            throw new IllegalArgumentException(errorMessage + "，value=" + value + "，可选值：" + enumValues, e);
        }
    }

    /**
     * 空白字符串转 null，并去除前后空格
     *
     * @param value 字符串
     * @return 非空白字符串或 null
     */
    private String blankToNull(String value) {
        return StrUtil.isBlank(value) ? null : StrUtil.trim(value);
    }
}
```

