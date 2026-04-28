package io.github.atengk.aws.s3.service.impl;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.date.DateUtil;
import cn.hutool.core.io.file.FileNameUtil;
import cn.hutool.core.lang.Assert;
import cn.hutool.core.map.MapUtil;
import cn.hutool.core.util.BooleanUtil;
import cn.hutool.core.util.IdUtil;
import cn.hutool.core.util.StrUtil;
import io.github.atengk.aws.s3.config.S3Properties;
import io.github.atengk.aws.s3.model.enums.S3ObjectAcl;
import io.github.atengk.aws.s3.model.enums.S3PresignedUrlMethod;
import io.github.atengk.aws.s3.model.enums.S3ServerSideEncryption;
import io.github.atengk.aws.s3.model.enums.S3StorageClass;
import io.github.atengk.aws.s3.model.info.S3MultipartUploadInfo;
import io.github.atengk.aws.s3.model.info.S3MultipartUploadPartInfo;
import io.github.atengk.aws.s3.model.info.S3ObjectInfo;
import io.github.atengk.aws.s3.model.info.S3ObjectVersionInfo;
import io.github.atengk.aws.s3.model.request.*;
import io.github.atengk.aws.s3.model.result.*;
import io.github.atengk.aws.s3.service.S3Service;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.InputStreamResource;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import software.amazon.awssdk.awscore.AwsRequestOverrideConfiguration;
import software.amazon.awssdk.core.ResponseBytes;
import software.amazon.awssdk.core.ResponseInputStream;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.core.sync.ResponseTransformer;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.*;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;
import software.amazon.awssdk.services.s3.presigner.model.*;

import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.net.URI;
import java.net.URLConnection;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.*;

