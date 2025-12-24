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
 * PostgreSQL 表结构元数据加载工具类（PostgreSQL 16）
 *
 * <p>
 * 用于从 information_schema / pg_catalog 中反向解析表结构，
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
public final class TablePGUtil {

    private TablePGUtil() {
        throw new UnsupportedOperationException("工具类不可实例化");
    }

    /**
     * 加载完整表元数据（使用当前 schema）
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
     * 获取当前 schema
     *
     * @param jdbcTemplate JdbcTemplate
     * @return schema 名称
     */
    private static String getCurrentSchema(JdbcTemplate jdbcTemplate) {

        String sql = "select current_schema()";
        return jdbcTemplate.queryForObject(sql, String.class);
    }

    /**
     * 加载完整表元数据
     *
     * @param jdbcTemplate JdbcTemplate
     * @param schema       schema 名称
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
            if ("PRIMARY".equals(index.getType())) {
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
     */
    private static TableMeta queryTable(
            JdbcTemplate jdbcTemplate,
            String schema,
            String tableName
    ) {

        String sql =
                "select c.relname as table_name, " +
                        "obj_description(c.oid) as table_comment " +
                        "from pg_class c " +
                        "join pg_namespace n on n.oid = c.relnamespace " +
                        "where n.nspname = ? and c.relname = ?";

        return jdbcTemplate.queryForObject(
                sql,
                new Object[]{schema, tableName},
                (rs, rowNum) -> {
                    TableMeta table = new TableMeta();
                    table.setTableName(rs.getString("table_name"));
                    table.setComment(rs.getString("table_comment"));
                    table.setEngine(null);
                    table.setCharset(null);
                    return table;
                }
        );
    }

    /**
     * 查询 PostgreSQL 表字段信息（PostgreSQL 16）
     *
     * <p>
     * 使用 information_schema.columns + pg_catalog 补充 identity 信息，
     * 适配 Beetl PostgreSQL 建表模板。
     * </p>
     */
    private static List<ColumnMeta> queryColumns(
            JdbcTemplate jdbcTemplate,
            String schema,
            String tableName
    ) {

        String sql =
                "select " +
                        "c.column_name, " +
                        "c.data_type, " +
                        "c.is_nullable, " +
                        "c.column_default, " +
                        "c.character_maximum_length, " +
                        "c.numeric_precision, " +
                        "c.numeric_scale, " +
                        "c.is_identity, " +
                        "c.identity_generation, " +
                        "c.udt_name, " +
                        "pgd.description as column_comment " +
                        "from information_schema.columns c " +
                        "left join pg_catalog.pg_class pc " +
                        "  on pc.relname = c.table_name " +
                        "left join pg_catalog.pg_namespace pn " +
                        "  on pn.nspname = c.table_schema " +
                        "left join pg_catalog.pg_description pgd " +
                        "  on pgd.objoid = pc.oid " +
                        " and pgd.objsubid = c.ordinal_position " +
                        "where c.table_schema = ? " +
                        "  and c.table_name = ? " +
                        "order by c.ordinal_position";

        return jdbcTemplate.query(
                sql,
                new Object[]{schema, tableName},
                (rs, rowNum) -> {

                    ColumnMeta column = new ColumnMeta();

                    /* ---------- 基本信息 ---------- */
                    column.setName(rs.getString("column_name"));
                    String dataType = rs.getString("data_type");
                    String udtName = rs.getString("udt_name");

                    if ("USER-DEFINED".equals(dataType)) {
                        column.setType(udtName);
                    } else {
                        column.setType(dataType);
                    }

                    /* ---------- nullable ---------- */
                    column.setNullable("YES".equals(rs.getString("is_nullable")));

                    /* ---------- 默认值 ---------- */
                    String defaultValue = rs.getString("column_default");
                    if (defaultValue != null) {
                        column.setDefaultValue(defaultValue);
                    }

                    /* ---------- varchar / char ---------- */
                    Integer charLen = rs.getObject(
                            "character_maximum_length",
                            Integer.class
                    );
                    if (charLen != null) {
                        column.setLength(charLen);
                    }

                    /* ---------- numeric / decimal ---------- */
                    Integer precision = rs.getObject(
                            "numeric_precision",
                            Integer.class
                    );
                    Integer scale = rs.getObject(
                            "numeric_scale",
                            Integer.class
                    );
                    if (precision != null && scale != null) {
                        column.setPrecision(precision);
                        column.setScale(scale);
                    }

                    /* ---------- identity（自增） ---------- */
                    String isIdentity = rs.getString("is_identity");
                    if ("YES".equals(isIdentity)) {
                        column.setAutoIncrement(true);
                    } else {
                        column.setAutoIncrement(false);
                    }

                    /* ---------- PostgreSQL 不支持 ON UPDATE ---------- */
                    column.setOnUpdateCurrentTimestamp(false);

                    /* ---------- 主键默认 false（后续由索引补） ---------- */
                    column.setPrimaryKey(false);

                    /* ---------- 注释 ---------- */
                    column.setComment(rs.getString("column_comment"));

                    return column;
                }
        );
    }

