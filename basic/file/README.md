# IO 与 NIO 文件处理



## 基础概念

本节主要说明 Java 项目开发中文件处理相关的基础概念，包括传统 IO 与 NIO 的区别、字节流与字符流的使用场景、输入流与输出流的方向关系，以及阻塞 IO 与非阻塞 IO 的处理模型。理解这些概念后，在处理文件上传、文件下载、文本读取、大文件复制、批量导入导出等场景时，可以更准确地选择合适的 API。

### IO 与 NIO 的区别

Java IO 是传统的流式处理模型，数据按照顺序从输入流读取，或者通过输出流写入目标位置。它的编程模型简单，适合普通文件读写、配置文件读取、小型文本处理、文件复制等场景。

Java NIO 是面向缓冲区和通道的处理模型，核心组件包括 `Buffer`、`Channel`、`Selector`、`Path`、`Files` 等。对于文件处理来说，常用的是 `Path`、`Files`、`FileChannel` 和 `ByteBuffer`。NIO 更适合大文件处理、文件通道复制、随机访问文件、文件锁、目录遍历等场景。

| 对比项     | IO                                                | NIO                                          |
| ---------- | ------------------------------------------------- | -------------------------------------------- |
| 数据模型   | 面向流                                            | 面向缓冲区和通道                             |
| 数据方向   | 通常是单向流动                                    | 通道可以配合缓冲区双向操作                   |
| 常用类     | `InputStream`、`OutputStream`、`Reader`、`Writer` | `Path`、`Files`、`FileChannel`、`ByteBuffer` |
| 文件定位   | 多数场景按顺序读写                                | 支持指定位置读写                             |
| 编程复杂度 | 简单直观                                          | 相对复杂                                     |
| 适用场景   | 小文件、文本文件、普通复制                        | 大文件、高性能复制、随机访问、文件锁         |

在实际项目中，不需要刻意追求 NIO。普通业务文件处理可以优先使用传统 IO、`Files` 工具类或 Hutool 的 `FileUtil`、`IoUtil`。当文件较大、需要高性能复制、需要随机读写或需要更细粒度控制文件通道时，再考虑使用 NIO。

常见选择建议如下：

| 场景               | 推荐方式                                                  |
| ------------------ | --------------------------------------------------------- |
| 读取配置文件       | IO、`Files`、Hutool `FileUtil`                            |
| 按行读取文本       | 字符流、`Files.readAllLines`、Hutool `FileUtil.readLines` |
| 上传文件保存到本地 | IO、`Files.copy`                                          |
| 普通文件复制       | `Files.copy`、Hutool `FileUtil.copy`                      |
| 大文件复制         | NIO `FileChannel`                                         |
| 文件随机读写       | `RandomAccessFile`、`FileChannel`                         |
| 扫描目录文件       | NIO `Files.walk`                                          |
| 文件加锁           | NIO `FileChannel.lock`                                    |

### 字节流与字符流

字节流以字节为单位处理数据，适合处理所有类型的文件。图片、视频、音频、压缩包、PDF、Excel、Word 等文件，本质上都应该使用字节流处理。Java 中常见的字节流包括 `InputStream`、`OutputStream`、`FileInputStream`、`FileOutputStream`、`BufferedInputStream` 和 `BufferedOutputStream`。

字符流以字符为单位处理数据，适合处理文本内容。常见文本文件包括 `.txt`、`.csv`、`.json`、`.xml`、`.sql`、`.log` 等。Java 中常见的字符流包括 `Reader`、`Writer`、`FileReader`、`FileWriter`、`BufferedReader`、`BufferedWriter`、`InputStreamReader` 和 `OutputStreamWriter`。

字节流和字符流最大的区别是：字符流涉及字符编码，字节流不直接处理字符编码。

| 类型   | 处理单位 | 是否关注编码 | 适合处理                                       |
| ------ | -------- | ------------ | ---------------------------------------------- |
| 字节流 | byte     | 不关注       | 图片、视频、压缩包、PDF、Excel、任意二进制文件 |
| 字符流 | char     | 关注         | TXT、CSV、JSON、XML、SQL、日志文件             |

在 Java 项目中，可以按照以下原则选择：

1. 只要文件不是纯文本，优先使用字节流。
2. 只要需要按行读取文本，优先使用字符流。
3. 只要需要处理中文、特殊符号或跨平台文本文件，必须明确指定字符编码。
4. 读取文本时推荐使用 `UTF-8`，避免依赖操作系统默认编码。
5. 文件上传、下载、复制等不关心文本内容的操作，通常使用字节流。

例如，读取图片、PDF、Excel 时，不应该使用 `Reader` 或 `Writer`，而应该使用 `InputStream` 和 `OutputStream`。读取 `.txt`、`.csv` 或 `.log` 文件时，如果需要逐行解析内容，则更适合使用 `BufferedReader`。

字符流底层仍然依赖字节流。`InputStreamReader` 可以把字节输入流转换成字符输入流，`OutputStreamWriter` 可以把字符输出流转换成字节输出流。转换时需要指定字符集，例如 `UTF-8`。

### 输入流与输出流

输入流和输出流是以 Java 程序为中心定义的。

输入流表示数据从外部进入 Java 程序，例如从本地文件读取内容、从网络连接读取数据、从上传文件中读取字节。输出流表示数据从 Java 程序写出到外部，例如写入本地文件、写入 HTTP 响应、写入压缩包、写入对象存储等。

| 类型   | 数据方向           | 常见类                   | 常见场景                               |
| ------ | ------------------ | ------------------------ | -------------------------------------- |
| 输入流 | 外部数据进入程序   | `InputStream`、`Reader`  | 读取文件、读取上传内容、读取网络数据   |
| 输出流 | 程序数据写出到外部 | `OutputStream`、`Writer` | 保存文件、文件下载、导出报表、写入日志 |

在文件复制场景中，输入流和输出流通常成对出现。程序先通过输入流读取源文件内容，再通过输出流写入目标文件。

文件复制的数据流向可以理解为：

```text
源文件 -> 输入流 -> Java 程序缓冲区 -> 输出流 -> 目标文件
```

在 Spring Boot 文件上传场景中，上传文件的数据通过输入流进入后端程序，后端程序再将数据保存到磁盘、数据库或对象存储中。在文件下载场景中，后端程序读取文件内容后，通过输出流写入 HTTP 响应，浏览器或客户端接收响应后保存文件。

使用输入流和输出流时，需要重点关注资源关闭问题。如果流没有正确关闭，可能会导致文件句柄泄漏、文件被占用、数据未完全写入等问题。推荐使用 `try-with-resources` 自动关闭资源。

常见注意事项如下：

1. 输入流和输出流使用完成后必须关闭。
2. 输出流写入完成后建议执行 `flush()`，确保缓冲区数据被写出。
3. 大文件读写不要一次性加载到内存，应使用缓冲区分批处理。
4. 文本文件处理应明确字符编码。
5. 文件路径应做合法性校验，避免路径穿越风险。

### 阻塞 IO 与非阻塞 IO

阻塞 IO 指线程在执行读写操作时，如果数据还没有准备好，或者目标暂时不可写，当前线程会等待，直到读写完成、发生异常或超时。传统 Java IO 的大多数操作都是阻塞式的，例如 `InputStream.read()` 和 `OutputStream.write()`。

非阻塞 IO 指线程发起读写操作后，如果数据暂时不可用，不会一直等待，而是立即返回结果，由程序后续继续检查或通过事件通知机制处理。Java NIO 中的 `SocketChannel`、`ServerSocketChannel`、`Selector` 可以支持非阻塞网络 IO。

需要注意的是，在普通文件处理中，NIO 并不等同于“所有操作都是非阻塞”。NIO 在文件处理中的主要价值更多体现在通道、缓冲区、路径工具、高性能复制、随机访问和文件锁等能力上。典型的非阻塞模型更多出现在网络通信场景中。

| 类型      | 行为特点             | 优点               | 缺点                     | 常见场景                               |
| --------- | -------------------- | ------------------ | ------------------------ | -------------------------------------- |
| 阻塞 IO   | 当前线程等待操作完成 | 编程简单，逻辑清晰 | 高并发下线程资源消耗较大 | 普通文件读写、文件上传下载、批处理任务 |
| 非阻塞 IO | 数据未就绪时立即返回 | 适合高并发连接     | 编程复杂，需要事件模型   | 网络服务、长连接通信、网关、中间件     |

在普通 Java 后端业务中，文件上传、文件下载、报表导出、日志读取、数据导入等场景，大多数情况下使用阻塞 IO 即可。因为这些任务通常是明确的文件读写任务，代码可读性和稳定性比复杂的非阻塞模型更重要。

在高并发网络通信场景中，阻塞 IO 的问题会更明显。如果每个连接都占用一个线程，当连接数大量增加时，会造成线程数量膨胀、上下文切换增加、内存占用升高等问题。此时可以考虑使用 NIO、Netty、WebFlux 等技术。

实际选择时可以参考以下原则：

1. 普通文件读写优先使用阻塞 IO、`Files` 或 Hutool 工具类。
2. 大文件复制、随机访问、文件锁等场景优先考虑 NIO。
3. 高并发网络通信优先考虑 NIO、Netty 或响应式框架。
4. Spring Boot 普通业务接口中，不要为了“非阻塞”而过度复杂化文件处理代码。
5. 对于耗时文件任务，可以通过线程池、异步任务、消息队列等方式解耦，而不是强行改造成非阻塞 IO。



## 文件读写基础

本节主要说明 Java 项目中文件读写的基础操作，包括 `File` 类的使用、文件创建与删除、常见读取方式、写入方式，以及追加写入和覆盖写入的区别。普通业务开发中，建议优先使用 `Files` 或 Hutool 的 `FileUtil` 简化文件操作，只有在需要更细粒度控制时再直接使用底层流对象。

### File 类的基本使用

`File` 是 Java 早期提供的文件和目录抽象类，位于 `java.io` 包下。它既可以表示文件，也可以表示目录，但它本身不直接代表文件内容，只用于描述文件路径、文件名、文件大小、是否存在、是否为目录等元信息。

常见方法如下：

| 方法                | 说明                   |
| ------------------- | ---------------------- |
| `exists()`          | 判断文件或目录是否存在 |
| `isFile()`          | 判断是否为普通文件     |
| `isDirectory()`     | 判断是否为目录         |
| `getName()`         | 获取文件名             |
| `getParent()`       | 获取父级路径           |
| `getAbsolutePath()` | 获取绝对路径           |
| `length()`          | 获取文件字节大小       |
| `lastModified()`    | 获取最后修改时间       |
| `listFiles()`       | 获取目录下的文件列表   |

需要注意的是，`File` 只负责路径和元信息，不适合表达现代文件系统能力。新项目中如果需要路径处理、文件复制、目录遍历、文件属性读取，通常更推荐使用 NIO 的 `Path` 和 `Files`。

下面示例演示 `File` 的常见元信息读取方式。

文件位置：`src/main/java/io/github/atengk/file/basic/FileInfoExample.java`

```java
package io.github.atengk.file.basic;

import cn.hutool.core.io.FileUtil;
import cn.hutool.core.util.NumberUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;

/**
 * 文件基础信息示例
 *
 * @author Ateng
 * @since 2026-05-13
 */
public class FileInfoExample {

    private static final Logger log = LoggerFactory.getLogger(FileInfoExample.class);

    /**
     * 打印文件基础信息
     *
     * @param filePath 文件路径
     */
    public static void printFileInfo(String filePath) {
        File file = FileUtil.file(filePath);

        if (!FileUtil.exist(file)) {
            log.warn("文件不存在，路径：{}", filePath);
            return;
        }

        LocalDateTime lastModifiedTime = LocalDateTime.ofInstant(
                Instant.ofEpochMilli(file.lastModified()),
                ZoneId.systemDefault()
        );

        log.info("文件名称：{}", file.getName());
        log.info("绝对路径：{}", file.getAbsolutePath());
        log.info("父级路径：{}", file.getParent());
        log.info("是否文件：{}", file.isFile());
        log.info("是否目录：{}", file.isDirectory());
        log.info("文件大小：{} KB", NumberUtil.div(file.length(), 1024, 2));
        log.info("最后修改时间：{}", lastModifiedTime);
    }
}
```

如果只是做文件路径判断，`File` 仍然可以使用。如果要处理跨平台路径、符号链接、文件属性、目录流、文件通道等能力，则建议使用 `Path` 和 `Files`。

### 文件创建与删除

文件创建主要包括创建普通文件、创建目录、创建多级目录。删除操作包括删除普通文件、删除空目录、递归删除目录等。

Java 原生方式可以使用 `File#createNewFile()`、`File#mkdir()`、`File#mkdirs()`，NIO 可以使用 `Files.createFile()`、`Files.createDirectory()`、`Files.createDirectories()`。在业务代码中，Hutool 的 `FileUtil.touch()`、`FileUtil.mkdir()`、`FileUtil.del()` 使用更简洁。

常见创建和删除方式如下：

| 操作         | 原生 API                    | Hutool API         |
| ------------ | --------------------------- | ------------------ |
| 创建文件     | `Files.createFile()`        | `FileUtil.touch()` |
| 创建目录     | `Files.createDirectory()`   | `FileUtil.mkdir()` |
| 创建多级目录 | `Files.createDirectories()` | `FileUtil.mkdir()` |
| 删除文件     | `Files.deleteIfExists()`    | `FileUtil.del()`   |
| 删除目录     | 手动递归或工具类            | `FileUtil.del()`   |

下面示例演示文件和目录的创建、删除操作。

文件位置：`src/main/java/io/github/atengk/file/basic/FileCreateDeleteExample.java`

```java
package io.github.atengk.file.basic;

import cn.hutool.core.io.FileUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;

/**
 * 文件创建与删除示例
 *
 * @author Ateng
 * @since 2026-05-13
 */
public class FileCreateDeleteExample {

    private static final Logger log = LoggerFactory.getLogger(FileCreateDeleteExample.class);

    /**
     * 创建普通文件，如果父级目录不存在会自动创建
     *
     * @param filePath 文件路径
     */
    public static void createFile(String filePath) {
        File file = FileUtil.touch(filePath);
        log.info("文件创建完成，路径：{}", file.getAbsolutePath());
    }

    /**
     * 创建目录，支持多级目录
     *
     * @param dirPath 目录路径
     */
    public static void createDirectory(String dirPath) {
        File directory = FileUtil.mkdir(dirPath);
        log.info("目录创建完成，路径：{}", directory.getAbsolutePath());
    }

    /**
     * 删除文件或目录
     *
     * @param path 文件或目录路径
     */
    public static void deleteFileOrDirectory(String path) {
        File file = FileUtil.file(path);

        if (!FileUtil.exist(file)) {
            log.warn("删除跳过，目标不存在，路径：{}", path);
            return;
        }

        boolean result = FileUtil.del(file);
        log.info("删除完成，路径：{}，结果：{}", path, result);
    }
}
```

需要注意的是，`FileUtil.del()` 删除目录时会递归删除目录下的内容，实际项目中应避免直接删除用户传入路径。涉及删除操作时，建议先做路径白名单、业务归属校验和日志记录，防止误删重要文件。

### 文件读取方式

文件读取方式可以按照文件类型和文件大小来选择。小型文本文件可以一次性读取，大型文本文件建议按行读取或流式读取，二进制文件建议使用字节流分批读取。

常见读取方式如下：

| 场景           | 推荐方式                                          |
| -------------- | ------------------------------------------------- |
| 小型文本文件   | `FileUtil.readUtf8String()`、`Files.readString()` |
| 按行读取文本   | `FileUtil.readLines()`、`BufferedReader`          |
| 二进制文件读取 | `InputStream`、`BufferedInputStream`              |
| 大文件读取     | 分批读取，避免一次性加载到内存                    |
| CSV、日志文件  | 按行读取，逐行解析                                |

下面示例提供三种常见读取方式：读取完整文本、按行读取文本、使用缓冲区读取字节。

文件位置：`src/main/java/io/github/atengk/file/basic/FileReadExample.java`

```java
package io.github.atengk.file.basic;

import cn.hutool.core.io.FileUtil;
import cn.hutool.core.util.CharsetUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedInputStream;
import java.io.IOException;
import java.util.List;

/**
 * 文件读取示例
 *
 * @author Ateng
 * @since 2026-05-13
 */
public class FileReadExample {

    private static final Logger log = LoggerFactory.getLogger(FileReadExample.class);

    /**
     * 一次性读取 UTF-8 文本文件
     *
     * @param filePath 文件路径
     * @return 文件内容
     */
    public static String readText(String filePath) {
        String content = FileUtil.readUtf8String(filePath);
        log.info("文本文件读取完成，路径：{}，字符数：{}", filePath, content.length());
        return content;
    }

    /**
     * 按行读取 UTF-8 文本文件
     *
     * @param filePath 文件路径
     * @return 文本行列表
     */
    public static List<String> readLines(String filePath) {
        List<String> lines = FileUtil.readLines(filePath, CharsetUtil.CHARSET_UTF_8);
        log.info("文本文件按行读取完成，路径：{}，行数：{}", filePath, lines.size());
        return lines;
    }

    /**
     * 使用字节缓冲区读取文件，适合二进制文件或大文件
     *
     * @param filePath 文件路径
     * @return 文件字节数
     */
    public static long readByBuffer(String filePath) {
        long totalSize = 0;

        try (BufferedInputStream inputStream = FileUtil.getInputStream(filePath)) {
            byte[] buffer = new byte[8192];
            int length;

            while ((length = inputStream.read(buffer)) != -1) {
                totalSize += length;
            }

            log.info("文件缓冲读取完成，路径：{}，字节数：{}", filePath, totalSize);
            return totalSize;
        } catch (IOException e) {
            log.error("文件缓冲读取失败，路径：{}", filePath, e);
            throw new RuntimeException("文件读取失败", e);
        }
    }
}
```

如果文件较小，可以直接读取完整内容。如果文件较大，尤其是日志文件、导入文件、视频文件、压缩包等，不建议一次性读取到内存，应使用缓冲区分批读取。

### 文件写入方式

文件写入可以分为文本写入和字节写入。文本写入适合保存字符串、JSON、CSV、日志片段等内容；字节写入适合保存图片、PDF、Excel、压缩包、上传文件等二进制内容。

常见写入方式如下：

| 场景               | 推荐方式                           |
| ------------------ | ---------------------------------- |
| 写入字符串         | `FileUtil.writeUtf8String()`       |
| 写入多行文本       | `FileUtil.writeLines()`            |
| 写入字节数组       | `FileUtil.writeBytes()`            |
| 大文件写入         | `OutputStream` 分批写入            |
| 写入 HTTP 下载响应 | `ServletOutputStream` 或响应输出流 |

