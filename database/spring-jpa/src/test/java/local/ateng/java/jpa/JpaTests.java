package local.ateng.java.jpa;

import local.ateng.java.jpa.entity.MyUser;
import local.ateng.java.jpa.service.MyUserService;
import lombok.RequiredArgsConstructor;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

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


}
