package io.github.atengk.controller;

import cn.hutool.core.lang.Dict;
import cn.hutool.core.util.StrUtil;
import io.github.atengk.utils.SecureUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.bind.annotation.*;

import javax.crypto.SecretKey;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.KeyPair;
import java.security.PrivateKey;
import java.security.PublicKey;

/**
 * SecureUtil 工具类使用示例接口。
 * <p>
 * 该 Controller 覆盖 SecureUtil 的常量、编码转换、摘要、HMAC、对称加密、RSA、DSA、签名验签、
 * 文件处理、密钥转换和国密 SM2/SM3/SM4 相关方法，便于在 Spring Boot 3 + JDK 21 项目中验证工具类能力。
 * </p>
 * <p>
 * 注意：SM2、SM3、SM4 需要项目引入 BouncyCastle Provider 依赖，否则相关接口会返回错误信息。
 * 文件加解密接口会创建临时文件，不依赖固定本地路径。
 * </p>
 *
 * @author Ateng
 * @since 2026-04-27
 */
@RestController
@RequestMapping("/api/secure")
public class SecureDemoController {

    private static final Logger log = LoggerFactory.getLogger(SecureDemoController.class);

    private static final String DEFAULT_TEXT = "你好，SecureUtil";
    private static final String AES_KEY = "1234567890abcdef";
    private static final byte[] AES_KEY_BYTES = AES_KEY.getBytes(StandardCharsets.UTF_8);
    private static final byte[] AES_IV = "abcdef1234567890".getBytes(StandardCharsets.UTF_8);
    private static final String DES_KEY = "abcd1234";
    private static final byte[] DES_IV = "12345678".getBytes(StandardCharsets.UTF_8);
    private static final String DESEDE_KEY = "123456789012345678901234";
    private static final byte[] DESEDE_IV = "87654321".getBytes(StandardCharsets.UTF_8);
    private static final String HMAC_KEY = "secure-hmac-key";

    /**
     * 查看 SecureUtil 常量和算法支持情况。
     *
     * @return 常量信息
     */
    @GetMapping("/constants")
    public Dict constants() {
        return Dict.create()
                .set("AES_ECB_PKCS5", SecureUtil.AES_ECB_PKCS5)
                .set("AES_CBC_PKCS5", SecureUtil.AES_CBC_PKCS5)
                .set("AES_GCM_NOPADDING", SecureUtil.AES_GCM_NOPADDING)
                .set("DES_ECB_PKCS5", SecureUtil.DES_ECB_PKCS5)
                .set("DES_CBC_PKCS5", SecureUtil.DES_CBC_PKCS5)
                .set("DESEDE_ECB_PKCS5", SecureUtil.DESEDE_ECB_PKCS5)
                .set("DESEDE_CBC_PKCS5", SecureUtil.DESEDE_CBC_PKCS5)
                .set("DESede_ECB_PKCS5", SecureUtil.DESede_ECB_PKCS5)
                .set("DESede_CBC_PKCS5", SecureUtil.DESede_CBC_PKCS5)
                .set("SM4_ECB_PKCS5", SecureUtil.SM4_ECB_PKCS5)
                .set("SM4_CBC_PKCS5", SecureUtil.SM4_CBC_PKCS5)
                .set("RSA_PKCS1", SecureUtil.RSA_PKCS1)
                .set("RSA_ECB_PKCS1", SecureUtil.RSA_ECB_PKCS1)
                .set("RSA_ECB_OAEP_SHA1", SecureUtil.RSA_ECB_OAEP_SHA1)
                .set("RSA_ECB_OAEP_SHA256", SecureUtil.RSA_ECB_OAEP_SHA256)
                .set("SHA1_WITH_RSA", SecureUtil.SHA1_WITH_RSA)
                .set("SHA256_WITH_RSA", SecureUtil.SHA256_WITH_RSA)
                .set("SHA384_WITH_RSA", SecureUtil.SHA384_WITH_RSA)
                .set("SHA512_WITH_RSA", SecureUtil.SHA512_WITH_RSA)
                .set("SHA1_WITH_DSA", SecureUtil.SHA1_WITH_DSA)
                .set("SHA256_WITH_DSA", SecureUtil.SHA256_WITH_DSA)
                .set("SM3_WITH_SM2", SecureUtil.SM3_WITH_SM2)
                .set("isAlgorithmSupported_AES_CBC", SecureUtil.isAlgorithmSupported(SecureUtil.AES_CBC_PKCS5))
                .set("isAlgorithmSupported_AES_GCM", SecureUtil.isAlgorithmSupported(SecureUtil.AES_GCM_NOPADDING))
                .set("isAlgorithmSupported_SM4_CBC", SecureUtil.isAlgorithmSupported(SecureUtil.SM4_CBC_PKCS5));
    }

