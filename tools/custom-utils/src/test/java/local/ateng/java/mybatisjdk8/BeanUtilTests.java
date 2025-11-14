package local.ateng.java.mybatisjdk8;

import local.ateng.java.customutils.entity.MyTask;
import local.ateng.java.customutils.entity.MyUser0;
import local.ateng.java.customutils.entity.MyUser1;
import local.ateng.java.customutils.entity.MyUser2;
import local.ateng.java.customutils.utils.BeanUtil;
import lombok.AllArgsConstructor;
import lombok.Data;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class BeanUtilTests {

    @Test
    void testBeanUtil() {
        MyUser2 myUser2 = new MyUser2();
        MyUser1 myUser1 = createMyUser1Sample();
        BeanUtil.copy(myUser1, myUser2);
        System.out.println(myUser2);
        System.out.println(myUser2.getUserName());
        System.out.println(myUser2.getMyUser0List().get(0).getId());
        myUser2.setId(0L);
        System.out.println(myUser1);
        System.out.println(myUser2);
    }

    @Test
    void copy() {
        MyUser2 myUser2 = new MyUser2();
        MyUser1 myUser1 = createMyUser1Sample();
        BeanUtil.copy(myUser1, myUser2);
        System.out.println(myUser2);
        System.out.println(myUser2.getUserName());
        System.out.println(myUser2.getMyUser0List().get(0).getId());
        myUser2.setId(0L);
        System.out.println(myUser1);
        System.out.println(myUser2);
    }

    @Test
    void copy2() {
        MyUser2 myUser2 = new MyUser2();
        myUser2.setId(0L);
        myUser2.setUserName("test");
        MyUser1 myUser1 = createMyUser1Sample();
        myUser1.setId(null);
        myUser1.setUserName(null);
        BeanUtil.copy(myUser1, myUser2);
        System.out.println(myUser2);
    }

    /**
     * Bean转Bean
     */
    @Test
    void test021() {
        A a = new A();
        a.setA("A");
        B b = new B();
        b.setB("B");
        C c = new C();
        BeanUtil.copy(a, c);
        BeanUtil.copy(b, c);
        System.out.println(c);
    }
    @Data
    public class A {
        private String a;
    }
    @Data
    public class B {
        private String b;
    }
    @Data
    public class C {
        private String a;
        private String b;
        private String c;
    }


    public static MyUser1 createMyUser1Sample() {
        MyUser0 user1 = new MyUser0();
        user1.setId(1001L);
        user1.setUserName("alice");
        user1.setToday(LocalDate.now());
        user1.setCreateTime(LocalDateTime.now().minusDays(1));

        MyUser0 user2 = new MyUser0();
        user2.setId(1002L);
        user2.setUserName("bob");
        user2.setToday(LocalDate.now().minusDays(2));
        user2.setCreateTime(LocalDateTime.now().minusHours(5));

        MyUser1 myUser1 = new MyUser1();
        myUser1.setId(1L);
        myUser1.setUserName("admin");
        myUser1.setToday(LocalDate.now());
        myUser1.setCreateTime(LocalDateTime.now());
        myUser1.setMyUser0(user1);
        myUser1.setMyUser0List(Arrays.asList(user1, user2));

        return myUser1;
    }

    @Test
    void toMap() {
        MyUser1 myUser1 = createMyUser1Sample();
        Map<String, Object> map = BeanUtil.toMap(myUser1);
        System.out.println(myUser1);
        System.out.println(map);
    }

    @Test
    void getProperty() {
        MyUser1 myUser1 = createMyUser1Sample();
        String userName = BeanUtil.getProperty(myUser1, "userName");
        System.out.println(userName);
        System.out.println(userName.getClass());
    }

    @Test
    void setProperty() {
        MyUser1 myUser1 = createMyUser1Sample();
        BeanUtil.setProperty(myUser1, "userName", "alice");
        String userName = BeanUtil.getProperty(myUser1, "userName");
        System.out.println(userName);
        System.out.println(userName.getClass());
    }

    @Test
    void getAllFieldNames() {
        List<String> allFieldNames = BeanUtil.getAllFieldNames(MyUser1.class);
        System.out.println(allFieldNames);
    }

    @Test
    void beanToMapMapping() {
        MyTask task = new MyTask();
        task.setId(1L);
        task.setStatus(2); // 原始值是 2

        // 构建字段映射表
        Map<String, Map<Object, Object>> valueMapping = new HashMap<>();
        Map<Object, Object> statusMap = new HashMap<>();
        statusMap.put(1, "未开始");
        statusMap.put(2, "进行中");
        statusMap.put(3, "已完成");
        valueMapping.put("status", statusMap);

        Map<String, Object> result = BeanUtil.toMapWithValueMapping(task, valueMapping);
        System.out.println(result); // 输出：进行中

    }

    @Test
    void desensitize() {
        MyUser1 myUser1 = createMyUser1Sample();
        Map<String, Object> map = BeanUtil.toDesensitizedMap(myUser1 , Arrays.asList("userName", "createTime", "myUser0","myUser0List"), "*");
        System.out.println(myUser1);
        System.out.println(map);
    }



    @Test
    void testBeanCopy() {
        SourceEntity source = new SourceEntity();
        source.setUsername("blair");
        source.setEmail("blair@example.com");
        source.setAge(30);
        source.setUserId(1001L);
        source.setActive(true);
        source.setCreateTime(LocalDateTime.now());
        source.setBalance(new BigDecimal("1234.56"));

        TargetEntity target = new TargetEntity();

        // 拷贝
        BeanUtil.copy(source, target);

        System.out.println(source);
        System.out.println(target);
    }
    @Data
    public class SourceEntity {
        private String username;
        private String email;
        private Integer age;
        private Long userId;
        private Boolean active;
        private LocalDateTime createTime;
        private BigDecimal balance;
    }
    @Data
    public class TargetEntity {
        private String username;
        private String email;
        private Integer age;
        private Long userId;
        private Boolean active;
        private LocalDateTime createTime;
        private BigDecimal balance;
    }


    @Test
    void testBeanCopyAdvanced() {
        SourceEntity2 source = new SourceEntity2();
        source.setUsername("blair");
        source.setEmail("blair@example.com");
        source.setAge(30);
        source.setUserId(1001L);
        source.setActive(true);
        source.setCreateTime(LocalDateTime.now());
        source.setBalance(new BigDecimal("1234.56"));
        source.setStatus(Status.ACTIVE);

        Map<String, String> meta = new HashMap<>();
        meta.put("role", "admin");
        meta.put("dept", "IT");
        source.setMeta(meta);

        List<String> tags = Arrays.asList("java", "backend", "spring");
        source.setTags(tags);

        NestedObject nested = new NestedObject();
        nested.setField1("nestedField1");
        nested.setField2(999);
        source.setNestedObject(nested);

        TargetEntity2 target = new TargetEntity2();

        // 拷贝
        BeanUtil.copy(source, target);

        System.out.println("Source: " + source);
        System.out.println("Target: " + target);
    }

    @Data
    public static class SourceEntity2 {
        private String username;
        private String email;
        private Integer age;
        private Long userId;
        private Boolean active;
        private LocalDateTime createTime;
        private BigDecimal balance;
        private Status status;
        private Map<String, String> meta;
        private List<String> tags;
        private NestedObject nestedObject;
    }

    @Data
    public static class TargetEntity2 {
        private String username;
        private String email;
        private Integer age;
        private Long userId;
        private Boolean active;
        private LocalDateTime createTime;
        private BigDecimal balance;
        private Status status;
        private Map<String, String> meta;
        private List<String> tags;
        private NestedObject nestedObject;
    }

    @Data
    public static class NestedObject {
        private String field1;
        private Integer field2;
    }

    public enum Status {
        ACTIVE,
        INACTIVE,
        LOCKED
    }

    @Test
    void testBeanCopyWithLists() {
        ParentSource parentSource = new ParentSource();
        parentSource.setName("Parent1");
        parentSource.setId(101L);
        parentSource.setAmount(new BigDecimal("500.75"));
        parentSource.setCreatedAt(LocalDateTime.now());

        // Nested list
        NestedItem nested1 = new NestedItem("nested1", 10);
        NestedItem nested2 = new NestedItem("nested2", 20);
        parentSource.setNestedItems(Arrays.asList(nested1, nested2));

        // Children entity list
        ChildEntity child1 = new ChildEntity(201L, "child1");
        ChildEntity child2 = new ChildEntity(202L, "child2");
        parentSource.setChildren(Arrays.asList(child1, child2));

        ParentTarget parentTarget = new ParentTarget();

        // 拷贝
        BeanUtil.copy(parentSource, parentTarget);

        System.out.println("Source: " + parentSource);
        System.out.println("Target: " + parentTarget);
    }

    @Data
    public static class ParentSource {
        private Long id;
        private String name;
        private BigDecimal amount;
        private LocalDateTime createdAt;
        private List<NestedItem> nestedItems;
        private List<ChildEntity> children;
    }

    @Data
    public static class ParentTarget {
        private Long id;
        private String name;
        private BigDecimal amount;
        private LocalDateTime createdAt;
        private List<NestedItem> nestedItems;
        private List<ChildEntity> children;
    }

    @Data
    @AllArgsConstructor
    public static class NestedItem {
        private String field;
        private Integer value;
    }

    @Data
    @AllArgsConstructor
    public static class ChildEntity {
        private Long childId;
        private String childName;
    }

}


