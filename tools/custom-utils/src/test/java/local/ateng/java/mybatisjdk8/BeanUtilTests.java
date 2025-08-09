package local.ateng.java.mybatisjdk8;

import local.ateng.java.customutils.entity.MyUser;
import local.ateng.java.customutils.entity.MyUser1;
import local.ateng.java.customutils.entity.MyUser2;
import local.ateng.java.customutils.utils.BeanUtil;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Arrays;

public class BeanUtilTests {

    @Test
    void testBeanUtil() {
        MyUser2 myUser2 = new MyUser2();
        BeanUtil.copyProperties(createMyUser1Sample(), myUser2);
        System.out.println(myUser2);
        System.out.println(myUser2.getUserName());
        System.out.println(myUser2.getMyUserList().get(0).getId());
    }

    public static MyUser1 createMyUser1Sample() {
        MyUser user1 = new MyUser();
        user1.setId(1001L);
        user1.setUserName("alice");
        user1.setToday(LocalDate.now());
        user1.setCreateTime(LocalDateTime.now().minusDays(1));

        MyUser user2 = new MyUser();
        user2.setId(1002L);
        user2.setUserName("bob");
        user2.setToday(LocalDate.now().minusDays(2));
        user2.setCreateTime(LocalDateTime.now().minusHours(5));

        MyUser1 myUser1 = new MyUser1();
        myUser1.setId(1L);
        myUser1.setUserName("admin");
        myUser1.setToday(LocalDate.now());
        myUser1.setCreateTime(LocalDateTime.now());
        myUser1.setMyUserList(Arrays.asList(user1, user2));

        return myUser1;
    }

}