下面示例演示文本写入、多行写入和字节写入。

文件位置：`src/main/java/io/github/atengk/file/basic/FileWriteExample.java`

```java
package io.github.atengk.file.basic;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.io.FileUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.charset.StandardCharsets;
import java.util.List;

/**
 * 文件写入示例
 *
 * @author Ateng
 * @since 2026-05-13
 */
public class FileWriteExample {

    private static final Logger log = LoggerFactory.getLogger(FileWriteExample.class);

    /**
     * 写入 UTF-8 文本，默认覆盖原文件
     *
     * @param filePath 文件路径
     * @param content  文件内容
     */
    public static void writeText(String filePath, String content) {
        FileUtil.writeUtf8String(content, filePath);
        log.info("文本文件写入完成，路径：{}，字符数：{}", filePath, content.length());
    }

    /**
     * 写入多行文本，默认覆盖原文件
     *
     * @param filePath 文件路径
     * @param lines    文本行
     */
    public static void writeLines(String filePath, List<String> lines) {
        if (CollUtil.isEmpty(lines)) {
            log.warn("写入跳过，文本行为空，路径：{}", filePath);
            return;
        }

        FileUtil.writeLines(lines, filePath, StandardCharsets.UTF_8);
        log.info("多行文本写入完成，路径：{}，行数：{}", filePath, lines.size());
    }

    /**
     * 写入字节数组，适合二进制文件
     *
     * @param filePath 文件路径
     * @param bytes    字节内容
     */
    public static void writeBytes(String filePath, byte[] bytes) {
        if (bytes == null || bytes.length == 0) {
            log.warn("写入跳过，字节内容为空，路径：{}", filePath);
            return;
        }

        FileUtil.writeBytes(bytes, filePath);
        log.info("字节文件写入完成，路径：{}，字节数：{}", filePath, bytes.length);
    }
}
```

写入文件时需要关注父级目录是否存在、目标文件是否允许覆盖、文件编码是否一致、写入失败时是否需要回滚。对于重要业务文件，可以先写入临时文件，校验成功后再重命名为正式文件，减少写入中断导致的文件损坏问题。

### 追加写入与覆盖写入

覆盖写入表示新内容会替换原文件内容。追加写入表示新内容会写入到文件末尾，原文件内容保留。

两者区别如下：

| 写入方式 | 行为                   | 常见场景                         |
| -------- | ---------------------- | -------------------------------- |
| 覆盖写入 | 删除原内容，写入新内容 | 导出文件、生成配置、重新生成报表 |
| 追加写入 | 保留原内容，在末尾追加 | 日志记录、操作轨迹、批量追加数据 |

Hutool 中，`FileUtil.writeUtf8String()` 默认是覆盖写入，`FileUtil.appendUtf8String()` 是追加写入。Java 原生 `FileOutputStream` 构造方法的第二个参数可以控制是否追加，例如 `new FileOutputStream(file, true)` 表示追加写入。

下面示例演示覆盖写入和追加写入。

文件位置：`src/main/java/io/github/atengk/file/basic/FileAppendOverwriteExample.java`

```java
package io.github.atengk.file.basic;

import cn.hutool.core.io.FileUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * 追加写入与覆盖写入示例
 *
 * @author Ateng
 * @since 2026-05-13
 */
public class FileAppendOverwriteExample {

    private static final Logger log = LoggerFactory.getLogger(FileAppendOverwriteExample.class);

    /**
     * 覆盖写入文件
     *
     * @param filePath 文件路径
     * @param content  文件内容
     */
    public static void overwrite(String filePath, String content) {
        FileUtil.writeUtf8String(content, filePath);
        log.info("文件覆盖写入完成，路径：{}", filePath);
    }

    /**
     * 追加写入文件
     *
     * @param filePath 文件路径
     * @param content  追加内容
     */
    public static void append(String filePath, String content) {
        FileUtil.appendUtf8String(content, filePath);
        log.info("文件追加写入完成，路径：{}", filePath);
    }
}
```

实际开发中，覆盖写入要格外谨慎。如果目标文件是用户上传原始文件、业务凭证、历史记录或审计日志，不应直接覆盖。追加写入适合简单日志或轨迹记录，但高并发场景下需要考虑并发写入冲突、文件锁、日志框架或队列异步写入。

## 流的使用与关闭

本节主要说明 Java 输入输出流的使用方式和关闭原则。流对象通常持有底层系统资源，例如文件句柄、网络连接、缓冲区等。如果使用后不关闭，可能造成资源泄漏、文件被占用、数据未完全写入等问题。

### InputStream 与 OutputStream

`InputStream` 和 `OutputStream` 是字节输入流和字节输出流的顶层抽象，适合处理二进制数据。文件上传、文件下载、图片处理、PDF 处理、Excel 导入导出等场景通常都使用字节流。

`InputStream` 用于读取字节数据，常见实现类包括 `FileInputStream`、`BufferedInputStream`、`ByteArrayInputStream`。`OutputStream` 用于写出字节数据，常见实现类包括 `FileOutputStream`、`BufferedOutputStream`、`ByteArrayOutputStream`。

下面示例演示使用字节流复制文件。

文件位置：`src/main/java/io/github/atengk/file/stream/ByteStreamExample.java`

```java
package io.github.atengk.file.stream;

import cn.hutool.core.io.FileUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.IOException;

/**
 * 字节流使用示例
 *
 * @author Ateng
 * @since 2026-05-13
 */
public class ByteStreamExample {

    private static final Logger log = LoggerFactory.getLogger(ByteStreamExample.class);

    /**
     * 使用字节流复制文件
     *
     * @param sourcePath 源文件路径
     * @param targetPath 目标文件路径
     */
    public static void copy(String sourcePath, String targetPath) {
        try (
                BufferedInputStream inputStream = FileUtil.getInputStream(sourcePath);
                BufferedOutputStream outputStream = FileUtil.getOutputStream(targetPath)
        ) {
            byte[] buffer = new byte[8192];
            int length;

            while ((length = inputStream.read(buffer)) != -1) {
                outputStream.write(buffer, 0, length);
            }

            outputStream.flush();
            log.info("字节流复制完成，源文件：{}，目标文件：{}", sourcePath, targetPath);
        } catch (IOException e) {
            log.error("字节流复制失败，源文件：{}，目标文件：{}", sourcePath, targetPath, e);
            throw new RuntimeException("文件复制失败", e);
        }
    }
}
```

字节流不关心字符编码，适合原样读写文件内容。只要不需要解析文本含义，优先使用字节流更加通用。

### Reader 与 Writer

`Reader` 和 `Writer` 是字符输入流和字符输出流的顶层抽象，适合处理文本内容。它们会根据字符集进行编码和解码，因此在读取中文、特殊符号、跨平台文本文件时，应明确指定字符编码。

常见字符输入类包括 `FileReader`、`BufferedReader`、`InputStreamReader`。常见字符输出类包括 `FileWriter`、`BufferedWriter`、`OutputStreamWriter`。

下面示例演示使用字符流按行读取文本，并写入到另一个文件中。

文件位置：`src/main/java/io/github/atengk/file/stream/CharStreamExample.java`

```java
package io.github.atengk.file.stream;

import cn.hutool.core.io.FileUtil;
import cn.hutool.core.util.CharsetUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;

/**
 * 字符流使用示例
 *
 * @author Ateng
 * @since 2026-05-13
 */
public class CharStreamExample {

    private static final Logger log = LoggerFactory.getLogger(CharStreamExample.class);

    /**
     * 使用字符流复制文本文件
     *
     * @param sourcePath 源文件路径
     * @param targetPath 目标文件路径
     */
    public static void copyText(String sourcePath, String targetPath) {
        try (
                BufferedReader reader = FileUtil.getReader(sourcePath, CharsetUtil.CHARSET_UTF_8);
                BufferedWriter writer = FileUtil.getWriter(targetPath, CharsetUtil.CHARSET_UTF_8, false)
        ) {
            String line;
            long lineCount = 0;

            while ((line = reader.readLine()) != null) {
                writer.write(line);
                writer.newLine();
                lineCount++;
            }

            writer.flush();
            log.info("字符流复制完成，源文件：{}，目标文件：{}，行数：{}", sourcePath, targetPath, lineCount);
        } catch (IOException e) {
            log.error("字符流复制失败，源文件：{}，目标文件：{}", sourcePath, targetPath, e);
            throw new RuntimeException("文本文件复制失败", e);
        }
    }
}
```

字符流适合处理文本，但不适合处理图片、视频、PDF、Excel 等二进制文件。二进制文件如果误用字符流处理，可能导致文件损坏。

### try-with-resources 自动关闭

`try-with-resources` 是 Java 7 引入的资源自动关闭语法。只要资源对象实现了 `AutoCloseable` 或 `Closeable` 接口，就可以放在 `try` 后面的括号中，代码块执行结束后会自动调用 `close()` 方法。

输入流、输出流、Reader、Writer、文件通道、数据库连接等资源都可以使用 `try-with-resources` 管理。

推荐写法如下：

```java
try (
        InputStream inputStream = FileUtil.getInputStream(sourcePath);
        OutputStream outputStream = FileUtil.getOutputStream(targetPath)
) {
    // 使用资源
}
```

不推荐写法如下：

```java
InputStream inputStream = null;
try {
    inputStream = FileUtil.getInputStream(sourcePath);
    // 使用资源
} finally {
    if (inputStream != null) {
        inputStream.close();
    }
}
```

`try-with-resources` 的优势是代码更简洁，而且多个资源会按照声明的相反顺序自动关闭。例如先声明输入流，再声明输出流，关闭时会先关闭输出流，再关闭输入流。

下面示例演示多个资源的自动关闭。

文件位置：`src/main/java/io/github/atengk/file/stream/TryWithResourcesExample.java`

```java
package io.github.atengk.file.stream;

import cn.hutool.core.io.FileUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.InputStream;
import java.io.OutputStream;

/**
 * try-with-resources 自动关闭示例
 *
 * @author Ateng
 * @since 2026-05-13
 */
public class TryWithResourcesExample {

    private static final Logger log = LoggerFactory.getLogger(TryWithResourcesExample.class);

    /**
     * 使用 try-with-resources 自动关闭输入流和输出流
     *
     * @param sourcePath 源文件路径
     * @param targetPath 目标文件路径
     */
    public static void copy(String sourcePath, String targetPath) {
        try (
                InputStream inputStream = FileUtil.getInputStream(sourcePath);
                OutputStream outputStream = FileUtil.getOutputStream(targetPath)
        ) {
            inputStream.transferTo(outputStream);
            outputStream.flush();
            log.info("自动关闭资源复制完成，源文件：{}，目标文件：{}", sourcePath, targetPath);
        } catch (Exception e) {
            log.error("自动关闭资源复制失败，源文件：{}，目标文件：{}", sourcePath, targetPath, e);
            throw new RuntimeException("文件复制失败", e);
        }
    }
}
```

项目开发中，只要手动创建了流对象，优先使用 `try-with-resources`。除非资源生命周期需要跨方法、跨对象管理，否则不建议依赖手动 `close()`。

### flush 与 close 的区别

`flush()` 和 `close()` 都常见于输出流或写入器中，但含义不同。

`flush()` 表示刷新缓冲区，将缓冲区中尚未写出的数据推送到底层目标，例如文件、网络连接、HTTP 响应等。调用 `flush()` 后，流仍然可以继续使用。

`close()` 表示关闭流对象，释放底层资源。多数输出流在关闭前会自动执行一次 `flush()`，但关闭后流不能继续使用。

| 方法      | 作用             | 调用后是否还能继续写入 | 是否释放资源 |
| --------- | ---------------- | ---------------------- | ------------ |
| `flush()` | 刷新缓冲区       | 可以                   | 不释放       |
| `close()` | 关闭流并释放资源 | 不可以                 | 释放         |

适合调用 `flush()` 的场景包括：

1. 文件写入过程中需要确保阶段性内容落盘。
2. HTTP 响应需要尽快把数据发送给客户端。
3. 长时间写入任务中需要定期刷新缓冲区。
4. 使用 `BufferedWriter`、`BufferedOutputStream` 等缓冲流时，需要明确推送缓冲数据。

适合调用 `close()` 的场景包括：

1. 文件处理完成。
2. 网络连接使用结束。
3. 上传、下载、导入、导出任务结束。
4. 资源对象不再需要继续使用。

下面示例演示 `flush()` 后继续写入，以及 `close()` 后不再使用流对象。

```java
try (BufferedWriter writer = FileUtil.getWriter("logs/demo.log", CharsetUtil.CHARSET_UTF_8, true)) {
    writer.write("第一行日志");
    writer.newLine();
    writer.flush();

    writer.write("第二行日志");
    writer.newLine();
    writer.flush();
}
```

在 `try-with-resources` 中不需要手动调用 `close()`，代码块结束后会自动关闭。但对于输出流，如果业务上要求立即写出数据，可以在关键位置手动调用 `flush()`。

### 常见资源泄漏问题

资源泄漏指程序申请了系统资源，但使用完成后没有及时释放。文件 IO 中常见泄漏对象包括输入流、输出流、Reader、Writer、文件通道、目录流等。

常见问题如下：

| 问题                 | 后果                         | 处理方式                                   |
| -------------------- | ---------------------------- | ------------------------------------------ |
| 输入流未关闭         | 文件句柄泄漏，文件长期被占用 | 使用 `try-with-resources`                  |
| 输出流未关闭         | 数据可能未完整写入，文件损坏 | 使用 `try-with-resources` 并关注 `flush()` |
| 异常分支未关闭资源   | 失败场景下资源泄漏           | 避免手动关闭，统一自动关闭                 |
| 大文件一次性读入内存 | 内存占用过高，甚至 OOM       | 使用缓冲区分批读取                         |
| 目录流未关闭         | 目录句柄泄漏                 | 使用 `try-with-resources`                  |
| 静态集合缓存文件内容 | 内存泄漏或旧数据长期占用     | 控制缓存大小和生命周期                     |

不推荐写法如下：

```java
InputStream inputStream = FileUtil.getInputStream("data/source.dat");
byte[] bytes = inputStream.readAllBytes();
```

上面代码没有关闭输入流，如果方法频繁调用，可能造成文件句柄泄漏。

推荐写法如下：

```java
try (InputStream inputStream = FileUtil.getInputStream("data/source.dat")) {
    byte[] bytes = inputStream.readAllBytes();
}
```

对于大文件，不推荐使用 `readAllBytes()`，应使用缓冲区分批读取。

```java
try (InputStream inputStream = FileUtil.getInputStream("data/big-file.dat")) {
    byte[] buffer = new byte[8192];
    int length;
    while ((length = inputStream.read(buffer)) != -1) {
        // 分批处理读取到的数据，避免一次性加载整个文件
    }
}
```

实际项目中可以遵循以下原则减少资源泄漏：

1. 所有流对象优先使用 `try-with-resources`。
2. 不要把流对象作为方法返回值随意传递，除非明确由调用方负责关闭。
3. 大文件不要一次性读取到内存。
4. 输出流写入完成后关注 `flush()` 和 `close()`。
5. 异常分支中不要遗漏资源释放。
6. 文件删除、移动、重命名前，确保相关流已经关闭。
7. Web 下载接口中，不要提前关闭由容器管理的响应对象，但自己创建的输入流必须关闭。



## 缓冲流使用

缓冲流是在普通输入输出流外层增加缓冲区的一类包装流。它可以减少程序与磁盘、网络等底层资源的直接交互次数，从而提升读写效率。文件处理时，缓冲流通常是默认推荐写法，尤其适合文件复制、批量读取、大文件分块处理和文本按行处理等场景。

### BufferedInputStream 与 BufferedOutputStream

`BufferedInputStream` 和 `BufferedOutputStream` 是字节缓冲流，适合处理图片、视频、压缩包、PDF、Excel、二进制文件等内容。它们本质上仍然是字节流，只是在内部维护了一个缓冲区，避免每次读取或写入都直接访问底层文件。

常见使用场景如下：

| 类                     | 作用             | 常见场景                         |
| ---------------------- | ---------------- | -------------------------------- |
| `BufferedInputStream`  | 缓冲读取字节数据 | 读取图片、视频、压缩包、上传文件 |
| `BufferedOutputStream` | 缓冲写出字节数据 | 保存文件、复制文件、写出下载内容 |

下面示例使用字节缓冲流复制文件，适合普通文件和二进制文件处理。

文件位置：`src/main/java/io/github/atengk/file/buffer/BufferedByteStreamExample.java`

```java
package io.github.atengk.file.buffer;

import cn.hutool.core.io.FileUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.IOException;

/**
 * 字节缓冲流示例
 *
 * @author Ateng
 * @since 2026-05-13
 */
public class BufferedByteStreamExample {

    private static final Logger log = LoggerFactory.getLogger(BufferedByteStreamExample.class);

    /**
     * 使用字节缓冲流复制文件
     *
     * @param sourcePath 源文件路径
     * @param targetPath 目标文件路径
     */
    public static void copy(String sourcePath, String targetPath) {
        try (
                BufferedInputStream inputStream = FileUtil.getInputStream(sourcePath);
                BufferedOutputStream outputStream = FileUtil.getOutputStream(targetPath)
        ) {
            byte[] buffer = new byte[8192];
            int length;

            while ((length = inputStream.read(buffer)) != -1) {
                outputStream.write(buffer, 0, length);
            }

            outputStream.flush();
            log.info("字节缓冲流复制完成，源文件：{}，目标文件：{}", sourcePath, targetPath);
        } catch (IOException e) {
            log.error("字节缓冲流复制失败，源文件：{}，目标文件：{}", sourcePath, targetPath, e);
            throw new RuntimeException("文件复制失败", e);
        }
    }
}
```

字节缓冲流不关心字符编码，因此不适合直接解析文本内容。如果需要按行读取文本，应使用 `BufferedReader`。

### BufferedReader 与 BufferedWriter

`BufferedReader` 和 `BufferedWriter` 是字符缓冲流，适合处理文本文件。它们通常用于按行读取日志、CSV、SQL、TXT、JSONL 等文本内容，也可以用于逐行写入文本文件。

`BufferedReader` 常用方法是 `readLine()`，可以每次读取一行文本。`BufferedWriter` 常用方法是 `write()` 和 `newLine()`，可以写入文本并按系统换行符换行。

下面示例使用字符缓冲流读取文本并写入另一个文件。

文件位置：`src/main/java/io/github/atengk/file/buffer/BufferedCharStreamExample.java`

