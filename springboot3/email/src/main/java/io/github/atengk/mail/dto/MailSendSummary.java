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
 * 批量邮件发送汇总结果。
 *
 * @author Ateng
 * @since 2026-05-06
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MailSendSummary implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 批量任务编号。
     */
    private String batchBizId;

    /**
     * 总数量。
     */
    private Integer totalCount;

    /**
     * 成功数量。
     */
    private Integer successCount;

    /**
     * 失败数量。
     */
    private Integer failureCount;

    /**
     * 明细结果。
     */
    @Builder.Default
    private List<MailSendResult> results = CollUtil.newArrayList();

    /**
     * 批量任务完成时间。
     */
    @Builder.Default
    private LocalDateTime finishTime = LocalDateTime.now();
}
