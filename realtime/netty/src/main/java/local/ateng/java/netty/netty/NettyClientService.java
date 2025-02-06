package local.ateng.java.netty.netty;

import io.netty.channel.Channel;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * Netty 客户端 服务
 *
 * @author 孔余
 * @email 2385569970@qq.com
 * @date 2024-08-29 09:12:29
 */
@Component
@RequiredArgsConstructor(onConstructor = @__(@Autowired))
public class NettyClientService {
    private final NettyClient nettyClient;

    /**
     * 发送消息
     *
     * @param message 消息
     */
    public void send(String message) {
        Channel channel = nettyClient.getChannel();
        channel.writeAndFlush(message);
    }
}
