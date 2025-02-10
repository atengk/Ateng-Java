package local.ateng.java.http.controller;

import local.ateng.java.http.entity.MyUser;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/patch")
public class PatchController {

    // 部分更新用户信息
    @PatchMapping("/user/{id}")
    public String updateUserPartially(@PathVariable Long id, @RequestBody MyUser user) {
        return "User with ID: " + id + " partially updated to name: " + user.getName();
    }

}
