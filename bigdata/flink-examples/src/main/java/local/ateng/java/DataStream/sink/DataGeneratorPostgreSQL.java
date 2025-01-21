package local.ateng.java.DataStream.sink;

import local.ateng.java.function.MyGeneratorFunction;
import local.ateng.java.entity.UserInfoEntity;
import org.apache.flink.api.common.eventtime.WatermarkStrategy;
import org.apache.flink.api.common.typeinfo.TypeInformation;
import org.apache.flink.api.connector.source.util.ratelimit.RateLimiterStrategy;
import org.apache.flink.connector.datagen.source.DataGeneratorSource;
import org.apache.flink.connector.jdbc.JdbcConnectionOptions;
import org.apache.flink.connector.jdbc.JdbcExecutionOptions;
import org.apache.flink.connector.jdbc.JdbcSink;
import org.apache.flink.streaming.api.datastream.DataStreamSource;
import org.apache.flink.streaming.api.environment.StreamExecutionEnvironment;
import org.apache.flink.streaming.api.functions.sink.SinkFunction;

import java.sql.Timestamp;

/**
 * 数据生成连接器，用于生成模拟数据并将其输出到PostgreSQL中
 *
 * @author 孔余
 * @email 2385569970@qq.com
 * @since 2025-01-14
 */
public class DataGeneratorPostgreSQL {
    public static void main(String[] args) throws Exception {
        // 获取执行环境
        StreamExecutionEnvironment env = StreamExecutionEnvironment.getExecutionEnvironment();
        // 设置并行度为 1，仅用于简化示例
        env.setParallelism(1);

        // 创建数据生成器源，生成器函数为 MyGeneratorFunction，每秒生成 1000 条数据，速率限制为 3 条/秒
        DataGeneratorSource<UserInfoEntity> source = new DataGeneratorSource<>(
                new MyGeneratorFunction(),
                1000,
                RateLimiterStrategy.perSecond(3),
                TypeInformation.of(UserInfoEntity.class)
        );

        // 将数据生成器源添加到流中
        DataStreamSource<UserInfoEntity> stream =
                env.fromSource(source,
                        WatermarkStrategy.noWatermarks(), // 不生成水印，仅用于演示
                        "Generator Source");

        /**
         * 写入mysql
         * JDBCSink的4个参数:
         *    第一个参数： 执行的sql，一般就是 insert into
         *    第二个参数： 预编译sql， 对占位符填充值
         *    第三个参数： 执行选项 ---》 攒批、重试
         *    第四个参数： 连接选项 ---》 url、用户名、密码
         */
        SinkFunction<UserInfoEntity> jdbcSink = JdbcSink.sink(
                "insert into my_user(name,age,score,birthday,province,city) values(?,?,?,?,?,?)",
                (preparedStatement, user) -> {
                    //每收到一条UserInfoEntity，如何去填充占位符
                    preparedStatement.setString(1, user.getName());
                    preparedStatement.setInt(2, user.getAge());
                    preparedStatement.setDouble(3, user.getScore());
                    preparedStatement.setTimestamp(4, new Timestamp(user.getBirthday().getTime()));
                    preparedStatement.setString(5, user.getProvince());
                    preparedStatement.setString(6, user.getCity());
                },
                JdbcExecutionOptions.builder()
                        .withMaxRetries(3) // 重试次数
                        .withBatchSize(1000) // 批次的大小：条数
                        .withBatchIntervalMs(3000) // 批次的时间
                        .build(),
                new JdbcConnectionOptions.JdbcConnectionOptionsBuilder()
                        .withUrl("jdbc:postgresql://192.168.1.10:32297/kongyu_flink?currentSchema=public")
                        .withUsername("postgres")
                        .withPassword("Lingo@local_postgresql_5432")
                        .withDriverName("org.postgresql.Driver")
                        .withConnectionCheckTimeoutSeconds(60) // 重试的超时时间
                        .build()
        );

        // 将数据打印到控制台
        stream.print("sink postgres");
        // 打印流中的数据
        stream.addSink(jdbcSink);

        // 执行 Flink 作业
        env.execute("生成模拟数据并写入PostgreSQL");
    }
}
