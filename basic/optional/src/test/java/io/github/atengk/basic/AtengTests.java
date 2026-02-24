package io.github.atengk.basic;

import io.github.atengk.basic.entity.MyUser;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Optional;

/**
 * Optional 企业级常用示例
 *
 * @author ateng
 */
public class AtengTests {

    /**
     * 构造一个完整用户
     */
    private MyUser buildUser() {
        return MyUser.builder()
                .id(1L)
                .name("Tony")
                .age(25)
                .email("tony@example.com")
                .phoneNumber("13800000000")
                .score(new BigDecimal("99.5"))
                .ratio(0.85)
                .province("东京")
                .city("新宿")
                .birthday(LocalDate.of(2000, 1, 1))
                .createTime(LocalDateTime.now())
                .build();
    }

    /**
     * 1️⃣ Optional 基础创建 & orElse
     */
    @Test
    void test01() {

        MyUser user = buildUser();

        // ofNullable：允许为空，推荐使用
        Optional<MyUser> optional = Optional.ofNullable(user);

        // map：安全获取 name
        String name = optional
                .map(MyUser::getName)
                .orElse("UNKNOWN");

        System.out.println("用户名: " + name);
    }

    /**
     * 2️⃣ 链式调用防止 NPE
     */
    @Test
    void test02() {

        MyUser user = null;

        // 即使 user 为 null，也不会 NPE
        String upperName = Optional.ofNullable(user)
                .map(MyUser::getName)
                .map(String::toUpperCase)
                .orElse("DEFAULT");

        System.out.println("大写用户名: " + upperName);
    }

    /**
     * 3️⃣ orElse vs orElseGet 区别
     */
    @Test
    void test03() {

        MyUser user = buildUser();

        // orElse：默认值会提前执行
        String city1 = Optional.ofNullable(user)
                .map(MyUser::getCity)
                .orElse(loadDefaultCity());

        // orElseGet：只有为空时才执行
        String city2 = Optional.ofNullable(user)
                .map(MyUser::getCity)
                .orElseGet(this::loadDefaultCity);

        System.out.println(city1);
        System.out.println(city2);
    }

    private String loadDefaultCity() {
        System.out.println("加载默认城市...");
        return "默认城市";
    }

    /**
     * 4️⃣ filter 条件过滤
     */
    @Test
    void test04() {

        MyUser user = buildUser();

        String result = Optional.ofNullable(user)
                .map(MyUser::getAge)
                .filter(age -> age >= 18) // 年龄过滤
                .map(age -> "成年人")
                .orElse("未成年");

        System.out.println(result);
    }

    /**
     * 5️⃣ orElseThrow 强制异常（企业级常用）
     */
    @Test
    void test05() {

        MyUser user = buildUser();

        String email = Optional.ofNullable(user)
                .map(MyUser::getEmail)
                .orElseThrow(() -> new IllegalArgumentException("邮箱不能为空"));

        System.out.println("邮箱: " + email);
    }

    /**
     * 6️⃣ ifPresent 使用
     */
    @Test
    void test06() {

        MyUser user = buildUser();

        Optional.ofNullable(user)
                .map(MyUser::getEmail)
                .ifPresent(email -> System.out.println("发送邮件到: " + email));
    }

    /**
     * 7️⃣ 处理 BigDecimal 默认值
     */
    @Test
    void test07() {

        MyUser user = new MyUser(); // score 为 null

        BigDecimal score = Optional.ofNullable(user)
                .map(MyUser::getScore)
                .orElse(BigDecimal.ZERO);

        System.out.println("分数: " + score);
    }

    /**
     * 8️⃣ flatMap 处理嵌套 Optional
     */
    @Test
    void test08() {

        MyUser user = buildUser();

        String email = Optional.ofNullable(user)
                .flatMap(this::getEmailOptional)
                .orElse("NO_EMAIL");

        System.out.println(email);
    }

    /**
     * 返回 Optional 的方法
     */
    private Optional<String> getEmailOptional(MyUser user) {
        return Optional.ofNullable(user.getEmail());
    }

}