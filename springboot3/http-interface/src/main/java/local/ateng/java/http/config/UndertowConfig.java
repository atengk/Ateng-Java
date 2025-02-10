package local.ateng.java.http.config;

import io.undertow.UndertowOptions;
import io.undertow.server.DefaultByteBufferPool;
import io.undertow.websockets.jsr.WebSocketDeploymentInfo;
import org.springframework.boot.web.embedded.undertow.UndertowServletWebServerFactory;
import org.springframework.boot.web.server.WebServerFactoryCustomizer;
import org.springframework.context.annotation.Configuration;

/**
 * Undertow 服务器配置
 *
 * @author 孔余
 * @email 2385569970@qq.com
 * @since 2025-02-10
 */
@Configuration
public class UndertowConfig implements WebServerFactoryCustomizer<UndertowServletWebServerFactory> {
    private final int core = Runtime.getRuntime().availableProcessors();

    @Override
    public void customize(UndertowServletWebServerFactory factory) {
        // 定制 Undertow 服务器的构建器
        factory.addBuilderCustomizers(builder -> {
            // 设置IO线程数
            builder.setIoThreads(core * 2);
            // 设置工作线程数
            builder.setWorkerThreads(core * 2 * 4);
            // 设置缓冲区大小
            builder.setBufferSize(1024);
            // 设置是否直接使用Buffers
            builder.setDirectBuffers(true);
            // 启用HTTP/2
            builder.setServerOption(UndertowOptions.ENABLE_HTTP2, true);
            // 设置最大HTTP POST请求大小
            builder.setServerOption(UndertowOptions.MAX_ENTITY_SIZE, 10 * 1024 * 1024L); // 10MB
        });
        // 定制 WebSocket 的部署信息
        factory.addDeploymentInfoCustomizers(deploymentInfo -> {
            WebSocketDeploymentInfo webSocketDeploymentInfo = new WebSocketDeploymentInfo();
            webSocketDeploymentInfo.setBuffers(new DefaultByteBufferPool(true, 1024));
            deploymentInfo.addServletContextAttribute("io.undertow.websockets.jsr.WebSocketDeploymentInfo", webSocketDeploymentInfo);
        });
    }
}
