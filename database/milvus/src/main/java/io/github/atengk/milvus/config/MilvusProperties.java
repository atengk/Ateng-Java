package io.github.atengk.milvus.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration
@ConfigurationProperties(prefix = "milvus")
@Data
public class MilvusProperties {

    private String host;
    private int port;
    private String database;
    private String username;
    private String password;
}
