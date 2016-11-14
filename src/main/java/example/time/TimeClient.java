package example.time;

import example.time.codec.TimeDecoder;
import io.netty.bootstrap.Bootstrap;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelOption;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioSocketChannel;

/**
 * Created by PaulZhang on 2016/11/14.
 */
public class TimeClient {

    private int port;
    private String host;

    public TimeClient(String host, int port) {
        this.port = port;
        this.host = host;
    }

    public void start() throws InterruptedException {
        EventLoopGroup workGroup = new NioEventLoopGroup();
        try {
            Bootstrap bootstrap = new Bootstrap();
            bootstrap.group(workGroup)
                    .channel(NioSocketChannel.class)
                    .handler(new ChannelInitializer<SocketChannel>() {
                        @Override
                        protected void initChannel(SocketChannel socketChannel) throws Exception {
                            socketChannel.pipeline().addLast(new TimeDecoder(), new TimeClientHandler());
                        }
                    })
                    .option(ChannelOption.SO_KEEPALIVE, true);

            ChannelFuture future = bootstrap.connect(host, port).sync();

            future.channel().closeFuture().sync();
        } finally {
            workGroup.shutdownGracefully();
        }
    }

    public static void main(String[] args) throws InterruptedException {
        String host;
        int port;

        if (args.length == 2) {
            host = args[0];
            port = Integer.parseInt(args[1]);
        } else {
            host = "localhost";
            port = 8899;
        }

        new TimeClient(host, port).start();
    }
}
