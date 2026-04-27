package io.github.atengk.utils;

import javax.crypto.*;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.PBEKeySpec;
import javax.crypto.spec.SecretKeySpec;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.InputStream;
import java.io.OutputStream;
import java.lang.reflect.Constructor;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.security.*;
import java.security.interfaces.RSAKey;
import java.security.spec.ECGenParameterSpec;
import java.security.spec.PKCS8EncodedKeySpec;
import java.security.spec.X509EncodedKeySpec;
import java.util.*;

/**
 * 安全工具类，提供摘要、HMAC、对称加解密、RSA/DSA/SM 系列签名加密、密钥与编码转换等能力。
 * <p>
 * 说明：SM2、SM3、SM4 依赖 BouncyCastle Provider，使用前请在项目中添加 bcprov-jdk18on 依赖。
 * 本类通过反射初始化 BC Provider，避免在未使用国密能力时产生编译期强依赖。
 * </p>
 *
 * @author Ateng
 * @since 2026-04-27
 */
public final class SecureUtil {

    private static final Charset UTF_8 = StandardCharsets.UTF_8;
    private static final SecureRandom RANDOM = new SecureRandom();
    private static final HexFormat HEX_FORMAT = HexFormat.of();
    private static final int BUFFER_SIZE = 16 * 1024;
    private static final int GCM_TAG_LENGTH_BIT = 128;
    private static final int GCM_IV_LENGTH = 12;
    private static final int DEFAULT_PBKDF2_ITERATIONS = 120_000;
    private static final String BC_PROVIDER = "BC";
    private static final String BC_PROVIDER_CLASS = "org.bouncycastle.jce.provider.BouncyCastleProvider";

    /**
     * 常用 AES 模式。
     */
    public static final String AES_ECB_PKCS5 = "AES/ECB/PKCS5Padding";
    public static final String AES_CBC_PKCS5 = "AES/CBC/PKCS5Padding";
    public static final String AES_GCM_NOPADDING = "AES/GCM/NoPadding";

    /**
     * 常用 DES 模式。
     */
    public static final String DES_ECB_PKCS5 = "DES/ECB/PKCS5Padding";
    public static final String DES_CBC_PKCS5 = "DES/CBC/PKCS5Padding";

    /**
     * 常用 3DES 模式。
     */
    public static final String DESEDE_ECB_PKCS5 = "DESede/ECB/PKCS5Padding";
    public static final String DESEDE_CBC_PKCS5 = "DESede/CBC/PKCS5Padding";

    /**
     * 兼容旧常量命名。
     */
    @Deprecated
    public static final String DESede_ECB_PKCS5 = DESEDE_ECB_PKCS5;
    @Deprecated
    public static final String DESede_CBC_PKCS5 = DESEDE_CBC_PKCS5;

    /**
     * SM4 模式，依赖 BouncyCastle Provider。
     */
    public static final String SM4_ECB_PKCS5 = "SM4/ECB/PKCS5Padding";
    public static final String SM4_CBC_PKCS5 = "SM4/CBC/PKCS5Padding";

    /**
     * 常用 RSA 模式。
     */
    public static final String RSA_PKCS1 = "RSA/ECB/PKCS1Padding";
    public static final String RSA_ECB_PKCS1 = RSA_PKCS1;
    public static final String RSA_ECB_OAEP_SHA1 = "RSA/ECB/OAEPWithSHA-1AndMGF1Padding";
    public static final String RSA_ECB_OAEP_SHA256 = "RSA/ECB/OAEPWithSHA-256AndMGF1Padding";

    /**
     * 常用签名算法。
     */
    public static final String SHA1_WITH_RSA = "SHA1withRSA";
    public static final String SHA256_WITH_RSA = "SHA256withRSA";
    public static final String SHA384_WITH_RSA = "SHA384withRSA";
    public static final String SHA512_WITH_RSA = "SHA512withRSA";
    public static final String SHA1_WITH_DSA = "SHA1withDSA";
    public static final String SHA256_WITH_DSA = "SHA256withDSA";
    public static final String SM3_WITH_SM2 = "SM3withSM2";

    /**
     * 禁止实例化工具类。
     */
    private SecureUtil() {
        throw new UnsupportedOperationException("工具类不可实例化");
    }

    /**
     * 字符串加密，返回 Base64 密文。
     *
     * @param data           明文
     * @param transformation 算法模式
     * @param key            密钥字符串，按 UTF-8 转字节
     * @return Base64 密文
     */
    public static String encrypt(String data, String transformation, String key) {
        return encrypt(data, transformation, key, null);
    }

    /**
     * 字符串加密，返回 Base64 密文。
     *
     * @param data           明文
     * @param transformation 算法模式
     * @param key            密钥字符串，按 UTF-8 转字节
     * @param iv             IV 或 GCM Nonce
     * @return Base64 密文
     */
    public static String encrypt(String data, String transformation, String key, byte[] iv) {
        try {
            requireText(data, "明文不能为空");
            requireText(key, "密钥不能为空");
            return base64Encode(encrypt(data.getBytes(UTF_8), transformation, key.getBytes(UTF_8), iv));
        } catch (Exception e) {
            throw wrap(e, "字符串加密失败");
        }
    }

    /**
     * 字符串解密，输入 Base64 密文。
     *
     * @param data           Base64 密文
     * @param transformation 算法模式
     * @param key            密钥字符串，按 UTF-8 转字节
     * @return 明文
     */
    public static String decrypt(String data, String transformation, String key) {
        return decrypt(data, transformation, key, null);
    }

    /**
     * 字符串解密，输入 Base64 密文。
     *
     * @param data           Base64 密文
     * @param transformation 算法模式
     * @param key            密钥字符串，按 UTF-8 转字节
     * @param iv             IV 或 GCM Nonce
     * @return 明文
     */
    public static String decrypt(String data, String transformation, String key, byte[] iv) {
        try {
            requireText(data, "密文不能为空");
            requireText(key, "密钥不能为空");
            return new String(decrypt(base64Decode(data), transformation, key.getBytes(UTF_8), iv), UTF_8);
        } catch (Exception e) {
            throw wrap(e, "字符串解密失败");
        }
    }

    /**
     * 字节数组加密。
     *
     * @param data           明文字节
     * @param transformation 算法模式
     * @param key            密钥字节
     * @param iv             IV 或 GCM Nonce
     * @return 密文字节
     */
    public static byte[] encrypt(byte[] data, String transformation, byte[] key, byte[] iv) {
        try {
            Objects.requireNonNull(data, "明文不能为空");
            Cipher cipher = initSymmetricCipher(Cipher.ENCRYPT_MODE, transformation, key, iv);
            return cipher.doFinal(data);
        } catch (Exception e) {
            throw wrap(e, "字节数组加密失败");
        }
    }

    /**
     * 字节数组解密。
     *
     * @param data           密文字节
     * @param transformation 算法模式
     * @param key            密钥字节
     * @param iv             IV 或 GCM Nonce
     * @return 明文字节
     */
    public static byte[] decrypt(byte[] data, String transformation, byte[] key, byte[] iv) {
        try {
            Objects.requireNonNull(data, "密文不能为空");
            Cipher cipher = initSymmetricCipher(Cipher.DECRYPT_MODE, transformation, key, iv);
            return cipher.doFinal(data);
        } catch (Exception e) {
            throw wrap(e, "字节数组解密失败");
        }
    }

    /**
     * 使用随机 IV 加密，并将 IV 拼接在密文前方后输出 Base64。
     *
     * @param data           明文字节
     * @param transformation 算法模式
     * @param key            密钥字节
     * @param ivLength       IV 长度
     * @return Base64，内容为 IV + 密文
     */
    public static String encryptWithRandomIvToBase64(byte[] data, String transformation, byte[] key, int ivLength) {
        byte[] iv = randomIv(ivLength);
        byte[] encrypted = encrypt(data, transformation, key, iv);
        return base64Encode(merge(iv, encrypted));
    }

    /**
     * 解密 IV 前置的 Base64 密文。
     *
     * @param base64         Base64，内容为 IV + 密文
     * @param transformation 算法模式
     * @param key            密钥字节
     * @param ivLength       IV 长度
     * @return 明文字节
     */
    public static byte[] decryptWithPrefixedIvFromBase64(String base64, String transformation, byte[] key, int ivLength) {
        requireText(base64, "密文不能为空");
        if (ivLength <= 0) {
            throw new IllegalArgumentException("IV长度必须大于0");
        }
        byte[] all = base64Decode(base64);
        if (all.length <= ivLength) {
            throw new IllegalArgumentException("密文长度非法");
        }
        byte[] iv = Arrays.copyOfRange(all, 0, ivLength);
        byte[] encrypted = Arrays.copyOfRange(all, ivLength, all.length);
        return decrypt(encrypted, transformation, key, iv);
    }

    /**
     * AES-GCM 加密，随机生成 12 字节 Nonce，并输出 Base64（Nonce + 密文 + Tag）。
     *
     * @param data 明文
     * @param key  AES 密钥
     * @return Base64 密文
     */
    public static String aesGcmEncrypt(String data, byte[] key) {
        requireText(data, "明文不能为空");
        return encryptWithRandomIvToBase64(data.getBytes(UTF_8), AES_GCM_NOPADDING, key, GCM_IV_LENGTH);
    }

