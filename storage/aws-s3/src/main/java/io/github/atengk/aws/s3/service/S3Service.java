package io.github.atengk.aws.s3.service;

import io.github.atengk.aws.s3.model.enums.S3ObjectAcl;
import io.github.atengk.aws.s3.model.enums.S3StorageClass;
import io.github.atengk.aws.s3.model.info.S3MultipartUploadInfo;
import io.github.atengk.aws.s3.model.info.S3MultipartUploadPartInfo;
import io.github.atengk.aws.s3.model.info.S3ObjectInfo;
import io.github.atengk.aws.s3.model.info.S3ObjectVersionInfo;
import io.github.atengk.aws.s3.model.request.*;
import io.github.atengk.aws.s3.model.result.*;
import org.springframework.core.io.Resource;
import org.springframework.web.multipart.MultipartFile;

import java.io.InputStream;
import java.net.URI;
import java.nio.file.Path;
import java.time.Duration;
import java.util.List;
import java.util.Map;

/**
 * S3 文件服务接口
 *
 * @author Ateng
 * @since 2026-04-28
 */
public interface S3Service {

    // ==================== 存储桶管理 ====================

    /**
     * 判断存储桶是否存在。
     *
     * @param bucketName 存储桶名称
     * @return true 存在，false 不存在
     */
    boolean bucketExists(String bucketName);

    /**
     * 创建存储桶。
     *
     * @param bucketName 存储桶名称
     */
    void createBucket(String bucketName);

    /**
     * 存储桶不存在时创建。
     *
     * @param bucketName 存储桶名称
     */
    void createBucketIfAbsent(String bucketName);

    /**
     * 删除存储桶。
     *
     * @param bucketName 存储桶名称
     */
    void deleteBucket(String bucketName);

    /**
     * 查询当前凭证可见的存储桶列表。
     *
     * @return 存储桶名称列表
     */
    List<String> listBuckets();

    /**
     * 获取默认存储桶名称。
     *
     * @return 默认存储桶名称
     */
    String getDefaultBucketName();

    /**
     * 解析存储桶名称，为空时返回默认存储桶。
     *
     * @param bucketName 存储桶名称
     * @return 解析后的存储桶名称
     */
    String resolveBucketName(String bucketName);

    // ==================== 对象基础操作 ====================

    /**
     * 判断默认存储桶下的对象是否存在。
     *
     * @param objectKey 对象 Key
     * @return true 存在，false 不存在
     */
    boolean objectExists(String objectKey);

    /**
     * 判断指定存储桶下的对象是否存在。
     *
     * @param bucketName 存储桶名称
     * @param objectKey  对象 Key
     * @return true 存在，false 不存在
     */
    boolean objectExists(String bucketName, String objectKey);

    /**
     * 获取默认存储桶下的对象信息。
     *
     * @param objectKey 对象 Key
     * @return 对象信息
     */
    S3ObjectInfo getObjectInfo(String objectKey);

    /**
     * 获取指定存储桶下的对象信息。
     *
     * @param bucketName 存储桶名称
     * @param objectKey  对象 Key
     * @return 对象信息
     */
    S3ObjectInfo getObjectInfo(String bucketName, String objectKey);

    /**
     * 获取默认存储桶下的对象大小。
     *
     * @param objectKey 对象 Key
     * @return 对象大小，单位字节
     */
    Long getObjectSize(String objectKey);

    /**
     * 获取指定存储桶下的对象大小。
     *
     * @param bucketName 存储桶名称
     * @param objectKey  对象 Key
     * @return 对象大小，单位字节
     */
    Long getObjectSize(String bucketName, String objectKey);

    /**
     * 获取默认存储桶下对象的 Content-Type。
     *
     * @param objectKey 对象 Key
     * @return Content-Type
     */
    String getObjectContentType(String objectKey);

    /**
     * 获取指定存储桶下对象的 Content-Type。
     *
     * @param bucketName 存储桶名称
     * @param objectKey  对象 Key
     * @return Content-Type
     */
    String getObjectContentType(String bucketName, String objectKey);

