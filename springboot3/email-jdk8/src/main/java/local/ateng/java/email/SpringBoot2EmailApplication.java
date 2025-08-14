package local.ateng.java.email;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableAsync;

@SpringBootApplication
@EnableAsync
public class SpringBoot2EmailApplication {

    public static void main(String[] args) {
        SpringApplication.run(SpringBoot2EmailApplication.class, args);
    }

}
