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


}
