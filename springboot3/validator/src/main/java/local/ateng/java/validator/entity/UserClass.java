package local.ateng.java.validator.entity;

import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import lombok.Data;

import java.util.List;

@Data
public class UserClass {

    @NotBlank(message = "用户名不能为空")
    @Size(min = 3, max = 20, message = "用户名长度必须在3到20个字符之间")
    private String username;

    @NotBlank(message = "密码不能为空")
    @Size(min = 6, max = 100, message = "密码长度至少为6个字符")
    private String password;

    @NotBlank(message = "邮箱不能为空")
    @Email(message = "邮箱格式不正确")
    private String email;

    @NotNull(message = "班级信息不能为空")
    @Valid
    private ClassName className;

    @NotNull(message = "ids不能为空")
    private List<@Valid @Size(min = 6, max = 100, message = "密码长度至少为6个字符") String> ids;

    @NotNull(message = "custom不能为空")
    private String custom;

    // 自定义校验方法
    @AssertTrue(message = "custom属性不满足要求")
    public boolean isCustomValid() {
        return custom != null && custom.length() > 3 && custom.contains("custom");
    }
}

