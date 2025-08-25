package local.ateng.java.mybatisjdk8.config;

import com.baomidou.mybatisplus.annotation.DbType;
import com.baomidou.mybatisplus.autoconfigure.ConfigurationCustomizer;
import com.baomidou.mybatisplus.extension.plugins.MybatisPlusInterceptor;
import com.baomidou.mybatisplus.extension.plugins.inner.PaginationInnerInterceptor;
import local.ateng.java.mybatisjdk8.handler.GeometryTypeHandler;
import local.ateng.java.mybatisjdk8.handler.JacksonTypeHandler;
import local.ateng.java.mybatisjdk8.handler.UUIDTypeHandler;
import local.ateng.java.mybatisjdk8.interceptor.MyCustomInterceptor;
import org.mybatis.spring.annotation.MapperScan;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@MapperScan("local.ateng.java.mybatisjdk8.**.mapper")
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
            configuration.getTypeHandlerRegistry().register(GeometryTypeHandler.class);
            configuration.getTypeHandlerRegistry().register(JacksonTypeHandler.class);
            configuration.getTypeHandlerRegistry().register(UUIDTypeHandler.class);

            // 方式二：包扫描注册，推荐用于统一管理 TypeHandler
            // configuration.getTypeHandlerRegistry()
            //         .register("com.example.mybatis.handler");
        };
    }

    /**
     * 注册 MyBatis-Plus 拦截器。
     * <p>
     * 该拦截器支持分页、乐观锁、防全表更新删除等功能。
     * 当前仅启用分页插件。
     * </p>
     * <p>
     * ⚠️ 注意：MyBatis-Plus 3.5 中，多个 InnerInterceptor 的执行顺序
     * 与注册顺序一致。分页插件会修改 SQL 以实现分页功能，
     * 因此建议分页插件 **最后注册**，以保证其他自定义或内置插件
     * 能在原始 SQL 上执行操作，避免分页逻辑被提前应用导致异常。
     * </p>
     *
     * @return MybatisPlusInterceptor 拦截器实例
     */
    @Bean
    public MybatisPlusInterceptor mybatisPlusInterceptor() {
        MybatisPlusInterceptor interceptor = new MybatisPlusInterceptor();

        // 自定义插件（先注册）
        interceptor.addInnerInterceptor(new MyCustomInterceptor());
//        interceptor.addInnerInterceptor(new SqlPrintInterceptor());


        // 分页插件配置
        PaginationInnerInterceptor paginationInterceptor = new PaginationInnerInterceptor();
        // 设置数据库类型（推荐明确指定，避免推断错误）
        paginationInterceptor.setDbType(DbType.MYSQL);
        // 溢出总页数后是否进行处理，true 返回首页，false 继续请求
        paginationInterceptor.setOverflow(false);
        // 单页最大记录数，-1 表示不受限制
        paginationInterceptor.setMaxLimit(1000L);

        interceptor.addInnerInterceptor(paginationInterceptor);
        return interceptor;
    }

}
