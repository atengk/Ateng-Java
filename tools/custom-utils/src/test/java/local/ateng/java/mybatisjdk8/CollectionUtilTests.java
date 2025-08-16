package local.ateng.java.mybatisjdk8;

import local.ateng.java.customutils.entity.Menu;
import local.ateng.java.customutils.utils.CollectionUtil;
import local.ateng.java.customutils.utils.JsonUtil;
import org.junit.jupiter.api.Test;

import java.util.*;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeoutException;
import java.util.stream.Collectors;

public class CollectionUtilTests {

    @Test
    void buildTree() {
        List<Menu> menus = Arrays.asList(
                new Menu(1, 0, "系统管理"),
                new Menu(2, 1, "用户管理"),
                new Menu(3, 1, "角色管理"),
                new Menu(4, 2, "用户列表"),
                new Menu(5, 0, "首页"),
                new Menu(6, 3, "权限设置")
        );

        List<Menu> tree = CollectionUtil.buildTree(
                menus,
                Menu::getId,
                Menu::getParentId,
                Menu::setChildren,
                0
        );

        System.out.println(JsonUtil.toJsonString(tree));
    }

    @Test
    void buildMultiRootTree() {
        List<Menu> menus = Arrays.asList(
                new Menu(1, 0, "系统管理"),
                new Menu(2, 1, "用户管理"),
                new Menu(3, 1, "角色管理"),
                new Menu(4, 2, "用户列表"),
                new Menu(5, 0, "首页"),
                new Menu(6, 3, "权限设置")
        );

        List<Menu> tree = CollectionUtil.buildMultiRootTree(
                menus,
                Menu::getId,
                Menu::getParentId,
                Menu::setChildren,
                new HashSet<Integer>(Arrays.asList(0,1))
        );

        System.out.println(JsonUtil.toJsonString(tree));
    }

    @Test
    void test111() {
        Set<Object> singleton = Collections.singleton(null);
        System.out.println(singleton.contains(null));
    }

    @Test
    void buildTreeComparator() {
        List<Menu> menus = Arrays.asList(
                new Menu(1, 0, "系统管理"),
                new Menu(2, 1, "用户管理"),
                new Menu(3, 1, "角色管理"),
                new Menu(4, 2, "用户列表"),
                new Menu(5, 0, "首页"),
                new Menu(6, 3, "权限设置")
        );

        List<Menu> tree = CollectionUtil.buildTree(
                menus,
                Menu::getId,
                Menu::getParentId,
                Menu::setChildren,
                Menu::getChildren,
                0,
                Comparator.comparing(Menu::getId).reversed()
        );

        System.out.println(JsonUtil.toJsonString(tree));
    }

    @Test
    void buildMultiRootTreeComparator() {
        List<Menu> menus = Arrays.asList(
                new Menu(1, 0, "系统管理"),
                new Menu(2, 1, "用户管理"),
                new Menu(3, 1, "角色管理"),
                new Menu(4, 2, "用户列表"),
                new Menu(5, 0, "首页"),
                new Menu(6, 3, "权限设置")
        );

        List<Menu> tree = CollectionUtil.buildMultiRootTree(
                menus,
                Menu::getId,
                Menu::getParentId,
                Menu::setChildren,
                Menu::getChildren,
                new HashSet<>(Arrays.asList(0)),
                Comparator.comparing(Menu::getId, Comparator.reverseOrder())
                        .thenComparing(Menu::getName)
        );

        System.out.println(JsonUtil.toJsonString(tree));
    }

    @Test
    void treeToList() {
        List<Menu> menus = Arrays.asList(
                new Menu(1, 0, "系统管理"),
                new Menu(2, 1, "用户管理"),
                new Menu(3, 1, "角色管理"),
                new Menu(4, 2, "用户列表"),
                new Menu(5, 0, "首页"),
                new Menu(6, 3, "权限设置")
        );

        // 构建树
        List<Menu> tree = CollectionUtil.buildTree(
                menus,
                Menu::getId,
                Menu::getParentId,
                Menu::setChildren,
                0
        );

        // 将树结构转换为扁平列表
        List<Menu> flatList = CollectionUtil.treeToList(
                tree,
                Menu::getChildren,
                Menu::setChildren,
                Comparator.comparing(Menu::getId)
        );
        flatList.forEach(System.out::println);

    }

    @Test
    void batchProcess() {
        List<Integer> data = new ArrayList<>();
        for (int i = 1; i <= 25; i++) {
            data.add(i);
        }

        CollectionUtil.batchProcess(data, 10, batch -> {
            System.out.println("处理批次：" + batch);
            // 这里可以做数据库批量插入、网络请求等操作
        });
    }

    @Test
    void batchProcessAsync() {
        List<Integer> data = new ArrayList<>();
        for (int i = 1; i <= 250; i++) {
            data.add(i);
        }

        List<String> results = CollectionUtil.batchProcessAsync(data, 10, batch -> {
            // 模拟异步批处理，比如调用远程接口或数据库
            System.out.println("异步处理批次：" + batch);
            return batch.stream()
                    .map(i -> "处理结果-" + i)
                    .collect(Collectors.toList());
        });

        System.out.println("所有批次处理完毕，合并结果：" + results);

    }

    @Test
    void batchProcessAsync2() {
        List<Integer> data = new ArrayList<>();
        for (int i = 1; i <= 250; i++) {
            data.add(i);
        }

        ExecutorService executor = Executors.newFixedThreadPool(4);

        try {
            List<String> results = CollectionUtil.batchProcessAsync(
                    data,
                    10,
                    batch -> {
                        System.out.println("处理批次：" + batch);
                        // 模拟可能抛异常的处理
                        if (batch.contains(13)) {
                            throw new RuntimeException("模拟异常");
                        }
                        return batch.stream()
                                .map(i -> "结果-" + i)
                                .collect(Collectors.toList());
                    },
                    executor,
                    5000
            );
            System.out.println("所有批次处理完毕：" + results);
        } catch (TimeoutException e) {
            System.err.println("批处理超时：" + e.getMessage());
        } catch (ExecutionException e) {
            System.err.println("批处理执行异常：" + e.getCause().getMessage());
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            System.err.println("批处理被中断");
        } finally {
            executor.shutdown();
        }

    }


    @Test
    void test1111() {
        List<String> list = new ArrayList<>();
        list.add("B");
        list.add("C");

        // 在列表最前面插入一条数据
        list.add(0, "A");

        System.out.println(list); // 输出: [A, B, C]
    }

}
