package local.ateng.java.http.controller;

import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/get")
public class GetController {

    // 获取单个用户，路径参数
    @GetMapping("/user/{id}")
    public String getUserByPath(@PathVariable Long id) {
        return "User with ID: " + id;
    }

    // 获取单个用户，请求参数
    @GetMapping("/user")
    public String getUserByParam(@RequestParam Long id) {
        return "User with ID: " + id;
    }

    // 获取单个用户，无参数
    @GetMapping("/users")
    public String getAllUsers() {
        return "List of all users";
    }

}
