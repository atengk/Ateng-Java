package local.ateng.java.storage;

import org.dromara.x.file.storage.spring.EnableFileStorage;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
@EnableFileStorage
public class XFileStorageApplication {

    public static void main(String[] args) {
        SpringApplication.run(XFileStorageApplication.class, args);
    }

}
