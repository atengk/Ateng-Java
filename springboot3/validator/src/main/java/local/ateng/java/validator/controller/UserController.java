package local.ateng.java.validator.controller;

import jakarta.validation.constraints.Min;
import local.ateng.java.validator.entity.User;
import local.ateng.java.validator.utils.Result;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;


@RestController
@RequestMapping("/user") // 基础路径为/user
@Validated // 启用方法参数的校验功能，允许在控制器方法中使用验证注解。
public class UserController {

    /**
     * 注册用户的方法，处理POST请求。
     *
     * @param user 需要注册的用户信息，@RequestBody会将请求体中的JSON映射为User对象。
     * @return Result对象，表示操作结果。
     */
    @PostMapping("/register") // 映射HTTP POST请求到/register路径。
    public Result registerUser(@Validated @RequestBody User user) {
        // @Valid注解用于验证user对象的属性，如果不符合要求会抛出异常。
        // 在这里处理用户注册逻辑，比如保存用户到数据库
        return Result.success(); // 返回成功的结果
    }

    /**
     * 查找用户的方法，处理GET请求。
     *
     * @param id 用户的唯一标识符，必须大于0。
     * @return Result对象，表示操作结果。
     */
    @GetMapping("/findUser") // 映射HTTP GET请求到/findUser路径。
    public Result findUser(
            @RequestParam("id") // 指定id为请求参数，来自URL查询字符串。
            @Min(value = 1, message = "用户ID必须大于0") // 校验id必须大于0，校验失败时返回错误信息。
            Long id
    ) {
        // 假设这里处理查询逻辑
        return Result.success(); // 返回成功的结果
    }
}

