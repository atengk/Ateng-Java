package local.ateng.java.customutils.utils;

import net.sf.jsqlparser.expression.Expression;
import net.sf.jsqlparser.parser.CCJSqlParserUtil;
import net.sf.jsqlparser.schema.Table;
import net.sf.jsqlparser.statement.Statement;
import net.sf.jsqlparser.statement.select.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * SQL 工具类
 * 基于 JSQLParser 实现，适用于生产环境常见的 SQL 操作。
 * 使用场景包括：SQL 格式化、字段提取、表名提取、分页改写等。
 *
 * @author 孔余
 * @since 2025-09-19
 */
public final class SqlUtil {

    /**
     * 禁止实例化工具类
     */
    private SqlUtil() {
        throw new UnsupportedOperationException("工具类不可实例化");
    }

    /**
     * 格式化 SQL，去除多余空格与换行
     *
     * @param sql 原始 SQL
     * @return 格式化后的单行 SQL
     */
    public static String formatSql(String sql) {
        if (StringUtil.isBlank(sql)) {
            return sql;
        }
        try {
            Statement statement = CCJSqlParserUtil.parse(sql);
            if (statement == null) {
                return sql.replaceAll("[\\s]+", " ").trim();
            }
            String parsed = statement.toString();
            if (StringUtil.isBlank(parsed)) {
                return sql.replaceAll("[\\s]+", " ").trim();
            }
            return parsed.replaceAll("[\\s]+", " ").trim();
        } catch (Exception e) {
            return sql.replaceAll("[\\s]+", " ").trim();
        }
    }

    /**
     * 获取 SQL 中的表名
     *
     * @param sql 原始 SQL
     * @return 表名
     */
    public static String getTableName(String sql) {
        try {
            Statement statement = CCJSqlParserUtil.parse(sql);
            if (statement instanceof Select) {
                Select select = (Select) statement;
                SelectBody selectBody = select.getSelectBody();
                if (selectBody instanceof PlainSelect) {
                    PlainSelect plainSelect = (PlainSelect) selectBody;
                    return plainSelect.getFromItem().toString();
                }
            }
        } catch (Exception e) {
            throw new IllegalArgumentException("SQL 解析失败", e);
        }
        return null;
    }

    /**
     * 获取 SQL 中的查询字段
     *
     * @param sql 原始 SQL
     * @return 字段列表
     */
    public static List<String> getSelectItems(String sql) {
        try {
            Statement statement = CCJSqlParserUtil.parse(sql);
            if (statement instanceof Select) {
                Select select = (Select) statement;
                SelectBody selectBody = select.getSelectBody();
                if (selectBody instanceof PlainSelect) {
                    PlainSelect plainSelect = (PlainSelect) selectBody;
                    List<SelectItem> items = plainSelect.getSelectItems();
                    return items.stream()
                            .map(Object::toString)
                            .collect(Collectors.toList());
                }
            }
        } catch (Exception e) {
            throw new IllegalArgumentException("SQL 解析失败", e);
        }
        return null;
    }

    /**
     * 将 SQL 包装为 count 查询
     * 适用于分页场景
     *
     * @param sql 原始 SQL
     * @return count SQL
     */
    public static String toCountSql(String sql) {
        try {
            Statement statement = CCJSqlParserUtil.parse(sql);
            if (statement instanceof Select) {
                return "SELECT COUNT(1) FROM (" + removeOrderBy(sql) + ") tmp_count";
            }
        } catch (Exception e) {
            throw new IllegalArgumentException("SQL 解析失败", e);
        }
        return null;
    }

    /**
     * 为 SQL 添加分页
     * 适用于 MySQL 场景
     *
     * @param sql    原始 SQL
     * @param offset 偏移量
     * @param limit  分页大小
     * @return 分页 SQL
     */
    public static String addLimit(String sql, int offset, int limit) {
        if (offset < 0 || limit <= 0) {
            throw new IllegalArgumentException("分页参数不合法");
        }
        return formatSql(sql) + " LIMIT " + offset + "," + limit;
    }

