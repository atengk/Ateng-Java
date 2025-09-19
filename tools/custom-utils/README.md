# 自定义工具类模块

## FileUtil

文件工具类



## FileTypeUtil

获取文件类型工具类

**添加依赖**

```xml
<!--文件类型工具类-->
<dependency>
    <groupId>org.apache.tika</groupId>
    <artifactId>tika-core</artifactId>
    <version>3.2.1</version>
</dependency>
```



## ZipUtil

压缩/解压工具类



## DateTimeUtil

日期/时间工具类



## StringUtil

字符串工具类



## ObjectUtil

对象工具类



## JsonUtil 

JSON 工具类



## ValidateUtil

效验工具类



## BeanUtil

JavaBean对象工具类



## EnumUtil

枚举工具类



## SpringUtil

Springboot 相关的工具类



## CollectionUtil

集合、列表相关的工具类



## MapUtil

Map 工具类



## SystemUtil

系统工具类



## EncodingUtil

编码工具类



## RandomUtil

随机数工具类



## NumberUtil

数字工具类



## HttpUtil

HTTP工具类



## XmlUtil

Xml工具类

**添加依赖**

```xml
<!-- Jackson 核心模块：用于 JSON 与 Java 对象的互转 -->
<dependency>
    <groupId>com.fasterxml.jackson.core</groupId>
    <artifactId>jackson-databind</artifactId>
</dependency>

<!-- Jackson XML 扩展模块：用于 XML 与 Java 对象、JSON 的互转 -->
<dependency>
    <groupId>com.fasterxml.jackson.dataformat</groupId>
    <artifactId>jackson-dataformat-xml</artifactId>
</dependency>
```



## WordUtil

Word工具类

**添加依赖**

```xml
<properties>
    <poi.version>5.3.0</poi.version>
</properties>
<dependencies>
    <!-- Apache POI 核心库，用于操作 Excel（HSSF 格式，即 .xls）和 Word 等旧版 Office 文档 -->
    <dependency>
        <groupId>org.apache.poi</groupId>
        <artifactId>poi</artifactId>
        <version>${poi.version}</version>
    </dependency>

    <!-- Apache POI 扩展库，支持操作 Excel 2007+（XSSF 格式，即 .xlsx）等新版 Office 文档 -->
    <dependency>
        <groupId>org.apache.poi</groupId>
        <artifactId>poi-ooxml</artifactId>
        <version>${poi.version}</version>
    </dependency>
</dependencies>
```



## PDFUtil

PDF工具类

**添加依赖**

```xml
<!-- OpenPDF (iText 2.x 开源分支) -->
<dependency>
    <groupId>com.github.librepdf</groupId>
    <artifactId>openpdf</artifactId>
    <version>1.3.43</version>
</dependency>
```



## AsyncUtil

异步和线程池工具类



## DesensitizedUtil

数据脱敏工具类



## SqlUtil

SQL工具类

**添加依赖**

```xml
<!-- JSqlParser：解析和操作 SQL 语句 -->
<dependency>
    <groupId>com.github.jsqlparser</groupId>
    <artifactId>jsqlparser</artifactId>
    <version>4.6</version>
</dependency>
```