```java
package io.github.atengk.file.buffer;

import cn.hutool.core.io.FileUtil;
import cn.hutool.core.util.CharsetUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;

/**
 * 字符缓冲流示例
 *
 * @author Ateng
 * @since 2026-05-13
 */
public class BufferedCharStreamExample {

    private static final Logger log = LoggerFactory.getLogger(BufferedCharStreamExample.class);

    /**
     * 使用字符缓冲流复制文本文件
     *
     * @param sourcePath 源文件路径
     * @param targetPath 目标文件路径
     */
    public static void copyText(String sourcePath, String targetPath) {
        try (
                BufferedReader reader = FileUtil.getReader(sourcePath, CharsetUtil.CHARSET_UTF_8);
                BufferedWriter writer = FileUtil.getWriter(targetPath, CharsetUtil.CHARSET_UTF_8, false)
        ) {
            String line;
            long lineCount = 0;

            while ((line = reader.readLine()) != null) {
                writer.write(line);
                writer.newLine();
                lineCount++;
            }

            writer.flush();
            log.info("字符缓冲流复制完成，源文件：{}，目标文件：{}，行数：{}", sourcePath, targetPath, lineCount);
        } catch (IOException e) {
            log.error("字符缓冲流复制失败，源文件：{}，目标文件：{}", sourcePath, targetPath, e);
            throw new RuntimeException("文本文件复制失败", e);
        }
    }
}
```

字符缓冲流只适合文本文件。不要使用 `BufferedReader` 或 `BufferedWriter` 处理图片、PDF、Excel、压缩包等二进制文件，否则可能导致文件损坏。

### 缓冲区大小选择

缓冲区大小会影响文件读写效率。缓冲区过小会导致读写次数过多，缓冲区过大则可能造成不必要的内存占用。普通文件处理通常使用 `8192` 字节，也就是 8KB，这是一个常见且稳定的默认值。

常见建议如下：

| 场景           | 推荐缓冲区大小                         |
| -------------- | -------------------------------------- |
| 普通文件复制   | 8KB 或 16KB                            |
| 小文件读取     | 4KB 或 8KB                             |
| 大文件复制     | 64KB 或 128KB                          |
| 网络文件传输   | 8KB 到 64KB                            |
| 高并发文件处理 | 不宜过大，避免线程过多导致内存占用升高 |

缓冲区大小不是越大越好。实际项目中，建议从 8KB 或 16KB 开始，只有在大文件批量处理或性能压测发现瓶颈时，再调整为 64KB、128KB 等更大的缓冲区。

下面示例将缓冲区大小抽取为常量，便于统一调整。

文件位置：`src/main/java/io/github/atengk/file/buffer/BufferSizeExample.java`

```java
package io.github.atengk.file.buffer;

import cn.hutool.core.io.FileUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.IOException;

/**
 * 缓冲区大小示例
 *
 * @author Ateng
 * @since 2026-05-13
 */
public class BufferSizeExample {

    private static final Logger log = LoggerFactory.getLogger(BufferSizeExample.class);

    /**
     * 普通文件处理推荐缓冲区大小
     */
    private static final int DEFAULT_BUFFER_SIZE = 8192;

    /**
     * 大文件处理缓冲区大小
     */
    private static final int LARGE_FILE_BUFFER_SIZE = 1024 * 64;

    /**
     * 使用指定缓冲区复制文件
     *
     * @param sourcePath 源文件路径
     * @param targetPath 目标文件路径
     * @param largeFile  是否按大文件处理
     */
    public static void copyWithBufferSize(String sourcePath, String targetPath, boolean largeFile) {
        int bufferSize = largeFile ? LARGE_FILE_BUFFER_SIZE : DEFAULT_BUFFER_SIZE;

        try (
                BufferedInputStream inputStream = FileUtil.getInputStream(sourcePath);
                BufferedOutputStream outputStream = FileUtil.getOutputStream(targetPath)
        ) {
            byte[] buffer = new byte[bufferSize];
            int length;

            while ((length = inputStream.read(buffer)) != -1) {
                outputStream.write(buffer, 0, length);
            }

            outputStream.flush();
            log.info("文件复制完成，源文件：{}，目标文件：{}，缓冲区大小：{}", sourcePath, targetPath, bufferSize);
        } catch (IOException e) {
            log.error("文件复制失败，源文件：{}，目标文件：{}", sourcePath, targetPath, e);
            throw new RuntimeException("文件复制失败", e);
        }
    }
}
```

如果文件处理任务并发量较高，需要同时关注缓冲区大小和线程数量。例如 100 个线程同时使用 1MB 缓冲区，理论上仅缓冲区就会占用约 100MB 内存。

### 按行读取文件

按行读取是文本文件处理中最常见的方式，适合处理日志文件、CSV 文件、SQL 文件、文本导入文件等。按行读取可以避免一次性把整个文件加载到内存中，适合大部分文本解析场景。

常见按行读取方式如下：

| 方式                        | 特点                              |
| --------------------------- | --------------------------------- |
| `BufferedReader.readLine()` | 标准写法，适合逐行处理            |
| `FileUtil.readLines()`      | Hutool 简化写法，适合小到中等文件 |
| `Files.lines()`             | 返回 Stream，需要注意关闭         |
| `LineNumberReader`          | 需要行号时使用                    |

下面示例按行读取文件，并对每一行进行简单过滤。

文件位置：`src/main/java/io/github/atengk/file/buffer/ReadLineExample.java`

```java
package io.github.atengk.file.buffer;

import cn.hutool.core.io.FileUtil;
import cn.hutool.core.text.StrPool;
import cn.hutool.core.util.CharsetUtil;
import cn.hutool.core.util.StrUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * 按行读取文件示例
 *
 * @author Ateng
 * @since 2026-05-13
 */
public class ReadLineExample {

    private static final Logger log = LoggerFactory.getLogger(ReadLineExample.class);

    /**
     * 按行读取有效文本，忽略空行和注释行
     *
     * @param filePath 文件路径
     * @return 有效文本行
     */
    public static List<String> readValidLines(String filePath) {
        List<String> result = new ArrayList<>();

        try (BufferedReader reader = FileUtil.getReader(filePath, CharsetUtil.CHARSET_UTF_8)) {
            String line;
            long lineNumber = 0;

            while ((line = reader.readLine()) != null) {
                lineNumber++;

                if (StrUtil.isBlank(line) || StrUtil.startWith(line.trim(), StrPool.HASH)) {
                    continue;
                }

                result.add(line);
            }

            log.info("按行读取完成，文件：{}，有效行数：{}，总读取行数：{}", filePath, result.size(), lineNumber);
            return result;
        } catch (IOException e) {
            log.error("按行读取失败，文件：{}", filePath, e);
            throw new RuntimeException("按行读取文件失败", e);
        }
    }
}
```

如果文件非常大，不建议把有效行全部放入 `List` 返回。更推荐边读边处理，例如逐行写入数据库、逐行校验、逐行转换后输出。

### 大文件读写处理

大文件处理的核心原则是分批读取、分批写入，避免一次性加载到内存。常见大文件包括日志文件、视频文件、数据库导出文件、压缩包、大型 CSV、Excel 导出结果等。

大文件处理时应注意以下问题：

1. 不要使用 `readAllBytes()` 读取大文件。
2. 不要使用 `FileUtil.readUtf8String()` 读取超大文本。
3. 优先使用缓冲区分批处理。
4. 文本大文件优先按行读取。
5. 二进制大文件优先使用字节缓冲流或 NIO。
6. 需要记录处理进度时，可以统计已处理字节数或行数。
7. 失败重试时，应考虑临时文件和断点位置。

下面示例演示大文件分批复制，并记录处理进度。

文件位置：`src/main/java/io/github/atengk/file/buffer/LargeFileExample.java`

```java
package io.github.atengk.file.buffer;

import cn.hutool.core.io.FileUtil;
import cn.hutool.core.util.NumberUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.IOException;

/**
 * 大文件处理示例
 *
 * @author Ateng
 * @since 2026-05-13
 */
public class LargeFileExample {

    private static final Logger log = LoggerFactory.getLogger(LargeFileExample.class);

    /**
     * 大文件缓冲区大小
     */
    private static final int BUFFER_SIZE = 1024 * 64;

    /**
     * 分批复制大文件
     *
     * @param sourcePath 源文件路径
     * @param targetPath 目标文件路径
     */
    public static void copyLargeFile(String sourcePath, String targetPath) {
        long fileSize = FileUtil.size(FileUtil.file(sourcePath));
        long copiedSize = 0;

        try (
                BufferedInputStream inputStream = FileUtil.getInputStream(sourcePath);
                BufferedOutputStream outputStream = FileUtil.getOutputStream(targetPath)
        ) {
            byte[] buffer = new byte[BUFFER_SIZE];
            int length;

            while ((length = inputStream.read(buffer)) != -1) {
                outputStream.write(buffer, 0, length);
                copiedSize += length;

                if (fileSize > 0) {
                    double progress = NumberUtil.mul(NumberUtil.div(copiedSize, fileSize, 4), 100);
                    log.debug("大文件复制进度，源文件：{}，进度：{}%", sourcePath, NumberUtil.roundStr(progress, 2));
                }
            }

            outputStream.flush();
            log.info("大文件复制完成，源文件：{}，目标文件：{}，大小：{} 字节", sourcePath, targetPath, fileSize);
        } catch (IOException e) {
            log.error("大文件复制失败，源文件：{}，目标文件：{}", sourcePath, targetPath, e);
            throw new RuntimeException("大文件复制失败", e);
        }
    }
}
```

大文件处理通常不建议在 Web 请求线程中长时间执行。对于耗时任务，可以使用异步线程池、消息队列、批处理任务或任务表记录状态，避免接口长时间阻塞。

## 常用文件操作场景

本节主要整理 Java 项目开发中常见的文件操作场景，包括文件复制、移动与重命名、内容追加、编码处理和临时文件处理。这些操作在文件上传下载、批量导入导出、报表生成、日志处理和对象存储中非常常见。

### 文件复制

文件复制是最常见的文件操作之一。普通复制可以使用 `FileUtil.copy()` 或 `Files.copy()`，大文件复制可以使用缓冲流或 NIO `FileChannel`。

常见复制方式如下：

| 方式                       | 适用场景           |
| -------------------------- | ------------------ |
| `FileUtil.copy()`          | 普通文件或目录复制 |
| `Files.copy()`             | 标准 NIO 文件复制  |
| 缓冲流复制                 | 需要边读边处理时   |
| `FileChannel.transferTo()` | 大文件高性能复制   |

下面示例使用 Hutool 完成普通文件复制。

文件位置：`src/main/java/io/github/atengk/file/scene/FileCopyScene.java`

```java
package io.github.atengk.file.scene;

import cn.hutool.core.io.FileUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;

/**
 * 文件复制场景示例
 *
 * @author Ateng
 * @since 2026-05-13
 */
public class FileCopyScene {

    private static final Logger log = LoggerFactory.getLogger(FileCopyScene.class);

    /**
     * 复制文件，目标文件存在时覆盖
     *
     * @param sourcePath 源文件路径
     * @param targetPath 目标文件路径
     */
    public static void copyFile(String sourcePath, String targetPath) {
        File sourceFile = FileUtil.file(sourcePath);
        File targetFile = FileUtil.file(targetPath);

        if (!FileUtil.exist(sourceFile)) {
            log.warn("文件复制跳过，源文件不存在：{}", sourcePath);
            return;
        }

        FileUtil.copy(sourceFile, targetFile, true);
        log.info("文件复制完成，源文件：{}，目标文件：{}", sourcePath, targetPath);
    }
}
```

如果复制的是用户上传文件，需要校验文件大小、文件类型和目标路径，避免覆盖重要文件或产生路径穿越问题。

### 文件移动与重命名

文件移动和重命名本质上都是改变文件路径。移动通常表示从一个目录转移到另一个目录，重命名通常表示父目录不变，只修改文件名称。

Hutool 可以使用 `FileUtil.move()`，NIO 可以使用 `Files.move()`。如果目标路径和源路径在同一个目录下，移动操作就相当于重命名。

下面示例演示文件移动和重命名。

文件位置：`src/main/java/io/github/atengk/file/scene/FileMoveRenameScene.java`

```java
package io.github.atengk.file.scene;

import cn.hutool.core.io.FileUtil;
import cn.hutool.core.util.StrUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;

/**
 * 文件移动与重命名场景示例
 *
 * @author Ateng
 * @since 2026-05-13
 */
public class FileMoveRenameScene {

    private static final Logger log = LoggerFactory.getLogger(FileMoveRenameScene.class);

    /**
     * 移动文件，目标存在时覆盖
     *
     * @param sourcePath 源文件路径
     * @param targetPath 目标文件路径
     */
    public static void moveFile(String sourcePath, String targetPath) {
        File sourceFile = FileUtil.file(sourcePath);
        File targetFile = FileUtil.file(targetPath);

        if (!FileUtil.exist(sourceFile)) {
            log.warn("文件移动跳过，源文件不存在：{}", sourcePath);
            return;
        }

        FileUtil.move(sourceFile, targetFile, true);
        log.info("文件移动完成，源文件：{}，目标文件：{}", sourcePath, targetPath);
    }

    /**
     * 在原目录下重命名文件
     *
     * @param sourcePath 源文件路径
     * @param newFileName 新文件名
     */
    public static void renameFile(String sourcePath, String newFileName) {
        if (StrUtil.isBlank(newFileName)) {
            throw new IllegalArgumentException("新文件名不能为空");
        }

        File sourceFile = FileUtil.file(sourcePath);

        if (!FileUtil.exist(sourceFile)) {
            log.warn("文件重命名跳过，源文件不存在：{}", sourcePath);
            return;
        }

        File renamedFile = FileUtil.rename(sourceFile, newFileName, true);
        log.info("文件重命名完成，原文件：{}，新文件：{}", sourcePath, renamedFile.getAbsolutePath());
    }
}
```

移动文件时，如果目标文件已经存在，需要明确是覆盖、跳过还是报错。业务文件通常不建议静默覆盖，应记录日志并保留操作记录。

### 文件内容追加

文件内容追加适合保存日志、审计记录、导入错误信息、批处理结果等。追加写入会保留原文件内容，并把新内容写到文件末尾。

Hutool 中常用 `FileUtil.appendUtf8String()` 或 `FileUtil.appendLines()`。如果使用原生 API，可以通过 `new FileOutputStream(file, true)` 或 `new FileWriter(file, true)` 开启追加模式。

下面示例演示追加写入文本内容。

文件位置：`src/main/java/io/github/atengk/file/scene/FileAppendScene.java`

```java
package io.github.atengk.file.scene;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.io.FileUtil;
import cn.hutool.core.util.CharsetUtil;
import cn.hutool.core.util.StrUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

/**
 * 文件内容追加场景示例
 *
 * @author Ateng
 * @since 2026-05-13
 */
public class FileAppendScene {

    private static final Logger log = LoggerFactory.getLogger(FileAppendScene.class);

    /**
     * 追加单行文本
     *
     * @param filePath 文件路径
     * @param content  追加内容
     */
    public static void appendText(String filePath, String content) {
        if (StrUtil.isBlank(content)) {
            log.warn("追加写入跳过，内容为空，文件：{}", filePath);
            return;
        }

        FileUtil.appendUtf8String(content + System.lineSeparator(), filePath);
        log.info("文本追加完成，文件：{}", filePath);
    }

    /**
     * 批量追加多行文本
     *
     * @param filePath 文件路径
     * @param lines    文本行列表
     */
    public static void appendLines(String filePath, List<String> lines) {
        if (CollUtil.isEmpty(lines)) {
            log.warn("批量追加跳过，文本行为空，文件：{}", filePath);
            return;
        }

        FileUtil.appendLines(lines, filePath, CharsetUtil.CHARSET_UTF_8);
        log.info("多行文本追加完成，文件：{}，行数：{}", filePath, lines.size());
    }
}
```

高并发场景下不建议多个线程同时直接追加同一个文件，可能出现内容交错、写入顺序不稳定等问题。日志类内容应优先使用成熟日志框架，例如 Logback、Log4j2，业务审计类内容可以考虑写入数据库或消息队列。

### 文件编码处理

文件编码处理主要出现在文本文件读写场景中。常见问题包括中文乱码、跨平台换行差异、文件实际编码与读取编码不一致等。

常见编码如下：

| 编码             | 说明                                         |
| ---------------- | -------------------------------------------- |
| `UTF-8`          | 推荐默认编码，跨平台兼容性较好               |
| `GBK`            | 常见于部分 Windows 中文环境或历史系统        |
| `ISO-8859-1`     | 西欧字符编码，不适合中文                     |
| `UTF-8 with BOM` | 部分 Windows 工具生成的 UTF-8 文件可能带 BOM |

在项目开发中，应尽量统一使用 `UTF-8`。读取外部系统生成的文件时，需要确认对方编码。如果编码不一致，可能出现中文乱码。

下面示例演示使用指定编码读取和写入文本。

文件位置：`src/main/java/io/github/atengk/file/scene/FileCharsetScene.java`

```java
package io.github.atengk.file.scene;

import cn.hutool.core.io.FileUtil;
import cn.hutool.core.util.CharsetUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.charset.Charset;

/**
 * 文件编码处理场景示例
 *
 * @author Ateng
 * @since 2026-05-13
 */
public class FileCharsetScene {

    private static final Logger log = LoggerFactory.getLogger(FileCharsetScene.class);

    /**
     * 使用指定编码读取文本
     *
     * @param filePath 文件路径
     * @param charset  字符编码
     * @return 文件内容
     */
    public static String readText(String filePath, Charset charset) {
        String content = FileUtil.readString(filePath, charset);
        log.info("指定编码读取完成，文件：{}，编码：{}，字符数：{}", filePath, charset, content.length());
        return content;
    }

    /**
     * 使用指定编码写入文本
     *
     * @param filePath 文件路径
     * @param content  文件内容
     * @param charset  字符编码
     */
    public static void writeText(String filePath, String content, Charset charset) {
        FileUtil.writeString(content, filePath, charset);
        log.info("指定编码写入完成，文件：{}，编码：{}，字符数：{}", filePath, charset, content.length());
    }

    /**
     * 将 GBK 文本转换为 UTF-8 文本
     *
     * @param sourcePath GBK 源文件路径
     * @param targetPath UTF-8 目标文件路径
     */
    public static void convertGbkToUtf8(String sourcePath, String targetPath) {
        String content = FileUtil.readString(sourcePath, CharsetUtil.CHARSET_GBK);
        FileUtil.writeString(content, targetPath, CharsetUtil.CHARSET_UTF_8);
        log.info("文件编码转换完成，源文件：{}，目标文件：{}，编码：GBK -> UTF-8", sourcePath, targetPath);
    }
}
```

如果文件来自用户上传，不能只根据文件扩展名判断编码。对于关键导入文件，可以要求用户按模板导出，或者在导入前做编码检测和错误提示。

### 临时文件处理

