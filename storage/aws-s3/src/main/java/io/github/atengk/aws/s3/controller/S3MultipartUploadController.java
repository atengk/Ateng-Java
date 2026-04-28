package io.github.atengk.aws.s3.controller;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.lang.Assert;
import cn.hutool.core.lang.Dict;
import cn.hutool.core.util.StrUtil;
import cn.hutool.json.JSONArray;
import cn.hutool.json.JSONObject;
import cn.hutool.json.JSONUtil;
import io.github.atengk.aws.s3.model.enums.S3ObjectAcl;
import io.github.atengk.aws.s3.model.enums.S3ServerSideEncryption;
import io.github.atengk.aws.s3.model.enums.S3StorageClass;
import io.github.atengk.aws.s3.model.info.S3MultipartUploadInfo;
import io.github.atengk.aws.s3.model.info.S3MultipartUploadPartInfo;
import io.github.atengk.aws.s3.model.request.S3MultipartUploadCompleteRequest;
import io.github.atengk.aws.s3.model.request.S3MultipartUploadInitRequest;
import io.github.atengk.aws.s3.model.request.S3MultipartUploadPartRequest;
import io.github.atengk.aws.s3.model.result.S3MultipartUploadInitResult;
import io.github.atengk.aws.s3.model.result.S3MultipartUploadPartResult;
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
import java.util.*;
import java.util.stream.Collectors;

/**
 * S3 分片上传接口
 *
 * @author Ateng
 * @since 2026-04-28
 */
@Slf4j
@Validated
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/s3/multipart-uploads")
public class S3MultipartUploadController {

    private final S3Service s3Service;

    /**
     * 初始化分片上传
     * <p>
     * bucketName 不传时使用默认存储桶。
     * metadataJson 和 tagsJson 为 JSON 对象字符串。
     * <p>
     * curl 使用示例：
     * curl -X POST "http://localhost:14002/api/s3/multipart-uploads/init" \
     * -d "bucketName=data" \
     * -d "objectKey=large/video.mp4" \
     * -d "contentType=video/mp4" \
     * -d "metadataJson={\"biz-type\":\"video\",\"owner\":\"ateng\"}" \
     * -d "tagsJson={\"scene\":\"multipart\",\"env\":\"dev\"}" \
     * -d "acl=PRIVATE" \
     * -d "storageClass=STANDARD"
     * <p>
     * curl 使用示例：
     * curl -X POST "http://localhost:14002/api/s3/multipart-uploads/init" \
     * -d "objectKey=secure/archive.zip" \
     * -d "contentType=application/zip" \
     * -d "serverSideEncryption=AWS_KMS" \
     * -d "kmsKeyId=your-kms-key-id"
     *
     * @param bucketName           存储桶名称，可为空
     * @param objectKey            对象 Key
     * @param contentType          Content-Type，可为空
     * @param metadataJson         元数据 JSON，可为空
     * @param tagsJson             标签 JSON，可为空
     * @param acl                  对象 ACL，可为空
     * @param storageClass         存储类型，可为空
     * @param serverSideEncryption 服务端加密方式，可为空
     * @param kmsKeyId             KMS Key ID，可为空
     * @return 分片上传初始化结果
     */
    @PostMapping(value = "/init", consumes = MediaType.APPLICATION_FORM_URLENCODED_VALUE)
    public S3MultipartUploadInitResult initMultipartUpload(@RequestParam(required = false) String bucketName,
                                                           @RequestParam String objectKey,
                                                           @RequestParam(required = false) String contentType,
                                                           @RequestParam(required = false) String metadataJson,
                                                           @RequestParam(required = false) String tagsJson,
                                                           @RequestParam(required = false) String acl,
                                                           @RequestParam(required = false) String storageClass,
                                                           @RequestParam(required = false) String serverSideEncryption,
                                                           @RequestParam(required = false) String kmsKeyId) {
        String resolvedBucketName = s3Service.resolveBucketName(bucketName);

        S3MultipartUploadInitRequest request = new S3MultipartUploadInitRequest(
                resolvedBucketName,
                objectKey,
                blankToNull(contentType),
                parseStringMap(metadataJson),
                parseStringMap(tagsJson),
                parseEnum(S3ObjectAcl.class, acl, "S3 对象 ACL 不合法"),
                parseEnum(S3StorageClass.class, storageClass, "S3 存储类型不合法"),
                parseEnum(S3ServerSideEncryption.class, serverSideEncryption, "S3 服务端加密方式不合法"),
                blankToNull(kmsKeyId)
        );

        S3MultipartUploadInitResult result = s3Service.initMultipartUpload(request);

        log.info("初始化 S3 分片上传成功，bucketName={}，objectKey={}，uploadId={}",
                result.bucketName(), result.objectKey(), result.uploadId());

        return result;
    }

