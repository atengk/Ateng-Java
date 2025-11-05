package local.ateng.java.fastjson1;

import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.alibaba.fastjson.JSONPath;
import local.ateng.java.fastjson.entity.UserInfoEntity;
import local.ateng.java.fastjson.init.InitData;
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
        // 如果是数组结构 []，不要转换成字符串，不然会解析失败
        JSONArray stringList = (JSONArray) JSONPath.eval(list, "$[*].name");
        System.out.println(stringList.getClass());
        // 注意这里实际的类型 class com.alibaba.fastjson.JSONArray
        System.out.println(stringList);
        // ["武笑愚","尹黎昕", ... ]
    }

    // 读取集合多个元素的某个属性
    @Test
    void test01_2() {
        // 如果是对象结构 {}，可以直接传入对象字符串解析，当然也可以直接传对象
        String json = "{\"students\":[{\"name\":\"John\"},{\"name\":\"Alice\"}]}";
        JSONArray names = (JSONArray) JSONPath.eval(json, "$.students[*].name");
        System.out.println(names.getClass());
        // 注意这里实际的类型 class com.alibaba.fastjson.JSONArray
        System.out.println(names);
        // ["John","Alice"]
    }
    @Test
    void test01_2_1() {
        String json = "{\"张三\":[{\"name\":\"John\"},{\"name\":\"Alice\"}]}";
        JSONArray names = (JSONArray) JSONPath.eval(json, "$.张三[*].name");
        System.out.println(names.getClass());
        // 注意这里实际的类型 class com.alibaba.fastjson.JSONArray
        System.out.println(names);
        // ["John","Alice"]
    }
    @Test
    void test01_3() {
        // 递归查询
        String json = "{\"students\":[{\"name\":\"John\"},{\"name\":\"Alice\"}]}";
        List<String> names = (List<String>) JSONPath.eval(json, "$.students..name");
        System.out.println(names.getClass());
        // 注意这里实际的类型 class java.util.ArrayList
        System.out.println(names);
        // [John, Alice]
    }
    @Test
    void test01_04() {
        String jsonString = "{\"students\":[{\"name\":\"John\",\"age\":25,\"gender\":\"Male\"},{\"name\":\"Alice\",\"age\":30,\"gender\":\"Female\"},{\"name\":\"Bob\",\"age\":28,\"gender\":\"Male\"}]}";
        // 使用 JSONPath.eval 获取多条件筛选的结果
        JSONArray result = (JSONArray) JSONPath.eval(jsonString, "$.students[?(@.age >= 25 && @.age <= 30 && @.gender == 'Male')].name");
        System.out.println(result.getClass());
        // 注意这里实际的类型 class com.alibaba.fastjson.JSONArray
        System.out.println(result);
        // [John, Alice]
    }
    @Test
    void test01_05() {
        String jsonString = "{\"students\":[{\"name\":\"John\",\"age\":25,\"gender\":\"Male\"},{\"name\":\"Alice\",\"age\":30,\"gender\":\"Female\"},{\"name\":\"Bob\",\"age\":28,\"gender\":\"Male\"}]}";
        // 使用 JSONPath.eval 获取1条件筛选的结果
        JSONObject result = (JSONObject) JSONPath.eval(jsonString, "$.students[?(@.age >= 25 && @.age <= 30 && @.gender == 'Male')][0]");
        System.out.println(result.getClass());
        // 注意这里实际的类型 class com.alibaba.fastjson.JSONObject
        System.out.println(result);
        // {"gender":"Male","name":"John","age":25}
    }
    @Test
    void test01_06() {
        // 如果是数组结构 []，数组字符串要转换成对象数组，不然会解析失败
        String jsonString = "[{\"name\":\"高新区管委会\",\"congestionIndex\":1.2,\"realSpeed\":\"60.918\",\"yoy\":0.0,\"mom\":0.0,\"status\":\"畅通\"},{\"name\":\"保税区-B区\",\"congestionIndex\":1.2,\"realSpeed\":\"39.3355\",\"yoy\":-0.1,\"mom\":0.0,\"status\":\"畅通\"},{\"name\":\"康居西城\",\"congestionIndex\":1.3,\"realSpeed\":\"29.8503\",\"yoy\":0.0,\"mom\":0.0,\"status\":\"畅通\"},{\"name\":\"白市驿\",\"congestionIndex\":1.1,\"realSpeed\":\"45.5646\",\"yoy\":-0.1,\"mom\":0.0,\"status\":\"畅通\"},{\"name\":\"金凤\",\"congestionIndex\":1.2,\"realSpeed\":\"44.2227\",\"yoy\":0.0,\"mom\":0.0,\"status\":\"畅通\"},{\"name\":\"保税区-A区\",\"congestionIndex\":1.4,\"realSpeed\":\"30.8192\",\"yoy\":0.0,\"mom\":0.0,\"status\":\"畅通\"},{\"name\":\"高新天街\",\"congestionIndex\":1.4,\"realSpeed\":\"19.2326\",\"yoy\":0.1,\"mom\":0.0,\"status\":\"畅通\"},{\"name\":\"熙街\",\"congestionIndex\":1.6,\"realSpeed\":\"23.2695\",\"yoy\":0.0,\"mom\":0.0,\"status\":\"缓行\"}]";
        // 使用 JSONPath.eval 获取多条件筛选的结果
        JSONArray congestionIndexList1 = (JSONArray) JSONPath.eval(JSONArray.parseArray(jsonString), "$[?(@.name == '高新区管委会')].congestionIndex");
        System.out.println(congestionIndexList1.getClass());
        // 注意这里实际的类型 class com.alibaba.fastjson.JSONArray
        System.out.println(congestionIndexList1);
        // [1.2]
        Object value = congestionIndexList1.get(0);
        System.out.println(value.getClass());
        // class java.math.BigDecimal
        System.out.println(value);
        // 1.2
    }

    // 返回集合中多个元素
    @Test
    void test02() {
        // 如果是数组结构 []，不要转换成字符串，不然会解析失败
        JSONArray objectList = (JSONArray) JSONPath.eval(list, "[1,5]"); // 返回下标为1和5的元素
        System.out.println(objectList.getClass());
        // 注意这里实际的类型 class com.alibaba.fastjson.JSONArray
        List<UserInfoEntity> userList = objectList.toJavaList(UserInfoEntity.class);
        System.out.println(userList);
        // [UserInfoEntity(id=2, name=吕昊 ...
    }

    // 按范围返回集合的子集
    @Test
    void test03() {
        // 如果是数组结构 []，不要转换成字符串，不然会解析失败
        List<UserInfoEntity> objectList =  (List<UserInfoEntity>) JSONPath.eval(list, "[1:5]"); // 返回下标从1到5的元素
        System.out.println(objectList.getClass());
        // 注意这里实际的类型 class java.util.ArrayList
        System.out.println(objectList.get(0).getClass());
        // class local.ateng.java.fastjson.entity.UserInfoEntity
        System.out.println(objectList);
        // [UserInfoEntity(id=2, name=陈嘉熙, ...
    }

    // 通过条件过滤，返回集合的子集
    @Test
    void test04() {
        // 如果是数组结构 []，不要转换成字符串，不然会解析失败
        JSONArray objectList = (JSONArray) JSONPath.eval(list, "$[?(@.id in (88,99))]"); // 返回数组中id在从88到99的元素
        System.out.println(objectList.getClass());
        // 注意这里实际的类型 class com.alibaba.fastjson.JSONArray
        List<UserInfoEntity> userList = objectList.toJavaList(UserInfoEntity.class);
        System.out.println(userList);
        // [UserInfoEntity(id=2, name=吕昊 ...
    }
    @Test
    void test04_2() {
        // 如果是数组结构 []，不要转换成字符串，不然会解析失败
        JSONArray objectList = (JSONArray) JSONPath.eval(list, "$[?(@.age = 88)]"); // 返回列表对象的age=88的数据
        System.out.println(objectList.getClass());
        // 注意这里实际的类型 class com.alibaba.fastjson.JSONArray
        List<UserInfoEntity> userList = objectList.toJavaList(UserInfoEntity.class);
        System.out.println(userList);
        // [UserInfoEntity(id=42, name=邱振家, age=88,
    }

    // 多条件筛选，返回集合的子集
    @Test
    void test04_3() {
        // 如果是数组结构 []，不要转换成字符串，不然会解析失败
        JSONArray objectList = (JSONArray) JSONPath.eval(list, "$[?(@.age == 88 || @.age == 95)]"); // 返回列表对象的age=88或者=95的数据
        System.out.println(objectList.getClass());
        // 注意这里实际的类型 class com.alibaba.fastjson.JSONArray
        List<UserInfoEntity> userList = objectList.toJavaList(UserInfoEntity.class);
        System.out.println(userList);
        // [UserInfoEntity(id=45, name=覃文轩, age=88,
    }
    @Test
    void test04_4() {
        // 如果是数组结构 []，不要转换成字符串，不然会解析失败
        JSONArray objectList = (JSONArray) JSONPath.eval(list, "$[?(@.age > 50 && @.age < 95 && @.city='东莞')]");
        System.out.println(objectList.getClass());
        // 注意这里实际的类型 class com.alibaba.fastjson.JSONArray
        List<UserInfoEntity> userList = objectList.toJavaList(UserInfoEntity.class);
        System.out.println(userList);
        // [UserInfoEntity(id=33, name=阎健柏, age=77,
    }

    // 通过条件过滤，获取数组长度
    @Test
    void test04_5() {
        // 如果是数组结构 []，不要转换成字符串，不然会解析失败
        Integer length = (Integer) JSONPath.eval(list, "$[?(@.age = 88)].length()"); // 返回列表对象的age=88的数据的长度
        System.out.println(JSONPath.eval(list, "$[?(@.age = 88)].length()").getClass());
        // 注意这里实际的类型 class com.alibaba.fastjson.JSONArray
        System.out.println(length);
        // 67
    }

}
