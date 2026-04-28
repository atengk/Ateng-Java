package io.github.atengk.aws.s3.model.enums;

/**
 * S3 服务端加密方式
 *
 * @author Ateng
 * @since 2026-04-28
 */
public enum S3ServerSideEncryption {

    /**
     * SSE-S3 AES256 加密
     */
    AES256,

    /**
     * SSE-KMS 加密
     */
    AWS_KMS
}
