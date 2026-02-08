package io.github.atengk.milvus.config;

import io.milvus.client.MilvusClient;
import io.milvus.client.MilvusServiceClient;
import io.milvus.param.ConnectParam;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class MilvusConfig {

    @Bean(destroyMethod = "close")
    public MilvusClient milvusClient(MilvusProperties props) {
        return new MilvusServiceClient(
                ConnectParam.newBuilder()
                        .withHost(props.getHost())
                        .withPort(props.getPort())
                        .withDatabaseName(props.getDatabase())
                        .withAuthorization(
                                props.getUsername(),
                                props.getPassword()
                        )
                        .build()
        );
    }

}