    /**
     * AES-GCM 解密，输入 Base64（Nonce + 密文 + Tag）。
     *
     * @param base64 Base64 密文
     * @param key    AES 密钥
     * @return 明文
     */
    public static String aesGcmDecrypt(String base64, byte[] key) {
        return new String(decryptWithPrefixedIvFromBase64(base64, AES_GCM_NOPADDING, key, GCM_IV_LENGTH), UTF_8);
    }

    /**
     * 文件加密。
     *
     * @param source         源文件
     * @param target         目标文件
     * @param transformation 算法模式
     * @param key            密钥字节
     * @param iv             IV 或 GCM Nonce
     */
    public static void encryptFile(File source, File target, String transformation, byte[] key, byte[] iv) {
        processFile(source, target, transformation, key, iv, Cipher.ENCRYPT_MODE);
    }

    /**
     * 文件解密。
     *
     * @param source         源文件
     * @param target         目标文件
     * @param transformation 算法模式
     * @param key            密钥字节
     * @param iv             IV 或 GCM Nonce
     */
    public static void decryptFile(File source, File target, String transformation, byte[] key, byte[] iv) {
        processFile(source, target, transformation, key, iv, Cipher.DECRYPT_MODE);
    }

    /**
     * 文件加解密核心逻辑，先写临时文件，成功后再替换目标文件。
     *
     * @param source         源文件
     * @param target         目标文件
     * @param transformation 算法模式
     * @param key            密钥字节
     * @param iv             IV 或 GCM Nonce
     * @param mode           Cipher 模式
     */
    private static void processFile(File source, File target, String transformation, byte[] key, byte[] iv, int mode) {
        Objects.requireNonNull(source, "源文件不能为空");
        Objects.requireNonNull(target, "目标文件不能为空");

        Path sourcePath = source.toPath();
        Path targetPath = target.toPath();
        Path parent = targetPath.toAbsolutePath().getParent();
        Path tmpPath = null;

        try {
            if (!Files.isRegularFile(sourcePath)) {
                throw new IllegalArgumentException("源文件不存在或不是普通文件: " + sourcePath);
            }
            if (parent != null) {
                Files.createDirectories(parent);
            }

            String prefix = targetPath.getFileName() == null ? "secure" : targetPath.getFileName().toString();
            tmpPath = Files.createTempFile(parent, prefix, ".tmp");

            Cipher cipher = initSymmetricCipher(mode, transformation, key, iv);
            try (InputStream is = Files.newInputStream(sourcePath, StandardOpenOption.READ);
                 OutputStream os = Files.newOutputStream(tmpPath, StandardOpenOption.WRITE, StandardOpenOption.TRUNCATE_EXISTING)) {
                byte[] buffer = new byte[BUFFER_SIZE];
                int len;
                while ((len = is.read(buffer)) != -1) {
                    byte[] processed = cipher.update(buffer, 0, len);
                    if (processed != null && processed.length > 0) {
                        os.write(processed);
                    }
                }
                byte[] finalBytes = cipher.doFinal();
                if (finalBytes != null && finalBytes.length > 0) {
                    os.write(finalBytes);
                }
            }
            moveReplace(tmpPath, targetPath);
            tmpPath = null;
        } catch (Exception e) {
            throw wrap(e, "文件加解密失败");
        } finally {
            deleteIfExists(tmpPath);
        }
    }

    /**
     * AES 加密，字符串输出 Base64。
     *
     * @param data           明文
     * @param key            密钥字符串
     * @param transformation 算法模式
     * @param iv             IV 或 GCM Nonce
     * @return Base64 密文
     */
    public static String aesEncrypt(String data, String key, String transformation, byte[] iv) {
        return encrypt(data, transformation, key, iv);
    }

    /**
     * AES 解密，Base64 输入。
     *
     * @param data           Base64 密文
     * @param key            密钥字符串
     * @param transformation 算法模式
     * @param iv             IV 或 GCM Nonce
     * @return 明文
     */
    public static String aesDecrypt(String data, String key, String transformation, byte[] iv) {
        return decrypt(data, transformation, key, iv);
    }

    /**
     * DES 加密。
     *
     * @param data           明文
     * @param key            密钥字符串
     * @param transformation 算法模式
     * @param iv             IV
     * @return Base64 密文
     */
    public static String desEncrypt(String data, String key, String transformation, byte[] iv) {
        return encrypt(data, transformation, key, iv);
    }

    /**
     * DES 解密。
     *
     * @param data           Base64 密文
     * @param key            密钥字符串
     * @param transformation 算法模式
     * @param iv             IV
     * @return 明文
     */
    public static String desDecrypt(String data, String key, String transformation, byte[] iv) {
        return decrypt(data, transformation, key, iv);
    }

    /**
     * 3DES 加密。
     *
     * @param data           明文
     * @param key            密钥字符串
     * @param transformation 算法模式
     * @param iv             IV
     * @return Base64 密文
     */
    public static String tripleDesEncrypt(String data, String key, String transformation, byte[] iv) {
        return encrypt(data, transformation, key, iv);
    }

    /**
     * 3DES 解密。
     *
     * @param data           Base64 密文
     * @param key            密钥字符串
     * @param transformation 算法模式
     * @param iv             IV
     * @return 明文
     */
    public static String tripleDesDecrypt(String data, String key, String transformation, byte[] iv) {
        return decrypt(data, transformation, key, iv);
    }

    /**
     * SM4 加密，依赖 BouncyCastle Provider。
     *
     * @param data           明文
     * @param key            密钥字符串
     * @param transformation 算法模式
     * @param iv             IV
     * @return Base64 密文
     */
    public static String sm4Encrypt(String data, String key, String transformation, byte[] iv) {
        return encrypt(data, transformation, key, iv);
    }

    /**
     * SM4 解密，依赖 BouncyCastle Provider。
     *
     * @param data           Base64 密文
     * @param key            密钥字符串
     * @param transformation 算法模式
     * @param iv             IV
     * @return 明文
     */
    public static String sm4Decrypt(String data, String key, String transformation, byte[] iv) {
        return decrypt(data, transformation, key, iv);
    }

    /**
     * SM4-CBC 加密，返回 Base64。
     *
     * @param data 明文
     * @param key  16 字节密钥
     * @param iv   16 字节 IV
     * @return Base64 密文
     */
    public static String sm4Encrypt(String data, byte[] key, byte[] iv) {
        requireText(data, "明文不能为空");
        return base64Encode(encrypt(data.getBytes(UTF_8), SM4_CBC_PKCS5, key, iv));
    }

    /**
     * SM4-CBC 解密。
     *
     * @param data Base64 密文
     * @param key  16 字节密钥
     * @param iv   16 字节 IV
     * @return 明文
     */
    public static String sm4Decrypt(String data, byte[] key, byte[] iv) {
        requireText(data, "密文不能为空");
        return new String(decrypt(base64Decode(data), SM4_CBC_PKCS5, key, iv), UTF_8);
    }

    /**
     * 通用对称加密。
     *
     * @param data           明文字节
     * @param key            密钥字节
     * @param transformation 算法模式
     * @param iv             IV 或 GCM Nonce
     * @return 密文字节
     */
    public static byte[] symmetricEncrypt(byte[] data, byte[] key, String transformation, byte[] iv) {
        return encrypt(data, transformation, key, iv);
    }

    /**
     * 通用对称解密。
     *
     * @param data           密文字节
     * @param key            密钥字节
     * @param transformation 算法模式
     * @param iv             IV 或 GCM Nonce
     * @return 明文字节
     */
    public static byte[] symmetricDecrypt(byte[] data, byte[] key, String transformation, byte[] iv) {
        return decrypt(data, transformation, key, iv);
    }

    /**
     * 根据密码派生对称密钥。兼容旧方法签名，建议优先使用带盐的重载方法。
     *
     * @param password  密码
     * @param algorithm 算法，AES / DES / DESede / SM4
     * @param keySize   密钥长度，AES:128/192/256，DES:56，DESede:168，SM4:128
     * @return 密钥字节
     */
    public static byte[] generateKey(String password, String algorithm, int keySize) {
        byte[] salt = (SecureUtil.class.getName() + ":" + normalizeAlgorithmName(algorithm)).getBytes(UTF_8);
        return generateKey(password, salt, algorithm, keySize, DEFAULT_PBKDF2_ITERATIONS);
    }

    /**
     * 根据密码和盐派生对称密钥。
     *
     * @param password   密码
     * @param salt       盐值
     * @param algorithm  算法，AES / DES / DESede / SM4
     * @param keySize    密钥长度，AES:128/192/256，DES:56，DESede:168，SM4:128
     * @param iterations PBKDF2 迭代次数
     * @return 密钥字节
     */
    public static byte[] generateKey(String password, byte[] salt, String algorithm, int keySize, int iterations) {
        requireText(password, "密码不能为空");
        Objects.requireNonNull(salt, "盐值不能为空");
        if (salt.length < 8) {
            throw new IllegalArgumentException("盐值长度不能小于8字节");
        }
        if (iterations < 10_000) {
            throw new IllegalArgumentException("PBKDF2迭代次数不能小于10000");
        }

        char[] chars = password.toCharArray();
        try {
            int actualKeyBits = actualKeyBytes(algorithm, keySize) * Byte.SIZE;
            PBEKeySpec keySpec = new PBEKeySpec(chars, salt, iterations, actualKeyBits);
            SecretKeyFactory factory = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256");
            return factory.generateSecret(keySpec).getEncoded();
        } catch (Exception e) {
            throw wrap(e, "生成密钥失败");
        } finally {
            Arrays.fill(chars, '\0');
        }
    }

