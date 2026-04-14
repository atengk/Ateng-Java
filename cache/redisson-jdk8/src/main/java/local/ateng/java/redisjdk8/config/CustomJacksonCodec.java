package local.ateng.java.redisjdk8.config;

import org.redisson.codec.JsonJacksonCodec;

public class CustomJacksonCodec extends JsonJacksonCodec {

    public CustomJacksonCodec() {
        super(JacksonObjectMapperFactory.buildStorageObjectMapper());
    }

}
