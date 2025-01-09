package local.ateng.java.stream;


import local.ateng.java.stream.entity.UserInfoEntity;
import local.ateng.java.stream.init.InitData;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Stream流的基本使用
 *
 * @author 孔余
 * @email 2385569970@qq.com
 * @since 2025-01-09
 */
public class StreamBasicTests {
    List<UserInfoEntity> list;
    List<UserInfoEntity> list2;

    public StreamBasicTests() {
        InitData initData = new InitData();
        list = initData.getList();
        list2 = initData.getList2();
    }

    /**
     * int[]转List<Integer>
     */
    @Test
    void test01() {
        int[] ints = {1, 2, 3, 4};
        List<Integer> list = Arrays.stream(ints).boxed().collect(Collectors.toList());
        System.out.println(list);
    }

    /**
     * 过滤指定字段数据
     */
    @Test
    void filter01() {
        List<UserInfoEntity> filteredList = list.stream()
                .filter(user -> user.getAge() > 90)
                .collect(Collectors.toList());
        System.out.println(filteredList);
    }

    /**
     * 查找Find:
     * 使用findFirst和findAny查找第一个匹配元素。
     */
    @Test
    void filter02() {
        // findFirst
        UserInfoEntity firstMatch = list.stream()
                .filter(user -> user.getName().startsWith("孔"))
                .findFirst()
                .get();
        System.out.println(firstMatch);
        // findAny
        boolean anyAge25 = list.stream().anyMatch(user -> user.getAge() == 25);
        System.out.println(anyAge25);
    }

    /**
     * 映射flatMap:
     */
    @Test
    void flatMap01() {
        List<String> flatMapList = list.stream()
                .flatMap(user -> Stream.of(user.getName() + user.getId()))
                .collect(Collectors.toList());
        System.out.println(flatMapList);
    }

    /**
     * 映射Map:
     * 使用map将元素转换为新的值。
     */
    @Test
    void map01() {
        List<String> namesList = list.stream()
                .map(user -> user.getName())
                .collect(Collectors.toList());
        System.out.println(namesList);
    }

    /**
     * 映射Map:
     * 使用mapToInt将所有的Age字段求和。
     */
    @Test
    void map02() {
        int sum = list.stream()
                .mapToInt(UserInfoEntity::getAge).sum();
        System.out.println(sum);
    }

    /**
     * 映射Map:
     * 使用map将元素转换为新Map。
     */
    @Test
    void map03() {
        Map<Long, String> map = list.stream()
                .collect(Collectors.toMap(
                        UserInfoEntity::getId, UserInfoEntity::getName,
                        (existing, replacement) -> replacement // 在发生冲突时保留新值
                ));
        System.out.println(map);
    }
    @Test
    void map03_2() {
        Map<Long, UserInfoEntity> map = list.stream()
                .collect(Collectors.toMap(
                        UserInfoEntity::getId, Function.identity(),
                        (existing, replacement) -> replacement // 在发生冲突时保留新值
                ));
        System.out.println(map);
    }

    /**
     * 按province字段去重，保留最新的数据
     */
    @Test
    void map04() {
        // 保存最新数据
        List<UserInfoEntity> uniqueByProvinceList = list.stream()
                .collect(Collectors.toMap(
                        UserInfoEntity::getProvince, user -> user,
                        (existing, replacement) -> replacement
                ))
                .values()
                .stream().collect(Collectors.toList());
        System.out.println(uniqueByProvinceList);
    }

    /**
     * 按province字段去重，保留最新的数据
     */
    @Test
    void map05() {
        // 根据指定的逻辑保存数据
        List<UserInfoEntity> uniqueByProvinceList = list.stream()
                .collect(Collectors.toMap(
                        UserInfoEntity::getProvince, user -> user, (existing, replacement) -> {
                            Integer existingAge = existing.getAge();
                            Integer replacementAge = replacement.getAge();
                            return existingAge < replacementAge ? replacement : existing;
                        }
                ))
                .values()
                .stream().collect(Collectors.toList());
        System.out.println(uniqueByProvinceList);
    }

    /**
     * 排序Sort:
     * 使用sorted对元素排序。
     */
    @Test
    void sorted01() {
        List<UserInfoEntity> sortedByAgeAsc1 = list.stream()
                .sorted((user1, user2) -> Integer.compare(user1.getAge(), user2.getAge()))
                .collect(Collectors.toList());
        System.out.println("升序排序：" + sortedByAgeAsc1.get(0));
        List<UserInfoEntity> sortedByAgeDesc1 = list.stream()
                .sorted((user1, user2) -> Integer.compare(user2.getAge(), user1.getAge()))
                .collect(Collectors.toList());
        System.out.println("降序排序：" + sortedByAgeDesc1.get(0));
    }

    /**
     * 排序Sort:
     * 使用sorted对元素排序。
     */
    @Test
    void sorted02() {
        List<UserInfoEntity> sortedByAgeAsc2 = list.stream()
                .sorted(Comparator.comparing(UserInfoEntity::getAge))
                .collect(Collectors.toList());
        System.out.println("升序排序：" + sortedByAgeAsc2.get(0));
        List<UserInfoEntity> sortedByAgeDesc2 = list.stream()
                .sorted(Comparator.comparing(UserInfoEntity::getAge).reversed())
                .collect(Collectors.toList());
        System.out.println("降序排序：" + sortedByAgeDesc2.get(0));
    }

    /**
     * 遍历ForEach:
     * 使用forEach遍历元素。
     */
    @Test
    void forEach() {
        list.stream().forEach(System.out::println);
    }

    /**
     * 限制Limit和跳过Skip:
     * 使用limit限制元素数量，使用skip跳过元素。
     */
    @Test
    void limitAndSkip() {
        List<UserInfoEntity> limitedList = list.stream()
                .limit(1)
                .collect(Collectors.toList());
        List<UserInfoEntity> skippedList = list.stream()
                .skip(2999)
                .collect(Collectors.toList());
        System.out.println(limitedList);
        System.out.println(skippedList);
    }

    /**
     * 归约Reduce:
     * 使用reduce将元素聚合成一个值。
     */
    @Test
    void reduce() {
        Double totalValue = list.stream()
                .map(UserInfoEntity::getScore)
                .reduce(0.0, (a, b) -> a + b);
        System.out.println(totalValue);
    }

}
