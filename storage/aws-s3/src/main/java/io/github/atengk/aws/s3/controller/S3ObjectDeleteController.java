package io.github.atengk.aws.s3.controller;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.lang.Assert;
import cn.hutool.core.lang.Dict;
import cn.hutool.core.util.BooleanUtil;
import cn.hutool.core.util.StrUtil;
import io.github.atengk.aws.s3.model.result.S3DeleteResult;
import io.github.atengk.aws.s3.service.S3Service;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * S3 对象删除接口
 *
 * @author Ateng
 * @since 2026-04-28
 */
@Slf4j
@Validated
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/s3/objects/delete")
public class S3ObjectDeleteController {

    private final S3Service s3Service;

    /**
     * 删除单个对象
     * <p>
     * bucketName 不传时使用默认存储桶。
     * <p>
     * curl 使用示例：
     * curl -X DELETE "http://localhost:14002/api/s3/objects/delete?objectKey=upload/2026/04/28/test.txt"
     * <p>
     * curl 使用示例：
     * curl -X DELETE "http://localhost:14002/api/s3/objects/delete?bucketName=data&objectKey=upload/2026/04/28/test.txt"
     *
     * @param bucketName 存储桶名称，可为空
     * @param objectKey  对象 Key
     * @return 删除结果
     */
    @DeleteMapping
    public Dict delete(@RequestParam(required = false) String bucketName,
                       @RequestParam String objectKey) {
        String resolvedBucketName = s3Service.resolveBucketName(bucketName);
        s3Service.delete(resolvedBucketName, objectKey);

        log.info("删除 S3 单个对象成功，bucketName={}，objectKey={}", resolvedBucketName, objectKey);

        return Dict.create()
                .set("bucketName", resolvedBucketName)
                .set("objectKey", objectKey)
                .set("deleted", true);
    }

    /**
     * 批量删除对象
     * <p>
     * bucketName 不传时使用默认存储桶。
     * objectKeys 使用重复参数传递。
     * <p>
     * curl 使用示例：
     * curl -X DELETE "http://localhost:14002/api/s3/objects/delete/batch?objectKeys=batch/a.txt&objectKeys=batch/b.txt"
     * <p>
     * curl 使用示例：
     * curl -X DELETE "http://localhost:14002/api/s3/objects/delete/batch?bucketName=data&objectKeys=batch/a.txt&objectKeys=batch/b.txt"
     *
     * @param bucketName 存储桶名称，可为空
     * @param objectKeys 对象 Key 列表
     * @return 删除结果
     */
    @DeleteMapping("/batch")
    public Dict deleteBatch(@RequestParam(required = false) String bucketName,
                            @RequestParam List<String> objectKeys) {
        Assert.notEmpty(objectKeys, "批量删除对象 Key 不能为空");

        String resolvedBucketName = s3Service.resolveBucketName(bucketName);
        s3Service.deleteBatch(resolvedBucketName, objectKeys);

        log.info("批量删除 S3 对象成功，bucketName={}，count={}", resolvedBucketName, objectKeys.size());

        return Dict.create()
                .set("bucketName", resolvedBucketName)
                .set("objectKeys", objectKeys)
                .set("count", objectKeys.size())
                .set("deleted", true);
    }

    /**
     * 批量删除对象，并返回详细删除结果
     * <p>
     * bucketName 不传时使用默认存储桶。
     * objectKeys 使用重复参数传递。
     * <p>
     * curl 使用示例：
     * curl -X DELETE "http://localhost:14002/api/s3/objects/delete/batch/detailed?objectKeys=batch/a.txt&objectKeys=batch/b.txt"
     * <p>
     * curl 使用示例：
     * curl -X DELETE "http://localhost:14002/api/s3/objects/delete/batch/detailed?bucketName=data&objectKeys=batch/a.txt&objectKeys=batch/b.txt"
     *
     * @param bucketName 存储桶名称，可为空
     * @param objectKeys 对象 Key 列表
     * @return 详细删除结果
     */
    @DeleteMapping("/batch/detailed")
    public S3DeleteResult deleteBatchDetailed(@RequestParam(required = false) String bucketName,
                                              @RequestParam List<String> objectKeys) {
        Assert.notEmpty(objectKeys, "批量删除对象 Key 不能为空");

        String resolvedBucketName = s3Service.resolveBucketName(bucketName);
        S3DeleteResult result = s3Service.deleteBatchDetailed(resolvedBucketName, objectKeys);

        log.info("批量删除 S3 对象完成，bucketName={}，deletedCount={}，errorCount={}",
                resolvedBucketName, result.deletedCount(), result.errorCount());

        return result;
    }

