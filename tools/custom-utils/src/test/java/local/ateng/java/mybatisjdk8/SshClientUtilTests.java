package local.ateng.java.mybatisjdk8;

import local.ateng.java.customutils.utils.SshClientUtil;
import org.apache.sshd.client.SshClient;
import org.apache.sshd.client.session.ClientSession;
import org.junit.jupiter.api.Test;

import java.io.IOException;

public class SshClientUtilTests {

    @Test
    void testSsh() throws IOException {
        SshClient client = SshClientUtil.createDefaultClient();
        ClientSession session = SshClientUtil.createPasswordSession(client, "192.168.1.10", 38101, "root", "Admin@123");
        String result = SshClientUtil.execute(session, "echo hello");
        System.out.println(result);
    }


}
