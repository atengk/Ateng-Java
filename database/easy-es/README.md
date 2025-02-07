# Easy-Es

Easy-Es（简称EE）是一款基于ElasticSearch(简称Es)官方提供的RestHighLevelClient打造的ORM开发框架，在 RestHighLevelClient 的基础上,只做增强不做改变，为简化开发、提高效率而生,您如果有用过Mybatis-Plus(简称MP),那么您基本可以零学习成本直接上手EE,EE是MP的Es平替版,在有些方面甚至比MP更简单,同时也融入了更多Es独有的功能,助力您快速实现各种场景的开发.

参考：[官方文档](https://www.easy-es.cn/)



## 基础配置

### 依赖配置

添加依赖

```xml
<easy-es.version>2.1.0</easy-es.version>
<!-- Easy-Es 依赖 -->
<dependency>
    <groupId>org.dromara.easy-es</groupId>
    <artifactId>easy-es-boot-starter</artifactId>
    <version>${easy-es.version}</version>
</dependency>
```

完整的pom.xml文件如下

```xml
<?xml version="1.0" encoding="UTF-8"?>
<project xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance" xmlns="http://maven.apache.org/POM/4.0.0"
         xsi:schemaLocation="http://maven.apache.org/POM/4.0.0 https://maven.apache.org/xsd/maven-4.0.0.xsd">
    <!-- 项目模型版本 -->
    <modelVersion>4.0.0</modelVersion>

    <!-- 项目坐标 -->
    <groupId>local.ateng.java</groupId>
    <artifactId>easy-es</artifactId>
    <version>v1.0</version>
    <name>easy-es</name>
    <description>
        Easy-Es（简称EE）是一款基于ElasticSearch(简称Es)官方提供的RestHighLevelClient打造的ORM开发框架，
        在 RestHighLevelClient 的基础上,只做增强不做改变，为简化开发、提高效率而生,您如果有用过Mybatis-Plus(简称MP),
        那么您基本可以零学习成本直接上手EE,EE是MP的Es平替版,在有些方面甚至比MP更简单,同时也融入了更多Es独有的功能,助力您快速实现各种场景的开发.
    </description>

    <!-- 项目属性 -->
    <properties>
        <java.version>21</java.version>
        <project.build.sourceEncoding>UTF-8</project.build.sourceEncoding>
        <project.reporting.outputEncoding>UTF-8</project.reporting.outputEncoding>
        <spring-boot.version>3.4.1</spring-boot.version>
        <maven-compiler.version>3.12.1</maven-compiler.version>
        <lombok.version>1.18.36</lombok.version>
        <fastjson2.version>2.0.53</fastjson2.version>
        <hutool.version>5.8.35</hutool.version>
        <easy-es.version>2.1.0</easy-es.version>
    </properties>

    <!-- 项目依赖 -->
    <dependencies>
        <!-- Spring Boot Web Starter: 包含用于构建Web应用程序的Spring Boot依赖项 -->
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-web</artifactId>
        </dependency>

        <!-- Spring Boot Starter Test: 包含用于测试Spring Boot应用程序的依赖项 -->
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-test</artifactId>
            <scope>test</scope>
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
        <!-- 在 Spring 中集成 Fastjson2 -->
        <!-- https://github.com/alibaba/fastjson2/blob/main/docs/spring_support_cn.md -->
        <dependency>
            <groupId>com.alibaba.fastjson2</groupId>
            <artifactId>fastjson2-extension-spring6</artifactId>
            <version>${fastjson2.version}</version>
        </dependency>

        <!-- Hutool: Java工具库，提供了许多实用的工具方法 -->
        <dependency>
            <groupId>cn.hutool</groupId>
            <artifactId>hutool-all</artifactId>
            <version>${hutool.version}</version>
        </dependency>

        <!-- JavaFaker: 用于生成虚假数据的Java库 -->
        <dependency>
            <groupId>com.github.javafaker</groupId>
            <artifactId>javafaker</artifactId>
            <version>1.0.2</version>
        </dependency>

        <!-- Easy-Es 依赖 -->
        <dependency>
            <groupId>org.dromara.easy-es</groupId>
            <artifactId>easy-es-boot-starter</artifactId>
            <version>${easy-es.version}</version>
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
                    <!-- 编译参数 -->
                    <compilerArgs>
                        <!-- 启用Java 8参数名称保留功能 -->
                        <arg>-parameters</arg>
                    </compilerArgs>
                </configuration>
            </plugin>

            <!-- Spring Boot Maven 插件 -->
            <plugin>
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

### 编辑配置文件

编辑`application.yml`配置文件，详情参考官网的[配置文档](https://www.easy-es.cn/pages/eddebb/)

```yaml
server:
  port: 15006
  servlet:
    context-path: /
spring:
  main:
    web-application-type: servlet
  application:
    name: ${project.artifactId}
---
# Easy-Es 的相关配置
easy-es:
  enable: true
  address: dev.es.lingo.local:80
  username: elastic
  password: Admin@123
  schema: "http"
  banner: false
  global-config:
    process_index_mode: manual #smoothly:平滑模式, not_smoothly:非平滑模式, manual:手动模式(默认)
    async-process-index-blocking: true # 异步处理索引是否阻塞主线程 默认阻塞
    distributed: false # 项目是否分布式环境部署,默认为true, 如果是单机运行可填false,将不加分布式锁,效率更高.
    reindexTimeOutHours: 72 # 重建索引超时时间 单位小时,默认72H 根据迁移索引数据量大小灵活指定
    db-config:
      map-underscore-to-camel-case: true # 是否开启下划线转驼峰 默认为false
```

### 创建实体类

注解详情参考[官网文档](https://www.easy-es.cn/pages/ac41f0/)

```java
package local.ateng.java.es.entity;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.dromara.easyes.annotation.HighLight;
import org.dromara.easyes.annotation.IndexField;
import org.dromara.easyes.annotation.IndexName;
import org.dromara.easyes.annotation.Settings;
import org.dromara.easyes.annotation.rely.Analyzer;
import org.dromara.easyes.annotation.rely.FieldType;

import java.io.Serial;
import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Date;


@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Settings(
        shardsNum = 1,
        replicasNum = 0,
        maxResultWindow = 10000
)
@IndexName(value = "my_user")
public class MyUser implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * 主键id
     */
    private String id;

    /**
     * 名称
     */
    private String name;

    /**
     * 年龄
     */
    private Integer age;

    /**
     * 手机号码
     */
    private String phoneNumber;

    /**
     * 邮箱
     */
    private String email;

    /**
     * IP地址
     */
    @IndexField(fieldType = FieldType.IP)
    private String ipaddress;

    /**
     * 公司名称
     */
    private String company;

    /**
     * 分数
     */
    private BigDecimal score;

    /**
     * 生日
     */
    @IndexField(fieldType = FieldType.DATE, dateFormat = "yyyy-MM-dd")
    private Date birthday;

    /**
     * 所在省份
     */
    private String province;

    /**
     * 所在城市
     */
    private String city;

    /**
     * 地址
     */
    private String address;

    /**
     * 经纬度(lat,lng)
     */
    @IndexField(fieldType = FieldType.GEO_POINT)
    private String location;

    /**
     * 一段文字
     */
    @IndexField(fieldType = FieldType.TEXT, analyzer = Analyzer.IK_SMART, searchAnalyzer = Analyzer.IK_MAX_WORD)
    private String paragraph;

    /**
     * 记录创建时间，默认当前时间
     */
    @IndexField(fieldType = FieldType.DATE, dateFormat = "yyyy-MM-dd HH:mm:ss.SSS")
    private LocalDateTime createTime;

}
```

### 创建Mapper

```java
package local.ateng.java.es.mapper;

