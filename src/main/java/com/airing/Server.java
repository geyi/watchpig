package com.airing;

import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.Channel;
import io.netty.channel.ChannelDuplexHandler;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelOption;
import io.netty.channel.ChannelPipeline;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.handler.codec.http.HttpObjectAggregator;
import io.netty.handler.codec.http.HttpServerCodec;
import io.netty.handler.codec.http.websocketx.WebSocketServerProtocolHandler;
import io.netty.handler.timeout.IdleState;
import io.netty.handler.timeout.IdleStateEvent;
import io.netty.handler.timeout.IdleStateHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.InetSocketAddress;
import java.util.concurrent.TimeUnit;

public class Server {

    private static final Logger log = LoggerFactory.getLogger(Server.class);

    private int bossN;
    private int workerN;
    private NioEventLoopGroup boss;
    private NioEventLoopGroup worker;
    private Channel channel;

    public Server(int bossN, int workerN) {
        this.bossN = bossN;
        this.workerN = workerN;
        boss = new NioEventLoopGroup(bossN);
        worker = new NioEventLoopGroup(workerN);
    }

    public Channel startServer(int port) throws Exception {
        ServerBootstrap serverBootstrap = new ServerBootstrap().group(boss, worker)
                .channel(NioServerSocketChannel.class)
                .option(ChannelOption.SO_BACKLOG, 8)
                .childHandler(new ChannelInitializer<Channel>() {
                    @Override
                    protected void initChannel(Channel ch) throws Exception {
                        ChannelPipeline pipeline = ch.pipeline();
                        pipeline.addLast(new IdleStateHandler(1000, 0, 0, TimeUnit.MILLISECONDS));
                        pipeline.addLast(new IdleHandler());
                        // HttpRequestDecoder和HttpResponseEncoder的组合，使服务器端的HTTP实现更加容易
                        pipeline.addLast(new HttpServerCodec());
                        /*
                        将HttpMessage及其后续HttpContents聚合为单个的FullHttpRequest或者FullHttpResponse
                        原因是HTTP解码器在每个HTTP消息中会生成多个消息对象
                         */
                        pipeline.addLast(new HttpObjectAggregator(64 * 1024));
                        /*
                        负责websocket握手以及控制帧（Close、Ping、Pong）的处理
                        文本和二进制数据帧将传递给管道中的下一个处理程序（TextWebSocketFrameHandler）进行处理
                         */
                        pipeline.addLast(new WebSocketServerProtocolHandler("/psql"));
                        pipeline.addLast(new WebSocketServerHandler());
                    }
                });
        ChannelFuture bindFuture = serverBootstrap.bind(new InetSocketAddress(port));
        channel = bindFuture.sync().channel();
        log.info("server startup!!! bind port: {}, bossN: {}, workN: {}", port, bossN, workerN);
        return channel;
    }

    public void destroy() {
        if (channel != null) {
            channel.close().syncUninterruptibly();
        }
        boss.shutdownGracefully();
        worker.shutdownGracefully();
        log.info("server destroy!!!");
    }

    private static class IdleHandler extends ChannelDuplexHandler {
        public void userEventTriggered(ChannelHandlerContext ctx, Object evt) throws Exception {
            if (evt instanceof IdleStateEvent) {
                IdleStateEvent e = (IdleStateEvent) evt;
                if (e.state() == IdleState.READER_IDLE) {
                    log.debug("server read idle");
                    ctx.close();
                } else if (e.state() == IdleState.WRITER_IDLE) {
                }
            }
        }
    }

}
