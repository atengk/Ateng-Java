package io.github.atengk.aws.s3.controller;

import cn.hutool.core.lang.Dict;
import cn.hutool.core.util.StrUtil;
import io.github.atengk.aws.s3.service.S3Service;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * S3 对象工具方法接口
 *
 * @author Ateng
 * @since 2026-04-28
 */
@Slf4j
@Validated
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/s3/objects/tools")
public class S3ObjectToolController {

    private final S3Service s3Service;

    /**
     * 规范化对象 Key
     * <p>
     * 会去除首部 /，替换反斜杠为正斜杠，并压缩连续斜杠。
     * <p>
     * curl 使用示例：
     * curl -X GET "http://localhost:14002/api/s3/objects/tools/normalize-key?objectKey=/upload//2026\\04\\28/test.txt"
     *
     * @param objectKey 原始对象 Key
     * @return 规范化后的对象 Key
     */
    @GetMapping("/normalize-key")
    public Dict normalizeObjectKey(@RequestParam String objectKey) {
        String normalizedObjectKey = s3Service.normalizeObjectKey(objectKey);

        log.info("规范化 S3 对象 Key 成功，objectKey={}，normalizedObjectKey={}",
                objectKey, normalizedObjectKey);

        return Dict.create()
                .set("objectKey", objectKey)
                .set("normalizedObjectKey", normalizedObjectKey);
    }

    /**
     * 构建对象 Key
     * <p>
     * directory 为空时只使用 filename。
     * <p>
     * curl 使用示例：
     * curl -X GET "http://localhost:14002/api/s3/objects/tools/build-key?directory=upload/2026/04/28&filename=test.txt"
     * <p>
     * curl 使用示例：
     * curl -X GET "http://localhost:14002/api/s3/objects/tools/build-key?filename=test.txt"
     *
     * @param directory 目录，可为空
     * @param filename  文件名
     * @return 构建后的对象 Key
     */
    @GetMapping("/build-key")
    public Dict buildObjectKey(@RequestParam(required = false) String directory,
                               @RequestParam String filename) {
        String objectKey = s3Service.buildObjectKey(directory, filename);

        log.info("构建 S3 对象 Key 成功，directory={}，filename={}，objectKey={}",
                StrUtil.blankToDefault(directory, "空目录"),
                filename,
                objectKey);

        return Dict.create()
                .set("directory", directory)
                .set("filename", filename)
                .set("objectKey", objectKey);
    }

    /**
     * 获取对象 Key 中的文件名
     * <p>
     * curl 使用示例：
     * curl -X GET "http://localhost:14002/api/s3/objects/tools/filename?objectKey=upload/2026/04/28/test.txt"
     *
     * @param objectKey 对象 Key
     * @return 文件名
     */
    @GetMapping("/filename")
    public Dict getFilename(@RequestParam String objectKey) {
        String filename = s3Service.getFilename(objectKey);

        log.info("解析 S3 对象文件名成功，objectKey={}，filename={}", objectKey, filename);

        return Dict.create()
                .set("objectKey", objectKey)
                .set("filename", filename);
    }

    /**
     * 获取对象 Key 中的扩展名
     * <p>
     * 返回值不包含点号。
     * <p>
     * curl 使用示例：
     * curl -X GET "http://localhost:14002/api/s3/objects/tools/extension?objectKey=upload/2026/04/28/test.txt"
     * <p>
     * curl 使用示例：
     * curl -X GET "http://localhost:14002/api/s3/objects/tools/extension?objectKey=upload/2026/04/28/README"
     *
     * @param objectKey 对象 Key
     * @return 扩展名
     */
    @GetMapping("/extension")
    public Dict getExtension(@RequestParam String objectKey) {
        String extension = s3Service.getExtension(objectKey);

        log.info("解析 S3 对象扩展名成功，objectKey={}，extension={}",
                objectKey,
                StrUtil.blankToDefault(extension, "无扩展名"));

        return Dict.create()
                .set("objectKey", objectKey)
                .set("extension", extension);
    }

    /**
     * 获取默认存储桶下对象的公开访问 URL
     * <p>
     * 该 URL 不是预签名 URL。
     * 对象是否可访问取决于桶策略、对象 ACL、网关或 CDN 配置。
     * <p>
     * curl 使用示例：
     * curl -X GET "http://localhost:14002/api/s3/objects/tools/public-url?objectKey=upload/2026/04/28/test.txt"
     *
     * @param objectKey 对象 Key
     * @return 公开访问 URL
     */
    @GetMapping("/public-url")
    public Dict getPublicUrl(@RequestParam String objectKey) {
        String bucketName = s3Service.getDefaultBucketName();
        String publicUrl = s3Service.getPublicUrl(objectKey);

        log.info("生成 S3 默认存储桶对象公开访问 URL 成功，bucketName={}，objectKey={}",
                bucketName, objectKey);

        return Dict.create()
                .set("bucketName", bucketName)
                .set("objectKey", objectKey)
                .set("url", publicUrl);
    }

    /**
     * 获取指定存储桶下对象的公开访问 URL
     * <p>
     * 该 URL 不是预签名 URL。
     * 对象是否可访问取决于桶策略、对象 ACL、网关或 CDN 配置。
     * <p>
     * curl 使用示例：
     * curl -X GET "http://localhost:14002/api/s3/objects/tools/public-url/bucket?bucketName=data&objectKey=upload/2026/04/28/test.txt"
     *
     * @param bucketName 存储桶名称
     * @param objectKey  对象 Key
     * @return 公开访问 URL
     */
    @GetMapping("/public-url/bucket")
    public Dict getPublicUrlByBucket(@RequestParam String bucketName,
                                     @RequestParam String objectKey) {
        String resolvedBucketName = s3Service.resolveBucketName(bucketName);
        String publicUrl = s3Service.getPublicUrl(resolvedBucketName, objectKey);

        log.info("生成 S3 指定存储桶对象公开访问 URL 成功，bucketName={}，objectKey={}",
                resolvedBucketName, objectKey);

        return Dict.create()
                .set("bucketName", resolvedBucketName)
                .set("objectKey", objectKey)
                .set("url", publicUrl);
    }
}