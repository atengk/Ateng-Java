package local.ateng.java.cloud.service;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

@FeignClient(name = "user-service", url = "https://www.wanandroid.com")
public interface FeignUserService {

    @GetMapping("/article/list/{id}/json")
    String list(@PathVariable("id") Long id);

}
