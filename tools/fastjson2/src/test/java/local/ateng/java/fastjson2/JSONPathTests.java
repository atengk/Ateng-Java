package local.ateng.java.fastjson2;

import com.alibaba.fastjson2.JSONArray;
import com.alibaba.fastjson2.JSONObject;
import com.alibaba.fastjson2.JSONPath;
import local.ateng.java.fastjson2.entity.UserInfoEntity;
import local.ateng.java.fastjson2.init.InitData;
import org.junit.jupiter.api.Test;

import java.util.List;

/**
 * 在FASTJSON2中，JSONPath是一等公民，支持通过JSONPath在不完整解析JSON Document的情况下，
 * 根据JSONPath读取内容；也支持用JSONPath对JavaBean求值，可以在Java框架中当做对象查询语言（OQL）来使用。
 * https://github.com/alibaba/fastjson2/blob/main/docs/jsonpath_cn.md
 *
 * @author 孔余
 * @since 2024-01-18 15:44
 */
public class JSONPathTests {
    List<UserInfoEntity> list;
    List<UserInfoEntity> list2;

    public JSONPathTests() {
        InitData initData = new InitData();
        list = initData.getList();
        list2 = initData.getList2();
    }


    // 读取集合多个元素的某个属性
    @Test
    void test01() {
        JSONArray stringList = (JSONArray) JSONPath.eval(list, "$[*].name");
        System.out.println(stringList);
        // ["段哲瀚","汪昊然","万修杰", ... ]
    }

    // 读取集合多个元素的某个属性
    @Test
    void test01_2() {
        String json = "{\"students\":[{\"name\":\"John\"},{\"name\":\"Alice\"}]}";
        JSONArray names = (JSONArray) JSONPath.eval(json, "$.students[*].name");
        System.out.println(names);
        // ["John","Alice"]
    }
    @Test
    void test01_2_1() {
        String json = "{\"张三\":[{\"name\":\"John\"},{\"name\":\"Alice\"}]}";
        JSONArray names = (JSONArray) JSONPath.eval(json, "$.张三[*].name");
        System.out.println(names);
        // ["John","Alice"]
    }
    @Test
    void test01_3() {
        // 递归查询
        String json = "{\"students\":[{\"name\":\"John\"},{\"name\":\"Alice\"}]}";
        JSONArray names = (JSONArray) JSONPath.eval(json, "$.students..name");
        System.out.println(names);
        // ["John","Alice"]
    }
    @Test
    void test01_04() {
        String jsonString = "{\"students\":[{\"name\":\"John\",\"age\":25,\"gender\":\"Male\"},{\"name\":\"Alice\",\"age\":30,\"gender\":\"Female\"},{\"name\":\"Bob\",\"age\":28,\"gender\":\"Male\"}]}";
        // 使用 JSONPath.eval 获取多条件筛选的结果
        JSONArray result = (JSONArray) JSONPath.eval(jsonString, "$.students[?(@.age >= 25 && @.age <= 30 && @.gender == 'Male')].name");
        System.out.println(result);
        // ["John","Bob"]
    }
    @Test
    void test01_05() {
        String jsonString = "{\"students\":[{\"name\":\"John\",\"age\":25,\"gender\":\"Male\"},{\"name\":\"Alice\",\"age\":30,\"gender\":\"Female\"},{\"name\":\"Bob\",\"age\":28,\"gender\":\"Male\"}]}";
        // 使用 JSONPath.eval 获取1条件筛选的结果
        JSONObject result = (JSONObject) JSONPath.eval(jsonString, "$.students[?(@.age >= 25 && @.age <= 30 && @.gender == 'Male')][0]");
        System.out.println(result);
        // {"name":"John","age":25,"gender":"Male"}
    }
    @Test
    void test01_06() {
        String jsonString = "[{\"name\":\"高新区管委会\",\"congestionIndex\":1.2,\"realSpeed\":\"60.918\",\"yoy\":0.0,\"mom\":0.0,\"status\":\"畅通\"},{\"name\":\"保税区-B区\",\"congestionIndex\":1.2,\"realSpeed\":\"39.3355\",\"yoy\":-0.1,\"mom\":0.0,\"status\":\"畅通\"},{\"name\":\"康居西城\",\"congestionIndex\":1.3,\"realSpeed\":\"29.8503\",\"yoy\":0.0,\"mom\":0.0,\"status\":\"畅通\"},{\"name\":\"白市驿\",\"congestionIndex\":1.1,\"realSpeed\":\"45.5646\",\"yoy\":-0.1,\"mom\":0.0,\"status\":\"畅通\"},{\"name\":\"金凤\",\"congestionIndex\":1.2,\"realSpeed\":\"44.2227\",\"yoy\":0.0,\"mom\":0.0,\"status\":\"畅通\"},{\"name\":\"保税区-A区\",\"congestionIndex\":1.4,\"realSpeed\":\"30.8192\",\"yoy\":0.0,\"mom\":0.0,\"status\":\"畅通\"},{\"name\":\"高新天街\",\"congestionIndex\":1.4,\"realSpeed\":\"19.2326\",\"yoy\":0.1,\"mom\":0.0,\"status\":\"畅通\"},{\"name\":\"熙街\",\"congestionIndex\":1.6,\"realSpeed\":\"23.2695\",\"yoy\":0.0,\"mom\":0.0,\"status\":\"缓行\"}]";
        // 使用 JSONPath.eval 获取多条件筛选的结果
        JSONArray congestionIndexList1 = (JSONArray) JSONPath.eval(jsonString, "$[?(@.name == '高新区管委会')].congestionIndex");
        System.out.println(congestionIndexList1);
        // [1.2]
        System.out.println(congestionIndexList1.getFirst().getClass());
        // class java.math.BigDecimal
    }