    /**
     * 判断 SQL 是否为 SELECT 语句
     *
     * @param sql 原始 SQL
     * @return 是否为 SELECT
     */
    public static boolean isSelect(String sql) {
        try {
            Statement statement = CCJSqlParserUtil.parse(sql);
            return statement instanceof Select;
        } catch (Exception e) {
            return false;
        }
    }

    /**
     * 获取 SQL 中的 WHERE 条件
     *
     * @param sql 原始 SQL
     * @return WHERE 条件字符串，若不存在返回 null
     */
    public static String getWhereCondition(String sql) {
        try {
            Statement statement = CCJSqlParserUtil.parse(sql);
            if (statement instanceof Select) {
                Select select = (Select) statement;
                SelectBody selectBody = select.getSelectBody();
                if (selectBody instanceof PlainSelect) {
                    PlainSelect plainSelect = (PlainSelect) selectBody;
                    if (plainSelect.getWhere() != null) {
                        return plainSelect.getWhere().toString();
                    }
                }
            }
        } catch (Exception e) {
            throw new IllegalArgumentException("SQL 解析失败", e);
        }
        return null;
    }

    /**
     * 获取 SQL 中的 ORDER BY 部分
     *
     * @param sql 原始 SQL
     * @return ORDER BY 子句，若不存在返回 null
     */
    public static String getOrderBy(String sql) {
        try {
            Statement statement = CCJSqlParserUtil.parse(sql);
            if (statement instanceof Select) {
                Select select = (Select) statement;
                SelectBody selectBody = select.getSelectBody();
                if (selectBody instanceof PlainSelect) {
                    PlainSelect plainSelect = (PlainSelect) selectBody;
                    if (plainSelect.getOrderByElements() != null) {
                        return plainSelect.getOrderByElements().toString();
                    }
                }
            }
        } catch (Exception e) {
            throw new IllegalArgumentException("SQL 解析失败", e);
        }
        return null;
    }

    /**
     * 移除 SQL 中的 ORDER BY
     *
     * @param sql 原始 SQL
     * @return 移除 ORDER BY 后的 SQL
     */
    public static String removeOrderBy(String sql) {
        try {
            Statement statement = CCJSqlParserUtil.parse(sql);
            if (statement instanceof Select) {
                Select select = (Select) statement;
                SelectBody selectBody = select.getSelectBody();
                if (selectBody instanceof PlainSelect) {
                    PlainSelect plainSelect = (PlainSelect) selectBody;
                    plainSelect.setOrderByElements(null);
                    return formatSql(select.toString());
                }
            }
        } catch (Exception e) {
            throw new IllegalArgumentException("SQL 解析失败", e);
        }
        return sql;
    }

    /**
     * 在复杂 SELECT SQL 的最外层追加 WHERE 条件
     *
     * @param sql             原始 SQL
     * @param columnCondition 需要追加的条件，例如 "tenant_id = 1001"
     * @return 拼接后的 SQL
     */
    public static String addWhereCondition(String sql, String columnCondition) {
        if (columnCondition == null || columnCondition.trim().isEmpty()) {
            return sql;
        }
        try {
            Statement statement = CCJSqlParserUtil.parse(sql);
            if (!(statement instanceof Select)) {
                throw new IllegalArgumentException("仅支持 SELECT SQL");
            }
            Select select = (Select) statement;

            // 1. 收集最外层表及别名
            Map<String, String> aliasMap = new HashMap<>();
            collectTableAliases(select.getSelectBody(), aliasMap);

            // 2. 构造条件表达式，支持多表
            String combinedCondition = buildConditionWithAliases(columnCondition, aliasMap);

            // 3. 递归追加条件，标记最外层
            SelectBody newBody = addWhereToSelectBody(select.getSelectBody(), combinedCondition, true);
            select.setSelectBody(newBody);

            return select.toString();
        } catch (Exception e) {
            throw new IllegalArgumentException("SQL 解析失败", e);
        }
    }

