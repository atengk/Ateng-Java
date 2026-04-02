package io.github.atengk.license;

import cn.hutool.crypto.SecureUtil;
import cn.hutool.crypto.asymmetric.RSA;
import org.junit.jupiter.api.Test;

/**
 * RSA 密钥生成
 */
public class RsaKeyGeneratorTests {

    @Test
    public void testGenerate() {
        RSA rsa = SecureUtil.rsa();

        String publicKey = rsa.getPublicKeyBase64();
        String privateKey = rsa.getPrivateKeyBase64();

        System.out.println("公钥：");
        System.out.println(publicKey);

        System.out.println("私钥：");
        System.out.println(privateKey);
    }

}
