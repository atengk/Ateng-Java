package io.github.atengk.interceptor.util;


import jakarta.servlet.http.HttpServletRequest;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.*;

/**
 * 签名工具类
 */
public class SignUtil {

    private static final String SECRET = "demo_secret_key"; // 服务端密钥

    /**
     * 生成签名
     */
    public static String generateSign(Map<String, String> params) throws Exception {

        // 1. 参数排序
        List<String> keys = new ArrayList<>(params.keySet());
        Collections.sort(keys);

        // 2. 拼接字符串
        StringBuilder sb = new StringBuilder();
        for (String key : keys) {
            if ("sign".equals(key)) continue; // 排除sign本身
            String value = params.get(key);
            if (value != null && !value.isEmpty()) {
                sb.append(key).append("=").append(value).append("&");
            }
        }

        // 3. 拼接密钥
        sb.append("key=").append(SECRET);

        // 4. MD5
        return md5(sb.toString());
    }

    /**
     * 从 request 获取参数
     */
    public static Map<String, String> getParams(HttpServletRequest request) {

        Map<String, String> map = new HashMap<>();

        request.getParameterMap().forEach((k, v) -> {
            if (v.length > 0) {
                map.put(k, v[0]);
            }
        });

        return map;
    }

    /**
     * MD5
     */
    private static String md5(String str) throws Exception {

        MessageDigest md = MessageDigest.getInstance("MD5");
        byte[] digest = md.digest(str.getBytes(StandardCharsets.UTF_8));

        StringBuilder hex = new StringBuilder();
        for (byte b : digest) {
            String s = Integer.toHexString(0xff & b);
            if (s.length() == 1) {
                hex.append('0');
            }
            hex.append(s);
        }
        return hex.toString();
    }
}
