package local.ateng.java.aop;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.EnableAspectJAutoProxy;
import org.springframework.scheduling.annotation.EnableAsync;

@SpringBootApplication
@EnableAspectJAutoProxy
@EnableAsync
public class SpringBoot3AOPApplication {

    public static void main(String[] args) {
        SpringApplication.run(SpringBoot3AOPApplication.class, args);
    }

}
