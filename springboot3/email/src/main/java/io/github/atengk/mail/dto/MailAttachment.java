package io.github.atengk.mail.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

/**
 * 邮件附件信息。
 *
 * @author Ateng
 * @since 2026-05-06
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MailAttachment implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 附件文件名。
     */
    private String fileName;

    /**
     * 附件内容类型，例如 application/pdf、image/png。
     */
    private String contentType;

    /**
     * 本地文件路径，适用于服务端已有文件的场景。
     */
    private String filePath;

    /**
     * 附件字节内容，适用于动态生成文件或上传文件转发的场景。
     */
    private byte[] content;
}
