# Hutool

📚简介
Hutool是一个小而全的Java工具类库，通过静态方法封装，降低相关API的学习成本，提高工作效率，使Java拥有函数式语言般的优雅，让Java语言也可以“甜甜的”。

Hutool中的工具方法来自每个用户的精雕细琢，它涵盖了Java开发底层代码中的方方面面，它既是大型项目开发中解决小问题的利器，也是小型项目中的效率担当；

Hutool是项目中“util”包友好的替代，它节省了开发人员对项目中公用类和公用工具方法的封装时间，使开发专注于业务，同时可以最大限度的避免封装不完善带来的bug。

🎁Hutool名称的由来

Hutool = Hu + tool，是原公司项目底层代码剥离后的开源库，“Hu”是公司名称的表示，tool表示工具。Hutool谐音“糊涂”，一方面简洁易懂，一方面寓意“难得糊涂”。

参考：[官方文档](https://doc.hutool.cn/pages/index)



## 添加依赖

### 全部添加

```xml
<properties>
    <hutool.version>5.8.35</hutool.version>
</properties>
<dependencies>
    <!-- Hutool: Java工具库，提供了许多实用的工具方法 -->
    <dependency>
        <groupId>cn.hutool</groupId>
        <artifactId>hutool-all</artifactId>
        <version>${hutool.version}</version>
    </dependency>
</dependencies>
```

### 使用import

添加依赖管理

```xml
<dependencyManagement>
    <dependencies>
        <dependency>
            <groupId>cn.hutool</groupId>
            <artifactId>hutool-bom</artifactId>
            <version>${hutool.version}</version>
            <type>pom</type>
            <scope>import</scope>
        </dependency>
    </dependencies>
</dependencyManagement>
```

映入依赖，此时引入依赖就不需要设备版本号了

```xml
<dependency>
    <groupId>cn.hutool</groupId>
    <artifactId>hutool-core</artifactId>
</dependency>
<dependency>
    <groupId>cn.hutool</groupId>
    <artifactId>hutool-http</artifactId>
</dependency>
<dependency>
    <groupId>cn.hutool</groupId>
    <artifactId>hutool-extra</artifactId>
</dependency>
<dependency>
    <groupId>cn.hutool</groupId>
    <artifactId>hutool-jwt</artifactId>
</dependency>
```

如果需要排除模块，引入的模块比较多，但是某几个模块没用，可以：

```xml
<dependency>
    <groupId>cn.hutool</groupId>
    <artifactId>hutool-bom</artifactId>
    <version>${hutool.version}</version>
    <type>pom</type>
    <scope>import</scope>
    <exclusions>
        <exclusion>
            <groupId>cn.hutool</groupId>
            <artifactId>hutool-system</artifactId>
        </exclusion>
    </exclusions>
</dependency>
```



## 使用Hutool

见 `test` 包下的测试代码

![image-20250226112925787](./assets/image-20250226112925787.png)
