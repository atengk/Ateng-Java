package local.ateng.java.fastjson.controller;

import local.ateng.java.fastjson.entity.ClassNameEntity;
import local.ateng.java.fastjson.entity.UserInfoEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 类的模板注释
 *
 * @author 孔余
 * @since 2024-02-05 16:25
 */
@RestController
@RequestMapping("/class")
public class ClassNameController {

    @PostMapping("/add")
    public ClassNameEntity add(@RequestBody ClassNameEntity className) {
        return className;
    }

    @PostMapping("/user")
    public UserInfoEntity add(@RequestBody UserInfoEntity userInfoEntity) {
        return userInfoEntity;
    }

}
