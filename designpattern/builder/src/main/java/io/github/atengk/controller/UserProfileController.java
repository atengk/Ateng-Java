package io.github.atengk.controller;


import io.github.atengk.model.UserProfile;
import io.github.atengk.service.UserProfileService;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * 用户档案控制器
 */
@RestController
@RequestMapping("/builder")
public class UserProfileController {

    private final UserProfileService userProfileService;

    public UserProfileController(UserProfileService userProfileService) {
        this.userProfileService = userProfileService;
    }

    @PostMapping("/register")
    public String register(@RequestParam String username,
                           @RequestParam String email,
                           @RequestParam(required = false) String address,
                           @RequestParam(required = false) String phone,
                           @RequestParam(required = false) Integer age) {

        // 使用构建者模式创建复杂对象
        UserProfile userProfile = UserProfile.builder()
                .username(username)
                .email(email)
                .address(address)
                .phone(phone)
                .age(age)
                .active(true)
                .admin(false)
                .build();

        return userProfileService.register(userProfile);
    }
}
