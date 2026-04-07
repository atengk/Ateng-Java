package io.github.atengk.crypto.util;

import cn.hutool.core.codec.Base64;
import cn.hutool.core.util.HexUtil;
import cn.hutool.core.util.RandomUtil;
import cn.hutool.crypto.SecureUtil;
import cn.hutool.crypto.digest.HMac;
import cn.hutool.crypto.digest.HmacAlgorithm;
import cn.hutool.crypto.symmetric.AES;
import cn.hutool.extra.spring.SpringUtil;
import io.github.atengk.crypto.config.CryptoProperties;

import java.nio.charset.StandardCharsets;

/**
 * Key 生成工具类
 *
 * @author 孔余
 * @since 2026-01-29
 */
public class CryptoGenerateUtil {

    /**
     * 生成 AES Key（Base64）
     */
    public static String generateAesKey() {
        return Base64.encode(RandomUtil.randomBytes(16));
    }

    /**
     * 生成 AES Key（Hex）
     */
    public static String generateAesKeyHex() {
        return HexUtil.encodeHexStr(RandomUtil.randomBytes(16));
    }

    /**
     * 生成签名 Key
     */
    public static String generateSignKey() {
        return SecureUtil.sha256(RandomUtil.randomString(32));
    }

    /**
     * 生成随机 IV
     */
    public static String generateIv() {
        return Base64.encode(RandomUtil.randomBytes(16));
    }
}