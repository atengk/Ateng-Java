package io.github.atengk.mail.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Map;

/**
 * 动态 SMTP 配置。
 *
 * @author Ateng
 * @since 2026-05-06
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MailSmtpConfig {

    /**
     * SMTP 服务地址。
     */
    private String host;

    /**
     * SMTP 服务端口。
     */
    private Integer port;

    /**
     * SMTP 认证账号。
     */
    private String username;

    /**
     * SMTP 密码或授权码。
     */
    @JsonProperty(access = JsonProperty.Access.WRITE_ONLY)
    private String password;

    /**
     * 默认编码。
     */
    private String defaultEncoding;

    /**
     * 是否开启 SMTP 认证。
     */
    private Boolean auth;

    /**
     * 是否启用 STARTTLS。
     */
    private Boolean starttlsEnable;

    /**
     * 是否强制 STARTTLS。
     */
    private Boolean starttlsRequired;

    /**
     * 是否启用 SSL。
     */
    private Boolean sslEnable;

    /**
     * 连接超时时间，单位毫秒。
     */
    private Integer connectionTimeout;

    /**
     * 读取超时时间，单位毫秒。
     */
    private Integer timeout;

    /**
     * 写入超时时间，单位毫秒。
     */
    private Integer writeTimeout;

    /**
     * 是否强制 from.address 和 username 一致。
     */
    private Boolean forceFromSameAsUsername;

    /**
     * 额外 JavaMail 属性。
     */
    private Map<String, String> properties;
}