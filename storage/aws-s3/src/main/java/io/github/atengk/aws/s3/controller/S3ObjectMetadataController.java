package io.github.atengk.aws.s3.controller;

import cn.hutool.core.lang.Assert;
import cn.hutool.core.lang.Dict;
import cn.hutool.core.util.BooleanUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.json.JSONObject;
import cn.hutool.json.JSONUtil;
import io.github.atengk.aws.s3.model.enums.S3StorageClass;
import io.github.atengk.aws.s3.model.request.S3MetadataRequest;
import io.github.atengk.aws.s3.service.S3Service;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * S3 对象元数据管理接口
 *
 * @author Ateng
 * @since 2026-04-28
 */
@Slf4j
@Validated
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/s3/objects/metadata")
public class S3ObjectMetadataController {

    private final S3Service s3Service;

    /**
     * 获取对象自定义元数据
     * <p>
     * bucketName 不传时使用默认存储桶。
     * 返回的是对象自定义 metadata，不包含 Content-Type、Content-Length、ETag 等系统元数据。
     * <p>
     * curl 使用示例：
     * curl -X GET "http://localhost:14002/api/s3/objects/metadata?objectKey=upload/2026/04/28/test.txt"
     * <p>
     * curl 使用示例：
     * curl -X GET "http://localhost:14002/api/s3/objects/metadata?bucketName=data&objectKey=upload/2026/04/28/test.txt"
     *
     * @param bucketName 存储桶名称，可为空
     * @param objectKey  对象 Key
     * @return 自定义元数据
     */
    @GetMapping
    public Dict getMetadata(@RequestParam(required = false) String bucketName,
                            @RequestParam String objectKey) {
        String resolvedBucketName = s3Service.resolveBucketName(bucketName);
        Map<String, String> metadata = s3Service.getMetadata(resolvedBucketName, objectKey);

        log.info("获取 S3 对象元数据成功，bucketName={}，objectKey={}，metadataSize={}",
                resolvedBucketName, objectKey, metadata.size());

        return Dict.create()
                .set("bucketName", resolvedBucketName)
                .set("objectKey", objectKey)
                .set("metadata", metadata);
    }

    /**
     * 替换对象自定义元数据
     * <p>
     * bucketName 不传时使用默认存储桶。
     * metadataJson 为 JSON 对象字符串。
     * 该接口会完整替换原对象自定义 metadata，不是增量合并。
     * <p>
     * curl 使用示例：
     * curl -X PUT "http://localhost:14002/api/s3/objects/metadata" \
     * -d "objectKey=upload/2026/04/28/test.txt" \
     * -d "metadataJson={\"biz-type\":\"report\",\"owner\":\"ateng\"}"
     * <p>
     * curl 使用示例：
     * curl -X PUT "http://localhost:14002/api/s3/objects/metadata" \
     * -d "bucketName=data" \
     * -d "objectKey=upload/2026/04/28/test.txt" \
     * -d "metadataJson={\"source\":\"api\",\"trace-id\":\"abc123\"}"
     * <p>
     * curl 使用示例：
     * curl -X PUT "http://localhost:14002/api/s3/objects/metadata" \
     * -d "bucketName=data" \
     * -d "objectKey=upload/2026/04/28/test.txt" \
     * -d "metadataJson={}"
     *
     * @param bucketName   存储桶名称，可为空
     * @param objectKey    对象 Key
     * @param metadataJson 元数据 JSON 对象字符串，可为空或 {}
     * @return 替换结果
     */
    @PutMapping(consumes = MediaType.APPLICATION_FORM_URLENCODED_VALUE)
    public Dict replaceMetadata(@RequestParam(required = false) String bucketName,
                                @RequestParam String objectKey,
                                @RequestParam(required = false) String metadataJson) {
        String resolvedBucketName = s3Service.resolveBucketName(bucketName);
        Map<String, String> metadata = parseStringMap(metadataJson);

        s3Service.replaceMetadata(resolvedBucketName, objectKey, metadata);

        log.info("替换 S3 对象元数据成功，bucketName={}，objectKey={}，metadataSize={}",
                resolvedBucketName, objectKey, metadata.size());

        return Dict.create()
                .set("bucketName", resolvedBucketName)
                .set("objectKey", objectKey)
                .set("metadata", metadata)
                .set("replaced", true);
    }

