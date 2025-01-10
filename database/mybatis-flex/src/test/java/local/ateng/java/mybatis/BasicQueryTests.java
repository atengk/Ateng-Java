package local.ateng.java.mybatis;

import com.alibaba.fastjson2.JSON;
import com.mybatisflex.core.query.QueryColumn;
import com.mybatisflex.core.row.Row;
import local.ateng.java.mybatis.entity.MyUser;
import local.ateng.java.mybatis.service.MyOrderService;
import local.ateng.java.mybatis.service.MyUserService;
import local.ateng.java.mybatis.vo.MyUserOrderVo;
import local.ateng.java.mybatis.vo.MyUserVo;
import lombok.RequiredArgsConstructor;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.List;

import static com.mybatisflex.core.query.QueryMethods.*;
import static local.ateng.java.mybatis.entity.table.MyOrderTableDef.MY_ORDER;
import static local.ateng.java.mybatis.entity.table.MyUserTableDef.MY_USER;


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
    private final MyUserService myUserService;
    private final MyOrderService myOrderService;

    @Test
    void test() {
        long count = myUserService.count();
        System.out.println(count);
    }

    @Test
    void test01() {
        // 查询id是88的一条数据
        MyUser myUser = myUserService.queryChain()
                .where(MY_USER.ID.eq(88))
                .one();
        System.out.println(myUser);
    }

    @Test
    void test02() {
        // 查询id是88到90(包含)这个范围内的数据
        List<MyUser> userList = myUserService.queryChain()
                .where(MY_USER.ID.between(88, 90))
                .list();
        System.out.println(userList);
    }

    @Test
    void test03() {
        // 查询所有的区域
        // 常见的SQL函数：https://mybatis-flex.com/zh/base/querywrapper.html#select-function-sql-%E5%87%BD%E6%95%B0
        List<String> list = myUserService.queryChain()
                .select(distinct(MY_USER.CITY))
                .listAs(String.class);
        System.out.println(list);
    }

    @Test
    void test04() {
        // 查询创建时间是2024年8月的数据数量
        // 自定义字符串列名：https://mybatis-flex.com/zh/base/querywrapper.html#%E8%87%AA%E5%AE%9A%E4%B9%89%E5%AD%97%E7%AC%A6%E4%B8%B2%E5%88%97%E5%90%8D
        QueryColumn monthF = column("DATE_FORMAT(create_time, '%Y-%m')");
        QueryColumn month = column("month");
        List<Row> list = myUserService.queryChain()
                .select(monthF.as("month"), count().as("count"))
                .groupBy(monthF)
                .having(month.eq("2025-01"))
                .listAs(Row.class);
        System.out.println(list);
    }

    @Test
    void test05() {
        // 查询并按照创建时间排序(降序)，创建时间一样则按照id排序(降序)
        List<MyUser> userList = myUserService.queryChain()
                .where(MY_USER.ID.between(88, 90))
                .orderBy(MY_USER.CREATE_TIME.desc(), MY_USER.ID.desc())
                .list();
        System.out.println(userList);
    }

    @Test
    void test06() {
        // 关联查询
        List<MyUserOrderVo> list = myUserService.queryChain()
                .select(
                        // 要实现全映射需要手动写出所有字段
                        MY_USER.ALL_COLUMNS,
                        MY_ORDER.ID.as("order_id"),
                        MY_ORDER.DATE.as("order_date"),
                        MY_ORDER.TOTAL_AMOUNT
                )
                .from(MY_USER.as("u"))
                .where(MY_USER.ID.between(88, 90))
                .leftJoin(MY_ORDER.as("r"))
                .on(MY_USER.ID.eq(MY_ORDER.USER_ID))
                .listAs(MyUserOrderVo.class);
        System.out.println(JSON.toJSONString(list));
    }

    @Test
    void test07() {
        // 关联查询 一对多
        List<MyUserVo> list = myUserService.queryChain()
                .select(
                        // 要实现全映射需要手动写出所有字段
                        MY_USER.ID,
                        MY_USER.NAME,
                        MY_USER.AGE,
                        MY_USER.SCORE,
                        MY_USER.BIRTHDAY,
                        MY_USER.PROVINCE,
                        MY_USER.CITY,
                        MY_USER.CREATE_TIME,
                        MY_ORDER.ID,
                        MY_ORDER.USER_ID,
                        MY_ORDER.DATE,
                        MY_ORDER.TOTAL_AMOUNT
                )
                .from(MY_USER.as("u"))
                .where(MY_USER.ID.between(88, 90))
                .leftJoin(MY_ORDER.as("r"))
                .on(MY_USER.ID.eq(MY_ORDER.USER_ID))
                .listAs(MyUserVo.class);
        System.out.println(JSON.toJSONString(list));
    }

}
