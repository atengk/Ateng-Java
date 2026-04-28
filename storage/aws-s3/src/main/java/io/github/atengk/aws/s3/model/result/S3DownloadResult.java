package io.github.atengk.aws.s3.model.result;

import java.io.InputStream;
import java.util.Map;

/**
 * S3 对象下载结果
 *
 * @param bucketName    存储桶名称
 * @param objectKey     对象 Key
 * @param filename      文件名
 * @param contentType   内容类型
 * @param contentLength 内容长度
 * @param metadata      自定义元数据
 * @param inputStream   下载输入流
 * @author Ateng
 * @since 2026-04-28
 */
public record S3DownloadResult(
        String bucketName,
        String objectKey,
        String filename,
        String contentType,
        Long contentLength,
        Map<String, String> metadata,
        InputStream inputStream
) {
}