    /**
     * 按前缀删除对象
     * <p>
     * bucketName 不传时使用默认存储桶。
     * 该接口会删除 prefix 匹配到的全部对象，必须传 confirm=true。
     * <p>
     * curl 使用示例：
     * curl -X DELETE "http://localhost:14002/api/s3/objects/delete/prefix?prefix=temp/&confirm=true"
     * <p>
     * curl 使用示例：
     * curl -X DELETE "http://localhost:14002/api/s3/objects/delete/prefix?bucketName=data&prefix=temp/&confirm=true"
     *
     * @param bucketName 存储桶名称，可为空
     * @param prefix     对象 Key 前缀
     * @param confirm    是否确认删除
     * @return 删除结果
     */
    @DeleteMapping("/prefix")
    public Dict deleteByPrefix(@RequestParam(required = false) String bucketName,
                               @RequestParam String prefix,
                               @RequestParam(required = false) Boolean confirm) {
        assertDeletePrefixConfirmed(prefix, confirm);

        String resolvedBucketName = s3Service.resolveBucketName(bucketName);
        Long deletedCount = s3Service.deleteByPrefix(resolvedBucketName, prefix);

        log.info("按前缀删除 S3 对象成功，bucketName={}，prefix={}，deletedCount={}",
                resolvedBucketName, prefix, deletedCount);

        return Dict.create()
                .set("bucketName", resolvedBucketName)
                .set("prefix", prefix)
                .set("deletedCount", deletedCount)
                .set("deleted", true);
    }

    /**
     * 按前缀删除对象，并返回详细删除结果
     * <p>
     * bucketName 不传时使用默认存储桶。
     * 该接口会删除 prefix 匹配到的全部对象，必须传 confirm=true。
     * <p>
     * curl 使用示例：
     * curl -X DELETE "http://localhost:14002/api/s3/objects/delete/prefix/detailed?prefix=temp/&confirm=true"
     * <p>
     * curl 使用示例：
     * curl -X DELETE "http://localhost:14002/api/s3/objects/delete/prefix/detailed?bucketName=data&prefix=temp/&confirm=true"
     *
     * @param bucketName 存储桶名称，可为空
     * @param prefix     对象 Key 前缀
     * @param confirm    是否确认删除
     * @return 详细删除结果
     */
    @DeleteMapping("/prefix/detailed")
    public S3DeleteResult deleteByPrefixDetailed(@RequestParam(required = false) String bucketName,
                                                 @RequestParam String prefix,
                                                 @RequestParam(required = false) Boolean confirm) {
        assertDeletePrefixConfirmed(prefix, confirm);

        String resolvedBucketName = s3Service.resolveBucketName(bucketName);
        S3DeleteResult result = s3Service.deleteByPrefixDetailed(resolvedBucketName, prefix);

        log.info("按前缀删除 S3 对象完成，bucketName={}，prefix={}，deletedCount={}，errorCount={}",
                resolvedBucketName, prefix, result.deletedCount(), result.errorCount());

        return result;
    }

    /**
     * 删除指定版本对象
     * <p>
     * bucketName 必传。
     * <p>
     * curl 使用示例：
     * curl -X DELETE "http://localhost:14002/api/s3/objects/delete/version?bucketName=data&objectKey=archive/report.pdf&versionId=your-version-id"
     *
     * @param bucketName 存储桶名称
     * @param objectKey  对象 Key
     * @param versionId  版本 ID
     * @return 删除结果
     */
    @DeleteMapping("/version")
    public Dict deleteVersion(@RequestParam String bucketName,
                              @RequestParam String objectKey,
                              @RequestParam String versionId) {
        String resolvedBucketName = s3Service.resolveBucketName(bucketName);
        s3Service.deleteVersion(resolvedBucketName, objectKey, versionId);

        log.info("删除 S3 指定版本对象成功，bucketName={}，objectKey={}，versionId={}",
                resolvedBucketName, objectKey, versionId);

        return Dict.create()
                .set("bucketName", resolvedBucketName)
                .set("objectKey", objectKey)
                .set("versionId", versionId)
                .set("deleted", true);
    }

    /**
     * 校验按前缀删除确认参数
     *
     * @param prefix  对象 Key 前缀
     * @param confirm 是否确认删除
     */
    private void assertDeletePrefixConfirmed(String prefix, Boolean confirm) {
        Assert.notBlank(prefix, "按前缀删除时 prefix 不能为空");
        Assert.isTrue(BooleanUtil.isTrue(confirm), "按前缀删除属于高风险操作，必须传 confirm=true");

        String normalizedPrefix = StrUtil.trim(prefix);
        Assert.isTrue(!StrUtil.equals(normalizedPrefix, "/"), "禁止使用根路径前缀删除对象");
        Assert.isTrue(!StrUtil.equals(normalizedPrefix, "*"), "禁止使用通配符前缀删除对象");
        Assert.isTrue(CollUtil.newArrayList(normalizedPrefix.split("/")).stream().anyMatch(StrUtil::isNotBlank),
                "按前缀删除时 prefix 不能是空路径");
    }
}