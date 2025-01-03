package local.ateng.java.kafka;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class KafkaProviderApplication {

    public static void main(String[] args) {
        SpringApplication.run(KafkaProviderApplication.class, args);
    }

}
