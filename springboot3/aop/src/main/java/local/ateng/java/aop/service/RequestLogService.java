package local.ateng.java.aop.service;

import local.ateng.java.aop.event.RequestLogEvent;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

/**
 * 接受切面发布日志事件的数据
 *
 * @author 孔余
 * @email 2385569970@qq.com
 * @since 2025-02-20
 */
@Service
@Slf4j
public class RequestLogService {

    @EventListener
    @Async
    public void requestLogEvent(RequestLogEvent requestLogEvent) {
        log.info("接受到日志数据={}", requestLogEvent.getRequestLogInfo());
        // 将 RequestLogEvent 事件实体类转换成业务类
        // ...
    }

}
