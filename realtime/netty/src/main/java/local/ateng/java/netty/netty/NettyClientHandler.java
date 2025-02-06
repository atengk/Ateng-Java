package local.ateng.java.netty.netty;

import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;

public class NettyClientHandler extends SimpleChannelInboundHandler<String> {

    /**
     * 处理从服务器接收到的消息。
     *
     * @param ctx 客户端上下文
     * @param msg 接收到的消息
     */
    @Override
    protected void channelRead0(ChannelHandlerContext ctx, String msg) throws Exception {
        //System.out.println("Received message from server: " + msg);
    }

    /**
     * 当连接到服务器时调用，可以在这里执行连接成功后的逻辑。
     */
    @Override
    public void channelActive(ChannelHandlerContext ctx) throws Exception {
        //System.out.println("Client connected to server: " + ctx.channel().remoteAddress());
    }

    /**
     * 当与服务器断开连接时调用，可以在这里执行断开后的逻辑。
     */
    @Override
    public void channelInactive(ChannelHandlerContext ctx) throws Exception {
        //System.out.println("Client disconnected from server");
    }

    /**
     * 捕获处理过程中发生的异常并关闭连接。
     *
     * @param ctx   客户端上下文
     * @param cause 异常原因
     */
    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
        cause.printStackTrace();  // 打印异常堆栈
        ctx.close();  // 关闭连接
    }

}