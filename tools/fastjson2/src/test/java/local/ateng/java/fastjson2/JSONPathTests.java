package local.ateng.java.fastjson2;

import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.JSONArray;
import com.alibaba.fastjson2.JSONPath;
import local.ateng.java.fastjson2.entity.UserInfoEntity;
import local.ateng.java.fastjson2.init.InitData;
import org.junit.jupiter.api.Test;
import org.springframework.util.ObjectUtils;

import java.math.BigDecimal;
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
        List<String> stringList = (List<String>) JSONPath.eval(JSON.toJSONString(list), "$[*].name");
        System.out.println(stringList);
    }

    // 读取集合多个元素的某个属性
    @Test
    void test01_2() {
        String json = "{\"students\":[{\"name\":\"John\"},{\"name\":\"Alice\"}]}";
        List<String> names = (List<String>) JSONPath.eval(json, "$.students[*].name");
        System.out.println(names);
    }
    @Test
    void test01_2_1() {
        String json = "{\"张三\":[{\"name\":\"John\"},{\"name\":\"Alice\"}]}";
        List<String> names = (List<String>) JSONPath.eval(json, "$.张三[*].name");
        System.out.println(names);
    }
    @Test
    void test01_3() {
        // 递归查询
        String json = "{\"students\":[{\"name\":\"John\"},{\"name\":\"Alice\"}]}";
        List<String> names = (List<String>) JSONPath.eval(json, "$.students..name");
        System.out.println(names);
    }
    @Test
    void test01_04() {
        String jsonString = "{\"students\":[{\"name\":\"John\",\"age\":25,\"gender\":\"Male\"},{\"name\":\"Alice\",\"age\":30,\"gender\":\"Female\"},{\"name\":\"Bob\",\"age\":28,\"gender\":\"Male\"}]}";
        // 使用 JSONPath.eval 获取多条件筛选的结果
        List<Object> result = (List<Object>) JSONPath.eval(jsonString, "$.students[?(@.age >= 25 && @.age <= 30 && @.gender == 'Male')].name");
        System.out.println(result);
    }
    @Test
    void test01_05() {
        String jsonString = "{\"students\":[{\"name\":\"John\",\"age\":25,\"gender\":\"Male\"},{\"name\":\"Alice\",\"age\":30,\"gender\":\"Female\"},{\"name\":\"Bob\",\"age\":28,\"gender\":\"Male\"}]}";
        // 使用 JSONPath.eval 获取1条件筛选的结果
        Object result = JSONPath.eval(jsonString, "$.students[?(@.age >= 25 && @.age <= 30 && @.gender == 'Male')][0]");
        System.out.println(result);
    }
    @Test
    void test01_06() {
        String jsonString = "[{\"name\":\"高新区管委会\",\"congestionIndex\":1.2,\"realSpeed\":\"60.918\",\"yoy\":0.0,\"mom\":0.0,\"status\":\"畅通\"},{\"name\":\"保税区-B区\",\"congestionIndex\":1.2,\"realSpeed\":\"39.3355\",\"yoy\":-0.1,\"mom\":0.0,\"status\":\"畅通\"},{\"name\":\"康居西城\",\"congestionIndex\":1.3,\"realSpeed\":\"29.8503\",\"yoy\":0.0,\"mom\":0.0,\"status\":\"畅通\"},{\"name\":\"白市驿\",\"congestionIndex\":1.1,\"realSpeed\":\"45.5646\",\"yoy\":-0.1,\"mom\":0.0,\"status\":\"畅通\"},{\"name\":\"金凤\",\"congestionIndex\":1.2,\"realSpeed\":\"44.2227\",\"yoy\":0.0,\"mom\":0.0,\"status\":\"畅通\"},{\"name\":\"保税区-A区\",\"congestionIndex\":1.4,\"realSpeed\":\"30.8192\",\"yoy\":0.0,\"mom\":0.0,\"status\":\"畅通\"},{\"name\":\"高新天街\",\"congestionIndex\":1.4,\"realSpeed\":\"19.2326\",\"yoy\":0.1,\"mom\":0.0,\"status\":\"畅通\"},{\"name\":\"熙街\",\"congestionIndex\":1.6,\"realSpeed\":\"23.2695\",\"yoy\":0.0,\"mom\":0.0,\"status\":\"缓行\"}]";
        // 使用 JSONPath.eval 获取多条件筛选的结果
        List<BigDecimal> congestionIndexList1 = (List<BigDecimal>) JSONPath.eval(jsonString, "$[?(@.name == '高新区管委会')].congestionIndex");
        double congestionIndex1 = ObjectUtils.isEmpty(congestionIndexList1) ? 1.0 : congestionIndexList1.get(0).doubleValue();
        System.out.println(congestionIndex1);

    }

    // 返回集合中多个元素
    @Test
    void test02() {
        Object objectList = JSONPath.eval(JSON.toJSONString(list), "[1,5]"); // 返回下标为1和5的元素
        List<UserInfoEntity> userList = JSONArray.parse(objectList.toString()).toJavaList(UserInfoEntity.class);
        System.out.println(userList);
    }

    // 按范围返回集合的子集
    @Test
    void test03() {
        Object objectList = JSONPath.eval(JSON.toJSONString(list), "[1:5]"); // 返回下标从1到5的元素
        List<UserInfoEntity> userList = JSONArray.parse(objectList.toString()).toJavaList(UserInfoEntity.class);
        System.out.println(userList);
    }

    // 通过条件过滤，返回集合的子集
    @Test
    void test04() {
        Object objectList = JSONPath.eval(JSON.toJSONString(list), "$[?(@.id in (88,99))]"); // 返回下标从1到5的元素
        List<UserInfoEntity> userList = JSONArray.parse(objectList.toString()).toJavaList(UserInfoEntity.class);
        System.out.println(userList);
    }

    // 通过条件过滤，返回集合的子集
    @Test
    void test04_2() {
        Object objectList = JSONPath.eval(JSON.toJSONString(list), "$[?(@.age = 88)]"); // 返回列表对象的age=88的数据
        List<UserInfoEntity> userList = JSONArray.parse(objectList.toString()).toJavaList(UserInfoEntity.class);
        System.out.println(userList);
    }

    // 多条件筛选，返回集合的子集
    @Test
    void test04_3() {
        Object objectList = JSONPath.eval(JSON.toJSONString(list), "$[?(@.age == 88 || @.age == 95)]"); // 返回列表对象的age=88或者=95的数据
        List<UserInfoEntity> userList = JSONArray.parse(objectList.toString()).toJavaList(UserInfoEntity.class);
        System.out.println(userList);
    }
    // 多条件筛选，返回集合的子集
    @Test
    void test04_4() {
        Object objectList = JSONPath.eval(JSON.toJSONString(list), "$[?(@.age > 50 && @.age < 95 && @.city='东莞')]");
        List<UserInfoEntity> userList = JSONArray.parse(objectList.toString()).toJavaList(UserInfoEntity.class);
        System.out.println(userList);
    }

    // 通过条件过滤，获取数组长度
    @Test
    void test04_5() {
        int length = (int) JSONPath.eval(JSON.toJSONString(list), "$[?(@.age = 88)].length()"); // 返回列表对象的age=88的数据的长度
        System.out.println(length);
    }

}
