package local.ateng.java.http.controller;

import local.ateng.java.http.entity.MyUser;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/put")
public class PutController {

    // 更新用户信息
    @PutMapping("/user/{id}")
    public String updateUser(@PathVariable Long id, @RequestBody MyUser user) {
        return "User with ID: " + id + " updated to name: " + user.getName() + " and age: " + user.getAge();
    }

}