    /**
     * 上传单个分片
     * <p>
     * bucketName 不传时使用默认存储桶。
     * partNumber 从 1 开始，最大 10000。
     * <p>
     * curl 使用示例：
     * curl -X POST "http://localhost:14002/api/s3/multipart-uploads/part" \
     * -F "bucketName=data" \
     * -F "objectKey=large/video.mp4" \
     * -F "uploadId=your-upload-id" \
     * -F "partNumber=1" \
     * -F "file=@/data/parts/video.part001"
     * <p>
     * curl 使用示例：
     * curl -X POST "http://localhost:14002/api/s3/multipart-uploads/part" \
     * -F "objectKey=large/video.mp4" \
     * -F "uploadId=your-upload-id" \
     * -F "partNumber=2" \
     * -F "file=@/data/parts/video.part002"
     *
     * @param bucketName 存储桶名称，可为空
     * @param objectKey  对象 Key
     * @param uploadId   上传 ID
     * @param partNumber 分片编号
     * @param file       分片文件
     * @return 分片上传结果
     */
    @PostMapping(value = "/part", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public S3MultipartUploadPartResult uploadPart(@RequestParam(required = false) String bucketName,
                                                  @RequestParam String objectKey,
                                                  @RequestParam String uploadId,
                                                  @RequestParam Integer partNumber,
                                                  @RequestParam MultipartFile file) {
        Assert.notNull(file, "S3 分片文件不能为空");

        String resolvedBucketName = s3Service.resolveBucketName(bucketName);

        try (InputStream inputStream = file.getInputStream()) {
            S3MultipartUploadPartRequest request = new S3MultipartUploadPartRequest(
                    resolvedBucketName,
                    objectKey,
                    uploadId,
                    partNumber,
                    inputStream,
                    file.getSize()
            );

            S3MultipartUploadPartResult result = s3Service.uploadPart(request);

            log.info("上传 S3 分片成功，bucketName={}，objectKey={}，uploadId={}，partNumber={}，size={}，eTag={}",
                    resolvedBucketName, objectKey, uploadId, result.partNumber(), result.size(), result.eTag());

            return result;
        } catch (IOException e) {
            log.error("读取 S3 分片文件流失败，bucketName={}，objectKey={}，uploadId={}，partNumber={}，filename={}",
                    resolvedBucketName, objectKey, uploadId, partNumber, file.getOriginalFilename(), e);
            throw new UncheckedIOException("读取 S3 分片文件流失败", e);
        }
    }

    /**
     * 完成分片上传
     * <p>
     * bucketName 不传时使用默认存储桶。
     * partsJson 为 JSON 数组字符串，数组元素必须包含 partNumber 和 eTag，size 可选。
     * <p>
     * curl 使用示例：
     * curl -X POST "http://localhost:14002/api/s3/multipart-uploads/complete" \
     * -d "bucketName=data" \
     * -d "objectKey=large/video.mp4" \
     * -d "uploadId=your-upload-id" \
     * -d "partsJson=[{\"partNumber\":1,\"eTag\":\"etag-1\",\"size\":5242880},{\"partNumber\":2,\"eTag\":\"etag-2\",\"size\":5242880}]"
     * <p>
     * curl 使用示例：
     * curl -X POST "http://localhost:14002/api/s3/multipart-uploads/complete" \
     * -d "objectKey=large/video.mp4" \
     * -d "uploadId=your-upload-id" \
     * -d "partsJson=[{\"partNumber\":1,\"eTag\":\"etag-1\"},{\"partNumber\":2,\"eTag\":\"etag-2\"}]"
     *
     * @param bucketName 存储桶名称，可为空
     * @param objectKey  对象 Key
     * @param uploadId   上传 ID
     * @param partsJson  分片结果 JSON 数组
     * @return 上传结果
     */
    @PostMapping(value = "/complete", consumes = MediaType.APPLICATION_FORM_URLENCODED_VALUE)
    public S3UploadResult completeMultipartUpload(@RequestParam(required = false) String bucketName,
                                                  @RequestParam String objectKey,
                                                  @RequestParam String uploadId,
                                                  @RequestParam String partsJson) {
        String resolvedBucketName = s3Service.resolveBucketName(bucketName);
        List<S3MultipartUploadPartResult> parts = parseMultipartUploadParts(partsJson);

        S3MultipartUploadCompleteRequest request = new S3MultipartUploadCompleteRequest(
                resolvedBucketName,
                objectKey,
                uploadId,
                parts
        );

        S3UploadResult result = s3Service.completeMultipartUpload(request);

        log.info("完成 S3 分片上传成功，bucketName={}，objectKey={}，uploadId={}，partCount={}，size={}，eTag={}",
                result.bucketName(), result.objectKey(), uploadId, parts.size(), result.size(), result.eTag());

        return result;
    }

    /**
     * 终止分片上传
     * <p>
     * bucketName 不传时使用默认存储桶。
     * 终止后，已上传但未完成合并的分片会被释放。
     * <p>
     * curl 使用示例：
     * curl -X DELETE "http://localhost:14002/api/s3/multipart-uploads?bucketName=data&objectKey=large/video.mp4&uploadId=your-upload-id"
     * <p>
     * curl 使用示例：
     * curl -X DELETE "http://localhost:14002/api/s3/multipart-uploads?objectKey=large/video.mp4&uploadId=your-upload-id"
     *
     * @param bucketName 存储桶名称，可为空
     * @param objectKey  对象 Key
     * @param uploadId   上传 ID
     * @return 终止结果
     */
    @DeleteMapping
    public Dict abortMultipartUpload(@RequestParam(required = false) String bucketName,
                                     @RequestParam String objectKey,
                                     @RequestParam String uploadId) {
        String resolvedBucketName = s3Service.resolveBucketName(bucketName);

        s3Service.abortMultipartUpload(resolvedBucketName, objectKey, uploadId);

        log.info("终止 S3 分片上传成功，bucketName={}，objectKey={}，uploadId={}",
                resolvedBucketName, objectKey, uploadId);

        return Dict.create()
                .set("bucketName", resolvedBucketName)
                .set("objectKey", objectKey)
                .set("uploadId", uploadId)
                .set("aborted", true);
    }

    /**
     * 查询未完成的分片上传任务
     * <p>
     * bucketName 不传时使用默认存储桶。
     * prefix 不传时查询整个存储桶下未完成的分片上传任务。
     * <p>
     * curl 使用示例：
     * curl -X GET "http://localhost:14002/api/s3/multipart-uploads?bucketName=data&prefix=large/"
     * <p>
     * curl 使用示例：
     * curl -X GET "http://localhost:14002/api/s3/multipart-uploads?prefix=large/"
     *
     * @param bucketName 存储桶名称，可为空
     * @param prefix     对象 Key 前缀，可为空
     * @return 未完成分片上传任务列表
     */
    @GetMapping
    public List<S3MultipartUploadInfo> listMultipartUploads(@RequestParam(required = false) String bucketName,
                                                            @RequestParam(required = false) String prefix) {
        String resolvedBucketName = s3Service.resolveBucketName(bucketName);
        List<S3MultipartUploadInfo> uploads = s3Service.listMultipartUploads(resolvedBucketName, prefix);

        log.info("查询 S3 未完成分片上传任务成功，bucketName={}，prefix={}，count={}",
                resolvedBucketName, StrUtil.blankToDefault(prefix, "全部"), uploads.size());

        return uploads;
    }

    /**
     * 查询指定分片上传任务已上传的分片列表
     * <p>
     * bucketName 不传时使用默认存储桶。
     * <p>
     * curl 使用示例：
     * curl -X GET "http://localhost:14002/api/s3/multipart-uploads/parts?bucketName=data&objectKey=large/video.mp4&uploadId=your-upload-id"
     * <p>
     * curl 使用示例：
     * curl -X GET "http://localhost:14002/api/s3/multipart-uploads/parts?objectKey=large/video.mp4&uploadId=your-upload-id"
     *
     * @param bucketName 存储桶名称，可为空
     * @param objectKey  对象 Key
     * @param uploadId   上传 ID
     * @return 已上传分片列表
     */
    @GetMapping("/parts")
    public List<S3MultipartUploadPartInfo> listMultipartUploadParts(@RequestParam(required = false) String bucketName,
                                                                    @RequestParam String objectKey,
                                                                    @RequestParam String uploadId) {
        String resolvedBucketName = s3Service.resolveBucketName(bucketName);
        List<S3MultipartUploadPartInfo> parts = s3Service.listMultipartUploadParts(
                resolvedBucketName,
                objectKey,
                uploadId
        );

        log.info("查询 S3 已上传分片列表成功，bucketName={}，objectKey={}，uploadId={}，count={}",
                resolvedBucketName, objectKey, uploadId, parts.size());

        return parts;
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
     * 解析完成分片上传的分片列表
     *
     * @param partsJson 分片 JSON 数组字符串
     * @return 分片上传结果列表
     */
    private List<S3MultipartUploadPartResult> parseMultipartUploadParts(String partsJson) {
        Assert.notBlank(partsJson, "S3 完成分片上传 partsJson 不能为空");
        Assert.isTrue(JSONUtil.isTypeJSONArray(partsJson), "partsJson 必须是 JSON 数组字符串");

        JSONArray jsonArray = JSONUtil.parseArray(partsJson);
        Assert.isTrue(CollUtil.isNotEmpty(jsonArray), "S3 完成分片上传分片列表不能为空");

        List<S3MultipartUploadPartResult> parts = new ArrayList<>(jsonArray.size());

        for (int i = 0; i < jsonArray.size(); i++) {
            Object item = jsonArray.get(i);
            JSONObject jsonObject = JSONUtil.parseObj(item);

            Integer partNumber = jsonObject.getInt("partNumber");
            String eTag = jsonObject.getStr("eTag");
            Long size = jsonObject.getLong("size");

            Assert.notNull(partNumber, "S3 分片 partNumber 不能为空，index={}", i);
            Assert.notBlank(eTag, "S3 分片 eTag 不能为空，index={}", i);

            parts.add(new S3MultipartUploadPartResult(
                    partNumber,
                    eTag,
                    size
            ));
        }

        return parts;
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
            String enumValues = Arrays.stream(enumClass.getEnumConstants())
                    .map(Enum::name)
                    .collect(Collectors.joining(","));

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