package io.github.atengk.aws.s3.controller;

import cn.hutool.core.util.BooleanUtil;
import cn.hutool.core.util.StrUtil;
import io.github.atengk.aws.s3.model.info.S3ObjectInfo;
import io.github.atengk.aws.s3.model.info.S3ObjectVersionInfo;
import io.github.atengk.aws.s3.model.request.S3ListRequest;
import io.github.atengk.aws.s3.model.result.S3ObjectPage;
import io.github.atengk.aws.s3.service.S3Service;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * S3 对象列表与分页查询接口
 *
 * @author Ateng
 * @since 2026-04-28
 */
@Slf4j
@Validated
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/s3/objects/list")
public class S3ObjectListController {

    private final S3Service s3Service;

    /**
     * 查询对象当前层级列表
     * <p>
     * bucketName 不传时使用默认存储桶。
     * prefix 不传时查询根层级。
     * 当前层级列表默认不递归展开子目录。
     * <p>
     * curl 使用示例：
     * curl -X GET "http://localhost:14002/api/s3/objects/list"
     * <p>
     * curl 使用示例：
     * curl -X GET "http://localhost:14002/api/s3/objects/list?bucketName=data&prefix=upload/"
     *
     * @param bucketName 存储桶名称，可为空
     * @param prefix     对象 Key 前缀，可为空
     * @return 对象信息列表
     */
    @GetMapping
    public List<S3ObjectInfo> listObjects(@RequestParam(required = false) String bucketName,
                                          @RequestParam(required = false) String prefix) {
        String resolvedBucketName = s3Service.resolveBucketName(bucketName);
        List<S3ObjectInfo> objects = s3Service.listObjects(resolvedBucketName, prefix);

        log.info("查询 S3 当前层级对象列表成功，bucketName={}，prefix={}，count={}",
                resolvedBucketName,
                StrUtil.blankToDefault(prefix, "根层级"),
                objects.size());

        return objects;
    }

    /**
     * 分页查询对象列表
     * <p>
     * bucketName 不传时使用默认存储桶。
     * delimiter 常用值为 /，用于按目录层级聚合 commonPrefixes。
     * recursive=true 时会忽略 delimiter，递归查询对象。
     * <p>
     * curl 使用示例：
     * curl -X GET "http://localhost:14002/api/s3/objects/list/page?bucketName=data&prefix=upload/&delimiter=/&maxKeys=100"
     * <p>
     * curl 使用示例：
     * curl -X GET "http://localhost:14002/api/s3/objects/list/page?bucketName=data&prefix=upload/&maxKeys=100&continuationToken=your-next-token&recursive=true"
     *
     * @param bucketName        存储桶名称，可为空
     * @param prefix            对象 Key 前缀，可为空
     * @param delimiter         分隔符，可为空
     * @param maxKeys           最大返回数量，可为空
     * @param continuationToken 分页令牌，可为空
     * @param recursive         是否递归查询，可为空
     * @return 对象分页结果
     */
    @GetMapping("/page")
    public S3ObjectPage listObjectsPage(@RequestParam(required = false) String bucketName,
                                        @RequestParam(required = false) String prefix,
                                        @RequestParam(required = false) String delimiter,
                                        @RequestParam(required = false) Integer maxKeys,
                                        @RequestParam(required = false) String continuationToken,
                                        @RequestParam(required = false) Boolean recursive) {
        String resolvedBucketName = s3Service.resolveBucketName(bucketName);

        S3ListRequest request = new S3ListRequest(
                resolvedBucketName,
                blankToNull(prefix),
                blankToNull(delimiter),
                maxKeys,
                blankToNull(continuationToken),
                BooleanUtil.isTrue(recursive)
        );

        S3ObjectPage page = s3Service.listObjectsPage(request);

        log.info("分页查询 S3 对象列表成功，bucketName={}，prefix={}，maxKeys={}，recursive={}，objectCount={}，commonPrefixCount={}，truncated={}",
                page.bucketName(),
                StrUtil.blankToDefault(page.prefix(), "根层级"),
                page.maxKeys(),
                BooleanUtil.isTrue(recursive),
                page.objects() == null ? 0 : page.objects().size(),
                page.commonPrefixes() == null ? 0 : page.commonPrefixes().size(),
                page.truncated());

        return page;
    }

