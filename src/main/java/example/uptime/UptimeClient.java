package example.uptime;

import io.netty.bootstrap.Bootstrap;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelFutureListener;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioSocketChannel;
import io.netty.handler.timeout.IdleStateHandler;

/**
 * Created by PaulZhang on 2016/7/4.
 */
public class UptimeClient {

    static final String HOST = System.getProperty("host", "127.0.0.1");
    static final int PORT = Integer.parseInt(System.getProperty("port", "8080"));

    // Sleep 5 seconds before a reconnection attempt.
    static final int RECONNECT_DELAY = Integer.parseInt(System.getProperty("reconnectDelay", "5"));
    // Reconnect when the server sends nothing for 10 seconds.
    static final int READ_TIMEOUT = Integer.parseInt(System.getProperty("readTimeout", "10"));

    private static final UptimeClientHandler handler = new UptimeClientHandler();

    public static void main(String[] args) {
        configBootstrap(new Bootstrap()).connect();
    }

    private static Bootstrap configBootstrap(Bootstrap bootstrap) {
        return configBootstrap(bootstrap, new NioEventLoopGroup());
    }

    static Bootstrap configBootstrap(Bootstrap bootstrap, EventLoopGroup eventExecutors) {
        bootstrap.group(eventExecutors)
                .channel(NioSocketChannel.class)
                .remoteAddress(HOST, PORT)
                .handler(new ChannelInitializer<SocketChannel>() {
                    @Override
                    protected void initChannel(SocketChannel socketChannel) throws Exception {
                        socketChannel.pipeline().addLast(new IdleStateHandler(READ_TIMEOUT, 0, 0), handler);
                    }
                });

        return bootstrap;
    }

    static void connect(Bootstrap bootstrap) {
        bootstrap.connect().addListener(new ChannelFutureListener() {
            @Override
            public void operationComplete(ChannelFuture channelFuture) throws Exception {
                if (channelFuture.cause() != null) {
                    handler.startTime = -1;
                    handler.println("Failed to connect: " + channelFuture.cause());
                }
            }
        });
    }
}
