package io.github.atengk.aws.s3.model.result;

/**
 * S3 对象删除错误
 *
 * @param objectKey 对象 Key
 * @param versionId 版本 ID
 * @param code      错误码
 * @param message   错误信息
 * @author Ateng
 * @since 2026-04-28
 */
public record S3DeleteError(
        String objectKey,
        String versionId,
        String code,
        String message
) {
}
