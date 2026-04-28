package io.github.atengk.aws.s3.model.result;

/**
 * S3 分片上传初始化结果
 *
 * @param bucketName 存储桶名称
 * @param objectKey  对象 Key
 * @param uploadId   上传 ID
 * @author Ateng
 * @since 2026-04-28
 */
public record S3MultipartUploadInitResult(
        String bucketName,
        String objectKey,
        String uploadId
) {
}