    // 返回集合中多个元素
    @Test
    void test02() {
        JSONArray objectList = (JSONArray) JSONPath.eval(list, "[1,5]"); // 返回下标为1和5的元素
        List<UserInfoEntity> userList = objectList.toJavaList(UserInfoEntity.class);
        System.out.println(userList);
        // [UserInfoEntity(id=2, name=宋鹏, age=86, score=48.951, num=null, bi ...
    }

    // 按范围返回集合的子集
    @Test
    void test03() {
        JSONArray objectList = (JSONArray) JSONPath.eval(list, "[1:5]"); // 返回下标从1到5的元素
        List<UserInfoEntity> userList = objectList.toJavaList(UserInfoEntity.class);
        System.out.println(userList);
        // [UserInfoEntity(id=2, name=林浩, age=95, score=69.378, num=null, ...
    }

    // 通过条件过滤，返回集合的子集
    @Test
    void test04() {
        JSONArray objectList = (JSONArray) JSONPath.eval(list,"$[?(@.id in (88,99))]"); // 返回下标从1到5的元素
        List<UserInfoEntity> userList = objectList.toJavaList(UserInfoEntity.class);
        System.out.println(userList);
        // [UserInfoEntity(id=88, name=方越彬, age=63, score=63.994, num=null,
    }
    @Test
    void test04_2() {
        JSONArray objectList = (JSONArray) JSONPath.eval(list, "$[?(@.age = 88)]"); // 返回列表对象的age=88的数据
        List<UserInfoEntity> userList = objectList.toJavaList(UserInfoEntity.class);
        System.out.println(userList);
        // [UserInfoEntity(id=28, name=邵浩然, age=88, score=24.06, num=null, birthday=Tue Jun ...
    }

    // 多条件筛选，返回集合的子集
    @Test
    void test04_3() {
        JSONArray objectList = (JSONArray) JSONPath.eval(list, "$[?(@.age == 88 || @.age == 95)]"); // 返回列表对象的age=88或者=95的数据
        List<UserInfoEntity> userList = objectList.toJavaList(UserInfoEntity.class);
        System.out.println(userList);
        // [UserInfoEntity(id=11, name=朱烨磊, age=88, score=66.78, ...
    }
    @Test
    void test04_4() {
        JSONArray objectList = (JSONArray) JSONPath.eval(list, "$[?(@.age > 50 && @.age < 95 && @.city='东莞')]");
        List<UserInfoEntity> userList = objectList.toJavaList(UserInfoEntity.class);
        System.out.println(userList);
        // [UserInfoEntity(id=64, name=崔志泽, age=70, score=64.082, num=null ...
    }

    // 通过条件过滤，获取数组长度
    @Test
    void test04_5() {
        Integer length = (Integer) JSONPath.eval(list, "$[?(@.age = 88)].length()"); // 返回列表对象的age=88的数据的长度
        System.out.println(length);
    }

}
