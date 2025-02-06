package local.ateng.java.netty.netty;

import io.netty.channel.group.ChannelGroup;

/**
 * Netty 服务端 服务
 *
 * @author 孔余
 * @email 2385569970@qq.com
 * @date 2024-08-29 09:12:29
 */
public class NettyServerService {

    /**
     * 发送消息
     *
     * @param message 消息
     */
    public static void send(String message) {
        ChannelGroup channels = NettyServerHandler.getChannels();
        channels.writeAndFlush(message);
    }
}