import local.ateng.java.es.entity.MyUser;
import org.dromara.easyes.core.kernel.BaseEsMapper;

public interface MyUserMapper extends BaseEsMapper<MyUser> {
}
```

### 配置扫描 Mapper 文件

```java
package local.ateng.java.es;

import org.dromara.easyes.spring.annotation.EsMapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
@EsMapperScan("local.ateng.java.es.**.mapper")
public class DBEasyEsApplication {

    public static void main(String[] args) {
        SpringApplication.run(DBEasyEsApplication.class, args);
    }

}
```

### 创建数据生成器

```java
package local.ateng.java.es.init;

import com.github.javafaker.Faker;
import local.ateng.java.es.entity.MyUser;
import lombok.Getter;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * 初始化数据
 *
 * @author 孔余
 * @email 2385569970@qq.com
 * @since 2025-01-09
 */
@Getter
public class InitData {
    List<MyUser> list;

    public InitData() {
        //生成测试数据
        // 创建一个Java Faker实例，指定Locale为中文
        Faker faker = new Faker(new Locale("zh-CN"));
        List<MyUser> userList = new ArrayList();
        for (int i = 1; i <= 1000; i++) {
            MyUser user = new MyUser();
            user.setName(faker.name().fullName());
            user.setAge(faker.number().numberBetween(0, 100));
            user.setPhoneNumber(faker.phoneNumber().cellPhone());
            user.setEmail(faker.internet().emailAddress());
            user.setIpaddress(faker.internet().ipV4Address());
            user.setCompany(faker.company().name());
            user.setScore(BigDecimal.valueOf(faker.number().randomDouble(2, 0, 100)));
            user.setBirthday(faker.date().birthday());
            user.setProvince(faker.address().state());
            user.setCity(faker.address().cityName());
            user.setAddress(faker.address().fullAddress());
            user.setLocation(faker.address().latitude() + "," + faker.address().longitude());
            user.setParagraph(faker.lorem().paragraph());
            user.setCreateTime(LocalDateTime.now());
            userList.add(user);
        }
        list = userList;
    }

}
```



## 索引管理

参考官方文档：

- [索引托管模式](https://www.easy-es.cn/pages/cc15ba/)
- [索引CRUD](https://www.easy-es.cn/pages/e8b9ad/)

### 创建测试类

```java
package local.ateng.java.es;

