package local.ateng.java.flink;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableAsync;

@SpringBootApplication
public class BigdataFlinkClusterApplication {

    public static void main(String[] args) {
        SpringApplication.run(BigdataFlinkClusterApplication.class, args);
    }

}
