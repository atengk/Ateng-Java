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



## FastJson2Util 

FastJSON2 工具类

**添加依赖**

```xml
<properties>
    <fastjson2.version>2.0.61</fastjson2.version>
</properties>
<dependencies>
    <!-- 高性能的JSON库 -->
    <!-- https://github.com/alibaba/fastjson2/wiki/fastjson2_intro_cn#0-fastjson-20%E4%BB%8B%E7%BB%8D -->
    <dependency>
        <groupId>com.alibaba.fastjson2</groupId>
        <artifactId>fastjson2</artifactId>
        <version>${fastjson2.version}</version>
    </dependency>
</dependencies>
```



## FastJsonUtil 

FastJSON1 工具类

**添加依赖**

```xml
<properties>
    <fastjson.version>1.2.83</fastjson.version>
</properties>
<dependencies>
    <!-- 高性能的JSON库 -->
    <dependency>
        <groupId>com.alibaba</groupId>
        <artifactId>fastjson</artifactId>
        <version>${fastjson.version}</version>
    </dependency>
</dependencies>
```



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


## VirtualThreadUtil

虚拟线程工具类


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



## SqlFormatUtil

SQL格式化工具类

**添加依赖**

```xml
<!-- SQL格式化 -->
<dependency>
    <groupId>com.github.vertical-blank</groupId>
    <artifactId>sql-formatter</artifactId>
    <version>2.0.5</version>
</dependency>
```



## AssertUtil

断言工具类



## ImageUtil

图片工具类



## OshiUtil

OSHI 系统监控工具类

**添加依赖**

```xml
<!-- OSHI 硬件信息获取 -->
<dependency>
    <groupId>com.github.oshi</groupId>
    <artifactId>oshi-core</artifactId>
    <version>6.9.3</version>
</dependency>
```



## SshClientUtil 

Apache MINA SSHD 是 The Apache Software Foundation 旗下的 Java SSH 实现项目，为 Java 应用程序提供 SSH 客户端与服务器功能。它广泛用于嵌入式管理、自动化部署和安全文件传输场景，以模块化设计和可扩展性著称。

**添加依赖**

```xml
<!-- Apache MINA SSHD 核心模块 -->
<dependency>
    <groupId>org.apache.sshd</groupId>
    <artifactId>sshd-core</artifactId>
    <version>2.17.1</version>
</dependency>
<!-- Apache MINA SSHD 公共模块 -->
<dependency>
    <groupId>org.apache.sshd</groupId>
    <artifactId>sshd-common</artifactId>
    <version>2.17.1</version>
</dependency>
<!-- Apache MINA SSHD OpenSSH 私钥解析模块 -->
<dependency>
    <groupId>org.apache.sshd</groupId>
    <artifactId>sshd-openpgp</artifactId>
    <version>2.17.1</version>
</dependency>
```



## CommonUtil

通用基础工具类（基于 Hutool 工具类）

**添加依赖**

```xml
<properties>
    <hutool.version>5.8.44</hutool.version>
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



## MachineFingerprintUtil

机器指纹工具类

组成：
CPU + 主网卡MAC + 磁盘序列号 + 主机名
特性：
1. 自动容错（字段缺失不影响整体）
2. 多磁盘合并（避免随机性）
3. 统一标准化（保证hash一致）
4. 内置缓存（避免重复计算）

**添加依赖**

```xml
<properties>
    <hutool.version>5.8.44</hutool.version>
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



