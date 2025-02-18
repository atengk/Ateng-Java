package local.ateng.java.jetcache.remote;

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
public class JetcacheRemoteConfig {
    private final CacheManager cacheManager;
    private Cache<Integer, String> manualRemoteCache;

    @PostConstruct
    public void init() {
        QuickConfig qc = QuickConfig.newBuilder("manualRemoteCache:")
                .expire(Duration.ofSeconds(3600))
                .cacheType(CacheType.REMOTE)
                .build();
        manualRemoteCache = cacheManager.getOrCreateCache(qc);
    }

    @Bean
    public Cache<Integer, String> manualRemoteCache() {
        return manualRemoteCache;
    }

}
