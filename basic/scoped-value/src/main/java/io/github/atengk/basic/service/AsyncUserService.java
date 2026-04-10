package io.github.atengk.basic.service;

import io.github.atengk.basic.async.AsyncExecutor;
import io.github.atengk.basic.context.RequestContext;
import io.github.atengk.basic.context.TenantContext;
import io.github.atengk.basic.util.LogUtil;
import org.springframework.stereotype.Service;

/**
 * 异步业务示例
 * <p>
 * 演示 StructuredTaskScope 下 ScopedValue 自动传播能力
 *
 * @author Ateng
 * @since 2026-04-10
 */
@Service
public class AsyncUserService {

    /**
     * 并行执行两个任务
     */
    public void processAsync() {

        AsyncExecutor.run(
                () -> {

                    LogUtil.info("任务A执行，userId=" + RequestContext.USER_ID.get()
                            + ", tenantId=" + TenantContext.getTenantId());

                    return "A完成";
                },
                () -> {

                    LogUtil.info("任务B执行，userId=" + RequestContext.USER_ID.get()
                            + ", tenantId=" + TenantContext.getTenantId());

                    return "B完成";
                }
        );
    }
}
