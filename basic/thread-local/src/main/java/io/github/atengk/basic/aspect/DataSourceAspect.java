package io.github.atengk.basic.aspect;

import cn.hutool.core.util.StrUtil;
import io.github.atengk.basic.annotation.DS;
import io.github.atengk.basic.holder.DataSourceContextHolder;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.stereotype.Component;

/**
 * 数据源切面
 *
 * @author Ateng
 * @since 2026-04-10
 */
@Aspect
@Component
public class DataSourceAspect {

    @Around("@annotation(ds)")
    public Object around(ProceedingJoinPoint point, DS ds) throws Throwable {

        String dsKey = ds.value();

        try {
            // 设置数据源
            if (StrUtil.isNotBlank(dsKey)) {
                DataSourceContextHolder.set(dsKey);
            }

            return point.proceed();
        } finally {
            // 清理，避免线程复用污染
            DataSourceContextHolder.clear();
        }
    }
}
