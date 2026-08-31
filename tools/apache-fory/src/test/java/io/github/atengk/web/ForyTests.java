package io.github.atengk.web;

import io.github.atengk.entity.MyUser;
import io.github.atengk.init.InitData;
import org.apache.fory.json.ForyJson;
import org.apache.fory.reflect.TypeRef;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

/**
 * Apache Fory JSON 测试
 *
 * @author Ateng
 * @since 2026-08-31
 */
public class ForyTests {

    private static final ForyJson JSON = ForyJson.builder()
            .build();

    /**
     * 测试 Java 对象序列化为 JSON 字符串
     */
    @Test
    void testToJson() {
        MyUser myUser = InitData.getDataList(1).getFirst();

        String json = JSON.toJson(myUser);

        System.out.println("JSON：");
        System.out.println(json);
    }

    /**
     * 测试 JSON 字符串反序列化为 Java 对象
     */
    @Test
    void testFromJson() {
        MyUser myUser = InitData.getDataList(1).getFirst();

        String json = JSON.toJson(myUser);
        MyUser result = JSON.fromJson(json, MyUser.class);

        System.out.println("JSON：");
        System.out.println(json);
        System.out.println("对象：");
        System.out.println(result);
    }

    /**
     * 测试 Java 对象序列化为 JSON 字节数组
     */
    @Test
    void testToJsonBytes() {
        MyUser myUser = InitData.getDataList(1).getFirst();

        byte[] bytes = JSON.toJsonBytes(myUser);

        System.out.println("JSON 字节数组长度：" + bytes.length);
        System.out.println("JSON 字节数组：");
        System.out.println(new String(bytes));
    }

    /**
     * 测试 JSON 字节数组反序列化为 Java 对象
     */
    @Test
    void testFromJsonBytes() {
        MyUser myUser = InitData.getDataList(1).getFirst();

        byte[] bytes = JSON.toJsonBytes(myUser);
        MyUser result = JSON.fromJson(bytes, MyUser.class);

        System.out.println("对象：");
        System.out.println(result);
    }

    /**
     * 测试 List 泛型对象反序列化
     */
    @Test
    void testFromJsonList() {
        List<MyUser> dataList = InitData.getDataList(3);

        String json = JSON.toJson(dataList);

        List<MyUser> result = JSON.fromJson(
                json,
                new TypeRef<List<MyUser>>() {
                }
        );

        System.out.println("JSON：");
        System.out.println(json);
        System.out.println("List：");
        System.out.println(result);
    }

    /**
     * 测试 Map 泛型对象反序列化
     */
    @Test
    void testFromJsonMap() {
        List<MyUser> dataList = InitData.getDataList(3);

        Map<Long, MyUser> userMap = dataList.stream()
                .collect(java.util.stream.Collectors.toMap(
                        MyUser::getId,
                        user -> user
                ));

        String json = JSON.toJson(userMap);

        Map<Long, MyUser> result = JSON.fromJson(
                json,
                new TypeRef<Map<Long, MyUser>>() {
                }
        );

        System.out.println("JSON：");
        System.out.println(json);
        System.out.println("Map：");
        System.out.println(result);
    }

}
