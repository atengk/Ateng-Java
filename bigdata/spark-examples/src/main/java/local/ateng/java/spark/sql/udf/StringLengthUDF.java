package local.ateng.java.spark.sql.udf;

import org.apache.spark.sql.api.java.UDF1;

/**
 * 数据长度
 * 一元UDF
 *
 * @author 孔余
 * @email 2385569970@qq.com
 * @since 2025-01-24
 */
public class StringLengthUDF implements UDF1<String, Integer> {

    @Override
    public Integer call(String input) throws Exception {
        return input == null ? 0 : input.length();
    }
}