临时文件适合处理中间结果，例如文件上传过渡、报表生成中间文件、压缩包打包过程、导入失败明细文件等。临时文件通常不应该长期保存，使用完成后应及时删除。

Java 原生可以使用 `Files.createTempFile()` 创建临时文件，也可以使用 `FileUtil.createTempFile()` 简化处理。临时文件建议放在系统临时目录或项目指定临时目录下，并通过定时任务定期清理。

下面示例演示创建临时文件、写入内容、读取内容并删除。

文件位置：`src/main/java/io/github/atengk/file/scene/TempFileScene.java`

```java
package io.github.atengk.file.scene;

import cn.hutool.core.io.FileUtil;
import cn.hutool.core.util.CharsetUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;

/**
 * 临时文件处理场景示例
 *
 * @author Ateng
 * @since 2026-05-13
 */
public class TempFileScene {

    private static final Logger log = LoggerFactory.getLogger(TempFileScene.class);

    /**
     * 创建并使用临时文件
     *
     * @param content 临时文件内容
     * @return 临时文件内容读取结果
     */
    public static String useTempFile(String content) {
        File tempFile = null;

        try {
            tempFile = FileUtil.createTempFile("ateng-", ".tmp", true);
            FileUtil.writeString(content, tempFile, CharsetUtil.CHARSET_UTF_8);

            String result = FileUtil.readString(tempFile, CharsetUtil.CHARSET_UTF_8);
            log.info("临时文件处理完成，路径：{}", tempFile.getAbsolutePath());
            return result;
        } catch (Exception e) {
            log.error("临时文件处理失败", e);
            throw new RuntimeException("临时文件处理失败", e);
        } finally {
            if (tempFile != null && FileUtil.exist(tempFile)) {
                boolean deleted = FileUtil.del(tempFile);
                log.info("临时文件清理完成，路径：{}，结果：{}", tempFile.getAbsolutePath(), deleted);
            }
        }
    }
}
```

临时文件处理时需要注意以下事项：

1. 临时文件名应避免冲突，可以使用 UUID、时间戳或业务流水号。
2. 临时文件不要放在源码目录或固定公共目录下。
3. 使用完成后应主动删除。
4. 异常场景下也要在 `finally` 中清理。
5. 大批量临时文件应配合定时任务清理。
6. 涉及敏感数据的临时文件，应避免长期落盘或被其他用户读取。



## NIO Path 与 Files

本节主要说明 Java NIO 中 `Path`、`Paths` 和 `Files` 的基础用法。相比传统 `File` 类，NIO 提供了更现代的路径表达和文件操作 API，更适合处理文件判断、目录遍历、文件复制、文件移动、权限属性、符号链接等场景。

### Path 的创建与转换

`Path` 用于表示文件系统中的路径，可以表示文件路径，也可以表示目录路径。它本身只是路径抽象，不代表文件一定真实存在。创建 `Path` 后，可以进一步通过 `Files` 判断路径是否存在、是否为文件、是否为目录等。

常见创建方式如下：

| 写法                                    | 说明                         |
| --------------------------------------- | ---------------------------- |
| `Path.of("data/demo.txt")`              | Java 11 之后推荐写法         |
| `Paths.get("data/demo.txt")`            | Java 7 开始支持              |
| `Path.of("data", "upload", "demo.txt")` | 按路径片段创建，跨平台更友好 |
| `file.toPath()`                         | `File` 转换为 `Path`         |
| `path.toFile()`                         | `Path` 转换为 `File`         |

下面示例演示 `Path` 的创建、路径信息读取以及 `File` 和 `Path` 的相互转换。

文件位置：`src/main/java/io/github/atengk/file/nio/PathExample.java`

```java
package io.github.atengk.file.nio;

import cn.hutool.core.io.FileUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.nio.file.Path;

/**
 * Path 基础使用示例
 *
 * @author Ateng
 * @since 2026-05-13
 */
public class PathExample {

    private static final Logger log = LoggerFactory.getLogger(PathExample.class);

    /**
     * 创建并打印 Path 基础信息
     *
     * @param filePath 文件路径
     */
    public static void printPathInfo(String filePath) {
        Path path = Path.of(filePath);

        log.info("原始路径：{}", path);
        log.info("文件名：{}", path.getFileName());
        log.info("父级路径：{}", path.getParent());
        log.info("路径层级数量：{}", path.getNameCount());
        log.info("绝对路径：{}", path.toAbsolutePath());
        log.info("标准化路径：{}", path.normalize());
    }

    /**
     * 演示 File 与 Path 互相转换
     *
     * @param filePath 文件路径
     */
    public static void convertFileAndPath(String filePath) {
        File file = FileUtil.file(filePath);
        Path path = file.toPath();
        File convertedFile = path.toFile();

        log.info("File 转 Path：{}", path);
        log.info("Path 转 File：{}", convertedFile.getAbsolutePath());
    }
}
```

`Path#normalize()` 可以消除路径中的 `.` 和 `..` 等冗余片段，但它不会访问真实文件系统。如果需要得到真实路径，可以使用 `toRealPath()`，不过该方法要求文件必须存在。

### Paths 与 Path 的关系

`Path` 是路径接口，表示一个文件系统路径；`Paths` 是工具类，用于创建 `Path` 对象。早期 Java 版本中通常使用 `Paths.get()` 创建路径，Java 11 之后可以直接使用 `Path.of()`，写法更简洁。

两种写法效果基本一致：

```java
Path path1 = Paths.get("data/demo.txt");
Path path2 = Path.of("data/demo.txt");
```

实际项目中，如果项目使用 Java 11 及以上版本，推荐使用 `Path.of()`。如果需要兼容 Java 8，则使用 `Paths.get()`。

下面示例演示 `Paths.get()` 与 `Path.of()` 的等价用法。

文件位置：`src/main/java/io/github/atengk/file/nio/PathsExample.java`

```java
package io.github.atengk.file.nio;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * Paths 与 Path 关系示例
 *
 * @author Ateng
 * @since 2026-05-13
 */
public class PathsExample {

    private static final Logger log = LoggerFactory.getLogger(PathsExample.class);

    /**
     * 比较 Paths.get 与 Path.of 的创建效果
     */
    public static void compareCreatePath() {
        Path pathByPaths = Paths.get("data", "upload", "demo.txt");
        Path pathByPath = Path.of("data", "upload", "demo.txt");

        log.info("Paths.get 创建路径：{}", pathByPaths);
        log.info("Path.of 创建路径：{}", pathByPath);
        log.info("两个路径是否相等：{}", pathByPaths.equals(pathByPath));
    }
}
```

`Paths` 本身没有表示路径的能力，它只是创建 `Path` 的入口。真正参与路径解析、路径拼接、文件名获取、父目录获取的是 `Path` 对象。

### Files 常用方法

`Files` 是 NIO 中最常用的文件工具类，提供了文件创建、删除、复制、移动、读取、写入、判断、遍历等能力。相比传统 IO，`Files` 的方法更集中，代码也更简洁。

常见方法如下：

| 方法                                        | 说明                 |
| ------------------------------------------- | -------------------- |
| `Files.exists(path)`                        | 判断路径是否存在     |
| `Files.notExists(path)`                     | 判断路径是否不存在   |
| `Files.isRegularFile(path)`                 | 判断是否为普通文件   |
| `Files.isDirectory(path)`                   | 判断是否为目录       |
| `Files.size(path)`                          | 获取文件大小         |
| `Files.createFile(path)`                    | 创建文件             |
| `Files.createDirectories(path)`             | 创建多级目录         |
| `Files.copy(source, target, options)`       | 复制文件             |
| `Files.move(source, target, options)`       | 移动或重命名文件     |
| `Files.deleteIfExists(path)`                | 文件存在时删除       |
| `Files.readString(path)`                    | 读取文本内容         |
| `Files.writeString(path, content, options)` | 写入文本内容         |
| `Files.walk(path)`                          | 递归遍历目录         |
| `Files.list(path)`                          | 遍历当前目录一级内容 |

下面示例演示 `Files` 的常用文件操作。

文件位置：`src/main/java/io/github/atengk/file/nio/FilesBasicExample.java`

```java
package io.github.atengk.file.nio;

import cn.hutool.core.util.StrUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;

/**
 * Files 常用方法示例
 *
 * @author Ateng
 * @since 2026-05-13
 */
public class FilesBasicExample {

    private static final Logger log = LoggerFactory.getLogger(FilesBasicExample.class);

    /**
     * 创建文件和父级目录
     *
     * @param filePath 文件路径
     */
    public static void createFile(String filePath) {
        try {
            Path path = Path.of(filePath);
            Path parent = path.getParent();

            if (parent != null) {
                Files.createDirectories(parent);
            }

            if (Files.notExists(path)) {
                Files.createFile(path);
                log.info("文件创建完成，路径：{}", path.toAbsolutePath());
            } else {
                log.warn("文件创建跳过，文件已存在：{}", path.toAbsolutePath());
            }
        } catch (IOException e) {
            log.error("文件创建失败，路径：{}", filePath, e);
            throw new RuntimeException("文件创建失败", e);
        }
    }

    /**
     * 复制文件，目标文件存在时覆盖
     *
     * @param sourcePath 源文件路径
     * @param targetPath 目标文件路径
     */
    public static void copyFile(String sourcePath, String targetPath) {
        try {
            Path source = Path.of(sourcePath);
            Path target = Path.of(targetPath);

            if (Files.notExists(source)) {
                throw new IllegalArgumentException(StrUtil.format("源文件不存在：{}", sourcePath));
            }

            Path parent = target.getParent();
            if (parent != null) {
                Files.createDirectories(parent);
            }

            Files.copy(source, target, StandardCopyOption.REPLACE_EXISTING);
            log.info("文件复制完成，源文件：{}，目标文件：{}", source, target);
        } catch (IOException e) {
            log.error("文件复制失败，源文件：{}，目标文件：{}", sourcePath, targetPath, e);
            throw new RuntimeException("文件复制失败", e);
        }
    }

    /**
     * 删除文件，如果文件不存在则跳过
     *
     * @param filePath 文件路径
     */
    public static void deleteFile(String filePath) {
        try {
            Path path = Path.of(filePath);
            boolean deleted = Files.deleteIfExists(path);
            log.info("文件删除完成，路径：{}，结果：{}", path, deleted);
        } catch (IOException e) {
            log.error("文件删除失败，路径：{}", filePath, e);
            throw new RuntimeException("文件删除失败", e);
        }
    }
}
```

`Files` 方法大多数会抛出 `IOException`，业务代码中应统一捕获并转换为业务异常或运行时异常。涉及删除、移动、覆盖等操作时，应先明确业务规则，避免误删或误覆盖。

### 文件与目录判断

NIO 中判断文件和目录时，通常使用 `Files.exists()`、`Files.isRegularFile()`、`Files.isDirectory()`。这些方法比传统 `File` 类表达更清晰，也能配合 `LinkOption.NOFOLLOW_LINKS` 控制是否跟随符号链接。

常见判断方法如下：

| 方法                         | 说明       |
| ---------------------------- | ---------- |
| `Files.exists(path)`         | 路径存在   |
| `Files.notExists(path)`      | 路径不存在 |
| `Files.isRegularFile(path)`  | 是普通文件 |
| `Files.isDirectory(path)`    | 是目录     |
| `Files.isReadable(path)`     | 可读       |
| `Files.isWritable(path)`     | 可写       |
| `Files.isExecutable(path)`   | 可执行     |
| `Files.isSymbolicLink(path)` | 是符号链接 |

下面示例演示文件和目录的判断逻辑。

文件位置：`src/main/java/io/github/atengk/file/nio/FileCheckExample.java`

```java
package io.github.atengk.file.nio;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.file.Files;
import java.nio.file.Path;

/**
 * 文件与目录判断示例
 *
 * @author Ateng
 * @since 2026-05-13
 */
public class FileCheckExample {

    private static final Logger log = LoggerFactory.getLogger(FileCheckExample.class);

    /**
     * 检查路径类型和权限
     *
     * @param filePath 文件或目录路径
     */
    public static void checkPath(String filePath) {
        Path path = Path.of(filePath);

        if (Files.notExists(path)) {
            log.warn("路径不存在：{}", path.toAbsolutePath());
            return;
        }

        log.info("是否普通文件：{}", Files.isRegularFile(path));
        log.info("是否目录：{}", Files.isDirectory(path));
        log.info("是否可读：{}", Files.isReadable(path));
        log.info("是否可写：{}", Files.isWritable(path));
        log.info("是否可执行：{}", Files.isExecutable(path));
        log.info("是否符号链接：{}", Files.isSymbolicLink(path));
    }
}
```

需要注意的是，`Files.exists()` 和 `Files.notExists()` 不是完全互斥关系。在权限不足、文件系统异常等场景下，两者都可能返回 `false`。因此关键业务中不要只依赖布尔判断，还应结合异常处理。

### 文件遍历与过滤

NIO 提供了多种目录遍历方式。`Files.list()` 只遍历当前目录一级内容，`Files.walk()` 可以递归遍历多级目录，`Files.find()` 可以在遍历时直接使用过滤条件。

常见遍历方式如下：

| 方法                               | 说明                             |
| ---------------------------------- | -------------------------------- |
| `Files.list(path)`                 | 遍历当前目录一级内容             |
| `Files.walk(path)`                 | 递归遍历目录                     |
| `Files.find(path, depth, matcher)` | 按条件递归查找                   |
| `DirectoryStream`                  | 低层目录流，适合控制资源生命周期 |

`Files.list()`、`Files.walk()`、`Files.find()` 返回的是 `Stream<Path>`，必须关闭。推荐放在 `try-with-resources` 中使用。

下面示例演示递归遍历目录，并过滤指定后缀的文件。

文件位置：`src/main/java/io/github/atengk/file/nio/FileWalkExample.java`

```java
package io.github.atengk.file.nio;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.StrUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.stream.Stream;

/**
 * 文件遍历与过滤示例
 *
 * @author Ateng
 * @since 2026-05-13
 */
public class FileWalkExample {

    private static final Logger log = LoggerFactory.getLogger(FileWalkExample.class);

    /**
     * 递归查找指定后缀文件
     *
     * @param dirPath 目录路径
     * @param suffixList 文件后缀列表，例如 .txt、.log、.csv
     * @return 匹配到的文件路径列表
     */
    public static List<Path> findFilesBySuffix(String dirPath, List<String> suffixList) {
        if (CollUtil.isEmpty(suffixList)) {
            throw new IllegalArgumentException("文件后缀列表不能为空");
        }

        Path root = Path.of(dirPath);

        if (!Files.isDirectory(root)) {
            throw new IllegalArgumentException(StrUtil.format("目录不存在或不是目录：{}", dirPath));
        }

        try (Stream<Path> pathStream = Files.walk(root)) {
            List<Path> result = pathStream
                    .filter(Files::isRegularFile)
                    .filter(path -> suffixList.stream()
                            .anyMatch(suffix -> StrUtil.endWithIgnoreCase(path.getFileName().toString(), suffix)))
                    .toList();

            log.info("文件遍历完成，目录：{}，匹配文件数：{}", dirPath, result.size());
            return result;
        } catch (IOException e) {
            log.error("文件遍历失败，目录：{}", dirPath, e);
            throw new RuntimeException("文件遍历失败", e);
        }
    }
}
```

目录遍历时需要控制扫描范围，避免递归扫描过深导致耗时过长。对于上传目录、日志目录、临时目录等业务目录，建议限制最大层级、文件类型和文件数量。

## NIO 文件读写

本节主要说明 NIO 中常见的文件读写方法，包括一次性文本读写、按行读写、字节流读写、字符缓冲流读写，以及 `StandardOpenOption` 常用选项。实际项目中应根据文件大小、文件类型和是否需要追加、覆盖、创建等行为选择合适 API。

### Files.readString 与 Files.writeString

`Files.readString()` 和 `Files.writeString()` 适合处理中小型文本文件。它们使用简单，适合读取配置片段、模板文件、小型 JSON、SQL 脚本、说明文本等。

默认情况下，`Files.readString()` 使用 `UTF-8` 读取文本。`Files.writeString()` 可以配合 `StandardOpenOption` 控制创建、覆盖、追加等写入行为。

下面示例演示使用 NIO 读取和写入字符串内容。

文件位置：`src/main/java/io/github/atengk/file/nio/NioStringExample.java`

```java
package io.github.atengk.file.nio;

import cn.hutool.core.util.StrUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * NIO 字符串读写示例
 *
 * @author Ateng
 * @since 2026-05-13
 */
public class NioStringExample {

    private static final Logger log = LoggerFactory.getLogger(NioStringExample.class);

    /**
     * 读取 UTF-8 文本内容
     *
     * @param filePath 文件路径
     * @return 文件文本内容
     */
    public static String readString(String filePath) {
        try {
            Path path = Path.of(filePath);

            if (!Files.isRegularFile(path)) {
                throw new IllegalArgumentException(StrUtil.format("文件不存在或不是普通文件：{}", filePath));
            }

            String content = Files.readString(path, StandardCharsets.UTF_8);
            log.info("NIO 文本读取完成，文件：{}，字符数：{}", filePath, content.length());
            return content;
        } catch (IOException e) {
            log.error("NIO 文本读取失败，文件：{}", filePath, e);
            throw new RuntimeException("读取文本文件失败", e);
        }
    }

    /**
     * 覆盖写入 UTF-8 文本内容
     *
     * @param filePath 文件路径
     * @param content 文件内容
     */
    public static void writeString(String filePath, String content) {
        try {
            Path path = Path.of(filePath);
            Path parent = path.getParent();

            if (parent != null) {
                Files.createDirectories(parent);
            }

            Files.writeString(path, content, StandardCharsets.UTF_8);
            log.info("NIO 文本写入完成，文件：{}，字符数：{}", filePath, content.length());
        } catch (IOException e) {
            log.error("NIO 文本写入失败，文件：{}", filePath, e);
            throw new RuntimeException("写入文本文件失败", e);
        }
    }
}
```

该方式不适合读取超大文件。大文件文本处理应优先使用 `Files.newBufferedReader()` 按行读取，避免一次性占用大量内存。

### Files.readAllLines 与 Files.write

`Files.readAllLines()` 可以一次性读取文本文件的所有行，返回 `List<String>`。它适合小型文本文件，不适合超大日志或大型 CSV 文件。`Files.write()` 可以写入字节数组或文本行集合，适合简单文件输出。

常见用法如下：

| 方法                                | 说明                 |
| ----------------------------------- | -------------------- |
| `Files.readAllLines(path, charset)` | 一次性读取全部文本行 |
| `Files.write(path, lines, charset)` | 写入多行文本         |
| `Files.write(path, bytes)`          | 写入字节数组         |

下面示例演示读取全部行、写入多行文本和写入字节数组。

