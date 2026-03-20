package io.github.atengk.sshj;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class ToolsSSHJApplication {

    public static void main(String[] args) {
        SpringApplication.run(ToolsSSHJApplication.class, args);
    }

}
