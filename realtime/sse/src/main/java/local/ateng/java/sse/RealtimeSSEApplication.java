package local.ateng.java.sse;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class RealtimeSSEApplication {

    public static void main(String[] args) {
        SpringApplication.run(RealtimeSSEApplication.class, args);
    }

}
