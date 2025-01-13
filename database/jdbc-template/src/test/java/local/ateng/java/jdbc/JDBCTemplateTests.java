package local.ateng.java.jdbc;

import local.ateng.java.jdbc.entity.MyUser;
import lombok.RequiredArgsConstructor;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;

import java.util.List;
import java.util.Map;

@SpringBootTest
@RequiredArgsConstructor(onConstructor = @__(@Autowired))
public class JDBCTemplateTests {
    private final JdbcTemplate jdbcTemplate;

    @Test
    public void test() {
        String sql = "SELECT * FROM my_user";
        List<Map<String, Object>> list = jdbcTemplate.queryForList(sql);
        System.out.println(list);
    }

    @Test
    public void test01() {
        // 查询
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

    @Test
    public void test02() {
        // 更新
        String sql = "UPDATE my_user SET name = ? WHERE id = ?";
        int update = jdbcTemplate.update(sql, "阿腾", "1");
        System.out.println("更新行数：" + update);
    }

}
