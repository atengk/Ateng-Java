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