文件位置：`src/main/java/io/github/atengk/file/nio/NioLinesBytesExample.java`

```java
package io.github.atengk.file.nio;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.StrUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

/**
 * NIO 行数据与字节读写示例
 *
 * @author Ateng
 * @since 2026-05-13
 */
public class NioLinesBytesExample {

    private static final Logger log = LoggerFactory.getLogger(NioLinesBytesExample.class);

    /**
     * 一次性读取所有文本行
     *
     * @param filePath 文件路径
     * @return 文本行列表
     */
    public static List<String> readAllLines(String filePath) {
        try {
            Path path = Path.of(filePath);

            if (!Files.isRegularFile(path)) {
                throw new IllegalArgumentException(StrUtil.format("文件不存在或不是普通文件：{}", filePath));
            }

            List<String> lines = Files.readAllLines(path, StandardCharsets.UTF_8);
            log.info("NIO 文本行读取完成，文件：{}，行数：{}", filePath, lines.size());
            return lines;
        } catch (IOException e) {
            log.error("NIO 文本行读取失败，文件：{}", filePath, e);
            throw new RuntimeException("读取文本行失败", e);
        }
    }

    /**
     * 写入多行文本
     *
     * @param filePath 文件路径
     * @param lines 文本行列表
     */
    public static void writeLines(String filePath, List<String> lines) {
        if (CollUtil.isEmpty(lines)) {
            log.warn("文本行写入跳过，内容为空，文件：{}", filePath);
            return;
        }

        try {
            Path path = Path.of(filePath);
            Path parent = path.getParent();

            if (parent != null) {
                Files.createDirectories(parent);
            }

            Files.write(path, lines, StandardCharsets.UTF_8);
            log.info("NIO 多行文本写入完成，文件：{}，行数：{}", filePath, lines.size());
        } catch (IOException e) {
            log.error("NIO 多行文本写入失败，文件：{}", filePath, e);
            throw new RuntimeException("写入文本行失败", e);
        }
    }

    /**
     * 写入字节数组
     *
     * @param filePath 文件路径
     * @param bytes 字节数组
     */
    public static void writeBytes(String filePath, byte[] bytes) {
        if (bytes == null || bytes.length == 0) {
            log.warn("字节写入跳过，内容为空，文件：{}", filePath);
            return;
        }

        try {
            Path path = Path.of(filePath);
            Path parent = path.getParent();

            if (parent != null) {
                Files.createDirectories(parent);
            }

            Files.write(path, bytes);
            log.info("NIO 字节写入完成，文件：{}，字节数：{}", filePath, bytes.length);
        } catch (IOException e) {
            log.error("NIO 字节写入失败，文件：{}", filePath, e);
            throw new RuntimeException("写入字节文件失败", e);
        }
    }
}
```

`readAllLines()` 会把所有行一次性加载到内存中。如果文件较大，应改用 `BufferedReader` 或 `Files.lines()` 流式处理，并注意关闭资源。

### Files.newInputStream 与 Files.newOutputStream

`Files.newInputStream()` 和 `Files.newOutputStream()` 用于创建 NIO 风格的字节输入输出流。它们适合处理二进制文件、上传文件、下载文件、压缩包、图片、PDF、Excel 等内容。

这两个方法本质上仍然返回 `InputStream` 和 `OutputStream`，因此使用方式与传统字节流一致，建议配合缓冲流和 `try-with-resources` 使用。

下面示例使用 NIO 创建字节流并复制文件。

文件位置：`src/main/java/io/github/atengk/file/nio/NioByteStreamExample.java`

```java
package io.github.atengk.file.nio;

import cn.hutool.core.util.StrUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;

/**
 * NIO 字节流读写示例
 *
 * @author Ateng
 * @since 2026-05-13
 */
public class NioByteStreamExample {

    private static final Logger log = LoggerFactory.getLogger(NioByteStreamExample.class);

    /**
     * 使用 NIO 字节流复制文件
     *
     * @param sourcePath 源文件路径
     * @param targetPath 目标文件路径
     */
    public static void copyByStream(String sourcePath, String targetPath) {
        Path source = Path.of(sourcePath);
        Path target = Path.of(targetPath);

        if (!Files.isRegularFile(source)) {
            throw new IllegalArgumentException(StrUtil.format("源文件不存在或不是普通文件：{}", sourcePath));
        }

        try {
            Path parent = target.getParent();
            if (parent != null) {
                Files.createDirectories(parent);
            }

            try (
                    InputStream inputStream = new BufferedInputStream(Files.newInputStream(source, StandardOpenOption.READ));
                    OutputStream outputStream = new BufferedOutputStream(Files.newOutputStream(
                            target,
                            StandardOpenOption.CREATE,
                            StandardOpenOption.TRUNCATE_EXISTING,
                            StandardOpenOption.WRITE
                    ))
            ) {
                byte[] buffer = new byte[8192];
                int length;

                while ((length = inputStream.read(buffer)) != -1) {
                    outputStream.write(buffer, 0, length);
                }

                outputStream.flush();
                log.info("NIO 字节流复制完成，源文件：{}，目标文件：{}", sourcePath, targetPath);
            }
        } catch (IOException e) {
            log.error("NIO 字节流复制失败，源文件：{}，目标文件：{}", sourcePath, targetPath, e);
            throw new RuntimeException("NIO 字节流复制失败", e);
        }
    }
}
```

如果需要追加写入，可以在 `Files.newOutputStream()` 中使用 `StandardOpenOption.APPEND`，同时不要使用 `TRUNCATE_EXISTING`，否则会与追加语义冲突。

### Files.newBufferedReader 与 Files.newBufferedWriter

`Files.newBufferedReader()` 和 `Files.newBufferedWriter()` 用于创建字符缓冲流，适合处理文本文件。相比 `readString()` 和 `readAllLines()`，它们更适合大文件或需要逐行处理的场景。

常见使用场景如下：

| 方法                        | 适合场景                             |
| --------------------------- | ------------------------------------ |
| `Files.newBufferedReader()` | 按行读取日志、CSV、SQL、TXT          |
| `Files.newBufferedWriter()` | 分批写入文本、生成 CSV、写入导出结果 |

下面示例演示按行读取文本，过滤空行后写入目标文件。

文件位置：`src/main/java/io/github/atengk/file/nio/NioBufferedTextExample.java`

```java
package io.github.atengk.file.nio;

import cn.hutool.core.util.StrUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;

/**
 * NIO 字符缓冲读写示例
 *
 * @author Ateng
 * @since 2026-05-13
 */
public class NioBufferedTextExample {

    private static final Logger log = LoggerFactory.getLogger(NioBufferedTextExample.class);

    /**
     * 过滤空行并写入新文件
     *
     * @param sourcePath 源文本文件路径
     * @param targetPath 目标文本文件路径
     */
    public static void copyNonBlankLines(String sourcePath, String targetPath) {
        Path source = Path.of(sourcePath);
        Path target = Path.of(targetPath);

        if (!Files.isRegularFile(source)) {
            throw new IllegalArgumentException(StrUtil.format("源文件不存在或不是普通文件：{}", sourcePath));
        }

        try {
            Path parent = target.getParent();
            if (parent != null) {
                Files.createDirectories(parent);
            }

            try (
                    BufferedReader reader = Files.newBufferedReader(source, StandardCharsets.UTF_8);
                    BufferedWriter writer = Files.newBufferedWriter(
                            target,
                            StandardCharsets.UTF_8,
                            StandardOpenOption.CREATE,
                            StandardOpenOption.TRUNCATE_EXISTING,
                            StandardOpenOption.WRITE
                    )
            ) {
                String line;
                long readCount = 0;
                long writeCount = 0;

                while ((line = reader.readLine()) != null) {
                    readCount++;

                    if (StrUtil.isBlank(line)) {
                        continue;
                    }

                    writer.write(line);
                    writer.newLine();
                    writeCount++;
                }

                writer.flush();
                log.info("NIO 文本过滤写入完成，源文件：{}，目标文件：{}，读取行数：{}，写入行数：{}",
                        sourcePath, targetPath, readCount, writeCount);
            }
        } catch (IOException e) {
            log.error("NIO 文本过滤写入失败，源文件：{}，目标文件：{}", sourcePath, targetPath, e);
            throw new RuntimeException("NIO 文本过滤写入失败", e);
        }
    }
}
```

字符缓冲流要明确指定字符集，推荐使用 `StandardCharsets.UTF_8`。如果文件来自外部系统，应根据实际编码选择 `GBK`、`UTF-8` 或其他字符集。

### StandardOpenOption 常用选项

`StandardOpenOption` 用于控制文件打开方式，常用于 `Files.writeString()`、`Files.write()`、`Files.newOutputStream()`、`Files.newBufferedWriter()` 等方法中。它可以明确指定文件是创建、覆盖、追加、只读还是只写。

常用选项如下：

| 选项                | 说明                             |
| ------------------- | -------------------------------- |
| `READ`              | 以读取方式打开                   |
| `WRITE`             | 以写入方式打开                   |
| `CREATE`            | 文件不存在时创建，存在时直接使用 |
| `CREATE_NEW`        | 创建新文件，如果文件已存在则报错 |
| `TRUNCATE_EXISTING` | 文件存在时清空原内容             |
| `APPEND`            | 追加写入到文件末尾               |
| `DELETE_ON_CLOSE`   | 流关闭时删除文件                 |
| `SYNC`              | 每次写入都同步到存储设备         |
| `DSYNC`             | 每次写入都同步文件内容到存储设备 |

常见组合如下：

| 目标行为                     | 推荐选项                               |
| ---------------------------- | -------------------------------------- |
| 文件不存在则创建，存在则覆盖 | `CREATE`、`TRUNCATE_EXISTING`、`WRITE` |
| 文件不存在则创建，存在则追加 | `CREATE`、`APPEND`                     |
| 只允许创建新文件，存在则失败 | `CREATE_NEW`、`WRITE`                  |
| 读取文件                     | `READ`                                 |
| 写入后尽量同步落盘           | `CREATE`、`WRITE`、`DSYNC`             |

下面示例演示覆盖写入、追加写入和只创建新文件三种常见模式。

文件位置：`src/main/java/io/github/atengk/file/nio/OpenOptionExample.java`

```java
package io.github.atengk.file.nio;

import cn.hutool.core.util.StrUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;

/**
 * StandardOpenOption 使用示例
 *
 * @author Ateng
 * @since 2026-05-13
 */
public class OpenOptionExample {

    private static final Logger log = LoggerFactory.getLogger(OpenOptionExample.class);

    /**
     * 覆盖写入文件
     *
     * @param filePath 文件路径
     * @param content 文件内容
     */
    public static void overwrite(String filePath, String content) {
        writeText(
                filePath,
                content,
                StandardOpenOption.CREATE,
                StandardOpenOption.TRUNCATE_EXISTING,
                StandardOpenOption.WRITE
        );
        log.info("覆盖写入完成，文件：{}", filePath);
    }

    /**
     * 追加写入文件
     *
     * @param filePath 文件路径
     * @param content 文件内容
     */
    public static void append(String filePath, String content) {
        writeText(
                filePath,
                content,
                StandardOpenOption.CREATE,
                StandardOpenOption.APPEND
        );
        log.info("追加写入完成，文件：{}", filePath);
    }

    /**
     * 只创建新文件并写入，如果文件已存在则失败
     *
     * @param filePath 文件路径
     * @param content 文件内容
     */
    public static void createNew(String filePath, String content) {
        writeText(
                filePath,
                content,
                StandardOpenOption.CREATE_NEW,
                StandardOpenOption.WRITE
        );
        log.info("新文件写入完成，文件：{}", filePath);
    }

    /**
     * 按指定打开选项写入文本
     *
     * @param filePath 文件路径
     * @param content 文件内容
     * @param options 打开选项
     */
    private static void writeText(String filePath, String content, StandardOpenOption... options) {
        if (StrUtil.isBlank(content)) {
            log.warn("文件写入跳过，内容为空，文件：{}", filePath);
            return;
        }

        try {
            Path path = Path.of(filePath);
            Path parent = path.getParent();

            if (parent != null) {
                Files.createDirectories(parent);
            }

            Files.writeString(path, content, StandardCharsets.UTF_8, options);
        } catch (IOException e) {
            log.error("文件写入失败，文件：{}", filePath, e);
            throw new RuntimeException("文件写入失败", e);
        }
    }
}
```

使用 `StandardOpenOption` 时要避免互相冲突的选项组合。例如 `APPEND` 表示追加写入，`TRUNCATE_EXISTING` 表示清空原内容，两者不要同时使用。对于重要业务文件，建议优先使用 `CREATE_NEW` 防止覆盖已有文件；如果确实需要覆盖，应保留日志或备份文件。



## Channel 与 Buffer

本节主要说明 NIO 中 `Channel` 与 `Buffer` 的基础用法。`Channel` 表示数据传输通道，`Buffer` 表示数据缓冲区。文件读写时，常用的是 `FileChannel` 和 `ByteBuffer`。相比传统流式读写，`Channel + Buffer` 更适合大文件复制、文件分块读写、随机位置读写和高性能文件处理。

### FileChannel 基本使用

`FileChannel` 是 NIO 中用于文件读写的通道类，可以从文件中读取数据，也可以向文件写入数据。它通常通过 `FileChannel.open()` 创建，也可以从 `FileInputStream`、`FileOutputStream`、`RandomAccessFile` 中获取。

常见创建方式如下：

| 创建方式                          | 说明                 |
| --------------------------------- | -------------------- |
| `FileChannel.open(path, options)` | NIO 推荐方式         |
| `fileInputStream.getChannel()`    | 从输入流获取只读通道 |
| `fileOutputStream.getChannel()`   | 从输出流获取写通道   |
| `randomAccessFile.getChannel()`   | 支持随机读写         |

`FileChannel` 的常用方法如下：

| 方法             | 说明                         |
| ---------------- | ---------------------------- |
| `read(buffer)`   | 从通道读取数据到缓冲区       |
| `write(buffer)`  | 将缓冲区数据写入通道         |
| `position()`     | 获取当前通道位置             |
| `position(long)` | 设置当前通道位置             |
| `size()`         | 获取文件大小                 |
| `truncate(size)` | 截断文件                     |
| `force(true)`    | 强制将数据刷新到磁盘         |
| `transferTo()`   | 将当前通道数据传输到目标通道 |
| `transferFrom()` | 从源通道接收数据             |

下面示例演示使用 `FileChannel` 读取文件字节数。

文件位置：`src/main/java/io/github/atengk/file/channel/FileChannelExample.java`

```java
package io.github.atengk.file.channel;

import cn.hutool.core.util.StrUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;

/**
 * FileChannel 基础使用示例
 *
 * @author Ateng
 * @since 2026-05-13
 */
public class FileChannelExample {

    private static final Logger log = LoggerFactory.getLogger(FileChannelExample.class);

    /**
     * 默认缓冲区大小
     */
    private static final int BUFFER_SIZE = 8192;

    /**
     * 使用 FileChannel 读取文件字节数
     *
     * @param filePath 文件路径
     * @return 文件字节数
     */
    public static long countBytes(String filePath) {
        Path path = Path.of(filePath);

        if (!Files.isRegularFile(path)) {
            throw new IllegalArgumentException(StrUtil.format("文件不存在或不是普通文件：{}", filePath));
        }

        try (FileChannel channel = FileChannel.open(path, StandardOpenOption.READ)) {
            ByteBuffer buffer = ByteBuffer.allocate(BUFFER_SIZE);
            long total = 0;

            while (channel.read(buffer) != -1) {
                buffer.flip();
                total += buffer.remaining();
                buffer.clear();
            }

            log.info("FileChannel 文件读取完成，文件：{}，字节数：{}", filePath, total);
            return total;
        } catch (IOException e) {
            log.error("FileChannel 文件读取失败，文件：{}", filePath, e);
            throw new RuntimeException("FileChannel 文件读取失败", e);
        }
    }
}
```

`FileChannel` 使用完成后必须关闭。推荐使用 `try-with-resources` 管理资源，避免文件通道未关闭导致文件句柄泄漏。

### ByteBuffer 基本操作

`ByteBuffer` 是 NIO 中最常用的字节缓冲区。文件通道读取数据时，数据先进入 `ByteBuffer`；文件通道写入数据时，数据也需要先放入 `ByteBuffer`。

`ByteBuffer` 常见创建方式如下：

| 方法                              | 说明                                   |
| --------------------------------- | -------------------------------------- |
| `ByteBuffer.allocate(size)`       | 创建堆内缓冲区，普通场景常用           |
| `ByteBuffer.allocateDirect(size)` | 创建直接缓冲区，适合部分高性能 IO 场景 |
| `ByteBuffer.wrap(bytes)`          | 将已有字节数组包装成缓冲区             |

常用操作如下：

| 方法             | 说明                                  |
| ---------------- | ------------------------------------- |
| `put()`          | 向缓冲区写入数据                      |
| `get()`          | 从缓冲区读取数据                      |
| `flip()`         | 从写模式切换到读模式                  |
| `clear()`        | 清空缓冲区，准备下一轮写入            |
| `compact()`      | 保留未读数据，并准备继续写入          |
| `rewind()`       | 将 position 重置为 0，重新读取        |
| `remaining()`    | 获取当前位置到 limit 之间的剩余数据量 |
| `hasRemaining()` | 判断是否还有可读或可写数据            |

下面示例演示 `ByteBuffer` 的基本写入、切换、读取和清理流程。

文件位置：`src/main/java/io/github/atengk/file/channel/ByteBufferExample.java`

```java
package io.github.atengk.file.channel;

import cn.hutool.core.util.CharsetUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.ByteBuffer;

/**
 * ByteBuffer 基础操作示例
 *
 * @author Ateng
 * @since 2026-05-13
 */
public class ByteBufferExample {

    private static final Logger log = LoggerFactory.getLogger(ByteBufferExample.class);

    /**
     * 演示 ByteBuffer 写入和读取流程
     */
    public static void basicOperation() {
        ByteBuffer buffer = ByteBuffer.allocate(32);

        log.info("初始化，position：{}，limit：{}，capacity：{}",
                buffer.position(), buffer.limit(), buffer.capacity());

        buffer.put("Java NIO".getBytes(CharsetUtil.CHARSET_UTF_8));

        log.info("写入后，position：{}，limit：{}，capacity：{}",
                buffer.position(), buffer.limit(), buffer.capacity());

        buffer.flip();

        log.info("flip 后，position：{}，limit：{}，capacity：{}",
                buffer.position(), buffer.limit(), buffer.capacity());

        byte[] bytes = new byte[buffer.remaining()];
        buffer.get(bytes);

        log.info("读取内容：{}", new String(bytes, CharsetUtil.CHARSET_UTF_8));
        log.info("读取后，position：{}，limit：{}，capacity：{}",
                buffer.position(), buffer.limit(), buffer.capacity());

        buffer.clear();

        log.info("clear 后，position：{}，limit：{}，capacity：{}",
                buffer.position(), buffer.limit(), buffer.capacity());
    }
}
```

