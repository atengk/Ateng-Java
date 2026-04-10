package io.github.atengk.basic.service;

import io.github.atengk.basic.holder.TraceIdContextHolder;
import org.springframework.stereotype.Service;

/**
 * 示例业务类（打印 TraceId）
 *
 * @author Ateng
 * @since 2026-04-10
 */
@Service
public class TraceService {

    public String process() {
        String traceId = TraceIdContextHolder.get();
        System.out.println("当前 TraceId：" + traceId);
        return traceId;
    }
}
