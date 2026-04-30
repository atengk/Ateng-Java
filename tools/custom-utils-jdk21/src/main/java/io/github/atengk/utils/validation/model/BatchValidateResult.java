package io.github.atengk.utils.validation.model;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

/**
 * 批量校验结果。
 *
 * @author Ateng
 * @since 2026-04-30
 */
public final class BatchValidateResult {

    private final boolean valid;

    private final int total;

    private final List<BatchValidateError> errors;

    /**
     * 创建批量校验结果。
     *
     * @param total 总数量
     * @param errors 批量错误信息列表
     */
    public BatchValidateResult(int total, List<BatchValidateError> errors) {
        if (total < 0) {
            throw new IllegalArgumentException("总数量不能小于 0");
        }
        List<BatchValidateError> safeErrors = errors == null ? List.of() : errors.stream().filter(Objects::nonNull).toList();
        this.total = total;
        this.errors = Collections.unmodifiableList(new ArrayList<>(safeErrors));
        this.valid = safeErrors.isEmpty();
    }

    /**
     * 创建批量校验结果。
     *
     * @param total 总数量
     * @param errors 批量错误信息列表
     * @return 批量校验结果
     */
    public static BatchValidateResult of(int total, List<BatchValidateError> errors) {
        return new BatchValidateResult(total, errors);
    }

    /**
     * 判断是否全部校验通过。
     *
     * @return 全部通过返回 true，否则返回 false
     */
    public boolean isValid() {
        return valid;
    }

    /**
     * 判断是否存在错误。
     *
     * @return 存在错误返回 true，否则返回 false
     */
    public boolean hasErrors() {
        return !errors.isEmpty();
    }

    /**
     * 获取总数量。
     *
     * @return 总数量
     */
    public int getTotal() {
        return total;
    }

    /**
     * 获取错误数量。
     *
     * @return 错误数量
     */
    public int getErrorCount() {
        return errors.size();
    }

    /**
     * 获取批量错误信息列表。
     *
     * @return 不可变批量错误信息列表
     */
    public List<BatchValidateError> getErrors() {
        return errors;
    }

    /**
     * 获取第一条错误消息。
     *
     * @return 第一条错误消息
     */
    public Optional<String> getFirstMessage() {
        return errors.stream().map(BatchValidateError::toString).findFirst();
    }

    /**
     * 返回结果摘要。
     *
     * @return 结果摘要
     */
    @Override
    public String toString() {
        return "BatchValidateResult{valid=" + valid + ", total=" + total + ", errors=" + errors + '}';
    }

    /**
     * 判断结果是否相同。
     *
     * @param obj 比较对象
     * @return 相同返回 true，否则返回 false
     */
    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (!(obj instanceof BatchValidateResult that)) {
            return false;
        }
        return valid == that.valid && total == that.total && Objects.equals(errors, that.errors);
    }

    /**
     * 获取哈希值。
     *
     * @return 哈希值
     */
    @Override
    public int hashCode() {
        return Objects.hash(valid, total, errors);
    }
}