    // ==================== 对象上传 ====================

    /**
     * 上传文件到默认存储桶，并自动生成对象 Key。
     *
     * @param file 上传文件
     * @return 上传结果
     */
    S3UploadResult upload(MultipartFile file);

    /**
     * 上传文件到默认存储桶。
     *
     * @param objectKey 对象 Key
     * @param file      上传文件
     * @return 上传结果
     */
    S3UploadResult upload(String objectKey, MultipartFile file);

    /**
     * 上传文件到指定存储桶。
     *
     * @param bucketName 存储桶名称
     * @param objectKey  对象 Key
     * @param file       上传文件
     * @return 上传结果
     */
    S3UploadResult upload(String bucketName, String objectKey, MultipartFile file);

    /**
     * 上传字节数组到默认存储桶。
     *
     * @param bytes     字节数组
     * @param objectKey 对象 Key
     * @return 上传结果
     */
    S3UploadResult upload(byte[] bytes, String objectKey);

    /**
     * 上传字节数组到指定存储桶。
     *
     * @param bucketName 存储桶名称
     * @param objectKey  对象 Key
     * @param bytes      字节数组
     * @return 上传结果
     */
    S3UploadResult upload(String bucketName, String objectKey, byte[] bytes);

    /**
     * 上传输入流到默认存储桶。
     *
     * @param inputStream   输入流
     * @param objectKey     对象 Key
     * @param contentLength 内容长度
     * @return 上传结果
     */
    S3UploadResult upload(InputStream inputStream, String objectKey, long contentLength);

    /**
     * 上传输入流到指定存储桶。
     *
     * @param bucketName    存储桶名称
     * @param objectKey     对象 Key
     * @param inputStream   输入流
     * @param contentLength 内容长度
     * @return 上传结果
     */
    S3UploadResult upload(String bucketName, String objectKey, InputStream inputStream, long contentLength);

    /**
     * 上传本地文件到默认存储桶。
     *
     * @param filePath  本地文件路径
     * @param objectKey 对象 Key
     * @return 上传结果
     */
    S3UploadResult upload(Path filePath, String objectKey);

    /**
     * 上传本地文件到指定存储桶。
     *
     * @param bucketName 存储桶名称
     * @param objectKey  对象 Key
     * @param filePath   本地文件路径
     * @return 上传结果
     */
    S3UploadResult upload(String bucketName, String objectKey, Path filePath);

    /**
     * 根据上传请求上传对象。
     *
     * @param request 上传请求
     * @return 上传结果
     */
    S3UploadResult upload(S3UploadRequest request);

    /**
     * 批量上传对象。
     *
     * @param requests 上传请求列表
     * @return 上传结果列表
     */
    List<S3UploadResult> uploadBatch(List<S3UploadRequest> requests);

    // ==================== 对象下载 ====================

    /**
     * 下载默认存储桶下的对象为字节数组。
     *
     * @param objectKey 对象 Key
     * @return 字节数组
     */
    byte[] downloadAsBytes(String objectKey);

    /**
     * 下载指定存储桶下的对象为字节数组。
     *
     * @param bucketName 存储桶名称
     * @param objectKey  对象 Key
     * @return 字节数组
     */
    byte[] downloadAsBytes(String bucketName, String objectKey);

    /**
     * 下载默认存储桶下的对象为输入流。
     *
     * @param objectKey 对象 Key
     * @return 输入流，调用方负责关闭
     */
    InputStream downloadAsStream(String objectKey);

    /**
     * 下载指定存储桶下的对象为输入流。
     *
     * @param bucketName 存储桶名称
     * @param objectKey  对象 Key
     * @return 输入流，调用方负责关闭
     */
    InputStream downloadAsStream(String bucketName, String objectKey);

    /**
     * 下载默认存储桶下的对象为 Spring Resource。
     *
     * @param objectKey 对象 Key
     * @return Spring Resource
     */
    Resource downloadAsResource(String objectKey);