    /**
     * 查询索引信息
     */
    private static List<IndexMeta> queryIndexes(
            JdbcTemplate jdbcTemplate,
            String schema,
            String tableName
    ) {

        String sql =
                "select " +
                        "i.relname as index_name, " +
                        "ix.indisunique, " +
                        "ix.indisprimary, " +
                        "a.attname as column_name, " +
                        "am.amname as index_type, " +
                        "case when (ix.indoption[a.attnum - 1] & 1) = 1 then 'DESC' else 'ASC' end as sort_order, " +
                        "array_position(ix.indkey, a.attnum) as seq " +
                        "from pg_class t " +
                        "join pg_namespace n on n.oid = t.relnamespace " +
                        "join pg_index ix on t.oid = ix.indrelid " +
                        "join pg_class i on i.oid = ix.indexrelid " +
                        "join pg_am am on i.relam = am.oid " +
                        "join pg_attribute a on a.attrelid = t.oid and a.attnum = any(ix.indkey) " +
                        "where n.nspname = ? and t.relname = ? " +
                        "order by index_name, seq";

        Map<String, IndexMeta> indexMap = new LinkedHashMap<>();

        jdbcTemplate.query(
                sql,
                new Object[]{schema, tableName},
                rs -> {
                    String indexName = rs.getString("index_name");

                    IndexMeta indexMeta = indexMap.computeIfAbsent(indexName, key -> {
                        IndexMeta meta = new IndexMeta();
                        meta.setName(indexName);

                        boolean primary = false;
                        boolean unique = false;
                        try {
                            primary = rs.getBoolean("indisprimary");
                            unique = rs.getBoolean("indisunique");
                        } catch (SQLException e) {
                            throw new RuntimeException(e);
                        }

                        if (primary) {
                            meta.setType("PRIMARY");
                        } else if (unique) {
                            meta.setType("UNIQUE");
                        } else {
                            meta.setType("NORMAL");
                        }

                        try {
                            meta.setUsing(rs.getString("index_type"));
                        } catch (SQLException e) {
                            throw new RuntimeException(e);
                        }
                        meta.setColumns(new ArrayList<>());
                        return meta;
                    });

                    IndexColumnMeta columnMeta = new IndexColumnMeta();
                    columnMeta.setColumnName(rs.getString("column_name"));
                    columnMeta.setOrder(rs.getString("sort_order"));

                    indexMeta.getColumns().add(columnMeta);
                }
        );

        return new ArrayList<>(indexMap.values());
    }

    /**
     * 规范化主键索引
     */
    public static void normalizePrimaryIndex(TableMeta tableMeta) {

        if (tableMeta.getColumns() == null || tableMeta.getColumns().isEmpty()) {
            return;
        }

        boolean hasPrimaryIndex = tableMeta.getIndexes() != null
                && tableMeta.getIndexes().stream()
                .anyMatch(idx -> "PRIMARY".equals(idx.getType()));

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
