package io.github.atengk.beetl.utils;

import io.github.atengk.beetl.model.*;
import org.springframework.jdbc.core.JdbcTemplate;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * MySQL 表结构元数据加载工具类
 *
 * <p>
 * 用于从 information_schema 中反向解析表结构，
 * 并组装为 TableMeta / ColumnMeta / IndexMeta 模型。
 * </p>
 *
 * <p>
 * 设计为纯静态工具类，不依赖 Spring 容器，
 * JdbcTemplate 由调用方显式传入。
 * </p>
 *
 * @author 孔余
 * @since 2025-12-24
 */
public final class TableUtil {

    /**
     * 私有构造函数，禁止实例化
     */
    private TableUtil() {
        throw new UnsupportedOperationException("工具类不可实例化");
    }

    /**
     * 加载完整表元数据
     * <p>
     * 自动从当前连接中获取 schema
     * </p>
     *
     * @param jdbcTemplate JdbcTemplate
     * @param tableName    表名
     * @return 表元数据
     */
    public static TableMeta loadTableMeta(
            JdbcTemplate jdbcTemplate,
            String tableName
    ) {

        String schema = getCurrentSchema(jdbcTemplate);
        return loadTableMeta(jdbcTemplate, schema, tableName);
    }

    /**
     * 获取当前连接使用的数据库名
     *
     * @param jdbcTemplate JdbcTemplate
     * @return schema 名称
     */
    private static String getCurrentSchema(JdbcTemplate jdbcTemplate) {

        String sql = "select database()";

        return jdbcTemplate.queryForObject(
                sql,
                String.class
        );
    }

    /**
     * 加载完整表元数据
     *
     * @param jdbcTemplate JdbcTemplate
     * @param schema       数据库名
     * @param tableName    表名
     * @return 表元数据
     */
    public static TableMeta loadTableMeta(
            JdbcTemplate jdbcTemplate,
            String schema,
            String tableName
    ) {

        TableMeta tableMeta = queryTable(jdbcTemplate, schema, tableName);

        List<ColumnMeta> columns = queryColumns(jdbcTemplate, schema, tableName);
        Map<String, ColumnMeta> columnMap = columns.stream()
                .collect(Collectors.toMap(
                        ColumnMeta::getName,
                        Function.identity()
                ));

        List<IndexMeta> indexes = queryIndexes(jdbcTemplate, schema, tableName);

        for (IndexMeta index : indexes) {
            if (index.getType() == "PRIMARY") {
                for (IndexColumnMeta indexColumn : index.getColumns()) {
                    ColumnMeta columnMeta = columnMap.get(indexColumn.getColumnName());
                    if (columnMeta != null) {
                        columnMeta.setPrimaryKey(true);
                    }
                }
            }
        }

        tableMeta.setColumns(columns);
        tableMeta.setIndexes(indexes);
        return tableMeta;
    }

    /**
     * 查询表基本信息
     *
     * @param jdbcTemplate JdbcTemplate
     * @param schema       数据库名
     * @param tableName    表名
     * @return 表元数据
     */
    private static TableMeta queryTable(
            JdbcTemplate jdbcTemplate,
            String schema,
            String tableName
    ) {

        String sql =
                "select table_name, table_comment, engine, table_collation " +
                        "from information_schema.tables " +
                        "where table_schema = ? and table_name = ?";

        return jdbcTemplate.queryForObject(
                sql,
                new Object[]{schema, tableName},
                (rs, rowNum) -> {
                    TableMeta table = new TableMeta();
                    table.setTableName(rs.getString("table_name"));
                    table.setComment(rs.getString("table_comment"));
                    table.setEngine(rs.getString("engine"));
                    table.setCharset(extractCharset(rs.getString("table_collation")));
                    return table;
                }
        );
    }

    /**
     * 查询字段信息
     *
     * @param jdbcTemplate JdbcTemplate
     * @param schema       数据库名
     * @param tableName    表名
     * @return 字段列表
     */
    private static List<ColumnMeta> queryColumns(
            JdbcTemplate jdbcTemplate,
            String schema,
            String tableName
    ) {

        String sql =
                "select column_name, column_type, is_nullable, column_default, extra, column_comment " +
                        "from information_schema.columns " +
                        "where table_schema = ? and table_name = ? " +
                        "order by ordinal_position";

        return jdbcTemplate.query(
                sql,
                new Object[]{schema, tableName},
                (rs, rowNum) -> {
                    ColumnMeta column = new ColumnMeta();
                    column.setName(rs.getString("column_name"));
                    column.setType(rs.getString("column_type"));
                    column.setNullable("YES".equals(rs.getString("is_nullable")));
                    column.setDefaultValue(rs.getString("column_default"));

                    String extra = rs.getString("extra");
                    column.setAutoIncrement(
                            extra != null && extra.contains("auto_increment")
                    );
                    column.setOnUpdateCurrentTimestamp(
                            extra != null && extra.toLowerCase().contains("on update current_timestamp")
                    );
                    column.setPrimaryKey(false);
                    column.setComment(rs.getString("column_comment"));
                    return column;
                }
        );
    }

