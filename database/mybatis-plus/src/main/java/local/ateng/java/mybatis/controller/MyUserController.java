package local.ateng.java.mybatis.controller;

import local.ateng.java.mybatis.service.IMyUserService;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * <p>
 * 用户信息表，存储用户的基本信息 前端控制器
 * </p>
 *
 * @author 孔余
 * @since 2025-01-13
 */
@RestController
@RequestMapping("/myUser")
@RequiredArgsConstructor(onConstructor = @__(@Autowired))
public class MyUserController {
    private final IMyUserService myUserService;

    @GetMapping("/count")
    public void count() {
        long count = myUserService.count();
        System.out.println(count);
    }

}
