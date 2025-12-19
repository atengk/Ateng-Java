package io.github.atengk.beetl;

import io.github.atengk.beetl.model.ColumnMeta;
import io.github.atengk.beetl.model.IndexMeta;
import io.github.atengk.beetl.model.TableMeta;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@SpringBootTest
public class MySQLTests {
    @Qualifier("mysqlJdbcTemplate")
    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Test
    public void test() {
        TableMeta tableMeta = loadTableMeta("kongyu", "t_user");
        System.out.println(tableMeta);
    }

    /**
     * 加载完整表元数据
     *
     * @param schema    数据库名
     * @param tableName 表名
     * @return 表元数据
     */
    public TableMeta loadTableMeta(String schema, String tableName) {

        TableMeta tableMeta = queryTable(schema, tableName);

        List<ColumnMeta> columns = queryColumns(schema, tableName);
        Map<String, ColumnMeta> columnMap = columns.stream()
                .collect(Collectors.toMap(
                        ColumnMeta::getName,
                        Function.identity()
                ));

        List<IndexMeta> indexes = queryIndexes(schema, tableName);
        for (IndexMeta index : indexes) {
            if (Boolean.TRUE.equals(index.getPrimary())) {
                for (String column : index.getColumns()) {
                    ColumnMeta columnMeta = columnMap.get(column);
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
    private TableMeta queryTable(String schema, String tableName) {

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
     */
    private List<ColumnMeta> queryColumns(String schema, String tableName) {

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
                    column.setAutoIncrement(
                            rs.getString("extra") != null
                                    && rs.getString("extra").contains("auto_increment")
                    );
                    column.setPrimaryKey(false);
                    column.setComment(rs.getString("column_comment"));
                    return column;
                }
        );
    }


    /**
     * 查询索引信息
     */
    private List<IndexMeta> queryIndexes(String schema, String tableName) {

        String sql =
                "select index_name, non_unique, column_name, seq_in_index " +
                        "from information_schema.statistics " +
                        "where table_schema = ? and table_name = ? " +
                        "order by index_name, seq_in_index";

        Map<String, IndexMeta> indexMap = new LinkedHashMap<>();

        jdbcTemplate.query(
                sql,
                new Object[]{schema, tableName},
                rs -> {
                    String indexName = rs.getString("index_name");
                    IndexMeta index = indexMap.computeIfAbsent(indexName, k -> {
                        IndexMeta meta = new IndexMeta();
                        meta.setName(indexName);
                        meta.setPrimary("PRIMARY".equals(indexName));
                        try {
                            meta.setUnique(rs.getInt("non_unique") == 0);
                        } catch (SQLException e) {
                            throw new RuntimeException(e);
                        }
                        return meta;
                    });
                    index.getColumns().add(rs.getString("column_name"));
                }
        );

        return new ArrayList<>(indexMap.values());
    }


    /**
     * 从 collation 中提取字符集
     */
    private String extractCharset(String collation) {
        if (collation == null) {
            return null;
        }
        int index = collation.indexOf('_');
        if (index > 0) {
            return collation.substring(0, index);
        }
        return collation;
    }
}
