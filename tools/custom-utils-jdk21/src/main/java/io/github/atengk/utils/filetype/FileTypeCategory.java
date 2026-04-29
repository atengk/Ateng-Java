package io.github.atengk.utils.filetype;

/**
 * 文件类型业务分类。
 *
 * @author Ateng
 * @since 2026-04-29
 */
public enum FileTypeCategory {

    /** 图片。 */
    IMAGE("图片"),

    /** 音频。 */
    AUDIO("音频"),

    /** 视频。 */
    VIDEO("视频"),

    /** 普通文档。 */
    DOCUMENT("文档"),

    /** PDF 文档。 */
    PDF("PDF"),

    /** Word 文档。 */
    WORD("Word"),

    /** Excel 表格。 */
    EXCEL("Excel"),

    /** PowerPoint 演示文稿。 */
    POWERPOINT("PowerPoint"),

    /** 文本。 */
    TEXT("文本"),

    /** 代码文件。 */
    CODE("代码"),

    /** 压缩包。 */
    ARCHIVE("压缩包"),

    /** 字体。 */
    FONT("字体"),

    /** 邮件。 */
    EMAIL("邮件"),

    /** 可执行文件。 */
    EXECUTABLE("可执行文件"),

    /** 脚本文件。 */
    SCRIPT("脚本"),

    /** 数据库文件。 */
    DATABASE("数据库文件"),

    /** 未知类型。 */
    UNKNOWN("未知");

    private final String displayName;

    FileTypeCategory(String displayName) {
        this.displayName = displayName;
    }

    /**
     * 获取分类展示名称。
     *
     * @return 分类展示名称
     */
    public String getDisplayName() {
        return displayName;
    }
}
