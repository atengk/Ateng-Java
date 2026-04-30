package io.github.atengk.utils.spring;

import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.stereotype.Component;

/**
 * SpringUtil 初始化组件。
 *
 * @author Ateng
 * @since 2026-04-30
 */
@Component
public class SpringUtilInitializer implements ApplicationContextAware {

    /**
     * 设置 Spring 应用上下文。
     *
     * @param applicationContext Spring 应用上下文
     */
    @Override
    public void setApplicationContext(ApplicationContext applicationContext) {
        SpringUtil.setApplicationContext(applicationContext);
    }

}