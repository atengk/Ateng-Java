package io.github.atengk.config;

import io.github.atengk.chain.EmailFormatHandler;
import io.github.atengk.chain.UsernameNotEmptyHandler;
import io.github.atengk.chain.UsernameUniqueHandler;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * 责任链配置
 */
@Configuration
public class HandlerChainConfig {

    @Bean
    public UsernameNotEmptyHandler handlerChain(UsernameNotEmptyHandler h1,
                                                EmailFormatHandler h2,
                                                UsernameUniqueHandler h3) {
        // 按顺序组装责任链
        h1.setNext(h2).setNext(h3);
        return h1;
    }
}