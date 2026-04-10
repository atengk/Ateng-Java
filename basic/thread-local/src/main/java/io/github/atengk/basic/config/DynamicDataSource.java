package io.github.atengk.basic.config;

import io.github.atengk.basic.holder.DataSourceContextHolder;
import org.springframework.jdbc.datasource.lookup.AbstractRoutingDataSource;

/**
 * 动态数据源路由
 *
 * @author Ateng
 * @since 2026-04-10
 */
public class DynamicDataSource extends AbstractRoutingDataSource {

    /**
     * 决定当前使用哪个数据源
     */
    @Override
    protected Object determineCurrentLookupKey() {
        return DataSourceContextHolder.get();
    }
}
