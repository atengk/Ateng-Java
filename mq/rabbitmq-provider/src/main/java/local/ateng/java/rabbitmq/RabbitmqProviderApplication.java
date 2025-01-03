package local.ateng.java.rabbitmq;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class RabbitmqProviderApplication {

    public static void main(String[] args) {
        SpringApplication.run(RabbitmqProviderApplication.class, args);
    }

}