    /**
     * 生成随机密钥。
     *
     * @param algorithm 算法，AES / DES / DESede / SM4
     * @param keySize   密钥长度
     * @return 密钥字节
     */
    public static byte[] generateRandomKey(String algorithm, int keySize) {
        try {
            KeyGenerator keyGenerator = getKeyGenerator(algorithm);
            keyGenerator.init(keySize, RANDOM);
            return keyGenerator.generateKey().getEncoded();
        } catch (Exception e) {
            throw wrap(e, "生成随机密钥失败");
        }
    }

    /**
     * 生成随机 IV。
     *
     * @param length IV 长度
     * @return IV 字节
     */
    public static byte[] generateIv(int length) {
        return randomIv(length);
    }

    /**
     * 生成 RSA 密钥对。
     *
     * @param keySize 密钥长度，建议不小于 2048
     * @return RSA 密钥对
     */
    public static KeyPair generateRsaKeyPair(int keySize) {
        return generateKeyPair("RSA", keySize);
    }

    /**
     * 公钥转 Base64。
     *
     * @param publicKey 公钥
     * @return Base64 公钥
     */
    public static String getPublicKey(PublicKey publicKey) {
        return publicKeyToBase64(publicKey);
    }

    /**
     * 私钥转 Base64。
     *
     * @param privateKey 私钥
     * @return Base64 私钥
     */
    public static String getPrivateKey(PrivateKey privateKey) {
        return privateKeyToBase64(privateKey);
    }

    /**
     * Base64 或 PEM 转 RSA 公钥。
     *
     * @param base64Key Base64 或 PEM 公钥
     * @return RSA 公钥
     */
    public static PublicKey loadPublicKey(String base64Key) {
        return loadPublicKey(base64Key, "RSA");
    }

    /**
     * Base64 或 PEM 转 RSA 私钥。
     *
     * @param base64Key Base64 或 PEM 私钥
     * @return RSA 私钥
     */
    public static PrivateKey loadPrivateKey(String base64Key) {
        return loadPrivateKey(base64Key, "RSA");
    }

    /**
     * PKCS#1 RSA 私钥转 PKCS#8 私钥对象。
     *
     * @param base64Key PKCS#1 Base64 或 PEM 私钥
     * @return RSA 私钥
     */
    public static PrivateKey loadPkcs1PrivateKey(String base64Key) {
        try {
            byte[] pkcs1Bytes = base64Decode(fromPem(base64Key));
            PKCS8EncodedKeySpec spec = new PKCS8EncodedKeySpec(wrapPkcs1ToPkcs8(pkcs1Bytes));
            return KeyFactory.getInstance("RSA").generatePrivate(spec);
        } catch (Exception e) {
            throw wrap(e, "加载PKCS#1私钥失败");
        }
    }

    /**
     * PKCS#1 私钥字节封装为 PKCS#8 字节。
     *
     * @param pkcs1 PKCS#1 私钥字节
     * @return PKCS#8 私钥字节
     */
    private static byte[] wrapPkcs1ToPkcs8(byte[] pkcs1) {
        Objects.requireNonNull(pkcs1, "PKCS#1私钥不能为空");
        try {
            byte[] version = derIntegerZero();
            byte[] rsaOid = new byte[]{0x06, 0x09, 0x2a, (byte) 0x86, 0x48, (byte) 0x86, (byte) 0xf7, 0x0d, 0x01, 0x01, 0x01};
            byte[] nullParam = new byte[]{0x05, 0x00};
            byte[] algorithmIdentifier = derSequence(rsaOid, nullParam);
            byte[] privateKey = derOctetString(pkcs1);
            return derSequence(version, algorithmIdentifier, privateKey);
        } catch (Exception e) {
            throw wrap(e, "PKCS#1转PKCS#8失败");
        }
    }

    /**
     * RSA 公钥加密，返回 Base64。
     *
     * @param data      明文
     * @param publicKey 公钥
     * @return Base64 密文
     */
    public static String rsaEncryptByPublicKey(String data, PublicKey publicKey) {
        return rsaEncryptByPublicKey(data, publicKey, RSA_PKCS1);
    }

    /**
     * RSA 公钥加密，返回 Base64。
     *
     * @param data           明文
     * @param publicKey      公钥
     * @param transformation RSA 模式
     * @return Base64 密文
     */
    public static String rsaEncryptByPublicKey(String data, PublicKey publicKey, String transformation) {
        requireText(data, "明文不能为空");
        return base64Encode(rsaSplitCodec(data.getBytes(UTF_8), publicKey, Cipher.ENCRYPT_MODE, transformation));
    }

    /**
     * RSA 私钥解密。
     *
     * @param data       Base64 密文
     * @param privateKey 私钥
     * @return 明文
     */
    public static String rsaDecryptByPrivateKey(String data, PrivateKey privateKey) {
        return rsaDecryptByPrivateKey(data, privateKey, RSA_PKCS1);
    }

    /**
     * RSA 私钥解密。
     *
     * @param data           Base64 密文
     * @param privateKey     私钥
     * @param transformation RSA 模式
     * @return 明文
     */
    public static String rsaDecryptByPrivateKey(String data, PrivateKey privateKey, String transformation) {
        requireText(data, "密文不能为空");
        byte[] result = rsaSplitCodec(base64Decode(data), privateKey, Cipher.DECRYPT_MODE, transformation);
        return new String(result, UTF_8);
    }

    /**
     * RSA 私钥加密，返回 Base64。
     *
     * @param data       明文
     * @param privateKey 私钥
     * @return Base64 密文
     */
    public static String rsaEncryptByPrivateKey(String data, PrivateKey privateKey) {
        return rsaEncryptByPrivateKey(data, privateKey, RSA_PKCS1);
    }

    /**
     * RSA 私钥加密，返回 Base64。
     *
     * @param data           明文
     * @param privateKey     私钥
     * @param transformation RSA 模式
     * @return Base64 密文
     */
    public static String rsaEncryptByPrivateKey(String data, PrivateKey privateKey, String transformation) {
        requireText(data, "明文不能为空");
        return base64Encode(rsaSplitCodec(data.getBytes(UTF_8), privateKey, Cipher.ENCRYPT_MODE, transformation));
    }

    /**
     * RSA 公钥解密。
     *
     * @param data      Base64 密文
     * @param publicKey 公钥
     * @return 明文
     */
    public static String rsaDecryptByPublicKey(String data, PublicKey publicKey) {
        return rsaDecryptByPublicKey(data, publicKey, RSA_PKCS1);
    }

    /**
     * RSA 公钥解密。
     *
     * @param data           Base64 密文
     * @param publicKey      公钥
     * @param transformation RSA 模式
     * @return 明文
     */
    public static String rsaDecryptByPublicKey(String data, PublicKey publicKey, String transformation) {
        requireText(data, "密文不能为空");
        byte[] result = rsaSplitCodec(base64Decode(data), publicKey, Cipher.DECRYPT_MODE, transformation);
        return new String(result, UTF_8);
    }

    /**
     * RSA 分段处理。
     *
     * @param data           输入数据
     * @param key            RSA 密钥
     * @param mode           Cipher 模式
     * @param transformation RSA 模式
     * @return 输出数据
     */
    private static byte[] rsaSplitCodec(byte[] data, Key key, int mode, String transformation) {
        try {
            Objects.requireNonNull(data, "数据不能为空");
            Objects.requireNonNull(key, "RSA密钥不能为空");
            if (!(key instanceof RSAKey rsaKey)) {
                throw new IllegalArgumentException("密钥不是RSA密钥");
            }

            Cipher cipher = Cipher.getInstance(transformation);
            cipher.init(mode, key);

            int keyBytes = (rsaKey.getModulus().bitLength() + 7) / 8;
            int maxBlock = mode == Cipher.ENCRYPT_MODE ? rsaEncryptBlockSize(transformation, keyBytes) : keyBytes;
            if (maxBlock <= 0) {
                throw new IllegalArgumentException("RSA密钥长度过短，无法使用当前填充模式");
            }

            try (ByteArrayOutputStream out = new ByteArrayOutputStream()) {
                for (int offset = 0; offset < data.length; offset += maxBlock) {
                    int len = Math.min(maxBlock, data.length - offset);
                    out.write(cipher.doFinal(data, offset, len));
                }
                return out.toByteArray();
            }
        } catch (Exception e) {
            throw wrap(e, "RSA分段处理失败");
        }
    }

    /**
     * 初始化 BouncyCastle Provider。
     */
    public static void initBouncyCastle() {
        if (Security.getProvider(BC_PROVIDER) != null) {
            return;
        }
        synchronized (SecureUtil.class) {
            if (Security.getProvider(BC_PROVIDER) != null) {
                return;
            }
            try {
                Class<?> providerClass = Class.forName(BC_PROVIDER_CLASS);
                Constructor<?> constructor = providerClass.getDeclaredConstructor();
                Provider provider = (Provider) constructor.newInstance();
                Security.addProvider(provider);
            } catch (ReflectiveOperationException e) {
                throw wrap(e, "初始化BouncyCastle失败，请添加bcprov-jdk18on依赖");
            }
        }
    }

    /**
     * 生成 SM2 密钥对。
     *
     * @return SM2 密钥对
     */
    public static KeyPair generateSm2KeyPair() {
        try {
            initBouncyCastle();
            KeyPairGenerator generator = KeyPairGenerator.getInstance("EC", BC_PROVIDER);
            generator.initialize(new ECGenParameterSpec("sm2p256v1"), RANDOM);
            return generator.generateKeyPair();
        } catch (Exception e) {
            throw wrap(e, "生成SM2密钥对失败");
        }
    }

