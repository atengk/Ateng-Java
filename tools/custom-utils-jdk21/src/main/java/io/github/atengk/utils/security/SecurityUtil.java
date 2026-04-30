package io.github.atengk.utils.security;

import javax.crypto.*;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.PBEKeySpec;
import javax.crypto.spec.SecretKeySpec;
import java.io.*;
import java.math.BigInteger;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.*;
import java.security.cert.*;
import java.security.cert.Certificate;
import java.security.interfaces.RSAKey;
import java.security.spec.ECGenParameterSpec;
import java.security.spec.PKCS8EncodedKeySpec;
import java.security.spec.X509EncodedKeySpec;
import java.time.Duration;
import java.time.Instant;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * JDK 21 原生安全工具类，提供随机数、编码、摘要、HMAC、AES、RSA、签名、PEM、证书、密码、JWT、接口签名、文件安全、脱敏等常用能力。
 *
 * @author Ateng
 * @since 2026-04-30
 */
public final class SecurityUtil {

    private static final SecureRandom SECURE_RANDOM = new SecureRandom();
    private static final HexFormat HEX_FORMAT = HexFormat.of();
    private static final Pattern HEX_PATTERN = Pattern.compile("^[0-9a-fA-F]+$");
    private static final Pattern BASE64_PATTERN = Pattern.compile("^[A-Za-z0-9+/]*={0,2}$");
    private static final Pattern BASE64_URL_PATTERN = Pattern.compile("^[A-Za-z0-9_-]*={0,2}$");
    private static final Pattern ENC_PATTERN = Pattern.compile("^ENC\\((.*)\\)$");
    private static final byte[] FILE_AES_GCM_MAGIC = "SUTILGCM1".getBytes(StandardCharsets.US_ASCII);
    private static final int AES_GCM_TAG_BITS = 128;
    private static final int AES_GCM_IV_BYTES = 12;
    private static final int AES_CBC_IV_BYTES = 16;
    private static final int DEFAULT_PASSWORD_ITERATIONS = 120_000;
    private static final int DEFAULT_PASSWORD_HASH_BITS = 256;
    private static final String DEFAULT_PASSWORD_ALGORITHM = "PBKDF2WithHmacSHA256";
    private static final Set<String> WEAK_PASSWORDS = Set.of("123456", "12345678", "111111", "password", "qwerty", "admin", "letmein", "000000");
    private static final Set<String> SENSITIVE_KEYWORDS = Set.of(
            "password", "passwd", "pwd", "secret", "token", "accesstoken", "refreshtoken", "authorization",
            "cookie", "privatekey", "apikey", "appsecret", "credential", "session", "jwt"
    );

    private SecurityUtil() {
        throw new UnsupportedOperationException("SecurityUtil 是静态工具类，不能实例化");
    }

    /**
     * 生成指定长度的安全随机字节。
     *
     * @param length 字节长度，必须大于 0
     * @return 随机字节数组
     */
    public static byte[] randomBytes(int length) {
        requirePositive(length, "length");
        byte[] bytes = new byte[length];
        SECURE_RANDOM.nextBytes(bytes);
        return bytes;
    }

    /**
     * 生成指定字节长度对应的随机 Hex 字符串。
     *
     * @param byteLength 随机字节长度，必须大于 0
     * @return Hex 随机字符串
     */
    public static String randomHex(int byteLength) {
        return hexEncode(randomBytes(byteLength));
    }

    /**
     * 生成指定字节长度对应的 Base64 随机字符串。
     *
     * @param byteLength 随机字节长度，必须大于 0
     * @return Base64 随机字符串
     */
    public static String randomBase64(int byteLength) {
        return base64Encode(randomBytes(byteLength));
    }

    /**
     * 生成指定字节长度对应的 URL 安全 Base64 随机字符串。
     *
     * @param byteLength 随机字节长度，必须大于 0
     * @return URL 安全 Base64 随机字符串
     */
    public static String randomUrlSafeBase64(int byteLength) {
        return base64UrlEncode(randomBytes(byteLength));
    }

    /**
     * 生成指定长度的数字验证码。
     *
     * @param length 验证码长度，必须大于 0
     * @return 数字验证码
     */
    public static String randomNumeric(int length) {
        requirePositive(length, "length");
        StringBuilder builder = new StringBuilder(length);
        for (int i = 0; i < length; i++) {
            builder.append(SECURE_RANDOM.nextInt(10));
        }
        return builder.toString();
    }

    /**
     * 生成指定长度的字母数字随机字符串。
     *
     * @param length 字符长度，必须大于 0
     * @return 字母数字随机字符串
     */
    public static String randomAlphaNumeric(int length) {
        requirePositive(length, "length");
        String alphabet = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789";
        StringBuilder builder = new StringBuilder(length);
        for (int i = 0; i < length; i++) {
            builder.append(alphabet.charAt(SECURE_RANDOM.nextInt(alphabet.length())));
        }
        return builder.toString();
    }

    /**
     * 生成标准 UUID 字符串。
     *
     * @return UUID 字符串
     */
    public static String uuid() {
        return UUID.randomUUID().toString();
    }

    /**
     * 生成不带横线的 UUID 字符串。
     *
     * @return 无横线 UUID 字符串
     */
    public static String uuidWithoutDash() {
        return uuid().replace("-", "");
    }

    /**
     * 生成接口签名常用 Nonce。
     *
     * @return Nonce 字符串
     */
    public static String nonce() {
        return randomUrlSafeBase64(16);
    }

    /**
     * 生成密码哈希或密钥派生常用盐值。
     *
     * @param byteLength 盐值字节长度，必须大于 0
     * @return 盐值字节数组
     */
    public static byte[] salt(int byteLength) {
        return randomBytes(byteLength);
    }

    /**
     * 生成通用安全 Token。
     *
     * @param byteLength Token 原始字节长度，必须大于 0
     * @return URL 安全 Token
     */
    public static String token(int byteLength) {
        return randomUrlSafeBase64(byteLength);
    }

    /**
     * 生成指定长度的初始化向量。
     *
     * @param byteLength IV 字节长度，必须大于 0
     * @return IV 字节数组
     */
    public static byte[] iv(int byteLength) {
        return randomBytes(byteLength);
    }

    /**
     * 生成 AES-GCM 推荐的 12 字节初始化向量。
     *
     * @return 12 字节 IV
     */
    public static byte[] aesGcmIv() {
        return iv(AES_GCM_IV_BYTES);
    }

    /**
     * 生成包含当前时间戳语义的 Nonce。
     *
     * @return 时间戳加随机串组成的 Nonce
     */
    public static String timestampNonce() {
        return Instant.now().toEpochMilli() + "-" + nonce();
    }

    /**
     * 将字符串转换为 UTF-8 字节。
     *
     * @param text 字符串，不能为 null
     * @return UTF-8 字节数组
     */
    public static byte[] toUtf8Bytes(String text) {
        return requireNonNull(text, "text").getBytes(StandardCharsets.UTF_8);
    }

    /**
     * 将 UTF-8 字节转换为字符串。
     *
     * @param bytes 字节数组，不能为 null
     * @return 字符串
     */
    public static String fromUtf8Bytes(byte[] bytes) {
        return new String(requireNonNull(bytes, "bytes"), StandardCharsets.UTF_8);
    }

    /**
     * 将字节数组编码为小写 Hex 字符串。
     *
     * @param bytes 字节数组，不能为 null
     * @return Hex 字符串
     */
    public static String hexEncode(byte[] bytes) {
        return HEX_FORMAT.formatHex(requireNonNull(bytes, "bytes"));
    }

    /**
     * 将 Hex 字符串解码为字节数组。
     *
     * @param hex Hex 字符串，长度必须为偶数
     * @return 字节数组
     */
    public static byte[] hexDecode(String hex) {
        String value = requireNotBlank(hex, "hex");
        if ((value.length() & 1) != 0 || !HEX_PATTERN.matcher(value).matches()) {
            throw new IllegalArgumentException("hex 格式非法");
        }
        return HEX_FORMAT.parseHex(value);
    }

    /**
     * 将字节数组编码为 Base64 字符串。
     *
     * @param bytes 字节数组，不能为 null
     * @return Base64 字符串
     */
    public static String base64Encode(byte[] bytes) {
        return Base64.getEncoder().encodeToString(requireNonNull(bytes, "bytes"));
    }

    /**
     * 将 Base64 字符串解码为字节数组。
     *
     * @param base64 Base64 字符串，不能空白
     * @return 字节数组
     */
    public static byte[] base64Decode(String base64) {
        try {
            return Base64.getDecoder().decode(normalizeBase64(base64));
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("base64 格式非法", e);
        }
    }

    /**
     * 将字节数组编码为 URL 安全 Base64 字符串，不包含填充符。
     *
     * @param bytes 字节数组，不能为 null
     * @return URL 安全 Base64 字符串
     */
    public static String base64UrlEncode(byte[] bytes) {
        return Base64.getUrlEncoder().withoutPadding().encodeToString(requireNonNull(bytes, "bytes"));
    }

    /**
     * 将 URL 安全 Base64 字符串解码为字节数组。
     *
     * @param text URL 安全 Base64 字符串，不能空白
     * @return 字节数组
     */
    public static byte[] base64UrlDecode(String text) {
        try {
            return Base64.getUrlDecoder().decode(requireNotBlank(text, "text"));
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("base64 url 格式非法", e);
        }
    }

    /**
     * 将字节数组编码为适合 PEM 内容的换行 Base64 字符串。
     *
     * @param bytes 字节数组，不能为 null
     * @return 每 64 字符换行的 Base64 字符串
     */
    public static String pemBase64Encode(byte[] bytes) {
        String base64 = base64Encode(bytes);
        StringBuilder builder = new StringBuilder(base64.length() + base64.length() / 64);
        for (int i = 0; i < base64.length(); i += 64) {
            builder.append(base64, i, Math.min(i + 64, base64.length())).append('\n');
        }
        return builder.toString();
    }

    /**
     * 标准化 Base64 文本，移除空白字符。
     *
     * @param text Base64 文本，不能空白
     * @return 标准化后的 Base64 文本
     */
    public static String normalizeBase64(String text) {
        return requireNotBlank(text, "text").replaceAll("\\s+", "");
    }

    /**
     * 判断字符串是否为合法 Hex。
     *
     * @param text 待判断字符串
     * @return 是 Hex 返回 true
     */
    public static boolean isHex(String text) {
        return text != null && !text.isBlank() && (text.length() & 1) == 0 && HEX_PATTERN.matcher(text).matches();
    }

    /**
     * 判断字符串是否为合法 Base64 或 URL 安全 Base64。
     *
     * @param text 待判断字符串
     * @return 是 Base64 返回 true
     */
    public static boolean isBase64(String text) {
        if (text == null || text.isBlank()) {
            return false;
        }
        String normalized = text.replaceAll("\\s+", "");
        if (!BASE64_PATTERN.matcher(normalized).matches() && !BASE64_URL_PATTERN.matcher(normalized).matches()) {
            return false;
        }
        try {
            if (normalized.indexOf('-') >= 0 || normalized.indexOf('_') >= 0) {
                Base64.getUrlDecoder().decode(normalized);
            } else {
                Base64.getDecoder().decode(normalized);
            }
            return true;
        } catch (IllegalArgumentException e) {
            return false;
        }
    }

    /**
     * 生成 AES 密钥。
     *
     * @param bitLength 密钥位数，只支持 128、192、256
     * @return AES 密钥
     */
    public static SecretKey generateAesKey(int bitLength) {
        validateAesBitLength(bitLength);
        try {
            KeyGenerator generator = KeyGenerator.getInstance("AES");
            generator.init(bitLength, SECURE_RANDOM);
            return generator.generateKey();
        } catch (GeneralSecurityException e) {
            throw new IllegalStateException("生成 AES 密钥失败", e);
        }
    }

    /**
     * 生成 Base64 格式的 AES 密钥。
     *
     * @param bitLength 密钥位数，只支持 128、192、256
     * @return Base64 AES 密钥
     */
    public static String generateAesKeyBase64(int bitLength) {
        return secretKeyToBase64(generateAesKey(bitLength));
    }

    /**
     * 生成 HMAC-SHA256 密钥。
     *
     * @return HMAC-SHA256 密钥
     */
    public static SecretKey generateHmacSha256Key() {
        return generateMacKey("HmacSHA256", 256);
    }

    /**
     * 生成 HMAC-SHA512 密钥。
     *
     * @return HMAC-SHA512 密钥
     */
    public static SecretKey generateHmacSha512Key() {
        return generateMacKey("HmacSHA512", 512);
    }

    /**
     * 生成 RSA 密钥对。
     *
     * @param bitLength 密钥位数，建议不小于 2048
     * @return RSA 密钥对
     */
    public static KeyPair generateRsaKeyPair(int bitLength) {
        if (bitLength < 2048) {
            throw new IllegalArgumentException("RSA 密钥长度不能小于 2048 位");
        }
        try {
            KeyPairGenerator generator = KeyPairGenerator.getInstance("RSA");
            generator.initialize(bitLength, SECURE_RANDOM);
            return generator.generateKeyPair();
        } catch (GeneralSecurityException e) {
            throw new IllegalStateException("生成 RSA 密钥对失败", e);
        }
    }

    /**
     * 生成 EC 密钥对。
     *
     * @param curveName 曲线名称，例如 secp256r1
     * @return EC 密钥对
     */
    public static KeyPair generateEcKeyPair(String curveName) {
        try {
            KeyPairGenerator generator = KeyPairGenerator.getInstance("EC");
            generator.initialize(new ECGenParameterSpec(requireNotBlank(curveName, "curveName")), SECURE_RANDOM);
            return generator.generateKeyPair();
        } catch (GeneralSecurityException e) {
            throw new IllegalStateException("生成 EC 密钥对失败", e);
        }
    }

    /**
     * 生成 Ed25519 密钥对。
     *
     * @return Ed25519 密钥对
     */
    public static KeyPair generateEd25519KeyPair() {
        try {
            KeyPairGenerator generator = KeyPairGenerator.getInstance("Ed25519");
            return generator.generateKeyPair();
        } catch (GeneralSecurityException e) {
            throw new IllegalStateException("生成 Ed25519 密钥对失败", e);
        }
    }

    /**
     * 使用 PBKDF2 从密码派生密钥字节。
     *
     * @param password   密码，不能空白
     * @param salt       盐值，不能为 null 或空数组
     * @param iterations 迭代次数，必须大于 0
     * @param bitLength  密钥位数，必须大于 0
     * @param algorithm  PBKDF2 算法名称，不能空白
     * @return 派生后的密钥字节
     */
    public static byte[] deriveKeyByPbkdf2(String password, byte[] salt, int iterations, int bitLength, String algorithm) {
        requireNotBlank(password, "password");
        requireNotEmpty(salt, "salt");
        requirePositive(iterations, "iterations");
        requirePositive(bitLength, "bitLength");
        try {
            PBEKeySpec spec = new PBEKeySpec(password.toCharArray(), salt, iterations, bitLength);
            try {
                return SecretKeyFactory.getInstance(requireNotBlank(algorithm, "algorithm")).generateSecret(spec).getEncoded();
            } finally {
                spec.clearPassword();
            }
        } catch (GeneralSecurityException e) {
            throw new IllegalStateException("PBKDF2 密钥派生失败", e);
        }
    }

    /**
     * 使用 PBKDF2 从密码派生 AES 密钥。
     *
     * @param password   密码，不能空白
     * @param salt       盐值，不能为 null 或空数组
     * @param iterations 迭代次数，必须大于 0
     * @param bitLength  AES 密钥位数，只支持 128、192、256
     * @return AES 密钥
     */
    public static SecretKey deriveAesKeyByPbkdf2(String password, byte[] salt, int iterations, int bitLength) {
        validateAesBitLength(bitLength);
        return toAesKey(deriveKeyByPbkdf2(password, salt, iterations, bitLength, DEFAULT_PASSWORD_ALGORITHM));
    }

    /**
     * 将字节数组转换为指定算法的 SecretKey。
     *
     * @param keyBytes  密钥字节，不能为 null 或空数组
     * @param algorithm 算法名称，不能空白
     * @return SecretKey
     */
    public static SecretKey toSecretKey(byte[] keyBytes, String algorithm) {
        requireNotEmpty(keyBytes, "keyBytes");
        return new SecretKeySpec(keyBytes.clone(), requireNotBlank(algorithm, "algorithm"));
    }

    /**
     * 将字节数组转换为 AES 密钥。
     *
     * @param keyBytes AES 密钥字节，长度必须为 16、24 或 32
     * @return AES 密钥
     */
    public static SecretKey toAesKey(byte[] keyBytes) {
        validateAesKey(keyBytes);
        return toSecretKey(keyBytes, "AES");
    }

