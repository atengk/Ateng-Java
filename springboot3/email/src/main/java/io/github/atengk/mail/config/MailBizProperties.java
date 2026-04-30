package io.github.atengk.mail.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.ArrayList;
import java.util.List;

/**
 * 邮件业务配置属性
 *
 * @author Ateng
 * @since 2026-04-30
 */
@Data
@ConfigurationProperties(prefix = "mail.biz")
public class MailBizProperties {

    /**
     * 是否启用邮件发送
     */
    private Boolean enabled = true;

    /**
     * 默认发件人名称
     */
    private String fromName = "系统通知";

    /**
     * 是否记录邮件发送日志
     */
    private Boolean recordEnabled = true;

    /**
     * 是否启用异步发送
     */
    private Boolean asyncEnabled = true;

    /**
     * 测试环境收件人白名单
     */
    private List<String> testReceiverWhitelist = new ArrayList<>();

    /**
     * 单封邮件最大附件大小，单位 MB
     */
    private Integer maxAttachmentSizeMb = 20;

    /**
     * 单次最多收件人数
     */
    private Integer maxReceiverCount = 50;
}