# Stream

Java的`Stream`是一个用于处理集合的工具，它允许以声明式方式进行过滤、映射、排序、聚合等操作。通过`Stream`，可以链式调用各种操作，提升代码简洁性和可读性。`Stream`支持惰性求值，并且可以轻松实现并行处理。常见操作包括`filter`、`map`、`collect`和`forEach`等。

## 数据准备

### 创建实体类

```java
package local.ateng.java.stream.entity;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Date;

/**
 * 用户信息实体类
 * 用于表示系统中的用户信息。
 *
 * @author 孔余
 * @since 2024-01-10 15:51
 */
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class UserInfoEntity {

    /**
     * 用户ID
     */
    private Long id;

    /**
     * 用户姓名
     */
    private String name;

    /**
     * 用户年龄
     * 注意：这里使用Integer类型，表示年龄是一个整数值。
     */
    private Integer age;

    /**
     * 分数
     */
    private Double score;

    /**
     * 用户生日
     * 注意：这里使用Date类型，表示用户的生日。
     */
    private Date birthday;

    /**
     * 用户所在省份
     */
    private String province;

    /**
     * 用户所在城市
     */
    private String city;
}
```

### 数据生成器

```java
package local.ateng.java.stream.init;

import com.github.javafaker.Faker;
import local.ateng.java.stream.entity.UserInfoEntity;
import lombok.Getter;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * 初始化数据
 *
 * @author 孔余
 * @email 2385569970@qq.com
 * @since 2025-01-09
 */
@Getter
public class InitData {
    List<UserInfoEntity> list;
    List<UserInfoEntity> list2;

    public InitData() {
        //生成测试数据
        // 创建一个Java Faker实例，指定Locale为中文
        Faker faker = new Faker(new Locale("zh-CN"));
        // 创建一个包含不少于100条JSON数据的列表
        List<UserInfoEntity> userList = new ArrayList();
        for (int i = 1; i <= 3000; i++) {
            UserInfoEntity user = new UserInfoEntity();
            user.setId((long) i);
            user.setName(faker.name().fullName());
            user.setBirthday(faker.date().birthday());
            user.setAge(faker.number().numberBetween(0, 100));
            user.setProvince(faker.address().state());
            user.setCity(faker.address().cityName());
            user.setScore(faker.number().randomDouble(3, 1, 100));
            userList.add(user);
        }
        list = userList;
        for (int i = 1; i <= 3000; i++) {
            UserInfoEntity user = new UserInfoEntity();
            user.setId((long) i);
            user.setName(faker.name().fullName());
            user.setBirthday(faker.date().birthday());
            user.setAge(faker.number().numberBetween(0, 100));
            user.setProvince(faker.address().state());
            user.setCity(faker.address().cityName());
            user.setScore(faker.number().randomDouble(3, 1, 100));
            userList.add(user);
        }
        list2 = userList;
    }

}
```



## 基本使用

### 数组转列表

```java
    /**
     * int[]转List<Integer>
     */
    @Test
    void test01() {
        int[] ints = {1, 2, 3, 4};
        List<Integer> list = Arrays.stream(ints).boxed().collect(Collectors.toList());
        System.out.println(list);
    }
```

### 过滤指定字段数据

```java
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
```

### 查找数据

```java
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
```

### 映射flatMap

```java
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
```

### 映射Map

```java
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
```

### 数据去重

```java
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
```

### 排序Sort

```java
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
```

### 遍历ForEach

```java
    /**
     * 遍历ForEach:
     * 使用forEach遍历元素。
     */
    @Test
    void forEach() {
        list.stream().forEach(System.out::println);
    }
```

### 限制Limit和跳过Skip

```java
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
```

### 归约Reduce

```java
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
```



## 分组聚合

### 分组Grouping

```java
    /**
     分组Grouping:
     使用Collectors.groupingBy进行分组操作。
     */
    @Test
    void group01() {
        Map<String, List<UserInfoEntity>> groupedByRegion = list.stream()
                .collect(Collectors.groupingBy(user -> user.getProvince()));
        System.out.println(groupedByRegion.get("重庆市"));
    }
```

