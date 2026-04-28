package io.github.atengk.aws.s3.model.enums;

/**
 * S3 对象 ACL
 *
 * @author Ateng
 * @since 2026-04-28
 */
public enum S3ObjectAcl {

    /**
     * 私有访问
     */
    PRIVATE,

    /**
     * 公共读
     */
    PUBLIC_READ,

    /**
     * 公共读写
     */
    PUBLIC_READ_WRITE,

    /**
     * 已认证用户可读
     */
    AUTHENTICATED_READ,

    /**
     * 存储桶拥有者可读
     */
    BUCKET_OWNER_READ,

    /**
     * 存储桶拥有者完全控制
     */
    BUCKET_OWNER_FULL_CONTROL
}
