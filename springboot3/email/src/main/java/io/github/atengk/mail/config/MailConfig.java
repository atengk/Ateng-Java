package io.github.atengk.mail.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;

import jakarta.annotation.PostConstruct;

/**
 * 邮件模块配置
 *
 * @author Ateng
 * @since 2026-04-30
 */
@Slf4j
@Configuration
@EnableConfigurationProperties(MailBizProperties.class)
public class MailConfig {

    private final MailBizProperties mailBizProperties;

    public MailConfig(MailBizProperties mailBizProperties) {
        this.mailBizProperties = mailBizProperties;
    }

    /**
     * 初始化邮件模块配置
     */
    @PostConstruct
    public void init() {
        log.info("邮件模块初始化完成，启用状态：{}，异步发送：{}，发送记录：{}",
                mailBizProperties.getEnabled(),
                mailBizProperties.getAsyncEnabled(),
                mailBizProperties.getRecordEnabled());
    }
}