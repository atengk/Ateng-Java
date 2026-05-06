package io.github.atengk.mail.dto;

import cn.hutool.core.collection.CollUtil;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.List;

/**
 * 单封邮件发送结果。
 *
 * @author Ateng
 * @since 2026-05-06
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MailSendResult implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 业务编号。
     */
    private String bizId;

    /**
     * 邮件服务商或 SMTP 返回的消息编号。
     */
    private String messageId;

    /**
     * 邮件主题。
     */
    private String subject;

    /**
     * 收件人地址列表。
     */
    @Builder.Default
    private List<String> recipients = CollUtil.newArrayList();

    /**
     * 是否发送成功。
     */
    private Boolean success;

    /**
     * 失败编码。
     */
    private String errorCode;

    /**
     * 失败信息。
     */
    private String errorMessage;

    /**
     * 发送完成时间。
     */
    @Builder.Default
    private LocalDateTime sendTime = LocalDateTime.now();
}
