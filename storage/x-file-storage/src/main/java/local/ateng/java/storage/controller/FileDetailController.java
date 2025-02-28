package local.ateng.java.storage.controller;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.date.DateTime;
import cn.hutool.core.date.DateUtil;
import cn.hutool.core.io.FileTypeUtil;
import cn.hutool.core.map.MapProxy;
import cn.hutool.core.util.ObjUtil;
import cn.hutool.core.util.StrUtil;
import com.alibaba.fastjson2.JSONObject;
import lombok.RequiredArgsConstructor;
import org.dromara.x.file.storage.core.FileInfo;
import org.dromara.x.file.storage.core.FileStorageService;
import org.dromara.x.file.storage.core.ProgressListener;
import org.dromara.x.file.storage.core.constant.Constant;
import org.dromara.x.file.storage.core.get.RemoteFileInfo;
import org.dromara.x.file.storage.core.presigned.GeneratePresignedUrlResult;
import org.dromara.x.file.storage.core.upload.FilePartInfo;
import org.dromara.x.file.storage.core.upload.FilePartInfoList;
import org.dromara.x.file.storage.core.upload.MultipartUploadSupportInfo;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.util.Assert;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.Date;
import java.util.List;
import java.util.stream.Collectors;

/**
 * X File Storage 演示
 *
 * @author 孔余
 * @email 2385569970@qq.com
 * @since 2025-01-07
 */
@RestController
@RequestMapping("/file")
@RequiredArgsConstructor(onConstructor = @__(@Autowired))
public class FileDetailController {
    private final FileStorageService fileStorageService;

    /**
     * 上传文件，使用默认值
     */
    @PostMapping("/upload1")
    public FileInfo upload1(MultipartFile file) {
        return fileStorageService.of(file).upload();
    }

    /**
     * 上传文件，阿里云OSS
     */
    @PostMapping("/upload-oss")
    public FileInfo uploadOss(MultipartFile file) {
        return fileStorageService.of(file).setPlatform("aliyun-oss-1").upload();
    }

    /**
     * 上传文件，FTP
     */
    @PostMapping("/upload-ftp")
    public FileInfo uploadFtp(MultipartFile file) {
        return fileStorageService.of(file).setPlatform("ftp-1").upload();
    }

    /**
     * 监听上传进度
     */
    @PostMapping("/upload-progress")
    public FileInfo uploadProgressListener(MultipartFile file) {
        // 设置文件路径
        DateTime dateTime = DateUtil.date();
        String path = DateUtil.format(dateTime, "yyyy/MM/dd/");
        return fileStorageService
                .of(file)
                .setPath(path)
                .setProgressListener(new ProgressListener() {
                    @Override
                    public void start() {
                        System.out.println("上传开始");
                    }

                    @Override
                    public void progress(long progressSize, Long allSize) {
                        System.out.println("已上传 " + progressSize + " 总大小" + (allSize == null ? "未知" : allSize));
                    }

                    @Override
                    public void finish() {
                        System.out.println("上传结束");
                    }
                })
                .upload();
    }

    /**
     * 生成上传 URL 给客户端，从而实现客户端直传文件，不经过后台
     */
    @GetMapping("/generate-presigned-url")
    public GeneratePresignedUrlResult generatePresignedUrl() {
        GeneratePresignedUrlResult uploadResult = fileStorageService
                .generatePresignedUrl()
                .setPlatform("minio-1") // 存储平台，不传使用默认的
                .setPath("test/") // 设置路径
                .setFilename("image.jpg") // 设置保存的文件名
                .setMethod(Constant.GeneratePresignedUrl.Method.PUT)    // 签名方法
                .setExpiration(DateUtil.offsetMinute(new Date(), 10))   // 设置过期时间 10 分钟
                .putHeaders(Constant.Metadata.CONTENT_TYPE, "image/jpeg") // 设置要上传文件 MIME 类型
                .putHeaders(Constant.Metadata.CONTENT_DISPOSITION, "attachment;filename=DownloadFileName.jpg") //设置其它元数据，不需要可省略
                .putUserMetadata("role", "666") //设置自定义用户元数据，不需要可省略
                .putQueryParams("admin", "123456") //设置自定义查询参数，不需要可省略
                .generatePresignedUrl();
        Assert.notNull(uploadResult, "生成上传预签名 URL 失败！");
        return uploadResult;
    }

