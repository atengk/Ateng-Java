package local.ateng.java.mybatisjdk8;

import local.ateng.java.customutils.entity.Menu;
import local.ateng.java.customutils.entity.Menu2;
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
import java.util.function.Function;
import java.util.stream.Collectors;

public class CollectionUtilTests {

    @Test
    void toMap() {
        List<Menu> menus = Arrays.asList(
                new Menu(1, 0, "系统管理"),
                new Menu(2, 1, "用户管理"),
                new Menu(3, 1, "角色管理"),
                new Menu(4, 2, "用户列表"),
                new Menu(5, 0, "首页"),
                new Menu(6, 3, "权限设置")
        );
        // 等价于你原来的“后覆盖前”
        Map<Integer, Menu> map = CollectionUtil.toMap(
                menus,
                Menu::getId,
                Function.identity(),
                (oldVal, newVal) -> newVal,
                HashMap::new,
                true,
                false
        );
        System.out.println(map);
        // 保持插入顺序（LinkedHashMap）
        Map<Integer, Menu> map2 = CollectionUtil.toMap(
                menus,
                Menu::getId,
                Function.identity(),
                (v1, v2) -> v2,
                LinkedHashMap::new,
                true,
                true
        );
        System.out.println(map2);
        // key 冲突时报错（更严谨）
        Map<Integer, Menu> map3 = CollectionUtil.toMap(
                menus,
                Menu::getId,
                Function.identity(),
                (v1, v2) -> {
                    throw new IllegalStateException("key 冲突：" + v1);
                },
                HashMap::new,
                true,
                true
        );
        System.out.println(map3);
        // value 合并（例如数值累加）
        Map<Integer, Integer> countMap = CollectionUtil.toMap(
                menus,
                Menu::getId,
                Menu::getParentId,
                Integer::sum,
                HashMap::new,
                true,
                true
        );
        System.out.println(countMap);
    }

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
    void fillTreeCode() {
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

        CollectionUtil.fillTreeCode(
                tree,
                Menu::getChildren,
                Menu::setTreeCode,
                "."
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
                new HashSet<Integer>(Arrays.asList(0, 1))
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
    void testFindInTree() {
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

        // 1. 在树中找到id为4的数据
        Menu target = CollectionUtil.findInTree(
                tree,
                Menu::getChildren,
                Menu::getId,
                4
        );
        System.out.println(JsonUtil.toJsonString(target));
        // 2. 在树中找到标识为"4-用户列表"的数据
        Menu target2 = CollectionUtil.findInTree(
                tree,
                Menu::getChildren,
                key -> key.getId() + "-" + key.getName(),
                "4-用户列表"
        );
        System.out.println(JsonUtil.toJsonString(target2));
    }

    @Test
    void testCollectTreeKeys() {
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

        // 1. 在树中找到id
        List<Integer> idList = CollectionUtil.collectTreeKeys(
                tree,
                Menu::getChildren,
                Menu::getId
        );
        System.out.println(idList);
        // 2. 在树中找到唯一标识
        List<String> keyList = CollectionUtil.collectTreeKeys(
                tree,
                Menu::getChildren,
                key -> key.getId() + "-" + key.getName()
        );
        System.out.println(keyList);
    }

    @Test
    void operateSubTreeById_test() {
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

        // 对 “系统管理(id=1)” 及其所有子节点执行操作
        CollectionUtil.operateSubTreeById(
                tree,
                Menu::getId,
                Menu::getChildren,
                2,
                menu -> menu.setName(menu.getName() + "_copy")
        );
        System.out.println(JsonUtil.toJsonString(tree));
    }

    @Test
    void operateMatchedNodeAndAncestors_test() {
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

        // 命中「用户列表(id=4)」，向上标记
        CollectionUtil.operateMatchedNodeAndAncestors(
                tree,
                menu -> Objects.equals(menu.getId(), 4),
                Menu::getChildren,
                menu -> menu.setName(menu.getName() + "-active")
        );
        System.out.println(JsonUtil.toJsonString(tree));
    }

    @Test
    void test0104() {
        List<Menu2> menus = Arrays.asList(
                new Menu2(1, 0, "系统管理", false, null),
                new Menu2(2, 1, "用户管理", false, null),
                new Menu2(3, 1, "角色管理", false, null),
                new Menu2(4, 2, "用户列表", false, null),
                new Menu2(5, 0, "首页"  , false, null  ),
                new Menu2(6, 3, "权限设置", false, null)
        );

        List<Menu2> tree = CollectionUtil.buildTree(
                menus,
                Menu2::getId,
                Menu2::getParentId,
                Menu2::setChildren,
                0
        );
        System.out.println(JsonUtil.toJsonString(tree));
        List<String> keyList = Arrays.asList("用户列表", "权限设置");

        CollectionUtil.markTreeByChildrenAllMatch(
                tree,
                Menu2::getChildren,
                node -> keyList.contains(node.getName()),
                node -> node.setDisabled(true)
        );
        System.out.println(JsonUtil.toJsonString(tree));

    }

    @Test
    void operateMatchedNode_test() {
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

        // 对 “系统管理(id=1)” 及其所有子节点执行操作
        CollectionUtil.operateMatchedNode(
                tree,
                menu -> Objects.equals(menu.getId(), 1),
                Menu::getChildren,
                menu -> menu.setName(menu.getName() + "-copy")
        );
        System.out.println(JsonUtil.toJsonString(tree));
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

    @Test
    void testGroupByMultiKey() {

        List<MyUser> userList = InitData.getDataList();

        Map<String, List<MyUser>> map = CollectionUtil.groupBy(
                userList,
                user -> user.getProvince() + "-" + user.getCity() + "-" + user.getAge(),
                Function.identity(),
                LinkedHashMap::new,
                ArrayList::new,
                true,
                false
        );

        map.forEach((k, v) -> {
            System.out.println("key = " + k);
            v.forEach(u -> System.out.println("  " + u.getName()));
        });
    }

    @Test
    void testGroupByProvinceOnlyName() {

        List<MyUser> userList = InitData.getDataList();

        Map<String, List<String>> map = CollectionUtil.groupBy(
                userList,
                MyUser::getProvince,
                MyUser::getName,
                HashMap::new,
                ArrayList::new,
                true,
                true
        );

        map.forEach((k, v) -> {
            System.out.println("province = " + k + ", names = " + v);
        });
    }

    @Test
    void testGroupByProvinceKeepOrder() {

        List<MyUser> userList = InitData.getDataList();

        Map<String, List<MyUser>> map = CollectionUtil.groupBy(
                userList,
                MyUser::getProvince,
                Function.identity(),
                LinkedHashMap::new,
                LinkedList::new,
                true,
                false
        );

        map.forEach((k, v) -> {
            System.out.println("province = " + k);
            v.forEach(u -> System.out.println("  " + u.getId() + " - " + u.getName()));
        });
    }

    @Test
    void testGroupByAgeRange() {

        List<MyUser> userList = InitData.getDataList();

        Map<String, List<MyUser>> map = CollectionUtil.groupBy(
                userList,
                user -> {
                    if (user.getAge() == null) {
                        return "UNKNOWN";
                    }
                    if (user.getAge() < 18) {
                        return "UNDER_18";
                    }
                    if (user.getAge() <= 30) {
                        return "18_30";
                    }
                    return "30_PLUS";
                },
                Function.identity(),
                HashMap::new,
                ArrayList::new,
                true,
                false
        );

        map.forEach((k, v) -> {
            System.out.println("ageRange = " + k + ", count = " + v.size());
        });
    }

    @Test
    void testGroupByMultiFields() {
        List<Employee> list = Arrays.asList(
                new Employee("技术部", "后端", "上海"),
                new Employee("技术部", "前端", "上海"),
                new Employee("技术部", "后端", "杭州"),
                new Employee("市场部", "销售", "上海"),
                new Employee("技术部", "后端", "上海")
        );

        Map<String, List<Employee>> grouped = CollectionUtil.groupBy(
                list,
                e -> e.getDept() + "-" + e.getJob() + "-" + e.getCity()
        );

        grouped.forEach((k, v) -> {
            System.out.println("分组：" + k);
            v.forEach(System.out::println);
            System.out.println();
        });
    }

    @Data
    @AllArgsConstructor
    @NoArgsConstructor
    public static class Employee {
        private String dept;
        private String job;
        private String city;
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

    @Data
    public static class UserEntity {
        private Long id;
        private String name;
        private Integer age;
    }

    @Data
    public static class UserVO {
        private Long id;
        private String name;
    }

    public static List<UserEntity> buildSourceList() {
        List<UserEntity> list = new ArrayList<>();

        UserEntity u1 = new UserEntity();
        u1.setId(1L);
        u1.setName("张三");
        u1.setAge(18);

        UserEntity u2 = new UserEntity();
        u2.setId(2L);
        u2.setName("李四");
        u2.setAge(20);

        UserEntity u3 = new UserEntity();
        u3.setId(3L);
        u3.setName("王五");
        u3.setAge(22);

        list.add(u1);
        list.add(u2);
        list.add(u3);

        return list;
    }

    public static List<UserVO> buildTargetList() {
        List<UserVO> list = new ArrayList<>();

        UserVO v1 = new UserVO();
        v1.setId(1L);

        UserVO v2 = new UserVO();
        v2.setId(2L);

        UserVO v3 = new UserVO();
        v3.setId(4L);

        list.add(v1);
        list.add(v2);
        list.add(v3);

        return list;
    }

    /**
     * 单字段回填测试
     */
    @Test
    public void testFillNameById() {
        List<UserEntity> sourceList = buildSourceList();
        List<UserVO> targetList = buildTargetList();

        CollectionUtil.fillByKey(
                sourceList,
                targetList,
                UserEntity::getId,
                UserVO::getId,
                UserEntity::getName,
                UserVO::setName
        );

        targetList.forEach(System.out::println);
    }

    /**
     * 多字段回填测试
     */
    @Test
    public void testFillMultiField() {
        List<UserEntity> sourceList = buildSourceList();
        List<UserVO> targetList = buildTargetList();

        CollectionUtil.fillByKey(
                sourceList,
                targetList,
                UserEntity::getId,
                UserVO::getId,
                source -> source,
                (target, source) -> {
                    target.setName(source.getName());
                }
        );

        targetList.forEach(System.out::println);
    }

    @Test
    public void testFillByOrder() {
        List<UserEntity> sourceList = buildSourceList();
        List<UserVO> targetList = buildTargetList();

        CollectionUtil.fillByOrder(
                sourceList,
                targetList,
                UserEntity::getName,
                UserVO::setName
        );

        targetList.forEach(System.out::println);
    }

    @Test
    public void testIsEqualIgnoreOrder() {
        List<String> list1 = Arrays.asList("1", "2", "3");
        List<String> list2 = Arrays.asList("1", "2", "3");
        boolean result = CollectionUtil.isEqualIgnoreOrder(list1, list2);
        System.out.println(result);
        List<String> list12 = Arrays.asList("1", "2", "3");
        List<String> list22 = Arrays.asList("1", "2", "3", "4");
        boolean result2 = CollectionUtil.isEqualIgnoreOrder(list12, list22);
        System.out.println(result2);
        List<String> list13 = Arrays.asList("1", "2", "3");
        List<String> list23 = Arrays.asList("1", "3", "2");
        boolean result3 = CollectionUtil.isEqualIgnoreOrder(list13, list23);
        System.out.println(result3);
    }

}
