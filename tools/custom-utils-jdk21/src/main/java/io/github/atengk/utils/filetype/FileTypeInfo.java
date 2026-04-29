package io.github.atengk.utils.filetype;


import java.util.Objects;

/**
 * 文件类型识别结果。
 *
 * @author Ateng
 * @since 2026-04-29
 */
public final class FileTypeInfo {

    private final String fileName;
    private final String extension;
    private final String mimeType;
    private final String mediaType;
    private final FileTypeCategory category;
    private final boolean known;
    private final boolean binary;
    private final boolean safe;
    private final boolean extensionMatched;
    private final boolean dangerous;
    private final String description;

    /**
     * 创建文件类型识别结果。
     *
     * @param fileName 原始文件名
     * @param extension 扩展名
     * @param mimeType MIME 类型
     * @param mediaType MediaType 字符串
     * @param category 文件分类
     * @param known 是否已知类型
     * @param binary 是否二进制
     * @param safe 是否安全
     * @param extensionMatched 扩展名是否匹配 MIME 类型
     * @param dangerous 是否危险类型
     * @param description 类型描述
     */
    public FileTypeInfo(String fileName,
                        String extension,
                        String mimeType,
                        String mediaType,
                        FileTypeCategory category,
                        boolean known,
                        boolean binary,
                        boolean safe,
                        boolean extensionMatched,
                        boolean dangerous,
                        String description) {
        this.fileName = FileTypeUtil.nullToEmpty(fileName);
        this.extension = FileTypeUtil.normalizeExtension(extension);
        this.mimeType = FileTypeUtil.normalizeMimeType(mimeType);
        this.mediaType = FileTypeUtil.nullToEmpty(mediaType);
        this.category = category == null ? FileTypeCategory.UNKNOWN : category;
        this.known = known;
        this.binary = binary;
        this.safe = safe;
        this.extensionMatched = extensionMatched;
        this.dangerous = dangerous;
        this.description = FileTypeUtil.nullToEmpty(description);
    }

    /**
     * 获取原始文件名。
     *
     * @return 原始文件名
     */
    public String getFileName() {
        return fileName;
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
     * 获取 MIME 类型。
     *
     * @return MIME 类型
     */
    public String getMimeType() {
        return mimeType;
    }

    /**
     * 获取 MediaType 字符串。
     *
     * @return MediaType 字符串
     */
    public String getMediaType() {
        return mediaType;
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
     * 判断是否已知类型。
     *
     * @return true 表示已知类型
     */
    public boolean isKnown() {
        return known;
    }

    /**
     * 判断是否二进制类型。
     *
     * @return true 表示二进制类型
     */
    public boolean isBinary() {
        return binary;
    }

    /**
     * 判断是否安全。
     *
     * @return true 表示安全
     */
    public boolean isSafe() {
        return safe;
    }

    /**
     * 判断扩展名是否匹配 MIME 类型。
     *
     * @return true 表示匹配
     */
    public boolean isExtensionMatched() {
        return extensionMatched;
    }

    /**
     * 判断是否危险类型。
     *
     * @return true 表示危险类型
     */
    public boolean isDangerous() {
        return dangerous;
    }

    /**
     * 获取类型描述。
     *
     * @return 类型描述
     */
    public String getDescription() {
        return description;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof FileTypeInfo that)) {
            return false;
        }
        return known == that.known
                && binary == that.binary
                && safe == that.safe
                && extensionMatched == that.extensionMatched
                && dangerous == that.dangerous
                && Objects.equals(fileName, that.fileName)
                && Objects.equals(extension, that.extension)
                && Objects.equals(mimeType, that.mimeType)
                && Objects.equals(mediaType, that.mediaType)
                && category == that.category
                && Objects.equals(description, that.description);
    }

    @Override
    public int hashCode() {
        return Objects.hash(fileName, extension, mimeType, mediaType, category, known, binary, safe, extensionMatched, dangerous, description);
    }

    @Override
    public String toString() {
        return "FileTypeInfo{" +
                "fileName='" + fileName + '\'' +
                ", extension='" + extension + '\'' +
                ", mimeType='" + mimeType + '\'' +
                ", mediaType='" + mediaType + '\'' +
                ", category=" + category +
                ", known=" + known +
                ", binary=" + binary +
                ", safe=" + safe +
                ", extensionMatched=" + extensionMatched +
                ", dangerous=" + dangerous +
                ", description='" + description + '\'' +
                '}';
    }
}
