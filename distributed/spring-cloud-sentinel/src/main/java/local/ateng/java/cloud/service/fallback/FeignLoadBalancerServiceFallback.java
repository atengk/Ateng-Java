package local.ateng.java.cloud.service.fallback;

import local.ateng.java.cloud.service.FeignLoadBalancerService;
import org.springframework.stereotype.Component;

@Component
public class FeignLoadBalancerServiceFallback implements FeignLoadBalancerService {
    @Override
    public String get() {
        return "null";
    }
}
