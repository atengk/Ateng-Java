package local.ateng.java.mybatisjdk8.utils;

import com.baomidou.dynamic.datasource.DynamicRoutingDataSource;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

import javax.sql.DataSource;
import java.sql.SQLException;

/**
 * JdbcTemplate 提供器
 * 用于在动态多数据源环境下，根据数据源名称动态创建对应的 JdbcTemplate 实例。
 * 适用于 P6Spy 装饰后的 DataSource（DecoratedDataSource），必须通过 unwrap 获取动态路由数据源。
 *
 * @author 孔余
 * @since 2025-12-02
 */
@Component
public class JdbcTemplateProvider {

    /**
     * 项目主数据源（可能被 P6Spy 等框架代理或包装）
     */
    private final DataSource primaryDataSource;

    /**
     * 通过构造方法注入数据源
     *
     * @param primaryDataSource 主数据源实例
     */
    public JdbcTemplateProvider(DataSource primaryDataSource) {
        this.primaryDataSource = primaryDataSource;
    }

    /**
     * 根据数据源名称获取对应的 JdbcTemplate。
     * 会自动 unwrap 获取 DynamicRoutingDataSource，避免 P6Spy DecoratedDataSource 导致的类型不匹配问题。
     *
     * @param dataSourceName 数据源名称（与 dynamic-datasource 的配置一致）
     * @return 对应数据源的 JdbcTemplate
     */
    public JdbcTemplate getJdbcTemplate(String dataSourceName) {
        DynamicRoutingDataSource routingDataSource = unwrapDynamicDataSource(primaryDataSource);
        DataSource targetDataSource = routingDataSource.getDataSource(dataSourceName);
        return new JdbcTemplate(targetDataSource);
    }

    /**
     * 从可能被 P6Spy 装饰的 DataSource 中解包出 DynamicRoutingDataSource。
     *
     * @param dataSource 主数据源（被代理或包装）
     * @return DynamicRoutingDataSource 实例
     */
    private DynamicRoutingDataSource unwrapDynamicDataSource(DataSource dataSource) {
        try {
            return dataSource.unwrap(DynamicRoutingDataSource.class);
        } catch (SQLException ex) {
            throw new IllegalStateException("无法从 DataSource 中 unwrap DynamicRoutingDataSource，请检查是否启用了 P6Spy 代理", ex);
        }
    }
}
