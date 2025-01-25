## Spark on SpringBoot

集成SpringBoot框架的Spark程序，运行在集群上



## 基础配置

首先创建一个SpringBoot项目，以下配置基于该项目做更改。

### 服务版本

- JDK：8
- SpringBoot：2.7.18
- Spark：3.5.4

### 配置pom.xml

```xml
<?xml version="1.0" encoding="UTF-8"?>
<project xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance" xmlns="http://maven.apache.org/POM/4.0.0"
         xsi:schemaLocation="http://maven.apache.org/POM/4.0.0 https://maven.apache.org/xsd/maven-4.0.0.xsd">
    <!-- 项目模型版本 -->
    <modelVersion>4.0.0</modelVersion>

    <!-- 项目坐标 -->
    <groupId>local.ateng.java.spark</groupId>
    <artifactId>spark-cluster</artifactId>
    <version>v1.0</version>
    <name>spark-cluster</name>
    <description>
        集成SpringBoot框架的Spark程序，运行在集群上
    </description>

    <!-- 项目属性 -->
    <properties>
        <!-- 默认主程序 -->
        <start-class>local.ateng.java.spark.BigdataSparkClusterApplication</start-class>
        <java.version>8</java.version>
        <project.build.sourceEncoding>UTF-8</project.build.sourceEncoding>
        <project.reporting.outputEncoding>UTF-8</project.reporting.outputEncoding>
        <spring-boot.version>2.7.18</spring-boot.version>
        <maven-compiler.version>3.12.1</maven-compiler.version>
        <maven-shade.version>3.5.1</maven-shade.version>
        <lombok.version>1.18.36</lombok.version>
        <fastjson2.version>2.0.53</fastjson2.version>
        <hutool.version>5.8.35</hutool.version>
        <spark.version>3.5.4</spark.version>
        <hadoop.version>3.3.6</hadoop.version>
        <mysql.version>8.0.33</mysql.version>
        <postgresql.version>42.7.1</postgresql.version>
    </properties>

    <!-- 项目依赖 -->
    <dependencies>
        <!-- Spring Boot Web Starter: 包含用于构建Web应用程序的Spring Boot依赖项 -->
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-web</artifactId>
            <exclusions>
                <exclusion>
                    <groupId>org.springframework.boot</groupId>
                    <artifactId>spring-boot-starter-tomcat</artifactId>
                </exclusion>
                <exclusion>
                    <groupId>org.springframework.boot</groupId>
                    <artifactId>spring-boot-starter-logging</artifactId>
                </exclusion>
            </exclusions>
        </dependency>
        <!-- 使用log4j2作为日志实现 -->
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-log4j2</artifactId>
        </dependency>

        <!-- Spring Boot Starter Test: 包含用于测试Spring Boot应用程序的依赖项 -->
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-test</artifactId>
            <scope>test</scope>
        </dependency>

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

        <!-- Spark Core -->
        <dependency>
            <groupId>org.apache.spark</groupId>
            <artifactId>spark-core_2.12</artifactId>
            <version>${spark.version}</version>
            <scope>provided</scope>
            <exclusions>
                <exclusion>
                    <artifactId>gson</artifactId>
                    <groupId>com.google.code.gson</groupId>
                </exclusion>
            </exclusions>
        </dependency>
        <!-- Spark SQL -->
        <dependency>
            <groupId>org.apache.spark</groupId>
            <artifactId>spark-sql_2.12</artifactId>
            <version>${spark.version}</version>
            <scope>provided</scope>
        </dependency>
        <!-- Spark Hive -->
        <dependency>
            <groupId>org.apache.spark</groupId>
            <artifactId>spark-hive_2.12</artifactId>
            <version>${spark.version}</version>
            <scope>provided</scope>
            <exclusions>
                <exclusion>
                    <artifactId>gson</artifactId>
                    <groupId>com.google.code.gson</groupId>
                </exclusion>
            </exclusions>
        </dependency>

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

        <!-- 集群模式SpringBoot依赖的Gson和Hadoop、Spark集群依赖有冲突，使用relocation解决冲突 -->
        <dependency>
            <groupId>com.google.code.gson</groupId>
            <artifactId>gson</artifactId>
            <version>2.9.1</version>
        </dependency>
    </dependencies>

    <!-- Spring Boot 依赖管理 -->
    <dependencyManagement>
        <dependencies>
            <!-- SpringBoot 依赖管理 -->
            <dependency>
                <groupId>org.springframework.boot</groupId>
                <artifactId>spring-boot-dependencies</artifactId>
                <version>${spring-boot.version}</version>
                <type>pom</type>
                <scope>import</scope>
            </dependency>
        </dependencies>
    </dependencyManagement>

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

            <!-- Spring Boot Maven 插件 -->
            <!--<plugin>
                <groupId>org.springframework.boot</groupId>
                <artifactId>spring-boot-maven-plugin</artifactId>
                <version>${spring-boot.version}</version>
                <executions>
                    <execution>
                        <id>repackage</id>
                        <goals>
                            <goal>repackage</goal>
                        </goals>
                    </execution>
                </executions>
            </plugin>-->

            <!-- Maven Shade 打包插件 -->
            <!-- https://maven.apache.org/plugins/maven-shade-plugin/shade-mojo.html -->
            <plugin>
                <groupId>org.apache.maven.plugins</groupId>
                <artifactId>maven-shade-plugin</artifactId>
                <version>${maven-shade.version}</version>
                <configuration>
                    <!-- 禁用生成 dependency-reduced-pom.xml 文件 -->
                    <createDependencyReducedPom>false</createDependencyReducedPom>
                    <!-- 附加shaded工件时使用的分类器的名称 -->
                    <shadedClassifierName>shaded</shadedClassifierName>
                    <!--
                    重新定位（relocate）类的包名，以防止与其他库中的同名类发生冲突
                    解决和Spark的gson冲突问题
                    -->
                    <relocations>
                        <relocation>
                            <pattern>com.google.gson</pattern>
                            <shadedPattern>shaded.com.google.gson</shadedPattern>
                        </relocation>
                    </relocations>
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
                                <exclude>**/Log4j2Plugins.dat</exclude>
                                <!--<exclude>META-INF/*.txt</exclude>
                                <exclude>META-INF/NOTICE</exclude>
                                <exclude>META-INF/LICENSE</exclude>
                                <exclude>META-INF/services/java.sql.Driver</exclude>
                                -->
                                <!-- 排除resources下的xml配置文件 -->
                                <!--<exclude>*.xml</exclude>-->
                            </excludes>
                        </filter>
                    </filters>
                    <artifactSet>
                        <!-- 排除依赖项 -->
                        <excludes>
                            <!--<exclude>org.apache.logging.log4j:*</exclude>
                            <exclude>org.slf4j:*</exclude>-->
                        </excludes>
                    </artifactSet>
                </configuration>
                <dependencies>
                    <dependency>
                        <groupId>org.springframework.boot</groupId>
                        <artifactId>spring-boot-maven-plugin</artifactId>
                        <version>${spring-boot.version}</version>
                    </dependency>
                </dependencies>
                <executions>
                    <execution>
                        <phase>package</phase>
                        <goals>
                            <goal>shade</goal>
                        </goals>
                        <configuration>
                            <transformers>
                                <!-- 合并 Log4j2Plugins.dat 文件 -->
                                <transformer implementation="org.apache.maven.plugins.shade.resource.AppendingTransformer">
                                    <resource>Log4j2Plugins.dat</resource>
                                </transformer>
                                <transformer implementation="org.apache.maven.plugins.shade.resource.AppendingTransformer">
                                    <resource>META-INF/additional-spring-configuration-metadata.json</resource>
                                </transformer>
                                <transformer implementation="org.apache.maven.plugins.shade.resource.AppendingTransformer">
                                    <resource>META-INF/spring-configuration-metadata.json</resource>
                                </transformer>
                                <transformer implementation="org.apache.maven.plugins.shade.resource.AppendingTransformer">
                                    <resource>META-INF/spring.handlers</resource>
                                </transformer>
                                <transformer implementation="org.apache.maven.plugins.shade.resource.AppendingTransformer">
                                    <resource>META-INF/spring.schemas</resource>
                                </transformer>
                                <transformer implementation="org.apache.maven.plugins.shade.resource.AppendingTransformer">
                                    <resource>META-INF/spring.tooling</resource>
                                </transformer>
                                <transformer implementation="org.apache.maven.plugins.shade.resource.AppendingTransformer">
                                    <resource>META-INF/spring/org.springframework.boot.autoconfigure.AutoConfiguration.imports</resource>
                                </transformer>
                                <transformer implementation="org.apache.maven.plugins.shade.resource.AppendingTransformer">
                                    <resource>
                                        META-INF/spring/org.springframework.boot.actuate.autoconfigure.web.ManagementContextConfiguration.imports
                                    </resource>
                                </transformer>
                                <transformer implementation="org.springframework.boot.maven.PropertiesMergingResourceTransformer">
                                    <resource>META-INF/spring.factories</resource>
                                </transformer>
                                <transformer implementation="org.apache.maven.plugins.shade.resource.ServicesResourceTransformer"/>
                                <transformer implementation="org.apache.maven.plugins.shade.resource.ManifestResourceTransformer">
                                    <!-- 指定默认主程序 -->
                                    <mainClass>${start-class}</mainClass>
                                </transformer>
                            </transformers>
                        </configuration>
                    </execution>
                </executions>
            </plugin>
        </plugins>
        <resources>
            <!-- 第一个资源配置块 -->
            <resource>
                <directory>src/main/resources</directory>
                <filtering>false</filtering>
            </resource>
            <!-- 第二个资源配置块 -->
            <resource>
                <directory>src/main/resources</directory>
                <includes>
                    <include>application*</include>
                    <include>bootstrap*.yml</include>
                    <include>common*</include>
                    <include>banner*</include>
                </includes>
                <filtering>true</filtering>
            </resource>
        </resources>
    </build>

</project>
```

