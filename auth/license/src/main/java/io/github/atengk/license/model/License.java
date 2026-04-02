package io.github.atengk.license.model;

import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.Set;

/**
 * License 实体
 *
 * 用于描述系统授权信息
 */
@Data
public class License implements Serializable {

    /**
     * License 唯一标识
     */
    private String licenseId;

    /**
     * 客户名称
     */
    private String customerName;

    /**
     * 签发时间
     */
    private LocalDateTime issuedAt;

    /**
     * 过期时间
     */
    private LocalDateTime expireAt;

    /**
     * 绑定机器码
     */
    private String machineCode;

    /**
     * IP 白名单
     */
    private Set<String> allowIps;

    /**
     * 功能权限集合
     */
    private Set<String> features;

    /**
     * 扩展字段
     */
    private String extra;

    /**
     * 数字签名
     */
    private String signature;

}