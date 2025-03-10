package local.ateng.java.cloud.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

import java.time.LocalDate;

@Configuration
@ConfigurationProperties(prefix = "ateng")
@Data
public class AtengConfig {
    private String name;
    private Integer age;
    private String address;
    private LocalDate date;
}