    /**
     * 演示 Base64、Hex、字符串字节转换和基础校验方法。
     *
     * @param text 原始文本
     * @return 编码转换结果
     */
    @GetMapping("/codec")
    public Dict codec(@RequestParam(defaultValue = DEFAULT_TEXT) String text) {
        byte[] textBytes = SecureUtil.toBytes(text);
        String hexByBytesToHex = SecureUtil.bytesToHex(textBytes);
        String hexByToHex = SecureUtil.toHex(textBytes);
        String base64ByEncode = SecureUtil.base64Encode(textBytes);
        String base64ByToBase64 = SecureUtil.toBase64(textBytes);
        RuntimeException wrapped = SecureUtil.wrap(new IllegalStateException("原始异常"), "统一异常包装示例");

        SecureUtil.requireNonNull(text, "文本不能为空");

        return Dict.create()
                .set("toBytes", SecureUtil.toHex(textBytes))
                .set("toString", SecureUtil.toString(textBytes))
                .set("bytesToHex", hexByBytesToHex)
                .set("hexToBytes", SecureUtil.toString(SecureUtil.hexToBytes(hexByBytesToHex)))
                .set("toHex", hexByToHex)
                .set("fromHex", SecureUtil.toString(SecureUtil.fromHex(hexByToHex)))
                .set("base64Encode", base64ByEncode)
                .set("base64Decode", SecureUtil.toString(SecureUtil.base64Decode(base64ByEncode)))
                .set("toBase64", base64ByToBase64)
                .set("fromBase64", SecureUtil.toString(SecureUtil.fromBase64(base64ByToBase64)))
                .set("merge", SecureUtil.toString(SecureUtil.merge("A".getBytes(StandardCharsets.UTF_8), "B".getBytes(StandardCharsets.UTF_8))))
                .set("isEmptyBlank", SecureUtil.isEmpty("   "))
                .set("isBlank", SecureUtil.isBlank("   "))
                .set("safe", SecureUtil.safe(null))
                .set("requireNonNull", "已通过")
                .set("wrapMessage", wrapped.getMessage());
    }

    /**
     * 演示对称密钥生成、随机数、SecretKey 转换和密钥校验方法。
     *
     * @return 密钥相关结果
     */
    @GetMapping("/key")
    public Dict key() {
        byte[] generatedKey = SecureUtil.generateKey("demo-password", "AES", 128);
        byte[] generatedKeyWithSalt = SecureUtil.generateKey("demo-password", SecureUtil.randomSalt(16), "AES", 128, 120_000);
        byte[] randomKey = SecureUtil.generateRandomKey("AES", 128);
        byte[] symmetricKey = SecureUtil.generateSymmetricKey("AES", 128);
        byte[] generateIv = SecureUtil.generateIv(16);
        byte[] randomIv = SecureUtil.randomIv(16);
        byte[] randomSalt = SecureUtil.randomSalt(16);
        byte[] rawRandomKey = SecureUtil.randomKey(16);

        SecureUtil.checkSymmetricKey(randomKey, "AES");
        SecretKey secretKey = SecureUtil.toSecretKey(randomKey, "AES");
        String secretKeyBase64 = SecureUtil.secretKeyToBase64(secretKey);
        SecretKey secretKeyFromBase64 = SecureUtil.toSecretKey(secretKeyBase64, "AES");
        SecretKey secretKeyFromString = SecureUtil.stringToSecretKey(secretKeyBase64, "AES");
        String secretKeyHex = SecureUtil.keyToHex(secretKey);
        SecretKey secretKeyFromHex = SecureUtil.hexToSecretKey(secretKeyHex, "AES");

        return Dict.create()
                .set("generateKey", SecureUtil.toHex(generatedKey))
                .set("generateKeyWithSalt", SecureUtil.toHex(generatedKeyWithSalt))
                .set("generateRandomKey", SecureUtil.toHex(randomKey))
                .set("generateSymmetricKey", SecureUtil.toHex(symmetricKey))
                .set("generateIv", SecureUtil.toHex(generateIv))
                .set("randomIv", SecureUtil.toHex(randomIv))
                .set("randomSalt", SecureUtil.toHex(randomSalt))
                .set("randomKey", SecureUtil.toHex(rawRandomKey))
                .set("checkSymmetricKey", "已通过")
                .set("toSecretKeyBytes", secretKey.getAlgorithm())
                .set("toSecretKeyBase64", secretKeyFromBase64.getAlgorithm())
                .set("stringToSecretKey", secretKeyFromString.getAlgorithm())
                .set("secretKeyToBase64", secretKeyBase64)
                .set("keyToBytes", SecureUtil.toHex(SecureUtil.keyToBytes(secretKey)))
                .set("keyToString", SecureUtil.keyToString(secretKey))
                .set("keyToHex", secretKeyHex)
                .set("hexToSecretKey", secretKeyFromHex.getAlgorithm());
    }

