package local.ateng.java.aop.aspect;

import jakarta.servlet.http.HttpServletRequest;
import local.ateng.java.aop.annotation.PermissionCheck;
import lombok.RequiredArgsConstructor;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Before;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Aspect
@Component
@RequiredArgsConstructor(onConstructor = @__(@Autowired))
public class PermissionAspect {

    private final HttpServletRequest request;  // 自动注入 HttpServletRequest

    /**
     * 前置通知：拦截带有 @PermissionCheck 注解的方法，检查权限
     */
    @Before("@annotation(permissionCheck)")  // 拦截带有 PermissionCheck 注解的方法
    public void checkPermission(PermissionCheck permissionCheck) throws Exception {
        String requiredPermission = permissionCheck.value();  // 获取所需权限
        String userPermission = getUserPermission();  // 从请求头获取用户权限

        if (userPermission == null || !userPermission.equals(requiredPermission)) {
            // 如果用户权限为空或不匹配所需权限，抛出权限不足异常
            throw new Exception("权限不足，无法访问该接口");
        }
    }

    /**
     * 从请求头中获取 token，然后解析用户权限
     */
    private String getUserPermission() {
        // 从请求头中获取 token（假设 token 的名字是 "Authorization"）
        String token = request.getHeader("Authorization");

        if (token != null && token.startsWith("Bearer ")) {
            token = token.substring(7);  // 去掉 "Bearer " 前缀
        }

        // 在实际情况下，可以通过解析 token 来获取用户信息
        // 这里简化为固定返回权限，实际情况中应该解析 token 获取用户权限
        // 例如：JWT 解码，获取用户角色等信息

        // 假设 token 解析结果为以下内容，用户权限是 "USER" 或 "ADMIN"
        if ("1234567890".equals(token)) {
            return "USER";  // 返回 "USER" 权限
        } else if ("2385569970".equals(token)) {
            return "ADMIN";  // 返回 "ADMIN" 权限
        }

        // 如果没有有效的 token 或权限信息，返回 null
        return null;
    }
}

