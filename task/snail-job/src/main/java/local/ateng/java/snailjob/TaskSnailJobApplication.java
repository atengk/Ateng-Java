package local.ateng.java.snailjob;

import com.aizuda.snailjob.client.starter.EnableSnailJob;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
@EnableSnailJob
public class TaskSnailJobApplication {

    public static void main(String[] args) {
        SpringApplication.run(TaskSnailJobApplication.class, args);
    }

}