import local.ateng.java.es.entity.MyUser;
import local.ateng.java.es.mapper.MyUserMapper;
import lombok.RequiredArgsConstructor;
import org.dromara.easyes.annotation.rely.FieldType;
import org.dromara.easyes.core.kernel.EsWrappers;
import org.elasticsearch.client.indices.GetIndexResponse;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest
@RequiredArgsConstructor(onConstructor = @__(@Autowired))
public class EasyEsIndexTests {
    private final MyUserMapper myUserMapper;
}
```

### 是否存在

```java
    @Test
    public void exists() {
        // 是否存在索引
        Boolean result = myUserMapper.existsIndex("my_user");
        System.out.println(result);
    }
```

### 创建索引

```java
    @Test
    public void create() {
        // 根据实体及自定义注解一键创建索引
        Boolean result = myUserMapper.createIndex();
        System.out.println(result);
    }
```

### 获取索引

```java
    @Test
    public void getIndex() {
        // 获取索引信息
        GetIndexResponse index = myUserMapper.getIndex();
        System.out.println(index.getSettings());
        System.out.println(index.getMappings().get("my_user").getSourceAsMap());
    }
```

### 更新索引

```java
    @Test
    public void update() {
        // 更新索引
        Boolean result = EsWrappers.lambdaChainIndex(myUserMapper)
                .indexName("my_user")
                .mapping(MyUser::getAddress, FieldType.KEYWORD_TEXT)
                .updateIndex();
        System.out.println(result);
    }
```

### 删除索引

```java
    @Test
    public void delete() {
        // 删除索引
        Boolean result = myUserMapper.deleteIndex("my_user");
        System.out.println(result);
    }
```



## 数据管理

参考官方文档：

- [数据CRUD](https://www.easy-es.cn/pages/f3ee10/)

### 创建测试类

```java
package local.ateng.java.es;

import local.ateng.java.es.entity.MyUser;
import local.ateng.java.es.init.InitData;
import local.ateng.java.es.mapper.MyUserMapper;
import lombok.RequiredArgsConstructor;
import org.dromara.easyes.core.biz.EsPageInfo;
import org.dromara.easyes.core.biz.SAPageInfo;
import org.dromara.easyes.core.conditions.select.LambdaEsQueryChainWrapper;
import org.dromara.easyes.core.kernel.EsWrappers;
import org.elasticsearch.common.geo.GeoPoint;
import org.elasticsearch.common.unit.DistanceUnit;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.List;

