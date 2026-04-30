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



## OsUtil

系统运行环境工具类，提供操作系统、JVM、进程、环境变量、路径、命令、网络、容器和诊断等通用能力

使用方法（Test包）：io.github.atengk.os



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



## ImageUtil

图像处理工具类，提供读取、写入、缩放、裁剪、压缩、格式转换、水印、拼接、校验等常用能力

使用方法（Test包）：io.github.atengk.image



## DesensitizedUtil

数据脱敏工具类，提供常见个人信息、联系方式、账号凭证、金融支付、网络设备、文本、日志、对象字段等脱敏能力

使用方法（Test包）：io.github.atengk.desensitized



## OshiUtil

OSHI 系统监控静态工具类，封装操作系统、CPU、内存、磁盘、网络、进程、硬件、JVM 和运行环境信息

使用方法（Test包）：io.github.atengk.oshi

添加依赖

```xml
<properties>
    <oshi.version>6.12.0</oshi.version>
</properties>
<dependencies>
    <!-- OSHI JNA 版 -->
    <dependency>
        <groupId>com.github.oshi</groupId>
        <artifactId>oshi-core</artifactId>
        <version>${oshi.version}</version>
    </dependency>
</dependencies>
```





## RandomUtil

随机工具类，提供数字、字符串、集合、时间、安全随机、权重随机、模拟数据等常用随机能力

使用方法（Test包）：io.github.atengk.random



## WordUtil

Word 文档工具类，基于 Apache POI 的 XWPFDocument 实现常用 docx 操作

使用方法（Test包）：io.github.atengk.word

添加依赖

```xml
<properties>
    <poi.version>5.5.1</poi.version>
</properties>
<dependencies>
    <!-- Apache POI OOXML，用于处理 docx 文档 -->
    <dependency>
        <groupId>org.apache.poi</groupId>
        <artifactId>poi-ooxml</artifactId>
        <version>${poi.version}</version>
    </dependency>
</dependencies>
```



## PDFUtil

PDF 通用工具类，基于 OpenPDF 封装 PDF 创建、编辑、读取、加密、表单和常用业务能力

使用方法（Test包）：io.github.atengk.pdf

添加依赖

```xml
<properties>
    <openpdf.version>3.0.3</openpdf.version>
</properties>
<dependencies>
    <dependency>
        <groupId>com.github.librepdf</groupId>
        <artifactId>openpdf</artifactId>
        <version>${openpdf.version}</version>
    </dependency>
</dependencies>
```





## DiffUtil

差异对比工具类，覆盖单值、对象、集合、Map、文本、JSON、文件、补丁、审计日志、树、快照和同步数据等常见项目场景

使用方法（Test包）：io.github.atengk.diff




## IdUtil

ID 工具类，提供 UUID、UUID v7、Snowflake、ULID、业务单号、TraceId、短 ID、验证码、文件名、安全随机 ID 等常用能力

使用方法（Test包）：io.github.atengk.id



## MailUtil

邮件工具类，基于 JDK 21 与 Jakarta Mail 提供邮件发送、接收、解析、附件、模板、校验等通用能力

使用方法（Test包）：io.github.atengk.mail

添加依赖

```xml
<properties>
    <jakarta.mail.version>2.0.1</jakarta.mail.version>
    <jakarta.activation.version>2.0.1</jakarta.activation.version>
</properties>
<dependencies>
    <!-- Jakarta Mail 实现，提供 SMTP、IMAP、POP3、MIME 等邮件能力 -->
    <dependency>
        <groupId>com.sun.mail</groupId>
        <artifactId>jakarta.mail</artifactId>
        <version>${jakarta.mail.version}</version>
    </dependency>
    
    <!-- Jakarta Activation，处理附件、DataHandler、DataSource 等内容类型能力 -->
    <dependency>
        <groupId>com.sun.activation</groupId>
        <artifactId>jakarta.activation</artifactId>
        <version>${jakarta.activation.version}</version>
    </dependency>
</dependencies>
```




## AnnotationUtil

JDK 原生注解工具类，提供注解判断、查找、属性读取、元注解解析、合并、缓存等通用能力

使用方法（Test包）：io.github.atengk.annotation



