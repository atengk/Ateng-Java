package local.ateng.java.DataStream.sink;

import cn.hutool.core.date.DateUtil;
import local.ateng.java.entity.UserInfoEntity;
import local.ateng.java.function.MyGeneratorFunction;
import org.apache.flink.api.common.eventtime.WatermarkStrategy;
import org.apache.flink.api.common.typeinfo.TypeInformation;
import org.apache.flink.api.connector.source.util.ratelimit.RateLimiterStrategy;
import org.apache.flink.connector.datagen.source.DataGeneratorSource;
import org.apache.flink.connector.hbase.sink.HBaseMutationConverter;
import org.apache.flink.connector.hbase.sink.HBaseSinkFunction;
import org.apache.flink.streaming.api.datastream.SingleOutputStreamOperator;
import org.apache.flink.streaming.api.environment.StreamExecutionEnvironment;
import org.apache.hadoop.hbase.HBaseConfiguration;
import org.apache.hadoop.hbase.client.Mutation;
import org.apache.hadoop.hbase.client.Put;
import org.apache.hadoop.hbase.util.Bytes;

import java.nio.charset.StandardCharsets;

/**
 * 写入数据到HBase
 *
 * @author 孔余
 * @email 2385569970@qq.com
 * @since 2025-01-16
 */
public class DataGeneratorHBase {
    public static void main(String[] args) throws Exception {
        // 获取执行环境
        StreamExecutionEnvironment env = StreamExecutionEnvironment.getExecutionEnvironment();
        // 设置并行度为 1，仅用于简化示例
        env.setParallelism(1);

        // 创建 DataGeneratorSource 生成模拟数据
        DataGeneratorSource<UserInfoEntity> source = new DataGeneratorSource<>(
                new MyGeneratorFunction(), // 自定义的生成器函数
                Long.MAX_VALUE, // 生成数据的数量
                RateLimiterStrategy.perSecond(10), // 生成数据的速率限制
                TypeInformation.of(UserInfoEntity.class) // 数据类型信息
        );

        // 将生成的 UserInfoEntity 对象转换为 JSON 字符串
        SingleOutputStreamOperator<UserInfoEntity> stream = env
                .fromSource(source, WatermarkStrategy.noWatermarks(), "Generator Source");

        // 配置 HBase
        String tableName = "user_info_table";  // HBase 表名
        String zkQuorum = "server01,server02,server03";  // Zookeeper 地址
        String zkPort = "2181"; // Zookeeper 端口

        // 创建 HBase 配置对象
        org.apache.hadoop.conf.Configuration hbaseConfig = HBaseConfiguration.create();
        hbaseConfig.set("hbase.zookeeper.quorum", zkQuorum); // 设置 Zookeeper quorum 地址
        hbaseConfig.set("hbase.zookeeper.property.clientPort", zkPort); // 设置 Zookeeper 端口

        // 使用 HBaseSinkFunction 连接器
        HBaseSinkFunction<UserInfoEntity> hbaseSink = new HBaseSinkFunction<>(
                tableName,  // 设置 HBase 表名
                hbaseConfig, // 配置对象
                new HBaseMutationConverter<UserInfoEntity>() {
                    @Override
                    public void open() {

                    }

                    @Override
                    public Mutation convertToMutation(UserInfoEntity userInfoEntity) {
                        // 将 UserInfoEntity 转换为 HBase Put 操作
                        Put put = new Put(String.valueOf(userInfoEntity.getId()).getBytes(StandardCharsets.UTF_8)); // 使用 userId 作为行键（row key）

                        // 假设我们有一个列族 'cf'，并且需要写入 'user_name' 和 'email' 列
                        put.addColumn(Bytes.toBytes("cf"), Bytes.toBytes("name"), Bytes.toBytes(userInfoEntity.getName()));
                        put.addColumn(Bytes.toBytes("cf"), Bytes.toBytes("age"), String.valueOf(userInfoEntity.getAge()).getBytes(StandardCharsets.UTF_8));
                        put.addColumn(Bytes.toBytes("cf"), Bytes.toBytes("score"), String.valueOf(userInfoEntity.getScore()).getBytes(StandardCharsets.UTF_8));
                        put.addColumn(Bytes.toBytes("cf"), Bytes.toBytes("province"), Bytes.toBytes(userInfoEntity.getProvince()));
                        put.addColumn(Bytes.toBytes("cf"), Bytes.toBytes("city"), Bytes.toBytes(userInfoEntity.getCity()));
                        put.addColumn(Bytes.toBytes("cf"), Bytes.toBytes("birthday"), DateUtil.format(userInfoEntity.getBirthday(), "yyyy-MM-dd HH:mm:ss.SSS").getBytes(StandardCharsets.UTF_8));
                        put.addColumn(Bytes.toBytes("cf"), Bytes.toBytes("create_time"), DateUtil.format(userInfoEntity.getCreateTime(), "yyyy-MM-dd HH:mm:ss.SSS").getBytes(StandardCharsets.UTF_8));

                        return put; // 返回 Mutation 对象，这里是 Put 操作
                    }
                }, 1000, 100, 100
        );

        // 将数据打印到控制台
        stream.print("sink hbase");
        // 将数据发送到 HBase
        stream.addSink(hbaseSink);

        // 执行程序
        env.execute("生成模拟数据并写入HBase");
    }
}
