package io.github.atengk.utils.filetype;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

/**
 * 文件类型校验结果。
 *
 * @author Ateng
 * @since 2026-04-29
 */
public final class FileTypeCheckResult {

    private final boolean passed;
    private final FileTypeInfo fileTypeInfo;
    private final List<String> messages;

    private FileTypeCheckResult(boolean passed, FileTypeInfo fileTypeInfo, List<String> messages) {
        this.passed = passed;
        this.fileTypeInfo = fileTypeInfo;
        this.messages = Collections.unmodifiableList(new ArrayList<>(messages == null ? List.of() : messages));
    }

    /**
     * 创建通过的校验结果。
     *
     * @param fileTypeInfo 文件类型信息
     * @return 校验结果
     */
    public static FileTypeCheckResult passed(FileTypeInfo fileTypeInfo) {
        return new FileTypeCheckResult(true, fileTypeInfo, List.of());
    }

    /**
     * 创建失败的校验结果。
     *
     * @param fileTypeInfo 文件类型信息
     * @param messages 失败消息
     * @return 校验结果
     */
    public static FileTypeCheckResult failed(FileTypeInfo fileTypeInfo, List<String> messages) {
        return new FileTypeCheckResult(false, fileTypeInfo, messages);
    }

    /**
     * 判断校验是否通过。
     *
     * @return true 表示通过
     */
    public boolean isPassed() {
        return passed;
    }

    /**
     * 获取文件类型信息。
     *
     * @return 文件类型信息
     */
    public FileTypeInfo getFileTypeInfo() {
        return fileTypeInfo;
    }

    /**
     * 获取校验消息列表。
     *
     * @return 校验消息列表
     */
    public List<String> getMessages() {
        return messages;
    }

    /**
     * 获取第一条校验消息。
     *
     * @return 第一条校验消息，不存在时返回空字符串
     */
    public String getFirstMessage() {
        return messages.isEmpty() ? "" : messages.getFirst();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof FileTypeCheckResult that)) {
            return false;
        }
        return passed == that.passed
                && Objects.equals(fileTypeInfo, that.fileTypeInfo)
                && Objects.equals(messages, that.messages);
    }

    @Override
    public int hashCode() {
        return Objects.hash(passed, fileTypeInfo, messages);
    }

    @Override
    public String toString() {
        return "FileTypeCheckResult{" +
                "passed=" + passed +
                ", fileTypeInfo=" + fileTypeInfo +
                ", messages=" + messages +
                '}';
    }
}
