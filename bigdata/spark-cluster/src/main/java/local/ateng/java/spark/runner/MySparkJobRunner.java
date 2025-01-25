package local.ateng.java.spark.runner;

import cn.hutool.extra.spring.SpringUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.context.ApplicationContext;
import org.springframework.stereotype.Component;
import org.springframework.util.ObjectUtils;

import java.lang.reflect.Method;
import java.util.List;

/**
 * 运行Spark任务
 *
 * @author 孔余
 * @email 2385569970@qq.com
 * @since 2025-01-25
 */
@Component
@RequiredArgsConstructor(onConstructor = @__(@Autowired))
public class MySparkJobRunner implements ApplicationRunner {
    private final ApplicationContext applicationContext;

    @Override
    public void run(ApplicationArguments args) throws Exception {
        // 获取需要运行的任务名称
        List<String> classArgs = args.getOptionValues("class");
        List<String> methodArgs = args.getOptionValues("method");
        if (ObjectUtils.isEmpty(classArgs) || ObjectUtils.isEmpty(methodArgs)) {
            System.out.println("请提供参数：--class=xxx --method=xxx");
            System.out.println("例如：--class=local.ateng.java.spark.sql.SQLCount --method=run");
            int exitCode = SpringApplication.exit(SpringUtil.getApplicationContext(), () -> 1);
            System.exit(exitCode);
        }
        String className = classArgs.get(0);
        String methodName = methodArgs.get(0);
        System.out.println("运行" + className + "的" + methodName + "方法");
        // 使用反射机制调用指定的类和方法
        Class<?> clazz = Class.forName(className);
        Object instance = applicationContext.getBean(clazz);
        Method method = clazz.getMethod(methodName);
        method.invoke(instance);
    }
}
