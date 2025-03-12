package local.ateng.java.cloud.controller;

import local.ateng.java.cloud.service.FeignLoadBalancerService;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/loadbalancer")
@RequiredArgsConstructor(onConstructor = @__(@Autowired))
public class LoadBalancerController {

    private final FeignLoadBalancerService feignLoadBalancerService;

    @GetMapping("/get")
    public String get() {
        return feignLoadBalancerService.get();
    }

}