    /**
     * 将 X.509 编码字节转换为公钥。
     *
     * @param keyBytes  公钥编码字节，不能为空
     * @param algorithm 算法名称，例如 RSA、EC、Ed25519
     * @return 公钥
     */
    public static PublicKey toPublicKey(byte[] keyBytes, String algorithm) {
        requireNotEmpty(keyBytes, "keyBytes");
        try {
            return KeyFactory.getInstance(requireNotBlank(algorithm, "algorithm")).generatePublic(new X509EncodedKeySpec(keyBytes));
        } catch (GeneralSecurityException e) {
            throw new IllegalArgumentException("公钥格式非法", e);
        }
    }

    /**
     * 将 PKCS#8 编码字节转换为私钥。
     *
     * @param keyBytes  私钥编码字节，不能为空
     * @param algorithm 算法名称，例如 RSA、EC、Ed25519
     * @return 私钥
     */
    public static PrivateKey toPrivateKey(byte[] keyBytes, String algorithm) {
        requireNotEmpty(keyBytes, "keyBytes");
        try {
            return KeyFactory.getInstance(requireNotBlank(algorithm, "algorithm")).generatePrivate(new PKCS8EncodedKeySpec(keyBytes));
        } catch (GeneralSecurityException e) {
            throw new IllegalArgumentException("私钥格式非法", e);
        }
    }

    /**
     * 将公钥转换为 Base64 字符串。
     *
     * @param publicKey 公钥，不能为 null
     * @return Base64 公钥
     */
    public static String publicKeyToBase64(PublicKey publicKey) {
        return base64Encode(requireNonNull(publicKey, "publicKey").getEncoded());
    }

    /**
     * 将私钥转换为 Base64 字符串。
     *
     * @param privateKey 私钥，不能为 null
     * @return Base64 私钥
     */
    public static String privateKeyToBase64(PrivateKey privateKey) {
        return base64Encode(requireNonNull(privateKey, "privateKey").getEncoded());
    }

    /**
     * 将对称密钥转换为 Base64 字符串。
     *
     * @param secretKey 对称密钥，不能为 null
     * @return Base64 对称密钥
     */
    public static String secretKeyToBase64(SecretKey secretKey) {
        return base64Encode(requireNonNull(secretKey, "secretKey").getEncoded());
    }

    /**
     * 将 Base64 字符串转换为 AES 密钥。
     *
     * @param base64 Base64 AES 密钥
     * @return AES 密钥
     */
    public static SecretKey base64ToAesKey(String base64) {
        return toAesKey(base64Decode(base64));
    }

    /**
     * 将 Base64 字符串转换为公钥。
     *
     * @param base64    Base64 公钥
     * @param algorithm 算法名称，例如 RSA、EC、Ed25519
     * @return 公钥
     */
    public static PublicKey base64ToPublicKey(String base64, String algorithm) {
        return toPublicKey(base64Decode(base64), algorithm);
    }

    /**
     * 将 Base64 字符串转换为私钥。
     *
     * @param base64    Base64 私钥
     * @param algorithm 算法名称，例如 RSA、EC、Ed25519
     * @return 私钥
     */
    public static PrivateKey base64ToPrivateKey(String base64, String algorithm) {
        return toPrivateKey(base64Decode(base64), algorithm);
    }

    /**
     * 校验 AES 密钥长度是否合法。
     *
     * @param keyBytes AES 密钥字节
     */
    public static void validateAesKeyLength(byte[] keyBytes) {
        validateAesKey(keyBytes);
    }

    /**
     * 校验 RSA 密钥长度是否合法。
     *
     * @param key RSA 密钥
     */
    public static void validateRsaKeyLength(Key key) {
        requireNonNull(key, "key");
        if (!(key instanceof RSAKey rsaKey)) {
            throw new IllegalArgumentException("不是 RSA 密钥");
        }
        if (rsaKey.getModulus().bitLength() < 2048) {
            throw new IllegalArgumentException("RSA 密钥长度不能小于 2048 位");
        }
    }

    /**
     * 获取密钥算法名称。
     *
     * @param key 密钥，不能为 null
     * @return 算法名称
     */
    public static String getKeyAlgorithm(Key key) {
        return requireNonNull(key, "key").getAlgorithm();
    }

    /**
     * 获取密钥编码格式。
     *
     * @param key 密钥，不能为 null
     * @return 编码格式
     */
    public static String getKeyFormat(Key key) {
        return requireNonNull(key, "key").getFormat();
    }

    /**
     * 计算字符串 SHA-256 摘要字节。
     *
     * @param text 字符串，不能为 null
     * @return SHA-256 摘要字节
     */
    public static byte[] sha256(String text) {
        return sha256(toUtf8Bytes(text));
    }

    /**
     * 计算字节数组 SHA-256 摘要字节。
     *
     * @param data 字节数组，不能为 null
     * @return SHA-256 摘要字节
     */
    public static byte[] sha256(byte[] data) {
        return digest(data, "SHA-256");
    }

    /**
     * 计算字符串 SHA-256 Hex 摘要。
     *
     * @param text 字符串，不能为 null
     * @return SHA-256 Hex 摘要
     */
    public static String sha256Hex(String text) {
        return digestHex(toUtf8Bytes(text), "SHA-256");
    }

    /**
     * 计算字符串 SHA-256 Base64 摘要。
     *
     * @param text 字符串，不能为 null
     * @return SHA-256 Base64 摘要
     */
    public static String sha256Base64(String text) {
        return base64Encode(sha256(text));
    }

    /**
     * 计算字符串 SHA-512 摘要字节。
     *
     * @param text 字符串，不能为 null
     * @return SHA-512 摘要字节
     */
    public static byte[] sha512(String text) {
        return digest(toUtf8Bytes(text), "SHA-512");
    }

    /**
     * 计算字符串 SHA-512 Hex 摘要。
     *
     * @param text 字符串，不能为 null
     * @return SHA-512 Hex 摘要
     */
    public static String sha512Hex(String text) {
        return digestHex(toUtf8Bytes(text), "SHA-512");
    }

    /**
     * 计算字符串 SHA-1 Hex 摘要，仅建议兼容旧系统。
     *
     * @param text 字符串，不能为 null
     * @return SHA-1 Hex 摘要
     */
    public static String sha1Hex(String text) {
        return digestHex(toUtf8Bytes(text), "SHA-1");
    }

    /**
     * 计算字符串 MD5 Hex 摘要，仅建议兼容旧系统。
     *
     * @param text 字符串，不能为 null
     * @return MD5 Hex 摘要
     */
    public static String md5Hex(String text) {
        return digestHex(toUtf8Bytes(text), "MD5");
    }

    /**
     * 按指定算法计算字节数组摘要。
     *
     * @param data      字节数组，不能为 null
     * @param algorithm 摘要算法，不能空白
     * @return 摘要字节
     */
    public static byte[] digest(byte[] data, String algorithm) {
        requireNonNull(data, "data");
        try {
            return MessageDigest.getInstance(requireNotBlank(algorithm, "algorithm")).digest(data);
        } catch (GeneralSecurityException e) {
            throw new IllegalArgumentException("摘要算法不支持: " + algorithm, e);
        }
    }

    /**
     * 按指定算法计算字节数组 Hex 摘要。
     *
     * @param data      字节数组，不能为 null
     * @param algorithm 摘要算法，不能空白
     * @return Hex 摘要
     */
    public static String digestHex(byte[] data, String algorithm) {
        return hexEncode(digest(data, algorithm));
    }

    /**
     * 按指定算法计算文件摘要。
     *
     * @param path      文件路径，不能为 null
     * @param algorithm 摘要算法，不能空白
     * @return 摘要字节
     * @throws IOException 文件读取失败时抛出
     */
    public static byte[] digestFile(Path path, String algorithm) throws IOException {
        requireReadableFile(path);
        try {
            MessageDigest digest = MessageDigest.getInstance(requireNotBlank(algorithm, "algorithm"));
            try (InputStream input = Files.newInputStream(path)) {
                byte[] buffer = new byte[8192];
                int read;
                while ((read = input.read(buffer)) != -1) {
                    digest.update(buffer, 0, read);
                }
            }
            return digest.digest();
        } catch (GeneralSecurityException e) {
            throw new IllegalArgumentException("摘要算法不支持: " + algorithm, e);
        }
    }

    /**
     * 计算文件 SHA-256 Hex 摘要。
     *
     * @param path 文件路径，不能为 null
     * @return 文件 SHA-256 Hex 摘要
     * @throws IOException 文件读取失败时抛出
     */
    public static String sha256File(Path path) throws IOException {
        return hexEncode(digestFile(path, "SHA-256"));
    }

    /**
     * 校验数据摘要是否与预期 Hex 摘要一致。
     *
     * @param data        原始数据，不能为 null
     * @param expectedHex 预期 Hex 摘要
     * @param algorithm   摘要算法
     * @return 一致返回 true
     */
    public static boolean verifyDigest(byte[] data, String expectedHex, String algorithm) {
        if (!isHex(expectedHex)) {
            return false;
        }
        return constantTimeEquals(digestHex(data, algorithm), expectedHex.toLowerCase(Locale.ROOT));
    }

    /**
     * 使用常量时间比较两个字符串，降低时序攻击风险。
     *
     * @param a 字符串 a
     * @param b 字符串 b
     * @return 相等返回 true
     */
    public static boolean constantTimeEquals(String a, String b) {
        if (a == null || b == null) {
            return false;
        }
        return constantTimeEquals(a.getBytes(StandardCharsets.UTF_8), b.getBytes(StandardCharsets.UTF_8));
    }

    /**
     * 使用常量时间比较两个字节数组，降低时序攻击风险。
     *
     * @param a 字节数组 a
     * @param b 字节数组 b
     * @return 相等返回 true
     */
    public static boolean constantTimeEquals(byte[] a, byte[] b) {
        if (a == null || b == null) {
            return false;
        }
        return MessageDigest.isEqual(a, b);
    }

    /**
     * 使用 HMAC-SHA256 计算签名字节。
     *
     * @param data 原文，不能为 null
     * @param key  密钥，不能为 null
     * @return HMAC 字节
     */
    public static byte[] hmacSha256(String data, String key) {
        return hmac(toUtf8Bytes(data), toUtf8Bytes(key), "HmacSHA256");
    }

    /**
     * 使用 HMAC-SHA256 计算 Hex 签名。
     *
     * @param data 原文，不能为 null
     * @param key  密钥，不能为 null
     * @return Hex 签名
     */
    public static String hmacSha256Hex(String data, String key) {
        return hexEncode(hmacSha256(data, key));
    }

    /**
     * 使用 HMAC-SHA256 计算 Base64 签名。
     *
     * @param data 原文，不能为 null
     * @param key  密钥，不能为 null
     * @return Base64 签名
     */
    public static String hmacSha256Base64(String data, String key) {
        return base64Encode(hmacSha256(data, key));
    }

    /**
     * 使用 HMAC-SHA512 计算签名字节。
     *
     * @param data 原文，不能为 null
     * @param key  密钥，不能为 null
     * @return HMAC 字节
     */
    public static byte[] hmacSha512(String data, String key) {
        return hmac(toUtf8Bytes(data), toUtf8Bytes(key), "HmacSHA512");
    }

    /**
     * 使用 HMAC-SHA512 计算 Hex 签名。
     *
     * @param data 原文，不能为 null
     * @param key  密钥，不能为 null
     * @return Hex 签名
     */
    public static String hmacSha512Hex(String data, String key) {
        return hexEncode(hmacSha512(data, key));
    }

    /**
     * 使用指定 HMAC 算法计算签名字节。
     *
     * @param data      原文字节，不能为 null
     * @param key       密钥字节，不能为空
     * @param algorithm HMAC 算法，不能空白
     * @return HMAC 签名字节
     */
    public static byte[] hmac(byte[] data, byte[] key, String algorithm) {
        requireNonNull(data, "data");
        requireNotEmpty(key, "key");
        try {
            Mac mac = Mac.getInstance(requireNotBlank(algorithm, "algorithm"));
            mac.init(new SecretKeySpec(key, algorithm));
            return mac.doFinal(data);
        } catch (GeneralSecurityException e) {
            throw new IllegalArgumentException("HMAC 计算失败: " + algorithm, e);
        }
    }

    /**
     * 校验 HMAC-SHA256 Hex 签名。
     *
     * @param data        原文，不能为 null
     * @param key         密钥，不能为 null
     * @param expectedHex 预期 Hex 签名
     * @return 校验通过返回 true
     */
    public static boolean verifyHmacSha256(String data, String key, String expectedHex) {
        return isHex(expectedHex) && constantTimeEquals(hmacSha256Hex(data, key), expectedHex.toLowerCase(Locale.ROOT));
    }

    /**
     * 校验 HMAC-SHA512 Hex 签名。
     *
     * @param data        原文，不能为 null
     * @param key         密钥，不能为 null
     * @param expectedHex 预期 Hex 签名
     * @return 校验通过返回 true
     */
    public static boolean verifyHmacSha512(String data, String key, String expectedHex) {
        return isHex(expectedHex) && constantTimeEquals(hmacSha512Hex(data, key), expectedHex.toLowerCase(Locale.ROOT));
    }

    /**
     * 按指定 HMAC 算法计算文件签名。
     *
     * @param path      文件路径，不能为 null
     * @param key       密钥字节，不能为空
     * @param algorithm HMAC 算法，不能空白
     * @return HMAC 签名字节
     * @throws IOException 文件读取失败时抛出
     */
    public static byte[] hmacFile(Path path, byte[] key, String algorithm) throws IOException {
        requireReadableFile(path);
        requireNotEmpty(key, "key");
        try {
            Mac mac = Mac.getInstance(requireNotBlank(algorithm, "algorithm"));
            mac.init(new SecretKeySpec(key, algorithm));
            try (InputStream input = Files.newInputStream(path)) {
                byte[] buffer = new byte[8192];
                int read;
                while ((read = input.read(buffer)) != -1) {
                    mac.update(buffer, 0, read);
                }
            }
            return mac.doFinal();
        } catch (GeneralSecurityException e) {
            throw new IllegalArgumentException("文件 HMAC 计算失败", e);
        }
    }

    /**
     * 使用 AES-GCM 加密字符串，返回包含 IV 和密文的 Base64 字符串。
     *
     * @param plainText 明文，不能为 null
     * @param key       AES 密钥字节，长度必须为 16、24 或 32
     * @return Base64 密文
     */
    public static String aesGcmEncrypt(String plainText, byte[] key) {
        byte[] iv = aesGcmIv();
        byte[] cipher = aesGcmEncrypt(toUtf8Bytes(plainText), key, iv);
        return base64Encode(combineIvAndCipher(iv, cipher));
    }

    /**
     * 使用 AES-GCM 解密包含 IV 和密文的 Base64 字符串。
     *
     * @param cipherText Base64 密文，不能空白
     * @param key        AES 密钥字节，长度必须为 16、24 或 32
     * @return 明文字符串
     */
    public static String aesGcmDecrypt(String cipherText, byte[] key) {
        byte[][] parts = splitIvAndCipher(base64Decode(cipherText), AES_GCM_IV_BYTES);
        return fromUtf8Bytes(aesGcmDecrypt(parts[1], key, parts[0]));
    }

    /**
     * 使用 AES-GCM 加密字节数组。
     *
     * @param data 明文字节，不能为 null
     * @param key  AES 密钥字节，长度必须为 16、24 或 32
     * @param iv   12 字节 IV
     * @return 密文字节，包含 GCM 认证标签
     */
    public static byte[] aesGcmEncrypt(byte[] data, byte[] key, byte[] iv) {
        requireNonNull(data, "data");
        validateAesKey(key);
        validateIv(iv, AES_GCM_IV_BYTES);
        return doCipher("AES/GCM/NoPadding", Cipher.ENCRYPT_MODE, toAesKey(key), new GCMParameterSpec(AES_GCM_TAG_BITS, iv), data);
    }

    /**
     * 使用 AES-GCM 解密字节数组。
     *
     * @param data 密文字节，不能为 null
     * @param key  AES 密钥字节，长度必须为 16、24 或 32
     * @param iv   12 字节 IV
     * @return 明文字节
     */
    public static byte[] aesGcmDecrypt(byte[] data, byte[] key, byte[] iv) {
        requireNonNull(data, "data");
        validateAesKey(key);
        validateIv(iv, AES_GCM_IV_BYTES);
        return doCipher("AES/GCM/NoPadding", Cipher.DECRYPT_MODE, toAesKey(key), new GCMParameterSpec(AES_GCM_TAG_BITS, iv), data);
    }

    /**
     * 使用 Base64 AES 密钥执行 AES-GCM 加密。
     *
     * @param plainText 明文，不能为 null
     * @param base64Key Base64 AES 密钥
     * @return Base64 密文
     */
    public static String aesGcmEncryptToBase64(String plainText, String base64Key) {
        return aesGcmEncrypt(plainText, base64Decode(base64Key));
    }

