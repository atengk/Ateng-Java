package io.github.atengk.crypto;

import io.github.atengk.crypto.util.CryptoGenerateUtil;
import org.junit.jupiter.api.Test;

/**
 * Key 生成
 */
public class KeyGeneratorTests {

    @Test
    public void AES_KEY() {
        String key = CryptoGenerateUtil.generateAesKey();
        System.out.println(key);
        // QD2RQPTG8ujbImZVwYeVeQ==
    }

    @Test
    public void SIGN_KEY() {
        String signKey = CryptoGenerateUtil.generateSignKey();
        System.out.println(signKey);
        // 676182be2b2adc09ab80a989f305222c454b7002dfb0b4fa3ef97c230ff94f2c
    }

    @Test
    public void IV() {
        String iv = CryptoGenerateUtil.generateIv();
        System.out.println(iv);
        // paY8aTRpCzpppJ5hwb64pw==
    }

}