使用 `ByteBuffer` 时，最容易出错的点是忘记调用 `flip()`。通道读取数据到缓冲区后，缓冲区处于写模式，如果要从缓冲区取出数据写入另一个通道，必须先调用 `flip()` 切换为读模式。

### Buffer 的 position、limit、capacity

`Buffer` 的核心状态由 `position`、`limit` 和 `capacity` 组成。理解这三个属性，是正确使用 `ByteBuffer` 的关键。

| 属性       | 说明                       |
| ---------- | -------------------------- |
| `capacity` | 缓冲区容量，创建后固定不变 |
| `position` | 当前读写位置               |
| `limit`    | 当前可读或可写边界         |

在写模式下，`position` 表示下一个写入位置，`limit` 通常等于 `capacity`。在读模式下，`position` 表示下一个读取位置，`limit` 表示上一次写入结束的位置。

典型流程如下：

| 操作           | position | limit | 说明                     |
| -------------- | -------- | ----- | ------------------------ |
| `allocate(10)` | 0        | 10    | 创建容量为 10 的缓冲区   |
| `put(3 bytes)` | 3        | 10    | 写入 3 个字节            |
| `flip()`       | 0        | 3     | 切换为读模式             |
| `get()`        | 递增     | 3     | 从 position 读取到 limit |
| `clear()`      | 0        | 10    | 清空状态，准备下一次写入 |

下面示例专门打印 `position`、`limit`、`capacity` 的变化。

文件位置：`src/main/java/io/github/atengk/file/channel/BufferStateExample.java`

```java
package io.github.atengk.file.channel;

import cn.hutool.core.util.CharsetUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.ByteBuffer;

/**
 * Buffer 状态变化示例
 *
 * @author Ateng
 * @since 2026-05-13
 */
public class BufferStateExample {

    private static final Logger log = LoggerFactory.getLogger(BufferStateExample.class);

    /**
     * 打印 Buffer 在不同操作后的状态
     */
    public static void printState() {
        ByteBuffer buffer = ByteBuffer.allocate(10);
        print("初始化", buffer);

        buffer.put("abc".getBytes(CharsetUtil.CHARSET_UTF_8));
        print("写入 abc 后", buffer);

        buffer.flip();
        print("调用 flip 后", buffer);

        byte first = buffer.get();
        log.info("读取第一个字节：{}", (char) first);
        print("读取一个字节后", buffer);

        buffer.rewind();
        print("调用 rewind 后", buffer);

        buffer.clear();
        print("调用 clear 后", buffer);
    }

    /**
     * 打印缓冲区状态
     *
     * @param stage 阶段名称
     * @param buffer 缓冲区
     */
    private static void print(String stage, ByteBuffer buffer) {
        log.info("{}，position：{}，limit：{}，capacity：{}",
                stage, buffer.position(), buffer.limit(), buffer.capacity());
    }
}
```

`clear()` 并不会真正清除底层数组中的数据，它只是重置 `position` 和 `limit`，让缓冲区可以重新写入。因此不要把 `clear()` 理解为安全的数据擦除操作。

### Channel 文件复制

使用 `FileChannel` 复制文件有两种常见方式。一种是手动配合 `ByteBuffer` 循环读写，另一种是使用 `transferTo()` 或 `transferFrom()` 进行通道间传输。

手动缓冲区复制适合边读边处理数据的场景。`transferTo()` 和 `transferFrom()` 更适合直接复制文件，代码更简洁，也可能利用操作系统底层优化。

下面示例同时提供两种复制方式。

文件位置：`src/main/java/io/github/atengk/file/channel/ChannelCopyExample.java`

```java
package io.github.atengk.file.channel;

import cn.hutool.core.util.StrUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;

/**
 * Channel 文件复制示例
 *
 * @author Ateng
 * @since 2026-05-13
 */
public class ChannelCopyExample {

    private static final Logger log = LoggerFactory.getLogger(ChannelCopyExample.class);

    /**
     * 默认缓冲区大小
     */
    private static final int BUFFER_SIZE = 8192;

    /**
     * 使用 ByteBuffer 手动复制文件
     *
     * @param sourcePath 源文件路径
     * @param targetPath 目标文件路径
     */
    public static void copyByBuffer(String sourcePath, String targetPath) {
        Path source = Path.of(sourcePath);
        Path target = Path.of(targetPath);

        if (!Files.isRegularFile(source)) {
            throw new IllegalArgumentException(StrUtil.format("源文件不存在或不是普通文件：{}", sourcePath));
        }

        try {
            createParentDirectories(target);

            try (
                    FileChannel inputChannel = FileChannel.open(source, StandardOpenOption.READ);
                    FileChannel outputChannel = FileChannel.open(
                            target,
                            StandardOpenOption.CREATE,
                            StandardOpenOption.TRUNCATE_EXISTING,
                            StandardOpenOption.WRITE
                    )
            ) {
                ByteBuffer buffer = ByteBuffer.allocate(BUFFER_SIZE);

                while (inputChannel.read(buffer) != -1) {
                    buffer.flip();

                    while (buffer.hasRemaining()) {
                        outputChannel.write(buffer);
                    }

                    buffer.clear();
                }

                outputChannel.force(true);
                log.info("Channel 缓冲区复制完成，源文件：{}，目标文件：{}", sourcePath, targetPath);
            }
        } catch (IOException e) {
            log.error("Channel 缓冲区复制失败，源文件：{}，目标文件：{}", sourcePath, targetPath, e);
            throw new RuntimeException("Channel 文件复制失败", e);
        }
    }

    /**
     * 使用 transferTo 复制文件
     *
     * @param sourcePath 源文件路径
     * @param targetPath 目标文件路径
     */
    public static void copyByTransferTo(String sourcePath, String targetPath) {
        Path source = Path.of(sourcePath);
        Path target = Path.of(targetPath);

        if (!Files.isRegularFile(source)) {
            throw new IllegalArgumentException(StrUtil.format("源文件不存在或不是普通文件：{}", sourcePath));
        }

        try {
            createParentDirectories(target);

            try (
                    FileChannel inputChannel = FileChannel.open(source, StandardOpenOption.READ);
                    FileChannel outputChannel = FileChannel.open(
                            target,
                            StandardOpenOption.CREATE,
                            StandardOpenOption.TRUNCATE_EXISTING,
                            StandardOpenOption.WRITE
                    )
            ) {
                long size = inputChannel.size();
                long position = 0;

                while (position < size) {
                    long transferred = inputChannel.transferTo(position, size - position, outputChannel);

                    if (transferred <= 0) {
                        break;
                    }

                    position += transferred;
                }

                outputChannel.force(true);
                log.info("Channel transferTo 复制完成，源文件：{}，目标文件：{}，字节数：{}",
                        sourcePath, targetPath, size);
            }
        } catch (IOException e) {
            log.error("Channel transferTo 复制失败，源文件：{}，目标文件：{}", sourcePath, targetPath, e);
            throw new RuntimeException("Channel 文件复制失败", e);
        }
    }

    /**
     * 创建父级目录
     *
     * @param path 文件路径
     * @throws IOException IO 异常
     */
    private static void createParentDirectories(Path path) throws IOException {
        Path parent = path.getParent();

        if (parent != null) {
            Files.createDirectories(parent);
        }
    }
}
```

使用 `transferTo()` 复制大文件时，不要假设一次调用一定能传输完整文件。更稳妥的方式是循环调用，并累计已传输的位置。

### 大文件分块读写

大文件分块读写的核心思想是固定每次处理的数据大小，避免一次性把整个文件加载到内存中。它适合处理大日志、大视频、大压缩包、大型导入文件等场景。

常见分块策略如下：

| 策略             | 说明                          |
| ---------------- | ----------------------------- |
| 按固定字节块读取 | 适合二进制文件复制、上传分片  |
| 按行读取         | 适合日志、CSV、TXT 等文本文件 |
| 按指定位置读取   | 适合断点续传、文件切片下载    |
| 分块写入临时文件 | 适合大文件拆分、分片上传      |

下面示例演示使用 `FileChannel` 将大文件拆分为多个分片文件。

文件位置：`src/main/java/io/github/atengk/file/channel/LargeFileChunkExample.java`

```java
package io.github.atengk.file.channel;

import cn.hutool.core.io.FileUtil;
import cn.hutool.core.util.StrUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;

/**
 * 大文件分块读写示例
 *
 * @author Ateng
 * @since 2026-05-13
 */
public class LargeFileChunkExample {

    private static final Logger log = LoggerFactory.getLogger(LargeFileChunkExample.class);

    /**
     * 默认分片大小：10MB
     */
    private static final int DEFAULT_CHUNK_SIZE = 10 * 1024 * 1024;

    /**
     * 将大文件拆分为多个分片文件
     *
     * @param sourcePath 源文件路径
     * @param chunkDirPath 分片目录路径
     */
    public static void splitFile(String sourcePath, String chunkDirPath) {
        Path source = Path.of(sourcePath);
        Path chunkDir = Path.of(chunkDirPath);

        if (!Files.isRegularFile(source)) {
            throw new IllegalArgumentException(StrUtil.format("源文件不存在或不是普通文件：{}", sourcePath));
        }

        try {
            Files.createDirectories(chunkDir);

            try (FileChannel sourceChannel = FileChannel.open(source, StandardOpenOption.READ)) {
                ByteBuffer buffer = ByteBuffer.allocate(DEFAULT_CHUNK_SIZE);
                int chunkIndex = 0;

                while (sourceChannel.read(buffer) != -1) {
                    buffer.flip();

                    Path chunkPath = chunkDir.resolve(StrUtil.format("chunk-{}.part", chunkIndex));
                    try (FileChannel chunkChannel = FileChannel.open(
                            chunkPath,
                            StandardOpenOption.CREATE,
                            StandardOpenOption.TRUNCATE_EXISTING,
                            StandardOpenOption.WRITE
                    )) {
                        while (buffer.hasRemaining()) {
                            chunkChannel.write(buffer);
                        }
                        chunkChannel.force(true);
                    }

                    log.info("文件分片写入完成，源文件：{}，分片：{}，大小：{} 字节",
                            sourcePath, chunkPath, FileUtil.size(chunkPath.toFile()));

                    buffer.clear();
                    chunkIndex++;
                }

                log.info("大文件拆分完成，源文件：{}，分片目录：{}，分片数量：{}", sourcePath, chunkDirPath, chunkIndex);
            }
        } catch (IOException e) {
            log.error("大文件拆分失败，源文件：{}，分片目录：{}", sourcePath, chunkDirPath, e);
            throw new RuntimeException("大文件拆分失败", e);
        }
    }

    /**
     * 将分片文件合并为完整文件
     *
     * @param chunkDirPath 分片目录路径
     * @param targetPath 目标文件路径
     * @param chunkCount 分片数量
     */
    public static void mergeFile(String chunkDirPath, String targetPath, int chunkCount) {
        if (chunkCount <= 0) {
            throw new IllegalArgumentException("分片数量必须大于 0");
        }

        Path chunkDir = Path.of(chunkDirPath);
        Path target = Path.of(targetPath);

        try {
            Path parent = target.getParent();
            if (parent != null) {
                Files.createDirectories(parent);
            }

            try (FileChannel targetChannel = FileChannel.open(
                    target,
                    StandardOpenOption.CREATE,
                    StandardOpenOption.TRUNCATE_EXISTING,
                    StandardOpenOption.WRITE
            )) {
                for (int i = 0; i < chunkCount; i++) {
                    Path chunkPath = chunkDir.resolve(StrUtil.format("chunk-{}.part", i));

                    if (!Files.isRegularFile(chunkPath)) {
                        throw new IllegalArgumentException(StrUtil.format("分片文件不存在：{}", chunkPath));
                    }

                    try (FileChannel chunkChannel = FileChannel.open(chunkPath, StandardOpenOption.READ)) {
                        long size = chunkChannel.size();
                        long position = 0;

                        while (position < size) {
                            long transferred = chunkChannel.transferTo(position, size - position, targetChannel);

                            if (transferred <= 0) {
                                break;
                            }

                            position += transferred;
                        }
                    }

                    log.info("分片合并完成，分片：{}", chunkPath);
                }

                targetChannel.force(true);
                log.info("大文件合并完成，分片目录：{}，目标文件：{}", chunkDirPath, targetPath);
            }
        } catch (IOException e) {
            log.error("大文件合并失败，分片目录：{}，目标文件：{}", chunkDirPath, targetPath, e);
            throw new RuntimeException("大文件合并失败", e);
        }
    }
}
```

大文件分块处理时，应额外保存分片数量、分片大小、原始文件名、文件总大小、文件摘要值等元数据。合并完成后可以计算 MD5 或 SHA-256 摘要，校验合并后的文件是否与原文件一致。

## 文件编码与字符集

本节主要说明 Java 文件处理中字符集的使用方式。编码问题只出现在文本文件处理场景中，例如 TXT、CSV、JSON、XML、SQL、日志文件等。图片、视频、PDF、Excel、压缩包等二进制文件不应该按字符集读取。

### Charset 的使用

`Charset` 表示字符集，用于在字节和字符之间进行转换。读取文本时，程序需要按照指定字符集把字节解码为字符；写入文本时，程序需要按照指定字符集把字符编码为字节。

常见字符集如下：

| 字符集       | 说明                                           |
| ------------ | ---------------------------------------------- |
| `UTF-8`      | 推荐默认使用，跨平台兼容性较好                 |
| `GBK`        | 常见于中文 Windows 环境或历史系统              |
| `GB2312`     | 较早的简体中文编码                             |
| `ISO-8859-1` | 西欧字符编码，不支持中文                       |
| `UTF-16`     | 使用 2 字节或 4 字节表示字符，文件体积通常更大 |

Java 中推荐使用 `StandardCharsets` 获取标准字符集，Hutool 中也可以使用 `CharsetUtil`。

下面示例演示字符集的基本使用。

文件位置：`src/main/java/io/github/atengk/file/charset/CharsetExample.java`

```java
package io.github.atengk.file.charset;

import cn.hutool.core.util.CharsetUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;

/**
 * Charset 基础使用示例
 *
 * @author Ateng
 * @since 2026-05-13
 */
public class CharsetExample {

    private static final Logger log = LoggerFactory.getLogger(CharsetExample.class);

    /**
     * 演示字符串编码和解码
     */
    public static void encodeAndDecode() {
        String text = "Java 文件编码处理";

        byte[] utf8Bytes = text.getBytes(StandardCharsets.UTF_8);
        byte[] gbkBytes = text.getBytes(CharsetUtil.CHARSET_GBK);

        String utf8Text = new String(utf8Bytes, StandardCharsets.UTF_8);
        String gbkText = new String(gbkBytes, CharsetUtil.CHARSET_GBK);

        log.info("UTF-8 字节：{}", Arrays.toString(utf8Bytes));
        log.info("GBK 字节：{}", Arrays.toString(gbkBytes));
        log.info("UTF-8 解码结果：{}", utf8Text);
        log.info("GBK 解码结果：{}", gbkText);
    }

    /**
     * 获取系统默认字符集
     *
     * @return 系统默认字符集
     */
    public static Charset getDefaultCharset() {
        Charset charset = Charset.defaultCharset();
        log.info("系统默认字符集：{}", charset);
        return charset;
    }
}
```

实际项目中不建议依赖 `Charset.defaultCharset()` 处理业务文件。不同操作系统、不同容器镜像、不同启动参数下，默认字符集可能不一致。

### UTF-8 编码处理

`UTF-8` 是当前 Java 项目中最推荐使用的文本编码。它可以兼容中文、英文、数字、特殊符号，跨平台表现稳定，适合配置文件、接口报文、日志、CSV、JSON、SQL 等文本场景。

项目中建议统一以下位置的编码：

| 位置           | 建议           |
| -------------- | -------------- |
| 源码文件       | 使用 UTF-8     |
| Maven 编译编码 | 使用 UTF-8     |
| 配置文件       | 使用 UTF-8     |
| 日志文件       | 使用 UTF-8     |
| 文件导入导出   | 明确指定 UTF-8 |
| HTTP 响应      | 明确设置 UTF-8 |

Maven 项目可以在 `pom.xml` 中统一编码。

```xml
<properties>
    <!-- 项目源码和资源文件统一使用 UTF-8 编码 -->
    <project.build.sourceEncoding>UTF-8</project.build.sourceEncoding>

    <!-- Maven 报告输出统一使用 UTF-8 编码 -->
    <project.reporting.outputEncoding>UTF-8</project.reporting.outputEncoding>
</properties>
```

下面示例演示使用 UTF-8 读取和写入文本文件。

文件位置：`src/main/java/io/github/atengk/file/charset/Utf8FileExample.java`

```java
package io.github.atengk.file.charset;

import cn.hutool.core.io.FileUtil;
import cn.hutool.core.util.CharsetUtil;
import cn.hutool.core.util.StrUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * UTF-8 文件处理示例
 *
 * @author Ateng
 * @since 2026-05-13
 */
public class Utf8FileExample {

    private static final Logger log = LoggerFactory.getLogger(Utf8FileExample.class);

    /**
     * 使用 UTF-8 写入文本
     *
     * @param filePath 文件路径
     * @param content 文件内容
     */
    public static void writeUtf8(String filePath, String content) {
        if (StrUtil.isBlank(content)) {
            log.warn("UTF-8 写入跳过，内容为空，文件：{}", filePath);
            return;
        }

        FileUtil.writeString(content, filePath, CharsetUtil.CHARSET_UTF_8);
        log.info("UTF-8 文件写入完成，文件：{}，字符数：{}", filePath, content.length());
    }

    /**
     * 使用 UTF-8 读取文本
     *
     * @param filePath 文件路径
     * @return 文件内容
     */
    public static String readUtf8(String filePath) {
        String content = FileUtil.readString(filePath, CharsetUtil.CHARSET_UTF_8);
        log.info("UTF-8 文件读取完成，文件：{}，字符数：{}", filePath, content.length());
        return content;
    }
}
```

对于接口返回的文件下载，如果内容是文本，也应明确响应编码和文件内容编码一致。否则服务端写入是 UTF-8，客户端按 GBK 打开时仍可能出现乱码。

### 中文乱码问题

中文乱码通常是编码和解码使用的字符集不一致导致的。例如文件使用 `GBK` 写入，但程序按 `UTF-8` 读取；或者程序按 `UTF-8` 写出，但客户端按 `GBK` 打开。

常见乱码原因如下：

