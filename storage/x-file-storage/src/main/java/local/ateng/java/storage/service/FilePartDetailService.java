package local.ateng.java.storage.service;

import com.mybatisflex.core.service.IService;
import local.ateng.java.storage.entity.FilePartDetail;
import org.dromara.x.file.storage.core.upload.FilePartInfo;

/**
 * 文件分片信息表，仅在手动分片上传时使用 服务层。
 *
 * @author ATeng
 * @since 2025-02-28
 */
public interface FilePartDetailService extends IService<FilePartDetail> {

    // 保存文件分片信息
    void saveFilePart(FilePartInfo info);

    // 删除文件分片信息
    void deleteFilePartByUploadId(String uploadId);
}
