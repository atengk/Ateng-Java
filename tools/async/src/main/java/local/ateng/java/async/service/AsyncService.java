package local.ateng.java.async.service;

import cn.hutool.core.thread.ThreadUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Async;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

@Service
public class AsyncService {

    private static final Logger logger = LoggerFactory.getLogger(AsyncService.class);

    @Scheduled(fixedRate = 5000)
    @Async
    public void asyncMethod() {
        logger.info("执行异步任务，线程：{}", Thread.currentThread().toString());
        ThreadUtil.sleep(5000);
    }

}

