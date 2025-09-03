package local.ateng.java.jasypt;

import local.ateng.java.jasypt.utils.EncryptorUtil;
import org.junit.jupiter.api.Test;

public class Tests {

    @Test
     void test() {
        String password = "Admin@123"; // 加密密钥（建议用 JVM 参数传入）
        String plain = "root123";           // 待加密的明文

        // 加密
        String enc = EncryptorUtil.encrypt(plain, password);
        System.out.println("加密后: " + enc);

        // 解密
        String dec = EncryptorUtil.decrypt(enc, password);
        System.out.println("解密后: " + dec);
    }

}
