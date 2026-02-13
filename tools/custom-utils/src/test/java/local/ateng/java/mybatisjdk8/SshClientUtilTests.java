package local.ateng.java.mybatisjdk8;

import local.ateng.java.customutils.utils.SshClientUtil;
import org.junit.jupiter.api.Test;

public class SshClientUtilTests {

    @Test
    void testEnterpriseSsh() {

        SshClientUtil.SshConfig config =
                new SshClientUtil.SshConfig()
                        .setHost("192.168.1.10")
                        .setPort(38101)
                        .setUsername("root")
                        .setPassword("Admin@123");

        SshClientUtil.SshResult result =
                SshClientUtil.exec(config, "ip a");

        System.out.println(result.getStdout());
    }


}