    /**
     * 使用 Base64 AES 密钥执行 AES-GCM 解密。
     *
     * @param base64CipherText Base64 密文
     * @param base64Key        Base64 AES 密钥
     * @return 明文字符串
     */
    public static String aesGcmDecryptFromBase64(String base64CipherText, String base64Key) {
        return aesGcmDecrypt(base64CipherText, base64Decode(base64Key));
    }

    /**
     * 使用 AES-CBC 加密字节数组。
     *
     * @param data 明文字节，不能为 null
     * @param key  AES 密钥字节，长度必须为 16、24 或 32
     * @param iv   16 字节 IV
     * @return 密文字节
     */
    public static byte[] aesCbcEncrypt(byte[] data, byte[] key, byte[] iv) {
        requireNonNull(data, "data");
        validateAesKey(key);
        validateIv(iv, AES_CBC_IV_BYTES);
        return doCipher("AES/CBC/PKCS5Padding", Cipher.ENCRYPT_MODE, toAesKey(key), new IvParameterSpec(iv), data);
    }

    /**
     * 使用 AES-CBC 解密字节数组。
     *
     * @param data 密文字节，不能为 null
     * @param key  AES 密钥字节，长度必须为 16、24 或 32
     * @param iv   16 字节 IV
     * @return 明文字节
     */
    public static byte[] aesCbcDecrypt(byte[] data, byte[] key, byte[] iv) {
        requireNonNull(data, "data");
        validateAesKey(key);
        validateIv(iv, AES_CBC_IV_BYTES);
        return doCipher("AES/CBC/PKCS5Padding", Cipher.DECRYPT_MODE, toAesKey(key), new IvParameterSpec(iv), data);
    }

    /**
     * 使用 AES-ECB 加密字节数组，仅建议兼容旧系统。
     *
     * @param data 明文字节，不能为 null
     * @param key  AES 密钥字节，长度必须为 16、24 或 32
     * @return 密文字节
     */
    public static byte[] aesEcbEncrypt(byte[] data, byte[] key) {
        requireNonNull(data, "data");
        validateAesKey(key);
        return doCipher("AES/ECB/PKCS5Padding", Cipher.ENCRYPT_MODE, toAesKey(key), null, data);
    }

    /**
     * 使用 AES-ECB 解密字节数组，仅建议兼容旧系统。
     *
     * @param data 密文字节，不能为 null
     * @param key  AES 密钥字节，长度必须为 16、24 或 32
     * @return 明文字节
     */
    public static byte[] aesEcbDecrypt(byte[] data, byte[] key) {
        requireNonNull(data, "data");
        validateAesKey(key);
        return doCipher("AES/ECB/PKCS5Padding", Cipher.DECRYPT_MODE, toAesKey(key), null, data);
    }

    /**
     * 使用 AES-GCM 加密文件，目标文件会写入魔数、IV 和密文。
     *
     * @param source 源文件路径
     * @param target 目标文件路径
     * @param key    AES 密钥字节
     * @throws IOException 文件读写失败时抛出
     */
    public static void encryptFile(Path source, Path target, byte[] key) throws IOException {
        encryptLargeFile(source, target, key);
    }

    /**
     * 使用 AES-GCM 解密文件。
     *
     * @param source 加密文件路径
     * @param target 解密目标路径
     * @param key    AES 密钥字节
     * @throws IOException 文件读写失败时抛出
     */
    public static void decryptFile(Path source, Path target, byte[] key) throws IOException {
        decryptLargeFile(source, target, key);
    }

    /**
     * 使用 AES-GCM 流式加密大文件。
     *
     * @param source 源文件路径
     * @param target 目标文件路径
     * @param key    AES 密钥字节
     * @throws IOException 文件读写失败时抛出
     */
    public static void encryptLargeFile(Path source, Path target, byte[] key) throws IOException {
        requireReadableFile(source);
        requireNonNull(target, "target");
        validateAesKey(key);
        byte[] iv = aesGcmIv();
        Cipher cipher = initCipher("AES/GCM/NoPadding", Cipher.ENCRYPT_MODE, toAesKey(key), new GCMParameterSpec(AES_GCM_TAG_BITS, iv));
        Path parent = target.getParent();
        if (parent != null) {
            Files.createDirectories(parent);
        }
        try (InputStream input = Files.newInputStream(source);
             OutputStream rawOutput = Files.newOutputStream(target)) {
            rawOutput.write(FILE_AES_GCM_MAGIC);
            rawOutput.write(iv);
            try (CipherOutputStream output = new CipherOutputStream(rawOutput, cipher)) {
                input.transferTo(output);
            }
        }
    }

    /**
     * 使用 AES-GCM 流式解密大文件。
     *
     * @param source 加密文件路径
     * @param target 解密目标路径
     * @param key    AES 密钥字节
     * @throws IOException 文件读写失败时抛出
     */
    public static void decryptLargeFile(Path source, Path target, byte[] key) throws IOException {
        requireReadableFile(source);
        requireNonNull(target, "target");
        validateAesKey(key);
        Path parent = target.getParent();
        if (parent != null) {
            Files.createDirectories(parent);
        }
        try (InputStream rawInput = Files.newInputStream(source)) {
            byte[] magic = rawInput.readNBytes(FILE_AES_GCM_MAGIC.length);
            if (!Arrays.equals(magic, FILE_AES_GCM_MAGIC)) {
                throw new IllegalArgumentException("文件不是 SecurityUtil AES-GCM 加密格式");
            }
            byte[] iv = rawInput.readNBytes(AES_GCM_IV_BYTES);
            validateIv(iv, AES_GCM_IV_BYTES);
            Cipher cipher = initCipher("AES/GCM/NoPadding", Cipher.DECRYPT_MODE, toAesKey(key), new GCMParameterSpec(AES_GCM_TAG_BITS, iv));
            try (CipherInputStream input = new CipherInputStream(rawInput, cipher);
                 OutputStream output = Files.newOutputStream(target)) {
                input.transferTo(output);
            }
        }
    }

    /**
     * 合并 IV 和密文字节。
     *
     * @param iv     IV 字节，不能为空
     * @param cipher 密文字节，不能为空
     * @return 合并后的字节数组
     */
    public static byte[] combineIvAndCipher(byte[] iv, byte[] cipher) {
        requireNotEmpty(iv, "iv");
        requireNotEmpty(cipher, "cipher");
        byte[] result = new byte[iv.length + cipher.length];
        System.arraycopy(iv, 0, result, 0, iv.length);
        System.arraycopy(cipher, 0, result, iv.length, cipher.length);
        return result;
    }

    /**
     * 按指定 IV 长度拆分 IV 和密文字节。
     *
     * @param data     合并后的字节数组
     * @param ivLength IV 长度，必须大于 0
     * @return 二维数组，索引 0 为 IV，索引 1 为密文
     */
    public static byte[][] splitIvAndCipher(byte[] data, int ivLength) {
        requireNotEmpty(data, "data");
        requirePositive(ivLength, "ivLength");
        if (data.length <= ivLength) {
            throw new IllegalArgumentException("数据长度必须大于 IV 长度");
        }
        return new byte[][]{
                Arrays.copyOfRange(data, 0, ivLength),
                Arrays.copyOfRange(data, ivLength, data.length)
        };
    }

    /**
     * 使用 RSA 公钥加密字符串，返回 Base64 密文。
     *
     * @param plainText 明文，不能为 null
     * @param publicKey RSA 公钥
     * @return Base64 密文
     */
    public static String rsaEncryptByPublicKey(String plainText, PublicKey publicKey) {
        return base64Encode(rsaOaepEncrypt(toUtf8Bytes(plainText), publicKey));
    }

    /**
     * 使用 RSA 私钥解密 Base64 密文。
     *
     * @param cipherText Base64 密文
     * @param privateKey RSA 私钥
     * @return 明文字符串
     */
    public static String rsaDecryptByPrivateKey(String cipherText, PrivateKey privateKey) {
        return fromUtf8Bytes(rsaOaepDecrypt(base64Decode(cipherText), privateKey));
    }

    /**
     * 使用 RSA 私钥执行 PKCS#1 v1.5 加密，主要用于兼容旧接口。
     *
     * @param plainText  明文，不能为 null
     * @param privateKey RSA 私钥
     * @return Base64 密文
     */
    public static String rsaEncryptByPrivateKey(String plainText, PrivateKey privateKey) {
        return base64Encode(rsaPkcs1PrivateEncrypt(toUtf8Bytes(plainText), privateKey));
    }

    /**
     * 使用 RSA 公钥执行 PKCS#1 v1.5 解密，主要用于兼容旧接口。
     *
     * @param cipherText Base64 密文
     * @param publicKey  RSA 公钥
     * @return 明文字符串
     */
    public static String rsaDecryptByPublicKey(String cipherText, PublicKey publicKey) {
        return fromUtf8Bytes(rsaPkcs1PublicDecrypt(base64Decode(cipherText), publicKey));
    }

    /**
     * 使用 RSA 公钥加密字节并输出 Base64。
     *
     * @param data      明文字节，不能为 null
     * @param publicKey RSA 公钥
     * @return Base64 密文
     */
    public static String rsaEncryptToBase64(byte[] data, PublicKey publicKey) {
        return base64Encode(rsaOaepEncrypt(data, publicKey));
    }

    /**
     * 使用 RSA 私钥解密 Base64 密文。
     *
     * @param cipherText Base64 密文
     * @param privateKey RSA 私钥
     * @return 明文字节
     */
    public static byte[] rsaDecryptFromBase64(String cipherText, PrivateKey privateKey) {
        return rsaOaepDecrypt(base64Decode(cipherText), privateKey);
    }

    /**
     * 使用 RSA-OAEP SHA-256 加密字节数组。
     *
     * @param data      明文字节，不能为 null
     * @param publicKey RSA 公钥
     * @return 密文字节
     */
    public static byte[] rsaOaepEncrypt(byte[] data, PublicKey publicKey) {
        requireNonNull(data, "data");
        validateRsaKeyLength(publicKey);
        return rsaEncryptByBlock(data, publicKey, "RSA/ECB/OAEPWithSHA-256AndMGF1Padding", getMaxRsaEncryptBlockSize(publicKey, "OAEP-SHA256"));
    }

    /**
     * 使用 RSA-OAEP SHA-256 解密字节数组。
     *
     * @param data       密文字节，不能为 null
     * @param privateKey RSA 私钥
     * @return 明文字节
     */
    public static byte[] rsaOaepDecrypt(byte[] data, PrivateKey privateKey) {
        requireNonNull(data, "data");
        validateRsaKeyLength(privateKey);
        return rsaDecryptByBlock(data, privateKey, "RSA/ECB/OAEPWithSHA-256AndMGF1Padding");
    }

    /**
     * 使用 RSA PKCS#1 v1.5 公钥加密字节数组。
     *
     * @param data      明文字节，不能为 null
     * @param publicKey RSA 公钥
     * @return 密文字节
     */
    public static byte[] rsaPkcs1Encrypt(byte[] data, PublicKey publicKey) {
        requireNonNull(data, "data");
        validateRsaKeyLength(publicKey);
        return rsaEncryptByBlock(data, publicKey, "RSA/ECB/PKCS1Padding", getMaxRsaEncryptBlockSize(publicKey, "PKCS1"));
    }

    /**
     * 使用 RSA PKCS#1 v1.5 私钥解密字节数组。
     *
     * @param data       密文字节，不能为 null
     * @param privateKey RSA 私钥
     * @return 明文字节
     */
    public static byte[] rsaPkcs1Decrypt(byte[] data, PrivateKey privateKey) {
        requireNonNull(data, "data");
        validateRsaKeyLength(privateKey);
        return rsaDecryptByBlock(data, privateKey, "RSA/ECB/PKCS1Padding");
    }

    /**
     * 使用 RSA 公钥保护随机 AES 密钥，并使用 AES-GCM 加密数据。
     *
     * @param data      明文字节，不能为 null
     * @param publicKey RSA 公钥
     * @return Base64 包装数据
     */
    public static String encryptByKeyPair(byte[] data, PublicKey publicKey) {
        SecretKey aesKey = generateAesKey(256);
        byte[] iv = aesGcmIv();
        byte[] cipher = aesGcmEncrypt(data, aesKey.getEncoded(), iv);
        byte[] encryptedKey = rsaOaepEncrypt(aesKey.getEncoded(), publicKey);
        Map<String, Object> wrapper = new LinkedHashMap<>();
        wrapper.put("alg", "RSA-OAEP-256+A256GCM");
        wrapper.put("key", base64UrlEncode(encryptedKey));
        wrapper.put("iv", base64UrlEncode(iv));
        wrapper.put("data", base64UrlEncode(cipher));
        return base64UrlEncode(toJson(wrapper).getBytes(StandardCharsets.UTF_8));
    }

    /**
     * 解密 encryptByKeyPair 生成的混合加密数据。
     *
     * @param wrappedData Base64 包装数据
     * @param privateKey  RSA 私钥
     * @return 明文字节
     */
    public static byte[] decryptByKeyPair(String wrappedData, PrivateKey privateKey) {
        Map<String, Object> wrapper = parseJsonObject(fromUtf8Bytes(base64UrlDecode(wrappedData)));
        byte[] aesKey = rsaOaepDecrypt(base64UrlDecode(String.valueOf(wrapper.get("key"))), privateKey);
        return aesGcmDecrypt(base64UrlDecode(String.valueOf(wrapper.get("data"))), aesKey, base64UrlDecode(String.valueOf(wrapper.get("iv"))));
    }

    /**
     * 获取 RSA 最大单块加密字节数。
     *
     * @param key     RSA 密钥
     * @param padding 填充方式，支持 PKCS1、OAEP-SHA256
     * @return 最大单块加密字节数
     */
    public static int getMaxRsaEncryptBlockSize(Key key, String padding) {
        requireNonNull(key, "key");
        if (!(key instanceof RSAKey rsaKey)) {
            throw new IllegalArgumentException("不是 RSA 密钥");
        }
        int keyBytes = (rsaKey.getModulus().bitLength() + 7) / 8;
        String value = requireNotBlank(padding, "padding").toUpperCase(Locale.ROOT);
        if (value.contains("OAEP")) {
            return keyBytes - 2 * 32 - 2;
        }
        if (value.contains("PKCS1")) {
            return keyBytes - 11;
        }
        throw new IllegalArgumentException("不支持的 RSA 填充方式: " + padding);
    }

    /**
     * 使用 RSA 分段加密。
     *
     * @param data           明文字节，不能为 null
     * @param key            RSA 密钥
     * @param transformation Cipher transformation
     * @param blockSize      单块明文字节数
     * @return 密文字节
     */
    public static byte[] rsaEncryptByBlock(byte[] data, Key key, String transformation, int blockSize) {
        requireNonNull(data, "data");
        requirePositive(blockSize, "blockSize");
        Cipher cipher = initCipher(requireNotBlank(transformation, "transformation"), Cipher.ENCRYPT_MODE, requireNonNull(key, "key"), null);
        return doBlockCipher(data, cipher, blockSize);
    }

    /**
     * 使用 RSA 分段解密。
     *
     * @param data           密文字节，不能为 null
     * @param key            RSA 密钥
     * @param transformation Cipher transformation
     * @return 明文字节
     */
    public static byte[] rsaDecryptByBlock(byte[] data, Key key, String transformation) {
        requireNonNull(data, "data");
        requireNonNull(key, "key");
        if (!(key instanceof RSAKey rsaKey)) {
            throw new IllegalArgumentException("不是 RSA 密钥");
        }
        int blockSize = (rsaKey.getModulus().bitLength() + 7) / 8;
        Cipher cipher = initCipher(requireNotBlank(transformation, "transformation"), Cipher.DECRYPT_MODE, key, null);
        return doBlockCipher(data, cipher, blockSize);
    }

    /**
     * 使用 SHA256withRSA 对字符串签名，返回 Base64 签名。
     *
     * @param data       原文，不能为 null
     * @param privateKey RSA 私钥
     * @return Base64 签名
     */
    public static String signSha256WithRsa(String data, PrivateKey privateKey) {
        return signToBase64(toUtf8Bytes(data), privateKey, "SHA256withRSA");
    }

    /**
     * 使用 SHA256withRSA 验证 Base64 签名。
     *
     * @param data      原文，不能为 null
     * @param signature Base64 签名
     * @param publicKey RSA 公钥
     * @return 验签通过返回 true
     */
    public static boolean verifySha256WithRsa(String data, String signature, PublicKey publicKey) {
        return verifyBase64Signature(toUtf8Bytes(data), signature, publicKey, "SHA256withRSA");
    }

    /**
     * 使用 SHA512withRSA 对字符串签名，返回 Base64 签名。
     *
     * @param data       原文，不能为 null
     * @param privateKey RSA 私钥
     * @return Base64 签名
     */
    public static String signSha512WithRsa(String data, PrivateKey privateKey) {
        return signToBase64(toUtf8Bytes(data), privateKey, "SHA512withRSA");
    }

