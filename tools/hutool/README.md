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

引入依赖，此时引入依赖就不需要设备版本号了

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

### BeanUtil

Bean工具类主要是针对这些setXXX和getXXX方法进行操作，比如将Bean对象转为Map等等。

见测试类：BeanUtilTests

### CaptchaUtil

验证码工具

见测试类：CaptchaTests

### CollUtil

集合工具

见测试类：CollUtilTests

### Convert

类型转换工具类

见测试类：ConvertTests

### CoordinateUtil

坐标系转换相关工具类

见测试类：CoordinateUtilTests

### DateUtil

日期时间工具类

见测试类：DateUtilTests

### EmojiUtil

Emoji工具类

见测试类：EmojiUtilTests

### ExceptionUtil

针对异常封装，例如包装为RuntimeException。

见测试类：ExceptionUtilTests

### FileUtil

文件工具类

见测试类：FileUtilTests

### FTPUtil

FTP工具类和配置

见测试类：FtpUtilTests

### HttpUtil

HTTP客户端

见测试类：HttpUtilTests

### IdUtil

ID生成工具类

见测试类：IdUtilTests

### JsonUtil

JSON 工具类

见测试类：JsonUtilTests

### JWTUtil

JWT就是一种网络身份认证和信息交换格式

见测试类：JWTUtilTests

### LogFactory和StaticLog

Logfactory.get方法不再需要（或者不是必须）传入当前类名，会自动解析当前类名

见测试类：LogFactoryTests

### MachineFingerprintUtil

获取机器唯一指纹

见测试类：MachineFingerprintTest

### MetaUtil

MetaUtil 数据库表元数据读取

见测试类：MetaUtilTests

### NetUtil

网络工具

见测试类：NetUtilTests

### NumberUtil

数字工具

见测试类：NumberUtilTests

### OshiUtil

系统信息工具类

见测试类：OshiUtilTests

### ReUtil

正则工具

见测试类：ReUtilTests

### SecureUtil

加密解密工具

见测试类：SecureUtilTests

### SpringUtil

Spring中Bean获取的工具类——SpringUtil

见测试类：SpringUtilTests

### StrUtil

字符串工具类

见测试类：StrUtilTests

### TreeUtil

树工具类

见测试类：TreeUtilTests

### UrlBuilder

UrlBuilder主要应用于http模块，在构建HttpRequest时，用户传入的URL五花八门，为了做大最好的适应性，减少用户对URL的处理，使用UrlBuilder完成URL的规范化。

见测试类：UrlBuilderUtilTests

### URLUtil

URL工具

见测试类：URLUtilTests

### XmlUtil

简化XML的创建、读和写的过程

见测试类：XmlUtilTests

### ZipUtil

压缩包工具类

见测试类：ZipUtils



