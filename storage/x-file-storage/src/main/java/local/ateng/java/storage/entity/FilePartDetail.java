package local.ateng.java.storage.entity;

import com.mybatisflex.annotation.Id;
import com.mybatisflex.annotation.KeyType;
import com.mybatisflex.annotation.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serial;
import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 文件分片信息表，仅在手动分片上传时使用 实体类。
 *
 * @author ATeng
 * @since 2025-02-28
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Table("file_part_detail")
public class FilePartDetail implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * 分片id
     */
    @Id(keyType = KeyType.Auto)
    private String id;

    /**
     * 存储平台
     */
    private String platform;

    /**
     * 上传ID，仅在手动分片上传时使用
     */
    private String uploadId;

    /**
     * 分片 ETag
     */
    private String eTag;

    /**
     * 分片号。每一个上传的分片都有一个分片号，一般情况下取值范围是1~10000
     */
    private Integer partNumber;

    /**
     * 文件大小，单位字节
     */
    private Long partSize;

    /**
     * 哈希信息
     */
    private String hashInfo;

    /**
     * 创建时间
     */
    private LocalDateTime createTime;

}
