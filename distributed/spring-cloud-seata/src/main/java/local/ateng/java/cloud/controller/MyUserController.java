package local.ateng.java.cloud.controller;

import local.ateng.java.cloud.entity.MyUser;
import local.ateng.java.cloud.service.IMyUserService;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * <p>
 *  前端控制器
 * </p>
 *
 * @author Ateng
 * @since 2025-03-19
 */
@RestController
@RequestMapping("/myUser")
@RequiredArgsConstructor(onConstructor = @__(@Autowired))
public class MyUserController {
    private final IMyUserService myUserService;

    @PostMapping("/save")
    public String save(@RequestBody MyUser myUser) {
        myUserService.saveUser(myUser);
        return "success";
    }

}
