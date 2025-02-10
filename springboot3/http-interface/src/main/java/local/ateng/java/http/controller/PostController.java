package local.ateng.java.http.controller;

import local.ateng.java.http.entity.MyUser;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/post")
public class PostController {

    // 创建新用户
    @PostMapping("/user")
    public String createUser(@RequestBody MyUser user) {
        return "User " + user.getName() + " created with age " + user.getAge();
    }

}
