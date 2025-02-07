package local.ateng.java.mongo;

import cn.hutool.core.date.LocalDateTimeUtil;
import com.mongoplus.model.PageResult;
import local.ateng.java.mongo.entity.MyUser;
import local.ateng.java.mongo.init.InitData;
import local.ateng.java.mongo.service.MyUserService;
import lombok.RequiredArgsConstructor;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.List;

@SpringBootTest
@RequiredArgsConstructor(onConstructor = @__(@Autowired))
public class MongoPlusTests {
    private final MyUserService myUserService;

    @Test
    public void saveBatch() {
        // 批量写入数据
        Boolean result = myUserService.saveBatch(InitData.list);
        System.out.println(result);
    }

    @Test
    public void list() {
        // 查询数据列表
        List<MyUser> list = myUserService.list();
        System.out.println(list);
    }

    @Test
    public void listByLike() {
        // 模糊查询
        List<MyUser> list = myUserService.lambdaQuery()
                .like(MyUser::getProvince, "重庆")
                .orderByAsc(MyUser::getCreateTime)
                .list();
        System.out.println(list);
    }

    @Test
    public void listByTime() {
        // 时间范围查询
        List<MyUser> list = myUserService.lambdaQuery()
                .between(MyUser::getBirthday, LocalDateTimeUtil.parse("1990-01-01 00:00:00", "yyyy-MM-dd HH:mm:ss"), LocalDateTimeUtil.parse("2000-01-01 00:00:00", "yyyy-MM-dd HH:mm:ss"), true)
                .orderByAsc(MyUser::getCreateTime)
                .list();
        System.out.println(list);
    }

    @Test
    public void page() {
        // 分页查询
        PageResult<MyUser> page = myUserService.lambdaQuery()
                .orderByDesc(MyUser::getCreateTime)
                .page(1, 20);
        System.out.println(page);
    }

}
