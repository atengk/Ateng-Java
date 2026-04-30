package io.github.atengk.mail.service;

import cn.hutool.core.collection.ListUtil;
import io.github.atengk.mail.client.MailSendClient;
import io.github.atengk.mail.model.MailSendRequest;
import io.github.atengk.mail.model.MailSendResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

/**
 * 普通文本邮件示例服务
 *
 * @author Ateng
 * @since 2026-04-30
 */
@Service
public class MailTextExampleService {

    private static final Logger log = LoggerFactory.getLogger(MailTextExampleService.class);

    private final MailSendClient mailSendClient;

    public MailTextExampleService(MailSendClient mailSendClient) {
        this.mailSendClient = mailSendClient;
    }

    /**
     * 发送账号登录提醒
     *
     * @param receiver 收件人邮箱
     * @return 邮件发送结果
     */
    public MailSendResult sendLoginNotice(String receiver) {
        String subject = "账号登录提醒";
        String content = "您的账号刚刚完成一次登录操作，如非本人操作，请及时修改密码。";

        MailSendRequest request = MailSendRequest.text(
                ListUtil.of(receiver),
                subject,
                content
        );

        MailSendResult result = mailSendClient.sendTextMail(request);
        log.info("登录提醒邮件发送完成，收件人：{}，结果：{}", receiver, result.message());
        return result;
    }
}