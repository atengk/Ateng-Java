package io.github.atengk.mail.service;

import cn.hutool.core.collection.ListUtil;
import cn.hutool.core.io.FileUtil;
import io.github.atengk.mail.client.MailSendClient;
import io.github.atengk.mail.model.MailAttachment;
import io.github.atengk.mail.model.MailSendRequest;
import io.github.atengk.mail.model.MailSendResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.io.File;

/**
 * 附件邮件示例服务
 *
 * @author Ateng
 * @since 2026-04-30
 */
@Service
public class MailAttachmentExampleService {

    private static final Logger log = LoggerFactory.getLogger(MailAttachmentExampleService.class);

    private final MailSendClient mailSendClient;

    public MailAttachmentExampleService(MailSendClient mailSendClient) {
        this.mailSendClient = mailSendClient;
    }

    /**
     * 发送日报附件
     *
     * @param receiver 收件人邮箱
     * @return 邮件发送结果
     */
    public MailSendResult sendDailyReport(String receiver) {
        File reportFile = FileUtil.file("/data/report/daily-report.xlsx");
        MailAttachment attachment = new MailAttachment("每日业务报表.xlsx", reportFile);

        MailSendRequest request = MailSendRequest.attachment(
                ListUtil.of(receiver),
                "每日业务报表",
                "您好，附件为今日业务报表，请查收。",
                false,
                ListUtil.of(attachment)
        );

        MailSendResult result = mailSendClient.sendAttachmentMail(request);
        log.info("日报附件邮件发送完成，收件人：{}，附件：{}，结果：{}",
                receiver, reportFile.getAbsolutePath(), result.message());
        return result;
    }
}