    /**
     * 使用 SHA512withRSA 验证 Base64 签名。
     *
     * @param data      原文，不能为 null
     * @param signature Base64 签名
     * @param publicKey RSA 公钥
     * @return 验签通过返回 true
     */
    public static boolean verifySha512WithRsa(String data, String signature, PublicKey publicKey) {
        return verifyBase64Signature(toUtf8Bytes(data), signature, publicKey, "SHA512withRSA");
    }

    /**
     * 使用 Ed25519 签名字节数组。
     *
     * @param data       原文字节，不能为 null
     * @param privateKey Ed25519 私钥
     * @return 签名字节
     */
    public static byte[] signEd25519(byte[] data, PrivateKey privateKey) {
        return sign(data, privateKey, "Ed25519");
    }

    /**
     * 使用 Ed25519 验签。
     *
     * @param data      原文字节，不能为 null
     * @param signature 签名字节，不能为 null
     * @param publicKey Ed25519 公钥
     * @return 验签通过返回 true
     */
    public static boolean verifyEd25519(byte[] data, byte[] signature, PublicKey publicKey) {
        return verify(data, signature, publicKey, "Ed25519");
    }

    /**
     * 使用 SHA256withECDSA 签名字节数组。
     *
     * @param data       原文字节，不能为 null
     * @param privateKey EC 私钥
     * @return 签名字节
     */
    public static byte[] signEcdsaSha256(byte[] data, PrivateKey privateKey) {
        return sign(data, privateKey, "SHA256withECDSA");
    }

    /**
     * 使用 SHA256withECDSA 验签。
     *
     * @param data      原文字节，不能为 null
     * @param signature 签名字节，不能为 null
     * @param publicKey EC 公钥
     * @return 验签通过返回 true
     */
    public static boolean verifyEcdsaSha256(byte[] data, byte[] signature, PublicKey publicKey) {
        return verify(data, signature, publicKey, "SHA256withECDSA");
    }

    /**
     * 使用指定算法签名字节数组。
     *
     * @param data       原文字节，不能为 null
     * @param privateKey 私钥，不能为 null
     * @param algorithm  签名算法，不能空白
     * @return 签名字节
     */
    public static byte[] sign(byte[] data, PrivateKey privateKey, String algorithm) {
        requireNonNull(data, "data");
        requireNonNull(privateKey, "privateKey");
        try {
            Signature signer = Signature.getInstance(requireNotBlank(algorithm, "algorithm"));
            signer.initSign(privateKey, SECURE_RANDOM);
            signer.update(data);
            return signer.sign();
        } catch (GeneralSecurityException e) {
            throw new IllegalArgumentException("签名失败: " + algorithm, e);
        }
    }

    /**
     * 使用指定算法验签。
     *
     * @param data      原文字节，不能为 null
     * @param signature 签名字节，不能为 null
     * @param publicKey 公钥，不能为 null
     * @param algorithm 签名算法，不能空白
     * @return 验签通过返回 true
     */
    public static boolean verify(byte[] data, byte[] signature, PublicKey publicKey, String algorithm) {
        requireNonNull(data, "data");
        requireNonNull(signature, "signature");
        requireNonNull(publicKey, "publicKey");
        try {
            Signature verifier = Signature.getInstance(requireNotBlank(algorithm, "algorithm"));
            verifier.initVerify(publicKey);
            verifier.update(data);
            return verifier.verify(signature);
        } catch (GeneralSecurityException e) {
            return false;
        }
    }

    /**
     * 使用指定算法签名并输出 Base64。
     *
     * @param data       原文字节，不能为 null
     * @param privateKey 私钥，不能为 null
     * @param algorithm  签名算法，不能空白
     * @return Base64 签名
     */
    public static String signToBase64(byte[] data, PrivateKey privateKey, String algorithm) {
        return base64Encode(sign(data, privateKey, algorithm));
    }

    /**
     * 验证 Base64 格式签名。
     *
     * @param data      原文字节，不能为 null
     * @param signature Base64 签名
     * @param publicKey 公钥，不能为 null
     * @param algorithm 签名算法，不能空白
     * @return 验签通过返回 true
     */
    public static boolean verifyBase64Signature(byte[] data, String signature, PublicKey publicKey, String algorithm) {
        if (!isBase64(signature)) {
            return false;
        }
        return verify(data, base64Decode(signature), publicKey, algorithm);
    }

    /**
     * 对文件进行数字签名。
     *
     * @param path       文件路径，不能为 null
     * @param privateKey 私钥，不能为 null
     * @param algorithm  签名算法，不能空白
     * @return 签名字节
     * @throws IOException 文件读取失败时抛出
     */
    public static byte[] signFile(Path path, PrivateKey privateKey, String algorithm) throws IOException {
        requireReadableFile(path);
        requireNonNull(privateKey, "privateKey");
        try {
            Signature signer = Signature.getInstance(requireNotBlank(algorithm, "algorithm"));
            signer.initSign(privateKey, SECURE_RANDOM);
            try (InputStream input = Files.newInputStream(path)) {
                byte[] buffer = new byte[8192];
                int read;
                while ((read = input.read(buffer)) != -1) {
                    signer.update(buffer, 0, read);
                }
            }
            return signer.sign();
        } catch (GeneralSecurityException e) {
            throw new IllegalArgumentException("文件签名失败", e);
        }
    }

    /**
     * 验证文件数字签名。
     *
     * @param path      文件路径，不能为 null
     * @param signature 签名字节，不能为 null
     * @param publicKey 公钥，不能为 null
     * @param algorithm 签名算法，不能空白
     * @return 验签通过返回 true
     * @throws IOException 文件读取失败时抛出
     */
    public static boolean verifyFileSignature(Path path, byte[] signature, PublicKey publicKey, String algorithm) throws IOException {
        requireReadableFile(path);
        requireNonNull(signature, "signature");
        requireNonNull(publicKey, "publicKey");
        try {
            Signature verifier = Signature.getInstance(requireNotBlank(algorithm, "algorithm"));
            verifier.initVerify(publicKey);
            try (InputStream input = Files.newInputStream(path)) {
                byte[] buffer = new byte[8192];
                int read;
                while ((read = input.read(buffer)) != -1) {
                    verifier.update(buffer, 0, read);
                }
            }
            return verifier.verify(signature);
        } catch (GeneralSecurityException e) {
            return false;
        }
    }

    /**
     * 读取 PEM 文件内容。
     *
     * @param path PEM 文件路径
     * @return PEM 文本
     * @throws IOException 文件读取失败时抛出
     */
    public static String readPem(Path path) throws IOException {
        requireReadableFile(path);
        return Files.readString(path, StandardCharsets.UTF_8);
    }

    /**
     * 写入 PEM 文件。
     *
     * @param path    文件路径
     * @param type    PEM 类型，例如 PUBLIC KEY、PRIVATE KEY、CERTIFICATE
     * @param content DER 编码内容
     * @throws IOException 文件写入失败时抛出
     */
    public static void writePem(Path path, String type, byte[] content) throws IOException {
        requireNonNull(path, "path");
        requireNotBlank(type, "type");
        requireNotEmpty(content, "content");
        Path parent = path.getParent();
        if (parent != null) {
            Files.createDirectories(parent);
        }
        Files.writeString(path, buildPem(type, content), StandardCharsets.UTF_8);
    }

    /**
     * 从 PEM 文件读取公钥。
     *
     * @param path      PEM 文件路径
     * @param algorithm 算法名称，例如 RSA、EC、Ed25519
     * @return 公钥
     * @throws IOException 文件读取失败时抛出
     */
    public static PublicKey readPublicKeyPem(Path path, String algorithm) throws IOException {
        return pemToPublicKey(readPem(path), algorithm);
    }

    /**
     * 从 PEM 文件读取私钥。
     *
     * @param path      PEM 文件路径
     * @param algorithm 算法名称，例如 RSA、EC、Ed25519
     * @return 私钥
     * @throws IOException 文件读取失败时抛出
     */
    public static PrivateKey readPrivateKeyPem(Path path, String algorithm) throws IOException {
        return pemToPrivateKey(readPem(path), algorithm);
    }

    /**
     * 从 PEM 文件读取 X.509 证书。
     *
     * @param path PEM 文件路径
     * @return X.509 证书
     * @throws IOException 文件读取失败时抛出
     */
    public static X509Certificate readCertificatePem(Path path) throws IOException {
        return pemToCertificate(readPem(path));
    }

    /**
     * 将公钥转换为 PEM 文本。
     *
     * @param publicKey 公钥，不能为 null
     * @return PEM 文本
     */
    public static String publicKeyToPem(PublicKey publicKey) {
        return buildPem("PUBLIC KEY", requireNonNull(publicKey, "publicKey").getEncoded());
    }

    /**
     * 将私钥转换为 PKCS#8 PEM 文本。
     *
     * @param privateKey 私钥，不能为 null
     * @return PEM 文本
     */
    public static String privateKeyToPem(PrivateKey privateKey) {
        return buildPem("PRIVATE KEY", requireNonNull(privateKey, "privateKey").getEncoded());
    }

    /**
     * 将证书转换为 PEM 文本。
     *
     * @param certificate 证书，不能为 null
     * @return PEM 文本
     */
    public static String certificateToPem(Certificate certificate) {
        try {
            return buildPem("CERTIFICATE", requireNonNull(certificate, "certificate").getEncoded());
        } catch (CertificateEncodingException e) {
            throw new IllegalArgumentException("证书编码失败", e);
        }
    }

    /**
     * 将 PEM 文本转换为公钥。
     *
     * @param pem       PEM 文本
     * @param algorithm 算法名称，例如 RSA、EC、Ed25519
     * @return 公钥
     */
    public static PublicKey pemToPublicKey(String pem, String algorithm) {
        validatePemType(pem, "PUBLIC KEY");
        return toPublicKey(base64Decode(removePemHeaderFooter(pem)), algorithm);
    }

    /**
     * 将 PEM 文本转换为私钥，支持 PKCS#8 PRIVATE KEY 和 RSA PRIVATE KEY。
     *
     * @param pem       PEM 文本
     * @param algorithm 算法名称，例如 RSA、EC、Ed25519
     * @return 私钥
     */
    public static PrivateKey pemToPrivateKey(String pem, String algorithm) {
        String type = detectPemType(pem);
        byte[] der = base64Decode(removePemHeaderFooter(pem));
        if ("RSA PRIVATE KEY".equals(type)) {
            if (!"RSA".equalsIgnoreCase(requireNotBlank(algorithm, "algorithm"))) {
                throw new IllegalArgumentException("RSA PRIVATE KEY 只能使用 RSA 算法解析");
            }
            der = wrapRsaPkcs1PrivateKeyToPkcs8(der);
        } else if (!"PRIVATE KEY".equals(type)) {
            throw new IllegalArgumentException("不是支持的私钥 PEM 类型: " + type);
        }
        return toPrivateKey(der, algorithm);
    }

    /**
     * 将 PEM 文本转换为 X.509 证书。
     *
     * @param pem PEM 文本
     * @return X.509 证书
     */
    public static X509Certificate pemToCertificate(String pem) {
        validatePemType(pem, "CERTIFICATE");
        try {
            CertificateFactory factory = CertificateFactory.getInstance("X.509");
            return (X509Certificate) factory.generateCertificate(new ByteArrayInputStream(base64Decode(removePemHeaderFooter(pem))));
        } catch (CertificateException e) {
            throw new IllegalArgumentException("证书 PEM 格式非法", e);
        }
    }

    /**
     * 移除 PEM 头尾和空白字符，仅返回 Base64 内容。
     *
     * @param pem PEM 文本
     * @return Base64 内容
     */
    public static String removePemHeaderFooter(String pem) {
        String text = requireNotBlank(pem, "pem").replace("\r", "");
        return Arrays.stream(text.split("\n"))
                .map(String::trim)
                .filter(line -> !line.isEmpty())
                .filter(line -> !line.startsWith("-----BEGIN "))
                .filter(line -> !line.startsWith("-----END "))
                .collect(Collectors.joining());
    }

    /**
     * 识别 PEM 类型。
     *
     * @param pem PEM 文本
     * @return PEM 类型
     */
    public static String detectPemType(String pem) {
        Matcher matcher = Pattern.compile("-----BEGIN ([A-Z0-9 ]+)-----").matcher(requireNotBlank(pem, "pem"));
        if (!matcher.find()) {
            throw new IllegalArgumentException("PEM 缺少 BEGIN 头");
        }
        return matcher.group(1);
    }

    /**
     * 标准化 PEM 文本。
     *
     * @param pem PEM 文本
     * @return 标准化 PEM 文本
     */
    public static String normalizePem(String pem) {
        String type = detectPemType(pem);
        return buildPem(type, base64Decode(removePemHeaderFooter(pem)));
    }

    /**
     * 从文件加载证书。
     *
     * @param path 证书文件路径
     * @return 证书
     * @throws IOException 文件读取失败时抛出
     */
    public static Certificate loadCertificate(Path path) throws IOException {
        requireReadableFile(path);
        try (InputStream input = Files.newInputStream(path)) {
            return CertificateFactory.getInstance("X.509").generateCertificate(input);
        } catch (CertificateException e) {
            throw new IllegalArgumentException("证书格式非法", e);
        }
    }

    /**
     * 从文件加载 X.509 证书。
     *
     * @param path 证书文件路径
     * @return X.509 证书
     * @throws IOException 文件读取失败时抛出
     */
    public static X509Certificate loadX509Certificate(Path path) throws IOException {
        return (X509Certificate) loadCertificate(path);
    }

    /**
     * 从文件加载 X.509 证书链。
     *
     * @param path 证书链文件路径
     * @return X.509 证书列表
     * @throws IOException 文件读取失败时抛出
     */
    public static List<X509Certificate> loadCertificateChain(Path path) throws IOException {
        requireReadableFile(path);
        try (InputStream input = Files.newInputStream(path)) {
            Collection<? extends Certificate> certificates = CertificateFactory.getInstance("X.509").generateCertificates(input);
            return certificates.stream().map(X509Certificate.class::cast).toList();
        } catch (CertificateException e) {
            throw new IllegalArgumentException("证书链格式非法", e);
        }
    }

    /**
     * 获取证书主体。
     *
     * @param certificate X.509 证书
     * @return 主体名称
     */
    public static String getSubject(X509Certificate certificate) {
        return requireNonNull(certificate, "certificate").getSubjectX500Principal().getName();
    }

    /**
     * 获取证书颁发者。
     *
     * @param certificate X.509 证书
     * @return 颁发者名称
     */
    public static String getIssuer(X509Certificate certificate) {
        return requireNonNull(certificate, "certificate").getIssuerX500Principal().getName();
    }

    /**
     * 获取证书序列号。
     *
     * @param certificate X.509 证书
     * @return 证书序列号
     */
    public static BigInteger getSerialNumber(X509Certificate certificate) {
        return requireNonNull(certificate, "certificate").getSerialNumber();
    }

    /**
     * 获取证书生效时间。
     *
     * @param certificate X.509 证书
     * @return 生效时间
     */
    public static Instant getNotBefore(X509Certificate certificate) {
        return requireNonNull(certificate, "certificate").getNotBefore().toInstant();
    }

    /**
     * 获取证书过期时间。
     *
     * @param certificate X.509 证书
     * @return 过期时间
     */
    public static Instant getNotAfter(X509Certificate certificate) {
        return requireNonNull(certificate, "certificate").getNotAfter().toInstant();
    }

    /**
     * 判断证书当前是否过期或尚未生效。
     *
     * @param certificate X.509 证书
     * @return 过期或尚未生效返回 true
     */
    public static boolean isExpired(X509Certificate certificate) {
        try {
            requireNonNull(certificate, "certificate").checkValidity();
            return false;
        } catch (CertificateException e) {
            return true;
        }
    }

    /**
     * 判断证书是否为自签名证书。
     *
     * @param certificate X.509 证书
     * @return 自签名返回 true
     */
    public static boolean isSelfSigned(X509Certificate certificate) {
        requireNonNull(certificate, "certificate");
        try {
            certificate.verify(certificate.getPublicKey());
            return certificate.getSubjectX500Principal().equals(certificate.getIssuerX500Principal());
        } catch (GeneralSecurityException e) {
            return false;
        }
    }

    /**
     * 使用颁发者证书校验证书签名。
     *
     * @param certificate       待校验证书
     * @param issuerCertificate 颁发者证书
     * @return 校验通过返回 true
     */
    public static boolean verifyCertificate(X509Certificate certificate, X509Certificate issuerCertificate) {
        requireNonNull(certificate, "certificate");
        requireNonNull(issuerCertificate, "issuerCertificate");
        try {
            certificate.verify(issuerCertificate.getPublicKey());
            certificate.checkValidity();
            return true;
        } catch (GeneralSecurityException e) {
            return false;
        }
    }

