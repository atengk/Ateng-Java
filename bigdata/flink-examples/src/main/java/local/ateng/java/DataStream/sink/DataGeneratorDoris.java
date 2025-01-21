package local.ateng.java.DataStream.sink;

import cn.hutool.core.bean.BeanUtil;
import com.alibaba.fastjson2.JSONObject;
import local.ateng.java.entity.UserInfoEntity;
import local.ateng.java.function.MyGeneratorFunction;
import org.apache.doris.flink.cfg.DorisExecutionOptions;
import org.apache.doris.flink.cfg.DorisOptions;
import org.apache.doris.flink.cfg.DorisReadOptions;
import org.apache.doris.flink.sink.DorisSink;
import org.apache.doris.flink.sink.writer.serializer.SimpleStringSerializer;
import org.apache.flink.api.common.eventtime.WatermarkStrategy;
import org.apache.flink.api.common.typeinfo.TypeInformation;
import org.apache.flink.api.connector.source.util.ratelimit.RateLimiterStrategy;
import org.apache.flink.connector.datagen.source.DataGeneratorSource;
import org.apache.flink.streaming.api.CheckpointingMode;
import org.apache.flink.streaming.api.datastream.SingleOutputStreamOperator;
import org.apache.flink.streaming.api.environment.StreamExecutionEnvironment;

import java.time.LocalDateTime;
import java.util.Properties;


/**
 * 数据生成连接器，用于生成模拟数据并将其输出到Doris中
 *
 * @author 孔余
 * @email 2385569970@qq.com
 * @since 2025-01-15
 */
public class DataGeneratorDoris {
    public static void main(String[] args) throws Exception {
        // 获取执行环境
        StreamExecutionEnvironment env = StreamExecutionEnvironment.getExecutionEnvironment();
        // 设置并行度为 1，仅用于简化示例
        env.setParallelism(1);
        // 启用检查点，设置检查点间隔为 10 秒，检查点模式为 精准一次
        env.enableCheckpointing(10 * 1000, CheckpointingMode.EXACTLY_ONCE);

        // 创建数据生成器源，生成器函数为 MyGeneratorFunction，每秒生成 1000 条数据，速率限制为 3 条/秒
        DataGeneratorSource<UserInfoEntity> source = new DataGeneratorSource<>(
                new MyGeneratorFunction(),
                1000,
                RateLimiterStrategy.perSecond(3),
                TypeInformation.of(UserInfoEntity.class)
        );

        // 将数据生成器源添加到流中
        SingleOutputStreamOperator<String> stream = env
                .fromSource(source, WatermarkStrategy.noWatermarks(), "Generator Source")
                .map(user -> {
                    // 转换成和 Doris 表字段一致
                    LocalDateTime createTime = user.getCreateTime();
                    user.setCreateTime(null);
                    JSONObject jsonObject = BeanUtil.toBean(user, JSONObject.class);
                    jsonObject.put("create_time", createTime);
                    return JSONObject.toJSONString(jsonObject);
                });

        // Sink
        DorisSink.Builder<String> builder = DorisSink.builder();
        DorisOptions.Builder dorisBuilder = DorisOptions.builder();
        // doris 相关信息
        dorisBuilder.setFenodes("192.168.1.12:9040")
                .setTableIdentifier("kongyu_flink.my_user") // db.table
                .setUsername("admin")
                .setPassword("Admin@123");
        Properties properties = new Properties();
        // JSON 格式需要设置的参数
        properties.setProperty("format", "json");
        properties.setProperty("read_json_by_line", "true");
        DorisExecutionOptions.Builder executionBuilder = DorisExecutionOptions.builder();
        // Stream load 导入使用的 label 前缀。2pc 场景下要求全局唯一，用来保证 Flink 的 EOS 语义。
        executionBuilder.setLabelPrefix("label-doris2")
                .setDeletable(false)
                .setStreamLoadProp(properties);
        builder.setDorisReadOptions(DorisReadOptions.builder().build())
                .setDorisExecutionOptions(executionBuilder.build())
                .setSerializer(new SimpleStringSerializer())
                .setDorisOptions(dorisBuilder.build());
        DorisSink<String> sink = builder.build();

        // 将数据打印到控制台
        stream.print("sink doris");
        // 打印流中的数据
        stream.sinkTo(sink);

        // 执行 Flink 作业
        env.execute("生成模拟数据并写入Doris");
    }
}
