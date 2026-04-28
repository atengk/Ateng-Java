package local.ateng.java.awss3.controller;

import local.ateng.java.awss3.service.S3Service;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.HashMap;
import java.util.Map;

/**
 * S3 分片上传控制器
 *
 * 端点说明：
 *  - POST /api/s3/multipart/init    -> 初始化 multipart 上传，返回 uploadId
 *  - POST /api/s3/multipart/upload  -> 上传单个分片（表单 file）
 *  - GET  /api/s3/multipart/parts   -> 列出已上传的分片信息（用于续传/检查）
 *  - POST /api/s3/multipart/complete-> 完成 multipart 合并
 *  - POST /api/s3/multipart/abort   -> 中止 multipart（放弃）
 */
@RestController
@RequestMapping("/api/s3/multipart")
@RequiredArgsConstructor
public class S3MultipartController {

    private final S3Service s3Service;

    /**
     * 初始化一个 Multipart Upload，会返回 uploadId（后续上传分片时使用）
     *
     * @param key         S3 对象 key（必须）
     * @param contentType 可选的 contentType（建议传入）
     * @return JSON 包含 uploadId
     */
    @PostMapping("/init")
    public ResponseEntity<Map<String, String>> init(@RequestParam("key") String key,
                                                    @RequestParam(value = "contentType", required = false) String contentType) {
        String uploadId = s3Service.initiateMultipartUpload(key, contentType);
        HashMap<String, String> map = new HashMap<>();
        map.put("uploadId", uploadId);
        return ResponseEntity.ok(map);
    }

    /**
     * 接收并上传单个分片到 S3（后台直接调用 UploadPart）
     *
     * 参数说明：
     *  - uploadId : 必须（init 时得到）
     *  - partNumber : 分片序号（1 ~ 10000）
     *  - file : 分片数据（multipart/form-data）
     *
     * 返回：
     *  - {"partNumber":1, "eTag":"..."}
     */
    @PostMapping("/upload")
    public ResponseEntity<Map<String, String>> uploadPart(@RequestParam("uploadId") String uploadId,
                                                          @RequestParam("partNumber") int partNumber,
                                                          @RequestParam("file") MultipartFile file) {
        String eTag = s3Service.uploadPart(uploadId, partNumber, file);
        HashMap<String, String> map = new HashMap<>();
        map.put("eTag", eTag);
        map.put("partNumber", String.valueOf(partNumber));
        return ResponseEntity.ok(map);
    }

    /**
     * 列出当前 uploadId 已上传的分片（返回 map: partNumber -> eTag）
     * 可用于前端在中断后查询已上传哪些分片，决定是否重新上传
     */
    @GetMapping("/parts")
    public ResponseEntity<Map<Integer, String>> listParts(@RequestParam("uploadId") String uploadId) {
        Map<Integer, String> parts = s3Service.listUploadedParts(uploadId);
        return ResponseEntity.ok(parts);
    }

    /**
     * 完成 multipart 合并（由前端在确认所有分片均已上传后调用）
     *
     * @param uploadId 必须
     */
    @PostMapping("/complete")
    public ResponseEntity<Map<String, String>> complete(@RequestParam("uploadId") String uploadId) {
        String locationOrETag = s3Service.completeMultipartUpload(uploadId);
        HashMap<String, String> map = new HashMap<>();
        map.put("result", "completed");
        map.put("etagOrLocation", locationOrETag);
        return ResponseEntity.ok(map);
    }

    /**
     * 中止 multipart（放弃此次上传）
     *
     * @param uploadId 必须
     */
    @PostMapping("/abort")
    public ResponseEntity<Map<String, String>> abort(@RequestParam("uploadId") String uploadId) {
        s3Service.abortMultipartUpload(uploadId);
        HashMap<String, String> map = new HashMap<>();
        map.put("result", "aborted");
        return ResponseEntity.ok(map);
    }
}
