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

```

    enum S3PresignedUrlMethod {
        GET,
        PUT,
        HEAD,
        DELETE
    }

    enum S3ObjectAcl {
        PRIVATE,
        PUBLIC_READ,
        PUBLIC_READ_WRITE,
        AUTHENTICATED_READ,
        BUCKET_OWNER_READ,
        BUCKET_OWNER_FULL_CONTROL
    }

    enum S3StorageClass {
        STANDARD,
        INTELLIGENT_TIERING,
        STANDARD_IA,
        ONEZONE_IA,
        GLACIER,
        DEEP_ARCHIVE,
        REDUCED_REDUNDANCY
    }

    enum S3ServerSideEncryption {
        AES256,
        AWS_KMS
    }

    record S3UploadRequest(
            String bucketName,
            String objectKey,
            InputStream inputStream,
            Long contentLength,
            String contentType,
            Map<String, String> metadata,
            Map<String, String> tags,
            S3ObjectAcl acl,
            S3StorageClass storageClass,
            S3ServerSideEncryption serverSideEncryption,
            String kmsKeyId
    ) {
    }

    record S3UploadResult(
            String bucketName,
            String objectKey,
            String originalFilename,
            String contentType,
            Long size,
            String eTag,
            String versionId,
            String url,
            Instant uploadTime
    ) {
    }

    record S3DownloadRequest(
            String bucketName,
            String objectKey,
            String versionId,
            Long rangeStart,
            Long rangeEnd
    ) {
    }

    record S3DownloadResult(
            String bucketName,
            String objectKey,
            String filename,
            String contentType,
            Long contentLength,
            Map<String, String> metadata,
            InputStream inputStream
    ) {
    }

    record S3CopyRequest(
            String sourceBucketName,
            String sourceKey,
            String sourceVersionId,
            String targetBucketName,
            String targetKey,
            Map<String, String> metadata,
            Map<String, String> tags,
            Boolean replaceMetadata,
            Boolean replaceTags,
            S3ObjectAcl acl,
            S3StorageClass storageClass,
            S3ServerSideEncryption serverSideEncryption,
            String kmsKeyId
    ) {
    }

    record S3CopyResult(
            String sourceBucketName,
            String sourceKey,
            String targetBucketName,
            String targetKey,
            String eTag,
            String versionId,
            Instant copyTime
    ) {
    }

    record S3MoveRequest(
            String sourceBucketName,
            String sourceKey,
            String targetBucketName,
            String targetKey,
            Boolean overwrite
    ) {
    }

    record S3MoveResult(
            String sourceBucketName,
            String sourceKey,
            String targetBucketName,
            String targetKey,
            Boolean deletedSource,
            Instant moveTime
    ) {
    }

    record S3ListRequest(
            String bucketName,
            String prefix,
            String delimiter,
            Integer maxKeys,
            String continuationToken,
            Boolean recursive
    ) {
    }

    record S3ObjectPage(
            String bucketName,
            String prefix,
            List<S3ObjectInfo> objects,
            List<String> commonPrefixes,
            Boolean truncated,
            String nextContinuationToken,
            Integer maxKeys
    ) {
    }

    record S3ObjectInfo(
            String bucketName,
            String objectKey,
            String filename,
            String contentType,
            Long size,
            String eTag,
            String versionId,
            S3StorageClass storageClass,
            Instant lastModified,
            Map<String, String> metadata,
            Map<String, String> tags
    ) {
    }

    record S3ObjectVersionInfo(
            String bucketName,
            String objectKey,
            String versionId,
            Boolean latest,
            Boolean deleteMarker,
            Long size,
            String eTag,
            Instant lastModified
    ) {
    }

    record S3PresignedUrlRequest(
            String bucketName,
            String objectKey,
            S3PresignedUrlMethod method,
            Duration expire,
            String contentType,
            String filename,
            Map<String, String> requestHeaders,
            Map<String, String> responseHeaders
    ) {
    }

    record S3PresignedUrlResult(
            String bucketName,
            String objectKey,
            S3PresignedUrlMethod method,
            URI url,
            Instant expireTime
    ) {
    }

    record S3MetadataRequest(
            String bucketName,
            String objectKey,
            Map<String, String> metadata,
            Boolean preserveTags,
            Boolean preserveAcl,
            S3StorageClass storageClass
    ) {
    }

    record S3MultipartUploadInitRequest(
            String bucketName,
            String objectKey,
            String contentType,
            Map<String, String> metadata,
            Map<String, String> tags,
            S3ObjectAcl acl,
            S3StorageClass storageClass,
            S3ServerSideEncryption serverSideEncryption,
            String kmsKeyId
    ) {
    }

    record S3MultipartUploadInitResult(
            String bucketName,
            String objectKey,
            String uploadId
    ) {
    }

    record S3MultipartUploadPartRequest(
            String bucketName,
            String objectKey,
            String uploadId,
            Integer partNumber,
            InputStream inputStream,
            Long contentLength
    ) {
    }

    record S3MultipartUploadPartResult(
            Integer partNumber,
            String eTag,
            Long size
    ) {
    }

    record S3MultipartUploadCompleteRequest(
            String bucketName,
            String objectKey,
            String uploadId,
            List<S3MultipartUploadPartResult> parts
    ) {
    }

    record S3MultipartUploadInfo(
            String bucketName,
            String objectKey,
            String uploadId,
            Instant initiated
    ) {
    }

    record S3MultipartUploadPartInfo(
            Integer partNumber,
            String eTag,
            Long size,
            Instant lastModified
    ) {
    }

    record S3DeleteResult(
            String bucketName,
            List<String> deletedObjectKeys,
            List<S3DeleteError> errors,
            Long deletedCount,
            Long errorCount
    ) {
    }

    record S3DeleteError(
            String objectKey,
            String versionId,
            String code,
            String message
    ) {
    }
```



## 创建服务接口

```java

```



## 创建服务实现

```java

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

