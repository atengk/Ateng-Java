package io.github.atengk.mail.dto;

import cn.hutool.core.collection.CollUtil;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Positive;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.time.Duration;
import java.util.List;

/**
 * 批量邮件发送请求。
 *
 * @author Ateng
 * @since 2026-05-06
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MailBatchSendRequest implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 批量任务编号。
     */
    private String batchBizId;

    /**
     * 待发送邮件列表。
     */
    @Valid
    @NotEmpty(message = "批量邮件列表不能为空")
    @Builder.Default
    private List<MailSendRequest> messages = CollUtil.newArrayList();

    /**
     * 单批次大小，用于实现类控制发送节奏。
     */
    @Positive(message = "批次大小必须大于 0")
    @Builder.Default
    private Integer batchSize = 50;

    /**
     * 批次间隔，用于限流或降低 SMTP 压力。
     */
    @Builder.Default
    private Duration batchInterval = Duration.ZERO;

    /**
     * 单封发送失败后是否继续处理剩余邮件。
     */
    @Builder.Default
    private Boolean continueOnError = true;
}
