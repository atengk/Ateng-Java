package io.github.atengk.beetl.config;

import org.beetl.core.GroupTemplate;
import org.beetl.core.resource.ClasspathResourceLoader;
import org.springframework.context.annotation.Bean;

/**
 * Beetl 模板引擎配置
 *
 * @author 孔余
 * @since 2025-12-18
 */
@org.springframework.context.annotation.Configuration
public class BeetlConfig {

    @Bean
    public GroupTemplate groupTemplate() throws Exception {
        ClasspathResourceLoader loader = new ClasspathResourceLoader();
        org.beetl.core.Configuration configuration =
                org.beetl.core.Configuration.defaultConfiguration();
        return new GroupTemplate(loader, configuration);
    }

}