    /**
     * 下载指定存储桶下的对象为 Spring Resource。
     *
     * @param bucketName 存储桶名称
     * @param objectKey  对象 Key
     * @return Spring Resource
     */
    Resource downloadAsResource(String bucketName, String objectKey);

    /**
     * 下载默认存储桶下的对象到本地文件。
     *
     * @param objectKey  对象 Key
     * @param targetPath 本地目标路径
     */
    void downloadToFile(String objectKey, Path targetPath);

    /**
     * 下载指定存储桶下的对象到本地文件。
     *
     * @param bucketName 存储桶名称
     * @param objectKey  对象 Key
     * @param targetPath 本地目标路径
     */
    void downloadToFile(String bucketName, String objectKey, Path targetPath);

    /**
     * 根据下载请求下载对象。
     *
     * @param request 下载请求
     * @return 下载结果
     */
    S3DownloadResult download(S3DownloadRequest request);

    // ==================== 对象删除 ====================

    /**
     * 删除默认存储桶下的对象。
     *
     * @param objectKey 对象 Key
     */
    void delete(String objectKey);

    /**
     * 删除指定存储桶下的对象。
     *
     * @param bucketName 存储桶名称
     * @param objectKey  对象 Key
     */
    void delete(String bucketName, String objectKey);

    /**
     * 批量删除默认存储桶下的对象。
     *
     * @param objectKeys 对象 Key 列表
     */
    void deleteBatch(List<String> objectKeys);

    /**
     * 批量删除指定存储桶下的对象。
     *
     * @param bucketName 存储桶名称
     * @param objectKeys 对象 Key 列表
     */
    void deleteBatch(String bucketName, List<String> objectKeys);

    /**
     * 删除默认存储桶下指定前缀的对象。
     *
     * @param prefix 对象 Key 前缀
     * @return 删除成功数量
     */
    Long deleteByPrefix(String prefix);

    /**
     * 删除指定存储桶下指定前缀的对象。
     *
     * @param bucketName 存储桶名称
     * @param prefix     对象 Key 前缀
     * @return 删除成功数量
     */
    Long deleteByPrefix(String bucketName, String prefix);

    /**
     * 删除指定版本的对象。
     *
     * @param bucketName 存储桶名称
     * @param objectKey  对象 Key
     * @param versionId  版本 ID
     */
    void deleteVersion(String bucketName, String objectKey, String versionId);

    /**
     * 批量删除默认存储桶下的对象，并返回详细结果。
     *
     * @param objectKeys 对象 Key 列表
     * @return 删除详细结果
     */
    S3DeleteResult deleteBatchDetailed(List<String> objectKeys);

    /**
     * 批量删除指定存储桶下的对象，并返回详细结果。
     *
     * @param bucketName 存储桶名称
     * @param objectKeys 对象 Key 列表
     * @return 删除详细结果
     */
    S3DeleteResult deleteBatchDetailed(String bucketName, List<String> objectKeys);

    /**
     * 删除默认存储桶下指定前缀的对象，并返回详细结果。
     *
     * @param prefix 对象 Key 前缀
     * @return 删除详细结果
     */
    S3DeleteResult deleteByPrefixDetailed(String prefix);

    /**
     * 删除指定存储桶下指定前缀的对象，并返回详细结果。
     *
     * @param bucketName 存储桶名称
     * @param prefix     对象 Key 前缀
     * @return 删除详细结果
     */
    S3DeleteResult deleteByPrefixDetailed(String bucketName, String prefix);

    // ==================== 对象复制与移动 ====================

    /**
     * 在默认存储桶内复制对象。
     *
     * @param sourceKey 源对象 Key
     * @param targetKey 目标对象 Key
     * @return 复制结果
     */
    S3CopyResult copy(String sourceKey, String targetKey);

    /**
     * 复制对象到指定存储桶。
     *
     * @param sourceBucketName 源存储桶名称
     * @param sourceKey        源对象 Key
     * @param targetBucketName 目标存储桶名称
     * @param targetKey        目标对象 Key
     * @return 复制结果
     */
    S3CopyResult copy(String sourceBucketName, String sourceKey, String targetBucketName, String targetKey);

