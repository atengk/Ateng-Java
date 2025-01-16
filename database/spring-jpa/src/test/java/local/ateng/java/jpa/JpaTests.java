package local.ateng.java.jpa;

import local.ateng.java.jpa.entity.MyUser;
import local.ateng.java.jpa.service.MyUserService;
import lombok.RequiredArgsConstructor;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@SpringBootTest
@RequiredArgsConstructor(onConstructor = @__(@Autowired))
public class JpaTests {
    private final MyUserService myUserService;

    @Test
    public void test01() {
        // 保存数据
        myUserService.save(new MyUser(null, "jpa", 24, 1.1, LocalDateTime.now(),"重庆","重庆",LocalDateTime.now()));
    }

    @Test
    public void test02() {
        // 查看数据
        List<MyUser> list = myUserService.findAll();
        System.out.println(list);
    }
    @Test
    public void test03() {
        // 根据id查询数据
        MyUser myUser = myUserService.findById(1L);
        System.out.println(myUser);
    }

    @Test
    public void test04() {
        // 根据数据保存或更新数据
        myUserService.update(new MyUser(1L, "阿腾2", 24, 1.1, LocalDateTime.now(),"重庆","重庆",LocalDateTime.now()));
    }

    @Test
    public void test05() {
        // 删除数据
        myUserService.deleteById(1000L);
    }

    @Test
    public void test06() {
        // 批量保存数据
        ArrayList<MyUser> list = new ArrayList<>();
        list.add(new MyUser(null, "jpa1", 24, 1.1, LocalDateTime.now(),"重庆","重庆",LocalDateTime.now()));
        list.add(new MyUser(null, "jpa2", 24, 1.2, LocalDateTime.now(),"重庆","重庆",LocalDateTime.now()));
        myUserService.saveAll(list);
    }

    @Test
    public void test07() {
        // 清空表
        myUserService.truncate();
    }

    @Test
    public void test08() {
        // 自定义SQL查询，一个条件参数和返回一个值
        MyUser myUser = myUserService.findCustomOne(1L);
        System.out.println(myUser);
    }

    @Test
    public void test09() {
        // 自定义SQL查询，多个条件参数和返回列表
        List<MyUser> userList = myUserService.findCustomList("jpa%", 18);
        System.out.println(userList);
    }

}
