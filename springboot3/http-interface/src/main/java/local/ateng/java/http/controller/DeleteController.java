package local.ateng.java.http.controller;

import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/delete")
public class DeleteController {

    // 删除用户
    @DeleteMapping("/user/{id}")
    public String deleteUser(@PathVariable Long id) {
        return "User with ID: " + id + " deleted.";
    }

}