    /**
     * 根据复制请求复制对象。
     *
     * @param request 复制请求
     * @return 复制结果
     */
    S3CopyResult copy(S3CopyRequest request);

    /**
     * 在默认存储桶内移动对象。
     *
     * @param sourceKey 源对象 Key
     * @param targetKey 目标对象 Key
     * @return 移动结果
     */
    S3MoveResult move(String sourceKey, String targetKey);

    /**
     * 移动对象到指定存储桶。
     *
     * @param sourceBucketName 源存储桶名称
     * @param sourceKey        源对象 Key
     * @param targetBucketName 目标存储桶名称
     * @param targetKey        目标对象 Key
     * @return 移动结果
     */
    S3MoveResult move(String sourceBucketName, String sourceKey, String targetBucketName, String targetKey);

    /**
     * 根据移动请求移动对象。
     *
     * @param request 移动请求
     * @return 移动结果
     */
    S3MoveResult move(S3MoveRequest request);

    /**
     * 在默认存储桶内重命名对象。
     *
     * @param sourceKey 源对象 Key
     * @param targetKey 目标对象 Key
     */
    void rename(String sourceKey, String targetKey);

    /**
     * 在指定存储桶内重命名对象。
     *
     * @param bucketName 存储桶名称
     * @param sourceKey  源对象 Key
     * @param targetKey  目标对象 Key
     */
    void rename(String bucketName, String sourceKey, String targetKey);

    // ==================== 对象列表与分页查询 ====================

    /**
     * 查询默认存储桶根层级对象列表。
     *
     * @return 对象信息列表
     */
    List<S3ObjectInfo> listObjects();

    /**
     * 查询默认存储桶指定前缀当前层级对象列表。
     *
     * @param prefix 对象 Key 前缀
     * @return 对象信息列表
     */
    List<S3ObjectInfo> listObjects(String prefix);

    /**
     * 查询指定存储桶指定前缀当前层级对象列表。
     *
     * @param bucketName 存储桶名称
     * @param prefix     对象 Key 前缀
     * @return 对象信息列表
     */
    List<S3ObjectInfo> listObjects(String bucketName, String prefix);

    /**
     * 分页查询对象列表。
     *
     * @param request 列表查询请求
     * @return 对象分页结果
     */
    S3ObjectPage listObjectsPage(S3ListRequest request);

    /**
     * 递归查询默认存储桶指定前缀下的对象列表。
     *
     * @param prefix 对象 Key 前缀
     * @return 对象信息列表
     */
    List<S3ObjectInfo> listObjectsRecursive(String prefix);

    /**
     * 递归查询指定存储桶指定前缀下的对象列表。
     *
     * @param bucketName 存储桶名称
     * @param prefix     对象 Key 前缀
     * @return 对象信息列表
     */
    List<S3ObjectInfo> listObjectsRecursive(String bucketName, String prefix);

    /**
     * 递归查询默认存储桶指定前缀下的对象 Key 列表。
     *
     * @param prefix 对象 Key 前缀
     * @return 对象 Key 列表
     */
    List<String> listObjectKeys(String prefix);

    /**
     * 递归查询指定存储桶指定前缀下的对象 Key 列表。
     *
     * @param bucketName 存储桶名称
     * @param prefix     对象 Key 前缀
     * @return 对象 Key 列表
     */
    List<String> listObjectKeys(String bucketName, String prefix);

    /**
     * 查询对象版本列表。
     *
     * @param bucketName 存储桶名称
     * @param prefix     对象 Key 前缀
     * @return 对象版本信息列表
     */
    List<S3ObjectVersionInfo> listObjectVersions(String bucketName, String prefix);

    // ==================== 目录语义操作 ====================

    /**
     * 在默认存储桶下创建目录占位对象。
     *
     * @param directoryKey 目录 Key
     */
    void createDirectory(String directoryKey);

    /**
     * 在指定存储桶下创建目录占位对象。
     *
     * @param bucketName   存储桶名称
     * @param directoryKey 目录 Key
     */
    void createDirectory(String bucketName, String directoryKey);

