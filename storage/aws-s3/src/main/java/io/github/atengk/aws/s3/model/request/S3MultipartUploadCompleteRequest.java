package io.github.atengk.aws.s3.model.request;

import io.github.atengk.aws.s3.model.result.S3MultipartUploadPartResult;

import java.util.List;

/**
 * S3 完成分片上传请求
 *
 * @param bucketName 存储桶名称
 * @param objectKey  对象 Key
 * @param uploadId   上传 ID
 * @param parts      分片上传结果列表
 * @author Ateng
 * @since 2026-04-28
 */
public record S3MultipartUploadCompleteRequest(
        String bucketName,
        String objectKey,
        String uploadId,
        List<S3MultipartUploadPartResult> parts
) {
}