    /**
     * 演示摘要、带盐摘要和文件摘要方法。
     *
     * @param text 原始文本
     * @return 摘要结果
     */
    @GetMapping("/digest")
    public Dict digest(@RequestParam(defaultValue = DEFAULT_TEXT) String text) {
        byte[] bytes = SecureUtil.toBytes(text);
        return Dict.create()
                .set("digestBytes_SHA256", SecureUtil.toHex(SecureUtil.digest(bytes, "SHA-256")))
                .set("digestString_SHA256", SecureUtil.digest(text, "SHA-256"))
                .set("digestStringWithSalt_SHA256", SecureUtil.digest(text, "salt", "SHA-256"))
                .set("digestWithSalt", SecureUtil.digestWithSalt(text, SecureUtil.randomSalt(16), "SHA-256"))
                .set("md5WithSalt", SecureUtil.md5(text, "salt"))
                .set("sha256WithSalt", SecureUtil.sha256(text, "salt"))
                .set("sha512WithSalt", SecureUtil.sha512(text, "salt"))
                .set("md5String", SecureUtil.md5(text))
                .set("sha1String", SecureUtil.sha1(text))
                .set("sha224String", safeValue("sha224String", () -> SecureUtil.sha224(text)))
                .set("sha256String", SecureUtil.sha256(text))
                .set("sha384String", SecureUtil.sha384(text))
                .set("sha512String", SecureUtil.sha512(text))
                .set("md5Bytes", SecureUtil.toHex(SecureUtil.md5(bytes)))
                .set("sha1Bytes", SecureUtil.toHex(SecureUtil.sha1(bytes)))
                .set("sha224Bytes", safeValue("sha224Bytes", () -> SecureUtil.toHex(SecureUtil.sha224(bytes))))
                .set("sha256Bytes", SecureUtil.toHex(SecureUtil.sha256(bytes)))
                .set("sha384Bytes", SecureUtil.toHex(SecureUtil.sha384(bytes)))
                .set("sha512Bytes", SecureUtil.toHex(SecureUtil.sha512(bytes)))
                .set("fileDigest", safeValue("fileDigest", () -> {
                    Path file = createTempTextFile(text);
                    File source = file.toFile();
                    return Dict.create()
                            .set("digestFile_SHA256", SecureUtil.digest(source, "SHA-256"))
                            .set("md5File", SecureUtil.md5(source))
                            .set("sha1File", SecureUtil.sha1(source))
                            .set("sha224File", safeValue("sha224File", () -> SecureUtil.sha224(source)))
                            .set("sha256File", SecureUtil.sha256(source))
                            .set("sha384File", SecureUtil.sha384(source))
                            .set("sha512File", SecureUtil.sha512(source));
                }));
    }

