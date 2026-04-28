package local.ateng.java.awss3.controller;

import local.ateng.java.awss3.service.S3Service;
import local.ateng.java.awss3.utils.ZipUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.Duration;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/s3")
@RequiredArgsConstructor
public class S3Controller {

    private final S3Service s3Service;

    @PostMapping("/uploadFile")
    public ResponseEntity<Void> uploadFile(MultipartFile file, String key) {
        String path = s3Service.generateKey(file.getOriginalFilename());
        System.out.println(path);
        s3Service.uploadFile(key, file);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/uploadFileAndMeta")
    public ResponseEntity<Void> uploadFileAndMeta(MultipartFile file, String key) {
        Map<String, String> metadata = new HashMap<>();
        metadata.put("original-filename", file.getOriginalFilename());
        metadata.put("data-name", "test name");
        metadata.put("data-name2", "test 阿腾");
        s3Service.uploadFile(key, file, metadata);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/getDecodedMetadata")
    public ResponseEntity<Map<String, String>> getDecodedMetadata(String key) {
        return ResponseEntity.ok(s3Service.getDecodedMetadata(key));
    }

    @PostMapping("/uploadMultipleFiles")
    public ResponseEntity<Void> uploadMultipleFiles(String[] keys, MultipartFile[] files) {
        s3Service.uploadMultipleFiles(Arrays.asList(keys), Arrays.asList(files));
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/downloadToFile")
    public ResponseEntity<Void> downloadToFile(String key, String localPath) {
        s3Service.downloadToFile(key, Paths.get(localPath));
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/downloadFileAsBase64Uri")
    public ResponseEntity<String> downloadFileAsBase64Uri(String key) {
        return ResponseEntity.ok(s3Service.downloadFileAsBase64Uri(key));
    }

    @PostMapping("/downloadToResponse")
    public void downloadToResponse(String key, String fileName, HttpServletResponse response) {
        s3Service.downloadToResponse(key, fileName, response);
    }

    @PostMapping("/downloadMultipleToFilesAsync")
    public ResponseEntity<Void> downloadMultipleToFilesAsync() {
        List<String> keys = Arrays.asList("upload/1.jpg", "upload/2.jpg", "upload/3.jpg");
        List<Path> localPaths = Arrays.asList(Paths.get("D:\\temp\\download\\1.jpg"), Paths.get("D:\\temp\\download\\2.jpg"), Paths.get("D:\\temp\\download\\3.jpg"));
        s3Service.downloadMultipleToFilesAsync(keys, localPaths, true);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/listFiles")
    public ResponseEntity<List<String>> listFiles(String prefix) {
        List<String> files = s3Service.listFilesStr(prefix);
        return ResponseEntity.ok(files);
    }

    @GetMapping("/generatePublicUrl")
    public ResponseEntity<String> generatePublicUrl(String key) {
        String url = s3Service.generatePublicUrl(key);
        return ResponseEntity.ok(url);
    }

    @GetMapping("/generatePresignedUrl")
    public ResponseEntity<String> generatePresignedUrl(String key) {
        String url = s3Service.generatePresignedUrl(key, Duration.ofHours(1));
        return ResponseEntity.ok(url);
    }

    @GetMapping("/generatePresignedUploadUrl")
    public ResponseEntity<String> generatePresignedUploadUrl(String key) {
        String url = s3Service.generatePresignedUploadUrl(key, Duration.ofHours(1));
        return ResponseEntity.ok(url);
    }

    @DeleteMapping("/deleteFile")
    public ResponseEntity<Void> deleteFile(String key) {
        s3Service.deleteFile(key);
        return ResponseEntity.noContent().build();
    }

    @DeleteMapping("/deleteFolderRecursively")
    public ResponseEntity<Void> deleteFolderRecursively(String key) {
        s3Service.deleteFolderRecursively(key);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/zip")
    public ResponseEntity<Void> zip(HttpServletResponse response) throws IOException {
        List<Path> localPaths = Arrays.asList(Paths.get("D:\\temp\\download\\1.jpg"), Paths.get("D:\\temp\\download\\2.jpg"));
        ZipUtil.zip(localPaths, response, "孔余  asdhasiu 8738&@!*&#(!.zip");
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/downloadFolder")
    public void downloadFolder(String prefix, String localBaseDir) {
        s3Service.downloadFolder(prefix, Paths.get(localBaseDir));
    }

    @PutMapping("/uploadFolder")
    public ResponseEntity<Void> uploadFolder(String localBaseDir, String prefix) {
        s3Service.uploadFolder(Paths.get(localBaseDir), prefix);
        return ResponseEntity.noContent().build();
    }

}
