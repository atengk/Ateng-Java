package local.ateng.java.storage.controller;

import com.mybatisflex.core.paginate.Page;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.beans.factory.annotation.Autowired;
import local.ateng.java.storage.entity.FilePartDetail;
import local.ateng.java.storage.service.FilePartDetailService;
import org.springframework.web.bind.annotation.RestController;
import java.util.List;

/**
 * 文件分片信息表，仅在手动分片上传时使用 控制层。
 *
 * @author ATeng
 * @since 2025-02-28
 */
@RestController
@RequestMapping("/filePartDetail")
public class FilePartDetailController {

    @Autowired
    private FilePartDetailService filePartDetailService;

    /**
     * 添加文件分片信息表，仅在手动分片上传时使用。
     *
     * @param filePartDetail 文件分片信息表，仅在手动分片上传时使用
     * @return {@code true} 添加成功，{@code false} 添加失败
     */
    @PostMapping("save")
    public boolean save(@RequestBody FilePartDetail filePartDetail) {
        return filePartDetailService.save(filePartDetail);
    }

    /**
     * 根据主键删除文件分片信息表，仅在手动分片上传时使用。
     *
     * @param id 主键
     * @return {@code true} 删除成功，{@code false} 删除失败
     */
    @DeleteMapping("remove/{id}")
    public boolean remove(@PathVariable String id) {
        return filePartDetailService.removeById(id);
    }

    /**
     * 根据主键更新文件分片信息表，仅在手动分片上传时使用。
     *
     * @param filePartDetail 文件分片信息表，仅在手动分片上传时使用
     * @return {@code true} 更新成功，{@code false} 更新失败
     */
    @PutMapping("update")
    public boolean update(@RequestBody FilePartDetail filePartDetail) {
        return filePartDetailService.updateById(filePartDetail);
    }

    /**
     * 查询所有文件分片信息表，仅在手动分片上传时使用。
     *
     * @return 所有数据
     */
    @GetMapping("list")
    public List<FilePartDetail> list() {
        return filePartDetailService.list();
    }

    /**
     * 根据文件分片信息表，仅在手动分片上传时使用主键获取详细信息。
     *
     * @param id 文件分片信息表，仅在手动分片上传时使用主键
     * @return 文件分片信息表，仅在手动分片上传时使用详情
     */
    @GetMapping("getInfo/{id}")
    public FilePartDetail getInfo(@PathVariable String id) {
        return filePartDetailService.getById(id);
    }

    /**
     * 分页查询文件分片信息表，仅在手动分片上传时使用。
     *
     * @param page 分页对象
     * @return 分页对象
     */
    @GetMapping("page")
    public Page<FilePartDetail> page(Page<FilePartDetail> page) {
        return filePartDetailService.page(page);
    }

}