    /**
     * 演示 HMAC 和 MAC 校验方法。
     *
     * @param text 原始文本
     * @return HMAC 结果
     */
    @GetMapping("/hmac")
    public Dict hmac(@RequestParam(defaultValue = DEFAULT_TEXT) String text) {
        byte[] data = SecureUtil.toBytes(text);
        byte[] key = SecureUtil.toBytes(HMAC_KEY);
        String hmacSha256 = SecureUtil.hmacSha256(text, HMAC_KEY);

        return Dict.create()
                .set("hmacBytes", SecureUtil.toHex(SecureUtil.hmac(data, key, "HmacSHA256")))
                .set("hmacString", SecureUtil.hmac(text, HMAC_KEY, "HmacSHA256"))
                .set("hmacMd5", SecureUtil.hmacMd5(text, HMAC_KEY))
                .set("hmacSha1", SecureUtil.hmacSha1(text, HMAC_KEY))
                .set("hmacSha224", safeValue("hmacSha224", () -> SecureUtil.hmacSha224(text, HMAC_KEY)))
                .set("hmacSha256", hmacSha256)
                .set("hmacSha384", SecureUtil.hmacSha384(text, HMAC_KEY))
                .set("hmacSha512", SecureUtil.hmacSha512(text, HMAC_KEY))
                .set("hmacSha256Bytes", SecureUtil.toHex(SecureUtil.hmacSha256(data, key)))
                .set("verifyMac", SecureUtil.verifyMac(text, HMAC_KEY, "HmacSHA256", hmacSha256))
                .set("fileHmac", safeValue("fileHmac", () -> {
                    Path file = createTempTextFile(text);
                    return SecureUtil.hmac(file.toFile(), key, "HmacSHA256");
                }));
    }

    /**
     * 演示 AES、DES、3DES 和通用对称加解密方法。
     *
     * @param text 原始文本
     * @return 对称加解密结果
     */
    @GetMapping("/symmetric")
    public Dict symmetric(@RequestParam(defaultValue = DEFAULT_TEXT) String text) {
        byte[] data = SecureUtil.toBytes(text);
        byte[] desKey = SecureUtil.toBytes(DES_KEY);
        byte[] desedeKey = SecureUtil.toBytes(DESEDE_KEY);

        String encryptNoIv = SecureUtil.encrypt(text, SecureUtil.AES_ECB_PKCS5, AES_KEY);
        String decryptNoIv = SecureUtil.decrypt(encryptNoIv, SecureUtil.AES_ECB_PKCS5, AES_KEY);
        String encryptString = SecureUtil.encrypt(text, SecureUtil.AES_CBC_PKCS5, AES_KEY, AES_IV);
        String decryptString = SecureUtil.decrypt(encryptString, SecureUtil.AES_CBC_PKCS5, AES_KEY, AES_IV);
        byte[] encryptBytes = SecureUtil.encrypt(data, SecureUtil.AES_CBC_PKCS5, AES_KEY_BYTES, AES_IV);
        byte[] decryptBytes = SecureUtil.decrypt(encryptBytes, SecureUtil.AES_CBC_PKCS5, AES_KEY_BYTES, AES_IV);
        String randomIvCipher = SecureUtil.encryptWithRandomIvToBase64(data, SecureUtil.AES_CBC_PKCS5, AES_KEY_BYTES, 16);
        String randomIvPlain = SecureUtil.toString(SecureUtil.decryptWithPrefixedIvFromBase64(randomIvCipher, SecureUtil.AES_CBC_PKCS5, AES_KEY_BYTES, 16));
        String aesGcmCipher = SecureUtil.aesGcmEncrypt(text, AES_KEY_BYTES);
        String aesGcmPlain = SecureUtil.aesGcmDecrypt(aesGcmCipher, AES_KEY_BYTES);
        String aesCipher = SecureUtil.aesEncrypt(text, AES_KEY, SecureUtil.AES_CBC_PKCS5, AES_IV);
        String desCipher = SecureUtil.desEncrypt(text, DES_KEY, SecureUtil.DES_CBC_PKCS5, DES_IV);
        String tripleDesCipher = SecureUtil.tripleDesEncrypt(text, DESEDE_KEY, SecureUtil.DESEDE_CBC_PKCS5, DESEDE_IV);
        byte[] symmetricCipher = SecureUtil.symmetricEncrypt(data, AES_KEY_BYTES, SecureUtil.AES_CBC_PKCS5, AES_IV);
        String encryptToBase64 = SecureUtil.encryptToBase64(data, SecureUtil.AES_CBC_PKCS5, AES_KEY_BYTES, AES_IV);
        String encryptWithAlgorithm = SecureUtil.encryptWithAlgorithm(text, SecureUtil.AES_CBC_PKCS5, AES_KEY_BYTES, AES_IV);

        return Dict.create()
                .set("encryptStringNoIv", encryptNoIv)
                .set("decryptStringNoIv", decryptNoIv)
                .set("encryptString", encryptString)
                .set("decryptString", decryptString)
                .set("encryptBytes", SecureUtil.toBase64(encryptBytes))
                .set("decryptBytes", SecureUtil.toString(decryptBytes))
                .set("encryptWithRandomIvToBase64", randomIvCipher)
                .set("decryptWithPrefixedIvFromBase64", randomIvPlain)
                .set("aesGcmEncrypt", aesGcmCipher)
                .set("aesGcmDecrypt", aesGcmPlain)
                .set("aesEncrypt", aesCipher)
                .set("aesDecrypt", SecureUtil.aesDecrypt(aesCipher, AES_KEY, SecureUtil.AES_CBC_PKCS5, AES_IV))
                .set("desEncrypt", desCipher)
                .set("desDecrypt", SecureUtil.desDecrypt(desCipher, DES_KEY, SecureUtil.DES_CBC_PKCS5, DES_IV))
                .set("tripleDesEncrypt", tripleDesCipher)
                .set("tripleDesDecrypt", SecureUtil.tripleDesDecrypt(tripleDesCipher, DESEDE_KEY, SecureUtil.DESEDE_CBC_PKCS5, DESEDE_IV))
                .set("symmetricEncrypt", SecureUtil.toBase64(symmetricCipher))
                .set("symmetricDecrypt", SecureUtil.toString(SecureUtil.symmetricDecrypt(symmetricCipher, AES_KEY_BYTES, SecureUtil.AES_CBC_PKCS5, AES_IV)))
                .set("encryptToBase64", encryptToBase64)
                .set("decryptFromBase64", SecureUtil.toString(SecureUtil.decryptFromBase64(encryptToBase64, SecureUtil.AES_CBC_PKCS5, AES_KEY_BYTES, AES_IV)))
                .set("encryptWithAlgorithm", encryptWithAlgorithm)
                .set("decryptWithAlgorithm", SecureUtil.decryptWithAlgorithm(encryptWithAlgorithm, SecureUtil.AES_CBC_PKCS5, AES_KEY_BYTES, AES_IV))
                .set("desByteKey", SecureUtil.toHex(desKey))
                .set("desedeByteKey", SecureUtil.toHex(desedeKey));
    }

