package local.ateng.java.mybatisjdk8;

import net.sf.jsqlparser.JSQLParserException;
import net.sf.jsqlparser.expression.LongValue;
import net.sf.jsqlparser.expression.operators.relational.GreaterThan;
import net.sf.jsqlparser.parser.CCJSqlParserUtil;
import net.sf.jsqlparser.schema.Column;
import net.sf.jsqlparser.schema.Table;
import net.sf.jsqlparser.statement.Statement;
import net.sf.jsqlparser.statement.delete.Delete;
import net.sf.jsqlparser.statement.insert.Insert;
import net.sf.jsqlparser.statement.select.PlainSelect;
import net.sf.jsqlparser.statement.select.Select;
import net.sf.jsqlparser.statement.select.SelectItem;
import net.sf.jsqlparser.statement.update.Update;
import org.junit.jupiter.api.Test;

public class JSQLParserTest {
    /**
     * SELECT 解析与修改示例
     */
    @Test
    void parseSelect() throws Exception {
        System.out.println("=== SELECT 解析 ===");
        String sql = "SELECT id, name FROM user WHERE age > 20";
        Statement stmt = CCJSqlParserUtil.parse(sql);

        if (stmt instanceof Select) {
            Select select = (Select) stmt;
            PlainSelect plain = (PlainSelect) select.getSelectBody();

            // 表名
            Table table = (Table) plain.getFromItem();
            System.out.println("表名：" + table.getName());

            // 列名
            for (SelectItem item : plain.getSelectItems()) {
                System.out.println("列名：" + item);
            }

            // 修改 WHERE 条件
            GreaterThan gt = new GreaterThan();
            gt.setLeftExpression(new Column("age"));
            gt.setRightExpression(new LongValue(30));
            plain.setWhere(gt);

            System.out.println("修改后的 SQL：" + select.toString());
        }
        System.out.println();
    }

    /**
     * INSERT 解析示例
     */
    @Test
    void parseInsert() throws Exception {
        System.out.println("=== INSERT 解析 ===");
        String sql = "INSERT INTO user(id, name) VALUES (1, 'Tony')";
        Statement stmt = CCJSqlParserUtil.parse(sql);

        if (stmt instanceof Insert) {
            Insert insert = (Insert) stmt;
            System.out.println("表名：" + insert.getTable().getName());
            System.out.println("列：" + insert.getColumns());
            System.out.println("值：" + insert.getItemsList());
        }
        System.out.println();
    }

    /**
     * UPDATE 解析示例
     */
    @Test
    void parseUpdate() throws Exception {
        System.out.println("=== UPDATE 解析 ===");
        String sql = "UPDATE user SET name = 'Tony' WHERE id = 1";
        Statement stmt = CCJSqlParserUtil.parse(sql);

        if (stmt instanceof Update) {
            Update update = (Update) stmt;
            System.out.println("表名：" + update.getTable());
            System.out.println("列：" + update.getColumns());
            System.out.println("表达式：" + update.getExpressions());
            System.out.println("条件：" + update.getWhere());
        }
        System.out.println();
    }

    /**
     * DELETE 解析示例
     */
    @Test
    void parseDelete() throws Exception {
        System.out.println("=== DELETE 解析 ===");
        String sql = "DELETE FROM user WHERE id = 1";
        Statement stmt = CCJSqlParserUtil.parse(sql);

        if (stmt instanceof Delete) {
            Delete delete = (Delete) stmt;
            System.out.println("表名：" + delete.getTable());
            System.out.println("条件：" + delete.getWhere());
        }
        System.out.println();
    }

    @Test
    void test1() throws JSQLParserException {
        String sql = "SELECT id,name FROM user WHERE age>20 order BY name";
        Statement stmt = CCJSqlParserUtil.parse(sql);

        // 使用 toString() 输出标准化 SQL
        String formattedSql = stmt.toString();
        System.out.println(formattedSql);
    }

    @Test
    void test2() throws JSQLParserException {
        String sql = "SELECT * FROM project_mini WHERE 1=1   <if test='status != null'>AND status = #{status}</if>   <if test='name != null'>AND name LIKE CONCAT('%', #{name}, '%')</if> LIMIT 10";
        Statement stmt = CCJSqlParserUtil.parse(sql);

        // 使用 toString() 输出标准化 SQL
        String formattedSql = stmt.toString();
        System.out.println(formattedSql);
    }


}
