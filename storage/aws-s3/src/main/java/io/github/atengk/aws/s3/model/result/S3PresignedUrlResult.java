package io.github.atengk.aws.s3.model.result;

import io.github.atengk.aws.s3.model.enums.S3PresignedUrlMethod;

import java.net.URI;
import java.time.Instant;

/**
 * S3 预签名 URL 结果
 *
 * @param bucketName 存储桶名称
 * @param objectKey  对象 Key
 * @param method     请求方法
 * @param url        预签名 URL
 * @param expireTime 过期时间
 * @author Ateng
 * @since 2026-04-28
 */
public record S3PresignedUrlResult(
        String bucketName,
        String objectKey,
        S3PresignedUrlMethod method,
        URI url,
        Instant expireTime
) {
}
