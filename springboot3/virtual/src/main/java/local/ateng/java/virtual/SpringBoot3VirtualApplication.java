package local.ateng.java.virtual;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
@EnableAsync
public class SpringBoot3VirtualApplication {

    public static void main(String[] args) {
        SpringApplication.run(SpringBoot3VirtualApplication.class, args);
    }

}
