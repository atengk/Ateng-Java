package local.ateng.java.cloud;

import org.apache.dubbo.config.spring.context.annotation.EnableDubbo;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;
import org.springframework.cloud.openfeign.EnableFeignClients;

@SpringBootApplication
@EnableDiscoveryClient // 启动Nacos服务发现
@EnableFeignClients  // 启用 Feign
@EnableDubbo // 启用 Dubbo
public class DistributedCloudSeataDemoApplication {

    public static void main(String[] args) {
        SpringApplication.run(DistributedCloudSeataDemoApplication.class, args);
    }

}
