package local.ateng.java.netty.netty;

import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.Channel;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelPipeline;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.handler.codec.string.StringDecoder;
import io.netty.handler.codec.string.StringEncoder;
import io.netty.handler.logging.LogLevel;
import io.netty.handler.logging.LoggingHandler;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;


/**
 * Netty服务端
 *
 * @author 孔余
 * @email 2385569970@qq.com
 * @date 2024-08-29 09:21:02
 */
@Component
public class NettyServer {

    private static final Logger log = LoggerFactory.getLogger(NettyServer.class);
    // BossGroup 负责处理客户端的连接请求，线程数为1（通常即可）。
    private final EventLoopGroup bossGroup = new NioEventLoopGroup(1);
    // WorkerGroup 负责处理客户端的读写操作，线程数默认为 CPU 核心数的两倍。
    private final EventLoopGroup workerGroup = new NioEventLoopGroup();
    // 从 Spring 配置文件中读取端口号，默认为 27248
    @Value("${netty.server.port:27248}")
    private int port;
    // Netty 服务端的通道对象
    private Channel channel;

    /**
     * 启动 Netty 服务，绑定指定端口，处理客户端连接。
     * 使用 @PostConstruct 注解确保该方法在 Spring Bean 初始化后执行。
     */
    @PostConstruct
    public void start() {
        try {
            ServerBootstrap b = new ServerBootstrap(); // 创建并配置服务端的启动引导类

            b.group(bossGroup, workerGroup) // 设置线程组，处理连接和 IO 操作
                    .channel(NioServerSocketChannel.class) // 指定服务端 NIO 通信的通道类型
                    .handler(new LoggingHandler(LogLevel.INFO)) // 为服务端通道添加日志记录器，便于调试
                    .childHandler(new ChannelInitializer<SocketChannel>() { // 为每个客户端连接初始化处理器
                        @Override
                        protected void initChannel(SocketChannel ch) throws Exception {
                            ChannelPipeline p = ch.pipeline(); // 获取客户端的处理管道
                            // 添加编码器和解码器，处理字符串类型的数据传输
                            p.addLast(new StringDecoder());
                            p.addLast(new StringEncoder());
                            // 添加自定义的业务逻辑处理器
                            p.addLast(new NettyServerHandler());
                        }
                    });

            // 绑定端口并同步等待服务端启动
            channel = b.bind(port).sync().channel();
            log.info("启动TCP服务端成功，端口号：{}", port);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt(); // 恢复线程中断状态
            log.error("启动TCP服务端失败，端口号：{}，失败内容：{}", port, e.getMessage());
        }
    }

    /**
     * 获取 Netty 服务端通道
     *
     * @return 服务端通道对象
     */
    public Channel getChannel() {
        return channel;
    }

    /**
     * 停止 Netty 服务并释放资源。
     * 使用 @PreDestroy 注解确保该方法在 Spring Bean 销毁之前执行。
     */
    @PreDestroy
    public void stop() {
        if (channel != null) {
            channel.close(); // 关闭服务端通道
        }
        // 优雅地关闭线程组，释放资源
        bossGroup.shutdownGracefully();
        workerGroup.shutdownGracefully();
        log.info("关闭TCP服务，端口号：{}", port);
    }
}

