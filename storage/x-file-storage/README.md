# X File Storage

一行代码将文件存储到本地、FTP、SFTP、WebDAV、阿里云 OSS、华为云 OBS、七牛云 Kodo、腾讯云 COS、百度云 BOS、又拍云 USS、MinIO、 Amazon S3、GoogleCloud Storage、FastDFS、 Azure Blob Storage、Cloudflare R2、金山云 KS3、美团云 MSS、京东云 OSS、天翼云 OOS、移动 云EOS、沃云 OSS、 网易数帆 NOS、Ucloud US3、青云 QingStor、平安云 OBS、首云 OSS、IBM COS、其它兼容 S3 协议的存储平台。查看 所有支持的存储平台



## 基础配置

**添加依赖**

```xml
        <!-- X File Storage: 将文件存储各个存储平台 -->
        <dependency>
            <groupId>org.dromara.x-file-storage</groupId>
            <artifactId>x-file-storage-spring</artifactId>
            <version>${file-storage.version}</version>
        </dependency>
        <!-- MinIO 对象存储 -->
        <dependency>
            <groupId>io.minio</groupId>
            <artifactId>minio</artifactId>
            <version>${minio.version}</version>
        </dependency>
```

**编辑配置文件**

```yaml
---
# 设置文件和请求大小
spring:
  servlet:
    multipart:
      max-file-size: 10MB
      max-request-size: 10MB
      resolve-lazily: true # 开启 multipart 懒加载
# 文件存储配置
dromara:
  x-file-storage:
    default-platform: minio-1 #默认使用的存储平台
    thumbnail-suffix: ".min.jpg" #缩略图后缀，例如【.min.jpg】【.png】
    minio: # MinIO，由于 MinIO SDK 支持 AWS S3，其它兼容 AWS S3 协议的存储平台也都可配置在这里
      - platform: minio-1 # 存储平台标识
        enable-storage: true  # 启用存储
        access-key: admin
        secret-key: Admin@123
        end-point: "http://dev.minio.lingo.local"
        bucket-name: test
        domain: "http://dev.minio.lingo.local/test/" # 访问域名，注意“/”结尾，例如：http://minio.abc.com/abc/
        base-path: ateng/ # 基础路径
```

**启动服务**

```java
package local.ateng.java.storage;

import org.dromara.x.file.storage.spring.EnableFileStorage;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
@EnableFileStorage
public class XFileStorageApplication {

    public static void main(String[] args) {
        SpringApplication.run(XFileStorageApplication.class, args);
    }

}
```



## 上传文件

### 创建接口类

```java
import lombok.RequiredArgsConstructor;
import org.dromara.x.file.storage.core.FileInfo;
import org.dromara.x.file.storage.core.FileStorageService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

/**
 * X File Storage 演示
 *
 * @author 孔余
 * @email 2385569970@qq.com
 * @since 2025-01-07
 */
@RestController
@RequestMapping("/file")
@RequiredArgsConstructor(onConstructor = @__(@Autowired))
public class FileDetailController {
    private final FileStorageService fileStorageService;

}
```

### 基础上传

最简洁的方式，所有参数使用默认值，实际项目中不会用到这种情况。

```java
    /**
     * 上传文件，使用默认值
     */
    @PostMapping("/upload1")
    public FileInfo upload1(MultipartFile file) {
        return fileStorageService.of(file).upload();
    }
```

FileInfo的内容如下：

```json
{
	"id": null,
	"url": "http://dev.minio.lingo.local/test/ateng/677d27131dfb8b85121a9a1a.docx",
	"size": 735436,
	"filename": "677d27131dfb8b85121a9a1a.docx",
	"originalFilename": "我的文档.docx",
	"basePath": "ateng/",
	"path": "",
	"ext": "docx",
	"contentType": "application/vnd.openxmlformats-officedocument.wordprocessingml.document",
	"platform": "minio-1",
	"thUrl": null,
	"thFilename": null,
	"thSize": null,
	"thContentType": null,
	"objectId": null,
	"objectType": null,
	"metadata": {},
	"userMetadata": {},
	"thMetadata": {},
	"thUserMetadata": {},
	"attr": {},
	"fileAcl": null,
	"thFileAcl": null,
	"hashInfo": {},
	"uploadId": null,
	"uploadStatus": null,
	"createTime": "2025-01-07T13:07:31.025+00:00"
}
```

