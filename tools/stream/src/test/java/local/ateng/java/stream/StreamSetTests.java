package local.ateng.java.stream;

import local.ateng.java.stream.entity.UserInfoEntity;
import local.ateng.java.stream.init.InitData;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * 交集、并集、差集
 *
 * @author 孔余
 * @since 2024-02-27 15:07
 */
public class StreamSetTests {
    List<UserInfoEntity> list;
    List<UserInfoEntity> list2;

    public StreamSetTests() {
        InitData initData = new InitData();
        list = initData.getList();
        list2 = initData.getList2();
    }

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




}
