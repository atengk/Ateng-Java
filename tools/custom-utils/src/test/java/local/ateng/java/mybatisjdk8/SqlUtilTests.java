package local.ateng.java.mybatisjdk8;

import local.ateng.java.customutils.utils.SqlUtil;
import org.junit.jupiter.api.Test;

import java.util.List;

public class SqlUtilTests {
    @Test
    public void testFormatSql() {
        String sql = " SELECT   id,   name  FROM   user  where id = 1  ";
        String formatted = SqlUtil.formatSql(sql);
        System.out.println("【formatSql】" + formatted);
    }

    @Test
    public void testGetTableName() {
        String sql = "SELECT id, name FROM user WHERE id = 1";
        String tableName = SqlUtil.getTableName(sql);
        System.out.println("【getTableName】" + tableName);

        String joinSql = "SELECT u.id, o.id FROM user u JOIN orders o ON u.id = o.user_id";
        System.out.println("【getTableName】" + SqlUtil.getTableName(joinSql));
    }

    @Test
    public void testGetSelectItems() {
        String sql = "SELECT id, name, age FROM user";
        List<String> items = SqlUtil.getSelectItems(sql);
        System.out.println("【getSelectItems】" + items);
    }

    @Test
    public void testToCountSql() {
        String sql = "SELECT id, name FROM user WHERE age > 20 ORDER BY id DESC";
        String countSql = SqlUtil.toCountSql(sql);
        System.out.println("【toCountSql】" + countSql);
    }

    @Test
    public void testAddLimit() {
        String sql = "SELECT id, name FROM user";
        String limitSql = SqlUtil.addLimit(sql, 0, 10);
        System.out.println("【addLimit】" + limitSql);
    }

    @Test
    public void testIsSelect() {
        String sql1 = "SELECT id FROM user";
        String sql2 = "UPDATE user SET name = 'Tom' WHERE id = 1";
        System.out.println("【isSelect-1】" + SqlUtil.isSelect(sql1));
        System.out.println("【isSelect-2】" + SqlUtil.isSelect(sql2));
    }

    @Test
    public void testGetWhereCondition() {
        String sql = "SELECT id, name FROM user WHERE age > 18 AND status = 1";
        String where = SqlUtil.getWhereCondition(sql);
        System.out.println("【getWhereCondition】" + where);
    }

    @Test
    public void testGetOrderBy() {
        String sql = "SELECT id, name FROM user WHERE age > 18 ORDER BY create_time DESC, id ASC";
        String orderBy = SqlUtil.getOrderBy(sql);
        System.out.println("【getOrderBy】" + orderBy);
    }

    @Test
    public void testRemoveOrderBy() {
        String sql = "SELECT id, name FROM user WHERE age > 18 ORDER BY create_time DESC, id ASC";
        String removed = SqlUtil.removeOrderBy(sql);
        System.out.println("【removeOrderBy】" + removed);
    }

    @Test
    public void testAddWhereCondition() {
        String sql = "SELECT id, name FROM user WHERE status = 1";
        String newSql = SqlUtil.addWhereCondition(sql, "tenant_id = 1001");
        System.out.println("【addWhereCondition】" + newSql);
    }

    @Test
    public void testGetSelectAliases() {
        String sql = "SELECT id, name AS username, age AS userAge FROM user";
        List<String> aliases = SqlUtil.getSelectAliases(sql);
        System.out.println("【getSelectAliases】" + aliases);
    }

