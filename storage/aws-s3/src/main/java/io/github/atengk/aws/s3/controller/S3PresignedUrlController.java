package io.github.atengk.aws.s3.controller;

import cn.hutool.core.lang.Assert;
import cn.hutool.core.lang.Dict;
import cn.hutool.core.util.StrUtil;
import cn.hutool.json.JSONObject;
import cn.hutool.json.JSONUtil;
import io.github.atengk.aws.s3.model.enums.S3PresignedUrlMethod;
import io.github.atengk.aws.s3.model.request.S3PresignedUrlRequest;
import io.github.atengk.aws.s3.model.result.S3PresignedUrlResult;
import io.github.atengk.aws.s3.service.S3Service;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.convert.DurationStyle;
import org.springframework.http.MediaType;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.net.URI;
import java.time.Duration;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * S3 预签名 URL 接口
 *
 * @author Ateng
 * @since 2026-04-28
 */
@Slf4j
@Validated
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/s3/presigned-urls")
public class S3PresignedUrlController {

    private final S3Service s3Service;

    /**
     * 生成对象下载预签名 URL
     * <p>
     * bucketName 不传时使用默认存储桶。
     * expire 不传时使用配置中的默认过期时间。
     * <p>
     * curl 使用示例：
     * curl -X GET "http://localhost:14002/api/s3/presigned-urls/download?objectKey=upload/2026/04/28/test.txt"
     * <p>
     * curl 使用示例：
     * curl -X GET "http://localhost:14002/api/s3/presigned-urls/download?bucketName=data&objectKey=upload/2026/04/28/test.txt&expire=10m"
     *
     * @param bucketName 存储桶名称，可为空
     * @param objectKey  对象 Key
     * @param expire     有效期，可为空，例如 10m、30s、2h、7d、PT10M
     * @return 预签名 URL 信息
     */
    @GetMapping("/download")
    public Dict generateDownloadUrl(@RequestParam(required = false) String bucketName,
                                    @RequestParam String objectKey,
                                    @RequestParam(required = false) String expire) {
        String resolvedBucketName = s3Service.resolveBucketName(bucketName);
        Duration resolvedExpire = parseDuration(expire);

        URI url = resolvedExpire == null
                ? s3Service.generateDownloadUrl(resolvedBucketName, objectKey)
                : s3Service.generateDownloadUrl(resolvedBucketName, objectKey, resolvedExpire);

        log.info("生成 S3 下载预签名 URL 成功，bucketName={}，objectKey={}，expire={}",
                resolvedBucketName, objectKey, StrUtil.blankToDefault(expire, "默认配置"));

        return Dict.create()
                .set("bucketName", resolvedBucketName)
                .set("objectKey", objectKey)
                .set("method", S3PresignedUrlMethod.GET)
                .set("url", url.toString());
    }

    /**
     * 生成对象上传预签名 URL
     * <p>
     * bucketName 不传时使用默认存储桶。
     * expire 不传时使用配置中的默认过期时间。
     * <p>
     * curl 使用示例：
     * curl -X GET "http://localhost:14002/api/s3/presigned-urls/upload?objectKey=upload/2026/04/28/test.txt&expire=10m"
     * <p>
     * curl 使用示例：
     * curl -X GET "http://localhost:14002/api/s3/presigned-urls/upload?bucketName=data&objectKey=upload/2026/04/28/test.txt&expire=10m"
     *
     * @param bucketName 存储桶名称，可为空
     * @param objectKey  对象 Key
     * @param expire     有效期，可为空，例如 10m、30s、2h、7d、PT10M
     * @return 预签名 URL 信息
     */
    @GetMapping("/upload")
    public Dict generateUploadUrl(@RequestParam(required = false) String bucketName,
                                  @RequestParam String objectKey,
                                  @RequestParam(required = false) String expire) {
        String resolvedBucketName = s3Service.resolveBucketName(bucketName);
        Duration resolvedExpire = parseDuration(expire);

        URI url = s3Service.generateUploadUrl(resolvedBucketName, objectKey, resolvedExpire);

        log.info("生成 S3 上传预签名 URL 成功，bucketName={}，objectKey={}，expire={}",
                resolvedBucketName, objectKey, StrUtil.blankToDefault(expire, "默认配置"));

        return Dict.create()
                .set("bucketName", resolvedBucketName)
                .set("objectKey", objectKey)
                .set("method", S3PresignedUrlMethod.PUT)
                .set("url", url.toString());
    }

    /**
     * 生成对象 HEAD 预签名 URL
     * <p>
     * bucketName 不传时使用默认存储桶。
     * expire 不传时使用配置中的默认过期时间。
     * <p>
     * curl 使用示例：
     * curl -X GET "http://localhost:14002/api/s3/presigned-urls/head?objectKey=upload/2026/04/28/test.txt&expire=10m"
     * <p>
     * curl 使用示例：
     * curl -X GET "http://localhost:14002/api/s3/presigned-urls/head?bucketName=data&objectKey=upload/2026/04/28/test.txt&expire=10m"
     *
     * @param bucketName 存储桶名称，可为空
     * @param objectKey  对象 Key
     * @param expire     有效期，可为空，例如 10m、30s、2h、7d、PT10M
     * @return 预签名 URL 信息
     */
    @GetMapping("/head")
    public Dict generateHeadUrl(@RequestParam(required = false) String bucketName,
                                @RequestParam String objectKey,
                                @RequestParam(required = false) String expire) {
        String resolvedBucketName = s3Service.resolveBucketName(bucketName);
        Duration resolvedExpire = parseDuration(expire);

        URI url = s3Service.generateHeadUrl(resolvedBucketName, objectKey, resolvedExpire);

        log.info("生成 S3 HEAD 预签名 URL 成功，bucketName={}，objectKey={}，expire={}",
                resolvedBucketName, objectKey, StrUtil.blankToDefault(expire, "默认配置"));

        return Dict.create()
                .set("bucketName", resolvedBucketName)
                .set("objectKey", objectKey)
                .set("method", S3PresignedUrlMethod.HEAD)
                .set("url", url.toString());
    }

