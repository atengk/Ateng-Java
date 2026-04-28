package io.github.atengk.aws.s3.controller;

import cn.hutool.core.lang.Assert;
import cn.hutool.core.lang.Dict;
import cn.hutool.core.util.BooleanUtil;
import cn.hutool.core.util.StrUtil;
import io.github.atengk.aws.s3.model.info.S3ObjectInfo;
import io.github.atengk.aws.s3.service.S3Service;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * S3 目录语义操作接口
 *
 * @author Ateng
 * @since 2026-04-28
 */
@Slf4j
@Validated
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/s3/directories")
public class S3DirectoryController {

    private final S3Service s3Service;

    /**
     * 创建目录占位对象
     * <p>
     * bucketName 不传时使用默认存储桶。
     * S3 没有真实目录，该接口会创建一个以 / 结尾的零字节对象作为目录占位。
     * <p>
     * curl 使用示例：
     * curl -X POST "http://localhost:14002/api/s3/directories" \
     * -d "directoryKey=upload/2026/04/28/"
     * <p>
     * curl 使用示例：
     * curl -X POST "http://localhost:14002/api/s3/directories" \
     * -d "bucketName=data" \
     * -d "directoryKey=upload/2026/04/28/"
     *
     * @param bucketName   存储桶名称，可为空
     * @param directoryKey 目录 Key
     * @return 创建结果
     */
    @PostMapping(consumes = MediaType.APPLICATION_FORM_URLENCODED_VALUE)
    public Dict createDirectory(@RequestParam(required = false) String bucketName,
                                @RequestParam String directoryKey) {
        Assert.notBlank(directoryKey, "S3 目录 Key 不能为空");

        String resolvedBucketName = s3Service.resolveBucketName(bucketName);
        s3Service.createDirectory(resolvedBucketName, directoryKey);

        log.info("创建 S3 目录成功，bucketName={}，directoryKey={}", resolvedBucketName, directoryKey);

        return Dict.create()
                .set("bucketName", resolvedBucketName)
                .set("directoryKey", directoryKey)
                .set("created", true);
    }

    /**
     * 判断目录是否存在
     * <p>
     * bucketName 不传时使用默认存储桶。
     * 如果目录占位对象不存在，但该前缀下存在对象，也会认为目录存在。
     * <p>
     * curl 使用示例：
     * curl -X GET "http://localhost:14002/api/s3/directories/exists?directoryKey=upload/2026/04/28/"
     * <p>
     * curl 使用示例：
     * curl -X GET "http://localhost:14002/api/s3/directories/exists?bucketName=data&directoryKey=upload/2026/04/28/"
     *
     * @param bucketName   存储桶名称，可为空
     * @param directoryKey 目录 Key
     * @return 是否存在
     */
    @GetMapping("/exists")
    public Dict directoryExists(@RequestParam(required = false) String bucketName,
                                @RequestParam String directoryKey) {
        Assert.notBlank(directoryKey, "S3 目录 Key 不能为空");

        String resolvedBucketName = s3Service.resolveBucketName(bucketName);
        boolean exists = s3Service.directoryExists(resolvedBucketName, directoryKey);

        log.info("检查 S3 目录是否存在完成，bucketName={}，directoryKey={}，exists={}",
                resolvedBucketName, directoryKey, exists);

        return Dict.create()
                .set("bucketName", resolvedBucketName)
                .set("directoryKey", directoryKey)
                .set("exists", exists);
    }

    /**
     * 删除目录及其下所有对象
     * <p>
     * bucketName 不传时使用默认存储桶。
     * 该接口会删除 directoryKey 前缀匹配到的所有对象，必须传 confirm=true。
     * <p>
     * curl 使用示例：
     * curl -X DELETE "http://localhost:14002/api/s3/directories?directoryKey=temp/&confirm=true"
     * <p>
     * curl 使用示例：
     * curl -X DELETE "http://localhost:14002/api/s3/directories?bucketName=data&directoryKey=temp/&confirm=true"
     *
     * @param bucketName   存储桶名称，可为空
     * @param directoryKey 目录 Key
     * @param confirm      是否确认删除
     * @return 删除结果
     */
    @DeleteMapping
    public Dict deleteDirectory(@RequestParam(required = false) String bucketName,
                                @RequestParam String directoryKey,
                                @RequestParam(required = false) Boolean confirm) {
        assertDeleteDirectoryConfirmed(directoryKey, confirm);

        String resolvedBucketName = s3Service.resolveBucketName(bucketName);
        Long deletedCount = s3Service.deleteDirectory(resolvedBucketName, directoryKey);

        log.info("删除 S3 目录成功，bucketName={}，directoryKey={}，deletedCount={}",
                resolvedBucketName, directoryKey, deletedCount);

        return Dict.create()
                .set("bucketName", resolvedBucketName)
                .set("directoryKey", directoryKey)
                .set("deletedCount", deletedCount)
                .set("deleted", true);
    }

    /**
     * 查询目录当前层级对象列表
     * <p>
     * bucketName 不传时使用默认存储桶。
     * 该接口默认只查询当前目录层级，不递归展开子目录。
     * <p>
     * curl 使用示例：
     * curl -X GET "http://localhost:14002/api/s3/directories/list?directoryKey=upload/2026/04/28/"
     * <p>
     * curl 使用示例：
     * curl -X GET "http://localhost:14002/api/s3/directories/list?bucketName=data&directoryKey=upload/2026/04/28/"
     *
     * @param bucketName   存储桶名称，可为空
     * @param directoryKey 目录 Key
     * @return 当前层级对象列表
     */
    @GetMapping("/list")
    public List<S3ObjectInfo> listDirectory(@RequestParam(required = false) String bucketName,
                                            @RequestParam String directoryKey) {
        Assert.notBlank(directoryKey, "S3 目录 Key 不能为空");

        String resolvedBucketName = s3Service.resolveBucketName(bucketName);
        List<S3ObjectInfo> objects = s3Service.listDirectory(resolvedBucketName, directoryKey);

        log.info("查询 S3 目录当前层级对象列表成功，bucketName={}，directoryKey={}，count={}",
                resolvedBucketName, directoryKey, objects.size());

        return objects;
    }

    /**
     * 校验目录删除确认参数
     *
     * @param directoryKey 目录 Key
     * @param confirm      是否确认删除
     */
    private void assertDeleteDirectoryConfirmed(String directoryKey, Boolean confirm) {
        Assert.notBlank(directoryKey, "删除 S3 目录时 directoryKey 不能为空");
        Assert.isTrue(BooleanUtil.isTrue(confirm), "删除 S3 目录属于高风险操作，必须传 confirm=true");

        String normalizedDirectoryKey = StrUtil.trim(directoryKey);
        Assert.isTrue(!StrUtil.equals(normalizedDirectoryKey, "/"), "禁止删除根目录");
        Assert.isTrue(!StrUtil.equals(normalizedDirectoryKey, "*"), "禁止使用通配符删除目录");
        Assert.isTrue(StrUtil.isNotBlank(StrUtil.removeAll(normalizedDirectoryKey, "/")),
                "删除 S3 目录时 directoryKey 不能是空路径");
    }
}