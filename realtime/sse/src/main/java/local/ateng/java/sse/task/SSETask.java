package local.ateng.java.sse.task;

import cn.hutool.core.date.DateUtil;
import cn.hutool.core.util.RandomUtil;
import cn.hutool.core.util.StrUtil;
import local.ateng.java.sse.service.SSEService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * 定时发送消息
 *
 * @author 孔余
 * @email 2385569970@qq.com
 * @since 2025-02-06
 */
@Component
public class SSETask {
    @Autowired
    private SSEService sseService;

    // 定时任务，每秒向指定endpoint和user发送数据
    @Scheduled(fixedRate = 3000)
    public void sendEvents() {
        String endpoint = "/events";
        String userId = String.valueOf(RandomUtil.randomInt(1, 3));
        String data = StrUtil.format("userId={}, This is a real-time event at {}", userId, DateUtil.now());
        sseService.sendDataToUserInEndpoint(endpoint, userId, data);
    }

    // 定时任务，每秒向指定endpoint和user发送数据
    @Scheduled(fixedRate = 3000)
    public void sendEvents2() {
        String endpoint = "/events2";
        String data = StrUtil.format("This is a real-time event at {}", DateUtil.now());
        sseService.sendDataToAllUsersInEndpoint(endpoint, data);
    }
}
