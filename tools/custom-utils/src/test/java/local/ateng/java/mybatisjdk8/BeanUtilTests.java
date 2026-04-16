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
        // MyUser2(id=1, userName=admin, tmp=null, myUser0List=[MyUser0(id=1001, userName=alice, today=2026-04-16, createTime=2026-04-15T21:06:11.652720600), MyUser0(id=1002, userName=bob, today=2026-04-14, createTime=2026-04-16T16:06:11.652720600)])
        System.out.println(myUser2.getUserName());
        // admin
        System.out.println(myUser2.getMyUser0List().get(0).getId());
        // 1001
        myUser2.setId(0L);
        System.out.println(myUser1);
        // MyUser1(id=1, userName=admin, today=2026-04-16, createTime=2026-04-16T21:06:11.652720600, myUser0=MyUser0(id=1001, userName=alice, today=2026-04-16, createTime=2026-04-15T21:06:11.652720600), myUser0List=[MyUser0(id=1001, userName=alice, today=2026-04-16, createTime=2026-04-15T21:06:11.652720600), MyUser0(id=1002, userName=bob, today=2026-04-14, createTime=2026-04-16T16:06:11.652720600)])
        System.out.println(myUser2);
        // MyUser2(id=0, userName=admin, tmp=null, myUser0List=[MyUser0(id=1001, userName=alice, today=2026-04-16, createTime=2026-04-15T21:06:11.652720600), MyUser0(id=1002, userName=bob, today=2026-04-14, createTime=2026-04-16T16:06:11.652720600)])
    }

    @Test
    void copy() {
        MyUser2 myUser2 = new MyUser2();
        MyUser1 myUser1 = createMyUser1Sample();
        BeanUtil.copy(myUser1, myUser2);
        System.out.println(myUser2);
        // MyUser2(id=1, userName=admin, tmp=null, myUser0List=[MyUser0(id=1001, userName=alice, today=2026-04-16, createTime=2026-04-15T21:07:06.802893700), MyUser0(id=1002, userName=bob, today=2026-04-14, createTime=2026-04-16T16:07:06.802893700)])
        System.out.println(myUser2.getUserName());
        // admin
        System.out.println(myUser2.getMyUser0List().get(0).getId());
        // 1001
        myUser2.setId(0L);
        System.out.println(myUser1);
        // MyUser1(id=1, userName=admin, today=2026-04-16, createTime=2026-04-16T21:07:06.802893700, myUser0=MyUser0(id=1001, userName=alice, today=2026-04-16, createTime=2026-04-15T21:07:06.802893700), myUser0List=[MyUser0(id=1001, userName=alice, today=2026-04-16, createTime=2026-04-15T21:07:06.802893700), MyUser0(id=1002, userName=bob, today=2026-04-14, createTime=2026-04-16T16:07:06.802893700)])
        System.out.println(myUser2);
        // MyUser2(id=0, userName=admin, tmp=null, myUser0List=[MyUser0(id=1001, userName=alice, today=2026-04-16, createTime=2026-04-15T21:07:06.802893700), MyUser0(id=1002, userName=bob, today=2026-04-14, createTime=2026-04-16T16:07:06.802893700)])
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
        // MyUser2(id=null, userName=null, tmp=null, myUser0List=[MyUser0(id=1001, userName=alice, today=2026-04-16, createTime=2026-04-15T21:05:44.663116500), MyUser0(id=1002, userName=bob, today=2026-04-14, createTime=2026-04-16T16:05:44.663116500)])
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
        // BeanUtilTests.C(a=A, b=B, c=null)
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
        // MyUser1(id=1, userName=admin, today=2026-04-16, createTime=2026-04-16T21:08:02.973731, myUser0=MyUser0(id=1001, userName=alice, today=2026-04-16, createTime=2026-04-15T21:08:02.972734400), myUser0List=[MyUser0(id=1001, userName=alice, today=2026-04-16, createTime=2026-04-15T21:08:02.972734400), MyUser0(id=1002, userName=bob, today=2026-04-14, createTime=2026-04-16T16:08:02.972734400)])
        System.out.println(map);
        // {id=1, userName=admin, today=2026-04-16, createTime=2026-04-16T21:08:02.973731, myUser0={id=1001, userName=alice, today=2026-04-16, createTime=2026-04-15T21:08:02.972734400}, myUser0List=[{id=1001, userName=alice, today=2026-04-16, createTime=2026-04-15T21:08:02.972734400}, {id=1002, userName=bob, today=2026-04-14, createTime=2026-04-16T16:08:02.972734400}]}
    }

    @Test
    void getProperty() {
        MyUser1 myUser1 = createMyUser1Sample();
        String userName = BeanUtil.getProperty(myUser1, "userName");
        System.out.println(userName);
        // admin
        System.out.println(userName.getClass());
        // class java.lang.String
    }

    @Test
    void setProperty() {
        MyUser1 myUser1 = createMyUser1Sample();
        BeanUtil.setProperty(myUser1, "userName", "alice");
        String userName = BeanUtil.getProperty(myUser1, "userName");
        System.out.println(userName);
        //alice
        System.out.println(userName.getClass());
        //class java.lang.String
    }

    @Test
    void getAllFieldNames() {
        List<String> allFieldNames = BeanUtil.getAllFieldNames(MyUser1.class);
        System.out.println(allFieldNames);
        // [id, userName, today, createTime, myUser0, myUser0List]
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
        //{id=1, status=进行中}

    }

    @Test
    void desensitize() {
        MyUser1 myUser1 = createMyUser1Sample();
        Map<String, Object> map = BeanUtil.toDesensitizedMap(myUser1 , Arrays.asList("userName", "createTime", "myUser0","myUser0List"), "*");
        System.out.println(myUser1);
        //MyUser1(id=1, userName=admin, today=2026-04-16, createTime=2026-04-16T21:11:21.566555700, myUser0=MyUser0(id=1001, userName=alice, today=2026-04-16, createTime=2026-04-15T21:11:21.565558600), myUser0List=[MyUser0(id=1001, userName=alice, today=2026-04-16, createTime=2026-04-15T21:11:21.565558600), MyUser0(id=1002, userName=bob, today=2026-04-14, createTime=2026-04-16T16:11:21.566555700)])
        System.out.println(map);
        //{id=1, userName=*, today=2026-04-16, createTime=*, myUser0={id=*, userName=*, today=*, createTime=*}, myUser0List=[{id=*, userName=*, today=*, createTime=*}, {id=*, userName=*, today=*, createTime=*}]}
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
        //BeanUtilTests.SourceEntity(username=blair, email=blair@example.com, age=30, userId=1001, active=true, createTime=2026-04-16T21:12:07.030866, balance=1234.56)
        System.out.println(target);
        //BeanUtilTests.TargetEntity(username=blair, email=blair@example.com, age=30, userId=1001, active=true, createTime=2026-04-16T21:12:07.030866, balance=1234.56)
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
        //Source: BeanUtilTests.SourceEntity2(username=blair, email=blair@example.com, age=30, userId=1001, active=true, createTime=2026-04-16T21:12:49.373384600, balance=1234.56, status=ACTIVE, meta={role=admin, dept=IT}, tags=[java, backend, spring], nestedObject=BeanUtilTests.NestedObject(field1=nestedField1, field2=999))
        System.out.println("Target: " + target);
        //Target: BeanUtilTests.TargetEntity2(username=blair, email=blair@example.com, age=30, userId=1001, active=true, createTime=2026-04-16T21:12:49.373384600, balance=1234.56, status=ACTIVE, meta={role=admin, dept=IT}, tags=[java, backend, spring], nestedObject=BeanUtilTests.NestedObject(field1=nestedField1, field2=999))
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
        //Source: BeanUtilTests.ParentSource(id=101, name=Parent1, amount=500.75, createdAt=2026-04-16T21:13:19.803112500, nestedItems=[BeanUtilTests.NestedItem(field=nested1, value=10), BeanUtilTests.NestedItem(field=nested2, value=20)], children=[BeanUtilTests.ChildEntity(childId=201, childName=child1), BeanUtilTests.ChildEntity(childId=202, childName=child2)])
        System.out.println("Target: " + parentTarget);
        //Target: BeanUtilTests.ParentTarget(id=101, name=Parent1, amount=500.75, createdAt=2026-04-16T21:13:19.803112500, nestedItems=[BeanUtilTests.NestedItem(field=nested1, value=10), BeanUtilTests.NestedItem(field=nested2, value=20)], children=[BeanUtilTests.ChildEntity(childId=201, childName=child1), BeanUtilTests.ChildEntity(childId=202, childName=child2)])
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


