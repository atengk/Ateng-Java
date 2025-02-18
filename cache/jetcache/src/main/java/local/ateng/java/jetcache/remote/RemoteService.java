package local.ateng.java.jetcache.remote;

import com.alicp.jetcache.anno.CacheInvalidate;
import com.alicp.jetcache.anno.CacheType;
import com.alicp.jetcache.anno.CacheUpdate;
import com.alicp.jetcache.anno.Cached;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.concurrent.TimeUnit;

@Service
@Slf4j
public class RemoteService {

    // 使用 @Cached 注解缓存方法的返回值
    @Cached(cacheType = CacheType.REMOTE, name = "my:cache", key = "#id", expire = 2, timeUnit = TimeUnit.MINUTES)
    public String getDataById(int id) {
        // 模拟从数据库或其他数据源获取数据
        String data = "Data for id " + id;
        log.info(data);
        return data;
    }

    // 使用 @CacheUpdate 注解更新缓存
    @CacheUpdate(name = "my:cache", key = "#id", value = "#result")
    public String updateData(int id, String newData) {
        // 模拟更新数据源
        String data = "newData " + newData + " for id " + id;
        log.info(data);
        return data;
    }

    // 使用 @CacheInvalidate 注解删除缓存
    @CacheInvalidate(name = "my:cache", key = "#id")
    public void deleteData(int id) {
        // 模拟删除数据源
        String data = "Data for id " + id;
        log.info(data);
    }

}
