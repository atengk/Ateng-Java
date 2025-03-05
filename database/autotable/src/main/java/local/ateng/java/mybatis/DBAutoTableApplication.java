package local.ateng.java.mybatis;

import org.dromara.autotable.springboot.EnableAutoTable;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@EnableAutoTable // 声明使用AutoTable框架
@SpringBootApplication
public class DBAutoTableApplication {

    public static void main(String[] args) {
        SpringApplication.run(DBAutoTableApplication.class, args);
    }

}