    /**
     * 判断默认存储桶下的目录是否存在。
     *
     * @param directoryKey 目录 Key
     * @return true 存在，false 不存在
     */
    boolean directoryExists(String directoryKey);

    /**
     * 判断指定存储桶下的目录是否存在。
     *
     * @param bucketName   存储桶名称
     * @param directoryKey 目录 Key
     * @return true 存在，false 不存在
     */
    boolean directoryExists(String bucketName, String directoryKey);

    /**
     * 删除默认存储桶下的目录及其对象。
     *
     * @param directoryKey 目录 Key
     * @return 删除成功数量
     */
    Long deleteDirectory(String directoryKey);

    /**
     * 删除指定存储桶下的目录及其对象。
     *
     * @param bucketName   存储桶名称
     * @param directoryKey 目录 Key
     * @return 删除成功数量
     */
    Long deleteDirectory(String bucketName, String directoryKey);

    /**
     * 查询默认存储桶下目录当前层级对象列表。
     *
     * @param directoryKey 目录 Key
     * @return 对象信息列表
     */
    List<S3ObjectInfo> listDirectory(String directoryKey);

    /**
     * 查询指定存储桶下目录当前层级对象列表。
     *
     * @param bucketName   存储桶名称
     * @param directoryKey 目录 Key
     * @return 对象信息列表
     */
    List<S3ObjectInfo> listDirectory(String bucketName, String directoryKey);

    // ==================== 预签名 URL ====================

    /**
     * 生成默认存储桶下对象的下载预签名 URL。
     *
     * @param objectKey 对象 Key
     * @return 下载预签名 URL
     */
    URI generateDownloadUrl(String objectKey);

    /**
     * 生成指定存储桶下对象的下载预签名 URL。
     *
     * @param bucketName 存储桶名称
     * @param objectKey  对象 Key
     * @return 下载预签名 URL
     */
    URI generateDownloadUrl(String bucketName, String objectKey);

    /**
     * 生成默认存储桶下对象的下载预签名 URL。
     *
     * @param objectKey 对象 Key
     * @param expire    有效期
     * @return 下载预签名 URL
     */
    URI generateDownloadUrl(String objectKey, Duration expire);

    /**
     * 生成指定存储桶下对象的下载预签名 URL。
     *
     * @param bucketName 存储桶名称
     * @param objectKey  对象 Key
     * @param expire     有效期
     * @return 下载预签名 URL
     */
    URI generateDownloadUrl(String bucketName, String objectKey, Duration expire);

    /**
     * 生成默认存储桶下对象的上传预签名 URL。
     *
     * @param objectKey 对象 Key
     * @param expire    有效期
     * @return 上传预签名 URL
     */
    URI generateUploadUrl(String objectKey, Duration expire);

    /**
     * 生成指定存储桶下对象的上传预签名 URL。
     *
     * @param bucketName 存储桶名称
     * @param objectKey  对象 Key
     * @param expire     有效期
     * @return 上传预签名 URL
     */
    URI generateUploadUrl(String bucketName, String objectKey, Duration expire);

    /**
     * 生成默认存储桶下对象的 HEAD 预签名 URL。
     *
     * @param objectKey 对象 Key
     * @param expire    有效期
     * @return HEAD 预签名 URL
     */
    URI generateHeadUrl(String objectKey, Duration expire);

    /**
     * 生成指定存储桶下对象的 HEAD 预签名 URL。
     *
     * @param bucketName 存储桶名称
     * @param objectKey  对象 Key
     * @param expire     有效期
     * @return HEAD 预签名 URL
     */
    URI generateHeadUrl(String bucketName, String objectKey, Duration expire);

    /**
     * 根据请求生成预签名 URL。
     *
     * @param request 预签名 URL 请求
     * @return 预签名 URL 结果
     */
    S3PresignedUrlResult generatePresignedUrl(S3PresignedUrlRequest request);