    /**
     * 递归追加条件到 SelectBody
     *
     * @param body              SelectBody 对象
     * @param combinedCondition 需要追加的条件
     * @param isTopLevel        是否最外层 SELECT，用于避免重复拼接条件
     * @return 新的 SelectBody
     * @throws Exception 解析异常
     */
    private static SelectBody addWhereToSelectBody(SelectBody body, String combinedCondition, boolean isTopLevel) throws Exception {
        if (body instanceof PlainSelect) {
            PlainSelect plain = (PlainSelect) body;

            // 最外层 PlainSelect 才添加条件
            if (isTopLevel && combinedCondition != null && !combinedCondition.isEmpty()) {
                Expression newCond = CCJSqlParserUtil.parseCondExpression(combinedCondition);
                if (plain.getWhere() == null) {
                    plain.setWhere(newCond);
                } else {
                    Expression combined = CCJSqlParserUtil.parseCondExpression(
                            "(" + plain.getWhere().toString() + ") AND (" + combinedCondition + ")"
                    );
                    plain.setWhere(combined);
                }
            }

            // 递归处理 FROM / JOIN 子查询
            if (plain.getFromItem() instanceof SubSelect) {
                SubSelect sub = (SubSelect) plain.getFromItem();
                sub.setSelectBody(addWhereToSelectBody(sub.getSelectBody(), combinedCondition, false));
            }
            if (plain.getJoins() != null) {
                for (Join join : plain.getJoins()) {
                    if (join.getRightItem() instanceof SubSelect) {
                        SubSelect sub = (SubSelect) join.getRightItem();
                        sub.setSelectBody(addWhereToSelectBody(sub.getSelectBody(), combinedCondition, false));
                    }
                }
            }

            return plain;

        } else if (body instanceof SetOperationList) {
            // 最外层 SetOperationList → 每个子 SELECT 当作最外层加条件
            SetOperationList setOp = (SetOperationList) body;
            List<SelectBody> selects = setOp.getSelects();
            for (int i = 0; i < selects.size(); i++) {
                // 子 SELECT 添加条件，标记 true，因为它们是最外层 PlainSelect
                selects.set(i, addWhereToSelectBody(selects.get(i), combinedCondition, true));
            }
            return setOp;

        } else if (body instanceof WithItem) {
            // CTE 内部保持不变
            return body;

        } else {
            throw new UnsupportedOperationException("未知 SelectBody 类型: " + body.getClass().getName());
        }
    }

    /**
     * 收集最外层 FROM / JOIN 表及其别名
     *
     * @param body     SelectBody 对象
     * @param aliasMap 表名 -> 别名（无别名为 null）
     */
    private static void collectTableAliases(SelectBody body, Map<String, String> aliasMap) {
        if (body instanceof PlainSelect) {
            PlainSelect plain = (PlainSelect) body;

            // FROM 表
            FromItem from = plain.getFromItem();
            if (from instanceof Table) {
                Table t = (Table) from;
                aliasMap.put(t.getName(), t.getAlias() != null ? t.getAlias().getName() : null);
            }

            // JOIN 表
            if (plain.getJoins() != null) {
                for (Join join : plain.getJoins()) {
                    FromItem right = join.getRightItem();
                    if (right instanceof Table) {
                        Table t = (Table) right;
                        aliasMap.put(t.getName(), t.getAlias() != null ? t.getAlias().getName() : null);
                    }
                }
            }

        } else if (body instanceof SetOperationList) {
            for (SelectBody sb : ((SetOperationList) body).getSelects()) {
                collectTableAliases(sb, aliasMap);
            }
        }
    }