    /**
     * 生成对象删除预签名 URL
     * <p>
     * bucketName 不传时使用默认存储桶。
     * expire 不传时使用配置中的默认过期时间。
     * <p>
     * curl 使用示例：
     * curl -X GET "http://localhost:14002/api/s3/presigned-urls/delete?objectKey=temp/a.txt&expire=10m"
     * <p>
     * curl 使用示例：
     * curl -X GET "http://localhost:14002/api/s3/presigned-urls/delete?bucketName=data&objectKey=temp/a.txt&expire=10m"
     *
     * @param bucketName 存储桶名称，可为空
     * @param objectKey  对象 Key
     * @param expire     有效期，可为空，例如 10m、30s、2h、7d、PT10M
     * @return 预签名 URL 信息
     */
    @GetMapping("/delete")
    public Dict generateDeleteUrl(@RequestParam(required = false) String bucketName,
                                  @RequestParam String objectKey,
                                  @RequestParam(required = false) String expire) {
        String resolvedBucketName = s3Service.resolveBucketName(bucketName);
        Duration resolvedExpire = parseDuration(expire);

        URI url = s3Service.generateDeleteUrl(resolvedBucketName, objectKey, resolvedExpire);

        log.info("生成 S3 删除预签名 URL 成功，bucketName={}，objectKey={}，expire={}",
                resolvedBucketName, objectKey, StrUtil.blankToDefault(expire, "默认配置"));

        return Dict.create()
                .set("bucketName", resolvedBucketName)
                .set("objectKey", objectKey)
                .set("method", S3PresignedUrlMethod.DELETE)
                .set("url", url.toString());
    }

    /**
     * 生成高级预签名 URL
     * <p>
     * 支持 GET、PUT、HEAD、DELETE。
     * requestHeadersJson 和 responseHeadersJson 为 JSON 对象字符串。
     * responseHeadersJson 主要用于 GET 下载 URL 的响应头覆盖。
     * bucketName 不传时使用默认存储桶。
     * <p>
     * curl 使用示例：
     * curl -X POST "http://localhost:14002/api/s3/presigned-urls/generate" \
     * -d "bucketName=data" \
     * -d "objectKey=upload/report.pdf" \
     * -d "method=GET" \
     * -d "expire=10m" \
     * -d "filename=report.pdf" \
     * -d "responseHeadersJson={\"response-content-type\":\"application/pdf\"}"
     * <p>
     * curl 使用示例：
     * curl -X POST "http://localhost:14002/api/s3/presigned-urls/generate" \
     * -d "objectKey=upload/test.txt" \
     * -d "method=PUT" \
     * -d "expire=10m" \
     * -d "contentType=text/plain" \
     * -d "requestHeadersJson={\"x-amz-meta-source\":\"presigned-url\"}"
     *
     * @param bucketName          存储桶名称，可为空
     * @param objectKey           对象 Key
     * @param method              请求方法，可为空，默认 GET
     * @param expire              有效期，可为空，例如 10m、30s、2h、7d、PT10M
     * @param contentType         Content-Type，可为空
     * @param filename            下载文件名，可为空
     * @param requestHeadersJson  参与签名的请求头 JSON，可为空
     * @param responseHeadersJson GET 响应头覆盖 JSON，可为空
     * @return 预签名 URL 结果
     */
    @PostMapping(value = "/generate", consumes = MediaType.APPLICATION_FORM_URLENCODED_VALUE)
    public S3PresignedUrlResult generatePresignedUrl(@RequestParam(required = false) String bucketName,
                                                     @RequestParam String objectKey,
                                                     @RequestParam(required = false) String method,
                                                     @RequestParam(required = false) String expire,
                                                     @RequestParam(required = false) String contentType,
                                                     @RequestParam(required = false) String filename,
                                                     @RequestParam(required = false) String requestHeadersJson,
                                                     @RequestParam(required = false) String responseHeadersJson) {
        String resolvedBucketName = s3Service.resolveBucketName(bucketName);
        S3PresignedUrlMethod resolvedMethod = parseEnum(
                S3PresignedUrlMethod.class,
                StrUtil.blankToDefault(method, S3PresignedUrlMethod.GET.name()),
                "S3 预签名 URL 方法不合法"
        );

        S3PresignedUrlRequest request = new S3PresignedUrlRequest(
                resolvedBucketName,
                objectKey,
                resolvedMethod,
                parseDuration(expire),
                blankToNull(contentType),
                blankToNull(filename),
                parseStringMap(requestHeadersJson),
                parseStringMap(responseHeadersJson)
        );

        S3PresignedUrlResult result = s3Service.generatePresignedUrl(request);

        log.info("生成 S3 高级预签名 URL 成功，bucketName={}，objectKey={}，method={}，expire={}",
                result.bucketName(), result.objectKey(), result.method(), StrUtil.blankToDefault(expire, "默认配置"));

        return result;
    }

    /**
     * 解析 Duration 字符串
     *
     * @param value Duration 字符串
     * @return Duration
     */
    private Duration parseDuration(String value) {
        if (StrUtil.isBlank(value)) {
            return null;
        }

        try {
            return DurationStyle.detectAndParse(StrUtil.trim(value));
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("S3 预签名 URL 有效期格式不合法，value=" + value + "，示例：10m、30s、2h、7d、PT10M", e);
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