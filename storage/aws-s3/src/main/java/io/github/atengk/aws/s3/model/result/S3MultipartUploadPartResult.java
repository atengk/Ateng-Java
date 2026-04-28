package io.github.atengk.aws.s3.model.result;

/**
 * S3 分片上传结果
 *
 * @param partNumber 分片编号
 * @param eTag       ETag
 * @param size       分片大小
 * @author Ateng
 * @since 2026-04-28
 */
public record S3MultipartUploadPartResult(
        Integer partNumber,
        String eTag,
        Long size
) {
}
