package io.github.atengk.util;

import cn.afterturn.easypoi.excel.entity.ExportParams;

/**
 * Excel 导出参数配置回调接口
 *
 * @author 孔余
 * @since 2026-01-22
 */
@FunctionalInterface
public interface ExportParamsConfigurer {

    /**
     * 对 EasyPOI 的 {@link ExportParams} 进行个性化配置
     *
     * @param params EasyPOI 导入参数对象
     */
    void configure(ExportParams params);

}