    /**
     * 为每张表生成条件组合
     * 有别名的表加别名，无别名保持列名
     *
     * @param columnCondition 原始列条件
     * @param aliasMap        表名 -> 别名
     * @return 多表组合条件
     */
    private static String buildConditionWithAliases(String columnCondition, Map<String, String> aliasMap) {
        if (aliasMap.isEmpty()) {
            return columnCondition;
        }
        StringBuilder sb = new StringBuilder();
        int i = 0;
        for (String alias : aliasMap.values()) {
            if (i > 0) {
                sb.append(" AND ");
            }
            if (alias != null) {
                sb.append(alias).append(".").append(columnCondition);
            } else {
                sb.append(columnCondition);
            }
            i++;
        }
        return sb.toString();
    }

    /**
     * 获取 SQL 中的字段别名
     *
     * @param sql 原始 SQL
     * @return 字段别名列表（若无别名则返回字段本身）
     */
    public static List<String> getSelectAliases(String sql) {
        try {
            Statement statement = CCJSqlParserUtil.parse(sql);
            if (statement instanceof Select) {
                Select select = (Select) statement;
                SelectBody selectBody = select.getSelectBody();
                if (selectBody instanceof PlainSelect) {
                    PlainSelect plainSelect = (PlainSelect) selectBody;
                    List<SelectItem> items = plainSelect.getSelectItems();
                    return items.stream()
                            .map(Object::toString)
                            .map(item -> {
                                if (item.contains(" as ") || item.contains(" AS ")) {
                                    return item.substring(item.toLowerCase().lastIndexOf(" as ") + 4).trim();
                                }
                                return item.trim();
                            })
                            .collect(Collectors.toList());
                }
            }
        } catch (Exception e) {
            throw new IllegalArgumentException("SQL 解析失败", e);
        }
        return null;
    }

    /**
     * 获取 GROUP BY 片段
     *
     * @param sql 原始 SQL
     * @return GROUP BY 片段，如果不存在则返回 null
     */
    public static String getGroupBy(String sql) {
        try {
            Statement statement = CCJSqlParserUtil.parse(sql);
            if (statement instanceof Select) {
                Select select = (Select) statement;
                SelectBody body = select.getSelectBody();
                if (body instanceof PlainSelect) {
                    PlainSelect ps = (PlainSelect) body;
                    if (ps.getGroupBy() != null) {
                        return ps.getGroupBy().toString();
                    }
                }
            }
            return null;
        } catch (Exception e) {
            throw new IllegalArgumentException("SQL 解析失败", e);
        }
    }

    /**
     * 获取 JOIN 表信息
     *
     * @param sql 原始 SQL
     * @return JOIN 信息列表，如果不存在则返回 null
     */
    public static List<String> getJoinTables(String sql) {
        try {
            Statement statement = CCJSqlParserUtil.parse(sql);
            if (statement instanceof Select) {
                SelectBody body = ((Select) statement).getSelectBody();
                if (body instanceof PlainSelect) {
                    PlainSelect ps = (PlainSelect) body;
                    if (ps.getJoins() != null) {
                        return ps.getJoins().stream()
                                .map(Object::toString)
                                .collect(Collectors.toList());
                    }
                }
            }
            return null;
        } catch (Exception e) {
            throw new IllegalArgumentException("SQL 解析失败", e);
        }
    }

    /**
     * 判断 SQL 是否包含 DISTINCT
     *
     * @param sql 原始 SQL
     * @return 存在 DISTINCT 返回 true，否则返回 false
     */
    public static boolean hasDistinct(String sql) {
        try {
            Statement statement = CCJSqlParserUtil.parse(sql);
            if (statement instanceof Select) {
                SelectBody body = ((Select) statement).getSelectBody();
                if (body instanceof PlainSelect) {
                    return ((PlainSelect) body).getDistinct() != null;
                }
            }
            return false;
        } catch (Exception e) {
            throw new IllegalArgumentException("SQL 解析失败", e);
        }
    }

