package local.ateng.java.validator.entity;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import local.ateng.java.validator.validation.AddGroup;
import local.ateng.java.validator.validation.UpdateGroup;
import lombok.Data;

@Data
public class Account {
    @NotBlank(message = "账户名不能为空", groups = AddGroup.class) // 创建时必填
    @Size(min = 3, max = 15, message = "账户名长度必须在3到15个字符之间", groups = AddGroup.class) // 创建时长度限制
    @Size(min = 3, message = "账户名长度至少为3个字符", groups = UpdateGroup.class) // 更新时的最小长度
    private String accountName;

    @NotBlank(message = "密码不能为空", groups = AddGroup.class) // 创建时必填
    @Size(min = 6, max = 100, message = "密码长度至少为6个字符", groups = AddGroup.class) // 创建时长度限制
    private String password;

    @NotBlank(message = "邮箱不能为空", groups = AddGroup.class) // 创建时必填
    private String email;
}
