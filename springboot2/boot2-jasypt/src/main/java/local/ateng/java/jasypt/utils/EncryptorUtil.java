package local.ateng.java.jasypt.utils;

import org.jasypt.encryption.pbe.StandardPBEStringEncryptor;

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
     */
    private static final String ALGORITHM = "PBEWITHMD5ANDDES";

    /**
     * 加密字符串
     *
     * @param plainText 明文
     * @param password  加密密钥
     * @return 加密后的密文，格式 ENC(xxx)
     */
    public static String encrypt(String plainText, String password) {
        StandardPBEStringEncryptor encryptor = new StandardPBEStringEncryptor();
        encryptor.setAlgorithm(ALGORITHM);
        encryptor.setPassword(password);
        String encrypted = encryptor.encrypt(plainText);
        return "ENC(" + encrypted + ")";
    }

    /**
     * 解密字符串
     *
     * @param encryptedText 加密后的字符串（可以带 ENC(...)，也可以直接是密文）
     * @param password      加密密钥
     * @return 解密后的明文
     */
    public static String decrypt(String encryptedText, String password) {
        StandardPBEStringEncryptor encryptor = new StandardPBEStringEncryptor();
        encryptor.setAlgorithm(ALGORITHM);
        encryptor.setPassword(password);

        // 去掉 ENC(...) 包裹
        String text = encryptedText;
        if (encryptedText.startsWith("ENC(") && encryptedText.endsWith(")")) {
            text = encryptedText.substring(4, encryptedText.length() - 1);
        }

        return encryptor.decrypt(text);
    }

}

