package local.ateng.java.validator.entity;

import jakarta.validation.constraints.*;
import local.ateng.java.validator.validator.MyCustomConstraint;
import lombok.Data;

@Data
public class User {

    @NotBlank(message = "用户名不能为空")
    @Size(min = 3, max = 20, message = "用户名长度必须在3到20个字符之间")
    @MyCustomConstraint
    private String username;

    @NotBlank(message = "密码不能为空")
    @Size(min = 6, max = 100, message = "密码长度至少为6个字符")
    private String password;

    @NotBlank(message = "邮箱不能为空")
    @Email(message = "邮箱格式不正确")
    private String email;


}

