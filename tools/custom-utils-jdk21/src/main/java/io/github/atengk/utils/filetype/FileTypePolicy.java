package io.github.atengk.utils.filetype;

import java.util.Collection;
import java.util.Collections;
import java.util.EnumSet;
import java.util.LinkedHashSet;
import java.util.Objects;
import java.util.Set;

/**
 * 文件类型校验策略。
 *
 * @author Ateng
 * @since 2026-04-29
 */
public final class FileTypePolicy {

    private final Set<String> allowMimeTypes;
    private final Set<String> denyMimeTypes;
    private final Set<String> allowExtensions;
    private final Set<String> denyExtensions;
    private final Set<FileTypeCategory> allowCategories;
    private final Set<FileTypeCategory> denyCategories;
    private final boolean checkExtensionMatch;
    private final boolean rejectUnknownType;
    private final boolean rejectDangerousType;
    private final boolean allowWildcardMimeType;

    private FileTypePolicy(Builder builder) {
        this.allowMimeTypes = normalizeMimeSet(builder.allowMimeTypes);
        this.denyMimeTypes = normalizeMimeSet(builder.denyMimeTypes);
        this.allowExtensions = normalizeExtensionSet(builder.allowExtensions);
        this.denyExtensions = normalizeExtensionSet(builder.denyExtensions);
        this.allowCategories = normalizeCategorySet(builder.allowCategories);
        this.denyCategories = normalizeCategorySet(builder.denyCategories);
        this.checkExtensionMatch = builder.checkExtensionMatch;
        this.rejectUnknownType = builder.rejectUnknownType;
        this.rejectDangerousType = builder.rejectDangerousType;
        this.allowWildcardMimeType = builder.allowWildcardMimeType;
    }

    /**
     * 创建策略构建器。
     *
     * @return 策略构建器
     */
    public static Builder builder() {
        return new Builder();
    }

    /**
     * 创建默认策略。
     *
     * @return 默认策略
     */
    public static FileTypePolicy defaults() {
        return builder().build();
    }

    /**
     * 获取允许的 MIME 类型集合。
     *
     * @return 允许的 MIME 类型集合
     */
    public Set<String> getAllowMimeTypes() {
        return allowMimeTypes;
    }

    /**
     * 获取禁止的 MIME 类型集合。
     *
     * @return 禁止的 MIME 类型集合
     */
    public Set<String> getDenyMimeTypes() {
        return denyMimeTypes;
    }

    /**
     * 获取允许的扩展名集合。
     *
     * @return 允许的扩展名集合
     */
    public Set<String> getAllowExtensions() {
        return allowExtensions;
    }

    /**
     * 获取禁止的扩展名集合。
     *
     * @return 禁止的扩展名集合
     */
    public Set<String> getDenyExtensions() {
        return denyExtensions;
    }

    /**
     * 获取允许的文件分类集合。
     *
     * @return 允许的文件分类集合
     */
    public Set<FileTypeCategory> getAllowCategories() {
        return allowCategories;
    }

    /**
     * 获取禁止的文件分类集合。
     *
     * @return 禁止的文件分类集合
     */
    public Set<FileTypeCategory> getDenyCategories() {
        return denyCategories;
    }

    /**
     * 判断是否校验扩展名和 MIME 类型匹配关系。
     *
     * @return true 表示校验
     */
    public boolean isCheckExtensionMatch() {
        return checkExtensionMatch;
    }

    /**
     * 判断是否拒绝未知类型。
     *
     * @return true 表示拒绝未知类型
     */
    public boolean isRejectUnknownType() {
        return rejectUnknownType;
    }

    /**
     * 判断是否拒绝危险类型。
     *
     * @return true 表示拒绝危险类型
     */
    public boolean isRejectDangerousType() {
        return rejectDangerousType;
    }

    /**
     * 判断是否允许 MIME 类型通配符。
     *
     * @return true 表示允许通配符
     */
    public boolean isAllowWildcardMimeType() {
        return allowWildcardMimeType;
    }

    private static Set<String> normalizeMimeSet(Collection<String> values) {
        if (values == null || values.isEmpty()) {
            return Set.of();
        }
        Set<String> result = new LinkedHashSet<>();
        for (String value : values) {
            String normalized = FileTypeUtil.normalizeMimeType(value);
            if (!normalized.isEmpty()) {
                result.add(normalized);
            }
        }
        return Collections.unmodifiableSet(result);
    }

    private static Set<String> normalizeExtensionSet(Collection<String> values) {
        if (values == null || values.isEmpty()) {
            return Set.of();
        }
        Set<String> result = new LinkedHashSet<>();
        for (String value : values) {
            String normalized = FileTypeUtil.normalizeExtension(value);
            if (!normalized.isEmpty()) {
                result.add(normalized);
            }
        }
        return Collections.unmodifiableSet(result);
    }

    private static Set<FileTypeCategory> normalizeCategorySet(Collection<FileTypeCategory> values) {
        if (values == null || values.isEmpty()) {
            return Set.of();
        }
        EnumSet<FileTypeCategory> result = EnumSet.noneOf(FileTypeCategory.class);
        for (FileTypeCategory value : values) {
            if (value != null) {
                result.add(value);
            }
        }
        return Collections.unmodifiableSet(result);
    }

    /**
     * 文件类型校验策略构建器。
     *
     * @author Ateng
     * @since 2026-04-29
     */
    public static final class Builder {

