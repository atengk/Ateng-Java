package local.ateng.java.hutool;

import cn.hutool.core.codec.Morse;
import cn.hutool.core.date.DateTime;
import cn.hutool.core.date.DateUtil;
import cn.hutool.core.io.FileUtil;
import cn.hutool.jwt.JWT;
import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;

/**
 * JWT就是一种网络身份认证和信息交换格式
 * https://www.hutool.cn/docs/#/jwt/%E6%A6%82%E8%BF%B0
 *
 * @author 孔余
 * @email 2385569970@qq.com
 * @date 2024-07-10 15:35:40
 */
public class JWTUtilTests {


    @Test
    public void test() {
        // JWT生成
        DateTime dateTime = DateUtil.date();
        final String token = JWT.create()
                .setNotBefore(dateTime)
                .setExpiresAt(DateUtil.offsetDay(dateTime, 30))
                .setPayload("company", "Lingo")
                .setPayload("name", "孔余")
                .setKey("Admin@123".getBytes())
                .sign();
        System.out.println(token);

        // JWT解析
        JWT jwt = JWT.of(token);
        System.out.println(jwt.getHeaders());
        System.out.println(jwt.getPayloads());

        // JWT校验
        byte[] key = "Admin@123".getBytes();
        boolean validate = JWT.of(token).setKey(key).validate(0);
        System.out.println(validate);

        // 摩斯密码
        String morseToken = myMorse(token,true);

        // 写入文件
        FileUtil.writeString(morseToken, "D:\\Temp\\202407\\test.jwt", StandardCharsets.UTF_8);

    }

    private String myMorse(String text, Boolean isEncode) {
        final Morse morseCoder = new Morse();
        if (isEncode) {
            return morseCoder.encode(text);
        } else {
            return morseCoder.decode(text);
        }
    }

    @Test
    public void test2() {
        String morseToken = myMorse("eyJ0eXAiOiJKV1QiLCJhbGciOiJIUzI1NiJ9.eyJuYmYiOjE3MjA2MDI0NjQsImV4cCI6MTcyMzE5NDQ2NCwiY29tcGFueSI6IkxpbmdvIiwibmFtZSI6IuWtlOS9mSJ9.CnHkPk-eo58a2ORENWr61-mjRp7OqONk4l0JpDBkmmY", true);
        System.out.println(morseToken);

        String token = myMorse(morseToken, false);
        System.out.println(token);
    }

}
