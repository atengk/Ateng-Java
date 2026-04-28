package io.github.atengk.aws.s3.model.result;

import java.util.List;

/**
 * S3 对象删除结果
 *
 * @param bucketName        存储桶名称
 * @param deletedObjectKeys 删除成功的对象 Key
 * @param errors            删除失败信息
 * @param deletedCount      删除成功数量
 * @param errorCount        删除失败数量
 * @author Ateng
 * @since 2026-04-28
 */
public record S3DeleteResult(
        String bucketName,
        List<String> deletedObjectKeys,
        List<S3DeleteError> errors,
        Long deletedCount,
        Long errorCount
) {
}
