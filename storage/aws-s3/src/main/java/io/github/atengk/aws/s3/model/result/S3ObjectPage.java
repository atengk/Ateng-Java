package io.github.atengk.aws.s3.model.result;

import io.github.atengk.aws.s3.model.info.S3ObjectInfo;

import java.util.List;

/**
 * S3 对象分页结果
 *
 * @param bucketName             存储桶名称
 * @param prefix                 对象 Key 前缀
 * @param objects                对象列表
 * @param commonPrefixes         公共前缀列表
 * @param truncated              是否还有下一页
 * @param nextContinuationToken  下一页令牌
 * @param maxKeys                最大返回数量
 * @author Ateng
 * @since 2026-04-28
 */
public record S3ObjectPage(
        String bucketName,
        String prefix,
        List<S3ObjectInfo> objects,
        List<String> commonPrefixes,
        Boolean truncated,
        String nextContinuationToken,
        Integer maxKeys
) {
}
