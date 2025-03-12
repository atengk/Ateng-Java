package local.ateng.java.cloud;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;
import org.springframework.cloud.openfeign.EnableFeignClients;

@SpringBootApplication
@EnableDiscoveryClient // 启动Nacos服务发现
@EnableFeignClients  // 启用 Feign
public class DistributedCloudOpenFeignApplication {

    public static void main(String[] args) {
        SpringApplication.run(DistributedCloudOpenFeignApplication.class, args);
    }

}
