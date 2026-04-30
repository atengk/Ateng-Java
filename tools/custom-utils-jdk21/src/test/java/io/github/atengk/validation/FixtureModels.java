package io.github.atengk.validation;

import io.github.atengk.utils.validation.ValidateGroups;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.util.List;

class UserForm {

    @NotNull(message = "用户ID不能为空", groups = ValidateGroups.Update.class)
    private Long id;

    @NotBlank(message = "用户名不能为空")
    private String username;

    @NotBlank(message = "邮箱不能为空")
    @Email(message = "邮箱格式不正确")
    private String email;

    @Min(value = 18, message = "年龄不能小于18")
    private int age;

    @NotBlank(message = "创建来源不能为空", groups = ValidateGroups.Create.class)
    private String createSource;

    @Size(min = 1, message = "标签不能为空")
    private List<@NotBlank(message = "标签名称不能为空") String> tags;

    UserForm(Long id, String username, String email, int age, String createSource, List<String> tags) {
        this.id = id;
        this.username = username;
        this.email = email;
        this.age = age;
        this.createSource = createSource;
        this.tags = tags;
    }

    static UserForm valid() {
        return new UserForm(1L, "ateng", "ateng@example.com", 20, "web", List.of("java"));
    }

    static UserForm invalid() {
        return new UserForm(null, "", "bad-email", 17, "", List.of(""));
    }
}

class AddressForm {

    @NotBlank(message = "城市不能为空")
    private String city;

    AddressForm(String city) {
        this.city = city;
    }
}

class ItemForm {

    @NotBlank(message = "商品名称不能为空")
    private String name;

    ItemForm(String name) {
        this.name = name;
    }
}

class OrderForm {

    @Valid
    @NotNull(message = "地址不能为空")
    private AddressForm address;

    @Valid
    @Size(min = 1, message = "商品不能为空")
    private List<ItemForm> items;

    OrderForm(AddressForm address, List<ItemForm> items) {
        this.address = address;
        this.items = items;
    }
}

class ExecutableService {

    public void create(@NotBlank(message = "参数名称不能为空") String name, @Min(value = 1, message = "数量不能小于1") int count) {
    }

    @NotBlank(message = "返回值不能为空")
    public String getName(boolean blank) {
        return blank ? "" : "ateng";
    }
}
