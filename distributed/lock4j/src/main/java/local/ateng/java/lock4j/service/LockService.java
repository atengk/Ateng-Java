package local.ateng.java.lock4j.service;

import com.baomidou.lock.LockInfo;
import com.baomidou.lock.LockTemplate;
import com.baomidou.lock.annotation.Lock4j;
import local.ateng.java.lock4j.entity.MyUser;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.concurrent.atomic.AtomicInteger;

@Service
@Slf4j
@RequiredArgsConstructor(onConstructor = @__(@Autowired))
public class LockService {
    private final LockTemplate lockTemplate;
    private AtomicInteger counter = new AtomicInteger(0);

    @Lock4j
    public void simple() {
        try {
            log.info("Task is being processed...");

            // 这里模拟任务的执行，假设我们有一些需要在分布式环境下确保单一实例执行的操作
            Thread.sleep(5000);  // 模拟耗时操作

            log.info("Task finished.");
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
    }

    // 完全配置，支持spel
    @Lock4j(keys = {"#myUser.id", "#myUser.name"}, expire = 60000, acquireTimeout = 1000)
    public MyUser customMethod(MyUser myUser) {
        try {
            Thread.sleep(1000);  // 模拟耗时操作
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
        return myUser;
    }

    /**
     * 手动上锁解锁
     *
     * @param userId
     */
    public void programmaticLock(String userId) {
        // 各种查询操作 不上锁
        // ...
        // 获取锁
        final LockInfo lockInfo = lockTemplate.lock(userId, 30000L, 5000L);
        if (null == lockInfo) {
            throw new RuntimeException("业务处理中,请稍后再试");
        }
        // 获取锁成功，处理业务
        try {
            System.out.println("执行简单方法1 , 当前线程:" + Thread.currentThread().getName() + " , counter：" + counter.addAndGet(1));
        } finally {
            //释放锁
            lockTemplate.releaseLock(lockInfo);
        }
        //结束
    }

    /**
     * 指定时间内不释放锁(限流)
     * 用户在5秒内只能访问1次
     *
     * @return
     */
    @Lock4j(keys = {"#myUser.id"}, acquireTimeout = 0, expire = 5000, autoRelease = false)
    public MyUser limit(MyUser myUser) {
        return myUser;
    }

}

