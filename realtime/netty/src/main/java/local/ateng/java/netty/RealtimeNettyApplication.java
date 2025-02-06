package local.ateng.java.netty;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class RealtimeNettyApplication {

    public static void main(String[] args) {
        SpringApplication.run(RealtimeNettyApplication.class, args);
    }

}
