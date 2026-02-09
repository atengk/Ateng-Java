# Apache Tika

**Apache Tika** 是一个开源的 Java 库，被誉为文件处理领域的“瑞士军刀”。它的核心使命是：**从各种格式的文档中检测类型并提取元数据和文本内容。**

无论你交给它的是 PDF、Word、Excel、图片、音频还是视频，Tika 都能通过统一的接口告诉你：“这是什么类型的文件”以及“文件里写了什么”。

- [官网地址](https://tika.apache.org/)



## 基础配置

### 添加依赖

```xml
<properties>
    <tika.version>3.2.3</tika.version>
</properties>
<dependencies>
    <!-- Apache Tika 检测库 -->
    <dependency>
        <groupId>org.apache.tika</groupId>
        <artifactId>tika-core</artifactId>
        <version>${tika.version}</version>
    </dependency>
    <!-- Apache Tika 解析内容库 -->
    <dependency>
        <groupId>org.apache.tika</groupId>
        <artifactId>tika-parsers-standard-package</artifactId>
        <version>${tika.version}</version>
    </dependency>
</dependencies>
```

### 创建 TikaUtil 

```java

```



## 使用测试
