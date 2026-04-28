package io.github.atengk.aws.s3.controller;

import cn.hutool.core.lang.Dict;
import cn.hutool.core.util.StrUtil;
import io.github.atengk.aws.s3.model.info.S3ObjectInfo;
import io.github.atengk.aws.s3.service.S3Service;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * S3 对象基础操作接口
 *
 * @author Ateng
 * @since 2026-04-28
 */
@Slf4j
@Validated
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/s3/objects/basic")
public class S3ObjectBasicController {

    private final S3Service s3Service;

    /**
     * 判断对象是否存在
     * <p>
     * bucketName 不传时使用默认存储桶。
     * <p>
     * curl 使用示例：
     * curl -X GET "http://localhost:14002/api/s3/objects/basic/exists?objectKey=upload/2026/04/28/test.txt"
     * <p>
     * curl 使用示例：
     * curl -X GET "http://localhost:14002/api/s3/objects/basic/exists?bucketName=data&objectKey=upload/2026/04/28/test.txt"
     *
     * @param bucketName 存储桶名称，可为空
     * @param objectKey  对象 Key
     * @return 是否存在
     */
    @GetMapping("/exists")
    public Dict objectExists(@RequestParam(required = false) String bucketName,
                             @RequestParam String objectKey) {
        String resolvedBucketName = s3Service.resolveBucketName(bucketName);
        boolean exists = s3Service.objectExists(resolvedBucketName, objectKey);

        log.info("检查 S3 对象是否存在完成，bucketName={}，objectKey={}，exists={}",
                resolvedBucketName, objectKey, exists);

        return Dict.create()
                .set("bucketName", resolvedBucketName)
                .set("objectKey", objectKey)
                .set("exists", exists);
    }

    /**
     * 获取对象基础信息
     * <p>
     * bucketName 不传时使用默认存储桶。
     * <p>
     * curl 使用示例：
     * curl -X GET "http://localhost:14002/api/s3/objects/basic/info?objectKey=upload/2026/04/28/test.txt"
     * <p>
     * curl 使用示例：
     * curl -X GET "http://localhost:14002/api/s3/objects/basic/info?bucketName=data&objectKey=upload/2026/04/28/test.txt"
     *
     * @param bucketName 存储桶名称，可为空
     * @param objectKey  对象 Key
     * @return 对象基础信息
     */
    @GetMapping("/info")
    public S3ObjectInfo getObjectInfo(@RequestParam(required = false) String bucketName,
                                      @RequestParam String objectKey) {
        String resolvedBucketName = s3Service.resolveBucketName(bucketName);
        S3ObjectInfo objectInfo = s3Service.getObjectInfo(resolvedBucketName, objectKey);

        log.info("获取 S3 对象基础信息成功，bucketName={}，objectKey={}，size={}，contentType={}",
                resolvedBucketName, objectKey, objectInfo.size(), objectInfo.contentType());

        return objectInfo;
    }

    /**
     * 获取对象大小
     * <p>
     * bucketName 不传时使用默认存储桶。
     * <p>
     * curl 使用示例：
     * curl -X GET "http://localhost:14002/api/s3/objects/basic/size?objectKey=upload/2026/04/28/test.txt"
     * <p>
     * curl 使用示例：
     * curl -X GET "http://localhost:14002/api/s3/objects/basic/size?bucketName=data&objectKey=upload/2026/04/28/test.txt"
     *
     * @param bucketName 存储桶名称，可为空
     * @param objectKey  对象 Key
     * @return 对象大小，单位字节
     */
    @GetMapping("/size")
    public Dict getObjectSize(@RequestParam(required = false) String bucketName,
                              @RequestParam String objectKey) {
        String resolvedBucketName = s3Service.resolveBucketName(bucketName);
        Long size = s3Service.getObjectSize(resolvedBucketName, objectKey);

        log.info("获取 S3 对象大小成功，bucketName={}，objectKey={}，size={}",
                resolvedBucketName, objectKey, size);

        return Dict.create()
                .set("bucketName", resolvedBucketName)
                .set("objectKey", objectKey)
                .set("size", size);
    }

    /**
     * 获取对象 Content-Type
     * <p>
     * bucketName 不传时使用默认存储桶。
     * <p>
     * curl 使用示例：
     * curl -X GET "http://localhost:14002/api/s3/objects/basic/content-type?objectKey=upload/2026/04/28/test.txt"
     * <p>
     * curl 使用示例：
     * curl -X GET "http://localhost:14002/api/s3/objects/basic/content-type?bucketName=data&objectKey=upload/2026/04/28/test.txt"
     *
     * @param bucketName 存储桶名称，可为空
     * @param objectKey  对象 Key
     * @return 对象 Content-Type
     */
    @GetMapping("/content-type")
    public Dict getObjectContentType(@RequestParam(required = false) String bucketName,
                                     @RequestParam String objectKey) {
        String resolvedBucketName = s3Service.resolveBucketName(bucketName);
        String contentType = s3Service.getObjectContentType(resolvedBucketName, objectKey);

        log.info("获取 S3 对象 Content-Type 成功，bucketName={}，objectKey={}，contentType={}",
                resolvedBucketName,
                objectKey,
                StrUtil.blankToDefault(contentType, "未设置"));

        return Dict.create()
                .set("bucketName", resolvedBucketName)
                .set("objectKey", objectKey)
                .set("contentType", contentType);
    }
}