    /**
     * 递归查询对象列表
     * <p>
     * bucketName 不传时使用默认存储桶。
     * prefix 不传时递归查询整个存储桶。
     * <p>
     * curl 使用示例：
     * curl -X GET "http://localhost:14002/api/s3/objects/list/recursive"
     * <p>
     * curl 使用示例：
     * curl -X GET "http://localhost:14002/api/s3/objects/list/recursive?bucketName=data&prefix=upload/"
     *
     * @param bucketName 存储桶名称，可为空
     * @param prefix     对象 Key 前缀，可为空
     * @return 对象信息列表
     */
    @GetMapping("/recursive")
    public List<S3ObjectInfo> listObjectsRecursive(@RequestParam(required = false) String bucketName,
                                                   @RequestParam(required = false) String prefix) {
        String resolvedBucketName = s3Service.resolveBucketName(bucketName);
        List<S3ObjectInfo> objects = s3Service.listObjectsRecursive(resolvedBucketName, prefix);

        log.info("递归查询 S3 对象列表成功，bucketName={}，prefix={}，count={}",
                resolvedBucketName,
                StrUtil.blankToDefault(prefix, "全部对象"),
                objects.size());

        return objects;
    }

    /**
     * 递归查询对象 Key 列表
     * <p>
     * bucketName 不传时使用默认存储桶。
     * prefix 不传时递归查询整个存储桶下的对象 Key。
     * <p>
     * curl 使用示例：
     * curl -X GET "http://localhost:14002/api/s3/objects/list/keys"
     * <p>
     * curl 使用示例：
     * curl -X GET "http://localhost:14002/api/s3/objects/list/keys?bucketName=data&prefix=upload/"
     *
     * @param bucketName 存储桶名称，可为空
     * @param prefix     对象 Key 前缀，可为空
     * @return 对象 Key 列表
     */
    @GetMapping("/keys")
    public List<String> listObjectKeys(@RequestParam(required = false) String bucketName,
                                       @RequestParam(required = false) String prefix) {
        String resolvedBucketName = s3Service.resolveBucketName(bucketName);
        List<String> objectKeys = s3Service.listObjectKeys(resolvedBucketName, prefix);

        log.info("递归查询 S3 对象 Key 列表成功，bucketName={}，prefix={}，count={}",
                resolvedBucketName,
                StrUtil.blankToDefault(prefix, "全部对象"),
                objectKeys.size());

        return objectKeys;
    }

    /**
     * 查询对象版本列表
     * <p>
     * bucketName 不传时使用默认存储桶。
     * prefix 不传时查询整个存储桶的对象版本。
     * 返回结果包含普通对象版本和删除标记。
     * <p>
     * curl 使用示例：
     * curl -X GET "http://localhost:14002/api/s3/objects/list/versions?bucketName=data&prefix=archive/"
     * <p>
     * curl 使用示例：
     * curl -X GET "http://localhost:14002/api/s3/objects/list/versions?prefix=archive/report.pdf"
     *
     * @param bucketName 存储桶名称，可为空
     * @param prefix     对象 Key 前缀，可为空
     * @return 对象版本信息列表
     */
    @GetMapping("/versions")
    public List<S3ObjectVersionInfo> listObjectVersions(@RequestParam(required = false) String bucketName,
                                                        @RequestParam(required = false) String prefix) {
        String resolvedBucketName = s3Service.resolveBucketName(bucketName);
        List<S3ObjectVersionInfo> versions = s3Service.listObjectVersions(resolvedBucketName, prefix);

        log.info("查询 S3 对象版本列表成功，bucketName={}，prefix={}，count={}",
                resolvedBucketName,
                StrUtil.blankToDefault(prefix, "全部版本"),
                versions.size());

        return versions;
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