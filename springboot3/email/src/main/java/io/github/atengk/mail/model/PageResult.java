package io.github.atengk.mail.model;

import java.util.Collections;
import java.util.List;

/**
 * 分页结果对象
 *
 * @author Ateng
 * @since 2026-04-30
 */
public record PageResult<T>(
        Long total,
        Long pageNum,
        Long pageSize,
        List<T> records
) {

    /**
     * 返回空分页结果
     *
     * @param pageNum  当前页
     * @param pageSize 每页条数
     * @return 分页结果
     */
    public static <T> PageResult<T> empty(Long pageNum, Long pageSize) {
        return new PageResult<>(0L, pageNum, pageSize, Collections.emptyList());
    }

    /**
     * 返回分页结果
     *
     * @param total    总条数
     * @param pageNum  当前页
     * @param pageSize 每页条数
     * @param records  数据列表
     * @return 分页结果
     */
    public static <T> PageResult<T> of(Long total, Long pageNum, Long pageSize, List<T> records) {
        return new PageResult<>(total, pageNum, pageSize, records == null ? Collections.emptyList() : records);
    }
}