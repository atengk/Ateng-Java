package local.ateng.java.mybatis;

import com.mybatisflex.core.datasource.DataSourceKey;
import com.mybatisflex.core.row.Db;
import com.mybatisflex.core.row.Row;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.List;

@SpringBootTest
public class MultiDatasourceTests {

    @Test
    void test01() {
        try {
            DataSourceKey.use("doris");
            List<Row> rows = Db.selectAll("example_tbl_unique");
            System.out.println(rows);
        } finally {
            DataSourceKey.clear();
        }
    }

    @Test
    void test02() {
        // 测试Doris的Arrow Flight SQL协议
        try {
            DataSourceKey.use("doris-arrow-flight");
            List<Row> list = Db.selectListBySql("select * from kongyu.user_info");
            System.out.println(list);
        } finally {
            DataSourceKey.clear();
        }
    }

}
