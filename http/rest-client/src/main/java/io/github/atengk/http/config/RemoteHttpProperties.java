package io.github.atengk.http.config;

import cn.hutool.core.map.MapUtil;
import cn.hutool.core.util.ObjectUtil;
import io.github.atengk.http.constant.RemoteHttpConstant;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

import java.time.Duration;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 远程 HTTP 配置属性
 *
 * @author Ateng
 * @since 2026-04-29
 */
@Data
@Validated
@ConfigurationProperties(prefix = "remote.http")
public class RemoteHttpProperties {

    @Valid
    private LoggingConfig logging = new LoggingConfig();

    @Valid
    private TraceConfig trace = new TraceConfig();

    @Valid
    private ErrorHandlingConfig errorHandling = new ErrorHandlingConfig();

    @Valid
    private ClientConfig defaultConfig = new ClientConfig();

    @Valid
    private Map<String, ClientConfig> clients = new LinkedHashMap<>();

    /**
     * 获取合并后的客户端配置
     *
     * @param clientName 客户端名称
     * @return 客户端配置
     */
    public ClientConfig getMergedClient(String clientName) {
        ClientConfig source = clients.get(clientName);
        if (ObjectUtil.isNull(source)) {
            return defaultConfig;
        }

        ClientConfig merged = new ClientConfig();
        merged.setBaseUrl(ObjectUtil.defaultIfNull(source.getBaseUrl(), defaultConfig.getBaseUrl()));
        merged.setConnectTimeout(ObjectUtil.defaultIfNull(source.getConnectTimeout(), defaultConfig.getConnectTimeout()));
        merged.setConnectionRequestTimeout(ObjectUtil.defaultIfNull(source.getConnectionRequestTimeout(), defaultConfig.getConnectionRequestTimeout()));
        merged.setResponseTimeout(ObjectUtil.defaultIfNull(source.getResponseTimeout(), defaultConfig.getResponseTimeout()));
        merged.setMaxIdleTime(ObjectUtil.defaultIfNull(source.getMaxIdleTime(), defaultConfig.getMaxIdleTime()));
        merged.setMaxConnTotal(ObjectUtil.defaultIfNull(source.getMaxConnTotal(), defaultConfig.getMaxConnTotal()));
        merged.setMaxConnPerRoute(ObjectUtil.defaultIfNull(source.getMaxConnPerRoute(), defaultConfig.getMaxConnPerRoute()));

        Map<String, String> headers = new LinkedHashMap<>();
        if (MapUtil.isNotEmpty(defaultConfig.getDefaultHeaders())) {
            headers.putAll(defaultConfig.getDefaultHeaders());
        }
        if (MapUtil.isNotEmpty(source.getDefaultHeaders())) {
            headers.putAll(source.getDefaultHeaders());
        }
        merged.setDefaultHeaders(headers);

        merged.setProxy(ObjectUtil.defaultIfNull(source.getProxy(), defaultConfig.getProxy()));
        merged.setSsl(ObjectUtil.defaultIfNull(source.getSsl(), defaultConfig.getSsl()));

        return merged;
    }

    /**
     * 远程调用日志配置
     *
     * @author Ateng
     * @since 2026-04-29
     */
    @Data
    public static class LoggingConfig {

        private Boolean enabled = true;

        private Boolean includeHeaders = true;

        private Boolean includeRequestBody = false;

        @Min(value = 0, message = "请求体日志最大长度不能小于 0")
        private Integer maxRequestBodyLength = 2048;

        private List<String> sensitiveHeaders = List.of(
                "Authorization",
                "Cookie",
                "Set-Cookie",
                "X-API-Key",
                "X-App-Secret"
        );

    }

    /**
     * TraceId 配置
     *
     * @author Ateng
     * @since 2026-04-29
     */
    @Data
    public static class TraceConfig {

        private Boolean enabled = true;

        private String mdcName = RemoteHttpConstant.MDC_TRACE_ID;

        private String headerName = RemoteHttpConstant.DEFAULT_TRACE_HEADER;

        private Boolean generateIfAbsent = true;

    }

    /**
     * 错误处理配置
     *
     * @author Ateng
     * @since 2026-04-29
     */
    @Data
    public static class ErrorHandlingConfig {

        private Boolean enabled = true;

        @Min(value = 0, message = "错误响应体最大长度不能小于 0")
        private Integer maxResponseBodyLength = 4096;

    }

    /**
     * HTTP 客户端基础配置
     *
     * @author Ateng
     * @since 2026-04-29
     */
    @Data
    public static class ClientConfig {

        private String baseUrl;

        private Duration connectTimeout = Duration.ofSeconds(3);

        private Duration connectionRequestTimeout = Duration.ofSeconds(2);

        private Duration responseTimeout = Duration.ofSeconds(10);

        private Duration maxIdleTime = Duration.ofSeconds(30);

        @Min(value = 1, message = "总连接数必须大于 0")
        private Integer maxConnTotal = 200;

        @Min(value = 1, message = "单路由连接数必须大于 0")
        private Integer maxConnPerRoute = 50;

        private Map<String, String> defaultHeaders = new LinkedHashMap<>();

        @Valid
        private ProxyConfig proxy = new ProxyConfig();

        @Valid
        private SslConfig ssl = new SslConfig();

    }

    /**
     * HTTP 代理配置
     *
     * @author Ateng
     * @since 2026-04-29
     */
    @Data
    public static class ProxyConfig {

        private Boolean enabled = false;

        private String scheme = "http";

        private String host;

        private Integer port = 0;

        private String username;

        private String password;

    }

    /**
     * SSL 配置
     *
     * @author Ateng
     * @since 2026-04-29
     */
    @Data
    public static class SslConfig {

        private Boolean enabled = false;

        private Boolean trustAll = false;

        private Boolean hostnameVerificationEnabled = true;

        private String protocol = "TLS";

        private String keyStorePath;

        private String keyStorePassword;

        private String keyStoreType = "PKCS12";

        private String trustStorePath;

        private String trustStorePassword;

        private String trustStoreType = "PKCS12";

    }

}