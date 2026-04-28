package local.ateng.java.awss3.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

/**
 * S3 配置属性类
 *
 * @author Ateng
 * @since 2025-07-18
 */
@Configuration
@ConfigurationProperties(prefix = "s3")
@Data
public class S3Properties {
    private String bucketName;
    private String accessKey;
    private String secretKey;
    private String region;
    private String endpoint;
    private boolean pathStyleAccess;
}
