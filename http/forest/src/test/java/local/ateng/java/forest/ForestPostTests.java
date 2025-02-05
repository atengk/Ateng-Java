package local.ateng.java.forest;

import com.alibaba.fastjson2.JSONObject;
import com.dtflys.forest.Forest;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

/**
 * 编程式接口: POST
 * 🚋 请求体: https://forest.dtflyx.com/pages/1.5.36/api_execute/
 *
 * @author 孔余
 * @since 2024-05-16 17:33:26
 */
@SpringBootTest
class ForestPostTests {

    @Test
    void getAsString() {
        String str = Forest
                .post("https://api.apiopen.top/api/login")
                .contentTypeJson()        // 指定请求体为JSON格式
                .addBody("account", "309324904@qq.com")
                .addBody("password", "123456")
                .executeAsString();
        System.out.println(str);
    }

    @Test
    void getAsString2() {
        JSONObject body = JSONObject.of(
                "account", "309324904@qq.com",
                "password", "123456"
        );
        String str = Forest
                .post("https://api.apiopen.top/api/login")
                .contentTypeJson()        // 指定请求体为JSON格式
                .addBody(body)
                .executeAsString();
        System.out.println(str);
    }

}
