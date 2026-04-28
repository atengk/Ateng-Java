package io.github.atengk.aws.s3.model.enums;

/**
 * S3 对象存储类型
 *
 * @author Ateng
 * @since 2026-04-28
 */
public enum S3StorageClass {

    /**
     * 标准存储
     */
    STANDARD,

    /**
     * 智能分层
     */
    INTELLIGENT_TIERING,

    /**
     * 标准低频访问
     */
    STANDARD_IA,

    /**
     * 单区低频访问
     */
    ONEZONE_IA,

    /**
     * Glacier 归档
     */
    GLACIER,

    /**
     * 深度归档
     */
    DEEP_ARCHIVE,

    /**
     * 低冗余存储
     */
    REDUCED_REDUNDANCY
}
