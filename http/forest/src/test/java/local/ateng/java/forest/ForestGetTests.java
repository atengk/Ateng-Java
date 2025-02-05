package local.ateng.java.forest;

import com.alibaba.fastjson2.JSONObject;
import com.dtflys.forest.Forest;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

/**
 * 编程式接口: GET
 * ✨ 执行请求: https://forest.dtflyx.com/pages/1.5.36/api_execute/
 * 🧀 URL 参数: https://forest.dtflyx.com/pages/1.5.36/api_request_query/#%E6%B7%BB%E5%8A%A0%E5%8F%82%E6%95%B0
 *
 * @author 孔余
 * @email 2385569970@qq.com
 * @since 2025-02-05
 */
@SpringBootTest
class ForestGetTests {

    @Test
    void getAsString() {
        // 直接以 String 类型接受数据
        String str = Forest.get("https://www.wanandroid.com/article/list/0/json").executeAsString();
        System.out.println(str);
    }

    @Test
    void getHttpsAsString() {
        // 不安全的https
        String str = Forest.get("https://192.168.1.12:11443/").executeAsString();
        System.out.println(str);
    }

    @Test
    void getAsJson() {
        // 自定义类型
        JSONObject json = Forest.get("https://www.wanandroid.com/article/list/0/json").execute(JSONObject.class);
        System.out.println(json);
    }

}
