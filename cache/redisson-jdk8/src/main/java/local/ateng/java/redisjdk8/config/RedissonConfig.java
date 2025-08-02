package local.ateng.java.redisjdk8.config;

import lombok.RequiredArgsConstructor;
import org.redisson.Redisson;
import org.redisson.api.RedissonClient;
import org.redisson.config.Config;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.io.IOException;

@Configuration
@RequiredArgsConstructor(onConstructor = @__(@Autowired))
public class RedissonConfig {
    private final RedissonProperties redissonProperties;

    @Bean
    public RedissonClient redissonClient() throws IOException {
        Config config = Config.fromYAML(redissonProperties.getConfig());
//        config.setCodec(new CustomJacksonCodec());
        return Redisson.create(config);
    }

}
