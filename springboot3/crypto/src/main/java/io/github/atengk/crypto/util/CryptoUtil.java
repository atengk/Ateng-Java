package io.github.atengk.crypto.util;

import cn.hutool.core.codec.Base64;
import cn.hutool.core.util.ObjectUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.crypto.digest.HMac;
import cn.hutool.crypto.digest.HmacAlgorithm;
import cn.hutool.crypto.symmetric.AES;
import io.github.atengk.crypto.config.CryptoProperties;

import java.nio.charset.StandardCharsets;

/**
 * 加密工具类
 *
 * @author 孔余
 * @since 2026-01-29
 */
public class CryptoUtil {

    private static byte[] AES_KEY;
    private static byte[] AES_IV;
    private static byte[] SIGN_KEY;

    private static AES AES_INSTANCE;
    private static HMac HMAC_INSTANCE;

    /**
     * 初始化（由 Spring 调用）
     */
    public static void init(CryptoProperties properties) {

        AES_KEY = Base64.decode(properties.getAesKey());
        AES_IV = Base64.decode(properties.getIv());
        SIGN_KEY = properties.getSignKey().getBytes(StandardCharsets.UTF_8);

        AES_INSTANCE = new AES("CBC", "PKCS5Padding", AES_KEY, AES_IV);
        HMAC_INSTANCE = new HMac(HmacAlgorithm.HmacSHA256, SIGN_KEY);
    }

    /**
     * AES 加密
     */
    public static String encrypt(String data) {

        if (StrUtil.isBlank(data)) {
            return StrUtil.EMPTY;
        }

        return AES_INSTANCE.encryptBase64(data);
    }

    /**
     * AES 解密
     */
    public static String decrypt(String data) {

        if (StrUtil.isBlank(data)) {
            return StrUtil.EMPTY;
        }

        return AES_INSTANCE.decryptStr(data);
    }

    /**
     * 生成签名
     */
    public static String sign(String method,
                              String path,
                              String data,
                              long timestamp,
                              String nonce) {

        String content = buildSignContent(method, path, data, timestamp, nonce);

        return HMAC_INSTANCE.digestHex(content);
    }

    /**
     * 校验签名（常量时间比较）
     */
    public static boolean verify(String method,
                                 String path,
                                 String data,
                                 long timestamp,
                                 String nonce,
                                 String sign) {

        if (StrUtil.isBlank(sign)) {
            return false;
        }

        String localSign = sign(method, path, data, timestamp, nonce);

        return constantTimeEquals(localSign, sign);
    }

    /**
     * 构建签名字符串
     */
    private static String buildSignContent(String method,
                                           String path,
                                           String data,
                                           long timestamp,
                                           String nonce) {

        return "method=" + normalizeMethod(method) +
                "&path=" + normalizePath(path) +
                "&data=" + safe(data) +
                "&timestamp=" + timestamp +
                "&nonce=" + safe(nonce);
    }

    /**
     * Method 标准化
     */
    private static String normalizeMethod(String method) {
        return StrUtil.toUpperCase(StrUtil.blankToDefault(method, "GET"));
    }

    /**
     * Path 标准化
     */
    private static String normalizePath(String path) {

        if (StrUtil.isBlank(path)) {
            return "/";
        }

        path = path.replaceAll("//+", "/");

        if (path.length() > 1 && path.endsWith("/")) {
            path = path.substring(0, path.length() - 1);
        }

        return path;
    }

    /**
     * 防止 null
     */
    private static String safe(String str) {
        return ObjectUtil.defaultIfNull(str, "");
    }

    /**
     * 常量时间比较（防时序攻击）
     */
    private static boolean constantTimeEquals(String a, String b) {

        if (a == null || b == null) {
            return false;
        }

        if (a.length() != b.length()) {
            return false;
        }

        int result = 0;

        for (int i = 0; i < a.length(); i++) {
            result |= a.charAt(i) ^ b.charAt(i);
        }

        return result == 0;
    }
}