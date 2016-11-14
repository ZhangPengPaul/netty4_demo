package example.echo;


import io.netty.bootstrap.ServerBootstrap;
import io.netty.buffer.Unpooled;
import io.netty.channel.*;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.handler.codec.DelimiterBasedFrameDecoder;
import io.netty.handler.codec.string.StringDecoder;
import io.netty.handler.logging.LogLevel;
import io.netty.handler.logging.LoggingHandler;
import io.netty.handler.ssl.SslContext;
import io.netty.handler.ssl.SslContextBuilder;
import io.netty.handler.ssl.util.SelfSignedCertificate;

import javax.net.ssl.SSLException;
import java.security.cert.CertificateException;
import java.util.Objects;

/**
 * Echo back any received data from a client
 * <p>
 * Created by PaulZhang on 2016/7/1.
 */
public final class EchoServer {

    static final boolean SSL = System.getProperty("ssl") != null;
    static final Integer PORT = Integer.parseInt(System.getProperty("port", "8007"));

    public static void main(String[] args) throws CertificateException, SSLException, InterruptedException {
        // config ssl
        final SslContext sslCtx;
        if (SSL) {
            SelfSignedCertificate ssc = new SelfSignedCertificate();
            sslCtx = SslContextBuilder.forServer(ssc.certificate(), ssc.privateKey()).build();
        } else {
            sslCtx = null;
        }

        // config the server
        EventLoopGroup bossGroup = new NioEventLoopGroup(1); // boss group, accept incoming connection
        EventLoopGroup workerGroup = new NioEventLoopGroup(); // worker group, handler the traffic of accepted connection

        try {
            ServerBootstrap b = new ServerBootstrap();
            b.group(bossGroup, workerGroup)
                    .channel(NioServerSocketChannel.class)
                    .option(ChannelOption.SO_BACKLOG, 100)
                    .handler(new LoggingHandler(LogLevel.INFO))
                    .childHandler(new ChannelInitializer<SocketChannel>() {
                        protected void initChannel(SocketChannel socketChannel) throws Exception {
                            ChannelPipeline pipeline = socketChannel.pipeline();
                            if (Objects.nonNull(sslCtx)) {
                                pipeline.addLast(sslCtx.newHandler(socketChannel.alloc()));
                            }

//                            pipeline.addLast(new LineBasedFrameDecoder(1024));
                            pipeline.addLast(new DelimiterBasedFrameDecoder(2048, Unpooled.copiedBuffer("#".getBytes())));
                            pipeline.addLast(new StringDecoder());

                            pipeline.addLast(new EchoServerHandler());
                        }
                    });

            // start the server
            ChannelFuture future = b.bind(PORT).sync();

            // wait until the server socket is closed
            future.channel().closeFuture().sync();
        } finally {
            // shutdown all the event loops
            bossGroup.shutdownGracefully();
            workerGroup.shutdownGracefully();

        }

    }
}
