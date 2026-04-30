package io.github.atengk.utils.validation;

/**
 * 常用校验分组。
 *
 * @author Ateng
 * @since 2026-04-30
 */
public final class ValidateGroups {

    private ValidateGroups() {
        throw new UnsupportedOperationException("ValidateGroups 不允许实例化");
    }

    /** 新增校验分组。 */
    public interface Create {
    }

    /** 修改校验分组。 */
    public interface Update {
    }

    /** 删除校验分组。 */
    public interface Delete {
    }

    /** 查询校验分组。 */
    public interface Query {
    }

    /** 详情校验分组。 */
    public interface Detail {
    }

    /** 导入校验分组。 */
    public interface Import {
    }

    /** 导出校验分组。 */
    public interface Export {
    }

    /** 批量操作校验分组。 */
    public interface Batch {
    }

    /** 提交校验分组。 */
    public interface Submit {
    }

    /** 审核校验分组。 */
    public interface Audit {
    }
}
