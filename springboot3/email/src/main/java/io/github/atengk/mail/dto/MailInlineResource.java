package io.github.atengk.mail.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

/**
 * HTML 邮件内嵌资源信息。
 *
 * @author Ateng
 * @since 2026-05-06
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MailInlineResource implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * HTML 中引用的 contentId，例如 logo，对应 cid:logo。
     */
    private String contentId;

    /**
     * 资源名称。
     */
    private String name;

    /**
     * 资源内容类型，例如 image/png。
     */
    private String contentType;

    /**
     * 本地资源路径。
     */
    private String filePath;

    /**
     * 资源字节内容。
     */
    private byte[] content;
}
