package io.github.atengk.basic.service;

import org.springframework.stereotype.Service;

import java.util.HashSet;
import java.util.Set;

/**
 * 权限服务（模拟查询）
 *
 * @author Ateng
 * @since 2026-04-10
 */
@Service
public class PermissionService {

    /**
     * 根据用户ID获取权限集合
     */
    public Set<String> getPermissions(Long userId) {

        // 模拟不同用户权限
        Set<String> permissions = new HashSet<>();

        if (userId == 1001L) {
            permissions.add("order:create");
            permissions.add("order:view");
        } else {
            permissions.add("order:view");
        }

        return permissions;
    }
}
