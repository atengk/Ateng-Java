package local.ateng.java.virtual.test;

import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
@Slf4j
public class DemoTask {

    @Scheduled(fixedRate = 5000)
    @Async
    public void demoTask() {
        String threadInfo = Thread.currentThread().toString();
        log.info("Current thread: " + threadInfo);
    }

}