### 拷贝Hive配置文件

将Hive的配置文件 `hive-site.xml` 拷贝到 `resources` 目录下。

### 编辑应用配置文件

编辑 `application.yml` 配置文件

```yaml
server:
  port: 16012
  servlet:
    context-path: /
spring:
  main:
    web-application-type: none
  application:
    name: ${project.artifactId}
```

### 创建日志配置文件

在 `resources` 目录下创建 `log4j2.xml ` 日志配置文件

```xml
<?xml version="1.0" encoding="UTF-8"?>
<Configuration status="WARN">
    <Appenders>
        <Console name="Console" target="SYSTEM_OUT">
            <PatternLayout pattern="%d{ISO8601} [%t] %-5level %logger{36} - %msg%n" />
        </Console>
    </Appenders>
    <Loggers>
        <!-- 设置 org.apache.kafka 包日志级别为 WARN -->
        <Logger name="org.apache.kafka" level="warn" additivity="false">
            <AppenderRef ref="Console" />
        </Logger>
        <!-- 设置 org.apache.spark 包日志级别为 WARN -->
        <Logger name="org.apache.spark" level="warn" additivity="false">
            <AppenderRef ref="Console" />
        </Logger>
        <!-- 设置 org.sparkproject 包日志级别为 WARN -->
        <Logger name="org.sparkproject" level="warn" additivity="false">
            <AppenderRef ref="Console" />
        </Logger>
        <!-- 根日志级别设置为 INFO -->
        <Root level="info">
            <AppenderRef ref="Console" />
        </Root>
    </Loggers>
</Configuration>
```



