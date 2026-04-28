package local.ateng.java.awss3.entity;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 断点记录类：保存 uploadId、key、以及已上传分片（partNumber -> eTag）
 *
 * @author 孔余
 * @since 2025-07-21
 */
public class UploadResumeRecord {
    private final String uploadId;
    private final String key;
    private final Map<Integer, String> uploadedParts = new ConcurrentHashMap<>();

    public UploadResumeRecord(String uploadId, String key) {
        this.uploadId = uploadId;
        this.key = key;
    }

    public String getUploadId() {
        return uploadId;
    }

    public String getKey() {
        return key;
    }

    public Map<Integer, String> getUploadedParts() {
        return uploadedParts;
    }
}
