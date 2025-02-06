package local.ateng.java.netty.netty;


import io.netty.bootstrap.Bootstrap;
import io.netty.channel.Channel;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelPipeline;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioSocketChannel;
import io.netty.handler.codec.string.StringDecoder;
import io.netty.handler.codec.string.StringEncoder;
import io.netty.handler.logging.LogLevel;
import io.netty.handler.logging.LoggingHandler;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;


/**
 * Netty客户端
 *
 * @author 孔余
 * @email 2385569970@qq.com
 * @date 2024-08-29 09:21:02
 */
@Component
@Slf4j
public class NettyClient {

    private final EventLoopGroup group = new NioEventLoopGroup();
    private Channel channel;

    // 从 Spring 配置文件中读取服务器地址和端口号
    @Value("${netty.client.host:192.168.1.12}")
    private String host;
    @Value("${netty.client.port:27248}")
    private int port;

    /**
     * 启动 Netty 客户端并连接到服务器。
     * 使用 @PostConstruct 注解确保该方法在 Spring Bean 初始化后执行。
     */
    @PostConstruct
    public void start() {
        try {
            Bootstrap b = new Bootstrap(); // 创建并配置客户端的启动引导类

            b.group(group) // 设置线程组
                    .channel(NioSocketChannel.class) // 指定客户端 NIO 通信的通道类型
                    .handler(new LoggingHandler(LogLevel.INFO)) // 为客户端通道添加日志记录器，便于调试
                    .handler(new ChannelInitializer<SocketChannel>() { // 初始化客户端的通道
                        @Override
                        protected void initChannel(SocketChannel ch) throws Exception {
                            ChannelPipeline p = ch.pipeline(); // 获取客户端的处理管道
                            // 添加编码器和解码器，处理字符串类型的数据传输
                            p.addLast(new StringDecoder());
                            p.addLast(new StringEncoder());
                            // 添加自定义的业务逻辑处理器
                            p.addLast(new NettyClientHandler());
                        }
                    });

            // 连接到服务器并同步等待连接完成
            channel = b.connect(host, port).sync().channel();
            log.info("连接TCP服务端成功：{}:{}", host, port);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt(); // 恢复线程中断状态
            log.error("连接TCP服务端失败：{}:{}, 错误内容：{}", host, port, e.getMessage());
        }
    }

    /**
     * 获取 Netty 客户端通道
     *
     * @return 客户端通道对象
     */
    public Channel getChannel() {
        return channel;
    }

    /**
     * 停止 Netty 客户端并释放资源。
     * 使用 @PreDestroy 注解确保该方法在 Spring Bean 销毁之前执行。
     */
    @PreDestroy
    public void stop() {
        if (channel != null) {
            channel.close(); // 关闭客户端通道
        }
        // 优雅地关闭线程组，释放资源
        group.shutdownGracefully();
        log.info("关闭TCP服务端连接：{}:{}", host, port);
    }
}

