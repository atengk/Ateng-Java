package io.github.atengk.sshj.config;

import io.github.atengk.sshj.core.SshClientFactory;
import io.github.atengk.sshj.core.SshClientPool;
import io.github.atengk.sshj.core.SshTemplate;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class SshAutoConfiguration {

    @Bean
    public SshClientFactory sshClientFactory(SshProperties properties) {
        return new SshClientFactory(properties);
    }

    @Bean
    public SshClientPool sshClientPool(SshClientFactory factory, SshProperties properties) {
        return new SshClientPool(factory, properties);
    }

    @Bean
    public SshTemplate sshTemplate(SshClientPool pool) {
        return new SshTemplate(pool);
    }
}