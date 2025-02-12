package local.ateng.java.snailjob.service;

import cn.hutool.core.util.RandomUtil;
import com.aizuda.snailjob.client.core.annotation.Retryable;
import com.aizuda.snailjob.client.core.retryer.RetryType;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Component
@Slf4j
public class RetryService {

    // 本地重试
    @Retryable(scene = "localRetry", localTimes = 5, localInterval = 3, retryStrategy = RetryType.ONLY_LOCAL)
    public void localRetry() {
        myTask("localRetry");
    }

    // 远程重试
    @Retryable(scene = "remoteRetry", retryStrategy = RetryType.ONLY_REMOTE)
    public void remoteRetry() {
        myTask("remoteRetry");
    }

    // 先本地重试，再远程重试
    @Retryable(scene = "localRemoteRetry", localTimes = 5, localInterval = 3, retryStrategy = RetryType.LOCAL_REMOTE)
    public void localRemoteRetry() {
        myTask("localRemoteRetry");
    }

    private void myTask(String type) {
        log.info("[重试任务][{}] 运行任务...", type);
        int num = RandomUtil.randomInt(0, 3);
        log.info("[重试任务][{}] 计算结果：{}", type, 100 / num);
    }

}
