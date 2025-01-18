# Flink开发文档

Flink 是一个开源的分布式流处理框架，专注于大规模数据流的实时处理。它提供了高吞吐量、低延迟的处理能力，支持有状态和无状态的数据流操作。Flink 可以处理事件时间、窗口化、流与批处理混合等复杂场景，广泛应用于实时数据分析、实时监控、机器学习等领域。其强大的容错机制和高可扩展性，使其成为大数据领域中的重要技术之一。

- [官网链接](https://nightlies.apache.org/flink/flink-docs-release-1.19/docs/dev/datastream/overview/)



## 基础配置

### 创建项目

创建Maven项目，IDEA配置该项目SDK为JDK8、Maven的JRE也配置文JDK8、应用程序配置需要设置 `provided` 作用域

![image-20250114171619635](./assets/image-20250114171619635.png)



### 配置pom.xml

```xml
<?xml version="1.0" encoding="UTF-8"?>
<project xmlns="http://maven.apache.org/POM/4.0.0"
         xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
         xsi:schemaLocation="http://maven.apache.org/POM/4.0.0 http://maven.apache.org/xsd/maven-4.0.0.xsd">
    <!-- 项目模型版本 -->
    <modelVersion>4.0.0</modelVersion>

    <!-- 项目坐标 -->
    <groupId>local.ateng.java</groupId>
    <artifactId>flink-examples</artifactId>
    <version>v1.0</version>
    <name>flink-examples</name>
    <description>
        Maven项目使用Java8对的Flink使用
    </description>

    <!-- 项目属性 -->
    <properties>
        <!-- 默认主程序 -->
        <start-class>local.ateng.java.WindowWordCount</start-class>
        <java.version>8</java.version>
        <project.build.sourceEncoding>UTF-8</project.build.sourceEncoding>
        <project.reporting.outputEncoding>UTF-8</project.reporting.outputEncoding>
        <maven-compiler.version>3.12.1</maven-compiler.version>
        <maven-shade.version>3.5.1</maven-shade.version>
        <lombok.version>1.18.36</lombok.version>
        <fastjson2.version>2.0.53</fastjson2.version>
        <hutool.version>5.8.35</hutool.version>
        <hadoop.version>3.3.6</hadoop.version>
        <flink.version>1.19.1</flink.version>
    </properties>

    <!-- 项目依赖 -->
    <dependencies>

        <!-- Hutool: Java工具库，提供了许多实用的工具方法 -->
        <dependency>
            <groupId>cn.hutool</groupId>
            <artifactId>hutool-all</artifactId>
            <version>${hutool.version}</version>
        </dependency>

        <!-- Lombok: 简化Java代码编写的依赖项 -->
        <!-- https://mvnrepository.com/artifact/org.projectlombok/lombok -->
        <dependency>
            <groupId>org.projectlombok</groupId>
            <artifactId>lombok</artifactId>
            <version>${lombok.version}</version>
            <scope>provided</scope>
        </dependency>

        <!-- 高性能的JSON库 -->
        <!-- https://github.com/alibaba/fastjson2/wiki/fastjson2_intro_cn#0-fastjson-20%E4%BB%8B%E7%BB%8D -->
        <dependency>
            <groupId>com.alibaba.fastjson2</groupId>
            <artifactId>fastjson2</artifactId>
            <version>${fastjson2.version}</version>
        </dependency>

        <!-- JavaFaker: 用于生成虚假数据的Java库 -->
        <dependency>
            <groupId>com.github.javafaker</groupId>
            <artifactId>javafaker</artifactId>
            <version>1.0.2</version>
        </dependency>

        <!-- SLF4J API -->
        <dependency>
            <groupId>org.slf4j</groupId>
            <artifactId>slf4j-api</artifactId>
            <version>1.7.36</version>
            <scope>provided</scope>
        </dependency>
        <!-- Log4j 2.x API -->
        <dependency>
            <groupId>org.apache.logging.log4j</groupId>
            <artifactId>log4j-api</artifactId>
            <version>2.19.0</version>
            <scope>provided</scope>
        </dependency>
        <!-- Log4j 2.x 实现 -->
        <dependency>
            <groupId>org.apache.logging.log4j</groupId>
            <artifactId>log4j-core</artifactId>
            <version>2.19.0</version>
            <scope>provided</scope>
        </dependency>
        <!-- SLF4J 和 Log4j 2.x 绑定 -->
        <dependency>
            <groupId>org.apache.logging.log4j</groupId>
            <artifactId>log4j-slf4j-impl</artifactId>
            <version>2.19.0</version>
            <scope>provided</scope>
        </dependency>

        <!-- Apache Flink 客户端库 -->
        <dependency>
            <groupId>org.apache.flink</groupId>
            <artifactId>flink-clients</artifactId>
            <version>${flink.version}</version>
            <scope>provided</scope>
        </dependency>

    </dependencies>

    <!-- 插件仓库配置 -->
    <repositories>
        <!-- Central Repository -->
        <repository>
            <id>central</id>
            <name>阿里云中央仓库</name>
            <url>https://maven.aliyun.com/repository/central</url>
            <!--<name>Maven官方中央仓库</name>
            <url>https://repo.maven.apache.org/maven2/</url>-->
        </repository>
    </repositories>

    <!-- 构建配置 -->
    <build>
        <finalName>${project.name}-${project.version}</finalName>
        <plugins>
            <!-- Maven 编译插件 -->
            <plugin>
                <groupId>org.apache.maven.plugins</groupId>
                <artifactId>maven-compiler-plugin</artifactId>
                <version>${maven-compiler.version}</version>
                <configuration>
                    <source>${java.version}</source>
                    <target>${java.version}</target>
                    <encoding>${project.build.sourceEncoding}</encoding>
                </configuration>
            </plugin>
            <!-- Maven Shade 打包插件 -->
            <!-- https://maven.apache.org/plugins/maven-shade-plugin/shade-mojo.html -->
            <plugin>
                <groupId>org.apache.maven.plugins</groupId>
                <artifactId>maven-shade-plugin</artifactId>
                <version>${maven-shade.version}</version>
                <executions>
                    <execution>
                        <phase>package</phase>
                        <goals>
                            <goal>shade</goal>
                        </goals>
                        <configuration>
                            <!-- 禁用生成 dependency-reduced-pom.xml 文件 -->
                            <createDependencyReducedPom>false</createDependencyReducedPom>
                            <!-- 附加shaded工件时使用的分类器的名称 -->
                            <shadedClassifierName>shaded</shadedClassifierName>
                            <transformers>
                                <transformer
                                        implementation="org.apache.maven.plugins.shade.resource.ManifestResourceTransformer">
                                    <!-- 指定默认主程序 -->
                                    <mainClass>${start-class}</mainClass>
                                </transformer>
                                <transformer implementation="org.apache.maven.plugins.shade.resource.ServicesResourceTransformer"/>
                            </transformers>
                            <artifactSet>
                                <!-- 排除依赖项 -->
                                <excludes>
                                    <exclude>org.apache.logging.log4j:*</exclude>
                                    <exclude>org.slf4j:*</exclude>
                                    <exclude>com.google.code.findbugs:jsr305</exclude>
                                </excludes>
                            </artifactSet>
                            <filters>
                                <!-- 不复制 META-INF 下的签名文件 -->
                                <filter>
                                    <artifact>*:*</artifact>
                                    <excludes>
                                        <exclude>module-info.class</exclude>
                                        <exclude>META-INF/*.SF</exclude>
                                        <exclude>META-INF/*.MF</exclude>
                                        <exclude>META-INF/*.DSA</exclude>
                                        <exclude>META-INF/*.RSA</exclude>
                                        <exclude>META-INF/*.txt</exclude>
                                        <exclude>META-INF/NOTICE</exclude>
                                        <exclude>META-INF/LICENSE</exclude>
                                        <exclude>META-INF/services/java.sql.Driver</exclude>
                                        <!-- 排除resources下的xml配置文件 -->
                                        <exclude>*.xml</exclude>
                                    </excludes>
                                </filter>
                            </filters>
                        </configuration>
                    </execution>
                </executions>
            </plugin>
        </plugins>
    </build>
</project>
```

### 配置log4j2.properties

在**resources**目录下创建log4j2的日志配置文件

```properties
# 配置日志格式
appender.console.name = ConsoleAppender
appender.console.type = CONSOLE
appender.console.layout.type = PatternLayout
appender.console.layout.pattern = %d{ISO8601} [%t] %-5level %logger{36} - %msg%n

# 定义根日志级别
rootLogger.level = INFO
rootLogger.appenderRefs = console
rootLogger.appenderRef.console.ref = ConsoleAppender

# Kafka
logger.kafka.name = org.apache.kafka
logger.kafka.level = ERROR
logger.kafka.appenderRefs = console
logger.kafka.appenderRef.console.ref = ConsoleAppender

# Flink
logger.flink.name = org.apache.flink
logger.flink.level = WARN
logger.flink.appenderRefs = console
logger.flink.appenderRef.console.ref = ConsoleAppender
```

### 使用WordCount

创建第一个Flink程序WordCount

socketTextStream的地址需要自行修改，在Linux服务器需要安装安装nc（`sudo yum -y install nc`），然后使用 `nc -lk 9999` 启动Socket。

启动程序后，在nc上输入字符就开始统计数据了

```java
package local.ateng.java;

import org.apache.flink.api.common.functions.FlatMapFunction;
import org.apache.flink.api.java.tuple.Tuple2;
import org.apache.flink.streaming.api.datastream.DataStream;
import org.apache.flink.streaming.api.environment.StreamExecutionEnvironment;
import java.time.Duration;

import org.apache.flink.streaming.api.windowing.assigners.TumblingProcessingTimeWindows;
import org.apache.flink.util.Collector;

public class WindowWordCount {

    public static void main(String[] args) throws Exception {

        StreamExecutionEnvironment env = StreamExecutionEnvironment.getExecutionEnvironment();

        DataStream<Tuple2<String, Integer>> dataStream = env
                .socketTextStream("192.168.1.12", 9999)
                .flatMap(new Splitter())
                .keyBy(value -> value.f0)
                .window(TumblingProcessingTimeWindows.of(Duration.ofSeconds(5)))
                .sum(1);

        dataStream.print();

        env.execute("Window WordCount");
    }

    public static class Splitter implements FlatMapFunction<String, Tuple2<String, Integer>> {
        @Override
        public void flatMap(String sentence, Collector<Tuple2<String, Integer>> out) throws Exception {
            for (String word: sentence.split(" ")) {
                out.collect(new Tuple2<String, Integer>(word, 1));
            }
        }
    }

}
```

在nc终端输入以下内容：

```
java python
python java
ateng java
java kongyu
```

Flink程序就输入以下内容：

![image-20250114172836279](./assets/image-20250114172836279.png)



## DataStream

Flink 中的 DataStream 程序是对数据流（例如过滤、更新状态、定义窗口、聚合）进行转换的常规程序。数据流的起始是从各种源（例如消息队列、套接字流、文件）创建的。结果通过 sink 返回，例如可以将数据写入文件或标准输出（例如命令行终端）。Flink 程序可以在各种上下文中运行，可以独立运行，也可以嵌入到其它程序中。任务执行可以运行在本地 JVM 中，也可以运行在多台机器的集群上。

DataStream API 得名于特殊的 DataStream 类，该类用于表示 Flink 程序中的数据集合。你可以认为 它们是可以包含重复项的不可变数据集合。这些数据可以是有界（有限）的，也可以是无界（无限）的，但用于处理它们的API是相同的。

DataStream 在用法上类似于常规的 Java 集合，但在某些关键方面却大不相同。它们是不可变的，这意味着一旦它们被创建，你就不能添加或删除元素。你也不能简单地察看内部元素，而只能使用 DataStream API 操作来处理它们，DataStream API 操作也叫作转换（transformation）。

你可以通过在 Flink 程序中添加 source 创建一个初始的 DataStream。然后，你可以基于 DataStream 派生新的流，并使用 map、filter 等 API 方法把 DataStream 和派生的流连接在一起。

- [官网地址](https://nightlies.apache.org/flink/flink-docs-release-1.19/zh/docs/dev/datastream/overview/)



### 数据生成

#### 创建实体类

```java
package local.ateng.java.entity;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.Date;

/**
 * 用户信息实体类
 * 用于表示系统中的用户信息。
 *
 * @author 孔余
 * @since 2024-01-10 15:51
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UserInfoEntity implements Serializable {
    private static final long serialVersionUID = 1L;

    /**
     * 用户ID
     */
    private Long id;

    /**
     * 用户姓名
     */
    private String name;

    /**
     * 用户年龄
     * 注意：这里使用Integer类型，表示年龄是一个整数值。
     */
    private Integer age;

    /**
     * 分数
     */
    private Double score;

    /**
     * 用户生日
     * 注意：这里使用Date类型，表示用户的生日。
     */
    private Date birthday;

    /**
     * 用户所在省份
     */
    private String province;

    /**
     * 用户所在城市
     */
    private String city;

    /**
     * 创建时间
     */
    private LocalDateTime createTime;
}
```

#### 创建生成器函数

```java
package local.ateng.java.function;

import com.github.javafaker.Faker;
import local.ateng.java.entity.UserInfoEntity;
import org.apache.flink.api.connector.source.SourceReaderContext;
import org.apache.flink.connector.datagen.source.GeneratorFunction;

import java.time.LocalDateTime;
import java.util.Locale;

/**
 * 生成器函数
 *
 * @author 孔余
 * @since 2024-02-29 17:07
 */
public class MyGeneratorFunction implements GeneratorFunction {
    // 创建一个Java Faker实例，指定Locale为中文
    private Faker faker;

    // 初始化随机数数据生成器
    @Override
    public void open(SourceReaderContext readerContext) throws Exception {
        faker = new Faker(new Locale("zh-CN"));
    }

    @Override
    public UserInfoEntity map(Object value) throws Exception {
        // 使用 随机数数据生成器 来创建实例
        UserInfoEntity user = UserInfoEntity.builder()
                .id(System.currentTimeMillis())
                .name(faker.name().fullName())
                .birthday(faker.date().birthday())
                .age(faker.number().numberBetween(0, 100))
                .province(faker.address().state())
                .city(faker.address().cityName())
                .score(faker.number().randomDouble(3, 1, 100))
                .createTime(LocalDateTime.now())
                .build();
        return user;
    }

}
```

#### 创建数据生成器

参考：[官方文档](https://nightlies.apache.org/flink/flink-docs-release-1.19/zh/docs/connectors/datastream/datagen/)

```java
package local.ateng.java.DataStream.sink;

import local.ateng.java.function.MyGeneratorFunction;
import local.ateng.java.entity.UserInfoEntity;
import org.apache.flink.api.common.eventtime.WatermarkStrategy;
import org.apache.flink.api.common.typeinfo.TypeInformation;
import org.apache.flink.api.connector.source.util.ratelimit.RateLimiterStrategy;
import org.apache.flink.connector.datagen.source.DataGeneratorSource;
import org.apache.flink.streaming.api.datastream.DataStreamSource;
import org.apache.flink.streaming.api.environment.StreamExecutionEnvironment;

/**
 * 数据生成连接器，用于生成模拟数据并将其输出到 Flink 流中。
 *
 * @author 孔余
 * @since 2024-02-29 16:55
 */
public class DataGenerator {
    public static void main(String[] args) throws Exception {
        // 获取执行环境
        StreamExecutionEnvironment env = StreamExecutionEnvironment.getExecutionEnvironment();
        // 设置并行度为 1，仅用于简化示例
        env.setParallelism(1);

        // 创建数据生成器源，生成器函数为 MyGeneratorFunction，生成 Long.MAX_VALUE 条数据，速率限制为 3 条/秒
        DataGeneratorSource<UserInfoEntity> source = new DataGeneratorSource<>(
                new MyGeneratorFunction(),
                Long.MAX_VALUE,
                RateLimiterStrategy.perSecond(3),
                TypeInformation.of(UserInfoEntity.class)
        );

        // 将数据生成器源添加到流中
        DataStreamSource<UserInfoEntity> stream =
                env.fromSource(source,
                        WatermarkStrategy.noWatermarks(), // 不生成水印，仅用于演示
                        "Generator Source");

        // 打印流中的数据
        stream.print();

        // 执行 Flink 作业
        env.execute();
    }
}
```

![image-20250114175146261](./assets/image-20250114175146261.png)



#### 创建数据生成器（Kafka）

参考：[官方文档](https://nightlies.apache.org/flink/flink-docs-release-1.19/zh/docs/connectors/datastream/kafka/)

**添加依赖**

```xml
    <properties>
        <flink-kafka.version>3.3.0-1.19</flink-kafka.version>
    </properties>

    <dependencies>
        <!-- Apache Flink 连接器基础库库 -->
        <dependency>
            <groupId>org.apache.flink</groupId>
            <artifactId>flink-connector-base</artifactId>
            <version>${flink.version}</version>
            <scope>provided</scope>
        </dependency>
        <!-- Apache Flink Kafka 连接器库 -->
        <dependency>
            <groupId>org.apache.flink</groupId>
            <artifactId>flink-connector-kafka</artifactId>
            <version>${flink-kafka.version}</version>
            <scope>provided</scope>
        </dependency>
    </dependencies>
```

**创建生成器**

```java
package local.ateng.java.DataStream.sink;

import com.alibaba.fastjson2.JSONObject;
import local.ateng.java.function.MyGeneratorFunction;
import local.ateng.java.entity.UserInfoEntity;
import org.apache.flink.api.common.eventtime.WatermarkStrategy;
import org.apache.flink.api.common.serialization.SimpleStringSchema;
import org.apache.flink.api.common.typeinfo.TypeInformation;
import org.apache.flink.api.connector.source.util.ratelimit.RateLimiterStrategy;
import org.apache.flink.connector.base.DeliveryGuarantee;
import org.apache.flink.connector.datagen.source.DataGeneratorSource;
import org.apache.flink.connector.kafka.sink.KafkaRecordSerializationSchema;
import org.apache.flink.connector.kafka.sink.KafkaSink;
import org.apache.flink.streaming.api.datastream.SingleOutputStreamOperator;
import org.apache.flink.streaming.api.environment.StreamExecutionEnvironment;

/**
 * 示例说明：
 * 此类演示了如何使用 Flink 的 DataGenConnector 和 KafkaSink 将生成的模拟数据发送到 Kafka 中。
 * 1. 创建 DataGeneratorSource 生成模拟数据；
 * 2. 使用 map 函数将 UserInfoEntity 转换为 JSON 字符串；
 * 3. 配置 KafkaSink 将数据发送到 Kafka 中。
 *
 * @author 孔余
 * @since 2024-02-29 16:55
 */
public class DataGeneratorKafka {
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

        // 将生成的 UserInfoEntity 对象转换为 JSON 字符串
        SingleOutputStreamOperator<String> stream = env
                .fromSource(source, WatermarkStrategy.noWatermarks(), "Generator Source")
                .map(user -> JSONObject.toJSONString(user));

        // 配置 KafkaSink 将数据发送到 Kafka 中
        KafkaSink<String> sink = KafkaSink.<String>builder()
                .setBootstrapServers("192.168.1.10:9094") // Kafka 服务器地址和端口
                .setRecordSerializer(KafkaRecordSerializationSchema.builder()
                        .setTopic("ateng_flink_json") // Kafka 主题
                        .setValueSerializationSchema(new SimpleStringSchema()) // 数据序列化方式
                        .build()
                )
                .setDeliveryGuarantee(DeliveryGuarantee.AT_LEAST_ONCE) // 传输保障级别
                .build();

        // 将数据打印到控制台
        stream.print("sink kafka");
        // 将数据发送到 Kafka
        stream.sinkTo(sink);

        // 执行程序
        env.execute();
    }
}
```

![image-20250114193523483](./assets/image-20250114193523483.png)

![image-20250114193549354](./assets/image-20250114193549354.png)



#### 创建数据生成器（Doris）

参考：[官方文档](https://doris.apache.org/zh-CN/docs/ecosystem/flink-doris-connector)

**添加依赖**

```xml
    <dependencies>
        <!-- Apache Flink Doris 连接器库 -->
        <dependency>
            <groupId>org.apache.doris</groupId>
            <artifactId>flink-doris-connector-1.19</artifactId>
            <version>24.1.0</version>
            <scope>provided</scope>
        </dependency>
    </dependencies>
```

**创建生成器**

注意重复执行需要修改 **LabelPrefix** ，详情参考[官方文档](https://doris.apache.org/zh-CN/docs/ecosystem/flink-doris-connector#sink-%E9%85%8D%E7%BD%AE%E9%A1%B9)

```sql
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
        env.execute();
    }
}
```

**创建表**

创建表

```sql
DROP TABLE IF EXISTS my_user;
CREATE TABLE IF NOT EXISTS my_user (
    id BIGINT NOT NULL,
    create_time DATETIME,
    name STRING,
    age INT,
    score DOUBLE,
    birthday DATETIME,
    province STRING,
    city STRING
)
DUPLICATE KEY (id, create_time)
DISTRIBUTED BY HASH(id) BUCKETS 10
PROPERTIES (
    "replication_allocation" = "tag.location.default: 1"
);
```

查看表

```sql
SHOW CREATE TABLE my_user\G;
```

查看数据

```sql
select * from my_user;
```



#### 创建数据生成器（MySQL）

参考：[官方文档](https://nightlies.apache.org/flink/flink-docs-release-1.19/zh/docs/connectors/datastream/jdbc/)

**添加依赖**

```xml
    <properties>
        <flink-jdbc.version>3.2.0-1.19</flink-jdbc.version>
    </properties>

    <dependencies>
		<!-- Apache Flink JDBC 连接器库 -->
        <dependency>
            <groupId>org.apache.flink</groupId>
            <artifactId>flink-connector-jdbc</artifactId>
            <version>${flink-jdbc.version}</version>
            <scope>provided</scope>
        </dependency>
        <!-- MySQL驱动 -->
        <dependency>
            <groupId>com.mysql</groupId>
            <artifactId>mysql-connector-j</artifactId>
            <version>8.0.33</version>
            <scope>provided</scope>
        </dependency>
    </dependencies>
```

**创建生成器**

```java
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
 * 数据生成连接器，用于生成模拟数据并将其输出到Mysql中。
 *
 * @author 孔余
 * @since 2024-03-06 16:11
 */
public class DataGeneratorMySQL {
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
                        .withUrl("jdbc:mysql://192.168.1.10:35725/kongyu_flink")
                        .withUsername("root")
                        .withPassword("Admin@123")
                        .withDriverName("com.mysql.cj.jdbc.Driver")
                        .withConnectionCheckTimeoutSeconds(60) // 重试的超时时间
                        .build()
        );

        // 将数据打印到控制台
        stream.print("sink mysql");
        // 打印流中的数据
        stream.addSink(jdbcSink);

        // 执行 Flink 作业
        env.execute();
    }
}
```

**创建表**

```sql
DROP TABLE IF EXISTS my_user;
CREATE TABLE IF NOT EXISTS my_user (
    id          BIGINT AUTO_INCREMENT PRIMARY KEY,          -- 用户唯一标识，主键自动递增
    name        VARCHAR(50) DEFAULT NULL,                   -- 增加name字段长度，并设置默认值为NULL
    age         INT DEFAULT NULL,                           -- 年龄，默认为NULL
    score       DOUBLE DEFAULT 0.0,                         -- 成绩，默认为0.0
    birthday    DATE DEFAULT NULL,                          -- 生日，默认为NULL
    province    VARCHAR(50) DEFAULT NULL,                   -- 省份，增加字段长度
    city        VARCHAR(50) DEFAULT NULL,                   -- 城市，增加字段长度
    create_time TIMESTAMP(3) DEFAULT CURRENT_TIMESTAMP(3),  -- 创建时间，默认当前时间
    INDEX idx_name (name),                                  -- 如果name经常用于查询，可以考虑为其加索引
    INDEX idx_province_city (province, city)                -- 如果经常根据省市进行查询，创建联合索引
);
```

![image-20250114195605661](./assets/image-20250114195605661.png)

![image-20250114195632746](./assets/image-20250114195632746.png)



#### 创建数据生成器（PostgreSQL）

参考：[官方文档](https://nightlies.apache.org/flink/flink-docs-release-1.19/zh/docs/connectors/datastream/jdbc/)

**添加依赖**

```xml
    <properties>
        <flink-jdbc.version>3.2.0-1.19</flink-jdbc.version>
    </properties>

    <dependencies>
		<!-- Apache Flink JDBC 连接器库 -->
        <dependency>
            <groupId>org.apache.flink</groupId>
            <artifactId>flink-connector-jdbc</artifactId>
            <version>${flink-jdbc.version}</version>
            <scope>provided</scope>
        </dependency>
        <!-- PostgreSQL驱动 -->
        <dependency>
            <groupId>org.postgresql</groupId>
            <artifactId>postgresql</artifactId>
            <version>42.7.1</version>
            <scope>provided</scope>
        </dependency>
    </dependencies>
```

**创建生成器**

```java
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
        env.execute();
    }
}
```

**创建表**

```sql
DROP TABLE IF EXISTS my_user;
CREATE TABLE IF NOT EXISTS my_user (
    id          BIGSERIAL PRIMARY KEY,                         -- BIGSERIAL 自动递增主键
    name        VARCHAR(50) DEFAULT NULL,                      -- 增加 name 字段长度，默认 NULL
    age         INTEGER DEFAULT NULL,                          -- 使用 INTEGER 替代 INT
    score       DOUBLE PRECISION DEFAULT 0.0,                  -- 使用 DOUBLE PRECISION
    birthday    DATE DEFAULT NULL,                             -- 生日字段，默认 NULL
    province    VARCHAR(50) DEFAULT NULL,                      -- 省份字段，增加长度
    city        VARCHAR(50) DEFAULT NULL,                      -- 城市字段，增加长度
    create_time TIMESTAMP(3) DEFAULT CURRENT_TIMESTAMP(3)      -- 创建时间字段，默认当前时间
);
-- 创建普通索引
CREATE INDEX idx_name ON my_user(name);                       -- 为 name 字段创建普通索引
CREATE INDEX idx_province_city ON my_user(province, city);     -- 为 province 和 city 字段创建联合索引
```

![image-20250114205417333](./assets/image-20250114205417333.png)

![image-20250114205434028](./assets/image-20250114205434028.png)



#### 创建数据生成器（MongoDB）

参考：[官方文档](https://nightlies.apache.org/flink/flink-docs-release-1.19/zh/docs/connectors/datastream/mongodb/)

**添加依赖**

```xml
    <properties>
        <flink-mongodb.version>1.2.0-1.19</flink-mongodb.version>
    </properties>

    <dependencies>
		<!-- Apache Flink MongoDB 连接器库 -->
        <dependency>
            <groupId>org.apache.flink</groupId>
            <artifactId>flink-connector-mongodb</artifactId>
            <version>${flink-mongodb.version}</version>
            <scope>provided</scope>
        </dependency>
    </dependencies>
```

**创建生成器**

```java
package local.ateng.java.DataStream.sink;

import com.alibaba.fastjson2.JSONObject;
import com.mongodb.client.model.InsertOneModel;
import local.ateng.java.function.MyGeneratorFunction;
import local.ateng.java.entity.UserInfoEntity;
import org.apache.flink.api.common.eventtime.WatermarkStrategy;
import org.apache.flink.api.common.typeinfo.TypeInformation;
import org.apache.flink.api.connector.source.util.ratelimit.RateLimiterStrategy;
import org.apache.flink.connector.base.DeliveryGuarantee;
import org.apache.flink.connector.datagen.source.DataGeneratorSource;
import org.apache.flink.connector.mongodb.sink.MongoSink;
import org.apache.flink.streaming.api.datastream.DataStreamSource;
import org.apache.flink.streaming.api.environment.StreamExecutionEnvironment;
import org.bson.BsonDocument;

/**
 * 数据生成连接器，用于生成模拟数据并将其输出到MongoDB中
 *
 * @author 孔余
 * @email 2385569970@qq.com
 * @since 2025-01-14
 */
public class DataGeneratorMongoDB {
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

        // MongoDB Sink
        MongoSink<UserInfoEntity> sink = MongoSink.<UserInfoEntity>builder()
                .setUri("mongodb://root:Admin%40123@192.168.1.10:33627")
                .setDatabase("kongyu_flink")
                .setCollection("my_user")
                .setBatchSize(1000)
                .setBatchIntervalMs(3000)
                .setMaxRetries(3)
                .setDeliveryGuarantee(DeliveryGuarantee.AT_LEAST_ONCE)
                .setSerializationSchema(
                        (input, context) -> new InsertOneModel<>(BsonDocument.parse(JSONObject.toJSONString(input))))
                .build();

        // 将数据打印到控制台
        stream.print("sink mongodb");
        // 写入数据
        stream.sinkTo(sink);

        // 执行 Flink 作业
        env.execute();
    }
}
```

![image-20250114210540277](./assets/image-20250114210540277.png)

![image-20250114210603464](./assets/image-20250114210603464.png)

#### 创建数据生成器（OpenSearch）

注意以下事项：

- 支持 **OpenSearch1.x** ，1.3.19 版本验证通过
- 支持 **ElasticSearch7.x** ，7.17.26 版本验证通过

- 不支持 OpenSearch2.x
- 不支持 ElasticSearch8.x

参考：[官方文档](https://nightlies.apache.org/flink/flink-docs-release-1.19/zh/docs/connectors/datastream/mongodb/)

**添加依赖**

```xml
    <properties>
        <flink-opensearch.version>1.2.0-1.19</flink-opensearch.version>
    </properties>

    <dependencies>
		<!-- Apache Flink OpenSearch 连接器库 -->
        <dependency>
            <groupId>org.apache.flink</groupId>
            <artifactId>flink-connector-opensearch</artifactId>
            <version>${flink-opensearch.version}</version>
            <scope>provided</scope>
        </dependency>
    </dependencies>
```

**创建生成器**

```java
package local.ateng.java.DataStream.sink;

import cn.hutool.core.bean.BeanUtil;
import local.ateng.java.function.MyGeneratorFunction;
import local.ateng.java.entity.UserInfoEntity;
import org.apache.flink.api.common.eventtime.WatermarkStrategy;
import org.apache.flink.api.common.typeinfo.TypeInformation;
import org.apache.flink.api.connector.source.util.ratelimit.RateLimiterStrategy;
import org.apache.flink.connector.datagen.source.DataGeneratorSource;
import org.apache.flink.connector.opensearch.sink.FlushBackoffType;
import org.apache.flink.connector.opensearch.sink.OpensearchSink;
import org.apache.flink.connector.opensearch.sink.OpensearchSinkBuilder;
import org.apache.flink.streaming.api.datastream.DataStreamSource;
import org.apache.flink.streaming.api.environment.StreamExecutionEnvironment;
import org.apache.http.HttpHost;
import org.opensearch.action.index.IndexRequest;
import org.opensearch.client.Requests;

import java.util.Map;

/**
 * 数据生成连接器，用于生成模拟数据并将其输出到OpenSearch中
 *
 * @author 孔余
 * @email 2385569970@qq.com
 * @since 2025-01-14
 */
public class DataGeneratorOpenSearch {
    public static void main(String[] args) throws Exception {
        // 获取执行环境
        StreamExecutionEnvironment env = StreamExecutionEnvironment.getExecutionEnvironment();
        // 设置并行度为 1，仅用于简化示例
        env.setParallelism(1);

        // 创建数据生成器源，生成器函数为 MyGeneratorFunction，每秒生成 1000 条数据，速率限制为 3 条/秒
        DataGeneratorSource<UserInfoEntity> source = new DataGeneratorSource<>(
                new MyGeneratorFunction(),
                Long.MAX_VALUE,
                RateLimiterStrategy.perSecond(10),
                TypeInformation.of(UserInfoEntity.class)
        );

        // 将数据生成器源添加到流中
        DataStreamSource<UserInfoEntity> stream =
                env.fromSource(source,
                        WatermarkStrategy.noWatermarks(), // 不生成水印，仅用于演示
                        "Generator Source");

        // Sink
        OpensearchSink<UserInfoEntity> sink = new OpensearchSinkBuilder<UserInfoEntity>()
                .setHosts(new HttpHost("192.168.1.10", 31723, "http"))
                .setEmitter(
                        (element, context, indexer) ->
                                indexer.add(createIndexRequest(element)))
                .setBulkFlushMaxActions(1000)
                .setBulkFlushInterval(3000)
                .setBulkFlushMaxSizeMb(10)
                .setBulkFlushBackoffStrategy(FlushBackoffType.EXPONENTIAL, 5, 1000)
                .build();

        // 将数据打印到控制台
        stream.print("sink opensearch");
        // 写入数据
        stream.sinkTo(sink);

        // 执行 Flink 作业
        env.execute();
    }

    private static IndexRequest createIndexRequest(UserInfoEntity element) {
        Map map = BeanUtil.toBean(element, Map.class);
        return Requests.indexRequest()
                //.id(String.valueOf(element.getId()))
                .index("my_user")
                .source(map);
    }
}
```

**查看索引**

创建索引，可以选择手动创建索引，Flink自动创建的索引可能其设置不满足相关需求

```
curl -X PUT "http://localhost:9200/my_user" -H 'Content-Type: application/json' -d '{
  "settings": {
    "mapping.total_fields.limit": "1000000",
    "max_result_window": "1000000",
    "number_of_shards": 1,
    "number_of_replicas": 0
  }
}'
```

查看所有索引

```
curl http://localhost:9200/_cat/indices?v
```

查看索引的统计信息

```
curl "http://localhost:9200/my_user/_stats/docs?pretty"
```

查看索引映射（mappings）和设置

```
curl http://localhost:9200/my_user/_mapping?pretty
curl http://localhost:9200/my_user/_settings?pretty
```

查看数据

```
curl -X GET "http://localhost:9200/my_user/_search?pretty" -H 'Content-Type: application/json' -d'
{
  "query": {
    "match_all": {}
  },
  "sort": [
    {
      "createTime": {
        "order": "desc"
      }
    }
  ],
  "size": 10
}'
```

删除索引

```
curl -X DELETE "http://localhost:9200/my_user"
```

#### 创建数据生成器（HDFS）

参考：[官方文档](https://nightlies.apache.org/flink/flink-docs-release-1.19/zh/docs/connectors/datastream/filesystem/)

**添加依赖**

```xml
    <dependencies>
        <!-- Hadoop HDFS客户端 -->
        <dependency>
            <groupId>org.apache.hadoop</groupId>
            <artifactId>hadoop-client</artifactId>
            <version>${hadoop.version}</version>
            <scope>provided</scope>
            <exclusions>
                <exclusion>
                    <groupId>org.slf4j</groupId>
                    <artifactId>slf4j-reload4j</artifactId>
                </exclusion>
                <exclusion>
                    <groupId>ch.qos.logback</groupId>
                    <artifactId>logback-classic</artifactId>
                </exclusion>
                <exclusion>
                    <artifactId>gson</artifactId>
                    <groupId>com.google.code.gson</groupId>
                </exclusion>
            </exclusions>
        </dependency>
        <!-- Apache Flink Files 连接器库 -->
        <dependency>
            <groupId>org.apache.flink</groupId>
            <artifactId>flink-connector-files</artifactId>
            <version>${flink.version}</version>
            <scope>provided</scope>
        </dependency>
    </dependencies>
```

**创建生成器**

```java
package local.ateng.java.DataStream.sink;

import com.alibaba.fastjson2.JSONObject;
import local.ateng.java.function.MyGeneratorFunction;
import local.ateng.java.entity.UserInfoEntity;
import org.apache.flink.api.common.eventtime.WatermarkStrategy;
import org.apache.flink.api.common.serialization.SimpleStringEncoder;
import org.apache.flink.api.common.typeinfo.TypeInformation;
import org.apache.flink.api.connector.source.util.ratelimit.RateLimiterStrategy;
import org.apache.flink.connector.datagen.source.DataGeneratorSource;
import org.apache.flink.connector.file.sink.FileSink;
import org.apache.flink.core.fs.Path;
import org.apache.flink.formats.parquet.protobuf.ParquetProtoWriters;
import org.apache.flink.streaming.api.CheckpointingMode;
import org.apache.flink.streaming.api.datastream.SingleOutputStreamOperator;
import org.apache.flink.streaming.api.environment.StreamExecutionEnvironment;
import org.apache.flink.streaming.api.functions.sink.filesystem.rollingpolicies.OnCheckpointRollingPolicy;

/**
 * 数据生成连接器，用于生成模拟数据并将其输出到HDFS中
 *
 * @author 孔余
 * @email 2385569970@qq.com
 * @since 2025-01-15
 */
public class DataGeneratorHDFS {
    public static void main(String[] args) throws Exception {
        // 获取执行环境
        StreamExecutionEnvironment env = StreamExecutionEnvironment.getExecutionEnvironment();
        // 启用检查点，设置检查点间隔为 2 分钟，检查点模式为 精准一次
        env.enableCheckpointing(120 * 1000, CheckpointingMode.EXACTLY_ONCE);
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
        SingleOutputStreamOperator<String> stream = env
                .fromSource(source, WatermarkStrategy.noWatermarks(), "Generator Source")
                .map(user -> JSONObject.toJSONString(user));

        // Sink
        FileSink<String> sink = FileSink
                .forRowFormat(new Path("hdfs://server01:8020/data/flink/sink"), new SimpleStringEncoder<String>("UTF-8"))
                // 基于 Checkpoint 的滚动策略
                .withRollingPolicy(OnCheckpointRollingPolicy.build())
                .build();

        // 将数据打印到控制台
        stream.print("sink hdfs");
        // 写入数据
        stream.sinkTo(sink);

        // 执行程序
        env.execute();
    }
}
```

**配置HDFS**

Windows操作系统配置Hadoop

- 参考：[地址](https://github.com/cdarlint/winutils/tree/master)

操作系统设置环境变量

```
HADOOP_GROUP_NAME=ateng
HADOOP_USER_NAME=admin
```

创建目录并设置权限

```
hadoop fs -mkdir -p /data
hadoop fs -chown admin:ateng /data
```

查看文件

```
hadoop fs -ls /data/flink/sink
```

![image-20250115093703501](./assets/image-20250115093703501.png)



#### 创建数据生成器（MinIO）

参考：[官方文档](https://nightlies.apache.org/flink/flink-docs-release-1.19/zh/docs/connectors/datastream/filesystem/)

**添加依赖**

```xml
    <dependencies>
        <!-- S3 连接器依赖 -->
        <dependency>
            <groupId>org.apache.flink</groupId>
            <artifactId>flink-s3-fs-hadoop</artifactId>
            <version>${flink.version}</version>
            <scope>provided</scope>
        </dependency>
        <dependency>
            <groupId>org.apache.hadoop</groupId>
            <artifactId>hadoop-aws</artifactId>
            <version>${hadoop.version}</version>
            <scope>provided</scope>
        </dependency>
        <!-- Apache Flink Files 连接器库 -->
        <dependency>
            <groupId>org.apache.flink</groupId>
            <artifactId>flink-connector-files</artifactId>
            <version>${flink.version}</version>
            <scope>provided</scope>
        </dependency>
    </dependencies>
```

**创建生成器**

```java
package local.ateng.java.DataStream.sink;

import com.alibaba.fastjson2.JSONObject;
import local.ateng.java.function.MyGeneratorFunction;
import local.ateng.java.entity.UserInfoEntity;
import org.apache.flink.api.common.eventtime.WatermarkStrategy;
import org.apache.flink.api.common.serialization.SimpleStringEncoder;
import org.apache.flink.api.common.typeinfo.TypeInformation;
import org.apache.flink.api.connector.source.util.ratelimit.RateLimiterStrategy;
import org.apache.flink.configuration.Configuration;
import org.apache.flink.connector.datagen.source.DataGeneratorSource;
import org.apache.flink.connector.file.sink.FileSink;
import org.apache.flink.core.fs.FileSystem;
import org.apache.flink.core.fs.Path;
import org.apache.flink.core.plugin.PluginManager;
import org.apache.flink.core.plugin.PluginUtils;
import org.apache.flink.streaming.api.CheckpointingMode;
import org.apache.flink.streaming.api.datastream.SingleOutputStreamOperator;
import org.apache.flink.streaming.api.environment.StreamExecutionEnvironment;
import org.apache.flink.streaming.api.functions.sink.filesystem.rollingpolicies.OnCheckpointRollingPolicy;

/**
 * 数据生成连接器，用于生成模拟数据并将其输出到MinIO中
 *
 * @author 孔余
 * @email 2385569970@qq.com
 * @since 2025-01-15
 */
public class DataGeneratorMinIO {
    public static void main(String[] args) throws Exception {
        // 初始化 s3 插件
        Configuration pluginConfiguration = new Configuration();
        pluginConfiguration.setString("s3.endpoint", "http://192.168.1.13:9000");
        pluginConfiguration.setString("s3.access-key", "admin");
        pluginConfiguration.setString("s3.secret-key", "Lingo@local_minio_9000");
        pluginConfiguration.setString("s3.path.style.access", "true");
        PluginManager pluginManager = PluginUtils.createPluginManagerFromRootFolder(pluginConfiguration);
        FileSystem.initialize(pluginConfiguration, pluginManager);

        // 获取执行环境
        StreamExecutionEnvironment env = StreamExecutionEnvironment.getExecutionEnvironment();
        // 启用检查点，设置检查点间隔为 2 分钟，检查点模式为 精准一次
        env.enableCheckpointing(120 * 1000, CheckpointingMode.EXACTLY_ONCE);
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
        SingleOutputStreamOperator<String> stream = env
                .fromSource(source, WatermarkStrategy.noWatermarks(), "Generator Source")
                .map(user -> JSONObject.toJSONString(user));

        // Sink
        FileSink<String> sink = FileSink
                .forRowFormat(new Path("s3a://test/flink/sink"), new SimpleStringEncoder<String>("UTF-8"))
                // 基于 Checkpoint 的滚动策略
                .withRollingPolicy(OnCheckpointRollingPolicy.build())
                .build();

        // 将数据打印到控制台
        stream.print("sink minio");
        // 写入数据
        stream.sinkTo(sink);

        // 执行程序
        env.execute();
    }
}
```

**查看文件**

```
mcli ls minio-server/test/flink/sink/2025-01-15--10/
```

![image-20250115102715787](./assets/image-20250115102715787.png)

#### 创建数据生成器（HBase）

**创建表**

创建表

> 首先通过 `hbase shell` 进入客户端

```
create 'user_info_table', 'cf'
```

查看表数据

```
count 'user_info_table'
scan 'user_info_table', {LIMIT => 1, REVERSED => true, FORMATTER => 'toString'}
```

清空表

```
disable 'user_info_table'
truncate 'user_info_table'
enable 'user_info_table'
```

**添加依赖**

```xml
        <!-- Apache Flink HBase 连接器库 -->
        <dependency>
            <groupId>org.apache.flink</groupId>
            <artifactId>flink-connector-hbase-2.2</artifactId>
            <version>4.0.0-1.19</version>
            <scope>provided</scope>
        </dependency>
```

**创建生成器**

```java
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
        env.execute();
    }
}
```



### 数据源

#### Kafka

参考：[官方文档](https://nightlies.apache.org/flink/flink-docs-release-1.19/zh/docs/connectors/datastream/kafka/)

```java
package local.ateng.java.DataStream.source;

import org.apache.flink.api.common.eventtime.WatermarkStrategy;
import org.apache.flink.api.common.serialization.SimpleStringSchema;
import org.apache.flink.connector.kafka.source.KafkaSource;
import org.apache.flink.connector.kafka.source.enumerator.initializer.OffsetsInitializer;
import org.apache.flink.streaming.api.CheckpointingMode;
import org.apache.flink.streaming.api.datastream.DataStreamSource;
import org.apache.flink.streaming.api.environment.StreamExecutionEnvironment;
import org.apache.kafka.clients.consumer.OffsetResetStrategy;

/**
 * Apache Kafka 连接器
 * https://nightlies.apache.org/flink/flink-docs-master/zh/docs/connectors/datastream/kafka/
 * Kafka 数据流处理，用于从 Kafka 主题接收 JSON 数据，并对用户年龄进行统计。
 *
 * @author 孔余
 * @since 2024-02-29 15:59
 */
public class DataSourceKafka {

    public static void main(String[] args) throws Exception {

        // 获取执行环境
        StreamExecutionEnvironment env = StreamExecutionEnvironment.getExecutionEnvironment();
        // 启用检查点，设置检查点间隔为 5 秒，检查点模式为 精准一次
        env.enableCheckpointing(5 * 1000, CheckpointingMode.EXACTLY_ONCE);
        // Kafka的Topic有多少个分区就设置多少并行度（可以设置为分区的倍数），例如：Topic有3个分区就设置并行度为3
        env.setParallelism(3);

        // 创建 Kafka 数据源，连接到指定的 Kafka 服务器和主题
        KafkaSource<String> source = KafkaSource.<String>builder()
                // 设置 Kafka 服务器地址
                .setBootstrapServers("192.168.1.10:9094")
                // 设置要订阅的主题
                .setTopics("ateng_flink_json")
                // 设置消费者组 ID
                .setGroupId("ateng")
                // 设置在检查点时提交偏移量（offsets）以确保精确一次语义
                .setProperty("commit.offsets.on.checkpoint", "true")
                // 启用自动提交偏移量
                .setProperty("enable.auto.commit", "true")
                // 自动提交偏移量的时间间隔
                .setProperty("auto.commit.interval.ms", "1000")
                // 设置分区发现的时间间隔
                .setProperty("partition.discovery.interval.ms", "10000")
                // 设置起始偏移量（如果没有提交的偏移量，则从最早的偏移量开始消费）
                .setStartingOffsets(OffsetsInitializer.committedOffsets(OffsetResetStrategy.EARLIEST))
                // 设置仅接收值的反序列化器
                .setValueOnlyDeserializer(new SimpleStringSchema())
                // 构建 Kafka 数据源
                .build();

        // 从 source 中读取数据
        DataStreamSource<String> dataStream = env.fromSource(source, WatermarkStrategy.noWatermarks(), "Kafka Source");

        // 打印计算结果
        dataStream.print("output");

        // 执行流处理作业
        env.execute("Kafka Source");
    }

}
```

#### HDFS

参考：[官方文档](https://nightlies.apache.org/flink/flink-docs-release-1.19/zh/docs/connectors/datastream/filesystem/)

```java
package local.ateng.java.DataStream.source;

import org.apache.flink.api.common.eventtime.WatermarkStrategy;
import org.apache.flink.api.common.serialization.SimpleStringSchema;
import org.apache.flink.connector.file.src.FileSource;
import org.apache.flink.connector.file.src.reader.StreamFormat;
import org.apache.flink.connector.file.src.reader.TextLineInputFormat;
import org.apache.flink.core.fs.Path;
import org.apache.flink.streaming.api.CheckpointingMode;
import org.apache.flink.streaming.api.datastream.DataStream;
import org.apache.flink.streaming.api.environment.StreamExecutionEnvironment;

import java.time.Duration;

/**
 * 读取HDFS文件
 *
 * @author 孔余
 * @email 2385569970@qq.com
 * @since 2025-01-15
 */
public class DataSourceHDFS {

    public static void main(String[] args) throws Exception {
        // 获取执行环境
        StreamExecutionEnvironment env = StreamExecutionEnvironment.getExecutionEnvironment();
        // 启用检查点，设置检查点间隔为 120 秒，检查点模式为 精准一次
        env.enableCheckpointing(120 * 1000, CheckpointingMode.EXACTLY_ONCE);
        // 设置并行度为 1
        env.setParallelism(1);

        // 创建 FileSource 从 HDFS 中持续读取数据
        FileSource<String> fileSource = FileSource
                .forRecordStreamFormat(new TextLineInputFormat(), new Path("hdfs://server01:8020/data/flink/sink"))
                .monitorContinuously(Duration.ofMillis(5))
                .build();

        // 从 Source 中读取数据
        DataStream<String> stream = env.fromSource(fileSource, WatermarkStrategy.noWatermarks(), "HDFS Source");

        // 输出流数据
        stream.print("output");

        // 执行程序
        env.execute("HDFS Source");
    }

}
```

#### MinIO

参考：[官方文档](https://nightlies.apache.org/flink/flink-docs-release-1.19/zh/docs/connectors/datastream/filesystem/)

```java
package local.ateng.java.DataStream.source;

import org.apache.flink.api.common.eventtime.WatermarkStrategy;
import org.apache.flink.configuration.Configuration;
import org.apache.flink.connector.file.src.FileSource;
import org.apache.flink.connector.file.src.reader.TextLineInputFormat;
import org.apache.flink.core.fs.FileSystem;
import org.apache.flink.core.fs.Path;
import org.apache.flink.core.plugin.PluginManager;
import org.apache.flink.core.plugin.PluginUtils;
import org.apache.flink.streaming.api.CheckpointingMode;
import org.apache.flink.streaming.api.datastream.DataStream;
import org.apache.flink.streaming.api.environment.StreamExecutionEnvironment;

import java.time.Duration;

/**
 * 读取MinIO文件
 *
 * @author 孔余
 * @email 2385569970@qq.com
 * @since 2025-01-15
 */
public class DataSourceMinIO {

    public static void main(String[] args) throws Exception {
        // 初始化 s3 插件
        Configuration pluginConfiguration = new Configuration();
        pluginConfiguration.setString("s3.endpoint", "http://192.168.1.13:9000");
        pluginConfiguration.setString("s3.access-key", "admin");
        pluginConfiguration.setString("s3.secret-key", "Lingo@local_minio_9000");
        pluginConfiguration.setString("s3.path.style.access", "true");
        PluginManager pluginManager = PluginUtils.createPluginManagerFromRootFolder(pluginConfiguration);
        FileSystem.initialize(pluginConfiguration, pluginManager);

        // 获取执行环境
        StreamExecutionEnvironment env = StreamExecutionEnvironment.getExecutionEnvironment();
        // 启用检查点，设置检查点间隔为 120 秒，检查点模式为 精准一次
        env.enableCheckpointing(120 * 1000, CheckpointingMode.EXACTLY_ONCE);
        // 设置并行度为 1
        env.setParallelism(1);

        // 创建 FileSource 从 MinIO 中持续读取数据
        FileSource<String> fileSource = FileSource
                .forRecordStreamFormat(new TextLineInputFormat(), new Path("s3a://test/flink/sink"))
                .monitorContinuously(Duration.ofMillis(5))
                .build();

        // 从 Source 中读取数据
        DataStream<String> stream = env.fromSource(fileSource, WatermarkStrategy.noWatermarks(), "MinIO Source");

        // 输出流数据
        stream.print("output");

        // 执行程序
        env.execute("MinIO Source");
    }

}
```

#### Doris

参考：[官方文档](https://doris.apache.org/zh-CN/docs/ecosystem/flink-doris-connector)

```java
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
```



### 算子

参考：[官方文档](https://nightlies.apache.org/flink/flink-docs-release-1.19/zh/docs/dev/datastream/operators/overview/)

#### Map

DataStream → DataStream
输入一个元素同时输出一个元素。

```java
package local.ateng.java.DataStream.operator;

import com.alibaba.fastjson2.JSONObject;
import local.ateng.java.entity.UserInfoEntity;
import org.apache.flink.api.common.eventtime.WatermarkStrategy;
import org.apache.flink.api.common.functions.MapFunction;
import org.apache.flink.api.common.serialization.SimpleStringSchema;
import org.apache.flink.connector.kafka.source.KafkaSource;
import org.apache.flink.connector.kafka.source.enumerator.initializer.OffsetsInitializer;
import org.apache.flink.streaming.api.CheckpointingMode;
import org.apache.flink.streaming.api.datastream.DataStreamSource;
import org.apache.flink.streaming.api.datastream.SingleOutputStreamOperator;
import org.apache.flink.streaming.api.environment.StreamExecutionEnvironment;
import org.apache.kafka.clients.consumer.OffsetResetStrategy;

import java.time.Duration;

/**
 * 数据流转换 Map
 * https://nightlies.apache.org/flink/flink-docs-release-1.19/zh/docs/dev/datastream/operators/overview/
 * DataStream → DataStream
 * 输入一个元素同时输出一个元素
 *
 * @author 孔余
 * @email 2385569970@qq.com
 * @since 2025-01-15
 */
public class OperatorMap {

    public static void main(String[] args) throws Exception {
        // 环境准备
        StreamExecutionEnvironment env = StreamExecutionEnvironment.getExecutionEnvironment();
        env.enableCheckpointing(5 * 1000, CheckpointingMode.EXACTLY_ONCE);
        env.setParallelism(1);
        KafkaSource<String> source = KafkaSource.<String>builder()
                .setBootstrapServers("192.168.1.10:9094")
                .setTopics("ateng_flink_json")
                .setGroupId("ateng")
                .setProperty("commit.offsets.on.checkpoint", "true")
                .setProperty("enable.auto.commit", "true")
                .setProperty("auto.commit.interval.ms", "1000")
                .setProperty("partition.discovery.interval.ms", "10000")
                .setStartingOffsets(OffsetsInitializer.committedOffsets(OffsetResetStrategy.EARLIEST))
                .setValueOnlyDeserializer(new SimpleStringSchema())
                .build();

        // 从 Kafka 数据源读取数据，设置水印策略为 BoundedOutOfOrderness，最大乱序时间为 3 秒，命名为 "Kafka Source"
        DataStreamSource<String> streamSource = env.fromSource(source, WatermarkStrategy.forBoundedOutOfOrderness(Duration.ofSeconds(3)), "Kafka Source");

        // 算子
        SingleOutputStreamOperator<UserInfoEntity> operator = streamSource
                .map(new MapFunction<String, UserInfoEntity>() {
                    @Override
                    public UserInfoEntity map(String str) throws Exception {
                        UserInfoEntity userInfoEntity = JSONObject.parseObject(str, UserInfoEntity.class);
                        userInfoEntity.setName(userInfoEntity.getName() + ".local");
                        return userInfoEntity;
                    }
                });
        operator.print("sink");

        // 执行流处理作业
        env.execute("Kafka Stream");
    }

}
```

![image-20250115162952405](./assets/image-20250115162952405.png)

#### FlatMap

DataStream → DataStream
输入一个元素同时产生零个、一个或多个元素。

```java
package local.ateng.java.DataStream.operator;

import cn.hutool.core.util.StrUtil;
import com.alibaba.fastjson2.JSONObject;
import local.ateng.java.entity.UserInfoEntity;
import org.apache.flink.api.common.eventtime.WatermarkStrategy;
import org.apache.flink.api.common.functions.FlatMapFunction;
import org.apache.flink.api.common.serialization.SimpleStringSchema;
import org.apache.flink.connector.kafka.source.KafkaSource;
import org.apache.flink.connector.kafka.source.enumerator.initializer.OffsetsInitializer;
import org.apache.flink.streaming.api.CheckpointingMode;
import org.apache.flink.streaming.api.datastream.DataStreamSource;
import org.apache.flink.streaming.api.datastream.SingleOutputStreamOperator;
import org.apache.flink.streaming.api.environment.StreamExecutionEnvironment;
import org.apache.flink.util.Collector;
import org.apache.kafka.clients.consumer.OffsetResetStrategy;

import java.time.Duration;

/**
 * 数据流转换 FlatMap
 * https://nightlies.apache.org/flink/flink-docs-release-1.19/zh/docs/dev/datastream/operators/overview/
 * DataStream → DataStream
 * 输入一个元素同时产生零个、一个或多个元素
 *
 * @author 孔余
 * @email 2385569970@qq.com
 * @since 2025-01-15
 */
public class OperatorFlatMap {

    public static void main(String[] args) throws Exception {
        // 环境准备
        StreamExecutionEnvironment env = StreamExecutionEnvironment.getExecutionEnvironment();
        env.enableCheckpointing(5 * 1000, CheckpointingMode.EXACTLY_ONCE);
        env.setParallelism(1);
        KafkaSource<String> source = KafkaSource.<String>builder()
                .setBootstrapServers("192.168.1.10:9094")
                .setTopics("ateng_flink_json")
                .setGroupId("ateng")
                .setProperty("commit.offsets.on.checkpoint", "true")
                .setProperty("enable.auto.commit", "true")
                .setProperty("auto.commit.interval.ms", "1000")
                .setProperty("partition.discovery.interval.ms", "10000")
                .setStartingOffsets(OffsetsInitializer.committedOffsets(OffsetResetStrategy.EARLIEST))
                .setValueOnlyDeserializer(new SimpleStringSchema())
                .build();

        // 从 Kafka 数据源读取数据，设置水印策略为 BoundedOutOfOrderness，最大乱序时间为 3 秒，命名为 "Kafka Source"
        DataStreamSource<String> streamSource = env.fromSource(source, WatermarkStrategy.forBoundedOutOfOrderness(Duration.ofSeconds(3)), "Kafka Source");

        // 算子
        SingleOutputStreamOperator<String> operator = streamSource
                .flatMap(new FlatMapFunction<String, String>() {
                    @Override
                    public void flatMap(String str, Collector<String> collector) throws Exception {
                        UserInfoEntity userInfoEntity = JSONObject.parseObject(str, UserInfoEntity.class);
                        String name = userInfoEntity.getName();
                        String city = userInfoEntity.getCity();
                        collector.collect(StrUtil.format("{}-{}", city, name));
                    }
                });
        operator.print("sink");

        // 执行流处理作业
        env.execute("Kafka Stream");
    }

}
```

![image-20250115162928599](./assets/image-20250115162928599.png)



#### Filter

DataStream → DataStream

为每个元素执行一个布尔 function，并保留那些 function 输出值为 true 的元素。

```java
package local.ateng.java.DataStream.operator;

import com.alibaba.fastjson2.JSONObject;
import local.ateng.java.entity.UserInfoEntity;
import org.apache.flink.api.common.eventtime.WatermarkStrategy;
import org.apache.flink.api.common.functions.FilterFunction;
import org.apache.flink.api.common.serialization.SimpleStringSchema;
import org.apache.flink.connector.kafka.source.KafkaSource;
import org.apache.flink.connector.kafka.source.enumerator.initializer.OffsetsInitializer;
import org.apache.flink.streaming.api.CheckpointingMode;
import org.apache.flink.streaming.api.datastream.DataStreamSource;
import org.apache.flink.streaming.api.datastream.SingleOutputStreamOperator;
import org.apache.flink.streaming.api.environment.StreamExecutionEnvironment;
import org.apache.kafka.clients.consumer.OffsetResetStrategy;

import java.time.Duration;

/**
 * 数据流转换 Filter
 * https://nightlies.apache.org/flink/flink-docs-release-1.19/zh/docs/dev/datastream/operators/overview/
 * DataStream → DataStream
 * 为每个元素执行一个布尔 function，并保留那些 function 输出值为 true 的元素
 *
 * @author 孔余
 * @email 2385569970@qq.com
 * @since 2025-01-15
 */
public class OperatorFilter {

    public static void main(String[] args) throws Exception {
        // 环境准备
        StreamExecutionEnvironment env = StreamExecutionEnvironment.getExecutionEnvironment();
        env.enableCheckpointing(5 * 1000, CheckpointingMode.EXACTLY_ONCE);
        env.setParallelism(1);
        KafkaSource<String> source = KafkaSource.<String>builder()
                .setBootstrapServers("192.168.1.10:9094")
                .setTopics("ateng_flink_json")
                .setGroupId("ateng")
                .setProperty("commit.offsets.on.checkpoint", "true")
                .setProperty("enable.auto.commit", "true")
                .setProperty("auto.commit.interval.ms", "1000")
                .setProperty("partition.discovery.interval.ms", "10000")
                .setStartingOffsets(OffsetsInitializer.committedOffsets(OffsetResetStrategy.EARLIEST))
                .setValueOnlyDeserializer(new SimpleStringSchema())
                .build();

        // 从 Kafka 数据源读取数据，设置水印策略为 BoundedOutOfOrderness，最大乱序时间为 3 秒，命名为 "Kafka Source"
        DataStreamSource<String> streamSource = env.fromSource(source, WatermarkStrategy.forBoundedOutOfOrderness(Duration.ofSeconds(3)), "Kafka Source");

        // 算子
        SingleOutputStreamOperator<String> operator = streamSource
                .filter(new FilterFunction<String>() {
                    @Override
                    public boolean filter(String str) throws Exception {
                        UserInfoEntity userInfoEntity = JSONObject.parseObject(str).toJavaObject(UserInfoEntity.class);
                        Integer age = userInfoEntity.getAge();
                        if (age == 25) {
                            return true;
                        }
                        return false;
                    }
                });
        operator.print("sink");

        // 执行流处理作业
        env.execute("Kafka Stream");
    }

}
```

#### KeyBy Reduce

DataStream → KeyedStream → DataStream

在逻辑上将流划分为不相交的分区。具有相同 key 的记录都分配到同一个分区。在内部， keyBy() 是通过哈希分区实现的。有多种指定 key 的方式。

在相同 key 的数据流上“滚动”执行 reduce。将当前元素与最后一次 reduce 得到的值组合然后输出新值。

```java
package local.ateng.java.DataStream.operator;

import com.alibaba.fastjson2.JSONObject;
import local.ateng.java.entity.UserInfoEntity;
import org.apache.flink.api.common.eventtime.WatermarkStrategy;
import org.apache.flink.api.common.functions.MapFunction;
import org.apache.flink.api.common.functions.ReduceFunction;
import org.apache.flink.api.common.serialization.SimpleStringSchema;
import org.apache.flink.api.java.functions.KeySelector;
import org.apache.flink.connector.kafka.source.KafkaSource;
import org.apache.flink.connector.kafka.source.enumerator.initializer.OffsetsInitializer;
import org.apache.flink.streaming.api.CheckpointingMode;
import org.apache.flink.streaming.api.datastream.DataStreamSource;
import org.apache.flink.streaming.api.datastream.SingleOutputStreamOperator;
import org.apache.flink.streaming.api.environment.StreamExecutionEnvironment;
import org.apache.kafka.clients.consumer.OffsetResetStrategy;

import java.time.Duration;

/**
 * 数据流转换 KeyBy Reduce
 * https://nightlies.apache.org/flink/flink-docs-release-1.19/zh/docs/dev/datastream/operators/overview/
 * DataStream → KeyedStream → DataStream
 * 在逻辑上将流划分为不相交的分区。具有相同 key 的记录都分配到同一个分区。在内部， keyBy() 是通过哈希分区实现的。有多种指定 key 的方式。
 * 在相同 key 的数据流上“滚动”执行 reduce。将当前元素与最后一次 reduce 得到的值组合然后输出新值。
 *
 * @author 孔余
 * @email 2385569970@qq.com
 * @since 2025-01-15
 */
public class OperatorKeyByReduce {

    public static void main(String[] args) throws Exception {
        // 环境准备
        StreamExecutionEnvironment env = StreamExecutionEnvironment.getExecutionEnvironment();
        env.enableCheckpointing(5 * 1000, CheckpointingMode.EXACTLY_ONCE);
        env.setParallelism(1);
        KafkaSource<String> source = KafkaSource.<String>builder()
                .setBootstrapServers("192.168.1.10:9094")
                .setTopics("ateng_flink_json")
                .setGroupId("ateng")
                .setProperty("commit.offsets.on.checkpoint", "true")
                .setProperty("enable.auto.commit", "true")
                .setProperty("auto.commit.interval.ms", "1000")
                .setProperty("partition.discovery.interval.ms", "10000")
                .setStartingOffsets(OffsetsInitializer.committedOffsets(OffsetResetStrategy.EARLIEST))
                .setValueOnlyDeserializer(new SimpleStringSchema())
                .build();

        // 从 Kafka 数据源读取数据，设置水印策略为 BoundedOutOfOrderness，最大乱序时间为 3 秒，命名为 "Kafka Source"
        DataStreamSource<String> streamSource = env.fromSource(source, WatermarkStrategy.forBoundedOutOfOrderness(Duration.ofSeconds(3)), "Kafka Source");

        // 算子
        SingleOutputStreamOperator<JSONObject> operator = streamSource
                .map(new MapFunction<String, JSONObject>() {
                    @Override
                    public JSONObject map(String str) throws Exception {
                        UserInfoEntity userInfoEntity = JSONObject.parseObject(str, UserInfoEntity.class);
                        return JSONObject.of(
                                "province", userInfoEntity.getProvince(),
                                "score", userInfoEntity.getScore());
                    }
                })
                .keyBy(new KeySelector<JSONObject, String>() {
                    @Override
                    public String getKey(JSONObject jsonObject) throws Exception {
                        return jsonObject.getString("province");
                    }
                })
                .reduce(new ReduceFunction<JSONObject>() {
                    @Override
                    public JSONObject reduce(JSONObject json1, JSONObject json2) throws Exception {
                        return JSONObject.of(
                                "province", json1.getString("province"),
                                "score", json1.getDouble("score") + json2.getDouble("score"));
                    }
                });

        operator.print("sink");

        // 执行流处理作业
        env.execute("Kafka Stream");
    }

}
```



### 滚动窗口 (Tumbling Window)

滚动窗口是大小固定且不重叠的时间窗口。它将数据流划分为一系列连续的、大小相同的时间段，每个时间段的事件完全独立。在每个窗口内，所有事件的时间戳都在相同的时间区间内，窗口到期后就会触发计算。这种窗口模式通常用于定期的统计计算，如每分钟、每小时等。其优点是操作简单，适合周期性的聚合需求。

参考：[官方文档](https://nightlies.apache.org/flink/flink-docs-release-1.19/zh/docs/dev/datastream/operators/windows/)

#### 事件时间WindowAll

DataStream → AllWindowedStream → DataStream

对于 non-keyed stream，原始的 stream 不会被分割为多个逻辑上的 stream， 所以所有的窗口计算会被同一个 task 完成，也就是 parallelism 为 1。

> 这适用于非并行转换的大多数场景。所有记录都将收集到 windowAll 算子对应的一个任务中。

```java
package local.ateng.java.DataStream.window.tumbling;

import cn.hutool.core.date.DateUtil;
import cn.hutool.core.date.LocalDateTimeUtil;
import com.alibaba.fastjson2.JSONArray;
import com.alibaba.fastjson2.JSONObject;
import local.ateng.java.entity.UserInfoEntity;
import org.apache.flink.api.common.eventtime.WatermarkStrategy;
import org.apache.flink.api.common.serialization.SimpleStringSchema;
import org.apache.flink.connector.kafka.source.KafkaSource;
import org.apache.flink.connector.kafka.source.enumerator.initializer.OffsetsInitializer;
import org.apache.flink.streaming.api.CheckpointingMode;
import org.apache.flink.streaming.api.datastream.DataStreamSource;
import org.apache.flink.streaming.api.datastream.SingleOutputStreamOperator;
import org.apache.flink.streaming.api.environment.StreamExecutionEnvironment;
import org.apache.flink.streaming.api.functions.windowing.AllWindowFunction;
import org.apache.flink.streaming.api.windowing.assigners.TumblingEventTimeWindows;
import org.apache.flink.streaming.api.windowing.windows.TimeWindow;
import org.apache.flink.util.Collector;
import org.apache.kafka.clients.consumer.OffsetResetStrategy;

import java.time.Duration;

/**
 * 可以在普通 DataStream 上定义 Window。 Window 根据某些特征（例如，最近 5 秒内到达的数据）对所有流事件进行分组。
 * DataStream → AllWindowedStream → DataStream
 * https://nightlies.apache.org/flink/flink-docs-release-1.19/zh/docs/dev/datastream/operators/overview/#windowall
 * <p>
 * 这个程序通过 Flink 从 Kafka 中读取消息。
 * 它使用事件时间滚动窗口（TumblingEventTimeWindows），每 1 分钟对数据进行一次窗口计算。
 * 对每个窗口中的数据，程序会创建一个 JSON 对象，包含窗口的开始和结束时间、窗口中的数据（第一个、最后一个元素）、数据量以及当前时间。
 * 最后，处理后的结果被输出到控制台。
 *
 * @author 孔余
 * @email 2385569970@qq.com
 * @since 2025-01-15
 */
public class EventTimeWindowsAll {

    public static void main(String[] args) throws Exception {
        // 环境准备
        StreamExecutionEnvironment env = StreamExecutionEnvironment.getExecutionEnvironment();
        env.enableCheckpointing(3 * 1000, CheckpointingMode.EXACTLY_ONCE);
        env.setParallelism(1);
        KafkaSource<String> source = KafkaSource.<String>builder()
                .setBootstrapServers("192.168.1.10:9094")
                .setTopics("ateng_flink_json")
                .setGroupId("ateng")
                .setProperty("commit.offsets.on.checkpoint", "true")
                .setProperty("enable.auto.commit", "true")
                .setProperty("auto.commit.interval.ms", "1000")
                .setProperty("partition.discovery.interval.ms", "10000")
                .setStartingOffsets(OffsetsInitializer.committedOffsets(OffsetResetStrategy.EARLIEST))
                .setValueOnlyDeserializer(new SimpleStringSchema())
                .build();

        // 定义水印策略：WatermarkStrategy 可以在 Flink 应用程序中的两处使用，第一种是直接在数据源上使用，第二种是直接在非数据源的操作之后使用。
        // 允许最多 5 秒的事件时间乱序，使用 createTime 字段为事件时间戳（毫秒）
        WatermarkStrategy<String> watermarkStrategy = WatermarkStrategy.<String>forBoundedOutOfOrderness(Duration.ofSeconds(5))
                .withTimestampAssigner(
                        (event, recordTimestamp) -> {
                            // 解析 JSON 格式的事件，并获取事件时间
                            UserInfoEntity user = JSONObject.parseObject(event).toJavaObject(UserInfoEntity.class);
                            long timestamp = LocalDateTimeUtil.toEpochMilli(user.getCreateTime());
                            return timestamp;
                        });

        // 从 Kafka 数据源读取数据，设置水印策略
        DataStreamSource<String> streamSource = env.fromSource(source, watermarkStrategy, "Kafka Source");

        // 窗口
        SingleOutputStreamOperator<JSONObject> operator = streamSource
                // 设置水印策略为事件时间
                //.assignTimestampsAndWatermarks(watermarkStrategy)
                // 1分钟滚动窗口
                .windowAll(TumblingEventTimeWindows.of(Duration.ofMinutes(1)))
                .apply(new AllWindowFunction<String, JSONObject, TimeWindow>() {
                    @Override
                    public void apply(TimeWindow timeWindow, Iterable<String> iterable, Collector<JSONObject> collector) throws Exception {
                        long start = timeWindow.getStart();
                        long end = timeWindow.getEnd();
                        JSONObject json = JSONObject.of("start", DateUtil.format(DateUtil.date(start), "yyyy-MM-dd HH:mm:ss.SSS"), "end", DateUtil.format(DateUtil.date(end), "yyyy-MM-dd HH:mm:ss.SSS"));
                        JSONArray jsonArray = JSONArray.of();
                        for (String string : iterable) {
                            jsonArray.add(JSONObject.parseObject(string));
                        }
                        int size = jsonArray.size();
                        json.put("data^", jsonArray.get(0));
                        json.put("data$", jsonArray.get(size - 1));
                        json.put("size", size);
                        json.put("dateTime", DateUtil.format(DateUtil.date(), "yyyy-MM-dd HH:mm:ss.SSS"));
                        collector.collect(json);
                    }
                });
        operator.print("sink");

        // 执行流处理作业
        env.execute("Kafka Stream");
    }

}
```

![image-20250116075623947](./assets/image-20250116075623947.png)

#### 处理时间WindowAll

DataStream → AllWindowedStream → DataStream

对于 non-keyed stream，原始的 stream 不会被分割为多个逻辑上的 stream， 所以所有的窗口计算会被同一个 task 完成，也就是 parallelism 为 1。

> 这适用于非并行转换的大多数场景。所有记录都将收集到 windowAll 算子对应的一个任务中。

```java
package local.ateng.java.DataStream.window.tumbling;

import cn.hutool.core.date.DateUtil;
import com.alibaba.fastjson2.JSONArray;
import com.alibaba.fastjson2.JSONObject;
import org.apache.flink.api.common.eventtime.WatermarkStrategy;
import org.apache.flink.api.common.serialization.SimpleStringSchema;
import org.apache.flink.connector.kafka.source.KafkaSource;
import org.apache.flink.connector.kafka.source.enumerator.initializer.OffsetsInitializer;
import org.apache.flink.streaming.api.CheckpointingMode;
import org.apache.flink.streaming.api.datastream.DataStreamSource;
import org.apache.flink.streaming.api.datastream.SingleOutputStreamOperator;
import org.apache.flink.streaming.api.environment.StreamExecutionEnvironment;
import org.apache.flink.streaming.api.functions.windowing.AllWindowFunction;
import org.apache.flink.streaming.api.windowing.assigners.TumblingProcessingTimeWindows;
import org.apache.flink.streaming.api.windowing.windows.TimeWindow;
import org.apache.flink.util.Collector;
import org.apache.kafka.clients.consumer.OffsetResetStrategy;

import java.time.Duration;

/**
 * 可以在普通 DataStream 上定义 Window。 Window 根据某些特征（例如，最近 5 秒内到达的数据）对所有流事件进行分组。
 * DataStream → AllWindowedStream → DataStream
 * https://nightlies.apache.org/flink/flink-docs-release-1.19/zh/docs/dev/datastream/operators/overview/#windowall
 * <p>
 * 这个程序通过 Flink 从 Kafka 中读取消息。
 * 它使用处理时间滚动窗口（TumblingProcessingTimeWindows），每 1 分钟对数据进行一次窗口计算。
 * 对每个窗口中的数据，程序会创建一个 JSON 对象，包含窗口的开始和结束时间、窗口中的数据（第一个、最后一个元素）、数据量以及当前时间。
 * 最后，处理后的结果被输出到控制台。
 *
 * @author 孔余
 * @email 2385569970@qq.com
 * @since 2025-01-15
 */
public class ProcessingTimeWindowsAll {

    public static void main(String[] args) throws Exception {
        // 环境准备
        StreamExecutionEnvironment env = StreamExecutionEnvironment.getExecutionEnvironment();
        env.enableCheckpointing(3 * 1000, CheckpointingMode.EXACTLY_ONCE);
        env.setParallelism(1);
        KafkaSource<String> source = KafkaSource.<String>builder()
                .setBootstrapServers("192.168.1.10:9094")
                .setTopics("ateng_flink_json")
                .setGroupId("ateng")
                .setProperty("commit.offsets.on.checkpoint", "true")
                .setProperty("enable.auto.commit", "true")
                .setProperty("auto.commit.interval.ms", "1000")
                .setProperty("partition.discovery.interval.ms", "10000")
                .setStartingOffsets(OffsetsInitializer.committedOffsets(OffsetResetStrategy.EARLIEST))
                .setValueOnlyDeserializer(new SimpleStringSchema())
                .build();

        // 从 Kafka 数据源读取数据，不设置水印策略（处理时间窗口不存在数据乱序问题）
        DataStreamSource<String> streamSource = env.fromSource(source, WatermarkStrategy.noWatermarks(), "Kafka Source");

        // 窗口
        SingleOutputStreamOperator<JSONObject> operator = streamSource
                // 1分钟滚动窗口
                .windowAll(TumblingProcessingTimeWindows.of(Duration.ofMinutes(1)))
                .apply(new AllWindowFunction<String, JSONObject, TimeWindow>() {
                    @Override
                    public void apply(TimeWindow timeWindow, Iterable<String> iterable, Collector<JSONObject> collector) throws Exception {
                        long start = timeWindow.getStart();
                        long end = timeWindow.getEnd();
                        JSONObject json = JSONObject.of("start", DateUtil.format(DateUtil.date(start), "yyyy-MM-dd HH:mm:ss.SSS"), "end", DateUtil.format(DateUtil.date(end), "yyyy-MM-dd HH:mm:ss.SSS"));
                        JSONArray jsonArray = JSONArray.of();
                        for (String string : iterable) {
                            jsonArray.add(JSONObject.parseObject(string));
                        }
                        int size = jsonArray.size();
                        json.put("data^", jsonArray.get(0));
                        json.put("data$", jsonArray.get(size - 1));
                        json.put("size", size);
                        json.put("dateTime", DateUtil.format(DateUtil.date(), "yyyy-MM-dd HH:mm:ss.SSS"));
                        collector.collect(json);
                    }
                });
        operator.print("sink");

        // 执行流处理作业
        env.execute("Kafka Stream");
    }

}
```

![image-20250116075917578](./assets/image-20250116075917578.png)

#### 事件时间Window

KeyedStream → WindowedStream → DataStream

可以在已经分区的 KeyedStreams 上定义 Window。Window 根据某些特征（例如，最近 5 秒内到达的数据）对每个 key Stream 中的数据进行分组

```java
package local.ateng.java.DataStream.window.tumbling;

import cn.hutool.core.date.DateUtil;
import cn.hutool.core.date.LocalDateTimeUtil;
import com.alibaba.fastjson2.JSONObject;
import local.ateng.java.entity.UserInfoEntity;
import org.apache.flink.api.common.eventtime.WatermarkStrategy;
import org.apache.flink.api.common.functions.MapFunction;
import org.apache.flink.api.common.serialization.SimpleStringSchema;
import org.apache.flink.api.java.functions.KeySelector;
import org.apache.flink.connector.kafka.source.KafkaSource;
import org.apache.flink.connector.kafka.source.enumerator.initializer.OffsetsInitializer;
import org.apache.flink.streaming.api.CheckpointingMode;
import org.apache.flink.streaming.api.datastream.DataStreamSource;
import org.apache.flink.streaming.api.datastream.SingleOutputStreamOperator;
import org.apache.flink.streaming.api.environment.StreamExecutionEnvironment;
import org.apache.flink.streaming.api.functions.windowing.WindowFunction;
import org.apache.flink.streaming.api.windowing.assigners.TumblingEventTimeWindows;
import org.apache.flink.streaming.api.windowing.assigners.TumblingProcessingTimeWindows;
import org.apache.flink.streaming.api.windowing.windows.TimeWindow;
import org.apache.flink.util.Collector;
import org.apache.kafka.clients.consumer.OffsetResetStrategy;

import java.time.Duration;

/**
 * 可以在已经分区的 KeyedStreams 上定义 Window。Window 根据某些特征（例如，最近 5 秒内到达的数据）对每个 key Stream 中的数据进行分组。
 * KeyedStream → WindowedStream → DataStream
 * https://nightlies.apache.org/flink/flink-docs-release-1.19/zh/docs/dev/datastream/operators/overview/#window
 *
 * 这个 Flink 程序的主要功能是：
 * 从 Kafka 读取 JSON 格式的用户数据，每条数据包含 province（省份）和 score（分数）信息。
 * 将数据按 province 字段进行分组，并基于处理时间（ProcessingTime）使用滚动窗口（每 1 分钟一个窗口）对每个省份的 score 进行聚合。
 * 每个窗口内，程序计算该省份的总分数，并输出窗口的开始时间、结束时间、总分数以及当前时间。
 *
 * @author 孔余
 * @email 2385569970@qq.com
 * @since 2025-01-15
 */
public class EventTimeWindowKeyBy {

    public static void main(String[] args) throws Exception {
        // 环境准备
        StreamExecutionEnvironment env = StreamExecutionEnvironment.getExecutionEnvironment();
        env.enableCheckpointing(5 * 1000, CheckpointingMode.EXACTLY_ONCE);
        env.setParallelism(3);
        KafkaSource<String> source = KafkaSource.<String>builder()
                .setBootstrapServers("192.168.1.10:9094")
                .setTopics("ateng_flink_json")
                .setGroupId("ateng")
                .setProperty("commit.offsets.on.checkpoint", "true")
                .setProperty("enable.auto.commit", "true")
                .setProperty("auto.commit.interval.ms", "1000")
                .setProperty("partition.discovery.interval.ms", "10000")
                .setStartingOffsets(OffsetsInitializer.committedOffsets(OffsetResetStrategy.EARLIEST))
                .setValueOnlyDeserializer(new SimpleStringSchema())
                .build();

        // 定义水印策略：WatermarkStrategy 可以在 Flink 应用程序中的两处使用，第一种是直接在数据源上使用，第二种是直接在非数据源的操作之后使用。
        // 允许最多 5 秒的事件时间乱序，使用 createTime 字段为事件时间戳（毫秒）
        WatermarkStrategy<String> watermarkStrategy = WatermarkStrategy.<String>forBoundedOutOfOrderness(Duration.ofSeconds(5))
                .withTimestampAssigner(
                        (event, recordTimestamp) -> {
                            // 解析 JSON 格式的事件，并获取事件时间
                            UserInfoEntity user = JSONObject.parseObject(event).toJavaObject(UserInfoEntity.class);
                            long timestamp = LocalDateTimeUtil.toEpochMilli(user.getCreateTime());
                            return timestamp;
                        });

        // 从 Kafka 数据源读取数据，设置水印策略
        DataStreamSource<String> streamSource = env.fromSource(source, watermarkStrategy, "Kafka Source");

        // 算子
        SingleOutputStreamOperator<JSONObject> operator = streamSource
                // 设置水印策略为事件时间
                //.assignTimestampsAndWatermarks(watermarkStrategy)
                .map(new MapFunction<String, JSONObject>() {
                    @Override
                    public JSONObject map(String str) throws Exception {
                        UserInfoEntity userInfoEntity = JSONObject.parseObject(str, UserInfoEntity.class);
                        return JSONObject.of(
                                "province", userInfoEntity.getProvince(),
                                "score", userInfoEntity.getScore());
                    }
                })
                .keyBy(new KeySelector<JSONObject, String>() {
                    @Override
                    public String getKey(JSONObject jsonObject) throws Exception {
                        return jsonObject.getString("province");
                    }
                })
                // 1分钟滚动窗口
                .window(TumblingEventTimeWindows.of(Duration.ofMinutes(1)))
                .apply(new WindowFunction<JSONObject, JSONObject, String, TimeWindow>() {
                    @Override
                    public void apply(String str, TimeWindow timeWindow, Iterable<JSONObject> iterable, Collector<JSONObject> collector) throws Exception {
                        long start = timeWindow.getStart();
                        long end = timeWindow.getEnd();
                        JSONObject json = JSONObject.of("start", DateUtil.format(DateUtil.date(start), "yyyy-MM-dd HH:mm:ss.SSS"), "end", DateUtil.format(DateUtil.date(end), "yyyy-MM-dd HH:mm:ss.SSS"));
                        Double score = 0.0;
                        Long count = 0L;
                        for (JSONObject jsonObject : iterable) {
                            score += jsonObject.getDouble("score");
                            count ++;
                        }
                        json.put("province", str);
                        json.put("score", score);
                        json.put("count", count);
                        json.put("dateTime", DateUtil.format(DateUtil.date(), "yyyy-MM-dd HH:mm:ss.SSS"));
                        collector.collect(json);
                    }
                });
        operator.print("sink");

        // 执行流处理作业
        env.execute("Kafka Stream");
    }

}
```

![image-20250116080053941](./assets/image-20250116080053941.png)

#### 处理时间Window

KeyedStream → WindowedStream → DataStream

可以在已经分区的 KeyedStreams 上定义 Window。Window 根据某些特征（例如，最近 5 秒内到达的数据）对每个 key Stream 中的数据进行分组

```java
package local.ateng.java.DataStream.window.tumbling;

import cn.hutool.core.date.DateUtil;
import com.alibaba.fastjson2.JSONObject;
import local.ateng.java.entity.UserInfoEntity;
import org.apache.flink.api.common.eventtime.WatermarkStrategy;
import org.apache.flink.api.common.functions.MapFunction;
import org.apache.flink.api.common.serialization.SimpleStringSchema;
import org.apache.flink.api.java.functions.KeySelector;
import org.apache.flink.connector.kafka.source.KafkaSource;
import org.apache.flink.connector.kafka.source.enumerator.initializer.OffsetsInitializer;
import org.apache.flink.streaming.api.CheckpointingMode;
import org.apache.flink.streaming.api.datastream.DataStreamSource;
import org.apache.flink.streaming.api.datastream.SingleOutputStreamOperator;
import org.apache.flink.streaming.api.environment.StreamExecutionEnvironment;
import org.apache.flink.streaming.api.functions.windowing.WindowFunction;
import org.apache.flink.streaming.api.windowing.assigners.TumblingProcessingTimeWindows;
import org.apache.flink.streaming.api.windowing.windows.TimeWindow;
import org.apache.flink.util.Collector;
import org.apache.kafka.clients.consumer.OffsetResetStrategy;

import java.time.Duration;

/**
 * 可以在已经分区的 KeyedStreams 上定义 Window。Window 根据某些特征（例如，最近 5 秒内到达的数据）对每个 key Stream 中的数据进行分组。
 * KeyedStream → WindowedStream → DataStream
 * https://nightlies.apache.org/flink/flink-docs-release-1.19/zh/docs/dev/datastream/operators/overview/#window
 *
 * 这个 Flink 程序的主要功能是：
 * 从 Kafka 读取 JSON 格式的用户数据，每条数据包含 province（省份）和 score（分数）信息。
 * 将数据按 province 字段进行分组，并基于处理时间（ProcessingTime）使用滚动窗口（每 1 分钟一个窗口）对每个省份的 score 进行聚合。
 * 每个窗口内，程序计算该省份的总分数，并输出窗口的开始时间、结束时间、总分数以及当前时间。
 *
 * @author 孔余
 * @email 2385569970@qq.com
 * @since 2025-01-15
 */
public class ProcessingTimeWindowKeyBy {

    public static void main(String[] args) throws Exception {
        // 环境准备
        StreamExecutionEnvironment env = StreamExecutionEnvironment.getExecutionEnvironment();
        env.enableCheckpointing(5 * 1000, CheckpointingMode.EXACTLY_ONCE);
        env.setParallelism(3);
        KafkaSource<String> source = KafkaSource.<String>builder()
                .setBootstrapServers("192.168.1.10:9094")
                .setTopics("ateng_flink_json")
                .setGroupId("ateng")
                .setProperty("commit.offsets.on.checkpoint", "true")
                .setProperty("enable.auto.commit", "true")
                .setProperty("auto.commit.interval.ms", "1000")
                .setProperty("partition.discovery.interval.ms", "10000")
                .setStartingOffsets(OffsetsInitializer.committedOffsets(OffsetResetStrategy.EARLIEST))
                .setValueOnlyDeserializer(new SimpleStringSchema())
                .build();

        // 从 Kafka 数据源读取数据，不设置水印策略（处理时间窗口不存在数据乱序问题）
        DataStreamSource<String> streamSource = env.fromSource(source, WatermarkStrategy.noWatermarks(), "Kafka Source");

        // 算子
        SingleOutputStreamOperator<JSONObject> operator = streamSource
                .map(new MapFunction<String, JSONObject>() {
                    @Override
                    public JSONObject map(String str) throws Exception {
                        UserInfoEntity userInfoEntity = JSONObject.parseObject(str, UserInfoEntity.class);
                        return JSONObject.of(
                                "province", userInfoEntity.getProvince(),
                                "score", userInfoEntity.getScore());
                    }
                })
                .keyBy(new KeySelector<JSONObject, String>() {
                    @Override
                    public String getKey(JSONObject jsonObject) throws Exception {
                        return jsonObject.getString("province");
                    }
                })
                .window(TumblingProcessingTimeWindows.of(Duration.ofMinutes(1)))
                .apply(new WindowFunction<JSONObject, JSONObject, String, TimeWindow>() {
                    @Override
                    public void apply(String str, TimeWindow timeWindow, Iterable<JSONObject> iterable, Collector<JSONObject> collector) throws Exception {
                        long start = timeWindow.getStart();
                        long end = timeWindow.getEnd();
                        JSONObject json = JSONObject.of("start", DateUtil.format(DateUtil.date(start), "yyyy-MM-dd HH:mm:ss.SSS"), "end", DateUtil.format(DateUtil.date(end), "yyyy-MM-dd HH:mm:ss.SSS"));
                        Double score = 0.0;
                        Long count = 0L;
                        for (JSONObject jsonObject : iterable) {
                            score += jsonObject.getDouble("score");
                            count ++;
                        }
                        json.put("province", str);
                        json.put("score", score);
                        json.put("count", count);
                        json.put("dateTime", DateUtil.format(DateUtil.date(), "yyyy-MM-dd HH:mm:ss.SSS"));
                        collector.collect(json);
                    }
                });
        operator.print("sink");

        // 执行流处理作业
        env.execute("Kafka Stream");
    }

}
```

![image-20250116080110680](./assets/image-20250116080110680.png)



### 滑动窗口 (Sliding Window)

滑动窗口与滚动窗口类似，但窗口在时间上有重叠。它的大小固定，且每隔一个指定的时间步长滑动一次。在滑动窗口中，多个窗口可能共享相同的事件，因此可以实现更细粒度的计算。比如，一个5分钟大小、每2分钟滑动的窗口会包含不同的事件。滑动窗口适用于实时、连续计算的场景，如计算每分钟的移动平均值，能更灵活地处理流数据。

参考：[官方文档](https://nightlies.apache.org/flink/flink-docs-release-1.19/zh/docs/dev/datastream/operators/windows/)

#### 事件时间WindowAll

```java
package local.ateng.java.DataStream.window.sliding;

import cn.hutool.core.date.DateUtil;
import cn.hutool.core.date.LocalDateTimeUtil;
import com.alibaba.fastjson2.JSONArray;
import com.alibaba.fastjson2.JSONObject;
import local.ateng.java.entity.UserInfoEntity;
import org.apache.flink.api.common.eventtime.WatermarkStrategy;
import org.apache.flink.api.common.serialization.SimpleStringSchema;
import org.apache.flink.connector.kafka.source.KafkaSource;
import org.apache.flink.connector.kafka.source.enumerator.initializer.OffsetsInitializer;
import org.apache.flink.streaming.api.CheckpointingMode;
import org.apache.flink.streaming.api.datastream.DataStreamSource;
import org.apache.flink.streaming.api.datastream.SingleOutputStreamOperator;
import org.apache.flink.streaming.api.environment.StreamExecutionEnvironment;
import org.apache.flink.streaming.api.functions.windowing.AllWindowFunction;
import org.apache.flink.streaming.api.windowing.assigners.SlidingEventTimeWindows;
import org.apache.flink.streaming.api.windowing.assigners.SlidingProcessingTimeWindows;
import org.apache.flink.streaming.api.windowing.windows.TimeWindow;
import org.apache.flink.util.Collector;
import org.apache.kafka.clients.consumer.OffsetResetStrategy;

import java.time.Duration;

/**
 * 可以在普通 DataStream 上定义 Window。 Window 根据某些特征（例如，最近 5 秒内到达的数据）对所有流事件进行分组。
 * DataStream → AllWindowedStream → DataStream
 * https://nightlies.apache.org/flink/flink-docs-release-1.19/zh/docs/dev/datastream/operators/windows/#%e6%bb%91%e5%8a%a8%e7%aa%97%e5%8f%a3sliding-windows
 *
 * 这段代码的主要功能是：
 * 从 Kafka 消息队列读取 JSON 格式的数据，数据包含时间戳（createTime）。
 * 使用事件时间来处理数据，并对乱序事件提供 5 秒的容忍度。
 * 使用 基于事件时间的滑动窗口（2 分钟大小，1 分钟滑动一次）对数据进行窗口聚合。
 * 每个窗口计算时，输出窗口的开始时间、结束时间、窗口内的数据量、首尾数据等信息。
 * 将计算结果输出到控制台。
 *
 * @author 孔余
 * @email 2385569970@qq.com
 * @since 2025-01-15
 */
public class EventTimeWindowsAll {

    public static void main(String[] args) throws Exception {
        // 环境准备
        StreamExecutionEnvironment env = StreamExecutionEnvironment.getExecutionEnvironment();
        env.enableCheckpointing(3 * 1000, CheckpointingMode.EXACTLY_ONCE);
        env.setParallelism(1);
        KafkaSource<String> source = KafkaSource.<String>builder()
                .setBootstrapServers("192.168.1.10:9094")
                .setTopics("ateng_flink_json")
                .setGroupId("ateng")
                .setProperty("commit.offsets.on.checkpoint", "true")
                .setProperty("enable.auto.commit", "true")
                .setProperty("auto.commit.interval.ms", "1000")
                .setProperty("partition.discovery.interval.ms", "10000")
                .setStartingOffsets(OffsetsInitializer.committedOffsets(OffsetResetStrategy.EARLIEST))
                .setValueOnlyDeserializer(new SimpleStringSchema())
                .build();

        // 定义水印策略：WatermarkStrategy 可以在 Flink 应用程序中的两处使用，第一种是直接在数据源上使用，第二种是直接在非数据源的操作之后使用。
        // 允许最多 5 秒的事件时间乱序，使用 createTime 字段为事件时间戳（毫秒）
        WatermarkStrategy<String> watermarkStrategy = WatermarkStrategy.<String>forBoundedOutOfOrderness(Duration.ofSeconds(5))
                .withTimestampAssigner(
                        (event, recordTimestamp) -> {
                            // 解析 JSON 格式的事件，并获取事件时间
                            UserInfoEntity user = JSONObject.parseObject(event).toJavaObject(UserInfoEntity.class);
                            long timestamp = LocalDateTimeUtil.toEpochMilli(user.getCreateTime());
                            return timestamp;
                        });

        // 从 Kafka 数据源读取数据，设置水印策略
        DataStreamSource<String> streamSource = env.fromSource(source, watermarkStrategy, "Kafka Source");

        // 窗口
        SingleOutputStreamOperator<JSONObject> operator = streamSource
                // 设置水印策略为事件时间
                //.assignTimestampsAndWatermarks(watermarkStrategy)
                // 滑动窗口：2分钟窗口数据，1分钟刷新一次数据（整个数据区间就是前2分钟）
                .windowAll(SlidingEventTimeWindows.of(Duration.ofMinutes(2), Duration.ofMinutes(1)))
                .apply(new AllWindowFunction<String, JSONObject, TimeWindow>() {
                    @Override
                    public void apply(TimeWindow timeWindow, Iterable<String> iterable, Collector<JSONObject> collector) throws Exception {
                        long start = timeWindow.getStart();
                        long end = timeWindow.getEnd();
                        JSONObject json = JSONObject.of("start", DateUtil.format(DateUtil.date(start), "yyyy-MM-dd HH:mm:ss.SSS"), "end", DateUtil.format(DateUtil.date(end), "yyyy-MM-dd HH:mm:ss.SSS"));
                        JSONArray jsonArray = JSONArray.of();
                        for (String string : iterable) {
                            jsonArray.add(JSONObject.parseObject(string));
                        }
                        int size = jsonArray.size();
                        json.put("data^", jsonArray.get(0));
                        json.put("data$", jsonArray.get(size - 1));
                        json.put("size", size);
                        json.put("dateTime", DateUtil.format(DateUtil.date(), "yyyy-MM-dd HH:mm:ss.SSS"));
                        collector.collect(json);
                    }
                });
        operator.print("sink");

        // 执行流处理作业
        env.execute("Kafka Stream");
    }

}
```

![image-20250116161842218](./assets/image-20250116161842218.png)

#### 处理时间WindowAll

```java
package local.ateng.java.DataStream.window.sliding;

import cn.hutool.core.date.DateUtil;
import com.alibaba.fastjson2.JSONArray;
import com.alibaba.fastjson2.JSONObject;
import org.apache.flink.api.common.eventtime.WatermarkStrategy;
import org.apache.flink.api.common.serialization.SimpleStringSchema;
import org.apache.flink.connector.kafka.source.KafkaSource;
import org.apache.flink.connector.kafka.source.enumerator.initializer.OffsetsInitializer;
import org.apache.flink.streaming.api.CheckpointingMode;
import org.apache.flink.streaming.api.datastream.DataStreamSource;
import org.apache.flink.streaming.api.datastream.SingleOutputStreamOperator;
import org.apache.flink.streaming.api.environment.StreamExecutionEnvironment;
import org.apache.flink.streaming.api.functions.windowing.AllWindowFunction;
import org.apache.flink.streaming.api.windowing.assigners.SlidingEventTimeWindows;
import org.apache.flink.streaming.api.windowing.assigners.SlidingProcessingTimeWindows;
import org.apache.flink.streaming.api.windowing.windows.TimeWindow;
import org.apache.flink.util.Collector;
import org.apache.kafka.clients.consumer.OffsetResetStrategy;

import java.time.Duration;

/**
 * 可以在普通 DataStream 上定义 Window。 Window 根据某些特征（例如，最近 5 秒内到达的数据）对所有流事件进行分组。
 * DataStream → AllWindowedStream → DataStream
 * https://nightlies.apache.org/flink/flink-docs-release-1.19/zh/docs/dev/datastream/operators/windows/#%e6%bb%91%e5%8a%a8%e7%aa%97%e5%8f%a3sliding-windows
 *
 * 这段代码的主要功能是：
 * 使用 基于处理时间的滑动窗口（2 分钟大小，1 分钟滑动一次）对数据进行窗口聚合。
 * 每个窗口计算时，输出窗口的开始时间、结束时间、窗口内的数据量、首尾数据等信息。
 * 将计算结果输出到控制台。
 *
 * @author 孔余
 * @email 2385569970@qq.com
 * @since 2025-01-15
 */
public class ProcessingTimeWindowsAll {

    public static void main(String[] args) throws Exception {
        // 环境准备
        StreamExecutionEnvironment env = StreamExecutionEnvironment.getExecutionEnvironment();
        env.enableCheckpointing(3 * 1000, CheckpointingMode.EXACTLY_ONCE);
        env.setParallelism(1);
        KafkaSource<String> source = KafkaSource.<String>builder()
                .setBootstrapServers("192.168.1.10:9094")
                .setTopics("ateng_flink_json")
                .setGroupId("ateng")
                .setProperty("commit.offsets.on.checkpoint", "true")
                .setProperty("enable.auto.commit", "true")
                .setProperty("auto.commit.interval.ms", "1000")
                .setProperty("partition.discovery.interval.ms", "10000")
                .setStartingOffsets(OffsetsInitializer.committedOffsets(OffsetResetStrategy.EARLIEST))
                .setValueOnlyDeserializer(new SimpleStringSchema())
                .build();

        // 从 Kafka 数据源读取数据，不设置水印策略（处理时间窗口不存在数据乱序问题）
        DataStreamSource<String> streamSource = env.fromSource(source, WatermarkStrategy.noWatermarks(), "Kafka Source");

        // 窗口
        SingleOutputStreamOperator<JSONObject> operator = streamSource
                // 滑动窗口：2分钟窗口数据，1分钟刷新一次数据（整个数据区间就是前2分钟）
                .windowAll(SlidingProcessingTimeWindows.of(Duration.ofMinutes(2), Duration.ofMinutes(1)))
                .apply(new AllWindowFunction<String, JSONObject, TimeWindow>() {
                    @Override
                    public void apply(TimeWindow timeWindow, Iterable<String> iterable, Collector<JSONObject> collector) throws Exception {
                        long start = timeWindow.getStart();
                        long end = timeWindow.getEnd();
                        JSONObject json = JSONObject.of("start", DateUtil.format(DateUtil.date(start), "yyyy-MM-dd HH:mm:ss.SSS"), "end", DateUtil.format(DateUtil.date(end), "yyyy-MM-dd HH:mm:ss.SSS"));
                        JSONArray jsonArray = JSONArray.of();
                        for (String string : iterable) {
                            jsonArray.add(JSONObject.parseObject(string));
                        }
                        int size = jsonArray.size();
                        json.put("data^", jsonArray.get(0));
                        json.put("data$", jsonArray.get(size - 1));
                        json.put("size", size);
                        json.put("dateTime", DateUtil.format(DateUtil.date(), "yyyy-MM-dd HH:mm:ss.SSS"));
                        collector.collect(json);
                    }
                });
        operator.print("sink");

        // 执行流处理作业
        env.execute("Kafka Stream");
    }

}
```

![image-20250116080706492](./assets/image-20250116080706492.png)

#### 事件时间Window

```java
package local.ateng.java.DataStream.window.sliding;

import cn.hutool.core.date.DateUtil;
import cn.hutool.core.date.LocalDateTimeUtil;
import com.alibaba.fastjson2.JSONObject;
import local.ateng.java.entity.UserInfoEntity;
import org.apache.flink.api.common.eventtime.WatermarkStrategy;
import org.apache.flink.api.common.functions.MapFunction;
import org.apache.flink.api.common.serialization.SimpleStringSchema;
import org.apache.flink.api.java.functions.KeySelector;
import org.apache.flink.connector.kafka.source.KafkaSource;
import org.apache.flink.connector.kafka.source.enumerator.initializer.OffsetsInitializer;
import org.apache.flink.streaming.api.CheckpointingMode;
import org.apache.flink.streaming.api.datastream.DataStreamSource;
import org.apache.flink.streaming.api.datastream.SingleOutputStreamOperator;
import org.apache.flink.streaming.api.environment.StreamExecutionEnvironment;
import org.apache.flink.streaming.api.functions.windowing.WindowFunction;
import org.apache.flink.streaming.api.windowing.assigners.SlidingEventTimeWindows;
import org.apache.flink.streaming.api.windowing.windows.TimeWindow;
import org.apache.flink.util.Collector;
import org.apache.kafka.clients.consumer.OffsetResetStrategy;

import java.time.Duration;

/**
 * 可以在已经分区的 KeyedStreams 上定义 Window。Window 根据某些特征（例如，最近 5 秒内到达的数据）对每个 key Stream 中的数据进行分组。
 * KeyedStream → WindowedStream → DataStream
 * https://nightlies.apache.org/flink/flink-docs-release-1.19/zh/docs/dev/datastream/operators/overview/#window
 * <p>
 * 这个 Flink 程序的主要功能是：
 * 从 Kafka 读取 JSON 格式的用户数据，每条数据包含 province（省份）和 score（分数）信息。
 * 将数据按 province 字段进行分组，并基于处理时间（ProcessingTime）使用滚动窗口（2 分钟大小，1 分钟滑动一次）对每个省份的 score 进行聚合。
 * 每个窗口内，程序计算该省份的总分数，并输出窗口的开始时间、结束时间、总分数以及当前时间。
 *
 * @author 孔余
 * @email 2385569970@qq.com
 * @since 2025-01-15
 */
public class EventTimeWindowKeyBy {

    public static void main(String[] args) throws Exception {
        // 环境准备
        StreamExecutionEnvironment env = StreamExecutionEnvironment.getExecutionEnvironment();
        env.enableCheckpointing(5 * 1000, CheckpointingMode.EXACTLY_ONCE);
        env.setParallelism(3);
        KafkaSource<String> source = KafkaSource.<String>builder()
                .setBootstrapServers("192.168.1.10:9094")
                .setTopics("ateng_flink_json")
                .setGroupId("ateng")
                .setProperty("commit.offsets.on.checkpoint", "true")
                .setProperty("enable.auto.commit", "true")
                .setProperty("auto.commit.interval.ms", "1000")
                .setProperty("partition.discovery.interval.ms", "10000")
                .setStartingOffsets(OffsetsInitializer.committedOffsets(OffsetResetStrategy.EARLIEST))
                .setValueOnlyDeserializer(new SimpleStringSchema())
                .build();

        // 定义水印策略：WatermarkStrategy 可以在 Flink 应用程序中的两处使用，第一种是直接在数据源上使用，第二种是直接在非数据源的操作之后使用。
        // 允许最多 5 秒的事件时间乱序，使用 createTime 字段为事件时间戳（毫秒）
        WatermarkStrategy<String> watermarkStrategy = WatermarkStrategy.<String>forBoundedOutOfOrderness(Duration.ofSeconds(5))
                .withTimestampAssigner(
                        (event, recordTimestamp) -> {
                            // 解析 JSON 格式的事件，并获取事件时间
                            UserInfoEntity user = JSONObject.parseObject(event).toJavaObject(UserInfoEntity.class);
                            long timestamp = LocalDateTimeUtil.toEpochMilli(user.getCreateTime());
                            return timestamp;
                        });

        // 从 Kafka 数据源读取数据，设置水印策略
        DataStreamSource<String> streamSource = env.fromSource(source, watermarkStrategy, "Kafka Source");

        // 算子
        SingleOutputStreamOperator<JSONObject> operator = streamSource
                // 设置水印策略为事件时间
                //.assignTimestampsAndWatermarks(watermarkStrategy)
                .map(new MapFunction<String, JSONObject>() {
                    @Override
                    public JSONObject map(String str) throws Exception {
                        UserInfoEntity userInfoEntity = JSONObject.parseObject(str, UserInfoEntity.class);
                        return JSONObject.of(
                                "province", userInfoEntity.getProvince(),
                                "score", userInfoEntity.getScore());
                    }
                })
                .keyBy(new KeySelector<JSONObject, String>() {
                    @Override
                    public String getKey(JSONObject jsonObject) throws Exception {
                        return jsonObject.getString("province");
                    }
                })
                // 滑动窗口：2分钟窗口数据，1分钟刷新一次数据（整个数据区间就是前2分钟）
                .window(SlidingEventTimeWindows.of(Duration.ofMinutes(2), Duration.ofMinutes(1)))
                .apply(new WindowFunction<JSONObject, JSONObject, String, TimeWindow>() {
                    @Override
                    public void apply(String str, TimeWindow timeWindow, Iterable<JSONObject> iterable, Collector<JSONObject> collector) throws Exception {
                        long start = timeWindow.getStart();
                        long end = timeWindow.getEnd();
                        JSONObject json = JSONObject.of("start", DateUtil.format(DateUtil.date(start), "yyyy-MM-dd HH:mm:ss.SSS"), "end", DateUtil.format(DateUtil.date(end), "yyyy-MM-dd HH:mm:ss.SSS"));
                        Double score = 0.0;
                        Long count = 0L;
                        for (JSONObject jsonObject : iterable) {
                            score += jsonObject.getDouble("score");
                            count++;
                        }
                        json.put("province", str);
                        json.put("score", score);
                        json.put("count", count);
                        json.put("dateTime", DateUtil.format(DateUtil.date(), "yyyy-MM-dd HH:mm:ss.SSS"));
                        collector.collect(json);
                    }
                });
        operator.print("sink");

        // 执行流处理作业
        env.execute("Kafka Stream");
    }

}
```

![image-20250116162806572](./assets/image-20250116162806572.png)

#### 处理时间Window

```java
package local.ateng.java.DataStream.window.sliding;

import cn.hutool.core.date.DateUtil;
import com.alibaba.fastjson2.JSONObject;
import local.ateng.java.entity.UserInfoEntity;
import org.apache.flink.api.common.eventtime.WatermarkStrategy;
import org.apache.flink.api.common.functions.MapFunction;
import org.apache.flink.api.common.serialization.SimpleStringSchema;
import org.apache.flink.api.java.functions.KeySelector;
import org.apache.flink.connector.kafka.source.KafkaSource;
import org.apache.flink.connector.kafka.source.enumerator.initializer.OffsetsInitializer;
import org.apache.flink.streaming.api.CheckpointingMode;
import org.apache.flink.streaming.api.datastream.DataStreamSource;
import org.apache.flink.streaming.api.datastream.SingleOutputStreamOperator;
import org.apache.flink.streaming.api.environment.StreamExecutionEnvironment;
import org.apache.flink.streaming.api.functions.windowing.WindowFunction;
import org.apache.flink.streaming.api.windowing.assigners.SlidingProcessingTimeWindows;
import org.apache.flink.streaming.api.windowing.windows.TimeWindow;
import org.apache.flink.util.Collector;
import org.apache.kafka.clients.consumer.OffsetResetStrategy;

import java.time.Duration;

/**
 * 可以在已经分区的 KeyedStreams 上定义 Window。Window 根据某些特征（例如，最近 5 秒内到达的数据）对每个 key Stream 中的数据进行分组。
 * KeyedStream → WindowedStream → DataStream
 * https://nightlies.apache.org/flink/flink-docs-release-1.19/zh/docs/dev/datastream/operators/overview/#window
 * <p>
 * 这个 Flink 程序的主要功能是：
 * 从 Kafka 读取 JSON 格式的用户数据，每条数据包含 province（省份）和 score（分数）信息。
 * 将数据按 province 字段进行分组，并基于处理时间（ProcessingTime）使用滑动窗口（2 分钟大小，1 分钟滑动一次）对每个省份的 score 进行聚合。
 * 每个窗口内，程序计算该省份的总分数，并输出窗口的开始时间、结束时间、总分数以及当前时间。
 *
 * @author 孔余
 * @email 2385569970@qq.com
 * @since 2025-01-15
 */
public class ProcessingTimeWindowKeyBy {

    public static void main(String[] args) throws Exception {
        // 环境准备
        StreamExecutionEnvironment env = StreamExecutionEnvironment.getExecutionEnvironment();
        env.enableCheckpointing(5 * 1000, CheckpointingMode.EXACTLY_ONCE);
        env.setParallelism(3);
        KafkaSource<String> source = KafkaSource.<String>builder()
                .setBootstrapServers("192.168.1.10:9094")
                .setTopics("ateng_flink_json")
                .setGroupId("ateng")
                .setProperty("commit.offsets.on.checkpoint", "true")
                .setProperty("enable.auto.commit", "true")
                .setProperty("auto.commit.interval.ms", "1000")
                .setProperty("partition.discovery.interval.ms", "10000")
                .setStartingOffsets(OffsetsInitializer.committedOffsets(OffsetResetStrategy.EARLIEST))
                .setValueOnlyDeserializer(new SimpleStringSchema())
                .build();

        // 从 Kafka 数据源读取数据，不设置水印策略（处理时间窗口不存在数据乱序问题）
        DataStreamSource<String> streamSource = env.fromSource(source, WatermarkStrategy.noWatermarks(), "Kafka Source");

        // 算子
        SingleOutputStreamOperator<JSONObject> operator = streamSource
                .map(new MapFunction<String, JSONObject>() {
                    @Override
                    public JSONObject map(String str) throws Exception {
                        UserInfoEntity userInfoEntity = JSONObject.parseObject(str, UserInfoEntity.class);
                        return JSONObject.of(
                                "province", userInfoEntity.getProvince(),
                                "score", userInfoEntity.getScore());
                    }
                })
                .keyBy(new KeySelector<JSONObject, String>() {
                    @Override
                    public String getKey(JSONObject jsonObject) throws Exception {
                        return jsonObject.getString("province");
                    }
                })
                // 滑动窗口：2分钟窗口数据，1分钟刷新一次数据（整个数据区间就是前2分钟）
                .window(SlidingProcessingTimeWindows.of(Duration.ofMinutes(2), Duration.ofMinutes(1)))
                .apply(new WindowFunction<JSONObject, JSONObject, String, TimeWindow>() {
                    @Override
                    public void apply(String str, TimeWindow timeWindow, Iterable<JSONObject> iterable, Collector<JSONObject> collector) throws Exception {
                        long start = timeWindow.getStart();
                        long end = timeWindow.getEnd();
                        JSONObject json = JSONObject.of("start", DateUtil.format(DateUtil.date(start), "yyyy-MM-dd HH:mm:ss.SSS"), "end", DateUtil.format(DateUtil.date(end), "yyyy-MM-dd HH:mm:ss.SSS"));
                        Double score = 0.0;
                        Long count = 0L;
                        for (JSONObject jsonObject : iterable) {
                            score += jsonObject.getDouble("score");
                            count++;
                        }
                        json.put("province", str);
                        json.put("score", score);
                        json.put("count", count);
                        json.put("dateTime", DateUtil.format(DateUtil.date(), "yyyy-MM-dd HH:mm:ss.SSS"));
                        collector.collect(json);
                    }
                });
        operator.print("sink");

        // 执行流处理作业
        env.execute("Kafka Stream");
    }

}
```

![image-20250116162451675](./assets/image-20250116162451675.png)



### 会话窗口 (Session Window)

会话窗口基于事件之间的间隔动态划分窗口。当两个事件的时间间隔超过预定的“会话间隔”阈值时，窗口关闭并触发计算。会话窗口适合处理用户行为、点击流等不规则的流数据，例如，一个用户的连续操作可能属于同一个会话，若没有新事件到达，则会话会结束。这种窗口模式非常适合分析活动聚集型的数据，如网站会话和社交媒体用户行为。

参考：[官方文档](https://nightlies.apache.org/flink/flink-docs-release-1.19/zh/docs/dev/datastream/operators/windows/)

#### 事件时间WindowAll

```java
package local.ateng.java.DataStream.window.session;

import cn.hutool.core.date.DateUtil;
import cn.hutool.core.date.LocalDateTimeUtil;
import com.alibaba.fastjson2.JSONArray;
import com.alibaba.fastjson2.JSONObject;
import local.ateng.java.entity.UserInfoEntity;
import org.apache.flink.api.common.eventtime.WatermarkStrategy;
import org.apache.flink.api.common.serialization.SimpleStringSchema;
import org.apache.flink.connector.kafka.source.KafkaSource;
import org.apache.flink.connector.kafka.source.enumerator.initializer.OffsetsInitializer;
import org.apache.flink.streaming.api.CheckpointingMode;
import org.apache.flink.streaming.api.datastream.DataStreamSource;
import org.apache.flink.streaming.api.datastream.SingleOutputStreamOperator;
import org.apache.flink.streaming.api.environment.StreamExecutionEnvironment;
import org.apache.flink.streaming.api.functions.windowing.AllWindowFunction;
import org.apache.flink.streaming.api.windowing.assigners.EventTimeSessionWindows;
import org.apache.flink.streaming.api.windowing.assigners.ProcessingTimeSessionWindows;
import org.apache.flink.streaming.api.windowing.windows.TimeWindow;
import org.apache.flink.util.Collector;
import org.apache.kafka.clients.consumer.OffsetResetStrategy;

import java.time.Duration;

/**
 * 可以在普通 DataStream 上定义 Window。 Window 根据某些特征（例如，最近 5 秒内到达的数据）对所有流事件进行分组。
 * DataStream → AllWindowedStream → DataStream
 * https://nightlies.apache.org/flink/flink-docs-release-1.19/zh/docs/dev/datastream/operators/windows/#%e6%bb%91%e5%8a%a8%e7%aa%97%e5%8f%a3sliding-windows
 *
 * 这段代码的功能如下：
 * 从 Kafka 主题读取数据流，以字符串形式消费事件。
 * 使用 事件时间会话窗口，窗口间隔为 2 分钟：
 * 如果事件之间的时间间隔超过 2 分钟，则窗口关闭，新窗口开启。
 * 每个窗口触发时，提取窗口的时间范围、数据数量、首尾事件，并构建一个结果 JSON 输出。
 * 最终的窗口处理结果打印到控制台。
 * 这种会话窗口特别适合处理 非固定间隔的数据流，如用户会话数据分析或系统事件日志聚合。
 *
 * @author 孔余
 * @email 2385569970@qq.com
 * @since 2025-01-15
 */
public class EventTimeWindowsAll {

    public static void main(String[] args) throws Exception {
        // 环境准备
        StreamExecutionEnvironment env = StreamExecutionEnvironment.getExecutionEnvironment();
        env.enableCheckpointing(3 * 1000, CheckpointingMode.EXACTLY_ONCE);
        env.setParallelism(1);
        KafkaSource<String> source = KafkaSource.<String>builder()
                .setBootstrapServers("192.168.1.10:9094")
                .setTopics("ateng_flink_json")
                .setGroupId("ateng")
                .setProperty("commit.offsets.on.checkpoint", "true")
                .setProperty("enable.auto.commit", "true")
                .setProperty("auto.commit.interval.ms", "1000")
                .setProperty("partition.discovery.interval.ms", "10000")
                .setStartingOffsets(OffsetsInitializer.committedOffsets(OffsetResetStrategy.EARLIEST))
                .setValueOnlyDeserializer(new SimpleStringSchema())
                .build();

        // 定义水印策略：WatermarkStrategy 可以在 Flink 应用程序中的两处使用，第一种是直接在数据源上使用，第二种是直接在非数据源的操作之后使用。
        // 允许最多 5 秒的事件时间乱序，使用 createTime 字段为事件时间戳（毫秒）
        WatermarkStrategy<String> watermarkStrategy = WatermarkStrategy.<String>forBoundedOutOfOrderness(Duration.ofSeconds(5))
                .withTimestampAssigner(
                        (event, recordTimestamp) -> {
                            // 解析 JSON 格式的事件，并获取事件时间
                            UserInfoEntity user = JSONObject.parseObject(event).toJavaObject(UserInfoEntity.class);
                            long timestamp = LocalDateTimeUtil.toEpochMilli(user.getCreateTime());
                            return timestamp;
                        });

        // 从 Kafka 数据源读取数据，不设置水印策略（处理时间窗口不存在数据乱序问题）
        DataStreamSource<String> streamSource = env.fromSource(source, watermarkStrategy, "Kafka Source");

        // 窗口
        SingleOutputStreamOperator<JSONObject> operator = streamSource
                // 设置水印策略为事件时间
                //.assignTimestampsAndWatermarks(watermarkStrategy)
                // 会话窗口：2分钟窗口数据
                // 表示会话窗口的间隔为 2 分钟。也就是说，窗口会根据事件之间的时间间隔划分。如果两个事件之间的时间差超过了 2 分钟，那么当前的会话窗口就结束，接下来的事件会形成新的窗口。
                .windowAll(EventTimeSessionWindows.withGap(Duration.ofMinutes(2)))
                .apply(new AllWindowFunction<String, JSONObject, TimeWindow>() {
                    @Override
                    public void apply(TimeWindow timeWindow, Iterable<String> iterable, Collector<JSONObject> collector) throws Exception {
                        long start = timeWindow.getStart();
                        long end = timeWindow.getEnd();
                        JSONObject json = JSONObject.of("start", DateUtil.format(DateUtil.date(start), "yyyy-MM-dd HH:mm:ss.SSS"), "end", DateUtil.format(DateUtil.date(end), "yyyy-MM-dd HH:mm:ss.SSS"));
                        JSONArray jsonArray = JSONArray.of();
                        for (String string : iterable) {
                            jsonArray.add(JSONObject.parseObject(string));
                        }
                        int size = jsonArray.size();
                        json.put("data^", jsonArray.get(0));
                        json.put("data$", jsonArray.get(size - 1));
                        json.put("size", size);
                        json.put("dateTime", DateUtil.format(DateUtil.date(), "yyyy-MM-dd HH:mm:ss.SSS"));
                        collector.collect(json);
                    }
                });
        operator.print("sink");

        // 执行流处理作业
        env.execute("Kafka Stream");
    }

}
```

![image-20250116192951765](./assets/image-20250116192951765.png)

#### 处理时间WindowAll

```java
package local.ateng.java.DataStream.window.session;

import cn.hutool.core.date.DateUtil;
import com.alibaba.fastjson2.JSONArray;
import com.alibaba.fastjson2.JSONObject;
import org.apache.flink.api.common.eventtime.WatermarkStrategy;
import org.apache.flink.api.common.serialization.SimpleStringSchema;
import org.apache.flink.connector.kafka.source.KafkaSource;
import org.apache.flink.connector.kafka.source.enumerator.initializer.OffsetsInitializer;
import org.apache.flink.streaming.api.CheckpointingMode;
import org.apache.flink.streaming.api.datastream.DataStreamSource;
import org.apache.flink.streaming.api.datastream.SingleOutputStreamOperator;
import org.apache.flink.streaming.api.environment.StreamExecutionEnvironment;
import org.apache.flink.streaming.api.functions.windowing.AllWindowFunction;
import org.apache.flink.streaming.api.windowing.assigners.ProcessingTimeSessionWindows;
import org.apache.flink.streaming.api.windowing.windows.TimeWindow;
import org.apache.flink.util.Collector;
import org.apache.kafka.clients.consumer.OffsetResetStrategy;

import java.time.Duration;

/**
 * 可以在普通 DataStream 上定义 Window。 Window 根据某些特征（例如，最近 5 秒内到达的数据）对所有流事件进行分组。
 * DataStream → AllWindowedStream → DataStream
 * https://nightlies.apache.org/flink/flink-docs-release-1.19/zh/docs/dev/datastream/operators/windows/#%e6%bb%91%e5%8a%a8%e7%aa%97%e5%8f%a3sliding-windows
 *
 * 这段代码的功能如下：
 * 从 Kafka 主题读取数据流，以字符串形式消费事件。
 * 使用 处理时间会话窗口，窗口间隔为 2 分钟：
 * 如果事件之间的时间间隔超过 2 分钟，则窗口关闭，新窗口开启。
 * 每个窗口触发时，提取窗口的时间范围、数据数量、首尾事件，并构建一个结果 JSON 输出。
 * 最终的窗口处理结果打印到控制台。
 * 这种会话窗口特别适合处理 非固定间隔的数据流，如用户会话数据分析或系统事件日志聚合。
 *
 * @author 孔余
 * @email 2385569970@qq.com
 * @since 2025-01-15
 */
public class ProcessingTimeWindowsAll {

    public static void main(String[] args) throws Exception {
        // 环境准备
        StreamExecutionEnvironment env = StreamExecutionEnvironment.getExecutionEnvironment();
        env.enableCheckpointing(3 * 1000, CheckpointingMode.EXACTLY_ONCE);
        env.setParallelism(1);
        KafkaSource<String> source = KafkaSource.<String>builder()
                .setBootstrapServers("192.168.1.10:9094")
                .setTopics("ateng_flink_json")
                .setGroupId("ateng")
                .setProperty("commit.offsets.on.checkpoint", "true")
                .setProperty("enable.auto.commit", "true")
                .setProperty("auto.commit.interval.ms", "1000")
                .setProperty("partition.discovery.interval.ms", "10000")
                .setStartingOffsets(OffsetsInitializer.committedOffsets(OffsetResetStrategy.EARLIEST))
                .setValueOnlyDeserializer(new SimpleStringSchema())
                .build();

        // 从 Kafka 数据源读取数据，不设置水印策略（处理时间窗口不存在数据乱序问题）
        DataStreamSource<String> streamSource = env.fromSource(source, WatermarkStrategy.noWatermarks(), "Kafka Source");

        // 窗口
        SingleOutputStreamOperator<JSONObject> operator = streamSource
                // 会话窗口：2分钟窗口数据
                // 表示会话窗口的间隔为 2 分钟。也就是说，窗口会根据事件之间的时间间隔划分。如果两个事件之间的时间差超过了 2 分钟，那么当前的会话窗口就结束，接下来的事件会形成新的窗口。
                .windowAll(ProcessingTimeSessionWindows.withGap(Duration.ofMinutes(2)))
                .apply(new AllWindowFunction<String, JSONObject, TimeWindow>() {
                    @Override
                    public void apply(TimeWindow timeWindow, Iterable<String> iterable, Collector<JSONObject> collector) throws Exception {
                        long start = timeWindow.getStart();
                        long end = timeWindow.getEnd();
                        JSONObject json = JSONObject.of("start", DateUtil.format(DateUtil.date(start), "yyyy-MM-dd HH:mm:ss.SSS"), "end", DateUtil.format(DateUtil.date(end), "yyyy-MM-dd HH:mm:ss.SSS"));
                        JSONArray jsonArray = JSONArray.of();
                        for (String string : iterable) {
                            jsonArray.add(JSONObject.parseObject(string));
                        }
                        int size = jsonArray.size();
                        json.put("data^", jsonArray.get(0));
                        json.put("data$", jsonArray.get(size - 1));
                        json.put("size", size);
                        json.put("dateTime", DateUtil.format(DateUtil.date(), "yyyy-MM-dd HH:mm:ss.SSS"));
                        collector.collect(json);
                    }
                });
        operator.print("sink");

        // 执行流处理作业
        env.execute("Kafka Stream");
    }

}
```

![image-20250116191805040](./assets/image-20250116191805040.png)

#### 事件时间Window

```java
package local.ateng.java.DataStream.window.session;

import cn.hutool.core.date.DateUtil;
import cn.hutool.core.date.LocalDateTimeUtil;
import com.alibaba.fastjson2.JSONObject;
import local.ateng.java.entity.UserInfoEntity;
import org.apache.flink.api.common.eventtime.WatermarkStrategy;
import org.apache.flink.api.common.functions.MapFunction;
import org.apache.flink.api.common.serialization.SimpleStringSchema;
import org.apache.flink.api.java.functions.KeySelector;
import org.apache.flink.connector.kafka.source.KafkaSource;
import org.apache.flink.connector.kafka.source.enumerator.initializer.OffsetsInitializer;
import org.apache.flink.streaming.api.CheckpointingMode;
import org.apache.flink.streaming.api.datastream.DataStreamSource;
import org.apache.flink.streaming.api.datastream.SingleOutputStreamOperator;
import org.apache.flink.streaming.api.environment.StreamExecutionEnvironment;
import org.apache.flink.streaming.api.functions.windowing.WindowFunction;
import org.apache.flink.streaming.api.windowing.assigners.EventTimeSessionWindows;
import org.apache.flink.streaming.api.windowing.assigners.ProcessingTimeSessionWindows;
import org.apache.flink.streaming.api.windowing.windows.TimeWindow;
import org.apache.flink.util.Collector;
import org.apache.kafka.clients.consumer.OffsetResetStrategy;

import java.time.Duration;

/**
 * 可以在已经分区的 KeyedStreams 上定义 Window。Window 根据某些特征（例如，最近 5 秒内到达的数据）对每个 key Stream 中的数据进行分组。
 * KeyedStream → WindowedStream → DataStream
 * https://nightlies.apache.org/flink/flink-docs-release-1.19/zh/docs/dev/datastream/operators/overview/#window
 *
 * 这段代码实现了以下功能：
 * Kafka 数据读取：从 Kafka 消费 JSON 格式事件数据。
 * 数据分组：按 province 分组，独立处理每个省份的数据流。
 * 会话窗口：基于 事件时间 的 2 分钟会话窗口。
 * 窗口聚合：
 * 累计每个窗口内的 score 总和和事件数量。
 * 提取窗口时间范围和分组键。
 * 结果输出：将每个窗口的聚合结果输出到控制台。
 * 这种逻辑常用于场景：
 * 实时分析用户行为，按地区（province）统计得分。
 * 分析会话行为，比如用户活跃时间段的统计。
 *
 * @author 孔余
 * @email 2385569970@qq.com
 * @since 2025-01-15
 */
public class EventTimeWindowsKeyBy {

    public static void main(String[] args) throws Exception {
        // 环境准备
        StreamExecutionEnvironment env = StreamExecutionEnvironment.getExecutionEnvironment();
        env.enableCheckpointing(3 * 1000, CheckpointingMode.EXACTLY_ONCE);
        env.setParallelism(1);
        KafkaSource<String> source = KafkaSource.<String>builder()
                .setBootstrapServers("192.168.1.10:9094")
                .setTopics("ateng_flink_json")
                .setGroupId("ateng")
                .setProperty("commit.offsets.on.checkpoint", "true")
                .setProperty("enable.auto.commit", "true")
                .setProperty("auto.commit.interval.ms", "1000")
                .setProperty("partition.discovery.interval.ms", "10000")
                .setStartingOffsets(OffsetsInitializer.committedOffsets(OffsetResetStrategy.EARLIEST))
                .setValueOnlyDeserializer(new SimpleStringSchema())
                .build();

        // 定义水印策略：WatermarkStrategy 可以在 Flink 应用程序中的两处使用，第一种是直接在数据源上使用，第二种是直接在非数据源的操作之后使用。
        // 允许最多 5 秒的事件时间乱序，使用 createTime 字段为事件时间戳（毫秒）
        WatermarkStrategy<String> watermarkStrategy = WatermarkStrategy.<String>forBoundedOutOfOrderness(Duration.ofSeconds(5))
                .withTimestampAssigner(
                        (event, recordTimestamp) -> {
                            // 解析 JSON 格式的事件，并获取事件时间
                            UserInfoEntity user = JSONObject.parseObject(event).toJavaObject(UserInfoEntity.class);
                            long timestamp = LocalDateTimeUtil.toEpochMilli(user.getCreateTime());
                            return timestamp;
                        });

        // 从 Kafka 数据源读取数据，不设置水印策略（处理时间窗口不存在数据乱序问题）
        DataStreamSource<String> streamSource = env.fromSource(source, watermarkStrategy, "Kafka Source");

        // 窗口
        SingleOutputStreamOperator<JSONObject> operator = streamSource
                // 设置水印策略为事件时间
                //.assignTimestampsAndWatermarks(watermarkStrategy)
                .map(new MapFunction<String, JSONObject>() {
                    @Override
                    public JSONObject map(String str) throws Exception {
                        UserInfoEntity userInfoEntity = JSONObject.parseObject(str, UserInfoEntity.class);
                        return JSONObject.of(
                                "province", userInfoEntity.getProvince(),
                                "score", userInfoEntity.getScore());
                    }
                })
                .keyBy(new KeySelector<JSONObject, String>() {
                    @Override
                    public String getKey(JSONObject jsonObject) throws Exception {
                        return jsonObject.getString("province");
                    }
                })
                // 会话窗口：2分钟窗口数据
                // 表示会话窗口的间隔为 2 分钟。也就是说，窗口会根据事件之间的时间间隔划分。如果两个事件之间的时间差超过了 2 分钟，那么当前的会话窗口就结束，接下来的事件会形成新的窗口。
                .window(EventTimeSessionWindows.withGap(Duration.ofMinutes(2)))
                .apply(new WindowFunction<JSONObject, JSONObject, String, TimeWindow>() {
                    @Override
                    public void apply(String str, TimeWindow timeWindow, Iterable<JSONObject> iterable, Collector<JSONObject> collector) throws Exception {
                        long start = timeWindow.getStart();
                        long end = timeWindow.getEnd();
                        JSONObject json = JSONObject.of("start", DateUtil.format(DateUtil.date(start), "yyyy-MM-dd HH:mm:ss.SSS"), "end", DateUtil.format(DateUtil.date(end), "yyyy-MM-dd HH:mm:ss.SSS"));
                        Double score = 0.0;
                        Long count = 0L;
                        for (JSONObject jsonObject : iterable) {
                            score += jsonObject.getDouble("score");
                            count++;
                        }
                        json.put("province", str);
                        json.put("score", score);
                        json.put("count", count);
                        json.put("dateTime", DateUtil.format(DateUtil.date(), "yyyy-MM-dd HH:mm:ss.SSS"));
                        collector.collect(json);
                    }
                });
        operator.print("sink");

        // 执行流处理作业
        env.execute("Kafka Stream");
    }

}
```

![image-20250116195151902](./assets/image-20250116195151902.png)

#### 处理时间Window

```java
package local.ateng.java.DataStream.window.session;

import cn.hutool.core.date.DateUtil;
import com.alibaba.fastjson2.JSONArray;
import com.alibaba.fastjson2.JSONObject;
import local.ateng.java.entity.UserInfoEntity;
import org.apache.flink.api.common.eventtime.WatermarkStrategy;
import org.apache.flink.api.common.functions.MapFunction;
import org.apache.flink.api.common.serialization.SimpleStringSchema;
import org.apache.flink.api.java.functions.KeySelector;
import org.apache.flink.connector.kafka.source.KafkaSource;
import org.apache.flink.connector.kafka.source.enumerator.initializer.OffsetsInitializer;
import org.apache.flink.streaming.api.CheckpointingMode;
import org.apache.flink.streaming.api.datastream.DataStreamSource;
import org.apache.flink.streaming.api.datastream.SingleOutputStreamOperator;
import org.apache.flink.streaming.api.environment.StreamExecutionEnvironment;
import org.apache.flink.streaming.api.functions.windowing.AllWindowFunction;
import org.apache.flink.streaming.api.functions.windowing.WindowFunction;
import org.apache.flink.streaming.api.windowing.assigners.ProcessingTimeSessionWindows;
import org.apache.flink.streaming.api.windowing.assigners.SlidingProcessingTimeWindows;
import org.apache.flink.streaming.api.windowing.windows.TimeWindow;
import org.apache.flink.util.Collector;
import org.apache.kafka.clients.consumer.OffsetResetStrategy;

import java.time.Duration;

/**
 * 可以在已经分区的 KeyedStreams 上定义 Window。Window 根据某些特征（例如，最近 5 秒内到达的数据）对每个 key Stream 中的数据进行分组。
 * KeyedStream → WindowedStream → DataStream
 * https://nightlies.apache.org/flink/flink-docs-release-1.19/zh/docs/dev/datastream/operators/overview/#window
 *
 * 这段代码实现了以下功能：
 * Kafka 数据读取：从 Kafka 消费 JSON 格式事件数据。
 * 数据分组：按 province 分组，独立处理每个省份的数据流。
 * 会话窗口：基于 处理时间 的 2 分钟会话窗口。
 * 窗口聚合：
 * 累计每个窗口内的 score 总和和事件数量。
 * 提取窗口时间范围和分组键。
 * 结果输出：将每个窗口的聚合结果输出到控制台。
 * 这种逻辑常用于场景：
 * 实时分析用户行为，按地区（province）统计得分。
 * 分析会话行为，比如用户活跃时间段的统计。
 *
 * @author 孔余
 * @email 2385569970@qq.com
 * @since 2025-01-15
 */
public class ProcessingTimeWindowsKeyBy {

    public static void main(String[] args) throws Exception {
        // 环境准备
        StreamExecutionEnvironment env = StreamExecutionEnvironment.getExecutionEnvironment();
        env.enableCheckpointing(3 * 1000, CheckpointingMode.EXACTLY_ONCE);
        env.setParallelism(1);
        KafkaSource<String> source = KafkaSource.<String>builder()
                .setBootstrapServers("192.168.1.10:9094")
                .setTopics("ateng_flink_json")
                .setGroupId("ateng")
                .setProperty("commit.offsets.on.checkpoint", "true")
                .setProperty("enable.auto.commit", "true")
                .setProperty("auto.commit.interval.ms", "1000")
                .setProperty("partition.discovery.interval.ms", "10000")
                .setStartingOffsets(OffsetsInitializer.committedOffsets(OffsetResetStrategy.EARLIEST))
                .setValueOnlyDeserializer(new SimpleStringSchema())
                .build();

        // 从 Kafka 数据源读取数据，不设置水印策略（处理时间窗口不存在数据乱序问题）
        DataStreamSource<String> streamSource = env.fromSource(source, WatermarkStrategy.noWatermarks(), "Kafka Source");

        // 窗口
        SingleOutputStreamOperator<JSONObject> operator = streamSource
                .map(new MapFunction<String, JSONObject>() {
                    @Override
                    public JSONObject map(String str) throws Exception {
                        UserInfoEntity userInfoEntity = JSONObject.parseObject(str, UserInfoEntity.class);
                        return JSONObject.of(
                                "province", userInfoEntity.getProvince(),
                                "score", userInfoEntity.getScore());
                    }
                })
                .keyBy(new KeySelector<JSONObject, String>() {
                    @Override
                    public String getKey(JSONObject jsonObject) throws Exception {
                        return jsonObject.getString("province");
                    }
                })
                // 会话窗口：2分钟窗口数据
                // 表示会话窗口的间隔为 2 分钟。也就是说，窗口会根据事件之间的时间间隔划分。如果两个事件之间的时间差超过了 2 分钟，那么当前的会话窗口就结束，接下来的事件会形成新的窗口。
                .window(ProcessingTimeSessionWindows.withGap(Duration.ofMinutes(2)))
                .apply(new WindowFunction<JSONObject, JSONObject, String, TimeWindow>() {
                    @Override
                    public void apply(String str, TimeWindow timeWindow, Iterable<JSONObject> iterable, Collector<JSONObject> collector) throws Exception {
                        long start = timeWindow.getStart();
                        long end = timeWindow.getEnd();
                        JSONObject json = JSONObject.of("start", DateUtil.format(DateUtil.date(start), "yyyy-MM-dd HH:mm:ss.SSS"), "end", DateUtil.format(DateUtil.date(end), "yyyy-MM-dd HH:mm:ss.SSS"));
                        Double score = 0.0;
                        Long count = 0L;
                        for (JSONObject jsonObject : iterable) {
                            score += jsonObject.getDouble("score");
                            count++;
                        }
                        json.put("province", str);
                        json.put("score", score);
                        json.put("count", count);
                        json.put("dateTime", DateUtil.format(DateUtil.date(), "yyyy-MM-dd HH:mm:ss.SSS"));
                        collector.collect(json);
                    }
                });
        operator.print("sink");

        // 执行流处理作业
        env.execute("Kafka Stream");
    }

}
```

![image-20250116193835688](./assets/image-20250116193835688.png)

### 全局窗口 (Global Window)

全局窗口没有固定的大小，所有事件都被视为一个单独的窗口。在没有明确的时间或数量边界的情况下，所有事件都会累积在一个窗口中，直到触发器或其他条件触发计算。通常，开发者需要自定义触发器来决定何时进行计算。全局窗口适用于特殊的计算需求，例如累积所有事件的某些统计信息，或在特定条件下才进行计算。它的灵活性较大，但也需要精心设计触发策略。

参考：[官方文档](https://nightlies.apache.org/flink/flink-docs-release-1.19/zh/docs/dev/datastream/operators/windows/)

#### 计数器

使用程序的 **CountTrigger** 来实现每 100 个事件触发一次窗口计算

```java
        // 窗口
        SingleOutputStreamOperator<JSONObject> operator = streamSource
                .windowAll(GlobalWindows.create())
                .trigger(CountTrigger.of(100))  // 每 100 个事件触发一次窗口计算
                .apply(new AllWindowFunction<String, JSONObject, GlobalWindow>() {
                    @Override
                    public void apply(GlobalWindow globalWindow, Iterable<String> iterable, Collector<JSONObject> collector) throws Exception {
                        JSONObject json = JSONObject.of("maxTimestamp", globalWindow.maxTimestamp());
                        JSONArray jsonArray = JSONArray.of();
                        for (String string : iterable) {
                            jsonArray.add(JSONObject.parseObject(string));
                        }
                        int size = jsonArray.size();
                        json.put("data^", jsonArray.get(0));
                        json.put("data$", jsonArray.get(size - 1));
                        json.put("size", size);
                        json.put("dateTime", DateUtil.format(DateUtil.date(), "yyyy-MM-dd HH:mm:ss.SSS"));
                        collector.collect(json);
                    }
                });
        operator.print("sink");
```

![image-20250116224208224](./assets/image-20250116224208224.png)

#### 自定义计数器

自定义计数器来实现每 100 个事件触发一次窗口计算

```java
        // 窗口
        SingleOutputStreamOperator<JSONObject> operator = streamSource
                .windowAll(GlobalWindows.create())
                .trigger(new Trigger<String, GlobalWindow>() {
                    @Override
                    public TriggerResult onElement(String element, long timestamp, GlobalWindow window, TriggerContext ctx) throws Exception {
                        // 获取窗口状态
                        ValueState<Long> eventCountState = ctx.getPartitionedState(
                                new ValueStateDescriptor<>("event-count", Long.class));

                        // 获取当前事件计数
                        Long count = eventCountState.value();
                        if (count == null) {
                            count = 0L;
                        }

                        // 更新事件计数
                        count++;

                        // 每当计数器达到阈值 100 时，触发窗口计算并清空窗口数据
                        if (count >= 100) {
                            eventCountState.clear();  // 清空事件计数
                            return TriggerResult.FIRE_AND_PURGE;  // 触发计算并清空窗口数据
                        }

                        // 否则，继续等待更多事件
                        eventCountState.update(count);
                        return TriggerResult.CONTINUE;
                    }

                    @Override
                    public TriggerResult onProcessingTime(long timestamp, GlobalWindow window, TriggerContext ctx) throws Exception {
                        // 基于处理时间触发
                        return TriggerResult.CONTINUE;
                    }

                    @Override
                    public TriggerResult onEventTime(long timestamp, GlobalWindow window, TriggerContext ctx) throws Exception {
                        // 基于事件时间触发
                        return TriggerResult.CONTINUE;
                    }

                    @Override
                    public void clear(GlobalWindow window, TriggerContext ctx) throws Exception {
                        
                    }
                })
                .apply(new AllWindowFunction<String, JSONObject, GlobalWindow>() {
                    @Override
                    public void apply(GlobalWindow globalWindow, Iterable<String> iterable, Collector<JSONObject> collector) throws Exception {
                        JSONObject json = JSONObject.of("maxTimestamp", globalWindow.maxTimestamp());
                        JSONArray jsonArray = JSONArray.of();
                        for (String string : iterable) {
                            jsonArray.add(JSONObject.parseObject(string));
                        }
                        int size = jsonArray.size();
                        json.put("data^", jsonArray.get(0));
                        json.put("data$", jsonArray.get(size - 1));
                        json.put("size", size);
                        json.put("dateTime", DateUtil.format(DateUtil.date(), "yyyy-MM-dd HH:mm:ss.SSS"));
                        collector.collect(json);
                    }
                });
        operator.print("sink");
```

![image-20250116223839322](./assets/image-20250116223839322.png)

#### 自定义时间窗口

自定义时间窗口来实现 每 1分钟 触发一次窗口计算

```java
        // 窗口
        SingleOutputStreamOperator<JSONObject> operator = streamSource
                .windowAll(GlobalWindows.create())
                .trigger(new Trigger<String, GlobalWindow>() {
                    // 定义窗口长度为60秒
                    private static final long WINDOW_LENGTH = 60 * 1000;

                    @Override
                    public TriggerResult onElement(String element, long timestamp, GlobalWindow window, TriggerContext ctx) throws Exception {
                        // 获取当前处理时间
                        long currentProcessingTime = ctx.getCurrentProcessingTime();

                        // 使用 PartitionedState 存储窗口的状态
                        ValueState<Long> lastTimerState = ctx.getPartitionedState(
                                new ValueStateDescriptor<>("lastTimer", Long.class)
                        );

                        // 如果窗口没有注册过定时器
                        if (lastTimerState.value() == null) {
                            // 计算下一次触发时间：当前时间 + 60秒
                            long nextTriggerTime = currentProcessingTime + WINDOW_LENGTH;

                            // 注册处理时间定时器，60秒后触发
                            ctx.registerProcessingTimeTimer(nextTriggerTime);

                            // 更新窗口状态，标记定时器已注册
                            lastTimerState.update(1L);

                            // 打印下一次定时器触发的时间（调试用）
                            System.out.println("下一次定时器触发时间: " + DateUtil.date(nextTriggerTime));
                        }

                        // 继续等待处理时间触发
                        return TriggerResult.CONTINUE;
                    }

                    @Override
                    public TriggerResult onProcessingTime(long timestamp, GlobalWindow window, TriggerContext ctx) throws Exception {
                        // 执行窗口计算的逻辑
                        //System.out.println("处理时间触发，窗口计算中...");

                        // 清除当前窗口的定时器状态
                        ValueState<Long> lastTimerState = ctx.getPartitionedState(
                                new ValueStateDescriptor<>("lastTimer", Long.class)
                        );

                        // 删除窗口状态中的定时器信息
                        lastTimerState.clear();

                        // 在这里执行窗口计算的逻辑
                        return TriggerResult.FIRE_AND_PURGE; // 触发窗口计算并清除状态
                    }

                    @Override
                    public TriggerResult onEventTime(long timestamp, GlobalWindow window, TriggerContext ctx) throws Exception {
                        // 不设置基于事件时间的触发
                        return TriggerResult.CONTINUE;
                    }

                    @Override
                    public void clear(GlobalWindow window, TriggerContext ctx) throws Exception {

                    }

                })
                .apply(new AllWindowFunction<String, JSONObject, GlobalWindow>() {
                    @Override
                    public void apply(GlobalWindow globalWindow, Iterable<String> iterable, Collector<JSONObject> collector) throws Exception {
                        JSONObject json = JSONObject.of("maxTimestamp", globalWindow.maxTimestamp());
                        JSONArray jsonArray = JSONArray.of();
                        for (String string : iterable) {
                            jsonArray.add(JSONObject.parseObject(string));
                        }
                        int size = jsonArray.size();
                        json.put("data^", jsonArray.get(0));
                        json.put("data$", jsonArray.get(size - 1));
                        json.put("size", size);
                        json.put("dateTime", DateUtil.format(DateUtil.date(), "yyyy-MM-dd HH:mm:ss.SSS"));
                        collector.collect(json);
                    }
                });
        operator.print("sink");
```

#### 基于事件时间的时间窗口

基于事件时间，自定义时间窗口来实现 每 1分钟 触发一次窗口计算。

注意数据推积导致事件时间触发机制问题。

```java
package local.ateng.java.DataStream.window.global;

import cn.hutool.core.date.DateUtil;
import cn.hutool.core.date.LocalDateTimeUtil;
import com.alibaba.fastjson2.JSONArray;
import com.alibaba.fastjson2.JSONObject;
import local.ateng.java.entity.UserInfoEntity;
import org.apache.flink.api.common.eventtime.WatermarkStrategy;
import org.apache.flink.api.common.serialization.SimpleStringSchema;
import org.apache.flink.api.common.state.ValueState;
import org.apache.flink.api.common.state.ValueStateDescriptor;
import org.apache.flink.connector.kafka.source.KafkaSource;
import org.apache.flink.connector.kafka.source.enumerator.initializer.OffsetsInitializer;
import org.apache.flink.streaming.api.CheckpointingMode;
import org.apache.flink.streaming.api.datastream.DataStreamSource;
import org.apache.flink.streaming.api.datastream.SingleOutputStreamOperator;
import org.apache.flink.streaming.api.environment.StreamExecutionEnvironment;
import org.apache.flink.streaming.api.functions.windowing.AllWindowFunction;
import org.apache.flink.streaming.api.windowing.assigners.GlobalWindows;
import org.apache.flink.streaming.api.windowing.triggers.Trigger;
import org.apache.flink.streaming.api.windowing.triggers.TriggerResult;
import org.apache.flink.streaming.api.windowing.windows.GlobalWindow;
import org.apache.flink.util.Collector;
import org.apache.kafka.clients.consumer.OffsetResetStrategy;

import java.time.Duration;

/**
 * 可以在普通 DataStream 上定义 Window。 Window 根据某些特征（例如，最近 5 秒内到达的数据）对所有流事件进行分组。
 * DataStream → AllWindowedStream → DataStream
 * https://nightlies.apache.org/flink/flink-docs-release-1.19/zh/docs/dev/datastream/operators/windows/#%e6%bb%91%e5%8a%a8%e7%aa%97%e5%8f%a3sliding-windows
 *
 * 这段代码使用 Flink 从 Kafka 读取消息流
 * 并对消息进行基于事件时间的窗口处理。
 * 窗口大小为 60 秒（1 分钟），每 60 秒触发一次窗口计算，
 * 计算窗口中的数据并输出。
 * 例如，窗口中的第一个元素、最后一个元素、窗口大小以及当前时间都会被输出。
 * @author 孔余
 * @email 2385569970@qq.com
 * @since 2025-01-16
 */
public class EventTimeWindowsAll {

    public static void main(String[] args) throws Exception {
        // 环境准备
        StreamExecutionEnvironment env = StreamExecutionEnvironment.getExecutionEnvironment();
        env.enableCheckpointing(3 * 1000, CheckpointingMode.EXACTLY_ONCE);
        env.setParallelism(1);
        KafkaSource<String> source = KafkaSource.<String>builder()
                .setBootstrapServers("192.168.1.10:9094")
                .setTopics("ateng_flink_json")
                .setGroupId("ateng")
                .setProperty("commit.offsets.on.checkpoint", "true")
                .setProperty("enable.auto.commit", "true")
                .setProperty("auto.commit.interval.ms", "1000")
                .setProperty("partition.discovery.interval.ms", "10000")
                .setStartingOffsets(OffsetsInitializer.committedOffsets(OffsetResetStrategy.EARLIEST))
                .setValueOnlyDeserializer(new SimpleStringSchema())
                .build();

        // 定义水印策略：WatermarkStrategy 可以在 Flink 应用程序中的两处使用，第一种是直接在数据源上使用，第二种是直接在非数据源的操作之后使用。
        // 允许最多 5 秒的事件时间乱序，使用 createTime 字段为事件时间戳（毫秒）
        WatermarkStrategy<String> watermarkStrategy = WatermarkStrategy.<String>forBoundedOutOfOrderness(Duration.ofSeconds(5))
                .withTimestampAssigner(
                        (event, recordTimestamp) -> {
                            // 解析 JSON 格式的事件，并获取事件时间
                            UserInfoEntity user = JSONObject.parseObject(event).toJavaObject(UserInfoEntity.class);
                            long timestamp = LocalDateTimeUtil.toEpochMilli(user.getCreateTime());
                            return timestamp;
                        });

        // 从 Kafka 数据源读取数据，不设置水印策略（处理时间窗口不存在数据乱序问题）
        DataStreamSource<String> streamSource = env.fromSource(source, watermarkStrategy, "Kafka Source");

        // 窗口
        SingleOutputStreamOperator<JSONObject> operator = streamSource
                // 设置水印策略为事件时间
                //.assignTimestampsAndWatermarks(watermarkStrategy)
                .windowAll(GlobalWindows.create())
                .trigger(new Trigger<String, GlobalWindow>() {
                    // 定义窗口长度为60秒
                    private static final long WINDOW_LENGTH = 60 * 1000;

                    @Override
                    public TriggerResult onElement(String element, long timestamp, GlobalWindow window, TriggerContext ctx) throws Exception {
                        // 使用 PartitionedState 存储窗口的状态
                        ValueState<Long> lastTimerState = ctx.getPartitionedState(
                                new ValueStateDescriptor<>("lastTimer", Long.class)
                        );

                        // 如果窗口没有注册过定时器
                        if (lastTimerState.value() == null) {
                            // 计算下一次触发时间：事件时间 + 60秒
                            long nextTriggerTime = timestamp + WINDOW_LENGTH;

                            // 注册事件时间定时器，60秒后触发
                            ctx.registerEventTimeTimer(nextTriggerTime);

                            // 更新窗口状态，标记定时器已注册
                            lastTimerState.update(1L);

                            // 打印下一次定时器触发的时间（调试用）
                            System.out.println("下一次定时器触发时间: " + DateUtil.date(nextTriggerTime));
                        }

                        // 继续等待事件时间触发
                        return TriggerResult.CONTINUE;
                    }

                    @Override
                    public TriggerResult onProcessingTime(long timestamp, GlobalWindow window, TriggerContext ctx) throws Exception {
                        // 不设置基于处理时间的触发
                        return TriggerResult.CONTINUE;
                    }

                    @Override
                    public TriggerResult onEventTime(long timestamp, GlobalWindow window, TriggerContext ctx) throws Exception {
                        // 执行窗口计算的逻辑
                        System.out.println("事件时间触发，窗口计算中...");

                        // 清除当前窗口的定时器状态
                        ValueState<Long> lastTimerState = ctx.getPartitionedState(
                                new ValueStateDescriptor<>("lastTimer", Long.class)
                        );

                        // 删除窗口状态中的定时器信息
                        lastTimerState.clear();

                        // 在这里执行窗口计算的逻辑
                        return TriggerResult.FIRE_AND_PURGE; // 触发窗口计算并清除状态
                    }

                    @Override
                    public void clear(GlobalWindow window, TriggerContext ctx) throws Exception {

                    }

                })
                .apply(new AllWindowFunction<String, JSONObject, GlobalWindow>() {
                    @Override
                    public void apply(GlobalWindow globalWindow, Iterable<String> iterable, Collector<JSONObject> collector) throws Exception {
                        JSONObject json = JSONObject.of("maxTimestamp", globalWindow.maxTimestamp());
                        JSONArray jsonArray = JSONArray.of();
                        for (String string : iterable) {
                            jsonArray.add(JSONObject.parseObject(string));
                        }
                        int size = jsonArray.size();
                        json.put("data^", jsonArray.get(0));
                        json.put("data$", jsonArray.get(size - 1));
                        json.put("size", size);
                        json.put("dateTime", DateUtil.format(DateUtil.date(), "yyyy-MM-dd HH:mm:ss.SSS"));
                        collector.collect(json);
                    }
                });
        operator.print("sink");

        // 执行流处理作业
        env.execute("Kafka Stream");
    }

}
```



### 窗口函数（Window Functions）

#### ReduceFunction 

ReduceFunction 指定两条输入数据如何合并起来产生一条输出数据，输入和输出数据的类型必须相同。 Flink 使用 ReduceFunction 对窗口中的数据进行增量聚合。

```java
package local.ateng.java.DataStream.window.function;

import com.alibaba.fastjson2.JSONObject;
import local.ateng.java.entity.UserInfoEntity;
import org.apache.flink.api.common.eventtime.WatermarkStrategy;
import org.apache.flink.api.common.functions.MapFunction;
import org.apache.flink.api.common.functions.ReduceFunction;
import org.apache.flink.api.common.serialization.SimpleStringSchema;
import org.apache.flink.api.java.functions.KeySelector;
import org.apache.flink.api.java.tuple.Tuple2;
import org.apache.flink.connector.kafka.source.KafkaSource;
import org.apache.flink.connector.kafka.source.enumerator.initializer.OffsetsInitializer;
import org.apache.flink.streaming.api.CheckpointingMode;
import org.apache.flink.streaming.api.datastream.DataStreamSource;
import org.apache.flink.streaming.api.datastream.SingleOutputStreamOperator;
import org.apache.flink.streaming.api.environment.StreamExecutionEnvironment;
import org.apache.flink.streaming.api.windowing.assigners.TumblingProcessingTimeWindows;
import org.apache.kafka.clients.consumer.OffsetResetStrategy;

import java.time.Duration;

/**
 * ReduceFunction #
 * ReduceFunction 指定两条输入数据如何合并起来产生一条输出数据，输入和输出数据的类型必须相同。 Flink 使用 ReduceFunction 对窗口中的数据进行增量聚合。
 * https://nightlies.apache.org/flink/flink-docs-release-1.19/zh/docs/dev/datastream/operators/windows/#reducefunction
 *
 * @author 孔余
 * @email 2385569970@qq.com
 * @since 2025-01-18
 */
public class WindowsReduceFunction {

    public static void main(String[] args) throws Exception {
        // 环境准备
        StreamExecutionEnvironment env = StreamExecutionEnvironment.getExecutionEnvironment();
        env.enableCheckpointing(3 * 1000, CheckpointingMode.EXACTLY_ONCE);
        env.setParallelism(3);
        KafkaSource<String> source = KafkaSource.<String>builder()
                .setBootstrapServers("192.168.1.10:9094")
                .setTopics("ateng_flink_json")
                .setGroupId("ateng")
                .setProperty("commit.offsets.on.checkpoint", "true")
                .setProperty("enable.auto.commit", "true")
                .setProperty("auto.commit.interval.ms", "1000")
                .setProperty("partition.discovery.interval.ms", "10000")
                .setStartingOffsets(OffsetsInitializer.committedOffsets(OffsetResetStrategy.EARLIEST))
                .setValueOnlyDeserializer(new SimpleStringSchema())
                .build();

        // 从 Kafka 数据源读取数据，不设置水印策略（处理时间窗口不存在数据乱序问题）
        DataStreamSource<String> streamSource = env.fromSource(source, WatermarkStrategy.noWatermarks(), "Kafka Source");

        // 窗口
        SingleOutputStreamOperator<Tuple2<String, Double>> operator = streamSource
                .map(new MapFunction<String, Tuple2<String, Double>>() {
                    @Override
                    public Tuple2<String, Double> map(String str) throws Exception {
                        UserInfoEntity userInfoEntity = JSONObject.parseObject(str, UserInfoEntity.class);
                        return Tuple2.of(userInfoEntity.getProvince(), userInfoEntity.getScore());
                    }
                })
                .keyBy(new KeySelector<Tuple2<String, Double>, String>() {
                    @Override
                    public String getKey(Tuple2<String, Double> t) throws Exception {
                        return t.f0;
                    }
                })
                // 1分钟滚动窗口
                .window(TumblingProcessingTimeWindows.of(Duration.ofMinutes(1)))
                .reduce(new ReduceFunction<Tuple2<String, Double>>() {
                    @Override
                    public Tuple2<String, Double> reduce(Tuple2<String, Double> t1, Tuple2<String, Double> t2) throws Exception {
                        return Tuple2.of(t1.f0, t1.f1 + t2.f1);
                    }
                });
        operator.print("sink");

        // 执行流处理作业
        env.execute("Kafka Stream");
    }

}
```

#### AggregateFunction 

ReduceFunction 是 AggregateFunction 的特殊情况。 AggregateFunction 接收三个类型：输入数据的类型(IN)、累加器的类型（ACC）和输出数据的类型（OUT）。 输入数据的类型是输入流的元素类型，AggregateFunction 接口有如下几个方法： 把每一条元素加进累加器、创建初始累加器、合并两个累加器、从累加器中提取输出（OUT 类型）。与 ReduceFunction 相同，Flink 会在输入数据到达窗口时直接进行增量聚合。

```java
package local.ateng.java.DataStream.window.function;

import com.alibaba.fastjson2.JSONObject;
import local.ateng.java.entity.UserInfoEntity;
import org.apache.flink.api.common.eventtime.WatermarkStrategy;
import org.apache.flink.api.common.functions.AggregateFunction;
import org.apache.flink.api.common.functions.MapFunction;
import org.apache.flink.api.common.serialization.SimpleStringSchema;
import org.apache.flink.api.java.functions.KeySelector;
import org.apache.flink.api.java.tuple.Tuple2;
import org.apache.flink.connector.kafka.source.KafkaSource;
import org.apache.flink.connector.kafka.source.enumerator.initializer.OffsetsInitializer;
import org.apache.flink.streaming.api.CheckpointingMode;
import org.apache.flink.streaming.api.datastream.DataStreamSource;
import org.apache.flink.streaming.api.datastream.SingleOutputStreamOperator;
import org.apache.flink.streaming.api.environment.StreamExecutionEnvironment;
import org.apache.flink.streaming.api.windowing.assigners.TumblingProcessingTimeWindows;
import org.apache.kafka.clients.consumer.OffsetResetStrategy;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Duration;

/**
 * AggregateFunction
 * ReduceFunction 是 AggregateFunction 的特殊情况。 AggregateFunction 接收三个类型：输入数据的类型(IN)、累加器的类型（ACC）和输出数据的类型（OUT）。 输入数据的类型是输入流的元素类型，AggregateFunction 接口有如下几个方法： 把每一条元素加进累加器、创建初始累加器、合并两个累加器、从累加器中提取输出（OUT 类型）。
 * 与 ReduceFunction 相同，Flink 会在输入数据到达窗口时直接进行增量聚合。
 * https://nightlies.apache.org/flink/flink-docs-release-1.19/zh/docs/dev/datastream/operators/windows/#aggregatefunction
 *
 * @author 孔余
 * @email 2385569970@qq.com
 * @since 2025-01-18
 */
public class WindowsAggregateFunction {

    public static void main(String[] args) throws Exception {
        // 环境准备
        StreamExecutionEnvironment env = StreamExecutionEnvironment.getExecutionEnvironment();
        env.enableCheckpointing(3 * 1000, CheckpointingMode.EXACTLY_ONCE);
        env.setParallelism(3);
        KafkaSource<String> source = KafkaSource.<String>builder()
                .setBootstrapServers("192.168.1.10:9094")
                .setTopics("ateng_flink_json")
                .setGroupId("ateng")
                .setProperty("commit.offsets.on.checkpoint", "true")
                .setProperty("enable.auto.commit", "true")
                .setProperty("auto.commit.interval.ms", "1000")
                .setProperty("partition.discovery.interval.ms", "10000")
                .setStartingOffsets(OffsetsInitializer.committedOffsets(OffsetResetStrategy.EARLIEST))
                .setValueOnlyDeserializer(new SimpleStringSchema())
                .build();

        // 从 Kafka 数据源读取数据，不设置水印策略（处理时间窗口不存在数据乱序问题）
        DataStreamSource<String> streamSource = env.fromSource(source, WatermarkStrategy.noWatermarks(), "Kafka Source");

        // 窗口
        SingleOutputStreamOperator<Tuple2<String, Double>> operator = streamSource
                .map(new MapFunction<String, Tuple2<String, Double>>() {
                    @Override
                    public Tuple2<String, Double> map(String str) throws Exception {
                        UserInfoEntity userInfoEntity = JSONObject.parseObject(str, UserInfoEntity.class);
                        return Tuple2.of(userInfoEntity.getProvince(), userInfoEntity.getScore());
                    }
                })
                .keyBy(new KeySelector<Tuple2<String, Double>, String>() {
                    @Override
                    public String getKey(Tuple2<String, Double> t) throws Exception {
                        return t.f0;
                    }
                })
                // 1分钟滚动窗口
                .window(TumblingProcessingTimeWindows.of(Duration.ofMinutes(1)))
                .aggregate(new AggregateFunction<Tuple2<String, Double>, Tuple2<String, Double>, Tuple2<String, Double>>() {

                    // 初始化聚合值，返回初始状态
                    @Override
                    public Tuple2<String, Double> createAccumulator() {
                        return Tuple2.of("", 0.0);
                    }

                    // 增量聚合阶段：根据每个输入值更新聚合值
                    @Override
                    public Tuple2<String, Double> add(Tuple2<String, Double> t1, Tuple2<String, Double> t2) {
                        return Tuple2.of(t1.f0, t1.f1 + t2.f1);
                    }

                    // 最终聚合阶段：输出聚合结果
                    @Override
                    public Tuple2<String, Double> getResult(Tuple2<String, Double> t) {
                        return t;
                    }

                    // 合并聚合状态：如果窗口分为多个子窗口，这里合并不同的状态
                    @Override
                    public Tuple2<String, Double> merge(Tuple2<String, Double> t1, Tuple2<String, Double> t2) {
                        return Tuple2.of(t1.f0, t1.f1 + t2.f1);
                    }
                });
        operator.print("sink");

        // 执行流处理作业
        env.execute("Kafka Stream");
    }

}
```

#### ProcessWindowFunction

ProcessWindowFunction 有能获取包含窗口内所有元素的 Iterable， 以及用来获取时间和状态信息的 Context 对象，比其他窗口函数更加灵活。 ProcessWindowFunction 的灵活性是以性能和资源消耗为代价的， 因为窗口中的数据无法被增量聚合，而需要在窗口触发前缓存所有数据。

```java
package local.ateng.java.DataStream.window.function;

import cn.hutool.core.date.DateUtil;
import cn.hutool.core.util.StrUtil;
import com.alibaba.fastjson2.JSONObject;
import local.ateng.java.entity.UserInfoEntity;
import org.apache.flink.api.common.eventtime.WatermarkStrategy;
import org.apache.flink.api.common.functions.MapFunction;
import org.apache.flink.api.common.serialization.SimpleStringSchema;
import org.apache.flink.api.java.functions.KeySelector;
import org.apache.flink.api.java.tuple.Tuple1;
import org.apache.flink.api.java.tuple.Tuple2;
import org.apache.flink.connector.kafka.source.KafkaSource;
import org.apache.flink.connector.kafka.source.enumerator.initializer.OffsetsInitializer;
import org.apache.flink.streaming.api.CheckpointingMode;
import org.apache.flink.streaming.api.datastream.DataStreamSource;
import org.apache.flink.streaming.api.datastream.SingleOutputStreamOperator;
import org.apache.flink.streaming.api.environment.StreamExecutionEnvironment;
import org.apache.flink.streaming.api.functions.windowing.ProcessWindowFunction;
import org.apache.flink.streaming.api.windowing.assigners.TumblingProcessingTimeWindows;
import org.apache.flink.streaming.api.windowing.windows.TimeWindow;
import org.apache.flink.util.Collector;
import org.apache.kafka.clients.consumer.OffsetResetStrategy;

import java.time.Duration;

/**
 * ProcessWindowFunction #
 * ProcessWindowFunction 有能获取包含窗口内所有元素的 Iterable， 以及用来获取时间和状态信息的 Context 对象，比其他窗口函数更加灵活。
 * ProcessWindowFunction 的灵活性是以性能和资源消耗为代价的， 因为窗口中的数据无法被增量聚合，而需要在窗口触发前缓存所有数据。
 * https://nightlies.apache.org/flink/flink-docs-release-1.19/zh/docs/dev/datastream/operators/windows/#processwindowfunction
 *
 * @author 孔余
 * @email 2385569970@qq.com
 * @since 2025-01-18
 */
public class WindowsProcessFunction {

    public static void main(String[] args) throws Exception {
        // 环境准备
        StreamExecutionEnvironment env = StreamExecutionEnvironment.getExecutionEnvironment();
        env.enableCheckpointing(3 * 1000, CheckpointingMode.EXACTLY_ONCE);
        env.setParallelism(3);
        KafkaSource<String> source = KafkaSource.<String>builder()
                .setBootstrapServers("192.168.1.10:9094")
                .setTopics("ateng_flink_json")
                .setGroupId("ateng")
                .setProperty("commit.offsets.on.checkpoint", "true")
                .setProperty("enable.auto.commit", "true")
                .setProperty("auto.commit.interval.ms", "1000")
                .setProperty("partition.discovery.interval.ms", "10000")
                .setStartingOffsets(OffsetsInitializer.committedOffsets(OffsetResetStrategy.EARLIEST))
                .setValueOnlyDeserializer(new SimpleStringSchema())
                .build();

        // 从 Kafka 数据源读取数据，不设置水印策略（处理时间窗口不存在数据乱序问题）
        DataStreamSource<String> streamSource = env.fromSource(source, WatermarkStrategy.noWatermarks(), "Kafka Source");

        // 窗口
        SingleOutputStreamOperator<Tuple1<String>> operator = streamSource
                .map(new MapFunction<String, Tuple2<String, Double>>() {
                    @Override
                    public Tuple2<String, Double> map(String str) throws Exception {
                        UserInfoEntity userInfoEntity = JSONObject.parseObject(str, UserInfoEntity.class);
                        return Tuple2.of(userInfoEntity.getProvince(), userInfoEntity.getScore());
                    }
                })
                .keyBy(new KeySelector<Tuple2<String, Double>, String>() {
                    @Override
                    public String getKey(Tuple2<String, Double> t) throws Exception {
                        return t.f0;
                    }
                })
                // 1分钟滚动窗口
                .window(TumblingProcessingTimeWindows.of(Duration.ofMinutes(1)))
                .process(new ProcessWindowFunction<Tuple2<String, Double>, Tuple1<String>, String, TimeWindow>() {
                    @Override
                    public void process(String key, Context context, Iterable<Tuple2<String, Double>> iterable, Collector<Tuple1<String>> collector) throws Exception {
                        long windowStart = context.window().getStart();  // 窗口的开始时间
                        long windowEnd = context.window().getEnd();      // 窗口的结束时间
                        long currentProcessingTime = context.currentProcessingTime();  // 获取当前处理时间
                        long currentWatermark = context.currentWatermark();  // 获取当前水位线
                        Double score = 0.0;
                        for (Tuple2<String, Double> t : iterable) {
                            score += t.f1;
                        }
                        String string = StrUtil.format("windowStart={},windowEnd={},currentProcessingTime={},currentWatermark={},data={}",
                                DateUtil.date(windowStart), DateUtil.date(windowEnd),
                                DateUtil.date(currentProcessingTime), DateUtil.date(currentWatermark),
                                key + ":" + score);
                        collector.collect(Tuple1.of(string));
                    }
                });
        operator.print("sink");

        // 执行流处理作业
        env.execute("Kafka Stream");
    }

}
```

#### ApplyWindowFunction

在某些可以使用 ProcessWindowFunction 的地方，你也可以使用 WindowFunction。 它是旧版的 ProcessWindowFunction，只能提供更少的环境信息且缺少一些高级的功能，比如 per-window state。 这个接口会在未来被弃用。

```java
package local.ateng.java.DataStream.window.function;

import cn.hutool.core.date.DateUtil;
import cn.hutool.core.util.StrUtil;
import com.alibaba.fastjson2.JSONObject;
import local.ateng.java.entity.UserInfoEntity;
import org.apache.flink.api.common.eventtime.WatermarkStrategy;
import org.apache.flink.api.common.functions.MapFunction;
import org.apache.flink.api.common.serialization.SimpleStringSchema;
import org.apache.flink.api.java.functions.KeySelector;
import org.apache.flink.api.java.tuple.Tuple1;
import org.apache.flink.api.java.tuple.Tuple2;
import org.apache.flink.connector.kafka.source.KafkaSource;
import org.apache.flink.connector.kafka.source.enumerator.initializer.OffsetsInitializer;
import org.apache.flink.streaming.api.CheckpointingMode;
import org.apache.flink.streaming.api.datastream.DataStreamSource;
import org.apache.flink.streaming.api.datastream.SingleOutputStreamOperator;
import org.apache.flink.streaming.api.environment.StreamExecutionEnvironment;
import org.apache.flink.streaming.api.functions.windowing.ProcessWindowFunction;
import org.apache.flink.streaming.api.functions.windowing.WindowFunction;
import org.apache.flink.streaming.api.windowing.assigners.TumblingProcessingTimeWindows;
import org.apache.flink.streaming.api.windowing.windows.TimeWindow;
import org.apache.flink.util.Collector;
import org.apache.kafka.clients.consumer.OffsetResetStrategy;

import java.time.Duration;

/**
 * WindowFunction（已过时）
 * 在某些可以使用 ProcessWindowFunction 的地方，你也可以使用 WindowFunction。
 * 它是旧版的 ProcessWindowFunction，只能提供更少的环境信息且缺少一些高级的功能，比如 per-window state。 这个接口会在未来被弃用。
 *
 * https://nightlies.apache.org/flink/flink-docs-release-1.19/zh/docs/dev/datastream/operators/windows/#windowfunction%e5%b7%b2%e8%bf%87%e6%97%b6
 *
 * @author 孔余
 * @email 2385569970@qq.com
 * @since 2025-01-18
 */
public class WindowsApplyFunction {

    public static void main(String[] args) throws Exception {
        // 环境准备
        StreamExecutionEnvironment env = StreamExecutionEnvironment.getExecutionEnvironment();
        env.enableCheckpointing(3 * 1000, CheckpointingMode.EXACTLY_ONCE);
        env.setParallelism(3);
        KafkaSource<String> source = KafkaSource.<String>builder()
                .setBootstrapServers("192.168.1.10:9094")
                .setTopics("ateng_flink_json")
                .setGroupId("ateng")
                .setProperty("commit.offsets.on.checkpoint", "true")
                .setProperty("enable.auto.commit", "true")
                .setProperty("auto.commit.interval.ms", "1000")
                .setProperty("partition.discovery.interval.ms", "10000")
                .setStartingOffsets(OffsetsInitializer.committedOffsets(OffsetResetStrategy.EARLIEST))
                .setValueOnlyDeserializer(new SimpleStringSchema())
                .build();

        // 从 Kafka 数据源读取数据，不设置水印策略（处理时间窗口不存在数据乱序问题）
        DataStreamSource<String> streamSource = env.fromSource(source, WatermarkStrategy.noWatermarks(), "Kafka Source");

        // 窗口
        SingleOutputStreamOperator<Tuple1<String>> operator = streamSource
                .map(new MapFunction<String, Tuple2<String, Double>>() {
                    @Override
                    public Tuple2<String, Double> map(String str) throws Exception {
                        UserInfoEntity userInfoEntity = JSONObject.parseObject(str, UserInfoEntity.class);
                        return Tuple2.of(userInfoEntity.getProvince(), userInfoEntity.getScore());
                    }
                })
                .keyBy(new KeySelector<Tuple2<String, Double>, String>() {
                    @Override
                    public String getKey(Tuple2<String, Double> t) throws Exception {
                        return t.f0;
                    }
                })
                // 1分钟滚动窗口
                .window(TumblingProcessingTimeWindows.of(Duration.ofMinutes(1)))
                .apply(new WindowFunction<Tuple2<String, Double>, Tuple1<String>, String, TimeWindow>() {
                    @Override
                    public void apply(String key, TimeWindow timeWindow, Iterable<Tuple2<String, Double>> iterable, Collector<Tuple1<String>> collector) throws Exception {
                        long windowStart = timeWindow.getStart();  // 窗口的开始时间
                        long windowEnd = timeWindow.getEnd();      // 窗口的结束时间
                        Double score = 0.0;
                        for (Tuple2<String, Double> t : iterable) {
                            score += t.f1;
                        }
                        String string = StrUtil.format("windowStart={},windowEnd={},data={}",
                                DateUtil.date(windowStart), DateUtil.date(windowEnd),
                                key + ":" + score);
                        collector.collect(Tuple1.of(string));
                    }
                });
        operator.print("sink");

        // 执行流处理作业
        env.execute("Kafka Stream");
    }

}
```

#### ReduceProcessFunction 

**增量聚合的 ProcessWindowFunction**

ProcessWindowFunction 可以与 ReduceFunction 或 AggregateFunction 搭配使用， 使其能够在数据到达窗口的时候进行增量聚合。当窗口关闭时，ProcessWindowFunction 将会得到聚合的结果。 这样它就可以增量聚合窗口的元素并且从 ProcessWindowFunction` 中获得窗口的元数据。

```java
package local.ateng.java.DataStream.window.function;

import cn.hutool.core.date.DateUtil;
import cn.hutool.core.util.StrUtil;
import com.alibaba.fastjson2.JSONObject;
import local.ateng.java.entity.UserInfoEntity;
import org.apache.flink.api.common.eventtime.WatermarkStrategy;
import org.apache.flink.api.common.functions.MapFunction;
import org.apache.flink.api.common.functions.ReduceFunction;
import org.apache.flink.api.common.serialization.SimpleStringSchema;
import org.apache.flink.api.java.functions.KeySelector;
import org.apache.flink.api.java.tuple.Tuple1;
import org.apache.flink.api.java.tuple.Tuple2;
import org.apache.flink.connector.kafka.source.KafkaSource;
import org.apache.flink.connector.kafka.source.enumerator.initializer.OffsetsInitializer;
import org.apache.flink.streaming.api.CheckpointingMode;
import org.apache.flink.streaming.api.datastream.DataStreamSource;
import org.apache.flink.streaming.api.datastream.SingleOutputStreamOperator;
import org.apache.flink.streaming.api.environment.StreamExecutionEnvironment;
import org.apache.flink.streaming.api.functions.windowing.ProcessWindowFunction;
import org.apache.flink.streaming.api.windowing.assigners.TumblingProcessingTimeWindows;
import org.apache.flink.streaming.api.windowing.windows.TimeWindow;
import org.apache.flink.util.Collector;
import org.apache.kafka.clients.consumer.OffsetResetStrategy;

import java.time.Duration;

/**
 * 增量聚合的 ProcessWindowFunction
 * ProcessWindowFunction 可以与 ReduceFunction 或 AggregateFunction 搭配使用， 使其能够在数据到达窗口的时候进行增量聚合。当窗口关闭时，ProcessWindowFunction 将会得到聚合的结果。
 * 这样它就可以增量聚合窗口的元素并且从 ProcessWindowFunction` 中获得窗口的元数据。
 * https://nightlies.apache.org/flink/flink-docs-release-1.19/zh/docs/dev/datastream/operators/windows/#%e5%a2%9e%e9%87%8f%e8%81%9a%e5%90%88%e7%9a%84-processwindowfunction
 *
 * @author 孔余
 * @email 2385569970@qq.com
 * @since 2025-01-18
 */
public class WindowsReduceProcessFunction {

    public static void main(String[] args) throws Exception {
        // 环境准备
        StreamExecutionEnvironment env = StreamExecutionEnvironment.getExecutionEnvironment();
        env.enableCheckpointing(3 * 1000, CheckpointingMode.EXACTLY_ONCE);
        env.setParallelism(3);
        KafkaSource<String> source = KafkaSource.<String>builder()
                .setBootstrapServers("192.168.1.10:9094")
                .setTopics("ateng_flink_json")
                .setGroupId("ateng")
                .setProperty("commit.offsets.on.checkpoint", "true")
                .setProperty("enable.auto.commit", "true")
                .setProperty("auto.commit.interval.ms", "1000")
                .setProperty("partition.discovery.interval.ms", "10000")
                .setStartingOffsets(OffsetsInitializer.committedOffsets(OffsetResetStrategy.EARLIEST))
                .setValueOnlyDeserializer(new SimpleStringSchema())
                .build();

        // 从 Kafka 数据源读取数据，不设置水印策略（处理时间窗口不存在数据乱序问题）
        DataStreamSource<String> streamSource = env.fromSource(source, WatermarkStrategy.noWatermarks(), "Kafka Source");

        // 窗口
        SingleOutputStreamOperator<Tuple1<String>> operator = streamSource
                .map(new MapFunction<String, Tuple2<String, Double>>() {
                    @Override
                    public Tuple2<String, Double> map(String str) throws Exception {
                        UserInfoEntity userInfoEntity = JSONObject.parseObject(str, UserInfoEntity.class);
                        return Tuple2.of(userInfoEntity.getProvince(), userInfoEntity.getScore());
                    }
                })
                .keyBy(new KeySelector<Tuple2<String, Double>, String>() {
                    @Override
                    public String getKey(Tuple2<String, Double> t) throws Exception {
                        return t.f0;
                    }
                })
                // 1分钟滚动窗口
                .window(TumblingProcessingTimeWindows.of(Duration.ofMinutes(1)))
                .reduce(new ReduceFunction<Tuple2<String, Double>>() {
                    @Override
                    public Tuple2<String, Double> reduce(Tuple2<String, Double> t1, Tuple2<String, Double> t2) throws Exception {
                        return Tuple2.of(t1.f0, t1.f1 + t2.f1);
                    }
                }, new ProcessWindowFunction<Tuple2<String, Double>, Tuple1<String>, String, TimeWindow>() {
                    @Override
                    public void process(String key, Context context, Iterable<Tuple2<String, Double>> iterable, Collector<Tuple1<String>> collector) throws Exception {
                        long windowStart = context.window().getStart();  // 窗口的开始时间
                        long windowEnd = context.window().getEnd();      // 窗口的结束时间
                        long currentProcessingTime = context.currentProcessingTime();  // 获取当前处理时间
                        long currentWatermark = context.currentWatermark();  // 获取当前水位线
                        // ReduceFunction 已经聚合的结果通过 ProcessWindowFunction 处理，所有iterable这里只有一个值
                        Tuple2<String, Double> t = iterable.iterator().next();
                        String string = StrUtil.format("windowStart={},windowEnd={},currentProcessingTime={},currentWatermark={},data={},size={}",
                                DateUtil.date(windowStart), DateUtil.date(windowEnd),
                                DateUtil.date(currentProcessingTime), DateUtil.date(currentWatermark),
                                key + ":" + t.f0 + ":" + t.f1, iterable.spliterator().estimateSize());
                        collector.collect(Tuple1.of(string));
                    }
                });
        operator.print("sink");

        // 执行流处理作业
        env.execute("Kafka Stream");
    }

}
```

#### AggregateProcessFunction

**增量聚合的 ProcessWindowFunction**

ProcessWindowFunction 可以与 ReduceFunction 或 AggregateFunction 搭配使用， 使其能够在数据到达窗口的时候进行增量聚合。当窗口关闭时，ProcessWindowFunction 将会得到聚合的结果。 这样它就可以增量聚合窗口的元素并且从 ProcessWindowFunction` 中获得窗口的元数据。

```java
package local.ateng.java.DataStream.window.function;

import cn.hutool.core.date.DateUtil;
import cn.hutool.core.util.StrUtil;
import com.alibaba.fastjson2.JSONObject;
import local.ateng.java.entity.UserInfoEntity;
import org.apache.flink.api.common.eventtime.WatermarkStrategy;
import org.apache.flink.api.common.functions.AggregateFunction;
import org.apache.flink.api.common.functions.MapFunction;
import org.apache.flink.api.common.serialization.SimpleStringSchema;
import org.apache.flink.api.java.functions.KeySelector;
import org.apache.flink.api.java.tuple.Tuple1;
import org.apache.flink.api.java.tuple.Tuple2;
import org.apache.flink.connector.kafka.source.KafkaSource;
import org.apache.flink.connector.kafka.source.enumerator.initializer.OffsetsInitializer;
import org.apache.flink.streaming.api.CheckpointingMode;
import org.apache.flink.streaming.api.datastream.DataStreamSource;
import org.apache.flink.streaming.api.datastream.SingleOutputStreamOperator;
import org.apache.flink.streaming.api.environment.StreamExecutionEnvironment;
import org.apache.flink.streaming.api.functions.windowing.ProcessWindowFunction;
import org.apache.flink.streaming.api.windowing.assigners.TumblingProcessingTimeWindows;
import org.apache.flink.streaming.api.windowing.windows.TimeWindow;
import org.apache.flink.util.Collector;
import org.apache.kafka.clients.consumer.OffsetResetStrategy;

import java.time.Duration;

/**
 * 增量聚合的 ProcessWindowFunction
 * ProcessWindowFunction 可以与 ReduceFunction 或 AggregateFunction 搭配使用， 使其能够在数据到达窗口的时候进行增量聚合。当窗口关闭时，ProcessWindowFunction 将会得到聚合的结果。
 * 这样它就可以增量聚合窗口的元素并且从 ProcessWindowFunction` 中获得窗口的元数据。
 * https://nightlies.apache.org/flink/flink-docs-release-1.19/zh/docs/dev/datastream/operators/windows/#%e5%a2%9e%e9%87%8f%e8%81%9a%e5%90%88%e7%9a%84-processwindowfunction
 *
 * @author 孔余
 * @email 2385569970@qq.com
 * @since 2025-01-18
 */
public class WindowsAggregateProcessFunction {

    public static void main(String[] args) throws Exception {
        // 环境准备
        StreamExecutionEnvironment env = StreamExecutionEnvironment.getExecutionEnvironment();
        env.enableCheckpointing(3 * 1000, CheckpointingMode.EXACTLY_ONCE);
        env.setParallelism(3);
        KafkaSource<String> source = KafkaSource.<String>builder()
                .setBootstrapServers("192.168.1.10:9094")
                .setTopics("ateng_flink_json")
                .setGroupId("ateng")
                .setProperty("commit.offsets.on.checkpoint", "true")
                .setProperty("enable.auto.commit", "true")
                .setProperty("auto.commit.interval.ms", "1000")
                .setProperty("partition.discovery.interval.ms", "10000")
                .setStartingOffsets(OffsetsInitializer.committedOffsets(OffsetResetStrategy.EARLIEST))
                .setValueOnlyDeserializer(new SimpleStringSchema())
                .build();

        // 从 Kafka 数据源读取数据，不设置水印策略（处理时间窗口不存在数据乱序问题）
        DataStreamSource<String> streamSource = env.fromSource(source, WatermarkStrategy.noWatermarks(), "Kafka Source");

        // 窗口
        SingleOutputStreamOperator<Tuple1<String>> operator = streamSource
                .map(new MapFunction<String, Tuple2<String, Double>>() {
                    @Override
                    public Tuple2<String, Double> map(String str) throws Exception {
                        UserInfoEntity userInfoEntity = JSONObject.parseObject(str, UserInfoEntity.class);
                        return Tuple2.of(userInfoEntity.getProvince(), userInfoEntity.getScore());
                    }
                })
                .keyBy(new KeySelector<Tuple2<String, Double>, String>() {
                    @Override
                    public String getKey(Tuple2<String, Double> t) throws Exception {
                        return t.f0;
                    }
                })
                // 1分钟滚动窗口
                .window(TumblingProcessingTimeWindows.of(Duration.ofMinutes(1)))
                .aggregate(new AggregateFunction<Tuple2<String, Double>, Tuple2<String, Double>, Tuple2<String, Double>>() {

                    // 初始化聚合值，返回初始状态
                    @Override
                    public Tuple2<String, Double> createAccumulator() {
                        return Tuple2.of("", 0.0);
                    }

                    // 增量聚合阶段：根据每个输入值更新聚合值
                    @Override
                    public Tuple2<String, Double> add(Tuple2<String, Double> t1, Tuple2<String, Double> t2) {
                        return Tuple2.of(t1.f0, t1.f1 + t2.f1);
                    }

                    // 最终聚合阶段：输出聚合结果
                    @Override
                    public Tuple2<String, Double> getResult(Tuple2<String, Double> t) {
                        return t;
                    }

                    // 合并聚合状态：如果窗口分为多个子窗口，这里合并不同的状态
                    @Override
                    public Tuple2<String, Double> merge(Tuple2<String, Double> t1, Tuple2<String, Double> t2) {
                        return Tuple2.of(t1.f0, t1.f1 + t2.f1);
                    }
                }, new ProcessWindowFunction<Tuple2<String, Double>, Tuple1<String>, String, TimeWindow>() {
                    @Override
                    public void process(String key, Context context, Iterable<Tuple2<String, Double>> iterable, Collector<Tuple1<String>> collector) throws Exception {
                        long windowStart = context.window().getStart();  // 窗口的开始时间
                        long windowEnd = context.window().getEnd();      // 窗口的结束时间
                        long currentProcessingTime = context.currentProcessingTime();  // 获取当前处理时间
                        long currentWatermark = context.currentWatermark();  // 获取当前水位线
                        // ReduceFunction 已经聚合的结果通过 ProcessWindowFunction 处理，所有iterable这里只有一个值
                        Tuple2<String, Double> t = iterable.iterator().next();
                        String string = StrUtil.format("windowStart={},windowEnd={},currentProcessingTime={},currentWatermark={},data={},size={}",
                                DateUtil.date(windowStart), DateUtil.date(windowEnd),
                                DateUtil.date(currentProcessingTime), DateUtil.date(currentWatermark),
                                key + ":" + t.f0 + ":" + t.f1, iterable.spliterator().estimateSize());
                        collector.collect(Tuple1.of(string));
                    }
                });
        operator.print("sink");

        // 执行流处理作业
        env.execute("Kafka Stream");
    }

}
```



## Table API



## SQL



文档：[性能调优](https://nightlies.apache.org/flink/flink-docs-release-1.19/zh/docs/dev/table/tuning/)
