package local.ateng.java.aop.aspect;


import cn.hutool.core.util.RandomUtil;
import cn.hutool.core.util.StrUtil;
import jakarta.servlet.http.HttpServletRequest;
import local.ateng.java.aop.annotation.Debounce;
import lombok.RequiredArgsConstructor;
import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.annotation.AfterReturning;
import org.aspectj.lang.annotation.AfterThrowing;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Before;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.DefaultParameterNameDiscoverer;
import org.springframework.core.ParameterNameDiscoverer;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.expression.EvaluationContext;
import org.springframework.expression.Expression;
import org.springframework.expression.spel.standard.SpelExpressionParser;
import org.springframework.expression.spel.support.StandardEvaluationContext;
import org.springframework.stereotype.Component;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * AOP切面，用于处理防抖逻辑。
 */
@Aspect
@Component
@RequiredArgsConstructor(onConstructor = @__(@Autowired))
public class DebounceAspect {
    private static final ThreadLocal<String> KEY = new ThreadLocal<>();
    private final StringRedisTemplate redisTemplate;
    private final HttpServletRequest request;
    private final SpelExpressionParser parser = new SpelExpressionParser();
    private final ParameterNameDiscoverer nameDiscoverer = new DefaultParameterNameDiscoverer();

    /**
     * 方法执行前
     * 首先判断Redis的数据是否存在，存在就抛出错误，不存在则继续
     * 将用户的接口访问信息存储到Redis，并设置TTL
     *
     * @param joinPoint
     * @param debounce
     */
    @Before("@annotation(debounce)")
    public void before(JoinPoint joinPoint, Debounce debounce) {
        // 获取访问的接口
        String uri = request.getRequestURI();
        // 获取用户名
        String username = getUsername(request);
        // 写入Redis并设置TTL
        String key = "ateng:debounce:interface:" + uri + ":" + username;
        // 获取 keys
        List<String> keyList = getKeyList(joinPoint, debounce);
        if (keyList != null && keyList.size() > 0) {
            key = StrUtil.format("{}:{}", key, String.join(":", keyList));
        }
        Boolean isExist = redisTemplate.opsForValue().setIfAbsent(key, "", debounce.interval(), debounce.timeUnit());
        if (!isExist) {
            throw new RuntimeException(debounce.message());
        }
        KEY.set(key);
    }

    /**
     * 方法正常返回后
     *
     * @param joinPoint
     * @param debounce
     */
    @AfterReturning(pointcut = "@annotation(debounce)", returning = "result")
    public void afterReturning(JoinPoint joinPoint, Debounce debounce, Object result) {
        KEY.remove();
    }

    /**
     * 方法异常后
     *
     * @param joinPoint
     * @param debounce
     */
    @AfterThrowing(pointcut = "@annotation(debounce)", throwing = "e")
    public void afterThrowing(JoinPoint joinPoint, Debounce debounce, Exception e) {
        KEY.remove();
    }

    /**
     * 获取当前请求所属用户
     */
    private String getUsername(HttpServletRequest request) {
        String username = null;
        // 根据实际项目逻辑获取用户名
        username = RandomUtil.randomEle(Arrays.asList("ateng", "kongyu"));
        return username;
    }

    /**
     * 获取注解中 keys 的数据列表
     *
     * @param joinPoint JoinPoint
     * @param debounce  Debounce 注解
     * @return keys 数据列表
     */
    private List<String> getKeyList(JoinPoint joinPoint, Debounce debounce) {
        MethodSignature signature = (MethodSignature) joinPoint.getSignature();
        Method method = signature.getMethod();
        Object[] args = joinPoint.getArgs();
        String[] paramNames = nameDiscoverer.getParameterNames(method);

        EvaluationContext context = new StandardEvaluationContext();
        for (int i = 0; i < args.length; i++) {
            context.setVariable(paramNames[i], args[i]);
        }

        List<String> keyList = new ArrayList<>();
        for (String key : debounce.keys()) {
            Expression expression = parser.parseExpression(key);
            Object value = expression.getValue(context);
            keyList.add(value != null ? value.toString() : "null");
        }
        return keyList;
    }

}
