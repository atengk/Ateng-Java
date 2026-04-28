package io.github.atengk.aws.s3.model.request;

import io.github.atengk.aws.s3.model.enums.S3ObjectAcl;
import io.github.atengk.aws.s3.model.enums.S3ServerSideEncryption;
import io.github.atengk.aws.s3.model.enums.S3StorageClass;

import java.util.Map;

/**
 * S3 分片上传初始化请求
 *
 * @param bucketName           存储桶名称
 * @param objectKey            对象 Key
 * @param contentType          内容类型
 * @param metadata             自定义元数据
 * @param tags                 对象标签
 * @param acl                  对象 ACL
 * @param storageClass         存储类型
 * @param serverSideEncryption 服务端加密方式
 * @param kmsKeyId             KMS Key ID
 * @author Ateng
 * @since 2026-04-28
 */
public record S3MultipartUploadInitRequest(
        String bucketName,
        String objectKey,
        String contentType,
        Map<String, String> metadata,
        Map<String, String> tags,
        S3ObjectAcl acl,
        S3StorageClass storageClass,
        S3ServerSideEncryption serverSideEncryption,
        String kmsKeyId
) {
}
