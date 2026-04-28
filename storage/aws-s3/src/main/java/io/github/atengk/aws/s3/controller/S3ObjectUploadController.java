package io.github.atengk.aws.s3.controller;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.io.IoUtil;
import cn.hutool.core.lang.Assert;
import cn.hutool.core.util.StrUtil;
import cn.hutool.json.JSONObject;
import cn.hutool.json.JSONUtil;
import io.github.atengk.aws.s3.model.enums.S3ObjectAcl;
import io.github.atengk.aws.s3.model.enums.S3ServerSideEncryption;
import io.github.atengk.aws.s3.model.enums.S3StorageClass;
import io.github.atengk.aws.s3.model.request.S3UploadRequest;
import io.github.atengk.aws.s3.model.result.S3UploadResult;
import io.github.atengk.aws.s3.service.S3Service;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * S3 对象上传接口
 *
 * @author Ateng
 * @since 2026-04-28
 */
@Slf4j
@Validated
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/s3/objects/upload")
public class S3ObjectUploadController {

    private final S3Service s3Service;

    /**
     * 上传 MultipartFile 文件
     * <p>
     * objectKey 不传时，仅支持上传到默认存储桶，并由 Service 自动生成对象 Key。
     * bucketName 不传时使用默认存储桶。
     * <p>
     * curl 使用示例：
     * curl -X POST "http://localhost:14002/api/s3/objects/upload/file" \
     * -F "file=@/data/test.txt"
     * <p>
     * curl 使用示例：
     * curl -X POST "http://localhost:14002/api/s3/objects/upload/file" \
     * -F "bucketName=data" \
     * -F "objectKey=upload/2026/04/28/test.txt" \
     * -F "file=@/data/test.txt"
     *
     * @param bucketName 存储桶名称，可为空
     * @param objectKey  对象 Key，可为空
     * @param file       上传文件
     * @return 上传结果
     */
    @PostMapping(value = "/file", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public S3UploadResult uploadFile(@RequestParam(required = false) String bucketName,
                                     @RequestParam(required = false) String objectKey,
                                     @RequestParam MultipartFile file) {
        Assert.notNull(file, "上传文件不能为空");

        if (StrUtil.isBlank(objectKey)) {
            Assert.isTrue(
                    StrUtil.isBlank(bucketName),
                    "objectKey 为空时不支持指定 bucketName，请传入 objectKey 或使用默认存储桶自动生成对象 Key"
            );

            S3UploadResult result = s3Service.upload(file);

            log.info("上传 S3 文件成功，bucketName={}，objectKey={}，originalFilename={}，size={}",
                    result.bucketName(), result.objectKey(), result.originalFilename(), result.size());
            return result;
        }

        String resolvedBucketName = s3Service.resolveBucketName(bucketName);
        S3UploadResult result = s3Service.upload(resolvedBucketName, objectKey, file);

        log.info("上传 S3 文件成功，bucketName={}，objectKey={}，originalFilename={}，size={}",
                result.bucketName(), result.objectKey(), result.originalFilename(), result.size());

        return result;
    }

    /**
     * 上传二进制请求体
     * <p>
     * bucketName 不传时使用默认存储桶。
     * <p>
     * curl 使用示例：
     * curl -X POST "http://localhost:14002/api/s3/objects/upload/bytes?objectKey=upload/2026/04/28/test.bin" \
     * -H "Content-Type: application/octet-stream" \
     * --data-binary "@/data/test.bin"
     * <p>
     * curl 使用示例：
     * curl -X POST "http://localhost:14002/api/s3/objects/upload/bytes?bucketName=data&objectKey=upload/2026/04/28/test.bin" \
     * -H "Content-Type: application/octet-stream" \
     * --data-binary "@/data/test.bin"
     *
     * @param bucketName 存储桶名称，可为空
     * @param objectKey  对象 Key
     * @param bytes      二进制内容
     * @return 上传结果
     */
    @PostMapping(value = "/bytes", consumes = MediaType.APPLICATION_OCTET_STREAM_VALUE)
    public S3UploadResult uploadBytes(@RequestParam(required = false) String bucketName,
                                      @RequestParam String objectKey,
                                      @RequestBody byte[] bytes) {
        Assert.notNull(bytes, "上传字节数组不能为空");

        String resolvedBucketName = s3Service.resolveBucketName(bucketName);
        S3UploadResult result = s3Service.upload(resolvedBucketName, objectKey, bytes);

        log.info("上传 S3 二进制对象成功，bucketName={}，objectKey={}，size={}",
                result.bucketName(), result.objectKey(), result.size());

        return result;
    }

    /**
     * 上传服务端本地文件
     * <p>
     * 说明：filePath 是服务端机器上的文件路径，不是客户端本机路径。
     * bucketName 不传时使用默认存储桶。
     * <p>
     * curl 使用示例：
     * curl -X POST "http://localhost:14002/api/s3/objects/upload/local-file" \
     * -d "filePath=/data/test.xlsx" \
     * -d "objectKey=excel/test.xlsx"
     * <p>
     * curl 使用示例：
     * curl -X POST "http://localhost:14002/api/s3/objects/upload/local-file" \
     * -d "bucketName=data" \
     * -d "filePath=/data/test.xlsx" \
     * -d "objectKey=excel/test.xlsx"
     *
     * @param bucketName 存储桶名称，可为空
     * @param objectKey  对象 Key
     * @param filePath   服务端本地文件路径
     * @return 上传结果
     */
    @PostMapping(value = "/local-file", consumes = MediaType.APPLICATION_FORM_URLENCODED_VALUE)
    public S3UploadResult uploadLocalFile(@RequestParam(required = false) String bucketName,
                                          @RequestParam String objectKey,
                                          @RequestParam String filePath) {
        String resolvedBucketName = s3Service.resolveBucketName(bucketName);
        S3UploadResult result = s3Service.upload(resolvedBucketName, objectKey, Path.of(filePath));

        log.info("上传 S3 本地文件成功，bucketName={}，objectKey={}，filePath={}，size={}",
                result.bucketName(), result.objectKey(), filePath, result.size());

        return result;
    }

    /**
     * 高级上传 MultipartFile 文件
     * <p>
     * 支持设置 contentType、metadata、tags、ACL、存储类型和服务端加密。
     * metadataJson 和 tagsJson 为 JSON 对象字符串。
     * bucketName 不传时使用默认存储桶。
     * <p>
     * curl 使用示例：
     * curl -X POST "http://localhost:14002/api/s3/objects/upload/advanced" \
     * -F "bucketName=data" \
     * -F "objectKey=secure/report.pdf" \
     * -F "contentType=application/pdf" \
     * -F "metadataJson={\"biz-type\":\"report\",\"owner\":\"ateng\"}" \
     * -F "tagsJson={\"env\":\"prod\",\"type\":\"pdf\"}" \
     * -F "acl=PRIVATE" \
     * -F "storageClass=STANDARD" \
     * -F "serverSideEncryption=AWS_KMS" \
     * -F "kmsKeyId=your-kms-key-id" \
     * -F "file=@/data/report.pdf"
     * <p>
     * curl 使用示例：
     * curl -X POST "http://localhost:14002/api/s3/objects/upload/advanced" \
     * -F "objectKey=image/avatar.png" \
     * -F "tagsJson={\"scene\":\"avatar\"}" \
     * -F "file=@/data/avatar.png"
     *
     * @param bucketName           存储桶名称，可为空
     * @param objectKey            对象 Key
     * @param file                 上传文件
     * @param contentType          Content-Type，可为空
     * @param metadataJson         元数据 JSON，可为空
     * @param tagsJson             标签 JSON，可为空
     * @param acl                  对象 ACL，可为空
     * @param storageClass         存储类型，可为空
     * @param serverSideEncryption 服务端加密方式，可为空
     * @param kmsKeyId             KMS Key ID，可为空
     * @return 上传结果
     */
    @PostMapping(value = "/advanced", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public S3UploadResult uploadAdvanced(@RequestParam(required = false) String bucketName,
                                         @RequestParam String objectKey,
                                         @RequestParam MultipartFile file,
                                         @RequestParam(required = false) String contentType,
                                         @RequestParam(required = false) String metadataJson,
                                         @RequestParam(required = false) String tagsJson,
                                         @RequestParam(required = false) String acl,
                                         @RequestParam(required = false) String storageClass,
                                         @RequestParam(required = false) String serverSideEncryption,
                                         @RequestParam(required = false) String kmsKeyId) {
        Assert.notNull(file, "上传文件不能为空");

        String resolvedBucketName = s3Service.resolveBucketName(bucketName);
        String resolvedContentType = StrUtil.blankToDefault(contentType, file.getContentType());

        try (InputStream inputStream = file.getInputStream()) {
            S3UploadRequest request = new S3UploadRequest(
                    resolvedBucketName,
                    objectKey,
                    inputStream,
                    file.getSize(),
                    resolvedContentType,
                    parseStringMap(metadataJson),
                    parseStringMap(tagsJson),
                    parseEnum(S3ObjectAcl.class, acl, "S3 对象 ACL 不合法"),
                    parseEnum(S3StorageClass.class, storageClass, "S3 存储类型不合法"),
                    parseEnum(S3ServerSideEncryption.class, serverSideEncryption, "S3 服务端加密方式不合法"),
                    blankToNull(kmsKeyId)
            );

            S3UploadResult result = s3Service.upload(request);

            log.info("高级上传 S3 文件成功，bucketName={}，objectKey={}，originalFilename={}，size={}",
                    result.bucketName(), result.objectKey(), result.originalFilename(), result.size());

            return result;
        } catch (IOException e) {
            log.error("读取高级上传文件流失败，bucketName={}，objectKey={}，originalFilename={}",
                    resolvedBucketName, objectKey, file.getOriginalFilename(), e);
            throw new UncheckedIOException("读取高级上传文件流失败", e);
        }
    }

    /**
     * 批量上传 MultipartFile 文件
     * <p>
     * files 和 objectKeys 按顺序一一对应。
     * metadataJson、tagsJson、acl、storageClass、serverSideEncryption、kmsKeyId 会应用到本次批量上传的所有文件。
     * bucketName 不传时使用默认存储桶。
     * <p>
     * curl 使用示例：
     * curl -X POST "http://localhost:14002/api/s3/objects/upload/batch" \
     * -F "bucketName=data" \
     * -F "objectKeys=batch/a.txt" \
     * -F "objectKeys=batch/b.txt" \
     * -F "files=@/data/a.txt" \
     * -F "files=@/data/b.txt"
     * <p>
     * curl 使用示例：
     * curl -X POST "http://localhost:14002/api/s3/objects/upload/batch" \
     * -F "objectKeys=batch/a.txt" \
     * -F "objectKeys=batch/b.txt" \
     * -F "tagsJson={\"batch\":\"true\"}" \
     * -F "acl=PRIVATE" \
     * -F "files=@/data/a.txt" \
     * -F "files=@/data/b.txt"
     *
     * @param bucketName           存储桶名称，可为空
     * @param files                上传文件列表
     * @param objectKeys           对象 Key 列表
     * @param metadataJson         元数据 JSON，可为空
     * @param tagsJson             标签 JSON，可为空
     * @param acl                  对象 ACL，可为空
     * @param storageClass         存储类型，可为空
     * @param serverSideEncryption 服务端加密方式，可为空
     * @param kmsKeyId             KMS Key ID，可为空
     * @return 上传结果列表
     */
    @PostMapping(value = "/batch", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public List<S3UploadResult> uploadBatch(@RequestParam(required = false) String bucketName,
                                            @RequestParam List<MultipartFile> files,
                                            @RequestParam List<String> objectKeys,
                                            @RequestParam(required = false) String metadataJson,
                                            @RequestParam(required = false) String tagsJson,
                                            @RequestParam(required = false) String acl,
                                            @RequestParam(required = false) String storageClass,
                                            @RequestParam(required = false) String serverSideEncryption,
                                            @RequestParam(required = false) String kmsKeyId) {
        Assert.notEmpty(files, "批量上传文件不能为空");
        Assert.notEmpty(objectKeys, "批量上传对象 Key 不能为空");
        Assert.isTrue(files.size() == objectKeys.size(), "批量上传 files 和 objectKeys 数量必须一致");

        String resolvedBucketName = s3Service.resolveBucketName(bucketName);
        Map<String, String> metadata = parseStringMap(metadataJson);
        Map<String, String> tags = parseStringMap(tagsJson);
        S3ObjectAcl resolvedAcl = parseEnum(S3ObjectAcl.class, acl, "S3 对象 ACL 不合法");
        S3StorageClass resolvedStorageClass = parseEnum(S3StorageClass.class, storageClass, "S3 存储类型不合法");
        S3ServerSideEncryption resolvedServerSideEncryption = parseEnum(
                S3ServerSideEncryption.class,
                serverSideEncryption,
                "S3 服务端加密方式不合法"
        );

        List<InputStream> inputStreams = new ArrayList<>(files.size());
        List<S3UploadRequest> requests = new ArrayList<>(files.size());

        try {
            for (int i = 0; i < files.size(); i++) {
                MultipartFile file = files.get(i);
                String objectKey = objectKeys.get(i);

                Assert.notNull(file, "批量上传文件不能为空，index={}", i);
                Assert.notBlank(objectKey, "批量上传对象 Key 不能为空，index={}", i);

                InputStream inputStream = file.getInputStream();
                inputStreams.add(inputStream);

                requests.add(new S3UploadRequest(
                        resolvedBucketName,
                        objectKey,
                        inputStream,
                        file.getSize(),
                        file.getContentType(),
                        metadata,
                        tags,
                        resolvedAcl,
                        resolvedStorageClass,
                        resolvedServerSideEncryption,
                        blankToNull(kmsKeyId)
                ));
            }

            List<S3UploadResult> results = s3Service.uploadBatch(requests);

            log.info("批量上传 S3 文件成功，bucketName={}，count={}", resolvedBucketName, results.size());
            return results;
        } catch (IOException e) {
            log.error("读取批量上传文件流失败，bucketName={}", resolvedBucketName, e);
            throw new UncheckedIOException("读取批量上传文件流失败", e);
        } finally {
            for (InputStream inputStream : inputStreams) {
                IoUtil.close(inputStream);
            }
        }
    }

    /**
     * 解析 JSON 字符串为 Map
     *
     * @param json JSON 字符串
     * @return 字符串 Map
     */
    private Map<String, String> parseStringMap(String json) {
        if (StrUtil.isBlank(json)) {
            return Map.of();
        }

        Assert.isTrue(JSONUtil.isTypeJSONObject(json), "参数必须是 JSON 对象字符串：{}", json);

        JSONObject jsonObject = JSONUtil.parseObj(json);
        if (jsonObject.isEmpty()) {
            return Map.of();
        }

        Map<String, String> map = new LinkedHashMap<>();

        jsonObject.forEach((key, value) -> {
            if (StrUtil.isBlank(key) || value == null) {
                return;
            }

            map.put(StrUtil.trim(key), StrUtil.toString(value));
        });

        return map;
    }

    /**
     * 解析枚举
     *
     * @param enumClass    枚举类型
     * @param value        枚举值
     * @param errorMessage 错误信息
     * @param <E>          枚举泛型
     * @return 枚举值
     */
    private <E extends Enum<E>> E parseEnum(Class<E> enumClass, String value, String errorMessage) {
        if (StrUtil.isBlank(value)) {
            return null;
        }

        try {
            return Enum.valueOf(enumClass, StrUtil.trim(value).toUpperCase());
        } catch (IllegalArgumentException e) {
            String enumValues = CollUtil.join(List.of(enumClass.getEnumConstants()), ",");
            throw new IllegalArgumentException(errorMessage + "，value=" + value + "，可选值：" + enumValues, e);
        }
    }

    /**
     * 空白字符串转 null，并去除前后空格
     *
     * @param value 字符串
     * @return 非空白字符串或 null
     */
    private String blankToNull(String value) {
        return StrUtil.isBlank(value) ? null : StrUtil.trim(value);
    }
}