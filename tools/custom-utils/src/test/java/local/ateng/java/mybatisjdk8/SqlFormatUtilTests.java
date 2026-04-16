package local.ateng.java.mybatisjdk8;

import com.github.vertical_blank.sqlformatter.core.FormatConfig;
import com.github.vertical_blank.sqlformatter.languages.Dialect;
import local.ateng.java.customutils.utils.SqlFormatUtil;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * SqlFormatUtil 工具类测试。
 *
 * @author Ateng
 * @since 2026-04-16
 */
public class SqlFormatUtilTests {

    private static final String SQL_1 =
            "select id, user_name, created_time from sys_user where status = 1 and age > 18 order by created_time desc";

    private static final String SQL_2 =
            "select * from t_order where order_id = ? and order_name = ?";

    private static final String SQL_3 =
            "select * from t_user where name = :name and age >= :age";

    private static final String SQL_4 =
            "select a.id, a.name, b.role_name from sys_user a left join sys_role b on a.role_id = b.id where a.deleted = 0";

    private static final String SQL_MULTI =
            "select * from t1; select * from t2;";

    /**
     * 默认格式化
     */
    @Test
    public void format_default() {
        printTitle("默认格式化");
        print(SqlFormatUtil.format(SQL_1));
        /*
        select
          id,
          user_name,
          created_time
        from
          sys_user
        where
          status = 1
          and age > 18
        order by
          created_time desc
         */
    }

    /**
     * 指定缩进
     */
    @Test
    public void format_withIndent() {
        printTitle("指定缩进");
        print(SqlFormatUtil.format(SQL_1, "  "));
        /*
        select
          id,
          user_name,
          created_time
        from
          sys_user
        where
          status = 1
          and age > 18
        order by
          created_time desc
         */
    }

    /**
     * 使用 FormatConfig
     */
    @Test
    public void format_withConfig() {
        printTitle("FormatConfig");

        FormatConfig config = FormatConfig.builder()
                .indent("  ")
                .uppercase(true)
                .linesBetweenQueries(2)
                .maxColumnLength(40)
                .build();

        print(SqlFormatUtil.format(SQL_4, config));
        /*
        SELECT
          a.id,
          a.name,
          b.role_name
        FROM
          sys_user a
          LEFT JOIN sys_role b ON a.role_id = b.id
        WHERE
          a.deleted = 0
         */
    }

    /**
     * List 参数替换
     */
    @Test
    public void format_listParams() {
        printTitle("List 参数替换");

        List<Object> params = Arrays.asList("SO-001", "测试订单");
        print(SqlFormatUtil.format(SQL_2, params));
        /*
        select
          *
        from
          t_order
        where
          order_id = SO-001
          and order_name = 测试订单
         */
    }

    /**
     * List 参数 + 缩进
     */
    @Test
    public void format_listParams_withIndent() {
        printTitle("List 参数 + 缩进");

        List<Object> params = Arrays.asList("SO-002", "订单B");
        print(SqlFormatUtil.format(SQL_2, "    ", params));
        /*
        select
            *
        from
            t_order
        where
            order_id = SO-002
            and order_name = 订单B
         */
    }

    /**
     * Map 参数替换
     * ！！！有问题，无法替换
     */
    @Test
    public void format_mapParams() {
        printTitle("Map 参数替换");

        Map<String, Object> params = new HashMap<>();
        params.put("name", "Ateng");
        params.put("age", 18);

        print(SqlFormatUtil.format(SQL_3, params));
        /*
        select
          *
        from
          t_user
        where
          name =: name
          and age >=: age
         */
    }

    /**
     * Map 参数 + 缩进
     * ！！！有问题，无法替换
     */
    @Test
    public void format_mapParams_withIndent() {
        printTitle("Map 参数 + 缩进");

        Map<String, Object> params = new HashMap<>();
        params.put("name", "Ateng");
        params.put("age", 25);

        print(SqlFormatUtil.format(SQL_3, "  ", params));
        /*
        select
          *
        from
          t_user
        where
          name =: name
          and age >=: age
         */
    }

    /**
     * 指定 Dialect
     */
    @Test
    public void format_withDialect() {
        printTitle("MySQL 方言");
        print(SqlFormatUtil.format(SQL_4, Dialect.MySql));
        /*
        select
          a.id,
          a.name,
          b.role_name
        from
          sys_user a
          left join sys_role b on a.role_id = b.id
        where
          a.deleted = 0
         */
    }

    /**
     * 方言 + FormatConfig
     */
    @Test
    public void format_dialect_withConfig() {
        printTitle("方言 + FormatConfig");

        FormatConfig config = FormatConfig.builder()
                .indent("    ")
                .uppercase(true)
                .build();

        print(SqlFormatUtil.format(SQL_4, Dialect.MySql, config));
        /*
        SELECT
            a.id,
            a.name,
            b.role_name
        FROM
            sys_user a
            LEFT JOIN sys_role b ON a.role_id = b.id
        WHERE
            a.deleted = 0
         */
    }

    /**
     * 字符串方言 + FormatConfig
     */
    @Test
    public void format_dialectName_withConfig() {
        printTitle("字符串方言 + FormatConfig");

        FormatConfig config = FormatConfig.builder()
                .indent("  ")
                .uppercase(false)
                .build();

        print(SqlFormatUtil.format(SQL_4, "mysql", config));
        /*
        select
          a.id,
          a.name,
          b.role_name
        from
          sys_user a
          left join sys_role b on a.role_id = b.id
        where
          a.deleted = 0
         */
    }

    /**
     * 多 SQL 语句
     */
    @Test
    public void format_multiSql() {
        printTitle("多 SQL 语句");

        print(SqlFormatUtil.format(SQL_MULTI));
        /*
        select
          *
        from
          t1;
        select
          *
        from
          t2;
         */
    }

    /**
     * 空 SQL
     */
    @Test
    public void format_blankSql() {
        printTitle("空 SQL");

        print(SqlFormatUtil.format("   "));
        //
    }

    /**
     * null SQL
     */
    @Test
    public void format_nullSql() {
        printTitle("null SQL");

        print(SqlFormatUtil.format(null));
        // null
    }

    /**
     * 复杂 SQL
     */
    @Test
    public void format_complexSql() {
        printTitle("复杂 SQL");

        String sql = "select u.id,u.name,(select count(1) from t_order o where o.user_id=u.id) as order_count from sys_user u where u.status=1 and exists(select 1 from t_role r where r.id=u.role_id)";
        print(SqlFormatUtil.format(sql));
        /*
        select
          u.id,
          u.name,
          (
            select
              count(1)
            from
              t_order o
            where
              o.user_id = u.id
          ) as order_count
        from
          sys_user u
        where
          u.status = 1
          and exists(
            select
              1
            from
              t_role r
            where
              r.id = u.role_id
          )
         */
    }

    // ==================== 工具方法 ====================

    private void printTitle(String title) {
        System.out.println();
        System.out.println("========== " + title + " ==========");
    }

    private void print(String result) {
        System.out.println(result);
    }
}