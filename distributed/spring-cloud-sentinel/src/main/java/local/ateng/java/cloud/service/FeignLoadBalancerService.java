package local.ateng.java.cloud.service;

import local.ateng.java.cloud.service.fallback.FeignLoadBalancerServiceFallback;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;

@FeignClient(name = "spring-cloud-nacos", fallback = FeignLoadBalancerServiceFallback.class)
public interface FeignLoadBalancerService {

    @GetMapping("/config/get")
    String get();

}
