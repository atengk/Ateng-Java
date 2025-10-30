package io.github.atengk.config;

import io.github.atengk.service.AtengService;
import io.github.atengk.service.impl.AtengServiceImpl;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.context.annotation.Bean;

@AutoConfiguration
public class AtengAutoConfiguration {

    @Bean
    public AtengService atengService() {
        return new AtengServiceImpl();
    }

}
