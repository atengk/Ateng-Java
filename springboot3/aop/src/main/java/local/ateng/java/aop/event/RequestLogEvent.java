package local.ateng.java.aop.event;

import local.ateng.java.aop.entity.RequestLogInfo;
import org.springframework.context.ApplicationEvent;

/**
 * 日志事件，用于发布请求日志
 *
 * @author 孔余
 * @email 2385569970@qq.com
 * @since 2025-02-20
 */
public class RequestLogEvent extends ApplicationEvent {
    private final RequestLogInfo requestLogInfo;

    public RequestLogEvent(Object source, RequestLogInfo requestLogInfo) {
        super(source);
        this.requestLogInfo = requestLogInfo;
    }

    public RequestLogInfo getRequestLogInfo() {
        return requestLogInfo;
    }
}