    /**
     * 生成默认存储桶下对象的删除预签名 URL。
     *
     * @param objectKey 对象 Key
     * @param expire    有效期
     * @return 删除预签名 URL
     */
    URI generateDeleteUrl(String objectKey, Duration expire);

    /**
     * 生成指定存储桶下对象的删除预签名 URL。
     *
     * @param bucketName 存储桶名称
     * @param objectKey  对象 Key
     * @param expire     有效期
     * @return 删除预签名 URL
     */
    URI generateDeleteUrl(String bucketName, String objectKey, Duration expire);

    // ==================== 元数据管理 ====================

    /**
     * 获取默认存储桶下对象的自定义元数据。
     *
     * @param objectKey 对象 Key
     * @return 自定义元数据
     */
    Map<String, String> getMetadata(String objectKey);

    /**
     * 获取指定存储桶下对象的自定义元数据。
     *
     * @param bucketName 存储桶名称
     * @param objectKey  对象 Key
     * @return 自定义元数据
     */
    Map<String, String> getMetadata(String bucketName, String objectKey);

    /**
     * 替换默认存储桶下对象的自定义元数据。
     *
     * @param objectKey 对象 Key
     * @param metadata  自定义元数据
     */
    void replaceMetadata(String objectKey, Map<String, String> metadata);

    /**
     * 替换指定存储桶下对象的自定义元数据。
     *
     * @param bucketName 存储桶名称
     * @param objectKey  对象 Key
     * @param metadata   自定义元数据
     */
    void replaceMetadata(String bucketName, String objectKey, Map<String, String> metadata);

    /**
     * 根据请求替换对象自定义元数据。
     *
     * @param request 元数据替换请求
     */
    void replaceMetadata(S3MetadataRequest request);

    // ==================== 标签管理 ====================

    /**
     * 获取默认存储桶下对象标签。
     *
     * @param objectKey 对象 Key
     * @return 标签 Map
     */
    Map<String, String> getTags(String objectKey);

    /**
     * 获取指定存储桶下对象标签。
     *
     * @param bucketName 存储桶名称
     * @param objectKey  对象 Key
     * @return 标签 Map
     */
    Map<String, String> getTags(String bucketName, String objectKey);

    /**
     * 写入默认存储桶下对象标签。
     *
     * @param objectKey 对象 Key
     * @param tags      标签 Map
     */
    void putTags(String objectKey, Map<String, String> tags);

    /**
     * 写入指定存储桶下对象标签。
     *
     * @param bucketName 存储桶名称
     * @param objectKey  对象 Key
     * @param tags       标签 Map
     */
    void putTags(String bucketName, String objectKey, Map<String, String> tags);

    /**
     * 删除默认存储桶下对象标签。
     *
     * @param objectKey 对象 Key
     */
    void deleteTags(String objectKey);

    /**
     * 删除指定存储桶下对象标签。
     *
     * @param bucketName 存储桶名称
     * @param objectKey  对象 Key
     */
    void deleteTags(String bucketName, String objectKey);

    /**
     * 获取指定版本对象的标签。
     *
     * @param bucketName 存储桶名称
     * @param objectKey  对象 Key
     * @param versionId  版本 ID
     * @return 标签 Map
     */
    Map<String, String> getTags(String bucketName, String objectKey, String versionId);

    /**
     * 写入指定版本对象的标签。
     *
     * @param bucketName 存储桶名称
     * @param objectKey  对象 Key
     * @param versionId  版本 ID
     * @param tags       标签 Map
     */
    void putTags(String bucketName, String objectKey, String versionId, Map<String, String> tags);

    /**
     * 删除指定版本对象的标签。
     *
     * @param bucketName 存储桶名称
     * @param objectKey  对象 Key
     * @param versionId  版本 ID
     */
    void deleteTags(String bucketName, String objectKey, String versionId);

    // ==================== 访问控制与存储属性 ====================

    /**
     * 设置默认存储桶下对象 ACL。
     *
     * @param objectKey 对象 Key
     * @param acl       对象 ACL
     */
    void setObjectAcl(String objectKey, S3ObjectAcl acl);

