package io.github.atengk.beetl.service;

import cn.hutool.db.sql.SqlUtil;
import io.github.atengk.beetl.enums.DatabaseType;
import io.github.atengk.beetl.model.TableMeta;
import io.github.atengk.beetl.utils.TableUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.beetl.core.GroupTemplate;
import org.beetl.core.Template;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.SQLException;

/**
 * 动态 DDL 服务
 *
 * @author 孔余
 * @since 2025-12-18
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class DynamicDdlService {


    private final GroupTemplate groupTemplate;

    /**
     * 创建表
     *
     * @param dataSource 数据源
     * @param tableMeta  表结构
     */
    public void createTable(DataSource dataSource, TableMeta tableMeta) {

        TableUtil.normalizePrimaryIndex(tableMeta);

        DatabaseType databaseType = resolveDatabaseType(dataSource);
        String templatePath = resolveCreateTableTemplate(databaseType);

        Template template = groupTemplate.getTemplate(templatePath);
        template.binding("table", tableMeta);

        String ddl = template.render();
        log.info("创建表DDL [{}]:\n{}", databaseType, SqlUtil.formatSql(ddl));

        JdbcTemplate jdbcTemplate = new JdbcTemplate(dataSource);
        jdbcTemplate.execute(ddl);
    }

    /**
     * 解析数据库类型
     *
     * @param dataSource 数据源
     * @return 数据库类型
     */
    private DatabaseType resolveDatabaseType(DataSource dataSource) {
        try (Connection connection = dataSource.getConnection()) {
            DatabaseMetaData metaData = connection.getMetaData();
            return DatabaseType.fromProductName(metaData.getDatabaseProductName());
        } catch (SQLException e) {
            throw new IllegalStateException("Failed to resolve database type", e);
        }
    }

    /**
     * 获取建表模板路径
     *
     * @param databaseType 数据库类型
     * @return 模板路径
     */
    private String resolveCreateTableTemplate(DatabaseType databaseType) {
        switch (databaseType) {
            case MYSQL:
                return "ddl/mysql/create_table.tbl";
            case POSTGRESQL:
                return "ddl/postgresql/create_table.tbl";
            default:
                throw new UnsupportedOperationException(
                        "Create table not supported for database type: " + databaseType
                );
        }
    }
}