    /**
     * 演示文件加解密、文件摘要、文件 HMAC 和文件签名。
     *
     * @param text 原始文本
     * @return 文件处理结果
     */
    @PostMapping("/file")
    public Dict file(@RequestParam(defaultValue = DEFAULT_TEXT) String text) {
        return Dict.create()
                .set("fileCrypto", safeValue("fileCrypto", () -> {
                    Path source = createTempTextFile(text);
                    Path encrypted = Files.createTempFile("secure-encrypted-", ".bin");
                    Path decrypted = Files.createTempFile("secure-decrypted-", ".txt");

                    SecureUtil.encryptFile(source.toFile(), encrypted.toFile(), SecureUtil.AES_CBC_PKCS5, AES_KEY_BYTES, AES_IV);
                    SecureUtil.decryptFile(encrypted.toFile(), decrypted.toFile(), SecureUtil.AES_CBC_PKCS5, AES_KEY_BYTES, AES_IV);

                    return Dict.create()
                            .set("source", source.toString())
                            .set("encrypted", encrypted.toString())
                            .set("decrypted", decrypted.toString())
                            .set("decryptedText", Files.readString(decrypted));
                }))
                .set("fileDigest", safeValue("fileDigest", () -> {
                    Path source = createTempTextFile(text);
                    return Dict.create()
                            .set("sha256", SecureUtil.sha256(source.toFile()))
                            .set("hmacSha256", SecureUtil.hmac(source.toFile(), SecureUtil.toBytes(HMAC_KEY), "HmacSHA256"));
                }))
                .set("fileSign", safeValue("fileSign", () -> {
                    Path source = createTempTextFile(text);
                    KeyPair rsaKeyPair = SecureUtil.generateRsaKeyPair(2048);
                    String signFile = SecureUtil.signFile(source.toFile(), rsaKeyPair.getPrivate(), SecureUtil.SHA256_WITH_RSA);
                    return Dict.create()
                            .set("signFile", signFile)
                            .set("signFileLength", signFile.length());
                }));
    }

