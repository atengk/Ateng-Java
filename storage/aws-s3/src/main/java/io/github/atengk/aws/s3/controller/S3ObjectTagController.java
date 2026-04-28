package io.github.atengk.aws.s3.controller;

import cn.hutool.core.lang.Assert;
import cn.hutool.core.lang.Dict;
import cn.hutool.core.util.StrUtil;
import cn.hutool.json.JSONObject;
import cn.hutool.json.JSONUtil;
import io.github.atengk.aws.s3.service.S3Service;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * S3 对象标签管理接口
 *
 * @author Ateng
 * @since 2026-04-28
 */
@Slf4j
@Validated
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/s3/objects/tags")
public class S3ObjectTagController {

    private final S3Service s3Service;

    /**
     * 获取对象标签
     * <p>
     * bucketName 不传时使用默认存储桶。
     * <p>
     * curl 使用示例：
     * curl -X GET "http://localhost:14002/api/s3/objects/tags?objectKey=upload/2026/04/28/test.txt"
     * <p>
     * curl 使用示例：
     * curl -X GET "http://localhost:14002/api/s3/objects/tags?bucketName=data&objectKey=upload/2026/04/28/test.txt"
     *
     * @param bucketName 存储桶名称，可为空
     * @param objectKey  对象 Key
     * @return 对象标签
     */
    @GetMapping
    public Dict getTags(@RequestParam(required = false) String bucketName,
                        @RequestParam String objectKey) {
        String resolvedBucketName = s3Service.resolveBucketName(bucketName);
        Map<String, String> tags = s3Service.getTags(resolvedBucketName, objectKey);

        log.info("获取 S3 对象标签成功，bucketName={}，objectKey={}，tagSize={}",
                resolvedBucketName, objectKey, tags.size());

        return Dict.create()
                .set("bucketName", resolvedBucketName)
                .set("objectKey", objectKey)
                .set("tags", tags);
    }

    /**
     * 获取指定版本对象标签
     * <p>
     * bucketName 不传时使用默认存储桶。
     * <p>
     * curl 使用示例：
     * curl -X GET "http://localhost:14002/api/s3/objects/tags/version?objectKey=archive/report.pdf&versionId=your-version-id"
     * <p>
     * curl 使用示例：
     * curl -X GET "http://localhost:14002/api/s3/objects/tags/version?bucketName=data&objectKey=archive/report.pdf&versionId=your-version-id"
     *
     * @param bucketName 存储桶名称，可为空
     * @param objectKey  对象 Key
     * @param versionId  对象版本 ID
     * @return 对象标签
     */
    @GetMapping("/version")
    public Dict getTagsByVersion(@RequestParam(required = false) String bucketName,
                                 @RequestParam String objectKey,
                                 @RequestParam String versionId) {
        String resolvedBucketName = s3Service.resolveBucketName(bucketName);
        Map<String, String> tags = s3Service.getTags(resolvedBucketName, objectKey, versionId);

        log.info("获取 S3 指定版本对象标签成功，bucketName={}，objectKey={}，versionId={}，tagSize={}",
                resolvedBucketName, objectKey, versionId, tags.size());

        return Dict.create()
                .set("bucketName", resolvedBucketName)
                .set("objectKey", objectKey)
                .set("versionId", versionId)
                .set("tags", tags);
    }

    /**
     * 写入对象标签
     * <p>
     * bucketName 不传时使用默认存储桶。
     * tagsJson 为 JSON 对象字符串。
     * 该接口会完整覆盖原对象标签，不是增量合并。
     * tagsJson 为空或 {} 时，会清空对象标签。
     * <p>
     * curl 使用示例：
     * curl -X PUT "http://localhost:14002/api/s3/objects/tags" \
     * -d "objectKey=upload/2026/04/28/test.txt" \
     * -d "tagsJson={\"env\":\"dev\",\"type\":\"text\"}"
     * <p>
     * curl 使用示例：
     * curl -X PUT "http://localhost:14002/api/s3/objects/tags" \
     * -d "bucketName=data" \
     * -d "objectKey=upload/2026/04/28/test.txt" \
     * -d "tagsJson={\"env\":\"prod\",\"owner\":\"ateng\"}"
     * <p>
     * curl 使用示例：
     * curl -X PUT "http://localhost:14002/api/s3/objects/tags" \
     * -d "bucketName=data" \
     * -d "objectKey=upload/2026/04/28/test.txt" \
     * -d "tagsJson={}"
     *
     * @param bucketName 存储桶名称，可为空
     * @param objectKey  对象 Key
     * @param tagsJson   标签 JSON 对象字符串，可为空或 {}
     * @return 写入结果
     */
    @PutMapping(consumes = MediaType.APPLICATION_FORM_URLENCODED_VALUE)
    public Dict putTags(@RequestParam(required = false) String bucketName,
                        @RequestParam String objectKey,
                        @RequestParam(required = false) String tagsJson) {
        String resolvedBucketName = s3Service.resolveBucketName(bucketName);
        Map<String, String> tags = parseStringMap(tagsJson);

        s3Service.putTags(resolvedBucketName, objectKey, tags);

        log.info("写入 S3 对象标签成功，bucketName={}，objectKey={}，tagSize={}",
                resolvedBucketName, objectKey, tags.size());

        return Dict.create()
                .set("bucketName", resolvedBucketName)
                .set("objectKey", objectKey)
                .set("tags", tags)
                .set("updated", true);
    }

