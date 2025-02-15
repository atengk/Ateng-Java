package local.ateng.java.log;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;

@SpringBootApplication
@EnableScheduling
public class SpringBoot3LogApplication {

    private static final Logger log = LogManager.getLogger(SpringBoot3LogApplication.class);

    public static void main(String[] args) {
        SpringApplication.run(SpringBoot3LogApplication.class, args);
    }

    @Scheduled(fixedRate = 5000)
    public void log() {
        log.debug("Hello World!");
        log.info("Hello World!");
        log.error("Hello World!");
    }

}
