package io.github.atengk;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class RealtimeWebSocketClusterApplication {

    public static void main(String[] args) {
        SpringApplication.run(RealtimeWebSocketClusterApplication.class, args);
    }

}
