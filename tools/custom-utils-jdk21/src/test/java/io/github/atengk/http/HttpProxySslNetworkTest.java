package io.github.atengk.http;

import io.github.atengk.utils.http.HttpUtil;
import org.junit.jupiter.api.Test;

import java.net.InetSocketAddress;
import java.net.http.HttpClient;

import static org.junit.jupiter.api.Assertions.*;

class HttpProxySslNetworkTest {

    @Test
    void shouldCreateClientWithProxyAndInsecureSslContext() {
        HttpUtil.ClientOptions options = HttpUtil.ClientOptions.defaults()
                .withProxy(new InetSocketAddress("127.0.0.1", 8888))
                .withSslContext(HttpUtil.insecureSslContext());
        HttpClient client = HttpUtil.newClient(options);
        assertNotNull(client);
        assertTrue(client.proxy().isPresent());
        assertTrue(client.sslContext().getProtocol().contains("TLS"));
    }
}
