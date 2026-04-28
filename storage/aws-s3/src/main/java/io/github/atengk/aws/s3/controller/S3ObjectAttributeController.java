package io.github.atengk.aws.s3.controller;

import cn.hutool.core.lang.Dict;
import cn.hutool.core.util.StrUtil;
import io.github.atengk.aws.s3.model.enums.S3ObjectAcl;
import io.github.atengk.aws.s3.model.enums.S3StorageClass;
import io.github.atengk.aws.s3.service.S3Service;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.Arrays;
import java.util.stream.Collectors;

/**
 * S3 对象访问控制与存储属性接口
 *
 * @author Ateng
 * @since 2026-04-28
 */
@Slf4j
@Validated
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/s3/objects/attributes")
public class S3ObjectAttributeController {

    private final S3Service s3Service;

    /**
     * 设置对象 ACL
     * <p>
     * bucketName 不传时使用默认存储桶。
     * 常用 acl：PRIVATE、PUBLIC_READ、BUCKET_OWNER_FULL_CONTROL。
     * <p>
     * curl 使用示例：
     * curl -X PUT "http://localhost:14002/api/s3/objects/attributes/acl" \
     * -d "objectKey=upload/2026/04/28/test.txt" \
     * -d "acl=PRIVATE"
     * <p>
     * curl 使用示例：
     * curl -X PUT "http://localhost:14002/api/s3/objects/attributes/acl" \
     * -d "bucketName=data" \
     * -d "objectKey=upload/2026/04/28/test.txt" \
     * -d "acl=PUBLIC_READ"
     *
     * @param bucketName 存储桶名称，可为空
     * @param objectKey  对象 Key
     * @param acl        对象 ACL
     * @return 设置结果
     */
    @PutMapping(value = "/acl", consumes = MediaType.APPLICATION_FORM_URLENCODED_VALUE)
    public Dict setObjectAcl(@RequestParam(required = false) String bucketName,
                             @RequestParam String objectKey,
                             @RequestParam String acl) {
        String resolvedBucketName = s3Service.resolveBucketName(bucketName);
        S3ObjectAcl resolvedAcl = parseEnum(S3ObjectAcl.class, acl, "S3 对象 ACL 不合法");

        s3Service.setObjectAcl(resolvedBucketName, objectKey, resolvedAcl);

        log.info("设置 S3 对象 ACL 成功，bucketName={}，objectKey={}，acl={}",
                resolvedBucketName, objectKey, resolvedAcl);

        return Dict.create()
                .set("bucketName", resolvedBucketName)
                .set("objectKey", objectKey)
                .set("acl", resolvedAcl)
                .set("updated", true);
    }

    /**
     * 修改对象存储类型
     * <p>
     * bucketName 不传时使用默认存储桶。
     * 常用 storageClass：STANDARD、INTELLIGENT_TIERING、STANDARD_IA、ONEZONE_IA、GLACIER、DEEP_ARCHIVE。
     * <p>
     * curl 使用示例：
     * curl -X PUT "http://localhost:14002/api/s3/objects/attributes/storage-class" \
     * -d "objectKey=upload/2026/04/28/test.txt" \
     * -d "storageClass=STANDARD_IA"
     * <p>
     * curl 使用示例：
     * curl -X PUT "http://localhost:14002/api/s3/objects/attributes/storage-class" \
     * -d "bucketName=data" \
     * -d "objectKey=archive/report.pdf" \
     * -d "storageClass=GLACIER"
     *
     * @param bucketName   存储桶名称，可为空
     * @param objectKey    对象 Key
     * @param storageClass 存储类型
     * @return 修改结果
     */
    @PutMapping(value = "/storage-class", consumes = MediaType.APPLICATION_FORM_URLENCODED_VALUE)
    public Dict changeStorageClass(@RequestParam(required = false) String bucketName,
                                   @RequestParam String objectKey,
                                   @RequestParam String storageClass) {
        String resolvedBucketName = s3Service.resolveBucketName(bucketName);
        S3StorageClass resolvedStorageClass = parseEnum(
                S3StorageClass.class,
                storageClass,
                "S3 存储类型不合法"
        );

        s3Service.changeStorageClass(resolvedBucketName, objectKey, resolvedStorageClass);

        log.info("修改 S3 对象存储类型成功，bucketName={}，objectKey={}，storageClass={}",
                resolvedBucketName, objectKey, resolvedStorageClass);

        return Dict.create()
                .set("bucketName", resolvedBucketName)
                .set("objectKey", objectKey)
                .set("storageClass", resolvedStorageClass)
                .set("updated", true);
    }

    /**
     * 发起归档对象恢复任务
     * <p>
     * bucketName 不传时使用默认存储桶。
     * days 表示恢复后的临时副本保留天数，不传时由 Service 使用默认值。
     * 适用于 GLACIER、DEEP_ARCHIVE 等归档存储类型对象。
     * <p>
     * curl 使用示例：
     * curl -X POST "http://localhost:14002/api/s3/objects/attributes/restore" \
     * -d "objectKey=archive/report.pdf" \
     * -d "days=7"
     * <p>
     * curl 使用示例：
     * curl -X POST "http://localhost:14002/api/s3/objects/attributes/restore" \
     * -d "bucketName=data" \
     * -d "objectKey=archive/report.pdf" \
     * -d "days=3"
     *
     * @param bucketName 存储桶名称，可为空
     * @param objectKey  对象 Key
     * @param days       恢复副本保留天数，可为空
     * @return 恢复任务发起结果
     */
    @PostMapping(value = "/restore", consumes = MediaType.APPLICATION_FORM_URLENCODED_VALUE)
    public Dict restoreArchiveObject(@RequestParam(required = false) String bucketName,
                                     @RequestParam String objectKey,
                                     @RequestParam(required = false) Integer days) {
        String resolvedBucketName = s3Service.resolveBucketName(bucketName);

        s3Service.restoreArchiveObject(resolvedBucketName, objectKey, days);

        log.info("发起 S3 归档对象恢复任务成功，bucketName={}，objectKey={}，days={}",
                resolvedBucketName,
                objectKey,
                days == null ? "默认值" : days);

        return Dict.create()
                .set("bucketName", resolvedBucketName)
                .set("objectKey", objectKey)
                .set("days", days)
                .set("restored", true);
    }

    /**
     * 解析枚举
     *
     * @param enumClass    枚举类型
     * @param value        枚举值
     * @param errorMessage 错误信息
     * @param <E>          枚举泛型
     * @return 枚举值
     */
    private <E extends Enum<E>> E parseEnum(Class<E> enumClass, String value, String errorMessage) {
        if (StrUtil.isBlank(value)) {
            return null;
        }

        try {
            return Enum.valueOf(enumClass, StrUtil.trim(value).toUpperCase());
        } catch (IllegalArgumentException e) {
            String enumValues = Arrays.stream(enumClass.getEnumConstants())
                    .map(Enum::name)
                    .collect(Collectors.joining(","));

            throw new IllegalArgumentException(errorMessage + "，value=" + value + "，可选值：" + enumValues, e);
        }
    }
}