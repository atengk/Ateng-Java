package io.github.atengk.basic.dao;

import io.github.atengk.basic.holder.PageContextHolder;
import org.springframework.stereotype.Repository;

import java.util.ArrayList;
import java.util.List;

/**
 * 示例 DAO（模拟分页查询）
 *
 * @author Ateng
 * @since 2026-04-10
 */
@Repository
public class UserDao {

    /**
     * 查询用户列表（自动分页）
     */
    public List<String> queryUsers() {

        int offset = PageContextHolder.getOffset();
        int limit = PageContextHolder.getLimit();

        // 模拟 SQL
        String sql = "SELECT * FROM user LIMIT " + offset + ", " + limit;
        System.out.println("执行SQL：" + sql);

        // 模拟返回数据
        List<String> list = new ArrayList<>();
        for (int i = 1; i <= limit; i++) {
            list.add("User_" + (offset + i));
        }

        return list;
    }
}