    /**
     * 查询索引信息
     *
     * @param jdbcTemplate JdbcTemplate
     * @param schema       数据库名
     * @param tableName    表名
     * @return 索引列表
     */
    private static List<IndexMeta> queryIndexes(
            JdbcTemplate jdbcTemplate,
            String schema,
            String tableName
    ) {

        String sql =
                "select index_name, non_unique, column_name, seq_in_index, collation, index_type " +
                        "from information_schema.statistics " +
                        "where table_schema = ? and table_name = ? " +
                        "order by index_name, seq_in_index";

        Map<String, IndexMeta> indexMap = new LinkedHashMap<>();

        jdbcTemplate.query(
                sql,
                new Object[]{schema, tableName},
                rs -> {
                    String indexName = rs.getString("index_name");

                    IndexMeta indexMeta = indexMap.computeIfAbsent(indexName, key -> {
                        IndexMeta meta = new IndexMeta();
                        meta.setName(indexName);

                        try {
                            int nonUnique = rs.getInt("non_unique");

                            if ("PRIMARY".equals(indexName)) {
                                meta.setType("PRIMARY");
                            } else if (nonUnique == 0) {
                                meta.setType("UNIQUE");
                            } else {
                                meta.setType("NORMAL");
                            }

                            meta.setUsing(rs.getString("index_type"));
                            meta.setColumns(new ArrayList<>());
                        } catch (SQLException e) {
                            throw new RuntimeException(e);
                        }

                        return meta;
                    });

                    IndexColumnMeta columnMeta = new IndexColumnMeta();
                    columnMeta.setColumnName(rs.getString("column_name"));

                    String collation = rs.getString("collation");
                    if ("A".equals(collation)) {
                        columnMeta.setOrder("ASC");
                    } else if ("D".equals(collation)) {
                        columnMeta.setOrder("DESC");
                    }

                    indexMeta.getColumns().add(columnMeta);
                }
        );

        return new ArrayList<>(indexMap.values());
    }

    /**
     * 从 collation 中提取字符集
     *
     * @param collation 排序规则
     * @return 字符集
     */
    private static String extractCharset(String collation) {
        if (collation == null) {
            return null;
        }
        int index = collation.indexOf('_');
        if (index > 0) {
            return collation.substring(0, index);
        }
        return collation;
    }

    /**
     * 规范化表索引定义
     *
     * <p>
     * 如果 ColumnMeta 中声明了 primaryKey，但未显式定义 PRIMARY 索引，
     * 则自动补充 PRIMARY 索引定义
     * </p>
     */
    public static void normalizePrimaryIndex(TableMeta tableMeta) {

        if (tableMeta.getColumns() == null || tableMeta.getColumns().isEmpty()) {
            return;
        }

        boolean hasPrimaryIndex = tableMeta.getIndexes() != null
                && tableMeta.getIndexes().stream()
                .anyMatch(idx -> idx.getType() == "PRIMARY");

        if (hasPrimaryIndex) {
            return;
        }

        List<IndexColumnMeta> pkColumns = tableMeta.getColumns().stream()
                .filter(col -> Boolean.TRUE.equals(col.getPrimaryKey()))
                .map(col -> {
                    IndexColumnMeta ic = new IndexColumnMeta();
                    ic.setColumnName(col.getName());
                    return ic;
                })
                .collect(Collectors.toList());

        if (pkColumns.isEmpty()) {
            return;
        }

        IndexMeta primaryIndex = new IndexMeta();
        primaryIndex.setName("PRIMARY");
        primaryIndex.setType("PRIMARY");
        primaryIndex.setColumns(pkColumns);

        if (tableMeta.getIndexes() == null) {
            tableMeta.setIndexes(new ArrayList<>());
        }

        tableMeta.getIndexes().add(0, primaryIndex);
    }


}