    /**
     * SM2 公钥转 Base64。
     *
     * @param publicKey SM2 公钥
     * @return Base64 公钥
     */
    public static String sm2PublicKeyToBase64(PublicKey publicKey) {
        return publicKeyToBase64(publicKey);
    }

    /**
     * SM2 私钥转 Base64。
     *
     * @param privateKey SM2 私钥
     * @return Base64 私钥
     */
    public static String sm2PrivateKeyToBase64(PrivateKey privateKey) {
        return privateKeyToBase64(privateKey);
    }

    /**
     * Base64 或 PEM 转 SM2 公钥。
     *
     * @param base64 Base64 或 PEM 公钥
     * @return SM2 公钥
     */
    public static PublicKey loadSm2PublicKey(String base64) {
        return loadPublicKey(base64, "EC", BC_PROVIDER);
    }

    /**
     * Base64 或 PEM 转 SM2 私钥。
     *
     * @param base64 Base64 或 PEM 私钥
     * @return SM2 私钥
     */
    public static PrivateKey loadSm2PrivateKey(String base64) {
        return loadPrivateKey(base64, "EC", BC_PROVIDER);
    }

    /**
     * SM2 公钥加密，返回 Base64。
     *
     * @param data      明文
     * @param publicKey SM2 公钥
     * @return Base64 密文
     */
    public static String sm2Encrypt(String data, PublicKey publicKey) {
        try {
            initBouncyCastle();
            requireText(data, "明文不能为空");
            Cipher cipher = Cipher.getInstance("SM2", BC_PROVIDER);
            cipher.init(Cipher.ENCRYPT_MODE, publicKey, RANDOM);
            return base64Encode(cipher.doFinal(data.getBytes(UTF_8)));
        } catch (Exception e) {
            throw wrap(e, "SM2加密失败");
        }
    }

    /**
     * SM2 私钥解密。
     *
     * @param data       Base64 密文
     * @param privateKey SM2 私钥
     * @return 明文
     */
    public static String sm2Decrypt(String data, PrivateKey privateKey) {
        try {
            initBouncyCastle();
            requireText(data, "密文不能为空");
            Cipher cipher = Cipher.getInstance("SM2", BC_PROVIDER);
            cipher.init(Cipher.DECRYPT_MODE, privateKey);
            return new String(cipher.doFinal(base64Decode(data)), UTF_8);
        } catch (Exception e) {
            throw wrap(e, "SM2解密失败");
        }
    }

    /**
     * SM2 签名，返回 Base64。
     *
     * @param data       待签名文本
     * @param privateKey SM2 私钥
     * @return Base64 签名
     */
    public static String sm2Sign(String data, PrivateKey privateKey) {
        return sign(data, privateKey, SM3_WITH_SM2);
    }

    /**
     * SM2 验签。
     *
     * @param data      原文
     * @param sign      Base64 签名
     * @param publicKey SM2 公钥
     * @return 是否通过
     */
    public static boolean sm2Verify(String data, String sign, PublicKey publicKey) {
        return verify(data, sign, publicKey, SM3_WITH_SM2);
    }

    /**
     * SM2 标准签名，返回 Base64。
     *
     * @param data       待签名文本
     * @param privateKey SM2 私钥
     * @return Base64 签名
     */
    public static String sm2SignStd(String data, PrivateKey privateKey) {
        return sm2Sign(data, privateKey);
    }

    /**
     * SM2 标准验签。
     *
     * @param data      原文
     * @param sign      Base64 签名
     * @param publicKey SM2 公钥
     * @return 是否通过
     */
    public static boolean sm2VerifyStd(String data, String sign, PublicKey publicKey) {
        return sm2Verify(data, sign, publicKey);
    }

    /**
     * SM3 摘要，输出 hex。
     *
     * @param data 原文
     * @return hex 摘要
     */
    public static String sm3(String data) {
        requireText(data, "摘要内容不能为空");
        return bytesToHex(digest(data.getBytes(UTF_8), "SM3"));
    }

    /**
     * SM3 摘要，输出 hex。
     *
     * @param data 原文
     * @return hex 摘要
     */
    public static String sm3Hex(String data) {
        return sm3(data);
    }

    /**
     * 通用摘要。
     *
     * @param data      数据
     * @param algorithm 摘要算法
     * @return 摘要字节
     */
    public static byte[] digest(byte[] data, String algorithm) {
        try {
            Objects.requireNonNull(data, "摘要内容不能为空");
            MessageDigest md = getMessageDigest(algorithm);
            return md.digest(data);
        } catch (Exception e) {
            throw wrap(e, "摘要计算失败");
        }
    }

    /**
     * 通用摘要，字符串输出 hex。
     *
     * @param data      原文
     * @param algorithm 摘要算法
     * @return hex 摘要
     */
    public static String digest(String data, String algorithm) {
        requireText(data, "摘要内容不能为空");
        return bytesToHex(digest(data.getBytes(UTF_8), algorithm));
    }

    /**
     * 带盐摘要。
     *
     * @param data      原文
     * @param salt      盐值
     * @param algorithm 摘要算法
     * @return hex 摘要
     */
    public static String digest(String data, String salt, String algorithm) {
        requireText(data, "摘要内容不能为空");
        requireText(salt, "盐值不能为空");
        return bytesToHex(digest((data + salt).getBytes(UTF_8), algorithm));
    }

    /**
     * 文件摘要。
     *
     * @param file      文件
     * @param algorithm 摘要算法
     * @return hex 摘要
     */
    public static String digest(File file, String algorithm) {
        Objects.requireNonNull(file, "文件不能为空");
        try (InputStream is = Files.newInputStream(file.toPath(), StandardOpenOption.READ)) {
            MessageDigest md = getMessageDigest(algorithm);
            byte[] buffer = new byte[BUFFER_SIZE];
            int len;
            while ((len = is.read(buffer)) != -1) {
                md.update(buffer, 0, len);
            }
            return bytesToHex(md.digest());
        } catch (Exception e) {
            throw wrap(e, "文件摘要失败");
        }
    }

    /**
     * MD5 摘要。
     *
     * @param data 原文
     * @return hex 摘要
     */
    public static String md5(String data) {
        return digest(data, "MD5");
    }

    /**
     * SHA-1 摘要。
     *
     * @param data 原文
     * @return hex 摘要
     */
    public static String sha1(String data) {
        return digest(data, "SHA-1");
    }

    /**
     * SHA-224 摘要。
     *
     * @param data 原文
     * @return hex 摘要
     */
    public static String sha224(String data) {
        return digest(data, "SHA-224");
    }

    /**
     * SHA-256 摘要。
     *
     * @param data 原文
     * @return hex 摘要
     */
    public static String sha256(String data) {
        return digest(data, "SHA-256");
    }

    /**
     * SHA-384 摘要。
     *
     * @param data 原文
     * @return hex 摘要
     */
    public static String sha384(String data) {
        return digest(data, "SHA-384");
    }

    /**
     * SHA-512 摘要。
     *
     * @param data 原文
     * @return hex 摘要
     */
    public static String sha512(String data) {
        return digest(data, "SHA-512");
    }

    /**
     * MD5 摘要。
     *
     * @param data 数据
     * @return 摘要字节
     */
    public static byte[] md5(byte[] data) {
        return digest(data, "MD5");
    }

    /**
     * SHA-1 摘要。
     *
     * @param data 数据
     * @return 摘要字节
     */
    public static byte[] sha1(byte[] data) {
        return digest(data, "SHA-1");
    }

    /**
     * SHA-224 摘要。
     *
     * @param data 数据
     * @return 摘要字节
     */
    public static byte[] sha224(byte[] data) {
        return digest(data, "SHA-224");
    }

    /**
     * SHA-256 摘要。
     *
     * @param data 数据
     * @return 摘要字节
     */
    public static byte[] sha256(byte[] data) {
        return digest(data, "SHA-256");
    }

    /**
     * SHA-384 摘要。
     *
     * @param data 数据
     * @return 摘要字节
     */
    public static byte[] sha384(byte[] data) {
        return digest(data, "SHA-384");
    }

    /**
     * SHA-512 摘要。
     *
     * @param data 数据
     * @return 摘要字节
     */
    public static byte[] sha512(byte[] data) {
        return digest(data, "SHA-512");
    }

    /**
     * 文件 MD5 摘要。
     *
     * @param file 文件
     * @return hex 摘要
     */
    public static String md5(File file) {
        return digest(file, "MD5");
    }

    /**
     * 文件 SHA-1 摘要。
     *
     * @param file 文件
     * @return hex 摘要
     */
    public static String sha1(File file) {
        return digest(file, "SHA-1");
    }

    /**
     * 文件 SHA-224 摘要。
     *
     * @param file 文件
     * @return hex 摘要
     */
    public static String sha224(File file) {
        return digest(file, "SHA-224");
    }

    /**
     * 文件 SHA-256 摘要。
     *
     * @param file 文件
     * @return hex 摘要
     */
    public static String sha256(File file) {
        return digest(file, "SHA-256");
    }

    /**
     * 文件 SHA-384 摘要。
     *
     * @param file 文件
     * @return hex 摘要
     */
    public static String sha384(File file) {
        return digest(file, "SHA-384");
    }

    /**
     * 文件 SHA-512 摘要。
     *
     * @param file 文件
     * @return hex 摘要
     */
    public static String sha512(File file) {
        return digest(file, "SHA-512");
    }