        private Collection<String> allowMimeTypes = Set.of();
        private Collection<String> denyMimeTypes = Set.of();
        private Collection<String> allowExtensions = Set.of();
        private Collection<String> denyExtensions = Set.of();
        private Collection<FileTypeCategory> allowCategories = Set.of();
        private Collection<FileTypeCategory> denyCategories = Set.of();
        private boolean checkExtensionMatch = true;
        private boolean rejectUnknownType = true;
        private boolean rejectDangerousType = true;
        private boolean allowWildcardMimeType = true;

        private Builder() {
        }

        /**
         * 设置允许的 MIME 类型。
         *
         * @param allowMimeTypes 允许的 MIME 类型
         * @return 当前构建器
         */
        public Builder allowMimeTypes(Collection<String> allowMimeTypes) {
            this.allowMimeTypes = allowMimeTypes;
            return this;
        }

        /**
         * 设置禁止的 MIME 类型。
         *
         * @param denyMimeTypes 禁止的 MIME 类型
         * @return 当前构建器
         */
        public Builder denyMimeTypes(Collection<String> denyMimeTypes) {
            this.denyMimeTypes = denyMimeTypes;
            return this;
        }

        /**
         * 设置允许的扩展名。
         *
         * @param allowExtensions 允许的扩展名
         * @return 当前构建器
         */
        public Builder allowExtensions(Collection<String> allowExtensions) {
            this.allowExtensions = allowExtensions;
            return this;
        }

        /**
         * 设置禁止的扩展名。
         *
         * @param denyExtensions 禁止的扩展名
         * @return 当前构建器
         */
        public Builder denyExtensions(Collection<String> denyExtensions) {
            this.denyExtensions = denyExtensions;
            return this;
        }

        /**
         * 设置允许的文件分类。
         *
         * @param allowCategories 允许的文件分类
         * @return 当前构建器
         */
        public Builder allowCategories(Collection<FileTypeCategory> allowCategories) {
            this.allowCategories = allowCategories;
            return this;
        }

        /**
         * 设置禁止的文件分类。
         *
         * @param denyCategories 禁止的文件分类
         * @return 当前构建器
         */
        public Builder denyCategories(Collection<FileTypeCategory> denyCategories) {
            this.denyCategories = denyCategories;
            return this;
        }

        /**
         * 设置是否校验扩展名和 MIME 类型匹配关系。
         *
         * @param checkExtensionMatch true 表示校验
         * @return 当前构建器
         */
        public Builder checkExtensionMatch(boolean checkExtensionMatch) {
            this.checkExtensionMatch = checkExtensionMatch;
            return this;
        }

        /**
         * 设置是否拒绝未知类型。
         *
         * @param rejectUnknownType true 表示拒绝未知类型
         * @return 当前构建器
         */
        public Builder rejectUnknownType(boolean rejectUnknownType) {
            this.rejectUnknownType = rejectUnknownType;
            return this;
        }

        /**
         * 设置是否拒绝危险类型。
         *
         * @param rejectDangerousType true 表示拒绝危险类型
         * @return 当前构建器
         */
        public Builder rejectDangerousType(boolean rejectDangerousType) {
            this.rejectDangerousType = rejectDangerousType;
            return this;
        }

        /**
         * 设置是否允许 MIME 类型通配符。
         *
         * @param allowWildcardMimeType true 表示允许通配符
         * @return 当前构建器
         */
        public Builder allowWildcardMimeType(boolean allowWildcardMimeType) {
            this.allowWildcardMimeType = allowWildcardMimeType;
            return this;
        }

        /**
         * 构建文件类型校验策略。
         *
         * @return 文件类型校验策略
         */
        public FileTypePolicy build() {
            return new FileTypePolicy(this);
        }
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof FileTypePolicy that)) {
            return false;
        }
        return checkExtensionMatch == that.checkExtensionMatch
                && rejectUnknownType == that.rejectUnknownType
                && rejectDangerousType == that.rejectDangerousType
                && allowWildcardMimeType == that.allowWildcardMimeType
                && Objects.equals(allowMimeTypes, that.allowMimeTypes)
                && Objects.equals(denyMimeTypes, that.denyMimeTypes)
                && Objects.equals(allowExtensions, that.allowExtensions)
                && Objects.equals(denyExtensions, that.denyExtensions)
                && Objects.equals(allowCategories, that.allowCategories)
                && Objects.equals(denyCategories, that.denyCategories);
    }

    @Override
    public int hashCode() {
        return Objects.hash(allowMimeTypes, denyMimeTypes, allowExtensions, denyExtensions, allowCategories,
                denyCategories, checkExtensionMatch, rejectUnknownType, rejectDangerousType, allowWildcardMimeType);
    }

    @Override
    public String toString() {
        return "FileTypePolicy{" +
                "allowMimeTypes=" + allowMimeTypes +
                ", denyMimeTypes=" + denyMimeTypes +
                ", allowExtensions=" + allowExtensions +
                ", denyExtensions=" + denyExtensions +
                ", allowCategories=" + allowCategories +
                ", denyCategories=" + denyCategories +
                ", checkExtensionMatch=" + checkExtensionMatch +
                ", rejectUnknownType=" + rejectUnknownType +
                ", rejectDangerousType=" + rejectDangerousType +
                ", allowWildcardMimeType=" + allowWildcardMimeType +
                '}';
    }
}