| 原因                  | 示例                              |
| --------------------- | --------------------------------- |
| 读取编码错误          | GBK 文件按 UTF-8 读取             |
| 写入编码错误          | 应写 UTF-8，却使用系统默认编码    |
| 工具打开编码错误      | 文件本身 UTF-8，编辑器按 GBK 打开 |
| HTTP 响应编码缺失     | 浏览器或客户端自行猜测编码        |
| CSV 被 Excel 错误识别 | UTF-8 CSV 在部分 Excel 中打开乱码 |
| 数据库连接编码不一致  | 文件导入数据库后中文异常          |

下面示例演示错误编码读取和正确编码读取的区别。

文件位置：`src/main/java/io/github/atengk/file/charset/ChineseGarbledExample.java`

```java
package io.github.atengk.file.charset;

import cn.hutool.core.io.FileUtil;
import cn.hutool.core.util.CharsetUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.charset.StandardCharsets;

/**
 * 中文乱码问题示例
 *
 * @author Ateng
 * @since 2026-05-13
 */
public class ChineseGarbledExample {

    private static final Logger log = LoggerFactory.getLogger(ChineseGarbledExample.class);

    /**
     * 演示 GBK 文件使用不同编码读取的效果
     *
     * @param filePath 文件路径
     */
    public static void compareReadCharset(String filePath) {
        String wrongContent = FileUtil.readString(filePath, StandardCharsets.UTF_8);
        String rightContent = FileUtil.readString(filePath, CharsetUtil.CHARSET_GBK);

        log.info("使用 UTF-8 读取 GBK 文件，可能乱码：{}", wrongContent);
        log.info("使用 GBK 读取 GBK 文件，正常内容：{}", rightContent);
    }

    /**
     * 将 GBK 文件转换为 UTF-8 文件
     *
     * @param sourcePath GBK 源文件路径
     * @param targetPath UTF-8 目标文件路径
     */
    public static void convertGbkToUtf8(String sourcePath, String targetPath) {
        String content = FileUtil.readString(sourcePath, CharsetUtil.CHARSET_GBK);
        FileUtil.writeString(content, targetPath, CharsetUtil.CHARSET_UTF_8);
        log.info("文件编码转换完成，源文件：{}，目标文件：{}，编码：GBK -> UTF-8", sourcePath, targetPath);
    }
}
```

解决中文乱码时，不要只在显示端处理，应先确认文件真实编码、程序读取编码、程序写出编码和客户端打开编码是否一致。对于外部导入文件，建议在模板说明中明确要求 UTF-8 编码。

### 默认编码风险

默认编码指程序在没有明确指定字符集时，使用运行环境的默认字符集。它依赖操作系统、JDK 版本、容器镜像、启动参数等因素，存在较强的不确定性。

容易依赖默认编码的写法包括：

```java
new String(bytes);
text.getBytes();
new FileReader(file);
new FileWriter(file);
Files.readString(path);
Files.writeString(path, content);
```

其中部分 API 在新版本 JDK 中默认使用 UTF-8，但为了代码可读性和跨环境稳定性，业务代码中仍建议显式指定字符集。

不推荐写法如下：

```java
String content = new String(bytes);
byte[] bytes = content.getBytes();
```

推荐写法如下：

```java
String content = new String(bytes, StandardCharsets.UTF_8);
byte[] bytes = content.getBytes(StandardCharsets.UTF_8);
```

下面示例演示如何避免默认编码风险。

文件位置：`src/main/java/io/github/atengk/file/charset/DefaultCharsetRiskExample.java`

```java
package io.github.atengk.file.charset;

import cn.hutool.core.io.FileUtil;
import cn.hutool.core.util.CharsetUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;

/**
 * 默认编码风险示例
 *
 * @author Ateng
 * @since 2026-05-13
 */
public class DefaultCharsetRiskExample {

    private static final Logger log = LoggerFactory.getLogger(DefaultCharsetRiskExample.class);

    /**
     * 打印当前运行环境默认编码
     */
    public static void printDefaultCharset() {
        Charset defaultCharset = Charset.defaultCharset();
        log.info("当前运行环境默认编码：{}", defaultCharset);
    }

    /**
     * 使用明确编码进行字符串和字节数组转换
     *
     * @param text 文本内容
     * @return UTF-8 字节数组
     */
    public static byte[] toUtf8Bytes(String text) {
        byte[] bytes = text.getBytes(StandardCharsets.UTF_8);
        log.info("文本转 UTF-8 字节完成，字符数：{}，字节数：{}", text.length(), bytes.length);
        return bytes;
    }

    /**
     * 使用明确编码读取文本文件
     *
     * @param filePath 文件路径
     * @return 文本内容
     */
    public static String readWithCharset(String filePath) {
        String content = FileUtil.readString(filePath, CharsetUtil.CHARSET_UTF_8);
        log.info("明确编码读取完成，文件：{}，编码：UTF-8，字符数：{}", filePath, content.length());
        return content;
    }

    /**
     * 使用明确编码写入文本文件
     *
     * @param filePath 文件路径
     * @param content 文件内容
     */
    public static void writeWithCharset(String filePath, String content) {
        FileUtil.writeString(content, filePath, CharsetUtil.CHARSET_UTF_8);
        log.info("明确编码写入完成，文件：{}，编码：UTF-8，字符数：{}", filePath, content.length());
    }
}
```

实际项目中建议形成统一规范：所有文本文件读写都必须显式指定字符集，默认使用 `UTF-8`；只有兼容历史系统、银行系统、政企系统或 Windows 旧工具导出文件时，才按实际情况使用 `GBK` 等其他编码。



## 异常处理

文件操作直接依赖操作系统、文件系统、权限、磁盘空间、路径规则和外部输入，因此异常处理是文件开发中必须重点关注的部分。常见异常包括文件不存在、权限不足、路径错误、文件被占用、读写失败等。业务代码中应明确区分异常类型，记录关键路径、操作类型和异常原因，便于快速定位问题。

### IOException 处理

`IOException` 是 Java IO 和 NIO 文件操作中最常见的受检异常，表示输入输出过程中发生错误。文件读取失败、写入失败、复制失败、移动失败、删除失败、磁盘异常、网络文件系统异常等都可能抛出 `IOException`。

常见触发场景如下：

| 场景         | 说明                               |
| ------------ | ---------------------------------- |
| 文件读取失败 | 文件损坏、文件被占用、磁盘异常     |
| 文件写入失败 | 磁盘空间不足、目录不存在、权限不足 |
| 文件复制失败 | 源文件不可读、目标路径不可写       |
| 文件移动失败 | 目标文件已存在、跨文件系统移动失败 |
| 文件删除失败 | 文件被占用、权限不足               |

处理 `IOException` 时，不建议简单吞掉异常。应记录操作类型、源路径、目标路径、文件大小等关键信息，然后根据业务需要转换为运行时异常或业务异常。

下面示例演示统一处理 `IOException` 的文件复制逻辑。

文件位置：`src/main/java/io/github/atengk/file/exception/IOExceptionExample.java`

```java
package io.github.atengk.file.exception;

import cn.hutool.core.util.StrUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;

/**
 * IOException 处理示例
 *
 * @author Ateng
 * @since 2026-05-13
 */
public class IOExceptionExample {

    private static final Logger log = LoggerFactory.getLogger(IOExceptionExample.class);

    /**
     * 复制文件并处理 IOException
     *
     * @param sourcePath 源文件路径
     * @param targetPath 目标文件路径
     */
    public static void copyFile(String sourcePath, String targetPath) {
        Path source = Path.of(sourcePath);
        Path target = Path.of(targetPath);

        try {
            if (!Files.isRegularFile(source)) {
                throw new IllegalArgumentException(StrUtil.format("源文件不存在或不是普通文件：{}", sourcePath));
            }

            Path parent = target.getParent();
            if (parent != null) {
                Files.createDirectories(parent);
            }

            Files.copy(source, target, StandardCopyOption.REPLACE_EXISTING);
            log.info("文件复制成功，源文件：{}，目标文件：{}", sourcePath, targetPath);
        } catch (IOException e) {
            log.error("文件复制失败，源文件：{}，目标文件：{}", sourcePath, targetPath, e);
            throw new RuntimeException("文件复制失败", e);
        }
    }
}
```

实际项目中，可以把底层 `IOException` 转换为统一的业务异常，例如 `FileOperateException`，让 Controller 层或全局异常处理器返回统一响应。

### FileNotFoundException 处理

`FileNotFoundException` 通常出现在传统 IO 中，例如使用 `FileInputStream`、`FileOutputStream`、`FileReader`、`FileWriter` 时。如果源文件不存在、路径指向目录而不是文件、目标文件无法创建，都可能触发该异常。

常见原因如下：

| 原因           | 示例                                                |
| -------------- | --------------------------------------------------- |
| 源文件不存在   | 读取 `/data/demo.txt`，但文件不存在                 |
| 路径是目录     | 使用文件流读取目录                                  |
| 父级目录不存在 | 写入 `/data/upload/a.txt`，但 `/data/upload` 不存在 |
| 权限不足       | 当前用户没有读写权限                                |

如果使用 NIO，文件不存在时更常见的是 `NoSuchFileException`。如果使用传统 IO，则更常见的是 `FileNotFoundException`。

下面示例演示传统 IO 中处理 `FileNotFoundException`。

文件位置：`src/main/java/io/github/atengk/file/exception/FileNotFoundExceptionExample.java`

```java
package io.github.atengk.file.exception;

import cn.hutool.core.io.FileUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;

/**
 * FileNotFoundException 处理示例
 *
 * @author Ateng
 * @since 2026-05-13
 */
public class FileNotFoundExceptionExample {

    private static final Logger log = LoggerFactory.getLogger(FileNotFoundExceptionExample.class);

    /**
     * 使用传统 IO 读取文件
     *
     * @param filePath 文件路径
     * @return 文件字节数
     */
    public static long readFile(String filePath) {
        try (BufferedInputStream inputStream = FileUtil.getInputStream(filePath)) {
            long total = 0;
            byte[] buffer = new byte[8192];
            int length;

            while ((length = inputStream.read(buffer)) != -1) {
                total += length;
            }

            log.info("文件读取完成，文件：{}，字节数：{}", filePath, total);
            return total;
        } catch (cn.hutool.core.io.IORuntimeException e) {
            Throwable cause = e.getCause();

            if (cause instanceof FileNotFoundException) {
                log.error("文件不存在或无法打开，文件：{}", filePath, e);
                throw new RuntimeException("文件不存在或无法打开", e);
            }

            log.error("文件读取失败，文件：{}", filePath, e);
            throw new RuntimeException("文件读取失败", e);
        } catch (IOException e) {
            log.error("文件读取失败，文件：{}", filePath, e);
            throw new RuntimeException("文件读取失败", e);
        }
    }
}
```

在业务入口处，建议先校验文件是否存在、是否为普通文件、是否可读，再执行读取操作。这样可以把底层异常转换为更清晰的业务提示。

### AccessDeniedException 处理

`AccessDeniedException` 是 NIO 中常见的权限异常，表示当前程序没有权限访问目标文件或目录。它通常发生在读取无权限文件、写入受保护目录、删除被保护文件、访问系统目录等场景。

常见触发场景如下：

| 场景               | 说明                     |
| ------------------ | ------------------------ |
| 读取无权限文件     | 当前用户没有读权限       |
| 写入无权限目录     | 目标目录不可写           |
| 删除系统文件       | 当前用户没有删除权限     |
| 文件被安全策略限制 | 容器、系统或安全软件拦截 |
| Linux 用户权限不足 | 应用进程用户不是文件属主 |

下面示例演示单独捕获 `AccessDeniedException`，便于输出明确日志。

文件位置：`src/main/java/io/github/atengk/file/exception/AccessDeniedExceptionExample.java`

```java
package io.github.atengk.file.exception;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.file.AccessDeniedException;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * AccessDeniedException 处理示例
 *
 * @author Ateng
 * @since 2026-05-13
 */
public class AccessDeniedExceptionExample {

    private static final Logger log = LoggerFactory.getLogger(AccessDeniedExceptionExample.class);

    /**
     * 删除文件并处理权限异常
     *
     * @param filePath 文件路径
     */
    public static void deleteFile(String filePath) {
        Path path = Path.of(filePath);

        try {
            boolean deleted = Files.deleteIfExists(path);
            log.info("文件删除完成，文件：{}，结果：{}", filePath, deleted);
        } catch (AccessDeniedException e) {
            log.error("文件删除失败，权限不足，文件：{}", filePath, e);
            throw new RuntimeException("文件权限不足，无法删除", e);
        } catch (IOException e) {
            log.error("文件删除失败，文件：{}", filePath, e);
            throw new RuntimeException("文件删除失败", e);
        }
    }
}
```

生产环境中遇到权限异常时，应检查应用运行用户、目录属主、读写权限、容器挂载权限、SELinux 或安全策略等因素。不要通过扩大权限绕过问题，应遵循最小权限原则。

### NoSuchFileException 处理

`NoSuchFileException` 是 NIO 中表示文件或目录不存在的异常，常见于 `Files.readString()`、`Files.copy()`、`Files.move()`、`Files.size()` 等操作。

常见触发场景如下：

| 场景             | 说明                       |
| ---------------- | -------------------------- |
| 读取不存在文件   | 源文件路径错误             |
| 复制不存在文件   | 源文件不存在               |
| 获取文件大小失败 | 文件已被删除               |
| 移动文件失败     | 源文件不存在或父目录不存在 |

下面示例演示读取文件时单独处理 `NoSuchFileException`。

文件位置：`src/main/java/io/github/atengk/file/exception/NoSuchFileExceptionExample.java`

```java
package io.github.atengk.file.exception;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.NoSuchFileException;
import java.nio.file.Path;

/**
 * NoSuchFileException 处理示例
 *
 * @author Ateng
 * @since 2026-05-13
 */
public class NoSuchFileExceptionExample {

    private static final Logger log = LoggerFactory.getLogger(NoSuchFileExceptionExample.class);

    /**
     * 读取文本文件并处理文件不存在异常
     *
     * @param filePath 文件路径
     * @return 文件内容
     */
    public static String readText(String filePath) {
        Path path = Path.of(filePath);

        try {
            String content = Files.readString(path, StandardCharsets.UTF_8);
            log.info("文件读取完成，文件：{}，字符数：{}", filePath, content.length());
            return content;
        } catch (NoSuchFileException e) {
            log.warn("文件读取失败，文件不存在：{}", filePath);
            throw new RuntimeException("文件不存在", e);
        } catch (IOException e) {
            log.error("文件读取失败，文件：{}", filePath, e);
            throw new RuntimeException("文件读取失败", e);
        }
    }
}
```

对于用户上传、异步任务、临时文件处理等场景，文件可能在任务执行前被清理或移动，因此即使前面判断过文件存在，真正读写时也仍然需要处理 `NoSuchFileException`。

### 异常日志记录

文件异常日志应记录足够的上下文信息，否则问题排查会非常困难。至少应包含操作类型、文件路径、目标路径、文件大小、业务编号、用户编号、异常堆栈等信息。

推荐记录内容如下：

| 日志字段 | 说明                           |
| -------- | ------------------------------ |
| 操作类型 | 读取、写入、复制、移动、删除   |
| 文件路径 | 当前操作的源文件路径           |
| 目标路径 | 复制、移动、写入时的目标路径   |
| 文件大小 | 大文件处理时尤其重要           |
| 业务编号 | 订单号、任务编号、导入批次号等 |
| 用户编号 | 操作发起人                     |
| 异常堆栈 | 必须保留完整异常对象           |

下面示例提供一个简单的文件异常日志工具方法。

文件位置：`src/main/java/io/github/atengk/file/exception/FileExceptionLogExample.java`

```java
package io.github.atengk.file.exception;

import cn.hutool.core.util.StrUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * 文件异常日志记录示例
 *
 * @author Ateng
 * @since 2026-05-13
 */
public class FileExceptionLogExample {

    private static final Logger log = LoggerFactory.getLogger(FileExceptionLogExample.class);

    /**
     * 记录文件操作异常日志
     *
     * @param operateType 操作类型
     * @param sourcePath 源文件路径
     * @param targetPath 目标文件路径
     * @param businessNo 业务编号
     * @param exception 异常对象
     */
    public static void logFileException(String operateType,
                                        String sourcePath,
                                        String targetPath,
                                        String businessNo,
                                        Exception exception) {
        log.error("文件操作异常，操作类型：{}，源文件：{}，目标文件：{}，业务编号：{}，异常信息：{}",
                operateType,
                sourcePath,
                targetPath,
                businessNo,
                StrUtil.blankToDefault(exception.getMessage(), "无异常消息"),
                exception);
    }
}
```

不要只记录 `e.getMessage()` 而丢失异常对象。正确写法应把异常对象作为日志最后一个参数传入，这样日志框架才能输出完整堆栈。

## 开发规范与最佳实践

本节整理 Java 文件处理中的常用开发规范。文件操作属于容易产生线上问题的基础能力，开发时应优先考虑资源关闭、路径安全、编码一致性、大文件内存风险、异常日志和可测试性。

### 优先使用 try-with-resources

只要代码中手动创建了流、Reader、Writer、Channel、DirectoryStream 等资源，就应优先使用 `try-with-resources` 自动关闭。这样可以减少异常分支遗漏关闭资源的问题。

推荐写法如下：

```java
try (
        InputStream inputStream = Files.newInputStream(sourcePath);
        OutputStream outputStream = Files.newOutputStream(targetPath)
) {
    inputStream.transferTo(outputStream);
    outputStream.flush();
}
```

不推荐写法如下：

```java
InputStream inputStream = null;
try {
    inputStream = Files.newInputStream(sourcePath);
    // 处理文件
} finally {
    if (inputStream != null) {
        inputStream.close();
    }
}
```

开发规范如下：

1. 流对象不要依赖 GC 回收。
2. 多个资源应放在同一个 `try-with-resources` 中统一管理。
3. 返回流对象给调用方时，必须明确由谁负责关闭。
4. `Files.walk()`、`Files.list()` 返回的 `Stream<Path>` 也需要关闭。
5. Web 响应输出流由容器管理时，不要随意关闭响应对象，但自己创建的输入流必须关闭。

### 优先使用 Path 与 Files

新项目中建议优先使用 `Path` 和 `Files`，而不是继续大量使用 `File`。`Path` 和 `Files` 的语义更清晰，API 更完整，适合现代 Java 文件操作。

推荐使用方式如下：

| 操作     | 推荐 API                                           |
| -------- | -------------------------------------------------- |
| 路径表示 | `Path`                                             |
| 文件判断 | `Files.exists()`、`Files.isRegularFile()`          |
| 创建目录 | `Files.createDirectories()`                        |
| 文件复制 | `Files.copy()`                                     |
| 文件移动 | `Files.move()`                                     |
| 文件删除 | `Files.deleteIfExists()`                           |
| 文本读取 | `Files.readString()`、`Files.newBufferedReader()`  |
| 文本写入 | `Files.writeString()`、`Files.newBufferedWriter()` |
| 目录遍历 | `Files.walk()`、`Files.list()`                     |

