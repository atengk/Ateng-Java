package io.github.atengk.aws.s3.model.request;

/**
 * S3 对象列表查询请求
 *
 * @param bucketName        存储桶名称
 * @param prefix            对象 Key 前缀
 * @param delimiter         分隔符
 * @param maxKeys           最大返回数量
 * @param continuationToken 分页令牌
 * @param recursive         是否递归查询
 * @author Ateng
 * @since 2026-04-28
 */
public record S3ListRequest(
        String bucketName,
        String prefix,
        String delimiter,
        Integer maxKeys,
        String continuationToken,
        Boolean recursive
) {
}