    /**
     * 加载 KeyStore。
     *
     * @param path     KeyStore 文件路径
     * @param password KeyStore 密码，可以为 null
     * @param type     KeyStore 类型，例如 JKS、PKCS12
     * @return KeyStore
     * @throws IOException 文件读取失败时抛出
     */
    public static KeyStore loadKeyStore(Path path, String password, String type) throws IOException {
        requireReadableFile(path);
        try (InputStream input = Files.newInputStream(path)) {
            KeyStore keyStore = KeyStore.getInstance(requireNotBlank(type, "type"));
            keyStore.load(input, password == null ? null : password.toCharArray());
            return keyStore;
        } catch (GeneralSecurityException e) {
            throw new IllegalArgumentException("KeyStore 加载失败", e);
        }
    }

    /**
     * 加载 JKS KeyStore。
     *
     * @param path     JKS 文件路径
     * @param password 密码，可以为 null
     * @return KeyStore
     * @throws IOException 文件读取失败时抛出
     */
    public static KeyStore loadJks(Path path, String password) throws IOException {
        return loadKeyStore(path, password, "JKS");
    }

    /**
     * 加载 PKCS12 KeyStore。
     *
     * @param path     PKCS12 文件路径
     * @param password 密码，可以为 null
     * @return KeyStore
     * @throws IOException 文件读取失败时抛出
     */
    public static KeyStore loadPkcs12(Path path, String password) throws IOException {
        return loadKeyStore(path, password, "PKCS12");
    }

    /**
     * 从 KeyStore 获取私钥。
     *
     * @param keyStore KeyStore，不能为 null
     * @param alias    别名，不能空白
     * @param password 私钥密码，可以为 null
     * @return 私钥
     */
    public static PrivateKey getPrivateKey(KeyStore keyStore, String alias, String password) {
        requireNonNull(keyStore, "keyStore");
        try {
            Key key = keyStore.getKey(requireNotBlank(alias, "alias"), password == null ? null : password.toCharArray());
            if (key instanceof PrivateKey privateKey) {
                return privateKey;
            }
            throw new IllegalArgumentException("指定别名不是私钥: " + alias);
        } catch (GeneralSecurityException e) {
            throw new IllegalArgumentException("获取私钥失败", e);
        }
    }

    /**
     * 从 KeyStore 获取证书。
     *
     * @param keyStore KeyStore，不能为 null
     * @param alias    别名，不能空白
     * @return 证书
     */
    public static Certificate getCertificate(KeyStore keyStore, String alias) {
        requireNonNull(keyStore, "keyStore");
        try {
            Certificate certificate = keyStore.getCertificate(requireNotBlank(alias, "alias"));
            if (certificate == null) {
                throw new IllegalArgumentException("证书别名不存在: " + alias);
            }
            return certificate;
        } catch (GeneralSecurityException e) {
            throw new IllegalArgumentException("获取证书失败", e);
        }
    }

    /**
     * 获取 KeyStore 中的所有别名。
     *
     * @param keyStore KeyStore，不能为 null
     * @return 别名列表
     */
    public static List<String> listAliases(KeyStore keyStore) {
        requireNonNull(keyStore, "keyStore");
        try {
            List<String> aliases = new ArrayList<>();
            Enumeration<String> enumeration = keyStore.aliases();
            while (enumeration.hasMoreElements()) {
                aliases.add(enumeration.nextElement());
            }
            return aliases;
        } catch (GeneralSecurityException e) {
            throw new IllegalArgumentException("读取 KeyStore 别名失败", e);
        }
    }

    /**
     * 判断 KeyStore 是否包含指定别名。
     *
     * @param keyStore KeyStore，不能为 null
     * @param alias    别名，不能空白
     * @return 包含返回 true
     */
    public static boolean containsAlias(KeyStore keyStore, String alias) {
        requireNonNull(keyStore, "keyStore");
        try {
            return keyStore.containsAlias(requireNotBlank(alias, "alias"));
        } catch (GeneralSecurityException e) {
            throw new IllegalArgumentException("判断 KeyStore 别名失败", e);
        }
    }

    /**
     * 使用默认 PBKDF2 参数生成密码哈希。
     *
     * @param rawPassword 原始密码，不能空白
     * @return 编码后的密码哈希
     */
    public static String hashPassword(String rawPassword) {
        return hashPasswordByPbkdf2(rawPassword, DEFAULT_PASSWORD_ITERATIONS, DEFAULT_PASSWORD_HASH_BITS);
    }

    /**
     * 校验原始密码是否匹配编码后的密码哈希。
     *
     * @param rawPassword     原始密码，不能空白
     * @param encodedPassword 编码后的密码哈希
     * @return 匹配返回 true
     */
    public static boolean verifyPassword(String rawPassword, String encodedPassword) {
        return verifyPbkdf2Password(rawPassword, encodedPassword);
    }

    /**
     * 使用 PBKDF2 生成密码哈希。
     *
     * @param rawPassword 原始密码，不能空白
     * @return 编码后的密码哈希
     */
    public static String hashPasswordByPbkdf2(String rawPassword) {
        return hashPasswordByPbkdf2(rawPassword, DEFAULT_PASSWORD_ITERATIONS, DEFAULT_PASSWORD_HASH_BITS);
    }

    /**
     * 使用指定 PBKDF2 参数生成密码哈希。
     *
     * @param rawPassword 原始密码，不能空白
     * @param iterations  迭代次数，必须大于 0
     * @param hashBits    哈希位数，必须大于 0
     * @return 编码后的密码哈希
     */
    public static String hashPasswordByPbkdf2(String rawPassword, int iterations, int hashBits) {
        requireNotBlank(rawPassword, "rawPassword");
        requirePositive(iterations, "iterations");
        requirePositive(hashBits, "hashBits");
        byte[] salt = generatePasswordSalt();
        byte[] hash = deriveKeyByPbkdf2(rawPassword, salt, iterations, hashBits, DEFAULT_PASSWORD_ALGORITHM);
        return String.join("$", DEFAULT_PASSWORD_ALGORITHM, String.valueOf(iterations), base64UrlEncode(salt), String.valueOf(hashBits), base64UrlEncode(hash));
    }

    /**
     * 校验 PBKDF2 密码哈希。
     *
     * @param rawPassword     原始密码，不能空白
     * @param encodedPassword 编码后的密码哈希
     * @return 匹配返回 true
     */
    public static boolean verifyPbkdf2Password(String rawPassword, String encodedPassword) {
        requireNotBlank(rawPassword, "rawPassword");
        try {
            String[] parts = requireNotBlank(encodedPassword, "encodedPassword").split("\\$");
            if (parts.length != 5) {
                return false;
            }
            String algorithm = parts[0];
            int iterations = Integer.parseInt(parts[1]);
            byte[] salt = base64UrlDecode(parts[2]);
            int hashBits = Integer.parseInt(parts[3]);
            byte[] expected = base64UrlDecode(parts[4]);
            byte[] actual = deriveKeyByPbkdf2(rawPassword, salt, iterations, hashBits, algorithm);
            return constantTimeEquals(actual, expected);
        } catch (RuntimeException e) {
            return false;
        }
    }

    /**
     * 生成密码盐值。
     *
     * @return 16 字节盐值
     */
    public static byte[] generatePasswordSalt() {
        return salt(16);
    }

    /**
     * 计算密码强度分数，范围 0 到 5。
     *
     * @param password 密码，可以为 null
     * @return 强度分数
     */
    public static int checkPasswordStrength(String password) {
        if (password == null || password.isBlank()) {
            return 0;
        }
        int score = 0;
        if (password.length() >= 8) score++;
        if (password.length() >= 12) score++;
        if (password.chars().anyMatch(Character::isUpperCase) && password.chars().anyMatch(Character::isLowerCase))
            score++;
        if (password.chars().anyMatch(Character::isDigit)) score++;
        if (password.chars().anyMatch(ch -> !Character.isLetterOrDigit(ch))) score++;
        if (isWeakPassword(password) || containsSequentialChars(password) || containsRepeatedChars(password)) {
            score = Math.max(0, score - 2);
        }
        return Math.min(score, 5);
    }

    /**
     * 判断密码是否为弱密码。
     *
     * @param password 密码，可以为 null
     * @return 弱密码返回 true
     */
    public static boolean isWeakPassword(String password) {
        return password == null || password.isBlank() || WEAK_PASSWORDS.contains(password.toLowerCase(Locale.ROOT));
    }

    /**
     * 判断密码是否包含连续字符。
     *
     * @param password 密码，可以为 null
     * @return 包含连续字符返回 true
     */
    public static boolean containsSequentialChars(String password) {
        if (password == null || password.length() < 3) {
            return false;
        }
        String lower = password.toLowerCase(Locale.ROOT);
        for (int i = 0; i <= lower.length() - 3; i++) {
            char a = lower.charAt(i);
            char b = lower.charAt(i + 1);
            char c = lower.charAt(i + 2);
            if (b == a + 1 && c == b + 1) {
                return true;
            }
        }
        return false;
    }

    /**
     * 判断密码是否包含连续重复字符。
     *
     * @param password 密码，可以为 null
     * @return 包含连续重复字符返回 true
     */
    public static boolean containsRepeatedChars(String password) {
        if (password == null || password.length() < 3) {
            return false;
        }
        for (int i = 0; i <= password.length() - 3; i++) {
            if (password.charAt(i) == password.charAt(i + 1) && password.charAt(i) == password.charAt(i + 2)) {
                return true;
            }
        }
        return false;
    }

    /**
     * 对密码进行脱敏展示。
     *
     * @param password 密码，可以为 null
     * @return 脱敏结果
     */
    public static String maskPassword(String password) {
        if (password == null || password.isEmpty()) {
            return "";
        }
        return "******";
    }

    /**
     * 生成随机强密码。
     *
     * @param length 密码长度，必须不小于 8
     * @return 随机密码
     */
    public static String generateRandomPassword(int length) {
        if (length < 8) {
            throw new IllegalArgumentException("密码长度不能小于 8");
        }
        String upper = "ABCDEFGHIJKLMNOPQRSTUVWXYZ";
        String lower = "abcdefghijklmnopqrstuvwxyz";
        String digits = "0123456789";
        String special = "!@#$%^&*()-_=+[]{}";
        String all = upper + lower + digits + special;
        StringBuilder builder = new StringBuilder(length);
        builder.append(upper.charAt(SECURE_RANDOM.nextInt(upper.length())));
        builder.append(lower.charAt(SECURE_RANDOM.nextInt(lower.length())));
        builder.append(digits.charAt(SECURE_RANDOM.nextInt(digits.length())));
        builder.append(special.charAt(SECURE_RANDOM.nextInt(special.length())));
        while (builder.length() < length) {
            builder.append(all.charAt(SECURE_RANDOM.nextInt(all.length())));
        }
        List<Character> chars = new ArrayList<>();
        for (int i = 0; i < builder.length(); i++) {
            chars.add(builder.charAt(i));
        }
        Collections.shuffle(chars, SECURE_RANDOM);
        StringBuilder result = new StringBuilder(length);
        chars.forEach(result::append);
        return result.toString();
    }

    /**
     * 判断密码哈希参数是否需要升级。
     *
     * @param encodedPassword 编码后的密码哈希
     * @return 需要升级返回 true
     */
    public static boolean needRehash(String encodedPassword) {
        try {
            String[] parts = requireNotBlank(encodedPassword, "encodedPassword").split("\\$");
            return parts.length != 5
                    || !DEFAULT_PASSWORD_ALGORITHM.equals(parts[0])
                    || Integer.parseInt(parts[1]) < DEFAULT_PASSWORD_ITERATIONS
                    || Integer.parseInt(parts[3]) < DEFAULT_PASSWORD_HASH_BITS;
        } catch (RuntimeException e) {
            return true;
        }
    }

    /**
     * 创建 HS256 JWT Token。
     *
     * @param claims 自定义 Claims，不能为 null
     * @param secret HMAC 密钥，不能空白
     * @param ttl    有效期，必须大于 0
     * @return JWT Token
     */
    public static String createToken(Map<String, Object> claims, String secret, Duration ttl) {
        return createJwt(claims, secret, ttl, "HS256");
    }

    /**
     * 创建访问 Token。
     *
     * @param subject 主体标识，不能空白
     * @param claims  自定义 Claims，可以为 null
     * @param secret  HMAC 密钥，不能空白
     * @param ttl     有效期，必须大于 0
     * @return JWT 访问 Token
     */
    public static String createAccessToken(String subject, Map<String, Object> claims, String secret, Duration ttl) {
        Map<String, Object> values = new LinkedHashMap<>();
        if (claims != null) {
            values.putAll(claims);
        }
        values.put("sub", requireNotBlank(subject, "subject"));
        values.put("typ", "access");
        return createToken(values, secret, ttl);
    }

    /**
     * 创建刷新 Token。
     *
     * @param subject 主体标识，不能空白
     * @param secret  HMAC 密钥，不能空白
     * @param ttl     有效期，必须大于 0
     * @return JWT 刷新 Token
     */
    public static String createRefreshToken(String subject, String secret, Duration ttl) {
        Map<String, Object> values = new LinkedHashMap<>();
        values.put("sub", requireNotBlank(subject, "subject"));
        values.put("typ", "refresh");
        return createToken(values, secret, ttl);
    }

    /**
     * 解析 JWT Payload，不校验签名。
     *
     * @param token  JWT Token
     * @param secret HMAC 密钥，保留用于接口一致性，不能空白
     * @return Claims
     */
    public static Map<String, Object> parseToken(String token, String secret) {
        requireNotBlank(secret, "secret");
        return getClaims(token);
    }

    /**
     * 校验 JWT 签名和过期时间。
     *
     * @param token  JWT Token
     * @param secret HMAC 密钥，不能空白
     * @return 校验通过返回 true
     */
    public static boolean verifyToken(String token, String secret) {
        try {
            String[] parts = splitJwt(token);
            Map<String, Object> header = parseJsonObject(fromUtf8Bytes(base64UrlDecode(parts[0])));
            String alg = String.valueOf(header.get("alg"));
            byte[] expected = jwtSign(parts[0] + "." + parts[1], secret, alg);
            if (!constantTimeEquals(expected, base64UrlDecode(parts[2]))) {
                return false;
            }
            return !isExpired(token);
        } catch (RuntimeException e) {
            return false;
        }
    }

    /**
     * 判断 JWT 是否已经过期。
     *
     * @param token JWT Token
     * @return 过期返回 true
     */
    public static boolean isExpired(String token) {
        Instant expireTime = getExpireTime(token);
        return expireTime != null && !expireTime.isAfter(Instant.now());
    }

    /**
     * 获取 JWT Subject。
     *
     * @param token JWT Token
     * @return Subject，可能为 null
     */
    public static String getSubject(String token) {
        Object value = getClaim(token, "sub");
        return value == null ? null : String.valueOf(value);
    }

    /**
     * 获取 JWT 指定 Claim。
     *
     * @param token JWT Token
     * @param name  Claim 名称，不能空白
     * @return Claim 值，可能为 null
     */
    public static Object getClaim(String token, String name) {
        return getClaims(token).get(requireNotBlank(name, "name"));
    }

    /**
     * 获取 JWT 所有 Claims，不校验签名。
     *
     * @param token JWT Token
     * @return Claims
     */
    public static Map<String, Object> getClaims(String token) {
        String[] parts = splitJwt(token);
        return parseJsonObject(fromUtf8Bytes(base64UrlDecode(parts[1])));
    }

    /**
     * 获取 JWT 过期时间。
     *
     * @param token JWT Token
     * @return 过期时间，缺少 exp 时返回 null
     */
    public static Instant getExpireTime(String token) {
        Object exp = getClaims(token).get("exp");
        return exp == null ? null : Instant.ofEpochSecond(toLong(exp, "exp"));
    }

    /**
     * 获取 JWT 签发时间。
     *
     * @param token JWT Token
     * @return 签发时间，缺少 iat 时返回 null
     */
    public static Instant getIssuedAt(String token) {
        Object iat = getClaims(token).get("iat");
        return iat == null ? null : Instant.ofEpochSecond(toLong(iat, "iat"));
    }

    /**
     * 刷新 JWT Token，保留原有 Claims 并重新设置时间。
     *
     * @param token  原 JWT Token
     * @param secret HMAC 密钥，不能空白
     * @param ttl    新有效期，必须大于 0
     * @return 新 JWT Token
     */
    public static String refreshToken(String token, String secret, Duration ttl) {
        Map<String, Object> claims = new LinkedHashMap<>(getClaims(token));
        claims.remove("iat");
        claims.remove("exp");
        return createToken(claims, secret, ttl);
    }

    /**
     * 从 Authorization 请求头中提取 Bearer Token。
     *
     * @param authorization Authorization 请求头
     * @return Token 字符串
     */
    public static String extractBearerToken(String authorization) {
        String value = requireNotBlank(authorization, "authorization");
        if (!value.regionMatches(true, 0, "Bearer ", 0, 7)) {
            throw new IllegalArgumentException("不是 Bearer Token");
        }
        return requireNotBlank(value.substring(7).trim(), "token");
    }

