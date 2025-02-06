package local.ateng.java.netty.netty;

import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.channel.group.ChannelGroup;
import io.netty.channel.group.DefaultChannelGroup;
import io.netty.util.concurrent.GlobalEventExecutor;

/**
 * 处理 Netty 服务端的客户端连接和消息。
 *
 * @author 孔余
 * @email 2385569970@qq.com
 * @date 2024-08-29 09:23:17
 */
public class NettyServerHandler extends SimpleChannelInboundHandler<String> {

    // 线程安全的 ChannelGroup，用于保存所有已连接的客户端通道
    private static final ChannelGroup channels = new DefaultChannelGroup(GlobalEventExecutor.INSTANCE);

    /**
     * 获取所有连接的客户端通道组
     *
     * @return 保存所有客户端通道的 ChannelGroup
     */
    public static ChannelGroup getChannels() {
        return channels;
    }

    /**
     * 当客户端连接到服务器时，调用该方法。
     * 将客户端通道添加到 channels 中。
     */
    @Override
    public void channelActive(ChannelHandlerContext ctx) throws Exception {
        channels.add(ctx.channel());
        System.out.println("Client connected: " + ctx.channel().remoteAddress());
    }

    /**
     * 当客户端断开连接时，调用该方法。
     * 将客户端通道从 channels 中移除。
     */
    @Override
    public void channelInactive(ChannelHandlerContext ctx) throws Exception {
        channels.remove(ctx.channel());
        System.out.println("Client disconnected: " + ctx.channel().remoteAddress());
    }

    /**
     * 当接收到客户端的消息时，调用该方法。
     * 打印接收到的消息。
     *
     * @param ctx 客户端的上下文信息
     * @param msg 接收到的消息
     */
    @Override
    protected void channelRead0(ChannelHandlerContext ctx, String msg) throws Exception {
        //System.out.println("Received message from " + ctx.channel().remoteAddress() + ": " + msg);
    }

    /**
     * 当处理过程中发生异常时，调用该方法。
     * 打印异常信息并关闭该客户端的连接。
     */
    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
        cause.printStackTrace();  // 打印异常堆栈
        ctx.close();  // 关闭连接
    }
}
