package io.github.atengk.utils.filetype;

import java.util.Objects;

/**
 * 文件类型规则，用于需要外部组织规则列表的场景。
 *
 * @author Ateng
 * @since 2026-04-29
 */
public final class FileTypeRule {

    private final String name;
    private final String mimePattern;
    private final String extension;
    private final FileTypeCategory category;
    private final boolean allow;

    /**
     * 创建文件类型规则。
     *
     * @param name 规则名称
     * @param mimePattern MIME 匹配模式
     * @param extension 扩展名
     * @param category 文件分类
     * @param allow true 表示允许规则，false 表示拒绝规则
     */
    public FileTypeRule(String name, String mimePattern, String extension, FileTypeCategory category, boolean allow) {
        this.name = FileTypeUtil.nullToEmpty(name);
        this.mimePattern = FileTypeUtil.normalizeMimeType(mimePattern);
        this.extension = FileTypeUtil.normalizeExtension(extension);
        this.category = category == null ? FileTypeCategory.UNKNOWN : category;
        this.allow = allow;
    }

    /**
     * 获取规则名称。
     *
     * @return 规则名称
     */
    public String getName() {
        return name;
    }

    /**
     * 获取 MIME 匹配模式。
     *
     * @return MIME 匹配模式
     */
    public String getMimePattern() {
        return mimePattern;
    }

    /**
     * 获取扩展名。
     *
     * @return 扩展名
     */
    public String getExtension() {
        return extension;
    }

    /**
     * 获取文件分类。
     *
     * @return 文件分类
     */
    public FileTypeCategory getCategory() {
        return category;
    }

    /**
     * 判断是否允许规则。
     *
     * @return true 表示允许规则
     */
    public boolean isAllow() {
        return allow;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof FileTypeRule that)) {
            return false;
        }
        return allow == that.allow
                && Objects.equals(name, that.name)
                && Objects.equals(mimePattern, that.mimePattern)
                && Objects.equals(extension, that.extension)
                && category == that.category;
    }

    @Override
    public int hashCode() {
        return Objects.hash(name, mimePattern, extension, category, allow);
    }

    @Override
    public String toString() {
        return "FileTypeRule{" +
                "name='" + name + '\'' +
                ", mimePattern='" + mimePattern + '\'' +
                ", extension='" + extension + '\'' +
                ", category=" + category +
                ", allow=" + allow +
                '}';
    }
}
