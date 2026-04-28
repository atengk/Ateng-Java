package io.github.atengk.aws.s3.controller;

import cn.hutool.core.lang.Dict;
import cn.hutool.core.util.StrUtil;
import io.github.atengk.aws.s3.service.S3Service;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * S3 存储桶管理接口
 *
 * @author Ateng
 * @since 2026-04-28
 */
@Slf4j
@Validated
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/s3/buckets")
public class S3BucketController {

    private final S3Service s3Service;

    /**
     * 查询当前凭证可见的存储桶列表
     * <p>
     * curl 使用示例：
     * curl -X GET "http://localhost:14002/api/s3/buckets"
     *
     * @return 存储桶名称列表
     */
    @GetMapping
    public List<String> listBuckets() {
        List<String> buckets = s3Service.listBuckets();

        log.info("查询 S3 存储桶列表成功，count={}", buckets.size());
        return buckets;
    }

    /**
     * 获取默认存储桶名称
     * <p>
     * curl 使用示例：
     * curl -X GET "http://localhost:14002/api/s3/buckets/default"
     *
     * @return 默认存储桶信息
     */
    @GetMapping("/default")
    public Dict getDefaultBucketName() {
        String bucketName = s3Service.getDefaultBucketName();

        log.info("获取 S3 默认存储桶名称成功，bucketName={}", bucketName);
        return Dict.create()
                .set("bucketName", bucketName);
    }

    /**
     * 解析存储桶名称，为空时返回默认存储桶
     * <p>
     * curl 使用示例：
     * curl -X GET "http://localhost:14002/api/s3/buckets/resolve"
     * <p>
     * curl 使用示例：
     * curl -X GET "http://localhost:14002/api/s3/buckets/resolve?bucketName=data"
     *
     * @param bucketName 存储桶名称，可为空
     * @return 解析后的存储桶名称
     */
    @GetMapping("/resolve")
    public Dict resolveBucketName(@RequestParam(required = false) String bucketName) {
        String resolvedBucketName = s3Service.resolveBucketName(bucketName);

        log.info("解析 S3 存储桶名称成功，inputBucketName={}，resolvedBucketName={}",
                StrUtil.blankToDefault(bucketName, "默认存储桶"),
                resolvedBucketName);

        return Dict.create()
                .set("bucketName", resolvedBucketName);
    }

    /**
     * 判断存储桶是否存在
     * <p>
     * bucketName 不传时判断默认存储桶。
     * <p>
     * curl 使用示例：
     * curl -X GET "http://localhost:14002/api/s3/buckets/exists"
     * <p>
     * curl 使用示例：
     * curl -X GET "http://localhost:14002/api/s3/buckets/exists?bucketName=data"
     *
     * @param bucketName 存储桶名称，可为空
     * @return 是否存在
     */
    @GetMapping("/exists")
    public Dict bucketExists(@RequestParam(required = false) String bucketName) {
        String resolvedBucketName = s3Service.resolveBucketName(bucketName);
        boolean exists = s3Service.bucketExists(resolvedBucketName);

        log.info("检查 S3 存储桶是否存在完成，bucketName={}，exists={}", resolvedBucketName, exists);
        return Dict.create()
                .set("bucketName", resolvedBucketName)
                .set("exists", exists);
    }

    /**
     * 创建存储桶
     * <p>
     * curl 使用示例：
     * curl -X POST "http://localhost:14002/api/s3/buckets/data"
     *
     * @param bucketName 存储桶名称
     * @return 创建结果
     */
    @PostMapping("/{bucketName}")
    public Dict createBucket(@PathVariable String bucketName) {
        String resolvedBucketName = s3Service.resolveBucketName(bucketName);
        s3Service.createBucket(resolvedBucketName);

        log.info("创建 S3 存储桶接口调用成功，bucketName={}", resolvedBucketName);
        return Dict.create()
                .set("bucketName", resolvedBucketName)
                .set("created", true);
    }

    /**
     * 存储桶不存在时创建
     * <p>
     * curl 使用示例：
     * curl -X POST "http://localhost:14002/api/s3/buckets/data/ensure"
     *
     * @param bucketName 存储桶名称
     * @return 处理结果
     */
    @PostMapping("/{bucketName}/ensure")
    public Dict createBucketIfAbsent(@PathVariable String bucketName) {
        String resolvedBucketName = s3Service.resolveBucketName(bucketName);
        s3Service.createBucketIfAbsent(resolvedBucketName);

        log.info("确保 S3 存储桶存在接口调用成功，bucketName={}", resolvedBucketName);
        return Dict.create()
                .set("bucketName", resolvedBucketName)
                .set("ensured", true);
    }

    /**
     * 删除存储桶
     * <p>
     * 说明：S3 删除存储桶前通常要求存储桶为空。
     * <p>
     * curl 使用示例：
     * curl -X DELETE "http://localhost:14002/api/s3/buckets/data"
     *
     * @param bucketName 存储桶名称
     * @return 删除结果
     */
    @DeleteMapping("/{bucketName}")
    public Dict deleteBucket(@PathVariable String bucketName) {
        String resolvedBucketName = s3Service.resolveBucketName(bucketName);
        s3Service.deleteBucket(resolvedBucketName);

        log.info("删除 S3 存储桶接口调用成功，bucketName={}", resolvedBucketName);
        return Dict.create()
                .set("bucketName", resolvedBucketName)
                .set("deleted", true);
    }
}