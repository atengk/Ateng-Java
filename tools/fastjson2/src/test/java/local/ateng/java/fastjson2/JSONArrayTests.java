package local.ateng.java.fastjson2;

import com.alibaba.fastjson2.JSONArray;
import com.alibaba.fastjson2.JSONObject;
import local.ateng.java.fastjson2.entity.UserInfoEntity;
import local.ateng.java.fastjson2.init.InitData;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.stream.IntStream;

/**
 * 类的模板注释
 *
 * @author 孔余
 * @since 2024-01-18 16:53
 */
public class JSONArrayTests {
    List<UserInfoEntity> list;
    List<UserInfoEntity> list2;

    public JSONArrayTests() {
        InitData initData = new InitData();
        list = initData.getList();
        list2 = initData.getList2();
    }

    // 将字符串转换为JSONArray
    @Test
    void test01() {
        String str = "[{\"age\":87,\" \":\"1979-04-15 03:44:31.797\",\"city\":\"东莞\",\"id\":994,\"name\":\"贺熠彤\",\"province\":\"四川省\",\"score\":65.833},{\"age\":90,\"birthday\":\"1999-12-07 12:45:15.226\",\"city\":\"东莞\",\"id\":1023,\"name\":\"沈伟宸\",\"province\":\"山西省\",\"score\":57.183},{\"age\":57,\"birthday\":\"1987-05-08 04:42:58.07\",\"city\":\"东莞\",\"id\":1213,\"name\":\"戴弘文\",\"province\":\"四川省\",\"score\":53.744},{\"age\":87,\"birthday\":\"1982-12-20 23:35:45.202\",\"city\":\"东莞\",\"id\":1221,\"name\":\"钱振家\",\"province\":\"宁夏\",\"score\":67.445},{\"age\":68,\"birthday\":\"2000-03-17 11:49:20.09\",\"city\":\"东莞\",\"id\":1828,\"name\":\"顾智辉\",\"province\":\"湖南省\",\"score\":44.626},{\"age\":66,\"birthday\":\"1997-10-19 06:26:17.117\",\"city\":\"东莞\",\"id\":2035,\"name\":\"董擎宇\",\"province\":\"河南省\",\"score\":27.493},{\"age\":90,\"birthday\":\"1967-02-21 04:33:24.239\",\"city\":\"东莞\",\"id\":170,\"name\":\"谭思聪\",\"province\":\"澳门\",\"score\":27.498},{\"age\":92,\"birthday\":\"1988-09-02 06:51:55.229\",\"city\":\"东莞\",\"id\":822,\"name\":\"蒋聪健\",\"province\":\"宁夏\",\"score\":38.001},{\"age\":65,\"birthday\":\"2005-10-20 09:59:55.274\",\"city\":\"东莞\",\"id\":1816,\"name\":\"丁天磊\",\"province\":\"宁夏\",\"score\":47.401},{\"age\":58,\"birthday\":\"1990-08-08 13:11:58.805\",\"city\":\"东莞\",\"id\":1845,\"name\":\"钟志泽\",\"province\":\"黑龙江省\",\"score\":16.65},{\"age\":75,\"birthday\":\"1996-09-04 15:02:12.485\",\"city\":\"东莞\",\"id\":2283,\"name\":\"韦鹏煊\",\"province\":\"上海市\",\"score\":75.307},{\"age\":66,\"birthday\":\"1978-01-25 22:22:42.441\",\"city\":\"东莞\",\"id\":2563,\"name\":\"曹明杰\",\"province\":\"广东省\",\"score\":89.582}]";
        JSONArray jsonArray = JSONArray.parse(str);
        System.out.println(jsonArray);
    }

