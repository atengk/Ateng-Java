package local.ateng.java.spark.sql.udf;

import org.apache.spark.sql.api.java.UDF1;

/**
 * 小写转大写
 * 一元UDF
 *
 * @author 孔余
 * @email 2385569970@qq.com
 * @since 2025-01-24
 */
public class UpperCaseUDF implements UDF1<String, String> {
    @Override
    public String call(String input) throws Exception {
        return input == null ? "" : input.toUpperCase();
    }
}