    /**
     * 判断 SQL 是否包含子查询
     *
     * @param sql 原始 SQL
     * @return 存在子查询返回 true，否则返回 false
     */
    public static boolean hasSubQuery(String sql) {
        try {
            Statement statement = CCJSqlParserUtil.parse(sql);
            String sqlString = statement.toString().toUpperCase();
            return sqlString.contains("(SELECT");
        } catch (Exception e) {
            throw new IllegalArgumentException("SQL 解析失败", e);
        }
    }

    /**
     * 获取 LIMIT 分页信息
     *
     * @param sql 原始 SQL
     * @return LIMIT 子句，如果不存在则返回 null
     */
    public static String getLimit(String sql) {
        try {
            Statement statement = CCJSqlParserUtil.parse(sql);
            if (statement instanceof Select) {
                SelectBody body = ((Select) statement).getSelectBody();
                if (body instanceof PlainSelect) {
                    PlainSelect ps = (PlainSelect) body;
                    if (ps.getLimit() != null) {
                        return ps.getLimit().toString();
                    }
                }
            }
            return null;
        } catch (Exception e) {
            throw new IllegalArgumentException("SQL 解析失败", e);
        }
    }

    /**
     * 获取 SQL 中所有的表名（包含 FROM、JOIN）
     *
     * @param sql 原始 SQL
     * @return 表名列表
     */
    public static List<String> getAllTableNames(String sql) {
        try {
            Statement statement = CCJSqlParserUtil.parse(sql);
            String sqlString = statement.toString().toLowerCase();

            // 按空格拆分，再过滤包含 from/join 关键字的片段
            String[] tokens = sqlString.replaceAll("[^a-z0-9_, ]", " ").split("\\s+");
            List<String> tables = new java.util.ArrayList<>();

            for (int i = 0; i < tokens.length; i++) {
                if ("from".equals(tokens[i]) || "join".equals(tokens[i])) {
                    if (i + 1 < tokens.length) {
                        tables.add(tokens[i + 1]);
                    }
                }
            }
            return tables;
        } catch (Exception e) {
            throw new IllegalArgumentException("SQL 解析失败", e);
        }
    }

    /**
     * 获取 SQL 中的 CTE 定义 (WITH 子句)
     *
     * @param sql 原始 SQL
     * @return CTE 名称列表，如果不存在则返回 null
     */
    public static List<String> getCteNames(String sql) {
        try {
            Statement statement = CCJSqlParserUtil.parse(sql);
            if (statement instanceof Select) {
                Select select = (Select) statement;
                if (select.getWithItemsList() != null) {
                    return select.getWithItemsList().stream()
                            .map(withItem -> withItem.getName().toString())
                            .collect(Collectors.toList());
                }
            }
            return null;
        } catch (Exception e) {
            throw new IllegalArgumentException("SQL 解析失败", e);
        }
    }

    /**
     * 判断 SQL 是否为 UNION 查询
     *
     * @param sql 原始 SQL
     * @return 存在 UNION 返回 true，否则返回 false
     */
    public static boolean hasUnion(String sql) {
        try {
            Statement statement = CCJSqlParserUtil.parse(sql);
            if (statement instanceof Select) {
                SelectBody body = ((Select) statement).getSelectBody();
                return body.getClass().getSimpleName().contains("SetOperationList");
            }
            return false;
        } catch (Exception e) {
            throw new IllegalArgumentException("SQL 解析失败", e);
        }
    }

    /**
     * 获取 HAVING 条件
     *
     * @param sql 原始 SQL
     * @return HAVING 条件字符串，如果不存在则返回 null
     */
    public static String getHavingCondition(String sql) {
        try {
            Statement statement = CCJSqlParserUtil.parse(sql);
            if (statement instanceof Select) {
                SelectBody body = ((Select) statement).getSelectBody();
                if (body instanceof PlainSelect) {
                    PlainSelect ps = (PlainSelect) body;
                    if (ps.getHaving() != null) {
                        return ps.getHaving().toString();
                    }
                }
            }
            return null;
        } catch (Exception e) {
            throw new IllegalArgumentException("SQL 解析失败", e);
        }
    }

