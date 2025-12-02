package local.ateng.java.jdbc;

import local.ateng.java.jdbc.entity.MyUser;
import lombok.RequiredArgsConstructor;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.BeanPropertyRowMapper;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;

import java.sql.PreparedStatement;
import java.sql.Statement;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

@SpringBootTest
@RequiredArgsConstructor(onConstructor = @__(@Autowired))
public class JDBCTemplateTests {

    private final JdbcTemplate jdbcTemplate;

    /**
     * 查询多行，返回 Map 列表
     */
    @Test
    public void testQueryListMap() {
        String sql = "SELECT * FROM my_user";
        List<Map<String, Object>> list = jdbcTemplate.queryForList(sql);
        System.out.println(list);
    }

    /**
     * 查询多行，RowMapper 手动映射为实体类
     */
    @Test
    public void testQueryListRowMapper() {
        String sql = "SELECT * FROM my_user";
        List<MyUser> list = jdbcTemplate.query(sql, (rs, rowNum) -> {
            MyUser user = new MyUser();
            user.setId(rs.getLong("id"));
            user.setName(rs.getString("name"));
            user.setAge(rs.getInt("age"));
            user.setScore(rs.getDouble("score"));
            user.setBirthday(rs.getDate("birthday"));
            user.setProvince(rs.getString("province"));
            user.setCity(rs.getString("city"));
            return user;
        });
        System.out.println(list);
    }

    /**
     * 使用 BeanPropertyRowMapper 自动映射为实体类
     * 字段名需与数据库字段一致或能映射
     */
    @Test
    public void testBeanPropertyRowMapper() {
        String sql = "SELECT * FROM my_user";
        List<MyUser> list = jdbcTemplate.query(
                sql,
                new BeanPropertyRowMapper<>(MyUser.class)
        );
        System.out.println(list);
    }

    /**
     * 查询单个对象（queryForObject）
     */
    @Test
    public void testQueryForObject() {
        String sql = "SELECT name FROM my_user WHERE id = ?";
        String name = jdbcTemplate.queryForObject(sql, String.class, 1L);
        System.out.println("用户名：" + name);
    }

    /**
     * 查询单行 Map（queryForMap）
     */
    @Test
    public void testQueryForMap() {
        String sql = "SELECT * FROM my_user WHERE id = ?";
        Map<String, Object> result = jdbcTemplate.queryForMap(sql, 1L);
        System.out.println(result);
    }

    /**
     * 查询单值：例如 count(*)
     */
    @Test
    public void testQueryForSingleValue() {
        String sql = "SELECT COUNT(1) FROM my_user";
        Integer count = jdbcTemplate.queryForObject(sql, Integer.class);
        System.out.println("总数：" + count);
    }

    /**
     * 判断某行是否存在
     */
    @Test
    public void testExists() {
        String sql = "SELECT COUNT(1) FROM my_user WHERE id = ?";
        Integer count = jdbcTemplate.queryForObject(sql, Integer.class, 1L);
        boolean exists = count != null && count > 0;
        System.out.println("是否存在：" + exists);
    }

    /**
     * 简单更新
     */
    @Test
    public void testUpdate() {
        String sql = "UPDATE my_user SET name = ? WHERE id = ?";
        int update = jdbcTemplate.update(sql, "阿腾", 1L);
        System.out.println("更新行数：" + update);
    }

    /**
     * 插入数据并返回主键
     */
    @Test
    public void testInsertReturnKey() {
        String sql = "INSERT INTO my_user(name, age, score) VALUES (?, ?, ?)";
        KeyHolder keyHolder = new GeneratedKeyHolder();

        jdbcTemplate.update(connection -> {
            PreparedStatement ps = connection.prepareStatement(
                    sql,
                    Statement.RETURN_GENERATED_KEYS
            );
            ps.setString(1, "小明");
            ps.setInt(2, 20);
            ps.setDouble(3, 88.5);
            return ps;
        }, keyHolder);

        Number key = keyHolder.getKey();
        System.out.println("生成的主键：" + key);
    }

    /**
     * 删除数据
     */
    @Test
    public void testDelete() {
        String sql = "DELETE FROM my_user WHERE id = ?";
        int delete = jdbcTemplate.update(sql, 10L);
        System.out.println("删除行数：" + delete);
    }

    /**
     * 批量更新（批处理）
     */
    @Test
    public void testBatchUpdate() {
        String sql = "UPDATE my_user SET age = ? WHERE id = ?";
        List<Object[]> batchArgs = Arrays.asList(
                new Object[]{20, 1L},
                new Object[]{22, 2L},
                new Object[]{30, 3L}
        );

        int[] results = jdbcTemplate.batchUpdate(sql, batchArgs);
        System.out.println("批量结果：" + Arrays.toString(results));
    }

    /**
     * 批量插入
     */
    @Test
    public void testBatchInsert() {
        String sql = "INSERT INTO my_user(name, age, score) VALUES (?, ?, ?)";
        List<Object[]> params = Arrays.asList(
                new Object[]{"用户A", 18, 90.5},
                new Object[]{"用户B", 20, 86.0},
                new Object[]{"用户C", 25, 95.0}
        );

        int[] results = jdbcTemplate.batchUpdate(sql, params);
        System.out.println("批量插入：" + Arrays.toString(results));
    }

    @Test
    public void testPagination() {
        // ---------- 分页参数 ----------
        int page = 1; // 页码（从1开始）
        int size = 5; // 每页条数
        int offset = (page - 1) * size;

        // ---------- 查询条件 ----------
        String sqlBase = "SELECT id, name, age, score, birthday, province, city FROM my_user WHERE age > ?";
        Object[] params = new Object[]{10};

        // ---------- 统计总数 ----------
        String sqlCount = "SELECT COUNT(1) FROM (" + sqlBase + ") AS temp_count";
        Long total = jdbcTemplate.queryForObject(sqlCount, params, Long.class);
        System.out.println("总条数：" + total);

        // ---------- 分页查询 ----------
        String sqlPage = sqlBase + " LIMIT ? OFFSET ?";
        Object[] pageParams = Arrays.copyOf(params, params.length + 2);
        pageParams[params.length] = size;
        pageParams[params.length + 1] = offset;

        List<MyUser> pageList = jdbcTemplate.query(sqlPage, pageParams, new BeanPropertyRowMapper<>(MyUser.class));

        // ---------- 输出分页结果 ----------
        System.out.println("当前页码：" + page);
        System.out.println("每页条数：" + size);
        System.out.println("分页数据：" + pageList);
    }


}
