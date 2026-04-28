package io.github.atengk.aws.s3.model.request;

/**
 * S3 对象下载请求
 *
 * @param bucketName 存储桶名称
 * @param objectKey  对象 Key
 * @param versionId  版本 ID
 * @param rangeStart Range 起始字节
 * @param rangeEnd   Range 结束字节
 * @author Ateng
 * @since 2026-04-28
 */
public record S3DownloadRequest(
        String bucketName,
        String objectKey,
        String versionId,
        Long rangeStart,
        Long rangeEnd
) {
}