@SpringBootTest
@RequiredArgsConstructor(onConstructor = @__(@Autowired))
public class EasyEsDataTests {
    private final MyUserMapper myUserMapper;
}
```

### 批量插入数据

```java
    @Test
    public void insertBatch() {
        // 批量插入数据
        List<MyUser> list = new InitData().getList();
        Integer result = myUserMapper.insertBatch(list);
        System.out.println(result);
    }
```

### 查询数据

```java
    @Test
    public void one() {
        // 查询数据
        MyUser myUser = EsWrappers.lambdaChainQuery(myUserMapper)
                .eq(MyUser::getProvince, "重庆市")
                .limit(1)
                .one();
        System.out.println(myUser);
    }
```

### 查询数据列表

```java
    @Test
    public void list() {
        // 查询数据列表
        List<MyUser> list = EsWrappers.lambdaChainQuery(myUserMapper)
                .eq(MyUser::getProvince, "重庆市")
                .list();
        System.out.println(list);
    }
```

### 根据时间范围查询数据列表

```java
    @Test
    public void listByTime() {
        // 根据时间范围查询数据列表
        List<MyUser> list = EsWrappers.lambdaChainQuery(myUserMapper)
                .between(MyUser::getBirthday, "1990-01-01", "2000-01-01")
                .orderByDesc(MyUser::getCreateTime)
                .list();
        System.out.println(list);
    }
```

### 根据IP范围查询数据列表

参考：[官方文档](https://www.easy-es.cn/pages/f1f529/)

```java
    @Test
    public void listByIP() {
        // 根据IP范围查询数据列表
        List<MyUser> list = EsWrappers.lambdaChainQuery(myUserMapper)
                .eq(MyUser::getIpaddress, "10.0.0.0/8")
                .orderByDesc(MyUser::getCreateTime)
                .list();
        System.out.println(list);
    }
```

### 根据经纬度范围查询数据列表

参考：[官方文档](https://www.easy-es.cn/pages/775ca1/)

```java
    @Test
    public void listByGeo() {
        // 根据经纬度范围查询数据列表
        List<MyUser> list = EsWrappers.lambdaChainQuery(myUserMapper)
                .geoDistance(MyUser::getLocation, 1000.0, DistanceUnit.KILOMETERS, new GeoPoint(39.92, 116.44))
                .orderByDesc(MyUser::getCreateTime)
                .list();
        System.out.println(list);
    }
```

### 浅分页

```java
    @Test
    public void page() {
        // 浅分页
        // 分页查询，适用于查询数据量少于1万的情况
        EsPageInfo<MyUser> pageInfo = EsWrappers.lambdaChainQuery(myUserMapper)
                .match(MyUser::getProvince, "重庆")
                .page(1, 10);
        System.out.println(pageInfo);
    }
```

### 分页查询

```java
    @Test
    public void searchAfter() {
        // 分页查询
        // 使用searchAfter必须指定排序,若没有排序不仅会报错,而且对跳页也不友好. 需要保持searchAfter排序唯一,不然会导致分页失效,推荐使用id,uuid等进行排序.
        int pageSize = 10;
        LambdaEsQueryChainWrapper<MyUser> wrapper = EsWrappers.lambdaChainQuery(myUserMapper);
        wrapper.size(pageSize);
        // 必须指定一种排序规则,且排序字段值必须唯一 此处我选择用id进行排序 实际可根据业务场景自由指定,不推荐用创建时间,因为可能会相同
        wrapper.orderByDesc(MyUser::getId);
        // 第一页
        SAPageInfo<MyUser> saPageInfo = wrapper.searchAfterPage(null, 10);
        System.out.println(saPageInfo);
        // 获取下一页
        List<Object> nextSearchAfter = saPageInfo.getNextSearchAfter();
        SAPageInfo<MyUser> saNextPageInfo = wrapper.searchAfterPage(nextSearchAfter, pageSize);
        System.out.println(saNextPageInfo);
    }
```

