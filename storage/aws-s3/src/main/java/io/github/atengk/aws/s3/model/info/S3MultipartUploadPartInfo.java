package io.github.atengk.aws.s3.model.info;

import java.time.Instant;

/**
 * S3 已上传分片信息
 *
 * @param partNumber  分片编号
 * @param eTag        ETag
 * @param size        分片大小
 * @param lastModified 最后修改时间
 * @author Ateng
 * @since 2026-04-28
 */
public record S3MultipartUploadPartInfo(
        Integer partNumber,
        String eTag,
        Long size,
        Instant lastModified
) {
}
