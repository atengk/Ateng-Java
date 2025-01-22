package local.ateng.java.flink;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableAsync;

@SpringBootApplication
@EnableAsync
public class BigdataFlinkStandaloneApplication {

    public static void main(String[] args) {
        SpringApplication.run(BigdataFlinkStandaloneApplication.class, args);
    }

}