    /**
     * 演示 RSA 密钥、加解密、PEM 转换、签名验签等方法。
     *
     * @param text 原始文本
     * @return RSA 相关结果
     */
    @GetMapping("/rsa")
    public Dict rsa(@RequestParam(defaultValue = DEFAULT_TEXT) String text) {
        KeyPair keyPair = SecureUtil.generateRsaKeyPair(2048);
        PublicKey publicKey = keyPair.getPublic();
        PrivateKey privateKey = keyPair.getPrivate();
        String publicKeyBase64 = SecureUtil.getPublicKey(publicKey);
        String privateKeyBase64 = SecureUtil.getPrivateKey(privateKey);
        PublicKey loadedPublicKey = SecureUtil.loadPublicKey(publicKeyBase64);
        PrivateKey loadedPrivateKey = SecureUtil.loadPrivateKey(privateKeyBase64);
        PublicKey genericPublicKey = SecureUtil.loadPublicKey(publicKeyBase64, "RSA");
        PrivateKey genericPrivateKey = SecureUtil.loadPrivateKey(privateKeyBase64, "RSA");
        String publicPem = SecureUtil.toPemPublicKey(SecureUtil.publicKeyToBase64(publicKey));
        String privatePem = SecureUtil.toPemPrivateKey(SecureUtil.privateKeyToBase64(privateKey));

        String publicEncrypt = SecureUtil.rsaEncryptByPublicKey(text, loadedPublicKey);
        String publicDecrypt = SecureUtil.rsaDecryptByPrivateKey(publicEncrypt, loadedPrivateKey);
        String publicEncryptOaep = SecureUtil.rsaEncryptByPublicKey(text, loadedPublicKey, SecureUtil.RSA_ECB_OAEP_SHA256);
        String publicDecryptOaep = SecureUtil.rsaDecryptByPrivateKey(publicEncryptOaep, loadedPrivateKey, SecureUtil.RSA_ECB_OAEP_SHA256);
        String privateEncrypt = SecureUtil.rsaEncryptByPrivateKey(text, genericPrivateKey);
        String privateDecrypt = SecureUtil.rsaDecryptByPublicKey(privateEncrypt, genericPublicKey);
        String privateEncryptPkcs1 = SecureUtil.rsaEncryptByPrivateKey(text, genericPrivateKey, SecureUtil.RSA_PKCS1);
        String privateDecryptPkcs1 = SecureUtil.rsaDecryptByPublicKey(privateEncryptPkcs1, genericPublicKey, SecureUtil.RSA_PKCS1);

        byte[] data = SecureUtil.toBytes(text);
        byte[] signBytes = SecureUtil.sign(data, privateKey, SecureUtil.SHA256_WITH_RSA);
        String signBase64 = SecureUtil.sign(text, privateKey, SecureUtil.SHA256_WITH_RSA);
        String rsaSign = SecureUtil.rsaSign(text, privateKey, SecureUtil.SHA256_WITH_RSA);
        byte[] rsaSignBytes = SecureUtil.rsaSign(data, privateKey, SecureUtil.SHA256_WITH_RSA);
        String signToHex = SecureUtil.signToHex(data, privateKey, SecureUtil.SHA256_WITH_RSA);
        String signToBase64 = SecureUtil.signToBase64(data, privateKey, SecureUtil.SHA256_WITH_RSA);
        String signText = SecureUtil.signText(text, privateKey, SecureUtil.SHA256_WITH_RSA);
        String signStream = SecureUtil.signStream(new ByteArrayInputStream(data), privateKey, SecureUtil.SHA256_WITH_RSA);

        return Dict.create()
                .set("generateRsaKeyPair", publicKey.getAlgorithm())
                .set("getPublicKey", abbreviate(publicKeyBase64))
                .set("getPrivateKey", abbreviate(privateKeyBase64))
                .set("loadPublicKey", loadedPublicKey.getAlgorithm())
                .set("loadPrivateKey", loadedPrivateKey.getAlgorithm())
                .set("loadPublicKeyGeneric", genericPublicKey.getAlgorithm())
                .set("loadPrivateKeyGeneric", genericPrivateKey.getAlgorithm())
                .set("publicKeyToBase64", abbreviate(SecureUtil.publicKeyToBase64(publicKey)))
                .set("privateKeyToBase64", abbreviate(SecureUtil.privateKeyToBase64(privateKey)))
                .set("toPemPublicKey", abbreviate(publicPem))
                .set("toPemPrivateKey", abbreviate(privatePem))
                .set("fromPemPublicKey", SecureUtil.fromPem(publicPem).equals(publicKeyBase64))
                .set("fromPemPrivateKey", SecureUtil.fromPem(privatePem).equals(privateKeyBase64))
                .set("rsaEncryptByPublicKey", abbreviate(publicEncrypt))
                .set("rsaDecryptByPrivateKey", publicDecrypt)
                .set("rsaEncryptByPublicKeyWithTransformation", abbreviate(publicEncryptOaep))
                .set("rsaDecryptByPrivateKeyWithTransformation", publicDecryptOaep)
                .set("rsaEncryptByPrivateKey", abbreviate(privateEncrypt))
                .set("rsaDecryptByPublicKey", privateDecrypt)
                .set("rsaEncryptByPrivateKeyWithTransformation", abbreviate(privateEncryptPkcs1))
                .set("rsaDecryptByPublicKeyWithTransformation", privateDecryptPkcs1)
                .set("keyToBytes", SecureUtil.toHex(SecureUtil.keyToBytes(publicKey)))
                .set("signBytes", SecureUtil.toBase64(signBytes))
                .set("verifyBytes", SecureUtil.verify(data, signBytes, publicKey, SecureUtil.SHA256_WITH_RSA))
                .set("signString", signBase64)
                .set("verifyString", SecureUtil.verify(text, signBase64, publicKey, SecureUtil.SHA256_WITH_RSA))
                .set("rsaSignString", rsaSign)
                .set("rsaVerifyString", SecureUtil.rsaVerify(text, rsaSign, publicKey, SecureUtil.SHA256_WITH_RSA))
                .set("rsaSignBytes", SecureUtil.toBase64(rsaSignBytes))
                .set("rsaVerifyBytes", SecureUtil.rsaVerify(data, rsaSignBytes, publicKey, SecureUtil.SHA256_WITH_RSA))
                .set("signToHex", signToHex)
                .set("signToBase64", signToBase64)
                .set("signText", signText)
                .set("signStream", signStream);
    }