## 创建Runner

通过该Runner运行指定的类和方法

```java
package local.ateng.java.spark.runner;

import cn.hutool.extra.spring.SpringUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.context.ApplicationContext;
import org.springframework.stereotype.Component;
import org.springframework.util.ObjectUtils;

import java.lang.reflect.Method;
import java.util.List;

/**
 * 运行Spark任务
 *
 * @author 孔余
 * @email 2385569970@qq.com
 * @since 2025-01-25
 */
@Component
@RequiredArgsConstructor(onConstructor = @__(@Autowired))
public class MySparkJobRunner implements ApplicationRunner {
    private final ApplicationContext applicationContext;

    @Override
    public void run(ApplicationArguments args) throws Exception {
        // 获取需要运行的任务名称
        List<String> classArgs = args.getOptionValues("class");
        List<String> methodArgs = args.getOptionValues("method");
        if (ObjectUtils.isEmpty(classArgs) || ObjectUtils.isEmpty(methodArgs)) {
            System.out.println("请提供参数：--class=xxx --method=xxx");
            System.out.println("例如：--class=local.ateng.java.spark.sql.SQLCount --method=run");
            int exitCode = SpringApplication.exit(SpringUtil.getApplicationContext(), () -> 1);
            System.exit(exitCode);
        }
        String className = classArgs.get(0);
        String methodName = methodArgs.get(0);
        System.out.println("运行" + className + "的" + methodName + "方法");
        // 使用反射机制调用指定的类和方法
        Class<?> clazz = Class.forName(className);
        Object instance = applicationContext.getBean(clazz);
        Method method = clazz.getMethod(methodName);
        method.invoke(instance);
    }
}
```



## 编辑Spark代码

### 创建实体类

```java
package local.ateng.java.spark.entity;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 用户实体类
 *
 * @author 孔余
 * @email 2385569970@qq.com
 * @since 2025-01-24
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class MyUser implements Serializable {
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
    private BigDecimal score;

    /**
     * 用户生日
     * 注意：这里使用Date类型，表示用户的生日。
     */
    private LocalDate birthday;

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

### 创建Spark环境配置

```java
package local.ateng.java.spark.config;

