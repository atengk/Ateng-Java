package local.ateng.java.lock4j.config;


import com.baomidou.lock.LockFailureStrategy;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.lang.reflect.Method;
import java.util.Arrays;

/**
 * 自定义锁获取失败策略
 *
 * @author 孔余
 * @email 2385569970@qq.com
 * @since 2025-02-19
 */
@Component
@Slf4j
public class MyLockFailureStrategy implements LockFailureStrategy {

    @Override
    public void onLockFailure(String key, Method method, Object[] arguments) {
        String msg = String.format("获取锁超时, msg: key=%s,method=%s,arguments=%s", key, method, Arrays.toString(arguments));
        log.error(msg);
        throw new RuntimeException(msg);
    }
}
