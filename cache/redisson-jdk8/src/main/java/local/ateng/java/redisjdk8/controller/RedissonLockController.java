package local.ateng.java.redisjdk8.controller;


import local.ateng.java.redisjdk8.service.RedissonService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.concurrent.TimeUnit;

/**
 * 分布式锁使用示例 Controller
 * 展示 RedissonService 中所有方法的实际使用方式。
 */
@RestController
@RequestMapping("/lock-demo")
public class RedissonLockController {

    private final RedissonService redissonService;

    public RedissonLockController(RedissonService redissonService) {
        this.redissonService = redissonService;
    }

    /**
     * 示例：阻塞式获取锁（lock）
     */
    @GetMapping("/lock")
    public String lock(@RequestParam String key, @RequestParam Long sleeptime) {
        redissonService.lock(key);
        try {
            Thread.sleep(sleeptime);
            return "锁已获取，业务执行完成";
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return "执行被中断";
        } finally {
            redissonService.unlock(key);
        }
    }

    /**
     * 示例：阻塞式获取锁并设置自动释放时间（lock with leaseTime）
     */
    @GetMapping("/lock-lease")
    public String lockWithLease(@RequestParam String key, @RequestParam Long sleeptime, @RequestParam long leaseTime) {
        redissonService.lock(key, leaseTime);
        try {
            Thread.sleep(sleeptime);
            return "已获取锁，自动释放时间：" + leaseTime + " 秒";
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return "执行被中断";
        } finally {
            redissonService.unlock(key);
        }
    }

    /**
     * 示例：尝试获取锁（tryLock）
     */
    @GetMapping("/try-lock")
    public String tryLock(
            @RequestParam String key,
            @RequestParam Long sleeptime,
            @RequestParam long wait,
            @RequestParam long lease
    ) {
        boolean locked = redissonService.tryLock(key, wait, lease, TimeUnit.SECONDS);
        if (!locked) {
            return "未获取到锁：请稍后重试";
        }
        try {
            Thread.sleep(sleeptime);
            return "tryLock 成功，执行完成";
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return "执行被中断";
        } finally {
            redissonService.unlock(key);
        }
    }

    /**
     * 示例：释放锁（unlock）
     */
    @GetMapping("/unlock")
    public String unlock(@RequestParam String key) {
        redissonService.unlock(key);
        return "unlock 调用完成";
    }

    /**
     * 示例：判断当前线程是否持有指定锁
     */
    @GetMapping("/held-by-me")
    public String heldByMe(@RequestParam String key) {
        boolean held = redissonService.isHeldByCurrentThread(key);
        return held ? "当前线程持有锁" : "当前线程未持有锁";
    }

    /**
     * 示例：判断锁是否被任意线程持有
     */
    @GetMapping("/is-locked")
    public String isLocked(@RequestParam String key) {
        boolean locked = redissonService.isLocked(key);
        return locked ? "锁正在被占用" : "锁未被占用";
    }

    /**
     * 示例：读锁（阻塞式）
     */
    @GetMapping("/read-lock")
    public String readLock(@RequestParam String key) {
        redissonService.readLock(key);
        try {
            return "已获取读锁";
        } finally {
            redissonService.unlockRead(key);
        }
    }

    /**
     * 示例：写锁（阻塞式）
     */
    @GetMapping("/write-lock")
    public String writeLock(@RequestParam String key) {
        redissonService.writeLock(key);
        try {
            return "已获取写锁";
        } finally {
            redissonService.unlockWrite(key);
        }
    }

    /**
     * 示例：尝试获取读锁（tryReadLock）
     */
    @GetMapping("/try-read-lock")
    public String tryReadLock(
            @RequestParam String key,
            @RequestParam long wait,
            @RequestParam long lease
    ) {
        boolean locked = redissonService.tryReadLock(key, wait, lease, TimeUnit.SECONDS);
        if (!locked) {
            return "未获取到读锁";
        }
        try {
            return "成功获取读锁";
        } finally {
            redissonService.unlockRead(key);
        }
    }

    /**
     * 示例：尝试获取写锁（tryWriteLock）
     */
    @GetMapping("/try-write-lock")
    public String tryWriteLock(
            @RequestParam String key,
            @RequestParam long wait,
            @RequestParam long lease
    ) {
        boolean locked = redissonService.tryWriteLock(key, wait, lease, TimeUnit.SECONDS);
        if (!locked) {
            return "未获取到写锁";
        }
        try {
            return "成功获取写锁";
        } finally {
            redissonService.unlockWrite(key);
        }
    }

    /**
     * 示例：释放读锁
     */
    @GetMapping("/unlock-read")
    public String unlockRead(@RequestParam String key) {
        redissonService.unlockRead(key);
        return "读锁已释放";
    }

    /**
     * 示例：释放写锁
     */
    @GetMapping("/unlock-write")
    public String unlockWrite(@RequestParam String key) {
        redissonService.unlockWrite(key);
        return "写锁已释放";
    }
}