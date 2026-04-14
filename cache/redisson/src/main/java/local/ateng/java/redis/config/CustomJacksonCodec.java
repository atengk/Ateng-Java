package local.ateng.java.redis.config;

import org.redisson.codec.JsonJacksonCodec;

public class CustomJacksonCodec extends JsonJacksonCodec {

    public CustomJacksonCodec() {
        super(JacksonObjectMapperFactory.buildStorageObjectMapper());
    }

}
