package local.ateng.java.aop;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.EnableAspectJAutoProxy;

@SpringBootApplication
@EnableAspectJAutoProxy
public class SpringBoot3AOPApplication {

    public static void main(String[] args) {
        SpringApplication.run(SpringBoot3AOPApplication.class, args);
    }

}
