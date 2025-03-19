package local.ateng.java.cloud.service;

import local.ateng.java.cloud.entity.MyOrder;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

@FeignClient(name = "spring-cloud-seata-demo")
public interface FeignMyOrderService {

    @PostMapping("/myOrder/save")
    MyOrder save(@RequestBody MyOrder myOrder);

}
