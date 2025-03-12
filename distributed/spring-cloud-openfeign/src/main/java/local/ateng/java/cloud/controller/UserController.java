package local.ateng.java.cloud.controller;

import local.ateng.java.cloud.service.FeignUserService;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/users")
@RequiredArgsConstructor(onConstructor = @__(@Autowired))
public class UserController {

    private final FeignUserService feignUserService;

    @GetMapping("/{id}")
    public String getUser(@PathVariable Long id) {
        return feignUserService.list(id);
    }

}
