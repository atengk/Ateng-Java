package io.github.atengk.basic;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.EnableAspectJAutoProxy;

@SpringBootApplication
@EnableAspectJAutoProxy
public class BasicThreadLocalApplication {

    public static void main(String[] args) {
        SpringApplication.run(BasicThreadLocalApplication.class, args);
    }

}
