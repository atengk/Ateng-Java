package io.github.atengk.license.service;

import io.github.atengk.license.model.License;

import java.io.InputStream;
import java.time.LocalDateTime;

/**
 * License 服务接口
 *
 * 特点
 * 1 Redis 为唯一数据源
 * 2 文件仅用于导入导出
 * 3 每次校验实时读取 Redis
 */
public interface LicenseService {

    /**
     * 生成 License 字符串
     *
     * @param license License 对象
     * @return License 字符串
     */
    String generate(License license);

    /**
     * 解析 License 字符串
     *
     * @param licenseStr License 字符串
     * @return License 对象
     */
    License parse(String licenseStr);

    /**
     * 写入 Redis
     *
     * @param licenseStr License 字符串
     */
    void save(String licenseStr);

    /**
     * 从 Redis 获取 License 字符串
     *
     * @return License 字符串
     */
    String get();

    /**
     * 获取当前 License（解析后）
     *
     * @return License 对象
     */
    License current();

    /**
     * 导出 License 文件（用于下载）
     *
     * @return License 文件字节
     */
    byte[] exportLicense();

    /**
     * 导入 License 文件
     *
     * 流程
     * 1 读取文件内容
     * 2 解析 License
     * 3 校验合法性
     * 4 写入 Redis
     *
     * @param inputStream License 文件流
     */
    void importLicense(InputStream inputStream);

    /**
     * 校验当前 License（基础校验）
     *
     * @return true 表示合法
     */
    boolean validate();

    /**
     * 校验指定 License
     *
     * @param license License
     * @return true 表示合法
     */
    boolean validate(License license);

    /**
     * 校验签名
     *
     * @param license License
     * @return true 表示合法
     */
    boolean verifySignature(License license);

    /**
     * 判断是否过期
     *
     * @param license License
     * @return true 表示过期
     */
    boolean isExpired(License license);

    /**
     * 刷新 License
     *
     * @param license 原 License
     * @param newExpireAt 新过期时间
     * @return 新 License 字符串
     */
    String refresh(License license, LocalDateTime newExpireAt);

    /**
     * 删除 License（清空 Redis）
     */
    void remove();

    /**
     * 生成机器码
     *
     * @return 机器码
     */
    String generateMachineCode();

    /**
     * 是否匹配当前机器
     *
     * @param license License
     * @return true 表示匹配
     */
    boolean matchMachine(License license);

    /**
     * IP 是否允许
     *
     * @param license License
     * @param clientIp IP
     * @return true 表示允许
     */
    boolean matchIp(License license, String clientIp);

    /**
     * 是否具备功能权限
     *
     * @param license License
     * @param featureCode 功能编码
     * @return true 表示允许
     */
    boolean hasFeature(License license, String featureCode);

}