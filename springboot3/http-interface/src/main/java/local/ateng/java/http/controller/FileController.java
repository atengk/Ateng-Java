package local.ateng.java.http.controller;

import local.ateng.java.http.entity.MyUser;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@RestController
@RequestMapping("/file")
public class FileController {

    // 上传单个文件
    @PostMapping("/upload")
    public String uploadFile(@RequestParam("file") MultipartFile file) {
        // 获取文件名
        String fileName = file.getOriginalFilename();

        return "File uploaded successfully: " + fileName;
    }

    // 上传文件和其他字段（例如描述）
    @PostMapping("/uploadWithFields")
    public String uploadFileWithFields(@RequestParam("file") MultipartFile file,
                                       @RequestParam("description") String description) {
        String fileName = file.getOriginalFilename();
        return "File " + fileName + " uploaded with description: " + description;
    }

    // 上传文件和实体类
    // https://www.cnblogs.com/Jason-Xiang/p/14611447.html
    @PostMapping("/uploadWithJson")
    public String uploadFileWithJson(@RequestPart("file") MultipartFile file,
                                       @RequestPart("user") MyUser user) {
        String fileName = file.getOriginalFilename();
        return "File " + fileName + " uploaded with user: " + user;
    }

    // 上传多个文件
    @PostMapping("/uploadMultiple")
    public String uploadMultipleFiles(@RequestParam("files") List<MultipartFile> files) {
        StringBuilder fileNames = new StringBuilder();

        // 遍历所有上传的文件
        for (MultipartFile file : files) {
            fileNames.append(file.getOriginalFilename()).append(", ");
        }

        return "Uploaded files: " + fileNames;
    }

}