    /**
     * 构建 Bearer Token 请求头值。
     *
     * @param token Token，不能空白
     * @return Bearer Token 请求头值
     */
    public static String buildBearerToken(String token) {
        return "Bearer " + requireNotBlank(token, "token");
    }

    /**
     * 根据参数构建签名原文，保持 Map 迭代顺序。
     *
     * @param params 参数 Map，不能为 null
     * @return 签名原文
     */
    public static String buildSignText(Map<String, ?> params) {
        return buildQueryString(filterBlankParams(params), false);
    }

    /**
     * 根据参数名升序构建签名原文。
     *
     * @param params 参数 Map，不能为 null
     * @return 排序后的签名原文
     */
    public static String buildSortedSignText(Map<String, ?> params) {
        return buildQueryString(filterBlankParams(params), true);
    }

    /**
     * 构建 query string 风格签名原文。
     *
     * @param params 参数 Map，不能为 null
     * @return query string 原文
     */
    public static String buildQuerySignText(Map<String, ?> params) {
        return buildSortedSignText(params);
    }

    /**
     * 过滤值为空白的参数。
     *
     * @param params 参数 Map，不能为 null
     * @return 过滤后的参数 Map
     */
    public static Map<String, Object> filterBlankParams(Map<String, ?> params) {
        requireNonNull(params, "params");
        Map<String, Object> result = new LinkedHashMap<>();
        for (Map.Entry<String, ?> entry : params.entrySet()) {
            if (entry.getKey() == null || entry.getKey().isBlank()) {
                continue;
            }
            Object value = entry.getValue();
            if (value == null) {
                continue;
            }
            if (value instanceof String text && text.isBlank()) {
                continue;
            }
            result.put(entry.getKey(), value);
        }
        return result;
    }

