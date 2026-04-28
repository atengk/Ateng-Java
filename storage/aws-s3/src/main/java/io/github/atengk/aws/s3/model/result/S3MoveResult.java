package io.github.atengk.aws.s3.model.result;

import java.time.Instant;

/**
 * S3 对象移动结果
 *
 * @param sourceBucketName 源存储桶名称
 * @param sourceKey        源对象 Key
 * @param targetBucketName 目标存储桶名称
 * @param targetKey        目标对象 Key
 * @param deletedSource    是否已删除源对象
 * @param moveTime         移动时间
 * @author Ateng
 * @since 2026-04-28
 */
public record S3MoveResult(
        String sourceBucketName,
        String sourceKey,
        String targetBucketName,
        String targetKey,
        Boolean deletedSource,
        Instant moveTime
) {
}
