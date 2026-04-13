package local.ateng.java.mybatis.config;

import com.baomidou.mybatisplus.annotation.DbType;
import com.baomidou.mybatisplus.autoconfigure.ConfigurationCustomizer;
import com.baomidou.mybatisplus.extension.plugins.MybatisPlusInterceptor;
import com.baomidou.mybatisplus.extension.plugins.inner.PaginationInnerInterceptor;
import local.ateng.java.mybatis.handler.Fastjson2TypeHandler;
import local.ateng.java.mybatis.handler.JacksonTypeHandler;
import org.mybatis.spring.annotation.MapperScan;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@MapperScan("local.ateng.java.mybatis.**.mapper")
public class MyBatisPlusConfiguration {

    /**
     * 自定义 MyBatis 全局配置。
     * <p>
     * 方式一：逐个注册 TypeHandler。
     * 方式二：包扫描注册（推荐）。
     * </p>
     *
     * @return ConfigurationCustomizer 配置定制器
     */
    @Bean
    public ConfigurationCustomizer configurationCustomizer() {
        return configuration -> {
            // 方式一：逐个注册自定义 TypeHandler
            configuration.getTypeHandlerRegistry().register(JacksonTypeHandler.class);

            // 方式二：包扫描注册，推荐用于统一管理 TypeHandler
            // configuration.getTypeHandlerRegistry()
            //         .register("com.example.mybatis.handler");
        };
    }

    /**
     * 添加分页插件
     * https://baomidou.com/plugins/pagination/
     */
    @Bean
    public MybatisPlusInterceptor mybatisPlusInterceptor() {
        MybatisPlusInterceptor interceptor = new MybatisPlusInterceptor();
        interceptor.addInnerInterceptor(new PaginationInnerInterceptor(DbType.MYSQL)); // 如果配置多个插件, 切记分页最后添加
        // 如果有多数据源可以不配具体类型, 否则都建议配上具体的 DbType
        return interceptor;
    }

}
