package io.github.atengk.aws.s3.model.request;

/**
 * S3 对象移动请求
 *
 * @param sourceBucketName 源存储桶名称
 * @param sourceKey        源对象 Key
 * @param targetBucketName 目标存储桶名称
 * @param targetKey        目标对象 Key
 * @param overwrite        是否覆盖目标对象
 * @author Ateng
 * @since 2026-04-28
 */
public record S3MoveRequest(
        String sourceBucketName,
        String sourceKey,
        String targetBucketName,
        String targetKey,
        Boolean overwrite
) {
}