    /**
     * MD5 加盐摘要。
     *
     * @param data 原文
     * @param salt 盐值
     * @return hex 摘要
     */
    public static String md5(String data, String salt) {
        return digest(data, salt, "MD5");
    }

    /**
     * SHA-256 加盐摘要。
     *
     * @param data 原文
     * @param salt 盐值
     * @return hex 摘要
     */
    public static String sha256(String data, String salt) {
        return digest(data, salt, "SHA-256");
    }

    /**
     * SHA-512 加盐摘要。
     *
     * @param data 原文
     * @param salt 盐值
     * @return hex 摘要
     */
    public static String sha512(String data, String salt) {
        return digest(data, salt, "SHA-512");
    }

    /**
     * 通用 HMAC。
     *
     * @param data      数据
     * @param key       密钥
     * @param algorithm HMAC 算法
     * @return HMAC 字节
     */
    public static byte[] hmac(byte[] data, byte[] key, String algorithm) {
        try {
            Objects.requireNonNull(data, "数据不能为空");
            Objects.requireNonNull(key, "HMAC密钥不能为空");
            requireText(algorithm, "HMAC算法不能为空");
            Mac mac = getMac(algorithm);
            mac.init(new SecretKeySpec(key, algorithm));
            return mac.doFinal(data);
        } catch (Exception e) {
            throw wrap(e, "HMAC计算失败");
        }
    }

    /**
     * 通用 HMAC，字符串输出 hex。
     *
     * @param data      原文
     * @param key       密钥
     * @param algorithm HMAC 算法
     * @return hex HMAC
     */
    public static String hmac(String data, String key, String algorithm) {
        requireText(data, "数据不能为空");
        requireText(key, "HMAC密钥不能为空");
        return bytesToHex(hmac(data.getBytes(UTF_8), key.getBytes(UTF_8), algorithm));
    }

    /**
     * 文件 HMAC。
     *
     * @param file      文件
     * @param key       密钥
     * @param algorithm HMAC 算法
     * @return hex HMAC
     */
    public static String hmac(File file, byte[] key, String algorithm) {
        Objects.requireNonNull(file, "文件不能为空");
        Objects.requireNonNull(key, "HMAC密钥不能为空");
        try (InputStream is = Files.newInputStream(file.toPath(), StandardOpenOption.READ)) {
            Mac mac = getMac(algorithm);
            mac.init(new SecretKeySpec(key, algorithm));

            byte[] buffer = new byte[BUFFER_SIZE];
            int len;
            while ((len = is.read(buffer)) != -1) {
                mac.update(buffer, 0, len);
            }
            return bytesToHex(mac.doFinal());
        } catch (Exception e) {
            throw wrap(e, "文件HMAC计算失败");
        }
    }

    /**
     * HmacMD5。
     *
     * @param data 原文
     * @param key  密钥
     * @return hex HMAC
     */
    public static String hmacMd5(String data, String key) {
        return hmac(data, key, "HmacMD5");
    }

    /**
     * HmacSHA1。
     *
     * @param data 原文
     * @param key  密钥
     * @return hex HMAC
     */
    public static String hmacSha1(String data, String key) {
        return hmac(data, key, "HmacSHA1");
    }

    /**
     * HmacSHA224。
     *
     * @param data 原文
     * @param key  密钥
     * @return hex HMAC
     */
    public static String hmacSha224(String data, String key) {
        return hmac(data, key, "HmacSHA224");
    }

    /**
     * HmacSHA256。
     *
     * @param data 原文
     * @param key  密钥
     * @return hex HMAC
     */
    public static String hmacSha256(String data, String key) {
        return hmac(data, key, "HmacSHA256");
    }

    /**
     * HmacSHA384。
     *
     * @param data 原文
     * @param key  密钥
     * @return hex HMAC
     */
    public static String hmacSha384(String data, String key) {
        return hmac(data, key, "HmacSHA384");
    }

    /**
     * HmacSHA512。
     *
     * @param data 原文
     * @param key  密钥
     * @return hex HMAC
     */
    public static String hmacSha512(String data, String key) {
        return hmac(data, key, "HmacSHA512");
    }

    /**
     * HmacSHA256。
     *
     * @param data 数据
     * @param key  密钥
     * @return HMAC 字节
     */
    public static byte[] hmacSha256(byte[] data, byte[] key) {
        return hmac(data, key, "HmacSHA256");
    }

    /**
     * MAC 校验，使用常量时间比较降低时序攻击风险。
     *
     * @param data        原文
     * @param key         密钥
     * @param algorithm   HMAC 算法
     * @param expectedHex 期望 hex HMAC
     * @return 是否一致
     */
    public static boolean verifyMac(String data, String key, String algorithm, String expectedHex) {
        if (isBlank(expectedHex)) {
            return false;
        }
        String actual = hmac(data, key, algorithm);
        return constantTimeEquals(actual, expectedHex.toLowerCase(Locale.ROOT));
    }

    /**
     * 通用签名。
     *
     * @param data       待签名数据
     * @param privateKey 私钥
     * @param algorithm  签名算法
     * @return 签名字节
     */
    public static byte[] sign(byte[] data, PrivateKey privateKey, String algorithm) {
        try {
            Objects.requireNonNull(data, "待签名数据不能为空");
            Objects.requireNonNull(privateKey, "私钥不能为空");
            Signature signature = getSignature(algorithm);
            signature.initSign(privateKey, RANDOM);
            signature.update(data);
            return signature.sign();
        } catch (Exception e) {
            throw wrap(e, "签名失败");
        }
    }

    /**
     * 通用验签。
     *
     * @param data      原文
     * @param sign      签名字节
     * @param publicKey 公钥
     * @param algorithm 签名算法
     * @return 是否通过
     */
    public static boolean verify(byte[] data, byte[] sign, PublicKey publicKey, String algorithm) {
        try {
            Objects.requireNonNull(data, "验签数据不能为空");
            Objects.requireNonNull(sign, "签名不能为空");
            Objects.requireNonNull(publicKey, "公钥不能为空");
            Signature signature = getSignature(algorithm);
            signature.initVerify(publicKey);
            signature.update(data);
            return signature.verify(sign);
        } catch (Exception e) {
            throw wrap(e, "验签失败");
        }
    }

    /**
     * 字符串签名，输出 Base64。
     *
     * @param data       待签名文本
     * @param privateKey 私钥
     * @param algorithm  签名算法
     * @return Base64 签名
     */
    public static String sign(String data, PrivateKey privateKey, String algorithm) {
        requireText(data, "待签名文本不能为空");
        return base64Encode(sign(data.getBytes(UTF_8), privateKey, algorithm));
    }

    /**
     * 字符串验签，输入 Base64 签名。
     *
     * @param data      原文
     * @param sign      Base64 签名
     * @param publicKey 公钥
     * @param algorithm 签名算法
     * @return 是否通过
     */
    public static boolean verify(String data, String sign, PublicKey publicKey, String algorithm) {
        requireText(data, "验签文本不能为空");
        requireText(sign, "签名不能为空");
        return verify(data.getBytes(UTF_8), base64Decode(sign), publicKey, algorithm);
    }

    /**
     * RSA 签名，输出 Base64。
     *
     * @param data       待签名文本
     * @param privateKey RSA 私钥
     * @param algorithm  签名算法
     * @return Base64 签名
     */
    public static String rsaSign(String data, PrivateKey privateKey, String algorithm) {
        return sign(data, privateKey, algorithm);
    }

    /**
     * RSA 验签。
     *
     * @param data      原文
     * @param sign      Base64 签名
     * @param publicKey RSA 公钥
     * @param algorithm 签名算法
     * @return 是否通过
     */
    public static boolean rsaVerify(String data, String sign, PublicKey publicKey, String algorithm) {
        return verify(data, sign, publicKey, algorithm);
    }

    /**
     * DSA 签名，输出 Base64。
     *
     * @param data       待签名文本
     * @param privateKey DSA 私钥
     * @param algorithm  签名算法
     * @return Base64 签名
     */
    public static String dsaSign(String data, PrivateKey privateKey, String algorithm) {
        return sign(data, privateKey, algorithm);
    }

    /**
     * DSA 验签。
     *
     * @param data      原文
     * @param sign      Base64 签名
     * @param publicKey DSA 公钥
     * @param algorithm 签名算法
     * @return 是否通过
     */
    public static boolean dsaVerify(String data, String sign, PublicKey publicKey, String algorithm) {
        return verify(data, sign, publicKey, algorithm);
    }

    /**
     * RSA 签名。
     *
     * @param data       待签名数据
     * @param privateKey RSA 私钥
     * @param algorithm  签名算法
     * @return 签名字节
     */
    public static byte[] rsaSign(byte[] data, PrivateKey privateKey, String algorithm) {
        return sign(data, privateKey, algorithm);
    }

    /**
     * RSA 验签。
     *
     * @param data      原文
     * @param sign      签名字节
     * @param publicKey RSA 公钥
     * @param algorithm 签名算法
     * @return 是否通过
     */
    public static boolean rsaVerify(byte[] data, byte[] sign, PublicKey publicKey, String algorithm) {
        return verify(data, sign, publicKey, algorithm);
    }

    /**
     * 生成对称密钥。
     *
     * @param algorithm 算法，AES / DES / DESede / SM4
     * @param keySize   密钥长度
     * @return 密钥字节
     */
    public static byte[] generateSymmetricKey(String algorithm, int keySize) {
        return generateRandomKey(algorithm, keySize);
    }

