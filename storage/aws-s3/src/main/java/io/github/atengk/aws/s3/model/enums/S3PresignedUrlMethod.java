package io.github.atengk.aws.s3.model.enums;

/**
 * S3 预签名 URL 请求方法
 *
 * @author Ateng
 * @since 2026-04-28
 */
public enum S3PresignedUrlMethod {

    /**
     * 下载对象
     */
    GET,

    /**
     * 上传对象
     */
    PUT,

    /**
     * 查询对象元信息
     */
    HEAD,

    /**
     * 删除对象
     */
    DELETE
}
