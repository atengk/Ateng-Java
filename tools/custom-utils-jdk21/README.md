# 自定义工具类模块（基于 JDK21）



## SecurityUtil

安全工具类，提供随机数、编码、摘要、HMAC、AES、RSA、签名、PEM、证书、密码、JWT、接口签名、文件安全、脱敏等常用能力

使用方法（Test包）：io.github.atengk.security



## VirtualThreadUtil

虚拟线程工具类

使用方法（Test包）：io.github.atengk.thread



## ValidateUtil

效验工具类

使用方法（Test包）：io.github.atengk.validation

添加依赖

```xml
<!-- Spring Boot 参数校验依赖，提供 Jakarta Bean Validation 支持，用于 @NotNull、@NotBlank、@Size、@Valid 等注解校验 -->
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-validation</artifactId>
</dependency>
```



## SpringUtil

Spring 上下文工具类

使用方法（Test包）：io.github.atengk.spring

添加依赖

```xml
<dependency>
    <groupId>org.springframework</groupId>
    <artifactId>spring-tx</artifactId>
</dependency>
```



## CommonUtil

通用基础工具类（基于 Hutool 工具库）

使用方法（Test包）：io.github.atengk.CommonUtilTest



## CollectionUtil

集合工具类

使用方法（Test包）：io.github.atengk.collection



## StringUtil

字符串工具类

使用方法（Test包）：io.github.atengk.string



## BeanUtil

Java Bean 基础反射工具类

使用方法（Test包）：io.github.atengk.bean



## DateTimeUtil

日期时间工具类

使用方法（Test包）：io.github.atengk.datetime



## ZipUtil

压缩解压工具类

使用方法（Test包）：io.github.atengk.zip

添加依赖

```xml
<properties>
    <!-- 解压压缩依赖版本 -->
    <zip4j.version>2.11.6</zip4j.version>
    <commons-compress.version>1.28.0</commons-compress.version>
    <xz.version>1.12</xz.version>
</properties>
<!-- 项目依赖 -->
<dependencies>
    <dependency>
        <groupId>net.lingala.zip4j</groupId>
        <artifactId>zip4j</artifactId>
        <version>${zip4j.version}</version>
    </dependency>
    <dependency>
        <groupId>org.apache.commons</groupId>
        <artifactId>commons-compress</artifactId>
        <version>${commons-compress.version}</version>
    </dependency>
    <dependency>
        <groupId>org.tukaani</groupId>
        <artifactId>xz</artifactId>
        <version>${xz.version}</version>
    </dependency>
</dependencies>
```




## EnumUtil

枚举工具类

使用方法（Test包）：io.github.atengk.enums




## ObjectUtil

对象工具类

使用方法（Test包）：io.github.atengk.object



## MapUtil

Map 工具类

使用方法（Test包）：io.github.atengk.maputil



## NumberUtil

数字 工具类

使用方法（Test包）：io.github.atengk.number



## FileUtil

文件 工具类

使用方法（Test包）：io.github.atengk.file



## FileTypeUtil

文件类型 工具类

使用方法（Test包）：io.github.atengk.filetype

添加依赖

```xml
<!-- Apache Tika 核心模块：提供文件类型检测和 MIME 类型识别能力 -->
<dependency>
    <groupId>org.apache.tika</groupId>
    <artifactId>tika-core</artifactId>
    <version>3.3.0</version>
</dependency>
```



## CodecUtil

通用编解码工具类

使用方法（Test包）：io.github.atengk.codec



## ResourceUtil

基于 Spring Resource 的资源工具类

使用方法（Test包）：io.github.atengk.resource



## SystemUtil

系统运行环境工具类，提供操作系统、JVM、进程、环境变量、路径、命令、网络、容器和诊断等通用能力

使用方法（Test包）：io.github.atengk.system



## XmlUtil

系统运行环境工具类，提供操作系统、JVM、进程、环境变量、路径、命令、网络、容器和诊断等通用能力

使用方法（Test包）：io.github.atengk.xml

添加依赖

```xml
<!-- Jackson XML：对象、Tree、Map 与 XML 互转核心依赖 -->
<dependency>
    <groupId>com.fasterxml.jackson.dataformat</groupId>
    <artifactId>jackson-dataformat-xml</artifactId>
</dependency>

<!-- Jackson JavaTime：支持 LocalDate、LocalDateTime 等 JDK 8+ 时间类型 -->
<dependency>
    <groupId>com.fasterxml.jackson.datatype</groupId>
    <artifactId>jackson-datatype-jsr310</artifactId>
</dependency>
```