    /**
     * 判断是否存在 FROM 子查询
     *
     * @param sql 原始 SQL
     * @return 存在子查询返回 true，否则返回 false
     */
    public static boolean hasFromSubQuery(String sql) {
        try {
            Statement statement = CCJSqlParserUtil.parse(sql);
            return statement.toString().toUpperCase().contains("FROM (SELECT");
        } catch (Exception e) {
            throw new IllegalArgumentException("SQL 解析失败", e);
        }
    }

    /**
     * SQL 操作类型
     */
    public enum SqlOperationType {
        SELECT,
        INSERT,
        UPDATE,
        DELETE,
        MERGE,
        REPLACE,
        UPSERT,
        TRUNCATE,
        CREATE,
        ALTER,
        DROP,
        CALL,
        SHOW,
        DESCRIBE,
        EXPLAIN,
        SET,
        USE,
        UNKNOWN
    }

    /**
     * 美观格式化 SQL
     * 说明：
     * 1. 保留解析后的语义结构；
     * 2. 对常见关键字进行换行缩进；
     * 3. 适合日志输出、控制台展示、排查问题。
     *
     * @param sql 原始 SQL
     * @return 美观格式化后的 SQL
     */
    public static String formatSqlPretty(String sql) {
        if (StringUtil.isBlank(sql)) {
            return sql;
        }
        try {
            Statement statement = CCJSqlParserUtil.parse(sql);
            if (statement == null) {
                return prettyFormatSql(sql);
            }
            return prettyFormatSql(statement.toString());
        } catch (Exception e) {
            return prettyFormatSql(sql);
        }
    }

    /**
     * 获取 SQL 操作类型
     *
     * @param sql 原始 SQL
     * @return 操作类型，解析失败返回 UNKNOWN
     */
    public static SqlOperationType getOperationType(String sql) {
        if (StringUtil.isBlank(sql)) {
            return SqlOperationType.UNKNOWN;
        }
        try {
            Statement statement = CCJSqlParserUtil.parse(sql);
            if (statement == null) {
                return SqlOperationType.UNKNOWN;
            }

            // SELECT（包含 WITH 查询，最终也归类为 SELECT）
            if (statement instanceof Select) {
                return SqlOperationType.SELECT;
            }

            String simpleName = statement.getClass().getSimpleName().toUpperCase();

            if (simpleName.contains("INSERT")) {
                return SqlOperationType.INSERT;
            }
            if (simpleName.contains("UPDATE")) {
                return SqlOperationType.UPDATE;
            }
            if (simpleName.contains("DELETE")) {
                return SqlOperationType.DELETE;
            }
            if (simpleName.contains("MERGE")) {
                return SqlOperationType.MERGE;
            }
            if (simpleName.contains("REPLACE")) {
                return SqlOperationType.REPLACE;
            }
            if (simpleName.contains("UPSERT")) {
                return SqlOperationType.UPSERT;
            }
            if (simpleName.contains("TRUNCATE")) {
                return SqlOperationType.TRUNCATE;
            }
            if (simpleName.contains("CREATE")) {
                return SqlOperationType.CREATE;
            }
            if (simpleName.contains("ALTER")) {
                return SqlOperationType.ALTER;
            }
            if (simpleName.contains("DROP")) {
                return SqlOperationType.DROP;
            }
            if (simpleName.contains("CALL")) {
                return SqlOperationType.CALL;
            }
            if (simpleName.contains("SHOW")) {
                return SqlOperationType.SHOW;
            }
            if (simpleName.contains("DESCRIBE") || "DESC".equals(simpleName)) {
                return SqlOperationType.DESCRIBE;
            }
            if (simpleName.contains("EXPLAIN")) {
                return SqlOperationType.EXPLAIN;
            }
            if (simpleName.contains("SET")) {
                return SqlOperationType.SET;
            }
            if (simpleName.contains("USE")) {
                return SqlOperationType.USE;
            }

            return SqlOperationType.UNKNOWN;
        } catch (Exception e) {
            return SqlOperationType.UNKNOWN;
        }
    }

