package io.github.atengk.sqlformat;

import com.github.vertical_blank.sqlformatter.SqlFormatter;
import com.github.vertical_blank.sqlformatter.core.FormatConfig;
import com.github.vertical_blank.sqlformatter.languages.Dialect;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

/**
 * SQL Formatter 功能测试。
 *
 * @author Ateng
 * @since 2026-04-16
 */
public class SqlformatterTests {

    private static final String SQL_1 =
            "select id, user_name, created_time from sys_user where status = 1 and age > 18 order by created_time desc";

    private static final String SQL_2 =
            "select * from t_order where order_id = ? and order_name = ?";

    private static final String SQL_3 =
            "select * from t_user where name = :name and age >= :age";

    private static final String SQL_4 =
            "select a.id, a.name, b.role_name from sys_user a left join sys_role b on a.role_id = b.id where a.deleted = 0";

    @Test
    public void defaultFormatTest() {
        printTitle("默认格式化");
        printResult(SqlFormatter.format(SQL_1));
    }

    @Test
    public void formatWithIndentTest() {
        printTitle("指定缩进格式化");
        printResult(SqlFormatter.format(SQL_1, "    "));
    }

    @Test
    public void formatWithConfigTest() {
        printTitle("使用 FormatConfig 格式化");

        String result = SqlFormatter.format(SQL_4,
                FormatConfig.builder()
                        .indent("  ")
                        .uppercase(true)
                        .linesBetweenQueries(2)
                        .maxColumnLength(40)
                        .skipWhitespaceNearBlockParentheses(true)
                        .build());

        printResult(result);
    }

    @Test
    public void formatWithListParamsTest() {
        printTitle("占位符替换 - List 参数");

        String result = SqlFormatter.format(
                SQL_2,
                Arrays.asList("SO-20260416-001", "测试订单")
        );

        printResult(result);
    }

    @Test
    public void formatWithListParamsAndIndentTest() {
        printTitle("占位符替换 - List 参数 + 指定缩进");

        String result = SqlFormatter.format(
                SQL_2,
                "    ",
                Arrays.asList("SO-20260416-001", "测试订单")
        );

        printResult(result);
    }

    @Test
    public void formatWithMapParamsTest() {
        printTitle("占位符替换 - Map 参数");

        Map<String, Object> params = new HashMap<String, Object>();
        params.put("name", "Ateng");
        params.put("age", 18);

        String result = SqlFormatter.format(SQL_3, params);
        printResult(result);
    }

    @Test
    public void formatWithMapParamsAndIndentTest() {
        printTitle("占位符替换 - Map 参数 + 指定缩进");

        Map<String, Object> params = new HashMap<String, Object>();
        params.put("name", "Ateng");
        params.put("age", 18);

        String result = SqlFormatter.format(SQL_3, "    ", params);
        printResult(result);
    }

    @Test
    public void standardDialectTest() {
        printTitle("标准方言");
        printResult(SqlFormatter.of(Dialect.StandardSql).format(SQL_1));
    }

    @Test
    public void mysqlDialectTest() {
        printTitle("MySQL 方言");
        printResult(SqlFormatter.of(Dialect.MySql).format(SQL_4));
    }

    @Test
    public void postgresqlDialectTest() {
        printTitle("PostgreSQL 方言");
        printResult(SqlFormatter.of(Dialect.PostgreSql).format(SQL_4));
    }

    @Test
    public void plSqlDialectTest() {
        printTitle("PL/SQL 方言");
        printResult(SqlFormatter.of(Dialect.PlSql).format(SQL_4));
    }

    @Test
    public void sparkSqlDialectTest() {
        printTitle("Spark SQL 方言");
        printResult(SqlFormatter.of(Dialect.SparkSql).format(SQL_4));
    }

    @Test
    public void stringDialectTest() {
        printTitle("通过字符串指定方言");
        printResult(SqlFormatter.of("mysql").format(SQL_1));
    }

    @Test
    public void extendFormatterTest() {
        printTitle("扩展格式器");

        String result = SqlFormatter
                .of(Dialect.MySql)
                .extend(cfg -> cfg.plusOperators("=>"))
                .format("select * from t where a => 4");

        printResult(result);
    }

    @Test
    public void unsupportedDialectTest() {
        printTitle("非法方言处理");

        try {
            SqlFormatter.of("not-exists").format(SQL_1);
        } catch (Exception e) {
            printResult("异常信息: " + e.getMessage());
        }
    }

    private void printTitle(String title) {
        System.out.println();
        System.out.println("========== " + title + " ==========");
    }

    private void printResult(String result) {
        System.out.println(result);
    }
}