### 自定义路径

```java
    /**
     * 上传文件，自定义路径
     */
    @PostMapping("/upload2")
    public FileInfo upload2(MultipartFile file) {
        // 设置文件路径
        DateTime dateTime = DateUtil.date();
        String path = DateUtil.format(dateTime, "yyyy/MM/dd/");
        return fileStorageService
                .of(file)
                .setPath(path)
                .upload();
    }
```

**二进制数据**

```java
    /**
     * 上传文件，二进制数据byte[]
     * 这种情况需要自定义文件名和类型
     */
    @PostMapping("/upload3")
    public FileInfo upload3(MultipartFile file) throws IOException {
        // 假设这里是得到了一个文件的二进制，可以是流或者其他途径
        byte[] bytes = file.getBytes();
        // FileTypeUtil 是 Hutool 提供的工具类，它通过分析文件头的魔数（magic number）来判断文件的类型
        String type = FileTypeUtil.getType(new ByteArrayInputStream(bytes), true);
        // 设置文件路径
        DateTime dateTime = DateUtil.date();
        String path = DateUtil.format(dateTime, "yyyy/MM/dd/");
        // 设置文件名
        String dateTimeStr = DateUtil.format(dateTime, "yyyyMMdd-HHmmss");
        String filename = StrUtil.format("{}.{}", dateTimeStr, type);
        return fileStorageService
                .of(file)
                .setPath(path)
                .setSaveFilename(filename)
                .setOriginalFilename(filename)
                .upload();
    }
```

### 上传图片

这里上传图片做了压缩

```java
    /**
     * 上传图片，成功返回文件信息
     * 图片处理使用的是 https://github.com/coobird/thumbnailator
     * 会将图片压缩到1000像素
     */
    @PostMapping("/upload-image")
    public FileInfo uploadImage(MultipartFile file) {
        return fileStorageService.of(file)
                .image(img -> img.size(1000, 1000))  //将图片大小调整到 1000*1000
                .thumbnail(th -> th.size(200, 200))  //再生成一张 200*200 的缩略图
                .upload();
    }
```



## 收到初始化存储平台

参考代码

```java
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.dromara.x.file.storage.core.FileStorageProperties;
import org.dromara.x.file.storage.core.FileStorageService;
import org.dromara.x.file.storage.core.FileStorageServiceBuilder;
import org.dromara.x.file.storage.core.platform.FileStorage;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.Collections;
import java.util.concurrent.CopyOnWriteArrayList;

@Component
@RequiredArgsConstructor(onConstructor = @__(@Autowired))
@Slf4j
public class TestTask {
    private final FileStorageService fileStorageService;//注入实列
    private CopyOnWriteArrayList<FileStorage> fileStorageList;


    @Scheduled(cron = "0 * * * * ?")
    public void checkFtpHealth() {
        this.initFileStorage();
        String platform = fileStorageList.get(0).getPlatform();
        int size = fileStorageService.listFiles().setPlatform(platform).listFiles().getFileList().size();
        log.info("访问FTP成功，目录下文件数量：{}", size);
        this.stopFileStorage();
    }

    public void initFileStorage() {
        fileStorageList = fileStorageService.getFileStorageList();
        FileStorageProperties.FtpConfig config = new FileStorageProperties.FtpConfig();
        config.setPlatform("vsftp");
        config.setHost("192.168.1.13");
        config.setPort(21);
        config.setUser("admin");
        config.setPassword("Admin@123");
        config.setDomain("ftp://192.168.1.13/");
        config.setBasePath("data/");
        config.setStoragePath("/");
        config.setIsActive(false);
        config.setConnectionTimeout(60000);
        config.setSoTimeout(60000);
        fileStorageList.addAll(FileStorageServiceBuilder.buildFtpFileStorage(Collections.singletonList(config),null));
    }

    public void stopFileStorage() {
        //删除
        FileStorage myStorage = fileStorageService.getFileStorage("vsftp");
        fileStorageList.remove(myStorage);
        myStorage.close();//释放资源
    }

}
```



