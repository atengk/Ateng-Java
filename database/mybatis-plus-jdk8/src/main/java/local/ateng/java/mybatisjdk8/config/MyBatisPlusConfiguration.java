package local.ateng.java.mybatisjdk8.config;

import com.baomidou.mybatisplus.annotation.DbType;
import com.baomidou.mybatisplus.autoconfigure.ConfigurationCustomizer;
import com.baomidou.mybatisplus.extension.plugins.MybatisPlusInterceptor;
import com.baomidou.mybatisplus.extension.plugins.inner.PaginationInnerInterceptor;
import local.ateng.java.mybatisjdk8.handler.GeometryTypeHandler;
import local.ateng.java.mybatisjdk8.handler.JacksonTypeHandler;
import local.ateng.java.mybatisjdk8.handler.UUIDTypeHandler;
import org.mybatis.spring.annotation.MapperScan;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@MapperScan("local.ateng.java.mybatisjdk8.**.mapper")
public class MyBatisPlusConfiguration {

    @Bean
    public ConfigurationCustomizer configurationCustomizer() {
        return configuration -> {
            // 方式一：逐个注册
            configuration.getTypeHandlerRegistry().register(GeometryTypeHandler.class);
            configuration.getTypeHandlerRegistry().register(JacksonTypeHandler.class);
            configuration.getTypeHandlerRegistry().register(UUIDTypeHandler.class);

            // 方式二：包扫描（推荐）
            //configuration.getTypeHandlerRegistry().register("local.ateng.java.mybatisjdk8.handler");
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
