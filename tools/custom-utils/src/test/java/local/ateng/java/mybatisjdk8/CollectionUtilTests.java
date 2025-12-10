package local.ateng.java.mybatisjdk8;

import local.ateng.java.customutils.entity.Menu;
import local.ateng.java.customutils.entity.MyUser;
import local.ateng.java.customutils.init.InitData;
import local.ateng.java.customutils.utils.CollectionUtil;
import local.ateng.java.customutils.utils.JsonUtil;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
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

    @Test
    void groupBy01() {
        List<String> list = Arrays.asList("a", "bb", "ccc", "dd");
        Map<Integer, List<String>> result = CollectionUtil.groupBy(list, String::length);
        System.out.println(result);
    }

    @Test
    void groupBy02() {
        Map<String, List<MyUser>> result = CollectionUtil.groupBy(InitData.getDataList(), MyUser::getProvince);
        System.out.println(result);
    }

    @Test
    void groupBy03() {
        Map<String, List<MyUser>> result = CollectionUtil.groupBy(InitData.getDataList(), item -> {
            Integer age = item.getAge();
            if (age < 18) return "未成年";
            else if (age < 40) return "青年";
            else return "中年";
        });
        System.out.println(result);
    }

    @Data
    @AllArgsConstructor
    @NoArgsConstructor
    public static class ConfTypeFlatDTO {
        private Long id;
        private String name;
        private String employeeNo;
        private String role;
        private Long scoreId;
        private String scoreName;
        private Integer scoreValue;
        // getter / setter
    }
    @Data
    @AllArgsConstructor
    @NoArgsConstructor
    public static class ConfScoreBaseDTO {
        private Long id;
        private String name;
        private Integer value;
        // getter / setter
    }
    @Data
    @AllArgsConstructor
    @NoArgsConstructor
    public static class ConfTypeVO {
        private Long id;
        private String name;
        private String employeeNo;
        private String role;
        private List<ConfScoreBaseDTO> scoreList;
        // getter / setter
    }
    @Test
    void groupAsParentChildren() {
        List<ConfTypeFlatDTO> flatList = Arrays.asList(
                new ConfTypeFlatDTO(1L, "基础类型", "E001", "admin",
                        101L, "加班分", 5),

                new ConfTypeFlatDTO(1L, "基础类型", "E001", "admin",
                        102L, "绩效分", 8),

                new ConfTypeFlatDTO(2L, "专项类型", "E002", "leader",
                        201L, "奖金分", 10),

                new ConfTypeFlatDTO(2L, "专项类型", "E002", "leader",
                        202L, "惩罚分", -3)
        );
        List<ConfTypeVO> voList = CollectionUtil.groupAsParentChildren(
                flatList,
                ConfTypeFlatDTO::getId,
                f -> {
                    ConfTypeVO vo = new ConfTypeVO();
                    vo.setId(f.getId());
                    vo.setName(f.getName());
                    vo.setEmployeeNo(f.getEmployeeNo());
                    vo.setRole(f.getRole());
                    return vo;
                },
                ConfTypeFlatDTO::getScoreId,
                f -> {
                    ConfScoreBaseDTO dto = new ConfScoreBaseDTO();
                    dto.setId(f.getScoreId());
                    dto.setName(f.getScoreName());
                    dto.setValue(f.getScoreValue());
                    return dto;
                },
                ConfTypeVO::getScoreList,
                ConfTypeVO::setScoreList
        );
        System.out.println(voList);
        System.out.println(JsonUtil.toJsonString(voList));

    }

}