    /**
     * 演示 PKCS#1 私钥加载方法。
     *
     * @param base64Pkcs1 PKCS#1 私钥 Base64，不包含 PEM 头尾
     * @return 私钥加载结果
     */
    @PostMapping("/rsa/pkcs1/load")
    public Dict loadPkcs1PrivateKey(@RequestParam String base64Pkcs1) {
        PrivateKey privateKey = SecureUtil.loadPkcs1PrivateKey(base64Pkcs1);
        return Dict.create()
                .set("loadPkcs1PrivateKey", privateKey.getAlgorithm())
                .set("privateKeyToBase64", abbreviate(SecureUtil.privateKeyToBase64(privateKey)));
    }

    /**
     * 演示 DSA 密钥、签名和验签方法。
     *
     * @param text 原始文本
     * @return DSA 相关结果
     */
    @GetMapping("/dsa")
    public Dict dsa(@RequestParam(defaultValue = DEFAULT_TEXT) String text) {
        KeyPair dsaKeyPair = SecureUtil.generateKeyPair("DSA", 2048);
        String dsaSign = SecureUtil.dsaSign(text, dsaKeyPair.getPrivate(), SecureUtil.SHA256_WITH_DSA);
        return Dict.create()
                .set("generateKeyPair", dsaKeyPair.getPublic().getAlgorithm())
                .set("dsaSign", dsaSign)
                .set("dsaVerify", SecureUtil.dsaVerify(text, dsaSign, dsaKeyPair.getPublic(), SecureUtil.SHA256_WITH_DSA));
    }

