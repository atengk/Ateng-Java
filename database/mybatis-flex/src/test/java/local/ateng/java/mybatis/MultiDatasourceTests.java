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

}
