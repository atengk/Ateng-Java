package io.github.atengk.aws.s3.controller;

import cn.hutool.core.lang.Assert;
import cn.hutool.core.lang.Dict;
import cn.hutool.core.util.BooleanUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.json.JSONObject;
import cn.hutool.json.JSONUtil;
import io.github.atengk.aws.s3.model.enums.S3ObjectAcl;
import io.github.atengk.aws.s3.model.enums.S3ServerSideEncryption;
import io.github.atengk.aws.s3.model.enums.S3StorageClass;
import io.github.atengk.aws.s3.model.request.S3CopyRequest;
import io.github.atengk.aws.s3.model.request.S3MoveRequest;
import io.github.atengk.aws.s3.model.result.S3CopyResult;
import io.github.atengk.aws.s3.model.result.S3MoveResult;
import io.github.atengk.aws.s3.service.S3Service;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * S3 对象复制与移动接口
 *
 * @author Ateng
 * @since 2026-04-28
 */
@Slf4j
@Validated
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/s3/objects/transfer")
public class S3ObjectTransferController {

    private final S3Service s3Service;

    /**
     * 在默认存储桶内复制对象
     * <p>
     * curl 使用示例：
     * curl -X POST "http://localhost:14002/api/s3/objects/transfer/copy" \
     * -d "sourceKey=upload/a.txt" \
     * -d "targetKey=backup/a.txt"
     *
     * @param sourceKey 源对象 Key
     * @param targetKey 目标对象 Key
     * @return 复制结果
     */
    @PostMapping(value = "/copy", consumes = MediaType.APPLICATION_FORM_URLENCODED_VALUE)
    public S3CopyResult copy(@RequestParam String sourceKey,
                             @RequestParam String targetKey) {
        S3CopyResult result = s3Service.copy(sourceKey, targetKey);

        log.info("复制 S3 对象成功，sourceBucketName={}，sourceKey={}，targetBucketName={}，targetKey={}",
                result.sourceBucketName(), result.sourceKey(), result.targetBucketName(), result.targetKey());

        return result;
    }

    /**
     * 跨存储桶复制对象
     * <p>
     * curl 使用示例：
     * curl -X POST "http://localhost:14002/api/s3/objects/transfer/copy/cross-bucket" \
     * -d "sourceBucketName=data" \
     * -d "sourceKey=upload/a.txt" \
     * -d "targetBucketName=archive" \
     * -d "targetKey=backup/a.txt"
     *
     * @param sourceBucketName 源存储桶名称
     * @param sourceKey        源对象 Key
     * @param targetBucketName 目标存储桶名称
     * @param targetKey        目标对象 Key
     * @return 复制结果
     */
    @PostMapping(value = "/copy/cross-bucket", consumes = MediaType.APPLICATION_FORM_URLENCODED_VALUE)
    public S3CopyResult copyCrossBucket(@RequestParam String sourceBucketName,
                                        @RequestParam String sourceKey,
                                        @RequestParam String targetBucketName,
                                        @RequestParam String targetKey) {
        S3CopyResult result = s3Service.copy(sourceBucketName, sourceKey, targetBucketName, targetKey);

        log.info("跨桶复制 S3 对象成功，sourceBucketName={}，sourceKey={}，targetBucketName={}，targetKey={}",
                result.sourceBucketName(), result.sourceKey(), result.targetBucketName(), result.targetKey());

        return result;
    }

