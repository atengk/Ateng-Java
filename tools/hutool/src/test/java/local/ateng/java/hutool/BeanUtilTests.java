package local.ateng.java.hutool;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.json.JSONObject;
import local.ateng.java.hutool.entity.UserInfoEntity;
import lombok.Data;
import org.junit.jupiter.api.Test;

import java.util.Date;
import java.util.Map;

/**
 * Bean工具类主要是针对这些setXXX和getXXX方法进行操作，比如将Bean对象转为Map等等。
 * https://www.hutool.cn/docs/#/core/JavaBean/Bean%E5%B7%A5%E5%85%B7-BeanUtil
 *
 * @author 孔余
 * @since 2024-03-29 14:08
 */
public class BeanUtilTests {
    /**
     * 是否为Bean对象
     */
    @Test
    void test01() {
        boolean isBean = BeanUtil.isBean(JSONObject.class);//true
        System.out.println(isBean);
    }

    /**
     * Bean转Bean
     */
    @Test
    void test02() {
        UserInfoEntity user = new UserInfoEntity();
        user.setId(1L);
        user.setName("阿腾");
        user.setAge(24);
        user.setScore(99.99D);
        user.setBirthday(new Date());
        user.setProvince("重庆");
        user.setCity("重庆");
        JSONObject json = new JSONObject();
        BeanUtil.copyProperties(user, json);
        System.out.println(json);
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
        BeanUtil.copyProperties(a, c);
        BeanUtil.copyProperties(b, c);
        System.out.println(c);
    }
    @Data
    class A {
        private String a;
    }
    @Data
    class B {
        private String b;
    }
    @Data
    class C {
        private String a;
        private String b;
        private String c;
    }

    /**
     * Bean转为Map
     */
    @Test
    void test03() {
        UserInfoEntity user = new UserInfoEntity();
        user.setId(1L);
        user.setName("阿腾");
        user.setAge(24);
        user.setScore(99.99D);
        user.setBirthday(new Date());
        user.setProvince("重庆");
        user.setCity("重庆");
        Map<String, Object> map = BeanUtil.beanToMap(user);
        System.out.println(map);
    }
}
