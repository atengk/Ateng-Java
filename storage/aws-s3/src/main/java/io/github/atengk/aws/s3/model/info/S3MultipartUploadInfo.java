package io.github.atengk.aws.s3.model.info;

import java.time.Instant;

/**
 * S3 分片上传任务信息
 *
 * @param bucketName 存储桶名称
 * @param objectKey  对象 Key
 * @param uploadId   上传 ID
 * @param initiated  初始化时间
 * @author Ateng
 * @since 2026-04-28
 */
public record S3MultipartUploadInfo(
        String bucketName,
        String objectKey,
        String uploadId,
        Instant initiated
) {
}