    /**
     * 高级复制对象
     * <p>
     * 支持 sourceVersionId、metadata、tags、replaceMetadata、replaceTags、ACL、存储类型和服务端加密。
     * metadataJson 和 tagsJson 为 JSON 对象字符串。
     * sourceBucketName 或 targetBucketName 不传时使用默认存储桶。
     * <p>
     * curl 使用示例：
     * curl -X POST "http://localhost:14002/api/s3/objects/transfer/copy/advanced" \
     * -d "sourceBucketName=data" \
     * -d "sourceKey=upload/report.pdf" \
     * -d "sourceVersionId=your-source-version-id" \
     * -d "targetBucketName=archive" \
     * -d "targetKey=backup/report.pdf" \
     * -d "replaceMetadata=true" \
     * -d "metadataJson={\"biz-type\":\"report\",\"owner\":\"ateng\"}" \
     * -d "replaceTags=true" \
     * -d "tagsJson={\"env\":\"prod\",\"type\":\"pdf\"}" \
     * -d "acl=PRIVATE" \
     * -d "storageClass=STANDARD" \
     * -d "serverSideEncryption=AWS_KMS" \
     * -d "kmsKeyId=your-kms-key-id"
     * <p>
     * curl 使用示例：
     * curl -X POST "http://localhost:14002/api/s3/objects/transfer/copy/advanced" \
     * -d "sourceKey=upload/a.txt" \
     * -d "targetKey=backup/a.txt" \
     * -d "replaceTags=false"
     *
     * @param sourceBucketName     源存储桶名称，可为空
     * @param sourceKey            源对象 Key
     * @param sourceVersionId      源对象版本 ID，可为空
     * @param targetBucketName     目标存储桶名称，可为空
     * @param targetKey            目标对象 Key
     * @param metadataJson         元数据 JSON，可为空
     * @param tagsJson             标签 JSON，可为空
     * @param replaceMetadata      是否替换元数据
     * @param replaceTags          是否替换标签
     * @param acl                  对象 ACL，可为空
     * @param storageClass         存储类型，可为空
     * @param serverSideEncryption 服务端加密方式，可为空
     * @param kmsKeyId             KMS Key ID，可为空
     * @return 复制结果
     */
    @PostMapping(value = "/copy/advanced", consumes = MediaType.APPLICATION_FORM_URLENCODED_VALUE)
    public S3CopyResult copyAdvanced(@RequestParam(required = false) String sourceBucketName,
                                     @RequestParam String sourceKey,
                                     @RequestParam(required = false) String sourceVersionId,
                                     @RequestParam(required = false) String targetBucketName,
                                     @RequestParam String targetKey,
                                     @RequestParam(required = false) String metadataJson,
                                     @RequestParam(required = false) String tagsJson,
                                     @RequestParam(required = false) Boolean replaceMetadata,
                                     @RequestParam(required = false) Boolean replaceTags,
                                     @RequestParam(required = false) String acl,
                                     @RequestParam(required = false) String storageClass,
                                     @RequestParam(required = false) String serverSideEncryption,
                                     @RequestParam(required = false) String kmsKeyId) {
        String resolvedSourceBucketName = s3Service.resolveBucketName(sourceBucketName);
        String resolvedTargetBucketName = s3Service.resolveBucketName(targetBucketName);

        S3CopyRequest request = new S3CopyRequest(
                resolvedSourceBucketName,
                sourceKey,
                blankToNull(sourceVersionId),
                resolvedTargetBucketName,
                targetKey,
                parseStringMap(metadataJson),
                parseStringMap(tagsJson),
                BooleanUtil.isTrue(replaceMetadata),
                BooleanUtil.isTrue(replaceTags),
                parseEnum(S3ObjectAcl.class, acl, "S3 对象 ACL 不合法"),
                parseEnum(S3StorageClass.class, storageClass, "S3 存储类型不合法"),
                parseEnum(S3ServerSideEncryption.class, serverSideEncryption, "S3 服务端加密方式不合法"),
                blankToNull(kmsKeyId)
        );

        S3CopyResult result = s3Service.copy(request);

        log.info("高级复制 S3 对象成功，sourceBucketName={}，sourceKey={}，targetBucketName={}，targetKey={}，replaceMetadata={}，replaceTags={}",
                result.sourceBucketName(),
                result.sourceKey(),
                result.targetBucketName(),
                result.targetKey(),
                BooleanUtil.isTrue(replaceMetadata),
                BooleanUtil.isTrue(replaceTags));

        return result;
    }

    /**
     * 在默认存储桶内移动对象
     * <p>
     * curl 使用示例：
     * curl -X POST "http://localhost:14002/api/s3/objects/transfer/move" \
     * -d "sourceKey=upload/a.txt" \
     * -d "targetKey=archive/a.txt"
     *
     * @param sourceKey 源对象 Key
     * @param targetKey 目标对象 Key
     * @return 移动结果
     */
    @PostMapping(value = "/move", consumes = MediaType.APPLICATION_FORM_URLENCODED_VALUE)
    public S3MoveResult move(@RequestParam String sourceKey,
                             @RequestParam String targetKey) {
        S3MoveResult result = s3Service.move(sourceKey, targetKey);

        log.info("移动 S3 对象成功，sourceBucketName={}，sourceKey={}，targetBucketName={}，targetKey={}，deletedSource={}",
                result.sourceBucketName(),
                result.sourceKey(),
                result.targetBucketName(),
                result.targetKey(),
                result.deletedSource());

        return result;
    }

    /**
     * 跨存储桶移动对象
     * <p>
     * curl 使用示例：
     * curl -X POST "http://localhost:14002/api/s3/objects/transfer/move/cross-bucket" \
     * -d "sourceBucketName=data" \
     * -d "sourceKey=upload/a.txt" \
     * -d "targetBucketName=archive" \
     * -d "targetKey=backup/a.txt"
     *
     * @param sourceBucketName 源存储桶名称
     * @param sourceKey        源对象 Key
     * @param targetBucketName 目标存储桶名称
     * @param targetKey        目标对象 Key
     * @return 移动结果
     */
    @PostMapping(value = "/move/cross-bucket", consumes = MediaType.APPLICATION_FORM_URLENCODED_VALUE)
    public S3MoveResult moveCrossBucket(@RequestParam String sourceBucketName,
                                        @RequestParam String sourceKey,
                                        @RequestParam String targetBucketName,
                                        @RequestParam String targetKey) {
        S3MoveResult result = s3Service.move(sourceBucketName, sourceKey, targetBucketName, targetKey);

        log.info("跨桶移动 S3 对象成功，sourceBucketName={}，sourceKey={}，targetBucketName={}，targetKey={}，deletedSource={}",
                result.sourceBucketName(),
                result.sourceKey(),
                result.targetBucketName(),
                result.targetKey(),
                result.deletedSource());

        return result;
    }

