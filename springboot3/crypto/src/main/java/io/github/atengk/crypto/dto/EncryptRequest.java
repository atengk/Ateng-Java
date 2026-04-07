package io.github.atengk.crypto.dto;

import lombok.Data;

/**
 * 加密请求体
 * <p>
 * 用于前后端统一传输加密数据，字段说明如下：
 * 1. data：业务数据经过 AES 加密后的字符串（Base64 编码）
 * 2. timestamp：请求时间戳（毫秒），用于防重放攻击
 * 3. nonce：随机字符串（一次性使用），用于防重放攻击
 * 4. sign：签名值（HmacSHA256），用于防篡改校验
 *
 * 请求处理流程：
 * 1. 服务端先校验 timestamp（时间窗口）
 * 2. 校验 nonce 是否重复（Redis）
 * 3. 校验 sign 是否正确
 * 4. 最后解密 data 得到原始业务数据
 *
 * @author 孔余
 * @since 2026-01-29
 */
@Data
public class EncryptRequest {

    /**
     * 加密数据（AES 加密后的 Base64 字符串）
     */
    private String data;

    /**
     * 时间戳（毫秒）
     */
    private Long timestamp;

    /**
     * 随机字符串（防重放）
     */
    private String nonce;

    /**
     * 签名（HmacSHA256）
     */
    private String sign;
}