/**
 * S3 文件服务实现
 *
 * @author Ateng
 * @since 2026-04-28
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class S3ServiceImpl implements S3Service {

    private final S3Client s3Client;

    private final S3Presigner s3Presigner;

    private final S3Properties s3Properties;

    private static final String REGION_US_EAST_1 = "us-east-1";

    private static final String DEFAULT_CONTENT_TYPE = "application/octet-stream";

    private static final String DEFAULT_UPLOAD_DIRECTORY = "upload";

    private static final DateTimeFormatter DEFAULT_DATE_PATH_FORMATTER = DateTimeFormatter.ofPattern("yyyy/MM/dd");

    private static final Duration AWS_S3_MAX_PRESIGN_EXPIRE = Duration.ofDays(7);

    private static final Duration DEFAULT_MIN_PRESIGN_EXPIRE = Duration.ofSeconds(1);

    // ==================== 存储桶管理 ====================

    /**
     * 判断存储桶是否存在
     *
     * @param bucketName 存储桶名称
     * @return true 存在，false 不存在
     */
    @Override
    public boolean bucketExists(String bucketName) {
        String resolvedBucketName = resolveBucketName(bucketName);

        try {
            HeadBucketRequest request = HeadBucketRequest.builder()
                    .bucket(resolvedBucketName)
                    .build();

            s3Client.headBucket(request);
            return true;
        } catch (NoSuchBucketException e) {
            log.info("S3 存储桶不存在，bucketName={}", resolvedBucketName);
            return false;
        } catch (S3Exception e) {
            if (isNotFound(e)) {
                log.info("S3 存储桶不存在，bucketName={}", resolvedBucketName);
                return false;
            }

            if (isForbidden(e)) {
                log.warn("S3 存储桶存在但当前凭证无访问权限，bucketName={}，errorCode={}",
                        resolvedBucketName, getErrorCode(e));
                return true;
            }

            log.error("检查 S3 存储桶失败，bucketName={}，statusCode={}，errorCode={}，message={}",
                    resolvedBucketName, e.statusCode(), getErrorCode(e), e.getMessage(), e);
            throw e;
        }
    }

    /**
     * 创建存储桶
     *
     * @param bucketName 存储桶名称
     */
    @Override
    public void createBucket(String bucketName) {
        String resolvedBucketName = resolveBucketName(bucketName);

        try {
            CreateBucketRequest request = buildCreateBucketRequest(resolvedBucketName);
            s3Client.createBucket(request);

            log.info("创建 S3 存储桶成功，bucketName={}，region={}",
                    resolvedBucketName, s3Properties.getRegion());
        } catch (BucketAlreadyOwnedByYouException e) {
            log.info("S3 存储桶已存在且归当前凭证所有，bucketName={}", resolvedBucketName);
        } catch (BucketAlreadyExistsException e) {
            log.error("S3 存储桶已被其他账号占用，bucketName={}", resolvedBucketName, e);
            throw e;
        } catch (S3Exception e) {
            if (isBucketAlreadyOwnedByYou(e)) {
                log.info("S3 存储桶已存在且归当前凭证所有，bucketName={}，errorCode={}",
                        resolvedBucketName, getErrorCode(e));
                return;
            }

            if (isBucketAlreadyExists(e)) {
                log.error("S3 存储桶已存在或已被占用，bucketName={}，statusCode={}，errorCode={}",
                        resolvedBucketName, e.statusCode(), getErrorCode(e), e);
                throw e;
            }

            log.error("创建 S3 存储桶失败，bucketName={}，statusCode={}，errorCode={}，message={}",
                    resolvedBucketName, e.statusCode(), getErrorCode(e), e.getMessage(), e);
            throw e;
        }
    }

    /**
     * 存储桶不存在时创建
     *
     * @param bucketName 存储桶名称
     */
    @Override
    public void createBucketIfAbsent(String bucketName) {
        String resolvedBucketName = resolveBucketName(bucketName);

        if (bucketExists(resolvedBucketName)) {
            log.info("S3 存储桶已存在，跳过创建，bucketName={}", resolvedBucketName);
            return;
        }

        createBucket(resolvedBucketName);
    }

    /**
     * 删除存储桶
     *
     * @param bucketName 存储桶名称
     */
    @Override
    public void deleteBucket(String bucketName) {
        String resolvedBucketName = resolveBucketName(bucketName);

        try {
            DeleteBucketRequest request = DeleteBucketRequest.builder()
                    .bucket(resolvedBucketName)
                    .build();

            s3Client.deleteBucket(request);
            log.info("删除 S3 存储桶成功，bucketName={}", resolvedBucketName);
        } catch (NoSuchBucketException e) {
            log.warn("删除 S3 存储桶时发现存储桶不存在，bucketName={}", resolvedBucketName);
            throw e;
        } catch (S3Exception e) {
            log.error("删除 S3 存储桶失败，bucketName={}，statusCode={}，errorCode={}，message={}",
                    resolvedBucketName, e.statusCode(), getErrorCode(e), e.getMessage(), e);
            throw e;
        }
    }

    /**
     * 查询当前凭证可见的存储桶列表
     *
     * @return 存储桶名称列表
     */
    @Override
    public List<String> listBuckets() {
        try {
            ListBucketsResponse response = s3Client.listBuckets();

            return response.buckets()
                    .stream()
                    .map(Bucket::name)
                    .filter(StrUtil::isNotBlank)
                    .toList();
        } catch (S3Exception e) {
            log.error("查询 S3 存储桶列表失败，statusCode={}，errorCode={}，message={}",
                    e.statusCode(), getErrorCode(e), e.getMessage(), e);
            throw e;
        }
    }

    /**
     * 获取默认存储桶名称
     *
     * @return 默认存储桶名称
     */
    @Override
    public String getDefaultBucketName() {
        String bucketName = StrUtil.trim(s3Properties.getBucketName());
        Assert.notBlank(bucketName, "S3 默认存储桶名称不能为空");
        return bucketName;
    }

    /**
     * 解析存储桶名称，为空时使用默认存储桶
     *
     * @param bucketName 存储桶名称
     * @return 最终存储桶名称
     */
    @Override
    public String resolveBucketName(String bucketName) {
        String resolvedBucketName = StrUtil.blankToDefault(StrUtil.trim(bucketName), getDefaultBucketName());
        Assert.notBlank(resolvedBucketName, "S3 存储桶名称不能为空");
        return resolvedBucketName;
    }

    // ==================== 工具方法 ====================

    /**
     * 规范化对象 Key
     *
     * @param objectKey 对象 Key
     * @return 规范化后的对象 Key
     */
    @Override
    public String normalizeObjectKey(String objectKey) {
        String normalizedObjectKey = StrUtil.trim(objectKey);
        Assert.notBlank(normalizedObjectKey, "S3 对象 Key 不能为空");

        normalizedObjectKey = StrUtil.replace(normalizedObjectKey, "\\", "/");
        normalizedObjectKey = normalizedObjectKey.replaceAll("/{2,}", "/");

        while (StrUtil.startWith(normalizedObjectKey, "/")) {
            normalizedObjectKey = StrUtil.removePrefix(normalizedObjectKey, "/");
        }

        while (StrUtil.startWith(normalizedObjectKey, "./")) {
            normalizedObjectKey = StrUtil.removePrefix(normalizedObjectKey, "./");
        }

        Assert.notBlank(normalizedObjectKey, "S3 对象 Key 不能为空");
        return normalizedObjectKey;
    }

    /**
     * 构建对象 Key
     *
     * @param directory 目录
     * @param filename  文件名
     * @return 对象 Key
     */
    @Override
    public String buildObjectKey(String directory, String filename) {
        String normalizedFilename = normalizeObjectKey(filename);

        if (StrUtil.isBlank(directory)) {
            return normalizedFilename;
        }

        String normalizedDirectory = normalizeObjectKey(directory);
        normalizedDirectory = StrUtil.removeSuffix(normalizedDirectory, "/");

        return normalizedDirectory + "/" + normalizedFilename;
    }

    /**
     * 获取文件名
     *
     * @param objectKey 对象 Key
     * @return 文件名
     */
    @Override
    public String getFilename(String objectKey) {
        String normalizedObjectKey = normalizeObjectKey(objectKey);
        String keyWithoutTrailingSlash = StrUtil.removeSuffix(normalizedObjectKey, "/");

        if (StrUtil.isBlank(keyWithoutTrailingSlash)) {
            return StrUtil.EMPTY;
        }

        return FileNameUtil.getName(keyWithoutTrailingSlash);
    }

    /**
     * 获取文件扩展名
     *
     * @param objectKey 对象 Key
     * @return 文件扩展名，不包含点号
     */
    @Override
    public String getExtension(String objectKey) {
        String filename = getFilename(objectKey);

        if (StrUtil.isBlank(filename)) {
            return StrUtil.EMPTY;
        }

        return StrUtil.blankToDefault(FileNameUtil.extName(filename), StrUtil.EMPTY);
    }

    /**
     * 获取默认存储桶下对象的公开访问 URL
     *
     * @param objectKey 对象 Key
     * @return 公开访问 URL
     */
    @Override
    public String getPublicUrl(String objectKey) {
        return getPublicUrl(getDefaultBucketName(), objectKey);
    }

    /**
     * 获取指定存储桶下对象的公开访问 URL
     *
     * @param bucketName 存储桶名称
     * @param objectKey  对象 Key
     * @return 公开访问 URL
     */
    @Override
    public String getPublicUrl(String bucketName, String objectKey) {
        String resolvedBucketName = resolveBucketName(bucketName);
        String normalizedObjectKey = normalizeObjectKey(objectKey);
        String encodedObjectKey = encodeObjectKey(normalizedObjectKey);

        String endpoint = normalizeEndpoint(s3Properties.getEndpoint());
        boolean pathStyleAccess = BooleanUtil.isTrue(s3Properties.getPathStyleAccess());

        if (StrUtil.isBlank(endpoint)) {
            return buildAwsDefaultPublicUrl(resolvedBucketName, encodedObjectKey, pathStyleAccess);
        }

        URI endpointUri = URI.create(endpoint);
        Assert.notBlank(endpointUri.getScheme(), "S3 endpoint 必须包含协议，例如：https://minio.example.com");
        Assert.notBlank(endpointUri.getHost(), "S3 endpoint 必须包含主机，例如：https://minio.example.com");

        if (pathStyleAccess || isEndpointNotSuitableForVirtualHost(endpointUri)) {
            return joinUrlPath(endpoint, encodePathSegment(resolvedBucketName), encodedObjectKey);
        }

        return buildVirtualHostPublicUrl(endpointUri, resolvedBucketName, encodedObjectKey);
    }

    // ==================== 对象基础操作 ====================

    /**
     * 判断默认存储桶下的对象是否存在
     *
     * @param objectKey 对象 Key
     * @return true 存在，false 不存在
     */
    @Override
    public boolean objectExists(String objectKey) {
        return objectExists(getDefaultBucketName(), objectKey);
    }

    /**
     * 判断指定存储桶下的对象是否存在
     *
     * @param bucketName 存储桶名称
     * @param objectKey  对象 Key
     * @return true 存在，false 不存在
     */
    @Override
    public boolean objectExists(String bucketName, String objectKey) {
        String resolvedBucketName = resolveBucketName(bucketName);
        String normalizedObjectKey = normalizeObjectKey(objectKey);

        try {
            HeadObjectRequest request = buildHeadObjectRequest(resolvedBucketName, normalizedObjectKey);
            s3Client.headObject(request);
            return true;
        } catch (NoSuchKeyException e) {
            log.info("S3 对象不存在，bucketName={}，objectKey={}", resolvedBucketName, normalizedObjectKey);
            return false;
        } catch (NoSuchBucketException e) {
            log.info("S3 存储桶不存在，bucketName={}，objectKey={}", resolvedBucketName, normalizedObjectKey);
            return false;
        } catch (S3Exception e) {
            if (isNotFound(e)) {
                log.info("S3 对象不存在，bucketName={}，objectKey={}，errorCode={}",
                        resolvedBucketName, normalizedObjectKey, getErrorCode(e));
                return false;
            }

            log.error("检查 S3 对象是否存在失败，bucketName={}，objectKey={}，statusCode={}，errorCode={}，message={}",
                    resolvedBucketName, normalizedObjectKey, e.statusCode(), getErrorCode(e), e.getMessage(), e);
            throw e;
        }
    }

    /**
     * 获取默认存储桶下的对象信息
     *
     * @param objectKey 对象 Key
     * @return 对象信息
     */
    @Override
    public S3ObjectInfo getObjectInfo(String objectKey) {
        return getObjectInfo(getDefaultBucketName(), objectKey);
    }

    /**
     * 获取指定存储桶下的对象信息
     *
     * @param bucketName 存储桶名称
     * @param objectKey  对象 Key
     * @return 对象信息
     */
    @Override
    public S3ObjectInfo getObjectInfo(String bucketName, String objectKey) {
        String resolvedBucketName = resolveBucketName(bucketName);
        String normalizedObjectKey = normalizeObjectKey(objectKey);

        try {
            HeadObjectRequest request = buildHeadObjectRequest(resolvedBucketName, normalizedObjectKey);
            HeadObjectResponse response = s3Client.headObject(request);

            return buildS3ObjectInfo(resolvedBucketName, normalizedObjectKey, response);
        } catch (NoSuchKeyException e) {
            log.warn("获取 S3 对象信息失败，对象不存在，bucketName={}，objectKey={}",
                    resolvedBucketName, normalizedObjectKey);
            throw e;
        } catch (NoSuchBucketException e) {
            log.warn("获取 S3 对象信息失败，存储桶不存在，bucketName={}，objectKey={}",
                    resolvedBucketName, normalizedObjectKey);
            throw e;
        } catch (S3Exception e) {
            log.error("获取 S3 对象信息失败，bucketName={}，objectKey={}，statusCode={}，errorCode={}，message={}",
                    resolvedBucketName, normalizedObjectKey, e.statusCode(), getErrorCode(e), e.getMessage(), e);
            throw e;
        }
    }

    /**
     * 获取默认存储桶下的对象大小
     *
     * @param objectKey 对象 Key
     * @return 对象大小，单位字节
     */
    @Override
    public Long getObjectSize(String objectKey) {
        return getObjectSize(getDefaultBucketName(), objectKey);
    }

    /**
     * 获取指定存储桶下的对象大小
     *
     * @param bucketName 存储桶名称
     * @param objectKey  对象 Key
     * @return 对象大小，单位字节
     */
    @Override
    public Long getObjectSize(String bucketName, String objectKey) {
        return getObjectInfo(bucketName, objectKey).size();
    }

    /**
     * 获取默认存储桶下的对象 Content-Type
     *
     * @param objectKey 对象 Key
     * @return Content-Type
     */
    @Override
    public String getObjectContentType(String objectKey) {
        return getObjectContentType(getDefaultBucketName(), objectKey);
    }

    /**
     * 获取指定存储桶下的对象 Content-Type
     *
     * @param bucketName 存储桶名称
     * @param objectKey  对象 Key
     * @return Content-Type
     */
    @Override
    public String getObjectContentType(String bucketName, String objectKey) {
        return getObjectInfo(bucketName, objectKey).contentType();
    }

    // ==================== 对象上传 ====================

    /**
     * 上传 MultipartFile 到默认存储桶，自动生成对象 Key
     *
     * @param file 上传文件
     * @return 上传结果
     */
    @Override
    public S3UploadResult upload(MultipartFile file) {
        Assert.notNull(file, "上传文件不能为空");

        String objectKey = buildAutoObjectKey(file.getOriginalFilename());
        return upload(objectKey, file);
    }

    /**
     * 上传 MultipartFile 到默认存储桶
     *
     * @param objectKey 对象 Key
     * @param file      上传文件
     * @return 上传结果
     */
    @Override
    public S3UploadResult upload(String objectKey, MultipartFile file) {
        return upload(getDefaultBucketName(), objectKey, file);
    }

    /**
     * 上传 MultipartFile 到指定存储桶
     *
     * @param bucketName 存储桶名称
     * @param objectKey  对象 Key
     * @param file       上传文件
     * @return 上传结果
     */
    @Override
    public S3UploadResult upload(String bucketName, String objectKey, MultipartFile file) {
        Assert.notNull(file, "上传文件不能为空");

        String resolvedBucketName = resolveBucketName(bucketName);
        String normalizedObjectKey = normalizeObjectKey(objectKey);
        String originalFilename = normalizeOriginalFilename(file.getOriginalFilename());
        long contentLength = file.getSize();
        String contentType = resolveContentType(file.getContentType(), normalizedObjectKey);

        try (InputStream inputStream = file.getInputStream()) {
            S3UploadRequest request = new S3UploadRequest(
                    resolvedBucketName,
                    normalizedObjectKey,
                    inputStream,
                    contentLength,
                    contentType,
                    Map.of(),
                    Map.of(),
                    null,
                    null,
                    null,
                    null
            );

            return uploadInternal(request, originalFilename, RequestBody.fromInputStream(inputStream, contentLength));
        } catch (IOException e) {
            log.error("读取 MultipartFile 上传流失败，bucketName={}，objectKey={}，originalFilename={}",
                    resolvedBucketName, normalizedObjectKey, originalFilename, e);
            throw new UncheckedIOException("读取 MultipartFile 上传流失败", e);
        }
    }

    /**
     * 上传字节数组到默认存储桶
     *
     * @param bytes     字节数组
     * @param objectKey 对象 Key
     * @return 上传结果
     */
    @Override
    public S3UploadResult upload(byte[] bytes, String objectKey) {
        return upload(getDefaultBucketName(), objectKey, bytes);
    }

    /**
     * 上传字节数组到指定存储桶
     *
     * @param bucketName 存储桶名称
     * @param objectKey  对象 Key
     * @param bytes      字节数组
     * @return 上传结果
     */
    @Override
    public S3UploadResult upload(String bucketName, String objectKey, byte[] bytes) {
        Assert.notNull(bytes, "上传字节数组不能为空");

        String resolvedBucketName = resolveBucketName(bucketName);
        String normalizedObjectKey = normalizeObjectKey(objectKey);
        long contentLength = bytes.length;

        S3UploadRequest request = new S3UploadRequest(
                resolvedBucketName,
                normalizedObjectKey,
                null,
                contentLength,
                resolveContentType(null, normalizedObjectKey),
                Map.of(),
                Map.of(),
                null,
                null,
                null,
                null
        );

        return uploadInternal(request, getFilename(normalizedObjectKey), RequestBody.fromBytes(bytes));
    }

    /**
     * 上传输入流到默认存储桶
     *
     * @param inputStream   输入流
     * @param objectKey     对象 Key
     * @param contentLength 内容长度
     * @return 上传结果
     */
    @Override
    public S3UploadResult upload(InputStream inputStream, String objectKey, long contentLength) {
        return upload(getDefaultBucketName(), objectKey, inputStream, contentLength);
    }

    /**
     * 上传输入流到指定存储桶
     *
     * @param bucketName    存储桶名称
     * @param objectKey     对象 Key
     * @param inputStream   输入流
     * @param contentLength 内容长度
     * @return 上传结果
     */
    @Override
    public S3UploadResult upload(String bucketName, String objectKey, InputStream inputStream, long contentLength) {
        Assert.notNull(inputStream, "上传输入流不能为空");
        Assert.isTrue(contentLength >= 0, "S3 上传内容长度不能小于 0");

        String resolvedBucketName = resolveBucketName(bucketName);
        String normalizedObjectKey = normalizeObjectKey(objectKey);

        S3UploadRequest request = new S3UploadRequest(
                resolvedBucketName,
                normalizedObjectKey,
                inputStream,
                contentLength,
                resolveContentType(null, normalizedObjectKey),
                Map.of(),
                Map.of(),
                null,
                null,
                null,
                null
        );

        return upload(request);
    }

    /**
     * 上传本地文件到默认存储桶
     *
     * @param filePath  本地文件路径
     * @param objectKey 对象 Key
     * @return 上传结果
     */
    @Override
    public S3UploadResult upload(Path filePath, String objectKey) {
        return upload(getDefaultBucketName(), objectKey, filePath);
    }

    /**
     * 上传本地文件到指定存储桶
     *
     * @param bucketName 存储桶名称
     * @param objectKey  对象 Key
     * @param filePath   本地文件路径
     * @return 上传结果
     */
    @Override
    public S3UploadResult upload(String bucketName, String objectKey, Path filePath) {
        Assert.notNull(filePath, "上传文件路径不能为空");
        Assert.isTrue(Files.exists(filePath), "上传文件不存在：{}", filePath);
        Assert.isTrue(Files.isRegularFile(filePath), "上传路径不是普通文件：{}", filePath);

        String resolvedBucketName = resolveBucketName(bucketName);
        String normalizedObjectKey = normalizeObjectKey(objectKey);
        String originalFilename = filePath.getFileName() == null ? getFilename(normalizedObjectKey) : filePath.getFileName().toString();

        try {
            long contentLength = Files.size(filePath);
            String contentType = resolveContentType(Files.probeContentType(filePath), normalizedObjectKey);

            S3UploadRequest request = new S3UploadRequest(
                    resolvedBucketName,
                    normalizedObjectKey,
                    null,
                    contentLength,
                    contentType,
                    Map.of(),
                    Map.of(),
                    null,
                    null,
                    null,
                    null
            );

            return uploadInternal(request, originalFilename, RequestBody.fromFile(filePath));
        } catch (IOException e) {
            log.error("读取本地文件上传失败，bucketName={}，objectKey={}，filePath={}",
                    resolvedBucketName, normalizedObjectKey, filePath, e);
            throw new UncheckedIOException("读取本地文件上传失败：" + filePath, e);
        }
    }

    /**
     * 根据上传请求上传对象
     *
     * @param request 上传请求
     * @return 上传结果
     */
    @Override
    public S3UploadResult upload(S3UploadRequest request) {
        Assert.notNull(request, "S3 上传请求不能为空");
        Assert.notNull(request.inputStream(), "S3 上传输入流不能为空");
        Assert.notNull(request.contentLength(), "S3 上传内容长度不能为空");
        Assert.isTrue(request.contentLength() >= 0, "S3 上传内容长度不能小于 0");

        String normalizedObjectKey = normalizeObjectKey(request.objectKey());

        return uploadInternal(
                request,
                getFilename(normalizedObjectKey),
                RequestBody.fromInputStream(request.inputStream(), request.contentLength())
        );
    }

    /**
     * 批量上传对象
     *
     * @param requests 上传请求列表
     * @return 上传结果列表
     */
    @Override
    public List<S3UploadResult> uploadBatch(List<S3UploadRequest> requests) {
        if (CollUtil.isEmpty(requests)) {
            return List.of();
        }

        List<S3UploadResult> results = new ArrayList<>(requests.size());

        for (int i = 0; i < requests.size(); i++) {
            S3UploadRequest request = requests.get(i);
            Assert.notNull(request, "S3 批量上传请求不能为空，index={}", i);

            try {
                results.add(upload(request));
            } catch (RuntimeException e) {
                log.error("S3 批量上传失败，index={}，bucketName={}，objectKey={}",
                        i, request.bucketName(), request.objectKey(), e);
                throw e;
            }
        }

        log.info("S3 批量上传完成，total={}", results.size());
        return results;
    }

    // ==================== 对象下载 ====================

    /**
     * 下载默认存储桶下的对象为字节数组
     *
     * @param objectKey 对象 Key
     * @return 字节数组
     */
    @Override
    public byte[] downloadAsBytes(String objectKey) {
        return downloadAsBytes(getDefaultBucketName(), objectKey);
    }

    /**
     * 下载指定存储桶下的对象为字节数组
     *
     * @param bucketName 存储桶名称
     * @param objectKey  对象 Key
     * @return 字节数组
     */
    @Override
    public byte[] downloadAsBytes(String bucketName, String objectKey) {
        String resolvedBucketName = resolveBucketName(bucketName);
        String normalizedObjectKey = normalizeObjectKey(objectKey);

        try {
            GetObjectRequest request = buildGetObjectRequest(resolvedBucketName, normalizedObjectKey);
            ResponseBytes<GetObjectResponse> responseBytes = s3Client.getObject(request, ResponseTransformer.toBytes());

            log.info("下载 S3 对象为字节数组成功，bucketName={}，objectKey={}，size={}",
                    resolvedBucketName, normalizedObjectKey, responseBytes.asByteArray().length);

            return responseBytes.asByteArray();
        } catch (NoSuchKeyException e) {
            log.warn("下载 S3 对象失败，对象不存在，bucketName={}，objectKey={}",
                    resolvedBucketName, normalizedObjectKey);
            throw e;
        } catch (NoSuchBucketException e) {
            log.warn("下载 S3 对象失败，存储桶不存在，bucketName={}，objectKey={}",
                    resolvedBucketName, normalizedObjectKey);
            throw e;
        } catch (S3Exception e) {
            log.error("下载 S3 对象为字节数组失败，bucketName={}，objectKey={}，statusCode={}，errorCode={}，message={}",
                    resolvedBucketName, normalizedObjectKey, e.statusCode(), getErrorCode(e), e.getMessage(), e);
            throw e;
        }
    }

    /**
     * 下载默认存储桶下的对象为输入流
     *
     * @param objectKey 对象 Key
     * @return 输入流，调用方需要关闭
     */
    @Override
    public InputStream downloadAsStream(String objectKey) {
        return downloadAsStream(getDefaultBucketName(), objectKey);
    }

    /**
     * 下载指定存储桶下的对象为输入流
     *
     * @param bucketName 存储桶名称
     * @param objectKey  对象 Key
     * @return 输入流，调用方需要关闭
     */
    @Override
    public InputStream downloadAsStream(String bucketName, String objectKey) {
        S3DownloadRequest request = new S3DownloadRequest(
                bucketName,
                objectKey,
                null,
                null,
                null
        );

        return download(request).inputStream();
    }

    /**
     * 下载默认存储桶下的对象为 Spring Resource
     *
     * @param objectKey 对象 Key
     * @return Spring Resource
     */
    @Override
    public Resource downloadAsResource(String objectKey) {
        return downloadAsResource(getDefaultBucketName(), objectKey);
    }

    /**
     * 下载指定存储桶下的对象为 Spring Resource
     *
     * @param bucketName 存储桶名称
     * @param objectKey  对象 Key
     * @return Spring Resource
     */
    @Override
    public Resource downloadAsResource(String bucketName, String objectKey) {
        S3DownloadRequest request = new S3DownloadRequest(
                bucketName,
                objectKey,
                null,
                null,
                null
        );

        S3DownloadResult result = download(request);

        return new InputStreamResource(result.inputStream()) {

            /**
             * 获取资源文件名
             *
             * @return 文件名
             */
            @Override
            public String getFilename() {
                return result.filename();
            }

            /**
             * 获取资源内容长度
             *
             * @return 内容长度
             */
            @Override
            public long contentLength() {
                return result.contentLength() == null ? -1L : result.contentLength();
            }
        };
    }

    /**
     * 下载默认存储桶下的对象到本地文件
     *
     * @param objectKey  对象 Key
     * @param targetPath 本地目标路径
     */
    @Override
    public void downloadToFile(String objectKey, Path targetPath) {
        downloadToFile(getDefaultBucketName(), objectKey, targetPath);
    }

    /**
     * 下载指定存储桶下的对象到本地文件
     *
     * @param bucketName 存储桶名称
     * @param objectKey  对象 Key
     * @param targetPath 本地目标路径
     */
    @Override
    public void downloadToFile(String bucketName, String objectKey, Path targetPath) {
        Assert.notNull(targetPath, "S3 下载目标路径不能为空");
        Assert.isFalse(Files.isDirectory(targetPath), "S3 下载目标路径不能是目录：{}", targetPath);

        String resolvedBucketName = resolveBucketName(bucketName);
        String normalizedObjectKey = normalizeObjectKey(objectKey);

        try {
            Path parentPath = targetPath.toAbsolutePath().getParent();
            if (parentPath != null) {
                Files.createDirectories(parentPath);
            }

            GetObjectRequest request = buildGetObjectRequest(resolvedBucketName, normalizedObjectKey);
            s3Client.getObject(request, ResponseTransformer.toFile(targetPath));

            log.info("下载 S3 对象到本地文件成功，bucketName={}，objectKey={}，targetPath={}",
                    resolvedBucketName, normalizedObjectKey, targetPath);
        } catch (IOException e) {
            log.error("创建 S3 下载目标文件目录失败，bucketName={}，objectKey={}，targetPath={}",
                    resolvedBucketName, normalizedObjectKey, targetPath, e);
            throw new UncheckedIOException("创建 S3 下载目标文件目录失败：" + targetPath, e);
        } catch (NoSuchKeyException e) {
            log.warn("下载 S3 对象到本地文件失败，对象不存在，bucketName={}，objectKey={}，targetPath={}",
                    resolvedBucketName, normalizedObjectKey, targetPath);
            throw e;
        } catch (NoSuchBucketException e) {
            log.warn("下载 S3 对象到本地文件失败，存储桶不存在，bucketName={}，objectKey={}，targetPath={}",
                    resolvedBucketName, normalizedObjectKey, targetPath);
            throw e;
        } catch (S3Exception e) {
            log.error("下载 S3 对象到本地文件失败，bucketName={}，objectKey={}，targetPath={}，statusCode={}，errorCode={}，message={}",
                    resolvedBucketName, normalizedObjectKey, targetPath, e.statusCode(), getErrorCode(e), e.getMessage(), e);
            throw e;
        }
    }

    /**
     * 根据下载请求下载对象
     *
     * @param request 下载请求
     * @return 下载结果，inputStream 需要调用方关闭
     */
    @Override
    public S3DownloadResult download(S3DownloadRequest request) {
        Assert.notNull(request, "S3 下载请求不能为空");

        String resolvedBucketName = resolveBucketName(request.bucketName());
        String normalizedObjectKey = normalizeObjectKey(request.objectKey());

        try {
            GetObjectRequest getObjectRequest = buildGetObjectRequest(request);
            ResponseInputStream<GetObjectResponse> responseInputStream = s3Client.getObject(getObjectRequest);
            GetObjectResponse response = responseInputStream.response();

            S3DownloadResult result = new S3DownloadResult(
                    resolvedBucketName,
                    normalizedObjectKey,
                    getFilename(normalizedObjectKey),
                    response.contentType(),
                    response.contentLength(),
                    normalizeMetadata(response.metadata()),
                    responseInputStream
            );

            log.info("打开 S3 对象下载流成功，bucketName={}，objectKey={}，versionId={}，range={}，contentLength={}",
                    resolvedBucketName,
                    normalizedObjectKey,
                    StrUtil.blankToDefault(request.versionId(), "默认版本"),
                    StrUtil.blankToDefault(getDownloadRange(request.rangeStart(), request.rangeEnd()), "完整对象"),
                    result.contentLength());

            return result;
        } catch (NoSuchKeyException e) {
            log.warn("打开 S3 对象下载流失败，对象不存在，bucketName={}，objectKey={}",
                    resolvedBucketName, normalizedObjectKey);
            throw e;
        } catch (NoSuchBucketException e) {
            log.warn("打开 S3 对象下载流失败，存储桶不存在，bucketName={}，objectKey={}",
                    resolvedBucketName, normalizedObjectKey);
            throw e;
        } catch (S3Exception e) {
            log.error("打开 S3 对象下载流失败，bucketName={}，objectKey={}，statusCode={}，errorCode={}，message={}",
                    resolvedBucketName, normalizedObjectKey, e.statusCode(), getErrorCode(e), e.getMessage(), e);
            throw e;
        }
    }

    // ==================== 对象删除 ====================

    /**
     * 删除默认存储桶下的对象
     *
     * @param objectKey 对象 Key
     */
    @Override
    public void delete(String objectKey) {
        delete(getDefaultBucketName(), objectKey);
    }

    /**
     * 删除指定存储桶下的对象
     *
     * @param bucketName 存储桶名称
     * @param objectKey  对象 Key
     */
    @Override
    public void delete(String bucketName, String objectKey) {
        String resolvedBucketName = resolveBucketName(bucketName);
        String normalizedObjectKey = normalizeObjectKey(objectKey);

        try {
            DeleteObjectRequest request = DeleteObjectRequest.builder()
                    .bucket(resolvedBucketName)
                    .key(normalizedObjectKey)
                    .build();

            s3Client.deleteObject(request);

            log.info("删除 S3 对象成功，bucketName={}，objectKey={}", resolvedBucketName, normalizedObjectKey);
        } catch (NoSuchBucketException e) {
            log.warn("删除 S3 对象失败，存储桶不存在，bucketName={}，objectKey={}",
                    resolvedBucketName, normalizedObjectKey);
            throw e;
        } catch (S3Exception e) {
            log.error("删除 S3 对象失败，bucketName={}，objectKey={}，statusCode={}，errorCode={}，message={}",
                    resolvedBucketName, normalizedObjectKey, e.statusCode(), getErrorCode(e), e.getMessage(), e);
            throw e;
        }
    }

    /**
     * 批量删除默认存储桶下的对象
     *
     * @param objectKeys 对象 Key 列表
     */
    @Override
    public void deleteBatch(List<String> objectKeys) {
        deleteBatch(getDefaultBucketName(), objectKeys);
    }

    /**
     * 批量删除指定存储桶下的对象
     *
     * @param bucketName 存储桶名称
     * @param objectKeys 对象 Key 列表
     */
    @Override
    public void deleteBatch(String bucketName, List<String> objectKeys) {
        S3DeleteResult result = deleteBatchDetailed(bucketName, objectKeys);
        assertNoDeleteErrors(result);
    }

    /**
     * 删除默认存储桶下指定前缀的对象
     *
     * @param prefix 对象 Key 前缀
     * @return 删除成功数量
     */
    @Override
    public Long deleteByPrefix(String prefix) {
        return deleteByPrefix(getDefaultBucketName(), prefix);
    }

    /**
     * 删除指定存储桶下指定前缀的对象
     *
     * @param bucketName 存储桶名称
     * @param prefix     对象 Key 前缀
     * @return 删除成功数量
     */
    @Override
    public Long deleteByPrefix(String bucketName, String prefix) {
        S3DeleteResult result = deleteByPrefixDetailed(bucketName, prefix);
        assertNoDeleteErrors(result);
        return result.deletedCount();
    }

    /**
     * 删除指定版本的对象
     *
     * @param bucketName 存储桶名称
     * @param objectKey  对象 Key
     * @param versionId  版本 ID
     */
    @Override
    public void deleteVersion(String bucketName, String objectKey, String versionId) {
        String resolvedBucketName = resolveBucketName(bucketName);
        String normalizedObjectKey = normalizeObjectKey(objectKey);
        String resolvedVersionId = StrUtil.trim(versionId);

        Assert.notBlank(resolvedVersionId, "S3 对象版本 ID 不能为空");

        try {
            DeleteObjectRequest request = DeleteObjectRequest.builder()
                    .bucket(resolvedBucketName)
                    .key(normalizedObjectKey)
                    .versionId(resolvedVersionId)
                    .build();

            s3Client.deleteObject(request);

            log.info("删除 S3 指定版本对象成功，bucketName={}，objectKey={}，versionId={}",
                    resolvedBucketName, normalizedObjectKey, resolvedVersionId);
        } catch (NoSuchBucketException e) {
            log.warn("删除 S3 指定版本对象失败，存储桶不存在，bucketName={}，objectKey={}，versionId={}",
                    resolvedBucketName, normalizedObjectKey, resolvedVersionId);
            throw e;
        } catch (S3Exception e) {
            log.error("删除 S3 指定版本对象失败，bucketName={}，objectKey={}，versionId={}，statusCode={}，errorCode={}，message={}",
                    resolvedBucketName, normalizedObjectKey, resolvedVersionId,
                    e.statusCode(), getErrorCode(e), e.getMessage(), e);
            throw e;
        }
    }

    /**
     * 批量删除默认存储桶下的对象，并返回详细结果
     *
     * @param objectKeys 对象 Key 列表
     * @return 删除详细结果
     */
    @Override
    public S3DeleteResult deleteBatchDetailed(List<String> objectKeys) {
        return deleteBatchDetailed(getDefaultBucketName(), objectKeys);
    }

    /**
     * 批量删除指定存储桶下的对象，并返回详细结果
     *
     * @param bucketName 存储桶名称
     * @param objectKeys 对象 Key 列表
     * @return 删除详细结果
     */
    @Override
    public S3DeleteResult deleteBatchDetailed(String bucketName, List<String> objectKeys) {
        String resolvedBucketName = resolveBucketName(bucketName);

        if (CollUtil.isEmpty(objectKeys)) {
            return emptyDeleteResult(resolvedBucketName);
        }

        List<ObjectIdentifier> objectIdentifiers = buildObjectIdentifiers(objectKeys);
        if (CollUtil.isEmpty(objectIdentifiers)) {
            return emptyDeleteResult(resolvedBucketName);
        }

        return deleteObjectIdentifiers(resolvedBucketName, objectIdentifiers);
    }

    /**
     * 删除默认存储桶下指定前缀的对象，并返回详细结果
     *
     * @param prefix 对象 Key 前缀
     * @return 删除详细结果
     */
    @Override
    public S3DeleteResult deleteByPrefixDetailed(String prefix) {
        return deleteByPrefixDetailed(getDefaultBucketName(), prefix);
    }

    /**
     * 删除指定存储桶下指定前缀的对象，并返回详细结果
     *
     * @param bucketName 存储桶名称
     * @param prefix     对象 Key 前缀
     * @return 删除详细结果
     */
    @Override
    public S3DeleteResult deleteByPrefixDetailed(String bucketName, String prefix) {
        String resolvedBucketName = resolveBucketName(bucketName);
        String normalizedPrefix = normalizeObjectKey(prefix);

        List<ObjectIdentifier> objectIdentifiers = listObjectIdentifiersByPrefix(resolvedBucketName, normalizedPrefix);
        if (CollUtil.isEmpty(objectIdentifiers)) {
            log.info("按前缀删除 S3 对象完成，未匹配到对象，bucketName={}，prefix={}",
                    resolvedBucketName, normalizedPrefix);
            return emptyDeleteResult(resolvedBucketName);
        }

        S3DeleteResult result = deleteObjectIdentifiers(resolvedBucketName, objectIdentifiers);

        log.info("按前缀删除 S3 对象完成，bucketName={}，prefix={}，deletedCount={}，errorCount={}",
                resolvedBucketName, normalizedPrefix, result.deletedCount(), result.errorCount());

        return result;
    }

    // ==================== 对象复制与移动 ====================

    /**
     * 在默认存储桶内复制对象
     *
     * @param sourceKey 源对象 Key
     * @param targetKey 目标对象 Key
     * @return 复制结果
     */
    @Override
    public S3CopyResult copy(String sourceKey, String targetKey) {
        return copy(getDefaultBucketName(), sourceKey, getDefaultBucketName(), targetKey);
    }

    /**
     * 复制对象
     *
     * @param sourceBucketName 源存储桶名称
     * @param sourceKey        源对象 Key
     * @param targetBucketName 目标存储桶名称
     * @param targetKey        目标对象 Key
     * @return 复制结果
     */
    @Override
    public S3CopyResult copy(String sourceBucketName, String sourceKey, String targetBucketName, String targetKey) {
        S3CopyRequest request = new S3CopyRequest(
                sourceBucketName,
                sourceKey,
                null,
                targetBucketName,
                targetKey,
                Map.of(),
                Map.of(),
                false,
                false,
                null,
                null,
                null,
                null
        );

        return copy(request);
    }

    /**
     * 根据复制请求复制对象
     *
     * @param request 复制请求
     * @return 复制结果
     */
    @Override
    public S3CopyResult copy(S3CopyRequest request) {
        Assert.notNull(request, "S3 复制请求不能为空");

        String sourceBucketName = resolveBucketName(request.sourceBucketName());
        String sourceKey = normalizeObjectKey(request.sourceKey());
        String targetBucketName = resolveBucketName(request.targetBucketName());
        String targetKey = normalizeObjectKey(request.targetKey());

        try {
            CopyObjectRequest copyObjectRequest = buildCopyObjectRequest(request);
            CopyObjectResponse response = s3Client.copyObject(copyObjectRequest);

            S3CopyResult result = new S3CopyResult(
                    sourceBucketName,
                    sourceKey,
                    targetBucketName,
                    targetKey,
                    response.copyObjectResult() == null ? null : response.copyObjectResult().eTag(),
                    response.versionId(),
                    response.copyObjectResult() == null ? Instant.now() : response.copyObjectResult().lastModified()
            );

            log.info("复制 S3 对象成功，sourceBucketName={}，sourceKey={}，targetBucketName={}，targetKey={}",
                    sourceBucketName, sourceKey, targetBucketName, targetKey);

            return result;
        } catch (NoSuchBucketException e) {
            log.warn("复制 S3 对象失败，存储桶不存在，sourceBucketName={}，sourceKey={}，targetBucketName={}，targetKey={}",
                    sourceBucketName, sourceKey, targetBucketName, targetKey);
            throw e;
        } catch (NoSuchKeyException e) {
            log.warn("复制 S3 对象失败，源对象不存在，sourceBucketName={}，sourceKey={}，targetBucketName={}，targetKey={}",
                    sourceBucketName, sourceKey, targetBucketName, targetKey);
            throw e;
        } catch (S3Exception e) {
            log.error("复制 S3 对象失败，sourceBucketName={}，sourceKey={}，targetBucketName={}，targetKey={}，statusCode={}，errorCode={}，message={}",
                    sourceBucketName, sourceKey, targetBucketName, targetKey,
                    e.statusCode(), getErrorCode(e), e.getMessage(), e);
            throw e;
        }
    }

    /**
     * 在默认存储桶内移动对象
     *
     * @param sourceKey 源对象 Key
     * @param targetKey 目标对象 Key
     * @return 移动结果
     */
    @Override
    public S3MoveResult move(String sourceKey, String targetKey) {
        return move(getDefaultBucketName(), sourceKey, getDefaultBucketName(), targetKey);
    }

    /**
     * 移动对象
     *
     * @param sourceBucketName 源存储桶名称
     * @param sourceKey        源对象 Key
     * @param targetBucketName 目标存储桶名称
     * @param targetKey        目标对象 Key
     * @return 移动结果
     */
    @Override
    public S3MoveResult move(String sourceBucketName, String sourceKey, String targetBucketName, String targetKey) {
        S3MoveRequest request = new S3MoveRequest(
                sourceBucketName,
                sourceKey,
                targetBucketName,
                targetKey,
                true
        );

        return move(request);
    }

    /**
     * 根据移动请求移动对象
     *
     * @param request 移动请求
     * @return 移动结果
     */
    @Override
    public S3MoveResult move(S3MoveRequest request) {
        Assert.notNull(request, "S3 移动请求不能为空");

        String sourceBucketName = resolveBucketName(request.sourceBucketName());
        String sourceKey = normalizeObjectKey(request.sourceKey());
        String targetBucketName = resolveBucketName(request.targetBucketName());
        String targetKey = normalizeObjectKey(request.targetKey());
        boolean overwrite = request.overwrite() == null || BooleanUtil.isTrue(request.overwrite());

        if (isSameObject(sourceBucketName, sourceKey, targetBucketName, targetKey)) {
            log.info("S3 源对象与目标对象一致，跳过移动，bucketName={}，objectKey={}", sourceBucketName, sourceKey);

            return new S3MoveResult(
                    sourceBucketName,
                    sourceKey,
                    targetBucketName,
                    targetKey,
                    false,
                    Instant.now()
            );
        }

        if (!overwrite && objectExists(targetBucketName, targetKey)) {
            String message = StrUtil.format(
                    "S3 移动目标对象已存在，targetBucketName={}，targetKey={}",
                    targetBucketName,
                    targetKey
            );
            throw new IllegalStateException(message);
        }

        S3CopyRequest copyRequest = new S3CopyRequest(
                sourceBucketName,
                sourceKey,
                null,
                targetBucketName,
                targetKey,
                Map.of(),
                Map.of(),
                false,
                false,
                null,
                null,
                null,
                null
        );

        copy(copyRequest);
        delete(sourceBucketName, sourceKey);

        S3MoveResult result = new S3MoveResult(
                sourceBucketName,
                sourceKey,
                targetBucketName,
                targetKey,
                true,
                Instant.now()
        );

        log.info("移动 S3 对象成功，sourceBucketName={}，sourceKey={}，targetBucketName={}，targetKey={}，overwrite={}",
                sourceBucketName, sourceKey, targetBucketName, targetKey, overwrite);

        return result;
    }

    /**
     * 在默认存储桶内重命名对象
     *
     * @param sourceKey 源对象 Key
     * @param targetKey 目标对象 Key
     */
    @Override
    public void rename(String sourceKey, String targetKey) {
        rename(getDefaultBucketName(), sourceKey, targetKey);
    }

    /**
     * 在指定存储桶内重命名对象
     *
     * @param bucketName 存储桶名称
     * @param sourceKey  源对象 Key
     * @param targetKey  目标对象 Key
     */
    @Override
    public void rename(String bucketName, String sourceKey, String targetKey) {
        move(bucketName, sourceKey, bucketName, targetKey);
    }

    // ==================== 对象列表与分页查询 ====================

    /**
     * 查询默认存储桶当前层级对象列表
     *
     * @return 对象信息列表
     */
    @Override
    public List<S3ObjectInfo> listObjects() {
        return listObjects(getDefaultBucketName(), null);
    }

    /**
     * 查询默认存储桶指定前缀当前层级对象列表
     *
     * @param prefix 对象 Key 前缀
     * @return 对象信息列表
     */
    @Override
    public List<S3ObjectInfo> listObjects(String prefix) {
        return listObjects(getDefaultBucketName(), prefix);
    }

    /**
     * 查询指定存储桶指定前缀当前层级对象列表
     *
     * @param bucketName 存储桶名称
     * @param prefix     对象 Key 前缀
     * @return 对象信息列表
     */
    @Override
    public List<S3ObjectInfo> listObjects(String bucketName, String prefix) {
        String resolvedBucketName = resolveBucketName(bucketName);
        String normalizedPrefix = normalizeObjectPrefix(prefix);

        List<S3ObjectInfo> objects = new ArrayList<>();
        String continuationToken = null;

        do {
            S3ListRequest request = new S3ListRequest(
                    resolvedBucketName,
                    normalizedPrefix,
                    "/",
                    1000,
                    continuationToken,
                    false
            );

            S3ObjectPage page = listObjectsPage(request);
            objects.addAll(page.objects());
            continuationToken = page.nextContinuationToken();
        } while (StrUtil.isNotBlank(continuationToken));

        return objects;
    }

    /**
     * 分页查询对象列表
     *
     * @param request 列表查询请求
     * @return 对象分页结果
     */
    @Override
    public S3ObjectPage listObjectsPage(S3ListRequest request) {
        Assert.notNull(request, "S3 对象列表查询请求不能为空");

        String resolvedBucketName = resolveBucketName(request.bucketName());
        String normalizedPrefix = normalizeObjectPrefix(request.prefix());
        Integer maxKeys = normalizeMaxKeys(request.maxKeys());

        try {
            ListObjectsV2Request listObjectsRequest = buildListObjectsV2Request(
                    resolvedBucketName,
                    normalizedPrefix,
                    request.delimiter(),
                    maxKeys,
                    request.continuationToken(),
                    request.recursive()
            );

            ListObjectsV2Response response = s3Client.listObjectsV2(listObjectsRequest);

            List<S3ObjectInfo> objects = response.contents()
                    .stream()
                    .map(object -> buildS3ObjectInfo(resolvedBucketName, object))
                    .toList();

            List<String> commonPrefixes = response.commonPrefixes()
                    .stream()
                    .map(CommonPrefix::prefix)
                    .filter(StrUtil::isNotBlank)
                    .toList();

            return new S3ObjectPage(
                    resolvedBucketName,
                    normalizedPrefix,
                    objects,
                    commonPrefixes,
                    response.isTruncated(),
                    response.nextContinuationToken(),
                    maxKeys
            );
        } catch (NoSuchBucketException e) {
            log.warn("分页查询 S3 对象列表失败，存储桶不存在，bucketName={}，prefix={}",
                    resolvedBucketName, normalizedPrefix);
            throw e;
        } catch (S3Exception e) {
            log.error("分页查询 S3 对象列表失败，bucketName={}，prefix={}，statusCode={}，errorCode={}，message={}",
                    resolvedBucketName, normalizedPrefix, e.statusCode(), getErrorCode(e), e.getMessage(), e);
            throw e;
        }
    }

    /**
     * 递归查询默认存储桶指定前缀下的全部对象
     *
     * @param prefix 对象 Key 前缀
     * @return 对象信息列表
     */
    @Override
    public List<S3ObjectInfo> listObjectsRecursive(String prefix) {
        return listObjectsRecursive(getDefaultBucketName(), prefix);
    }

    /**
     * 递归查询指定存储桶指定前缀下的全部对象
     *
     * @param bucketName 存储桶名称
     * @param prefix     对象 Key 前缀
     * @return 对象信息列表
     */
    @Override
    public List<S3ObjectInfo> listObjectsRecursive(String bucketName, String prefix) {
        String resolvedBucketName = resolveBucketName(bucketName);
        String normalizedPrefix = normalizeObjectPrefix(prefix);

        List<S3ObjectInfo> objects = new ArrayList<>();
        String continuationToken = null;

        do {
            S3ListRequest request = new S3ListRequest(
                    resolvedBucketName,
                    normalizedPrefix,
                    null,
                    1000,
                    continuationToken,
                    true
            );

            S3ObjectPage page = listObjectsPage(request);
            objects.addAll(page.objects());
            continuationToken = page.nextContinuationToken();
        } while (StrUtil.isNotBlank(continuationToken));

        return objects;
    }

    /**
     * 递归查询默认存储桶指定前缀下的对象 Key 列表
     *
     * @param prefix 对象 Key 前缀
     * @return 对象 Key 列表
     */
    @Override
    public List<String> listObjectKeys(String prefix) {
        return listObjectKeys(getDefaultBucketName(), prefix);
    }

    /**
     * 递归查询指定存储桶指定前缀下的对象 Key 列表
     *
     * @param bucketName 存储桶名称
     * @param prefix     对象 Key 前缀
     * @return 对象 Key 列表
     */
    @Override
    public List<String> listObjectKeys(String bucketName, String prefix) {
        return listObjectsRecursive(bucketName, prefix)
                .stream()
                .map(S3ObjectInfo::objectKey)
                .filter(StrUtil::isNotBlank)
                .toList();
    }

    /**
     * 查询对象版本列表
     *
     * @param bucketName 存储桶名称
     * @param prefix     对象 Key 前缀
     * @return 对象版本信息列表
     */
    @Override
    public List<S3ObjectVersionInfo> listObjectVersions(String bucketName, String prefix) {
        String resolvedBucketName = resolveBucketName(bucketName);
        String normalizedPrefix = normalizeObjectPrefix(prefix);

        List<S3ObjectVersionInfo> versions = new ArrayList<>();
        String keyMarker = null;
        String versionIdMarker = null;

        try {
            do {
                ListObjectVersionsRequest.Builder builder = ListObjectVersionsRequest.builder()
                        .bucket(resolvedBucketName)
                        .maxKeys(1000);

                if (StrUtil.isNotBlank(normalizedPrefix)) {
                    builder.prefix(normalizedPrefix);
                }

                if (StrUtil.isNotBlank(keyMarker)) {
                    builder.keyMarker(keyMarker);
                }

                if (StrUtil.isNotBlank(versionIdMarker)) {
                    builder.versionIdMarker(versionIdMarker);
                }

                ListObjectVersionsResponse response = s3Client.listObjectVersions(builder.build());

                versions.addAll(response.versions()
                        .stream()
                        .map(objectVersion -> buildS3ObjectVersionInfo(resolvedBucketName, objectVersion))
                        .toList());

                versions.addAll(response.deleteMarkers()
                        .stream()
                        .map(deleteMarker -> buildS3ObjectVersionInfo(resolvedBucketName, deleteMarker))
                        .toList());

                keyMarker = response.nextKeyMarker();
                versionIdMarker = response.nextVersionIdMarker();
            } while (StrUtil.isNotBlank(keyMarker));

            return versions;
        } catch (NoSuchBucketException e) {
            log.warn("查询 S3 对象版本列表失败，存储桶不存在，bucketName={}，prefix={}",
                    resolvedBucketName, normalizedPrefix);
            throw e;
        } catch (S3Exception e) {
            log.error("查询 S3 对象版本列表失败，bucketName={}，prefix={}，statusCode={}，errorCode={}，message={}",
                    resolvedBucketName, normalizedPrefix, e.statusCode(), getErrorCode(e), e.getMessage(), e);
            throw e;
        }
    }

    // ==================== 目录语义操作 ====================

    /**
     * 在默认存储桶下创建目录占位对象
     *
     * @param directoryKey 目录 Key
     */
    @Override
    public void createDirectory(String directoryKey) {
        createDirectory(getDefaultBucketName(), directoryKey);
    }

    /**
     * 在指定存储桶下创建目录占位对象
     *
     * @param bucketName   存储桶名称
     * @param directoryKey 目录 Key
     */
    @Override
    public void createDirectory(String bucketName, String directoryKey) {
        String resolvedBucketName = resolveBucketName(bucketName);
        String normalizedDirectoryKey = normalizeDirectoryKey(directoryKey);

        try {
            PutObjectRequest request = PutObjectRequest.builder()
                    .bucket(resolvedBucketName)
                    .key(normalizedDirectoryKey)
                    .contentLength(0L)
                    .contentType("application/x-directory")
                    .build();

            PutObjectResponse response = s3Client.putObject(request, RequestBody.empty());

            log.info("创建 S3 目录占位对象成功，bucketName={}，directoryKey={}，eTag={}，versionId={}",
                    resolvedBucketName, normalizedDirectoryKey, response.eTag(), response.versionId());
        } catch (NoSuchBucketException e) {
            log.warn("创建 S3 目录失败，存储桶不存在，bucketName={}，directoryKey={}",
                    resolvedBucketName, normalizedDirectoryKey);
            throw e;
        } catch (S3Exception e) {
            log.error("创建 S3 目录失败，bucketName={}，directoryKey={}，statusCode={}，errorCode={}，message={}",
                    resolvedBucketName, normalizedDirectoryKey, e.statusCode(), getErrorCode(e), e.getMessage(), e);
            throw e;
        }
    }

    /**
     * 判断默认存储桶下的目录是否存在
     *
     * @param directoryKey 目录 Key
     * @return true 存在，false 不存在
     */
    @Override
    public boolean directoryExists(String directoryKey) {
        return directoryExists(getDefaultBucketName(), directoryKey);
    }

    /**
     * 判断指定存储桶下的目录是否存在
     *
     * @param bucketName   存储桶名称
     * @param directoryKey 目录 Key
     * @return true 存在，false 不存在
     */
    @Override
    public boolean directoryExists(String bucketName, String directoryKey) {
        String resolvedBucketName = resolveBucketName(bucketName);
        String normalizedDirectoryKey = normalizeDirectoryKey(directoryKey);

        if (objectExists(resolvedBucketName, normalizedDirectoryKey)) {
            return true;
        }

        try {
            ListObjectsV2Request request = ListObjectsV2Request.builder()
                    .bucket(resolvedBucketName)
                    .prefix(normalizedDirectoryKey)
                    .maxKeys(1)
                    .build();

            ListObjectsV2Response response = s3Client.listObjectsV2(request);
            boolean exists = CollUtil.isNotEmpty(response.contents());

            if (!exists) {
                log.info("S3 目录不存在，bucketName={}，directoryKey={}",
                        resolvedBucketName, normalizedDirectoryKey);
            }

            return exists;
        } catch (NoSuchBucketException e) {
            log.info("判断 S3 目录是否存在失败，存储桶不存在，bucketName={}，directoryKey={}",
                    resolvedBucketName, normalizedDirectoryKey);
            return false;
        } catch (S3Exception e) {
            if (isNotFound(e)) {
                log.info("S3 目录不存在，bucketName={}，directoryKey={}，errorCode={}",
                        resolvedBucketName, normalizedDirectoryKey, getErrorCode(e));
                return false;
            }

            log.error("判断 S3 目录是否存在失败，bucketName={}，directoryKey={}，statusCode={}，errorCode={}，message={}",
                    resolvedBucketName, normalizedDirectoryKey, e.statusCode(), getErrorCode(e), e.getMessage(), e);
            throw e;
        }
    }

    /**
     * 删除默认存储桶下的目录及其所有对象
     *
     * @param directoryKey 目录 Key
     * @return 删除成功数量
     */
    @Override
    public Long deleteDirectory(String directoryKey) {
        return deleteDirectory(getDefaultBucketName(), directoryKey);
    }

    /**
     * 删除指定存储桶下的目录及其所有对象
     *
     * @param bucketName   存储桶名称
     * @param directoryKey 目录 Key
     * @return 删除成功数量
     */
    @Override
    public Long deleteDirectory(String bucketName, String directoryKey) {
        String resolvedBucketName = resolveBucketName(bucketName);
        String normalizedDirectoryKey = normalizeDirectoryKey(directoryKey);

        S3DeleteResult result = deleteByPrefixDetailed(resolvedBucketName, normalizedDirectoryKey);
        assertNoDeleteErrors(result);

        log.info("删除 S3 目录完成，bucketName={}，directoryKey={}，deletedCount={}",
                resolvedBucketName, normalizedDirectoryKey, result.deletedCount());

        return result.deletedCount();
    }

    /**
     * 查询默认存储桶下目录当前层级对象列表
     *
     * @param directoryKey 目录 Key
     * @return 对象信息列表
     */
    @Override
    public List<S3ObjectInfo> listDirectory(String directoryKey) {
        return listDirectory(getDefaultBucketName(), directoryKey);
    }

    /**
     * 查询指定存储桶下目录当前层级对象列表
     *
     * @param bucketName   存储桶名称
     * @param directoryKey 目录 Key
     * @return 对象信息列表
     */
    @Override
    public List<S3ObjectInfo> listDirectory(String bucketName, String directoryKey) {
        String resolvedBucketName = resolveBucketName(bucketName);
        String normalizedDirectoryKey = normalizeDirectoryKey(directoryKey);

        S3ListRequest request = new S3ListRequest(
                resolvedBucketName,
                normalizedDirectoryKey,
                "/",
                1000,
                null,
                false
        );

        List<S3ObjectInfo> objects = new ArrayList<>();
        String continuationToken = null;

        do {
            S3ObjectPage page = listObjectsPage(new S3ListRequest(
                    request.bucketName(),
                    request.prefix(),
                    request.delimiter(),
                    request.maxKeys(),
                    continuationToken,
                    request.recursive()
            ));

            objects.addAll(page.objects()
                    .stream()
                    .filter(object -> !StrUtil.equals(object.objectKey(), normalizedDirectoryKey))
                    .toList());

            continuationToken = page.nextContinuationToken();
        } while (StrUtil.isNotBlank(continuationToken));

        return objects;
    }

    // ==================== 预签名 URL ====================

    /**
     * 生成默认存储桶下对象的下载预签名 URL
     *
     * @param objectKey 对象 Key
     * @return 下载预签名 URL
     */
    @Override
    public URI generateDownloadUrl(String objectKey) {
        return generateDownloadUrl(getDefaultBucketName(), objectKey);
    }

    /**
     * 生成指定存储桶下对象的下载预签名 URL
     *
     * @param bucketName 存储桶名称
     * @param objectKey  对象 Key
     * @return 下载预签名 URL
     */
    @Override
    public URI generateDownloadUrl(String bucketName, String objectKey) {
        return generateDownloadUrl(bucketName, objectKey, null);
    }

    /**
     * 生成默认存储桶下对象的下载预签名 URL
     *
     * @param objectKey 对象 Key
     * @param expire    有效期
     * @return 下载预签名 URL
     */
    @Override
    public URI generateDownloadUrl(String objectKey, Duration expire) {
        return generateDownloadUrl(getDefaultBucketName(), objectKey, expire);
    }

    /**
     * 生成指定存储桶下对象的下载预签名 URL
     *
     * @param bucketName 存储桶名称
     * @param objectKey  对象 Key
     * @param expire     有效期
     * @return 下载预签名 URL
     */
    @Override
    public URI generateDownloadUrl(String bucketName, String objectKey, Duration expire) {
        S3PresignedUrlRequest request = new S3PresignedUrlRequest(
                bucketName,
                objectKey,
                S3PresignedUrlMethod.GET,
                expire,
                null,
                null,
                Map.of(),
                Map.of()
        );

        return generatePresignedUrl(request).url();
    }

    /**
     * 生成默认存储桶下对象的上传预签名 URL
     *
     * @param objectKey 对象 Key
     * @param expire    有效期
     * @return 上传预签名 URL
     */
    @Override
    public URI generateUploadUrl(String objectKey, Duration expire) {
        return generateUploadUrl(getDefaultBucketName(), objectKey, expire);
    }

    /**
     * 生成指定存储桶下对象的上传预签名 URL
     *
     * @param bucketName 存储桶名称
     * @param objectKey  对象 Key
     * @param expire     有效期
     * @return 上传预签名 URL
     */
    @Override
    public URI generateUploadUrl(String bucketName, String objectKey, Duration expire) {
        S3PresignedUrlRequest request = new S3PresignedUrlRequest(
                bucketName,
                objectKey,
                S3PresignedUrlMethod.PUT,
                expire,
                null,
                null,
                Map.of(),
                Map.of()
        );

        return generatePresignedUrl(request).url();
    }

    /**
     * 生成默认存储桶下对象的 HEAD 预签名 URL
     *
     * @param objectKey 对象 Key
     * @param expire    有效期
     * @return HEAD 预签名 URL
     */
    @Override
    public URI generateHeadUrl(String objectKey, Duration expire) {
        return generateHeadUrl(getDefaultBucketName(), objectKey, expire);
    }

    /**
     * 生成指定存储桶下对象的 HEAD 预签名 URL
     *
     * @param bucketName 存储桶名称
     * @param objectKey  对象 Key
     * @param expire     有效期
     * @return HEAD 预签名 URL
     */
    @Override
    public URI generateHeadUrl(String bucketName, String objectKey, Duration expire) {
        S3PresignedUrlRequest request = new S3PresignedUrlRequest(
                bucketName,
                objectKey,
                S3PresignedUrlMethod.HEAD,
                expire,
                null,
                null,
                Map.of(),
                Map.of()
        );

        return generatePresignedUrl(request).url();
    }

    /**
     * 生成默认存储桶下对象的删除预签名 URL
     *
     * @param objectKey 对象 Key
     * @param expire    有效期
     * @return 删除预签名 URL
     */
    @Override
    public URI generateDeleteUrl(String objectKey, Duration expire) {
        return generateDeleteUrl(getDefaultBucketName(), objectKey, expire);
    }

    /**
     * 生成指定存储桶下对象的删除预签名 URL
     *
     * @param bucketName 存储桶名称
     * @param objectKey  对象 Key
     * @param expire     有效期
     * @return 删除预签名 URL
     */
    @Override
    public URI generateDeleteUrl(String bucketName, String objectKey, Duration expire) {
        S3PresignedUrlRequest request = new S3PresignedUrlRequest(
                bucketName,
                objectKey,
                S3PresignedUrlMethod.DELETE,
                expire,
                null,
                null,
                Map.of(),
                Map.of()
        );

        return generatePresignedUrl(request).url();
    }

    /**
     * 生成预签名 URL
     *
     * @param request 预签名 URL 请求
     * @return 预签名 URL 结果
     */
    @Override
    public S3PresignedUrlResult generatePresignedUrl(S3PresignedUrlRequest request) {
        Assert.notNull(request, "S3 预签名 URL 请求不能为空");

        String resolvedBucketName = resolveBucketName(request.bucketName());
        String normalizedObjectKey = normalizeObjectKey(request.objectKey());
        S3PresignedUrlMethod method = request.method() == null ? S3PresignedUrlMethod.GET : request.method();
        Duration signatureDuration = normalizePresignExpire(request.expire());

        URI url = switch (method) {
            case GET -> presignGetObject(request, resolvedBucketName, normalizedObjectKey, signatureDuration);
            case PUT -> presignPutObject(request, resolvedBucketName, normalizedObjectKey, signatureDuration);
            case HEAD -> presignHeadObject(request, resolvedBucketName, normalizedObjectKey, signatureDuration);
            case DELETE -> presignDeleteObject(request, resolvedBucketName, normalizedObjectKey, signatureDuration);
        };

        Instant expireTime = Instant.now().plus(signatureDuration);

        log.info("生成 S3 预签名 URL 成功，bucketName={}，objectKey={}，method={}，expireTime={}",
                resolvedBucketName, normalizedObjectKey, method, DateUtil.date(expireTime.toEpochMilli()));

        return new S3PresignedUrlResult(
                resolvedBucketName,
                normalizedObjectKey,
                method,
                url,
                expireTime
        );
    }

    // ==================== 元数据管理 ====================

    /**
     * 获取默认存储桶下对象的自定义元数据
     *
     * @param objectKey 对象 Key
     * @return 自定义元数据
     */
    @Override
    public Map<String, String> getMetadata(String objectKey) {
        return getMetadata(getDefaultBucketName(), objectKey);
    }

    /**
     * 获取指定存储桶下对象的自定义元数据
     *
     * @param bucketName 存储桶名称
     * @param objectKey  对象 Key
     * @return 自定义元数据
     */
    @Override
    public Map<String, String> getMetadata(String bucketName, String objectKey) {
        String resolvedBucketName = resolveBucketName(bucketName);
        String normalizedObjectKey = normalizeObjectKey(objectKey);

        try {
            HeadObjectRequest request = buildHeadObjectRequest(resolvedBucketName, normalizedObjectKey);
            HeadObjectResponse response = s3Client.headObject(request);

            return response.metadata() == null ? Map.of() : Map.copyOf(response.metadata());
        } catch (NoSuchKeyException e) {
            log.warn("获取 S3 对象元数据失败，对象不存在，bucketName={}，objectKey={}",
                    resolvedBucketName, normalizedObjectKey);
            throw e;
        } catch (NoSuchBucketException e) {
            log.warn("获取 S3 对象元数据失败，存储桶不存在，bucketName={}，objectKey={}",
                    resolvedBucketName, normalizedObjectKey);
            throw e;
        } catch (S3Exception e) {
            log.error("获取 S3 对象元数据失败，bucketName={}，objectKey={}，statusCode={}，errorCode={}，message={}",
                    resolvedBucketName, normalizedObjectKey, e.statusCode(), getErrorCode(e), e.getMessage(), e);
            throw e;
        }
    }

    /**
     * 替换默认存储桶下对象的自定义元数据
     *
     * @param objectKey 对象 Key
     * @param metadata  新元数据
     */
    @Override
    public void replaceMetadata(String objectKey, Map<String, String> metadata) {
        replaceMetadata(getDefaultBucketName(), objectKey, metadata);
    }

    /**
     * 替换指定存储桶下对象的自定义元数据
     *
     * @param bucketName 存储桶名称
     * @param objectKey  对象 Key
     * @param metadata   新元数据
     */
    @Override
    public void replaceMetadata(String bucketName, String objectKey, Map<String, String> metadata) {
        S3MetadataRequest request = new S3MetadataRequest(
                bucketName,
                objectKey,
                metadata,
                true,
                false,
                null
        );

        replaceMetadata(request);
    }

    /**
     * 根据请求替换对象自定义元数据
     *
     * @param request 元数据替换请求
     */
    @Override
    public void replaceMetadata(S3MetadataRequest request) {
        Assert.notNull(request, "S3 元数据替换请求不能为空");

        String resolvedBucketName = resolveBucketName(request.bucketName());
        String normalizedObjectKey = normalizeObjectKey(request.objectKey());
        Map<String, String> metadata = normalizeMetadata(request.metadata());
        boolean preserveAcl = BooleanUtil.isTrue(request.preserveAcl());

        GetObjectAclResponse aclResponse = null;
        if (preserveAcl) {
            aclResponse = getObjectAcl(resolvedBucketName, normalizedObjectKey);
        }

        try {
            CopyObjectRequest copyObjectRequest = buildReplaceMetadataCopyRequest(
                    resolvedBucketName,
                    normalizedObjectKey,
                    metadata,
                    request
            );

            CopyObjectResponse response = s3Client.copyObject(copyObjectRequest);

            if (preserveAcl && aclResponse != null) {
                putObjectAcl(resolvedBucketName, normalizedObjectKey, aclResponse);
            }

            log.info("替换 S3 对象元数据成功，bucketName={}，objectKey={}，metadataSize={}，versionId={}，eTag={}",
                    resolvedBucketName,
                    normalizedObjectKey,
                    metadata.size(),
                    response.versionId(),
                    response.copyObjectResult() == null ? null : response.copyObjectResult().eTag());
        } catch (NoSuchBucketException e) {
            log.warn("替换 S3 对象元数据失败，存储桶不存在，bucketName={}，objectKey={}",
                    resolvedBucketName, normalizedObjectKey);
            throw e;
        } catch (NoSuchKeyException e) {
            log.warn("替换 S3 对象元数据失败，对象不存在，bucketName={}，objectKey={}",
                    resolvedBucketName, normalizedObjectKey);
            throw e;
        } catch (S3Exception e) {
            log.error("替换 S3 对象元数据失败，bucketName={}，objectKey={}，statusCode={}，errorCode={}，message={}",
                    resolvedBucketName, normalizedObjectKey, e.statusCode(), getErrorCode(e), e.getMessage(), e);
            throw e;
        }
    }

    // ==================== 标签管理 ====================

    /**
     * 获取默认存储桶下对象标签
     *
     * @param objectKey 对象 Key
     * @return 标签 Map
     */
    @Override
    public Map<String, String> getTags(String objectKey) {
        return getTags(getDefaultBucketName(), objectKey);
    }

    /**
     * 获取指定存储桶下对象标签
     *
     * @param bucketName 存储桶名称
     * @param objectKey  对象 Key
     * @return 标签 Map
     */
    @Override
    public Map<String, String> getTags(String bucketName, String objectKey) {
        return getTags(bucketName, objectKey, null);
    }

    /**
     * 写入默认存储桶下对象标签
     *
     * @param objectKey 对象 Key
     * @param tags      标签 Map
     */
    @Override
    public void putTags(String objectKey, Map<String, String> tags) {
        putTags(getDefaultBucketName(), objectKey, tags);
    }

    /**
     * 写入指定存储桶下对象标签
     *
     * @param bucketName 存储桶名称
     * @param objectKey  对象 Key
     * @param tags       标签 Map
     */
    @Override
    public void putTags(String bucketName, String objectKey, Map<String, String> tags) {
        putTags(bucketName, objectKey, null, tags);
    }

    /**
     * 删除默认存储桶下对象标签
     *
     * @param objectKey 对象 Key
     */
    @Override
    public void deleteTags(String objectKey) {
        deleteTags(getDefaultBucketName(), objectKey);
    }

    /**
     * 删除指定存储桶下对象标签
     *
     * @param bucketName 存储桶名称
     * @param objectKey  对象 Key
     */
    @Override
    public void deleteTags(String bucketName, String objectKey) {
        deleteTags(bucketName, objectKey, null);
    }

    /**
     * 获取指定对象版本的标签
     *
     * @param bucketName 存储桶名称
     * @param objectKey  对象 Key
     * @param versionId  版本 ID
     * @return 标签 Map
     */
    @Override
    public Map<String, String> getTags(String bucketName, String objectKey, String versionId) {
        String resolvedBucketName = resolveBucketName(bucketName);
        String normalizedObjectKey = normalizeObjectKey(objectKey);
        String resolvedVersionId = StrUtil.trim(versionId);

        try {
            GetObjectTaggingRequest.Builder builder = GetObjectTaggingRequest.builder()
                    .bucket(resolvedBucketName)
                    .key(normalizedObjectKey);

            if (StrUtil.isNotBlank(resolvedVersionId)) {
                builder.versionId(resolvedVersionId);
            }

            GetObjectTaggingResponse response = s3Client.getObjectTagging(builder.build());
            return toTagMap(response.tagSet());
        } catch (NoSuchKeyException e) {
            log.warn("获取 S3 对象标签失败，对象不存在，bucketName={}，objectKey={}，versionId={}",
                    resolvedBucketName, normalizedObjectKey, StrUtil.blankToDefault(resolvedVersionId, "默认版本"));
            throw e;
        } catch (NoSuchBucketException e) {
            log.warn("获取 S3 对象标签失败，存储桶不存在，bucketName={}，objectKey={}，versionId={}",
                    resolvedBucketName, normalizedObjectKey, StrUtil.blankToDefault(resolvedVersionId, "默认版本"));
            throw e;
        } catch (S3Exception e) {
            log.error("获取 S3 对象标签失败，bucketName={}，objectKey={}，versionId={}，statusCode={}，errorCode={}，message={}",
                    resolvedBucketName,
                    normalizedObjectKey,
                    StrUtil.blankToDefault(resolvedVersionId, "默认版本"),
                    e.statusCode(),
                    getErrorCode(e),
                    e.getMessage(),
                    e);
            throw e;
        }
    }

    /**
     * 写入指定对象版本的标签
     *
     * @param bucketName 存储桶名称
     * @param objectKey  对象 Key
     * @param versionId  版本 ID
     * @param tags       标签 Map
     */
    @Override
    public void putTags(String bucketName, String objectKey, String versionId, Map<String, String> tags) {
        String resolvedBucketName = resolveBucketName(bucketName);
        String normalizedObjectKey = normalizeObjectKey(objectKey);
        String resolvedVersionId = StrUtil.trim(versionId);
        Map<String, String> normalizedTags = normalizeTags(tags);

        if (MapUtil.isEmpty(normalizedTags)) {
            deleteTags(resolvedBucketName, normalizedObjectKey, resolvedVersionId);
            return;
        }

        try {
            PutObjectTaggingRequest.Builder builder = PutObjectTaggingRequest.builder()
                    .bucket(resolvedBucketName)
                    .key(normalizedObjectKey)
                    .tagging(Tagging.builder()
                            .tagSet(toTagList(normalizedTags))
                            .build());

            if (StrUtil.isNotBlank(resolvedVersionId)) {
                builder.versionId(resolvedVersionId);
            }

            s3Client.putObjectTagging(builder.build());

            log.info("写入 S3 对象标签成功，bucketName={}，objectKey={}，versionId={}，tagSize={}",
                    resolvedBucketName,
                    normalizedObjectKey,
                    StrUtil.blankToDefault(resolvedVersionId, "默认版本"),
                    normalizedTags.size());
        } catch (NoSuchKeyException e) {
            log.warn("写入 S3 对象标签失败，对象不存在，bucketName={}，objectKey={}，versionId={}",
                    resolvedBucketName, normalizedObjectKey, StrUtil.blankToDefault(resolvedVersionId, "默认版本"));
            throw e;
        } catch (NoSuchBucketException e) {
            log.warn("写入 S3 对象标签失败，存储桶不存在，bucketName={}，objectKey={}，versionId={}",
                    resolvedBucketName, normalizedObjectKey, StrUtil.blankToDefault(resolvedVersionId, "默认版本"));
            throw e;
        } catch (S3Exception e) {
            log.error("写入 S3 对象标签失败，bucketName={}，objectKey={}，versionId={}，statusCode={}，errorCode={}，message={}",
                    resolvedBucketName,
                    normalizedObjectKey,
                    StrUtil.blankToDefault(resolvedVersionId, "默认版本"),
                    e.statusCode(),
                    getErrorCode(e),
                    e.getMessage(),
                    e);
            throw e;
        }
    }

    /**
     * 删除指定对象版本的标签
     *
     * @param bucketName 存储桶名称
     * @param objectKey  对象 Key
     * @param versionId  版本 ID
     */
    @Override
    public void deleteTags(String bucketName, String objectKey, String versionId) {
        String resolvedBucketName = resolveBucketName(bucketName);
        String normalizedObjectKey = normalizeObjectKey(objectKey);
        String resolvedVersionId = StrUtil.trim(versionId);

        try {
            DeleteObjectTaggingRequest.Builder builder = DeleteObjectTaggingRequest.builder()
                    .bucket(resolvedBucketName)
                    .key(normalizedObjectKey);

            if (StrUtil.isNotBlank(resolvedVersionId)) {
                builder.versionId(resolvedVersionId);
            }

            s3Client.deleteObjectTagging(builder.build());

            log.info("删除 S3 对象标签成功，bucketName={}，objectKey={}，versionId={}",
                    resolvedBucketName,
                    normalizedObjectKey,
                    StrUtil.blankToDefault(resolvedVersionId, "默认版本"));
        } catch (NoSuchKeyException e) {
            log.warn("删除 S3 对象标签失败，对象不存在，bucketName={}，objectKey={}，versionId={}",
                    resolvedBucketName, normalizedObjectKey, StrUtil.blankToDefault(resolvedVersionId, "默认版本"));
            throw e;
        } catch (NoSuchBucketException e) {
            log.warn("删除 S3 对象标签失败，存储桶不存在，bucketName={}，objectKey={}，versionId={}",
                    resolvedBucketName, normalizedObjectKey, StrUtil.blankToDefault(resolvedVersionId, "默认版本"));
            throw e;
        } catch (S3Exception e) {
            log.error("删除 S3 对象标签失败，bucketName={}，objectKey={}，versionId={}，statusCode={}，errorCode={}，message={}",
                    resolvedBucketName,
                    normalizedObjectKey,
                    StrUtil.blankToDefault(resolvedVersionId, "默认版本"),
                    e.statusCode(),
                    getErrorCode(e),
                    e.getMessage(),
                    e);
            throw e;
        }
    }

    // ==================== 访问控制与存储属性 ====================

    /**
     * 设置默认存储桶下对象 ACL
     *
     * @param objectKey 对象 Key
     * @param acl       对象 ACL
     */
    @Override
    public void setObjectAcl(String objectKey, S3ObjectAcl acl) {
        setObjectAcl(getDefaultBucketName(), objectKey, acl);
    }

    /**
     * 设置指定存储桶下对象 ACL
     *
     * @param bucketName 存储桶名称
     * @param objectKey  对象 Key
     * @param acl        对象 ACL
     */
    @Override
    public void setObjectAcl(String bucketName, String objectKey, S3ObjectAcl acl) {
        String resolvedBucketName = resolveBucketName(bucketName);
        String normalizedObjectKey = normalizeObjectKey(objectKey);

        Assert.notNull(acl, "S3 对象 ACL 不能为空");

        try {
            PutObjectAclRequest request = PutObjectAclRequest.builder()
                    .bucket(resolvedBucketName)
                    .key(normalizedObjectKey)
                    .acl(toAwsObjectCannedAcl(acl))
                    .build();

            s3Client.putObjectAcl(request);

            log.info("设置 S3 对象 ACL 成功，bucketName={}，objectKey={}，acl={}",
                    resolvedBucketName, normalizedObjectKey, acl);
        } catch (NoSuchKeyException e) {
            log.warn("设置 S3 对象 ACL 失败，对象不存在，bucketName={}，objectKey={}，acl={}",
                    resolvedBucketName, normalizedObjectKey, acl);
            throw e;
        } catch (NoSuchBucketException e) {
            log.warn("设置 S3 对象 ACL 失败，存储桶不存在，bucketName={}，objectKey={}，acl={}",
                    resolvedBucketName, normalizedObjectKey, acl);
            throw e;
        } catch (S3Exception e) {
            log.error("设置 S3 对象 ACL 失败，bucketName={}，objectKey={}，acl={}，statusCode={}，errorCode={}，message={}",
                    resolvedBucketName,
                    normalizedObjectKey,
                    acl,
                    e.statusCode(),
                    getErrorCode(e),
                    e.getMessage(),
                    e);
            throw e;
        }
    }

    /**
     * 修改默认存储桶下对象存储类型
     *
     * @param objectKey    对象 Key
     * @param storageClass 存储类型
     */
    @Override
    public void changeStorageClass(String objectKey, S3StorageClass storageClass) {
        changeStorageClass(getDefaultBucketName(), objectKey, storageClass);
    }

    /**
     * 修改指定存储桶下对象存储类型
     *
     * @param bucketName   存储桶名称
     * @param objectKey    对象 Key
     * @param storageClass 存储类型
     */
    @Override
    public void changeStorageClass(String bucketName, String objectKey, S3StorageClass storageClass) {
        String resolvedBucketName = resolveBucketName(bucketName);
        String normalizedObjectKey = normalizeObjectKey(objectKey);

        Assert.notNull(storageClass, "S3 存储类型不能为空");

        try {
            CopyObjectRequest request = buildChangeStorageClassRequest(
                    resolvedBucketName,
                    normalizedObjectKey,
                    storageClass
            );

            CopyObjectResponse response = s3Client.copyObject(request);

            log.info("修改 S3 对象存储类型成功，bucketName={}，objectKey={}，storageClass={}，versionId={}，eTag={}",
                    resolvedBucketName,
                    normalizedObjectKey,
                    storageClass,
                    response.versionId(),
                    response.copyObjectResult() == null ? null : response.copyObjectResult().eTag());
        } catch (NoSuchKeyException e) {
            log.warn("修改 S3 对象存储类型失败，对象不存在，bucketName={}，objectKey={}，storageClass={}",
                    resolvedBucketName, normalizedObjectKey, storageClass);
            throw e;
        } catch (NoSuchBucketException e) {
            log.warn("修改 S3 对象存储类型失败，存储桶不存在，bucketName={}，objectKey={}，storageClass={}",
                    resolvedBucketName, normalizedObjectKey, storageClass);
            throw e;
        } catch (S3Exception e) {
            log.error("修改 S3 对象存储类型失败，bucketName={}，objectKey={}，storageClass={}，statusCode={}，errorCode={}，message={}",
                    resolvedBucketName,
                    normalizedObjectKey,
                    storageClass,
                    e.statusCode(),
                    getErrorCode(e),
                    e.getMessage(),
                    e);
            throw e;
        }
    }

    /**
     * 恢复默认存储桶下归档对象
     *
     * @param objectKey 对象 Key
     * @param days      恢复副本保留天数
     */
    @Override
    public void restoreArchiveObject(String objectKey, Integer days) {
        restoreArchiveObject(getDefaultBucketName(), objectKey, days);
    }

    /**
     * 恢复指定存储桶下归档对象
     *
     * @param bucketName 存储桶名称
     * @param objectKey  对象 Key
     * @param days       恢复副本保留天数
     */
    @Override
    public void restoreArchiveObject(String bucketName, String objectKey, Integer days) {
        String resolvedBucketName = resolveBucketName(bucketName);
        String normalizedObjectKey = normalizeObjectKey(objectKey);
        Integer resolvedDays = normalizeRestoreDays(days);

        try {
            RestoreObjectRequest request = RestoreObjectRequest.builder()
                    .bucket(resolvedBucketName)
                    .key(normalizedObjectKey)
                    .restoreRequest(RestoreRequest.builder()
                            .days(resolvedDays)
                            .glacierJobParameters(GlacierJobParameters.builder()
                                    .tier(Tier.STANDARD)
                                    .build())
                            .build())
                    .build();

            s3Client.restoreObject(request);

            log.info("发起 S3 归档对象恢复成功，bucketName={}，objectKey={}，days={}，tier={}",
                    resolvedBucketName, normalizedObjectKey, resolvedDays, Tier.STANDARD);
        } catch (NoSuchKeyException e) {
            log.warn("恢复 S3 归档对象失败，对象不存在，bucketName={}，objectKey={}，days={}",
                    resolvedBucketName, normalizedObjectKey, resolvedDays);
            throw e;
        } catch (NoSuchBucketException e) {
            log.warn("恢复 S3 归档对象失败，存储桶不存在，bucketName={}，objectKey={}，days={}",
                    resolvedBucketName, normalizedObjectKey, resolvedDays);
            throw e;
        } catch (S3Exception e) {
            if (StrUtil.equalsAnyIgnoreCase(getErrorCode(e), "RestoreAlreadyInProgress")) {
                log.info("S3 归档对象恢复任务已在进行中，bucketName={}，objectKey={}，days={}",
                        resolvedBucketName, normalizedObjectKey, resolvedDays);
                return;
            }

            if (StrUtil.equalsAnyIgnoreCase(getErrorCode(e), "ObjectAlreadyInActiveTierError")) {
                log.info("S3 对象已处于可访问存储层，无需恢复，bucketName={}，objectKey={}",
                        resolvedBucketName, normalizedObjectKey);
                return;
            }

            log.error("恢复 S3 归档对象失败，bucketName={}，objectKey={}，days={}，statusCode={}，errorCode={}，message={}",
                    resolvedBucketName,
                    normalizedObjectKey,
                    resolvedDays,
                    e.statusCode(),
                    getErrorCode(e),
                    e.getMessage(),
                    e);
            throw e;
        }
    }

    // ==================== 分片上传 ====================

    /**
     * 初始化分片上传
     *
     * @param request 分片上传初始化请求
     * @return 分片上传初始化结果
     */
    @Override
    public S3MultipartUploadInitResult initMultipartUpload(S3MultipartUploadInitRequest request) {
        Assert.notNull(request, "S3 分片上传初始化请求不能为空");

        String resolvedBucketName = resolveBucketName(request.bucketName());
        String normalizedObjectKey = normalizeObjectKey(request.objectKey());

        try {
            CreateMultipartUploadRequest createRequest = buildCreateMultipartUploadRequest(
                    request,
                    resolvedBucketName,
                    normalizedObjectKey
            );

            CreateMultipartUploadResponse response = s3Client.createMultipartUpload(createRequest);

            log.info("初始化 S3 分片上传成功，bucketName={}，objectKey={}，uploadId={}",
                    resolvedBucketName, normalizedObjectKey, response.uploadId());

            return new S3MultipartUploadInitResult(
                    resolvedBucketName,
                    normalizedObjectKey,
                    response.uploadId()
            );
        } catch (NoSuchBucketException e) {
            log.warn("初始化 S3 分片上传失败，存储桶不存在，bucketName={}，objectKey={}",
                    resolvedBucketName, normalizedObjectKey);
            throw e;
        } catch (S3Exception e) {
            log.error("初始化 S3 分片上传失败，bucketName={}，objectKey={}，statusCode={}，errorCode={}，message={}",
                    resolvedBucketName, normalizedObjectKey, e.statusCode(), getErrorCode(e), e.getMessage(), e);
            throw e;
        }
    }

    /**
     * 上传分片
     *
     * @param request 分片上传请求
     * @return 分片上传结果
     */
    @Override
    public S3MultipartUploadPartResult uploadPart(S3MultipartUploadPartRequest request) {
        Assert.notNull(request, "S3 分片上传请求不能为空");

        String resolvedBucketName = resolveBucketName(request.bucketName());
        String normalizedObjectKey = normalizeObjectKey(request.objectKey());
        String resolvedUploadId = normalizeUploadId(request.uploadId());
        Integer resolvedPartNumber = normalizePartNumber(request.partNumber());

        Assert.notNull(request.inputStream(), "S3 分片上传输入流不能为空");
        Assert.notNull(request.contentLength(), "S3 分片上传内容长度不能为空");
        Assert.isTrue(request.contentLength() > 0, "S3 分片上传内容长度必须大于 0");

        try {
            UploadPartRequest uploadPartRequest = UploadPartRequest.builder()
                    .bucket(resolvedBucketName)
                    .key(normalizedObjectKey)
                    .uploadId(resolvedUploadId)
                    .partNumber(resolvedPartNumber)
                    .contentLength(request.contentLength())
                    .build();

            UploadPartResponse response = s3Client.uploadPart(
                    uploadPartRequest,
                    RequestBody.fromInputStream(request.inputStream(), request.contentLength())
            );

            log.info("上传 S3 分片成功，bucketName={}，objectKey={}，uploadId={}，partNumber={}，size={}，eTag={}",
                    resolvedBucketName,
                    normalizedObjectKey,
                    resolvedUploadId,
                    resolvedPartNumber,
                    request.contentLength(),
                    response.eTag());

            return new S3MultipartUploadPartResult(
                    resolvedPartNumber,
                    response.eTag(),
                    request.contentLength()
            );
        } catch (NoSuchBucketException e) {
            log.warn("上传 S3 分片失败，存储桶不存在，bucketName={}，objectKey={}，uploadId={}，partNumber={}",
                    resolvedBucketName, normalizedObjectKey, resolvedUploadId, resolvedPartNumber);
            throw e;
        } catch (S3Exception e) {
            log.error("上传 S3 分片失败，bucketName={}，objectKey={}，uploadId={}，partNumber={}，statusCode={}，errorCode={}，message={}",
                    resolvedBucketName,
                    normalizedObjectKey,
                    resolvedUploadId,
                    resolvedPartNumber,
                    e.statusCode(),
                    getErrorCode(e),
                    e.getMessage(),
                    e);
            throw e;
        }
    }

    /**
     * 完成分片上传
     *
     * @param request 分片上传完成请求
     * @return 上传结果
     */
    @Override
    public S3UploadResult completeMultipartUpload(S3MultipartUploadCompleteRequest request) {
        Assert.notNull(request, "S3 完成分片上传请求不能为空");

        String resolvedBucketName = resolveBucketName(request.bucketName());
        String normalizedObjectKey = normalizeObjectKey(request.objectKey());
        String resolvedUploadId = normalizeUploadId(request.uploadId());
        List<S3MultipartUploadPartResult> normalizedParts = normalizeMultipartUploadParts(request.parts());

        try {
            CompleteMultipartUploadRequest completeRequest = CompleteMultipartUploadRequest.builder()
                    .bucket(resolvedBucketName)
                    .key(normalizedObjectKey)
                    .uploadId(resolvedUploadId)
                    .multipartUpload(CompletedMultipartUpload.builder()
                            .parts(toCompletedParts(normalizedParts))
                            .build())
                    .build();

            CompleteMultipartUploadResponse response = s3Client.completeMultipartUpload(completeRequest);

            S3ObjectInfo objectInfo = tryGetObjectInfoAfterComplete(resolvedBucketName, normalizedObjectKey);
            Long totalSize = objectInfo == null ? sumMultipartUploadPartSize(normalizedParts) : objectInfo.size();
            String contentType = objectInfo == null ? null : objectInfo.contentType();

            S3UploadResult result = new S3UploadResult(
                    resolvedBucketName,
                    normalizedObjectKey,
                    getFilename(normalizedObjectKey),
                    contentType,
                    totalSize,
                    response.eTag(),
                    response.versionId(),
                    getPublicUrl(resolvedBucketName, normalizedObjectKey),
                    Instant.now()
            );

            log.info("完成 S3 分片上传成功，bucketName={}，objectKey={}，uploadId={}，partCount={}，size={}，eTag={}",
                    resolvedBucketName,
                    normalizedObjectKey,
                    resolvedUploadId,
                    normalizedParts.size(),
                    result.size(),
                    result.eTag());

            return result;
        } catch (NoSuchBucketException e) {
            log.warn("完成 S3 分片上传失败，存储桶不存在，bucketName={}，objectKey={}，uploadId={}",
                    resolvedBucketName, normalizedObjectKey, resolvedUploadId);
            throw e;
        } catch (NoSuchKeyException e) {
            log.warn("完成 S3 分片上传失败，对象不存在，bucketName={}，objectKey={}，uploadId={}",
                    resolvedBucketName, normalizedObjectKey, resolvedUploadId);
            throw e;
        } catch (S3Exception e) {
            log.error("完成 S3 分片上传失败，bucketName={}，objectKey={}，uploadId={}，statusCode={}，errorCode={}，message={}",
                    resolvedBucketName,
                    normalizedObjectKey,
                    resolvedUploadId,
                    e.statusCode(),
                    getErrorCode(e),
                    e.getMessage(),
                    e);
            throw e;
        }
    }

    /**
     * 终止分片上传
     *
     * @param bucketName 存储桶名称
     * @param objectKey  对象 Key
     * @param uploadId   上传 ID
     */
    @Override
    public void abortMultipartUpload(String bucketName, String objectKey, String uploadId) {
        String resolvedBucketName = resolveBucketName(bucketName);
        String normalizedObjectKey = normalizeObjectKey(objectKey);
        String resolvedUploadId = normalizeUploadId(uploadId);

        try {
            AbortMultipartUploadRequest request = AbortMultipartUploadRequest.builder()
                    .bucket(resolvedBucketName)
                    .key(normalizedObjectKey)
                    .uploadId(resolvedUploadId)
                    .build();

            s3Client.abortMultipartUpload(request);

            log.info("终止 S3 分片上传成功，bucketName={}，objectKey={}，uploadId={}",
                    resolvedBucketName, normalizedObjectKey, resolvedUploadId);
        } catch (NoSuchBucketException e) {
            log.warn("终止 S3 分片上传失败，存储桶不存在，bucketName={}，objectKey={}，uploadId={}",
                    resolvedBucketName, normalizedObjectKey, resolvedUploadId);
            throw e;
        } catch (S3Exception e) {
            if (isNotFound(e)) {
                log.info("终止 S3 分片上传时任务已不存在，bucketName={}，objectKey={}，uploadId={}，errorCode={}",
                        resolvedBucketName, normalizedObjectKey, resolvedUploadId, getErrorCode(e));
                return;
            }

            log.error("终止 S3 分片上传失败，bucketName={}，objectKey={}，uploadId={}，statusCode={}，errorCode={}，message={}",
                    resolvedBucketName,
                    normalizedObjectKey,
                    resolvedUploadId,
                    e.statusCode(),
                    getErrorCode(e),
                    e.getMessage(),
                    e);
            throw e;
        }
    }

    /**
     * 查询未完成的分片上传任务
     *
     * @param bucketName 存储桶名称
     * @param prefix     对象 Key 前缀
     * @return 分片上传任务列表
     */
    @Override
    public List<S3MultipartUploadInfo> listMultipartUploads(String bucketName, String prefix) {
        String resolvedBucketName = resolveBucketName(bucketName);
        String normalizedPrefix = normalizeObjectPrefix(prefix);

        List<S3MultipartUploadInfo> uploads = new ArrayList<>();
        String keyMarker = null;
        String uploadIdMarker = null;

        try {
            do {
                ListMultipartUploadsRequest.Builder builder = ListMultipartUploadsRequest.builder()
                        .bucket(resolvedBucketName)
                        .maxUploads(1000);

                if (StrUtil.isNotBlank(normalizedPrefix)) {
                    builder.prefix(normalizedPrefix);
                }

                if (StrUtil.isNotBlank(keyMarker)) {
                    builder.keyMarker(keyMarker);
                }

                if (StrUtil.isNotBlank(uploadIdMarker)) {
                    builder.uploadIdMarker(uploadIdMarker);
                }

                ListMultipartUploadsResponse response = s3Client.listMultipartUploads(builder.build());

                uploads.addAll(response.uploads()
                        .stream()
                        .map(upload -> buildS3MultipartUploadInfo(resolvedBucketName, upload))
                        .toList());

                keyMarker = response.nextKeyMarker();
                uploadIdMarker = response.nextUploadIdMarker();
            } while (StrUtil.isNotBlank(keyMarker));

            return uploads;
        } catch (NoSuchBucketException e) {
            log.warn("查询 S3 未完成分片上传任务失败，存储桶不存在，bucketName={}，prefix={}",
                    resolvedBucketName, normalizedPrefix);
            throw e;
        } catch (S3Exception e) {
            log.error("查询 S3 未完成分片上传任务失败，bucketName={}，prefix={}，statusCode={}，errorCode={}，message={}",
                    resolvedBucketName, normalizedPrefix, e.statusCode(), getErrorCode(e), e.getMessage(), e);
            throw e;
        }
    }

    /**
     * 查询指定分片上传任务已上传的分片列表
     *
     * @param bucketName 存储桶名称
     * @param objectKey  对象 Key
     * @param uploadId   上传 ID
     * @return 已上传分片列表
     */
    @Override
    public List<S3MultipartUploadPartInfo> listMultipartUploadParts(String bucketName, String objectKey, String uploadId) {
        String resolvedBucketName = resolveBucketName(bucketName);
        String normalizedObjectKey = normalizeObjectKey(objectKey);
        String resolvedUploadId = normalizeUploadId(uploadId);

        List<S3MultipartUploadPartInfo> parts = new ArrayList<>();
        Integer partNumberMarker = null;

        try {
            do {
                ListPartsRequest.Builder builder = ListPartsRequest.builder()
                        .bucket(resolvedBucketName)
                        .key(normalizedObjectKey)
                        .uploadId(resolvedUploadId)
                        .maxParts(1000);

                if (partNumberMarker != null) {
                    builder.partNumberMarker(partNumberMarker);
                }

                ListPartsResponse response = s3Client.listParts(builder.build());

                parts.addAll(response.parts()
                        .stream()
                        .map(this::buildS3MultipartUploadPartInfo)
                        .toList());

                partNumberMarker = response.nextPartNumberMarker();
            } while (partNumberMarker != null);

            return parts;
        } catch (NoSuchBucketException e) {
            log.warn("查询 S3 已上传分片列表失败，存储桶不存在，bucketName={}，objectKey={}，uploadId={}",
                    resolvedBucketName, normalizedObjectKey, resolvedUploadId);
            throw e;
        } catch (S3Exception e) {
            if (isNotFound(e)) {
                log.info("查询 S3 已上传分片列表时任务不存在，bucketName={}，objectKey={}，uploadId={}，errorCode={}",
                        resolvedBucketName, normalizedObjectKey, resolvedUploadId, getErrorCode(e));
                return List.of();
            }

            log.error("查询 S3 已上传分片列表失败，bucketName={}，objectKey={}，uploadId={}，statusCode={}，errorCode={}，message={}",
                    resolvedBucketName,
                    normalizedObjectKey,
                    resolvedUploadId,
                    e.statusCode(),
                    getErrorCode(e),
                    e.getMessage(),
                    e);
            throw e;
        }
    }

    // ==================== 私有辅助方法 ====================

    /**
     * 构建创建存储桶请求
     *
     * @param bucketName 存储桶名称
     * @return 创建存储桶请求
     */
    private CreateBucketRequest buildCreateBucketRequest(String bucketName) {
        CreateBucketRequest.Builder builder = CreateBucketRequest.builder()
                .bucket(bucketName);

        String region = StrUtil.trim(s3Properties.getRegion());

        if (StrUtil.isNotBlank(region) && !StrUtil.equalsIgnoreCase(REGION_US_EAST_1, region)) {
            CreateBucketConfiguration configuration = CreateBucketConfiguration.builder()
                    .locationConstraint(BucketLocationConstraint.fromValue(region))
                    .build();

            builder.createBucketConfiguration(configuration);
        }

        return builder.build();
    }

    /**
     * 判断是否为资源不存在异常
     *
     * @param e S3 异常
     * @return true 是，false 否
     */
    private boolean isNotFound(S3Exception e) {
        return e.statusCode() == 404
                || StrUtil.equalsAnyIgnoreCase(getErrorCode(e), "NoSuchBucket", "NotFound");
    }

    /**
     * 判断是否为无权限异常
     *
     * @param e S3 异常
     * @return true 是，false 否
     */
    private boolean isForbidden(S3Exception e) {
        return e.statusCode() == 403
                || StrUtil.equalsAnyIgnoreCase(getErrorCode(e), "AccessDenied", "Forbidden");
    }

    /**
     * 判断存储桶是否已归当前凭证所有
     *
     * @param e S3 异常
     * @return true 是，false 否
     */
    private boolean isBucketAlreadyOwnedByYou(S3Exception e) {
        return StrUtil.equalsAnyIgnoreCase(getErrorCode(e), "BucketAlreadyOwnedByYou");
    }

    /**
     * 判断存储桶是否已经存在
     *
     * @param e S3 异常
     * @return true 是，false 否
     */
    private boolean isBucketAlreadyExists(S3Exception e) {
        return e.statusCode() == 409
                || StrUtil.equalsAnyIgnoreCase(getErrorCode(e), "BucketAlreadyExists");
    }

    /**
     * 获取 S3 错误码
     *
     * @param e S3 异常
     * @return 错误码
     */
    private String getErrorCode(S3Exception e) {
        if (e == null || e.awsErrorDetails() == null) {
            return null;
        }
        return e.awsErrorDetails().errorCode();
    }

    /**
     * 构建标准 AWS S3 公开访问 URL
     *
     * @param bucketName       存储桶名称
     * @param encodedObjectKey 已编码对象 Key
     * @param pathStyleAccess  是否路径风格访问
     * @return 公开访问 URL
     */
    private String buildAwsDefaultPublicUrl(String bucketName, String encodedObjectKey, boolean pathStyleAccess) {
        String region = StrUtil.blankToDefault(StrUtil.trim(s3Properties.getRegion()), REGION_US_EAST_1);
        String encodedBucketName = encodePathSegment(bucketName);

        if (pathStyleAccess) {
            return "https://s3." + region + ".amazonaws.com/" + encodedBucketName + "/" + encodedObjectKey;
        }

        return "https://" + bucketName + ".s3." + region + ".amazonaws.com/" + encodedObjectKey;
    }

    /**
     * 构建虚拟主机风格公开访问 URL
     *
     * @param endpointUri      endpoint URI
     * @param bucketName       存储桶名称
     * @param encodedObjectKey 已编码对象 Key
     * @return 公开访问 URL
     */
    private String buildVirtualHostPublicUrl(URI endpointUri, String bucketName, String encodedObjectKey) {
        String scheme = endpointUri.getScheme();
        String host = endpointUri.getHost();
        int port = endpointUri.getPort();
        String rawPath = StrUtil.blankToDefault(endpointUri.getRawPath(), StrUtil.EMPTY);

        String authority = bucketName + "." + host;
        if (port > 0) {
            authority = authority + ":" + port;
        }

        String baseUrl = scheme + "://" + authority;
        if (StrUtil.isNotBlank(rawPath) && !StrUtil.equals(rawPath, "/")) {
            baseUrl = joinUrlPath(baseUrl, StrUtil.removePrefix(rawPath, "/"));
        }

        return joinUrlPath(baseUrl, encodedObjectKey);
    }

    /**
     * 规范化 endpoint
     *
     * @param endpoint endpoint
     * @return 规范化后的 endpoint
     */
    private String normalizeEndpoint(String endpoint) {
        String normalizedEndpoint = StrUtil.trim(endpoint);

        while (StrUtil.endWith(normalizedEndpoint, "/")) {
            normalizedEndpoint = StrUtil.removeSuffix(normalizedEndpoint, "/");
        }

        return normalizedEndpoint;
    }

    /**
     * 拼接 URL 路径
     *
     * @param baseUrl URL 基础地址
     * @param paths   路径片段
     * @return 拼接后的 URL
     */
    private String joinUrlPath(String baseUrl, String... paths) {
        String url = StrUtil.removeSuffix(baseUrl, "/");

        for (String path : paths) {
            if (StrUtil.isBlank(path)) {
                continue;
            }

            url = url + "/" + StrUtil.removePrefix(path, "/");
        }

        return url;
    }

    /**
     * 编码对象 Key，保留路径分隔符
     *
     * @param objectKey 对象 Key
     * @return 编码后的对象 Key
     */
    private String encodeObjectKey(String objectKey) {
        String[] parts = objectKey.split("/", -1);
        StringBuilder builder = new StringBuilder();

        for (int i = 0; i < parts.length; i++) {
            if (i > 0) {
                builder.append("/");
            }
            builder.append(encodePathSegment(parts[i]));
        }

        return builder.toString();
    }

    /**
     * 编码 URL 路径片段
     *
     * @param pathSegment 路径片段
     * @return 编码后的路径片段
     */
    private String encodePathSegment(String pathSegment) {
        if (StrUtil.isEmpty(pathSegment)) {
            return StrUtil.EMPTY;
        }

        return URLEncoder.encode(pathSegment, StandardCharsets.UTF_8)
                .replace("+", "%20")
                .replace("%7E", "~");
    }

    /**
     * 判断 endpoint 是否不适合虚拟主机风格访问
     *
     * @param endpointUri endpoint URI
     * @return true 不适合，false 适合
     */
    private boolean isEndpointNotSuitableForVirtualHost(URI endpointUri) {
        String host = endpointUri.getHost();

        if (StrUtil.isBlank(host)) {
            return true;
        }

        return StrUtil.equalsAnyIgnoreCase(host, "localhost")
                || host.matches("\\d{1,3}(\\.\\d{1,3}){3}")
                || StrUtil.contains(host, ":");
    }

    /**
     * 构建 HeadObject 请求
     *
     * @param bucketName 存储桶名称
     * @param objectKey  对象 Key
     * @return HeadObject 请求
     */
    private HeadObjectRequest buildHeadObjectRequest(String bucketName, String objectKey) {
        return HeadObjectRequest.builder()
                .bucket(bucketName)
                .key(objectKey)
                .build();
    }

    /**
     * 构建 S3 对象信息
     *
     * @param bucketName 存储桶名称
     * @param objectKey  对象 Key
     * @param response   HeadObject 响应
     * @return S3 对象信息
     */
    private S3ObjectInfo buildS3ObjectInfo(String bucketName, String objectKey, HeadObjectResponse response) {
        Map<String, String> metadata = response.metadata() == null
                ? Map.of()
                : Map.copyOf(response.metadata());

        return new S3ObjectInfo(
                bucketName,
                objectKey,
                getFilename(objectKey),
                response.contentType(),
                response.contentLength(),
                response.eTag(),
                response.versionId(),
                toS3StorageClass(response.storageClassAsString()),
                response.lastModified(),
                metadata,
                Map.of()
        );
    }

    /**
     * 转换 S3 存储类型
     *
     * @param storageClass 存储类型字符串
     * @return 存储类型枚举
     */
    private S3StorageClass toS3StorageClass(String storageClass) {
        if (StrUtil.isBlank(storageClass)) {
            return S3StorageClass.STANDARD;
        }

        try {
            return S3StorageClass.valueOf(storageClass);
        } catch (IllegalArgumentException e) {
            log.warn("发现未适配的 S3 存储类型，storageClass={}", storageClass);
            return null;
        }
    }

    /**
     * 执行对象上传
     *
     * @param request          上传请求
     * @param originalFilename 原始文件名
     * @param requestBody      请求体
     * @return 上传结果
     */
    private S3UploadResult uploadInternal(S3UploadRequest request, String originalFilename, RequestBody requestBody) {
        Assert.notNull(request, "S3 上传请求不能为空");
        Assert.notNull(requestBody, "S3 上传请求体不能为空");

        PutObjectRequest putObjectRequest = buildPutObjectRequest(request);
        String bucketName = putObjectRequest.bucket();
        String objectKey = putObjectRequest.key();
        String resolvedOriginalFilename = StrUtil.blankToDefault(StrUtil.trim(originalFilename), getFilename(objectKey));

        try {
            PutObjectResponse response = s3Client.putObject(putObjectRequest, requestBody);

            S3UploadResult result = new S3UploadResult(
                    bucketName,
                    objectKey,
                    resolvedOriginalFilename,
                    putObjectRequest.contentType(),
                    putObjectRequest.contentLength(),
                    response.eTag(),
                    response.versionId(),
                    getPublicUrl(bucketName, objectKey),
                    Instant.now()
            );

            log.info("上传 S3 对象成功，bucketName={}，objectKey={}，size={}，contentType={}",
                    bucketName, objectKey, result.size(), result.contentType());

            return result;
        } catch (S3Exception e) {
            log.error("上传 S3 对象失败，bucketName={}，objectKey={}，statusCode={}，errorCode={}，message={}",
                    bucketName, objectKey, e.statusCode(), getErrorCode(e), e.getMessage(), e);
            throw e;
        }
    }

    /**
     * 构建 PutObject 请求
     *
     * @param request 上传请求
     * @return PutObject 请求
     */
    private PutObjectRequest buildPutObjectRequest(S3UploadRequest request) {
        String bucketName = resolveBucketName(request.bucketName());
        String objectKey = normalizeObjectKey(request.objectKey());

        Assert.notNull(request.contentLength(), "S3 上传内容长度不能为空");
        Assert.isTrue(request.contentLength() >= 0, "S3 上传内容长度不能小于 0");

        PutObjectRequest.Builder builder = PutObjectRequest.builder()
                .bucket(bucketName)
                .key(objectKey)
                .contentLength(request.contentLength())
                .contentType(resolveContentType(request.contentType(), objectKey));

        Map<String, String> metadata = normalizeMetadata(request.metadata());
        if (MapUtil.isNotEmpty(metadata)) {
            builder.metadata(metadata);
        }

        String tagging = buildTagging(request.tags());
        if (StrUtil.isNotBlank(tagging)) {
            builder.tagging(tagging);
        }

        if (request.acl() != null) {
            builder.acl(toAwsObjectCannedAcl(request.acl()));
        }

        if (request.storageClass() != null) {
            builder.storageClass(toAwsStorageClass(request.storageClass()));
        }

        configureServerSideEncryption(builder, request.serverSideEncryption(), request.kmsKeyId());

        return builder.build();
    }

    /**
     * 生成自动上传对象 Key
     *
     * @param originalFilename 原始文件名
     * @return 对象 Key
     */
    private String buildAutoObjectKey(String originalFilename) {
        String filename = normalizeOriginalFilename(originalFilename);
        String extension = FileNameUtil.extName(filename);

        String generatedFilename = IdUtil.fastSimpleUUID();
        if (StrUtil.isNotBlank(extension)) {
            generatedFilename = generatedFilename + "." + extension;
        }

        String directory = DEFAULT_UPLOAD_DIRECTORY + "/" + LocalDate.now().format(DEFAULT_DATE_PATH_FORMATTER);
        return buildObjectKey(directory, generatedFilename);
    }

    /**
     * 规范化原始文件名
     *
     * @param originalFilename 原始文件名
     * @return 规范化后的文件名
     */
    private String normalizeOriginalFilename(String originalFilename) {
        String filename = StrUtil.trim(originalFilename);

        if (StrUtil.isBlank(filename)) {
            return "file";
        }

        filename = StrUtil.replace(filename, "\\", "/");
        filename = FileNameUtil.getName(filename);

        return StrUtil.blankToDefault(filename, "file");
    }

    /**
     * 解析 Content-Type
     *
     * @param contentType 显式 Content-Type
     * @param objectKey   对象 Key
     * @return Content-Type
     */
    private String resolveContentType(String contentType, String objectKey) {
        if (StrUtil.isNotBlank(contentType)) {
            return StrUtil.trim(contentType);
        }

        String guessedContentType = URLConnection.guessContentTypeFromName(objectKey);
        return StrUtil.blankToDefault(guessedContentType, DEFAULT_CONTENT_TYPE);
    }

    /**
     * 规范化对象元数据
     *
     * @param metadata 元数据
     * @return 规范化后的元数据
     */
    private Map<String, String> normalizeMetadata(Map<String, String> metadata) {
        if (MapUtil.isEmpty(metadata)) {
            return Map.of();
        }

        Map<String, String> normalizedMetadata = new LinkedHashMap<>();

        metadata.forEach((key, value) -> {
            if (StrUtil.isBlank(key) || value == null) {
                return;
            }
            normalizedMetadata.put(StrUtil.trim(key), value);
        });

        return normalizedMetadata;
    }

    /**
     * 构建 S3 标签字符串
     *
     * @param tags 标签
     * @return 标签字符串
     */
    private String buildTagging(Map<String, String> tags) {
        if (MapUtil.isEmpty(tags)) {
            return null;
        }

        Map<String, String> normalizedTags = new LinkedHashMap<>();

        tags.forEach((key, value) -> {
            if (StrUtil.isBlank(key) || value == null) {
                return;
            }
            normalizedTags.put(StrUtil.trim(key), value);
        });

        if (MapUtil.isEmpty(normalizedTags)) {
            return null;
        }

        Assert.isTrue(normalizedTags.size() <= 10, "S3 对象标签数量不能超过 10 个");

        List<String> tagParts = new ArrayList<>(normalizedTags.size());
        normalizedTags.forEach((key, value) -> tagParts.add(encodePathSegment(key) + "=" + encodePathSegment(value)));

        return String.join("&", tagParts);
    }

    /**
     * 配置服务端加密
     *
     * @param builder              PutObject 请求构建器
     * @param serverSideEncryption 服务端加密方式
     * @param kmsKeyId             KMS Key ID
     */
    private void configureServerSideEncryption(PutObjectRequest.Builder builder,
                                               S3ServerSideEncryption serverSideEncryption,
                                               String kmsKeyId) {
        if (serverSideEncryption == null && StrUtil.isBlank(kmsKeyId)) {
            return;
        }

        S3ServerSideEncryption resolvedEncryption = serverSideEncryption;
        if (resolvedEncryption == null) {
            resolvedEncryption = S3ServerSideEncryption.AWS_KMS;
        }

        builder.serverSideEncryption(toAwsServerSideEncryption(resolvedEncryption));

        if (resolvedEncryption == S3ServerSideEncryption.AWS_KMS) {
            Assert.notBlank(kmsKeyId, "S3 使用 AWS_KMS 服务端加密时 kmsKeyId 不能为空");
            builder.ssekmsKeyId(kmsKeyId);
        }
    }

    /**
     * 转换对象 ACL
     *
     * @param acl 对象 ACL
     * @return AWS SDK 对象 ACL
     */
    private ObjectCannedACL toAwsObjectCannedAcl(S3ObjectAcl acl) {
        return switch (acl) {
            case PRIVATE -> ObjectCannedACL.PRIVATE;
            case PUBLIC_READ -> ObjectCannedACL.PUBLIC_READ;
            case PUBLIC_READ_WRITE -> ObjectCannedACL.PUBLIC_READ_WRITE;
            case AUTHENTICATED_READ -> ObjectCannedACL.AUTHENTICATED_READ;
            case BUCKET_OWNER_READ -> ObjectCannedACL.BUCKET_OWNER_READ;
            case BUCKET_OWNER_FULL_CONTROL -> ObjectCannedACL.BUCKET_OWNER_FULL_CONTROL;
        };
    }

    /**
     * 转换存储类型
     *
     * @param storageClass 存储类型
     * @return AWS SDK 存储类型
     */
    private StorageClass toAwsStorageClass(S3StorageClass storageClass) {
        return switch (storageClass) {
            case STANDARD -> StorageClass.STANDARD;
            case INTELLIGENT_TIERING -> StorageClass.INTELLIGENT_TIERING;
            case STANDARD_IA -> StorageClass.STANDARD_IA;
            case ONEZONE_IA -> StorageClass.ONEZONE_IA;
            case GLACIER -> StorageClass.GLACIER;
            case DEEP_ARCHIVE -> StorageClass.DEEP_ARCHIVE;
            case REDUCED_REDUNDANCY -> StorageClass.REDUCED_REDUNDANCY;
        };
    }

    /**
     * 转换服务端加密方式
     *
     * @param serverSideEncryption 服务端加密方式
     * @return AWS SDK 服务端加密方式
     */
    private ServerSideEncryption toAwsServerSideEncryption(S3ServerSideEncryption serverSideEncryption) {
        return switch (serverSideEncryption) {
            case AES256 -> ServerSideEncryption.AES256;
            case AWS_KMS -> ServerSideEncryption.AWS_KMS;
        };
    }

    /**
     * 构建 GetObject 请求
     *
     * @param bucketName 存储桶名称
     * @param objectKey  对象 Key
     * @return GetObject 请求
     */
    private GetObjectRequest buildGetObjectRequest(String bucketName, String objectKey) {
        return GetObjectRequest.builder()
                .bucket(bucketName)
                .key(objectKey)
                .build();
    }

    /**
     * 构建 GetObject 请求
     *
     * @param request 下载请求
     * @return GetObject 请求
     */
    private GetObjectRequest buildGetObjectRequest(S3DownloadRequest request) {
        String bucketName = resolveBucketName(request.bucketName());
        String objectKey = normalizeObjectKey(request.objectKey());

        GetObjectRequest.Builder builder = GetObjectRequest.builder()
                .bucket(bucketName)
                .key(objectKey);

        if (StrUtil.isNotBlank(request.versionId())) {
            builder.versionId(request.versionId());
        }

        String range = getDownloadRange(request.rangeStart(), request.rangeEnd());
        if (StrUtil.isNotBlank(range)) {
            builder.range(range);
        }

        return builder.build();
    }

    /**
     * 构建 Range 下载头
     *
     * @param rangeStart 起始位置
     * @param rangeEnd   结束位置
     * @return Range 下载头
     */
    private String getDownloadRange(Long rangeStart, Long rangeEnd) {
        if (rangeStart == null && rangeEnd == null) {
            return null;
        }

        if (rangeStart != null) {
            Assert.isTrue(rangeStart >= 0, "S3 Range 下载起始位置不能小于 0");
        }

        if (rangeEnd != null) {
            Assert.isTrue(rangeEnd >= 0, "S3 Range 下载结束位置不能小于 0");
        }

        if (rangeStart != null && rangeEnd != null) {
            Assert.isTrue(rangeEnd >= rangeStart, "S3 Range 下载结束位置不能小于起始位置");
            return "bytes=" + rangeStart + "-" + rangeEnd;
        }

        if (rangeStart != null) {
            return "bytes=" + rangeStart + "-";
        }

        return "bytes=0-" + rangeEnd;
    }

    /**
     * 构建对象标识列表
     *
     * @param objectKeys 对象 Key 列表
     * @return 对象标识列表
     */
    private List<ObjectIdentifier> buildObjectIdentifiers(List<String> objectKeys) {
        Set<String> normalizedObjectKeys = new LinkedHashSet<>();

        for (String objectKey : objectKeys) {
            normalizedObjectKeys.add(normalizeObjectKey(objectKey));
        }

        return normalizedObjectKeys.stream()
                .map(objectKey -> ObjectIdentifier.builder()
                        .key(objectKey)
                        .build())
                .toList();
    }

    /**
     * 查询指定前缀下的对象标识列表
     *
     * @param bucketName 存储桶名称
     * @param prefix     对象 Key 前缀
     * @return 对象标识列表
     */
    private List<ObjectIdentifier> listObjectIdentifiersByPrefix(String bucketName, String prefix) {
        List<ObjectIdentifier> objectIdentifiers = new ArrayList<>();
        String continuationToken = null;

        try {
            do {
                ListObjectsV2Request.Builder builder = ListObjectsV2Request.builder()
                        .bucket(bucketName)
                        .prefix(prefix)
                        .maxKeys(1000);

                if (StrUtil.isNotBlank(continuationToken)) {
                    builder.continuationToken(continuationToken);
                }

                ListObjectsV2Response response = s3Client.listObjectsV2(builder.build());

                for (S3Object object : response.contents()) {
                    if (object == null || StrUtil.isBlank(object.key())) {
                        continue;
                    }

                    objectIdentifiers.add(ObjectIdentifier.builder()
                            .key(object.key())
                            .build());
                }

                continuationToken = response.nextContinuationToken();
            } while (StrUtil.isNotBlank(continuationToken));

            return objectIdentifiers;
        } catch (NoSuchBucketException e) {
            log.warn("按前缀查询 S3 对象失败，存储桶不存在，bucketName={}，prefix={}", bucketName, prefix);
            throw e;
        } catch (S3Exception e) {
            log.error("按前缀查询 S3 对象失败，bucketName={}，prefix={}，statusCode={}，errorCode={}，message={}",
                    bucketName, prefix, e.statusCode(), getErrorCode(e), e.getMessage(), e);
            throw e;
        }
    }

    /**
     * 删除对象标识列表
     *
     * @param bucketName        存储桶名称
     * @param objectIdentifiers 对象标识列表
     * @return 删除详细结果
     */
    private S3DeleteResult deleteObjectIdentifiers(String bucketName, List<ObjectIdentifier> objectIdentifiers) {
        if (CollUtil.isEmpty(objectIdentifiers)) {
            return emptyDeleteResult(bucketName);
        }

        List<String> deletedObjectKeys = new ArrayList<>();
        List<S3DeleteError> deleteErrors = new ArrayList<>();

        for (List<ObjectIdentifier> batch : partitionObjectIdentifiers(objectIdentifiers, 1000)) {
            S3DeleteResult batchResult = deleteObjectIdentifierBatch(bucketName, batch);
            deletedObjectKeys.addAll(batchResult.deletedObjectKeys());
            deleteErrors.addAll(batchResult.errors());
        }

        S3DeleteResult result = new S3DeleteResult(
                bucketName,
                List.copyOf(deletedObjectKeys),
                List.copyOf(deleteErrors),
                (long) deletedObjectKeys.size(),
                (long) deleteErrors.size()
        );

        log.info("批量删除 S3 对象完成，bucketName={}，deletedCount={}，errorCount={}",
                bucketName, result.deletedCount(), result.errorCount());

        return result;
    }

    /**
     * 删除单批对象标识列表
     *
     * @param bucketName        存储桶名称
     * @param objectIdentifiers 对象标识列表
     * @return 删除详细结果
     */
    private S3DeleteResult deleteObjectIdentifierBatch(String bucketName, List<ObjectIdentifier> objectIdentifiers) {
        if (CollUtil.isEmpty(objectIdentifiers)) {
            return emptyDeleteResult(bucketName);
        }

        try {
            DeleteObjectsRequest request = DeleteObjectsRequest.builder()
                    .bucket(bucketName)
                    .delete(Delete.builder()
                            .objects(objectIdentifiers)
                            .quiet(false)
                            .build())
                    .build();

            DeleteObjectsResponse response = s3Client.deleteObjects(request);

            List<String> deletedObjectKeys = response.deleted()
                    .stream()
                    .map(DeletedObject::key)
                    .filter(StrUtil::isNotBlank)
                    .toList();

            List<S3DeleteError> deleteErrors = response.errors()
                    .stream()
                    .map(this::toS3DeleteError)
                    .toList();

            return new S3DeleteResult(
                    bucketName,
                    deletedObjectKeys,
                    deleteErrors,
                    (long) deletedObjectKeys.size(),
                    (long) deleteErrors.size()
            );
        } catch (NoSuchBucketException e) {
            log.warn("批量删除 S3 对象失败，存储桶不存在，bucketName={}，count={}",
                    bucketName, objectIdentifiers.size());
            throw e;
        } catch (S3Exception e) {
            log.error("批量删除 S3 对象失败，bucketName={}，count={}，statusCode={}，errorCode={}，message={}",
                    bucketName, objectIdentifiers.size(), e.statusCode(), getErrorCode(e), e.getMessage(), e);
            throw e;
        }
    }

    /**
     * 分片对象标识列表
     *
     * @param objectIdentifiers 对象标识列表
     * @param batchSize         每批数量
     * @return 分片后的对象标识列表
     */
    private List<List<ObjectIdentifier>> partitionObjectIdentifiers(List<ObjectIdentifier> objectIdentifiers, int batchSize) {
        Assert.isTrue(batchSize > 0, "S3 批量删除分片大小必须大于 0");

        if (CollUtil.isEmpty(objectIdentifiers)) {
            return List.of();
        }

        List<List<ObjectIdentifier>> partitions = new ArrayList<>();

        for (int start = 0; start < objectIdentifiers.size(); start += batchSize) {
            int end = Math.min(start + batchSize, objectIdentifiers.size());
            partitions.add(objectIdentifiers.subList(start, end));
        }

        return partitions;
    }

    /**
     * 转换删除错误信息
     *
     * @param error S3 删除错误
     * @return 删除错误信息
     */
    private S3DeleteError toS3DeleteError(S3Error error) {
        return new S3DeleteError(
                error.key(),
                error.versionId(),
                error.code(),
                error.message()
        );
    }

    /**
     * 构建空删除结果
     *
     * @param bucketName 存储桶名称
     * @return 空删除结果
     */
    private S3DeleteResult emptyDeleteResult(String bucketName) {
        return new S3DeleteResult(
                bucketName,
                List.of(),
                List.of(),
                0L,
                0L
        );
    }

    /**
     * 校验删除结果是否存在失败项
     *
     * @param result 删除结果
     */
    private void assertNoDeleteErrors(S3DeleteResult result) {
        if (result == null || result.errorCount() == null || result.errorCount() <= 0) {
            return;
        }

        S3DeleteError firstError = result.errors().getFirst();

        String message = StrUtil.format(
                "S3 批量删除存在失败项，bucketName={}，deletedCount={}，errorCount={}，firstErrorKey={}，firstErrorCode={}，firstErrorMessage={}",
                result.bucketName(),
                result.deletedCount(),
                result.errorCount(),
                firstError.objectKey(),
                firstError.code(),
                firstError.message()
        );

        throw new IllegalStateException(message);
    }

    /**
     * 构建 CopyObject 请求
     *
     * @param request 复制请求
     * @return CopyObject 请求
     */
    private CopyObjectRequest buildCopyObjectRequest(S3CopyRequest request) {
        String sourceBucketName = resolveBucketName(request.sourceBucketName());
        String sourceKey = normalizeObjectKey(request.sourceKey());
        String targetBucketName = resolveBucketName(request.targetBucketName());
        String targetKey = normalizeObjectKey(request.targetKey());

        CopyObjectRequest.Builder builder = CopyObjectRequest.builder()
                .copySource(buildCopySource(sourceBucketName, sourceKey, request.sourceVersionId()))
                .sourceBucket(sourceBucketName)
                .sourceKey(sourceKey)
                .destinationBucket(targetBucketName)
                .destinationKey(targetKey)
                .bucket(targetBucketName)
                .key(targetKey);

        configureCopyMetadata(builder, request);
        configureCopyTags(builder, request);

        if (request.acl() != null) {
            builder.acl(toAwsObjectCannedAcl(request.acl()));
        }

        if (request.storageClass() != null) {
            builder.storageClass(toAwsStorageClass(request.storageClass()));
        }

        configureCopyServerSideEncryption(builder, request.serverSideEncryption(), request.kmsKeyId());

        return builder.build();
    }

    /**
     * 构建复制源路径
     *
     * @param bucketName 存储桶名称
     * @param objectKey  对象 Key
     * @param versionId  源对象版本 ID
     * @return 复制源路径
     */
    private String buildCopySource(String bucketName, String objectKey, String versionId) {
        String copySource = encodePathSegment(bucketName) + "/" + encodeObjectKey(objectKey);

        if (StrUtil.isNotBlank(versionId)) {
            copySource = copySource + "?versionId=" + encodePathSegment(versionId);
        }

        return copySource;
    }

    /**
     * 配置复制对象元数据
     *
     * @param builder CopyObject 请求构建器
     * @param request 复制请求
     */
    private void configureCopyMetadata(CopyObjectRequest.Builder builder, S3CopyRequest request) {
        boolean replaceMetadata = BooleanUtil.isTrue(request.replaceMetadata());

        if (!replaceMetadata) {
            builder.metadataDirective(MetadataDirective.COPY);
            return;
        }

        builder.metadataDirective(MetadataDirective.REPLACE);

        Map<String, String> metadata = normalizeMetadata(request.metadata());
        if (MapUtil.isNotEmpty(metadata)) {
            builder.metadata(metadata);
        }
    }

    /**
     * 配置复制对象标签
     *
     * @param builder CopyObject 请求构建器
     * @param request 复制请求
     */
    private void configureCopyTags(CopyObjectRequest.Builder builder, S3CopyRequest request) {
        boolean replaceTags = BooleanUtil.isTrue(request.replaceTags());

        if (!replaceTags) {
            builder.taggingDirective(TaggingDirective.COPY);
            return;
        }

        builder.taggingDirective(TaggingDirective.REPLACE);

        String tagging = buildTagging(request.tags());
        builder.tagging(StrUtil.blankToDefault(tagging, StrUtil.EMPTY));
    }

    /**
     * 配置复制对象服务端加密
     *
     * @param builder              CopyObject 请求构建器
     * @param serverSideEncryption 服务端加密方式
     * @param kmsKeyId             KMS Key ID
     */
    private void configureCopyServerSideEncryption(CopyObjectRequest.Builder builder,
                                                   S3ServerSideEncryption serverSideEncryption,
                                                   String kmsKeyId) {
        if (serverSideEncryption == null && StrUtil.isBlank(kmsKeyId)) {
            return;
        }

        S3ServerSideEncryption resolvedEncryption = serverSideEncryption;
        if (resolvedEncryption == null) {
            resolvedEncryption = S3ServerSideEncryption.AWS_KMS;
        }

        builder.serverSideEncryption(toAwsServerSideEncryption(resolvedEncryption));

        if (resolvedEncryption == S3ServerSideEncryption.AWS_KMS) {
            Assert.notBlank(kmsKeyId, "S3 使用 AWS_KMS 服务端加密时 kmsKeyId 不能为空");
            builder.ssekmsKeyId(kmsKeyId);
        }
    }

    /**
     * 判断是否为同一个对象
     *
     * @param sourceBucketName 源存储桶名称
     * @param sourceKey        源对象 Key
     * @param targetBucketName 目标存储桶名称
     * @param targetKey        目标对象 Key
     * @return true 是，false 否
     */
    private boolean isSameObject(String sourceBucketName, String sourceKey, String targetBucketName, String targetKey) {
        return StrUtil.equals(sourceBucketName, targetBucketName)
                && StrUtil.equals(sourceKey, targetKey);
    }

    /**
     * 构建 ListObjectsV2 请求
     *
     * @param bucketName        存储桶名称
     * @param prefix            对象 Key 前缀
     * @param delimiter         分隔符
     * @param maxKeys           最大返回数量
     * @param continuationToken 分页令牌
     * @param recursive         是否递归查询
     * @return ListObjectsV2 请求
     */
    private ListObjectsV2Request buildListObjectsV2Request(String bucketName,
                                                           String prefix,
                                                           String delimiter,
                                                           Integer maxKeys,
                                                           String continuationToken,
                                                           Boolean recursive) {
        ListObjectsV2Request.Builder builder = ListObjectsV2Request.builder()
                .bucket(bucketName)
                .maxKeys(normalizeMaxKeys(maxKeys));

        if (StrUtil.isNotBlank(prefix)) {
            builder.prefix(prefix);
        }

        if (!BooleanUtil.isTrue(recursive) && StrUtil.isNotBlank(delimiter)) {
            builder.delimiter(delimiter);
        }

        if (StrUtil.isNotBlank(continuationToken)) {
            builder.continuationToken(continuationToken);
        }

        return builder.build();
    }

    /**
     * 规范化对象 Key 前缀
     *
     * @param prefix 对象 Key 前缀
     * @return 规范化后的对象 Key 前缀
     */
    private String normalizeObjectPrefix(String prefix) {
        String normalizedPrefix = StrUtil.trim(prefix);

        if (StrUtil.isBlank(normalizedPrefix)) {
            return null;
        }

        normalizedPrefix = StrUtil.replace(normalizedPrefix, "\\", "/");
        normalizedPrefix = normalizedPrefix.replaceAll("/{2,}", "/");

        while (StrUtil.startWith(normalizedPrefix, "/")) {
            normalizedPrefix = StrUtil.removePrefix(normalizedPrefix, "/");
        }

        while (StrUtil.startWith(normalizedPrefix, "./")) {
            normalizedPrefix = StrUtil.removePrefix(normalizedPrefix, "./");
        }

        return StrUtil.blankToDefault(normalizedPrefix, null);
    }

    /**
     * 规范化分页最大返回数量
     *
     * @param maxKeys 最大返回数量
     * @return 规范化后的最大返回数量
     */
    private Integer normalizeMaxKeys(Integer maxKeys) {
        if (maxKeys == null) {
            return 1000;
        }

        if (maxKeys < 1) {
            return 1;
        }

        return Math.min(maxKeys, 1000);
    }

    /**
     * 根据 ListObjectsV2 对象构建对象信息
     *
     * @param bucketName 存储桶名称
     * @param object     S3 对象
     * @return 对象信息
     */
    private S3ObjectInfo buildS3ObjectInfo(String bucketName, S3Object object) {
        return new S3ObjectInfo(
                bucketName,
                object.key(),
                getFilename(object.key()),
                null,
                object.size(),
                object.eTag(),
                null,
                toS3StorageClass(object.storageClassAsString()),
                object.lastModified(),
                Map.of(),
                Map.of()
        );
    }

    /**
     * 根据对象版本构建对象版本信息
     *
     * @param bucketName    存储桶名称
     * @param objectVersion 对象版本
     * @return 对象版本信息
     */
    private S3ObjectVersionInfo buildS3ObjectVersionInfo(String bucketName, ObjectVersion objectVersion) {
        return new S3ObjectVersionInfo(
                bucketName,
                objectVersion.key(),
                objectVersion.versionId(),
                objectVersion.isLatest(),
                false,
                objectVersion.size(),
                objectVersion.eTag(),
                objectVersion.lastModified()
        );
    }

    /**
     * 根据删除标记构建对象版本信息
     *
     * @param bucketName   存储桶名称
     * @param deleteMarker 删除标记
     * @return 对象版本信息
     */
    private S3ObjectVersionInfo buildS3ObjectVersionInfo(String bucketName, DeleteMarkerEntry deleteMarker) {
        return new S3ObjectVersionInfo(
                bucketName,
                deleteMarker.key(),
                deleteMarker.versionId(),
                deleteMarker.isLatest(),
                true,
                null,
                null,
                deleteMarker.lastModified()
        );
    }

    /**
     * 规范化目录 Key
     *
     * @param directoryKey 目录 Key
     * @return 规范化后的目录 Key
     */
    private String normalizeDirectoryKey(String directoryKey) {
        String normalizedDirectoryKey = normalizeObjectKey(directoryKey);
        normalizedDirectoryKey = StrUtil.removeSuffix(normalizedDirectoryKey, "/");

        Assert.notBlank(normalizedDirectoryKey, "S3 目录 Key 不能为空");

        return normalizedDirectoryKey + "/";
    }

    /**
     * 生成 GET 对象预签名 URL
     *
     * @param request           预签名请求
     * @param bucketName        存储桶名称
     * @param objectKey         对象 Key
     * @param signatureDuration 签名有效期
     * @return 预签名 URL
     */
    private URI presignGetObject(S3PresignedUrlRequest request,
                                 String bucketName,
                                 String objectKey,
                                 Duration signatureDuration) {
        GetObjectRequest.Builder getObjectBuilder = GetObjectRequest.builder()
                .bucket(bucketName)
                .key(objectKey);

        configureGetObjectResponseHeaders(getObjectBuilder, request);
        configureRequestHeaders(getObjectBuilder, request.requestHeaders());

        GetObjectPresignRequest presignRequest = GetObjectPresignRequest.builder()
                .signatureDuration(signatureDuration)
                .getObjectRequest(getObjectBuilder.build())
                .build();

        PresignedGetObjectRequest presignedRequest = s3Presigner.presignGetObject(presignRequest);
        return URI.create(presignedRequest.url().toString());
    }

    /**
     * 生成 PUT 对象预签名 URL
     *
     * @param request           预签名请求
     * @param bucketName        存储桶名称
     * @param objectKey         对象 Key
     * @param signatureDuration 签名有效期
     * @return 预签名 URL
     */
    private URI presignPutObject(S3PresignedUrlRequest request,
                                 String bucketName,
                                 String objectKey,
                                 Duration signatureDuration) {
        PutObjectRequest.Builder putObjectBuilder = PutObjectRequest.builder()
                .bucket(bucketName)
                .key(objectKey)
                .contentType(resolveContentType(request.contentType(), objectKey));

        configureRequestHeaders(putObjectBuilder, request.requestHeaders());

        PutObjectPresignRequest presignRequest = PutObjectPresignRequest.builder()
                .signatureDuration(signatureDuration)
                .putObjectRequest(putObjectBuilder.build())
                .build();

        PresignedPutObjectRequest presignedRequest = s3Presigner.presignPutObject(presignRequest);
        return URI.create(presignedRequest.url().toString());
    }

    /**
     * 生成 HEAD 对象预签名 URL
     *
     * @param request           预签名请求
     * @param bucketName        存储桶名称
     * @param objectKey         对象 Key
     * @param signatureDuration 签名有效期
     * @return 预签名 URL
     */
    private URI presignHeadObject(S3PresignedUrlRequest request,
                                  String bucketName,
                                  String objectKey,
                                  Duration signatureDuration) {
        HeadObjectRequest.Builder headObjectBuilder = HeadObjectRequest.builder()
                .bucket(bucketName)
                .key(objectKey);

        configureRequestHeaders(headObjectBuilder, request.requestHeaders());

        HeadObjectPresignRequest presignRequest = HeadObjectPresignRequest.builder()
                .signatureDuration(signatureDuration)
                .headObjectRequest(headObjectBuilder.build())
                .build();

        PresignedHeadObjectRequest presignedRequest = s3Presigner.presignHeadObject(presignRequest);
        return URI.create(presignedRequest.url().toString());
    }

    /**
     * 生成 DELETE 对象预签名 URL
     *
     * @param request           预签名请求
     * @param bucketName        存储桶名称
     * @param objectKey         对象 Key
     * @param signatureDuration 签名有效期
     * @return 预签名 URL
     */
    private URI presignDeleteObject(S3PresignedUrlRequest request,
                                    String bucketName,
                                    String objectKey,
                                    Duration signatureDuration) {
        DeleteObjectRequest.Builder deleteObjectBuilder = DeleteObjectRequest.builder()
                .bucket(bucketName)
                .key(objectKey);

        configureRequestHeaders(deleteObjectBuilder, request.requestHeaders());

        DeleteObjectPresignRequest presignRequest = DeleteObjectPresignRequest.builder()
                .signatureDuration(signatureDuration)
                .deleteObjectRequest(deleteObjectBuilder.build())
                .build();

        PresignedDeleteObjectRequest presignedRequest = s3Presigner.presignDeleteObject(presignRequest);
        return URI.create(presignedRequest.url().toString());
    }

    /**
     * 规范化预签名 URL 有效期
     *
     * @param expire 有效期
     * @return 规范化后的有效期
     */
    private Duration normalizePresignExpire(Duration expire) {
        Duration defaultExpire = s3Properties.getPresign() == null
                ? Duration.ofMinutes(10)
                : s3Properties.getPresign().getDefaultExpire();

        Duration maxExpire = s3Properties.getPresign() == null
                ? AWS_S3_MAX_PRESIGN_EXPIRE
                : s3Properties.getPresign().getMaxExpire();

        Duration resolvedExpire = expire == null ? defaultExpire : expire;
        Duration resolvedMaxExpire = maxExpire == null ? AWS_S3_MAX_PRESIGN_EXPIRE : maxExpire;

        if (resolvedMaxExpire.compareTo(AWS_S3_MAX_PRESIGN_EXPIRE) > 0) {
            resolvedMaxExpire = AWS_S3_MAX_PRESIGN_EXPIRE;
        }

        if (resolvedExpire.compareTo(DEFAULT_MIN_PRESIGN_EXPIRE) < 0) {
            return DEFAULT_MIN_PRESIGN_EXPIRE;
        }

        if (resolvedExpire.compareTo(resolvedMaxExpire) > 0) {
            log.warn("S3 预签名 URL 有效期超过最大限制，expire={}，maxExpire={}，已自动裁剪",
                    resolvedExpire, resolvedMaxExpire);
            return resolvedMaxExpire;
        }

        return resolvedExpire;
    }

    /**
     * 配置 GET 对象响应头
     *
     * @param builder GetObject 请求构建器
     * @param request 预签名 URL 请求
     */
    private void configureGetObjectResponseHeaders(GetObjectRequest.Builder builder, S3PresignedUrlRequest request) {
        if (StrUtil.isNotBlank(request.contentType())) {
            builder.responseContentType(StrUtil.trim(request.contentType()));
        }

        if (StrUtil.isNotBlank(request.filename())) {
            builder.responseContentDisposition(buildContentDisposition(request.filename()));
        }

        Map<String, String> responseHeaders = request.responseHeaders();
        if (MapUtil.isEmpty(responseHeaders)) {
            return;
        }

        responseHeaders.forEach((name, value) -> {
            if (StrUtil.isBlank(name) || value == null) {
                return;
            }

            String normalizedName = StrUtil.trim(name).toLowerCase(Locale.ROOT);
            String normalizedValue = StrUtil.trim(value);

            switch (normalizedName) {
                case "response-content-type", "content-type" -> builder.responseContentType(normalizedValue);
                case "response-content-disposition", "content-disposition" ->
                        builder.responseContentDisposition(normalizedValue);
                case "response-cache-control", "cache-control" -> builder.responseCacheControl(normalizedValue);
                case "response-content-encoding", "content-encoding" ->
                        builder.responseContentEncoding(normalizedValue);
                case "response-content-language", "content-language" ->
                        builder.responseContentLanguage(normalizedValue);
                default -> log.warn("忽略不支持的 S3 GET 预签名响应头，name={}", name);
            }
        });
    }

    /**
     * 配置请求头
     *
     * @param builder        GetObject 请求构建器
     * @param requestHeaders 请求头
     */
    private void configureRequestHeaders(GetObjectRequest.Builder builder, Map<String, String> requestHeaders) {
        AwsRequestOverrideConfiguration overrideConfiguration = buildRequestOverrideConfiguration(requestHeaders);
        if (overrideConfiguration != null) {
            builder.overrideConfiguration(overrideConfiguration);
        }
    }

    /**
     * 配置请求头
     *
     * @param builder        PutObject 请求构建器
     * @param requestHeaders 请求头
     */
    private void configureRequestHeaders(PutObjectRequest.Builder builder, Map<String, String> requestHeaders) {
        AwsRequestOverrideConfiguration overrideConfiguration = buildRequestOverrideConfiguration(requestHeaders);
        if (overrideConfiguration != null) {
            builder.overrideConfiguration(overrideConfiguration);
        }
    }

    /**
     * 配置请求头
     *
     * @param builder        HeadObject 请求构建器
     * @param requestHeaders 请求头
     */
    private void configureRequestHeaders(HeadObjectRequest.Builder builder, Map<String, String> requestHeaders) {
        AwsRequestOverrideConfiguration overrideConfiguration = buildRequestOverrideConfiguration(requestHeaders);
        if (overrideConfiguration != null) {
            builder.overrideConfiguration(overrideConfiguration);
        }
    }

    /**
     * 配置请求头
     *
     * @param builder        DeleteObject 请求构建器
     * @param requestHeaders 请求头
     */
    private void configureRequestHeaders(DeleteObjectRequest.Builder builder, Map<String, String> requestHeaders) {
        AwsRequestOverrideConfiguration overrideConfiguration = buildRequestOverrideConfiguration(requestHeaders);
        if (overrideConfiguration != null) {
            builder.overrideConfiguration(overrideConfiguration);
        }
    }

    /**
     * 构建请求覆盖配置
     *
     * @param requestHeaders 请求头
     * @return 请求覆盖配置
     */
    private AwsRequestOverrideConfiguration buildRequestOverrideConfiguration(Map<String, String> requestHeaders) {
        if (MapUtil.isEmpty(requestHeaders)) {
            return null;
        }

        AwsRequestOverrideConfiguration.Builder builder = AwsRequestOverrideConfiguration.builder();

        requestHeaders.forEach((name, value) -> {
            if (StrUtil.isBlank(name) || value == null) {
                return;
            }

            builder.putHeader(StrUtil.trim(name), StrUtil.trim(value));
        });

        return builder.build();
    }

    /**
     * 构建 Content-Disposition
     *
     * @param filename 文件名
     * @return Content-Disposition
     */
    private String buildContentDisposition(String filename) {
        String normalizedFilename = normalizeOriginalFilename(filename);
        String escapedFilename = StrUtil.replace(normalizedFilename, "\"", "\\\"");
        String encodedFilename = encodePathSegment(normalizedFilename);

        return "attachment; filename=\"" + escapedFilename + "\"; filename*=UTF-8''" + encodedFilename;
    }

    /**
     * 构建替换元数据的 CopyObject 请求
     *
     * @param bucketName 存储桶名称
     * @param objectKey  对象 Key
     * @param metadata   新元数据
     * @param request    元数据替换请求
     * @return CopyObject 请求
     */
    private CopyObjectRequest buildReplaceMetadataCopyRequest(String bucketName,
                                                              String objectKey,
                                                              Map<String, String> metadata,
                                                              S3MetadataRequest request) {
        CopyObjectRequest.Builder builder = CopyObjectRequest.builder()
                .copySource(buildCopySource(bucketName, objectKey, null))
                .bucket(bucketName)
                .key(objectKey)
                .metadataDirective(MetadataDirective.REPLACE)
                .metadata(metadata);

        if (BooleanUtil.isTrue(request.preserveTags())) {
            builder.taggingDirective(TaggingDirective.COPY);
        } else {
            builder.taggingDirective(TaggingDirective.REPLACE)
                    .tagging(StrUtil.EMPTY);
        }

        if (request.storageClass() != null) {
            builder.storageClass(toAwsStorageClass(request.storageClass()));
        }

        return builder.build();
    }

    /**
     * 获取对象 ACL
     *
     * @param bucketName 存储桶名称
     * @param objectKey  对象 Key
     * @return 对象 ACL 响应
     */
    private GetObjectAclResponse getObjectAcl(String bucketName, String objectKey) {
        try {
            GetObjectAclRequest request = GetObjectAclRequest.builder()
                    .bucket(bucketName)
                    .key(objectKey)
                    .build();

            return s3Client.getObjectAcl(request);
        } catch (S3Exception e) {
            log.error("获取 S3 对象 ACL 失败，bucketName={}，objectKey={}，statusCode={}，errorCode={}，message={}",
                    bucketName, objectKey, e.statusCode(), getErrorCode(e), e.getMessage(), e);
            throw e;
        }
    }

    /**
     * 写回对象 ACL
     *
     * @param bucketName  存储桶名称
     * @param objectKey   对象 Key
     * @param aclResponse 原对象 ACL 响应
     */
    private void putObjectAcl(String bucketName, String objectKey, GetObjectAclResponse aclResponse) {
        try {
            AccessControlPolicy accessControlPolicy = AccessControlPolicy.builder()
                    .owner(aclResponse.owner())
                    .grants(aclResponse.grants())
                    .build();

            PutObjectAclRequest request = PutObjectAclRequest.builder()
                    .bucket(bucketName)
                    .key(objectKey)
                    .accessControlPolicy(accessControlPolicy)
                    .build();

            s3Client.putObjectAcl(request);

            log.info("恢复 S3 对象 ACL 成功，bucketName={}，objectKey={}", bucketName, objectKey);
        } catch (S3Exception e) {
            log.error("恢复 S3 对象 ACL 失败，bucketName={}，objectKey={}，statusCode={}，errorCode={}，message={}",
                    bucketName, objectKey, e.statusCode(), getErrorCode(e), e.getMessage(), e);
            throw e;
        }
    }

    /**
     * 规范化对象标签
     *
     * @param tags 标签 Map
     * @return 规范化后的标签 Map
     */
    private Map<String, String> normalizeTags(Map<String, String> tags) {
        if (MapUtil.isEmpty(tags)) {
            return Map.of();
        }

        Map<String, String> normalizedTags = new LinkedHashMap<>();

        tags.forEach((key, value) -> {
            if (StrUtil.isBlank(key) || value == null) {
                return;
            }

            normalizedTags.put(StrUtil.trim(key), StrUtil.trim(value));
        });

        Assert.isTrue(normalizedTags.size() <= 10, "S3 对象标签数量不能超过 10 个");

        return normalizedTags;
    }

    /**
     * 转换为 AWS SDK 标签列表
     *
     * @param tags 标签 Map
     * @return AWS SDK 标签列表
     */
    private List<Tag> toTagList(Map<String, String> tags) {
        if (MapUtil.isEmpty(tags)) {
            return List.of();
        }

        return tags.entrySet()
                .stream()
                .map(entry -> Tag.builder()
                        .key(entry.getKey())
                        .value(entry.getValue())
                        .build())
                .toList();
    }

    /**
     * 转换为标签 Map
     *
     * @param tags AWS SDK 标签列表
     * @return 标签 Map
     */
    private Map<String, String> toTagMap(List<Tag> tags) {
        if (CollUtil.isEmpty(tags)) {
            return Map.of();
        }

        Map<String, String> tagMap = new LinkedHashMap<>();

        for (Tag tag : tags) {
            if (tag == null || StrUtil.isBlank(tag.key())) {
                continue;
            }

            tagMap.put(tag.key(), StrUtil.blankToDefault(tag.value(), StrUtil.EMPTY));
        }

        return Map.copyOf(tagMap);
    }

    /**
     * 构建修改对象存储类型的 CopyObject 请求
     *
     * @param bucketName   存储桶名称
     * @param objectKey    对象 Key
     * @param storageClass 存储类型
     * @return CopyObject 请求
     */
    private CopyObjectRequest buildChangeStorageClassRequest(String bucketName,
                                                             String objectKey,
                                                             S3StorageClass storageClass) {
        return CopyObjectRequest.builder()
                .copySource(buildCopySource(bucketName, objectKey, null))
                .bucket(bucketName)
                .key(objectKey)
                .metadataDirective(MetadataDirective.COPY)
                .taggingDirective(TaggingDirective.COPY)
                .storageClass(toAwsStorageClass(storageClass))
                .build();
    }

    /**
     * 规范化归档恢复天数
     *
     * @param days 恢复副本保留天数
     * @return 规范化后的恢复副本保留天数
     */
    private Integer normalizeRestoreDays(Integer days) {
        Integer resolvedDays = days == null ? 1 : days;
        Assert.isTrue(resolvedDays >= 1, "S3 归档对象恢复天数不能小于 1");
        return resolvedDays;
    }

    /**
     * 构建初始化分片上传请求
     *
     * @param request    初始化请求
     * @param bucketName 存储桶名称
     * @param objectKey  对象 Key
     * @return 初始化分片上传请求
     */
    private CreateMultipartUploadRequest buildCreateMultipartUploadRequest(S3MultipartUploadInitRequest request,
                                                                           String bucketName,
                                                                           String objectKey) {
        CreateMultipartUploadRequest.Builder builder = CreateMultipartUploadRequest.builder()
                .bucket(bucketName)
                .key(objectKey)
                .contentType(resolveContentType(request.contentType(), objectKey));

        Map<String, String> metadata = normalizeMetadata(request.metadata());
        if (MapUtil.isNotEmpty(metadata)) {
            builder.metadata(metadata);
        }

        String tagging = buildTagging(request.tags());
        if (StrUtil.isNotBlank(tagging)) {
            builder.tagging(tagging);
        }

        if (request.acl() != null) {
            builder.acl(toAwsObjectCannedAcl(request.acl()));
        }

        if (request.storageClass() != null) {
            builder.storageClass(toAwsStorageClass(request.storageClass()));
        }

        configureMultipartServerSideEncryption(builder, request.serverSideEncryption(), request.kmsKeyId());

        return builder.build();
    }

    /**
     * 配置分片上传服务端加密
     *
     * @param builder              初始化分片上传请求构建器
     * @param serverSideEncryption 服务端加密方式
     * @param kmsKeyId             KMS Key ID
     */
    private void configureMultipartServerSideEncryption(CreateMultipartUploadRequest.Builder builder,
                                                        S3ServerSideEncryption serverSideEncryption,
                                                        String kmsKeyId) {
        if (serverSideEncryption == null && StrUtil.isBlank(kmsKeyId)) {
            return;
        }

        S3ServerSideEncryption resolvedEncryption = serverSideEncryption;
        if (resolvedEncryption == null) {
            resolvedEncryption = S3ServerSideEncryption.AWS_KMS;
        }

        builder.serverSideEncryption(toAwsServerSideEncryption(resolvedEncryption));

        if (resolvedEncryption == S3ServerSideEncryption.AWS_KMS) {
            Assert.notBlank(kmsKeyId, "S3 使用 AWS_KMS 服务端加密时 kmsKeyId 不能为空");
            builder.ssekmsKeyId(kmsKeyId);
        }
    }

    /**
     * 规范化上传 ID
     *
     * @param uploadId 上传 ID
     * @return 规范化后的上传 ID
     */
    private String normalizeUploadId(String uploadId) {
        String resolvedUploadId = StrUtil.trim(uploadId);
        Assert.notBlank(resolvedUploadId, "S3 分片上传 uploadId 不能为空");
        return resolvedUploadId;
    }

    /**
     * 规范化分片编号
     *
     * @param partNumber 分片编号
     * @return 规范化后的分片编号
     */
    private Integer normalizePartNumber(Integer partNumber) {
        Assert.notNull(partNumber, "S3 分片编号不能为空");
        Assert.isTrue(partNumber >= 1, "S3 分片编号不能小于 1");
        Assert.isTrue(partNumber <= 10000, "S3 分片编号不能大于 10000");
        return partNumber;
    }

    /**
     * 规范化完成分片上传的分片列表
     *
     * @param parts 分片上传结果列表
     * @return 规范化后的分片上传结果列表
     */
    private List<S3MultipartUploadPartResult> normalizeMultipartUploadParts(List<S3MultipartUploadPartResult> parts) {
        Assert.notEmpty(parts, "S3 完成分片上传时分片列表不能为空");

        return parts.stream()
                .peek(part -> {
                    Assert.notNull(part, "S3 分片信息不能为空");
                    normalizePartNumber(part.partNumber());
                    Assert.notBlank(part.eTag(), "S3 分片 eTag 不能为空，partNumber={}", part.partNumber());
                })
                .sorted(Comparator.comparing(S3MultipartUploadPartResult::partNumber))
                .toList();
    }

    /**
     * 转换为 AWS SDK 完成分片列表
     *
     * @param parts 分片上传结果列表
     * @return AWS SDK 完成分片列表
     */
    private List<CompletedPart> toCompletedParts(List<S3MultipartUploadPartResult> parts) {
        return parts.stream()
                .map(part -> CompletedPart.builder()
                        .partNumber(part.partNumber())
                        .eTag(part.eTag())
                        .build())
                .toList();
    }

    /**
     * 统计分片总大小
     *
     * @param parts 分片上传结果列表
     * @return 分片总大小
     */
    private Long sumMultipartUploadPartSize(List<S3MultipartUploadPartResult> parts) {
        if (CollUtil.isEmpty(parts)) {
            return 0L;
        }

        return parts.stream()
                .map(S3MultipartUploadPartResult::size)
                .filter(size -> size != null && size > 0)
                .reduce(0L, Long::sum);
    }

    /**
     * 完成分片上传后尝试读取对象信息
     *
     * @param bucketName 存储桶名称
     * @param objectKey  对象 Key
     * @return 对象信息，读取失败时返回 null
     */
    private S3ObjectInfo tryGetObjectInfoAfterComplete(String bucketName, String objectKey) {
        try {
            return getObjectInfo(bucketName, objectKey);
        } catch (RuntimeException e) {
            log.warn("完成 S3 分片上传后读取对象信息失败，将使用分片结果构建上传结果，bucketName={}，objectKey={}",
                    bucketName, objectKey, e);
            return null;
        }
    }

    /**
     * 构建分片上传任务信息
     *
     * @param bucketName 存储桶名称
     * @param upload     分片上传任务
     * @return 分片上传任务信息
     */
    private S3MultipartUploadInfo buildS3MultipartUploadInfo(String bucketName, MultipartUpload upload) {
        return new S3MultipartUploadInfo(
                bucketName,
                upload.key(),
                upload.uploadId(),
                upload.initiated()
        );
    }

    /**
     * 构建已上传分片信息
     *
     * @param part 已上传分片
     * @return 已上传分片信息
     */
    private S3MultipartUploadPartInfo buildS3MultipartUploadPartInfo(Part part) {
        return new S3MultipartUploadPartInfo(
                part.partNumber(),
                part.eTag(),
                part.size(),
                part.lastModified()
        );
    }
}