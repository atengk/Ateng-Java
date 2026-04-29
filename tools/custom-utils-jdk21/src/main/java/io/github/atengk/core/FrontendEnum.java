package io.github.atengk.core;

import java.util.Collections;
import java.util.Map;

/**
 * 可暴露给前端的枚举契约。
 *
 * @param <C> 编码类型
 * @author Ateng
 * @since 2026-04-29
 */
public interface FrontendEnum<C> extends DictEnum<C> {

    /**
     * 获取前端扩展字段。
     *
     * @return 扩展字段映射
     */
    default Map<String, Object> getExtra() {
        return Collections.emptyMap();
    }
}
