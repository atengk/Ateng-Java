# MongoTemplate

`MongoTemplate` 是 Spring Data MongoDB 提供的一个核心类，用于在 Spring 应用中操作 MongoDB 数据库。它为 MongoDB 提供了简单且强大的 API，允许开发者通过常用的 Java 方法与 MongoDB 进行交互。

参考：[官方文档](https://spring.io/projects/spring-data-mongodb)

## 基础配置

### 添加依赖

```xml
<!-- MongoDB 依赖 -->
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-data-mongodb</artifactId>
</dependency>
```

### 编辑配置文件

```yaml
server:
  port: 15008
  servlet:
    context-path: /
spring:
  main:
    web-application-type: servlet
  application:
    name: ${project.artifactId}
---
# MongoPlus 的相关配置
spring:
  data:
    mongodb:
      host: 192.168.1.10 # 数据库的ip地址
      authentication-database: admin # 登录用户所在的数据库
      port: 33627 # 端口号
      username: root # 用户账号
      password: Admin@123 # 用户密码
      database: ateng # 指定使用的数据库
```

### 创建实体类

```java
package local.ateng.java.mongo.entity;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.io.Serial;
import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Date;


@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Document("my_user")
public class MyUser implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * 主键id
     */
    @Id
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
    private String location;

    /**
     * 一段文字
     */
    private String paragraph;

    /**
     * 记录创建时间，默认当前时间
     */
    private LocalDateTime createTime;

}
```

### 创建数据生成器

```java
package local.ateng.java.mongo.init;

import com.github.javafaker.Faker;
import local.ateng.java.mongo.entity.MyUser;
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
    public static List<MyUser> list;

    static {
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



## 使用Template

### 创建测试类

```java
package local.ateng.java.mongo;

import com.mongodb.client.result.DeleteResult;
import com.mongodb.client.result.UpdateResult;
import local.ateng.java.mongo.entity.MyUser;
import local.ateng.java.mongo.init.InitData;
import lombok.RequiredArgsConstructor;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.Update;

import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.regex.Pattern;

@SpringBootTest
@RequiredArgsConstructor(onConstructor = @__(@Autowired))
public class MongoTemplateTests {
    private final MongoTemplate mongoTemplate;
}
```

### 新增数据

```java
    // 新增数据
    @Test
    void saveUser() {
        Collection<MyUser> list = mongoTemplate.insertAll(InitData.list);
        System.out.println(list);
    }
```

### 查询数据列表

```java
    // 查询数据列表
    @Test
    void getAll() {
        List<MyUser> users = mongoTemplate.findAll(MyUser.class);
        users.forEach(System.out::println);
    }
```

### 根据ID查询

```java
    @Test
    void getById() {
        MyUser myUser = mongoTemplate.findById("67a5be3d2509c17b98a797ac", MyUser.class);
        System.out.println(myUser);
    }
```

### 条件查询

```java
    @Test
    void getQuery() {
        Query query = new Query(Criteria.where("province").is("重庆市").and("age").gte(25));
        List<MyUser> users = mongoTemplate.find(query, MyUser.class);
        users.forEach(System.out::println);
    }
```

### 模糊查询

```java
    // 模糊查询
    @Test
    void findLikeName() {
        String province = "重庆";
        String regex = String.format("%s%s%s", "^.*", province, ".*$");//采用正则表达式进行匹配
        Pattern pattern = Pattern.compile(regex, Pattern.CASE_INSENSITIVE);
        Query query = new Query(Criteria.where("province").regex(pattern));
        List<MyUser> users = mongoTemplate.find(query, MyUser.class);
        users.forEach(System.out::println);
    }
```

### 分页查询

```java
    // 分页查询
    @Test
    void findPage() {
        String province = "重庆";
        int pageNo = 1; // 当前页
        int pageSize = 10; // 每页的大小

        Query query = new Query(); //条件构建部分
        String regex = String.format("%s%s%s", "^.*", province, ".*$");
        Pattern pattern = Pattern.compile(regex, Pattern.CASE_INSENSITIVE);
        query.addCriteria(Criteria.where("province").regex(pattern));
        int totalCount = (int) mongoTemplate.count(query, MyUser.class); //查询记录数
        //其中的skip表示跳过的记录数，当前页为1则跳过0条，为2则跳过10条，（也就是跳过第一页的10条数据）
        List<MyUser> users = mongoTemplate.find(query.skip((pageNo - 1) * pageSize).limit(pageSize), MyUser.class);//分页查询
        HashMap<String, Object> map = new HashMap<>();
        map.put("data", users);
        map.put("total", totalCount);
        System.out.println(map);
    }
```

### 修改数据

```java
    // 修改数据
    @Test
    void updateUser() {
        Query query = new Query(Criteria.where("_id").is("67a5be3d2509c17b98a797dd"));
        Update update = new Update().set("age", 26);
        UpdateResult updateResult = mongoTemplate.updateFirst(query, update, MyUser.class);
        System.out.println(updateResult);
    }
```

### 删除数据

```java
    // 删除数据
    @Test
    void delete() {
        Query query = new Query(Criteria.where("_id").is("67a5be3d2509c17b98a797dd"));
        DeleteResult result = mongoTemplate.remove(query, MyUser.class);
        System.out.println(result);
    }
```

