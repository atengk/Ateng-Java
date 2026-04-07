package io.github.atengk.crypto.config;

import cn.hutool.core.codec.Base64;
import cn.hutool.core.util.StrUtil;
import jakarta.annotation.PostConstruct;
import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * 加密配置属性
 * <p>
 * 用于加载 AES Key、签名 Key、IV
 *
 * @author 孔余
 * @since 2026-01-29
 */
@Component
@Data
@ConfigurationProperties(prefix = "crypto")
public class CryptoProperties {

    /**
     * AES 密钥（Base64 或明文 16/24/32 位）
     */
    private String aesKey;

    /**
     * 签名密钥
     */
    private String signKey;

    /**
     * 初始向量 IV
     */
    private String iv;

    /**
     * 初始化校验（企业级必须）
     */
    @PostConstruct
    public void validate() {

        if (StrUtil.isBlank(aesKey)) {
            throw new IllegalArgumentException("crypto.aes-key 不能为空");
        }

        if (StrUtil.isBlank(signKey)) {
            throw new IllegalArgumentException("crypto.sign-key 不能为空");
        }

        if (StrUtil.isBlank(iv)) {
            throw new IllegalArgumentException("crypto.iv 不能为空");
        }

        /*
         * AES Key 校验（解码后必须 16/24/32 字节）
         */
        byte[] keyBytes = Base64.decode(aesKey);
        int keyLength = keyBytes.length;

        if (keyLength != 16 && keyLength != 24 && keyLength != 32) {
            throw new IllegalArgumentException("crypto.aes-key 解码后长度必须为 16/24/32 字节");
        }

        /*
         * IV 校验（解码后必须 16 字节）
         */
        byte[] ivBytes = Base64.decode(iv);

        if (ivBytes.length != 16) {
            throw new IllegalArgumentException("crypto.iv 解码后长度必须为 16 字节");
        }
    }
}
