package local.ateng.java.storage.controller;

import cn.hutool.core.date.DateTime;
import cn.hutool.core.date.DateUtil;
import cn.hutool.core.io.FileTypeUtil;
import cn.hutool.core.util.StrUtil;
import lombok.RequiredArgsConstructor;
import org.dromara.x.file.storage.core.FileInfo;
import org.dromara.x.file.storage.core.FileStorageService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.io.ByteArrayInputStream;
import java.io.IOException;
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
