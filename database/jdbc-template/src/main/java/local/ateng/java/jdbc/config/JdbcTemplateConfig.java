package local.ateng.java.jdbc.config;


import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.jdbc.core.JdbcTemplate;

import javax.sql.DataSource;

/**
 * 多 JdbcTemplate 配置类
 * 通过 @Qualifier 精准绑定不同的数据源。
 */
@Configuration
public class JdbcTemplateConfig {

    /**
     * mysql JdbcTemplate
     *
     * @param dataSource 主数据源
     * @return JdbcTemplate
     */
    @Bean(name = "mysqlJdbcTemplate")
    public JdbcTemplate mysqlJdbcTemplate(
            @Qualifier("mysqlDataSource") DataSource dataSource) {
        return new JdbcTemplate(dataSource);
    }

    /**
     * postgresql JdbcTemplate
     *
     * @param dataSource 次数据源
     * @return JdbcTemplate
     */
    @Bean(name = "postgresqlJdbcTemplate")
    public JdbcTemplate postgresqlJdbcTemplate(
            @Qualifier("postgresqlDataSource") DataSource dataSource) {
        return new JdbcTemplate(dataSource);
    }
}