    /**
     * 高级移动对象
     * <p>
     * 支持控制目标对象存在时是否覆盖。
     * sourceBucketName 或 targetBucketName 不传时使用默认存储桶。
     * <p>
     * curl 使用示例：
     * curl -X POST "http://localhost:14002/api/s3/objects/transfer/move/advanced" \
     * -d "sourceBucketName=data" \
     * -d "sourceKey=upload/a.txt" \
     * -d "targetBucketName=archive" \
     * -d "targetKey=backup/a.txt" \
     * -d "overwrite=false"
     * <p>
     * curl 使用示例：
     * curl -X POST "http://localhost:14002/api/s3/objects/transfer/move/advanced" \
     * -d "sourceKey=temp/a.txt" \
     * -d "targetKey=done/a.txt" \
     * -d "overwrite=true"
     *
     * @param sourceBucketName 源存储桶名称，可为空
     * @param sourceKey        源对象 Key
     * @param targetBucketName 目标存储桶名称，可为空
     * @param targetKey        目标对象 Key
     * @param overwrite        是否覆盖目标对象，可为空，默认由 Service 处理
     * @return 移动结果
     */
    @PostMapping(value = "/move/advanced", consumes = MediaType.APPLICATION_FORM_URLENCODED_VALUE)
    public S3MoveResult moveAdvanced(@RequestParam(required = false) String sourceBucketName,
                                     @RequestParam String sourceKey,
                                     @RequestParam(required = false) String targetBucketName,
                                     @RequestParam String targetKey,
                                     @RequestParam(required = false) Boolean overwrite) {
        String resolvedSourceBucketName = s3Service.resolveBucketName(sourceBucketName);
        String resolvedTargetBucketName = s3Service.resolveBucketName(targetBucketName);

        S3MoveRequest request = new S3MoveRequest(
                resolvedSourceBucketName,
                sourceKey,
                resolvedTargetBucketName,
                targetKey,
                overwrite
        );

        S3MoveResult result = s3Service.move(request);

        log.info("高级移动 S3 对象成功，sourceBucketName={}，sourceKey={}，targetBucketName={}，targetKey={}，overwrite={}，deletedSource={}",
                result.sourceBucketName(),
                result.sourceKey(),
                result.targetBucketName(),
                result.targetKey(),
                overwrite,
                result.deletedSource());

        return result;
    }

    /**
     * 在默认存储桶内重命名对象
     * <p>
     * curl 使用示例：
     * curl -X POST "http://localhost:14002/api/s3/objects/transfer/rename" \
     * -d "sourceKey=upload/old-name.txt" \
     * -d "targetKey=upload/new-name.txt"
     *
     * @param sourceKey 源对象 Key
     * @param targetKey 目标对象 Key
     * @return 重命名结果
     */
    @PostMapping(value = "/rename", consumes = MediaType.APPLICATION_FORM_URLENCODED_VALUE)
    public Dict rename(@RequestParam String sourceKey,
                       @RequestParam String targetKey) {
        s3Service.rename(sourceKey, targetKey);

        log.info("重命名默认存储桶 S3 对象成功，sourceKey={}，targetKey={}", sourceKey, targetKey);

        return Dict.create()
                .set("bucketName", s3Service.getDefaultBucketName())
                .set("sourceKey", sourceKey)
                .set("targetKey", targetKey)
                .set("renamed", true);
    }

    /**
     * 在指定存储桶内重命名对象
     * <p>
     * curl 使用示例：
     * curl -X POST "http://localhost:14002/api/s3/objects/transfer/rename/bucket" \
     * -d "bucketName=data" \
     * -d "sourceKey=upload/old-name.txt" \
     * -d "targetKey=upload/new-name.txt"
     *
     * @param bucketName 存储桶名称
     * @param sourceKey  源对象 Key
     * @param targetKey  目标对象 Key
     * @return 重命名结果
     */
    @PostMapping(value = "/rename/bucket", consumes = MediaType.APPLICATION_FORM_URLENCODED_VALUE)
    public Dict renameInBucket(@RequestParam String bucketName,
                               @RequestParam String sourceKey,
                               @RequestParam String targetKey) {
        String resolvedBucketName = s3Service.resolveBucketName(bucketName);
        s3Service.rename(resolvedBucketName, sourceKey, targetKey);

        log.info("重命名指定存储桶 S3 对象成功，bucketName={}，sourceKey={}，targetKey={}",
                resolvedBucketName, sourceKey, targetKey);

        return Dict.create()
                .set("bucketName", resolvedBucketName)
                .set("sourceKey", sourceKey)
                .set("targetKey", targetKey)
                .set("renamed", true);
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

            map.put(StrUtil.trim(key), String.valueOf(value));
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