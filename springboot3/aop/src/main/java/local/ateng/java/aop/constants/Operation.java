package local.ateng.java.aop.constants;

/**
 * 用于定义操作类型的枚举
 *
 * @author 孔余
 * @email 2385569970@qq.com
 * @since 2025-02-20
 */
public enum Operation {
    NULL(null),
    CREATE("创建"),
    READ("查询"),
    UPDATE("更新"),
    DELETE("删除"),
    EXPORT("导出"),
    IMPORT("导入");

    private final String description;

    Operation(String description) {
        this.description = description;
    }

    public String getDescription() {
        return description;
    }
}