    /**
     * 获取 SQL 操作类型名称
     *
     * @param sql 原始 SQL
     * @return 操作类型名称
     */
    public static String getOperationTypeName(String sql) {
        return getOperationType(sql).name();
    }

    /**
     * 美观格式化 SQL 的核心实现
     *
     * @param sql 原始 SQL
     * @return 格式化后的 SQL
     */
    private static String prettyFormatSql(String sql) {
        if (StringUtil.isBlank(sql)) {
            return sql;
        }

        String prettySql = sql.replaceAll("[\\r\\n]+", " ")
                .replaceAll("\\s+", " ")
                .trim();

        if (StringUtil.isBlank(prettySql)) {
            return prettySql;
        }

        // 统一首行关键字的换行方式
        prettySql = prettySql.replaceAll("(?i)^SELECT\\s+", "SELECT\n    ");
        prettySql = prettySql.replaceAll("(?i)^INSERT\\s+INTO\\s+", "INSERT INTO\n    ");
        prettySql = prettySql.replaceAll("(?i)^UPDATE\\s+", "UPDATE\n    ");
        prettySql = prettySql.replaceAll("(?i)^DELETE\\s+FROM\\s+", "DELETE FROM\n    ");
        prettySql = prettySql.replaceAll("(?i)^MERGE\\s+INTO\\s+", "MERGE INTO\n    ");
        prettySql = prettySql.replaceAll("(?i)^TRUNCATE\\s+TABLE\\s+", "TRUNCATE TABLE\n    ");
        prettySql = prettySql.replaceAll("(?i)^CREATE\\s+", "CREATE\n    ");
        prettySql = prettySql.replaceAll("(?i)^ALTER\\s+", "ALTER\n    ");
        prettySql = prettySql.replaceAll("(?i)^DROP\\s+", "DROP\n    ");
        prettySql = prettySql.replaceAll("(?i)^WITH\\s+", "WITH\n    ");

        // 常见子句换行
        prettySql = prettySql.replaceAll("(?i)\\s+FROM\\s+", "\nFROM ");
        prettySql = prettySql.replaceAll("(?i)\\s+WHERE\\s+", "\nWHERE ");
        prettySql = prettySql.replaceAll("(?i)\\s+GROUP\\s+BY\\s+", "\nGROUP BY ");
        prettySql = prettySql.replaceAll("(?i)\\s+HAVING\\s+", "\nHAVING ");
        prettySql = prettySql.replaceAll("(?i)\\s+ORDER\\s+BY\\s+", "\nORDER BY ");
        prettySql = prettySql.replaceAll("(?i)\\s+LIMIT\\s+", "\nLIMIT ");
        prettySql = prettySql.replaceAll("(?i)\\s+UNION\\s+ALL\\s+", "\nUNION ALL ");
        prettySql = prettySql.replaceAll("(?i)\\s+UNION\\s+", "\nUNION ");
        prettySql = prettySql.replaceAll("(?i)\\s+INTERSECT\\s+", "\nINTERSECT ");
        prettySql = prettySql.replaceAll("(?i)\\s+EXCEPT\\s+", "\nEXCEPT ");
        prettySql = prettySql.replaceAll("(?i)\\s+LEFT\\s+JOIN\\s+", "\nLEFT JOIN ");
        prettySql = prettySql.replaceAll("(?i)\\s+RIGHT\\s+JOIN\\s+", "\nRIGHT JOIN ");
        prettySql = prettySql.replaceAll("(?i)\\s+INNER\\s+JOIN\\s+", "\nINNER JOIN ");
        prettySql = prettySql.replaceAll("(?i)\\s+FULL\\s+JOIN\\s+", "\nFULL JOIN ");
        prettySql = prettySql.replaceAll("(?i)\\s+CROSS\\s+JOIN\\s+", "\nCROSS JOIN ");
        prettySql = prettySql.replaceAll("(?i)\\s+JOIN\\s+", "\nJOIN ");
        prettySql = prettySql.replaceAll("(?i)\\s+SET\\s+", "\nSET ");
        prettySql = prettySql.replaceAll("(?i)\\s+VALUES\\s+", "\nVALUES ");

        // ON 子句单独缩进，便于阅读
        prettySql = prettySql.replaceAll("(?i)\\s+ON\\s+", "\n    ON ");

        return prettySql.trim();
    }