    /**
     * 查询已上传的文件
     */
    @GetMapping("/get-file-info")
    public FileInfo getFileInfo() {
        //自行传入 path 及 filename 获取文件信息
        RemoteFileInfo info3 = fileStorageService.getFile().setPath("test/").setFilename("image.jpg").getFile();
        Assert.notNull(info3, "文件不存在");
        //文件元数据
        MapProxy metadata = info3.getKebabCaseInsensitiveMetadata();
        System.out.println(metadata);
        //文件用户元数据
        MapProxy userMetadata = info3.getKebabCaseInsensitiveUserMetadata();
        System.out.println(userMetadata);
        //转换成 FileInfo 可方便进行其它操作或保存到数据库中
        FileInfo fileInfo = info3.toFileInfo();
        return fileInfo;
    }

    /**
     * 手动分片上传
     * 1. 客户端首次准备分片上传时，需要调用该接口，什么参数都不需要传，由服务端判断后返回FileInfo数据（标识）给客户端
     * 2. 接着就开始进行分片上传，将FileInfo数据和file、chunkIndex、totalChunks依次上传给服务端，当chunkIndex==totalChunks时即视为分片文件上传完毕
     *
     * @param file        分片的文件
     * @param chunkIndex  分片索引号，分片文件的顺序号，从1开始
     * @param totalChunks 分片文件总数
     * @param fileInfoStr FileInfo字符串
     * @return
     */
    @PostMapping("/uploadPart")
    public JSONObject uploadPart(
            MultipartFile file,
            Integer chunkIndex,
            Integer totalChunks,
            String fileInfoStr
    ) {
        JSONObject result = JSONObject.of("code", "0");
        // 1. 手动分片上传-是否支持
        MultipartUploadSupportInfo supportInfo = fileStorageService.isSupportMultipartUpload();
        Assert.isTrue(supportInfo.getIsSupport(), "不支持分片上传");
        Assert.isTrue((chunkIndex == null && totalChunks == null) || (totalChunks >= chunkIndex && chunkIndex >= 1), "分片上传错误");
        System.out.println(supportInfo.getIsSupport());//是否支持手动分片上传，正常情况下判断此参数就行了
        System.out.println(supportInfo.getIsSupportListParts());//是否支持列举已上传的分片
        System.out.println(supportInfo.getIsSupportAbort());//是否支持取消上传
        // 2. 手动分片上传-初始化
        FileInfo fileInfo = JSONObject.parseObject(fileInfoStr, FileInfo.class);
        if (BeanUtil.isEmpty(fileInfo)) {
            // 首次上传文件请求直接返回FileInfo信息
            String path = DateUtil.format(DateUtil.date(), "yyyy/MM/dd/");
            fileInfo = fileStorageService
                    .initiateMultipartUpload()
                    .setPath(path)
                    .setSaveFilename("1111111.txt")
                    .putUserMetadata("author", "ateng")
                    .init();
            result.put("fileInfo", fileInfo);
            return result;
        }
        // 3. 手动分片上传-上传分片
        FilePartInfo filePartInfo = fileStorageService.uploadPart(fileInfo, chunkIndex, file).upload();
        //System.out.println(filePartInfo);
        // 4. 手动分片上传-列举已上传的分片
        FilePartInfoList partList = fileStorageService.listParts(fileInfo).listParts();
        //System.out.println(partList);
        result.put("fileInfo", fileInfo);
        result.put("partList", partList.getList());
        // 5. 手动分片上传-完成
        if (chunkIndex == totalChunks) {
            FileInfo completeFileInfo = fileStorageService.completeMultipartUpload(fileInfo).complete();
            //System.out.println(completeFileInfo);
            // completeFileInfo.uploadStatus=2 表示分片上传完成
            result.put("fileInfo", fileInfo);
        }
        return result;
    }

