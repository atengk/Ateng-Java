package io.github.atengk.basic.service;

import io.github.atengk.basic.context.UserContext;
import io.github.atengk.basic.util.LogUtil;
import org.springframework.stereotype.Service;

/**
 * 权限与用户业务示例
 *
 * 演示登录态信息统一获取（角色 / 权限 / 用户）
 *
 * @author Ateng
 * @since 2026-04-10
 */
@Service
public class PermissionService {

    /**
     * 权限校验示例
     */
    public void checkPermission() {

        UserContext.UserInfo user = UserContext.getUser();

        LogUtil.info("当前用户：" + user.username()
                + ", roles=" + user.roles()
                + ", permissions=" + user.permissions());

        // 示例：权限判断
        if (!user.permissions().contains("user:write")) {
            throw new RuntimeException("无权限访问");
        }

        LogUtil.info("权限校验通过");
    }
}
