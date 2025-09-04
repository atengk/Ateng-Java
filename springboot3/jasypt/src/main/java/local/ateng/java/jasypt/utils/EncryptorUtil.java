package local.ateng.java.jasypt.utils;

import org.jasypt.encryption.pbe.PooledPBEStringEncryptor;
import org.jasypt.encryption.pbe.config.SimpleStringPBEConfig;

/**
 * Jasypt 加密/解密工具类
 * 用于快速生成 ENC(...) 格式的密文，或解密校验。
 * <p>
 * 注意：生产环境不要把加密密钥写死在代码里，建议通过 JVM 参数或环境变量传入。
 *
 * @author 孔余
 * @since 2025-09-03
 */
public class EncryptorUtil {

    /**
     * 默认加密算法，可根据需要调整
     * PBEWITHHMACSHA512ANDAES_128 在所有 JDK 上都能跑
     * PBEWITHHMACSHA512ANDAES_256 在 JDK8u162+ / JDK21 默认支持
     */
    private static final String ALGORITHM = "PBEWITHHMACSHA512ANDAES_256";

    private static PooledPBEStringEncryptor getEncryptor(String password) {
        SimpleStringPBEConfig config = new SimpleStringPBEConfig();
        config.setPassword(password);
        config.setAlgorithm(ALGORITHM);
        config.setKeyObtentionIterations("1000"); // 派生迭代次数
        config.setPoolSize("1");
        config.setProviderName("SunJCE");
        config.setSaltGeneratorClassName("org.jasypt.salt.RandomSaltGenerator");
        config.setIvGeneratorClassName("org.jasypt.iv.RandomIvGenerator");
        config.setStringOutputType("base64");

        PooledPBEStringEncryptor encryptor = new PooledPBEStringEncryptor();
        encryptor.setConfig(config);
        return encryptor;
    }

    /**
     * 加密字符串
     *
     * @param plainText 明文
     * @param password  加密密钥
     * @return 加密后的密文，格式 ENC(xxx)
     */
    public static String encrypt(String plainText, String password) {
        PooledPBEStringEncryptor encryptor = getEncryptor(password);
        return "ENC(" + encryptor.encrypt(plainText) + ")";
    }

    /**
     * 解密字符串
     *
     * @param encryptedText 加密后的字符串（可以带 ENC(...)，也可以直接是密文）
     * @param password      加密密钥
     * @return 解密后的明文
     */
    public static String decrypt(String encryptedText, String password) {
        PooledPBEStringEncryptor encryptor = getEncryptor(password);

        String text = encryptedText;
        if (encryptedText.startsWith("ENC(") && encryptedText.endsWith(")")) {
            text = encryptedText.substring(4, encryptedText.length() - 1);
        }
        return encryptor.decrypt(text);
    }
}
