package local.ateng.java.validator.controller;

import local.ateng.java.validator.entity.UserClass;
import local.ateng.java.validator.utils.Result;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;


@RestController
@RequestMapping("/user-class")
public class UserClassController {

    @PostMapping("/add")
    public Result add(@Validated @RequestBody UserClass userClass) {
        return Result.success(userClass);
    }

}

