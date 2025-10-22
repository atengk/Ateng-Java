package local.ateng.java.email.config;

import lombok.Data;
import org.dromara.email.api.Blacklist;
import org.dromara.email.api.MailClient;
import org.dromara.email.comm.config.MailSmtpConfig;
import org.dromara.email.core.factory.MailFactory;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.ArrayList;
import java.util.List;

@Configuration
@ConfigurationProperties(prefix = "sms-email")
@Data
public class DefaultMailSmtpConfig {

    private String port;
    private String fromAddress;
    private String nickName;
    private String smtpServer;
    private String username;
    private String password;
    private String isSSL = "true";
    private String isAuth = "true";
    private int retryInterval = 5;
    private int maxRetries = 1;

    @Bean
    public MailClient mailClient(){
        MailSmtpConfig config = MailSmtpConfig.builder()
                .port(port)
                .fromAddress(fromAddress)
                .smtpServer(smtpServer)
                .username(username)
                .password(password)
                .isSSL(isSSL)
                .isAuth(isAuth)
                .retryInterval(retryInterval)
                .maxRetries(maxRetries)
                .build();
        String key = "qq";
        MailFactory.put(key, config);
       return MailFactory.createMailClient(key, new BlackListImpl());
    }

    /**
     * 黑名单配置
     */
    public class BlackListImpl implements Blacklist {

        @Override
        public List<String> getBlacklist() {
            List<String> blacklist = new ArrayList<>();
            blacklist.add("2385569970@qq.com");
            return blacklist;
        }
    }

}
