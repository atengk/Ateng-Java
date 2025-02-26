package local.ateng.java.auth.controller;

import local.ateng.java.auth.utils.Result;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Arrays;

@RestController
@RequestMapping("/system")
public class SystemController {

    // 获取数据
    @GetMapping("/list")
    public Result list() {
        return Result.success(Arrays.asList(1, 2, 3, 4, 5));
    }

}
