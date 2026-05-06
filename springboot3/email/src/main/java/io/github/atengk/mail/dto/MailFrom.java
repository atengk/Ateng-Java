package io.github.atengk.mail.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 邮件发件人。
 *
 * @author Ateng
 * @since 2026-05-06
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MailFrom {

    /**
     * 发件邮箱。
     *
     * <p>
     * 不传时默认使用当前 SMTP 配置的 username。
     * 多数 SMTP 服务商要求该值必须和 username 一致。
     * </p>
     */
    private String address;

    /**
     * 发件人显示名称。
     */
    private String name;

    /**
     * 动态 SMTP 配置。
     *
     * <p>
     * 不传时使用 spring.mail 默认配置。
     * 传入时本次邮件使用该配置发送。
     * </p>
     */
    private MailSmtpConfig mailConfig;
}