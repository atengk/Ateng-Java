package local.ateng.java.DataStream.operator;

import local.ateng.java.entity.UserInfoEntity;
import local.ateng.java.function.MyGeneratorFunction;
import org.apache.flink.api.common.eventtime.WatermarkStrategy;
import org.apache.flink.api.common.typeinfo.TypeInformation;
import org.apache.flink.api.connector.source.util.ratelimit.RateLimiterStrategy;
import org.apache.flink.api.java.tuple.Tuple2;
import org.apache.flink.connector.datagen.source.DataGeneratorSource;
import org.apache.flink.streaming.api.datastream.SideOutputDataStream;
import org.apache.flink.streaming.api.datastream.SingleOutputStreamOperator;
import org.apache.flink.streaming.api.environment.StreamExecutionEnvironment;
import org.apache.flink.streaming.api.functions.ProcessFunction;
import org.apache.flink.util.Collector;
import org.apache.flink.util.OutputTag;

/**
 * 旁路输出
 * 除了由 DataStream 操作产生的主要流之外，你还可以产生任意数量的旁路输出结果流。
 * 结果流中的数据类型不必与主要流中的数据类型相匹配，并且不同旁路输出的类型也可以不同。
 * 当你需要拆分数据流时，通常必须复制该数据流，然后从每个流中过滤掉不需要的数据，这个操作十分有用。
 *
 * @author 孔余
 * @since 2025-01-19
 */
public class OperatorSideOutput {
    public static void main(String[] args) throws Exception {
        // 获取执行环境
        StreamExecutionEnvironment env = StreamExecutionEnvironment.getExecutionEnvironment();
        // Kafka的Topic有多少个分区就设置多少并行度（可以设置为分区的倍数），例如：Topic有3个分区就设置并行度为3
        env.setParallelism(3);

        // 创建 DataGeneratorSource 生成模拟数据
        DataGeneratorSource<UserInfoEntity> source = new DataGeneratorSource<>(
                new MyGeneratorFunction(), // 自定义的生成器函数
                Long.MAX_VALUE, // 生成数据的数量
                RateLimiterStrategy.perSecond(10), // 生成数据的速率限制
                TypeInformation.of(UserInfoEntity.class) // 数据类型信息
        );

        // 定义用于标识旁路输出流的 OutputTag
        OutputTag<String> outputTag = new OutputTag<String>("side-output") {
        };

        // 将生成的 UserInfoEntity 对象转换为 JSON 字符串
        SingleOutputStreamOperator<Tuple2<String, Integer>> stream = env
                .fromSource(source, WatermarkStrategy.noWatermarks(), "Generator Source")
                .process(new ProcessFunction<UserInfoEntity, Tuple2<String, Integer>>() {
                    @Override
                    public void processElement(UserInfoEntity userInfoEntity, Context context, Collector<Tuple2<String, Integer>> collector) throws Exception {
                        // 正常流的输出
                        String province = userInfoEntity.getProvince();
                        Integer age = userInfoEntity.getAge();
                        Tuple2<String, Integer> tuple2 = new Tuple2<>(province, age);
                        if (age == 25) {
                            // 旁路输出流
                            context.output(outputTag, tuple2.toString());
                        } else {
                            // 正常流
                            collector.collect(tuple2);
                        }
                    }
                });

        // 打印正常流数据
        stream.print("output");

        // 获取旁路输出流
        SideOutputDataStream<String> sideStream = stream.getSideOutput(outputTag);
        // 打印旁路输出流数据
        sideStream.print("side-output");

        // 执行程序
        env.execute("SideOutput旁路输出的使用");
    }
}
