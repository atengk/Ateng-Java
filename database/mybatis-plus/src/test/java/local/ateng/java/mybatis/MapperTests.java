package local.ateng.java.mybatis;

import com.alibaba.fastjson2.JSONObject;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import local.ateng.java.mybatis.entity.MyJson;
import local.ateng.java.mybatis.entity.MyUser;
import local.ateng.java.mybatis.mapper.MyJsonMapper;
import local.ateng.java.mybatis.mapper.MyUserMapper;
import lombok.RequiredArgsConstructor;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@SpringBootTest
@RequiredArgsConstructor(onConstructor = @__(@Autowired))
public class MapperTests {
    private final MyUserMapper myUserMapper;
    private final MyJsonMapper myJsonMapper;

    @Test
    void test01() {
        List<MyUser> list = myUserMapper.selectAllUsers();
        System.out.println(list);
    }

    @Test
    void test02() {
        MyUser myUser = myUserMapper.selectUserById(1L);
        System.out.println(myUser);
    }

    @Test
    void test03() {
        List<JSONObject> list = myUserMapper.selectUsersWithOrders(1L);
        System.out.println(list);
    }

    @Test
    void test04() {
        List<MyJson> myJsons = myJsonMapper.selectMyJson();
        System.out.println(myJsons);
    }

    @Test
    void test05() {
        IPage<JSONObject> page = myUserMapper.selectUsersWithOrderPage(new Page(1, 3), "重");
        System.out.println(page);
    }

    @Test
    void test06() {
        QueryWrapper<MyUser> wrapper = new QueryWrapper<>();
        wrapper.like("city", "重");
        wrapper.eq("u.id", 1);
        wrapper.orderByAsc("u.id");
        IPage page = new Page(1, 3);
        page = myUserMapper.selectUsersWithOrderPageWrapper(page, wrapper);
        System.out.println(page);
    }

    @Test
    void test07() {
        LambdaQueryWrapper<MyUser> wrapper = Wrappers.lambdaQuery();
        wrapper.like(MyUser::getCity, "重");
        IPage<JSONObject> page = new Page(1, 3);
        page = myUserMapper.selectUsersWithOrderPageWrapper(page, wrapper);
        System.out.println(page);
    }

    @Test
    void test08() {
        LambdaQueryWrapper<MyUser> wrapper = Wrappers.lambdaQuery();
        wrapper.like(MyUser::getCity, "重");
        IPage<JSONObject> page = new Page(1, 3);
        Map<String, Object> map = new HashMap<>();
        map.put("_id", 1);
        page = myUserMapper.selectUsersWithOrderPageWrapperQuery(page, wrapper, map);
        System.out.println(page);
    }

}
