package local.ateng.java.awss3.config;

import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.S3Configuration;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;

import java.net.URI;

/**
 * S3 配置类
 *
 * @author Ateng
 * @since 2025-07-18
 */
@Configuration
@RequiredArgsConstructor
public class S3Config {

    private final S3Properties s3Properties;

    /**
     * S3Client Bean
     */
    @Bean
    public S3Client s3Client() {
        return S3Client.builder()
                .endpointOverride(URI.create(s3Properties.getEndpoint()))
                .region(Region.of(s3Properties.getRegion()))
                .credentialsProvider(StaticCredentialsProvider.create(
                        AwsBasicCredentials.create(
                                s3Properties.getAccessKey(),
                                s3Properties.getSecretKey()
                        )
                ))
                .serviceConfiguration(S3Configuration.builder()
                        .pathStyleAccessEnabled(s3Properties.isPathStyleAccess())
                        .build())
                .build();
    }

    /**
     * S3Presigner Bean
     */
    @Bean
    public S3Presigner s3Presigner(S3Properties s3Properties) {
        return S3Presigner.builder()
                .endpointOverride(URI.create(s3Properties.getEndpoint()))
                .region(Region.of(s3Properties.getRegion()))
                .credentialsProvider(StaticCredentialsProvider.create(
                        AwsBasicCredentials.create(
                                s3Properties.getAccessKey(),
                                s3Properties.getSecretKey()
                        )
                ))
                .serviceConfiguration(S3Configuration.builder()
                        .pathStyleAccessEnabled(s3Properties.isPathStyleAccess())
                        .build())
                .build();
    }

}
