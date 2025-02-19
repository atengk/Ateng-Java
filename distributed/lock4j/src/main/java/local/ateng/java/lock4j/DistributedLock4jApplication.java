package local.ateng.java.lock4j;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public class DistributedLock4jApplication {

    public static void main(String[] args) {
        SpringApplication.run(DistributedLock4jApplication.class, args);
    }

}
