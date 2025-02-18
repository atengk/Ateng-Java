package local.ateng.java.jetcache.local;

import com.alicp.jetcache.Cache;
import com.alicp.jetcache.CacheManager;
import com.alicp.jetcache.anno.CacheType;
import com.alicp.jetcache.template.QuickConfig;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Duration;

@Configuration
@RequiredArgsConstructor(onConstructor = @__(@Autowired))
public class JetcacheLocalConfig {
    private final CacheManager cacheManager;
    private Cache<Integer, String> manualCache;

    @PostConstruct
    public void init() {
        QuickConfig qc = QuickConfig.newBuilder("manualCache:")
                .expire(Duration.ofSeconds(3600))
                .cacheType(CacheType.LOCAL)
                .build();
        manualCache = cacheManager.getOrCreateCache(qc);
    }

    @Bean
    public Cache<Integer, String> manualCache() {
        return manualCache;
    }

}
