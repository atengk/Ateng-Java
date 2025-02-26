package local.ateng.java.auth.controller;

import com.alibaba.fastjson2.JSONObject;
import jakarta.servlet.http.HttpServletRequest;
import local.ateng.java.auth.constant.AppCodeEnum;
import local.ateng.java.auth.utils.Result;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.context.HttpSessionSecurityContextRepository;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;

@RestController
@RequestMapping("/user")
public class UserController {


    @GetMapping("/login")
    public Result login(String username, String password, HttpServletRequest request) {
        // 自定义验证用户的账号和密码（这里只是示例，实际应该去数据库或其他存储验证）
        if (!"admin".equals(username)) {
            return Result.error(AppCodeEnum.AUTH_USER_NOT_FOUND.getCode(), AppCodeEnum.AUTH_USER_NOT_FOUND.getDescription());
        }
        if (!"Admin@123".equals(password)) {
            return Result.error(AppCodeEnum.AUTH_PASSWORD_INCORRECT.getCode(), AppCodeEnum.AUTH_PASSWORD_INCORRECT.getDescription());
        }

        // 验证账号密码
        UsernamePasswordAuthenticationToken token = new UsernamePasswordAuthenticationToken(username, null, new ArrayList<>());
        token.setDetails("我是阿腾");
        // 存入上下文
        SecurityContextHolder.getContext().setAuthentication(token);
        request.getSession().setAttribute(HttpSessionSecurityContextRepository.SPRING_SECURITY_CONTEXT_KEY, SecurityContextHolder.getContext());

        // 返回成功
        return Result.success(AppCodeEnum.AUTH_USER_LOGIN_SUCCESS.getCode(), AppCodeEnum.AUTH_USER_LOGIN_SUCCESS.getDescription()).setData(JSONObject.of("token", "1234567890"));
    }


    // 查询用户信息
    @GetMapping("/get-info")
    public Result getInfo() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        Object principal = authentication.getPrincipal();
        Object credentials = authentication.getCredentials();
        Object details = authentication.getDetails();
        Collection<? extends GrantedAuthority> authorities = authentication.getAuthorities();
        return Result.success(JSONObject.of(
                "principal", principal,
                "credentials", credentials,
                "details", details,
                "authorities", authorities
        ));
    }

    // 获取数据
    @GetMapping("/list")
    public Result list() {
        return Result.success(Arrays.asList(1, 2, 3, 4, 5));
    }

    // 新增用户（user.add权限测试）
    @GetMapping("/add")
    public Result add() {
        return Result.success();
    }

    // 注解角色测试
    @GetMapping("/check-role")
    @PreAuthorize("hasRole('admin')")
    public Result checkRole() {
        return Result.success();
    }
    // 注解权限测试
    @PreAuthorize("hasAuthority('user.add')")
    @GetMapping("/check-permission")
    public Result checkPermission() {
        return Result.success();
    }

}