下面示例演示一个推荐的基础文件写入方法。

文件位置：`src/main/java/io/github/atengk/file/bestpractice/PathFilesBestPractice.java`

```java
package io.github.atengk.file.bestpractice;

import cn.hutool.core.util.StrUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * Path 与 Files 最佳实践示例
 *
 * @author Ateng
 * @since 2026-05-13
 */
public class PathFilesBestPractice {

    private static final Logger log = LoggerFactory.getLogger(PathFilesBestPractice.class);

    /**
     * 安全写入 UTF-8 文本文件
     *
     * @param filePath 文件路径
     * @param content 文件内容
     */
    public static void writeUtf8Text(String filePath, String content) {
        if (StrUtil.isBlank(content)) {
            log.warn("文件写入跳过，内容为空，文件：{}", filePath);
            return;
        }

        try {
            Path path = Path.of(filePath);
            Path parent = path.getParent();

            if (parent != null) {
                Files.createDirectories(parent);
            }

            Files.writeString(path, content, StandardCharsets.UTF_8);
            log.info("文件写入完成，文件：{}，字符数：{}", filePath, content.length());
        } catch (IOException e) {
            log.error("文件写入失败，文件：{}", filePath, e);
            throw new RuntimeException("文件写入失败", e);
        }
    }
}
```

`File` 并不是不能使用，但更适合和历史 API、第三方库兼容。业务代码内部建议尽量以 `Path` 作为路径表达。

### 避免一次性读取超大文件

一次性读取超大文件会导致内存瞬间升高，严重时可能触发 `OutOfMemoryError`。例如 `readAllBytes()`、`readAllLines()`、`readString()`、`FileUtil.readUtf8String()` 都不适合读取超大文件。

不推荐写法如下：

```java
byte[] bytes = Files.readAllBytes(Path.of("data/big-file.dat"));
String content = Files.readString(Path.of("data/big.log"));
```

推荐使用缓冲区或按行读取。

下面示例演示大文件按缓冲区处理。

文件位置：`src/main/java/io/github/atengk/file/bestpractice/LargeFileBestPractice.java`

```java
package io.github.atengk.file.bestpractice;

import cn.hutool.core.util.StrUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedInputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * 大文件处理最佳实践示例
 *
 * @author Ateng
 * @since 2026-05-13
 */
public class LargeFileBestPractice {

    private static final Logger log = LoggerFactory.getLogger(LargeFileBestPractice.class);

    /**
     * 默认缓冲区大小
     */
    private static final int BUFFER_SIZE = 1024 * 64;

    /**
     * 分批读取大文件
     *
     * @param filePath 文件路径
     * @return 文件字节数
     */
    public static long readLargeFile(String filePath) {
        Path path = Path.of(filePath);

        if (!Files.isRegularFile(path)) {
            throw new IllegalArgumentException(StrUtil.format("文件不存在或不是普通文件：{}", filePath));
        }

        try (BufferedInputStream inputStream = new BufferedInputStream(Files.newInputStream(path))) {
            byte[] buffer = new byte[BUFFER_SIZE];
            long total = 0;
            int length;

            while ((length = inputStream.read(buffer)) != -1) {
                total += length;
            }

            log.info("大文件读取完成，文件：{}，字节数：{}", filePath, total);
            return total;
        } catch (IOException e) {
            log.error("大文件读取失败，文件：{}", filePath, e);
            throw new RuntimeException("大文件读取失败", e);
        }
    }
}
```

如果是文本大文件，例如日志、CSV、SQL，优先按行读取；如果是二进制大文件，例如视频、压缩包，优先按字节缓冲区读取。

### 明确指定字符编码

文本文件读写必须明确指定字符编码，推荐默认使用 `UTF-8`。不要依赖操作系统默认编码，否则在 Windows、Linux、Docker、CI/CD 环境之间迁移时，可能出现中文乱码。

推荐写法如下：

```java
String content = Files.readString(path, StandardCharsets.UTF_8);
Files.writeString(path, content, StandardCharsets.UTF_8);
```

不推荐写法如下：

```java
String content = new String(bytes);
byte[] bytes = content.getBytes();
```

开发规范如下：

1. 文本文件统一使用 `UTF-8`。
2. 外部系统文件必须确认真实编码。
3. CSV 导入导出要特别注意 Excel 打开乱码问题。
4. HTTP 下载文本文件时，应保证文件编码和响应编码一致。
5. 字符流转换时必须指定字符集。

### 合理使用缓冲流

缓冲流可以减少底层 IO 调用次数，提高读写效率。普通文件复制、大文件读写、文本逐行处理都建议使用缓冲流。

推荐场景如下：

| 场景           | 推荐方式                                      |
| -------------- | --------------------------------------------- |
| 二进制文件复制 | `BufferedInputStream`、`BufferedOutputStream` |
| 文本按行读取   | `BufferedReader`                              |
| 文本分批写入   | `BufferedWriter`                              |
| 大文件处理     | 缓冲流或 `FileChannel`                        |
| 小文本读取     | `Files.readString()` 即可                     |

缓冲区大小建议如下：

1. 普通文件使用 8KB 或 16KB。
2. 大文件使用 64KB 或 128KB。
3. 高并发场景不要盲目使用过大缓冲区。
4. 缓冲区大小应根据压测结果调整，而不是凭经验无限增大。

### 文件路径安全校验

文件路径安全是文件开发中最容易被忽略的问题。只要路径来自用户输入，就必须校验，避免路径穿越、任意文件读取、任意文件覆盖和任意文件删除。

危险路径示例：

```text
../../../../etc/passwd
../application.yml
D:\system\secret.txt
/tmp/upload/../../config.yml
```

安全处理思路是：先定义允许访问的根目录，然后将用户传入路径解析为标准化绝对路径，最后判断目标路径是否仍然位于根目录下。

下面示例演示文件路径安全校验。

文件位置：`src/main/java/io/github/atengk/file/bestpractice/FilePathSecurityExample.java`

```java
package io.github.atengk.file.bestpractice;

import cn.hutool.core.util.StrUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.file.Path;

/**
 * 文件路径安全校验示例
 *
 * @author Ateng
 * @since 2026-05-13
 */
public class FilePathSecurityExample {

    private static final Logger log = LoggerFactory.getLogger(FilePathSecurityExample.class);

    /**
     * 解析安全文件路径，防止路径穿越
     *
     * @param rootDir 允许访问的根目录
     * @param userInputPath 用户输入路径
     * @return 安全路径
     */
    public static Path resolveSafePath(String rootDir, String userInputPath) {
        if (StrUtil.isBlank(rootDir)) {
            throw new IllegalArgumentException("根目录不能为空");
        }
        if (StrUtil.isBlank(userInputPath)) {
            throw new IllegalArgumentException("用户输入路径不能为空");
        }

        Path rootPath = Path.of(rootDir).toAbsolutePath().normalize();
        Path targetPath = rootPath.resolve(userInputPath).toAbsolutePath().normalize();

        if (!targetPath.startsWith(rootPath)) {
            log.warn("非法文件路径访问，根目录：{}，用户输入：{}，解析路径：{}", rootPath, userInputPath, targetPath);
            throw new SecurityException("非法文件路径");
        }

        log.info("文件路径校验通过，根目录：{}，目标路径：{}", rootPath, targetPath);
        return targetPath;
    }
}
```

路径安全校验应放在文件读取、下载、删除、预览、移动、重命名等所有入口处。不要相信前端传入的文件路径，也不要把服务器绝对路径直接暴露给客户端。

## 测试与验证

文件操作测试需要覆盖正常读写、异常路径、权限不足、文件不存在、编码处理、临时目录、大文件性能等场景。测试时应避免直接操作真实业务目录，推荐使用 JUnit 5 的 `@TempDir` 创建临时目录，测试结束后自动清理。

### 单元测试场景

文件处理单元测试应覆盖核心操作和边界场景，而不是只测试成功路径。常见测试场景如下：

| 测试场景   | 验证目标                       |
| ---------- | ------------------------------ |
| 文件创建   | 文件是否成功创建               |
| 文件写入   | 内容是否正确写入               |
| 文件读取   | 读取内容是否与写入一致         |
| 文件复制   | 目标文件是否存在，大小是否一致 |
| 文件删除   | 文件是否被删除                 |
| 空内容写入 | 是否跳过或抛出预期异常         |
| 文件不存在 | 是否抛出预期异常               |
| 编码处理   | 中文是否正常读写               |
| 路径穿越   | 是否被安全校验拦截             |

下面示例演示使用 `@TempDir` 进行基础文件读写测试。

文件位置：`src/test/java/io/github/atengk/file/FileBasicTest.java`

```java
package io.github.atengk.file;

import cn.hutool.core.io.FileUtil;
import cn.hutool.core.util.CharsetUtil;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;

/**
 * 文件基础操作测试
 *
 * @author Ateng
 * @since 2026-05-13
 */
class FileBasicTest {

    @TempDir
    Path tempDir;

    /**
     * 测试文本文件写入和读取
     */
    @Test
    void testWriteAndReadText() {
        Path filePath = tempDir.resolve("demo.txt");
        String content = "Java 文件读写测试";

        FileUtil.writeString(content, filePath.toFile(), CharsetUtil.CHARSET_UTF_8);
        String readContent = FileUtil.readString(filePath.toFile(), CharsetUtil.CHARSET_UTF_8);

        Assertions.assertTrue(Files.exists(filePath));
        Assertions.assertEquals(content, readContent);
    }

    /**
     * 测试文件删除
     */
    @Test
    void testDeleteFile() {
        Path filePath = tempDir.resolve("delete.txt");

        FileUtil.writeUtf8String("待删除文件", filePath.toFile());
        boolean deleted = FileUtil.del(filePath.toFile());

        Assertions.assertTrue(deleted);
        Assertions.assertFalse(Files.exists(filePath));
    }
}
```

`@TempDir` 会为每个测试创建隔离的临时目录，可以避免测试污染项目目录，也能减少不同测试之间的相互影响。

### 文件读写结果校验

文件读写结果校验不应只判断代码是否执行完成，还应验证文件是否存在、内容是否一致、文件大小是否正确、编码是否正确、行数是否符合预期。

常见校验点如下：

1. 文件是否存在。
2. 文件是否为普通文件。
3. 文件大小是否大于 0。
4. 文本内容是否一致。
5. 文本行数是否正确。
6. 中文内容是否乱码。
7. 复制后源文件和目标文件大小是否一致。
8. 追加写入后原内容是否保留。

下面示例演示复制文件后的结果校验。

文件位置：`src/test/java/io/github/atengk/file/FileCopyTest.java`

```java
package io.github.atengk.file;

import cn.hutool.core.io.FileUtil;
import cn.hutool.core.util.CharsetUtil;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;

/**
 * 文件复制结果测试
 *
 * @author Ateng
 * @since 2026-05-13
 */
class FileCopyTest {

    @TempDir
    Path tempDir;

    /**
     * 测试文件复制后内容和大小一致
     *
     * @throws Exception IO 异常
     */
    @Test
    void testCopyResult() throws Exception {
        Path source = tempDir.resolve("source.txt");
        Path target = tempDir.resolve("target.txt");
        String content = "文件复制结果校验";

        FileUtil.writeString(content, source.toFile(), CharsetUtil.CHARSET_UTF_8);
        Files.copy(source, target);

        Assertions.assertTrue(Files.exists(target));
        Assertions.assertEquals(Files.size(source), Files.size(target));
        Assertions.assertEquals(content, Files.readString(target, CharsetUtil.CHARSET_UTF_8));
    }
}
```

对于二进制文件，通常校验文件大小和摘要值。对于文本文件，可以直接校验内容或行数。

### 异常场景验证

异常场景验证用于确认文件不存在、路径非法、权限不足、路径穿越等情况下，程序是否按照预期抛出异常或返回错误信息。异常测试可以提高文件操作代码的稳定性。

常见异常测试包括：

| 异常场景       | 预期结果                              |
| -------------- | ------------------------------------- |
| 读取不存在文件 | 抛出 `NoSuchFileException` 或业务异常 |
| 复制不存在文件 | 抛出业务异常                          |
| 路径穿越       | 抛出 `SecurityException`              |
| 空路径         | 抛出 `IllegalArgumentException`       |
| 写入空内容     | 跳过或抛出业务异常                    |
| 删除不存在文件 | 返回 false 或记录跳过日志             |

下面示例演示路径穿越校验测试。

文件位置：`src/test/java/io/github/atengk/file/FilePathSecurityTest.java`

```java
package io.github.atengk.file;

import io.github.atengk.file.bestpractice.FilePathSecurityExample;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;

/**
 * 文件路径安全测试
 *
 * @author Ateng
 * @since 2026-05-13
 */
class FilePathSecurityTest {

    @TempDir
    Path tempDir;

    /**
     * 测试合法路径可以通过校验
     */
    @Test
    void testSafePath() {
        Path safePath = FilePathSecurityExample.resolveSafePath(tempDir.toString(), "upload/demo.txt");

        Assertions.assertTrue(safePath.startsWith(tempDir.toAbsolutePath().normalize()));
    }

    /**
     * 测试路径穿越会被拦截
     */
    @Test
    void testPathTraversal() {
        Assertions.assertThrows(SecurityException.class, () ->
                FilePathSecurityExample.resolveSafePath(tempDir.toString(), "../application.yml")
        );
    }
}
```

异常场景不应只依赖人工测试，尤其是路径安全、文件删除、文件覆盖等高风险操作，应尽量通过自动化测试固定下来。

### 临时目录测试

临时目录测试适合验证上传、导出、压缩、解压、临时文件生成等场景。使用临时目录可以避免污染项目目录，也可以让测试在不同环境下保持一致。

常见临时目录测试场景如下：

1. 创建临时文件。
2. 创建多级临时目录。
3. 写入临时文件并读取。
4. 复制文件到临时目录。
5. 删除临时文件。
6. 验证临时目录下文件数量。

下面示例演示在临时目录中创建多级目录并写入文件。

文件位置：`src/test/java/io/github/atengk/file/TempDirectoryTest.java`

```java
package io.github.atengk.file;

import cn.hutool.core.io.FileUtil;
import cn.hutool.core.util.CharsetUtil;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;

/**
 * 临时目录测试
 *
 * @author Ateng
 * @since 2026-05-13
 */
class TempDirectoryTest {

    @TempDir
    Path tempDir;

    /**
     * 测试在临时目录下创建多级目录和文件
     *
     * @throws Exception IO 异常
     */
    @Test
    void testCreateFileInTempDirectory() throws Exception {
        Path uploadDir = tempDir.resolve("upload").resolve("2026").resolve("05");
        Path filePath = uploadDir.resolve("demo.txt");

        Files.createDirectories(uploadDir);
        FileUtil.writeString("临时目录测试", filePath.toFile(), CharsetUtil.CHARSET_UTF_8);

        Assertions.assertTrue(Files.isDirectory(uploadDir));
        Assertions.assertTrue(Files.isRegularFile(filePath));
        Assertions.assertEquals("临时目录测试", Files.readString(filePath, CharsetUtil.CHARSET_UTF_8));
    }
}
```

测试文件操作时，不建议使用固定路径，例如 `/tmp/demo.txt` 或 `D:\\test\\demo.txt`。固定路径容易受操作系统、权限、并发测试和历史残留文件影响。

### 大文件性能验证

大文件性能验证用于确认文件读写在数据量较大时是否存在明显性能问题、内存问题或耗时过长问题。测试时应关注处理耗时、文件大小、吞吐量、内存占用和是否发生异常。

常见验证指标如下：

| 指标       | 说明                   |
| ---------- | ---------------------- |
| 文件大小   | 测试文件的数据量       |
| 处理耗时   | 文件读写总耗时         |
| 吞吐量     | 每秒处理多少 MB        |
| 内存占用   | 是否出现明显内存升高   |
| 结果正确性 | 文件大小或摘要是否一致 |

下面示例生成一个测试文件，然后使用缓冲流复制并统计耗时。

文件位置：`src/test/java/io/github/atengk/file/LargeFilePerformanceTest.java`

```java
package io.github.atengk.file;

import cn.hutool.core.io.FileUtil;
import cn.hutool.core.util.NumberUtil;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * 大文件性能测试
 *
 * @author Ateng
 * @since 2026-05-13
 */
class LargeFilePerformanceTest {

    @TempDir
    Path tempDir;

    /**
     * 缓冲区大小
     */
    private static final int BUFFER_SIZE = 1024 * 64;

    /**
     * 测试大文件复制性能
     *
     * @throws Exception IO 异常
     */
    @Test
    void testLargeFileCopyPerformance() throws Exception {
        Path source = tempDir.resolve("large-source.dat");
        Path target = tempDir.resolve("large-target.dat");

        createTestFile(source, 20 * 1024 * 1024);

        long startTime = System.nanoTime();

        try (
                BufferedInputStream inputStream = FileUtil.getInputStream(source.toFile());
                BufferedOutputStream outputStream = FileUtil.getOutputStream(target.toFile())
        ) {
            byte[] buffer = new byte[BUFFER_SIZE];
            int length;

            while ((length = inputStream.read(buffer)) != -1) {
                outputStream.write(buffer, 0, length);
            }

            outputStream.flush();
        }

        long costMillis = (System.nanoTime() - startTime) / 1_000_000;
        long sourceSize = Files.size(source);
        long targetSize = Files.size(target);
        double mb = NumberUtil.div(sourceSize, 1024 * 1024, 2);

        Assertions.assertEquals(sourceSize, targetSize);
        System.out.printf("大文件复制完成，文件大小：%s MB，耗时：%s ms%n", mb, costMillis);
    }

    /**
     * 创建指定大小的测试文件
     *
     * @param filePath 文件路径
     * @param size 文件大小
     * @throws Exception IO 异常
     */
    private void createTestFile(Path filePath, int size) throws Exception {
        byte[] buffer = new byte[BUFFER_SIZE];
        int written = 0;

        try (BufferedOutputStream outputStream = FileUtil.getOutputStream(filePath.toFile())) {
            while (written < size) {
                int writeSize = Math.min(buffer.length, size - written);
                outputStream.write(buffer, 0, writeSize);
                written += writeSize;
            }

            outputStream.flush();
        }
    }
}
```

大文件性能测试不建议在普通单元测试阶段频繁执行，可以放到集成测试、性能测试或本地专项测试中。CI 环境中如果执行大文件测试，应控制文件大小和耗时，避免影响流水线稳定性。