import org.apache.spark.SparkConf;
import org.apache.spark.api.java.JavaSparkContext;
import org.apache.spark.sql.SparkSession;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Spark Config
 *
 * @author 孔余
 * @email 2385569970@qq.com
 * @since 2025-01-25
 */
@Configuration
public class MySparkConfig {

    /**
     * 创建Spark配置
     *
     * @return Spark上下文
     */
    @Bean
    public SparkConf sparkConf() {
        // 创建Spark配置
        SparkConf conf = new SparkConf();
        // 设置应用名称
        conf.setAppName("SparkOnSpring");
        // 开启 Web UI
        conf.set("spark.ui.enabled", "true");
        // 指定hive仓库中的默认位置
        //conf.set("spark.sql.warehouse.dir", "hdfs://server01:8020/hive/warehouse");
        // 设置运行环境
        String masterValue = conf.get("spark.master", "local[*]");
        conf.setMaster(masterValue);
        return conf;
    }

    /**
     * 创建Spark上下文
     *
     * @param conf
     * @return JavaSparkContext
     */
    @Bean(destroyMethod = "stop")
    public JavaSparkContext sparkContext(SparkConf conf) {
        return new JavaSparkContext(conf);
    }

    /**
     * 创建Spark Session
     *
     * @param conf
     * @return SparkSession
     */
    @Bean(destroyMethod = "stop")
    public SparkSession sparkSession(SparkConf conf) {
        return SparkSession
                .builder()
                .config(conf)
                .enableHiveSupport()
                .getOrCreate();
    }

}
```

### 创建RDD任务

```java
package local.ateng.java.spark.rdd;

import cn.hutool.extra.spring.SpringUtil;
import lombok.extern.slf4j.Slf4j;
import org.apache.spark.api.java.JavaRDD;
import org.apache.spark.api.java.JavaSparkContext;
import org.springframework.stereotype.Component;

/**
 * 读取HDFS文件并计算行数
 *
 * @author 孔余
 * @email 2385569970@qq.com
 * @since 2025-01-24
 */
@Component
@Slf4j
public class RDDCount {

    public void run() {
        // 获取环境
        JavaSparkContext sc = SpringUtil.getBean("sparkContext", JavaSparkContext.class);

        // 将数据并行化为RDD
        JavaRDD<String> textFileRDD = sc.textFile("hdfs://server01:8020/data/my_user.csv");

        // 使用count操作，获取RDD中行数。
        long count = textFileRDD.count();

        // 打印结果
        System.out.println("RDD计算结果: " + count);

    }
}
```



### 创建SQL任务

```java
package local.ateng.java.spark.sql;

import cn.hutool.extra.spring.SpringUtil;
import lombok.extern.slf4j.Slf4j;
import org.apache.spark.sql.Dataset;
import org.apache.spark.sql.Row;
import org.apache.spark.sql.SparkSession;
import org.springframework.stereotype.Component;

/**
 * 查询数据数量
 *
 * @author 孔余
 * @email 2385569970@qq.com
 * @since 2025-01-24
 */
@Component
@Slf4j
public class SQLCount {

    public void run() {
        // 获取环境
        SparkSession spark = SpringUtil.getBean("sparkSession", SparkSession.class);

        // 执行SQL查询
        Dataset<Row> ds = spark.sql("SELECT COUNT(*) FROM my_user");

        // 显示查询结果
        System.out.println("SQL计算结果：");
        ds.show();

    }
}
```



## 运行任务

### IDEA运行

配置参数，运行指定的类和方法

```java
--class=local.ateng.java.spark.sql.SQLCount --method=run
```

![image-20250125170613567](./assets/image-20250125170613567.png)

### 打包Jar

通过Maven将代码打包成Jar，如下图所示

![image-20250125171000086](./assets/image-20250125171000086.png)

### Spark Standalone

部署集群参考：[安装Spark集群](https://kongyu666.github.io/ops/#/work/bigdata/05-spark/cluster/)

将Jar包运行在Spark Standalone集群上，这里以运行读取Hive数据库数据为例。

**客户端运行**

`client` 模式适用于需要在本地客户端调试或开发的场景

```
spark-submit --master spark://server01:7077 \
    --deploy-mode client \
    --total-executor-cores 3 \
    spark-cluster-v1.0.jar \
    --class=local.ateng.java.spark.sql.SQLCount \
    --method=run