    @Test
    public void test() {

        // 基础 SELECT
        String baseSql = "SELECT id, name FROM user WHERE status = 1 ORDER BY id DESC LIMIT 10";

        // CTE 查询
        String cteSql = "WITH cte AS (SELECT id, name FROM user WHERE status = 1) " +
                "SELECT * FROM cte WHERE id > 10";

        // UNION 查询
        String unionSql = "SELECT id, name FROM user WHERE status = 1 " +
                "UNION ALL " +
                "SELECT id, name FROM user WHERE status = 0";

        // GROUP BY + HAVING
        String havingSql = "SELECT dept_id, COUNT(*) AS cnt FROM employee " +
                "GROUP BY dept_id HAVING COUNT(*) > 10";

        // 子查询
        String subQuerySql = "SELECT * FROM (SELECT id, name FROM user) t WHERE t.id > 5";

        // INSERT
        String insertSql = "INSERT INTO user (id, name, status) VALUES (1, 'Tom', 1)";

        // UPDATE
        String updateSql = "UPDATE user SET name = 'Jerry' WHERE id = 1";

        // DELETE
        String deleteSql = "DELETE FROM user WHERE status = 0";

        System.out.println("=========== 格式化 SQL ===========");
        System.out.println(SqlUtil.formatSql(baseSql));

        System.out.println("=========== GROUP BY ===========");
        System.out.println(SqlUtil.getGroupBy(havingSql));

        System.out.println("=========== HAVING ===========");
        System.out.println(SqlUtil.getHavingCondition(havingSql));

        System.out.println("=========== JOIN 表 ===========");
        String joinSql = "SELECT u.id, o.id FROM user u JOIN orders o ON u.id = o.user_id";
        List<String> joinTables = SqlUtil.getJoinTables(joinSql);
        if (joinTables != null) {
            joinTables.forEach(System.out::println);
        }

        System.out.println("=========== DISTINCT ===========");
        String distinctSql = "SELECT DISTINCT name FROM user";
        System.out.println(SqlUtil.hasDistinct(distinctSql));

        System.out.println("=========== 子查询 ===========");
        System.out.println(SqlUtil.hasSubQuery(subQuerySql));
        System.out.println(SqlUtil.hasFromSubQuery(subQuerySql));

        System.out.println("=========== LIMIT ===========");
        System.out.println(SqlUtil.getLimit(baseSql));

        System.out.println("=========== 所有表名 ===========");
        List<String> allTables = SqlUtil.getAllTableNames(joinSql);
        if (allTables != null) {
            allTables.forEach(System.out::println);
        }

        System.out.println("=========== CTE ===========");
        List<String> cteNames = SqlUtil.getCteNames(cteSql);
        if (cteNames != null) {
            cteNames.forEach(System.out::println);
        }

        System.out.println("=========== UNION ===========");
        System.out.println(SqlUtil.hasUnion(unionSql));

        System.out.println("=========== INSERT 语句表名 ===========");
        System.out.println(SqlUtil.getTableName(insertSql));

        System.out.println("=========== UPDATE 语句表名 ===========");
        System.out.println(SqlUtil.getTableName(updateSql));

        System.out.println("=========== DELETE 语句表名 ===========");
        System.out.println(SqlUtil.getTableName(deleteSql));
    }

    @Test
    public void test2() {
        String sql = "SELECT id, name FROM user";
        String sql2 = "SELECT id, name FROM user WHERE status = 1";
        String cteSql = "WITH cte AS (SELECT id, name FROM user WHERE status = 1) SELECT * FROM cte";
        String unionSql = "SELECT id, name FROM user " +
                "UNION ALL " +
                "SELECT id, name FROM order";
        String unionSql2 = "SELECT id, name FROM user WHERE status = 1 " +
                "UNION ALL " +
                "SELECT id, name FROM user WHERE status = 0";
        String nestedSql = "SELECT * FROM (SELECT id, name FROM user WHERE status = 1) t";
        String joinSql = "SELECT u.id, u.tenant_id , o.id FROM user u JOIN orders o ON u.id = o.user_id";
        String joinSql2 = "SELECT u.id, u.tenant_id , o.id FROM user u JOIN orders o ON u.id = o.user_id where u.id > 1";

        System.out.println(SqlUtil.addWhereCondition(sql, "tenant_id = 1001"));
        System.out.println(SqlUtil.addWhereCondition(sql2, "tenant_id = 1001"));
        System.out.println(SqlUtil.addWhereCondition(cteSql, "tenant_id = 1001"));
        System.out.println(SqlUtil.addWhereCondition(unionSql, "tenant_id = 1001"));
        System.out.println(SqlUtil.addWhereCondition(unionSql2, "tenant_id = 1001"));
        System.out.println(SqlUtil.addWhereCondition(nestedSql, "tenant_id = 1001"));
        System.out.println(SqlUtil.addWhereCondition(joinSql, "tenant_id = 1001"));
        System.out.println(SqlUtil.addWhereCondition(joinSql2, "tenant_id = 1001"));
        /*
        SELECT id, name FROM user WHERE tenant_id = 1001
        SELECT id, name FROM user WHERE (status = 1) AND (tenant_id = 1001)
        WITH cte AS (SELECT id, name FROM user WHERE status = 1) SELECT * FROM cte WHERE tenant_id = 1001
        SELECT id, name FROM user WHERE tenant_id = 1001 AND tenant_id = 1001 UNION ALL SELECT id, name FROM order WHERE tenant_id = 1001 AND tenant_id = 1001
        SELECT id, name FROM user WHERE (status = 1) AND (tenant_id = 1001) UNION ALL SELECT id, name FROM user WHERE (status = 0) AND (tenant_id = 1001)
        SELECT * FROM (SELECT id, name FROM user WHERE status = 1) t WHERE tenant_id = 1001
        SELECT u.id, u.tenant_id, o.id FROM user u JOIN orders o ON u.id = o.user_id WHERE o.tenant_id = 1001 AND u.tenant_id = 1001
        SELECT u.id, u.tenant_id, o.id FROM user u JOIN orders o ON u.id = o.user_id WHERE (u.id > 1) AND (o.tenant_id = 1001 AND u.tenant_id = 1001)
         */
    }

}
