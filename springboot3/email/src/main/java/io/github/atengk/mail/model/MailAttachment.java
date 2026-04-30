package io.github.atengk.mail.model;

import cn.hutool.core.util.StrUtil;

import java.io.File;

/**
 * 邮件附件对象
 *
 * @author Ateng
 * @since 2026-04-30
 */
public record MailAttachment(String fileName, File file) {

    /**
     * 获取附件显示名称
     *
     * @return 附件显示名称
     */
    public String getDisplayFileName() {
        if (file == null) {
            return fileName;
        }
        return StrUtil.blankToDefault(fileName, file.getName());
    }
}