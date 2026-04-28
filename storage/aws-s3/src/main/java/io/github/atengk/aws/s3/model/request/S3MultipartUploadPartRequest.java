package io.github.atengk.aws.s3.model.request;

import java.io.InputStream;

/**
 * S3 分片上传请求
 *
 * @param bucketName    存储桶名称
 * @param objectKey     对象 Key
 * @param uploadId      上传 ID
 * @param partNumber    分片编号
 * @param inputStream   分片输入流
 * @param contentLength 分片内容长度
 * @author Ateng
 * @since 2026-04-28
 */
public record S3MultipartUploadPartRequest(
        String bucketName,
        String objectKey,
        String uploadId,
        Integer partNumber,
        InputStream inputStream,
        Long contentLength
) {
}
