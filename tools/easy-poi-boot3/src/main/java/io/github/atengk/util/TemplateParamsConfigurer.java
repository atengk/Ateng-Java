package io.github.atengk.util;

import cn.afterturn.easypoi.excel.entity.TemplateExportParams;

/**
 * Excel 模板导出参数配置回调接口
 *
 * @author 孔余
 * @since 2026-01-22
 */
@FunctionalInterface
public interface TemplateParamsConfigurer {

    /**
     * 对 EasyPOI 的 {@link TemplateExportParams} 进行个性化配置
     *
     * @param params EasyPOI 模板导出参数对象
     */
    void configure(TemplateExportParams params);
}
