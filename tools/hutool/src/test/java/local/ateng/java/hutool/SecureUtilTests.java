package local.ateng.java.hutool;

import cn.hutool.core.util.StrUtil;
import cn.hutool.crypto.SecureUtil;
import org.junit.jupiter.api.Test;

/**
 * 加密解密工具-SecureUtil
 * https://www.hutool.cn/docs/#/crypto/%E5%8A%A0%E5%AF%86%E8%A7%A3%E5%AF%86%E5%B7%A5%E5%85%B7-SecureUtil
 *
 * @author 孔余
 * @email 2385569970@qq.com
 * @date 2024-07-25 10:24:44
 */
public class SecureUtilTests {


    /**
     * md5加密
     * 通用加密流程:(第一次登陆返回没有method字段)
     * passwd-value0 = encryptType-value(passwd-value) 其中passwd-value是密码明文。
     * passwd-value1 = encryptType-value(username-value:passwd-value0) 中间无:号
     * passwd-tmp = encryptType-value(passwd-value1)
     * encrypted-passwd = encryptType-value(username-value:realm-value:passwd-tmp) 中间含:号
     * signature = encryptType-value(encrypted-passwd:randomKey-value) 中间含:号
     */
    @Test
    public void test() {
        String username = "duijie";
        String passwd = "JZWSD@2024";
        String realm = "160F090CD90D1346";
        String randomKey = "10783704070883392";
        String passwdValue0 = SecureUtil.md5(passwd);
        String passwdValue1 = SecureUtil.md5(StrUtil.format("{}{}", username, passwdValue0));
        String passwdValue2 = SecureUtil.md5(passwdValue1);
        String encryptedPasswd = SecureUtil.md5(StrUtil.format("{}:{}:{}", username, realm, passwdValue2));
        String signature = SecureUtil.md5(StrUtil.format("{}:{}", encryptedPasswd, randomKey));
        System.out.println("signature=" + signature);
        // 266f9473da4167e87751a193a1f59f51
    }

    @Test
    public void test2() {
        /**
         * String signature = encrypt(password, "MD5");
         * 		signature = encrypt(this.userName+signature, "MD5");
         * 		signature = encrypt(signature, "MD5");
         * 		signature = encrypt(this.userName+":"+realm+":"+signature, "MD5");
         * 		signature = encrypt(signature+":"+this.randomKey, "MD5");
         * 		return signature;
         */
        String username = "duijie";
        String passwd = "JZWSD@2024";
        String realm = "160F090CD90D1346";
        String randomKey = "10698618046056512";
        String passwdValue0 = SecureUtil.md5(passwd);
        String passwdValue1 = SecureUtil.md5(StrUtil.format("{}{}", username, passwdValue0));
        String passwdValue2 = SecureUtil.md5(passwdValue1);
        String encryptedPasswd = SecureUtil.md5(StrUtil.format("{}:{}:{}", username, realm, passwdValue2));
        String signature = SecureUtil.md5(StrUtil.format("{}:{}", encryptedPasswd, randomKey));
        System.out.println("signature=" + signature);
    }
}
