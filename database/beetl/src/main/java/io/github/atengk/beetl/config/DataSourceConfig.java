package io.github.atengk.beetl.config;

import com.zaxxer.hikari.HikariDataSource;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import javax.sql.DataSource;

/**
 * 多数据源配置类
 * 用于创建主从两个 DataSource。
 */
@Configuration
public class DataSourceConfig {

    /**
     * MySQL 数据源
     *
     * @return DataSource
     */
    @Bean(name = "mysqlDataSource")
    @ConfigurationProperties(prefix = "spring.datasource.mysql")
    public DataSource mysqlDataSource() {
        return new HikariDataSource();
    }

    /**
     * PostgreSQL 数据源
     *
     * @return DataSource
     */
    @Bean(name = "postgresqlDataSource")
    @ConfigurationProperties(prefix = "spring.datasource.postgresql")
    public DataSource postgresqlDataSource() {
        return new HikariDataSource();
    }
}


