package local.ateng.java.virtual.test;

import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@Slf4j
public class DemoController {

    @GetMapping("/test")
    public String test() {
        String threadInfo = Thread.currentThread().toString();
        log.info("Current thread: " + threadInfo);
        return "Thread info: " + threadInfo;
    }
}