    /**
     * 生成非对称密钥对。
     *
     * @param algorithm 算法，RSA / DSA / EC
     * @param keySize   密钥长度
     * @return 密钥对
     */
    public static KeyPair generateKeyPair(String algorithm, int keySize) {
        try {
            requireText(algorithm, "密钥算法不能为空");
            KeyPairGenerator generator = KeyPairGenerator.getInstance(algorithm);
            generator.initialize(keySize, RANDOM);
            return generator.generateKeyPair();
        } catch (Exception e) {
            throw wrap(e, "生成密钥对失败");
        }
    }

    /**
     * 字节数组转对称密钥。
     *
     * @param key       密钥字节
     * @param algorithm 算法名称
     * @return 对称密钥
     */
    public static SecretKey toSecretKey(byte[] key, String algorithm) {
        Objects.requireNonNull(key, "密钥不能为空");
        requireText(algorithm, "密钥算法不能为空");
        return new SecretKeySpec(key, algorithm);
    }

    /**
     * Base64 转对称密钥。
     *
     * @param base64Key Base64 密钥
     * @param algorithm 算法名称
     * @return 对称密钥
     */
    public static SecretKey toSecretKey(String base64Key, String algorithm) {
        return toSecretKey(base64Decode(base64Key), algorithm);
    }

    /**
     * 对称密钥转 Base64。
     *
     * @param key 对称密钥
     * @return Base64 密钥
     */
    public static String secretKeyToBase64(SecretKey key) {
        return keyToString(key);
    }

    /**
     * Key 转字节数组。
     *
     * @param key 密钥
     * @return 密钥字节
     */
    public static byte[] keyToBytes(Key key) {
        Objects.requireNonNull(key, "密钥不能为空");
        return key.getEncoded();
    }

    /**
     * Base64 或 PEM 转公钥。
     *
     * @param base64Key Base64 或 PEM 公钥
     * @param algorithm 算法名称
     * @return 公钥
     */
    public static PublicKey loadPublicKey(String base64Key, String algorithm) {
        return loadPublicKey(base64Key, algorithm, null);
    }

    /**
     * Base64 或 PEM 转公钥。
     *
     * @param base64Key Base64 或 PEM 公钥
     * @param algorithm 算法名称
     * @param provider  Provider 名称
     * @return 公钥
     */
    private static PublicKey loadPublicKey(String base64Key, String algorithm, String provider) {
        try {
            requireText(base64Key, "公钥不能为空");
            requireText(algorithm, "公钥算法不能为空");
            if (provider != null) {
                initBouncyCastle();
            }
            byte[] bytes = base64Decode(fromPem(base64Key));
            X509EncodedKeySpec spec = new X509EncodedKeySpec(bytes);
            KeyFactory factory = provider == null ? KeyFactory.getInstance(algorithm) : KeyFactory.getInstance(algorithm, provider);
            return factory.generatePublic(spec);
        } catch (Exception e) {
            throw wrap(e, "加载公钥失败");
        }
    }

    /**
     * Base64 或 PEM 转私钥。
     *
     * @param base64Key Base64 或 PEM 私钥
     * @param algorithm 算法名称
     * @return 私钥
     */
    public static PrivateKey loadPrivateKey(String base64Key, String algorithm) {
        return loadPrivateKey(base64Key, algorithm, null);
    }

    /**
     * Base64 或 PEM 转私钥。
     *
     * @param base64Key Base64 或 PEM 私钥
     * @param algorithm 算法名称
     * @param provider  Provider 名称
     * @return 私钥
     */
    private static PrivateKey loadPrivateKey(String base64Key, String algorithm, String provider) {
        try {
            requireText(base64Key, "私钥不能为空");
            requireText(algorithm, "私钥算法不能为空");
            if (provider != null) {
                initBouncyCastle();
            }
            byte[] bytes = base64Decode(fromPem(base64Key));
            PKCS8EncodedKeySpec spec = new PKCS8EncodedKeySpec(bytes);
            KeyFactory factory = provider == null ? KeyFactory.getInstance(algorithm) : KeyFactory.getInstance(algorithm, provider);
            return factory.generatePrivate(spec);
        } catch (Exception e) {
            throw wrap(e, "加载私钥失败");
        }
    }

    /**
     * 公钥转 Base64。
     *
     * @param key 公钥
     * @return Base64 公钥
     */
    public static String publicKeyToBase64(PublicKey key) {
        return keyToString(key);
    }

    /**
     * 私钥转 Base64。
     *
     * @param key 私钥
     * @return Base64 私钥
     */
    public static String privateKeyToBase64(PrivateKey key) {
        return keyToString(key);
    }

    /**
     * Base64 公钥转 PEM。
     *
     * @param base64 Base64 公钥
     * @return PEM 公钥
     */
    public static String toPemPublicKey(String base64) {
        requireText(base64, "公钥内容不能为空");
        return "-----BEGIN PUBLIC KEY-----\n" + wrapPem(base64) + "\n-----END PUBLIC KEY-----";
    }

    /**
     * Base64 私钥转 PEM。
     *
     * @param base64 Base64 私钥
     * @return PEM 私钥
     */
    public static String toPemPrivateKey(String base64) {
        requireText(base64, "私钥内容不能为空");
        return "-----BEGIN PRIVATE KEY-----\n" + wrapPem(base64) + "\n-----END PRIVATE KEY-----";
    }

    /**
     * PEM 转 Base64，去除头尾和空白字符。
     *
     * @param pem PEM 或 Base64 文本
     * @return Base64 内容
     */
    public static String fromPem(String pem) {
        requireText(pem, "PEM内容不能为空");
        return pem.replaceAll("-----BEGIN [^-]+-----", "")
                .replaceAll("-----END [^-]+-----", "")
                .replaceAll("\\s+", "");
    }

    /**
     * PEM 内容 64 列换行。
     *
     * @param base64 Base64 内容
     * @return 换行后的 Base64 内容
     */
    private static String wrapPem(String base64) {
        String normalized = fromPem(base64);
        StringBuilder sb = new StringBuilder(normalized.length() + normalized.length() / 64);
        for (int i = 0; i < normalized.length(); i += 64) {
            int end = Math.min(i + 64, normalized.length());
            sb.append(normalized, i, end).append('\n');
        }
        return sb.toString().stripTrailing();
    }

    /**
     * 校验对称密钥长度。
     *
     * @param key       密钥字节
     * @param algorithm 算法名称
     */
    public static void checkSymmetricKey(byte[] key, String algorithm) {
        Objects.requireNonNull(key, "密钥不能为空");
        String normalized = normalizeAlgorithmName(algorithm);
        int len = key.length;
        switch (normalized.toUpperCase(Locale.ROOT)) {
            case "AES" -> {
                if (len != 16 && len != 24 && len != 32) {
                    throw new IllegalArgumentException("AES密钥长度必须为16/24/32字节");
                }
            }
            case "DES" -> {
                if (len != 8) {
                    throw new IllegalArgumentException("DES密钥长度必须为8字节");
                }
            }
            case "DESEDE" -> {
                if (len != 24) {
                    throw new IllegalArgumentException("3DES密钥长度必须为24字节");
                }
            }
            case "SM4" -> {
                if (len != 16) {
                    throw new IllegalArgumentException("SM4密钥长度必须为16字节");
                }
            }
            default -> throw new IllegalArgumentException("不支持的对称算法: " + algorithm);
        }
    }

    /**
     * 生成指定长度的随机密钥或随机字节。
     *
     * @param length 字节长度
     * @return 随机字节
     */
    public static byte[] randomKey(int length) {
        if (length <= 0) {
            throw new IllegalArgumentException("长度必须大于0");
        }
        byte[] key = new byte[length];
        RANDOM.nextBytes(key);
        return key;
    }

    /**
     * 生成随机 IV。
     *
     * @param length 字节长度
     * @return IV 字节
     */
    public static byte[] randomIv(int length) {
        return randomKey(length);
    }

    /**
     * 生成随机盐值。
     *
     * @param length 字节长度
     * @return 盐值字节
     */
    public static byte[] randomSalt(int length) {
        return randomKey(length);
    }

    /**
     * 密钥转 Hex。
     *
     * @param key 密钥
     * @return hex 密钥
     */
    public static String keyToHex(Key key) {
        return bytesToHex(keyToBytes(key));
    }

    /**
     * Hex 转对称密钥。
     *
     * @param hex       hex 密钥
     * @param algorithm 算法名称
     * @return 对称密钥
     */
    public static SecretKey hexToSecretKey(String hex, String algorithm) {
        return new SecretKeySpec(hexToBytes(hex), algorithm);
    }

    /**
     * byte[] 转 Hex。
     *
     * @param bytes 字节数组
     * @return hex 字符串
     */
    public static String bytesToHex(byte[] bytes) {
        return toHex(bytes);
    }

    /**
     * Hex 转 byte[]。
     *
     * @param hex hex 字符串
     * @return 字节数组
     */
    public static byte[] hexToBytes(String hex) {
        return fromHex(hex);
    }

    /**
     * byte[] 转 Hex。
     *
     * @param bytes 字节数组
     * @return hex 字符串
     */
    public static String toHex(byte[] bytes) {
        if (bytes == null) {
            return null;
        }
        return HEX_FORMAT.formatHex(bytes);
    }