    /**
     * 过滤签名字段 sign、signature。
     *
     * @param params 参数 Map，不能为 null
     * @return 过滤后的参数 Map
     */
    public static Map<String, Object> filterSignParam(Map<String, ?> params) {
        return filterBlankParams(params).entrySet().stream()
                .filter(entry -> !"sign".equalsIgnoreCase(entry.getKey()) && !"signature".equalsIgnoreCase(entry.getKey()))
                .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue, (a, b) -> b, LinkedHashMap::new));
    }

    /**
     * 使用 HMAC-SHA256 对参数签名。
     *
     * @param params 参数 Map，不能为 null
     * @param secret 签名密钥，不能为 null
     * @return Hex 签名
     */
    public static String signByHmacSha256(Map<String, ?> params, String secret) {
        return hmacSha256Hex(buildSortedSignText(filterSignParam(params)), secret);
    }

    /**
     * 使用 RSA 私钥对参数签名。
     *
     * @param params     参数 Map，不能为 null
     * @param privateKey RSA 私钥
     * @return Base64 签名
     */
    public static String signByRsa(Map<String, ?> params, PrivateKey privateKey) {
        return signSha256WithRsa(buildSortedSignText(filterSignParam(params)), privateKey);
    }

    /**
     * 校验 HMAC-SHA256 参数签名。
     *
     * @param params      参数 Map，不能为 null
     * @param secret      签名密钥，不能为 null
     * @param expectedHex 预期 Hex 签名
     * @return 校验通过返回 true
     */
    public static boolean verifyHmacSha256Sign(Map<String, ?> params, String secret, String expectedHex) {
        return constantTimeEquals(signByHmacSha256(params, secret), expectedHex);
    }

    /**
     * 校验 RSA 参数签名。
     *
     * @param params    参数 Map，不能为 null
     * @param signature Base64 签名
     * @param publicKey RSA 公钥
     * @return 校验通过返回 true
     */
    public static boolean verifyRsaSign(Map<String, ?> params, String signature, PublicKey publicKey) {
        return verifySha256WithRsa(buildSortedSignText(filterSignParam(params)), signature, publicKey);
    }

    /**
     * 校验时间戳是否在允许秒数范围内。
     *
     * @param timestampMillis 请求时间戳，毫秒
     * @param allowedSeconds  允许偏移秒数，必须大于 0
     * @return 在范围内返回 true
     */
    public static boolean verifyTimestamp(long timestampMillis, long allowedSeconds) {
        requirePositive(allowedSeconds, "allowedSeconds");
        long delta = Math.abs(Instant.now().toEpochMilli() - timestampMillis);
        return delta <= Duration.ofSeconds(allowedSeconds).toMillis();
    }

    /**
     * 校验 Nonce 基础格式。
     *
     * @param nonce Nonce 字符串
     * @return 格式合法返回 true
     */
    public static boolean verifyNonce(String nonce) {
        return nonce != null && nonce.length() >= 16 && nonce.length() <= 128 && Pattern.matches("^[A-Za-z0-9_\\-]+$", nonce);
    }

    /**
     * 判断请求是否为重放请求。
     *
     * @param nonce           Nonce 字符串
     * @param timestampMillis 请求时间戳，毫秒
     * @param allowedWindow   允许时间窗口
     * @param usedNonces      已使用 Nonce 集合，调用方可用 Redis 或本地集合传入
     * @return 重放或非法请求返回 true
     */
    public static boolean isReplayRequest(String nonce, long timestampMillis, Duration allowedWindow, Set<String> usedNonces) {
        requireNonNull(allowedWindow, "allowedWindow");
        requireNonNull(usedNonces, "usedNonces");
        if (!verifyNonce(nonce) || !verifyTimestamp(timestampMillis, allowedWindow.toSeconds())) {
            return true;
        }
        return !usedNonces.add(nonce);
    }

    /**
     * 标准化请求头并构建签名原文。
     *
     * @param headers 请求头 Map，不能为 null
     * @return 标准化请求头原文
     */
    public static String canonicalizeHeaders(Map<String, String> headers) {
        requireNonNull(headers, "headers");
        return headers.entrySet().stream()
                .filter(e -> e.getKey() != null && e.getValue() != null)
                .sorted(Map.Entry.comparingByKey(String.CASE_INSENSITIVE_ORDER))
                .map(e -> e.getKey().toLowerCase(Locale.ROOT).trim() + ":" + e.getValue().trim())
                .collect(Collectors.joining("\n"));
    }

    /**
     * 构建完整请求签名原文。
     *
     * @param method      HTTP 方法，不能空白
     * @param path        请求路径，不能空白
     * @param queryParams 查询参数，可以为 null
     * @param headers     请求头，可以为 null
     * @param body        请求体，可以为 null
     * @return 标准化请求签名原文
     */
    public static String canonicalizeRequest(String method, String path, Map<String, ?> queryParams, Map<String, String> headers, byte[] body) {
        String query = queryParams == null ? "" : buildSortedSignText(queryParams);
        String headerText = headers == null ? "" : canonicalizeHeaders(headers);
        String bodyHash = body == null ? sha256Hex("") : hexEncode(sha256(body));
        return requireNotBlank(method, "method").toUpperCase(Locale.ROOT) + "\n"
                + requireNotBlank(path, "path") + "\n"
                + query + "\n"
                + headerText + "\n"
                + bodyHash;
    }

    /**
     * 计算文件 MD5 Hex 摘要，仅建议用于非安全兼容场景。
     *
     * @param path 文件路径
     * @return MD5 Hex 摘要
     * @throws IOException 文件读取失败时抛出
     */
    public static String md5(Path path) throws IOException {
        return hexEncode(digestFile(path, "MD5"));
    }

    /**
     * 计算文件指定算法 Hex 摘要。
     *
     * @param path      文件路径
     * @param algorithm 摘要算法
     * @return Hex 摘要
     * @throws IOException 文件读取失败时抛出
     */
    public static String digest(Path path, String algorithm) throws IOException {
        return hexEncode(digestFile(path, algorithm));
    }

    /**
     * 校验文件摘要是否匹配。
     *
     * @param path        文件路径
     * @param expectedHex 预期 Hex 摘要
     * @param algorithm   摘要算法
     * @return 匹配返回 true
     * @throws IOException 文件读取失败时抛出
     */
    public static boolean verifyFileDigest(Path path, String expectedHex, String algorithm) throws IOException {
        return isHex(expectedHex) && constantTimeEquals(digest(path, algorithm), expectedHex.toLowerCase(Locale.ROOT));
    }

    /**
     * 检测文件 MIME 类型。
     *
     * @param path 文件路径
     * @return MIME 类型，无法识别时返回 application/octet-stream
     * @throws IOException 文件读取失败时抛出
     */
    public static String detectMimeType(Path path) throws IOException {
        requireReadableFile(path);
        String type = Files.probeContentType(path);
        return type == null ? "application/octet-stream" : type;
    }

    /**
     * 校验文件扩展名是否在允许列表内。
     *
     * @param filename 文件名，不能空白
     * @param allowed  允许的扩展名集合，不区分大小写
     * @return 合法返回 true
     */
    public static boolean validateFileExtension(String filename, Set<String> allowed) {
        requireNotBlank(filename, "filename");
        requireNonNull(allowed, "allowed");
        String ext = "";
        int index = filename.lastIndexOf('.');
        if (index >= 0 && index < filename.length() - 1) {
            ext = filename.substring(index + 1).toLowerCase(Locale.ROOT);
        }
        String finalExt = ext;
        return allowed.stream().filter(Objects::nonNull).map(s -> s.replace(".", "").toLowerCase(Locale.ROOT)).anyMatch(finalExt::equals);
    }

    /**
     * 校验文件大小是否不超过限制。
     *
     * @param size    文件大小字节数，不能小于 0
     * @param maxSize 最大字节数，不能小于 0
     * @return 未超过返回 true
     */
    public static boolean validateFileSize(long size, long maxSize) {
        if (size < 0 || maxSize < 0) {
            throw new IllegalArgumentException("文件大小不能小于 0");
        }
        return size <= maxSize;
    }

    /**
     * 清理文件名中的危险字符。
     *
     * @param filename 文件名，不能空白
     * @return 安全文件名
     */
    public static String sanitizeFilename(String filename) {
        String value = requireNotBlank(filename, "filename").replace("\\", "/");
        value = value.substring(value.lastIndexOf('/') + 1);
        value = value.replaceAll("[\\r\\n\\t]", "_").replaceAll("[<>:\"/\\|?*]", "_");
        if (value.equals(".") || value.equals("..") || value.isBlank()) {
            throw new IllegalArgumentException("文件名非法");
        }
        return value;
    }

    /**
     * 判断目标路径是否位于基础目录内。
     *
     * @param baseDir    基础目录
     * @param targetPath 目标路径
     * @return 安全返回 true
     */
    public static boolean isSafePath(Path baseDir, Path targetPath) {
        requireNonNull(baseDir, "baseDir");
        requireNonNull(targetPath, "targetPath");
        Path base = baseDir.toAbsolutePath().normalize();
        Path target = base.resolve(targetPath).toAbsolutePath().normalize();
        return target.startsWith(base);
    }

    /**
     * 读取文件魔数的 Hex 表示。
     *
     * @param path 文件路径
     * @return 最多前 16 字节 Hex 魔数
     * @throws IOException 文件读取失败时抛出
     */
    public static String checkMagicNumber(Path path) throws IOException {
        requireReadableFile(path);
        try (InputStream input = Files.newInputStream(path)) {
            return hexEncode(input.readNBytes(16));
        }
    }

    /**
     * 判断文件是否为可执行文件。
     *
     * @param path 文件路径
     * @return 可执行返回 true
     */
    public static boolean isExecutableFile(Path path) {
        requireNonNull(path, "path");
        if (!Files.isRegularFile(path)) {
            return false;
        }
        String name = path.getFileName().toString().toLowerCase(Locale.ROOT);
        return Files.isExecutable(path) || name.endsWith(".exe") || name.endsWith(".bat") || name.endsWith(".cmd") || name.endsWith(".sh");
    }

    /**
     * 手机号脱敏。
     *
     * @param mobile 手机号
     * @return 脱敏手机号
     */
    public static String maskMobile(String mobile) {
        return mask(mobile, 3, 4);
    }

    /**
     * 邮箱脱敏。
     *
     * @param email 邮箱
     * @return 脱敏邮箱
     */
    public static String maskEmail(String email) {
        if (email == null || email.isBlank()) {
            return "";
        }
        int index = email.indexOf('@');
        if (index <= 0) {
            return mask(email, 1, 1);
        }
        return mask(email.substring(0, index), 1, 0) + email.substring(index);
    }

    /**
     * 身份证号脱敏。
     *
     * @param idCard 身份证号
     * @return 脱敏身份证号
     */
    public static String maskIdCard(String idCard) {
        return mask(idCard, 6, 4);
    }

    /**
     * 银行卡号脱敏。
     *
     * @param bankCard 银行卡号
     * @return 脱敏银行卡号
     */
    public static String maskBankCard(String bankCard) {
        return mask(bankCard, 6, 4);
    }

    /**
     * 姓名脱敏。
     *
     * @param name 姓名
     * @return 脱敏姓名
     */
    public static String maskName(String name) {
        if (name == null || name.isBlank()) {
            return "";
        }
        if (name.length() <= 1) {
            return "*";
        }
        return name.charAt(0) + "*".repeat(name.length() - 1);
    }

    /**
     * 地址脱敏。
     *
     * @param address 地址
     * @return 脱敏地址
     */
    public static String maskAddress(String address) {
        return mask(address, Math.min(6, address == null ? 0 : address.length()), 0);
    }

    /**
     * IP 地址脱敏。
     *
     * @param ip IP 地址
     * @return 脱敏 IP
     */
    public static String maskIp(String ip) {
        if (ip == null || ip.isBlank()) {
            return "";
        }
        if (ip.contains(".")) {
            String[] parts = ip.split("\\.");
            if (parts.length == 4) {
                return parts[0] + "." + parts[1] + ".*.*";
            }
        }
        return mask(ip, 4, 4);
    }

    /**
     * Token 脱敏。
     *
     * @param token Token
     * @return 脱敏 Token
     */
    public static String maskToken(String token) {
        return mask(token, 6, 6);
    }

    /**
     * 密钥脱敏。
     *
     * @param key 密钥
     * @return 脱敏密钥
     */
    public static String maskSecretKey(String key) {
        return mask(key, 4, 4);
    }

    /**
     * 通用脱敏。
     *
     * @param text   原文
     * @param prefix 保留前缀长度，不能小于 0
     * @param suffix 保留后缀长度，不能小于 0
     * @return 脱敏文本
     */
    public static String mask(String text, int prefix, int suffix) {
        if (prefix < 0 || suffix < 0) {
            throw new IllegalArgumentException("保留长度不能小于 0");
        }
        if (text == null || text.isEmpty()) {
            return "";
        }
        int length = text.length();
        if (prefix + suffix >= length) {
            return "*".repeat(length);
        }
        return text.substring(0, prefix) + "*".repeat(length - prefix - suffix) + text.substring(length - suffix);
    }

    /**
     * 使用正则匹配内容并脱敏。
     *
     * @param text    原文
     * @param pattern 正则模式，不能为 null
     * @return 脱敏文本
     */
    public static String maskByPattern(String text, Pattern pattern) {
        if (text == null || text.isEmpty()) {
            return "";
        }
        requireNonNull(pattern, "pattern");
        Matcher matcher = pattern.matcher(text);
        StringBuilder builder = new StringBuilder();
        while (matcher.find()) {
            matcher.appendReplacement(builder, Matcher.quoteReplacement(mask(matcher.group(), 1, 1)));
        }
        matcher.appendTail(builder);
        return builder.toString();
    }

    /**
     * 对简单 JSON 文本中的指定字段值脱敏。
     *
     * @param json   JSON 文本
     * @param fields 字段名集合
     * @return 脱敏后的 JSON 文本
     */
    public static String maskJson(String json, Set<String> fields) {
        if (json == null || json.isBlank()) {
            return "";
        }
        requireNonNull(fields, "fields");
        String result = json;
        for (String field : fields) {
            if (field == null || field.isBlank()) {
                continue;
            }
            Pattern pattern = Pattern.compile("(\\\"" + Pattern.quote(field) + "\\\"\\s*:\\s*\\\")(.*?)(\\\")");
            Matcher matcher = pattern.matcher(result);
            StringBuffer buffer = new StringBuffer();
            while (matcher.find()) {
                matcher.appendReplacement(buffer, Matcher.quoteReplacement(matcher.group(1) + mask(matcher.group(2), 1, 1) + matcher.group(3)));
            }
            matcher.appendTail(buffer);
            result = buffer.toString();
        }
        return result;
    }

    /**
     * 对 Map 中的指定字段脱敏。
     *
     * @param data   数据 Map，不能为 null
     * @param fields 字段名集合，不能为 null
     * @return 脱敏后的新 Map
     */
    public static Map<String, Object> maskMap(Map<String, Object> data, Set<String> fields) {
        requireNonNull(data, "data");
        requireNonNull(fields, "fields");
        Map<String, Object> result = new LinkedHashMap<>(data);
        for (String field : fields) {
            if (result.containsKey(field) && result.get(field) != null) {
                result.put(field, mask(String.valueOf(result.get(field)), 1, 1));
            }
        }
        return result;
    }

    /**
     * 加密手机号字段。
     *
     * @param mobile 手机号明文
     * @param key    AES 密钥字节
     * @return 密文
     */
    public static String encryptMobile(String mobile, byte[] key) {
        return encryptField(mobile, "mobile", key);
    }

    /**
     * 解密手机号字段。
     *
     * @param encryptedMobile 手机号密文
     * @param key             AES 密钥字节
     * @return 手机号明文
     */
    public static String decryptMobile(String encryptedMobile, byte[] key) {
        return decryptField(encryptedMobile, "mobile", key);
    }

    /**
     * 加密邮箱字段。
     *
     * @param email 邮箱明文
     * @param key   AES 密钥字节
     * @return 密文
     */
    public static String encryptEmail(String email, byte[] key) {
        return encryptField(email, "email", key);
    }

    /**
     * 解密邮箱字段。
     *
     * @param encryptedEmail 邮箱密文
     * @param key            AES 密钥字节
     * @return 邮箱明文
     */
    public static String decryptEmail(String encryptedEmail, byte[] key) {
        return decryptField(encryptedEmail, "email", key);
    }

    /**
     * 加密身份证号字段。
     *
     * @param idCard 身份证号明文
     * @param key    AES 密钥字节
     * @return 密文
     */
    public static String encryptIdCard(String idCard, byte[] key) {
        return encryptField(idCard, "idCard", key);
    }

    /**
     * 解密身份证号字段。
     *
     * @param encryptedIdCard 身份证号密文
     * @param key             AES 密钥字节
     * @return 身份证号明文
     */
    public static String decryptIdCard(String encryptedIdCard, byte[] key) {
        return decryptField(encryptedIdCard, "idCard", key);
    }

    /**
     * 加密银行卡号字段。
     *
     * @param bankCard 银行卡号明文
     * @param key      AES 密钥字节
     * @return 密文
     */
    public static String encryptBankCard(String bankCard, byte[] key) {
        return encryptField(bankCard, "bankCard", key);
    }

    /**
     * 解密银行卡号字段。
     *
     * @param encryptedBankCard 银行卡号密文
     * @param key               AES 密钥字节
     * @return 银行卡号明文
     */
    public static String decryptBankCard(String encryptedBankCard, byte[] key) {
        return decryptField(encryptedBankCard, "bankCard", key);
    }

    /**
     * 加密通用字段。
     *
     * @param plainText 明文，不能为 null
     * @param fieldName 字段名，不能空白
     * @param key       AES 密钥字节
     * @return 密文
     */
    public static String encryptField(String plainText, String fieldName, byte[] key) {
        requireNotBlank(fieldName, "fieldName");
        return aesGcmEncrypt(plainText, key);
    }

    /**
     * 解密通用字段。
     *
     * @param cipherText 密文，不能空白
     * @param fieldName  字段名，不能空白
     * @param key        AES 密钥字节
     * @return 明文
     */
    public static String decryptField(String cipherText, String fieldName, byte[] key) {
        requireNotBlank(fieldName, "fieldName");
        return aesGcmDecrypt(cipherText, key);
    }

    /**
     * 对简单 JSON 文本中的指定字段值加密。
     *
     * @param json   JSON 文本
     * @param fields 字段名集合
     * @param key    AES 密钥字节
     * @return 加密后的 JSON 文本
     */
    public static String encryptJsonFields(String json, Set<String> fields, byte[] key) {
        return transformJsonFields(json, fields, value -> aesGcmEncrypt(value, key));
    }

    /**
     * 对简单 JSON 文本中的指定字段值解密。
     *
     * @param json   JSON 文本
     * @param fields 字段名集合
     * @param key    AES 密钥字节
     * @return 解密后的 JSON 文本
     */
    public static String decryptJsonFields(String json, Set<String> fields, byte[] key) {
        return transformJsonFields(json, fields, value -> aesGcmDecrypt(value, key));
    }

    /**
     * 加密配置值。
     *
     * @param value 配置明文，不能为 null
     * @param key   AES 密钥字节
     * @return ENC 包装后的密文
     */
    public static String encryptConfigValue(String value, byte[] key) {
        return wrapEncryptedValue(aesGcmEncrypt(value, key));
    }

    /**
     * 解密配置值。
     *
     * @param encryptedValue ENC 包装后的密文或原始密文
     * @param key            AES 密钥字节
     * @return 配置明文
     */
    public static String decryptConfigValue(String encryptedValue, byte[] key) {
        return aesGcmDecrypt(unwrapEncryptedValue(encryptedValue), key);
    }

    /**
     * 判断配置值是否为 ENC 包装格式。
     *
     * @param value 配置值
     * @return 是密文配置返回 true
     */
    public static boolean isEncryptedValue(String value) {
        return value != null && ENC_PATTERN.matcher(value.trim()).matches();
    }

    /**
     * 将密文包装为 ENC 格式。
     *
     * @param cipherText 密文，不能空白
     * @return ENC 包装密文
     */
    public static String wrapEncryptedValue(String cipherText) {
        return "ENC(" + requireNotBlank(cipherText, "cipherText") + ")";
    }

    /**
     * 提取 ENC 包装中的密文。
     *
     * @param value ENC 包装密文或原始密文
     * @return 密文
     */
    public static String unwrapEncryptedValue(String value) {
        String text = requireNotBlank(value, "value").trim();
        Matcher matcher = ENC_PATTERN.matcher(text);
        return matcher.matches() ? matcher.group(1) : text;
    }

    /**
     * 当配置值为 ENC 格式时解密，否则返回原值。
     *
     * @param value 配置值
     * @param key   AES 密钥字节
     * @return 解析后的配置值
     */
    public static String decryptIfNecessary(String value, byte[] key) {
        return isEncryptedValue(value) ? decryptConfigValue(value, key) : value;
    }

    /**
     * 加密数据库密码配置。
     *
     * @param password 数据库密码，不能为 null
     * @param key      AES 密钥字节
     * @return ENC 包装密文
     */
    public static String encryptDataSourcePassword(String password, byte[] key) {
        return encryptConfigValue(password, key);
    }

    /**
     * 解密数据库密码配置。
     *
     * @param encryptedPassword ENC 包装密文
     * @param key               AES 密钥字节
     * @return 数据库密码明文
     */
    public static String decryptDataSourcePassword(String encryptedPassword, byte[] key) {
        return decryptConfigValue(encryptedPassword, key);
    }

    /**
     * 从系统属性 secure.master-key 或环境变量 SECURE_MASTER_KEY 加载 Base64 AES 主密钥。
     *
     * @return AES 主密钥字节
     */
    public static byte[] loadMasterKey() {
        String value = System.getProperty("secure.master-key");
        if (value == null || value.isBlank()) {
            value = System.getenv("SECURE_MASTER_KEY");
        }
        if (value == null || value.isBlank()) {
            throw new IllegalStateException("未配置主密钥 secure.master-key 或 SECURE_MASTER_KEY");
        }
        byte[] key = base64Decode(value);
        validateAesKey(key);
        return key;
    }

    /**
     * 使用新旧主密钥轮换配置密文。
     *
     * @param encryptedValue 旧密钥加密的配置值
     * @param oldKey         旧 AES 密钥字节
     * @param newKey         新 AES 密钥字节
     * @return 新密钥加密后的配置值
     */
    public static String rotateMasterKey(String encryptedValue, byte[] oldKey, byte[] newKey) {
        return encryptConfigValue(decryptConfigValue(encryptedValue, oldKey), newKey);
    }

    /**
     * 校验字符串不为空白并返回原值。
     *
     * @param value 字符串
     * @param name  参数名
     * @return 原字符串
     */
    public static String requireNotBlank(String value, String name) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(name + " 不能为空");
        }
        return value;
    }

    /**
     * 校验密钥长度是否在允许列表内。
     *
     * @param key            密钥字节，不能为空
     * @param allowedLengths 允许长度列表，单位字节
     */
    public static void validateKeyLength(byte[] key, int... allowedLengths) {
        requireNotEmpty(key, "key");
        requireNotNullOrEmpty(allowedLengths, "allowedLengths");
        for (int allowedLength : allowedLengths) {
            if (key.length == allowedLength) {
                return;
            }
        }
        throw new IllegalArgumentException("密钥长度非法: " + key.length);
    }

    /**
     * 校验 AES 密钥长度是否合法。
     *
     * @param key AES 密钥字节
     */
    public static void validateAesKey(byte[] key) {
        validateKeyLength(key, 16, 24, 32);
    }

    /**
     * 校验初始化向量长度。
     *
     * @param iv             IV 字节
     * @param expectedLength 期望长度，单位字节
     */
    public static void validateIv(byte[] iv, int expectedLength) {
        requirePositive(expectedLength, "expectedLength");
        validateKeyLength(iv, expectedLength);
    }

    /**
     * 校验算法名称是否可用。
     *
     * @param algorithm 算法名称，不能空白
     */
    public static void validateAlgorithm(String algorithm) {
        if (!isSupportedAlgorithm(algorithm)) {
            throw new IllegalArgumentException("算法不支持: " + algorithm);
        }
    }

    /**
     * 判断算法名称是否被当前 JDK 支持。
     *
     * @param algorithm 算法名称，不能空白
     * @return 支持返回 true
     */
    public static boolean isSupportedAlgorithm(String algorithm) {
        String value = requireNotBlank(algorithm, "algorithm");
        return isDigestAlgorithm(value) || isMacAlgorithm(value) || isCipherAlgorithm(value) || isSignatureAlgorithm(value) || isKeyPairAlgorithm(value);
    }

    /**
     * 判断是否为强密码。
     *
     * @param password 密码
     * @return 强密码返回 true
     */
    public static boolean isStrongPassword(String password) {
        return checkPasswordStrength(password) >= 4;
    }

    /**
     * 判断文件名是否安全。
     *
     * @param filename 文件名
     * @return 安全返回 true
     */
    public static boolean isSafeFilename(String filename) {
        if (filename == null || filename.isBlank()) {
            return false;
        }
        return filename.equals(sanitizeFilename(filename));
    }

    /**
     * 判断重定向地址是否安全。
     *
     * @param url 重定向地址
     * @return 安全返回 true
     */
    public static boolean isSafeRedirectUrl(String url) {
        if (url == null || url.isBlank()) {
            return false;
        }
        String value = url.trim();
        if (value.startsWith("//")) {
            return false;
        }
        if (value.startsWith("/")) {
            return !value.contains("\\") && !value.contains("\r") && !value.contains("\n");
        }
        try {
            URI uri = new URI(value);
            String scheme = uri.getScheme();
            return ("http".equalsIgnoreCase(scheme) || "https".equalsIgnoreCase(scheme)) && uri.getUserInfo() == null;
        } catch (URISyntaxException e) {
            return false;
        }
    }

    /**
     * 判断 IP 是否为内网地址。
     *
     * @param ip IPv4 地址
     * @return 内网地址返回 true
     */
    public static boolean isPrivateIp(String ip) {
        int[] parts = parseIpv4(ip);
        if (parts == null) {
            return false;
        }
        return parts[0] == 10
                || (parts[0] == 172 && parts[1] >= 16 && parts[1] <= 31)
                || (parts[0] == 192 && parts[1] == 168);
    }

    /**
     * 判断 IP 是否为本机回环地址。
     *
     * @param ip IPv4 地址
     * @return 回环地址返回 true
     */
    public static boolean isLoopbackIp(String ip) {
        int[] parts = parseIpv4(ip);
        return parts != null && parts[0] == 127;
    }

    /**
     * 对日志值进行安全脱敏。
     *
     * @param value 原始值
     * @return 安全日志值
     */
    public static String safeLogValue(String value) {
        return maskSecret(value);
    }

    /**
     * 对敏感值进行日志脱敏。
     *
     * @param secret 敏感值
     * @return 脱敏值
     */
    public static String maskSecret(String secret) {
        return mask(secret, 3, 3);
    }

    /**
     * 对 Authorization 请求头脱敏。
     *
     * @param authorization Authorization 请求头
     * @return 脱敏请求头
     */
    public static String maskAuthorization(String authorization) {
        if (authorization == null || authorization.isBlank()) {
            return "";
        }
        if (authorization.regionMatches(true, 0, "Bearer ", 0, 7)) {
            return "Bearer " + maskToken(authorization.substring(7).trim());
        }
        return maskSecret(authorization);
    }

    /**
     * 对 Cookie 请求头脱敏。
     *
     * @param cookie Cookie 请求头
     * @return 脱敏 Cookie
     */
    public static String maskCookie(String cookie) {
        if (cookie == null || cookie.isBlank()) {
            return "";
        }
        return Arrays.stream(cookie.split(";"))
                .map(String::trim)
                .map(item -> {
                    int index = item.indexOf('=');
                    if (index <= 0) {
                        return maskSecret(item);
                    }
                    return item.substring(0, index + 1) + maskSecret(item.substring(index + 1));
                })
                .collect(Collectors.joining("; "));
    }

    /**
     * 对请求头 Map 脱敏。
     *
     * @param headers 请求头 Map，不能为 null
     * @return 脱敏后的新 Map
     */
    public static Map<String, String> maskHeader(Map<String, String> headers) {
        requireNonNull(headers, "headers");
        Map<String, String> result = new LinkedHashMap<>();
        for (Map.Entry<String, String> entry : headers.entrySet()) {
            if (containsSensitiveKey(entry.getKey())) {
                result.put(entry.getKey(), maskSecret(entry.getValue()));
            } else {
                result.put(entry.getKey(), entry.getValue());
            }
        }
        return result;
    }

    /**
     * 对请求参数 Map 脱敏。
     *
     * @param params 请求参数 Map，不能为 null
     * @return 脱敏后的新 Map
     */
    public static Map<String, Object> maskRequestParams(Map<String, ?> params) {
        requireNonNull(params, "params");
        Map<String, Object> result = new LinkedHashMap<>();
        for (Map.Entry<String, ?> entry : params.entrySet()) {
            if (containsSensitiveKey(entry.getKey())) {
                result.put(entry.getKey(), maskSecret(String.valueOf(entry.getValue())));
            } else {
                result.put(entry.getKey(), entry.getValue());
            }
        }
        return result;
    }

    /**
     * 删除 Map 中的敏感字段。
     *
     * @param data 数据 Map，不能为 null
     * @return 删除敏感字段后的新 Map
     */
    public static Map<String, Object> removeSensitiveFields(Map<String, Object> data) {
        requireNonNull(data, "data");
        Map<String, Object> result = new LinkedHashMap<>();
        for (Map.Entry<String, Object> entry : data.entrySet()) {
            if (!containsSensitiveKey(entry.getKey())) {
                result.put(entry.getKey(), entry.getValue());
            }
        }
        return result;
    }

    /**
     * 判断字段名是否为敏感字段。
     *
     * @param key 字段名
     * @return 敏感字段返回 true
     */
    public static boolean containsSensitiveKey(String key) {
        if (key == null || key.isBlank()) {
            return false;
        }
        String normalized = key.replace("_", "").replace("-", "").toLowerCase(Locale.ROOT);
        return SENSITIVE_KEYWORDS.stream().anyMatch(normalized::contains);
    }

    /**
     * 清理异常消息中的敏感值。
     *
     * @param message 异常消息
     * @return 清理后的异常消息
     */
    public static String sanitizeExceptionMessage(String message) {
        if (message == null || message.isBlank()) {
            return "";
        }
        String result = message;
        for (String keyword : SENSITIVE_KEYWORDS) {
            Pattern pattern = Pattern.compile("(?i)(" + Pattern.quote(keyword) + "\\s*[=:]\\s*)([^,;\\s]+)");
            Matcher matcher = pattern.matcher(result);
            StringBuffer buffer = new StringBuffer();
            while (matcher.find()) {
                matcher.appendReplacement(buffer, Matcher.quoteReplacement(matcher.group(1) + "******"));
            }
            matcher.appendTail(buffer);
            result = buffer.toString();
        }
        return result;
    }

    private static SecretKey generateMacKey(String algorithm, int bitLength) {
        try {
            KeyGenerator generator = KeyGenerator.getInstance(algorithm);
            generator.init(bitLength, SECURE_RANDOM);
            return generator.generateKey();
        } catch (GeneralSecurityException e) {
            throw new IllegalStateException("生成 HMAC 密钥失败", e);
        }
    }

    private static void validateAesBitLength(int bitLength) {
        if (bitLength != 128 && bitLength != 192 && bitLength != 256) {
            throw new IllegalArgumentException("AES 密钥位数只支持 128、192、256");
        }
    }

    private static byte[] doCipher(String transformation, int mode, Key key, java.security.spec.AlgorithmParameterSpec spec, byte[] data) {
        try {
            Cipher cipher = Cipher.getInstance(transformation);
            if (spec == null) {
                cipher.init(mode, key);
            } else {
                cipher.init(mode, key, spec);
            }
            return cipher.doFinal(data);
        } catch (GeneralSecurityException e) {
            throw new IllegalArgumentException("加解密失败: " + transformation, e);
        }
    }

    private static Cipher initCipher(String transformation, int mode, Key key, java.security.spec.AlgorithmParameterSpec spec) {
        try {
            Cipher cipher = Cipher.getInstance(transformation);
            if (spec == null) {
                cipher.init(mode, key);
            } else {
                cipher.init(mode, key, spec);
            }
            return cipher;
        } catch (GeneralSecurityException e) {
            throw new IllegalArgumentException("初始化 Cipher 失败: " + transformation, e);
        }
    }

    private static byte[] doBlockCipher(byte[] data, Cipher cipher, int blockSize) {
        try {
            ByteArrayOutputStream output = new ByteArrayOutputStream();
            for (int offset = 0; offset < data.length; offset += blockSize) {
                int length = Math.min(blockSize, data.length - offset);
                output.write(cipher.doFinal(data, offset, length));
            }
            return output.toByteArray();
        } catch (GeneralSecurityException | IOException e) {
            throw new IllegalArgumentException("RSA 分段处理失败", e);
        }
    }

    private static byte[] rsaPkcs1PrivateEncrypt(byte[] data, PrivateKey privateKey) {
        validateRsaKeyLength(privateKey);
        return rsaEncryptByBlock(data, privateKey, "RSA/ECB/PKCS1Padding", getMaxRsaEncryptBlockSize(privateKey, "PKCS1"));
    }

    private static byte[] rsaPkcs1PublicDecrypt(byte[] data, PublicKey publicKey) {
        validateRsaKeyLength(publicKey);
        return rsaDecryptByBlock(data, publicKey, "RSA/ECB/PKCS1Padding");
    }

    private static String buildPem(String type, byte[] content) {
        String normalizedType = requireNotBlank(type, "type").toUpperCase(Locale.ROOT);
        return "-----BEGIN " + normalizedType + "-----\n" + pemBase64Encode(content) + "-----END " + normalizedType + "-----\n";
    }

    private static void validatePemType(String pem, String expectedType) {
        String actual = detectPemType(pem);
        if (!expectedType.equals(actual)) {
            throw new IllegalArgumentException("PEM 类型不匹配，期望 " + expectedType + "，实际 " + actual);
        }
    }

    private static byte[] wrapRsaPkcs1PrivateKeyToPkcs8(byte[] pkcs1) {
        byte[] version = derIntegerZero();
        byte[] algorithm = derSequence(derOidRsaEncryption(), derNull());
        byte[] privateKey = derOctetString(pkcs1);
        return derSequence(version, algorithm, privateKey);
    }

    private static byte[] derSequence(byte[]... values) {
        return derTag(0x30, concat(values));
    }

    private static byte[] derIntegerZero() {
        return new byte[]{0x02, 0x01, 0x00};
    }

    private static byte[] derNull() {
        return new byte[]{0x05, 0x00};
    }

    private static byte[] derOctetString(byte[] value) {
        return derTag(0x04, value);
    }

    private static byte[] derOidRsaEncryption() {
        return new byte[]{0x06, 0x09, 0x2A, (byte) 0x86, 0x48, (byte) 0x86, (byte) 0xF7, 0x0D, 0x01, 0x01, 0x01};
    }

    private static byte[] derTag(int tag, byte[] value) {
        return concat(new byte[]{(byte) tag}, derLength(value.length), value);
    }

    private static byte[] derLength(int length) {
        if (length < 128) {
            return new byte[]{(byte) length};
        }
        int temp = length;
        int bytes = 0;
        while (temp > 0) {
            temp >>= 8;
            bytes++;
        }
        byte[] result = new byte[1 + bytes];
        result[0] = (byte) (0x80 | bytes);
        for (int i = bytes; i > 0; i--) {
            result[i] = (byte) (length & 0xFF);
            length >>= 8;
        }
        return result;
    }

    private static byte[] concat(byte[]... arrays) {
        int length = 0;
        for (byte[] array : arrays) {
            length += array.length;
        }
        byte[] result = new byte[length];
        int offset = 0;
        for (byte[] array : arrays) {
            System.arraycopy(array, 0, result, offset, array.length);
            offset += array.length;
        }
        return result;
    }

    private static String createJwt(Map<String, Object> claims, String secret, Duration ttl, String alg) {
        requireNonNull(claims, "claims");
        requireNotBlank(secret, "secret");
        if (ttl == null || ttl.isZero() || ttl.isNegative()) {
            throw new IllegalArgumentException("ttl 必须大于 0");
        }
        Instant now = Instant.now();
        Map<String, Object> header = new LinkedHashMap<>();
        header.put("typ", "JWT");
        header.put("alg", alg);
        Map<String, Object> payload = new LinkedHashMap<>(claims);
        payload.put("iat", now.getEpochSecond());
        payload.put("exp", now.plus(ttl).getEpochSecond());
        String headerPart = base64UrlEncode(toJson(header).getBytes(StandardCharsets.UTF_8));
        String payloadPart = base64UrlEncode(toJson(payload).getBytes(StandardCharsets.UTF_8));
        String signText = headerPart + "." + payloadPart;
        return signText + "." + base64UrlEncode(jwtSign(signText, secret, alg));
    }

    private static byte[] jwtSign(String signText, String secret, String alg) {
        return switch (requireNotBlank(alg, "alg")) {
            case "HS256" -> hmac(toUtf8Bytes(signText), toUtf8Bytes(secret), "HmacSHA256");
            case "HS512" -> hmac(toUtf8Bytes(signText), toUtf8Bytes(secret), "HmacSHA512");
            default -> throw new IllegalArgumentException("JWT 仅支持 HS256 和 HS512: " + alg);
        };
    }

    private static String[] splitJwt(String token) {
        String[] parts = requireNotBlank(token, "token").split("\\.");
        if (parts.length != 3) {
            throw new IllegalArgumentException("JWT 格式非法");
        }
        return parts;
    }

    private static long toLong(Object value, String name) {
        if (value instanceof Number number) {
            return number.longValue();
        }
        try {
            return Long.parseLong(String.valueOf(value));
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException(name + " 不是有效数字", e);
        }
    }

    private static String buildQueryString(Map<String, ?> params, boolean sorted) {
        requireNonNull(params, "params");
        return params.entrySet().stream()
                .filter(e -> e.getKey() != null && e.getValue() != null)
                .sorted(sorted ? Map.Entry.comparingByKey() : (a, b) -> 0)
                .map(e -> e.getKey() + "=" + e.getValue())
                .collect(Collectors.joining("&"));
    }

    private static String transformJsonFields(String json, Set<String> fields, StringTransformer transformer) {
        if (json == null || json.isBlank()) {
            return "";
        }
        requireNonNull(fields, "fields");
        String result = json;
        for (String field : fields) {
            if (field == null || field.isBlank()) {
                continue;
            }
            Pattern pattern = Pattern.compile("(\\\"" + Pattern.quote(field) + "\\\"\\s*:\\s*\\\")(.*?)(\\\")");
            Matcher matcher = pattern.matcher(result);
            StringBuffer buffer = new StringBuffer();
            while (matcher.find()) {
                matcher.appendReplacement(buffer, Matcher.quoteReplacement(matcher.group(1) + transformer.apply(matcher.group(2)) + matcher.group(3)));
            }
            matcher.appendTail(buffer);
            result = buffer.toString();
        }
        return result;
    }

    private static boolean isDigestAlgorithm(String algorithm) {
        try {
            MessageDigest.getInstance(algorithm);
            return true;
        } catch (GeneralSecurityException e) {
            return false;
        }
    }

    private static boolean isMacAlgorithm(String algorithm) {
        try {
            Mac.getInstance(algorithm);
            return true;
        } catch (GeneralSecurityException e) {
            return false;
        }
    }

    private static boolean isCipherAlgorithm(String algorithm) {
        try {
            Cipher.getInstance(algorithm);
            return true;
        } catch (GeneralSecurityException e) {
            return false;
        }
    }

    private static boolean isSignatureAlgorithm(String algorithm) {
        try {
            Signature.getInstance(algorithm);
            return true;
        } catch (GeneralSecurityException e) {
            return false;
        }
    }

    private static boolean isKeyPairAlgorithm(String algorithm) {
        try {
            KeyPairGenerator.getInstance(algorithm);
            return true;
        } catch (GeneralSecurityException e) {
            return false;
        }
    }

    private static int[] parseIpv4(String ip) {
        if (ip == null || ip.isBlank()) {
            return null;
        }
        String[] split = ip.split("\\.");
        if (split.length != 4) {
            return null;
        }
        int[] result = new int[4];
        try {
            for (int i = 0; i < 4; i++) {
                result[i] = Integer.parseInt(split[i]);
                if (result[i] < 0 || result[i] > 255) {
                    return null;
                }
            }
            return result;
        } catch (NumberFormatException e) {
            return null;
        }
    }

    private static Path requireReadableFile(Path path) throws IOException {
        requireNonNull(path, "path");
        if (!Files.isRegularFile(path) || !Files.isReadable(path)) {
            throw new IOException("文件不可读: " + path);
        }
        return path;
    }

    private static <T> T requireNonNull(T value, String name) {
        if (value == null) {
            throw new IllegalArgumentException(name + " 不能为 null");
        }
        return value;
    }

    private static void requireNotEmpty(byte[] bytes, String name) {
        if (bytes == null || bytes.length == 0) {
            throw new IllegalArgumentException(name + " 不能为空");
        }
    }

    private static void requireNotNullOrEmpty(int[] values, String name) {
        if (values == null || values.length == 0) {
            throw new IllegalArgumentException(name + " 不能为空");
        }
    }

    private static void requirePositive(int value, String name) {
        if (value <= 0) {
            throw new IllegalArgumentException(name + " 必须大于 0");
        }
    }

    private static void requirePositive(long value, String name) {
        if (value <= 0) {
            throw new IllegalArgumentException(name + " 必须大于 0");
        }
    }

    private static String toJson(Object value) {
        if (value == null) {
            return "null";
        }
        if (value instanceof String text) {
            return "\"" + escapeJson(text) + "\"";
        }
        if (value instanceof Number || value instanceof Boolean) {
            return String.valueOf(value);
        }
        if (value instanceof Map<?, ?> map) {
            return map.entrySet().stream()
                    .map(e -> toJson(String.valueOf(e.getKey())) + ":" + toJson(e.getValue()))
                    .collect(Collectors.joining(",", "{", "}"));
        }
        if (value instanceof Collection<?> collection) {
            return collection.stream().map(SecurityUtil::toJson).collect(Collectors.joining(",", "[", "]"));
        }
        if (value.getClass().isArray()) {
            int length = java.lang.reflect.Array.getLength(value);
            List<Object> list = new ArrayList<>(length);
            for (int i = 0; i < length; i++) {
                list.add(java.lang.reflect.Array.get(value, i));
            }
            return toJson(list);
        }
        return toJson(String.valueOf(value));
    }

    private static String escapeJson(String text) {
        StringBuilder builder = new StringBuilder(text.length());
        for (int i = 0; i < text.length(); i++) {
            char ch = text.charAt(i);
            switch (ch) {
                case '"' -> builder.append("\\\"");
                case '\\' -> builder.append("\\\\");
                case '\b' -> builder.append("\\b");
                case '\f' -> builder.append("\\f");
                case '\n' -> builder.append("\\n");
                case '\r' -> builder.append("\\r");
                case '\t' -> builder.append("\\t");
                default -> {
                    if (ch < 0x20) {
                        builder.append(String.format("\\u%04x", (int) ch));
                    } else {
                        builder.append(ch);
                    }
                }
            }
        }
        return builder.toString();
    }

    private static Map<String, Object> parseJsonObject(String json) {
        Object value = new JsonParser(requireNotBlank(json, "json")).parseValue();
        if (!(value instanceof Map<?, ?> map)) {
            throw new IllegalArgumentException("JSON 不是对象");
        }
        Map<String, Object> result = new LinkedHashMap<>();
        for (Map.Entry<?, ?> entry : map.entrySet()) {
            result.put(String.valueOf(entry.getKey()), entry.getValue());
        }
        return result;
    }

    private interface StringTransformer {
        String apply(String value);
    }

    private static final class JsonParser {
        private final String json;
        private int index;

        private JsonParser(String json) {
            this.json = json;
        }

        private Object parseValue() {
            skipWhitespace();
            if (index >= json.length()) {
                throw new IllegalArgumentException("JSON 为空");
            }
            char ch = json.charAt(index);
            if (ch == '{') {
                return parseObject();
            }
            if (ch == '[') {
                return parseArray();
            }
            if (ch == '"') {
                return parseString();
            }
            if (ch == 't' || ch == 'f') {
                return parseBoolean();
            }
            if (ch == 'n') {
                return parseNull();
            }
            if (ch == '-' || Character.isDigit(ch)) {
                return parseNumber();
            }
            throw new IllegalArgumentException("JSON 字符非法: " + ch);
        }

        private Map<String, Object> parseObject() {
            expect('{');
            Map<String, Object> map = new LinkedHashMap<>();
            skipWhitespace();
            if (peek('}')) {
                index++;
                return map;
            }
            while (true) {
                String key = parseString();
                skipWhitespace();
                expect(':');
                Object value = parseValue();
                map.put(key, value);
                skipWhitespace();
                if (peek('}')) {
                    index++;
                    return map;
                }
                expect(',');
            }
        }

        private List<Object> parseArray() {
            expect('[');
            List<Object> list = new ArrayList<>();
            skipWhitespace();
            if (peek(']')) {
                index++;
                return list;
            }
            while (true) {
                list.add(parseValue());
                skipWhitespace();
                if (peek(']')) {
                    index++;
                    return list;
                }
                expect(',');
            }
        }

        private String parseString() {
            expect('"');
            StringBuilder builder = new StringBuilder();
            while (index < json.length()) {
                char ch = json.charAt(index++);
                if (ch == '"') {
                    return builder.toString();
                }
                if (ch == '\\') {
                    if (index >= json.length()) {
                        throw new IllegalArgumentException("JSON 转义非法");
                    }
                    char escape = json.charAt(index++);
                    switch (escape) {
                        case '"' -> builder.append('"');
                        case '\\' -> builder.append('\\');
                        case '/' -> builder.append('/');
                        case 'b' -> builder.append('\b');
                        case 'f' -> builder.append('\f');
                        case 'n' -> builder.append('\n');
                        case 'r' -> builder.append('\r');
                        case 't' -> builder.append('\t');
                        case 'u' -> {
                            if (index + 4 > json.length()) {
                                throw new IllegalArgumentException("JSON unicode 转义非法");
                            }
                            builder.append((char) Integer.parseInt(json.substring(index, index + 4), 16));
                            index += 4;
                        }
                        default -> throw new IllegalArgumentException("JSON 转义非法: " + escape);
                    }
                } else {
                    builder.append(ch);
                }
            }
            throw new IllegalArgumentException("JSON 字符串未闭合");
        }

        private Boolean parseBoolean() {
            if (json.startsWith("true", index)) {
                index += 4;
                return Boolean.TRUE;
            }
            if (json.startsWith("false", index)) {
                index += 5;
                return Boolean.FALSE;
            }
            throw new IllegalArgumentException("JSON boolean 非法");
        }

        private Object parseNull() {
            if (json.startsWith("null", index)) {
                index += 4;
                return null;
            }
            throw new IllegalArgumentException("JSON null 非法");
        }

        private Number parseNumber() {
            int start = index;
            if (peek('-')) {
                index++;
            }
            while (index < json.length() && Character.isDigit(json.charAt(index))) {
                index++;
            }
            boolean decimal = false;
            if (peek('.')) {
                decimal = true;
                index++;
                while (index < json.length() && Character.isDigit(json.charAt(index))) {
                    index++;
                }
            }
            if (index < json.length() && (json.charAt(index) == 'e' || json.charAt(index) == 'E')) {
                decimal = true;
                index++;
                if (index < json.length() && (json.charAt(index) == '+' || json.charAt(index) == '-')) {
                    index++;
                }
                while (index < json.length() && Character.isDigit(json.charAt(index))) {
                    index++;
                }
            }
            String number = json.substring(start, index);
            return decimal ? Double.parseDouble(number) : Long.parseLong(number);
        }

        private void expect(char expected) {
            skipWhitespace();
            if (index >= json.length() || json.charAt(index) != expected) {
                throw new IllegalArgumentException("JSON 期望字符: " + expected);
            }
            index++;
        }

        private boolean peek(char expected) {
            return index < json.length() && json.charAt(index) == expected;
        }

        private void skipWhitespace() {
            while (index < json.length() && Character.isWhitespace(json.charAt(index))) {
                index++;
            }
        }
    }
}
