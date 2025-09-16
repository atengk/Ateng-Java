package local.ateng.java.mybatisjdk8.config;

import org.apache.ibatis.session.SqlSessionFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * 动态 SQL 执行器配置
 *
 * @author 孔余
 * @since 2025-09-16
 */
@Configuration
public class DynamicSqlConfiguration {

    @Bean
    public DynamicSqlExecutor dynamicSqlExecutor(SqlSessionFactory sqlSessionFactory) {
        return new DynamicSqlExecutor(sqlSessionFactory);
    }
}