    /**
     * Hex 转 byte[]。
     *
     * @param hex hex 字符串
     * @return 字节数组
     */
    public static byte[] fromHex(String hex) {
        if (isBlank(hex)) {
            return null;
        }
        String normalized = hex.trim();
        if ((normalized.length() & 1) == 1) {
            throw new IllegalArgumentException("Hex长度必须为偶数");
        }
        try {
            return HEX_FORMAT.parseHex(normalized);
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("Hex格式非法", e);
        }
    }

    /**
     * Base64 编码。
     *
     * @param data 数据
     * @return Base64 字符串
     */
    public static String base64Encode(byte[] data) {
        return toBase64(data);
    }

    /**
     * Base64 解码。
     *
     * @param data Base64 字符串
     * @return 字节数组
     */
    public static byte[] base64Decode(String data) {
        return fromBase64(data);
    }

    /**
     * Base64 编码。
     *
     * @param data 数据
     * @return Base64 字符串
     */
    public static String toBase64(byte[] data) {
        if (data == null) {
            return null;
        }
        return Base64.getEncoder().encodeToString(data);
    }

    /**
     * Base64 解码。
     *
     * @param base64 Base64 字符串
     * @return 字节数组
     */
    public static byte[] fromBase64(String base64) {
        if (isBlank(base64)) {
            return null;
        }
        return Base64.getDecoder().decode(base64.replaceAll("\\s+", ""));
    }

    /**
     * String 转 byte[]。
     *
     * @param data 文本
     * @return UTF-8 字节
     */
    public static byte[] toBytes(String data) {
        if (data == null) {
            return null;
        }
        return data.getBytes(UTF_8);
    }

    /**
     * byte[] 转 String。
     *
     * @param bytes 字节数组
     * @return UTF-8 文本
     */
    public static String toString(byte[] bytes) {
        if (bytes == null) {
            return null;
        }
        return new String(bytes, UTF_8);
    }

    /**
     * Key 转 Base64。
     *
     * @param key 密钥
     * @return Base64 密钥
     */
    public static String keyToString(Key key) {
        if (key == null) {
            return null;
        }
        return toBase64(key.getEncoded());
    }

    /**
     * Base64 转 SecretKey。
     *
     * @param base64Key Base64 密钥
     * @param algorithm 算法名称
     * @return 对称密钥
     */
    public static SecretKey stringToSecretKey(String base64Key, String algorithm) {
        return toSecretKey(base64Key, algorithm);
    }

