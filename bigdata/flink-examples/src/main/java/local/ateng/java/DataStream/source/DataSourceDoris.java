package local.ateng.java.DataStream.source;

import org.apache.doris.flink.cfg.DorisOptions;
import org.apache.doris.flink.cfg.DorisReadOptions;
import org.apache.doris.flink.deserialization.SimpleListDeserializationSchema;
import org.apache.doris.flink.source.DorisSource;
import org.apache.flink.api.common.RuntimeExecutionMode;
import org.apache.flink.api.common.eventtime.WatermarkStrategy;
import org.apache.flink.connector.file.src.FileSource;
import org.apache.flink.connector.file.src.reader.TextLineInputFormat;
import org.apache.flink.core.fs.Path;
import org.apache.flink.streaming.api.CheckpointingMode;
import org.apache.flink.streaming.api.datastream.DataStream;
import org.apache.flink.streaming.api.datastream.DataStreamSource;
import org.apache.flink.streaming.api.environment.StreamExecutionEnvironment;

import java.time.Duration;
import java.util.List;

/**
 * 读取Doris
 *
 * @author 孔余
 * @email 2385569970@qq.com
 * @since 2025-01-15
 */
public class DataSourceDoris {

    public static void main(String[] args) throws Exception {
        // 获取执行环境
        StreamExecutionEnvironment env = StreamExecutionEnvironment.getExecutionEnvironment();
        // 设置运行模式为批处理模式
        env.setRuntimeMode(RuntimeExecutionMode.BATCH);
        // 设置并行度为 1
        env.setParallelism(1);

        // 创建 Doris 数据源
        DorisOptions.Builder builder = DorisOptions.builder()
                .setFenodes("192.168.1.12:9040")
                .setTableIdentifier("kongyu_flink.my_user") // db.table
                .setUsername("admin")
                .setPassword("Admin@123");

        DorisSource<List<?>> dorisSource = DorisSource.<List<?>>builder()
                .setDorisOptions(builder.build())
                .setDorisReadOptions(DorisReadOptions.builder().build())
                .setDeserializer(new SimpleListDeserializationSchema())
                .build();

        // 从 Source 中读取数据
        DataStreamSource<List<?>> stream = env.fromSource(dorisSource, WatermarkStrategy.noWatermarks(), "Doris Source");

        // 输出流数据
        stream.print("output");

        // 执行程序
        env.execute("Doris Source");
    }

}