    /**
     * 演示 SM2、SM3、SM4 相关方法。
     *
     * @param text 原始文本
     * @return 国密算法结果
     */
    @GetMapping("/sm")
    public Dict sm(@RequestParam(defaultValue = DEFAULT_TEXT) String text) {
        return Dict.create()
                .set("initBouncyCastle", safeValue("initBouncyCastle", () -> {
                    SecureUtil.initBouncyCastle();
                    return "已初始化";
                }))
                .set("sm2", safeValue("sm2", () -> {
                    KeyPair sm2KeyPair = SecureUtil.generateSm2KeyPair();
                    String publicBase64 = SecureUtil.sm2PublicKeyToBase64(sm2KeyPair.getPublic());
                    String privateBase64 = SecureUtil.sm2PrivateKeyToBase64(sm2KeyPair.getPrivate());
                    PublicKey publicKey = SecureUtil.loadSm2PublicKey(publicBase64);
                    PrivateKey privateKey = SecureUtil.loadSm2PrivateKey(privateBase64);
                    String cipher = SecureUtil.sm2Encrypt(text, publicKey);
                    String plain = SecureUtil.sm2Decrypt(cipher, privateKey);
                    String sign = SecureUtil.sm2Sign(text, privateKey);
                    String signStd = SecureUtil.sm2SignStd(text, privateKey);
                    return Dict.create()
                            .set("generateSm2KeyPair", sm2KeyPair.getPublic().getAlgorithm())
                            .set("sm2PublicKeyToBase64", abbreviate(publicBase64))
                            .set("sm2PrivateKeyToBase64", abbreviate(privateBase64))
                            .set("loadSm2PublicKey", publicKey.getAlgorithm())
                            .set("loadSm2PrivateKey", privateKey.getAlgorithm())
                            .set("sm2Encrypt", abbreviate(cipher))
                            .set("sm2Decrypt", plain)
                            .set("sm2Sign", sign)
                            .set("sm2Verify", SecureUtil.sm2Verify(text, sign, publicKey))
                            .set("sm2SignStd", signStd)
                            .set("sm2VerifyStd", SecureUtil.sm2VerifyStd(text, signStd, publicKey));
                }))
                .set("sm3", safeValue("sm3", () -> Dict.create()
                        .set("sm3", SecureUtil.sm3(text))
                        .set("sm3Hex", SecureUtil.sm3Hex(text))))
                .set("sm4", safeValue("sm4", () -> {
                    byte[] key = AES_KEY_BYTES;
                    byte[] iv = AES_IV;
                    String sm4ByStringKey = SecureUtil.sm4Encrypt(text, AES_KEY, SecureUtil.SM4_CBC_PKCS5, iv);
                    String sm4ByBytesKey = SecureUtil.sm4Encrypt(text, key, iv);
                    byte[] sm4RandomKey = SecureUtil.generateSymmetricKey("SM4", 128);
                    SecureUtil.checkSymmetricKey(sm4RandomKey, "SM4");
                    return Dict.create()
                            .set("sm4EncryptStringKey", sm4ByStringKey)
                            .set("sm4DecryptStringKey", SecureUtil.sm4Decrypt(sm4ByStringKey, AES_KEY, SecureUtil.SM4_CBC_PKCS5, iv))
                            .set("sm4EncryptBytesKey", sm4ByBytesKey)
                            .set("sm4DecryptBytesKey", SecureUtil.sm4Decrypt(sm4ByBytesKey, key, iv))
                            .set("generateSymmetricKey_SM4", SecureUtil.toHex(sm4RandomKey))
                            .set("checkSymmetricKey_SM4", "已通过");
                }));
    }

    /**
     * 聚合运行基础示例，不包含文件和国密示例。
     *
     * @param text 原始文本
     * @return 聚合示例结果
     */
    @GetMapping("/all-basic")
    public Dict allBasic(@RequestParam(defaultValue = DEFAULT_TEXT) String text) {
        return Dict.create()
                .set("constants", constants())
                .set("codec", codec(text))
                .set("key", key())
                .set("digest", digest(text))
                .set("hmac", hmac(text))
                .set("symmetric", symmetric(text))
                .set("rsa", rsa(text))
                .set("dsa", dsa(text));
    }

    /**
     * 支持抛出受检异常的示例执行器。
     *
     * @param <T> 返回值类型
     */
    @FunctionalInterface
    private interface CheckedSupplier<T> {

        /**
         * 执行示例逻辑。
         *
         * @return 执行结果
         * @throws Exception 执行异常
         */
        T get() throws Exception;
    }

    /**
     * 安全执行示例代码，避免可选 Provider 或算法缺失导致接口整体失败。
     *
     * @param name     示例名称
     * @param supplier 执行逻辑
     * @return 执行结果或错误信息
     */
    private Object safeValue(String name, CheckedSupplier<?> supplier) {
        try {
            return supplier.get();
        } catch (Exception e) {
            log.warn("SecureUtil 示例执行失败，name={}", name, e);
            return Dict.create()
                    .set("success", false)
                    .set("name", name)
                    .set("message", e.getMessage())
                    .set("exception", e.getClass().getName());
        }
    }

    /**
     * 创建临时文本文件。
     *
     * @param text 文件内容
     * @return 文件路径
     */
    private Path createTempTextFile(String text) throws Exception {
        Path file = Files.createTempFile("secure-demo-", ".txt");
        Files.writeString(file, text, StandardCharsets.UTF_8);
        file.toFile().deleteOnExit();
        return file;
    }

    /**
     * 缩短长文本，避免接口返回过大。
     *
     * @param value 原始文本
     * @return 缩短后的文本
     */
    private String abbreviate(String value) {
        return StrUtil.maxLength(value, 96);
    }
}
