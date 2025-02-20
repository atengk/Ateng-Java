package local.ateng.java.aop.constants;

/**
 * 用于定义模块的枚举
 *
 * @author 孔余
 * @email 2385569970@qq.com
 * @since 2025-02-20
 */
public enum Module {
    NULL(null),
    USER("用户模块"),
    ORDER("订单模块"),
    PRODUCT("商品模块");

    private final String description;

    Module(String description) {
        this.description = description;
    }

    public String getDescription() {
        return description;
    }
}
