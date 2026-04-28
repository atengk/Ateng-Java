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