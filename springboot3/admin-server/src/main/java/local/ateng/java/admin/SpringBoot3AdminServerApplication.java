package local.ateng.java.admin;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
public class SpringBoot3AdminServerApplication {

    public static void main(String[] args) {
        SpringApplication.run(SpringBoot3AdminServerApplication.class, args);
    }

}
