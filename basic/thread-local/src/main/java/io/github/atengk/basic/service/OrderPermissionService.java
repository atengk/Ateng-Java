package io.github.atengk.basic.service;

import io.github.atengk.basic.holder.PermissionContextHolder;
import org.springframework.stereotype.Service;

/**
 * 示例业务类（权限校验）
 *
 * @author Ateng
 * @since 2026-04-10
 */
@Service
public class OrderPermissionService {

    /**
     * 创建订单（需要权限）
     */
    public String createOrder() {
        if (!PermissionContextHolder.hasPermission("order:create")) {
            throw new RuntimeException("无权限创建订单");
        }
        return "订单创建成功";
    }

    /**
     * 查看订单
     */
    public String viewOrder() {
        if (!PermissionContextHolder.hasPermission("order:view")) {
            throw new RuntimeException("无权限查看订单");
        }
        return "订单查看成功";
    }
}