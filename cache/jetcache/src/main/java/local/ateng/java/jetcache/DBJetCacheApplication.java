package local.ateng.java.jetcache;

import com.alicp.jetcache.anno.config.EnableMethodCache;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
@EnableMethodCache(basePackages = {"local.ateng.java"})
public class DBJetCacheApplication {

    public static void main(String[] args) {
        SpringApplication.run(DBJetCacheApplication.class, args);
    }

}
