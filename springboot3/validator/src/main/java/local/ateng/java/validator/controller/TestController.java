package local.ateng.java.validator.controller;

import local.ateng.java.validator.constant.AppCodeEnum;
import local.ateng.java.validator.utils.Result;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;

/**
 * 测试接口
 *
 * @author 孔余
 * @email 2385569970@qq.com
 * @since 2025-01-09
 */
@RestController
@RequestMapping("/test")
public class TestController {

    @GetMapping("/exception")
    public Result exception(@RequestParam(name = "id", required = false, defaultValue = "0") Long id) {
        long result = 1 / id;
        HashMap<String, Long> map = new HashMap<>() {{
            put("result", result);
            put("null", null);
        }};
        return Result.success(AppCodeEnum.SUCCESS.getCode(), AppCodeEnum.SUCCESS.getDescription()).setData(map);
    }

}