### 分组聚合

```java
    /**
     * 将数据分组并进行聚合操作
     */
    @Test
    void group02() {
        // 平均值
        Map<String, Double> averageAgeByProvince = list.stream()
                .collect(Collectors.groupingBy(UserInfoEntity::getProvince, Collectors.averagingInt(UserInfoEntity::getAge)));
        System.out.println(averageAgeByProvince);
        // 数量
        Map<String, Long> groupByCount = list.stream()
                .collect(Collectors.groupingBy(UserInfoEntity::getCity, Collectors.counting()));
        System.out.println(groupByCount);
    }
```

### 分组映射

```java
    /**
     * 将数据分组并进行聚合操作，最后返回为一个自定义列表
     */
    @Test
    void group02_1() {
        // 数量
        List<HashMap<String, Object>> mapList = list.stream()
                .collect(Collectors.groupingBy(UserInfoEntity::getCity, Collectors.counting()))
                .entrySet()
                .stream()
                .map(d -> new HashMap<String, Object>() {{
                    put("name", d.getKey());
                    put("count", d.getValue());
                }})
                .collect(Collectors.toList());
        System.out.println(mapList);
    }
```

### 分组排序

```java
    /**
     * 分组后排序
     * 按province字段分组，然后对每个分组按姓名排序
     */
    @Test
    void groupAndSorted() {
        Map<String, List<UserInfoEntity>> groupedAndSortedData = list.stream()
                .collect(Collectors.groupingBy(UserInfoEntity::getProvince, Collectors.toList()))
                .entrySet()
                .stream()
                .collect(Collectors.toMap(Map.Entry::getKey, entry -> entry.getValue().stream()
                        .sorted(Comparator.comparing(UserInfoEntity::getName))
                        .collect(Collectors.toList())));
        System.out.println(groupedAndSortedData);
    }
```

### 分组计算

```java
    /**
     * 列表去除重复的name，其他项并求平均值。
     */
    @Test
    void test1() {
        List<UserInfoEntity> userList = list.stream()
                .collect(Collectors.groupingBy(UserInfoEntity::getName,
                        Collectors.collectingAndThen(
                                Collectors.reducing((data1, data2) ->
                                        new UserInfoEntity(
                                                data1.getId(),
                                                data1.getName(),
                                                NumberUtil.round((data1.getAge() + data2.getAge()) / 2, 0).intValue(),
                                                NumberUtil.round((data1.getScore() + data2.getScore()) / 2, 1).doubleValue(),
                                                data1.getBirthday(),
                                                data1.getProvince(),
                                                data1.getCity()
                                        )
                                ),
                                data -> data.orElse(null)
                        )
                ))
                .values()
                .stream()
                .collect(Collectors.toList());
        System.out.println(userList);
    }
```

## 集合

### 计算交集

```java
    /**
     * 计算交集
     * 交集：获取两个列表中相同province的UserEntity对象。
     */
    @Test
    void intersection() {
        List<UserInfoEntity> intersection = list.stream()
                .filter(user1 -> list2.stream().anyMatch(user2 -> user2.getName().equals(user1.getName())))
                .collect(Collectors.toList());
        System.out.println(intersection.size());
    }
```

### 计算并集

```java
    /**
     * 计算并集
     * 并集：合并两个列表并去除重复的name。
     */
    @Test
    void union() {
        List<UserInfoEntity> union = Stream.of(list, list2)
                .flatMap(List::stream)
                .collect(Collectors.toMap(UserInfoEntity::getName, Function.identity(), (a, b) -> a))
                .values()
                .stream()
                .collect(Collectors.toList());
        System.out.println(union.size());

    }
```

### 计算差集

```java
    /**
     * 计算差集
     * 差集：获取在list1中但不在list2中的UserEntity对象。
     */
    @Test
    void difference() {
        List<UserInfoEntity> difference = list.stream()
                .filter(user1 -> list2.stream().noneMatch(user2 -> user2.getName().equals(user1.getName())))
                .collect(Collectors.toList());
        System.out.println(difference.size());
    }
```

