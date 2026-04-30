package io.github.atengk.mail.model;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 邮件发送记录返回对象
 *
 * @author Ateng
 * @since 2026-04-30
 */
public record MailRecordVO(
        Long id,
        List<String> receivers,
        String subject,
        String mailType,
        String status,
        Boolean success,
        Boolean skipped,
        String errorMessage,
        LocalDateTime sendTime,
        LocalDateTime createTime
) {
}