package local.ateng.java.validator.entity;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class ClassName {
    @NotNull(message = "id不能为空")
    @Min(value = 1, message = "id错误")
    private Long id;
    @NotBlank(message = "name不能为空")
    @Size(min = 3, max = 20, message = "name长度必须在3到20个字符之间")
    private String name;
}
