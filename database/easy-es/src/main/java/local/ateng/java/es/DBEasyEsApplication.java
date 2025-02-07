package local.ateng.java.es;

import org.dromara.easyes.spring.annotation.EsMapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
@EsMapperScan("local.ateng.java.es.**.mapper")
public class DBEasyEsApplication {

    public static void main(String[] args) {
        SpringApplication.run(DBEasyEsApplication.class, args);
    }

}
