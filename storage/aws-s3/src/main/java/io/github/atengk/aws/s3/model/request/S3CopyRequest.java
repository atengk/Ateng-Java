package io.github.atengk.aws.s3.model.request;

import io.github.atengk.aws.s3.model.enums.S3ObjectAcl;
import io.github.atengk.aws.s3.model.enums.S3ServerSideEncryption;
import io.github.atengk.aws.s3.model.enums.S3StorageClass;

import java.util.Map;

/**
 * S3 对象复制请求
 *
 * @param sourceBucketName     源存储桶名称
 * @param sourceKey            源对象 Key
 * @param sourceVersionId      源对象版本 ID
 * @param targetBucketName     目标存储桶名称
 * @param targetKey            目标对象 Key
 * @param metadata             目标对象元数据
 * @param tags                 目标对象标签
 * @param replaceMetadata      是否替换元数据
 * @param replaceTags          是否替换标签
 * @param acl                  目标对象 ACL
 * @param storageClass         目标对象存储类型
 * @param serverSideEncryption 服务端加密方式
 * @param kmsKeyId             KMS Key ID
 * @author Ateng
 * @since 2026-04-28
 */
public record S3CopyRequest(
        String sourceBucketName,
        String sourceKey,
        String sourceVersionId,
        String targetBucketName,
        String targetKey,
        Map<String, String> metadata,
        Map<String, String> tags,
        Boolean replaceMetadata,
        Boolean replaceTags,
        S3ObjectAcl acl,
        S3StorageClass storageClass,
        S3ServerSideEncryption serverSideEncryption,
        String kmsKeyId
) {
}