    // 将字符串转换为java对象列表
    @Test
    void test02() {
        String str = "[{\"age\":87,\"birthday\":\"1979-04-15 03:44:31.797\",\"city\":\"东莞\",\"id\":994,\"name\":\"贺熠彤\",\"province\":\"四川省\",\"score\":65.833},{\"age\":90,\"birthday\":\"1999-12-07 12:45:15.226\",\"city\":\"东莞\",\"id\":1023,\"name\":\"沈伟宸\",\"province\":\"山西省\",\"score\":57.183},{\"age\":57,\"birthday\":\"1987-05-08 04:42:58.07\",\"city\":\"东莞\",\"id\":1213,\"name\":\"戴弘文\",\"province\":\"四川省\",\"score\":53.744},{\"age\":87,\"birthday\":\"1982-12-20 23:35:45.202\",\"city\":\"东莞\",\"id\":1221,\"name\":\"钱振家\",\"province\":\"宁夏\",\"score\":67.445},{\"age\":68,\"birthday\":\"2000-03-17 11:49:20.09\",\"city\":\"东莞\",\"id\":1828,\"name\":\"顾智辉\",\"province\":\"湖南省\",\"score\":44.626},{\"age\":66,\"birthday\":\"1997-10-19 06:26:17.117\",\"city\":\"东莞\",\"id\":2035,\"name\":\"董擎宇\",\"province\":\"河南省\",\"score\":27.493},{\"age\":90,\"birthday\":\"1967-02-21 04:33:24.239\",\"city\":\"东莞\",\"id\":170,\"name\":\"谭思聪\",\"province\":\"澳门\",\"score\":27.498},{\"age\":92,\"birthday\":\"1988-09-02 06:51:55.229\",\"city\":\"东莞\",\"id\":822,\"name\":\"蒋聪健\",\"province\":\"宁夏\",\"score\":38.001},{\"age\":65,\"birthday\":\"2005-10-20 09:59:55.274\",\"city\":\"东莞\",\"id\":1816,\"name\":\"丁天磊\",\"province\":\"宁夏\",\"score\":47.401},{\"age\":58,\"birthday\":\"1990-08-08 13:11:58.805\",\"city\":\"东莞\",\"id\":1845,\"name\":\"钟志泽\",\"province\":\"黑龙江省\",\"score\":16.65},{\"age\":75,\"birthday\":\"1996-09-04 15:02:12.485\",\"city\":\"东莞\",\"id\":2283,\"name\":\"韦鹏煊\",\"province\":\"上海市\",\"score\":75.307},{\"age\":66,\"birthday\":\"1978-01-25 22:22:42.441\",\"city\":\"东莞\",\"id\":2563,\"name\":\"曹明杰\",\"province\":\"广东省\",\"score\":89.582}]";
        List<UserInfoEntity> userList = JSONArray.parseArray(str, UserInfoEntity.class);
        System.out.println(userList);
    }

    // 将java对象列表转换为字符串
    @Test
    void test03() {
        String str = JSONArray.toJSONString(list);
        System.out.println(str);
    }

    // 循环 增强 for 循环
    @Test
    void test04() {
        String str = "[{\"age\":87,\"birthday\":\"1979-04-15 03:44:31.797\",\"city\":\"东莞\",\"id\":994,\"name\":\"贺熠彤\",\"province\":\"四川省\",\"score\":65.833},{\"age\":90,\"birthday\":\"1999-12-07 12:45:15.226\",\"city\":\"东莞\",\"id\":1023,\"name\":\"沈伟宸\",\"province\":\"山西省\",\"score\":57.183},{\"age\":57,\"birthday\":\"1987-05-08 04:42:58.07\",\"city\":\"东莞\",\"id\":1213,\"name\":\"戴弘文\",\"province\":\"四川省\",\"score\":53.744},{\"age\":87,\"birthday\":\"1982-12-20 23:35:45.202\",\"city\":\"东莞\",\"id\":1221,\"name\":\"钱振家\",\"province\":\"宁夏\",\"score\":67.445},{\"age\":68,\"birthday\":\"2000-03-17 11:49:20.09\",\"city\":\"东莞\",\"id\":1828,\"name\":\"顾智辉\",\"province\":\"湖南省\",\"score\":44.626},{\"age\":66,\"birthday\":\"1997-10-19 06:26:17.117\",\"city\":\"东莞\",\"id\":2035,\"name\":\"董擎宇\",\"province\":\"河南省\",\"score\":27.493},{\"age\":90,\"birthday\":\"1967-02-21 04:33:24.239\",\"city\":\"东莞\",\"id\":170,\"name\":\"谭思聪\",\"province\":\"澳门\",\"score\":27.498},{\"age\":92,\"birthday\":\"1988-09-02 06:51:55.229\",\"city\":\"东莞\",\"id\":822,\"name\":\"蒋聪健\",\"province\":\"宁夏\",\"score\":38.001},{\"age\":65,\"birthday\":\"2005-10-20 09:59:55.274\",\"city\":\"东莞\",\"id\":1816,\"name\":\"丁天磊\",\"province\":\"宁夏\",\"score\":47.401},{\"age\":58,\"birthday\":\"1990-08-08 13:11:58.805\",\"city\":\"东莞\",\"id\":1845,\"name\":\"钟志泽\",\"province\":\"黑龙江省\",\"score\":16.65},{\"age\":75,\"birthday\":\"1996-09-04 15:02:12.485\",\"city\":\"东莞\",\"id\":2283,\"name\":\"韦鹏煊\",\"province\":\"上海市\",\"score\":75.307},{\"age\":66,\"birthday\":\"1978-01-25 22:22:42.441\",\"city\":\"东莞\",\"id\":2563,\"name\":\"曹明杰\",\"province\":\"广东省\",\"score\":89.582}]";
        JSONArray jsonArray = JSONArray.parse(str);
        for (Object obj : jsonArray) {
            JSONObject jsonObject = (JSONObject) obj;
            System.out.println("Name: " + jsonObject.getString("name") + ", Age: " + jsonObject.getIntValue("age"));
        }
    }