    /**
     * 判断 Cipher 算法模式是否可用。
     *
     * @param transformation 算法模式
     * @return 是否可用
     */
    public static boolean isAlgorithmSupported(String transformation) {
        try {
            getCipher(transformation);
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    /**
     * 统一异常包装。
     *
     * @param e       异常
     * @param message 错误信息
     * @return RuntimeException
     */
    public static RuntimeException wrap(Throwable e, String message) {
        if (e instanceof RuntimeException runtimeException) {
            return new RuntimeException(message, runtimeException);
        }
        return new RuntimeException(message, e);
    }

    /**
     * 空值判断。
     *
     * @param str 字符串
     * @return 是否为空白
     */
    public static boolean isEmpty(String str) {
        return isBlank(str);
    }

    /**
     * 空白判断。
     *
     * @param str 字符串
     * @return 是否为空白
     */
    public static boolean isBlank(String str) {
        return str == null || str.isBlank();
    }

    /**
     * 空值保护。
     *
     * @param str 字符串
     * @return 非 null 字符串
     */
    public static String safe(String str) {
        return str == null ? "" : str;
    }

    /**
     * 非空校验。
     *
     * @param obj     对象
     * @param message 错误信息
     */
    public static void requireNonNull(Object obj, String message) {
        if (obj == null) {
            throw new IllegalArgumentException(message);
        }
    }

    /**
     * 合并多个 byte[]。
     *
     * @param arrays 多个数组
     * @return 合并后的数组
     */
    public static byte[] merge(byte[]... arrays) {
        if (arrays == null || arrays.length == 0) {
            return new byte[0];
        }
        int total = 0;
        for (byte[] arr : arrays) {
            if (arr != null) {
                total += arr.length;
            }
        }
        byte[] result = new byte[total];
        int pos = 0;
        for (byte[] arr : arrays) {
            if (arr != null && arr.length > 0) {
                System.arraycopy(arr, 0, result, pos, arr.length);
                pos += arr.length;
            }
        }
        return result;
    }

    /**
     * 摘要加盐。
     *
     * @param data      原文
     * @param salt      盐值
     * @param algorithm 摘要算法
     * @return hex 摘要
     */
    public static String digestWithSalt(String data, byte[] salt, String algorithm) {
        requireText(data, "摘要内容不能为空");
        Objects.requireNonNull(salt, "盐值不能为空");
        return toHex(digest(merge(toBytes(data), salt), algorithm));
    }

    /**
     * 加密后输出 Base64。
     *
     * @param data           明文字节
     * @param transformation 算法模式
     * @param key            密钥字节
     * @param iv             IV 或 GCM Nonce
     * @return Base64 密文
     */
    public static String encryptToBase64(byte[] data, String transformation, byte[] key, byte[] iv) {
        return toBase64(encrypt(data, transformation, key, iv));
    }

    /**
     * Base64 输入解密。
     *
     * @param base64         Base64 密文
     * @param transformation 算法模式
     * @param key            密钥字节
     * @param iv             IV 或 GCM Nonce
     * @return 明文字节
     */
    public static byte[] decryptFromBase64(String base64, String transformation, byte[] key, byte[] iv) {
        return decrypt(fromBase64(base64), transformation, key, iv);
    }

    /**
     * 签名输出 Hex。
     *
     * @param data      待签名数据
     * @param key       私钥
     * @param algorithm 签名算法
     * @return hex 签名
     */
    public static String signToHex(byte[] data, PrivateKey key, String algorithm) {
        return toHex(sign(data, key, algorithm));
    }

    /**
     * 签名输出 Base64。
     *
     * @param data      待签名数据
     * @param key       私钥
     * @param algorithm 签名算法
     * @return Base64 签名
     */
    public static String signToBase64(byte[] data, PrivateKey key, String algorithm) {
        return toBase64(sign(data, key, algorithm));
    }

    /**
     * 文本签名。
     *
     * @param data      待签名文本
     * @param key       私钥
     * @param algorithm 签名算法
     * @return Base64 签名
     */
    public static String signText(String data, PrivateKey key, String algorithm) {
        return sign(data, key, algorithm);
    }

    /**
     * 文件签名。
     *
     * @param file      文件
     * @param key       私钥
     * @param algorithm 签名算法
     * @return Base64 签名
     */
    public static String signFile(File file, PrivateKey key, String algorithm) {
        Objects.requireNonNull(file, "文件不能为空");
        try (InputStream is = Files.newInputStream(file.toPath(), StandardOpenOption.READ)) {
            return signStream(is, key, algorithm);
        } catch (Exception e) {
            throw wrap(e, "文件签名失败");
        }
    }

    /**
     * 流签名。
     *
     * @param is        输入流
     * @param key       私钥
     * @param algorithm 签名算法
     * @return Base64 签名
     */
    public static String signStream(InputStream is, PrivateKey key, String algorithm) {
        try {
            Objects.requireNonNull(is, "输入流不能为空");
            Objects.requireNonNull(key, "私钥不能为空");
            Signature signature = getSignature(algorithm);
            signature.initSign(key, RANDOM);
            byte[] buffer = new byte[BUFFER_SIZE];
            int len;
            while ((len = is.read(buffer)) != -1) {
                signature.update(buffer, 0, len);
            }
            return toBase64(signature.sign());
        } catch (Exception e) {
            throw wrap(e, "流签名失败");
        }
    }

    /**
     * 支持配置化算法加密。
     *
     * @param data           明文
     * @param transformation 算法模式
     * @param key            密钥字节
     * @param iv             IV 或 GCM Nonce
     * @return Base64 密文
     */
    public static String encryptWithAlgorithm(String data, String transformation, byte[] key, byte[] iv) {
        requireText(data, "明文不能为空");
        return encryptToBase64(data.getBytes(UTF_8), transformation, key, iv);
    }

    /**
     * 支持配置化算法解密。
     *
     * @param data           Base64 密文
     * @param transformation 算法模式
     * @param key            密钥字节
     * @param iv             IV 或 GCM Nonce
     * @return 明文
     */
    public static String decryptWithAlgorithm(String data, String transformation, byte[] key, byte[] iv) {
        return new String(decryptFromBase64(data, transformation, key, iv), UTF_8);
    }

    /**
     * 初始化对称加解密 Cipher。
     *
     * @param mode           Cipher 模式
     * @param transformation 算法模式
     * @param key            密钥字节
     * @param iv             IV 或 GCM Nonce
     * @return Cipher
     * @throws GeneralSecurityException 安全异常
     */
    private static Cipher initSymmetricCipher(int mode, String transformation, byte[] key, byte[] iv) throws GeneralSecurityException {
        requireText(transformation, "算法模式不能为空");
        String algorithm = getAlgorithmName(transformation);
        checkSymmetricKey(key, algorithm);

        Cipher cipher = getCipher(transformation);
        SecretKeySpec secretKey = new SecretKeySpec(key, algorithm);
        if (isEcbMode(transformation)) {
            if (iv != null && iv.length > 0) {
                throw new IllegalArgumentException("ECB模式不允许传入IV");
            }
            cipher.init(mode, secretKey);
            return cipher;
        }

        if (iv == null || iv.length == 0) {
            throw new IllegalArgumentException("当前模式需要IV或Nonce");
        }
        if (isGcmMode(transformation)) {
            cipher.init(mode, secretKey, new GCMParameterSpec(GCM_TAG_LENGTH_BIT, iv));
        } else {
            cipher.init(mode, secretKey, new IvParameterSpec(iv));
        }
        return cipher;
    }

    /**
     * 获取 Cipher 实例。
     *
     * @param transformation 算法模式
     * @return Cipher
     * @throws GeneralSecurityException 安全异常
     */
    private static Cipher getCipher(String transformation) throws GeneralSecurityException {
        requireText(transformation, "算法模式不能为空");
        if (requiresBouncyCastle(transformation)) {
            initBouncyCastle();
            return Cipher.getInstance(transformation, BC_PROVIDER);
        }
        return Cipher.getInstance(transformation);
    }

    /**
     * 获取 MessageDigest 实例。
     *
     * @param algorithm 摘要算法
     * @return MessageDigest
     * @throws GeneralSecurityException 安全异常
     */
    private static MessageDigest getMessageDigest(String algorithm) throws GeneralSecurityException {
        requireText(algorithm, "摘要算法不能为空");
        if (requiresBouncyCastle(algorithm)) {
            initBouncyCastle();
            return MessageDigest.getInstance(algorithm, BC_PROVIDER);
        }
        return MessageDigest.getInstance(algorithm);
    }

    /**
     * 获取 Mac 实例。
     *
     * @param algorithm HMAC 算法
     * @return Mac
     * @throws GeneralSecurityException 安全异常
     */
    private static Mac getMac(String algorithm) throws GeneralSecurityException {
        requireText(algorithm, "HMAC算法不能为空");
        if (requiresBouncyCastle(algorithm)) {
            initBouncyCastle();
            return Mac.getInstance(algorithm, BC_PROVIDER);
        }
        return Mac.getInstance(algorithm);
    }

    /**
     * 获取 Signature 实例。
     *
     * @param algorithm 签名算法
     * @return Signature
     * @throws GeneralSecurityException 安全异常
     */
    private static Signature getSignature(String algorithm) throws GeneralSecurityException {
        requireText(algorithm, "签名算法不能为空");
        if (requiresBouncyCastle(algorithm)) {
            initBouncyCastle();
            return Signature.getInstance(algorithm, BC_PROVIDER);
        }
        return Signature.getInstance(algorithm);
    }

    /**
     * 获取 KeyGenerator 实例。
     *
     * @param algorithm 算法名称
     * @return KeyGenerator
     * @throws GeneralSecurityException 安全异常
     */
    private static KeyGenerator getKeyGenerator(String algorithm) throws GeneralSecurityException {
        requireText(algorithm, "密钥算法不能为空");
        if (requiresBouncyCastle(algorithm)) {
            initBouncyCastle();
            return KeyGenerator.getInstance(algorithm, BC_PROVIDER);
        }
        return KeyGenerator.getInstance(algorithm);
    }

    /**
     * 提取算法名称。
     *
     * @param transformation 算法模式
     * @return 算法名称
     */
    private static String getAlgorithmName(String transformation) {
        requireText(transformation, "算法模式不能为空");
        int index = transformation.indexOf('/');
        return normalizeAlgorithmName(index > 0 ? transformation.substring(0, index) : transformation);
    }

    /**
     * 标准化算法名称。
     *
     * @param algorithm 算法名称
     * @return 标准算法名称
     */
    private static String normalizeAlgorithmName(String algorithm) {
        requireText(algorithm, "算法名称不能为空");
        return switch (algorithm.toUpperCase(Locale.ROOT)) {
            case "AES" -> "AES";
            case "DES" -> "DES";
            case "DESEDE", "3DES", "TRIPLEDES" -> "DESede";
            case "SM4" -> "SM4";
            default -> algorithm;
        };
    }

    /**
     * 是否 ECB 模式。
     *
     * @param transformation 算法模式
     * @return 是否 ECB
     */
    private static boolean isEcbMode(String transformation) {
        return transformation.toUpperCase(Locale.ROOT).contains("/ECB/");
    }

    /**
     * 是否 GCM 模式。
     *
     * @param transformation 算法模式
     * @return 是否 GCM
     */
    private static boolean isGcmMode(String transformation) {
        return transformation.toUpperCase(Locale.ROOT).contains("/GCM/");
    }

    /**
     * 是否需要 BouncyCastle。
     *
     * @param algorithmOrTransformation 算法或算法模式
     * @return 是否需要 BC
     */
    private static boolean requiresBouncyCastle(String algorithmOrTransformation) {
        String upper = algorithmOrTransformation.toUpperCase(Locale.ROOT);
        return upper.startsWith("SM") || upper.contains("WITHSM2") || upper.contains("SM3") || upper.contains("SM4");
    }

    /**
     * 计算密码派生密钥实际字节长度。
     *
     * @param algorithm 算法名称
     * @param keySize   密钥位数
     * @return 字节长度
     */
    private static int actualKeyBytes(String algorithm, int keySize) {
        String normalized = normalizeAlgorithmName(algorithm);
        return switch (normalized.toUpperCase(Locale.ROOT)) {
            case "AES" -> switch (keySize) {
                case 128 -> 16;
                case 192 -> 24;
                case 256 -> 32;
                default -> throw new IllegalArgumentException("AES密钥长度必须为128/192/256位");
            };
            case "DES" -> {
                if (keySize != 56 && keySize != 64) {
                    throw new IllegalArgumentException("DES密钥长度必须为56位或64位");
                }
                yield 8;
            }
            case "DESEDE" -> {
                if (keySize != 168 && keySize != 192) {
                    throw new IllegalArgumentException("3DES密钥长度必须为168位或192位");
                }
                yield 24;
            }
            case "SM4" -> {
                if (keySize != 128) {
                    throw new IllegalArgumentException("SM4密钥长度必须为128位");
                }
                yield 16;
            }
            default -> throw new IllegalArgumentException("不支持的对称算法: " + algorithm);
        };
    }

    /**
     * RSA 加密分段大小。
     *
     * @param transformation RSA 模式
     * @param keyBytes       密钥字节数
     * @return 最大加密分段大小
     */
    private static int rsaEncryptBlockSize(String transformation, int keyBytes) {
        String upper = transformation.toUpperCase(Locale.ROOT);
        if (upper.contains("OAEPWITHSHA-512")) {
            return keyBytes - 2 * 64 - 2;
        }
        if (upper.contains("OAEPWITHSHA-384")) {
            return keyBytes - 2 * 48 - 2;
        }
        if (upper.contains("OAEPWITHSHA-256")) {
            return keyBytes - 2 * 32 - 2;
        }
        if (upper.contains("OAEPWITHSHA-224")) {
            return keyBytes - 2 * 28 - 2;
        }
        if (upper.contains("OAEP")) {
            return keyBytes - 2 * 20 - 2;
        }
        return keyBytes - 11;
    }

    /**
     * 常量时间字符串比较。
     *
     * @param a 字符串 A
     * @param b 字符串 B
     * @return 是否一致
     */
    private static boolean constantTimeEquals(String a, String b) {
        if (a == null || b == null) {
            return false;
        }
        return MessageDigest.isEqual(a.getBytes(UTF_8), b.getBytes(UTF_8));
    }

    /**
     * 非空文本校验。
     *
     * @param value   文本
     * @param message 错误信息
     */
    private static void requireText(String value, String message) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(message);
        }
    }

    /**
     * 原子替换移动，不支持原子移动时降级为普通替换。
     *
     * @param source 源路径
     * @param target 目标路径
     */
    private static void moveReplace(Path source, Path target) {
        try {
            Files.move(source, target, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
        } catch (AtomicMoveNotSupportedException e) {
            try {
                Files.move(source, target, StandardCopyOption.REPLACE_EXISTING);
            } catch (Exception ex) {
                throw wrap(ex, "文件替换失败");
            }
        } catch (Exception e) {
            throw wrap(e, "文件替换失败");
        }
    }

    /**
     * 删除文件，忽略删除异常。
     *
     * @param path 文件路径
     */
    private static void deleteIfExists(Path path) {
        if (path == null) {
            return;
        }
        try {
            Files.deleteIfExists(path);
        } catch (Exception ignored) {
            // 忽略临时文件清理异常，避免覆盖主异常。
        }
    }

    /**
     * DER INTEGER 0。
     *
     * @return DER 字节
     */
    private static byte[] derIntegerZero() {
        return new byte[]{0x02, 0x01, 0x00};
    }

    /**
     * DER SEQUENCE。
     *
     * @param values DER 子项
     * @return DER 字节
     */
    private static byte[] derSequence(byte[]... values) {
        return derEncode((byte) 0x30, merge(values));
    }

    /**
     * DER OCTET STRING。
     *
     * @param value 内容
     * @return DER 字节
     */
    private static byte[] derOctetString(byte[] value) {
        return derEncode((byte) 0x04, value);
    }

    /**
     * DER 编码。
     *
     * @param tag   标签
     * @param value 内容
     * @return DER 字节
     */
    private static byte[] derEncode(byte tag, byte[] value) {
        return merge(new byte[]{tag}, derLength(value.length), value);
    }

    /**
     * DER 长度编码。
     *
     * @param length 长度
     * @return DER 长度字节
     */
    private static byte[] derLength(int length) {
        if (length < 0) {
            throw new IllegalArgumentException("DER长度不能小于0");
        }
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
            result[i] = (byte) (length & 0xff);
            length >>= 8;
        }
        return result;
    }
}
