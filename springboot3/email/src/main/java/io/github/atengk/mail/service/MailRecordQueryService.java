package io.github.atengk.mail.service;

import io.github.atengk.mail.model.MailRecordQueryRequest;
import io.github.atengk.mail.model.MailRecordVO;
import io.github.atengk.mail.model.PageResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

/**
 * 邮件发送记录查询服务
 *
 * @author Ateng
 * @since 2026-04-30
 */
@Service
public class MailRecordQueryService {

    private static final Logger log = LoggerFactory.getLogger(MailRecordQueryService.class);

    /**
     * 分页查询邮件发送记录
     *
     * @param request 查询请求
     * @return 分页结果
     */
    public PageResult<MailRecordVO> page(MailRecordQueryRequest request) {
        log.warn("邮件发送记录持久化暂未接入，当前返回空分页结果，页码：{}，每页条数：{}",
                request.safePageNum(), request.safePageSize());

        return PageResult.empty(request.safePageNum(), request.safePageSize());
    }
}