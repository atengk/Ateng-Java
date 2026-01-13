package io.github.atengk.beetl;

import cn.hutool.json.JSONUtil;
import io.github.atengk.beetl.model.TableMeta;
import io.github.atengk.beetl.service.DynamicDdlService;
import io.github.atengk.beetl.utils.TablePGUtil;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;

@SpringBootTest
public class PostgreSQLTests {
    @Qualifier("postgresqlJdbcTemplate")
    @Autowired
    private JdbcTemplate jdbcTemplate;
    @Autowired
    private DynamicDdlService dynamicDdlService;

    @Test
    public void test() {
        TableMeta tableMeta = TablePGUtil.loadTableMeta(jdbcTemplate, "project");
        System.out.println(JSONUtil.toJsonPrettyStr(tableMeta));
    }

    @Test
    public void test2() {
        TableMeta tableMeta = TablePGUtil.loadTableMeta(jdbcTemplate, "project");
        tableMeta.setIfNotExists(true);
        dynamicDdlService.createTable(jdbcTemplate.getDataSource(), tableMeta);
    }

}