    /**
     * 写入指定版本对象标签
     * <p>
     * bucketName 不传时使用默认存储桶。
     * tagsJson 为 JSON 对象字符串。
     * 该接口会完整覆盖指定版本对象标签，不是增量合并。
     * tagsJson 为空或 {} 时，会清空指定版本对象标签。
     * <p>
     * curl 使用示例：
     * curl -X PUT "http://localhost:14002/api/s3/objects/tags/version" \
     * -d "objectKey=archive/report.pdf" \
     * -d "versionId=your-version-id" \
     * -d "tagsJson={\"status\":\"archived\",\"year\":\"2026\"}"
     * <p>
     * curl 使用示例：
     * curl -X PUT "http://localhost:14002/api/s3/objects/tags/version" \
     * -d "bucketName=data" \
     * -d "objectKey=archive/report.pdf" \
     * -d "versionId=your-version-id" \
     * -d "tagsJson={\"status\":\"verified\"}"
     *
     * @param bucketName 存储桶名称，可为空
     * @param objectKey  对象 Key
     * @param versionId  对象版本 ID
     * @param tagsJson   标签 JSON 对象字符串，可为空或 {}
     * @return 写入结果
     */
    @PutMapping(value = "/version", consumes = MediaType.APPLICATION_FORM_URLENCODED_VALUE)
    public Dict putTagsByVersion(@RequestParam(required = false) String bucketName,
                                 @RequestParam String objectKey,
                                 @RequestParam String versionId,
                                 @RequestParam(required = false) String tagsJson) {
        String resolvedBucketName = s3Service.resolveBucketName(bucketName);
        Map<String, String> tags = parseStringMap(tagsJson);

        s3Service.putTags(resolvedBucketName, objectKey, versionId, tags);

        log.info("写入 S3 指定版本对象标签成功，bucketName={}，objectKey={}，versionId={}，tagSize={}",
                resolvedBucketName, objectKey, versionId, tags.size());

        return Dict.create()
                .set("bucketName", resolvedBucketName)
                .set("objectKey", objectKey)
                .set("versionId", versionId)
                .set("tags", tags)
                .set("updated", true);
    }

    /**
     * 删除对象标签
     * <p>
     * bucketName 不传时使用默认存储桶。
     * <p>
     * curl 使用示例：
     * curl -X DELETE "http://localhost:14002/api/s3/objects/tags?objectKey=upload/2026/04/28/test.txt"
     * <p>
     * curl 使用示例：
     * curl -X DELETE "http://localhost:14002/api/s3/objects/tags?bucketName=data&objectKey=upload/2026/04/28/test.txt"
     *
     * @param bucketName 存储桶名称，可为空
     * @param objectKey  对象 Key
     * @return 删除结果
     */
    @DeleteMapping
    public Dict deleteTags(@RequestParam(required = false) String bucketName,
                           @RequestParam String objectKey) {
        String resolvedBucketName = s3Service.resolveBucketName(bucketName);
        s3Service.deleteTags(resolvedBucketName, objectKey);

        log.info("删除 S3 对象标签成功，bucketName={}，objectKey={}", resolvedBucketName, objectKey);

        return Dict.create()
                .set("bucketName", resolvedBucketName)
                .set("objectKey", objectKey)
                .set("deleted", true);
    }

    /**
     * 删除指定版本对象标签
     * <p>
     * bucketName 不传时使用默认存储桶。
     * <p>
     * curl 使用示例：
     * curl -X DELETE "http://localhost:14002/api/s3/objects/tags/version?objectKey=archive/report.pdf&versionId=your-version-id"
     * <p>
     * curl 使用示例：
     * curl -X DELETE "http://localhost:14002/api/s3/objects/tags/version?bucketName=data&objectKey=archive/report.pdf&versionId=your-version-id"
     *
     * @param bucketName 存储桶名称，可为空
     * @param objectKey  对象 Key
     * @param versionId  对象版本 ID
     * @return 删除结果
     */
    @DeleteMapping("/version")
    public Dict deleteTagsByVersion(@RequestParam(required = false) String bucketName,
                                    @RequestParam String objectKey,
                                    @RequestParam String versionId) {
        String resolvedBucketName = s3Service.resolveBucketName(bucketName);
        s3Service.deleteTags(resolvedBucketName, objectKey, versionId);

        log.info("删除 S3 指定版本对象标签成功，bucketName={}，objectKey={}，versionId={}",
                resolvedBucketName, objectKey, versionId);

        return Dict.create()
                .set("bucketName", resolvedBucketName)
                .set("objectKey", objectKey)
                .set("versionId", versionId)
                .set("deleted", true);
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
}