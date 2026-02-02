package local.ateng.java.hutool;

import cn.hutool.db.ds.simple.SimpleDataSource;
import cn.hutool.db.meta.MetaUtil;
import cn.hutool.db.meta.Table;
import cn.hutool.json.JSONUtil;
import org.junit.jupiter.api.Test;

import javax.sql.DataSource;
import java.util.List;

/**
 * Hutool MetaUtil 元数据读取测试
 */
public class MetaUtilTests {

    /**
     * 测试：读取 MySQL 数据库中的表名列表及指定表的元数据
     */
    @Test
    void should_read_mysql_table_metadata() {
        DataSource dataSource = createMysqlDataSource();

        List<String> tableNames = MetaUtil.getTables(dataSource);
        System.out.println("MySQL 表列表：");
        System.out.println(JSONUtil.toJsonStr(tableNames));

        Table tableMeta = MetaUtil.getTableMeta(dataSource, "project");
        System.out.println("MySQL 表元数据：");
        System.out.println(JSONUtil.toJsonStr(tableMeta));
    }

    /**
     * 创建 MySQL 数据源
     *
     * @return MySQL DataSource
     */
    private DataSource createMysqlDataSource() {
        return new SimpleDataSource(
                "jdbc:mysql://175.178.193.128:20001/kongyu",
                "root",
                "Admin@123"
        );
    }

    /**
     * 测试：读取 PostgreSQL 数据库中的表名列表及指定表的元数据
     */
    @Test
    void should_read_postgresql_table_metadata() {
        DataSource dataSource = createPostgreSqlDataSource();

        List<String> tableNames = MetaUtil.getTables(dataSource);
        System.out.println("PostgreSQL 表列表：");
        System.out.println(JSONUtil.toJsonStr(tableNames));

        Table tableMeta = MetaUtil.getTableMeta(dataSource, "project");
        System.out.println("PostgreSQL 表元数据：");
        System.out.println(JSONUtil.toJsonStr(tableMeta));
    }

    /**
     * 创建 PostgreSQL 数据源
     *
     * @return PostgreSQL DataSource
     */
    private DataSource createPostgreSqlDataSource() {
        return new SimpleDataSource(
                "jdbc:postgresql://175.178.193.128:20002/kongyu",
                "postgres",
                "Admin@123"
        );
    }

}