    /**
     * 设置指定存储桶下对象 ACL。
     *
     * @param bucketName 存储桶名称
     * @param objectKey  对象 Key
     * @param acl        对象 ACL
     */
    void setObjectAcl(String bucketName, String objectKey, S3ObjectAcl acl);

    /**
     * 修改默认存储桶下对象存储类型。
     *
     * @param objectKey    对象 Key
     * @param storageClass 存储类型
     */
    void changeStorageClass(String objectKey, S3StorageClass storageClass);

    /**
     * 修改指定存储桶下对象存储类型。
     *
     * @param bucketName   存储桶名称
     * @param objectKey    对象 Key
     * @param storageClass 存储类型
     */
    void changeStorageClass(String bucketName, String objectKey, S3StorageClass storageClass);

    /**
     * 恢复默认存储桶下归档对象。
     *
     * @param objectKey 对象 Key
     * @param days      恢复副本保留天数
     */
    void restoreArchiveObject(String objectKey, Integer days);

    /**
     * 恢复指定存储桶下归档对象。
     *
     * @param bucketName 存储桶名称
     * @param objectKey  对象 Key
     * @param days       恢复副本保留天数
     */
    void restoreArchiveObject(String bucketName, String objectKey, Integer days);

    // ==================== 分片上传 ====================

    /**
     * 初始化分片上传。
     *
     * @param request 分片上传初始化请求
     * @return 分片上传初始化结果
     */
    S3MultipartUploadInitResult initMultipartUpload(S3MultipartUploadInitRequest request);

    /**
     * 上传分片。
     *
     * @param request 分片上传请求
     * @return 分片上传结果
     */
    S3MultipartUploadPartResult uploadPart(S3MultipartUploadPartRequest request);

    /**
     * 完成分片上传。
     *
     * @param request 分片上传完成请求
     * @return 上传结果
     */
    S3UploadResult completeMultipartUpload(S3MultipartUploadCompleteRequest request);

    /**
     * 终止分片上传。
     *
     * @param bucketName 存储桶名称
     * @param objectKey  对象 Key
     * @param uploadId   上传 ID
     */
    void abortMultipartUpload(String bucketName, String objectKey, String uploadId);

    /**
     * 查询未完成的分片上传任务。
     *
     * @param bucketName 存储桶名称
     * @param prefix     对象 Key 前缀
     * @return 分片上传任务列表
     */
    List<S3MultipartUploadInfo> listMultipartUploads(String bucketName, String prefix);

    /**
     * 查询指定分片上传任务已上传的分片列表。
     *
     * @param bucketName 存储桶名称
     * @param objectKey  对象 Key
     * @param uploadId   上传 ID
     * @return 已上传分片列表
     */
    List<S3MultipartUploadPartInfo> listMultipartUploadParts(String bucketName, String objectKey, String uploadId);

    // ==================== 工具方法 ====================

    /**
     * 规范化对象 Key。
     *
     * @param objectKey 对象 Key
     * @return 规范化后的对象 Key
     */
    String normalizeObjectKey(String objectKey);

    /**
     * 构建对象 Key。
     *
     * @param directory 目录
     * @param filename  文件名
     * @return 对象 Key
     */
    String buildObjectKey(String directory, String filename);

    /**
     * 获取对象 Key 中的文件名。
     *
     * @param objectKey 对象 Key
     * @return 文件名
     */
    String getFilename(String objectKey);

    /**
     * 获取对象 Key 中的扩展名。
     *
     * @param objectKey 对象 Key
     * @return 扩展名，不包含点号
     */
    String getExtension(String objectKey);

    /**
     * 获取默认存储桶下对象的公开访问 URL。
     *
     * @param objectKey 对象 Key
     * @return 公开访问 URL
     */
    String getPublicUrl(String objectKey);

    /**
     * 获取指定存储桶下对象的公开访问 URL。
     *
     * @param bucketName 存储桶名称
     * @param objectKey  对象 Key
     * @return 公开访问 URL
     */
    String getPublicUrl(String bucketName, String objectKey);

}
