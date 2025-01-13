package local.ateng.java.mybatis;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import local.ateng.java.mybatis.entity.MyUser;
import local.ateng.java.mybatis.service.IMyOrderService;
import local.ateng.java.mybatis.service.IMyUserService;
import lombok.RequiredArgsConstructor;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.List;
import java.util.Map;


/**
 * 基础查询
 *
 * @author 孔余
 * @email 2385569970@qq.com
 * @since 2025-01-10
 */
@SpringBootTest
@RequiredArgsConstructor(onConstructor = @__(@Autowired))
public class BasicQueryTests {
    private final IMyUserService myUserService;
    private final IMyOrderService myOrderService;

    @Test
    void test() {
        long count = myUserService.count();
        System.out.println(count);
    }

    @Test
    void test01() {
        // 查询id是88的一条数据
        MyUser user = myUserService.lambdaQuery()
                .eq(MyUser::getId, 88)
                .one();
        System.out.println(user);
    }

    @Test
    void test02() {
        // 查询id是88到90(包含)这个范围内的数据
        List<MyUser> userList = myUserService.lambdaQuery()
                .between(MyUser::getId, 88, 90)
                .list();
        System.out.println(userList);
    }

    @Test
    void test03() {
        // 查询所有的区域
        List<MyUser> list = myUserService.query()
                .select("DISTINCT city")
                .list();
        System.out.println(list.stream().map(MyUser::getCity).toList());
    }

    @Test
    void test04() {
        // 查询创建时间是2024年8月的数据数量
        QueryWrapper<MyUser> wrapper = new QueryWrapper<MyUser>()
                .select("DATE_FORMAT(create_time, '%Y-%m') as month", "COUNT(*) as count")
                .groupBy("DATE_FORMAT(create_time, '%Y-%m')")
                .having("month = '2025-01'");
        List<Map<String, Object>> list = myUserService.listMaps(wrapper);
        System.out.println(list);
    }

    @Test
    void test05() {
        // 查询并按照创建时间排序(降序)，创建时间一样则按照id排序(降序)
        List<MyUser> userList = myUserService.lambdaQuery()
                .between(MyUser::getId, 88, 90)
                .orderByDesc(MyUser::getCreateTime, MyUser::getId)
                .list();
        System.out.println(userList);
    }

    @Test
    void test06() {
        // 引入 MyBatis-Plus 分页插件
        Page<MyUser> page = new Page<>(2, 10);  // 第2页，每页10条记录
        // 分页查询
        Page<MyUser> userPage = myUserService.lambdaQuery()
                .between(MyUser::getId, 88, 888)
                .page(page);
        // 获取分页结果
        List<MyUser> users = userPage.getRecords();  // 分页数据
        long total = userPage.getTotal();  // 总记录数
        long pages = userPage.getPages();  // 总页数
        // 输出查询结果
        System.out.println(userPage);
        System.out.println("Total: " + total);
        System.out.println("Pages: " + pages);
        users.forEach(user -> System.out.println(user));
    }

}