    /**
     * 高级替换对象自定义元数据
     * <p>
     * bucketName 不传时使用默认存储桶。
     * metadataJson 为 JSON 对象字符串。
     * preserveTags=true 表示保留原对象标签。
     * preserveAcl=true 表示复制前读取原 ACL，复制后再写回原 ACL。
     * storageClass 不传时保留服务端默认处理，不主动修改存储类型。
     * <p>
     * curl 使用示例：
     * curl -X PUT "http://localhost:14002/api/s3/objects/metadata/advanced" \
     * -d "bucketName=data" \
     * -d "objectKey=upload/report.pdf" \
     * -d "metadataJson={\"biz-type\":\"report\",\"owner\":\"ateng\"}" \
     * -d "preserveTags=true" \
     * -d "preserveAcl=false" \
     * -d "storageClass=STANDARD"
     * <p>
     * curl 使用示例：
     * curl -X PUT "http://localhost:14002/api/s3/objects/metadata/advanced" \
     * -d "objectKey=upload/test.txt" \
     * -d "metadataJson={}" \
     * -d "preserveTags=false" \
     * -d "preserveAcl=false"
     *
     * @param bucketName   存储桶名称，可为空
     * @param objectKey    对象 Key
     * @param metadataJson 元数据 JSON 对象字符串，可为空或 {}
     * @param preserveTags 是否保留原标签，可为空
     * @param preserveAcl  是否保留原 ACL，可为空
     * @param storageClass 存储类型，可为空
     * @return 替换结果
     */
    @PutMapping(value = "/advanced", consumes = MediaType.APPLICATION_FORM_URLENCODED_VALUE)
    public Dict replaceMetadataAdvanced(@RequestParam(required = false) String bucketName,
                                        @RequestParam String objectKey,
                                        @RequestParam(required = false) String metadataJson,
                                        @RequestParam(required = false) Boolean preserveTags,
                                        @RequestParam(required = false) Boolean preserveAcl,
                                        @RequestParam(required = false) String storageClass) {
        String resolvedBucketName = s3Service.resolveBucketName(bucketName);
        Map<String, String> metadata = parseStringMap(metadataJson);
        S3StorageClass resolvedStorageClass = parseEnum(
                S3StorageClass.class,
                storageClass,
                "S3 存储类型不合法"
        );

        S3MetadataRequest request = new S3MetadataRequest(
                resolvedBucketName,
                objectKey,
                metadata,
                BooleanUtil.isTrue(preserveTags),
                BooleanUtil.isTrue(preserveAcl),
                resolvedStorageClass
        );

        s3Service.replaceMetadata(request);

        log.info("高级替换 S3 对象元数据成功，bucketName={}，objectKey={}，metadataSize={}，preserveTags={}，preserveAcl={}，storageClass={}",
                resolvedBucketName,
                objectKey,
                metadata.size(),
                BooleanUtil.isTrue(preserveTags),
                BooleanUtil.isTrue(preserveAcl),
                resolvedStorageClass);

        return Dict.create()
                .set("bucketName", resolvedBucketName)
                .set("objectKey", objectKey)
                .set("metadata", metadata)
                .set("preserveTags", BooleanUtil.isTrue(preserveTags))
                .set("preserveAcl", BooleanUtil.isTrue(preserveAcl))
                .set("storageClass", resolvedStorageClass)
                .set("replaced", true);
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
            String enumValues = Arrays.stream(enumClass.getEnumConstants())
                    .map(Enum::name)
                    .collect(Collectors.joining(","));

            throw new IllegalArgumentException(errorMessage + "，value=" + value + "，可选值：" + enumValues, e);
        }
    }
}