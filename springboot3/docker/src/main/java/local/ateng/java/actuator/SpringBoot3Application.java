package local.ateng.java.actuator;

import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;

@SpringBootApplication
@Slf4j
public class SpringBoot3Application {

    public static void main(String[] args) {
        SpringApplication.run(SpringBoot3Application.class, args);
    }

    @EventListener(ApplicationReadyEvent.class)
    public void applicationReady() {
        log.info("阿腾：系统已就绪！请开始使用吧！！！");
    }

}
