package local.ateng.java.cloud;

import org.apache.dubbo.config.spring.context.annotation.EnableDubbo;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;

@SpringBootApplication
@EnableDiscoveryClient // 启动Nacos服务发现
@EnableDubbo // 启用 Dubbo
public class DistributedCloudDubboConsumerApplication {

    public static void main(String[] args) {
        SpringApplication.run(DistributedCloudDubboConsumerApplication.class, args);
    }

}
