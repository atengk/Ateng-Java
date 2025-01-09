package local.ateng.java.stream;

import cn.hutool.core.util.NumberUtil;
import local.ateng.java.stream.entity.UserInfoEntity;
import local.ateng.java.stream.init.InitData;
import org.junit.jupiter.api.Test;

import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Stream流的分组聚合使用
 *
 * @author 孔余
 * @since 2024-02-27 14:09
 */
public class StreamGroupTests {
    List<UserInfoEntity> list;
    List<UserInfoEntity> list2;

    public StreamGroupTests() {
        InitData initData = new InitData();
        list = initData.getList();
        list2 = initData.getList2();
    }

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


}