    // 循环 Java 8 的 Stream API
    @Test
    void test05() {
        String str = "[{\"age\":87,\"birthday\":\"1979-04-15 03:44:31.797\",\"city\":\"东莞\",\"id\":994,\"name\":\"贺熠彤\",\"province\":\"四川省\",\"score\":65.833},{\"age\":90,\"birthday\":\"1999-12-07 12:45:15.226\",\"city\":\"东莞\",\"id\":1023,\"name\":\"沈伟宸\",\"province\":\"山西省\",\"score\":57.183},{\"age\":57,\"birthday\":\"1987-05-08 04:42:58.07\",\"city\":\"东莞\",\"id\":1213,\"name\":\"戴弘文\",\"province\":\"四川省\",\"score\":53.744},{\"age\":87,\"birthday\":\"1982-12-20 23:35:45.202\",\"city\":\"东莞\",\"id\":1221,\"name\":\"钱振家\",\"province\":\"宁夏\",\"score\":67.445},{\"age\":68,\"birthday\":\"2000-03-17 11:49:20.09\",\"city\":\"东莞\",\"id\":1828,\"name\":\"顾智辉\",\"province\":\"湖南省\",\"score\":44.626},{\"age\":66,\"birthday\":\"1997-10-19 06:26:17.117\",\"city\":\"东莞\",\"id\":2035,\"name\":\"董擎宇\",\"province\":\"河南省\",\"score\":27.493},{\"age\":90,\"birthday\":\"1967-02-21 04:33:24.239\",\"city\":\"东莞\",\"id\":170,\"name\":\"谭思聪\",\"province\":\"澳门\",\"score\":27.498},{\"age\":92,\"birthday\":\"1988-09-02 06:51:55.229\",\"city\":\"东莞\",\"id\":822,\"name\":\"蒋聪健\",\"province\":\"宁夏\",\"score\":38.001},{\"age\":65,\"birthday\":\"2005-10-20 09:59:55.274\",\"city\":\"东莞\",\"id\":1816,\"name\":\"丁天磊\",\"province\":\"宁夏\",\"score\":47.401},{\"age\":58,\"birthday\":\"1990-08-08 13:11:58.805\",\"city\":\"东莞\",\"id\":1845,\"name\":\"钟志泽\",\"province\":\"黑龙江省\",\"score\":16.65},{\"age\":75,\"birthday\":\"1996-09-04 15:02:12.485\",\"city\":\"东莞\",\"id\":2283,\"name\":\"韦鹏煊\",\"province\":\"上海市\",\"score\":75.307},{\"age\":66,\"birthday\":\"1978-01-25 22:22:42.441\",\"city\":\"东莞\",\"id\":2563,\"name\":\"曹明杰\",\"province\":\"广东省\",\"score\":89.582}]";
        JSONArray jsonArray = JSONArray.parse(str);
        IntStream.range(0, jsonArray.size())
                .mapToObj(jsonArray::getJSONObject)
                .forEach(jsonObject ->
                        System.out.println("Name: " + jsonObject.getString("name") + ", Age: " + jsonObject.getIntValue("age"))
                );
    }
}