```

- --total-executor-cores: 指定 Spark 应用总共使用的 CPU 核心

验证是否执行成功，查看数据的写入时间

```
hadoop fs -ls /data/spark/output
```

**服务端运行**

`cluster` 模式适用于生产环境，确保集群中的所有资源得到合理利用，并且 Driver 程序不依赖于本地环境

上传Jar到HDFS

```
hadoop fs -mkdir -p /data/spark
hadoop fs -put spark-cluster-v1.0.jar /data/spark
```

在集群运行任务

```
spark-submit --master spark://server01:7077 \
    --deploy-mode cluster \
    --total-executor-cores 3 \
    hdfs://server01:8020/data/spark/spark-cluster-v1.0.jar \
    --class=local.ateng.java.spark.sql.SQLCount \
    --method=run
```

- --total-executor-cores: 指定 Spark 应用总共使用的 CPU 核心

验证是否执行成功，查看数据的写入时间

```
hadoop fs -ls /data/spark/output
```



### YARN

部署集群参考：[安装配置Spark On YARN](https://kongyu666.github.io/ops/#/work/bigdata/05-flink/yarn/)

将Jar包运行在Spark Standalone集群上，这里以运行读取Hive数据库数据为例。

**客户端运行**

`client` 模式适用于需要在本地客户端调试或开发的场景

```
spark-submit --master yarn \
    --deploy-mode client \
    --num-executors 3 \
    spark-cluster-v1.0.jar \
    --class=local.ateng.java.spark.sql.SQLCount \
    --method=run
```

- --num-executors: 指定 Executor 数量

验证是否执行成功，查看数据的写入时间

```
hadoop fs -ls /data/spark/output
```

**服务端运行**

`cluster` 模式适用于生产环境，确保集群中的所有资源得到合理利用，并且 Driver 程序不依赖于本地环境

```
spark-submit --master yarn \
    --deploy-mode cluster \
    --num-executors 3 \
    --name "统计Hive数据库数据" \
    spark-cluster-v1.0.jar \
    --class=local.ateng.java.spark.sql.SQLCount \
    --method=run
```

- --num-executors: 指定 Executor 数量

查看日志

```
yarn logs -applicationId application_1737640156298_0008 -log_files stdout
```

验证是否执行成功，查看数据的写入时间

```
hadoop fs -ls /data/spark/output
```



### Java

将Spark相关的依赖的作用域都设置为compile（默认），最后打包

```bash
java -jar \
    -server -Xms128m -Xmx1024m \
    spark-cluster-v1.0.jar \
    --class=local.ateng.java.spark.sql.SQLCount \
    --method=run
```

![image-20250125173138945](./assets/image-20250125173138945.png)

### Kubernetes

参考：[Spark Operator使用文档](https://kongyu666.github.io/ops/#/work/bigdata/05-spark/kubernetes-operator/examples/)

以下Spark应用配置参考

```yaml
apiVersion: "sparkoperator.k8s.io/v1beta2"
kind: SparkApplication
metadata:
  name: spark-spring-myapp
  namespace: ateng-spark
spec:
  type: Java
  mode: cluster
  image: "registry.lingo.local/service/spark:3.5.4"
  imagePullPolicy: Always
  proxyUser: admin
  #mainClass: org.apache.spark.examples.SparkPi
  arguments:
    - "--class=local.ateng.java.spark.sql.SQLCount"
    - "--method=run"
  mainApplicationFile: "http://192.168.1.12:9000/test/spark/spark-cluster-v1.0.jar"
  sparkVersion: "3.5.4"
  sparkUIOptions:
    serviceType: NodePort
  dynamicAllocation:
    enabled: true
    initialExecutors: 1
    maxExecutors: 5
    minExecutors: 1
  restartPolicy:
    type: Never
  driver:
    cores: 2
    coreLimit: "2"
    coreRequest: "1"
    memory: "2g"
    env:
      - name: TZ
        value: Asia/Shanghai      
    labels:
      version: 3.5.4
      app.kubernetes.io/name: spark-spring-myapp
      app.kubernetes.io/component: driver
    hostAliases:
      - ip: "192.168.1.18"
        hostnames:
          - "server01"
    serviceAccount: spark-operator-spark
  executor:
    cores: 2
    coreLimit: "2"
    coreRequest: "1"
    memory: "4g"
    instances: 1
    env:
      - name: TZ
        value: Asia/Shanghai      
    labels:
      version: 3.5.4
      app.kubernetes.io/name: spark-spring-myapp
      app.kubernetes.io/component: executor
    hostAliases:
      - ip: "192.168.1.18"
        hostnames:
          - "server01"
```

