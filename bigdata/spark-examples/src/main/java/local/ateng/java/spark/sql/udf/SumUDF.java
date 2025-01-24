package local.ateng.java.spark.sql.udf;

import org.apache.spark.sql.api.java.UDF2;

/**
 * 两列数据的和
 * 二元UDF
 *
 * @author 孔余
 * @email 2385569970@qq.com
 * @since 2025-01-24
 */
public class SumUDF implements UDF2<Integer, Integer, Integer> {
    @Override
    public Integer call(Integer t1, Integer t2) throws Exception {
        return t1 + t2;
    }
}
