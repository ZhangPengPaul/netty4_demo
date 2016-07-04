package example.uptime;

import io.netty.bootstrap.Bootstrap;
import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.EventLoop;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.handler.timeout.IdleState;
import io.netty.handler.timeout.IdleStateEvent;

import java.util.concurrent.TimeUnit;

/**
 * Created by PaulZhang on 2016/7/4.
 */
@ChannelHandler.Sharable
public class UptimeClientHandler extends SimpleChannelInboundHandler<Object> {

    long startTime = -1;

    @Override
    protected void channelRead0(ChannelHandlerContext channelHandlerContext, Object o) throws Exception {

    }

    @Override
    public void channelActive(ChannelHandlerContext ctx) throws Exception {
        if (startTime < 0) {
            startTime = System.currentTimeMillis();
        }

        println("Connected to: " + ctx.channel().remoteAddress());
    }

    @Override
    public void userEventTriggered(ChannelHandlerContext ctx, Object evt) throws Exception {
        if (!(evt instanceof IdleStateEvent)) {
            return;
        }

        IdleStateEvent e = (IdleStateEvent) evt;
        if (e.state() == IdleState.READER_IDLE) {
            println("Disconnecting due to no inbound traffic");
            ctx.close();
        }
    }

    @Override
    public void channelInactive(ChannelHandlerContext ctx) throws Exception {
        println("Disconnected from: " + ctx.channel().remoteAddress());
    }

    @Override
    public void channelRegistered(ChannelHandlerContext ctx) throws Exception {
        println("Sleeping for: " + UptimeClient.RECONNECT_DELAY + 's');
        final EventLoop loop = ctx.channel().eventLoop();
        loop.schedule(() -> {
            println("Reconnecting to: " + UptimeClient.HOST + ':' + UptimeClient.PORT);
            UptimeClient.connect(UptimeClient.configBootstrap(new Bootstrap(), loop));

        }, UptimeClient.READ_TIMEOUT, TimeUnit.SECONDS);
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
        cause.printStackTrace();
        ctx.close();
    }

    void println(String msg) {
        if (startTime < 0) {
            System.err.format("[SERVER IS DOWN] %s%n", msg);
        } else {
            System.err.format("[UPTIME: %5ds] %s%n", (System.currentTimeMillis() - startTime) / 1000, msg);
        }
    }
}
