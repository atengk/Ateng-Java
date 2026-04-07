package io.github.atengk.crypto.util;

import cn.hutool.core.codec.Base64;
import cn.hutool.crypto.digest.HMac;
import cn.hutool.crypto.digest.HmacAlgorithm;
import cn.hutool.crypto.symmetric.AES;
import cn.hutool.extra.spring.SpringUtil;
import io.github.atengk.crypto.config.CryptoProperties;

import java.nio.charset.StandardCharsets;

/**
 * 加密工具类
 * <p>
 * 功能：
 * 1. AES 加密解密（带 IV）
 * 2. HmacSHA256 签名
 * 3. 签名校验
 *
 * @author 孔余
 * @since 2026-01-29
 */
public class CryptoUtil {

    private static final CryptoProperties cryptoProperties = SpringUtil.getBean(CryptoProperties.class);

    /**
     * AES Key（16/24/32字节）
     */
    private static final byte[] AES_KEY = Base64.decode(cryptoProperties.getAesKey());

    /**
     * IV（必须和前端一致）
     */
    private static final byte[] AES_IV = Base64.decode(cryptoProperties.getIv());

    /**
     * 签名 Key
     */
    private static final byte[] SIGN_KEY = cryptoProperties.getSignKey().getBytes(StandardCharsets.UTF_8);

    /**
     * AES 加密（CBC + PKCS5Padding）
     */
    public static String encrypt(String data) {
        AES aes = new AES("CBC", "PKCS5Padding", AES_KEY, AES_IV);
        return aes.encryptBase64(data);
    }

    /**
     * AES 解密
     */
    public static String decrypt(String data) {
        AES aes = new AES("CBC", "PKCS5Padding", AES_KEY, AES_IV);
        return aes.decryptStr(data);
    }

    /**
     * 生成签名（推荐结构化拼接）
     */
    public static String sign(String data, long timestamp, String nonce) {

        String content = buildSignContent(data, timestamp, nonce);

        HMac mac = new HMac(HmacAlgorithm.HmacSHA256, SIGN_KEY);
        return mac.digestHex(content);
    }

    /**
     * 校验签名
     */
    public static boolean verify(String data, long timestamp, String nonce, String sign) {

        String localSign = sign(data, timestamp, nonce);

        return localSign.equalsIgnoreCase(sign);
    }

    /**
     * 构建签名字符串（避免拼接歧义）
     */
    private static String buildSignContent(String data, long timestamp, String nonce) {

        /*
         * 使用 key=value 结构，避免：
         * data=12 + 34 和 data=1 + 234 冲突
         */
        return "data=" + data +
                "&timestamp=" + timestamp +
                "&nonce=" + nonce;
    }

}