    /**
     * 手动分片上传，解决需要顺序上传的问题，实现客户端多线程上传
     * 1. 客户端首次准备分片上传时，需要调用该接口，什么参数都不需要传（可以传入filename），由服务端判断后返回FileInfo数据（标识）给客户端
     * 2. 接着就开始进行分片上传，将FileInfo数据和file、chunkIndex、totalChunks依次上传给服务端
     * 3. 所有分片文件都上传完毕后，调用该接口传入 fileInfoStr 和 isComplete=true（其他参数不用传）完成分片的合并
     *
     * @param file        分片的文件
     * @param chunkIndex  分片索引号，分片文件的顺序号，从1开始
     * @param totalChunks 分片文件总数
     * @param fileInfoStr FileInfo字符串
     * @param isComplete 分片是否上传完成
     * @return
     */
    @PostMapping("/uploadPartPlus")
    public JSONObject uploadPartPlus(
            String filename,
            MultipartFile file,
            Integer chunkIndex,
            Integer totalChunks,
            String fileInfoStr,
            Boolean isComplete
    ) {
        JSONObject result = JSONObject.of("code", "0");
        // 1. 手动分片上传-是否支持
        MultipartUploadSupportInfo supportInfo = fileStorageService.isSupportMultipartUpload();
        Assert.isTrue(supportInfo.getIsSupport(), "不支持分片上传");
        Assert.isTrue((chunkIndex == null && totalChunks == null) || (totalChunks >= chunkIndex && chunkIndex >= 1), "分片上传错误");
        // 2. 手动分片上传-初始化
        FileInfo fileInfo = JSONObject.parseObject(fileInfoStr, FileInfo.class);
        if (BeanUtil.isEmpty(fileInfo) && ObjUtil.isEmpty(isComplete)) {
            // 首次上传文件请求直接返回FileInfo信息
            String path = DateUtil.format(DateUtil.date(), "yyyy/MM/dd/");
            fileInfo = fileStorageService
                    .initiateMultipartUpload()
                    .setPath(path)
                    .setSaveFilename(!StrUtil.isBlank(filename), filename)
                    .setOriginalFilename(!StrUtil.isBlank(filename), filename)
                    .putUserMetadata("author", "ateng")
                    .init();
            result.put("fileInfo", fileInfo);
            return result;
        }
        // 5. 手动分片上传-完成
        if (!ObjUtil.isEmpty(isComplete)) {
            FileInfo completeFileInfo = fileStorageService.completeMultipartUpload(fileInfo).complete();
            // completeFileInfo.uploadStatus=2 表示分片上传完成
            result.put("fileInfo", fileInfo);
        } else {
            // 3. 手动分片上传-上传分片
            FilePartInfo filePartInfo = fileStorageService.uploadPart(fileInfo, chunkIndex, file).upload();
            // 4. 手动分片上传-列举已上传的分片
            FilePartInfoList partList = fileStorageService.listParts(fileInfo).listParts();
            result.put("fileInfo", fileInfo);
            result.put("partList", partList.getList());
        }
        return result;
    }

    /**
     * 上传文件，自定义路径
     */
    @PostMapping("/upload2")
    public FileInfo upload2(MultipartFile file) {
        // 设置文件路径
        DateTime dateTime = DateUtil.date();
        String path = DateUtil.format(dateTime, "yyyy/MM/dd/");
        return fileStorageService
                .of(file)
                .setPath(path)
                .upload();
    }

    /**
     * 上传文件，二进制数据byte[]
     * 这种情况需要自定义文件名和类型
     */
    @PostMapping("/upload3")
    public FileInfo upload3(MultipartFile file) throws IOException {
        // 假设这里是得到了一个文件的二进制，可以是流或者其他途径
        byte[] bytes = file.getBytes();
        // FileTypeUtil 是 Hutool 提供的工具类，它通过分析文件头的魔数（magic number）来判断文件的类型
        String type = FileTypeUtil.getType(new ByteArrayInputStream(bytes), true);
        // 设置文件路径
        DateTime dateTime = DateUtil.date();
        String path = DateUtil.format(dateTime, "yyyy/MM/dd/");
        // 设置文件名
        String dateTimeStr = DateUtil.format(dateTime, "yyyyMMdd-HHmmss");
        String filename = StrUtil.format("{}.{}", dateTimeStr, type);
        return fileStorageService
                .of(file)
                .setPath(path)
                .setSaveFilename(filename)
                .setOriginalFilename(filename)
                .upload();
    }

    /**
     * 上传多个文件
     */
    @PostMapping("/upload4")
    public List<FileInfo> uploadMultipleFiles(List<MultipartFile> files) {
        return files.stream()
                .map(file -> fileStorageService.of(file).upload())
                .collect(Collectors.toList());
    }

    /**
     * 上传图片，成功返回文件信息
     * 图片处理使用的是 https://github.com/coobird/thumbnailator
     * 会将图片压缩到1000像素
     */
    @PostMapping("/upload-image")
    public FileInfo uploadImage(MultipartFile file) {
        return fileStorageService.of(file)
                .image(img -> img.size(1000, 1000))  //将图片大小调整到 1000*1000
                .thumbnail(th -> th.size(200, 200))  //再生成一张 200*200 的缩略图
                .upload();
    }


}