    /**
     * 判断是否为查询类 SQL
     *
     * @param sql 原始 SQL
     * @return 是查询 SQL 返回 true，否则返回 false
     */
    public static boolean isQuery(String sql) {
        return getOperationType(sql) == SqlOperationType.SELECT;
    }

    /**
     * 判断是否为写入类 SQL
     *
     * @param sql 原始 SQL
     * @return 是写入 SQL 返回 true，否则返回 false
     */
    public static boolean isDml(String sql) {
        SqlOperationType operationType = getOperationType(sql);
        return operationType == SqlOperationType.INSERT
                || operationType == SqlOperationType.UPDATE
                || operationType == SqlOperationType.DELETE
                || operationType == SqlOperationType.REPLACE
                || operationType == SqlOperationType.MERGE
                || operationType == SqlOperationType.UPSERT;
    }

    /**
     * 判断是否为结构定义类 SQL
     *
     * @param sql 原始 SQL
     * @return 是 DDL SQL 返回 true，否则返回 false
     */
    public static boolean isDdl(String sql) {
        SqlOperationType operationType = getOperationType(sql);
        return operationType == SqlOperationType.CREATE
                || operationType == SqlOperationType.ALTER
                || operationType == SqlOperationType.DROP
                || operationType == SqlOperationType.TRUNCATE;
    }

    /**
     * 判断是否包含 WHERE 子句
     *
     * @param sql 原始 SQL
     * @return 存在 WHERE 返回 true，否则返回 false
     */
    public static boolean hasWhere(String sql) {
        return StringUtil.isNotBlank(getWhereCondition(sql));
    }

    /**
     * 判断是否包含 GROUP BY 子句
     *
     * @param sql 原始 SQL
     * @return 存在 GROUP BY 返回 true，否则返回 false
     */
    public static boolean hasGroupBy(String sql) {
        return StringUtil.isNotBlank(getGroupBy(sql));
    }

    /**
     * 判断是否包含 HAVING 子句
     *
     * @param sql 原始 SQL
     * @return 存在 HAVING 返回 true，否则返回 false
     */
    public static boolean hasHaving(String sql) {
        return StringUtil.isNotBlank(getHavingCondition(sql));
    }

    /**
     * 判断是否包含 ORDER BY 子句
     *
     * @param sql 原始 SQL
     * @return 存在 ORDER BY 返回 true，否则返回 false
     */
    public static boolean hasOrderBy(String sql) {
        return StringUtil.isNotBlank(getOrderBy(sql));
    }

    /**
     * 判断是否包含 LIMIT 子句
     *
     * @param sql 原始 SQL
     * @return 存在 LIMIT 返回 true，否则返回 false
     */
    public static boolean hasLimit(String sql) {
        return StringUtil.isNotBlank(getLimit(sql));
    }

    /**
     * 判断是否包含 JOIN 子句
     *
     * @param sql 原始 SQL
     * @return 存在 JOIN 返回 true，否则返回 false
     */
    public static boolean hasJoin(String sql) {
        List<String> joinTables = getJoinTables(sql);
        return joinTables != null && !joinTables.isEmpty();
    }

    /**
     * 判断是否包含 WITH 子句
     *
     * @param sql 原始 SQL
     * @return 存在 WITH 返回 true，否则返回 false
     */
    public static boolean hasWith(String sql) {
        List<String> cteNames = getCteNames(sql);
        return cteNames != null && !cteNames.isEmpty();
    }

}
